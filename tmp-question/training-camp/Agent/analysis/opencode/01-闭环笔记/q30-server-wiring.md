# q30 — 应用层接线(深度版:Server 生命周期 + 双路由面)

> 域:应用组装 | 文件:opencode/src/server/(server.ts 226/mdns.ts 47/routes/)+ opencode/src/cli/(run.ts 1011/cmd/*)+ server/routes/instance/httpapi/
> review 轮次:1-2 轮(源码全文核心)

---

## 假设

Server = "Effect HttpRouter + Scope 生命周期"的组装点:HttpApiApp(双路由面:webHandler 全栈 + createRoutes 路由)、端口回退、优雅停止、mDNS 发布。CLI run 是 TUI/Server 的启动编排。

## 验证

### 1. 双路由面(设计 1:webHandler vs createRoutes)

```ts
// server.ts:56-65,100-114
Default = HttpApiApp.webHandler()(全栈 fetch 处理——Embedded 用)
listen = HttpRouter.serve(HttpApiApp.createRoutes(opts), { middleware: disposeMiddleware })
  + WebSocketTracker + serverLayer + 每监听器新 ConfigProvider(env 快照问题)
// 关键:嵌入式(Default)与网络(createRoutes)共用同一 HttpApiApp——边界一致(CONTEXT.md:139)
```

### 2. 生命周期(设计 2:Scope + 幂等停止)

```ts
// server.ts:124-193
startListener:Scope.makeUnsafe + Layer.buildWithMemoMap → ListenerState{scope, server, http, websockets}
startWithPortFallback:端口 0 → 先试 4096,再任意空闲端口(兼容 legacy)
makeStop:forceCloseOnce(cached)+ closeScopeOnce(cached)——stop(close=true)强制关连接
// mDNS:hostname 非回环才发布;Effect.cached 幂等 unpublish
// server.close monkey-patch:forceStop 时 closeAllConnections
```

### 3. ConfigProvider 每监听器重建(设计 3:env 快照问题)

```ts
// server.ts:108-113 注释:
// Effect 默认 ConfigProvider 首次读取快照 process.env 并缓存到模块单例 Reference;
// 不覆盖的话,后续 Server.listen() 一直看到旧 env 快照。
// 解法:每个 listener 注入 ConfigProvider.layer(ConfigProvider.fromEnv())
```

### 4. 初始化顺序(设计 4:init-projectors)

```ts
// server.ts:1 "import ./init-projectors"(副作用:注册投影器)
// global-lifecycle.ts:启动全局生命周期(投影器/清理)
```

### 5. CLI 编排(设计 5:run.ts 启动)

```ts
// cli/cmd/run.ts(1011 行):TUI 启动 + Server.listen + 事件桥接
// cli/bootstrap.ts:环境引导(安装/升级/配置)
// cli/upgrade.ts:自更新
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 双路由面(webHandler/createRoutes 共享 HttpApiApp) | server.ts:56-114 | ②嵌入式一致性 |
| 2 | Scope 生命周期 + 幂等停止 + 端口回退 | server.ts:124-193 | ②服务生命周期 |
| 3 | ConfigProvider 每监听器重建(env 快照) | server.ts:108-113 | ②配置新鲜度 |
| 4 | 副作用导入注册投影器 | server.ts:1 | ④启动注册 |
| 5 | CLI 编排(TUI+Server+事件) | cli/run.ts | ②应用组装 |

## 面试弹药

- "嵌入式与网络共享 HttpApiApp":webHandler 全栈 vs createRoutes 路由,同一契约——Embedded 不另起炉灶
- "env 快照是坑":Effect ConfigProvider 模块单例缓存——每监听器重建才新鲜
- "stop(close) 幂等":forceCloseOnce/closeScopeOnce 用 Effect.cached——多次 stop 安全
- "端口 0 → 4096 → 任意":兼容 legacy 的端口解析行为

## 待深挖

- [ ] routes/instance/httpapi/(server/routes/handlers 组织)
- [ ] WebSocketTracker(连接跟踪)
- [ ] CLI run.ts 的 TUI/Server 交互细节
