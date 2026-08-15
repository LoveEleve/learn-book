# q43 — SessionMessage + Prompt schema(深度版:消息类型联合)

> 域:④知识库(数据模型) | 文件:schema/src/session-message.ts(213)+ prompt.ts(57)+ session-input.ts(23)+ prompt-input.ts(26)
> review 轮次:2 轮(源码全文)

---

## 假设

SessionMessage = 8 类型联合(type 判别),是"投影历史的存储格式"。工具状态 = 4 态联合(pending/running/completed/error)。Prompt = 文本 + 文件附件 + agent 引用。这是 q9(投影规则)和 q23(翻译规则)操作的静态模型。

## 验证

### 1. 消息类型联合(设计 1:8 类型)

```ts
// session-message.ts:200-212
Message = AgentSwitched | ModelSwitched | User | Synthetic | System | Shell | Assistant | Compaction
// 每类型带 Base{id, metadata, time.created};toTaggedUnion("type")
// User:Prompt 字段(text/files/agents)——与 Prompt 共享 schema
// Assistant:agent + model + content[] + snapshot{start,end,files} + finish + cost/tokens + error
// Compaction:reason(auto|manual) + summary + recent
```

### 2. 工具状态机(设计 2:4 态联合)

```ts
// session-message.ts:81-119
Pending{status, input: string}(输入是字符串——流式累积)
Running{input: Record, structured, content}(解析后)
Completed{attachments, content, outputPaths, structured, result}
Error{content, structured, error: UnknownError, result}
// AssistantTool{type:"tool", id, name, provider{executed, metadata, resultMetadata}?, state, time{created,ran,completed,pruned}}
//   pruned 字段 = V1 压缩擦除标记(q38)在 V2 schema 的对应
```

**关键**:pending 的 input 是 string(流式),running 后是 Record(解析)——状态迁移伴随输入形状变化(q9 投影器处理)。

### 3. Provider 元数据分离(设计 3:双 metadata)

```ts
// session-message.ts:126-130
provider = { executed: Boolean, metadata?, resultMetadata? }
// metadata = call 侧(模型返回);resultMetadata = settlement 侧(q1 的 provider 分离原则)
// q23 翻译:reuseProviderMetadata 时用 resultMetadata ?? metadata
```

### 4. Prompt(设计 4:附件模型)

```ts
// prompt.ts:40-56
Prompt = { text, files?: FileAttachment[], agents?: AgentAttachment[] }
FileAttachment = { uri, mime, name?, description?, source?{start,end,text} }
  ——source = 原始文档片段(引用来源可审计)
AgentAttachment = { name, source? }(agent 引用:任务指导)
// statics:equivalence(Schema.toEquivalence,收件箱幂等比较用 q4)+ fromUserMessage
```

### 5. 收件箱 schema(session-input.ts)

```ts
// session-input.ts(23 行):Admitted{id, sessionID, admittedSeq, prompt, delivery, timeCreated, promotedSeq?}
// Delivery = "steer" | "queue"
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 8 类型消息联合(type 判别) | session-message.ts:200-212 | ④知识库存储格式 |
| 2 | 工具 4 态 + pruned 标记 | session-message.ts:81-119 | ③工具终态 |
| 3 | provider 双 metadata(call/settlement) | session-message.ts:126-130 | ②元数据保真 |
| 4 | Prompt 附件 + source 审计 | prompt.ts:40-56 | ④引用可审计 |
| 5 | 收件箱 Admitted 结构 | session-input.ts | ④收件箱格式 |

## 面试弹药

- "pending 输入是 string,running 后是 Record":状态迁移伴随形状变化——流式累积到解析的确定性
- "pruned 字段":V1 压缩擦除在 V2 schema 的对应——历史兼容
- "source 审计":附件带原始片段引用——"这引用来自哪"可查
- "双 metadata":call 侧 vs settlement 侧分离——不互相覆盖

## 待深挖

- [ ] llm.ts schema(ProviderMetadata/ToolContent 结构)
- [ ] model.ts schema(Model.Ref)
- [ ] 8 类型的完整字段对照表
