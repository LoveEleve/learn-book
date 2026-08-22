# RocketMQ-35 重写规划

> 题目：Metrics 为什么不是“打几个指标”——RocketMQ 可观测性主链
> 状态：5.x 能力篇。对应 `hz` 中 4.x 与 5.x Metrics 实现原理主题，把 Metrics 放回 RocketMQ 的宿主边界与运行时主链里。

## 1. 读者困惑
- RocketMQ 的 Metrics 到底只是多打几条监控吗？
- 4.x 和 5.x 在可观测性上为什么差异这么大？
- Metrics 为什么要和 Proxy、Broker、消费模型一起看？
- 指标采集只是运维问题，还是会反映架构边界变化？

## 2. 一句话顿悟
**RocketMQ 5.x 的 Metrics 不是“给现有流程补点监控”，而是把原来散落在 Broker/客户端/接入层里的运行状态，显式变成可度量的宿主能力。可观测性在 5.x 里不再只是附属品，而是接入层、消费模型、路由与存储边界被重新组织后的自然结果。**

## 3. 失败方案推演
- 把 Metrics 理解成运维附录
- 只看埋点数量，不看采集边界和宿主位置
- 不区分 4.x 的零散监控与 5.x 的显式可观测能力

## 4. 章节问题
- 4.x 和 5.x 的 Metrics 差别到底在哪？
- 为什么可观测性会和 Proxy / Pop / Broker 宿主边界相关？
- 指标到底反映了哪些运行时状态？
- Metrics 为什么也是架构演进的一部分？

## 5. 至少要排除的误解
- Metrics 就是多打几个 counter
- 可观测性不影响架构理解
- 4.x 和 5.x 只是指标名字不同
- Proxy/Pop 与 Metrics 无关

## 6. 关键证据清单
- `proxy/.../metrics`
- `broker/...` metrics related classes
- `common/remoting` observation points as needed
- 前面 RocketMQ-30/31/33 作为上下文

## 7. 版本与实现边界
- 对比对象以 RocketMQ 4.x / 5.x 为主
- 本篇是可观测性总览，不做某个监控平台接入教程

## 8. 字数预算
- 6000~9000 字