# 闭环笔记 q4: RMapCache 五结构 — TTL/idle/LRU 协同

## 假设
RMapCache 不只主 hash: 每条目 TTL 和 idle 分别存 zset 分数, 读时惰性检查 + 更新 idle/LRU。五个辅助结构: 主 hash + TTL zset + idle zset + LRU zset + options hash。

## 验证过程
- `getWithLease` Lua (RedissonMapCache.java:129-176):
  - L129 主 hash hget; L141 `struct.unpack('dLc0', value)` 解 TTL 头
  - L142-146 **TTL 检查**: `zscore(KEYS[2] TTL zset)` → expireDate
  - L147-155 **idle 检查**: `zscore(KEYS[3] idle zset)` → expireIdle; **t>0 (有 idle) → `zadd(KEYS[3], t+now)` 刷新 idle 窗口** (L151)
  - L153 `expireDate = min(expireDate, expireIdle)` — TTL 与 idle 取更早
  - L156 **过期判定**: `expireDate <= now` → 返回不存在 (惰性)
  - L167-175 **LRU/LFU**: maxSize 配置 → `zadd(KEYS[4], now)` (LRU 访问时间) / `zincrby(KEYS[4], 1)` (LFU 频率)
- 五结构 (L177-178): name (主 hash) + timeoutSetName (TTL zset) + idleSetName (idle zset) + lastAccessTimeSetName (LRU zset) + optionsName (maxSize/mode hash)
- 惰性语义: 读时检查+清理已过期条目 (返回不存在); 后台 EvictionTask 再物理删 (Q5)

## 代码类型
Algorithmic (五结构索引) — TTL/idle/LRU 的 zset 协同

## 跨域关联
- Q5 (EvictionTask) → 后台物理清理
- Redis r22 (过期) → 服务端 TTL 惰性 vs 客户端缓存 zset
- RD-6 (本地缓存) → maxSize eviction 策略

## 结论
RMapCache = 主 hash + 4 辅助结构 (TTL/idle/LRU zset + options hash)。读一次: TTL 检查 + idle 刷新 + LRU 更新 + 过期判定。idle 窗口动态刷新 (zadd t+now), TTL/idle 取更早。这是"缓存语义"的 Redis 实现。
源码位置: RedissonMapCache.java:129-179