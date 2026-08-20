# OF-3 契约集成 — Pass 2 闭环 Q2: 请求面 (四解析 + CollectionFormat)

> 核心: parseProduces/Consumes/Headers/Params | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: produces/consumes/headers/params 怎么变成请求模板?**

## 机制链 (已实证)

```
parseProduces (L420-426): produces[] → **md.template().header(ACCEPT, ...)**
parseConsumes (L430-434): consumes[] → **md.template().header(CONTENT_TYPE, ...)**
parseHeaders (L438-447):
├── headers[] 逐条: "key=value" 拆分 (indexOf '=')
├── "!=" 排除 (L443: !header.contains("!="))
└── resolve 占位符 (L445): header(key), header(value)
parseParams (L451-457):
└── params[] → NameValueResolver (条件参数, "!=" 排除语义)
CollectionFormat (q1): 类级/方法级 → template.collectionFormat(value)
```

## 关键设计 (why)

1. **MVC 语义 → Feign 模板**: produces→Accept / consumes→Content-Type — 注解语义直接映射请求头
2. **headers 键值拆分**: "key=value" 解析 + resolve 占位符 — 配置动态化
3. **"!=" 排除语义**: headers/params 的否定条件 (MVC 特性)
4. **CollectionFormat**: 集合参数格式 (CSV/SSV 等) — 类级+方法级双支持

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| parseProduces → ACCEPT | SpringMvcContract.java:420-426 |
| parseConsumes → CONTENT_TYPE | SpringMvcContract.java:430-434 |
| parseHeaders 键值拆分 | SpringMvcContract.java:438-447 |
| parseParams + NameValueResolver | SpringMvcContract.java:451-457 |
| CollectionFormat 双级 | SpringMvcContract.java:233-237,289-291 |

## 负面空间 (Q2 面)

- 不 headers 多值支持 (TODO 注释 L439: 仅单值)
- 不动态 header 生成 (注解静态)
- 不 params 表达式求值 (静态条件)
