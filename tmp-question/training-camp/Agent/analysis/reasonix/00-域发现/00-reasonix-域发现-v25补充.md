# Reasonix 域发现 v25 补充(续扫第十一轮:cli 补全/转录)— 2026-08-14

> 承接:v24。本轮:internal/cli/(complete 849/transcript 790)。
> 结论:补全系统(斜杠目录缓存失效/共享 ArgData)确认——TUI 细节,产品价值低;无新域。

---

## 一、v25 深化确认(cli 层)

| 设计 | 位置 | 要点 |
|------|------|------|
| **斜杠补全目录** | complete.go:68-108 | slashItems/buildSlashCatalog + **invalidateSlashCatalog(缓存失效)**/refreshHostAndInvalidate(宿主变更 → 失效) |
| **参数补全共享** | :273-297 | slashArgItems → slashArgData(control.ArgData——**与 desktop 共享补全逻辑**) |
| **子命令补全** | :322-343 | explicitSubcommandItems/bareSubcommandSpace(裸子命令空格判定) |
| **转录渲染** | transcript.go:19-226 | transcriptSource 跟踪/append/set/remove 块/**replay 捆绑渲染**/回合收据带(renderTurnReceiptBand)/reflow |

---

## 二、关键设计(通用价值)

1. **"缓存失效协议"**:宿主变更 → 失效斜杠目录——**动态目录的一致性**(与 Hermes reload_skills 同族)
2. **"补全共享函数"**:CLI 与 desktop 用同一 ArgData——**多前端一致性**(v16 已记,此处验证)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v24 | — | 102 | 102 |
| v25 | cli 补全/转录 | +0(深化 1 设计) | **102**(深化) |

> 继续:next 轮 appidentity/i18n、migration、trajectory、eventwire——支撑层收尾。
