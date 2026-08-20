# Spring 生态源码分析 — 交接文档 (Spring Stage 2)

> **日期**: 2026-08-10
> **状态**: Stage 1 ✅ (Netty 13章36篇 + Tomcat 7域19篇 = 55篇 v5)
> Stage 2 🔄 (Spring 23/84域 = ...→3🟡域→@Import/@EnableXxx)
> **总进度**: 100 篇 v5 大纲

---

## §零 状态速查

| 阶段 | 框架 | 域/章 | 状态 |
|:--:|------|:--:|:--:|
| 1 | Netty | 13 章 | ✅ 36篇 |
| 1 | Tomcat | 7 域 | ✅ 19篇 |
| 2 | Spring Framework | 23/60 | 🔄 |
**当前**: S2-16 @Import/@EnableXxx 完成 — **spring-context 全16域完成！** next = Stage 3 spring-aop。
| 2 | Spring Boot | 0/24 | ⏳ |
| 3-6 | 全部 | 0/277 | ⏳ |

**当前**: S2-14 AOT/Native Image 完成(1篇/2P1+1P2/0缺陷)。next = S2-15 spring-context 最后3🟡域。

---

## §一 方法论（完整内联 — 不需要读外部文件）

### 1.1 知识规划管线（每域必须全部走完）

```
01 逐源提取(Agent 并行) → 01 聚合(P1≥5/P2 2-4/P3 1) → 02 深度分类(🔴🟡🟢 per KP, 必须有"为什么")
  → 03 聚类(教学顺序+每个决定的原因) → v5 大纲(AI 自写, 对照源码 grep 验证行号)
  → completeness-questions(≥3 身份, ≥5 问) → 六层深审
```

**禁止跳过任何步骤**。Agent 只做逐源提取表 — 大纲由 AI 对照源码自写(T-1~T-7 119+锚点 0%错误 vs Ch12 Agent 18%错误)。

### 1.2 v5 大纲格式

```
### N. 机制名 — 一句话描述

场景: [真实场景, 读者知道"为什么要看这个"]

源码路径: file:line + 函数名。精确到行号(grep 验证, 非凭记忆)。
  例如: `DefaultSingletonBeanRegistry.java:210` — getSingleton(beanName, true)

关键设计: [为什么这样实现] — 必须有"为什么"。标注 [模式: XXX]。
  纯框架(Spring): 标注 [模式: Template Method]
  规范参考实现(Tomcat): 标注 → 实现规范: ServletContext (6.0)

数据流: [代码级 trace, 从前一步到后一步的完整方法调用链]
  例如: `getBean("a")`→`getSingleton("a")`→singletonObjects null→`beforeSingletonCreation()`→`createBean()`...
```

**严禁**: 裸行号 `(line 233)` / 伪行号 / 文件总行数代行号(如 `File.java:102` 指向 `}`) / 代码拼接 / 技术清单式 bullet

### 1.3 密度标准

| 级别 | 行数/篇 | 机制/篇 |
|:--:|:--:|:--:|
| 🔴 Deep | 39-69 | 3-6 |
| 🟡 Working | 35-49 | 2-4 |

### 1.4 六层深审（宣称完成前必须逐项执行）

| 层 | 检查项 | 检测方法 |
|:--:|------|------|
| 1 | 源码行号 | `grep -n 'methodName' ActualFile.java` — 每处 `.java:数字` 验证该行有实质内容(非 `}` / 非空) |
| 2 | 内容密度 | `wc -l` 每篇 — 低于 §1.3 阈值→补全 |
| 3 | 语义 | 逐行 Read — 前后矛盾 / 编辑残留 / 内容省略("..."/"等") |
| 4 | 技术声明 | 每个"某某类处理某某机制"→grep 源码确认归属类(注意父类继承) |
| 5 | 算法正确性 | 描述"Step 1→2→3"的流程→逐步骤对照源码顺序 |
| 6 | 机制归属 | 方法/字段的确切类(不凭继承推断) — 特别检查子类 vs 父类 |

### 1.5 全维度方法论检查 (KP层+大纲层+管线层+跨层一致性)

深审必须覆盖四个独立维度:

1. **KP层**: 01逐源提取表 + 01聚合(P1/P2/P3) + 02深度分类(🔴🟡🟢 per KP, 必须有"为什么") + 03聚类(教学顺序+每个决定的原因)
2. **大纲层**: 每机制四要素(场景/源码路径/关键设计/数据流) + 语义 + 桥 + 模式标注
3. **管线层**: completeness-questions(≥3身份/≥5问) + 逐行Read + 源锚交叉
4. **跨层一致性**: KP → 大纲 → 源码 三源验证 — 数值/标识/顺序必须一致

