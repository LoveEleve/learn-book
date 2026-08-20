# RM-12 HA/DLedger+Controller — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **表述精确化** | **"主推从拉"语义**: 连接建立与节奏控制 (报 offset) 在从库, 数据流动是主库 WriteSocketService 主动推 — 混称"拉取"易误; 精确: 从库控制、主库推送 | 大纲 §2 补注 |
| 2 | **补充锚点** | **首连特殊路径**: slaveRequestOffset==0 → 主从最近 **1GB 段起点** (getMappedFileSizeCommitLog 对齐) 全量补同步 (L287-299) — 新从库冷启动语义 | 大纲 §2 补注 |
| 3 | **语义标注** | **offset 强校验 = 对齐检测非进度校验**: 从库校验主推 offset == 自身 maxPhyOffset, 不等即断 — 防错乱 (非防落后); 落后靠报自身 offset 续推补 | 大纲 §2 补注 |
| 4 | **补充锚点** | **AutoSwitch 依赖 RocksDB**: truncateInvalidMsg 用 RocksDB 判定非法消息 (L493+) — 与 RM-3 queue/RocksDB 面交叉; 启用 AutoSwitch 隐含 RocksDB 依赖 | 大纲 §3 补注 |
| 5 | 行号验证 | 全函数 42 锚点 + 跨文件 14 处 grep (DefaultHAService 98-200 / DefaultHAConnection 211-384 / DefaultHAClient 251-370 / AutoSwitchHAService 83-209 / ReplicasInfoManager 193-325 / DefaultBrokerHeartbeatManager 59-90 / ReplicasManager 229-300,378-420,878-880 / ControllerManager 184-195,309-348) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 三水位推进链
- 从库强校验/续推
- epoch 截断裁决
- 选举三要素

### 维度2 性能
- 32KB 批量 + 流控
- 首连 1GB 段起点
- 1MB 读缓冲双缓冲

### 维度3 内存
- 1MB 读缓冲 (byteBufferRead/backup)
- controller 1GB 日志
- NotifyTask map

### 维度4 一致性
- 组提交等确认水位
- epoch 递增守卫 (broker/notify 双)
- fenced 双层
- stateVersion 仲裁 (namesrv)

### 维度5 负面空间 (已写入大纲 7 条)
- 不自动转移 (经典)/不跨机房/不消息级确认/不从读均衡/不 RPO=0/写放大/无 gossip

