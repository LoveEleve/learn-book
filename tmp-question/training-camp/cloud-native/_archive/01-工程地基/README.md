# 01 工程地基（Week 1-2 · 5课时）

## 学完能干什么
搭出标准化多模块 Maven 工程，写出带校验/异常处理/幂等性的 REST API，理解 Spring Boot 自动装配原理。

## 课时 01：Maven 多模块工程

**📖 读**: `stage-1/docs/01.` + `02.`（全文 ~1400 行）

**9 个核心概念**：
1. POM 继承 vs 聚合、2. 依赖仲裁"最近定义优先"、3. BOM import scope、4. 六种依赖作用域、5. 模块命名规范、6. 业务四层结构（api/data/core/web）、7. Spring Boot Maven Plugin 排除 lombok、8. Build Profiles、9. Flatten Maven Plugin

**🔍 看**: `stage-1/src/biz-project/biz-dependencies/pom.xml`（BOM 写法）、`biz-web/pom.xml`（Spring Boot Plugin）

**✏️ 动手**: `cd stage-1/src/biz-project && mvn dependency:tree | head -30` 找一处依赖仲裁实例

## 课时 02：REST API 服务端

**📖 读**: `stage-1/docs/03.`（全文 ~900 行）

**核心链路**: DispatcherServlet → HandlerMapping → HandlerAdapter → ModelAndView

**🔍 看**: `biz-api/.../ApiRequest.java`（泛型请求模型）、`biz-api/.../ApiResponse.java`（code/message）、`biz-web/.../mvc/method/annotation/ApiResponseHandlerMethodReturnValueHandler.java`（POJO 隐式包装）

**✅ 算过关**: 如果让你实现请求去重（幂等），用什么数据结构？放哪里？

## 课时 03：REST API 客户端

**📖 读**: `stage-1/docs/04.`（全文 ~700 行）

**三层扩展**: HttpMessageConverter（序列化）→ ClientHttpRequestFactory（网络）→ ClientHttpRequestInterceptor（拦截器链）

**🔍 看**: `biz-api/.../interfaces/UserService.java`（@FeignClient）、`biz-client/.../BizClientApplication.java`

**✅ 算过关**: `InterceptingClientHttpRequestFactory` 怎么用装饰器模式实现拦截器链？

## 课时 04：Spring 脚手架定制

**📖 读**: `stage-1/docs/23.` + `24.`（~600 行）

**生成流程**: ProjectRequest → ProjectDescription → ProjectGenerator.generate()

## 课时 05：Spring Boot 自动装配

**📖 补充**: segfault-lessons/spring-boot/lesson-20/（PersonAutoConfiguration 完整源码）

**三个条件注解**: @ConditionalOnWebApplication / @ConditionalOnProperty / @AutoConfigureAfter —— 这就是 microsphere-spring-boot 所有 AutoConfiguration 的原型

## 本阶段自检清单
- [ ] 能独立搭建 api/data/core/web 四层 Maven 工程
- [ ] 理解依赖仲裁和 BOM 的作用
- [ ] 能写出带统一模型/校验/异常处理的 REST API
- [ ] 理解 DispatcherServlet 的处理流程
- [ ] 能写一个自定义 Spring Boot Starter
