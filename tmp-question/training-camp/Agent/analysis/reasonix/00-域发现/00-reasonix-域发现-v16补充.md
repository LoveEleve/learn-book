# Reasonix 域发现 v16 补充(续扫第二轮:control 剩余 — approval/slash/goal/refs)— 2026-08-14

> 承接:v15。本轮:internal/control 剩余(approval 670/slash 671/goal 1,051/refs 1,358)。
> 结论:approvalManager 的"严格叶子"与 goalMachine 状态机确认,深化 ②执行;无新域。

---

## 一、v16 深化确认(control 剩余)

| 设计 | 位置 | 要点 |
|------|------|------|
| **approvalManager 严格叶子** | approval.go:9-50 | **只动自己的状态,绝不回调 Controller**(bookkeeping 提取,orchestration 留 Controller——"approval 阻塞用户输入且有副作用,只提取记账");自己的锁 off c.mu;三种模式 ask/auto/yolo;approvalTimeout 防走开的用户永久卡会话(#4626/#4402);计划自动批准(planAutoApprove) |
| **slash 共享补全** | slash.go | SlashItem(Label/Insert/Hint/**Descend 下一级**);**ArgData 共享函数**(CLI 与 desktop 用同一补全逻辑——两前端提示一致) |
| **goalMachine 状态机** | goal.go:73-350 | 预算分类(legacy 模式映射)/goalState 快照/**continuationToken 续跑令牌**/严格模式/legacy 归档阻塞;goalStatePath 持久化 |
| **@-引用注入** | refs.go | **maxFileRefBytes 防大文件炸上下文**(保留头+截断标注);@-引用文件注入消息 |

---

## 二、关键设计(通用价值)

1. **"严格叶子"**:只动自己状态、绝不回调父——**组件隔离的防死锁原则**(approval 在自有锁下,不与 Controller 互锁)
2. **"共享补全逻辑"**:两前端用同一 ArgData 函数——**多前端一致性**(与 Hermes COMMAND_REGISTRY 单一事实源同思想)
3. **"continuationToken"**:目标续跑令牌——**无人值守循环的恢复锚点**
4. **"@-引用防炸"**:文件注入上限+截断标注——**上下文保护**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v15 | — | 102 | 102 |
| v16 | control 剩余(approval/slash/goal/refs) | +0(深化 4 设计) | **102**(深化) |

> 继续:next 轮 checkpoint 源码验证(rq7 覆盖后验证)、acp 完整服务(3,057)、config 细节。
