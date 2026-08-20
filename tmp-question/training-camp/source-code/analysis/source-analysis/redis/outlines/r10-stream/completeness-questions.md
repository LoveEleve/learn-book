# R-10 Stream+rax — completeness-questions + 时空溯源

## R-10a rax — 开发者视角

1. raxNode 的位域 (iskey/isnull/iscompr/size)?
2. 压缩节点和非压缩节点的子指针布局?
3. raxLowWalk 返回什么? parentlink 是什么?
4. ALGO 1 的 3a/3b 区别?
5. postfix 节点什么时候是"原子子"?
6. ALGO 2 的触发条件?
7. 压缩节点能承载键吗?
8. raxFind 的匹配语义?

## R-10a rax — 架构师视角

9. 路径压缩的复杂度收益?
10. 为什么 children 按位置索引而非字符值?
11. 键数据设在叶子 vs 压缩节点的取舍?
12. ALGO 1 分裂的 5 情形?
13. 删除后怎么收缩?
14. 迭代器的中序语义 (前缀优先)?
15. RAX_ITER_SAFE 的意义?
16. rax 与 B 树/字典树的对比?

## R-10a rax — 学生视角

17. 插入 "foobar" 到 "foo" 树会发生什么?
18. 插入 "ANNI" 到 "ANNIBALE" 树会发生什么?
19. 查找 "ANNI" 停在哪个节点?
20. rax 的深度是什么?

## R-10b stream — 开发者视角

1. stream 的 rax+listpack 双层结构?
2. 消息 ID 怎么生成 (ms-seq)?
3. 时钟回退怎么处理?
4. 指定 ID 的规则?
5. master entry 是什么?
6. PEL 的双层结构?
7. XACK 做什么?
8. XDEL 为什么是墓碑?

## R-10b stream — 架构师视角

9. 128bit BE ID 的字典序=ID 序设计?
10. 近似裁剪 (100×node_max_entries) 的权衡?
11. entries_read 的精确/估算语义?
12. PEL 共享 NACK 的引用设计?
13. 墓碑删除 vs 物理删除?
14. 阻塞消费与 R-26 框架的衔接?
15. Stream vs pubsub (R-29) 的本质差异?
16. XCLAIM 的幂等重投?

## R-10b stream — 学生视角

17. XADD * 两次同 ms 的 ID 是什么?
18. 消费后不 XACK 会怎样?
19. XGROUP CREATE $ 的含义?
20. MAXLEN ~ 1000 的近似语义?

# 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2017 (4.0?) | rax 由 antirez 开发 (rax.c 版权 2017; stream 计划期) |
| 5.0 | **Stream 发布** (t_stream.c 版权 2018; XADD/XREAD/XGROUP/XACK/XCLAIM/XTRIM) + rax 全面使用 |
| 5.0 | master entry 主条目压缩 (listpack 字段引用) |
| 6.2 | **stream-node-max-entries 配置** + 近似裁剪完善; XAUTOCLAIM (6.2) |
| 7.0 | entries_read 修正 (SCG_INVALID_ENTRIES_READ); XINFO 增强; RDB STREAM_LISTPACKS_3 |
| 7.4 | HFE 时代 stream 保持 rax+listpack 结构 |

## 痕迹证据

- rax.c:529-595: ALGO 1 五情形图解注释 (权威教学素材)
- rax.c:759-806: ALGO 2 完整实现 + "The key is already inserted" 注释
- rax.c:384-394: raxCompressNode 保留 iskey 注释
- t_stream.c:475-520: master entry 布局注释 (+-------+---------+...+)
- t_stream.c:122-129: streamNextID "never go backward" 注释
- stream.h:59-63: entries_read 语义注释 ("reasoning behind this value is detailed...")
- stream.h:82-88: 消费者 PEL 共享 NACK 注释

## 推断标注

- "rax 2017/stream 5.0" — 版本推断 (版权年份 + Redis 版本史)
- "XAUTOCLAIM 6.2" — 版本推断
- "近似裁剪 100× 系数" — 代码事实 (t_stream.c:866-867 limit=100*)
- "master entry 首条目不压缩" — 代码事实 (t_stream.c 注释 "first added entry is NOT represented in the master entry")

## harness 实证记录 (5 轮迭代)

| 轮 | 问题 | 修复 |
|:--:|:--|:--|
| 1 | 根节点类型错误 (压缩 vs 非压缩) | 根 = 非压缩空节点 (raxNewNode(0,0)) |
| 2 | 压缩节点必有子节点 (空叶子) — newtail 缺子 | split 产物挂空叶子 |
| 3 | **children 位置索引** (非字符值) — 关键认知 | rwalk/插入全改位置语义 |
| 4 | **ALGO 1 j==0 (3a)** splitnode 替换+iskey 继承 — 遗漏 | algo1_split 重写 |
| 5 | **ALGO 2 条件 i==len** + postfix=原节点余下+新键 | algo2_split; 键保留 (raxCompressNode L384-394); raxFind 压缩 splitpos==0 允许 |

**结论**: 32 断言全过 ASan+LSan clean; 与真实 rax.c 编译对照验证 (t.c 实测 "ANNI"/"ANNIBALE" 双键共存, raxShow 结构一致)。
