# §1.1 ByteBuffer 核心抽象 — 文章大纲

## 读者基线

读者知道 `byte[]`、基本 I/O 概念（InputStream/OutputStream）、Java 对象创建。不知道 NIO 的任何概念。

## 概念依赖链

```
byte[] 的局限 → mark/position/limit/capacity 四字段模型 → invariant 不变式 → flip/rewind/clear 三件套 → compact 写回操作 → absolute vs relative get/put → read-only buffer
```

## 叙事顺序

**开篇场景（用 `byte[]` 的痛）**

```java
byte[] buf = new byte[1024];
int n = socket.getInputStream().read(buf);
// 读了 n 个字节——但下一次 read 从哪里开始写？buf[0] 还是 buf[n]？
// byte[] 不知道"已经写了多少"——你得自己记。
```

一个最简单的网络读操作就暴露了 `byte[]` 的致命缺陷：它只有容量，没有**位置**。`byte[] buf` 不知道你已经写了多少、读了多少。每一轮读写都要手动维护一个 `int offset`——忘记更新就是覆盖旧数据或者读到脏数据。

**第一层：Buffer 的四字段**

如果让缓冲区自己记住"读到哪里"和"写到哪里"会怎样？Java NIO 在 `Buffer.java:198-201` 定义了四个字段：

```
mark      — 书签位置（配 mark()/reset() 使用，未设置时为 -1）
position  — 当前操作位置（读写都走这里）
limit     — 有效数据的边界（不能越过这里读）
capacity  — 底层数组的总容量（不变）
```

这四个字段之间有一个**严格的不变式**（`Buffer.java:102-107`）：

```
0  <=  mark  <=  position  <=  limit  <=  capacity
```

这是整个 NIO Buffer 设计的地基。所有 `flip()`、`clear()`、`rewind()`、`mark()`、`reset()` 的语义都从这个不变式推导出来——每个方法操作后这四个字段必须仍满足不变式。

用代码演示四字段的初始状态和读写推进：

```java
ByteBuffer buf = ByteBuffer.allocate(8);
// [mark=-1, pos=0, lim=8, cap=8] — 初始: mark 未设, position 在起点

buf.put((byte) 1);  // pos=1, lim=8, cap=8
buf.put((byte) 2);  // pos=2, lim=8, cap=8
// [mark=-1, pos=2, lim=8, cap=8] — 写了 2 个字节
```

**第二层："重置三件套"——flip / rewind / clear**

写完了，想从 buffer 中读数据。position 指向 2，但读应该从 0 开始。三种重置方式：

| 方法 | position | limit | mark | 什么时候用 |
|------|:--:|:--:|:--:|------|
| `flip()` | → 0 | → position | → -1 | 写完了准备读 |
| `rewind()` | → 0 | 不变 | → -1 | 重读一遍（limit 不动） |
| `clear()` | → 0 | → capacity | → -1 | 准备重新写（limit 拉满） |

源码对照（`Buffer.java`）：

```java
// Buffer.java:449-454
public Buffer flip() {
    limit = position;     // 之前写入的数据就是有效数据的边界
    position = 0;         // 从头开始读
    mark = -1;            // 丢弃书签
    return this;
}

// Buffer.java:471 — rewind: 只重置 position, limit 不动
// Buffer.java:421-426 — clear: position=0, limit=capacity, 底层数据不清理
```

三个方法都是改指针，不碰底层数组。**rewind() 常被忽略——但它是"重读"场景的正确选择**，用 flip() 反而会丢失之前设的 limit。

**第三层：flip 的陷阱**

`flip()` 的根源问题：**position 是一把单刀，却要在读和写两个战场上使用**。每个操作都在隐式推进 position：

```java
buf.put(data);     // 写——position 推进
buf.flip();        // 切到读——limit=position, position=0
// ... 读数据 ...
buf.compact();     // 切回写——把未读数据移到开头
// ... 继续写 ...
```

陷阱清单：
- 写了一堆数据，忘记 `flip()` 就开始 `get()` → 读到垃圾
- 读了一半，需要继续写——不能用 `clear()`（会把未读数据"清理"掉），必须用 `compact()`
- `clear()` 的命名误导人：只改指针，不清数据。它的语义是"准备写入"，不是"清空数据"
- 连接复用时，上一次 read 推进的 position 污染下一次 write——因为 position 是共享状态

**第四层：compact——昂贵的"切回写"**

`flip()` 只需要改指针（零拷贝），但 `compact()` **不是**——它把未读数据从 position..limit 复制到数组 0..(limit-position)，然后设 `position = 复制后的长度`。`compact()` 是唯一有内存拷贝的控制操作。

```
compact() 内部做的事（HeapByteBuffer 实现）:
  System.arraycopy(hb, position, hb, 0, remaining());  // 拷贝
  position = remaining();
  limit = capacity;
```

读了一部分写一部分的场景下，每次 flip→compact 的代价是 O(剩余字节) 的拷贝。这是后面 Netty 要解决的核心问题之一——但此时不讲。

**第五层：absolute vs relative——两套操作**

`Buffer.java:67-77` 区分了两种 get/put：

| 操作类型 | 方法签名 | 是否动 position | 什么时候用 |
|------|------|:--:|------|
| Relative | `get()` / `put(byte)` | ✅ 推进 | 顺序读写（最常见） |
| Absolute | `get(int index)` / `put(int index, byte)` | ❌ 不动 | 随机访问中间某个字节 |

```java
buf.put((byte) 1);           // relative: pos=1
buf.put((byte) 2);           // relative: pos=2
buf.put(5, (byte) 99);       // absolute: 写 index=5, pos 仍是 2
byte b = buf.get(5);         // absolute: 读 index=5, pos 仍是 2
```

Absolute 操作存在的原因：有时候你需要读/写 buffer 中间某个位置，但不想破坏当前的 position 状态——比如解析协议头。

**第六层：asReadOnlyBuffer——防御式共享**

如果上游给你一个 buffer，你只想读、不想改——`asReadOnlyBuffer()`。返回的 buffer 共享同一块底层内存（零拷贝），但 `put()` 抛 `ReadOnlyBufferException`。设计权衡：零拷贝共享 vs 数据安全。这种"共享视图但限制操作"的模式在 ByteBuffer 中反复出现——`duplicate()` 和 `slice()` 也共享数据但各自独立操作位置，§1.3 将揭露这些视图的完整陷阱。

**结尾悬念**

四字段解决了"读写到哪里"的问题。但缓冲区本身分配在哪里？`allocate(1024)` → JVM 堆上，GC 管。`allocateDirect(1024)` → 堆外，GC 管不到。那堆外内存谁来释放？下一节。

## 核心悬念

**读完能回答**：`byte[]` 已经有容量语义，为什么还要四个字段（mark/position/limit/capacity）？flip/rewind/clear 三个重置操作有什么区别？为什么 compact 比 flip 贵？position 的单刀共用是这个 API 所有陷阱的根源——那有没有更好的设计？

## 文章边界

- **在本篇内**: byte[] → 四字段 + 不变式 → flip/rewind/clear → flip陷阱 + compact → absolute/relative → read-only → 引出内存分配悬念
- **不属于本篇**: Direct 内存管理（§1.2）、Channel 与 ByteBuffer 的协作（§2.3）
