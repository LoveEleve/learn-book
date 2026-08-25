# 11-01：11-microsphere-resilience4j 源码文件清单

> **核心命题**：11-microsphere-resilience4j 模块的全部源码文件列表，共 99 个 Java 文件。

---

## 项目结构

### microsphere-resilience4j-commons（30 文件）

- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/bulkhead/BulkheadTemplate.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/bulkhead/ThreadPoolBulkheadTemplate.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/circuitbreaker/CircuitBreakerTemplate.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/AdvancedResilience4jOperations.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/CallbackChain.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/ChainableResilience4jFacade.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/Resilience4jConstants.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/Resilience4jContext.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/Resilience4jFacade.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/Resilience4jModule.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/Resilience4jOperations.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/common/Resilience4jTemplate.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/ratelimiter/RateLimiterTemplate.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/retry/RetryTemplate.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/timelimiter/TimeLimiterTemplate.java`
- `microsphere-resilience4j-commons/src/main/java/io/microsphere/resilience4j/util/Resilience4jUtils.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/bulkhead/BulkheadTemplateTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/bulkhead/ThreadPoolBulkheadTemplateTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/circuitbreaker/CircuitBreakerTemplateTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/common/AbstractResilience4jTemplateTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/common/CallbackChainTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/common/ChainableResilience4jFacadeTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/common/Resilience4jConstantsTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/common/Resilience4jContextTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/common/Resilience4jModuleTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/ratelimiter/RateLimiterTemplateTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/retry/RetryTemplateTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/test/Resilience4jTestUtils.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/timelimiter/TimeLimiterTemplateTest.java`
- `microsphere-resilience4j-commons/src/test/java/io/microsphere/resilience4j/util/Resilience4jUtilsTest.java`

### microsphere-resilience4j-plugins/microsphere-resilience4j-alibaba-druid（3 文件）

- `microsphere-resilience4j-plugins/microsphere-resilience4j-alibaba-druid/src/main/java/io/microsphere/resilience4j/alibaba/druid/DruidResilience4jPlugin.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-alibaba-druid/src/main/java/io/microsphere/resilience4j/alibaba/druid/filter/Resilience4jDruidFilter.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-alibaba-druid/src/test/java/io/microsphere/resilience4j/alibaba/druid/filter/Resilience4jDruidFilterTest.java`

### microsphere-resilience4j-plugins/microsphere-resilience4j-hibernate-core（2 文件）

- `microsphere-resilience4j-plugins/microsphere-resilience4j-hibernate-core/src/main/java/io/microsphere/resilience4j/hibernate/Resilience4jHibernateEntityCallback.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-hibernate-core/src/test/java/io/microsphere/resilience4j/hibernate/Resilience4jHibernateEntityCallbackTest.java`

### microsphere-resilience4j-plugins/microsphere-resilience4j-mybatis（5 文件）

- `microsphere-resilience4j-plugins/microsphere-resilience4j-mybatis/src/main/java/io/microsphere/resilience4j/mybatis/MyBatisResilience4jPlugin.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-mybatis/src/main/java/io/microsphere/resilience4j/mybatis/executor/Resilience4jExecutor.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-mybatis/src/main/java/io/microsphere/resilience4j/mybatis/plugin/Resilience4jExecutorInterceptor.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-mybatis/src/test/java/io/microsphere/resilience4j/mybatis/MyBatisResilience4jPluginTest.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-mybatis/src/test/java/io/microsphere/resilience4j/mybatis/plugin/Resilience4jExecutorInterceptorTest.java`

### microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign（15 文件）

- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/main/java/io/microsphere/resilience4j/feign/Resilience4jCapability.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/main/java/io/microsphere/resilience4j/feign/Resilience4jClient.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/main/java/io/microsphere/resilience4j/feign/Resilience4jInvocationHandler.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/main/java/io/microsphere/resilience4j/feign/Resilience4jInvocationHandlerFactory.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/AbstractResilience4jFeignTest.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/DelegatingCapability.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/DelegatingClient.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/DelegatingInvocationHandler.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/DelegatingInvocationHandlerFactory.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/Resilience4jCapabilityTest.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/Resilience4jClientTest.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/Resilience4jInvocationHandlerFactoryTest.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/api/User.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/api/UserService.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-openfeign/src/test/java/io/microsphere/resilience4j/feign/server/SimpleUserService.java`

### microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web（3 文件）

- `microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web/src/main/java/io/microsphere/resilience4j/spring/web/Resilience4jHandlerMethodInterceptor.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web/src/main/java/io/microsphere/resilience4j/spring/web/SpringWebResilience4jPlugin.java`
- `microsphere-resilience4j-plugins/microsphere-resilience4j-spring-web/src/test/java/io/microsphere/resilience4j/spring/web/SpringWebResilience4jPluginTest.java`

### microsphere-resilience4j-spring（41 文件）

- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/LazyResilience4jFacade.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/bulkhead/annotation/EnableBulkhead.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/bulkhead/annotation/EnableBulkheadRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/bulkhead/annotation/EnableThreadPoolBulkhead.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/bulkhead/annotation/EnableThreadPoolBulkheadRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/bulkhead/event/BulkheadApplicationEventPublisher.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/bulkhead/event/BulkheadEventConsumerBeanRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/circuitbreaker/annotation/EnableCircuitBreaker.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/circuitbreaker/annotation/EnableCircuitBreakerRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/circuitbreaker/event/CircuitBreakerApplicationEventPublisher.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/circuitbreaker/event/CircuitBreakerEventConsumerBeanRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/common/Resilience4jPlugin.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/common/annotation/EnableResilience4j.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/common/annotation/EnableResilience4jRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/common/event/EventConsumerMethodFilter.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/common/event/Resilience4jEventApplicationEventPublisher.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/common/event/Resilience4jEventConsumerBeanRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/ratelimiter/annotation/EnableRateLimiter.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/ratelimiter/annotation/EnableRateLimiterRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/ratelimiter/event/RateLimiterApplicationEventPublisher.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/ratelimiter/event/RateLimiterEventConsumerBeanRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/retry/annotation/EnableRetry.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/retry/annotation/EnableRetryRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/retry/event/RetryApplicationEventPublisher.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/retry/event/RetryEventConsumerBeanRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/timelimiter/annotation/EnableTimeLimiter.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/timelimiter/annotation/EnableTimeLimiterRegistrar.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/timelimiter/event/TimeLimiterApplicationEventPublisher.java`
- `microsphere-resilience4j-spring/src/main/java/io/microsphere/resilience4j/spring/timelimiter/event/TimeLimiterEventConsumerBeanRegistrar.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/LazyResilience4jFacadeTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/LoggingResilience4jPlugin.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/NoOpResilience4jPlugin.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/bulkhead/annotation/EnableBulkheadTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/bulkhead/annotation/EnableThreadPoolBulkheadTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/circuitbreaker/annotation/EnableCircuitBreakerTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/circuitbreaker/event/CircuitBreakerApplicationEventPublisherTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/circuitbreaker/event/CircuitBreakerEventConsumerBeanRegistrarTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/common/event/EventConsumerMethodFilterTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/ratelimiter/annotation/EnableRateLimiterTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/retry/annotation/EnableRetryTest.java`
- `microsphere-resilience4j-spring/src/test/java/io/microsphere/resilience4j/spring/timelimiter/annotation/EnableTimeLimiterTest.java`

---

**总计**：99 个 Java 文件
