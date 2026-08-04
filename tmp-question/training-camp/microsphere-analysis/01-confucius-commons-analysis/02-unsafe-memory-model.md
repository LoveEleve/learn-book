# Unsafe 内存模型 —— Java 并发到底在 CPU 层面做了什么

> confucius-commons 源码对应：`UnsafeUtils.java`（1383 行）、`ReflectionUtils.java`
> JDK 11 对比：`VarHandle` 替代 `Unsafe`，`StackWalker` 替代 `sun.reflect`

---

## 一、你为什么需要 Unsafe

### 1.1 问题——`volatile` 不够精细

你用 `volatile` 修饰一个字段时，JVM 保证两件事：

1. **可见性**：一个线程写入 volatile 字段后，其他线程立即看到新值
2. **禁止重排**：volatile 写之前的操作不能被重排到 volatile 写之后

这是通过 **StoreLoad 内存屏障**（x86: `mfence`, ARM: `dmb ish`）实现的。这个屏障很贵——在 x86 上约 50-100 CPU 周期，在 ARM 上可能更高。

但有时候你只需要**保证写入顺序**，不需要**立即可见**。比如 Disruptor ring buffer 的 sequence 更新：

```java
// 生产者线程
ringBuffer[seq] = data;
cursor.set(seq);  // ← 只需要保证 ringBuffer[seq] 在 cursor.set(seq) 之前完成
                   //   不需要其他线程立即看到 cursor 的新值
```

在这个场景中，`volatile` 太贵了——你花了全套 StoreLoad 屏障的钱，但只需要 StoreStore 屏障。

`Unsafe` 提供了 `putOrderedInt()`——它只加 StoreStore 屏障（x86: 几乎免费，ARM: `dmb ishst`，约 20-30 周期）。比 `volatile` 便宜 2-5 倍。

### 1.2 JDK 为什么不让直接用 Unsafe

`Unsafe` 能做很多危险的事：直接操作内存、绕过类型检查、修改 final 字段。JDK 设计者不想让普通开发者误用，所以：

- 构造函数是 `private` 的
- 唯一的实例是 `private static final Unsafe theUnsafe`
- 官方文档上说「不要用这个类，将来会被移除」

但实际上，JDK 自己的 `java.util.concurrent` 包（AtomicInteger、ConcurrentHashMap、AQS）全都在用 Unsafe。它不会真的被移除——至少不会在 OpenJDK 支持的 Java 版本中移除——因为太多关键功能依赖它。

JDK 9+ 引入了 `VarHandle`（JEP 193）作为官方替代——但 `Unsafe` 仍然可用（只是需要 `--add-opens java.base/sun.misc=ALL-UNNAMED`）。

**Unsafe 是 JDK 的「逃逸舱」**--和 [§1 ClassLoader 内省](./01-classloader-introspection.md) §4 的「内省 vs 封装」是同一主题的不同维度。ClassLoader 的封装是「访问控制」（protected/private），Unsafe 的封装是「内存安全」（JMM + 类型安全）。两者都是 JDK 设计者为了安全而锁住、框架作者为了性能/诊断而需要绕过的。跨语言看：C/C++ 没有这层封装（直接内存访问，无 JMM），Rust 用 `unsafe` 关键字显式标记逃逸舱（和 Java 的 `Unsafe` 类异曲同工）。Java 选择「强封装 + 有限逃逸舱」--`Unsafe` 存在但不推荐，`VarHandle` 是官方替代。这是「安全优先于性能」的设计哲学，Unsafe 是它的妥协产物。

---

## 二、Unsafe 操作的基础——对象在内存中是怎么布局的

### 2.1 对象头和字段偏移

在 HotSpot JVM 中，一个 Java 对象在内存中的布局是：

```
[对象头 (Mark Word + Klass pointer)] [字段1] [字段2] [字段3] ...
 ↑                                    ↑
 对象起始地址                         字段1 的 offset（相对于对象起始地址）
```

**实际的字节级布局（64位 JVM，压缩指针开启）**：

