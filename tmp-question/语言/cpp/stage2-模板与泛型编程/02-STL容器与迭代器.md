# STL 容器与迭代器

> **前置依赖**：读者已完成 Ch01 模板基础，理解"蓝图→代码"的模板编译模型。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §31-33，深入理解C++11 §10

---

## 1. 引言：C 的"手工数据结构"和 C++ 的"开箱即用"

C 里没有标准容器——你需要一个动态数组就得写 `malloc`+`realloc`、需要一个哈希表就得手写几百行链表+哈希函数。每次新项目都重新发明轮子，而且用 `void*` 做泛型没有类型安全。

C++ 的 STL 容器是 Ch01 模板思想的经典应用：`vector<int>` 跟 `int[]` 一样快，但多了自动扩容、`size()`、迭代器、移动语义。本章不是 STL API 手册——是让一个 C 程序员理解"什么时候用哪个容器"和"迭代器是什么"。

---

## 2. `vector`——告别 `malloc`/`realloc`

### 2.1 C 的动态数组 vs C++ 的 `vector`

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| `int* arr = malloc(n * sizeof(int));` 手动 `free` | `vector<int> arr(n);` 离开作用域自动释放 |
| `arr = realloc(arr, new_size);` O(n) 搬家 | `arr.push_back(42);` 自动扩容 |
| 不能问"arr 多大"——必须自己记 `n` | `arr.size()` O(1) |

```cpp
#include <vector>
std::vector<int> v;
v.push_back(10);
v.push_back(20);
v.push_back(30);
std::cout << v[1];      // 20——像数组一样用 []
std::cout << v.size();  // 3
// v 离开作用域——析构函数自动释放内存
```

### 2.2 `vector` 的内存模型——三指针

`vector` 内部用三个指针管理内存：

```
┌──────────────┐
│ begin        │──→ 已使用空间的起始
│ end          │──→ 已使用空间的末尾
│ capacity_end │──→ 已分配空间的末尾
└──────────────┘

[10][20][30][??????????????????????????]
 ↑         ↑                        ↑
 begin     end                capacity_end
 size = end - begin = 3
 capacity = capacity_end - begin = 16（举例）
```

- **`size()`**——已使用的元素数（`end - begin`）
- **`capacity()`**——已分配的空间数（`capacity_end - begin`）
- **`size() <= capacity()`**——没有剩余空间时 `push_back` 触发扩容

### 2.3 扩容策略——2x vs 1.5x

扩容时分配更大的内存（通常 2 倍）、把旧元素**移动**到新内存（Ch03 移动语义）、释放旧内存。虽然这次操作 O(n)，但扩容后容量翻倍，接下来很多次 push_back 都不需要扩容——均摊 O(1)。

**为什么是 2x 或 1.5x？** 2x——扩容最简单，但可能导致内存利用率最多 50%（刚翻倍的空间一半是空的）。1.5x——每次扩容释放的旧内存，其大小必定大于之前所有分配总和的一半——旧内存可以被**后续分配复用**（减少向 OS 申请新内存的次数）。VS 选 1.5x 是为了更高效的内存复用。

### 2.4 `reserve` vs `resize`

```cpp
v.reserve(1000);  // 预留空间（不改 size）——后续 push_back 不扩容
v.resize(1000);   // 实际创建 1000 个元素——size 变成 1000
```

知道要插入大量元素时，先用 `reserve` 预留空间——避免多次扩容。反过来，如果 `vector` 当前 `capacity` 远大于 `size`（如 `clear()` 后），可以用 `v.shrink_to_fit()`（C++11）建议释放多余容量——注意是"建议"，标准不保证一定回收。

---

## 3. `string`——不是 `char*` 的包装

### 3.1 C 字符串的五个痛点

C 的 `char*` 有五个根本缺陷：无长度字段（`strlen` O(n)）、不能存 `\0`（二进制不安全）、拼接需要 `malloc+strcpy+free`、比较用 `strcmp` 不是 `==`、缓冲区溢出风险。C++ 的 `std::string` 全部解决：

