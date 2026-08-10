# Ch3 NIO Selector — 知识规划

> 来源: 3 源文件 | ~1410 行 | 学完第二章读者基线: Channel → Selector

---

## 01 提取 — 逐源映射

### SelectionKey.java (466行 — 事件定义)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| SelectionKey.java:296 | **OP_READ = 1<<0** — 数据可读事件 | High |
| SelectionKey.java:308 | **OP_WRITE = 1<<2** — 写空间可用事件 | High |
| SelectionKey.java:320 | **OP_CONNECT = 1<<3** — 连接完成事件 | High |
| SelectionKey.java:332 | **OP_ACCEPT = 1<<4** — 接受新连接事件 | High |
| SelectionKey.java:168 | **interestOps() / interestOps(int)** — 设置/获取关注事件 | High |
| SelectionKey.java: | **readyOps()** — 查询当前就绪事件 | High |
| SelectionKey.java:85-87 | **attach(obj) / attachment()** — 绑定任意对象 (如 ByteBuffer) | High |
| SelectionKey.java:136 | **isValid()** — 有效直到 cancel/channel close/selector close | High |
| SelectionKey.java:140-145 | **cancel()** — 加入 cancelled-key set, 下次 select 移除 | High |

### Selector.java (633行 — 选择器抽象)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Selector.java | **open()**: SelectorProvider.openSelector() → EPollSelectorImpl | High |
| Selector.java | **select()**: 阻塞直到有 Channel 就绪 | High |
| Selector.java | **select(long timeout)**: 阻塞等待最多 timeout ms | High |
| Selector.java | **selectNow()**: 非阻塞, 立即返回 | High |
| Selector.java | **selectedKeys()**: 返回 ready 的 SelectionKey Set — **必须手动 remove** | High |
| Selector.java | **wakeup()**: 打断正在阻塞的 select() | High |
| Selector.java | **keys()**: 返回所有注册的 SelectionKey | Medium |
| Selector.java | **close()**: 关闭 Selector, 释放所有 key | High |

### SelectorImpl.java (311行 — 选择器实现)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| SelectorImpl.java | **selectedKeys HashSet**: HashSet<SelectionKey> — selected 集合 | High |
| SelectorImpl.java | **publicKeys/puplicSelectedKeys**: 不可修改视图 vs 直接引用 | Medium |
| SelectorImpl.java | **doSelect(long)**: 底层 native poll — EPollSelectorImpl 重写 | High |

---

## 01 聚合 — 跨文件汇总

N=3, P1=≥2, P2=1, P3=单文件特有

### P1 — 共识

| Knowledge Point | 出现文件 |
|----------------|---------|
| OP_READ/OP_WRITE/OP_CONNECT/OP_ACCEPT | SelectionKey(定义) + Selector(文档引用) + SelectorImpl(通过 readyOps 使用) |
| select() / select(timeout) / selectNow() | Selector + SelectorImpl |
| selectedKeys() | Selector + SelectorImpl |
| interestOps / readyOps | SelectionKey + SelectorImpl |
| attach / attachment | SelectionKey + (所有 Channel register 使用) |
| cancel() | SelectionKey + SelectorImpl |
| wakeup() | Selector + SelectorImpl |

### P2 — 局部重要

| Knowledge Point | 出现文件 |
|----------------|---------|
| keys() — 所有注册 key | Selector + SelectorImpl |
| isValid() | SelectionKey (单文件, 但语义贯穿 select 循环) |

---

## 02 深度分类

### 🔴 Deep

| KP | 为什么🔴 |
|----|---------|
| select()/selectNow() — 阻塞 vs 非阻塞选择 | NIO 非阻塞模型的核心 — 一线程多连接的基础 |
| selectedKeys() — 必须手动 remove | NIO 最著名陷阱 — 每次 select 后 selectedKeys 累积不清理 |
| OP_WRITE 几乎总是就绪 | JDK 设计缺陷 — socket send buffer 通常有空间 → select 几乎每次因 OP_WRITE 唤醒 |
| wakeup() — 打断阻塞 select | 多线程控制 select 循环的基础 |

### 🟡 Working

| KP | 01 Pri | 说明 |
|----|--------|------|
| interestOps vs readyOps | P1 | 关注 vs 就绪 — register 和 select 之间的桥梁 |
| attach/attachment | P1 | 绑定对象（如 ByteBuffer）到 key — 回音循环关键 |
| cancel() — 取消注册 | P1 | 生命周期管理 — cancelled-key set 机制 |
| OP_CONNECT — 非阻塞连接 | P1 | 与 §2.2 finishConnect 配合 |
| OP_ACCEPT — 接受新连接 | P1 | ServerSocketChannel 专用 |

### 🟢 Surface

| KP | 01 Pri | 放在哪 |
|----|--------|-------|
| keys() — 全部注册 key | P2 | 和 selectedKeys 对比 |
| isValid() | P2 | cancel 流程 — 生命周期 |
| Selector.open() | P1 | 创建流程 — 和 select 一起 |

---

## 03 聚类

### Cluster A: Selector 模型 (5 KPs)
  1. select()/selectNow()/select(timeout) — 三种选择模式
  2. interestOps vs readyOps — 关注 vs 就绪
  3. OP_READ/OP_WRITE/OP_CONNECT/OP_ACCEPT — 四种事件
  4. selectedKeys() — 返回就绪 key — 必须手动 remove
  5. attach/attachment — 绑定 ByteBuffer

### Cluster B: 单线程 select 循环 (4 KPs)
  1. register() → interestOps → select() → selectedKeys → process → remove
  2. cancel() — cancelled-key set 机制
  3. wakeup() — 打断阻塞
  4. OP_WRITE 陷阱 — 几乎总是就绪

### 教学顺序: A(模型) → B(循环)

---

## 04 Per-Article Outline 映射

| 篇 | 覆盖 | 核心问题 |
|:--:|------|------|
| §3.1 | Cluster A | select() 怎么工作的？四种事件分别什么时候触发？ |
| §3.2 | Cluster B | 一个线程+一个 Selector 怎么管理多个连接？OP_WRITE 为什么是陷阱？ |
