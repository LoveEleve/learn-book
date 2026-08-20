# Pass 2 闭环笔记 Q3: @Spi alias 别名机制 — classMap 的 key 是什么?

## 初始假设
- @Spi 有 alias 属性,可注册多个别名。
- 实际:**@Spi 注解只有 4 个属性** — value()(别名)/ isSingleton()/ isDefault()/ order()(Spi.java:34-49),无 alias 数组。

## 验证过程
- 读 `SpiLoader.java:384-391` (load 解析): `aliasName = spi == null || "".equals(spi.value()) ? clazz.getName() : spi.value()` — **别名缺省 = 全限定类名**。
- 读 `SpiLoader.java:386-390`: 重复别名 → fail 硬错误("Found repeat alias name");`SpiLoader.java:374-376`: 重复类(多 jar 声明同实现)→ warn 跳过。
- 读 `SpiLoader.java:284-295` (loadInstance(aliasName)): classMap.get(aliasName) → createInstance;找不到 fail。
- 消费方检索: 核心 + extension 中 loadInstance(aliasName) **无调用方** — 别名 API 是给外部用户(如按名加载自定义实现)的扩展口。
- PLAN 断言修正: "@Spi alias 多别名" → **@Spi.value() 单别名,缺省全限定类名**。

## 代码类型
- Glue(名称↔实现映射)

## 跨域关联
- S-1 → S-7 规则管理: DataSource 等扩展面用同样模式 (loadFirstInstanceOrDefault)

## 结论
classMap: aliasName(@Spi.value(), 缺省全限定类名) → Class。别名冲突 = 启动硬失败,重复实现 = warn 跳过(多 jar 场景)。别名 API 在核心内部零消费,面向扩展方(SpiLoader.java:384-391, 284-295 + Spi.java:34-49)。