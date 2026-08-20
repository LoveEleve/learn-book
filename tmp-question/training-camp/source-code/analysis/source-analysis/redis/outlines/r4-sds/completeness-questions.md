# R-4 SDS — completeness-questions

## 开发者视角

1. sds 是什么?为什么能直接 printf 又能存二进制?
2. sdslen 怎么读?为什么是 O(1)?
3. 5 种头部什么时候用哪种?sdshdr5 有什么限制?
4. sdscat 追加时什么情况原地/搬家?为什么升级不能 realloc?
5. 追加 1 字节, 为什么有时分配翻倍?
6. sdsIncrLen 的用途?正负增量各什么语义?
7. sdsclear 后缓冲还在吗?为什么要保留?
8. sdsResize 缩小后头部类型会变吗?

## 架构师视角

9. "指针即对象" (sds=char*) 的设计取舍?和结构体指针方案比?
10. 分级头的空间-时间权衡?为什么阈值是 31/255/65535?
11. 预分配 2× vs +1MB 的 1MB 分界依据?NonGreedy 为什么给 querybuf?
12. 伪降型 (保留旧头只缩 alloc) 的设计动机?什么场景收益?
13. usable 接力 (alloc=分配器实际) 与 je_nallocx 预查询 — 省了什么?
14. 双标准 (\0 + len) 的代价?什么场景会踩坑 (sdsupdatelen)?
15. 零拷贝 (read 直写 sds) 的前提条件?断言守卫防什么?
16. 导出分配器解决什么问题?Lua 为什么需要?

## 学生视角

17. sdsnewlen("hello",5) 从调用到返回的完整内存布局?
18. 存 5 字节为什么头部可能占 1 字节?sdshdr8 的 len/alloc/flags 各是什么?
19. 追加 100 次 1 字节, 预分配怎么避免 100 次 realloc?
20. 客户端发来数据怎么进 querybuf?CRLF 怎么去掉?
