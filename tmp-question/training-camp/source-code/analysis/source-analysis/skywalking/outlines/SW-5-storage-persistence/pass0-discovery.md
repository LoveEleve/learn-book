# SW-5 Storage / Persistence — Pass 0 发现

> 模块族: `oap-server/server-storage-plugin/*` + `server-core storage abstractions`
> 日期: 2026-08-18

## 1. 域定位

`SW-5` 负责 OAP 的持久化与查询存储边界，不是单一插件，而是一组抽象 + 多实现：

### 1.1 server-core 抽象
- `StorageModule`
- `StorageDAO`
- `StorageBuilder` / `StorageBuilderFactory`
- `StorageModels`
- `StorageID`
- `PersistenceTimer`
- `StorageTTLStatusQuery`
- 各类 `I*DAO` 查询接口

### 1.2 主要存储实现族
- `storage-banyandb-plugin`
- `storage-elasticsearch-plugin`
- `storage-jdbc-hikaricp-plugin`
- 以及其 provider / installer / common dao 层

## 2. 这不是一个小域

SW-5 会天然膨胀成多个机制完全不同的子系统：
1. 核心存储抽象与模型
2. 写入批处理与持久化调度
3. 查询 DAO 抽象与 debug/query side 行为
4. BanyanDB 存储实现
5. Elasticsearch 存储实现
6. JDBC/MySQL/PostgreSQL 存储实现
7. TTL / 历史清理 / schema installer

因此不能直接整域吞并，后续必须拆分子域审计。

## 3. 当前可见主链

```text
analyzer/query service
  -> server-core storage/query DAO interface
     -> StorageModule provider
        -> storage plugin implementation
           -> concrete backend client / SQL / ES / BanyanDB
```

写入链还包含：

```text
worker data
  -> PersistenceTimer
     -> StorageDAO / batch persist path
        -> storage plugin write implementation
```

## 4. 当前已知相关入口

### 4.1 核心抽象
- `server-core/src/main/java/.../storage/StorageModule.java`
- `server-core/src/main/java/.../storage/StorageDAO.java`
- `server-core/src/main/java/.../storage/model/StorageModels.java`
- `server-core/src/main/java/.../storage/PersistenceTimer.java`

### 4.2 查询接口使用侧
- `ZipkinQueryService` 使用 `IZipkinQueryDAO` / `ISpanAttachedEventQueryDAO`
- `ProfileTaskQueryService` / `AsyncProfilerQueryService` 使用 profiling DAO
- 各 query service 通过 `StorageModule.NAME` 拉取 DAO

### 4.3 插件实现族
- JDBC：`storage-jdbc-hikaricp-plugin`
- ES：`storage-elasticsearch-plugin`
- BanyanDB：`storage-banyandb-plugin`

## 5. 当前测试现实

从首轮文件盘点看：
- ES 有若干 util/base 测试
- JDBC 有少量 installer/history delete/driver 测试
- BanyanDB 有少量 util 测试
- 但大量核心 DAO / provider / batch write / query edge 仍可能依赖集成测试或缺少专项 harness

这说明 SW-5 的真实风险很可能藏在：
- 接口抽象与实现不一致
- backend-specific query/write 行为分叉
- history delete / TTL / schema install 边界
- batch persist 与 partial failure 语义

## 6. 首轮质疑点

### Q1: `PersistenceTimer` / batch write 是否在失败、并发和空批次下保持正确语义
### Q2: `StorageModels` 与 backend-specific model extension 是否一致
### Q3: DAO query 接口在各 backend 中是否语义等价
### Q4: Zipkin / profiling / log query 等特殊 DAO 是否存在 backend 分叉缺陷
### Q5: JDBC/ES/BanyanDB 的 installer、history delete、TTL 语义是否一致
### Q6: provider 注册的 service 列表是否与上游 query/analyzer 预期完全匹配
### Q7: debug/coldStage/trace-related query 是否只在部分 backend 生效却未被正确隔离

## 7. 建议拆域

下一轮建议先拆成：
- `SW-5A storage core abstraction + persistence timer`
- `SW-5B JDBC storage family`
- `SW-5C Elasticsearch storage family`
- `SW-5D BanyanDB storage family`
- `SW-5E special DAOs (zipkin/profiling/history delete/ttl)`

其中应优先从 `SW-5A` 开始，因为它定义所有实现的共同契约，也是最容易产出“抽象层真实缺陷”的位置。

## 8. Pass0 结论

SW-5 已确认是必须拆分的大域；当前不进入任何具体 backend 深挖。下一步应以 `SW-5A storage core abstraction + persistence timer` 作为首个子域，先做契约、批处理和 query DAO 装配边界审计。
