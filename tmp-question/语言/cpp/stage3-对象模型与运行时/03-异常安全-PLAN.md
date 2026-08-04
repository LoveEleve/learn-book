# Stage3 Ch03 规划：异常安全

> **定位**：Stage3 第三章。前提——读者已完成 Ch01（继承/虚函数）和 Ch02（多重继承/虚继承），理解 C++ 运行时多态的完整机制。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §13，精通现代C++ §9

---

## 1. 本章要解决的核心问题

C 里的错误处理是返回码——`if (ret < 0) { perror("..."); return -1; }`。这有两个问题：(1) 调用方可能忘记检查返回码；(2) 错误路径充斥着 `goto cleanup`/`if (ret) free(buf)`——和正常逻辑混在一起。

C++ 的异常给了另一种思路：**正常逻辑和错误处理分离**——正常代码走主路径，错误自动"抛"到专门的 `catch` 块。但异常引入了新问题——**异常安全**：当异常穿过你的代码时，你的资源（内存/文件/锁）是否被正确释放？

本章教三件事：怎么抛和抓异常、怎么让代码在异常时也不泄漏资源、以及 `noexcept` 何时带来性能收益。

---

## 2. 节结构（5 节）

### §1 `try`/`catch`/`throw`——异常的基本语法

```cpp
#include <stdexcept>

int divide(int a, int b) {
    if (b == 0) throw std::runtime_error("division by zero");
    return a / b;
}

int main() {
    try {
        int result = divide(10, 0);
        std::cout << result;  // 不会执行——异常跳过了
    } catch (const std::runtime_error& e) {
        std::cout << "Error: " << e.what() << "\n";
    } catch (...) {
        std::cout << "Unknown error\n";  // 兜底——捕获任何东西
    }
}
```

- **`throw`**：抛出异常——可以是任何类型（int/string/自定义类）。生产代码抛标准异常子类（`std::runtime_error`/`std::logic_error`）或自定义的异常类。
- **`catch`**：按类型匹配——`catch (const std::runtime_error& e)` 只捕获这一种。`catch (...)` 兜底捕获所有。
- **异常安全的核心问题**：`throw` 之后，`throw` 和 `catch` 之间的所有栈帧被"展开"——局部对象被析构，但**裸指针**不会被自动释放。

### §2 栈展开——异常穿过你的代码时发生了什么

```cpp
void f() {
    int* p = new int(42);    // 🔴 裸指针——异常后不会自动释放！
    g();                     // g 可能抛异常
    delete p;                // 如果 g 抛异常——这行不会执行——泄漏！
}
```

- **栈展开**：异常从 `throw` 点向上冒泡——每经过一个函数帧，该帧的局部对象（`string`/`vector`/`unique_ptr`）被析构。但裸指针、裸文件句柄（没有 RAII 包装的）**不会被自动释放**。
- **正确姿势**——RAII 包装：

```cpp
void f() {
    auto p = std::unique_ptr<int>(new int(42));  // ✓ RAII 管理
    g();                                          // 即使 g 抛异常——p 的析构函数释放 int
}
```

- **核心规则**：**异常安全的代码 = 所有资源都用 RAII 对象管理**。这呼应了 Stage1 Ch04 的 RAII——异常安全是 RAII 最重要的使用场景。

### §3 异常安全的三个保证级别

每个函数在异常发生时提供的保证分为三级：

| 级别 | 承诺 | 例子 |
|---|---|---|
| **基本保证（basic）** | 异常抛出后对象仍然有效——不泄漏资源、数据一致（但值可能变了） | `vector::push_back` 抛异常后，vector 仍是有效状态（size 未变） |
| **强保证（strong）** | 异常抛出后对象恢复到操作前的状态——"要么全做，要么全不做" | `vector::insert` 抛异常后，vector 内容和操作前一样 |
| **不抛保证（nothrow）** | 承诺不抛异常 | `swap`/移动构造/析构函数 |

```cpp
class Widget {
    std::string name;
    int value;
public:
    // 强保证——如果 setName 抛异常，name 和 value 都不变
    void update(const std::string& new_name, int new_value) {
        std::string old_name = name;  // 备份
        int old_value = value;
        name = new_name;              // 可能抛异常（string 的内存分配）
        value = new_value;            // 不抛异常——到这里成功了
        // 不需要 old_name 和 old_value——如果 name = new_name 抛异常，
        // 栈展开析构 these，name 没被修改（old_name 是局部副本）
    }
};
```

