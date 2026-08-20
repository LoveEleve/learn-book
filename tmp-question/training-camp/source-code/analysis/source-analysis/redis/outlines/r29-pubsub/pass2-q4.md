# 闭环笔记 q4: 消息格式 — RESP2 数组 vs RESP3 push

## 假设
RESP2 用 mbulkhdr 数组格式; RESP3 用 push 类型; CLIENT_PUSHING 标志管理。

## 验证过程
- addReplyPubsubMessage (pubsub.c:86-97): `c->resp == 2 → addReply(shared.mbulkhdr[3])` (3 元素数组头) / else `addReplyPushLen(c,3)` (RESP3 push) (L89-92); 内容 = messagebulk + channel + msg (L93-95)
- addReplyPubsubPatMessage (L102-114): 4 元素 (pmessagebulk + pattern + channel + msg)
- addReplyPubsubSubscribed (L117-128): 3 元素 (subscribebulk + channel + 订阅计数) — **计数 = type.subscriptionCount(c) 函数指针** (L126)
- addReplyPubsubUnsubscribed (L134-148): channel 可为 NULL (批量零订阅) → addReplyNull (L142-145)
- **CLIENT_PUSHING 标志** (L87-88,96): 置位 → 回复 → 复位 (保存 old_flags 防止嵌套覆盖, L96); server.h:385 `CLIENT_PUSHING (1ULL<<46)` — RESP3 push 上下文标记 (R-28 处理 push 回复)
- **共享消息类型对象** (server.c:1932-1940): messagebulk "$7\r\nmessage\r\n" / pmessagebulk / subscribebulk / unsubscribebulk / ssubscribebulk / sunsubscribebulk / smessagebulk / psubscribebulk / punsubscribebulk — 全部 createStringObject 预生成, RESP2 协议串
- pingCommand pubsub 特殊回复 (server.c:4603-4612): RESP2+CLIENT_PUBSUB → ["pong", msg] 数组; 否则普通 +PONG
- **RESP2 pubsub 白名单** (server.c:4112-4125): CLIENT_PUBSUB+RESP2 只允许 ping/subscribe/ssubscribe/unsubscribe/sunsubscribe/psubscribe/punsubscribe/quit/reset — 其他命令拒绝 ("only (P|S)SUBSCRIBE / (P|S)UNSUBSCRIBE / PING / QUIT / RESET are allowed"); **RESP3 无限制** (L4112 注释) — RESP3 客户端可在订阅状态执行其他命令

## 代码类型
Protocol (消息格式)

## 跨域关联
- R-28 (已交付): RESP2/RESP3 回复体系/mbulkhdr/shared 对象
- R-1: 共享对象复用

## 结论
格式 = RESP2 数组 (mbulkhdr[3/4]) / RESP3 push 双轨, CLIENT_PUSHING 上下文标记; 协议串全部共享对象; RESP2 订阅态命令白名单 vs RESP3 自由。
源码位置: pubsub.c:86-182; server.c:1932-1940,4112-4125,4603-4612
