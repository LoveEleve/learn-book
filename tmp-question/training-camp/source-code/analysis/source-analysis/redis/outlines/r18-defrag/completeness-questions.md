# R-18 内存碎片 — completeness-questions (全视角提问验证)

## 开发者视角

1. je_get_defrag_hint 判定"值得搬"的标准? (slab 利用率 ≤ 平均+12.5%, 跳过 slabcur)
2. 为什么必须 no_tcache 分配? (防 thread cache 立刻复用同一块)
3. EMBSTR 搬移为什么特殊? (sds 内嵌 robj 分配, 需重算偏移)
4. expected_refcount 参数的作用? (共享对象不搬)
5. 键名搬移后 expires 表怎么同步? (哈希+旧指针定位, 不能字符串查找)
6. 大键延后的阈值和续扫方式? (max-scan-fields=1000; bookmark/游标/static-last 三种)
7. 搬移失败 (hint=0) 的统计? (misses++)
8. CONFIG SET 后什么时候生效? (configuration_changed 立即决策)

## 架构师视角

9. 为什么碎片判定交给 jemalloc 而不是自己做? (逐指针零成本过滤)
10. 双门槛 AND 语义的设计动机? (低碎片率高浪费不启动 / 高碎片率低浪费不启动)
11. effort 为什么只升不降? (扫描中途降级无意义)
12. fork 暂停 vs 常态扫描的取舍? (COW 写页放大)
13. 四阶段顺序的意义? (keys 主战场 → expires 计数 → pubsub 两表)
14. 大键延后如何与主扫描协调? (每桶前清积压, 游标不受影响)
15. whileBlockedCron 补齐的动机? (阻塞命令期间预算守恒)
16. digest 校验为什么是黄金测试? (搬移零数据损坏的强证明)

## 学生视角

17. 碎片是什么? 为什么重启能治? (RSS 高 vs 逻辑内存)
18. "搬移"为什么不是移动内存而是"新分配+拷贝+释放"? (分配器 API 决定)
19. 为什么只有小对象产生可治碎片? (slab 内部空洞)
20. 在线整理和 maxmemory 淘汰什么关系? (一个治碎片一个治总量)
