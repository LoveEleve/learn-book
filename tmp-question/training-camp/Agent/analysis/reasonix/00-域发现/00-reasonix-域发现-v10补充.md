# Reasonix 域发现 v10 补充(第五轮深扫:boot 运行时组装层)— 2026-08-14

> 承接:v9。v10 深扫 internal/boot(boot.go 2,893/runtime.go 575)——此前完全未打开的运行时组装层。
> 结论:boot 是"启动正确性"的完整样本(迁移族/凭证保护先于一切/同步 sink/成本报价链/权限记忆),新增 5 个中高价值域。

---

## 一、v10 新增域

### 🔴 高价值新增(2 个)

| # | 域 | 文件 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 81 | **运行时组装 Build** | internal/boot/boot.go(2,893) | 2,893 | **build() 完整组装**:6 个配置迁移族先于 Load;凭证保护层(FilterSubprocessEnv/ProtectSensitiveFiles/RegisterCredentialEnvKeys)**先于任何工具/hook/插件 subprocess 启动**;同步 sink(后台 job 自己的 goroutine 发射,与回合发射可重叠→共享同步 sink);**成本报价链**(Coalesce→GoalUsageTee→Sync→CostQuote→Recorder→frontend,所有消费者同一时刻报价);BuildResult(Controller+扩展内核快照+RuntimeOwner 生命周期) | ②执行组装 |
| 82 | **权限规则记忆** | internal/boot/boot.go:2082-2130 | — | **rememberPermissionRule**:用户"always allow"→ 持久化到 reasonix.toml;coveredBy 检查(已有规则覆盖则跳过)+ **pruneCoveredPermissionRules**(新规则剪掉被覆盖的旧规则);LockConfigFileEdits 锁编辑 | ①权限记忆 |

### 🟡 中价值新增(3 个)

| # | 域 | 文件 | 体量 | 设计要点 |
|---|----|------|:--:|---------|
| 83 | **计划模式只读信任** | boot.go:2130-2207 | — | coveredPlanModeReadOnlyCommand/pruneCoveredPermissionRules——只读命令信任的覆盖检查与剪枝 |
| 84 | **子代理模型解析** | boot.go:2216-2278 | — | subagentModelRef/EffortRef/SubagentModelKeys——子代理模型键解析链 |
| 85 | **运行时生命周期** | boot/runtime.go(575) | 575 | RuntimeOwner(会话血缘生命周期):独立 build 独立 owner;RebuildFrom 复用旧 owner 只排空旧代;侧车 Manager 随 Controller 代死亡 |

---

## 二、boot 层关键设计(启动正确性)

1. **迁移先于加载**:6 个配置迁移族(v1/v0.5/DeepSeek 协议/step 限制/redact/memory 编译器/多阈值压缩)在 Load 前跑——"旧配置必须能启动"
2. **凭证保护先于一切**:secrets 保护层在**任何工具/hook/插件 subprocess 能 spawn 之前**武装——"进程模型的安全前提"
3. **同步 sink**:后台 job 与回合发射可重叠 → 共享同步 sink——"多发射者的并发安全"
4. **成本报价链**:所有消费者(统计/CLI/ACP/desktop)看到同一时刻报价——"成本一致性"
5. **权限记忆去重剪枝**:新 allow 规则覆盖旧的 → 剪枝,防规则库膨胀

**产品④映射**:
- "迁移先于加载" → 知识库版本迁移必须先于读(旧版本日志必须能打开)
- "凭证保护先于一切" → 知识库的敏感信息过滤必须在任何子进程前武装
- "权限记忆去重剪枝" → 知识库规则的覆盖检查(新结论覆盖旧结论 → 剪枝,与 #24 结论矛盾检测同思路)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v5 | 原始 | 41 | 41 |
| v6 | 体量排序复测 | +12 | 53 |
| v7 | 策略/契约/存储层 | +14 | 67 |
| v8 | 验收报告层 | +6 | 73 |
| v9 | 执行正确性/并行层 | +7 | 80 |
| v10 | boot 运行时组装层 | +5 | **85** |

> 剩余:bot 网关细节、extension 内部、provider 适配、i18n/notify——产品价值持续衰减。85 域已远超闭环笔记需求(现有 9 份笔记覆盖核心)。
