# hq10 生命周期账本(Lifecycle Ledger)— 产品④"脏死检测"蓝本

> 项目:Hermes(gateway/lifecycle_ledger.py 332 行 + gateway/shutdown_watchdog.py 心跳/退出接线 + gateway/run.py 启动/退出接线 + hermes_cli/container_boot.py 标签消费 + utils.py atomic_json_write)
> 假设:优雅退出的取证(谁发了 SIGTERM)已有,但**不洁死亡**(SIGKILL/OOM/VM 死)没有任何记录——下次 boot 不知道上一生命暴力终结。Hermes 用微型哨兵状态机持久化到 gateway.lifecycle.json,是"脏死检测"的完整样本。
> 结论:✅ 成立——哨兵状态机/心跳内存采样/OOM 启发/所有权守卫/标志前传全具备,产品④"崩溃归因"直接蓝本。

---

## 一、架构全景:为什么需要生命周期账本(NS-608)

```
问题:支持单如 NS-608 问"什么杀了 gateway?"需要人工交叉核对 4 个日志文件 + 2 个外部 API。
     优雅退出有取证(shutdown_forensics:SIGTERM 来源)+ exit-diag 日志,
     但 SIGKILL/OOM/VM 死 = 任何 handler 都没跑 → 上一生命怎么结束的毫无记录。

┌────────────────────────────────────────────────────────────┐
│ 启动:record_startup()                                      │
│   读上一生命留下的哨兵:phase=="running" → 从未走退出路径     │
│   = 不洁死亡 → 取证(含最后心跳内存样本)→ 追加 exit-diag     │
│   → WARNING 日志 → 哨兵改写为 phase=running(新生命认领)     │
├────────────────────────────────────────────────────────────┤
│ 运行:shutdown_watchdog 30s 心跳嵌入内存样本(<1ms /proc)     │
│   = 死亡前最近的遥测快照(OOM 崩溃循环从体积即可分类)         │
├────────────────────────────────────────────────────────────┤
│ 退出:mark_exited()(每个干净退出路径,含两个 watchdog os._exit)│
│   哨兵 phase=exited + exit_code + reason                     │
└────────────────────────────────────────────────────────────┘
```

**与 delivery ledger 的差异**:delivery ledger 记"欠平台的响应";lifecycle ledger 记"上一生命怎么死的"。同为"崩溃后可恢复的持久真相",但读的是哨兵文件而非 SQLite。

---

## 二、设计 1:哨兵状态机(running/exited + 不洁检测)

**位置**:`gateway/lifecycle_ledger.py:16-27`(模块注释)+ `181-221`(detect_unclean_exit)+ `224-277`(record_startup)+ `280-310`(mark_exited)

```
状态机:
  phase=running   → 启动时认领(record_startup)
  phase=exited    → 每个干净退出路径(mark_exited)
  下一 boot 读哨兵:phase=="running" → 上一生命从未走退出路径 = 不洁死亡

detect_unclean_exit(只读,不改写):
  1. 哨兵 phase != "running" → None(干净)
  2. _pid_alive_with_start_time(pid, start_time) → 活 → None
     (活主 = --replace 计划交接在飞,不是死亡——防误报)
  3. 死主 → 取证 dict:prior_pid/prior_started_at/prior_start_time

record_startup:
  detect → 有取证 → 写 exit-diag(JSON 行,tag=gateway.previous_unclean_exit)
    + WARNING(pid/started_at/last_heartbeat_at/last_mem/suspected_oom)
  → 认领新生命哨兵(phase=running + 本进程 pid/start_time/started_at)
  → 取证判定前传:prior_unclean_exit / prior_suspected_oom(NS-656,见设计 5)

mark_exited(幂等,绝不抛):
  sentinel.pid != os.getpid() → return(所有权守卫,见设计 4)
  → phase=exited + exit_code + exit_reason + exited_at
```

**正确性价值**:
1. "干净退出 = 走退出路径"是判定基础——exit 路径覆盖越全,不洁检测越准
2. 活主豁免:--replace 交接窗口内新 boot 不误报
3. 只读 detect 与改写 record 分离
4. **死因区分(哨兵预写)**:watchdog 两个 os._exit 之前先 mark_exited(reason="loop_liveness_watchdog"/"shutdown_watchdog",shutdown_watchdog.py:193/419)——下一 boot 报"watchdog 硬退出"而非误判 SIGKILL/OOM;"不洁死亡"因此精确 = 没有任何 exit 路径跑过

**产品④映射**:知识库进程的生命周期哨兵——"上次关闭是干净的吗"是崩溃恢复的第一问。

## 设计 2:心跳内存采样(死亡前遥测快照)

**位置**:`lifecycle_ledger.py:77-110`(sample_memory)+ `shutdown_watchdog.py:250-262`(嵌入心跳)

