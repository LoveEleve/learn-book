# Stage4 Ch01 规划：Lambda 与函数对象

> **定位**：Stage4 第一章。前提——读者在 Stage2 Ch03 见过 Lambda 基本语法（`[](int x){ return x>100; }`、`[=]`/`[&]`、`std::function`），理解 STL 算法和函数对象的配合。
> **主线版本**：C++11
> **参考书**：深入理解C++11 §6，C++程序设计语言 §11

---

## 1. 本章要解决的核心问题

Stage2 Ch03 教了 Lambda 的基本用法——用 `find_if` 的时候写个 `[](int x) { return x > 100; }`。但 Lambda 不是"匿名函数的语法糖"——它是一个**编译器生成的闭包对象**（closure object），有构造函数、成员变量、`operator()`。

本章从"用 Lambda"升级到"理解 Lambda"——闭包对象的生命周期、捕获机制的内存模型、什么时候用 `auto` 存 Lambda 什么时候用 `std::function`。学完本章你不再"跟着感觉写 Lambda"——你知道每一行 Lambda 背后编译器生成了什么。

---

## 2. 节结构（5 节）

### §1 Lambda 不是函数——是对象

Lambda 表达式编译后不是函数指针——是一个**匿名类的实例**：

```cpp
// 你写的：
auto add = [](int a, int b) { return a + b; };

// 编译器生成的（等价的伪代码）：
struct __lambda_123 {
    int operator()(int a, int b) const { return a + b; }
};
__lambda_123 add;
```

- **每个 Lambda 有唯一的类型**——即使两个 Lambda 的签名完全一样（都返回 `int`，都接受两个 `int`），它们的类型也是不同的。
- **`operator()` 默认是 `const`**——Lambda 内部不能修改按值捕获的变量，除非加 `mutable`。
- **为什么用 `auto` 存 Lambda**——你不知道（也不应该手写）编译器生成的具体类型名。`auto` 是唯一的方式。

### §2 捕获——把外部变量"搬进"闭包对象

捕获列表不只是语法——每个被捕获的变量变成了闭包对象的**成员变量**：

```cpp
int x = 10;
double y = 3.14;

auto f = [x, &y](int z) { return x + y + z; };

// 编译器生成的（等价伪代码）：
struct __lambda_456 {
    int  x;       // = 10 ——按值捕获，调用拷贝构造函数
    double& y;    // 引用——按引用捕获
    __lambda_456(int x_, double& y_) : x(x_), y(y_) {}
    auto operator()(int z) const { return x + y + z; }
};
```

- **按值捕获 `[x]`**：闭包对象有一个 `int x` 成员——初始化为 `x` 的拷贝。Lambda 内部的 `x` 是闭包对象的成员，和外部的 `x` 无关。
- **按引用捕获 `[&y]`**：闭包对象有一个 `double& y` 成员——引用外部的 `y`。修改 Lambda 内部的 `y` 会改变外部的 `y`。
- **`[=]` vs `[this]`**：`[=]` 按值捕获所有局部变量——但 `this` 总是按引用捕获（隐式捕获 `this` 指针）。这是 C++11 的设计折衷——如果拷贝 `*this`，闭包对象必须知道完整类定义（此时类可能还没完整声明），且不经意间拷贝大对象有性能隐患。C++14 的 `[*this]` 才能显式拷贝整个对象。
- **捕获成员变量**：只能捕获局部变量和 `this`——

```cpp
class Widget {
    int x;
    auto getter() { return [this]{ return this->x; }; }  // 捕捉 this
    // 等价于返回 [x = this->x]{ ... } (C++14, init capture)
};
```
- C++11 不能 `[x]` 直接捕获成员（报"未捕获局部变量"）——只能捕获 `this`。C++14 的 init capture 解决了这个问题。

### §3 `mutable` ——让 Lambda 可以修改按值捕获的变量

```cpp
int counter = 0;
auto f = [counter]() mutable {  // 不加 mutable——编译错误
    return ++counter;           // counter 是闭包对象的成员——修改它
};
std::cout << f();  // 1
std::cout << f();  // 2
std::cout << counter;  // 0 ——外部 counter 没变
```

- **`mutable` 做了什么**：去掉 `operator()` 的 `const` 修饰——Lambda 内部可以修改闭包成员。
- **修改的是闭包对象的成员**——不影响外部的 `counter`（因为是按值捕获的拷贝）。
- **`mutable` 的实际用例**：给 Lambda "内部状态"——`std::generate` 需要一个每次调用返回值递增的函数，`mutable` Lambda 内部维护一个计数器。

