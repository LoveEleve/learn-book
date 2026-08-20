# D-1 连接池核心 — Lock+Condition 阻塞模型 (借/还/init/shutdown)

> 项目: Druid (JDBC 连接池) | 🔴 Deep / 1 篇 | DruidDataSource(3979行)+DruidAbstractDataSource(2388行)+DruidConnectionHolder(476行)+DruidPooledConnection(1298行)
> 基线: DRUID-PLAN D-1 (池核心) — 前置: **C-11 jdbc DataSource + HikariCP H-1~H-4(两极对照)** — 展开池核心全链路; 对 D-2/D-7 用导航指针 (环已按 PLAN §四化解)

---

## §0.8

- 🔴 Deep，1篇 — 入口 init(L659: 校验→SPI→driver→filter.init→数组预分配[L772-775]→同步/异步初建→三线程[L807-809]→initedLatch 同步[L812-817]→registerMbean[L822]) → 借出(getConnection[L1332]→无 filter 直连 getConnectionDirect[L1352/1366]→getConnectionInternal[L1543]: createDirect 自建[L1565]→maxWaitThreadCount 限流[L1613]→onFatalError 保护[L1620]→pollLast/takeLast[L2214/2218] notEmpty.await 阻塞→incrementUseCount+new DruidPooledConnection[L1768-1770]) → 用户使用(setAutoCommit useLocalSessionState 优化[L720-727]→transactionRecord 事务跟踪[L745-760]→handleEndTransaction 直方图[L844]) → 归还(DruidPooledConnection.close[L236] 双路径→recycle[L329]→DruidDataSource.recycle[L1894]: rollback[L1938]→holder.reset 四态[L371]→phyMaxUseCount/密码版本[L1959]→testOnReturn[L1979]→putLast+notEmpty.signal[L2029/2208]) → 收尾(close[L2081]: interrupt 三线程→cancel futures→关 PSCache+物理连接→unregisterMbean[L2136]→signalAll[L2139]→filter.destroy) → 异常联动(handleConnectionException[L1773]→isExceptionFatal→handleFatalError, shrink 驱逐来源) + 建连翻译(connectTimeout 按库属性[Abstract L1745-1793])
- 设计模式: [模式: 门面]—DruidDataSource; [模式: 条件变量]—notEmpty/empty Condition; [模式: 组合]—DataSource 聚合 ConnectionHolder 数组

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DruidDataSource.java:659,772,807,812,822 | 入口 | **init()**: lockInterruptibly→校验(连接性/数值一致性)→filter.init→connections=new DruidConnectionHolder[maxActive](L772)→三线程启动(L807-809)→initedLatch.await(L812-817)→registerMbean(L822) | High |
| DruidDataSource.java:1332,1343,1352,1366,1543 | 借出入口 | **getConnection(L1332)→filtersSize>0?createChain→dataSource_connect(L1347) : getConnectionDirect(L1352)→getConnectionInternal(L1543)** | High |
| DruidDataSource.java:1565,1613,1620,1667,2214,2218 | 借出核心 | **getConnectionInternal**: createDirect CAS 自建直连(L1565-1602)→maxWaitThreadCount 限流(L1613)→onFatalError 保护(L1620)→pollLast 带超时/takeLast 无限(L2214/2218)→notEmpty.await 阻塞→holder.discard 重试→超时抛富异常 | High |
| DruidDataSource.java:1768,1770 | 借出结尾 | **incrementUseCount→return new DruidPooledConnection(holder)** | High |
| DruidPooledConnection.java:236,329 | 归还入口 | **close 双路径**: filtersSize>0→createChain().dataSource_recycle(L274) : recycle()(L281)→recycle()[L329]→holder.dataSource.recycle(this)(L343) | High |
| DruidDataSource.java:1894,1938,1943,1959,1979,2029 | 归还核心 | **recycle**: 非同一线程锁护 reset→未提交事务 rollback(L1938)→holder.reset 四态复位(L1943/1952)→phyMaxUseCount/密码版本(L1959)→testOnReturn(L1979)→putLast+recycleCount(L2029) | High |
| DruidConnectionHolder.java:371 | 状态复位 | **reset()**: readOnly/holdability/isolation/autoCommit 四态→清监听→关 statementTrace→clearWarnings(L371-418) | High |
| DruidDataSource.java:2081,2110,2136,2139,2147 | 收尾 | **close()**: interrupt logStats/creator/destroy 三线程(L2098-2108)→cancel futures(L2110-2116)→关池内 PSCache+物理连接(L2118-2134)→unregisterMbean(L2136)→enable=false+notEmpty.signalAll(L2138-2139)→filter.destroy(L2147) | High |
| DruidDataSource.java:1773,1797 | 异常联动 | **handleConnectionException**: 广播 ConnectionEvent→exceptionSorter.isExceptionFatal(L1797)→handleFatalError(L1798) — fatalErrorCount 是 shrink 驱逐的输入 | High |
| DruidAbstractDataSource.java:243,299 | 并发模型 | **ReentrantLock lock + Condition notEmpty/empty**(L243-244/L299-300) — 数组+锁+条件变量替代无锁容器 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 池核心是 D-1 主体 — 1篇 (~60行) 按"架构总览 → 借出 → 归还 → 收尾"展开; init/shutdown/异常联动全部落在 D-1 (它们操作同一个数组+锁); 借还中的 filter 链分支对 D-2 导航 (环已化解), 验证对 D-7 导航。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | init 生命周期 (校验→数组→三线程→initedLatch) | 🔴 | **为什么🔴**: 池怎么被启动 |
| P1-2 | 借出链 (getConnectionInternal→pollLast) | 🔴 | **为什么🔴**: 连接怎么出去 |
| P1-3 | 归还链 (recycle→putLast) | 🔴 | **为什么🔴**: 连接怎么回来 |
| P1-4 | Condition 阻塞模型 (notEmpty.await/signal) | 🔴 | **为什么🔴**: Druid 并发模型核心 |
| P1-5 | shutdown 收尾 (interrupt→close→signalAll) | 🔴 | **为什么🔴**: 池怎么关 |
| P2-1 | createDirect 自建直连优化 | 🟡 | **为什么🟡**: 池空时绕过线程调度的快路径 |
| P2-2 | 异常联动 (isExceptionFatal→fatalError) | 🟡 | **为什么🟡**: 致命错误如何传导到 shrink |
| P2-3 | 三线程+initedLatch 启动同步 | 🟡 | **为什么🟡**: 线程启动时序 |
| P2-4 | 会话与事务优化 (useLocalSessionState/transactionInfo) | 🟡 | **为什么🟡**: 无状态不发指令+事务监控 |
| P2-5 | 建连翻译 (connectTimeout 按库属性) | 🟡 | **为什么🟡**: 配置归一化 |
| P3-1 | 与 HikariCP ConcurrentBag 两极对照 | 🟢 | **为什么🟢**: 两种并发模型取舍 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **架构总览 (Lock+Condition 模型+init)** | 🔴 | 并发模型定义 |
| B | **借出链** | 🔴 | 主路径 |
| C | **归还链** | 🔴 | 主路径 |
| D | **收尾+异常联动** | 🔴 | 生命周期 |

