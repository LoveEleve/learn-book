# 闭环笔记 q6: writeToClient — 写路径与 writev

## 假设
写路径 = 从库走 replBufBlock 共享缓冲; 普通客户端 writev 批量 (静态 buf + 链表节点拼 iov); sentlen 跟踪部分写。

## 验证过程
- writeToClient (networking.c:1978-2051): 统计 (L1980) → _writeToClient (L1917)
- **_writeToClient 从库分支** (L1919-1942): **replBufBlock 共享** (R-9: 从库不持 reply 链表, 直接引用复制缓冲块)
  - 断言 bufpos==0 && reply 空 (L1920)
  - ref_repl_buf_node + ref_block_pos 定位 (L1922-1930); 块发完 refcount-- 移下一块 (L1934-1939) + 增量 trim backlog
- **普通分支** (L1944-1965):
  - reply 链表非空 → **_writevToClient** (L1844-1910): iov 数组 = 静态 buf 剩余 + 链表节点 (L1851-1876); **iovmax = min(IOV_MAX, conn->iovcnt)** (L1846); **NET_MAX_WRITES_PER_EVENT 上限** (L1863); 空节点跳过释放 (L1865-1870)
    - 写后 (L1883-1907): 静态 buf 先扣 (sentlen 推进, bufpos 清零 L1889-1893); 链表逐节点扣减 (sentlen 部分写 L1899-1901); reply_bytes 记账同步
  - 链表空 + buf 有数据 (L1954-1965): connWrite 直写 + sentlen 推进; 写满 bufpos=0
- handler_installed 语义 (L1970-1977): 线程调用 (io threads) = 0, 不装写事件 (线程不碰事件循环)
- 写事件: sendReplyToClient (L2053) → writeToClient(1) → 写完删写事件 / 未完保留
- handleClientsWithPendingWrites (L2062): 批量直写 (事件循环前一次机会)

## 代码类型
Mechanism (写路径)

## 跨域关联
- R-2 (io threads 扇出) / R-9 (replBufBlock 共享) / R-20 (beforeSleep)

## 结论
写路径 = **一次 writev 批量发** (省系统调用): 静态 buf + 链表节点拼成 iov 数组, sentlen 精确跟踪部分写。从库走共享 replBufBlock (零复制, R-9 引用计数管理)。NET_MAX_WRITES_PER_EVENT 限制单轮写量 (防饿死事件循环)。
源码位置: networking.c:1844-2062
