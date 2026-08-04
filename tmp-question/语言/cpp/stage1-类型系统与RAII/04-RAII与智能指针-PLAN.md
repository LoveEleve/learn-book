# Stage1 Ch04 规划：RAII 与智能指针

> **定位**：Stage1 第四章（最后一章）。前提——读者已完成 Ch01（new/delete/引用）、Ch02（构造/析构/Rule of Three）、Ch03（移动语义/Rule of Five/Rule of Zero）。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §19，深入理解C++11 §5

---

## 1. 本章要解决的核心问题

Ch02 教了怎么用构造函数和析构函数管理资源——构造函数 `fopen`，析构函数 `fclose`。Ch03 教了怎么高效转移资源——移动语义避免了不必要的深拷贝。

本章收尾 Stage1：**用标准库的智能指针消除裸指针——让 Rule of Five 变成 Rule of Zero。**

在 C 里，每个 `malloc` 都要对应一个 `free`，每个 `fopen` 都要对应一个 `fclose`，而且你必须在所有代码路径上记得这件事（包括错误返回）。C++ 的 RAII（Resource Acquisition Is Initialization，资源获取即初始化）把"资源生命周期"绑定到"对象生命周期"——对象析构时自动释放资源。

智能指针是 RAII 在内存管理上的标准实现：**你不写 `delete`——智能指针替你准时调用。**

---

## 2. 节结构（5 节）

### §1 RAII：C++ 最重要的设计模式

从 Ch02 §3 的 File 类出发——展示 RAII 的核心思想：

```cpp
// C 里这样——每一个 return 前都要 fclose
FILE* f = fopen("data.txt", "r");
if (!f) return -1;
char buf[256];
if (fgets(buf, sizeof(buf), f) == NULL) { fclose(f); return -1; }  // 别忘了！
// ...
fclose(f);

// C++ 里这样——析构函数自动 fclose，不管从哪条路退出
class File {
    FILE* f;
public:
    File(const char* path) : f(fopen(path, "r")) {}
    ~File() { if (f) fclose(f); }
    operator FILE*() { return f; }
};
File file("data.txt");   // 打开
if (fgets(buf, sizeof(buf), file) == NULL) return -1;  // 不用 fclose！
// file 离开作用域 → 析构函数自动 fclose
```

- **RAII 三个步骤**：获取资源（构造函数）→ 使用资源 → 自动释放（析构函数）
- **三个应用场景**：文件句柄（`fopen`/`fclose`）、互斥锁（`lock`/`unlock`——Ch05）、动态内存（`new`/`delete`——本章核心）
- **为什么说 RAII 是 C++ 最重要的设计模式**：它把 C 里最容易出错的事（忘记释放资源）变成编译器和运行时的自动保证

### §2 `unique_ptr` —— 独占所有权

| 你在 C 里这样写 | C++11 里可以这样 |
|---|---|
| `int* p = malloc(sizeof(int));` `*p = 42;` `free(p);` — 每个 malloc 要配 free | `auto p = std::unique_ptr<int>(new int(42));` — 不用写 delete |
| 函数返回指针——调用方得记得 `free` | 函数返回 `unique_ptr`——所有权转移，接收方负责释放 |
| 指针被拷贝——两个指针指向同一块，谁 free？ | `unique_ptr` 不能拷贝——独占所有权，编译期禁止 |

```cpp
#include <memory>

// 基本用法——离开作用域自动 delete
void basic_usage() {
    auto p = std::unique_ptr<int>(new int(42));
    std::cout << *p << "\n";   // 42
}  // p 析构 → delete 内部的 int ——自动

// 所有权转移——不能拷贝，只能移动
void transfer() {
    auto p1 = std::unique_ptr<int>(new int(10));
    // auto p2 = p1;           // ✗ 编译错误——unique_ptr 不能拷贝
    auto p2 = std::move(p1);   // ✓ 移动——p1 变成 nullptr
    // p1 现在是空——不能再用
}
```

