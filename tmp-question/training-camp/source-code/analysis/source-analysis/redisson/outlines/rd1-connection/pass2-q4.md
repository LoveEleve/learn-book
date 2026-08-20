# 闭环笔记 q4: DNSMonitor 故障切换协议 — 多轮确认 + 成功才更新

## 假设
DNSMonitor 用 ServiceManager.newTimeout 周期自循环调度; 每轮对 master/slaves 各做 DNS 解析; 变化需多轮确认 (防抖动); changeMaster 成功后才更新本地缓存。

## 验证过程
- DNSMonitor.java:53-64 (构造): 对 masterHost + slaveHosts 先 `resolveAddr().join()` 建立初始 maps (RedisURI→InetSocketAddress)
- L66-69 (start) → L77-88 (monitorDnsChange): `newTimeout(task, dnsMonitoringInterval)` → 任务内 monitorMasters + monitorSlaves → **allOf.whenComplete → 递归调度下一轮** (自循环, 天然防重叠)
- **resolveTimes (L268-283)**: `resolveAll` 解析 → 若含当前地址 → true (未变); 否则 `times+1 < dnsMonitoringTimes` → 再解析 (最多 dnsMonitoringTimes 次确认) → false = 判定变化 — **多轮确认防 DNS 抖动**
- monitorMasters (L90-170): 解析失败/空 → 跳过; **多 IP 警告 "Use Redisson PRO with Proxy mode"** (L110-116, 4.x 只取一个); 排序取第一个; 变化 → `masterSlaveEntry.changeMaster(newMasterAddr, entry.getKey())` (L152) → **成功才更新 masters map** (L157)
- monitorSlaves (L172-266): 变化 → 找含旧 slave 的 entry → hasSlave(new)? slaveUpAsync : addSlave → 成功后 slaves 更新 + `slaveDown(old)` (L225-249)
- changeMaster 协议 (MasterSlaveEntry:500-545): setupMasterEntry(新) → 成功 → 从 slaves 移除同地址项 (L527-534) + removeMaster(old) + `useMasterAsSlave()` (旧 master 降级为 slave, L540-543); 失败 → **回滚恢复旧 master** (L515-518)
- stop (L71-75): 取消 Timeout

## 代码类型
Implementation (监控协议) — 高价值: 自循环调度 + 多轮确认 + 成功才提交

## 跨域关联
- RD-1 篇1 (ServiceManager.newTimeout L297) → 调度宿主
- RD-1 篇2 (MasterSlaveEntry changeMaster) → 切换执行者
- CHANGELOG 时空: dnsMonitoringTimes 设置新增 (4.x), "RTopic 重订阅 DNS 变化回归 3.27.0" — 该机制有真实线上事故史

## 结论
DNS 故障切换 = 周期轮询 (自循环不重叠) → 多轮确认 (dnsMonitoringTimes 防抖动) → changeMaster/changeSlave → **成功才更新本地镜像**; 主切换后旧 master 自动降级为 slave (useMasterAsSlave), 失败回滚。多 IP 需 Pro 版 (警告明确)。
源码位置: DNSMonitor.java:53-283, MasterSlaveEntry.java:500-545
