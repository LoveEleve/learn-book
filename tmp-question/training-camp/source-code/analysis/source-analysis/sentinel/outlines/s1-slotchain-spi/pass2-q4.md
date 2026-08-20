# Pass 2 闭环笔记 Q4: shouldUseContextClassloader — 默认用谁的 ClassLoader?

## 初始假设
- 默认用 TCCL(线程上下文加载器),需显式关闭。
- 实际**相反**:默认**不用** TCCL。

## 验证过程
- 读 `SentinelConfig.java:339-343`: `getConfig(SPI_CLASSLOADER)` == "context"(忽略大小写)才返回 true — **默认 null → false**。
- 读 `SentinelConfig.java:47, 62`: CLASSLOADER_CONTEXT = "context";SPI_CLASSLOADER = `csp.sentinel.spi.classloader`。
- 消费方 `SpiLoader.java:319-327`: false 时用 `service.getClassLoader()`(SPI 接口的加载器),null 再降级 system ClassLoader。
- 语义: 默认 = **SPI 接口类加载器**(与 JDK ServiceLoader 一致);显式 `-Dcsp.sentinel.spi.classloader=context` 才切 TCCL(应对容器/热部署场景)。

## 代码类型
- Glue(类加载策略选择)

## 跨域关联
- S-1 → S-8 适配器: 适配器 jar 由各自加载器加载,SPI 文件跨 jar 合并(load 的 getResources 多 URL 枚举,SpiLoader.java:328-330)

## 结论
默认 service.getClassLoader(),配 `csp.sentinel.spi.classloader=context` 才用 TCCL — 与 JDK SPI 语义对齐,context 模式为容器场景可选(SentinelConfig.java:47,62,339-343 + SpiLoader.java:319-327)。