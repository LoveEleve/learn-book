# RM-7 Producer 发送 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | DefaultMQProducer 骨架: send 三模式 + 轮询选择 (sendWhichQueue) + SYNC 重试 2 次 + sendKernelImpl (头构造+hook+invoke) |
| 4.x | **MQFaultStrategy** (7 档延迟→冷却映射 + availableFilter 故障过滤); selector 家族 (hash/机房间/随机); retryResponseCodes 白名单 |
| 5.x | **ProduceAccumulator** (批量累积: 延迟 1ms-30s/字节 1B-2MB 双条件); **ServiceDetector** (服务探测 — reachableFlag 双维度, startDetectorEnable); Request-Reply (RequestResponseFuture) |

## 痕迹证据

- DefaultMQProducerImpl.java:733-880: sendDefaultImpl 重试编排 (异常三分类注释风格)
- MQFaultStrategy.java:29-30: 7 档映射常量 (4.x 起)
- MQFaultStrategy.java:174: isolation 固定 10000ms
- LatencyFaultToleranceImpl.java:73-75: reachableFlag 恢复 ("is reachable now")
- ProduceAccumulator.java:168-181: 批量双条件 (5.x)
- DefaultMQProducer.java:127-139: 重试配置

## 推断标注

- "3.x 骨架" — RocketMQ 公知版本线 (标注)
- "4.x 故障策略/选择器" — 特性年代推断 (标注)
- "5.x 累积/探测/请求响应" — 与 5.x 同代推断 (标注)
- 未做 git 考古, 版本线为代码结构推断, 已逐条标注
