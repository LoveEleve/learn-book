# ALI-A3 Nacos 服务发现+注册 — 时空溯源 (2025.0.0.0 实证)

> 仓库为浅克隆单提交 — 演进证据取自代码内注释 (@since/@Deprecated)

## 演化主线: 发现/注册从薄封装到"容错+元数据增强"

| 时代 | 机制 | 证据 (2025.0.0.0 源码) | 演进信号 |
|:--|:--|:--|:--|
| 基座 | DiscoveryClient/ServiceRegistry SPI 实现 | NacosDiscoveryClient (92 行) / NacosServiceRegistry (193 行) | 全 5 方法实现 (SCC-3/4 契约) |
| 容错 | ServiceCache 静态缓存 | ServiceCache `@since 2021.0.1.0` (L39) | 时间锚: 2021.0.1.0 引入 |
| 改名 | set/get → setServiceIds/getServiceIds | `@Deprecated since 2021.0.1.1` (L72/L91) | 时间锚: 2021.0.1.1 改名 |
| 增强 | failure-tolerance-enabled 开关 | NacosDiscoveryClient:47-48 @Value 注入 | 容错从默认行为变可选 |
| 增强 | 六键元数据命名空间 | NacosServiceDiscovery:98-106 | "nacos." 前缀隔离 |

## 关键事件锚

- **@since 2021.0.1.0** (ServiceCache.java:39): 静态写穿缓存引入 — 与 NacosConfigDataLocationResolver 同版本 (A1 的 ConfigData 轨)
- **@since 2021.0.1.1** (ServiceCache.java:72/91): set/get → setServiceIds/getServiceIds — API 命名规范化 (2021.0.1.1 也是 Smart rebinder 引入版本 — 该版本是"精细化"里程碑)
- **@Deprecated setPort** (NacosAutoServiceRegistration.java:50-53): 端口设置 API 弃用 — 端口仲裁移到 getRegistration (L56-61)
- 注释锚 "it's not real-time" (ServiceCache.java:34-36): 缓存语义官方声明
- NacosDiscoveryInfoChangedEvent @EventListener (NacosAutoServiceRegistration.java:107-110): 发现配置变更 → restart 重注册 — 运行时重配置支持

## 架构递进逻辑

```
2021.0.1.0: 缓存引入 (容灾雏形)
2021.0.1.1: API 规范化 + Smart 刷新 (精细化里程碑)
持续: 条件装配细化 (Blocking/Reactive 分流, watch.enabled 可选)
```
→ 演进方向: 从"SPI 薄实现"到"缓存容错 + 元数据增强 + 事件驱动重注册"的完整发现注册面。
