# W-2 @RequestMapping 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 启动时 Spring 怎么扫描所有 @RequestMapping 方法？ | §1 |
| 2 | GET /users/1 和 GET /users/2 — 怎么知道哪个 HandlerMethod？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | MappingRegistry 有四个 Map(registry/pathLookup/nameLookup/corsLookup) — 为什么需要四个？ | §1 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | @GetMapping 和 @RequestMapping(method=GET) 有什么区别？ | §1 |

## 覆盖: 4 问 / 3 身份 / 100%
