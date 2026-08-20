# E-1 Index Engine — 全视角提问验证 (completeness)

> 验证时机: 3 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生
> 覆盖统计: 见文末

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 写入 | index() 持哪些锁? 粒度? | ✅ 01-L1 (三级锁 01-L1 InternalEngine.java:1134-1141) |
| 2 | 开发者 | 写入 | plan 五分支怎么选? | ✅ 01-L2 (五分支 L1314-1383) |
| 3 | 开发者 | 写入 | 执行失败返回什么? (IndexResult FAILURE?) | ✅ 01-L3 (InternalEngine.java:1428) |
| 4 | 开发者 | append-only | 重试文档怎么防重复? | ✅ 01-L4 (maxUnsafeAutoIdTimestamp) |
| 5 | 开发者 | 版本 | LiveVersionMap 存什么? | ✅ 03-L1 (version/seqNo/term/location) |
| 6 | 开发者 | 失败 | 副本文档失败为什么 tragic? | ✅ 03-L4 (不对称根因) |
| 7 | 架构师 | 两阶段 | 为什么计划与执行分离? | ✅ 01-L2 (主副共用决策) |
| 8 | 架构师 | NRT | realtime get vs search 为什么不同? | ✅ 02-L2/L3 (版本表 vs 新 reader) |
| 9 | 架构师 | flush | refresh/flush/commit 三层语义? | ✅ 02-L4 |
| 10 | 架构师 | 版本 | 双 map 为什么不用锁? | ✅ 03-L2 (volatile 发布) |
| 11 | 性能工程师 | 写入 | append-only 省了什么? | ✅ 01-L4 (跳过版本查找) |
| 12 | 性能工程师 | refresh | refresh 1s 的代价? (内存/CPU?) | ⚠️ 02-L3 提了段 reader 未量化 → 补一句 (新段持有已删文档) |
| 13 | 性能工程师 | flush | flush 阈值怎么定? (512MB/1min) | ✅ 02-L4 |
| 14 | SRE | 失败 | tragic 后 shard 什么状态? | ⚠️ 03-L4 未提运维 → 补一句 (引擎失效→shard failed, E-5 展开) |
| 15 | SRE | 恢复 | 崩溃后 translog 回放范围? | ✅ 02-L4 (commit 安全水位) + E-3 承接 |
| 16 | 研究者 | 对照 | Redis RDB/AOF vs ES commit/translog? | ✅ 02-L4 + 03-L6 |
| 17 | 研究者 | 时空 | v0.90 单 map → 双 map 演进原因? | ✅ 02-L5 + 03-L2 |
| 18 | 子系统开发者 | 衔接 | translog.add 在 index() 哪一步? | ✅ 01-L3 (InternalEngine.java:1221 先日志) |
| 19 | 子系统开发者 | 衔接 | seqNo 谁生成? (E-6) | ✅ 01-L5 (InternalEngine.java:1105) |
| 20 | 子系统开发者 | 衔接 | soft deletes 供谁消费? (E-5/E-8) | ✅ 03-L5 |
| 21 | 学生 | NRT | 写入后多久能搜到? | ✅ 02-L1 (1s) |
| 22 | 学生 | flush | "ES 重启丢数据吗"? | ✅ 02-L4 (translog 回放) |
| 23 | 学生 | 版本 | 版本号怎么递增? | ⚠️ 01 未提 updateVersion → 补一句 (versionType.updateVersion L1376 区) |
| 24 | 学生 | append-only | 为什么叫 append-only? | ✅ 01-L4 (跳过查找直接加) |
| 25 | 学生 | 失败 | 主分片写坏文档会挂掉整个 shard 吗? | ✅ 03-L4 (不会, 单请求失败) |

**统计**: ✅ 22 / ⚠️ 3 / ❌ 0 — ⚠️ 项全部"补一句"级
→ 回补 3 项: 02-L3 refresh 内存代价 / 03-L4 tragic 后 shard failed (E-5 衔接) / 01-L3 版本递增 updateVersion

## 回补清单

1. 01-L3: 补 versionType.updateVersion (版本递增, L1376 附近 plan.processNormally)
2. 02-L3: 补 refresh 内存代价一句 (新段 reader 持有已删文档直至 merge)
3. 03-L4: 补 tragic 后 shard 进入 failed 状态 (E-5 衔接)
