# FFI 与其他语言互操作

> **对应 Layer**：Layer 8（设计模式与工程化—FFI），参考 C-C++技术专家学习路线.md § Layer 8  
> **参考书章节**：极致C 第21章（与其他语言的集成）  
> **前置依赖**：S5Ch01（opaque pointer）；S2Ch03（ABI）  
> **主线标准**：C11/C17

---

## 1. 引言：C 是"通用语言"的真正含义

S1Ch01 说"C 是系统软件领域的通用语言"——但这句话还有一个更深的含义：所有主流语言都能调用 C 库。Python 的 `numpy` 核心是 C，Java 的 `JNI` 调用 C，Go 的 `cgo` 调用 C，Rust 的 `extern "C"` 调用 C。

**C 是计算机世界的"通用 AB"——几乎所有语言的 FFI 都以 C ABI 为目标。** 这一章不讲"每种语言的全部 FFI 细节"——那需要一本书。本章讲**C 端最佳实践**（如何设计一个能被任意语言调用的 C 库）+ 每种语言的**一个完整示例**（编译后可以运行）+ 每种语言的**性能代价和常见陷阱**。

---

## 2. C 端规范——对外接口的设计模式

### 2.1 最基本的要求

```c
// mylib.h
#ifndef MYLIB_H
#define MYLIB_H

// extern "C" — C++ 编译器使用，C 编译器忽略
// 对 FFI 最关键：确保符号名不被 C++ mangling 破坏
#ifdef __cplusplus
extern "C" {
#endif

typedef struct mylib mylib_t;  // opaque pointer — 隐藏实现

mylib_t *mylib_create(const char *config);
int      mylib_process(mylib_t *lib, const void *in, size_t in_len, void *out, size_t *out_len);
void     mylib_destroy(mylib_t *lib);

#ifdef __cplusplus
}
#endif

#endif
```

**为什么这是必须的**：
1. `extern "C"` —— C++ 编译器不改名符号（`mylib_process` 而非 `_Z13mylib_processP5mylibPKvS2_mPvPm`）
2. `opaque pointer` —— FFI 调用者不需要知道 struct 内部结构，只传指针
3. `char *` / `void *` —— 所有 FFI 都能传递原始指针（复杂类型需要序列化）
4. 每一层的输入/输出大小用独立参数 — FFI 不共享 C 的 `sizeof` 信息

### 2.2 内存所有权——谁分配、谁释放

```c
// 规则：C 端分配 → C 端释放
mylib_t *lib = mylib_create("config");   // C 端分配
mylib_destroy(lib);                       // C 端释放

// 反模式：让 FFI 调用者 free() C 库分配的内存
// Python/Java/Go 的分配器是它们自己的 — 交叉调用 = UB
void *buf = mylib_allocate(1024);    // buf 由 C 的 malloc 分配
free(buf);                           // Python/Java 的 free ≠ C 的 free → UB！
```

**规则**：C 库分配的内存必须由 C 库释放。FFI 调用者只传指针不传所有权。这是跨语言内存安全的唯一保证。

---

## 3. Python — cffi（最简洁）

### 3.1 完整示例

```python
# test_mylib.py
from cffi import FFI

ffi = FFI()

# 1. 声明 C 接口（从 .h 复制——cffi 解析 C 声明）
ffi.cdef("""
    typedef struct mylib mylib_t;
    mylib_t *mylib_create(const char *config);
    int      mylib_process(mylib_t *lib, const void *in, size_t in_len,
                           void *out, size_t *out_len);
    void     mylib_destroy(mylib_t *lib);
""")

# 2. 加载共享库
lib = ffi.dlopen("./libmylib.so")

# 3. 调用
handle = lib.mylib_create(b"config")
in_buf = b"hello"
out_buf = ffi.new("char[1024]")
out_len = ffi.new("size_t *", 1024)
lib.mylib_process(handle, in_buf, len(in_buf), out_buf, out_len)
result = ffi.string(out_buf, out_len[0])
lib.mylib_destroy(handle)
```

**两种模式对比**：

| | ABI 模式（`ffi.dlopen`） | API 模式（`ffi.set_source`） |
|---|---|---|
| 原理 | 直接加载 .so — 无需编译 | 编译 C 扩展（.so）— 类似 C 扩展模块 |
| 性能 | 较低（每次调用需类型转换） | 接近原生 |
| 部署 | 简单——一个 .py + .so | 需编译——需 gcc 环境 |
| 适用 | 快速原型、小规模调用 | 高频调用、生产环境 |

### 3.2 Python FFI 的常见陷阱

**陷阱 1：Python 的 `str` → C 的 `const char*` 需要编码**

```python
lib.mylib_create("config".encode("utf-8"))  # Python str → bytes
```

**陷阱 2：`ffi.string(buf, n)` 截断长度**——如果 `n` 比实际字符串长，`ffi.string` 继续读 → 越界。始终用 `out_len` 里的实际值。

---

## 4. Java — JNI（最常用但开销最大）

### 4.1 完整示例

