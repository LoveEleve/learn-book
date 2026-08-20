# 闭环笔记 q5: ACL LOG — 聚合条目 + 容量控制

## 假设
拒绝事件聚合为条目 (同类合并计数); acllog-max-len 上限。

## 验证过程
- **结构** (acl.c:2586-2605 ACLLogEntry): count (发生次数)/reason (ACL_DENIED_*)/context (TOPLEVEL/LUA/MULTI)/object (键或命令名)/username/ctime/cinfo (客户端信息)/**entry_id + timestamp_created** (集群故障恢复辨识 — 重启后新序列)
- **聚合** (ACLLogMatchEntry L2602-2619): 同 reason + 同 context + **时间差 ≤ ACL_LOG_GROUPING_MAX_TIME_DELTA** + 同 object + 同 username → 合并 count++
- **容量** (trimACLLogEntriesToMaxLen L2637): 超 acllog-max-len (默认 128, config.c:3198) 裁剪
- **记录入口** (addACLLogEntry L2661): 调用点 — processCommand ACL 拒绝 (server.c:3989) + AUTH 失败 (L1492) + 频道/pubsub 拒绝等
- **reason 枚举** (ACL_DENIED_*): AUTH/CMD/KEY/CHANNEL (+ 待穷举)
- **ACL LOG 命令面**: 查看 (ACL LOG) / 重置 (ACL LOG RESET)
- **ACL dryrun** (aclCommand dryrun): **预演权限** — 以指定用户身份检查命令不实际执行 (排障利器)
- **ACL CAT**: 分类与命令对照查询

## 代码类型
Implementation (审计日志)

## 跨域关联
- R-20 (server): INFO metrics (ACL 统计, ACLUpdateInfoMetrics L2623)

## 结论
ACL LOG = 聚合审计 (同类合并计数, 时间窗分组) + 容量上限 + 集群可辨识 ID; dryrun 提供零副作用预演; 认证失败也记录。
源码位置: acl.c:2586-2661,2736; config.c:3198
