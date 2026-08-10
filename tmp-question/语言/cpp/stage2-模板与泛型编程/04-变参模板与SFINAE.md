# 变参模板与 SFINAE

> **前置依赖**：读者已完成 Ch01 模板基础（函数模板/类模板/全特化），理解编译器"按需生成"的模板编译模型。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §28，深入理解C++11 §7-8

---

## 1. 引言：固定参数不够用了

Ch01 的模板只能接受固定数量、固定类型的参数——`template <typename T>`。这对 80% 的场景够了。但剩下 20% 呢？

```cpp
// 你想要的——像 printf 一样接受任意类型、任意数量的参数
print(1, 2.5, "hello", 'c');    // 输出：1, 2.5, hello, c

// Ch01 的模板能做到吗？
template <typename T> void print(T x);           // 只能一个参数
template <typename T1, typename T2> void print(T1 a, T2 b);  // 只能两个
// 要写多少个重载？
```

C 里处理可变参数用 `...`——`printf(const char* fmt, ...)`：

```cpp
// C 的方式——运行时解析，无类型安全
printf("int=%d, double=%f, string=%s\n", 42, 3.14, "hello");
// 如果格式串写错 "%d" 但传了一个字符串 → 运行时 UB
```

C 的 `...` 有三个根本缺陷：(1) 类型不安全——运行时根据格式串猜测类型，猜错就是 UB；(2) 不能传非平凡对象——`std::string`、用户自定义类型不能传；(3) 无法推断参数数量——必须靠格式串或其他哨兵。

C++11 给了**变参模板**（variadic templates）——编译时递归展开、完全类型安全。同时本章还讲**SFINAE**（替换失败不是错误）——编译时类型筛选机制。两个概念合在一起，就是你看 STL 源码时遇到的那些"吓人"的模板代码。

---

## 2. 变参模板——从一个简单的 `print` 开始

### 2.1 递归展开——每次剥离一个参数

变参模板的核心原理是**递归**——每次处理第一个参数，递归调用处理剩余参数，直到参数用完：

```cpp
#include <iostream>

// 递归终止条件——没有参数了
void print() { std::cout << '\n'; }

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

**逐个拆解**：

```
          print(1, 2.5, "hello", 'c')
             │
             ▼ T=int, rest={2.5, "hello", 'c'}
          print(2.5, "hello", 'c')
             │
             ▼ T=double, rest={"hello", 'c'}
          print("hello", 'c')
             │
             ▼ T=const char*, rest={'c'}
          print('c')
             │
             ▼ T=char, rest={}  (空包)
          print()  ← 匹配无参版本，递归终止
```

**三个 `...` 的含义**——这是变参模板最容易混淆的地方：

| 位置 | 写法 | 含义 |
|---|---|---|
| 模板参数声明 | `typename... Args` | **参数包声明**——"零个或多个类型参数" |
| 函数参数声明 | `Args... rest` | **参数包**——"零个或多个值，类型对应 Args" |
| 调用点的包展开 | `print(rest...)` | **包展开**——把 rest 的每个元素独立传给 print |

`rest...` 不是说"把 rest 整体传进去"——它是说"把 rest 里的每个元素展开成独立的参数"。`{a, b, c}...` → `a, b, c`。

> **C++11 的 `if` vs C++17 的 `if constexpr`**：上面的 `if (sizeof...(rest) > 0)` 是运行时 `if`——依赖编译器常量折叠优化来消除死分支。C++17 的 `if constexpr` 才是编译时条件分派：`if constexpr (sizeof...(rest) > 0)` 在编译时就只保留一个分支，不产生死代码。C++11 时代的分支优化依赖编译器——虽然没有正确性问题，但 `if constexpr` 是更精确的表达。

### 2.2 `sizeof...(Args)`——编译时获取参数数量

变参模板里有一个特殊的运算符：

```cpp
template <typename... Args>
void info(Args... args) {
    std::cout << "参数数量: " << sizeof...(Args) << '\n';  // 编译时常量
    std::cout << "参数数量: " << sizeof...(args) << '\n';  // 等价写法
}

info(1, 2, 3);           // 3
info("hello", 3.14);     // 2
info();                  // 0——空包，合法
```

`sizeof...(Args)` 返回参数包的元素数量——**编译时常量**，不是运行时计算的。因为它是在模板实例化时求值的——编译器生成 `info<int, int, int>` 时就知道 Args 里有 3 个类型。

### 2.3 和 C `printf` 的根本区别

把 C 的 `printf` 和 C++ 变参模板放一起看：

```cpp
// C——运行时解析格式串，类型错误→UB
printf("%d %s\n", 42, "hello");     // ✓ 格式串匹配
printf("%s %d\n", "hello", 42);     // ✓ 格式串匹配
printf("%d %d\n", 42, "hello");     // 🔴 格式串说int但传入const char*——UB

