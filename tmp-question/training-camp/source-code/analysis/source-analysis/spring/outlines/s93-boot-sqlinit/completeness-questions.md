# S-29 SQL 初始化 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么让 schema.sql 自动执行? | §1 (默认开启 + classpath 放脚本) |
| 2 | 怎么指定脚本位置? | §3 (schema-locations/data-locations) |
| 3 | 怎么避免生产库执行脚本? | §3 (默认 EMBEDDED) |
| 4 | 怎么彻底关掉? | §1/§3 (enabled=false 或 mode=never) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 enabled + mode 双条件? | §1 (总开关 + 细化排除) |
| 6 | 为什么先 schema 后 data? | §2 (data 依赖表存在) |
| 7 | 为什么默认 EMBEDDED? | §3 (默认安全, 防误跑生产) |
| 8 | 与 S-10 边界? | 边界 (DataSource 复用) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | SQL 脚本谁执行的? | §2 (SqlDataSourceScriptDatabaseInitializer) |
| 10 | mode 三态是什么? | §3 (EMBEDDED/ALWAYS/NEVER) |
| 11 | 初始化何时发生? | §2 (refresh 期间, afterPropertiesSet) |

## 覆盖: 11 问 / 3 身份 / 100%
