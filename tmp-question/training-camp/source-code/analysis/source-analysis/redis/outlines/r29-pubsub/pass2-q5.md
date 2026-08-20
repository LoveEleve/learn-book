# 闭环笔记 q5: Keyspace 通知 — 位掩码 + 双频道格式

## 假设
NOTIFY_* 位掩码过滤; 两类频道 __keyspace@ / __keyevent@; 默认全关; module 旁路。

## 验证过程
- **15 类位掩码** (server.h:641-656): NOTIFY_KEYSPACE (1<<0,K) / KEYEVENT (1<<1,E) / GENERIC (1<<2,g) / STRING (1<<3,$) / LIST (1<<4,l) / SET (1<<5,s) / HASH (1<<6,h) / ZSET (1<<7,z) / EXPIRED (1<<8,x) / EVICTED (1<<9,e) / STREAM (1<<10,t) / KEY_MISS (1<<11,m) / LOADED (1<<12, module only) / MODULE (1<<13,d) / NEW (1<<14,n)
- **NOTIFY_ALL = 10 类组合** (L656): GENERIC|STRING|LIST|SET|HASH|ZSET|EXPIRED|EVICTED|STREAM|MODULE — **不含 KEYSPACE/KEYEVENT/KEY_MISS/LOADED/NEW** (注释 L652: KEY_MISS "excluded from NOTIFY_ALL on purpose")
- 配置解析: keyspaceEventsStringToFlags (notify.c:19-44) 字母→位 (**15 字符映射含 A**: A,g,$,l,s,h,z,x,e,K,E,t,m,d,n, 未知 → -1); **LOADED 无字符映射仅 module 内部**; 反向 keyspaceEventsFlagsToString (L50-73, **A 条件 = (flags & NOTIFY_ALL) == NOTIFY_ALL** L54)
- notifyKeyspaceEvent (L83-124):
  - **module 旁路优先** (L93): moduleNotifyKeyspaceEvent — "bypasses the notifications configuration"
  - **type 过滤** (L96): `if (!(server.notify_keyspace_events & type)) return;`
  - **__keyspace@<db>__:<key>** (L101-110): 频道 = "keyspace@N__:"+key, 消息 = event 名 ("SET"/"del"...) → pubsubPublishMessage
  - **__keyevent@<db>__:<event>** (L113-122): 频道 = "keyevent@N__:"+event, 消息 = key 对象 → pubsubPublishMessage
  - 两者都受 K/E 位门控 (L101, L113)
- 默认: `server.notify_keyspace_events = 0` (server.c:2081) — 全关, 需 CONFIG SET notify-keyspace-events
- 消费端 (grep 实证): 15 个文件调用 notifyKeyspaceEvent — db.c 9 处 (键空间操作)、t_*.c 60+ 处 (命令面)、expire.c (过期 x)、evict.c (淘汰 e)、module/sort/cluster
- 事件名约定: 命令小写 ("set"/"hset"/"pfadd"/"geosearchstore"/"del"/"expired"/"evicted")

## 代码类型
Mechanism (事件通知)

## 跨域关联
- R-21/22/23/24/25/26/27/11/12/13 (全部已交付): 各命令/键空间调 notifyKeyspaceEvent — 本域是消费端实现
- R-30 (未来): module 事件面
- R-17 (未来): 客户端缓存 invalidate 的类似机制对照

## 结论
通知 = 位掩码门 + 双频道 (key 面/event 面) 转 PUBLISH; A 仅含 10 类 (K/E/m/n 不随 A); module 旁路绕过配置。
源码位置: server.h:641-656; notify.c:19-73,83-124; server.c:2081
