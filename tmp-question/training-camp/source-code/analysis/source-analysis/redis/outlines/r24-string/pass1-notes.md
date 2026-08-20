# R-24 t_string — Pass 1 探索笔记

> 域: R-24 t_string (t_string.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/t_string.c (930) | Redis 7.4.2

## 调用图

```
SET 族:
setCommand (L274) → tryObjectEncoding (L283, R-1) → parseExtendedStringArgumentsOrReply (L188, 8 标志)
  → setGenericCommand (L63): NX/XX/GET 检查 (L78-85) → setKey (L91, SETKEY_KEEPTTL/ALREADY/DOESNT)
    → expire → setExpire + 重写 PXAT (L95-105) → GET 时剥 GET 重写 (L112-128)
  getExpireMillisecondsOrReply (L143): 单位换算/溢出/≤0/相对+now

GET 族:
getGenericCommand (L302): lookupKeyReadOrReply → checkType → addReplyBulk
getexCommand (L340): 读 + TTL 三路径 (已过期删 L369-378 / setExpire+重写 PEXPIREAT L379-388 / PERSIST L389-396)
getdelCommand (L399): 读 + dbSyncDelete + 重写 DEL
getsetCommand (L410): 读 + setKey + 重写 SET

范围/位:
setrangeCommand (L421): offset<0 拒 → 不存在 sdsnewlen 预填零 (L446) / 存在 dbUnshareStringValue (L467)
  → sdsgrowzero + memcpy (L471-472)
getrangeCommand (L481): INT 编码解码 (L494-496) → 负索引转换 (L503-511) → 边界 (L515-519)

批量:
mgetCommand (L522): 逐键 lookup + 非字符串 null
msetGenericCommand (L540): NX 预检 (L551-556) → SETKEY_ADD_OR_UPDATE 变化 (L566)

自增:
incrDecrCommand (L580): 溢出检查 (L589-593) → **原地 INT 更新** (L596-601: refcount==1 && INT && 非共享 && LONG 范围)
  → 否则 createStringObjectFromLongLongForValue (L603)
incrbyfloatCommand (L643): long double + NaN/Inf 拒 (L654) → **重写 SET KEEPTTL** (L671-673, 浮点精度传播一致)

拼接:
appendCommand (L676): 不存在 dbAdd (L684) / 存在 dbUnshare + sdscatlen (L698-699)
strlenCommand (L708): stringObjectLen

LCS:
lcsCommand (L716): DP 表 (alen+1)×(blen+1) uint32 (L786-787) → 内存上限 proto_max_bulk_len (L790) + ztrymalloc (L794)
```

## 基本元素分解

1. **SET 标志矩阵**: 9 标志位 (NX/XX/EX/PX/KEEPTTL/GET/EXAT/PXAT/PERSIST) + 互斥校验
2. **setGenericCommand**: 条件设置 + KEEPTTL + 传播重写 (PXAT/DEL)
3. **编码链**: tryObjectEncoding (SET) / getDecodedObject (LCS/GETRANGE)
4. **INCR 原地优化**: INT 编码 + 引用计数 + 非共享范围 → 指针直改
5. **浮点传播**: INCRBYFLOAT → SET KEEPTTL 重写 (精度一致性)
6. **破坏性修改**: dbUnshareStringValue (SETRANGE/APPEND 共享/编码对象)
7. **范围命令**: GETRANGE 负索引 / SETRANGE 零填充
8. **LCS**: DP 表 + 内存防护 (proto_max_bulk_len + ztrymalloc)

## 标记问题 (8 个)

1. SET 的 9 标志互斥矩阵怎么校验?
2. SET 带 GET 的传播重写逻辑 (剥 GET)?
3. INCR 原地更新的四个条件?
4. INCRBYFLOAT 为什么重写 SET KEEPTTL?
5. SETRANGE 的零填充 (sdsgrowzero)?
6. GETRANGE 负索引语义?
7. GETEX 的 TTL 三路径?
8. LCS 内存防护 (UINT32_MAX/512MB/ztrymalloc)?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): t_string.c 初版 — get/set/incr/append 家族 (版权 2009-Present)
- 2.6: SETNX/SETEX/GETSET; SETRANGE/GETRANGE 引入
- 4.0: SET NX/XX/EX/PX 选项 (setGenericCommand 重构); INCRBYFLOAT
- 6.2: GETEX/GETDEL; SET GET/KEEPTTL (标志扩展)
- 7.x: 8 标志互斥校验完善 (parseExtendedStringArgumentsOrReply); LCS (6.0+ 持续完善)

## 大域拆分判断

930 行单文件 — **不拆** (🟡 B, 6 闭环足够)。
