# Lambda 与函数对象

> **前置依赖**：读者在 Stage2 Ch03 见过 Lambda 基本语法（`[](int x){ return x>100; }`、`[=]`/`[&]`、`std::function`），理解 STL 算法和函数对象的配合。
> **主线版本**：C++11
> **参考书**：深入理解C++11 §6，C++程序设计语言 §11

---

## 1. 引言：从"用 Lambda"到"理解 Lambda"

Stage2 Ch03 教你用 Lambda 配合 `find_if`/`sort`/`for_each` 写简洁的算法代码。那时 Lambda 是"让 STL 好用"的工具。

但 Lambda 不是"匿名函数的语法糖"——你写的每一个 `[](int x) { return x > 100; }`，编译器生成的是**一个有名字的类、一个构造函数、一个 `operator()` 成员函数**。Lambda 是对象——不是函数。

```cpp
auto add = [](int a, int b) { return a + b; };

// 这行代码背后——编译器生成了等价于下面的东西：
struct __anonymous_lambda {
    int operator()(int a, int b) const { return a + b; }
};
__anonymous_lambda add;  // add 不是函数——是匿名类的实例！
```

本章从"你会用"升级到"你理解每一行背后的内存和编译模型"——闭包对象有多大？捕获的变量存储在哪儿？什么时候用 `auto` 存 Lambda、什么时候用 `std::function`？读完本章，你不会再"跟着感觉写 Lambda"。

---

## 2. Lambda 不是函数——是对象

### 2.1 编译后的等价代码

每个 Lambda 表达式编译后是一个**匿名类的实例**——每个 Lambda 有**唯一的类型**：

```cpp
auto f1 = [](int x) { return x * 2; };
auto f2 = [](int x) { return x * 2; };

// f1 和 f2 的类型不同——即使签名完全一样！
// f1 = f2;  // ✗ 编译错误——类型不同
```

**两个 Lambda 签名一样但类型不同**——这很重要。每个 Lambda 是独立匿名类，不共享类型。`auto` 是唯一能存 Lambda 的方式——你不能（也不应该）手写编译器生成的匿名类名。

### 2.2 `operator()` 默认是 `const`

闭包类的 `operator()` 默认带 `const`——这意味着 Lambda 内部不能修改闭包对象的成员：

```cpp
int counter = 0;
auto bad = [counter]() { return ++counter; };  // ✗ 编译错误——operator() 是 const
auto good = [counter]() mutable { return ++counter; };  // ✓ mutable 去掉 const
```

`mutable` 去掉 `operator()` 的 `const` 修饰——允许修改闭包对象的成员变量（不影响外部变量，因为是按值捕获的拷贝）。

---

## 3. 捕获——把外部变量"搬进"闭包对象

### 3.1 捕获变量 = 闭包对象的成员变量

捕获列表不是魔法——每个被捕获的变量变成闭包类的**成员变量**，通过**构造函数**初始化：

```cpp
int x = 10;
double y = 3.14;

auto f = [x, &y](int z) { return x + y + z; };

// 编译器等价生成：
struct __lambda_456 {
    int  x;       // = 10——按值捕获：调用拷贝构造函数
    double& y;    // 引用——按引用捕获
    __lambda_456(int x_arg, double& y_ref) : x(x_arg), y(y_ref) {}
    int operator()(int z) const { return x + y + z; }
};
__lambda_456 f{x, y};  // 构造函数初始化闭包成员
```

**两种捕获的本质区别**：

| 捕获方式 | 闭包成员类型 | 初始化 | 修改影响 |
|---|---|---|---|
| `[x]` 按值 | `int x` | 调用 `int` 的拷贝构造 | 不影响外部 |
| `[&y]` 按引用 | `double& y` | 引用绑定到外部 `y` | 修改就是在修改外部 |

按值捕获**调用被捕获对象的拷贝构造函数**——这就是为什么不能用 `[p]` 捕获 `unique_ptr`（没有拷贝构造）。C++14 的 `[p = std::move(p)]`（init capture）才能转移所有权。

### 3.2 `[=]` 对 `this` 的陷阱

`[=]` 按值捕获所有局部变量——但 `this` 是**隐式按引用捕获**。这是 C++11 最常见的 Lambda 陷阱：

