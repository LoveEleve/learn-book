# 20-01：20-microsphere-microprofile 源码文件清单

> **核心命题**：20-microsphere-microprofile 模块的全部源码文件列表，共 101 个 Java 文件。

---

## 项目结构

### microsphere-microprofile-config（52 文件）

- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/DefaultConfig.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/DefaultConfigBuilder.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/DefaultConfigProviderResolver.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/DefaultConfigValue.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/annotation/ConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/annotation/ConfigSourceFactory.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/annotation/DefaultConfigSourceFactory.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/AbstractConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/BigDecimalConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/BigIntegerConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/BooleanConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/ByteConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/CharacterConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/ClassConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/Converters.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/DelegatingConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/DoubleConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/FloatConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/IntegerConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/LongConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/ObjectConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/PrioritizedConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/ShortConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/StringConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/converter/URIConverter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/servlet/listener/ConfigServletRequestListener.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/ConfigSourceOrdinalComparator.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/ConfigSources.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/DefaultResourceConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/DefaultResourceConfigSources.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/DynamicConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/EnumerableConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/JavaSystemPropertiesConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/MapBasedConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/MapConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/OperationSystemEnvironmentVariablesConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/servlet/FilterConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/servlet/ServletConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/servlet/ServletContextConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/servlet/ServletRequestHeaderConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/servlet/ServletRequestParameterConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/servlet/initializer/ServletConfigInitializer.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/servlet/initializer/ServletContextConfigInitializer.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/servlet/initializer/ServletRequestThreadLocalListener.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/spring/ConfigSourcesAdapter.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/source/spring/PropertySourceConfigSource.java`
- `microsphere-microprofile-config/src/main/java/io/microsphere/microprofile/config/util/DelegatingPropertiesAdapter.java`
- `microsphere-microprofile-config/src/main/java/sun/net/www/protocol/classpath/Handler.java`
- `microsphere-microprofile-config/src/test/java/io/microsphere/microprofile/config/annotation/ConfigSourceTest.java`
- `microsphere-microprofile-config/src/test/java/io/microsphere/microprofile/config/converter/ConvertersTest.java`
- `microsphere-microprofile-config/src/test/java/io/microsphere/microprofile/config/inject/ConfigPropertyTest.java`
- `microsphere-microprofile-config/src/test/java/io/microsphere/microprofile/config/source/spring/PropertySourceConfigSourceTest.java`

### microsphere-microprofile-fault-tolerance（18 文件）

- `microsphere-microprofile-fault-tolerance/src/main/java/io/microsphere/microprofile/faulttolerance/AsynchronousInterceptor.java`
- `microsphere-microprofile-fault-tolerance/src/main/java/io/microsphere/microprofile/faulttolerance/BulkheadInterceptor.java`
- `microsphere-microprofile-fault-tolerance/src/main/java/io/microsphere/microprofile/faulttolerance/CircuitBreakerInterceptor.java`
- `microsphere-microprofile-fault-tolerance/src/main/java/io/microsphere/microprofile/faulttolerance/FallbackInterceptor.java`
- `microsphere-microprofile-fault-tolerance/src/main/java/io/microsphere/microprofile/faulttolerance/RetryInterceptor.java`
- `microsphere-microprofile-fault-tolerance/src/main/java/io/microsphere/microprofile/faulttolerance/TimeoutInterceptor.java`
- `microsphere-microprofile-fault-tolerance/src/main/java/io/microsphere/microprofile/faulttolerance/package-info.java`
- `microsphere-microprofile-fault-tolerance/src/main/java/io/microsphere/microprofile/faulttolerance/util/TimeUtils.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/AsynchronousInterceptorTest.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/BulkheadInterceptorTest.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/BuzService.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/BuzServiceImpl.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/CircuitBreakerInterceptorTest.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/EchoService.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/EchoServiceCglibTest.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/FallbackInterceptorTest.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/RetryInterceptorTest.java`
- `microsphere-microprofile-fault-tolerance/src/test/java/io/microsphere/microprofile/faulttolerance/TimeoutInterceptorTest.java`

### microsphere-microprofile-rest-client（31 文件）

- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/DefaultRestClientBuilder.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/DefaultRestClientBuilderResolver.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/ReflectiveRequestTemplateResolver.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/RequestTemplate.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/RequestTemplateResolver.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/annotation/AnnotatedParamMetadata.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/reflect/RestClientInterfaceInvocationHandler.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/uri/MatrixParamUriBuilderAssembler.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/uri/PathParamUriBuilderAssembler.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/uri/QueryParamUriBuilderAssembler.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/microprofile/rest/uri/UriBuilderAssembler.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/DefaultRuntimeDelegate.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/client/DefaultClient.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/client/DefaultClientBuilder.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/client/DefaultInvocationBuilder.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/client/DefaultVariantListBuilder.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/client/HttpGetInvocation.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/client/ImmutableWebTarget.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/client/MutableClientRequestContext.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/client/MutableClientResponseContext.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/core/DefaultResponse.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/core/DefaultResponseBuilder.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/core/DefaultUriBuilder.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/util/PathUtils.java`
- `microsphere-microprofile-rest-client/src/main/java/io/microsphere/rest/util/URLUtils.java`
- `microsphere-microprofile-rest-client/src/test/java/io/microsphere/microprofile/rest/DefaultRestClientBuilderTest.java`
- `microsphere-microprofile-rest-client/src/test/java/io/microsphere/rest/core/DefaultUriBuilderTest.java`
- `microsphere-microprofile-rest-client/src/test/java/io/microsphere/rest/demo/HttpURLConnectionDemo.java`
- `microsphere-microprofile-rest-client/src/test/java/io/microsphere/rest/demo/RestClientDemo.java`
- `microsphere-microprofile-rest-client/src/test/java/io/microsphere/rest/util/Maps.java`
- `microsphere-microprofile-rest-client/src/test/java/io/microsphere/rest/util/URLUtilsTest.java`

---

**总计**：101 个 Java 文件
