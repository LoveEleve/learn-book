# RAII 信号与 Socket

> **前置依赖**：读者已完成 C 系统编程（`sigaction`/socket/bind/listen/accept/Unix domain socket），理解进程间通信和网络编程的基本模式。
> **主线版本**：C++11
> **参考书**：C++程序设计语言 §43

---

## 1. 引言：C 的 socket 有多少手动管理

C 里写一个 TCP 服务器——`socket`/`bind`/`listen`/`accept` 每个都是独立函数调用，每条路径都要手动管理：

```cpp
// C 的 TCP 服务器——每个资源都要手动清理
int listen_fd = socket(AF_INET, SOCK_STREAM, 0);
if (listen_fd < 0) return -1;

struct sockaddr_in addr = {};
addr.sin_family = AF_INET;
addr.sin_port = htons(8080);
addr.sin_addr.s_addr = INADDR_ANY;

if (bind(listen_fd, (sockaddr*)&addr, sizeof(addr)) < 0) { close(listen_fd); return -1; }
if (listen(listen_fd, 10) < 0) { close(listen_fd); return -1; }

while (true) {
    int conn = accept(listen_fd, NULL, NULL);
    if (conn < 0) continue;

    // 处理连接...
    char buf[256];
    ssize_t n = read(conn, buf, sizeof(buf));
    write(conn, "OK", 2);
    close(conn);  // 别忘了！
}
close(listen_fd);
```

**资源管理问题**：每个 `socket` 后要配 `close`、每条错误路径都要记得 `close`、客户端连接 `conn` 也要单独 `close`。C++ 用 RAII 消灭这一切——离开作用域自动释放。

---

## 2. RAII 文件描述符——所有系统资源的统一包装

### 2.1 `Fd`——最基础的 RAII 包装

```cpp
#include <unistd.h>
#include <stdexcept>

class Fd {
    int fd_ = -1;

public:
    explicit Fd(int fd = -1) : fd_(fd) {}

    ~Fd() {
        if (fd_ >= 0) close(fd_);
    }

    Fd(const Fd&) = delete;
    Fd& operator=(const Fd&) = delete;

    Fd(Fd&& other) noexcept : fd_(other.fd_) { other.fd_ = -1; }

    int get() const { return fd_; }
    bool valid() const { return fd_ >= 0; }

    int release() {    // 把 fd 转移出去——调用方接管
        int tmp = fd_;
        fd_ = -1;
        return tmp;
    }
};
```

**这是所有 socket/IPC RAII 的基础**——`Fd` 只管一件事：构造时拿 fd、析构时 close。所有的 socket/pipe/mmap 包装都可以用 `Fd` 作为成员。

### 2.2 异常安全——Socket 创建

```cpp
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

class Socket : public Fd {
public:
    Socket() : Fd(socket(AF_INET, SOCK_STREAM, 0)) {
        if (!valid()) throw std::runtime_error("socket failed");
    }

    // TCP 绑定+监听——两个操作一起包装
    void bind_listen(uint16_t port, int backlog = 10) {
        struct sockaddr_in addr = {};
        addr.sin_family = AF_INET;
        addr.sin_port = htons(port);
        addr.sin_addr.s_addr = INADDR_ANY;

        if (bind(get(), (sockaddr*)&addr, sizeof(addr)) < 0)
            throw std::runtime_error("bind failed");
        if (listen(get(), backlog) < 0)
            throw std::runtime_error("listen failed");
    }

    // 接受连接——返回一个新的 Fd
    Fd accept() {
        int conn = ::accept(get(), NULL, NULL);
        if (conn < 0) throw std::runtime_error("accept failed");
        return Fd(conn);
    }
};
```

---

## 3. RAII 信号处理——`sigaction` 的封装

### 3.1 C 的 `sigaction`——需要手动设置和恢复

```cpp
// C 的模式——设置 handler，进程退出前必须恢复（否则可能误调用）
struct sigaction sa;
sa.sa_handler = my_handler;
sigemptyset(&sa.sa_mask);
sa.sa_flags = 0;
sigaction(SIGINT, &sa, NULL);  // 设置

// ... 运行中 ...

// 恢复默认 handler——如果忘了，下次 SIGINT 可能调用已销毁的函数
signal(SIGINT, SIG_DFL);
```

### 3.2 C++ RAII——离开作用域自动恢复

```cpp
#include <csignal>
#include <iostream>

class SignalHandler {
    struct sigaction old_action_;
    int signum_;

public:
    SignalHandler(int signum, void (*handler)(int)) : signum_(signum) {
        struct sigaction sa;
        sa.sa_handler = handler;
        sigemptyset(&sa.sa_mask);
        sa.sa_flags = 0;
        sigaction(signum_, &sa, &old_action_);  // 保存旧的
    }

    ~SignalHandler() {
        sigaction(signum_, &old_action_, NULL);  // 恢复旧的
    }

    SignalHandler(const SignalHandler&) = delete;
    SignalHandler& operator=(const SignalHandler&) = delete;
};

void handle_sigint(int) {
    std::cout << "Ctrl+C caught!\n";
}

int main() {
    SignalHandler sigint_guard(SIGINT, handle_sigint);
    // ... 运行中——SIGINT 会触发 handler ...
    // sigint_guard 析构——恢复默认 handler
    return 0;
}
```

