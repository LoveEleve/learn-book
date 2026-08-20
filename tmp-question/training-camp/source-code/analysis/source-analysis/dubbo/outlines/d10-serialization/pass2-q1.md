# D-10 序列化 — Pass 2 闭环 Q1: 抽象面 (Serialization SPI + ObjectInput/Output)

> 核心: common/serialize/Serialization + ObjectInput/ObjectOutput | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 序列化抽象出什么? 协议头怎么标识序列化方式?**

## 机制链 (已实证)

```
Serialization @SPI(scope=FRAMEWORK) (common/serialize/Serialization.java:36-37):
├── getContentTypeId() — 序列化 ID (协议头字节标识, 如 hessian2=2?)
├── getContentType() — content-type (HTTP 面, D-8b mediaType 驱动呼应)
├── serialize(url, output) → ObjectOutput
└── deserialize(url, input) → ObjectInput

ObjectOutput/ObjectInput (api 族):
├── 原始类型: writeBoolean/writeInt/writeUTF/writeObject...
├── 对象: writeObject/readObject
└── 附件: writeAttachments/readAttachments (传输上下文)

SPI 注册表 (internal 穷举, 4 项):
├── default=DefaultMultipleSerialization (按 URL 自适应组合, L25-43)
├── wrapper=DefaultSerializationExceptionWrapper (D-1 Wrapper 织入, 异常包装)
├── hessian2=Hessian2Serialization (dubbo 协议默认)
└── fastjson2=FastJson2Serialization (json 面)
```

## 关键设计 (why)

1. **SPI + URL 参数**: serialization= 参数选实现 — 协议与序列化解耦 (dubbo 协议可换序列化)
2. **ID + content-type 双标识**: 二进制协议用 ID (协议头), HTTP 用 content-type (D-8b) — 双通道适配
3. **ObjectInput/Output 契约**: 原始类型 + 对象 + 附件统一 — 序列化实现只写转换
4. **Wrapper 异常包装**: DefaultSerializationExceptionWrapper — 序列化异常统一 (D-1 机制应用)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Serialization @SPI + 契约 | common/serialize/Serialization.java:36-37 |
| ObjectOutput/ObjectInput | common/serialize/ |
| SPI 注册表 4 项 | dubbo-serialization 各模块 META-INF |
| DefaultMultipleSerialization | common/serialize/DefaultMultipleSerialization.java:25-43 |

## 负面空间 (Q1 面)

- 不做自动探测序列化 (URL 显式指定)
- 不做跨协议序列化兼容 (ID 固定绑定)
- 不做序列化版本协商 (版本由协议头)
