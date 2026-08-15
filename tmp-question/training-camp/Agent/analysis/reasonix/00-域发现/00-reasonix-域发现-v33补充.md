# Reasonix 域发现 v33 补充(续扫第十九轮:permission/tool 契约)— 2026-08-14

> 承接:v32。本轮:internal/permission(956)/internal/tool/tool.go(683)。
> 结论:permission 三列表决策与 tool 接口族确认,深化 ②安全/③扩展。

---

## 一、v33 深化确认

### permission(956)

| 设计 | 位置 | 要点 |
|------|------|------|
| **三列表决策** | :144-199 | Policy(allow/ask/deny 三列表)New(mode, allow, ask, deny);**Decide(toolName, readOnly, args)**——决策入口 |
| **bash 分段判定** | :285-304 | decideBashSegments(分段命令逐段判定——组合命令分段决策) |
| **规则匹配** | :324-527 | matchAny/matchAnyRaw/rawBashPrefixMatches/大小写不敏感 PowerShell cmdlet;**RuleCoversString(规则覆盖检测**——新规则是否被已有覆盖) |
| **决策枚举** | :18-44 | Decision(枚举+String+Parse) |

### tool(683)

| 设计 | 要点 |
|------|------|
| **接口族** | Tool(基础)/ContextualTool(上下文)/Previewer(变更预览)/ImageTool/PlanModeClassifier/**ReadOnlyExecutionHostMutation/BlockReason**/MCPMetadata 族(MCP 可见元数据/绑定/注解/服务端授权) |
| **执行意图上下文** | WithReaderExecutionIntent/HasReaderExecutionIntent/NonDestructiveMCPExecutionIntent/PlanReplacementAuthorization——**执行意图注入** |

---

## 二、关键设计(通用价值)

1. **"bash 分段判定"**:组合命令逐段决策(allow 一段 deny 一段 → 按段)——**命令级权限的粒度**
2. **"规则覆盖检测"**:RuleCoversString——**规则去重/剪枝的基础**(与 boot rememberPermission 剪枝呼应)
3. **"执行意图注入"**:reader/non-destructive/plan-replacement 意图上下文——**执行意图显式化**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v32 | — | 102 | 102 |
| v33 | permission/tool 契约 | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 plugin/oauth、session_events、branch/compact 细节——按需收尾。
