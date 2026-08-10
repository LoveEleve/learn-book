# RAII 与智能指针

> **前置依赖**：读者已完成 Ch01-Ch03——new/delete/引用/构造/析构/深拷贝/移动语义/Rule of Five。理解"裸指针管理资源"的完整路径。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §19，深入理解C++11 §5

---

## 1. 引言：你不再需要手写 `delete`

Ch01 教了 `new`/`delete` 替代 `malloc`/`free`。Ch02 教了构造函数和析构函数——对象出生时自动初始化、死亡时自动清理。Ch03 教了移动语义——避免不必要的深拷贝。

但到现在为止，你仍然在**手写** `delete`——而手写 `delete` 是 C++ bug 的头号来源：忘记 delete→内存泄漏、delete 两次→double-free、delete 之后还用它→UAF。

本章给 Stage1 收尾——**用标准库的智能指针消除裸指针**。RAII 把"资源生命周期"绑定到"对象生命周期"——对象析构时自动释放资源，你不再需要手写 `delete`。Ch02 的 Rule of Three 和 Ch03 的 Rule of Five 在智能指针面前退化为 **Rule of Zero**——你一个字都不需要写，编译器替你管理一切。

---

## 2. RAII：C++ 最重要的设计模式

### 2.1 从 Ch02 的 File 类回顾

Ch02 §4 写了一个 `File` 类——构造函数 `fopen`，析构函数 `fclose`。这是 RAII 的经典实现：

```cpp
class File {
    FILE *f;
public:
    File(const char *path) : f(fopen(path, "r")) {
        if (!f) throw std::runtime_error("Cannot open");
    }
    ~File() { if (f) fclose(f); }
    operator FILE*() { return f; }
};

void read_file() {
    File f("data.txt");    // 构造函数——打开文件
    // ... 使用 f ...
    // 不管从哪个 return 退出——析构函数自动 fclose
}
```

**RAII 三个步骤**：获取资源（构造函数）→ 使用资源 → 自动释放（析构函数）。C 里每条代码路径上都要写 `fclose`——漏一条就是资源泄漏。RAII 让你写一次清理逻辑——编译器在**所有路径**上替你准时调用。

### 2.2 三个应用场景

RAII 不只是文件——任何需要"手工获取→手工释放"的资源都是 RAII 的适用场景：

| 场景 | 获取 | 释放 | 对应的标准库类型 |
|---|---|---|---|
| 堆内存 | `new` | `delete` | `unique_ptr`/`shared_ptr`（本章核心）|
| 文件句柄 | `fopen` | `fclose` | `File` 类（本章示例）|
| 互斥锁 | `mutex.lock()` | `mutex.unlock()` | `lock_guard<mutex>`（Stage5）|

**为什么说 RAII 是 C++ 最重要的设计模式**——它把 C 里最容易出错的"每条路径都要记得释放"变成了编译器和运行时的自动保证。不是"约定"，是"机制"。

---

## 3. `unique_ptr`——独占所有权

### 3.1 基本用法——离开作用域自动 delete

| 你在 C 里这样写 | C++11 里可以这样 |
|---|---|
| `int *p = malloc(sizeof(int)); *p = 42; free(p);` — 每个 malloc 配 free | `auto p = unique_ptr<int>(new int(42));` — 不用写 delete |
| 函数返回指针——调用方得记得 `free` | 函数返回 `unique_ptr`——所有权转移 |
| 指针被拷贝——两个指针指同一块，谁 free？ | `unique_ptr` 不能拷贝——独占，编译期禁止 |

```cpp
#include <memory>
using std::unique_ptr;

void basic_usage() {
    auto p = unique_ptr<int>(new int(42));   // 分配
    std::cout << *p << '\n';                  // 像指针一样解引用
}   // p 离开作用域 → 析构 → delete 内部的 int ——自动
```

### 3.2 独占——不能拷贝，只能移动

`unique_ptr` 的名字已经揭示了它的核心设计：**独一无二的所有权**。同一���对象只能有一个 `unique_ptr` 拥有它：

```cpp
auto p1 = unique_ptr<int>(new int(10));
// auto p2 = p1;            // ✗ 编译错误——unique_ptr 不能拷贝
auto p2 = std::move(p1);    // ✓ 移动——所有权从 p1 转移到 p2
// p1 现在是 nullptr——不能再用
```

这和 Ch03 的移动语义呼应——`unique_ptr` 只有移动构造函数和移动赋值运算符（拷贝构造被 `= delete` 了）。这不是限制——是保证。当你看到 `unique_ptr`，你就知道它是**唯一持有这块内存的人**，不会有人和你 share——不会有人偷偷 free 掉它。

### 3.3 异常安全——为什么 `make_unique` 更好（C++14）

