# 闭环笔记 q5: 命令表 — 生成式定义 + 双字典注册

## 假设
命令表由生成文件 commands.def (11235 行/122 命令) 定义, 运行时 populateCommandTable 注册到双字典 (commands/orig_commands — rename-command 免疫)。

## 验证过程
- commands.c (13 行薄壳): include commands.def (生成自 JSON 命令定义)
- commands.def (11235 行): `struct COMMAND_STRUCT redisCommandTable[]` (L10965) — **122 个命令**静态初始化 (声明式字段: name/summary/complexity/since/flags/acl/key_specs/arity/proc)
- populateCommandTable (server.c:3075-3095): 遍历表 → populateCommandStructure (sentinel 过滤 L3034-3039 / ACL 隐式分类 L3043 / 延迟直方图 L3047 / key_specs 处理) → **双字典注册: server.commands + server.orig_commands** (L3087-3090, 注释: "unaffected by rename-command")
- redisCommand 结构 (server.h:2341+): 声明字段 (doc 面) + proc/arity/flags (执行面) + key_specs (cluster 重定向/ACL) + subcommands (命令组)
- 查找: lookupCommand (先 commands 后 orig?) — rename-command 只影响前者
- 演进: 早期手写命令表 → JSON 定义生成 (文档/ACL/key_specs 单一来源)

## 代码类型
Glue (注册机制) + Interface (声明式命令)

## 跨域关联
- R-32 (ACL 命令分类) → acl_categories
- R-15 (cluster key 重定向) → key_specs
- R-3 (dict 双表) → 注册载体

## 结论
命令表 = 生成式定义 (122 命令, JSON 单一来源) + 双字典注册 (rename 免疫); populateCommandStructure 完成 sentinel 过滤/ACL/直方图延迟分配。声明字段 (doc) 与执行字段 (proc) 一体。
源码位置: commands.c/commands.def; server.c:3032-3095; server.h:2341+