```
对象元数据区（对象头，12 字节）:
  offset 0-7:   Mark Word（8 字节）— 锁状态、GC 标记、identity hashCode
  offset 8-11:  Klass Pointer（4 字节）— 指向方法区中类元数据的压缩指针
                （32-bit 压缩后最多可寻址 32GB 堆，64-bit 不压缩时为 8 字节）
字段数据区:
  offset 12-15: int field1       （4 字节，4 字节对齐）
  offset 16-19: int field2       （4 字节）
  offset 20-23: int field3       （4 字节）
  offset 24-31: long field4      （8 字节，8 字节对齐——JVM 会插入 4 字节 padding）
  offset 32-39: Object field5    （4 字节引用压缩或 8 字节不压缩）
  ...
  （如果对象没有任何字段，如 Object.class，对象头之后直接结束，总共 12-16 字节）
```

**64位 JVM 不压缩指针时**：

```
offset 0-7:   Mark Word（8 字节）
offset 8-15:  Klass Pointer（8 字节）——不压缩，可直接寻址所有内存
offset 16-19: int field1（4 字节）
offset 20-23: int field2（4 字节）
...
对象头 = 16 字节（比压缩时多 4 字节）
```

`Unsafe.objectFieldOffset(Field)` 返回的就是给定字段在这个布局中的起始偏移量。比如上面的 `int field1` 的 offset 是 16（64位不压缩）或 12（64位压缩指针开启）。

**为什么框架作者需要知道这些？**

因为你要手动计算内存地址来操作字段：

```java
// 字段相对于对象起始地址的偏移
long fieldOffset = unsafe.objectFieldOffset(MyClass.class.getDeclaredField("count"));

// 对象在堆内存中的实际地址
// （这不是 Java 的引用地址——是堆内存中的物理偏移）
// unsafe.getInt(myObj, 12)  → 读 offset 12 处的 4 个字节，作为 int 返回
```

你不需要知道对象本身的地址——JVM 在内部维护了对象引用到堆地址的映射。你只需要知道「字段在对象中的偏移」，Unsafe 自动完成 `&object + offset` 的计算。

### 2.2 数组的特殊布局

数组在内存中的布局稍有不同：

```
[对象头 (Mark Word + Klass pointer)] [数组长度] [元素0] [元素1] [元素2] ...
                                     ↑           ↑
                                     (固定偏移)   arrayBaseOffset = 对象头大小 + 4（数组长度）
                                                 arrayIndexScale = 元素大小（long=8, int=4, ...）
```

`arrayBaseOffset` 返回第一个元素（索引 0）的地址偏移。`arrayIndexScale` 返回相邻两个元素之间的地址差。

confucius-commons 在 `UnsafeUtils` 的 static 初始化中预计算了所有基本类型 + Object 数组的这两个值：

```java
// UnsafeUtils.java:121-139
LONG_ARRAY_BASE_OFFSET   = unsafe.arrayBaseOffset(long[].class);   // 16（64位JVM，压缩指针开启）
LONG_ARRAY_INDEX_SCALE   = unsafe.arrayIndexScale(long[].class);   // 8（long 占 8 字节）
INT_ARRAY_BASE_OFFSET    = unsafe.arrayBaseOffset(int[].class);    // 16
INT_ARRAY_INDEX_SCALE    = unsafe.arrayIndexScale(int[].class);    // 4（int 占 4 字节）
SHORT_ARRAY_BASE_OFFSET  = unsafe.arrayBaseOffset(short[].class);  // 16
SHORT_ARRAY_INDEX_SCALE  = unsafe.arrayIndexScale(short[].class);  // 2
// ... 对 byte、boolean、double、float、char、Object[] 都做了同样的事
```

有了这两个值，数组第 `i` 个元素的地址：

```
元素地址 = arrayBaseOffset + i * arrayIndexScale
```

这就是 `java.util.concurrent.atomic.AtomicIntegerArray` 和 `AtomicLongArray` 的底层原理。

---

## 三、offset 缓存——为什么每次反射会杀性能

### 3.1 调用链分析

