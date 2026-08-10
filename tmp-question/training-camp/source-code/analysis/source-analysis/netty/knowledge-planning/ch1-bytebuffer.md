# Ch1 NIO ByteBuffer — 知识规划

> 来源: 5 源文件 | ~4000 行 | 学完第一章读者基线: byte[] → ByteBuffer

---

## 01 提取 — 逐源映射

### Buffer.java (736行 — 所有 NIO Buffer 基类)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Buffer.java:197-201 | **四字段模型**: mark(-1) / position(0) / limit / capacity | High |
| Buffer.java:197-201 | **invariant**: 0 ≤ mark ≤ position ≤ limit ≤ capacity | High |
| Buffer.java:62-83 (Javadoc) | **Absolute vs Relative 操作**: absolute=get/put(index)不影响position; relative=position++ | High |
| Buffer.java:380 | **mark()**: 将当前 position 记录到 mark | High |
| Buffer.java:396 | **reset()**: 将 position 恢复到 mark—InvalidMarkException if mark<0 | High |
| Buffer.java:421 | **clear()**: position=0, limit=capacity, mark=-1 — 准备写模式 | High |
| Buffer.java:449 | **flip()**: limit=position, position=0, mark=-1 — 切换到读模式 | High |
| Buffer.java:471 | **rewind()**: position=0, mark=-1 — 重读但不改 limit | High |
| Buffer.java:483 | **remaining()**: limit - position — 还剩多少可读空间 | High |
| Buffer.java:188 | **UNSAFE = Unsafe.getUnsafe()** — cached, 用于 Direct 缓冲区快速访问 | Medium |
| Buffer.java:203-213 | **address field**: Heap=数组偏移; Direct=堆外内存起始地址—JNI GetDirectBufferAddress 使用 | Medium |
| Buffer.java:504 | **isReadOnly() abstract**: 子类实现决定是否只读 | High |
| Buffer.java:580 | **isDirect() abstract**: 子类实现决定是否堆外 | High |

### X-Buffer.java.template (1828行 — ByteBuffer 抽象类)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| X-Buffer.t:316 | **allocateDirect(int)**: 通过 Bits.reserveMemory 分配堆外内存 → 返回 DirectByteBuffer | High |
| X-Buffer.t:345 | **allocate(int)**: 分配 new byte[capacity] → 返回 HeapByteBuffer | High |
| X-Buffer.t:389 | **wrap(array, offset, length)**: 包裹现有 byte[] 不拷贝—position=offset, limit=offset+length | High |
| X-Buffer.t:421 | **wrap(array)**: 包裹整数组—语义 equals wrap(array, 0, array.length) | High |
| X-Buffer.t:553 | **slice() abstract**: 创建共享底层数据的子窗口 [position,limit)，独立 mark/position/limit | High |
| X-Buffer.t:576 | **duplicate() abstract**: 创建全量共享视图，独立 mark/position/limit | High |
| X-Buffer.t:601 | **asReadOnlyBuffer() abstract**: 只读包装器—任何写入抛 ReadOnlyBufferException | High |
| X-Buffer.t:1024 | **hasArray()**: 是否有底层 byte[] (Heap=true, Direct=false) | High |
| X-Buffer.t:1047 | **array()**: 返回底层 byte[]—Direct 或无数组时 UnsupportedOperationException | High |
| X-Buffer.t:1216 | **compact() abstract**: 将 [position,limit) 复制到 [0,limit-position)，position=remaining, limit=capacity—准备写模式 | High |
| X-Buffer.t:1307 | **equals(Object)**: 只在 remaining() 范围内比较—不是全量 byte[] 比较 | High |
| X-Buffer.t:1347 | **compareTo(ByteBuffer)**: 在各自 remaining() 范围内字典序比较 | High |
| X-Buffer.t:1624 | **order() abstract**: 返回 ByteOrder—Heap 默认 BIG_ENDIAN | High |
| X-Buffer.t:1651 | **order(ByteOrder)**: 设置端序—影响后续 getInt/getLong 等多字节操作 | High |
| X-Buffer.t:615 | **get()**: relative read—读 position → position++ | High |
| X-Buffer.t:634 | **put(byte)**: relative write—写 position → position++ | High |
| X-Buffer.t:649 | **get(int)**: absolute read—读指定 index，不影响 position | High |
| X-Buffer.t:685 | **put(int, byte)**: absolute write—写指定 index | High |
| X-Buffer.t:740 | **get(byte[], int, int)**: bulk relative read—读取到目标数组 | High |
| X-Buffer.t:881 | **put(byte[], int, int)**: bulk relative write—从源数组写入 | High |

