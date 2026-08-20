# R-22 过期机制 — completeness-questions

## 开发者视角

1. activeExpireCycle 的 SLOW 和 FAST 有什么区别?触发条件分别是什么?
2. FAST 为什么可能"完全拒绝运行"?两个条件是?
3. 一次 SLOW 循环每个 DB 扫多少键?桶上限多少?
4. expires_cursor 是做什么的?为什么需要它?
5. HFE 主动过期的配额怎么算?积压多了怎么办?
6. 可写从库为什么需要 slaveKeysWithExpire?主库的键为什么不用记?
7. EXPIRE 命令的 NX/XX/GT/LT 各是什么语义?
8. TTL 返回 -2 和 -1 各代表什么?

## 架构师视角

9. SLOW 25% CPU 预算怎么算出来的 (公式)?为什么每 16 迭代查一次时间?
10. repeat 判定为什么用"过期比例"而非"还有过期键"?10% 门槛的 tradeoff?
11. avg_ttl 的 pow(0.98) 常数表是怎么从循环推成闭式的?
12. HFE 序列放大 ×32 封顶 — 为什么怕积压又不无限放大?
13. 传播统一到 PEXPIREAT / DEL|UNLINK 解决了什么问题 (主从时钟)?
14. checkAlreadyExpired 的 loading/masterhost 豁免是为什么?
15. 采样填充率 <1% 跳过 — 为什么等缩容而非硬扫?
16. effort 1-10 缩放四个参数 — 为什么是"加法缩放"而非乘法?

## 学生视角

17. EXPIRE key 100 的完整执行路径 (从命令到 AOF)?
18. 一条 TTL 3000ms 的键在 TTL 命令里怎么变成 3?
19. 主库键过期 → 从库怎么知道要删?
20. 一次 activeExpireCycle(SLOW) 的执行: 从调用到超时的完整循环?
21. stale_perc 5%/95% 滑动平均的意义 (FAST 触发)?
