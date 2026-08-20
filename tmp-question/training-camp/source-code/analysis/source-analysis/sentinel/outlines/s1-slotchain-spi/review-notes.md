# S-1 审查记录(全流程)

> 日期: 2026-08-17 | 范围: 规划 → Pass 1 → Pass 2 → 大纲 → 三篇正文 → harness
> 审查方法: 09 怀疑审计 + 全视角提问 + 源码实证 + 费曼法复现

## 一、规划阶段审查(completeness-questions.md)

- 全视角 36 问(6 身份): ✅ 12 / ⚠️ 12 / ❌ 6
- 逼迫出的 4 个真实缺口: 单例机制(#5)/ 日志级别键(#18)/ entrySize 监控面(#19)/ SPI 概念引入(#25)
- 回补全部落地: 中篇第 2 节(SPI 概念)、中篇第 5 节(单例)、下篇第 3 节(级别)、下篇第 4 节(entrySize)

## 二、Pass 1 审查

- 09 域级审计 7 条断言全过; 补锚 2 项(S-10 传输域、CommandHandler 计数在 PLAN 阶段已修)
- 域边界再确认: slotchain 11 / spi 3 / init 3 / logger 2 / log 14 / config 2 = 35 文件

## 三、Pass 2 审查(11 闭环, 修正 3 处 PLAN 断言)

| 闭环 | 对 PLAN/既有规划的关键修正 |
|---|---|
| Q1 | 9 内置 order 全唯一; ParamFlow(-3000) 插入 System 与 Flow 之间(实证) |
| Q3 | **PLAN 修正**: "@Spi alias 多别名" → @Spi.value() 单别名, 缺省全限定类名 |
| Q5 | **文档偏差**: Env 注释 "process will exit" vs 实际 printStackTrace 不退出(以代码为准) |
| Q9 | HotParamSlotChainBuilder = @Deprecated 空类(死代码); addFirst 零消费 |

## 四、大纲审查(completeness-questions 回补后 v2)

- 结构修正: 下篇标题 "开机自检与日志" → "开机自检、日志体系与资源键"(容纳 Q11)
- 断言修正: "JDK ServiceLoader 每查必加载" → 不准确, 改为"无 order/别名/默认/单例"
- 悬念修正: 下篇核心悬念 → "block 日志为什么不走 RecordLog?"

## 五、上篇正文审查(4 轮, 修正 10 处)

| 轮 | 发现问题 |
|---|---|
| R1 | "逐字未变"过度断言 → git diff 实证: 唯一差异 = 1.8.0 prioritized 参数; exit 签名行号错误(32-36→44-47) |
| R2 | GOF 责任链对比缺失(回补第 1 节); 代码块缺"节选"标注 ×3; "计数记到节点"表述不精确; 1.7.0 对调动机未标推测 |
| R3 | MAX_SLOT_CHAIN_SIZE 行号错误(CtSph:51-54 → **Constants.java:37**); 槽抽象行号(16-38→29-47) |
| R4 | 语义工具补验(chainMap 路径); 与 completeness 36 问逐项对照 ✓ |

## 六、中篇正文审查(3 轮, 修正 10 处)

| 轮 | 发现问题 |
|---|---|
| R1 | **原型消费者重大遗漏**: NodeSelectorSlot/ClusterBuilderSlot 是 @Spi(isSingleton=false)(实证: NodeSelectorSlot.java:127, ClusterBuilderSlot.java:49)!上篇契约 6 与中篇第 5 节"原型零消费者"全错 → 重构为"持有资源绑定状态的槽必须原型, 每链一新"; 同步修正上篇契约 |
| R2 | "四块"→"六块"; CAS 误称"双检锁"(loaded 是 AtomicBoolean CAS); createInstance 行号 446→459; 表格"空时=默认实现"精度; CommandHandler 引用收窄; DataSource/Logger 过度断言 → Logger 例外(LoggerSpiProvider.java:53 循环依赖注释实证) |
| R3 | DefaultSlotChainBuilder 行号 30-32→39-42 |

## 七、下篇正文审查(2 轮, 修正 6 处)

| 轮 | 发现问题 |
|---|---|
| R1 | Env 行号(33→32); InitExecutor 行号(36-70→42-52, 56-63→57-63); **EagleEye 来历无据推测删除**(类头无注释) |
| R2 | **TokenBucket 职责误述**: "限流写盘" → 实证是给自检日志限流(EagleEye.java:46, 10s/10 次); "最后一次写入为准" → 同链同规则集、规则存储归 S-7; SphU→Env 链实证(SphU.java:85) |

## 八、harness 费曼法(3 轮修正, 终态 PASS=9/9)

| 轮 | 盲区 |
|---|---|
| R1 | 嵌套注解缺 @Retention(RUNTIME) → getAnnotation 返回 null(NPE) — 对照真实 Spi.java:26 实证保留策略知识 |
| R2 | 链序取值错误(log1 取成第 2 槽 ClusterBuilder)→ 第 3 槽才是 LogSlot; 诊断输出定位 |
| R3 | 断言分母 9/10 不一致 → PASS=9/9 |

## 九、自审检查单(01)

- [x] grep-verified 声明远超 3 条(每轮均有)
- [x] 每轮审查均发现真实内容问题(累计 30+ 处修正, 零"格式扫描式"审查)
- [x] 语义工具使用: codebase-memory search_graph(ProcessorSlot 签名验证)
- [x] 数值声明全部对照源码: 6000/542/order 值/8 InitFunc/10 槽/行号
- [x] 无类名推断: TokenBucket 职责读方法体才发现误述; NodeSelector 原型注解实证
- [x] 闭环笔记 12 文件存在(pass1 + q1-q11)
- [x] 概念覆盖: completeness 36 问全部 ✅ 后收束
- [x] 代码示例: 全文代码块标注 [伪代码] 或"节选自 file:line"
- [x] 跨段同步: 上篇契约 6 修正 → 中篇第 5 节同步(原型语义); PLAN/闭环笔记/正文三处一致

## 十、遗留(诚实清单)

1. 1.7.0 Authority/System 对调动机: 行为变化已实证, 对调 commit 说明未检索到(正文已标推测)
2. EagleEye 引擎的批处理内部(StatLogController/daemon 线程模型): 属 S-5 统计域的展开面, S-1 只取分工结论
3. SpiLoaderTest 11 个测试方法的细节断言: 未逐一对照(核心语义已被闭环覆盖, 留作 S-1 回归基准)