**设计要点**：`SignalHandler` 在构造时保存旧的 `sigaction`，析构时恢复。如果函数中提前 `return`（或异常抛出）——析构自动恢复。这和 C 的"手动恢复"形成对比。

---

## 4. TCP 服务器——RAII 完整封装

### 4.1 完整的服务器框架

```cpp
#include <thread>
#include <vector>
#include <iostream>

void handle_connection(Fd conn) {
    char buf[256];
    ssize_t n = read(conn.get(), buf, sizeof(buf));
    if (n > 0) {
        std::cout << "Received: " << std::string(buf, n) << '\n';
        write(conn.get(), "OK\n", 3);
    }
    // conn 析构——自动 close
}

int main() {
    Socket server;
    server.bind_listen(8080);

    std::cout << "Listening on :8080\n";

    std::vector<std::thread> threads;
    while (true) {
        Fd conn = server.accept();              // RAII 管理连接 fd
        threads.emplace_back(handle_connection, std::move(conn));
        // 或者 std::thread(...).detach()——用 RAII 的 conn 在线程内析构
    }
    // server 析构——自动 close(listen_fd)
    return 0;
}
```

**线程传递 `Fd`**：`Fd conn = server.accept()` → `std::move(conn)` 把所有权移交给线程。`conn` 在新线程的 `handle_connection` 中析构时自动 `close(conn)`。旧线程不再需要手动清理。

---

## 5. Unix Domain Socket——进程间通信的轻量方式

### 5.1 和 TCP 的区别

Unix domain socket 走文件系统路径（不是 IP+端口）——同一台机器上的两个进程通过文件通信。比 TCP 快（不走网络栈）且支持文件描述符传递（`SCM_RIGHTS`）。

```cpp
#include <sys/socket.h>
#include <sys/un.h>

class UnixSocket : public Fd {
public:
    UnixSocket() : Fd(socket(AF_UNIX, SOCK_STREAM, 0)) {
        if (!valid()) throw std::runtime_error("unix socket failed");
    }

    // 服务端——绑定路径 + 监听
    void bind_listen(const char* path) {
        struct sockaddr_un addr = {};
        addr.sun_family = AF_UNIX;
        strncpy(addr.sun_path, path, sizeof(addr.sun_path) - 1);

        // 先删除可能已存在的 socket 文件
        unlink(path);

        if (bind(get(), (sockaddr*)&addr, sizeof(addr)) < 0)
            throw std::runtime_error("unix bind failed");
        if (listen(get(), 10) < 0)
            throw std::runtime_error("unix listen failed");
    }

    // 客户端——连接
    void connect_to(const char* path) {
        struct sockaddr_un addr = {};
        addr.sun_family = AF_UNIX;
        strncpy(addr.sun_path, path, sizeof(addr.sun_path) - 1);

        if (connect(get(), (sockaddr*)&addr, sizeof(addr)) < 0)
            throw std::runtime_error("unix connect failed");
    }

    Fd accept() {
        int conn = ::accept(get(), NULL, NULL);
        if (conn < 0) throw std::runtime_error("unix accept failed");
        return Fd(conn);
    }
};
```

### 5.2 使用——比 TCP 更轻

```cpp
// 服务端
UnixSocket server;
server.bind_listen("/tmp/my_socket");
while (true) {
    Fd conn = server.accept();
    // 处理...
}

// 客户端
UnixSocket client;
client.connect_to("/tmp/my_socket");
write(client.get(), "hello", 5);
```

**Unix domain socket vs TCP 对比**：

| | Unix Socket | TCP Socket |
|---|---|---|
| 速度 | 快——不走网络栈 | 慢——协议开销 |
| 地址 | 文件路径 `/tmp/xxx.sock` | IP + 端口 |
| 文件权限 | 有——可以控制谁连 | 无 |
| 传 fd | 可以（`SCM_RIGHTS`） | 不能 |
| 适用 | 同机进程间 | 跨机器网络 |

---

## 6. 可运行实验

### 实验：RAII Socket 服务器——一行关闭所有资源