```
sample_memory(纯 /proc 读,Linux-only,永不抛,<1ms):
  /proc/self/status → VmRSS(rss_kib)
  /proc/meminfo → MemTotal/MemAvailable/SwapTotal-SwapFree(swap_used_kib)

嵌入:shutdown_watchdog 30s 心跳 payload["mem"] = sample
  → 不洁死亡报告带"死亡前 N 秒的内存可用量"
  → OOM 崩溃循环从体积即可分类(无 Prometheus 保留竞态)
```

**正确性价值**:心跳是"循环最后证明活着的时刻"——其内存样本是死亡最近的遥测;不洁死亡没有 handler 可跑,心跳是唯一幸存记录。

**产品④映射**:知识库守护进程的心跳必须带环境遥测(内存/CPU)——崩溃归因的第一证据。

## 设计 3:OOM 怀疑启发(保守标注,不判定)

**位置**:`lifecycle_ledger.py:54-58`(阈值)+ `205-220`(enrich)

```
启发(故意保守——只注释报告,分类留给人类):
  avail < 64 MiB(_LOW_MEM_AVAILABLE_KIB)或
  avail/MemTotal < 5%(_LOW_MEM_AVAILABLE_FRACTION)
  → evidence["suspected_oom"] = True

测试(test_record_startup_carries_unclean_flags_onto_new_sentinel):
  心跳 mem_total=1GiB, mem_available=20KiB → suspected_oom=True
  → 哨兵前传 prior_unclean_exit + prior_suspected_oom
```

**正确性价值**:"注释"而非"判定"——启发只加提示,分类是人的决定;阈值双条件(绝对 + 相对)防单指标误判。

**产品④映射**:知识库崩溃归因的启发标注——保守提示,不自动判定(与 GoalGate 的"门不判定"同哲学)。

## 设计 4:所有权守卫(mark_exited 不覆盖接管者)

**位置**:`lifecycle_ledger.py:287-298`(mark_exited 守卫)+ `145-178`(_pid_alive_with_start_time)

```
mark_exited 守卫:sentinel.pid != os.getpid() → return
  原因:--replace 时替换者在旧进程完成 teardown 前认领哨兵,
       旧生命退出时不得把新所有者的 running 哨兵改成 exited
  pid=None 或畸形 → 所有权未知 → 同样不动(不能把无法证明是自己的证据覆盖成 clean)

_pid_alive_with_start_time(活主判定):
  - 不用 os.kill(pid,0):Windows 上 = 给目标控制台组发 CTRL_C_EVENT(bpo-14484)!
    用 gateway.status._pid_exists(psutil-backed,跨平台):
    psutil 主路径(Windows 内部 OpenProcess + WaitForSingleObject——WAIT_TIMEOUT=仍在运行;
    ERROR_ACCESS_DENIED=存在但属他人=活;ERROR_INVALID_PARAMETER=已消失=死)
    + psutil 缺失时 ctypes OpenProcess/WaitForSingleObject 回退(POSIX 才用 os.kill(pid,0))
  - **zombie 判定**(#42126):psutil.pid_exists 对僵尸返回 True 但实际已死
    (SIGKILL 无效、不能是运行中的 gateway)→ 报告为死,否则 --replace 等旧 PID 死
    (它直到父进程 reap 都不死)→ systemd Restart=always 下静默崩溃循环
  - start_time ±2s 匹配才活(pid 复用防护)
  - start_time=None → 视为活(不能区分 pid 复用,宁可错在"活"边)
```

**正确性价值**:
1. 所有权守卫防 --replace 双写竞态
2. os.kill(pid,0) 在 Windows 的杀伤性——"活着吗"的探测必须用非杀伤 API(bpo-14484 教训)
3. 不确定 → 保守视为活(与 delivery ledger _owner_alive 同哲学)

**产品④映射**:哨兵/锁的所有权校验——"只有能证明是我的才改写";跨平台活性探测绝不使用杀伤 API。

## 设计 5:判定前传(哨兵携带上一生命结论,NS-656)

**位置**:`lifecycle_ledger.py:256-274`(record_startup 认领)+ 注释

```
认领时携带判定:
  claim["prior_unclean_exit"] = True
  claim["prior_suspected_oom"] = True(如怀疑)

原因:exit-diag 日志是给人读的追加散文;哨兵是判定唯一机器可读存活形式
     ——/api/status 读哨兵告诉用户"你的 agent 在(疑似)内存耗尽后重启"

作用域:仅本生命——下次干净退出/boot 改写哨兵,标志随之老化
测试(test_record_startup_clean_boot_has_no_prior_flags):
  干净退出后 boot → 无 prior_* 标志
```

**正确性价值**:判定跨生命存活(机器可读),但不永久——只带到下一生命,避免陈旧结论永存。

**产品④映射**:知识库"上次运行结论"前传机制——崩溃归因随会话带出,但随新会话老化。

## 设计 6:原子写(哨兵永不全写)

**位置**:`utils.py:346-380`(atomic_json_write)+ lifecycle_ledger.py:121-129(_write_sentinel)

