# 闭环笔记 q1: MappedFile — mmap + 双缓冲生命周期

## 假设
单文件 = mmap 映射 + writeBuffer 写缓冲; append/commit/flush 三阶段; 卸载 unmap。

## 验证过程
- **映射** (DefaultMappedFile.java:168): `fileChannel.map(MapMode.READ_WRITE, 0, fileSize)` → mappedByteBuffer (1GB CommitLog 文件)
- **双缓冲** (L83-95): writeBuffer (TransientStorePool 借用, L153) + mappedByteBuffer; `getAppendBuffer` (L301-302): **writeBuffer != null ? writeBuffer : mappedByteBuffer** — 池启用时写堆外缓冲, 否则直写 mmap
- **append 三形态** (L311/316/342/361): byte[] / ByteBuffer / offset 定位 / **appendMessageUsingFileChannel** (L361: 池启用且 buffer 满时 fileChannel 直写 — "never both" L390)
- **commit 阶段** (L412, commitLeastPages): writeBuffer → fileChannel.transferTo (堆外→页缓存) — 只在池启用时有效
- **flush 阶段**: mappedByteBuffer.force() / fileChannel.force() — 页缓存→磁盘
- **selectMappedBuffer** (L503/527): 读面 (mmap 切片) — 消费者读取
- **cleanup/unmap** (L545-591): 引用计数 (cleanupOver 防重复) + unmap (释放页表) — **引用计数 + 延迟卸载** (被读时不可卸)
- **isLoaded0 反射** (L118-121): 页加载状态统计 (5.x)
- **预热**: warmMappedFile (页缓存预热 — 预分配文件加载)

## 代码类型
Implementation (内存映射文件)

## 跨域关联
- RM-3 (CommitLog): append 调用方
- RM-8/9 (消费): selectMappedBuffer 读面

## 结论
MappedFile = 双缓冲文件抽象: 池启用 → 写堆外 buffer → commit (transferTo 页缓存) → flush (force 磁盘); 池关闭 → 直写 mmap → flush; 读面 mmap 切片; 引用计数延迟卸载。
源码位置: DefaultMappedFile.java:83-95,168,301-302,361-412,503-591; MappedFile.java (接口 382 行)
