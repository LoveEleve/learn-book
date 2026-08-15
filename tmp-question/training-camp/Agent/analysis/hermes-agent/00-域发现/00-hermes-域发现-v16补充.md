# Hermes 域发现 v16 补充(续扫第五轮:gateway 进程管理/api_server)— 2026-08-14

> 承接:v15。本轮:hermes_cli/gateway.py(7,668)+ gateway/platforms/api_server.py(7,521)。
> 结论:优雅重启与 SSE 协议确认,无新域。

---

## 一、v16 深化确认

### hermes_cli/gateway.py(7,668)

| 设计 | 位置 | 要点 |
|------|------|------|
| **SIGUSR1 优雅重启** | :242-297 | _request_gateway_self_restart/_graceful_restart_via_sigusr1(**drain_timeout** 后 SIGUSR1);_wait_for_pid_exit |
| **pid 祖先判定** | :180-227 | _is_pid_ancestor_of_current_process——**进程关系验证** |
| **网关 pid 扫描** | :357-611 | _scan_gateway_pids/_filter_venv_launcher_stubs(过滤启动器桩) |
| **profile 网关进程** | :649-676 | find_profile_gateway_processes/_gateway_run_args_for_profile |

### api_server(7,521)

| 设计 | 位置 | 要点 |
|------|------|------|
| **SSE 帧** | :188 | _sse_frame(event 字段/ascii)——流式事件协议 |
| **线程安全队列** | :162 | ThreadSafeAsyncQueue——跨线程事件 |
| **请求消毒** | :261-267 | _clean_request_string——**输入清洗** |
| **运行时覆盖** | :313-372 | _apply_runtime_agent_overrides/_resolve_request_runtime_agent_kwargs——每请求 agent 覆盖 |

---

## 二、关键设计(通用价值)

1. **"SIGUSR1 优雅重启"**:drain 后信号重启——**优雅重启协议**(与 Reasonix serve/restart、drain_control 同族)
2. **"pid 祖先验证"**:进程关系证明——**进程身份验证**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v15 | — | 80 | 80 |
| v16 | gateway 进程管理/api_server | +0(深化 2 设计) | **80**(深化) |

> 继续:next 轮 platforms/base.py(7,322)细看/plugins.py(6,318)细看/update_cmd(5,893)。
