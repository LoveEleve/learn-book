# q45 — httpapi-codegen + Server 中间件(深度版:生成器 + 授权/路由)

> 域:接口契约层 | 文件:packages/httpapi-codegen/src/index.ts(1185)+ server/src/(middleware/* 3 个/handlers/session.ts 385/auth.ts/cors.ts/location.ts)+ protocol/src/middleware/(authorization/schema-error)
> review 轮次:2 轮(源码全文核心)

---

## 假设

httpapi-codegen = HttpApi → SDK Contract IR 的编译器(portable 分析 + 生成)。Server middleware 三层:Authorization(Basic 认证)、SessionLocation(按会话路由 Location 服务)、SchemaError(错误规范化)。

## 验证

### 1. 生成器(设计 1:compile → Contract)

```ts
// httpapi-codegen/src/index.ts:76-102
compile(api):遍历 group/endpoint → Endpoint{name, params, successSchemas, errors, middleware}
portable Map:SchemaAST 可移植性分析(哪些 schema 能跨运行时)
// 关键概念:middleware.requiredForClient(端点标记哪些中间件客户端必须实现)
// manifestName = ".httpapi-codegen.json"(生成清单)
// Contract 输出:IR(编码/解码投影 + 传输元数据)——q21 的 SDK Contract IR
```

### 2. SessionLocation 中间件(设计 2:按会话路由)

```ts
// server/src/middleware/session-location.ts:24-67
从 route params 解 sessionID → 查 session 表(directory/workspaceID)→
  Effect.provide(locations.get(Location.Ref.make({directory, workspaceID})))——把路由的 Location 服务注入请求
// 用途:任何 /api/session/:sessionID/* 请求都自动获得该会话所在目录的服务集
// 错误:InvalidRequestError(坏 ID)/SessionNotFoundError(不存在)
```

### 3. Authorization(设计 3:Basic 认证 + PTY 票据豁免)

```ts
// server/src/middleware/authorization.ts:38-57
ServerAuth.required(config) 为假 → 透传(本地无认证)
认证来源:auth_token query(WebSocket/浏览器)或 Authorization: Basic
PTY 连接票据豁免:hasPtyConnectTicketURL → 跳过(浏览器 WebSocket 无法带头,票据验证在 connect handler)
失败 → 401 + WWW-Authenticate: Basic realm="Secure Area"
```

**设计要点**:Basic 认证(简单)/auth_token query(无头场景)/PTY 票据(WebSocket 特例)——三层降级。

### 4. Handler 模式(设计 4:HttpApiBuilder.group)

```ts
// handlers/session.ts:19-385
HttpApiBuilder.group(Api, "server.session", (handlers) => ...)
每个 handle:Effect.fn(ctx => ...)——ctx.query/params/payload
session.list:游标解析(parse → InvalidCursorError)→ session.list → previous/next 游标构造(anchor 双向)
// 错误映射:core 错误 → protocol 错误(SessionNotFoundError 等)
// 分页默认:50(q21 的 DefaultSessionsLimit)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | compile → IR(portable 分析 + requiredForClient) | httpapi-codegen:76-102 | ②代码生成 |
| 2 | SessionLocation 按会话路由服务 | middleware/session-location.ts | ②请求路由 |
| 3 | Basic 认证 + query token + PTY 票据豁免 | middleware/authorization.ts | ②认证降级 |
| 4 | Handler 模式(错误映射 + 游标构造) | handlers/session.ts | ②服务实现模式 |

## 面试弹药

- "requiredForClient 标记":端点声明哪些中间件客户端必须实现——生成器感知契约边界
- "SessionLocation = 路由级服务注入":请求到达即获得会话目录的服务集——Location 架构在 HTTP 层的落地
- "三层认证降级":Basic 头 / auth_token query / PTY 票据豁免——每种传输的限制都覆盖
- "游标在 handler 构造":previous/next 基于首尾元素 + anchor 方向——分页闭环

## 待深挖

- [ ] codegen 的 emitter(promise/effect 双发射器细节)
- [ ] server/src/auth.ts(ServerAuth 配置)
- [ ] handlers 的其他组(message/pty 等)
