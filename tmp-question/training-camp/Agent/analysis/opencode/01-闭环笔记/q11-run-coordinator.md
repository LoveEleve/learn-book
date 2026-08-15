# q11 — SessionRunCoordinator(深度版:每 key 串行 + wake 合并 + 15 测试契约)

> 域:②执行(调度) | 文件:core/src/session/run-coordinator.ts(104)+ execution.ts(34)+ execution/local.ts(46)+ core/test/session-run-coordinator.test.ts(418,15 契约)
> review 轮次:2 轮(源码全文 + 全部测试契约)

---

## 假设

RunCoordinator = 进程内"每 key 串行、跨 key 并行"的调度器:drain(force) 是唯一的执行入口;run 加入/唤醒合并/interrupt 幂等;失败也产生确定性的 successor 语义。它是 EventV2 的 sliding-1 合并思想在"执行"层面的应用。

## 验证

### 1. 核心状态(设计 1:Entry 四字段)

```ts
// run-coordinator.ts:17-22
Entry = { done: Deferred, owner?: Fiber, pendingWake: boolean, stopping: boolean }
active = Map<Key, Entry>
// fork = FiberSet.makeRuntime(scope 关闭 → 全部中断,测试 122 "cleans active executions when its scope closes")
```

### 2. run(设计 2:加入 vs 启动)

```ts
// run-coordinator.ts:67-79
run(key):
- active 有 entry:stopping → await done 后递归 run(等清理完再跑);否则 await done(加入)
- 无 entry → 创建 + start(force=true)+ await
```

**测试证据**:
- 9 "joins concurrent resumes for one key":2 并发 resume → runs=1
- 351 "does not cancel execution when a joined waiter is interrupted":等待者中断不影响执行

### 3. wake(设计 3:合并 + 非强制)

```ts
// run-coordinator.ts:81-92
wake(key):
- active 有 entry → pendingWake = true(合并,不启动新执行)
- 无 entry → 创建 + start(force=false)
```

**测试证据**:
- 141 "coalesces wakes received during active execution"
- 173 "runs again when woken during the follow-up"(settle 时 pendingWake → successor)
- 57 "starts execution when woken while idle"
- 31 "joins a wake-started execution without forcing a successor"(forces=[false])

### 4. settle(设计 4:successor 语义)

```ts
// run-coordinator.ts:51-65
settle(key, entry, exit):
- 成功 且 !stopping 且 pendingWake → 同 entry 重新 start(successor=true,不强制)
- 否则:pendingWake → 新 entry + start(successor=true);无 → 删除 active
- Deferred.doneUnsafe(entry.done, exit)(唤醒所有等待者)
```

**测试证据**:
- 321 "starts one follow-up when a wake races with failure"(失败 + wake 竞争 → 恰好一个 follow-up)

### 5. interrupt(设计 5:幂等 + 清 wake)

```ts
// run-coordinator.ts:94-101
interrupt(key):无 owner → void(幂等);有 → stopping=true + pendingWake=false + Fiber.interrupt
// 中断清理期间:settle 检查 stopping → 不启动 successor;但清理后新 wake/resume 正常注册
```

**测试证据**:
- 209 "does nothing when interrupted while idle"
- 218 "interrupts active execution and clears its pending wake"
- 247 "runs a wake registered during interruption cleanup"
- 285 "starts a resume registered during interruption cleanup"

### 6. trampoline(设计 6:同步自唤醒防栈溢出)

**测试证据**:
- 395 "trampolines synchronous self-waking execution":successor 用 Effect.yieldNow 起步(run-coordinator.ts:40)——自唤醒不递归爆栈

### 7. Execution 路由(设计 7:Session-ID → Location runner)

```ts
// execution/local.ts:16-28
drain(sessionID, force):
  store.get(sessionID) → SessionRunner.Service.use(run) 
  → Effect.provide(locations.get(session.location))  ← Location 服务注入
  → 非中断失败 logError(中断静默)
// noopLayer(execution.ts:26-34):active=空/resume/wake/interrupt=void —— "只记录不执行"的低配层
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Entry 四字段 + Map + FiberSet(scope 关闭全中断) | run-coordinator.ts:17-29 | ②执行生命周期 |
| 2 | run 加入/启动(stopping 时递归等) | run-coordinator.ts:67-79 | ②并发控制 |
| 3 | wake 合并(执行中只标 pendingWake) | run-coordinator.ts:81-92 | ②唤醒去重 |
| 4 | settle successor 语义(成功+pend/失败+pend 都恰好一个 follow-up) | run-coordinator.ts:51-65 | ②失败循环(D17) |
| 5 | interrupt 幂等 + 清 wake + 清理期新注册可运行 | run-coordinator.ts:94-101 | ②用户打断 |
| 6 | trampoline(yieldNow 起步防栈溢出) | run-coordinator.ts:40 | ②自唤醒安全 |
| 7 | Session-ID → Location 服务注入 + noopLayer | execution/local.ts:16-28 | ②路由抽象 |

## 面试弹药

- "每 key 串行,跨 key 并行":Map<Key, Entry>,同 session 的 resume 合并成一次 drain(测试 runs=1)
- "wake 合并 = 执行中只打标记":pendingWake 布尔,settle 时决定 successor——不启动新 fiber,不丢唤醒
- "失败 + wake 竞争 → 恰好一个 follow-up":settle 统一处理,竞态不会产生两个后继
- "interrupt 三件事":stopping=true(不启动 successor)+ pendingWake=false(清合并)+ Fiber.interrupt
- "trampoline 防爆栈":successor 用 yieldNow 起步——同步自唤醒不递归
- "等待者中断不影响执行":join 的中断不会级联到 owner fiber

## 待深挖

- [ ] noopLayer 的使用场景(哪些部署用它)
- [ ] SessionExecution 在 opencode 应用层的接线
