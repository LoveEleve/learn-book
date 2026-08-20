# D-10 序列化 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: Serialization SPI (hessian2 默认) + ObjectInput/Output + Hessian2ObjectInput/Output; 序列化 ID 协议头 |
| 2.7.x | SerializableClassRegistry (类预注册); SerializationOptimizer (optimizeSerialization) |
| 3.x | **fastjson2 替换 fastjson** (Fastjson2SecurityManager 安全面); DefaultSerializationSelector (系统属性/环境变量覆盖); DefaultMultipleSerialization (URL 自适应); protobuf 面入 triple |
| 3.3.x | dubbo-serialization 31 文件 (api 16 + hessian2 9 + fastjson2 6) 稳定 |

## 痕迹证据

- Serialization.java:36-37: @SPI(FRAMEWORK) + getContentTypeId/getContentType (2.x 锚)
- DefaultSerializationSelector.java: DEFAULT_REMOTING_SERIALIZATION = "hessian2" (2.x 锚)
- DubboProtocol.java:372,453: optimizeSerialization 双端挂钩 (2.7+ 锚)
- Fastjson2SecurityManager.java:38: implements AllowClassNotifyListener (3.x 锚 — fastjson2 换装)
- Hessian2ClassLoaderListener: 类加载隔离 (2.x 锚)
- HessianReflectionTypeDescriberRegistrar (aot): GraalVM 支持 (3.x 锚)

## 推断标注

- "2.x hessian2 默认" — 常量实证 (实证)
- "2.7 优化器" — SerializableClassRegistry 存在性推断 (标注)
- "3.x fastjson2" — 模块结构/类名实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- gRPC (G-1 ProtoBuf): 跨语言序列化 vs hessian2 Java 优先 — 对照
- Java 原生序列化: 安全/性能劣势 vs Dubbo 序列化抽象 — 对照
- JSON (fastjson2): 可读性 vs 二进制 — 场景对照
