# OF-1 注册机制 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | @EnableFeignClients + FeignClientsRegistrar 骨架: ImportBeanDefinitionRegistrar + ClassPathScanning 扫描 + FeignClientFactoryBean 注册 |
| 2.1+ | contextId 引入 (同 name 多上下文区分); FeignClientSpecification (NamedContextFactory.Specification) |
| 3.x | 懒注册模式 (lazy-attributes-resolution); validateFallback 校验; qualifiers 限定符 |
| 4.x | refreshableClient 属性 (刷新开关); EnvironmentAware 增强 |

## 痕迹证据

- EnableFeignClients.java:50-88: 5 组属性 (2.x 锚)
- FeignClientsRegistrar.java:71: ImportBeanDefinitionRegistrar (2.x 锚)
- FeignClientsRegistrar.java:210-215: lazy-attributes-resolution (3.x 锚)
- FeignClientsRegistrar.java:225-245: BeanDefinitionBuilder 10 属性 (3.x+ 锚, refreshableClient 4.x)
- FeignClientSpecification.java:29: NamedContextFactory.Specification (2.1+ 锚)
- FeignClientsRegistrar.java:83-87: validateFallback 校验 (3.x 锚)

## 推断标注

- "2.x 骨架" — Spring Cloud OpenFeign 公知版本线 (标注)
- "3.x 懒注册" — lazy-attributes-resolution 常量实证 (实证)
- "4.x refreshableClient" — 属性实证 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- Spring @ComponentScan: 同源扫描器 (ClassPathScanningCandidateComponentProvider) — 扫描对照
- MyBatis MapperScan (MapperScannerRegistrar): ImportBeanDefinitionRegistrar 同类 — 注册对照
- SCC (C-13 NamedContextFactory): Specification 对接 — 子上下文对照 (OF-7)
