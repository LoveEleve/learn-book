# 02. 地址到符号名: ELF 解析与 demangle — 符号解析

> 🔴 Deep | 13 KP 中的 3 个(ELF 多路径/demangle/归属 lookup)
> 读者处境: 行走器给出了一串地址(native 帧)——火焰图里要显示 `libc.so` 的 `__GI___clock_nanosleep`。地址怎么变成这个名字?

### 1. "ELF 里的符号表" — symtab 与 gnu_hash

场景: 每个 native 库是个 ELF 文件,符号在里面。

- `ElfParser::loadSymbols`(src/symbols_linux.cpp:505): 找 `.symtab` 段(:510,注释 "Parse debug symbols from the original .so")→ `loadSymbolTable` 装入(`sh_link` 连 strtab :512-513)
- `getSymbolCount`(:489): **gnu_hash 桶计算**(:490-499)——按哈希桶遍历符号
- `SymbolDesc`(:90): 符号描述符(名/地址/大小)
- [ELF: .symtab 是符号表(名+地址+类型),.strtab 是名字串;gnu_hash 是 GNU 扩展哈希(快速查找)]

关键设计: **直接解析 ELF,不用 dladdr**: dladdr 只能查导出符号,`.symtab` 能查全部(含静态/局部)——async-profiler 自己解析 ELF,拿到**完整符号集**,代价是解析代码(917 行 symbols_linux.cpp)与兼容性工作。

### 2. "符号没了怎么办" — build-id / debuglink / debuginfod

场景: 发布版 .so 常常 strip 掉 .symtab——符号去哪找?

- 解析顺序(symbols_linux.cpp:514): `loadSymbolsUsingBuildId() || loadSymbolsUsingDebugLink()`——**build-id 路径**(按 build-id 找外部 debug 文件)/**debuglink 路径**(.gnu_debuglink 指名的 debug 文件)
- `loadSymbolsFromDebuginfodCache`(:273): debuginfod 缓存(网络分发 debug 符号的新方式)
- [ELF: build-id 是文件唯一哈希(.note.gnu.build-id);debuglink 是 .gnu_debuglink 段指向的 debug 文件名——两者都能定位"符号的另一份拷贝"]

关键设计: **符号的"降级链"**: 主符号表 → build-id 外部文件 → debuglink → debuginfod——每层都是"符号完整度 vs 获取成本"的权衡,采样器在**后台线程**补符号(不阻塞采样热路径)。

### 3. "demangle: 名字的翻译" — C++ 与 Rust

场景: `_ZN4java4lang5ThreadC1Ev` 这种名字要变回 `java::lang::Thread::Thread()`。

- `Demangle::demangleCpp`(demangle.cpp:13): `abi::__cxa_demangle`(:15)——**标准 C++ demangle**
- `isRustSymbol`(:29): Rust V0(`_R` 前缀,:30)直接判定;**legacy 格式与 C++ 冲突**(:35-36,`_ZN...h...E` 看起来像 C++ 但要用 Rust 解)——需要区分逻辑
- [ABI: Itanium C++ ABI 的 mangling 规则;Rust V0 是自研方案(前缀 _R),legacy 复用 C++ 规则但哈希后缀不同]

关键设计: **两个 demangler 的分工**: C++ 用 libstdc++ 的 `__cxa_demangle`,Rust 用自己的规则(AP 淘汰清单里 rustDemangle.cpp 2039 行是完整实现)——符号名字的"语言指纹"判定(isRustSymbol)决定走哪个。这也是火焰图里能同时看到 `java::...` 和 `core::...` 的原因。

### 4. "地址属于哪个库" — lookup 归属

场景: 解析符号前,得先知道地址在哪个 .so 里。

- `_classes->lookup(lib_name/class_name)`(lookup.cpp:103-154): 地址 → 归属库/类(从 /proc/self/maps 的区间表)
- 配合: 归属库 → 该库的 ELF → 库内符号偏移 → 符号名

关键设计: **"先归属,再查名"**: 地址的解析是两段——先定位所属映射区间(库),再在库的符号表内查偏移——lookup 提供第一段,ElfParser 提供第二段。

---

跨域桥: 符号表消费方 = AP-3 篇 3(CodeCache 是 JIT 帧的"第三张表");native 帧行走 = 上一篇;地址→库归属 = lookup(与 /proc maps 关联)。

**OpenJDK 关联**: 符号解析为 ELF 层技术,与 JVM 无直接域;可对照 [域 27 JNI — outlines/27-jni/](native 符号的 JVM 侧使用)。
