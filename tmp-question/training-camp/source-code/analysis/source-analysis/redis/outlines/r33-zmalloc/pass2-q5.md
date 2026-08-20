# 闭环笔记 q5: extend_to_usable — 骗过编译器的 alloc_size 属性

## 假设
usable 家族把"实际大小 > 请求大小"的内存交给调用方使用时, 编译器 (gcc 的 _FORTIFY_SOURCE/对象大小检查) 会认为越界 — 需要一个声明"新大小"的传导函数。

## 验证过程
- zmalloc.h:110-124 (长注释): malloc_usable_size 不是设计给"使用剩余空间"的 (诊断用途), _FORTIFY_SOURCE 下编译器感知访问越界 → **SIGABRT**; 解决方案 = 假分配器函数 `extend_to_usable(ptr, size)`, 唯一作用是通过 `alloc_size(2)` 属性告诉编译器新大小
- 注释明确: "This cannot be a static inline because gcc then loses the attributes"
- zmalloc.c:84-89: 实现 `return ptr;` — 纯传导, 无实际操作
- zmalloc.h:92-95: zmalloc/zcalloc 等声明带 `__attribute__((malloc,alloc_size(1),noinline))` — malloc 属性 (指针不别名)+alloc_size (大小关联)+noinline (防属性丢失)
- 引用: systemd PR #25688 (同一技巧来源) + gcc bug 96503 (LTO 下属性丢失)
- 使用链: ztrymalloc_usable → extend_to_usable(ptr, usable_size) → 调用方 (SDS) 后续写 usable 范围内存, 编译器不报越界

## 代码类型
Glue (编译器交互) — 纯工程技巧, 无算法

## 跨域关联
- q4 (usable 消费) → extend_to_usable 是消费的前提
- gcc/LTO 工具链面 → 编译期属性契约

## 结论
usable 优化触碰了"malloc_usable_size 剩余空间不可用"的 C 语义红线: _FORTIFY_SOURCE 下编译器插桩检测越界写 → SIGABRT。extend_to_usable = alloc_size 属性的传导空洞, 让编译器相信指针大小已扩展。纯工程 hack, 但揭示了"利用分配器剩余空间"与"编译器优化"的冲突。
源码位置: zmalloc.h:125-146; zmalloc.c:84-89
