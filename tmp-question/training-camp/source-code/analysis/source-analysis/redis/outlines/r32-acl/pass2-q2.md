# 闭环笔记 q2: 规则解析器 — ACLSetSelector 全语法

## 假设
规则左到右顺序处理; selector 语法面 = 键/频道/命令三族 + 特殊标志。

## 验证过程
- **入口** (ACLSetUser L1272 → ACLSetSelector L1025): 每规则一调用
- **命令面** (L1043-1058): `allcommands`/`+@all` → 全开位图; `nocommands`/`-@all` → 全零 + 清 ALLCOMMANDS 标志; `+cmd`/`-cmd` → ACLChangeSelectorPerm (L623, 单命令位); `+@cat`/`-@cat` → ACLSetSelectorCategory (L697, 分类位图循环)
- **子命令白名单** (ACLAddAllowedFirstArg L933): `+config|get` 形式 — 精确到子命令 (验证时 firstargs 匹配, L1697-1711); 父命令位图清零+记录子命令
- **键模式** (L1060-1102):
  - `~*`/`allkeys` → ALLKEYS 标志 + 清 patterns; `resetkeys` → 反向
  - `~pattern` → 默认读写权限 (ACL_ALL_PERMISSION)
  - **`%R~pat`/`%W~pat`/`%RW~pat`** (L1074-1096): 解析 R/W 权限位 (去重) + `~` 后接模式; 无权限位或非法 → EINVAL
  - **ALLKEYS 后加模式 → EEXIST** (L1071-1073): 冲突拒绝
  - 模式去重合并 (L1097-1102: listSearchKey → 已存在则 flags |= )
- **频道模式** (L1103-1120): `&*`/`allchannels` → ALLCHANNELS; `&pattern`; resetchannels 反向; ALLCHANNELS 后加 → EISDIR
- **密码/标志面** (ACLSetUser): `on/off/nopass/resetpass/>password/<password/reset` — 密码列表管理
- **顺序语义** (redis.conf L982 文档): "ACL rules are processed left-to-right" — 后规则覆盖前规则 (如 `-@all +get`)

## 代码类型
Interface (规则 DSL 解析)

## 跨域关联
- R-20 (server): 命令分类表 (ACLInitCommandCategories L98)

## 结论
规则 = 左到右顺序 DSL: 命令位图 (±cmd/±@cat/子命令白名单) + 键模式 (~pat 读写 / %R~%W~ 分离 / ~* 全开) + 频道 (&pat) + 标志; 冲突检查 (EEXIST/EISDIR/EINVAL) 防规则歧义。
源码位置: acl.c:1025-1120,1272; redis.conf:974-982
