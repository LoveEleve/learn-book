# stage-3 · 第 33 节：加餐八：现代 Java 发展与变化 — 知识点提取

> 课程：stage-3 三高架构 第 33 节（收尾篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/33.加餐八：现代 Java 发展与变化.md`
> 提取时间：2026-08-12 | 权重：核心（Java 9-21 版本演进全景——stage-3 收尾）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）

---

## 一、本节概览

- **技术域**：Java 9-21 演进（语言/API/JVM 三段式全景，含 60+ JEP）
- **维度**：`[规范]`（语言/API 演进）+ `[性能优化]`（JVM 演进）+ `[工程问题]`（生态影响）
- **核心命题**：**现代 Java 演进全景**——docs 主要内容：①JDK 9-21 语言/API/底层变化（docs 正文全量）②对生态的影响 ③直播互动（课堂记录）；docs 与 02/05/04/28 篇的 JVM 演进部分重叠（交叉引用）
- **知识点数**：8 个
- **前置**：02 篇（JIT/GC 演进）、05 篇（GC 全景）、04 篇（JFR）、28 篇（GraalVM）

## 前置条件清单
读者需先掌握：
1. **GC 演进**（02/05 篇：G1 默认/ZGC/CMS 移除）
2. **JFR**（04 篇：JEP328 开源化）
3. **GraalVM**（28 篇：JEP410 移除实验 AOT 的替代者）
未达前置者，先补：02/05 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **增量提取**：docs 的 JVM 演进（G1/ZGC/JFR/CDS——02/05/04 篇已深挖）交叉引用；本篇提取语言/API 演进增量
- **实例锚定**：my-xhs JDK17——var/Record/Sealed 使用面实证
- **docs 场景 vs 现状**：docs 覆盖到 Java 21（2023）；my-xhs 用 17（LTS）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Java 9-21 版本节奏与 LTS 线【32 篇 KP-01 衔接】
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：32 篇 KP-01
- **来源**：docs 三段结构（Java 9-11/12-17/18-21）+ 32 篇衔接
- **需求**：掌握 **Java 9-21 的发布节奏**——docs 三段式组织 + LTS 锚点（11/17/21）
- **自主实现**：若我设计——按 LTS 锚点组织认知：9-11（模块化+JIT 演进）→ 12-17（GC 定型+语言现代化）→ 18-21（虚拟线程+API 重构）
- **参考实现**（docs 三段 + 发散 + 32 篇衔接）：**docs 三段**——**Java 9-11**（模块化/Compact Strings/G1 默认——`Java 9-11` 标题实证）→ **Java 12-17**（ZGC/Shenandoah 转正/Switch 表达式/Record 预览——`Java 12 - 17` 标题实证）→ **Java 18-21**（虚拟线程/模式匹配正式/Foreign API——`Java 18 - 21` 标题实证）；**LTS 锚点**——11（2018）→ 17（2021）→ 21（2023）（32 篇 KP-01 衔接——8/11/17/21 线）；**节奏（发散）**——6 个月 + LTS 每 2 年（32 篇已提）
- **对比取舍**：**LTS vs 中间版**——稳定 vs 尝鲜（32 篇 KP-01 交叉）
- **测试佐证**：docs 三段标题（实证）+ 32 篇 KP-01（交叉引用）

### KP-02 语言演进主线（模块化/var/Record/Sealed/模式匹配/虚拟线程）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：无
- **来源**：docs 各版本"主要语言变化"节 + my-xhs 实证
- **需求**：掌握 **Java 9-21 语言演进主线**——docs：模块化（9）→ var（10）→ Switch 表达式（12-14）→ Text Blocks（13-15）→ Record/Sealed/模式匹配（14-17）→ 虚拟线程（19-21）
- **自主实现**：若我设计——按主线记忆：模块化（工程组织）→ 简洁语法（var/Text Blocks）→ 数据建模（Record/Sealed）→ 并发革命（虚拟线程）
- **参考实现**（docs 各版本 + my-xhs 实证）：**演进主线（docs 全量）**——①**Java 9**：**模块化系统（JSR 376）** + 语言更新（try-with-resources final/diamond 匿名类/接口 private 方法/@SafeVarargs 实例方法）②**Java 10**：**var 本地变量类型推断**③**Java 12-14**：Switch 表达式（JEP 325 预览 → 361 标准）④**Java 13-15**：Text Blocks（355 预览 → 378 标准）⑤**Java 14-16**：**Record**（359 预览 → 395 正式）+ instanceof 模式匹配（305 → 394）⑥**Java 15-17**：**Sealed 类**（360 → 409 正式）+ Switch 模式匹配（406 预览——17）⑦**Java 19-21**：**虚拟线程**（425 预览 → 444 正式）+ Record 模式（440）+ Switch 模式匹配（441）+ String 模板（430 预览）+ 未命名模式/类（443/445 预览）+ Scoped Values（446 预览）；**my-xhs 实证**——**var 使用**（common/chaos/ChaosInterceptor 等 6 文件 grep 实证——JDK17 语言特性实际使用）
- **对比取舍**：**预览→正式流程**——新特性渐进（docs 大量 [预览] 标注）——**生产用正式特性**（my-xhs 17 的 var）
- **测试佐证**：docs 各版本语言节（JEP 全量）+ my-xhs（var 6 文件实证）

### KP-03 API 演进（VarHandle/StackWalker/集合工厂/HTTP Client/Foreign API）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：无
- **来源**：docs 各版本"主要 API 变化"节
- **需求**：掌握 **Java 9-21 API 演进主线**——docs：Unsafe 替代（VarHandle）→ StackWalker → 集合工厂 → HTTP Client → Foreign API（JNI 替代）
- **自主实现**：若我设计——按"替代关系"记忆：VarHandle 替代 Unsafe 原子操作/StackWalker 替代 StackTraceElement/HTTP Client 替代 HttpURLConnection/Foreign API 替代 JNI
- **参考实现**（docs 全量）：**引入主线（docs）**——①**Java 9**：**ProcessHandle**（JEP 102 进程管控）/ **VarHandle**（JEP 193——**正式 API 替代 Unsafe**，原子和内存屏障操作）/ 日志 API（JEP 264）/ XML Catalog（JEP 268）/ **StackWalker**（JEP 259——替代 StackTraceElement 体系）；集合工厂（JEP 269 `Set.of(...)`）/ CompletableFuture 增强（JEP 266）/ @Deprecated since+forRemoval（JEP 277）/ Thread.onSpinWait（JEP 285）/ ObjectInputFilter（JEP 290）②**Java 11**：**HTTP Client 标准**（JEP 321）/ 安全（Curve25519/448 JEP 324、ChaCha20 JEP 329、**TLS 1.3 JEP 332**）③**Java 16**：Unix-Domain Socket（JEP 380）/ **Foreign Linker（JEP 389——替代 JNI 孵化）**④**Java 17**：Foreign Function & Memory（JEP 412 孵化）/ Random 增强（JEP 356）⑤**Java 18-21**：**UTF-8 默认**（JEP 400）/ **反射用 MethodHandles 重实现**（JEP 416）/ 简单 Web Server（JEP 408）/ 有序集合（JEP 431）/ Foreign API 演进（419→424→434→442）
- **对比取舍**：**替代演进**（Unsafe→VarHandle/StackTraceElement→StackWalker/HttpURLConnection→HTTP Client/JNI→Foreign API）——**安全化/标准化方向**
- **测试佐证**：docs 各版本 API 节（JEP 全量）

### KP-04 JVM 演进（Compact Strings/G1/ZGC/Shenandoah/CDS/JFR）【02/05/04 篇衔接】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：02/05/04 篇
- **来源**：docs 各版本"主要 JVM 变化"节 + 交叉引用
- **需求**：docs 的 JVM 演进与 **02/05/04 篇已深挖内容重叠——交叉引用不重复提取**
- **自主实现**：若我设计——只提取 docs 新增细节（Compact Strings 等），已提取的（G1/ZGC/JFR/CDS）交叉引用
- **参考实现**（docs + 交叉引用）：**已提取（交叉引用）**——**G1 默认**（JEP 248——02 篇 KP-10 已证）/ **CMS 移除**（JEP 291 预告 + 363 移除——02/05 篇已证）/ **ZGC**（333 实验 → 377 正式——02/05 篇已证）+ 分代 ZGC（JEP 439——21 新增 `[docs 新]`）/ **Shenandoah**（189 实验 → 379 正式——02 篇已提）/ **JFR**（JEP 328——04 篇已证）/ **CDS/AppCDS**（JEP 310——02 篇已证）+ 默认 CDS（JEP 341——12 新增 `[docs 新]`）/ **统一日志**（JEP 158——05 篇 -Xlog 已提）；**docs 新增细节**——**Compact Strings**（JEP 254——String byte[] 存储的 JVM 侧）/ Epsilon 无操作 GC（JEP 318——11）/ GC 接口（JEP 304——10）/ Thread-Local Handshakes（JEP 312）/ Code Cache 分段（JEP 197）/ jshell（JEP 222）/ jlink（JEP 282）/ 多版本 JAR（JEP 238）
- **对比取舍**：**已提取交叉 vs 新增提取**——避免重复（06 纪律）
- **测试佐证**：docs JVM 节 + 02/05/04 篇交叉引用

### KP-05 移除与弃用（Java EE 模块/SecurityManager/偏向锁/实验 AOT）【docs 主要内容①底层变化】
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[过时→移除清单（版本演进）]` | **置信度**：High
- **前置**：无
- **来源**：docs 各版本"移除/弃用"项
- **需求**：掌握 **Java 9-21 的移除/弃用清单**——docs：Java EE 模块/CORBA/Nashorn/SecurityManager/偏向锁/实验 AOT（生态迁移的关键）
- **自主实现**：若我设计——按"移除对象"记忆：中间件模块（JEE）→ 脚本引擎（Nashorn）→ 安全（SecurityManager）→ 性能实验（偏向锁/实验 AOT）
- **参考实现**（docs 全量）：**Java 11**——**移除 Java EE 和 CORBA 模块（JEP 320）**：JAX-WS/JAXB/JAF/Common Annotations/CORBA/JTA（**javax→jakarta 迁移的起点——04 纪律衔接**）+ Nashorn 弃用（JEP 335）+ Pack200 弃用（JEP 336）+ 移除 Applet/WebStart/JavaFX；**Java 14**——Pack200 移除（JEP 367）+ CMS 移除（JEP 363——02 篇已证）；**Java 15**——移除 Solaris/SPARC（JEP 381）+ **偏向锁弃用（JEP 374）**+ Nashorn 移除（JEP 372）；**Java 16**——javah 移除（JEP 313）；**Java 17**——**SecurityManager 弃用（JEP 411）**+ RMI Activation 移除（JEP 407）+ **移除实验 AOT 和 JIT 编译器（JEP 410——由 GraalVM 替代——28/32 篇衔接）**；**Java 18**——Finalization 弃用（JEP 421）；**Java 21**——Windows 32 位弃用（JEP 449）+ **预备禁止动态 Agent 加载（JEP 451）**
- **对比取舍**：**移除节奏**——弃用（deprecate）→ 移除（remove）两阶段（docs 大量 deprecate for removal）——**生产依赖需提前规划迁移**
- **测试佐证**：docs 各版本移除项（JEP 全量）+ 02 篇（CMS 移除交叉）+ 28/32 篇（JEP410→GraalVM 衔接）

