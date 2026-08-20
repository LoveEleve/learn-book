# S2-5 父子容器 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | containsBean("ds") 和 containsLocalBean("ds") 有什么区别？为什么子容器中前者返回 true 后者返回 false？ | §2 |
| 2 | getBean parent 委托什么时候走优化路径(直调 doGetBean)？什么时候走标准路径？ | §2-3 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | setParent 为什么要三线同步(BeanFactory + Environment + MessageSource)？如果只同步一个会出什么问题？ | §1 |
| 4 | Spring Boot 为什么用父子容器而非单一大容器？什么Bean放parent什么放child？ | §3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | parent 和继承有什么区别？父子容器中的 Bean 能覆盖 parent 的同名 Bean 吗？ | §2 |

## 覆盖: 5 问 / 3 身份 / 100%
