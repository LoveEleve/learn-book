# Stage2 Ch04 规划：变参模板与 SFINAE

> **定位**：Stage2 第四章（最后一章）。前提——读者已完成 Ch01 模板基础（函数模板/类模板/全特化），理解编译器"按需生成"的模板编译模型。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §28，深入理解C++11 §7-8

---

## 1. 本章要解决的核心问题

Ch01 的模板只能接受固定数量、固定类型的参数——`template <typename T>`。如果你的 `printf` 需要任意个参数呢？如果你的 `make_unique` 需要把任意个构造参数转发给 `T` 的构造函数呢？

C 里处理可变参数用 `...`（`printf(const char* fmt, ...)`）——运行时解析、无类型安全。C++11 给了**变参模板**——编译时递归展开、完全类型安全。

同时，模板写好后有时需要"条件编译"——"只有 T 是整数类型时才启用这个函数，否则编译报错"。C 里用 `#ifdef`——预处理器层面。C++ 用 **SFINAE**（替换失败不是错误）——编译时类型层面。

本章是 Stage2 的技术制高点。学完你就能读懂 STL 源码里的 `enable_if` 和变参模板。

---

## 2. 节结构（5 节）

### §1 变参模板——从一个简单的 `print` 开始

从 C 的 `printf` 痛点出发：

```cpp
// 需求：一个类型安全的 print——接受任意类型、任意数量的参数
// 递归的终止条件——没有参数了
void print() {}  // 空函数，递归到底

// 递归——打印第一个参数，然后递归打印剩下的
template <typename T, typename... Args>
void print(T first, Args... rest) {
    std::cout << first;
    if (sizeof...(rest) > 0) std::cout << ", ";
    print(rest...);   // 递归展开——调 print(第二个, 第三个, ...)
}

// 使用
print(1, 2.5, "hello", 'c');
// 输出：1, 2.5, hello, c
```

- **`typename... Args`**：模板参数包——"零个或多个类型"
- **`Args... rest`**：函数参数包——"零个或多个值"
- **`rest...`**：包展开——把包里的每个元素"展开"成独立的参数
- **递归终止**：`print()` 的空版本——最后一个参数处理完后，编译器匹配空版本，递归结束
- **和 C `printf` 的对比**：C 的 `...` 需要格式串 `"%d %s"` →运行时解析→类型不匹配→UB。C++ 变参模板在**编译时**检查类型——`print(1, "hello")` 编译时就知道第一个是 `int`、第二个是 `const char*`。

### §2 变参模板的经典应用——完美转发和 `emplace_back`

变参模板最常见的用途：**把参数"原样转运"给另一个函数**。这是 STL 中 `emplace_back`、`make_shared`、`make_unique` 的基础：

```cpp
#include <memory>

// 自己实现 make_unique（C++11 没有，C++14 才有）
template <typename T, typename... Args>
std::unique_ptr<T> make_unique(Args&&... args) {
    return std::unique_ptr<T>(new T(std::forward<Args>(args)...));
}

// 使用——把构造参数原样转发给 T 的构造函数
auto p = make_unique<std::string>(5, 'x');  // string("xxxxx")
auto q = make_unique<std::pair<int, double>>(1, 3.14);
```

- **`Args&&...`（转发引用）**：每个参数保持原有的左值/右值特性——左值保持左值传，右值保持右值传。**为什么能做到**？引用折叠规则——`T& &` → `T&`、`T&& &` → `T&`、`T& &&` → `T&`、`T&& &&` → `T&&`。当参数是左值时 `T` 推导为 `T&`→`T& &&` 折叠为 `T&`（保留左值）；当参数是右值时 `T` 推导为 `T`→`T&&` 不发生折叠（保留右值）。这是 `forward` 能"原样转发"的编译器层面机制。
- **`std::forward<Args>(args)...`**：对每个参数调用 `std::forward`——实现**完美转发**（和 Ch03 移动语义呼应）。
- **`...` 的位置**：和 §1 的递归展开不同，这里 `...` 同时对多个参数作用（`std::forward<Args>(args)...` → 展开为 `forward<A1>(a1), forward<A2>(a2), ...`）。

### §3 SFINAE：编译时说"这个版本不行，换一个"

**Substitution Failure Is Not An Error**——替换失败不是错误。当编译器尝试实例化一个模板时，如果某个候选版本替换失败，编译器**不报错——而是静默排除该版本，继续尝试下一个版本**：

