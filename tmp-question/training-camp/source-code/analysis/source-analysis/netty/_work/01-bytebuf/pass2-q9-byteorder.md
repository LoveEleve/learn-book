## Loop Note: Q9 — ByteOrder 字节序

**Hypothesis**: ByteBuf 默认用 BigEndian（网络字节序），但支持 `order(LITTLE_ENDIAN)` 切换。`getInt/getLong/setInt` 等操作遵循当前的 `order()` 设置。

**Verification** (ByteBuf.java:284-1107):
- `order()` (line 284) — 返回当前字节序，默认 `BIG_ENDIAN`
- `order(ByteOrder)` (line 298) — 切换字节序，返回新视图（若端序相同则返回 this）
- `getIntLE(index)` (line 1069) — `LE` 后缀方法强制 LittleEndian，忽略 `order()` 设置
- `getInt/readInt/writeInt` — 遵循 `order()` 设置
- SwappedByteBuf: 当 `order(LITTLE_ENDIAN)` 调用在 BIG_ENDIAN buf 上时，返回 SwappedByteBuf 包装器

**Code type**: Implementation

**设计权衡**:
| 方案 | 优点 | 缺点 |
|------|------|------|
| 默认 BigEndian | 网络协议标准（TCP/IP、HTTP） | x86 是 LE，需额外转换 |
| LE 后缀方法 | 强制端序，不依赖 order() 状态 | API 膨胀 |
| SwappedByteBuf | 零开销切换端序（纯数学翻转） | 多层包装 |

**为什么重要**:
- HTTP Content-Length header → BigEndian int → `buf.writeInt(length)` ✅ 直接写
- 与 C 二进制协议通信 → 可能需要 LittleEndian → `buf.writeIntLE(value)` 
- 跨语言序列化（Protobuf）→ 协议层处理，ByteBuf 不需要切换

**结论**: ByteBuf 默认 BigEndian 匹配网络协议惯例，`order()`/`SwappedByteBuf`/`*LE` 后缀提供三重 LittleEndian 支持。设计决策：不强制统一的端序，而是让协议层通过 `order()` 灵活切换。source: ByteBuf.java:284,298,1069
