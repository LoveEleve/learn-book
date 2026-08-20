# N-10 客户端管理面 — 知识规划 (KP)

> 🔴 A | 模块: naming/core/v2 (53 文件 9,191 行) | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 门面 | ClientService/ClientServiceImpl:57 | 查询委托 manager |
| 2 | 三管理器 | ConnectionBased/EphemeralIpPort/PersistentIpPort | 连接模型分型 |
| 3 | 委托聚合 | ClientManagerDelegate | 对外统一 |
| 4 | 客户端模型 | AbstractClient:44 | 状态+同步双载体 |
| 5 | 同步数据 | AbstractClient:138-164 | ClientSyncData |
| 6 | 工厂族 | ClientFactory 三实现 | 创建与使用分离 |
| 7 | 操作面 | ClientOperationService:36 + 双实现 (Persistent 503 行) | 临时/持久 |
| 8 | 清理器 | NamingCleaner 族 | EmptyService/ExpiredMetadata |
| 9 | 事件发布 | NamingEventPublisher:190 | 客户端事件 |
| 10 | 索引/元数据 | NamingFuzzyWatchContextService:338 | 模糊订阅索引 |

## 02 高频坑

1. clientId 是 3.x 核心 — 连接/注册/订阅全挂它
2. 三管理器分型 (grpc/临时/持久) — delegate 聚合
3. generateSyncData 是集群同步载体
4. 2.x API @Deprecated (getPublishedServiceListAdapt)
5. 重复连接 putIfAbsent 首客户端胜出
6. release = manager.remove + client.release 双动作
7. 操作面临时/持久双实现 (对应 Distro/JRaft)

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 门面 | ClientService / 委托 / 2.x 遗留 |
| 管理器 | 三实现 / delegate / syncClientConnected |
| 模型 | AbstractClient / ConnectionBased / IpPortBased / Attributes |
| 工厂 | 三工厂 / Holder |
| 操作 | ClientOperationService 接口 / Ephemeral / Persistent 变体 |
| 横切 | cleaner / event / index / metadata |

## 04 跨域桥接

- ← NC-6: InstanceOperator 消费落点
- → NC-11: 健康检查消费客户端状态
- ↔ NC-1: 客户端 clientId 两端闭合
- → 面试: "Nacos 3.x 客户端模型" — clientId + 三管理器 + 同步数据