- **强保证的实现模式**："先做可能失败的事、再做不失败的事"。如果可能失败的事做了，用 swap 替换旧状态。
- **`std::swap` 的不抛保证**：C++11 的 `std::swap` 用移动语义实现——只交换指针不分配内存→不抛异常。Ch03 讲的移动语义在这里兑现了异常安全收益。

### §4 `noexcept`——承诺不抛异常

C++11 引入 `noexcept` 替代 C++98 的 `throw()`（动态异常规范，已废弃）：

```cpp
// 承诺不抛异常——编译器可能做优化
void swap(Widget& a, Widget& b) noexcept {
    // 交换——仅指针交换，不分配内存
}

// 条件 noexcept——仅当 T 的移动构造不抛时，这个函数也不抛
template <typename T>
void my_swap(T& a, T& b) noexcept(noexcept(T(std::move(a)))) {
    T tmp = std::move(a); a = std::move(b); b = std::move(tmp);
}
```

- **`noexcept` 的性能收益**：`vector` 扩容时检查移动构造是不是 `noexcept`——是就用移动（快），不是就用拷贝（安全但慢）。这就是 Ch03 §3 讲的"移动构造必须 noexcept"的终极原因。
- **`noexcept` 的运行时检查**：`noexcept(foo())` 编译时返回 true/false——告诉调用方"这个表达式会不会抛异常"。
- **析构函数默认是 `noexcept`**——C++11 起，析构函数隐式标记为 `noexcept(true)`。如果析构函数抛异常且栈已经在展开中→`std::terminate()`——程序直接终止。

### §5 析构函数不应抛异常 + 构造函数中的异常

**析构函数**：

```cpp
class File {
    FILE* f;
public:
    ~File() {
        if (fclose(f) != 0) {
            // 🔴 析构函数里不能抛异常！
            // 如果当前栈已经在展开中（因为另一个异常）→ std::terminate 直接杀进程
        }
    }
};
```

- **为什么**：如果析构函数在栈展开期间抛异常→两个异常同时在传播→C++ 标准说"这时调用 `std::terminate`"→程序直接死。
- **怎么做**：析构函数里捕获所有异常——`try { ... } catch(...) { /* 吞掉 */ }`——不要让它传播到析构函数外面。

**构造函数**：

```cpp
class Widget {
    int* p1;
    int* p2;
public:
    Widget() : p1(new int(1)), p2(new int(2)) {
        if (some_condition) throw std::runtime_error("bad");
    }
    ~Widget() { delete p1; delete p2; }
};
// 🔴 如果构造函数抛异常——p1 分配了但 Widget 对象没"活过"——析构函数不跑——p1 泄漏！
```

- **解决方案**：用 `unique_ptr` 替代裸指针——即使构造函数抛异常，已构造的成员（`unique_ptr<int> p1`）的析构函数会跑。

---

## 3. 编写方针

1. **从 C 的返回码痛点出发**——展示忘记检查 `if (ret < 0)` 的后果，引出异常让错误处理"强制化"
2. **§2 栈展开必须配图**——一张"异常从 throw 点到 catch 点经过的栈帧，RAII 对象被析构、裸指针被跳过"的图
3. **§3 三级保证是面试重点**——每个级别配一个可运行代码示例
4. 本章不讲的内容：`std::exception_ptr`/`std::nested_exception`（C++11 进阶）、异常的性能开销数据（太平台特定）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：异常安全的三个级别

**面试官**：C++ 的异常安全有哪三个级别？各给一个 STL 例子。

**回答**：(1) 基本保证——对象状态有效但不保证值不变。`vector::push_back` 扩容失败→vector 仍是有效状态（旧内存未被释放），但 size/capacity 未改变。(2) 强保证——要么全做要么全不做。`vector::insert` 抛异常→vector 回到插入前的状态。(3) 不抛保证——承诺不抛异常。`swap`/移动构造/析构函数。三个级别递增——基本≤强≤不抛。

**追问（面试官）**：怎么把基本保证升级为强保证？给一个实现模式。

**追问回答**："copy-and-swap"模式——(1) 在临时副本上做所有可能失败的操作；(2) 如果全部成功→用不抛的 `swap` 把临时副本换进来；(3) 如果任何一步失败→临时副本被析构，原对象未受任何影响。关键——`swap` 必须不抛（`noexcept`），否则第二步抛异常会破坏强保证。

