# W-6 ViewResolver 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | Controller 返回 String "userList" 时, 从字符串到 JSP 响应经过哪些环节？ | §1 (render 入口→resolveViewName 链) |
| 2 | 配置了 prefix/suffix 后, "userList" 是怎么变成 /WEB-INF/views/userList.jsp 的？ | §2 (buildView L571 setUrl) |
| 3 | "redirect:/users" 和 "forward:/login" 分别发生什么？ | §2 (createView L475-489: 302 vs 服务端转发) |
| 4 | 同一个 URL 怎么做到浏览器看 HTML、客户端拿 JSON？ | §3 (ContentNegotiatingViewResolver 协商) |
| 5 | 我写了不存在的视图名(拼错 JSP 文件名)会怎样？ | §1 (链尾 null→ServletException 500) + §2 (checkResource) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么 resolveViewName 返回 null 而不是抛异常？多个 resolver 如何协作？ | §1 关键设计 (责任链) |
| 7 | 视图缓存为什么收敛在基类？双缓存解决了什么并发问题？ | §1 (LRU+创建锁) |
| 8 | 视图层协商与 W-5 转换器协商的异同？ | §3 关键设计 (选 View vs 选转换器) |
| 9 | 为什么 ContentNegotiatingViewResolver 必须 order 最高？ | §3 (L106 HIGHEST_PRECEDENCE) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 10 | JSP 里为什么能用 ${name} 直接访问 Controller 的 model 数据？ | §2 (exposeModelAsRequestAttributes L142) |
| 11 | 视图解析的"逻辑名"和"物理资源"为什么分离？ | §1 关键设计 + §2 (checkResource) |

## 覆盖: 11 问 / 3 身份 / 100%
