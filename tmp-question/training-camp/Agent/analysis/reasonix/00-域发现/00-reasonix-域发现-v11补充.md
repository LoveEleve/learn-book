# Reasonix 域发现 v11 补充(第六轮深扫:bot 消息网关层)— 2026-08-14

> 承接:v10。v11 深扫 internal/bot(gateway.go 2,972/session/render/pairing)——消息网关层。
> 结论:bot 是"多平台消息驱动 agent"的完整实现(队列策略/审批超时/流式渲染/配对),新增 5 个中高价值域。

---

## 一、v11 新增域

### 🔴 高价值新增(2 个)

| # | 域 | 文件 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 86 | **消息网关 BotGateway** | internal/bot/gateway.go(2,972) | 2,972 | 多平台消息网关:队列 4 模式(**steer/followup/collect/interrupt**)+ 3 丢弃策略(**summarize/old/new**)+ 队列上限(20);**审批超时**(ApprovalTimeout 防被弃 prompt 永久卡死会话,#4626/#4402,负值=无限等待);IgnoreSelfMessages(SelfUserIDs + 最近出站 id);配对(TTL/上限);控制通道(ControlAddr/Token) | ②执行(消息驱动形态) |
| 87 | **消息渲染 renderSink** | internal/bot/render.go(736) | 736 | 事件流 → 平台消息;**messageEditor 原地编辑**(适配器可选能力:飞书 Im.Message.Patch → 回合中流式输出,同一 live 消息持续更新,非攒到结束分段发) | ②事件渲染 |

### 🟡 中价值新增(3 个)

| # | 域 | 文件 | 体量 | 设计要点 |
|---|----|------|:--:|---------|
| 88 | **会话队列策略** | internal/bot/session.go(454) | 454 | QueueMode 4 种 + QueueDrop 3 种 + QueueCap——busy 时入队策略 |
| 89 | **设备配对** | internal/bot/pairing.go(371) | 371 | 配对流(TTL/最大 pending) |
| 90 | **平台适配器族** | internal/bot/(weixin 666/feishu 1,010/qq 793) | 2,469 | 各平台适配器(与 Hermes gateway/platforms 同构) |

---

## 二、跨项目印证(消息网关归并)

| 维度 | Hermes | Reasonix |
|------|--------|----------|
| 队列策略 | _queue_or_replace_pending_event | **QueueMode 4 种 + QueueDrop 3 种** |
| 审批超时 | approval 队列 | **ApprovalTimeout(防永久卡死)** |
| 流式渲染 | draft streaming | **messageEditor 原地编辑** |
| 平台适配 | platforms/ 20+ | bot/(weixin/feishu/qq) |
| 自消息忽略 | — | **IgnoreSelfMessages(SelfUserIDs+出站 id)** |

**新增通用模式**:
1. **"审批超时防卡死"** — 被弃的审批 prompt 不能永久阻塞会话(Reasonix #4626/#4402 ↔ Hermes 审批队列)
2. **"busy 时入队而非丢弃"** — 队列 4 模式 + 3 丢弃策略是消息驱动的完整决策
3. **"原地编辑流式"** — 回合中 live 消息持续更新(消息平台版的流式输出)

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
| v11 | bot 消息网关层 | +5 | **90** |

> 剩余:extension 内部(rpcwire/sidecar/uihub)、provider 适配细节、i18n/notify 支撑——产品价值持续衰减。90 域远超闭环笔记需求。
