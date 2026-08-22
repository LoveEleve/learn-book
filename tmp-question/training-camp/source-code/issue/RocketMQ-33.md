# RocketMQ-33. RocketMQ 4.x vs 5.x 为什么不是“多了几个功能”——架构对照总览

> 场景：当我们把 RocketMQ 的 Producer、Broker、CommitLog、Consumer、DLedger、Controller、Proxy、Pop 一路拆开以后，一个更大的问题就会自然浮出来：RocketMQ 5.x 和 4.x 的差异，到底只是“多了几个新功能”，还是整个系统的关注点和宿主边界都发生了变化？如果只是把 5.x 看成 4.x 再外挂几个模块，就很难真正读懂 Proxy、Pop、Controller 为什么会站到台前。本篇把前面分散的知识重新放回版本演进的总图里对照一遍。

## 先把真正的困惑摆出来：5.x 真的只是 4.x 加了 Proxy、Pop、TieredStore 吗

表面上看，5.x 和 4.x 很容易被总结成：

- 多了 Proxy；
- 多了 Pop；
- 多了 Metrics / TieredStore；
- 多了 Controller 之类的新能力。

但如果仅仅停在“多了什么”，会漏掉更关键的一层：**这些新增能力为什么要被单独长出来，它们到底改变了原有主链的哪一段宿主边界。**

*关键设计（斜体）：* *RocketMQ 5.x 相比 4.x 的变化，不只是功能数量增加，而是把若干过去隐含在客户端/Broker 里的复杂语义显式外提成独立宿主边界：Proxy 把客户端接入层收口，Pop 把消费确认模型服务端化，Controller / DLedger 让高可用裁决与复制语义更明确，Broker 本身则更聚焦消息存储与核心处理。理解 5.x 的关键不是记住新名词，而是看清“复杂度被从哪里抽出来，重新放到哪里”。*[模式: 4.x 隐含复杂度 + 5.x 显式宿主边界 + 主链重组]

## 第一层：4.x 的默认心智更像“客户端直连 Broker”

如果用最粗的一句话概括 4.x 的主流阅读方式，它更像：

- Producer 直连 Broker 发送；
- Consumer 直连 Broker 拉消息；
- NameServer 提供路由；
- Broker 承担大量接入、消费、存储、复制相关语义。

也就是说，在 4.x 的直觉心智里，Broker 是系统里那个特别“厚”的宿主，很多复杂度天然都堆在它和客户端直连链路之间。

这并不代表 4.x 设计差，而是说明：**4.x 更多沿着“Broker 为中心”的主机房布局来组织复杂度。**

## 第二层：5.x 的第一大变化，是把“客户端接入层”从 Broker 边上抽出来

到了 5.x，Proxy 的出现不是偶然功能包，而是一次更显式的边界重划：

- 4.x 那种“客户端直连 Broker”主链并没有被宣布作废；
- 但在 5.x 里，接入协议、接入状态、路由代理、Broker 访问代理被更明确地收口到 Proxy；
- Broker 因此可以更聚焦消息处理与存储本体。

这不是单纯“多一跳”，而是：

> **原来更多隐含在客户端直连链路里的接入复杂度，被 5.x 更显式地抽成了一个宿主。**

所以读 5.x 时，如果还只用 4.x 那种“客户端基本直接理解 Broker 世界”的单一路径心智，就会一直觉得 Proxy 像多余中间层。

## 第三层：5.x 的第二大变化，是把消费确认模型服务端化

4.x 的传统消费心智更接近：

- 客户端拉到消息；
- 客户端本地消费推进；
- Broker 提供基础 offset 与拉取能力。

而到了 5.x，Pop 把这层关系改写了。

因为 Pop 不只是“拉消息 API 换个样子”，它要求服务端继续持有：

- 谁拿走了消息；
- invisibility 窗口多长；
- ack 是否到达；
- 超时后是否 revive / 重投 / 死信。

这意味着原本更多由客户端单边承受的消费确认语义，被往服务端拉了一层。

所以 5.x 的 Pop 不是附加能力，而是**消费模型边界的重划**。

## 第四层：高可用也从“复制实现”逐渐变成“裁决边界更清晰”

在前面的篇目里我们已经拆过：

- `RocketMQ-13`：传统 Master/Slave 复制
- `RocketMQ-14`：DLedgerCommitLog
- `RocketMQ-15`：Controller / 选主 / 角色切换

把它们放回 4.x vs 5.x 视角，就会发现一个明显趋势：

- 4.x 更容易让人把高可用理解成“Broker 主从复制怎么做”；
- 5.x 则把“数据复制语义”和“控制面裁决语义”拆得更显式、更适合单独理解，而不是说 4.x 完全没有这层心智。

这就是为什么 DLedger 和 Controller 不该被理解成“同一个升级包”。它们分别在回答：

- 日志复制怎么保证；
- 角色切换与谁能继续写由谁裁决。

