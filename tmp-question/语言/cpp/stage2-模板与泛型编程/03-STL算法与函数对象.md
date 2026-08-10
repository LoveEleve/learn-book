# STL 算法与函数对象

> **前置依赖**：读者已完成 Ch02（容器/迭代器），能用 `begin()`/`end()` 遍历 `vector`/`map`。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §32-34，深入理解C++11 §6

---

## 1. 引言：遍历之后是什么

Ch02 教了容器和迭代器——你有了装数据的"盒子"和"遍历盒子的指针"。但遍历之后呢？你需要**排序、查找、过滤、变换**。

C 程序员习惯手写 for 循环：

```cpp
// C 模式——手写循环完成"查找"和"计数"
int arr[] = {15, 3, 42, 8, 99, 1};
int count = 0, found = -1;
for (int i = 0; i < 6; i++) {
    if (arr[i] > 10) count++;         // 计数
    if (arr[i] == 42) found = arr[i]; // 查找
}
```

这段代码并不复杂——4 行。但读它的人必须一行行"逆向推断"意图：这个 for 到底在做什么？是在计数还是查找还是两者都是？变量 `count` 和 `found` 的初始化、修改、使用散布在循环的各个角落。

STL 算法给了另一种思路：**你只写"做什么"，不写"怎么做"**：

```cpp
std::vector<int> v = {15, 3, 42, 8, 99, 1};

int n = std::count_if(v.begin(), v.end(),
                      [](int x) { return x > 10; });        // "数一下大于 10 的"

auto it = std::find(v.begin(), v.end(), 42);               // "找 42"
```

`count_if` 一眼看出来：这是计数。`find` 一眼看出来：这是查找。意图不再藏在循环体里——算法名本身就是文档。

本章的核心信息：**停止手写 for 循环——让算法表达你的意图。**

---

## 2. 从手写 for 到算法调用——思维转换

### 2.1 算法的基本格式

所有 STL 算法的调用模式都一样：

```
std::算法名(开始迭代器, 结束迭代器, ...额外参数)
```

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `for (int i = 0; i < n; i++) { if (arr[i] > 10) count++; }` | `int n = std::count_if(v.begin(), v.end(), [](int x){ return x > 10; });` |
| 手写快排几十行 | `std::sort(v.begin(), v.end());` |
| 手写二分查找索引 | `auto it = std::lower_bound(v.begin(), v.end(), 42);` |

**为什么不用手写**：

(1) **性能**——算法的实现经过了数十年优化。`std::sort` 用 **intro sort**（快速排序 + 堆排序 + 插入排序的混合策略）：快排递归深度超过 O(log n) 阈值时自动切换堆排序（防止快排的最坏 O(n²) 退化）；小于 16 个元素时切换插入排序（小数组常数因子最优）。你手写的快排大概率达不到这个水平。

(2) **意图**——读代码的人一眼看出"这是在排序"而不是"这是在嵌套 for 循环"。算法名是自文档化的。

(3) **正确性**——迭代器半开区间 `[begin, end)` 防止 off-by-one 错误。手写 `for (int i = 0; i <= n; i++)` 多写一个 `=` 就是越界。算法没有这个风险。

**`#include <algorithm>`**——本章讲的排序/查找/计数/变换/过滤算法都在这个头文件里。用 `back_inserter` 时需要额外 `#include <iterator>`（`back_inserter` 是迭代器适配器，不属于 `<algorithm>`）。

### 2.2 迭代器——算法的"通用货币"

算法不关心容器的底层结构。它只通过迭代器和容器交互——`begin()` 和 `end()` 是算法知道的全部：

```
vector: [1][2][3][4][5]
         ↑           ↑
       begin        end

list:   head→[1]→[2]→[3]→[4]→[5]
         ↑                       ↑
       begin                    end

// 同一个算法对两种容器都生效
std::find(v.begin(), v.end(), 42);  // vector
std::find(l.begin(), l.end(), 42);  // list——代码一模一样
```

这就是 Ch01 模板思想和 Ch02 迭代器思想的合体——算法是模板函数，参数是迭代器类型。编译器为 `vector::iterator` 和 `list::iterator` 各生成一份 `find` 代码，每份都是零开销。

