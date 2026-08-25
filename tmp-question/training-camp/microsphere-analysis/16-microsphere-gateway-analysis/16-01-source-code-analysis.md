# 16-01：16-microsphere-gateway 源码文件清单

> **核心命题**：16-microsphere-gateway 模块的全部源码文件列表，共 53 个 Java 文件。

---

## 项目结构

### microsphere-spring-cloud-gateway-commons（15 文件）

- `microsphere-spring-cloud-gateway-commons/src/main/java/io/microsphere/spring/cloud/gateway/commons/annotation/ConditionalOnMicrosphereGatewayEnabled.java`
- `microsphere-spring-cloud-gateway-commons/src/main/java/io/microsphere/spring/cloud/gateway/commons/annotation/ConditionalOnMicrosphereWebEndpointMappingEnabled.java`
- `microsphere-spring-cloud-gateway-commons/src/main/java/io/microsphere/spring/cloud/gateway/commons/config/ConfigUtils.java`
- `microsphere-spring-cloud-gateway-commons/src/main/java/io/microsphere/spring/cloud/gateway/commons/config/WebEndpointConfig.java`
- `microsphere-spring-cloud-gateway-commons/src/main/java/io/microsphere/spring/cloud/gateway/commons/config/WebEndpointConfigurationPropertiesBindListener.java`
- `microsphere-spring-cloud-gateway-commons/src/main/java/io/microsphere/spring/cloud/gateway/commons/constants/CommonConstants.java`
- `microsphere-spring-cloud-gateway-commons/src/main/java/io/microsphere/spring/cloud/gateway/commons/constants/CommonsPropertyConstants.java`
- `microsphere-spring-cloud-gateway-commons/src/main/java/io/microsphere/spring/cloud/gateway/commons/constants/RouteConstants.java`
- `microsphere-spring-cloud-gateway-commons/src/test/java/io/microsphere/spring/cloud/gateway/commons/annotation/ConditionalOnMicrosphereGatewayEnabledTest.java`
- `microsphere-spring-cloud-gateway-commons/src/test/java/io/microsphere/spring/cloud/gateway/commons/annotation/ConditionalOnMicrosphereWebEndpointMappingEnabledTest.java`
- `microsphere-spring-cloud-gateway-commons/src/test/java/io/microsphere/spring/cloud/gateway/commons/config/ConfigUtilsTest.java`
- `microsphere-spring-cloud-gateway-commons/src/test/java/io/microsphere/spring/cloud/gateway/commons/config/WebEndpointConfigurationPropertiesBindListenerTest.java`
- `microsphere-spring-cloud-gateway-commons/src/test/java/io/microsphere/spring/cloud/gateway/commons/constants/CommonConstantsTest.java`
- `microsphere-spring-cloud-gateway-commons/src/test/java/io/microsphere/spring/cloud/gateway/commons/constants/CommonsPropertyConstantsTest.java`
- `microsphere-spring-cloud-gateway-commons/src/test/java/io/microsphere/spring/cloud/gateway/commons/constants/RouteConstantsTest.java`

### microsphere-spring-cloud-gateway-server-webflux（25 文件）

- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/annotation/ConditionalOnGatewayAvailable.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/annotation/ConditionalOnGatewayEnabled.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/autoconfigure/GatewayAutoConfiguration.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/autoconfigure/WebEndpointMappingGatewayAutoConfiguration.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/constants/GatewayPropertyConstants.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/context/WebEndpointApplicationContextInitializer.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/event/DisabledHeartbeatEventRouteRefreshListenerInterceptor.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/event/PropagatingRefreshRoutesEventApplicationListener.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/filter/DefaultGatewayFilterChain.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/filter/WebEndpointMappingGlobalFilter.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/handler/CachingFilteringWebHandler.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/handler/FilteringWebHandlerBeanDefinitionRegistryPostProcessor.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/main/java/io/microsphere/spring/cloud/gateway/server/webflux/util/GatewayUtils.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/autoconfigure/GatewayAutoConfigurationTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/autoconfigure/WebEndpointMappingGatewayAutoConfigurationIntegrationTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/autoconfigure/WebEndpointMappingGatewayAutoConfigurationTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/constants/GatewayPropertyConstantsTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/event/DisabledHeartbeatEventRouteRefreshListenerInterceptorTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/event/PropagatingRefreshRoutesEventApplicationListenerTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/filter/DefaultGatewayFilterChainTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/filter/NoOpGatewayFilter.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/filter/WebEndpointMappingGlobalFilterStaticTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/filter/WebEndpointMappingGlobalFilterTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/handler/CachingFilteringWebHandlerTest.java`
- `microsphere-spring-cloud-gateway-server-webflux/src/test/java/io/microsphere/spring/cloud/gateway/server/webflux/util/GatewayUtilsTest.java`

### microsphere-spring-cloud-gateway-server-webmvc（13 文件）

- `microsphere-spring-cloud-gateway-server-webmvc/src/main/java/io/microsphere/spring/cloud/gateway/server/webmvc/annotation/ConditionalOnGatewayServerMvcAvailable.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/main/java/io/microsphere/spring/cloud/gateway/server/webmvc/annotation/ConditionalOnGatewayServerMvcEnabled.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/main/java/io/microsphere/spring/cloud/gateway/server/webmvc/autoconfigure/WebEndpointMappingGatewayServerMvcAutoConfiguration.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/main/java/io/microsphere/spring/cloud/gateway/server/webmvc/constants/GatewayPropertyConstants.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/main/java/io/microsphere/spring/cloud/gateway/server/webmvc/context/WebEndpointApplicationContextInitializer.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/main/java/io/microsphere/spring/cloud/gateway/server/webmvc/filter/WebEndpointMappingHandlerFilterFunction.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/main/java/io/microsphere/spring/cloud/gateway/server/webmvc/filter/WebEndpointMappingHandlerSupplier.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/test/java/io/microsphere/spring/cloud/gateway/server/webmvc/autoconfigure/WebEndpointMappingGatewayServerMvcAutoConfigurationIntegrationDisabledTest.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/test/java/io/microsphere/spring/cloud/gateway/server/webmvc/autoconfigure/WebEndpointMappingGatewayServerMvcAutoConfigurationIntegrationTest.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/test/java/io/microsphere/spring/cloud/gateway/server/webmvc/autoconfigure/WebEndpointMappingGatewayServerMvcAutoConfigurationTest.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/test/java/io/microsphere/spring/cloud/gateway/server/webmvc/constants/GatewayPropertyConstantsTest.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/test/java/io/microsphere/spring/cloud/gateway/server/webmvc/filter/WebEndpointMappingHandlerFilterFunctionTest.java`
- `microsphere-spring-cloud-gateway-server-webmvc/src/test/java/io/microsphere/spring/cloud/gateway/server/webmvc/test/TestApplication.java`

---

**总计**：53 个 Java 文件
