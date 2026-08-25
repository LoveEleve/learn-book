# 08-01：08-microsphere-redis 源码文件清单

> **核心命题**：08-microsphere-redis 模块的全部源码文件列表，共 181 个 Java 文件。

---

## 项目结构

### microsphere-redis-core（21 文件）

- `microsphere-redis-core/src/main/java/io/microsphere/redis/metadata/MethodInfo.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/metadata/MethodMetadata.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/metadata/Parameter.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/metadata/ParameterMetadata.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/metadata/RedisMetadata.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/metadata/RedisMetadataLoader.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/util/RawValue.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/util/RedisCommandUtils.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/util/RedisUtils.java`
- `microsphere-redis-core/src/main/java/io/microsphere/redis/util/ValueHolder.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/metadata/MethodInfoTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/metadata/MethodMetadataTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/metadata/ParameterMetadataTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/metadata/ParameterTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/metadata/RedisMetadataLoaderTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/metadata/RedisMetadataTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/metadata/TestRedisMetadataLoader.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/util/RawValueTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/util/RedisCommandUtilsTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/util/RedisUtilsTest.java`
- `microsphere-redis-core/src/test/java/io/microsphere/redis/util/ValueHolderTest.java`

### microsphere-redis-generator（4 文件）

- `microsphere-redis-generator/src/main/java/io/microsphere/redis/generator/doclet/SpringDataRedisMetadataGenerationDoclet.java`
- `microsphere-redis-generator/src/main/java/io/microsphere/redis/generator/doclet/StandardOption.java`
- `microsphere-redis-generator/src/main/java/io/microsphere/redis/generator/doclet/logging/ReporterLoggerAdapter.java`
- `microsphere-redis-generator/src/main/java/io/microsphere/redis/generator/metadata/SpringDataRedisMetadataGenerator.java`

### microsphere-redis-replicator-spring（26 文件）

- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/RedisCommandReplicator.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/RedisReplicatorInitializer.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/RedisReplicatorModuleInitializer.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/config/RedisReplicatorConfiguration.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/event/RedisCommandReplicatedEvent.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/kafka/KafkaRedisReplicatorConfiguration.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/kafka/KafkaRedisReplicatorModuleInitializer.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/kafka/consumer/KafkaConsumerRedisReplicatorConfiguration.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/kafka/producer/KafkaProducerRedisCommandEventListener.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/kafka/producer/KafkaProducerRedisReplicatorConfiguration.java`
- `microsphere-redis-replicator-spring/src/main/java/io/microsphere/redis/replicator/spring/kafka/producer/RedisComandEventPartitioner.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/AbstractRedisReplicatorTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/RedisCommandReplicatorIntegrationTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/RedisCommandReplicatorTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/RedisReplicatorInitializerTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/config/DefaultRedisConfig.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/config/DefaultRedisReplicationConfig.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/config/FullRedisReplicationConfig.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/config/RedisReplicatorConfigurationTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/event/RedisCommandReplicatedEventTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/kafka/KafkaRedisReplicatorConfigurationTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/kafka/KafkaRedisReplicatorModuleInitializerIntegrationTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/kafka/KafkaRedisReplicatorModuleInitializerTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/kafka/consumer/KafkaConsumerRedisReplicatorConfigurationTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/kafka/producer/KafkaProducerRedisCommandEventListenerTest.java`
- `microsphere-redis-replicator-spring/src/test/java/io/microsphere/redis/replicator/spring/kafka/producer/KafkaProducerRedisReplicatorConfigurationTest.java`

### microsphere-redis-spring（115 文件）

- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/annotation/EnableRedisConfiguration.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/annotation/EnableRedisContext.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/annotation/EnableRedisInterceptor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/annotation/RedisConfigurationBeanDefinitionRegistrar.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/annotation/RedisContextBeanDefinitionRegistrar.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/annotation/RedisInterceptorBeanDefinitionRegistrar.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/beans/HoldingValueRedisTemplateWrapperProcessor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/beans/HoldingValueStringRedisTemplateWrapperProcessor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/beans/RedisConnectionFactoryProxyBeanPostProcessor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/beans/RedisTemplateWrapper.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/beans/RedisTemplateWrapperBeanPostProcessor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/beans/StringRedisTemplateWrapper.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/beans/WrapperProcessors.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/config/RedisConfiguration.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/connection/dynamic/DynamicRedisConnectionFactory.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/connection/dynamic/web/DynamicRedisConnectionFactoryCleanerListener.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/context/RedisContext.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/context/RedisInitializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/context/RedisInterceptorModuleInitializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/context/RedisModuleInitializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/event/RedisCommandEvent.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/event/RedisConfigurationPropertyChangedEvent.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/event/RedisOperationEvent.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/interceptor/EventPublishingRedisCommandInterceptor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/interceptor/InterceptingRedisConnectionInvocationHandler.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/interceptor/RedisCommandInterceptor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/interceptor/RedisConnectionInterceptor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/interceptor/RedisMethodContext.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/interceptor/RedisMethodInterceptor.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/metadata/SpringRedisMetadataLoader.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/metadata/SpringRedisMetadataRepository.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/AbstractSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/BooleanSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/BoundarySerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/ByteArraySerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/DoubleSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/EnumSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/ExpirationSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/GeoLocationSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/HoldingValueRedisSerializerWrapper.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/IntegerSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/LongSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/PointSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/RangeModel.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/RangeSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/RedisCommandEventSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/RedisZSetCommandsRangeSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/Serializers.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/ShortSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/SortParametersSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/serializer/WeightsSerializer.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/util/RedisConstants.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/util/RedisSpringUtils.java`
- `microsphere-redis-spring/src/main/java/io/microsphere/redis/spring/util/SpringRedisCommandUtils.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/AbstractRedisCommandEventTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/annotation/EnableRedisConfigurationTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/annotation/EnableRedisContextTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/annotation/EnableRedisInterceptorTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/annotation/EnableRedisInterceptorWithoutExposeCommandEventTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/annotation/RedisConfigurationBeanDefinitionRegistrarTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/annotation/RedisInterceptorBeanDefinitionRegistrarTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/beans/HoldingValueRedisTemplateWrapperProcessorTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/beans/HoldingValueStringRedisTemplateWrapperProcessorTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/beans/RedisConnectionFactoryProxyBeanPostProcessorTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/beans/RedisTemplateWrapperBeanPostProcessorTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/beans/RedisTemplateWrapperTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/beans/StringRedisTemplateWrapperTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/beans/WrapperProcessorTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/config/RedisConfigurationTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/config/RedisContextConfig.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/connection/dynamic/DynamicRedisConnectionFactoryTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/connection/dynamic/web/DynamicRedisConnectionFactoryCleanerListenerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/context/RedisContextTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/context/RedisInitializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/context/RedisInterceptorModuleInitializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/event/RedisCommandEventIntegrationTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/event/RedisCommandEventTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/event/RedisConfigurationPropertyChangedEventTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/event/RedisOperationEventTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/interceptor/EventPublishingRedisCommandInterceptorTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/interceptor/LoggingRedisCommandInterceptor.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/interceptor/LoggingRedisConnectionInterceptor.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/interceptor/RedisMethodContextTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/interceptor/RedisMethodInterceptorTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/interceptor/StopWatchRedisConnectionInterceptor.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/interceptor/ThrowingExceptionRedisCommandInterceptor.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/metadata/RedisMetadataRepositoryTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/metadata/SpringRedisMetadataLoaderTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/metadata/SpringRedisMetadataRepositoryTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/AbstractSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/BooleanSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/BoundarySerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/ByteArraySerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/DoubleSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/EnumSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/ExpirationSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/GeoLocationSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/HoldingValueRedisSerializerWrapperTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/IntegerSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/LargeEnum.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/LongSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/PointSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/PositionSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/RangeSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/RedisCommandEventSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/RedisZSetCommandsRangeSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/SerializersTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/SetOptionSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/ShortSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/SortParametersSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/TimeUnitSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/serializer/WeightsSerializerTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/util/RedisConstantsTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/util/RedisSpringUtilsTest.java`
- `microsphere-redis-spring/src/test/java/io/microsphere/redis/spring/util/SpringRedisCommandUtilsTest.java`