> **Cluster A (§1)**: 数组+Lock+Condition 模型 + init 启动链路
> **Cluster B (§2)**: getConnection→getConnectionInternal→pollLast + 优化/限流/保护
> **Cluster C (§3)**: close→recycle→putLast + 状态复位
> **Cluster D (§4)**: shutdown + handleConnectionException→fatalError

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | createDirect CAS | CAS creatingCountUpdater 0→1 防并发超建; 建完锁内复查池满则关闭白建 | DruidDataSource.java:1565-1602, 1649-1664 |
| q2 | maxWaitThreadCount 限流 | 等待者超阈值拒绝新借出(fail fast), 非排队 — 自建→限流→阻塞三级防御 | L1613-1618, 2244-2252 |
| q3 | rollback 先于 reset | rollback=数据正确性, reset=复用正确性, 顺序不可反; readOnly 跳过省往返 | L1936-1953, DruidConnectionHolder.java:371-418 |
| q4 | signal 三协议 | 归还 signal 精确配对 / 关闭 signalAll 广播退出 / 中断 signal 传播 | L2208, L2139, L2266 |
| q5 | init 双路径 | asyncInit+scheduler 并行建连快速返回; initedLatch 等"线程已启动"非"连接已建完" | L779-817, 2724-2737 |
| q6 | useLocalSessionState | 本地快照一致则 setAutoCommit 不发 SQL — 会话往返优化(默认 true) | DruidPooledConnection.java:720-730, Abstract:262 |
| q7 | transactionInfo 跟踪 | 池推断式事务监控: 非 autoCommit 首 SQL=起点, 无事务 rollback 跳过, 结束记直方图 | DruidPooledConnection.java:745-760, 792-823 |
| q8 | 建连翻译 | connectTimeout 按库翻译成驱动属性(5 种属性名+单位差异), 字符串缓存防重复转换 | DruidAbstractDataSource.java:1745-1796 |

→ 引出 D-2: Filter 拦截链 — 借/还入口的 filter 分支 (createChain→dataSource_connect/recycle), 环已按 PLAN §四化解, 正文用导航指针
