# D-8 PreparedStatementPool 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | PSCache 容量怎么控制?默认多少? | §1 (removeEldestEntry + 默认 10 per-connection L118) |
| 2 | 同一个 statement 会被两个线程同时用吗? | §2 (in-use 保护 L58-60, 共享需显式配置) |
| 3 | 执行异常过的 statement 还入缓存吗? | §2 (exceptionCount>0 → remove L183) |
| 4 | 怎么知道缓存命中率? | §3 (hit/miss 计数 L63/68) |
| 5 | 什么时候物理新建 statement? | §2 (未命中 L369 / 不缓存时 closeInternal L190) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么用 LinkedHashMap 实现 LRU? | §1 (accessOrder=true + removeEldestEntry 零手写) |
| 7 | 为什么缓存必须 per-connection? | §1 (statement 绑定物理连接) |
| 8 | 键为什么要全参数? | §3 (不同创建参数的 statement 不兼容) |
| 9 | 为什么 exceptionCount==0 才入池? | §2 (坏状态污染下次复用) |
| 10 | 与 Hikari 不缓存 statement 的取舍? | §1/§3 (Druid 省解析换内存与正确性复杂度; Hikari 追求极致简单) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 11 | LRU 是什么?淘汰谁? | §1 (最久未用, removeEldestEntry) |
| 12 | PreparedStatementKey 包含哪些字段? | §3 (sql+catalog+methodType+resultSet 参数) |
| 13 | Oracle 的隐式缓存是什么? | §3 (驱动自带缓存, Druid 进出适配) |
| 14 | close 后 statement 去哪? | §2 (三分支: 入池/剔除/物理关) |
| 15 | Oracle 行预取怎么调? | §3 (fetchRowPeak 驱动自适应: ≤1→2/>默认→默认/否则 peak+1) |
| 16 | fetchRowPeak 为什么只增不减? | §3 (历史峰值, 保守估计需求) |

## 覆盖: 14 问 / 3 身份 / 100%
