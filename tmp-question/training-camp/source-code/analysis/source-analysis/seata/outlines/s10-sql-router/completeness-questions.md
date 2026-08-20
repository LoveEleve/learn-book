# S-10 SQL 路由 — completeness-questions (全视角提问验证)

## 开发者视角

1. 路由入口? (ExecuteTemplate.execute)
2. 守卫? (非 AT 直通)
3. 路由 6 类型? (INSERT/UPDATE/DELETE/SELECT_FOR_UPDATE/ON_DUP/UPDATE_JOIN)
4. SQL 识别? (SQLVisitorFactory SPI)
5. SQLType 多少? (48)
6. executor 族? (基类 + 10 方言)
7. Multi? (批量语句)
8. 兜底? (Plain)

## 架构师视角

9. 为什么守卫直通? (非 AT 零开销)
10. 为什么 Insert SPI? (方言差异大 — 可插拔)
11. 为什么 2 类型仅 3 方言? (语法差异 — 显式不支持优于错执行)
12. 为什么 Multi? (批量语句镜像)
13. 为什么双解析器? (druid 成熟 vs antlr 自研 — 可切换)
14. 为什么 Plain 兜底? (未识别不拦截 — 安全优先)
15. 对照 MyBatis? (插件链 vs 路由表)
16. 为什么 SQLType 48? (协议完备性)

## 学生视角

17. 什么是路由? (按类型选处理者)
18. 什么是 executor? (SQL 执行器)
19. 什么是 SQLType? (SQL 类型枚举)
20. 什么是 Plain? (原样执行)
