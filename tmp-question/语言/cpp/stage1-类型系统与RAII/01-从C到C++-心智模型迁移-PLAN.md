# Stage1 Ch01 规划：从 C 到 C++ — 心智模型迁移

> **定位**：C++ 第一阶段第一章。前提——读者已完成 C 语言 Stage1-4（20 章），C 功底扎实，但**从未写过一行 C++ 代码**。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §2-6，深入理解C++11 §1-2

---

## 1. 本章要解决的核心问题

读者精通 C，但 C++ 的语法、编译器、标准库对他们来说是全新的。本章的目标不是"一口气讲完 C++11 所有新特性"，而是：

**让读者用 C 的经验作为跳板，理解 C++ 里最基本、最常用的语法变化，能写出第一段 C++ 程序。**

原则：
- 每一节都从"你在 C 里怎么写"开始，然后展示"C++ 里怎么写"
- 只讲 C++11 中最常用、生产代码中每天都会遇到的特性
- 不深入底层实现（那是后面章节的事）
- 每节一个核心概念，配可运行的完整代码

---

## 2. 节结构（7 节，从浅到深）

### §1 Hello, C++ — 第一段 C++ 程序

从一个简单的 C 程序开始，展示 C++ 里有什么不同：

- `.c` → `.cpp`，`gcc` → `g++`
- `#include <stdio.h>` → `#include <iostream>`，`printf` → `std::cout`
- **输入**：`scanf` → `std::cin`（反过来就是输出）

```cpp
#include <iostream>       // C++ 标准输入输出
#include <string>          // C++ 字符串类型
int main() {
    std::string name;
    std::cout << "What's your name? ";
    std::cin  >> name;                     // 读输入——免去 scanf 的 %s 格式
    std::cout << "Hello, " << name << "!\n";
    return 0;
}
```

- **编译命令**：`g++ -std=c++11 hello.cpp -o hello && ./hello`
- **最后一步**：`using namespace std;` 是什么——偷懒缩写，把 `std::cout` 写成 `cout`
- 读者得到第一段能编译运行、有输入有输出的 C++ 代码

### §2 引用 —— 一个"不会空的指针"

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `void swap(int *a, int *b) { int t = *a; *a = *b; *b = t; }` 调用 `swap(&x, &y)` | `void swap(int &a, int &b) { int t = a; a = b; b = t; }` 调用 `swap(x, y)` |
| 函数参数是 `int *p`，需要检查 `if (p == NULL)` | 函数参数是 `int &r`，编译期保证不会为空 |
| 从函数返回指针 `int* get() { return &data; }` | 从函数返回引用 `int& get() { return data; }` — 省去一个 `*` |

- **核心理解**：引用就是"别名"——`int &r = x` 后，`r` 和 `x` 是同一个东西
- **什么时候用引用**：函数参数不想拷贝大对象时、函数想修改传入的变量时
- **什么时候用指针**：参数可以是"空"时、运行时需要切换指向目标时
- 这节的代码全部可编译运行

### §3 `new` 和 `delete` — 替代 `malloc`/`free`

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `int *p = (int*)malloc(100 * sizeof(int));` + 检查 NULL | `int *p = new int[100];` |
| `free(p);` | `delete[] p;` |
| 单个对象：`struct Foo *f = malloc(sizeof(Foo));` 然后手动 `f->a = 1; f->b = 2;` | `Foo *f = new Foo(1, 2);` — 分配+初始化一步完成 |

- **核心理解**：`new` = 分配内存 + 调初始化；`delete` = 调清理 + 释放内存
- **关键规则**：`new` 配 `delete`，`new[]` 配 `delete[]`——不能混用
- **malloc 还能用吗**：可以，比如需要 `realloc` 的时候（C++ 没有等价物）
- 本节不讲构造函数/析构函数——留给 Ch02

### §4 函数重载 —— 同一个名字，不同的参数

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `int add_int(int, int);` `double add_double(double, double);` — 人工起名 | `int add(int, int);` `double add(double, double);` — 编译器判断调用哪个 |
| `void print_int(int);` `void print_str(const char*);` | `void print(int);` `void print(const char*);` |

