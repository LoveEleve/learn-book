# Stage3 Ch04 规划：C++ 内存模型

> **定位**：Stage3 第四章（最后一章）。前提——读者已完成 C 语言 Stage3（pthread/C11 原子操作/内存序/无锁数据结构），**深刻理解 memory_order_relaxed/acquire/release/seq_cst 的含义和 x86/ARM 上的硬件实现**。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §41-42，深入理解C++11 §8-9

---

## 1. 本章要解决的核心问题

C 语言阶段已经详细讲过原子操作和内存序——`atomic_int`、`memory_order_relaxed`、store buffer、MESI、x86 lock 前缀、ARM LDREX/STREX。C++11 的 `std::atomic<T>` 不是"重新讲一遍内存模型"——是讲**C++ 在 C11 基础上加了什么**：

1. **模板化**：`std::atomic<T>` 替代 C11 的 `atomic_int`/`atomic_long`——一套模板覆盖所有类型
2. **默认 seq_cst**：C11 的默认也是 seq_cst，但 C++ 在 STL 中的使用（`shared_ptr` 引用计数用 `memory_order_relaxed`）展示了"降级"的实际场景
3. **`std::atomic` 和普通变量的隔离**：`_Atomic` 修饰的变量不能和普通变量混用（C11 编译器强制），C++ 的 `std::atomic` 可以——但混用是数据竞争
4. **与 C11 的对照**：哪些一样、哪些 C++ 更灵活

---

## 2. 节结构（4 节）

### §1 `std::atomic<T>` —— 模板化的原子操作

从 C11 到 C++11 的迁移——读者熟悉的 C11 原子操作在 C++ 里怎么用：

```cpp
// C11 写法（读者已经会的）:
#include <stdatomic.h>
atomic_int counter = 0;
atomic_fetch_add(&counter, 1);

// C++11 写法——模板化：
#include <atomic>
std::atomic<int> counter{0};
counter.fetch_add(1, std::memory_order_relaxed);
counter.store(0);
int v = counter.load();

// 成员函数 vs 自由函数：
counter++;                       // ✓ operator++ 内部是 fetch_add(seq_cst)
int expected = 0;
counter.compare_exchange_strong(expected, 42);  // CAS
```

- **`std::atomic<T>` 的模板参数**：支持整数类型（`int`/`long`/`size_t`）、指针类型（`T*`）、trivially-copyable 类型。`float`/`double` 从 C++20 才支持。
- **`is_lock_free()`**：运行时检查当前类型在这个平台上是不是真正的无锁实现——有些大类型（如 16 字节 struct）可能用内部锁实现（仍然是原子的，但用了互斥锁）。
- **特殊成员函数被删除**：`atomic<T>` 没有拷贝构造、拷贝赋值——防止"原子变量被拷贝"的语义混乱。

### §2 C++ 内存序——与 C11 的对照

| C++11 内存序 | C11 等价 | 语义 | x86 开销 |
|---|---|---|---|
| `memory_order_relaxed` | `memory_order_relaxed` | 只保证原子性 | 仅原子指令 |
| `memory_order_acquire` | `memory_order_acquire` | 后续不重排到前面 | `mov`（免费） |
| `memory_order_release` | `memory_order_release` | 前面不重排到后面 | `mov`（免费） |
| `memory_order_acq_rel` | `memory_order_acq_rel` | RMW——acquire+release | `lock` 前缀 |
| `memory_order_seq_cst` | `memory_order_seq_cst` | 全局单一顺序（**默认**） | `xchg`/`mfence` |
| `memory_order_consume` | `memory_order_consume` | 依赖序（GCC 降级为 acquire） | 同 acquire |

- **和 C11 完全一样**——C 和 C++ 委员会协调了内存模型的设计，保证语义一致。
- **C++ 使用 `std::memory_order` 枚举**，C 使用 `memory_order` 枚举——名字不一样但值一样。
- **默认 seq_cst 的含义**：如果你不显式指定 memory order——`counter++` 就是 seq_cst。C++ 选择了"默认最安全"——C11 也是这个选择。

### §3 C++ 内存模型的关键差异

虽然基本语义一样，但有几个 C++ 特有的点：

**差异 1：`std::atomic` 和普通变量可以混用——但不要这样做**

