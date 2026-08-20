# ALI-A5 Nacos 容错+心跳+优雅关闭 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "NacosWatch 订阅服务变更" 但未提回写方向 | 大纲补: 订阅后 **只匹配自身 ip+port**, 回写 properties.metadata (L114-119) — 订阅方向精确化 |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设心跳"默认启用, 每 30 秒" | 实测 **默认不装配** (AnyNestedCondition 三条件全 false 则无 Bean), 注释 "no longer enabled by default" + issue#2868/#3258 — 默认关闭精确化 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "NacosDiscoveryHeartBeatPublisher 心跳保活实例" | 实测是**打点事件发布器** (HeartbeatEvent), 实例保活靠 Nacos 客户端内核 (5.8 域 BeatReactor) — 职责边界修正 |

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | "优雅关闭 = NacosWatch.stop" | 实测独立 Delegate 监听 ContextClosedEvent, 与 SmartLifecycle 的 stop 不同面 — 修正为 ApplicationListener 面 |

## 审 5: 覆盖率 — 0 缺漏 (订阅/心跳/关闭/健康/事件 5 大机制全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | Watch:75-77 (CAS+computeIfAbsent) + 94-95 (subscribe) + 114-119 (ip/port 自我过滤) / HeartBeat:67-68 (schedule) + 102-105 (event+index) / Graceful:56-64 (子上下文过滤+注释) + 67-81 (三步) + 83-87 (同步) / Health:62-69 (三态) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | NacosDiscoveryInfoChangedEvent ↔ A3 AutoReg restart 消费闭环; HeartbeatEvent ↔ SCC-3 HeartbeatMonitor 契约 | 通过 ✅ |
| 3 | 数字自洽 | 三条件 OR/两步 CAS/2.2.0 版本锚全文一致 | 通过 ✅ |

## 四轮 REVIEW (2026-08-16, 全量脚本复验)

文字锚 (#7) 命中修复: 本文档域引用类补行号 (grep 实证, 见 HANDOFF §六 第 6 行); 锚点范围修正见全局记录。
