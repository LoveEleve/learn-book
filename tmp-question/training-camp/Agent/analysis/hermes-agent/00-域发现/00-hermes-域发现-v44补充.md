# Hermes 域发现 v44 补充(续扫第三十三轮:gateway 剩余中小文件)— 2026-08-14

> 承接:v43。本轮:gateway/ 500 以下剩余文件核对。
> 结论:**shutdown_flush 的关闭冲刷协议确认**(关闭时数据不丢),深化 ④。

---

## 一、v44 深化确认(gateway 剩余)

| 文件 | 设计要点 |
|------|---------|
| **shutdown_flush(461)** | **关闭时冲刷协议**:flush_pending_to_file/spool_dropped_transcript_message(丢弃的转录消息先 spool)/**fsync_directory(目录 fsync 防目录项丢失)**/recover_pending_to_db(启动恢复)/flush_agent_history_to_file——**关闭/崩溃时在途数据不丢** |
| **session_state(476)** | TurnState/ConversationState/**PersistentState 三层状态** + SessionFieldView/TurnLeaseTokenView(字段视图,legacy 兼容属性) |
| **agent_cache_pressure(310)** | AgentCacheBounds + _cgroup_limit_bytes(cgroup 内存限制感知——缓存边界动态) |
| **slash_access(229)** | 网关斜杠命令访问控制 |
| 其余(已覆盖) | delivery_ledger/turn_lease/lifecycle_ledger/wake/drain_control/mirror/stream_events/scale_to_zero/memory_monitor |

---

## 二、关键设计(通用价值)

1. **"关闭冲刷 + 目录 fsync"**:丢弃消息先 spool 文件,恢复时回灌;目录 fsync 防目录项丢失——**关闭路径的数据正确性**(与 Reasonix shutdown_flush 同族)
2. **"cgroup 感知缓存"**:缓存边界随内存限制动态——**自适应资源**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v43 | — | 81 | 81 |
| v44 | gateway 剩余 | +0(深化 2 设计) | **81**(深化) |

> 继续:next 轮 agent/transports 完整 + tests 契约(agent/hermes_cli 面)——按需。
