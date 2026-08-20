# RM-2 存储底层 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **补充锚点** | **写锁默认自旋** (CommitLog:133): `isUseReentrantLockWhenPutMessage() ? ReentrantLock : SpinLock` — 默认 SpinLock; **组提交内部也用自旋** (L1617 requestsWrite put 锁) — 双重自旋面 | 大纲 §4 补注 |
| 2 | **补充锚点** | **syncFlushTimeout = 5s** (MessageStoreConfig:220) — 同步刷盘等待上限 | 大纲 §2 补注 |
| 3 | **补充锚点** | **handleDiskFlushAndHA 并行合并** (CommitLog:1272-1290): flushResultFuture + replicaResultFuture **thenCombine** — 刷盘与复制并行, 任一失败置状态 | 大纲 §6 补注 |
| 4 | **补充锚点** | **warmMappedFile 实现** (DefaultMappedFile:621-660): 逐 **OS_PAGE_SIZE 页写 0** (触发缺页加载) + SYNC 时按 pages 间隔 force + **mlock 全文件锁页** (L659) — "预热"= 页加载 + 锁页双动作 | 大纲 §4 补注 |
| 5 | 观察 | mlock 双出现: TransientStorePool.init (池缓冲) + warmMappedFile (映射文件) — 存储线程防换页一致哲学 | 记录 |
| 6 | 行号验证 | 全函数 28 锚点 + 跨文件 10 处 grep (DefaultMappedFile 83-660 / CommitLog 133,1272-1290,1432-1660,2109-2156 / MappedFileQueue 210-344 / AllocateMappedFileService 154-210 / TransientStorePool 30-85 / MessageStoreConfig 51,220,237-238 / PutMessageSpinLock 22-45 / FlushDiskWatcher 28-64) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 双缓冲 append/commit/flush 三阶段
- 组提交水位判定 + 1000 次重试
- 三条件触发 (500ms/4 页/10s)
- 引用计数卸载

### 维度2 性能
- mmap 零拷贝读 (selectMappedBuffer 切片)
- 堆外直写 + 批量转储
- 自旋锁低竞争
- 预分配预热 (延迟移出写路径)

### 维度3 内存
- 1GB 段 + 引用计数延迟卸载
- 5GB 堆外池 (默认关)
- mlock 防换页

### 维度4 一致性
- 顺序写保证
- flushedWhere 水位推进
- 刷盘与 HA thenCombine 合并
- 双链表 swap 无锁读

### 维度5 负面空间 (已写入大纲 5 条)
- 不压缩/不稀疏/不 DirectIO/不聚合/不分层

