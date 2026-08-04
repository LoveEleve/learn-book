# Stage4 Ch02 规划：类型萃取与模板元编程进阶

> **定位**：Stage4 第二章。前提——读者已完成 Stage2（模板基础/变参模板/SFINAE/enable_if），理解编译器"按需生成模板代码"的编译模型。
> **主线版本**：C++11
> **参考书**：深入理解C++11 §7-8，C++程序设计语言 §28

---

## 1. 本章要解决的核心问题

Stage2 Ch04 教你用 `enable_if` 做编译时分派——"T 是整数类型就启用版本 A"。但 `enable_if` 只用到了 `is_integral<T>`——这只是 `<type_traits>` 的冰山一角。

`<type_traits>` 提供了**编译时的类型检测、类型变换、类型计算**——这是模板元编程的工具箱。`decltype` 让你从表达式推导类型而不执行表达式。`constexpr` 函数让编译时计算变得像写普通函数一样自然。tag dispatch 让你不用 `enable_if` 也能做编译时分派——而且更清晰。

本章教你"模板库作者"的思维——不光是"会调用 STL 模板"，而是"能写出自己的泛型库"。

---

## 2. 节结构（5 节）

### §1 `<type_traits>`——编译时的"类型数据库"

`<type_traits>` 提供三大类模板——检测、变换、计算：

```cpp
#include <type_traits>

// 第一类：类型检测——"这个类型有什么特性？"
static_assert(std::is_integral<int>::value,     "int is integral");       // ✓
static_assert(std::is_pointer<int*>::value,      "int* is a pointer");    // ✓
static_assert(std::is_class<std::string>::value, "string is a class");    // ✓
static_assert(std::is_same<int, int>::value,     "same type");            // ✓
static_assert(std::is_base_of<Animal, Dog>::value, "Dog inherits Animal");// ✓

// 第二类：类型变换——"从这个类型变出另一个类型"
using T = int;
using ConstT = std::add_const<T>::type;            // const int
using PtrT   = std::add_pointer<T>::type;           // int*
using RawT   = std::remove_reference<int&>::type;   // int
using DecayT = std::decay<const int&>::type;        // int（去掉 const &）

// 第三类：类型计算——"给出一组类型中的'最优'选择"
using Common = std::common_type<int, double>::type; // double
// C++14 起有 _t/_v 简写：is_integral_v<T>、add_const_t<T> 等。
// C++11 里自己写 template aliases 也能做到同样的简化。
```

- **为什么叫"萃取"（trait）**：你从类型中"榨取"信息——不运行代码，不等运行时，编译时就知道。
- **和 C 的对比**：C 里你要知道 sizeof(int) 可以用 `sizeof`——但你要知道"T 是不是整数"——没有。类型检测全靠手动。
- **`static_assert` 配合**：编译时断言——如果条件为 false，编译直接报错。和运行时 `assert` 不同——错误在编译阶段暴露。

**本节重点展示的 10 个最常用 trait**：

| trait | 作用 | 生产场景 |
|---|---|---|
| `is_integral<T>` | T 是整数类型？ | SFINAE 限制模板只接受整数 |
| `is_floating_point<T>` | T 是浮点？ | 同上 |
| `is_pointer<T>` | T 是指针？ | 区分 "delete vs delete[]" |
| `is_class<T>` | T 是 class/struct？ | 区分基本类型和对象类型 |
| `is_same<A, B>` | A 和 B 相同类型？ | 模板特化判断、禁止某种实例化 |
| `is_base_of<Base, Derived>` | Derived 继承自 Base？ | 运行时类型检查的编译时版本 |
| `enable_if<Cond, T>` | Cond 为 true 时类型 T 有效 | SFINAE（Stage2 Ch04 已讲） |
| `remove_reference<T>` | 去掉 T 的引用 | 模板内部需要"原始类型" |
| `decay<T>` | 去掉 const/volatile/引用 | 标准库 `std::move` 和 `std::forward` 内部用它 |
| `conditional<Cond, A, B>` | Cond 为 true→A，false→B | 编译时 if-else |

### §2 `decltype`——从表达式"抄"类型

`auto` 从初始化表达式推导类型。`decltype` 从任意表达式推导类型——**不执行表达式**：

```cpp
int x = 5;
decltype(x)    y = 10;   // y 是 int——从 x 的类型推导
decltype(x+0.5) z = 3.14; // z 是 double——x+0.5 的类型

std::vector<int> v = {1,2,3};
decltype(v[0])  elem = v[0];   // elem 是 int&——v[0] 返回引用
decltype(v.size()) sz = v.size(); // sz 是 size_t

// C++11 返回类型后置——模板函数的返回类型依赖参数
template <typename T1, typename T2>
auto multiply(T1 a, T2 b) -> decltype(a * b) {  // 🆕 返回类型后置
    return a * b;
}
```

