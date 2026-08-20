# C-6 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + harness 25/25 实证

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "trySetValue 四步" — SharedValue.java:177-200 逐字核对 (比对 zxid+value L183-185 / -1 抛 L186-188 / withVersion L191 / BadVersion 重读 L194-199) | 通过 ✅ (harness A: 两过期版本 CAS 恰 1 成功) |
| 2 | 事实 | §1 "updateValue zxid 单调" — L202-214 CAS 自旋 | 通过 ✅ |
| 3 | 事实 | §2 "tryOnce 版本 CAS" — DistributedAtomicValue.java:273-296 (L280-284 setData withVersion) | 通过 ✅ (harness C/D: 顺序 10 / 并发 5×10=50) |
| 4 | 事实 | §2 "MakeValue 抽象" — L23 | 通过 ✅ |
| 5 | 事实 | §3 "PromotedToLock 升级" — L221-250 (mutex.acquire L225, 默认不重试 L234-241) | 通过 ✅ (源码核对; harness 未直接触发升级路径 — 低冲突场景乐观即胜) |
| 6 | 事实 | §4 "CachedAtomicLong 分块" — CachedAtomicLong.java:48-70 | 通过 ✅ (harness G: 25 次全唯一; 注意构造签名 = (DistributedAtomicLong, int) — 非 client/path 三元 — 大纲表述用机制不写构造签名) |
| 7 | 事实 | §5 "SharedCount 委托" — SharedCount.java:48-50, L160-168 | 通过 ✅ (harness H: 版本 CAS 恰 1 成功 + countHasChanged) |
| 8 | 事实 | §1 "initialize 布尔返回" — DistributedAtomicLong.java:89 `boolean initialize` (非 AtomicValue) | 通过 ✅ (harness F 实证; 初始实现误当 AtomicValue — 修正) |
| 9 | 事实 | §2 "AtomicValue succeeded 必查" — AtomicValue.java:32-53 | 通过 ✅ |
| 10 | 过程 | **harness 实证发现**: 高并发 5 线程×10 次全 succeeded — 乐观重试在低冲突场景近乎零失败 | 大纲可引用: 乐观 CAS 冲突率与重试关系 |
| 11 | 结构 | 负面空间 "不做事务" 与 C-1 事务面不冲突 (事务是 multi 原子, 这里是单节点 CAS) | 通过 ✅ |

**结论**: 11 项核对 1 修正 (initialize 返回类型)。harness 25/25 覆盖: A 版本 CAS 互斥 B watcher 更新 C/D 原子累加 E compareAndSet F initialize G 号段 H SharedCount。
