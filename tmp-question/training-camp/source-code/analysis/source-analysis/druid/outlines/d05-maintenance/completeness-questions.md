# D-5 维护体系 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 谁周期性地跑 shrink?多久一次? | §1 (DestroyConnectionThread sleep(timeBetweenEvictionRunsMillis) L2869) |
| 2 | shrink 把连接分几类处理? | §2 (四阶段: fatalError/驱逐/keepAlive/紧凑) |
| 3 | keepAlive 的连接验证成功/失败去哪? | §3 (成功 put 回池, 失败 discard+补连) |
| 4 | removeAbandoned 会误杀正在执行的连接吗? | §4 (isRunning 跳过 L2943) |
| 5 | 驱逐后数组有洞怎么办? | §2 (System.arraycopy 紧凑 L3151) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么 checkCount = poolingCount - minIdle? | §2 (只逐超出 minIdle 部分, maxEvictable 是绝对上限) |
| 7 | 为什么 keepAlive 逆序 put? | §3 (最新连接先被借出, 老连接沉淀淘汰) |
| 8 | 为什么两个创建通道 (线程+Task)? | §1 (有 scheduler 并行扩池, 无则单线程省资源) |
| 9 | 与 Hikari H-6 HouseKeeper 的差异? | §2 (Druid 四阶段批量 vs Hikari 借出时淘汰; Druid 保底 minIdle vs Hikari 收到 minIdle) |
| 10 | fatalError 增量驱逐解决什么问题? | §2 (故障后老连接不信任, 强制重新验证) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 11 | 后台有哪几个线程? | §1 (Creator/Destroyer/LogStats 三线程) |
| 12 | minEvictable 和 maxEvictable 区别? | §2 (min 逐超额部分, max 绝对上限) |
| 13 | keepAlive 验证用什么? | §3 (validateConnection → D-7 的 checker) |
| 14 | removeAbandoned 默认开吗? | §4 (默认关, 生产警告) |
| 15 | running 标记什么时候置位? | §4 (仅 removeAbandoned 开启时, SQL 执行前后 beforeExecute/afterExecute) |
| 16 | 建连失败后 Creator 线程还等吗? | §1 (emptyWait: createError 非空且池空且无 discard 变化 → 不等, 先重试) |

## 覆盖: 14 问 / 3 身份 / 100%
