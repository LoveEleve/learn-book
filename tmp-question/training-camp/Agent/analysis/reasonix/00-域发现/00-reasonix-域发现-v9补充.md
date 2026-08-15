# Reasonix 域发现 v9 补充(第四轮深扫:执行正确性/并行/存储层)— 2026-08-14

> 承接:v8。v9 深扫 agent/ 内部执行细节(fleet/parallel_tasks/execute_batch/session_lease/recovery_gc)+ memory store_v2 + retrieval BM25。
> 结论:**再发现 3 个高价值域(mutationBarrier 写屏障/fleet 并行写预声明/session_lease 跨进程租约)+ 4 个中价值**。

---

## 一、v9 新增域

### 🔴 高价值新增(3 个 — 执行正确性)

| # | 域 | 文件 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 74 | **变异屏障 MutationBarrier** | internal/agent/execute_batch.go(552) | 552 | **工具批中第一个持久状态写失败/被阻 → 后续所有变异跳过,验证不执行**;mutationBarrierCause 不可变描述(分类已知/未知);"Fix or re-run the failed change first; verification was not executed" | ②执行正确性(失败后不继续写) |
| 75 | **并行任务 Fleet** | internal/agent/fleet.go(428)+ parallel_tasks.go(372) | 800 | **并行子代理**:写任务必须预声明非重叠 write_paths,**preflight 失败不启动任何任务**;并行只读任务各自 goroutine + 嵌套事件(前端独立卡片);fleetMin 2/max 64 | ②并行执行 |
| 76 | **会话路径租约 SessionLease** | internal/agent/session_lease.go(375) | 375 | **跨进程 session 路径租约**:WriterID/PID/Hostname/AcquiredAt;pending vs active 分离(防未获锁的 repair 当作所有权证据);CompareAndDelete 防旧代驱逐新代;ErrSessionLeaseHeld | ④多进程写 |

### 🟡 中价值新增(4 个)

| # | 域 | 文件 | 体量 | 设计要点 |
|---|----|------|:--:|---------|
| 77 | **回收分支 GC** | internal/agent/recovery_gc.go(860) | 860 | **恢复分支自动回收**:仅当原 session 已含 fork 全部内容才可回收;24h 宽限(新鲜 fork 属活跃冲突流);触发 = 保存冲突 |
| 78 | **记忆存储 v2(CAS)** | internal/memory/store_v2.go(612) | 612 | SaveOptions:ExpectedRevision/**RequireExpectedRevision(CAS)**/RequireCreate/ClearExpiry;文件 + sha256 |
| 79 | **BM25 检索实现** | internal/retrieval/bm25.go + v2.go | — | CJK 感知 token 化/BM25Score/KeepTopRelativeScore(相对分数截断)/多字节安全摘要 | 
| 80 | **协调器接口** | internal/agent/coordinator.go(789) | 789 | **Runner 接口统一单模型 Agent / 双模型 Coordinator**;PlannerPlanApprover 绑定原生审批 UI;DefaultPlannerPrompt(规划器只产简洁计划不执行) |

---

## 二、跨项目印证(执行正确性归并)

| 机制 | Pi | Hermes | Reasonix |
|------|----|--------|----------|
| 同文件写串行 | file-mutation-queue | file_state | **fleet 预声明 write_paths + mutationBarrier** |
| 跨进程租约 | writer-leases(fence) | compression_lock/scale_to_zero | **session_lease(PID/CompareAndDelete)** |
| 失败后停止 | — | execute 中断检查 | **mutationBarrier(第一个失败 → 后续跳过)** |
| 并行任务 | — | delegate_task(max_concurrent) | **fleet(2-64,preflight 全停)** |
| 记忆 CAS | — | — | **store_v2(RequireExpectedRevision)** |
| 检索 | — | FTS5 降级链 | **BM25(CJK/相对分数)** |

**新增通用模式**:
1. **"写任务必须预声明 write_paths"** — 并行写安全的先验约束(preflight 失败不启动)
2. **"第一个持久写失败 → 后续变异全跳"** — 失败后不继续写(mutationBarrier),验证不执行
3. **"pending vs active 分离"** — 租约/所有权状态的双态区分(防把未获锁当所有权证据)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v5 | 原始 | 41 | 41 |
| v6 | 体量排序复测 | +12 | 53 |
| v7 | 策略/契约/存储层 | +14 | 67 |
| v8 | 验收报告层 | +6 | 73 |
| v9 | 执行正确性/并行/存储层 | +7 | **80** |

> 剩余:boot/bot 细节、extension 内部、provider 适配细节、i18n/notify 等支撑——按产品价值持续衰减,可收敛或按需深挖。
