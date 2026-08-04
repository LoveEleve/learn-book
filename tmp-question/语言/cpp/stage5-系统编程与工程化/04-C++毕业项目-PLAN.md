# Stage5 Ch04 规划：C++ 毕业项目

> **定位**：Stage5 第四章（20 章最后一章）。前提——读者已完成全部 19 章内容。
> **项目类型**：从零写一个 C++ 网络服务——KV 存储 或 HTTP 服务器。
> **主线版本**：C++11

---

## 1. 本章要解决的核心问题

前 19 章教你 C++ 的各个子系统——类/RAII/智能指针/模板/STL/虚函数/并发/异常安全/性能优化。本章是**把这些碎片拼成一个完整的 C++ 项目**——从设计到实现到测试。

毕业项目不是"再学一个新知识点"——是"用你已有的 19 章知识完成一个完整的软件"。

---

## 2. 项目结构（KV 存储——推荐方案）

KV 存储是一个网络服务——客户端通过 socket 发送 `GET key`/`SET key value`/`DEL key` 命令，服务端回复结果。技术栈覆盖 Stage1-5 全部知识点：

| 组件 | 使用的 C++ 知识 | 对应章节 |
|---|---|---|
| TCP server + epoll Reactor | Socket/epoll 编程 | Stage5 Ch04 socket 知识 + C 的 epoll |
| 请求解析 | 字符串处理 + regex | Stage2 Ch02 string + Stage4 Ch03 regex |
| 内存存储引擎 | `unordered_map<string,string>` | Stage2 Ch02 容器 |
| 持久化（AOF 日志） | 文件 I/O + RAII | Stage1 Ch04 RAII + C 的文件操作 |
| 线程池（可选） | `thread` + `async` + `mutex` | Stage5 Ch01 并发 |
| 配置加载 | 命令行参数 + 文件读取 | 工程实践 |
| 单元测试 | GTest | Stage5 Ch03 工程化 |
| CMake 构建 | target 体系 | Stage5 Ch03 工程化 |

**简化版（单线程 Reactor）**：`main()` → 创建 Server → epoll loop → `accept` 新连接 → `read` 请求 → `process` 逻辑 → `write` 响应。不需要线程池，约 300-500 行 C++。

**完整版（多线程线程池）**：主线程 Reactor accept + 分发 fd → 线程池 worker 处理 I/O。约 800-1200 行。

---

## 3. 简易架构

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ TCP (GET/SET/DEL)
┌──────▼──────────────────────────┐
│  Server (epoll Reactor)         │
│  ┌──────────┐  ┌────────────┐  │
│  │ Parser   │→│ Engine     │  │
│  │ (string) │  │ (umap)    │  │
│  └──────────┘  └─────┬──────┘  │
│                      │ 每次 SET│
│               ┌──────▼──────┐  │
│               │ AOF Logger  │  │
│               │ (ofstream)  │  │
│               └─────────────┘  │
└────────────────────────────────┘
```

---

## 4. 实现指引（关键代码骨架）

**Server 类——封装 socket + epoll**（用 RAII）：
```cpp
class Server {
    int listen_fd;
    int epfd;
public:
    Server(int port);           // 构造——bind+listen+epoll_create
    ~Server();                  // 析构——close fd（RAII）
    void run();                 // 主循环——epoll_wait + accept + I/O
};
```

**Engine 类——`unordered_map` 存储**：
```cpp
class Engine {
    std::unordered_map<std::string, std::string> store;
    std::ofstream aof;  // Append-Only File
public:
    std::string get(const std::string& key);
    void set(const std::string& key, const std::string& value);
    bool del(const std::string& key);
};
```

**测试**：用 GTest 测试 Engine 的 GET/SET/DEL + AOF 恢复。

---

## 5. DIY 扩展方向

| 方向 | 涉及的进阶技术 |
|---|---|
| 多线程版本 | 线程池 + `shared_ptr` 共享 Engine + mutex 保护 |
| RESP 协议兼容 | 兼容 Redis 客户端——`redis-cli` 直接连接 |
| LRU 淘汰 | `list` + `unordered_map::iterator` 实现 order |
| 过期时间 | `map<time_t, string>` + timer |
| 快照持久化 (RDB) | mmap + 二进制序列化 |

---

## 6. 编写方针

1. **不是"手把手教程"**——给出设计思路 + 关键骨架 + 每个组件的实现注意事项，读者自己完成剩余代码
2. **每个技术选型标注"为什么选这个"**——为什么用 `unordered_map` 不用 `map`（O(1) vs O(log n)）、为什么用 `string` 不用 `char*`（自动内存管理）
3. 本章不讲的内容：完整的 RESP 协议实现、完整的 RDB 持久化格式

---

## 7. 4D 自检

| 维度 | 预估 |
|---|---|
| 技术深度 | 7/10 — 架构设计的选择理由、RAII 封装 socket/epoll、AOF 日志的一致性（fsync） |
| 面试覆盖 | 7/10 — 为什么选 unordered_map？AOF rewrite 怎么实现？单线程 Reactor vs 多线程？ |
| 生产陷阱 | 8/10 — AOF 不 fsync 的 crash 数据丢失、epoll ET 模式读不完整、连接泄漏 |
| 交叉引用 | 9/10 — ⊕Stage1 RAII → Server 析构、⊕Stage2 容器 → Engine、⊕Stage5 并发 → 线程池、⊕C epoll → Reactor |
| **总分** | **31/40** |
