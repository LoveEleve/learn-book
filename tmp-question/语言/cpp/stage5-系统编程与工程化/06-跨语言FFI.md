# 跨语言 FFI——从 `extern "C"` 到多语言集成

> **前置依赖**：读者已完成 Stage1-4 + Stage5 Ch01-05。理解 `name mangling`（C Stage2）、RAII（Stage1 Ch04）、异常安全（Stage3 Ch03）。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §15

---

## 1. 引言：C 和 C++ 之间有一道名字的墙

你在 C++ 项目里调用一个 C 库（libcurl、OpenSSL）——链接时报 `undefined reference to ...`。原因不是函数不存在——是**名字不匹配**。

C++ 编译器把 `void foo(int)` 变成 `_Z3fooi`（name mangling——为了支持重载）。C 编译器把 `void foo(int)` 变成 `foo`（不 mangling）。链接器找 `_Z3fooi` 找不到 → 报错。

**这就是 FFI 要解决的最基础问题——让 C++ 和 C 用同一个名字称呼同一个函数。**

---

## 2. `extern "C"`——拆掉名字的墙

### 2.1 语法和机制

```cpp
// C 库的头文件——同时兼容 C 和 C++ 编译器
#ifdef __cplusplus
extern "C" {         // C++ 编译器看到——"以下函数用 C 的符号名"
#endif

    void curl_init();
    int  curl_get(const char* url);
    void curl_close(void* handle);

#ifdef __cplusplus
}
#endif
```

`extern "C"` 告诉 C++ 编译器——这些函数用 C 的链接方式（不 name-mangle）。编译出来的 `.o` 里的符号名是 `curl_get` 而不是 `_Z8curl_getPKc`——和 C 库的符号名匹配。

**`#ifdef __cplusplus` 的作用**：C 编译器不认识 `extern "C"`——忽略；C++ 编译器定义了 `__cplusplus` 宏——执行 `extern "C"`。一个头文件同时兼容两种编译器。

### 2.2 不能用于什么

```cpp
extern "C" {
    void foo(int);         // ✓ 全局函数
    // void foo(double);   // ✗ 同名函数——C 不支持重载
    // class Bar { ... };  // ✗ C 没有类
    // template<typename T> void baz(T);  // ✗ 模板——C 没有
}
```

`extern "C"` 只能用于**全局函数**——C 能理解的接口。

---

## 3. C++ 代码暴露给 C——手动写 C wrapper

### 3.1 C++ 对象用 `void*` 隐藏

C 不认识 C++ 的类——把它包装成 C 风格函数：

```cpp
// ===== cpp_lib.h —— C++ 库的原始接口 =====
#include <string>
class Calculator {
public:
    int add(int a, int b)           { return a + b; }
    int multiply(int a, int b)      { return a * b; }
    std::string get_version() const { return "1.0"; }
};

// ===== cpp_lib_c.h —— 给 C 调用方的 wrapper =====
#ifdef __cplusplus
extern "C" {
#endif
    void* calculator_create();
    int   calculator_add(void* handle, int a, int b);
    int   calculator_multiply(void* handle, int a, int b);
    void  calculator_destroy(void* handle);
#ifdef __cplusplus
}
#endif

// ===== cpp_lib_c.cpp —— wrapper 实现层 =====
#include "cpp_lib.h"
#include "cpp_lib_c.h"

extern "C" {
    void* calculator_create() {
        return new Calculator();
    }
    int calculator_add(void* h, int a, int b) {
        try {
            return static_cast<Calculator*>(h)->add(a, b);
        } catch (...) {  // catch (...)——不只 std::exception——任何 throw 都要拦住
            return -1;   // 错误码——C 能理解
        }
    }
    int calculator_multiply(void* h, int a, int b) {
        try {
            return static_cast<Calculator*>(h)->multiply(a, b);
        } catch (...) {
            return -1;
        }
    }
    void calculator_destroy(void* h) {
        delete static_cast<Calculator*>(h);
    }
}
```

