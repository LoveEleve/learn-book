# 闭环笔记 RQ4:ContextManager — 产品②上下文管理(缓存优先)

> 域:internal/agent/(context_manager.go 217 行 + compact.go 678 行 + compact_projection.go 582 行 + compact_commit.go 82 行 + context_usage.go 54 行 + context_status.go 89 行)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

Reasonix 的上下文管理以"缓存优先"为最高原则:唯一自动阈值(compact_ratio),低于阈值零操作,到阈值单次摘要事务。这是产品②上下文管理的核心参考。

## 验证过程

### 1. ContextManager 定位(context_manager.go:18-22)— 唯一所有者

> "ContextManager is the sole owner of provider-visible context maintenance. Canonical session messages are immutable inputs; Prepare evolves only the durable projection and returns the exact visible view for one sampling round."

**设计要点**:
- **唯一所有者**:上下文维护只有一个入口
- **canonical 消息不可变**:原始会话永不改
- **Prepare 只演进 durable projection**:可变的是投影,不是原始日志
- 返回"exact visible view for one sampling round"

### 2. 触发决策流(prepareOnce, context_manager.go:58-121)— 低于阈值零操作

```
1. est = 可见请求 token 估算(含消息+工具+角色投影)
2. 检查维护阻塞(contextMaintenanceBlocked):
   阻塞 + 非手动 → overflow 或超硬上限 → ErrCompactionRequired(必须释放)
   否则 → 返回现状(不操作)
3. est < fold(触发阈值)→ 清零 consecutive/stuck → 返回(零操作!)
4. stuck + pressure → 返回(压力重试无意义)
5. forceFold = 手动/overflow/超硬上限
6. est < fold 且非 forceFold → 返回(零操作!)
7. 否则 → foldContext(单次摘要事务)
```

**关键**:**低于阈值时绝对零操作**——不重写/不剪枝/不写 sidecar/不 bump 版本(注释:任何重写都会从该点杀 prompt 缓存)。

### 3. 阈值体系(compact.go:91-153)— 量化预算

```
compactTrigger  = window × compact_ratio(默认 0.85,范围 0.65-0.85)
hardInputCeiling = window - protocolReserveTokens(物理安全边界,非用户阈值)
recentTailBudget = clamp(window×10%, 32K, 96K)(最近逐字尾巴预算)
checkpointCeiling = window × 50%(摘要接受上限,不加 padding)
exceptionalMinimumSavings = window × 比例(固定前缀超 50% 时额外节省要求)
foldEconomics:region ≥ 400 token 才值得摘要调用
```

**多层阈值**:**触发阈值(0.85)** 控制"何时压缩";**接受上限(50%)** 控制"摘要多大";**硬上限** 控制"物理安全";**经济性(400 token)** 控制"值不值得调 API"。

### 4. 摘要 system prompt(compact.go:60-85)— 产品④章节摘要完整模板

```
7 个固定标题(省略无内容的标题):
## Standing facts & constraints — 用户声明的事实/约束,原话,详尽
   "this is the durable contract, so prefer over- to under-including"
## Goal — 请求与意图
## Decisions & rationale — 关键决策+理由(防重新争论/推翻)
## Files & code — 文件+签名/行号/数据形状/精确修改
   "this is what lets the agent act without re-reading everything"
## Commands & outcomes — 命令+结果+错误文本
## Errors & fixes — 踩过的坑+解决(防重复走死路)
## Pending & next step — 未完成+最具体的下一步

规则:terse(bullet 非 prose);标识符/路径/数字精确保留;
不编造——不知道就省略
```

