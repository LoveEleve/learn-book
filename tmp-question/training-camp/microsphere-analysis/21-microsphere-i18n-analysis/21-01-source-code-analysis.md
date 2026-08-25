# 21-01：21-microsphere-i18n 源码文件清单

> **核心命题**：21-microsphere-i18n 模块的全部源码文件列表，共 78 个 Java 文件。

---

## 项目结构

### microsphere-i18n-core（28 文件）

- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/AbstractResourceServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/AbstractServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/CompositeServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/DefaultServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/EmptyServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/PropertiesResourceServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/ReloadableResourceServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/ResourceServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/ServiceMessageException.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/ServiceMessageSource.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/util/I18nUtils.java`
- `microsphere-i18n-core/src/main/java/io/microsphere/i18n/util/MessageUtils.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/AbstractI18nTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/AbstractResourceServiceMessageSourceTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/CompositeServiceMessageSourceTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/DefaultServiceMessageSourceTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/EmptyServiceMessageSourceTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/PropertiesResourceServiceMessageSourceTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/ReloadableResourceServiceMessageSourceTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/ResourceServiceMessageSourceTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/ServiceMessageExceptionTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/ServiceMessageSourceTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/TestPropertiesResourceServiceMessageSource.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/TestReloadableResourceServiceMessageSource.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/TestResourceServiceMessageSource.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/TestServiceMessageSource.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/util/I18nUtilsTest.java`
- `microsphere-i18n-core/src/test/java/io/microsphere/i18n/util/MessageUtilsTest.java`

### microsphere-i18n-openfeign（2 文件）

- `microsphere-i18n-openfeign/src/main/java/io/microsphere/i18n/feign/AcceptLanguageHeaderRequestInterceptor.java`
- `microsphere-i18n-openfeign/src/test/java/io/microsphere/i18n/feign/AcceptLanguageHeaderRequestInterceptorTest.java`

### microsphere-i18n-spring（29 文件）

- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/DelegatingServiceMessageSource.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/PropertySourcesServiceMessageSource.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/annotation/EnableI18n.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/annotation/I18nImportBeanDefinitionRegistrar.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/beans/factory/ServiceMessageSourceFactoryBean.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/beans/factory/support/ServiceMessageSourceBeanLifecyclePostProcessor.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/constants/I18nConstants.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/context/I18nApplicationListener.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/context/MessageSourceAdapter.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/context/ResourceServiceMessageSourceChangedEvent.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/util/I18nBeanUtils.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/util/LocaleUtils.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/validation/beanvalidation/I18nLocalValidatorFactoryBeanPostProcessor.java`
- `microsphere-i18n-spring/src/main/java/io/microsphere/i18n/spring/web/servlet/AcceptHeaderLocaleResolverBeanPostProcessor.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/AbstractSpringTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/DelegatingServiceMessageSourceTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/PropertySourcesServiceMessageSourceTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/annotation/EnableI18nTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/annotation/I18nImportBeanDefinitionRegistrarTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/beans/factory/ServiceMessageSourceFactoryBeanTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/beans/factory/support/ServiceMessageSourceBeanLifecyclePostProcessorTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/config/DisabledEnableI18nConfiguration.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/config/TestSourceEnableI18nConfiguration.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/config/UnexposedMessageSourceEnableI18nConfiguration.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/context/I18nApplicationListenerTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/context/MessageSourceAdapterTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/util/I18nBeanUtilsTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/validation/beanvalidation/I18NLocalValidatorFactoryBeanPostProcessorTest.java`
- `microsphere-i18n-spring/src/test/java/io/microsphere/i18n/spring/web/servlet/AcceptHeaderLocaleResolverBeanPostProcessorTest.java`

### microsphere-i18n-spring-boot（9 文件）

- `microsphere-i18n-spring-boot/src/main/java/io/microsphere/i18n/spring/boot/actuate/I18nEndpoint.java`
- `microsphere-i18n-spring-boot/src/main/java/io/microsphere/i18n/spring/boot/actuate/autoconfigure/I18nEndpointAutoConfiguration.java`
- `microsphere-i18n-spring-boot/src/main/java/io/microsphere/i18n/spring/boot/autoconfigure/I18nAutoConfiguration.java`
- `microsphere-i18n-spring-boot/src/main/java/io/microsphere/i18n/spring/boot/condition/ConditionalOnI18nAvailable.java`
- `microsphere-i18n-spring-boot/src/test/java/io/microsphere/i18n/spring/boot/actuate/I18nEndpointTest.java`
- `microsphere-i18n-spring-boot/src/test/java/io/microsphere/i18n/spring/boot/actuate/autoconfigure/I18nEndpointAutoConfigurationBootstrap.java`
- `microsphere-i18n-spring-boot/src/test/java/io/microsphere/i18n/spring/boot/actuate/autoconfigure/I18nEndpointAutoConfigurationTest.java`
- `microsphere-i18n-spring-boot/src/test/java/io/microsphere/i18n/spring/boot/autoconfigure/I18nAutoConfigurationTest.java`
- `microsphere-i18n-spring-boot/src/test/java/io/microsphere/i18n/spring/boot/condition/ConditionalOnI18nAvailableTests.java`

### microsphere-i18n-spring-cloud（5 文件）

- `microsphere-i18n-spring-cloud/src/main/java/io/microsphere/i18n/spring/cloud/autoconfigure/I18nCloudAutoConfiguration.java`
- `microsphere-i18n-spring-cloud/src/main/java/io/microsphere/i18n/spring/cloud/event/ReloadableResourceServiceMessageSourceListener.java`
- `microsphere-i18n-spring-cloud/src/test/java/io/microsphere/i18n/spring/cloud/I18nBootstrap.java`
- `microsphere-i18n-spring-cloud/src/test/java/io/microsphere/i18n/spring/cloud/autoconfigure/I18nCloudAutoConfigurationTest.java`
- `microsphere-i18n-spring-cloud/src/test/java/io/microsphere/i18n/spring/cloud/integration/MessageSourceIntegrationTest.java`

### microsphere-i18n-spring-cloud-server（5 文件）

- `microsphere-i18n-spring-cloud-server/src/main/java/io/microsphere/i18n/spring/cloud/server/annotation/EnableI18nServer.java`
- `microsphere-i18n-spring-cloud-server/src/main/java/io/microsphere/i18n/spring/cloud/server/autoconfigure/I18nServerAutoConfiguration.java`
- `microsphere-i18n-spring-cloud-server/src/main/java/io/microsphere/i18n/spring/cloud/server/controller/I18nServerController.java`
- `microsphere-i18n-spring-cloud-server/src/test/java/io/microsphere/i18n/spring/cloud/server/I18nServerBootstrap.java`
- `microsphere-i18n-spring-cloud-server/src/test/java/io/microsphere/i18n/spring/cloud/server/autoconfigure/I18nServerAutoConfigurationTest.java`

---

**总计**：78 个 Java 文件
