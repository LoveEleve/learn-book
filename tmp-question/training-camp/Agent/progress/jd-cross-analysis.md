# JD 交叉分析:10 个岗位(DS/Kimi/GLM/MiniMax/字节/腾讯×2/美团×3)

> 生成日期:2026-08-14(首版),迭代 7 次
> 输入:jd/ 目录全部 10 份 JD
> 用途:决定 Agent 源码分析的方法论权重分配与面试准备优先级

## 一句话定位对比

| | DeepSeek | Kimi | GLM | MiniMax | 字节方舟 | 腾讯A(核心) | 腾讯B(可观测) | 美团Tabbit | 美团平台 | 美团框架 |
|---|---|---|---|---|---|---|---|---|---|---|
| 岗位性质 | 研究前沿 | 工程产品化 | 框架+评测 | 基础设施/安全 | 平台基建 | 业务核心系统 | 可观测/评测 | Harness 全栈 | 通用 Agent 平台 | **Agent 框架核心组件** |
| 考核焦点 | 科研品味、idea | 工程、架构、闭环 | 评测方法论 | 系统抽象+隔离 | 运行时/执行引擎 | 规划/记忆/RAG | tracing+eval | 效果/Trace 驱动 | 架构设计+业务落地 | **Planner/Executor/Evaluator + 范式** |
| 语言偏好 | 未指定 | **Go** | Py+Go/Rust/TS | 未指定 | Py/Go/Rust/Java | 未指定 | 未指定 | Python/Java/Go | Python/Go/Java | **Python/Java/Go** |
| 关键差异 | 模型侧深度 | 后端工程广度 | 评测方法论 | 沙箱/隔离/审计 | 通用运行时+范式 | 业务 Agent 系统 | 可观测性+质量 | 自迭代/经验学习 | 编排框架+RAG+安全 | **ReAct/Plan-and-Execute + 生产化** |

## 关键词雷达合并(去重后按权重)

### Tier 0:两家都考的核心(面试必答)
1. **Agent Loop / Agent 内核** — DS:任职#7 显式点名;Kimi:职责#1 内核框架
2. **Tool Use / 工具生态** — DS:#7 点名;Kimi:职责#1 工具生态 + 要求#2 真实开发经验
3. **Context Engineering / 上下文管理** — DS:核心使命#1;Kimi:效果调优#3 隐含
4. **MCP** — DS:#7 点名;Kimi:要求#2 显式点名
5. **Skills** — DS:#7 点名;Kimi:要求#2 显式点名
6. **Memory / 长期记忆** — DS:使命#1 显式;Kimi:效果#3 隐含

### Tier 1:DeepSeek 独有(研究向,偏模型侧)
7. **KV Cache** — DS 独有点名,需模型侧知识(prefix cache、长上下文)
8. **Reasoning / Planning** — DS:#7 点名
9. **Subagent / Multi-Agent** — DS:使命#1
10. **自进化 Agent** — DS:使命#1
11. **超长程任务(Long-horizon)** — DS:使命#1
12. **评测基准/数据标注** — DS:研究职责#3
13. **Prompt Engineering / Harness Engineering** — DS:#7 点名,Harness 是岗位标题

### Tier 1:Kimi 独有(工程向,偏系统侧)
14. **Sandbox(沙箱/隔离)** — Kimi:要求#2 显式点名
15. **任务执行可控/可查/可复现** — Kimi:职责#2(可观测性、trace)
16. **后端基础设施** — Kimi:要求#3(Go、微服务、gRPC、OpenTelemetry)
17. **存储组件** — Kimi:要求#4(PostgreSQL/MySQL/Redis/ES/S3/HBase)
18. **DevOps / CI/CD** — Kimi:职责#5 + 要求#5
19. **用户体验 / 开发者体验** — Kimi:要求#1,#3;DS 工程方向#4

### 共同隐含项
20. **模型与 Harness 共同进化** — DS 使命#2;Kimi:要求#1 效果关注
21. **用户反馈 → 数据 → 迭代** — DS 研究职责#4;Kimi:职责#3

### Tier 1:GLM 独有(评测向,偏框架×数据)
22. **评测体系与基准构建** — GLM:职责#2(真实任务/长程多轮/完整工具链)
23. **自动化评测平台** — GLM:职责#3(多 Agent 并行、环境隔离、可观测、可视化)
24. **失败模式识别算法** — GLM:职责#4(**指令偏移、上下文遗忘、测试投机**)+ 缺陷诊断包/回归用例
25. **主流 Code Agent 对标** — GLM:职责#2 点名 Claude Code、Roo Code、Cline —— 我们的 4 项目正是这族生态的开源样本
26. **代码理解/任务规划** — GLM:职责#1(与 DS 的 Planning 对应)
27. **多轮交互** — GLM:职责#1(长程对话,与 DS 超长程任务对应)

### 三家共同核心(Tier 0 扩充版)
28. **评测/数据/基准** — DS 研究职责#3 + Kimi 职责#3 + GLM 职责#2/3/4 —— **三家全中,升级为 Tier 0**
29. **代码理解/任务规划** — DS Planning + GLM 职责#1 + Kimi 效果调优

