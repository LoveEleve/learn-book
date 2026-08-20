# 闭环笔记 Q5 — ServerCallImpl 状态机: headers→messages→close 单向推进

假设: 服务端调用的响应方向是严格状态机 — sendHeaders 一次 → sendMessage (unary 限 1) → close 一次, 顺序违规抛 IllegalStateException, 协议违规 (多响应/缺响应) 转 INTERNAL。

验证过程:
- **sendHeadersInternal** (ServerCallImpl.java:104-133): `checkState(!sendHeadersCalled)` / `checkState(!closeCalled)` (L105-106) — **一次性**
- **压缩协商在此**: 读客户端 MESSAGE_ACCEPT_ENCODING → 服务端选中的 compressor 不在客户端接受列表 → 降级 `Codec.Identity.NONE` (L111-121); 写 MESSAGE_ENCODING (实际压缩器) + MESSAGE_ACCEPT_ENCODING (服务端能力) (L124-131)
- `stream.writeHeaders(headers, !serverSendsOneMessage())` (L133) — **unary 时 headers 即 endStream**
- **sendMessageInternal** (L156-174): `checkState(sendHeadersCalled, "sendHeaders has not been called")` (L158) — **headers 必须先发**; unary 第二次消息 → `TOO_MANY_RESPONSES` INTERNAL (L162-164, 与 G-1 的 request(2) 双响应防护呼应); unary 不 flush (L170-171, close 统一冲)
- **setCompression** (L188-197): `checkState(!sendHeadersCalled, "sendHeaders has been called")` — 压缩器只能在 headers 前设置
- **closeInternal** (L216-230): `checkState(!closeCalled)` (L218) — **一次性**; unary 且 `!messageSent` → `MISSING_RESPONSE` INTERNAL (L221-223); finally reportCallEnded

代码类型: Implementation (状态机)

结论: 三阶段状态机 (未启动→发送中→已关闭) 由 checkState 硬编码; **两个语义层**: ① 调用方编程错误 (顺序违规) → IllegalStateException 立即失败 (运行时即时暴露) ② 协议违规 (多响应/缺响应) → INTERNAL Status (线上错误, 正常关闭流); **被放弃的方案: 柔性容错 (自动补 headers/忽略多响应)** — gRPC 选择严格状态机, 让编程错误即时暴露, 协议错误以清晰状态码关闭。压缩协商只发生在 headers 时刻, 之后不可变。 [跨域: G-1 双响应防护对称] (ServerCallImpl.java:104-133,156-174,188-197,216-230)