`UnsafeUtils.putInt(object, "count", 42)` 内部做了什么？

```java
// 步骤1：查找字段的 offset
long offset = getObjectFieldOffset(object, "count");

// 步骤2：用 offset 直接写入内存
unsafe.putInt(object, offset, 42);
```

步骤1 的 `getObjectFieldOffset` 做了两件事：

```java
// 第1步：反射查找 Field 对象
Field field = FieldUtils.getField(type, "count", true);
// 内部：遍历 Class.getDeclaredFields() → O(n) 逐个比较名字 → 返回 Field 对象

// 第2步：JNI 调用获取字段偏移
long offset = unsafe.objectFieldOffset(field);
// 内部：JNI 调用 → HotSpot VM 查找 Klass（类元数据）→ 计算字段在对象布局中的偏移
```

两步都是**重量级操作**：
- `getDeclaredFields()` 每次调用都会创建新的 `Field[]` 数组（虽然底层会缓存类元数据，但数组本身需要分配内存）
- `objectFieldOffset()` 是 native 方法——每次调用都有 JNI 开销

如果你在 CAS 循环中每秒调用 10 万次 `putInt`，10 万次都做这两个重量级操作 = 性能灾难。

### 3.2 confucius-commons 的缓存方案

```java
// UnsafeUtils.java:103 + 1358-1368
private static final ConcurrentMap<String, Long> offsetCache = new ConcurrentHashMap<>();

protected static long getObjectFieldOffset(Object object, String fieldName) {
    Class<?> type = object.getClass();

    // 热路径：查缓存
    Long cached = getOffsetFromCache(type, fieldName);
    if (cached != null) return cached;

    // 冷路径：只在第一次访问这个字段时执行
    Field field = FieldUtils.getField(type, fieldName, true);
    long offset = unsafe.objectFieldOffset(field);
    putOffsetFromCache(type, fieldName, offset);
    return offset;
}
```

**关键细节**：

```java
// 缓存 Key = "全限定类名#字段名"
// UnsafeUtils.java:273-276
protected static String createOffsetCacheKey(Class<?> type, String fieldName) {
    return new StringBuilder(type.getName()).append("#").append(fieldName).toString();
}
```

**为什么必须用 `类名#字段名` 而非只有字段名？**

考虑这个场景：

```java
class Parent {
    private int count;   // offset = 16
}

class Child extends Parent {
    private int count;   // offset = 20  ← 同名但完全不同！
}
```

`Parent.count` 和 `Child.count` 在内存中的 offset 完全不一样（前者在父类字段区域，后者在子类字段区域）。如果只用 `"count"` 做 Key 而不用 `"Parent#count"` vs `"Child#count"`，缓存会返回错误的 offset。

**为什么用 Map 而不用字段常量？**

因为 `UnsafeUtils` 是一个**工具类**——它不能预知用户会对哪些类的哪些字段调用 `putInt`。业务代码可能对几百个不同的类调用，每个类可能有几十个字段。用 Map 做惰性计算 + 缓存是最灵活的策略——不需要提前声明哪些字段会被访问。

**这是「memoization」（记忆化）模式的典型应用**——把昂贵的计算结果（反射查找 Field + JNI 获取 offset）缓存，后续直接查缓存。热路径（查缓存）O(1)，冷路径（首次计算）只执行一次。这个模式在性能敏感代码里反复出现：Spring 的 `BeanUtils` 缓存 PropertyDescriptor、Jackson 缓存 `JsonSerializer` by type、Netty 缓存 `Recycler` 对象池。confucius-commons 的实现是其中最简洁的——`ConcurrentHashMap` + `computeIfAbsent` 保证线程安全，`类名#字段名` 作 Key 避免继承链同名冲突。

---

## 四、三种写入在 CPU 层面到底发生了什么

### 4.1 内存模型的基础——为什么 CPU 会重排你的代码

考虑这段代码：

```java
int a = 1;    // 操作1
int b = 2;    // 操作2
```