```
atomic_json_write:temp file + json.dump + flush + os.fsync + os.replace
  → 目标文件永不处于半写状态;写中崩溃 → 旧版本完好
  → 哨兵是状态机真相,半写 = 读到垃圾 = 误判
  → 另:atomic_replace 处理跨设备/bind-mount 重命名 + 符号链接原地替换(GitHub #16743)

_write_sentinel:mkdir parent + atomic_json_write(indent=None)+ try/except(尽力而为)
```

**正确性价值**:哨兵本身就是取证对象——写它时必须原子,否则取证工具损坏自己的证据。

**产品④映射**:知识库真相文件的原子写(与 hq1 记忆原子写同族——temp+replace 是通用纪律)。

## 设计 7:尽力而为 + 失败绝不干预生命周期

**位置**:`lifecycle_ledger.py:35-36`(模块原则)+ 各处 try/except

```
- 取证失败绝不影响被观察的生命周期(record_startup/mark_exited 全 try/except)
- 采样失败 → {} 不炸心跳
- 哨兵读失败 → 视为无哨兵(不误报)
- read_prior_exit_label(容器启动标注):clean/unclean/unknown 一词摘要,异常安全
  ——容器 boot 时旧 PID namespace 已消失,任何 running 哨兵 = 不洁(设计特化)
```

**产品④映射**:观测/取证层绝不干预被观测对象(与 delivery ledger"主数据优先"同哲学)。

---

## 三、与四项目对比(崩溃归因)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes lifecycle ledger |
|------|----|----------|----------|-----|------------------------|
| 干净退出记录 | — | 意图先持久化 | 事务提交 | shutdown_flush 冲刷 | **哨兵 phase=exited + reason** |
| 不洁死亡检测 | 恢复三态(0/1/2) | — | — | — | **phase=running + 死主 → 不洁** |
| 死亡前遥测 | — | — | — | — | **心跳嵌入内存样本(<1ms /proc)** |
| OOM 归因 | — | — | — | — | **保守启发(双阈值)+ 前传标志** |
| 所有权 | Fencing/seq | — | owner 栅栏 | — | **mark_exited pid 守卫 + start_time ±2s** |
| 原子写 | — | — | — | WriteBehind | **temp+fsync+os.replace** |
| 活性探测 | — | — | — | — | **_pid_exists(psutil),非 os.kill(Windows 杀伤)** |

**结论**:产品"崩溃归因"参考 = Hermes lifecycle ledger 全案(delivery ledger 的 owner 语义 + Pi 恢复三态 + dsh shutdown_flush 同族)。**与 delivery ledger 配对:一个管"欠的响应",一个管"死的进程"——崩溃恢复的两面**。

---

## 四、面试弹药

1. **"不洁死亡没有任何 handler 跑"**:SIGKILL/OOM/VM 死 = 取证只能在下次 boot 做——哨兵是跨生命真相
2. **"os.kill(pid,0) 在 Windows 是杀伤"**:bpo-14484——sig=0 与 CTRL_C_EVENT 在 C 层撞值,发 Ctrl+C 给整个控制台组;活性探测必须用 psutil/OpenProcess
3. **"心跳 = 死亡前遥测"**:30s 心跳嵌内存样本,不洁死亡报告带"死亡前 N 秒内存可用量"——OOM 循环从体积可分类
4. **"mark_exited 所有权守卫"**:--replace 时替换者先认领,旧生命退出不得把新所有者的 running 改成 exited;pid=None 未知所有权同样不动
5. **"启发注释不判定"**:OOM 怀疑双阈值(64MiB + 5%)只加提示,分类留给人——保守是取证的纪律
6. **"判定前传但随生命老化"**:prior_unclean_exit 只带到下一生命,干净退出后消失——机器可读但不过期残留

---

## 五、产品映射汇总

| 设计 | 产品④用法 |
|------|---------|
| 哨兵状态机 | 知识库进程"上次关闭干净吗"第一问 |
| 心跳内存采样 | 守护进程心跳带环境遥测 |
| OOM 保守启发 | 崩溃归因注释不判定(双阈值) |
| 所有权守卫 | 哨兵/锁只有能证明是我的才改写 |
| 判定前传 + 老化 | 崩溃结论跨生命带出,随新生命老化 |
| 原子写 | 取证对象自己必须原子写 |
| 尽力而为 | 取证层绝不干预被观测对象 |
| _pid_exists 非 os.kill | 跨平台活性探测纪律(bpo-14484) |

> 覆盖设计数:7(设计 1-7)
> 测试契约:test_lifecycle_ledger.py(9 用例:内存采样/首启认领/干净周期/死主不洁/取证持久+认领/标志前传/干净无标志/pid=None 所有权守卫/损坏哨兵标签)+ test_memory_status.py(关联)
> 调用点:run.py:28705(启动,在 PID 文件认领后——只有权威 gateway 碰哨兵)/run.py:29046(优雅退出单漏斗 #53107)/shutdown_watchdog.py:193(loop_liveness_watchdog)/419(shutdown_watchdog)两个 os._exit 前
