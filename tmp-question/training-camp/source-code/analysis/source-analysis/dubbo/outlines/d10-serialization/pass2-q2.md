# D-10 序列化 — Pass 2 闭环 Q2: 选择与优化面 (Selector + Optimizer)

> 核心: DefaultSerializationSelector + SerializationOptimizer | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 默认序列化是什么? optimizeSerialization 优化什么?**

## 机制链 (已实证)

```
DefaultSerializationSelector (serialize/support/):
├── DEFAULT_REMOTING_SERIALIZATION = "hessian2" (默认!)
├── 覆盖顺序: 系统属性 (DUBBO_DEFAULT_REMOTING_SERIALIZATION) → 环境变量 → 默认 hessian2
└── getDefaultRemotingSerialization() — 选择入口

SerializationOptimizer (serialize/support/SerializationOptimizer.java) — 优化器接口:
└── getSerializableClasses() — 返回预注册类清单

⚠ optimizeSerialization 完整流程 (AbstractProtocol.java:152-174 — 协议基类!):
├── OPTIMIZER_KEY URL 参数 → 类名
├── isAssignableFrom(SerializationOptimizer) 校验 (L162)
├── 实例化 optimizer (L167) → getSerializableClasses() (L169)
└── 循环 SerializableClassRegistry.registerClass(c) (L173-174)
DubboProtocol:372 (export) / 453 (refer) 继承调用 — D-2 挂钩实证!
SerializableClassRegistry: registerClass(clazz) / registerClass(clazz, serializer) — 自定义序列化器也可注册
```

## 关键设计 (why)

1. **默认 hessian2**: 二进制高效 + 对象图支持 — dubbo 协议默认序列化
2. **属性/环境变量覆盖**: 全局换序列化不用改代码 (运维面)
3. **optimizeSerialization 双端挂钩**: export (D-2) + refer 都触发 — 类预注册优化
4. **多序列化组合**: DefaultMultipleSerialization — 不同场景可配不同序列化 (按 URL)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 默认 hessian2 + 覆盖链 | serialize/support/DefaultSerializationSelector.java:23-60 |
| SerializationOptimizer 接口 | serialize/support/SerializationOptimizer.java |
| optimizeSerialization 挂钩 | DubboProtocol.java:372,453 (D-2 实证) |
| SerializableClassRegistry | serialize/SerializableClassRegistry.java |

## 负面空间 (Q2 面)

- 不做运行时热切 (属性静态读取)
- 不做按方法序列化 (URL 级粒度)
- 不做自动回退 (序列化失败即错, 不降级)
