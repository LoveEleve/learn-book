# S-15 AOT/Native Image — SpringApplicationAotProcessor (Boot AOT 入口)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | SpringApplicationAotProcessor+AotProcessorHook+ContextAotProcessor(spring-framework)+RuntimeHints(spring-core)
> 基线: BOOT-PLAN-v2 S-15 — Boot AOT 管线; 前置: **s21 AOT(机制复用: ContextAotProcessor/RuntimeHints 已在 s21 讲) + S-4** — 展开 Boot 侧入口与触发

---

## §0.8

- 🟡 Working，1篇 — 入口(SpringApplicationAotProcessor extends ContextAotProcessor: 运行 main 启动应用→拦截容器) → 钩子(AotProcessorHook: SpringApplication.withHook 运行 main → AbandonedRunException 拦截 → 拿 GenericApplicationContext 交 AOT 管线) → 管线复用(ContextAotProcessor: 反射/资源/序列化 hints 收集 — **s21 已讲**) → 触发(构建插件 maven/gradle 调用) → 与 s21 边界
- 设计模式: [模式: 钩子拦截]—withHook 运行 main; [模式: 管线复用]—ContextAotProcessor(s21); [模式: 构建期处理]—AOT 在编译期

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SpringApplicationAotProcessor.java:43 | 入口 | **extends ContextAotProcessor**(spring-framework/context/aot — s21 已分析) | High |
| SpringApplicationAotProcessor.java:103,123 | 钩子 | **AotProcessorHook L103**: run L123 — SpringApplication.withHook(this, action) 运行 main → AbandonedRunException 拦截(容器已创建) → 拿 GenericApplicationContext | High |
| ContextAotProcessor(spring-framework) | 管线 | **AOT 管线**: GenericApplicationContext → 收集 RuntimeHints(反射/资源/序列化) → 生成 AOT 编译产物 — **s21 机制复用** | High |
| RuntimeHints(spring-core/aot/hint) | hints | **RuntimeHints**: reflection()/resources()/serialization()/proxies() — s21 已覆盖 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: Boot AOT 入口约 200 行核心 — 知识主线: "运行 main 拿容器 → 交 s21 的 AOT 管线". 1篇 (~43行) 按"入口→钩子→管线复用→触发"展开; **ContextAotProcessor/RuntimeHints 机制全部复用 s21(06 §2.5)**。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | SpringApplicationAotProcessor (运行 main 启动应用) | 🔴 | **为什么🔴**: AOT 需要"真启动"拿容器 — Boot 侧入口 |
| P1-2 | AotProcessorHook (withHook 拦截容器) | 🔴 | **为什么🔴**: 运行 main 又不真正服务请求的机制 |
| P1-3 | 与 s21 边界 (ContextAotProcessor/RuntimeHints 复用) | 🔴 | **为什么🔴**: 机制在 s21, Boot 只提供入口与触发 |
| P2-1 | 触发方式 (maven/gradle 插件在编译期) | 🟡 | **为什么🟡**: AOT 何时跑 |
| P2-2 | Native Image 衔接 (GraalVM) | 🟡 | **为什么🟡**: AOT 产物的消费方 |
| P3-1 | 与 S-4 衔接 (run 流程在 AOT 中被模拟) | 🟢 | **为什么🟢**: main 方法运行方式 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **Boot 入口** (SpringApplicationAotProcessor) | 🔴 | AOT 怎么启动 |
| B | **钩子机制** (AotProcessorHook) | 🔴 | 运行 main 拦截容器 |
| C | **复用与触发** (s21 管线 + 插件) | 🟡 | 分工与执行时机 |

> **Cluster A (§1)**: SpringApplicationAotProcessor(extends ContextAotProcessor)
> **Cluster B (§2)**: AotProcessorHook(SpringApplication.withHook → AbandonedRunException 拦截 → GenericApplicationContext)
> **Cluster C (§3)**: s21 管线复用(ContextAotProcessor/RuntimeHints) + 构建插件触发 + Native Image 衔接

→ 引出 S-16: 诊断 — AOT 之外的运行时: FailureAnalyzer — 启动失败的友好诊断(s21 收束进入运行时层)

(End of file - total 61 lines)
