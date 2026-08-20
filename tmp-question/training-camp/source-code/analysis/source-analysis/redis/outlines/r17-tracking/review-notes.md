# R-17 客户端缓存 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **机制洞察** | **BCAST 过滤 (tracking.c:378-380) 的真正动机 = 惰性清理的残留防线**: disableTracking 不删表项 (L42-45 惰性), 客户端 off→on(BCAST) 后**旧条目残留在主表**; 若无此过滤, 会收到 off 前的陈旧失效消息。测试 L383-396 "After switching from normal tracking to BCAST mode, no invalidation message for pre-BCAST keys" 实证 | 大纲节 3 补注 |
| 2 | **机制洞察** | **pending + execution_nesting = 输出缓冲保序设计**: RESP3 push 与命令响应**共享输出缓冲**; 穿插会破坏客户端 RESP 解析。afterCommand (server.c:3796-3809) 顺序 = postExecutionUnitOperations (传播收尾) → trackingHandlePendingKeyInvalidations — "reply to client before invalidating cache" (L3798-3799 注释); 缓冲语义: 响应字节已在缓冲, 推送追加在后 | 大纲节 3 |
| 3 | **覆盖缺口** | **客户端内存记账不含主表归属**: clientMemUsage (networking.c:3874-3876) 仅前缀低估记账; 主表 rax 节点全局进 used_memory 但**不归属任何客户端** → 客户端内存淘汰 (R-28 client eviction) 不感知 tracking 开销 (大表由 1M 限额兜底) | 大纲节 5 补注 |
| 4 | **表述精确化** | sendTrackingMessage 的 old_flags 双重恢复 (tracking.c:272-279,298-310): redirection 切换后 `old_flags = c->flags` **重新赋值**再置 CLIENT_PUSHING — 目标客户端原有 pushing 状态被尊重; 防御性模式 (非共享状态残留) | 大纲节 6 注 |
| 5 | 锚点补充 | sendTrackingMessage 尾部 `updateClientMemUsageAndBucket(c)` (L309) — 推送计入客户端内存桶 (R-28 交叉) | 大纲节 6 注 |
| 6 | **横切验证 (主从复制面)** | **从库上的 tracking 客户端也会收到失效**: 主库写命令经复制流到从库 → processCommand 全链 (R-9) → setKey (db.c:329) → signalModifiedKey → trackingInvalidateKey — 失效在**从库本地**触发; tracking 消息本身**不传播** (不走 AOF/复制, 直发连接) | 大纲节 3 补注 |
| 7 | 行号验证 | 全函数 22 锚点 + 跨文件 12 处 grep 穷举全部命中 (tracking.c 46-648 / networking.c 196,1515-1516,2122-2123,2832-2834,3354-3575,1832-1837,3874-3876 / server.c 1500-1504,1719-1724,3710-3725,3796-3809,4039-4043,4062-4064,5648,5905-5907 / db.c 621-624,640,1799,2260,2434 / config.c 3225 / lazyfree.c 219-232 / server.h 361-370,1249-1250) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 记住/失效/冲刷三面逐行对照: getKeysFromCommand (db.c:2434) / RO 脚本豁免 (server.c:3713-3714) / 三重过滤 (L378-391) / NOLOOP 双实现 (发送时 vs 构建时) / FLUSH NULL (L456-484) — 全部一致
- 回归实证: #11715 (MULTI+限额驱逐崩溃, 测试 L746-782) / lazy 过期双失效竞争 (测试 L136-150) / BLMOVE 唤醒后失效 (L497-513)

### 维度2 性能
- 双层 rax 前缀压缩 + 二进制 key 免分配
- 失效即删表项 (懒重建)
- BCAST: 公共 proto 构建一次共享 (写放大 O(1) per 前缀)
- 代价标注: trackingRememberKeyToBroadcast **每次键修改 O(前缀总数) 全扫** (L319-335) — BCAST 客户端多/前缀多时修改路径放大 (无索引)

