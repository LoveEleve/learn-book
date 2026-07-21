# 第六个月：前沿层 —— Go 2周 + AI 2周（~84h）

> **目标**：拓宽技术视野——掌握 Go 并发编程（Java 工程师的差异化加分项）、理解大模型/AI 核心原理（面试趋势话题）、巩固数据结构与算法（持续刷题）、了解构建工具（Makefile/CMake/Maven）。
> **核心理念**：前五个月已建立从底层到架构再到性能的完整技术体系，第六个月是"加分项"——Go 和 AI 是两个独立的知识域，各自有充足时间系统学习，不需要像前几个月那样深钻，但要在关键知识点上能讲清楚、能写代码、能展现技术视野。
> **月末产出**：能脱稿讲清楚 Go 的 GMP 调度模型和 GC 三色标记+混合写屏障原理、能写出 Go channel/goroutine 的并发代码、能脱稿讲清楚 Transformer/Self-Attention/MoE 架构和 RAG/RLHF 训练流程、能手写 10 道高频算法题、能解释 Makefile/CMake/Maven 核心概念。

>
> **本规划依据**：[学习大纲.md](../学习大纲.md) 第六阶段（Go 7本 + Python/AI 16本 + 算法 1本 + 工具 1本）+ [4个月面试冲刺路线.md](../4个月面试冲刺路线.md)

---

## 第六个月要攻克什么？

```
第六个月的核心命题：技术广度 + 差异化竞争力

    ┌──────────────────────────────────────────────────────┐
    │                    第六个月：前沿层                     │
    ├─────────────────────┬────────────────────────────────┤
    │  Go 语言（前2周）     │  Python/AI（后2周）             │
    │  第二十一~二十二周   │  第二十三~二十四周              │
    │  ~70h               │  ~70h                         │
    ├─────────────────────┼────────────────────────────────┤
    │  GMP 调度模型         │  Transformer/Self-Attention    │
    │  goroutine/栈管理     │  MoE 架构/DeepSeekMoE         │
    │  channel 底层         │  RAG/RLHF/GRPO               │
    │  GC 三色标记+写屏障    │  LoRA/QLoRA 微调              │
    │  sync.Mutex/Map      │  DeepSeek 创新（MLA/GRPO）     │
    │  逃逸分析/接口         │  MCP 协议/Agent 开发          │
    │  ───────────────────  │  ───────────────────          │
    │  算法巩固（融入Go周）   │  构建工具（碎片时间）          │
    │  每日3-5题保持手感     │  Makefile/CMake/Maven        │
    └─────────────────────┴────────────────────────────────┘
```

**四条主线并行推进**：
- **主线1（Go 并发模型，前2周）**：GMP 调度模型 → goroutine 栈管理 → channel 底层 → sync.Mutex → 内存管理 → GC → 逃逸分析
- **主线2（AI/LLM，后2周）**：Python 基础 → Transformer 架构 → Self-Attention/Multi-Head/FFN → MoE 架构 → RAG/RLHF → DeepSeek 创新
- **主线3（算法巩固，贯穿全月）**：每日 3-5 题保持手感，重点巩固 DP/回溯/图/海量数据
- **主线4（构建工具，碎片时间）**：Makefile 规则与函数 → CMake 核心概念 → Maven 依赖管理与生命周期

**四条主线的交汇点**：
- Go channel CSP 模型 ← → Java BlockingQueue ← → 生产者-消费者模式（算法设计）
- Go GC 三色标记+写屏障 ← → JVM G1 SATB+写屏障 ← → GC 通用设计思想
- Transformer Self-Attention ← → 并行计算思想 ← → Go 并发编程的并行思维
- Makefile 依赖图 ← → 拓扑排序 ← → Maven 依赖仲裁（最短路径优先）

---

## 四周内容总览（与学习大纲完全对齐）

