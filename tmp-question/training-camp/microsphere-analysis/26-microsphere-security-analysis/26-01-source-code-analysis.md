# 26-01：26-microsphere-security 源码文件清单

> **核心命题**：26-microsphere-security 模块的全部源码文件列表，共 12 个 Java 文件。

---

## 项目结构

### microsphere-security-core（2 文件）

- `microsphere-security-core/src/main/java/io/microsphere/security/constants/SecurityConstants.java`
- `microsphere-security-core/src/test/java/io/microsphere/security/constants/SecurityConstantsTest.java`

### microsphere-security-spring-boot（10 文件）

- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/annotation/WebSecurityFilter.java`
- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/autoconfigure/WebSecurityProperties.java`
- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/autoconfigure/web/servlet/ServletWebSecurityAutoConfiguration.java`
- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/condition/ConditionalOnEnabledSecurity.java`
- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/condition/ConditionalOnEnabledWebSecurity.java`
- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/web/servlet/WebSecurityProcessor.java`
- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/web/servlet/WebSecurityProcessorFilter.java`
- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/web/servlet/WebSecurityProcessors.java`
- `microsphere-security-spring-boot/src/main/java/io/microsphere/security/spring/boot/web/servlet/csp/ContentSecurityPolicyProcessor.java`
- `microsphere-security-spring-boot/src/test/java/io/microsphere/security/spring/boot/autoconfigure/web/servlet/ServletWebSecurityAutoConfigurationTest.java`

---

**总计**：12 个 Java 文件
