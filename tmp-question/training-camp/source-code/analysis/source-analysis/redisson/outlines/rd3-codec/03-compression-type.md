# RD-3 篇3 — compression-type: 压缩装饰器与类型保真三档

> 前置: [[RD-3-篇2]] (默认 Kryo5 内层) | 复用: [[rd1-connection]] (EventLoopGroup) | 对照: [[r5-quicklist]] (服务端 LZF 逐 node 压缩) | 引出: [[rd5-rmap]] (mapValueTypeReference 场景)
> 🔴 A | 3 KP | [模式: 装饰器压缩 + 类型保真分级]
> Pass 2 闭环: q4(压缩装饰器) q5(类型保真) q7(线程安全三策)

**读者处境**: 存个 100KB 的 JSON 要不要开压缩?LZ4 和 Kryo5 是同一层吗?为什么反序列化 JSON 会丢类型、而 Kryo/results 不丢?Redis 服务端也有压缩 (quicklist), 和客户端压缩一样吗?这篇拆压缩 codec 的装饰器结构 (压字节不压对象), 类型保真的三档设计 (丢/指定/内置), 与各 codec 的线程安全策略 (池化/单例/缓存)。

### 概念依赖链
q4(压缩装饰器) ← q5(类型保真) ← q7(线程安全) — 先讲压缩怎么套层, 再讲类型怎么保真, 最后收敛到并发策略。

### 核心悬念
"LZ4 和 Kryo5 谁包谁？JSON 为什么丢类型而 Kryo 不丢？线程安全为什么有三种解法？"

### 叙事顺序
1. 问题引入: 100KB JSON 要不要开压缩
2. 压缩装饰器 (q4) — 解压头 + 解压流 + innerCodec, 默认内层 Kryo5
3. 类型保真三档 (q5) — Object.class 丢 / TypeReference 指定 / ClassAndObject 内置
4. 线程安全三策 (q7) — 池化 Kryo / 单例共享 ObjectMapper / Schema 缓存 Protobuf
5. 收束: 面试三连 "压不压/丢不丢/安全不安全" 一表收编

### 1. 压缩装饰器 — 压字节, 不压对象

场景: LZ4 和序列化器谁是外层?
源码路径:
- `LZ4CodecV2` (org.redisson.codec:44): `private final Codec innerCodec`; `LZ4CodecV2() → this(new Kryo5Codec())` (LZ4CodecV2.java:50-52) — **默认套 Kryo5**
- decode (LZ4CodecV2.java:72-78): `int decompressionSize = buf.readInt()` (读解压目标大小头部) → BlockLZ4CompressorInputStream → `innerCodec.getValueDecoder().decode(out)`
- encode (LZ4CodecV2.java:82-96): `innerCodec.getValueEncoder().encode(in)` → 压缩 → 写 decompressionSize 头
- SnappyCodecV2 (43) / ZStdCodec (43) 同构 (extend BaseCodec + innerCodec + 头/解压/内层)
- **LZ4 两版并存**: 旧 `LZ4Codec` (LZ4Codec.java:46) 用 lz4-java (`net.jpountz.lz4.LZ4Factory`, L21), `LZ4CodecV2` (LZ4CodecV2.java:44) 用 commons-compress (`BlockLZ4CompressorInputStream`, L19) — 算法同 (LZ4 block), 实现库不同; V2 承接 4.x 依赖演进, 旧版保留兼容
- 协议: 每个快照前写 int 解压大小 → 解压已知边界
- 默认全压组合: LZ4/Snappy/ZStd 无参构造 → inner=Kryo5 = "二进制序列化 + 无损压缩"
关键设计 (q4): 装饰器两层: **外层压字节, 内层序列化对象**; 头部记录解压后大小。[模式: 双层装饰 + 大小头]
数据流: encode(obj) → Kryo5 序列化 → LZ4 压缩 → [size头][压缩字节] → Redis; decode 反向。

### 2. 类型保真三档 — 反序列化丢类型的根源

