# OF-7 配置隔离 — Pass 2 闭环 Q4: AOT 与隔离语义面

> 核心: AOT 处理器 + FeignClientConfigurer | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 子上下文怎么支持 AOT? 隔离语义的开关从哪来?**

## 机制链 (已实证)

```
FeignClientBeanFactoryInitializationAotProcessor (L68-102):
├── FeignClientFactoryBean 识别 (L85)
├── processAheadOfTime (L102): 扫描子上下文规范
└── BeanDefinitionBuilder 生成 (L205) — 编译期注册子上下文 BeanDefinition

FeignChildContextInitializer (L51) — BeanRegistrationAotProcessor:
├── **ApplicationContextAotGenerator 生成初始化器类** (L107)
└── **per-contextId 初始化器 Map** (L118-122) — 每个子上下文独立初始化器

FeignClientConfigurer (接口):
├── **primary() 默认 true** — 主 Bean 标记
└── **inheritParentConfiguration() 默认 true** (L) — OF-2 继承开关来源!
    → configureFeign L172-173 设置 inheritParentContext
    → FeignClientFactoryBean L100 消费
```

## 关键设计 (why)

1. **AOT 双处理器**: BeanFactoryInitialization (子上下文注册) + BeanRegistration (初始化器生成) — GraalVM native 支持
2. **隔离开关来源完整**: FeignClientConfigurer.inheritParentConfiguration (默认 true) → configureFeign → FactoryBean — 一链贯通 (OF-2 呼应)
3. **primary 默认 true**: 多客户端同类型时主选 — @Primary 语义
4. **编译期生成**: ApplicationContextAotGenerator — native 镜像子上下文可用

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| AOT 处理器 | aot/FeignClientBeanFactoryInitializationAotProcessor.java:68-102 |
| 初始化器生成 | aot/FeignChildContextInitializer.java:107-122 |
| inheritParentConfiguration 默认 true | clientconfig/FeignClientConfigurer.java |
| 开关链 | FeignClientFactoryBean.java:172-173 |

## 负面空间 (Q4 面)

- 不 AOT 全量支持 (核心面生成)
- 不继承配置深度控制 (布尔开关)
- 不做 native 反射全量注册 (按需)

## 域归属说明

- FeignClientFactoryBean 组件消费 → OF-2 (代理创建)
- FeignClientSpecification 注册 → OF-1 (注册机制)
- 指标 Capability → 本域 (配置类内)
