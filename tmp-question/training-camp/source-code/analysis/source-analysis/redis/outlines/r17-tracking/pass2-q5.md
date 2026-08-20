# 闭环笔记 q5: 限额驱逐与 FLUSH 全失效

## 假设
tracking-table-max-keys 独立限额 (默认 1M); 超限随机驱逐 + effort 递增; FLUSH 发 NULL 全失效 + 表重建。

## 验证过程
- **配置** (config.c:3225): `createSizeTConfig("tracking-table-max-keys", ..., 0, LONG_MAX, server.tracking_table_max_keys, 1000000, ...)` — 默认 1M; **0 = 无限** (tracking.c:499 "No limits set")
- **驱逐函数** (L496-533): trackingLimitUsedSlots —
  - 判定: `raxSize(TrackingTable) > max_keys` (超的是**键数**非 ID 数; raxSize = 顶层键条目)
  - **effort = 100 × (timeout_counter+1)** (L509) — 上一轮没清完 → 本轮翻倍级递增 (100→200→300..., 测试 L587-606 "evacuate enough keys")
  - 随机驱逐 (L512-527): raxRandomWalk(ri,0) 随机选键 → **伪造失效**: trackingInvalidateKey(NULL,keyobj,0) — **bcast=0** (键没真变, 不广播), 注释 L346-352 明确: "we also call the function in order to evict keys in the key table in case of memory pressure: ... the key didn't really change"
  - 提前返回: 每删一键查 `raxSize <= max_keys` → 清零 counter 返回 (L522-526)
  - 上限内 → counter 清零 (L501-503)
  - 到点没清完 → timeout_counter++ (L532) — 幂等持续 (cron 每 tick 重试)
- **双调用点**: processCommand 尾 (server.c:4064, "Make sure to use a reasonable amount of memory ... at every command execution") + serverCron (server.c:1500-1504, "if the last command executed changes the value via CONFIG SET ... even if completely idle") — 注释即设计理由
- **CLIENT TRACKING 触发坑**: CONFIG SET 生效在下次调用; 测试 #11715 (L746-782): MULTI 队列 + pause-cron → 驱逐消息穿插 QUEUED 回复的崩溃回归
- **FLUSH 全失效** (L440-484): trackingInvalidateKeysOnFlush(async) —
  - 发送: 遍历 server.clients, CLIENT_TRACKING → **RESP NULL** (shared.null[resp] 编码作 proto=1, "A RESP NULL is sent to indicate that all keys are invalid") — 不逐键发 (L442-445 注释 "avoid flooding clients")
  - 当前客户端 → pending 队列 NULL 项 (L464-466, 延迟到响应后; 测试 L568-580 flushdb + EXEC)
  - **表重建**: TrackingTable 整体释放 + raxNew() + TotalItems=0 (L474-483); async → freeTrackingRadixTreeAsync (lazyfree.c:219-232: numnodes > LAZYFREE_THRESHOLD(64) → bio 异步, 对照 R-21 lazyfree 面)
- **消费端**: signalFlushedDb (db.c:640, FLUSHDB/FLUSHALL) + flushdbCommand ASYNC (db.c:1799) + swapdbCommand (db.c:1799, 换库也全失效 — 键面全变)
- **驱逐与 maxmemory 无耦合**: 独立限额; 但 performEvictions 淘汰真实键会走 signalModifiedKey → trackingInvalidateKey (bcast=1) — 测试 L466-495 验证淘汰键的失效先于响应

## 代码类型
Algorithmic (随机采样驱逐 + 幂等 effort 递增)

## 跨域关联
- R-23 (evict): 对照 maxmemory 十策略 — tracking 是独立键数限额随机驱逐
- R-2 (events): serverCron 调度
- R-21 (db): lazyfree bio 异步回收
- R-22 (expire): 失效消息也可由过期删除触发

## 结论
限额 = tracking-table-max-keys (默认 1M, 0=无限): 超限随机驱逐伪失效, effort 幂等递增, 双调用点 (命令尾+cron); FLUSH = NULL 全失效 + 表整体重建 (async 走 bio)。与 maxmemory 完全独立。
源码位置: config.c:3225; tracking.c:440-533; server.c:1500-1504,4062-4064; lazyfree.c:219-232; db.c:640,1799