```cpp
class Widget {
    int value = 42;
public:
    std::function<int()> get_fn() {
        return [=] { return value; };  // 🔴 [=] 没拷贝 value！捕获的是 this！
    }
};

Widget* w = new Widget;
auto fn = w->get_fn();
delete w;                        // w 已销毁
int v = fn();                    // 🔴 UB——this 悬垂，value 是 this->value！
```

**为什么 `[=]` 不拷贝 `this`**：C++11 这样设计有两个原因——(1) 如果拷贝 `*this`，闭包必须知道完整的类定义（声明可能还没完）；(2) 不经意间拷贝大对象有性能隐患。C++20 废弃了 `[=]` 的隐式 `this` 捕获，C++14 的 `[*this]`（init capture）可以显式拷贝。

**C++11 正确做法**：

```cpp
auto get_fn_safe() {
    int copy = value;               // 手动拷贝到局部变量
    return [copy] { return copy; }; // ✓ 按值捕获局部变量
}
```

### 3.3 闭包对象的 `sizeof`

闭包对象的大小 = 按值捕获的成员变量之和（加上对齐）：

```cpp
#include <iostream>
#include <vector>

int main() {
    int a = 1, b = 2;
    std::vector<int> v = {1, 2, 3, 4, 5};

    auto f1 = []{};                      // 不捕获
    auto f2 = [a]{};                     // 捕获一个 int
    auto f3 = [&a]{};                    // 引用捕获 int——存的是指针（8 字节）
    auto f4 = [v]{};                     // 捕获 vector——拷贝整个 vector 对象

    std::cout << "no capture:   " << sizeof(f1) << " (expect 1)\n";
    std::cout << "capture int:  " << sizeof(f2) << " (expect 4)\n";
    std::cout << "capture int&: " << sizeof(f3) << " (expect 8, a pointer)\n";
    std::cout << "capture vec:  " << sizeof(f4) << " (expect " << sizeof(v) << ")\n";
    return 0;
}
```

**预期输出**：
- 不捕获 = 1 字节（C++ 要求空对象至少 1 字节）
- 按值捕获 int = 4 字节（int 成员）
- 引用捕获 int = 8 字节（64 位指针）
- 按值捕获 vector = 24 字节（拷贝整个 `vector` 对象本身，不包括堆上的元素数据）

---

## 4. `mutable`——让 Lambda 持有内部状态

### 4.1 语法和原理

```cpp
int counter = 0;
auto gen = [counter]() mutable { return ++counter; };

std::cout << gen() << '\n';  // 1
std::cout << gen() << '\n';  // 2
std::cout << gen() << '\n';  // 3
std::cout << counter << '\n'; // 0——外部的 counter 没变
```

`mutable` 去掉 `operator()` 的 `const`——Lambda 内部可以修改闭包对象的成员变量。每次调用 `gen()` 递增闭包内的 `counter`——不影响外部的 `counter`。

**闭包对象的状态是每个对象独立的**：

```cpp
auto g1 = gen;  // 拷贝闭包对象——g1 有自己的 counter（和 gen 相同）
auto g2 = gen;  // 又一个拷贝——g2 也有自己的 counter

g1();  // g1.counter = 1
g2();  // g2.counter = 1——独立！
gen(); // gen.counter = 1——也是独立的！
g1();  // g1.counter = 2——继续递增自己的
```

这和函数里的 `static` 局部变量完全不同——`static` 变量是所有调用共享的。闭包对象的成员变量是**每个对象私有**的——拷贝闭包对象就拷贝状态。

### 4.2 生产场景——`std::generate` 生成序列号

```cpp
#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::vector<int> ids(10);
    int next_id = 100;

    // Lambda 持有 next_id 的拷贝——每次调用递增
    std::generate(ids.begin(), ids.end(),
        [next_id]() mutable { return next_id++; });
    // ids = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109}

    for (int id : ids) std::cout << id << ' ';
    return 0;
}
```

---

## 5. Lambda vs `std::function` vs 函数对象——性能与选择

### 5.1 四种可调用方式

Lambda 不是唯一的"可调用东西"。C++ 有四种方式——每种有不同的内存和性能特性：

