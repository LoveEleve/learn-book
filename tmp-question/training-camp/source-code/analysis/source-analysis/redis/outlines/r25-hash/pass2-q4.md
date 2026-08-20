# 闭环笔记 q4: 命令面 — HSET/HGETALL/HRANDFIELD/HSCAN

## 假设
命令面 = 查找 + 迭代器遍历 + 回复组装; HGETALL 有 skipExpiredFields 优化。

## 验证过程
- hsetCommand (L2158-2186): LookupWriteOrCreate → TryConversion → 循环 hashTypeSet → **HSET/HMSET 回复差异** (L2173-2178: cmdname[1]=='s' → 新字段数, 否则 OK)
- hincrbyCommand (L2187-2229): hashTypeGetValue (HFE_LAZY_EXPIRE) → GETF_EXPIRED 时值=0 (L2198-2201) → 溢出检查 (L2211-2214, 同 R-24) → hashTypeSet (TAKE_VALUE|KEEP_TTL)
- hgetallCommand (L2445-2486): **length 先算** (hashTypeLength subtractExpiredFields, L2460) → **skipExpiredFields 优化** (L2462-2464: 全局 HFE 无最小到期 → 迭代免逐字段查) → 迭代回复 (map/array 按 flags) + **断言 count==length** (L2483)
- hrandfieldWithCountCommand (L2547-2790): COUNT 正负语义 (正=可重复/负=不重复) + 小 hash listpack 直接随机 (L2520) / 大 hash 加权 (L2628+)
- hscanCommand (L2509): scanGenericCommand 委托 (R-21)
- hdelCommand (L2352-2390): 批量 hashTypeDelete + 空 hash 删键 (L2382-2386)
- hlen/hstrlen/hexists (L2392-2507): hashTypeLength/stringObjectLen 快查
- hmgetCommand (L2321-2351): 批量 GetValueObject + 过期字段 null

## 代码类型
Glue (命令面装配)

## 跨域关联
- R-21 (scanGenericCommand) / R-24 (HINCRBY 溢出同款) / R-28 (addReply 族)

## 结论
命令面 = 查找 → 迭代 → 回复 三段式; HGETALL 的 skipExpiredFields 是"全局无到期 → 免逐字段查"的批量优化; HSET/HMSET 回复差异是历史兼容 (HMSET 已弃用)。
源码位置: t_hash.c:2134-2510,2547-2790
