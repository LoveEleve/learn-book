# 闭环笔记 Q3:上下文构建管线 + 压缩算法核心

> 域:harness/session/context.ts(100 行)+ harness/compaction/compaction.ts(848 行)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

context.ts 是"会话条目 → LLM 上下文"的转换管线;compaction.ts 的 prepareCompaction 是压缩的核心算法。两者配合决定"LLM 每次看到什么"。

## 验证过程

### 1. 上下文构建管线(context.ts,100 行全文读完)

**三段管线**(90-99):
```
buildSessionContext(pathEntries):
  ① deriveSessionContextState → 推导 thinkingLevel/model/activeTools(遍历变更条目,25-43)
  ② buildContextEntries → 条目过滤(默认压缩处理 + 可插拔 transforms,59-63)
  ③ sessionEntryToContextMessages → 条目→消息映射(65-88)
```

**③ 的映射规则**(65-88):
| 条目 | 转消息 |
|------|--------|
| message | 原样,但 **stopReason === "deferred" 的 assistant 消息被过滤**(:72) |
| compaction | 摘要消息 + **...entry.retainedTail(内嵌尾巴)**(:75-80) |
| branch_summary | 分支摘要消息 |
| custom | **entryProjectors[customType] 投影器**(:84-86),可插拔 |
| 其他 | 空 |

**两个可插拔点**(12-23):
- `entryTransforms`:条目变换管道(transform 链)
- `entryProjectors`:custom 条目 → 消息的投影器(按 customType 注册)

**核心洞察**:
- **deferred 消息过滤**:模型"延迟完成"的 assistant 消息不进上下文——中间推理不污染
- **compaction 条目自带 retainedTail**:尾巴消息内嵌在压缩条目里,不是靠 firstKeptEntryId 指针

### 2. 压缩算法心脏 prepareCompaction(620-687)

```
prepareCompaction 流程:
1. 找最近一次压缩(prevCompactionIndex,624-630)
2. 把上次压缩的 retainedTail 转成"虚拟消息条目"(637-645)
   ——尾巴也能参与再次压缩!
3. findCutPoint 按 keepRecentTokens 找切点(651)
4. 切点之前 → messagesToSummarize(摘要对象)
5. 切点在轮中间 → turnPrefixMessages(轮前缀单独摘要,659-664)
6. 切点之后 → retainedTail(尾巴,665-669)
7. extractFileOperations 从摘要对象提取文件操作(670)
```

### 3. findCutPoint 算法(374-438)— 按 token 预算切

```
findValidCutPoints:有效切点 = user/assistant/bashExecution/custom/branchSummary/compactionSummary
                    toolResult 不是切点(工具结果跟它的请求一起被切)
从尾部往回累计 token(394-407):累计到 keepRecentTokens(默认 20000,161)
→ 对齐到最近的合法切点
→ 切点前若不是 user 消息 → 找轮起始(turnStartIndex)→ isSplitTurn
```

**关键设计**:
- **保留最近 20000 token,其余全部摘要**——"保留尾巴"的量化定义
- **toolResult 不单独作切点**:工具结果必须跟着它的请求——上下文语义完整
- **轮前缀单独摘要**(TURN_PREFIX_SUMMARIZATION_PROMPT,689-699):半个轮被切时,前缀用专门 prompt 摘要,为保留的后缀提供上下文

### 4. 摘要同时提取文件操作(extractFileOperations,44+)

压缩摘要时**同时提取改动的文件**——压缩后仍知道"这个会话改过哪些文件"。

### 5. compact() 执行流程(707-848)— review 新增

```
compact(preparation) 执行:
- 非 split turn:一次 generateSummaryWithUsage(历史摘要,767-782)
- split turn:两次 LLM 调用
  ① generateSummaryWithUsage(历史,735-750)
  ② generateTurnPrefixSummary(轮前缀,751-760,用 TURN_PREFIX_SUMMARIZATION_PROMPT)
  ③ 拼接:historyText + "---" + Turn Context + prefixText(762)
- 文件操作:computeFileLists(read/modified)→ formatFileOperations 追加到摘要末尾(784-785)
- 返回:{summary, tokensBefore, usage, retainedTail, details{readFiles, modifiedFiles}}
```

**关键设计**:
- **轮前缀摘要的 maxTokens 限制**:`Math.floor(0.5 * reserveTokens)`(805-808)——半个预算
- **摘要专用 system prompt**(SUMMARIZATION_SYSTEM_PROMPT:424):"Do NOT continue the conversation. ONLY output the structured summary."——**防摘要跑偏**

### 6. shouldCompact 触发判定(review 新增)

```ts
shouldCompact(contextTokens, contextWindow, settings):
  return contextTokens > contextWindow - settings.reserveTokens;
```
**简单而精确**:当前 token > 窗口 - 预留空间。reserveTokens 是"留给输出的空间"。

