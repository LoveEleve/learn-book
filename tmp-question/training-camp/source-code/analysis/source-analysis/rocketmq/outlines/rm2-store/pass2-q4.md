# 闭环笔记 q4: 预分配与写锁 — AllocateMappedFileService + PutMessageLock

## 假设
后台预分配 mmap (防写入时映射卡顿); 写入锁自旋/重入两实现。

## 验证过程
- **AllocateMappedFileService** (ServiceThread 后台线程): mmapOperation (L154-210) —
  - requestTable **按文件路径去重** (L160-169: expectedRequest != req → 过期请求跳过)
  - 创建: 池启用 → **ServiceLoader.load(MappedFile.class)** 自定义实现 (5.x 扩展点) + 兜底 DefaultMappedFile; 池关闭 → DefaultMappedFile
  - **耗时 >10ms 告警** (L190-194): 映射慢的可见性
  - warmMappedFile (L199): **页缓存预热** (写入填充触发缺页加载)
- **写锁** (PutMessageSpinLock L22-45): CAS 自旋 (AtomicBoolean) — "suggest using this with low race conditions"; PutMessageReentrantLock (ReentrantLock 版本) — 按配置选 (5.x 自旋默认?)
- **预分配队列**: requestQueue + requestTable (去重) — 写路径 putRequestAndReturnMappedFile (CommitLog 调用, 提前预创建下一文件)
- **池不足降级** (L83-89): transientStorePool buffer 不够 → 跳过预分配 (告警)

## 代码类型
Implementation (并发与预热)

## 跨域关联
- RM-3 (CommitLog): putRequestAndReturnMappedFile
- RM-5 (Broker): 写路径性能

## 结论
预分配 = 后台线程 mmap + 路径去重 + 池感知 (ServiceLoader 扩展点) + 预热 + >10ms 告警; 写锁 = 自旋 (低竞争) / 重入锁双实现。
源码位置: AllocateMappedFileService.java:39-210; PutMessageSpinLock.java:22-45
