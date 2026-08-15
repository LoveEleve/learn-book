# Reasonix 域发现 v32 补充(续扫第十八轮:event Kind 全集)— 2026-08-14

> 承接:v31。本轮:internal/event/event.go(884)全貌。
> 结论:**30+ 种事件 Kind 全集确认**——每种事件的渲染语义文档化,②执行事件层完整。

---

## 一、v32 深化确认(event Kind 全集)

| 事件族 | Kind | 渲染语义 |
|--------|------|---------|
| 回合 | TurnStarted(无载荷,重置回合渲染)/TurnDone(Err 非 nil 失败;**用户取消不是错误**——总是回合最后事件) | 生命周期 |
| 流式 | Reasoning(思考增量,静音"thinking"头)/Text(答案增量)/Message(完整,可重渲染为 markdown) | 三阶段 |
| 工具 | ToolDispatch(宣布)/ToolResult(完成)/**ToolProgress(长工具实时 chunk)** | 镜像压缩 |
| 压缩 | CompactionStarted(占位)/CompactionDone(摘要;**aborted 也发空 Summary 让占位符解除**) | 占位符协议 |
| 交互 | ApprovalRequest(阻塞至 Approve)/AskRequest(结构化多选,阻塞至 AnswerQuestion) | 阻塞语义 |
| 守护 | GuardianAssessment(子代理安全审查结果) | 安全 |
| 扩展 | ExtensionSurface(卡片/表单/通知)/ExtensionStatus(一行状态) | 结构化 UI |
| 重试 | Retrying(Attempt of Max)/StreamAttempt(begin/discard/commit,host-local) | 失败可见 |
| 状态 | Steer(队列消息已注入确认)/TurnPhase(working/checking/verifying/reviewing)/WorkspaceChanged(防抖)/CompletionSummary(内容无关质量摘要)/ContextMaintenanceEvent(免付费摘要卡片) | 状态 |
| 哨兵 | **KindCount(新事件必须插在其上,完备性测试自动覆盖)** | 契约 |

---

## 二、关键设计(通用价值)

1. **"KindCount 哨兵"**:新事件必须插在 KindCount 上——**完备性测试自动覆盖**(加新事件忘测试 = 编译/测试失败)
2. **"aborted 压缩也发 Done"**:空 Summary 让占位符解除——**占位符协议的正确性**(UI 永不悬挂)
3. **"用户取消不是错误"**:TurnDone Err nil 含用户取消——**取消语义的明确**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v31 | — | 102 | 102 |
| v32 | event Kind 全集 | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 tool.go 契约/permission(956)/plugin/oauth——按需。
