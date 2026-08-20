# 一棵树,一个汇总节点

> S-5 上篇。本文只讲节点树怎么长出来，以及为什么同一资源既要有 `DefaultNode`，又要有 `ClusterNode`。

## 悬念

同一个资源 `getUser`，为什么 Sentinel 要同时给它建两种节点：树上的 `DefaultNode`，以及全局的 `ClusterNode`？这不是重复建模，而是故意把“局部路径”与“全局汇总”拆开。

## 一、NodeSelectorSlot: 先长出一棵调用树

`NodeSelectorSlot` 是调用树真正开始生长的地方。它是原型槽，内部有一张实例级 map：

```java
@Spi(isSingleton = false, order = Constants.ORDER_NODE_SELECTOR_SLOT)
public class NodeSelectorSlot extends AbstractLinkedProcessorSlot<Object> {
    private volatile Map<String, DefaultNode> map = new HashMap<String, DefaultNode>(10);
}
```

这张 map 的 key 不是资源名，而是 `context.getName()` (`NodeSelectorSlot.java:133-167`)。原因正好反过来：走到这一步时，同一条 `ProcessorSlotChain` 已经保证资源名相同；真正会变化的是 context 名。

所以 `entry(...)` 做的是：按 context 名查 `DefaultNode`，没有就新建，然后挂到 `context.getLastNode()` 下面 (`NodeSelectorSlot.java:151-167`)。这一步等价于说：

- 同一资源,不同入口树 → 各自长一个 `DefaultNode`
- 同一入口树,同一路径 → 复用已有 `DefaultNode`

这就是 Sentinel 的第一层视角：**树内局部节点**。

## 二、为什么不是一张全局树

如果只用资源名做 key，那么 `entrance-a/getUser` 与 `entrance-b/getUser` 会被合并成一个节点，入口树就塌了。Sentinel 明确不这么做。

`NodeSelectorSlot` 的注释把语义写得很直白：同一资源在不同 context 下会生成多个 `DefaultNode`，但只共享一个 `ClusterNode` (`NodeSelectorSlot.java:69-111`)。

所以这里实际上拆开了两件事：

- 树回答“这次调用是从哪条路径进来的”
- 汇总回答“这个资源整体跑得怎么样”

## 三、ClusterBuilderSlot: 再绑上一个全局汇总节点

`ClusterBuilderSlot` 是第二层。它也是原型槽，但内部同时持有：

- 静态全局 `clusterNodeMap`
- 实例级 `clusterNode` 指针 (`ClusterBuilderSlot.java:70-74`)

entry 时它先确保当前链实例拿到自己的 `clusterNode`，再执行：

```java
node.setClusterNode(clusterNode);
```

这里的 `node` 是上一个槽刚建/拿到的 `DefaultNode`。这句绑定之后，树上的局部节点就和全局汇总节点接上了。

为什么 `ClusterBuilderSlot` 必须是原型？因为实例字段 `clusterNode` 承载的是“**这条链的资源缓存**”。如果它是单例，不同资源链会共用一个字段，后来的资源节点就会错绑到前一个资源的 `ClusterNode` 上。原型槽把“每条链一个局部指针”和“全局一张共享 map”同时成立了。

## 四、三层节点不是替身,是三种视角

三者职责其实非常清楚：

- `EntranceNode`：某个 context 的入口根节点
- `DefaultNode`：某个 resource 在某个 context 下的局部节点
- `ClusterNode`：某个 resource 跨所有 context 的全局汇总节点

`EntranceNode` 甚至直接重写了 `avgRt/passQps/blockQps/...`，全部通过遍历 childList 来聚合 (`EntranceNode.java:40-97`)。它不持有“自己的独立全局统计语义”，它就是一棵入口树的观察窗。

而 `ClusterNode` 则相反：它不关心树结构，只关心“同名资源的总统计”，并且还能按 `origin` 再拆出 `originCountMap` (`ClusterNode.java:58-104`)。

## 五、DefaultNode 的真正作用: 双写代理层

`DefaultNode` 最关键的地方不是 `childList`，而是它把统计写操作全部覆写了一遍：

- `addPassRequest`
- `increaseBlockQps`
- `increaseExceptionQps`
- `addRtAndSuccess`
- `increaseThreadNum`
- `decreaseThreadNum` (`DefaultNode.java:92-123`)

这些方法的形状都一样：先 `super.xxx(...)` 写自己，再 `clusterNode.xxx(...)` 写全局节点。

这意味着 `StatisticSlot` 根本不需要知道 `ClusterNode` 的存在。它只要对当前 `DefaultNode` 写统计，`DefaultNode` 自己就会把数据透传到全局汇总层。

这是一种很“老派但诚实”的设计：

- 树结构在 `NodeSelectorSlot`
- 汇总绑定在 `ClusterBuilderSlot`
- 双写透传藏在 `DefaultNode`

每一层各干各的，不互相污染。

## 悬念回收

为什么一个资源要有两种节点？因为 Sentinel 要同时回答两个问题：

- 在某条入口路径里，这个资源节点现在怎样？→ `DefaultNode`
- 不管从哪条入口树进来，这个资源整体怎样？→ `ClusterNode`

而 `EntranceNode` 则是第三个坐标：这整棵入口树现在怎样。

## 锚点

- `NodeSelectorSlot.java:127`
- `NodeSelectorSlot.java:133`
- `NodeSelectorSlot.java:151`
- `ClusterBuilderSlot.java:49`
- `ClusterBuilderSlot.java:70`
- `ClusterBuilderSlot.java:93`
- `DefaultNode.java:110`
- `EntranceNode.java:46`
- `ClusterNode.java:68`
