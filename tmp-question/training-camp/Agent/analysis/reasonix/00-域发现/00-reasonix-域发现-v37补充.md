# Reasonix 域发现 v37 补充(续扫第二十三轮:plugin 宿主/hook 系统)— 2026-08-14

> 承接:v36。本轮:internal/plugin/plugin.go(1,969)+ internal/hook/hook.go(1,592)——两个 1,500+ 行核心未细看。
> 结论:hook 系统(15 事件/仅 2 阻塞/超时分级)与 plugin 分阶段启动确认,深化 ②③。

---

## 一、v37 深化确认

### hook 系统(1,592)

| 设计 | 位置 | 要点 |
|------|------|------|
| **15 种事件** | hook.go:41-79 | PreToolUse/PostToolUse/PostToolUseFailure/PermissionRequest/UserPromptSubmit/Stop/StopFailure/**PostLLMCall(钩子 stdout 可替换推理内容)**/SessionStart/End/SubagentStop/Notification/**PreCompact(stdout 注入压缩指导)** |
| **仅 2 种阻塞** | :80-93 | IsBlocking = PreToolUse/UserPromptSubmit;**PreCompact 不阻塞只贡献指导**;Claude 导入的 PermissionRequest 也阻塞(exit 2 契约) |
| **超时分级** | :95-104 | 门事件(PreToolUse/Permission/UserPromptSubmit)5s;其余 30s——**门事件更紧** |
| **作用域加载** | :186-234 | GlobalSettingsPath/ProjectSettingsPath/**项目钩子先于全局**/Load/appendPluginHooks |

### plugin 宿主(1,969)

| 设计 | 位置 | 要点 |
|------|------|------|
| **分阶段启动** | plugin.go:321-360 | Start:**信号量并发启动**(concurrency 上限,收集按 idx 稳定 /mcp status)/每插件超时 PerPluginTimeout |
| **三种启动** | :292-321 | StartAll(全部)/StartAvailable(可用的)/Start(策略) |
| **Host 服务** | :212-560 | ReadResource/Close/**queueBackgroundWrite(背景写队列)**/StartPhaseB/fetchPrompts/fetchResources(后台表面加载) |
| **授权启动** | :674-686 | AuthorizeSpecLaunch/AuthorizeProjectSpecLaunch(启动前授权) |

---

## 二、关键设计(通用价值)

1. **"阻塞事件的显式枚举"**:15 种事件只有 2 种能阻塞循环——**钩子的影响面显式化**(PreCompact 只贡献指导不阻塞)
2. **"超时分级"**:门事件 5s vs 其余 30s——**阻塞点超时更紧**(与 Hermes 拦截超时同族)
3. **"信号量并发启动"**:收集按 idx 稳定——**并发与稳定顺序的平衡**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v36 | — | 102 | 102 |
| v37 | plugin 宿主/hook 系统 | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 recovery/gate(1,415 自动守卫)/serve.go(1,684)/evidence 剩余。
