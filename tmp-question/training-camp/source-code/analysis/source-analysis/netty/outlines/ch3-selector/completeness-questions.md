# Ch3 NIO Selector — 全视角完备性验证

## 开发者视角

| # | 问题 | 对应 outline |
|:--:|------|:--:|
| 1 | select() / selectNow() / select(timeout) 三个方法什么区别？ | §3.1 §1 |
| 2 | interestOps 和 readyOps 什么区别？ | §3.1 §2 |
| 3 | OP_READ/OP_WRITE/OP_CONNECT/OP_ACCEPT 什么时候触发？ | §3.1 §3 |
| 4 | selectedKeys() 为什么必须手动 remove？不 remove 会怎样？ | §3.1 §4 |
| 5 | attach 和 attachment 怎么用？register 的三参数重载是什么？ | §3.1 §5 |
| 6 | 完整的 select 循环怎么写？ | §3.2 §1 |
| 7 | cancel() 做了什么？key 以后还能用吗？ | §3.2 §2 |
| 8 | wakeup() 怎么打断正在阻塞的 select？ | §3.2 §3 |
| 9 | OP_WRITE 为什么几乎总是就绪？怎么避免？ | §3.2 §4 |

## 架构师视角

| # | 问题 | 对应 outline |
|:--:|------|:--:|
| 1 | NIO 一线程多连接 vs BIO 一线程一连接的根本区别？ | §3.2 §1 |
| 2 | 为什么 OP_WRITE 的连续触发会让 NIO 退化为轮询？ | §3.2 §4 |
| 3 | wakeup 在多线程模型中的作用？ | §3.2 §3 |

## 覆盖审计

| 身份 | 提问数 | 可回答 | 覆盖率 |
|------|:--:|:--:|:--:|
| 开发者 | 9 | 9 | 100% |
| 架构师 | 3 | 3 | 100% |
| **合计** | **12** | **12** | **100%** |
