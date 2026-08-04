# Stage4 Ch04 规划：性能与内存优化

> **定位**：Stage4 第四章（最后一章）。前提——读者已完成 Stage1-4 全部内容，理解移动语义/SSO/SBO/虚函数/内联/模板编译模型。
> **主线版本**：C++11
> **参考书**：C++性能优化指南，C++代码调试的艺术 第2版

---

## 1. 本章要解决的核心问题

前 15 章教了你怎么写出**正确**的 C++ 代码——类/RAII/智能指针/模板/STL/虚函数/异常安全。本章的视角不同——同样正确的代码，性能可以差 10 倍。

本章的核心信息：**正确的 C++ 代码 ≠ 快的 C++ 代码**。优化不是"加 `-O3` 编译"——是理解每一行代码在 CPU 缓存、内存分配、编译器优化层面的代价。学完本章你能：用 `emplace_back` 替代 `push_back` 省一次移动、用 `noexcept` 让 vector 扩容走移动、用 `perf` 找到真正的热点。

---

## 2. 节结构（5 节）

### §1 `emplace` 系列——零拷贝构造

```cpp
std::vector<std::string> v;

// push_back——先构造临时 string，再移动进 vector
v.push_back(std::string(5, 'x'));  // 构造临时对象 → 移动进 vector → 临时对象析构

// emplace_back——直接在 vector 内部构造
v.emplace_back(5, 'x');  // 只调用一次构造函数——零拷贝零移动
```

- **`emplace_back` 做了什么**：把构造参���原样转发给容器内部——在容器的内存上一把构造完成。省掉了临时对象的构造 + 移动 + 析构（三步变一步）。
- **什么时候一定用 emplace**：对象构造代价高（`string`、`vector`、任何有内存分配的类型）。简单类型（`int`、`pointer`）——`push_back` 和 `emplace_back` 没区别。
- **`emplace` 系列全家**：`vector::emplace_back`（尾插）、`map::emplace`（键值插入）、`set::emplace`（元素插入）——所有 STL 容器都支持。

### §2 `noexcept` 的性能兑现——vector 扩容的真实收益

Stage1 Ch03 和 Stage3 Ch03 反复提到"移动构造不标记 `noexcept` → vector 扩容用拷贝"。本章用 benchmark 验证这个说法：

```cpp
#include <vector>
#include <chrono>

struct NoExcept { NoExcept(NoExcept&&) noexcept {} };
struct Throwing  { Throwing(Throwing&&) {} };  // 没有 noexcept

// 测试：push_back 触发多次扩容
std::vector<NoExcept> v1;  // 扩容用移动——快
std::vector<Throwing>  v2; // 扩容用拷贝——慢
```

- **benchmark 预期**：`NoExcept` 版本比 `Throwing` 快 2-5 倍（取决于对象大小）。
- **核心检查**：你的移动构造/移动赋值标记 `noexcept` 了吗？没标记→vector/deque 的扩容不信任你的移动→走拷贝→性能白给。
- **工具检查**：`static_assert(std::is_nothrow_move_constructible<T>::value, "T not noexcept moveable");`——编译时验证。

### §3 内存优化——缓存、对齐与分配器

**缓存友好的数据结构**：

```cpp
// 🔴 缓存不友好——两个数组，遍历需要来回跳
struct Particles {
    std::vector<float> x, y, z;  // 三个独立分配
};
// 遍历：读 x[0]→读 y[0]→读 z[0]→x[1]→y[1]→z[1]——在三个内存区域间跳

// ✓ 缓存友好——AoS→SoA 转换
struct Particle { float x, y, z; };
std::vector<Particle> particles;  // xyz 连续存储
// 遍历：读 p[0].x→p[0].y→p[0].z→p[1].x——数据在连续内存中——CPU cache line 一次抓取
```

- **AoS vs SoA**：Array of Structs（结构体数组）vs Struct of Arrays（数组结构体）。粒子系统用 AoS、矩阵用 SoA——取决于你的访问模式是"逐个对象的所有字段"还是"所有对象的同一个字段"。
- **cache line 大小**：x86 64 字节——如果你的热数据在 64 字节内，一次内存访问带到 cache——零额外开销。

**内存对齐与 false sharing**：

