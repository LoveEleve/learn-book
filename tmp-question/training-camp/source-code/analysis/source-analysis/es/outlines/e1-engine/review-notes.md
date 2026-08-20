# E-1 Index Engine — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 跨域引用核验
> **结论: 深审通过 (自查抓 7 处偏差, 裸行号 86 处根治, 修复后零残留)**

## 第一层: 行号偏差 (7 处, 全部自查抓出 — 写后即验的延续)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 | addStaleDocs (L1395) / updateDocs (L1399) / addDocs (L1410-1412) | **方法定义在 L1472/L1584/L1463** (调用点 vs 定义混淆) | 严重 |
| 2 | 02 大纲 | heavy-lifting 注释 (L2049-2051) | **L2035** | -14 |
| 3 | 03 大纲 | Maps 类 (L125-169) | **L112-175** (Maps 类 L112) | 行号 |
| 4 | 03 大纲 | buildTransitionMap (L146-152) | **L149-157** | -3 |
| 5 | 03 大纲 | invalidateOldMap (L165-169) | **L167-172** | -2 |
| 6 | 03 大纲 | tombstones (L211) | **L210** | -1 |
| 7 | 02 大纲 | refresh 内存代价未量化 | 补"旧段标记删除直至 merge" (completeness ⚠️) | 覆盖 |

## 第二层: 机制实证 (全过)

- index 三级锁 (L1135-1145) / planIndexingAsPrimary 五分支 (L1314-1383) ✅
- append-only 幂等 (L1059-1090 + 注释 L1143-1173) ✅
- realtime get 路径 (L865-924) / flush 四步 (L2184-2259) ✅
- LiveVersionMap 双 map (L149-172) / tombstones L210 / unsafeKeysMap L216 ✅
- 失败分级 treatDocumentFailureAsTragicError (L1432-1444) ✅
- updateVersion 版本递增 (InternalEngine.java:1375 + VersionType.java:251) ✅
- v0.90 RobinEngine 单 map (L182) / SearcherManager (L115) git show 实证 ✅

## 第三层: 编造检查 (零)

- RobinEngine→InternalEngine 更名 2014-01-13 (8247e4beaee) 日期实证 ✅
- v0.90 flushNeeded/flushing 字段 git show 实证 ✅

## 第四层: 覆盖缺口 (3 项 — completeness ⚠️ 已回补)

- 01-L3 versionType.updateVersion 版本递增 ✅
- 02-L3 refresh 内存代价 (旧段持有已删文档) ✅
- 03-L4 tragic 后 shard failed 运维视角 ✅

## 第五层: 裸行号 (铁律 9)

- 修复前: 86 处 `(Lxxx)` 简写 (7 文件)
- 修复后: **0 残留** (正则批量 + 手工处理多行号格式)
- 教训: 批量正则处理 `(L\d+)` 会漏多行号 `(L1352,1358,1368)` 与 `(L1090 附近)` 变体 → 修后必须二次扫描

## 第六层: 跨域引用核验

- redis/r8-persistence (持久化对照) / r9-replication (复制对照) / r21-db ✅
- redisson/rd2-rlock (看门狗对照) ✅
- E-3/E-7 内部域引用 (translog/parse 衔接) ✅
- 全部 [ -d ] 验证; 对照声明含"同词+一句摘要" ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 自查修复后复核

> 目的: 验证自查修复是否精确落地 + 是否遗漏 (沿 E-3/E-7 教训: 修复后必须再逐锚点 grep)

### 发现 10 处新偏差 (第一轮自查只验了方法起始行, 未验分支内部)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 | optimizedAppendOnly (L1323) | **L1325** | -2 |
| 2 | 01 大纲 | skipDueToVersionConflict ×3 (L1352,1358,1368) | **L1349,1360,1367** | 3 处偏移 |
| 3 | 01 大纲 | failAsTooManyDocs (L1326,1378) | **L1323,1371** | 2 处偏移 |
| 4 | 01 大纲 | processNormally (L1376) | **L1373** | -3 |
| 5 | 01 大纲 | index 三级锁 (L1135-1145) | readLock **L1134** / acquireLock **L1139** / throttle **L1140** | 区间偏移 |
| 6 | 03 大纲 | treatDocumentFailureAsTragicError (L1432-1444) | 方法定义 **L1435-1443** (L1432-1434 是注释) | -3 |
| 7 | 03 大纲 | 冲突三分支 (L1352,1358,1368 简写) | 同上 #2 | 格式 |
| 8 | pass2-q5 | index 锁 (L1136/L1140/L1141) | **L1134/L1139/L1140** | 同 #5 |
| 9 | pass1 | acquireLock (L1137-1140) | **L1139** | -1 |
| 10 | completeness | 3 处简写 (L1428/L1221/L1105) | 补 File.java 前缀 | 格式 |

### 已验证正确 (20+ 项)

- get (L827-845) / realtimeGetUnderLock (L865) / flush 内部 (L2184/2202/2212/2225/2259) / 双 ReaderManager (L140-141) ✅
- Maps L112 / buildTransitionMap L149 / invalidateOldMap L167 / tombstones L210 / unsafeKeysMap L216 ✅
- canOptimizeAddDocument L1059 / generateSeqNo L1105 / index L1131 / indexIntoLucene L1384 / IndexingStrategy L1483 ✅
- addDocs L1463 / updateDocs L1584 / addStaleDocs L1472 (第一轮已修) ✅

### 根因与根治 (再升级)

- **根因**: 第一轮自查只验证"方法起始行" (grep 签名), 未验证"方法体内分支行" — 分支行号靠 sed 输出推算, 偏差 1-3 行
- **根治方案 (终版)**: 三遍验证闭环 — ① 写大纲时每个锚点立即 grep ② 自查时方法起始 + **方法体内关键行**都 grep ③ REVIEW-2 逐锚点复核 (本流程)。**方法体内行号必须用 grep 关键词定位, 不用偏移推算**
- harness 15/15 重跑通过 / 行号上限检查 OK / 裸行号零残留 / 跨域引用 4 个全部 [ -d ] 通过
