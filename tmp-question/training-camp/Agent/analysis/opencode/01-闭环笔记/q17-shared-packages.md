# q17 — 共享包(深度版:Flock 文件锁 + KeyedMutex + Sync Fence)

> 域:并发基础设施(D15 理论层) | 文件:core/src/util/flock.ts(358)+ effect-flock.ts(284)+ effect/keyed-mutex.ts(45)+ opencode/src/server/shared/fence.ts(60)+ util/token.ts + effect/instance-state.ts
> review 轮次:2 轮(源码全文)

---

## 假设

OpenCode 的并发正确性靠三层:进程间文件锁(Flock,Lease 语义带心跳/崩溃恢复)、进程内按 key 互斥(KeyedMutex)、控制面同步栅栏(Sync Fence)。这是"多进程写共享资源"的理论层。

## 验证

### 1. Flock(设计 1:目录锁 + Lease 语义)

```ts
// flock.ts:148-204 tryAcquireLockDir
1. mkdir(lockDir, 0700) 原子创建 = 获取(EEXIST = 已被持)
2. EEXIST → stale() 检测:heartbeat/meta/dir 任一 mtime 超 staleMs → 过期
3. 过期 → mkdir(lockDir + ".breaker") 抢 breaker(唯一清理权)
   —— 防止多个进程同时清理锁目录
4. breaker 所有者在 stale 复查后 rm(lockDir) 重建
5. 写 heartbeatPath("wx")+ meta.json("wx")——"wx" 失败 = 可能被篡改 → 拒绝获取
   ("Lock acquired but heartbeat already existed (possible compromise)")
```

**心跳与释放**(flock.ts:223-259+):
- startHeartbeat:interval = staleMs/3(防长临界区被误判过期)
- release:读 meta.json 校验 **token** 匹配才删除——防删掉别人重新获取的锁
  ("Refusing to release: lock is compromised (metadata missing/invalid)")
- Options:staleMs/timeoutMs/baseDelayMs/maxDelayMs/onWait(重试 + 抖动 30%)

**使用者**:global/models-dev/npm/repository-cache/config/mcp-auth/plugin-install/plugin-meta(多进程写共享资源)。

**Effect 封装**:effect-flock.ts(284 行)提供 Effect 化接口。

### 2. KeyedMutex(设计 2:同 key 排队,不同 key 并行)

```ts
// keyed-mutex.ts(45 行)
Semaphore per key;withLock(key)(effect):同 key → 排队;不同 key → 独立
// 无持有者/等待者时条目移除(内存清理)
// 使用者:file-mutation(文件写)/git(仓库操作)/plugin
```

### 3. Sync Fence(设计 3:控制面同步栅栏)

```ts
// fence.ts(60 行)
HEADER = "x-opencode-sync"
load(db, ids?):event_sequence 表 → Record<aggregateID, seq>
diff(prev, next):变化的聚合
// 使用者:workspace-routing middleware——同步请求携带 x-opencode-sync 头(聚合 seq 快照),
// 服务端比对差异决定哪些事件需要同步
// 与 EventV2 owner 栅栏的关系:CONTEXT.md "Event replay owner claims are separate from clustered Session execution ownership"
```

### 4. Token 估算(设计 4:compaction 预算)

```ts
// util/token.ts:Token.estimate(value) = JSON.stringify 估算(非 provider tokenizer)
// 使用者:compaction 预算估计(紧凑)
```

### 5. InstanceState(设计 5:按目录隔离实例状态)

```ts
// opencode/src/effect/instance-state.ts
ScopedCache keyed by directory:每打开项目一个实例,自动清理
// 使用者:permission pending 状态/session 状态/MCP 客户端等
// AGENTS.md:"If two open directories should not share one copy of the service, it needs InstanceState"
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Flock 目录锁(breaker 竞态 + 心跳 + token 校验释放) | flock.ts:148-259 | ②多进程写入安全 |
| 2 | KeyedMutex(同 key 排队/不同 key 并行) | keyed-mutex.ts | ②进程内互斥 |
| 3 | Sync Fence(x-opencode-sync + event seq 快照) | fence.ts | ④多节点同步 |
| 4 | Token 估算(JSON.stringify 近似) | util/token.ts | ②压缩预算 |
| 5 | InstanceState(ScopedCache 按目录) | effect/instance-state.ts | ②实例隔离 |

## 面试弹药

- "Flock = 文件 Lease":目录原子创建获取 + breaker 抢清理权 + 心跳防误判 + token 校验释放——崩溃恢复安全的进程间锁
- "妥协检测":heartbeat 已存在(flag wx 失败)→ 拒绝获取,不静默接管——防锁被篡改
- "breaker 单一清理者":stale 清理只能一个进程做,防多个进程同时 rm 重建
- "KeyedMutex 内存自动清理":无持有者即移除条目——不泄漏
- "Sync Fence 与 owner 栅栏分离":同步头是"读侧差异检测",写侧栅栏在 EventV2 owner

## 待深挖

- [ ] effect-flock.ts 的 Effect 封装细节(重试调度)
- [ ] workspace-routing 的 fence 使用场景