- **`decltype` vs `auto`**：`auto` 推导类型但**丢弃**引用和 const；`decltype` 原样保留引用和 const。
- **`decltype(expr)` vs `decltype((expr))`**：括号括起来的表达式→`decltype((x))` 返回 `int&`（左值引用），`decltype(x)` 返回 `int`。这是最容易踩的坑——面试必考。
- **返回类型后置**：C++11 中如果模板函数的返回类型依赖参数类型——`decltype(a * b)`——必须把类型写在 `->` 后（参数在这之前声明了）。C++14 起 `auto` 返回类型推导简化了这个需求。

### §3 `constexpr` 函数——编译时计算

C++11 的 `constexpr` 不仅用于变量——还可以用于函数：

```cpp
// 编译时的阶乘——编译器在编译时就算出值
constexpr int factorial(int n) {
    return n <= 1 ? 1 : n * factorial(n - 1);
}

int arr[factorial(5)];      // = arr[120] ——编译时常量
static_assert(factorial(5) == 120, "bad math"); // 编译时断言

// 生产场景：编译时计算哈希值
constexpr unsigned int hash(const char* s, unsigned int h = 0) {
    return *s == '\0' ? h : hash(s + 1, (h * 31 + *s));
}
static_assert(hash("hello") != hash("world"), "hash collision!");
```

- **`constexpr` 函数的限制（C++11）**：函数体只能包含一个 `return` 语句——不能有循环、局部变量、分支。C++14 放宽了这些限制（可以有多个 return、循环等）。
- **和模板元编程的对比**：模板也可以做编译时计算（`template<int N> struct Factorial { ... }`），但 `constexpr` 函数更直观——写起来像普通函数。
- **`constexpr` vs `const`**：`const` 变量可能是运行时确定的（`const int x = rand();`），`constexpr` 保证编译时确定。

### §4 Tag Dispatch——不用 `enable_if` 的编译时分派

`enable_if` 做 SFINAE 功能强大但代码丑陋。Tag dispatch 用更清晰的方式实现编译时分派：

```cpp
// 定义 tag 类型——空 struct，纯做编译时标记
struct random_access_tag {};
struct bidirectional_tag {};
struct forward_tag {};

// 给容器"问"它支持哪种迭代器（用 trait 萃取）
template <typename T> struct iterator_traits {};
// vector 的迭代器是随机访问
template <> struct iterator_traits<std::vector<int>::iterator> {
    using category = random_access_tag;
};

// 根据 tag 选择不同实现——函数重载区分
template <typename Iter>
void advance(Iter& it, int n, random_access_tag) {
    it += n;  // O(1)——随机访问迭代器可以直接跳
}

template <typename Iter>
void advance(Iter& it, int n, bidirectional_tag) {
    if (n > 0) while (n--) ++it;  // O(n)——双向迭代器只能一步步走
    else       while (n++) --it;
}

// 统一入口——用 trait 获取 tag，自动分派
template <typename Iter>
void advance(Iter& it, int n) {
    advance(it, n, typename iterator_traits<Iter>::category{});
}
```

- **Tag dispatch vs `enable_if`**：tag dispatch 用函数重载区分版本（清晰）、`enable_if` 用 SFINAE 排除版本（强大但难读）。优先 tag dispatch——只在 tag 方案不够时才用 `enable_if`。
- **STL 大量使用 tag dispatch**——`std::advance`/`std::distance`/`std::copy` 都��用迭代器 tag 选择正确的实现。
- **空 tag 对象零开销**——`random_access_tag{}` 是空对象（大小 1 字节），作为函数参数不生成任何代码（编译器优化掉）。

### §5 模板元编程实战——编译时选择容器

把 trait、decltype、tag dispatch 串起来——写一个"根据类型选择最优容器"的工具：

```cpp
// 编译时选择：小类型用 vector，大类型用 list
template <typename T>
using best_container = typename std::conditional<
    (sizeof(T) <= 64),                  // 编译时条件
    std::vector<T>,                      // 小→vector
    std::list<T>                         // 大→list
>::type;

best_container<int>  good_for_int;    // vector<int>——int 只有 4 字节
best_container<std::array<char, 256>> big; // list<array<char,256>>——array 是 256 字节
```

这个例子展示了 trait + conditional + 模板别名的组合——编译时根据类型大小自动选择最优容器。不是语法教学——是"工具组合解决实际问题"的思维。

---

## 3. 编写方针

1. **§1 trait 速查表是本章骨架**——10 个最常用的 trait，每个配一个短代码示例
2. **decltype 双括号陷阱必须强调**——`decltype(x)` vs `decltype((x))` 面试必考
3. **tag dispatch 是"更好的 enable_if"**——展示了不依赖 SFINAE 的编译时分派方案
4. 本章不讲的内容：完整模板元编程（图灵完备/SFINAE 高级技巧）——太深，留给读者自己探索

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：decltype 双括号陷阱

