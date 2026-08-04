# Stage5 Ch03 规划：C++ 工程化实战

> **定位**：Stage5 第三章。前提——读者有 CMake 基础（C Stage5 已讲过），能编译单文件/多文件 C++ 项目。
> **主线版本**：C++11 + CMake 3.10+
> **参考书**：C++编程之禅，C++调试的艺术

---

## 1. 本章要解决的核心问题

前 17 章教你写了"正确的 C++ 代码"。但一个生产级项目不只是代码——你的 `g++ -std=c++11 main.cpp` 只有你一个人能编译。团队需要 CMake 统一构建、Google Test 保证每次改代码不改逻辑、clang-tidy 在编译时发现潜在 bug。

本章教你"让 C++ 项目像一个真正的工程"——从"单文件 g++ 编译"到"CMake + GTest + clang-tidy + CI"。本章技术深度天然低于 C++ 机制章（不是语言特性，是工程工具），但生产陷阱和面试覆盖不缩水。

---

## 2. 节结构（4 节）

### §1 CMake 现代化——target 体系

C Stage5 教了 CMake 基础。C++ 项目需要更结构化的 CMake——modern CMake 的 target 体系：

```cmake
cmake_minimum_required(VERSION 3.10)
project(myapp LANGUAGES CXX)

add_library(core STATIC src/core/engine.cpp src/core/config.cpp)
target_include_directories(core PUBLIC include)
target_compile_features(core PUBLIC cxx_std_11)      # 要求 C++11
target_compile_options(core PRIVATE -Wall -Wextra)

add_executable(myapp src/main.cpp)
target_link_libraries(myapp PRIVATE core)
```

- **target 体系 vs 全局变量**：`target_xxx` 比 `set(CMAKE_CXX_FLAGS ...)` 更好——每个 target 独立控制选项。把 `-Wall` 加给 core 不影响第三方库（如 GTest）的编译。
- **`PUBLIC`/`PRIVATE`/`INTERFACE`**：PUBLIC=使用者也需要（头文件路径）；PRIVATE=只有自己需要（编译警告）；INTERFACE=自己不需要但使用者需要（header-only 库）。
- **`cxx_std_11` 的生产意义**：不加这个→编译器用默认 C++ 标准（GCC 默认 gnu++17、MSVC 默认 C++14）→代码在不同平台上用不同标准编译→"在我机器上能编译但 CI 上不行"。
- **和 C CMake 的差异**：C++ 项目必须管理头文件依赖（`target_include_directories`）、C++11 标准版本（`cxx_std_11`）、跨平台差异（MSVC 的 `/W4` vs GCC 的 `-Wall`）。

### §2 Google Test——单元测试

```cpp
#include <gtest/gtest.h>
int add(int a, int b) { return a + b; }

TEST(AddTest, Positive) { EXPECT_EQ(add(2, 3), 5); }
TEST(AddTest, Negative) { EXPECT_EQ(add(-2, -3), -5); }
```

- **`EXPECT_EQ` vs `ASSERT_EQ`**：EXPECT 失败继续跑后续断言；ASSERT 失败立即终止测试。
- **测试 fixture——共享初始化**：
```cpp
class QueueTest : public ::testing::Test {
protected:
    std::queue<int> q;
    void SetUp() override { q.push(1); q.push(2); }
};
TEST_F(QueueTest, Pop) { EXPECT_EQ(q.front(), 1); q.pop(); }
TEST_F(QueueTest, Size) { EXPECT_EQ(q.size(), 2); }  // 每个 TEST_F 有全新 fixture
```
- **GTest + CMake**：`find_package(GTest REQUIRED)` + `target_link_libraries(test GTest::GTest)` + `add_test(NAME mytest COMMAND test_exe)`。

### §3 静态分析——clang-tidy

```bash
clang-tidy src/*.cpp -- -std=c++11
# warning: use 'nullptr' instead of NULL
# warning: use 'override' on virtual function
```

- **clang-tidy 核心检查类别**：`modernize-*`（C++11 替代旧写法）、`performance-*`（参数应传引用而非值）、`readability-*`（可读性）。CMake 集成：`set(CMAKE_CXX_CLANG_TIDY "clang-tidy;-checks=modernize-*,performance-*")`。
- **cppcheck**：不需要编译的纯源码分析——和 clang-tidy 互补。
- **CI 集成**：和 test 并排跑——lint 失败阻塞合并，但不阻塞 test 执行（两者独立）。

### §4 CI 配置——GitHub Actions 模板

```yaml
name: C++ CI
on: [push, pull_request]
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install deps
        run: sudo apt-get install -y g++ libgtest-dev clang-tidy
      - name: Configure
        run: cmake -B build -DCMAKE_BUILD_TYPE=Release
      - name: Build
        run: cmake --build build -- -j$(nproc)
      - name: Test
        run: cd build && ctest --output-on-failure
      - name: Lint
        run: clang-tidy src/*.cpp -- -std=c++11
```

