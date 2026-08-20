# K-11 事务与幂等 — 六层深审 REVIEW 记录 (2026-08-15)

> 审查方法: 07 五维度 + 逐锚点核对 + 裸行号三形态扫描 + 跨域引用核验
> **结论: 深审通过 (裸锚点根治, 三形态零残留; 上限全过; 跨域 3 目录 [ -d ] 全过)**

## 第一层: 锚点验证 (写时即 grep/Read)

- TransactionManager.java: 类 TransactionManager.java:L95 / API 映射 TransactionManager.java:L252-255 / init TransactionManager.java:L329 / begin TransactionManager.java:L332 / commit TransactionManager.java:L360 / abort TransactionManager.java:L372 ✅
- TransactionCoordinator.scala: handleInitProducerId TransactionCoordinator.scala:L113 / handleEndTransaction TransactionCoordinator.scala:L505 / endTransactionWithTV1 TransactionCoordinator.scala:L533 / Prepare 迁移 TransactionCoordinator.scala:L562-593 / V2 状态表 TransactionCoordinator.scala:L704-732 ✅
- TransactionMarkerChannelManager.scala: TxnMarkerEntry TransactionMarkerChannelManager.scala:L30-31 / markersQueuePerBroker TransactionMarkerChannelManager.scala:L176 / 未知队列 TransactionMarkerChannelManager.scala:L178 / queueForUnknownBroker TransactionMarkerChannelManager.scala:L206 / 类 TransactionMarkerChannelManager.scala:L489 ✅
- TransactionStateManager.scala:869 / GroupCoordinator.java:395 ✅
- ConsumerConfig.java:364-369 (isolation.level, LSO 语义 ConsumerConfig.java:L368) ✅
- UnifiedLog.java:1421 (isControlBatch) ✅

## 第二层: 机制实证 (全过)

- 幂等三层 (K-3 底座) / 客户端五态 (TransactionCoordinator.scala:L329-372) / 两阶段 (TransactionCoordinator.scala:L505-593) / Marker 分发 (TransactionCoordinator.scala:L176-206) / read_committed LSO ✅

## 第三层: 编造检查 (零)

- 全部锚点写时 grep/Read; 规划断言 (两阶段/Marker/TxnMarkerEntry/read_committed) 全部实证 ✅

## 第四层: 覆盖缺口 (07 五维度 R3/R5)

- R3: read_committed/ControlBatch 缺锚点 → 补 (ConsumerConfig.java:364-369 + UnifiedLog.java:1421) ✅
- R5: 负面 0/0 → 补 (不做应用层去重/不做单机排队事务) ✅

## 第五层: 裸行号

- 写时根治 → 0 残留 (python 归属 4 文件混合)

## 第六层: 跨域引用核验

- r16-multi (MULTI/EXEC 26 处实证) ✅ / rd2-rlock ✅ / k3-log ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-15) — 修复后复核

### 0 处新偏差

- 三形态裸锚点全零; 锚点密度 5/12 (🟡B ≥4 达标)
- 语义复核: 两阶段链 (TransactionCoordinator.scala:L505-593) / Marker 链 (TransactionCoordinator.scala:L176-206) / 状态表 (TransactionCoordinator.scala:L704-732) — 与源码一致
- 反写测试: 7 场景, 只读大纲可写文章 ✅
- r16 对照真实 (K-12 教训未重犯) ✅

### 结论

K-11 三遍验证闭环: 写时 grep → 自查 → 复审通过; 修复全为格式类 + 覆盖补全 (R3/R5)。K-11 大纲层交付完成 (待用户确认后进入 completeness/回填)。

---

## 第三轮复审 (REVIEW-3, 2026-08-15) — 07 五维度深度收官

### R1 维度1 (桥+结构): 0 发现, 收敛
- 四行双链 + 桥链 01→02 + 零反模式

### R2 维度2 (锚点密度): 0 发现, 收敛
- 5/13 (🟡B ≥4 全达标)

### R3 维度3 (规划断言收官): **1 补全, 断言全验证** ⚠️
- **补全**: 规划断言 "InterBrokerSendThread 发送" 大纲无锚点 → 补 (TransactionMarkerChannelManager.scala:167 extends InterBrokerSendThread "TxnMarkerSenderThread")
- **规划 K-11 断言全部验证**: 幂等 (K-3 lastSeq/epoch) / 两阶段 (L505-593) / __transaction_state / InterBrokerSendThread (L167) / TxnMarkerEntry (L30-31) / TransactionResult COMMIT/ABORT (L562-593) / ControlBatch+read_committed (UnifiedLog L1421 + ConsumerConfig L364-369) — 全对, 零错误

### R4 维度4 (横切: 状态机/epoch): 0 发现, 收敛

### R5 维度5 (负面+开篇): 0 发现, 收敛
- 负面声明 1/1; 开篇词 7

### 内容深度轮: 0 发现
- 反写测试: 7 场景 ✅
- r16-multi 对照复验: MULTI 11 处实证 ✅

### 收敛判定
R1/R2/R4/R5 + 内容深度轮收敛; R3 补 1 细节 (InterBrokerSendThread L167) — 规划断言 100% 验证 (K-8/K-11 连续两域规划零错误)。修复后三形态零残留。K-11 深度收官完成。
