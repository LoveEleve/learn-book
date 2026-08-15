# q8 — Session 持久化/投影(深度版:write-behind + 双后端 + 损坏语义)

> 域:④知识库(存储) | 文件:packages/session/(persistence/coordinator.ts 1361 + write-behind 159 + persistence-jsonl 967 + persistence-sqlite 414 + projection 428)+ docs/persistence-catalog.md
> review 轮次:2 轮(源码全文核心)

---

## 假设

持久化 = 插件(订阅 session/event + drain 时 flush)。核心 = PersistenceCoordinator(后端可换,TornMarker 泛型)+ SessionWriteBehind(有界每会话写批量)。双后端:JSONL(zstd 压缩)vs SQLite。损坏/版本不符显式错误。

## 验证

### 1. WriteBehind(设计 1:批量 + 耐久屏障)

```ts
// write-behind.ts:8-46:
SessionWriteBehind:每 live 会话的 pending 事件/固定批量截止/active 写/失败保留/显式 quiescence 屏障
  pending: 队列事件;timer: 批量截止(maxDelayMs)
  active: 当前写;barrier: 显式静止屏障(flush 用)
  write: 持久一个稳定有序前缀——resolve 只在 backend durability 后
  reportBackgroundFailure: 观察后台写失败而不拒绝生产者(非阻塞生产者)
// coordinator.ts:30:DEFAULT_WRITE_BATCH_MAX_DELAY_MS = 200
//   MAX_WRITE_BATCH_DELAY_MS = MAX_TIMER_DELAY_MS
```

**设计要点**:写失败不阻塞 agent 循环(后台报告);flush 提供显式屏障(会话关闭/检查点用)。与 OpenCode 的"事件+投影同事务"对比:这里批量延迟 200ms,耐久性靠 flush 显式。

### 2. 协调器(设计 2:后端抽象)

```ts
// coordinator.ts:84-127:
PersistenceBackend<TornMarker>: 后端接口(泛型 torn 标记)
StoredPrefix<TornMarker> / StoredSuffix: 存储前缀/后缀(撕裂检测)
SessionPersistenceCorruptionError / SessionFormatUnsupportedError: 损坏/版本错误
sessionFormatVersionRefusal(id, version): 版本拒绝消息
// 准备缓存:DEFAULT_PREPARED_SESSION_CACHE_SIZE = 5
```

### 3. 双后端(设计 3:JSONL vs SQLite)

```ts
// persistence-jsonl(967):行式 + zstd 压缩(zstd.ts/zstd-private-decoder/zstd-public-decoder)
//   ——zstd 私有/公共解码器分离(格式演进)
// persistence-sqlite(414 + schema.ts 270):
//   表:persistence_state / sessions / events(schema.ts:117-136)
// SCHEMA_VERSION 单调(AGENTS.md:后端拒绝旧格式)
// 选择:部署差异(web 用 SQLite?headless 用 JSONL?)
```

### 4. 投影(设计 4:session-projection 428)

```ts
// projection:从事件日志投影派生状态(与 core/session 的 surface 投影互补)
//   surface = 模型可见消息;projection = 会话派生视图(标题/统计/检查点)
// projection-cache:投影缓存
// checkpoint-policy:检查点策略(压缩/裁剪触发)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | WriteBehind 批量 + 耐久屏障 | write-behind.ts | ④写性能/耐久 |
| 2 | 后端抽象(TornMarker + 撕裂检测) | coordinator.ts:84-127 | ④存储可换 |
| 3 | 双后端(JSONL+zstd vs SQLite) | persistence-jsonl/sqlite | ④部署差异 |
| 4 | 损坏/版本显式错误 | coordinator.ts:36-77 | ④失败语义 |
| 5 | 投影 + 检查点策略 | projection/checkpoint-policy | ④派生视图 |

## 面试弹药

- "写失败不阻塞生产者":后台报告——agent 循环不等磁盘(与 OpenCode 的事件同事务对比:这里耐久性 = flush 显式屏障)
- "稳定有序前缀 + 撕裂检测":write 持久前缀,resolve 只在后端耐久后——崩溃恢复可重放
- "zstd 私有/公共解码器分离":格式演进不破坏旧日志
- "后端是插件":JSONL/SQLite 按部署选——持久化实现与核心解耦

## 待深挖

- [ ] coordinator 的完整状态机(撕裂恢复)
- [ ] checkpoint-policy 的触发条件
- [ ] sqlite vs jsonl 的选择逻辑
