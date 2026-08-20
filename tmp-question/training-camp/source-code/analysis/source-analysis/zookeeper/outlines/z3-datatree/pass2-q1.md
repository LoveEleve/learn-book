# 闭环笔记 q1: 树结构 — NodeHashMap + DataNode + 分类集合

## 假设
扁平 HashMap 路径 → 节点; DataNode 持有 data/stat/children/acl; 分类集合辅助清扫。

## 验证过程
- **NodeHashMap** (DataTree:105): `nodes` — 扁平路径 key → DataNode; "tree is the source of truth" 注释 (L102-104)
- **NodeHashMapImpl** (NodeHashMapImpl.java:31-120): **preChange/postChange → digestCalculator.calculateDigest** — **tree digest 增量校验**; DIGEST_LOG_LIMIT=**1024** / DIGEST_LOG_INTERVAL=**128** (DataTree:169-173)
- **DataNode** (DataNode.java:40-88): data (volatile byte[]) + **StatPersisted stat** (czxid/mzxid/pzxid/cversion/version/ephemeralOwner/dataLength) + children (HashSet) + acl (Long 引用 id)
- **分类集合** (DataTree:154-166): **ephemerals (sessionId → HashSet<path>, ConcurrentHashMap)** / containers / ttls (ConcurrentHashMap 包 Set) + **ReferenceCountedACLCache** (aclCache)
- **系统节点** (L115-139): rootZookeeper="/" + procZookeeper/quotaZookeeper/configZookeeper (三子树) + **PathTrie** (quota 前缀)
- **统计**: nodeDataSize (AtomicLong, L112) — 增量维护 vs approximateDataSize (全遍历, L240-249)

## 代码类型
Data Structure (扁平树 + 分类索引)

## 跨域关联
- Z-5 (Session): ephemerals 会话清扫面
- Z-6 (Watcher): dataWatches/childWatches 分离
- Z-9 (持久化): digest 与快照校验

## 结论
树 = 扁平 NodeHashMap (路径 key) + DataNode (data/stat/children/acl) + digest 增量校验 + 分类集合 (ephemeral 清扫/ACL 缓存)。
源码位置: DataTree.java:95-190,215-259; DataNode.java:40-88; NodeHashMapImpl.java:31-120
