# q13 — Guard/Goal/Jobs/Schedule(深度版:循环辅助域)

> 域:②执行引擎(辅助) | 文件:packages/(guard/timeout-policy + repeat-tool-reminder)+(goal/goal + command-goal + goal-round-driver + tool-goal)+(jobs/jobs + jobs-local + tool-jobs)+ schedule/schedule(domain/runtime/transaction/persistence/tools)
> review 轮次:2 轮(源码全文核心)

---

## 假设

循环辅助域四件套:Guard(超时/重复提醒)、Goal(同会话目标)、Jobs(后台任务)、Schedule(调度)。全部是"挂事件不碰循环"的插件(guard 挂 tools/execute 瀑布,goal 用 goal/change 事件)。

## 验证

### 1. Timeout(设计 1:tools/execute 包装)

```ts
// guard/timeout-policy/src/index.ts:25-85:
TOOL_TIMEOUT 常量;name = 'timeout-policy';inject = ['tools']
apply:ctx.on('tools/execute', (exec, next) => {
  timeoutMs = ctx.tools.get(exec.name, exec.agent)?.timeoutMs——工具声明预算
  无预算 → next() 原样委托(无期限)
  有预算 → deadline(exec.signal, timeoutMs, TOOL_TIMEOUT)
    // 派生期限换到 exec.signal 分派,然后恢复调用方 signal
    // ——post-execute 监听器永不见本插件的(可能已 abort 的)超时 signal
  result = await next()
  // 我们的计时器触发(代码作用域——嵌套外部期限在此读 undefined)
  //   → 工具已见 abort 并达静止;用结构化 TOOL_TIMEOUT 替换它返回的(自身 abort 结果)
  if (timeoutOf(d.signal, TOOL_TIMEOUT)) return toolTimeoutResult(timeoutMs)
  return result
  finally: exec.signal = upstream(恢复)
})
```

**设计要点**:超时 = execute 瀑布包装(不是循环逻辑);"替换它返回的自身 abort 结果"——模型看到结构化 TOOL_TIMEOUT 而非工具的 abort 结果;signal 恢复保证 post-execute 干净。

### 2. RepeatReminder(设计 2:advisory 重复检测)

```ts
// guard/repeat-tool-reminder/src/index.ts:
"Advisory per-agent repeat-call detector:enriches post-execute decisions with logged
 model context without vetoing or rewriting calls"
Config:thresholds(默认 [3,5,8])/include/exclude(*-wildcard 工具名谓词,调用时匹配)
  ——exclude: [mcp_*] 在无 MCP 部署也必须合法(模式不引用注册表)
"misconfiguration fails loud":空 thresholds/非整数/<2/重复 → 插件加载时 throw(绝不静默回退)
DETAILED reminder 参数引文上限(默认 500)——链 key 始终比较完整 canonical 串
```

**产品启示**:③验收器可借鉴——"重复调用提醒"(连续 3/5/8 次)是收敛性检测的现成样本(产品 7 卡点 #24 收敛性)。

### 3. Goal(设计 3:同会话目标)

```ts
// goal/goal/src/index.ts:96-97,546-557:
applyGoalProjection(state, event):event.type !== 'goal/change' → 返回 state(投影规则)
'goal/change' 事件(session durable)+ 'goal/changed'(live agent 通知)
// goal-round-driver:轮驱动;command-goal:命令;tool-goal:模型工具
// architecture.md:125:"Manage a same-session objective:use ctx.goals;continue through agent/*"
```

### 4. Jobs + Schedule(设计 4:后台/调度)

```ts
// jobs/:ctx.jobs + jobs-local + tool-jobs(收集/停止)
// schedule/:domain/runtime/transaction/persistence/tools——事务化调度(transaction.ts)
//   inject = ['agents','sessions','tools','sessionPersistence']
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 超时 = execute 瀑布包装 + TOOL_TIMEOUT 结构化结果 | timeout-policy | ②失败语义 |
| 2 | 重复提醒(3/5/8 阈值 + advisory) | repeat-tool-reminder | ③收敛性检测 |
| 3 | Goal(goal/change 事件 + 投影) | goal/goal | ②目标跟踪 |
| 4 | Jobs/Schedule(后台 + 事务化调度) | jobs + schedule | ②后台 |

## 面试弹药

- "超时替换工具自身 abort 结果":模型看到结构化 TOOL_TIMEOUT,不是工具的 abort——失败归一化
- "signal 恢复":post-execute 监听器永不见超时插件的 signal——插件不泄漏
- "重复提醒是 advisory":丰富 post-execute 决策,不 veto 不改写——收敛性提示而非强制
- "misconfiguration fails loud":阈值非法 → 加载时 throw,绝不静默回退

## 待深挖

- [ ] goal-round-driver 的轮驱动语义
- [ ] schedule 的 transaction 实现
- [ ] jobs-local 的后台执行