你期望 CPU 先执行操作1，再执行操作2。但 CPU 可能会：
- **把操作2提前到操作1之前**——因为操作2的数据已经在缓存中，而操作1的数据还在主存里
- **延迟写回操作1的结果**——CPU 把操作1放在写缓冲区中，先处理后面的读操作

这两种行为统称为**指令重排**。现代 CPU 这样做的原因是性能——在数据到达内存之前就执行后面的指令，不让 CPU 空转。

**内存屏障**就是 CPU 提供的「不要重排」的信号。

### 4.2 Plain 写入（无屏障）——最快的，也是你最不能依赖的

```java
unsafe.putInt(obj, offset, 42);   // ← 没有任何内存屏障
```

对应 CPU 指令：`mov [dest], 42`

```assembly
; x86 上的 Plain 写入:
mov eax, 42
mov [rbx + offset], eax   ; 把 42 写入 [对象基址 + 字段偏移]
; 注意：没有 mfence，没有 sfence，没有 lfence
; CPU 可以把这个写入延迟数百个周期
; CPU 可以把后面的读操作提到这个写前面
```

**什么时候能用 Plain 写入？**

只有当你能保证**没有其他线程同时读这个字段**时。比如：
- 对象刚刚创建，引用还没有被任何其他线程拿到
- 外部已有 `synchronized` 保护

### 4.3 Volatile 写入（StoreLoad 屏障）——JMM 语义的完整实现

```java
unsafe.putIntVolatile(obj, offset, 42);   // ← StoreLoad 屏障
```

对应 CPU 指令：`mov [dest], 42` + `mfence`（x86）

```assembly
; x86 上的 Volatile 写入:
mov eax, 42
mov [rbx + offset], eax   ; 写入内存
mfence                     ; ← 关键：StoreLoad 屏障
                            ; 1. 所有后续的 Load 和 Store 必须等这条写完成
                            ; 2. 所有之前的 Load 和 Store 必须在这条写之前完成
```

`mfence` 是 x86 上最重的屏障指令，通常 50-100 CPU 周期。

**ARM 上的 Volatile 写入**：

```assembly
; ARM 上的 Volatile 写入:
str w0, [x1, #offset]    ; 写入内存
dmb ish                   ; ← Data Memory Barrier, Inner Shareable
                           ; 作用等同于 x86 的 mfence
                           ; 但 ARM 是弱内存模型，这个屏障比 x86 更必要
```

**什么时候必须用 Volatile 写入？**

当写入之后**必须确保**其他线程立即看到时。比如「任务完成」标志位：

```java
// 工作线程
data = computeResult();
UnsafeUtils.putIntVolatile(this, "done", 1);  // ← 必须用 volatile！

// 等待线程
while (UnsafeUtils.getInt(this, "done") == 0) { }  // 自旋等待
int result = (int) UnsafeUtils.getObject(this, "data");
```

如果这里用 Plain 写入，即使工作线程已经执行了 `putInt(1)`，等待线程可能一直看到 `done == 0`（因为 StoreLoad 屏障缺失导致写入停留在 CPU 的写缓冲区中不传播到其他核）。

### 4.4 Ordered 写入（StoreStore 屏障）——性能与正确性的折中

```java
unsafe.putOrderedInt(obj, offset, 42);   // ← StoreStore 屏障
```

对应 CPU 指令：`mov [dest], 42` + `sfence`（x86，但 x86 TSO 模型下 `sfence` 几乎免费）

```assembly
; x86 上的 Ordered 写入:
mov eax, 42
mov [rbx + offset], eax   ; 写入内存
                           ; x86 TSO 模型：硬件已经保证 store ordering
                           ; JIT 编译器只需要插入一个编译器屏障（阻止 JIT 重排指令）
                           ; 不需要真实的 mfence 或 sfence
```

**x86 的优势——Total Store Ordering (TSO)**：

x86 处理器在硬件层面就已经保证了**Store 之间的顺序**：如果 CPU 先执行 `mov [addr1], a` 再执行 `mov [addr2], b`，硬件会确保 `[addr1]` 的写入先被其他核看到。

