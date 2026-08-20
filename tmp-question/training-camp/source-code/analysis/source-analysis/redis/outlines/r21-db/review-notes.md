# R-21 db 键空间 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **行号偏差** | 交接文档 "kvstoreCreate 键空间创建 (L2665-2680)" — 实测 **L2667-2675** (slot_count_bits 声明 L2667, kvstoreCreate L2674-2675) | 大纲 02 节 1 + HANDOFF §十二 修正 |
| 2 | **行号偏差** | hashTypeDbActiveExpire 实测在 **t_hash.c:2073** (闭环初稿写 2080) | pass2-q6 修正 |
| 3 | **机制实证 (harness)** | expires 表删除共享键必须不释放 (key destructor=NULL) — harness 初版无条件 free 键 → ASan use-after-free 复现, 修复后 44/44 | harness 迭代: dictDelete 增加 free_key 标志 (对照 dbExpiresDictType server.c:505) |
| 4 | harness 自身缺陷 | [9] 断言错 (k3 的 TTL 仍在, expires->key_count 应为 1); [10] 桶扫描 `i < mask` 漏最后桶 (应 `i <= mask`) | harness 2 处修正 |
| 5 | 表述修正 | pass2-q8 "TYPE 过滤 7.8 起" — 无版本依据, 实际过滤在 Step3 复查 (db.c:1290-1296) | 改为 "过滤实现在 Step3 复查" |
| 6 | 通过项 | 全锚点行号 grep 验证 ~80 处 (db.c/kvstore.c/ebuckets.c/lazyfree.c/server.c/server.h/cluster.h/t_hash.c/expire.c) ✅; expireIfNeeded db.c:1974 精确 ✅; keyIsExpired L1928 ✅; CLUSTER_SLOT_MASK_BITS=14 (cluster.h:8) / CLUSTER_SLOTS=16384 ✅; EB_SEG_MAX_ITEMS=16 (ebuckets.c:62) ✅; EB_BUCKET_KEY_PRECISION=0 (ebuckets.h:142 TBD) ✅; LAZYFREE_THRESHOLD=64 (lazyfree.c:181) ✅; CRON_DICTS_PER_DB=16 (server.h:105) ✅; INCREMENTAL_REHASHING_THRESHOLD_US=1000 (server.h:128) ✅; HFE_DB_BASE_ACTIVE_EXPIRE_FIELDS_PER_SEC=10000 (expire.c:98) ✅ | 记录 |

## 07 五维度

### 维度1 功能正确性
- 三态过期 (VALID/EXPIRED/DELETED) 与复制语义 harness 实证 (主库删/从库报/客户端豁免)
- 删除顺序 (先 expires 后 keys) harness 实证安全 — 键共享 + destructor=NULL
- 游标 48+bits 编码往返一致; BIT 选桶概率 8947/10000 ≈ 90% (理论 90%)

### 维度2 性能
- expires 零拷贝键 (指针共享) + 整数值 (联合) — 每 TTL ≈ 1 dictEntry
- ebuckets 批量过期摊销 O(1)/桶; segment 聚合省 rax 叶子 ~40B/项
- lazyfree 阈值 64 按分配数估 effort; FLUSHDB ASYNC 换表 O(1)

### 维度3 内存
- kvstore dict 按需分配 + 空桶回收 (FREE_EMPTY_DICTS)
- ebuckets 小规模 list (NULL 起步), 满 16 转 rax

### 维度4 一致性/并发
- 单线程事件循环 → 无锁; 增量 rehash 预算制 (1000us/tick, 子进程跳过防 CoW)
- 换表法 (SWAPDB/emptyDbAsync) 指针交换保客户端无感

### 维度5 负面空间 (已写入大纲)
- 不做自动过期扫描 (R-22 主动面) / 不做键级锁 / 不做独立键对象 / 不做 dict 数组动态伸缩 / 不做时间桶精度提升 (PRECISION=0 TBD) / 不做 ebRemove 合并 (TODO) / 不做无界异步

