# 闭环笔记 RQ7:Checkpoint — 每轮快照 + 原子回滚

> 域:internal/checkpoint/(5459 行:checkpoint.go 1042 + transaction.go 1773 + barrier.go 153 + capture.go 209 + blob.go 184 + observer.go 287 + types.go 278)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

Checkpoint 是 Reasonix 的"每轮快照 + 原子回滚"系统:每轮记录所有触及文件的编辑前状态,回滚事务原子发布。这是产品③验收器"验收失败回滚"和"长跑恢复"的核心参考。

## 验证过程

### 1. Checkpoint 结构(checkpoint.go:65-84)— 每轮快照

```go
type Checkpoint struct {
  Turn       int       // 轮次
  Prompt     string    // 用户提示
  MsgIndex   int       // 会话回滚边界(len(Session.Messages) at turn's start)
  SessionID  string
  Files      []FileSnap  // 本轮到所有触及文件
  Coverage   Coverage    // 覆盖范围
  ActiveWriters []ActiveWriter
  LastMutationSeq int64
  SessionRevision int64
}
```

**关键设计**:
- **MsgsIndex = 会话回滚边界**:"persisted so a resumed session can rewind the conversation and fork, not just the code"
- **每轮快照所有触及文件的编辑前状态**(Content/SHA256/BlobRef)
- **文件 + 会话双回滚**:回滚代码也能回滚对话

### 2. FileSnap 编辑前状态(checkpoint.go:38-51)

```
Content(编辑前内容)/Encoding/Mode/SHA256/BlobRef
AfterSHA256/AfterExisted/AfterMode(编辑后指纹——Owned 所有权)
CaptureSource(捕获来源)
PayloadExpired(blob 被 GC 但元数据保留)
```

**文件三态**:编辑前(Content)/编辑后(After 指纹)/所有权(Owned)

### 3. 意图先持久化(transaction.go:695-708)— 发布前先写意图

> "Persist a conservative 'may have published' intent before the first filesystem rename. Recovery can safely compensate even if the crash happened just before publish."

```
每个文件发布前:先持久化 t.Published = true(意图)
→ 崩溃在 rename 前:恢复可安全补偿
→ 崩溃在 rename 后:恢复可继续
```

**"先写意图再动作"**——与 Pi 的事件溯源、2PC 的 PREPARE 同哲学。

### 4. 逐文件发布 + 每步持久化(transaction.go:678-743)— 每步可恢复

```
for 每个文件:
  1. 持久化"may have published"意图
  2. publishTarget(文件系统 rename)
  3. 持久化"published"
  4. 失败 → failTransaction(回滚已发布文件)
```

**每步状态持久化**——任意点崩溃,恢复能确定"哪步完成了"。

### 5. InjectFail 故障注入(transaction.go:685, 718)— 每个阶段可模拟崩溃

```
InjectFail{Phase, AfterFiles}:
  publish_file / delete_file / after_publish_before_progress / conversation
```

**故障注入测试**:每个事务阶段可注入崩溃模拟——**事务正确性被测试证明**(不是靠运气)。

### 6. 文件后对话(transaction.go:752+)

```
文件事务完成 → 再截断对话(ApplyConversationTruncate)
文件 + 对话 = 两阶段事务
对话失败 → restoreTransactionConversation(补偿)
```

### 7. Barrier 屏障(barrier.go:153)— 写入协调

```
MutationBarrier:并发写入协调
```

### 8. Undo/重做(transaction.go:242+)

```
UndoRewind:撤销回滚(事务 ID)
LastUndoTransactionID / InvalidateUndo
```

### 9. PrepareRewind 预检(transaction.go:45-144)— review 新增

**回滚可行性预检**:
```
活跃写入者 → 禁用(ConflictBusyWriter)
无会话边界 → 对话回滚不可用
无文件捕获 → 文件回滚不可用
blob 过期 → 禁用(ConflictExpired)
legacy 检查点 → 只能显式警告恢复(ConflictCoverageLegacy)
文件冲突 precheck → 冲突禁用
RewindBoth → 两侧都过才可
```