```cpp
// 🔴 false sharing——两个 int 在同一个 cache line 里
struct Counters {
    int produced;  // 线程 A 写
    int consumed;  // 线程 B 写
};
// A 写 produced → B 的 cache line 被 invalidate → B 写 consumed 需要重新加载 → A 的又失效...
// 两个不相关的变量互相拖慢——因为它们在同一个 cache line 里

// ✓ 对齐隔离——每个字段占满一个 cache line
struct alignas(64) Counters {
    int produced;          // 4 字节 + 60 字节 padding ——独享一个 cache line
    alignas(64) int consumed;  // 同上
};
// C++17 有 std::hardware_destructive_interference_size——C++11 用 alignas(64)
```

**自定义分配器**：默认的 `new`/`delete` 是通用分配——没有为你的使用模式优化。自定义分配器可以：
- 内存池：预分配一大块→你需要时切一块→大量小对象分配 O(1)
- arena 分配：按生命周期分配——一批对象释放时一次 `free` 整块，不是每个对象单独 `delete`

本节不讲完整的 C++ allocator 接口（`<memory>` 的 `allocator_traits` 细节）——只讲"为什么需要"和"带来多少收益"。

### §4 用 `perf` 找热点——不要猜，要量

```bash
# 编译——保留符号 + frame pointer
g++ -std=c++11 -O2 -g -fno-omit-frame-pointer bench.cpp -o bench

# 采样——记录每 1000 个 CPU 周期程序在哪
perf record -g ./bench

# 看报告——哪个函数花了最多 CPU 时间
perf report
```

- **`perf top` vs `perf record`**：`top` 实时看、"record + report` 运行完后看完整报告。
- **热点分析**：`perf report` 显示每个函数的 CPU 占比——你不需要猜"这里可能慢"，perf 直接告诉你。
- **cache miss 分析**：`perf stat -e cache-misses,cache-references ./bench`——如果 cache miss 率 > 5-10%，数据结构需要重新设计（§3 的缓存友好布局）。
- **分支预测**：`perf stat -e branch-misses ./bench`——高的分支 miss 率暗示 `if` 条件不够可预测——考虑用查表或位运算替代。

### §5 编译优化——`-O2` vs `-O3` 的实际影响

编译器的优化级别不是"越高越好"：

| 优化级别 | 做了什么 | 适用场景 | 注意事项 |
|---|---|---|---|
| `-O0` | 无优化 | 开发/调试 | 代码直译，无内联 |
| `-O1` | 基本优化 | 调试中的性能检查 | 内联简单函数 |
| `-O2` | **生产标准** | 绝大多数场景 | 速度与体积平衡——不展开太大的内联 |
| `-O3` | 激进优化 | 计算密集型 | 积极内联、向量化——可能增大二进制 |

- **`-O2` 是生产默认**——它包含了最重要的优化（内联、常量传播、死代码消除）而不像 `-O3` 那样疯狂展开内联。
- **`-O3` 的陷阱**：更积极的内联可能让代码体积暴涨 → instruction cache miss 增加 → 反而比 `-O2` 慢。唯一判断方法——benchmark。
- **`-march=native`**：让编译器利用当前 CPU 的所有指令集（SSE/AVX）——在 CI 机器上编译的二进制部署到老 CPU 上可能 SIGILL。

---

## 3. 编写方针

1. **每节必须有可量化的数据**——benchmark 不是"感觉快了"，是"慢 3.2 倍"、"cache miss 降低了 80%"
2. **perf 的实际运行说明**——不止讲"perf 是什么"，给出一个完整的工作流：编译→采样→看报告→改代码→再测
3. **§1-§2 是前面知识的性能兑现**——emplace 是 Stage1 Ch03 移动语义的兑现、noexcept 是 Stage3 Ch03 异常安全的兑现
4. 本章不讲的内容：完全的内存池实现（太长）、`-flto`/`-fprofile-generate`（工具教程）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：emplace_back vs push_back

**面试官**：`emplace_back` 和 `push_back` 的区别？为什么 `emplace_back` 更快？

**回答**：`push_back`——先构造临时对象→拷贝/移动到容器→临时对象析构（三步）。`emplace_back`——把构造参数原地转发给容器→在容器内部一次构造完成（一步）。省了临时对象的构造+析构、省了一次额外的移动/拷贝。对大对象（`string`/`vector`——有堆内存分配）收益更大——每次省一次 `new`+`delete`。

**追问（面试官）**：`push_back` 传右值和 `emplace_back` 有什么区别？

