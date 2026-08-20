# Z-8 Recipes — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | 执行计划 "LeaderLatch/InterProcessMutex" 是 Curator 类 (09 审计已修正); 本域深审再补: **三 recipes = 同一算法三语义** (锁=选举 Javadoc L36-37) — 非三个独立模式 | 大纲 §1 共同模式 |
| 2 | **表述精确化** | "前驱 watch" 需精确: 选举/锁 = **exists 单节点 watch**; 队列 = **getChildren 整目录变更 watch** — 两种触发面不同 (DELETE vs CHILDREN_CHANGED) | 大纲 §1/§4 |
| 3 | **认知修正 (悬挂竞态)** | **WriteLock 前驱消失竞态**: getChildren 与 exists 之间前驱被删 → stat==null 只 log.warn (L243) → 但 exists 已注册 watch (NONODE → 创建 watch, ZooKeeper:322) → 顺序节点名唯一**永不再创建** → watch 永不触发 → **lock() 返回 FALSE 后永久悬挂**; 而 LES 同类竞态 (becomeReady stat==null L248-257) 走 **determineElectionStatus() 递归重读** — 同一模式两种处理 | 大纲 §2/§3 对照标注 |
| 4 | **语义标注** | **exists watch 双语义**: OK → 删除 watch; NONODE → 创建 watch (ZooKeeper:316-322) — recipes 依赖此语义但没意识到 NONODE 分支是死 watch | 大纲 §2 注 |
| 5 | **补充锚点** | **父目录自举两种路**: WriteLock ensurePathExists 吞异常 (ProtocolSupport:173-175) vs DQueue offer/take 直接 create(dir) — NodeExistsException 竞态未捕获直抛 (DQueue:236,268) — 处理不一致 | 大纲 §4 注 ⚠ |
| 6 | **认知修正 (会话失效)** | **LES 会话失效静默**: process() 只处理 NodeDeleted (L328) + 排除自身路径 (L329) → 会话过期自身 ephemeral 被删 → **无任何事件** → 停留在 ELECTED/READY; Javadoc "best effort" (L66-87) 承认 — **双主窗口风险** | 大纲负面空间 |
| 7 | **补充锚点** | **stop() Javadoc 与实现不符**: Javadoc "disconnects from ZooKeeper" (L145) vs 实现只 delete offer 不 close 客户端 (L153-160) | 大纲负面空间注 |
| 8 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (LES 119-341 / WriteLock 119-291 / ProtocolSupport 41-200 / DQueue 65-300 / ZooKeeper 316-322) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 最小序号判定三实现
- 前驱 watch 触发链
- 队列 FIFO + NoNode 重试

### 维度2 性能
- 事件驱动替代轮询 (O(1) vs O(N))
- getLeaderHostName O(N) 无缓存
- remove 快照重读无 sleep — 高竞争下 busy loop

### 维度3 内存
- LeaderOffer/TreeSet/TreeMap 快照
- 快照逐节点重读

### 维度4 一致性
- 序号全序 → 互斥保证
- ephemeral 会话绑定
- 前驱消失竞态两种处理不一致

### 维度5 负面空间 (已写入大纲 6 条)
- 不服务端原语/不会话自愈/不锁超时/不 ack/不重试抽象/不续约

