# RM-2 存储底层 — completeness-questions (全视角提问验证)

## 开发者视角

1. mmap 怎么映射? (fileChannel.map READ_WRITE)
2. writeBuffer 和 mappedByteBuffer 何时用哪个? (池启用写堆外, 否则直写 mmap)
3. 同步刷盘怎么实现? (GroupCommitService + future 等待)
4. 异步刷盘的触发条件? (500ms / 4 页 / 10s)
5. 1GB 文件怎么定位? (offset/fileSize 除法)
6. 预分配干什么? (后台 mmap + 预热, 防写时卡顿)
7. 写锁怎么选? (自旋低竞争 / 重入)
8. 引用计数卸载是什么? (被读时延迟 unmap)

## 架构师视角

9. 为什么 mmap 而非常规 IO? (页缓存 + 零拷贝读)
10. 双缓冲 (堆外+mmap) 的取舍? (页缓存颠簸 vs 5GB 堆外 + mlock)
11. 组提交双链表 swap 的设计? (无锁读侧批量消费)
12. flushedWhere 水位的作用? (组提交判定 + 复制进度)
13. 1GB 对齐段的好处? (O(1) 定位 + 顺序写)
14. 为什么默认关闭堆外池? (普通场景页缓存够用, 5GB 代价大)
15. mlock 锁页的意义? (防 swap 抖动 — 存储线程不能等换页)
16. 预分配 + 预热解决什么? (mmap 首次访问缺页延迟)

## 学生视角

17. mmap 是什么? (文件映射到内存, 读写=内存操作)
18. SYNC 和 ASYNC 刷盘差在哪? (消息返回时机 vs 吞吐)
19. 为什么日志文件适合顺序写? (机械盘顺序 vs 随机)
20. 1GB 文件满了怎么办? (滚动到下一个文件 — 段式日志)
