# Stage4 Ch03 规划：C++11 标准库深度

> **定位**：Stage4 第三章。前提——读者已完成 Stage2（STL 容器/算法/迭代器）和 Stage4 Ch01-02（Lambda/类型萃取），对 `<vector>`/`<map>`/`<algorithm>` 有使用经验。
> **主线版本**：C++11
> **参考书**：深入理解C++11 §10-11，精通现代C++ §2-3

---

## 1. 本章要解决的核心问题

Stage2 的 STL 覆盖了最常用的容器（`vector`/`map`/`string`）和算法（`sort`/`find`/`transform`）。但 C++11 还补充了大量"不用自己写的标准工具"——时间库替代 `time()`/`gettimeofday()`、正则库替代手写字符串解析、随机数替代 `rand()`%N、`tuple` 替代手写多返回值 struct。

本章不是"标准库 API 大全"——是"C 程序员习惯手写的功能 C++11 已经替你做好了"的指南。学完本章你不再手写 `strptime`/`sscanf`/`rand()`——有更好用、更安全、零开销的标准替代。

---

## 2. 节结构（5 节）

### §1 `<chrono>`——告别 `gettimeofday` 和 `sleep(1)`

C 的时间操作是分散的——`time()` 秒级精度、`gettimeofday()` 微秒但受系统时间跳变影响、`usleep()` 被废弃。`<chrono>` 统一了这一切：

```cpp
#include <chrono>
#include <thread>

// 时间点——"现在是什么时候"
auto start = std::chrono::steady_clock::now();  // 单调钟——不受系统时间影响
// ... 做点什么 ...
auto end = std::chrono::steady_clock::now();

// 时间段——"过了多久"
auto elapsed = end - start;                     // duration 类型
auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count();
std::cout << "took " << ms << " ms\n";

// 延时——替代 usleep()
std::this_thread::sleep_for(std::chrono::milliseconds(100));  // 睡 100ms
```

| 时钟类型 | 含义 | 用途 |
|---|---|---|
| `system_clock` | 墙上时钟——会跳变 | 日志时间戳 |
| `steady_clock` | 单调钟——只前进不退 | **所有超时/延时/性能测量** |
| `high_resolution_clock` | 最高精度（通常是 steady_clock 的别名） | 基准测试 |

- **和 C 的对比**：`gettimeofday()` → `system_clock::now()`；`usleep(1000)` → `sleep_for(milliseconds(1))`；手动 `tv_sec*1000 + tv_usec` 换算 → `duration_cast<milliseconds>()`。
- **类型安全**：`end - start` 返回的是带单位的类型——不会搞混"这是秒还是毫秒"。
- **`<thread>` 中的 `sleep_for`**：需要一个时间段，不是绝对时间点——`sleep_for(1s)` 不是 `sleep_until(next_second)`。

### §2 `<regex>`——告别 `sscanf` 和手写字符串解析

```cpp
#include <regex>
#include <string>

// 匹配——"这像不像一个 email？"
std::regex email_regex(R"((\w+)(\.\w+)*@(\w+)(\.\w+)+)");
std::string text = "Contact us at support@example.com or sales@foo.org";

std::smatch match;
// 搜索——找第一个匹配
if (std::regex_search(text, match, email_regex)) {
    std::cout << "Found: " << match.str() << "\n";  // support@example.com
}

// 替换——把所有 email 改成 [hidden]
auto result = std::regex_replace(text, email_regex, "[hidden]");
```

- **和 C 的对比**：C 里要么手写字符扫描（几十行）、要么用 POSIX `regex.h`（C API，不跨平台）。C++11 `regex` 是跨平台标准库。
- **性能注意**：`regex` 构造时有编译开销（把正则字符串编译成状态机）——循环中重复构造 `regex` 对象是性能陷阱。应该构造一次、复用。
- **原始字符串 `R"(...)"`（C++11）**：不用转义反斜杠——`R"(\w+)"` 代替 C 的 `"\\w+"`。

### §3 `<tuple>` 与 `<pair>`——多返回值和异构容器

```cpp
#include <tuple>
#include <string>

// 返回多个值——不用手写一个小 struct
std::tuple<int, std::string, bool> get_user(int id) {
    return std::make_tuple(25, "Alice", true);
}

auto result = get_user(42);
int age           = std::get<0>(result);     // 按位置取
std::string name  = std::get<1>(result);
bool active       = std::get<2>(result);

// C++11 的 tie——拆包
int a; std::string n; bool act;
std::tie(a, n, act) = get_user(42);
// C++17 结构化绑定：auto [age, name, active] = get_user(42);

// pair——两个值的 tuple 特化
std::pair<std::string, int> entry = {"Alice", 30};
std::cout << entry.first << ": " << entry.second << "\n";
```

