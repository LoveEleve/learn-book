# 交接文档 — Microsphere 训练营知识点提取

> **本文件是唯一权威的进度与约定文档。** 接手前请**完整阅读**本文（含工作方法、当前状态、交接约定），再动任何文件。
> 最后更新：2026-08-12 | stage-1/2/3 提取全部完成（含结营文档 `[跳过]` 处理）；下一步见 §四.5

---

## 一、任务背景

对**小马哥（mercyblitz）训练营 + microsphere 生态**做系统性的知识点提取，最终产出：
1. **训练营课程知识点**（stage-1~4 的 docs + 示例代码 + 论文）
2. **microsphere 生态源码知识点**（核心代码仓库）
3. **合并的维度化总教学大纲**（按 5 大维度组织）+ 课程↔源码关联分析

**核心视角（08 SOP）**：小马哥课程/源码**只是参考**，不是知识本体。每个知识点走三层次：需求 → 自主实现 → 参考实现 → 对比取舍。

**重要升级（本会话新增）**：
- **09-前置知识与掌握度 SOP**：每篇标注前置知识 + 读者掌握度（用户画像：读源码多、Spring 熟、Maven/工程化工具弱）
- **架构师视角补全机制（08 增强）**：每篇强制补全"完整认知/关键权衡/常见坑/生态位置/结论/来源标注"，防"井底之蛙"——不只转述 docs
- **参考实现按主流性选（08 §1.3.1）**：不照搬标题，国内主流优先（如容错用 Sentinel 非 Resilience4j；Ribbon 过时→LoadBalancer；Sleuth 过时→Micrometer Tracing）

---