- **核心理解**：`unique_ptr` = "有且仅有一个指针拥有这块内存"。不能拷贝（两个所有权说不清），只能移动（所有权转移）。
- **`make_unique`（C++14，但 C++11 可以手写）**：`auto p = std::unique_ptr<int>(new int(42));` 有两次写法——手写 `new` + 包装。标注"C++14 起有 `std::make_unique`，C++11 可以自己写一个"。
- **⚠️ 异常安全问题**：`foo(unique_ptr<T>(new T), bar());`——如果 `bar()` 抛异常，`new T` 出来的裸指针还没被 `unique_ptr` 接管→泄漏。`make_unique` 消除这种风险：`foo(make_unique<T>(), bar());`——分配和包装是一体的。
- **和裸指针的大小一样**：`sizeof(unique_ptr<T>) == sizeof(T*)` ——零额外内存开销。

### §3 `shared_ptr` —— 共享所有权 + 引用计数

有些资源需要多个指针共享——最后一个用完的负责释放：

```cpp
#include <memory>

struct Node {
    int value;
    Node(int v) : value(v) {}
    ~Node() { std::cout << "Node(" << value << ") destroyed\n"; }
};

void shared_demo() {
    auto p1 = std::make_shared<Node>(1);  // 引用计数 = 1
    {
        auto p2 = p1;                      // 引用计数 = 2
        auto p3 = p1;                      // 引用计数 = 3
    }  // p2, p3 析构——引用计数降回 1
    // p1 还在
}  // p1 析构——引用计数归零 → delete Node
```

- **`make_shared`（C++11）**：一次分配对象 + 控制块——比分开 `new` + 构造 `shared_ptr` 更高效（一次内存分配 vs 两次）
- **`shared_ptr` 的大小**：16 字节（64 位下）——两个指针（对象指针 + 控制块指针）——是裸指针的两倍
- **引用计数怎么存的**：控制块包含强引用计数 + 弱引用计数 + deleter

### §4 `weak_ptr` —— 打破循环引用

`shared_ptr` 的最大陷阱——循环引用导致内存泄漏：

```cpp
struct B;  // 前向声明
struct A { std::shared_ptr<B> b_ptr; ~A() { std::cout << "A destroyed\n"; } };
struct B { std::shared_ptr<A> a_ptr; ~B() { std::cout << "B destroyed\n"; } };

void cycle() {
    auto a = std::make_shared<A>();
    auto b = std::make_shared<B>();
    a->b_ptr = b;   // B 的引用计数 = 2
    b->a_ptr = a;   // A 的引用计数 = 2
    // a, b 离开作用域 → 引用计数都降为 1（互相持有）→ 永远不归零 → 泄漏！
}
```

**`weak_ptr` 解环**：

```cpp
struct A;
struct B { std::weak_ptr<A> a_ptr; };  // 改成 weak_ptr——不增加引用计数
struct A { std::shared_ptr<B> b_ptr; };
// 现在 A 和 B 的引用计数都能正常归零
```

- **`weak_ptr` 的用法**：`auto sp = wp.lock();` ——尝试提升为 `shared_ptr`。如果对象还在→成功；如果已释放→返回空 `shared_ptr`。
- **核心理解**：`weak_ptr` 是"观察者"——它不拥有对象，不增加引用计数，只是"看"对象还在不在。

### §5 自定义 Deleter 与 C 互操作

智能指针和 C 的 `malloc`/`fopen` 互操作：

```cpp
// 用 malloc 分配的内存——delete 不能用，必须用 free
auto p = std::unique_ptr<char, decltype(&free)>(
    (char*)malloc(100), free);

// 文件句柄——析构时 fclose
auto file = std::unique_ptr<FILE, decltype(&fclose)>(
    fopen("data.txt", "r"), fclose);
// file 离开作用域 → fclose(file) 自动调用
```

