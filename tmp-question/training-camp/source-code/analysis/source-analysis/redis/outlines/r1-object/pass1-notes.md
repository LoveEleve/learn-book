# R-1 redisObject — Pass 1 探索笔记

> 域: R-1 redisObject (对象模型) | 🔴 方案 A | 2026-08-13
> 源码: src/object.c (1677) + server.h robj (903-911) | Redis 7.4.2

## 继承树/调用图

```
struct redisObject (server.h:903-911, 16 字节):
  type:4 + encoding:4 + lru:LRU_BITS(24) + refcount(int) + ptr
  OBJ_STATIC_REFCOUNT = INT_MAX-1 (栈上对象) / OBJ_SHARED_REFCOUNT (不可变共享)

创建:
  createObject (L22-30): 默认 RAW, refcount=1
  createStringObject (L102-107): ≤44B → EMBSTR / 否则 RAW
  createEmbeddedStringObject (L71-93): robj+sdshdr8+buf 一次分配 (64B arena)
  createStringObjectFromLongLongWithOptions (L128+): 共享整数 (<10000) / INT 编码 / RAW
    + LL2STROBJ_NO_SHARED / NO_INT_ENC 标志

编码优化链:
  tryObjectEncodingEx (L607-678): RAW/EMBSTR → INT (string2l ≤20 字符)
    → 共享整数 (maxmemory 无 NO_SHARED_INTEGERS 时) → EMBSTR (≤44B)
  tryObjectEncoding (L679): 包装 (try_trim)
  getDecodedObject (L685+): INT → 字符串 (解码)

引用计数:
  incrRefCount (L349-359): 特殊 refcount 不碰
  decrRefCount (L361-377): ==1 → 释放 (freeXxxObject 分派) / 否则 --
  makeObjectShared (L56-60): refcount = OBJ_SHARED_REFCOUNT

共享对象:
  createSharedObjects (server.c:1847+): 命令响应串 (+OK/错误/批量前缀) + shared.integers[10000]
  main 启动调用 (server.c:2653)

命令面:
  objectCommand (L1442+): TYPE/OBJECT ENCODING/REFCOUNT/IDLETIME/FREQ/HELP
  checkType (L565): 类型错误 → WRONGTYPE

字符串特殊:
  EMBSTR 44B 上限 (L101): "fit into the 64 byte arena of jemalloc"
  INT 编码: ptr 直接存 long 值 (不分配!)
```

## 基本元素分解

1. **16 字节外壳**: type/encoding/lru/refcount/ptr — 全部值的统一头
2. **三态字符串**: INT (指针存值) / EMBSTR (同 chunk) / RAW (sds 独立)
3. **共享机制**: 特殊 refcount (不可变) + shared 池 (10000 整数 + 响应串)
4. **编码优化链**: RAW → INT → 共享 / EMBSTR (tryObjectEncoding)
5. **引用计数**: 分派释放 (freeXxxObject) + 特殊值守卫
6. **LRU/LFU**: lru 字段双用途 (24bit)

## 标记问题 (8 个)

1. 16 字节外壳的位域布局 — type/encoding 各 4bit? lru 24bit?
2. EMBSTR 44B 上限的数学: 为什么恰好 64B arena? (robj 16 + sdshdr8 3 + 44 + 1)
3. INT 编码: ptr 直接存值 — 什么时候能/不能? (共享限制?)
4. tryObjectEncoding 优化链顺序 — 为什么先 INT 后 EMBSTR? maxmemory 下为什么不用共享整数?
5. 引用计数分派释放 + 特殊 refcount 语义 (共享/栈上)
6. 共享对象池: 10000 整数的选择? 响应串共享的收益?
7. getDecodedObject — INT→字符串的转换时机?
8. LRU/LFU 双用途 + NO_SHARED_INTEGERS 关联?

## 时空溯源 (代码内痕迹)

- 2010 初版: robj 三字段 (type/encoding/ptr) + refcount 引入早期
- 2013+: EMBSTR 引入 (44B 上限 64B arena 设计)
- LRU_BITS 演进: 22→24 bit; LFU 模式 (8bit 频率+16bit 时间)
- OBJ_SHARED_INTEGERS=10000: 长期稳定
- shared 响应串: 持续扩充 (新命令常量)