```cpp
std::string s = "hello";
s += " world";                    // 拼接——自动管理内存
std::cout << s.length();          // 11——O(1)
std::string sub = s.substr(0, 5); // "hello"
if (s == "hello world") { ... }   // == 直接比较内容
```

### 3.2 SSO（短字符串优化）

C++ 标准库最经典的零开销优化：对于短字符串（阈值取决于编译器，GCC libstdc++ 为 15 字符，MSVC 为 15，Clang libc++ 为 22），`string` **不分配堆内存**——数据直接存在对象内部。`sizeof(string)` 通常是 24-32 字节——除了堆指针外还有内部缓冲区。

### 3.3 和 C API 互操作——`c_str()`

```cpp
std::string s = "data.txt";
FILE* f = fopen(s.c_str(), "r");  // c_str() 返回 const char*——兼容 C API
```

`c_str()` 保证返回 `\0` 结尾的 C 风格字符串——`vector<char>` 没有这个保证。

---

## 4. `map` / `unordered_map`——键值存储

### 4.1 两个选择

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| 手写哈希表几百行 | `unordered_map<string, int>` 一行 |

```cpp
// map——红黑树，键有序，O(log n)
std::map<std::string, int> ages;
ages["Alice"] = 30;
ages["Bob"]   = 25;
for (auto& kv : ages) {          // 遍历——按键的字母顺序
    std::cout << kv.first << ": " << kv.second << "\n";
}  // Alice: 30, Bob: 25

// unordered_map——哈希表，键无序，O(1) 平均
std::unordered_map<std::string, int> scores;
scores["Alice"] = 100;
```

| 容器 | 底层 | 查找 | 插入 | 有序？ | 内存开销 |
|---|---|---|---|---|---|
| `map` | 红黑树 | O(log n) | O(log n) | 是 | 每节点 3 指针 + 颜色 |
| `unordered_map` | 哈希表 | O(1) 平均 | O(1) 平均 | 否 | 桶数组 + 链表节点 |

**选哪个**：需要按序遍历键 → `map`；只要"有没有" → `unordered_map`。

### 4.2 `operator[]` 的陷阱

```cpp
int x = ages["Charlie"];   // 🔴 Charlie 不存在——自动插入一个 age=0！
```

`m["key"]` 在 key 不存在时**插入默认值**。读操作应该用 `m.at("key")`（不存在时抛异常）或 `m.find("key")`（返回迭代器，`== m.end()` 表示不存在）。

---

## 5. 迭代器——容器世界的"通用指针"

### 5.1 为什么需要迭代器

不同容器的内部结构完全不同——`vector` 是连续数组，`list` 是链表，`map` 是红黑树。但迭代器给所有容器提供了**统一的访问接口**：

```cpp
std::vector<int> v = {1, 2, 3, 4, 5};

// 用迭代器遍历——和用指针遍历一样
for (auto it = v.begin(); it != v.end(); ++it) {
    std::cout << *it << " ";   // *it 像解引用指针
}

// range-for (C++11)——更简洁
for (int x : v) { std::cout << x << " "; }
```

### 5.2 迭代器和指针的关系

- `vector` 的迭代器通常是 `T*`（裸指针）——三大编译器（GCC/Clang/MSVC）都用裸指针实现，`*it` 是真实的指针解引用。标准只要求 RandomAccessIterator——Debug 模式下可能是带检查的包装类。`vector<int>::iterator` ≈ `int*`。
- `list`/`map` 的迭代器是类对象——`*it` 是 `operator*` 的重载，内部维护指向链表节点或红黑树节点的指针。

### 5.3 `begin()`/`end()` 和半开区间

`begin()` 指向第一个元素，`end()` 指向**最后一个元素之后**（不是最后一个）。迭代器范围是半边开的 `[begin, end)`——和 C 的"数组+长度"不同。

### 5.4 迭代器失效——最常见的 STL bug

`vector` 扩容后所有老的迭代器失效——因为数据从旧内存搬到了新内存：

