# 03. 地址链变成人话: 帧命名与缓存 — FrameName

> 🟡 Working | 13 KP 中的 3 个(JMethodCache/GetMethodName/classMap)
> 读者处境: Java 帧的"地址"其实是个 jmethodID(来自 ASGCT)——怎么变成 `com.demo.Foo.bar()` 并附上行号?

### 1. "jmethodID 到名字" — GetMethodName 与缓存

场景: 每次采样都调 JVMTI GetMethodName 太慢——命名必须缓存。

- `FrameName::javaMethodName(jmethodID)`(src/frameName.cpp:151-165): `GetMethodName(method, &method_name, &method_sig)`(:165)取方法名+签名
- **`JMethodCache _cache`**(frameName.cpp:75)——jmethodID→名字的缓存(采样热路径直接命中)
- 行号: 经 classMap(字节码偏移→源码行,与 AP-3 的 bytecode 改写共享信息)
- [JVMTI: GetMethodName 返回方法名+签名(分配内存,信号内不能调——所以命名在**非信号上下文**做,信号内只存地址)]

关键设计: **信号内只存"指针",信号外才命名**: 采样热路径(信号处理器)只把 jmethodID 存进调用栈存储(AP-4 篇 4),**命名是采样后的后台工作**(FrameName 在输出阶段跑)——这是"采样快、命名慢"的分工,保证信号处理器最小化。

### 2. "类名去哪拿" — classMap 批量收集

场景: 方法名有了,类名呢?

- `FrameName::FrameName(...)`(frameName.cpp:77-98): `Profiler::instance()->classMap()->collect(_class_names)`(:95)——**从 classMap 批量收集类名**(类加载时已记录)
- 与 AP-3 的关联: ClassLoad 回调(AP-3 篇 1 的 16 回调之一)维护 classMap——**类名在加载时就备好了**
- [JVM: 类加载事件 = 类名+Class 对象;classMap 是"类 ID→名字"的运行时字典]

关键设计: **命名所需的全部信息在事件层预收集**: 类名(ClassLoad 时)、方法名(GetMethodName 缓存)、行号(字节码信息)——采样器"平时攒料,采样时只用地址,输出时查料"。这就是为什么火焰图能秒级生成。

### 3. "native 帧的名字" — decodeNativeSymbol 与类型后缀

场景: native 帧是纯地址——符号解析后还要加工。

- `decodeNativeSymbol`(frameName.cpp:115): 去掉地址偏移(如 `libc.so.6+0x1234`)
- `typeSuffix`(:138): 帧类型标记——Java(黄色)/JIT 编译(native 绿)/解释器——**火焰图颜色语义的来源**(AP-0 篇 3 的读法)
- 内联帧: AP-3 篇 3 的 walkVM scope 展开 → `[inlined]` 标记

关键设计: **帧的"身份证"**: 每个帧 = (地址/方法 + 类型)——类型决定火焰图颜色 + 行走策略(Java 帧走 ASGCT, native 帧走符号表)。typeSuffix 是"类型 → 显示语义"的最后一公里。

---

跨域桥: 类名收集 = AP-3 篇 1(ClassLoad 回调);JIT 帧 = AP-3 篇 3(CodeCache);颜色语义 = AP-0 篇 3;方法名缓存与 Arthas JMethodCache 无直接对应(Arthas 用反射,AR-2 篇 3)。

**OpenJDK 关联**: [域 17 Threads — outlines/17-threads/] — 线程/方法命名的 JVM 侧信息源(GetMethodName 的 JDK 实现)。
