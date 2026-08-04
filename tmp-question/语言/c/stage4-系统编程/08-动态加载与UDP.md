# 动态加载与 UDP

> **对应 Layer**：Layer 4（OS 接口补充—插件系统 + 无连接网络），stage4 补遗第 4 篇  
> **主线标准**：C11/C17 + Linux 系统调用

---

## Part A：动态加载——`dlopen`/`dlsym`

### A.1 基本机制

```c
#include <dlfcn.h>
#include <stdio.h>

void *handle = dlopen("./libmylib.so", RTLD_NOW);
if (!handle) {
    fprintf(stderr, "dlopen: %s\n", dlerror());
    return -1;
}

// 按函数名查找符号
typedef int (*add_fn)(int, int);
add_fn add = (add_fn)dlsym(handle, "my_add");
char *error = dlerror();
if (error) { fprintf(stderr, "dlsym: %s\n", error); dlclose(handle); return -1; }

printf("100 + 200 = %d\n", add(100, 200));
dlclose(handle);
```

### A.2 `RTLD_NOW` vs `RTLD_LAZY`

| 标志 | 行为 | 适用场景 |
|---|---|---|
| `RTLD_NOW` | `dlopen` 时立即解析所有未定义符号——找不到立即失败 | 生产——尽早发现缺失 |
| `RTLD_LAZY` | 按需解析——只在首次调用函数时解析符号 | 可选依赖 |

### A.3 `RTLD_GLOBAL`——符号对其他动态库可见

```c
void *handle = dlopen("libmodule.so", RTLD_NOW | RTLD_GLOBAL);
// libmodule.so 中导出的符号现在对所有后续 dlopen 的库可见
```

**生产案例——Nginx 模块机制**：Nginx 在 `ngx_load_module()` 中用 `dlopen(module_path, RTLD_NOW | RTLD_GLOBAL)` 加载模块 `.so`。每个模块定义 `ngx_module_t ngx_http_xxx_module` 全局变量，Nginx 通过 `dlsym` 查找该变量并加入模块链。

### A.4 内核实现

`dlopen` → `open()` 实际 `.so` 文件 → `mmap` 将 ELF 的 LOAD 段映射到进程地址空间 → `do_dlopen()` 解析符号。S4Ch03 的 mmap 和 S2Ch03 的 ELF 在这里会合。

### A.5 `dlclose` 后函数指针悬空

```c
add_fn add = dlsym(handle, "my_add");
dlclose(handle);
int result = add(100, 200);  // SIGSEGV！handle 卸载后代码段已释放
```

**防护**：在模块卸载前清空所有缓存的函数指针，或用 `RTLD_NODELETE` 标志防止卸载。

### A.5 `dlclose` 后函数指针悬空

```c
add_fn add = dlsym(handle, "my_add");
dlclose(handle);
int result = add(100, 200);  // SIGSEGV！handle 卸载后代码段已释放
```

**防护**：在模块卸载前清空所有缓存的函数指针，或用 `RTLD_NODELETE` 标志防止卸载。

### A.6 `dlinfo`——查询加载模块信息

```c
#include <link.h>
struct link_map *lm;
dlinfo(handle, RTLD_DI_LINKMAP, &lm);
for (struct link_map *p = lm; p; p = p->l_next)
    printf("Loaded: %s (base=%p)\n", p->l_name, (void*)p->l_addr);
```

### A.7 LD_PRELOAD 原理——不是魔法，是 `dlopen` 优先级——不是魔法，是 `dlopen` 优先级

```bash
LD_PRELOAD=./libevil.so ./myapp
```

动态链接器（`ld-linux.so`）的符号查找优先级：**LD_PRELOAD → DT_NEEDED（直接依赖）→ RTLD_GLOBAL dlopen → RTLD_LOCAL dlopen**。LD_PRELOAD 的 `.so` 在所有其他库之前被加载和查找。

**防御 LD_PRELOAD 劫持**：
1. **`-Wl,-z,now`**（Full RELRO）——立即绑定所有符号（GOT 完全只读），消除懒惰绑定的劫持窗口
2. **`/proc/self/maps` 自检**——`grep -v ld-linux /proc/self/maps | grep -E '\.so$'` 列出所有加载的库
3. **静态链接**——完全消除动态链接的 LD_PRELOAD 攻击面
4. **`RTLD_NODELETE`**——`dlopen` 时防止 `dlclose` 卸载模块

---

## Part B：UDP + `getaddrinfo`

### B.1 UDP 的本质

TCP 是**字节流**——`send`/`recv` 不保留消息边界。UDP 是**数据报**——每个 `sendto`/`recvfrom` 是一个独立的边界。接收端一个 `recvfrom` 对应发送端的一个 `sendto`——不会粘连，不会分段。

```c
#include <sys/socket.h>
#include <netinet/in.h>

int fd = socket(AF_INET, SOCK_DGRAM, 0);

struct sockaddr_in dst = { AF_INET, htons(53) };
inet_pton(AF_INET, "8.8.8.8", &dst.sin_addr);

// 发送——数据报立即发出
sendto(fd, dns_query, query_len, 0, (struct sockaddr *)&dst, sizeof(dst));

// 接收——一个完整的数据报
char buf[65536];  // UDP 最大数据报 65535 字节
struct sockaddr_in src;
socklen_t addrlen = sizeof(src);
ssize_t n = recvfrom(fd, buf, sizeof(buf), 0, (struct sockaddr *)&src, &addrlen);
// n = 单个数据报的大小
```

### B.2 `getaddrinfo`——协议无关的地址解析