### 1.6 淘汰机制处理（§0.11）

场景/数据流必须以当代生态方式为主(Spring Boot 程序化/注解)。淘汰的 XML 机制只能作"独立部署中..."附带。`grep -rn 'server\.xml\|web\.xml' outlines/` — 所有引用必须为辅助形式。

### 1.7 Spring 域类型判定

Spring 是**纯框架**(非规范参考实现)。需标注设计模式 `[模式: XXX]`。不需要 §0.6 的规范对应维度(Tomcat 专属)。

---

## §二 域产出清单

### Stage 1 (全部完成)

| 框架 | 域 | 篇数 | KP | 深审 | 状态 |
|------|:--:|:--:|:--:|:--:|:--:|
| Netty | 13 章 | 36 | — | v5 源码验证 | ✅ |
| Tomcat T-1 | 容器+Lifecycle | 4 | 232行 | 23/23 0% | ✅ |
| Tomcat T-2 | Connector+Adapter | 4 | 198行 | 44锚点 0% | ✅ |
| Tomcat T-3 | Pipeline+双链 | 4 | — | 18锚点 0% | ✅ |
| Tomcat T-4 | 线程模型 | 2 | — | 18锚点 0% | ✅ |
| Tomcat T-5 | Mapper路由 | 2 | — | 11锚点 0% | ✅ |
| Tomcat T-6 | ClassLoader | 1 | — | 5锚点 | ✅ |
| Tomcat T-7 | SpringBoot集成 | 2 | — | 4锚点 | ✅ |

### Stage 2 — spring-beans 层 (7/7 完成)

| 域 | 主题 | 篇数 | 深审修复 | 状态 |
|:--:|------|:--:|:--:|:--:|
| S1-1 | BeanDefinition | 2 | 2(文件行号代行号: GenericBD:102/RootBD:682) | ✅ |
| S1-2 | BeanFactory | 2 | 2(文件行号: ListableBF:423/ConfigurableBF:436)+14(补行号)+3(术语误用+FactoryBean why) | ✅ |
| S1-3 | Bean 生命周期 | 2 | 3(内容深度: 最后一致性→引用语义/数据流不精确/FactoryBean why缺乏) | ✅ |
| S1-4 | 循环依赖 | 1 | 1(字段名编造) | ✅ |
| S1-5 | DI 注入 | 2 | 2(算法顺序+方法名编造) | ✅ |
| S1-6 | BPP 全景 | 1 | 4(机制归属+方法名) | ✅ |
| S1-7 | FactoryBean | 1 | 1(缓存归属编造) | ✅ |

### Stage 2 — spring-context 层 (14/16 完成)

| 域 | 主题 | 篇数 | 深审修复 | 状态 |
|:--:|------|:--:|:--:|:--:|
| S2-1 | refresh() 12步 | 1 | 4(KP缺P1P2P3+🔴🟡🟢/§1缺数据流/§3缺源码+数据流/L588→L589) | ✅ |
| S2-2 | @Configuration 解析 | 3 | 0(六层深审全绿: 34行号/54-50-53行/8问3身份) | ✅ |
| S2-3 | 事件机制 | 2 | 0(2篇四要素全/46-47行/7问3身份/10行号验证) | ✅ |
| S2-4 | MessageSource 国际化 | 1 | 0(1篇四要素全/45行/6问3身份/5行号验证) | ✅ |
| S2-5 | 父子容器 | 1 | 0(1篇四要素全/46行/5问3身份/7行号验证) | ✅ |
| S2-6 | BFPP 全景 | 1 | 0(1篇四要素全/46行/5问3身份/8行号验证) | ✅ |
| S2-7 | Bean 作用域 | 1 | 0(1篇四要素全/44行/5问3身份/5行号验证+1行号修正) | ✅ |
| S2-8 | AppContext 三大实现 | 1 | 0(1篇四要素全/44行/5问3身份/2行号验证) | ✅ |
| S2-9 | @Conditional | 1 | 0(1篇四要素全/43行/5问/4行号) | ✅ |
| S2-10 | @Async | 1 | 0(1篇四要素全/45行/5问/4行号+行号修正) | ✅ |
| S2-11 | @Scheduled | 1 | 0(1篇四要素全/45行/5问/6行号) | ✅ |
| S2-12 | @Cacheable | 1 | 0(1篇四要素全/44行/5问/6行号+4行号修正) | ✅ |
| S2-13 | @Lazy/@Primary/@DependsOn | 1 | 0(1篇四要素全/45行/5问/6行号+3行号修正) | ✅ |
| S2-14 | AOT/Native Image | 1 | 0(1篇四要素全/33行/5问/4行号+4行号修正) | ✅ |
| S2-15~S2-16 | 3🟡域/@Import | — | — | ⏳ |

