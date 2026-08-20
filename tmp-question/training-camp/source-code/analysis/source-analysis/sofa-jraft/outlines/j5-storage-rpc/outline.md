# J-5 存储与 RPC — 日志的仓库与传送带: 双介质分流与可插拔传输

> 前置: [[J-2-日志复制]] (LogStorage 接口消费) + [[J-3-快照压缩]] (截断联动) | 引出: 阶段5 Nacos (JRaftProtocol 存储复用) | 对照: Kafka 分段日志 + gRPC/Bolt 双实现
> 🟡 B | 16 KP | [模式: 阈值分流 + 预分配 + SPI]
> Pass 2 闭环: q1(双介质) q2(恢复) q3(SPI) q4(pipeline)

**读者处境**: 日志条目最终存在哪? 为什么新存储把"大日志"和"小日志"分开? 换 RPC 框架 (Bolt→gRPC) 要改多少代码?

### 1. RFC-0001 — 为什么想换掉纯 RocksDB 存储

场景: 老存储有什么问题, 值得写 RFC 重做?
源码路径:
- 动机 (0001-new-log-storage.md:24-28): **①绑定 RocksDB 指定版本 ②依赖加大包体积** — 目标纯 Java 实现
- 蓝图三 DB + 文件管理 + 预分配 + 组提交 (L31-63, L213-225)
- **落地差异**: 未完全文件化, 而是 **RocksDBSegmentLogStorage extends RocksDBLogStorage** (RocksDBSegmentLogStorage.java:65) — 类内混合
关键设计 (q1): **蓝图与落地的距离** — RFC 要"纯 Java 替换", 落地是"RocksDB 保留 + 段文件分流" — 兼容老数据 + 改动小; 这正是 J-5 讲述的主线。 [模式: 演进而非重写]

### 2. 双介质 — 4K 阈值把日志分流

场景: 一条 100KB 的日志写 RocksDB 会怎样?
源码路径:
- 阈值 **4K** (RocksDBSegmentLogStorage.java:157-161); 小值直接 RocksDB (RocksDBLogStorage.java:519)
- **大值**: SegmentFile.write 顺序写 mmap (L1092) → **16B 位置元数据** (magic/firstLogIndex/wrotePos, L1109-1116) 进 RocksDB
- 写屏障: onDataAppend → put → joinAll → doSync (L517-523)
关键设计 (q1): **大日志绕开 LSM 写放大** — RocksDB 只存"指针", 数据顺序追加; 小日志留在 RocksDB 享受成熟恢复逻辑。 [模式: 阈值分流]

### 3. 预分配 — 换文件不卡顿的秘密

场景: 段文件写满要开新文件, 为什么写性能不掉?
源码路径:
- **SegmentAllocator 守护线程**维持 blankSegments ≥ 2 (L646-656, PRE_ALLOCATE_SEGMENT_COUNT=2 L67); 写入侧 emptyCond 等待/fullCond 补货 (L399-416)
- 新文件创建即按 **1G 映射整个大小** (L474-507) — mmap 一次到位
- 段切换: 旧段并行 sync (L379-388) + 新段从队列取 (L410)
关键设计 (q3): **生产者-消费者预分配** — mmap 建映射 + 缺页中断是写入杀手 (RFC L183-191), 提前建好 2 个空段; 切换只做指针换。 [模式: 双缓冲]

### 4. 异步写 — wrotePos 占位与 writeExecutor 搬运

场景: 写线程被 memcpy 阻塞怎么办?
源码路径:
- **锁内占位**: wrotePos 推进 + 索引更新 + 返回位置 (SegmentFile.java:746-754) → **writeExecutor 异步 memcpy** (L760-770; 线程池 core=cpus/max=cpus*3/CallerRunsPolicy, L300-304)
- **wrotePos vs committedPos** (L838-865): sync 前移 committedPos + force; 读限 committedPos (L806-811)
关键设计 (q3): **写线程零拷贝等待** — 占位即返回, 数据搬运异步; 崩溃窗口由 committedPos 语义兜住 (未提交不可读)。 [模式: 写后置]

### 5. 崩溃恢复 — checkpoint + abort + 脏尾截断

