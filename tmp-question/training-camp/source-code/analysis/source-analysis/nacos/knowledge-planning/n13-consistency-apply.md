# N-13 一致性落点 — 知识规划 (KP)

> 🟡 B | 模块: naming/consistency (10 文件) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 数据处理 | DistroClientDataProcessor | onReceive 落地 |
| 2 | 组件注册 | DistroClientComponentRegistry | 五件套注册 |
| 3 | 传输代理 | DistroClientTransportAgent | 同步传输 |
| 4 | 失败处理 | DistroClientTaskFailedHandler | 失败兜底 |
| 5 | 校验 | DistroClientVerifyInfo | verify 数据 |
| 6 | 快照基类 | AbstractSnapshotOperation | 批量读写 |
| 7 | 操作代理 | ClientOperationServiceProxy | 临时/持久路由 |
| 8 | 键契约 | Datum/KeyBuilder | 数据寻址 |

## 02 高频坑
1. 组件按 resourceType 注册 (NC-5 消费)
2. 同步载体是 ClientSyncData
3. 快照走批量协议
4. 操作代理分临时/持久

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| Distro 面 | 五件套 / verify |
| 快照面 | AbstractSnapshotOperation / 批量 |
| 代理面 | Proxy / 双实现 |
| 契约 | Datum / KeyBuilder |

## 04 跨域桥接
- ← NC-5: 协议注册面
- ← N-10: ClientSyncData
- → 面试: "数据怎么进一致性协议" — 组件注册 + 快照批量
