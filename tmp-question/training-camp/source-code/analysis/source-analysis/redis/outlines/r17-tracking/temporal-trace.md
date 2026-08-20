# R-17 客户端缓存 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2019 (6.0 开发期) | tracking.c 版权 **2019-Present** (L3) — 客户端缓存为 6.0 头条特性 (2020-05 发布) |
| 6.0.0 | CLIENT TRACKING / CLIENT CACHING / CLIENT GETREDIR (commands.def L1542-1556 实证); **双层 rax 表 + BCAST 前缀 + OPTIN/OPTOUT + REDIRECT + NOLOOP** 均在初版 |
| 6.2.0 | CLIENT TRACKINGINFO (commands.def L1558 实证) — 诊断面补全 (flags/redirect/prefixes) |
| 7.0.x | **#11715 回归修复**: MULTI 队列 + 限额驱逐 → 失效消息穿插 QUEUED 回复的崩溃 (测试 L746-782 实证, debug pause-cron 复现) — 催生/强化 pending 延迟机制 |
| 7.x (推断) | **延迟发送成熟**: tracking_pending_keys + execution_nesting 门控 (L412-438) — 事务/脚本响应不穿插 (测试 L428-464); CLIENT_TRACKING_BROKEN_REDIR 通知 (L263-273) — 重定向目标消失场景 (测试 L255-277, 2022 前后); lazyfree 异步表回收 (lazyfree.c:219-232) |

## 痕迹证据

- tracking.c:1-8: 模块自述 "Client side caching: keys tracking and invalidation"
- tracking.c:12-22: 双层 rax 设计注释 (keys → client IDs)
- tracking.c:42-45: disableTracking 惰性清理注释 ("remove the ID reference in a lazy way ... cost a lot of time")
- tracking.c:342-352: trackingInvalidateKey bcast 参数双语义注释 (真实修改 vs 驱逐伪失效)
- tracking.c:393-401: pending 延迟注释 ("may be interleaved with command response and should after command response")
- tracking.c:442-445: FLUSH 发 NULL 注释 ("avoid flooding clients with many invalidation messages")
- server.c:1500-1504: 双调用点注释 (CONFIG SET 后 idle 也要生效)
- server.c:3710-3725: call() 记住面注释 (RO 脚本豁免 + 外部客户端身份)
- redis.conf:833-867: 配置文档 (1M 默认/0=无限/BCAST 零内存/INFO 统计)

## 推断标注

- "NOLOOP 初版即有" — commands.def History=NULL 无法直接实证版本, 由 6.0 设计文档风格推断 (标注)
- "7.x 延迟发送成熟" — #11715 测试存在 + execution_nesting 字段为 7.0 kvstore 时代引入 (标注)
- "tracking-redir-broken 2022 前后" — 与 #11715 同期测试风格推断 (标注)
- 仓库浅克隆 (单 commit a0a6f23) 无法 git 考古 — 全部时期线依赖 commands.def/注释/测试, 已逐条标注实证级别
