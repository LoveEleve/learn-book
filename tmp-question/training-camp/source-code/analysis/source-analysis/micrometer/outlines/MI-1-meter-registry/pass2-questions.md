# MI-1 MeterRegistry — Pass 2 问题清单与收敛

## 已深挖问题
| # | 问题 | 结论 | 锚点 |
|---|---|---|---|
| P1 | preFilterIdToMeterMap 读无锁安全性 | **设计权衡**: 写全在 meterMapLock 内, 读无锁 (注释 L103-106 明示)。HashMap get 与 put 竞争最坏返回 null/旧值, 不会破坏结构; 注册路径可容忍重试 | MeterRegistry L671 |
| P2 | syntheticAssociation 机制 | HistogramGauges 为 Timer/DistributionSummary 注册百分位/桶 gauge 时 `.synthetic(meter.getId())` — 合成 gauge 关联源 meter, remove 时级联删除 (L853-869); 只此一处调用 | HistogramGauges L125/L140 |
| P3 | Step 家族聚合 | StepValue 双缓冲: current (DoubleAdder) + previous (volatile); rollCount 按 stepTime=now/stepMillis CAS 推进; **连续步长才保留值, 空档归零**; poll()=上一完整步长; _closingRollover 收尾 | StepValue L50-71 |
| P4 | 配置 merge 顺序 | merge(parent): **this 优先, null 取 parent**; 注册流程 filter.configure 变换后 merge(defaultHistogramConfig()) — default 最低优先级 | DistributionStatisticConfig L78-93; MeterRegistry L723-729 |
| P5 | Noop 降级路径 | 两触发点: isClosed() (L707) + accept DENY (L711); NoopCounter increment 空操作 + count=0 静默降级 | MeterRegistry L707/L711; NoopCounter 全 |
| P6 | 双重注册警告 | 只对 Gauge/FunctionCounter/FunctionTimer 警告 (L768-775), Counter/Timer 静默 — 幂等重复注册太常见; WarnThenDebugLogger 降级 | MeterRegistry L768-775 |

## Pass 2 收敛判定
- 注册主流程全链理解: registerMeterIfNecessary → getOrCreateMeter → mapId → 双 map 检查 → lock → accept → configure+merge → create → synthetic 关联 → listeners → 三 map 写入 ✅
- Filter 三重作用 (map/accept/configure) + 短路语义 (ACCEPT 立即通过 L792) ✅
- remove 三态 + 级联 synthetics + listeners ✅
- close/clear/isClosed 生命周期 ✅
- Metrics 门面 = globalRegistry (Composite) 全委托 ✅
- Search/RequiredSearch 查询 = meterStream 流式过滤 (name/tags 谓词) + acceptFilter 转 MeterFilter 联动 MI-7 ✅

## 反模式自查
- [x] 锚点全部实读验证 (非仅行号存在)
- [x] 无编造数字/行号
- [x] 机制描述均为源码实证
- [x] 逻辑闭环 (注册/查询/移除/关闭全链)

## 待 harness 验证 (费曼)
1. SimpleMeterRegistry 注册 counter/timer/gauge + find 查询
2. filter DENY → noop 降级 (count()=0)
3. filter ACCEPT 短路
4. filter.map 重命名 → meterMap 用 mapped id
5. CompositeMeterRegistry 转发 (add 后注册 → 子 registry 可见)
6. Metrics.addRegistry 全局可见
7. 同名不同类型 → IllegalArgumentException