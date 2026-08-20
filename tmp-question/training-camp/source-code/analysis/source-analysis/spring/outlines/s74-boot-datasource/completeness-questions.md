# S-10 DataSource 自动装配全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 没配 url 也有 DataSource? | §2 (嵌入式分支) |
| 2 | Hikari 为什么是默认池? | §3 (matchIfMissing) |
| 3 | 怎么换别的连接池? | §3 (换依赖+spring.datasource.type) |
| 4 | 自定义 DataSource 怎么覆盖? | §3 (@ConditionalOnMissingBean) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么双分支设计? | §1 (零配置体验) |
| 6 | 嵌入式判定为什么双重条件? | §2 (避免误配) |
| 7 | 与 C-11/阶段3 的边界? | §3 (池机制复用, 只讲装配) |
| 8 | JdbcConnectionDetails 解决什么? | §3 (连接细节封装) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 两个分支的共同保护条件? | §1 (@ConditionalOnMissingBean) |
| 10 | matchIfMissing=true 的意义? | §3 (未指定即默认) |

## 覆盖: 10 问 / 3 身份 / 100%
