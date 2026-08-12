# stage-3 · 第 32 节：第二十二节："高并发、高性能与高可用" Java Native 应用 — 知识点提取

> 课程：stage-3 三高架构 第 32 节（Native 组收官 28,31-32）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/32. 第二十二节："高并发、高性能与高可用"Java Native 应用.md`
> 提取时间：2026-08-12 | 权重：核心（Java Native 方向 + Native 组收官汇总）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）

> **文档形态**：docs 为**最短篇（3 行）**——仅 1 标题（Java 版本历史）+ 1 维基百科链接——知识本体 = 架构师发散 + 28/31 篇（Native 组）收官汇总（08 §2）。

---

## 一、本节概览

- **技术域**：Java 版本历史（LTS 线）、Java Native 方向（Project Leyden/AOT 演进）、Native 三路径汇总
- **维度**：`[规范]`（Java 版本）+ `[工程问题]`（Native 方向）
- **核心命题**：**Java Native 的方向与 Native 组收官**——docs 仅"Java 版本历史"标题+链接；Java Native（docs 标题主题）在 Java 生态的演进方向（Project Leyden）
- **知识点数**：4 个
- **前置**：28 篇（GraalVM Native）、31 篇（Spring Native——Native 组汇总基础）

## 前置条件清单
读者需先掌握：
1. **GraalVM Native Image**（28 篇 KP-04/05）
2. **Spring AOT**（31 篇 KP-01/03）
3. **JDK LTS 线**（8/11/17/21）
未达前置者，先补：28/31 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **发散为主**：docs 3 行 → 发散重建（08 §2 极限案例之四——最短篇）
- **收官汇总**：Native 组（28/31/32）三路径闭环
- **实例对照**：my-xhs JDK17 标准栈（28/31 篇延续）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Java 版本历史与 LTS 线（docs 唯一内容）
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：无
- **来源**：docs §Java 版本历史（维基百科链接）+ 架构师发散
- **需求**：掌握 **Java 版本演进主线**——docs 唯一内容：Java 版本历史（LTS 节奏）
- **自主实现**：若我设计——按 LTS 线组织认知：8（2014）→ 11（2018）→ 17（2021）→ 21（2023）——中间版本为特性预览
- **参考实现**（docs 链接 + 发散 + 衔接）：**docs**——Java 版本历史（维基百科链接 `[无本地源码：外部参考]`）；**LTS 主线（发散）**——**Java 8（2014）→ Java 11（2018）→ Java 17（2021）→ Java 21（2023）**（**6 个月节奏 + LTS 每 2 年**——02/28 篇已涉）；**特性节奏（发散）**——新特性先预览（preview）后转正（JEP 流程——02 篇 ZGC 演进实证）；**my-xhs 衔接**——JDK17（LTS——pom 实证）
- **对比取舍**：**LTS（稳定支持）vs 中间版（新特性）**——生产用 LTS vs 尝鲜——my-xhs 用 17（LTS 合理）
- **测试佐证**：docs §Java 版本历史（链接）+ 02 篇（ZGC 版本演进交叉）

### KP-02 Java Native 方向（Project Leyden/AOT 演进）【docs 标题主题发散】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：28 篇（Native）
- **来源**：docs 标题主题（Java Native 应用）+ 架构师发散
- **需求**：理解 **Java 生态自身的 Native 方向**——docs 标题：Java Native 应用（区别于 GraalVM 外部工具的 Java 原生方向）
- **自主实现**：若我设计——Java 官方 AOT/原生方向：Project Leyden（启动/内存优化——条件 AOT 编译）
- **参考实现**（docs 标题 + 发散 + 28/31 衔接）：**方向（发散）**——Java 生态的 Native/AOT 演进：①**Project Leyden**（JDK 官方项目——改善启动时间/内存占用/包体——"conditional AOT"条件编译——对运行期影响最小化的 AOT 探索）②**GraalVM Native（28 篇）**——外部工具链的 AOT（成熟但 closed-world 约束）③**Spring Native/AOT（31 篇）**——框架层适配；**对比（发散）**——**Leyden（官方渐进）vs GraalVM Native（外部彻底）**——Leyden 保持 Java 语义（动态性）兼顾启动优化——**"Java Native"标题指向生态演进方向**
- **对比取舍**：**Leyden（语义保持）vs GraalVM（封闭假设）**——渐进兼容 vs 彻底 AOT——**生态演进的两种路线**
- **测试佐证**：docs 标题（Java Native）+ 28/31 篇衔接

### KP-03 Native 三路径汇总（GraalVM Native/Spring Native/Java Native）【Native 组收官】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：28/31/32 KP
- **来源**：Native 组三篇汇总 + 架构师整合
- **需求**：**Native 组（28,31-32）收官汇总**——三路径的关系与选型
- **自主实现**：若我设计——三路径理解：①GraalVM Native（底层工具——28 篇：AOT 编译/closed-world/Metadata）②Spring Native（框架适配——31 篇：Spring AOT 生成 Metadata/代理编译期化）③Java Native（生态方向——本篇：Leyden 官方演进）
- **参考实现**（三篇汇总 + 发散）：**层次关系（发散）**——**底层**（GraalVM Native Image：任何 Java 应用的 AOT 工具）→ **框架层**（Spring AOT：Spring 应用的 Metadata 生成/适配）→ **生态层**（Project Leyden：Java 官方启动优化）；**选型矩阵（发散）**——**启动敏感 + Spring 生态** → GraalVM Native + Spring AOT（28+31）；**Java 语义保持诉求** → 等 Leyden（32）；**无诉求** → 标准 JVM（my-xhs——28/31 篇现状）；**共同核心（发散）**——**closed-world 假设、Metadata（reflect/resource/proxy——28 篇 KP-05）、编译期化**贯穿三路径
- **对比取舍**：**三路径按"生态位置 × 诉求"选**——工具/框架/官方各有定位——**Native 是"启动敏感场景"的工程决策**
- **测试佐证**：28 篇（GraalVM/Metadata）+ 31 篇（Spring AOT）+ 本篇（Leyden）

### KP-04 现状核对（my-xhs：JDK17 标准栈——Native 组收官汇总）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03
- **来源**：Native 组汇总 + my-xhs 实证
- **需求**：Native 组（28/31/32）的 my-xhs 对照汇总
- **自主实现**：若我设计——核对面：GraalVM（无）、Spring Native（无）、Leyden（JDK 官方——跟随）、触发条件（无）
- **参考实现**（my-xhs 实证 + 发散）：**28 篇（GraalVM）**——OpenJDK17 标准栈 `[现状：无多语言/极致性能诉求]`；**31 篇（Spring Native）**——无 AOT `[现状：无启动敏感场景]`；**本篇（Leyden）**——JDK 官方项目——**跟随 JDK 演进（无需单独决策）**；**触发条件汇总（发散）**——启动敏感（Serverless/边缘/弹性扩容）出现时：①评估 GraalVM Native + Spring AOT（28+31）②或等 Leyden 成熟（32）——**当前均未触发 `[决策待定]`**
- **对比取舍**：**标准 JVM（当前）vs Native（触发时）**——动态自由 vs 启动优化——触发条件驱动的演进
- **测试佐证**：my-xhs（OpenJDK17——28/31 篇实证汇总）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Java 版本历史与 LTS 线 | 规范 | 支撑 | P2 | 🟡 | 有效 | High |
| Java Native 方向（Project Leyden） | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| Native 三路径汇总（收官） | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 现状核对（Native 组汇总） | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：28/31 篇交叉引用 + my-xhs（OpenJDK17 实证）
- **关键实证**（交叉引用）：my-xhs（OpenJDK17——28/31 篇）；31 篇（Spring AOT 源码实证：spring-beans factory/aot AotServices）
- **诚实标注**：docs 为**最短篇（3 行）**——仅 1 标题（Java 版本历史）+ 1 维基百科链接 → 发散重建（头部已声明）；维基百科链接 `[无本地源码：外部参考]`；Project Leyden 为架构师发散（docs 未提——`[待验证：Leyden 官方状态细节]`）；docs 标题"Java Native 应用"正文为空——主题发散
- **关联标注**：28 篇（GraalVM Native/Metadata）；31 篇（Spring Native/AOT）；02 篇（ZGC 版本演进——LTS 节奏）；33 篇（现代 Java 发展与变化——本篇衔接）

---

## 五、本节小结（三层次视角）

**需求**：Java Native 方向与 Native 组收官——Java 版本历史、Leyden 方向、三路径汇总（docs 仅 1 标题+1 链接）。

**自主实现核心**：若我设计——①LTS 线认知（8/11/17/21）②Native 三路径分层（GraalVM 工具 → Spring AOT 框架 → Leyden 生态）③触发条件驱动选型（启动敏感才 Native）。

**参考实现**：docs（1 标题+1 链接照录）+ 发散（Leyden/三路径）+ 28/31 篇交叉引用。**docs 照录 + 发散标注**。

**对比取舍**：知识本体是"**Java Native 方向与 Native 组收官**"——Leyden（官方渐进）vs GraalVM（外部彻底）、三路径层次（工具/框架/生态）、触发条件；my-xhs 标准 JVM 栈（无触发场景——决策待定）；**Native 组（28,31-32）收官**。

**待验证汇总**：
- Project Leyden 官方状态细节（发散——JDK 演进观察项）
- my-xhs Native 触发条件（Serverless/启动敏感——未出现）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| Java 版本（LTS） | ✅ JDK17（LTS——pom 实证） | 无（LTS 合理；21 迁移为演进项） |
| GraalVM Native（28 篇） | ❌ 未用（标准 JVM 栈） | 现状说明：无多语言/极致性能诉求 |
| Spring Native/AOT（31 篇） | ❌ 未用 | 现状说明：无启动敏感场景 |
| Java Native（Leyden） | ⚠️ 跟随 JDK 演进（无单独决策） | 现状说明：Leyden 成熟后评估 |

### 差距清单（Native 组汇总）

1. **P3**：Native 触发条件监控（Serverless/启动敏感场景出现时——28/31/32 三路径评估）
2. **P3**：JDK21 迁移评估（LTS 更新——现有 17 合理）

**结论**：32 篇——my-xhs 全部 Native 路径未采用（现状说明：无触发诉求）；**Native 组（28,31-32）收官**——三路径认知闭环（工具/框架/生态）。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 3 行最短篇（1 标题+1 链接）；知识本体 = 发散 + 28/31 篇交叉；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：Java Native 与 Native 组的完整认知该讲什么

docs 只有 1 个链接标题。完整还该包含：

1. **"Native 是场景驱动的工程决策，不是趋势跟风"**（发散 + 28/31 篇汇总）：三路径（GraalVM 工具/Spring AOT 框架/Leyden 生态）都有**触发条件**（启动敏感/Serverless/边缘）——**无诉求上 Native = 复杂度白增**（my-xhs 现状合理）
2. **"Leyden 是 Java 官方的渐进答案"**（发散）：Project Leyden（条件 AOT）**保持 Java 语义**（动态性）兼顾启动——对比 GraalVM 的 closed-world 约束——**生态演进两条路线：兼容渐进 vs 彻底重构**
3. **"三路径的共同核心"**（发散）：**closed-world 假设 + Metadata（reflect/resource/proxy——28 篇 KP-05）+ 编译期化**——无论哪条路径，**"动态性编译期化"是 Native 的本质挑战**（31 篇 AOT）
4. **"LTS 节奏是 Java 生态的锚"**（docs 链接 + 发散）：6 个月 + LTS 每 2 年（8/11/17/21）——**特性演进（02 篇 ZGC）与生产选型（LTS）的双轨**——my-xhs 用 17 符合节奏
5. **"Native 的收益面与放弃面"**（发散）：启动毫秒级（弹性/Serverless）vs 构建复杂（AOT/Metadata）+ 运行期吞吐可能不如 JIT 稳态（02 篇）——**收益面窄（短生命周期场景）**

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| GraalVM Native vs Spring AOT vs Leyden | 工具彻底 vs 框架适配 vs 官方渐进（三路径定位） |
| LTS vs 中间版 | 稳定支持 vs 新特性（生产用 LTS） |
| Native vs 标准 JVM | 启动/内存 vs 动态自由/构建复杂度 |
| Leyden（语义保持）vs GraalVM（封闭） | 兼容渐进 vs 彻底 AOT |

### 常见坑/反模式

1. **Native 跟风**：无启动敏感诉求硬上——复杂度白增（触发条件判据）
2. **三路径混用**：GraalVM + Spring AOT 配套使用（28+31）——不是二选一
3. **忽略 Leyden 演进**：生态官方方向——跟随 JDK（无单独决策）
4. **LTS 不更新**：长期停留旧 LTS——按节奏迁移（8→11→17→21）

### 生态位置

- **stage-3 教学主线**：Native 组（28,31-32）**收官**——28 GraalVM 基础 → 31 Spring Native → **32 Java Native（本篇）**——**生态 → 框架 → 官方方向三路径闭环**；33 转现代 Java 发展与变化（收尾组）
- **前后篇衔接**：28 篇（GraalVM/Metadata）→ 31 篇（Spring AOT）→ 本篇（Leyden/汇总）；02 篇（JIT/LTS 节奏）；33 篇（现代 Java 发展——收尾衔接）
- **与源码提取的关系**：28/31 篇交叉引用；my-xhs（OpenJDK17）实证

**架构师视角结论**：本篇为 **docs 最短篇（3 行）发散重建**——Java 版本历史（LTS 线）、Java Native 方向（Project Leyden 渐进路线）、**Native 三路径汇总**（GraalVM 工具/Spring AOT 框架/Leyden 生态——触发条件驱动的选型矩阵）——知识本体是"**Java Native 方向与 Native 组收官**"；my-xhs 全部路径未采用（无触发诉求——现状合理）；**Native 组（28,31-32）收官**，下篇转 33（现代 Java 发展与变化——收尾）。