### 3.2 两个跨边界陷阱

**陷阱 1——忘了调 destroy**：C 调用方拿到 `void* handle` 后**必须**手动调 `calculator_destroy`。忘了调→`new` 出来的对象永远不会被释放。RAII 在跨语言边界失效了。

**陷阱 2——异常不能跨 C 边界**：C 的栈帧没有异常处理表（`.eh_frame`）。C++ 异常展开时不知道 C 帧里有什么资源要清理→UB。Wrapper 函数**必须** `catch (...)` 拦截所有异常——不只 `std::exception` 子类——`throw 42` 或自定义类型也要拦住→转成错误码（`return -1`）或 errno。

### 3.3 可运行实验——多文件编译

```bash
# 1. 编译 C++ wrapper 为共享库
g++ -std=c++11 -shared -fPIC cpp_lib_c.cpp -o libcalc.so

# 2. 写 C 测试程序
cat > test.c << 'EOF'
#include <stdio.h>
extern void* calculator_create();
extern int   calculator_add(void*, int, int);
extern void  calculator_destroy(void*);
int main() {
    void* calc = calculator_create();
    printf("5 + 3 = %d\n", calculator_add(calc, 5, 3));
    calculator_destroy(calc);
    return 0;
}
EOF

# 3. 编译 C 程序 + 链接 C++ 共享库
gcc test.c -L. -lcalc -o test
./test  # 输出: 5 + 3 = 8
```

---

## 4. 共享库 ABI——符号可见性

### 4.1 生产环境——默认隐藏所有符号

```bash
g++ -std=c++11 -shared -fPIC -fvisibility=hidden lib.cpp -o libmodule.so
```

`-fvisibility=hidden` 默认隐藏所有符号。只有显式标记的函数对外可见：

```cpp
// 对外导出
__attribute__((visibility("default")))
void public_api() { /* ... */ }

// 内部使用——不可见
void internal_helper() { /* ... */ }
```

### 4.2 为什么需要隐藏符号

| 问题 | 隐藏符号的好处 |
|---|---|
| 符号表膨胀 | C++ 模板每个实例化都生成符号——不隐藏的 `.so` 符号表可能比 C 等效大 10-100 倍 |
| 加载速度 | 隐藏的符号不参与动态链接查找——`.so` 加载更快 |
| 符号冲突 | 两个库都有 `internal_init()` → 冲突。隐藏后各自独立 |
| 编译器优化 | 隐藏符号——编译器确定外部看不到→激进内联 |

### 4.3 `-fPIC`——位置无关代码

`-fPIC`（Position-Independent Code）——共享库必须开启。C Stage2 已讲 PIC/GOT/PLT 机制——生成的代码通过 GOT 间接访问全局变量，通过 PLT 间接调用其他共享库的函数——让同一份代码可以被加载到不同进程的不同地址空间。

**MSVC 对照**：GCC 的 `-fvisibility=hidden` + `__attribute__((visibility("default")))` 在 MSVC 对应 `__declspec(dllexport)` / `__declspec(dllimport)`。

---

## 5. 多语言 FFI 概览

| 目标 | 方式 | 关键坑 |
|---|---|---|
| Python 调 C++ | pybind11（C++11 优先） | 对象生命周期——Python GC 和 C++ delete 谁管？pybind11 双向管理 |
| Go 调 C++ | cgo + `extern "C"` wrapper | C++ 异常不能跨 Go 栈——Go panic-recover ≠ C++ try-catch |
| JNI（Java 调 C++）| JNI + `extern "C"` 函数 | `GetStringUTFChars`→`ReleaseStringUTFChars` 必须配对——RAII wrapper 是必需品 |
| JavaScript 调 C++ | N-API / WebAssembly | 沙箱限制——不能直接访问系统资源 |

**通用铁律**：暴露给外部语言的**永远是** `extern "C"` 函数 + 不透明 `void*` handle。C++ 的对象模型（类/虚函数/RAII/异常）不能跨 FFI 边界。

---

## 6. 面试题

