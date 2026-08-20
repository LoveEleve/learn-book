# Pass 2 闭环笔记 Q6: Context — curEntry 栈与节点访问链

## 初始假设
- Context 维护完整 entry 栈(多条引用)。

## 验证过程
- 读 `Context.java:57-97` (字段): name/entranceNode/curEntry/origin/async 五字段 — **只有一个 curEntry 指针**, 无栈结构。
- 读 `Context.java:111-127` (getCurNode/setCurNode): 直接转 `curEntry.getCurNode()` — 当前节点 = 当前 entry 的节点。
- 读 `Context.java:170-181` (getLastNode): `curEntry.getLastNode()`(即 parent.getCurNode())或退化到 entranceNode — "链上最近经过的节点"。
- 调用栈实为 **CtEntry 的 parent 单向链**(C2 已证: parent/child 双向, 入口见 CtEntry.java:61-63), Context 只持栈顶。
- 树状嵌套: child 指针构成"当前栈路径"; entry 树可沿 parent 回溯, 但 Context 语义是线性栈(curEntry 单指针)。

## 代码类型
- Implementation(单指针 + 链表 = 隐式栈)

## 跨域关联
- S-2 → S-5: getLastNode/getCurNode 是统计节点定位 API(StatisticSlot 消费)
- S-2 → S-1: entranceNode 即 S-1 的 EntranceNode(树根挂载点)

## 结论
Context 不存栈, 只存 curEntry 栈顶指针 + entranceNode + origin(Context.java:62-79); 调用栈 = CtEntry.parent 链。节点访问 getCurNode(当前)/getLastNode(父级或入口节点)都沿 curEntry 链取值。