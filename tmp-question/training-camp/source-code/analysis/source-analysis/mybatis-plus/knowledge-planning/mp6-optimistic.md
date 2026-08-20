# MP-6 乐观锁 — @Version CAS 更新: beforeUpdate 旧值捕获 + 新值回写

> 项目: MyBatis-Plus | 🟡 Working / 1 篇 | OptimisticLockerInnerInterceptor(319)+VersionFactory+TableFieldInfo.getVersionOli(MP-2)
> 基线: MP-PLAN MP-6 — 前置: **MP-2 (versionFieldInfo/getVersionOli) + MP-4 (beforeUpdate 回调)** — 展开 旧值捕获→WHERE 条件注入→新值回写→冲突语义

---

## §0.8

- 🟡 Working，1篇 — 入口(**beforeUpdate L106-116: 仅 UPDATE 命令→parameter Map→doOptimisticLocker**) → 旧值捕获(**doOptimisticLocker L118-165: et=map.get(ENTITY)[updateById/update(et,wrapper) 的实体]→getVersionFieldInfo(et.getClass())[MP-2 versionFieldInfo]→versionField.get(et) 旧值[null→自定义 exception 或 return L130-136]→**methodName 判定**: "update"[wrapper 路径: aw.apply(versionColumn+" = {0}", 旧值) 或新建 UpdateWrapper.eq L143-153] / 其他[updateById→map.put(MP_OPTLOCK_VERSION_ORIGINAL, 旧值) L154]**→versionField.set(et, 新值) L156**; wrapperMode 分支[无 et 仅 wrapper→setVersionByWrapper L158-163]**) → 新值生成(**VERSION_FUNCTION_MAP 8 类 L290-297: long/Long/int/Integer→+1; Date→new Date; Timestamp→now; LocalDateTime→now; Instant→now; 不支持类型→原值 L300-305**) → SQL 联动(**getVersionOli(MP-2 L578-586): `AND column=#{MP_OPTLOCK_VERSION_ORIGINAL}`+convertIf 非空守卫 — 注入 SQL 的 WHERE version=旧值; MP_OPTLOCK_VERSION_ORIGINAL 由 beforeUpdate 写入参数 map**)
- 设计模式: [模式: CAS(比较并交换)+策略(版本类型函数)]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| OptimisticLockerInnerInterceptor.java:106-116 | 入口 | beforeUpdate: 仅 UPDATE 命令; parameter Map 才处理 | High |
| OptimisticLockerInnerInterceptor.java:118-136 | 旧值 | et=ENTITY 键; getVersionFieldInfo(MP-2); versionField.get 旧值; null→exception 或 return | High |
| OptimisticLockerInnerInterceptor.java:143-156 | 双路径 | "update" 方法→wrapper.apply(column={0} 旧值); 其他(updateById)→MP_OPTLOCK_VERSION_ORIGINAL; versionField.set 新值 | High |
| OptimisticLockerInnerInterceptor.java:290-305 | 新值 | VERSION_FUNCTION_MAP: 数值+1/时间 now; 不支持类型原值 | High |
| TableFieldInfo.java:578-586 | SQL 联动 | getVersionOli: `AND version=#{MP_OPTLOCK_VERSION_ORIGINAL}`+非空守卫(MP-2 交付) | High |
| OptimisticLockerInnerInterceptor.java:158-163 | wrapperMode | 无 et 仅 wrapper→setVersionByWrapper(wrapper 模式兼容) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 乐观锁是单机制 — 1篇 (~50行) 按"入口→旧值捕获→双路径注入→新值生成→SQL 联动"展开; versionFieldInfo/getVersionOli 衔接 MP-2(引用), 回调衔接 MP-4(引用)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | beforeUpdate 旧值捕获 + 双路径 (wrapper/updateById) | 🔴 | **为什么🔴**: CAS 核心 |
| P1-2 | MP_OPTLOCK_VERSION_ORIGINAL 与 getVersionOli 联动 | 🔴 | **为什么🔴**: WHERE 条件注入 |
| P1-3 | VERSION_FUNCTION_MAP 新值生成 (8 类型) | 🔴 | **为什么🔴**: 版本演进 |
| P2-1 | 冲突语义 (影响行数 0 = 并发冲突) | 🟡 | **为什么🟡**: 业务面 |
| P2-2 | wrapperMode/异常自定义 | 🟡 | **为什么🟡**: 兼容面 |
| P3-1 | 与 spring-tx 并发控制对照 (引用) | 🟢 | **为什么🟢**: 方案对比 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **CAS 核心** | 🔴 | 机制 |
| B | **版本类型** | 🔴 | 策略面 |
| C | **兼容与冲突** | 🟡 | 边界 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | CAS 语义 | updateById 拦截: 实体旧版本值→`MP_OPTLOCK_VERSION_ORIGINAL` 参数→实体回写新值 → 注入 SQL 执行 `SET version=#{新值} ... AND version=#{MP_OPTLOCK_VERSION_ORIGINAL}`(getVersionOli) — **影响行数 0 即版本冲突**(并发方已改) | OptimisticLockerInnerInterceptor.java:118-156; TableFieldInfo.java:578-586 |
| q2 | 双路径 | methodName="update"(update(et, wrapper))→wrapper.apply(versionColumn+" = {0}", 旧值) 或新建 UpdateWrapper.eq; 其他(updateById)→MP_OPTLOCK_VERSION_ORIGINAL 参数通道 — wrapper 路径条件进 wrapper, updateById 路径条件进注入 SQL | OptimisticLockerInnerInterceptor.java:143-154 |
| q3 | 版本类型策略 | VERSION_FUNCTION_MAP **8 类**(L290-297): 数值 long/Long/int/Integer→+1; 时间 Date/Timestamp/LocalDateTime/Instant→取当前; **不支持类型返回原值**; getUpdatedVersionVal protected 可覆写自定义(L308-311) | OptimisticLockerInnerInterceptor.java:290-311 |
| q4 | 空值守卫 | 旧值为 null → 自定义 exception 抛或直接 return(不拦截) — 版本字段必须初始化 | OptimisticLockerInnerInterceptor.java:130-136 |
| q5 | wrapperMode | 无实体仅 wrapper 的 update(LambdaUpdateWrapper) → setVersionByWrapper 从 wrapper 条件里找版本 eq 值 — wrapper 模式兼容 | OptimisticLockerInnerInterceptor.java:158-163,198-203 |
| q6 | 执行链 | beforeUpdate(MP-4 回调)在 Executor.update 拦截 — 实体参数 map 修改后透传, 注入 SQL 的 SET/WHERE 已含版本逻辑(MP-1 生成) | MP-4 交付; MP-1 交付 |

→ 引出 MP-7: 自动填充 — MetaObjectHandler 是另一类"执行期参数处理", 与乐观锁同属 MP 的执行期注入族。
