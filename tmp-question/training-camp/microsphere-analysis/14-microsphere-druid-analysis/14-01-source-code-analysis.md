# 14-01：14-microsphere-druid 源码文件清单

> **核心命题**：14-microsphere-druid 模块的全部源码文件列表，共 38 个 Java 文件。

---

## 项目结构

### microsphere-alibaba-druid-core（7 文件）

- `microsphere-alibaba-druid-core/src/main/java/io/microsphere/alibaba/druid/constants/PropertyConstants.java`
- `microsphere-alibaba-druid-core/src/main/java/io/microsphere/alibaba/druid/filter/AbstractStatementFilter.java`
- `microsphere-alibaba-druid-core/src/main/java/io/microsphere/alibaba/druid/filter/LoggingStatementFilter.java`
- `microsphere-alibaba-druid-core/src/test/java/io/microsphere/alibaba/druid/constants/PropertyConstantsTest.java`
- `microsphere-alibaba-druid-core/src/test/java/io/microsphere/alibaba/druid/filter/AbstractFilterTest.java`
- `microsphere-alibaba-druid-core/src/test/java/io/microsphere/alibaba/druid/filter/AbstractStatementFilterTest.java`
- `microsphere-alibaba-druid-core/src/test/java/io/microsphere/alibaba/druid/filter/LoggingStatementFilterTest.java`

### microsphere-alibaba-druid-spring（8 文件）

- `microsphere-alibaba-druid-spring/src/main/java/io/microsphere/alibaba/druid/spring/beans/factory/config/DruidDataSourceBeanPostProcessor.java`
- `microsphere-alibaba-druid-spring/src/main/java/io/microsphere/alibaba/druid/spring/context/annotation/AlibabaDruidRegistrar.java`
- `microsphere-alibaba-druid-spring/src/main/java/io/microsphere/alibaba/druid/spring/context/annotation/EnableAlibabaDruid.java`
- `microsphere-alibaba-druid-spring/src/test/java/io/microsphere/alibaba/druid/filter/AutoLoadFilter.java`
- `microsphere-alibaba-druid-spring/src/test/java/io/microsphere/alibaba/druid/filter/LoadFilter.java`
- `microsphere-alibaba-druid-spring/src/test/java/io/microsphere/alibaba/druid/filter/TestStatementFilter.java`
- `microsphere-alibaba-druid-spring/src/test/java/io/microsphere/alibaba/druid/spring/beans/factory/config/DruidDataSourceBeanPostProcessorTest.java`
- `microsphere-alibaba-druid-spring/src/test/java/io/microsphere/alibaba/druid/spring/context/annotation/EnableAlibabaDruidTest.java`

### microsphere-alibaba-druid-spring-boot（9 文件）

- `microsphere-alibaba-druid-spring-boot/src/main/java/io/microsphere/alibaba/druid/spring/boot/AlibabaDruidProperties.java`
- `microsphere-alibaba-druid-spring-boot/src/main/java/io/microsphere/alibaba/druid/spring/boot/autoconfigure/AlibabaDruidAutoConfiguration.java`
- `microsphere-alibaba-druid-spring-boot/src/main/java/io/microsphere/alibaba/druid/spring/boot/condition/ConditionalOnAlibabaDruidAvailable.java`
- `microsphere-alibaba-druid-spring-boot/src/main/java/io/microsphere/alibaba/druid/spring/boot/condition/ConditionalOnAlibabaDruidEnabled.java`
- `microsphere-alibaba-druid-spring-boot/src/main/java/io/microsphere/alibaba/druid/spring/boot/metadata/DruidDataSourcePoolMetadata.java`
- `microsphere-alibaba-druid-spring-boot/src/test/java/io/microsphere/alibaba/druid/spring/boot/AlibabaDruidPropertiesTest.java`
- `microsphere-alibaba-druid-spring-boot/src/test/java/io/microsphere/alibaba/druid/spring/boot/autoconfigure/AlibabaDruidAutoConfigurationIntegrationTest.java`
- `microsphere-alibaba-druid-spring-boot/src/test/java/io/microsphere/alibaba/druid/spring/boot/autoconfigure/AlibabaDruidAutoConfigurationTest.java`
- `microsphere-alibaba-druid-spring-boot/src/test/java/io/microsphere/alibaba/druid/spring/boot/metadata/DruidDataSourcePoolMetadataTest.java`

### microsphere-alibaba-druid-spring-cloud（3 文件）

- `microsphere-alibaba-druid-spring-cloud/src/main/java/io/microsphere/alibaba/druid/spring/cloud/autoconfigure/AlibabaDruidCloudAutoConfiguration.java`
- `microsphere-alibaba-druid-spring-cloud/src/test/java/io/microsphere/alibaba/druid/spring/cloud/autoconfigure/AlibabaDruidCloudAutoConfigurationIntegrationTest.java`
- `microsphere-alibaba-druid-spring-cloud/src/test/java/io/microsphere/alibaba/druid/spring/cloud/autoconfigure/AlibabaDruidCloudAutoConfigurationTest.java`

### microsphere-alibaba-druid-spring-test（3 文件）

- `microsphere-alibaba-druid-spring-test/src/main/java/io/microsphere/alibaba/druid/test/spring/AbstractDruidSpringTest.java`
- `microsphere-alibaba-druid-spring-test/src/main/java/io/microsphere/alibaba/druid/test/spring/DruidDataSourceTestConfiguration.java`
- `microsphere-alibaba-druid-spring-test/src/test/java/io/microsphere/alibaba/druid/test/spring/DefaultDruidSpringTest.java`

### microsphere-alibaba-druid-test（8 文件）

- `microsphere-alibaba-druid-test/src/main/java/io/microsphere/alibaba/druid/test/AbstractAlibabaDruidTest.java`
- `microsphere-alibaba-druid-test/src/main/java/io/microsphere/alibaba/druid/test/AlibabaDruidTestUtils.java`
- `microsphere-alibaba-druid-test/src/main/java/io/microsphere/alibaba/druid/test/junit/jupiter/AlibabaDruidRuntime.java`
- `microsphere-alibaba-druid-test/src/main/java/io/microsphere/alibaba/druid/test/junit/jupiter/AlibabaDruidTest.java`
- `microsphere-alibaba-druid-test/src/main/java/io/microsphere/alibaba/druid/test/junit/jupiter/AlibabaDruidTestExtension.java`
- `microsphere-alibaba-druid-test/src/test/java/io/microsphere/alibaba/druid/test/AlibabaDruidTestUtilsTest.java`
- `microsphere-alibaba-druid-test/src/test/java/io/microsphere/alibaba/druid/test/DefaultAlibabaDruidTest.java`
- `microsphere-alibaba-druid-test/src/test/java/io/microsphere/alibaba/druid/test/junit/jupiter/AlibabaDruidTestExtensionTest.java`

---

**总计**：38 个 Java 文件
