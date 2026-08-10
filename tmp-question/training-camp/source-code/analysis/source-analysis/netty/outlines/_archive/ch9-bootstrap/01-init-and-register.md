# Bootstrap 核心 — Netty 的组装工厂

## 概念依赖链

```
Q1 (initAndRegister 组装链) → Q3 (Bootstrap vs ServerBootstrap)

Q1 定义"组件怎么连接" → Q3 回答"客户端和服务端两种模式"
```

## 叙事顺序

1. **问题引入** — 从 Ch8 内存池化过渡
   - 前五章讲了 ByteBuf、EventLoop、Promise、Pipeline、内存池——每一个都是独立的精妙设计
   - 但这些是零件——怎么组装成能 `bind(8080)` 的服务？

2. **Q1：initAndRegister — 五步组装线**
   - `bind(localAddress)` → `doBind()` → `initAndRegister()` (`AbstractBootstrap.java:324-354`)
   - 步骤 1: `channelFactory.newChannel()` — 通过反射或工厂创建 Channel 实例 (`ReflectiveChannelFactory`)
   - 步骤 2: `init(channel)` (Template Method) — `AbstractBootstrap` 声明抽象, `ServerBootstrap/Bootstrap` 各自实现
   - 步骤 3: `config().group().register(channel)` — 将 Channel 注册到 EventLoop → 返回 `ChannelFuture`
   - 步骤 4: `doBind0(regFuture, channel, localAddress, promise)` (`AbstractBootstrap.java:371`) — 注册完成后异步绑定
   - 异常路径: 任一步失败 → `channel.unsafe().closeForcibly()` — 不泄漏半初始化的 Channel

3. **Q3：Bootstrap vs ServerBootstrap**
   - `Bootstrap` (client): `connect()` 替代 `bind()` — 客户端单连接 + 一个 Channel
   - `ServerBootstrap`: `bind()` → accept → 每个新连接 = 一个新的 child Channel
   - `handler()` vs `childHandler()`: server 的 handler 在 server socket Channel; childHandler 在每个 accepted Channel
   - `group()` vs `group(parentGroup, childGroup)`: server 可以指定不同的 EventLoopGroup 用于 accept 和 child I/O
   - `ChannelInitializer` + `ChannelInitializerExtension` — 初始化钩子

4. **收束**: Bootstrap = Netty 的依赖注入框架——Builder 模式串起 group/channel/handler/options/attrs。`init()` 模板方法 + `initAndRegister()` 组装线 = 所有组件在正确的时刻连接。

## 核心悬念

**"ServerBootstrap 不自己打开端口——它创建 Channel、绑定 EventLoop、注入 Handler，然后把地址告诉 Channel 让它去 bind。"**
