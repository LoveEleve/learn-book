# q12 — SDK/API/Typert + Hooks/ACP(深度版:协议面)

> 域:④知识库(协议面)+ 集成 | 文件:packages/(sdk/protocol 440 + server 362 + client 952)+(api/gateway + remotes)+(typert/generator 6245 + registry 909 + loader 472 + protocol)+(hooks/*)+(acp/acp)+ mcp/
> review 轮次:2 轮(源码结构 + 接口面)

---

## 假设

协议面分三层:SDK(JSON-RPC 协议/服务器/TS 客户端)、API(Typert RPC 网关/BFF)、Typert(类型图生成器——从 TS 源码生成类型注册表)。Hooks = Claude Code/Codex hook 桥;ACP = 自动化 ACP 服务器;MCP = 集成。

## 验证

### 1. SDK(设计 1:JSON-RPC 三件)

```ts
// sdk/protocol(440):JSON-RPC 协议 + JsonRpcTransportPeer(transport.ts)
// sdk/server(362):JSON-RPC 服务器
// sdk/client(952):TypeScript 客户端
// ——subagent-dsh-sdk provider 用它委托
```

### 2. API + Typert(设计 2:类型图生成器)

```ts
// api/gateway:TypertGatewayService + TypertGatewayError(远程 BFF 组装)
// api/remotes:agent-lookup + remote-events(远程事件)
// typert/generator(6245):WorkspaceAnalyzer(编译器无关模型)+ FaceModelEmitter +
//   TypeGraphRenderer + cordis-catalog(生成器——从 TS 源码生成类型图/目录)
// typert/registry(909):运行时注册表;typert/loader(472):加载器;typert/protocol:协议
// ——"type graph generator, loader, and runtime registry"(AGENTS.md:16)
//   从源码生成类型目录(文档/API 面自动生成的基础)
```

### 3. Hooks(设计 3:外部 hook 桥)

```ts
// hooks/hook-protocol:codec/detached/events/matcher/merge/runner(线协议库)
// hooks-claude-code + hooks-codex:Claude Code/Codex hook 桥
// ——把 dsh 会话接到外部工具的 hook 体系(审批/权限经 hook)
```

### 4. ACP(设计 4:自动化协议服务器)

```ts
// acp/acp:name='acp' + inject=['agents'] + Config + apply
// ——automation-only Agent Client Protocol 服务器(架构:非交互)
// 注意:packages/AGENTS.md 提到 postmortem 0001(ACP 默认导出丢弃 inject——坑记录)
```

### 5. MCP(设计 5:集成)

```ts
// mcp/:MCP 集成(外部工具协议)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | SDK JSON-RPC 三件 | sdk/* | ②对外协议 |
| 2 | Typert 类型图生成器(6245 行) | typert/generator | ④文档/API 自动生成 |
| 3 | Hook 桥(Claude/Codex) | hooks/* | ②外部集成 |
| 4 | ACP 自动化服务器 | acp/acp | ②自动化 |
| 5 | MCP 集成 | mcp/ | ②外部工具 |

## 面试弹药

- "Typert = 从源码生成类型目录":编译器无关分析器 + 发射器——dsh 的文档/API 面自动生成(类型即真相)
- "hook-protocol 是线协议库":codec/matcher/runner 独立于 claude/codex 实现——协议与桥分离
- "ACP 是 automation-only":非交互协议服务器——与 OpenCode 的 ACP(编辑器集成)定位不同

## 待深挖

- [ ] typert generator 的分析器(6245 行)
- [ ] sdk client 的传输层
- [ ] hook-protocol 的 matcher/merge