| 周次 | 主题 | 核心产出 | 目录 | 对应学习大纲 |
|------|------|---------|------|------------|
| **第21周** | Go 语言核心能力（一） | 能讲清楚 GMP 调度、GC 三色标记+混合写屏障、channel 底层实现；能手写 Go 并发代码 | [第二十一周/](./第二十一周/) | 6.1 Go 语言（书79/80/81 核心章节） |
| **第22周** | Go 语言核心能力（二）+ 算法巩固 | 掌握 RWMutex/WaitGroup/Cond/Once/sync.Map/Pool/Context/Atomic；理解布隆过滤器/一致性哈希/跳表；能用 pprof 做性能分析 | [第二十二周/](./第二十二周/) | 6.1 Go 语言（书79/80/81/82 高级章节）+ 算法巩固 |
| **第23周** | Python 基础 + AI 核心（一） | 掌握 Python 基础（数据模型/NumPy/Pandas）；能讲清楚 Transformer/Self-Attention/Multi-Head/MoE 架构 | [第二十三周/](./第二十三周/) | 6.2 Python/AI（书134/135/136 核心章节） |
| **第24周** | AI 前沿（二）+ 综合复习 | 能讲清楚 RAG/RLHF/GRPO/DeepSeek 创新；理解 MCP 协议；算法巩固；构建工具核心概念 | [第二十四周/](./第二十四周/) | 6.2 AI（书137/144/145）+ 6.3 算法 + 6.4 工具（书150） |

---

## 完整书籍章节映射（来自学习大纲.md）

### 第21周：Go 语言核心能力（一）

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 6.1 Go 语言 | **书79《深入理解Go并发编程》** | 第1-2章（调度器+Mutex）、第11-13章（channel+内存模型）、第14章（Semaphore）、第18章（限流）、第20章（并发模式） | GMP 调度模型、Mutex 自旋→信号量演进、channel hchan 结构（环形队列/等待队列）、Go 内存模型（happens before）、信号量原理、令牌桶限流、并发模式（半异步半同步/反应器） |
| 6.1 Go 语言 | **书80《深度探索Go语言》** | 第3章（函数/defer/panic）、第5章（接口 eface/iface/反射）、第6章（GMP 调度循环/抢占/netpoller）、第8章（内存管理/GC）、第9章（栈管理） | 逃逸分析、defer/panic/recover 实现、interface 底层（eface/iface）、GMP 调度器初始化/G 创建退出/调度循环/抢占式调度/timer/netpoller/监控线程、TCMalloc/mspan/mcache/mcentral/mheap、三色标记+混合写屏障、连续栈/栈扩容收缩 |
| 6.1 Go 语言 | **书81《Go底层原理与工程化实践》** | 第2-5章（GMP+并发+GC）、第11章（pprof/Trace/dlv） | GMP 调度模型+调度器触发时机、CSP 并发模型+锁同步、GC 三色标记与写屏障+GC 调优、pprof 性能分析/Trace 追踪/dlv 调试 |

**多书交叉印证**（学习大纲推荐）：
- GMP 调度模型：书80 第6章（调度器初始化/调度循环/抢占/netpoller）+ 书79 第1章（调度器概念视角）+ 书81 第2-3章（工程实践视角）
- Go 内存管理+GC：书80 第8章（堆内存+TCMalloc/span/arena+三色标记+混合写屏障）+ 书81 第5章（GC 原理与调优+实战案例）+ 书79 第13章（Go 内存模型视角）
- channel 底层：书79 第11-12章（channel 使用/实现/陷阱/反射操作）+ 书80 第6章（netpoller 与 channel 关联）+ 书81 第4章（CSP 并发模式）

### 第22周：Go 语言核心能力（二）+ 算法巩固

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 6.1 Go 语言 | **Go 并发原语扩展** | RWMutex/WaitGroup/Cond/Once/sync.Map/Pool/Context/Atomic | 读写锁写优先策略、WaitGroup 任务编排、Cond 条件变量 Broadcast vs Signal、Once 双重检查锁定、sync.Map dirty/read 双 map 设计、Pool victim cache 机制、Context 超时取消传播、atomic CAS 循环 |
| 6.1 Go 语言 | **高频算法概念** | 布隆过滤器/一致性哈希/跳表 | 多哈希+位数组原理与误判率、哈希环与虚拟节点、跳表层级索引与 Redis ZSet 实现 |
| 6.1 Go 语言 | **书80《深度探索Go语言》** | 第5章（接口/反射深入） | 接口+反射：书80 第5章（空接口/非空接口/类型断言/反射）+ 书82 第13-14章（接口编程哲学+反射三定律） |
| 算法巩固 | **LeetCode Hot 100** | DP/回溯/图/海量数据 重点巩固 | 每日 3-5 题，保持第五个月冲刺的手感 |

