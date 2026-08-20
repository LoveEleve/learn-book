# SCC-13 NamedContextFactory — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. contexts/configurations 双 Map 的分工? 为什么分离?
2. getContext 双检锁 vs ConcurrentHashMap.computeIfAbsent 的取舍?
3. "default." 前缀配置为什么注入所有子上下文? 设计动机?
4. buildContext 的 setParent 带来什么可见性? 与含祖先查找的关系?
5. AOT 分支为什么用 GenericApplicationContext 而非 AnnotationConfig?

## B. 源码实证 (5)

6. registerBeans 的三段注册分别是什么? (grep L143-159)
7. buildContext 的 propertySourceName 注入什么值? (grep L185-187)
8. getAnnotatedInstance 多 bean 时抛什么? (grep L248)
9. ClientFactoryObjectProvider 的"延迟解析"怎么实现? (grep L35-43)
10. testBadThreadContextClassLoader 用什么类加载器? (grep Tests:63-77)

## C. 推理深挖 (5)

11. 为什么 getContext 用 synchronized(this.contexts) 而非锁单个 name?
12. destroy 为什么"关闭失败只 WARN 不抛"?
13. 子上下文 refresh 失败会怎样? (配置类非法时)
14. default. 前缀与精确匹配冲突时谁赢? (registerBeans 顺序)
15. 为什么 Spec 配置在构建期固定 (负面空间)? 热更新会破坏什么?

## D. 跨域扩展 (5)

16. NamedContextFactory vs Spring 父子容器 (HierarchicalBeanFactory) 的隔离差异?
17. FeignClientFactory (阶段 5.6) 怎么用 Specification 隔离每个客户端配置?
18. LoadBalancerClientFactory (SCC-6) 怎么为每服务配不同策略?
19. AOT 分支 vs 传统反射注册的差异? 对 GraalVM 的意义?
20. 如果给 NamedContextFactory 加配置热更新, 应该改哪层? (对照 SCC-2 RefreshScope)
