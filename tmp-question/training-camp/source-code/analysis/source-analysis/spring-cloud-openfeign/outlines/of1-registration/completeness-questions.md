# OF-1 注册机制 — completeness-questions (全视角提问验证)

## 开发者视角

1. @EnableFeignClients 怎么触发? (ImportBeanDefinitionRegistrar)
2. 扫描哪些包? (四级兜底)
3. 怎么找 @FeignClient? (AnnotationTypeFilter)
4. 懒/急注册? (lazy-attributes-resolution)
5. BeanDefinition 有哪些属性? (10 个)
6. 配置怎么注册? (FeignClientSpecification)
7. clients() 显式指定? (跳过扫描)
8. 默认配置? (defaultConfiguration)

## 架构师视角

9. 为什么 Import 机制? (与自动装配同源)
10. 为什么 useDefaultFilters=false? (只收 Feign 客户端)
11. 为什么四级包解析兜底? (默认扫启动类包)
12. 为什么 FactoryBean 做 BeanDefinition? (代理创建延迟)
13. 为什么注册期校验? (错误早暴露)
14. 为什么懒/急可配? (启动优化)
15. 为什么 Specification? (对接子上下文)
16. 为什么 refreshableClient? (OF-9 联动)

## 学生视角

17. 什么是 ImportBeanDefinitionRegistrar? (注册回调)
18. 什么是类路径扫描? (找注解类)
19. 什么是 BeanDefinition? (Bean 的蓝图)
20. 什么是 Specification? (配置规范)