---

## 3. 核心算法速览——5 个最常用的

不是 API 手册。只讲生产代码中出现频率最高的 5 个算法——目标是你看完本节后能自己查 cppreference 学其他的。

### 3.1 `std::sort`——排序

```cpp
#include <algorithm>
std::vector<int> v = {3, 1, 4, 1, 5, 9, 2, 6};

std::sort(v.begin(), v.end());           // 升序 → {1, 1, 2, 3, 4, 5, 6, 9}
std::sort(v.begin(), v.end(), std::greater<int>()); // 降序

// 自定义排序——按绝对值
std::sort(v.begin(), v.end(),
    [](int a, int b) { return abs(a) < abs(b); });
// 正数时 abs 排序效果和升序一样——有负数才体现差异：
// {-5, 3} → abs(3)=3 < abs(-5)=5 → {3, -5}
```

- **时间复杂度**：O(n log n)
- **算法**：intro sort（快排 + 堆排 + 插入排序混合）
- **要求随机访问迭代器**：`vector`/`deque`/`array` 可以用，`list` 不能用——`list` 有自己的 `sort()` 成员函数。这不是 bug——`sort` 需要 O(1) 随机交换元素，链表做不到。Ch02 §4 讲过 `list::sort()` 使用自底向上 merge sort，O(n log n) 比较 + O(1) 指针修改——算法和数组排序完全不同。
- **比较函数的约定**：`bool cmp(T a, T b)` 必须满足**严格弱序**（strict weak ordering）。最简单的记忆——`cmp` 就像 `<`：`cmp(a, b)` 为 true 表示 `a` 应该排在 `b` 前面。`cmp(a, a)` 必须为 false（不自反）。如果 `cmp(a, b) && cmp(b, a)` 同时为 true——UB。

### 3.2 `std::find` / `std::find_if`——查找

```cpp
auto it = std::find(v.begin(), v.end(), 42);  // 找值 42
if (it != v.end()) {
    std::cout << "找到了: " << *it << '\n';
} else {
    std::cout << "没找到\n";
}

// find_if——按条件找第一个满足的
auto it = std::find_if(v.begin(), v.end(),
    [](int x) { return x > 100; });

// 没找到的哨兵比较——it == v.end()
```

- **返回迭代器**——不是 `bool`，不是索引。为什么？因为迭代器可以直接解引用获取找到的元素。而且"没找到"用 `v.end()` 表示——不需要额外标志位。
- **O(n)**——线性扫描。对于有序容器应该用 `std::lower_bound`（O(log n)——返回迭代器，指向第一个 ≥ 目标值的位置）或 `map::find`（O(log n)）。注意 `std::binary_search` 虽然也是 O(log n)，但只返回 `bool`（有没有）——不能替代 `find` 拿元素。**`lower_bound` 返回的是"插入位置"**——即使元素不存在也会返回有效迭代器。确认元素是否存在必须加 `it != v.end() && *it == target`。
- **`find_if_not`**——C++11 新增，找第一个**不满足**条件的。

### 3.3 `std::count` / `std::count_if`——计数

```cpp
int n3 = std::count(v.begin(), v.end(), 3);                    // 数 3 出现了几次
int ne = std::count_if(v.begin(), v.end(),
    [](int x) { return x % 2 == 0; });                         // 数偶数
```

- **O(n)**——遍历整个区间。注意和 `map::count` 的区别：`map::count` 是 O(log n)（红黑树计数）；算法 `count` 对 `map` 也是 O(n)（线性扫描）。
- **常见错误**：对 `map` 用 `std::count` 代替 `map::count`——性能差了一个对数级。规则：有成员函数 `count` 的容器优先用成员的——它知道容器结构。

### 3.4 `std::transform`——变换（映射）

