# Stage2 Ch03 规划：STL 算法与函数对象

> **定位**：Stage2 第三章。前提——读者已完成 Ch02（容器/迭代器），能用 `begin()`/`end()` 遍历 `vector`/`map`。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §32-34，深入理解C++11 §6

---

## 1. 本章要解决的核心问题

Ch02 教了容器和迭代器——你有了装数据的"盒子"和"遍历盒子的指针"。但遍历之后呢？你需要**排序、查找、过滤、变换**。C 程序员习惯手写 for 循环：`for (int i = 0; i < n; i++) { if (arr[i] > threshold) { ... } }`。

STL 算法给了另一种思路：**你只写"做什么"，不写"怎么做"**。`sort(v.begin(), v.end())` 比你自己写快排更准确（实现经过了数十年的优化）。`find_if` 比你自己写 for+break 更清晰（意图一眼可见）。

本章的核心信息：**停止手写 for 循环——让算法表达你的意图。**

---

## 2. 节结构（5 节）

### §1 从手写 for 到算法调用——思维转换

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `for (int i = 0; i < n; i++) { if (arr[i] > 10) count++; }` | `int n = std::count_if(v.begin(), v.end(), [](int x){ return x > 10; });` |
| 手写快排几十行 | `std::sort(v.begin(), v.end());` |
| 手写二分查找 | `std::binary_search(v.begin(), v.end(), 42);` |

- **算法的基本格式**：`std::算法名(开始迭代器, 结束迭代器, ...额外参数)`
- **为什么不用手写**：(1) 算法比手写快（sort 用 intro sort 混合策略——快排+堆排+插入排序）；(2) 算法意图明确——读代码的人一眼看出"这是在排序"而不是"这是在嵌套 for 循环"；(3) 算法不会越界——迭代器半开区间 `[begin, end)` 防止 off-by-one 错误。
- **`#include <algorithm>`**——几乎所有算法都在这个头文件里。

### §2 核心算法速览——5 个最常用的

不是 API 手册——只讲生产代码中出现频率最高的 5 个算法：

**`std::sort`——排序**：
```cpp
std::vector<int> v = {3, 1, 4, 1, 5, 9, 2, 6};
std::sort(v.begin(), v.end());           // 升序 → 1 1 2 3 4 5 6 9
std::sort(v.begin(), v.end(), std::greater<int>()); // 降序
// 自定义排序——按绝对值
std::sort(v.begin(), v.end(), [](int a, int b) { return abs(a) < abs(b); });
```
- O(n log n)，intro sort（三种排序算法的混合），要求随机访问迭代器（`vector`/`deque`/`array` 可以用，`list` 不能用——`list` 有自己的 `sort()` 成员函数）。
- 这个限制不是"bug"——`sort` 需要 O(1) 随机交换元素，链表做不到。

**`std::find` / `std::find_if`——查找**：
```cpp
auto it = std::find(v.begin(), v.end(), 42);  // 找 42，返回迭代器
if (it != v.end()) { /* 找到了 */ }

// find_if——按条件找
auto it = std::find_if(v.begin(), v.end(), [](int x) { return x > 100; });
```

**`std::count` / `std::count_if`——计数**：
```cpp
int n = std::count_if(v.begin(), v.end(), [](int x) { return x % 2 == 0; });
// n = 偶数个数
```

**`std::transform`——变换（映射）**：
```cpp
std::vector<int> doubled(v.size());
std::transform(v.begin(), v.end(), doubled.begin(),
               [](int x) { return x * 2; });  // 每个元素翻倍
// v = {3,1,4} → doubled = {6,2,8}
```

**`std::copy_if`——过滤**：
```cpp
std::vector<int> evens;
std::copy_if(v.begin(), v.end(), std::back_inserter(evens),
             [](int x) { return x % 2 == 0; });
// 只拷贝偶数到 evens——back_inserter 自动调用 push_back
```

### §3 Lambda 表达式——让算法活起来的"即用即扔"函数

上面的代码里出现了 `[](int x) { return x > 100; }`——这是 Lambda 表达式。它是 C++11 最重要的特性之一——让 STL 算法从"有用"变成"好用"：

```cpp
// Lambda 语法：[捕获](参数) { 函数体 }
// 例子分解：
[](int x) { return x > 100; }
//  ↑        ↑                    ↑
//  捕获列表  参数列表              函数体

// 捕获——把外部的变量"带进"Lambda
int threshold = 100;
auto it = std::find_if(v.begin(), v.end(),
    [threshold](int x) { return x > threshold; }); // 捕获 threshold 的值
```