## 结论
R-21 全部锚点行号 ~80 处 grep 验证, harness 44/44 PASS (gcc+ASan 零错误, 抓到 1 处真实机制语义实证: expires 键共享的删除安全), 2 篇大纲 + 8 闭环完成。零发现 = 不合格 — 本域 5 处修正/实证。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **前向依赖违规** | 02 前置声明 [[R-15-cluster]] — R-15 执行序第 25 位, 尚未讲 (07 维度3: 依赖必须序号 < 当前域) | 移到对照位, 标注 "仅引用 cluster.h 常量, 域详述在 R-15" |
| 8 | 前向引用 | 01 对照 [[R-16-transaction]] 同属未来域 | 标注 "仅信号语义, 域详述在 R-16" |
| 9 | 通过项 | 01 引出 R-21-下 ↔ 02 前置 R-21-上 承接 ✅; 02 引出 R-22 ✅; 五结构元素 (读者处境/场景/源码路径/关键设计/数据流/负面空间) 齐备 ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 01 篇 ~45 锚点 / 02 篇 ~42 锚点 (file:line 格式) — 🔴A 标准 ≥8, 大幅超 ⏫ | 记录 |

## R3 维度3: 前向引用 (已并入 R1, 无独立新发现) — 收敛

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **覆盖缺口** | 从库主动过期面 (expireSlaveKeys) 未提及 — 01 节 2 只讲了惰性面 (只报不删), 读者会误以为从库永不删过期键 | 01 节 2 补 "从库端主动清理走 expireSlaveKeys (R-22 详述)" |
| 12 | **覆盖缺口** | getKeys 的 cluster 消费面 (多键命令槽一致检查) 未提及 — 这是 key-spec 的核心用途 | 01 节 5 补 "消费面: cluster 多键命令槽一致检查 + RO/RW/OW 标志" |
| 13 | 通过项 | LRU/LFU (01 节 1) / 通知 (keymiss/NEW/expired) / 阻塞键 (signalKeyAsReady + SWAPDB 不换 blocked) / 复制 (从库三态) 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 14 | 通过项 | 01 负面 5 条 / 02 负面 5 条, 表格式优化名+原因 ✅; 开篇均为具体场景 (GET 背后/从库看得到删不得/大 key 删除卡顿) ✅ | 记录 |

## R6 内容深度: 反写测试 (只读大纲能否写文章) + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | **编造** | 02 节 4 "ebGetNextTimeToExpire (L1663): 下次最小到期 (**serverCron 调度依据**)" — 消费端穷举: **仅 t_hash.c:1992** (hash 本地 hfe), 全局 hexpires 无消费端 (主动过期走 expires_cursor 遍历) — "调度依据"是编造 | 修正为 "消费端仅 hash 本地 hfe t_hash.c:1992; 全局表不依赖它, 主动过期走 expires_cursor 遍历 (R-22)"; ebExpireDryRun 同步标注 (仅 t_hash.c:1307) |
| 16 | **表述错误** | 02 节 4 "ExpireMeta 48bit + **5 位标志**" — 位域实为 4 单标志 (lastInSegment/firstItemBucket/lastItemBucket/trash) + numItems:5 + userData:3 | 精确化为位域明细 |
| 17 | **行号区间** | 01 节 5 getKeys "L2133-2284" 不含 legacy range (L2379-2421) | 改 L2133-2442 |
| 18 | **表述误导** | 01 节 5 "hashTypeAddToExpires (db.c:1446 等)" — db.c 只是 RENAME/MOVE/COPY 特例, 常规注册在 t_hash.c (hashTypeConvert 内联 ebAdd L1624-1662 + hashTypeSet L1229-1231) | 02 节 5 修正为 t_hash.c:2040 唯一入口 + 常规/特殊路径分解 |
| 19 | 边界 | 01 节 4 "PERSIST 语义" 未注实现位置 — PERSIST 命令在 expire.c | 加注 "(PERSIST 命令面在 expire.c, R-22 边界)" |
| 20 | 通过项 | 其余 ~40 句机制描述逐句对源码一致 ✅ (三态/从库豁免/静态键转换/键共享/游标 48+bits/Fenwick 二分/换表法均验证) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **10 处新发现** (前向依赖 1 / 覆盖缺口 2 / 编造 1 / 表述 3 / 区间 1 / 边界 1 / 精确化 1), 全部修复。反写测试结论: 大纲机制面完整可支撑写作, 数据流可追溯。