### microsphere-redis-spring-boot（7 文件）

- `microsphere-redis-spring-boot/src/main/java/io/microsphere/redis/spring/boot/autoconfigure/condition/ConditionalOnRedisAvailable.java`
- `microsphere-redis-spring-boot/src/main/java/io/microsphere/redis/spring/boot/autoconfigure/condition/ConditionalOnRedisEnabled.java`
- `microsphere-redis-spring-boot/src/main/java/io/microsphere/redis/spring/boot/autoconfigure/condition/ConditionalOnRedisInterceptorEnabled.java`
- `microsphere-redis-spring-boot/src/test/java/io/microsphere/redis/spring/boot/autoconfigure/RedisInitializerSpringBootTest.java`
- `microsphere-redis-spring-boot/src/test/java/io/microsphere/redis/spring/boot/autoconfigure/condition/ConditionalOnRedisAvailableTest.java`
- `microsphere-redis-spring-boot/src/test/java/io/microsphere/redis/spring/boot/autoconfigure/condition/ConditionalOnRedisEnabledTest.java`
- `microsphere-redis-spring-boot/src/test/java/io/microsphere/redis/spring/boot/autoconfigure/condition/ConditionalOnRedisInterceptorEnabledTest.java`

### microsphere-redis-spring-cloud（5 文件）

- `microsphere-redis-spring-cloud/src/main/java/io/microsphere/redis/spring/cloud/autoconfigure/RedisCloudAutoConfiguration.java`
- `microsphere-redis-spring-cloud/src/main/java/io/microsphere/redis/spring/cloud/event/PropagatingRedisConfigurationPropertyChangedEventApplicationListener.java`
- `microsphere-redis-spring-cloud/src/test/java/io/microsphere/redis/spring/cloud/autoconfigure/RedisCloudAutoConfigurationIntegrationTest.java`
- `microsphere-redis-spring-cloud/src/test/java/io/microsphere/redis/spring/cloud/autoconfigure/RedisCloudAutoConfigurationTest.java`
- `microsphere-redis-spring-cloud/src/test/java/io/microsphere/redis/spring/cloud/event/PropagatingRedisConfigurationPropertyChangedEventApplicationListenerTest.java`

### microsphere-redis-spring-test（3 文件）

- `microsphere-redis-spring-test/src/main/java/io/microsphere/redis/spring/test/AbstractRedisTest.java`
- `microsphere-redis-spring-test/src/main/java/io/microsphere/redis/spring/test/config/RedisConfig.java`
- `microsphere-redis-spring-test/src/test/java/io/microsphere/redis/spring/test/RedisTest.java`

---

**总计**：181 个 Java 文件