### KP-06 强封装与反射受限（JEP 396/403 + 反射 MethodHandles 重实现）【docs 主要内容①底层变化】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：反射基础
- **来源**：docs JEP 396/403/416 + 架构师发散
- **需求**：掌握 **JDK 内部强封装的生态影响**——docs：16 强封装默认（396）→ 17 强封装（403）→ 18 反射用 MethodHandles 重实现（416）
- **自主实现**：若我设计——理解影响：强封装（--add-opens 白名单）+ 反射重实现（MethodHandles 底层）——**框架对 JDK 内部 API 的依赖被切断**
- **参考实现**（docs JEP + 发散 + 衔接）：**docs**——**JEP 396**（16：JDK 内部 API 默认强封装）+ **JEP 403**（17：强封装完成）+ **JEP 416**（18：反射基于 MethodHandles 重实现）；**影响（发散）**——①**框架/工具对 sun.misc 等内部 API 的访问需 --add-opens 显式开放**（12 篇 MyBatis/18 篇 Arthas 类工具的影响面）②反射性能/行为变化（MethodHandles 底层——02 篇 JIT 衔接）③**JDK 内部 API 不可依赖**（生态标准化方向——VarHandle 等正式 API 替代——KP-03）
- **对比取舍**：**强封装（安全）vs 反射自由（便利）**——内部 API 封闭 vs 框架 hack——**生态向正式 API 迁移**
- **测试佐证**：docs JEP 396/403/416 + 架构师发散

