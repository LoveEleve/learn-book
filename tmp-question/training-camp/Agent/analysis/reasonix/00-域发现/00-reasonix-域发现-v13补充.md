# Reasonix 域发现 v13 补充(第八轮深扫:sandbox/remote 执行环境 + provider 恢复层)— 2026-08-14

> 承接:v12。v13 深扫 internal/sandbox(OS 级 jail)+ internal/remote(SSH 全套)+ provider 的 InterruptedTurnRecovery。
> 结论:**sandbox 是"权限策略之下的强制层"**(Seatbelt/bubblewrap,无后端 fail closed);**InterruptedTurnRecovery 是中断恢复的最细粒度实现**。新增 4 个中高价值域。

---

## 一、v13 新增域

### 🔴 高价值新增(2 个)

| # | 域 | 文件 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 95 | **OS 级沙箱 Sandbox** | internal/sandbox/(sandbox.go/shell.go/seatbelt_*/prepare_*) | ~800 | **权限策略之下的强制层**:macOS Seatbelt + Linux bubblewrap;**无可用 OS 沙箱后端 → fail closed(不裸跑命令)**;WriteRoots 白名单(workspace+extras+temp/工具链缓存)/ReadRoots/可选禁读;Windows 无 bash 沙箱 → off;网络可选;写者内置工具在 tool/builtin 单独限制 | ②执行安全(D10 Linux 基准) |
| 96 | **中断回合恢复** | internal/provider/provider.go:137-158 | — | **中断的持久化交接**:Pending/CompletedTools(不复制参数结果,规范消息是真相源)/InterruptedTools/DroppedPartialText/Reasoning 标记;**原始部分推理留在 LocalOnly Message,绝不复制进恢复 prompt** | ②中断恢复(#21 相关) |

### 🟡 中价值新增(2 个)

| # | 域 | 文件 | 体量 | 设计要点 |
|---|----|------|:--:|---------|
| 97 | **远程执行 Remote** | internal/remote/(client/dial/knownhosts/jump/forward/sftpfs/agent_unix) | ~2,000 | SSH 全套:knownhosts 错误硬化/dial 硬化/jump host/agent 转发/sftpfs——远程执行环境 |
| 98 | **决策收据 DecisionReceipt** | internal/provider/provider.go:125-137 | — | ID/Kind/Tool/Subject/Outcome——权限决策的可审计收据 |

---

## 二、跨项目印证(执行环境归并)

| 维度 | Pi | Hermes | Reasonix |
|------|----|--------|----------|
| 沙箱 | restore-sandbox-env(36 行最小) | environments 7 种后端 | **OS 级 jail(Seatbelt/bwrap,fail closed)** |
| 远程 | — | ssh/daytona/modal | **SSH 全套(knownhosts/jump/sftpfs)** |
| 中断恢复 | findOpenOperations 三态 | interrupted 不持久化 | **InterruptedTurnRecovery(工具摘要+局部推理 LocalOnly)** |
| 权限收据 | — | approval 队列 | **DecisionReceipt(可审计)** |

**新增通用模式**:
1. **"沙箱是策略之下的强制层"** — 权限规则(政策)与 OS jail(强制)两层:被许可的命令仍不能逃出盒子;无后端 fail closed
2. **"中断的部分推理绝不进恢复 prompt"** — 部分推理 = 展示用 LocalOnly,结构性事实(完成/中断工具)才是恢复输入——与 #21 交接"只传结论不传死路"同哲学
3. **"决策收据"** — 每个权限决策可审计(ID/Kind/Outcome)

---

## 三、累计覆盖对账

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
| v13 | sandbox/remote/provider 恢复层 | +4 | **98** |

> 剩余:provider 各适配器细节、i18n/notify/secrets、desktop 前端——产品价值已低。98 域。
