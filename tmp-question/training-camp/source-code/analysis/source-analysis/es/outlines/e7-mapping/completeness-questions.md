# E-7 Mapping — 全视角提问验证 (completeness)

> 验证时机: 3 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生
> 覆盖统计: 见文末

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 类型体系 | Mapper/FieldMapper/ObjectMapper/MetadataFieldMapper 边界是什么? | ✅ 01-L2 |
| 2 | 开发者 | Parameter | 新增一个字段参数要走哪些步骤? (parser/validator/serializer) | ✅ 01-L3 (五元组) |
| 3 | 开发者 | Parameter | requires/precludes 互斥怎么声明? | ✅ 01-L3 (script 与 indexed 互斥) |
| 4 | 开发者 | 解析 | parseCreateField 抛异常后 parser 位置在哪? | ✅ 02-L4 (rethrow 重读 preview) |
| 5 | 开发者 | 动态 | 动态建字段后什么时候固化成正式 mapping? | ✅ 03-L5 (createDynamicUpdate) |
| 6 | 架构师 | 类型体系 | 为什么分三级 (Field/Object/Metadata)? | ✅ 01-L2 (生命周期不同) |
| 7 | 架构师 | 合并 | 为什么 type 不可改? 参数为什么部分可更新? | ✅ 01-L4 (索引面已写入) |
| 8 | 架构师 | 参数化 | 为什么 2020 要重构 Parameter? 收益? | ✅ 01-L3 (时空溯源: 四件事收敛) |
| 9 | 架构师 | 动态 | RUNTIME 态的设计意图 (vs 具体类型)? | ⚠️ 03-L2 提了不展开 → 补一句 (runtime field 不落盘, 查询时计算) |
| 10 | 性能工程师 | 解析 | 解析路径的性能热点? (逐字段 new Field?) | ⚠️ 02 未提 → 补一句 (Field 复用/context.doc 累积) |
| 11 | 性能工程师 | 动态 | numeric/date detection 的开销? 可关吗? | ✅ 03-L3 (三开关索引级) |
| 12 | SRE | 错误工程 | 线上解析报错怎么快速定位? (preview 价值) | ✅ 02-L4 (三要素错误消息) |
| 13 | SRE | 动态 | 动态映射误判 (数字当 text) 线上怎么处理? | ✅ 03-L6 (只能重建索引) |
| 14 | SRE | 合并 | mapping 更新冲突对线上影响? (拒绝更新) | ✅ 01-L4 (冲突抛错) |
| 15 | 研究者 | 类型体系 | ES 类型体系 vs MySQL 字段类型 vs Redis 无类型? | ✅ 01-L5 + 03-L6 (对照收束) |
| 16 | 研究者 | 参数化 | 声明式参数 vs 手写 Builder 的取舍? | ✅ 01-L3 (时空溯源) |
| 17 | 研究者 | 动态 | dynamic 三态 vs 四态演进 (RUNTIME 何时加入)? | ✅ 03-L2 (8.x 新增) |
| 18 | 子系统开发者 | 解析 | DocumentMapper.parse 的产物谁消费? (E-1) | ✅ 02-L5 (ParsedDocument → Engine) |
| 19 | 子系统开发者 | 动态 | 动态映射更新怎么发布? (E-10 衔接) | ✅ 03-L5 (ClusterState 发布) |
| 20 | 子系统开发者 | 解析 | docValues 面谁建? (E-11 衔接) | ✅ 02-L5 (hasDocValues 隐式) |
| 21 | 学生 | 类型体系 | text 和 keyword 到底差在哪? | ✅ 01-L5 提及 + 02-L3 (四变体) |
| 22 | 学生 | 解析 | "age": "abc" 会发生什么? | ✅ 02-L4 (抛错/ignoreMalformed) |
| 23 | 学生 | 动态 | 为什么 "25" 可能被猜成 long? | ✅ 03-L3 (推断算法) |
| 24 | 学生 | 动态 | strict 模式为什么存在? | ✅ 03-L2 (四态) |
| 25 | 学生 | 合并 | 为什么 text 不能改成 keyword? | ✅ 01-L4 |
| 26 | 开发者 | 合并 | 同类型参数合并 (如 ignore_above 改值) 会怎样? | ⚠️ 01-L4 讲冲突, 未讲可更新参数成功合并 → 补一句 (updateable 覆盖) |
| 27 | 性能工程师 | 解析 | coerce/ignoreMalformed 默认值对性能影响? | ✅ 02-L4 (遮蔽坑) |
| 28 | SRE | 错误工程 | "Could not parse field value preview" 何时出现? | ✅ 02-L4 (preview 读失败降级) |

**统计**: ✅ 25 / ⚠️ 3 / ❌ 0 — ⚠️ 项全部为"补一句"级
→ 回大纲补全 (3 项: 01-L4 updateable 成功合并 / 02-L2 解析性能 / 03-L2 RUNTIME 设计意图)

## 回补清单

1. 01-L4: 补"可更新参数合并成功路径"一句 (updateable=true 直接覆盖)
2. 02-L2: 补解析性能一句 (Field 复用 + context.doc 累积 + 单遍遍历)
3. 03-L2: 补 RUNTIME 设计意图一句 (runtime field 不落具体索引面, 查询时计算 — 减少 schema 固化负担)