### Tier 1:MiniMax 独有(基础设施/安全向)
30. **Sandbox / 执行环境抽象** — MiniMax:职责#1/2(浏览器/容器/K8s/VM/物理机统一抽象)
31. **隔离与安全** — MiniMax:职责#3/4(容器化/虚拟化/进程隔离/文件访问控制/网络策略/权限边界/凭证/不可信代码/逃逸防护)
32. **状态快照/回放/审计** — MiniMax:职责#5/6(确定性回放、差异分析、审计追踪、执行轨迹)
33. **资源编排/多租户** — MiniMax:职责#7(队列调度、环境复用、弹性伸缩、故障恢复)
34. **可观测系统** — MiniMax:职责#6(工具调用/资源占用/环境变更/权限变更/异常现场)
35. **统一抽象能力** — MiniMax:要求#4(异构执行环境 → 稳定可运维可审计的工程模块)

### Tier 1:字节独有(平台/运行时向)
36. **Agent 运行时与执行引擎** — 字节:职责#2(高可靠、可扩展、执行效率/安全/稳定性)
37. **通用 Agent 基础设施** — 字节:职责#1(面向复杂任务的通用基建)
38. **平台产品化** — 字节:职责#3(开发者效率、业务场景落地)
39. **Agent 范式创新** — 字节:职责#4(新范式、引领行业)
40. **Self-involve 等关键机制** — 字节:要求#2(工具调用/上下文管理/长程执行)

### Tier 1:腾讯独有(业务核心 + 可观测双岗)
41. **Durable Execution 长任务执行框架** — 腾讯A:职责#2(显式点名 Durable Execution)
42. **长期记忆 + RAG + 意图识别** — 腾讯A:职责#1(业务场景 Agent 核心系统)
43. **Agent Observability / tracing** — 腾讯B:职责#1(执行全链路 tracing & observability)
44. **eval pipeline / A/B testing / regression detection** — 腾讯B:职责#2
45. **Agent debugging 工具 / SBS 评测 / 数据管道** — 腾讯B:职责#3/4(标注、SBS、数据管道)
46. **failure mode 体感** — 腾讯B:要求#1(agentic coding 的能力边界和 failure mode)
47. **模型侧深度** — 腾讯A:要求#1(Transformer/LLM 原理)+ 职责#5(推理加速/微调/RL)

### Tier 1:美团框架独有(范式/生产化向)
52. **Planner / Executor / Evaluator 核心组件** — 美团框架:职责#3(显式点名三组件)
53. **ReAct / Plan-and-Execute 范式** — 美团框架:要求#4(显式点名,与 LangGraph/AutoGen/CrewAI 对比)
54. **LLM 集成与推理链路优化** — 美团框架:职责#2
55. **Prompt 版本管理** — 美团框架:要求#6(生产级工程实践)
56. **向量检索/知识库构建(RAG)** — 美团框架:要求#5

### 共同核心(Tier 0 扩充版,计数 = 多少家点名)
28. **评测/数据/基准** — DS#3 + Kimi#3 + GLM#2/3/4 + 腾讯B#2 + 字节(隐) + 美团T#3 + 美团P(隐) + 美团F#6(评估体系) = **8/10**
29. **可观测性/可控可查可复现** — Kimi#2 + MiniMax#6 + GLM#3 + 腾讯B#1 + 字节#3(隐) + 美团T#3(Trace) + 美团F#6(Trace/Log) = **7/10**
30. **Sandbox/安全/隔离** — Kimi#2 + MiniMax#3/4 + 字节#3 + 腾讯A#2 + 美团T(优先#3) + 美团P#2(Prompt 注入/权限/输出审核) = **6/10**
31. **Memory/长期记忆** — DS#1 + GLM#1 + 腾讯A#1 + 美团P#1 + 美团F#1(记忆管理) = **5/10**
32. **Planning/任务规划** — DS + GLM#1 + 腾讯A#1 + 字节#2 + 美团P#1 + 美团F#1 = **6/10**
33. **MCP** — DS#7 + Kimi#2 + GLM#2 + 腾讯A#2 + 美团T(优先#3) + 美团F#1(优先#1) = **6/10**
34. **Skills** — DS#7 + Kimi#2 + GLM#1 + 腾讯A#2 + 美团T#2 = **5/10**
35. **多 Agent / Subagent / 编排** — DS#1 + 腾讯A#1 + 美团P#3 + 美团F#1(多 Agent 协同) = **4/10**
36. **评测失败模式** — GLM#4 + 腾讯B#1 + 美团T#3 = **3/10 且权重极高**
37. **KV Cache** — DS 独点,需模型侧补充
38. **Agent 自迭代/自进化** — DS#1 + 美团T#4 + 美团T(优先#1) = **2/10 但美团T 是岗位亮点**
39. **System Prompt / Prompt Engineering** — DS#7 + 腾讯B#3 + 美团T#2 + 美团P#2 + 美团F#2 = **5/10**
40. **RAG/检索** — 腾讯A#1 + 美团P(要求#3) + 美团F#5 = **3/10 且美团F 显式要求向量检索**