## 二、方法论框架（先读这些）

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/microsphere-extraction/
├── index/zh/README.md              ← 入口 + G0 盘问闸 + 执行流程
├── methodology/zh/  (10 SOP)
│   ├── 00-项目清单与依赖.md         ← 两主体清单（36 仓库已核实）
│   ├── 01-训练营课程提取.md
│   ├── 02-源码逐文件提取.md
│   ├── 03-聚合与深度分类.md        ← 5 大维度 + 提取权重(核心/支撑/边缘)
│   ├── 04-过时内容处理.md          ← 三级过时【JDK11/17】+ 命名空间迁移 + 现代替代物细节
│   ├── 05-聚类与教学顺序.md
│   ├── 06-关联分析.md
│   ├── 07-产出格式.md              ← 产出物三级(L1提取表/L2聚合/L3教学大纲) + 架构师补全模板
│   ├── 08-需求转换与自主实现视角.md ★贯穿 + 架构师补全 + 参考实现按主流
│   └── 09-前置知识与掌握度对齐.md  ★贯穿
├── prompt/zh/self-constraint-prompt.md   ← 已沉淀多条 review 教训(见下)
├── skills/zh/ (01-快速参考 + grill-me)
└── progress/                       ← 本目录（交接 + 每单元进度）
```

**5 大维度**（03，总大纲按此组织，非按仓库）：`[规范]` / `[分布式问题]` / `[分布式理论]` / `[工程问题]` / `[性能优化]`

**产出物三级**（07，重要约定）：
- **L1 提取表**（当前每篇产出）= **中间素材，不是给读者读的最终产物**，元数据(维度/权重/深度/过时/置信度)必须齐全
- **L2 聚合/聚类文档** = 中间素材
- **L3 总教学大纲 + 逐知识点讲解** = 最终交付物（未开始）

**参考实现来源优先级**（08 §1.3.1）：
1. `code/spring/` 官方框架源码（sentinel/spring-framework/spring-boot/spring-cloud-* 等，有源码优先）
2. 小马哥 biz-project / microsphere 代码
3. docs 描述（无源码时的经验参考）
> 参考实现按**主流性/生态适用性**选，不照搬 docs 标题（国内主流优先：容错用 Sentinel 非 Resilience4j、Ribbon 过时→LoadBalancer、Sleuth 过时→Micrometer Tracing）

**prompt 已沉淀的 review 教训**（后续写篇务必遵守）：
1. 命名空间迁移(javax→jakarta)≠机制
2. 幂等/容错等承载核心决策的功能不得降为支撑
3. docs 举例技术要实际工程验证过时（FastJSON→Jackson）
4. 机制关系先源码验证再表述（@RefreshScope vs rebinder 是配合）
5. 代码"看起来在做"≠真的实现（查 TODO，如 CpuUsageLoadBalancer）
6. 标"过时→替代"必须给现代替代物细节（源码验证）
7. docs 明确结论置信度应为 High（不过度保守）
8. **交叉引用前必须核对目标章节标题**（高频错误：曾把 WebFlux 误指第 16 节 Push 监控、把第 15 节当配置模块、把第 19 节当网关负载均衡）
9. **现状核对不得吞没知识本体**（2026-08-12 沉淀，08 SOP §5-4 + prompt §3.5/§4）：my-xhs 未用/未触发 ≠ 主题知识一句话带过——知识规划（需求/自主实现/参考实现/触发条件/分阶段路径）独立于案例载体完整落位；判据：知识本体 KP 出现"现状"字样 = FAIL（反例：stage-4 01 篇跨区域多活曾被"无诉求"吞没→补 §七 规划；正例：stage-3 23 篇"范式学习与工程选型分离"）

---

## 三、源码与环境位置

### 训练营课程
```
/data/workspace/java-training-camp/stage-{1..4}/docs/   ← 课程 docs
/data/workspace/java-training-camp/stage-1/src/biz-project/       ← stage-1 示例
/data/workspace/java-training-camp/stage-2/src/middleware-projects/ ← stage-2 示例
/data/workspace/java-training-camp/stage-2/papers/                ← 10 篇论文
```

### microsphere 生态源码（参考实现验证用）
```
/data/workspace/source-code/code/microsphere/
/data/workspace/java-training-camp/cloud-native-code/   ← 更全
```
**框架源码（参考实现优先）**：`/data/workspace/source-code/code/spring/`（spring-framework/spring-boot/spring-cloud-*/sentinel/micrometer/micrometer-tracing/netty/tomcat 等，有源码优先用源码验证）

### JDK 源码（过时验证，JDK11/JDK17 基准）
```
/data/workspace/source-code/openjdk11u/    ← JDK11
/data/workspace/source-code/code/spring/jdk17/  ← JDK17
/data/workspace/source-code/openjdk-book/  ← 格物致知 OpenJDK 分析
```

---

## 四、当前状态（2026-08-11）

### 方法论：✅ 完整（10 SOP + prompt + skills），经多次 review 沉淀

### stage-1 提取：✅ 全部完成（24 篇 docs，5/6 i18n 跳过，20 篇产出）

- **1-4**:工程化(Maven/业务模板)、REST 服务端/客户端
- **7-10**:Tomcat 容错、Web 容错(Sentinel)、整合第三方、动态变更
- **11-12**:监控指标负载均衡、动态权重
- **13-16**:Micrometer 基础/整合、Pull 监控、Push 监控
- **17-18**:链路追踪、Java Instrument 重构
- **19-20**:网关稳定性、网关可观测
- **21-22**:Spring Web/Cloud 性能优化
- **23-24**:Spring 脚手架运用/架构/定制 + 原理/实现/扩展

### stage-2 提取：✅ 全部完成（28 篇 docs，产出在 `progress/course/stage-2/`）

- **1-4**:CAP/BASE、Paxos、Raft、ZAB（分布式理论）
- **5-6**:SOFAJRaft 实现/架构（源码验证）
- **7-8**:Nacos 2.x Raft/Distro（AP/CP 双协议，源码验证）
- **9-12**:Zookeeper 数据模型/通讯会话/共识实现/共识运用（源码验证）
- **13-16**:Java EE/Spring 本地事务、JTA/XA、分布式事务整合（JDK17 验证）
- **17-20**:可靠事件(本地消息表)、TCC、Seata 架构上下（rocketmq/seata 源码验证）
- **21-22**:RPC 微内核、RPC 生态整合（netty/dubbo 源码验证）
- **23-24**:配置中心设计、配置客户端设计（nacos/spring-core 源码验证）
- **25-26**:读写分离设计、ShardingSphere（shardingsphere 源码验证）
- **27-28**:分布式缓存设计/实战（redis/redisson 源码验证；Redis/Redisson 深挖后续单独规划）

> **stage-1/2 L1 提取全部完成。** 每篇均经深 review（穷尽性/源码行号核对/过时工具模式区分/空节标注/置信度）。L2 聚合/聚类、L3 总大纲未开始（07 约定：两主体都提取完再合并）。

**下一步（stage-3）**：**全部完成（2026-08-12）**——stage-3-01~33 全部提取（33 篇 docs，15 缺失跳过；含损坏文档 14 清理重建、空节/极短篇发散重建）；**结营文档已完成 `[跳过]` 处理（2026-08-12——`progress/course/stage-3/stage-3-结营.md`：无新教学知识点，阿里云 2023.11.12 事故案例作高可用主题关联素材）**。**stage-3 细节见 `progress/HANDOVER-session004.md`（详细交接：决策记录/机制演进/差距全景/待验证 37 项/源码索引/教训）**。**关键决策**：①（G0 决策 B）案例载体 Shopizer → my-xhs（`/data/workspace/my-xhs`），docs 方法论主线不变、实例验证锚定 my-xhs、差异显式标注；②（G0 决策 B2）**stage-3 产出 = 提取 + 现状核对**——每篇含"现状核对"小节（docs 升级目标 vs my-xhs 落地实证 + 差距清单），07-10 的 Eureka 主题按"Nacos 讲机制、Eureka 仅 docs 场景"处理（不实操）。详见 `progress/HANDOVER-session003.md`。

### 仓库清单：✅ 已核实（00）
- 官方 36 仓库；microsphere-test 已拉取(纯配置)；3 个站点/UI 仓库跳过；本地独有 confucius/shopizer/segmentfault

---

## 四.5 接手须知（给下一个 AI——stage-4 接手必读）

> **本文件（HANDOVER.md）是唯一权威进度文档，接手前完整阅读。** 以下为接手 stage-4 的全部须知（2026-08-12 固化，勿依赖口头传达）。

### 必读文件最小集合（复制给接手 AI）

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/microsphere-extraction/
├── progress/HANDOVER.md                 ← 本文件（权威进度——先读）
├── progress/HANDOVER-session004.md      ← stage-3 详细交接（机制/教训/差距——建议读）
└── prompt/zh/self-constraint-prompt.md  ← 绑定契约（含 §3.5 写入时自查 + §3.5.2 深度 review 七项——必读）
```

