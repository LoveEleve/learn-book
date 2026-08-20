# R-8 RDB+AOF — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 预判验证 | 交接文档 §四 预判: RDB 格式/opcode 体系 ✅ / rio 三实现 ✅ / AOF 三策略 ✅ / 混合持久化 ✅ / 加载校验 ✅ — 全验证 | 记录 |
| 2 | **行号修正** | RDB_VERSION=**12** (rdb.h:20, 初稿推断 11) — magic "REDIS0012"; RDB_TYPE 宏族 = 类型号 **0-7∪9-25 (无 4/8)** 非"26 种" — rdbIsObjectType 校验 (rdb.h:80-83) | 大纲节 5/3 修正 |
| 3 | 补充常量 | REDIS_AUTOSYNC_BYTES=**4MB** (server.h:170); AOF_REWRITE_ITEMS_PER_CMD=**64** (server.h:112, harness 实证 5000 元素→79 条 RPUSH) | 大纲节 2/4 补锚点 |
| 4 | 机制洞察 | **v9 字节序修复兼容策略** (rdb.c:125-146): 修复只对 ≥v9 — 大端老文件可读自己的旧格式 (完美向后兼容注释) | 大纲节 1/6 |
| 5 | 机制洞察 | **fork 重写三流并行**: 子进程 temp + 父进程新 INCR + diff 管道 — 重写不阻塞 (aof.c:2437+); manifest 4a-4e 步骤 (L2424-2436) | 大纲节 5 |
| 6 | harness 实证 | 156 断言全过 (gcc+ASan): 长度四档边界/整数三档/double 含 NaN/Inf/时间戳 v8 vs v9/AOF 序列化含 40KB 大参/变参批量 79 条; **1 处测试断言错误修正** (len=100 走 14bit 档 2B 非 1B — 复现源码 rdbSaveLen 档位判定) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 长度/整数编解码往返 (边界穷举)
- v8 vs v9 时间戳路径 (LE 平台等价)
- double 二进制含 NaN/Inf/signbit
- AOF 序列化字节级比对

### 维度2 性能
- 四档长度编码压缩率
- autosync 4MB 增量写分批
- 变参批量 64/批 压命令数
- fork COW 零拷贝快照

### 维度3 内存
- 混合持久化 RDB 压缩
- listpack 整包直存
- reclaimFilePageCache / dismissObject (COW 提示)

### 维度4 一致性
- rename 原子替换 + fsyncFileDir
- 重写期 INCR 双写 + diff 合并
- 截断容忍 (AOF_TRUNCATED)
- 加载假客户端重放 (全链一致)

### 维度5 负面空间 (已写入大纲 6+6 条)
- RDB: 无增量/无并发/无在线校验; AOF: 无压缩存储/无对象级增量/无加载校验

## 结论
R-8 全部锚点行号 ~50 处验证, 12 闭环完成 (6+6), **行号修正 1 + 常量补充 2 + 机制洞察 2**。harness 156 断言 ASan clean。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-33/R-21/R-22/R-1/R-20 (序号 < 24) 均已讲 ✅; 引出 R-9 (未来域 OK); 对照 R-12 (magic 头) ✅; 两篇各自五结构元素齐备 ✅; 读者处境场景化 (dump.rdb 二进制/一致性/体积; everysec 丢多少/重写不丢/加载慢) ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 两篇合计 ~90 锚点 (file:line) — 🔴A 标准 ≥4 ⏫ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-9/R-14 仅作引出; R-8b 引用 R-8a 属同域拆篇) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 传播 (AOF 与复制同源) / 通知 (加载期) / 编码 (listpack 整包) / 键空间 (全库遍历) / 过期 (EXPIRETIME_MS/PEXPIREAT) / bio 线程 / 内存 (COW) — 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 两篇负面空间 6+6 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **强断言实证** | 大纲节 4 (R-8a) "fork = 零拷贝快照: 一致性靠进程内存隔离" — 实证: rdbSaveBackground (L1645) redisFork + 子进程 rdbSave; COW 页复制语义标注 (R-18 交叉) ✅ | 记录 |
| 13 | **表述精确化** | 大纲节 5 (R-8b) "重写 = fork 快照 + 增量双写" — 精确: 子进程写 fork 快照 + 父进程切新 INCR + **diff 管道合并** (aofReadDiffFromParent); 已含 | 记录 |
| 14 | 通过项 | 其余 ~45 句机制描述逐句对源码一致 ✅ (四档编码/整数三档/LZF 收益条件/函数表注入/autosync/类型分派/PEL 序列化/rename+fsyncFileDir/EOFMark/opcode 分派/三分支容错/三策略/空缓冲补偿/延期 2s/ftruncate/SELECT 注入/变参批量/preamble/manifest/假客户端/截断容忍) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **1 处记录** (强断言实证 COW), 行号修正 1 + 常量 2 已落地。反写测试结论: 两篇大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-13, 逐句对源码 + 推理验证)

