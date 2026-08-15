# Hermes 域发现 v12 补充(续扫第一轮:顶层大文件对账)— 2026-08-14

> 触发:用户要求继续深挖 Hermes。按 Pi/Reasonix 方法做"体量排序 × 已打开"对账,发现大量顶层大文件未打开(域发现 v1-v11 的 80 域偏重 agent/ 内部,顶层文件只扫了结构)。
> 结论:**gateway/run.py 29,065 行(项目最大)等多达 15+ 个顶层大文件未细看**——"80 域收官"是假收敛(Hermes 第 2 次证伪)。

---

## 一、v12 新增/深化(顶层大文件)

### 对账发现的未打开文件(全部 >5,000 行)

| 文件 | 体量 | 角色 |
|------|:--:|------|
| gateway/run.py | **29,065** | 网关运行时(项目最大,只扫过接口层) |
| hermes_cli/web_server.py | 18,310 | 网页服务器 |
| tui_gateway/server.py | 14,488 | TUI 网关 |
| hermes_cli/main.py | 12,899 | CLI 入口(profile override 先于 argparse) |
| hermes_cli/kanban_db.py | 11,717 | Kanban 数据库 |
| hermes_cli/auth.py | 9,299 | CLI 认证 |
| tools/mcp_tool.py | 7,752 | MCP 工具 |
| hermes_cli/gateway.py | 7,668 | CLI 网关命令 |
| gateway/platforms/api_server.py | 7,521 | API 服务器平台 |
| gateway/platforms/base.py | 7,322 | 平台抽象(v7 扫过结构) |
| hermes_cli/plugins.py | 6,318 | 插件系统(v6 扫过) |
| hermes_cli/update_cmd.py | 5,893 | 更新 |
| gateway/slash_commands.py | 5,772 | 网关斜杠命令 |
| hermes_cli/models.py | 5,752 | 模型管理 |
| hermes_cli/tools_config.py | 5,553 | 工具配置 |
| cron/scheduler.py | 5,432 | 调度器(域发现已覆盖) |
| tools/browser_tool.py | 5,383 | 浏览器工具 |

### 深化确认(gateway/run.py)

| 设计 | 位置 | 要点 |
|------|------|------|
| **消息→agent 管线** | _run_agent_inner(26123) | 代理模式先行(_get_proxy_url → _run_agent_via_proxy)/**线程池运行不阻塞事件循环**/run_generation 检查当前会话(_run_still_current)/每平台工具集解析 |
| **GatewayRunner mixin 族** | 6200 | GatewayAuthorizationMixin/GatewayKanbanWatchersMixin/GatewaySlashCommandsMixin |
| **hygiene 压缩族** | :155-251 | _hygiene_cooldown_for_failure/_reset_hygiene_failure_streak(卫生压缩的冷却/失败连击) |

---

## 二、跨项目印证

1. **"线程池运行不阻塞事件循环"**:Hermes gateway ↔ Reasonix Controller(run_in_session)——异步网关的通用模式
2. **"代理模式先行"**:proxy URL → 远程 API 服务器——**远程执行路由**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v11 | — | 80 | 80 |
| v12 | 顶层大文件对账 | +0(对账确认 17 个未打开) | **80**(对账) |

> 教训:v11"80 域收官"声明证伪——顶层 17 个 >5,000 行文件未细看(域发现偏重 agent/ 内部)。继续:next 轮逐个打开这些文件确认设计。
