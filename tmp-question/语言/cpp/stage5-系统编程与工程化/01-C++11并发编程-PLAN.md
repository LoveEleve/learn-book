# Stage5 Ch01 规划：C++11 并发编程

> **定位**：Stage5 第一章。前提——读者已完成 C 语言 Stage3（pthread/互斥锁/条件变量/原子操作），**深刻理解 mutex/condvar 的使用模式**。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §41-42

---

## 1. 本章要解决的核心问题

读者在 C 阶段已经精通 pthread——能用 `pthread_create`/`pthread_mutex_lock`/`pthread_cond_wait` 写线程池。C++11 的 `<thread>`/`<mutex>`/`<condition_variable>`/`<future>` 不是"重新讲一遍并发"——是**把 C 的并发原语包装成 RAII 对象**。

C 里最大的并发陷阱——`pthread_mutex_lock` 后忘了 `unlock`（异常返回路径上）、`pthread_create` 后忘了 `join`/`detach`。C++11 用 RAII 消除这两个坑：`lock_guard` 自动释放 mutex、`thread` 对象析构时如果仍 joinable→`terminate`（强制你不忘了结线程）。

---

## 2. 节结构（5 节）

### §1 `std::thread`——RAII 管理的线程

| C 的 pthread | C++11 替代 | RAII 保证 |
|---|---|---|
| `pthread_create` + `pthread_join` 手动配对 | `std::thread` 对象——析构时自动处理 | 忘记 join→析构 `terminate` 提醒你 |
| 手动 `pthread_detach` | `t.detach()` | 分离后析构安全 |

```cpp
#include <thread>
#include <iostream>

void worker(int id) { std::cout << "Thread " << id << "\n"; }

int main() {
    std::thread t1(worker, 1);          // 创建——参数直接传给 worker
    std::thread t2(worker, 2);
    t1.join();                           // 等待
    t2.join();
    // 如果忘了 join——t1 析构时调用 std::terminate
    return 0;
}
```

- **`std::thread` 对象必须"了结"**：要么 `join()`（等待完成）、要么 `detach()`（分离不等待）。如果析构时既没 join 也没 detach→`std::terminate()`。这个设计强制你面对"这个线程谁来收尸"的问题。
- **和 pthread 的对比**：`pthread_create` 不给 join→子进程变 zombie。`std::thread` 不给 join→进程直接死。C++ 的哲学是"未定义行为应该是显式崩溃，不是静默泄漏"。

### §2 `std::mutex` + `std::lock_guard`——自动释放锁

| C 的 pthread | C++11 替代 | RAII 保证 |
|---|---|---|
| `pthread_mutex_lock/unlock` 手动配对 | `lock_guard<mutex>` ——作用域结束自动 unlock | **异常安全**——即使抛异常也释放锁 |
| 忘记 unlock→死锁 | 离开 `}` 自动 unlock——忘不了 | 编译时保证 |

```cpp
#include <mutex>
std::mutex mtx;
int counter = 0;

void increment() {
    std::lock_guard<std::mutex> lock(mtx);  // 构造——lock
    counter++;                               // 临界区
}  // lock 析构——自动 unlock
```

- **`lock_guard` 是最简单的 RAII 锁包装**：不可移动、不可拷贝、不能手动 unlock。替代 C 里忘记 `pthread_mutex_unlock` 的 #1 bug。
- **`unique_lock`——更灵活的版本**：可以延迟 lock、手动 unlock、移动所有权。配合 `condition_variable` 使用（condvar 需要能临时 unlock 的锁）。
- **`std::mutex` vs `std::recursive_mutex`**：普通 mutex 同一线程多次 lock→死锁。递归 mutex 允许同一线程多次 lock（内部计数——匹配解锁）。

### §3 `std::condition_variable`——和 C 的一样逻辑，但用 `unique_lock`

