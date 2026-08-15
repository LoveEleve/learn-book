# q21 — Server/Protocol/Client/SDK(深度版:HttpApi 契约层 + 游标 + 生成)

> 域:接口契约层 | 文件:packages/protocol/src/(api.ts 86/groups/* 18 组/middleware/*/errors.ts)+ packages/server/src/(api/handlers/* 18 个/routes/middleware)+ packages/httpapi-codegen/ + packages/client/ + sdk-next/(opencode.ts 268/tool.ts)
> review 轮次:2 轮(协议层全文 + 生成架构文档)

---

## 假设

协议层 = "权威 HttpApi 契约"的唯一定义处。Protocol 拥有分组和 middleware 放置(占位),Server 注入具体 middleware key 并实现 handlers,Client/SDK 从同一 HttpApi 生成。光标 = base64url 编码的完整查询(不透明)。

## 验证

### 1. HttpApi 组合(设计 1:17 组 + 双 middleware)

```ts
// protocol/src/api.ts:26-65 makeApiFromGroup
HttpApi.make("server")
  .add(Health/Location/Agent/Session/Message/Model/Provider/Integration/Credential/
       Permission/FileSystem/Command/Skill/Event/Pty/Question/Reference/ProjectCopy)  ← 18 组
  .middleware(Authorization)
  .middleware(SchemaErrorMiddleware)
// Protocol 放置 middleware,Server 注入具体 key(注释: "Protocol owns middleware placement, while Server injects concrete keys")
// makeApi(definitions):事件组由调用方传入定义(Client 投影可传 transport-only keys)
```

**依赖纪律**:Client 生成代码只依赖 Schema/Protocol,永不依赖 Core/Server(AGENTS.md)。

### 2. 端点契约(session 组样本,设计 2:14 端点)

```ts
// groups/session.ts:106-379
list(游标)/create/get/active/switchAgent/switchModel/prompt/compact/wait/
revert{stage,clear,commit}/context/history/events(SSE)/interrupt/message
// 错误契约:SessionNotFoundError/MessageNotFoundError/ConflictError/InvalidCursorError/ServiceUnavailableError
// SSE:HttpApiSchema.StreamSse({ data: SessionEvent.Durable })(session.events, 重放+tail)
// history:有限分页(after + limit ≤ 100,hasMore)
```

### 3. 不透明游标(设计 3:base64url 编码完整查询)

```ts
// groups/session.ts:49-81
SessionsCursor = String branded
make: Encoding.encodeBase64Url(JSON(查询字段 + anchor))
parse: decodeBase64UrlString → decodeSessionsCursor → 失败 = InvalidCursorError
// "Opaque pagination cursor"(specs/v2/session.md:171-174):
// 初始查询固定 scope/filter/order/page size;续页只传光标(不携带任何查询参数)
```

### 4. 生成链(设计 4:SDK Contract IR → 双发射器)

```
Server HttpApi(权威)
  → httpapi-codegen:编译 HttpApi → SDK Contract IR(运行时中立,保留编码/解码投影 + 传输元数据)
  → Promise 发射器:零 Effect 结构化 wire 类型 + AsyncIterable 流
  → Effect 发射器:rich 解码值 + Schema 运行时校验 + HttpApiClient 传输
// 依赖方向:Client 根(零 Effect)→ /effect 入口(Effect + Schema + Protocol)
// sdk-next:进程内 Embedded OpenCode(内存 HttpApiClient 跑 Server 的 HttpRouter,不开端口)
```

### 5. 错误划分(设计 5:域错误 vs 基础设施)

```ts
// CONTEXT.md:150-153
declared failures:tagged 结构化 wire 值 + 生成 type guards(不依赖 Error 子类身份)
infrastructure failures:ClientError(transport/unexpected status/unsupported content type/malformed)
// Promise reject:tagged declared failure 或 ClientError(与 Effect 的 domain/infrastructure 划分对应)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | HttpApi 18 组 + Protocol 放置/Server 注入 | protocol/api.ts:26-65 | ②接口契约单一来源 |
| 2 | 端点契约 + 错误声明 + SSE | groups/session.ts | ②API 设计(产品可抄) |
| 3 | 不透明游标(base64url 完整查询) | groups/session.ts:49-81 | ④分页契约 |
| 4 | SDK Contract IR → 双发射器 → Embedded | httpapi-codegen + client + sdk-next | ②多客户端生成 |
| 5 | 域错误 vs 基础设施错误划分 | CONTEXT.md:150-153 | ③错误契约 |

## 面试弹药

- "接口契约单一来源":Server 的 HttpApi 权威,codegen 反射成 IR,Promise/Effect 发射器各自解释——不物理共享类型包
- "游标 = 编码的完整查询":初始查询固定一切,续页零参数——防调用方改 scope 分页(一致性)
- "Embedded = 内存 HttpRouter":sdk-next 不开端口,直接跑 Server 路由——进程内集成的完整 HTTP 边界保留
- "错误不依赖类身份":tagged wire 值 + 生成 type guard——跨包副本/跨 realm 可判别

## 待深挖

- [ ] server/src/handlers/session.ts(385)的实现细节
- [ ] httpapi-codegen 的 IR 结构
- [ ] client 生成的 SSE 重连语义(CONTEXT.md:156-157:不自动重连,显式失败)
