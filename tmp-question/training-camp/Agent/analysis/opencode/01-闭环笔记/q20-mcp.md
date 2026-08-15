# q20 — MCP 集成(深度版:三传输 + OAuth 动态注册 + 凭证绑定)

> 域:MCP(JD 6/10) | 文件:opencode/src/mcp/(index.ts 1004/oauth-provider.ts 259/oauth-callback.ts 194/auth.ts 163/catalog.ts 170/browser.ts 37)+ @modelcontextprotocol/sdk
> review 轮次:2 轮(服务能力面 + OAuth 全文)

---

## 假设

MCP = 外部工具/资源/提示的统一集成:三传输(stdio/StreamableHTTP/SSE)、OAuth 授权码流(带动态注册)、凭证按服务器 URL 绑定持久化。V1 注册表桥接,V2 规范化为 follow-up。

## 验证

### 1. 服务能力面(设计 1:19 方法 + 5 态)

```ts
// mcp/index.ts:164-183
status/clients/instructions/tools/prompts/resources/resourceTemplates(只读面)
add/connect/disconnect(生命周期)
getPrompt/readResource(资源访问)
startAuth/authenticate/finishAuth/removeAuth/supportsOAuth/hasStoredTokens/getAuthStatus(认证)
// 状态机(index.ts:84-106):connected/disabled/failed/needsAuth/needsClientRegistration
```

### 2. 三传输 + 资源安全(设计 2)

```ts
// index.ts:213-237
Transport = Stdio | StreamableHTTP | SSE
connectTransport:acquireUseRelease——失败关闭传输,成功调用方拥有
connectRemote:远程 + OAuth;connectLocal:stdio 子进程
```

### 3. OAuth 授权码流(设计 3:完整 RFC 6749 实现)

```ts
// oauth-provider.ts:35-129
redirectUrl:配置 redirectUri 或 http://127.0.0.1:19876/mcp/oauth/callback(固定端口)
clientMetadata:grant_types [authorization_code, refresh_token];token_endpoint_auth_method = client_secret_post | none
clientInformation:配置 clientId → 直接返回;否则查存储(动态注册);clientSecretExpiresAt 过期 → 重新注册
saveTokens/saveClientInformation → auth 持久化(getForUrl 校验 URL)
state():无保存状态时自动生成(自动连接场景)——SDK 把 state() 当生成器而非读取器
```

**关键设计**:
- **URL 绑定凭证**:getForUrl(mcpName, serverUrl)——服务器 URL 变了,凭证不匹配(防凭证错配)
- **client secret 过期重注册**:clientSecretExpiresAt 检查
- **动态客户端注册**:无配置 clientId → OAuth 服务器动态注册(DCR)

### 4. Pending 暂存(设计 4:先收集后 commit)

```ts
// oauth-provider.ts:183-238 McpOAuthPendingProvider
pendingClientInfo/pendingTokens 内存暂存(自动认证路径)
commit():一次性写入持久化——中途失败不污染已有凭证
```

### 5. 凭证存储(auth.ts:163)

```ts
// auth.ts:McpAuth.Service:get/set/remove/getForUrl/updateTokens/updateClientInfo/updateCodeVerifier/updateOAuthState
// 存储结构:每 MCP 服务器 { clientInfo, tokens, codeVerifier, oauthState }
// code verifier 持久化(PKCE):codeVerifier()/saveCodeVerifier
```

### 6. 回调服务器(oauth-callback.ts:194)

```ts
// 本地 HTTP 回调:接收 authorizationCode → finishAuth
// parseRedirectUri:从 redirectUri 解析端口/路径(默认 19876 + /mcp/oauth/callback)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 19 方法能力面 + 5 态状态机 | index.ts:164-183,84-106 | ②外部服务集成 |
| 2 | 三传输 + acquireUseRelease | index.ts:213-237 | ②连接生命周期 |
| 3 | OAuth 授权码流(动态注册 + 刷新) | oauth-provider.ts:35-129 | ②第三方认证 |
| 4 | Pending 暂存后 commit(防污染) | oauth-provider.ts:183-238 | ②失败安全 |
| 5 | 凭证 URL 绑定 + secret 过期重注册 | auth.ts | ④凭证安全 |
| 6 | 本地回调服务器 | oauth-callback.ts | ②OAuth 回调 |

## 面试弹药

- "凭证绑定服务器 URL":getForUrl 校验——URL 变了凭证不生效(防凭证错配到恶意服务器)
- "动态客户端注册":无 clientId 时走 DCR,secret 过期自动重注册
- "pending 暂存":自动认证先内存收集,commit 才持久化——中途失败不污染旧凭证
- "state() 是生成器":SDK 语义下无状态时自动生成——接口设计细节
- "固定回调端口 + 本地回环":127.0.0.1 回调——OAuth 本地回调安全实践

## 待深挖

- [ ] mcp/catalog.ts(170):服务器目录/发现
- [ ] index.ts 的 connectLocal/status 管理细节
- [ ] MCP 工具注入 V1 注册表的桥接(tool/registry 450)
