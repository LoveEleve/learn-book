# SCC-1 Bootstrap 上下文 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. BootstrapApplicationListener 为什么监听 ApplicationEnvironmentPreparedEvent 而非启动后事件?
2. 三守卫各防什么? 为什么需要"已含 bootstrap 源 → return"?
3. bootstrap 上下文为什么是主应用的 parent? 属性可见性怎么实现?
4. PropertySourceLocator 的 locateCollection 为什么展开 Composite? 返回 null 表示什么?
5. insertPropertySources 的三种插入策略分别对应什么配置场景?

## B. 源码实证 (5)

6. DEFAULT_ORDER 的具体值? 为什么 HIGHEST_PRECEDENCE+5? (grep BootstrapApplicationListener:87)
7. MARKER_CLASS 是什么? 什么时候自动启用 bootstrap? (grep PropertyUtils:37-38)
8. findBootstrapContext 为什么用反射取 parent? (grep L121-129)
9. bootstrapServiceContext 注入了哪些 spring.config.* 属性? (grep L151-162)
10. filterListeners 过滤了什么? 为什么? (grep L214-221)

## C. 推理深挖 (5)

11. 如果 bootstrap 上下文重复构建会怎样? 守卫 2 怎么防?
12. overrideSystemProperties=true 且 allowOverride=false 时, 插入位置在哪? 语义?
13. CompositePropertySource 展开后顺序怎么保证? 与 addFirst 反转的关系?
14. 为什么 mergeDefaultProperties 只补缺失键? (L223+)
15. 新 spring.config.import 机制下, 4.3.2 为什么还保留完整 bootstrap?

## D. 跨域扩展 (5)

16. Bootstrap 的 PropertySourceLocator vs Spring Boot ConfigData 的 ConfigDataLocationResolver 异同?
17. Nacos 的 NacosPropertySourceLocator 怎么实现 locate? (对照 Nacos 仓库 5.8)
18. bootstrap 高优先级 vs @RefreshScope 刷新后的属性来源优先级? (对照 SCC-2)
19. bootstrap 父子上下文 vs Feign NamedContextFactory 子上下文 (对照 SCC-13) 的设计差异?
20. 如果给新项目选配置加载方式, bootstrap vs spring.config.import, 怎么选?
