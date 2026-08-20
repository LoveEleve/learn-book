# D-8b HTTP 传输栈 http12 — completeness-questions (全视角提问验证)

## 开发者视角

1. HttpChannel 抽象? (writeHeader/writeMessage)
2. h1 vs h2 差异? (完整消息 vs 流式帧)
3. 消息体怎么编解码? (mediaType 驱动)
4. 有哪些 codec? (binary/json/html/jsonpb)
5. 双栈怎么共存? (ProtocolSelectorHandler)
6. 写背压? (HttpWriteQueueHandler)
7. REST 映射? (@Mapping)
8. OpenAPI? (自描述文档)

## 架构师视角

9. 为什么协议无关通道? (上层无感)
10. 为什么 mediaType 驱动? (HTTP 原生语义)
11. 为什么双栈同端口? (端口节约)
12. 为什么写队列背压? (流式不压垮)
13. 为什么 jsonpb? (triple 协议场景)
14. 为什么注解驱动 REST? (静态声明)
15. 为什么禁用 content-type? (安全面)
16. 为什么统一 HttpChannel 出口? (h1/h2 隔离)

## 学生视角

17. 什么是 HTTP/2 流? (多路复用帧)
18. 什么是 content-type? (消息格式声明)
19. 什么是 REST 映射? (路径→方法)
20. 什么是背压? (写不下时等待)