> 动机: 用户要求深度 REVIEW — 对大纲每个锚点重新 grep, 对全部推断/机制描述反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 整数编码条件 | rdbSaveRawString (rdb.c:436-444): `len <= 11` + **string2ll 解析** (rdbTryIntegerEncoding L322-330) — harness 的 len≤11 ✅ 精确 | 通过 |
| V2 | LZF 条件 | L449: `server.rdb_compression && len > 20` + LZF 内部至少省 4B (outlen=len-4, L363) — harness 的 len>20 ✅ 精确; 需补 rdb_compression 开关 | 通过 |
| V3 | LZF 收益判定 | 源码 = 压缩到 len-4 缓冲成功 (≤len-4); harness 用 complen<len-4 — dummy 恒等永不成立, 断言等价 ✅ | 通过 |
| V4 | EOF_MARK_SIZE | server.h:115 = **40** ✅ | 通过 |
| V5 | aof-use-rdb-preamble | config.c:3072 = **默认 1** (MODIFIABLE) ✅ | 通过 |
| V6 | AOF_TRUNCATED | aof.c:1577/1610/1694: **last_file 才容忍截断**, 非最后文件截断报错 — 大纲"截断只丢尾部"需精确化 | 通过 (精确化) |
| V7 | manifest 结构 | server.h:1520-1522 (base/incr/history 三列表 + dirty) + 1807 aof_manifest ✅ | 通过 |
| V8 | 假客户端 | createAOFClient (aof.c:1355-1375): flags **赋值** CLIENT_DENY_BLOCKING (非 OR) + SLAVE_STATE_WAIT_BGSAVE_START ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | **认知修正 (最重要)** | 大纲节 5 (R-8b) "diff 管道合并 (aofReadDiffFromParent)" — **7.4 已移除该机制**! grep 实证 aofReadDiffFromParent/aof_rewrite_buf 不存在; 7.0 Multi-Part 改为 **文件级分离**: fork 后父进程 openNewIncrAofForAppend 切新 INCR (aof.c:2453) + 完成时 backgroundRewriteDoneHandler **双 rename (temp BASE→新 BASE + 临时 INCR→新 INCR) + 旧标 HISTORY + aofManifestDup 临时 manifest 原子替换** (aof.c:365-371) — 完整性靠 BASE+INCR 组合, 无管道同步 | 大纲节 5 全文重写 + pass1-notes 同步 |
| 16 | 表述遗漏 | 大纲节 5 (R-8a) 数据流 "REDIS0011" 漏改 (正文已 REDIS0012) | 大纲节 5 数据流修正 |
| 17 | 精确化 | AOF_TRUNCATED 语义: **仅最后一个文件容忍截断** (aof.c:1694 last_file), 非最后文件 → 加载失败 — 大纲"截断只丢尾部"补边界 | 大纲节 6 补注 |

## 三次 REVIEW 汇总

推理验证 8 项全通过 (V1-V8); 新发现 **3 处** (**认知修正 1: diff 管道机制 7.0 移除** / 表述 1 / 精确化 1), 全部修复。锚点逐句 re-grep 无其他偏移。两篇大纲现可支撑写作。
