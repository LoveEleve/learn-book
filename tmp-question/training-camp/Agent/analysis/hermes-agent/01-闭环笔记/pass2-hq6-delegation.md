# hq6 委派与子代理(delegate_task + 生命周期)— 产品②"并行执行"蓝本

> 项目:Hermes(tools/delegate_tool.py 4,678 行 + tools/async_delegation.py 1,603 + agent/subagent_lifecycle.py 540 + tools/daemon_pool.py)
> 假设:Hermes 委派是"多代理并行执行"最完整样本——角色树/并发上限/摘要预算/心跳活性/生命周期服务。
> 结论:✅ 成立——委派展示了"子代理安全并行"的每个维度。

---

## 一、架构全景

```
┌────────────────────────────────────────────────────────────┐
│ delegate_task(单/批/控制三形态)                            │
│   单:goal+context / 批:tasks[] 并行 / 控制:list/steer/stop │
└──────────────┬─────────────────────────────────────────────┘
               │ 角色树(leaf/orchestrator,深度上限)
┌──────────────▼─────────────────────────────────────────────┐
│ 并发治理:max_concurrent_children + 信号量 + 中断轮询        │
└──────────────┬─────────────────────────────────────────────┘
               │ 摘要预算(动态 headroom ÷ batch)
┌──────────────▼─────────────────────────────────────────────┐
│ 结果回注:摘要裁剪(head+tail+溢出文件)+ 父上下文保护        │
└──────────────┬─────────────────────────────────────────────┘
               │ 活性:心跳(父活动防超时)+ stale 检测
┌──────────────▼─────────────────────────────────────────────┐
│ 生命周期:SubagentLifecycleService(launch/status/wait/      │
│           cancel/reconnect)+ 后台完成队列                   │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:工具块清单(子代理能力边界)

**位置**:`tools/delegate_tool.py:50-57`

```
DELEGATE_BLOCKED_TOOLS:
- delegate_task(无递归委派)
- clarify(无用户交互)
- memory(无共享 MEMORY.md 写)
- send_message(无跨平台副作用)
- cronjob(无调度更多工作)
```

**工具集剥离**:`_strip_blocked_tools`(含只含被禁工具的复合工具集)+ `_blocked_toolsets_for_role`(orchestrator 保留 delegate_task 的 deny 集)。

**产品④映射**:子代理能力边界——不能递归/不能交互/不能共享写。

## 设计 2:角色树(leaf/orchestrator)

**位置**:`delegate_tool.py:1061-1160`(_build_child_system_prompt)

```
- leaf(默认):不能 delegate_task/clarify/memory/send_message/cronjob;保留 execute_code
- orchestrator:保留 delegate_task,可 spawn 自己 worker
- 深度上限 max_spawn_depth(默认 1——扁平:parent(0)→child(1),孙代默认拒绝,提高配置才允许;system prompt 注入"你深度 N,树封顶 M"
  ("深度说明是字面真相——LLM 不臆造不存在的嵌套能力")
```

**产品④映射**:多章并行分析的委派树——深度上限 + 角色边界。

## 设计 3:并发治理

**位置**:`delegate_tool.py:734-816` + `3656-3780`(_execute_and_aggregate)

```
- max_concurrent_children(默认 3)
- DaemonThreadPoolExecutor(守护线程——父中断时 wedged child 不阻塞解释器退出)
- wait(FIRST_COMPLETED,0.5s 轮询)防 as_completed 阻塞(父中断时收集已完放弃其余)
- 父中断:已完的取结果,未完成标记 interrupted(不无限等待)
- contextvars 复制(profile 隔离)
```

**产品④映射**:多章并行——并发上限 + 中断时"收集已完放弃其余"。

## 设计 4:摘要预算(父上下文保护)

**位置**:`delegate_tool.py:2072-2260`

```
- _parent_summary_char_budget:父剩余 headroom ÷ batch 数
  (context_length - 已用 tokens - 输出预留) × 0.5 折(_SUMMARY_HEADROOM_FRACTION)→ 每摘要预算
- 超预算 → _trim_summary_with_footer:head+tail(75/25,行对齐)+
  spill 全量到 cache/delegation 文件 + footer 指示 read_file offset 分页
- 静态上限 delegation.max_summary_chars 取 min
- 背景:issue #9126 批量 N 个全量摘要炸父上下文 → 压缩/429 死亡螺旋
```

**产品④映射**:多章结果回注的上下文预算——head+tail+溢出文件+分页指示。

## 设计 5:心跳与 stale 检测

**位置**:`delegate_tool.py:2295-2420`

```
- 周期 touch 父活动(防网关不活动超时杀)
- stale 检测:子 (tool, iteration, activity_ts) 三信号
  - 任一前进 → 重置计数
  - 全冻结 → 计数;in-tool 阈值高于 idle(慢工具 vs 真卡死)
- stale 达限 → 停止心跳,让网关超时触发(不干预)
```

**产品④映射**:并行任务活性——父活动保持 + 子 stale 分级判定。

## 设计 6:SubagentLifecycleService(完整生命周期)

**位置**:`agent/subagent_lifecycle.py:187-487`

```
- launch(request → handle)/status/wait/cancel/reconnect/result
- SubagentState 枚举 + 注册表 + 父代理绑定
- 验证:_validate_request(能力/深度/工具)
- 错误:SubagentLifecycleError
```

**产品④映射**:子代理生命周期 API(launch→status→wait→cancel→reconnect)。

## 设计 7:后台委派(异步完成队列)

**位置**:`tools/async_delegation.py:1,603` + delegate_tool background

```
- background=true:立即返回 delegation id,结果经异步完成队列回注
- 批作为单异步单元:整个 fan-out 在守护 executor,join 全部,
  完成推单事件(合并 per-task 结果)——聊天不被阻塞
- 持久化:async_delegations 表(delivery_state 账本,hermes_state v9)
- 耐久性规则:background delegate 进程内;跨重启用 cronjob/terminal(background)
```

**产品④映射**:异步并行回注——单完成事件合并批结果 + 持久化账本。

## 设计 8:审批回调(子代理线程安全)

**位置**:`delegate_tool.py:60-100`

```
- CLI 交互审批回调在 threading.local → worker 线程不继承
  (没有回调时 input() 从 worker 线程 → 与父 prompt_toolkit 死锁)
- ThreadPoolExecutor(initializer=_set_subagent_approval_cb) 注入
- subagent_auto_approve:false(默认)→ _subagent_auto_deny(安全)
  true → _subagent_auto_approve(opt-in YOLO)
- 都 logger.warning 审计
```

**产品④映射**:并行线程的审批安全——worker 不继承交互回调 → 默认拒绝。

## 设计 9:委派结果记忆观察

**位置**:`memory_provider.py on_delegation(task, result)`

- 父代理记忆 provider 收到 (task, result) 对作为"委派观察"
- 子代理自身 skip_memory(独立视角不污染主记忆)

**产品④映射**:委派痕迹持久化——父侧观察而非子侧写。

## 设计 10:委派事件协议

**位置**:`delegate_tool.py:1024-1060`

```
DelegateEvent 枚举:TASK_SPAWNED/TASK_PROGRESS/TASK_COMPLETED/TASK_FAILED/
TASK_THINKING/TASK_TOOL_STARTED/TASK_TOOL_COMPLETED
+ 旧字符串映射(_LEGACY_EVENT_MAP——兼容窗口)
```

**产品④映射**:并行任务的事件化(进度/完成/失败对前端透明)。

---

## 三、与 Pi/Reasonix 对比

| 维度 | Pi | Reasonix | Hermes |
|------|----|----------|--------|
| 并行 | file-mutation-queue | fleet(2-64,预声明 write_paths) | delegate_task(并发上限 3+信号量) |
| 角色 | — | subagent depth | leaf/orchestrator 树 |
| 结果回注 | — | — | **摘要预算 head+tail+溢出文件** |
| 活性 | — | — | **心跳+stale 分级** |
| 生命周期 | — | SubagentLifecycleService(Reasonix 同名!) | launch/status/wait/cancel/reconnect |
| 持久化 | — | jobs.Manager | async_delegations 账本 |

**结论**:产品②并行执行——Reasonix fleet(预声明写路径)+ Hermes 摘要预算/心跳 + 生命周期服务。

---

## 四、面试弹药

1. **"子代理审批死锁"**:交互回调在 TLS,worker 线程不继承 → input() 从 worker 与父 TUI 死锁;initializer 注入 + 默认 deny
2. **"N 个摘要炸父上下文"**(#9126):head+tail+溢出文件+分页 footer——批量回注的预算
3. **"as_completed 阻塞陷阱"**:父中断时 as_completed 等全部 → wait 0.5s 轮询收集已完
4. **"心跳区分慢与卡死"**:in-tool 阈值高于 idle——慢工具不误杀
5. **"工具块清单"**:delegate_task/clarify/memory/send_message/cronjob——子代理能力边界五禁

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|----------|
| 工具块清单 | 子代理能力边界 |
| 角色树+深度上限 | 委派树防递归爆炸 |
| 并发治理 | 并行上限+中断收集 |
| 摘要预算 | 结果回注上下文保护 |
| 心跳+stale | 并行活性管理 |
| 生命周期服务 | 子代理 API |
| 后台队列+账本 | 异步回注+持久化 |
| 审批回调 | 线程安全审批 |
| 委派记忆观察 | 父侧观察持久化 |
| 事件协议 | 并行进度透明 |

> 覆盖设计数:11(设计 1-10 + 3 子设计)
