# N-10 客户端管理面 — 时空溯源 (3.0.3 实证)

> 演进证据取自代码结构/Deprecated 标注/1.x↔3.x 对照

## 演化主线: 无客户端模型 → clientId 核心模型

| 时代 | 机制 | 证据 (3.0.3 源码) | 演进信号 |
|:--|:--|:--|:--|
| 1.x | 无客户端概念 (ip:port 维度) | — | 被替代 |
| 2.x | 2.x http API | **@Deprecated getPublishedServiceListAdapt** (ClientService) | "with removing 2.x http api" |
| 3.x | clientId 模型 | ClientService + ClientManager 三实现 + v2/client 族 | 主线 |
| 同步 | ClientSyncData | AbstractClient.generateSyncData (L138-164) | Distro 面数据 |

## 关键事件锚

- **@Deprecated 注释** (ClientService): "with removing 2.x http api. use getPublishedServiceList replaced" — 2.x 面移除路径
- **日志锚** (AbstractClient:82/97): "Client change for service {}" / "Client remove for service {}"
- **三管理器分型** (v2/client/manager/impl/): ConnectionBased / EphemeralIpPort / PersistentIpPort — 连接模型演进
- **NamingEventPublisher** (v2/event/publisher:190): 客户端事件独立发布器

## 架构递进逻辑

```
1.x: ip:port 维度注册 (无连接概念)
2.x: http API + 客户端雏形
3.x: clientId 核心 (gRPC 连接/临时/持久三型) + 工厂 + 清理 + 同步数据
```
→ 演进方向: 从"实例维度"到"客户端维度" — 连接、注册、订阅全部挂 clientId, 集群同步走 ClientSyncData。