### 7. 分支摘要结构化格式(178-205)— review 新增,产品交接文档直接蓝本

**BRANCH_SUMMARY_PROMPT 强制格式**:
```
## Goal — 这条分支想完成什么
## Constraints & Preferences — 约束/偏好
## Progress — Done / In Progress / Blocked
## Key Decisions — 决策 + 理由
## Next Steps — 下一步
要求:Preserve exact file paths, function names, and error messages
```

**这是产品①交接文档/书级知识库"章节摘要"的格式蓝本**——比用户的 HANDOVER 更结构化,且专门要求保留精确的 file path/函数名/错误信息(证据链)。

### 8. prepareBranchEntries 的 token 预算逻辑(132-171)— review 新增

```
从尾部往回:累计 token 到预算(contextWindow - reserveTokens)
预算耗尽时:
- compaction/branch_summary 条目:若总量 < 90% 预算则强制保留(unshift)
- 其他:break
同时从 branch_summary 的 details 和消息里提取文件操作
```

**关键**:90% 预算阈值(158)+ 摘要条目强制保留——预算耗尽也要留摘要,不丢上下文锚点。

### 9. 摘要请求的缓存隔离(102-120)— review 第二轮新增

`completeSimpleWithRetries`:
```ts
const requestOptions = {
  ...options,
  cacheRetention: "none",   // 摘要请求不写缓存
  sessionId: uuidv7(),       // 独立 sessionId,隔离路由
};
```

**关键设计**:**摘要请求特意不参与前缀缓存**——摘要内容不可复用,写缓存只会污染(注释 110:"isolate routing and avoid cache writes that cannot be reused")。
**产品映射**:产品"章节摘要/验收器请求"同样应该隔离缓存——不污染主执行链路的前缀缓存。

### 10. 完整摘要 prompt 体系(review 第二轮新增)— 产品质量核心

**SUMMARIZATION_PROMPT**(完整版,7 段):
```
## Goal / ## Constraints & Preferences
## Progress(Done/In Progress/Blocked)
## Key Decisions / ## Next Steps
## Critical Context(数据/示例/引用,继续工作所需)
要求:Preserve exact file paths, function names, and error messages
```

**UPDATE_SUMMARIZATION_PROMPT**(增量更新版):
- 前提:<previous-summary> 已存在
- 规则:PRESERVE 旧信息 + ADD 新进展 + UPDATE Progress(移到 Done)+ 可移除过时
- **增量压缩不重建,只更新**——多轮压缩的历史连续性

**触发逻辑**(529+):`previousSummary ? UPDATE : SUMMARIZATION`——第二次压缩起用增量版。

**产品映射**:书级知识库章节摘要直接抄——**"章节 1 摘要 + 增量更新"替代"每次全量重写"**,保历史连续性。这正是解决 #24"压缩后跑偏"的质量保障。

### 11. estimateTokens 分角色估算(271-305)— review 第二轮新增

```
chars/4 估算,按角色:
- user:文本+图片内容
- assistant:text + thinking + toolCall(name + 参数 JSON)
- toolResult:文本内容
- bashExecution:命令 + 输出
- branchSummary/compactionSummary:摘要长度
```
**简单但分角色**——不需要 tokenizer,足够压缩决策用。

### 12. 文件操作三态 + XML 标签(review 第二轮新增)

```
FileOperations: read / written / edited 三个 Set(utils.ts:5-12)
extractFileOpsFromMessage:assistant 消息的 toolCall 里按 name 分类
  read → read 集合 / write → written / edit → edited
computeFileLists:modified = edited ∪ written;readOnly = read - modified
formatFileOperations:<read-files>/<modified-files> XML 标签追加到摘要
```

**关键**:**文件操作是"压缩摘要的元数据"**,用 XML 标签结构化追加——LLM 和程序都能解析。
**产品映射**:书级知识库"这章分析了哪些文件"的审计信息,结构化存储。

### 13. serializeConversation + 截断(91-131)— review 第二轮新增

- 对话序列化格式:`[User]:` / `[Assistant thinking]:` / `[Assistant]:` / `[Assistant tool calls]:` / `[Tool result]:`
- **工具结果截断 2000 字符**(TOOL_RESULT_MAX_CHARS:74)——摘要输入限制

### 14. estimateContextTokens 双轨策略(216-244)— review 第三轮新增

```
estimateContextTokens(messages):
- 找最后一条带 usage 的 assistant 消息(getLastAssistantUsageInfo)
- 有 usage: tokens = calculateContextTokens(usage) + trailingTokens(之后消息的估算)
- 无 usage: 全部用 chars/4 估算
```

