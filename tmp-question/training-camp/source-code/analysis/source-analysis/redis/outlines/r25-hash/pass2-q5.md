# 闭环笔记 q5: HFE 惰性过期链 — GETF 三态

## 假设
字段级惰性过期 = hashTypeGetValue 内联判定: 过期 → 从库只报 / 主库删字段+传播+通知+空 hash 删键。

## 验证过程
- hashTypeGetValue (t_hash.c:711-779):
  - 查找: listpack 家族 (L716-722: lpFind + expiredAt 提取, EX 编码三元素组) / HT (L724-735: hfieldGetExpireTime)
  - **过期判定** (L737): `expiredAt >= commandTimeSnapshot()` → GETF_OK (时间冻结语义, R-22)
  - **从库语义** (L740-747): CLIENT_MASTER 视为有效 (L742-743); 用户客户端 → GETF_EXPIRED **不删** (L746)
  - **跳过删除** (L749-753): loading / lazy_expire_disabled / HFE_LAZY_AVOID_FIELD_DEL / PAUSE_ACTION_EXPIRE
  - **主库删除链** (L760-778): hashTypeDelete 字段 (L761) + **propagateHashFieldDeletion** (L762, 传播 HDEL) + stat_expired_subkeys (L763) + **notify "hexpired"** (L769) + **空 hash → dbDelete + GETF_EXPIRED_HASH** (L770-775) + signalModifiedKey (L776)
- hashTypeGetValueObject (L791-803): robj 封装 (vstr → createStringObject / vll → FromLongLong)
- hashTypeExists (L821-854): 布尔判定 + 惰性删
- GETF 枚举: OK / NOT_FOUND / EXPIRED / EXPIRED_HASH (L812-820 附近)

## 代码类型
Mechanism (惰性过期链)

## 跨域关联
- R-22 (命令时间冻结/PAUSE) / R-21 (dbDelete) / R-29 (notify hexpired)

## 结论
字段惰性过期 = 键级 expireIfNeeded (R-21) 的字段版: 三态 + 角色语义 (从库只报) + 传播 (HDEL) + 级联 (空 hash 删键)。GETF_EXPIRED_HASH 让调用方感知"读一个字段把整个 hash 读没了"。
源码位置: t_hash.c:711-854
