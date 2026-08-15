# Hermes 域发现 v20 补充(续扫第九轮:cron scheduler/browser_tool)— 2026-08-14

> 承接:v19。本轮:cron/scheduler.py(5,432)细看 + tools/browser_tool.py(5,383)。
> 结论:运行 job 注册/中断标记与浏览器命令超时族确认,无新域。

---

## 一、v20 深化确认

### cron/scheduler(5,432)

| 设计 | 位置 | 要点 |
|------|------|------|
| **运行 job 注册** | :486-571 | get_running_job_ids/try_register_running_job(跨进程互斥)/release_running_job/**mark_running_jobs_interrupted(中断广播)**/_is_interrupted/_consume_interrupted_flag |
| **prompt 注入阻塞** | :282 | CronPromptInjectionBlocked——**cron prompt 注入检测** |
| **工具集解析** | :295-448 | _resolve_cron_disabled_toolsets/_merge_mcp_into_per_job_toolsets/_resolve_cron_enabled_toolsets(每 job 工具集) |
| **池管理** | :744-775 | 并行池/顺序池/关闭——**并发执行** |
| **读写锁** | :608 | _ReadWriteLock(自定义) |

### browser_tool(5,383)

| 设计 | 要点 |
|------|------|
| **命令超时族** | _get_command_timeout/_safe_command_timeout/_get_open_command_timeout(first_open 区分)——**超时分层** |
| **沙箱绕过检测** | _needs_chromium_sandbox_bypass——环境检测 |
| **URL 日志消毒** | _sanitize_url_for_logs |
| **环境构建** | _build_browser_env/homebrew node 目录发现/_merge_browser_path——**浏览器路径解析** |

---

## 二、关键设计(通用价值)

1. **"中断广播"**:mark_running_jobs_interrupted——**跨 job 中断传播**(kill 全部)
2. **"超时分层"**:常规/安全/首次打开不同超时——**超时的场景化**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v19 | — | 80 | 80 |
| v20 | cron scheduler/browser_tool | +0(深化 2 设计) | **80**(深化) |

> 继续:next 轮 approval(4,919)细看/tts_tool(4,502)/config_defaults(4,547)——按需。
