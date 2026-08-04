# Stage2 Ch01 规划：模板基础

> **定位**：Stage2 第一章。前提——读者已完成 Stage1（类/RAII/移动语义/智能指针），能写出带有构造函数、拷贝控制、移动语义的 C++ 类。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §22-23，深入理解C++11 §7

---

## 1. 本章要解决的核心问题

Stage1 教了你写类——`Buffer`、`File`、`Foo`。但如果你的 `Stack` 需要同时支持 `int`、`string`、`Buffer`，你要写三遍几乎一样的代码？每个类型一份拷贝——加一个字段就意味着改三份。

C 里的泛型手段是 `void*` + 函数指针（或宏），有类型不安全 + 运行时开销的代价。C++ 模板给了第三种方案：**编译器替你生成代码——零运行时开销、完全类型安全。**

本章教模板的"基础层"：函数模板、类模板、特化。变参模板和 SFINAE 留给 Ch04。

---

## 2. 节结构（6 节）

### §1 为什么需要模板——从"写三遍"到"写一遍"

从 C 程序员的痛点出发——展示没有泛型的代价：

```cpp
// 你需要一个"取较大值"的函数——
int    max_int(int a, int b)    { return a > b ? a : b; }
double max_double(double a, double b) { return a > b ? a : b; }
// 如果还有 long、float、string...你要写多少个？
// 并且：函数体完全一样——只是类型不同
```

- **C 的解决方案**：`void*` + 比较函数指针 → `qsort` 的模式——类型不安全（`void*` 没有类型信息）、有函数调用开销
- **C 的宏方案**：`#define MAX(a,b) ((a)>(b)?(a):(b))` → 无类型检查、有副作用
- **C++ 的模板方案**：编译器在你使用模板时针对具体类型自动生成一份代码——你说"我想要 max<int>"，编译器给你生成一份 `int max(int a, int b)`。

### §2 函数模板——语法与使用

```cpp
// 模板的定义——template <typename T> 告诉编译器"T 是类型占位符"
template <typename T>
T max(T a, T b) {
    return a > b ? a : b;    // 只要 T 支持 > 运算符就能用
}

// 使用——编译器从参数自动推导 T
int x = max(3, 5);           // T = int，编译器生成 max<int>(int, int)
double y = max(3.14, 2.71);  // T = double
std::string s = max(std::string("abc"), std::string("xyz"));  // T = string
```

- **`typename` vs `class`**：`template <typename T>` 和 `template <class T>` 完全一样——C++ 世界没有偏好，本教材统一用 `typename`（表达更准确：T 不一定是 class，可以是 int/double 等基本类型）。
- **模板函数不是函数**：`max` 是一个**模板**（蓝图），`max<int>` 才是真正的**函数**（编译器生成的）。程序里没有 `max`——只有你调用后编译器生成的 `max<int>`、`max<double>` 等。

### §3 类模板——泛型容器

```cpp
// 定义一个泛型栈——元素类型是 T，栈大小是 N
template <typename T, int N>
class Stack {
    T data[N];       // 用模板参数 N 指定数组大小
    int top;
public:
    Stack() : top(0) {}
    void push(const T& val) { data[top++] = val; }
    T pop() { return data[--top]; }
    bool empty() const { return top == 0; }
};

// 使用——每种类型组合生成一份独立的类
Stack<int, 10>    int_stack;     // 元素是 int，大小 10
Stack<std::string, 100> str_stack;  // 元素是 string，大小 100
// 编译器生成两份完全独立的代码——int_stack 和 str_stack 的 push/pop 没有任何关系
```

- **模板参数可以是类型（`typename T`），也可以是值（`int N`）**——后者叫"非类型模板参数"
- **非类型模板参数能传什么**：整数（`int`/`unsigned`/`size_t`）、枚举、指针、引用——但不能是浮点数（`double`）、字符串（`const char*`）、自定义类对象。C++11 放宽了部分限制（允许 `long long` 等），但核心规则不变。
- **和 C 的对比**：C 里你要么写 `IntStack`、`StringStack` 两份代码，要么用 `void*` + 运行时强制类型转换——都会出错
- **每种实例化是独立编译的**：`Stack<int,10>` 和 `Stack<double,10>` 在生成的机器码里是两套 `push`/`pop` 函数——零运行时开销（和手写两份代码一样快）

### §4 模板参数推导与显式指定

编译器通常能从参数推导出模板类型——但有些情况需要你显式指定：

