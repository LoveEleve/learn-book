# Stage5 Ch02 规划：C++ 系统编程与 FFI

> **定位**：Stage5 第二章。前提——读者已完成 C 系统编程（syscall/IO/进程/信号/IPC/Socket）、C Stage2（符号表/name mangling）、C++ Stage1-4。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §15，C++编程之禅

---

## 1. 本章要解决的核心问题

C 和 C++ 代码经常共存——你维护的 C++ 项目可能需要调用已有的 C 库（libcurl、OpenSSL），或者你写的 C++ 库需要暴露给 C 调用方（如 Linux 内核模块的 C 接口）。C++ 和 C 的链接模型不同——C++ 的 name mangling 让 `void foo(int)` 在符号表里变成 `_Z3fooi`，C 期望它就叫 `foo`。

本章教两件事：extern "C" 怎么让 C++ 和 C 互调、以及生产环境中的共享库 ABI 管理。

---

## 2. 节结构（4 节）

### §1 `extern "C"`——C++ 调用 C 库

```cpp
// C 库头文件（libcurl.h）：
#ifdef __cplusplus
extern "C" {         // C++ 编译器看到这个——用 C 链接方式处理名字
#endif
    void curl_init();
    int  curl_get(const char* url);
#ifdef __cplusplus
}
#endif
```

- **`extern "C"` 做了什么**：告诉 C++ 编译器"这些函数用 C 的链接方式（不 name-mangle）"。编译出来的 `.o` 文件里的符号名是 `curl_get` 而不是 `_Z8curl_getPKc`——和 C 库的符号名匹配。C Stage2 Ch02 讲了符号表和 name mangling——`extern "C"` 就是"关掉 C++ 的 name mangling"。
- **`#ifdef __cplusplus` 的用途**：C 库里加这个保护——C 编译器看到时忽略 `extern "C"`（C 不认识这个语法），C++ 编译器看到时用上它。
- **`extern "C"` 只能用于函数——不能用于类/模板/重载函数**：C 没有类的概念，C++ 的 name mangling 是重载的基础——关掉 mangling 意味着重载同名函数会冲突。

### §2 C++ 代码暴露给 C——手动写 C wrapper

```cpp
// cpp_lib.h —— C++ 库的原始接口
class Calculator {
public:
    int add(int a, int b);
    int multiply(int a, int b);
};

// cpp_lib_c.h —— 给 C 调用方的 wrapper（只有 C 能理解的接口）
#ifdef __cplusplus
extern "C" {
#endif
    void* calculator_create();
    int   calculator_add(void* handle, int a, int b);
    void  calculator_destroy(void* handle);
#ifdef __cplusplus
}
#endif

// cpp_lib_c.cpp —— wrapper 实现层
extern "C" {
    void* calculator_create()  { return new Calculator(); }
    int   calculator_add(void* h, int a, int b) {
        return static_cast<Calculator*>(h)->add(a, b);
    }
    void  calculator_destroy(void* h) { delete static_cast<Calculator*>(h); }
}
```

- **C++ 对象的生命周期完全隐藏**：C 调用方看到的是一个不透明的 `void*` handle——不能访问成员、不知道大小、不知道实现。
- **⚠️ 内存泄漏陷阱**：C 调用方拿到 `void* handle` 后必须手动调 `calculator_destroy`——忘了调→`new` 出来的 Calculator 永远不会被释放。这又回到了 C 的每个 malloc 要配 free 的痛点——RAII 在跨语言边界失效了。好习惯：wrapper 文档里明确标注"必须调用 destroy"。
- **异常不能跨 C 边界**：C++ 抛异常传到 C 代码→UB。因为 C 的栈帧没有异常处理表（`.eh_frame`）——C++ 异常展开时不知道 C 帧里有什么要清理。Wrapper 函数必须 catch 所有异常→转成错误码或 errno：
```cpp
int calculator_add(void* h, int a, int b) {
    try {
        return static_cast<Calculator*>(h)->add(a, b);
    } catch (const std::exception& e) {
        errno = EINVAL;
        return -1;
    }
}
```

