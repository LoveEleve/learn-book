# 闭环笔记 q6: MapCacheEvictionTask Lua — 批量过期清理

## 假设
MapCacheEvictionTask 用 Lua: zrangebyscore 从 TTL/idle zset 取已过期 key (score≤now) → hdel 从主 hash 批量删 (unpack ≤4999/次)。

## 验证过程
- MapCacheEvictionTask (eviction/MapCacheEvictionTask.java:35,77-111):
  - L77 `zrangebyscore(KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2])` — TTL zset 取 score∈[0,now], limit=ARGV[2] (批量上限)
  - L90-93 **五路清理**: `zrem(KEYS[5]/KEYS[3]/KEYS[2], unpack(expiredKeys1, i, min(i+4999, n)))` — **三个辅助 zset 各 zrem + 主 hash hdel**, 每批 ≤4999 (Redis 参数上限 10240 内)
  - L95-111 同样的 idle zset (KEYS[3]) 清理
  - **双 zset (TTL+idle) 各自取过期 → zrem 辅助结构 + hdel 主 hash** (五路删除)
- execute 返回清理数 (size) → EvictionTask 自适应调频 (Q5)
- KEYS[1]=主 hash; KEYS[2]=TTL zset; KEYS[3]=idle zset; KEYS[5]=另一辅助
- 与惰性检查 (Q4 读时) 互补: 惰性=访问路径即时过期; 此任务=后台批量物理清理

## 代码类型
Algorithmic (批量清理) — 过期索引→主存批量删

## 跨域关联
- Q4 (五结构) → 清理对象
- Q5 (自适应) → 清理量信号源
- Redis r22 (主动过期) → 服务端 activeExpireCycle 对照

## 结论
后台清理 = 过期索引驱动: TTL/idle 两个 zset 各 zrangebyscore(0,now) → 主 hash hdel (unpack 4999 批次)。双索引各自清, 返回量喂自适应调度。这是"缓存过期"在 Redis 里的索引化实现。
源码位置: MapCacheEvictionTask.java:77-111