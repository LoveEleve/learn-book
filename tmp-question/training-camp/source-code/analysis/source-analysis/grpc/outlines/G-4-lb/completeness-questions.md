# G-4 全视角提问验证 (completeness-questions)

> 🟡 B 域: 30 问 / 5 身份。每问标注大纲覆盖 (✅ / ⚠️ / ❌)。

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | SPI | createSubchannel 为什么必须在 syncContext? | ✅ §1 |
| 2 | 开发者 | SPI | Picker 什么时候被调用? | ✅ §1 每 RPC |
| 3 | 开发者 | 策略委托 | 自定义策略怎么接入? | ✅ §2 META-INF/services |
| 4 | 开发者 | PickFirst | shuffle 是干嘛的? | ✅ §3 负载分散 |
| 5 | 开发者 | PickFirst | Happy Eyeballs 什么时候启用? | ✅ §3 标志位 |
| 6 | 开发者 | Subchannel | 代理地址怎么处理? | ✅ §4 解包 |
| 7 | 开发者 | 轮询 | sequence 为什么随机起点? | ✅ §5 防同相 |
| 8 | 开发者 | 离群 | enforcementPercentage 为什么不是 100? | ✅ §6 留样本 |
| 9 | 架构师 | SPI | Picker 解耦的价值? | ✅ §1 无锁读 |
| 10 | 架构师 | 策略委托 | 默认 PickFirst 的取舍? | ✅ §2 |
| 11 | 架构师 | PickFirst | 两代粒度差异的本质? | ✅ §3 连接粒度 |
| 12 | 架构师 | Subchannel | 连接粒度 = 地址粒度的意义? | ✅ §4 |
| 13 | 架构师 | 轮询 | 组合设计 vs 自实现的取舍? | ✅ §5 |
| 14 | 架构师 | 离群 | 统计阈值 vs 固定阈值? | ✅ §6 自适应 |
| 15 | SRE | Subchannel | 重连退避怎么调? | ✅ §4 G-6 复用 |
| 16 | SRE | 离群 | 被 eject 的地址多久恢复? | ✅ §6 uneject 周期 |
| 17 | SRE | PickFirst | 首地址不可达会怎样? | ✅ §3 250ms 试探 |
| 18 | SRE | 策略委托 | 策略名打错会怎样? | ✅ §2 TRANSIENT_FAILURE |
| 19 | SRE | 轮询 | 全部子流失败的状态? | ✅ §5 TRANSIENT_FAILURE |
| 20 | 学生 | SPI | LoadBalancer 是接口吗? | ✅ §1 |
| 21 | 学生 | PickFirst | PickFirst 是不是只选第一个? | ✅ §3 失败会换 |
| 22 | 学生 | Subchannel | Subchannel 是连接吗? | ✅ §4 逻辑连接 |
| 23 | 学生 | 轮询 | RoundRobin 是轮流吗? | ✅ §5 READY 子流 |
| 24 | 学生 | 离群 | 离群检测是什么? | ✅ §6 |
| 25 | 研究者 | SPI | vs Dubbo LoadBalance SPI? | ✅ 对照声明 |
| 26 | 研究者 | PickFirst | Happy Eyeballs vs hedging 关系? | ✅ §3 同思想 |
| 27 | 研究者 | 轮询 | 加权轮询 (xds) 与普通轮询差异? | ✅ → G-7 桥 |
| 28 | 研究者 | 离群 | vs Envoy outlier detection? | ✅ §6 同源 |
| 29 | 研究者 | Subchannel | 连接复用 vs 每 RPC 连接? | ✅ §4 传输复用 |
| 30 | 研究者 | 策略 | 同 Zone 优先在哪实现? | ✅ → G-7 WrrLocality 桥 |

30/30 全 ✅ 或已声明桥链 (⚠️ 0, ❌ 0)。
