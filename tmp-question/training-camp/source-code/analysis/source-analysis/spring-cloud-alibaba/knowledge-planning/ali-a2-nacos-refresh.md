# ALI-A2 Nacos 配置动态刷新 — 知识规划 (KP)

> 🔴 A | 模块: spring-alibaba-nacos-config/refresh + annotation (58+176+118+135+793) + starter/refresh + configdata | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 旧轨长轮询监听 | NacosContextRefresher:86-112 | ApplicationReadyEvent → 遍历 Repository 注册 refreshable 监听 |
| 2 | 回调五连 | NacosContextRefresher:117-138 | 计数/历史/快照/事件四动作 + 发布 NacosConfigRefreshEvent |
| 3 | 新轨转发 | NacosConfigRefreshEventListener:52 | NacosConfigRefreshEvent → RefreshEvent (Commons SCC-8) |
| 4 | 新旧仲裁 | NacosPropertySourceRefreshListener:98 | containsBean("nacosConfigSpringCloudRefreshEventListener") 让贤 |
| 5 | 旧轨换源 | NacosPropertySourceRefreshListener:100-112 | build 重建 + target.replace |
| 6 | Smart rebind | SmartConfigurationPropertiesRebinder:88-108 | SPECIFIC_BEAN 前缀匹配 + refreshedSet 去重 |
| 7 | 双源判断 | SmartConfigurationPropertiesRebinder:89-91 | context==source \|\| keys==source 兼容 |
| 8 | 注解注入 | NacosAnnotationProcessor:117-133 | 类/字段/方法三级扫描 |
| 9 | 注解缓存 | NacosAnnotationProcessor:76-108 | groupKeyCache 双检 + refreshed=false 不监听 |
| 10 | 刷新历史 | NacosRefreshHistory:71-77 | MAX 20 环形 + MD5 |

## 02 高频坑

1. 旧轨只换源不发 EnvironmentChangeEvent — @RefreshScope 全量刷新不触发
2. containsBean 仲裁: 装配新轨后旧轨完全让贤
3. Smart rebinder 反射读私有字段 — 升级 Spring 需警惕字段名变化
4. refreshed=false 的 @NacosConfig 注入一次即止
5. @NacosConfigListener 方法必须单参数 (L317 强校验)
6. REFRESH_COUNT 是静态的 — 多 context 共享计数
7. 默认 ALL_BEANS 行为不用 Smart (官方注释)

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 并发 | ready CAS (两监听者) / computeIfAbsent (listenerMap) / 双检 (groupKeyCache) |
| 事件链 | NacosConfigRefreshEvent → RefreshEvent → EnvironmentChangeEvent (三级) |
| 仲裁 | containsBean 装配探测 / @ConditionalOnNonDefaultBehavior / @ConditionalOnMissingBean |
| 粒度 | 全量 rebind (ALL_BEANS) / 前缀精准 (SPECIFIC_BEAN) / key 级注解监听 |
| 可观测 | NacosRefreshHistory (20 条 MD5) / REFRESH_COUNT |
| 兼容 | 旧轨保留 / 反射兼容 / 双源判断向后兼容 |

## 04 跨域桥接

- → SCC-8: RefreshEvent 的发布者实证 (Commons 主源码零发布, 发布者在 Nacos 集成层!)
- → SCC-2: ContextRefresher.refresh 的触发方
- → ALI-A1: putConfigSnapshot 写入方 / refreshCount 消费方 (双向闭环)
- → 面试: "Nacos 配置怎么热刷新" — 事件链 + 双轨 + Smart 精准刷新