**面试官**：`int x = 5; decltype(x)` 和 `decltype((x))` 返回什么类型？有什么区别？

**回答**：`decltype(x)` 返回 `int`——`x` 是变量的**名字**，decltype 返回变量的声明类型。`decltype((x))` 返回 `int&`——`(x)` 是一个**表达式**（带括号的名字被当作左值表达式），decltype 对左值表达式返回引用类型。这个差异是 C++ 标准故意设计的——用于区分"我要这个变量的类型"和"我要这个表达式的值类型"。

**追问（面试官）**：那 `decltype(auto)` 呢？和 `auto` 的区别？

**追问回答**：`decltype(auto)`（C++14）——用 `decltype` 的规则推导类型而非 `auto` 的规则。`auto` 丢掉引用和 const→`auto x = func()` 中 `func()` 返回 `int&` 时 `x` 是 `int` 不是 `int&`。`decltype(auto) x = func()`——保留引用和 const。如果需要完美转发返回值的引用属性，用 `decltype(auto)`。

### 面试题 2：constexpr 函数 vs 普通函数

**面试官**：C++11 的 `constexpr` 函数有什么限制？和 C++14 的区别？

**回答**：C++11 限制——函数体只能包含一个 `return` 语句（不能有循环/局部变量/if/switch），但有递归（通过三元运算符 `?:` 和递归调用）。C++14 放宽——可以有多条语句、循环、局部变量、if 分支。所以 C++11 的阶乘写成 `return n <= 1 ? 1 : n * factorial(n-1)`，C++14 可以写成普通的 `if/for` 形式。

**追问（面试官）**：`constexpr` 函数一定在编译时执行吗？什么时候走运行时？

**追问回答**：不一定——`constexpr` 表示"在这个上下文中可以被编译时求值"。如果传递给 `constexpr` 函数的参数是编译时常量（如 `factorial(5)`）→编译器在编译时算好值。如果参数是运行时值（如 `factorial(n)` 中 `n` 是运行时输入）→函数像普通函数一样在运行时执行。`constexpr` 是可能性不是强制性。

### 面试题 3：tag dispatch vs enable_if

**面试官**：tag dispatch 和 `enable_if` 的区别？什么时候用哪个？

**回答**：tag dispatch——用函数重载 + tag 类型区分——清晰、零语法负担。`enable_if`——用 SFINAE 在模板替换阶段排除无效版本——灵活但语法复杂。优先 tag dispatch——只有在需要"根据多种布尔条件组合选择"时才用 `enable_if`。STL 的 `advance`/`distance`/`copy` 大量用 tag dispatch——只有少数场景（如根据 `is_copy_constructible` 判断移动 vs 拷贝）用 `enable_if`。

**追问（面试官）**：tag dispatch 用的空 tag 对象有运行时开销吗？

**追问回答**：没有——空 struct 对象作为函数参数传入，编译器在优化时直接内联并消除它（不产生任何指令）。tag dispatch 是零运行时开销的编译时分派机制——编译后的代码和手写 if-else 一样快，但类型安全得多（编译时就知道类型，不需要运行时判断）。

---

## 5. 可运行错误实验

### 实验：`decltype((x))` 返回引用的陷阱

```cpp
#include <iostream>
#include <type_traits>

int main() {
    int x = 5;

    auto a = x;                         // a 是 int
    static_assert(std::is_same<decltype(a), int>::value);

    decltype(x)  b = x;                 // b 是 int
    static_assert(std::is_same<decltype(b), int>::value);

    decltype((x)) c = x;                // c 是 int& ——小心！引用 x
    static_assert(std::is_same<decltype(c), int&>::value);
    c = 10;                             // 修改 c 就是修改 x
    std::cout << "x=" << x << "\n";      // 10——不是 5

    return 0;
}
```

**核心体验**：`decltype((x))` 返回引用的规则不是语言 bug——是故意设计的。`(x)` 作为一个左值表达式，decltype 对左值表达式返回引用。`x` 作为变量名，decltype 返回变量声明类型。用 `static_assert` + `is_same` 编译时验证这些区别。

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | 10 个核心 trait 的编译时检测机制（模板特化+integral_constant）；`decltype` 双括号规则的标准依据；`constexpr` 函数 C++11 单 return 限制与 C++14 放松的编译器实现差异；tag dispatch 空对象被编译器消除的优化原理 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（decltype 双括号 / constexpr 编译时 vs 运行时 / tag dispatch vs enable_if），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | `decltype((x))` 返回引用的可运行实验；`constexpr` 函数在"看起来是常量"实际走运行时的场景；`std::decay` 在模板参数推导中的隐性应用 | 8/10 |
| 交叉引用 | §3 `constexpr` 函数→Stage1 Ch01 constexpr 变量；§4 tag dispatch→Stage2 Ch04 SFINAE enable_if；§2 decltype→Stage1 Ch01 auto 类型推导 | 8/10 |
| **总分** | | **33/40** |
