# RAII 文件 I/O——从 C 的 `FILE*` 到 C++ 的 `fstream`

> **前置依赖**：读者已完成 C 系统编程（syscall/open/read/write/close/stdio），理解文件描述符和 `FILE*` 的基本用法。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §38-39

---

## 1. 引言：C 文件 I/O 的四个痛点

C 里文件 I/O 有两种选择——系统调用（`open`/`read`/`write`/`close`）和标准库（`fopen`/`fprintf`/`fclose`）。两种都有同样的痛：

```cpp
// C 的模式——每一次 open 必须配 close，每一条错误路径上都不能忘
int fd = open("data.txt", O_RDONLY);
if (fd < 0) { perror("open"); return -1; }

char buf[1024];
ssize_t n = read(fd, buf, sizeof(buf));
if (n < 0) { perror("read"); close(fd); return -1; }  // 别忘了 close！

close(fd);  // 正常路径——也要写

// 三个分支、三处 close、一次忘了就泄漏 fd
```

**四个痛点**：
1. **手动配对**——`open` 和 `close`、`fopen` 和 `fclose` 必须手动配对。每条错误路径都要记得关文件。
2. **无 RAII**——即使 C++ 代码里用了 `FILE*`，异常抛出后文件还是泄漏——因为 `FILE*` 的析构函数什么也不做。
3. **格式化字符串不安全**——`sprintf` 没有缓冲区溢出保护、`fscanf` 没有类型检查。
4. **二进制安全缺失**——`fprintf("%s", data)` 遇到 `\0` 就截断。

C++ 的标准 I/O 流解决全部四个问题。`fstream` 用 RAII 自动关闭文件。`stringstream` 提供类型安全的格式化。本章展示 C++ 怎么让你在文件 I/O 这件事上忘掉 C 的手动资源管理。

---

## 2. `fstream`——RAII 管理的文件描述符

### 2.1 打开和关闭——自动完成

```cpp
#include <fstream>
#include <string>
#include <iostream>

// 写文件——ofstream（output file stream）
{
    std::ofstream out("output.txt");     // 构造——打开文件
    out << "Hello, RAII!\n";             // 写
    out << 42 << '\n';                   // 任何类型都能写——和 cout 一样
}  // out 析构——自动 close，即使抛异常也关

// 读文件——ifstream（input file stream）
{
    std::ifstream in("output.txt");      // 构造——打开文件
    std::string line;
    while (std::getline(in, line)) {     // 逐行读
        std::cout << line << '\n';
    }
}  // in 析构——自动 close
```

**和 C 的对比**——同样的逻辑在 C 里需要 15 行（`fopen` + 错误检查 + `fgets` 循环 + 错误检查 + `fclose`×每个退出路径）。C++ 版本——7 行，零手动资源管理。

### 2.2 打开模式

```cpp
// 默认——ofstream 截断写、ifstream 只读
std::ofstream out("file.txt");                       // 截断——覆盖已有内容

// 追加写——和 C 的 "a" 一样
std::ofstream out("file.txt", std::ios::app);        // 追加——写在末尾

// 二进制模式——和 C 的 "b" 一样
std::ifstream in("data.bin", std::ios::binary);      // 二进制读——不处理 \r\n 转换

// 组合——追加 + 二进制
std::ofstream out("log.bin", std::ios::app | std::ios::binary);

// 读写——fstream（同时可读可写）
std::fstream fs("file.txt", std::ios::in | std::ios::out);
```

**模式标志表**：

| 标志 | C 等价 | 含义 |
|---|---|---|
| `std::ios::in` | `"r"` | 读（ifstream 默认） |
| `std::ios::out` | `"w"` | 截断写（ofstream 默认） |
| `std::ios::app` | `"a"` | 追加写 |
| `std::ios::binary` | `"b"` | 二进制模式 |
| `std::ios::ate` | — | 打开后定位到文件末尾 |
| `std::ios::trunc` | `"w"` | 打开时清空文件（out 默认） |

### 2.3 检查文件是否成功打开

```cpp
std::ifstream in("nonexistent.txt");
if (!in) {                       // ✓ 简洁——operator! 检查 failbit
    std::cerr << "Cannot open file\n";
    return;
}
// 或者显式写法：if (!in.is_open()) { ... }
// 或者：if (in.fail()) { ... }
```

**和 C 的对比**：C 里 `fopen` 返回 `NULL`→你忘了检查→后续 `fread` 访问 `NULL`→UB。C++ 的 `fstream`——即使忘了检查 `!in`，后续的 `>>` 操作**静默失败**（不崩溃，只是读到空字符串或 0）。比 C 安全——不会 UB。

### 2.4 关闭和重新打开

```cpp
std::fstream fs;
fs.open("first.txt", std::ios::out);
fs << "first file\n";
fs.close();                         // 显式关闭——用于检查关闭错误或切换文件

fs.open("second.txt", std::ios::out);
fs << "second file\n";
// fs 析构时自动 close
```

---

## 3. `iostream` 格式化——告别 `sprintf`

### 3.1 基础格式化