---

## §三 缺陷谱系 — 8 种编造类型及检测方法

Spring 8 域深审发现的**所有编造类型**及检测方法:

| # | 类型 | 示例 | 检测 |
|:--:|------|------|------|
| 1 | **文件总行数代行号** | `GenericBeanDefinition.java:102` 指 `}` — parentName 在 L46 | `sed -n '行号p' File.java` → 确认非空/非 `}` |
| 2 | **零行号引用** | getBean 内部流程全无 `.java:数字` | grep `\.java:\d+` → <机制数×2→补行号 |
| 3 | **字段名编造** | `earlyProxyReferences` → 实际 `earlyBeanReferences`(L140) | grep 字段名 → 确认存在 |
| 4 | **方法名编造** | `autowireByName()` → 实际 `autowireResource()`(L576) | grep 方法名 → 确认存在 |
| 5 | **算法顺序编造** | `@Primary→@Priority→beanName` → 实际 `@Primary→beanName→@Qualifier→@Priority`(L2046) | 逐步骤 grep 源码对照顺序 |
| 6 | **术语误用** | "三级保证最终一致性" → 实际是引用语义(非分布式概念) | 人工判断概念准确性 |
| 7 | **机制归属编造** | `invokeInitMethods` 标为 BPP → 实际是 Bean 自身接口回调 | grep 方法所在类 → 确认谁调用 |
| 8 | **缓存归属编造** | `factoryBeanObjectCache` 归 AbstractBeanFactory → 实际在 FactoryBeanRegistrySupport.java:44 | grep 字段 → 确认定义类(非继承) |

---

## §四 执行计划

### 当前 Spring 执行计划 (从 `issue/源码分析执行计划.md`)

- **spring-beans 层** (9域, ✅ 7/9): S1-1~S1-7 ✅ → S1-8 BeanFactoryPostProcessor / S1-9 Bean 作用域
- **spring-context 层** (16域, 🔄 14/16): ...S2-13 ✅ → S2-14 ✅ → S2-15 3🟡域 → ...
- **spring-aop/tx/web/...** (35域 ⏳)
- **Spring Boot** (24域 ⏳)
- **剩余阶段 3-6** (277域 ⏳)

### 下一步

**S2-15 spring-context 最后3🟡域** — @Validated + @DateTimeFormat + 元注解

---

## §五 文件路径

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/
├── issue/
│   ├── HANDOVER.md                ← 全局交接文档
│   ├── 源码分析执行计划.md          ← 32仓库/337域/6阶段完整计划
│   └── Tomcat源码学习范围规划.md
├── netty/                         ← Stage 1 ✅
├── tomcat/                        ← Stage 1 ✅
│   ├── knowledge-planning/ (7域)
│   └── outlines/ (7域/19篇)
└── spring/                        ← Stage 2 🔄
    ├── knowledge-planning/ (12域)
    └── outlines/
        ├── s1-beandefinition/ (2篇)
        ├── s2-beanfactory/ (2篇)
        ├── s3-bean-lifecycle/ (2篇)
        ├── s4-circular-dependency/ (1篇)
        ├── s5-di-injection/ (2篇)
        ├── s6-bpp-panorama/ (1篇)
        ├── s7-factorybean/ (1篇)
        ├── s8-refresh/ (1篇)
        ├── s9-configuration/ (3篇)
        ├── s10-event/ (2篇)
        ├── s11-messagesource/ (1篇)
        └── s12-parent/ (1篇)
```

---

## §六 踩坑速查

| # | 坑 | 检测 |
|:--:|------|------|
| 1 | 文件总行数代行号 | `sed -n '行号p' File.java` — 确认非空/非 `}` |
| 2 | 方法名/字段名编造 | `grep -n 'methodName' File.java` |
| 3 | 算法顺序编造 | 逐步骤 grep 源码对照 |
| 4 | 缓存/机制归属错类 | `grep -n 'fieldName' File.java` — 确认定义在哪个类(非继承) |
| 5 | BFPP/BPP/Bean回调归属混淆 | grep 谁调了这个方法 — BPP=BeanFactory 调, Bean回调=initializeBean 内直接调 |
| 6 | 计数不准("3个默认BPP"实际2个) | 逐个数 — 不凭记忆 |
| 7 | KP层缺 P1/P2/P3 + 🔴🟡🟢 (早期域) | 逐域grep: `^\| P[123]-\d` + `🔴\|🟡\|🟢` |
| 8 | 大纲缺数据流 / 缺源码路径 (早期域) | 逐节对比: `节数` vs `数据流` vs `源码路径` 三数应相等 |
