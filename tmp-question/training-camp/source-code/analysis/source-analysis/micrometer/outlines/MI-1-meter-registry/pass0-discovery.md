# MI-1 MeterRegistry — Pass 0 发现 (计划层)

> 锚点: MeterRegistry.java (1337 行) | 日期: 2026-08-17

## 1. 双重 ID 映射架构 (核心)
- `preFilterIdToMeterMap` (HashMap, L103-106): **pre-filter ID → meter**
- `meterMap` (CHM, L99-102): **mapped/filtered ID → meter** (并发迭代安全)
- `meterToPreFilterIdMap` (L110-113): remove 时反向查询
- `stalePreFilterIds` (L118-120): 迟配置 filter 后旧 ID 集合 (L909 meterFilter 时 addAll)
- `syntheticAssociations` (L124-127): 合成 meter 级联移除
- 全注册写路径 meterMapLock 保护; preFilter 读无锁 (getOrCreateMeter L671)

## 2. getOrCreateMeter 注册主流程 (L688-751)
1. preFilterIdToMeterMap 命中且非 stale → 返回 + 双重注册警告
2. mapId(originalId): 遍历 filters 的 `map()` 变换 (L677-684)
3. meterMap(mappedId) 命中 → unmarkStale + 返回
4. `synchronized(meterMapLock)` 双重检查
5. `accept(mappedId)`: filters `accept()` → DENY 返回 **noop** (L785-794)
6. `configure`: filters `configure()` 变换 DistributionStatisticConfig + `merge(defaultHistogramConfig())` (L723-729)
7. meterSupplier.create(registry, mappedId, config, pauseDetector)
8. synthetic 关联 + onAdd listeners + 三 map 写入

## 3. Filter 三重作用 (mapId L682 / accept L786 / configure L723)
- `map()`: ID 变换 (重命名/加标签)
- `accept()`: DENY→拒绝 (noop 降级) / ACCEPT→短路立即通过 / NEUTRAL→继续
- `configure()`: DistributionStatisticConfig 变换 (直方图配置)

## 4. API 门面
- 公开: counter/timer/summary/gauge/gaugeCollectionSize/gaugeMapSize/find/get/forEachMeter/remove/clear/close
- `Config` 内部类: commonTags→meterFilter 包装 / onMeterAdded/Removed/RegistrationFailed (L930-1013) / namingConvention (默认 snakeCase L145) / pauseDetector (默认 NoPauseDetector) / withHighCardinalityTagsDetector (L1018-1063)
- `More` 内部类 (L1066-1266): longTaskTimer / functionCounter / functionTimer / timeGauge
- 每种 meter 内部注册方法走 registerMeterIfNecessary + **捕获 lambda 避免分配** (implNote)

## 5. 其他关键机制
- 类型检查: registerMeterIfNecessary 里 `meterClass.isInstance(m)` 否则 IllegalArgumentException 同名不同类型 (L629-633)
- remove 三态: remove(meter) / removeByPreFilterId (L825) / remove(mappedId) — 级联 synthetics + onRemove (L853-869)
- close(): CAS closed + 遍历 meter.close() + highCardinalityTagsDetector.close() (L1271-1286)
- isClosed() 后注册 → **noopBuilder.apply(mappedId)** (L707)
- WarnThenDebugLogger doubleRegistrationLogger (L68) — 警告降级
- BASE_TIME_UNIT_STRING_CACHE EnumMap 静态缓存 (L69-79)

## 6. 待 Pass 1 验证的问题
- Q1: Search/RequiredSearch 如何实现查询 (MeterNotFoundException 时机)
- Q2: HighCardinalityTagsDetector 检测机制 (阈值/延迟/关闭)
- Q3: Meter.Id 结构 (withBaseUnit/withTag/syntheticAssociation/convention 转换)
- Q4: SimpleMeterRegistry 的 new* 具体实现 (StepMeter 家族?)
- Q5: CompositeMeterRegistry 转发机制
- Q6: Metrics 门面与全局注册