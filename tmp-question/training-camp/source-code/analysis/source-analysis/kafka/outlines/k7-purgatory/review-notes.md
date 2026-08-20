# K-7 Purgatory — 六层深审 REVIEW 记录 (2026-08-15)

> 审查方法: 07 五维度 + 逐锚点核对 + 裸行号三形态扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (裸锚点 60+ 处根治, 三形态零残留; 上限全过; 跨域 [ -d ] 全过)**

## 第一层: 锚点验证 (写时即 grep/Read)

- DelayedOperation.java: 类 L38 / completed L41 / forceComplete L48-72 (已完成拒绝 L50 / 重检 L56 / cancel+onComplete L59-62) / onExpiration L93-96 / onComplete L98-100 ✅
- DelayedOperationPurgatory.java: 类 DelayedOperationPurgatory.java:L38 / SHARDS=512 DelayedOperationPurgatory.java:L41 / estimatedTotalOperations DelayedOperationPurgatory.java:L47 / 构造 purgeInterval=1000 DelayedOperationPurgatory.java:L58,66 / tryCompleteElseWatch DelayedOperationPurgatory.java:L122-170 (挂 watch DelayedOperationPurgatory.java:L156-158 / 超时 DelayedOperationPurgatory.java:L168) / 死锁注释 DelayedOperationPurgatory.java:L135-154 / checkAndComplete DelayedOperationPurgatory.java:L153-165 (tryCompleteWatched DelayedOperationPurgatory.java:L160) ✅
- TimingWheel.java: 类 TimingWheel.java:L97 / tickMs/wheelSize/interval TimingWheel.java:L98-103 / buckets TimingWheel.java:L103 / overflowWheel TimingWheel.java:L106-108 / addOverflowWheel TimingWheel.java:L131-141 / add 三分支 TimingWheel.java:L143-175 (过期 TimingWheel.java:L149-151 / 本层 TimingWheel.java:L152-169 / 溢出 TimingWheel.java:L171-173) / advanceClock TimingWheel.java:L177-184 ✅
- SystemTimer.java: 类 L30 / TimingWheel 组装 L58-61 ✅
- DelayedProduce.scala: 类 DelayedProduce.scala:L57 / tryComplete DelayedProduce.scala:L89-116 / checkEnoughReplicasReachOffset DelayedProduce.scala:L101 / forceComplete DelayedProduce.scala:L112-113 / onExpiration DelayedProduce.scala:L119-124 / onComplete DelayedProduce.scala:L131-134 ✅
- K-4 衔接: Partition.scala:826-827 (tryCompleteDelayedRequests) ✅

## 第二层: 机制实证 (全过)

- 双检锁只完成一次 (DelayedOperation.java:L48-72) / 双保险注册 (DelayedOperationPurgatory.java:L122-170) / 分级时间轮 (TimingWheel.java:L143-175) / acks=all 触发链 (DelayedProduce.scala:L89-116 + Partition.scala:L826) ✅

## 第三层: 编造检查 (零)

- 全部锚点写时 grep/Read; 死锁注释/双保险语义为源码实证 ✅

## 第四层: 覆盖缺口 (completeness 18 问, 1 回补)

- ⚠️ 1 项: estimatedTotalOperations 指标 → 已回补 01-L3 ✅

## 第五层: 裸行号

- 写时 60+ 处 (5 文件混合) → awk 精确行号集合根治 + 7 处文字参数形手动修 → **0 残留**
- 上限: 全部锚点 ≤ 文件行数 (DelayedOperation 155 / Purgatory 423 / TimingWheel 185 / SystemTimer 121 / DelayedProduce 151) ✅

## 第六层: 跨域引用核验

- ch14-timer (netty) ✅ / r8-persistence ✅ / E-3-translog ✅ / K-4 衔接 ✅
- Netty 对照声明 (HashedWheelTimer 同构) 与 K-12 教训一致: 对照前核验对方域 (netty/outlines/ch14-timer 存在, HashedWheelTimer 是 ch14 核心) ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-15) — 修复后复核

### 0 处新偏差

- 三形态裸锚点全零; 锚点密度 15/11 (🟡B ≥4 超标)
- 语义复核: forceComplete 双检锁 (L48-72) / add 三分支 (TimingWheel.java:L143-175) / tryCompleteElseWatch 双保险 (DelayedOperationPurgatory.java:L122-170) / DelayedProduce 触发链 (L89-116) — 与源码一致
- completeness 回补项实证 (estimatedTotalOperations L47)

### 结论

K-7 三遍验证闭环: 写时 grep → 自查 → 复审通过; 修复全为格式类 (裸锚点归属)。K-7 交付完成。

---

## 第三轮复审 (REVIEW-3, 2026-08-15) — 07 五维度深度收官

### R1 维度1 (桥+结构): 0 发现, 收敛

- 四行双链 + K-4→K-7 回补链 (04-hw-commit.md 验证) + 零反模式

### R2 维度2 (锚点密度): 0 发现, 收敛

- 15/14 (🟡B ≥4 超标 3 倍); 上限全过

### R3 维度3 (规划对照): **1 发现, 1 修复** ⚠️

- **发现**: 规划断言 "分级时间轮 1ms/20ms/400ms" 未覆盖 — 源码实证 SystemTimer 无参构造 tickMs=1ms, wheelSize=20 (SystemTimer.java:40-42) → 层级 1ms→20ms→400ms 正确
- **修复**: 02-L2 补默认参数锚点 (规划断言实证)

### R4 维度4 (横切: 线程面): **1 发现, 1 修复** ⚠️

- **发现**: ExpiredOperationReaper 线程 + SystemTimer 单线程执行器 0 覆盖 — 线程面缺口 (超时兜底的第二执行者)
- **修复**: 01-L3 补 Reaper (DelayedOperationPurgatory.java:91,101,409) + 02-L2 补单线程执行器 (SystemTimer.java:54,80)

### R5 维度5 (负面+开篇): 0 发现, 收敛

- 负面空间 1/1 (不做线程阻塞/不做全量压缩对照); 开篇词 5

### 内容深度轮: **1 重大发现, 1 修复** ⚠️⚠️

- **发现 (awk 归属错误)**: 行号集合重叠 (L98-103/L149-151 同时属 DelayedOperation/DelayedOperationPurgatory 集合) → 5 处错标: TimingWheel 的 tickMs 字段/已过期分支被标成 DelayedOperation/Purgatory
- **修复**: 5 处归属修正 + 交叉验证 (TimingWheel.java:98-103 = tickMs 字段, L149-151 = add 已过期分支) — K-4 教训第三次重现: **批量修复后必须逐处人工复核归属**
- 反写测试: 10 场景, 只读大纲可写文章 ✅
- 跨域对照核验: ch14-timer HashedWheelTimer 真实存在 (netty/outlines/ch14-timer/01-hashedwheeltimer.md, 3 处) ✅ — K-12 编造对照教训未重犯

### 收敛判定

R1/R2/R5 收敛 + R3 1 发现 (数值覆盖) + R4 1 发现 (线程面) + 内容深度轮 1 重大发现 (归属错误) — 全部修复后三形态零残留。K-7 深度收官完成。
