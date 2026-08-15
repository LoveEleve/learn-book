# Hermes 域发现 v31 补充(续扫第二十轮:tests 契约)— 2026-08-14

> 承接:v30。本轮:tests/ 契约补查。
> 结论:**issue 编号回归测试族确认**(每个 issue 一个测试 = 契约锁定),无新域。

---

## 一、v31 确认(tests 契约)

### 关键观察:issue 编号回归测试

```
tests/gateway/(20+ issue 编号测试):
  test_10710_auto_reset_evicts_cached_agent   — 自动重置驱逐缓存 agent
  test_13121_shutdown_inflight_transcript_flush — 关闭时在途转录 flush
  test_25107_stale_base_url_api_mode           — stale base URL
  test_35809_auto_reset_clean_context          — 自动重置干净上下文
  test_35994_reset_button_deadlock             — 重置按钮死锁
  test_42039_duplicate_user_message            — 重复用户消息
  test_64674_multiplex_primary_token_scope     — 多路复用 token 作用域
  test_7100_transient_failure_transcript       — 瞬态失败转录
  test_73771_media_resend_dedup                — 媒体重发去重
  test_75349_whatsapp_multiplex_secret_scope   — WhatsApp 秘密作用域
  ...
```

**"每个 issue 一个回归测试" = 失败模式的契约锁定**——bug 修复后行为被测试固化,防回归。

### 测试目录全貌(2,906 个)

- gateway(最大,含 issue 回归族)/agent/hermes_cli/run_agent/cron/hermes_state/state/skills/providers/plugins/monitoring/acp/dashboard/e2e/stress/integration/install

---

## 二、关键设计(通用价值)

1. **"issue 编号测试"**:每个修复的 bug 变成测试(编号可追溯)——**失败模式的契约化**(与 Pi/Reasonix 测试即契约同思想,但 issue 编号可追溯到根因)
2. **"回归测试命名即文档"**:测试名描述问题场景(deadlock/duplicate/dedup)——**可读的契约**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v30 | — | 81 | 81 |
| v31 | tests 契约 | +0(契约族确认) | **81**(确认) |

> 继续:next 轮 500-1,000 行区间抽查(agent/tools 剩余)/plugins 内部复核——按需收尾。