// C++ 变参模板——编译时检查类型
print(42, "hello");                 // ✓ 编译时就知道第一个是int，第二个是const char*
print("hello", 42);                 // ✓ 同样类型安全
// 根本不存在"格式串与实际参数不匹配"的漏洞——编译器直接看到参数的真实类型
```

**核心区别**：C 的 `...` 丢失了所有类型信息——运行时靠 `va_arg` 手动猜测类型。C++ 的 `typename... Args` 保留了所有类型信息——编译时递归展开，每一步都知道"当前参数的类型是什么"。

---

## 3. 完美转发——变参模板的经典应用

### 3.1 `emplace_back` 为什么能接受任意参数

回忆 Ch02 §6 的 `emplace_back`：

```cpp
v.emplace_back("hello", 5, 'x');  // 在 vector 内部构造 string("xxxxx")
```

`emplace_back` 怎么做到接受任意数量、任意类型的参数？答案——变参模板 + 完美转发。

### 3.2 自己实现 `make_unique`

C++11 有 `make_shared` 但没有 `make_unique`（直到 C++14）。用变参模板自己实现一个：

```cpp
#include <memory>
#include <utility>

template <typename T, typename... Args>
std::unique_ptr<T> make_unique(Args&&... args) {
    return std::unique_ptr<T>(new T(std::forward<Args>(args)...));
}

// 使用——把构造参数原样转发给 T 的构造函数
auto s = make_unique<std::string>(5, 'x');         // string("xxxxx")
auto p = make_unique<std::pair<int, double>>(1, 3.14); // pair{1, 3.14}
// make_unique 和 emplace_back 本质一样——都是一个参数转发器
```

**逐行拆解**：

1. `typename... Args` ——接受任意数量的模板参数
2. `Args&&... args` ——转发引用（forwarding reference，以前叫万能引用），接受任意类型的参数
3. `std::forward<Args>(args)...` ——对包中每个参数独立调用 `std::forward`，保留左值/右值属性
4. `new T(std::forward<Args>(args)...)` ——把完美转发后的参数传给 `T` 的构造函数

### 3.3 转发引用和引用折叠——`forward` 怎么工作的

`Args&&` 不是右值引用——它是**转发引用**。当传入左值时 `T` 推导为 `T&`，传入右值时 `T` 推导为 `T`：

```cpp
int x = 42;
// make_unique<string>(x)    → Args = {int&},   Args&& = int& && = int&（左值引用）
// make_unique<string>(42)   → Args = {int},    Args&& = int&&（右值引用）
```

**引用折叠规则**（只有四种组合）：

| 外层 | 内层 | 结果 |
|---|---|---|
| `T&` | `&` | `T&` |
| `T&` | `&&` | `T&` |
| `T&&` | `&` | `T&` |
| `T&&` | `&&` | `T&&` |

唯一产生右值引用的组合是 `T&& &&` ——两个右值引用叠在一起。其他三种组合全部折叠为左值引用。

`std::forward` 利用引用折叠实现"按原样转发"——左值参数 `forward` 返回左值引用（拷贝），右值参数 `forward` 返回右值引用（移动）。这就是 Ch03 移动语义在变参场景下的延伸——变参模板让你一次转发所有参数，而不是逐个写。

### 3.4 `...` 的两种展开模式

变参模板的展开有两种位置——视觉效果完全不同但本质一样：

```cpp
// 模式 1：包展开在函数调用里——对每个参数独立调用 forward
// std::forward<Args>(args)... → forward<A1>(a1), forward<A2>(a2), forward<A3>(a3)