### §4 Lambda vs `std::function` vs 函数对象——性能与选择

| 方式 | 类型 | 开销 | 何时用 |
|---|---|---|---|
| `auto f = [](int x){...};` | 编译器生成的匿名类型 | 零开销——直接调用 `operator()`，可 inline | **首选**——当你不需要"存多种 Lambda"时 |
| `std::function<void(int)> f = ...;` | 标准库类型擦除包装 | 有开销——虚表查找 + 可能堆分配 | 回调注册表、类成员变量、需要存不同类型的可调用对象 |
| 函数指针 `void (*)(int)` | 裸指针 | 零开销——但不能存有捕获的 Lambda | 和 C API 互操作 |
| 函数对象（自定义 struct） | 自定义类型 | 零开销——但需要手写类 | 需要复用同一个逻辑 + 内部状态 |

- **Lambda 和 `std::function` 的选择**：Lambda 是零开销但��型是匿名的——`auto` 推导；`std::function` 有开销但类型是统一的——任何可调用东西都能存。模板用 Lambda（`template<typename F> void sort(F cmp)`），类成员用 `std::function`。
- **`std::function` 的 SBO（Small Buffer Optimization）**：内部有一个小缓冲区（通常 16-32 字节）——如果可调用对象的大小不超过这个缓冲区，不需要堆分配。不捕获任何东西的 Lambda 和只捕获几个 int 的 Lambda 通常能放进 SBO。捕获大对象时需要堆分配——和 `std::string` 的 SSO 一样的优化思路。
- **函数对象的现代写法**：C++98 用 `struct` + `operator()`——C++11 用 Lambda 替代 90% 的场景。但需要内部状态 + 复用 + 有名字时仍然写函数对象。

### §5 `std::bind`——"部分应用"的替代（简讲）

```cpp
#include <functional>
int add(int a, int b) { return a + b; }

// bind 的第二个参数固定为 10
auto add10 = std::bind(add, std::placeholders::_1, 10);
std::cout << add10(5);  // 15 ——等价于 add(5, 10)
```

- **`bind` 的历史地位**：C++98 时代的函数对象"工厂"。C++11 Lambda 出现后——大部分 `bind` 场景用 Lambda 更清晰。`auto add10 = [](int x) { return add(x, 10); };` 比 `bind` 直观。
- **`bind` 的遗留用途**：需要"绑定成员函数"——`std::bind(&Widget::method, &w, _1)`。但即便是这个——C++11 的 Lambda 也可以：`[&w](int x) { return w.method(x); }`。
- **本节定位**：一句话总结——"见过就行，能用 Lambda 替代就不用 bind"。

---

## 3. 编写方针

1. **§2 捕获机制是本章灵魂**——必须用"等价伪代码"（`__lambda_456` struct）让读者看到闭包对象的真实布局
2. **§4 选择矩阵是面试核心**——Lambda/function/函数指针/函数对象四种方式的性能对比表
3. **每个概念配可运行代码**——读者编译运行亲眼看到闭包对象的 `sizeof`、`mutable` 的效果
4. 本章不讲的内容：泛型 Lambda（`[](auto x){...}` —— C++14）、init capture（`[x = 42]` —— C++14）、`std::bind` 的进阶用法

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：Lambda 闭包对象的内存模型

**面试官**：`auto f = [x, &y](int z) { return x + y + z; };` — `f` 是什么类型？闭包对象里有什么？

**回答**：`f` 是编译器生成的匿名类的一个实例——每个 Lambda 表达式有唯一的类型。闭包对象内部有两个成员——`int x`（按值捕获，拷贝构造）和 `double& y`（按引用捕获）。`operator()(int z)` 里 `x` 访问闭包成员，`y` 通过引用访问外部变量。闭包对象的大小取决于按值捕获的变量数量和大小——不捕获任何东西时 `sizeof` = 1（C++ 要求空对象至少 1 字节）。

**追问（面试官）**：`[=]` 按值捕获一切——那为什么 `this` 没有被拷贝？

**追问回答**：C++11 的 `[=]` 对 `this` 是**隐式按引用捕获**——不是拷贝。这是设计上的折衷——如果拷贝 `*this`，那闭包对象必须知道完整的类定义（此时类可能还没完整声明），且拷贝大对象在不知不觉中发生会有性能隐患。C++14 的 init capture `[*this]` 才能拷贝整个对象。C++11 里写 `[this]` 明确表示"我可能需要访问成员变量"。