### Heap-X-Buffer.java.template (658行 — HeapByteBuffer 实现)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Heap-X.t:104 | **slice()**: new HeapByteBuffer with offset=pos, capacity=lim-pos—**共享**原始 byte[] | High |
| Heap-X.t:130 | **duplicate()**: new HeapByteBuffer with all fields copied—**共享**原始 byte[] | High |
| Heap-X.t:164 | **get()**: `hb[ix(nextGetIndex())]` — O(1) 直接数组读取 | High |
| Heap-X.t:168 | **get(int)**: `hb[ix(i)]` — O(1) 绝对索引读取 | High |
| Heap-X.t:198 | **put(byte)**: `hb[ix(nextPutIndex())]=x` — O(1) 直接数组写入 | High |
| Heap-X.t:207 | **put(int, byte)**: `hb[ix(i)]=x` — O(1) 绝对索引写入 | High |
| Heap-X.t:261 | **compact()**: `System.arraycopy(hb, ix(pos), hb, ix(0), rem)` — O(N) 拷贝 + position=rem, limit=capacity | High |
| Heap-X.t:281-287 | **_get(int) / _put(int, byte)**: package-private — `hb[i]` / `hb[i]=b` — HotSpot 内联热点 | High |
| Heap-X.t:188 | **isDirect()=false** | High |
| Heap-X.t:194 | **isReadOnly()** — 读/写子类覆盖为 true/false | High |

### Direct-X-Buffer.java.template (543行 — DirectByteBuffer 实现)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Direct-X.t:54-56 | **address field**: 堆外内存起始地址 (继承自 Buffer)—JNI GetDirectBufferAddress 直接取 | High |
| Direct-X.t:69-90 | **Deallocator**: 封装 address+size+capacity — `UNSAFE.freeMemory(address)` 释放堆外内存 | High |
| Direct-X.t:90 | **防 double-free**: `if(address==0) return` + `address=0` — 释放后将 address 归零 | High |
| Direct-X.t:96 | **Cleaner**: PhantomReference 绑定 — GC 回收 DirectByteBuffer 时触发 Deallocator 释放堆外内存 | High |
| Direct-X.t:33 | **import jdk.internal.ref.Cleaner** — GC 触发的被动回收 | Medium |
| Direct-X.t:9-23 | **_get(i)**: `UNSAFE.getByte(address + i)` — O(1) Unsafe 直接内存读取 | High |
| Direct-X.t:9-23 | **_put(i, byte)**: `UNSAFE.putByte(address + i, value)` — O(1) Unsafe 直接内存写入 | High |
| Direct-X.t:34 | **Bits.reserveMemory**: 分配前检查是否超过 `-XX:MaxDirectMemorySize` | High |
| Direct-X.t:34 | **isDirect()=true** | High |

### Bits.java (234行 — 内存管理)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Bits.java:1 | **reserveMemory**: 分配 Direct 内存前检查总池 — 超过 limit → 触发 GC + 重试 | High |
| Bits.java:1 | **MaxDirectMemorySize**: `-XX:MaxDirectMemorySize` JVM 参数控制上限 — 默认为堆最大值 | High |
| Bits.java:1 | **指数退避**: reserveMemory 等待 space 可用的重试循环 — Thread.sleep 1,2,4,8...256ms (max 9) | Medium |
| Bits.java:1 | **unreserveMemory**: 释放时归还计数 | High |
| Bits.java:1 | **copyFromArray**: `UNSAFE.copyMemory` — src byte[] → dst native address | Medium |

---

## 01 聚合 — 跨文件汇总

N=5, P1=≥3, P2=2, P3=1

### P1 — 共识 (≥3 文件, 10 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| 四字段模型 (position/limit/capacity/mark) + invariant | Buffer + X-Buffer + Heap + Direct — 全链基数 |
| flip() — 切换到读模式 (limit=pos, pos=0) | Buffer + 全子类 — 所有 NIO read 的前置步骤 |
| clear() — 准备写模式 (pos=0, limit=cap) | Buffer + 全子类 |
| get()/put() — relative 读写 | X-Buffer + Heap + Direct |
| get(int)/put(int, byte) — absolute 读写 | X-Buffer + Heap + Direct |
| hasArray()/array() — 后端数组暴露 | X-Buffer + Heap(true) + Direct(throw) |
| isDirect()/isReadOnly() | Buffer + Heap + Direct |
| allocate(int)/allocateDirect(int) | X-Buffer(入口) + Heap + Direct(实际执行) |
| order() — 字节序获取/设置 | X-Buffer + Heap(BIG_ENDIAN) + Direct(BIG_ENDIAN) |
| compact() — 压缩未读数据 | X-Buffer(abstract) + Heap(arraycopy) + Direct(impl) |

### P2 — 局部重要 (2 文件, 9 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| Deallocator + Cleaner — Direct 释放机制 | Direct-X.template + Bits.java |
| Bits.reserveMemory/unreserveMemory — 总池 | Direct-X.template + Bits.java |
| mark()/reset() | Buffer + X-Buffer |
| rewind() — pos=0 保留 limit | Buffer + 全子类(被 flip/clear 替代) |
| slice() — 共享子窗口 | X-Buffer(abstract) + Heap(impl) |
| duplicate() — 共享全量视图 | X-Buffer(abstract) + Heap(impl) |
| wrap(byte[]) — 包裹现有数组 | X-Buffer(entry) + Heap(impl) |
| bulk get(byte[], offset, length) | X-Buffer(abstract) + Heap(impl) |
| bulk put(byte[], offset, length) | X-Buffer(abstract) + Heap(impl) |

