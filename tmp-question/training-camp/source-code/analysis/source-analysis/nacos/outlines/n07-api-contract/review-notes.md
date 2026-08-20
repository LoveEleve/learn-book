# N-07 API 契约面 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B, 3 篇大纲域级)

## 审 1: 事实错误 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划未提 api 契约面独立成域 (274 文件 22,889 行) | 域发现 N-07 新增: 3 篇大纲 (naming/config/remote 契约) |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "fuzzyWatch 是 ConfigService 方法" | 实测: 接口定义在 ConfigService (L)，实现走 worker.addTenantFuzzyWatcher — 契约/实现面精确化 |

## 审 3: 文件名/目录名推断 — 0

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | "AbstractSharedListener 是客户端类" | 实测: **契约在 api/config/listener** (客户端 client/config 是其实现/使用面) — 归属修正 |

## 审 5: 覆盖率 — 0 缺漏 (接口族/pojo/监听/注解/协议 5 面全覆盖, 3 篇)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 3 项修正, 全部落盘大纲; 锚点重 grep 验证通过
