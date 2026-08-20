# OF-3 契约集成 — Pass 2 闭环 Q1: 注解面 (类级禁止 + 方法级解析)

> 核心: processAnnotationOnClass/Method | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Spring MVC 注解怎么变成 Feign 请求模板? 类级为什么禁止?**

## 机制链 (已实证)

```
processAnnotationOnClass (L226-260):
├── **@RequestMapping 类级禁止** (L227-231): IllegalArgumentException
│   "@RequestMapping annotation not allowed on @FeignClient interfaces"
│   ← 路径必须在方法级 — 接口级路径歧义防护
└── CollectionFormat 类级 (L233-237)

processAnnotationOnMethod (L287+):
├── CollectionFormat 方法级 (L289-291)
├── 组合注解判定 (L293-296): 非 @RequestMapping 且 !isAnnotationPresent(RequestMapping) → return
│   ← @GetMapping/@PostMapping 等组合注解被接受
├── findMergedAnnotation(method, RequestMapping.class) (L298) — 组合注解合并解析
├── HTTP method (L302-306): methods 空 → **默认 GET** + checkOne (多 method 报错)
│   → data.template().method(Request.HttpMethod.valueOf(...))
├── path (L308-317): checkAtMostOne + value[0] + **resolve 占位符** (L343-347) +
│   前缀补 "/" + **removeTrailingSlash** → template.uri(pathValue, true)
└── 四解析 (L326-335): parseProduces → parseConsumes → parseHeaders → parseParams
    → indexToExpander + Pageable 特殊处理 (q3)
```

## 关键设计 (why)

1. **类级禁止**: @RequestMapping 在接口上语义歧义 (多方法共享路径) — 强制方法级声明
2. **组合注解支持**: isAnnotationPresent + findMergedAnnotation — @GetMapping 等 Spring 风格注解无缝
3. **GET 默认**: 无 method 声明默认 GET — 与 Spring MVC 语义一致
4. **checkOne/checkAtMostOne**: 多 method/多 path 校验 — 配置错误早暴露
5. **resolve 占位符**: ${...} 环境变量解析 (ResourceLoaderAware)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 类级禁止 | SpringMvcContract.java:226-231 |
| 组合注解判定 | SpringMvcContract.java:293-298 |
| GET 默认 + checkOne | SpringMvcContract.java:302-306 |
| path 解析 + 占位符 | SpringMvcContract.java:308-317,343-347 |
| 四解析 | SpringMvcContract.java:326-335 |

## 负面空间 (Q1 面)

- 不做类级路径拼接 (禁止)
- 不做多 method 支持 (checkOne)
- 不做动态 method (注解静态声明)
