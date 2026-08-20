# C-7 持久节点与组成员 — 知识规划 (KP)

> 域级: 🟡 B | 模块: recipes/nodes/ (5 文件: PersistentNode 509 / PersistentTtlNode 257 / GroupMember 143 / PersistentNodeListener 33 / PersistentEphemeralNode 120 废弃)
> 日期: 2026-08-15 | 版本: 5.8.0

## 一、机制提取 (逐源)

### M1 PersistentNode: 跨会话保活节点 (509)
- **三重触发重建 createNode()**:
  1. watcher: checkExists 挂 watch (L470-482), NodeDeleted → createNode (L86-87), NodeDataChanged → 重挂 (L88-90)
  2. 连接状态: RECONNECTED + active → createNode (L130-137)
  3. checkExists 回调: NONODE → createNode (L95-113)
- **createNode** (L397-445): 已有路径复用 (useProtection=false L412-414); sequential 模式剥后缀重建 (L415-416); getCreateMode(pathIsSet) 降级 *SEQUENTIAL (L447-468)
- 背景回调 (L239-273): NODEEXISTS (他人抢先) → setData 同步数据 (L264-265); OK → set nodePath + watchNode + 通知 listener (L259-269); NOAUTH → authFailure (L247-250); NONODE → parentCreationFailure (L251-257)
- **initialCreateLatch** (L64-65) + waitForInitialCreate (L302-307) — 调用方阻塞等首次创建成功
- **close 防孤儿** (L313-336): 删除节点 (guaranteed, L386-395); close 后迟到回调 processBackgroundCallbackClosedState → guaranteed 删残留 (L222-237)
- setData 前置校验 (L365-375)

### M2 PersistentTtlNode: CONTAINER + TTL 心跳 (257)
- 父节点 CONTAINER + deleteNode 覆写 NOP (L162-167) — 关闭不删父
- **touchTask** 定时 setData 子节点 (L178-199): 每 ttlMs/touchScheduleFactor (默认 factor=2, L60/L200-202); NoNode → create().orSetData().withTtl(PERSISTENT_WITH_TTL) 重建 (L183-190)
- 崩溃 → 停止 touch → TTL 到期 ZK 删子 → CONTAINER 父回收 (类注释 L38-57)
- **父子分层**: keep-alive 只写子节点, 不给父节点产生 watch 事件 (类注释 L51-53)

### M3 GroupMember: 组成员管理 (143)
- 自身成员节点 = PersistentNode(EPHEMERAL, membershipPath/thisId, payload) (L68-71); 成员视图 = **CuratorCache.bridgeBuilder** (L68) — 复用 C-5
- getCurrentMembers (L116-132): 缓存 stream 过滤直接子节点; **自己永远在结果里** (强制 thisId, L128-130 — EPHEMERAL 刚创建/重建期间)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M1 三触发重建 | P1 | 跨会话保活核心; 崩溃恢复语义 |
| M2 TTL 心跳 | P2 | CONTAINER+TTL 组合创新 |
| M3 GroupMember | P2 | 缓存复用范例 |

## 三、负面空间

- **不做节点数据 CAS**: setData 无版本校验 (写入最新值覆盖)
- **不做会话感知重放**: 重建时机依赖 watcher/事件, 分区期间节点可能长时间缺失
- **不做 leader 协调**: 多实例同时建同名节点靠 NODEEXISTS 抢占
- **不做数据版本历史**: 覆盖即丢