### 面试题 2：Lambda vs std::function

**面试官**：什么时候用 `auto f = [](...){...};`，什么时候用 `std::function<void()> f = ...;`？性能差别是什么？

**回答**：`auto` Lambda——零开销（直接调 `operator()`，可 inline），但类型是匿名的——不能存到需要统一类型的容器里。`std::function`——有开销（类型擦除 + 可能堆分配），但类型统一——可以存到 `map<string, function<void()>>` 做回调注册表。规则——能用 `auto` 就用 `auto`；需要统一类型存储时用 `std::function`。

**追问（面试官）**：`std::function` 的"可能堆分配"是什么场景下触发？什么场景不触发？

**追问回答**：`std::function` 内部有一个小缓冲区（SBO，Small Buffer Optimization）——如果可调用对象的大小不超过这个缓冲区（通常 16-32 字节），就不需要堆分配。不捕获任何东西的 Lambda（`[]`）和只捕获几个 int 的 Lambda 通常能放进 SBO。捕获大对象的 Lambda 或 `std::bind` 的结果可能需要堆分配。这是和 `std::string` 的 SSO 类似的优化。

### 面试题 3：mutable 和内部状态

**面试官**：Lambda 可以"记住"上一次调用的结果吗？给一个例子。

**回答**：`mutable` + 按值捕获——`auto gen = [counter = 0]() mutable { return ++counter; };`（C++14 init capture）或 C++11 写法 `int counter = 0; auto gen = [counter]() mutable { return counter; }; counter++; ...`。每次调用 `gen()` 返回递增的数字——闭包对象内部的 `counter` 成员在调用之间保持状态。实际场景——`std::generate` 生成递增序列号。

**追问（面试官）**：两个副本的这个 Lambda——它们的内部状态是独立的吗？

**追问回答**：是的——每个闭包对象有自己独立的成员变量。`auto g1 = gen; auto g2 = gen;` —— `g1()` 和 `g2()` 各自维护各自的 `counter`——互不影响。这和使用 `static` 局部变量的函数不同——`static` 变量是所有调用共享的。

---

## 5. 可运行错误实验

### 实验 1：`[&]` 捕获 + Lambda 被延迟调用 → 悬垂引用

```cpp
#include <iostream>
#include <functional>

std::function<void()> make_lambda() {
    int x = 42;
    return [&]() { std::cout << x << "\n"; };  // 🔴 x 马上销毁
}

int main() {
    auto f = make_lambda();
    f();  // 🔴 UB——访问已销毁的 x
    return 0;
}
```

### 实验 2：闭包对象的 sizeof

```cpp
#include <iostream>
#include <vector>

int main() {
    int a = 1, b = 2;
    std::vector<int> v = {1,2,3,4,5};

    auto f1 = []{};                         // 不捕获
    auto f2 = [a]{};                        // 捕获一个 int
    auto f3 = [&a]{};                       // 引用捕获一个 int
    auto f4 = [v]{};                        // 捕获 vector——拷贝大对象

    std::cout << "no capture:     " << sizeof(f1) << " (expect 1)\n";
    std::cout << "capture int:    " << sizeof(f2) << " (expect 4)\n";
    std::cout << "capture int&:   " << sizeof(f3) << " (expect 8, a pointer)\n";
    std::cout << "capture vector: " << sizeof(f4) << " (expect " << sizeof(v) << ")\n";
    return 0;
}
```

**预期**：不捕获=1 字节（C++ 要求空对象至少 1 字节）、按值捕获 int=4 字节、按引用捕获=int* 的 8 字节、按值捕获 vector=拷贝整个 vector 的大小。

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | Lambda 闭包对象的等价伪代码（member init/operator()/mutable 去 const）；`std::function` SBO 小缓冲区优化机制；按值捕获=调用拷贝构造的语义；C++11 `[=]` 对 `this` 隐式按引用捕获的工程设计理由 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（闭包对象内存模型+this 捕获 / Lambda vs function+SBO / mutable 内部状态+副本独立），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | `[&]` 悬垂引用可运行实验；`[=]` 对 this 隐式引用的陷阱；`std::function` SBO 大小的平台差异 | 8/10 |
| 交叉引用 | §1 Lambda 闭包对象→Stage2 Ch03 Lambda 基本用法；§2 捕获成员变量→Stage3 Ch01 this 指针；§4 std::function SBO→Stage2 Ch02 string SSO 类比；§5 bind→Stage2 Ch03 函数对象 | 8/10 |
| **总分** | | **33/40** |