### §3 共享库 ABI——符号可视性

```bash
# 生产级——只导出显式标记的符号
g++ -std=c++11 -shared -fPIC -fvisibility=hidden lib.cpp -o libmodule.so
# 只有 __attribute__((visibility("default"))) 的函数被导出
```

- **`-fvisibility=hidden`（GCC/Clang）**：默认隐藏所有符号——只有显式标记 `__attribute__((visibility("default")))` 的函数在 `.so` 对调用方可见。减少符号表大小（加载更快）、防止符号冲突、让编译器有机会优化内部函数调用。
- **MSVC 等价写法**：`__declspec(dllexport)` 显式导出 + 默认不导出（需要 `/DEF` 文件或 `__declspec(dllimport)` 对调用方）。
- **C++ 符号膨胀问题**：每个模板实例化都在符号表里增加条目——不加 `visibility=hidden` 的 C++ 共享库符号表可能比等效 C 库大 10-100 倍。
- **`-fPIC`**：指针无关代码——共享库必须开启。C Stage2 Ch03 已讲 PIC/GOT/PLT 机制。

### §4 C++ 与脚本语言 FFI 概览

| 目标 | 方式 | 关键坑 |
|---|---|---|
| Python 调用 C++ | pybind11（C++11 优先） | 对象生命周期——Python GC 和 C++ delete 谁管？pybind11 双向管理 |
| Go 调用 C++ | cgo + extern "C" wrapper | C++ 异常不能跨 Go stack——Go panic-recover ≠ C++ try-catch |
| JNI（Java 调 C++）| JNI + extern "C" 函数 | `GetStringUTFChars`→`ReleaseStringUTFChars` 必须配对——RAII wrapper 是必需品 |

**核心原则**：C++ 的对象模型不能跨 FFI 边界——暴露给外部语言的永远是 C 风格的函数（`extern "C"`）+ 不透明 handle。

---

## 3. 编写方针

1. **从 C 链接器的基础知识出发**——读者在 C Stage2 学过符号表和 name mangling，本章是"extern 'C' 怎么关掉 name mangling"
2. **§2 的 C++→C wrapper 是本章灵魂**——可运行的多文件编译实验（见 §5）
3. 本章不讲的内容：SWIG 等自动 wrapper 生成器、COM 编程、完整 pybind11 教程

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：extern "C" 的本质

**面试官**：`extern "C"` 做了什么？为什么不能用于类的成员函数？

**回答**：`extern "C"` 告诉编译器"用 C 链接方式"——不 name-mangle 函数名。不能用于成员函数因为——成员函数需要 `this` 指针（隐含第一个参数）、需要属于一个命名空间/类（C 没有这些概念）、可能重载（C 不支持——关掉 mangling 后同名函数符号冲突）。`extern "C"` 只能用于**全局函数**——C 能理解的接口。

**追问（面试官）**：`extern "C"` 对异常有什么影响？

**追问回答**：没有直接影响——`extern "C"` 只影响符号名（链接阶段），不影响异常处理（运行时）。但间接影响——`extern "C"` 函数通常是给 C 调用方用的，C 代码没有异常处理表。所以 `extern "C"` wrapper 里抛异常传到 C→UB。Wrapper 必须 catch 所有异常转成错误码。

### 面试题 2：C++ 异常不能跨 C 边界

**面试官**：C++ wrapper 里抛异常没 catch——传到 C 代码会怎样？

**回答**：UB——不是规范的崩溃，是彻底的未定义行为。因为 C 的栈帧没有异常处理表（`.eh_frame` section）——C++ 异常展开栈帧时遇到 C 帧→不知道这里有什么需要析构→跳过→可能跳过 C 里 `malloc` 的资源。wrapper 必须 catch 所有异常——转成错误码或 `errno`。

