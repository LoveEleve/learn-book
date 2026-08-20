# 03. 不写死 JVM 版本 — 内部结构解析与三种栈行走

> 🔴 Deep | 13 KP 中的 4 个(vmStructs 偏移/线程桥/三模式栈行走/codeCache)
> 读者处境: 信号处理器里要拿 Java 栈——但 JVM 内部结构(Klass/符号表)在不同 JDK 版本里偏移全不同。async-profiler 怎么做到 JDK 8~21 全兼容?

### 1. "运行时读偏移,不写死版本" — VMStructs

场景: 栈行走要读 Klass 名、符号表——但 HotSpot 内部结构是私有的,版本间偏移不同。

- `VMStructs::init(libjvm)`(vmStructs.cpp:134)→ `initOffsets`(:148)——**运行时从 libjvm 的导出表读取内部结构偏移**(如 `_klass_name_offset = *(int*)(entry + offset_offset)` :166)
- `resolveOffsets`(:445)完成判定;`_has_class_names = _klass_name_offset >= 0`(:470)——**无该结构的 JVM 降级**(不崩溃,少功能)
- 读的途径: libjvm 符号 + JNI 反射(java.lang.management 等)——**不依赖私有 API 的硬编码**
- [C++: 结构偏移 = 字段在对象内的字节位置;HotSpot 的 VMStructs 表(jdk 内部的 vmStructs.hpp 生成)是官方"内脏地图"——async-profiler 读取它,而不是自己猜]

关键设计: **兼容性 = 数据驱动**: 所有偏移在运行时从 JVM 自身读出——JDK 8 到 21 换个 JVM 也能跑(前提: 结构仍在)。`_has_class_names` 式降级保证"拿不到就少给,不崩"。这是"运行时反射式适配"的范例,与 Arthas 的反射桥(ArthasMethod,AR-2 篇 3)同哲学。

### 2. "信号里走栈的三条路" — walkFP / walkDwarf / walkVM

场景: 拿到 ucontext,怎么把 PC 链变成调用栈?

- `walkFP`(stackWalker.cpp:73): **frame pointer 链**——x86-64 的 RBP 链遍历;**最快**,但要求编译带 frame pointer(叶函数优化会断链)
- `walkDwarf`(:122): **DWARF 解帧**——按调试信息反解寄存器恢复规则;**无 FP 时用**,慢但全
- `walkVM`(:214): **VM 内部行走**——用 JVM 自己的栈布局知识(scope 解析 :363,内联帧展开!)——**JIT 内联帧只有它能展开**
- 选择策略: FP 可用→FP;否则 DWARF;Java 帧部分→VM 行走
- [x86: frame pointer 是 RBP 寄存器的链式结构(调用者地址+返回地址);DWARF 是 ELF 调试段的解帧规则;JIT 内联帧在 JVM 的 scope 描述里]

关键设计: **三模式递进**: FP(快,常见)→ DWARF(全,慢)→ VM(Java 专属,展开内联)——`[inlined]` 帧(AP-0 篇 3)只有 walkVM 能给。采样热路径用最快可用模式,失败再降级——"性能与完备的平衡"。

### 3. "地址 → JIT 方法" — CodeCache 索引

场景: 采样地址落在 JIT 编译的方法里——怎么知道是哪个方法?

- `CodeCache::add`(codeCache.cpp:78): 注册 JIT 方法(来自 CompiledMethodLoad 回调,AP-3 篇 1)→ `CodeBlob{start, end, name}`
- `findBlobByAddress`(:124): 地址二分查找(:126 区间判断)→ `binarySearch`(:133)
- 与栈行走配合: 地址在 JIT 区间 → walkVM 展开;在解释器 → JVMTI 帧

关键设计: **地址的双重身份**: 一个采样地址要么是 native(符号表,AP-4)、要么是 JIT 方法(CodeCache)、要么是解释器代码——**CodeCache 是"地址→Java 方法"的第三张映射表**(另两张: 符号表、JIT 内联 scope)。三张表合起来才能把任意地址变成可读帧。

---

跨域桥: 栈行走结果 = AP-4(frameName 命名);JIT 内联 = OpenJDK 域 16 Code Cache;字节码改写 = 上一篇;线程桥(nativeThreadId) = vmStructs 的配套。

**OpenJDK 关联**: [域 24 Frame & Stack — outlines/24-frame-stack/] — 栈行走的 JVM 帧布局;域 16 Code Cache — outlines/16-code-cache/(JIT 方法地址管理)。
