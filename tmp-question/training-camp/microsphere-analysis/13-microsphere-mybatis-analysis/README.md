# 13 - microsphere-mybatis 深度分析

## 源码信息

- **本地路径**: `/data/workspace/java-training-camp/cloud-native-code/share/microsphere-mybatis`
- **GitHub**: https://github.com/microsphere-projects/microsphere-mybatis
- **规模**: 54 个主 Java 文件 + 60 个测试，9 个 Maven 模块（core/spring/spring-boot/spring-cloud/test/spring-test/utils/parent/dependencies）
- **依赖**: mybatis 3.5.19、mybatis-spring 4.0.0、mybatis-spring-boot 3.0.5
- **git**: 450 提交，2025-02-15 创建，2026-07 活跃

## 文章索引

| 编号 | 文章 | 内容 |
|------|------|------|
| 13-01 | [核心机制——三层 Executor 拦截体系](13-01-core-interception-mechanism.md) | 官方 Plugin 基线（mybatis-3.5.16 源码）；InterceptingExecutor/ExecutorFilterChain/ExecutorFilter/ExecutorInterceptor/Adapter 三层解剖；plugin() 挂载三步（剥/合并/重包）；演进史（含初始 NPE bug）；P1-P6 问题 |
| 13-02 | [Spring 集成与测试基建](13-02-spring-integration-and-test.md) | 三套注解分工；Registrar 体系（ifAbsent/三源扫描）；SqlSessionFactoryBeanPostProcessor；自动配置与条件注解；features.yaml 官方 SPI 全景图；测试基建（JUnit5 扩展 + 10 resolver）；S1-S5 问题 |
| 13-03 | [与官方/18/07 的对照与生态定位](13-03-ecosystem-comparison.md) | 官方 Plugin vs microsphere（手术刀 vs 流水线，可共存、嵌套顺序）；Sentinel × MyBatis 集成闭环（资源名 = ms.getId()）；与 18（子上下文天然隔离）；与 14 的层级对照（Executor 层 vs JDBC 层，**14-02 展开的预埋**）；生产评估 |

## 核心结论（引用时直接用）

- **三层拦截体系**：`InterceptingExecutor`（实现官方 Executor 接口）→ 每次调用新建 `ExecutorFilterChain`（数组+游标）→ `ExecutorFilter`（责任链，可改参/短路，10 类操作）vs `ExecutorInterceptor`（观察者，18 个回调，异常隔离）
- **挂载**：`InterceptingExecutorInterceptor`（官方 Interceptor）的 `plugin()` 三步——剥 CachingExecutor.delegate（反射）→ 防嵌套合并 → 重包 CachingExecutor（缓存语义在外层，**二级缓存命中时过滤器不执行**）
- **与官方**：绕开动态代理（intercept() 只 warn），操作级分类 vs 方法级 @Signature；两者可共存，**嵌套顺序由 addInterceptor 顺序决定**（先 add 的在外层先执行）
- **Sentinel 集成**：`SentinelMyBatisExecutorFilter`（实现 ExecutorFilter + 注册 bean 即入管道）——"生态统一接入点"实证，资源名 = Mapper 方法全限定名
- **问题**：反射读私有字段 ×2（升级脆弱）、拼写错误 `BeanDefintionRegistrar`、初始提交 NPE bug（git 取证）、spring-cloud 空壳
