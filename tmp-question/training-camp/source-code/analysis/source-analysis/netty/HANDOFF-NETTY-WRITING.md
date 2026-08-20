# Netty 正文写作交接文档

> 交接对象：下一个 AI
> 当前阶段：Netty 正文写作
> 当前框架：Netty
> 正文目录：`/data/workspace/source-code/openjdk-book/docs/openjdk/vol-netty/`
> 源码：`/data/workspace/source-code/code/spring/netty/`
> 方法论：`/data/workspace/source-code/openjdk-book/docs/openjdk/WRITING-GUIDELINES.md`
> 正文写作方法论：`/data/workspace/source-code/openjdk-book/docs/openjdk/WRITING-METHODOLOGY.md`
> 正文依赖图谱：`/data/workspace/source-code/openjdk-book/docs/openjdk/WRITING-DEPENDENCY-GRAPH.md`

## 一、最重要的当前状态

Netty 的域规划和大纲已经完成，当前进入正文写作阶段。

正文顺序不能从 Ch4 开始，必须先写 NIO 前置知识：

```text
Ch1 ByteBuffer
  -> Ch2 Channel
  -> Ch3 Selector
  -> Ch4 ByteBuf
  -> Ch5 EventLoop
  -> Ch6 Promise/Future
  -> Ch7 Pipeline/Handler
  -> Ch8 MemoryPool
  -> Ch9 Bootstrap
  -> Ch10 Codec
  -> Ch11 HTTP Codec
  -> Ch12 HTTP/2 Codec
  -> Ch14 HashedWheelTimer
```

Ch13 Epoll 按既有规划跳过，不重新开域。

## 二、已完成正文

目录：`/data/workspace/source-code/openjdk-book/docs/openjdk/vol-netty/`

### Ch1 ByteBuffer

已完成 3 篇正文：

1. `ch1-bytebuffer/01-core-abstraction.md`
2. `ch1-bytebuffer/02-heap-vs-direct.md`
3. `ch1-bytebuffer/03-views-and-traps.md`

每篇都对应一个篇级规划和 review 记录：

- `01-core-abstraction.rewrite-plan.md`
- `01-core-abstraction.review-notes.md`
- `02-heap-vs-direct.rewrite-plan.md`
- `02-heap-vs-direct.review-notes.md`
- `03-views-and-traps.rewrite-plan.md`
- `03-views-and-traps.review-notes.md`

### Ch2 Channel

已完成 1 篇正文：

1. `ch2-channel/01-read-write.md`

对应规划和 review：

- `ch2-channel/01-read-write.rewrite-plan.md`
- `ch2-channel/01-read-write.review-notes.md`

当前正在处理 Ch2 的下一篇：

- 目标：`ch2-channel/02-connect-accept.md`
- 当前状态：尚未写正文
- 下一步：先读取既有大纲 `source-analysis/netty/outlines/ch2-channel/02-connect-accept.md`，再写篇级 `rewrite-plan`，之后才写正文。

## 三、已完成正文的核心内容

### Ch1-01：Buffer 四字段状态机

主线：

```text
byte[] 没有读写边界
  -> mark/position/limit/capacity
  -> flip：写边界变读边界
  -> compact：保留未读半包
  -> clear/rewind/mark-reset
  -> Netty readerIndex/writerIndex 的后续演进
```

重要修正：

- Buffer 不变量必须写成 `-1 <= mark <= position <= limit <= capacity`
- `mark=-1` 表示没有 mark，不能写成 `0 <= mark`
- `flip` 的“三个动作一起发生”是完整状态迁移的语义，不是并发原子性承诺

### Ch1-02：HeapBuffer 与 DirectBuffer

主线：

```text
HeapBuffer -> Java byte[] -> GC 管对象生命周期
DirectBuffer -> native address -> Cleaner/Deallocator/Bits
```

已明确：

- DirectBuffer 不是绝对零拷贝，只是减少某些 native IO 路径的中间复制机会
- `Cleaner` 释放依赖对象不可达与引用处理，不是确定性业务释放
- `Bits.reserveMemory` 有 direct buffer 容量限制和失败退避
- `MaxDirectMemorySize` 限制 direct buffer 总 capacity，不是所有 native memory
- `wrap` 是共享原数组，不是复制
- 没有沿用旧大纲里“默认 MaxDirectMemorySize 简单等于 Xmx”的过度表述

### Ch1-03：ByteBuffer 视图与陷阱

主线：

```text
共享底层数据
  -> slice：相对窗口 + 独立状态
  -> duplicate：完整视图状态复制
  -> read-only：共享数据但禁止写入口
  -> hasArray/array 能力分叉
  -> equals/compareTo 只比较 remaining
  -> order / bulk get-put / mark-reset
```

已明确：

- 视图复制状态，不复制底层存储
- read-only 权限隔离不等于线程安全
- `hasArray()==false` 不等于不可读，只表示没有可暴露的 byte[]
- `bulk get/put` 仍是相对操作，会推进 position
- `mark/reset` 只有一层标记，不是多层 undo

### Ch2-01：Channel 读写

主线：

```text
read/write
  -> 阻塞/非阻塞
  -> 正数/0/-1 状态信号
  -> Buffer position 保存部分进度
  -> readLock/writeLock 同向互斥、异向并行
  -> IOStatus 底层状态归一化
```