```cpp
std::vector<int> doubled(v.size());
std::transform(v.begin(), v.end(), doubled.begin(),
    [](int x) { return x * 2; });
// v = {3, 1, 4} → doubled = {6, 2, 8}

// 两个输入——对应元素相加
std::vector<int> a = {1, 2, 3}, b = {4, 5, 6}, result(3);
std::transform(a.begin(), a.end(), b.begin(), result.begin(),
    [](int x, int y) { return x + y; });
// result = {5, 7, 9}
```

- **输出必须预分配**——`doubled(v.size())` 提前分配好空间。如果不知道目标大小，用 `back_inserter`（见 §3.5）。注意目标容器长度必须 ≥ 输入区间长度——写越界是 UB，`transform` 不做任何边界检查。
- **不能用于 `map` 的值变换**——`transform` 会生成新容器，原容器不变。如果想直接修改 `map` 的值，用 range-for 或 `for_each`。

### 3.5 `std::copy_if`——过滤

```cpp
std::vector<int> evens;
std::copy_if(v.begin(), v.end(), std::back_inserter(evens),
    [](int x) { return x % 2 == 0; });
// evens = 所有偶数——back_inserter 自动调用 push_back
```

没有 `back_inserter` 的麻烦做法——需要提前分配空间再截断：

```cpp
std::vector<int> evens2(v.size());
auto it_end = std::copy_if(v.begin(), v.end(), evens2.begin(),
    [](int x) { return x % 2 == 0; });
evens2.erase(it_end, evens2.end());  // 去掉尾部未使用的位置
```

- **`std::back_inserter`**——迭代器适配器，每次赋值时调用容器的 `push_back`。不用提前知道目标大小。
- **为什么叫 `copy_if` 不叫 `filter`？** 因为它不做"原地删除"——它把满足条件的元素**拷贝**到另一个位置。原地删除用 `remove_if` + `erase`（erase-remove idiom——见实验部分）。
- **`remove_if` 和 `erase`**：`remove_if` 把不满足条件的元素往前搬，返回"逻辑末尾"迭代器。然后 `erase` 真正删除尾部的元素——两步操作是因为 `remove_if` 是算法（只知道迭代器，不知道容器的 `erase` 方法）。

---

## 4. Lambda 表达式——让算法活起来

上面的代码里出现了 `[](int x) { return x > 100; }`——这是 Lambda 表达式。它是 C++11 最重要的特性之一——让 STL 算法从"能用的工具"变成"好用的工具"。

### 4.1 Lambda 的基本语法

```
[捕获](参数) -> 返回类型 { 函数体 }
```

最简形式——返回类型省略（编译器自动推导）：

```cpp
[](int x) { return x > 100; }
//  ↑        ↑                    ↑
//  捕获列表  参数列表              函数体
```

- **捕获列表**：把外部的变量"带进"Lambda
- **参数列表**：和普通函数一样
- **函数体**：和普通函数一样

### 4.2 捕获——把外部变量带进来

```cpp
int threshold = 100;
std::vector<int> v = {10, 50, 150, 200, 80};

// 按值捕获——Lambda 内部是 threshold 的拷贝
auto it = std::find_if(v.begin(), v.end(),
    [threshold](int x) { return x > threshold; });  // threshold = 100

// 按引用捕获——Lambda 内部读写的是外部变量本身
int found = 0;
std::for_each(v.begin(), v.end(),
    [&found](int x) { if (x > 100) found++; });
// found = 2
```

**`[=]` vs `[&]` vs 显式捕获**：

```cpp
[=]            // 按值捕获所有——代码简短但不推荐——容易误改外部变量
[&]            // 按引用捕获所有——同样不推荐
[threshold]    // 按值捕获 threshold——推荐：清晰、防 bug
[&found]       // 按引用捕获 found
[=, &found]    // 大部分按值，found 按引用
[&, threshold] // 大部分按引用，threshold 按值
```

**C++ 社区共识**：永远不要写 `[=]` 和 `[&]`——显式写出每个捕获的变量名。`[=]` 让你以为"所有都是拷贝，很安全"——但如果有指针成员，你拷贝的是指针值，它指向的数据还在外面。`[&]` 更危险——大型 Lambda 被传给异步操作或存到队列里时，引用的变量早已销毁。

**更隐蔽的陷阱——成员函数中的 `[=]` 隐式捕获 `this`**：

