# NC-4 本地缓存+故障转移+地址管理 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "FailoverReactor 故障转移反应器 + FailoverDataSource/FailoverData/NamingFailoverData 数据 + FailoverSwitch 开关" 正确 | 实测确认 + **5 秒刷新周期 (L88) + 差异事件通道 (L114) + Micrometer 指标 (L193) 补齐** |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "failover 自动生成" | 实测: **FailoverDataSource 是 SPI 插槽, 默认无实现** — 用户按需注入; 开关由 FailoverSwitchRefresher 5 秒轮询刷新 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | 规划把地址管理归 "client-basic/address" | 实测确认: AbstractServerListManager/EndpointServerListProvider/PropertiesListProvider 全在 client-basic/address ✅ |

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | "ServerListProvider 是简单配置读取" | 实测: order 降序 + match 首个命中的 Provider 族 + Endpoint 30s 定时刷新 + 变更事件联动 NC-3 onServerListChange — 与 1.x 单类 ServerListManager 的架构差异精确化 |

## 审 5: 覆盖率 — 0 缺漏 (故障转移/SPI/地址族/磁盘/配置容灾 6 面全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过
