# 闭环笔记 q1: user 结构与命令位图 — 1024 位权限矩阵

## 假设
user 含标志/密码/selector 链表; 命令权限 = cmd->id 索引的 1024 位位图。

## 验证过程
- **user 结构** (server.h 内 user 定义): name/flags (USER_FLAG_*: ON/OFF/NOPASS/DISABLED/SANITIZE_*)/passwords (list)/selectors (7.0 多 selector 链表)
- **命令位图** (server.h:1067-1070): `USER_COMMAND_BITS_COUNT 1024` — "The total number of command bits"; aclSelector->allowed_commands 数组 (16×64 位字)
- **位图索引** (ACLGetCommandBitCoordinates L519): cmd->id → word/bitshift; ACLGetSelectorCommandBit (L533) / ACLSetSelectorCommandBit (L551)
- **ALLCOMMANDS 标志** (L1043-1050): +@all → memset(255) + SELECTOR_FLAG_ALLCOMMANDS + 清 command_rules — 全开位图
- **命令 ID 映射** (ACLGetCommandID L1522): 命令名 → id (注册顺序); ACLClearCommandID 重置
- **分类位图**: 21 个 ACL_CATEGORY_* (server.h:224-244, 1<<0..1<<20) — keyspace/read/write/set/sortedset/list/hash/string/bitmap/hll/geo/stream/pubsub/admin/fast/slow/blocking/dangerous/connection/transaction/scripting
- **selector 默认零权限** (acl-v2.tcl "Test ACL selectors by default have no permissions"): 新 selector 全 0 位图

## 代码类型
Implementation (权限矩阵)

## 跨域关联
- R-20 (server): cmd->id / 命令注册序

## 结论
权限 = 1024 位命令位图 (cmd->id 索引) × 多 selector; 21 分类 (1<<0..1<<20); +@all 全开位图; selector 默认零权限。
源码位置: server.h:224-244,1067-1070; acl.c:519-566,1522-1550
