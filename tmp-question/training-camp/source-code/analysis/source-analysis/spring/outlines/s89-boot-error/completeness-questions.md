# S-25 Error 处理自动装配 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么改错误端点路径? | §2 (server.error.path) |
| 2 | 浏览器和接口错误响应为何不同? | §2 (内容协商 HTML/JSON) |
| 3 | 怎么自定义错误属性/包含 trace? | §3 (ErrorAttributeOptions + server.error.include-*) |
| 4 | 怎么关掉白底错误页? | §3 (server.error.whitelabel.enabled=false) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 before WebMvcAutoConfiguration? | §1 (先注册再接管 /error) |
| 6 | 为什么一个端点两响应? | §2 (Accept 内容协商) |
| 7 | 为什么敏感字段按需开放? | §3 (避免泄露 stackTrace) |
| 8 | 与 C-13 异常解析链的关系? | 边界 (C-13 解析未处理才转 /error) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | /error 端点从哪来? | §1/§2 (BasicErrorController) |
| 10 | 错误 JSON 里的字段怎么来的? | §2 (DefaultErrorAttributes) |
| 11 | whitelabel 兜底页何时出现? | §3 (无模板 + 开启) |

## 覆盖: 11 问 / 3 身份 / 100%
