# G-3 temporal-trace — 时空溯源 (🔴 域)

> 浅克隆无 git 历史, 无 CHANGELOG — 溯源用代码内 @since/@ExperimentalApi + 注释痕迹。

## 演进时间线

| 版本线索 | 证据 | 意义 |
|---|---|---|
| 1.0.0 | ClientCallImpl/ManagedChannelImpl 核心无 @since (内部类) | 客户端骨架首发即定 |
| 1.24.0 | Deadline.getSystemTicker @since (Deadline.java:57) | Ticker 抽象公开化 (测试时钟注入) |
| 1.26.0+ | AbstractStub.newStub/StubFactory (G-1 溯源) | stub 工厂化同代演进 |
| 1.46.0+ | onReadyThreshold 背压细化 | 流控演进 |
| 1.60.0+ | JumpToApplicationThreadServerStreamListener (服务端) 同代 | 双执行器架构成熟 |
| 1.83.1 | syncContext 全面注释 "Must be accessed from" | 单线程模型是长期沉淀 |

## 架构稳定性判断

**核心十年未变**: ManagedChannelImpl (生命周期) + ClientCallImpl (状态机) + Context/Deadline — 客户端骨架 1.0 定版; **演进在边缘**: Ticker 注入 (测试性)、背压阈值、双执行器。

**关键演进痕迹**:
- `Context.current()` 的 ROOT 兜底 (Context.java:171-176) — 无上下文调用安全
- "This is an optimization for the case (typically with InProcessTransport)" (ManagedChannelImpl.java:872-874) — 内联优化是后期补的性能路径
- PendingCall/ConfigSelector 体系 — 服务配置 (service config) 加入后的调用路由演进

## 写书建议

客户端域按"骨架 + 优化演进"呈现: 核心 (syncContext/生命周期/状态机/Context/Deadline) 是 1.0 设计, 优化路径 (newCall 内联/PendingCall 缓冲/ConfigSelector) 标演进 — 体现"先正确后快"的演化轨迹。