> 历史 `HANDOVER-session003.md` 已被 session004 取代（忽略）；`progress/my-xhs-优化规划.md` 为 stage-3 B2 模式落地产物（stage-4 用不上，除非延续 B2 模式）；`progress/course/stage-{1,2,3}/` 产出在交叉引用时按需读。

### 源码与数据位置

| 用途 | 路径 |
|------|------|
| 案例载体（my-xhs，19 模块/15 服务） | `/data/workspace/my-xhs` |
| 训练营 docs | `/data/workspace/java-training-camp/stage-{1..4}/docs/`（stage-4 是下一步） |
| 框架源码（参考实现优先） | `/data/workspace/source-code/code/spring/`（spring-framework/spring-boot/spring-cloud-*/sentinel/nacos 等） |
| JDK 源码（JDK11/17 基准） | `/data/workspace/source-code/openjdk11u/` + `/data/workspace/source-code/code/spring/jdk17/` |
| microsphere 生态 | `/data/workspace/source-code/code/microsphere/` + `/data/workspace/java-training-camp/cloud-native-code/` |
| my-xhs 业务梳理文档（仅参考线索，以代码为准） | `/data/workspace/my-xhs/docs/test-2/` |

### git 约定

- 仓库：`/data/workspace/source-code/book/成长之路`，分支 `fresh`
- 远端：`git@github.com:LoveEleve/learn-book.git`
- **只提交 microsphere-extraction 相关文件**；**不要碰** `tmp-question/training-camp/source-code/analysis/source-analysis/issue/HANDOVER.md` 及其他项目未提交改动（`git status` 里他人项目的 M/?? 一律不动）

