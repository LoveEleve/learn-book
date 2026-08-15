# 闭环笔记 Q1:AgentSession 状态机 — 对齐→执行的全生命周期

> 域:AgentSession 状态机(core/agent-session.ts,3344 行)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

AgentSession 是 Pi 的核心状态机,承载"用户输入 → agent 执行 → 上下文管理"的完整生命周期。它的消息队列机制(steer/followUp)和压缩重建机制是产品最值得抄的设计。

## 验证过程

### 1. 定位:AgentSession 是什么

`agent-session.ts:1-14` 注释明确:
> "Core abstraction for agent lifecycle and session management. This class is shared between all run modes (interactive, print, rpc)."

**结论**:所有运行模式(交互/打印/RPC)共享这一个类,模式只加自己的 I/O 层。**"一个核心状态机 + 多个 I/O 壳"——产品执行引擎的架构模板。**

### 2. 消息队列机制(steer/followUp)——产品①对齐模块直接参考

`agent-session.ts:1343-1408` 验证:

```
steer(text)   → _queueSteer → push 到 _steeringMessages + agent.steer()
followUp(text) → _queueFollowUp → push 到 _followUpMessages + agent.followUp()
```

**关键语义差异**(来自注释 1356-1362):
- **steer**:干预当前执行(agent 还在跑就插入)
- **followUp**:当前执行结束后投递("Delivered only when agent has no more tool calls or steering messages")

**设计价值**:对齐模块需要"边执行边调整方向"的能力——steer = 纠正方向,followUp = 排队后续任务。产品①的对齐交互可以直接用这个双队列模型。

### 3. 驱动循环(_runAgentPrompt)——产品②执行引擎骨架

`agent-session.ts:1063-1075`:

```ts
private async _runAgentPrompt(messages) {
    this._isAgentRunActive = true;
    try {
        await this.agent.prompt(messages);
        while (await this._handlePostAgentRun()) {
            await this.agent.continue();   // ← 关键:返回 true 就继续跑
        }
    } finally { ... }
}
```

`_handlePostAgentRun`(1077-1105)决定是否继续,四种情况返回 true:
1. **重试**(_isRetryableError + _prepareRetry)→ 1084
2. **压缩**(_checkCompaction)→ 1098
3. **队列未清空**(agent.hasQueuedMessages)→ 1104
4. agent_end 扩展处理程序排队的消息

**设计价值**:这就是"agent 自主连续运行"的驱动机制——循环直到无事可做。**产品"让 agent 一直跑"的执行引擎骨架,就是这个 while 循环 + 判定函数。**

### 4. 压缩重建流程(compact)——产品②上下文管理核心

`agent-session.ts:1790-1899`:

```
compact() 流程:
1. abort() 暂停当前执行
2. prepareCompaction(pathEntries, settings) — 检查能否压缩(1805)
3. 扩展可接管压缩(session_before_compact,1818)
4. LLM 生成摘要(compact(),1854)
5. appendCompaction 持久化(1878)
6. buildSessionContext() 重建上下文(1880)
7. agent.state.messages = sessionContext.messages — 替换内存(1881)
8. 发 session_compact 事件(1890)
```

**关键设计**:压缩不是"删消息",而是**"摘要条目 + 后续条目重建上下文"**。压缩后 `agent.state.messages` 被替换——LLM 看到的是"摘要 + 新消息",历史细节进 SQLite。

**设计价值**:这就是"书级知识库"的核心操作——**章节完成 → 压缩成摘要条目 → 后续执行基于摘要继续**。产品④直接抄这个流程。

### 5. 触发压缩的三种时机

`agent-session.ts:154` 事件定义:
```
compaction_start: reason: "manual" | "threshold" | "overflow"
```
- manual:用户手动 /compact
- threshold:阈值触发(_checkCompaction,1098)
- overflow:上下文溢出恢复(_runAutoCompaction,2058)

**设计价值**:三种时机 = 产品自动验收器触发"章节压缩"的三种策略。

### 6. 压缩判定引擎 _checkCompaction(1962-2053)——review 新增,原笔记低估

这是一个远比"三种时机"复杂的判定函数,包含 **5 个保护性设计决策**:

