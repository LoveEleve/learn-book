# Hermes 域发现 v17 补充(续扫第六轮:platforms/base.py 细看)— 2026-08-14

> 承接:v16。本轮:gateway/platforms/base.py(7,322)完整实现。
> 结论:代理解析/SSRF 防护/媒体缓存确认,无新域。

---

## 一、v17 深化确认(platforms/base.py)

| 设计 | 位置 | 要点 |
|------|------|------|
| **UTF-16 长度处理** | :219-253 | utf16_len/_prefix_within_utf16_limit/_custom_unit_to_cp——**平台消息长度按 UTF-16 算** |
| **代理解析族** | :272-536 | 网络可达性/系统代理检测(macOS)/no_proxy 匹配/should_bypass_proxy/resolve_proxy_url/bot+aiohttp 两种代理 kwargs——**代理的完整解析** |
| **SSRF 重定向防护** | :699 | _ssrf_redirect_guard(异步重定向防护) |
| **媒体缓存** | :830-1005 | 图像嗅探(_looks_like_image)/URL 缓存(重试 2)/音频嗅探(_sniff_audio_ext)/缓存清理(max_age)——**媒体安全缓存** |
| **入站媒体限制** | :758-799 | get_inbound_media_max_bytes/validate_inbound_media_size/_read_httpx_body_with_limit——**入站媒体有界** |
| **TTS 句柄** | :592-660 | AudioFormat/StreamingTTSHandle/streaming_tts_turn_key |

---

## 二、关键设计(通用价值)

1. **"UTF-16 长度"**:平台消息按 UTF-16 计长(Telegram 等)——**多字节平台的长度正确性**
2. **"代理解析 + SSRF 防护"**:代理完整解析 + 重定向防护——**网络层安全**
3. **"媒体嗅探缓存"**:按魔数嗅探类型(不信扩展名)——**类型验证**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v16 | — | 80 | 80 |
| v17 | platforms/base.py | +0(深化 3 设计) | **80**(深化) |

> 继续:next 轮 plugins.py(6,318)细看/update_cmd(5,893)/slash_commands(5,772)。
