# Z-7 Client API — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **表述精确化** | 执行计划 "ClientCnxn/Packet/SendThread+EventThread" — 补全**三队列 + XID 特殊值** (outgoing/pending/事件 + -2/-4/-8/-1) | 大纲 §2 |
| 2 | **补充锚点** | **primeConnection 倒序入队**: 连接 → auth → watch (addFirst, L1092) — 重连恢复顺序语义 | 大纲 §3 |
| 3 | **补充锚点** | **watch 重注册分批**: SET_WATCHES_MAX_LENGTH + SetWatches/SetWatches2 兼容 (L1015-1075) | 大纲 §3 |
| 4 | **语义标注** | **conLossPacket 全量结算**: 连接丢失 → 挂起包按状态错误码 (AUTHFAILED/SESSIONEXPIRED/CONNECTIONLOSS, L782-797) — at-most-once | 大纲 §4 |
| 5 | 行号验证 | 全函数 28 锚点 + 跨文件 8 处 grep (ZooKeeper 265-362,445+ / ClientCnxn 120-153,469-533,725-797,969-1284 / ClientCnxnSocket 131-150) | 记录 |

## 07 五维度

### 维度1 功能正确性
- XID FIFO 匹配
- watch 响应驱动注册
- 连接丢失结算

### 维度2 性能
- 双线程分离
- 三队列解耦
- ping 自适应

### 维度3 内存
- 三队列容量
- watch 列表分批

### 维度4 一致性
- 会话恢复 + watch 重注册
- 单连接串行顺序
- SASL 认证

### 维度5 负面空间 (已写入大纲 6 条)
- 不协议扩展/不自动重试/不本地缓存/不 watch 持久化/不压缩

## 结论
Z-7 全部锚点 ~28 处验证, 8 闭环完成, **表述精确化 1 + 补充锚点 2 + 语义标注 1**。🟡 B 无 harness。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 Z-5/Z-6/Z-4 ✅; 引出 Z-8 ✅; 对照 Jedis/Redisson ✅; 读者处境场景化 ✅; 锚点 ~28 ✅; 负面空间 6 条 ✅; 横切 (并发/网络/会话/回调) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §2 未提 **EventThread 死事件队列** (queueEventOfDeath — L1235): AUTH_FAILED 后事件线程终止 — 生命周期收尾面 | 大纲 §2 补注 |
| 8 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (门面/回调族/状态机/双线程/三队列/XID/primeConnection/超时面/read-only/finishPacket) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (queueEventOfDeath #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | XID 匹配闭环 | FIFO pendingQueue — 服务端按序响应 → 匹配无歧义 ✅ | 通过 |
| V2 | watch 响应驱动 | 失败不注册 — 无幽灵 watch ✅ | 通过 |
| V3 | 重连恢复闭环 | sessionId 恢复 + setWatches 重注册 — 断线状态重建 ✅ | 通过 |
| V4 | ping 自适应 | readTimeout/2 + 10s 上限 — 心跳不空转 ✅ | 通过 |
| V5 | 连接丢失结算 | conLossPacket 全量 — 无挂起泄漏 ✅ | 通过 |
| V6 | 同步封装正确 | 异步内核 + notifyAll — 语义等价 ✅ | 通过 |
| V7 | 事件串行 | EventThread 单线程 — 回调无并发竞态 ✅ | 通过 |
| V8 | SASL 失败降级 | LoginException → AuthFailed 事件 + 继续连接 (注释 L1151-1160) ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **chroot 语义**: prependChroot (L1097-1112) — 客户端路径前缀 (多租户隔离) | 大纲 §3 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (chroot), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (readResponse 详细/心跳包响应/服务端推送/多地址/连接权重重试/状态转换), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | readResponse 详细? | ClientCnxnSocket.readResponse: replyHeader 解析 → xid 匹配 pendingQueue → finishPacket; 特殊 xid (NOTIFICATION) 事件 | 通过 (验证) |
| T2 | ping 响应? | PING_XID 响应 → 仅更新 idleRecv (无回调) — 保活确认 | 通过 (验证) |
| T3 | 服务端推送? | NOTIFICATION_XID (服务端) — 事件异步推送 (Z-6 投递面) | 通过 (验证) |
| T4 | 多地址? | hostProvider.next(1000) (L1197) — 地址轮询/重试 | 发现 10 (补锚) |
| T5 | 连接权重? | ClientCnxnLimitException (服务端, Z-5 connThrottle 交叉) — 超限断连重试 | 通过 (验证) |
| T6 | 状态转换? | CONNECTING→CONNECTED→CONNECTEDREADONLY→CLOSED + AUTH_FAILED — 转换驱动事件 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 保活闭环 | ping 发送 (自适应) + 响应更新 idleRecv — 双向活性 ✅ | 通过 |
| V2 | 事件推送面 | NOTIFICATION_XID 异步 — 与请求响应分离 ✅ | 通过 |
| V3 | 多地址容错 | hostProvider 轮询 — 单点故障换址 ✅ | 通过 |
| V4 | 状态-事件绑定 | 状态转换 → queueEvent (Disconnected/Expired) — 客户端感知 ✅ | 通过 |
| V5 | 重试语义 | 连接级重试 (不重放业务请求) — at-most-once ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **hostProvider 多地址轮询** (L1197): 连接重试换址 — 客户端容错面 | 大纲 §3 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 门面 (双 API/回调族/状态机) — 可写 ✅
- §2 双线程 (Send/Event/三队列/XID) — 可写 ✅
- §3 连接会话 (primeConnection/chroot/超时/多地址) — 可写 ✅
- §4 响应回调 (匹配/结算/连接丢失) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (hostProvider 多地址). 大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 追查 Packet 生命周期六存疑点)

