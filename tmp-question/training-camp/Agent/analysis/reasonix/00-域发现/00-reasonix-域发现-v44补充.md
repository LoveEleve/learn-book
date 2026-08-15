# Reasonix 域发现 v44 补充(续扫第三十轮:openai provider/skill 系统)— 2026-08-14

> 承接:v43。本轮:internal/provider/openai(1,331)+ internal/skill(skill.go 1,425/tools.go)。
> 结论:**renderInline 技能钉 sentinel(压缩保原文)**确认——与 Hermes 技能幽灵重注入同族的重要机制。

---

## 一、v44 深化确认

### skill 系统(1,425)

| 设计 | 位置 | 要点 |
|------|------|------|
| **技能钉 sentinel** | tools.go:640-646 | **renderInline:`<skill-pin name=...>` 包裹,让上下文压缩保原文而非改写**——与 Hermes 技能幽灵重注入(Pi 无对应)同族 |
| **cleanSkillName** | :648-663 | 清理装饰名(模型把索引的 "explore [🧬 subagent]" 原样复制进 name 参数——剥 [..] 取首 token) |
| **Render 格式** | :632-638 | # Skill: name/描述/作用域/正文/参数 |
| **ValidateInvocation** | skill.go:363-377 | Requires 依赖的可用性检查(requiresReady 回调)——不可用 → ErrInvocationUnavailable |
| **画像过滤** | :379-408 | AllowedInProfile/FilterForProfile(空 profiles = 全部;角色设置不再过滤索引——保留给诊断) |

### openai provider(1,331)

| 设计 | 要点 |
|------|------|
| **流式空闲超时 120s** | defaultStreamIdleTimeout |
| **前缀续接** | maxPrefixContinuations 1 + streamWithPrefixContinuation(完整文本/推理累积 + **Beta 失败在无续接字节时安全回退——首响应保持可见**) |
| **推理协议** | RequiresToolCallReasoning/RequiresReasoningRoundTrip/DeepSeek 工具调用推理检查 |

---

## 二、关键设计(通用价值)

1. **"技能钉让压缩保原文"**:sentinel 包裹防摘要改写指令——**压缩的指令保护**(与 Hermes 幽灵重注入、Pi compaction 标记同族——三项目共证"压缩不能丢指令")
2. **"装饰名清理"**:模型复制索引装饰 → 清理——**输入的宽容处理**
3. **"Beta 失败安全回退"**:无续接字节时保持首响应可见——**失败的可恢复性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v43 | — | 102 | 102 |
| v44 | openai provider/skill 系统 | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 config/edit.go(2,442)/render.go(1,816)/credentials.go(857)。
