# D-2 服务导出 — Pass 1 探索笔记

> 域: D-2 服务导出 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: dubbo-config-api/ServiceConfig (1202) + dubbo-rpc-dubbo/DubboProtocol (661) | Dubbo 3.3.7-SNAPSHOT

## 调用图

```
export() (L326) → doExport → doExportUrls (L582: loadRegistries + 遍历 protocols)
  → doExportUrlsFor1Protocol (L628): buildAttributes (map) → buildUrl → processServiceExecutor → exportUrl
    → exportUrl (L650): scope 三态:
        SCOPE_NONE → 不导出
        exportLocal (injvm: LOCAL_PROTOCOL + port 0 + EXPORTER_LISTENER)
        exportRemote: 遍历 registryURLs → doExportUrl (registryURL + EXPORT_KEY 属性)
          + EXT_PROTOCOL 附加协议 + MetadataUtils.publishServiceDefinition
    → doExportUrl (L978-993): proxyFactory.getInvoker(ref, interfaceClass, url)
        → DelegateProviderMetaDataInvoker → protocolSPI.export(invoker) → exporters 收集
protocolSPI = Protocol 自适应扩展 (L188, D-1 消费面) — Wrapper 链织入

DubboProtocol.export (L346-375): DubboExporter (invoker+key+exporterMap) → openServer(url) → optimizeSerialization
  → openServer (L377-405): serverMap 按地址缓存 + 双检锁 → createServer; 已存在 → server.reset (override)
  → createServer (L407-430): URL 补参数 (READONLYEVENT/HEARTBEAT 默认/CODEC=DubboCodec) → Exchangers.bind (Netty)
```

## 基本元素分解

1. **export 链**: export → doExport → doExportUrls → 1Protocol → exportUrl → doExportUrl (6 层)
2. **scope 三态**: NONE/LOCAL (injvm)/REMOTE (registry) + 本地始终导出
3. **invoker 生成**: proxyFactory.getInvoker + DelegateProviderMetaDataInvoker
4. **Protocol 自适应**: protocolSPI.export (D-1 消费 + Wrapper 织入)
5. **DubboProtocol**: DubboExporter + serverMap 缓存 + reset + Exchangers.bind

## 标记问题 (20 问)

1. export 入口? (同步 + 双检)
2. doExportUrls? (loadRegistries + 多协议)
3. buildAttributes? (map 组装)
4. scope 三态? (NONE/LOCAL/REMOTE)
5. exportLocal? (injvm: port 0)
6. exportRemote? (registry 遍历 + EXT_PROTOCOL)
7. doExportUrl? (proxyFactory + protocolSPI.export)
8. proxyFactory? (JavassistProxyFactory 默认)
9. protocolSPI? (Protocol 自适应 — D-1)
10. DelegateProviderMetaDataInvoker? (元数据包装)
11. DubboExporter? (invoker+key+exporterMap)
12. openServer? (serverMap 缓存 + 双检)
13. reset? (override 支持)
14. createServer? (URL 参数 + Exchangers.bind)
15. HEARTBEAT 默认? (DEFAULT_HEARTBEAT)
16. optimizeSerialization? (序列化优化)
17. exporters 收集? (registerType 分组)
18. 对照 ZK Netty? (4.3 NettyServer)
19. 延迟导出? (delay)
20. unexport? (销毁面)

## 时空溯源 (代码内注释锚)

- ServiceConfig:188 protocolSPI = Protocol 自适应 (2.x 锚)
- DubboProtocol:407-430 createServer URL 参数注释 (2.x 锚)
- DubboProtocol:230 "FIXME channel.getUrl() always binds to a fixed service" (历史注释)
- RegisterTypeEnum (AUTO/MANUAL/NEVER) — 3.x 注册模式

## 大域拆分判断

D-2 = 服务导出面 (export 链 + scope 三态 + DubboProtocol); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "ServiceConfig.export()—Protocol.export—Netty 启动" | 全链实证 (export 6 层 → protocolSPI.export → openServer → Exchangers.bind) | **接受** ✅ |
| 数字: scope 三态 | NONE/LOCAL/REMOTE + 本地始终导出 (exportUrl) | **补充** ✅ |
| 数字: RegisterType | 3 值 (AUTO/MANUAL/NEVER) — 执行计划未提 | **补充** ✅ |
| 对照 ZK (4.3) | NettyServer vs Dubbo Netty — 服务端对照 | **接受** ✅ |
