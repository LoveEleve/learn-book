# C-5 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + harness 19/19 实证

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "processBackgroundResult 状态机" — NodeCache.java:250-274 GET_DATA/EXISTS 双分支逐字核对 | 通过 ✅ (harness A: 增改删均触发) |
| 2 | 事实 | §1 "Objects.equal 全量比较" — L280-300 | 通过 ✅ |
| 3 | 事实 | §2 "双 watcher 分工" — PathChildrenCache.java:96-117 | 通过 ✅ (harness B: 三事件全到) |
| 4 | 事实 | §2 "StartMode 3 模式" — L282-302 (**修正: 无 STRONG/ENDPOINT**) | 通过 ✅ (harness C/D 实证 BUILD_INITIAL_CACHE 与 POST_INITIALIZED_EVENT) |
| 5 | 事实 | §2 "mzxid 判定 CHILD_UPDATED" — L682-703 | 通过 ✅ (harness B: 留窗口后 CHILD_UPDATED 触发 — 时序竞态实证: setData 在 getDataAndStat 前到达则更新事件丢失, 数据仍一致) |
| 6 | 事实 | §2 "INITIALIZED 只发一次" — L713-730 getAndSet(null) | 通过 ✅ (harness D 实际 1 次) |
| 7 | 事实 | §3 "TreeNode 双链路" — TreeCache.java:227 | 通过 ✅ |
| 8 | 事实 | §3 "DEAD 级联删除" — L301-333 | 通过 ✅ (harness E: 删父级联子节点不可见) |
| 9 | 事实 | §3 "INITIALIZED = outstandingOps 归零" — L456-460 | 通过 ✅ |
| 10 | 事实 | §4 "PERSISTENT_RECURSIVE" — CuratorCacheImpl.java:82-87 + PersistentWatcher.java:147-152 | 通过 ✅ (harness F/G 实证行为) |
| 11 | 事实 | §4 "cversion 差分" — L172-198 | 通过 ✅ |
| 12 | 事实 | §4 "version 判定" — L231-245 (与旧类 mzxid 的差异) | 通过 ✅ |
| 13 | 事实 | §4 "SINGLE_NODE_CACHE 选项" — CuratorCache.java:54-59 | 通过 ✅ (harness G: 子节点变化零事件) |
| 14 | 事实 | §5 "bridge 回退" — CompatibleCuratorCacheBridge L38-60 | 通过 ✅ (harness H: 旧监听器桥接收到事件) |
| 15 | 过程 | **harness 实证发现 1**: 启动时 TreeCache/CuratorCache 对根节点自身发 NODE_ADDED/NODE_CREATED (初始构建) | 大纲可加注: 监听器启动后可能立即收到根节点创建事件 |
| 16 | 过程 | **harness 实证发现 2**: 快速 create+setData 会吞掉 CHILD_UPDATED (watcher 挂载竞态) — 数据仍最新, 事件可丢 | 大纲 §5 "事件可能丢" 的实证; 与官方免责声明一致 |
| 17 | 事实 | §1 "isConnected 门控" — NodeCache.java:68-84 | 通过 ✅ |

**结论**: 17 项核对 0 修正 (2 项 harness 实证行为补充: 根节点初始事件/watcher 挂载竞态吞事件)。harness 19/19 覆盖: A NodeCache B PathChildrenCache 事件 C BUILD_INITIAL D INITIALIZED E TreeCache 级联 F CuratorCache 事件 G SINGLE_NODE H bridge。

## harness 发现 (可写入文章):

1. **根节点初始事件**: TreeCache/CuratorCache start 后会对根节点发 NODE_ADDED/CREATED — 监听器要准备接收。
2. **watcher 挂载竞态**: 创建后立刻 setData, CHILD_UPDATED 可能不触发 (getData 已取到新值) — 缓存数据一致但事件丢失, 官方"尽力而为"免责的活例证。
3. **INITIALIZED 恰好 1 次实证**。
4. **SINGLE_NODE_CACHE 子节点零事件实证** — 递归开关真实生效。
