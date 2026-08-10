## Loop Note: Q10 — IoHandle/IoOps/IoEvent 事件分发模型

**Hypothesis**: IoHandle.handle(IoRegistration, IoEvent) 是 Channel 接收 I/O 事件的统一入口——NioSelectableChannelIoHandle 将 NIO SelectionKey 的就绪操作映射为 IoEvent，通过 IoRegistration.submit(IoOps) 提交新的关注事件。

**Verification**:
- `IoHandle.java:27,36` — `interface IoHandle extends AutoCloseable`: `handle(IoRegistration, IoEvent)` — Channel 实现此接口接收事件
- `IoRegistration.java:22` — `interface IoRegistration`: `submit(IoOps)` — 提交新的 I/O 操作关注
- `IoOps.java:24` — `interface IoOps` — I/O 操作标记
- `IoEvent.java:23` — `interface IoEvent` — I/O 事件的表示
- `NioIoHandler.processSelectedKey()` (`line 586-596`) — `key.attachment()` 取到的是 `DefaultNioRegistration`（`NioIoHandler.java:318`），它封装了 IoHandle 引用 + 有效性检查 + readyOps→IoEvents 转换
- `DefaultNioRegistration.handle(int ready)` (`line 384-389`) — `NioIoOps.eventOf(ready)` 将 SelectionKey 的 readyOps 位掩码转换为 IoEvents，然后调用 `IoHandle.handle(this, ioEvent)`
- 完整分发链: `processSelectedKey → key.attachment()=DefaultNioRegistration → DefaultNioRegistration.handle(readyOps) → NioIoOps.eventOf → IoHandle.handle(registration, ioEvent)`

**Code type**: Glue (event dispatch)

**关键设计**: IoHandler 不知道 Channel 的存在——它只知道 IoHandle。注册是 IoHandle → IoHandler.register()。事件分发是 IoHandler → IoHandle.handle()。这是 IoHandler 独立于 Channel 的关键抽象层。

**Conclusion**: IoHandler 的 I/O 模型 = `IoHandle`(设备抽象) + `IoOps`(操作标志) + `IoEvent`(事件对象)。NioIoHandler 在 NIO Selector 层实现这组接口——Selector 的 selectedKeys 被翻译为 IoEvent 再分发到 IoHandle.handle。source: IoHandle.java:27-60, IoOps.java:24, IoHandler.java:20-33
