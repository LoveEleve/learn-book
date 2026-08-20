# RM-6 消息过滤 — completeness-questions (全视角提问验证)

## 开发者视角

1. SQL92 支持哪些运算符? (AND/OR/NOT/BETWEEN/IN/IS NULL/=TRUE)
2. TAG 和 SQL92 怎么选? (标签简单 vs 属性表达式)
3. 布隆参数怎么配? (f 误判率 + n 元素 → k/m)
4. 位图在哪算/存? (写时 CalcBitMap → CQ Ext)
5. 两级过滤的顺序? (CQ 粗筛 → CommitLog 精筛)
6. isMsgInLive 干什么? (订阅期窗口校验)
7. enableCalcFilterBitMap 默认? (false)
8. 重试消息怎么过滤? (真实 topic 数据精筛)

## 架构师视角

9. 布隆两级 vs 全量精筛的取舍? (O(1) 粗筛省解析, 误判精筛纠正)
10. 参数数学的推导? (f=(1-p)^k, k=log(0.5,f), m=n×log2(1/f)×log2(e))
11. 双哈希技巧? (Kirsch-Mitzenmacher — k 次哈希降为 2)
12. 写时位图的成本摊薄? (写路径多算, 读路径快)
13. 为什么默认关? (TAG 场景不需要, SQL92 才开)
14. SPI 扩展面? (FilterSpi 注册 — 自定义类型)
15. FilterServer 为什么废弃? (独立进程成本 vs broker 内嵌)
16. 订阅期 = 位图窗口的设计? (早于订阅的消息回退精筛)

## 学生视角

17. 布隆过滤器是什么? (位数组 + 多次哈希, 可能误判)
18. 为什么误判可以忍? (精筛兜底, 最多多读几条)
19. TAG 过滤的哈希链? (订阅 codeSet vs 消息 tagsCode)
20. 过滤在哪一层? (broker 存储层, 客户端无感知)
