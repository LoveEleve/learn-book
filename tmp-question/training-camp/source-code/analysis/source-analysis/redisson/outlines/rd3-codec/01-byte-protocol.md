# RD-3 篇1 — codec: 双包边界的接口契约与零开销原型

> 前置: [[rd0-intro]] (导论) + [[r1-object]] (服务端编码对照) | 复用: [[s75-boot-redis]] (Redis 模板序列化) | 对照: [[r19-listpack]] (服务端紧凑编码) [[r28-networking]] (RESP 协议面) | 引出: [[RD-3-篇2]] (默认与复合) + [[rd4-command]] (命令层衔接)
> 🔴 A | 3 KP | [模式: 协议接口 + 零开销原型 + 关注点分离]
> Pass 2 闭环: q1(双包边界) q6(原型零开销) q8(命令衔接)

**读者处境**: 你在 Spring Boot 里用 RedisTemplate 存对象, 反序列化回来类型总不对?为什么 Redisson 的 codec 接口不在 codec 包里、反而在 client 包里?存个 String 为什么要专门一个"StringCodec"?这篇拆编解码的双包结构: 为什么 Codec 接口属于"协议面", 原型 (String/Long/Integer) 凭什么零开销, 以及 codec 在命令层怎么被显式传递。

### 概念依赖链
q1(双包边界) ← q6(原型零开销) ← q8(命令衔接) — 先建立"协议面/实现面"分层认知, 再看原型为何便宜, 最后追踪 codec 在命令执行全链的流动。

### 核心悬念
"Codec 接口为什么放在 client 包里而不是 codec 包里？存 String 为什么零开销？命令执行时 codec 从哪来？"

### 叙事顺序
1. 问题引入: RedisTemplate 存对象反序列化丢类型 — 序列化框架的水有多深
2. 双包边界 (q1) — client/codec (接口+原型) vs org.redisson.codec (实现), StringCodec→JsonCodec 跨包证据
3. 接口契约 (q1) — 4 组编解码器 value/mapKey/mapValue + 双构造器要求
4. 原型零开销 (q6) — StringCodec ByteBuf 直读写, 命令参数总用原型的原因
5. 命令衔接 (q8) — codec 显式传参 evalWriteAsync, 响应侧 CommandDecoder.selectDecoder
6. 收束: "协议格式动态编解码分离" 设计 — 命令格式静态 / 字节转换动态

### 1. 双包边界 — 编解码是协议面的责任

场景: Codec 接口为什么在 org.redisson.client.codec 而非 org.redisson.codec?
源码路径:
- `Codec` 接口 (client/codec/Codec.java:30) — 与 BaseCodec + 9 原型 (StringCodec/LongCodec/IntegerCodec/DoubleCodec/BitSetCodec/ByteArrayCodec/TDigestDoubleCodec) 同包
- `org.redisson.codec` (36 文件) — Kryo5/Jackson 家族/压缩族/Composite/Serialization/Protobuf
- **跨包实现证据**: `StringCodec` (client/codec:34) `implements JsonCodec` (org.redisson.codec; L24 import) — client 原型实现了实现层的接口 → **双包是逻辑分层, 非物理隔离**
- 接口全是 `getMapKeyDecoder/getMapValueDecoder/getValueDecoder` (Codec.java:37-72) — 恰好是"命令消息字节 ↔ 对象"的转换面
- 同居理由: client 包已有 protocol (RedisCommand/CommandData) + handler (CommandDecoder) — Codec 与之同层: 协议命令/解码器/编解码契约三位一体
关键设计 (q1): 编解码的本质 = **命令消息字节↔对象** 的转换契约, 归属 client 协议面; org.redisson.codec 是向上抽象 (JsonCodec/ObjectCodec) 供结构对象用。[模式: 分层契约]
数据流: 结构对象 → org.redisson.codec 实现 → Codec 接口 → client 协议层 → RESP 字节。

### 2. 接口契约 — 四组编码器与双构造器

场景: 一个 Codec 接口到底需要实现什么?
源码路径:
- `Codec.java:37-79`: **4 组 E/D**: getValueEncoder/Decoder (普通对象) + getMapKeyEncoder/Decoder + getMapValueEncoder/Decoder (Hash 结构的 key 与 value 分开编) + getClassLoader (L79, 反序列化类加载)
- 注释要求 (Codec.java:24-26): "It's required for implementation to have two constructors: default and with ClassLoader object as parameter" — **双构造器契约**: 默认 (序列化用) + ClassLoader (类加载, 跨类加载器)
- BaseCodec (client/codec:32-69): 抽象; map 系列默认转发 value (BaseCodec.java:54-69) — 减派生类负担; `copy(classLoader, codec)` 反射重建
关键设计 (q1): 4 组 E/D = 一个接口承载 value + 双键 (mapKey/mapValue) 三种语义; map 系列兜底转发 = 派生类只需实现 value。[模式: 接口聚合 + 兜底转发]
数据流: getValueEncoder → encode(obj) → ByteBuf; getMapValueDecoder → decode(buf) → obj。

