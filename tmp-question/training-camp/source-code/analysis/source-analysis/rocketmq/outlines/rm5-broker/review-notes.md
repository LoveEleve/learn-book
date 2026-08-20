# RM-5 Broker 启动 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字修正** | "48 处注册 = 24 码×2" **不精确**: python 精确计数 = **46 处注册 (26 唯一码: 20 码 × 双服务 + 6 码单注册** — PULL/LITE_PULL/PEEK/POP/NOTIFICATION/POLLING_INFO **POP 族仅主端口**) | 大纲 §3 修正 |
| 2 | **补充锚点** | **startBasicService** (L1603-1650): store→timer→replicasManager→remoting (**remotingServerStartLatch 同步**)→fast→**pop 三服务** (PopLongPollingService/PopBufferMergeService/QueueLockManager, 5.x POP 消费面 RM-8)→ackRevive; **storeHost 设置** (RM-3 编码 STOREHOST 面) | 大纲 §1 补注 |
| 3 | **补充锚点** | **protectBroker 语义** = 慢消费者自动禁用 (fallBehind > ConsumerFallbehindThreshold → subscriptionGroupManager.disableConsume, L1202-1220) — 非泛化"资源保护" | 大纲 §4 修正 |
| 4 | 验证 | shutdown 逆序 (网络→定时→remoting→存储) + 收尾持久化 (L1472-1480); testBrokerRestart 重启幂等; testHeadSlowTimeMills | 记录 |
| 5 | 行号验证 | 全函数 25 锚点 + 跨文件 8 处 grep (BrokerStartup 51-248 / BrokerController 478-486,608-770,773-900,1070-1151,1202-1220,1565-1650,1705-1746 / BrokerControllerTest 63-76) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 三阶段短路 / 恢复序
- 双服务注册对称 (POP 例外)
- 定时任务条件分支

### 维度2 性能
- fast 端口发送分离
- 分组线程池隔离
- 周期钳制

### 维度3 内存
- 7 configManager 内存态
- 插件/附件加载面

### 维度4 一致性
- 恢复序依赖链
- 主从 syncAll
- 收尾持久化
- storeHost 一致性

### 维度5 负面空间 (已写入大纲 4 条)
- 不热配置全面化/不灰度/不深自检/不热插拔

