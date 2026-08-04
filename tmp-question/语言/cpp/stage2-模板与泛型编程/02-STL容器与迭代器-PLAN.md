# Stage2 Ch02 规划：STL 容器与迭代器

> **定位**：Stage2 第二章。前提——读者已完成 Ch01 模板基础，理解"蓝图→代码"的模板编译模型。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §31-33，深入理解C++11 §10

---

## 1. 本章要解决的核心问题

C 里没有标准容器——你要么手写链表/哈希表（几百行），要么用 `void*` 加函数指针模拟泛型容器（不安全）。C++ 的 STL 容器是模板的经典应用：**用 `vector<int>` 跟用 `int[]` 一样快，但多了自动扩容、边界检查、迭代器、移动语义。**

本章的定位：不是 STL API 手册——是**让一个 C 程序员理解"为什么 STL 容器设计成这样"、"什么时候用哪个容器"、"迭代器是什么鬼"**。学完本章你能：用 `vector` 替代 C 数组、用 `map` 替代手工哈希表、用迭代器写出不和具体容器绑定的代码。

---

## 2. 节结构（6 节）

### §1 从 C 数组到 `std::vector`——告别 `malloc`/`realloc`

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `int* arr = malloc(n * sizeof(int));` 然后手动 `free` | `vector<int> arr(n);` ——离开作用域自动释放 |
| `arr = realloc(arr, new_n * sizeof(int));` ——O(n) 搬家 | `arr.push_back(42);` ——自动扩容，几何级增长 |
| 不能问"arr 有多大"——必须自己记 `n` | `arr.size()` ——O(1) |

```cpp
#include <vector>
std::vector<int> v;
v.push_back(10);      // 尾加——O(1) 均摊
v.push_back(20);
v.push_back(30);
std::cout << v[1];    // 20——像数组一样用 []
std::cout << v.size(); // 3
// v 离开作用域——自动释放内存
```

- **`vector` 的内存模型**：三指针——`begin`/`end`/`capacity_end`。`size()` = `end - begin`，`capacity()` = `capacity_end - begin`。
- **扩容策略**：大多实现用 2 倍增长（VS 用 1.5 倍）。扩容时分配新内存→移动元素（C++11 移动语义）→释放旧内存。**为什么是 2x 或 1.5x？** 2x——扩容最简单，但可能导致内存利用率最多只有 50%（刚翻倍的空间一半是空的）。1.5x——任何一次扩容释放的旧内存，其大小必定大于之前所有分配的总和的一半，这意味着旧内存可以**被后续分配复用**（减少向 OS 申请新内存的次数）——VS 选择 1.5x 是为了更高效的内存复用。
- **`reserve` vs `resize`**：`reserve(1000)` 预留空间（不改 size），`resize(1000)` 实际创建 1000 个元素。知道要插入大量元素时用 `reserve` 先预留——避免多次扩容。

### §2 `std::string`——不是 `char*` 的包装

C 的字符串是 `char*`——没有长度字段（`strlen` O(n)）、不能存 `\0`、拼接需要 `malloc+strcpy+free`。C++ 的 `std::string`：

```cpp
std::string s = "hello";
s += " world";                // 拼接——自动管理内存
std::cout << s.length();      // 11 ——O(1)
std::string sub = s.substr(0, 5); // "hello"
// 比较——直接用 == != < >
if (s == "hello world") { ... }
```

- **SSO（短字符串优化）**：对于短字符串（通常 ≤15 字符），`string` 不分配堆内存——数据直接存在对象内部（`sizeof(string)` 通常是 24-32 字节）。这是 C++ 标准库最经典的零开销优化之一。
- **和 `vector<char>` 的区别**：`string` 保证 `\0` 结尾（`c_str()` 兼容 C API），有字符串专用操作（`find`/`substr`/`compare`）。

### §3 `std::map` / `std::unordered_map`——键值存储

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| 手写哈希表——几百行 | `unordered_map<string, int>` ——一行 |

```cpp
#include <map>
#include <unordered_map>

// map——红黑树，键有序，O(log n)
std::map<std::string, int> ages;
ages["Alice"] = 30;            // 插入
ages["Bob"]   = 25;
for (auto& kv : ages) {        // 遍历——按键的字母顺序
    std::cout << kv.first << ": " << kv.second << "\n";
}   // Alice: 30, Bob: 25

// unordered_map——哈希表，键无序，O(1) 平均
std::unordered_map<std::string, int> scores;
scores["Alice"] = 100;         // 插入
```

