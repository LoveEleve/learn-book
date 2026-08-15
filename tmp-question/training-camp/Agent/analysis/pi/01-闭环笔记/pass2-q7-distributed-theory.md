# 闭环笔记 Q7:Pi 的分布式协调理论(隐藏的理论层)

> 域:session-backends/sqlite-node/src/sqlite/(writer-leases + session-sequences + repo)
> 日期:2026-08-14
> 触发:review 中发现"内部协调机制"——用户追问是否引入数学/分布式理论,验证后确认存在
> 结论:**Pi 的持久层内置了分布式系统的经典理论模式**

---

## 一、为什么需要:多写入者问题

AGENTS.md:53 明确声明:
> "Multiple pi sessions may be running in this cwd at the same time, each modifying different files."

**多个 pi 进程可能同时写同一个 session 文件** → 这是经典的**多写入者协调问题**,于是:

## 二、发现 1:Writer Lease = Fencing Token 模式(教科书级实现)

`writer-leases.ts` 完整实现分布式系统的 **Fencing Token**(防脑裂):

```sql
-- 获取租约:过期才可抢占,抢占时 fence+1
INSERT INTO writer_leases (session_id, owner_id, fence, expires_at_ms)
VALUES (?, ?, 1, ?)
ON CONFLICT(session_id) DO UPDATE SET
  owner_id = excluded.owner_id,
  fence = writer_leases.fence + 1,          -- ★ 栅栏令牌递增
  expires_at_ms = excluded.expires_at_ms
WHERE writer_leases.expires_at_ms <= ?       -- 只有过期才能抢

-- 续约:必须 owner_id + fence 都匹配,且未过期
UPDATE writer_leases SET expires_at_ms = ?
WHERE session_id = ? AND owner_id = ? AND fence = ? AND expires_at_ms > ?

-- 释放:必须 owner_id + fence 都匹配
DELETE FROM writer_leases WHERE session_id = ? AND owner_id = ? AND fence = ?
```

**理论对应**(Martin Kleppmann《数据密集型应用系统设计》第 8 章):
- **fence 单调递增** → 防止"旧写入者复活后覆盖新写入者"——旧持有者持有旧 fence,写入被拒
- **过期抢占** → 租约超时后新写入者 fence+1 接管
- **条件续约/释放** → 只有"当前 fence 的持有者"才能续/放——**旧租约无法复活**

**为什么用 SQLite 做租约存储**:单机多进程共享文件 → SQLite 的 `ON CONFLICT DO UPDATE WHERE` 提供**原子比较-交换(CAS)**,这是租约获取的原子性保障。

## 三、发现 2:Sequence = 全序保证(事件溯源的序列号)

`session-sequences.ts`:每 session 一个**单调递增计数器**:
```
getNextSequence / advanceSequence / setNextSequence
```

结合 facts/records 表的 `(session_id, seq, ...)` 结构:
- **每条记录一个全局单调 seq** → 提供**全序(total order)**
- 这是**事件溯源(Event Sourcing)**的序列号机制:追加写 + 单调序号 = 可重放、可增量读取(getLog(afterSeq))

## 四、发现 3:恢复语义 = 2PC 的影子

Q2 已发现 `findOpenOperations`(session/types.ts):
```
limit: 2 的恢复判定:
  0 条 = 空闲
  1 条 = 挂起(suspended)
  2 条 = 损坏(corruption)
```

**理论对应**:这是**两阶段提交(2PC)的 in-doubt 状态恢复**——
- operation_started 记录 = 2PC 的 PREPARE 阶段(已声明开始,未确认完成)
- 崩溃恢复时查 open operations = 2PC 的恢复协议(问协调者"这个事务到底提交了没")
- 2 条 open = 损坏(不可能出现两个未完成的操作)

## 五、发现 4:事务内验证所有权(组合模式)

repo.ts:409 注释:
> "A transient heartbeat failure is retried. **Every write still verifies ownership transactionally.**"

**租约验证 + 数据写入在同一个 SQLite 事务里**——这是**租约 + 事务的组合**:写入不是"先查租约再写"(有 TOCTOU 窗口),而是"事务内验证 + 写入"(原子)。

## 六、理论全景

| 理论 | Pi 的应用 | 位置 |
|------|----------|------|
| **Fencing Token(防脑裂)** | writer_leases 表 + fence 递增 | writer-leases.ts |
| **Lease(租约)** | 过期抢占 + 续约 | writer-leases.ts |
| **Event Sourcing(事件溯源)** | JSONL 追加 + seq 全序 + 可重放 | session-sequences + storage |
| **2PC 恢复(两阶段提交)** | operation_started + findOpenOperations 三态判定 | records + session/types |
| **原子 CAS(比较交换)** | SQLite ON CONFLICT WHERE 原子抢占 | writer-leases.ts |
| **事务+租约组合** | 写入事务内验证所有权 | repo.ts:409 |

## 七、设计洞察

1. **Pi 把"分布式协调"降维到单机 SQLite**:多进程场景 → 用 SQLite 的原子性做协调,不需要分布式共识(Paxos/Raft)——**单机够用时不用分布式协议**
2. **理论不是论文装饰,是踩坑后的必需品**:AGENTS.md 声明多会话并发 → 必须解决写冲突 → Fencing Token 是最小正确解
3. **失败模式的严谨性**:2PC 恢复三态判定(0/1/2 条)是"崩溃恢复"的精确状态机——**不是"恢复时猜",是"恢复时有定理"**

## 八、产品映射

| 设计 | 产品怎么用 |
|------|-----------|
| Fencing Token + Lease | 产品多进程/多 session 写同一知识库时防冲突 |
| 事件溯源 + seq | 书级知识库的可重放/增量读取 |
| 2PC 恢复三态 | 分析中断后的恢复判定(0 空闲/1 挂起/2 损坏) |
| 事务内验证所有权 | 知识库写入的原子性 |

## 面试问答弹药

- **Q**:多 agent 会话写同一文件怎么防冲突?→ A:Fencing Token + Lease——fence 递增防旧写入者复活,过期抢占,事务内验证所有权
- **Q**:为什么用 SQLite 做协调?→ A:单机多进程用 SQLite 原子 CAS 就够,不需要分布式共识协议
- **Q**:崩溃恢复怎么判定状态?→ A:findOpenOperations 三态——0 空闲/1 挂起/2 损坏(2PC in-doubt 恢复)
- **Q**:事件溯源为什么重要?→ A:追加写 + seq 全序 → 可重放、增量读取、审计