```c
#include <netdb.h>
#include <sys/socket.h>

struct addrinfo hints = {
    .ai_family   = AF_UNSPEC,      // IPv4 或 IPv6——都行
    .ai_socktype = SOCK_DGRAM,     // UDP
    .ai_flags    = AI_PASSIVE      // 用于 bind——返回 INADDR_ANY
};

struct addrinfo *res;
int err = getaddrinfo("time.google.com", "123", &hints, &res);
if (err) { fprintf(stderr, "%s\n", gai_strerror(err)); return -1; }

// 遍历返回的地址列表——可能返回多个（IPv4+IPv6）
for (struct addrinfo *rp = res; rp; rp = rp->ai_next) {
    int fd = socket(rp->ai_family, rp->ai_socktype, rp->ai_protocol);
    if (fd == -1) continue;

    if (connect(fd, rp->ai_addr, rp->ai_addrlen) == 0) {
        // 成功——用这个
        break;
    }
    close(fd);
}
freeaddrinfo(res);
```

**`getaddrinfo` 替代了什么**：`gethostbyname()`（仅 IPv4）、手动 `inet_pton` + 硬编码地址族。`getaddrinfo` 同时处理 IPv4 和 IPv6，返回所有可用地址让调用者选择。`gethostbyname` 在 POSIX.1-2008 中废弃。

### B.3 UDP 的适用场景

| 场景 | 为什么用 UDP |
|---|---|
| DNS 查询 | 小数据包（<512 字节）——不需要 TCP 的开销 |
| NTP 时钟同步 | 时间敏感——TCP 的重传延迟比数据错误更糟 |
| 视频流/VoIP | 允许丢帧——TCP 的重传等不及 |
| 游戏实时通信 | 每秒 60 次位置更新——过时的包直接丢弃 |

### B.4 UDP vs TCP 的连接建立开销

| | TCP | UDP |
|---|---|---|
| 连接建立 | 三次握手（1.5 RTT） | 不需要连接 |
| 消息边界 | **无**——字节流 | **有**——每个数据报是独立的 |
| 可靠性 | 确认/重传——内核保证 | **无**——应用层必须自己处理 |
| 数据乱序 | 内核重排序 | **无**——应用层收到的就是发送顺序？（实际可能乱序） |
| 流量控制 | 内核拥塞控制 | **无** |

---

## 面试题

**面试官**：`dlopen` 加载的动态库，`dlclose` 后缓存的函数指针还能用吗？怎么防止？

**回答**：不能——`dlclose` 释放了 `.so` 的代码段（`munmap` 卸载 ELF LOAD 段），函数指针指向已取消的映射→调用触发 SIGSEGV。防止方案——(1) `RTLD_NODELETE` 标志（`dlopen` 时不卸载）；(2) 关闭前清空所有缓存指针；(3) 用引用计数只在最后一次关闭时卸载。Nginx 模块加载用 `RTLD_NOW | RTLD_GLOBAL` 不设 NODELETE——但 Nginx 从不卸载模块（只在 reload 时退出旧进程）。

**追问 1（面试官）**：LD_PRELOAD 的符号查找优先级是什么？为什么能拦截 `malloc`？

**追问 1 回答**：动态链接器的符号查找顺序——LD_PRELOAD（最高优先级）→ 程序直接依赖的 `.so`（DT_NEEDED）→ RTLD_GLOBAL dlopen → RTLD_LOCAL dlopen。LD_PRELOAD 的 `.so` 最先被加载且优先级最高——如果定义了 `malloc`，所有后续对 `malloc` 的符号查找都先匹配到它。防御——Full RELRO（`-z now`）立即绑定所有符号，GOT 设为只读，不再通过 PLT 懒绑定的方式查符号。

**追问 2（面试官）**：UDP 和 TCP 的应用场景怎么选？什么时候用 UDP、什么时候必须 TCP？

**追问 2 回答**：UDP——(1) 低延迟、允许丢包（DNS、NTP、VoIP、实时游戏）；(2) 每包独立、不需要长连接（DNS 每个查询一个包）；(3) 广播/多播（IP camera 视频流）。TCP——(1) 数据完整性要求高（文件传输、网页加载、数据库协议）；(2) 需要顺序保证和流量控制；(3) 连接中状态交互（HTTP/WebSocket/TLS）。UDP 的最大硬伤——内核不提供任何可靠性保证——丢包、乱序、重复全部交给应用层。QUIC（HTTP/3）选择了在 UDP 上建可靠性——不依赖内核 TCP 的拥塞控制。

---

## 总结

| 概念 | 核心知识 |
|---|---|
| `dlopen/dlsym` | 运行时加载 `.so` 并按函数名查找符号 |
| `RTLD_GLOBAL` | 加载模块的符号对后续模块可见——Nginx 用它暴露 API |
| LD_PRELOAD | 动态链接器优先级——不是内核特性 |
| `getaddrinfo` | 协议无关——同时支持 IPv4+IPv6——替代废弃的 `gethostbyname` |
| UDP 数据报 | 独立消息边界——不需要 accept/connect |
| UDP vs TCP | 无连接 vs 面向连接、不可靠 vs 可靠——根据场景选择 |

---

## 验收要点

1. `dlopen` 的 `RTLD_NOW` 和 `RTLD_LAZY` 的区别？生产代码用哪个？
2. Nginx 用 `RTLD_GLOBAL` 的目的是什么？
3. LD_PRELOAD 的原理是什么？为什么能拦截 `malloc`/`open`？
4. `getaddrinfo` 相比 `gethostbyname` 有什么优势？为什么后者被废弃？
5. UDP 的 `sendto`/`recvfrom` 和 TCP 的 `send`/`recv` 的根本区别？
6. UDP 适合什么应用场景？不适合什么？
