# N-14 命名集群与监控 — 深审

> 方案 B 六层

## 审 1: 事实错误 — 1 修正
| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | NC-6 未展开命名监控/远端面 | N-14: ServerStatusManager + TPS 族 + gRPC handler 族三面 |

## 审 2: API/实现路径编造 — 1 修正
| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "状态只有 UP/DOWN" | 实测: **三态 (UP/DOWN/READY_ONLY)** + NamingReadinessCheckService — 状态机精确化 |

## 审 3-6: 0 修正

## 结论: 2 项修正落盘
