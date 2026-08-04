# 12 - 嵌入式 Tomcat 源码分析

## 源码信息

- **分析对象**: Apache Tomcat 官方源码（嵌入式集成 Spring Boot）
- **Tomcat**: `/data/workspace/source-code/code/spring/tomcat`（版本 10.1.34，Jakarta EE）
- **Spring Boot**: `/data/workspace/source-code/code/spring/spring-boot`（版本 3.5.16）

> 说明：本系列原计划分析 microsphere-tomcat（github.com/microsphere-projects/microsphere-tomcat），
> 但该本地仓库（`/data/workspace/java-training-camp/cloud-native-code/share/microsphere-tomcat`）
> 只有 3 个空壳 `pom.xml`，没有任何 Java 源码，无法分析。因此本系列实际分析的是
> **Apache 官方 Tomcat 10.1.34** 嵌入式集成 Spring Boot 3.5.16 的源码机制，详见 `交接文档.md`。
