# §1.3 ByteBuffer 的视图与剩余陷阱 — 文章大纲

## 读者基线

已从 §1.1 掌握四字段模型和不变式、从 §1.2 了解 Heap/Direct 分配路径。已理解 flip/compact/clear 和内存管理——但不知道 ByteBuffer 还有"共享式视图"和"API 比较陷阱"。

## 概念依赖链

```
§1.1 的不变式 (0≤pos≤lim≤cap) → duplicate/slice 视图的共享语义 → wrap 与 allocate 差异 → equals 比较陷阱 → 线程不安全的隐式共享 → 读者已懂 ByteBuffer 全貌 → 引出 Channel
```

## 叙事顺序

**开篇场景：一个不该出现的 bug**

上两节理解了 ByteBuffer 的四字段模型和内存分配路径。有了这些基础，现在可以用 `duplicate()` 来"复制"一个 buffer 用于读取。但 `duplicate()` 的行为和它的名字不一样——

```java
ByteBuffer original = ByteBuffer.allocate(16);
original.put("hello".getBytes());       // original.position 推进到 5

ByteBuffer view = original.duplicate(); // 复制时的快照: view.position=5, view.limit=16
view.flip();                            // view.limit=5 (创建时的 position), view.position=0
original.clear();                       // 只动 original 的位置, view 不受影响
original.put("world!".getBytes());      // 写 original.hb[0..5] — 共享的底层数组被覆盖

view.get(new byte[5]);                  // 期望 "hello", 实际 "world"
```

读者本能：`duplicate()` = 复制一份数据。实际：**`duplicate()` 共享底层数组**。更隐蔽的是：`duplicate()` 复制的是**创建时那一刻** position/limit 的快照——之后 original 再怎么动都不影响 view 的位置。但底层数组只有一个，original 写进去的数据会立刻覆盖 view 看到的内容。这不是数据竞争的 race condition——是 API 设计的隐式陷阱。

**第一层：视图的两种语义——slice vs duplicate**

在 `X-Buffer.java.template:553-601` 中定义的两种视图：

| 方法 | 数据共享 | position/limit 独立 | 什么时候用 |
|------|:--:|:--:|------|
| `duplicate()` | ✅ 共享底层数组 | ✅ 位置独立 | 并发读取同一个 buffer 的不同段 |
| `slice()` | ✅ 共享底层数组 | ✅ 位置独立 | 只看 buffer 中 [position, limit) 这一段 |

两者都不是 `copy()`——`copy()` 是新建独立 buffer。但 API 命名上 `duplicate` 暗示"复制"，`slice` 暗示"切片"——前者比后者更容易被误解。

`slice()` 的实现（`Heap-X-Buffer.java.template:104-114`）基于**当前** `position` 和 `limit` 创建视图——切的是 `[position, limit)` 这一段。关键是：如果 position 不在 0（比如已经写了一些数据），`slice()` 不会从数组开头切。`slice()` 之后即使 original 的 position 移动了，这个视图仍然只看当初的那一段。和 `duplicate()` 一样——**创建时的快照**。它们共享的不仅是数据——连数据的规模都是快照时锁定的。

**第二层：wrap()——不"分配"的分配**

```java
byte[] data = "hello".getBytes();
ByteBuffer buf = ByteBuffer.wrap(data);  // 不是 allocate! 直接包住已有 byte[]
buf.put(0, (byte) 'H');                  // 改了 buf
System.out.println(new String(data));    // "Hello" — 共享数组
```

`wrap()` (`X-Buffer.java.template:421`) 和 `allocate()` 的区别：
| 操作 | 分配新数组 | 初始 position | 初始 limit |
|------|:--:|:--:|:--:|
| `allocate(8)` | ✅ `new byte[8]` | 0 | capacity |
| `wrap(data)` | ❌ 复用传入的 `byte[]` | 0 | data.length |
| `wrap(data, off, len)` | ❌ 复用传入的 `byte[]` | off | off+len |

