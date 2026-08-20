# N-10 客户端管理面 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划 (issue) 未提 3.x clientId 客户端模型 | 域发现 N-10 新增: ClientService/ClientManager/Client 三族 (v2/core 53 文件) |
| 2 | NC-6 大纲称 "ClientServiceImpl 客户端生命周期管理" 但未展开 | N-10 全深度展开: 三管理器/工厂族/同步数据/清理器 |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "release 直接从 manager 删" | 实测: AbstractClient.release 有自身清理逻辑 (L182), manager.remove + client.release 双动作 — harness 两段实证 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "ClientServiceImpl 在 core 顶层" | 实测: 接口在 core/, 客户端模型在 **core/v2/client/** (manager/factory/impl 四层) — 分层精确化 |

## 审 4: 跨项目概念转移 — 0

## 审 5: 覆盖率 — 0 缺漏 (门面/三管理器/模型/工厂/操作/横切 7 面全覆盖)

## 审 6: 跨层一致性 — 0 (harness 12/12)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| detail 断言用 dup | putIfAbsent 首客户端胜出 — 修正断言验证 first-wins 语义 |

## 结论: 4 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过
