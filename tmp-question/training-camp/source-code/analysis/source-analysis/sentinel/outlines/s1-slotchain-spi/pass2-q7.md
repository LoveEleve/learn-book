# Pass 2 闭环笔记 Q7: chainMap 6000 上限 — 超限后的行为是"放弃保护"还是"重建"?

## 初始假设
- 超限可能触发驱逐(重建/淘汰旧链)。
- 实际: **什么都不做 — 直接返回 null,该资源放弃规则检查**(降级放行)。

## 验证过程
- 读 `CtSph.java:194-215` (lookProcessChain): 双检锁;`chainMap.size() >= MAX_SLOT_CHAIN_SIZE` → `return null`(L201-203)。
- 消费方 `CtSph.java:136-142` (entryWithPriority): `chain == null` → `return new CtEntry(resourceWrapper, null, context)` — 无链的 CtEntry,不执行任何检查,直接返回成功。
- 同类降级路径: `CtSph.java:113-116` (NullContext — context 超限)与 `CtSph.java:130-134` (Constants.ON 关闭)同样返回无链 CtEntry — **三种"放弃保护"路径**。
- 实现细节: L206-210 用 **copy-on-write**(new HashMap + putAll 重建 + volatile chainMap 引用替换)而非原地 put — 无锁读优化(读路径无 synchronized)。

## 代码类型
- Implementation(有界缓存 + 降级语义)

## 跨域关联
- S-2 入口: 本域是 S-2 的入口决策点(链缺失/开关关闭/上下文超限 = 三种放行)

## 结论
超过 MAX_SLOT_CHAIN_SIZE(6000)后新资源**不再建链,静默放行**(CtSph.java:194-215, 136-142);与 context 超限、全局开关关闭构成三个降级放行点。链表用 copy-on-write 替换保证读无锁(CtSph.java:206-210)。