C++11 创建 `unique_ptr` 的写法需要额外写 `new`：

```cpp
foo(unique_ptr<Widget>(new Widget), bar());   // ⚠️ 如果 bar() 抛异常→new Widget 泄漏
```

这是著名的**参数求值顺序陷阱**——C++ 不保证函数参数的求值顺序。如果编译器先 `new Widget`，再调 `bar()`（抛异常），再构造 `unique_ptr`——`new` 出来的 Widget 还没被 `unique_ptr` 接管就泄漏了。C++14 的 `make_unique` 消除这个陷阱：`foo(make_unique<Widget>(), bar());`——分配和包装是一步完成的。C++11 里你可以自己写一个 `make_unique`。

### 3.4 零额外开销——和裸指针一样大

```cpp
static_assert(sizeof(unique_ptr<int>) == sizeof(int*));  // 8 字节（64 位）
```

`unique_ptr` 没有引用计数、没有控制块——只有裸指针本身。解引用 `*p` 就和裸指针解引用一样——零额外指令。这是 C++ "零开销抽象"的具体体现。

### 3.5 和 C API 交互——`get()`、`reset()`、`release()`

你需要传裸指针给 C 函数时，用 `get()` 获取但不交出所有权：

```cpp
auto p = unique_ptr<int>(new int(42));
some_c_function(p.get());   // 传裸指针——p 仍然持有所有权，不需要 delete
```

`reset()`——释放当前对象，可选换一个新对象：

```cpp
p.reset(new int(100));   // 释放旧的 42→持有新的 100
p.reset();               // 释放 100→p 变成 nullptr
```

`release()`——放弃所有权，返回裸指针，**不调用 delete**：

```cpp
int *raw = p.release();   // p 放弃所有权→变成 nullptr，raw 现在持有 42
// 现在 raw 是裸指针——你必须自己 delete raw
delete raw;
```

**三个函数的对比**：

| 操作 | 释放旧对象？| 所有权？||
|---|---|---|---|
| `get()` | 否 | p 仍持有——C API 用的临时借出 |
| `reset()` | 是 | p 持有新的或变 nullptr |
| `release()` | **否** ——不调 delete | 调用方接管——必须自己释放 |

---

## 4. `shared_ptr`——共享所有权 + 引用计数

### 4.1 基本用法——最后一个用完的负责释放

有些资源需要多个指针共享——"最后一个用完的人负责关门"：

```cpp
#include <memory>
using std::shared_ptr;
using std::make_shared;

struct Node {
    int value;
    Node(int v) : value(v) {}
    ~Node() { std::cout << "Node(" << value << ") destroyed\n"; }
};

void shared_demo() {
    auto p1 = make_shared<Node>(1);    // 引用计数 = 1
    {
        auto p2 = p1;                    // 引用计数 = 2
        auto p3 = p1;                    // 引用计数 = 3
    }   // p2, p3 析构——引用计数降回 1
}   // p1 析构——引用计数归零 → delete Node
```

### 4.2 `shared_ptr` 的内部结构

`shared_ptr` 不是裸指针的简单包装——它内部有两个指针：

```
┌─────────────────┐     ┌──────────────┐
│ shared_ptr<T>   │     │ 堆上的对象    │
│ ┌─────────────┐ │     │ T object     │
│ │ ptr(T*)     │─┼────→│              │
│ │ 8 bytes     │ │     └──────────────┘
│ ├─────────────┤ │     ┌──────────────┐
│ │ ctrl_block* │ │     │ 控制块       │
│ │ 8 bytes     │─┼────→│ strong count │  ← 强引用计数
│ └─────────────┘ │     │ weak count   │  ← 弱引用计数
│ sizeof = 16     │     │ deleter      │  ← 删除器
└─────────────────┘     └──────────────┘
```

- **`sizeof(shared_ptr<T>)` = 16 字节**（64 位下）——两个指针。裸指针的 2 倍
- **引用计数是原子的**——多线程同时拷贝/析构 `shared_ptr` 时计数不会出错
- **对象本身不提供线程安全**——如果多个线程通过 `shared_ptr` 同时读写对象，你仍然需要 mutex

### 4.3 `make_shared` vs `new shared_ptr`——一次分配 vs 两次

```cpp
// make_shared——一次分配：对象和控制块在连续内存中
auto p1 = make_shared<Widget>(42);       // 推荐

// new + shared_ptr——两次分配：先 new Widget，再分配控制块
shared_ptr<Widget> p2(new Widget(42));   // 不推荐
```

**为什么 `make_shared` 更好**：(1) 一次 `new` vs 两次——更快；(2) 对象和控制块连续——更好的缓存局部性；(3) 异常安全——`make_shared` 是一步完成的。**优先 `make_shared`**。