### KP-07 现代 Java 对生态的影响【docs 主要内容②】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-05/06
- **来源**：docs 主要内容② + 架构师发散
- **需求**：docs 主要内容②——现代 Java 发展对生态的影响（发散整合）
- **自主实现**：若我设计——影响四面：框架升级（javax→jakarta）/JVM 演进承接/语言特性采用/内部 API 依赖
- **参考实现**（docs 意图 + 发散 + 全篇衔接）：**①命名空间迁移**（KP-05 JEP320 移除 JEE 模块——**javax→jakarta 的根源**——04 纪律：迁移≠机制——09 篇 jakarta 实证）**②JVM 演进承接**（G1/ZGC/JFR——02/05/04 篇——my-xhs G1+JFR 实证）**③语言特性采用**（Record/Sealed/虚拟线程——my-xhs var 实证）**④内部 API 依赖清理**（KP-06 强封装——框架升级压力）；**my-xhs 实证**——Boot 3.2.5（jakarta——09 篇）+ JDK17（var——KP-02）+ G1/JFR（02/04 篇）——**现代 Java 生态的完整承接**
- **对比取舍**：**生态跟进 vs 保守**——新特性收益 vs 迁移成本——**LTS 节奏内采用正式特性**
- **测试佐证**：docs 主要内容② + my-xhs（jakarta/var/G1 实证）

