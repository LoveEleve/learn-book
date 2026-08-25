# 25-01：25-microsphere-netflix 源码文件清单

> **核心命题**：25-microsphere-netflix 模块的全部源码文件列表，共 11 个 Java 文件。

---

## 项目结构

### microsphere-netflix-eureka-client-spring-cloud（6 文件）

- `microsphere-netflix-eureka-client-spring-cloud/src/main/java/io/microsphere/netflix/eureka/client/spring/cloud/autoconfigure/EnhancedEurekaClientAutoConfiguration.java`
- `microsphere-netflix-eureka-client-spring-cloud/src/main/java/io/microsphere/netflix/eureka/client/spring/cloud/autoconfigure/MultipleEurekaClientAutoConfiguration.java`
- `microsphere-netflix-eureka-client-spring-cloud/src/main/java/io/microsphere/netflix/eureka/client/spring/cloud/condition/ConditionalOnEurekaClientEnabled.java`
- `microsphere-netflix-eureka-client-spring-cloud/src/main/java/io/microsphere/netflix/eureka/client/spring/cloud/constants/EurekaClientConstants.java`
- `microsphere-netflix-eureka-client-spring-cloud/src/test/java/io/microsphere/netflix/eureka/client/spring/cloud/autoconfigure/EnhancedEurekaClientAutoConfigurationTest.java`
- `microsphere-netflix-eureka-client-spring-cloud/src/test/java/io/microsphere/netflix/eureka/client/spring/cloud/autoconfigure/MultipleEurekaClientAutoConfigurationTest.java`

### microsphere-netflix-eureka-server-spring-cloud（5 文件）

- `microsphere-netflix-eureka-server-spring-cloud/src/main/java/io/microsphere/netflix/eureka/spring/cloud/tomcat/autoconfigure/EurekaServerEmbeddedTomcatAutoConfiguration.java`
- `microsphere-netflix-eureka-server-spring-cloud/src/main/java/io/microsphere/netflix/eureka/spring/cloud/tomcat/servlet/ReplicatedInstanceServletContainerInitializer.java`
- `microsphere-netflix-eureka-server-spring-cloud/src/main/java/io/microsphere/netflix/eureka/spring/cloud/tomcat/servlet/listener/EurekaServerListener.java`
- `microsphere-netflix-eureka-server-spring-cloud/src/main/java/io/microsphere/netflix/eureka/spring/cloud/tomcat/servlet/listener/ReplicatedInstanceListener.java`
- `microsphere-netflix-eureka-server-spring-cloud/src/test/java/io/microsphere/netflix/eureka/spring/cloud/tomcat/sample/EurekaServerApplication.java`

---

**总计**：11 个 Java 文件
