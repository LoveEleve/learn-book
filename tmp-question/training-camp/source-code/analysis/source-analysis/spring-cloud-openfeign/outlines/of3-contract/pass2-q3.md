# OF-3 契约集成 — Pass 2 闭环 Q3: 参数面 (注册表 + Pageable + GET 警告)

> 核心: processAnnotationsOnParameter + 7 处理器 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 参数怎么分发到处理器? Pageable 怎么处理? GET 参数警告为什么?**

## 机制链 (已实证)

```
processAnnotationsOnParameter (L372+):
├── **Pageable 特殊处理** (L362-370): Pageable 类型参数 →
│   ├── queryMapParamPresent 检查 (已有 QueryMap 则不覆盖)
│   └── data.queryMapIndex(paramIndex) — 分页参数映射 (OF-4 分页编码关联!)
├── context = SimpleAnnotatedParameterContext (L379)
├── 逐注解循环 (L381+): annotatedArgumentProcessors.get(注解类型)
│   → processor.processArgument(context, 注解, method) (L390)
│   + isHttpAnnotation 标志
└── 7 处理器注册表 (annotation/ 穷举):
    MatrixVariable/PathVariable/RequestParam/RequestHeader/CookieValue/QueryMap/RequestPart

GET 未注解参数警告 (L245-270):
└── isGetMethod && 有参数 && 无注解 → LOG.warn:
    "[OpenFeign Warning] Feign method 'xxx' is declared as GET with parameters, but none of
    the parameters are annotated... This may result in fallback to POST at runtime."
    ← 关键生产提示: 未注解参数会被 feign 当 body, GET 带 body → 运行时降级 POST!
```

## 关键设计 (why)

1. **注册表分发**: 注解类型 → 处理器 Map (可扩展, 自定义 AnnotatedParameterProcessor)
2. **Pageable 特判**: 分页对象整体作为 QueryMap — Spring Data 分页支持 (先于注解分发)
3. **GET 警告**: 未注解参数 → feign 推断为 body → GET 无 body 语义 → **运行时降级 POST** — 提前警告防踩坑
4. **queryMapParamPresent 防冲突**: 显式 QueryMap 与 Pageable 并存时以显式优先

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Pageable → queryMapIndex | SpringMvcContract.java:362-370 |
| 注册表分发 | SpringMvcContract.java:381-390 |
| 7 处理器注册表 | SpringMvcContract.java:470-485 |
| GET 警告完整文案 | SpringMvcContract.java:245-260 |

## 负面空间 (Q3 面)

- 不自定义注解处理器热注册 (构造时定)
- 不参数级排序 (注解顺序)
- 不做 GET body 支持 (警告后由 feign 降级)