// 模式 2：包展开在模板参数列表里——对每个类型独立做某件事
// print(rest...) → print(r1, r2, r3)——直接展开参数
```

关键——**`...` 总能找到它作用于哪个"包"**。`forward<Args>(args)...` 作用于 `args` 包，同时 `Args` 包跟着一起展开（两个包长度相等）。`print(rest...)` 作用于 `rest` 包，等价于逐个枚举包中元素。

---

## 4. SFINAE——编译时说"这个不行，换一个"

### 4.1 什么是 SFINAE

**S**ubstitution **F**ailure **I**s **N**ot **A**n **E**rror——替换失败不是错误。

当编译器尝试实例化一个函数模板时，可能有多个候选版本。如果某个候选版本在替换模板参数时失败了（比如访问了一个不存在的类型），编译器**不报错——而是静默排除此版本，继续尝试下个版本**。只有一个版本也失败时才报错。

把这个过程理解为"编译器在试钥匙"：

```
编译器看到调用 type_name(42):
  试版本 A → T=int → enable_if<is_integral<int>> → ::type 存在 → ✓ 替换成功，选中版本 A
  版本 B 不用试了

编译器看到调用 type_name(3.14):
  试版本 A → T=double → enable_if<is_integral<double>> → ::type 不存在 → 替换失败，排除版本 A
  试版本 B → T=double → enable_if<not is_integral<double>> → ::type 存在 → ✓ 选中版本 B
```

这不是运行时 if-else——编译完成后二进制里**只有一个版本**的代码。

### 4.2 `enable_if`——SFINAE 的实现工具

```cpp
#include <type_traits>

// 版本 A：只对整数类型有效
template <typename T>
typename std::enable_if<std::is_integral<T>::value, const char*>::type
type_name(T) { return "integer"; }

// 版本 B：对其他类型有效——!value 保证互斥
template <typename T>
typename std::enable_if<!std::is_integral<T>::value, const char*>::type
type_name(T) { return "other"; }

std::cout << type_name(42) << '\n';     // "integer"
std::cout << type_name(3.14) << '\n';   // "other"
```

**`enable_if` 的工作原理**：

```cpp
std::enable_if<true,  T>::type  →  T        // 条件为 true → ::type 存在，等于 T
std::enable_if<false, T>::type  →  不存在   // 条件为 false → ::type 不存在 → SFINAE 排除
```

版本 A 的返回值是 `enable_if<is_integral<T>, const char*>::type`：
- T = int → `is_integral<int>::value = true` → `enable_if<true, const char*>::type = const char*` → 返回 `const char*` ✓
- T = double → `is_integral<double>::value = false` → `enable_if<false, const char*>::type` 不存在 → 替换失败，排除版本 A

版本 B 反过来——当 T 不是整数时 `!value = true`，所以恰好和版本 A 互补。

### 4.3 预定义的类型萃取——你的"类型检测工具箱"

`<type_traits>` 提供了几百个预定义的类型检测——你不需要自己写：

```cpp
std::is_integral<T>        // T 是整数类型？（int, long, char, bool...）
std::is_floating_point<T>  // T 是浮点类型？（float, double, long double）
std::is_arithmetic<T>      // T 是数字类型？（整数或浮点）
std::is_pointer<T>         // T 是指针？
std::is_class<T>            // T 是类/结构体？
std::is_same<T, U>         // T 和 U 是同一个类型？
std::is_base_of<Base, T>   // Base 是 T 的基类？
std::is_convertible<From, To>  // From 能隐式转换为 To？
```

每个都有 `::value`（编译时 bool 常量）。C++14 起有 `_v` 后缀简化写法（`is_integral_v<T>`），但 C++11 需要用 `::value`。

### 4.4 `enable_if` 放在不同位置

`enable_if` 可以放在返回值、模板参数、函数参数三种位置——各有取舍：

```cpp
// 位置 1：返回值（最常用）——简洁
template <typename T>
typename std::enable_if<std::is_integral<T>::value, T>::type
square(T x) { return x * x; }

// 位置 2：模板参数（C++11）——对构造函数有效
template <typename T, typename = typename std::enable_if<std::is_integral<T>::value>::type>
T square(T x) { return x * x; }
// 注意：默认值 typename = ...——第二个模板参数有默认值，调用时不需要显式传

// 位置 3：函数参数（C++11）——最不影响签名
template <typename T>
T square(T x, typename std::enable_if<std::is_integral<T>::value>::type* = nullptr) {
    return x * x;
}
```

| 位置 | 优点 | 缺点 |
|---|---|---|
| 返回值 | 简洁、不影响参数列表 | 返回类型变复杂；构造函数/析构函数不能用（无返回值） |
| 模板参数 | 对构造函数/析构函数有效 | 占用模板参数位置；多个 `enable_if` 容易冲突（编译器视为重复默认参数） |
| 函数参数 | 最不影响类型签名 | 难看——额外的虚设参数 `= nullptr` |

### 4.5 SFINAE 和 `#ifdef` 的本质区别

