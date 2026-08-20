# D-10 序列化 — completeness-questions (全视角提问验证)

## 开发者视角

1. 序列化接口? (Serialization SPI)
2. 默认序列化? (hessian2)
3. 怎么换? (serialization 参数/系统属性)
4. optimizeSerialization? (类预注册)
5. hessian2 特性? (对象图/类加载隔离)
6. fastjson2 安全? (SecurityManager)
7. 附件怎么传? (writeAttachments)
8. protobuf? (triple 内, D-9)

## 架构师视角

9. 为什么 SPI 解耦? (协议与序列化独立演进)
10. 为什么默认 hessian2? (二进制高效+对象图)
11. 为什么 ID/content-type 双标识? (双通道)
12. 为什么类预注册? (反射优化)
13. 为什么类加载隔离? (多模块安全)
14. 为什么反序列化安全? (防 RCE)
15. 为什么动态开关? (性能/安全权衡)
16. 为什么三序列化并存? (场景选择)

## 学生视角

17. 什么是序列化? (对象→字节)
18. 什么是对象图? (循环引用/多态)
19. 什么是反序列化攻击? (RCE)
20. 什么是 ClassLoader 隔离? (类加载边界)