| 容器 | 底层结构 | 查找 | 插入 | 有序？ | 内存 |
|---|---|---|---|---|---|
| `map` | 红黑树 | O(log n) | O(log n) | 是 | 每个节点多一些指针 |
| `unordered_map` | 哈希表 | O(1) 平均 | O(1) 平均 | 否 | 哈希桶数组+链表 |

- **选哪个**：需要有序遍历→`map`；只需要"有没有"→`unordered_map`。
- **`operator[]` vs `at()`**：`m["key"]`——key 不存在时插入默认值。`m.at("key")`——key 不存在时抛异常。用 `[]` 读值时注意——一不小心就插入了一个你不想要的元素。

### §4 迭代器——容器之间的"通用语言"

不同容器的内部结构完全不同——`vector` 是连续数组，`list` 是链表，`map` 是红黑树。但迭代器给所有容器提供了**统一的访问接口**：

```cpp
std::vector<int> v = {1, 2, 3, 4, 5};

// 用迭代器遍历——和用指针遍历一样
for (auto it = v.begin(); it != v.end(); ++it) {
    std::cout << *it << " ";   // *it 像解引用指针
}

// range-for（C++11）——更简洁的语法糖
for (int x : v) {
    std::cout << x << " ";
}
```

- **迭代器和指针的关系**：`vector` 的迭代器就是 `T*`（指针）——`*it` 是真正的指针解引用。对 `list`/`map`，迭代器是类对象——`*it` 是 `operator*` 的重载。
- **`begin()`/`end()`**：`begin()` 指向第一个元素，`end()` 指向**最后一个元素之后**（不是最后一个）。和 C 的"数组+长度"不同——迭代器范围是"半开区间" `[begin, end)`。
- **迭代器失效**：`vector` 扩容后所有迭代器失效（因为数据搬家了）——这是常见 bug。`push_back` 后不要用之前保存的迭代器。

### §5 容器的元素存储——值语义与移动

STL 容器存的是**值的副本**——不是指针、不是引用：

```cpp
std::vector<Buffer> v;
Buffer b("hello");
v.push_back(b);              // 拷贝 b——深拷贝
v.push_back(std::move(b));   // 移动 b——b 变成空壳
v.emplace_back("world");     // 在容器内部直接构造——零拷贝零移动
```

- **`push_back` vs `emplace_back`**（C++11）：`push_back` 先构造临时对象→再拷贝/移动到容器。`emplace_back` 直接把构造参数传给容器——在容器内部原地构造（一次构造，零额外操作）。
- **容器里放指针？** 可以用，但容器不负责释放——`vector<Foo*>` 析构时只释放指针数组，不释放 `Foo` 对象。优先 `vector<Foo>`（值语义）或用 `vector<unique_ptr<Foo>>`（明确所有权）。

### §6 容器选择指南

| 场景 | 用 | 因为 |
|---|---|---|
| 存一系列元素，按顺序访问 | `vector` | 连续内存——缓存友好、O(1) 随机访问 |
| 在头部/中间插入删除 | `list`（或 `deque`） | vector 中间插入 O(n)——要搬家 |
| 键值查找，需要有序 | `map` | 红黑树——按键自动排序 |
| 键值查找，只要快 | `unordered_map` | 哈希表——O(1) |
| 不允许重复元素 | `set` | 自动去重 |
| 固定大小，编译时已知 | `array`（C++11） | 零开销——栈上分配，无动态内存 |

- **默认选 `vector`**——"vector 做了你想做的事的概率最高"
- 容器可以嵌套：`map<string, vector<int>>`——键是字符串，值是整数列表
- 本章不讲的内容：自定义 allocator（Stage4 性能章）、`deque`/`forward_list` 细节（参考手册即可）

---

## 3. 编写方针

1. **不是 API 手册**——不列所有成员函数，只讲核心概念和最常用的几个操作
2. **每节有可运行代码**——读者亲手创建容器、插入元素、遍历删除
3. **从 C 对应物出发**：C 数组→`vector`、C 手写哈希表→`unordered_map`、C 手写链表→`list`
4. 本章不讲的内容：STL 算法（Ch03）、allocator 进阶（Stage4）、自定义迭代器（Stage4）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：vector 的扩容机制

**面试官**：`vector` 的 `push_back` 为什么是 O(1) 均摊？扩容时发生了什么？

**回答**：`push_back` 大多数时候 O(1)——直接在已分配的空间末尾写一个元素。当 `size == capacity` 时需要扩容——分配更大的内存（通常 2 倍）、把旧元素移动/拷贝到新内存、释放旧内存。虽然这次操作 O(n)，但因为扩容后容量翻倍，接下来很多次 push_back 都不需要扩容——均摊到每次 push_back 还是 O(1)。