```cpp
// 场景 1：返回类型和参数类型不同——推导不了
template <typename To, typename From>
To cast(From val) { return static_cast<To>(val); }

// auto x = cast(3.14);       // ✗ 编译错误——编译器不知道 To 是什么
auto x = cast<int>(3.14);     // ✓ 显式指定 To = int，From 从 3.14 推导 = double

// 场景 2：没有参数可以推导
template <typename T>
T create() { return T(); }

// auto x = create();          // ✗ 没有参数，推导不了 T
auto x = create<int>();       // ✓ 显式指定
```

- **核心理解**：编译器从函数参数推导模板类型——如果推导不了，你需要显式写 `<int>` 告诉它
- **和 `auto` 的关系**：`auto x = max(3, 5);` 里 `auto` 推导出 `int`——模板从 `(3, 5)` 推导出 `T = int`——两个推导机制互相配合

### §5 模板特化——"对某个类型做一些不一样的事"

大部分类型用通用模板就够了。但有时需要给特定类型开"后门"——这叫**特化**：

```cpp
// 通用版本——对大多数类型
template <typename T>
const char* type_name() { return "unknown"; }

// 特化——对 int
template <>
const char* type_name<int>() { return "int"; }

// 特化——对 double
template <>
const char* type_name<double>() { return "double"; }

// 使用
std::cout << type_name<int>() << "\n";     // "int"
std::cout << type_name<double>() << "\n";  // "double"
std::cout << type_name<long>() << "\n";    // "unknown" ——没特化的走通用版本
```

- **全特化 vs 偏特化**：本节只讲全特化（`template <>` 全部参数固定）。偏特化（`template <typename U> class Foo<U, int>` 部分参数固定）留给 Ch04。
- **特化的典型用途**：`std::vector<bool>` 是位压缩特化（省内存）、`std::hash<T>` 给各种类型特化（提供哈希值）

### §6 模板代码必须放在头文件中

这是 C++ 模板最常见的编译/链接陷阱——你写好 `.h`（声明）和 `.cpp`（实现），分文件编译：

```cpp
// stack.h —— 声明
template <typename T> class Stack {
    void push(const T& val);  // 只有声明
};

// stack.cpp —— 实现
#include "stack.h"
template <typename T>
void Stack<T>::push(const T& val) { data[top++] = val; }

// main.cpp —— 使用
#include "stack.h"
Stack<int> s;  // ✗ 链接错误！编译器生成 Stack<int>::push 时找不到定义
s.push(42);
```

- **为什么**：模板是"蓝图"——编译器看到 `Stack<int>` 时才生成代码。如果 `push` 的定义在 `stack.cpp` 里，`main.cpp` 编译时看不到定义→生成不了代码→链接错误。
- **解决办法**：把模板的全部定义（不只是声明）放在头文件中。
- **思考**：这跟 C 的 `static inline` 函数放头文件的原理类似——每个 TU 生成自己的副本，链接器合并。

---

## 3. 编写方针

1. **每节从"写三遍"的痛点出发**——模板不是语法糖，是消除重复代码的机制
2. **模板不是"高级特性"——是基础**：读完 Stage1 第一个真正遇到的 C++ 高级抽象就是模板。写法可以简单，但概念要讲透。
3. **§6 头文件陷阱是关键**——几乎每个 C++ 初学者都会踩，必须有可运行的"编译失败的代码"和"修复好的代码"
4. 本章不讲的内容：变参模板（Ch04）、SFINAE/enable_if（Ch04）、Concepts（C++20）、模板元编程（Ch04）。

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：模板的编译模型

**面试官**：模板函数和普通函数在编译时有什么区别？为什么模板代码必须放头文件？

**回答**：普通函数——编译器直接生成机器码，一份 `.o` 文件包含一份实现。模板函数——编译器**不生成任何代码**直到你用到它。`max<int>` 被调用时，编译器看到 `.cpp` 里的调用点→根据 `.h` 里的模板定义→在调用点生成一份 `int max(int, int)` 的代码。如果定义在 `.cpp` 里，另一个 `.cpp` 编译时看不到定义→生成不了代码→链接错误。所以模板定义必须放头文件——让每个 TU 都能看到。

**追问（面试官）**：那模板会增大二进制体积吗？每个类型都生成一份会不会代码膨胀？

