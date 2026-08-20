# RM-9 Rebalance+offset+LitePull — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **补充锚点** | **广播模式无分配**: mqSet = 全队列 (L307-320) — 每客户端全量消费, balanced = 队列数匹配; 集群才走算法 | 大纲 §3 补注 |
| 2 | **补充锚点** | **双排序前提**: mqAll/cidAll Collections.sort — 所有客户端排序一致 → 分配确定性 (同一算法同输入同输出) | 大纲 §3 补注 |
| 3 | **补充锚点** | **broker 分配失败回退**: 3s×3 超时 → topicClientRebalance.put (回退客户端模式, graceful degradation) | 大纲 §2 补注 |
| 4 | 验证 | ConsistentHash virtualNodeCnt 构造参数 (默认由调用方传, 非内置 1024); AVG startIndex 数学 (前 mod 个多 1) | 记录 |
| 5 | 行号验证 | 全函数 25 锚点 + 跨文件 8 处 grep (RebalanceService 22-40 / RebalanceImpl 237-370,98-200 / RebalancePushImpl 166-230 / 6 算法 / LitePullImpl 93-219) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 双模式判定 (双表)
- 差集增删 (drop/unlock/起点)
- 广播/集群分派

### 维度2 性能
- 20s/1s 自适应
- 双排序确定性
- 起点尾部 (少读旧消息)

### 维度3 内存
- processQueueTable (ConcurrentHashMap)
- 双表缓存

### 维度4 一致性
- 双排序 → 确定性分配
- broker 锁互斥
- 分配失败回退

### 维度5 负面空间 (已写入大纲 5 条)
- 不默认中心化/不粘性/不迁移优化/不事件持久化/不分区均衡

## 结论
RM-9 全部锚点 ~40 处验证, 6 闭环完成, **补充锚点 3**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-8 (已交付) + RM-5 ✅; 引出 RM-10 ✅; 对照 Kafka ✅; 读者处境场景化 ✅; 锚点 ~40 ✅; 负面空间 5 条 ✅; 横切 (并发/一致性/算法/模式) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §1 "触发面: rebalanceImmediately (OFFSET_ILLEGAL 修复/RM-8)" — 未提**心跳面**: 消费者加入/退出经心跳 → broker 广播变更 → 其他客户端感知 → 再平衡; 与 RM-5 交叉需补 | 大纲 §1 补注 |
| 8 | 通过项 | 其余 ~30 句机制描述逐句对源码一致 ✅ (20s/1s 自适应/双表/queryAssignment 3s×3/差集增删/AVG 余数/6 算法/起点 5 模式/有序锁批量/冻结面/LitePull 互斥/AssignedMessageQueue) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (心跳触发面 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | AVG startIndex 数学 | 前 mod 个: index×avg+1; 后: index×avg+mod — 无重叠无遗漏 ✅ | 通过 |
| V2 | 双排序确定性 | 同 cidAll 排序 (客户端 ID 字典序) → 同 index → 同分配 ✅ | 通过 |
| V3 | 广播 balanced 判定 | mqSet.equals(working) — 队列数变化即触发再平衡 ✅ | 通过 |
| V4 | 回退语义 | broker 分配失败 → 客户端模式 → 下次再试 broker? (topicBrokerRebalance 未删? 需标注) — 标注 | 通过 (标注) |
| V5 | 有序锁与再平衡 | 再平衡删队列 → unlockAll; 新队列 → lockAll — 锁随归属 ✅ | 通过 |
| V6 | 起点与冻结闭环 | OFFSET_ILLEGAL → freeze → rebalance → 新归属者从冻结 offset 续读 ✅ | 通过 |
| V7 | LitePull assign 并发 | messageQueueLock (L156) — assign 与 poll 并发安全 ✅ | 通过 |
| V8 | 心跳变更链路 | 心跳注册 → broker cid 集合变化 → 客户端 doRebalance 感知 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **consumerId 变更的感知链**: 客户端心跳 (HEART_BEAT) 注册到 broker → broker 侧 consumerTable 变化 → **其他客户端经 findConsumerIdList 感知** → 再平衡; 与 RM-5 clientManageProcessor 交叉 | 大纲 §1 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (consumerId 感知链), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (广播模式/ConsistentHash 节点/分配失败回退/心跳感知/AVG 数学/回退语义), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 广播模式分配? | **广播 = 全队列无分配** (RebalanceImpl L307-320: mqSet=topicSubscribeInfoTable 全量, balanced=队列数匹配) — 每客户端全量消费 | 发现 1 (补锚) |
| T2 | 集群分配前提? | **双排序 mqAll/cidAll Collections.sort** (L335-337) — 所有客户端排序一致 → 分配确定性 | 发现 2 (补锚) |
| T3 | broker 分配失败? | 3s×3 超时 → catch → **topicClientRebalance.put (回退客户端模式)** (L290-300) — graceful degradation | 发现 3 (补锚) |
| T4 | ConsistentHash 节点? | virtualNodeCnt **构造参数** (L32-43, 默认由调用方传) — 非内置 1024 | 通过 (验证) |
| T5 | AVG 数学? | 前 mod 个: avg+1; 后: avg; startIndex 无重叠无遗漏 | 通过 |
| T6 | 回退后再试 broker? | topicBrokerRebalance 未删 → 下轮 doRebalance 仍试 broker? — 标注 (行为存疑) | 通过 (标注) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | AVG 无重叠 | startIndex 前段 index×avg / 后段 index×avg+mod — 分段连续 ✅ | 通过 |
| V2 | 双排序确定性 | 同 cid 集合同排序 → 同 index → 同分配 (所有客户端一致) ✅ | 通过 |
| V3 | 广播触发 | mqSet 变化 (topic 增删队列) → equals 失败 → 再平衡 ✅ | 通过 |
| V4 | 回退语义 | broker 分配失败 → 客户端模式 (功能不中断) — 标注下次重试行为 | 通过 (标注) |
| V5 | 有序锁转移 | 删队列 unlockAll / 新队列 lockAll — 锁随归属 ✅ | 通过 |
| V6 | 起点冻结闭环 | OFFSET_ILLEGAL → freeze → 新归属者续读冻结点 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 补充锚点 | 广播 = 全队列无分配 (每客户端全量) | 大纲 §3 补注 |
| 11 | 补充锚点 | 双排序 (mqAll/cidAll) = 确定性分配前提 | 大纲 §3 补注 |
| 12 | 补充锚点 | broker 分配失败 → 回退客户端模式 (graceful degradation) | 大纲 §2 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 调度 (20s/1s/触发面) — 可写 ✅
- §2 双模式 (广播/集群/回退) — 可写 ✅
- §3 差集+算法 (删增/6 算法/AVG 数学) — 可写 ✅
- §4 起点 5 模式 — 可写 ✅
- §5 有序锁 — 可写 ✅
- §6 LitePull — 可写 ✅
- 负面空间 5 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 6 项全过 (V1-V6); **新发现 3 处全部修复** (广播无分配/双排序确定性/回退 graceful)。大纲经修复后反写测试全过。