### 面试题 2：noexcept 的用途

**面试官**：`noexcept` 解决什么问题？不加 `noexcept` 有什么后果？

**回答**：(1) 文档——告诉调用方"这个函数不抛异常"；(2) 编译器优化——`noexcept` 函数调用处编译器可以省略栈展开的代码，生成更小的二进制；(3) `vector` 扩容时用 `noexcept` 判断移动构造是否安全——Ch03 的移动语义说的就是这里。不加 `noexcept` →移动构造存在但 `vector` 不敢用→扩容用拷贝→性能退化。

**追问（面试官）**：析构函数默认是 `noexcept` 吗？如果在析构函数里强制抛异常会发生什么？

**追问回答**：C++11 起析构函数默认 `noexcept(true)`。如果析构函数抛异常——正常场景（没有其他异常在传播）→异常照常传播；但如果栈已经在展开中（另一个异常正在传播）→两个异常同时存在→`std::terminate()`→程序直接终止。所以析构函数里绝对不要抛异常——吞掉或记录日志然后返回。

### 面试题 3：构造函数中的异常安全

**面试官**：构造函数中抛异常后，已构造的成员会被析构吗？已 `new` 的裸指针呢？

**回答**：已构造的成员（如 `string name`）会被析构——因为它们是完整对象。已 `new` 的裸指针**不会**被释放——因为裸指针的析构函数只释放指针本身（8 字节），不释放指向的内存。这就是为什么构造函数里要用 `unique_ptr` 替代裸指针——即使构造函数抛异常，已构造的 `unique_ptr` 成员会析构并释放内存。

**追问（面试官）**：构造函数里抛了异常——析构函数会跑吗？

**追问回答**：不会——对象从来没"活过"（构造未完成），编译器不调析构函数。所以构造函数必须自己处理已分配资源的清理——或者用 RAII 成员（`unique_ptr`/`string`/`vector`）让编译器替你做清理。

---

## 5. 可运行错误实验

### 实验 1：裸指针在异常中泄漏

```cpp
#include <iostream>
#include <stdexcept>

void leaky() {
    int* p = new int(42);       // 🔴 裸指针
    std::cout << "allocated: " << *p << "\n";
    throw std::runtime_error("oops!");  // 异常！delete p 不执行！
    delete p;                    // 永远不到这里
}

int main() {
    try { leaky(); }
    catch (...) { std::cout << "caught, but p leaked!\n"; }
    return 0;
}
```

**修复**：`auto p = std::unique_ptr<int>(new int(42));`——异常后 `unique_ptr` 的析构函数释放 `int`。

### 实验 2：析构函数抛异常→std::terminate

```cpp
#include <iostream>
#include <stdexcept>

struct BadDtor {
    ~BadDtor() noexcept(false) {  // 显式允许析构抛异常（极少见）
        throw std::runtime_error("dtor threw!");
    }
};

struct Trigger {
    ~Trigger() {
        BadDtor b;  // 🔴 析构 b 时抛异常——栈正在展开中（Trigger 本身被异常销毁）
    }
};

int main() {
    try {
        Trigger t;
        throw std::runtime_error("original exception");  // 第一个异常
        // t 析构→b 析构→throw→第二个异常→std::terminate()
    } catch (...) {
        std::cout << "never reached\n";  // 程序已终止
    }
    return 0;
}
```

**运行**：`g++ -std=c++11 terminate.cpp && ./a.out`——`terminate called after throwing an instance of 'std::runtime_error'`，程序以 `SIGABRT` 结束。

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | 栈展开时 RAII 对象析构 vs 裸指针泄漏的机制对比；`noexcept` 影响 `vector` 扩容策略的编译器实现原理；析构函数默认 noexcept 的标准规定；copy-and-swap 实现强保证的 swap 为什么必须不抛 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（安全三级+copy&swap / noexcept 优化+析构默认 / 构造异常+成员析构），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | 裸指针异常泄漏可运行实验；析构抛异常→std::terminate 实验；构造函数中裸指针泄漏演示 | 8/10 |
| 交叉引用 | §2 栈展开→Stage1 Ch04 RAII（异常是 RAII 最重要的使用场景）；§4 noexcept→Stage1 Ch03 移动语义（`vector` 扩容用 noexcept 判断）；§5 构造函数异常→Stage1 Ch04 `unique_ptr` 必要性 | 8/10 |
| **总分** | | **33/40** |
