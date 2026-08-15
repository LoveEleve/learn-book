# Hermes 域发现 v24 补充(续扫第十三轮:terminal_tool/cron jobs/file_operations)— 2026-08-14

> 承接:v23。本轮:tools/terminal_tool(3,841)/cron/jobs(3,271)/tools/file_operations(3,241)。
> 结论:终端守卫与文件写正确性确认,无新域。

---

## 一、v24 深化确认

### terminal_tool(3,841)

| 设计 | 要点 |
|------|------|
| **sudo 密码缓存** | 缓存作用域(_get_sudo_password_cache_scope)/缓存/重置——**sudo 处理** |
| **守卫检查族** | _check_all_guards/_docker_has_host_access(_docker_volume_uses_host_path 卷检测)/_check_vercel_sandbox_requirements——**环境安全前置** |
| **工作目录验证** | _validate_workdir(安全字符白名单)——**路径安全** |
| **错误消毒** | _redact_terminal_error_text |

### cron/jobs(3,271)

| 设计 | 要点 |
|------|------|
| **跨进程锁** | _jobs_lock_file/_jobs_lock + 一次运行 claim TTL(_oneshot_run_claim_ttl_seconds) |
| **记录规范化** | _normalize_job_record/_coerce_job_text(强制文本) |

### file_operations(3,241)

| 设计 | 要点 |
|------|------|
| **行尾/BOM 处理** | _detect_line_ending/_normalize_line_endings/_strip_bom——**跨平台文本正确性** |
| **写拒绝** | _is_write_denied——**路径写门** |

---

## 二、关键设计(通用价值)

1. **"跨平台文本正确性"**:行尾规范化 + BOM 处理——**文件写的平台适配**(Windows/Unix)
2. **"sudo 缓存作用域"**:缓存带作用域(防跨会话泄漏)——**凭据缓存的安全边界**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v23 | — | 80 | 80 |
| v24 | terminal/cron jobs/file_operations | +0(深化 2 设计) | **80**(深化) |

> 继续:next 轮 agent/anthropic_adapter(3,216)/credential_pool(3,178)/model_switch(3,368)——按需收尾。
