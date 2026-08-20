## §六 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

### A. 机制理解 (5)
1. 字节分类表为什么用 boolean[128] 查表而不是逐字节 if-else 链?
2. 严格解析与容忍解析的边界在哪? 为什么版本号校验有 "HTTP/" 例外?
3. relaxedPathChars/relaxedQueryChars 是全局的还是每请求的? 谁在构造 HttpParser 实例?
4. MimeHeaders 为什么用数组+count 而不是 Map? 查找复杂度是多少?
5. filter(Set<String>) 白名单在哪触发? 为什么需要它?

### B. 源码实证 (6)
6. HttpParser 有几个静态分类表? 各是什么? (grep boolean\[\])
7. IS_TOKEN 的判定条件? (grep 静态块)
8. MimeHeaders.recycle() 做了什么? (grep L125)
9. Parameters.processParameters 的字节级入口签名? (grep L230)
10. Rfc6265CookieProcessor 的 parseCookieHeader 签名? (grep L68)
11. urlDecode 的双入口 (ByteChunk/MessageBytes)? (grep L465/472)

### C. 推理深挖 (5)
12. 请求走私攻击怎么利用解析差异? Tomcat 的严格解析怎么封堵?
13. cookie 双实现 (RFC6265 vs legacy) 的切换机制? 为什么保留 legacy?
14. MimeHeaders 的 filter 在 HTTP/2 下怎么用? 头帧与 HTTP/1.1 头的差异?
15. 参数解析为什么在字节层做? 编码错误怎么处理 (decode 失败)?
16. 头部大小限制在哪实施? (MimeHeaders.setLimit 与 maxHttpHeaderSize 的关系)

### D. 跨域扩展 (4)
17. 本域 vs T-2 Http11Processor: 解析器的调用边界在哪?
18. 本域 vs T-9 WebSocket 握手: 复用哪些解析基础设施?
19. 本域 vs openjdk utf8 解码查表: 同构对照?
20. 本域 vs 网关 (Spring Cloud Gateway) 的请求解析: 严格性策略对比?

---

