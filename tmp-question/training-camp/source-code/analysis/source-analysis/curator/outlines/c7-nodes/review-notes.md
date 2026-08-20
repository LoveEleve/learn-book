# C-7 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + harness 12/12 实证

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "三重触发" — PersistentNode.java:86-87 (watcher NodeDeleted→createNode), L130-137 (RECONNECTED), L95-113 (NONODE 回调) | 通过 ✅ (harness B: 外部删除后 <5s 自动重建 — watcher 触发实证) |
| 2 | 事实 | §1 "createNode 路径复用/剥后缀" — L397-445 | 通过 ✅ |
| 3 | 事实 | §1 "NODEEXISTS → setData 同步" — L264-265 | 通过 ✅ |
| 4 | 事实 | §1 "waitForInitialCreate" — L302-307 | 通过 ✅ (harness A) |
| 5 | 事实 | §1 "close 防孤儿" — L313-336, L222-237 | 通过 ✅ (harness D: close 后节点被删) |
| 6 | 事实 | §2 "touchTask 周期 = ttlMs/2" — PersistentTtlNode.java:201 scheduleAtFixedRate 首延迟/周期均为 ttlMs/factor | 通过 ✅ (harness E: 需等 2500ms 首触发生效) |
| 7 | 事实 | §2 "CONTAINER 父 + PERSISTENT_WITH_TTL 子" — L162-199 | 通过 ✅ |
| 8 | 事实 | **部署前置发现**: ZK 服务端默认禁用 extendedTypes — TTL 创建失败 (服务端线程报错) | **重要发现** ✅: 需 `-Dzookeeper.extendedTypesEnabled=true` 才能用 PersistentTtlNode; 大纲/文章应注明此部署前提 |
| 9 | 事实 | §3 "getCurrentMembers 返回 Map<String,byte[]>" — GroupMember.java:116 (非 List<GroupMember>) | 通过 ✅ (harness F: 含自己 + 他成员可见) |
| 10 | 事实 | §3 "自己强制入列" — L128-130 | 通过 ✅ |
| 11 | 过程 | 构造签名: PersistentTtlNode(client, path, ttlMs, initData) — path 为**父节点**路径, 子节点固定名 "touch" | 通过 ✅ (DEFAULT_CHILD_NODE_NAME="touch" L59) |

**结论**: 11 项核对 0 修正 (1 项部署前置发现: extendedTypesEnabled)。harness 12/12 覆盖: A 创建+初始化 B 自动重建 C setData D close 删除 E TTL 父子 F 组成员。

## harness 发现 (可写入文章):

1. **删除后自动重建 <5s 实证**: watcher 触发路径完整走通。
2. **TTL 首触碰发 = ttlMs/2 实证**: scheduleAtFixedRate 首延迟与周期相同。
3. **部署前提**: TTL 节点要求 ZK 服务端开启 extendedTypesEnabled — 生产排错要点。
4. **成员视图实证**: 含自己 + 他成员 (2 成员时 Map size=2)。
