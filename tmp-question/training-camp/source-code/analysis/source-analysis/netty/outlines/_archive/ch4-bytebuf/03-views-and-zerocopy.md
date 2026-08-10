# Ch4 ByteBuf 视图与零拷贝 — 操作已有数据的三种武器

> 覆盖: Q6(slice/copy) / Q3(CompositeByteBuf) / Q9(ByteOrder)

---

### 1. 提取 — slice/duplicate/copy
  - `slice()` / `slice(int, int)` (ByteBuf.java:2207,2233) — 窗口截取, 共享数据, 不增 refCnt
  - `readSlice(int)` (ByteBuf.java:1595) — 推进 readerIndex 的 slice — Pipeline 常用
  - `retainedSlice()` (ByteBuf.java:2221) — 跨线程安全
  - `duplicate()` (ByteBuf.java:2261) — 全量视图, 独立 index
  - `copy()` (ByteBuf.java:2186) — 深拷贝, 独立 buffer
  - `readRetainedSlice(int)` (ByteBuf.java:1613) — 取一段+消费+retain

### 2. 聚合 — CompositeByteBuf
  - Component[] 数组 + `findComponent(offset)` 二分查找 (CompositeByteBuf.java:59,1617)
  - addComponent0: ownership transfer — 不调 retain() (line 280) — 调方转移所有权
  - 读取: O(log N) — 弱缓存 lastAccessed 优化连续访问
  - 写入: 跨 Component → 分裂+consolidate — 拷贝不可避免
  - 默认上限: `DEFAULT_MAX_COMPONENTS=16` (AbstractByteBufAllocator.java:33)
  - 超限: `consolidateIfNeeded()` → `size > maxNumComponents` → `consolidate0()` (CompositeByteBuf.java:566-572)

### 3. 格式 — ByteOrder
  - `order()` / `order(ByteOrder)` @Deprecated (ByteBuf.java:284,298) — Netty 4.2 废弃全局端序
  - LE 后缀方法: `getIntLE()`(701) / `getShortLE(611)` / `getLongLE(745)
  - SwappedByteBuf.getInt → `ByteBufUtil.swapInt(buf.getInt(index))` (SwappedByteBuf.java:283-284)
  - 选择: HTTP/gRPC → writeInt (BigEndian); 嵌入式 LE 协议 → writeIntLE

### 4. 收束
  - 全程零额外内存拷贝 — 数据从网卡 DMA → Direct buffer → 视图片 → Composite → DMA 到网卡
  - 数据已经有了、缓冲区已经准备好了 — 谁来驱动整个数据流？引出 Ch5 EventLoop

---

### 核心悬念
**"Netty 把 ByteBuffer 中每一个全局状态变量都局部化了：读位置、容量、内存回收、数据共享、端序。"**
