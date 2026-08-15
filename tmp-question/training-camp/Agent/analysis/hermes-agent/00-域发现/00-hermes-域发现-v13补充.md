# Hermes 域发现 v13 补充(续扫第二轮:CLI 入口/认证)— 2026-08-14

> 承接:v12。本轮:hermes_cli/main.py(12,899)+ hermes_cli/auth.py(9,299)。
> 结论:入口的 profile 机制与认证解析确认,无新域,深化 CLI/凭证域。

---

## 一、v13 深化确认

### hermes_cli/main.py(12,899 — 入口)

| 设计 | 位置 | 要点 |
|------|------|------|
| **profile override 先于 import** | :519-560 | `_apply_profile_override()` 在 argparse 前预解析 --profile/-p 并设 HERMES_HOME——**模块导入前生效** |
| **mcp add --args 透传边界** | :527-541 | `mcp add --args` 后的标志属于子命令(如 Docker MCP 的 --profile),**不是 Hermes 的 profile 选择器**——参数边界识别 |
| **sudo 用户解析** | :543-560 | `sudo hermes -p <name>` 用 SUDO_USER 的 home(profile 存储属调用者,root 只做特权动作) |
| **ultrafast version** | :404-480 | 快速版本输出(_try_ultrafast_version/_try_termux)——启动性能优化 |
| **环境检测族** | :374-399 | termux/container/global-fast 判定 |

### hermes_cli/auth.py(9,299)

| 设计 | 要点 |
|------|------|
| **ProviderConfig 解析** | :233+ 完整 provider 配置(密钥解析/端点探测) |
| **端点探测** | detect_zai_endpoint(超时 8s)/_resolve_zai_base_url——动态端点 |
| **认证错误族** | AuthError/is_rate_limited_auth_error/_parse_retry_after_seconds(RateLimit 重试头)/format_auth_error |
| **token 指纹** | _token_fingerprint(标识不泄露完整 token) |
| **OAuth 追踪** | _oauth_trace(事件+sequence_id——可诊断流) |
| **认证存储锁** | _auth_lock_path/_auth_lock_holder_for(跨进程) |

---

## 二、关键设计(通用价值)

1. **"profile 先于 import"**:HERMES_HOME 在模块导入前设置——**环境隔离的执行顺序**(与 Reasonix boot 迁移先于加载同思想)
2. **"参数透传边界"**:`--args` 后属于子命令——**CLI 参数所有权划分**
3. **"端点探测"**:zai 端点动态探测(8s 超时)——**provider 自适应**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v12 | — | 80 | 80 |
| v13 | CLI 入口/认证 | +0(深化 2 设计) | **80**(深化) |

> 继续:next 轮 web_server.py(18,310)/tui_gateway/server.py(14,488)/kanban_db.py(11,717)。
