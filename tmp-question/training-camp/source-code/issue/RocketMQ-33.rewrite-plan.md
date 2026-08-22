# RocketMQ-33 重写规划

> 题目：RocketMQ 4.x vs 5.x 为什么不是“多了几个功能”——架构对照总览
> 状态：桥接/对照篇。对应 `hz` 中 4.9.x 与 5.x 架构设计对比主题，用来把前面已拆开的主链（NameServer/Broker/Consumer/Proxy/Pop/DLedger/Controller）重新放回版本演进视角。

## 1. 读者困惑
- RocketMQ 5.x 和 4.x 的区别到底是什么？
- 真的是“多了 Proxy、Pop、Metrics、TieredStore”这么简单吗？
- 哪些变化是功能增量，哪些是架构重心变化？
- 4.x 读法和 5.x 读法为什么不该完全一样？

## 2. 一句话顿悟
**RocketMQ 5.x 相比 4.x 的变化，不只是多了几个功能点，而是把客户端接入、消费确认模型、可观测性与高可用能力重新外显成更明确的宿主边界：Proxy 把接入层收口，Pop 把消费确认语义服务端化，DLedger/Controller 让高可用语义更清晰，Broker 本身则更聚焦核心消息处理与存储。**

## 3. 失败方案推演
- 把 5.x 理解成 4.x + 功能包
- 只记新名词，不看宿主边界的变化
- 继续用 4.x 直连 Broker 心智理解 5.x 接入层

## 4. 章节问题
- 4.x 到 5.x，哪些边界被重新划分了？
- Proxy / Pop / Controller 分别改变了哪条主链？
- 哪些能力是“新增功能”，哪些是“架构重组”？
- 为什么同样叫 RocketMQ，5.x 读法必须更强调接入宿主与服务端确认模型？

## 5. 至少要排除的误解
- 5.x 只是 4.x 多几个模块
- Proxy 是可有可无的转发层
- Pop 只是 Pull 的升级接口
- Controller / DLedger 只是更换实现细节

## 6. 关键证据清单
- 前面 RocketMQ-14/15/30/31/32 等已完成篇目
- `proxy/...`
- `controller/...` and `dledger` related code paths as needed

## 7. 版本与实现边界
- 对比对象：RocketMQ 4.9.x 与 5.x
- 本篇是架构对照，不做逐版本 changelog

## 8. 字数预算
- 7000~10000 字