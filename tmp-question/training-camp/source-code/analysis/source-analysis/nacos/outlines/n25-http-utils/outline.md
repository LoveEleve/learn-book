# N-25 HTTP 客户端与工具族 — HttpClientFactory 与执行器/序列化

> 前置: 无 (公共底座) | 对照: 1.x HTTP 时代遗留 + 3.x 通用工具
> 🟡 B | 方案 B (重要域) | 闭环: q1(Http 工厂) q2(执行器) q3(序列化/缓存/任务)

**读者处境**: common/http 的 HttpClientFactory 怎么组织? 执行器 (ThreadPoolManager)? 序列化/缓存/任务引擎?

### 1. HTTP 客户端族 — HttpClientFactory 与 BeanHolder

场景: HTTP 客户端怎么工厂化?
源码路径:
- **HttpClientFactory** (http/HttpClientFactory.java:27) + **AbstractHttpClientFactory** / **AbstractApacheHttpClientFactory** / **DefaultHttpClientFactory**: 工厂族
- **HttpClientBeanHolder** (http/HttpClientBeanHolder.java:35): Bean 持有
- **HttpClientConfig** (http/HttpClientConfig.java:28): 配置
- **HttpUtils/HttpRestResult/Callback/BaseHttpMethod** (http/): 工具面
关键设计 (q1): **"工厂族 = 客户端可替换"** — Apache 实现抽象; BeanHolder 单例持有。 [模式: 工厂族]

### 2. 执行器面 — ThreadPoolManager 与线程工厂

场景: 公共线程池怎么管理?
源码路径:
- **ThreadPoolManager** (executor/ThreadPoolManager.java:41): 线程池管理器
- **NameThreadFactory** (executor/NameThreadFactory.java:29): 命名线程工厂 (全模块使用)
- **ExecutorFactory** (executor/ExecutorFactory.java:41): 工厂
- 消费: NC-1/NC-2 全部定时任务
关键设计 (q2): **"命名线程 = 可观测性"** — NameThreadFactory 统一线程命名 (排障关键)。 [模式: 命名线程]

### 3. 序列化/缓存/任务 — 工具三件

场景: 通用工具面?
源码路径:
- **Serializer 族** (consistency/serialize + common): 序列化
- **Cache 族** (cache/): Cache + builder + decorators + impl — 通用缓存
- **task/engine**: AbstractDelayTask + NacosTaskProcessor + 引擎 (N-15 dump 引擎消费)
- **NacosServiceLoader** (spi/NacosServiceLoader.java:31): SPI 加载器 (全模块 SPI 入口)
关键设计 (q3): **"SPI 加载器 = 插件统一入口"** — 所有 SPI (FailoverDataSource/EventPublisher/ConfigChangeParser) 经 NacosServiceLoader。 [模式: SPI 统一入口]

### 4. 测试与行为锚

场景: 工具边界?
源码路径:
- 测试: common test (ThreadPoolManagerTest 等)
- 锚: NacosServiceLoader 加载语义
关键设计 (q1): **"SPI 统一 = 插件生态"**。 [模式: 插件入口]
