# Reasonix 域发现 v38 补充(续扫第二十四轮:recovery Gate/serve)— 2026-08-14

> 承接:v37。本轮:internal/recovery/gate.go(1,415)+ internal/serve/serve.go(1,684)。
> 结论:Gate 的代际观察隔离与成功验证清除预算确认——自动守卫的完整语义。

---

## 一、v38 深化确认

### recovery Gate(1,415 — 自动守卫)

| 设计 | 位置 | 要点 |
|------|------|------|
| **代际观察隔离** | gate.go:510-525 | **StaleObservationsIgnored**:旧代观察忽略(防重新武装旧锁)——模式切换/剧集轮换中的迟到结果 |
| **成功验证清除预算** | :525-535 | 主机识别的验证成功 → **清除 Episode no-progress 预算**;任何成功变异同样清除 |
| **诊断证据摘录** | :535-545 | 诊断性读成功不清失败状态,但**保留有界证据摘录**(隔离审查器看到"失败+提议 diff+调查连接") |
| **失败指纹计数** | :545+ | observationFingerprint 指纹计数(上限 255) |
| **代际/剧集** | :132-169 | EpisodeID/Generation/BeginEpisode——恢复守卫状态机 |

### serve(1,684)

| 设计 | 要点 |
|------|------|
| **Controller 重建** | rebuild → boot.Rebuild(复用 runtime owner,只排空旧代) |
| **模型切换锁** | switchModelLocked(bindMu 串行化) |
| **扩展热重载** | reloadExtensions(HTTP 端点) |
| **CORS** | HandlerWithCORS(origin 配置) |

---

## 二、关键设计(通用价值)

1. **"成功验证清除失败预算"**:验证成功 → no-progress 预算清零——**成功的重置语义**(与 Hermes compression ineffective 清除同族)
2. **"诊断证据摘录"**:只保留有界证据(不丢调查连接)——**审查器的上下文供给**
3. **"失败指纹计数"**:指纹而非文本匹配——**失败去重**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v37 | — | 102 | 102 |
| v38 | recovery Gate/serve | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 evidence 剩余(Receipt 类型全集)/cli 剩余/autoresearch/store——按需收尾。
