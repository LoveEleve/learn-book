# T-5 Mapper 路由 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `exactWrappers/wildcardWrappers/extensionWrappers` 三个数组有什么区别？ | §1.1 |
| 2 | 同一个 URI `/app/user` 同时匹配 exact `/user` 和 prefix `/*` — 哪个生效？ | §1.2 |
| 3 | `MapperListener.containerEvent()` 怎么区分 ADD_CHILD_EVENT 和 ADD_MAPPING_EVENT？ | §2.1 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | Tomcat 为什么用 Container 树 + Mapper + Pipeline 三套独立结构？ | §2.2 |
| 5 | MapperListener 启动时怎么构建路由表？运行时热部署怎么更新？ | §2.1 |
| 6 | Mapper 的路由表用数组+二分查找而非 HashMap — 为什么？ | §1.1 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 7 | `mappingData.host/context/wrapper` 三个字段是谁填的？ | §1.2 |
| 8 | Tomcat 一个请求从 TCP accept 到 Servlet.service() 经历了哪些步骤？ | §2.2 全链路 |

## 覆盖: 8 问 / 3 身份 / 100%
