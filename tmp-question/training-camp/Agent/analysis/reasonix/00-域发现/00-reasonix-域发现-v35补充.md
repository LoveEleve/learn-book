# Reasonix 域发现 v35 补充(续扫第二十一轮:compact 压缩实现细节)— 2026-08-14

> 承接:v34。本轮:internal/agent/compact.go(678)内部——rq4 覆盖后的源码级细节。
> 结论:折叠经济学/CJK token 估算/固定前缀保留确认,深化 ②上下文管理。

---

## 一、v35 深化确认(compact 实现)

| 设计 | 位置 | 要点 |
|------|------|------|
| **触发预算族** | compact.go:91-157 | compactTrigger(触发判定)/hardInputCeiling/recentTailBudget/**checkpointCeiling**(检查点上限)/exceptionalMinimumSavings |
| **折叠经济学** | :159-162 | foldEconomics:区域 ≥ 400 token 才折叠——**折叠的最小收益门槛** |
| **CJK 感知 token 估算** | :193-208 | 字节/4 vs rune 数取大(英文 4 字节/token,CJK 1 字符/token)——**跨语言估算** |
| **固定前缀保留** | :292-308 | pinnedPrefixLen = system + 首个固定 user 回合(fixedPinnableUserTurn 预算 = 上下文窗口比例);**首 user 回合固定保留** |
| **保留策略** | :310-330 | keepIndexes:**保留仅适用于最新摘要后的消息**;更早的消息下次折叠可再叠(不会永远增长) |
| **摘要超时** | :56 | summaryTimeout 90s |

---

## 二、关键设计(通用价值)

1. **"折叠最小收益门槛"**:<400 token 区域不折叠——**压缩的经济性决策**(防无效压缩)
2. **"CJK 感知估算"**:字节/字符双估算——**多语言 token 估算正确性**
3. **"固定前缀 = system+首回合"**:首 user 回合固定保留——**对齐锚点不丢**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v34 | — | 102 | 102 |
| v35 | compact 实现细节 | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 remote/sftpfs、appidentity、desktoplauncher——支撑收尾。
