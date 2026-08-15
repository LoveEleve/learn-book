# Reasonix 域发现 v34 补充(续扫第二十轮:plugin oauth/session_events/branch)— 2026-08-14

> 承接:v33。本轮:plugin/oauth(743)/agent/session_events(886)/branch(642)。
> 结论:MCP OAuth 完整实现与事件日志重放上限确认,深化 ③/④;无新域。

---

## 一、v34 深化确认

### plugin/oauth(MCP OAuth)

| 设计 | 位置 | 要点 |
|------|------|------|
| **MCP OAuth 完整实现** | oauth.go:37-311 | **受保护资源发现**(discoverProtectedResource)/授权服务器元数据(RFC 8414)/**动态客户端注册**/令牌响应/刷新(authorizationHeader forceRefresh)/canRefresh |
| **HTTP MCP 授权** | :95-236 | AuthorizeHTTPMCP(openURL 回调)/ReconcileHTTPMCPOAuthAfterRemoval(资源移除后协调) |

### session_events(事件日志侧车)

| 设计 | 要点 |
|------|------|
| **重放上限保护** | SessionReplayLimitError + sessionReplayLimits(日志过大拒绝重放)——**侧车日志有界** |
| **侧车布局** | SessionEventLogPath/IndexPath/logSize/oversized 判定 |

### branch(分支元数据)

| 设计 | 要点 |
|------|------|
| **分支元数据持久化** | BranchMeta(默认作用域)/InFlightTurnMeta;**显示字段消毒**(sanitizeDisplayFields/StoredDisplayText——防存储注入) |

---

## 二、关键设计(通用价值)

1. **"MCP OAuth 的 RFC 合规"**:受保护资源发现+动态注册+刷新——**OAuth 2.1 家族完整实现**(MCP 规范第 7 章)
2. **"重放上限"**:事件日志过大 → 拒绝重放(显式错误)——**有界日志**(与 Hermes 日志上限、Pi 截断同哲学)
3. **"显示字段消毒"**:存储的显示文本消毒——**防存储型注入**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v33 | — | 102 | 102 |
| v34 | plugin oauth/session_events/branch | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 compact/branch-summary 细节、remote/sftpfs、appidentity——按需收尾。
