# R-23 内存淘汰 — completeness-questions

## 开发者视角

1. performEvictions 的三态返回各是什么意思?谁消费它们?
2. maxmemory 超了, 什么命令会被拒绝 (denyoom)?
3. 采样池多大?每次采样几个键?
4. volatile 和 allkeys 策略的本质区别 (采样源)?
5. LRU 的 idle 时间怎么算?24bit 回绕怎么处理?
6. LFU 的计数器为什么是"对数"的?新键为什么从 5 起跳?
7. tenacity 怎么换算成时间上限?
8. 淘汰一个键都做了什么 (删除+通知+传播)?

## 架构师视角

9. 为什么"跨 DB 全局池"比"每 DB 独立池"淘汰质量高?
10. overhead 剔除 (AOF/repl 缓冲) 为什么能防 DEL 反馈环?
11. LRU 降精度时钟 (24bit×1000ms) 的取舍 — 为什么不是精确时钟?
12. LFU 概率递增公式 p=1/(base×factor+1) 的数学意义?
13. 幽灵键机制 — 池不随删除更新, 为什么还能工作?
14. tenacity 线性→几何→无限的三级设计意图?
15. 每 16 键周期的三个检查为什么恰好是这三个 (从库/内存/时间)?
16. EVICT_RUNNING + aeTimeProc 异步续清 — 为什么不等清完再回命令?

## 学生视角

17. GET 一个键后它的 lru 字段怎么变?淘汰时怎么算 idle?
18. 一个键被访问 100 万次, LFU 计数大概到多少?
19. 一次 performEvictions 的完整流程 (从命令到淘汰)?
20. noeviction 策略下内存满了写命令会怎样?
21. volatile-ttl 为什么不需要查值对象?