```cpp
// 场景：一个函数，对整数类型返回"integer"，对其他类型返回"other"
#include <type_traits>

// 版本 A：只对整数类型有效
template <typename T>
typename std::enable_if<std::is_integral<T>::value, const char*>::type
type_name(T) { return "integer"; }

// 版本 B：对其他类型有效——互斥
template <typename T>
typename std::enable_if<!std::is_integral<T>::value, const char*>::type
type_name(T) { return "other"; }

int main() {
    std::cout << type_name(42);     // "integer"
    std::cout << type_name(3.14);   // "other"
    return 0;
}
```

- **`enable_if<条件, 类型>`**：条件为 true→`::type` 是第二个参数（这里 `const char*`）；条件为 false→`::type` 不存在→**替换失败→编译器排除此版本**。
- **关键理解**：SFINAE 不是"if-else"——不是运行时选择的。编译器尝试两个版本，版本 A 替换失败→排除，版本 B 替换成功→选中。编译后**只有一个版本**的代码存在。
- **和 `#ifdef` 的区别**：`#ifdef` 在预处理器层面做排除（不知道类型信息），SFINAE 在模板替换层面做排除（完全知道类型信息）。

### §4 `enable_if` 的实用场景

SFINAE 不是学术概念——生产代码中用到它的地方：

```cpp
// 场景 1：限制模板只接受数字类型
template <typename T>
typename std::enable_if<std::is_arithmetic<T>::value, T>::type
square(T x) { return x * x; }

// square("hello") → 编译错误（不是 arithmetic 类型）—比运行时报错安全

// 场景 2：容器类型的通用插入
template <typename Container>
auto insert_front(Container& c, typename Container::value_type val)
    -> decltype(c.insert(c.begin(), val)) {
    return c.insert(c.begin(), val);
}
// 如果 Container 没有 insert 方法→替换失败→编译器排除此模板→可选其他重载
```

- **`is_arithmetic<T>` 等类型萃取**（`<type_traits>`）：`is_integral`、`is_floating_point`、`is_pointer`、`is_class`——几百个预定义的类型检测。
- **返回类型后置 `-> decltype(...)`**：用 `decltype` 推导返回类型——顺便做 SFINAE（如果表达式不合法→替换失败）。

### §5 本章收尾 + Stage2 回顾

**本章收尾**：变参模板和 SFINAE 是 C++ 模板的两大高阶武器——变参模板让你写任意参数数量的泛型代码，SFINAE 让模板在编译时做类型筛选。这两个概念不是"学会语法就行"——需要多看、多写才能内化。

**Stage2 四章回顾**：
```
Ch01 模板基础     → 你学会写"一份代码，多个类型"
Ch02 STL 容器     → 你学会用 vector/map/string，不再手写 C 数据结构
Ch03 STL 算法     → 你学会用 sort/find/count_if 代替手写 for 循环
Ch04 变参与SFINAE  → 你学会模板的高阶用法，能读懂 STL 源码
```

**Stage3 预告**：对象模型与运行时——虚函数、继承、异常安全、C++ 内存模型。模板是"编译时多态"，Stage3 讲的是"运行时多态"。

---

## 3. 编写方针

1. **从"为什么要变参"开始**——不要直接展示语法，先展示痛点（`printf` 的类型不安全、手动传入 5 个参数的痛苦）
2. **SFINAE 用"试试看"比喻**——"编译器挨个试模板版本，不行的跳过，行的用上"——比直接讲标准术语更容易懂
3. **代码量要大但解释要浅**——变参和 SFINAE 的代码看起来"吓人"，每个例子后面必须有逐行解释
4. 本章不讲的内容：`concepts`（C++20）、`constexpr if`（C++17）、完整的模板元编程（超出 C++11 主线）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：变参模板的工作机制

**面试官**：变参模板 `template <typename... Args> void f(Args... args)` 是怎么工作的？`...` 在三个位置各是什么意思？

**回答**：(1) `typename... Args`——模板参数包，等号前表示"零或多个类型参数"；(2) `Args... args`——函数参数包，等号后表示"零或多个值"；(3) `f(args...)`——包展开，调用点把 args 包里的每个元素"展开"成独立的参数。实际工作——变参模板每次递归剥离一个参数——`f(a, b, c)` →处理 `a`，递归调 `f(b, c)` →处理 `b`，递归调 `f(c)` →处理 `c`，递归调 `f()` →匹配空参数版本→终止。

**追问（面试官）**：`sizeof...(Args)` 返回什么？变参模板编译后会不会代码膨胀？

