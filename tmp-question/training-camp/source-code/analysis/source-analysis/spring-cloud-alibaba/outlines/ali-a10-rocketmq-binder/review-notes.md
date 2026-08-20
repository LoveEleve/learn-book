# ALI-A10 RocketMQ Stream Binder — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "inbound=RocketMQInboundChannelAdapter (消费者)" 未提 pull 面 | 实测双面: push 适配器 + **pull 消息源** (RocketMQMessageSource, createPolledConsumerResources L148-159) — 双消费模式补全 |
| 2 | 规划未提事务消息与分区互斥 | 大纲补: 注释锚 "TransactionMQProducer does not currently support custom MessageQueueSelector" (L115-116) |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "错误处理只有 errorChannel" | 实测三分支: maxAttempts>1 → retryTemplate+recoverer / =1 → errorChannel / pull 错误 → ErrorAcknowledgeHandler 可插拔 (L137-144 + L171-174) |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "RocketMQMessageConverter 是唯一转换器" | 实测转换分两层: RocketMQMessageConverter (Stream 面) + **RocketMQMessageConverterSupport (MQ 面)** — 职责拆分 |

## 审 4: 跨项目概念转移 — 0

## 审 5: 覆盖率 — 0 缺漏 (Binder 契约/生产/消费/错误/辅助/约束 6 面全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | Binder:81-84 (enabled 校验) + 94-99 (分区拦截器) + 118-127 (anonymous/DLQ+文档引用) + 137-144 (maxAttempts) + 165-176 (errAck 三选) / Producer:97-99 (isTrans) + 115-117 (事务分区互斥注释) + 164-169 (TransactionListener) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | Stream Binder SPI 契约; 事务消息 vs A9 分布式事务边界对照 | 通过 ✅ |
| 3 | 数字自洽 | 三方法/双消费/三分支/33 文件全文一致 | 通过 ✅ |

## 四轮 REVIEW (2026-08-16, 全量脚本复验)

文字锚 (#7) 命中修复: 本文档域引用类补行号 (grep 实证, 见 HANDOFF §六 第 6 行); 锚点范围修正见全局记录。
