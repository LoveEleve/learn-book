# 闭环笔记 q5: 八策略矩阵 — 标志位分解

## 假设
8 策略 = 3 标志位 (LRU/LFU/ALLKEYS) × 来源 (volatile/allkeys) + TTL/RANDOM/NOEVICTION 特例。

## 验证过程
- 枚举 (server.h:556-569):
  - 标志: MAXMEMORY_FLAG_LRU (1<<0) / LFU (1<<1) / ALLKEYS (1<<2)
  - volatile-lru (0<<8)|LRU / volatile-lfu (1<<8)|LFU / **volatile-ttl (2<<8) 无标志** / volatile-random (3<<8) 无标志
  - allkeys-lru (4<<8)|LRU|ALLKEYS / allkeys-lfu (5<<8)|LFU|ALLKEYS / allkeys-random (6<<8)|ALLKEYS
  - noeviction (7<<8)
  - 即: 高 8 位 = 策略 id, 低 3 位 = 行为标志
- 解析 (config.c:3133): maxmemory_policy_enum → server.maxmemory_policy; 默认 NO_EVICTION
- 分派 (evict.c):
  - 池路径 (L564-566): `policy & (LRU|LFU) || policy == VOLATILE_TTL` — 采样排序淘汰
  - random 路径 (L635-637): ALLKEYS_RANDOM / VOLATILE_RANDOM — 直接随机
  - 拒绝路径 (L538): NO_EVICTION — 只拒绝不删
- 来源选择 (L577-581, 609-613): `policy & ALLKEYS → db->keys; 否则 db->expires` — volatile 系列从 expires 表采样 (天然只碰有 TTL 的键)
- 共享整数禁用 (server.h:559-560): MAXMEMORY_FLAG_NO_SHARED_INTEGERS = LRU|LFU — 有淘汰时 maxmemory 禁共享整数 (LRU 私有, R-1 已交付 L627-635)
- TTL idle 特例 (L163-165): `ULLONG_MAX - TTL` — 越早到期越优先 (不需要值对象, L143)

## 代码类型
Mechanism (策略分派)

## 跨域关联
- R-1 (NO_SHARED_INTEGERS) / R-22 (volatile 从 expires 采样) / R-21 (db->keys/expires)

## 结论
策略 = 8 位 id + 3 位标志的位域: LRU/LFU 决定打分, ALLKEYS 决定采样来源 (keys vs expires), TTL/RANDOM 走特例路径, NOEVICTION 拒绝。volatile 族天然兼容"过期优先回收"。
源码位置: server.h:556-569; evict.c:538,564-566,577-581,635-637
