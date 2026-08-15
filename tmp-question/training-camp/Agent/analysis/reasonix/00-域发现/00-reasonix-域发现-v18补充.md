# Reasonix 域发现 v18 补充(续扫第四轮:acp 服务/history 检索)— 2026-08-14

> 承接:v17。本轮:internal/acp/service.go(3,057)完整服务 + internal/history 检索层。
> 结论:acp 会话生命周期与 history 路径安全确认,深化 ③/④;无新域。

---

## 一、v18 深化确认

### acp 服务(3,057 — 最大未开文件)

| 设计 | 位置 | 要点 |
|------|------|------|
| **会话生命周期** | service.go:327-409 | begin(**TryLock 非阻塞准入**——ACP 准入不阻塞,同时关闭闲置检查窗口)/finish/abort/**abortAndWait/deleteAndWait**(等待 done+maintenance 双通道);**待决配置阻塞新回合**(prompt 绝不跑在过期配置上,维护 defer 应用) |
| **afterResponse 钩子** | :182-189 | 响应写后回调(传输清理) |
| **Factory/Rebuilder 抽象** | :68-113 | Factory(会话工厂)/SessionRebuilder(会话重建) |

### history 检索

| 设计 | 位置 | 要点 |
|------|------|------|
| **Around 上下文窗口** | search.go:225-263 | 前后窗口 clamp(默认/上限)/路径白名单 allowedPath/**visiblePath 清理中拒绝**/索引越界明确错误 |
| **Searcher 抽象** | :62-77 | Search(全文)/Around(窗口)/scope 规范化/kind 规范化 |

---

## 二、关键设计(通用价值)

1. **"TryLock 非阻塞准入"**:ACP 准入不阻塞(闭窗口与准入竞争时放弃)——**非阻塞准入的取舍**(比阻塞更优:宁可拒一次也不卡)
2. **"待决配置阻塞新回合"**:prompt 绝不跑在过期配置上——**配置变更的正确性**(与 Pi settings 修改追踪同思想)
3. **"清理中路径拒绝"**:visiblePath(待清理)→ 拒绝检索——**生命周期正确性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v17 | — | 102 | 102 |
| v18 | acp 服务/history 检索 | +0(深化 4 设计) | **102**(深化) |

> 继续:next 轮 config 细节(load 2,498/edit 2,442)、capdiag/capability(能力诊断)、doctor(诊断)。