- **`[=]` vs `[&]`**：`[=]` 按值捕获——Lambda 闭包对象内部有成员变量，初始化时**调用被捕获变量的拷贝构造函数**。这就是为什么 `[=]` 对 `unique_ptr` 不能用（`unique_ptr` 没有拷贝构造）——和 Ch04 的智能指针呼应。`[&]` 按引用捕获——内部存的是指针/引用。C++ 世界推荐**显式写捕获变量名**——`[threshold]` 而不是 `[=]`——清晰且防 bug。
- **Lambda 和函数指针的关系**：不捕获任何变量的 Lambda（`[]`）可以隐式转为函数指针——和 C 的函数指针兼容。
- **和函数重载的对比**：不用 Lambda 的话——`find_if` 需要你写一个命名函数或函数对象类（多几行）传给算法。Lambda 把"做什么"直接写在调用点——可读性最大。

### §4 `std::function` 与 `std::bind`——把"函数"当值传递

Lambda 的类型是编译器生成的匿名类型——不能直接存到变量里（只能用 `auto`）。`std::function` 给了 Lambda 一个"名字"：

```cpp
#include <functional>

// std::function——存储任何可调用的东西
std::function<int(int, int)> op;
op = [](int a, int b) { return a + b; };
int result = op(3, 5);  // 8

op = [](int a, int b) { return a * b; };
result = op(3, 5);  // 15 ——同一个 function 变量，不同行为
```

- **`std::function` 的使用场景**：回调注册——把函数存在 `map<string, function<void()>>` 里，按名字调用；策略模式——运行时切换不同算法。
- **`std::bind`**（C++11 但 C++ 世界逐渐偏向 Lambda）：`auto f = std::bind(add, std::placeholders::_1, 10);` ——绑定了第二个参数为 10。大多数场景 Lambda 比 `bind` 更清晰，所以本节只做"见过"级别的介绍。

### §5 range-for 与算法——什么时候用什么

C++11 的 range-for（`for (int x : v)`）看起来和算法很像——它们都是"对容器每个元素做某件事"：

```cpp
// range-for——适合"做某件事"
for (auto& x : v) { x *= 2; }             // 每个元素翻倍

// 算法——适合"求某件事的结果"
auto it = std::find(v.begin(), v.end(), 42);  // 找到 42
int n   = std::count_if(v.begin(), v.end(), is_even); // 数偶数
```

| 场景 | 用 | 因为 |
|---|---|---|
| 对每个元素做一件事（有副作用） | range-for | 简单直白 |
| 查找/计数/排序/变换（无副作用的目标） | 算法 | 意图明确、已优化、防 bug |
| 需要同时处理多个容器 | 算法 | `transform` 同时读 A 写 B |

- **核心观点**：算法表达"你想得到什么结果"——`count_if` 就是"数一下符合条件的"。range-for 表达"你想对每个元素做什么过程"——比算法更灵活但意图不够明确。
- 本章不讲的内容：`<numeric>` 的 `accumulate`/`iota`（参考手册即可）、并行算法（C++17）、ranges（C++20）。

---

## 3. 编写方针

1. **不是算法大全**——只讲 5 个核心算法（sort/find/count/transform/copy_if），引导读者学会"遇到新算法自查 cppreference"的习惯
2. **Lambda 是本章的灵魂**——没有 Lambda，STL 算法用起来很痛苦（得写函数对象类）。每节代码都展示 Lambda 用法
3. **从手写 for 循环开始**——对比 C 里的 for+break 和 C++ 的 find/count_if——让读者感受"不用自己写循环"的自由
4. 本章不讲的内容：迭代器适配器（`back_inserter` 只提一次）、`<numeric>` 全部、并行算法

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：算法 vs 手写循环

**面试官**：`std::find` 和手写 for 循环有什么区别？为什么应该用前者？

**回答**：(1) 意图——`find(v.begin(), v.end(), 42)` 一眼看出"找 42"，手写 for 需要多读几行才能确认意图；(2) 正确性——算法用半开区间 `[begin, end)`，无 off-by-one 错误——手写 `for (int i = 0; i <= n; i++)` 多写一个 `=` 就是越界；(3) 性能——`sort` 用 intro sort 混合策略，比大多数人手写的快排更快更稳。算法是"数十年的优化结晶"。

**追问（面试官）**：`std::list` 为什么不能用 `std::sort`？`list` 有自己的 `sort()` 成员函数——为什么要单独提供？

**追问回答**：`std::sort` 需要**随机访问迭代器**——O(1) 跳到第 N 个元素。`list` 的迭代器是**双向迭代器**——只能一步步前进后退。让 `sort` 在链表上工作理论上可以（O(n log n) 比较 + O(n) 指针修改），但标准委员会选择了让 `list` 自己提供 `sort()`——因为链表的排序算法（自底向上 merge sort）和数组完全不同——强制统一接口会导致错误的性能期待。