```cpp
#include <iomanip>
#include <iostream>

int n = 42;
double pi = 3.1415926535;

// 整数格式化
std::cout << std::dec << n << '\n';  // 42——十进制（默认）
std::cout << std::hex << n << '\n';  // 2a——十六进制
std::cout << std::oct << n << '\n';  // 52——八进制

// 浮点格式化
std::cout << std::fixed << pi << '\n';           // 3.141593——固定小数点（默认 6 位）
std::cout << std::scientific << pi << '\n';       // 3.141593e+00——科学计数法
std::cout << std::setprecision(3) << pi << '\n';  // 3.14——3 位有效数字

// 宽度和填充
std::cout << std::setw(10) << std::setfill('*') << n << '\n';  // "********42"

// 布尔值
std::cout << std::boolalpha << true << '\n';  // "true"——不是 "1"
```

### 3.2 格式化对比——C vs C++

```cpp
// C：sprintf——缓冲区溢出风险 + 类型不安全
char buf[32];
sprintf(buf, "%08x", 255);  // "000000ff"——如果 #08x 写错成别的东西→UB

// C++：stringstream——类型安全 + 无溢出风险
#include <sstream>
std::ostringstream oss;
oss << std::hex << std::setfill('0') << std::setw(8) << 255;
std::string result = oss.str();  // "000000ff"
```

### 3.3 `stringstream`——内存中的"文件"

```cpp
#include <sstream>

// ostringstream——写进字符串
std::ostringstream oss;
oss << "Age: " << 25 << ", Name: " << "Alice";
std::string s = oss.str();  // "Age: 25, Name: Alice"

// istringstream——从字符串读
std::istringstream iss("42 3.14 hello");
int i; double d; std::string w;
iss >> i >> d >> w;  // i=42, d=3.14, w="hello"

// stringstream——读写
std::stringstream ss;
ss << "value: " << 42;
int v; std::string label;
ss >> label >> v;  // label="value:", v=42
// 等价于 C 的 sscanf——但类型安全 + 可双向
```

---

## 4. 错误处理——流状态

### 4.1 四个状态位

每个流对象维护四个状态位：

```cpp
std::ifstream in("data.txt");
if (!in) {
    // in.fail()——逻辑错误（类型不匹配）
    // in.bad()——严重错误（底层读写失败、流缓冲区损坏）
    // in.eof()——到达文件末尾（读完后才设置，不是"下一次会失败"）
}

// fail() vs bad()：
// fail()——可恢复（读到字母但期望数字→忽略该 token 继续）
// bad()——不可恢复（底层 I/O 错误→流已损坏）
// 实际中：!in 覆盖 fail() 和 bad()——大部分场景够用
```

### 4.2 EOF 的正确判断

```cpp
// 🔴 常见的错误——EOF 检测太晚
int x;
while (!in.eof()) {    // eof 在读失败后才设置
    in >> x;            // 读到末尾——x 没更新——最后一行处理两次！
    process(x);
}

// ✓ 正确——把"读"放在条件里
int x;
while (in >> x) {      // >> 成功→继续；失败（包括 EOF）→停止
    process(x);
}
```

**读取整行**：
```cpp
std::string line;
while (std::getline(in, line)) {  // ✓ 读成功→处理；EOF→停止
    process(line);
}
```

---

## 5. 与 C FILE* 的互操作

### 5.1 `c_str()` 打开文件

```cpp
std::string filename = "data.txt";
std::ifstream in(filename.c_str());  // C++11：c_str() 转 const char*
// C++11 也接受 std::string 直接传参：ifstream in(filename);
```

### 5.2 C FILE* → C++ fstream

C++ 标准库没有"从 `FILE*` 创建 `fstream`"的直接方法。如果你已经有一个 `FILE*`（如从 C 库获取）→直接用 C API 操作、或用 `FILE*` 构造一个 RAII 包装器：

```cpp
#include <cstdio>
#include <memory>

// RAII 包装 FILE*
struct FileCloser { void operator()(FILE* f) const { if (f) fclose(f); } };
using FilePtr = std::unique_ptr<FILE, FileCloser>;

FilePtr f(fopen("data.txt", "r"), FileCloser{});
char buf[256];
while (fgets(buf, sizeof(buf), f.get())) {
    process(buf);
}
// f 析构时自动 fclose
```

**关键信息**：C++11 不提供 `fstream` 和 `FILE*` 的直接互转。新代码直接用 `fstream`；老代码的 `FILE*` 用 `unique_ptr` + 自定义 deleter 做 RAII。

---

## 6. 可运行实验

### 实验 1：fstream RAII vs FILE* 泄漏对比

```cpp
#include <fstream>
#include <cstdio>
#include <stdexcept>
#include <iostream>

// C 版本——异常后 FILE* 泄漏
void c_style() {
    FILE* f = fopen("test.txt", "w");  // 打开
    if (!f) return;
    fprintf(f, "hello\n");
    throw std::runtime_error("oops!");  // 🔴 fclose 不会执行——FILE* 泄漏！
    fclose(f);  // 永远不到这里
}

// C++ 版本——异常后 ofstream 自动关闭
void cpp_style() {
    std::ofstream out("test.txt");   // 打开
    out << "hello\n";
    throw std::runtime_error("oops!");  // ✓ out 析构自动 close——零泄漏
}

int main() {
    try { c_style(); } catch (...) {
        std::cout << "C-style: FILE* leaked!\n";
    }
    try { cpp_style(); } catch (...) {
        std::cout << "C++-style: file closed by RAII\n";
    }
    return 0;
}
```

