# Hermes 域发现 v45 补充(续扫第三十四轮:agent/transports 完整)— 2026-08-14

> 承接:v44。本轮:agent/transports/(codex_app_server/codex_event_projector/hermes_tools_mcp_server/types)。
> 结论:**hermes_tools_mcp_server(工具即 MCP)确认**,无新域。

---

## 一、v45 深化确认(agent/transports)

| 文件 | 设计要点 |
|------|---------|
| **hermes_tools_mcp_server(1,292?)** | **把 Hermes 工具暴露为 MCP 服务器**(工具 schema → MCP 工具定义,签名推断)——**工具即 MCP**(反向 MCP 桥) |
| **codex_app_server_session(1,292)** | Codex App 服务器会话(请求路由/通知归属/OAuth 失败分类/TurnResult) |
| **codex_event_projector** | 事件投影 + **确定性 call id**(item_type+item_id 派生——防重放错配) |
| **types(规范化)** | ToolCall/Usage/**NormalizedResponse(多后端统一响应)**/map_finish_reason(跨后端 finish reason 映射) |
| verify/(environment/recipes) | manifest 加载/recipe 检测(v3/v7 已覆盖) |

**价值**:hermes_tools_mcp_server 是"MCP 双向桥"的另一半(mcp_serve.py 是 MCP→Hermes,这是 Hermes→MCP 工具面)——工具生态互操作。

---

## 二、关键设计(通用价值)

1. **"工具即 MCP"**:Hermes 工具自动暴露为 MCP——**工具生态双向互操作**
2. **"确定性 call id"**:事件投影的 id 派生(防重放错配)——**幂等性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v44 | — | 81 | 81 |
| v45 | agent/transports 完整 | +0(深化 2 设计) | **81**(深化) |

> 继续:next 轮 tests 契约(agent/hermes_cli/cron 面)补查——按需收尾。