**产品价值**:这是产品④"章节摘要"的完整模板:
- Standing facts = 用户的约束契约(对应你的"边界排除")
- Decisions = 分析结论+理由
- Files & code = file:line 证据
- Errors & fixes = 踩坑记录(你的 #22 探索路径!)
- Pending & next step = 交接自动化(#21)

### 5. token 估算(compact.go:164-200)— 精确+兜底双轨

```
estimateMessagesTokens:逐消息累加
  4(框架开销)+ 文本 + 推理 + 名称 + tool_call(8+id+name+args)+ 响应项 + server search
estimateTextTokens:跨语言保守近似
  英文 ≈ 4 字节/token;CJK ≈ 1 字/token(运行时比例)
```

### 6. 保留策略(compact.go:313+ keepIndexes)— 什么保留

```
keepIndexes 决定保留哪些消息(keepToolCallGroup:工具调用组整体保留)
shouldKeepMessage + KeepPolicy(keep/recent_keep)
用户 turn 保留(kept verbatim)——用户原话永不压缩
```

### 7. foldContext 失败处理(context_manager.go:123-182)— 单次事务的失败语义

```
- errCompressStaleContext(摘要期间 transcript 变了)→ 丢弃候选 + 阻塞本代
  "context changed during summary; automatic retry blocked for this generation"
- 摘要失败 → 记录 blocked/failed receipt(代级作用域)
- 同一代不付第二次自动摘要(manual compress 可重试)
- 结果仍 ≥ fold → stuck=true(压力重试无意义)+ blocked receipt
```

**关键设计**:
- **一代一票**:摘要失败/阻塞后,本代不再自动重试——**防重复付费**
- **stale 保护**:摘要期间上下文变了,候选丢弃(不装过期摘要)
- **stuck 标记**:摘要后仍超阈值 = 卡住,不再压力重试

### 8. 硬上限 = 物理安全(context_manager.go:107-112)

```
hardInputCeiling = window - protocolReserveTokens
overflow 或超硬上限时:
  必须释放(ErrCompactionRequired)——即使自动摘要失败也要释放
```

**溢出恢复是"一次性物理恢复路径"**(注释:114-115)——不是常规压缩,是物理安全网。

### 9. 固定前缀 = 缓存稳定的核心(compact.go:292-301)— review 新增

```go
// pinnedPrefixLen counts the leading messages a fold keeps verbatim ahead of
// everything else: the system prompt and the first user turn (its task + stated
// facts/constraints) when it is small enough to be a brief.
func pinnedPrefixLen(msgs) int {
  system prompt(1)
  + 第一个用户 turn(若非摘要且足够小)——pinned 逐字保留
}
```

**缓存优先的完整实现**:
- **system prompt + 首用户 turn = 字节稳定的缓存前缀**(DeepSeek 前缀缓存命中区)
- **Digests 从不 pinned**(注释:287-291):任何 digest 进入 fold 区合并到下一个摘要——**防 digest 链堆积**

**产品映射**:产品"规格书 + 首章指令"作为固定前缀——分析全程字节稳定,缓存永远命中。

### 10. 保留策略 keepIndexes(compact.go:313-367)— review 新增

```
1. policyStart = 最新 digest 之后(旧消息允许下一轮折叠,防无限增长)
2. shouldKeepMessage:错误消息 / 用户标记消息 → 保留
3. 用户 turn 保留(keepUserTurns,原话永不压缩)
4. keepToolCallGroup:assistant toolCall + 后续 toolResult 整体保留
```

**关键设计**:
- **工具调用组不可分割**(352-367):请求+结果必须一起保留——与 Pi 的 toolResult 不单独切同哲学
- **错误消息保留**(370-371):踩过的坑不进摘要——上下文里可见(防重复踩)

### 11. 结构化失败检测(compact.go:390-404)— review 新增

```go
// failedExecution reads the failure the host already recorded, rather than
// guessing from the text. A `go test` run that reports FAIL exits non-zero
// while its output starts with "=== RUN", which no prefix match can see.
```

**读结构化状态(ToolExecution.State/ExitCode/Verification)而非文本前缀**——文本匹配会漏(`go test` 输出以 === RUN 开头但退出码非 0)。

**产品映射**:产品"验证失败检测"读结构化状态,不猜文本。

### 12. 用户标记保留(isUserMarked, compact.go:406-415)— review 新增

```
用户消息以 [[keep]] / [keep] / <keep> / <!-- keep --> 开头 → 永不压缩
```

**用户显式标记"这条保留"**——压缩例外机制。
**产品映射**:产品"用户标注'这章结论保留'"→ 永不压缩。

### 13. 自适应 token 校准 tokPerChar(compact.go:512-519)— review 第三轮新增

```go
// tokPerChar derives a tokens-per-character ratio from the last turn's real
// usage so per-message estimates track the provider's tokenizer without a local one.
func (a *Agent) tokPerChar() float64 {
  if cal := promptCalibration; cal.compactChars > 0 {
    if r := cal.promptTokens / cal.compactChars; r > 0.05 && r < 2 {
      return r   // 用上一轮真实 usage 校准
    }
  }
  return fallbackTokPerChar  // 未校准时 ~4 chars/token
}
```

**关键设计**:从上一轮真实 usage 推导比率(不用本地 tokenizer),异常比率过滤(0.05-2),无校准时回退。
**产品映射**:产品 token 估算同样"校准优先"——上一轮真实 usage 校准,不用本地 tokenizer。

### 14. 摘要请求细节(compact.go:543-576)— review 第三轮新增

```
summarize:
- 用执行器自己的 provider(无工具)做摘要——同一模型,摘要一致
- instructions(/compact focus + PreCompact 文本)→ "Additional focus" 注入
- UsageSourceCompaction 记账
- 90s 超时 + request attempt counter
- 输出预算:summaryOutputMaxTokens(受 maxOutputTokens 约束)
```

**产品映射**:章节摘要用主模型(无工具)+ 焦点指令注入 + 独立 usage 记账。

### 15. 尾巴对齐 tailStart(compact.go:487-510)— review 第三轮新增

```
tailStart:从最新→最旧增长逐字尾巴,直到预算
→ 边界回退对齐:不在工具结果中间断
  "aligns the boundary back off any tool result so the tail never begins with
   an orphan whose assistant tool_calls were summarized away"
```

**防孤儿工具结果**:尾巴不能以"assistant 调用被摘要掉"的工具结果开头——**请求+结果不可分割的投影侧保证**。

### 16. CAS 安装检查点(compact_commit.go:22-56)— review 第四轮新增

**摘要成为投影的最终提交是完整 CAS**:
```
锁内验证 5 个条件:
  transcriptVersion 匹配 / 消息数匹配 / coveredPrefixHash 匹配
  projectionVersion 匹配 / generation 匹配
→ 全部匹配才安装(否则 errCompressStaleContext)
→ 持久化失败 → 回滚(prev 恢复)
```

**关键设计**:
- **前缀哈希(coveredPrefixHash)验证**:不只版本号,内容前缀哈希也要匹配——**更强的一致性检查**
- **持久化失败回滚**:安装后持久化失败 → 恢复旧状态(事务性)
- **CacheBreak: true**:摘要安装明确标记"缓存已破坏"(不可避免)

**产品映射**:产品"章节摘要安装"同样 CAS——版本/哈希/代全部匹配才装,失败回滚。

### 17. 摘要接受判定 acceptCheckpointCandidate(compact_projection.go:500-541)— review 第四轮新增

```
5 层拒绝规则:
1. candidate ≥ source → 拒绝(没省 token)
2. manual + 低于触发 → 任何真实节省都接受
3. 固定前缀超 50% → 例外:需要额外最小节省(exceptionalMinimumSavings)
4. candidate > ceiling 且非 force → 拒绝(受保护内容太大)
5. candidate ≥ trigger 且非 force → 拒绝(还在触发线以上)
```

**关键语义**:**接受 = 严格小于源 + 低于触发线(或 force 例外)**——与 SPEC.md 一致。保护内容(keep/active-turn)太大时 force/overflow 仍可装(严格更小的视图)。

### 18. 压缩是"模型可主动调用的工具"(compact_projection.go:24-59)— review 第五轮新增

```go
func (a *Agent) CompressContext(ctx, req tool.CompressRequest)  // compress 工具
```

**agent 自己可以调 compress 工具**(不只是 host 自动维护):
```
校验:direction(before/after)+ anchor 必填 + 长度限制
anchor 唯一匹配:
  0 个匹配 → "retry with an exact excerpt from a visible user turn"
  >1 个匹配 → "retry with a longer unique excerpt"
```

**关键设计**:
- **压缩成为工具**:模型发现上下文快满时主动压缩
- **anchor 唯一性校验**:模糊锚点拒绝——必须精确引用可见用户 turn
- **错误消息=反馈**:告诉模型怎么改("longer unique excerpt")

**产品映射**:产品"章节压缩"同样可以是工具——模型主动压缩 + anchor 精确校验。

### 19. 压缩执行完整流程(compressVisibleRange, :159-244)— review 第五轮新增

```
1. 快照校验(explicitCompressionSnapshotCurrent,过期→stale)
2. planVisibleCompression(计划可见压缩范围)
3. CompactionStarted 事件
4. prepareVisibleCompression(构建折叠区+指令)
5. foldToSummary(执行摘要)
6. interceptCompactionComplete(扩展可拦截/修改摘要!)
7. buildVisibleCompressionProjection(构建投影)
8. 比较:projectionTokens >= sourceTokens → "compressed context would not be smaller"(拒绝)
9. CAS 提交(commitSummaryProjection)
10. CompactionDone/Aborted 事件 + 遥测
```

**关键设计**:
- **扩展可拦截摘要结果**(interceptCompactionComplete)——摘要可被插件修改
- **大小比较在提交前**(214-219)——投影不小于源则拒绝
- **全程事件可观测**(Started/Telemetry/Done/Aborted)

## 代码类型

Algorithmic + Implementation(上下文维护引擎)

## 跨域关联

- → 被消费:sampling request(每次请求的可见视图)
- ← 依赖:provider(请求估算)、event(维护事件)
- ↔ 对照:Pi 的 compaction(增量摘要 vs Reasonix 单次摘要)

## 结论

核心可抄设计 19 个:
1. **唯一所有者**(sole owner of context maintenance)→ 上下文维护单一入口
2. **canonical 不可变 + projection 可演进** → 原始日志永不改
3. **低于阈值零操作**(缓存优先)→ 不重写不杀缓存
4. **多层阈值**(触发 0.85/接受 50%/硬上限/经济性)→ 量化预算
5. **7 标题摘要模板** → 产品④章节摘要
6. **一代一票**(失败阻塞本代)→ 防重复付费
7. **stale 保护**(摘要期间上下文变→丢弃)→ 不装过期摘要
8. **stuck 标记**(摘要后仍超阈值→不再压力重试)→ 收敛性
9. **固定前缀**(system prompt + 首用户 turn 字节稳定)→ 缓存命中区
10. **保留策略**(错误保留/工具组不可分/用户 turn 原话)→ 压缩例外
11. **结构化失败检测**(读 State/ExitCode 非文本)→ 可靠检测
12. **用户标记保留**([[keep]] 等)→ 用户例外
13. **自适应 token 校准**(上一轮真实 usage 推导比率)→ 估算跟随 tokenizer
14. **摘要用主模型 + 焦点指令** → 摘要一致性 + 可聚焦
15. **尾巴对齐防孤儿工具结果** → 请求+结果不可分投影侧保证
16. **CAS 安装检查点**(5 条件验证 + 持久化回滚)→ 摘要安装事务性
17. **摘要接受判定**(严格小于源 + 低于触发线)→ 接受规则集
18. **压缩作为工具**(模型主动调 + anchor 唯一校验)→ 自主压缩
19. **扩展可拦截摘要 + 全程事件可观测** → 可扩展 + 可观测

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| 唯一所有者 | ✅ 抄 | 产品上下文维护单一入口 |
| canonical 不可变 | ✅ 抄 | 知识库原始日志永不改 |
| 低于阈值零操作 | ✅ 抄 | 章节未满不压缩 |
| 多层阈值 | ✅ 抄 | 触发/接受/硬上限/经济性 |
| 7 标题摘要 | ✅ 抄 | **章节摘要模板(Standing facts/Decisions/Files/Errors/Pending)** |
| 一代一票 | ✅ 抄 | 摘要失败不重复付费 |
| stale 保护 | ✅ 抄 | 摘要期间内容变→丢弃 |
| stuck 标记 | ✅ 抄 | 摘要无效→不再重试 |
| 固定前缀 | ✅ 抄 | 规格书+首章指令字节稳定 |
| 工具组不可分 | ✅ 抄 | 分析请求+结果一起保留 |
| 结构化失败检测 | ✅ 抄 | 验证失败读结构化状态 |
| 用户标记保留 | ✅ 抄 | 用户标注章节结论不压缩 |
| 自适应校准 | ✅ 抄 | token 估算跟随真实 usage |
| 摘要用主模型 | ✅ 抄 | 章节摘要一致性 |
| 尾巴对齐 | ✅ 抄 | 摘要尾巴不产生孤儿结果 |
| CAS 安装 | ✅ 抄 | 摘要安装事务性(失败回滚) |
| 接受判定 | ✅ 抄 | 摘要必须严格更小且低于触发线 |
| 压缩即工具 | ✅ 抄 | 模型主动压缩章节 |
| 拦截+事件 | ✅ 抄 | 摘要可扩展 + 全程可观测 |

## 面试问答弹药

- **Q**:上下文管理第一原则?→ A:缓存优先——低于阈值绝对零操作,任何重写都会杀 prompt 缓存
- **Q**:什么时候压缩?→ A:唯一自动阈值 compact_ratio(默认 0.85),window × ratio
- **Q**:摘要多大合适?→ A:checkpointCeiling = window×50%,不加 padding,典型落在 10-30%
- **Q**:摘要模板什么样?→ A:7 标题——Standing facts/Goal/Decisions/Files/Commands/Errors/Pending
- **Q**:摘要失败怎么办?→ A:一代一票——失败阻塞本代自动重试,manual 可重试;stale 保护丢弃过期候选
- **Q**:摘要后还超阈值?→ A:stuck 标记——压力重试无意义,不再自动压缩
- **Q**:怎么估算 token?→ A:自适应校准——上一轮真实 usage 推导 tokPerChar,异常比率过滤,无校准时回退
- **Q**:什么永远不压缩?→ A:固定前缀(system prompt+首用户 turn)+ 用户标记([[keep]])+ 错误消息 + 工具调用组
- **Q**:缓存怎么保持命中?→ A:固定前缀字节稳定——任何重写都从该点杀缓存,所以低于阈值零操作
- **Q**:摘要尾巴怎么保证完整?→ A:tailStart 对齐——不在工具结果中间断,防孤儿工具结果
- **Q**:摘要怎么安装?→ A:CAS——transcript 版本/消息数/前缀哈希/投影版本/代全部匹配才装,持久化失败回滚
- **Q**:摘要怎么算合格?→ A:严格小于源 + 低于触发线(或 force 例外);固定前缀超 50% 时需额外最小节省
- **Q**:模型能主动压缩吗?→ A:能——compress 工具,anchor 必须唯一匹配可见用户 turn,模糊锚点拒绝
- **Q**:压缩过程可观测吗?→ A:完整事件流——CompactionStarted/Telemetry/Done/Aborted;扩展可拦截摘要结果