- **`tie` vs 结构化绑定**：`tie` 是 C++11 的标准拆包方式——需要先声明变量。C++17 的结构化绑定更简洁（一行）。
- **`tuple_cat`/`tuple_element`**：连接 tuple、获取 tuple 中第 N 个元素的类型——模板元编程的常用工具。
- **和 `struct` 的选择**：返回两个值→`pair`；返回三个及以上、类型各不相同→`tuple`；返回多个值且有语义名称（不是"第 0 个"）→手写 struct（`struct UserResult { int age; string name; bool active; };`——名字更清晰）。

### §4 `<random>`——告别 `rand() % N`

`rand()` 的问题是全局状态 + 分布不均匀 + 不能重现。`<random>` 把"生成随机数"分成两步——引擎（产生随机位）+ 分布（把随机位映射到你想要的分布）：

```cpp
#include <random>

// 引擎——随机位来源
std::mt19937 rng(std::random_device{}());  // Mersenne Twister 引擎

// 分布——把随机位映射到范围
std::uniform_int_distribution<int> dist(1, 6);  // 骰子——1 到 6
for (int i = 0; i < 5; i++)
    std::cout << dist(rng) << " ";  // 如：3 1 6 2 5

// 其他分布
std::normal_distribution<double>  gauss(0.0, 1.0);    // 正态分布
std::bernoulli_distribution       coin(0.5);           // 硬币——50% true
```

- **`rand() % N` 的问题**：`rand()` 的范围是 0 到 RAND_MAX——`rand() % 6` 不均匀（因为 RAND_MAX 通常不是 6 的倍数，低几个数概率高）。`uniform_int_distribution(1,6)` 用 rejection sampling 保证每个数字概率完全相等——超出目标范围的随机位被丢弃并重试，消除偏置。
- **可重现性**：`std::mt19937 rng(42);`——固定种子，每次运行同样的"随机"序列。测试和调试的必备特性。
- **线程安全**：每个线程创建自己的 `rng`——不共享全局随机状态（`rand()` 是全局的）。

### §5 `<array>` 与补充特性速览

**`std::array`——带 size 的 C 数组**：
```cpp
std::array<int, 5> arr = {1, 2, 3, 4, 5};
std::cout << arr.size();   // 5 ——编译时常量，零开销
std::sort(arr.begin(), arr.end());  // 可以用 STL 算法！
// arr 传参时不会退化成指针——保持完整类型
// sizeof(arr) = 5 * sizeof(int) ——不是 sizeof(int*)
```

**`std::forward_list`**：单向链表——比 `list` 更省内存（每个节点少一个指针）。只有前向遍历——适合"只需要插入不需要回头"的场景。

**`std::unordered_set`**：哈希集合——只存 key 不存 value。`unordered_map` 不要 value 只有 key 的时候用它。

**`std::to_string` / `std::stoi`（C++11）**：字符串和数字互转——`to_string(42)` → `"42"`，`stoi("42")` → `42`。替代 C 的 `atoi/itoa/sprintf`。

这些特性不单独成一节——一张速查表 + 一句话说明即可。

---

## 3. 编写方针

1. **每节从 C 的对应物出发**——`chrono` ↔ `gettimeofday/usleep`、`regex` ↔ `sscanf`、`random` ↔ `rand()%N`——让读者看到"C++ 标准库让我少写了多少行 C 代码"
2. **不是 API 大全**——每个主题只讲最常用的 3-5 个操作，引导读者学会自查 cppreference
3. **§4 random 是面试重点**——`uniform_int_distribution` 的均匀性保证 vs `rand() % N` 的不均匀问题
4. 本章不讲的内容：`<filesystem>`（C++17）、`<optional>`/`<variant>`（C++17）、`<string_view>`（C++17）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：chrono 三时钟

**面试官**：`system_clock` 和 `steady_clock` 的区别？为什么超时计算不能用 `system_clock`？

**回答**：`system_clock` 是墙上时钟——NTP 步进/系统管理员 `date -s` 会导致时间跳变（回退或跳跃）。`steady_clock` 是单调钟——从某个未指定的起始点开始单调递增，不受外部影响。超时计算用 `system_clock` 可能因为时间回退→负超时→死循环。这和 C 里 `gettimeofday` vs `CLOCK_MONOTONIC` 是同一个道理——C++11 把它包装成了类型安全的 `<chrono>`。

