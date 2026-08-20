# D-2 服务导出 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: ServiceConfig.export 链 + DubboProtocol (DubboExporter + openServer + Exchangers.bind) + injvm 本地导出 |
| 2.7.x | scope 三态 + 延迟导出 (delay) + MetadataUtils.publishServiceDefinition (元数据中心) |
| 3.x | **RegisterTypeEnum (AUTO/MANUAL/NEVER)** + EXT_PROTOCOL 附加协议 + DelegateProviderMetaDataInvoker + processServiceExecutor (executor 隔离) |
| 3.3.x | ServiceConfig 1202 / DubboProtocol 661 稳定 |

## 痕迹证据

- ServiceConfig.java:188: protocolSPI = Protocol 自适应 (2.x 锚)
- DubboProtocol.java:230: "FIXME channel.getUrl() always binds to a fixed service" (历史注释锚)
- DubboProtocol.java:407-430: createServer URL 参数 (2.x 锚)
- ServiceConfig.java:978-993: doExportUrl invoker 链 (2.x 锚)
- RegisterTypeEnum: 3.x 注册模式 (3.x 锚)

## 推断标注

- "2.x 骨架" — Dubbo 2.x 公知版本线 (标注)
- "2.7.x 元数据中心" — MetadataUtils 存在性推断 (标注)
- "3.x RegisterType" — 枚举实证 (实证)
- git 多 commit 可考古 — 本域以注释锚 + 枚举为主

## 对照线 (阶段 4.3 已交付)

- ZK NettyServer (4.3): NIO/Netty 双实现 vs Dubbo Netty (Transporter SPI) — 服务端对照
- ZK 会话面 vs Dubbo 连接管理 — 网络层对照
