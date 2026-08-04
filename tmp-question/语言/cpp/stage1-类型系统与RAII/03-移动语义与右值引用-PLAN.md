# Stage1 Ch03 规划：移动语义与右值引用

> **定位**：Stage1 第三章。前提——读者已完成 Ch02（构造/析构/拷贝控制/Rule of Three），理解浅拷贝→double-free→深拷贝的完整路径。
> **主线版本**：C++11
> **参考书**：深入理解C++11 §3-4，C++程序设计语言 §19

---

## 1. 本章要解决的核心问题

Ch02 教了深拷贝——`Buffer b2 = b1;` 分配新内存、逐字节复制。但如果 `b1` 是一个临时对象（用完就扔），拷贝是纯浪费——为什么不直接把 `b1` 的资源"偷"过来？

这就是移动语义的核心：**C++11 让你区分"拷贝"（我不动你）和"移动"（你的给我，你清空）**。本章的目标是让读者理解三件事：

1. **什么时候拷贝、什么时候移动**——编译器怎么自动判断
2. **std::move 不是"移动"，是"允许移动的标记"**
3. **移动让 Ch02 的 Rule of Three 升级为 Rule of Five**

---

## 2. 节结构（5 节）

### §1 从"临时对象的浪费"开始

从 Ch02 的 Buffer 类出发——展示一个明显的浪费场景：

```cpp
Buffer make_buffer() {
    Buffer b("hello world");   // 在函数里创建
    return b;                   // 返回——触发一次拷贝！
}
Buffer b2 = make_buffer();     // 又触发一次拷贝！
// 总共：一次构造 + 两次拷贝（分配两次新内存、复制两次数据）
// 理想：一次构造 + 零次拷贝（直接把 b 的 data 指针给 b2）
```

- **核心问题**：`make_buffer()` 返回的 `b` 在函数结束后就销毁了——没人再用它。为什么要拷贝？直接转移所有权就好。
- **C++98 的遗憾**：编译器可以做 RVO（返回值优化），但不保证——如果 RVO 没生效，就是两次深拷贝。
- **C++11 的答案**：移动语义——让"把资源从一个对象转到另一个对象"成为语言的一部分。

### §2 左值与右值——"能取地址"vs"临时值"

这是理解移动语义的前提。用最简单的语言讲清楚：

| 概念 | 一句话 | 例子 |
|---|---|---|
| **左值（lvalue）** | 能取地址的、有名字的 | `int x = 5;` — `x` 是左值 |
| **右值（rvalue）** | 临时的、不能取地址的 | `5`、`x + y`、`get_string()` — 都是右值 |
| **左值引用** `T&` | 绑定左值 | `int &r = x;` ✓ / `int &r = 5;` ✗ |
| **右值引用** `T&&` | 绑定右值 | `int &&r = 5;` ✓ / `int &&r = x;` ✗ |

- **可运行实验**：试着写 `int &r = 5;`（编译不通过），然后写 `int &&r = 5;`（通过）——读者亲手感受左值引用和右值引用的语法区别。
- **核心理解**：右值引用 = "我可以绑定到一个临时的、即将销毁的值"——这是"移动"的前提：你绑定到一个快死的东西上，你可以放心地把它的资源转移到别处。

### §3 移动构造函数与移动赋值——如何写

从 Ch02 的 Buffer 类出发，加上移动构造函数和移动赋值：

```cpp
class Buffer {
    char* data;
    size_t size;
public:
    // Ch02 教的：拷贝构造——深拷贝
    Buffer(const Buffer& other) : size(other.size) {
        data = new char[size];
        memcpy(data, other.data, size);  // 拷贝数据
    }

    // 🆕 移动构造——转移资源
    Buffer(Buffer&& other) noexcept   // 注意：参数是右值引用 &&
        : data(other.data), size(other.size) {
        other.data = nullptr;   // 把源对象的 data 指针清空
        other.size = 0;         // 防止源对象的析构释放我们偷来的 data
    }

    // 移动赋值同理
    Buffer& operator=(Buffer&& other) noexcept {
        if (this != &other) {
            delete[] data;           // 释放自己的旧资源
            data = other.data;       // 偷对方的资源
            size = other.size;
            other.data = nullptr;    // 清空源对象
            other.size = 0;
        }
        return *this;
    }
};
```

- **移动构造 vs 拷贝构造**：拷贝构造的参数是 `const T&`（左值引用），移动构造的参数是 `T&&`（右值引用）。
- **`noexcept` 为什么重要**：如果移动构造不是 `noexcept`，`std::vector` 在扩容时不敢用移动（因为移动失败无法回滚拷贝前的状态）——只能走拷贝。所以移动构造**必须标记 `noexcept`**。
- **"偷"完之后**：源对象必须处于"可析构但不被使用"的状态——通常是把指针设为 nullptr。

