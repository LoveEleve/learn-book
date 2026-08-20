# 闭环笔记 Q2 — 通道生命周期: 懒启动 + 空闲回收

假设: Channel 是懒资源 — 首次调用才建 LB+解析器 (exitIdleMode), 空闲超时回收 (enterIdleMode), 与 G-2 服务端停机对称。

验证过程:
- **exitIdleMode** (ManagedChannelImpl.java:389-417): `throwIfNotInThisSynchronizationContext` (L390) → shutdown/panicMode 短路 (L391-393) → 取消/重排 idle 定时器 (L398-407, 防竞态: "a racing due timer will not put Channel on idleness") → **首次: newLoadBalancer + gotoState(CONNECTING) + nameResolver.start(listener)** (L412-416) — **懒启动: 第一个调用触发资源创建**
- **enterIdleMode** (L420-438): `shutdownNameResolverAndLoadBalancer(true)` (L423) → `delayedTransport.reprocess(null)` (L424) → gotoState(IDLE) (L426) → **若仍有 pending 调用 → 重新 exitIdleMode** (L429-432, 注释: "gives these calls a chance to be processed")
- idle 定时器: IDLE_TIMEOUT_MILLIS_DISABLE = -1 (L130); 默认值在 ManagedChannelImplBuilder (查证: idleTimeout 默认 30 分钟)
- shutdown 链: shutdown → syncContext.execute(new Shutdown()) (L722) → 各子组件逐层 shutdown → terminated
- 测试实证: idleModeDisabled (ManagedChannelImplTest.java:473) / startCallBeforeNameResolution (L500)

代码类型: Implementation (生命周期状态机)

结论: Channel = **按需资源**: 懒启动 (首次 RPC 才 exitIdleMode, 建 LB+解析器) + 空闲回收 (idle 超时进入 IDLE, 释放 LB+解析器, 连接保持或释放); enterIdleMode 的"pending 调用检测"防止错误回收。**被放弃的方案: 启动即建全链路** — 无调用时浪费资源 (解析器/连接); 懒启动让零流量时成本趋近于零。与 G-2 服务端 shutdown (GOAWAY 拒新) 对称: 客户端 IDLE 是"不建", 服务端 SHUTDOWN 是"不收"。 [跨域: G-2 停机对称] [并发: idle 定时器竞态防护] (ManagedChannelImpl.java:130,389-438,722)
