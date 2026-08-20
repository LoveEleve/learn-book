# NC-5 一致性协议 Distro + SOFA-JRaft — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "DistroProtocol + DistroDataProcessor + DistroDataStorage + DistroCallback — 最终一致, 各节点负责部分数据, 主动同步" | 实测确认 + 补: **任务链三件套 (LoadDataTask/VerifyTimedTask/ExecuteTask) + resourceType 组件注册面 (L164-173)** |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划 "NacosClosure/JSnapshotOperation/NoLeaderException" 正确 | 实测确认 + 补 **FailoverClosureImpl/NacosRead/WriteRequestProcessor/AbstractProcessor** (规划未列全) |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | 规划把 Distro 归 "core/distributed/distro" | 实测确认 + **DistroDelayTask/DistroVerifyTimedTask 的任务子包细化 (task/)** |

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | 初稿假设 "JRaft 是 Nacos 自研实现" | 实测: **SOFA-JRaft 框架集成** (JRaftServer 封装 node/group/cli, 注释架构图 L60-90) — 对照 SofaJRaft 4.6 域精确化 |

## 审 5: 覆盖率 — 0 缺漏 (统一契约/Distro 组件/JRaft 集成/状态机/读写分派 6 面全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过
