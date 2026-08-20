# 闭环笔记 q2: evictionPoolPopulate — 跨 DB 采样池

## 假设
全局池 (16 槽) + 每 DB 采样 (5 键) + 有序插入 (大 idle 靠右) — 跨 DB 找全局最优候选。

## 验证过程
- 结构 (evict.c:33-43): EVPOOL_SIZE=16 / EVPOOL_CACHED_SDS_SIZE=255; 池项 = idle + key + cached + dbid + slot
- evictionPoolPopulate (L125-225):
  - FAIR 选槽 (L129: kvstoreGetFairRandomDictIndex — R-21 BIT) + dictGetSomeKeys (L130, samples=5)
  - **idle 三路径** (L152-168): LRU → estimateObjectIdleTime; LFU → `255 - LFUDecrAndReturn(o)` (反频率); VOLATILE_TTL → `ULLONG_MAX - TTL` (越早到期越大)
  - TTL 特例 (L143-147): 不用值对象 — `policy != VOLATILE_TTL` 时才查找主 dict 取值
  - **有序插入** (L173-205): 升序扫描找插入点; 池满且比最差还差 → 跳过; 中间插入 memmove 右移 / 左移丢弃最小
  - **cached sds 复用** (L207-218): ≤255B 键直接 memcpy 进预分配 cached (注释: 分析器证明分配昂贵)
- 全 DB 填充 (L568-600, performEvictions 内):
  - ALLKEYS → keys 表; 否则 expires 表 (L577-581)
  - 每 DB: 非空槽数 l 循环采样, 满 samples 或 DB 键 < samples×10 提前退出 (L589-598)
  - **跨 DB 全局池** (注释 L571: "We don't want to make local-db choices")
- 选键 (L604-630): 从池尾 (最大 idle) 回退扫描, 幽灵键 (已删) 跳过, 命中即删池项
- 池不随删除更新 (注释 L94-95: "we don't remove keys from the pool when they are deleted") — 幽灵键由选键时跳过兜底

## 代码类型
Mechanism (近似最优选择)

## 跨域关联
- R-21 (kvstore FAIR 随机/BIT) / R-1 (robj lru 字段) / R-22 (volatile 用 expires 表)

## 结论
淘汰质量 = "全局采样 + 池内有序" 近似: 每次淘汰取全 DB 采样中最 idle 的键 (而非局部 DB), 池跨调用复用 (节省重复采样)。cached sds 复用消除每键分配。幽灵键机制保证池成员过期后自动跳过。
源码位置: evict.c:33-43,82-100,125-225,568-630