### §4 `std::move` —— 不是我移动你，是我"允许"你被移动

本节纠正最常见的误解：`std::move` 不是魔法——它只是把左值转成右值引用（类型转换），不生成任何代码。

```cpp
Buffer b1("hello");
Buffer b2 = b1;              // 拷贝——b1 是左值，触发拷贝构造
Buffer b3 = std::move(b1);   // 移动——std::move 把 b1 变成右值引用，触发移动构造
// b1 现在空了！data 已经被偷走——后续不要再碰 b1
```

- **`std::move` 做了什么事**：`static_cast<T&&>(x)` ——纯类型转换，和目标语言没有运行时开销。
- **什么可以 move**：临时对象（编译器自动）、局部变量返回（编译器自动或手动 `std::move`）、不再使用的变量（手动 `std::move`）。
- **什么不该 move**：还有用的变量——move 之后源对象是"空壳"，再使用它是 bug。

### §5 Rule of Five + RVO —— 总结

**从 Rule of Three 到 Rule of Five**：Ch02 说如果你自定义了析构/拷贝构造/拷贝赋值中的任何一个，就需要全部三个。C++11 增加两个：

| 函数 | Ch02 讲的 | 本章加的 |
|---|---|---|
| 析构函数 | ✅ §3 | — |
| 拷贝构造函数 | ✅ §4 | — |
| 拷贝赋值 | ✅ §4 | — |
| **移动构造函数** | — | 🆕 §3 |
| **移动赋值** | — | 🆕 §3 |

**RVO/NRVO（返回值优化）**：编译器自动消除移动

```cpp
Buffer make() {
    return Buffer("hello");   // 构造临时对象——但编译器把它直接构造在调用方的栈上
}
Buffer b = make();  // 零拷贝、零移动——RVO 直接在 b 的位置构造对象
// 即使你写了移动构造，RVO 也会绕过它——直接原地构造
```

- 原则：**不要对返回值写 `std::move`**——会阻止 RVO。`return std::move(b);` 反而多一次移动。
- RVO 在 C++17 是强制要求的——C++11 不强制但所有主流编译器都做了。
- **怎么验证 RVO 是否生效**：给类加打印构造/拷贝/移动日志（像 §5 实验 2 那样），用 `g++ -std=c++11 -fno-elide-constructors` 关闭 RVO 看对比——打开时零输出，关闭时多一次 MOVE。

---

## 3. 编写方针

1. **每节的核心代码可编译运行**——用 Ch02 的 Buffer 类延伸，读者已经熟悉它
2. **§4 是关键节**——`std::move` 的误解极其普遍，必须写清楚"move 不移动"。
3. **§5 是收尾**——把 Ch02 的 Rule of Three 和本章的 Rule of Five 串起来，形成"类设计总纲"
4. 本章不讲的内容：完美转发（留到 Stage2 模板章节）、`std::forward`（留到 Stage2）、引用折叠（Stage2）。

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：移动构造 vs 拷贝构造

**面试官**：什么时候会调用移动构造函数而不是拷贝构造函数？移动构造做了什么？

**回答**：当初始化的源头是右值（临时对象）时调用移动构造——编译器自动选择。移动构造"偷"源对象的资源（把 `data` 指针拷过来）而不是分配新内存拷贝数据——所以快数个数量级。源对象被置为"空壳"（`data = nullptr`），防止析构函数重复释放。关键标志——参数类型是 `T&&`（右值引用）+ 标记 `noexcept`。

**追问（面试官）**：为什么移动构造必须标记 `noexcept`？不标记有什么后果？

**追问回答**：`std::vector` 扩容时——如果移动构造有 `noexcept`→用移动（快、安全）。如果没有 `noexcept`→用拷贝（因为移动失败无法回滚旧状态——旧对象已被偷，数据丢了）。这意味着你的 `vector<Buffer>` 在扩容时不走移动而走拷贝——性能退化到 Ch02 的水准。所以：移动构造/移动赋值、`swap`——永远标记 `noexcept`。

### 面试题 2：std::move 的真相

**面试官**：`std::move` 做了什么？为什么叫"move"但其实不移动任何东西？

**回答**：`std::move` 是纯类型转换——`static_cast<T&&>(x)`——把左值转成右值引用。零运行时开销。它不"移动"数据——它只是告诉编译器"把这个值当右值处理，触发移动构造而不是拷贝构造"。真正的移动发生在移动构造函数里。所以更准确的名字是 `std::enable_move` 或 `std::rvalue_cast`——但名字已经定下来了。

