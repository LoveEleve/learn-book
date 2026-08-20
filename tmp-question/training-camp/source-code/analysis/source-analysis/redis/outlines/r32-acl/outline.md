# R-32 ACL — 权限体系 (1024 位命令矩阵 + 多 selector + 实时验证)

> 前置: [[R-20-server]] (命令表/cmd->id/key specs) + [[R-28-networking]] (AUTH/连接身份) + [[R-16-multi]] (EXEC 复查) + [[R-30-lua]] (脚本验证) + [[R-29-pubsub]] (频道权限) | 引出: [[R-31-module]] (模块命令权限) | 对照: [[R-30-lua]] (命令标志)
> 🟡 B | 6 KP | [模式: 位图权限矩阵 + 规则 DSL + 多 selector + 实时验证 + 审计日志]
> Pass 2 闭环: q1(位图) q2(规则解析) q3(验证链) q4(密码) q5(LOG) q6(命令面+加载)

**读者处境**: AUTH 只认一个密码, 怎么做到"这个用户只能 GET 某些键"? 命令权限存哪? 改权限为什么要重启? 7.0 的 %R~ 是什么? 这篇拆 ACL: 1024 位命令矩阵、规则 DSL、多 selector 读写分离、实时验证链、审计日志。

### 1. 权限矩阵 — 1024 位命令位图 × 21 分类

场景: 10 万个客户端各不同权限, 服务器怎么存?
源码路径:
- **命令位图** (server.h:1067-1070): `USER_COMMAND_BITS_COUNT 1024` — cmd->id 索引, 16×64 位字; ACLGetCommandBitCoordinates (acl.c:519) 定位
- **21 命令分类** (server.h:224-244): keyspace/read/write/set/sortedset/list/hash/string/bitmap/hll/geo/stream/pubsub/admin/fast/slow/blocking/dangerous/connection/transaction/scripting (1<<0..1<<20)
- **+@all 全开** (acl.c:1043-1050): memset(255) + SELECTOR_FLAG_ALLCOMMANDS
- **命令 ID 映射** (ACLGetCommandID L1522): 注册序 ID, 模块重载复用
- **selector 默认零权限** (acl-v2.tcl 实证): 新 selector 全 0 — "默认拒绝"哲学
关键设计 (q1): **位图 = O(1) 判定 + 极小内存** (每 selector 128 字节); 分类位是"命令面"粗粒度, 位图是精确面; 默认零权限 = 安全默认。[模式: 位图矩阵]

### 2. 规则 DSL — 左到右顺序解析

场景: `user alice on +@read ~app:* >pass123` 怎么变成权限?
源码路径:
- **入口** (ACLSetUser L1272 → ACLSetSelector L1025): 每条规则独立解析, **左到右顺序语义** (redis.conf L982: 后规则覆盖前规则 — `-@all +get` 模式)
- **命令面规则**: `+cmd`/`-cmd` (位操作) / `+@cat`/`-@cat` (分类循环) / `allcommands`/`nocommands` / **子命令白名单** `+config|get` (规则解析 L1176 → ACLAddAllowedFirstArg L933 — **父命令位图清零, 精确到子命令**; 验证时 firstargs 匹配)
- **键模式**: `~*`/allkeys (ALLKEYS 标志) / `~pattern` (默认读写) / **`%R~pat`/`%W~pat`/`%RW~pat`** (L1074-1096 读写分离, 7.0) / resetkeys
- **冲突检查**: ALLKEYS 后加模式 → EEXIST (L1071); ALLCHANNELS 后加频道 → EISDIR (L1106); 非法权限位 → EINVAL — **规则歧义显式拒绝**
- **频道**: `&*`/allchannels / `&pattern` / resetchannels
- **去重合并** (L1097-1102): 同模式重复 → flags |= (读+写合并)
关键设计 (q2): **DSL = 声明式权限语言**; 顺序语义让"先全部拒绝再精确放开"成为标准模式; 冲突检查防"模式互相覆盖"的静默错误。[模式: 规则 DSL]

### 3. 验证链 — 多 selector OR + keyspec 权限映射

