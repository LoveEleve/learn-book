# Pass 2 闭环笔记 Q4: DataSource 如何推送规则

## 验证过程

- `AbstractDataSource<S,T>` 持有 `Converter<S,T> parser` 与 `SentinelProperty<T> property`，构造时创建 `DynamicSentinelProperty<T>` (`AbstractDataSource.java:24-37`)。
- `loadConfig()` 读取外部源 `readSource()`，再用 converter 转为目标规则类型 (`AbstractDataSource.java:40-48`)。
- `getProperty()` 返回该 property，调用方把它注册到 `FlowRuleManager` / `AuthorityRuleManager` / 其他 RuleManager 的 `register2Property`。
- 外部具体数据源（Nacos/Apollo/Redis/ZK 等）负责实现 `readSource` 和刷新触发；解析只通过 Converter 解耦。
- 因此完整链路是：外部配置 → DataSource 读取 → Converter 解析 → `property.updateValue` → RuleManager listener → 运行时规则。

## 结论

DataSource 不直接操作任何 RuleManager，它只生产 `SentinelProperty<T>`；规则管理器通过 property 订阅获得动态更新。这使同一个数据源底座能接入多种规则类型和多个配置中心。