| 维度 | `#ifdef`（C 预处理器） | SFINAE（C++ 模板） |
|---|---|---|
| 作用阶段 | 预处理器——文本替换 | 编译时——模板实例化 |
| 能获取类型信息 | 不能——不知道 `T` 是什么 | 能——`is_integral<T>` 知道 T 的属性 |
| 排除方式 | 删代码——注释掉 | 替换失败——静默跳过候选版本 |
| 多版本共存 | 不同宏值 → 不同代码 | 编译器逐个尝试 → 选第一个替换成功的 |

`#ifdef` 只能问"这个宏定义了吗"，SFINAE 能问"T 是整数吗？T 有 `insert` 方法吗？T 的 `value_type` 是 `int` 吗？"——完全是不同层次的能力。

---

## 5. `enable_if` 的实用场景

### 5.1 限制模板只接受数字类型

```cpp
template <typename T>
typename std::enable_if<std::is_arithmetic<T>::value, T>::type
square(T x) { return x * x; }

auto a = square(5);        // ✓ T=int, is_arithmetic=true → 编译通过
auto b = square(3.14);     // ✓ T=double, is_arithmetic=true → 编译通过
// auto c = square("hello");  // ✗ T=const char*, is_arithmetic=false → SFINAE 排除 → 无匹配模板 → 编译错误
```

编译错误信息会非常长（模板错误的特点），但至少**编译时捕获**——比运行时出错强。

### 5.2 用 `decltype` 检测成员函数是否存在

这是一个更实用的场景——"只对支持这个操作的容器才让模板编译通过"：

```cpp
#include <vector>
#include <list>
#include <iostream>

// 只对支持 front() 的容器编译——用 decltype + SFINAE 检测
template <typename Container>
auto print_first(const Container& c)
    -> decltype(c.front(), void()) {
    std::cout << "first: " << c.front() << '\n';
}

std::vector<int> v = {1, 2, 3};
print_first(v);   // ✓ vector 有 front() → 编译通过
// struct NoFront { void back(); };
// print_first(NoFront{});  // ✗ NoFront 没有 front() → SFINAE 排除 → 编译错误
```

**`decltype(c.front(), void())` 的语法分解**：

- 逗号表达式 `c.front(), void()` ——先执行左边（检测 front() 是否合法），再求值右边（得到 void 类型）
- `decltype(表达式)` ——获取表达式的类型 → 返回类型声明为它
- 如果 `c.front()` 不合法（容器没这个方法）→ 整个 `decltype` 替换失败 → SFINAE 排除此模板
- 返回类型后置 `auto function(...) -> type` ——C++11 写法，等同于 `type function(...)`

这种模式叫 **expression SFINAE**——用任意表达式触发 SFINAE，不限于 `enable_if`。`decltype` 在这里是检测"这个表达式能不能编译"的手段。

**常见扩展**——给同名字写多个重载，各检测不同的成员函数，让编译器按容器能力自动选择：

```cpp
// 版本 A：有 push_front 的容器
template <typename Container>
auto add_front(Container& c, typename Container::value_type val)
    -> decltype(c.push_front(val), void()) {
    c.push_front(val);
}

// 版本 B：只有 push_back 的容器——兜底
template <typename Container>
auto add_front(Container& c, typename Container::value_type val)
    -> decltype(c.push_back(val), void()) {
    c.push_back(val);
}
```

```cpp
std::list<int> lst;
add_front(lst, 42);   // list 有 push_front → 选版本 A
```

> **注意**：如果容器同时有 push_front 和 push_back（如 `deque`），两个版本都生效 → 签名都是 `void(Container&, value_type)` → 编译器报重复定义。这是 expression SFINAE 的经典陷阱——多版本重载必须互斥。

### 5.3 `enable_if` 的常见陷阱

**陷阱 1——忘记提供兜底版本**：

```cpp
// 只有两个 enable_if 版本——互相排斥但没覆盖所有情况
template <typename T>
typename std::enable_if<std::is_integral<T>::value, void>::type f(T);     // 整数

template <typename T>
typename std::enable_if<std::is_floating_point<T>::value, void>::type f(T); // 浮点

f("hello");  // ✗——既不是整数也不是浮点——两个版本都被排除→"no matching function"错误
```

正确做法：要么加兜底版本（没有 enable_if 的通用版本），要么加 `static_assert` 给更好的错误信息：

