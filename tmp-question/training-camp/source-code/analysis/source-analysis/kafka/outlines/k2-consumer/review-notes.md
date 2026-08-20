# K-2 Consumer — 六层深审 REVIEW 记录 (2026-08-15)

> 审查方法: 07 五维度 + 逐锚点核对 + 裸行号三形态扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (裸锚点 50+ 处根治, 三形态零残留; 上限 4 文件全过; 跨域 4 目录 [ -d ] 全过)**

## 第一层: 锚点验证 (写时即 grep/Read)

- KafkaConsumer.java: 类 KafkaConsumer.java:L532 / CREATOR KafkaConsumer.java:L534 / delegate KafkaConsumer.java:L536 ✅
- ConsumerDelegateCreator.java: create L57-70 / Async L64 / Classic L66 ✅
- AsyncKafkaConsumer.java: 类 AsyncKafkaConsumer.java:L172 / 注释 AsyncKafkaConsumer.java:L299-300 / fetchBuffer AsyncKafkaConsumer.java:L304 / fetchCollector AsyncKafkaConsumer.java:L305 / ConsumerNetworkThread AsyncKafkaConsumer.java:L385 / 共享 AsyncKafkaConsumer.java:L428-429 / 应用线程专用 AsyncKafkaConsumer.java:L486-487 ✅
- ClassicKafkaConsumer.java: 类 ClassicKafkaConsumer.java:L116 / pollForFetches ClassicKafkaConsumer.java:L690 / updateFetchPositions ClassicKafkaConsumer.java:L1188 ✅
- AbstractCoordinator.java: pollHeartbeat AbstractCoordinator.java:L368 / ensureActiveGroup AbstractCoordinator.java:L400-401 / joinGroupIfNeeded AbstractCoordinator.java:L463 ✅
- ConsumerCoordinator.java: 类 ConsumerCoordinator.java:L103 / onJoinComplete ConsumerCoordinator.java:L375 ✅
- Fetcher.java:59 / FetchRequestManager.java / ConsumerConfig.java:175-179 (auto.offset.reset 三态) ✅

## 第二层: 机制实证 (全过)

- 门面+双模型 (KafkaConsumer.java:L532-536 + Creator L57-70) / FetchBuffer 跨线程 (AsyncKafkaConsumer.java:L299-305) / rebalance 四步 (AbstractCoordinator) / offset 单整数 (设计文档) ✅

## 第三层: 编造检查 (零)

- 全部锚点写时 grep/Read; 双模型分流/三态 reset 源码实证 ✅

## 第四层: 覆盖缺口 (07 五维度 R3/R4/R5)

- R3: **auto.offset.reset 三态** (earliest/latest/none, ConsumerConfig.java:175-179) — 规划断言两态遗漏 none → 02/pass2 补全 ✅
- R4: 02 篇锚点不足 (5<8) → 补 AbstractCoordinator/ConsumerCoordinator 具体行号 (5→8) ✅
- R5: 01/03 篇负面词 0 → 补 (不做 broker 推送/不做调用线程网络 IO) ✅

## 第五层: 裸行号

- 写时 50+ 处 (6 文件混合) → awk 归属修复 + 13 处文字形手动修 → **0 残留**
- 抓到 1 处 awk 归属错误 (ConsumerDelegateCreator.java:L64-66 错标 ClassicKafkaConsumer, 实为 ConsumerDelegateCreator.java) — 批量修复后人工复核铁律第 5 次验证

## 第六层: 跨域引用核验

- rm8-push / r9-replication / r14-sentinel / k12-fetchsession (4 目录 [ -d ]) ✅
- **rm8-push 对照真实性**: DefaultMQPushConsumer 在 RM-8 pass1 (6 处) + pass2 (3 处) 实证 ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-15) — 修复后复核

### 0 处新偏差

- 三形态裸锚点全零; 锚点密度 13/8/10 (🔴A ≥8 全达标)
- 语义复核: 门面分流 (L64-66) / FetchBuffer (AsyncKafkaConsumer.java:L304-305) / rebalance 触发链 (AbstractCoordinator.java:L400-401,AbstractCoordinator.java:L463) / 三态 reset (ConsumerConfig.java:L175-179) — 与源码一致
- 反写测试: 11 场景, 只读大纲可写文章 ✅

### 结论

K-2 三遍验证闭环: 写时 grep → 自查 → 复审通过; 修复全为格式类 + 覆盖补全 (R3/R4/R5)。K-2 大纲层交付完成 (待用户确认后进入时空溯源/harness/completeness)。

---

## 第三轮复审 (REVIEW-3, 2026-08-15) — 全流程交付后 07 五维度深度收官

### R1 维度1 (桥+结构): 0 发现, 收敛

- 四行双链 + 桥链 01→02→03 + 零反模式

### R2 维度2 (锚点密度): 0 发现, 收敛

- 13/8/10 (🔴A ≥8 全达标); harness 9 处对照锚点 + 14/14 复验 ALL PASS

### R3 维度3 (规划断言复核): **1 发现 (规划错误), 大纲正确** ⚠️

- **发现**: 规划断言 "ClassicKafkaConsumer(旧) → ConsumerNetworkThread 后台收数据" — **错误**: Classic 中 ConsumerNetworkThread 引用 = 0 (Async 才有 AsyncKafkaConsumer.java:L385)
- **验证**: 4.1.2 实际: Classic = 同步 Fetcher 拉取 (ClassicKafkaConsumer.java:128-129 Fetcher 字段 + ClassicKafkaConsumer.java:L690 pollForFetches) / Async = ConsumerNetworkThread 事件循环
- **结论**: 我的大纲表述正确 (规划 R2 时代断言被推翻 — 规划把 Async 的事件循环错安到 Classic 头上)
- 教训: 规划断言必须源码验证, 连"已 R2 验证"的断言也可能错 (规划第 3 处错误: 双模型分流细节)

### R4 维度4 (横切: 组活性/超时): **1 发现, 1 修复** ⚠️

- **发现**: wakeup/poll 超时/max.poll.interval 0 覆盖 (面试高频: poll 间隔超时被踢出组)
- **修复**: 02-L2 补组活性双超时 — max.poll.interval.ms 默认 300000 (ConsumerConfig.java:631) + wakeup() (KafkaConsumer.java:1850)

### R5 维度5 (负面+开篇): 0 发现, 收敛

- 负面声明 1/1/1; 开篇词 6

### 内容深度轮: 0 发现

- 反写测试: 11 场景, 只读大纲可写文章 ✅
- harness 对照审计: 9 处源码对照锚点 + 14/14 复验 ✅
- 跨域对照: rm8-push 真实 (DefaultMQPushConsumer) ✅

### 收敛判定

R1/R2/R5 + 内容深度轮收敛; R3 抓 1 规划断言错误 (Classic/Async 混淆, 大纲正确) + R4 抓 1 横切缺口 (组活性) — 修复后三形态零残留。K-2 深度收官完成。