### KP-08 现状核对（my-xhs JDK17 语言/API 使用面）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs 主题 + my-xhs 实证
- **需求**：docs 的 Java 演进 ↔ my-xhs JDK17 使用面核对
- **自主实现**：若我设计——核对面：语言特性（var/Record/Sealed/switch 模式匹配）、API（VarHandle/StackWalker/集合工厂）、JVM（G1/ZGC 选择）、迁移（17→21）
- **参考实现**（my-xhs 实证 + docs 对照）：**语言**——**var 使用实证**（6 文件）；Record/Sealed `[待验证：使用面]`（17 可用——docs 395/409 正式）；**API**——`[待验证：VarHandle/StackWalker 使用]`；**JVM**——G1+MaxGCPauseMillis（02 篇）+ JFR 未启用（04 篇）；**迁移**——JDK17 LTS（合理）→ 21 迁移评估 `[决策待定：虚拟线程（JEP 444）是高价值迁移点——docs 21 正式]`；**强封装影响**——`[待验证：my-xhs 依赖内部 API？]`
- **对比取舍**：**17（当前 LTS）vs 21（新 LTS）**——稳定 vs 虚拟线程/模式匹配正式——**虚拟线程是高并发演进点（衔接 17 篇线程模型）**
- **测试佐证**：my-xhs（var 6 文件 + G1/JFR——02/04 篇）+ docs（21 虚拟线程 JEP 444）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 版本节奏与 LTS 线 | 规范 | 核心 | P1 | 🟡 | 有效 | High |
| 语言演进主线 | 规范 | 核心 | P1 | 🔴 | 有效 | High |
| API 演进主线 | 规范 | 核心 | P1 | 🟡 | 有效 | High |
| JVM 演进（交叉引用） | 性能优化 | 核心 | P1 | 🟡 | 有效 | High |
| 移除与弃用清单 | 规范 | 核心 | P1 | 🟡 | 过时→移除 | High |
| 强封装与反射受限 | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| 现代 Java 对生态影响 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 现状核对（JDK17 使用面） | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（var 实证）+ 02/04/05/28/32 篇交叉引用
- **关键实证**（本次）：my-xhs var 使用 6 文件（ChaosInterceptor/RedissonConfig/GracefulShutdownListener/CouponService/MockPayService/RefundNotifyCompensateJob——grep 实证）
- **诚实标注**：docs 为**长篇（656 行）**但主要为 JEP 清单（机械罗列）——按"主线提取"（语言/API/JVM/移除 4 线）而非逐 JEP；docs 主要内容③（直播互动）为课堂记录 `[跳过：非知识内容]`；JVM 演进与 02/05/04 篇重叠 → 交叉引用（KP-04 声明）；移除实验 AOT（JEP 410）→ 28/32 篇 GraalVM 衔接
- **关联标注**：02 篇（JIT/GC 演进）；05 篇（GC 全景）；04 篇（JFR）；28/32 篇（GraalVM/Native——JEP410）；09 篇（jakarta——JEP320 迁移）；17 篇（线程模型——虚拟线程衔接）

