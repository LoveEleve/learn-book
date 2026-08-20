# 闭环笔记 q1: 压缩面 — 阈值 + 链式解压

## 假设
大 undo_log 压缩存储; 解压按 context 记录的类型恢复。

## 验证过程
- **压缩触发** (AbstractUndoLogManager:571-573, S-2 实证): **needCompress = ROLLBACK_INFO_COMPRESS_ENABLE (默认 true) && length > 64k (严格大于)** — 默认 zip
- **压缩写入** (L287-302, S-2): CompressorFactory.getCompressor(type).compress → context 记录 **COMPRESSOR_TYPE_KEY**
- **解压链** (L519-544, S-2): getRollbackInfo → **compressorType from context (默认 NONE)** → decompress — 无压缩记录跳过解压
- **压缩类型 SPI**: CompressorFactory (CompressorType 枚举: NONE/ZIP/GZIP/... + SPI 实现) — 可扩展
- **对照面**: ZK 快照压缩 (4.3: GZIP/SNAPPY/CHECKED) — 同思路不同实现

## 代码类型
Implementation (压缩链)

## 跨域关联
- S-2: 压缩触发/解压 (本域是汇总面)
- Z-9 (4.3): ZK 快照压缩对照

## 结论
压缩 = >64k 触发 (默认 zip) + context 记录类型 + 解压按记录恢复; SPI 可扩展。
源码位置: AbstractUndoLogManager.java:519-544,571-573; DefaultValues.java:368-380