### 第23周：Python 基础 + AI 核心（一）

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 6.2.1 Python | **Python 基础速览** | 数据模型（list/dict/tuple/set）/函数（lambda/装饰器/生成器）/常用库（NumPy/Pandas） | 列表推导、装饰器原理、生成器惰性计算、NumPy ndarray 广播、Pandas DataFrame 操作、Java vs Python 语法对比 |
| 6.2.1 Python | **书134《流畅的Python》** | 第1-3章（数据模型/序列/字典）、第7-9章（函数/装饰器）、第17-21章（迭代器/生成器/并发/异步） | Python 数据模型（魔术方法）、序列协议、字典和集合实现、函数一等对象、装饰器与闭包、迭代器/生成器/async/await |
| 6.2.2 大模型原理 | **书135《从零构建大模型》** / **书135b《从零开始写大模型》** | 第3-11章（Transformer 架构核心） | Tokenization 分词、位置编码、Self-Attention/QKV、Multi-Head Attention、FFN、残差连接+LayerNorm、训练/推理差异 |
| 6.2.3 DeepSeek | **书136《DeepSeek源码深度解析》** | 第1-3章（概述+MoE） | DeepSeek 发展历程、MoE 原理（门控网络/负载均衡）、DeepSeek-V3 架构 |

### 第24周：AI 前沿（二）+ 综合复习

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 6.2.3 DeepSeek | **书137《DeepSeek硬核技术解读》** | 第6-8章（架构+基础设施+训练） | MLA（多头潜在注意力）、DeepSeekMoE 优化、GRPO 强化学习、混合精度/DualPipe/分块量化 |
| 6.2.4 AI工程化 | **书144《一本书讲透MCP》** | 第2-4章（MCP协议+开发） | MCP 协议原理、MCP 应用开发、Agent 与工具调用 |
| 6.2.5 AI面试 | **书145《大模型工程师面试》** | 第4-7章（Transformer/MoE/RAG/RLHF） | Transformer 原理、LoRA/QLoRA 微调、RLHF 训练流程、MoE 架构、RAG 检索增强 |
| 6.3 算法 | **书149《数据结构与算法之美》** | 第1-11章（全章速览） | 复杂度分析、排序/查找、哈希表/树/堆、图/字符串匹配、贪心/分治/回溯/DP |
| 6.4 工具 | **书150《Makefile手册》** | 第1-8章（核心规则+函数） | Makefile 自动变量/模式规则/内置函数、CMake 核心概念、Maven 依赖管理与生命周期 |

**多书交叉印证**：
- Transformer 架构：书135b 第6-11章（从零实现视角）+ 书135 英文原版（理论基础视角）+ 书145 第4章（面试视角）
- MoE 架构：书136 第3章（DeepSeek 源码视角）+ 书137 第6章（硬核技术视角）+ 书145 第6章（面试高频问题）
- DeepSeek 创新：书137 第6-8章（MLA/MoE/GRPO/基础设施全视角）+ 书138（图解视角）+ 书136 第4章（V3 架构视角）

---

## 必须掌握的面试话题（第六月）

### Go 并发（面试加分项 —— Java 工程师的差异化竞争力）

1. **GMP 调度模型**：G（goroutine）、M（machine，OS 线程）、P（processor，逻辑处理器）三者关系？G 的生命周期？work stealing 机制？hand off 机制？——与 Java 线程池的对比：Go 的 goroutine 是用户态轻量级线程，初始栈仅 2KB，Java 线程是内核线程，默认栈 1MB

2. **goroutine 栈管理**：连续栈 vs 分段栈？栈扩容触发条件（stackguard0）？栈扩容的大小（2x）？栈收缩的时机（GC 时）？栈拷贝的过程？——与 Java 虚拟机栈的对比

