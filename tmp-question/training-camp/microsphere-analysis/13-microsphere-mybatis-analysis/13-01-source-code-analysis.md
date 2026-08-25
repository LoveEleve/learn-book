# 13-01：13-microsphere-mybatis 源码文件清单

> **核心命题**：13-microsphere-mybatis 模块的全部源码文件列表，共 85 个 Java 文件。

---

## 项目结构

### microsphere-mybatis-core（27 文件）

- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/constants/PropertyConstants.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/executor/ExecutorFilter.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/executor/ExecutorFilterChain.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/executor/ExecutorInterceptor.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/executor/Executors.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/executor/InterceptingExecutor.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/executor/InterceptorsExecutorFilterAdapter.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/executor/LoggingExecutorFilter.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/executor/LoggingExecutorInterceptor.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/plugin/InterceptingExecutorInterceptor.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/plugin/InterceptorContext.java`
- `microsphere-mybatis-core/src/main/java/io/microsphere/mybatis/plugin/Plugins.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/constants/PropertyConstantsTest.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/executor/ExecutorsTest.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/executor/InterceptingExecutorTest.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/executor/InterceptorsExecutorFilterAdapterTest.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/executor/LoggingExecutor.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/executor/NoOpExecutorInterceptor.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/executor/TestExecutorFilter.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/executor/ThrowingErrorExecutorFilter.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/executor/ThrowingErrorExecutorInterceptor.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/plugin/InterceptingExecutorInterceptorTest.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/plugin/InterceptorContextTest.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/plugin/NoOpInterceptor.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/plugin/PluginsTest.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/plugin/TestAnnotatedExecutorInterceptor.java`
- `microsphere-mybatis-core/src/test/java/io/microsphere/mybatis/plugin/TestInterceptorContextExecutorInterceptor.java`

### microsphere-mybatis-spring（11 文件）

- `microsphere-mybatis-spring/src/main/java/io/microsphere/mybatis/spring/annotation/EnableMyBatis.java`
- `microsphere-mybatis-spring/src/main/java/io/microsphere/mybatis/spring/annotation/EnableMyBatisExtension.java`
- `microsphere-mybatis-spring/src/main/java/io/microsphere/mybatis/spring/annotation/MyBatisBeanDefinitionRegistrar.java`
- `microsphere-mybatis-spring/src/main/java/io/microsphere/mybatis/spring/annotation/MyBatisConfiguration.java`
- `microsphere-mybatis-spring/src/main/java/io/microsphere/mybatis/spring/annotation/MyBatisConfigurationBeanDefintionRegistrar.java`
- `microsphere-mybatis-spring/src/main/java/io/microsphere/mybatis/spring/annotation/MyBatisExtensionBeanDefinitionRegistrar.java`
- `microsphere-mybatis-spring/src/main/java/io/microsphere/mybatis/spring/annotation/MyBatisImportBeanDefinitionRegistrar.java`
- `microsphere-mybatis-spring/src/main/java/io/microsphere/mybatis/spring/annotation/SqlSessionFactoryBeanPostProcessor.java`
- `microsphere-mybatis-spring/src/test/java/io/microsphere/mybatis/spring/annotation/EnableMyBatisExtensionTest.java`
- `microsphere-mybatis-spring/src/test/java/io/microsphere/mybatis/spring/annotation/EnableMyBatisTest.java`
- `microsphere-mybatis-spring/src/test/java/io/microsphere/mybatis/spring/annotation/MyBatisConfigurationTest.java`

### microsphere-mybatis-spring-boot（6 文件）

- `microsphere-mybatis-spring-boot/src/main/java/io/microsphere/mybatis/spring/boot/autoconfigure/MyBatisAutoConfiguration.java`
- `microsphere-mybatis-spring-boot/src/main/java/io/microsphere/mybatis/spring/boot/autoconfigure/condition/ConditionalOnMyBatisAvailable.java`
- `microsphere-mybatis-spring-boot/src/main/java/io/microsphere/mybatis/spring/boot/autoconfigure/condition/ConditionalOnMyBatisEnabled.java`
- `microsphere-mybatis-spring-boot/src/test/java/io/microsphere/mybatis/spring/boot/autoconfigure/MyBatisAutoConfigurationIntegrationTest.java`
- `microsphere-mybatis-spring-boot/src/test/java/io/microsphere/mybatis/spring/boot/autoconfigure/MyBatisAutoConfigurationTest.java`
- `microsphere-mybatis-spring-boot/src/test/java/io/microsphere/mybatis/spring/boot/autoconfigure/condition/ConditionalOnMyBatisEnabledTest.java`

### microsphere-mybatis-spring-cloud（3 文件）

- `microsphere-mybatis-spring-cloud/src/main/java/io/microsphere/mybatis/spring/cloud/autoconfigure/MyBatisCloudAutoConfiguration.java`
- `microsphere-mybatis-spring-cloud/src/test/java/io/microsphere/mybatis/spring/cloud/autoconfigure/MyBatisCloudAutoConfigurationIntegrationTest.java`
- `microsphere-mybatis-spring-cloud/src/test/java/io/microsphere/mybatis/spring/cloud/autoconfigure/MyBatisCloudAutoConfigurationTest.java`

### microsphere-mybatis-spring-test（6 文件）

- `microsphere-mybatis-spring-test/src/main/java/io/microsphere/mybatis/spring/test/config/MyBatisDataBaseTestConfiguration.java`
- `microsphere-mybatis-spring-test/src/main/java/io/microsphere/mybatis/spring/test/config/MyBatisDataSourceTestConfiguration.java`
- `microsphere-mybatis-spring-test/src/main/java/io/microsphere/mybatis/spring/test/config/MyBatisTestConfiguration.java`
- `microsphere-mybatis-spring-test/src/test/java/io/microsphere/mybatis/spring/test/config/MyBatisDataBaseTestConfigurationTest.java`
- `microsphere-mybatis-spring-test/src/test/java/io/microsphere/mybatis/spring/test/config/MyBatisDataSourceTestConfigurationTest.java`
- `microsphere-mybatis-spring-test/src/test/java/io/microsphere/mybatis/spring/test/config/MyBatisTestConfigurationTest.java`

### microsphere-mybatis-test（30 文件）

- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/AbstractExecutorTest.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/AbstractMapperTest.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/AbstractMyBatisTest.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/AbstractSqlSessionTest.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/MyBatisTestUtils.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/entity/Child.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/entity/Father.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/entity/User.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/MyBatisRuntime.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/MyBatisTest.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/MyBatisTestExtension.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/AbstractComponentResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/ComponentResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/ConfigurationResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/ConnectionResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/DataSourceResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/EnvironmentResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/ExecutorResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/MapperComponentResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/PropertiesResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/SqlSessionFactoryResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/SqlSessionResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/junit/jupiter/resolver/TransactionResolver.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/mapper/ChildMapper.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/mapper/FatherMapper.java`
- `microsphere-mybatis-test/src/main/java/io/microsphere/mybatis/test/mapper/UserMapper.java`
- `microsphere-mybatis-test/src/test/java/io/microsphere/mybatis/test/AbstractMapperTestTest.java`
- `microsphere-mybatis-test/src/test/java/io/microsphere/mybatis/test/AbstractMyBatisTestTest.java`
- `microsphere-mybatis-test/src/test/java/io/microsphere/mybatis/test/MyBatisTestUtilsTest.java`
- `microsphere-mybatis-test/src/test/java/io/microsphere/mybatis/test/junit/jupiter/MyBatisTestExtensionTest.java`

### microsphere-mybatis-utils（2 文件）

- `microsphere-mybatis-utils/src/main/java/io/microsphere/mybatis/util/MyBatisUtils.java`
- `microsphere-mybatis-utils/src/test/java/io/microsphere/mybatis/util/MyBatisUtilsTest.java`

---

**总计**：85 个 Java 文件
