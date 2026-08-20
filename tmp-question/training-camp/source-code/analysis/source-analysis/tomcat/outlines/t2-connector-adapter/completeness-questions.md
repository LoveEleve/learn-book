# T-2 Connector + Adapter 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

> 格式升级: 旧多视角 12 问 → 新 A/B/C/D 四节 20 问 (2026-08-17 查漏补缺)

### A. 机制理解 (5)
1. 为什么需要 coyote 和 catalina 两层 Request/Response? 如果合并成一个类会有什么问题?
2. Http11NioProtocol 只有 76 行 — 它到底做了什么? 真正的 HTTP/1.1 解析逻辑在哪?
3. Protocol 和 Endpoint 的解耦 — 如果不分离, 换 NIO→APR 需要改多少代码?
4. mapper.map() 返回 void — 为什么用"传入可变对象→填充"而非"返回新对象"? 这对 GC 有什么影响?
5. URI 安全管线在 Adapter 层而非 Container 层 — 为什么? 放在 StandardEngineValve 里有什么问题?

### B. 源码实证 (6)
6. Http11NioProtocol 类声明与泛型参数? (grep coyote/http11/Http11NioProtocol.java:28)
7. recycledProcessors 池的声明与 pop 使用位置? (grep coyote/AbstractProtocol.java:786/888)
8. ADAPTER_NOTES 常量值与 getNote 读取位置? (grep catalina/connector/CoyoteAdapter.java:75/114)
9. response.getOutputStream() 与 getWriter() 的 IllegalStateException 触发点? (grep catalina/connector/Response.java:501-526)
10. request.recycle() 有多少行? 为什么不直接 new? (grep catalina/connector/Request.java)
11. CoyoteAdapter.service() 的方法签名与入口? (grep catalina/connector/CoyoteAdapter.java:303)

### C. 推理深挖 (5)
12. AbstractProtocol.release() 中 recycledProcessors.push(processor) — 如果池满了会怎样? 池容量谁决定?
13. proxyName/proxyPort 和反向代理场景 — Tomcat 怎么知道原始 Host header 是什么?
14. 一个 HTTP 请求从 accept() 到 servlet.service() 经历了哪些对象? 对象归属各在 coyote/catalina 哪一层?
15. keep-alive 连接上 Processor 的复用 — recycle 后 Processor 状态怎么清零? 哪些字段必须清?
16. Http11Processor 与 Http11NioProtocol 的分工 — 协议解析与 IO 模型怎么分离?

### D. 跨域扩展 (4)
17. 本域 vs T-4 线程模型: Processor 是线程安全的吗? 一个 Processor 一个线程还是一连接一个?
18. 本域 vs T-5 Mapper: mapper.map() 在 Adapter.service() 的哪个调用点? 顺序?
19. 本域 vs T-8 HTTP 解析: Http11Processor 怎么消费 MimeHeaders/Parameters? 解析器边界在哪?
20. 本域 vs Netty: Netty 的 HttpServerCodec vs Tomcat Http11Processor — 协议解析与 IO 解耦的异同?