# 闭环笔记 q3: DubboProtocol — Exporter + serverMap

## 假设
Protocol.export 侧: DubboExporter 注册 + server 启动 (缓存/双检/reset)。

## 验证过程
- **export** (DubboProtocol:346-375): **DubboExporter (invoker + serviceKey + exporterMap)** — 服务键索引; **openServer(url)** + **optimizeSerialization**
- **openServer** (L377-405): **serverMap 按地址缓存** + 双检锁 → **createServer(url)**; 已存在 → **server.reset(url) — override 支持** (配置覆盖面)
- **createServer** (L407-430+): URL 补参数 — **CHANNEL_READONLYEVENT_SENT (默认 true)** + **HEARTBEAT (DEFAULT_HEARTBEAT)** + **CODEC_KEY=DubboCodec**; transporter 检查 (Transporter 扩展存在性); **Exchangers.bind(url, requestHandler) — Netty 启动** (bind → NettyTransporter → NettyServer)
- **checkDestroyed** (L407): 销毁后拒绝导出
- **服务端复用**: 同地址多服务共享 server (client 也可导出 callback 服务)

## 代码类型
Implementation (Protocol 服务端)

## 跨域关联
- D-4: requestHandler (消息分发)
- D-1: Transporter 扩展 (自适应)

## 结论
Protocol 侧 = DubboExporter 注册 (exporterMap) + serverMap 缓存启动 + reset override + Exchangers.bind Netty。
源码位置: DubboProtocol.java:346-430
