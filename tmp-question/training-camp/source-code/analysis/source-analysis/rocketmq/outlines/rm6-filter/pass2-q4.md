# 闭环笔记 q4: 写时位图计算 — CommitLogDispatcherCalcBitMap

## 假设
消息落盘时按所有 SQL92 订阅者评估, 命中者哈希入位图 (存 Ext)。

## 验证过程
- **触发** (dispatch L55-57): **enableCalcFilterBitMap 配置** + topic 有 SQL92 订阅者 (ConsumerFilterManager.get)
- **计算** (L65-100): 新建 BitsArray (m 位) → 逐订阅者: compiledExpression.evaluate (MessageEvaluationContext) → **布尔真 → bloomFilter.hashTo 置位** (订阅者位图数据 hashTo)
- **存储**: request.setBitMap(bytes) → DispatchRequest → RM-3 CQ Ext (filterBitMap 字段)
- **读取面**: 消费拉取时 ExpressionMessageFilter.isMatchedByConsumeQueue 读 Ext 位图 → 布隆命中判定
- **配置开关**: enableCalcFilterBitMap (默认? 需验证)
- **失败容错**: 无编译表达式/无布隆数据 → [BUG] 日志跳过; 评估异常 → 跳过 (位图不置位 → 该消息对所有订阅者"未命中" — 走精筛兜底?)

## 代码类型
Implementation (写路径位图)

## 跨域关联
- RM-3 (CQ Ext): filterBitMap 存储
- RM-5 (Broker): 分发链前置 (addFirst)

## 结论
写时位图 = 消息落盘时按 SQL92 订阅者评估 → 命中置位 → 存 Ext; 读时布隆命中粗筛; 开关 enableCalcFilterBitMap。
源码位置: CommitLogDispatcherCalcBitMap.java:35-100
