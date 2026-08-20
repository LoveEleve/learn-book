# 闭环笔记 q6: 命令面与加载面 — 13 子命令 + 持久化 + 变更即时性

## 假设
ACL 命令 13 子命令; aclfile 持久化; 权限变更实时生效 (无缓存)。

## 验证过程
- **子命令穷举** (aclCommand L2844, grep 实证 13 个): cat / deluser / dryrun / genpass / getuser / help / list / load / log / save / setuser / users / whoami
- **变更即时性**: ACLCheckAllPerm 每次实时查 (q3) — **无权限缓存**; SELECTOR_FLAG 变更即对下一条命令生效 (对照: 键权限 cache 仅限单次验证链内, initACLKeyResultCache/cleanup L1662-1677)
- **pubsub 收窄处理** (ACLKillPubsubClientsIfNeeded L1988-2019): 用户权限变更 (SETUSER) 后, 新权限非旧权限超集 → 遍历客户端断开不再有权的订阅者 (deauthenticateAndCloseClient); getUpcomingChannelList 计算超集
- **持久化三路径**:
  - aclfile (ACLLoadFromFile L2272 / ACLSaveToFile L2469): 启动加载 + ACL SAVE 写文件; aclfile 配置 IMMUTABLE (config.c:3098)
  - **config 内联 user 行** (config.c:548 user 配置解析 → UsersToLoad 列表)
  - ACL LOAD (从 aclfile 重载)
- **ACLLoadConfiguredUsers** (L2205): UsersToLoad → ACLCreateUser + 逐规则 ACLSetUser; **default 重复定义 → reset 重建** (L2218-2222)
- **ACLLoadUsersAtStartup** (L2551): 启动时按配置/文件加载
- **DESCRIBE 面** (ACLDescribeUser L846 / ACLDescribeSelector L803): GETUSER 输出规则原文 (密码哈希回显, 测试 "ACL GETUSER returns the password hash")
- **ACL SETUSER 规则合并** (ACLMergeSelectorArguments L2032): 多参数 selector 定义合并 (括号内参数)
- **测试面**: acl.tcl 1311 行 + acl-v2.tcl 551 行 (多 selector 专项) — 权限面最全测试域之一

## 代码类型
Interface (命令面 + 持久化)

## 跨域关联
- R-8 (persistence): 与 RDB/AOF 无关 (ACL 独立文件) — 对照
- R-20 (server): 启动管线 (ACLInit L1418)
- R-29 (pubsub): 频道权限 + 订阅者断开

## 结论
命令面 13 子命令 (含 dryrun 预演/LOG 审计); 持久化 = aclfile + config 内联双路径; 变更即时生效 (实时查 + pubsub 收窄断开); GETUSER 回显规则原文。
源码位置: acl.c:846,1418,1988-2019,2032,2205-2551,2844; config.c:548,3098,3136,3198
