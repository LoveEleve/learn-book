# 闭环笔记 Q8 — 工具面三件套: MetadataUtils/StreamObservers/StatusProto

假设: 三个小工具面承载 stub 层的实用设计: 静态头注入 (拦截器)、流控感知拷贝、google.rpc.Status 桥接。

验证过程:
- **MetadataUtils** (40): `newAttachHeadersInterceptor` (L50) 返回 **ClientInterceptor** 静态头注入 — "attach headers at stub level"; `newCaptureMetadataInterceptor` (L92) 捕获响应头/trailer; `newAttachMetadataServerInterceptor` (L168) 服务端附加 — 全部是拦截器工厂, 无 call 层侵入
- **StreamObservers** (27): `copyWithFlowControl` (L56-99) — 从 Iterator/Iterable 拷贝到响应流时**尊重目标流控** (isReady/onReadyHandler, 防背压溢出); `nextAndComplete` (L36) 工具
- **StatusProto** (protobuf/32): **google.rpc.Status ↔ gRPC 异常桥接** — `toStatusRuntimeException` (L51) 把 google.rpc.Status (含 details 扩展) 转 StatusRuntimeException; **`fromThrowable` (L153) 反向提取**; key = "grpc-status-details-bin" (L37) — 错误详情通过 **trailer 二进制元数据** 传输 (error details 协议, 面试点: grpc-status-details-bin)
- 测试: MetadataUtilsTest/StreamObserversTest/StatusProtoTest 存在 ✅

代码类型: Glue (桥接工具)

结论: 三件套: ① 头注入/捕获走拦截器工厂 (不改 call 语义) ② 流控拷贝防背压 (copyWithFlowControl 尊重目标流控) ③ StatusProto 是 google.rpc.Status (Rich Error Model) 与 gRPC 状态互转的桥 — 错误详情经 grpc-status-details-bin trailer 传输, 服务端可携带结构化错误 (code/message/details)。 (MetadataUtils.java:50,92,168; StreamObservers.java:36,56; StatusProto.java:37,51,153)
