# Pass 2 闭环笔记 Q11: ResourceWrapper 链键 — 只有资源名参与 equals/hashCode?

## 初始假设
- 链缓存 key 是完整的 ResourceWrapper(名+类型)。
- 实际: **仅资源名**决定相等性 — String 与 Method 包装器若同名则共享同一链。

## 验证过程
- 读 `ResourceWrapper.java:82-93`: `hashCode() = getName().hashCode()`;`equals()` 只比较 `rw.getName().equals(getName())` — 注释原文 "Only getName() is considered"。
- 推论: 同名 StringResourceWrapper 与 MethodResourceWrapper **等价** → chainMap 中同资源名只建一条链(CtSph.java:195-210)。
- 含义: 同一业务资源在不同入口(如 HTTP 方法 vs 方法注解)注册时共享同一套规则链 — 与 Sentinel 以"资源名"为规则作用域的设计一致(FlowRule.setResource(String))。
- 测试实证: SpiLoaderTest 的 toString/加载测试不涉此键;chainMap 键语义在 CtSphTest/entrySize 相关测试中体现。

## 代码类型
- Implementation(键语义设计)

## 跨域关联
- S-2 入口: context 维度(名字+origin)与资源维度(仅名)的键设计对比
- S-3 流控: 规则 resource 字段与链键同名 → 命中链即命中规则集

## 结论
chainMap 键 = 资源名(String wrapper 与 Method wrapper 同名等价)(ResourceWrapper.java:82-93 + CtSph.java:195-210)。资源名是规则作用域与链缓存的统一键 — 一条链绑定同名全部规则。