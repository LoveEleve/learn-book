# K-1 Producer — 六层深审 REVIEW 记录 (2026-08-15)

> 审查方法: 07 五维度 + 逐锚点核对 + 裸行号三形态扫描 + 行号上限检查 + 跨域引用核验 + MCP 语义工具 (trace_path)
> **结论: 深审通过 (裸锚点 30+ 处根治, 三形态零残留; 上限 6 文件全过; 跨域 4 目录 [ -d ] 全过)**

## 第一层: 锚点验证 (写时即 grep/Read + trace_path)

- KafkaProducer.java: send KafkaProducer.java:L940-945 / doSend KafkaProducer.java:L974-1075 / 关闭检查 KafkaProducer.java:L976 / AppendCallbacks KafkaProducer.java:L978-980 / waitOnMetadata KafkaProducer.java:L990 / 序列化 KafkaProducer.java:L1004-1016 / partition KafkaProducer.java:L1021 / ensureValidRecordSize KafkaProducer.java:L1031 / append KafkaProducer.java:L1036 / wakeup KafkaProducer.java:L1043-1045 / ApiException KafkaProducer.java:L1050-1059 / onSendError KafkaProducer.java:L1052-1058 / maybeTransitionToErrorState KafkaProducer.java:L1056 / 其他异常 KafkaProducer.java:L1060-1074 ✅
- RecordAccumulator.java: append RecordAccumulator.java:L275-356 / topicInfoMap RecordAccumulator.java:L276 / 粘性 RecordAccumulator.java:L300-302 / Deque RecordAccumulator.java:L308 / tryAppend RecordAccumulator.java:L319 / 切换 RecordAccumulator.java:L321-324 / 新批大小 RecordAccumulator.java:L327-329 / allocate RecordAccumulator.java:L330-333 / appendNewBatch RecordAccumulator.java:L345 / updatePartitionInfo RecordAccumulator.java:L351 ✅
- BufferPool.java: 注释 L39-41 / 字段 L49-53 / 构造 L70-75 ✅
- Sender.java: deallocate Sender.java:L174 / delivery timeout Sender.java:L195 / run Sender.java:L241-258 / runOnce Sender.java:L344-345 / sendProducerData Sender.java:L379-382 / ready Sender.java:L382 ✅
- BuiltInPartitioner.java: 类 L39 / stickyBatchSize L42 / 校验 L52-58 / murmur2 BuiltInPartitioner.java:L330 ✅
- ProducerConfig.java: buffer.memory=32MB ProducerConfig.java:L381 / batch.size=16KB ProducerConfig.java:L393 ✅
- DelayedProduce.scala:89-116 (K-7 衔接) ✅

## 第二层: 机制实证 (全过)

- doSend 六步链 (KafkaProducer.java:L974-1075) / 批聚合双路径 (RecordAccumulator.java:L319,345) / BufferPool 回收 (BufferPool.java:L39-75) / Sender 主循环 (Sender.java:L241-382) ✅
- trace_path 实证: doSend callees 含 waitOnMetadata/serialize/partition/append/Sender.wakeup/TransactionManager.maybeAddPartition ✅

## 第三层: 编造检查 (零)

- 全部锚点写时 grep/Read; 数值 (32MB/16KB/murmur2) 源码实证 ✅

## 第四层: 覆盖缺口 (07 五维度 R3/R4/R5)

- R3: 规划 3 数值断言 (32MB/16KB/murmur2) 验证正确但大纲未覆盖 → 补 02-L3 (ProducerConfig.java:381,393) + 01-L4 (BuiltInPartitioner.java:330) ✅
- R4: 回调链仅 1 处 → 补 01-KafkaProducer.java:L2 (AppendCallbacks KafkaProducer.java:L978-980 + onSendError KafkaProducer.java:L1052-1058) ✅
- R5: 负面空间 3 篇全 0 → 补 3 处 (不做同步阻塞/不做每消息发送/不做调用线程发送) ✅

## 第五层: 裸行号

- 写时 30+ 处 (5 文件混合) → awk 精确行号集合根治 + 4 处文字参数形手动修 → **0 残留**
- 上限: 6 文件全 OK

## 第六层: 跨域引用核验

- r24-string / rd4-command / rm7-producer / k7-purgatory (4 目录 [ -d ]) ✅
- **rm7-producer 对照真实性**: MQFaultStrategy 在 RM-7 outline/pass1 实证 (6 处) — K-12 编造对照教训未重犯 ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-15) — 修复后复核

### 0 处新偏差