所以 `putOrderedInt` 在 x86 上**没有额外的 CPU 开销**——JIT 编译器只需要插入一个编译器屏障（防止 JIT 把两条写入指令重排），不需要生成真正的 CPU 屏障指令。

**ARM 的现实——弱内存模型**：

```assembly
; ARM 上的 Ordered 写入:
str w0, [x1, #offset]    ; 写入内存
dmb ishst                  ; ← Data Memory Barrier, Store-Store only
                            ; 作用：确保前面的 store 在后面的 store 之前完成
                            ; 不保证：store 对读端立即可见（没有 StoreLoad 屏障）
                            ; 开销：约 20-30 CPU 周期
```

**生产上的重要区别**：

如果你的代码在 x86 服务器上跑（如 Intel Xeon、AMD EPYC），`putOrderedInt` 接近免费——因为你只差一个编译器屏障。

如果同样的代码在 ARM 服务器上跑（如 AWS Graviton），`putOrderedInt` 每调用一次就花 20-30 个 CPU 周期——因为需要真正的 `dmb ishst` 屏障指令。

**这就是为什么「在 x86 上用 Ordered 替代 Volatile」是一个隐性 bug**：代码在 x86 上测试通过，上线到 ARM 实例后，程序行为不确定。

### 4.6 compareAndSwap —— Unsafe 的第三根支柱

Plain 写入、Volatile 写入、Ordered 写入都是「无条件写入」。但并发编程中最常用的操作是 **原子性的「比较并交换」**——CAS。

```java
// confucius-commons UnsafeUtils 间接使用 CAS 的场景
int oldValue = UnsafeUtils.getIntVolatile(this, "count");
int newValue = oldValue + 1;
// ← 问题：这里有一个 read-modify-write 的 gap！
//    如果另一个线程在这两行之间修改了 count，你会覆盖它的值
UnsafeUtils.putIntVolatile(this, "count", newValue);  // ← 丢失更新！
```

CAS 解决了这个问题：

```java
// Unsafe 的 CAS（confucius-commons 的 UnsafeUtils 虽然没有直接封装，
// 但它的 offset 缓存就是为 CAS 准备的）
long offset = UnsafeUtils.getObjectFieldOffset(this, "count");  // 从缓存拿 offset
int expected = counter.getIntVolatile(this, offset);
int newVal = expected + 1;
// 尝试原子更新：只有当 count 仍然是 expected 时才写入 newVal
// 如果 count 已经被其他线程改了，返回 false，需要重试
while (!unsafe.compareAndSwapInt(this, offset, expected, newVal)) {
    expected = counter.getIntVolatile(this, offset);
    newVal = expected + 1;
}
```

**CAS 的 CPU 指令**：

```assembly
; x86: lock cmpxchg（Compare and Exchange）
lock cmpxchg [rbx + offset], eax
; lock 前缀：锁定总线或缓存行，确保原子性
; 如果 [rbx+offset] == eax，则 [rbx+offset] = newVal，设置 ZF=1
; 如果 [rbx+offset] != eax，则 eax = [rbx+offset]，设置 ZF=0

; ARM: ldaxr + stlxr（Load-Acquire Exclusive + Store-Release Exclusive）
ldaxr w2, [x1]        ; 1. 加载当前值，标记地址为 exclusive
add w2, w2, #1         ; 2. 计算新值
stlxr w3, w2, [x1]    ; 3. 尝试存储：如果地址的 exclusive 标记还在，写入成功
cbnz w3, retry         ; 4. 如果失败（地址被其他核访问过），重试
```

x86 的 CAS 是一条原子指令（`lock cmpxchg`）。ARM 的 CAS 是两条指令（`ldaxr` + `stlxr`），中间如果其他核访问了同一地址，`stlxr` 会失败——这是 ARM 弱内存模型的基础形式。

**confucius-commons 的 UnsafeUtils 为什么没直接封装 CAS？**

因为 CAS 操作的调用方式非常依赖使用场景——你需要自己重试循环、自己计算新值。封装成 `casInt(obj, field, expected, newVal)` 只能做一次尝试，不够用。更好的封装方式是只提供 offset 缓存（这是 UnsafeUtils 做的），让调用方自己用 `unsafe.compareAndSwapInt` 做循环。