```cpp
// 1. Lambda（用 auto 存）——零开销，类型匿名
auto f1 = [](int x) { return x * 2; };

// 2. 函数指针——零开销但不能存有捕获的 Lambda
int (*f2)(int) = nullptr;  // 只能指向函数或无捕获 Lambda
f2 = [](int x) { return x * 2; };  // ✓ [] 可以转函数指针

// 3. 函数对象（struct + operator()）——零开销，有名字
struct Doubler {
    int operator()(int x) const { return x * 2; }
};
Doubler f3;

// 4. std::function——有开销但类型统一
std::function<int(int)> f4 = [](int x) { return x * 2; };
```

### 5.2 选择矩阵

| | `auto` Lambda | `std::function` | 函数指针 | 函数对象 |
|---|---|---|---|---|
| 调用开销 | 零（直接调，可 inline） | 虚表查找 + 可能堆分配 | 间接跳转（通常不可 inline） | 零（直接调，可 inline） |
| 类型 | 匿名——`auto` 推导 | 统一——`function<Sig>` | 统一——`Ret(*)(Args)` | 具名——你的类型 |
| 可存容器 | ✗ 类型不同不能放同一个 vector | ✓ 任何 `function<void()>` 都能存 | ✓ | ✓（同类型） |
| 有捕获 Lambda | ✓ | ✓ | ✗——有捕获的不能转函数指针 | N/A |
| 内部状态 | `mutable` Lambda | 不支持 | ✗ | ✓ 成员变量 |
| 典型场景 | 传给 STL 算法 | 回调注册表、类成员 | C API 互操作 | 需要复用+命名的逻辑 |

**核心规则**：能用 `auto` Lambda 就用 `auto`（零开销）。需要"运行时存储不同类型可调用对象"时用 `std::function`——回调注册表（`map<string, function<void(int)>>`）、类成员变量（`auto` 不能作为成员）、虚接口参数。

### 5.3 `std::function` 的 SBO——类似 SSO 的小缓冲区优化

`std::function` 内部有一个小缓冲区（通常 16-32 字节）：

```cpp
std::function<int(int)> f;

// SBO 命中的场景——不触发堆分配
f = [](int x) { return x; };                    // 不捕获——装得进 SBO
f = [a, b, c](int x) { return a+b+c+x; };      // 三个 int——可能装得进 SBO

// SBO 不命中的场景——触发堆分配
std::vector<int> big(1000);
f = [big](int x) { return big[x]; };            // 按值捕获大对象→超出 SBO→堆分配
```

这和 `std::string` 的 SSO（Ch02 §3.2）相同的优化思路——小对象直接存内部，大对象才走堆。

---

## 6. `std::bind`——被 Lambda 取代的历史

`std::bind` 是 C++98→C++11 过渡时期的产物——在 Lambda 出现之前用来实现"部分应用"：

```cpp
#include <iostream>
#include <functional>

int add(int a, int b) { return a + b; }

// bind 方式——绑定第二个参数为 10
auto add10_bind = std::bind(add, std::placeholders::_1, 10);
std::cout << add10_bind(5) << '\n';  // 15

// Lambda 方式——更清晰
auto add10_lambda = [](int x) { return add(x, 10); };
std::cout << add10_lambda(5) << '\n';  // 15
```

Lambda 比 `bind` 更清晰——参数流向一目了然。C++ 社区的共识：**能用 Lambda 就用 Lambda**。本书后续不再用 `bind`——你知道它存在，看到老代码能读懂就够了。

---

## 7. 运算符重载——从 `operator()` 说起

### 7.1 函数对象的本质就是 `operator()`

Lambda 的 `operator()` 让闭包对象可以"像函数一样被调用"。C++ 支持重载 40+ 种运算符——`operator()` 只是其中一个。理解它是一种能力后，你可以用同样的思路重载其他运算符：