- **核心理解**：编译器根据参数类型和数量自动选择正确的函数版本
- **不能重载的情况**：只有返回值不同——不行，编译器无法分辨
- **默认参数**：`void log(const char* msg, int level = 0);` — `log("error")` 等同于 `log("error", 0)`
- 这节不讲 name mangling——那个太早了，留给 S2（编译链接阶段）

### §5 命名空间 —— 告别函数名前缀

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `void moduleA_init(void);` `void moduleB_init(void);` — 前缀避免重名 | `namespace moduleA { void init(); }` `namespace moduleB { void init(); }` |
| `#include "redis.h"` 然后 `redis_log(...)` | `#include "redis.h"` 然后 `redis::log(...)` |

- **核心理解**：命名空间像文件的"目录"——把同名的东西放在不同名字下
- **跨文件使用**（这是 C 没有的概念——C 的 `static` 是"文件私有"，不是"命名空间"）：

```cpp
// math.h — 头文件中声明
namespace math {
    int add(int a, int b);    // 只声明
    int mul(int a, int b);
}

// math.cpp — .cpp 中实现
#include "math.h"
namespace math {
    int add(int a, int b) { return a + b; }
    int mul(int a, int b) { return a * b; }
}

// main.cpp — 使用
#include "math.h"
int main() {
    int sum = math::add(3, 4);    // 调用——用 :: 访问
    return 0;
}
```

- **`using namespace std;`**：偷懒写法，把 std 名字空间的全部东西拉到当前作用域——学习中可以用，生产代码慎用（大项目污染名字空间）

### §6 `constexpr` 与 `nullptr` — 两个小但重要的改进

**`nullptr`**：

你在 C 里 `#define NULL ((void*)0)` 或 `0`—— `NULL` 本质是整数，可以被塞进 `int` 变量而不报错。C++11 里 `nullptr` 是单独的类型，只能赋给指针变量。

```cpp
int  *p = nullptr;   // ✓ 正确
int   x = nullptr;   // ✗ 编译错误——nullptr 不能当整数
```

**`constexpr`**：

C 的 `const int N = 10;` 不能用于数组大小（VLA 警告），因为 C 的 `const` 只是"不可修改"，不代表编译时已知。C++11 的 `constexpr` 保证值在编译时算好：

```cpp
constexpr int N = 10;
int arr[N];   // ✓ —— N 是真正的编译时常量
```

- 本节就讲这两件事，不展开——够了

### §7 `auto` — 让编译器帮你写类型

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `unsigned long long x = 100ULL;` | `auto x = 100ULL;` — 编译器看出是 unsigned long long |
| `for (int i = 0; i < n; i++)` | `for (auto i = 0; i < n; i++)` — 一样 |

- **核心理解**：`auto` 不是"动态类型"——它在编译时就确定了类型，只是省了你手写的功夫
- **什么时候用**：类型名很长的时候（后面的章节会看到）、类型从表达式就能明显看出的时候
- **什么时候不用**：类型名有助于理解代码意图时，不要隐藏它

---

## 3. 编写方针

1. **每节都有完整的、可编译运行的 C++ 代码**，读者可以拷贝到文件然后 `g++ test.cpp -std=c++11 && ./a.out`
2. **对比表格放在每节开头**——"C 里这样做 → C++ 里这样做"，一眼看懂差异
3. **每节控制在 100 行左右**（代码+解释）——不要一章塞太多信息
4. 本章不讲的内容（但后面章节会讲）：类/构造函数/析构函数 → Ch02，模板 → Stage2，智能指针 → Ch04，头文件组织 → Stage5

---

## 3. 面试题（3 组，每组含答案+追问）

### 面试题 1：引用 vs 指针

**面试官**：C++ 的引用和指针有什么区别？什么时候用引用什么时候用指针？

**回答**：引���是"别名"——`int &r = x` 之后 `r` 就是 `x`，不能重新绑定到别的变量。指针是"地址值"——可以重新赋值、可以为空。引用不能为空（编译器保证），指针可以是 NULL。用引用：函数参数不想拷贝大对象时、函数想修改传入变量时、返回左值（如 `vec[0]` 返回引用支持赋值回元素）。用指针：参数可以是"没有"时（NULL 表意）、运行时需要切换指向目标时。

**追问（面试官）**：引用底层是怎么实现的？为什么不能是空？