```java
// MyLib.java
public class MyLib {
    static { System.loadLibrary("mylib"); }

    public static native long create(String config);
    public static native int process(long handle, byte[] in, byte[] out);
    public static native void destroy(long handle);
}
```

```bash
# 生成 JNI 头文件
javac -h . MyLib.java
# 生成：MyLib.h — 包含 JNI 函数签名
```

```c
// MyLib.c — JNI 实现
#include "MyLib.h"
#include "mylib.h"

JNIEXPORT jlong JNICALL Java_MyLib_create(JNIEnv *env, jclass cls, jstring config) {
    const char *cfg = (*env)->GetStringUTFChars(env, config, NULL);
    mylib_t *lib = mylib_create(cfg);
    (*env)->ReleaseStringUTFChars(env, config, cfg);
    return (jlong)(uintptr_t)lib;  // 指针转 jlong——Java 没有指针
}

JNIEXPORT void JNICALL Java_MyLib_destroy(JNIEnv *env, jclass cls, jlong handle) {
    mylib_destroy((mylib_t *)(uintptr_t)handle);
}
```

### 4.2 JNI 的性能代价

| 操作 | 开销 | 原因 |
|---|---|---|
| JNI 调用边界 | ~10-50 ns | 参数转换 + JNIEnv 切换 |
| `GetStringUTFChars` | ~50-200 ns | UTF-16 → UTF-8 编码转换 |
| `NewByteArray` | ~100-500 ns + GC | 堆分配受 GC 管理 |

**结论**：JNI 适合中等频率调用（~1000-10000/秒）——高频调用（百万/秒）不适合。可以用 JNI 桥接 C 库但不应用 JNI 做内部循环。

---

## 5. Go — cgo（性能最差，建议少用）

### 5.1 完整示例

```go
// #cgo LDFLAGS: -L. -lmylib
// #include "mylib.h"
import "C"
import "unsafe"

func main() {
    config := C.CString("config")    // Go string → C char*（堆分配）
    defer C.free(unsafe.Pointer(config))

    handle := C.mylib_create(config)
    defer C.mylib_destroy(handle)

    inBuf := C.CBytes([]byte("hello"))
    outBuf := make([]byte, 1024)
    var outLen C.size_t = 1024

    C.mylib_process(handle, unsafe.Pointer(&inBuf[0]), C.size_t(len(inBuf)),
        unsafe.Pointer(&outBuf[0]), &outLen)
}
```

### 5.2 cgo 为什么性能差

| 操作 | 开销 | 原因 |
|---|---|---|
| cgo 调用 | ~40-80 ns | goroutine 必须切换栈 + 操作系统线程锁——C 代码可能阻塞 Go 调度器 |
| `C.CString` | ~50-100 ns + GC | Go 字符串 → C 字符数组——堆分配 |
| `C.free` | ~10 ns | 释放 C 端分配的 Go 字符串缓冲区 |

**建议**：用 cgo 做**一次性初始化**（加载 C 库、调用 `create`/`destroy`），做数据传递，但不要用 cgo 做**每秒百万次调用**——这种情况下应重写 C 库的核心环路到 Go。

---

## 6. Rust — FFI（最自然的互操作）

### 6.1 完整示例

```rust
// build.rs — 告诉 Cargo 链接 libmylib
fn main() {
    println!("cargo:rustc-link-search=.");
    println!("cargo:rustc-link-lib=mylib");
}

// main.rs
use std::ffi::{CString, CStr};
use std::os::raw::c_char;

#[repr(C)]
struct MyLib { _private: [u8; 0] }  // opaque — 与 C 的 typedef struct mylib 对应

extern "C" {
    fn mylib_create(config: *const c_char) -> *mut MyLib;
    fn mylib_process(lib: *mut MyLib, input: *const u8, in_len: usize,
                     output: *mut u8, out_len: *mut usize) -> i32;
    fn mylib_destroy(lib: *mut MyLib);
}

fn main() {
    unsafe {
        let cfg = CString::new("config").unwrap();
        let lib = mylib_create(cfg.as_ptr());
        let out_len: usize = 1024;
        mylib_process(lib, b"hello".as_ptr(), 5, out_buf.as_mut_ptr(), &out_len);
        mylib_destroy(lib);
    }
}
```

**为什么 Rust FFI 最自然**：Rust 默认用 C ABI 调用外部函数——`extern "C"` 直接匹配 C 的符号名和调用约定，没有 JNI 的边界转换、没有 cgo 的 goroutine 切换。唯一的代价是 `unsafe` 块——提醒你这些调用不经过 Rust 的安全检查。

---

## 7. 进阶——回调、错误传递与返回值

### 7.1 回调——C 库调用你的函数

```c
// mylib.h
typedef void (*progress_cb)(int percent, void *user_data);

void mylib_register_progress_callback(progress_cb cb, void *user_data);
// C 库在处理过程中调用 cb(50, user_data) 报告进度
```

