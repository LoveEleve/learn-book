# Reasonix 域发现 v14 补充(第九轮深扫:workspacelease/secrets 支撑层)— 2026-08-14

> 承接:v13。v14 深扫 workspacelease(核心域实现细节)+ secrets/notify。
> 结论:workspacelease 是"工作区写租约"完整实现(本地锁 + 写时获取 + 保留到完成);secrets 是"子进程 env 过滤 + 脱敏"完整实现。新增 3 个中价值域,本轮后支撑层基本扫完。

---

## 一、v14 新增域

| # | 域 | 文件 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 99 | **工作区租约实现** | internal/workspacelease/lease.go | ~300 | **核心域实现细节**:Owner.BeginRun/EndRun/**AcquireWrite(写时获取)**/RetainUntil(保留到完成);canonical workspace(符号链接解析/git worktree 根折叠/**linked worktree 保持独立**);重试间隔 75ms;本地锁(localLock);测试含 helper 进程 | ④写租约(与 Pi writer-leases 对比) |
| 100 | **密钥环境过滤** | internal/secrets/redact.go | ~230 | **子进程 env 过滤**(FilterSubprocessEnv/FilterEnv/ProcessEnv)+ 敏感文件保护(ProtectSensitiveFiles)+ **凭证脱敏**(Redact/RedactCredentials/RedactError,key-value 模式);注册凭证 env 键 | ④安全 |
| 101 | **平台通知** | internal/notify/(sender_*/sink.go) | ~150 | 平台通知(AppleScript/dunst 等)+ 异步 sink | — |

---

## 二、跨项目印证(租约归并 — 最终版)

| 维度 | Pi | Hermes | Reasonix |
|------|----|--------|----------|
| 写租约 | writer-leases(fence 单调) | compression_lock(租约) | **workspacelease(写时获取 + RetainUntil)** |
| 密钥过滤 | — | secret_scope/profile 隔离 | **FilterSubprocessEnv + 脱敏** |
| 沙箱 | restore-sandbox-env | environments 7 后端 | OS jail(Seatbelt/bwrap) |
| 中断恢复 | findOpenOperations | interrupted 跳过 | InterruptedTurnRecovery(LocalOnly) |

**租约模式三变体**:
1. Pi:数据级 fence(单调递增防 stale)
2. Hermes:会话级租约(压缩锁 TTL + pid 死检)
3. Reasonix:工作区级(写时获取 + 保留到完成 + canonical 规范化)

**产品④知识库写租约 = 三变体组合**:数据级 fence(防 stale 写)+ 工作区级(写时获取)+ TTL/pid 死检。

---

## 三、累计覆盖对账(收官)

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v5 | 原始 | 41 | 41 |
| v6 | 体量排序复测 | +12 | 53 |
| v7 | 策略/契约/存储层 | +14 | 67 |
| v8 | 验收报告层 | +6 | 73 |
| v9 | 执行正确性/并行层 | +7 | 80 |
| v10 | boot 运行时组装层 | +5 | 85 |
| v11 | bot 消息网关层 | +5 | 90 |
| v12 | extension 插件运行时层 | +4 | 94 |
| v13 | sandbox/remote/provider 恢复层 | +4 | 98 |
| v14 | workspacelease/secrets 支撑层 | +3 | **101** |

> **收敛判定(v14)**:9 轮深扫 41 → 101 域(+60)。internal/ 全部子目录已覆盖(96 个包逐一核对),支撑层(i18n/notify/secrets)已扫。剩余:provider 各适配器(同构)、desktop 前端(排除)、i18n 消息(数据)。
> **D18 完整实证**:Pi(59→88)、Reasonix(41→101)、Hermes(27→80)——三个项目都靠"体量排序 + 逐目录核对"才达到真收敛。