**追问回答**：`v.push_back(std::move(obj))`——移动进容器（一次移动构造）→源对象变成空壳。`v.emplace_back(std::move(obj))`——用移动构造在原地构造（效果和 push_back 移动版本一样）。两者在这个场景下性能等价——都是移动。但如果 `emplace_back` 直接传构造参数（如 `v.emplace_back(5, 'x')`）→比 `push_back(string(5, 'x'))` 少一次临时对象。结论——`emplace_back` 传构造参数才有性能优势，传现成对象和 `push_back` 一样。

### 面试题 2：缓存优化

**面试官**：什么是 false sharing？怎么用 C++11 解决？

**回答**：两个线程访问不同变量——但它们在同一个 cache line 里（64 字节）。当一个线程写入→CPU 将该 cache line 标记为"脏"→另一个 CPU 的 cache line 被 invalidate→下次读取需要重新加载。两个不相关的变量互相拖慢。C++11 解决——`struct alignas(64) { int a; }; struct alignas(64) { int b; };`——或 C++17 的 `hardware_destructive_interference_size`。

**追问（面试官）**：AoS 和 SoA 的区别？什么时候用哪种？

**追问回答**：AoS（Array of Structs）——`struct Particle {float x,y,z;}; vector<Particle>;`——遍历一个粒子的所有字段缓存友好。SoA（Struct of Arrays）——`struct Particles {vector<float> x,y,z;};`——对所有粒子的同一个字段做运算缓存友好。粒子物理→AoS（逐粒子更新坐标+速度）；矩阵乘法→SoA（逐分量运算）。

### 面试题 3：编译器优化级别的选择

**面试官**：`-O2` 和 `-O3` 的区别？为什么生产代码用 `-O2` 而不是 `-O3`？

**回答**：`-O2`——标准优化（内联/常量传播/死代码消除/循环优化），速度和二进制体积的平衡。`-O3`——更激进的内联、向量化、循环展开——可能让二进制体积暴涨 20-50%→I-cache miss 增加→有时比 `-O2` 慢。而且 `-O3` 更长的编译时间 + 更难调试（优化太激进，debug 信息不准）。生产默认 `-O2`，`-O3` 只在有 benchmark 证明收益时对特定文件启用。

**追问（面试官）**：`-march=native` 有什么风险？

**追问回答**：编译器利用当前 CPU 的所有指令集（如 AVX-512）。二进制在开发机上跑快——但部署到老 CPU 或云上不同的实例类型→缺少对应指令→`SIGILL`（非法指令）直接崩溃。生产环境的 CI 机器应该用目标部署平台的最低 CPU 指令集编译——或 `-march=x86-64-v2`（指定最低要求）。

---

## 5. 可运行错误实验

### 实验：noexcept 对 vector 扩容的影响——benchmark

```cpp
#include <vector>
#include <chrono>
#include <iostream>

struct NoExcept {
    char data[256];                           // 256 字节——足够大
    NoExcept() {}
    NoExcept(NoExcept&&) noexcept {}          // ✓ noexcept
};
struct Throwing {
    char data[256];
    Throwing() {}
    Throwing(Throwing&&) {}                   // ✗ 没有 noexcept
};

template<typename T>
double bench() {
    std::vector<T> v;
    auto start = std::chrono::steady_clock::now();
    for (int i = 0; i < 100000; i++) {
        v.push_back(T{});   // 触发多次扩容
    }
    auto end = std::chrono::steady_clock::now();
    return std::chrono::duration<double, std::milli>(end - start).count();
}

int main() {
    std::cout << "noexcept: " << bench<NoExcept>() << " ms\n";
    std::cout << "throwing: " << bench<Throwing>() << " ms\n";
    // 预期：noexcept 快 3-5 倍（扩容走移动 vs 走拷贝）
    return 0;
}
```

**运行**：`g++ -std=c++11 -O2 noexcept_bench.cpp && ./a.out`

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | `emplace_back` 的完美转发实现（参数转发不产生临时对象）；false sharing 的 MESI 协议层机制；`-O2` vs `-O3` 内联策略的具体区别；自定义 allocator 省去每次 malloc/free 系统调用的原理 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（emplace vs push / 缓存优化 false sharing+AoS/SoA / 编译优化 O2 vs O3+march），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | noexcept vector 扩容 benchmark 实验；false sharing 的隐蔽性能退化；`-march=native` 在不同 CPU 上的 SIGILL 风险 | 8/10 |
| 交叉引用 | §1 emplace → Stage1 Ch03 移动语义；§2 noexcept → Stage3 Ch03 异常安全；§3 cache line → C Ch07 内存对齐；§4 perf → C Stage5 GDB/ASan（工具沉淀） | 8/10 |
| **总分** | | **33/40** |