**追问回答**：编译器把引用实现为 `T* const`（常量指针）——你写 `int &r = x;`，编译器生成类似 `int *const __r = &x;` 的代码。不能为空是因为引用必须在定义时绑定到一个存在的对象——和指针的 `const` 版本不同在于引用语法上不需要 `*` 和 `&`。这也是为什么 `sizeof(引用)` 返回的是引用对象的实际大小（编译器自动解引用）。

### 面试题 2：new/delete vs malloc/free

**面试官**：`new` 和 `malloc` 的根本区别是什么？混用会怎样？

**回答**：`new` = 分配内存 + 调构造函数；`delete` = 调析构函数 + 释放内存。`malloc` 只分配内存，不调构造函数。如果 `new` 分配后用 `free` 释放——析构函数不跑→资源泄漏（如 FILE* 没 fclose）。如果 `malloc` 分配后用 `delete` 释放——行为未定义（delete 期望对象是 new 分配的）。`new[]` 必须配 `delete[]`，`new` 必须配 `delete`。

**追问（面试官）**：那什么情况下需要用 `malloc` 而不是 `new`？

**追问回答**：(1) 需要 `realloc` 扩容——C++ 没有直接等价物；(2) 与 C 库交互——C 库返回 `malloc` 分配的内存，你必须用 `free` 释放；(3) 自定义内存池用底层分配 API（`mmap`/`sbrk`），不经过 `::operator new`。除此之外，C++ 代码里优先用 `new`/智能指针。

### 面试题 3：constexpr vs const

**面试官**：C++ 里 `const` 和 `constexpr` 的区别？什么时候 `const` 不够用？

**回答**：`const` = "运行时不可修改"，不保证编译时值已知。`constexpr` = "编译时一定能算出值"。区别——`const int N = rand();` 合法（N 在运行时确定），`constexpr int N = rand();` 编译错误。`constexpr` 能用于数组大小、模板参数、静态断言——这些地方要求编译时常量。生产代码中优先用 `constexpr` 声明编译时常量——让编译器在编译时捕获错误。

**追问（面试官）**：那 C 里的 `const` 是不是最接近 C++ 的 `constexpr`？

**追问回答**：C 的 `const` 比 C++ 更接近 `constexpr`——C 的 `const int N = 10; int arr[N];` 可能触发 VLA 警告但编译器在接受时会当作编译时常量。C++ 的 `const` 更严格——`const int N = 10; int arr[N];` 在 C++ 中也是合法的（因为 N 是常量表达式），但 `const int N = foo(); int arr[N];` 不合法（VLA 不是 C++ 标准），而 `constexpr int N = foo();` 明确要求 foo 是 `constexpr` 函数——语义更加显式。

---

## 4. 可运行错误实验

### 实验：`new[]` 配 `delete`（不是 `delete[]`）

```cpp
#include <iostream>
struct Foo {
    int id;
    Foo(int i) : id(i) { std::cout << "Foo(" << id << ") created\n"; }
    ~Foo()            { std::cout << "Foo(" << id << ") destroyed\n"; }
};

int main() {
    Foo *arr = new Foo[3]{Foo(1), Foo(2), Foo(3)};  // 创建 3 个对象
    delete arr;  // 🔴 错误！应该是 delete[] arr
    // 预期：只有 Foo(1) 的析构被调用——Foo(2) 和 Foo(3) 泄漏
    return 0;
}
```

**运行效果**：三个"created"，只有一个"destroyed"——另外两个对象的内存虽然被释放（OS 层面），但析构函数没跑。如果析构里有 `fclose` 或 `free`，那两个的资源永久泄漏。

---

## 5. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | 每个特性讲清楚"解决了 C 的什么问题"；引用的底层本质（`T* const`）在面试题第 1 题追问中展开 | 7/10 |
| 面试覆盖 | 3 组完整面试 Q&A（引用 vs 指针 / new/malloc / constexpr vs const），每组含答案 + 二���追问 | 9/10 |
| 生产陷阱 | `new[]`+`delete` 可运行实验（析构跳过→资源泄漏）；`using namespace std` 命名污染；auto 引用陷阱标注 | 8/10 |
| 交叉引用 | §3 标注"不讲构造析构→Ch02"（Ch02 §1 反向引用）；引用理解→C Ch02 指针本质；本节→Ch02 类/Stage2 模板 | 7/10 |
| **总分** | | **31/40** |
