# Reasonix 域发现 v49 补充(续扫第三十五轮:responses/anthropic provider)— 2026-08-14

> 承接:v48。本轮:internal/provider/(responses 899/anthropic 850)——provider 适配族收官核对。
> 结论:anthropic 原生缓存断点设计确认——provider 层 prompt 缓存的实现细节。

---

## 一、v49 深化确认(provider 适配族)

### anthropic(850)

| 设计 | 位置 | 要点 |
|------|------|------|
| **原生缓存断点** | anthropic.go:414-428 | **ephemeral CacheControl 标记**:system 最后块/tools 最后块/最后消息最后块——**缓存断点的放置策略**(tools→system→messages 顺序) |
| **DeepSeek 例外** | :165-166/414-415 | DeepSeek 忽略 cache_control 自动管理前缀缓存——**按 provider 的缓存策略差异** |
| **first-party 定价** | :165 | 原生端点默认 5 分钟缓存写定价 |

### responses(899)

| 设计 | 要点 |
|------|------|
| **Responses API 适配** | 推理禁用判定(responsesReasoningDisabled)/自动输出预算(vendor+effort)/splitInstructions(system/instructions 拆分)/**stale 前响应错误检测**(isStalePreviousResponseError——重放旧响应拒绝) |

---

## 二、关键设计(通用价值)

1. **"缓存断点放置策略"**:ephemeral 标记放在块末尾(前缀匹配)——**prompt 缓存的最大命中设计**
2. **"按 provider 的缓存策略差异"**:DeepSeek 自动管理 → 不发 cache_control——**缓存协议的 provider 适配**
3. **"stale 前响应拒绝"**:旧响应错误检测——**重放保护**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v48 | — | 102 | 102 |
| v49 | responses/anthropic provider | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 bot/feishu(1,010)/qq(793)/weixin(666)——平台适配器,快速核对同构性。
