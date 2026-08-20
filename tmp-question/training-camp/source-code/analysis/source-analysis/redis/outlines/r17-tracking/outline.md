# R-17 客户端缓存 — 服务端协助的客户端缓存 (双层失效表 + 广播聚合)

> 前置: [[R-10a-rax]] (双层 rax) + [[R-21-db]] (signalModifiedKey 键修改打点) + [[R-20-server]] (call 命令链/CMD_READONLY) + [[R-28-networking]] (RESP3 push/客户端生命周期) + [[R-16-multi]] (execution_nesting) | 引出: [[R-18-defrag]] (内存面) + [[RD-1-redisson-cache]] (客户端缓存集成) | 对照: [[R-29-pubsub]] (推送通道) + [[R-22-expire]] (过期触发失效)
> 🟡 B | 6 KP | [模式: 双层 rax 索引 + 读打点 + 写失效 + 事件循环聚合 + 限额驱逐]
> Pass 2 闭环: q1(表结构) q2(记住面) q3(失效面) q4(BCAST) q5(限额+FLUSH) q6(命令面+重定向)

**读者处境**: GET 很便宜, 但每次跨网络取还是贵 — 能不能把值缓存到客户端内存? 服务端怎么知道客户端缓存了哪些键? 键改了怎么通知? BCAST 模式为什么"零内存"? 这篇拆客户端缓存: 双层 radix 表、读命令打点、失效传播与延迟发送、广播聚合、独立限额。

### 1. 双层 radix 表 — key → 客户端 ID 集合

场景: 服务器怎么记住"谁缓存了什么键"?
源码路径:
- **双层 rax** (tracking.c:12-23): `TrackingTable` = rax[key → rax[clientID]] — 顶层按键名, 内层按客户端 ID; **惰性创建** (L174-178, 首个客户端启用时)
- **ID 作二进制 key** (L237): `raxTryInsert(ids,&tracking->id,8,...)` — uint64 原生字节序 8B; 与 clients_index 的 `htonu64` (networking.c:85-90) 字节序不同, 但两表各自读写同序、互不混用 (q1 实证; 设计动机无注释, 标注推断)
- **7 标志位** (server.h:361-370): TRACKING(31)/BROKEN_REDIR(32)/BCAST(33)/OPTIN(34)/OPTOUT(35)/CACHING(36)/NOLOOP(37) + CLIENT_PUSHING(46)
- **计数**: TrackingTableTotalItems 全表 ID 总数 (L25-28) — INFO tracking_total_items 数据源
- **断连惰性清理** (L42-45): disableTracking 只减 tracking_clients, **表内 ID 残留** — 发送时 target==NULL 跳过 (省 O(表项) 清理成本)
关键设计 (q1): **双层 rax 选型**: key 共享前缀天然压缩 (rax 压缩节点, R-10a); 二进制 key 免字符串分配; 失效即删表项 (L405-409) 下次重建。[模式: 双层索引]

### 2. 记住面 — 只读命令后打点

场景: 客户端 GET 之后, 服务器怎么知道要跟踪这个键?
源码路径:
- **调用点** (server.c:3710-3725, call() 尾): 条件 = `CMD_READONLY` 且非 evalRo/evalShaRo/fcallro — **RO 脚本外层豁免, 内层命令各自打点** (测试 L225-253)
- **身份分离** (L3716-3723): 跟踪标志取 `server.current_client` (外部客户端), 键取 `c` (实际执行者 — 脚本/EXEC 内部客户端) — "original external client that triggered the command"
- **门控分层** (两层, 行号分属两文件): call() 层 (server.c:3713-3721): CMD_READONLY + 非 RO 脚本 + 非 BCAST; tracking.c 层 (L204-220): OPTIN 无 CACHING / OPTOUT 有 CACHING → 跳过; `CMD_PUBSUB` 跳过 (分片频道不跟踪); 零键跳过
- **键提取** (L209-214): getKeysFromCommand (db.c:2434) — R-21 key specs 复用
- **CACHING 一次性** (networking.c:2119-2123): 每条命令后清标志 (对照 ASKING L2116-2117); MULTI 内保活到 EXEC — `!(CLIENT_MULTI) && prevcmd != clientCommand`
关键设计 (q2): **读打点 + 一次性覆盖标志** — OPTIN/OPTOUT 是"白/黑名单默认 + 单命令翻转"两种粒度; BCAST 客户端不记主表 (redis.conf:864 零内存承诺)。[模式: 读打点]

### 3. 失效面 — 键修改即通知, 但不穿插响应

