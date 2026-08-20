# 闭环笔记 q3: 默认 Kryo5Codec — 三池化与线程安全契约

## 假设
Kryo 非线程安全 → Redisson 用 Pool<Kryo> 池化拿取; 默认 Codec 选 Kryo5 因 4.0.0 起 Jackson 变可选依赖, Kryo 成为零依赖默认。

## 验证过程
- Config copy 构造 `oldConf.setCodec(new Kryo5Codec())` (Config.java:165) — **默认 = Kryo5**
- **三池化** (Kryo5Codec.java:97-99): `kryoPool = new Pool<Kryo>(true,false,1024)` + `inputPool` (512, Input(8192)) + `outputPool` (512, Output(8192,-1))
  - `Pool(threadSafe=true, softReferences, max)` — Kryo5 默认池 1024 实例上限, Input/Output 各 512, 缓冲 8K
- encoder (L214-235): obtain kryo+output → writeClassAndObject → flush; **catch RuntimeException → out.release()** (L228, 失败归还 ByteBuf 内存)
- decoder (L194-212): obtain kryo+input → readClassAndObject; `if (success) inputPool.free` — 失败不归还缓冲
- createKryo (L157-192) 深度注册: SimpleInstantiatorStrategy (L67-95: 反射无参构造 → 回退 Objenesis StdInstantiator, 可绕过构造器造对象), `setRegistrationRequired(!allowedClasses.isEmpty())` (L163), Collections 家族 JavaSerializer (L170-177), EnumMap/Throwable/UUID/URI/Pattern/SocketAddress/InetAddress/Atomic* 全注册 (L178-188), KeySetView 专用 (L190)
- 4.0.0 CHANGELOG: "Jackson library is now optional" — Jackson 从必选降为可选 → 默认不能依赖它 → Kryo5 成默认根因

## 代码类型
Implementation (池化序列化) — 高价值: 池化 + 失败回收

## 跨域关联
- RD-1 (Config 默认) → Kryo5 是启动默认
- Q4 (压缩族) → LZ4 默认套 Kryo5 innerCodec
- 安全面: allowedClasses → registrationRequired (安全硬化入口)

## 结论
默认 Kryo5 的三重理由: ①4.0.0 Jackson optional (不能做默认依赖) ②Kryo 类型感知 (writeClassAndObject) 免显式类型 ③三池化解决 Kryo 非线程安全。池化语义: 1024 Kryo / 512+512 缓冲, 失败 {ByteBuf: release, 池: 不归还} — 精确的内存/对象归还协议。
源码位置: Config.java:165, Kryo5Codec.java:97-236, CHANGELOG 4.0.0