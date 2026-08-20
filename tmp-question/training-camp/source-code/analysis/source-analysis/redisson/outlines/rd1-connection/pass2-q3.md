# 闭环笔记 q3: connect 重试循环 + eager/lazy 汇聚 + detectCluster 自动发现

## 假设
connect() = retryAttempts+1 次尝试的同步循环 (配置错误不重试, 中断不重试); eager (构造期) 与 lazy (首次调用) 最终都汇聚到 doConnect; 连接成功后还有集群探测。

## 验证过程
- MasterSlaveConnectionManager.java:229-262 (connect): `attempt = retryAttempts+1` (=5); L234-236 `lastAttempt` 标记; L241-243 **IllegalArgumentException → shutdown + 直接抛** (配置错误无重试价值); L250-252 **InterruptedException → 直接抛**; L253-259 其他异常 → `config.getRetryDelay().calcDelay(attempt)` + Thread.sleep (EqualJitter 1-2s)
- 汇聚点 doConnect (L286-331): slaveNotUsed → SingleEntry : MasterSlaveEntry (L288-292); setupMasterEntry → **有界等待** `masterFuture.get(connectTimeout * max(1, masterConnectionMinimumIdleSize))` (L299, 注释: 防 minimumIdleSize==0 时无限 join 卡死 lazyConnect latch); initSlaveBalancer → 同样有界 (L311); startDNSMonitoring; 失败 → **internalShutdown() 清理已建连接** (L322)
- detectCluster (L264-284): EVAL 双 key 脚本 (`test1`/`test2`) → 若报 **CROSSSLOT** → `serviceManager.setClusterDetected(true)` — MasterSlave 模式对集群节点的自动识别 (4.x 特性)
- eager/lazy: ConnectionManager.create 末尾 `if (!configCopy.isLazyInitialization()) cm.connect()` (ConnectionManager.java:107-108) — 默认 lazy=false → 构造期立即连接

## 代码类型
Implementation (重试协议) + Algorithmic (探测算法)

## 跨域关联
- Q2 (lazyConnect) → connect 是其同步内核
- RD-4 (retryAttempts/retryDelay 消费点) → 同一配置
- RD-1 篇2 (连接池) → doConnect 建 Entry

## 结论
connect = 5 次尝试循环, 配置错/中断不重试, 其余按 jitter 退避; doConnect 全程有界等待 (防懒连接 latch 卡死); 成功后 EVAL 探测 CROSSSLOT 自动识别集群。lazy=false 默认 → 构造即连接。
源码位置: MasterSlaveConnectionManager.java:229-331, ConnectionManager.java:107-108