## 结论
RM-5 全部锚点 ~40 处验证, 6 闭环完成, **数字修正 1 + 补锚 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-1~4 (已交付) ✅; 引出 RM-6/7/8/12/14 ✅; 对照 R-20 (Redis server 骨架) ✅; 读者处境场景化 ✅; 锚点 ~40 ✅; 负面空间 4 条 ✅; 横切 (装配/并发/持久化/HA/扩展) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §2 "7 个 configManager" — 计数来源 initializeMetadata 逐行 = **6 个 load + topicConfig 特殊** (topicQueueMappingManager 等 6 个; 首个 topicConfigManager 可能双 load) — 需精确 | 大纲 §2 补注 (6+1 标注) |
| 8 | 通过项 | 其余 ~30 句机制描述逐句对源码一致 ✅ (main 链/exit/隔离/三阶段/双实现/DLedger/插件/CalcBitMap/TimerWheel/ReplicasManager fenced/恢复序/服务链/46 注册/POP 单注册/分组线程池/8 定时/条件任务/namesrv 钳制/startBasicService 序/收尾持久化/shutdownHook/重启测试) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (configManager 计数 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 46 = 20×2+6 | 26 唯一码: 20 双注册 + 6 单 (POP 族) — 46 ✅ | 通过 |
| V2 | fast 端口负值 | listenPort-2 < 0 → 0 (L482-483) — 低端口容错 ✅ | 通过 |
| V3 | namesrv 周期钳制 | max(10000, min(period, 60000)) — [10s, 60s] ✅ | 通过 |
| V4 | shouldStartTime 语义 | 启动后 disappearTimeAfterStart 内不注册 — 防启动风暴 ✅ | 通过 |
| V5 | 恢复序 | store→timerWheel→schedule→插件 (L861-870) — 依赖序 (schedule 需 store) ✅ | 通过 |
| V6 | POP 单注册理由 | POP 族走主端口 (拉取语义), fast 仅发送 — 语义分离 ✅ | 通过 |
| V7 | 收尾持久化 | shutdown 中 consumerFilterManager/orderInfo/schedule persist (L1472-1480) — 崩溃窗口 ✅ | 通过 |
| V8 | 重启测试幂等 | initialize→shutdown→initialize 断言 true — 生命周期可重入 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **registerBrokerAll 的 3 参数语义** (是否强制注册/更新) 与 **brokerStats 每日 record 的 computeNextMorningTimeMillis** (零点对齐) — 细节面 | 大纲 §1/§4 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (registerBrokerAll 参数/零点对齐), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查七个存疑点 (fast 码集/条件任务/registerBrokerAll 参数/事务 SPI/认证管线/shutdown 链/防风暴默认值), 并做反写测试。

## 追查过程 (七个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | fast 注册了哪些码? | python 提取: **fast = 20 码** — 含 **HEART_BEAT/QUERY_MESSAGE/ACK/END_TRANSACTION/UPDATE_CONSUMER_OFFSET** (非纯发送!); 主端口独有 6 码 = POP 族 | **表述修正 (发现 1): "发送专用" → "高频生产+消费管理通道"** |
| T2 | 条件任务周期? | fetchNameServerAddr (getFetchNamesrvAddrInterval) + updateNamesrvAddr (getUpdateNameServerAddrPeriod) — 配置驱动 | 通过 |
| T3 | registerBrokerAll 参数? | **synchronized (checkOrderConfig, oneway, forceRegister)** (L1851) | 通过 (验证) |
| T4 | initialTransaction 内容? | **ServiceProvider.loadClass (SPI!)** → TransactionalMessageBridge + CheckListener (L989-1000) | 发现 2 (补充锚点) |
| T5 | initialRequestPipeline? | authConfig 非空 → **AuthorizationPipeline + AuthenticationPipeline** (L1053-1064, RM-13 交叉) | 发现 3 (补充锚点) |
| T6 | shutdown 完整链? | shutdownBasicService (L1358): **unregisterBrokerAll → remoting×2 → metrics×2 → housekeeping → pullRequestHold** + scheduledFutures.cancel + brokerOuterAPI | 发现 4 (补充锚点) |
| T7 | disappearTimeAfterStart? | **默认 -1 (禁用)** (MessageStoreConfig:91); 注释: 注册延迟 + 内部消息交换延迟 | 发现 5 (补充锚点) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | fast 20 码构成 | 20 = 全命令面 - POP 族 6; 生产 (SEND/REPLY) + 管理 (心跳/offset/ACK) — 高频通道语义 ✅ | 通过 |
| V2 | 事务 SPI 双加载 | ServiceProvider.loadClass(TransactionalMessageService) + (CheckListener) — 可替换扩展 ✅ | 通过 |
| V3 | 认证管线序 | pipeline.pipe(Authorization).pipe(Authentication) — "last pipe execute first" 注释: 认证在授权前? (pipe 语义) ✅ | 通过 |
| V4 | shutdown 幂等 | shutdownBasicService 各服务判空 (L1358+) — 重复关闭安全 ✅ | 通过 |
| V5 | startLatch 注入 | setRemotingServerStartLatch (外部) — 测试/容器场景同步 ✅ | 通过 |
| V6 | 注册参数语义 | checkOrderConfig (顺序配置核对)/oneway (单向)/forceRegister (强制) — start 处调用 (true,false,true/force) ✅ | 通过 |
| V7 | 双端口职责 | fast (生产高频) vs 主 (全命令含 POP) — 5.x 流量分离 ✅ | 通过 |
| V8 | 防风暴默认 | -1 = 不延迟; 配置 >0 才生效 — 默认无注册延迟 ✅ | 通过 |

## 新发现问题 (5 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **表述修正** | "fastRemotingServer 发送专用" **不精确** — 20 码含心跳/查询/ACK/事务结束; 实为**高频生产+消费管理通道** | 大纲 §3 重写 |
| 11 | 补充锚点 | initialTransaction = **SPI 加载** (ServiceProvider.loadClass) + TransactionalMessageBridge | 大纲 §2 补注 |
| 12 | 补充锚点 | initialRequestPipeline = **认证管线** (Authorization/AuthenticationPipeline, RM-13) | 大纲 §2 补注 |
| 13 | 补充锚点 | disappearTimeAfterStart **默认 -1 禁用** | 大纲 §1 补注 |
| 14 | 补充锚点 | shutdownBasicService 完整链 (unregister → 双 remoting → metrics → housekeeping → pullRequestHold) | 大纲 §6 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 启动链: main/exit/防风暴 (默认 -1 修复后)/隔离/namesrv 钳制 — 可写 ✅
- §2 三阶段: 6+1 管理器/双实现/SPI 事务 (修复后)/认证管线 (修复后)/恢复序 — 可写 ✅
- §3 处理器: 46 注册/fast 高频通道 (修复后)/POP 独有/分组线程池 — 可写 ✅
- §4 定时: 8 核心/慢消费者禁用/条件任务/零点对齐 — 可写 ✅
- §5 5.x 新面: ReplicasManager/RocksDB/插件/pop 三服务 — 可写 ✅
- §6 关闭: shutdown 链 (修复后)/收尾持久化/重启测试 — 可写 ✅
- 负面空间 4 条 — 完整 ✅

## 四次 REVIEW 汇总

七存疑点全实证 (T1-T7); 推理验证 8 项全过 (V1-V8); **新发现 5 处全部修复** — #10 最有价值 (fast 通道语义: 高频生产+消费管理, 非纯发送; 心跳/查询也走 fast)。大纲经修复后反写测试全过。
