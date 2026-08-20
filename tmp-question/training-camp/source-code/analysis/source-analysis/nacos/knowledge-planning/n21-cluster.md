# N-21 集群管理面 — 知识规划 (KP)

> 🔴 A | 模块: core/cluster (22 文件 2,807 行) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 成员表 | ServerMemberManager:92/110 | ConcurrentSkipListMap 有序 |
| 2 | 自注册 | ServerMemberManager:169 | self 入表 |
| 3 | 健康上报 | ServerMemberManager:137/149 | 上报时间+不健康任务 |
| 4 | Lookup 接口 | MemberLookup:30/37/58 | start+afterLookup |
| 5 | 工厂判定 | LookupFactory:50/107-130 | 三类型选择 |
| 6 | 三 Lookup | Standalone/File/AddressServer | 节点来源 |
| 7 | 变更事件 | MembersChangeEvent | 集群感知 |
| 8 | 上报处理 | MemberReportHandler | gRPC 上报 |
| 9 | 集群代理 | ClusterRpcClientProxy:259 | 成员请求 |
| 10 | 模块健康 | ModuleHealthCheckerHolder | 自检聚合 |

## 02 高频坑
1. serverList 是有序表 (ConcurrentSkipListMap)
2. Lookup 工厂默认 address-server (非 standalone)
3. 自注册在启动时 (L169)
4. 不健康成员被排除 (getMemberAddressInfos)
5. 成员变更事件驱动 Distro 等
6. 单机 Lookup 是特例

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 成员 | 有序表 / 自注册 / 健康上报 |
| Lookup | 三实现 / 工厂 / 回调 |
| 事件 | MembersChangeEvent / Listener / 上报 |
| 健康 | 模块自检 / ReadinessResult |

## 04 跨域桥接
- ← NC-5: Distro memberManager 消费
- ← N-14: 状态机消费
- ↔ N-04: 客户端地址面对称
- → 面试: "Nacos 集群成员管理" — 有序成员表 + Lookup 族 + 变更事件