```cpp
auto it = v.begin();        // 指向 v[0]
for (int i = 0; i < 1000; i++) v.push_back(i);  // 触发扩容——迭代器失效！
std::cout << *it << '\n';   // 🔴 UB！it 指向已释放的旧内存
```

`list` 不同——它是链表，`push_back` 不搬家，已有迭代器不失效。

---

## 6. 容器的值语义与 `emplace_back`

STL 容器存的是**值的副本**——不是指针：

```cpp
std::vector<Buffer> v;
Buffer b("hello");
v.push_back(b);                  // 深拷贝 b
v.push_back(std::move(b));       // 移动 b——b 变空壳
v.emplace_back("world");         // 在容器内部直接构造——零拷贝零移动！
```

**`push_back` vs `emplace_back`（C++11）**：`push_back(existing_obj)` ——直接**拷贝**已有对象进容器（不经过临时对象）。`push_back(T(args...))` ——先构造临时对象→再**移动**进容器→两次构造。`emplace_back(args...)` ——把构造参数直接传给容器，在内部原地构造——**一次构造**完成。最典型的收益场景：`v.emplace_back("hello")` 直接在容器内构造 `string`，而 `v.push_back(string("hello"))` 需要先构造临时 `string`→再移动。对大对象（`string`、`Buffer`）收益更大。

**容器里放指针？** `vector<Foo*>` 析构时只释放指针数组，不释放 `Foo` 对象。优先 `vector<Foo>`（值语义）或 `vector<unique_ptr<Foo>>`（明确所有权）。

---

## 7. 容器选择指南

| 场景 | 用 | 原因 |
|---|---|---|
| 存一系列元素，顺序访问 | `vector` | 连续内存——缓存友好、O(1) 随机访问 |
| 头部插入删除 | `deque` | 头尾 O(1) + 分段连续内存——比 `list` 缓存友好。`list` 适用于需要频繁中间插入 + 迭代器不失效的场景 |
| 键值查找，需要有序 | `map` | 红黑树——按键自动排序 |
| 键值查找，只要快 | `unordered_map` | 哈希——O(1) |
| 不允许重复 | `set` / `unordered_set` | 自动去重 |
| 固定大小，编译时已知 | `array`（C++11） | 零开销——栈上分配 |

**默认选 `vector`**——"vector 做了你想做的事的概率最高"。

### 7.1 `deque`——头尾 O(1) 的双端队列

`deque`（double-ended queue）不是单段连续数组——是指向多个固定大小**块**的指针数组（分段连续内存）。这给了它 `vector` 没有的能力——头部插入 O(1)：

```cpp
#include <deque>

std::deque<int> dq = {3, 4, 5};
dq.push_front(2);             // ✓ vector 没有这个——vector 头部插入 O(n)
dq.push_back(6);              // 和 vector 一样
dq.pop_front();               // ✓ 头部弹出 O(1)
d[2];                         // ✓ 说了是分段连续——支持 O(1) 随机访问
```

| | `vector` | `deque` |
|---|---|---|
| push_back | O(1) 均摊 | O(1) |
| push_front | O(n) | O(1) |
| 随机访问 | O(1) | O(1)（略慢——需要先定位到块） |
| 内存 | 单段连续 | 分段连续——多一段间接寻址 |

**选 `deque` 时**：需要头部操作 + 随机访问。如果用 `list` 代替——没有随机访问。`deque` 是两者的折中。

### 7.2 `priority_queue`——堆适配器

`priority_queue` 不是独立容器——是**适配器**，底层默认用 `vector` + 堆算法：

```cpp
#include <queue>

std::priority_queue<int> pq;
pq.push(3); pq.push(1); pq.push(4); pq.push(1); pq.push(5);

while (!pq.empty()) {
    std::cout << pq.top() << ' ';  // 每次取最大元素
    pq.pop();
}
// 输出：5 4 3 1 1
```

- **默认是大顶堆**（`std::less`——最大在顶）。小顶堆——`priority_queue<int, vector<int>, greater<int>> pq;`
- **O(log n) push + O(1) top**——每次插入维护堆性质
- **不能随机访问**——只允许 `top()` + `push()` + `pop()`