```
_checkCompaction 执行顺序:
1. 开关检查(1963-1964):settings.enabled 关闭 → 不压缩
2. 中止跳过(1967):用户取消的响应 → 不压缩
3. 模型一致性(1971-1976):响应来自"旧模型"(用户已切换模型)→ 不触发
   ——防:从 opus(小窗)切到 codex(大窗),旧模型的溢出错误不该触发新模型压缩
4. 压缩边界保护(1981-1986):响应早于最近一次压缩 → 不触发
   ——防:压缩前遗留的 usage/error 在压缩后第一条 prompt 重复触发压缩
5. Case 1 溢出/可恢复失败(1988-2022):
   - 可恢复长度判定:输出低于模型期望上限 → 视为可恢复
   - overflow 恢复失败保护(2001-2012):已尝试过 1 次 compact+retry → 放弃并报错
   - 溢出消息从 agent 内存移除但保留 session 历史(2017-2020)
6. Case 2 阈值(2024-2052):
   - usage 缺失/错误时用估算(estimateContextTokens)
   - 估算来源必须晚于压缩边界(2038-2044)——防旧 usage 误触发
```

**核心设计洞察**:
- **上下文管理不是"满了就压",而是一系列防误触发保护**:模型切换、压缩边界、旧 usage 污染、恢复失败上限——每个都是真实场景踩坑后的防御
- **溢出恢复的"失败上限"设计(2001)**:压缩+重试只尝试一次,失败即放弃——**防死循环**。这正是我们 #24 收敛性问题的代码级答案:agent 不无限自愈,有明确的重试上限

### 7. overflow 恢复的消息处理(2015-2020)——review 新增

```
溢出响应处理:
1. 从 agent.state.messages 移除最后的 assistant 消息(2017-2019)
   ——内存中移除(agent 不再看到失败响应)
2. 但保留在 session 历史中(sessionManager 持久层)
   ——历史不丢,只是不进上下文
```
**"内存移除 + 历史保留"双态设计**——产品④知识库的"错误结论处理"参考:失败的中间结果可以不进上下文,但痕迹保留可审计。

### 8. 事件流水线 _handleAgentEvent(610-)— review 第二轮新增

`agent-session.ts:610` 起,每个 agent 事件经过 4 段流水线:

```
1. 队列去重(610-632):user 消息开始时,从 steering/followUp 队列移除
   ——UI 看到的队列状态与消息发送保持同步
2. 扩展事件(636):先发扩展处理
3. 监听器通知(638-640):agent_end 时附加 willRetry
4. 持久化(644-670):message_end 时按角色分流:
   - custom → appendCustomMessageEntry
   - user/assistant/toolResult → appendMessage
   - bashExecution/compactionSummary/branchSummary → 别处持久化
```

**关键设计**:
- **持久化挂在事件流水线里,不是显式调用**——只要 agent 发事件,状态就自动落库。**产品④知识库的"自动记录"机制直接抄**:监听事件 → 自动持久化,无需每个操作显式保存。
- **overflow 恢复标记在事件里重置**(message_start 时置 false,成功 assistant 时置 false)——恢复上限与消息流解耦。

### 9. 认证与请求组装(409-469)— review 第二轮新增

`_getRequiredRequestAuth`(409)/`_getSummarizationRequestAuth`(446):
- 正常请求:认证失败直接抛错(带 OAuth/API key 引导)
- 压缩/摘要请求:认证失败**静默降级**(catch 后返回 {model})——摘要不因认证失败而阻塞
**"关键路径强校验 + 辅助路径弱校验"双标准**——产品"验收器请求"也应容忍认证失败。

### 10. 工具钩子 _installAgentToolHooks(479-)— review 第二轮新增

`beforeToolCall`/`beforeToolResult` 拦截工具调用/结果:
- 在 Agent 实例上装一次,扩展重载不用重装(注释 474-477)
- **工具调用拦截 = 产品③自动验收器的挂载点**:验收器在工具结果落地前检查("这个工具结果要不要写进知识库?")

### 11. reload 热重载(2610-2635)— review 第二轮新增

```
reload 流程:扩展关闭 → settings 重载 → 队列模式同步 → API 重置
           → 资源重载 → 运行时重建 → 扩展绑定重发 session_start
```
**会话级热重载,不丢状态**——产品"配置变更后继续分析"参考。

### 12. retry 策略分层(2645-2679)— review 第二轮新增