3. **channel 底层实现**：hchan 结构体包含哪些字段？有缓冲 channel 的环形队列如何工作？无缓冲 channel 的同步机制？send/recv 在编译后变成什么？select 的运行时实现？

4. **sync.Mutex 实现**：Go 的 Mutex 三种模式（正常模式/饥饿模式）？自旋条件？从自旋到信号量的降级路径？——与 Java synchronized 锁升级的对比

5. **Go GC 三色标记+混合写屏障**：GC 四个阶段？三色标记（白/灰/黑）的含义？混合写屏障（Dijkstra 插入屏障+Yuasa 删除屏障）如何保证不漏标记？——与 JVM G1 的对比：G1 用 SATB+pre-write barrier，Go 用混合写屏障

6. **逃逸分析**：什么情况下变量会逃逸到堆上？`go build -gcflags="-m"` 查看逃逸分析结果？——与 Java 逃逸分析的对比

7. **interface 底层**：eface（空接口）和 iface（非空接口）的结构区别？interface 装箱的代价？

### AI/LLM（面试趋势话题 —— 展现技术视野）

8. **Transformer 架构**：Self-Attention 的计算公式（Q·K^T/√dk·V）？为什么除以 √dk？Multi-Head Attention 为什么有效？Position Encoding 的种类？

9. **MoE（混合专家）架构**：MoE 的基本思想（多个专家网络+门控路由）？Top-K 路由？DeepSeekMoE 的创新点（细粒度专家+共享专家）？

10. **RLHF 训练流程**：RLHF 三阶段（SFT→RM→PPO）？PPO 的目标函数？DPO 如何绕过显式训练 RM？GRPO 的创新点？

11. **RAG（检索增强生成）**：RAG 的基本流程（文档分块→嵌入向量→向量数据库检索→上下文注入→LLM 生成）？RAG 解决了什么问题？

12. **Fine-tuning 微调**：全量微调 vs PEFT？LoRA 的原理？QLoRA 的优化？什么时候需要微调，什么时候用 RAG 就够了？

13. **DeepSeek 核心创新**：MLA（多头潜在注意力）？DeepSeekMoE？GRPO？DualPipe？混合精度训练？

### 数据结构与算法（面试必考 —— 基础能力检验）

14. **复杂度分析**：时间复杂度（O(1)/O(logn)/O(n)/O(nlogn)/O(n²)/O(2ⁿ)）？空间复杂度？
15. **排序算法对比**：快排/归并/堆排序的复杂度、稳定性对比？Java `Arrays.sort()` 和 Go `sort.Slice()` 用的什么排序算法？
16. **哈希表冲突解决**：开放寻址法 vs 链表法？Java HashMap 和 Go map 分别用的哪种？
17. **B+树 vs 红黑树 vs 跳表**：三者的数据结构差异和适用场景？

### 构建工具（工程能力补充）

18. **Makefile**：自动变量（$@/$</$^）含义？模式规则的作用？内置函数的用法？
19. **CMake**：`add_executable()`/`add_library()`/`target_link_libraries()` 的基本用法？PUBLIC/PRIVATE/INTERFACE 的传递规则？
20. **Maven**：依赖仲裁规则（最短路径优先）？Maven 生命周期？

---

## 面试角色对照：Java 面试中的加分项策略

