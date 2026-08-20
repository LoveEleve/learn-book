# MI-1 MeterRegistry — outline 收敛版

> 核心文件: MeterRegistry.java (1337) + simple/ (4) + composite/ (13) + step/ (13) + push/ (3) + cumulative/ (6) + noop/ (10) + search/ (4) + logging/ (3) + HighCardinalityTagsDetector.java (308) + Metrics.java (323)
> harness: MiniMI1 25/25 PASS (R3+R6 补强 18→22→25) | 日期: 2026-08-17

## 一、核心架构: 双重 ID 映射 (设计核心)

```
注册 (getOrCreateMeter L688-751):
  originalId ──mapId──▶ mappedId ──▶ meterMap (CHM, filtered id)
      │                                    ▲
      └─▶ preFilterIdToMeterMap (HashMap, pre-filter id → meter)  ──┘
            (读无锁 L103-106 注释, 写 meterMapLock 保护)
```

- `meterMap` CHM: 并发迭代安全 (L99-102 注释)
- `preFilterIdToMeterMap` HashMap: 读 unguarded, 写 lock (设计权衡 P1)
- `meterToPreFilterIdMap`: remove 反向查询 (L110-113) — **多 preFilter id → 同 mapped id (filter 变换合并) → 同一 meter**, remove 时清全部 preFilter 映射 (官方测试 multiplePreFilterIdsMapToSameId 实证)
- `stalePreFilterIds`: 迟配置 filter 后旧 ID 集合 (L118-120; meterFilter L909 addAll)
- `syntheticAssociations`: 合成 meter 级联移除 (L124-127, L853-869)

## 二、注册主流程 (getOrCreateMeter L688-751)
1. preFilter 命中且非 stale → 直接返回 + 双重注册警告 — **不调用 filters (性能优化, doNotCallFiltersWhenUnnecessary 实证)** (仅 Gauge/FunctionCounter/FunctionTimer 警告, L768-775)
2. `mapId`: filters.map() 链式变换 (L677-684; syntheticAssociation 跳过)
3. meterMap(mappedId) 命中 → unmarkStale + 返回
4. `synchronized(meterMapLock)` 双重检查
5. `accept(mappedId)`: DENY→noop 降级 / **ACCEPT→短路返回 true** / NEUTRAL→继续 (L785-794)
6. `configure`: filters.configure() 链 + `merge(defaultHistogramConfig())` — this 优先 (L723-729, DSC L78-93)
7. meterSupplier.create(registry, mappedId, config, pauseDetector) — 工厂方法模式
8. synthetic 关联登记 + onMeterAdded listeners + 三 map 写入 + unmarkStale

## 三、Filter 三重作用 (MI-7 域的前置理解)
| 作用 | 方法 | 语义 |
|---|---|---|
| map | `map(Id)` L677-684 | ID 变换 (重命名/加标签) — 改变 meterMap 的 key |
| accept | `accept(Id)` L785-794 | DENY 拒绝→noop; ACCEPT 短路; NEUTRAL 继续 |
| configure | `configure(Id, DSC)` L723 | 直方图配置变换 |

## 四、Registry 家族 (E1)
| 类 | 机制 | 锚点 |
|---|---|---|
| SimpleMeterRegistry | mode: CUMULATIVE (默认) / STEP (step=1min); newTimer/Summary 里 `HistogramGauges.registerWithCommonFormat` 自动注册直方图 gauge (联动 MI-3) | SimpleMeterRegistry L61-127 |
| CompositeMeterRegistry | new* 返回 Composite* 家族; add(): `forbidSelfContainingComposite` 自包含禁止 + 嵌套 composite 的 addParent + `updateDescendants` 展平非 composite 后代 (L194-218); 已有 meter 级联 add/remove; onMeterAdded 监听新 meter 转发 (L69-72); `lock()` CAS 自旋 (L195-207); updateDescendants L209-239 | L41-253 |
| StepMeterRegistry/StepValue | 双缓冲: current (DoubleAdder) + previous (volatile); rollCount: stepTime=now/stepMillis CAS 推进; **连续步长保留, 空档归零**; poll()=上一完整步长; _closingRollover 收尾 | StepValue L50-71 |
| PushMeterRegistry | 推送型基类 (E1 域内) | push/ |
| LoggingMeterRegistry | 日志输出型 (E10) | logging/ |

