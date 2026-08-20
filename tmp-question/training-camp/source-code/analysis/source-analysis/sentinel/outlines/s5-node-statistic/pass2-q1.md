# Pass 2 闭环笔记 Q1: NodeSelectorSlot 如何把同一资源在不同 context 下拆成不同 DefaultNode

## 初始假设
- 同一资源进入同一条 ProcessorSlotChain，所以节点也只有一个。

## 验证过程
- `NodeSelectorSlot` 是原型槽，内部有实例级 `map<String, DefaultNode>`，key 不是资源名，而是 `context.getName()` (`NodeSelectorSlot.java:130-157`)。
- 注释已明说：同一资源会共享同一条 `ProcessorSlotChain`，因此进入这个槽时资源名天然相同，真正需要区分的是 context 名 (`NodeSelectorSlot.java:134-149`)。
- 首次命中某个 context 名时：
  1. `new DefaultNode(resourceWrapper, null)`
  2. COW 复制 `map`
  3. `((DefaultNode) context.getLastNode()).addChild(node)` 把它挂到当前调用树父节点下面 (`NodeSelectorSlot.java:151-156`)
- 然后 `context.setCurNode(node)`，把这个 `DefaultNode` 设为当前上下文节点，再继续 `fireEntry(...)` (`NodeSelectorSlot.java:161-162`)。
- 结果：同一资源在 `entrance1` 和 `entrance2` 两个 context 下，会各自生成一个 `DefaultNode`，但仍共用同一条链；这是“同链、多树节点”的设计。

## 代码类型
- Implementation(调用树分叉点)

## 结论
`NodeSelectorSlot` 用 `context name` 而不是 `resource name` 做 key，把“同一资源在不同入口树下的局部统计”拆成多个 `DefaultNode`；链是全局共享的，树是按 context 分叉的。