```cpp
#include <iostream>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>

class Fd {
    int fd_;
public:
    explicit Fd(int fd = -1) : fd_(fd) {}
    ~Fd() { if (fd_ >= 0) close(fd_); }
    Fd(const Fd&) = delete;
    Fd& operator=(const Fd&) = delete;
    Fd(Fd&& o) noexcept : fd_(o.fd_) { o.fd_ = -1; }
    int get() const { return fd_; }
    bool valid() const { return fd_ >= 0; }
};

class Socket : public Fd {
public:
    Socket() : Fd(socket(AF_INET, SOCK_STREAM, 0)) {
        if (!valid()) throw std::runtime_error("socket");
    }
    void bind_listen(uint16_t port) {
        sockaddr_in addr = {};
        addr.sin_family = AF_INET;
        addr.sin_port = htons(port);
        addr.sin_addr.s_addr = INADDR_ANY;
        if (bind(get(), (sockaddr*)&addr, sizeof(addr)) < 0) throw std::runtime_error("bind");
        if (listen(get(), 10) < 0) throw std::runtime_error("listen");
    }
    Fd accept() {
        int conn = ::accept(get(), NULL, NULL);
        if (conn < 0) throw std::runtime_error("accept");
        return Fd(conn);
    }
};

int main() {
    Socket server;
    server.bind_listen(8080);
    std::cout << "Listening on :8080 (try: nc localhost 8080)\n";

    while (true) {
        Fd conn = server.accept();
        char buf[256];
        ssize_t n = read(conn.get(), buf, sizeof(buf));
        if (n > 0) {
            write(conn.get(), "ACK\n", 4);
        }
    }  // conn/server 析构自动 close
}
```

**编译运行**：`g++ -std=c++11 socket_server.cpp -o server && ./server`，另开终端 `nc localhost 8080` 测试。

---

## 7. 面试题

### 面试题 1：RAII 文件描述符

**面试官**：设计一个 RAII 管理文件描述符的类——必须做什么？

**回答**：(1) 构造时接收 fd——`Fd(int fd)`；(2) 析构时 `close`——如果 `fd >= 0`；(3) **禁止拷贝**——两个对象共享同一个 fd→double close；(4) **允许移动**——转移所有权——新对象管 fd、旧对象置为 -1；(5) `release()`——把 fd 移出 RAII 管理（交给调用方手动管理，如传给 `close()` 回调）。这五点缺一不可——少了第 3 点就是 double-free 隐患。

**追问（面试官）**：Socket 服务器中 `accept()` 返回的连接怎么传参？

**追问回答**：`Fd conn = server.accept()`——`Fd` 的析构会在 `handle_connection` 函数结束时自动 `close`。传给线程时用 `std::move(conn)`——移动语义把所有权转给新线程，旧线程不再需要手动清理。多个线程共享同一个连接时需要 `shared_ptr<Fd>`——但通常一个连接只属于一个线程。

### 面试题 2：信号处理

**面试官**：C++ 中处理信号有哪些坑？

**回答**：(1) 信号处理器里只能调用**异步信号安全**的函数（`write`、`read`、`_exit`）——不能 `malloc`/`free`/`printf`/`new`/`delete`——这些可能持有锁，信号在持锁时触发→死锁。(2) 信号处理器里不能访问非 `volatile` 变量——编译器可能优化掉。(3) `sig_atomic_t` 是唯一可以安全读写的类型。(4) RAII 封装——`SignalHandler` 构造时保存旧 handler、析构时恢复——防止误调已销毁的函数。

**追问（面试官）**：`sig_atomic_t` 和 `std::atomic` 的区别？

**追问回答**：`sig_atomic_t` 是 C 标准——保证在信号处理器和主程序之间原子读写（不可分割）。`std::atomic` 是 C++11——保证多线程间的原子操作+内存序（happens-before）。信号处理器里用 `sig_atomic_t`——因为信号是异步事件，不是多线程同步。`std::atomic` 在多线程场景使用。

### 面试题 3：Unix Domain Socket vs TCP

**面试官**：Unix domain socket 和 TCP socket 的区别？什么时候用 Unix？

**回答**：Unix domain socket 走文件系统路径——不走网络协议栈——同一台机器上的 IPC。优势：(1) 快——无 TCP 握手/确认/序列化开销；(2) 文件权限控制——socket 文件的属主决定谁可以连；(3) 可以传递文件描述符（`SCM_RIGHTS`——一个进程把 fd 传给另一个进程，如 Web 服务器把工作连接传给 worker）。缺点——只能同机，不能跨网络。什么时候用 Unix——同机进程间通信（如 Docker daemon 的 `/var/run/docker.sock`）。什么时候用 TCP——跨机器网络通信。

---

## 8. 本章小结

| 概念 | C 的做法 | C++ 的做法 | 核心收益 |
|---|---|---|---|
| 文件描述符 | 手动 close，错误路径易忘 | `Fd` RAII——析构自动 close | 异常安全 |
| socket 服务器 | `socket`/`bind`/`listen`/`accept` 逐个调用 | `Socket` RAII——构造+绑定+监听一体 | 一行启动、零手动清理 |
| 信号处理 | `sigaction` 设置 + 手动恢复 | `SignalHandler` RAII——析构恢复旧 handler | 函数提前 return 也不留坑 |
| Unix socket | `sockaddr_un` + 手动 `unlink` | `UnixSocket` RAII——封装 bind/listen/connect | 和 TCP 一样的编程模式 |
| fd 传递 | `SCM_RIGHTS` 手动编码 | 通过 Unix socket RAII 传递 | 进程间共享资源 |

**Stage5 系统编程进度**：Ch02 文件 I/O ✅ → Ch03 内存映射 ✅ → Ch04 进程管理 ✅ → Ch05 信号与 Socket ✅ → 下一章——跨语言 FFI：`extern "C"` 深度、C++→C wrapper 完整封装、共享库 ABI。