**追问回答**：`sizeof...(Args)` 返回参数包中参数的数量——编译时常量。代码膨胀——每个不同参数组合生成一份独立模板实例——`f(int, double, char)` 和 `f(string, int)` 是两份不同代码。但和不提供变参模板比（你要写 20 个不同参数数量的重载版本），变参模板的"膨胀"是更少的（一份模板覆盖所有参数数量组合）。

### 面试题 2：SFINAE 和 `enable_if`

**面试官**：什么是 SFINAE？用 `enable_if` 给一个实际例子。

**回答**：Substitution Failure Is Not An Error——模板替换时如果某个候选版本不合法，编译器排除它而不报错。`enable_if<条件, 类型>` 是实现——条件为 true→`::type` 存在（模板生效），条件为 false→`::type` 不存在（SFINAE 排除）。场景——`template<typename T> enable_if<is_integral<T>, T> foo(T x)`——只对整数类型有效，对 `double`/`string` 编译错误（没有其他有效重载）。

**追问（面试官）**：`enable_if` 放在返回值里和放在模板参数里有什么区别？

**追问回答**：返回值里——`typename enable_if<..., T>::type`——简洁但返回类型难读，且对构造函数/析构函数无效（没有返回值）。模板参数里——`template <typename T, typename = enable_if<...>>`——对构造函数有效，但占用一个模板参数位置，且多个 `enable_if` 在同一函数上容易冲突（编译器视为重复定义）。C++14 加了 `enable_if_t` 简化写法。

### 面试题 3：变参+SFINAE 的组合

**面试官**：`make_unique` 怎么用变参模板实现？`std::forward` 在里面起什么作用？

**回答**：`template<typename T, typename... Args> unique_ptr<T> make_unique(Args&&... args)`——`Args&&...` 接受任意数量、任意类型的参数。`return unique_ptr<T>(new T(std::forward<Args>(args)...))`——把参数完美转发给 `T` 的构造函数。关键——`std::forward` 保留参数的左值/右值属性（左值→拷贝，右值→移动），`...` 同时对包中每个参数独立调用 `forward`。

**追问（面试官）**：如果 `T` 没有接受这些参数的构造函数会怎样？

**追问回答**：编译错误——在 `make_unique` 实例化时，`new T(args...)` 会触发构造函数查找。如果找不到匹配的构造函数→编译报错，错误信息在 `new T(...)` 的位置（不是 `make_unique` 的调用点）。这比 C 的 `void*` + 运行时 `if(type == INT)` 安全——C++ 在编译时就知道构造不合法。

---

## 5. 可运行错误实验

### 实验：SFINAE 的"安静失败"——亲手触发并理解编译器怎么选的

```cpp
#include <iostream>
#include <type_traits>

// 版本 1：整数版本
template <typename T>
typename std::enable_if<std::is_integral<T>::value, void>::type
print_kind(T) { std::cout << "integer\n"; }

// 版本 2：浮点版本
template <typename T>
typename std::enable_if<std::is_floating_point<T>::value, void>::type
print_kind(T) { std::cout << "floating point\n"; }

// 版本 3：其他版本——没有 enable_if，始终有效
template <typename T>
void print_kind(T) { std::cout << "other\n"; }

int main() {
    print_kind(42);      // integer——版本1 替换成功，版本2 失败被排除
    print_kind(3.14);    // floating point——版本1 失败被排除，版本2 成功
    print_kind("hello"); // other——版本1 和版本2 都失败被排除，版本3 兜底
    return 0;
}
```

**核心体验**：编译器对每个调用点尝试所有三个版本——不合适的静默跳过，合适的选上。三个版本都没有 `#ifdef` 但编译器自动分派——这就是 SFINAE 的意义。

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | 变参模板递归展开的编译时机制（每次剥离一个参数+最终匹配空参数版本）；`enable_if` 的替换失败→静默排除的编译前端处理流程；`forward` + 引用折叠在变参转发中的实现原理 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（变参展开机制+代码膨胀 / SFINAE+enable_if 放哪 / make_unique 变参+forward），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | SFINAE 三种 print_kind 版本的可运行实验（亲手看编译器怎么选）；`enable_if` 忘了给兜底版本→无匹配模板的编译错误；变参递归太深导致编译时间爆炸的提示 | 8/10 |
| 交叉引用 | §1 从 C `printf` 的 `...` 出发 → C 学习路线；§2 完美转发→Ch03 §§3-4 移动语义+std::move；§3 SFINAE → Ch03 模板特化（编译时分派的两种方式）；§5 Stage2 回顾→Stage3 预告 | 8/10 |
| **总分** | | **33/40** |
