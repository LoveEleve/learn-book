# Reasonix 域发现 v21 补充(续扫第七轮:安装层/目录服务)— 2026-08-14

> 承接:v20。本轮:internal/installsource(734)+ taskcatalog(710)+ usagecatalog(558)。
> 结论:三个 catalog 都是 projectiondb 的可丢弃投影(业务数据在库外),深化 ④;无新域。

---

## 一、v21 深化确认

### installsource(安装)

| 设计 | 要点 |
|------|------|
| **安装工具 install_source** | MCP 连接(MCPConnector)/审批流(ApprovalFunc 批量动作审批)/卸载/资源清理(cleanupActionResources)——**安装 = 审批 + 应用 + 清理** |

### taskcatalog / usagecatalog(目录 — projectiondb 投影)

| 设计 | 要点 |
|------|------|
| **task_events 表** | taskcatalog/catalog.go:150 — project_key/task_id/sequence/timestamp/event_type——事件投影 |
| **游标分页** | cursor 结构(Page/EventPage)——分页一致性 |
| **ObservedStore** | :192 — 接 taskmonitor.FileStore(**观察层 ↔ 投影打通**) |
| **usagecatalog** | Enqueue 收据(append 收据)/Rollup 汇总——用量投影 |
| **投影可丢弃** | 两个 catalog 都是 projectiondb 的可丢弃投影(migrations() 定义)——**业务数据在库外,库可重建** |

---

## 二、关键设计(通用价值)

1. **"可丢弃投影族"**:taskcatalog/usagecatalog/sessioncatalog/historycatalog 全是 projectiondb 投影——**"业务数据权威在库外,所有 SQLite 目录可重建"是 Reasonix 的存储架构定论**(与 Hermes FTS fail-open 同哲学)
2. **"观察层 ↔ 投影打通"**:ObservedStore 让 taskmonitor(观察层)写 taskcatalog(投影)——**观察与投影的单向数据流**
3. **"安装 = 审批 + 应用 + 清理"**:安装动作的完整生命周期

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v20 | — | 102 | 102 |
| v21 | 安装层/目录服务 | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 billing(报价)、shellsafe(效果检测)、taskpolicy 已覆盖——按需。
