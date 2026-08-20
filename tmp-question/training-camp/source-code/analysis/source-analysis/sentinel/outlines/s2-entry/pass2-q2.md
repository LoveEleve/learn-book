# Pass 2 闭环笔记 Q2: InternalContextUtil — 默认 context 的自动进入

## 初始假设
- 默认 context 只是一个特例名字,内部复用 public enter。

## 验证过程
- 读 `CtSph.java:248-254` (InternalContextUtil): `internalEnter(name) → trueEnter(name, "")` — **直接调 trueEnter,跳过 public enter 的名字校验**(ContextUtil.java:112-118 拒绝把默认名当自定义名)。
- 内部调用点仅两处: `CtSph.java:74` (asyncEntryWithPriorityInternal) 和 `CtSph.java:128` (entryWithPriority) — 都是 `context == null`(用户未显式 enter)时自动进入。
- 默认名: `Constants.java:40` CONTEXT_DEFAULT_NAME = "sentinel_default_context"。
- `isDefaultContext` (ContextUtil.java:224-228): 按名字判断 — CtEntry.exit 用它决定 parent==null 时是否自动 ContextUtil.exit()(CtEntry.java:123-125)。
- 语义: 用户不调 ContextUtil.enter → 全部 entry 落入全局默认 context;默认 context 的 EntranceNode 首次创建后全局共享;退出时自动清 ThreadLocal。

## 代码类型
- Glue(默认路径的静默注入)

## 跨域关联
- S-2 → S-1: 默认 context 的 EntranceNode 也是 NodeSelectorSlot map 的 key 之一(context 名做 key)
- S-2 → S-5: 默认 context 下所有资源共享一个入口节点统计

## 结论
InternalContextUtil 的存在价值 = 绕过 "默认名不可自定义" 的校验:框架内部用 internalEnter 自动进入默认 context,用户显式 enter 才需要合法名字(CtSph.java:248-254, 128 + ContextUtil.java:112-118, 224-228)。默认 context 的 ThreadLocal 清理由 CtEntry.exit 联动自动完成。