```
┌─────────────────────────────────────────────────────────────────┐
│             Java 面试中如何运用第六个月的知识？                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  【Go 并发】→ "我对并发编程有跨语言的理解"                          │
│  ─ 面试官问：你了解 Go 的 goroutine 吗？                           │
│  ─ 你回答：Go 的 GMP 调度模型与 Java 线程模型的设计哲学不同——        │
│    Go 用 M:N 调度（用户态 goroutine 映射到内核线程），JVM 用 1:1    │
│    线程映射。Go 的 channel CSP 模型与 Java BlockingQueue 类似，     │
│    但 channel 是语言内置的。Go 的 GC 三色标记+混合写屏障与           │
│    G1 的 SATB+pre-write barrier 都是为了解决并发标记的漏标问题。     │
│  → 展现：跨语言对比能力 + 底层原理理解深度                           │
│                                                                  │
│  【AI/LLM】→ "我有技术前沿视野"                                    │
│  ─ 面试官问：你对大模型有了解吗？                                    │
│  ─ 你回答：我理解 Transformer 架构的核心是 Self-Attention 机制，     │
│    它解决了 RNN 的长距离依赖问题。MoE 架构通过稀疏激活降低了推理      │
│    成本。如果我们要在系统中集成 LLM，RAG 是当前最成熟的方案——         │
│    把私有知识向量化存入向量数据库，检索相关上下文后注入 prompt。       │
│  → 展现：技术广度 + 工程化思维 + 实际落地能力                        │
│                                                                  │
│  【算法】→ "我的基本功扎实"                                        │
│  ─ 面试官问：手写一个 LRU 缓存                                      │
│  ─ 你回答：（直接写出哈希表+双向链表实现，解释 O(1) 复杂度来源）       │
│  → 展现：手写代码能力 + 数据结构选型能力                              │
│                                                                  │
│  【构建工具】→ "我有工程化意识"                                     │
│  ─ 面试官问：Maven 依赖冲突怎么解决？                                │
│  ─ 你回答：先用 mvn dependency:tree 查看依赖树，找出冲突的 Jar。      │
│    依赖仲裁遵循最短路径优先原则。如果传递依赖版本不对，用              │
│    <dependencyManagement> 统一版本或 <exclusions> 排除冲突依赖。     │
│  → 展现：工程实践能力 + 问题排查能力                                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 关键工具掌握

| 工具 | 用途 | 第几周 | 对应学习大纲建议 |
|------|------|--------|----------------|
| Go 工具链（`go build`/`go test`/`go tool compile`） | 编译、测试、查看逃逸分析（`-gcflags="-m"`） | 第21-22周 | 书81 第11章 + 书80 第3章 |
| pprof（`go tool pprof`） | CPU/内存/goroutine 性能分析 | 第22周 | 书81 第11章 |
| dlv（Delve） | Go 调试器，打断点、查看 goroutine 栈、GMP 状态 | 第21周 | 书81 第11章 |
| Python（`pip`/`venv`/`jupyter`） | Python 开发环境、LLM API 调用 | 第23-24周 | 书134 附录 |
| Jupyter Notebook | 交互式 Python 编程、模型实验 | 第23周 | 书135b 第15章 |
| LeetCode | 算法刷题巩固，每日 3-5 题 | 全月 | 书149 + LeetCode Hot 100 |
| Make / CMake / Maven | 构建工具实操，理解核心概念 | 第24周（碎片时间） | 书150 第1-8章 |

---

## 学习方法提醒（来自学习大纲）

1. **先骨架后细节**：Go 先理解 GMP 调度模型的三个角色和基本流程，再深入具体实现细节；AI 先理解 Transformer 的 Self-Attention 做了什么，再看 Multi-Head 和 FFN 的具体计算
2. **多书交叉印证**：同一主题参考 2-3 本不同作者的书——例如 GMP 调度同时看书80（深度视角）、书79（并发视角）、书81（工程视角）
3. **对比学习法**：Go 的 GC 与 JVM G1 GC 对比、Go channel 与 Java BlockingQueue 对比、Go goroutine 与 Java 线程对比——跨语言对比是面试中的杀手锏
4. **费曼学习法**：学完后用自己话讲一遍，讲不出来 = 没学会——特别是 GMP 调度、三色标记+写屏障、Transformer Self-Attention、MoE 架构
5. **面试导向**：每个主题学完后问自己"面试官会怎么问这个问题"——Go 并发和 AI/LLM 是展现你技术视野和差异化竞争力的关键话题
6. **关联前五个月**：学 Go GC 时回想 JVM G1 GC（第二个月）、学 channel 时回想 Linux pipe/epoll（第一个月）、学 Makefile 依赖图时回想拓扑排序（算法）
7. **不要贪多**：第六个月核心书：书79/80/81/82（Go）、书134/135b/136/137（AI）、书149（算法）、书150（工具）。其余书作为查阅/补充。**Go 和 AI 是加分项，不需要达到前五个月那种深度**
8. **算法保持手感**：每日 3-5 题，重点巩固第五个月冲刺的薄弱环节
9. **Python 快速入门即可**：不需要精通 Python，能看懂代码、能调用 API 就够——重点在 AI 概念理解

---

## 前置知识检查表（进入第六个月需要掌握）

| 知识领域 | 你需要先掌握的内容 | 参考来源 |
|---------|-----------------|---------|
| **Java 并发编程** | 线程模型、synchronized/ReentrantLock、volatile/JMM、线程池/BlockingQueue、AQS | 第二个月 第6-7周 |
| **JVM GC** | G1 Region/RSet/SATB/写屏障、CMS 并发标记、GC 日志解读 | 第二个月 第5-7周 |
| **Linux 进程/线程** | 内核线程 vs 用户线程、CPU 调度（CFS）、上下文切换 | 第一个月 第2-3周 |
| **Linux IO 多路复用** | epoll Reactor 模式、select/poll/epoll 对比 | 第一个月 第3周 |
| **Kafka/Raft** | ISR 机制、Raft 三子问题、日志复制 | 第二个月 第8周 |
| **分布式系统** | CAP 定理、Paxos/Raft、分布式事务 | 第三个月 第11周 |
| **算法基础** | 时间复杂度分析、排序算法、链表/树/哈希表、DP/回溯 | 第五个月 第19周 |
| **Java 基础** | Maven 基本使用（`mvn clean install`）、`pom.xml` 结构 | 日常开发 |
| **性能优化基础** | perf 火焰图、JIT 编译、GC 调优 | 第五个月 第17-18周 |

---

## 与第五个月（优化+冲刺层）的衔接说明

**第五个月聚焦性能优化和面试冲刺，第六个月是技术广度拓展——两者形成"深度+广度"的完整技术画像**：

| 第五个月（深度） | → | 第六个月（广度） | 关联说明 |
|-----------------|---|-----------------|---------|
| JVM GC 调优（G1/ZGC） | → | Go GC 三色标记+写屏障 | 跨语言 GC 设计思想对比：G1 SATB vs Go 混合写屏障 |
| JVM JIT 编译优化（逃逸分析/标量替换） | → | Go 逃逸分析（编译期栈/堆分配） | Go 的逃逸分析在编译期��定，JVM 在运行时 JIT 优化——静态 vs 动态两种思路 |
| eBPF/perf 性能分析 | → | Go pprof/Trace 性能分析 | 跨语言性能分析工具链对比 |
| 算法冲刺（Hot 100） | → | 算法巩固（每日 3-5 题） | 保持手感，重点巩固薄弱环节 |
| 系统设计面试准备 | → | AI/LLM 工程化应用 | 在系统设计中引入 AI 能力（RAG 检索增强、LLM 调用） |

---

## 第六月综合自测（面试模拟）

完成四周学习后，用以下问题做一次完整的面试模拟。每题准备 3 分钟脱稿回答。

### Go 并发（9题）
1. Go 的 GMP 调度模型中，G、M、P 分别代表什么？它们的数量关系是怎样的？work stealing 机制如何工作？（基础）
2. goroutine 的栈初始大小是多少？什么时候触发栈扩容？扩容到多大？栈拷贝的过程是怎样的？（基础）
3. channel 底层 hchan 结构包含哪些字段？有缓冲 channel 的环形队列如何实现？无缓冲 channel 的同步是如何完成的？（基础）
4. sync.Mutex 的正常模式和饥饿模式有什么区别？自旋的条件是什么？从自旋到信号量的降级路径是怎样的？（进阶）
5. Go GC 的三色标记（白/灰/黑）分别代表什么状态？混合写屏障如何保证并发标记不漏标？（进阶）
6. Go 的逃逸分析在什么情况下会让变量逃逸到堆上？如何用 `go build -gcflags="-m"` 查看逃逸分析结果？（基础）
7. Go 的 interface 底层 eface 和 iface 的结构有什么区别？interface 装箱有什么代价？（进阶）
8. sync.Map 的 dirty/read 双 map 设计是如何工作的？什么场景下性能最好？（基础）
9. Context 的 WithTimeout 返回的 cancel 函数为什么必须调用？不调用会有什么后果？（基础）

### AI/LLM（6题）
10. Transformer 的 Self-Attention 计算公式是什么？为什么要除以 √dk？Multi-Head Attention 为什么有效？（基础）
11. MoE（混合专家）架构的基本思想是什么？Top-K 路由如何工作？DeepSeekMoE 相比传统 MoE 有什么创新？（基础）
12. RLHF 训练流程的三个阶段分别是什么？GRPO 算法相比 PPO 有什么改进？（进阶）
13. RAG（检索增强生成）的基本流程是什么？它解决了 LLM 的哪些问题？什么场景下需要 Fine-tuning 而不是 RAG？（基础）
14. LoRA 微调的原理是什么？QLoRA 做了哪些优化？LoRA 的秩（rank）越大越好吗？（进阶）
15. DeepSeek 的核心创新有哪些？（MLA/DeepSeekMoE/GRPO/DualPipe/混合精度）（进阶）

### 数据结构与算法（5题）
16. 快速排序、归并排序、堆排序的时间复杂度、空间复杂度、稳定性分别是什么？Java 和 Go 标准库分别用的什么排序？（基础）
17. 哈希表冲突解决的两种主要方式是什么？Java HashMap 和 Go map 分别用的哪种？装载因子的含义是什么？（基础）
18. B+树、红黑树、跳表的适用场景分别是什么？为什么 MySQL 用 B+树，Redis ZSet 用跳表？（进阶）
19. 布隆过滤器的原理是什么？如何计算误判率？它在哪些场景下使用？（基础）
20. 动态规划的核心要素（最优子结构/无后效性/重叠子问题）分别是什么意思？举例说明。（基础）

### 构建工具（3题）
21. Makefile 的自动变量 $@、$<、$^ 分别代表什么？模式规则 `%.o: %.c` 的作用是什么？（基础）
22. CMake 中 PUBLIC/PRIVATE/INTERFACE 的依赖传递规则是什么？`find_package()` 的作用是什么？（基础）
23. Maven 的依赖仲裁规则是什么？`mvn dependency:tree` 输出的内容如何解读？如何排除冲突依赖？（基础）

### 跨语言对比思考（3题）
24. Go GC 的三色标记+混合写屏障与 JVM G1 的 SATB+pre-write barrier 有什么异同？两者各自为什么选择了不同的写屏障方案？（关联思考）
25. Go goroutine 与 Java 线程（Thread）在设计哲学上有什么根本区别？Go 的 channel CSP 模型与 Java 的 BlockingQueue 在并发编程范式上有何不同？（关联思考）
26. Go的GMP调度和Java的Virtual Thread调度有什么异同？（提示：GMP是M:N用户态调度，Virtual Thread是JVM层面的M:N调度——Platform Thread为M，Virtual Thread为G，ForkJoinPool中的Carrier Thread类似P的角色。两者都实现了"轻量级线程挂载到少量OS线程"的目标，但GMP调度器更成熟，而Virtual Thread是Java 21的新特性。）（关联思考）

### 动手能力（3题）
27. 如何用 Go 的 pprof 分析一个程序的 CPU 和内存热点？`go tool pprof -http=:8080` 的火焰图如何解读？
28. 如何用 Python 调用 OpenAI API 实现一个简单的 RAG 问答系统？涉及哪些关键步骤（文档加载→分块→向量化→检索→生成）？
29. 如何用 Maven 解决一个真实的依赖冲突？`mvn dependency:tree` 找到冲突后，如何用 `<exclusions>` 或 `<dependencyManagement>` 解决？

### 关联思考（2题）
30. 从 Go 的 GMP 调度到 Java 的虚拟线程调度，再到 Linux CFS 调度器，它们在"用户态调度"和"内核态调度"的分工上有什么共同的设计思路？（关联思考）
31. 从 Transformer 的 Self-Attention 到 Go 的 channel 通信，再到 Raft 的日志复制——它们在"信息传递"这个抽象层面上有什么相似之处？（关联思考）

---