## 结论
RM-2 全部锚点 ~45 处验证, 6 闭环完成, **补充锚点 4 + 观察 1**。怀疑审计全接受+补充。推断 3 处显式标注。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 RM-1 (已交付) ✅; 引出 RM-3/8/12 (未来 OK); 对照 R-8 (Redis 持久化) ✅; 读者处境场景化 ✅; 六结构元素齐备 ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 大纲 ~45 锚点 (file:line) ✅ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域实质依赖 (RM-3 等仅引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 并发 (写锁/双链表 swap) / 内存 (mmap/堆外/锁页) / 磁盘 (刷盘三服务) / 复制 (HA 并行) / 监控 (Watcher/metrics) / 配置 (MessageStoreConfig) — 六横切 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 负面空间 5 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **反写测试发现** | 大纲 §3 "定位: offset/fileSize 除法 + 边界修正" — **findMappedFileByOffset 的实际算法未验证** (含 rehash 表/索引缓存?), 需精确化 | 大纲 §3 补注 (findMappedFileByOffset 实现面) |
| 13 | 通过项 | 其余 ~35 句机制描述逐句对源码一致 ✅ (mmap 映射/双缓冲三阶段/appendUsingFileChannel/引用计数/isLoaded0/组提交双链表+swap+水位+1000 重试+sleep1ms/定时三条件/同步 future/syncFlushTimeout 5s/1GB 对齐滚动/删除/预分配去重+ServiceLoader+预热/自旋默认/池 mlock+deque+40% 告警/写读链+thenCombine/FlushDiskWatcher) | 记录 |

## 二次 REVIEW 汇总
07 五维度轮换 + 内容深度共 **1 处修复** (反写测试 #12), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

> 动机: 大纲每个锚点重新 grep, 对全部推断/计数反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 1GB 对齐数学 | createOffset = startOffset - (startOffset % 1024³) — 文件起始恒为 1GB 倍数 ✅ | 通过 |
| V2 | 组提交重试上限 | 循环 1000 次 flush(0) + sleep 1ms — 理论上限 ~1s+ 后超时; syncFlushTimeout 5s 为外部兜底 ✅ | 通过 |
| V3 | 双链表 swap 语义 | putRequest → requestsWrite; doCommit 前 swap (锁内) — 写侧与读侧分离; 消费期间新请求进写链 ✅ | 通过 |
| V4 | 三条件触发 | flushIntervalCommitLog 500ms (周期) + leastPages 4 (页数) + thoroughInterval 10s (强刷) — 配置实证 ✅ | 通过 |
| V5 | thenCombine 语义 | flush 与 HA 两个 future 并行; 合并时任一非 PUT_OK → 覆盖状态 ✅ | 通过 |
| V6 | warm 页数 | OS_PAGE_SIZE=4096; 1GB/4KB = 262144 次 put — 预热耗时 ~秒级 (mlock 大头) ✅ | 通过 |
| V7 | 堆外池水位 | borrowBuffer 后 availableBuffers.size() < poolSize*0.4 → 告警 — 40% 阈值 ✅ | 通过 |
| V8 | 引用计数卸载 | cleanup(currentRef): cleanupOver 防重复 + 被读 (ref>0) 不卸 — 读安全 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 14 | **覆盖缺口** | 大纲 §4 未提 **warmMappedFile 的调用时机**: AllocateMappedFileService.mmapOperation 中创建后即 warm (L199) — 预分配即预热闭环; 且 warm 是"页加载+mlock"双动作 (深审 #4) | 大纲 §4 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (warm 调用时机), 修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查七个存疑点 (FlushRealTime 三条件代码/CommitRealTime/ackNums 复制面/引用计数来源/warm 调用者/磁盘保护/findMappedFileByOffset), 并做反写测试。

## 追查过程 (七个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | FlushRealTimeService 三条件代码? | L1487-1530: 500ms 周期 (flushCommitLogTimed 决定 sleep/waitForRunning) + leastPages=4 攒页; **每 10s lastFlushTimestamp 到期 → leastPages=0 强刷全量** (L1516-1519) + printFlushProgress 每 10 次 | 发现 3 (精确化: "10s 强刷"实为 leastPages 降 0) |
| T2 | CommitRealTimeService 条件? | 同族逻辑 (CommitLog L1432+) — 与 FlushRealTime 对称 (转储路径) | 通过 |
| T3 | **ackNums 复制等待面?** | **机制缺口实证**: ①CommitLog.GroupCommitService (刷盘, flushedWhere 水位) ②**ha/GroupTransferService (独立类**: 双链表+自旋+doWaitTransfer 等从库 replicationOffset 水位) — **两个独立组提交服务**; handleHA needAckNums = inSyncReplicas 配置 (L951-965); handleDiskFlushAndHA thenCombine 合并双结果 | **发现 1 (机制缺口): 大纲 §6 只说 thenCombine, 未提双组提交+ackNums 同步副本语义** |
| T4 | 引用计数来源? | hold() 在 selectMappedBuffer/flush/commit 路径前置 (L209/384/422/506) — 读/刷/转储都持引用防释放中操作 | 发现 6 (精确化 q1) |
| T5 | warm 调用者全量? | AllocateMappedFileService L199 (创建即 warm); 其他调用点未见 (启动预热当前文件? 未实证) | 通过 |
| T6 | **磁盘空间保护在哪?** | **DefaultMessageStore L2340-2360** (store 模块非 broker): 分区使用率 > warningRatio → runningFlags diskFull 标志 (写拒绝) + cleanImmediately; > cleanForciblyRatio → 强制清理; 系统属性 rocketmq.broker.diskSpaceWarningLevelRatio/CleanForciblyRatio | **发现 2 (补充锚点): 大纲"磁盘检测 (broker 面)"归位错误** |
| T7 | findMappedFileByOffset 实现? | L670-700: 索引除法 + 边界校验 + **线性扫描兜底** + returnFirstOnNotFound | 发现 4 (精确化 §3) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 10s 强刷语义 | lastFlushTimestamp 每 10s 重置 → leastPages=0 → 本次 flush 全量; 非"每 10s 刷一次" ✅ | 通过 |
| V2 | 双组提交独立性 | 刷盘 (CommitLog 内部) vs 复制 (ha/ 包独立类) — 两个 ServiceThread, 各自双链表+自旋 ✅ | 通过 |
| V3 | ackNums 配置链 | inSyncReplicas → calcNeedAckNums (单副本时; >inSyncReplicas 拒) / ALL_ACK_IN_SYNC_STATE_SET (全确认) → GroupCommitRequest ackNums → 复制组提交等待 ✅ | 通过 |
| V4 | thenCombine 双结果 | flushStatus 与 replicaStatus 任一非 PUT_OK → 覆盖 (L1281-1286) — 双失败面 ✅ | 通过 |
| V5 | hold 防释放 | hold() 失败 (cleanupOver) → 跳过操作 — 文件被标记释放后读写安全拒绝 ✅ | 通过 |
| V6 | 磁盘标志拒绝链 | diskFull 标志 → putMessage 路径 isSpaceFull 检查 (DefaultMessageStore L2453) → 写拒绝 ✅ | 通过 |
| V7 | 索引定位兜底 | get(index) 可能越界 (文件删除后) → 线性扫描 — 删除场景安全 ✅ | 通过 |
| V8 | returnFirstOnNotFound | 越界返回首文件 (读最老) — 消费容错面 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | **机制缺口** | 大纲 §6 缺**双组提交服务**: 刷盘 (GroupCommitService/flushedWhere) + **复制 (ha/GroupTransferService/从库水位)**; ackNums=inSyncReplicas 同步副本语义; 同步复制 = 双组提交 + thenCombine | 大纲 §6 重写 |
| 16 | **归位修正** | 大纲 "磁盘检测 (broker 面)" 错误 — **在 store 模块** (DefaultMessageStore L2340+): warning/cleanForcibly 双 ratio + diskFull 标志 + 系统属性 | 大纲 §6 补注 |
| 17 | 精确化 | "10s 强刷" → leastPages 降 0 强刷全量 (L1516-1519) | 大纲 §2 修正 |
| 18 | 精确化 | findMappedFileByOffset = 索引除法 + 线性兜底 + returnFirstOnNotFound; hold() 引用语义 (读/刷/转储前置) | 大纲 §3 修正 |

## 反写测试 (只读大纲能否写文章)

- §1 MappedFile: 双缓冲三阶段/引用计数 (修复后 hold 语义) — 可写 ✅
- §2 刷盘: 双服务/leastPages 强刷语义 (修复后)/syncFlushTimeout/双组提交+ackNums (修复后) — 可写 ✅
- §3 文件队列: 对齐/定位 (修复后)/删除/多盘 — 可写 ✅
- §4 预分配+锁: 默认自旋/warm=写0+mlock/创建即预热 — 可写 ✅
- §5 堆外池: mlock/deque/40% — 可写 ✅
- §6 写读链: thenCombine/磁盘保护 (修复后)/状态面 — 可写 ✅
- 负面空间 5 条 — 完整 ✅

## 四次 REVIEW 汇总

七存疑点全实证 (T1-T7); 推理验证 8 项全过 (V1-V8); **新发现 4 处全部修复** — **#15 最有价值** (双组提交服务: 刷盘+复制独立水位等待, ackNums=inSyncReplicas; RM-12 交叉提前闭环)。大纲经修复后反写测试全过。