**关键设计**:
- **prepare 时完整预检**——"这个回滚能不能做"提前知道
- **WorkspaceToken = barrier 代**(:57):计划绑定写入屏障代
- **计划持久化 token**(:140-144):提交时验证新鲜度

### 10. CommitRewind 提交前重验证(transaction.go:182-239)— review 新增

```
1. 计划存在性(一次性消费,delete plans[planID])
2. 门条件重验证(CanFiles/CanConversation——prepare 和 commit 之间状态可能变)
3. 独占屏障(TryEnterExclusive——回滚时独占工作区)
4. 活跃写入者复查
5. WorkspaceToken 代验证:
   "workspace changed since preview"(防过期计划提交)
6. 文件冲突复查
7. prepareTransaction → commitTransaction
```

**关键设计**:
- **提交前重验证**:prepare 的门条件在 commit 时重新检查——**防 prepare 和 commit 之间状态变化**
- **WorkspaceToken 代验证**:与 Fencing Token 同哲学——**防过期计划提交**
- **独占屏障**:回滚时独占工作区,防并发写入

**产品映射**:产品"章节回滚"同样 prepare 预检 + commit 重验证——回滚前检查能否做,提交时验证没变。

### 11. MutationBarrier 读者-写者屏障(barrier.go:153 行)— review 第三轮新增

```
写者(EnterWrite/ExitWrite)与独占(EnterExclusive/ExitExclusive)互斥
Generation:每次写者退出/独占释放递增
  "Plans prepared before a completed writer can therefore never authorize
   a later commit without a fresh preview"——代号检测并发变更不靠墙钟
与业务锁分离:file I/O 永不在 App.mu/Controller 锁下运行
```

**关键设计**:
- **代号 = 并发变更检测**:不依赖墙钟(墙钟可回拨),每次写/独占释放递增
- **锁分离**:屏障独立于业务锁——文件 IO 不阻塞业务逻辑

**产品映射**:产品"章节发布屏障"同样代号检测 + 锁分离。

### 12. 原子状态写入 + 指纹捕获(capture.go + atomic_json.go)— review 第三轮新增

```
writeJSONAtomic:AtomicWriteFileStrict(状态文件原子发布)
CapturePath/FingerprintPath:编辑前指纹(路径/哈希/模式)
CompareIdentity:当前指纹 vs 编辑后指纹 → 冲突检测
NormalizeRelPath:相对路径归一化
```

**关键设计**:
- **状态文件原子写**:检查点元数据不会半写
- **指纹冲突检测**:编辑前指纹 vs 当前——**"文件被外部改了"检测**

**产品映射**:产品"章节状态"原子写 + 指纹冲突检测。

### 13. 事务状态机 + 双态目标(types.go:191-240)— review 第四轮新增

```
TransactionState:
  prepared → committing → committed / aborted / undone

TransactionTarget 双态:
  Restore(回滚目标=检查点前像:RestoreExisted/RestoreSHA/RestoreBlob)
  Forward(前进目标=prepare 时当前态:ForwardExisted/ForwardSHA)
  Staging 路径:事务唯一的兄弟路径(publish/补偿不跨文件系统)
```

**关键设计**:
- **Restore/Forward 分离**:回滚用 Restore(检查点前像),补偿/撤销用 Forward(prepare 时当前态)
- **Staging 同文件系统**:publish/补偿不跨文件系统(rename 原子性保障)

**产品映射**:产品"章节回滚"事务同样双态——回滚目标 + 前进目标分离。

### 14. BlobStore 内容寻址(blob.go:184 行)— review 第四轮新增

```
BlobStore:SHA-256 命名的内容寻址存储
  Put(data) → ref(SHA-256)
  Get(ref) / Has / Remove
  Prune(live refs):垃圾回收(检查点元数据保留,payload 可 GC)
  PayloadExpired:blob 被 GC 后元数据保留
```

**关键设计**:
- **大文件内容存 blob,检查点只存引用**——检查点文件小
- **引用计数 GC**:Prune 按活跃引用回收
- **PayloadExpired 标记**:blob 被 GC 但元数据保留——**引用完整性**

**产品映射**:产品"章节快照"大内容同样内容寻址——快照元数据小,内容 blob 引用 + GC。

## 代码类型

Implementation + 事务(原子提交)