## 五、Metrics 门面 (E5)
- `globalRegistry = new CompositeMeterRegistry()` (L35)
- addRegistry/removeRegistry + counter/timer/summary/gauge 静态委托 (L43-64)
- 静态 `More` 委托

## 六、查询 API (E9)
- `find(name)` → Search: meterStream 流式过滤 (name 谓词 + tags/requiredTagKeys/tagMatches, L256-268) → findOne (L200-202) / findAll (L320-323); `acceptFilter()` 转 MeterFilter (L215-225, 联动 MI-7)
- `get(name)` → RequiredSearch: 不匹配抛 MeterNotFoundException

## 七、生命周期
- remove 三态: remove(meter) / removeByPreFilterId (L821-826) / remove(mappedId) (L838-859, 级联 synthetics L845-852) — 级联 synthetics + onMeterRemoved
- clear(): keySet forEach remove (L871-875)
- close(): CAS + 遍历 meter.close() + highCardinalityTagsDetector.close() (L1267-1280); 关闭后注册→noop
- isClosed() 后注册 → `noopBuilder.apply(mappedId)` (L707)

## 八、Noop 降级 (P5)
- 触发: isClosed() (L707) / accept DENY (L711)
- NoopCounter: increment 空操作 + count=0 (静默)
- NoopMeter 家族 10 文件 (Counter/Timer/Gauge/Summary/LongTaskTimer/FunctionCounter/FunctionTimer/TimeGauge/Meter/CustomMeter)

## 九、因果链补强 (R6)
- `getMappedId` (L669-675): 唯一外部调用方 **MultiGauge** (L94, MI-2 域) — 快照行 pre-filter ID → 映射 ID 转换; 内部被 removeByPreFilterId (L821-826) 使用
- `requireValid` 校验链: MeterRegistryConfig.requireValid (L47-49) → validate().orThrow(); SimpleConfig.validate = checkAll(step, mode) (L58-60); MeterRegistryConfigValidator.checkAll (L36) — E7 配置校验 (MI-7 联动)

## 十、其他
- HighCardinalityTagsDetector (E11, 308 行): 同名列频率计数 > 阈值 → HighCardinalityMeterInfo; 默认阈值 `max(1000, min(堆10%×2000, 2M))` (calculateThreshold L177-183); 频率计数 (L168-176); scheduleWithFixedDelay 定时检测; Config.withHighCardinalityTagsDetector 启动 (L1018-1063)
- 双重注册警告: WarnThenDebugLogger 降级 (L68)
- BASE_TIME_UNIT_STRING_CACHE EnumMap (L69-79)
- TimeGauge: newTimeGauge 包装 Gauge + TimeUtils.convert (L253-276)

## 十一、harness 验证 (MiniMI1, 25/25)
1. 注册主流程: counter/timer/summary/gauge 全通过 ✅
2. find/get 查询语义 ✅
3. DENY → noop 降级 (count=0) ✅
4. renameTag 改 tag 不改 name — **harness 打脸修正** (原断言"旧名查询为空"错误, find 按 name 仍命中) ✅
5. ACCEPT 短路 → 后置 DENY 不生效 ✅
6. Composite 转发到子 registry + 自身可见 ✅
7. Metrics 门面转发 ✅
8. 同名不同类型 IllegalArgumentException ✅
9. 重复注册幂等 (同一实例) ✅
10. 迟 filter 后相同 id 重新注册返回原实例 (stale 实证) ✅
11. STEP 单步长推进 poll 上一步值 ✅
12. **STEP 跨步长空档归零** (实证 StepValue L58-62) ✅
13. LoggingMeterRegistry start→定时 publish→sink 输出 (E10; 空档归零在 publish 链传播实证) ✅
14. HCD 高基数检测 (3>2 阈值) + 频率计数 (E11) ✅