**追问（面试官）**：C++11 的移动语义对扩容有什么影响？为什么要让移动构造 `noexcept`？

**追问回答**：C++98 扩容用拷贝——O(n) 内存拷贝。C++11 扩容用移动——每个元素只是指针交换（O(1) 每个元素），整体扩容快数倍。前提是移动构造标记了 `noexcept`——如果没有，`vector` 在扩容时不敢用移动（因为移动中途抛异常，旧数据已经被"偷"了没法回滚）→退化为拷贝。这就是 Ch03 §3 讲的"移动构造必须 noexcept"的兑现场景。

### 面试题 2：map vs unordered_map

**面试官**：`map` 和 `unordered_map` 的区别？什么时候用哪个？

**回答**：底层结构不同——`map` 用红黑树（键有序，O(log n)），`unordered_map` 用哈希表（键无序，O(1) 平均）。需要按序遍历键→`map`；只要"有没有"→`unordered_map`。内存也不同——`map` 每个节点有额外的指针开销（左/右/父节点指针+颜色），`unordered_map` 有哈希桶数组开销。

**追问（面试官）**：`unordered_map` 的 O(1) 在什么情况下退化为 O(n)？

**追问回答**：哈希冲突严重时。如果所有 key 都映射到同一个桶——桶里的链表长度 = n → 查找变 O(n)。原因——(1) 自写的 bad hash 函数；或 (2) 攻击者故意构造碰撞 key（哈希洪水攻击）。C++ 标准库的 `std::hash` 对基本类型保证了较好的分布，但对自定义类型需要你自己写好 `operator==` 和 `std::hash` 特化。

### 面试题 3：迭代器失效

**面试官**：`vector` 在什么操作后迭代器会失效？写出一个迭代器失效的 bug 场景。

**追问回答**：(1) `push_back` 触发扩容时——所有已有迭代器全部失效（数据搬家了）；(2) 在中间 `insert`/`erase` 时——当前位置及之后的所有迭代器失效。典型 bug：`for (auto it = v.begin(); it != v.end(); ++it) { if (*it == target) v.erase(it); }`——erase 后 it 失效了，`++it` 是 UB。正确写法：`it = v.erase(it);`（erase 返回下一个有效迭代器）。

**追问（面试官）**：`list` 的迭代器呢？和 `vector` 有什么不同？

**追问回答**：`list` 是链表——`erase` 只失效被删除的元素的迭代器（其他元素的内存地址不变）。`push_back` 不会失效任何已有迭代器（新节点独立分配）。这是链表相比 vector 的一大优势——但 vector 的连续内存带来更好的缓存局部性。

---

## 5. 可运行错误实验

### 实验：迭代器失效

```cpp
#include <vector>
#include <iostream>

int main() {
    std::vector<int> v = {1, 2, 3, 4, 5};
    auto it = v.begin();      // 指向 v[0]
    std::cout << *it << "\n"; // 1

    // 触发扩容——所有数据从旧内存搬到新内存
    for (int i = 0; i < 100; i++) {
        v.push_back(i);       // 某次 push_back 触发扩容
    }

    std::cout << *it << "\n"; // 🔴 UB——it 指向的旧内存已被释放！
    // 运行时可能：打印随机值、打印 1（碰巧旧内存没被覆写）、SIGSEGV
    return 0;
}
```

**运行**：`g++ -std=c++11 iter_bug.cpp && ./a.out`——行为不确定。

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | `vector` 三指针内存模型 + 扩容倍数选择的权衡（2x vs 1.5x）；`string` SSO 优化的原理；`unordered_map` 哈希冲突的退化机制；迭代器与指针的本质关系 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（扩容机制+mobile noexcept / map vs unordered_map + 哈希退化 / 迭代器失效+list 对比），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | 迭代器失效可运行实验；`operator[]` 意外插入默认值；`push_back` vs `emplace_back` 的性能差异；`vector<bool>` 不是真正的容器（`&v[0]` 不返回 `bool*`） | 8/10 |
| 交叉引用 | §1 从 C 数组/malloc 出发 → C 学习路线；§1 扩容用移动 → Ch03 noexcept；§4 迭代器 → Ch03 STL 算法（range-for 和算法统一用迭代器）；§5 emplace_back → Ch01 右值引用和移动语义 | 8/10 |
| **总分** | | **33/40** |