### 4.7 伪共享（False Sharing）——为什么要手动计算 offset

伪共享是 CPU 缓存架构特有的性能陷阱。它的核心问题是：**CPU 缓存以「缓存行」（cache line）为单位交换数据，不是以字节为单位**。

```
CPU 缓存行大小：x86 = 64 字节，ARM = 64 或 128 字节

假设你有两个字段：
class Counter {
    volatile long count1;   // offset 16-23（8 字节）
    volatile long count2;   // offset 24-31（8 字节）
}
// count1 和 count2 在同一个 64 字节缓存行中！
```

两个线程分别更新 `count1` 和 `count2`：
1. 线程 A 在 Core 0 更新 `count1` → Core 0 的缓存行被标记为 modified
2. 线程 B 在 Core 1 更新 `count2` → 但 `count2` 和 `count1` 在同一缓存行！
3. Core 1 必须先**invalid** Core 0 的缓存行，然后加载最新的值
4. Core 0 再次更新 `count1` → 又需要 invalid Core 1 的缓存行

两个线程更新的是**毫不相关的字段**，但因为它们在同一个缓存行中，CPU 的缓存一致性协议（MESI）强制它们互相失效对方的缓存——这就是**伪共享**。性能下降可达 10-20 倍。

**防御方法——字段填充（padding）**：

```java
class Counter {
    volatile long count1;           // offset 16
    long p1, p2, p3, p4, p5, p6, p7; // 7 × 8 = 56 字节填充
    volatile long count2;           // offset 80（count1 和 count2 在
    // 不同的 64 字节缓存行中！count1 在 [0,64) 行，count2 在 [64,128) 行）
}
```

JDK 8+ 引入了 `@Contended` 注解简化这个操作：

```java
class Counter {
    @Contended  // ← 自动插入填充，保证不和同一缓存行中的其他字段冲突
    volatile long count1;

    @Contended
    volatile long count2;
}
// JVM 启动参数：-XX:-RestrictContended（JDK 8 默认只允许 JDK 内部使用）
```

**为什么 confucius-commons 需要 offset 缓存支持填充字段？**

因为填充字段的 offset 和业务字段的 offset 在代码中看起来很乱——`count1` 的 offset 是 16，但 `count2` 的 offset 是 80（中间插了 56 字节填充）。没有 offset 缓存，你每次都要反射查。有了缓存，`UnsafeUtils.putLong(obj, "count1", val)` 自动拿正确的 offset。

### 4.8 AQS 中的 putOrderedInt —— JDK 源码最佳教学案例

`java.util.concurrent.locks.AbstractQueuedSynchronizer`（AQS）是 JDK 并发包的基石（ReentrantLock、Semaphore、CountDownLatch 都基于它）。AQS 内部大量使用 `putOrderedInt`：

```java
// AbstractQueuedSynchronizer.java（JDK 源码，简化）
public class AbstractQueuedSynchronizer {
    private volatile int state;  // 同步状态

    // 释放锁时：先用 putOrderedInt 更新 state，
    // 不需要其他核立即看到 release 的副作用
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        // ... 检查逻辑 ...
        // 注意：这里用的是 putOrderedInt，不是 putIntVolatile
        // 因为：① 当前线程独占锁，没有其他线程竞争
        //       ② 如果需要唤醒等待线程，会用 volatile 写（在 unparkSuccessor 中）
        //       ③ 这里只是更新自己的状态，不需要立即全局可见
        U.putOrderedInt(this, STATE, c);
        return true;
    }
}
```

**为什么 AQS 选择 `putOrderedInt` 而不是 `putIntVolatile`？**

AQS 的 `state` 字段有些场景下是**独占的**——获取锁成功的线程是唯一能修改 `state` 的线程。在这种情况下，不需要 volatile 写——没有竞争者能看到旧值。用 `putOrderedInt` 节省了不必要的 StoreLoad 屏障。当确实需要全局可见性时（如 `unparkSuccessor` 唤醒等待线程），AQS 会用 `putIntVolatile`。

