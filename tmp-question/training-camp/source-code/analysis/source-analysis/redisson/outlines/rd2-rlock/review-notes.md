# RD-2 RLock+Watchdog — 深审 REVIEW 记录 (六层深审 + 07 五维度 + 二次 REVIEW)

## 二次 REVIEW (07 换维度: 反写/锚点/跨域/事实/补漏)

| 轮 | 维度 | 发现 | 修复 |
|:--:|:--|:--|:--|
| R1 | 反写覆盖 | 8 闭环机制全部落入 3 篇大纲, 无素材断层 ✅ | 通过 |
| R2 | **锚点密度 (第四次复发)** | 裸行号 10 处; file:line 01=6/02=10/03=5 (03 需≥8) | **全补 → 01=12/02=10/03=8** ✅ |
| R3 | 跨域引用 | r22-expire / s29-tx-chain 只在 header 声明正文零展开 (06 §2.5) | 两处补对照展开 |
| R4 | 事实核验 | Spin/Faster→BaseLock, Fair→RedissonLock, Red→MultiLock 继承关系 ✅; forceUnlock publish UNLOCK ✅ | 通过 |
| R5 | **覆盖缺口** | **三续期器只讲了 LockTask; ReadLockTask (读锁前缀) / FastMultilockTask (字段集) 缺失** — REDISSON-PLAN 明确写了三续期器 | 篇2 新增 "5. 三续期器" 节 (源码: ReadLockTask:33,66-73; FastMultilockTask:32,78) |

> ⚠️ 四连复发 (裸行号 RD-1/3/4/2; 对照零展开 RD-1/2): 说明"REVIEW 后修复"模式无法根治。**根本对策**: 写作时遵守两条硬规则 — ①行号出现即带文件 ②凡 header 声明对照/复用必须在正文出现同词 + 至少一句摘要。RD-5 起强制。

## 六层深审 (Pass 0-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **09 修正 (锁族)** | REDISSON-PLAN 说锁族 9 类 — 实测**根包 14 类** (含 ReadWriteLock 家族 ReadLock/WriteLock) | pass1 修正 14 + 篇3 全展开 |
| 2 | 机制实证 (harness) | 可重入计数/owner 校验/重入递减/forceUnlock/防重入心跳/并发互斥 → MiniRLock **15/15 PASS** | harness 对照 Lua 语义; 修复 harness 自身 2 缺陷 |
| 3 | Watchdog 完整闭环 | renewLock→add→tryRun/schedule→run→execute→schedule 续排→锁尽 stop | 篇2-S1 |
| 4 | 批量续期 Lua | 单脚本多键 hexists→pexpire + ContainsDecoder 摘除 | 篇2-S3 |
| 5 | LockPubSub 双消息 | UNLOCK(0)/READ(1) 语义 + latch.release 差异 | 篇1-S3 |
| 6 | completeness ❌ 回填 | Q31 心跳指标 / Q36 30s 理由 | 篇2 负面空间 |
| 7 | 通过项 | 全锚点行号 awk 验证 ✅ | 记录 |

## 07 五维度

### 维度1 功能正确性
- tryLock 时间会计实证; 可重入 hash 锁 harness 15/15; unlock vs forceUnlock 双轨

### 维度2 性能
- 批量续期 chunk=100 → 1 RTT/100 锁; 订阅唤醒零轮询; Cluster 按槽位分组

### 维度3 内存
- LockEntry 账本; 批量脚本 args 线性; latch 每等待者一个

### 维度4 一致性/并发
- Watchdog AtomicBoolean 防重入; time 会计; LockEntry ConcurrentHashMap.compute

### 维度5 边界/安全
- owner 校验防非法解锁; forceUnlock 逃生门; fencing token 防 GC 残留; tryLock 有界等待

## 完成状态

- [x] Pass 0-3: 8 闭环 + 3 篇 + 50 问 + harness 15/15 + 锁族 14 修正
- [x] **二次 REVIEW (R1-R5) 修复 4 项** (锚点/对照展开×2/三续期器缺口)
- [x] 铁律: 裸行号四连复发 → 写作期硬规则; RD-5 起强制