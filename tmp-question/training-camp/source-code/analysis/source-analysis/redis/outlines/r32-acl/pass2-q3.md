# 闭环笔记 q3: 验证链 — ACLCheckAllPerm 与多 selector OR 语义

## 假设
验证 = processCommand 实时查; 多 selector 任一通过即允许; 键检查按 keyspec 权限映射。

## 验证过程
- **调用点** (server.c:3987): processCommand 中 ACLCheckAllPerm → 失败 → **addACLLogEntry + -NOPERM** (L3989-3997, MULTI 上下文 ctx 标记)
- **ACLCheckAllPerm** (L1878) → **ACLCheckAllUserCommandPerm** (L1837):
  - u==NULL → ACL_OK (L1845-1846)
  - **多 selector OR 语义** (L1856-1872): 逐 selector 检查, 任一 ACL_OK → 通过; 全失败 → 选**最高错误级别** (relevant_error) + 最大 idx (错误定位精确化)
  - **key 结果缓存** (L1854-1855 + initACLKeyResultCache L1662): 多 selector 间共享 getKeys 结果 — 防重复提取
- **ACLSelectorCheckCmd** (L1678):
  - CMD_NO_AUTH 豁免 (L1681): AUTH/HELLO 等始终可执行
  - **命令位图** (L1682-1696): 位=0 → **firstargs 子命令白名单** (L1686-1696: allowed_firstargs[id] 数组, argv[idx] 匹配; idx = 有 parent 命令 (子命令) ? 2 : 1) → 无匹配 → ACL_DENIED_CMD
  - **键检查** (L1700-1710): 非 ALLKEYS 且 doesCommandHaveKeys → getKeysFromCommandWithSpecs (GET_KEYSPEC_DEFAULT) → ACLSelectorCheckKey
- **ACLSelectorCheckKey** (L1571-1598):
  - ALLKEYS → OK
  - **keyspec 权限映射** (L1579-1584): CMD_KEY_ACCESS → READ; INSERT/DELETE/UPDATE → WRITE
  - 模式过滤 (L1591-1593): `(pattern->flags & key_flags) == key_flags` — **模式权限必须覆盖所需** (只读模式不能过写命令)
  - glob 匹配: stringmatchlen (L1594, nocase=0)
- **ACLUserCheckChannelPerm** (L1810): 频道检查 (pubsub — R-29 交叉)
- **验证时机三处**: processCommand 每次 (server.c:3987) + EXEC 复查 (R-16 已见) + 脚本内 scriptVerifyACL (R-30 已见)

## 代码类型
Algorithmic (权限判定链)

## 跨域关联
- R-16 (multi): EXEC 复查
- R-30 (lua): scriptVerifyACL
- R-29 (pubsub): 频道权限
- R-20 (server): key specs (getKeysFromCommandWithSpecs)

## 结论
验证 = 实时无缓存: 命令位图 → 子命令白名单 → 键权限 (keyspec→R/W 映射 + 模式权限覆盖) → 频道; 多 selector OR 语义 + key 结果缓存; 错误定位选最高级别。
源码位置: acl.c:1571-1598,1678-1710,1837-1884; server.c:3987-3997
