# D-10 序列化 — SPI 抽象→选择优化→实现族→安全面

> 前置: [[D-1-SPI微内核]] [[D-9-Triple协议]] | 引出: 无 (收尾链) | 对照: Java 原生序列化 + JSON/protobuf 认知
> 🟡 B | 8 KP | [模式: SPI 抽象 + 双实现族 + 安全]
> Pass 2 闭环: q1(抽象面) q2(选择优化) q3(hessian2) q4(fastjson2+安全)

**读者处境**: RPC 传输的对象怎么序列化? 默认为什么 hessian2? 反序列化安全怎么防? 这篇拆 Serialization SPI + 选择器 + 双实现族。

### 1. 抽象面 — Serialization SPI + ObjectInput/Output

场景: 序列化抽象出什么?
源码路径:
- **Serialization @SPI(FRAMEWORK)** (Serialization.java:36-37): **getContentTypeId** (⚠ 协议头字节标识实证: **hessian2=2 / fastjson2=23**, Constants.java:20,35) / getContentType (HTTP 面) / serialize/deserialize → ObjectInput/Output
- **ObjectInput/Output**: 原始类型 + 对象 + 附件 (writeAttachments)
- **SPI 注册表 4 项**: default=**DefaultMultipleSerialization** (L25-50: 按 **serializeType 字符串** SPI 选实现 + **convertHessian 兼容转换** 老 "hessian"→hessian2) / wrapper=异常包装 (D-1) / hessian2 / fastjson2
关键设计 (q1): **SPI 解耦 (serialization= 参数) + ID/content-type 双标识 + 附件统一**。[模式: 抽象面]

### 2. 选择与优化面 — Selector + Optimizer

场景: 默认序列化? optimizeSerialization 优化什么?
源码路径:
- **DefaultSerializationSelector**: **默认 "hessian2"** + 覆盖链 (系统属性 → 环境变量 → 默认)
- **SerializationOptimizer** (support/): 接口 = **getSerializableClasses()** (类清单) — **optimizeSerialization 流程在协议基类 AbstractProtocol** (L152-174): **OPTIMIZER_KEY 参数 → 类名校验 (isAssignableFrom) → 实例化 → getSerializableClasses → SerializableClassRegistry.registerClass(c)** — DubboProtocol:372 export / 453 refer 继承调用 (D-2 实证!); **registerClass(clazz, serializer) 支持自定义序列化器**
关键设计 (q2): **默认 hessian2 + 全局可换 + 类预注册优化**。[模式: 选择面]

### 3. hessian2 实现族

场景: hessian2 怎么实现?
源码路径:
- **Hessian2Serialization** (L41) + Hessian2ObjectInput/Output + **Hessian2SerializerFactory** + **Hessian2FactoryManager** (per-scope) + **Hessian2ClassLoaderListener** (类加载隔离) + aot Registrar (GraalVM)
- hessian2 特性: **对象图 (循环引用/多态) + 紧凑二进制**
关键设计 (q3): **对象图序列化 + 类加载隔离 (ScopeModel 呼应) + 工厂 per-scope**。[模式: 实现族]

### 4. fastjson2 + 安全面

场景: JSON 面怎么实现? 反序列化安全?
源码路径:
- **FastJson2Serialization** + FastJson2ObjectInput/Output + Fastjson2CreatorManager
- ⚠ **Fastjson2SecurityManager implements AllowClassNotifyListener** (L38): checkSerializable 开关 (L48) + notifyCheckSerializable 动态通知 (L89) — **防反序列化 RCE**
- 三选择面: hessian2 (紧凑) / fastjson2 (可读) / protobuf (跨语言, D-9 已埋)
关键设计 (q4): **安全一等公民 + 动态开关 + 三序列化场景选择**。[模式: 安全面]

## 代码类型
Architecture (SPI 抽象) + Security (反序列化防护)

## 负面空间 (D-10, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不自动探测序列化 | URL 显式指定 (q1) |
| 不运行时热切 | 属性静态读取 (q2) |
| 不跨语言全兼容 | Java 优先; 异构走 protobuf/json (q3) |
| 不流式序列化 | hessian2 全量缓冲 (q3) |
| 不自动白名单学习 | 静态配置 (q4) |
| 不加密序列化 | 加密在传输层 SSL (q4) |

## 结尾桥 OUTBOUND

- → 收尾: 序列化是传输/协议/安全三面的汇合点 — D-11 元数据后全书收尾
- → 对照: Java 原生序列化 (性能/安全劣势) / protobuf (跨语言) / JSON (可读)
- → 阶段 5.3 gRPC (G-1 ProtoBuf): 序列化对照
