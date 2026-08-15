# Hermes 域发现 v14 补充(续扫第三轮:web_server/tui_gateway)— 2026-08-14

> 承接:v13。本轮:hermes_cli/web_server.py(18,310)+ tui_gateway/server.py(14,488)。
> 结论:Web 服务认证链与 TUI 会话槽位协议确认,无新域。

---

## 一、v14 深化确认

### web_server(18,310)

| 设计 | 位置 | 要点 |
|------|------|------|
| **6 个 HTTP 中间件** | :555-768 | 认证/授权/请求处理链 |
| **会话 token 认证** | :347-448 | _resolve_session_token/_has_valid_session_token/_has_valid_query_token(WS 升级用 query token)/_require_token |
| **SSH 会话安全** | :356-362 | _apply_ssh_session_token/_apply_ssh_owner_nonce |
| **宿主白名单** | :489-511 | should_require_auth(host 判定)/_is_accepted_host——**公网暴露保护** |
| **动态配置 schema** | :1105-1298 | _build_schema_from_config/_schema_with_dynamic_provider_options——**配置 schema 动态生成**(provider 选项) |
| **REST 端点族** | :2058+ | media/files/fs/ssh/health/status/system(20+) |

### tui_gateway(14,488)

| 设计 | 位置 | 要点 |
|------|------|------|
| **会话槽位协议** | :545-669 | _claim_active_session_slot/_release/_transfer(独占声明/释放/转移) |
| **孤儿会话检测** | :977 | _ws_session_is_orphaned |
| **_SlashWorker 子进程** | :365 | 斜杠命令在子进程执行(不阻塞 TUI) |
| **teardown 族** | :873-948 | _teardown_session/_teardown_popped_session(确定性清理) |
| **panic 钩子** | :74-103 | _panic_hook/_thread_panic_hook(线程异常捕获) |

---

## 二、关键设计(通用价值)

1. **"WS 用 query token"**:浏览器无法设 Authorization 头 → query token——**WS 升级认证**
2. **"宿主白名单公网保护"**:should_require_auth(host 判定)——**公网暴露的默认安全**
3. **"会话槽位独占"**:claim/release/transfer——**并发会话的独占协议**(与 Reasonix 会话槽位同族)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v13 | — | 80 | 80 |
| v14 | web_server/tui_gateway | +0(深化 3 设计) | **80**(深化) |

> 继续:next 轮 kanban_db.py(11,717)/mcp_tool.py(7,752)/gateway.py(7,668)。
