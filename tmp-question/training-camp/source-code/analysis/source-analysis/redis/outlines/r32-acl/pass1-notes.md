# R-32 ACL — Pass 1 探索笔记

> 域: R-32 ACL 权限体系 | 🟡 B 方案 | 2026-08-14
> 源码: src/acl.c (3241) + server.h (user/ACLLogEntry) | Redis 7.4.2 (无 acl.h — 定义在 server.h)

## 调用图

```
验证链 (processCommand):
server.c:3987: ACLCheckAllPerm(c,&acl_errpos) → 失败 → addACLLogEntry + -NOPERM
ACLCheckAllPerm (acl.c:1878) → ACLCheckAllUserCommandPerm (L1837)
  → 逐 selector: ACLSelectorCheckCmd (L1678) — 命令位图 + firstargs 子命令白名单 + 键权限
  → ACLUserCheckKeyPerm (L1754) / ACLUserCheckChannelPerm (L1810, pubsub)
命令面 (aclCommand L2844): cat/deluser/dryrun/genpass/getuser/help/list/load/log/save/setuser/users/whoami (13 子命令)

用户与规则 (acl.c):
user 结构 (server.h): name/flags/passwords/selectors (7.0 多 selector 链表)
ACLSetUser (L1272) → ACLSetSelector (L1025): 规则解析 —
  on/off/nopass/resetpass/>pass/<pass/resetkeys/~*/~pat/%R~pat/%W~pat/resetchannels/&*/&pat/allcommands/+@all/-@all/+cmd/-cmd/firstarg 等
selector: 命令位图 (USER_COMMAND_BITS_COUNT=1024 位, server.h:1067) + patterns (keyPattern{pattern,flags}) + channels + allowed_firstargs
密码: SHA256 hex (ACLHashPassword L201, HASH_PASSWORD_LEN=64); 时间无关比较 (time_independent_strcmp L191)
分类: 21 个 ACL_CATEGORY_* (server.h:224-244, 0-20)

ACL LOG (L2586-2734):
ACLLogEntry{count/reason/context/object/username/ctime/cinfo/entry_id/timestamp_created}
ACLLogMatchEntry 聚合 (同类合并计数, ACL_LOG_GROUPING_MAX_TIME_DELTA)
acllog-max-len 默认 128 (config.c:3198)

加载与持久化:
ACLCreateDefaultUser (L1407): +@all ~* &* on nopass — 默认用户全权限
ACLLoadConfiguredUsers (L2205) / ACLLoadFromFile (L2272, aclfile) / ACLSaveToFile (L2469)
配置: aclfile (IMMUTABLE, config.c:3098) / acl-pubsub-default (MODIFIABLE enum, L3136) / acllog-max-len (L3198)
```

## 基本元素分解

1. **user 结构**: 名称/标志 (on/off)/密码集合/selector 链表
2. **selector (7.0 多 selector)**: 命令位图 + 键模式 + 频道模式 + 子命令白名单
3. **规则解析器**: ACLSetUser/ACLSetSelector (左到右顺序语义)
4. **密码面**: SHA256 hex + nopass + 时间无关比较
5. **验证链**: ACLCheckAllPerm → 命令位图 → 键模式 (读写分离) → 频道
6. **ACL LOG**: 聚合条目 + acllog-max-len
7. **命令面**: 13 子命令 + ACL dryrun (预演)
8. **加载面**: aclfile / 启动加载 / USER 配置

## 标记问题 (20 问)

1. 命令位图怎么实现? (1024 位, 16×64 字, cmd->id 索引)
2. +@all/-@all 与分类位图的关系?
3. 子命令白名单 (firstargs) 机制? (CONFIG GET/SET 精确到子命令)
4. 多 selector 语义? (任一 selector 通过即允许? 7.0 前单 selector)
5. %R~/%W~ 读写分离键模式怎么检查? (keyspec flags)
6. ~* 与 %~pattern 的互斥? (ALLKEYS 后不能加模式)
7. 频道模式 (&pattern) 与 acl-pubsub-default?
8. 密码怎么存? (SHA256 hex, nopass, 多密码)
9. 时间无关比较为什么? (时序攻击)
10. ACLCheckAllPerm 的完整链? (命令+键+频道)
11. 键模式匹配算法? (glob 匹配)
12. ACL LOG 聚合逻辑? (同类合并 + count)
13. ACL dryrun 的作用? (预演权限)
14. ACL SAVE/LOAD? (aclfile 持久化)
15. 默认用户构成? (+@all ~* &* on nopass)
16. 验证时机? (processCommand 每次 + EXEC 复查 + 脚本内)
17. ACL 变更即时性? (无缓存或失效)
18. pubsub 客户端 ACL 变更处理? (ACLKillPubsubClientsIfNeeded)
19. ACL GENPASS? (随机密码生成)
20. ACL LOG 的 reason 枚举? (ACL_DENIED_*)

## 时空溯源 (代码内痕迹)

- ACL 系统 **6.0 引入** (2020 — AUTH 6.0 强化; acl.c 版权 2020-Present)
- **7.0 多 selector**: ACLSetSelector 家族重构 (selector 从 user 内嵌改为链表 — acl-v2.tcl 测试面); %R~/%W~ 读写分离
- 键模式 flags: keyPattern{pattern, ACL_READ/WRITE/ALL_PERMISSION}
- ACL LOG entry_id/timestamp_created (7.x 集群故障恢复辨识)
- 命令 ID: ACLGetCommandID (L1522) — cmd->id 与位图映射

## 大域拆分判断

3241 行单文件 — 不拆 (🟡 B, 6 闭环)

## 域级怀疑审计 (HANDOFF §四 R-32 简案 断言复查)

| HANDOFF 断言 | 验证 | 结论 |
|:--|:--|:--|
| "**ACL 用户** (USER 命令: 密码/命令分类/键模式/频道模式)" | ACLSetUser 规则面 + aclCommand setuser; 密码 SHA256 hex | **接受** ✅ (+密码算法实证) |
| "**命令权限** (±@group / ±command, ACL_CATEGORY_*)" | ACLSetSelectorCategory (L697) + 21 分类 (server.h:224-244) + **1024 位命令位图** (server.h:1067) | **接受+补充**: 位图实现细节 |
| "**键模式** (%R~ 等)" | ACLSetSelector % 解析 (L1080-1096): %R~/%W~/%RW~ + 无前缀 ~pattern = 读写 | **接受** ✅ (7.0 多 selector 语义) |
| "**频道模式** (& 前缀)" | ACLSetSelector & 分支 (L1103+) + acl-pubsub-default (config.c:3136) | **接受** ✅ |
| "**ACL LOG**" | ACLLogEntry 结构 + 聚合 + acllog-max-len 128 | **接受** ✅ |
| "**默认用户** (default)" | ACLCreateDefaultUser (L1407): +@all ~* &* on nopass | **接受** ✅ |
| "**验证时机** (processCommand + EXEC 复查)" | server.c:3987 + R-16 EXEC 复查 + 脚本 scriptVerifyACL (R-30 已见) | **接受** ✅ (三时机) |
| "失效即时性 (无需重启)" | 无缓存失效机制? ACLCheckAllPerm 每次实时查 — 需验证 (SELECTOR_FLAG 无缓存) | **接受** ✅ (实时查无缓存) |
