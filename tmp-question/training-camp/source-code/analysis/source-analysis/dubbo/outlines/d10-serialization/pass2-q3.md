# D-10 序列化 — Pass 2 闭环 Q3: hessian2 实现族

> 核心: hessian2/ 9 文件 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: hessian2 序列化怎么实现? 类加载隔离? 对象图支持?**

## 机制链 (已实证)

```
Hessian2Serialization (hessian2/ 9 文件):
├── Hessian2Serialization (L41) — Serialization 实现 (HESSIAN2_SERIALIZATION_ID)
├── Hessian2ObjectInput / Hessian2ObjectOutput — 对象读写 (hessian2 编解码)
├── **Hessian2SerializerFactory** — 序列化器工厂 (自定义序列化器注册)
├── **Hessian2FactoryManager** — 工厂管理 (per-scope)
├── **Hessian2ClassLoaderListener** — 类加载器隔离 (反序列化用指定 ClassLoader)
└── HessianReflectionTypeDescriberRegistrar (aot) — GraalVM native 支持

hessian2 特性 (知识面):
├── 对象图支持 (循环引用/多态)
├── 紧凑二进制格式 (类型标记)
└── ClassLoader 敏感 (反序列化类加载)
```

## 关键设计 (why)

1. **对象图序列化**: hessian2 支持循环引用/继承多态 — RPC 复杂对象传输
2. **类加载器隔离**: Hessian2ClassLoaderListener — 多应用/多模块类隔离 (3.x ScopeModel 呼应)
3. **工厂管理**: Hessian2FactoryManager per-scope — 序列化器配置隔离
4. **native 支持**: aot Registrar — GraalVM 兼容面 (D-8b native 排除项呼应)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Hessian2Serialization | hessian2/Hessian2Serialization.java:41 |
| Hessian2ObjectInput/Output | hessian2/ |
| Hessian2ClassLoaderListener | hessian2/Hessian2ClassLoaderListener.java |
| Hessian2FactoryManager | hessian2/Hessian2FactoryManager.java |
| aot Registrar | hessian2/aot/HessianReflectionTypeDescriberRegistrar.java |

## 负面空间 (Q3 面)

- 不做跨语言全兼容 (Java 优先, 异构走 protobuf/json)
- 不做流式序列化 (hessian2 全量缓冲)
- 不做压缩 (压缩在 D-9 compressor 面)
