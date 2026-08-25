# 22-01：22-microsphere-logging 源码文件清单

> **核心命题**：22-microsphere-logging 模块的全部源码文件列表，共 60 个 Java 文件。

---

## 项目结构

### microsphere-java-logging（2 文件）

- `microsphere-java-logging/src/main/java/io/microsphere/logging/jdk/JavaLogging.java`
- `microsphere-java-logging/src/test/java/io/microsphere/logging/jdk/JavaLoggingTest.java`

### microsphere-log4j（8 文件）

- `microsphere-log4j/src/main/java/io/microsphere/logging/log4j/Log4jLogger.java`
- `microsphere-log4j/src/main/java/io/microsphere/logging/log4j/Log4jLoggerFactory.java`
- `microsphere-log4j/src/main/java/io/microsphere/logging/log4j/Log4jLogging.java`
- `microsphere-log4j/src/main/java/io/microsphere/logging/log4j/util/Log4jUtils.java`
- `microsphere-log4j/src/test/java/io/microsphere/logging/log4j/Log4JLoggingTest.java`
- `microsphere-log4j/src/test/java/io/microsphere/logging/log4j/Log4jLoggerFactoryTest.java`
- `microsphere-log4j/src/test/java/io/microsphere/logging/log4j/Log4jLoggerTest.java`
- `microsphere-log4j/src/test/java/io/microsphere/logging/log4j/util/Log4jUtilsTest.java`

### microsphere-log4j2（16 文件）

- `microsphere-log4j2/src/main/java/io/microsphere/logging/log4j2/Log4j2Logger.java`
- `microsphere-log4j2/src/main/java/io/microsphere/logging/log4j2/Log4j2LoggerFactory.java`
- `microsphere-log4j2/src/main/java/io/microsphere/logging/log4j2/Log4j2Logging.java`
- `microsphere-log4j2/src/main/java/io/microsphere/logging/log4j2/LogEventComparator.java`
- `microsphere-log4j2/src/main/java/io/microsphere/logging/log4j2/appender/InMemoryAppender.java`
- `microsphere-log4j2/src/main/java/io/microsphere/logging/log4j2/layout/DelegatingLayout.java`
- `microsphere-log4j2/src/main/java/io/microsphere/logging/log4j2/layout/SmartFileAppenderLayout.java`
- `microsphere-log4j2/src/main/java/io/microsphere/logging/log4j2/util/Log4j2Utils.java`
- `microsphere-log4j2/src/test/java/io/microsphere/logging/log4j2/Log4j2LoggerFactoryTest.java`
- `microsphere-log4j2/src/test/java/io/microsphere/logging/log4j2/Log4j2LoggerTest.java`
- `microsphere-log4j2/src/test/java/io/microsphere/logging/log4j2/Log4j2LoggingTest.java`
- `microsphere-log4j2/src/test/java/io/microsphere/logging/log4j2/LogEventComparatorTest.java`
- `microsphere-log4j2/src/test/java/io/microsphere/logging/log4j2/appender/InMemoryAppenderTest.java`
- `microsphere-log4j2/src/test/java/io/microsphere/logging/log4j2/layout/DelegatingLayoutTest.java`
- `microsphere-log4j2/src/test/java/io/microsphere/logging/log4j2/layout/SmartFileAppenderLayoutTest.java`
- `microsphere-log4j2/src/test/java/io/microsphere/logging/log4j2/util/Log4j2UtilsTest.java`

### microsphere-logback（4 文件）

- `microsphere-logback/src/main/java/io/microsphere/logging/logback/LogbackLogging.java`
- `microsphere-logback/src/main/java/io/microsphere/logging/logback/util/LogbackUtils.java`
- `microsphere-logback/src/test/java/io/microsphere/logging/logback/LogbackLoggingTest.java`
- `microsphere-logback/src/test/java/io/microsphere/logging/logback/util/LogbackUtilsTest.java`

### microsphere-logging-commons（13 文件）

- `microsphere-logging-commons/src/main/java/io/microsphere/logging/DefaultLoggingLevelsResolver.java`
- `microsphere-logging-commons/src/main/java/io/microsphere/logging/Logging.java`
- `microsphere-logging-commons/src/main/java/io/microsphere/logging/LoggingLevelsResolver.java`
- `microsphere-logging-commons/src/main/java/io/microsphere/logging/LoggingUtils.java`
- `microsphere-logging-commons/src/main/java/io/microsphere/logging/jmx/LoggingMXBeanAdapter.java`
- `microsphere-logging-commons/src/main/java/io/microsphere/logging/jmx/LoggingMXBeanRegistrar.java`
- `microsphere-logging-commons/src/test/java/io/microsphere/logging/DefaultLoggingLevelsResolverTest.java`
- `microsphere-logging-commons/src/test/java/io/microsphere/logging/LoggingTest.java`
- `microsphere-logging-commons/src/test/java/io/microsphere/logging/LoggingUtilsTest.java`
- `microsphere-logging-commons/src/test/java/io/microsphere/logging/TestingLogging.java`
- `microsphere-logging-commons/src/test/java/io/microsphere/logging/ThrowableLogging.java`
- `microsphere-logging-commons/src/test/java/io/microsphere/logging/jmx/LoggingMXBeanAdapterTest.java`
- `microsphere-logging-commons/src/test/java/io/microsphere/logging/jmx/LoggingMXBeanRegistrarTest.java`

### microsphere-logging-examples（1 文件）

- `microsphere-logging-examples/src/main/java/io/microsphere/logging/examples/LoggingExample.java`

### microsphere-logging-test（16 文件）

- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/junit4/LoggingLevelsRule.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/junit4/LoggingLevelsStatement.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/LoggingLevelsClass.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/LoggingLevelsTest.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/extension/logging/LoggingLevelCallback.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/extension/logging/LoggingLevelParameterResolver.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/extension/logging/LoggingLevelTemplateInvocationContext.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/extension/logging/LoggingLevelsAttributes.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/extension/logging/LoggingLevelsClassExtension.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/extension/logging/LoggingLevelsExtension.java`
- `microsphere-logging-test/src/main/java/io/microsphere/logging/test/jupiter/extension/logging/LoggingLevelsTestExtension.java`
- `microsphere-logging-test/src/test/java/io/microsphere/logging/test/DefaultLoggingLevelsClass.java`
- `microsphere-logging-test/src/test/java/io/microsphere/logging/test/junit4/LoggingLevelsRuleTest.java`
- `microsphere-logging-test/src/test/java/io/microsphere/logging/test/jupiter/LoggingLevelsClassTest.java`
- `microsphere-logging-test/src/test/java/io/microsphere/logging/test/jupiter/LoggingLevelsTestTest.java`
- `microsphere-logging-test/src/test/java/io/microsphere/logging/test/jupiter/extension/logging/LoggingLevelParameterResolverTest.java`

---

**总计**：60 个 Java 文件
