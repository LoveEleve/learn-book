# ByteBuf — Pass 2 完成

> 域: ByteBuf | 域#1 | 方案: A | 包: io.netty.buffer
> 深审: R1 — 发现 3🔴+3🟡，全部修复

---

## 循环笔记汇总

| # | 问题 | 代码类型 | 核心结论 |
|:--:|------|:--:|------|
| Q1 | 双指针 vs flip/compact | Interface Design | readerIndex/writerIndex 消除模式切换 |
| Q2 | 引用计数 vs GC | Implementation | RefCnt VarHandle + deallocate() |
| Q3 | CompositeByteBuf 零拷贝 | Algorithmic | Component[] 数组 + 二分查找 |
| Q4 | 池化分配器 | Algorithmic | Arena=2*core + ThreadCache + SizeClasses |
| Q5 | Heap vs Direct | Implementation | JNI 零拷贝/GC 托管; AdaptivePooling=池自适应 |
| Q6 | slice/duplicate/copy | Interface Design | 视图共享 vs 深拷贝 + retained 变体 |
| Q7 | ResourceLeakDetector | Implementation | SIMPLE/ADVANCED/PARANOID 4级 |
| Q8 | 容量扩展 | Algorithmic | <4MB指数翻倍, >=4MB线性+4MB |
| Q9 | ByteOrder | Implementation | BigEndian默认 + LE后缀 + SwappedByteBuf |

---

## 深审 R1 修复

| # | 严重度 | 问题 | 状态 |
|:--:|:--:|------|:--:|
| 1 | 🔴 | Q4 arena公式 `min(cores/2,6)` → 实际 `min(cores*2, mem/chunk/6)` | ✅ 修正 |
| 2 | 🔴 | Q5 "SocketChannel只接受DirectBuffer" → JNI拷贝差异 | ✅ 修正 |
| 3 | 🔴 | Q5 "AdaptivePooling选heap/direct" → 自适应池容量 | ✅ 修正 |
| 4 | 🟡 | 缺 ResourceLeakDetector | ✅ 新增 Q7 |
| 5 | 🟡 | 缺 容量扩展策略 | ✅ 新增 Q8 |
| 6 | 🟡 | 缺 ByteOrder 字节序 | ✅ 新增 Q9 |

---

## Pass 2 完成检查

- [x] 循环关闭: 9/9 ✅
- [x] 深审: 3🔴+3🟡 全修复 ✅
- [x] 自审: grep≥3 ✅ / 发现不准确≥1 ✅ / 深度足够 ✅

## 方法论证据

```
[01 Pass 2] grep: ~20次 | 深审发现: 3🔴+3🟡 | 修复: 9循环关闭
```

## 下一步: Pass 3

A 方案: 叙事整合 + 设计权衡 + 面试题 + 交叉引用 + 演化追溯 + 极简复现
