# Reasonix 域发现 v17 补充(续扫第三轮:checkpoint 源码验证)— 2026-08-14

> 承接:v16。本轮:checkpoint/(transaction.go 1,773/checkpoint.go 1,042/barrier.go)源码验证——rq7(14 设计)的源码级确认。
> 结论:rq7 正确;发现 1 个未记录细节(generation 代际),无新域。

---

## 一、v17 验证确认(checkpoint 源码)

| 设计 | rq7 验证 |
|------|---------|
| InjectFail(注入失败测试钩子) | ✅ transaction.go:23 |
| PrepareRewind/CommitRewind/UndoRewind 三阶段 | ✅ :45/:182/:242 |
| 双阶段事务(prepare → commit + publish temp/backup) | ✅ :644-651(writePublishTemp/transactionSiblingPaths) |
| 补偿发布(compensatePublished) | ✅ :973 |
| 指纹匹配(fingerprintMatches) | ✅ :1095 |
| **MutationBarrier** | ✅ barrier.go:15-150 |
| **generation 代际(新细节)** | barrier.go:35 — **每次独占释放递增,prepare token 靠 generation 检测并发变异(不靠墙钟)** |
| ActiveWriters/activeWriterConflicts | ✅ checkpoint.go:214-228(主动写者冲突检测) |
| Barrier()/Blobs() | ✅ :188-196(MutationBarrier + BlobStore 访问器) |

---

## 二、关键设计(通用价值)

1. **"generation 检测并发变异"**:prepare token 验证用单调递增 generation,不靠墙钟——**代际一致性**(与 Pi availabilityRefreshSeq、Hermes routing_generation、Reasonix uihub 代际同族——**四项目共证代际模式**)
2. **"读写互斥屏障"**:EnterWrite(多写者)/EnterExclusive(独占)/Try 非阻塞变体——**屏障的完整原语集**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v16 | — | 102 | 102 |
| v17 | checkpoint 源码验证 | +0(rq7 验证+1 细节) | **102**(验证) |

> 继续:next 轮 acp 完整服务(3,057)/config 细节(load 2,498)/history 检索层。
