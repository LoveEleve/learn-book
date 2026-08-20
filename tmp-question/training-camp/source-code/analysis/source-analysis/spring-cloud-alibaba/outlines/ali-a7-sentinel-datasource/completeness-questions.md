# ALI-A7 Sentinel 数据源 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. 为什么用 SmartInitializingSingleton 而非 ApplicationReadyEvent? 时序差异?
2. "多数据源 active 即放弃" 的合理性? 为什么不做优先级?
3. dataType (json/xml/custom) 与 ruleType 两个维度怎么正交?
4. postRegister 的 register2Property 语义? 规则怎么热更新?
5. 动态注册的 Bean 名 `{name}-sentinel-{type}-datasource` 为什么含 type?

## B. 源码实证 (6)

6. getValidField 的反射逻辑? (grep L128-146)
7. 多源时 log 什么? (grep SentinelDataSourceHandler:83-88)
8. custom 分支缺 converterClass 会怎样? (grep L136-144)
9. 内置 converter 的 Bean 名格式? (grep L183-186)
10. postRegister 的 switch 有哪些 case? (grep AbstractDataSourceProperties:100-108)
11. RuleType 七枚举各自对应什么类? (grep RuleType:42-67)

## C. 推理深挖 (5)

12. 如果数据源 Bean 初始化失败 (Nacos 不可达), afterSingletonsInstantiated 的 catch 会怎样?
13. registerBean 里 getBean 立即初始化的意义? 不初始化会怎样?
14. custom converter 的 Bean 名防重复注册逻辑? (grep L149)
15. 数据源运行时变更规则 → RuleManager 热更新, 与 A2 的 refresh-behavior 有无关系?
16. 六字段里 file 和 nacos 都配了, validFields.size()==2 直接 return — 用户怎么知道?

## D. 跨域扩展 (4)

17. 数据源规则注册 vs A1 的 Nacos 配置加载: 两个"拉取+注入"链路异同?
18. register2Property vs SCC-9 的 Binder 钩子: 属性监听两种模式?
19. 六数据源 vs Nacos 5.8 的配置中心客户端: 规则数据源是 Nacos 客户端消费方?
20. 数据源动态 Bean 注册 vs A6 的拦截器动态注册: 两种动态装配对比?
