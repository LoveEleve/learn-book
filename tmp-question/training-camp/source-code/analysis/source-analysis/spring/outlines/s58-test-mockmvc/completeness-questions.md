# C-16 MockMvc 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 没启动 Tomcat 怎么测 Controller？ | §2 (MockFilterChain→真实 DispatcherServlet) |
| 2 | webAppContextSetup 和 standaloneSetup 区别？ | §1 (集成 vs 单元) |
| 3 | 怎么断言响应状态码/JSON/重定向？ | §3 (status/content/jsonPath/redirectedUrl) |
| 4 | 怎么传请求参数/请求体？ | §3 (param/content/uri) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | MockMvc 为什么复用真实 DispatcherServlet？ | §2 关键设计 (测真实行为) |
| 6 | 两种装配对应测试金字塔的哪两层？ | §1 (集成 vs 控制器单元) |
| 7 | ResultMatcher 和 ResultHandler 为什么分离？ | §3 (验证 vs 动作) |
| 8 | MockMvc 测到了哪些真实逻辑？ | §2 (W-1 全链路复用) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | perform 返回的 ResultActions 是什么？ | §2/§3 (收口断言) |
| 10 | andExpect 返回 this 有什么用？ | §3 (链式累积校验) |

## 覆盖: 10 问 / 3 身份 / 100%
