# Ch10 Codec 框架 — 知识规划

> 来源: 9 源文件 | ~2600 行 | codec-base/handler/codec/
> 基线: Ch9 Bootstrap 启动了服务器 — Ch10 回答 "原始字节怎么变成业务消息"

---

## 01-02 核心机制 + 分类

### 🔴 Deep
| KP | 为什么🔴 |
|----|---------|
| **Cumulator 双策略 (MERGE/COMPOSITE)** | 积攒是半包处理的根基 — MERGE 用 copy, COMPOSITE 用 CompositeByteBuf 零拷贝 |
| **callDecode 循环 + discardAfterReads=16** | 解码循环核心 — 读够了就 decode, 读太多就释放已读空间防 OOM |
| **三级状态机防重入** | INIT→CALLING_CHILD_DECODE→HANDLER_REMOVED_PENDING — handlerAdded 期间 remove 的安全处理 |
| **REPLAY Signal 非局部控制流** | Erlang 风格 — 读不足抛 Signal 而非返回 null, callDecode catch 后回退 readerIndex |

### 🟡 Working: 4 种拆包器 + MessageToByteEncoder + ReplayingDecoder 状态机

### 🟢 Surface: CodecOutputList Recycler / TypeParameterMatcher / 空结果处理

---

## 03 聚类

### Cluster A: ByteToMessageDecoder 核心 (8 KPs)
1. Cumulator 双策略: MERGE_CUMULATOR(拷贝) + COMPOSITE_CUMULATOR(CompositeByteBuf零拷贝)
2. channelRead: cumulation积攒→callDecode循环→fireChannelRead批量传播
3. callDecode 循环: decode返回null等数据→消费数据继续→out非空传播
4. 三级状态机防重入 + inputMessages 延迟队列
5. discardAfterReads=16 防 OOM
6. CodecOutputList Recycler 对象池
7. decodeLast 钩子
8. expandCumulation 扩容

### Cluster B: 编码器 + 拆包器 + ReplayingDecoder (10 KPs)
1. MessageToByteEncoder write 五步: 类型匹配→分配buf→encode→释放原msg→write
2. FixedLengthFrameDecoder: readRetainedSlice(frameLength)
3. DelimiterBasedFrameDecoder: 多分隔符最短帧 + 超长丢弃模式
4. LengthFieldBasedFrameDecoder: offset+length+adjustment+strip 四参数状态机
5. LineBasedFrameDecoder: findEndOfLine \r\n/\n 扫描
6. ReplayingDecoder: checkpoint(S) + REPLAY Signal 回退
7. 四种拆包器共性: return null→callDecode 自动等待更多数据
8. MessageToMessageDecoder: acceptInboundMessage→decode→fireChannelRead
9. MessageToMessageEncoder: voidPromise/PromiseCombiner 多结果
10. TypeParameterMatcher 泛型类型匹配

### 教学顺序: A → B