场景: 为什么 JSON 反序列化丢类型?怎么保住?
源码路径:
- `JsonJacksonCodec` (54): `mapObjectMapper.readValue(stream, Object.class)` (JsonJacksonCodec.java:105) — **类型全丢**, 反读需调用方已知类型
- `TypedJsonJacksonCodec` (40, extends JsonJacksonCodec): 字段 `valueTypeReference/mapKeyTypeReference/mapValueTypeReference` (TypedJsonJacksonCodec.java:73-75) + createDecoder (TypedJsonJacksonCodec.java:57-63): `readValue(stream, valueTypeReference)` (TypedJsonJacksonCodec.java:57-63) — **调用方构造时指定泛型**
- `Kryo5Codec` (62): `writeClassAndObject / readClassAndObject` (Kryo5Codec.java:224/202) — **类型写进流** (类名+字段)
- 三档: 丢类型 (readValue Object.class) / 指定类型 (TypeReference) / 内置类型 (ClassAndObject)
- 选型: 泛型已知场景 (RList<String>) → Jackson; 异构/多态对象 → Kryo5; 需要类型恢复 → TypedJsonJackson
- 面试对比: JSON 可读/跨语言但类型弱; Kryo 紧凑/类型强但 Java-only/不可读
关键设计 (q5): 类型保真 = 反序列化能否恢复对象真实类型的度量; 三档 = 复杂度与灵活性的三档 trade-off。[模式: 类型保真分级]
数据流: 存异构对象 → Kryo5 ClassAndObject → 类型签名入流 → 反读精确恢复。

### 3. 线程安全三策 — 池化/单例/缓存

场景: 高并发下每个 codec 怎么做到线程安全?
源码路径:
- **池化** (非线程安全者): Kryo5 `Pool<Kryo>(true,false,1024)` (Kryo5Codec.java:123) + Input 512 + Output 512 (Kryo5Codec.java:134-154) — 每次 borrow/free
- **单例共享** (线程安全者): JsonJacksonCodec `static final INSTANCE` + `protected final ObjectMapper mapObjectMapper` (JsonJacksonCodec.java:57/82) — Jackson 构建后线程安全
- **缓存** (按类型): ProtobufCodec `RuntimeSchema.getSchema(clazz)` (ProtobufCodec.java:189/196) — Schema 按类缓存, 读并发安全
- 归还协议: Kryo5 decode `if (success) inputPool.free` (Kryo5Codec.java:207-208) — 失败实例不回池 (防污染)
- 分界: 有状态 (Kryo 实例/缓冲) → 池; 无状态配置 (ObjectMapper) → 单例; 类型派生 (Schema) → 缓存
关键设计 (q7): 并发对称三策: **有状态池化 / 无状态单例 / 按型缓存**; 失败精确归还。[模式: 并发三策]
数据流: 并发编解码 → 池化拿私有实例 / 共享无状态 / 取缓存 Schema — 全部线程安全。

### 负面空间 — 压缩与类型刻意不做的事

- **不按大小动态决定压缩**: 无论多大都压 (无阈值判断, 小对象也压)
- **不做压缩级别配置**: 压缩质量/速度固定, 无 trade-off 参数 (zstd 例外)
- **不做类型声明校验**: TypedJsonJackson 不验证实际类型与 TypeReference 匹配
- **不限制 Objenesis**: Kryo 可绕过构造器 (默认允许)
- **不做 JSO 可读性兜底**: 压缩后的二进制无人类可读路径
- **不自动迁移数据格式**: 换 codec 不改写已有 Redis 数据 — 生产切 codec 必须业务侧重写或双写灰度, 否则旧 key 反序列化失败 (completeness Q15)

### 选型决策表 (completeness Q50 回填)

| 场景 | 推荐 codec | 类型保真 | 理由 |
|---|---|---|---|
| RList<RMap> 泛型已知 | JsonJackson | 丢 (Object.class) | 可读/调试友好, 类型已知无需保 |
| 异构/多态对象 | Kryo5 (默认) | 内置 (ClassAndObject) | 类型恢复零配置 |
| 需要跨语言互操作 | JsonJackson/Avro | 丢/协议 | 标准格式 |
| 高阀值大负载 | 压缩族 (LZ4+inner) | 随内层 | 省带宽, inner 决定保真 |
| 泛型 map 复杂类型 | TypedJsonJackson | 指定 (TypeReference) | 显式恢复 |
| 安全受限环境 | Kryo5+allowedClasses | 内置 | registrationRequired 白名单 |
| 大对象高吞吐 | Protobuf/Serialization | 协议 | Schema 缓存线程安全 |

→ 引出: RMap 用什么 codec 存 value?缓存里 mapValueTypeReference 怎么配?→ [[rd5-rmap]]