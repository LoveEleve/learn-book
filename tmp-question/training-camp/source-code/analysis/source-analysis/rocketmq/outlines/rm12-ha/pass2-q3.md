# 闭环笔记 q3: 5.x AutoSwitchHA — epoch 文件 + 截断恢复

## 假设
切换数据裁决 = epoch 区间 + 截断未确认数据。

## 验证过程
- **EpochFileCache** (327 行): 持久化 (epoch, startOffset) 序列 — 每个 epoch 的数据合法起点; storePathEpochFile 文件
- **AutoSwitchHAService extends DefaultHAService** (577 行):
  - init: epochCache 加载 (L83-84); 从库角色创建 AutoSwitchHAClient (L174)
  - **changeToMaster** (L199-209): 追加新 epoch 条目 (truncateSuffixByEpoch 先清后加)
  - **truncateInvalidMsg** (L128, L493+, **RocksDB**): 截断主传来的不完整/非法消息 — 从库侧数据净化
  - **truncateSuffixByEpoch** (L138-141): 升主时截断旧主 epoch 之后 (未确认) 的数据
  - truncateEpochFilePrefix (L484): 文件清理前缀
  - confirmOffset (L307-318): 从库追到 confirmOffset 内当前 leader epoch → caught-up (同步完成判定)
- **confirmOffset 语义**: storeCheckpoint 持久化的复制确认水位 (DefaultMessageStore:386 setConfirmOffset) — 新主从该点恢复; 旧主 epoch 数据 ≤ confirmOffset 才可能被新主保留
- **AutoSwitchHAConnection 协议扩展** (744 行): HANDSHAKE_HEADER_SIZE=**20B** (4+4+8+4: version/slaveId/epoch?) + TRANSFER_HEADER_SIZE=**28B** + EPOCH_ENTRY_SIZE=**12B** — 握手带角色身份与 epoch
- **AutoSwitchHAClient** (598 行): 从库侧握手 + epoch 缓存同步 + caught-up 上报

## 代码类型
Implementation (epoch 区间裁决 + 截断)

## 跨域关联
- RM-11 (路由): acting master 面 (从库代主) 与 AutoSwitch 数据面协同
- RM-10 (事务): enableSlaveActingMaster EscapeBridge (从库代主写转发)
- RM-12 q5: Controller 切换后 AutoSwitch 追平

## 结论
AutoSwitchHA = epoch 文件 (数据合法区间) + 三截断 (无效消息/旧 epoch 后缀/前缀清理) + confirmOffset 恢复点 + RocksDB 辅助; 协议头 20/28/12B 扩展。
源码位置: AutoSwitchHAService.java:83-84,128-141,199-209,493+; AutoSwitchHAConnection.java:56-72; DefaultMessageStore.java:386
