# W-3 RequestMappingHandlerAdapter 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `ha.handle()` 如何把 HandlerMethod 变成真正的业务方法调用？ | §1 |
| 2 | 业务方法参数 (@PathVariable/@RequestBody/@ModelAttribute) 从哪来？ | §1 (getMethodArgumentValues) |
| 3 | 业务方法返回 User 对象 — 怎么决定是渲染视图还是输出 JSON？ | §2 (invokeAndHandle) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | 为什么要 HandlerAdapter 接口？三个 Adapter(supports 匹配) 各自适配什么 Handler？ | §3 |
| 5 | 26 个 ArgumentResolver 为什么分三组(注解/类型/catch-all)？新增参数类型如何扩展？ | §3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | mavContainer.requestHandled = true 是什么意思？为什么会导致"短路"？ | §2 |
| 7 | invokeForRequest 和 doInvoke 有什么区别？为什么参数解析要独立出来？ | §1 |

## 覆盖: 7 问 / 3 身份 / 100%
