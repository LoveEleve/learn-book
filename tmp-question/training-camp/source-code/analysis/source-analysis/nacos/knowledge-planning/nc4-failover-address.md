# NC-4 本地缓存+故障转移+地址管理 — 知识规划 (KP)

> 🟡 B | 模块: client/naming/backups + client/naming/cache + client-basic/address + client/config/impl | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 5 秒开关轮询 | FailoverReactor:87-89 | FailoverSwitchRefresher 定时刷新 |
| 2 | 三态切换 | FailoverReactor:91-153 | null→关 / 开→加载+差异事件 / 关→回主缓存 |
| 3 | SPI 数据源 | FailoverReactor:72-77 | FailoverDataSource 首个实现 |
| 4 | 开关判定 | FailoverReactor:159-161 | 开关 && 有数据 && ipCount>0 |
| 5 | 地址 Provider 族 | AbstractServerListManager:50-58 | order 降序 + match 首个 |
| 6 | Endpoint 刷新 | EndpointServerListProvider:163-172 | 30s 定时 refreshServerListIfNeed |
| 7 | 磁盘写 | DiskCache:54-76 | ConcurrentDiskUtil 落盘 |
| 8 | 磁盘读 | DiskCache:90-105 | 目录解析回 Map |
| 9 | 配置容灾 | LocalConfigInfoProcessor:68/85 | failover/snapshot 文件 |
| 10 | 可观测 | FailoverReactor:187-207 | failover 实例数指标 |

## 02 高频坑

1. FailoverDataSource 默认无实现 — 用户 SPI 注入才生效
2. failover 开关 5 秒才刷新一次
3. 从 failover 切回时对比主缓存发差异事件
4. Endpoint 30 秒刷新间隔 (refreshServerListInternal)
5. 配置 failover 文件路径三级: data/config-data/config-data-tenant
6. 命名 failover 由用户维护 (与配置一致)
7. ServerListChangeEvent 联动 RpcClient.onServerListChange

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 容灾 | 5 秒开关 / 三态切换 / SPI 数据源 / 差异事件同通道 |
| 地址 | Provider 族排序匹配 / Endpoint 定时 / Properties 静态 |
| 缓存 | DiskCache 读写 / ServiceInfoHolder 联动 / 启动加载 |
| 配置容灾 | failover(用户) / snapshot(自动) / 文件路径结构 |
| 可观测 | Micrometer 指标 / 日志锚 |
| 演进 | 1.x ServerListManager → Provider 族 |

## 04 跨域桥接

- ← NC-1: isFailoverSwitch/getService 消费 (发现三路第一路)
- ← NC-2: LocalConfigInfoProcessor 三路消费
- → NC-3: ServerListChangeEvent → onServerListChange
- → 面试: "Nacos 客户端容灾" — failover 开关 + 磁盘缓存 + 地址 Provider
