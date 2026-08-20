# 03. alloc 事件的双轨采集 — 符号 Hook 与字节码插桩

> 🔴 Deep | 17 KP 中的 3 个(AllocTracer hook/objectSampler/mallocTracer)
> 读者处境: `-e alloc` 的火焰图——"谁在 new 对象"是怎么被知道的?答案: 两条轨——hook 进 JVM 的分配事件,和在字节码里插采样点。

### 1. "第一轨: 钻进 JVM 的分配管道" — AllocTracer hook

场景: HotSpot 内部本来就有分配事件(AllocTracer),async-profiler 直接挂钩。

- `allocTracer.cpp:27-34`: 在 libjvm 里按**符号前缀**找 `AllocTracer::send_allocation_in_new_tlab`/`send_allocation_outside_tlab`——**多套签名依次尝试**(`_ZN11AllocTracer27send_allocation_in_new_tlab` 等)——不同 JDK 版本签名不同,前缀匹配做版本兼容
- 找到后 hook(替换符号入口)→ 分配发生时回调采样
- 配套: `AllocTracer::trapHandler`(profiler.cpp:689 注册的 SIGTRAP)——分配陷阱
- [C++: ELF 符号解析(lookup.cpp)+ 地址重写——与 AP-4 的符号表同一套基础设施;HotSpot 的 AllocTracer 是内部事件管道(TLAB 内/外分配分别上报)]

关键设计: **hook 而非重编译**: async-profiler 不修改 JVM,而是运行时在 libjvm 符号上做"补丁"——版本兼容靠"多套签名依次匹配"(:27-34 的 if-elseif 链)。这是"无侵入注入"的极致: 分配事件在 JVM 内部源头被拦截。

### 2. "第二轨: JVMTI 的采样分配事件" — objectSampler

场景: 符号 hook 有版本风险,JVMTI 提供了官方采样分配事件。

- `ObjectSampler : public Engine`(objectSampler.h:15): **JVMTI `SampledObjectAlloc` 事件回调**(objectSampler.cpp:134)——JVM 按采样率上报分配,async-profiler 只做回调记录
- `_allocated_bytes` 计数(objectSampler.cpp:14)驱动采样节奏;`recordAllocation`(:145)做栈回溯
- `initLiveRefs/dumpLiveRefs`(:159/:166): `--live` 模式跟踪存活对象
- 与 allocTracer 的对比: hook=符号级拦截(全量但版本敏感);JVMTI 事件=官方接口(采样率内置、稳定)
- [JVMTI: SampledObjectAlloc 是 JDK 16+ 的官方采样分配事件(G1 采样)——async-profiler 的 objectSampler 是它的直接消费方]

关键设计: **双轨策略**: 符号 hook 覆盖 JVM 内部(低开销),字节码插桩覆盖方法层(可控)——两条轨由 `_alloc_engine` 统一调度。这也解释了 AP-0 篇 2 的"alloc 事件机制预告"。

### 3. "native 分配也管" — mallocTracer

场景: native 内存(new/malloc)上涨,Java 栈上看不到。

- `mallocTracer.cpp`(254 行): 拦截 malloc/free(符号替换或 LD_PRELOAD)——`-e nativemem`/`-e nofree` 事件
- 与 hooks.cpp 的 dlopen_hook(hooks.cpp:95-110)配合——native 库加载后重新挂钩
- 输出: native 调用栈(经 AP-4 的 native 栈行走)

关键设计: **Java 与 native 一个火焰图**: 分配采样同时覆盖 Java(插桩/hook)与 native(malloc 拦截)——火焰图里能同时看到 `new byte[]` 和 `malloc` 的归属,这是纯 Java 工具(如 Arthas)做不到的。

---

跨域桥: 字节码改写 = AP-3(instrument.cpp 1280 行,本域只提消费);符号查找 = AP-4(lookup.cpp);native 栈行走 = AP-4(stackFrame);trapHandler 信号 = 上一篇(信号注册)。

**OpenJDK 关联**: [域 32 JFR — outlines/32-jfr/] — SampledObjectAlloc 是 JFR 同源的分配采样事件;域 25/26 GC(TLAB 分配路径)。
