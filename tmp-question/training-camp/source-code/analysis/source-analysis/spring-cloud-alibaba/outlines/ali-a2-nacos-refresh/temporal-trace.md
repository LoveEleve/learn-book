# ALI-A2 Nacos 配置动态刷新 — 时空溯源 (2025.0.0.0 实证)

> 仓库为浅克隆单提交 — 演进证据取自代码内注释 (@since/版本语义/兼容注释)

## 演化主线: 从"换源"到"全链路"的刷新架构演进

| 时代 | 机制 | 证据 (2025.0.0.0 源码) | 演进信号 |
|:--|:--|:--|:--|
| 旧 | NacosContextRefresher 长轮询监听 → NacosConfigRefreshEvent | refresh/NacosContextRefresher.java:117-138 | 事件"五连"回调用 @Deprecated add (NacosRefreshHistory:62) 标识旧 API |
| 中 | @NacosConfig 注解族 (类/字段/方法级) | NacosAnnotationProcessor (793 行) | NacosPropertiesKeyListener/NacosConfigRefreshableListener — 注解式监听 |
| 新 | NacosConfigRefreshEvent → RefreshEvent → Commons 全链路 | NacosConfigRefreshEventListener **@since 2024.10.17** (L32) | **时间锚: 2024 年引入** |
| 精 | Smart rebinder SPECIFIC_BEAN | SmartConfigurationPropertiesRebinder/RefreshBehavior **@since 2021.0.1.1** (L49/L25) | **时间锚: 2021.0.1.1 引入** |

## 关键事件锚

- **@since 2024.10.17** (NacosConfigRefreshEventListener.java:32): 新轨 (RefreshEvent 转发) 引入 — 与 Boot 3/Commons SCC-8 刷新链路对齐
- **@since 2021.0.1.1** (SmartConfigurationPropertiesRebinder:49 + RefreshBehavior:25): SPECIFIC_BEAN 精准刷新引入 — 同版本双文件
- **@ConditionalOnNonDefaultBehavior** (starter refresh/condition/): NonDefaultBehaviorCondition — "非默认行为"才装配 Smart, 默认 ALL_BEANS 保持 Spring 原生
- 注释锚 "Minimize te possibility of making mistakes" (NacosConfigSpringCloudAutoConfiguration.java:45, 原文拼写 te=the): 默认求稳的官方理由
- NacosPropertySourceRefreshListener.containsBean 仲裁 (L98): 新轨抢占后旧轨让贤 — 双轨过渡期的互斥设计

## 架构递进逻辑

```
2021: Smart rebind (精准刷新, 可选)   → 刷新"粒度"可控
2024: RefreshEvent 转发 (全链路)      → 刷新"链路"与 Commons 对齐
持续: NacosContextRefresher 旧轨保留   → 老项目零迁移成本
```
→ 演进方向: 集成层刷新语义逐步向 Spring Cloud Commons 标准链路收敛, 自定义逻辑 (Smart) 退居可选增强。