```cpp
class Vec2 {
    double x, y;
public:
    Vec2(double x_, double y_) : x(x_), y(y_) {}

    // 下标——vec[0] 等价于 vec.x
    double& operator[](int i) { return i == 0 ? x : y; }

    // 算术——vec1 + vec2
    Vec2 operator+(const Vec2& rhs) const { return {x + rhs.x, y + rhs.y}; }

    // 比较——vec1 == vec2
    bool operator==(const Vec2& rhs) const { return x == rhs.x && y == rhs.y; }

    // 输出——std::cout << vec
    friend std::ostream& operator<<(std::ostream& os, const Vec2& v) {
        return os << '(' << v.x << ", " << v.y << ')';
    }
};

Vec2 a{1, 2}, b{3, 4};
std::cout << (a + b) << '\n';  // (4, 6)
std::cout << (a == b) << '\n'; // 0 (false)
```

### 7.2 可重载 vs 不可重载

| 类别 | 可重载 | 不可重载 |
|---|---|---|
| 算术 | `+` `-` `*` `/` `%` | — |
| 比较 | `==` `!=` `<` `>` `<=` `>=` | — |
| 赋值 | `=` `+=` `-=` `*=` | — |
| 下标 | `[]` | — |
| 调用 | `()` | — |
| 解引用 | `*` `->` | `.`（成员访问） |
| 类型转换 | `operator int()` | — |
| 内存 | `new` `delete` | `::` `.*` `?:` |

### 7.3 `friend`——当运算符不能写成成员函数时

`operator<<` 的第一个参数是 `std::ostream`——不能是 `Vec2` 的成员函数（因为 `this` 必须是 `Vec2`）。用 `friend` 声明为非成员函数，同时允许访问 `Vec2` 的 private 成员：

```cpp
class Vec2 {
    double x, y;  // private
    friend std::ostream& operator<<(std::ostream& os, const Vec2& v);
};
// 定义在外面——但不是成员，是 friend 函数
std::ostream& operator<<(std::ostream& os, const Vec2& v) {
    return os << '(' << v.x << ", " << v.y << ')';  // 访问 private x, y
}
```

### 7.4 核心原则——保持运算符的期望语义

运算符重载是 C++ 哲学的两面——用好了写出自然、可读的代码（`vec1 + vec2` 一目了然）。用坏了——`+` 做"连接字符串然后发网络请求"——读代码的人要翻开所有重载才能理解一行代码。规则：**运算符应该做和内置类型相同语义的事**——`+` 就是表达"加法"。

---

## 8. 可运行错误实验

### 实验 1：`[&]` 捕获 + Lambda 被延迟调用 → 悬垂引用

```cpp
#include <iostream>
#include <functional>

std::function<void()> make_lambda() {
    int x = 42;
    return [&]() { std::cout << x << '\n'; };  // 🔴 x 马上销毁
}

int main() {
    auto f = make_lambda();
    f();  // 🔴 UB——访问已销毁的 x（随机值或 crash）
    return 0;
}
```

**编译运行**：`g++ -std=c++11 dangling.cpp -o dangling && ./dangling`
**输出**：随机值（`0` 或 `32767`）或 crash——这是 UB。

**修复**：把 `[&]` 改成 `[=]`——按值捕获，Lambda 拥有 `x` 的拷贝。

### 实验 2：闭包对象的 sizeof

```cpp
#include <iostream>
#include <vector>

int main() {
    int a = 1, b = 2;
    std::vector<int> v = {1, 2, 3, 4, 5};

    auto f1 = []{};
    auto f2 = [a]{};
    auto f3 = [&a]{};
    auto f4 = [v]{};

    std::cout << "no capture:   " << sizeof(f1) << " (expect 1)\n";
    std::cout << "capture int:  " << sizeof(f2) << " (expect 4)\n";
    std::cout << "capture int&: " << sizeof(f3) << " (expect 8, a pointer)\n";
    std::cout << "capture vec:  " << sizeof(f4) << " (expect " << sizeof(v) << ")\n";
    return 0;
}
```

**编译运行**：`g++ -std=c++11 closure_size.cpp -o closure_size && ./closure_size`

---

## 9. 面试题

### 面试题 1：Lambda 闭包对象的内存模型

**面试官**：`auto f = [x, &y](int z) { return x + y + z; };` — `f` 是什么类型？闭包对象里有什么？

**回答**：`f` 是编译器生成的匿名类的一个实例——每个 Lambda 有唯一类型。闭包对象内部有两个成员——`int x`（按值捕获，拷贝构造初始化）和 `double& y`（按引用捕获，引用绑定到外部变量）。`operator()(int z)` 访问闭包成员 `x` 和引用成员 `y`。闭包对象大小 = 按值捕获的成员变量之和。不捕获任何东西时 sizeof = 1（C++ 要求空对象至少 1 字节）。