```cpp
template <typename T, typename = void>
void f(T) {
    static_assert(sizeof(T) == 0, "f() only supports integral or floating-point types");
}
```

**陷阱 2——多个模板参数 `enable_if` 冲突**：

```cpp
// 🔴 错误——编译器视为重复定义（默认参数不算函数签名的一部分）
template <typename T, typename = typename std::enable_if<std::is_integral<T>::value>::type>
void g(T);  // 参数为整数时，第二个模板参数是 void

template <typename T, typename = typename std::enable_if<std::is_floating_point<T>::value>::type>
void g(T);  // 参数为浮点时，第二个模板参数也是 void——和上面一模一样！
```

两个都是 `template<typename T, typename = 某个类型>`——即使默认值不同，函数签名（不含默认值）是一样的。解决——把 `enable_if` 放到返回值或函数参数位置。

---

## 6. 变参模板 + SFINAE 的组合

两个武器合在一起——实现"接受任意数量、但有限定类型的参数"：

```cpp
#include <type_traits>

// C++11 版本——递归逐参数检查 + 递归逐参数求和
template <typename T>
typename std::enable_if<std::is_arithmetic<T>::value, T>::type
sum(T first) { return first; }           // 终止条件——只有一个参数

template <typename T, typename... Args>
typename std::enable_if<std::is_arithmetic<T>::value, T>::type
sum(T first, Args... rest) {             // 递归——每次检查并处理第一个参数
    return first + sum(rest...);
}

std::cout << sum(1, 2, 3, 4) << '\n';     // 10
std::cout << sum(1.5, 2.5, 3.0) << '\n';  // 7.0
// sum("hello", 1);  // ✗ 编译错误——const char* 不是 arithmetic
```

> **注意**：不能写成 `std::is_arithmetic<Args...>::value` ——`is_arithmetic` 只接受**一个**类型参数，不能直接传参数包。C++17 起可以用折叠表达式 `(std::is_arithmetic_v<Args> && ...)` 一次性检查所有参数，但 C++11 只能用递归逐参数检查。

---

## 7. 可运行错误实验

### 实验：SFINAE 的"安静失败"——亲手触发并理解编译器怎么选

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

// 版本 3：兜底版本——没有 enable_if，始终有效
template <typename T>
void print_kind(T) { std::cout << "other\n"; }

int main() {
    print_kind(42);      // integer——版本1替换成功，版本2失败被排除
    print_kind(3.14);    // floating point——版本1失败被排除，版本2成功
    print_kind("hello"); // other——版本1和2都失败被排除，版本3兜底
    return 0;
}
```

**编译运行**：`g++ -std=c++11 sfinae.cpp -o sfinae && ./sfinae`
**输出**：
```
integer
floating point
other
```

**核心体验**：三个版本都没有 `if`/`#ifdef`——编译器对每个调用点逐个尝试模板版本，不合适的静默跳过。这就是 SFINAE 的意义——用类型系统做编译时分派。

---
<!-- Stage2 结束 — Stage3 开始: 对象模型与运行时多态 -->
## 8. 面试题

### 面试题 1：变参模板的工作机制

**面试官**：变参模板 `template <typename... Args> void f(Args... args)` 是怎么工作的？`...` 在三个位置各是什么意思？

**回答**：(1) `typename... Args`——模板参数包，表示"零或多个类型参数"；(2) `Args... args`——函数参数包，表示"零或多个值"；(3) `f(args...)`——包展开，把 args 包里的每个元素展开成独立参数。实际工作——变参模板每次递归剥离第一个参数：`f(a, b, c)` → 处理 `a`，递归调 `f(b, c)` → 处理 `b`，递归调 `f(c)` → 处理 `c`，递归调 `f()` → 匹配空参数版本 → 终止。

**追问（面试官）**：`sizeof...(Args)` 返回什么？变参模板编译后会不会代码膨胀？

**追问回答**：`sizeof...(Args)` 返回参数包中参数的数量——编译时常量，在模板实例化时求值。代码膨胀——每个不同参数组合生成一份独立模板实例——`f(int, double, char)` 和 `f(string, int)` 是两份不同代码。但和不提供变参模板相比（需要写 20 个不同参数数量的重载版本），变参模板的"膨胀"反而更少——一份模板覆盖所有参数数量组合，而且只为你实际用到的组合生成代码。

### 面试题 2：SFINAE 和 `enable_if`

**面试官**：什么是 SFINAE？用 `enable_if` 给一个实际例子。