**追问回答**：会——`max<int>`、`max<double>`、`max<string>` 各生成一份独立代码。但这是"按需生成"而非"全量生成"——你只为你用到的类型付出代价。对比 C 的 `void*`（无类型安全+函数指针开销）和 Java 的泛型（类型擦除+装箱开销），C++ 模板的"代码膨胀"是**用二进制体积换运行时零开销**——和手写三份 `max_int`/`max_double`/`max_string` 体积一样大、性能一样快。

### 面试题 2：类模板的使用

**面试官**：写一个 `template <typename T, int N> class Array`——元素类型是 T，大小是 N。`N` 是什么？编译时常量还是运行时变量？

**回答**：`N` 是非类型模板参数——必须是**编译时常量**。`Array<int, 10>` 合法（10 是常量），`Array<int, n>` 如果 `n` 是 `constexpr` 也合法，`Array<int, n>` 如果 `n` 是运行时变量则非法。因为编译器在编译时就要知道数组大小——生成 `data[N]` 的内存布局。类比：这跟 C99 VLA 不同——VLA 是运行时栈分配，模板是在编译时把大小写死在类型里。

**追问（面试官）**：`Array<int, 10>` 和 `Array<int, 100>` 是同一个类型吗？

**追问回答**：不是——`Array<int, 10>` 和 `Array<int, 100>` 是**完全不同的类型**。不同模板参数生成不同的类——它们的 `push`/`pop` 是不同的函数，对象大小不同（`sizeof` 差 90 个 int）。你不能把 `Array<int, 10>` 赋给 `Array<int, 100>` 的变量——就像 `int` 不能赋给 `double`。

### 面试题 3：模板特化

**面试官**：模板特化是什么？给一个实际场景。

**回答**：对特定类型（或特定参数组合）提供不同的模板实现。场景——`type_name<T>()` 对大多数类型返回 "unknown"，对 `int` 返回 "int"。另一个经典例子——`std::hash<T>` 对 `int`/`string`/`double` 各有特化返回哈希值。特化的理念是你先用通用模板覆盖 90% 的情况，再用特化覆盖剩下的 10%。

**追问（面试官）**：全特化和偏特化的区别？`std::vector<bool>` 是哪种？

**追问回答**：全特化——所有模板参数都固定（`template <> class Foo<int>`）。偏特化——部分参数固定（`template <typename U> class Pair<U, int>` 第二个参数固定为 int，第一个仍泛型）。`std::vector<bool>` 是全特化——只有一个模板参数 `T`，全部固定为 `bool`。每个 `bool` 只占 1 个 bit（不是 1 个 byte）——这是"为了省内存牺牲了 `&v[0]` 返回 `bool*` 的语义"的著名设计争议。

---

## 5. 可运行错误实验

### 实验：模板分离编译导致的链接错误

```cpp
// max.h —— 只有声明（函数模板声明和普通函数声明写法一样）
template <typename T>
T max(T a, T b);

// max.cpp —— 实现在这里
#include "max.h"
template <typename T>
T max(T a, T b) { return a > b ? a : b; }

// main.cpp —— 使用
#include "max.h"
int main() {
    int x = max(3, 5);           // ✗ 链接错误！编译器找不到 max<int> 的定义
    auto y = max(3.14, 2.71);    // ✗ 同样——找不到 max<double>
    return 0;
}
```

**运行**：`g++ -std=c++11 max.cpp main.cpp -o max_test` → `undefined reference to max<int>` 和 `undefined reference to max<double>`。
**修复**：把 `max.cpp` 的所有代码移到 `max.h` 里——main.cpp 编译时就能看到模板定义，编译器直接生成代码。`g++ -std=c++11 main.cpp -o max_test` → 成功。

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | 模板的"蓝图→代码"编译模型讲透；非类型参数必须是编译时常量的底层原因（内存布局在编译时确定）；特化的"override"机制与虚函数的本质区别（编译时 vs 运行时） | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（编译模型+代码膨胀 / 类模板+类型独立性 / 特化+vector<bool>），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | 头文件分离编译链接错误的可运行实验；非类型参数不能用变量（编译错误而非运行时 bug）；模板编译错误信息难以阅读的说明 | 8/10 |
| 交叉引用 | §1 从 C 的 void*/宏方案出发→与 C 学习路线衔接；§6 头文件陷阱 → C 的 static inline 头文件模式类比；§3 Stack → Ch02 STL 容器（std::stack）；§5 全特化 → Ch04 偏特化 | 8/10 |
| **总分** | | **33/40** |
