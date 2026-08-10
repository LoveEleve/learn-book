# ServerBootstrapAcceptor — 新连接的"第一站"

## 概念依赖链

```
Q2 (ServerBootstrapAcceptor) 独立文章

Q2 定义"accept 后新 Channel 怎么配置和注册"
```

## 叙事顺序

1. **问题引入** — 从篇一过渡
   - 篇一讲了 bind() 的组装线——但 bind 后 accept 进来的新连接怎么处理？
   - 每一个新 SocketChannel 都需要自己的 Pipeline、自己的 EventLoop——这些在 accept 的瞬间完成

2. **Q2：ServerBootstrapAcceptor — 新连接的装配工**
   - `ServerBootstrapAcceptor` (`ServerBootstrap.java:189`) — `extends ChannelInboundHandlerAdapter`
   - `channelRead(ctx, msg)` (`ServerBootstrap.java:223-255`): `final Channel child = (Channel) msg` — 新 accepted Channel
   - 步骤 1: `child.pipeline().addLast(childHandler)` — 注入用户定义的 childHandler
   - 步骤 2: `setChannelOptions(child, childOptions)` — 应用 TCP_NODELAY/SO_KEEPALIVE 等
   - 步骤 3: `setAttributes(child, childAttrs)` — 注入 Channel 属性
   - 步骤 4: `childGroup.register(child).addListener(future)` — 注册到 child EventLoopGroup → 成功/失败回调
   - 步骤 5: `ChannelInitializerExtension.postInitializeServerChildChannel(child)` — 扩展钩子
   - **autoread 延迟重开** (issue #1328): `exceptionCaught` → 暂停 accept 1 秒 → `enableAutoReadTask` 恢复
   - `forceClose(child, cause)`: 注册失败 → `child.unsafe().closeForcibly()` — 不泄漏

3. **收束**: ServerBootstrapAcceptor = accept 和 I/O 之间的关卡。一个 Accepted Channel 从裸 Socket 变成完整的 Netty Channel——Pipeline 注入、Options 设置、EventLoop 绑定——全部在 `channelRead` 的几行代码中完成。现在服务器已经能工作了——但网络传来的字节流怎么变成 Java 对象？怎么把 Java 对象变成字节流？引出 Ch10 Codec 框架。

## 核心悬念

**"Netty 没有让 ServerBootstrap 直接处理 accept——而是通过 ServerBootstrapAcceptor 让 EventLoop 在自己的线程中完成新 Channel 的全部初始化。这个分离让并发 accept 零锁——每个 EventLoop 只管自己 accept 到的 Channel。"**
