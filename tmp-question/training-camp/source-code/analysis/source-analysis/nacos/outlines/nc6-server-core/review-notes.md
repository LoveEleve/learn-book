# NC-6 服务端核心 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "NamingApp 启动类" 正确 | 实测确认 + scanBasePackages (naming+core) + @EnableScheduling 补齐 |
| 2 | 执行计划 5.8 NC-5 "TcpSuperSenseProcessor" | 3.x healthcheck 重构: **HealthCheckReactor + BeatCheckTask (5s) + v2/HealthCheckTaskV2** — TcpSuperSenseProcessor 已不存在 (1.x 语义) |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划 "InstanceController/ServiceController/ClusterController/HealthController/CatalogController" 正确 | 实测确认 + **v2/v3 控制器变体 + Operator 层 (InstanceOperatorClientImpl) 补齐** |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "push/ 推送面" | 实测: **UdpPushService (1.x 遗留) + v2/ (gRPC) + NamingSubscriberService 本地/聚合双实现** — 双通道精确化 |

## 审 4: 跨项目概念转移 — 0

## 审 5: 覆盖率 — 0 缺漏 (入口/健康/推送/一致性落点/配置存储 6 面全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过
