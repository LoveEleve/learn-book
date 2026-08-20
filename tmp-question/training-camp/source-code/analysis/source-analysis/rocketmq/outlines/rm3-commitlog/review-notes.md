# RM-3 CommitLog+ConsumeQueue+IndexFile — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | **Ext 触发条件**: isExtWriteEnable = consumeQueueExt != null && enableConsumeQueueExt (L1099-1103) — **配置开启** (位图过滤场景), 非"tagsCode 溢出"; q3 表述错误 | 大纲 §3 + pass2-q3 修正 |
| 2 | **表述精确化** | **恢复期分发双路径**: 正常退出 recoverNormally **doDispatch=false** ("normal recover doesn't require dispatching" L337 — CQ 已持久) / **异常退出 recoverAbnormally 重放分发** (重建 CQ/Index L695); q2 "恢复期跳过"只说一半 | 大纲 §2 + pass2-q2 修正 |
| 3 | **表述精确化** | **CRC32 段 = 属性校验和** (enabledAppendPropCRC 配置 → 4B 追加, L57), 非"保留位" | 大纲 §1 + pass2-q1 修正 |
| 4 | 补充锚点 | maxMessageSize = 4MB (MessageStoreConfig:168); maxMessageBodySize 独立 | 大纲 §1 补注 |
| 5 | 验证 | CQ 定位用 findMappedFileByOffset (ConsumeQueue:868); IndexFile 滚动 (IndexService:330 新文件创建); 双上限 (MESSAGE_ILLEGAL); 属性超限 (Short.MAX) | 记录 |
| 6 | 行号验证 | 全函数 30 锚点 + 跨文件 10 处 grep (MessageExtEncoder 41-278 / CommitLog 338-352,695 / DefaultMessageStore 266-272,418,781-830,1989-1995,2108-2112 / ConsumeQueue 59,1099-1103 / IndexHeader 36-45 / IndexFile 32-58 / IndexService 45-53,213-246 / queue/ 包 / MessageStoreConfig 112-168) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 18 段字段序 encode/calMsgLength 对称
- 双上限 + 属性超限
- 分发链顺序 (CQ→Index→Compaction)
- 恢复双路径 (正常/异常)

### 维度2 性能
- 20B 固定单元 O(1) 定位
- 哈希槽 + 链式索引
- mmap 切片读
- 写后同步分发 (读一致)

### 维度3 内存
- 索引文件容量 (500 万槽 × 4B + 项 × 20B)
- Ext 48MB
- CQ 5.7MB/文件

### 维度4 一致性
- 恢复截断对齐
- 异常恢复重放分发
- CQ 可写限流
- DispatchRequest 契约

### 维度5 负面空间 (已写入大纲 5 条)
- 不单条删除/不二级索引/不压缩/不过滤下推/不多主写

