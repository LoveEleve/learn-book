# Hermes 域发现 v15 补充(续扫第四轮:kanban_db/mcp_tool)— 2026-08-14

> 承接:v14。本轮:hermes_cli/kanban_db.py(11,717)+ tools/mcp_tool.py(7,752)。
> 结论:MCP 父死看门狗与 Kanban 崩溃宽限/限流退出码确认,深化 ②执行。

---

## 一、v15 深化确认

### mcp_tool(7,752)

| 设计 | 位置 | 要点 |
|------|------|------|
| **父死看门狗** | :853-884 | stdio MCP 命令包在 mcp_stdio_watchdog.py --ppid 后——**POSIX getppid 检测父死**(MCP 子进程不孤儿化);非 POSIX 靠进程组 killpg;看门狗簿记失败不阻塞连接 |
| **描述威胁扫描** | :703 | _scan_mcp_description(server/tool/description 扫描)——**MCP 描述注入检测** |
| **安全 env 构建** | :578-610 | _build_safe_env(过滤敏感变量) |
| **错误消毒** | :611-640 | _sanitize_error/_exc_str(错误不泄露) |

### kanban_db(11,717)

| 设计 | 位置 | 要点 |
|------|------|------|
| **claim TTL 解析** | :390-420 | 显式值 > env 覆盖 > 默认(15min) |
| **崩溃宽限** | :433-451 | **30s grace 覆盖 fork→/proc 可见性窗口**(防误判新 worker 崩溃;15min claim TTL 仍抓真崩溃) |
| **限流退出码 75** | :470+ | **KANBAN_RATE_LIMIT_EXIT_CODE=75(EX_TEMPFAIL)**:限流/配额耗尽退出不计数失败——**熔断器不因瞬态节流跳闸**;reap 分类器映射为 rate_limited 释放回 ready |
| **board slug 作用域** | :535-685 | scoped_current_board/board_dir——多板隔离 |

---

## 二、关键设计(通用价值)

1. **"父死看门狗"**:MCP 子进程的父死检测——**子进程孤儿化防护**(进程生命周期正确性)
2. **"限流 ≠ 失败"**:退出码 75 语义分离(瞬态节流 vs 真失败)——**失败分类的语义精确**(与 Reasonix ClassifyFailure 同思想)
3. **"崩溃宽限防误判"**:fork 窗口的 grace——**活体检测的假阳性抑制**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v14 | — | 80 | 80 |
| v15 | kanban_db/mcp_tool | +0(深化 3 设计) | **80**(深化) |

> 继续:next 轮 gateway.py(7,668)/api_server.py(7,521)/platforms/base.py 细看(7,322)。
