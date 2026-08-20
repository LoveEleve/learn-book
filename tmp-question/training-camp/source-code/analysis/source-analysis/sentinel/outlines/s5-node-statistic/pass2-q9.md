# Pass 2 闭环笔记 Q9: NodeBuilder 还活着吗

## 初始假设
- `NodeBuilder` 是 NodeSelector/ClusterBuilder 背后的正式构建扩展点。

## 验证过程
- `NodeBuilder` 自身已标 `@Deprecated`，接口只声明 `buildTreeNode` 和 `buildClusterNode` (`NodeBuilder.java:21-37`)。
- 在 main 代码里 grep `NodeBuilder`，除了接口自身外，没有任何生产代码消费者；测试里只剩 `ClusterNodeBuilderTest` 提及。
- 现行 1.8.9 的真实构建路径已经内联在两个槽里：
  - `NodeSelectorSlot` 直接 `new DefaultNode(...)`
  - `ClusterBuilderSlot` 直接 `new ClusterNode(...)`
- 说明它已经是历史遗留扩展点，不再参与主链。

## 代码类型
- Dead/Legacy(遗留接口)

## 结论
`NodeBuilder` 在 1.8.9 基本已经退场：主链不再调用，只剩接口壳与测试痕迹。S-5 正文应把它当“遗留接口”一笔带过，而不是主线。