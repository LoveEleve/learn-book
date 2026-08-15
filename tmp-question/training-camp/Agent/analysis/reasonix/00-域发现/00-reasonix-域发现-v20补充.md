# Reasonix 域发现 v20 补充(续扫第六轮:config 加载/guardian 守卫)— 2026-08-14

> 承接:v19。本轮:internal/config/load.go(2,498)+ internal/guardian(633)。
> 结论:config backfill 族与 guardian 回滚审查确认,深化 ②;无新域。

---

## 一、v20 深化确认

### config 加载(2,498)

| 设计 | 位置 | 要点 |
|------|------|------|
| **多入口** | load.go:25-68 | Load/LoadForRoot/LoadForRootReadOnly/**LoadUserConfigReadOnly**;loadForRoot(migrateOnDisk 开关——**只读加载不迁移**) |
| **backfill 族** | :360-470 | backfillDeepSeekPro/官方价格/端点默认(officialProviderKind 判定)——**默认值回填而非报错** |
| **默认值** | :252-269 | LoadBuiltinDefaultsForRoot/LoadRecoveryDefaultsForRoot(恢复专用默认) |
| **TOML 插件合并** | :492 | mergeTOMLPlugins |
| **键存在检测** | :325-346 | tomlFileDefinesKey(文件是否定义某键——区分默认与显式) |

### guardian(长活守卫 — 独立审查器)

| 设计 | 位置 | 要点 |
|------|------|------|
| **Review 独立判定** | guardian.go:124-139 | Review/ReviewVerdict(toolName+args → allow/reason)——**每个工具调用的守卫审查** |
| **回滚审查** | :301-331 | rollbackReview(before messages + rewriteBefore)——**审查失败回滚** |
| **角色交替规范化** | :332-388 | normalizeAlternation + hasConsecutiveUserMessages(连续 user 消息检测) |
| **游标持久化** | :254-279/397 | PathFor/CursorPathFor + TranscriptCursor |
| **会话验证** | :412 | validateLoadedSession(加载后验证) |

---

## 二、关键设计(通用价值)

1. **"backfill 而非报错"**:缺失的默认值自动回填(DeepSeek Pro 配置/价格/端点)——**配置兼容的前进路径**
2. **"只读加载不迁移"**:LoadForRootReadOnly 不写磁盘——**读取与迁移分离**(与 capdiag 只读诊断同哲学)
3. **"审查失败回滚"**:guardian 审查失败 → rollbackReview——**独立审查器的失败安全**(与 rq3 boundedllm 渐进收缩同族)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v19 | — | 102 | 102 |
| v20 | config 加载/guardian 守卫 | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 installsource/installlayout(安装层)、taskcatalog/usagecatalog(目录)、billing(报价)。
