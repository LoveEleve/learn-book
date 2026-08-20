# N-07 API 契约面 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. NamingService 接口 6 重载与实现端漏斗的关系? 接口定义了什么默认语义?
2. Instance/ServiceInfo 为什么能"传输与缓存双用"?
3. 精确订阅 (NamingEvent) 与模糊订阅 (FuzzyWatchChangeEvent) 的分层依据?
4. ConfigService 的"三种读 + 三种写"契约面?
5. Request 分型 (Internal/Server/业务) 的依据?

## B. 源码实证 (6)

6. NamingService 的 registerInstance 重载数? (grep api/naming/NamingService.java:46-100)
7. ServiceInfo 的关键字段? (grep pojo/ServiceInfo.java:36)
8. ConfigService.publishConfigCas 的参数? (grep L110-123)
9. AbstractSharedListener 的 fillContext 语义? (grep api/config/listener)
10. Payload 接口的职责? (grep api/remote/Payload.java:22)
11. RequestFuture/RequestCallBack 的异步契约? (grep api/remote)

## C. 推理深挖 (5)

12. PreservedMetadataKeys 的键被谁消费? (回链 ALI-A3 心跳键)
13. NamingSelector 与 clusters 参数的演进关系?
14. ConfigGrayInfo (灰度) 的契约语义? 3.x 新能力?
15. ability 能力协商怎么支撑 NC-1 的协议路由?
16. proto 自动生成与手写 Request 的双源怎么保持一致?

## D. 跨域扩展 (4)

17. 本域接口 vs NC-1 实现: 契约与实现的对应表?
18. 注解族 vs ALI-A2 NacosAnnotationProcessor: 契约消费方?
19. remote 协议 vs NC-3: 协议对象在 gRPC 通道的流转?
20. api 契约 vs openjdk 的类文件契约: 两种"契约面"的规划粒度对照?