场景: 写一半断电, 启动怎么知道从哪恢复?
源码路径:
- checkpoint 每 5s 记 "最后段文件名 + committedPos" (L759-765, 843-844); **abort 文件**判正常退出 (L471-475)
- 段文件三分类 (L480-506): blank 复用 / corrupted (仅最后文件可, 改名 .corrupted, 多个 fatal L623-641) / 正常
- recoverFiles (L557-603): checkpoint 文件从 committedPos 起; 尾部脏数据 truncateFile (SegmentFile.java:634-711)
关键设计 (q2): **断点 + 标记 + 截断三层** — checkpoint 定位, abort 判"要不要恢复", 脏尾截断兜底; 崩溃残留 abort = 恢复信号。 [模式: 断点恢复]

### 6. 截断的混合特色 — 段内截断查 RocksDB

场景: truncateSuffix 要删段中间的日志, 怎么知道准确位置?
源码路径:
- 前缀: 二分找段区间整段 destroy (L880-919)
- **后缀**: 先毁 keptFile 之后的段 (L967-969); 段内**借助 RocksDB 16B 元数据扫描 logWrotePos** (L979-1042) → truncateSuffix + clear 64B 洞 (SegmentFile.java:445-462)
关键设计 (q2): **双介质让截断也要双查** — 位置信息在 RocksDB, 数据在段文件; 混合架构的代价与特色。 [模式: 元数据导向截断]

### 7. RPC 可插拔 — 一个 SPI 文件换传输层

场景: 换 gRPC 要改多少代码? 零行。
源码路径:
- **RaftRpcFactory 接口** (RaftRpcFactory.java:27-107) + RpcFactoryHelper 静态加载 (L27-28); @SPI(priority) 选最大
- **切换载体 = META-INF/services**: Bolt (priority 0) / GrpcRaftRpcFactory (priority 1, GrpcRaftRpcFactory.java:42-43) — **加 jar 即切换**
- 处理器层完全传输无关: RaftRpcServerFactory 注册核心 7 + CLI 11 (L122-147)
关键设计 (q3): **工厂 + SPI = 静态插拔** — 类加载时一次选中; Bolt 与 gRPC 只差一个 SPI 文件; 处理器 (RpcProcessor) 双框架通用。 [模式: SPI 可插拔]

### 8. pipeline 并发模型 — 每 peer 单线程 + 响应排序

场景: 乱序响应怎么在服务端解决? (与 J-2 客户端呼应)
源码路径:
- **每 (groupId, peer) 一个 MpscSingleThreadExecutor** (AppendEntriesRequestProcessor.java:235-262) — 同 replicator 串行, 不同 peer 并行
- **响应按 sequence 排序投递** (L311-348): 非心跳递增序号 → PriorityQueue → 队头 == nextRequired 才发; 心跳直发 (L463-464)
- 积压超 maxReplicatorInflightMsgs → 关连接 (L339-346); 连接断开清上下文 (L497-516)
关键设计 (q4): **服务端与客户端双端排序** — 客户端 seq 消费 (J-2), 服务端 seq 投递; 心跳不排队保活性; pipeline 要求 Bolt 关闭默认派发 (ensurePipeline, BoltRaftRpcFactory.java:78-85)。 [模式: 双端序控制]

### 9. gRPC 的"单方法复用" — 一个 _call 承载所有消息

场景: gRPC 没有按消息类型分发, 怎么办?
源码路径:
- **FIXED_METHOD_NAME="_call"** (GrpcRaftRpcFactory.java:45); 方法全名 = 消息类名._call (L138); MarshallerRegistry 请求→响应映射 (L58-72)
- GrpcServer: 每 processor 建 UNARY MethodDescriptor (L131-207); interceptor 恢复"连接属性" (L145-146)
关键设计 (q4): **一个方法名承载所有消息** — gRPC 方法 vs Bolt interest 分发的适配; interceptor 补回 Bolt 的连接上下文能力, 让上层逻辑双框架通用。 [模式: 协议适配]

## 代码类型
Architecture (支撑面)

## 负面空间 — SOFAJRaft 存储/RPC 刻意不做的事

- **不做完全 Java 化存储**: RocksDB 依赖仍在 (RFC 未落地)
- **不做 RPC 运行时切换**: 静态 SPI, 换 jar 重启
- **不做跨语言协议**: Bolt/gRPC 均 Java 生态 (对照 brpc 的 C++ 面)
- **不做存储压缩/加密**: 原样落盘
- **不做段级并行读**: 段内串行读

→ 引出: 存储与 RPC 之下的地基是谁? 时间轮/MPSC/分段列表 → J-6 并发模型与定时器基础设施
