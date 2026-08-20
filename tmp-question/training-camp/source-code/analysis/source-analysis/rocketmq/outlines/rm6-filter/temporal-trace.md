# RM-6 消息过滤 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | **TAG 过滤** (订阅哈希 codeSet + CQ tagsCode 比对) + **FilterServer** (独立过滤进程 — 5.x 废弃, BrokerOuterAPI FilterServerList 残留 L518) |
| 4.x | **SQL92 过滤**: JavaCC 解析 (SelectorParser.jj) + ActiveMQ 风格表达式树 + **布隆两级过滤** (写时位图 CalcBitMap → CQ Ext + 读时粗筛) — 替代 FilterServer 的 broker 内嵌方案 |
| 5.x | **FilterFactory SPI 化** (FilterSpi 注册表, 可扩展类型); ConsumerFilterData 增强 (clientVersion/born-dead 生命周期); ExpressionForRetryMessageFilter (重试精筛); enableCalcFilterBitMap 默认关 |

## 痕迹证据

- ExpressionType.java:37-53: SQL92 语法文档 + TAG 定义 + isTagType
- PropertyExpression.java:23: "taken from ActiveMQ org.apache.activemq.filter" — 移植痕迹
- BloomFilter.java:70-86: 布隆数学注释 (p=e^(-kn/m))
- BloomFilter.java:92-108: Kirsch-Mitzenmacher 论文引用 ("Less Hashing")
- BrokerOuterAPI.java:518: FilterServerList (废弃残留)
- BrokerConfig.java:160: enableCalcFilterBitMap 默认 false
- ConsumerFilterManager.java:57: createByFn(20, 64) 默认参数

## 推断标注

- "3.x TAG+FilterServer" — RocketMQ 公知版本线 (标注)
- "4.x SQL92+布隆" — 特性年代推断 (标注)
- "5.x SPI/生命周期" — 代码结构推断 (标注)
- 未做 git 考古, 版本线为代码结构推断, 已逐条标注
