# hq35 MCP 服务器双向桥(MCP Serve)— 产品②"MCP 扩展"蓝本

> 项目:Hermes(mcp_serve.py 1,037 行 + tests/test_mcp_serve.py 等 102 用例)
> 假设:外部客户端(Claude Code/Cursor/Codex)经 MCP 操作 Hermes——Hermes 的 stdio MCP 服务器是"MCP 桥"的样本(域发现 v7:OpenClaw 9 工具桥面;v44:与 hermes_tools_mcp_server 构成双向桥)。
> 结论:✅ 成立——9 工具桥面/会话列表/消息读写/事件轮询/权限响应/双向桥全具备,产品②"MCP 扩展"直接蓝本。

---

## 一、架构全景:MCP ↔ Hermes 双向桥

```
┌────────────────────────────────────────────────────────────┐
│ MCP → Hermes(mcp_serve.py):stdio MCP 服务器               │
│   OpenClaw 9 工具桥面:                                    │
│   conversations_list/conversation_get/messages_read/       │
│   attachments_fetch/events_poll/events_wait/messages_send/ │
│   permissions_list_open/permissions_respond               │
│   ——外部客户端操作 Hermes 消息面                          │
├────────────────────────────────────────────────────────────┤
│ Hermes → MCP(hermes_tools_mcp_server):                    │
│   Hermes 工具自动暴露为 MCP 服务器(域发现 v44)            │
│   ——构成双向桥:MCP 可操作 Hermes,Hermes 工具可被 MCP 用   │
├────────────────────────────────────────────────────────────┤
│ 数据源:                                                  │
│   _get_session_db/_load_sessions_index(DB/JSON 双源)/      │
│   _load_channel_directory(渠道目录)                        │
├────────────────────────────────────────────────────────────┤
│ 事件:QueueEvent(293)/events_poll/events_wait(轮询/等待)   │
│ 权限:permissions_list_open/respond(挂起审批响应)           │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:MCP → Hermes(9 工具桥面)

**位置**:`mcp_serve.py:1-25`(模块头)+ 工具族

```
stdio MCP 服务器:任何 MCP 客户端(Claude Code/Cursor/Codex)可:
- 列会话/读消息/拉附件(conversations_list/get/messages_read/attachments_fetch)
- 轮询/等待事件(events_poll/wait——live 消息)
- 发消息(messages_send)
- 权限(permissions_list_open/respond——挂起审批响应)

匹配 OpenClaw 9 工具桥面(外部客户端经 MCP 操作 Hermes)
```

**正确性价值**:外部客户端完整操作面(读/写/事件/审批)——OpenClaw 兼容面。

**产品④映射**:MCP 扩展面——外部客户端操作消息面(读/写/事件/审批)。

## 设计 2:数据源抽象(DB/JSON 双源)

**位置**:`mcp_serve.py:72-234`

```
_get_session_db(会话库)/_load_sessions_index(DB 优先,JSON 回退)/
  _load_channel_directory(渠道目录)
_extract_message_content/_extract_attachments(消息内容/附件提取)
_coerce_int(参数强制)

——会话数据源统一(DB/JSON 兼容)
```

**正确性价值**:双源统一(DB/JSON 兼容,迁移期安全);提取/强制工具化。

**产品④映射**:MCP 数据源抽象——存储差异内化。

## 设计 3:事件轮询/等待

**位置**:`mcp_serve.py:293`(QueueEvent)+ 事件族

```
QueueEvent(293):事件队列模型
events_poll:轮询新事件;events_wait:等待(挂起直到新事件)
  ——live 消息面(非只读历史)

_ts_float(301):时间戳工具
```

**正确性价值**:live 事件面(轮询 + 等待)——外部客户端实时感知。

**产品④映射**:MCP 事件面——轮询/等待双模式(live 感知)。

## 设计 4:权限响应

**位置**:`mcp_serve.py`(permissions_list_open/respond)

```
permissions_list_open:列挂起审批
permissions_respond:响应审批(allow/deny)
  ——外部客户端可处理 Hermes 审批(远程操作)

与 approval 系统衔接(域发现 v7:permissions_respond)
```

**正确性价值**:远程审批——外部客户端处理挂起审批(远程操作面)。

**产品④映射**:MCP 审批面——挂起审批远程响应。

## 设计 5:双向桥(Hermes → MCP)

**位置**:`tests/agent/transports/test_hermes_tools_mcp_server.py`(关联)

```
hermes_tools_mcp_server:Hermes 工具自动暴露为 MCP 服务器(域发现 v44)
  ——与 mcp_serve.py(MCP→Hermes)构成双向桥:
    外部客户端操作 Hermes + Hermes 工具可被外部 MCP 宿主用
```

**正确性价值**:双向桥——一个进一个出(MCP 可操作 Hermes,Hermes 工具可被外部用)。

**产品④映射**:MCP 双向扩展——进出两个方向都通。

---

## 三、与四项目对比(MCP 扩展)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes mcp_serve |
|------|----|----------|----------|-----|-------------------|
| MCP 服务器 | — | — | MCP 客户端 | — | **stdio 服务器(9 工具桥面)** |
| 方向 | — | — | 客户端 | — | **双向(进+出)** |
| 数据源 | — | — | — | — | **DB/JSON 双源** |
| 事件 | — | — | — | — | **轮询/等待 live 面** |
| 权限 | — | — | — | — | **远程审批响应** |

**结论**:产品"MCP 扩展"参考 = Hermes mcp_serve(双向桥 + 9 工具面 + live 事件 + 远程审批)。**与 OpenCode 弃用 MCP 不同(MVP 不需要)——Hermes 保留为扩展点(域发现 v7)**。

---

## 四、面试弹药

1. **"OpenClaw 9 工具桥面"**:conversations/messages/events/permissions 完整面——外部客户端操作 Hermes
2. **"双向桥"**:mcp_serve(MCP→Hermes)+ hermes_tools_mcp_server(Hermes→MCP)——进出都通
3. **"live 事件面"**:events_poll/wait——非只读历史,实时感知
4. **"远程审批"**:permissions_respond——外部客户端处理挂起审批
5. **"DB/JSON 双源"**:迁移期兼容——数据源差异内化

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 9 工具桥面 | 外部客户端操作面 |
| 数据源抽象 | DB/JSON 双源统一 |
| 事件轮询/等待 | live 消息面 |
| 权限响应 | 远程审批 |
| 双向桥 | MCP 进出都通 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_mcp_serve.py + test_mcp_server_log_notifications.py + transports/test_hermes_tools_mcp_server.py(102 用例)
> 位置:mcp_serve.py:63-293(会话库/索引/渠道目录/事件族)/ 9 工具桥面(OpenClaw 兼容)
> 双向:hermes_tools_mcp_server(Hermes 工具 → MCP,域发现 v44)
