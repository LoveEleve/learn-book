# Pass 2 闭环笔记 Q2: ClusterBuilderSlot 为什么必须是原型槽

## 初始假设
- `ClusterBuilderSlot` 只是全局 clusterNodeMap 的门面，做成单例也没问题。

## 验证过程
- `ClusterBuilderSlot` 标了 `@Spi(isSingleton = false)`，是原型槽 (`ClusterBuilderSlot.java:49`)。
- 槽里有两层状态：
  - **静态全局层**：`clusterNodeMap`，key=`ResourceWrapper`，value=`ClusterNode`，COW + 双检锁维护 (`ClusterBuilderSlot.java:66-85`)。
  - **实例局部层**：`volatile ClusterNode clusterNode`，这是“当前这条链对应资源”的缓存 (`ClusterBuilderSlot.java:70`)。
- entry 时先看实例字段 `clusterNode == null`，没有则从全局层创建并写回实例字段；随后 `node.setClusterNode(clusterNode)` 把本链里的 `DefaultNode` 绑定到这个全局 `ClusterNode` (`ClusterBuilderSlot.java:75-88`)。
- 如果这个槽是单例，会出现致命串线：
  - 单例实例只持有一个 `clusterNode` 字段；
  - 第一条资源链初始化后，这个字段会固定指向资源 A 的 `ClusterNode`；
  - 后续资源 B 再进来，就会把自己的 `DefaultNode` 错绑到资源 A 的 `ClusterNode`。
- 正因为每条链对应一个槽实例，实例字段 `clusterNode` 才能安全地承担“本链资源缓存”角色；全局共享则交给静态 `clusterNodeMap`。

## 代码类型
- Implementation(实例缓存 + 全局共享的二层结构)

## 结论
`ClusterBuilderSlot` 必须是原型，不是为了 `clusterNodeMap`，而是为了那个实例字段 `clusterNode`：每条资源链都要有自己的“局部指针”，再共同指向全局唯一的 `ClusterNode`。