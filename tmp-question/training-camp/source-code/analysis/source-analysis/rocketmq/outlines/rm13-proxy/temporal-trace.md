# RM-13 Proxy+安全 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 5.0 (2022-) | **proxy 初版**: gRPC v2 协议 (GrpcMessagingApplication) + remoting 兼容接入 (RemotingProtocolServer 8080) + LOCAL/CLUSTER 双模式 + MessagingProcessor 统一消息面 — 客户端可经 proxy 拿路由 (RM-11 弱化交叉) |
| 5.1+ | **auth 模块独立**: authentication/ + authorization/ 分治 (chain 管线) + Stateful/Stateless 双策略 + 元数据 provider; 管线 pipe 反转 (ContextInit→Authn→Authz) |
| 5.x | **AuthMigrator** (acl→auth 迁移工具) + http2proxy 面 (gRPC↔remoting HTTP/2 辅助) + POP 语义统一 (LITE_PULL/POP 码) + 内嵌客户端认证凭据 (CLUSTER 模式) |

## 痕迹证据

- ProxyConfig.java:89,226,240: 三端口 (8081/8080/5557)
- GrpcMessagingApplication.java:145-158: pipe 反转管线 (authConfig 驱动注释)
- RequestPipeline.java:28-33: pipe 语义 (source 先执行)
- AclSigner.java:28-39: DEFAULT_ALGORITHM=HmacSHA1 (acl 复用 — auth 模块依赖 acl 签名器)
- AuthMigrator.java: 独立迁移工具类 (5.x)
- RemotingProtocolServer.java:191-217: 请求码注册 (SEND/PULL/LITE_PULL/POP 年代梯度)
- auth/ + acl/ + proxy/ 三模块依赖: proxy→auth→acl (pom 面, 迁移链证据)

## 推断标注

- "5.0 proxy 初版" — 公知版本线 (proxy 5.0.0 引入) (标注)
- "5.1+ auth 独立" — auth 模块独立于 acl 的架构年代推断 (标注); AuthMigrator 存在暗示 acl→auth 迁移发生在 auth 成熟后
- "http2proxy 面" — 目录存在推断 (标注); 未做 git 考古
