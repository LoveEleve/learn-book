# OF-3 契约集成 — Pass 2 闭环 Q4: 合并项面 (SpringQueryMap + Formatter + CollectionFormat)

> 核心: SpringQueryMap/FeignFormatterRegistrar/CollectionFormat | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: issue 规划的合并项怎么工作? 格式化/集合格式/对象查询参数?**

## 机制链 (已实证)

```
SpringQueryMap (L33-37): @Retention(RUNTIME) + @Target(PARAMETER) — 标记注解
└── QueryMapParameterProcessor (L34-48): queryMapIndex 设置 — 对象整体展开为查询参数

FeignFormatterRegistrar (extends FormatterRegistrar) (L27):
├── FeignClientsConfiguration L88 注入 List<FeignFormatterRegistrar>
├── feignConversionService (L152-160): DefaultFormattingConversionService
│   + feignFormatterRegistrars.forEach(addFormatters) (L154)
└── feignContract(conversionService) (L147): SpringMvcContract 构造参数 — 格式化用于参数转换

CollectionFormat (类级 L233-237 + 方法级 L289-291):
└── template.collectionFormat(value) — 集合参数序列化格式 (CSV/SSV 等)
```

## 关键设计 (why)

1. **对象查询参数**: SpringQueryMap + QueryMapParameterProcessor — POJO 整体展开为 query 参数 (替代手动拼接)
2. **格式化注册器**: FeignFormatterRegistrar → ConversionService → SpringMvcContract — 参数类型转换扩展点
3. **集合格式**: CollectionFormat 类级/方法级双支持 — 集合参数编码格式控制
4. **装配链**: FeignClientsConfiguration 组装 (ConversionService → Contract) — OF-7 配置类面

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| SpringQueryMap 注解 | SpringQueryMap.java:33-37 |
| QueryMapParameterProcessor | annotation/QueryMapParameterProcessor.java:34-48 |
| FeignFormatterRegistrar 装配 | FeignClientsConfiguration.java:88,152-160 |
| feignContract 构造 | FeignClientsConfiguration.java:147 |
| CollectionFormat 双级 | SpringMvcContract.java:233-237,289-291 |

## 负面空间 (Q4 面)

- 不 SpringQueryMap 嵌套展开 (单层对象)
- 不格式化自动发现 (显式注册)
- 不集合格式自动协商 (注解声明)