**步骤顺序的设计理由**：Configure→Build→Test+Lint 并排。Build 失败时不需要跑 Test；Test 和 Lint 互不依赖——它们可以并排跑（在 CI 里分两个 job 更快）。

---

## 3. 编写方针

1. 不是 CMake/GTest/clang-tidy 手册——每个工具讲"为什么需要"+"最常用的 3 个操作"
2. §1 CMakeLists.txt 完整可编译、§2 GTest 完整可跑
3. 本章不讲的内容：Conan/vcpkg（太平台特定）、Coverage（`gcov`）

---

## 4. 面试题（2 组，每组含答案+追问）

### 面试题 1：CMake target 体系

**面试官**：`target_compile_options` 和 `set(CMAKE_CXX_FLAGS)` 的区别？为什么 modern CMake 推荐前者？

**回答**：`set(CMAKE_CXX_FLAGS)` 是全局变量——影响整个项目中所有 target（包括你 include 的第三方库）。`target_compile_options(core PRIVATE -Wall)` 只影响 core 这一个 target——你的 GTest 依赖不会被 `-Wall` 污染（第三方库有自己的警告设置）。这就是 target 体系的核心——每个 target 独立管理自己的编译属性。

**追问**：PUBLIC/PRIVATE/INTERFACE 的区别？什么时候用 INTERFACE？

**追问回答**：PUBLIC——core 自己和依赖 core 的人都获得（如头文件路径）。PRIVATE——只有 core 自己获得（如编译警告）。INTERFACE——core 自己不需要、但依赖 core 的人需要（header-only 库的头文件路径）。典型场景——`target_include_directories(header-only-lib INTERFACE include)`——头文件库本身不需要编译，但使用者需要 `include` 路径。

### 面试题 2：GTest 测试组织

**面试官**：GTest 的 `TEST` 和 `TEST_F` 有什么区别？什么时候用 fixture？

**追问回答**：`TEST`——独立的测试用例，不需要共享初始化。`TEST_F`——需要用 fixture（一组测试共享同一个初始状态）。fixture 的 SetUp 在每个 `TEST_F` 执行前调用（保证每个测试有全新状态），TearDown 在每个测试完成后清理。用于需要昂贵初始化（数据库连接/大对象构造）的场景。

**追问**：GTest 的全局变量陷阱是什么？

**追问回答**：如果在 fixture 外定义了全局变量（如 `int global_counter = 0;`），多个测试用例共享同一个全局变量——测试的执行顺序不确定→一个测试修改了全局变量→另一个测试依赖特定的初始值→不可重现的失败。解决——把状态放进 fixture 成员变量（每个测试有独立副本），或每个测试开头显式初始化全局状态。

---

## 5. 可运行错误实验

### 实验 1：GTest 全局变量污染

```cpp
#include <gtest/gtest.h>

static int global_counter = 0;  // 🔴 全局变量——多个测试共享

TEST(CounterTest, Increment) {
    global_counter++;
    EXPECT_EQ(global_counter, 1);
}
TEST(CounterTest, CheckAgain) {
    EXPECT_EQ(global_counter, 0);  // 🔴 可能失败——Increment 先跑了！
}
// 修复：用 fixture 成员变量替代全局变量
```

### 实验 2：CMake 不设 cxx_std_11 的平台差异

展示不加 `cxx_std_11` 时——GCC 默认 gnu++17 → `auto` 返回类型推导直接生效；MSVC 默认 C++14 → C++11 的返回类型后置 `-> decltype(...)` 也能编译但行为边缘不同。加了 `cxx_std_11` 后两者统一行为。

---

## 6. 4 维度自检

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | target 体系的依赖传播（PUBLIC/PRIVATE/INTERFACE 的头文件+编译选项传播规则）；`cxx_std_11` 跨平台统一标准版本的底层机制（编译器 feature flag 而非仅版本号） | 7/10 |
| 面试覆盖 | 2 组完整面试 Q&A（CMake target 体系 / GTest fixture + 全局变量陷阱），每组含答案 + 二层追问 | 8/10 |
| 生产陷阱 | GTest 全局变量污染可运行实验；CMake 不设 cxx_std_11 的平台差异实验；clang-tidy `modernize-*` 不通过阻塞 CI 合并的工作流 | 8/10 |
| 交叉引用 | §1→C Stage5 CMake 基础（C++ 差异）；§1 cxx_std_11→Stage1 Ch01 C++11 标准；§2 fixture SetUp→Stage1 Ch04 RAII | 7/10 |
| **总分** | | **30/40** |