> 动机: 对 outline 全部锚点重新 grep 并追查 Packet 生命周期六个存疑点 (xid 分配时机/queuePacket 闭包/顺序校验/关闭链/心跳包/doIO 发送)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | xid 分配时机? | **发送时才分配**: queuePacket 注释 "Xid... generated later at send-time" (L334-336) + ClientCnxnSocketNIO:115 `setXid(cnxn.getXid())` — **ping/auth 包不分配 xid** (L114-116, 不入 pendingQueue) | 发现 11 (补锚) |
| T2 | queuePacket 闭包? | synchronized(outgoingQueue): **state 不活/closing → conLossPacket 立即结算**; **closeSession 包 → closing=true** (后续包全部结算) (L334-360) | 发现 12 (补锚) |
| T3 | 顺序校验? | readResponse: **Xid out of order 异常** (L945-949) — FIFO 强校验 | 发现 13 (补锚) |
| T4 | 关闭链? | ZooKeeper.close 幂等 (L1216-1233) → cnxn.close → **cleanup()** (L1384: socket cleanup + 通知) | 通过 (验证) |
| T5 | 心跳包处理? | ping 发送不入 pendingQueue — 响应仅更新 idleRecv (无回调) | 通过 (验证) |
| T6 | doIO 发送? | findSendablePacket → setXid + createBB → sock.write → 写完成才入 pendingQueue (L108-130) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | xid 惰性分配 | 发送时分配 → 未发送包无 xid — 语义清晰 ✅ | 通过 |
| V2 | 闭包结算 | 关闭后入队 → conLoss 立即结算 — 无悬挂 ✅ | 通过 |
| V3 | 顺序强校验 | out of order 异常 — 协议违例即失败 ✅ | 通过 |
| V4 | 心跳旁路 | ping 不入 pending — pendingQueue 只含业务请求 ✅ | 通过 |
| V5 | 关闭幂等 | 已关闭直接返回 — 重复 close 安全 ✅ | 通过 |
| V6 | 写完成入 pending | 半写包不匹配 — 正确性 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **XID 发送时分配** (L334-336 注释 + NIO:115): ping/auth 包不分配 — pendingQueue 仅业务请求 | 大纲 §2 补注 |
| 12 | **补充锚点** | **queuePacket 闭包**: 关闭后入队立即 conLoss 结算; closeSession 标记 closing | 大纲 §4 补注 |
| 13 | **补充锚点** | **顺序强校验**: Xid out of order 异常 (L945-949) | 大纲 §4 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 门面 (双 API/回调族/状态机) — 可写 ✅
- §2 双线程 (Send/Event/三队列/XID 惰性分配) — 可写 ✅
- §3 连接会话 (primeConnection/chroot/超时/多地址) — 可写 ✅
- §4 响应回调 (匹配/结算/闭包/顺序校验) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 6 项全过 (V1-V6); **新发现 3 处全部修复** (XID 惰性分配/queuePacket 闭包/顺序强校验)。核心认知: XID 发送时分配 + 写完成入 pending — Packet 生命周期闭环; 关闭后入队立即结算无悬挂。大纲经修复后反写测试全过。
