# R-21 db 键空间 — completeness-questions

## 开发者视角

1. lookupKey 查一个键, 除了拿值还做了哪些事?怎么关掉?
2. expireIfNeeded 的三个返回值什么语义?从库为什么只报过期不删?
3. SET 覆盖旧值、DEL 删除键, keys/expires/hexpires 三张表怎么保持一致?
4. expires 表的键为什么不用拷贝?值为什么不用分配?
5. 单机 (非 cluster) 的键空间也是 kvstore 吗?分几个 dict?
6. SCAN 的游标在一个 dict 和 16384 个 dict 之间有什么不同?
7. ebuckets 的 item 为什么要内嵌 ExpireMeta?指针低 1 位存什么?
8. lazyfree 的 freeObjAsync 什么时候走异步?阈值是多少?

## 架构师视角

9. kvstore 分片的真实动机 (cluster 槽定位) — 单机退化为 1 dict 的意义?
10. 游标 48+bits 复用 dictScan 语义 — 为什么"不新增扫描语义"是优点?
11. Fenwick 树 (dict_size_index) 解决什么问题?O(log n) 选桶/跳桶怎么做到的?
12. ebuckets list→rax→segment 三级的动机 (内存 vs 批量删除)?
13. EB_BUCKET_KEY_PRECISION=0 (TBD 10) 的取舍 — 主动过期近似 vs 惰性精确?
14. HFE 两级注册 (全局早到表 + hash 本地) — 为什么只挂最早字段?
15. lazyfree 阈值 64 与 effort 估算 — 为什么不按字节而是按"分配数"?
16. FLUSHDB ASYNC 的换表法 — 为什么比逐键删除快?

## 学生视角

17. GET 一条过期键: 从 lookupKey 到 DEL 传播的完整路径?
18. SET k v EX 100: setExpire 里键为什么是"零拷贝"的?
19. RANDOMKEY 在分片下怎么做到无偏 (FAIR)?
20. ebExpire 一次调用删到什么程度停?nextExpireTime 给谁用?
21. 一次完整 SCAN: 游标怎么跨 dict 跳转 (48+14 位)?
