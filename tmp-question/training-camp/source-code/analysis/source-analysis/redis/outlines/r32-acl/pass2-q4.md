# 闭环笔记 q4: 密码面 — SHA256 + nopass + 时序安全

## 假设
密码存 SHA256 hex; 明文传输 (AUTH 协议限制); 比较时间无关。

## 验证过程
- **哈希** (ACLHashPassword L201-218): SHA256 → 64 字符 hex (HASH_PASSWORD_LEN); ACLCheckPasswordHash (L220) 校验哈希格式 (仅 hex 字符)
- **存储**: user->passwords 列表 (可多密码); `>password` 明文添加 (内部哈希) / `<password` 移除 / `#hash` 直接加哈希 (ACLSetUser)
- **验证** (ACLCheckUserCredentials L1433-1465): 用户不存在 → ENOENT; 禁用 → EINVAL; **nopass → 直接 OK**; 否则明文重算 SHA256 逐一比对
- **时间无关比较** (time_independent_strcmp L191-199): 全长度比较 (不提前退出) — **防时序攻击** (密码逐字符差异可测)
- **认证链** (ACLAuthenticateUser L1504): **模块认证优先** (checkModuleAuthentication) → 未处理 → 密码认证; AUTH_OK/ERR/BLOCKED 三态
- **失败记录** (checkPasswordBasedAuth L1485-1502): addACLLogEntry(ACL_DENIED_AUTH) — **认证失败也进 ACL LOG**
- **GENPASS** (aclCommand): 随机密码生成 (hex, 长度可选)
- **协议面**: AUTH username password 明文 — 密码经 TCP 传输 (TLS 面防护, 对照 R-28)

## 代码类型
Implementation (凭据面)

## 跨域关联
- R-28 (networking): AUTH 命令面/连接认证状态
- R-30 (lua): 对照脚本 SHA1 — ACL 用 SHA256 (无碰撞安全面)

## 结论
密码 = SHA256 hex 多密码列表 + nopass 快捷 + 时间无关比较 (时序攻击防护); 认证失败进 ACL LOG; 模块认证可插拔; 明文协议限制由 TLS 兜底。
源码位置: acl.c:191-245,1433-1522
