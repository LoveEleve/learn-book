# R-32 ACL — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.4 (2011) | AUTH 初版 (单密码 requirepass, commands.def AUTH 1.0.0 实为 2.4 文档标注 — 需核对; 实际 AUTH 2.4 引入) |
| **6.0 (2020)** | **ACL 系统引入** — acl.c 版权 2020-Present (L1); 用户/命令位图/键模式/频道/ACL LOG; 密码 SHA256 (对照旧 requirepass 明文比较); DefaultUser 兼容 requirepass |
| 6.0 系列 | acl-pubsub-default 配置 (pubsub 权限默认收紧演进); ACL SAVE/LOAD (aclfile) |
| **7.0 (2022)** | **多 selector**: user 内嵌单 selector → selectors 链表 (acl-v2.tcl 专项测试); **%R~/%W~ 读写分离键模式** (keyspec flags 映射); 子命令白名单 firstargs 精确化; ACL dryrun 预演 |
| 7.x | ACL LOG entry_id/timestamp_created (集群故障恢复辨识); ACLKillPubsubClientsIfNeeded (变更即时断开); acllog-max-len 128 默认 |

## 痕迹证据

- acl.c:1-17: 版权 2020-Present + 模块自述
- acl.c:20: DefaultUser 全局
- server.h:1067-1070: USER_COMMAND_BITS_COUNT 1024 注释
- acl.c:1407-1415: ACLCreateDefaultUser 构成 (+@all ~* &* on nopass)
- acl.c:1854-1855: 多 selector key 缓存注释
- acl.c:1988-1992: pubsub 收窄断开注释
- acl-v2.tcl: 多 selector/读写分离专项测试 (7.0 时代)
- redis.conf:879-1000: ACL 规则文档 (左到右顺序语义 L982)
- config.c:548: user 配置解析 ("user <username> ... acl rules ...")

## 推断标注

- "AUTH 2.4 引入" — commands.def AUTH "1.0.0" 但 ACL 认证 6.0; 旧 requirepass 时代推断 (标注)
- "6.0 系列 acl-pubsub-default" — 配置年代推断 (标注)
- "firstargs 白名单 7.0" — 与多 selector 同代推断 (标注)
- 仓库浅克隆无法 git 考古 — 版本线依赖版权/注释/测试面, 已逐条标注实证级别