C11 的 `_Atomic int` 和普通 `int` 是不同���类型——混用会编译警告。C++ 的 `std::atomic<int>` 和 `int` 是不同的类型——但你可以通过 `.load()`/`.store()` 在两者间转换。如果同一个变量既用 `atomic<int>` 访问又用普通 `int` 指针对同一块内存访问→数据竞争（UB）。

**差异 2：`std::atomic_ref`（C++20）**

对已经存在的普通变量"加一层原子操作"——不创建新的原子变量。C++11 没有（C++20 才有）——标注即可。

**差异 3：`std::atomic_flag` —— 最轻量的原子布尔值**

```cpp
std::atomic_flag lock = ATOMIC_FLAG_INIT;
while (lock.test_and_set(std::memory_order_acquire));  // 自旋锁
lock.clear(std::memory_order_release);                  // 释放
```
- 保证 lock-free——所有平台上都是无锁的。
- C 里有等价的 `atomic_flag`。

**差异 4：`shared_ptr` 的引用计数用的是 `memory_order_relaxed`**

这不是巧合——`shared_ptr` 的控制块里强引用计数的增减用 relaxed 就行（因为析构时才需要看见最新的计数），不必要 seq_cst。编译器利用这一点省下了 mfence 的开销。这是"知道什么时候降级内存序能带来真实性能收益"的经典案例。

### §4 从 C11 无锁数据结构到 C++ 的迁移 + Stage3 收尾

C 阶段写过的无锁数据结构（MPSC 队列/Treiber 栈）在 C++ 里怎么写：

```cpp
// C 版本（读者已会）：
typedef struct {
    _Atomic(mpsc_node_t*) head;
    mpsc_node_t* tail;
} mpsc_queue_t;

// C++ 版本——模板化 + 类型安全：
template <typename T>
class MpscQueue {
    struct Node { T data; Node* next; };
    std::atomic<Node*> head;
    Node* tail;  // 消费者独占——不需要原子
public:
    void enqueue(T val);  // 用 atomic_exchange + release store
    T dequeue();          // 用 acquire load
};
```

**Stage3 四章回顾**：
```
Ch01 继承与多态    → 编译时多态（模板）→ 运行时多态（虚函数）
Ch02 多重继承      → vtable/vbptr 的工程复杂度——什么时候不能用 MI
Ch03 异常安全      → 异常让 RAII 成为必需品——析构必须不抛
Ch04 C++ 内存模型  → C11 原子的 C++ 封装——和 C 知识的衔接
```

**Stage4 预告**：C++11 进阶特性——Lambda 深度、类型萃取、C++11 标准库、性能优化。

---

## 3. 编写方针

1. **不是"从头讲内存模型"**——读者在 C 阶段已经学过了 store buffer/MESI/ARM weak ordering。本章是"C++ 怎么封装 C11 原子操作" + "哪些地方 C++ 做得不同"
2. **每个 C++ 特性配 C11 对照**——`std::atomic<T>` ↔ `_Atomic T`、`fetch_add` ↔ `atomic_fetch_add`——让读者把已有知识直接映射过来
3. **本章不讲的内容**：完整的 CPU 缓存一致性（C 阶段已讲）、无锁数据结构的新实现（C 阶段已讲）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：C11 vs C++11 原子操作

**面试官**：C11 的 `atomic_fetch_add(&counter, 1, memory_order_relaxed)` 在 C++11 里怎么写？有什么不同？

**回答**：C++11：`counter.fetch_add(1, std::memory_order_relaxed);`——成员函数语法 + `std::` 命名空间前缀。核心区别——(1) C++ 是模板类，任何 `T` 只要满足要求就能用 `atomic<T>`；(2) C++ 支持 `operator++`/`operator--` 等操作符重载——`counter++` 等价于 `fetch_add(1, seq_cst)`；(3) C++ 的内存序枚举在 `std::memory_order` 下。

**追问（面试官）**：`std::atomic<double>` 能用吗？`std::atomic<std::string>` 呢？

**追问回答**：`atomic<double>`——C++11 没有 `fetch_add` 等算术操作（C++20 才有 `atomic<float>` 支持），但 `load`/`store`/`exchange`/CAS 是可用的。`atomic<string>`——不能直接用（`string` 不是 trivially-copyable），编译器会报错或用锁实现（`is_lock_free()` 返回 false）。要原子操作 string→用 `atomic<string*>`（指针）或用 mutex 保护。

