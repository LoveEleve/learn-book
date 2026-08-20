# MP-6 乐观锁 — @Version CAS 更新: 旧值捕获 + 新值回写 + WHERE 条件注入

> 前置: [[MP-2-metadata]] (versionFieldInfo/getVersionOli) | 复用: [[MP-4-plugin]] (beforeUpdate 回调) | 对照: [[s29-tx]] (悲观锁 vs 乐观锁) | 引出: [[MP-7-fill]]
> 🟡 Working | 6 KP | [模式: CAS(比较并交换)+策略(版本类型函数)]
> Pass 2 闭环: q1(CAS 语义) q2(双路径) q3(版本类型策略) q4(空值守卫) q5(wrapperMode) q6(执行链)

**读者处境**: 实体加 `@Version private Integer version`, updateById 就自动乐观锁 — 为什么并发更新只有一个成功?version 字段怎么"自动 +1"?WHERE 里的旧版本值从哪来?这篇拆乐观锁插件的 CAS 三步骤: 捕获旧值、注入条件、回写新值。

### 1. 入口与旧值捕获 — beforeUpdate + doOptimisticLocker

场景: 乐观锁拦截什么操作?旧版本值怎么拿到?
源码路径:
- `OptimisticLockerInnerInterceptor.java:106-116` — beforeUpdate(q1 入口): **仅 UPDATE 命令**; parameter Map 才处理 → doOptimisticLocker
- `OptimisticLockerInnerInterceptor.java:118-136` — 旧值捕获: et=`map.get(ENTITY)`(updateById/update(et, wrapper) 的实体)→`getVersionFieldInfo(et.getClass())`(**MP-2 versionFieldInfo**)→`versionField.get(et)` 旧值; **null→自定义 exception 抛或 return**(q4)
- `OptimisticLockerInnerInterceptor.java:154-156` — updateById 路径: `map.put(MP_OPTLOCK_VERSION_ORIGINAL, 旧值)` + `versionField.set(et, 新值)`
关键设计: CAS 捕获(q1): 拦截时机是 MP-4 的 beforeUpdate — 在 Executor.update 前把实体的版本状态读出; 旧值 null 守卫(q4): 版本字段必须初始化(否则不拦截/可自定义抛)。[模式: 执行期状态捕获]
数据流: updateById(et) → Executor.update 拦截 → beforeUpdate → et 旧版本值 → MP_OPTLOCK_VERSION_ORIGINAL → 实体回写新值。

### 2. 条件注入 — 双路径: wrapper vs updateById

场景: update(et, wrapper) 和 updateById(et) 的版本条件分别怎么进 SQL?
源码路径:
- `OptimisticLockerInnerInterceptor.java:143-153` — **"update" 方法路径**(q2): aw=`map.get(WRAPPER)`; null→**新建 UpdateWrapper 并 eq(versionColumn, 旧值)**; 否则 `aw.apply(versionColumn + " = {0}", 旧值)` — 条件进 wrapper
- `OptimisticLockerInnerInterceptor.java:154` — **updateById 路径**: `map.put(MP_OPTLOCK_VERSION_ORIGINAL, 旧值)` — 条件进注入 SQL 参数
- `TableFieldInfo.java:578-586` — getVersionOli(MP-2): **`AND version=#{MP_OPTLOCK_VERSION_ORIGINAL}`** + convertIf 非空守卫 — 注入 SQL 的 WHERE 引用
关键设计: 双通道(q2): wrapper 路径把条件 apply 进 wrapper(更新 SQL 由 wrapper 拼); updateById 路径把旧值放参数通道, 注入 SQL 的 getVersionOli 片段引用 — **两条路径殊途同归: WHERE version=旧值**。[模式: 双通道条件注入]
数据流: methodName 判定 → "update"? wrapper.apply({0} 旧值) : MP_OPTLOCK_VERSION_ORIGINAL → SQL 含 version 条件。

### 3. 新值生成 — VERSION_FUNCTION_MAP 类型策略

场景: version 字段支持哪些类型?怎么递增?
源码路径:
- `OptimisticLockerInnerInterceptor.java:290-297` — **VERSION_FUNCTION_MAP 8 类**(q3, L290-297): 数值 `long/Long→+1`/`int/Integer→+1`; 时间 `Date→new Date()`/`Timestamp→now`/`LocalDateTime→now`/`Instant→now`
- `OptimisticLockerInnerInterceptor.java:300-305` — 不支持类型 → **返回原值**(仅比较不递增)
关键设计: 类型策略(q3): 数值类递增(含基本类型 long/int)、时间类取当前 — 版本演进语义按类型映射; 不支持类型(BigDecimal 等)降级为"只比较不递增"; **getUpdatedVersionVal protected 可覆写自定义策略**(L308-311)。[模式: 策略映射+扩展点]
数据流: getUpdatedVersionVal(clazz, 旧值) → VERSION_FUNCTION_MAP 查表 → 新值 → versionField.set。

### 4. wrapperMode 与冲突语义 — 边界兼容

场景: 只用 LambdaUpdateWrapper 更新(无实体)怎么乐观锁?冲突怎么识别?
源码路径:
- `OptimisticLockerInnerInterceptor.java:158-163,198-203` — **wrapperMode**(q5): 无 et 仅 wrapper → `setVersionByWrapper` 从 wrapper 条件中找版本 eq 值
- 冲突语义: 更新影响行数 0 = 并发冲突(WHERE version=旧值 不匹配)
关键设计: 兼容面(q5): wrapper 模式让纯 wrapper 更新也能乐观锁; 冲突识别靠影响行数(0=失败) — 与 M-2 的 update 返回行数语义衔接。[模式: 模式兼容+行数语义]
数据流: update(wrapper) → wrapperMode? setVersionByWrapper : (et 路径) → 执行 → 行数 0 = 冲突。

### 负面空间 — 乐观锁刻意不做的事

- **不做重试**: 冲突(行数 0)由业务层决定重试/提示, 插件不自动重试
- **不做悲观锁**: 不加 SELECT FOR UPDATE, 与 spring-tx 悲观方案对照(高并发下冲突率高的场景悲观锁更合适)
- **不做版本回滚**: 实体上的新值不回滚(拦截后实体已被改), 失败重试需重新读取

→ 引出: 自动填充是另一类执行期参数处理 — MetaObjectHandler 在 insert/update 前填充审计字段 → [[MP-7-fill]]
