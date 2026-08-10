# Ch7 Pipeline 全视角验证

## 开发者视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | HeadContext+TailContext 为什么是哨兵节点? 去掉 HeadContext 直接用第一个 Handler 做起点会有什么问题? | 4.1 §1 |
| 2 | 入站事件沿 next 传播, 出站事件沿 prev 传播 — 为什么方向相反? | 4.1 §4 |
| 3 | findContextInbound 跳过不匹配的 Handler — skipContext 怎么判断 "同 executor 才跳过"? 不同 executor 为什么不跳过? | 4.1 §3 |
| 4 | handlerState 四态 FSM 每一步转换对应什么生命周期事件? ADD_PENDING→ADD_COMPLETE 转换谁触发? | 4.1 §6 |
| 5 | JDK-8180450 三级 dispatch — 如果不做三级分派会出什么 bug? | 4.1 §7 |
| 6 | @Sharable Handler 被加入两个 Channel — checkMultiplicity 怎么检查? ThreadLocal 缓存在哪里? | 4.2 §1 |
| 7 | SimpleChannelInboundHandler 的 autoRelease — 匹配的消息为什么不自动释放? | 4.2 §6 |
| 8 | ChannelInitializer 为什么在 initChannel 后 remove 自己? 不 remove 会怎样? | 4.4 §1 |

## 性能工程师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | executionMask 位掩码 — 比 instanceof 快多少? skipContext 在长链中节省了什么? | 4.1 §5 |
| 10 | ChannelOutboundBuffer 的 nioBuffers 用 FastThreadLocal 数组 — 1024 不够了怎么办? | 4.3 §3 |
| 11 | 高低水位线 — totalPendingSize 超 highWaterMark 后写操作会阻塞吗? | 4.3 §4 |
| 12 | WriteTask Recycler — Integer.MIN_VALUE 编码 flush 标志是什么原理? | 4.3 §3 |

## SRE/运维视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 13 | Pipeline 日志报 PendingHandlerAddedTask — Channel 卡住了, 怎么排查? | 4.4 §2 |
| 14 | ChannelOutboundBuffer 持续 unwritable — 什么导致写不出去? 怎么定位? | 4.3 §4 |

## 架构师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 15 | Pipeline 的 Intercepting Filter 模式 vs Netty 旧版 ChannelHandler 链 — 为什么要改成双向? | 4.1 §1-2 |
| 16 | CombinedChannelDuplexHandler 为什么需要 DelegatingChannelHandlerContext? 不能直接继承一个双向 Handler? | 4.2 §7 |
| 17 | destroy 两阶段(up/down) — 为什么需要两个方向? 单向 destroy 不行吗? | 4.1 §8 |

## 学生/新人视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 18 | ChannelInboundHandler 和 ChannelOutboundHandler 有什么区别? 我怎么知道我的 Handler 应该是哪个? | 4.2 §2-3 |
| 19 | ctx.fireChannelRead(msg) 和 ctx.write(msg) 有什么区别? 一个读一个写? | 4.1 §4 |
| 20 | PendingHandlerCallback 是什么? 为什么 Handler 添加不是立即生效? | 4.4 §2-3 |

## 覆盖统计

| 角色 | 问题数 | 覆盖 |
|------|:--:|:--:|
| 开发者 | 8 | 4.1§1-7, 4.2§1,6, 4.4§1 |
| 性能工程师 | 4 | 4.1§5-6, 4.3§3-4 |
| SRE/运维 | 2 | 4.3§4, 4.4§2 |
| 架构师 | 3 | 4.1§1-2,8, 4.2§7 |
| 学生/新人 | 3 | 4.1§4, 4.2§2-3, 4.4§2-3 |
| **合计** | **20** | **100%** |
