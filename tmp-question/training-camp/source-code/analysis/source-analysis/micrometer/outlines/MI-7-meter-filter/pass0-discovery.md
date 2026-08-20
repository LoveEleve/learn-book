# MI-7 MeterFilter — Pass 0 发现 + Pass 2 问题清单

> 锚点: MeterFilter.java (481) / MeterFilterReply.java (3 值) / NamingConvention.java (4 预置) / MeterRegistryConfigValidator.java (3 静态) / PropertyValidator.java (10 种) / Validated.java (393) / MeterRegistryConfig.java
> 日期: 2026-08-17

## Pass 0 发现

### 1. 接口 3 默认方法 (L446-481)
- `accept(Id)` → MeterFilterReply.NEUTRAL (L449-452)
- `map(Id)` → id (L466-473)
- `configure(Id, DSC)` → config (L477-481; 只对 Timer/Summary 调用)

### 2. 静态工厂 20 个 (分类, grep 穷举)
| 类别 | 工厂 | 实现机制 |
|---|---|---|
| map 类 (ID 变换) | commonTags L55-64 (Tags.concat 前缀不覆盖) / renameTag L71-83 (前缀+key 重命名) / ignoreTags L96-109 (删 key) / replaceTagValues L122-140 (值替换+exceptions) | map() 里 replaceTags |
| accept 类 (准入) | denyUnless L147-155 (不匹配→DENY) / accept(Predicate) L162-170 (匹配→ACCEPT) / deny(Predicate) L177-185 (匹配→DENY) / accept() L191 / deny() L200 | accept() 里 reply |
| 上限类 | maximumAllowableMetrics L217-232 (全局时序上限, ids Set) / maximumAllowableTags L241-271 (前缀+key 上限, **双方法 accept+configure 委托 onMaxReached**) | accept()+configure() |
| 前缀速记 | denyNameStartsWith L286 / acceptNameStartsWith L297 | 委托 deny/accept |
| configure 类 | maxExpected L308-331 (Timer Duration→nanos) / L346-362 (Summary double) / minExpected L365-401 / L403-421 | configure() 里 **builder().max().build().merge(config)** |
| 条件委托 | forMeters L425-443 (@since 1.16.0, 3 方法全委托) | 谓词分流 |

### 3. 工厂重载计数修正 (R3)
- grep 穷举 static MeterFilter = **20** (非 15): map 4 + accept 5 + 上限 2 + 速记 2 + **configure 6 (maxExpected/minExpected 各 3 重载)** + forMeters 1
- PropertyValidator = **10 种** (非 6): 补 getString/getUrlString/getUriString/getStringMap

### 4. NamingConvention (E7)
- 4 预置: identity / dot=identity / snakeCase (默认, split "." join "_") / camelCase
- 应用点: Meter.Id.getConventionName (L322-323) / getConventionTags (L332-334, tagKey+tagValue 双变换)
- MeterRegistry 默认 namingConvention = snakeCase (L145)

### 5. 校验链 (E7)
- MeterRegistryConfig.requireValid (L47-49) → validate().orThrow()
- 调用点穷举 3 处: DropwizardMeterRegistry L59 / PushMeterRegistry L47 / SimpleMeterRegistry L55 (构造时)
- MeterRegistryConfigValidator: checkAll (L36-44, reduce and 链) / check (L58-70, getter 捕获 ValidationException) / checkRequired (L73-75, required 转换)
- PropertyValidator 10 种 (穷举): getDuration L44 / getTimeUnit L49 / getInteger L54 / getEnum L66 / getBoolean L96 / getSecret L102 / getString L107 / getUrlString L112 / getUriString L135 / getStringMap L158
- Validated: and (L79) / required (L110-112, MISSING) / orThrow (Invalid 抛 ValidationException L307)
- ValidationException extends IllegalStateException (L30)

## 顺序语义 (R2 补强)
- **map 先于 accept/configure**: 注册流程 mapId (L677-684) → accept (L785-794); accept 看到**已 map 的 id** (官方 mapThenAccept + harness 实证)
- ACCEPT 短路: 跳过后续全部 filter (含 deny())

## Pass 2 问题
| # | 问题 | 结论 | 锚点 |
|---|---|---|---|
| P1 | maxExpected merge 方向 | `builder().max().build().merge(config)` — **新建 config 作 this (优先), 传入作 parent**: filter 强制 max 覆盖 meter config | MeterFilter L312-316; DSC L78-92 |
| P2 | maximumAllowableTags 双方法 | 超限时 accept 和 configure **各自**委托 onMaxReached (双通道) — 准入+配置双维度同时封顶 | MeterFilter L255-270 |
| P3 | forMeters 条件委托 | 谓词 true → delegate 3 方法; false → MeterFilter.super (默认) | MeterFilter L425-443 |
| P4 | configure 链累积 | 注册流程 L723-729: 每个 filter 返回非 null 替换 config (后见前); 最后 merge(defaultHistogramConfig) — **优先级: filter 链 > meter builder > registry default** | MeterRegistry L723-729 |
| P5 | NamingConvention 应用 | Id.getConventionName/Tags 双变换; snakeCase 默认 | Meter.java L322-334 |
| P6 | 校验触发时机 | 3 Registry 构造时 requireValid → orThrow | Simple L55/Push L47/Dropwizard L59 |

## 反模式自查
- [x] 锚点全实读验证
- [x] 无编造数字
- [x] 语义精确 (reply 3 值/merge 方向/双方法委托)
- [x] 逻辑闭环 (filter 全生命周期: 构造→链式应用→校验)

## 待 harness 验证
1. commonTags 前缀追加不覆盖
2. renameTag/ignoreTags/replaceTagValues map 变换
3. denyUnless/accept/deny 谓词语义 + ACCEPT/DENY 短路
4. maximumAllowableMetrics 超限 DENY
5. maximumAllowableTags 超限 → onMaxReached
6. maxExpected 强制覆盖 meter 的 max (Timer)
7. forMeters 条件委托
8. snakeCase 命名应用 (Timer 名 "my.timer" → "my_timer"?)