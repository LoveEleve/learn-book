# 闭环笔记 GW-2-q4 — 配置绑定体系: ShortcutType 归一化 + Boot Binder

假设: 过滤器/谓词工厂的配置绑定分两步: ShortcutType 把短路语法归一化为完整 Map, 再经 Spring Boot Binder 绑定到 Config 类。

验证过程:
- **ShortcutConfigurable** (ShortcutConfigurable.java:86-150): shortcutType() 默认 **DEFAULT** (L86-87) + shortcutFieldOrder (L92-95) + **ShortcutType 枚举 3 种**:
  - **DEFAULT** (L107-125): 遍历 args → `normalizeKey(key, entryIdx, ...)` (L114, 按 fieldOrder 归一键) + `getValue(parser, beanFactory, value)` (L115, **SpEL 解析**) — `Path=/user/**` → `{patterns: [/user/**]}`
  - **GATHER_LIST** (L127-140): 所有值收集为 List (L134-137, fieldOrder size 1 断言 L135)
  - GATHER_LIST_TAIL_FLAG: 列表 + 尾布尔
- **ConfigurationService** (ConfigurationService.java:45-180): SpelExpressionParser (L53) + `bindOrCreate(Bindable, properties)` (L88, **Boot Binder**) + normalizeProperties → `shortcutType().normalize(...)` (L138-141) → `Bindable.of(configClass)` → bindOrCreate (L148-149)
- **短路语法**: `AddRequestHeader=X-Request,value` vs 完整 `{name: AddRequestHeader, args: {header: X-Request, value: value}}` — 同一 Config 类
- 事件钩子: FilterArgsEvent/PredicateArgsEvent (GW-1 q2 已见)

代码类型: Algorithmic (配置归一化)

结论: 绑定体系 = **短路归一化 + Binder**: ShortcutType 决定语法解释 (DEFAULT 键序/GATHER_LIST 集合), SpEL 值解析, Boot Binder 最终绑定到类型化 Config; 两条配置路径 (YAML 短路/完整 Map/Java DSL) 汇合到同一绑定管线。**被放弃的方案: 手写参数解析** — Binder 提供类型转换/校验/嵌套绑定; ShortcutType 让语法可扩展 (列表/标志变体)。 [跨域: GW-1 q2 装配消费; GW-3 谓词工厂同体系; Boot Binder] [模式: 归一化+绑定] (ShortcutConfigurable.java:86-150; ConfigurationService.java:45-149)
