# Pass 2 闭环笔记 Q4: EntryType — IN/OUT 的分流与消费面

## 初始假设
- EntryType 影响 NodeSelector/ClusterBuilder 的统计方向。

## 验证过程
- 读 `EntryType.java`: 仅 IN(入站)/ OUT(出站)两值, 无内部(注释说 "internal" 已在 1.8.4 移除语义, 见 SystemSlot 消费)。
- grep 全 core 消费方只有 3 处:
  - `StatisticSlot.java:71,88,106,140`: `getEntryType() == EntryType.IN` 时给 **Constants.ENTRY_NODE(全局入口节点)** 记 pass/block/thread — IN 流量参与全局统计。
  - `SystemRuleManager.java:300`: `getEntryType() != EntryType.IN` → 直接跳过系统规则检查 — **只有 IN 流量可被系统规则拦截**(文档注释: "only inbound traffic could be blocked by SystemRule" 实证)。
  - `ResourceWrapper.java:57`: 仅 getter。
- **修正假设**: NodeSelectorSlot / ClusterBuilderSlot **不消费 EntryType**(grep 零命中)— 统计方向的决定点在 StatisticSlot, 与节点选择无关。

## 代码类型
- Glue(分流标签, 消费端在统计与系统保护)

## 跨域关联
- S-2 → S-5: ENTRY_NODE 全局入口统计与 S-5 的树结构相关
- S-2 → S-3: SystemRuleManager 的 IN 前置判断是系统保护入口(本域只点分流, 规则归 S-3)

## 结论
EntryType 只影响两件事: 是否计入全局入口节点(StatisticSlot, IN 才记)与是否可被系统规则拦截(SystemRuleManager.java:300, 非 IN 直跳)。节点选择/链构建与 EntryType 无关 — 假设方向反了, 已修正。