**追问（面试官）**：`return std::move(local_var);` 是好习惯还是坏习惯？

**追问回答**：坏习惯——阻止了 RVO（返回值优化）。编译器本来可以把 `local_var` 直接在调用方的栈上构造（零拷贝零移动），但 `std::move` 强制返回右值引用→编译器不能用 RVO→多一次移动构造。原则：`return local_var;` 让编译器决定——有 RVO 就零开销，没有 RVO 编译器自动走移动。不要手动 `std::move` 返回值。

### 面试题 3：Rule of Five

**面试官**：Ch02 的 Rule of Three 在 C++11 变成了 Rule of Five——多了哪两个？为什么需要？

**回答**：多了移动构造函数和移动赋值运算符。如果类管理堆内存，移动可以避免不必要的深拷贝——对于大型 Buffer，从 O(n) 内存分配+拷贝降到 O(1) 指针交换。Rule of Five 的完整逻辑——如果你需要自定义任何���个（析构/拷贝/移动），几乎肯定需要全部五个。因为"管理资源"意味着"需要控制生（构造）死（析构）转移（拷贝/移动）"。

**追问（面试官）**：什么时候 Rule of Five 可以简化为 Rule of Zero？

**追问回答**：Rule of Zero ——如果你的类成员全部是"自己管理自己的"类型（`std::string`、`std::vector`、`std::unique_ptr`），编译器自动生成的五个函数完全正确——你一个都不用写。这对应 Ch04 的 RAII——用标准库的类型而不是裸指针，让编译器替你管理。所以 Rule of Five 主要出现在"类内部用裸指针管理资源"的场景——而这个场景在现代 C++ 里越来越少（因为有智能指针）。

---

## 5. 可运行错误实验

### 实验 1：move 之后再使用源对象（UB）

```cpp
#include <iostream>
#include <cstring>

class Buffer {
    char* data;
    size_t size;
public:
    Buffer(const char* src) : size(strlen(src)+1), data(new char[size]) {
        memcpy(data, src, size);
    }
    Buffer(Buffer&& other) noexcept : data(other.data), size(other.size) {
        other.data = nullptr; other.size = 0;
    }
    ~Buffer() { delete[] data; }
    const char* c_str() const { return data ? data : "(stolen)"; }
};

int main() {
    Buffer b1("hello");
    Buffer b2(std::move(b1));   // b1 的 data 被偷走
    std::cout << "b2: " << b2.c_str() << "\n";    // "hello"
    std::cout << "b1: " << b1.c_str() << "\n";    // "(stolen)" — 空壳
    // 如果没把 other.data 设 nullptr，b1.c_str() 就是野指针 → UB
    return 0;
}
```

### 实验 2：`return std::move` 阻止 RVO

```cpp
#include <iostream>

struct Foo {
    Foo()           { std::cout << "  default ctor\n"; }
    Foo(const Foo&) { std::cout << "  COPY ctor\n"; }
    Foo(Foo&&)      { std::cout << "  MOVE ctor\n"; }
};

// 版本 A：正确——让 RVO 生效
Foo make_good() {
    Foo f;
    return f;   // 编译器用 RVO——直接在调用方构造，零拷贝零移动
}

// 版本 B：错误——std::move 阻止 RVO
Foo make_bad() {
    Foo f;
    return std::move(f);  // 强制 move——绕过 RVO，多一次移动构造
}

int main() {
    std::cout << "Good:\n";
    Foo g = make_good();    // 预期：只看到 "default ctor"（RVO 生效）

    std::cout << "\nBad:\n";
    Foo b = make_bad();     // 预期：看到 "default ctor" + "MOVE ctor"（move 多一次）
    return 0;
}
```

**编译运行**：`g++ -std=c++11 -fno-elide-constructors rvo_test.cpp && ./a.out`
- `-fno-elide-constructors` 关闭 RVO 看看区别
- 不加这个 flag（默认）——`make_good` 零输出，`make_bad` 多一次 MOVE

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | 左值/右值的取地址标准讲透；`noexcept` 与 vector 扩容的关系（不标记→回退拷贝）；移动置空源指针的设计原理；RVO 与 `std::move` 冲突的编译器视角 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（移动 vs 拷贝 / std::move 真相 / Rule of Five→Rule of Zero），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | move 后使用源对象的可运行实验（含空壳检测）；`noexcept` 不标记导致的 vector 性能退化；`return std::move` 阻止 RVO 的可编译对比实验 | 8/10 |
| 交叉引用 | §1 从 Ch02 Buffer 出发（Rule of Three→Five）；§5 收尾总结 Rule of Five 对照 Ch02 Rule of Three；Rule of Zero 引向 Ch04 智能指针 | 8/10 |
| **总分** | | **33/40** |
