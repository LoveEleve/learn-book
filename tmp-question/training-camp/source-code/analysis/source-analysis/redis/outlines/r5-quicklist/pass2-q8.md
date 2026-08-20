# 闭环笔记 q8: 7.x 演进 + bookmarks

## 假设
quicklist 7.x 容器从 ziplist 切到 listpack (container 字段语义变化); bookmarks 是大列表分段迭代的可选锚点 (默认不用, 零开销)。

## 验证过程
- 历史: 3.2 前 Redis 用 双 linkedlist (每元素一节点) 或 小列表整体 ziplist; **2014 Matt Stancliff 引入 quicklist** (双向链表 + 每节点 ziplist 打包) — 2017 容器切 listpack (7.x 完成)
- 7.x 容器 (quicklist.h:54): container=2 (PACKED) 时 entry 是 listpack; 旧 ziplist 兼容 (加载旧 RDB 转 listpack)
- 位域 count:16: 单节点 ≤65535 元素 (listpack numele 同上限)
- bookmarks (quicklist.h:70-79,101-112): `quicklistBookmark bookmarks[]` — 大列表 (千节点) 的"分段锚点" (按名定位节点, 类似书签); 默认 bookmark_count=0 — **结构体尾部柔性数组, 不用时零内存**
  - 用途: 超大列表 (如百万节点) 的分段操作 (LINSERT 到指定段); 注释: "only be used for very big lists"
  - 代价: 删除节点时需更新引用锚点 (Q_BM_BITS=4 限制 16 个)
- compression 历史: 早期支持 per-node LZF (2015+) — 策略演进 (q3 的 #if 0 分支)

## 代码类型
Glue (演进兼容) + Interface (可选特性)

## 跨域关联
- R-19 (listpack 容器) → 7.x 载体
- R-26 (t_list) → 消费
- RDB 兼容面: 旧 ziplist 容器加载转换

## 结论
quicklist 演进: linkedlist+ziplist 混合 → quicklist (2014) → listpack 容器 (7.x); bookmarks 是"超大列表专用"的可选分段锚点 (柔性数组零默认开销, 上限 16 个)。位域 count:16 与 listpack numele 上限呼应。
源码位置: quicklist.h:47-112; quicklist.c (容器兼容)
