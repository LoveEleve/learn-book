# OF-3 契约集成 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (SpringMvcContract 611 / annotation/ 7 处理器 / SpringQueryMap / CollectionFormat)
> 09 域级审计: OPENFEIGN-PLAN OF-3 (接受, 数字 7 实证) — 断言 "SpringMvcContract→@RequestMapping + 7 处理器" 已 grep 验证

## 入口展开 (Level-1~3, 已读源码)

### Level-1: 类级处理 — processAnnotationOnClass (L226-260)

```
SpringMvcContract extends Contract.BaseContract implements ResourceLoaderAware (L98)
processAnnotationOnClass:
├── **@RequestMapping 类级禁止** (L227-231): IllegalArgumentException
│   "not allowed on @FeignClient interfaces" — 路径在方法级声明!
└── CollectionFormat 类级 (L233-237)
```

### Level-2: 方法级处理 — processAnnotationOnMethod (L287+)

```
processAnnotationOnMethod:
├── CollectionFormat 方法级 (L289-291)
├── 非 @RequestMapping 且无组合注解 → return (L293-296)
│   ← 组合注解判定: annotationType().isAnnotationPresent(RequestMapping.class)
├── findMergedAnnotation(method, RequestMapping.class) (L298) — @GetMapping 等组合解析
├── HTTP method: methods 空 → **默认 GET** (L303-305) + checkOne (L306)
├── path: checkAtMostOne + value 解析 + **前缀补 "/" + removeTrailingSlash** (L309-317)
└── (后续: headers/params/consumes/produces 处理?)
```

### Level-3: 参数处理 — 注册表分发 (L382-390)

```
参数分发:
└── annotatedArgumentProcessors.get(参数注解类型) → **processor.processArgument(context, 注解, method)** (L390)
    + isHttpAnnotation 标志

处理器注册表 (L470-485):
├── toAnnotatedArgumentProcessorMap: 注解类型 → 处理器 Map
└── getDefaultAnnotatedArgumentsProcessors: 默认 7 个 (annotation/ 穷举):
    MatrixVariable/PathVariable/RequestParam/RequestHeader/CookieValue/QueryMap/RequestPart

GET 未注解参数警告 (L245-261):
└── isGetMethod && 有参数 && shouldWarnAboutUnannotatedGetParameters → LOG.warn
    ← 生产提示: GET 未注解参数的处理语义
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-3 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "SpringMvcContract 611 行" | wc -l = 611 | 接受 |
| "7 个 AnnotatedParameterProcessor" | annotation/ 穷举 = 7 | 接受 (数字实证) |
| "@RequestMapping 组合注解解析" | findMergedAnnotation + isAnnotationPresent (L293-298) | 接受 |
| "类级禁止" | L227-231 IllegalArgumentException | 补锚 (PLAN 未提: @RequestMapping 不允许在接口类上) |
| 补锚: HTTP method 默认 GET | L303-305 | 补锚 |
| 补锚: GET 未注解参数警告 | L245-261 | 补锚 |
| 合并项: SpringQueryMap/CollectionFormat/FeignFormatterRegistrar | 存在 (PLAN 已列) | 接受 (随域展开) |

## 待展开 (下一层)

1. path 处理后续 (headers/params/consumes/produces)
2. processAnnotationOnParameter 完整上下文 (RequestTemplate 构建)
3. 7 个处理器各自语义 (PathVariable 模板变量/RequestParam 查询参数等)
4. SpringQueryMap/FeignFormatterRegistrar 合并项细节
5. 泛型返回解析 (BaseContract Types.resolveReturnType — feign 本体面)