```cpp
struct Foo {
    int val;
    auto get_fn() {
        return [=] { return val; };  // 🔴 [=] 不会拷贝 val！它捕获的是 this 指针
    }
};
Foo* f = new Foo{42};
auto fn = f->get_fn();
delete f;                // f 已销毁
fn();                    // 🔴 UB——this 悬垂，val 是 this->val
```

在成员函数里写 `[=]`，捕获列表中**自动加上了 `this`**——成员变量 `val` 走的是 `this->val`，而不是 `val` 的拷贝。标准委员会在 C++20 已废弃 `[=]` 的隐式 `this` 捕获（改为 `[=, this]` 显式声明）。解决方案——手动拷贝成员到局部变量：

```cpp
auto get_fn_safe() {
    int copy = val;
    return [copy] { return copy; };  // ✓ 明确按值捕获局部变量
}
```

### 4.3 Lambda 的本质——编译器生成的匿名类

Lambda 不是语法糖——它等价于编译器为你生成一个匿名类：

```cpp
// 你写的：
auto lambda = [threshold](int x) { return x > threshold; };

// 编译器等价生成：
class __anonymous_lambda {
    int threshold;   // 捕获的变量成为成员
public:
    __anonymous_lambda(int t) : threshold(t) {}
    bool operator()(int x) const { return x > threshold; }
};
auto lambda = __anonymous_lambda(threshold);
```

这就是为什么 `[=]` 按值捕获时**调用被捕获变量的拷贝构造函数**——闭包对象的成员变量初始化走的就是拷贝构造。这也解释了为什么 `[=]` 对 `unique_ptr` 不能用——`unique_ptr` 没有拷贝构造——和 Stage1 Ch04 的智能指针遥相呼应。

`operator()` 默认是 `const`——所以 `[=]` 捕获的变量默认不可修改。加 `mutable` 去掉 `const`：

```cpp
int x = 0;
auto f = [x]() mutable { return ++x; };
std::cout << f() << '\n';  // 1——修改的是 Lambda 内部的拷贝
std::cout << f() << '\n';  // 2——同一个 Lambda 对象，状态保持
std::cout << x << '\n';     // 0——外部变量没变
```

### 4.4 Lambda 和函数指针

不捕获任何变量的 Lambda 可以隐式转为函数指针——兼容 C API：

```cpp
void register_callback(void (*cb)(int));

register_callback([](int x) { std::cout << x; });  // ✓ [] 可以转为函数指针
int threshold = 10;
// register_callback([threshold](int x) { ... });   // ✗ 有捕获不能转为函数指针
```

> **C++14 起——泛型 Lambda**：参数类型也能写 `auto`：`[](auto x) { return x * 2; }`。编译器生成一个 `template<typename T> auto operator()(T x)` 的闭包类——和函数模板一样的"按需生成"模型。泛型 Lambda 让 `transform` 一行搞定类型无关的映射——不需要提前声明模板函数。

---

## 5. `std::function`——给 Lambda 一个"名字"

### 5.1 为什么需要 `std::function`

Lambda 的类型是编译器生成的匿名类型——不能直接存到成员变量里（`auto` 不能作为成员）。`std::function` 解决了这个问题——给任何可调用对象一个统一的类型：

```cpp
#include <functional>

// std::function——存储任何可调用的东西
std::function<int(int, int)> op;
op = [](int a, int b) { return a + b; };
int result = op(3, 5);  // 8

op = [](int a, int b) { return a * b; };
result = op(3, 5);      // 15——同一个 function 变量，不同行为
```

### 5.2 使用场景

```cpp
// 场景 1：回调注册表——按名字存不同 Lambda
std::map<std::string, std::function<void(int)>> callbacks;
callbacks["print"] = [](int x) { std::cout << x << '\n'; };
callbacks["double"] = [](int x) { std::cout << x * 2 << '\n'; };
callbacks["print"](42);  // 42

// 场景 2：策略模式——运行时切换算法
std::function<int(int, int)> strategy;
strategy = [](int a, int b) { return std::max(a, b); };  // 取最大值
strategy = [](int a, int b) { return a + b; };            // 切换为求和
```