### 7.3 `set` / `multiset` / `unordered_set`——只存 key 的容器

```cpp
#include <set>
#include <unordered_set>

// set——红黑树，有序，不允许重复
std::set<int> s = {3, 1, 4, 1, 5};  // → {1, 3, 4, 5}——排序 + 自动去重
s.insert(2);
if (s.find(3) != s.end()) { /* 找到了 */ }

// multiset——允许重复
std::multiset<int> ms = {3, 1, 4, 1, 5};  // → {1, 1, 3, 4, 5}
ms.count(1);  // 2——两个 1

// unordered_set——哈希表，无序
std::unordered_set<int> us = {3, 1, 4, 1, 5};  // 顺序不确定——O(1) 查找
```

**什么时候用 set**：需要"是否存在" + 有序遍历——`set`（O(log n)）。只要"是否存在"——`unordered_set`（O(1) 平均）。需要"有多少个"——`multiset`。

### 7.4 `bitset`——编译时常量长度的位数组

```cpp
#include <bitset>

std::bitset<8> flags;            // 8 位——全 0
flags.set(0);                    // 第 0 位 = 1
flags.set(3);                    // 第 3 位 = 1
std::cout << flags;              // 00001001
std::cout << flags.count();      // 2——有多少位是 1
flags.flip(0);                   // 翻转第 0 位——1→0

// 位运算
std::bitset<8> a("1010"), b("0110");
std::cout << (a & b);   // 0010——按位与
std::cout << (a | b);   // 1110——按位或
```

**和 C 的 `unsigned int` 位标志对比**：`bitset` 长度不受 CPU 字长限制（可以 `bitset<256>`）、有语义明确的操作（`set`/`flip`/`count` 代替 `|=` / `^=` / `__builtin_popcount`）、可以 `std::cout << bitset` 打印。

### 7.5 迭代器类别——五种迭代器的能力表

| 类别 | 支持 | 例子 |
|---|---|---|
| 输入迭代器 | `++`、`==`/`!=`、`*`（只读） | `istream_iterator`——从文件/网络流读一次 |
| 输出迭代器 | `++`、`*`（只写） | `ostream_iterator`——写入文件/网络流 |
| 前向迭代器 | 以上 + 可以重复遍历 | `forward_list::iterator`——单向链表 |
| 双向迭代器 | 以上 + `--` | `list::iterator`——双向链表 |
| 随机访问 | 以上 + `+=`/`+`/`-`/`[]`/`<` | `vector::iterator`——连续数组 |

**为什么需要知道类别**：算法对迭代器类别有最低要求。`std::sort` 要求随机访问（需要 O(1) 跳到任意位置）——`list` 的迭代器是双向的，不能传给 `sort`。这就是为什么 `list` 有自己的 `sort()` 成员函数——Ch03 §1 会展开。

### 7.6 自定义 Allocator——概念与动机

STL 容器默认用 `std::allocator<T>`（调用 `new`/`delete`）。自定义 allocator 可以替换分配策略：

```cpp
#include <memory>

// 最简 allocator——只为了看接口长什么样
template <typename T>
struct MyAlloc {
    using value_type = T;

    T* allocate(std::size_t n) {
        return static_cast<T*>(::operator new(n * sizeof(T)));
    }
    void deallocate(T* p, std::size_t) {
        ::operator delete(p);
    }
};

std::vector<int, MyAlloc<int>> v;  // 用自定义 allocator 替代默认
```

**什么时候需要自定义 allocator**：内存池（频繁小对象分配→一次 mmap 切块）、共享内存（用 mmap 创建进程间共享的 vector）、对齐分配（需要 SIMD 指令——16/32/64 字节对齐）。

本节不讲完整的 C++ allocator 接口——只是让你知道"可以换"，看到 `std::vector<int, MyAlloc<int>>` 时知道第二个模板参数是什么。

---

## 8. 可运行实验：迭代器失效

