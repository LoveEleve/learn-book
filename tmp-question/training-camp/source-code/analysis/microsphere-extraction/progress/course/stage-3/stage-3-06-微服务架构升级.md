# stage-3 · 第 06 节：第四节：高可用微服务架构升级 — 知识点提取

> 课程：stage-3 三高架构 第 06 节（实操组：容器/服务 06/10）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/06. 第四节：高可用微服务架构升级.md`
> 提取时间：2026-08-12 | 权重：核心（JMH 微基准 + 微服务化改造 + Spring Cloud 调用升级——单体→微服务的承上启下）
> 案例载体：my-xhs（决策 B，2026-08-12）

---

## 一、本节概览

- **技术域**：JMH 微基准（模式/注解/正确性/伪共享）、微服务化改造（按域拆分）、Spring Cloud 服务间调用（Feign）
- **维度**：`[性能优化]`（JMH）+ `[工程问题]`（微服务化）+ `[分布式问题]`（服务调用）
- **核心命题**：**升级前先有"尺子"**——JMH 微基准量化单点性能；再讲"怎么拆"（微服务化改造）与"怎么连"（Spring Cloud 调用）——docs 后两节为**空节**，知识本体在架构师补全 + my-xhs 实证
- **知识点数**：8 个
- **前置**：02 篇 KP-05/07（测试/JIT）、05 篇（容器调优）、stage-1 08/11（容错/负载均衡——微服务基础）

## 前置条件清单
读者需先掌握：
1. **JIT 优化对测量结果的误导**（02 篇 KP-07：死代码消除/常量折叠是编译器行为）
2. **性能指标与测试类型**（02 篇 KP-05/06）
3. **微服务基本概念**（stage-1 03/04 REST + stage-1 11/12 负载均衡）
未达前置者，先补：02/05 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **实例锚定**：my-xhs-benchmark 的 JMH 注解完整实证（@BenchmarkMode/@Warmup/@Fork）；Feign 客户端实证
- **诚实标注**：my-xhs benchmark 是**模拟 payload**（非真实服务调用）——"看起来在做≠真的实现"（03/05 篇教训延续）
- **docs 场景 vs my-xhs**：docs 讲 Shopizer 单体拆微服务；my-xhs **已是微服务**——改造知识点按方法论提取，实例对照现状

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 JMH 定位与架构（预热/优化路径/fork 隔离）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：02 篇 KP-05/07
- **来源**：docs §JMH 简介 + §快速开始 + §官方示例 + 架构师发散
- **需求**：选择微基准工具——**防 JIT 预热/编译优化干扰的精确测量**
- **自主实现**：若我设计——独立 fork 进程 + 预热迭代 + 生成器产生测量代码（防编译器优化掉被测代码）
- **参考实现**（docs + 发散）：**JMH 定位**——Java 工具：nano/micro/milli/macro 四级基准；**负责 JVM 预热和代码优化路径**（docs 明确：使基准尽可能简单）；**架构**（发散）——`jmh-core`（运行 harness）+ `jmh-generator-annprocess`（注解处理器生成测量代码）+ 自包含 benchmarks.jar（`mvn clean install` 后 `java -jar target/benchmarks.jar`）；**依赖**——`jmh-core` + `jmh-generator-annprocess`（docs 1.36，scope=test）`[有效：1.36 为 2023 版，现代 1.37 存在——版本演进非机制]`；**配套**——IDEA 插件（artyushov/idea-jmh-plugin）、**JMH Visualizer**（jmh.morethan.io 可视化，docs §可视化）
- **对比取舍**：**JMH vs 手写计时**——手写 `System.nanoTime` 循环会被 JIT 优化/预热污染；JMH 的 fork+预热+Blackhole 是"测量基建"
- **测试佐证**：docs §JMH 简介/快速开始 + my-xhs `my-xhs-benchmark/pom.xml:17-22`（jmh-core + generator 实证）

### KP-02 基准模式（Mode：Throughput/AverageTime/SampleTime/SingleShotTime）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §基准模式（Mode 枚举全注释）
- **需求**：按度量目标选模式——**吞吐 vs 平均耗时 vs 分布采样 vs 单次（冷启动）**
- **自主实现**：若我设计——默认 Throughput（ops/时间）；测延迟分布用 SampleTime；冷路径用 SingleShotTime
- **参考实现**（docs Mode 枚举注释）：**①Throughput（thrpt）**——单位时间操作数，持续调用到迭代时间到期；**②AverageTime（avgt）**——每操作平均时间（Throughput 的倒数，聚合策略不同）；**③SampleTime（sample）**——随机采样每次调用耗时（可看分布/尾部，自动调采样频率，可能漏采暂停）；**④SingleShotTime（ss）**——单次调用计时（**评估"冷"性能、不隐藏预热、逐次记录进度**；坑：需更多预热/测量迭代、计时器开销对小基准显著）⑤**All**——元模式（内部测试用）
- **对比取舍**：**SampleTime vs AverageTime**——分布信息（P99 可见）vs 平均单值（02 篇"只看均值"坑呼应）
- **测试佐证**：docs §基准模式（Mode 源码注释）+ my-xhs OrderServiceBenchmark `@BenchmarkMode(Mode.Throughput)`

### KP-03 JMH 关键注解组合（@Benchmark/@State/@Warmup/@Measurement/@Fork）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §热身 @Warmup + §状态 @State + my-xhs 实证
- **需求**：掌握基准的"标准骨架"——注解决定测量正确性
- **自主实现**：若我设计——@Benchmark 标方法 + @State 管共享/线程隔离数据 + @Warmup/@Measurement 控制预热与测量 + @Fork 进程隔离
- **参考实现**（docs + my-xhs 实证）：**@Benchmark**——被测方法（方法名无关，可多个）；**@State(Scope.Thread/Benchmark)**——状态对象（Thread=每线程独立实例；Benchmark=全线程共享——docs 示例：共享 vs 非共享测量竞争效果）；**@Warmup**——收集结果前的干运行（docs 原文"warmum"为**笔误**，正确属性 `warmups`）；**@Fork**——value=执行次数（docs 原文：value 控制执行次数、warmup 参数控制干运行次数）；**@BenchmarkMode/@OutputTimeUnit**——模式与单位；**@Measurement**——测量迭代（docs 未展开，发散补全）；**my-xhs 实证（OrderServiceBenchmark.java 完整注解）**——`@BenchmarkMode(Mode.Throughput)` + `@OutputTimeUnit(TimeUnit.SECONDS)` + `@State(Scope.Benchmark)` + `@Warmup(iterations=3, time=1, timeUnit=SECONDS)` + `@Measurement(iterations=5, time=2)` + `@Fork(1)` + `@Benchmark`——**与 docs 教学完全对应的工程骨架**
- **对比取舍**：**Scope.Thread vs Scope.Benchmark**——线程隔离（无竞争测量）vs 共享（真实竞争）——按被测对象语义选
- **测试佐证**：docs §热身/§状态 + my-xhs `my-xhs-benchmark/src/test/java/com/myxhs/benchmark/OrderServiceBenchmark.java`（全注解实证）

### KP-04 微基准正确性（死代码消除/常量折叠/Blackhole）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：02 篇 KP-07（JIT 优化机制——C2 死代码消除/常量折叠的根源）
- **来源**：docs §死代码消除 + §常量折叠（示例全文）
- **需求**：理解**编译器优化会让错误基准"快得离谱"**——测量值必须是"被使用"的
- **自主实现**：若我设计——计算结果必须被消费（返回/Blackhole），输入必须不可预测（非 final/非常量）
- **参考实现**（docs 示例）：**DCE（死代码消除）**——`measureWrong()` 调用 `compute(x)` 不用结果 → C2 判定冗余**整段消除** → 测得"不真实快"；`measureRight()` **返回结果** → JMH 隐式用 Blackhole 消费（防消除）；**常量折叠**——`measureWrong_1/2`（`Math.PI` 常量 / `final` 字段）→ 计算可折叠到循环外；`measureRight()` 用**非 final 实例字段**防折叠（docs 明确："字段别让 IDE 改成 final"——信任编译器不如信任 JMH 规则）
- **对比取舍**：**Blackhole 消费 vs 真实输出**——黑洞零成本消费（防消除）；基准结论："返回结果"是正确性底线
- **测试佐证**：docs §死代码消除/§常量折叠（JMHSample_08/10 全文）+ 02 篇 KP-07（DoEscapeAnalysis 等 C2 优化默认开启——基准干扰的根源）

### KP-05 伪共享（False Sharing / @Contended）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：CPU 缓存行（cache line）、02 篇 KP-08（缓存层级）
- **来源**：docs §未共享（FalseSharing 测试结果 5 组）+ JDK 源码验证
- **需求**：理解多线程下**缓存行竞争**——两个变量同缓存行 → 各自核修改触发整行失效（伪共享）
- **自主实现**：若我设计——热字段用 @Contended 填充隔离缓存行（或 padding/按字段稀疏排布）
- **参考实现**（docs 数据 + 源码验证）：**JMHSample_22_FalseSharing 5 变体**（docs 结果）——baseline（无保护，reader 竞争最重：**reader 784.716 ops/us**，docs 原文 writer 3477.901）/contended（@Contended）/hierarchy（层级）/padded（手动填充）/sparse（稀疏排布）——**hierarchy/padded 的 reader 明显提升（1616.513 vs 784.716，+106%）**，writer 列变化小（3471→3367）；**@Contended 注解**——JDK 内建 `jdk.internal.vm.annotation.Contended`（**JDK11/17 源码实证：`java.base/share/classes/jdk/internal/vm/annotation/Contended.java`**，需 `-XX:-RestrictContended` 解锁（JDK8+ 默认限制）`[待验证：RestrictContended 默认值]`）
- **对比取舍**：**@Contended vs 手写 padding**——注解声明式（+128 字节填充）vs 手动字段排列（无 JDK 内部依赖但有维护成本）
- **测试佐证**：docs §未共享 5 组数据 + JDK11/17 `java.base/.../jdk/internal/vm/annotation/Contended.java`

### KP-06 微服务化改造模式（docs 空节发散 + my-xhs 对照）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：微服务概念
- **来源**：docs §微服务化改造（**空节：仅标题**）+ 架构师发散 + my-xhs 对照
- **需求**：理解单体→微服务的改造模式——**按域拆服务 + 数据拆分 + 调用改造**（docs 场景 Shopizer；my-xhs 已是微服务，对照现状）
- **自主实现**：若我设计——①按业务域切服务边界（高内聚低耦合）②数据随服务拆分（库/表归属）③服务间调用改造（REST/Feign + 注册中心）④逐步灰度切流
- **参考实现**（docs 标题 + 架构师发散 + my-xhs）：**docs 改造对象**——店铺、类目、商品、优惠券、用户、购物车、订单、搜索 8 模块；**改造模式（发散）**——①**域划分**：按业务能力拆（docs 8 模块 = 8 域）②**数据拆分**：每服务独享数据（禁跨服务 join，事件化补充）③**调用改造**：HTTP/Feign + 服务发现（07/08 节 Eureka）④**拆分策略**：先边界清晰域（搜索/优惠券），后核心交易域（订单/支付）——**Strangler Fig 绞杀者模式**；⑤**一致性**：跨域事务转事件（17 节分布式事件）；**my-xhs 对照**——15 服务（user/content/product/order/cart/coupon/search...）正是"docs 8 模块"的现代落地（03 篇 KP-02 架构基线图——交叉引用不重复）
- **对比取舍**：**拆分的收益与代价**——独立扩缩容/故障隔离/团队自治 vs 分布式一致性/调用链复杂/运维面扩大——**docs 第 3 条"评估性能变化"即量化此权衡**
- **测试佐证**：docs §微服务化改造（空节标题）+ my-xhs 模块清单（pom.xml:14-35，03 篇 KP-02 已证）

### KP-07 Spring Cloud 服务间调用架构升级（Feign + 注册中心 + 负载均衡）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-1 03/04（REST）、11/12（负载均衡）
- **来源**：docs §Spring Cloud 架构升级（**空节：仅标题**）+ 架构师发散 + my-xhs Feign 实证
- **需求**：理解"上下游服务间 Spring Cloud 调用"的落地——**Feign 声明式调用 + 注册中心发现 + 负载均衡**
- **自主实现**：若我设计——Feign 接口声明（@FeignClient）→ 注册中心（Nacos）→ 负载均衡（LoadBalancer）→ 容错（Sentinel）
- **参考实现**（docs 标题 + 发散 + my-xhs）：**docs 意图**——上下游服务间以 Spring Cloud 方式调用（替代直连 IP），评估性能变化；**调用链路（发散）**——FeignClient 接口（声明式）→ 注册中心（服务名寻址，07/08 节 Eureka/Nacos）→ 客户端负载均衡（stage-1 11/12）→ 容错（stage-1 08）；**my-xhs 实证**——`my-xhs-order/feign/` 3 个客户端（`CouponFeignClient`/`InventoryFeignClient`/`PaymentFeignClient`——**订单服务依赖优惠券/库存/支付三服务**，全项目 59 个 java 文件引用 OpenFeign（03 篇已证）+ common/loadbalancer/`LeastConnectionsLoadBalancer`（03 篇已证）+ 网关 Nacos 发现（03 篇已证）
- **对比取舍**：**Feign 声明式 vs 手写 HTTP 客户端**——接口契约/负载均衡/容错一体化 vs 直连简单但硬编码——升级的"评估性能变化"正是对比链路开销（多一跳网络 vs 解耦收益）
- **测试佐证**：docs §Spring Cloud 架构升级（空节）+ my-xhs `my-xhs-order/src/main/java/com/myxhs/order/feign/`（3 FeignClient 实证）

### KP-08 改造前后性能评估方法论（"尺子"先行）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：02 篇 KP-01（调优流程）
- **来源**：docs §主要内容第 3 条 + 架构师整合
- **需求**：架构升级不是"拍板就改"——**改造前建基线、改造后对比**（02 篇 6 步循环的架构级应用）
- **自主实现**：若我设计——改造前全量 API 压测基线（JMeter/JFR/JMH 三工具）→ 逐模块改造 → 每步复测对比 → 决策保留/回滚
- **参考实现**（docs 意图 + 整合）：**docs 明确**——"评估架构升级后的各应用 API 的性能指标，对比前后性能变化"；**方法论整合**——02 篇 KP-01 6 步（测量驱动）+ 02 篇 KP-07 三工具（JMeter 压测/JFR 诊断/JMH 微基准）+ 本篇 JMH 骨架——**升级评估 = 三工具在架构层的组合**；**坑**——改造中环境变化（机器/数据量）会污染对比 → 控制变量
- **对比取舍**：**逐模块灰度改造 vs 大爆炸改造**——可控对比/回滚 vs 一步到位快但不可归因
- **测试佐证**：docs §主要内容 + 02 篇 KP-01/05/07 交叉引用

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| JMH 定位与架构 | 性能优化 | 核心 | P1 | 🔴 | 有效 | High |
| 基准模式（Mode 四模式） | 性能优化 | 支撑 | P2 | 🟡 | 有效 | High |
| JMH 关键注解组合 | 性能优化 | 核心 | P1 | 🔴 | 有效 | High |
| 微基准正确性（DCE/常量折叠） | 性能优化 | 核心 | P1 | 🔴 | 时间无关 | High |
| 伪共享（@Contended） | 性能优化 | 支撑 | P2 | 🟡 | 有效 | High |
| 微服务化改造模式 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Spring Cloud 调用升级（Feign） | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| 改造前后性能评估方法论 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（JMH 骨架/Feign）+ JDK11/17（@Contended）
- **关键源码**（本次实证）：
  - `my-xhs-benchmark/pom.xml:17-22`（jmh-core + jmh-generator-annprocess）
  - `my-xhs-benchmark/src/test/java/com/myxhs/benchmark/OrderServiceBenchmark.java`（@BenchmarkMode/@OutputTimeUnit/@State/@Warmup(3×1s)/@Measurement(5×2s)/@Fork(1)/@Benchmark 全注解实证）
  - `my-xhs-order/src/main/java/com/myxhs/order/feign/`（CouponFeignClient/InventoryFeignClient/PaymentFeignClient）
  - JDK11/17 `java.base/share/classes/jdk/internal/vm/annotation/Contended.java`
- **诚实标注**：docs §微服务化改造 + §Spring Cloud 架构升级为**完全空节**（仅标题）→ KP-06/07 架构师发散补全；**my-xhs benchmark 为模拟 payload**（OrderServiceBenchmark 注释"Simulate order creation logic/Lightweight simulation"——`simulateOrderCreation` 是 `Math.sqrt` 循环，**不代表真实服务性能**——"看起来在做≠真的实现"教训（05 篇纪律延续））；my-xhs 已是微服务（docs 的"拆分"场景对照现状）
- **关联标注**：衔接 07/08（Eureka 注册中心——本篇调用升级的服务发现基础）、17 节（分布式事件——跨域一致性的改造配套）、stage-1 11/12（负载均衡）、02 篇（测量方法论）

---

## 五、本节小结（三层次视角）

**需求**：微服务架构升级——JMH 量化（尺子）+ 微服务化改造（怎么拆）+ Spring Cloud 调用（怎么连）+ 前后对比（评估）。

**自主实现核心**：若我设计——①JMH 骨架（@Benchmark/@State/@Warmup/@Fork + 返回结果防 DCE）②按域拆 8 域（数据随服务拆分 + 事件化一致性）③Feign + 注册中心 + 负载均衡调用链 ④控制变量前后对比。

**参考实现**：docs（JMH 完整教学 + 两空节）+ **my-xhs 实证**（JMH 注解骨架、Feign 3 客户端）+ JDK（@Contended）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**架构升级的完整纪律**"——升级前建尺子（JMH/JMeter/JFR）、按域拆服务、Feign 链路、控制变量评估；my-xhs 已走完全程（15 服务 + Feign 59 处），是 docs 改造的现代实例。

**待验证汇总**：
- `-XX:-RestrictContended` 默认值（JDK8+ 限制 @Contended 仅内部用）
- my-xhs benchmark 的真实服务基准（当前为模拟）——18 节/压测专题展开
- 微服务化改造的灰度切流细节（docs 空节无内容）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标/知识点 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| JMH 微基准 | ⚠️ 骨架完整（OrderServiceBenchmark 全注解实证）但 **payload 为模拟**（Math.sqrt 循环） | 差距：注入真实 service/DB 调用才是可用基准（docs 意图"对比性能变化"的前提） |
| 微服务化改造 | ✅ **已完成**：15 服务（docs 8 模块的现代落地） | 无（docs 的"改造"在 my-xhs 是既成事实） |
| Spring Cloud 调用升级（Feign） | ✅ 59 处 Feign 引用 + order 3 客户端实证 | 无 |
| 改造前后性能对比 | ❌ 无对比数据 | 缺口：各服务 API 性能基线未建立（02 篇同一缺口） |

**结论**：06 篇的"改造"动作在 my-xhs 已完成（架构层面）；差距集中在**测量面**（JMH 真实化 + 性能基线）。

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 JMH 官方样例转写（完整可用）+ 两空节（微服务化/Spring Cloud）；my-xhs 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：微服务架构升级的完整架构该讲什么

docs 覆盖 JMH 用法与改造意图。完整还该包含：

1. **"升级前先有尺子"是架构纪律**（docs 第 3 条 + 发散）：JMH 量化单点 → JMeter 量化入口 → JFR 诊断现场——**三工具在升级评估中各有角色**（02 篇 KP-07 分工的架构级应用）
2. **微基准正确性是"反编译器"的艺术**（docs 示例 + 发散）：DCE/常量折叠是 C2 优化（02 篇 KP-07 DoEscapeAnalysis 默认开）对测量的干扰——**"返回结果 + 非 final 字段"是底线**（docs 示例逐行示范）
3. **伪共享是并发性能的隐藏杀手**（docs 数据 + 发散）：**reader 列** 784.716 → 1616.513 ops/us（+106%，hierarchy/padded 填充后）——**缓存行隔离**在计数器/高并发状态场景必查（common/counter 模块场景相关 `[待验证]`）
4. **微服务化改造的"域优先"策略**（docs 空节发散）：先拆边界清晰的域（搜索/优惠券——无强事务依赖），后拆核心交易域（订单/支付——需事件化一致性）——**绞杀者模式逐模块替换**
5. **拆分后的一致性债**（架构师发散）：跨域 join 没了 → 事件/补偿（17 节分布式事件、stage-2 17/18 本地消息表/TCC）——**改造评估必须计入一致性方案成本**
6. **my-xhs benchmark 的警示**（诚实标注）：模拟 payload 的基准只能验证"骨架可跑"，**不能当性能结论**——工程基准必须注入真实服务（连接池/DB 真实调用）
7. **调用链升级的收益形态**（docs 意图 + 发散）：直连 IP → Feign+注册中心：**多一跳网络 vs 解耦/弹性/容错**——docs"评估性能变化"即量化此权衡；链路延迟可被注册中心本地缓存/负载均衡策略优化

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| JMH vs 手写计时 | 测量可信 vs 简单 |
| Throughput vs SampleTime | 吞吐 vs 延迟分布（P99） |
| Scope.Thread vs Benchmark | 无竞争 vs 真实竞争 |
| 返回结果 vs 不返回 | 正确 vs 被 DCE 消除 |
| 先拆边界域 vs 先拆核心域 | 风险低 vs 价值高（绞杀者） |
| 直连 vs Feign+注册中心 | 少一跳 vs 解耦/弹性/容错 |
| 逐模块灰度 vs 大爆炸 | 可归因可回滚 vs 快但不可控 |

### 常见坑/反模式

1. **微基准不防 DCE/常量折叠**（本篇最大坑）：docs 示例的 measureWrong 快得离谱——**结果必须被消费**
2. **把模拟基准当真**：my-xhs benchmark 是骨架示范——模拟 payload 数据不能当性能基线
3. **改造无基线**：不测"改前"就拆——无法回答"改好了吗"（02 篇 6 步纪律）
4. **跨域 join 残留**：拆分后仍跨服务 join——性能灾难（应事件化/冗余字段）
5. **伪共享不知情**：高并发计数器同一缓存行——writer 竞争掉一半吞吐（docs 数据）
6. **大爆炸改造**：一次拆完 8 域——事故无法归因/回滚（绞杀者模式反例）
7. **只看吞吐均值**：SampleTime 才能看尾部延迟（02 篇 P99 纪律）

### 生态位置

- **stage-3 教学主线**：06 是容器/服务组（05-10）第二篇——**单体→微服务的架构升级**承上启下：05 容器基线 → 06 改造 + JMH → 07/08 Eureka（发现）→ 09 HTTP → 10 RPC
- **前后篇衔接**：stage-1 03/04（REST 基础）→ 本篇调用升级 → 07/08 节（注册中心落地）；stage-1 11/12（负载均衡）→ 本篇 Feign 链路；02 篇（测量方法论）→ 本篇 JMH
- **与源码提取的关系**：JMH（官方仓库）为机制源；my-xhs benchmark + feign 为实例；07/08 节将深挖 Eureka

**架构师视角结论**：本篇不是背 JMH 注解，而是掌握**架构升级的完整纪律**——"尺子先行"（JMH/JMeter/JFR 三工具）、"域优先拆分"（绞杀者）、"Feign 调用链"、"控制变量评估"；docs 两空节（微服务化/Spring Cloud）由 my-xhs 实证补全——它是 docs 改造意图的现代落地（15 服务 + 59 处 Feign），"docs 骨架 × 实例实证"范式再次验证。
