# N-26 公共底座 — 序列化/包扫描/事件/常量面

> 前置: 无 (最深底座) | 对照: 全模块依赖的散装工具面
> 🟡 B | 方案 B (重要域) | 闭环: q1(序列化) q2(包扫描) q3(常量/事件面)

**读者处境**: common 剩下的能力 (序列化/包扫描/常量/事件) 怎么组织?

### 1. 序列化面 — Serializer 双实现

场景: 序列化怎么组织?
源码路径:
- **Serializer** (consistency/serialize/Serializer.java:28 + consistency/Serializer.java:28): 接口
- **JacksonSerializer** (consistency/serialize/JacksonSerializer.java:30): JSON
- **HessianSerializer** (consistency/serialize/HessianSerializer.java:37) + **NacosHessianSerializerFactory**: Hessian
- **SerializeFactory** (consistency/SerializeFactory.java:30): 工厂
- 消费: NC-5 协议序列化
关键设计 (q1): **"序列化双实现 = JSON/Hessian 可选"** — 工厂选择; 协议面 (NC-5) 消费。 [模式: 序列化工厂]

### 2. 包扫描面 — packagescan 32 文件

场景: 包扫描怎么实现?
源码路径:
- **packagescan/resource** (22): 资源扫描 (ClassLoader 面)
- **packagescan/util** (6) + **classreading** (2): 类读取
- 消费: AOT/启动类扫描
关键设计 (q2): **"包扫描 = 自实现类加载"** — 非 Spring 依赖的独立扫描器。 [模式: 独立扫描]

### 3. 常量/事件/日志面 — 散装工具

场景: 剩余底座?
源码路径:
- **constant/** (Symbols/ResponseHandlerType/RequestUrlConstants/HttpHeaderConsts): 常量面
- **event/** (ServerConfigChangeEvent): 服务端配置事件
- **logging/**: 日志门面
- **labels/ability/codec/lifecycle**: 标签/能力/编解码/生命周期
关键设计 (q3): **"散装底座 = 常量契约集中"** — 跨模块常量/能力/生命周期接口集中。 [模式: 常量底座]

### 4. 测试与行为锚

场景: 底座边界?
源码路径:
- 测试: common test (JacksonSerializerTest 等)
- 锚: SerializeFactory 选择
关键设计 (q1): **"工厂 = 序列化切换点"**。 [模式: 工厂契约]
