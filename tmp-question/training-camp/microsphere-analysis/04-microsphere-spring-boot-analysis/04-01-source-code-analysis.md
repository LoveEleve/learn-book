# 04-01：04-microsphere-spring-boot 源码文件清单

> **核心命题**：04-microsphere-spring-boot 模块的全部源码文件列表，共 144 个 Java 文件。

---

## 项目结构

### microsphere-spring-boot-actuator（15 文件）

- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/MonitoredThreadPoolTaskScheduler.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/autoconfigure/ActuatorAutoConfiguration.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/autoconfigure/ActuatorEndpointsAutoConfiguration.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/condition/ConditionalOnActuatorEndpointPresent.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/condition/ConditionalOnConfigurationProcessorPresent.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/constants/PropertyConstants.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/endpoint/ArtifactsEndpoint.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/endpoint/ConfigurationMetadataEndpoint.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/endpoint/ConfigurationPropertiesEndpoint.java`
- `microsphere-spring-boot-actuator/src/main/java/io/microsphere/spring/boot/actuate/endpoint/WebEndpoints.java`
- `microsphere-spring-boot-actuator/src/test/java/io/microsphere/spring/boot/actuate/autoconfigure/ActuatorAutoConfigurationTest.java`
- `microsphere-spring-boot-actuator/src/test/java/io/microsphere/spring/boot/actuate/autoconfigure/ActuatorEndpointsAutoConfigurationTest.java`
- `microsphere-spring-boot-actuator/src/test/java/io/microsphere/spring/boot/actuate/constants/PropertyConstantsTest.java`
- `microsphere-spring-boot-actuator/src/test/java/io/microsphere/spring/boot/actuate/endpoint/WebEndpointsTest.java`
- `microsphere-spring-boot-actuator/src/test/java/io/microsphere/spring/boot/actuate/env/DefaultPropertiesTest.java`

### microsphere-spring-boot-compatible（10 文件）

- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/BootstrapContext.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/BootstrapContextClosedEvent.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/BootstrapRegistry.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/ConfigurableBootstrapContext.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/DefaultBootstrapContext.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/autoconfigure/jackson/JacksonProperties.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/autoconfigure/web/ServerProperties.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/autoconfigure/web/servlet/MultipartProperties.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/package-info.java`
- `microsphere-spring-boot-compatible/src/main/java/org/springframework/boot/web/servlet/MultipartConfigFactory.java`

### microsphere-spring-boot-core（95 文件）

- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/SpringBootVersion.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/autoconfigure/ConfigurableAutoConfigurationImportFilter.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/classloading/BannedArtifactClassLoadingListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/condition/ConditionalOnPropertyPrefix.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/condition/OnPropertyPrefixCondition.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/constants/PropertyConstants.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/constants/SpringBootPropertyConstants.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/LoggingOnceApplicationPreparedEventListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/LoggingOnceMainApplicationPreparedEventListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/OnceApplicationPreparedEventListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/OnceMainApplicationPreparedEventListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/autoconfigure/ConfigurationPropertiesAutoConfiguration.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/config/BindableConfigurationBeanBinder.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/ConfigurationPropertiesBeanInfo.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/ListenableConfigurationPropertiesBindHandlerAdvisor.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/annotation/EnableConfigurationPropertiesExtension.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/annotation/EnableConfigurationPropertiesExtensionRegistrar.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/BindListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/BindListeners.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/ConfigurationPropertiesBeanContext.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/ConfigurationPropertiesBeanProperty.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/ConfigurationPropertiesBeanPropertyChangedEvent.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/EventPublishingConfigurationPropertiesBeanPropertyChangedListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/ListenableBindHandlerAdapter.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/util/BindHandlerUtils.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/bind/util/BindUtils.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/metadata/ConfigurationMetadataReader.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/source/util/ConfigurationPropertyUtils.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/context/properties/util/ConfigurationPropertiesUtils.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/diagnostics/ArtifactsCollisionDiagnosisListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/diagnostics/ArtifactsCollisionException.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/diagnostics/ArtifactsCollisionFailureAnalyzer.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/env/DefaultPropertiesApplicationListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/env/DefaultPropertiesPostProcessor.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/env/PropertySourceLoaders.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/env/SpringApplicationDefaultPropertiesPostProcessor.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/env/config/OriginTrackedConfigurationPropertyInitializer.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/env/config/metadata/ConfigurationMetadataRepository.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/listener/FailureReportSpringApplicationRunListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/listener/LoggingSpringApplicationRunListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/listener/SpringApplicationRunListenerAdapter.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/report/ConditionEvaluationReportBuilder.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/report/ConditionEvaluationReportInitializer.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/report/ConditionEvaluationReportListener.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/report/ConditionEvaluationSpringBootExceptionReporter.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/report/ConditionsReportMessageBuilder.java`
- `microsphere-spring-boot-core/src/main/java/io/microsphere/spring/boot/util/SpringApplicationUtils.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/SpringBootVersionTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/autoconfigure/ApplicationAutoConfigurationTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/autoconfigure/ConfigurableAutoConfigurationImportFilterTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/classloading/BannedArtifactClassLoadingListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/condition/OnPropertyPrefixConditionTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/constants/PropertyConstantsTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/constants/SpringBootPropertyConstantsTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/AbstractApplicationPreparedEventTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/LoggingOnceApplicationPreparedEventListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/LoggingOnceMainApplicationPreparedEventListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/autoconfigure/ConfigurationPropertiesAutoConfigurationTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/config/BindableConfigurationBeanBinderTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/ConfigurationPropertiesBeanInfoTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/ListenableConfigurationPropertiesBindHandlerAdvisorTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/TestConfigurationProperties.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/TestConfigurationPropertiesBindHandlerAdvisor.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/TestConstructorBindingConfigurationProperties.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/annotation/EnableConfigurationPropertiesExtensionRegistrarTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/bind/BindListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/bind/ConfigurationPropertiesBeanContextTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/bind/ConfigurationPropertiesBeanPropertyTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/bind/EventPublishingConfigurationPropertiesBeanPropertyChangedListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/bind/ListenableBindHandlerAdapterTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/bind/TestBindListener.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/bind/util/BindHandlerUtilsTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/bind/util/BindUtilsTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/metadata/ConfigurationMetadataReaderTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/source/util/ConfigurationPropertyUtilsTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/context/properties/util/ConfigurationPropertiesUtilsTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/diagnostics/ArtifactsCollisionDiagnosisListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/diagnostics/ArtifactsCollisionExceptionTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/diagnostics/ArtifactsCollisionFailureAnalyzerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/diagnostics/ArtifactsCollisionResourceResolver.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/domain/User.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/env/DefaultPropertiesApplicationListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/env/DefaultPropertiesPostProcessorTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/env/PropertiesEmptyPropertySourceLoader.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/env/PropertySourceLoadersTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/env/config/OriginTrackedConfigurationPropertyInitializerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/env/config/metadata/ConfigurationMetadataRepositoryTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/listener/LoggingSpringApplicationRunListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/report/ConditionEvaluationReportBuilderTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/report/ConditionEvaluationReportListenerTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/report/ConditionEvaluationSpringBootExceptionReporterTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/report/ConditionsReportMessageBuilderTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/util/SpringApplicationUtilsTest.java`
- `microsphere-spring-boot-core/src/test/java/io/microsphere/spring/boot/util/TestUtils.java`
- `microsphere-spring-boot-core/src/test/java/org/springframework/cloud/bootstrap/BootstrapApplicationListener.java`

### microsphere-spring-boot-test（9 文件）

- `microsphere-spring-boot-test/src/main/java/io/microsphere/spring/boot/test/AbstractAutoConfigurationTest.java`
- `microsphere-spring-boot-test/src/main/java/io/microsphere/spring/boot/test/AutoConfigurationTest.java`
- `microsphere-spring-boot-test/src/main/java/io/microsphere/spring/boot/test/ReactiveWebAutoConfigurationTest.java`
- `microsphere-spring-boot-test/src/main/java/io/microsphere/spring/boot/test/WebAutoConfigurationTest.java`
- `microsphere-spring-boot-test/src/test/java/io/microsphere/spring/boot/test/HttpAutoConfiguration.java`
- `microsphere-spring-boot-test/src/test/java/io/microsphere/spring/boot/test/HttpAutoConfigurationTest.java`
- `microsphere-spring-boot-test/src/test/java/io/microsphere/spring/boot/test/JmxAutoConfigurationTest.java`
- `microsphere-spring-boot-test/src/test/java/io/microsphere/spring/boot/test/ReactiveWebApplicationAutoConfiguration.java`
- `microsphere-spring-boot-test/src/test/java/io/microsphere/spring/boot/test/ReactiveWebApplicationAutoConfigurationTest.java`

### microsphere-spring-boot-webflux（7 文件）

- `microsphere-spring-boot-webflux/src/main/java/io/microsphere/spring/boot/webflux/autoconfigure/WebFluxAutoConfiguration.java`
- `microsphere-spring-boot-webflux/src/main/java/io/microsphere/spring/boot/webflux/autoconfigure/condition/ConditionalOnWebFluxAvailable.java`
- `microsphere-spring-boot-webflux/src/main/java/io/microsphere/spring/boot/webflux/constants/PropertyConstants.java`
- `microsphere-spring-boot-webflux/src/test/java/io/microsphere/spring/boot/webflux/autoconfigure/AbstractWebFluxAutoConfigurationTest.java`
- `microsphere-spring-boot-webflux/src/test/java/io/microsphere/spring/boot/webflux/autoconfigure/WebFluxAutoConfigurationAllDisabledTest.java`
- `microsphere-spring-boot-webflux/src/test/java/io/microsphere/spring/boot/webflux/autoconfigure/WebFluxAutoConfigurationAllEnabledTest.java`
- `microsphere-spring-boot-webflux/src/test/java/io/microsphere/spring/boot/webflux/autoconfigure/WebFluxAutoConfigurationTest.java`

### microsphere-spring-boot-webmvc（8 文件）

- `microsphere-spring-boot-webmvc/src/main/java/io/microsphere/spring/boot/webmvc/autoconfigure/WebMvcAutoConfiguration.java`
- `microsphere-spring-boot-webmvc/src/main/java/io/microsphere/spring/boot/webmvc/autoconfigure/condition/ConditionalOnWebMvcAvailable.java`
- `microsphere-spring-boot-webmvc/src/main/java/io/microsphere/spring/boot/webmvc/constants/PropertyConstants.java`
- `microsphere-spring-boot-webmvc/src/test/java/io/microsphere/spring/boot/webmvc/autoconfigure/AbstractWebMvcAutoConfigurationTest.java`
- `microsphere-spring-boot-webmvc/src/test/java/io/microsphere/spring/boot/webmvc/autoconfigure/WebMvcAutoConfigurationAllDisabledTest.java`
- `microsphere-spring-boot-webmvc/src/test/java/io/microsphere/spring/boot/webmvc/autoconfigure/WebMvcAutoConfigurationAllEnabledTest.java`
- `microsphere-spring-boot-webmvc/src/test/java/io/microsphere/spring/boot/webmvc/autoconfigure/WebMvcAutoConfigurationTest.java`
- `microsphere-spring-boot-webmvc/src/test/java/io/microsphere/spring/boot/webmvc/constants/PropertyConstantsTest.java`

---

**总计**：144 个 Java 文件