已明确：

- 非阻塞 read 返回 0 是“暂时不可用”信号，不是异常或 EOF
- write 返回 0 时 position 不推进，remaining 数据必须保留
- `IOStatus.normalize` 只把 `UNAVAILABLE(-2)` 转为 0，不能把 EOF(-1) 写成 0
- `readLock` 与 `writeLock` 独立，同方向互斥，读写方向可并行
- Selector、connect/accept、EventLoop 只作为后续桥接，没有提前展开

## 四、Netty 规划与大纲位置

Netty 域规划根目录：

`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/netty/`

重要文件：

- `HANDOFF.md`：Netty 域规划总交接
- `00-book-plan.md`：Ch1-Ch14 全局章节规划
- `outlines/ch1-bytebuffer/`：Ch1 三篇大纲
- `outlines/ch2-channel/`：Ch2 三篇大纲
- `outlines/ch3-selector/`：Ch3 两篇大纲
- `outlines/ch4-bytebuf/`：Ch4 五篇大纲
- `outlines/ch5-eventloop/`：Ch5 四篇大纲
- 后续 `ch6`～`ch14` 均已有大纲，详见 `HANDOFF.md`

Ch2 下一篇应读取：

`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/netty/outlines/ch2-channel/02-connect-accept.md`

## 五、正文写作必须遵守的方法论

### 正式规范

`/data/workspace/source-code/openjdk-book/docs/openjdk/WRITING-GUIDELINES.md`

关键要求：

- 不能直接“读源码 → 写正文”
- 先做素材提取
- 再做读者理解路径设计
- 最后叙事写作
- 正文按“问题 → 失败方案 → 顿悟 → 机制 → 回收”推进
- 代码只能当证据，不能当文章骨架
- 代码块必须来自真实源码
- 写作时所有行号必须重新 grep/sed 验证
- 文章必须明确版本边界
- 必须做删码测试、陌生人测试、反向提纲测试、禁用词扫描

### 正文方法论

`/data/workspace/source-code/openjdk-book/docs/openjdk/WRITING-METHODOLOGY.md`

### 正文依赖图

`/data/workspace/source-code/openjdk-book/docs/openjdk/WRITING-DEPENDENCY-GRAPH.md`

### BIN 技术小屋参考

`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/BIN技术小屋/netty/`

它是表达风格参考，主要提供：

- 场景化开头
- 问题驱动
- 图解化解释
- 先人话，再源码
- 从现象打到机制

它不是正式方法论的替代品。

## 六、每篇正文的固定执行流程

### 1. 读取大纲

读取目标篇的：

- `source-analysis/netty/outlines/<chapter>/<article>.md`
- 该章 `completeness-questions.md`
- 相关 `00-book-plan.md`

### 2. 建立篇级 rewrite-plan

写入正文同目录：

`<article>.rewrite-plan.md`

至少包含：

- HARD/SOFT/NAV 前置依赖
- 一句话困惑
- 一句话顿悟
- 理解路径
- 失败方案
- 误解清单
- 字数预算（非硬门槛）
- 证据清单
- 深审清单

### 3. 写正文

写入：

`/data/workspace/source-code/openjdk-book/docs/openjdk/vol-netty/`

不能写入：

- `vol-02`
- `vol-java`
- Netty 的 `outlines` 目录
- 其他框架目录

### 4. 六轮 review

1. 事实审：类名/路径/版本/行号/代码块
2. 因果审：为了/因此/保证/避免是否有证据
3. 结构审：是否按读者理解路径而非源码文件顺序
4. 读者审：删码、陌生人、反向提纲测试
5. 边界审：并发/失败/资源释放/线程安全/版本
6. 依赖审：前置/后续/跨域/知识脑图边方向

review 记录写入：

`<article>.review-notes.md`

## 七、当前下一步

不要重新规划 Ch1，也不要跳到 Ch4。

下一步是：

1. 读取 `outlines/ch2-channel/02-connect-accept.md`
2. 为 `vol-netty/ch2-channel/02-connect-accept.md` 写篇级 `rewrite-plan`
3. 重新 grep JDK 11 SocketChannel/ServerSocketChannel 源码锚点
4. 写正文
5. 六轮深审

## 八、不要做的事

- 不要重新规划已经完成的 Ch1-Ch14 大纲
- 不要从 Ch4 ByteBuf 开始，Ch1-Ch3 是前置知识
- 不要把正文写进 `vol-02` 或 `vol-java`
- 不要把 OpenJDK 正文与 Netty 正文混在一起
- 不要把 8000 字当硬指标，优先保证闭环深度和广度
- 不要批量写多篇正文
- 不要为了补字数制造源码未证实的性能数字
- 不要把 Netty 后文实现提前写成当前 NIO 事实

## 九、当前已知注意事项

- `SocketChannelImpl.beginRead` 在当前 JDK 11 源码中主要登记 interrupt hook 与 reader thread，不能凭旧版本/旧大纲说成它直接调用 `LockSupport.park()`；写 Ch2 后续篇时必须重新看源码。
- `IOStatus.normalize` 的真实代码只把 `UNAVAILABLE` 转为 0，其他负值保留。
- Ch1 正文已经修正过一次 Buffer 不变量：必须允许 `mark=-1`。
- 任何大纲行号都只是线索，正文写作必须重新核对。
