# OF-2 代理创建与装配 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | FeignClientFactoryBean 骨架: getTarget + 双路径 (url/无 url) + configureUsingConfiguration 组件装配; Targeter/DefaultTargeter |
| 2.1+ | FeignBlockingLoadBalancerClient unwrap (有 url 剥 LB); FeignBuilderCustomizer 引入 |
| 3.x | OptionsFactoryBean followRedirects; Capability 能力集 (缓存); ExceptionPropagationPolicy; RefreshableHardCodedTarget (刷新); PropertyBasedTarget (AOT) |
| 4.x | loadBalance 生产陷阱错误信息; AnnotationAwareOrderComparator 定制排序 |

## 痕迹证据

- FeignClientFactoryBean.java:465-503: getTarget 双路径 + targeter.target (2.x 锚)
- FeignClientFactoryBean.java:449-451: "Did you forget to include spring-cloud-starter-loadbalancer?" (3.x+ 锚)
- FeignClientFactoryBean.java:495-497: unwrap 注释 "not load balancing because we have a url" (2.1+ 锚)
- FeignClientFactoryBean.java:153-161: applyBuildCustomizers + AnnotationAwareOrderComparator (2.1+ 锚)
- OptionsFactoryBean.java:60-95: followRedirects 三级优先级 (3.x 锚)
- FeignClientFactoryBean.java:524-542: resolveTarget 三分支 (Refreshable 3.x/PropertyBased 3.x 锚)

## 推断标注

- "2.x 骨架" — Spring Cloud OpenFeign 公知版本线 (标注)
- "2.1+ unwrap" — 注释锚实证 (实证)
- "3.x 刷新/懒加载" — RefreshableHardCodedTarget/PropertyBasedTarget 类实证 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- Feign 本体 F-1 Builder/F-3 代理: feign.target 底座 vs FactoryBean 封装 — 底座对照
- Spring FactoryBean 机制: 工厂 Bean 模式 (spring 域) — 机制对照
- SCC C-13 NamedContextFactory: 子上下文组件获取 (getInstances) — 上下文对照 (OF-7)