**什么时候不能用 `make_shared`**：
1. **需要自定义 Deleter**——`make_shared` 不能传 Deleter，只能用 `shared_ptr<T>(new T, deleter)` 的构造函数
2. **大对象 + 长生命周期 `weak_ptr`**——`make_shared` 把对象和控制块分配在一起，即使对象已释放，只要还有 `weak_ptr` 指向控制块，整块内存（包括已释放的对象空间）都不能归还 OS。如果对象很大且 `weak_ptr` 长期存在→用 `new T` 分开分配，对象释放后对象空间立即归还

---

## 5. `weak_ptr`——打破循环引用

### 5.1 `shared_ptr` 的最大陷阱——循环引用

看这两个互相引用的类：

```cpp
struct B;
struct A {
    shared_ptr<B> b_ptr;
    ~A() { std::cout << "A destroyed\n"; }
};
struct B {
    shared_ptr<A> a_ptr;
    ~B() { std::cout << "B destroyed\n"; }
};

void cycle() {
    auto a = make_shared<A>();
    auto b = make_shared<B>();
    a->b_ptr = b;   // B 的引用计数 → 2（局部 b + A 内部 b_ptr）
    b->a_ptr = a;   // A 的引用计数 → 2（局部 a + B 内部 a_ptr）
}   // 局部 a 局部 b 析构 → 计数各降 1 → 各剩 1
    // 🔴 两个对象互相持有对方的 shared_ptr → 计数永远不归零 → 内存泄漏！
```

程序退出时 `~A()` 和 `~B()` 从来不打印——两个对象都在内存里，永远不释放。

### 5.2 `weak_ptr` 解环

`weak_ptr` 不增加引用计数——它只是一个"观察者"：

```cpp
struct B {
    weak_ptr<A> a_ptr;    // 🆕 换成 weak_ptr——不增加 A 的引用计数
};
struct A {
    shared_ptr<B> b_ptr;  // 保持 shared_ptr——A 持有 B 的所有权
};

// 现在 A 的引用计数只有局部 a 的 1——离开作用域归零→释放
// A 释放→内部 b_ptr 析构→B 的引用计数降为 0→B 也释放
```

**`weak_ptr` 怎么用**——你必须"提升"为 `shared_ptr` 才能访问对象：

```cpp
weak_ptr<A> wp = ...;
if (auto sp = wp.lock()) {   // lock() 尝试提升为 shared_ptr
    sp->do_something();      // 提升成功——sp 持有 A，引用计数 +1，保证访问期间不被释放
}
// sp 离开作用域——引用计数 -1
```

`lock()` 可能失败（对象已被释放）——返回空 `shared_ptr`。这就是 `weak_ptr` 的核心能力：安全地检查"对象还在不在"。

---

## 6. 自定义 Deleter——和 C 的 `malloc`/`fopen` 互操作

智能指针不只管理 `new` 出来的内存——你可以告诉它怎么"释放"：

```cpp
// malloc 分配的内存——不能用 delete 释放，必须用 free
auto p = unique_ptr<char, decltype(&free)>(       // 第二个模板参数——删除器类型
    (char*)malloc(100), free);                      // 第二个构造函数参数——删除器函数

// 文件句柄——析构时 fclose
auto file = unique_ptr<FILE, decltype(&fclose)>(
    fopen("data.txt", "r"), fclose);
// file 离开作用域 → fclose(file) 自动调用
```

**自定义 Deleter 的代价**：
- `unique_ptr<T, Deleter>`：Deleter 如果是函数指针→多 8 字节（sizeof 不再是裸指针大小）；如果 Deleter 是无状态的 lambda 或空类→**零额外开销**（empty base optimization）。
- `shared_ptr<T>` 的自定义 Deleter：不增加 `shared_ptr` 本身的大小——Deleter 存在控制块里。语法也不同——通过构造函数传：

```cpp
// shared_ptr 自定义 Deleter——不占 shared_ptr 大小
auto file = shared_ptr<FILE>(
    fopen("data.txt", "r"),    // 对象指针
    fclose                     // Deleter——通过构造函数传
);  // 不需要 decltype，不影响 sizeof
```

---

## 7. 可运行实验

### 实验 1：循环引用泄漏 vs weak_ptr 修复

```cpp
#include <iostream>
#include <memory>
using std::shared_ptr;
using std::make_shared;
using std::weak_ptr;

struct B;
struct A {
    shared_ptr<B> b_ptr;
    ~A() { std::cout << "~A\n"; }
};
struct B {
    shared_ptr<A> a_ptr;    // 🔴 循环引用
    ~B() { std::cout << "~B\n"; }
};

int main() {
    auto a = make_shared<A>();
    auto b = make_shared<B>();
    a->b_ptr = b;
    b->a_ptr = a;
    return 0;
    // 🔴 ~A() 和 ~B() 从不打印——内存泄漏
}
```