**追问（面试官）**：`duration_cast` 是做什么的？为什么需要显式 cast？

**追问回答**：`duration_cast<target_unit>(duration)`——把时间段从一种单位转换成另一种（秒→毫秒）。需要显式 cast 是因为转换可能丢失精度（如 1.5 秒转成毫秒是 1500，但如果反过来毫秒→秒→1500ms = 1s——丢掉 500ms）。`duration_cast` 是截断转换；`floor`/`ceil`（C++17）分别是向下/向上取整转换。

### 面试题 2：random 库 vs rand()

**面试官**：C++11 的 `<random>` 和 C 的 `rand()` 有什么区别？为什么不能用 `rand() % N`？

**回答**：(1) 分布质量——`rand()` 返回 0~RAND_MAX 的均匀分布，但 `rand() % N` 不均匀（low bias）。`uniform_int_distribution(1,N)` 保证每个值概率完全相等。(2) 引擎和分布分离——你可以换引擎不换分布（如用于测试的固定种子引擎、用于加密的 `random_device`）。(3) 线程安全——每个线程独立创建 `mt19937` 对象，不共享全局状态。(4) 可重现——`mt19937 rng(42)` 每次运行生成相同序列。

**追问（面试官）**：`std::random_device` 是真正的随机数吗？

**追问回答**：取决于实现——通常用硬件熵源（如 `/dev/urandom`、RDRAND 指令）。如果平台没有熵源→可能退化为伪随机（和 `mt19937` 一样）。所以 `random_device` 用于**种子生成**（给 `mt19937` 随机种子），不用于**大量随机数生成**——它的吞吐量远低于 `mt19937`。

### 面试题 3：regex 的性能

**面试官**：`regex` 有什么性能陷阱？生产代码中怎么避免？

**回答**：`regex` 构造时把正则字符串编译成状态机——有编译开销。循环里每次都构造 `regex` 对象→同样的编译重复做→性能灾难。正确做法——构造一次，复用：`static const regex re(R"(\d+)");`——`static` 保证只构造一次。另外 `regex_match`（全匹配）比 `regex_search`（部分匹配）快——因为后者需要尝试从每个位置开始匹配。

**追问（面试官）**：C++ regex 比 Python/JS 的正则慢吗？为什么？

**追问回答**：通常慢——C++ regex 的标准库实现（libstdc++/libc++）没有用 JIT 编译或其他高级优化（Google RE2 有，但标准库不一定）。原因是 C++ 标注库设计目标更保守——跨平台兼容、不含外部依赖——和 Python 的 `re` 模块（可能调用 C 扩展）不同。如果正则匹配是热点路径→考虑 PCRE/RE2 等外部库。

---

## 5. 可运行错误实验

### 实验：regex 循环构造的性能陷阱

```cpp
#include <regex>
#include <chrono>
#include <iostream>

int main() {
    std::string text = "abc 123 def 456 ghi 789";
    auto start = std::chrono::steady_clock::now();

    // 🔴 坏习惯——每次循环构造 regex
    for (int i = 0; i < 10000; i++) {
        std::regex re(R"(\d+)");
        std::regex_search(text, re);
    }

    auto mid = std::chrono::steady_clock::now();
    std::cout << "re-construct: " << (mid - start).count() << " ns\n";

    // ✓ 好习惯——构造一次复用
    std::regex re(R"(\d+)");   // 提到循环外
    for (int i = 0; i < 10000; i++) {
        std::regex_search(text, re);
    }

    auto end = std::chrono::steady_clock::now();
    std::cout << "re-use:       " << (end - mid).count() << " ns\n";
    // 构造版本通常慢 10-100 倍
    return 0;
}
```

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | `steady_clock` vs `system_clock` 的 monotonic 保证（NTP/闰秒/date -s）；`uniform_int_distribution` 的偏置消除算法；regex 状态机编译的构造开销机制 | 7/10 |
| 面试覆盖 | 3 组完整面试 Q&A（chrono 三时钟 / random vs rand / regex 性能陷阱），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | regex 循环构造可运行实验；`rand()%N` 的不均匀问题演示；`system_clock` 用于超时计算的时间跳变隐患 | 8/10 |
| 交叉引用 | §1 steady_clock → C Stage4 Ch05 时间与定时器（CLOCK_MONOTONIC）；§3 tuple tie → C++17 结构化绑定（标注）；§4 random N 偏置 → C 的 rand() 问题 | 7/10 |
| **总分** | | **31/40** |
