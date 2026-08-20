# R-32 ACL — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **补充锚点** | **ACL_DENIED_* 枚举序** (server.h:2936-2939): CMD=1 < KEY=2 < AUTH=3 < CHANNEL=4 — "最高错误级别"选择 = 数值大者 (KEY > CMD, 更具体的键错误优先); 大纲 §3 未提枚举序 | 大纲 §3 补注 |
| 2 | **补充锚点** | **ACL_LOG_GROUPING_MAX_TIME_DELTA = 60000ms** (acl.c:2583) — 聚合时间窗 1 分钟; 大纲 §5 只写"时间窗"未给值 | 大纲 §5 补注 |
| 3 | **机制洞察** | **requirepass → DefaultUser 密码** (config.c:2567-2571 + updateRequirePass): "The old requirepass directive just translates to setting a password to the default user... for backward compatibility with Redis <= 5" — ACL 与旧配置的兼容桥; 大纲 §6 未提 | 大纲 §6 补注 |
| 4 | 验证 | firstargs 解析 (`+config|get`): ACLAddAllowedFirstArg 调用点 (acl.c:1176) — 规则解析 | 记录 |
| 5 | 验证 | 键模式 nocase=0 (L1594) — 键名大小写敏感 (对照频道?) | 记录 |
| 6 | 行号验证 | 全函数 30 锚点 + 跨文件 10 处 grep 穷举 (acl.c 191-3241 / server.h 224-244,1067-1070,2936-2939 / server.c 3987-3997 / config.c 548,2567-2571,3098,3136,3198) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 位图定位 (word/bitshift) / 分类循环 / firstargs 匹配 idx (parent?2:1)
- 键权限映射 (keyspec→R/W) + 模式覆盖检查
- 多 selector OR + key 缓存
- 密码时序比较 / nopass / disabled
- pubsub 收窄断开

### 维度2 性能
- 位图 O(1) 判定 (128B/selector)
- key 结果缓存跨 selector 共享
- 实时验证零缓存 (变更即时生效)

### 维度3 内存
- 1024 位 × 多 selector — 用户级内存可控
- ACL LOG 容量上限 (128)
- 密码哈希 64 字符

### 维度4 一致性
- 三时机验证 (processCommand/EXEC/脚本)
- 权限变更即时性 (无缓存 + pubsub 断开)
- requirepass 兼容桥
- 多 selector OR 语义一致性

### 维度5 负面空间 (已写入大纲 7 条)
- 不做角色/键粒度扩展/登录限流/密码轮换/权限继承/成功审计/TLS 内建

## 结论
R-32 全部锚点 ~40 处验证, 6 闭环完成, **机制洞察 1 + 补充锚点 2**。怀疑审计全接受+补充。推断 3 处显式标注。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-20/R-28/R-16/R-30/R-29 (序号均 < 32) 已讲 ✅; 引出 R-31 (未来 OK); 对照 R-30 ✅; 读者处境场景化 (单密码限制/权限存哪/重启疑问/%R~) ✅; 六结构元素齐备 ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 大纲 ~40 锚点 (file:line) — 🟡B ≥4 ⏫ ✅ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-31 仅引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 认证 (AUTH 链) / 命令面 (位图) / 键空间 (模式) / pubsub (频道+断开) / 事务 (EXEC 复查) / 脚本 (scriptVerifyACL) / 集群 (客户端 ACL) / 持久化 (aclfile) — 八横切全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 负面空间 7 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **反写测试发现** | 大纲 §2 "子命令白名单 `+config|get`" — **语法解析点未锚定** (ACLAddAllowedFirstArg 是存储, 解析在规则循环 L1176 附近); 且未说明**位图父命令清零行为** (允许子命令时必须精确到 firstargs — 否则 CONFIG 全开) — 需补解析锚点 | 大纲 §2 补注 |
| 13 | 通过项 | 其余 ~35 句机制描述逐句对源码一致 ✅ (1024 位/21 分类/+@all memset/EEXIST/EISDIR/去重合并/OR 语义/错误级别/CMD_NO_AUTH 豁免/nopass/SHA256/时序比较/GENPASS/聚合 60s/容量 128/dryrun/13 子命令/双路径持久化/DefaultUser/pubsub 断开/GETUSER 回显) | 记录 |

## 二次 REVIEW 汇总
07 五维度轮换 + 内容深度共 **1 处修复** (反写测试 #12), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

> 动机: 大纲每个锚点重新 grep, 对全部推断/计数反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 位图大小数学 | 1024 位 = 16×64 位字; allowed_commands 数组 16 元素 (user 结构) — GET/SET 位操作 word=id/64, bit=id%64 ✅ | 通过 |
| V2 | 分类数 | server.h:224-244 宏 1<<0..1<<20 = 21 个; +@all 为特殊规则非宏 ✅ | 通过 |
| V3 | 子命令 idx | L1692-1694: `int idx = cmd->parent ? 2 : 1` — CLIENT 类容器命令子命令 argv[1], 普通命令 argv[1] (idx=1) 与 (idx=2) 边界 ✅ | 通过 |
| V4 | 错误级别序 | CMD=1<KEY=2<AUTH=3<CHANNEL=4 — L1864 `acl_retval > relevant_error` 取最大; KEY 比 CMD 具体 (日志定位到键) ✅ | 通过 |
| V5 | 键缓存生命周期 | initACLKeyResultCache (L1662) / cleanup (L1666) — 单次 ACLCheckAllUserCommandPerm 内有效, 跨 selector 共享, 验证完释放 ✅ | 通过 |
| V6 | 聚合窗 | 60000ms — 同 reason/context/object/username 且 1 分钟内 → 合并 count++ ✅ | 通过 |
| V7 | pubsub 超集判定 | getUpcomingChannelList (L1884): 新权限 ⊇ 旧 → NULL (不动); 否则遍历客户端断开 — 精确到频道差集 ✅ | 通过 |
| V8 | DefaultUser 构成 | +@all ~* &* on nopass (L1407-1415) — 与 requirepass 兼容桥 (config.c:2567) 闭环 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 14 | **覆盖缺口** | 大纲 §6 未提 **ACL 与模块命令的交互**: 模块命令注册时纳入位图 (cmd->id + 分类, R-31 交叉); 模块可声明 acl_categories — 权限体系对扩展开放; 大纲"引出 R-31"已有但未点明机制 | 大纲 §6 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (覆盖缺口: 模块命令纳入位图), 修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。