**追问（面试官）**：`extern "C"` 对异常有什么影响？

**追问回答**：`extern "C"` 只影响符号名（链接阶段），不影响异常处理。但实践中——`extern "C"` 函数通常是给 C 或 FFI 调用方用的——它们的栈帧没有 `.eh_frame`——异常传播到这些帧→UB。所以 `extern "C"` 函数内部应该"不抛异常"（catch 所有异常）。

### 面试题 3：符号可视性

**面试官**：`-fvisibility=hidden` 解决什么问题？不加有什么后果？

**回答**：(1) 符号表缩小——只导出标记的函数，加载 `.so` 更快；(2) 防止符号冲突——两个 `.so` 的内部 Helper 类不会在全局符号表里冲突；(3) 编译器优化——内部函数可能被更激进内联。不加→C++ 模板生成的几百个符号全部暴露——符号表臃肿、冲突风险高。

**追问（面试官）**：MSVC 怎么实现同样的效果？

**追问回答**：MSVC 用 `__declspec(dllexport)` 显式导出 + DLL 默认不导出任何符号。GCC/Clang 用 `-fvisibility=hidden` 改变默认行为。跨平台代码用宏统一：`#ifdef _WIN32 #define EXPORT __declspec(dllexport) #else #define EXPORT __attribute__((visibility("default"))) #endif`。

---

## 5. 可运行错误实验

### 实验：extern "C" 包装 C++ 类——多文件编译

```cpp
// calc.h —— C++ 接口
class Calculator {
public:
    int add(int a, int b) { return a + b; }
};

// calc_c.h —— C wrapper 接口
#ifdef __cplusplus
extern "C" {
#endif
    void* calc_new();
    int   calc_add(void* h, int a, int b);
    void  calc_delete(void* h);
#ifdef __cplusplus
}
#endif

// calc_c.cpp —— wrapper 实现
#include "calc.h"
#include "calc_c.h"
extern "C" {
    void* calc_new()  { return new Calculator(); }
    int   calc_add(void* h, int a, int b) { return ((Calculator*)h)->add(a, b); }
    void  calc_delete(void* h) { delete (Calculator*)h; }
}

// main.c —— C 调用方
#include <stdio.h>
#include "calc_c.h"
int main() {
    void* h = calc_new();
    printf("1+2=%d\n", calc_add(h, 1, 2));
    calc_delete(h);  // 别忘了！
    return 0;
}
```

**编译与运行**：
```bash
g++ -std=c++11 -c calc_c.cpp -o calc_c.o
gcc -std=c99  -c main.c    -o main.o
g++ calc_c.o main.o -o app
./app  # 输出：1+2=3
```

**核心体验**：C++ 编译 `.cpp`→生成类+name mangling→`extern "C"` 关掉 wrapper 的 mangling→C 编译 `.c` 找不到 wrapper 的符号。C++ 链接器最后把两者接到一起。

---

## 6. 4 维度自检

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | `extern "C"` 关掉 name mangling 的链接层面原理（和 C Stage2 Ch02 符号表知识的承接）；异常跨 C 边界 UB 的 `.eh_frame` 缺失根因；visibility hidden 让编译器更激进内联的优化原理 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（extern "C" 本质+异常 / 异常跨 C 边界 / 符号可视性+GCC/MSVC 对比），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | wrapper 内存泄漏陷阱（C 调用方忘记 destroy）；异常跨 C 边界可运行 wrapper 代码；MSVC `dllexport` 在跨平台构建中的注意事项 | 8/10 |
| 交叉引用 | §1→C Stage2 Ch02 name mangling 和符号表（`extern "C"` ="关掉 name mangling"的实际应用）；§3→C Stage2 Ch03 PIC/GOT/PLT；§2→Stage1 Ch04 RAII（跨 C 边界 RAII 失效） | 8/10 |
| **总分** | | **33/40** |