---

## 五、本节小结（三层次视角）

**需求**：现代 Java 演进全景——语言/API/JVM/移除 4 线（docs 主要内容①②）。

**自主实现核心**：若我设计——①语言主线（模块化→var→Record/Sealed→虚拟线程）②API 替代演进（Unsafe→VarHandle/JNI→Foreign）③JVM 演进（G1/ZGC 定型）④移除清单（JEE 模块/SecurityManager/实验 AOT→GraalVM）⑤强封装影响（--add-opens）。

**参考实现**：docs（60+ JEP 清单——按主线提取）+ my-xhs 实证（var 6 文件）+ 02/05/04/28 篇交叉引用（JVM 演进不重复）。**docs JEP 照录 + 主线归纳**。

**对比取舍**：知识本体是"**现代 Java 演进的主线与生态影响**"——四线归纳（语言/API/JVM/移除）、预览→正式流程、强封装与迁移（javax→jakarta）；my-xhs JDK17 承接（var 实证 + G1 + jakarta）。

**待验证汇总**：
- my-xhs Record/Sealed/switch 模式匹配使用面（17 正式特性）
- my-xhs 依赖 JDK 内部 API？（强封装影响面）
- JDK21 迁移评估（虚拟线程 JEP 444 高价值点）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 语言特性（var） | ✅ var 使用 6 文件（grep 实证） | Record/Sealed 使用面 `[待验证]` |
| JVM 演进承接 | ✅ G1+MaxGCPauseMillis（02 篇）+ JDK17 | JFR 未启用（04 篇差距延续） |
| 强封装影响 | ❓ 未核（依赖内部 API？） | `[待验证]`：--add-opens 使用面 |
| 移除/弃用 | ✅ Boot 3.2.5 jakarta（09 篇——JEP320 迁移已完成） | 无 |
| JDK21 迁移 | ❌ 未迁移（17 LTS） | `[决策待定]`：虚拟线程（JEP 444）为高价值迁移点——17 篇线程模型衔接 |

### 差距清单（Java 演进层）

1. **P2**：JDK21 迁移评估（虚拟线程/模式匹配正式——docs 21 全量）
2. **P2**：强封装影响核对（内部 API 依赖/--add-opens）
3. **P3**：Record/Sealed 采用（17 正式——代码现代化）

**结论**：33 篇——my-xhs JDK17 承接良好（var/G1/jakarta）；**虚拟线程迁移（21）为高价值演进点（P2）**；**stage-3 全部 33 篇 docs 提取完成**。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 60+ JEP 机械清单；知识本体 = 主线归纳 + 交叉引用；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：现代 Java 演进的完整认知该讲什么