`wrap()` 的陷阱是——你传入了一个外部拥有的 `byte[]`，但返回的 ByteBuffer 让你可以修改它。如果外部代码也持有这个 `byte[]`，就是隐式的数据共享。和 `duplicate()` 不同，代码上看不到"视图"这个信号——`ByteBuffer.wrap(data)` 看起来像是"创建一个 buffer 包含 data 的内容"，但实际是"创建一个 buffer **引用** data 本身"。

**第三层：equals()——只比较 "remaining" 字节**

`X-Buffer.java.template:1307-1322` 的 `equals()` 实现：

```java
public boolean equals(Object ob) {
    int thisRem = this.limit() - this.position();   // remaining
    int thatRem = that.limit() - that.position();   // remaining
    if (thisRem != thatRem) return false;
    return mismatch(this, this.position(), that, that.position(), thisRem) < 0;
}
```

关键：比较的是 **`remaining()` 字节**——`limit - position`。不是比较 `capacity`，不是比较 `[0, capacity)`：

```java
ByteBuffer a = ByteBuffer.allocate(8);
a.put((byte) 1); a.put((byte) 2); a.flip();  // remaining=2

ByteBuffer b = ByteBuffer.allocate(8);
b.put((byte) 1); b.put((byte) 2);             // remaining=6, b 没有 flip

a.equals(b);  // false — remaining different
// 但 a.get() 和 b.get() 的第一个字节都是 1 — 语义上"数据相同"但不相等
```

`compareTo()`（line 1347-1350）同理——字典序比较 `remaining` 字节。

这个陷阱在生产中最常出现：你 flip 了一个 buffer 准备读，另一个代码路径用一个 `clear` 回到写模式的 buffer——两个 buffer 内容相同但 `remaining` 不同，`equals()` 返回 `false`。

**第四层：线程不安全——最后的隐藏**

ByteBuffer **没有线程安全保护**。两个线程同时操作一个 ByteBuffer：
- 线程A: `buf.get()` → 推进 position
- 线程B: `buf.put((byte) 42)` → 也推进 position
- 结果：两个位置状态交叉污染

加上视图的共享特性后问题更严重。DirectByteBuffer 的线程不安全尤为危险——§1.2 讲过它是通过 `UNSAFE.putByte(address+index)` 直接操作堆外原生内存。多个线程同时 `putByte` 到重叠的地址范围，导致的不是 JVM 的 `NullPointerException`，而是**原生内存损坏**——可能在任意时刻表现为 `SEGFAULT` 或者静默数据错误。HeapByteBuffer 至少还有 JVM 的越界检查和崩溃定位，Direct 的损坏几乎无法追踪。

**结尾悬念**

读者已经了解了 ByteBuffer 的全貌：四字段模型让他知道"数据在哪"、Heap/Direct 让他知道"数据怎么分配"、视图和陷阱让他知道"数据被谁分享"。但所有这些都只是工具——还不够。如果我有一个服务端端口、客户端连上来，什么时候该读数据、怎么写数据、怎么不阻塞地接收 1000 个连接？这一切需要一个新的抽象。下一章。

## 核心悬念

**读完能回答**：`duplicate()` 和 `slice()` 的数据共享是如何通过在底层存储引用上实现的？`wrap()` 的"复用传入数组"比 `duplicate()` 更隐蔽——为什么？`equals()` 比较 `remaining` 而非 `capacity` 会导致什么真实的比较 bug？ByteBuffer 为什么不保证线程安全？

## 文章边界

- **在本篇内**: duplicate/slice 共享语义 → wrap 复用陷阱 → equals 比较 remaining → 线程不安全 → 告别 ByteBuffer → 引出 Channel
- **不属于本篇**: Channel 的 read/write（§2.1-2.3）、Netty 的 ByteBuf 视图（§4.3）
