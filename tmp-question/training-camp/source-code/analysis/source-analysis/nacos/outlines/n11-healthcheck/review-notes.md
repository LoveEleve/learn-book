# N-11 健康检查 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 执行计划 NC-5 "TcpSuperSenseProcessor" | 09 审计确认不存在 — 3.x 是 HealthCheckTaskV2 + HealthCheckProcessorV2 族 (四实现) |
| 2 | 规划未提处理器族与拦截器链 | N-11 全展开: Delegate 注册表 + 四拦截器 + 心跳三维检查器 |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "健康检查只有 TCP" | 实测: **Tcp/Http/Mysql/None 四处理器 + SPI 扩展 (HealthCheckExtendProvider)** — 处理器族精确化 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "心跳与健康检查是一回事" | 实测: **heartbeat/ 子包 (BeatCheckTask/ClientBeatProcessorV2) 与 v2/processor 检查族分离** — 心跳 vs 主动检查双面 |

## 审 4: 跨项目概念转移 — 0

## 审 5: 覆盖率 — 0 缺漏 (任务模型/处理器族/拦截器链/心跳三维/状态同步 6 面全覆盖)

## 审 6: 跨层一致性 — 0 (harness 待建)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过