## 结论
Z-8 锚点 ~30 处验证, 8 闭环完成, **认知修正 3 + 表述精确化 1 + 语义标注 1 + 补充锚点 2**。🟡 B 无 harness。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置 Z-7/Z-6/Z-5/Z-3 ✅; 引出 Curator ✅; 对照 Redisson/Curator ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (竞态/会话/并发消费) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **反写测试发现** | **WriteLock 无 watch 悬挂路径**: lessThanMe 空且 !isOwner() → **不注册任何 watch 直接 return FALSE** (L245-253) — 场景: 自身 id 陈旧 (会话过期后节点已删, 序号仍小于全部现存节点) → headSet 空 → isOwner false → 无唤醒源悬挂; 且 do-while 只在 **id==null 或子节点空** 时重建 (L214,223-227) — **会话过期后永不重建** | 大纲 §3 注 + 负面空间 |
| 11 | 通过项 | 其余 ~24 句机制描述逐句对源码一致 ✅ (状态机/幂等创建/重试面/unlock/FIFO/take 阻塞/peek-poll) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (无 watch 悬挂路径 #10), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 最小者胜唯一性 | 序号全序 → 最小元素唯一 (TreeSet/TreeMap/排序后下标 0) — 无二主 ✅ | 通过 |
| V2 | 前驱 watch 闭环 | 前驱删除 → watch 触发 → 重跑判定 — 事件驱动闭环 ✅ (常规路径) | 通过 |
| V3 | 幂等恢复 | create 响应丢失 → 前缀扫描复用旧节点 — 不重复创建 ✅ | 通过 |
| V4 | 会话过期传递 | 自身 ephemeral 删 → 他人 NodeDeleted → 重选; 自身 SessionExpired 直抛 (WriteLock) ✅ | 通过 |
| V5 | 队列 FIFO | 全局序号排序 + 最小者先消费 — FIFO ✅ | 通过 |
| V6 | take 不丢事件 | **注册即读**: getChildren(带 watcher) → 空 → await — 变更必触发 latch (读后变必见) ✅ | 通过 |
| V7 | 并发消费安全 | getData+delete NoNode → 试下一个 — 无双删/双取 ✅ | 通过 |
| V8 | unlock 收尾 | delete + lockReleased finally — 异常也通知 ✅ | 通过 |
| V9 | 重试语义 | SessionExpired 直抛 (会话已死不重试) / ConnectionLoss 退避 10 次 ✅ | 通过 |
| V10 | 共享 ZooKeeper 安全 | LES 显式传 watcher (this) — 不污染默认 watcher ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **覆盖缺口** | **Integer 序号溢出**: LES id=Integer (L197) + ZNodeName seq=Integer (L43) vs DQueue Long (L79) — ZK 序列 %010d **10 位** (Z-3 交叉: 上限 9999999999) > Integer.MAX (2147483647) → **2.1B 次创建后 NumberFormatException → becomeFailed/parse 失败** — 位宽不一致 | 大纲 §1 注 |

## 三次 REVIEW 汇总
推理验证 10 项全过 (V1-V10); 新发现 **1 处** (Integer 溢出 #12), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 竞态穷举)

> 动机: 对全部锚点重新 grep, 穷举六个竞态面 (LockWatcher 事件类型/unlock 与 watch 并发/remove busy loop/LES 递归深度/DQueue 自举竞态/expiry 后重建)。

## 追查过程 (六个竞态面全部实证)

| # | 竞态面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | LockWatcher 事件类型? | **不检查 event type** (WriteLock:158-166) — 会话事件 (type=None, 断连/过期) 也触发 lock() 重试 — 陈旧 id 空转面 | 发现 13 (补锚) |
| T2 | unlock 与 watch 并发? | unlock 置 id=null 前已注册的 LockWatcher 事件仍会触发 → 新 lock() 重建新节点 (id==null) — 重建语义 (unlock 后重新入队) — 与 "移除请求队列" 的 Javadoc 目标 (L110-118) 有竞态窗口 | 通过 (验证+注释) |
| T3 | remove busy loop? | while(true) 无 sleep — 高竞争下快照反复重读 — **无界重读** (对比 take 有 watch 阻塞) | 发现 14 (补锚) |
| T4 | LES 递归深度? | becomeReady stat==null → determineElectionStatus 递归 — 深度 = 连续消失的前驱数 (正常情况 1, 风暴下可深) — 无深度守卫 | 通过 (验证) |
| T5 | DQueue 自举竞态? | 双消费者同时 take 空目录 → 双 create(dir) → 一方 NodeExistsException **未捕获直抛** (L236) — offer 同理 (L268) | 发现 15 (补锚) |
| T6 | expiry 后重建? | WriteLock SessionExpired 直抛后 **id 不清理** → 新会话同实例复用 → 陈旧 id (见 #10) — 无会话重建语义 | 通过 (验证 #10 成立) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | watch 触发闭环 | 删除事件 → 重判 → 前驱链推进 ✅ | 通过 |
| V2 | 序号位宽一致 | 锁/选举 Integer vs 队列 Long — 不一致 (见 #12) ✅ | 通过 |
| V3 | 快照重读终止 | 元素有界 → 每轮要么消费要么被抢 → 最终空/成功 ✅ (忙等面见 #14) | 通过 |
| V4 | latch 无死锁 | 注册即读 + 变更必触发 — 空队列不永久阻塞 ✅ | 通过 |
| V5 | 状态机无非法迁移 | 7 State 枚举 + 12 事件线性推进 — 单线程 synchronized 无并发迁移 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **补充锚点** | **LockWatcher 不筛事件类型** (WriteLock:158-166): 会话事件也触发 lock() — 陈旧 id 空转面 | 大纲 §3 注 |
| 14 | **补充锚点** | **element/remove 无界重读**: while(true) 无 sleep — 高竞争 busy loop (take 有 watch 阻塞, remove 没有) | 大纲 §4 注 |
| 15 | **语义标注** | **DQueue 自举竞态**: 并发 create(dir) → NodeExistsException 未捕获 (DQueue:236,268) — 与 WriteLock ensurePathExists 吞异常不一致 (ProtocolSupport:173-175) | 大纲 §4 ⚠ 强化 |

## 反写测试 (只读大纲能否写文章)

- §1 共同模式 (顺序节点/最小序号三实现/前驱 watch 双触发面) — 可写 ✅
- §2 选举 (状态机 7/12/快照判定/竞态处理) — 可写 ✅
- §3 锁 (幂等/监听/重试面/悬挂路径) — 可写 ✅
- §4 队列 (FIFO/阻塞 take/并发消费/自举竞态) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

六竞态面全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (LockWatcher 事件类型/remove 无界重读/DQueue 自举竞态)。核心认知: **同一模式三语义** (选举=锁=序号最小者) + **前驱消失竞态两实现不一致** (LES 递归重读 vs WriteLock 悬挂) + **会话失效三 recipe 三态度** (LES 静默/WriteLock 直抛/队列无感)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + watch 语义三重实证 + 并发/断连/幂等穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查八个存疑面 (exists watch 服务端注册/会话事件送达/断连事件丢失/toLeaderOffers 竞态/前缀校验/process 并发/unlock 残留 watch/offer 幂等)。

## 追查过程 (八个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | exists watch 服务端注册? | PrepRequestProcessor:889 (case OpCode.exists — 仅查 session) + FinalRequestProcessor:361 (statNode(path, watch?cnxn:null)) — **注册面在 DataTree.statNode, 节点不存在也注册** | 通过 (验证) |
| T2 | exists watch 客户端注册面? | ExistsWatchRegistration.shouldAddWatch OK‖NONODE (ZooKeeper:321-323) + getWatches: NONODE → **getExistWatches** (L316-317); ZKWatchManager.materialize: NodeCreated/NodeDeleted 均触 existWatches (L347-399) — 死 watch 三重实证 (注册+触发双面) | 通过 (验证) |
| T3 | 会话事件送达哪些 watcher? | ZKWatchManager.materialize case None: **defaultWatcher + 全部 data/exist/childWatches** (L345-370) — **显式 watcher 也收会话事件** → LockWatcher 必然收到 None 事件空转触发 lock() (部分自愈面) | 发现 16 (补锚) |
| T4 | 断连期间前驱死亡? | 事件在断连期丢失 (无队列) → setWatches 重注册仅恢复**仍存在节点**的 watch (Z-7 交叉) → 前驱已删 → 注册创建 watch → **永不触发 → 悬挂**; LES process 不处理 None → **无 SyncConnected 重判钩子** | 发现 17 (补锚) |
| T5 | toLeaderOffers 竞态? | L310 getData 逐节点读 — **前驱在快照后删除 → NoNodeException 直抛 → becomeFailed** — 同一"前驱消失"三处三种处理 (becomeReady 递归重读 / toLeaderOffers 直接 FAILED / WriteLock 悬挂) | 发现 18 (补锚) |
| T6 | 前缀校验? | LES toLeaderOffers **无前缀检查** — 共享 root 下他人节点 (非 n_) → substring NumberFormatException → FAILED (L197,313); 对照 DQueue regionMatches warn+skip (L74-83) | 发现 19 (补锚) |
| T7 | process 并发? | **process() 非 synchronized** — 事件线程 determineElectionStatus 与用户线程 start()/becomeReady 并发 → 双事件交错; state 字段非 volatile (L95) | 发现 20 (补锚) |
| T8 | unlock 残留 watch? | unlock (L119-149) 不撤销前驱路径上的 LockWatcher — 前驱随后死亡 → 事件 → lock() → **id==null 重建节点重新竞争** — 违反 Javadoc "removes your request in the queue" (L110-118) | 发现 21 (补锚) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 死 watch 闭环 | NONODE → existWatches (客户端) + statNode 注册 (服务端) + 唯一名永不创建 → 永不触发 — 三重实证闭环 ✅ | 通过 |
| V2 | None 事件空转 | LockWatcher 收 None → lock() → 陈旧 id 重判 → 悬挂路径不变 — 空转面成立 ✅ | 通过 |
| V3 | 断连事件丢失 | 无事件队列 + setWatches 不补已删节点 → 前驱死亡静默 — 悬挂面成立 ✅ | 通过 |
| V4 | 三处三处理 | becomeReady 重读 / toLeaderOffers FAILED / WriteLock 悬挂 — 同一竞态三结果 ✅ | 通过 |
| V5 | unlock 残留 | 前驱 watch 存活 → 事件 → 重建 — Javadoc 违约窗口成立 ✅ | 通过 |
| V6 | 幂等对照 | WriteLock sessionId 前缀幂等 vs DQueue create 无幂等 — 重复入队面 ✅ | 通过 |
| V7 | 共享客户端冲突 | 同 sessionId 同前缀, 扫描-创建非原子 → NodeExists (retryOperation 只重试 ConnectionLoss) ✅ | 通过 |

## 新发现问题 (6 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 16 | **语义标注** | **会话事件送达全部 watcher** (T3, ZKWatchManager:345-370 case None): LockWatcher 收 None 事件 → lock() 空转 (陈旧 id 下是空转面; 有效 id 下是部分自愈) — 佐证 #13 | 大纲 §3 注 |
| 17 | **语义标注** | **断连期事件丢失面** (T4): 断连期间前驱死亡 → 事件无队列丢失 → setWatches 重注册仅恢复仍存在节点的 watch (Z-7 交叉) → 创建 watch 永不触发 → 悬挂; LES process 不处理 None → **无 SyncConnected 重判钩子** | 大纲 §2/§3 注 |
| 18 | **认知修正** | **toLeaderOffers NoNode → FAILED** (T5, L310): 快照后前驱删除 → getData NoNode 直抛 → becomeFailed — **前驱消失三处三种处理** (becomeReady 递归重读 / toLeaderOffers 直接 FAILED / WriteLock 悬挂) | 大纲 §2 注 |
| 19 | **补充锚点** | **LES 无前缀校验** (T6): 共享 root 他人节点 (非 n_) → substring NumberFormatException → FAILED (L197,313); 对照 DQueue regionMatches warn+skip (L74-83) — 两库防御不一致 | 大纲 §2 注 |
| 20 | **补充锚点** | **process 并发无锁** (T7): 事件线程 vs 用户线程 determineElectionStatus 并发 → 双事件交错; state 非 volatile (L95) | 大纲 §2 注 |
| 21 | **语义标注** | **unlock 残留 watch 重新加锁** (T8): unlock 不撤销前驱路径 LockWatcher (L119-149) → 前驱后死 → lock() 重建节点 — 违反 Javadoc "removes your request" (L110-118) | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 共同模式 (顺序节点/最小序号三实现/前驱 watch 双触发面/位宽) — 可写 ✅
- §2 选举 (状态机/快照判定/竞态三处理/前缀校验/并发面) — 可写 ✅
- §3 锁 (幂等/监听/重试/悬挂路径/unlock 残留) — 可写 ✅
- §4 队列 (FIFO/阻塞/并发消费/自举竞态/无幂等) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

八存疑面全实证 (T1-T8); 推理验证 7 项全过 (V1-V7); **新发现 5 处全部修复** (会话事件送达+断连丢失/三处三处理/无前缀校验/process 并发/unlock 残留 watch)。核心认知: **死 watch 三重实证闭环** (NONODE→existWatches→唯一名) + **断连期事件丢失面** (无 SyncConnected 重判钩子) + **同一竞态三结果** (重读/FAILED/悬挂)。大纲经修复后反写测试全过。
