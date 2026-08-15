# q15 — ACP(Agent Client Protocol 服务端)

> 域:集成协议 | 文件:opencode/src/acp/(service 1105/event 421/tool 364/permission 254/session 232/usage 243/content/config-option/directory/profile/error 共 12 文件)+ @agentclientprotocol/sdk
> review 轮次:1 轮(服务结构 + 协议面)

---

## 假设

ACP = Zed 等编辑器的 Agent Client Protocol 集成。OpenCode 实现为"SDK 之上的适配层":把 ACP 会话操作(session/new/load/resume/fork/close)+ 提示词 + MCP 能力映射到 opencode 的会话 API。

## 验证

### 1. 能力面(设计 1:initialize 声明)

```ts
// service.ts:94-139
protocolVersion 1
agentCapabilities:
  loadSession / mcpCapabilities{http, sse} / promptCapabilities{embeddedContext, image}
  sessionCapabilities{close, fork, list, resume}
authMethods:[{ id: "opencode-login", description: "Run `opencode auth login`", terminal-auth 命令 }]
agentInfo:{ name: "OpenCode", version: InstallationVersion }
```

### 2. 会话操作(设计 2:new/load/resume/fork/close)

```ts
// service.ts:163-209 newSession:
1. directorySnapshot(cwd)(模型/模式发现)
2. selectDefaultModel + selectVariant + modeId
3. sdk.session.create(权限/模型)
4. session.create(内部状态:snapshot/mcpServers 缓存)
5. registerMcpServers + sendAvailableCommands
// loadSession(211):快照 + request 加载
// resumeSession(292):恢复 + abortBackingSession(330):取消 backing 会话
// forkSession(356):分叉会话
// closeSession(341):关闭
```

### 3. Prompt 处理(设计 3:promptResponse + content 转换)

```ts
// service.ts:824 promptResponse:ACP PromptRequest → opencode prompt
// content.ts:promptContentToParts:ACP content 结构 → opencode parts(embeddedContext/image 支持)
```

### 4. MCP 服务器(设计 4:注册 + 转发)

```ts
// newSession 时 registerMcpServers:ACP 客户端声明的 MCP 服务器 → opencode MCP 注册
// McpServer 配置转发(service.ts:196)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | ACP 能力面声明(loadSession/mcp/prompt/session) | service.ts:94-139 | ②集成协议 |
| 2 | 会话操作映射(new/load/resume/fork/close) | service.ts:163-356 | ②外部宿主集成 |
| 3 | Prompt 转换(embeddedContext/image) | service.ts:824 + content.ts | ②协议适配 |
| 4 | MCP 服务器注册转发 | service.ts:196 | ②MCP 复用 |

## 面试弹药

- "ACP = SDK 适配层":不重实现会话核心,把 ACP 协议映射到 opencode 会话 API——外部协议集成范式
- "terminal-auth 能力协商":client 声明 terminal-auth → 返回 auth login 命令(能力协商而非硬编码)
- "目录快照驱动配置":newSession 先发现 cwd 的模型/模式再创建——配置来自环境

## 待深挖

- [ ] ACP event.ts(421 行)的事件流映射
- [ ] ACP permission/tool 的协议化(把 opencode 权限暴露给外部客户端)
