# Hermes 域发现 v22 补充(续扫第十一轮:config_defaults/CLI mixin)— 2026-08-14

> 承接:v21。本轮:hermes_cli/config_defaults.py(4,547)+ cli_commands_mixin.py(3,650)。
> 结论:agent 缓存 LRU 权衡与 CLI 会话命令族确认,无新域。

---

## 一、v22 深化确认

### config_defaults(4,547)

| 设计 | 位置 | 要点 |
|------|------|------|
| **agent 缓存 LRU 权衡** | DEFAULT_CONFIG.agent.agent_cache | max_size 128/idle_ttl 3600/**匿名 RSS 预算自动派生**(cgroup 内存限制)——"缓存换内存,太小时每轮重付未缓存 prompt,太大时工具重转录填满堆" |
| **WAL 配置** | database | journal_mode wal(弱 fsync 共享文件系统用 DELETE——macOS virtiofs/NFS/SMB)/wal_autocheckpoint/journal_size_limit |
| **运行时 fd 上限** | runtime | nofile_soft_limit 4096(钳制到 OS 硬限) |
| **会话上限** | max_concurrent_sessions(全局)/max_live_sessions 16(LRU 驱逐 detached) |

### CLI mixin(3,650)

| 设计 | 要点 |
|------|------|
| 会话命令族 | rollback/diff/snapshot/export/import/stop/agents/journey/paste/copy/image/tools/profile/**handoff** |

---

## 二、关键设计(通用价值)

1. **"缓存权衡文档化"**:max_size/idle_ttl/RSS 预算的权衡明确——**配置默认值的理由**(产品④知识库缓存配置参考)
2. **"WAL 平台适配"**:弱 fsync 文件系统用 DELETE 模式——**WAL 的平台安全降级**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v21 | — | 80 | 80 |
| v22 | config_defaults/CLI mixin | +0(深化 2 设计) | **80**(深化) |

> 继续:next 轮 tts_tool(4,502)/terminal_tool(3,841)/gateway/session.py(3,966)——按需收尾。
