# R-16 事务 — MULTI/EXEC/WATCH (命令队列 + CAS 观察者)

> 前置: [[R-20-server]] (命令链/标志) + [[R-21-db]] (键空间修改路径) + [[R-22-expire]] (keyIsExpired) + [[R-1-object]] (引用计数) | 引出: [[R-8-rdb-aof]] (传播包裹) + [[R-17-client-caching]] (双失效) + [[R-30-lua]] (原子性对照) | 对照: [[R-29-pubsub]] (命令上下文)
> 🟡 B | 6 KP | [模式: 命令队列 + 错误延迟爆发 + 观察者双向注册 + 失效传播]
> Pass 2 闭环: q1(队列) q2(入队错误) q3(EXEC) q4(WATCH) q5(脏传播) q6(命令面)

**读者处境**: MULTI 的原子性哪来的? WATCH 一个不存在的键有意义吗? 入队时语法错误 EXEC 会发生什么? EXEC 为什么不能有 BLPOP? 这篇拆事务: 命令队列所有权转移、DIRTY 标志、嵌入式观察者、失效传播。

### 1. 命令队列 — multiState 所有权转移

场景: MULTI 后命令去哪了? 为什么回复 QUEUED?
源码路径:
- multiState (server.h:1005-1016): commands 数组 + count + **cmd_flags (任一命令标志)** + **cmd_inv_flags (任一命令缺某标志则置位 — 判断"全部命令都有 X": flags 有 X 且 inv 无 X)** + argv_len_sums + alloc_count; multiCmd (server.h:998-1003)
- queueMultiCommand (multi.c:39-75): **DIRTY 冻结** (L46-47, 注定失败不浪费内存) → **预分配 2** (L48-53, "at least two commands" 注释) → 倍增 (L54-57)
- **argv 所有权转移** (L71-74): c->argv=NULL — 参数归 mstate, 零拷贝
- processCommand 接入 (server.c:4193-4201): CLIENT_MULTI 且非 exec/discard/multi/watch/quit/reset → 入队 + QUEUED
关键设计 (q1): **指针转移 = 零拷贝入队**; cmd_flags/inv 双累计供后续批量判断 (任一/全部)。[模式: 命令队列]
数据流: 命令 → processCommand → 入队 (argv 转移) → QUEUED。

### 2. 入队错误 — DIRTY_EXEC 延迟爆发

场景: 入队期语法错误怎么办? 有什么命令进不了事务?
源码路径:
- flagTransaction (multi.c:86-89): CLIENT_MULTI → DIRTY_EXEC
- rejectCommand 系列 (server.c:3757-3785): 统一 flagTransaction; **被拒是 EXEC → execCommandAbort** (L3764-3766)
- **CMD_NO_MULTI 禁入** (server.c:3979-3981): grep 实证仅 **4 命令**: psync/save/shutdown/sync
- DIRTY_EXEC 语义: 队列期错误 (语法/权限/参数) 记标志, **EXEC 时统一 EXECABORT** — 不中断客户端 pipeline
关键设计 (q2): **错误延迟爆发**: 入队期零报错 (只 QUEUED), EXEC 期总清算 — pipeline 友好。[模式: 错误延迟爆发]
数据流: 错误命令 → flagTransaction(DIRTY_EXEC) → 继续入队 → EXEC 时报 EXECABORT。

### 3. EXEC — 双失败路径与执行循环

场景: EXEC 什么时候返回 nil? 什么时候报错?
源码路径:
- **双失败路径** (multi.c:149-157): DIRTY_EXEC → shared.execaborterr (**"previous errors" 固定文案**, server.c:1894-1895); 仅 DIRTY_CAS → **nullarray** ("technically not an error" L146-148); **另一种 EXECABORT 来源**: EXEC 本身被拒 → execCommandAbort (动态 "because of: %s", L115-119)
- isWatchedKeyExpired 前置检查 (L139-141); 执行准备: **CLIENT_DENY_BLOCKING** (L163, 阻塞命令禁入 EXEC) → unwatchAllKeys (L166) → in_exec=1 (L168, 消费端: aof.c:964/2510, module.c:3916/7787, rdb.c:4006, networking.c:4133 client_pause_in_transaction)
- **逐命令执行** (L175-222): 恢复 mstate argv → **ACL 复查** (L184-207, "changed after the commands were queued") → call (**CLIENT_ID_AOF=UINT64_MAX 特例 CMD_CALL_NONE**, L209-212, server.h:1105) → **mstate 回写** (L218-221, "Commands may alter argc/argv")
- execCommandAbort (L115-125): -EXECABORT 错误 + **monitor 反馈** (L124)
关键设计 (q3): **单线程顺序执行 = 天然原子**; CAS 失败是值语义 (nil) vs 队列错误是协议语义 (abort); 入队后 ACL 变更复查。[模式: 事务执行]
数据流: EXEC → 检查 → 逐命令 call → 数组回复。

