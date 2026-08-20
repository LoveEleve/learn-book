# MI-7 MeterFilter — outline 收敛版

> 核心文件: MeterFilter.java (481) + MeterFilterReply.java + NamingConvention.java + MeterRegistryConfigValidator.java + PropertyValidator.java + Validated.java (393) + MeterRegistryConfig.java
> harness: MiniMI7 **30/30** PASS (打脸 3 次: snakeCase 导出侧 / 重复注册幂等 / Tags dedupe) | 日期: 2026-08-17

## 一、接口契约 (3 默认方法, L446-481)
| 方法 | 默认 | 语义 |
|---|---|---|
| accept(Id) → MeterFilterReply | NEUTRAL L449-452 | 准入判定 (DENY/NEUTRAL/ACCEPT 3 值枚举) |
| map(Id) → Id | 原样 L466-473 | ID 变换 (注册侧, 改变 meterMap key) |
| configure(Id, DSC) → DSC | 原样 L477-481 | 直方图配置变换 (**只对 Timer/Summary 调用**) |

## 二、注册流程中的消费 (MI-1 联动)
**关键顺序语义 (R2 补强)**: `map` 先于 `accept`/`configure` 执行 — **accept 看到的是已 map 的 id** (官方 mapThenAccept 测试 + harness 实证: map 改名后 acceptNameStartsWith 命中新名)
```
mapId: L677-684   for filter: mappedId = filter.map(mappedId)        — 链式叠加
accept: L785-794  for filter: DENY→false(noop) / ACCEPT→短路返回     — 短路语义
configure: L723-729  for filter: cfg = filter.configure(id, cfg)      — 链式累积 (后见前)
                  最后 cfg = cfg.merge(defaultHistogramConfig())      — default 最低优先
```
优先级: **filter 链 > meter builder > registry default**

## 三、静态工厂 20 个 (分类, grep 穷举验证)
### map 类 (ID 变换)
- commonTags L55-64: `Tags.concat(common, idTags)` = `Tags.of(common).and(idTags)` — 真正机制: **sorted-merge dedupe, 同 key 冲突保留后集 (meter 自身 tag)** (Tags.java L147-148 注释实证)
- renameTag L71-83: name 前缀匹配 + 指定 key 重命名
- ignoreTags L96-109: 删除指定 key
- replaceTagValues L122-140: 值替换 + exceptions 保留原值

### accept 类 (准入)
- denyUnless L147-155: 不匹配→DENY (白名单)
- accept(Predicate) L162-170: 匹配→ACCEPT (保证包含)
- deny(Predicate) L177-185: 匹配→DENY (排除)
- accept() L191 / deny() L200: 全量版本 (作为 onMaxReached 从属)

### 上限类
- maximumAllowableMetrics L217-232: 全局时序上限 (ids CHM keySet, size>max→DENY) — 成本控制
- maximumAllowableTags L241-271: 前缀+key 上限 (observedTagValues Set); **超限 → onMaxReached 的 accept+configure 双通道委托**

### configure 类 (各 3 重载, 共 6)
- maxExpected: L308-331 Duration→nanos (Timer) / L333-344 long→nanos / L346-362 double (Summary)
- minExpected: L365-401 Duration / L390-401 long / L403-421 double
- **P1 关键语义**: `builder().max().build().merge(config)` — 新建 config 作 **this** (优先) → **filter 强制覆盖 meter 的 max** (harness 实证 100ms 覆盖 5s)

### 其他
- denyNameStartsWith L286 / acceptNameStartsWith L297: 委托速记
- forMeters L425-443 (@since 1.16.0): 谓词分流, 3 方法全委托, false→MeterFilter.super 默认

## 四、NamingConvention (E7)
- 4 预置: identity / dot=identity / **snakeCase (MeterRegistry 默认 L145)** / camelCase
- **P5 关键语义 (harness 打脸修正)**: 变换在**导出侧** — getId().getName() 保留原始名, getConventionName/conventionTags 才应用变换
- 应用点: Meter.Id.getConventionName (L322-323) / getConventionTags (L332-334, tagKey+tagValue 双变换)
- snakeCase 实现: `split(".")` join `_` (仅 name/tagKey, tagValue 不动)

## 五、校验链 (E7)
- 触发: 构造时 requireValid — 调用点穷举 3 处 (SimpleMeterRegistry L55 / PushMeterRegistry L47 / DropwizardMeterRegistry L59)
- 链: MeterRegistryConfig.requireValid (L47-49) → validate().orThrow() → ValidationException (extends IllegalStateException L30)
- MeterRegistryConfigValidator: checkAll (reduce and 链 L36-44) / check (getter 捕获异常 L58-70) / checkRequired (required 转换 L73-75)
- PropertyValidator 10 种 (穷举 L44-158): getDuration L44 / getTimeUnit L49 / getInteger L54 / getEnum L66 / getBoolean L96 / getSecret L102 / getString L107 / getUrlString L112 / getUriString L135 / getStringMap L158
- Validated: and (L79) / required (L110-112 MISSING) / orThrow (Invalid 抛异常)

## 六、harness 验证 (MiniMI7, 30/30)
1. commonTags 追加不覆盖 ✅
2. renameTag/ignoreTags/replaceTagValues (含 exceptions) ✅
3. denyUnless 白名单 → noop 降级 ✅
4. maximumAllowableMetrics 超限 DENY ✅
5. maximumAllowableTags 超限 → onMaxReached(deny) ✅
6. maxExpected **强制覆盖** meter 的 max (100ms<5s) ✅
7. forMeters 只对匹配谓词委托 (计数=1) ✅
8. **snakeCase 导出侧语义** (getName 原始 / getConventionName 变换) — 打脸修正 ✅
9. map→accept 顺序: accept 看到 map 后 id + ACCEPT 短路跳过 deny ✅
10. maximumAllowableTags 边界: 不同 tag key 不受限 / 已在允许集重复注册幂等 (同实例, 打脸修正) ✅
11. camelCase: 首段保留 + 后续段首字符大写 (已有大写保留) — 细节 L66-100
12. 校验链触发实证: SimpleMeterRegistry 构造 + 非法 step → ValidationException (requireValid→orThrow 闭环) ✅
13. commonTags 机制级修正: dedupe (Tags.java L147-148) 而非前缀追加
14. renameTag 前缀不匹配 → 原样不动 (官方 renameTags 对照)
15. replaceTagValues 函数只对匹配 key 调用;Tags 不可变 dedupe → 同 key 2 值不存在 (打脸 3)
16. maximumAllowableTags 超限 + onMaxReached=accept() → 放行 (双通道放行路径)
17. forMeters 非匹配: accept 不委托 (注册成功) + map 不委托 (无 mapped tag, 计数 0)
18. minExpected 强制覆盖 (Summary 100<999)
19. maxExpected 类型筛选: 对 GAUGE 无效 (config 原样);configure 只对 Timer/Summary