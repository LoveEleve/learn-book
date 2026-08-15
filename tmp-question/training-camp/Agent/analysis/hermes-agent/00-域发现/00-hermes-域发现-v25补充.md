# Hermes 域发现 v25 补充(续扫第十四轮:credential_pool/model_switch)— 2026-08-14

> 承接:v24。本轮:agent/credential_pool.py(3,178)细看 + hermes_cli/model_switch(3,368)。
> 结论:池化凭证的耗尽语义确认,无新域。

---

## 一、v25 深化确认

### credential_pool(3,178)

| 设计 | 位置 | 要点 |
|------|------|------|
| **耗尽 TTL** | :310-345 | _exhausted_ttl/_exhausted_until(sole_credential 例外)——**凭证耗尽的时间语义** |
| **重试延迟提取** | :375 | _extract_retry_delay_seconds(从错误消息提取)——**自适应退避** |
| **优先级** | :301 | _next_priority(下一个最低优先级) |
| **播种族** | :2484-2880 | _seed_from_singletons/_seed_from_env(env 优先 dotenv)——**凭证来源播种** |
| **写穿全局根** | :578 | _write_through_provider_state_to_global_root |
| **池策略** | :521 | get_pool_strategy(provider → 策略) |

### model_switch(3,368)

| 设计 | 要点 |
|------|------|
| **模型声明检测** | _declared_model_ids/_models_config_is_allowlist——**配置意图解析** |
| **别名解析** | _load_direct_aliases/DirectAlias |
| **模型警告** | _check_hermes_model_warning(hermes 非 agentic 模型警告) |

---

## 二、关键设计(通用价值)

1. **"耗尽 TTL + sole 例外"**:多凭证耗尽轮换;唯一凭证耗尽例外(不立即死)——**凭证池的耗尽语义**
2. **"从错误提取重试延迟"**:429 的 Retry-After 解析——**自适应退避**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v24 | — | 80 | 80 |
| v25 | credential_pool/model_switch | +0(深化 2 设计) | **80**(深化) |

> 继续:next 轮 anthropic_adapter(3,216)/gateway/platforms/qqbot(3,273)/tui_gateway/methods_session(3,304)——同构适配器,快速核对。
