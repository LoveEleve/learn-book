# Hermes 域发现 v28 补充(续扫第十七轮:1,000-1,500 行区间对账)— 2026-08-14

> 承接:v27。本轮:1,000-1,500 行区间对账。
> 结论:区间内文件大部分已覆盖(memory_tool/skill_usage/skills_guard/session_search/background_review 等);未覆盖的 4 个(mcp_oauth/plugin_llm/shell_hooks/lsp)确认无独立新域。

---

## 一、v28 确认(1,000-1,500 区间)

| 文件 | 要点 | 状态 |
|------|------|------|
| tools/mcp_oauth(1,427) | 端口预留/缓存重定向 URI/**非交互拒绝**(OAuthNonInteractiveError) | 确认 |
| agent/plugin_llm(1,217) | 插件 LLM 接口(文本/图像/结构化)+ **信任策略 allowlist** | 确认 |
| agent/shell_hooks(1,134) | ShellHookSpec(配置注册/stdin JSON 通信 spawn) | 确认 |
| agent/lsp/servers(1,187) | 语言服务器 spawn(pyright 检测/根标记) | 确认 |
| 其余 30+ 文件 | memory_tool/skill_usage/skills_guard/session_search/background_review/redact/turn_context/relay_llm/memory_manager/registry/batch_runner 等 | 已覆盖(此前轮次) |

**关键确认:plugin_llm 的信任策略**(插件 LLM 调用的 allowlist 白名单——插件调用 LLM 的能力受信任策略门控)。

---

## 二、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v27 | — | 81 | 81 |
| v28 | 1,000-1,500 区间对账 | +0(4 确认) | **81**(确认) |

> 继续:next 轮 500-1,000 行区间抽查(数量大,抽查即可)+ tests 契约补查——按需。
