# R-8 RDB+AOF — completeness-questions (两篇)

## R-8a RDB — 开发者视角

1. rdbSaveLen 四档的边界值?
2. 整数编码 INT8/16/32 怎么选?
3. 字符串什么时候用 LZF?
4. rio 三实现分别用于什么场景?
5. autosync 增量写分批解决什么问题?
6. bgsave 的一致性怎么保证?
7. rename 原子替换 + fsyncFileDir 为什么?
8. CRC64 什么时候算/不算?

## R-8a RDB — 架构师视角

9. 26 种 RDB_TYPE 的演进含义 (PRE_GA 痕迹)?
10. listpack 整包存储 vs dict 逐项的取舍?
11. 无盘复制 EOFMark 协议设计?
12. rdbReportError 三分支的容错哲学?
13. v9 字节序修复为什么只对 ≥v9?
14. RESIZEDB/SLOT_INFO 预扩的意义?
15. 为什么 RDB 不做增量?
16. AUX %前缀字段的设计意图?

## R-8a RDB — 学生视角

17. "REDIS0011" 的 magic 布局?
18. 损坏 RDB 加载时会发生什么?
19. RESTORE 命令和加载的差异?
20. fork COW 为什么内存翻倍风险?

## R-8b AOF — 开发者视角

1. 三策略的 fsync 时机?
2. everysec 空缓冲为什么还要 fsync?
3. 延期 ≤2s 的机制?
4. 短写怎么修复?
5. SELECT 注入的触发条件?
6. 重写怎么保证不丢新命令?
7. manifest 文件是什么?
8. 截断容忍的行为?

## R-8b AOF — 架构师视角

9. 写前日志 + 策略分级的权衡?
10. 命令级日志 vs 数据级快照的取舍?
11. 混合持久化 (RDB preamble) 的收益?
12. 变参批量 (64/批) 的设计?
13. fork 重写 + INCR 切换的并发模型?
14. 假客户端重放的哲学 (与真实客户端同链)?
15. Multi-Part AOF 解决了什么?
16. aof_state 三态 (ON/WAIT_REWRITE/OFF)?

## R-8b AOF — 学生视角

17. everysec 最多丢多少数据?
18. AOF 加载为什么比 RDB 慢?
19. BGREWRITEAOF 和 BGSAVE 的异同?
20. appendonly 开启时重启加载顺序?

# R-8 RDB+AOF — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (初版) | RDB 持久化 (rdb.c/rio.c 版权 2009; rio 抽象由 Pieter Noordhuis 编写 — rio.c:18) |
| 2010 (1.2) | AOF 引入 (appendfsync 三策略; aof.c 版权 2009-Present) |
| 2011 (2.2) | WATCH+持久化交互; bio 线程引入 (fsync 卸载主线程) |
| 3.2 | AOF 重写变参批量 (RPUSH/SADD 批量) |
| 4.0 | **混合持久化 aof-use-rdb-preamble** (rewriteAppendOnlyFile L2380-2385) + MODULE 类型 (RDB_TYPE_MODULE_2) |
| 5.0 | **RDB v9 字节序修复** (rdb.c:125-146 注释权威证据 — "before Redis 5 (RDB version 9)") + STREAM |
| 7.0 | **Multi-Part AOF manifest** (aof.c:128-214; 重写注释 L2424-2436) + FUNCTION2 + SLOT_INFO |
| 7.4 | HASH_METADATA (HFE, rdb.h:76-79 — "7.4 RC" PRE_GA 痕迹) |

## 痕迹证据

- rdb.c:125-146: v9 字节序修复注释 (时空溯源关键证据 — 版本/字节序/兼容策略完整解释)
- rdb.h:60-62: "Used in 4.0 release candidates" (MODULE_PRE_GA) — 版本痕迹
- rdb.h:76-79: "Doesn't attach min TTL at start (7.4 RC)" — 7.4 HFE 痕迹
- aof.c:2424-2436: Multi-Part 重写步骤注释 (4a-4e: BASE/HISTORY/manifest/bio)
- rio.c:1-14: 流抽象设计注释
- aof.c:1333-1334: "same way in AOF as in replication" — 传播统一设计
- aof.c:1051-1055: 空缓冲补偿动机注释

## 推断标注

- "bio 2.2 引入" — 版本推断
- "always=零丢失" — 语义推断 (崩溃瞬间窗口)
- "RDB_VERSION=12" — rdb.h:20 实证 (非推断, 已修正)
