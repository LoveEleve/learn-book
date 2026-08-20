# 闭环笔记 q7: Config→Manager 5 分支映射 — ConfigSupport 优先级 + 未定义报错

## 假设
ConnectionManager.create 通过 ConfigSupport.getConfig 识别 5 种模式配置; 优先级固定 (MasterSlave 最高); 全部未设置 → IllegalArgumentException。

## 验证过程
- ConnectionManager.java:89-111 (create): `BaseConfig<?> cfg = ConfigSupport.getConfig(configCopy)` → if-else 5 分支:
  - MasterSlaveServersConfig → MasterSlaveConnectionManager
  - SingleServerConfig → SingleConnectionManager
  - SentinelServersConfig → SentinelConnectionManager
  - ClusterServersConfig → ClusterConnectionManager
  - ReplicatedServersConfig → ReplicatedConnectionManager
  - null → `IllegalArgumentException("server(s) address(es) not defined!")` (L104-106)
  - `!lazyInitialization` → `cm.connect()` (L107-108)
- ConfigSupport.getConfig (L844-859): **优先级固定序: MasterSlave → Single → Sentinel → Cluster → Replicated**, 每个取到后先 `validate()` (模式特有校验, 如 SingleServerConfig 校验)
- Config 持有 5 个模式配置字段: useSingleServer()/useMasterSlaveServers()/useSentinelServers()/useClusterServers()/useReplicatedServers() 返回各自配置对象 (方法族)
- Sentinel/Cluster/Replicated 均 extends MasterSlaveConnectionManager (构造链复用 MasterSlave 全部机制, 仅 doConnect 不同: Sentinel 通过哨兵发现 master/slaves L81-208, Cluster 槽位映射 L87-232, Replicated 主从同步 L75-129)

## 代码类型
Interface (工厂) — 纯分支选择 + validate

## 跨域关联
- Q3 (doConnect) → 各子类覆写点
- RD-1 篇1 (Config 全局默认) → 模式配置的宿主
- 5 模式对照 (面试点): 单机/主从/哨兵/集群/复制

## 结论
模式选择 = ConfigSupport.getConfig 固定优先级 (MasterSlave 最优先) + validate 校验 → ConnectionManager.create 5 分支 if-else → 3 个子类只覆写 doConnect (发现逻辑), 连接池机制全复用 MasterSlaveConnectionManager。
源码位置: ConnectionManager.java:89-111, ConfigSupport.java:844-859