## 跨域关联

- → 被消费:control(每轮 checkpoint + rewind 命令)
- ← 依赖:diff(变更检测)、fileenc(编码)

## 结论

核心可抄设计 14 个:
1. **每轮快照**(所有触及文件编辑前状态)→ 产品章节快照
2. **MsgIndex 会话回滚边界**(回滚对话不只代码)→ 产品分析回滚
3. **意图先持久化**(发布前写意图)→ 崩溃可补偿
4. **逐文件发布 + 每步持久化** → 每步可恢复
5. **InjectFail 故障注入**(每阶段模拟崩溃)→ 事务正确性被测试
6. **文件 + 对话两阶段事务** → 完整回滚
7. **Barrier 写入协调** → 并发安全
8. **Undo 撤销** → 回滚可撤销
9. **PrepareRewind 预检**(可行性提前知道)→ 回滚前检查
10. **CommitRewind 重验证**(门条件 + WorkspaceToken 代 + 独占屏障)→ 防过期提交
11. **MutationBarrier 代号 + 锁分离** → 并发变更检测不靠墙钟
12. **原子状态写入 + 指纹冲突检测** → 状态文件半写防护
13. **事务状态机 + 双态目标**(Restore/Forward 分离)→ 回滚/补偿分离
14. **BlobStore 内容寻址**(SHA-256 + 引用 GC)→ 快照内容小 + 可回收

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| 每轮快照 | ✅ 抄 | 产品每章快照(分析前状态) |
| MsgIndex 回滚边界 | ✅ 抄 | 分析回滚(对话+知识库状态) |
| 意图先持久化 | ✅ 抄 | 章节发布前写意图 |
| 逐文件发布 | ✅ 抄 | 每步可恢复 |
| InjectFail | ✅ 抄 | 回滚事务故障注入测试 |
| 两阶段事务 | ✅ 抄 | 文件+对话完整回滚 |
| Barrier | ✅ 抄 | 多写者协调 |
| Undo | ✅ 抄 | 回滚可撤销 |
| 预检 | ✅ 抄 | 回滚前检查可行性 |
| 重验证 | ✅ 抄 | 提交时验证没变(代验证防过期) |
| 代号检测 | ✅ 抄 | 并发变更检测不靠墙钟 |
| 原子写 + 指纹 | ✅ 抄 | 状态文件安全 + 外部修改检测 |
| 双态目标 | ✅ 抄 | 回滚/补偿分离 |
| 内容寻址 | ✅ 抄 | 快照元数据小 + GC |

## 面试问答弹药

- **Q**:检查点怎么设计?→ A:每轮快照所有触及文件的编辑前状态 + MsgIndex 会话回滚边界——回滚代码也能回滚对话
- **Q**:回滚事务怎么保证原子?→ A:意图先持久化 + 逐文件发布 + 每步持久化——任意点崩溃可确定"哪步完成了"
- **Q**:怎么证明事务正确?→ A:InjectFail 故障注入——每个阶段可模拟崩溃,事务正确性被测试证明
- **Q**:回滚范围?→ A:文件 + 对话两阶段——先文件后对话,对话失败可补偿
- **Q**:并发写怎么协调?→ A:MutationBarrier——写入屏障 + 回滚时独占,代号检测并发变更
- **Q**:回滚能撤销吗?→ A:能——UndoRewind,事务 ID 定位
- **Q**:回滚前检查什么?→ A:PrepareRewind 预检——活跃写入者/会话边界/文件捕获/blob 过期/legacy/文件冲突
- **Q**:提交时怎么防过期?→ A:CommitRewind 重验证——门条件重查 + WorkspaceToken 代验证("workspace changed since preview")+ 独占屏障
- **Q**:代号怎么检测并发变更?→ A:Generation 每次写者退出/独占释放递增,不靠墙钟——准备令牌绑定代号,过期提交拒绝
- **Q**:事务状态怎么管理?→ A:TransactionState 状态机——prepared→committing→committed/aborted/undone;Restore(回滚)与 Forward(补偿)双态分离
- **Q**:大文件快照怎么存?→ A:BlobStore 内容寻址——SHA-256 命名 + 引用计数 GC,检查点只存引用
