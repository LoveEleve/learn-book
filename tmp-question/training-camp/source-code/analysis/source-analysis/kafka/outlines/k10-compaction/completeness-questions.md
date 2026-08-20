# K-10 Compaction — 全视角提问验证 (completeness)

> 验证时机: 2 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 双哈希 | SkimpyOffsetMap 每个 entry 存什么? 多大? | ✅ 01-L2 (16B MD5 哈希 + 8B offset = 24B/条) |
| 2 | 开发者 | 双哈希 | 冲突怎么处理? 无链表怎么找? | ✅ 01-L2 (hash1/hash2 双哈希线性探测) |
| 3 | 开发者 | 三阶段 | buildOffsetMap 遍历什么? 存什么? | ✅ 01-L3 (dirty 段 key→latestOffset, 旧值覆盖) |
| 4 | 开发者 | 三阶段 | cleanSegments 怎么判定保留? | ✅ 01-L3 (map 中 key 最新 offset == 当前才保留) |
| 5 | 开发者 | 三阶段 | 段怎么分组? 为什么按大小? | ✅ 01-L3 (groupSegmentsBySize ~segmentSize/组) |
| 6 | 架构师 | 设计 | 为什么不用 HashMap 存 key? | ✅ 01 核心悬念 (内存省一个量级, 碰撞探测代价) |
| 7 | 架构师 | 设计 | 为什么三阶段 build→group→clean? | ✅ 01-L3 (map 建一次复用 N 组, 分组控索引上限) |
| 8 | 架构师 | 设计 | 原子 swap 怎么让读者无感? | ✅ 02-L2 (replaceSegments 容器级, 段列表快照) |
| 9 | 性能工程师 | 内存 | dedupe buffer 128MB 能覆盖多大日志? | ✅ 01-L2 (128MB/24B ≈ 559 万 key 去重) |
| 10 | 性能工程师 | 调度 | 为什么选脏要有 0.5 阈值 + 限流? | ✅ 02-L4 (minCleanableRatio 防频繁小清, throttler 保护 IO) |
| 11 | SRE | 删除 | 墓碑消息多久才被物理删除? | ✅ 02-L3 (v2+ batch deleteHorizonMs = 写入时刻+delete.retention.ms 默认 24h) |
| 12 | SRE | 容错 | 压缩中途失败怎么办? | ✅ 02-L4 (LogCleaningAbortedException, .cleaned 删除, 重试) |
| 13 | SRE | 运维 | cleaner 线程默认几个? 卡住怎么办? | ✅ 02-L4 (默认 1 线程, backoff 15s 轮询) |
| 14 | 研究者 | 对照 | vs AOF rewrite 本质差异? | ✅ 02-L4 (去重重写段内 vs 全量归并 fork) |
| 15 | 研究者 | 对照 | vs Redis 删除语义差异? | ✅ 02-L3 (时间窗 deleteHorizonMs vs 惰性+主动过期 r22) |
| 16 | 子系统开发者 | 衔接 | LogCleaner 怎么消费 K-3 段? | ✅ 02-L4 (replaceSegments 原子替换 K-3 容器) |
| 17 | 子系统开发者 | 衔接 | 事务批/control batch 会被误删吗? | ✅ 01-L3 (CleanedTransactionMetadata, control batch 保留至事务记录删完) |
| 18 | 学生 | 概念 | 日志压缩通俗是什么? | ✅ 01-L1 (保留每 key 最新, 其余丢弃) |

**统计**: ✅ 18 / ⚠️ 0 / ❌ 0

## 回补清单

- 无 (两篇大纲覆盖全视角; 深审发现已当场修复: 24B/条数值、deleteHorizonMs 墓碑判定、r22 对照、调度阈值)
