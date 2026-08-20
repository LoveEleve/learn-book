# 闭环笔记 q4: DistributedQueue — FIFO 顺序 + 阻塞 take + 并发消费

## 假设
FIFO 靠序号全序; take 阻塞靠 watch + latch; 并发消费靠 NoNode 重试。

## 验证过程
- **FIFO 保证** (L65-87): orderedChildren = getChildren → 只收 "qn-" 前缀 (L74) → **TreeMap\<Long\> 按序列排序** (L79-80, Long 64 位) — 非 qn- 前缀/非数字子节点 warn+跳过 (L75-83)
- **offer** (L262-272): **PERSISTENT_SEQUENTIAL** (L265 — 数据跨会话存续, 与选举/锁的 ephemeral 对照) — 目录不存在 → **NoNode → create(dir) 自举** (L267-269)
- **element/remove** (L138-201): 循环读快照 → 按序逐节点 getData (element) 或 **getData+delete** (remove) → **NoNodeException = 被并发消费方抢走 → 试下一个** (L160-162,195-197) — 快照内全被抢 → 重读快照 (while true)
- **take** (L228-255): **LatchChildWatcher 注册在 getChildren 上** (L234 — 注册即读) → 空队列 → **await 阻塞** (L240) → 任何子节点变更 (offer 创建/他人删除) → latch 释放 → 重读 (虚假唤醒靠 re-loop 兜底) — **不用前驱 watch, 用整目录变更事件**
- **peek/poll** (L280-300): 异常 → null 的宽容面 (NoSuchElementException 包装)

## 代码类型
Implementation (队列原语)

## 跨域关联
- Z-3: PERSISTENT_SEQUENTIAL (数据存续) vs EPHEMERAL (会话绑定) 对照
- Z-6: getChildren watcher (NodeChildrenChanged) 触发面
- Z-7: 客户端 API (create/getChildren/getData/delete)

## 结论
队列 = 持久顺序节点 (qn-) + TreeMap\<Long\> FIFO + 注册即读的目录 watch 阻塞 + NoNode 重试并发消费。
源码位置: DistributedQueue.java:65-87,138-272
