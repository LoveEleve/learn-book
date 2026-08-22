# Elasticsearch 源码学习范围规划 — 六层审查报告

> 审查基线：`Elasticsearch源码学习范围规划.md`（11 域，R1+R2）
> 源码版本：v8.12.2，`/data/workspace/source-code/code/spring/elasticsearch/`
> 审查日期：2026-08-21

---

## 1️⃣ 事实审

### 仓库事实核实

| 项目 | 规划值 | 实际值 | 差异 |
|------|:------:|:------:|:----:|
| server 模块 Java 文件数 | 4023 | **6421** | +59% |
| index/ 包文件数 | 613 | **1038** | +69% |
| search/ 包文件数 | 809 | **1325** | +64% |
| cluster/ 包文件数 | 295 | **577** | +96% |
| action/ 包文件数 | 844 | **1249** | +48% |
| common/ 包文件数 | 493 | **694** | +41% |
| indices/ 包文件数 | 90 | **182** | +102% |

结论：规划文档的仓库事实数据**严重过时**，所有包的文件数都比规划值大 40%~100%。建议以实际值 6421 文件为准。

### 关键类存在与行数

| 类 | 规划 | 实际 | 状态 |
|----|:----:|:----:|:----:|
| InternalEngine | — | 3378 行 | ✅ |
| Translog | — | 1941 行 | ✅ |
| IndexShard | — | 4242 行 | ✅ |
| SearchService | — | 1825 行 | ✅ |
| QueryPhase | — | 270 行 | ✅ |
| RoutingTable | — | 654 行 | ✅ |
| ClusterState | — | 1195 行 | ✅ |
| CoordinationState | — | 656 行 | ✅ |
| ReplicationOperation | — | 698 行 | ✅ |
| BulkProcessor | — | 540 行 | ✅ |
| IndexFieldData | — | 255 行 | ✅ |
| ElasticsearchConcurrentMergeScheduler | — | 存在 | ✅ |
| MergePolicy | 规划列在 E-8 | **不存在**（Lucene 类） | ⚠️ |

### 行号偏差

- `InternalEngine.planIndexingAsPrimary()` 规划写 L530，实际在 **L1314**（版本差异）
- `Translog.newSnapshot()` 规划写 L657，实际在 L657 ✅

### 修正建议

1. 仓库事实数字以实际值 6421 文件/1038 index/1325 search/577 cluster 为准
2. E-8 MergePolicy 改为 `TieredMergePolicy`（Lucene 类）+ `ElasticsearchConcurrentMergeScheduler` 组合
3. 所有行号以写作时重新核对为准

---

## 2️⃣ 因果审

11 域因果链：

```
E-3 Translog(WAL)
  → E-1 Engine(写入引擎，依赖 Translog)
    → E-5 ShardLifecycle(分片生命周期，包含 Engine)
      → E-6 SeqNo&复制(复制协议，依赖 Engine)
        → E-4 Routing(分片路由，依赖 ClusterState)
          → E-2 Search(查询路径，依赖 Engine/Routing)
            → E-7 Mapping(文档映射，被 Engine 和 Search 依赖)
              → E-8 MergePolicy(段合并，Engine 后台任务)
                → E-9 BulkProcessor(批量写入，依赖 Engine)
                  → E-10 ClusterState(集群状态，基础设施)
                    → E-11 FieldData(聚合/排序性能，依赖 Search)
```

因果链成立，E-7 Mapping 被写入和查询双路径依赖，E-10 ClusterState 是路由和复制的基础。

---

## 3️⃣ 结构审

11 域按"写入路径→分片→复制→路由→查询→优化→集群"组织，主层次清晰。E-7 Mapping 在写入和查询之间，阅读顺序放在 E-2 之后合理。

---

## 4️⃣ 读者审

阅读顺序：`E-3→E-1→E-5→E-6→E-4→E-2→E-7→E-8→E-9→E-10`

前置依赖：需要了解 Lucene 基本概念（倒排索引、Segment、IndexWriter）。
与已有卷关系：vol-redis R-25（缓冲区）、vol-redisson R-4（命令执行）无直接交叉。

---

## 5️⃣ 边界审

淘汰清单合理（discovery/gateway/http/transport/monitor/script/ingest/snapshot/plugins 等均为 ES 特有或基础设施，不纳入搜索引擎内核主线）。

---

## 6️⃣ 依赖审

无循环依赖。E-10 ClusterState 是基础设施被 E-4 Routing 依赖，E-1 Engine 是核心被 E-5/E-6/E-9 依赖。

---

## 结论

| 审层 | 结果 | 关键发现 |
|:----:|:----:|---------|
| 事实审 | ⚠️ | 仓库事实数据过时（+40%~100%），MergePolicy 为 Lucene 类不存在，planIndexingAsPrimary 行号偏移 |
| 因果审 | ✅ | 因果链成立 |
| 结构审 | ✅ | 11 域组织合理 |
| 读者审 | ✅ | 阅读顺序合理 |
| 边界审 | ✅ | 淘汰清单合理 |
| 依赖审 | ✅ | 无循环依赖 |

**1 个阻塞性问题**：E-8 MergePolicy 的关键锚点 `MergePolicy.java` 是 Lucene 类，ES 源码中不存在。需要修正为 `TieredMergePolicy`（Lucene）+ `ElasticsearchConcurrentMergeScheduler`（ES）组合。
**2 个非阻塞性修正**：仓库事实数据更新、行号以写作时为准。
ENDOFFILE