```cpp
std::mutex mtx;
std::condition_variable cv;
bool ready = false;

// 消费者——和 C 的 pthread_cond_wait 一样逻辑
void consumer() {
    std::unique_lock<std::mutex> lock(mtx);   // 注意——必须是 unique_lock
    cv.wait(lock, []{ return ready; });        // 等价于 while(!ready) cv.wait(lock);
    // ready 为 true——消费数据
}

// 生产者
void producer() {
    {
        std::lock_guard<std::mutex> lock(mtx);
        ready = true;
    }
    cv.notify_one();  // 通知一个等待者
}
```

- **`cv.wait(lock, predicate)`**：C++11 的便捷形式——等价于 `while(!pred()) cv.wait(lock)`。**为什么是 while 不是 if？** 假唤醒（spurious wakeup）——POSIX 允许 `pthread_cond_wait` 在没有通知的情况下返回。C++ 继承了这份行为。用 while 确保唤醒后重新检查条件——`if` 可能"一觉醒来条件又变了"。这和 C 阶段讲的 futex/条件变量完全一致。
- **为什么 condvar 只能用 `unique_lock` 不能用 `lock_guard`**：`wait` 内部需要临时 unlock 锁再重新 lock——`lock_guard` 不支持手动 unlock。
- **`notify_one` vs `notify_all`**：和 C 的 `pthread_cond_signal`/`pthread_cond_broadcast` 完全对应。

### §4 `std::async` / `std::future`——异步任务的更高层抽象

```cpp
#include <future>
#include <iostream>

int compute() { return 42; }

int main() {
    // 异步启动——像调用函数一样简单
    std::future<int> result = std::async(std::launch::async, compute);
    // ... 干点别的 ...
    int value = result.get();  // 阻塞直到 compute 完成——获取返回值
    std::cout << value;        // 42
    return 0;
}
```

- **`std::async`**：启动一个异步任务，返回 `future`——不需要创建 `thread`、不需要 `join`、不需要传结果。
- **`std::launch::async` vs `std::launch::deferred`**：async=新线程执行；deferred=调用方第一次 `get()` 时才执行（惰性求值）。
- **`std::future::get()`**：只可调用一次——获取结果后 future 变成无效。重复 `get()` 是 UB。
- **`std::promise` + `std::future` 配对**：promise 是"生产者端"——在一个线程里 `set_value(42)`；future 是"消费者端"——在另一个线程里 `get()`。比手动 `pthread_create` + `void*` 返回参数安全得多。

### §5 `std::atomic<T>` 在并发中的位置 + 本章速查

C++ 内存模型已在 Stage3 Ch04 讲过——这里只做"在并发场景中什么时候用哪种同步方式"的速查：

| 同步方式 | 适用场景 | 开销 |
|---|---|---|
| `atomic<T>` + relaxed | 计数器/统计——不需要 happens-before | 仅原子指令 |
| `atomic<T>` + acquire/release | flag 通知——生产者消费者配对 | x86 免费/ARM DMB |
| `mutex` + `lock_guard` | 互斥访问共享资源——临界区 > 100ns | futex~1μs |
| `condvar` | 等待条件成立——生产者消费者 | futex + 上下文切换 |
| `async`/`future` | 一次性异步任务——需要返回值 | 线程创建+future 开销 |

**选择原则**：能用 `atomic` 不用 `mutex`、能用 `lock_guard` 不手写 `lock/unlock`、能用 `async` 不手写 `thread`。

---

## 3. 编写方针

1. **每个 C++ 特性配 C/pthread 对照**——让读者看到"C++ 比 C 多做了 XX 保证"
2. **§1-§3 是"用 RAII 消除 C 的并发陷阱"**——`lock_guard` 防忘记 unlock、`thread` 析构防忘记 join
3. **死锁检测工具速查**：TSan——`g++ -fsanitize=thread -g` 编译，运行时检测数据竞争和锁顺序反转（lock-order-inversion）；helgrind——`valgrind --tool=helgrind` 模拟执行，更慢但检测更广（20-50x 开销）。TSan 用于日常开发、helgrind 用于 CI 定期检测
4. 本章不讲的内容：`std::atomic` 详细机制（Stage3 Ch04 已讲）、线程池的实现（已有 C 版本，C++ 版本类似）

