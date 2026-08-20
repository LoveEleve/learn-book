# RD-3 篇2 — default-codec: 三池化 Kryo5 与 Composite 委托

> 前置: [[RD-3-篇1]] (接口契约 + 原型) | 复用: [[rd1-connection]] (Config 默认 + ServiceManager) | 对照: [[r1-object]] (服务端共享对象池) | 引出: [[RD-3-篇3]] (压缩与类型) + [[rd2-rlock]] (Lua codec)
> 🔴 A | 2 KP | [模式: 池化序列化 + 委托复合]
> Pass 2 闭环: q2(复合委托) q3(默认 Kryo5 三池化)

**读者处境**: 为什么默认 codec 是 Kryo5 不是 Jackson?Kryo 明明非线程安全, Redisson 怎么敢默认用它?一个 RBucket 的读写, 背后要申请归还几次池?RMap 怎么做到 key 用 String、value 用对象而互不干扰?这篇拆默认值的选择逻辑 (三池化线程安全) 与 CompositeCodec 的委托复合 (key/value 分离编解码)。

### 概念依赖链
q3(默认 Kryo5 三池化) ← q2(Composite 委托) — 先讲为什么默认 Kryo5 且线程安全 (池化), 再讲组合 codec 怎么按角色委托。

### 核心悬念
"非线程安全的 Kryo 为什么能当默认？RMap 怎么让它 key 和 value 用不同 codec？"

### 叙事顺序
1. 问题引入: 默认 codec 凭什么选 Kryo5
2. 三池化 (q3) — Pool<Kryo>(1024)/Input/Output 512, ClassAndObject 类型内置, Jackson optional 根因
3. 深度注册 (q3) — SimpleInstantiatorStrategy / allowedClasses / Collections 家族
4. 委托复合 (q2) — mapKey/mapValue/value 三委托, 2 参 value=null 的 Stream 场景
5. 收束: "池化解决有状态 + 委托满足多角色" 双设计

### 1. 三池化 Kryo5 — 非线程安全的默认

场景: Kryo 非线程安全, 默认却选它 — 线程安全怎么解决?
源码路径:
- Config copy 构造: `oldConf.setCodec(new Kryo5Codec())` (Config.java:165) — **默认 = Kryo5**
- **三池化** (Kryo5Codec.java:97-99,123-155): `kryoPool = new Pool<Kryo>(true,false,1024)` + `inputPool` (512, Input(8192)) + `outputPool` (512, Output(8192,-1)) — Kryo 实例池上限 1024, 缓冲各 512; **池满时 obtain() 阻塞等待归还** (Kryo Pool 官方阻塞语义 — 1024 并发 writer 才会触顶, 常态不会)
- encoder (Kryo5Codec.java:214-235): obtain kryo+output → writeClassAndObject → flush; **失败 out.release()** (Kryo5Codec.java:228 归还 ByteBuf 内存); 成功 free 回池
- decoder (Kryo5Codec.java:194-212): obtain → readClassAndObject; `if (success) inputPool.free` (Kryo5Codec.java:207-208) — 失败不归还 (防污染池)
- createKryo (Kryo5Codec.java:157-192): `setRegistrationRequired(!allowedClasses.isEmpty())` (Kryo5Codec.java:163, 安全) / SimpleInstantiatorStrategy (Kryo5Codec.java:67-95: 反射无参→回退 Objenesis) / Collections 家族 JavaSerializer (Kryo5Codec.java:170-177) / EnumMap/Throwable/UUID/URI/Pattern/SocketAddress/InetAddress/Atomic* (Kryo5Codec.java:178-188) / KeySetView (Kryo5Codec.java:190)
- 4.0.0 CHANGELOG: **"Jackson library is now optional"** — Jackson 从必选降可选 → 默认不能依赖 → Kryo5 成唯一零额外依赖默认
关键设计 (q3): 有状态序列化器 (Kryo) → **每次操作一个私有实例 (池化)**; ClassAndObject 类型内置; 失败归还协议精确 (ByteBuf release / 池不回收)。[模式: 池化有状态 + 失败精确归还]
数据流: encode → pool.obtain Kryo+Output → 序列化+类型 → 失败?released : 回池。

### 2. 委托复合 — mapKey/value 分离

场景: 一个结构中 key 和 value 需要不同 codec 怎么办?
源码路径:
- `CompositeCodec` (org.redisson.codec:30-45): `mapKeyCodec/mapValueCodec/valueCodec` 三字段; 2 参构造 `this(mapKey, mapValue, null)` (CompositeCodec.java:36-38); 3 参 (CompositeCodec.java:40-45)
- 转发 (CompositeCodec.java:54-87): getMapKeyXxx → mapKeyCodec; getMapValueXxx → mapValueCodec; getValueXxx → valueCodec (**无 null 兜底 L74-81 — 契约**)
- 实际调用:
  - `RedissonReliableTopic:82` `new CompositeCodec(StringCodec.INSTANCE, codec)` — **2 参 value=null**: Stream 用 map 语义 (id=String + msg=业务 codec), 普通 value 永不进入
  - `RedissonBuckets:83` `new CompositeCodec(StringCodec.INSTANCE, codec, codec)` — 3 参全给
- BaseCodec 兜底 (client/codec:54-69): map 系列默认转发 value — 非 Composite 的 codec 天然 map=value
- 为什么 Stream 2 参够: 结构动词面匹配 — Stream/Map 读写信道是 mapKey/mapValue, value 通道不用
关键设计 (q2): 委托复合把"结构角色"映射到 codec: Streaming/Map 只用 map 语义 (2 参), Buckets 混合 (3 参); value=null 是契约不是 bug。[模式: 角色委托 + 契约边界]
数据流: RMap 写 → getMapKeyEncoder (mapKeyCodec=String) + getMapValueEncoder (mapValueCodec=业务) → HSET → 读侧对应解码。

### 负面空间 — 默认与复合刻意不做的事

- **不做运行时 type 检查**: CompositeCodec 不校验 key/value 类型匹配 (依赖调用方正确组合)
- **不做池上限动态调整**: Kryo 池 1024/512 为编译期常量, 无自适应
- **不做 key/value 同 codec 特判**: 即使同 codec 也各走各的委托 (无 fastpath)
- **不做序列化版本兼容**: ClassAndObject 假设类结构不变
- **不禁用 Objenesis**: 绕过构造器的反序列化被允许 (非安全加固场景)

→ 引出: 压缩怎么套在序列化外层?类型保真三档怎么选?→ [[RD-3-篇3]]