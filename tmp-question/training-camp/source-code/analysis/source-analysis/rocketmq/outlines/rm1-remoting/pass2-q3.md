# 闭环笔记 q3: 分发核心 — processorTable 按 code 分派 + opaque 匹配

## 假设
请求按 code 分派到 (processor, 线程池) 对; 响应按 opaque 匹配 responseTable。

## 验证过程
- **类型分派** (processMessageReceived L177-189): REQUEST → processRequestCommand / RESPONSE → processResponseCommand
- **请求处理** (processRequestCommand L258-302):
  - `processorTable.get(cmd.getCode())` → 未匹配 → `defaultRequestProcessorPair` 兜底 (L260)
  - **Pair<NettyRequestProcessor, ExecutorService>** — 每个处理器绑独立线程池 (L260)
  - **rejectRequest()** (L286): 处理器可拒绝 (限流面) → 直接响应错误
  - RequestTask 提交线程池 (L297) — 异步处理, IO 线程不阻塞
  - 线程池满 → RejectedExecutionException 响应 (L302+)
- **响应处理** (processResponseCommand L385-400):
  - `opaque` → responseTable 匹配 (L388) → 移除 (L393)
  - 有 InvokeCallback → executeInvokeCallback (异步回调) / 否则 → putResponse + release (同步等待者唤醒)
  - 未匹配 → warn 日志 (过期/乱序响应)
- **responseTable** (L96): ConcurrentMap<opaque, ResponseFuture> — 请求 ID 表
- **writeResponse** (L207-245): opaque 回填 (L230) + markResponseType + channel.writeAndFlush + metrics (rpcLatency, 5.x)
- **RPCHook** (doBeforeRpcHooks/doAfterRpcHooks L191-205): 认证/统计挂点 (5.x 安全面接入)

## 代码类型
Implementation (请求-响应分发)

## 跨域关联
- RM-13 (Proxy): 认证 hook 挂点
- Netty (阶段1): IO 线程模型

## 结论
分发 = 请求按 code → (processor, 独立线程池) 对 (default 兜底) + rejectRequest 限流; 响应按 opaque → responseTable → 回调/唤醒; IO 线程零阻塞 (线程池异步)。
源码位置: NettyRemotingAbstract.java:96,177-302,385-400; NettyRequestProcessor.java