### 5.3 `std::function` 的开销——类型擦除的代价

`std::function` 的便利是有代价的：

```cpp
auto lambda = [](int x) { return x * 2; };    // 编译时类型——inline 化，零开销
std::function<int(int)> func = lambda;         // 类型擦除——间接调用 + 可能堆分配
```

- **间接调用**：`function` 内部用虚函数表或函数指针表分发调用，每次调用多一次间接跳转。Lambda 的 `operator()` 可以被 inline 进调用点——`function` 的间接调用阻止了 inline 优化。
- **堆分配**：每个 `function` 对象内部有一个小缓冲区（类似 SSO——Ch02 §3.2 的短字符串优化）。如果 Lambda 太大（捕获很多变量）装不进缓冲区，`function` 会在堆上分配内存。性能敏感的路径上，这可能是瓶颈。
- **类型擦除**：`function<int(int)>` 可以存 Lambda / 函数指针 / `bind` 表达式 / 函数对象——代价是运行时才知道具体存了什么。

**原则**：能用模板推导 Lambda 类型时（用 `auto`）不要用 `std::function`。只在需要"运行时存储不同类型可调用对象"时才用——成员变量、容器存储、虚接口参数。

### 5.4 `std::bind`——C++11 引入但 Lambda 更好

```cpp
#include <functional>
int add(int a, int b) { return a + b; }

// bind 方式——绑定第一个参数为 10
auto add10 = std::bind(add, 10, std::placeholders::_1);
std::cout << add10(5) << '\n';  // 15

// Lambda 方式——更清晰
auto add10_lambda = [](int b) { return 10 + b; };
```

C++ 社区的趋势是：**能用 Lambda 就不用 `bind`**。Lambda 更清晰（参数流向一目了然）、更高效（无需函数对象包装）、C++14 起通用 Lambda（`auto` 参数）覆盖了 `bind` 的大部分场景。本书后续不会再用 `bind`——你知道它存在就行。

---

## 6. range-for 与算法——什么时候用什么

### 6.1 两条路

```cpp
// range-for——适合"对每个元素做某件事"
for (auto& x : v) { x *= 2; }             // 每个元素翻倍

// 算法——适合"求某件事的结果"
auto it = std::find(v.begin(), v.end(), 42);             // 找到 42
int n   = std::count_if(v.begin(), v.end(),
    [](int x) { return x % 2 == 0; });    // 数偶数
```

### 6.2 选择表

| 场景 | 用 | 因为 |
|---|---|---|
| 对每个元素做一件事（有副作用） | range-for | 简单直白，无额外抽象 |
| 查找/计数/排序/变换（求结果） | 算法 | 意图明确、已优化、防 bug |
| 需要同时处理多个容器 | 算法 | `transform` 同时读 A 写 B |
| 中间退出循环（break/continue） | range-for | 算法没有 `break` |

### 6.3 折中——`std::for_each`

当你想用 range-for 写副操作、但希望保持算法风格时：

```cpp
// 等价于 range-for，但用算法调用——范围明确、不会越界
std::for_each(v.begin(), v.end(), [](int& x) { x *= 2; });
```

`for_each` 和 range-for 的基本功能等效——但它有两个额外优势：(1) 输入范围明确是 `[begin, end)`——range-for 依赖于整个容器的范围。(2) **`for_each` 返回函数对象**——配合有公开成员的命名函数对象可以读出累积状态：

```cpp
// 命名函数对象——累积状态且外部可读取
struct Sum {
    int total = 0;
    void operator()(int x) { total += x; }
};
Sum s = std::for_each(v.begin(), v.end(), Sum{});
std::cout << s.total << '\n';  // for_each 返回函数对象——total 可直接访问
```

这就是为什么 C++11 之前的算法需要写完整的函数对象类——闭包（Lambda）把状态封装在匿名类里不让你碰。Lambda 好用，但一旦需要"读完状态"，命名函数对象比 `mutable` Lambda 更合理。C++14 起可以用初始化捕获 `[sum = 0]` 绕开这个问题，但本书保持 C++11。

