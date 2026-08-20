# 闭环笔记 q6: 重试过滤与测试面

## 假设
重试消息过滤特例; 测试覆盖解析/表达式/布隆/SPI。

## 验证过程
- **重试过滤** (ExpressionForRetryMessageFilter, 继承 ExpressionMessageFilter): **isMatchedByCommitLog 覆写** (L40-84): 重试消息 (RETRY_TOPIC) — 用**真实 topic 的过滤数据** (realFilterData) 精筛; **CQ 级沿用父类** — 重试消息在 CQ 无位图时回退精筛
- **测试面** (filter 5 文件 1469 行):
  - ParserTest: 合法/非法/十进制溢出/浮点溢出/非法 BETWEEN — **语法边界**
  - ExpressionTest: contains/startsWith/endsWith/has + 否定 — 运算符面
  - BloomFilterTest: equals/hashTo/calcBitPositions/isHit/BloomFilterData/**checkFalseHit** — 数学与误判
  - FilterSpiTest: register/get — SPI 面
  - BitsArrayTest: 位操作
- **broker 测试** (2 文件 660 行): ConsumerFilterManagerTest / MessageStoreWithFilterTest (端到端过滤)
- **配置**: enableCalcFilterBitMap 默认 **false** (BrokerConfig:160) — 位图过滤默认关 (仅 SQL92 订阅时才需开)

## 代码类型
Interface (特例+测试)

## 跨域关联
- RM-8 (消费): 重试消息路径
- RM-5 (Broker): 配置面

## 结论
重试过滤用真实 topic 数据精筛; 测试 7 文件覆盖全机制; enableCalcFilterBitMap 默认关。
源码位置: ExpressionForRetryMessageFilter.java:33-84; filter/src/test 5 文件; BrokerConfig.java:160
