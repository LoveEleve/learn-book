# 闭环笔记 q5: 过滤元数据 — ConsumerFilterManager/Data 生命周期

## 假设
注册表按 topic 管理; BloomFilter 全局实例; 数据含编译表达式与位图参数。

## 验证过程
- **BloomFilter 实例** (ConsumerFilterManager L57-68): **createByFn(20, 64)** 默认 (f=20% → n=64) — 配置可调 (maxMsgLiveTime 等)
- **注册** (L104-155): 按 (topic, group) → filterDataByTopic 表; **compile (FilterFactory.get(type).compile)** + **bloomFilter.generate(group#topic)** — 每订阅者独立位图参数
- **ConsumerFilterData** (broker/filter): group/topic/expression/expressionType/**compiledExpression (transient)**/bornTime/**deadTime (过期)**/bloomFilterData/clientVersion
- **过期面** (L45-58): deadTime >= bornTime → 过期; **isMsgInLive(msgStoreTime)**: 消息存储时间 < 订阅死期 → 位图判定有效 (消息早于订阅注册则位图无值 — 回退精筛)
- **持久化**: ConsumerFilterManager 继承 ConfigManager (filter.json, RM-5 定时 persist)

## 代码类型
Implementation (元数据管理)

## 跨域关联
- RM-5 (Broker): 装配+定时持久化
- RM-8 (消费): 订阅注册触发

## 结论
过滤元数据 = topic 表 + 全局 BloomFilter (默认 20%/64) + 订阅者数据 (编译表达式+位图参数+存活期); isMsgInLive 控制位图有效窗口。
源码位置: ConsumerFilterManager.java:53-155; ConsumerFilterData.java:32-58
