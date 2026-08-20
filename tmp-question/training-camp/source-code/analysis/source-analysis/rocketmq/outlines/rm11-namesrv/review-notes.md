# RM-11 Namesrv 路由 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | PLAN v3 "RouteInfoManager (BrokerData/QueueData/TopicRouteData 三表)" — **实际六表**: +filterServerTable (FilterServer 遗留, RM-6 废弃面) +topicQueueMappingInfoTable (5.x 静态 topic); 三表是**协议结构** (返回给客户端), 存储面是六表 | 大纲 §1 补注 |
| 2 | **表述精确化** | **45s 就绪门禁默认关闭**: needWaitForService=false (NamesrvConfig:81) — "启动 45s 内拒答"仅在开启时成立; 且门禁是**时间条件 + 命中 disable 双保险** | 大纲 §4 补注 |
| 3 | **补充锚点** | **心跳三来源**: BROKER_HEARTBEAT=904 (轻量) / QUERY_DATA_VERSION=322 (比对兼心跳) / 注册本身 upsert — 不是单一心跳请求 | 大纲 §5 补注 |
| 4 | **补充锚点** | **acting master 全链四步**: 注册时 isPrimeSlave 擦写权限 (L343-347) / 注销时无主擦写 (L676-681) / 查询时最小 brokerId 伪装 MASTER_ID (L787-789) / minId 变化 oneway 通知 (L934) — 从库代主路由完整闭环 | 大纲 §6 补注 |
| 5 | 行号验证 | 全函数 38 锚点 + 跨文件 10 处 grep (RouteInfoManager 70-801/803-818/820-953 / DefaultRequestProcessor 223-395 / ClientRequestProcessor 65-108 / BatchUnregistrationService 61-76 / BrokerHousekeepingService 36-48 / NamesrvController 116-215 / ZoneRouteRPCHook 43-95) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 六表级联一致性 (注册/注销)
- 主从同组地址组织
- 查询快照完整性

### 维度2 性能
- RWLock 读写分离
- 查询独立线程池 (8/50000)
- 批注销 drainTo 合并

### 维度3 内存
- 六表初始容量 (1024/128/32/256/256/1024)
- KV 配置单文件

### 维度4 一致性
- stateVersion 仲裁
- 地址去重 (同 IP:PORT)
- 心跳超时双通道

### 维度5 负面空间 (已写入大纲 7 条)
- 不持久化路由/不共识/不推送/不探活/不存数据/zone 后置/acting master 默认关

