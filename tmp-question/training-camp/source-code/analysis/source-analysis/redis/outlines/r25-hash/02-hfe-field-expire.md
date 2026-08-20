# R-25 下 — HFE 字段过期: 两级注册与条件 TTL

> 前置: [[R-25-上]] (编码三态) + [[R-21-db]] (hexpires/ebuckets) + [[R-22-expire]] (主动过期配额) | 引出: [[R-29-pubsub]] (hexpired 通知) + [[R-9-replication]] (HDEL 传播) | 对照: [[R-22-expire]] (键级 TTL → 字段级镜像)
> 🔴 A | 4 KP | [模式: 两级注册+惰性三态+条件矩阵+三阶段框架]
> Pass 2 闭环: q5(惰性链) q6(条件 TTL) q7(命令族) q8(全局注册)

**读者处境**: hash 的字段也能过期?过期字段什么时候被删?HEXPIRE 的 GT/LT 和 EXPIRE 一样吗?一个 hash 里 100 万字段各带 TTL, 怎么管理?这篇拆 HFE: 惰性过期链、条件 TTL 矩阵、命令族、两级注册。

### 1. 惰性过期链 — GETF 三态

场景: 读一个过期字段会发生什么?
源码路径:
- hashTypeGetValue (t_hash.c:711-779): 查找 (listpack 家族 L716-722 / HT L724-735) → **过期判定** (L737: expiredAt >= commandTimeSnapshot, 时间冻结)
- **角色语义** (L740-753): CLIENT_MASTER 视为有效 (L742-743) / 从库用户 → GETF_EXPIRED 不删 (L746) / loading/pause/AVOID → 只报 (L749-753)
- **主库删除链** (L760-778): hashTypeDelete (L761) → **propagateHashFieldDeletion** (L762, HDEL 合成) → stat_expired_subkeys (L763) → notify "hexpired" (L769) → **空 hash → dbDelete + GETF_EXPIRED_HASH** (L770-775)
- GETF 枚举 (L32-35): OK / NOT_FOUND / EXPIRED / **EXPIRED_HASH**
关键设计 (q5): 字段惰性过期 = 键级 expireIfNeeded (R-21) 的**字段版**: 三态 + 角色 + 传播 + 级联删键。[模式: 惰性三态]
数据流: HGET 过期字段 → 判定 → 主库删+HDEL 传播 → 空 hash → 删键。

### 2. 条件 TTL — GT/LT/NX/XX 矩阵

场景: 字段 TTL 怎么条件设置?
源码路径:
- **三阶段框架** (L1114-1238): Init (上下文收集) → SetEx (执行) → Done (全局 HFE 聚合更新)
- hashTypeSetExpiryHT (L979-1060):
  - 无 TTL 字段 (L994-1004): **XX|GT 拒** (无 TTL 视为无限 — R-22 同语义)
  - 有 TTL (L1013-1017): GT 且 prev≥new / LT 且 prev≤new / NX → 拒
  - **已过期删字段** (L1044-1051): checkAlreadyExpired → 删 + HSETEX_DELETED (同 R-22)
- minExpireFields 追踪 (L1024-1025, L1053-1054): 批量时聚合一次全局更新
关键设计 (q6): 字段条件 TTL = 键级 EXPIRE 标志 (R-22 NX/XX/GT/LT) 的**字段版镜像** + 三阶段批量框架。[模式: 条件矩阵]
数据流: HEXPIRE k f 60 GT → 条件检查 → ebRemove/ebAdd 私有 hfe → Done 全局更新。

### 3. 命令族 — hexpire/httl/hpersist

场景: 字段级 TTL 命令长什么样?
源码路径:
- **hexpireGenericCommand** (t_hash.c:3124-3247): key + N 字段 + 时间 + 条件 — 批量; 传播归一 (同 R-22)
- httlGenericCommand (L2968-3123): **-2/-1/剩余毫秒三值** (同 TTL, R-22)
- hpersistCommand (L3289): hfieldPersist (L2886: 私有 hfe 摘除 + ExpireMeta 清除)
- **hfield 结构** (L2837-2916): mstr 头 (嵌入式, 奇数地址) + 可选 ExpireMeta; hfieldIsExpired/hfieldGetExpireTime
- 传播: propagateHashFieldDeletion (L2918-2938) — HDEL 合成
关键设计 (q7): HFE 命令族 = **键级命令的字段级镜像** (条件/三值/归一全同构); mstr 奇数地址支撑 ebuckets 指针判别 (R-21 itemsAddrAreOdd=1)。[模式: 命令镜像]
数据流: HEXPIRE k f1 f2 60 → 批量 SetEx → 聚合 → 传播 HEXPIREAT。

### 4. 两级注册 — 全局代理与私有明细

场景: 100 万带 TTL 字段怎么调度?
源码路径:
- **两级** (t_hash.c:115-130): 全局 db->hexpires (hash 级, 早到字段代理, itemsAddrAreOdd=0) + 私有 hfe (字段级, itemsAddrAreOdd=1)
- **hashTypeAddToExpires** (L2040-2060): 全局注册唯一入口 (key 引用 + ebAdd)
- **hashTypeRemoveFromExpires** (L1996-2013): 摘除 + 返回 minExpire (RENAME/MOVE/COPY 续接, R-21)
- **trash 标记** (L1634): 转换期"未注册"中间态
- 主动消费 (L2073-2094): hashTypeDbActiveExpire ← R-22 (10000 字段/秒配额) → ebExpire 全局表 → onFieldExpire 回调 (L2940-2952: REMOVE/UPDATE/STOP)
- hashTypeExpire (L1853-1921): 本地私有 hfe 的 ebExpire 包装
关键设计 (q8): 两级 = **全局只挂"最早到期 hash"** (R-22 一次触达), 明细留私有 hfe; trash 是转换期状态机。[模式: 两级注册]
数据流: 字段 TTL → 私有 ebAdd + 全局代理更新 → R-22 到期 → 全局 ebExpire → 回调删字段。

### 负面空间 — HFE 刻意不做的事

- **不做字段级精确扫描**: 主动过期按全局代理批量 (早到者优先), 无逐字段全扫
- **不做 HFE 降级**: LISTPACK_EX/带元数据 HT 不转回无 TTL 编码 (转换单向)
- **不做字段级持久化格式**: RDB/AOF 由上层处理 (字段 TTL 随 hash 序列化)
- **不做过期字段计数缓存**: stat_expired_subkeys 是累计, 无实时明细
- **不做 HEXPIRE 单位混用**: EX/PX/EXAT/PXAT 一次一个 (同 SET)

→ 引出: 过期通知怎么发给订阅者?→ [[R-29-pubsub]]