### P3 — 独立 (1 文件, 8 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| address field — JNI 快速访问 | Buffer.java |
| Absolute vs Relative 操作区分(Javadoc) | Buffer.java |
| UNSAFE cached | Buffer.java |
| asReadOnlyBuffer() | X-Buffer(abstract) |
| equals() — remaining()范围内比较 | X-Buffer |
| compareTo() — remaining()字典序 | X-Buffer |
| Heap._get()/_put() — HotSpot 内联 | Heap-X-Buffer |
| Direct._get()/_put() — Unsafe 直接内存 | Direct-X-Buffer |

---

## 02 深度分类

### 🔴 Deep (承载核心设计决策)

| KP | 为什么🔴 |
|----|---------|
| 四字段模型 + invariant | NIO 设计根基 — 三种模式(读/写/回退)全基于四个字段关系 |
| flip() — 切换到读模式 | 所有 NIO read 的标准前置 — 不理解 flip 则所有 read 操作都是错的 |
| compact() — 压缩未读数据 | flip 的互补操作 — O(N) arraycopy 避免数据拷贝。设计决策 |
| DirectByteBuffer + Deallocator + Cleaner | JVM 堆外内存回收 — GC 被动释放 → Netty 为什么用引用计数替代 |
| Bits.reserveMemory | 总池限制 + 指数退避 — MaxDirectMemorySize 超限后 JVM 的处理 |

### 🟡 Working (有设计决策, 非核心)

| KP | 01 Pri | 说明 |
|----|--------|------|
| allocate(int)/allocateDirect(int) | P1 | Heap vs Direct 的选择入口 |
| get()/put() relative | P1 | 推进 position — 后续 Netty ByteBuf 双指针的对比基线 |
| get(int)/put(int, byte) absolute | P1 | 不推进 position — 为 slice/duplicate 提供不变性 |
| hasArray()/array() | P1 | Heap=true, Direct=throw — 导致不可移植代码 |
| slice()/duplicate() | P2 | 共享视图 — Heap 实现带 offset/limit 语义 |
| wrap(byte[]) | P2 | 不拷贝包裹 — 修改 wrap 后 buffer = 修改原始数组 |
| mark()/reset() | P2 | peek 模式 — 和 Netty markReaderIndex 等价 |
| order() | P1 | BigEndian 默认 — 影响多字节读写 |

### 🟢 Surface (机制性了解即可)

| KP | 01 Pri | 放在哪 |
|----|--------|-------|
| clear()/rewind() | P1 | 和 flip 一起 — 三种状态切换 |
| isDirect()/isReadOnly() | P1 | 和 hasArray 一起 — 类型检测 |
| asReadOnlyBuffer() | P3 | 和 slice/duplicate 一起 — 视图操作 |
| equals()/compareTo() | P3 | API 陷阱篇 — 只在 remaining() 内比较 |
| bulk get/put | P2 | 和 relative get/put 一起 |
| UNSAFE cached + address field | P3 | Direct section — 底层依赖 |
| Heap._get()/_put() 内联 | P3 | Heap section — HotSpot 优化 |

---

## 03 聚类

### Cluster A: 核心抽象 (7 KPs) — 零前置依赖, 教读/写/切换
  机制边界: Buffer.java 的四字段模型 — 不涉及 Heap/Direct 分化
  1. 四字段模型 + invariant
  2. clear() — 准备写模式
  3. get()/put() relative — 读写推进 position
  4. get(int)/put(int, byte) absolute — 绝对定位
  5. flip() — 切换到读模式
  6. compact() — 压缩未读
  7. rewind() — 重读

### Cluster B: 分配与内存 (5 KPs) — 依赖 A, 展开 Heap vs Direct
  机制边界: X-Buffer allocate 入口 → Heap vs Direct 两个 branch
  1. allocate(int) → HeapByteBuffer
  2. allocateDirect(int) → DirectByteBuffer
  3. Bits.reserveMemory — 总池限制
  4. Deallocator + Cleaner — Direct 释放
  5. wrap(byte[]) — 包裹现有数组

### Cluster C: 视图与 API 陷阱 (6 KPs) — 依赖 A+B, 理解共享 vs 拷贝
  机制边界: 从原始 buffer 派生新 buffer — 共享底层数据
  1. slice() — 子窗口
  2. duplicate() — 全量视图
  3. asReadOnlyBuffer() — 只读包装
  4. hasArray()/array() — 后端数组暴露
  5. isDirect()/isReadOnly() — 类型检测
  6. equals()/compareTo() — remaining() 范围

### Cluster D: 基础设施 (4 KPs) — 穿插在 A~C 中
  1. order() — 字节序
  2. mark()/reset() — peek 模式
  3. bulk get/put — 批量读写
  4. UNSAFE + address field + HotSpot 内联

### 教学顺序: A(核心抽象) → B(分配与内存) → C(视图与陷阱)
  D(基础设施) 穿插在 A~C 中, 非独立篇
