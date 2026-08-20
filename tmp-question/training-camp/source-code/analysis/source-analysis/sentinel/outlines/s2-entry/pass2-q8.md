# Pass 2 闭环笔记 Q8: Tracer — 业务异常如何挂到 entry

## 初始假设
- Tracer 把异常记录到当前 entry, 过滤逻辑简单。

## 验证过程
- 读 `Tracer.java:67-114` (traceContext/traceEntry): 三步 — `shouldTrace(e)` 过滤 → context 为 null/NullContext 跳过 → `traceEntryInternal → entry.setError(e)` (L105-113)。
- 读 `Tracer.java:201-225` (shouldTrace): 过滤优先级:
  1. null 或 BlockException → false(**被规则拦截不算业务异常**);
  2. 自定义 exceptionPredicate(全局谓词)优先;
  3. ignoreClasses(忽略清单, 子类匹配 isAssignableFrom);
  4. traceClasses(指定清单, 命中才记);
  5. 全未指定 → **return true(默认全记)**。
- 读 `Entry.java:67,170-175`: error 字段 + setError — 异常挂在 entry 上, 由 exit 路径消费(统计异常数, 属 S-5 StatisticSlot 的 exit 侧)。
- 传播链: 用户 catch → Tracer.trace(e) → curEntry.error → exit → 统计节点记 exception 数。

## 代码类型
- Glue(业务异常 → 统计面的桥)

## 跨域关联
- S-2 → S-5: entry.error 是 StatisticSlot 异常统计的输入
- S-2 → S-3: BlockException 不 trace 的语义与规则层一致

## 结论
Tracer 是业务异常进统计的桥: BlockException 与 null 永不 trace, 默认全记, 可经全局谓词/ignore/trace 清单定制(Tracer.java:201-225); 落点 = entry.error, exit 时消费(Tracer.java:105-113 + Entry.java:174)。