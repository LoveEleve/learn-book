# q29 — V1 消息模型(深度版:message/part 双表 + 媒体兼容矩阵)

> 域:②参考(V1 数据模型) | 文件:opencode/src/session/message-v2.ts(737)+ core/src/v1/session.ts(message/part schema)+ opencode/src/session/compaction.ts(608,V1 压缩)
> review 轮次:2 轮(源码全文核心段)

---

## 假设

V1 消息模型 = message/part 双表(会话消息 + 内容部件),MessageV2 负责"V1 存储 → AI SDK 消息"转换。与 V2 的差异:V1 是"部件流"(text/file/tool/step-start/reasoning/compaction/subtask 部件),V2 是"聚合消息"(内容数组内嵌)。媒体兼容矩阵是 V1 的独特复杂度。

## 验证

### 1. 双表模型(设计 1:message + part)

```sql
-- sql.ts:68-98(V1 表保留)
message: id PK, session_id FK, data(json: SessionV1.Info)
part: id PK, message_id FK, session_id, data(json: SessionV1.Part)
-- V1 消息 = 头部(info)+ 部件数组(parts)
```

### 2. 部件类型(设计 2:9 种 part)

```ts
// message-v2.ts:198-242 处理:
text(可 ignored)/file(mime 分类)/compaction/subtask/step-start/reasoning/tool/step-finish/shell
// user 消息转换:
- text 非空非 ignored → text part
- file:text/plain + directory → 忽略(已转文本);其他 → file part(stripMedia 时 → "[Attached mime: name]")
- compaction part → "What did we do so far?"(V1 压缩标记在用户消息里!)
- subtask part → "The following tool was executed by the user"
```

### 3. 媒体兼容矩阵(设计 3:provider 能力表)

```ts
// message-v2.ts:147-159 supportsMediaInToolResult:
anthropic → true(全部支持)
openai → true
bedrock → 仅 image/
xai → 仅 image/
google-vertex/anthropic → true
google → 仅 gemini-3
// 用途:工具结果里的媒体,不支持的 provider → 提取为独立 user 消息注入
// (OpenAI 兼容 API 工具结果只支持字符串;Bedrock 支持图片不支持 PDF)
```

**设计要点**:媒体能力是 provider 表驱动(硬编码矩阵)——V2 用 providerExecuted round-trip 绕开了大部分。

### 4. Assistant 消息(设计 4:错误跳过 + 模型切换)

```ts
// message-v2.ts:244-270
- msg.info.error 且非纯 abort → continue(错误消息跳过,除 abort + 无实质部件)
- differentModel(providerID/modelID 不匹配)→ 不同处理路径
- Anthropic adaptive thinking 的签名位置保留注释(空文本分隔符语义)
```

### 5. V1 压缩(设计 5:与 V2 checkpoint 不同)

```ts
// opencode/src/session/compaction.ts(608 行):V1 分支摘要式压缩(压缩结果作为 compaction part 留在用户消息)
// 对比 V2:V2 是 checkpoint(独立 compaction 消息 + summary/recent),V1 是"换人继续"(分支模型)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | message/part 双表(V1 保留) | sql.ts:68-98 | ④迁移兼容 |
| 2 | 9 种部件 + 转换规则(compaction→"What did we do") | message-v2.ts:198-242 | ②消息模型演进 |
| 3 | 媒体兼容矩阵(provider 表驱动) | message-v2.ts:147-159 | ②媒体处理 |
| 4 | 错误消息跳过 + 模型切换处理 | message-v2.ts:244-270 | ②历史重放 |
| 5 | V1 分支压缩 vs V2 checkpoint | compaction.ts(608) | ②压缩演进 |

## 面试弹药

- "媒体兼容是 provider 表驱动":工具结果媒体不支持的 provider → 提取为 user 消息——V2 用 providerExecuted 简化
- "V1 压缩 = 换人继续":compaction part 变 "What did we do so far?" 用户消息——V2 的 checkpoint 是独立消息类型
- "错误消息重放跳过":非 abort 错误的 assistant 消息不进模型历史——防错误上下文污染

## 待深挖

- [ ] V1 compaction.ts(608)的摘要 prompt 细节
- [ ] message-v2 的 cursor/分页(filterCompacted)
- [ ] part 的 step-start/reasoning 语义(adaptive thinking)
