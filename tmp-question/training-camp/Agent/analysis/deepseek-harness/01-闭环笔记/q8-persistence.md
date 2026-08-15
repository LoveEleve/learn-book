# q8 — Session 持久化/投影(深度版:write-behind + 双后端 + 测试契约)

> 域:④知识库(存储) | 文件:packages/session/(persistence/coordinator.ts 1361 + write-behind 159 + persistence-jsonl 967 + persistence-sqlite 414 + projection 428)+ tests/(persistence.spec.ts 1938 + coordinator-contract.ts 1482 + jsonl.spec.ts 1621 + sqlite.spec.ts 1819)+ docs/persistence-catalog.md
> review 轮次:3 轮(源码全文核心 + 双测试契约族)

---

## 假设

持久化 = 插件(订阅 session/event + drain 时 flush)。核心 = PersistenceCoordinator(后端可换,TornMarker 泛型)+ SessionWriteBehind(有界每会话写批量)。双后端:JSONL(zstd 压缩)vs SQLite。**测试契约族(coordinator-contract 1482 行 + persistence 1938)揭示 HMR/崩溃/fork/resume 语义**。

## 验证

### 1. WriteBehind(设计 1:批量 + 耐久屏障)

```ts
// write-behind.ts:8-46:
SessionWriteBehind:每 live 会话的 pending 事件/固定批量截止/active 写/失败保留/显式 quiescence 屏障
write: 持久一个稳定有序前缀——resolve 只在 backend durability 后
reportBackgroundFailure: 观察后台写失败而不拒绝生产者
// coordinator.ts:30:DEFAULT_WRITE_BATCH_MAX_DELAY_MS = 200
```

### 2. 协调器(设计 2:后端抽象 + 状态机)

```ts
// coordinator.ts:84-127:
PersistenceBackend<TornMarker>: 后端接口(泛型 torn 标记)
StoredPrefix/StoredSuffix: 前缀/后缀(撕裂检测)
SessionPersistenceCorruptionError / SessionFormatUnsupportedError
// states = Map<SessionId, SessionState>(meta/cursor/materialized)
// append seq 校验:cursor + i 不匹配 throw(coordinator.ts:699-700)
```

### 3. 测试契约(设计 3:coordinator-contract 1482 行)★ review 轮 3

```ts
// tests/coordinator-contract.ts(后端无关契约,JSONL/SQLite 共享):
1. 往返与所有权(228-313):崩溃恢复加载拒绝 live 拥有;冷加载后重查 live 所有权;
   未材料化空 live 会话不加载
2. 边界往返(325-348):种子边界(seedLength)与委托深度持久化
3. 冻结语义(373-397):source-frozen 事件缓冲后不可变;加载返回不可变 identified-message 快照
4. 旧日志兼容(427-473):pre-identity/pre-react-loop 日志加载为可恢复当前会话
5. 畸形消息拒绝(582):持久化消息在返回前拒绝
6. append 快照(745):调用方数组/事件突变被忽略
7. fork/resume(774-797):seed 一次(无双写);resume 续 seq(不 re-append seed)
8. HMR(830-947):种子现有 live 会话/dispose drain/重载采用/后缀持久化;
   **HMR adoption 不崩溃修复活跃开放 turn(截断无 closers)**
9. 碰撞拒绝(980):新 live 会话碰撞持久化 id → 拒绝,不静默采用
10. 放弃惰性会话释放 id(1006);disposal drain 后退休所有权(1035)
// persistence.spec(1938):批截止取消/在飞写期间入队后续批/失败重叠写显式 flush 屏障重试/
//   冷 id 跨异步修复保留/准备失效重试/修复后重载提交图/相同 id 链健康
```

**设计要点**:HMR 语义精确(adoption 不破坏开放 turn);fork/resume 无双重写;所有权碰撞拒绝;撕裂/修复/重载全路径有契约。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | WriteBehind 批量 + 耐久屏障 | write-behind.ts | ④写性能/耐久 |
| 2 | 后端抽象(TornMarker + 撕裂检测) | coordinator.ts:84-127 | ④存储可换 |
| 3 | HMR/所有权/碰撞契约(测试族) | coordinator-contract.ts:830-1035 | ④热重载安全 |
| 4 | fork/resume 无双重写 | coordinator-contract.ts:774-797 | ④分支/恢复 |
| 5 | 双后端(JSONL+zstd vs SQLite) | persistence-jsonl/sqlite | ④部署差异 |
| 6 | 投影 + 检查点策略 | projection/checkpoint-policy | ④派生视图 |

## 面试弹药

- "写失败不阻塞生产者":后台报告——agent 循环不等磁盘
- "HMR adoption 不崩溃修复开放 turn":截断无 closers——热重载不破坏活跃执行
- "fork seed 一次无双写":持久化幂等(no-op flush 不重写)
- "碰撞拒绝不静默采用":新 live 会话撞持久化 id → 显式拒绝
- "后端无关契约族":coordinator-contract 共享跑 JSONL/SQLite——双后端行为一致

## 待深挖

- [ ] jsonl 的 zstd 压缩细节
- [ ] sqlite 的 SCHEMA_VERSION 迁移
- [ ] checkpoint-policy 的触发条件