场景: 一条 GET key 命令怎么走到允许/拒绝?
源码路径:
- **入口** (server.c:3987): processCommand → ACLCheckAllPerm → 失败 → ACL LOG + **-NOPERM**
- **多 selector OR 语义** (acl.c:1856-1872): 任一 selector 通过即允许; 全失败 → **最高错误级别定位** (ACL_DENIED_* 序: CMD=1<KEY=2<AUTH=3<CHANNEL=4, 数值大者优先 — KEY 比 CMD 具体, server.h:2936-2939); **key 结果缓存** 跨 selector 共享 (L1854, 单次验证链生命周期)
- **命令位图 + 子命令白名单** (L1682-1696): CMD_NO_AUTH 豁免 (AUTH/HELLO 始终可执行); 位=0 → firstargs 匹配 (argv[idx], 子命令 idx=2)
- **键检查** (L1700-1710): doesCommandHaveKeys → getKeysFromCommandWithSpecs → **ACLSelectorCheckKey** (L1571): ALLKEYS 快捷 / **keyspec→权限映射** (ACCESS→READ, INSERT/DELETE/UPDATE→WRITE, L1579-1584) / **模式权限覆盖** (pattern->flags ⊇ 所需, L1591-1593 — 只读模式不能过写命令) / stringmatchlen glob
- **三时机**: processCommand 每次 + EXEC 复查 (R-16) + 脚本内 (R-30 scriptVerifyACL)
关键设计 (q3): **实时验证零缓存** = 权限变更即刻生效 (无需重启); 多 selector 的 OR 语义 = "任一权限面覆盖即可"; keyspec 映射把"命令怎么写键"翻译为"需要什么权限"。[模式: 实时验证链]

### 4. 密码面 — SHA256 + nopass + 时序安全

场景: 密码怎么存才安全?
源码路径:
- **SHA256 hex** (ACLHashPassword L201-218): 64 字符; 多密码列表; `#hash` 直接加哈希 (不回显明文)
- **nopass** (L1451-1453): 免密用户; disabled 禁登录 (L1446-1449)
- **时间无关比较** (time_independent_strcmp L191): 全长度比较 — **防时序攻击** (密码逐字符差异可测)
- **认证链** (L1504): 模块认证优先 → 密码认证; 失败 → **ACL LOG (ACL_DENIED_AUTH)** (L1492)
- **GENPASS**: 随机密码生成
关键设计 (q4): **哈希存储 + 时序安全比较**; 认证失败也进审计日志 — 安全事件可追溯。[模式: 凭据面]

### 5. ACL LOG — 聚合审计

场景: 谁在反复尝试越权?
源码路径:
- **结构** (L2586-2605): count/reason/context/object/username/ctime/cinfo + **entry_id** (集群辨识)
- **聚合** (ACLLogMatchEntry L2602): 同 reason+context+object+username 且 **60 秒窗** (ACL_LOG_GROUPING_MAX_TIME_DELTA=60000, L2583) → count++ — **防刷屏**
- **容量** (L2637): acllog-max-len 默认 128 (config.c:3198)
- **dryrun** (aclCommand): **零副作用预演** (以指定用户身份试跑命令检查权限)
- **ACL CAT/LIST/GETUSER**: 查询面
关键设计 (q5): **审计 = 聚合日志 + 容量控制**; dryrun 让"调权限"变可测试 (CI 场景)。[模式: 审计日志]

### 6. 命令面与加载 — 13 子命令 + aclfile 持久化 + 变更即时性

场景: ACL 配置怎么持久化? 改权限对已连接客户端生效吗?
源码路径:
- **13 子命令** (aclCommand L2844): cat/deluser/dryrun/genpass/getuser/help/list/load/log/save/setuser/users/whoami
- **持久化双路径**: aclfile (ACLLoadFromFile L2272/ACLSaveToFile L2469 — ACL SAVE/LOAD) + **config 内联 user 行** (config.c:548 → UsersToLoad)
- **DefaultUser** (L1407): `+@all ~* &* on nopass` — **requirepass 兼容桥** (config.c:2567-2571: "requirepass just translates to setting a password to the default user... backward compatibility with Redis <= 5")
- **模块命令纳入位图**: 模块注册命令时分配 cmd->id + 声明 acl_categories (R-31 交叉 — 权限体系对扩展开放)
- **变更即时性**: 实时验证 (q3) + **pubsub 收窄断开** (ACLKillPubsubClientsIfNeeded L1988: 权限变更后遍历客户端, 不再有权的订阅者 deauthenticateAndCloseClient)
- **GETUSER 回显** (ACLDescribeUser L846): 规则原文 + 密码哈希
- **测试面**: acl.tcl 1311 行 + acl-v2.tcl 551 行 (多 selector 专项)
关键设计 (q6): **权限变更零重启 + 已连接客户端同步执行** (pubsub 断开是"权限即时性"的极致形态); aclfile 与 RDB/AOF 独立 (安全面单独管理)。[模式: 命令家族 + 即时生效]

### 负面空间 — ACL 刻意不做的事

- **不做角色/组**: 无"角色"概念, 规则直接绑用户 (或外部脚本生成 SETUSER)
- **不做键粒度通配扩展**: glob 模式, 无正则/无键名加密
- **不做登录尝试限流**: 暴力破解防护靠外部 (fail2ban) — ACL LOG 仅审计
- **不做密码过期/轮换**: 需外部管理
- **不做权限继承**: selector 是 OR 非继承; 无多用户组
- **不做细粒度审计**: ACL LOG 只记拒绝, 不记成功操作 (对照 Redis Enterprise)
- **不做 TLS 内建**: 密码明文协议由外部 TLS 兜底

→ 引出: 模块命令怎么纳入权限体系? → [[R-31-module]]