**追问（面试官）**：`[=]` 按值捕获一切——那为什么 `this` 没有被拷贝？

**追问回答**：C++11 的 `[=]` 对 `this` 是隐式按引用捕获——不是拷贝。设计原因：(1) 拷贝 `*this` 需要知道完整的类定义（声明可能还没结束）；(2) 不经意间拷贝大对象有性能隐患。C++20 废弃了 `[=]` 的隐式 `this` 捕获。C++11 里写 `[this, x]` 明确捕获 `this` + 按值捕获 `x` 即可。

### 面试题 2：Lambda vs std::function

**面试官**：什么时候用 `auto f = [](...){...};`，什么时候用 `std::function<void()> f = ...;`？性能差别是什么？

**回答**：`auto` Lambda——零开销（直接调 `operator()`，编译器可 inline），但类型是匿名的——不能存到需要统一类型的容器。`std::function`——有开销（类型擦除 + 可能堆分配），但类型统一——可以存到 `map<string, function<void()>>` 做回调注册表。规则——能用 `auto` 就用 `auto`；需要"运行时统一存储不同类型可调用对象"时才用 `std::function`。

**追问（面试官）**：`std::function` 的"可能堆分配"是什么场景下触发？什么场景不触发？

**追问回答**：`std::function` 内部有一个 SBO 小缓冲区（通常 16-32 字节）。不捕获任何东西的 Lambda（`[]`）和只捕获几个小类型变量的 Lambda 通常能放进 SBO——不触发堆分配。按值捕获大对象（如 `vector<int>`）的 Lambda 超出 SBO 容量→触发堆分配。这和 `std::string` 的 SSO 优化原理相同。

### 面试题 3：`mutable` 和内部状态

**面试官**：Lambda 可以"记住"上一次调用的结果吗？给一个例子。

**回答**：`mutable` + 按值捕获——`int counter = 0; auto gen = [counter]() mutable { return ++counter; };`。每次调用 `gen()` 返回递增数字——闭包内的 `counter` 成员在调用间保持状态。实际场景——`std::generate` 生成递增序列号。注意 `mutable` Lambda 不能以 `const` 传递——因为 `operator()` 不是 `const`。

**追问（面试官）**：两个副本的这个 Lambda——它们的内部状态是独立的吗？

**追问回答**：是的——每个闭包对象有自己独立的成员变量。`auto g1 = gen; auto g2 = gen;` —— `g1()` 和 `g2()` 各自维护各自的 `counter`——互不影响。这和 `static` 局部变量完全不同——`static` 变量是所有调用共享的。闭包对象的拷贝就是状态拷贝——每个对象都有完整的成员变量副本。

---

## 10. 本章小结

| 概念 | 核心理解 |
|---|---|
| Lambda 是对象 | 匿名类实例——有构造函数、成员变量、`operator()` |
| 捕获机制 | 按值捕获 = 闭包成员拷贝；按引用捕获 = 闭包成员引用 |
| `[=]` 不拷贝 this | C++11 的陷阱——this 隐式按引用捕获 |
| 闭包 sizeof | 不捕获 = 1 字节；按值捕获 = 成员大小之和 |
| `mutable` | 去掉 `operator()` 的 const——允许修改闭包成员 |
| Lambda vs function | `auto` → 零开销，匿名；`function` → SBO + 类型擦除，统一类型 |
| SBO | `std::function` 小缓冲区优化——小 Lambda 不触发堆分配 |
| 运算符重载 | `operator()` 是起点——`+`/`[]`/`<<` 遵循期望语义 |
| `friend` | 非成员运算符访问 private——`operator<<` 的经典模式 |

**本章没讲但后续章节会讲**：泛型 Lambda（`[](auto x){...}` —— C++14）、init capture（`[x = 42]` —— C++14）、`std::bind` 的进阶用法（参考手册即可）。

下一章进入**类型萃取与模板元编程**——`is_integral`/`decay`/`common_type` 的内部实现、`constexpr` 函数的编译时威力、以及为什么 `decltype((x))` 返回引用。
