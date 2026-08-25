# 07-01：07-microsphere-sentinel 源码文件清单

> **核心命题**：07-microsphere-sentinel 模块的全部源码文件列表，共 60 个 Java 文件。

---

## 项目结构

### microsphere-alibaba-sentinel-commons（21 文件）

- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/AbstractSentinelPlugin.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/SentinelContext.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/SentinelOperations.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/SentinelPlugin.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/SentinelPluginRepository.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/SentinelTemplate.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/SimpleSentinelPlugin.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/SimpleSentinelPluginRepository.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/constants/SentinelConstants.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/common/util/SentinelUtils.java`
- `microsphere-alibaba-sentinel-commons/src/main/java/io/microsphere/alibaba/sentinel/jmx/JMXSentinelPluginRepository.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/AbstractSentinelPluginTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/SentinelContextTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/SentinelPluginRepositoryTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/SentinelPluginTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/SentinelTemplateTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/SimpleSentinelPluginRepositoryTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/SimpleSentinelPluginTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/constants/SentinelConstantsTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/common/util/SentinelUtilsTest.java`
- `microsphere-alibaba-sentinel-commons/src/test/java/io/microsphere/alibaba/sentinel/jmx/JMXSentinelPluginRepositoryTest.java`

### microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-alibaba-druid（4 文件）

- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-alibaba-druid/src/main/java/io/microsphere/alibaba/sentinel/alibaba/druid/SentinelAlibabaDruidConstants.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-alibaba-druid/src/main/java/io/microsphere/alibaba/sentinel/alibaba/druid/SentinelAlibabaDruidFilter.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-alibaba-druid/src/test/java/io/microsphere/alibaba/sentinel/alibaba/druid/SentinelAlibabaDruidConstantsTest.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-alibaba-druid/src/test/java/io/microsphere/alibaba/sentinel/alibaba/druid/SentinelAlibabaDruidFilterTest.java`

### microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-hibernate-core（5 文件）

- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-hibernate-core/src/main/java/io/microsphere/alibaba/sentinel/hibernate/SentinelHibernateConstants.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-hibernate-core/src/main/java/io/microsphere/alibaba/sentinel/hibernate/entity/SentinelHibernateEntityCallback.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-hibernate-core/src/test/java/io/microsphere/alibaba/sentinel/hibernate/SentinelHibernateConstantsTest.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-hibernate-core/src/test/java/io/microsphere/alibaba/sentinel/hibernate/entity/SentinelHibernateEntityCallbackDisabledTest.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-hibernate-core/src/test/java/io/microsphere/alibaba/sentinel/hibernate/entity/SentinelHibernateEntityCallbackTest.java`

### microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-mybatis（4 文件）

- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-mybatis/src/main/java/io/microsphere/alibaba/sentinel/mybatis/SentinelMyBatisConstants.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-mybatis/src/main/java/io/microsphere/alibaba/sentinel/mybatis/executor/SentinelMyBatisExecutorFilter.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-mybatis/src/test/java/io/microsphere/alibaba/sentinel/mybatis/SentinelMyBatisConstantsTest.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-mybatis/src/test/java/io/microsphere/alibaba/sentinel/mybatis/executor/SentinelMyBatisExecutorFilterTest.java`

### microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-p6spy（4 文件）

- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-p6spy/src/main/java/io/microsphere/alibaba/sentinel/p6spy/SentinelJdbcEventListener.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-p6spy/src/main/java/io/microsphere/alibaba/sentinel/p6spy/SentinelP6SpyConstants.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-p6spy/src/test/java/io/microsphere/alibaba/sentinel/p6spy/SentinelJdbcEventListenerTest.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-p6spy/src/test/java/io/microsphere/alibaba/sentinel/p6spy/SentinelP6SpyConstantsTest.java`

### microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-redis（4 文件）

- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-redis/src/main/java/io/microsphere/alibaba/sentinel/redis/SentinelRedisConstants.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-redis/src/main/java/io/microsphere/alibaba/sentinel/redis/spring/SentinelRedisCommandInterceptor.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-redis/src/test/java/io/microsphere/alibaba/sentinel/redis/SentinelRedisConstantsTest.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-redis/src/test/java/io/microsphere/alibaba/sentinel/redis/spring/SentinelRedisCommandInterceptorTest.java`

### microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-spring-web（4 文件）

- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-spring-web/src/main/java/io/microsphere/alibaba/sentinel/spring/web/SentinelHandlerMethodInterceptor.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-spring-web/src/main/java/io/microsphere/alibaba/sentinel/spring/web/SentinelSpringWebConstants.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-spring-web/src/test/java/io/microsphere/alibaba/sentinel/spring/web/SentinelHandlerMethodInterceptorTest.java`
- `microsphere-alibaba-sentinel-plugins/microsphere-alibaba-sentinel-spring-web/src/test/java/io/microsphere/alibaba/sentinel/spring/web/SentinelSpringWebConstantsTest.java`

### microsphere-alibaba-sentinel-spring-boot（12 文件）

- `microsphere-alibaba-sentinel-spring-boot/src/main/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelAlibabaDruidAutoConfiguration.java`
- `microsphere-alibaba-sentinel-spring-boot/src/main/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelMyBatisAutoConfiguration.java`
- `microsphere-alibaba-sentinel-spring-boot/src/main/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelRedisAutoConfiguration.java`
- `microsphere-alibaba-sentinel-spring-boot/src/main/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelSpringWebAutoConfiguration.java`
- `microsphere-alibaba-sentinel-spring-boot/src/main/java/io/microsphere/alibaba/sentinel/spring/boot/condition/ConditionalOnSentinelAvailable.java`
- `microsphere-alibaba-sentinel-spring-boot/src/main/java/io/microsphere/alibaba/sentinel/spring/boot/condition/ConditionalOnSentinelEnabled.java`
- `microsphere-alibaba-sentinel-spring-boot/src/test/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/AutoConfigurationTest.java`
- `microsphere-alibaba-sentinel-spring-boot/src/test/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelAlibabaDruidAutoConfigurationTest.java`
- `microsphere-alibaba-sentinel-spring-boot/src/test/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelAutoConfigurationTest.java`
- `microsphere-alibaba-sentinel-spring-boot/src/test/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelMyBatisAutoConfigurationTest.java`
- `microsphere-alibaba-sentinel-spring-boot/src/test/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelRedisAutoConfigurationTest.java`
- `microsphere-alibaba-sentinel-spring-boot/src/test/java/io/microsphere/alibaba/sentinel/spring/boot/autoconfigure/SentinelSpringWebAutoConfigurationTest.java`

### microsphere-alibaba-sentinel-spring-cloud（2 文件）

- `microsphere-alibaba-sentinel-spring-cloud/src/main/java/io/microsphere/alibaba/sentinel/spring/cloud/SentinelCloudAutoConfiguration.java`
- `microsphere-alibaba-sentinel-spring-cloud/src/test/java/io/microsphere/alibaba/sentinel/spring/cloud/SentinelCloudAutoConfigurationTest.java`

---

**总计**：60 个 Java 文件
