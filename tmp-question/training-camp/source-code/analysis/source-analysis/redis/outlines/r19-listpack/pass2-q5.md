# 闭环笔记 q5: 双向遍历 — lpPrev 的 backlen 跳回

## 假设
向后遍历 O(1)/元素: 当前 entry 的前 1 字节 = 前驱 backlen 的末尾 → 解码前驱长度 → 跳回前驱起点; 无需从头扫。

## 验证过程
- lpPrev (listpack.c:473-482):
  - L475: `if (p-lp == LP_HDR_SIZE) return NULL` — 已到第一个元素 (后面就是头部)
  - L476: `p--` — **当前 entry 起点 - 1 = 前驱 backlen 的最后一个字节** (entry 布局: [enc+data][backlen] 紧密相连)
  - L477: `prevlen = lpDecodeBacklen(p)` — 解码前驱自身长度
  - L478: `prevlen += lpEncodeBacklen(NULL,prevlen)` — 加上前驱的 backlen 大小 = 前驱总大小
  - L479: `p -= prevlen-1` — 跳回前驱起点
- 依赖: backlen 是**前缀无歧义编码** (LP_MAX_BACKLEN_SIZE=5, lpEncodeBacklen L335-374: 首字节高位连续 1 表示后续字节数)
- lpLast (L495-498): 从 EOF 往前 = lpPrev(EOF) — 第一个元素
- 双向遍历的代价: 每 entry +1~5B backlen 空间 — 空间换 O(1) 双向

## 代码类型
Algorithmic (双向链表式布局)

## 跨域关联
- q1 (backlen 语义) → 本机制的依赖
- R-10 (t_stream 消费组反向扫描) → 向后遍历场景
- R-25 (hash 反向迭代) → 同

## 结论
lpPrev O(1): 前驱 backlen 紧贴当前 entry 前 — 解码长度直接跳回。backlen 的 1-5B 前缀编码 (连续 1 计数) 保证解码无歧义。空间 (每 entry ≤5B) 换双向遍历 — 与 ziplist 的 prevlen 同思路, 但无级联 (q1)。
源码位置: listpack.c:335-374,473-482