### 实验 2：stringstream vs sprintf 安全对比

```cpp
#include <sstream>
#include <cstdio>
#include <string>
#include <iostream>

int main() {
    // C——缓冲区溢出
    char buf[8];
    sprintf(buf, "%d", 1234567890);  // 🔴 10 位数字 + '\0' = 11 字节→溢出 8 字节 buf！

    // C++——零溢出风险
    std::ostringstream oss;
    oss << 1234567890;
    std::string result = oss.str();  // ✓ 自动扩容——不会溢出
    std::cout << result << '\n';

    // C——类型不安全
    sprintf(buf, "%s", 42);  // 🔴 %s 期望 char* 但传入 int→UB

    // C++——类型安全
    oss.str(""); oss.clear();  // 清空 stream
    oss << 42;                  // ✓ 编译时类型检查——42 正确输出为 "42"
    std::cout << oss.str() << '\n';
    return 0;
}
```

---

## 7. 面试题

### 面试题 1：fstream vs FILE*

**面试官**：`fstream` 和 C 的 `FILE*` 有什么区别？什么时候用哪个？

**回答**：`fstream` ——RAII 管理——构造打开、析构自动 close，即使异常抛出也关闭。`FILE*` ——手动 `fopen`/`fclose` 配对——忘了 close 在任何错误路径上都泄漏。`fstream` 通过 `operator<<`/`>>` 支持类型安全的格式化——直接 `out << 42`；`FILE*` 通过 `fprintf`/`fscanf`——格式串错误是 UB。结论——新代码用 `fstream`；老 C 库返回的 `FILE*` 用 `unique_ptr` + 自定义 `fclose` deleter 做 RAII。

**追问（面试官）**：`fstream` 怎么检查文件是否成功打开？

**追问回答**：`if (!in)` 检查 failbit——等价于 `if (in.fail())`。也可以 `if (!in.is_open())`。和 C 的 `fopen` 返回 NULL 不同——C++ 的 `fstream` 即使打开失败也不会是 NULL（是构造好的对象，内部状态标记了失败），所以不能和 `nullptr` 比较。

### 面试题 2：EOF 的正确检测

**面试官**：为什么 `while (!file.eof())` 是错的？

**回答**：`eofbit` 在"尝试读但读到末尾"之后才设置——不是在"下一个字符是 EOF"之前。用 `while (!eof())` 意味着——读完最后一行后，eof 还没设置→循环再跑一次→读失败→eof 才设置——但最后一行已经被处理了两次。正确写法——`while (in >> x)` 或 `while (getline(in, line))`——把"读"操作放在条件里，读成功→处理、读失败（包括 EOF）→停止。

**追问（面试官）**：`fail()` 和 `bad()` 的区别？

**追问回答**：`fail()` ——逻辑错误（期望数字却读到字母、格式不匹配）——可恢复——调用 `clear()` 后可以继续读。`bad()` ——严重错误（底层读写失败、流缓冲区损坏）——不可恢复——流已损坏，不能用。两种都导致 `!in` 为 true。

### 面试题 3：stringstream 的应用

**面试官**：`stringstream` 比 C 的 `sprintf`/`sscanf` 好在哪里？

**回答**：(1) 类型安全——`oss << 42` 编译时就知道 42 是 int→输出 "42"；`sprintf(buf, "%s", 42)` 格式串写错→UB。(2) 无缓冲区溢出——`stringstream` 内部用 `std::string` 自动扩容；`sprintf` 写固定大小 char 数组→溢出是 UB。(3) 可双向——同一个 `stringstream` 可以先写后读（`ss << data; ss >> result;`）——`sprintf` 只能写、`sscanf` 只能读。(4) 和 STL 生态一致——`stringstream` 可以传给任何接受 `istream&`/`ostream&` 的函数——"内存中的文件"。

---

## 8. 本章小结

| 概念 | C 的做法 | C++ 的做法 | 核心收益 |
|---|---|---|---|
| 文件打开/关闭 | `fopen`/`fclose` 手动配对 | `fstream` RAII | 异常安全——抛出异常也关文件 |
| 格式化写 | `fprintf` | `operator<<` + `setw`/`hex` | 类型安全——编译时检查 |
| 格式化读 | `fscanf` | `operator>>` | 无格式串错误→UB |
| EOF 检测 | `feof(fp)` | `while (in >> x)` | 最后一字节不重复处理 |
| 内存格式化 | `sprintf`/`sscanf` | `stringstream` | 无缓冲区溢出 + 双向读写 |
| C FILE* 包装 | — | `unique_ptr<FILE, FileCloser>` | RAII 管理现有 C 文件句柄 |

**本章是 Stage5 系统编程的第一章。** 下一章——RAII 内存映射与共享内存：`mmap` RAII 封装、`madvise`、内存池实战。