### 4. WATCH — 嵌入式节点双向注册

场景: WATCH 一个键, 谁碰了它怎么知道?
源码路径:
- watchedKey (multi.c:253-259): **listNode 嵌入** + key/db/client + **expired:1 位域**
- **O(1) 摘除三内联** (L262-276): 嵌入式 node.value 指回客户端列表 — "avoid listSearchKey and dictFind" (L252)
- watchForKey (L279-310): 同键去重 (L288-293) + db->watched_keys dict 双向注册 + 双引用 (L299/L307) + **expired = keyIsExpired** (L306)
- touchWatchedKey (L359-398): dict 查键 → **redis_member2struct** (L372) → **DIRTY_CAS + 立即 unwatch** (L389-393, 内存注释 L390-392)
  - **expired 键特例 (L375-387)**: WATCH 时已过期 → 键被删除则清标志跳过 (测试 multi.tcl "Delete WATCHed stale keys"); **键被 SET 重建则 break 不标记 — 与 WATCH 不存在键+SET→DIRTY 行为不同 (边界语义, 无测试覆盖, 存疑标注)**
- watchCommand (L452-467): **MULTI 内拒** (L455-458); DIRTY_CAS 早退 (L460-463)
关键设计 (q4): **嵌入式节点 = 双列表 O(1) 增删** (客户端列表 + db dict); touch 即脏 + 提前退订省内存。[模式: 观察者双向注册]
数据流: WATCH k → 双注册 → 键修改 → DIRTY_CAS + 退订 → EXEC 回 nil。

### 5. 脏传播 — signalModifiedKey 与全库失效

场景: 修改键的命令怎么通知 WATCH 者? FLUSHDB 呢?
源码路径:
- **signalModifiedKey** (db.c:620-623): touchWatchedKey + **trackingInvalidateKey** (R-17 双失效), 'c' 可为 NULL
- 覆盖面: 所有修改键命令 (setKey/dbAdd/dbDelete 等 — R-21 已讲)
- **touchAllWatchedKeysInDb** (multi.c:407-450): FLUSHDB (db.c:637) / SWAPDB (L1721-1722) / SELECT (L1767) 全量失效
  - **迭代中不能 unwatch** (L443-445): free 迭代器持有的下一节点 → **UAF 防护** (与单键路径不同!)
- isWatchedKeyExpired (L342-355): WATCH 时未过期但 EXEC 时已过期的键 → DIRTY_CAS
关键设计 (q5): **统一打点 + 批量失效**: 单键走 signalModifiedKey, 全库走 touchAll (后者只能置标志不能摘除 — UAF)。[模式: 失效传播]
数据流: SET k → signalModifiedKey → touchWatchedKey → 所有 WATCH 者 DIRTY。

### 6. 命令面与内存 — MULTI/DISCARD/WATCH/UNWATCH

场景: 四个命令的边界与资源统计?
源码路径:
- 版本: MULTI/EXEC **1.2.0** / DISCARD **2.0.0** / WATCH/UNWATCH **2.2.0** (commands.def); 全 CMD_NOSCRIPT|CMD_LOADING|CMD_STALE|CMD_FAST|CMD_ALLOW_BUSY, EXEC 额外 CMD_SKIP_SLOWLOG
- multiCommand (L91-99): 嵌套拒; discardCommand (L101-108): 无 MULTI 拒; discardTransaction (L77-82): 释放 mstate + 清三标志 + unwatch
- unwatchCommand (L469-473): 全退 + 清 DIRTY_CAS — **CAS 失败后可恢复**
- **multiStateMemOverhead** (L475-482): argv_len_sums + watched_keys×(listNode+watchedKey) + alloc_count×multiCmd; watching_clients 统计 (server.h:1995)
关键设计 (q6): **CAS 可逆**: UNWATCH 清 DIRTY_CAS 恢复; 内存面 argv 零拷贝 + 三构成统计。[模式: 命令家族]
数据流: DISCARD → 释放队列 → 清标志 → UNWATCH 全部。

### 负面空间 — 事务刻意不做的事

- **不做回滚**: 执行期命令错误不撤销已执行命令 (部分执行, 非全部或全无)
- **不做隔离**: 无快照隔离, WATCH 是唯一并发防护 (乐观锁)
- **不做嵌套事务**: MULTI 嵌套拒绝
- **不做事务内读时移**: 无 MVCC/undo log
- **不做 EXEC 内中断恢复**: 单线程要么全执行要么全不执行 (队列期)
- **不做 WATCH 多 DB 语义**: watched_keys 按 db 隔离 (wk->db)

→ 引出: 执行期原子性怎么更彻底? 对比 Lua 脚本怎么复用这套机制? → [[R-30-lua]]
