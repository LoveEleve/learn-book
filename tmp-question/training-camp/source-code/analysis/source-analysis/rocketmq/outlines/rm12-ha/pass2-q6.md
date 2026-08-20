# 闭环笔记 q6: 切换全链 — 心跳超时 → 选举 → 角色通知 → 重注册

## 假设
故障转移是"管控面选举 + 数据面追平 + 路由面更新"的完整链。

## 验证过程
- **触发面双轨**:
  1. namesrv 心跳超时 (RM-11: 5s 扫描/2min 超时) → 注销 → minId 变化通知 (905) → acting master (默认关)
  2. Controller 心跳超时 (本域: 5s 扫描/请求头超时) → isBrokerActive=false → 选举
- **选举**: ReplicasInfoManager.electMaster (syncStateSet 内, maxOffset+priority, 3 次重试) → apply 日志 (raft 多数派) → 结果广播
- **通知链**: ControllerManager.NotifyService (L309-348, notifyBrokerRoleChanged) → broker ReplicasManager.changeBrokerRole (epoch 守卫 L229-233) → changeToMaster/changeToSlave
- **数据面**:
  - 新主 (从升主): handleSlaveSynchronize 追平 (与旧主数据对齐) → haService.changeToMaster (AutoSwitch: truncateSuffixByEpoch + 追加新 epoch) → 从 confirmOffset 恢复
  - 从库: AutoSwitchHAClient 同步 epoch → caught-up 判定 → syncStateSet 扩员 (ALTER_SYNC_STATE_SET)
- **路由面**: registerBrokerWhenRoleChange (brokerId=0 换主注册) → namesrv 更新 → 客户端 10-60s 周期感知 (RM-5); namesrv acting master 擦权/伪装 (RM-11, 默认关)
- **旧主回归**: stateVersion 仲裁拒绝 (RM-11) + epoch 守卫拒绝旧通知 — 双保险防脑裂

## 代码类型
Architecture (故障转移编排)

## 跨域关联
- RM-11 (路由): 905/1003 + acting master + stateVersion
- RM-5 (Broker): registerBrokerAll 周期
- RM-2 (存储): 刷盘/复制双水位在切换中的角色

## 结论
切换 = 心跳超时 (双轨) → 选举 (3 次重试) → epoch 守卫角色通知 → 数据追平/截断 → 重注册 → 路由感知; 旧主回归双保险 (stateVersion + epoch)。
源码位置: ControllerManager.java:184-195,309-348; ReplicasManager.java:229-233,237-300,378-420,878-880; AutoSwitchHAService.java:128-141,199-209
