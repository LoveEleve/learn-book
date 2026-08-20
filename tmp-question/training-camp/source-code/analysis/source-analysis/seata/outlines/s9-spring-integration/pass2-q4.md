# 闭环笔记 q4: 自动配置 — SeataAutoConfiguration

## 假设
Spring Boot 自动装配扫描器/处理器; 数据源代理并行装配。

## 验证过程
- **SeataAutoConfiguration** (seata-spring-boot-starter): **failureHandler()** bean (L52) + **globalTransactionScanner()** bean (L59) — 自动注册
- **扫描器装配**: applicationId/txServiceGroup 从配置读取 (spring.application.name / seata.tx-service-group)
- **disableGlobalTransaction**: 配置关闭 → 不 initClient (仅监听配置变更) (GlobalTransactionScanner:526-533)
- **initClient()** (L535): TM/RM 客户端初始化 (事务管理器/资源管理器)
- **数据源代理面** (S-4 交叉): DataSourceAutoProxyCreator 并行自动代理数据源
- **配置变更监听**: CachedConfigurationChangeListener — disable 开关动态生效

## 代码类型
Architecture (自动配置)

## 跨域关联
- S-4: 数据源自动代理 (并行面)
- S-1: initClient → TM 客户端
- S-8: 客户端注册面

## 结论
自动配置 = failureHandler + scanner 两 bean; disable 开关动态; initClient 装配 TM/RM。
源码位置: SeataAutoConfiguration.java:47-59; GlobalTransactionScanner.java:526-539