**回答**：Substitution Failure Is Not An Error——模板实例化时，如果某个候选版本替换失败，编译器排除它而不报错，继续尝试下一个版本。`enable_if<条件, 类型>` 是实现——条件为 true → `::type` 存在（模板生效），条件为 false → `::type` 不存在（SFINAE 排除）。场景——`template<typename T> enable_if<is_integral<T>, T> square(T x)` ——只对整数类型有效，对 `double`/`string` 编译错误（因为没有其他有效重载被选中）。

**追问（面试官）**：`enable_if` 放在返回值里和放在模板参数里有什么区别？

**追问回答**：返回值里——`typename enable_if<..., T>::type` ——简洁但返回类型变复杂，且对构造函数/析构函数无效（无返回值）。模板参数里——`template <typename T, typename = enable_if<...>>` ——对构造函数有效，但占用模板参数位置，且多个 `enable_if` 在同一函数签名的模板参数位置容易冲突（编译器视为重复默认参数）。C++14 加了 `enable_if_t` 简化写法：`enable_if_t<条件, 类型>` 代替 `typename enable_if<条件, 类型>::type`。

### 面试题 3：变参 + SFINAE 的组合

**面试官**：`make_unique` 怎么用变参模板实现？`std::forward` 在里面起什么作用？

**回答**：`template<typename T, typename... Args> unique_ptr<T> make_unique(Args&&... args)` ——`Args&&...` 接受任意数量、任意类型的参数。`return unique_ptr<T>(new T(std::forward<Args>(args)...))` ——把参数完美转发给 `T` 的构造函数。关键——`std::forward` 保留参数的左值/右值属性（左值候选拷贝构造、右值候选移动构造），`...` 同时对包中每个参数独立调用 `forward`。引用折叠规则保证了转发过程中不丢失值类别信息。

**追问（面试官）**：如果 `T` 没有接受这些参数的构造函数会怎样？

**追问回答**：编译错误——在 `make_unique` 实例化时，`new T(args...)` 会触发构造函数查找。如果找不到匹配的构造函数 → 编译报错，错误信息指向 `new T(...)` 的位置。这比 C 的 `void*` + 运行时类型判断安全——C++ 在编译时就知道构造不合法，不会留下运行时炸弹。

---

## 9. Stage2 回顾 + Stage3 预告

### 9.1 本章小结

| 概念 | 核心理解 |
|---|---|
| 变参模板 | 编译时递归展开——每次剥离一个参数，最终匹配空参数版终止 |
| 参数包 | `typename... Args`（类型包）+ `Args... args`（值包）+ `args...`（展开） |
| 完美转发 | `Args&&` + `std::forward<Args>` 保留左值/右值属性——引用折叠是关键 |
| SFINAE | 替换失败不是错误——编译器逐个试候选版本，失败的跳过 |
| `enable_if` | SFINAE 的实现工具——条件 true→`::type` 存在，false→`::type` 不存在 |
| `decltype` | 检测表达式合法性——配合返回类型后置做 expression SFINAE |

### 9.2 Stage2 四章回顾

```
Ch01 模板基础     → 你学会写"一份代码，多个类型"——函数模板、类模板、特化
Ch02 STL 容器     → 你学会用 vector/map/string，不再手写 C 的 malloc+链表
Ch03 STL 算法     → 你学会用 sort/find/count_if 代替手写 for 循环
Ch04 变参与SFINAE  → 你学会模板的高阶用法——能读懂 STL 源码里的 enable_if 和变参
```

**Stage2 的核心主线**：模板是"编译时多态"——所有的选择、展开、分派都在编译时完成，运行时零开销。你写的 `vector<int>` 和手写的 `int[]` 一样快——因为编译器已经替你写好了那份专用代码。

### 9.3 Stage3 预告

Stage2 把"编译时"的多态讲完了。Stage3 转向"运行时"的多态——对象模型与继承：

- **虚函数**：一个基类指针指向子类对象——调哪个函数运行时才决定
- **继承层次**：`public`/`protected`/`private` 继承的真实语义
- **异常安全**：构造函数抛异常后析构函数还会调吗？RAII 怎么保你不死
- **C++ 对象模型**：`sizeof` 一个带虚函数的类到底多大——虚表指针在哪儿

模板和继承是 C++ 两大支柱——一个管编译时，一个管运行时。你学完了编译时这一半，下一半是如何让代码在运行时保持正确。本书后续将逐步深入这些领域，目标是让你在**没有运行时开销的前提下**写出类型安全且高性能的代码。