- 三形态裸锚点全零; 锚点密度 12/18/11 (🔴A ≥8 全达标)
- 语义复核: doSend 链 / 双路径 / BufferPool / Sender 循环 — 与源码一致
- R3/R4/R5 回补项锚点实证 (ProducerConfig.java:381,393 / KafkaProducer.java:978-980 / BuiltInPartitioner.java:330)

### 结论

K-1 三遍验证闭环: 写时 grep + trace_path → 自查 → 复审通过; 修复全为格式类 + 覆盖补全 (R3/R4/R5)。K-1 大纲层交付完成 (待用户确认后进入时空溯源/harness/completeness)。

---

## 第三轮复审 (REVIEW-3, 2026-08-15) — 全流程交付后收官

> 审查对象: 大纲确认后新增的时空溯源/harness/completeness + 全文件裸锚点终审

### 时空溯源 ✅

- 2011 初始 (Producer.scala) → 2014-02-06 Java 异步化 (KafkaProducer 四件套) → 2017-04-27 KAFKA-4818 幂等事务 → 2019-08-01 KIP-480 粘性分区 — 全部 commit 日期实证

### harness ✅ (13/13 ALL PASS)

- MiniKafkaProducer: 异步流水线/批聚合双路径/内存池回收/粘性分区/key 哈希/acks 语义
- 抓 2 处自身缺陷: ①computeIfAbsent 内重复 put (CME) ②测试预期错 (256 内存能容 2×128) — 验证 tryAppend/allocate 双路径语义

### completeness ✅ (30 问, 29✅ 1⚠️)

- ⚠️ linger.ms 未覆盖 → 已回补 02-L2 (设计文档 10ms 示例)

### 裸锚点终审 (本次最大修复)

- pass1/pass2/knowledge-planning/review-notes/completeness 共 ~100 处裸锚点 (上次只修了 3 篇大纲) → awk 归属修复 + 40 处文字形手动修 → **三形态零残留**
- **抓到 1 处 awk 归属错误**: client.poll 的 Sender.java:L345 被 awk 判给 RecordAccumulator (行号集合冲突: 345 同时属 RA appendNewBatch 与 Sender runOnce) — 交叉验证 Sender.java:345=client.poll 修正 — **K-4/K-7 教训第四次重现: 批量修复后必须逐处人工复核归属**

### 收敛判定

K-1 全流程交付完成: 时空溯源 + harness 13/13 + completeness 30 问 + 三形态零残留 + 07 五维度 (R3/R4/R5 三处覆盖补全)。K-1 收官。

---

## 第四轮复审 (REVIEW-4, 2026-08-15) — 07 五维度深度收官

### R1 维度1 (桥+结构): 0 发现, 收敛

- 四行双链 + 桥链 01→02→03 + 零反模式

### R2 维度2 (锚点密度): 0 发现, 收敛

- 13/20/11 (🔴A ≥8 全达标); 上限全过

### R3 维度3 (数值复核): **1 发现, 1 修复** ⚠️⚠️

- **发现 (编造级数值错误)**: linger.ms 表述 "设计文档 10ms 示例" — 实际默认 **5ms** (ProducerConfig.java:397), 10ms 是 2011 年历史示例非当前默认
- **修复**: 02-L2 + completeness 回补项同步修正 (默认 5ms L397 实证) — 历史文档示例被当默认值引用 (编造类错误第 2 次: K-4 256KB→2MB 后)

### R4 维度4 (横切: 重试语义): **1 发现, 1 修复** ⚠️

- **发现**: 重试横切 0 处 — Producer 重试是面试高频 (重试/幂等/乱序三角)
- **修复**: 03-L2 补 canRetry 链 (Sender.java:691,875-881: RetriableException + delivery.timeout 双限) + retries 默认 Integer.MAX_VALUE (ProducerConfig.java:382)

### R5 维度5 (负面+开篇): 0 发现, 收敛

- 负面声明 1/1/1; 开篇词 6

### 内容深度轮: 0 发现

- 反写测试: 12 场景, 只读大纲可写文章 ✅
- harness 对照审计: 16 处源码对照锚点, 注释声明与源码一致 ✅
- completeness 30/30 全 ✅
- 时空溯源断代复核: 4 个 commit 日期全部实证 ✅

### 收敛判定

R1/R2/R5 + 内容深度轮收敛; R3 抓 1 编造级数值错误 (linger 5ms) + R4 抓 1 横切缺口 (重试) — 修复后三形态零残留。K-1 深度收官完成。
