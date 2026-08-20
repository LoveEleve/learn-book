# D-1 连接池核心 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 连接存哪?借出取哪个位置? | §1 (connections[maxActive] 数组) / §2 (取尾 L2271-2273) |
| 2 | getConnection 池空时会发生什么? | §2 (notEmpty.await 阻塞 + createDirect 快路径) |
| 3 | conn.close() 后连接经历什么? | §3 (rollback→reset 四态→putLast) |
| 4 | 高峰期 getConnection 会不会无界堆积? | §2 (maxWaitThreadCount 限流 L1613) |
| 5 | 用户改了 autoCommit 会影响下个用户吗? | §3 (holder.reset 四态恢复 L371) |
| 6 | 重复 setAutoCommit(true) 会发 SQL 给数据库吗? | §3 (useLocalSessionState 本地一致跳过 L725-727) |
| 7 | 事务里的 SQL 被记录吗?事务耗时怎么统计? | §3 (transactionRecord L745 / handleEndTransaction 直方图 L844) |
| 8 | connectTimeout 怎么变成驱动属性? | §1 (按库翻译 L1745, MySQL/Oracle/PG 各不同) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么用 Lock+Condition 而不用 Hikari 的 ConcurrentBag? | §1 (单锁保证状态一致+条件等待) / §2 (createDirect 补偿) |
| 7 | 为什么 maxWaitThreadCount 拒绝比无限等待好? | §2 (防惊群/防堆积, 富异常反馈) |
| 8 | 为什么 close 用 signalAll 不用 signal? | §4 (关闭瞬间全部唤醒各自退出) |
| 9 | 数据库故障时池怎么隔离? | §4 (isExceptionFatal→fatalError→借出受限+shrink 驱逐) |
| 10 | 归还时 rollback 为什么在 reset 之前? | §3 (先防脏事务再复位状态) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 11 | init() 做了什么?三线程是哪三个? | §1 (校验→数组→线程→latch→MBean) |
| 12 | 借出链和归还链分别几步? | §2/§3 (三级调用 vs close→recycle→putLast) |
| 13 | Druid 和 Hikari 的并发模型差在哪? | §1/§2 (锁+条件变量 vs 无锁容器) |
| 14 | 超时异常里都有什么信息? | §2 (active/maxActive/creating/runningSql) |

## 覆盖: 14 问 / 3 身份 / 100%
