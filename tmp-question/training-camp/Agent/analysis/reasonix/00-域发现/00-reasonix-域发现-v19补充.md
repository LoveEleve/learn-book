# Reasonix 域发现 v19 补充(续扫第五轮:capdiag/capability/doctor)— 2026-08-14

> 承接:v18。本轮:internal/capdiag(820)/capability(407)/doctor。
> 结论:**capability Entry 是"能力路由"的完整数据模型(18 字段)——产品①对齐模块 B 档推荐的数据结构**,深化 ①。

---

## 一、v19 深化确认

### capability(能力清单 — 对齐模块相关)

| 设计 | 位置 | 要点 |
|------|------|------|
| **Entry 18 字段** | capability.go:43-66 | ID/Kind(5 类)/Name/Description/Source/Status(5 态)/**ReadOnly/Destructive/Cost/AutoUse(off/suggest/prefer/require 四档)**/**Triggers/NegativeTriggers**/NeedsFreshData/ToolName/ConnectSource/Requires(依赖)/Profiles(economy/balanced/delivery)/AutoStart/FailureReason(host-proven) |
| **RouteDecision** | :68-84 | Candidates(RouteCandidate: Entry+Policy+Reason)/**Delivery 画像代理路由**(use_capability 稳定代理 vs connect_tool_source 未注册)/CapabilityProxy |
| **SkillEntries** | :86+ | 技能 → 能力条目(工具就绪检测 run_skill/read_skill) |

### capdiag(诊断)

| 设计 | 要点 |
|------|------|
| **只读诊断** | Collect **永不写 config/cache/state/log**;Live MCP 可选(opt-in) |

### doctor(九类报告)

- ConfigReport/ProviderReport/PluginReport/LSPReport/SessionsReport/SandboxReport/NetworkReport/PermissionReport + RenderText

---

## 二、关键设计(通用价值)

1. **"能力条目 = 路由数据模型"**:18 字段(触发词/依赖/画像/成本/自动使用四档)——**产品①对齐模块 B 档"推荐带证据"的数据结构**(agent 能力清单 → 按画像推荐)
2. **"负触发词"**:NegativeTriggers(什么场景不该用)——**路由的否定约束**
3. **"画像路由"**:Profiles(economy/balanced/delivery)——**按成本/用途画像路由**
4. **"只读诊断"**:诊断永不写任何东西——**诊断的纯观测性**(与 taskmonitor 纯观察层同哲学)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v18 | — | 102 | 102 |
| v19 | capdiag/capability/doctor | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 config 细节(load 2,498)、installsource/installlayout(安装层)、guardian(长活守卫细节)。