**修复**：`struct B { weak_ptr<A> a_ptr; };`——编译运行——`~A()` 和 `~B()` 都打印。

### 实验 2：`make_shared` 一次分配 vs `new shared_ptr` 两次分配

```cpp
#include <iostream>
#include <memory>

static int alloc_count = 0;
void* operator new(size_t size) {
    alloc_count++;
    std::cout << "  new(" << size << " bytes)\n";
    return malloc(size);
}
void operator delete(void* p) noexcept { free(p); }

struct Foo {
    int x;
    Foo(int v) : x(v) { std::cout << "  Foo(" << v << ") ctor\n"; }
    ~Foo()            { std::cout << "  Foo(" << x << ") dtor\n"; }
};

int main() {
    alloc_count = 0;
    std::cout << "=== shared_ptr<T>(new T):\n";
    { auto p1 = shared_ptr<Foo>(new Foo(42)); }
    std::cout << "  allocations: " << alloc_count << " (expect 2)\n\n";

    alloc_count = 0;
    std::cout << "=== make_shared<T>:\n";
    { auto p2 = make_shared<Foo>(42); }
    std::cout << "  allocations: " << alloc_count << " (expect 1)\n";
    return 0;
}
```

**编译运行**：`g++ -std=c++11 alloc.cpp && ./a.out`——`make_shared` 一次 `new`，`new T`+`shared_ptr` 两次。

---

## 8. 面试题

### 面试题 1：unique_ptr vs shared_ptr

**面试官**：`unique_ptr` 和 `shared_ptr` 的区别？什么时候用哪个？

**回答**：`unique_ptr` = 独占所有权——不能拷贝只能移动，`sizeof` 等于裸指针（零额外内存）。`shared_ptr` = 共享所有权 + 引用计数——多个指针指向同一对象，最后一个析构时释放。16 字节（两个指针）+ 引用计数的原子操作开销。优先 `unique_ptr`——只有确实需要多处共享资源时才用 `shared_ptr`。

**追问（面试官）**：`shared_ptr` 的引用计数是线程安全的吗？对象本身呢？

**追问回答**：引用计数是线程安全的——控制块里的计数操作是原子的（`atomic_fetch_add`）。但对象本身不提供线程安全——多线程通过 `shared_ptr` 同时读写对象仍需 mutex。简记：读 `shared_ptr` 本身→安全；写对象→需要锁。

### 面试题 2：循环引用与 weak_ptr

**面试官**：`shared_ptr` 的循环引用怎么导致内存泄漏？`weak_ptr` 怎么解环？

**回答**：A 持有 `shared_ptr<B>`、B 持有 `shared_ptr<A>`——互相让对方引用计数 +1。局部变量离开作用域后计数各剩 1→永远不归零→两个对象都不释放。`weak_ptr` 不增加引用计数——把其中一方的 `shared_ptr` 换成 `weak_ptr`，打破循环。

**追问（面试官）**：`weak_ptr::lock()` 失败了返回什么？怎么安全访问？

**追问回答**：`lock()` 返回空 `shared_ptr`——意味着对象已死。用 `if (auto sp = wp.lock())` 检查——提升成功才访问，失败跳过。这是 C++ 少数"线程安全地检查对象生死"的机制。

### 面试题 3：make_shared vs new shared_ptr

**面试官**：`make_shared<T>(args)` 和 `shared_ptr<T>(new T(args))` 的区别？

**回答**：`make_shared`——一次分配（对象+控制块连续）→ 更快 + 更好的缓存局部性 + 异常安全。`new T`+`shared_ptr`——两次分配→ 多一次内存操作。优先 `make_shared`。

**追问（面试官）**：什么时候不能用 `make_shared`？

**追问回答**：(1) 需要自定义 Deleter——`make_shared` 不支持；(2) 大对象 + 长生命周期 `weak_ptr`——`make_shared` 把对象和控制块放在一起，对象释放后控制块还在（因为有 `weak_ptr` 指向），导致大对象内存未回收。这时用 `new T` 分开分配。

---

## 9. Stage1 收尾：你学完了 C++ 的基础抽象

四章的递进关系：

```
Ch01 从 C 到 C++    → 你学会写 C++ 代码（引用/new/重载/命名空间/auto）
Ch02 类与构造析构   → 你学会管理对象生命周期（构造/析构/深拷贝/Rule of Three）
Ch03 移动语义       → 你学会避免不必要的拷贝（移动/RVO/Rule of Five）
Ch04 RAII 与智能指针 → 你学会让编译器替你管理资源（unique_ptr/shared_ptr/weak_ptr/Rule of Zero）
```

**Stage1 之后，你不再需要手写 `delete`。Stage2 预告：模板——让代码自动生成。**
