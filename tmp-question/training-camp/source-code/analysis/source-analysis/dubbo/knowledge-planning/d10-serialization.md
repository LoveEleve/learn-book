# D-10 序列化 — SPI 抽象与反序列化安全

> 项目: Dubbo | 🟡 Deep / 1 篇 | Serialization SPI+ObjectInput/Output+DefaultSerializationSelector+Hessian2Serialization+FastJson2Serialization+Fastjson2SecurityManager
> 基线: DUBBO-PLAN D-10 (支撑层, 31 文件) — 前置: **D-1 (SPI) + D-9 (protobuf 钩子)** — 展开 抽象→选择优化→实现族→安全面

---

## §0.8

- 🟡 Deep，1篇 — 抽象(**Serialization @SPI(FRAMEWORK) L36-37: getContentTypeId[协议头字节标识 hessian2=2/fastjson2=23 Constants.java:20,35]+getContentType[HTTP 面]+serialize/deserialize→ObjectInput/Output[原始类型+对象+附件 writeAttachments]; SPI 注册表 4 项[default=DefaultMultipleSerialization L25-50[serializeType 字符串 SPI 选+convertHessian 兼容]+wrapper=异常包装[ProxyObjectOutput L49-60]+hessian2+fastjson2]**) → 选择优化(**DefaultSerializationSelector: 默认 "hessian2"+覆盖链[系统属性→环境变量→默认]; SerializationOptimizer.getSerializableClasses+optimizeSerialization 流程在协议基类 AbstractProtocol L152-174[OPTIMIZER_KEY→isAssignableFrom 校验→实例化→注册循环]+SerializableClassRegistry.registerClass(clazz,serializer)[自定义序列化器]**) → hessian2 族(**Hessian2Serialization L41+Hessian2ObjectInput/Output+SerializerFactory+FactoryManager per-scope+ClassLoaderListener 类加载隔离+aot Registrar[GraalVM]; 对象图[循环引用/多态]**) → fastjson2+安全(**FastJson2Serialization+ObjectInput/Output+CreatorManager; Fastjson2SecurityManager implements AllowClassNotifyListener L38[checkSerializable 开关 L48+notifyCheckSerializable 动态通知 L89 — 防反序列化 RCE]**) → 三选择面(**hessian2 紧凑/fastjson2 可读/protobuf 跨语言[D-9 已埋]**)
- 设计模式: [模式: SPI 抽象+双实现族+安全一等公民]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Serialization.java:36-37 | 契约 | **getContentTypeId + getContentType + serialize/deserialize** — ID/content-type 双标识 | High |
| Constants.java:20,35 | ID | **HESSIAN2=2 / FASTJSON2=23** — 协议头字节标识 | High |
| DefaultMultipleSerialization.java:25-50 | 多序列化 | **serializeType 字符串 SPI 选 + convertHessian 兼容 (老 "hessian"→hessian2)** | High |
| DefaultSerializationSelector.java | 默认 | **默认 "hessian2" + 系统属性→环境变量→默认覆盖链** | High |
| AbstractProtocol.java:152-174 | 优化 | **optimizeSerialization: OPTIMIZER_KEY→校验→实例化→getSerializableClasses→registerClass 循环** | High |
| SerializableClassRegistry.java | 注册 | **registerClass(clazz) / (clazz, serializer) — 自定义序列化器可注册** | High |
| Hessian2Serialization.java:41 | hessian2 | **HESSIAN2_SERIALIZATION_ID 实现 + 对象图支持** | High |
| Hessian2ClassLoaderListener.java | 隔离 | **类加载器隔离** — 反序列化类加载边界 | High |
| Fastjson2SecurityManager.java:38-90 | 安全 | **AllowClassNotifyListener + checkSerializable 动态开关** — 防 RCE | High |
| DubboProtocol.java:372,453 | 挂钩 | **optimizeSerialization 双端 (export/refer) — D-2 实证** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 序列化单机制 (抽象+选择+实现+安全) — 1篇按四段展开; 传输/协议/安全三面汇合点 (全书收尾前)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | SPI 解耦 + ID/content-type 双标识 | 🔴 | **为什么🔴**: 协议与序列化独立 |
| P1-2 | 默认 hessian2 + 全局覆盖链 | 🔴 | **为什么🔴**: 默认语义 |
| P1-3 | optimizeSerialization 类预注册 | 🔴 | **为什么🔴**: 反射优化 |
| P1-4 | 反序列化安全 (类检查+动态开关) | 🔴 | **为什么🔴**: 防 RCE 一等公民 |
| P2-1 | hessian2 对象图 + 类加载隔离 | 🟡 | **为什么🟡**: 实现特性 |
| P2-2 | 三序列化场景 (紧凑/可读/跨语言) | 🟡 | **为什么🟡**: 选择面 |
| P3-1 | 异常 Proxy 包装 + 附件默认 | 🟢 | **为什么🟢**: 细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **SPI 抽象** | 🔴 | 主线 |
| B | **选择与优化** | 🔴 | 决策面 |
| C | **实现族** | 🟡 | 适配层 |
| D | **安全面** | 🔴 | 防护 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 抽象面 | SPI 解耦 (serialization= 参数); **ID 双标识: hessian2=2/fastjson2=23 (协议头字节) + content-type (HTTP 面 D-8b mediaType 呼应)**; 附件统一 writeAttachments | Serialization.java:36-37; Constants.java:20,35 |
| q2 | 选择优化 | 默认 hessian2 (系统属性/环境变量可全局换); **优化流程在协议基类 AbstractProtocol: OPTIMIZER_KEY→校验→实例化→类注册**; 自定义序列化器 registerClass(clazz,serializer) | DefaultSerializationSelector.java; AbstractProtocol.java:152-174 |
| q3 | hessian2 | 对象图 (循环引用/多态) + 类加载隔离 (Hessian2ClassLoaderListener) + 工厂 per-scope + GraalVM aot | Hessian2Serialization.java:41; Hessian2ClassLoaderListener.java |
| q4 | 安全面 | **Fastjson2SecurityManager (AllowClassNotifyListener): 反序列化类检查 + 动态开关** — 防 RCE (Java 安全重灾区); 三序列化场景: hessian2 紧凑/fastjson2 可读/protobuf 跨语言 | Fastjson2SecurityManager.java:38-90 |

→ 收尾: 序列化是传输/协议/安全三面汇合点; 对照 gRPC G-1 ProtoBuf。
