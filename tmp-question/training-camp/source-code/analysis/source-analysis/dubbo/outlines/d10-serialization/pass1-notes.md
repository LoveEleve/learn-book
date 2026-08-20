# D-10 序列化 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-serialization 31 文件)
> 09 域级审计: 执行计划未覆盖 (31 文件) — PLAN 已记录; 序列化经 SPI 解耦 (协议不直接 import, PLAN §五: 12 import 全为 rpc.model)

## 入口展开 (Level-1~2, 已读源码)

### Level-1: Serialization SPI + 接口契约

```
Serialization @SPI(scope=FRAMEWORK) (common/serialize/Serialization.java:36-37):
├── getContentTypeId() — 序列化 ID (协议头用字节标识)
├── getContentType() — content-type (HTTP 面, D-8b mediaType 驱动呼应)
├── serialize(url, output) → ObjectOutput (写对象)
└── deserialize(url, input) → ObjectInput (读对象)

SPI 注册表 (internal 穷举, 4 项):
├── default=DefaultMultipleSerialization (多序列化组合, 默认)
├── wrapper=DefaultSerializationExceptionWrapper (D-1 Wrapper 织入, 异常包装)
├── hessian2=Hessian2Serialization (dubbo 协议默认)
└── fastjson2=FastJson2Serialization (json 面)

ObjectInput/ObjectOutput (api): 原始类型/对象读写 + 附件 (attachments)
```

### Level-2: 选择器 + 优化器 + 实现族

```
DefaultSerializationSelector (serialize/support/): getDefaultRemotingSerialization — 默认序列化选择
SerializationOptimizer: optimizeSerialization (D-2 挂钩: DubboProtocol:373) — 预注册优化类
SerializableClassRegistry (api): 可序列化类注册表

Hessian2Serialization (hessian2/ 9 文件):
├── Hessian2ObjectInput/Output — hessian2 对象读写
├── Hessian2SerializerFactory + Hessian2FactoryManager — 序列化器工厂管理
├── Hessian2ClassLoaderListener — 类加载器隔离 (反序列化类加载)
└── Hessian2Serialization (L41) + HESSIAN2_SERIALIZATION_ID

FastJson2Serialization (fastjson2/ 6 文件):
├── FastJson2ObjectInput/Output — fastjson2 对象读写
├── Fastjson2SecurityManager — 反序列化安全 (黑名单防护!)
└── FastJson2Serialization + Fastjson2CreatorManager

protobuf 面 (triple 内, D-9 已埋): PbUnpack/SingleProtobufUtils — 属 D-9 兼容面, 本域提钩子
```

## 09 域级审计表 (执行计划未覆盖 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| 执行计划未覆盖 | serialization 31 文件: api 16 + hessian2 9 + fastjson2 6 | 接受 (PLAN 已记录, D-10) |
| SPI 解耦实证 | 协议不直接 import serialization (PLAN §五: 12 全为 rpc.model) | 接受 |
| 执行计划未提: 序列化 ID | getContentTypeId — 协议头字节标识 | 补锚 |
| 执行计划未提: 优化器 | SerializationOptimizer + optimizeSerialization (D-2 挂钩) | 补锚 |
| 执行计划未提: 安全面 | Fastjson2SecurityManager — 反序列化黑名单 | 补锚 |
| 执行计划未提: 类加载隔离 | Hessian2ClassLoaderListener | 补锚 |

## 待展开 (下一层)

1. DefaultMultipleSerialization (默认组合 — 多序列化按 content-type?)
2. optimizeSerialization 完整 (SerializationOptimizer 类注册流程)
3. DefaultSerializationSelector 完整逻辑 (默认值来源)
4. Hessian2 序列化细节 (对象图/循环引用/ClassLoader)
5. 反序列化安全 (Fastjson2SecurityManager 黑名单)
