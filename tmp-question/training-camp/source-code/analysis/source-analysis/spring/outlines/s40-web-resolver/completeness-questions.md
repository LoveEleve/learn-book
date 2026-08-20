# W-4 HandlerMethodArgumentResolver 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `@PathVariable Long id` 的值到底从哪来？URL 怎么解析出 {id}？ | §1 (resolveName) + §2 (URI_TEMPLATE_VARIABLES_ATTRIBUTE) |
| 2 | `@RequestParam(defaultValue="0")` 的默认值机制怎么工作？ | §1 (defaultValue 分支) |
| 3 | `@RequestBody UserDto dto` + @Valid — 校验失败会发生什么？ | §3 (validateIfApplicable → MethodArgumentNotValidException) |
| 4 | @RequestParam 单值/多值/缺失分别怎么处理？ | §2 (getParameterValues + 单值[0] + MissingServletRequestParameterException) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | @PathVariable 和 @RequestParam 为什么共用 AbstractNamedValue 模板？差异点在哪？ | §1 (模板方法) + §2 (resolveName 差异) |
| 6 | @RequestBody 为什么不继承命名值模板？messageConverters 链如何选择转换器？ | §3 (canRead 双维度匹配) |
| 7 | 新增自定义参数类型(如 @AuthenticationPrincipal)如何扩展？ | §3 尾部 (扩展段) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 8 | supportsParameter 和 resolveArgument 各做什么？为什么要两个方法？ | §1 (接口双方法) |
| 9 | 类型转换 (String "1" → Long 1L) 发生在哪一步？ | §1 (convertIfNecessary L136) |

## 覆盖: 9 问 / 3 身份 / 100%