docs 是 JEP 清单。完整还该包含：

1. **"四条主线贯穿 9-21"**（docs 归纳 + 发散）：**语言**（模块化→简洁语法→数据建模→虚拟线程）/ **API**（替代演进：Unsafe→VarHandle、JNI→Foreign）/ **JVM**（GC 定型：G1→ZGC/Shenandoah）/ **移除**（JEE 模块→SecurityManager→实验 AOT）——**清单背后的方向：安全化+标准化+并发革命**
2. **"虚拟线程是最大的生态变量"**（docs 21 + 发散）：JEP 444 正式——**线程模型革命**（百万线程 vs 平台线程池）——**17 篇（线程模型/背压）的承接**：高并发服务的下一个范式（18 篇线程池教训的重新审视）
3. **"javax→jakarta 是 JEP320 的直接后果"**（docs 11 + 发散）：移除 JEE 模块 → 命名空间迁移（09 篇 Boot 3.2.5 实证）——**04 纪律：迁移≠机制**的活教材
4. **"强封装是框架升级的隐性成本"**（docs 396/403 + 发散）：内部 API 依赖的框架需 --add-opens 适配——**生态向正式 API 迁移**（12 篇 MyBatis 等的影响面）
5. **"预览→正式是采用节奏"**（docs 大量 [预览] + 发散）：新特性两三轮预览才转正（ZGC 15 正式/虚拟线程 21 正式）——**生产只用正式特性**（LTS 节奏内）
6. **"移除实验 AOT 印证 Native 路线"**（docs 17 JEP410 + 发散）：实验 AOT/JIT 移除、由 GraalVM 替代（28 篇）——**Java 官方的 Native 方向 = GraalVM 生态 + Leyden 探索（32 篇）**

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| LTS vs 中间版 | 稳定 vs 新特性（6 个月节奏） |
| 正式特性 vs 预览 | 生产可用 vs 尝鲜（两三轮预览） |
| 虚拟线程 vs 平台线程池 | 百万线程 vs 既有模型（21 正式） |
| 强封装 vs 反射自由 | 内部 API 封闭 vs 框架 hack（--add-opens） |
| 17 vs 21 | 稳定 vs 虚拟线程（my-xhs 决策待定） |

### 常见坑/反模式

1. **生产用预览特性**：行为可能变（预览→正式有调整）——LTS 正式特性（docs 流程）
2. **依赖 JDK 内部 API**：强封装后崩——--add-opens 或迁移正式 API（KP-06）
3. **javax 依赖不迁移**：JEP320 移除后 ClassNotFound——jakarta（09 篇）
4. **忽略虚拟线程演进**：高并发服务还在堆平台线程池——21 是下一范式（17 篇衔接）
5. **JVM 参数照搬旧版**：CMS/偏向锁参数已移除——升级前清理（05 篇 CMS 教训）

### 生态位置

- **stage-3 教学主线**：**收尾篇**——02（性能方法论）→ 05（GC）→ 33（Java 演进全景）——**贯穿全篇的 JDK 基准（11/17）在此收束**；**stage-3 全部 33 篇 docs 提取完成**
- **前后篇衔接**：02/05/04 篇（JVM 演进——交叉引用）；28/32 篇（GraalVM/Native——JEP410）；09 篇（jakarta——JEP320）；17 篇（线程模型——虚拟线程）
- **与源码提取的关系**：openjdk11u/jdk17（JDK 机制验证源——全篇贯穿）

**架构师视角结论**：本篇以 **docs 60+ JEP 清单 → 四线归纳**（语言/API/JVM/移除）——知识本体是"**现代 Java 演进的主线与生态影响**"：预览→正式流程、替代演进（Unsafe→VarHandle）、强封装与迁移（javax→jakarta）、**虚拟线程（21）为下一范式**；my-xhs JDK17 承接良好（var/G1/jakarta），**JDK21 迁移（虚拟线程）为高价值演进点**；**stage-3 33 篇 docs 全部提取完成**。