这是 `putOrderedInt` 的真实使用场景——**在确定没有数据竞争时，选择更便宜的写入语义**。

### 4.9 Unsafe 数组访问——无边界检查的陷阱

```java
// 正常 Java 数组——有边界检查
int[] array = new int[10];
int val = array[10];  // → ArrayIndexOutOfBoundsException

// Unsafe 数组访问——无边界检查
long baseOffset = unsafe.arrayBaseOffset(int[].class);
int indexScale = unsafe.arrayIndexScale(int[].class);
// 读索引 10：合法数组只有 0-9，但 Unsafe 不会抛异常
int val = unsafe.getInt(array, baseOffset + 10 * indexScale);
// → 读取 array[10] 的「位置」——实际是数组对象之后的内存区域
//    可能读到其他对象的字段、JVM 内部数据、或触发 SIGSEGV
```

**计算数组边界**：

```java
// 数组最后一个元素的地址
long lastElementAddress = baseOffset + (array.length - 1) * indexScale;
// 验证索引
if (index < 0 || baseOffset + index * indexScale > lastElementAddress) {
    throw new ArrayIndexOutOfBoundsException();
}
```

confucius-commons 的 `ReflectionUtils.assertArrayIndex()` 就是在 Unsafe 操作前做这个验证——虽然 Unsafe 不做边界检查，但你的代码应该在调用 Unsafe 前自己做。

```java
// 生产者线程
ringBuffer[index] = data;                           // Plain 写入——数据
UnsafeUtils.putOrderedInt(cursor, "seq", index);    // Ordered 写入——消费者信号

// 消费者线程
int currentSeq = UnsafeUtils.getInt(cursor, "seq"); // Plain 读取
if (currentSeq >= expectedSeq) {
    Data data = ringBuffer[currentSeq];              // Plain 读取
}
```

**x86 上的行为**（正确）：
- x86 TSO 保证 Store-Store 顺序 → `ringBuffer[index]` 的写入先于 `seq` 的写入被其他核看到
- 消费者读到 `seq >= expected` 时，`ringBuffer[index]` 一定已经写入——读到的数据是正确的

**ARM 上的行为**（可能出错）：
- `putOrderedInt` → `dmb ishst` 只保证生产者的两个写入之间的顺序
- 但不保证消费者读到的 `ringBuffer[index]` 和 `seq` 的顺序
- 消费者可能读到新 `seq`，但 `ringBuffer[index]` 读到旧数据

**正确修复**：
- 消费者的读取应该是 `getIntVolatile(cursor, "seq")`——加 StoreLoad 屏障
- 或者用 `VarHandle` 的 `acquire/release` 语义

---

## 五、JDK 11 的 VarHandle——Unsafe 的官方替代

### 5.1 Unsafe 在 JDK 11 上的状态

JDK 11 中 `Unsafe` 仍然存在，但：
- 标记为 `@Deprecated(since="9")`
- 默认 `--illegal-access=warn`（JDK 9-15）：能用，但会在控制台打印警告
- `--illegal-access=deny`（JDK 16+）：会抛出 `InaccessibleObjectException`

要在 JDK 17+ 上用 `Unsafe`，你需要：
```bash
--add-opens java.base/sun.misc=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED
```

### 5.2 VarHandle——JDK 9+ 的官方方案

```java
// JDK 6 的 confucius-commons 写法
UnsafeUtils.putIntVolatile(myObj, "count", 42);
int value = UnsafeUtils.getInt(myObj, "count");

// JDK 11 的 VarHandle 写法
VarHandle COUNT = MethodHandles.lookup()
    .findVarHandle(MyClass.class, "count", int.class);
COUNT.setVolatile(myObj, 42);
int value = (int) COUNT.get(myObj);
```

