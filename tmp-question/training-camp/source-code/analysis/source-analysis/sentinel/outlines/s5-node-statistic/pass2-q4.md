# Pass 2 闭环笔记 Q4: EntranceNode / DefaultNode / ClusterNode 三者各代表什么视角

## 初始假设
- 三种节点只是名字不同，统计职责差不多。

## 验证过程
- `DefaultNode` = “某个 resource 在某个 context 下的局部节点”，它继承 `StatisticNode`，还持有：
  - `ResourceWrapper id`
  - `childList`
  - `ClusterNode clusterNode` (`DefaultNode.java:38-51`)
- `ClusterNode` = “某个 resource 全局汇总节点”，同名资源跨 context 共享；还额外持有 `originCountMap`，按 origin 再拆子统计 (`ClusterNode.java:33-40, 58-104`)。
- `EntranceNode` 继承 `DefaultNode`，但它不是资源节点，而是“某个 context 的入口根节点”；它重写了 `avgRt/blockQps/curThreadNum/passQps/...`，全部通过遍历 childList 聚合子节点 (`EntranceNode.java:40-97`)。
- 关系可以简化成：
  - `EntranceNode` 看“这棵入口树整体怎么样”
  - `DefaultNode` 看“这棵树里某个资源节点怎么样”
  - `ClusterNode` 看“这个资源在全局所有树里总共怎么样”

## 代码类型
- Data Model(三层统计视角)

## 结论
`EntranceNode` 是入口树根，`DefaultNode` 是 context 内局部资源节点，`ClusterNode` 是跨 context 的全局资源汇总；三者不是同层替身，而是三种观察坐标。