## 结论
RM-12 全部锚点 ~42 处验证, 6 闭环完成, **表述精确化 1 + 补充锚点 2 + 语义标注 1**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-2/11/5 (已交付) ✅; 引出 RM-13 ✅; 对照 Kafka ISR/LeaderEpoch ✅; 读者处境场景化 ✅; 锚点 ~42 ✅; 负面空间 7 条 ✅; 横切 (并发/一致性/复制协议/选举) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §5 未提 **NotifyService epoch 覆盖**: 旧 epoch 通知被新 epoch **cancel 旧 future + 替换任务** (ControllerManager:322-341) — 通知链本身有 epoch 单调保证 (与 broker 侧 changeBrokerRole 守卫双层) | 大纲 §6 补注 |
| 8 | 通过项 | 其余 ~38 句机制描述逐句对源码一致 ✅ (三水位/拉取状态机/12B 头/首连 1GB/强校验/32KB/epoch 截断/confirmOffset/DLedger 角色回调/选举三要素/fenced 双层/切换链) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (NotifyService epoch 覆盖 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 双水位数学 | masterPutWhere - push2SlaveMaxOffset < 256MB → isSlaveOK; 超差从库踢出组提交 (inSync 判定同阈) ✅ | 通过 |
| V2 | 首连 1GB 对齐 | masterOffset - masterOffset % 1GB → 段起点; 负值钳 0 ✅ | 通过 |
| V3 | 强校验闭环 | 不等 → 断 → 重连报自身 maxPhyOffset → 主从该点续推 — 对齐恢复 ✅ | 通过 |
| V4 | 组提交等待链 | slaveAckOffset ≥ groupCommitRequest 水位 → notifyTransferSome 唤醒 → future.get(5s) ✅ | 通过 |
| V5 | epoch 截断正确性 | truncateSuffixByEpoch(旧主 epoch) — 只截旧主未确认数据; confirmOffset 内数据保留 ✅ | 通过 |
| V6 | 选举策略 | syncStateSet 内 maxOffset 降序 (数据最新) + priority 升序 (人工偏好) — 确定性 ✅ | 通过 |
| V7 | fenced 语义 | makeFenced: setIsolated (broker 逻辑层) + runningFlags (store 层) — 双层拒读写 ✅ | 通过 |
| V8 | 三层防脑裂 | broker epoch 守卫 + namesrv stateVersion 仲裁 + 选举 syncStateSet 内 — 旧主回归三层拦截 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **选举触发方双轨**: Controller 主动触发 (scanInactiveMasterInterval=5s 扫无主) vs broker 侧主动尝试 (ReplicasManager:378 brokerElect, 含 designateElect 强制指定) — 选举不只有心跳超时一条路 | 大纲 §5 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (选举触发双轨), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (NotifyService 覆盖/designateElect/registerCheckCode/从库升主追平/旧主降级数据/流控语义), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | NotifyService 并发通知? | **epoch 单调替换**: 新 epoch 通知 cancel 旧 future + map 覆盖 (ControllerManager:322-341); 3 线程池; 任务完成移除 — 无乱序 | 发现 7 (二次已修) |
| T2 | designateElect? | ElectMasterRequestHeader:49 (默认 false) → ReplicasInfoManager:221 `designateElect ? brokerId : null` — **强制指定选举** (跨 syncStateSet 边界, 需 isBrokerExist) | 发现 10 (补锚) |
| T3 | registerCheckCode? | ReplicasInfoManager:309-325: "brokerAddress;brokerId" 编码 + applyBrokerId 校验 — **防伪造 brokerId 分配** | 发现 11 (补锚) |
| T4 | 从库升主追平细节? | changeToMaster: **handleSlaveSynchronize** (L259) → AutoSwitch changeToMaster (truncate+新 epoch) — 追平后才接管 | 通过 (验证) |
| T5 | 旧主降级数据? | changeToSlave: 主未变 → changeToSlaveWhenMasterNotChange; 变从 → stopCheckSyncStateSet + role=SLAVE + AutoSwitch 同步新主 (epoch 截断) — 旧主数据被新主 epoch 覆盖 | 通过 |
| T6 | 流控语义? | DefaultHAConnection:341-350 canTransferMaxBytes — 主推**字节限速** (非背压) — 与消费流控 (RM-8) 对称 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 选举双轨收敛 | Controller 扫无主 (5s) + broker 主动尝试 (5s 重试) → 同一 electMaster 幂等 — 无双主 ✅ | 通过 |
| V2 | 通知-切换时序 | 通知 (epoch N) → changeBrokerRole (N > masterEpoch) → 切换; 乱序通知被 epoch 拒绝 ✅ | 通过 |
| V3 | 切换数据安全 | 新主 truncate 未确认 → confirmOffset 恢复 → 从库追平 caught-up → 扩 syncStateSet — 数据单调 ✅ | 通过 |
| V4 | 心跳超时双判 | namesrv (2min/5s) + controller (请求头/5s) — 双判不冲突 (不同责任面) ✅ | 通过 |
| V5 | fenced 恢复 | 新主选举后 unsetFenced? (setFenced(false) 对称) — 切换后解封 ✅ | 通过 (标注) |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **designateElect 强制指定选举** (ElectMasterRequestHeader:49 + ReplicasInfoManager:221) — 管理面强制切换通道 | 大纲 §5 补注 |
| 11 | **补充锚点** | **registerCheckCode 防伪** (applyBrokerId: "brokerAddress;brokerId" 编码校验) — 防伪造 brokerId | 大纲 §5 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 双水位 (三线/CAS/256MB) — 可写 ✅
- §2 主从同步 (拉取状态机/12B 头/首连 1GB/强校验/流控) — 可写 ✅
- §3 AutoSwitch (epoch/三截断/confirmOffset/RocksDB) — 可写 ✅
- §4 DLedger (Raft 接入/角色回调) — 可写 ✅
- §5 Controller (双实现/选举/fenced/designate/registerCheckCode) — 可写 ✅
- §6 切换链 (双轨触发/通知覆盖/三层防脑裂) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 2 处全部修复** (designateElect/registerCheckCode)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-14, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 追查六个存疑点 + 反写测试 + 推理验证)

