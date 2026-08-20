# 闭环笔记 q6: 命令面与重定向 — CLIENT TRACKING 家族 + 协议格式

## 假设
CLIENT TRACKING/CACHING/GETREDIR/TRACKINGINFO 四子命令 + 选项兼容性矩阵; sendTrackingMessage 按 RESP 版本分派。

## 验证过程
- **版本** (commands.def): TRACKING **6.0.0** (L1556) / GETREDIR 6.0.0 (L1544) / CACHING 6.0.0 (L1542) / TRACKINGINFO **6.2.0** (L1558); 全组 connection / CMD_NOSCRIPT|LOADING|STALE|SENTINEL
- **CLIENT TRACKING 解析** (networking.c:3354-3407): on/off + REDIRECT <id> (重复报错 L3368-3373; **目标必须存在** L3384-3389 "valid sanity check") + BCAST/OPTIN/OPTOUT/NOLOOP + PREFIX (可多个, zrealloc 累积)
- **兼容性矩阵** (L3410-3466, 全 error 拒绝):
  1. PREFIX 无 BCAST → 错 (L3413-3418)
  2. **BCAST 模式切换禁止** (L3420-3431): 已 tracking 且 oldbcast != newbcast → 必须先 off
  3. BCAST × (OPTIN|OPTOUT) → 错 (L3433-3440)
  4. OPTIN × OPTOUT → 错 (L3442-3448)
  5. 已 tracking 且 OPTIN↔OPTOUT 切换 → 错 (L3450-3459)
  6. BCAST → 前缀冲突检查 (L3461-3466)
- **enableTracking** (tracking.c:164-193): 清 5 标志位重设 + redirection 覆盖; 首客户端建三全局; BCAST → 注册前缀 (空 → "")
- **disableTracking** (L46-81): BCAST → 逐前缀退订 (最后客户端删 bcastState); 清 7 标志 + tracking_clients-- (**惰性: 表项不删**)
- **CLIENT CACHING** (networking.c:3478-3507): 无 TRACKING 拒 (L3479-3484); YES 仅 OPTIN / NO 仅 OPTOUT (q2 已详)
- **CLIENT GETREDIR** (L3508-3514): tracking 中 → redirection id (可能 0); 否则 -1
- **CLIENT TRACKINGINFO** (L3515-3575): map 3 字段 — flags (数组: on/off + bcast + optin[+caching-yes] + optout[+caching-no] + noloop + broken_redirect) + redirect (id 或 -1) + prefixes (客户端前缀数组)
- **sendTrackingMessage** (tracking.c:255-311):
  - CLIENT_PUSHING 保护 (L256-257): 穿透 CLIENT REPLY OFF/SKIP (networking.c:286-289) + addReplyPushLen 断言 (L1001)
  - **重定向查表** (L260-280): redirection → lookupClientByID; **目标不存在 → CLIENT_TRACKING_BROKEN_REDIR + RESP3 push ["tracking-redir-broken", id]** (L263-273; 测试 L255-277); 存在 → 切到 redir 客户端发
  - **三分派** (L282-300): ① resp>2 → push [2,"invalidate",keys] (L286-288) ② RESP2 + 重定向 + 目标 PUBSUB → `__redis__:invalidate` 频道 pubsub 消息 (L289-292, addReplyPubsubMessage — 20 字符频道名 L177) ③ 其余 (RESP2 无重定向) → **静默丢弃** (L293-299 — RESP2 不支持同连接 push)
  - value 部分 (L302-308): proto=1 → addReplyProto (预序列化批量数组) / 否则 addReplyArrayLen(1)+Bulk(key)
- **lookupClientByID** (networking.c:1832-1837): htonu64 → clients_index rax (q1 已述)
- **生命周期**: freeClient (networking.c:1515-1516) 与 clearClientConnectionState (L1536, RESET 命令) → disableTracking
- **CLIENT LIST 标志** (L2832-2834): t (TRACKING) / R (BROKEN_REDIR) / B (BCAST); CLIENT INFO redir=%I (L2891)
- **崩溃防护**: current_client 可能已被 free (tracking.c:425-427 注释) — pending 冲刷判空

## 代码类型
Interface (命令面 + 协议格式)

## 跨域关联
- R-28 (networking): RESP3 push 协议 / addReplyPubsubMessage / CLIENT LIST
- R-29 (pubsub): __redis__:invalidate 频道复用 pubsub 发送链
- R-16 (multi): CACHING 标志 MULTI 内保活 (q2)
- R-20 (server): commands.def / ACL 类别 CONNECTION

## 结论
命令面 = 4 子命令 + 6 类选项互斥检查 (BCAST 切换/OPTIN×OPTOUT/PREFIX 依赖); 发送面 = CLIENT_PUSHING 穿透 + 重定向链 (broken 通知) + RESP3 push / RESP2 pubsub 频道 / RESP2 静默丢弃三分派。
源码位置: networking.c:3354-3575,2832-2834,286-289,1001,1832-1837; tracking.c:46-81,164-193,255-311; commands.def:1542-1558