各语言的回调声明：
| 语言 | 回调声明方式 |
|---|---|
| Python cffi | `ffi.callback("void(int, void*)", my_py_cb)` |
| Java JNI | `env->CallVoidMethod(obj, methodID, percent, userData)` |
| Go cgo | `C.progress_cb(C.ProgressCallback(myGoCb))` — 需 `export` |
| Rust | `extern "C" fn my_cb(pct: c_int, data: *mut c_void)` |

**回调的所有权陷阱**：Python ffi.callback 返回的对象必须保持引用——被 GC 回收后回调仍然被 C 库调用 = 崩溃。

### 7.2 跨语言错误传递——errno 跨不了边界

问题：C 函数返回 `-1` 并设置 `errno`。跨语言如何读取？
```c
// 不可行——Python/Java/Go 没有 C 的 errno TLS
int result = mylib_process(handle, ...);
if (result < 0) printf("%s\n", strerror(errno));
```

**方案 1——返回错误码**（通用）：
```c
// 设计 FFI 友好的错误接口
const char *mylib_last_error(void);  // 返回错误字符串
```

**方案 2——通过输出参数传错误信息**：
```c
int mylib_process(..., int *error_code, char *error_msg, size_t msg_len);
```

### 7.3 返回 struct 值 vs 返回指针

```c
// C 端
struct result mylib_compute(void);  // 返回值——栈分配
struct result *mylib_compute_heap(void);  // 返回指针——堆分配
```

**为什么返回 struct 在跨 ABI 间不安全**：x86_64 上小 struct 用寄存器返回（`%rax`/`%rdx`），大 struct 用**隐藏指针参数**返回——不同编译器对大/小的判断阈值不同（GCC=16字节，Clang=16）。Python/Java 不知道 C 端编译器用寄存器还是隐藏指针——返回 struct 值的跨语言调用不是可移植的。

**规则**：FFI 友好的 C 库应该始终返回指针（堆分配）而非 struct 值。

---

## 8. 总结

| 语言 | 机制 | 性能 | 最佳场景 |
|---|---|---|---|
| Python | cffi（ABI 模式最简） | 中（类型转换） | 快速原型 |
| Java | JNI | 中（边界 + GC） | 中等频率桥接 |
| Go | cgo | 差（goroutine 切换） | 初始化 + 低频调用 |
| Rust | extern "C" | 高（零转换） | 直接替换 C——天然的互操作 |

**核心观点**：把 C 库设计为 FFI 友好的不是"加一个 extern 'C'"——这是错误的理解。真正的关键是**所有权边界**——C 库的内存由 C 库管理，不跨越 FFI 边界。技术专家不是"知道 JNI 怎么用"，而是"知道 JNI 的每次调用都有 Go 转 Java 转 C 的边界开销——如果被调函数 10ns 但 JNI 边界也 50ns，总体开销是 60ns 而非 10ns——这对调用频率的估算至关重要"。

---

## 验收要点

1. `extern "C"` 对 FFI 的作用是什么？为什么 C++ 编译器不改名符号？
2. 为什么 C 库分配的内存必须由 C 库释放？Python 的 `free` 为什么不能释放 C 的 `malloc` 内存？
3. cffi 的 ABI 模式和 API 模式的区别？为什么 ABI 更简单但 API 更快？
4. JNI 的调用开销来源有哪几个？为什么 JNI 不适合每秒百万次调用？
5. cgo 为什么性能最差？goroutine 切换和操作系统线程锁的作用？
6. 为什么 Rust 的 `extern "C"` 是最自然的 FFI？
---

## 面试题

**Q1：JNI 调用为什么有性能开销？拆解成几步？**

JNI 调用边界：1) Java→JNI 的参数复制（`GetStringUTFChars` → UTF-16 转 UTF-8，堆分配+复制），2) JNI→C 的调用（函数指针+调用约定匹配），3) C→JNI 的返回值转换（`ReleaseStringUTFChars` → 释放 JNI 临时内存），4) 返回 Java 时受 GC 影响（`NewByteArray` 在已分配 GC 内存中分配）。

**追问**："哪个开销最大？"——`NewByteArray` 受 GC 影响，在 Full GC 触发期间可能暂停整个 JVM。避免在 JNI 调用的热点路径中分配 Java 对象。

**Q2：为什么 cgo 性能最差？goroutine 切换具体发生了什么？**

Go 的 G/M/P 模型中，每个 G（goroutine）运行在 M（os 线程）上。cgo 调用时，当前 goroutine 被**锁定到当前 M**（`runtime.LockOSThread()`），其他 goroutine 无法复用该 M。cgo 调用返回后 M 被释放。如果 cgo 调用阻塞（如 C 库做了阻塞 I/O），被锁定的 M 也阻塞——Go 调度器无法利用该 CPU。

**追问**："什么场景下 cgo 是值得的？"——初始化阶段（加载 C 库、调用 create/destroy 一次）和数据传输（C 和 Go 之间传递大块数据）是值得的。每秒百万次小调用的场景不适用——应重写 C 核心环路到 Go。