> 动机: 对 outline 全部锚点重新 grep 并追查复制一致性与调度参数六个存疑点 (组提交水位语义/多连接计数缺陷/controller 心跳周期/唤醒链/冷启动边界/fenced 生命周期)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 组提交等哪个水位? | GroupTransferService.doWaitTransfer 三语义: **ackNums<=1 → 等 push2SlaveMaxOffset** (L103-106, 无 in sync 从库退化) / 经典 → 遍历 connectionList 数 slaveAckOffset>=nextOffset (L120-132) / **AutoSwitch ALL_ACK → syncStateSet 内 slaveId 匹配计数** (L111-118) — RM-2 "等确认水位"需按 ackNums 分档 | 发现 12 (表述精确化) |
| T2 | 多连接重复计数? | L127 代码 TODO 注释: "**We must ensure every HAConnection represents a different slave**" — 同从库多连接 (重连残留) 会重复计数 ackNums → 可能假 PUT_OK | 发现 13 (语义标注) |
| T3 | controller 心跳周期? | **brokerHeartbeatInterval=1s** (BrokerConfig:196) + controllerHeartBeatTimeoutMills=**10s** (BrokerConfig:348) → 容 10 次丢失; 速查"2s 初/5s 扫描"是 controller 侧扫描周期, 非心跳周期 (RM-12 速查需修正) | 发现 14 (数字修正) |
| T4 | 唤醒链? | **CommitLog:1310 haService.getWaitNotifyObject().wakeupAll()** — 主库消息追加立即唤醒 HA 写线程推送 (非 100ms 轮询); 无新数据才 100ms 等 | 通过 (验证) |
| T5 | 冷启动边界? | 从库首连请求 0 → 主库 1GB 段起点若已过期删除 → getCommitLogData null → 100ms 等 → 20s housekeeping 断连 → 重连循环 — **主库数据过期删除后新从库无法冷启动** (需主库保留数据或人工) | 发现 15 (语义标注) |
| T6 | fenced 生命周期? | **启动即 fenced** (BrokerController:845 setFenced(true)) → 注册+选举 RUNNING 时 **setFenced(false) 解封** (ReplicasManager:205) — "fenced 初始"实证; 状态机 INITIAL→SYNC_DONE→REGISTER_DONE→RUNNING (5 次注册重试 + 失败 5s 后台循环) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 组提交三语义数学 | ackNums<=1 (无从库) → 主推完即 PUT_OK; ackNums>1 → 等 ack 计数 ≥ 配置; ALL_ACK → syncStateSet 全副本 — 三档收敛 ✅ | 通过 |
| V2 | TODO 缺陷推导 | 同一从库双连接 (断线未清干净) → ackNums 虚高 1 → 可能提前 PUT_OK (真实缺陷面, 代码作者自认) ✅ | 通过 |
| V3 | 心跳余量 | 1s 周期 vs 10s 超时 = 容 10 次丢失; 与 namesrv 2min/10-60s 不同面 ✅ | 通过 |
| V4 | 唤醒闭环 | 写 (wakeupAll) + 组提交通知 (notifyTransferSome) 双唤醒 — 推送无延迟窗口 ✅ | 通过 |
| V5 | 冷启动边界 | 1GB 起点 < minOffset (删除) → 永远 null → 循环断连 — 操作面需保证主库保留窗口 ✅ | 通过 (标注) |
| V6 | 状态机收敛 | 5 次注册重试 (random sleep 防冲突) + 失败 5s 后台重试 — 有界重试+无限兜底 ✅ | 通过 |
| V7 | fenced 对称 | true (启动) → false (RUNNING) → 心跳丢失再 true (超时) — 生命周期闭合 ✅ | 通过 |
| V8 | inSync 与组提交交互 | 256MB 判定 in sync (isInSyncSlave) vs 组提交 ack 计数 — 两套独立判定, 组提交只看 ack 水位 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **表述精确化** | RM-2/12 "组提交等确认水位"需分档: **ackNums<=1 退化等主推水位**; 经典等 ack 计数; AutoSwitch 等 syncStateSet 全副本 | 大纲 §1 补注 |
| 13 | **语义标注** | **多连接重复计数缺陷** (GroupTransferService:127 TODO 自认): 同从库多连接可虚高 ackNums → 假 PUT_OK 风险 | 大纲 §1 补注 |
| 14 | **数字修正** | controller 心跳: brokerHeartbeatInterval=**1s** + controllerHeartBeatTimeoutMills=**10s** (容 10 次); 修正速查 "2s/5s 是扫描周期" | 大纲 §5 + HANDOFF 速查修正 |
| 15 | **语义标注** | **冷启动边界**: 主库数据过期删除后, 首连 0 请求的新从库无法补同步 (1GB 起点已删 → null 循环) | 大纲 §2 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 双水位 (三档组提交/多连接缺陷) — 可写 ✅
- §2 主从同步 (拉取状态机/冷启动边界) — 可写 ✅
- §3 AutoSwitch (epoch/截断/confirmOffset) — 可写 ✅
- §4 DLedger (Raft 接入/角色回调) — 可写 ✅
- §5 Controller (选举/fenced/心跳 1s-10s) — 可写 ✅
- §6 切换链 (状态机/fenced 生命周期) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 8 项全过 (V1-V8); **新发现 4 处全部修复** (组提交三档语义/多连接重复计数缺陷/心跳 1s-10s 修正/冷启动边界)。核心认知: 组提交等待语义按 ackNums 分三档; controller 心跳是独立 1s 周期 (10s 超时容 10 次)。大纲经修复后反写测试全过。