### 下一个任务（stage-4）

- 目录：`/data/workspace/java-training-camp/stage-4/docs/`（先 `ls` 确认清单）
- 按方法论流程：G0 盘问（grill-me）确认范围/深度/顺序 → 逐篇提取（01 SOP）→ 每篇交付前跑 §3.5 + §3.5.2 双层检查 → 附深度 review 七项报告 → 用户确认后 commit+push
- stage-4 主题（预判）：配置中心/服务治理等深化（以实际 docs 为准）
- 若延续 B2 模式（提取+现状核对 my-xhs）需与用户确认；默认按方法论 01/02 SOP 标准流程

### 关键纪律提醒（stage-1/2/3 沉淀的教训）

1. 数字必须逐条数 docs 原文（awk/grep），不得凭印象（22/25 篇教训）
2. 类名在**写入时** grep 验证（§3.5 第①条），不得"写了才查"
3. 版本断言核实（etcd 无 8.x 教训）
4. 交叉引用必须核对目标篇内容真实存在（§3.5.2 ⑤b）
5. review 三形态：存在性 + 核对性 + 批判性（主动证伪知识断言）
6. "看起来在做≠真的实现"（查 TODO/模拟实现）
7. docs 场景 vs my-xhs 现状差异显式标注；参考实现按主流性（Eureka→Nacos 纪律）

## 五、工作方法（铁律）

1. **G0 盘问（grill-me）**：每个任务/新单元前盘问"对象/范围/深度/顺序"，达成共识才动手
2. **禁止批量操作**：默认一次只做 1 个单元（一篇 docs），完成后停下交用户 review
3. **事实自查**：能用 bash/MCP/源码查到的事实不问用户；决策等用户
4. **穷尽性**：逐篇扫描，不遗漏
5. **三层次视角**：每个知识点带"需求+自主实现+参考实现+对比取舍"
6. **JDK11/17 验证**：JDK 机制断言用 JDK11/JDK17 源码验证，验证不了标 `[待验证]`
7. **每单元 review 门**：完成后停下交用户确认
8. **架构师补全**：每篇含"完整认知/权衡/坑/生态位置/结论/来源标注"
9. **每写完一篇及时 commit + push**（不堆积）
10. **交叉引用前核对章节标题**（避免张冠李戴）

---

## 六、产出位置

```
progress/
├── HANDOVER.md            ← 本文（权威进度）
├── 提取执行计划与进度.md   ← 每单元状态追踪表
├── 补充大纲-现代分布式事务实践.md ← 补充教学大纲(Transaction Outbox + 事件驱动/CDC, docs缺口, 供后续AI讲解)
├── course/                ← 训练营课程提取文档（按期分目录：stage-1/、stage-2/，stage-1 全完成）
├── source/                ← microsphere 源码提取文档（未开始）
└── outline/               ← 最终维度化总教学大纲（未开始）
```

---

## 七、交接约定（切换 session 时）

上下文满了切换 session 时：
1. **写新的 `progress/HANDOVER-{session}.md`** 记录本 session 完成单元、产出、未决问题
2. **更新 `HANDOVER.md`** 的"当前状态"
3. 下个 session 接手：先读 `HANDOVER.md` → 方法论 index → 继续未完成单元

**本次移交的教训（重要）**：上一个会话在编辑第 22 篇时陷入重复调用工具的循环。接手时如遇工具反复执行同一操作，应**停下来写交接文档让下个会话接手**，而非继续循环。