## 结论
RM-11 全部锚点 ~38 处验证, 6 闭环完成, **认知修正 1 + 表述精确化 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-5/7/1 (已交付) ✅; 引出 RM-12 ✅; 对照 Kafka KRaft ✅; 读者处境场景化 ✅; 锚点 ~38 ✅; 负面空间 7 条 ✅; 横切 (并发/一致性/注册中心模式/心跳) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §5 未提 **扫描无锁性**: scanNotActiveBroker 直接遍历 brokerLiveTable (ConcurrentHashMap) 无显式锁 — 弱一致迭代 (注册并发时可能漏扫一次, 下轮补扫); 另 **2min 超时 vs broker 10-60s 心跳周期余量**: 最短 10s 心跳 → 容 12 次丢失, 60s → 容 2 次 | 大纲 §5 补注 |
| 8 | 通过项 | 其余 ~35 句机制描述逐句对源码一致 ✅ (六表结构/注册仲裁链/批注销/级联清理/就绪门禁/版本 JSON/zone 过滤/Controller 内嵌/擦权链) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (扫描无锁 + 心跳余量 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 六表关联闭环 | 查询路径: topicQueueTable 得 brokerName 集 → brokerAddrTable 得 addr → brokerLiveTable 得存活 — 关联完整 ✅ | 通过 |
| V2 | 注册幂等数学 | registerFirst 或 DataVersion 变化才全量 topic 更新 — 10-60s 周期注册不重复写 ✅ | 通过 |
| V3 | stateVersion 仲裁 | old > new 拒绝 — 僵尸恢复的 stateVersion 必然落后于现主 (Controller 递增) ✅ | 通过 |
| V4 | 地址去重 | 同 IP:PORT 只保留一条 — 主从切换后旧主地址被新主注册顶掉 (switch 语义) ✅ | 通过 |
| V5 | prime slave 判定 | 非旧版 && 非主 && brokerId==min(brokerIds) && enableActingMaster → 擦写权限 — 从库代主前提齐全 ✅ | 通过 |
| V6 | drainTo 合并数学 | take(1) + drainTo(N) + HashSet 去重 → 单次批量处理 N+1 条; 队列满 (3000) → submit 失败 → 显式注销报错 ✅ | 通过 |
| V7 | 心跳判定 | lastUpdate + timeout < now → closeChannel + 注销; 10s 心跳 << 2min 超时 → 容 12 次丢失不误踢 ✅ | 通过 |
| V8 | 查询 clone 防篡改 | BrokerData clone (深拷贝) 返回 — 外部修改不影响路由表; QueueData 直接引用 (只读语义) ⚠ 标注 | 通过 (标注) |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **表述精确化** | **就绪门禁失效双路径**: 45s 时间条件自然失效 (time 判定短路) + 路由命中 disable — 即使所有查询都 TOPIC_NOT_EXIST, 门禁也会在 45s 后自然放行, 不会永久拒绝; 原大纲"命中后 disable"单一路径表述不全 | 大纲 §4 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (门禁失效双路径), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (锁内网络 I/O/写权限管理面/批注销满/ZONE 过滤语义/门禁时效/acting 伪装安全), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | notifyMinBrokerIdChanged 在哪调用? | **注册 (L398-401) 与注销 (L642-644) 均在 writeLock 临界区内** 调用; 实现为 **invokeOneway (300ms 超时)** (L934) — 锁内网络 I/O (oneway 缓解阻塞, 但注册写路径仍可被拖慢) | 发现 10 (语义标注) |
| T2 | WIPE/ADD 写权限管理面? | operateWritePermOfBroker (L532-555): 全表扫 topicQueueTable 逐 QueueData 改 perm; WIPE 只清 WRITE 位, **ADD 强制置 READ\|WRITE** | 发现 11 (补锚) |
| T3 | 批注销队列满行为? | submit 失败 → 显式注销返回 SYSTEM_ERROR (DefaultRequestProcessor:373-378); **通道事件注销失败仅 log** (RouteInfoManager:836-840) → 不重试 → 依赖 5s 扫描兜底 (终态不丢) | 发现 12 (语义标注) |
| T4 | ZONE 过滤语义? | master down (无 MASTER_ID) → **保留该 broker 全部从库** (ZoneRouteRPCHook:70-71 "break nearby route rule") — 可用性优先于就近 | 通过 (验证) |
| T5 | 门禁时效? | 45s 时间条件短路 + 命中 disable 双路径 (三次 REVIEW #9 已修) ✅ | 通过 |
| T6 | acting 伪装安全? | needActingMaster 前置判定无主 (L763-771) → remove(minId) 后 put(MASTER_ID) 无冲突; 仅作用于返回快照的 brokerAddrs (clone 内) — 不动原表 ✅ | 通过 |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 锁内通知影响面 | 仅 enableActingMaster + notifyMinBrokerIdChanged 双开时触发 — 默认双 false → 默认路径无锁内 I/O ✅ | 通过 |
| V2 | 批注销兜底闭环 | 队列满/失败 → 5s 扫描补位 → 最终一致 (最多延迟一个扫描周期) ✅ | 通过 |
| V3 | 写权限擦除链 | 注册 (prime slave) / 注销 (无主) / 管理 (WIPE) 三入口收敛到 QueueData.perm — 权限状态一致 ✅ | 通过 |
| V4 | 主从切换闭环 | 主挂 → 心跳超时/通道事件 → 注销 → minId 变化 → oneway 通知 → acting master 接管 (RM-12 交叉) ✅ | 通过 |
| V5 | 查询门禁安全 | 45s 后无条件放行 + 命中提前放行 — 无永久拒绝路径 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 语义标注 | **notifyMinBrokerIdChanged 在写锁内发起网络通知** (invokeOneway 300ms): 双配置开启时注册/注销写路径可被网络拖慢 (oneway 缓解) | 大纲 §6 补注 |
| 11 | 补充锚点 | **写权限管理面**: WIPE=205 / ADD=206 (全表扫; ADD 强制 READ\|WRITE) — 管理命令与 acting master 共用 perm 状态 | 大纲 §6 补注 |
| 12 | 语义标注 | **批注销满/失败面**: 显式注销报错, 通道事件注销静默 → 5s 扫描兜底 (终态不丢) | 大纲 §3 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 六表结构 (含 5.x 两张表) — 可写 ✅
- §2 注册 (crc32/版本/仲裁链/擦权) — 可写 ✅
- §3 注销 (三入口/批量/级联/兜底) — 可写 ✅
- §4 查询 (线程池/门禁/快照/三后置加工) — 可写 ✅
- §5 心跳 (三来源/5s 扫描/双通道/余量) — 可写 ✅
- §6 5.x 新面 (Controller 内嵌/acting 全链/zone/批注销) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (锁内网络通知/写权限管理面/批注销兜底)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-14, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 追查六个存疑点 + 反写测试 + 推理验证)

> 动机: 对 outline 全部锚点重新 grep 并追查并发一致性六个存疑点 (时间戳可见性/锁使用一致性/权限重建覆盖/冲突静默路径/批注销双触发/clusterTest)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 心跳时间戳并发安全? | **BrokerLiveInfo.lastUpdateTimestamp 普通 long 非 volatile** (L1181) + updateBrokerInfoUpdateTimestamp **无锁** (L469-475) — 心跳线程 (Netty) 写 vs scanNotActiveBroker 线程 (5s) 读: 无 happens-before 保证 — 扫描可能短暂看到旧值; 64 位 long 原子 + 5s 周期 + Netty 事件 HB 缓解, 实际误踢窗口极小 | 发现 13 (语义标注) |
| T2 | WIPE/ADD 权限生命周期? | createAndUpdateQueueData (L477-501) 在 DataVersion 变化时用 **topicConfig.getPerm() 重建 QueueData** — **WIPE/ADD 是一次性临时覆盖, 下一次配置变更即还原** (prime slave 擦权每注册重擦, 幂等不受影响) | 发现 14 (语义标注) |
| T3 | stateVersion 冲突响应? | L288-290: 拒绝后 `return result` — **空 RegisterBrokerResult 非 null** → DefaultRequestProcessor:263 null 判定不命中 → **broker 收到 SUCCESS 但 haServerAddr/masterAddr 为空** — 冲突方静默, 无感知, 靠下轮注册 (DataVersion 追平) 自愈 | 发现 15 (语义标注) |
| T4 | 锁使用一致性? | 读路径**部分加锁**: getAllTopicList/getAllClusterInfo 有 readLock; **queryBrokerTopicConfig (L460-467) 无锁** (纯 CHM get) — 注册写锁内直接调用 (无嵌套问题); 一致性靠 ConcurrentHashMap 原子性, 弱一致可接受 | 发现 16 (补锚) |
| T5 | 批注销双触发合并? | 通道事件 + 超时扫描可能对同一 broker 双触发 → 队列内 Set 去重 (L66-69) + unRegisterBroker 内 removeIf 幂等 — 合并安全 ✅ | 通过 |
| T6 | clusterTest 模式? | NamesrvController:205-207 — clusterTest=true 时 **ClusterTestRequestProcessor 替换 default processor** (86 行测试专用, 多 namesrv 集群测试回环) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 可见性窗口推导 | 心跳写 (Netty 线程) → 对象内 set (非 volatile) → 扫描读: 无 HB 边 → 旧值窗口 ≤ 心跳周期+5s; 但 2min 超时 >> 窗口 → **误踢需连续丢失 12 次且全部不可见 — 概率极低** ✅ | 通过 |
| V2 | WIPE 生命周期 | DataVersion 不变 → QueueData 不重建 → WIPE 保持; 配置变更 → 重建还原 (topicConfig.perm 为准) — 管理员需知悉一次性语义 ✅ | 通过 |
| V3 | 冲突静默闭环 | 冲突 → 静默 SUCCESS → broker 无感知继续注册 → 下轮 stateVersion 追平 (controller 递增) 后成功 — 最终一致 ✅ | 通过 |
| V4 | 批注销合并 | 双触发 (事件+扫描) → 队列 Set 去重 → 单次处理; 再触发 removeIf 幂等 — 无重复清理 ✅ | 通过 |
| V5 | 弱一致自愈 | 扫描漏看 (注册并发) → 下轮 5s 补扫 — 收敛 ✅ | 通过 |
| V6 | prime slave 擦权幂等 | 每注册重擦 (perm & ~WRITE) — 幂等不累积, 与 WIPE 区分 (WIPE 非幂等覆盖) ✅ | 通过 |
| V7 | 心跳余量再算 | 可见性延迟 (≤5s) << 超时 (2min) — 误踢窗口闭合 ✅ | 通过 |
| V8 | zone 级联完整性 | brokerData 过滤 → queueData 按 brokerName 清理 → filterServer 按 addr 集清理 — 三表同步 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **语义标注** | **lastUpdateTimestamp 非 volatile + 无锁更新** — 心跳写/扫描读跨线程可见性窗口 (64 位原子 + 5s 周期 + Netty HB 缓解, 误踢概率极低) | 大纲 §5 补注 |
| 14 | **语义标注** | **WIPE/ADD 一次性权限**: DataVersion 变化 → QueueData 重建还原 (topicConfig.perm 为准); prime slave 擦权幂等不受影响 | 大纲 §6 补注 |
| 15 | **语义标注** | **stateVersion 冲突静默**: 空 result 非 null → broker 收 SUCCESS 无感知 — 靠下轮注册自愈 (最终一致) | 大纲 §2 补注 |
| 16 | **补充锚点** | **锁使用不一致**: 读路径部分加锁 (queryBrokerTopicConfig 无锁, CHM 原子兜底) — 无嵌套问题, 弱一致可接受 | 大纲 §1 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 六表结构 (锁使用差异) — 可写 ✅
- §2 注册 (仲裁链/冲突静默面) — 可写 ✅
- §3 注销 (三入口/批量/合并/兜底) — 可写 ✅
- §4 查询 (线程池/门禁/快照/三后置加工) — 可写 ✅
- §5 心跳 (三来源/可见性窗口/双通道) — 可写 ✅
- §6 5.x 新面 (acting 全链/WIPE 生命周期) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 8 项全过 (V1-V8); **新发现 4 处全部修复** (时间戳可见性/WIPE 一次性/冲突静默/锁使用不一致)。核心认知: namesrv 依赖 ConcurrentHashMap + 时间/周期参数余量而非显式同步 — "宽松一致性"是注册中心取舍。大纲经修复后反写测试全过。
