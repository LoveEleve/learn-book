# q42 — Control-Plane + Account(深度版:云同步 + 账号)

> 域:控制面 | 文件:opencode/src/control-plane/(workspace 964/workspace-context 26/workspace-adapter-runtime 51/types 59/util 39)+ account/(account 473/repo 171/schema 99/url 8)+ core/src/control-plane/(workspace.sql 20/move-session 148)
> review 轮次:2 轮(源码全文核心)

---

## 假设

控制面 = 云工作区:Workspace(远程会话同步,SSE + 历史重放)与 Account(账号/组织/设备码登录)。这是 OpenCode 的企业/云能力,Session warp(会话搬运)与 sync fence(q17)配合。

## 验证

### 1. Workspace 同步(设计 1:SSE 连接 + 同步循环)

```ts
// workspace.ts:131-149 Interface
create/connectSSE/parseSSE/syncHistory/syncWorkspaceLoop/startSync/stopSync
// connectSSE(184):远程事件流;parseSSE(203):事件解析
// syncHistory(307):历史同步(事件重放——q6 的 durable tail 语义)
// syncWorkspaceLoop(366):同步主循环;startSync/stopSync(441/487):生命周期
// 错误族:SyncHttpError/SyncTimeoutError/SyncAbortedError/SessionEventsNotFoundError(78-116)
```

### 2. Session Warp(设计 2:会话搬运)

```ts
// workspace.ts:71-76 SessionWarpInput;SessionWarpHttpError(100)
// 云工作区间的会话搬运(与 core 的 move-session 配合)
```

### 3. WorkspaceContext(设计 3:本地上下文)

```ts
// workspace-context.ts:LocalContext.create("instance")——workspaceID 的 AsyncLocal 注入
// InstanceState.workspaceID = WorkspaceRef ?? WorkspaceContext.workspaceID(q37)
```

### 4. Account(设计 4:设备码登录 + 令牌刷新)

```ts
// account.ts:216-283
refreshToken(AccountRow):过期刷新(OAuth)
resolveToken/resolveAccess:令牌解析(缓存)
login(387):设备码登录流;poll(409):轮询授权
fetchOrgs/fetchUser:组织/用户信息
// repo.ts(171):account 表存取;schema.ts:AccountID/AccessToken/RefreshToken/DeviceCode
// 错误:AccountServiceError/AccountTransportError(域/传输分离)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Workspace 云同步(SSE + 历史重放) | workspace.ts:184-487 | ④跨设备知识库同步 |
| 2 | Session Warp 会话搬运 | workspace.ts:71-76 | ②会话迁移 |
| 3 | WorkspaceContext(AsyncLocal 注入) | workspace-context.ts | ②实例上下文 |
| 4 | Account 设备码登录 + 令牌刷新 | account.ts:216-409 | ④账号体系 |

## 面试弹药

- "云同步 = 事件重放":SSE 连接 + 历史同步——与本地 durable tail 同语义(q6)
- "设备码登录":login + poll(用户浏览器授权)——无密码的终端登录
- "域/传输错误分离":AccountServiceError vs AccountTransportError——业务与网络分开
- "workspace 是可选层":本地上下文 fallback(无 workspaceID 时)——云能力渐进增强

## 待深挖

- [ ] workspace 同步的具体事件协议
- [ ] account repo 的表结构
- [ ] 企业 URL 规范(enterpriseUrl)
