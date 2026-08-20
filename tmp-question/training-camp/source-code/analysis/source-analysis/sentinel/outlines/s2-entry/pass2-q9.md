# Pass 2 闭环笔记 Q9: resourceType — 分类标签如何穿透(含 equals 盲区)

## 初始假设
- 初始假设: resourceType 是 ResourceWrapper 的一部分, 参与链的唯一性。

## 验证过程
- 读 `SphU.java:293-329`: `entry(name, resourceType, trafficType[, args])` → `Env.sph.entryWithType`; `asyncEntryWithType`(1.7.0 起)。
- 读 `CtSph.java:338-355`: entryWithType → `new StringResourceWrapper(name, entryType, resourceType)` → entryWithPriority(L344-348); asyncEntryWithType 同构 → asyncEntryWithPriorityInternal(L351-355) — **统一汇聚, 无分叉**。
- 读 `StringResourceWrapper.java:29-36`: 三参构造 → super(name, e, resType); 两参构造默认 ResourceTypeConstants.COMMON。
- **盲区修正(重要)**: 读 `ResourceWrapper.java:82-94`: `equals/hashCode 只按 getName()` — **resourceType 与 entryType 都不参与链 key**!同一名字无论分类/方向如何都共享同一条链(chainMap 按 name 唯一)。
- 定位: resourceType 是纯展示/分流标签(如 Dashboard 按资源类型过滤), 不影响执行语义。

## 代码类型
- Glue(标签穿透, 零执行语义)

## 跨域关联
- S-2 → S-1: chainMap key = name 语义与 S-1 的 ResourceWrapper 契约一致(需 S-1 中篇回查: 若此前断言 "resourceType 参与 key" 需勘误)
- S-2 → S-10: Dashboard 资源列表按 resourceType 展示

## 结论
resourceType 只是穿透到 ResourceWrapper 的 int 分类标签(默认 COMMON), 不参与链唯一性 — equals 仅 name(ResourceWrapper.java:82-94)。所有 entry 变体最终都汇聚到 entryWithPriority / asyncEntryWithPriorityInternal 两个私有方法(CtSph.java:344-355)。