# Pass 2 闭环笔记 Q1: CtEntry.exit — curEntry 校验、错误释放、调用栈修复

## 初始假设
- exit 只是把 curEntry 退栈;释放顺序错乱时抛异常但栈可能残留。

## 验证过程
- 读 `CtEntry.java:90-127` (exitForContext):
  - NullContext → 直接 return(无清理)(L92-94)。
  - `context.getCurEntry() != this` → **错误释放**: 先 `while (e != null) { e.exit(count); e = (CtEntry) e.parent; }` 把 curEntry 到栈顶**全部 exit 扯平**(L98-106),再抛 ErrorEntryFreeException(L107-109)——**先修复调用栈,再报错**。
  - 正常路径: chain.exit(槽链 exit 方向遍历)→ callExitHandlersAndCleanUp(whenTerminate 注册的回调)(L111-115)→ context.setCurEntry(parent) + parent.child=null(L117-122)→ parent==null 且默认 context → ContextUtil.exit() 自动清 ThreadLocal(L123-125)→ clearEntryContext() 置 context=null 防重复 exit(L126)。
- 读 `CtEntry.java:139-143` (trueExit): 返回 parent — 链式退出。
- 读 `CtEntry.java:147-149` (getLastNode): parent==null ? null : parent.getCurNode() — 嵌套时取父级当前节点。
- 错误恢复语义: 释放顺序错乱**不会污染后续调用** — 残留 entry 被强制 exit,栈被拉平,异常只用于通知调用方。

## 代码类型
- Implementation(调用栈状态机 + 自愈)

## 跨域关联
- S-2 → S-1: chain.exit 沿链反向遍历(槽的 exit 方向, S-1 的 fireExit 直传)
- S-2 → S-5 统计: StatisticSlot 的 exit 侧算 RT 正是经这条链(槽 exit 后置语义的载体)

## 结论
exit 分三态: NullContext 免清理 / 错序先扯平全栈再抛 ErrorEntryFreeException / 正常退栈 + 默认 context 自动退出 + 防重复 exit 置 null(CtEntry.java:90-127)。entry 与 exit 的配对性由 context.curEntry 校验强制,释放错乱是自愈型错误而非崩溃。