### 面试题 1：`extern "C"` 的本质

**面试官**：`extern "C"` 做了什么？为什么不能用于类的成员函数？

**回答**：`extern "C"` 告诉编译器"用 C 链接方式"——不 name-mangle 函数名。不能用于成员函数因为——成员函数需要 `this` 指针（隐含第一个参数）、需要属于一个类（C 没有类概念）、可能重载（C 不支持——关掉 mangling 后同名函数符号冲突）。`extern "C"` 只能用于**全局函数**——C 能理解的接口。

**追问（面试官）**：`extern "C"` 对异常有什么影响？

**追问回答**：没有直接影响——`extern "C"` 只影响符号名（链接阶段），不影响异常处理（运行时）。但间接影响——`extern "C"` 函数通常给 C 调用方用，C 代码没有异常处理表。如果 wrapper 里抛异常传到 C→UB。Wrapper 必须 catch 所有异常转成错误码。

### 面试题 2：C++ 异常跨 C 边界

**面试官**：C++ wrapper 里抛异常没 catch——传到 C 代码会怎样？

**回答**：UB——C 的栈帧没有异常处理表（`.eh_frame`）。C++ 异常展开时不知道 C 帧里有什么需要析构→可能跳过 C 帧中的资源清理（如 `fopen` 未 `fclose`）、直接落到更外层（如果外面还有 C++ catch）或 `std::terminate`。Wrapper 函数必须 catch 所有异常——用 try-catch 转成错误码（`return -1;`）或 errno。

**追问（面试官）**：C++→C wrapper 中忘了 `catch(...)`——所有平台都会 crash 吗？

**追问回答**：不一定——取决于编译器/平台的异常展开实现。GCC 的 Itanium ABI 可能在展开时跳过 C 帧继续向上找 handler（如果外层有 C++ catch）→只是资源泄漏不 crash。MSVC 的 SEH 实现可能直接 `std::terminate`。不管哪种实现——结果是"不可预期的"——必须 `catch`。

### 面试题 3：共享库符号可见性

**面试官**：`-fvisibility=hidden` 是什么？为什么 C++ 共享库需要它？

**回答**：默认隐藏所有符号——只有显式标记 `__attribute__((visibility("default")))` 的函数对外导出。C++ 共享库需要它因为——每个模板实例化在符号表生成条目、每个内联函数的符号在每个包含它的编译单元生成弱符号→不加 `hidden` 的 C++ `.so` 符号表可能比等效 C 库大 10-100 倍。隐藏后——加载更快、符号不冲突、编译器有更多优化机会。

**追问（面试官）**：MSVC 怎么做一样的事？

**追问回答**：GCC 的 `-fvisibility=hidden` + `__attribute__((visibility("default")))` 在 MSVC 对应 `__declspec(dllexport)` 显式导出 + 默认不导出。通常通过宏统一—— `#ifdef _WIN32 #define EXPORT __declspec(dllexport) #else #define EXPORT __attribute__((visibility("default"))) #endif` ——一份代码兼容两种编译器。

---

## 7. 本章小结

| 概念 | 核心理解 |
|---|---|
| `extern "C"` | 关掉 name mangling——C++ 和 C 共享同一个符号名 |
| C++→C wrapper | `void*` 隐藏 C++ 对象——`new`/`delete` 包装、异常转错误码 |
| 异常跨 C 边界 | 必须 catch 全部→转错误码——C 没有 `.eh_frame` |
| `-fvisibility=hidden` | 隐藏内部符号——减小 `.so` 大小 + 加速加载 + 防冲突 |
| FFI 通用铁律 | 跨语言边界永远 `extern "C"` + 不透明 handle |

**Stage5 系统编程进度**：Ch02 文件 I/O ✅ → Ch03 内存映射 ✅ → Ch04 进程管理 ✅ → Ch05 信号与 Socket ✅ → Ch06 跨语言 FFI ✅ → 下一章——工程化实战：Modern CMake / Google Test / clang-tidy / CI。
