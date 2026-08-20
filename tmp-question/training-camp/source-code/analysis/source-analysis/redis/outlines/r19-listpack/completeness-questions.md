# R-19 listpack — completeness-questions

## 开发者视角

1. listpack 的头部 6 字节是什么?末尾的 0xFF 是什么?
2. 每个元素尾部的 backlen 存的是什么?为什么要存?
3. 整数和字符串分别怎么编码?首字节怎么区分?
4. HSET myhash count 5 — "5" 存成了什么编码?
5. 插入一个长字符串, 后面的元素会受影响吗?为什么?
6. lpPrev 怎么 O(1) 找到前一个元素?
7. 删除元素和替换元素在 lpInsert 里怎么统一?
8. 元素超过 65535 个时, 头部计数还准吗?

## 架构师视角

9. backlen 存自身长度 + 固定 5B — 为什么能消灭 ziplist 的级联更新?
10. 9 种编码的前缀分层 (0/10/110/1110/1111) 设计 — 解码为什么无歧义?
11. 整数嗅探 (string2ll) 的严格性 — 为什么必须无前导零?无损往返怎么保证?
12. 扩先 realloc 后 memmove / 缩先 memmove 后 realloc — 顺序为什么不能反?
13. lpInsert 三合一 (插入/删除/替换) 的 API 设计收益?
14. hash ≤512 字段用 listpack, 超了转 dict — 阈值设计逻辑?为什么单向?
15. lpValidateIntegrity 的 total_bytes == size 检查防什么?
16. listpack vs ziplist: 为什么 7.0 全面替换?

## 学生视角

17. lpAppend("hello", 5) 的内存变化: 从头部到 entry 布局?
18. 向后遍历: lpPrev(最后一个) 怎么一步步跳回?
19. "5" 存成整数编码 vs 字符串编码, 各占多少字节?
20. hash 用 listpack 时 HSET/HGET 是怎么在线性列表里查找的?