### 面试题 2：Lambda 的捕获

**面试官**：`[=]` 和 `[&]` 的区别？`[=]` 捕获的变量在 Lambda 内部可以修改吗？

**回答**：`[=]` 按值捕获——Lambda 内部是外部变量的**拷贝**，修改拷贝不影响外部。`[&]` 按引用捕获——Lambda 内部是**外部变量本身**，修改会改变外部。`[=]` 捕获的变量默认是 const 的——不能修改。如果想修改拷贝（不影响外部），加 `mutable`：`[=]() mutable { x++; }`。

**追问（面试官）**：Lambda 可能比捕获的变量活得久——什么场景下会出事？

**追问回答**：按引用捕获 `[&]` + Lambda 被存起来（如放入 `std::function` 成员变量、传入另一个线程）→ Lambda 调用时原始变量已销毁→悬垂引用→UB。典型场景：`std::function<void()> f; { int x = 5; f = [&]{ std::cout << x; }; } f();` —— `x` 已死，`f()` 读野内存。原则：Lambda 的生命周期可能超过变量时，用 `[=]` 按值捕获。

### 面试题 3：std::function 的开销

**面试官**：`std::function` 和函数指针、Lambda 在性能上有什么差别？

**回答**：函数指针——零开销，但只能存函数指针（不能存 Lambda、不能存 bind 表达式）。Lambda——零开销（编译时确定类型，inline 化）。`std::function`——有类型擦除开销（内部用虚函数或函数指针表分发调用），每次调用多一次间接跳转。`function` 还可能在堆上分配（如果 Lambda 太大装不进 `function` 的小缓冲区）。结论——能用模板/`auto` 推导 Lambda 类型时不要用 `std::function`，只在需要"运行时存储不同类型可调用对象"时才用。

**追问（面试官）**：那什么场景下必须用 `std::function` 而不是 `auto` Lambda？

**追问回答**：(1) 回调注册表——`map<string, function<void(int)>>` 存放多个不同 Lambda；(2) 类成员变量——不能声明 `auto` 成员（编译器不知道类型）；(3) 虚接口——把 `std::function` 作为参数类型让调用方可以传 Lambda/函数指针/函数对象。其他场景优先 `auto` Lambda——零开销。

---

## 5. 可运行错误实验

### 实验 1：`[&]` 捕获悬垂引用

```cpp
#include <iostream>
#include <functional>

std::function<void()> create_bug() {
    int x = 42;
    return [&]() {              // 🔴 按引用捕获 x！
        std::cout << x << "\n"; // x 在 create_bug 返回后已死
    };
}

int main() {
    auto f = create_bug();
    f();   // 🔴 UB——读已销毁的 x
    return 0;
}
```

### 实验 2：手写循环 vs 算法——同一需求两种写法

```cpp
#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::vector<int> v = {15, 3, 42, 8, 99, 1, 200, 7};

    // 需求 A：找第一个大于 100 的元素
    // 手写版本
    int found_hand = -1;
    for (size_t i = 0; i < v.size(); i++) {
        if (v[i] > 100) { found_hand = v[i]; break; }
    }

    // 算法版本
    auto it = std::find_if(v.begin(), v.end(),
                           [](int x) { return x > 100; });
    int found_algo = (it != v.end()) ? *it : -1;

    std::cout << "hand: " << found_hand << ", algo: " << found_algo << "\n";

    // 需求 B：把所有偶数翻倍——手写需要 3 行（for+if+赋值），算法需要 1 行
    std::transform(v.begin(), v.end(), v.begin(),
                   [](int x) { return x % 2 == 0 ? x * 2 : x; });
    // v = {15, 6, 84, 8, 99, 2, 200, 14}
    return 0;
}
```

**核心感受**：手写版本你需要看 4 行才能确认"这是在做查找"；算法版本 `find_if` 一个词就够了。

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | sort 的 intro sort 混合策略（快排+堆排+插入排序）原理；`std::function` 类型擦除的虚表开销；Lambda 按值捕获=拷贝构造函数的调用；`list::sort` 自底向上 merge sort 的指针操作实现 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（算法 vs 手写 / Lambda 捕获生命周期 / std::function 开销），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | `[&]` 悬垂引用可运行实验 + `[=]mutable` 陷阱；sort 要求随机迭代器→`list` 不能用（编译错误）；`std::function` 无节制使用的堆分配开销 | 8/10 |
| 交叉引用 | §1 从 C 手写 for 出发 → C 学习路线；§1 迭代器半开区间 → Ch02 §4 迭代器；§5 range-for→Ch02 §4；Lambda 捕获的外部变量 → 函数栈帧（C Ch02 内存模型） | 8/10 |
| **总分** | | **33/40** |