---

## 4. 面试题（3 组）

### 面试题 1：lock_guard vs unique_lock

**面试官**：`lock_guard` 和 `unique_lock` 的区别？什么时候必须用 `unique_lock`？

**回答**：`lock_guard`——最简单的 RAII 锁包装，构造 lock/析构 unlock，无额外功能。`unique_lock`——可延迟 lock、手动 unlock、移动所有权。必须用 `unique_lock` 的场景——配合 `condition_variable`（wait 需要临时 unlock）、需要手动 `unlock` 释放锁再重新 lock。默认用 `lock_guard`——只有在需要 `unique_lock` 功能时才换。

**追问**：`unique_lock` 比 `lock_guard` 大多少？有什么额外开销？

**追问回答**：多一个 `bool`（记录是否持有锁）和一个 `mutex*`——约 16 字节 vs 8 字节。构造/析构多一次条件判断。差别很小——不用为了"省这 8 字节"而用 `lock_guard` 替代 `unique_lock`。选锁类型应该看语义（我需要手动 unlock 吗？），不看大小。

### 面试题 2：thread 析构的安全性

**面试官**：`std::thread` 对象析构时如果没 join 也没 detach——会发生什么？为什么 C++ 这么设计？

**回答**：`std::terminate()`——程序直接崩溃。C++ 的设计逻辑——忘记 join 是 bug（资源泄漏），与其"静默泄漏"不如"显式崩溃"让 bug 被注意到。这和"未定义行为"的哲学不同——这里 C++ 选择了"定义行为——崩溃"而不是"未定义行为——可能运气好不出事"。

**追问**：`std::terminate` 和异常的区别？

**追问回答**：`terminate` 不展开栈——不跑析构函数（不像异常那样清理 RAII 对象）。它是"立即死亡"——所有 `thread_local` 变量不被析构、缓冲区不被 flush。`terminate` 等于告诉你"这是一个不可恢复的 bug——别试图优雅退出，直接死掉最好"。

### 面试题 3：async 的实现策略

**面试官**：`std::async` 一定创建新线程吗？什么参数控制？

**回答**：`std::launch::async`——必须创建新线程执行。`std::launch::deferred`——惰性求值（调用方 `get()` 时才在当前线程执行）。默认 `async|deferred`——让库选择。注意——不指定 `launch::async` 时 async 可能不走新线程→没有真正的并发。如果你需要"保证在线程中执行"→写 `std::async(std::launch::async, fn)`。

**追问**：`future::get()` 只能调用一次——为什么？

**追问回答**：`future` 是"单次消费"设计——一旦 `get()` 拿走结果，`future` 变成无效状态。如果需要多次读取结果→用 `shared_future`（多个消费者可以共享同一个结果，每个消费者有独立的 `get()`）。

---

## 5. 可运行错误实验

### 实验：忘记 join 的 thread 析构→terminate

```cpp
#include <thread>
void work() {}
int main() {
    std::thread t(work);  // 没有 join 或 detach
    return 0;             // 🔴 t 析构 → std::terminate()
}
```

---

## 6. 4 维度自检

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | `lock_guard`/`unique_lock` 的 RAII 实现原理；`thread` 析构 terminate 的标准依据；`condvar::wait` 的 predicate 版本消除 while/if 陷阱 | 8/10 |
| 面试覆盖 | 3 组完整 Q&A（lock_guard vs unique_lock / thread 析构 terminate / async launch 策略），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | 忘记 join→terminate 可运行实验；`async` 默认 launch 不走新线程的隐式串行化；死锁检测工具 helgrind/TSan 的推荐 | 8/10 |
| 交叉引用 | §1 从 C pthread_create/join 出发→C 学习路线；§2 lock_guard→Stage1 Ch04 RAII；§5 atomic→Stage3 Ch04 C++ 内存模型 | 8/10 |
| **总分** | | **33/40** |
