# Hermes 域发现 v40 补充(续扫第二十九轮:gateway 剩余/acp_adapter)— 2026-08-14

> 承接:v39。本轮:gateway/(platform_registry/delivery/session_context)+ acp_adapter/server。
> 结论:delivery 的静默叙述检测确认(与 kanban_stop 同族),无新域。

---

## 一、v40 深化确认

| 文件 | 要点 |
|------|------|
| **gateway/delivery(646)** | 投递传输解析(resolve_delivery_transport)/**静默叙述检测(_is_silence_narration——投递内容是否只是叙述)**/Telegram 私聊 id 判定/线程丢失错误识别 |
| **gateway/platform_registry(680)** | **插件作用域检测**(_caller_plugin_scope——调用者来自哪个插件)/PlatformEntry/PlatformRegistry |
| **acp_adapter/server(2,484)** | 资源格式化(文本/图像 MIME 猜测/路径解析/嵌入式资源转 parts)——ACP 资源面 |
| **gateway/session_context(511)** | 会话上下文构建(v3 已覆盖面) |

**价值**:delivery 的 _is_silence_narration = 投递层的叙述检测(与 kanban_stop 的伪完成检测同哲学——"内容是否只是叙述")。

---

## 二、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v39 | — | 81 | 81 |
| v40 | gateway 剩余/acp_adapter | +0(深化 1 设计) | **81**(深化) |

> 继续:next 轮 hermes_cli/ 剩余命令 + cron/ 剩余 + docker/scripts 部署面——按需收尾。
