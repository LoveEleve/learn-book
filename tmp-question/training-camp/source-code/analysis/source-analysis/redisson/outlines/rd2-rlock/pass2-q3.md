# 闭环笔记 q3: 批量续期协议 — 单 Lua 脚本多锁 + 失效摘除

## 假设
Watchdog 心跳不是每锁一个请求, 而是 chunkSize (默认 100) 把锁通过一个 Lua 脚本批量续期; ContainsDecoder 过滤"锁已消失"的槽位并自动摘除。

## 验证过程
- RenewalTask.execute (RenewalTask:78-88): 非 cluster → renew(name2entry 全部); **cluster → renewSlots 按 slot 分组**
- LockTask.renew (LockTask:37-43): `AsyncChunkProcessor.processAll(iter, chunkSize, this::buildChunk)` — 分片迭代器
- buildChunk (L50-83): 取 ≤chunkSize 有效锁 (跳过空 entry 无 threadId 无 lockName) → keys + args (lockName) + name2threadId
- **批量续期 Lua** (L84-96):
  ```
  for i = 1, #KEYS: hexists(KEYS[i], ARGV[i+1])? → pexpire(KEYS[i], ARGV[1]); result[i]=1
                      否则 result[i]=0
  return result
  ```
- ContainsDecoder 消费 result: **result[i]=0 (锁没了) → 从 keys 移除 → cancelExpirationRenewal** (L101-104) — 自动摘除已释放锁
- ARGV[1]=internalLockLeaseTime; ARGV[i+1]=对应锁的 holder 字段名
- chunkSize = lockWatchdogBatchSize (Config.json 默认 100)

## 代码类型
Algorithmic (批量协议) — 一次 RTT 续期 100 锁 + 失效检测合并

## 跨域关联
- Q4 (Watchdog 开关) → execute 每 10s 跑一轮
- Config (lockWatchdogBatchSize=100) → chunk 大小
- 性能: 100 锁续期 = 1 RTT vs 100 RTT (无批量前)

## 结论
批量续期 = AsyncChunkProcessor 分片 (chunk=100) + 单 Lua 多键续期 (hexists→pexpire) + ContainsDecoder 过滤失效锁 → 自动 cancelExpirationRenewal。设计: 一次脚本做"续期 + 检测 + 摘除"三合一, 心跳成本 = 每锁 1/100 RTT。
源码位置: LockTask.java:37-104, RenewalTask.java:78-88