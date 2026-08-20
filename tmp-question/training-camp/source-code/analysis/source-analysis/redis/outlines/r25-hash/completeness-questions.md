# R-25 t_hash — completeness-questions

## 开发者视角

1. hash 的三种编码是什么?什么时候切换?
2. hashTypeSet 的三个分支各怎么写入?
3. 转换的三个触发条件?
4. HSET 和 HMSET 的回复差异?
5. 读过期字段返回什么 (GETF 枚举)?
6. HEXPIRE 的 GT/LT/NX/XX 语义?
7. httl 返回 -2/-1 各代表什么?
8. 全局 HFE 注册的时机?

## 架构师视角

9. 编码三态为什么单向升级 (与 intset 同哲学)?
10. dictExpand 预扩在转换中的作用 (免 rehash)?
11. HFE 惰性链的 GETF_EXPIRED_HASH — 为什么需要这个返回值?
12. 字段条件 TTL 与键级 EXPIRE 的语义同构?
13. 两级注册 (全局代理 + 私有明细) 的调度优势?
14. trash 标记在转换期的状态机作用?
15. skipExpiredFields 优化 (HGETALL) 的原理?
16. HFE 三阶段框架 (Init/SetEx/Done) 为什么批量聚合?

## 学生视角

17. HSET k f v 到 100 万字段, 编码怎么演变?
18. HGET 一个过期字段: 完整删除链?
19. HEXPIRE k f 100 GT 不满足会发生什么?
20. 转换期 hash 的 HFE 元数据怎么迁移?
21. 一个 500 字段 hash 的内存分布 (listpack vs dict)?
