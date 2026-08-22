# RocketMQ-31 重写规划

> 题目：Pop 消费为什么不是“Pull 换个接口”——Pop 主链总览
> 状态：5.x 特色骨架篇。对应 `cqfy` 中 PopMessageProcessor、PopLongPollingService、ck/ack/revive 一系列主题的总览篇，先把 Pop 放回全局消费模型里。

## 1. 读者困惑
- Pop 消费是不是只是 Pull 换了个 API？
- 为什么 Pop 会引出 ack、ck、revive、死信这些额外概念？
- Pop 和传统 Push/Pull/LitePull 的核心差异是什么？
- Proxy 为什么会和 Pop 绑定得这么紧？

## 2. 一句话顿悟
**Pop 不是“拉消息接口换个名字”，而是 RocketMQ 5.x 在消费确认模型上的一次重构：Broker/Proxy 不再只负责把消息给你，还要跟踪 invisibility、ack、ck、超时恢复与死信转发。Pop 的主链因此天然比传统 Pull 多出一整套确认与恢复闭环。**

## 3. 失败方案推演
- 把 Pop 理解成更快的 Pull
- 只看拿消息，不看 ack/ck/revive
- 把 Pop 和 Proxy/Broker 的宿主职责拆开看

## 4. 章节问题
- Pop 相比 Pull 到底变了什么？
- ack、ck、revive 分别补的是什么语义洞？
- PopLongPollingService、PopMessageProcessor 在主链里扮演什么角色？
- Pop 为什么天然会把确认模型推到服务端？

## 5. 至少要排除的误解
- Pop = Pull + 长轮询
- Pop 成功拿到消息就等于完成消费
- ack 只是个普通回执
- revive 只是失败重试小补丁

## 6. 关键证据清单
- `broker/.../processor/PopMessageProcessor`
- `broker/.../longpolling/PopLongPollingService`
- `broker/.../offset/ConsumerOffsetManager` and related pop managers
- `proxy/...` pop related entry if relevant

## 7. 版本与实现边界
- RocketMQ 5.x 为主
- 本篇是 Pop 总览，不展开 ack/ck/revive 细节（留给 RocketMQ-32）

## 8. 字数预算
- 7000~10000 字