### 3. 原型零开销 — StringCodec 的直写路径

场景: StringCodec 为什么"零开销"?它凭什么是 singleton?
源码路径:
- `StringCodec` (client/codec:34-53): `INSTANCE = new StringCodec()` 单例 (StringCodec.java:36); Encoder `out.writeCharSequence(in.toString(), charset)` (StringCodec.java:44) — 直接写进 ByteBuf, 无中间对象; Decoder `buf.toString(charset); buf.readerIndex(buf.readableBytes()); return str` (StringCodec.java:52-53)
- **implements JsonCodec** (StringCodec.java:34) — 兼容 Json 抽象; 既是原型 (client) 又是 Json 实现 (org.redisson.codec)
- LongCodec/IntegerCodec/DoubleCodec 同理: 数值 → String → ByteBuf 直传
- 分界: 已知类型 (String/数值/byte[]) → 原型 (零序列化开销); 任意对象 → Kryo5/Jackson (对象图序列化)
- 用法: **命令元数据 (key/TTL 参数) 永远用原型** — 它们不该被序列化成对象
关键设计 (q6): 原型 = 已知类型的零开销直传 (输出即 RESP 负载); 分界线在"值是否由原始类型承载"。[模式: 零开销原型]
数据流: RedisCommands 命令参数 → LongCodec/StringCodec 编码 → RESP 直传。

### 4. 命令衔接 — codec 沿调用链显式传递

场景: 执行一条命令时, codec 从哪来?
源码路径:
- 发起: `CommandAsyncService.evalWriteAsync(String key, Codec codec, RedisCommand<T> ..., String script, ...)` (CommandAsyncService.java:471) — **codec 作为显式参数**
- CommandData 打包: codec 进 CommandData (type, key, codec, command, params)
- 响应: `CommandDecoder.selectDecoder` (CommandDecoder.java:565-575) — data==null 兜底 `StringCodec.INSTANCE.getValueDecoder()` (CommandDecoder.java:570); 否则 `multiDecoder.getDecoder(data.getCodec(), paramIndex, state, size, parts)` (CommandDecoder.java:573) — **从 CommandData 取 codec 选 decoder**
- **共享 RESP 面** (对照 [[r28-networking]]): r28 讲服务端从字节流到 argv 的解析; 本域是客户端反向 — 对象→codec→RESP 字节离开发送 (上传侧), 响应字节→codec→对象 (下传侧)。两端都以 RESP 为共同语言, codec 是"对象↔RESP 字节"的翻译器
- `RedisCommand` 自身不带 codec (纯命令元数据) — **命令格式静态 / 编解码动态**
关键设计 (q8): 关注点分离: RedisCommand 定义"命令长什么样", codec 决定"值怎么转换"; 沿调用链显式传, 响应侧按 CommandData.codec 选。[模式: 显式传参 + 响应侧选择]
数据流: 结构方法 → evalWriteAsync(codec) → CommandData(codec) → 服务端 → CommandDecoder → data.getCodec() 选 decoder → 对象。

### 负面空间 — 编解码刻意不做的事

- **不做隐式全局 codec**: 每条命令显式传, 无 thread-local/全局默认 (避免状态污染)
- **不做命令级 codec 存储**: RedisCommand 不带 codec (格式/转换分离)
- **不做跨语言可读性保证**: Kryo 二进制不可读 (可读性=Json 的取舍)
- **不做无限制类型注册**: Kryo registrationRequired 可开 (安全硬化)
- **不自动处理版本漂移**: 序列化格式无 schema 版本协商 (protobuf 例外)
- **不做默认值灰度**: Config 默认 Kryo5 是全局影响点, 生产切默认 codec 需显式 setCodec + 双写/重写灰度 (completeness Q31)

→ 引出: 默认 codec 三池化怎么保证线程安全?复合 codec 怎么委托?→ [[RD-3-篇2]]
→ 衔接: codec 沿命令链路流动 — 命令流水线怎么用它们 → [[rd4-command]]