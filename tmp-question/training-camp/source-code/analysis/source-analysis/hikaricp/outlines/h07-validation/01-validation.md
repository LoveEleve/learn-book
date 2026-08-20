# H-7 连接验证 — connectionTestQuery / validationTimeout / isValid

> 依赖 H-5 PoolBase (复用) | 🟡 Working | 6 KP | [模式: 双策略 + 超时保护 + 状态还原]

**读者处境**: 池怎么知道连接还活着?JDBC4 isValid 和 connectionTestQuery 什么区别?验证会污染连接吗?

### 1. 验证时机与超时 — isConnectionDead

场景: 借出连接前(或 HouseKeeper)要检查连接是否可用 — 怎么限制验证耗时?

源码路径:
- `PoolBase.java:157` — **isConnectionDead(connection)**: 验证入口(供 **H-3 borrow 前校验** L166 + **KeepaliveTask keepalive 维护** L882; H-6 HouseKeeper 的 idleTimeout 淘汰不调它)
- `PoolBase.java:160` — **超时保护**: `setNetworkTimeout(connection, validationTimeout)`(L160) — 验证期间把连接网络超时设为 validationTimeout, 防验证卡死
- `PoolBase.java:162` — **验证秒**: `validationSeconds = max(1000, validationTimeout)/1000`(L162)

关键设计: **Why setNetworkTimeout(validationTimeout)？** 验证操作(如 isValid)可能阻塞 — 先缩短连接的网络超时到 validationTimeout, 保证验证在时限内完成, 不拖死借出线程; 结束后 finally 恢复(§3)。[模式: 超时保护]

数据流: 借出前/HouseKeeper → isConnectionDead(conn)(L157) → setNetworkTimeout(validationTimeout)(L160) → 按方式验证(§2) → finally 恢复。

### 2. 验证方式 — isValid vs connectionTestQuery

场景: 两种验证连接的方式 — 什么时候用哪个?

源码路径:
- `PoolBase.java:92,112` — **方式判定**: `isUseJdbc4Validation`(L92) = `config.getConnectionTestQuery() == null`(L112) — **没配 test query 就用 JDBC4 isValid**
- `PoolBase.java:164,165` — **JDBC4**: `if (isUseJdbc4Validation) return !connection.isValid(validationSeconds)`(L164-165) — 驱动自带 isValid
- `PoolBase.java:170,173` — **测试查询**: `setQueryTimeout(statement, validationSeconds)`(L170) + `statement.execute(config.getConnectionTestQuery())`(L173) — 执行自定义测试 SQL

关键设计: **Why 自动选 isValid？** 现代驱动(JDBC4)支持 `Connection.isValid()` 内置校验, 无需测试 SQL — 所以未配 connectionTestQuery 时默认用 isValid(更快更安全); 配了则用测试查询(老驱动/特殊场景)。**Why validationSeconds 同步给 query timeout？** 测试查询也要限制在 validationTimeout 内, 不无限等待。[模式: 双策略自动选择]

数据流: isUseJdbc4Validation(L112 判定) → 是: connection.isValid(validationSeconds)(L165); 否: setQueryTimeout(validationSeconds)(L170)→execute(connectionTestQuery)(L173) → 抛异常=连接死。

### 3. 验证后清理 + 配置 — restore / rollback / connectionTestQuery

场景: 验证完怎么保证连接状态不被污染?测试查询怎么配?

源码路径:
- `PoolBase.java:177` — **恢复**: `setNetworkTimeout(connection, networkTimeout)`(L177) — 恢复原网络超时
- `PoolBase.java:179` — **隔离**: `if (isIsolateInternalQueries && !isAutoCommit) connection.rollback()`(L179) — 隔离内部查询(测试 SQL)对事务的影响
- `HikariConfig.java:76,369` — **配置**: `connectionTestQuery` 字段(L76) + `getConnectionTestQuery()`(L369) — 用户可配测试 SQL

关键设计: **Why finally 恢复？** 验证改变了连接的网络超时(validationTimeout)— 必须恢复原值(networkTimeout), 否则后续业务连接超时错误; rollback 隔离测试查询产生的未提交影响。**Why connectionTestQuery 可配？** 默认 isValid, 但老驱动/特殊库需自定义测试 SQL(如 `SELECT 1`) — 配了就切到测试查询模式。[模式: 状态还原 + 可配置]

数据流: 验证完 → finally: setNetworkTimeout(networkTimeout)(L177) → isIsolateInternalQueries?→ rollback(L179) → 返回 alive/dead。配置 connectionTestQuery(L76) → L112 判定 → 用测试查询而非 isValid。

→ 引出 H-12: 代理生成 — 验证之后: ProxyFactory/Javassist 生成 ProxyConnection 代理族(前置 s24-s28 aop)。