**VarHandle 的优势**：
1. **类型安全**：`findVarHandle` 时就绑定了类型（`int.class`），后续操作不会写错类型
2. **不需要 offset 缓存**：VarHandle 内部已经缓存了 offset
3. **不需要 `setAccessible(true)`**：不会触发模块系统限制
4. **提供所有 Unsafe 的内存语义**：`set()`（Plain）、`setVolatile()`、`setRelease()`（Ordered）、`setOpaque()`（Opaque）
5. **不需要 Unsafe 对象**：不需要反射获取 `theUnsafe` 字段

**VarHandle 的内存语义对应**：

| confucius-commons Unsafe | VarHandle | 语义 |
|---|---|---|
| `putInt(obj, field, val)` | `handle.set(obj, val)` | Plain |
| `putIntVolatile(obj, field, val)` | `handle.setVolatile(obj, val)` | Volatile (StoreLoad) |
| `putOrderedInt(obj, field, val)` | `handle.setRelease(obj, val)` | Release (StoreStore) |
| `getInt(obj, field)` | `handle.get(obj)` | Plain |
| `getIntVolatile(obj, field)` | `handle.getVolatile(obj)` | Volatile |
| — | `handle.getAcquire(obj)` | Acquire (LoadLoad+LoadStore) |
| — | `handle.setOpaque(obj, val)` | Opaque（仅保证原子性，不保证顺序） |

**新语义——Acquire 和 Release**：

```java
// Release（写端）：前面的 Store 不能重排到后面，后面的 Load 可以重排到前面
handle.setRelease(obj, 42);

// Acquire（读端）：后面的 Load 不能重排到前面，前面的 Store 可以重排到后面
int value = (int) handle.getAcquire(obj);
```

这对 `Release/Acquire` 比 `Ordered/Volatile` 更高效——减少了不必要的屏障。Disruptor 正是用这对语义实现高性能 ring buffer 的。

### 5.3 性能对比——VarHandle vs Unsafe

在 JDK 11 上，VarHandle 的性能和 Unsafe 几乎相同——因为 JVM 内部 VarHandle 就是用 Unsafe 实现的。区别只在几个方面：

| | Unsafe | VarHandle |
|---|---|---|
| 调用开销 | JNI 调用（第一次慢，JIT 后内联） | MethodHandle dispatch（JIT 后内联） |
| 类型检查 | 无（可以写坏内存） | 有（findVarHandle 时绑定类型） |
| JDK 兼容性 | 需要 `--add-opens` | 不需要 |
| 代码复杂度 | 简单（只需 import Unsafe） | 略复杂（需要 MethodHandles.lookup()） |

**结论**：如果你在 JDK 11+ 上写新代码，用 VarHandle。如果你在维护 JDK 8 的老代码，继续用 Unsafe。不要因为「Unsafe 被废弃」就恐慌——JDK 内部的 `AtomicInteger` 到 JDK 21 仍在用 Unsafe。

---

## 六、面试要点——这一节的三个高频问题

**Q1：「volatile 写和 putOrdered 写有什么区别？」**

答：volatile 写生成 StoreLoad 屏障（x86: mfence），保证写入对所有核立即可见，但最贵。putOrdered 只生成 StoreStore 屏障（x86: 编译器屏障，ARM: dmb ishst），保证写入顺序但不保证可见性。使用场景：volatile 用于其他线程需要立即读取的标志位，Ordered 用于只需保证写入顺序的 ring buffer sequence 等。

**Q2：「为什么在 x86 上用 Ordered 代替 Volatile 的代码可能没问题，但换到 ARM 就崩？」**

答：x86 是 TSO 模型，硬件已保证 Store-Store 有序，Ordered 在 x86 上只需编译器屏障（几乎免费）。ARM 是弱内存模型，Ordered 需要真实的 `dmb ishst` 指令，而且不保证读端可见性。如果读端用普通读（无屏障），可能读到旧值。

**Q3：「VarHandle 和 Unsafe 性能有差别吗？」**

答：几乎没有。JVM 内部 VarHandle 就是基于 Unsafe 实现的。区别在于 VarHandle 有类型检查（编译期绑定），Unsafe 没有（运行时内存安全全靠开发者自觉）。