```cpp
#include <vector>
#include <iostream>

int main() {
    std::vector<int> v = {1, 2, 3, 4, 5};
    auto it = v.begin();        // 指向 v[0]
    std::cout << *it << '\n';   // 1

    for (int i = 0; i < 100; i++) v.push_back(i);  // 触发扩容
    std::cout << *it << '\n';   // 🔴 UB！迭代器失效
    return 0;
}
```

**运行**：`g++ -std=c++11 iter_bug.cpp && ./a.out`——行为不确定（随机值/crash）。

---

## 9. 面试题

### 面试题 1：vector 扩容机制

**面试官**：`push_back` 为什么 O(1) 均摊？扩容做了什么？

**回答**：大多数时候直接在末尾写入——O(1)。`size==capacity` 时扩容：分配更大内存（2x）、移动旧元素、释放旧内存——单次 O(n) 但均摊 O(1)（扩容后有很多次 push_back 不需要再扩容）。

**追问（面试官）**：C++11 移动语义对扩容有什么影响？为什么移动构造要 `noexcept`？

**追问回答**：C++98 扩容用拷贝——O(n)。C++11 扩容用移动——每个元素只交换指针 O(1)。前提是移动构造标记 `noexcept`——没标记时 `vector` 不敢用移动（抛异常无法回滚）退化为拷贝。这就是 Ch03 "移动构造必须 noexcept"的兑现场景。

### 面试题 2：map vs unordered_map

**面试官**：`map` 和 `unordered_map` 的区别？什么时候用哪个？

**回答**：`map` 红黑树——键有序，O(log n)，需要按序遍历键时用。`unordered_map` 哈希表——键无序，O(1) 平均，只要"有没有"时用。内存也不同——`map` 每节点有左/右/父/颜色指针，`unordered_map` 有桶数组开销。

**追问（面试官）**：`unordered_map` 的 O(1) 在什么情况下退化为 O(n)？

**追问回答**：哈希冲突严重时——所有 key 落到同一个桶→链表长度=n→O(n)。原因：自写的 bad hash 函数或攻击者构造碰撞 key（哈希洪水攻击）。C++ 标准库的 `std::hash` 对基本类型分布较好，自定义类型需要你自己写好 `operator==` 和 `std::hash` 特化。生产环境中通过 `max_load_factor()` 和 `rehash()` 控制负载因子——如 `um.max_load_factor(0.75); um.reserve(10000);` 预先分配足够桶数，减少冲突。

### 面试题 3：迭代器失效

**面试官**：`vector` 在什么操作后迭代器失效？写出一个失效的 bug 场景。

**回答**：`push_back` 触发扩容——所有已有迭代器都失效（数据搬家）。中间 `insert`/`erase`——当前位置及之后的迭代器失效。

**追问（面试官）**：`list` 的迭代器也会失效吗？

**追问回答**：`list` 是链表——`push_back` 不搬家，已有迭代器不失效。`erase` 只失效被删元素的那个迭代器。这是链表相比 vector 的优势——但 vector 连续内存缓存友好。

---

## 10. 本章小结

| C 的做法 | C++ 的做法 | 核心收益 |
|---|---|---|
| `malloc`+`realloc`+手动 `free` | `vector` | 自动扩容 + 自动释放 |
| `char*` + `strlen` O(n) | `string` | O(1) length + SSO |
| 手写哈希表几百行 | `unordered_map` | 一行 |
| 不同数据结构访问方式各不同 | 迭代器 | 统一 `begin()`/`end()` 接口 |
| 头插用 list（无随机访问） | `deque` | 头尾 O(1) + 随机访问 |
| 手写堆 | `priority_queue` | vector + 堆算法——默认大顶堆 |
| 手写去重+排序 | `set` / `unordered_set` | 自动去重 |
| 手写位标志 | `bitset` | 编译时常量大小 + 语义操作 |

**本章没讲但后续章节会讲**：STL 算法（Ch03）、完整 allocator 接口（Stage4 性能优化章）。

下一章进入 **STL 算法与函数对象**——用 `sort`/`find`/`transform` 代替手写 for 循环。
