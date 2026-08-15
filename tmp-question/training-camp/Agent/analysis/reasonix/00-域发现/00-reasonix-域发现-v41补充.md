# Reasonix 域发现 v41 补充(续扫第二十七轮:cli 剩余/autoresearch store 验证)— 2026-08-14

> 承接:v40。本轮:cli/(setup_manager 926/mcp 866/upgrade 785)+ autoresearch/store。
> 结论:rq1 的存储实现验证 + cli 设置冲突检测确认,无新域。

---

## 一、v41 深化确认

| 设计 | 位置 | 要点 |
|------|------|------|
| **ResumeFromGoalText 验证** | autoresearch/store.go:107-141 | ExplicitTaskID:前缀提取;**前缀出现后,畸形 ID/额外路径组件是错误而非普通目标文本**;validateTaskID |
| **Store 查询族** | :143-223 | LoadTask/Findings(limit)/Heartbeats/LastHeartbeat/Progress/ValidateTask——rq1 存储实现验证 |
| **provider 设置冲突检测** | cli/setup_manager.go:19-118 | **文件快照检测外部冲突**(providerSetupFileSnapshot——设置会话前/后快照比较,外部修改检测);记录变异 recordProviderMutation |
| **MCP CLI** | cli/mcp.go:31-434 | parseMCPAdd/名称消毒(sanitizeMCPName)/远程 URL 检测/默认名从 argv 推导/注册表客户端(mcpBrowse/Install) |
| **upgrade** | cli/upgrade.go:74-356 | 发布渠道解析/版本规范化/渠道归属判定 |

---

## 二、关键设计(通用价值)

1. **"设置会话的冲突检测"**:文件快照前后比较——**设置向导防外部并发修改覆盖**(与 Hermes memory 漂移检测同哲学)
2. **"显式 ID 提取的严格性"**:前缀出现后畸形即错误——**交接文本的严格解析**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v40 | — | 102 | 102 |
| v41 | cli 剩余/autoresearch 验证 | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 session_events 重放细节/telemetry sink/proc——按需收尾。
