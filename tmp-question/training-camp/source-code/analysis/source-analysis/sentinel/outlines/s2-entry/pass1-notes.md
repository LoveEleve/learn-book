# S-2 入口域 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-17 | 源码: Sentinel 1.8.9 (context 4 + 根目录入口类 11 + 附 2)
> 09 域级审计: SENTINEL-PLAN S-2 (🔴 A) — 断言 "ContextUtil 281 行 + 三种降级放行 + entry 14 重载" 已 grep 验证

## 入口展开 (Level-1~2, 已读源码)

### Level-1: 用户触达面 — SphU/SphO 两套 API

```
SphU(368 行): 20 个公开入口 = 12 个 entry + 6 个 asyncEntry + 2 个 entryWithPriority
  + entryWithPriority(name / name+type) (L262/277) — OccupyTimeout 抢占入口
  + entry(name, resourceType, trafficType[, args]) (L293/310) — resourceType 新 API (SphResourceTypeSupport)
  + asyncEntry(...) / asyncEntryWithType(...) — 异步入口
  → 全部最终转发 Env.sph.entry(...) / asyncEntryWithType(...) (SphU.java:85 实证)

SphO.entry(226 行): boolean 返回版 (无异常, try/finally 内 exit) — 面向"不想处理异常"的调用方
```

### Level-2: 核心执行 — CtSph.entryWithPriority (L117-151)

```
Context context = ContextUtil.getContext();
├── NullContext 实例 → 无链 CtEntry (context 超限降级) (L120-124)
├── null → InternalContextUtil.internalEnter(默认 context "sentinel_default_context") (L126-129)
├── !Constants.ON → 无链 CtEntry (全局开关关闭) (L132-134)
├── chain == null (6000 上限) → 无链 CtEntry (L138-141)
└── 正常: new CtEntry(resourceWrapper, chain, context, count, args)
      → chain.entry(...) (L146-149)
      → catch BlockException → e.exit(count) + throw (L150-153)
      → catch Throwable → RecordLog.info (不抛!) (L154-156)
```

### Level-2: 上下文 — ContextUtil (213 行, 非 281)

```
contextHolder = ThreadLocal<Context> (L50)
contextNameNodeMap = volatile Map<name, DefaultNode> (L55) — COW 与 chainMap 同模式
NULL_CONTEXT 单例 (L57)
trueEnter(name, origin) (L120-160):
├── ThreadLocal 已有 → 直接返回 (同线程复用)
├── 无 → contextNameNodeMap.get(name):
│   ├── 超 MAX_CONTEXT_NAME_SIZE (2000) → setNullContext() (L130-132, 双检锁内再查 L138)
│   └── 新建 EntranceNode + Constants.ROOT.addChild + COW 更新 map (L143-149)
├── new Context(node, name) + setOrigin + contextHolder.set (L152-155)
enter(name, origin) (L112-118): 拒绝 "sentinel_default_context" 作为自定义 context 名 (ContextNameDefineException)
```

### Level-2: entry 生命周期 — CtEntry (157 行)

```
parent/child 双向链 (L37/61-63): parent = context.getCurEntry(); parent.child = this
exit (L90-127): 校验 curEntry == this (不匹配 → ErrorEntryFreeException, L98-109)
  → context.setCurEntry(parent) → parent.child = null → parent == null 时清空 ThreadLocal (L123-125)
```

## 09 域级审计表 (SENTINEL-PLAN S-2 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "ContextUtil 281 行" | wc -l = **213** | **修正: 213** (执行计划过时) |
| "三种降级放行" | NullContext (L120) + !ON (L132) + chain==null (L138) | 接受 (S-1 曾归并为三路径) |
| "entry 14 重载" | **修正**: SphU 共 20 个公开入口 = 12 entry + 6 asyncEntry + 2 entryWithPriority | 执行计划过时 |
| "COW 与 chainMap 同模式" | contextNameNodeMap 同款重建 (L143-149) | 接受 (补锚: 上下文上限 2000) |
| "EntryType IN/OUT 决定统计方向" | NodeSelector/ClusterBuilder 消费 (待 S-2 展开) | 待验证 |
| 执行计划未提: SphO boolean API | SphO.java 226 行 | 补锚 |
| 执行计划未提: AsyncEntry 两阶段 | asyncEntryWithPriorityInternal (L64-110) | 补锚 |

## 标记问题 (8 个)

1. CtEntry.exit 的 curEntry 校验细节: ErrorEntryFreeException 触发条件?错误释放后 context 状态如何恢复?— CtEntry.java:90-127
2. InternalContextUtil.internalEnter 与 trueEnter 差异: 为什么要子类覆写 enter?默认 context 的 EntranceNode 是全局共享还是每线程?— CtSph.java:248-257
3. AsyncEntry 机制: 异步线程没有 ThreadLocal context, entry 后如何挂到异步调用链?exit 在哪触发?— AsyncEntry.java + CtSph.java:64-110
4. EntryType IN/OUT: 如何影响 NodeSelector/ClusterBuilder/统计?— EntryType.java:34 + node 包
5. SphO 实现: boolean 语义如何转译 entry/exit?与 SphU 差异仅 API 形状还是行为不同?— SphO.java
6. Context 的 curEntry 栈: entry 嵌套时 parent/child 如何维护?树结构还是栈?— Context.java
7. MAX_CONTEXT_NAME_SIZE = 2000 的定位: 与 MAX_SLOT_CHAIN_SIZE 6000 的关系?— Constants.java
8. Tracer/Exception 记录: 业务异常如何上报到统计节点?— Tracer.java
9. [追加] SphResourceTypeSupport: resourceType 参数如何从新 API 穿透到 ResourceWrapper?— SphU.java:293-310

## 待展开 (下一层)

1. Context.java 完整结构 (curEntry/name/origin/entranceNode)
2. CtEntry exit 的完整校验流程
3. AsyncEntry 两阶段 (asyncEntryWithPriorityInternal + cleanCurrentEntryInLocal)
4. SphO 的转译
5. EntryType 的统计方向消费 (NodeSelectorSlot 实现)