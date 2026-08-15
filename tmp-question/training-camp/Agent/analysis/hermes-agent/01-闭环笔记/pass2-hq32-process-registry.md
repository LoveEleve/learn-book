# hq32 进程注册表(Process Registry)— 产品②"后台进程管理"蓝本

> 项目:Hermes(tools/process_registry.py 2,987 行 + daemon_pool.py + tests/tools/test_process_registry*.py 78 用例)
> 假设:terminal(background=true) 的后台进程需完整管理(spawn/poll/read_log/wait/kill/stdin)——Hermes 的进程注册表是"后台进程生命周期"的完整样本(域发现 v9:systemd scope/输出监听/模式匹配 + 守护线程池)。
> 结论:✅ 成立——双 spawn 路径/查询族/kill 族/恢复检查点/通知消费/内存限制全具备,产品②"后台任务管理"直接蓝本。

---

## 一、架构全景:后台进程生命周期

```
┌────────────────────────────────────────────────────────────┐
│ spawn 双路径:                                              │
│   spawn_local(966):本地进程                                │
│   spawn_via_env(1204):经环境接口(远端后端)                 │
│   ——"Background processes execute THROUGH the environment  │
│     interface"                                             │
├────────────────────────────────────────────────────────────┤
│ 查询族:                                                   │
│   get(1727)/poll(1807)/read_log(1850,offset+limit)/        │
│   wait(1892,timeout)/count_running/list_sessions(2260)/    │
│   has_active_*(task/session/any)                           │
├────────────────────────────────────────────────────────────┤
│ 控制族:                                                   │
│   kill_process(2011)/write_stdin(2138)/submit_stdin/       │
│   request_close_terminal/close_stdin/kill_started_since/   │
│   kill_all(2415)                                           │
├────────────────────────────────────────────────────────────┤
│ 生命周期:                                                 │
│   is_completion_consumed(1583)/is_session_waiting/         │
│   drain_notifications(1640)/recover_from_checkpoint(2537)  │
│   + systemd scope(_build_systemd_scope_argv,273)           │
├────────────────────────────────────────────────────────────┤
│ 守护:daemon_pool.py(守护线程池,解释器退出不阻塞)           │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:双 spawn 路径(本地/环境接口)

**位置**:`process_registry.py:966`(spawn_local)+ `1204`(spawn_via_env)

```
spawn_local:本地进程 spawn(直接)
spawn_via_env:经环境接口——远端后端(docker/ssh)同一注册表
  ("Background processes execute THROUGH the environment interface")

ProcessSession(367):进程会话模型(id/状态/输出监听)
```

**正确性价值**:双路径统一注册表——本地/远端同一管理面(环境接口抽象)。

**产品④映射**:后台任务统一管理——本地/远端同一注册表。

## 设计 2:查询族(完整状态面)

**位置**:`process_registry.py:1727-2317`

```
get/poll(状态快照)/read_log(offset+limit 分页)/wait(timeout)
count_running/list_sessions(task_id/session_key 过滤)
has_active_processes/has_active_for_session/has_any_active
snapshot_running_ids(task_id)

——后台进程状态完全可查询(与 hq18 gather_background_processes 衔接)
```

**正确性价值**:完整查询面——任何调用方可查状态/日志/活性。

**产品④映射**:后台任务查询面——状态/日志/活性完整可查。

## 设计 3:控制族(完整生命周期)

**位置**:`process_registry.py:2011-2415`

```
kill_process/kill_started_since/kill_all(会话/时间/全量)
write_stdin/submit_stdin/close_stdin(输入面)
request_close_terminal(终端关闭)

——进程可控(非只读):杀/输/关
```

**正确性价值**:进程可控完整——杀(会话/时间/全量)+ 输入面 + 关闭。

**产品④映射**:后台任务控制面——杀/输入/关闭完整。

## 设计 4:恢复检查点 + 通知消费

**位置**:`process_registry.py:2537`(recover_from_checkpoint)+ `1583-1640`(消费/等待)

```
recover_from_checkpoint:崩溃恢复(检查点重建活跃进程)
is_completion_consumed/is_session_waiting:完成消费/等待状态
drain_notifications:通知排空(完成事件消费)

——崩溃后恢复 + 通知一次性消费(与 delivery_ledger 同族)
```

**正确性价值**:崩溃恢复 + 通知消费——进程状态跨崩溃可恢复。

**产品④映射**:后台任务恢复——检查点重建 + 通知消费一次性。

## 设计 5:systemd scope + 守护线程池

**位置**:`process_registry.py:273`(_build_systemd_scope_argv)+ `248`(_is_supervised_gateway_process)+ daemon_pool.py

```
systemd scope:受监督网关下进程入 scope(资源隔离/统一清理)
  _is_supervised_gateway_process:受监督判定
daemon_pool:守护线程池——解释器退出不阻塞(域发现 v9)

内存限制:_worker_memory_max_bytes(108)——进程内存上限
```

**正确性价值**:受监督部署下资源隔离 + 守护池(退出不阻塞)+ 内存上限。

**产品④映射**:后台任务资源治理——scope/守护池/内存上限。

---

## 三、与四项目对比(后台进程管理)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes process_registry |
|------|----|----------|----------|-----|------------------------|
| 后台进程 | — | jobs.Manager | — | — | **完整注册表(spawn→kill)** |
| 双路径 | — | — | — | — | **本地/环境接口统一** |
| 查询 | — | — | — | — | **状态/日志/活性完整面** |
| 控制 | — | — | — | — | **杀/输入/关闭族** |
| 恢复 | — | checkpoint | — | — | **崩溃检查点重建** |

**结论**:产品"后台任务管理"参考 = Hermes process_registry(完整生命周期 + 双路径 + 恢复)+ Reasonix jobs.Manager(任务管理)。**与 hq18 wait 停泊衔接(gather_background_processes 读此注册表)**。

---

## 四、面试弹药

1. **"经环境接口"**:远端后端(docker/ssh)同一注册表——环境接口抽象统一
2. **"完整查询面"**:get/poll/read_log(分页)/wait——状态完全可查
3. **"完整控制面"**:kill(会话/时间/全量)/stdin/关闭——非只读
4. **"崩溃检查点重建"**:recover_from_checkpoint——进程状态跨崩溃恢复
5. **"systemd scope + 守护池"**:受监督部署资源隔离 + 解释器退出不阻塞

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 双 spawn 路径 | 本地/远端统一管理 |
| 查询族 | 状态/日志/活性完整面 |
| 控制族 | 杀/输入/关闭 |
| 恢复 + 通知 | 崩溃重建 + 一次性消费 |
| scope + 守护池 | 资源治理 + 退出不阻塞 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_process_registry.py + test_process_registry_write_stdin_surrogates.py(78 用例)
> 位置:ProcessRegistry :419 / spawn_local :966 / spawn_via_env :1204 / poll :1807 / read_log :1850 / kill_all :2415 / recover_from_checkpoint :2537 / systemd scope :273
> 关联:daemon_pool.py(守护线程池)/ hq18 gather_background_processes 读此注册表
