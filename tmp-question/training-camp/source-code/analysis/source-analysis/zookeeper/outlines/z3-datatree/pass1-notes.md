# Z-3 DataTree — Pass 1 探索笔记

> 域: Z-3 DataTree | 🔴 A 方案 | 2026-08-15
> 源码: DataTree (1971) + DataNode (待查行数) + NodeHashMapImpl + ReferenceCountedACLCache + ZKDatabase (806) + PathTrie + StatPersisted | ZooKeeper 3.9.5

## 调用图

```
写路径 (Z-4 交叉): PrepRequestProcessor → 事务 → Leader 广播 (Z-2) → COMMIT
  → FinalRequestProcessor → DataTree.processTxn (OpCode 分发)
      → createNode/deleteNode/setData/setACL
          → synchronized(parent) + pre/postChange (digest) + 分类维护 + watch 触发

读路径: getNode (O(1)) / getChildren / getData

快照面: serializeAcls + serializeNodes (DFS, "/" 结束)
  ↔ deserialize (父链重建 + 分类恢复 + acl addUsage)
ZKDatabase: dataTree + committedLog (ArrayDeque) + snapLog (Z-9)
  loadDataBase → snapLog.restore
```

## 基本元素分解

1. **树结构**: NodeHashMap (扁平) + DataNode (data/stat/children/acl) + 分类集合 (ephemerals/containers/ttls)
2. **写操作**: createNode/deleteNode/setData — 父锁 + digest + 分类 + watch 四步
3. **事务应用**: processTxn OpCode 分发 + multi 子事务 + TxnDigest
4. **快照面**: serialize/deserialize + ZKDatabase (committedLog/snapLog)

## 标记问题 (20 问)

1. NodeHashMap 结构? (扁平 HashMap)
2. DataNode 字段? (data/stat/children/acl)
3. pre/postChange? (digest 钩子)
4. ephemerals 结构? (sessionId → paths)
5. ACL 缓存? (引用计数)
6. createNode 校验? (父存在/不重复)
7. cversion/pzxid 保护? (单调)
8. deleteNode 顺序? (removeChild → remove → 分类清理)
9. setData 版本? (乐观锁)
10. watch 触发? (data/child 双管理器)
11. processTxn 分发? (OpCode switch)
12. multi 原子性? (子事务)
13. TxnDigest? (3.5+ 校验)
14. serialize 格式? (DFS + "/" 结束)
15. deserialize 重建? (父链 + 分类)
16. ZKDatabase 角色? (committedLog + snapLog)
17. loadDataBase? (snapLog.restore)
18. PathTrie? (quota 前缀索引)
19. nodeDataSize? (原子缓存)
20. 系统节点? (/zookeeper proc/quota/config)

## 时空溯源 (代码内痕迹)

- 3.4.x: 基础树 + createNode/deleteNode/setData 骨架
- 3.5.x: **tree digest (NodeHashMapImpl + pre/postChange + DIGEST_LOG)** — 一致性校验
- 3.6.x: **containers/ttls 分类** (EphemeralType) + createContainer/createTTL
- 3.9.x: fuzzy snapshot 注释 (L446-457) — 快照一致性增强

## 大域拆分判断

Z-3 = DataTree 单文件域 (1971) + DataNode + NodeHashMapImpl + ZKDatabase 引用面; 单篇 🔴 A (8 闭环 q1-q4 + 验证); watch 细节归 Z-6

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划) | 验证 | 结论 |
|:--|:--|:--|
| "DataTree NodeHashMap(ConcurrentHashMap)/DataNode(stat)/ZKDatabase(内存+txnlog+snapshot)" | 扁平 NodeHashMap + DataNode stat + ZKDatabase (snapLog + committedLog) | **接受+精确化**: NodeHashMapImpl 非纯 ConcurrentHashMap (带 digest) | ✅ |
| 数字: STAT_OVERHEAD_BYTES | (6×8)+(5×4)=68B | **补充** ✅ |
| 数字: DIGEST_LOG | LIMIT=1024 / INTERVAL=128 | **补充** ✅ |
| 数字: 系统节点 | /zookeeper + proc/quota/config | **补充** ✅ |