### 维度3 内存
- TrackingTableTotalItems 总量提示 (L25-28) + INFO 三统计
- 惰性 ID 清理 (断连残留, 发送时跳过) — 内存换延迟
- clientMemUsage 前缀低估记账 (#3 覆盖缺口)
- FLUSH ASYNC 走 bio (numnodes>64, lazyfree.c:219-232)

### 维度4 一致性
- 单线程顺序 + pending 队列 = 消息保序 (响应后)
- 驱逐伪失效与真实失效区分 (bcast 参数双语义, L346-352)
- 从库本地触发失效 (#6 横切验证)
- 表全局跨 DB (FLUSHDB 单库也全量 NULL — 注释 L440-442 明确)

### 维度5 负面空间 (已写入大纲 7 条)
- 不做值缓存/消息持久化/跨节点失效/多 DB 区分/按键驱逐/ACL 过滤/握手协商

## 结论
R-17 全部锚点 ~40 处验证, 6 闭环完成, **机制洞察 2 + 覆盖缺口 1 + 横切验证 1 + 表述精确化 1**。怀疑审计 4 处修正已入 pass1-notes。推断 4 处显式标注 (temporal-trace.md)。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 前置 R-10a/R-21/R-20/R-28/R-16 (序号均 < 29) 已讲 ✅; 引出 R-18/RD-1 (未来 OK); 对照 R-29/R-22 ✅; 读者处境场景化 (值缓存到客户端内存/服务器怎么知道/键改了怎么通知/BCAST 零内存) ✅; 六结构元素齐备 ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 大纲 ~40 锚点 (file:line) — 🟡B 标准 ≥4 ⏫; 裸行号 0 处 ✅ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 前置声明无未来域 (R-18/RD-1 仅引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 复制 (从库本地失效, #6) / 过期 (惰性+主动都走 signalModifiedKey) / 淘汰 (performEvictions 强制冲刷+真实键通知) / 事务 (execution_nesting) / 协议 (RESP2/3 三分派) / 脚本 (RO 豁免+内层打点) / 客户端生命周期 (freeClient/RESET→disableTracking) — 七横切全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | 通过项 | 负面空间 7 条 ✅ (含怀疑审计修正: 无失效重放); 开篇场景化 ✅ (GET 跨网络贵/BCAST 零内存悬念) | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **推断标注** | 大纲节 1 "与 clients_index 的 htonu64 **刻意**不同" — "刻意"是推断 (原生序全表自洽可实证, 但设计动机无注释证据) | 大纲节 1 改 "内部自洽 (同一文件读写同序)" + 推断移入 temporal-trace |
| 14 | 通过项 | 其余 ~30 句机制描述逐句对源码一致 ✅ (惰性创建/四层门控/CACHING 一次性/三重过滤/pending 门控/失效即删/前缀冲突/空前缀/公共 proto 共享/effort 幂等递增/NULL 全失效/CLIENT_PUSHING 穿透/三分派/broken 通知/t/R/B 标志/惰性清理) | 记录 |
| 15 | 通过项 | 数字穷举复核: 7 标志位 (server.h:361-370) / 4 子命令版本 (6.0.0×3 + 6.2.0) / 默认 1M (config.c:3225) / effort 100 基数 / 频道名 20 字符 (L177) / "invalidate" 10B (L288) / "tracking-redir-broken" 21B (L269) / LAZYFREE_THRESHOLD=64 (lazyfree.c:181) ✅ | 记录 |

## 二次 REVIEW 汇总
07 五维度轮换 + 内容深度共 **1 处修复** (推断标注 #13), 反写测试结论: 大纲机制面完整可支撑写作。数字穷举 8 项全过。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

> 动机: 大纲每个锚点重新 grep, 对全部推断/计数反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | effort 幂等性 | 超限 → effort=100×(counter+1) 次随机删; 删后复检 `raxSize<=max` → counter=0; 删不完 → counter++ — **每次调用预算递增, 成功清零, 幂等收敛** ✅ | 通过 |
| V2 | BCAST 空前缀匹配 | trackingRememberKeyToBroadcast: `ri.key_len > keylen` 跳过 (0 恒过) + `ri.key_len != 0 && memcmp` 跳过 (0 恒过) → 空串匹配全部键 ✅ | 通过 |
| V3 | 前缀冲突对称性 | stringCheckPrefix(a,b) = memcmp(min_len) — "互为前缀"判定对称; 检查面: 新输入两两 + 新输入×既有客户端前缀; **不查全局 PrefixTable** (跨客户端重叠允许 — 各客户端独立订阅) ✅ | 通过 |
| V4 | 字节序双路径 | tracking 表: 写 `&tracking->id` 原生序 (L237) ↔ 读 memcpy 原生序 (L370-371) 自洽; clients_index: 写 htonu64 (L90) ↔ 读 htonu64 (L1833) 自洽; **两表互不混用** ✅ | 通过 |
| V5 | 驱逐与 maxmemory 独立性 | evict.c 零 tracking 引用 (grep 实证); trackingLimitUsedSlots 只查 raxSize 与 tracking_table_max_keys; 真实淘汰键走 signalModifiedKey (bcast=1) vs 表驱逐伪失效 (bcast=0) — 两条独立链 ✅ | 通过 |
| V6 | RO 脚本豁免必要性 | EVAL_RO 标记 CMD_READONLY 但 proc==evalRoCommand → 外层跳过; 内层 redis.call 命令各自 call() 打点 — 否则 RO 脚本整体记键会**多记未读键** (脚本参数键 vs 实际读键); 测试 L225-253 逐场景实证 ✅ | 通过 |
| V7 | 从库失效链 | 复制流命令 (CLIENT_MASTER 客户端) → processCommand → setKey (db.c:329) → signalModifiedKey (c=主库连接) → trackingInvalidateKey — 从库本地 tracking 客户端收到失效; 主库的 tracking 表不复制 ✅ | 通过 |
| V8 | pending NULL 项 | trackingInvalidateKeysOnFlush 对 current_client 入 NULL 项 (L464-466); 冲刷时 key==NULL → 发 shared.null RESP 编码 (L430-433, proto=1) — 与逐键失效同一通道, FLUSHDB+EXEC 测试 L568-580 实证 ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 16 | **边界实证** | trackingInvalidateKeysOnFlush 发送面遍历 `server.clients` 只给 CLIENT_TRACKING 客户端发 NULL (L463); **但 BCAST 客户端同样收 NULL** (有 BCAST 标志仍 CLIENT_TRACKING) — 与"BCAST 走前缀表"路径并存: FLUSH 时 BCAST 也收 NULL 全失效 (测试 L557-565 ASYNC flushall 中 BCAST+普通混合场景) — 语义: NULL 是全量失效的**保底通道**, 且 FLUSH 后表重建 → BCAST 的 keys 聚合不受影响 (前缀表独立保留) | 大纲节 5 补注 |
| 17 | **行号偏移检查** | beforeSleep 断言 (server.c:1719-1720) 含 tracking_pending_keys 与 pending_push_messages 双断言 — 大纲节 3 只引 L1719 缺 1720 (补) | 大纲节 3 修正 |

## 三次 REVIEW 汇总
推理验证 8 项全通过 (V1-V8); 新发现 **2 处** (边界实证 1: BCAST 也收 NULL 保底通道 / 行号补 1), 全部修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查三个存疑点 (HELLO 与 tracking 状态 / processCommand 驱逐与 MULTI 入队顺序 / FLUSH 消费端归因), 并做反写测试。

## 追查过程 (三个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | HELLO 切换协议是否重置 tracking? | helloCommand (networking.c:3593+) 只改 resp, **零 tracking 调用** (grep 实证); 只有 RESET/断连走 clearClientConnectionState (L1536) 才 disableTracking — 测试 L193-210 "Invalidations of previous keys can be redirected after switching to RESP3" 依赖此保留语义 | 通过 |
| T2 | processCommand 驱逐 (L4064) 与 MULTI 入队 (L4193) 相对顺序? | 4064 < 4193 — **驱逐在入队之前**; 入队分支不走 call() → CLIENT_EXECUTING_COMMAND 未设 (仅 call() 内 L3564 设置/L3584 清除) → 驱逐失效**直发** → 穿插在 QUEUED 回复前 — 正解释 #11715 测试断言顺序 (L777-779: invalidate → QUEUED → PONG) | 通过 (驱动发现 3) |
| T3 | FLUSH 消费端归因: db.c:1799 到底是谁? | L1799 的 trackingInvalidateKeysOnFlush(1) 在 **swapMainDbWithTempDb** (L1760, DEBUG RELOAD/主库替换) 内; flushdbCommand (L791) → flushCommandCommon → signalFlushedDb (L537, async 透传) → L640; **swapdbCommand (L1804) 只调 dbSwapDatabases (L1712-1757), 后者零 tracking 调用** | 发现 1+2 (大纲行号归因错误 + SWAPDB 语义缺口) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | pending 判定时机 | CLIENT_EXECUTING_COMMAND 仅在 call() 内设置 (server.c:3564) — 即命令 proc 正在写响应期间; 入队 (L4193)/驱逐 (L4064) 均在 call() 外 → 无标志 → 直发 | 通过 |
| V2 | NOLOOP 过滤与 pending 的顺序 | trackingInvalidateKey 内 NOLOOP 检查 (L387-391) 在 pending 判断 (L396) **之前** — NOLOOP 客户端的自改键既不直发也不入 pending | 通过 |
| V3 | pending 唯一 NULL 来源 | trackingInvalidateKeysOnFlush 对 current_client 入 NULL 项 (L466) — pending 列表除 NULL 外全是 keyobj; 冲刷按 key==NULL 分派 (L428-433) | 通过 |
| V4 | BCAST 聚合不受 FLUSH 影响 | FLUSH 只重建 TrackingTable (L475-483), **不碰 PrefixTable/bs->keys** (L440-484 全程无引用) — 残留聚合键下周期仍发, 对已收 NULL 的客户端无害 (幂等冗余) | 通过 |
| V5 | count\*15 预分配语义 | trackingBuildBroadcastReply L564: sdsMakeRoomFor(count\*15) — **初始容量提示非上界** (sds 自动扩容), 键名超长不截断 | 通过 |
| V6 | FLUSHDB 单库也全量 | signalFlushedDb (L626-640) 对单库也调 trackingInvalidateKeysOnFlush — 表全局不分 DB, 注释 L440-442 "Caching keys are not specific for each DB" | 通过 |
| V7 | 表重建后重跟踪语义 | FLUSH 后 TrackingTable=raxNew() (L481) — 客户端需重新 GET 才被跟踪 (全失效 + 懒重建闭环) | 通过 |
| V8 | SWAPDB 无失效的间接证据 | tracking.tcl (902 行) 无任何 swapdb 场景; dbSwapDatabases 仅 touchAllWatchedKeysInDb/scanDatabaseFor* (L1723-1757) | 通过 (驱动发现 2) |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 18 | **行号归因错误** | 大纲 §5 "消费端: signalFlushedDb (L640) / FLUSHDB ASYNC (**db.c:1799**) / SWAPDB (**db.c:1799**)" — 1799 实为 swapMainDbWithTempDb (DEBUG RELOAD 主库替换); FLUSHDB ASYNC 走 flushCommandCommon→signalFlushedDb (L537, async 透传)→L640 | 大纲 §5 消费端重写 (含 async 透传链) |
| 19 | **覆盖缺口/机制缺口** | **SWAPDB 命令不触发 tracking 失效** — dbSwapDatabases (L1712-1757) 对 WATCH 有 touchAllWatchedKeysInDb (L1723-1724), 对 tracking 零调用; 全局表不分 DB, SWAPDB 交换后客户端缓存的键值已变但收不到通知 → 陈旧缓存; tracking.tcl 零覆盖 (V8) — 已知限制/存疑标注 | 大纲 §5 标注 + 负面空间新增 "不做 SWAPDB 失效" |
| 20 | **表述精确化** | 大纲 §3 关键设计 "消息必须严格在命令响应之后 (穿插会破坏客户端解析)" — **过度概括**: RESP3 push 允许穿插在命令之间 (#11715 实证: 驱逐消息先于 QUEUED), 禁止的是打断**单条命令响应内部**; pending 判定 = CLIENT_EXECUTING_COMMAND (仅 call 内) | 大纲 §3 关键设计重写 (含 MULTI 入队无标志→直发的语义解释) |
| 21 | **表述精确化** | 大纲 §2 "四层门控 (tracking.c:204-220)" — 行号只指 tracking.c, 但 READONLY/非RO脚本/非BCAST 三重在 server.c:3713-3721; 跨文件混合引用行号误导 | 大纲 §2 改为 "门控分层 (两层, 行号分属两文件)" |

## 反写测试 (只读大纲能否写文章)

- §1 双层表: 结构/惰性创建/二进制 key/标志位/计数/惰性清理 — 可写 ✅
- §2 记住面: 调用点/身份分离/**门控分层(修复后)**/键提取/CACHING 一次性 — 可写 ✅
- §3 失效面: 入口/过滤/NOLOOP/pending/冲刷/performEvictions/过期 + **响应内不穿插精确语义(修复后)** — 可写 ✅
- §4 BCAST: 结构/冲突/聚合/周期发送/每前缀一条/NOLOOP 双路径 — 可写 ✅ (V5 细节不影响叙事)
- §5 限额+FLUSH: 配置/驱逐/双调用点/FLUSH/**消费端修正(修复后)** — 可写 ✅
- §6 命令面: 版本/互斥矩阵/REDIRECT/CACHING/TRACKINGINFO/三分派/broken/生命周期 — 可写 ✅
- 负面空间 8 条 (修复后含 SWAPDB) — 完整 ✅

## 四次 REVIEW 汇总

三存疑点全实证 (T1-T3); 推理验证 8 项全过 (V1-V8); **新发现 4 处全部修复** (行号归因错误 1 / 机制缺口 1 / 表述精确化 2) — 其中 SWAPDB 缺失效是最有价值发现 (测试零覆盖的真实语义缺口, 已标注存疑)。大纲经修复后反写测试全过。