## 四项目 × 十岗位匹配矩阵(★ = 该岗位核心样本)

| 项目 | DS | Kimi | GLM | MiniMax | 字节 | 腾讯A | 腾讯B | 美团T | 美团P | 美团F | 合计 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Reasonix | ★★★★★ | ★★★★☆ | ★★★★☆ | ★★★☆☆ | ★★★★☆ | ★★★★☆ | ★★★★☆ | ★★★★★ | ★★★★☆ | ★★★★☆ | 39 |
| OpenCode | ★★★★☆ | ★★★★☆ | ★★★★☆ | ★★★★★ | ★★★★☆ | ★★★★☆ | ★★★★★ | ★★★★☆ | ★★★★☆ | ★★★★☆ | 42 |
| Hermes | ★★★★★ | ★★★☆ | ★★★★☆ | ★★★☆☆ | ★★★☆☆ | ★★★★☆ | ★★★★☆ | ★★★★★ | ★★★★☆ | ★★★★☆ | 38 |
| Pi | ★★★★☆ | ★★★☆ | ★★★☆ | ★★★☆☆ | ★★★☆☆ | ★★★★☆ | ★★★☆☆ | ★★★★☆ | ★★★★☆ | ★★★★☆ | 34 |

## 面试准备优先级(十岗位综合分析)

1. **Context Engineering 必须成为最厚的能力域** — 覆盖 8/10 岗位
2. **可观测性 + 评测成为双核心** — 8/10(评测)与 7/10(可观测);OpenCode 与 Reasonix 是主样本
3. **Sandbox/安全/隔离域升级(含 Prompt 注入/输出审核)** — 6/10;OpenCode(permission)与 Pi(containerization)主样本
4. **Reasonix 必须吃透** — Go 栈(Kimi)+ 缓存(DS)+ benchmarks(GLM)+ checkpoint(腾讯B)+ 长跑恢复(美团T)
5. **Hermes 记忆/自迭代轨升权** — DS + 腾讯A + 美团T(自迭代)+ 美团P(记忆管理)
6. **KV Cache/Transformer 模型侧补充轨** — DS + 腾讯A 显式要求
7. **失败模式三词建立源码映射** — GLM + 腾讯B + 美团T
8. **Durable Execution/任务状态与异常恢复专项** — 腾讯A + 美团T#2 + 美团P#3
9. **Agent 编排/多 Agent 专项** — 美团P#3 + 美团F#1(多 Agent 协同)+ 各项目 subagent 对比
10. **Planner/Executor/Evaluator 三组件 + ReAct/Plan-and-Execute 范式** — 美团F 显式点名:与各项目的实际循环结构(Reasonix executor+planner、OpenCode build/plan)对照
11. **RAG/向量检索/记忆检索专项** — 腾讯A + 美团P + 美团F(Hermes FTS5 检索 = 直接答案)
12. **Prompt 版本管理/生产化工程实践** — 美团F#6:各项目 prompt 管理、配置系统、发布流程对比
10. **RAG/记忆检索专项** — 腾讯A + 美团P(Hermes FTS5 检索 = 直接答案)
9. **Agent 自迭代是美团岗位亮点** — 自迭代/经验学习/Skill 自动生成/Harness 自动优化 = Hermes(技能自改进)+ Pi(self-extensible)主样本

## 对方法论 v2 的影响

- 域清单(00-域发现)必须以 JD Tier 0/1 关键词为"对照验证基准"(对齐原方法论 00 第九步)
- 面试考点地图(09)七轨分线:DS 研究题 / Kimi 工程题 / GLM 评测题 / MiniMax 基建题 / 字节平台题 / 腾讯核心题 / 腾讯可观测题
- 增加"模型侧知识补充"轨:KV Cache/Transformer/RL/推理加速 —— 源码无法覆盖,单独调研
- 评测轨升级为独立方法论章节(10-评测与演进):5/7 岗位要求,失败模式识别需要专门的源码映射轨
- **十岗位画像总结**:
  - DS:懂模型的 Agent 研究员(深度)
  - Kimi:懂工程的 Agent 工程师(广度)
  - GLM:懂数据的 Agent 评测专家(方法论)
  - MiniMax:懂安全的 Agent 基建专家(隔离/抽象)
  - 字节:懂平台的 Agent 运行时专家(引擎/范式)
  - 腾讯A:懂业务的 Agent 系统专家(规划/记忆/RAG)
  - 腾讯B:懂质量的 Agent 可观测专家(tracing/eval/debug)
  - 美团T:懂效果的 Agent Harness 工程师(Trace 驱动/自迭代)
  - 美团P:懂落地的 Agent 平台工程师(编排/RAG/安全)
  - 美团F:懂范式的 Agent 框架工程师(Planner/Executor/Evaluator/ReAct)
