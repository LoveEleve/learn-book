# Hermes 域发现 v33 补充(续扫第二十二轮:agent 剩余抽查)— 2026-08-14

> 承接:v32。本轮:agent/ 中小文件抽查(credits_tracker 852/context_breakdown 360/bounded_response 148 等)。
> 结论:有界错误读与上下文分解确认,无新域。

---

## 一、v33 深化确认

| 文件 | 设计要点 |
|------|---------|
| **bounded_response(148)** | 流式错误响应体**有界读**:字节上限 + **硬墙钟截止**(防恶意服务器悬挂);httpx iter_bytes 阻塞在 C/socket 读内(块间检查无法中断)——**有界 I/O 的正确实现** |
| **context_breakdown(360)** | 会话上下文窗口分解估算(system prompt 分层/工具 schema/历史,与压缩阈值同启发式)——**上下文可视化** |
| **credits_tracker(852)** | x-nous-credits-* 头部硬解析(版本/余额/订阅/SIGNED 债务)+ **耗尽检测**(paid_access)+ 订阅上限比例+ schema 版本 warn-once——**头部契约解析** |
| 其余 | subscription_view/trace_upload/replay_cleanup/reasoning_timeouts/backend_identity 等(支撑细节) |

---

## 二、关键设计(通用价值)

1. **"有界读的墙钟截止"**:字节上限不够(可能悬挂)——**必须双边界**(大小+时间)
2. **"头部契约硬解析"**:credits 头部的版本化解析(欠费为 SIGNED)——**外部契约的严格解析**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v32 | — | 81 | 81 |
| v33 | agent 剩余抽查 | +0(深化 2 设计) | **81**(深化) |

> 继续:next 轮 tools/ 剩余抽查/plugins 内部复核——按需收尾。
