# 闭环笔记 q4: 压缩装饰器 — innerCodec 套嵌 + 默认组合

## 假设
LZ4/Snappy/ZStd 是**装饰器**: 内层 innerCodec 做对象序列化, 外层做字节压缩。默认内层 = Kryo5 (序列化二进制再压)。

## 验证过程
- LZ4CodecV2 (44): `private final Codec innerCodec`; `LZ4CodecV2() → this(new Kryo5Codec())` (L50-52) — **默认套 Kryo5**
- decode (L72-78): `int decompressionSize = buf.readInt()` (读目标解压大小头部) → BlockLZ4CompressorInputStream 解压 → innerCodec.getValueDecoder().decode(out)
- encode (L82-96): `innerCodec.getValueEncoder().encode(in)` → 压缩 → 写 decompressionSize 头部
- SnappyCodecV2 (43)/ZStdCodec (43) 同构: 都 extends BaseCodec + innerCodec + 头/解压/内层
- **为什么 V2**: LZ4Codec (旧) vs LZ4CodecV2 — CHANGELOG 无显式, 但 V2 系列普遍是"v2 = 兼容新版客户端命令编解码重构" (4.x 双引擎 Native 面)
- 默认全压: LZ4/Snappy/ZStd 无参构造都用 Kryo5 → "二进制序列化 + 无损压缩"默认组合

## 代码类型
Implementation (装饰器) — 结构清晰: 外层压缩字节, 内层序列化对象

## 跨域关联
- Q3 (Kryo 内层) → 默认组合
- R-5 quicklist (服务端压缩) → 对照: 服务端 listpack 内嵌压缩 vs 客户端整体压缩
- 面试点: 压缩 codec 选型 = 压 CPU vs 省带宽 权衡

## 结论
压缩族 = 装饰器三件套 (解压头 + 解压流 + innerCodec.decode), 默认 innerCodec=Kryo5。协议: 每个快照前写 decompressionSize int 头 → 解压已知大小。对比服务端 (Redis quicklist LZF 逐 node): 客户端压整个 value, 粒度更大但 CPU 开销集中在传输边界。
源码位置: LZ4CodecV2.java:44,50-96, SnappyCodecV2.java:43, ZStdCodec.java:43