# ChannelHandlerMask — 编译器视角的 Handler 优化

## 概念依赖链

```
Q4 (HandlerMask 检测) 独立文章

Q4 定义"Netty 怎么知道一个 handler 有哪些方法" — 编译期 vs 运行期的选择
```

## 叙事顺序

1. **问题引入** — 从篇一篇二过渡
   - 两篇讲了 Inbound/Outbound 传播——但每个 fireChannelRead 都要 `findContextInbound` 跳到下一个
   - 问题: 怎么知道"下一个" handler 有没有实现 channelRead？逐个反射检查？— ChannelHandlerMask 在**编译期**解决

2. **Q4：ChannelHandlerMask — 编译期检测**
   - `ChannelHandlerMask.java:44-63`: MASK_ONLY_INBOUND 9 位 (channelRegistered→channelWritabilityChanged)
   - MASK_ONLY_OUTBOUND 8 位 (bind→close)
   - `isSkippable()` (`ChannelHandlerMask.java:97-112`): 检查 handlerClass 的每个 @Skip 注解方法 — 反射读取但只执行一次
   - 检测发生在 `AbstractChannelHandlerContext` 构造函数中 (`ChannelHandlerMask.mask(handler.getClass())`)
   - mask=0 → ChannelHandlerAdapter (覆盖所有方法为空实现) — `skipContext()` 快速跳过不实现具体逻辑的 handler
   - `findContextInbound(MASK_CHANNEL_READ)`: 从当前节点向前 → 跳过 mask & MASK_CHANNEL_READ == 0 的节点

3. **实际效果**:
   - 一个只实现 channelRead 的 Inbound handler: mask = MASK_CHANNEL_READ — inbound 传播只需要找到它
   - 一个没有任何重写的 Adapter: mask = 0 — inbound + outbound 都被跳过
   - HeadContext/TailContext: mask 包含了全部位 — inbound/outbound 两者都不跳过

4. **收束**: HandlerMask 是 Netty 的编译期多方法分发——反射一次，每次传播都省 O(N×M) 的遍历。Handler 链的反射优化已经做到极致了——但每个 Handler 里创建 ByteBuf 时，内存从哪来？不是每次 malloc/free 吧？内存怎么池化的？引出 Ch8 内存池化。

## 核心悬念

**"Netty 没有用接口判断 handler 类型——而是用编译期反射 + 位掩码。一个 handler 可以既是 Inbound 又是 Outbound，传播决定于它覆盖了哪些具体方法。"**