**关键设计**:**"精确优先、估算兜底"双轨**——provider 的 usage 是权威,只有缺失时才用启发式。trailingTokens 只算最后一条 usage 之后的增量。
**产品映射**:书级知识库的 token 计量——精确(usage)+ 兜底(估算)双轨,防止重复统计。

### 15. 图片估算常量(252-268)— review 第三轮新增

- `ESTIMATED_IMAGE_CHARS = 4800`——每张图按 4800 字符估算(约 1200 token)
- `estimateTextAndImageContentChars`:text 按字符,image 按常量

**产品映射**:书库含图章节的 token 预算参考。

### 16. 分支摘要执行细节(231-280)— review 第三轮新增

```
generateBranchSummary:
- instructions 三选一:replaceInstructions ? custom : (custom + "Additional focus") : BRANCH_SUMMARY_PROMPT
- maxTokens: 2048 硬上限(分支摘要比主压缩短——被放弃的路径不需要长摘要)
- 输出:BRANCH_SUMMARY_PREAMBLE + summary + formatFileOperations
```

**关键**:分支摘要 maxTokens 2048 << 主压缩(0.8*reserveTokens)——**摘要长度与信息价值成正比**:被放弃路径的摘要短,主路径的摘要长。

## 代码类型

Algorithmic(压缩算法)+ Glue(条目→消息转换)

## 跨域关联

- → 被依赖:buildSessionContext 被 AgentSession 调用(压缩后重建上下文)
- ← 依赖:messages.ts(摘要消息创建)、types.ts(Entry 定义)

## 结论

核心可抄设计 16 个:
1. **三段管线(状态推导 → 条目过滤 → 消息映射)** → 产品上下文构建
2. **compaction 内嵌 retainedTail** → 章节摘要自带尾巴
3. **deferred 消息过滤** → 中间推理不污染上下文
4. **entryTransforms/entryProjectors 可插拔** → 上下文构建可扩展
5. **按 token 预算切分 + toolResult 不单独切** → 保留尾巴的量化算法
6. **轮前缀单独摘要 + 文件操作提取** → 压缩不丢"改了什么文件"
7. **摘要专用 system prompt(防跑偏)+ split turn 双次调用** → 摘要质量保障
8. **分支摘要结构化格式(Goal/Progress/Decisions/NextSteps)** → 产品交接文档格式蓝本
9. **摘要请求缓存隔离(cacheRetention:none + 独立 sessionId)** → 不污染主链路缓存
10. **SUMMARIZATION/UPDATE 双 prompt(全量/增量更新)** → 章节摘要增量演进保连续性
11. **分角色 token 估算(chars/4)** → 无需 tokenizer 的估算
12. **文件操作三态 + XML 标签元数据** → 审计信息结构化
13. **对话序列化格式 + 2000 字符截断** → 摘要输入规范
14. **usage 优先/估算兜底双轨** → 精确计量不重复统计
15. **图片 4800 字符常量估算** → 含图章节预算
16. **摘要长度与信息价值成正比(分支 2048 vs 主压缩长摘要)** → 资源分配策略

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| 三段管线 | ✅ 抄 | 产品上下文构建(章节→上下文) |
| retainedTail 内嵌 | ✅ 抄 | 章节摘要自带"最近结论尾巴" |
| deferred 过滤 | ✅ 抄 | 探索过程不污染书库 |
| transforms/projectors | ✅ 抄 | 自定义条目(结论/证据)投影进上下文 |
| token 预算切分 | ✅ 抄 | 章节压缩的量化预算 |
| toolResult 不单独切 | ✅ 抄 | 保持"请求+结果"语义完整 |
| 摘要专用 system prompt | ✅ 抄 | 防摘要跑偏(#24 收敛性) |
| 分支摘要结构化格式 | ✅ 抄 | 书级知识库章节摘要/交接文档格式 |
| 缓存隔离 | ✅ 抄 | 摘要/验收请求不污染主链路缓存 |
| 增量更新 prompt | ✅ 抄 | 章节摘要演进(不重写历史) |
| usage 双轨计量 | ✅ 抄 | 知识库 token 计量 |
| 摘要长度分级 | ✅ 抄 | 资源分配(重要路径长摘要) |
| 文件操作提取 | ✅ 抄 | 压缩后仍知道改过什么(审计) |

## 面试问答弹药

- **Q**:上下文怎么从会话构建?→ A:三段管线:状态推导 → 条目过滤(压缩处理)→ 条目→消息映射
- **Q**:压缩保留多少尾巴?→ A:默认 keepRecentTokens 20000,从尾部往回累计,按 token 预算切
- **Q**:工具结果会被切掉吗?→ A:不会——toolResult 不是有效切点,必须跟它的请求一起
- **Q**:压缩会丢文件操作信息吗?→ A:不会——extractFileOperations 摘要时同步提取
- **Q**:custom 条目怎么进上下文?→ A:entryProjectors 投影器,按 customType 可插拔