- `_isRetryableError`(2645):**overflow 不重试(走压缩),其余错误(过载/限流/5xx)才重试**
- 压缩与摘要共享同一 retry 预算/退避(2653 注释)——单一 transient 断流不弄死整个操作
- **错误分层:可恢复(重试)vs 结构性(压缩)vs 致命(放弃)** ——产品执行引擎的错误处理骨架

### 13. abort 语义(1550-1554)— review 第二轮新增

`abort()` = abortRetry + agent.abort + waitForIdle——**abort 是异步等待完全停止**,不是立即打断。
产品"用户打断后必须等真正停止才能继续"。

## 代码类型

Implementation(状态机实现)+ Interface(与 agent-core 的契约)

## 跨域关联

- ← 依赖:agent-core 的 Agent(循环)、session-manager(持久化)、compaction/(压缩)、settings-manager(配置)
- → 被依赖:modes/(interactive/print/rpc 共享此状态机)

## 结论

AgentSession = **"对齐→执行→压缩→继续→恢复"的完整状态机**,核心可抄设计 13 个:
1. **单状态机多 I/O 壳**(modes 共享)→ 产品执行引擎架构
2. **steer/followUp 双队列**(执行中干预/结束后投递)→ 产品①对齐交互
3. **while(_handlePostAgentRun) 自主续跑循环**(重试/压缩/队列)→ 产品②"一直跑"骨架
4. **压缩 = 摘要条目重建上下文**(appendCompaction → buildSessionContext)→ 产品④知识库核心操作
5. **压缩判定 5 重保护**(模型一致性/压缩边界/旧 usage 污染/失败上限)→ 产品②上下文管理防御层
6. **溢出恢复失败上限**(compact+retry 仅一次)→ 防死循环,收敛性代码级答案
7. **内存移除+历史保留双态**(溢出消息)→ 产品④错误结论审计
8. **事件流水线持久化**(message_end 自动落库)→ 产品④"自动记录"机制
9. **关键路径强校验+辅助路径弱校验**(压缩认证失败静默)→ 产品验收器请求设计
10. **工具钩子拦截**(beforeToolCall)→ 产品③验收器挂载点
11. **reload 热重载不丢状态** → 产品配置变更继续分析
12. **错误三层分层**(可恢复/结构性/致命)→ 执行引擎错误处理骨架
13. **abort 异步等待完全停止** → 用户打断语义

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| steer/followUp 双队列 | ✅ 抄 | 对齐模块"边执行边纠正" |
| while+判定 自主续跑 | ✅ 抄 | 执行引擎"一直跑直到完成" |
| 压缩重建上下文 | ✅ 抄 | 书级知识库"章节摘要化" |
| 压缩三时机(manual/threshold/overflow) | ✅ 抄 | 验收器触发策略 |
| 压缩判定 5 重保护 | ✅ 抄 | 防误触发(模型切换/边界/污染) |
| 溢出恢复失败上限 | ✅ 抄 | 防死循环 |
| 内存移除+历史保留 | ✅ 抄 | 失败结论不进上下文但可审计 |
| 事件流水线持久化 | ✅ 抄 | 知识库自动记录 |
| 工具钩子拦截 | ✅ 抄 | 验收器挂载点 |
| 错误三层分层 | ✅ 抄 | 执行引擎错误处理 |
| 扩展可接管压缩(session_before_compact) | ⚠️ 改 | 换成"验收器可接管" |
| 单状态机多壳 | ✅ 抄 | 产品一个核心多个 UI |

## 面试问答弹药

- **Q**:agent 怎么做到"一直跑"?→ A:while(_handlePostAgentRun){agent.continue()},判定含重试/压缩/队列三因素
- **Q**:上下文满了怎么办?→ A:压缩成摘要条目重建上下文,不是删消息,历史进持久层
- **Q**:执行中用户插话?→ A:steer(立即干预)vs followUp(结束投递)双队列
- **Q**:上下文溢出怎么恢复?→ A:移除失败响应(内存)+ 压缩 + 重试,但只尝试一次,失败即放弃防死循环
- **Q**:模型切换后会不会误触发压缩?→ A:有模型一致性检查,旧模型溢出不触发新模型压缩
- **Q**:错误怎么处理?→ A:三层分层——overflow 走压缩不重试,可恢复错误(限流/5xx)走重试,致命错误放弃
- **Q**:状态怎么自动持久化?→ A:事件流水线挂持久化,message_end 自动落库,无需显式保存
