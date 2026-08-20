# OF-7 配置隔离 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | FeignClientFactory extends NamedContextFactory + FeignClientsConfiguration (默认组件) |
| 2.1+ | FeignClientProperties (defaultConfig + config Map); FeignClientConfigurer (primary/inheritParentConfiguration) |
| 3.x | isDefaultToProperties 覆盖开关; AOT 双处理器 (BeanFactoryInitialization + ChildContextInitializer); Micrometer Capability 条件装配 |
| 4.x | FeignChildContextInitializer (ApplicationContextAotGenerator 生成); per-contextId 初始化器 |

## 痕迹证据

- FeignClientFactory.java:39: extends NamedContextFactory (2.x 锚)
- FeignClientFactory.java:47-48: super(FeignClientsConfiguration, "spring.cloud.openfeign", "spring.cloud.openfeign.client.name") (2.x 锚)
- FeignClientProperties.java:31-33: defaultConfig + config Map (2.1+ 锚)
- FeignClientFactoryBean.java:174-188: isDefaultToProperties 分支 (3.x 锚)
- FeignClientFactoryBean.java:172-173: inheritParentContext 从 Configurer 读 (2.1+ 锚)
- aot/FeignChildContextInitializer.java:51,107-122: BeanRegistrationAotProcessor + 初始化器生成 (4.x 锚)

## 推断标注

- "2.x 骨架" — Spring Cloud OpenFeign 公知版本线 (标注)
- "2.1+ Properties" — 类实证 (实证)
- "3.x 开关/AOT" — 常量/类实证 (实证)
- "4.x 初始化器生成" — ApplicationContextAotGenerator 实证 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- SCC C-13 NamedContextFactory: scc13-named-context 域 (子上下文机制) — 底座对照
- Spring @Configuration: 配置类机制 vs 子上下文 — 容器对照
- Boot AOT: 编译期处理 vs 运行时 — 启动对照
