# 域 AP-4: 栈行走与符号解析 — 知识规划

> 源码路径: src/stackFrame_x64.cpp(245行) + src/stackFrame_aarch64.cpp(353行) + src/frameName.cpp(403行) + src/dwarf.cpp(418行) + src/symbols_linux.cpp(917行) + src/demangle.cpp + src/lookup.cpp(187行) + src/callTraceStorage.cpp(323行) + src/linearAllocator.cpp/h + src/dictionary.cpp + src/tsc.cpp
> 源码量: ~12 文件,核心 ~3500 行
> 提取日期: 2026-08-10(v1 深读提取——范围规划声明已逐文件验证)
> 前置域: AP-3(栈行走入口与选择——本域讲实现与命名)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| stackFrame_x64.cpp:21-58 | **寄存器访问器**: `pc/sp/fp/link/arg0-3/jarg0`——从 ucontext 取寄存器值(`REG(RBP, rbp)` :30);`link()`(:37)取 frame link(返回地址) | High |
| stackFrame_x64.cpp:88-155 | **函数序言识别**: `push rbp`(0x55)检测(:123-127)——判定函数是否建了 frame pointer;**叶函数优化处理**(frame pointer omission 的补偿 :142-155) | High |
| frameName.cpp:75-98 | **JMethodCache + classMap**: `JMethodCache _cache`(:75)缓存 jmethodID→名字;`classMap()->collect`(:95)批量收集类名 | High |
| frameName.cpp:115-138 | **符号解码与类型后缀**: `decodeNativeSymbol`(:115,去地址偏移);`typeSuffix`(:138,Java/JIT/native 帧标记) | Medium |
| frameName.cpp:151-165 | **Java 方法命名**: `GetMethodName(method, &method_name, &method_sig)`(:165)——JVMTI 查方法名;行号经 classMap | High |
| symbols_linux.cpp:270-276 | **ELF 符号表解析**: `getSymbolCount`(gnu_hash 桶 :489-499)/`loadSymbols`(:505: symtab→build-id→debuglink 多路径 :514)/`loadSymbolTable`(:276)——地址→符号名 | High |
| symbols_linux.cpp:553- | **debug 符号补充**: `loadSymbolsFromDebug`(:553)/debuginfod 缓存(:273)——无 .symtab 时从 debug 文件/网络补符号 | Medium |
| demangle.cpp:13-36 | **符号 demangle**: `__cxa_demangle`(:15, C++ 符号);`isRustSymbol`(:29)——Rust V0 前缀 `_R` 判定,legacy 格式与 C++ 区分(:35-36) | High |
| dwarf.cpp:76-80 | **eh_frame 解析**: `parseEhFrame`(:76)——解析 eh_frame_hdr 头(version/enc 字段 :77-80)→ DW_CFA 操作码解码(:21-31) | High |
| callTraceStorage.cpp:67-143 | **调用栈存储**: 原子计数 `__sync_add_and_fetch`(:67);**overflow 错误帧**(:84,`storage_overflow` 哨兵);chunk 分配(CALL_TRACE_CHUNK :86);`collectTraces/collectSamples`(:120/:143) | High |
| linearAllocator.cpp:54-81 | **无锁线性分配器**: chunk 链(`allocateChunk` :54);**并发竞争重试**(:80-81,"It's probably being allocated right now, so let's compete")——信号处理器安全分配 | High |
| lookup.cpp:103-154 | **类/库名查询**: `_classes->lookup(lib_name/class_name)`——地址→归属库/类(配合 symbols) | Medium |
| tsc.cpp | **TSC 时钟**: 纳秒时间戳(锁采样/延迟分析的计时基) | Low |

*13 个知识点*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| 地址→可读帧的完整管线 | stackFrame(行走), symbols(符号), frameName(命名), lookup(归属), callTraceStorage(存储) | 采样热路径的 5 步链 |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| 符号解析多路径 | symbols_linux.cpp(ELF/build-id/debuglink/debuginfod) |
| 无锁分配 | linearAllocator, callTraceStorage(chunk) |
| 命名缓存 | frameName(JMethodCache), lookup(classes) |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| demangle | demangle.cpp |
| eh_frame 解析 | dwarf.cpp |
| TSC | tsc.cpp |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| 寄存器行走 + 序言识别(叶函数补偿) | "信号里怎么走栈"的实现细节;frame pointer omission 的经典问题 |
| 无锁线性分配器(竞争重试) | 信号处理器不能 malloc 的解决方案——"竞争就重试"的无锁设计 |
| ELF 符号多路径解析 | 地址→符号的完整答案(symtab/build-id/debuglink/debuginfod) |
| 调用栈存储(原子计数+overflow 哨兵) | 采样数据的存放与边界处理 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| demangle(C++/Rust 区分) | 符号可读性,rustDemangle 专项 |
| eh_frame 解析(DW_CFA) | DWARF 行走的基础 |
| JMethodCache/classMap | 命名缓存设计 |
| Java 方法命名(GetMethodName) | 采样后命名阶段的核心——信号外才可调 JVMTI |
| debug 符号补充(build-id/debuglink/debuginfod) | 无 .symtab 时的符号降级链 |
| 寄存器访问器(pc/sp/fp/link) | ucontext 到行走接口的架构抽象 |
| 类/库归属 lookup | 辅助符号解析 |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| 类型后缀/符号解码 | 格式化细节 |
| TSC 时钟 | 工具性 |

---

## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 采样拿到 PC → 行走成地址链 → 地址变符号 → 符号变可读帧 → 存入存储。

### 依赖图

```
01 寄存器行走                     ← 无前置
  ├─ 02 符号解析(demangle)        ← 依赖 01 (地址是输入)
  ├─ 03 帧命名                    ← 依赖 02 (符号→名字)
  └─ 04 存储与分配                 ← 依赖 01-03 (结果存放)
```

### 教学顺序

```
01 stackFrame 行走(pc/sp/fp/link + 序言识别 + 叶函数补偿)
  → 02 symbols_linux(ELF 多路径) + demangle(C++/Rust)
    → 03 frameName(JMethodCache/GetMethodName) + lookup(归属)
      → 04 callTraceStorage + LinearAllocator(无锁)
```

### 文章拆分 (4 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-register-walking.md | 寄存器行走 | 访问器/序言识别/叶函数补偿 |
| 2 | 02-symbol-resolution.md | 符号解析 | ELF 多路径/demangle/归属 lookup |
| 3 | 03-frame-naming.md | 帧命名 | JMethodCache/GetMethodName/classMap/行号 |
| 4 | 04-storage-alloc.md | 存储与无锁分配 | callTraceStorage/overflow/LinearAllocator/TSC |

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "被优化的叶函数没有 frame pointer,栈怎么走?" | 序言识别+编译器知识(01 篇) |
| "地址怎么变成 'com.demo.Foo.bar'?" | ELF→符号→demangle→GetMethodName 链(02/03 篇) |
| "信号里不能 malloc,采样数据放哪?" | 无锁线性分配器(04 篇) |
