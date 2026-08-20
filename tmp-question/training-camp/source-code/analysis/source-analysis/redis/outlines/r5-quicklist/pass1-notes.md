# R-5 quicklist — Pass 1 探索笔记

> 域: R-5 quicklist (list 存储) | 🔴 方案 A | 2026-08-13
> 源码: src/quicklist.c (3334) + quicklist.h (215) | Redis 7.4.2
> 已读测试: quicklistTest (quicklist.c, 39 断言族): 优化偏移/压缩/分裂/迭代

## 继承树/调用图

```
quicklistNode (quicklist.h:47-59):
  prev/next + entry (listpack 或 PLAIN 裸元素或 LZF)
  sz (entry 字节) + 位域: count:16 / encoding:2 (RAW/LZF) / container:2 (PLAIN/PACKED)
  recompress:1 (读时解压后要重压) / attempted_compress / dont_compress / extra:9

quicklistLZF (quicklist.h:66-69): sz + compressed[] (LZF 数据)

quicklist (quicklist.h:101-112, 64 位 40B):
  head/tail + count (总元素) + len (节点数)
  fill:16 (负=字节 2^k 映射 / 正=元素数) + compress:16 (两端不压深度) + bookmark_count:4
  bookmarks[] (大列表分段迭代的锚点, 可选)

quicklistIter: current 节点 + offset/zi 位置 + direction

核心操作:
  quicklistCreate (L127): fill=-2 (默认 8KB), compress=0
  quicklistPushHead/Tail (L583-625): 三路 — 大元素→PLAIN 节点 (isLargeElement)
    / 允许则 lpPrepend/lpAppend / 不允许则新节点
  _quicklistSplitNode (L971-1004): 分裂 = 复制 listpack → lpDeleteRange 双侧裁剪
  _quicklistInsert (L1010+): 中间插入 → 可能分裂 (fill 超限)
  __quicklistCompressNode (L214-245): MIN_COMPRESS_BYTES 阈值 + lzf_compress
    + MIN_COMPRESS_IMPROVE (收益不足不压) + dont_compress 守卫
  __quicklistCompress (L307+): 两端 compress*2 深度内不压; #if 0 旧版显式分支
  DecompressNode + recompress 标志 (L260-290): 读时解压, 用后重压
  配置 (config.c:3152,3174): list-max-listpack-size=-2 (默认 8KB), list-compress-depth=0
  optimization_level = {4096, 8192, 16384, 32768, 65536} (负 fill 映射, L49)
```

## 基本元素分解

1. **双向链表分页**: quicklist = 双向链表, 每节点一个 listpack (小元素打包)
2. **双容器**: PACKED (listpack) / PLAIN (大元素裸节点)
3. **fill 双语义**: 正数=元素数上限, 负数=字节上限 (2^k 映射表)
4. **LZF 压缩**: 两端深度内不压 + 压缩收益阈值
5. **recompress 延迟重压**: 访问压缩节点先解压, 用完标记重压
6. **节点分裂/合并**: 中间插入分裂, 相邻合并
7. **bookmarks**: 大列表分段迭代锚点 (可选)

## 标记问题 (9 个)

1. 双容器 (PLAIN/PACKED): 大元素为什么单独成节点? (isLargeElement)
2. fill 双语义: 正数元素数/负数字节数 (2^k 映射) — 为什么两种?默认 -2=8KB?
3. 压缩策略: 两端深度 + MIN_COMPRESS_BYTES/IMPROVE — 为什么不压头尾?
4. recompress 机制: 读时解压 → 用完重压 — 压缩节点的访问成本?
5. 节点分裂: 复制 + lpDeleteRange 裁剪 — 为什么不用移动?
6. PushHead 三路: PLAIN / prepend / 新节点 — 判定逻辑?
7. 压缩后删除/迭代: LZF 节点上的操作怎么处理?
8. 7.x 演进: ziplist → listpack 容器 (何时切换)?
9. bookmarks 用途?

## 时空溯源 (代码内痕迹)

- 2014 (Matt Stancliff): quicklist 替代双 linkedlist + ziplist 混合 (3.2 之前: 小列表 ziplist 整体, 大列表 linkedlist)
- 7.x: 容器 ziplist → listpack (quicklistNode 的 container 字段语义变化)
- #if 0 压缩优化块: 显式 depth 分支被通用迭代替代
- packed_threshold: 测试专用 (PLAIN 阈值)
