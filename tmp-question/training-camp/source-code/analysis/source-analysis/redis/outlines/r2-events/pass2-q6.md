# 闭环笔记 q6: io threads 生命周期 — 互斥锁即栅栏

## 假设
io threads 用"互斥锁锁定/解锁"当启动/停止信号; 默认 1 (纯单线程), 上限 128。

## 验证过程
- 配置 (config.c:3149): io-threads 1-128, 默认 1, IMMUTABLE (启动时定, 不可热改); io-threads-do-reads 默认 0 (L3051)
- 常量 (networking.c:4215): IO_THREADS_MAX_NUM=128; CACHE_LINE_SIZE 64/128 (L4217-4223, 防伪共享 — threads_pending 原子变量按缓存行对齐)
- **initThreadedIO** (L4295-4325): io_threads_num==1 → 不 spawn (L4302-4303); >128 → 退出 (L4305); 每个额外线程: 创建时**锁住互斥锁** (L4319 init / L4320 lock) — 线程阻塞在 mutex 上
- **IOThreadMain** (L4248-4284): 先自旋等 pending>0 (L4260-4264, 最多 100 万次); pending 仍 0 → 尝试锁 mutex (L4267-4268) — 锁不住就继续等 → 锁住了 (已被 stop 锁) 阻塞
  - 工作循环: 处理 io_threads_list[id] (写/读按 io_threads_op) → 清空 → pending=0 (L4278-4283)
- **startThreadedIO** (L4347-4355): 解锁全部 → active=1 — 线程从 mutex 醒来
- **stopThreadedIO** (L4354-4363): 先 handleClientsWithPendingReadsUsingThreads (L4356, 清残留) → 锁住全部 → active=0 — 线程重新阻塞
- **惰性停** stopThreadedIOIfNeeded (L4373-4384): pending < io_threads_num×2 → 停 (L4377-4380) — 客户端少时避免线程切换开销
- 线程属性: 命名 io_thd_N (L4256) / CPU 亲和 (L4257) / killable (L4258)

## 代码类型
Mechanism (线程生命周期)

## 跨域关联
- R-20 (initThreadedIO 调用) / R-28 (读写回调)

## 结论
互斥锁 = **"启动/停止"双向信号**: 启动时解锁让线程跑, 停止时上锁让线程睡 (线程自旋等 pending 为主, 锁为兜底栅栏)。懒停止 (pending < 2×num) 避免小流量下的线程开销。IMMUTABLE 配置保证启动时确定线程数。
源码位置: networking.c:4215-4384