---

## 7. 可运行错误实验

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
    f();   // 🔴 UB——读已销毁的 x（随机值或 crash）
    return 0;
}
```

**编译运行**：`g++ -std=c++11 lambda_bug.cpp -o lambda_bug && ./lambda_bug`
**输出**：随机值（如 `0` 或 `32767`）或不 crash——但这是 UB，没有"正确"行为。

**修复**——把 `[&]` 改成 `[=]`：

```cpp
std::function<void()> create_fixed() {
    int x = 42;
    return [=]() {              // ✓ 按值捕获——Lambda 拥有 x 的拷贝
        std::cout << x << "\n";
    };
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
    // 手写版本——4 行，你需要逐行读才能确认意图
    int found_hand = -1;
    for (size_t i = 0; i < v.size(); i++) {
        if (v[i] > 100) { found_hand = v[i]; break; }
    }

    // 算法版本——1 行，意图一眼可见
    auto it = std::find_if(v.begin(), v.end(),
                           [](int x) { return x > 100; });
    int found_algo = (it != v.end()) ? *it : -1;

    std::cout << "hand: " << found_hand << ", algo: " << found_algo << "\n";

    // 需求 B：把所有偶数翻倍
    std::transform(v.begin(), v.end(), v.begin(),
                   [](int x) { return x % 2 == 0 ? x * 2 : x; });
    // v = {15, 6, 84, 8, 99, 2, 200, 14}

    return 0;
}
```

**核心感受**：手写版本的 `found_hand`、`i`、`break` 分布在 4 行里——读代码的人需要"拼接"这些碎片才能理解"这是在做查找"。算法版本的 `find_if` ——一个词就说明了一切。

### 实验 3：`remove_if` + `erase`——原地删除奇数

```cpp
#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::vector<int> v = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    // remove_if 把"不删除"的元素往前搬——返回"逻辑末尾"迭代器
    auto new_end = std::remove_if(v.begin(), v.end(),
        [](int x) { return x % 2 != 0; });  // 删除所有奇数

    // remove_if 之后 v 的内容——[2][4][6][8][10][?][?][?][?][?]
    //                                       ↑ new_end
    // 尾部 ? 是遗留值——必须 erase 真正删除
    v.erase(new_end, v.end());

    for (int x : v) std::cout << x << ' ';  // 2 4 6 8 10
    return 0;
}
```

**为什么两步**：`remove_if` 只是算法——它不知道容器的 `erase` 方法。它把要保留的元素往左搬，返回"逻辑末尾"迭代器。`erase` 才是容器的方法——从逻辑末尾到真实末尾，真正释放空间。这两步合称 **erase-remove idiom**——面试中极高频的考点。

---

## 8. 面试题

### 面试题 1：算法 vs 手写循环

**面试官**：`std::find` 和手写 for 循环有什么区别？为什么应该用前者？

**回答**：(1) 意图——`find(v.begin(), v.end(), 42)` 一眼看出"找 42"。手写 `for (int i = 0; i < n; i++) { if (arr[i] == 42) { ... } }` 需要多读几行才能确认意图。(2) 正确性——算法用半开区间 `[begin, end)`，无 off-by-one 错误。手写 `for (int i = 0; i <= n; i++)` 多写一个 `=` 就是越界。(3) 性能——`sort` 用 intro sort 混合策略（快排 + 堆排 + 插入排序），比大多数人手写的快排更快更稳。算法是"数十年的优化结晶"。

**追问（面试官）**：`std::list` 为什么不能用 `std::sort`？`list` 有自己的 `sort()` 成员函数——为什么要单独提供？

**追问回答**：`std::sort` 需要**随机访问迭代器**——O(1) 跳到第 N 个元素。`list` 的迭代器只是**双向迭代器**——只能一步步前进后退。让 `sort` 在链表上工作理论上可以（O(n log n) 比较 + O(n) 指针修改），但标准委员会特意让 `list` 自己提供 `sort()`——因为链表的排序算法（自底向上 merge sort）和数组的 intro sort 完全不同——强制统一接口会误导使用者（以为 `list` 也能 O(1) 随机访问）。

### 面试题 2：Lambda 的捕获

**面试官**：`[=]` 和 `[&]` 的区别？`[=]` 捕获的变量在 Lambda 内部可以修改吗？

**回答**：`[=]` 按值捕获——Lambda 内部是外部变量的**拷贝**，修改拷贝不影响外部。`[&]` 按引用捕获——Lambda 内部是**外部变量本身**，修改会改变外部。`[=]` 捕获的变量默认是 const 的——不能修改（因为闭包类的 `operator()` 默认 `const`）。如果想修改拷贝（不影响外部），加 `mutable`：`[=]() mutable { x++; }`——这去掉了 `operator()` 的 `const`。

**追问（面试官）**：Lambda 可能比捕获的变量活得久——什么场景下会出事？

**追问回答**：按引用捕获 `[&]` + Lambda 被存起来（如放入 `std::function` 成员变量、传入另一个线程）→ Lambda 调用时原始变量已销毁→悬垂引用→UB。典型场景：

```cpp
std::function<void()> f;
{
    int x = 5;
    f = [&] { std::cout << x; };  // 按引用捕获
}  // x 已死
f();  // 🔴 UB——读野内存
```

原则：Lambda 的生命周期可能超过捕获变量时，用 `[=]` 或 `[x]` 按值捕获。

### 面试题 3：`std::function` 的开销

**面试官**：`std::function` 和函数指针、Lambda 在性能上有什么差别？

**回答**：函数指针——零开销，但只能存函数指针（不能存有捕获的 Lambda、不能存 `bind` 表达式）。Lambda——零开销（编译时确定类型，可被 inline 化）。`std::function`——有类型擦除开销（内部用虚函数或函数指针表分发调用），每次调用多一次间接跳转。还可能堆分配（如果 Lambda 太大装不进 `function` 的小缓冲区）。结论——能用模板/`auto` 推导 Lambda 类型时不要用 `std::function`，只在需要"运行时存储不同类型可调用对象"时才用。

**追问（面试官）**：那什么场景下必须用 `std::function` 而不是 `auto` Lambda？

**追问回答**：(1) 回调注册表——`map<string, function<void(int)>>` 存放多个不同类型的 Lambda/函数指针/函数对象。(2) 类成员变量——不能声明 `auto` 类型的成员（编译器必须在类定义时知道成员的大小和类型）。(3) 虚接口——把 `std::function` 作为函数参数类型，让调用方可以传任何可调用对象进来。其他场景优先 `auto` Lambda——零开销。

---

## 9. 本章小结

| C 的做法 | C++ 的做法 | 核心收益 |
|---|---|---|
| `for (i=0; i<n; i++) { if(...) count++; }` | `count_if(v.begin(), v.end(), pred)` | 意图明确——"这是在计数" |
| 手写快排几十行 | `sort(v.begin(), v.end())` | intro sort 混合策略——比你手写的快 |
| 手写函数指针回调 | `function<void(int)> cb` | 类型安全、可存 Lambda |
| C 里没有闭包 | `[threshold](int x) { ... }` | 捕获上下文，即写即用 |

**本章没讲但后续章节会讲**：变参模板（Ch04——`make_shared` 的模板魔法）、SFINAE / `enable_if`（Ch04）、Concepts（C++20，本系列不展开）、并行算法（C++17）、ranges（C++20）。

**你学会查 cppreference 了吗？** 本章只讲了 5 个算法。剩下几十个（`all_of`/`any_of`/`none_of`、`remove_if`/`unique`、`lower_bound`/`upper_bound`、`partition`/`stable_partition`、`next_permutation`、`merge`/`inplace_merge`……）你不需要背——打开 [cppreference.com](https://en.cppreference.com/w/cpp/algorithm)，看分类、看签名、看复杂度，用的时候查即可。

下一章进入 Stage2 终章——**变参模板与 SFINAE**——模板的最深层。你会理解 `make_shared` 怎么做到参数个数任意、`enable_if` 怎么在编译时"开灯关灯"选择重载。
