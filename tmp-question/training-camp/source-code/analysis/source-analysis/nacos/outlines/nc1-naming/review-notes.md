# NC-1 NamingService 注册发现 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "registerInstance+getAllInstances 多重载" 未提 subscribe 重载面 | 大纲补: subscribe/unsubscribe 同样 4-6 重载收敛 (L454-481) |
| 2 | 规划 "NamingGrpcClientProxy (onEvent(ServerListChangeEvent) → rpcClient.onServerListChange())" 正确 | 大纲 L139-146 确认 ✅ |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿断言 "ephemeral→gRPC / persistent→HTTP" | 实测: **持久实例在服务端支持 gRPC 时也走 gRPC** (isEphemeral() \|\| isAbilitySupportedByServer) — 只有无 gRPC 能力的老服务端才回退 HTTP — harness 模拟修正 + 大纲重写 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | 规划把 InstancesDiffer 归 naming/core | 实测在 **naming/cache/** (ServiceInfoHolder 同包) — 路径修正 |

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | "getAllInstances 直接查服务端" | 实测默认走订阅缓存 (getServiceInfo → serviceInfoHolder), 非订阅才 queryInstancesOfService 直查 — 与 ALI-A3 selectInstances 的服务端过滤语义对照精确化 |

## 审 5: 覆盖率 — 0 缺漏 (门面/路由/订阅/缓存/三路/通知/保护 8 面全覆盖)

## 审 6: 跨层一致性 — 0 (harness 18/18)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| 代理路由 3 断言失败 | harness 模拟了错误的"ephemeral↔persistent 二选一"语义 — 源码是 ephemeral\|\|ability 短路 → 修正 harness + 大纲 (能力路由精确化) |
| modified 判定初稿用 weight 单键 | 源码是 instance.toString() 全字段比较 — harness 简化但标注语义 |

## 结论: 5 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过