所以 5.x 不是单纯把复制做强，而是**把高可用内部的两种复杂度拆开、显化。**

## 第五层：5.x 的变化，既有“边界重组”，也有“能力外显”

如果只按模块名字看，Metrics、TieredStore、Proxy、Pop、Controller 都像是“又多了几个大功能”。

但从架构视角看，这里面其实混着两类变化：

### 1) 更偏“边界重组”的变化
- Proxy：把客户端接入层显式收口；
- Pop：把消费确认模型更明确地服务端化；
- Controller / DLedger：把高可用内部的复制与裁决边界拆得更清楚。

### 2) 更偏“能力外显”的变化
- Metrics：把可观测性更明确地提升为一等能力；
- TieredStore：把存储/计算分层与冷热分离能力更显式地模块化。

也就是说，5.x 的核心特征并不只是“功能变多”，而是：**一部分复杂度被重组边界，一部分能力被显式外提。**

## 第六层：为什么 4.x 和 5.x 的阅读顺序也应该不同

这也是对学习方法最有价值的一点。

### 读 4.x 时，更自然的主链是：
- Producer/Broker/CommitLog/ConsumeQueue/Consumer
- 再补主从复制、事务、延迟、过滤

因为 4.x 的系统重心更容易沿“Broker 核心能力”展开。

### 读 5.x 时，更自然的主链则变成：
- 先看 Broker/存储主链仍然成立；
- 但必须提前把 Proxy、Pop、Controller 放进整体视角；
- 否则你会一直误把它们当附录模块。

所以 5.x 不只是功能更多，而是**阅读路径也被重排了**。

## 第七层：把 4.x 与 5.x 的差异压成一句话，就是“复杂度显式重组”

如果只记一件事，那就是：

- 4.x：更多复杂度隐含在 Broker 中心模型里；
- 5.x：更多复杂度被显式拆出成独立宿主边界。

Proxy、Pop、Controller 都是这种“显式重组”的代表。它们不是因为 4.x 做不到，而是因为 5.x 选择把那些复杂度从默认隐式位置拿出来，放到更清楚的模块边界上。

## 收网：RocketMQ 5.x 的关键，不是多功能，而是边界重划

把整篇压成一句话：RocketMQ 5.x 相比 4.x 的核心变化，不在于“多了几个新功能”，而在于它把客户端接入、消费确认模型、高可用裁决、可观测性与存储分层中的若干复杂度显式抽出，重组为更清楚的宿主边界；Proxy 不是可有可无的中间层，Pop 不是 Pull 的变体，Controller 也不只是实现细节升级。理解 5.x，关键是理解这些复杂度从哪里被搬出来，又被安放到了哪里。

```text
4.x
  → 客户端直连 Broker 心智更强
  → 复杂度更多隐含在 Broker 中心模型里

5.x
  → Proxy 收口接入层
  → Pop 收口服务端确认模型
  → Controller / DLedger 明确高可用边界
  → 更多能力显式模块化
```

**本篇的一句话困惑**：RocketMQ 5.x 和 4.x 的区别，真的只是“多了几个新功能模块”吗？

**本篇的一句话顿悟**：5.x 真正改变的不是功能数量，而是宿主边界：接入层、消费确认、高可用裁决等复杂度被从 4.x 的隐式位置中抽出来，变成了更清楚的显式模块。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“5.x 只是 4.x 多几个模块。”** 更关键的是复杂度和宿主边界被重划了。
2. **“Proxy 是可有可无的转发层。”** 它重构了客户端接入边界。
3. **“Pop 只是 Pull 的升级接口。”** 它重构了消费确认模型。
4. **“Controller / DLedger 只是实现细节升级。”** 它们把高可用内部的不同复杂度拆开了。
5. **“4.x 和 5.x 读法一样。”** 5.x 必须更早把 Proxy/Pop/Controller 纳入主链视角。

### 关键证据清单

- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-14.md:1`：DLedgerCommitLog。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-15.md:1`：Controller / 选主 / 角色切换。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-30.md:1`：Proxy 总览。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-31.md:1`：Pop 总览。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-32.md:1`：Ack/ck/revive 闭环。
- `proxy/src/main/java/org/apache/rocketmq/proxy/ProxyStartup.java`：5.x Proxy 宿主入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/PopMessageProcessor.java`：5.x Pop 取消息入口。
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java`：5.x Controller 宿主入口。

### 版本与实现边界

- 对比对象以 RocketMQ `4.9.x` 与 `5.x` 为主。
- 本篇是架构对照，不展开逐版本 changelog。
- 本篇关注宿主边界与主链重组，不替代各个单篇细节。

### 前置依赖与后续桥接

- 前置依赖：建议先读 `RocketMQ-13/14/15/30/31/32`。
- 后续桥接：下一篇可继续补 `RocketMQ-34`（为什么 RocketMQ 性能不如 Kafka）。