## 结论
RM-3 全部锚点 ~45 处验证, 6 闭环完成, **认知修正 1 + 表述精确化 2 + 补锚 1**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 RM-1/RM-2 (已交付) ✅; 引出 RM-8/9/16 ✅; 对照 R-8 + Kafka ✅; 读者处境场景化 ✅; 锚点 ~45 (≥4 ⏫) ✅; 负面空间 5 条 ✅; 开篇场景化 ✅; 横切 (并发/内存/磁盘/恢复/扩展) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §1 "PHYSICALOFFSET 写后回填" — 回填点未锚定 (doAppend 内 byteBuffer.putLong 位置) | 大纲 §1 补注 (回填在 doAppend L2010 区域) |
| 9 | 通过项 | 其余 ~35 句机制描述逐句对源码一致 ✅ (18 段/V6 标志/双上限/属性超限/Batch/encodeWithoutProperties/三链/30 次重试/isCQWriteable/20B 单元/5.7MB/Ext 配置/四实现/40B 头/500 万槽/20B 项/topic#uniqKey/链式 prevIndex/守卫/双限/恢复截断/maxPhysicOffset/测试 7 专项) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (PHYSICALOFFSET 回填锚点), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | calMsgLength 求和 | 4×6(定长头) + 8×4(QUEUEOFFSET/PHYSICALOFFSET/BORNTIMESTAMP/STORETIMESTAMP/PreparedTx) + bornhost 8/20 + storehost 8/20 + body + topic + 2(prop) — 与 encode 段序对称 ✅ | 通过 |
| V2 | CQ 文件容量 | 300000×20 = 6,000,000B ≈ 5.72MB ✅ | 通过 |
| V3 | 索引文件容量 | 40 + 500万×4 + N×20 — 槽表 20MB; 索引项数决定滚动 ✅ | 通过 |
| V4 | 20B 项位宽 | keyHash(4)+phyOffset(8)+timeDiff(4)+prevIndex(4) = 20 ✅ | 通过 |
| V5 | 40B 头位宽 | 8×4 + 4×2 = 40 ✅ | 通过 |
| V6 | 恢复双路径语义 | 正常 (flush 完整) → 截断即可; 异常 (半写) → 重放分发重建 ✅ | 通过 |
| V7 | 30 次重试语义 | CQ 写失败重试 30 次 (可写标志轮询) ✅ | 通过 |
| V8 | V2 topic 长度 | short vs byte — 2B vs 1B, 主题名上限 255→65535 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **覆盖缺口** | 大纲未提 **tagsCode 的生成**: 消息 tag 字符串 → 哈希 (MessageDecoder.tag2int 或类似) 存 8B; Ext 模式位图过滤 (RM-6 filter 交叉) | 大纲 §3 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (tagsCode 生成锚点), 修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查七个存疑点 (getMessageAsync 真伪/offset 语义/tagsCode 算法/恢复调用链/keyHash/Batch 单元/IndexFile 滚动), 并做反写测试。

## 追查过程 (七个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | getMessageAsync 真异步? | L987-990: **CompletableFuture.completedFuture(getMessage(...)) — 同步包装非异步**; 长轮询挂起在 broker PullRequestHoldService (broker/longpolling/, RM-8 交叉) | **认知修正 (发现 1): 大纲 §5 "5.x 长轮询 CompletableFuture" 错误** |
| T2 | getMessage offset 语义 | offset 为逻辑队列偏移; CQ 定位 = findMappedFileByOffset (ConsumeQueue:868, 物理字节 = queueOffset×20) | 通过 |
| T3 | tagsCode 算法 | **tags.hashCode()** (MessageExtBrokerInner:43-47, Java 哈希 32 位) | 发现 4 (补充锚点) |
| T4 | 恢复调用链 | DefaultMessageStore.recover (L1890-1902): **CQ 先恢复** (getMaxPhyOffsetInConsumeQueue) → **CommitLog recoverNormally(maxPhyOffset) 对齐截断** — CQ 定界 CommitLog 对齐 | 发现 2 (顺序精确化) |
| T5 | buildIndex 细节 | **TRANSACTION_ROLLBACK 跳过** (L219-224); **多 keys 逐个建索引** (keys.split(KEY_SEPARATOR) L235-246) | 发现 3 (补充锚点) |
| T6 | Batch CQ 单元 | **BatchConsumeQueue.CQ_STORE_UNIT_SIZE = 46B** (L63, 非 20) — mapperFileSizeBatchConsumeQueue 用此单元 | 发现 5 (补充锚点) |
| T7 | IndexFile 滚动条件 | putKey 返回 false (文件满) → **"is full, trying to create another one"** (L255-258) → 新文件 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | completedFuture 语义 | 同步执行 + 已完成 future — 调用方 (broker) 立即拿到结果; 挂起语义不在 store 层 ✅ | 通过 |
| V2 | 恢复顺序必要性 | CQ 先得物理界 → CommitLog 截断到界 — 防止 CQ 指向半写消息 ✅ | 通过 |
| V3 | tagsCode 位宽 | hashCode() 32 位 → 高 32 位恒 0 (8B 槽) — Ext 高位标记不冲突 (isExtAddr 用最高位) ✅ | 通过 |
| V4 | 多 key 索引 | 每条 key 独立 buildKey(topic#key) → 同消息多个索引项 (共享 phyOffset) ✅ | 通过 |
| V5 | rollback 跳过理由 | 回滚消息已标记删除, 索引指向无意义 ✅ | 通过 |
| V6 | Batch 46B 构成 | 46B 单元 (批量消息 CQ — 多消息聚合索引, 细节 RM-16) ✅ | 通过 |
| V7 | 索引满滚动 | 文件满 → 新文件 (endPhyOffset 截止, L330 创建参数) ✅ | 通过 |
| V8 | Ext 高位标记 | tagsCode 高位置 1 = extAddr (isExtAddr) — 与 hashCode 32 位不冲突 ✅ | 通过 |

## 新发现问题 (5 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **认知修正** | 大纲 §5 "getMessageAsync (5.x 长轮询 CompletableFuture)" **错误** — completedFuture 同步包装; 长轮询挂起在 broker PullRequestHoldService | 大纲 §5 重写 |
| 12 | 精确化 | 恢复顺序: CQ 先恢复定界 → CommitLog 对齐截断 (原表述"CQ 与 CommitLog 对齐"缺顺序) | 大纲 §5 修正 |
| 13 | 补充锚点 | buildIndex: TRANSACTION_ROLLBACK 跳过 + 多 keys 逐个建索引 | 大纲 §4 补注 |
| 14 | 补充锚点 | Batch CQ 单元 46B (非 20B) | 大纲 §3 补注 |
| 15 | 补充锚点 | tagsCode = tags.hashCode() | 大纲 §3 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 编码: 18 段/calMsgLength/双上限/CRC32 — 可写 ✅
- §2 分发: 三链/恢复双路径/30 次重试 — 可写 ✅
- §3 CQ: 20B 单元/tagsCode 算法 (修复后)/Ext 配置/Batch 46B (修复后) — 可写 ✅
- §4 IndexFile: 布局/槽表/多 key+rollback 跳过 (修复后)/滚动 — 可写 ✅
- §5 读面: 守卫/双限/恢复顺序 (修复后)/getMessageAsync 真相 (修复后) — 可写 ✅
- §6 多实现: 四实现/测试 — 可写 ✅
- 负面空间 5 条 — 完整 ✅

## 四次 REVIEW 汇总

七存疑点全实证 (T1-T7); 推理验证 8 项全过 (V1-V8); **新发现 5 处全部修复** — **#11 认知修正最有价值** (getMessageAsync 同步包装, 长轮询在 broker 层 — RM-8 边界修正)。大纲经修复后反写测试全过。