- **自定义 Deleter 的代价**：`unique_ptr<T, Deleter>` 的大小不再等于 `sizeof(T*)`——Delter 如果是函数指针，多 8 字节。
- **`shared_ptr` 的自定义 Deleter** 不增加 `shared_ptr` 本身的大小——Deleter 存在控制块里。

---

## 3. 编写方针

1. **每节从 C 的痛点出发**——"C 里你要记得 free/fclose，C++ 里你不用记"
2. **§4 是核心**——循环引用 + `weak_ptr` 面试必考，本节配完整的可运行泄漏/修复代码
3. **Stage1 收尾**——本章结尾会有一段"Stage1 回顾"（几段话），把 Ch01-Ch04 串起来
4. 本章不讲的内容：`enable_shared_from_this`（留给 Stage3 继承章节）、`atomic_shared_ptr`（C++20）、多线程下的 `shared_ptr` 安全性（Stage5 并发）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：unique_ptr vs shared_ptr

**面试官**：`unique_ptr` 和 `shared_ptr` 的区别？什么时候用哪个？

**回答**：`unique_ptr` = 独占所有权——不能拷贝只能移动，`sizeof` 等于裸指针（零额外内存）。`shared_ptr` = 共享所有权 + 引用计数——多个指针指向同一对象，最后一个析构时释放。`shared_ptr` 大（16 字节，两个指针）且有引用计数的原子操作开销。何时用——明确只有一个所有者→`unique_ptr`；需要多个指针共享→`shared_ptr`。优先 `unique_ptr`——不说服自己需要共享就不要共享。

**追问（面试官）**：`shared_ptr` 的引用计数是线程安全的吗？对象本身呢？

**追问回答**：引用计数是线程安全的——`shared_ptr` 的控制块里的计数操作是原子的（`atomic_fetch_add`），多线程同时拷贝/析构 `shared_ptr` 不会出现计数错乱。但**对象本身不提供线程安全**——如果多个线程通过 `shared_ptr` 同时读写对象，你仍然需要 mutex 保护。简记：多个线程读同一个 `shared_ptr` → 安全；多个线程写同一个对象 → 需要锁。

### 面试题 2：循环引用与 weak_ptr

**面试官**：`shared_ptr` 的循环引用是怎么导致内存泄漏的？用 `weak_ptr` 怎么解决？

**回答**：两个 `shared_ptr` 互相引用——`A` 持有 `shared_ptr<B>`，`B` 持有 `shared_ptr<A>`。`a` 和 `b` 离开作用域后各自的引用计数降为 1（因为对方还持有自己的 `shared_ptr`）→ 永远不归零→两个对象都无法释放。`weak_ptr` 解环——把其中一方的 `shared_ptr` 换成 `weak_ptr`——`weak_ptr` 不增加引用计数，不阻止释放。

**追问（面试官）**：`weak_ptr` 怎么安全地访问对象？`lock()` 失败了返回什么？

**追问回答**：`auto sp = wp.lock();`——如果对象已释放→返回空 `shared_ptr`；如果对象还在→返回一个有效的 `shared_ptr`（引用计数 +1，保证访问期间不被释放）。`lock()` 返回空 `shared_ptr` 意味着对象已死——不要尝试用过期指针。这是 C++ 里少数几个"线程安全地检查对象生死"的机制。

### 面试题 3：make_shared vs new shared_ptr

**面试官**：`std::make_shared<T>(args)` 和 `std::shared_ptr<T>(new T(args))` 有什么区别？

**回答**：`make_shared` 一次分配（对象 + 控制块在连续内存中）→ 更好缓存局部性 + 少一次 `new`。`new T` + 包装 `shared_ptr` → 两次分配（对象先 `new`，控制块再分配）→ 多一次内存分配 + 潜在的异常安全问题（如果第二步失败，第一步的 `new` 泄漏）。所以：**优先 `make_shared`**。

