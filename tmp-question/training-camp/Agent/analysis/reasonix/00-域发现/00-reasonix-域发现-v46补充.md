# Reasonix 域发现 v46 补充(续扫第三十二轮:execute_one 门控链/credentials/repair plan)— 2026-08-14

> 承接:v45。本轮:internal/agent/execute_one.go(884)+ config/credentials(857)+ repair/plan(853)。
> 结论:execute_one 的 **9 阶段门控链**确认——②执行工具调用的完整安全管线。

---

## 一、v46 深化确认

### execute_one 门控链(884)

| 阶段 | 位置 | 要点 |
|------|------|------|
| parseToolCall | :67 | 解析规范工具/拒绝歧义/重复成功与 stale 锚守卫 |
| **interceptToolBefore** | execute_one.go:39-42 | **扩展先于策略**:tool.before 在任何策略/权限检查前裁决;有效替换重新解析(后续阶段看到将执行的调用) |
| resolveToolPolicy | :136 | 工具策略解析 |
| applyContextualToolGate | :159 | 上下文工具门 |
| **applyMutationDependencyBarrier** | :195-231 | 先写失败 → 状态变异跳过(验证命令也跳过);结构化 shell 元数据(FailurePhase=ShellPhaseDependency) |
| applyPlanModeAndProxy | :231-358 | 计划模式/代理解析/解析目标重检/MCP 计划可用性 |
| applyDeliveryPolicyGates | :358 | 交付策略门 |
| applyRecoveryAndPermission | :441 | 恢复 + 权限 |
| prepareToolExecution/finishToolExecution | :553/702 | 准备/完成(变异观察/写释放 defer) |

### credentials / repair plan

| 设计 | 要点 |
|------|------|
| **CredentialResolver** | ResolveGlobalFirst(全局优先)/credentialEnvNamesForRoot/ProviderEntry.ResolveAPIKeyForRoot |
| **RepairPlan** | 结构化计划 + RepairPlanID/PreviewID(状态指纹派生 ID) |

---

## 二、关键设计(通用价值)

1. **"扩展先于策略"**:tool.before 在权限检查前裁决——**拦截优先级**(可替换执行)
2. **"门控链的 defer 释放"**:mutationObserved/写释放都进 defer——**异常路径也正确释放**
3. **"结构化失败元数据"**:bash 卡的 ShellPhaseDependency 结构化字段——**失败可程序化处理**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v45 | — | 102 | 102 |
| v46 | execute_one 门控链/credentials/repair | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 subagent_store(949)/bot/project_index(831)/config/migrate(827)。