场景: 键被 SET 了, 缓存了它的客户端怎么收到消息?
源码路径:
- **入口** (db.c:621-624): signalModifiedKey = touchWatchedKey + trackingInvalidateKey(c,key,1) — 所有键修改统一打点 (R-16/R-21); c 可 NULL (过期删除上下文)
- **三重过滤** (tracking.c:366-391): target 已断连 / 已关跟踪 / 已切 BCAST (旧表项不发, L373-383); **NOLOOP** (L385-391): target == server.current_client → 跳过
- **pending 延迟** (L393-401): 目标在**执行命令中** → incrRefCount 入 tracking_pending_keys — 注释 "should after command response"
- **冲刷** (L412-438): afterCommand (server.c:3803) — 传播收尾先行, 响应字节已在输出缓冲, 推送追加其后 (L3798-3799 注释 "reply to client before invalidating cache"); **execution_nesting 门控** (L417): EXEC/脚本内不发 → 不穿插事务响应 (测试 L449-464); beforeSleep 断言 pending 空 (server.c:1719-1720 双断言)
- **performEvictions 后强制冲刷** (server.c:4039-4043): 淘汰先于响应 (测试 L466-495)
- **过期路径**: 惰性/主动过期删除都走 signalModifiedKey → 客户端收到过期通知 (测试 L126-150)
关键设计 (q3): **失效即删表项 + 响应内不穿插** — pending 判定 = `CLIENT_EXECUTING_COMMAND` (仅 call() 内设置, server.c:3564/3584): 命令 proc 正在写响应 → 延迟; 精确语义: RESP3 push **允许穿插在命令之间** (协议合法, #11715 测试 L777-779 实证驱逐消息先于 QUEUED — MULTI 入队分支 L4193 不走 call(), 无标志 → 直发), 但**不得打断单条命令响应内部** (MGET 多键响应中途的惰性过期失效必须 pending — 测试 L428-447); 单线程内 pending 队列天然保序。[模式: 失效传播]

### 4. BCAST 广播面 — 前缀聚合, 每周期一批

场景: 想缓存一个键空间 (前缀), 不想逐个 GET 打点?
源码路径:
- **bcastState** (tracking.c:31-38): keys (本周期修改) + clients (订阅者) 双 rax; PrefixTable[prefix → bcastState]; **空前缀 "" = 全量** (L183)
- **冲突检查** (L83-132): checkPrefixCollisionsOrReply — 任一前缀不互为前缀 (memcmp min_len); 输入内部 + 与既有双检查 (测试 L398-414)
- **修改聚合** (L319-335): 全量扫 PrefixTable — key 以某前缀开头 → bs->keys 插入 (**value = 修改者客户端指针**, NOLOOP 用); O(前缀数)
- **周期发送** (L586-632): beforeSleep (server.c:1724) 每事件循环周期: 每前缀 keys 非空 → 构建公共 proto (*N\r\n 预序列化, L541-581) → 逐客户端: NOLOOP 个性化 (排除自己改的) / 共享 proto → sendTrackingMessage(proto=1) → 清空 keys
- **每前缀一条消息** (测试 L71-88 断言: 两前缀两条消息)
- **NOLOOP 双路径**: 非 BCAST 发送时逐键跳过 (L385-391) vs BCAST 构建时排除 (L610-616) — 因 BCAST 是整批客户端共享消息
关键设计 (q4): **广播聚合** — 一个事件循环周期内的多次修改合并为每前缀一条消息; 公共协议构建一次多客户端共享 (写放大 O(1)); BCAST 主表零内存的代价 = 客户端会收到未缓存键的失效。[模式: 批量广播]

### 5. 限额驱逐与 FLUSH — 独立的键数上限 + NULL 全失效

场景: 读多写少工作负载下失效表无限膨胀怎么办? FLUSHALL 后怎么通知?
源码路径:
- **配置** (config.c:3225): tracking-table-max-keys 默认 **1,000,000**; 0 = 无限
- **驱逐** (tracking.c:496-533): `raxSize(TrackingTable) > max_keys` → **effort = 100×(timeout_counter+1)** 幂等递增 (测试 L587-606); raxRandomWalk 随机选键 → **伪造失效** (trackingInvalidateKey(NULL,key,0) — bcast=0 键没真变, L346-352 注释); 每删一键复检上限
- **双调用点**: processCommand 尾 (server.c:4064) + serverCron (server.c:1500-1504) — CONFIG SET 后 idle 也生效
- **FLUSH** (tracking.c:440-484): trackingInvalidateKeysOnFlush — 所有 tracking 客户端 (含 BCAST, **NULL 是全量失效的保底通道**, L463 仅判 CLIENT_TRACKING) 收 **RESP NULL** (全失效语义, L442-445 注释 "avoid flooding"); 表整体重建 (async → freeTrackingRadixTreeAsync, lazyfree.c:219-232: numnodes>64 → bio); 前缀表独立保留 (BCAST 聚合不受影响)
- **消费端**: signalFlushedDb (db.c:640, FLUSHDB/FLUSHALL — async 由 flushCommandCommon L537 透传) / swapMainDbWithTempDb (db.c:1799, DEBUG RELOAD 等主库替换) / **SWAPDB 缺口: dbSwapDatabases (L1712-1757) 不触发 tracking 失效** (WATCH 有 touchAllWatchedKeysInDb, tracking 无 — 客户端缓存可能陈旧; 测试无覆盖, 存疑标注)
- **与 maxmemory 独立**: 驱逐真实键走 signalModifiedKey 通知 (淘汰先于响应, 测试 L466-495); 限额驱逐是表自身的随机淘汰
关键设计 (q5): **独立限额 + 伪失效驱逐** — 表上限是"服务器内存 vs 客户端缓存命中率"的旋钮; 驱逐=假装键被改 (客户端收到失效自然重取); effort 递增保证幂等清超。[模式: 限额驱逐]

### 6. 命令面与重定向 — 4 子命令 + 三分派协议

场景: CLIENT TRACKING 怎么开? 失效消息走什么协议?
源码路径:
- **版本** (commands.def): TRACKING/CACHING/GETREDIR **6.0.0**; TRACKINGINFO **6.2.0**
- **选项互斥矩阵** (networking.c:3410-3466): PREFIX 需 BCAST / **BCAST 模式不可原地切换** (L3420-3431) / BCAST×OPTIN|OPTOUT 拒 / OPTIN×OPTOUT 拒 / OPTIN↔OPTOUT 不可原地切换 / BCAST → 前缀冲突检查
- **REDIRECT** (L3354-3389): 只许一个目标; **目标必须当前存在** (L3384 sanity check); enableTracking 覆盖 redirection (tracking.c:170)
- **CLIENT CACHING** (L3478-3507): 无 TRACKING 拒; YES 仅 OPTIN / NO 仅 OPTOUT
- **TRACKINGINFO** (L3515-3575): flags 数组 (on/off+bcast+optin[+caching-yes]+optout[+caching-no]+noloop+broken_redirect) + redirect + prefixes
- **发送三分派** (tracking.c:255-311): CLIENT_PUSHING 穿透 REPLY OFF/SKIP (networking.c:286-289 + addReplyPushLen 断言 L1001); ① RESP3 → push [2,"invalidate",keys] (L286-288) ② RESP2+重定向+目标 PUBSUB → `__redis__:invalidate` 频道 (L289-292, 20 字符 L177) ③ RESP2 无重定向 → **静默丢弃** (L293-299)
- **broken 通知** (L260-280): 重定向目标消失 → BROKEN_REDIR 标志 + push ["tracking-redir-broken", id] (测试 L255-277)
- **生命周期**: freeClient (L1515-1516) / RESET (L1536) → disableTracking; CLIENT LIST 标志 t/R/B (L2832-2834)
关键设计 (q6): **重定向 = 失效消息与业务连接解耦** (专用 pubsub 客户端收失效, RESP2 也可用); 三分派按协议能力降级。[模式: 命令家族 + 重定向]

### 负面空间 — 客户端缓存刻意不做的事

- **不做服务器端值缓存**: 只跟踪键名, 值仍在客户端 (服务器不存副本)
- **不做消息持久化/积压**: 断连期间失效消息丢失, 客户端重连需自清缓存 (无失效重放, q1 审计修正)
- **不做 SWAPDB 失效**: dbSwapDatabases 只失效 WATCH, tracking 表不发通知 (全局表不分 DB + 交换后缓存陈旧 — 已知限制)
- **不做跨节点失效**: cluster 模式每节点独立表; MOVED 不触发失效 (客户端职责)
- **不做多 DB 区分**: 失效表全局共享, FLUSHDB 单库也全量 NULL
- **不做按键驱逐 (LRU)**: 限额驱逐是随机采样, 无热点保护 (对照 R-23)
- **不做 ACL/键模式过滤**: 能读不代表能跟踪判断 (命令面独立)
- **不做客户端握手协议**: 靠 CLIENT 命令显式开启, 无自动协商

→ 引出: 失效消息面怎么在 Redisson 落地? → [[RD-1-redisson-cache]]