**追问（面试官）**：那什么情况下不能用 `make_shared`、必须用 `new`？

**追问回答**：(1) 自定义 Deleter——`make_shared` 不能传 Deleter，只能 `shared_ptr<T>(new T, custom_deleter)`；(2) 从 `weak_ptr` 提升——`wp.lock()` 返回的是 `shared_ptr<T>`，不是 `make_shared` 创建的；(3) 大对象 + 长生命周期 `weak_ptr`——`make_shared` 把对象和控制块分在一起，即使对象已释放控制块还在（因为有 `weak_ptr` 指向），导致大对象内存实际上没回收——这时用 `new` 分开分配更好。

---

## 5. 可运行错误实验

### 实验 1：循环引用泄漏

完整代码——A 和 B 互相用 `shared_ptr` 引用——程序退出时 `~A()` 和 `~B()` 从来不打印（泄漏）。然后把 `B::a_ptr` 改成 `weak_ptr<A>`——再运行，两个析构都打印（正确释放）。

### 实验 2：make_shared 一次分配 vs new + shared_ptr 两次分配

```cpp
#include <iostream>
#include <memory>

// 重载 new 来统计分配次数（仅演示——生产代码不要重载全局 new！）
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
    // 方式 1：new + shared_ptr → 两次分配（Foo + 控制块）
    alloc_count = 0;
    std::cout << "=== shared_ptr<T>(new T):\n";
    {
        auto p1 = std::shared_ptr<Foo>(new Foo(42));
        std::cout << "  allocations: " << alloc_count << " (expect 2)\n\n";
    }

    // 方式 2：make_shared → 一次分配（Foo + 控制块连续）
    alloc_count = 0;
    std::cout << "=== make_shared<T>:\n";
    {
        auto p2 = std::make_shared<Foo>(42);
        std::cout << "  allocations: " << alloc_count << " (expect 1)\n\n";
    }

    std::cout << "Done\n";
    return 0;
}
```

**运行效果**：`g++ -std=c++11 alloc_test.cpp && ./a.out`
- `shared_ptr<T>(new T)` → 两次 `new`（Foo 单独分配 + 控制块再分配）
- `make_shared<T>` → 一次 `new`（Foo 和控制块在连续内存中一起分配）

---

## 6. Stage1 收尾回顾（本章末尾 ~30 行）

```
Stage1 四章的递进关系：
Ch01 从 C 到 C++ → 你学会写 C++ 代码（引用/new/重载/auto）
Ch02 类与构造析构 → 你学会管理对象生命周期（构造/析构/深拷贝）
Ch03 移动语义     → 你学会避免不必要的拷贝（移动/RVO/Rule of Five）
Ch04 RAII 与智能指针 → 你学会让编译器替你管理资源（unique_ptr/shared_ptr/weak_ptr）

Stage1 之后，你不再需要手写 delete。
Stage2 预告：模板——让代码也"自动生成"。
```

---

## 7. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | `shared_ptr` 控制块布局（16 字节的两个指针）；引用计数的原子操作（线程安全 vs 对象安全）；`make_shared` 一次分配的缓存局部性优势；自定义 Deleter 对 `unique_ptr` 大小的影响 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（unique_ptr vs shared_ptr / 循环引用+weak_ptr / make_shared vs new），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | 循环引用泄漏的可运行实验；`make_shared` vs `new shared_ptr` 全局 new 计数对比实验；`shared_ptr` 多线程下对象不安全（标注，Stage5 展开） | 8/10 |
| 交叉引用 | §1 从 Ch02 §3 File 类出发（RAII）；§2 从 Ch03 移动语义（`unique_ptr` 不能拷贝只能移动）；Stage1 收尾串联 Ch01-Ch04；Rule of Zero 回扣 Ch03 §5 | 8/10 |
| **总分** | | **33/40** |