### 面试题 2：shared_ptr 为什么用 relaxed？

**面试官**：`shared_ptr` 的引用计数操作为什么用 `memory_order_relaxed` 而不是默认的 `seq_cst`？

**回答**：强引用计数的增减不需要顺序保证——你只关心"什么时候归零"，不关心"和谁的 store 有什么 happens-before 关系"。当引用计数从 1 降到 0 时，最后一个 `shared_ptr` 析构时才需要 acquire 语义（确保看到其他线程对对象的所有修改）。降低内存序从 seq_cst 到 relaxed 省了 x86 上 mfence 的开销——每次 `shared_ptr` 拷贝/析构都省 ~33 cycles。

**追问（面试官）**：那弱引用计数（`weak_ptr`）呢？也是 relaxed 吗？

**追问回答**：弱引用计数的增减是 relaxed——同样不需要顺序保证。但 `wp.lock()`（尝试提升为 `shared_ptr`）需要 `compare_exchange_strong` + `memory_order_acquire`——如果提升成功，需要 acquire 语义保证看到对象的状态。如果提升失败（对象已释放）→不需要任何顺序保证。

### 面试题 3：C++ `volatile` vs `std::atomic`

**面试官**：C++ 的 `volatile` 和 `std::atomic` 的区别？为什么不能用 `volatile` 做线程同步？

**回答**：C++ 的 `volatile` 和 C 的 `volatile` 完全一样——告诉编译器"不要优化掉对这个变量的读写"，不做任何多线程保证（无原子性、无可见性、无 happens-before）。`std::atomic` 提供真正的多线程原子操作——原子性（不被打断）、可见性（对全局可见）、happens-before（配合 memory order 建立同步关系）。Java 的 `volatile` 有 happens-before——但 C++ 的 `volatile` 没有。这是 C++ 领域最常见的误解。

**追问（面试官）**：那 C++ 的 `volatile` 到底用在哪？

**追问回答**：和 C 的 volatile 一样的三个场景——(1) MMIO：硬件寄存器地址，每次读取可能改变硬件状态；(2) 信号处理器中修改的全局变量 `volatile sig_atomic_t`；(3) `setjmp`/`longjmp` 之间的局部变量。多线程同步——不在这三个场景内，必须用 `std::atomic`。

---

## 5. 可运行错误实验

### 实验：atomic 和普通变量混用的数据竞争

```cpp
#include <iostream>
#include <thread>
#include <atomic>

int shared = 0;               // 普通变量——没有原子保护
std::atomic<bool> ready{false};

void writer() {
    shared = 42;              // 🔴 不是原子的！
    ready.store(true);        // 原子写
}

void reader() {
    while (!ready.load());    // 原子读——看到 ready=true
    std::cout << shared;      // 🔴 数据竞争！shared 可能还是 0！
    // 因为 shared=42 和 ready.store(true) 之间没有 happen-before 关系
    // 如果 ready 用 release 写 + acquire 读——能保证看到 shared=42
}

int main() {
    std::thread t1(writer), t2(reader);
    t1.join(); t2.join();
    return 0;
}
```

**修复**：`ready` 用 `memory_order_release` 写 + `memory_order_acquire` 读——建立 happens-before，保证 writer 对 `shared` 的写入对 reader 可见。

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | C++ atomic 和 C11 atomic 的语义一致性与 API 差异对照；`shared_ptr` 引用计数选 relaxed 的工程设计理由（mqfence 成本分析）；`is_lock_free()` 的运行时检测意义；原子操作和普通变量混用的 happens-before 缺失 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（C11 vs C++11 / shared_ptr relaxed / volatile vs atomic），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | atomic 和普通变量混用的数据竞争可运行实验；`is_lock_free()` 为 false 时的性能退化；`atomic<T>` 被拷贝的编译错误（特化成员函数被删除） | 8/10 |
| 交叉引用 | §1 从 C11 原子操作出发→读者已有的 C 知识映射；§2-§3 引用 C Stage3 的内存模型/x86-ARM 差异/无锁数据结构；Stage3 收尾→Stage4 预告 | 8/10 |
| **总分** | | **33/40** |
