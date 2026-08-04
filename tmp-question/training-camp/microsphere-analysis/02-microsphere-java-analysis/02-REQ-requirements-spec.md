# 02-REQ：microsphere-java 完整需求规格（v2，源码已验证）

> **v2 重写说明**：v1 声称"70% 重叠 Spring"是基于类名 grep 的猜测——没有读任何源码。本次 v2 逐文件（342 主文件）+ 逐方法 + Spring Framework v6.2.17 源码对照验证后重写。**结论：不是 70% 重叠，而是 0%~90% 随包剧烈变化。**
>
> 需求分三类：
> 1. **已实现需求**（REQ-001~020，20 项）——按领域分组
> 2. **待改进**（REQ-D01~D06，6 项）——已知 bug + 测试覆盖缺口
> 3. **全新发散**（REQ-N01~N05，5 项）——生产需要但未覆盖
>
> **基准**：Java 8+，零框架依赖（仅 `java.*` + `javax.annotation`）

---

## 项目定位

**microsphere-java 是微球生态的"标准库"——从类加载器反射深度内省、运行时 artifact 检测、泛型类型系统、SPI 发现注册、文件监听、进程管理到方法黑名单控制。它不是 Spring 的替代品——它是 Spring 不碰的 JDK 内部领域和 Spring 实现不了的编译期工具链。**

**与 Spring Framework 重叠按包分布**：

| 包 | 文件数 | Spring 重叠 | 判定 |
|---|:---:|:---:|------|
| classloading | 16 | ~0% | 完全独特：ArtifactDetector/URLClassPathHandle 反射写/BannedArtifact |
| net | 13 | ~0% | 完全独特：URLStreamHandlerFactory 反射注册/sub-protocol/classpath: 协议 |
| process | 6 | ~0% | 完全独特：ProcessExecutor/PID resolver/ProcessManager |
| filter | 9 | ~0% | 完全独特：FilterOperator AND/OR/XOR/scanner 体系 |
| management | 12 | ~10% | 大部分独特：MXBean 全量获取/@DescriptorKey |
| invoke | 2 | ~10% | 完全独特：MethodHandleUtils Lookup 深度封装 |
| metadata | 9 | ~10% | 独特：独立配置属性元数据 SPI |
| json | 8 | ~15% | Android fork+JSONUtils Bean 转换 |
| logging | 8 | ~15% | 独特：可插拔 SPI 日志门面（vs Spring JCL 硬编码） |
| lang | 15 | ~30% | ClassDataRepository/Wrapper/Deprecation 独特，Throwable* 部分重叠 |
| io | 27 | ~40% | Serializer SPI/FileWatchService 独特，IOUtils~70% 重叠 StreamUtils |
| event | 10 | ~50% | 结构重叠但 4 项独特机制（SPI 加载/条件过滤/泛型分发/自扫描） |
| reflect | 21 | ~50% | JavaType~90% 重叠但 Definition/黑名单/JDK9 可访问性独特 |
| convert | 52 | ~60% | 功能重叠但 SPI 静态注册 vs Spring 运行时注册 |
| collection | 32 | ~70% | 工厂方法独特，Map/List/Set/Queue 类型无 Spring 对应 |
| util | 34 | ~50% | ClassLoaderUtils/ArrayUtils/ShutdownHookUtils/Version 等独特，StringUtils 子集 |

**外围模块**：

| 模块 | 文件数 | Spring 重叠 | 判定 |
|---|:---:|:---:|------|
| lang-model | 16 | ~0% | 完全独特：javax.lang.model 编译期工具库 |
| annotations | 8 | ~50% | Nullable/NonNull 重叠，Immutable/Experimental/Since 独特 |
| annotation-processor | 4 | ~30% | 配置元数据生成器，与 Spring Boot configuration-processor 同定位 |
| jdk-tools | 1 | ~0% | 完全独特：编程式 Compiler 封装 |

**源码**：`microsphere-java-core`（294）+ 外围（48）= 342 主文件。

---

## 一、JDK 内部反射与类加载（classloading/，16 文件，~0% 重叠）

### REQ-001：运行时 artifact 检测引擎

**问题**：运行时想知道加载的 jar 的 GAV（groupId/artifactId/version）和物理位置——没有标准 JDK API。

**产出**：`ArtifactDetector` + 3 个 Resolver（Maven pom.properties / MANIFEST.MF / 文件名拆解）+ `MavenArtifact`（g:a:v:location）。

**状态**：[已验证实现]

### REQ-002：运行时 classpath URL 动态移除

**问题**：`URLClassLoader` 只能 addURL 不能移除——但你需要在运行时排除冲突 jar。

**产出**：`URLClassPathHandle` SPI → 反射 `sun.misc/jdk.internal.URLClassPath` 内部字段 → `BannedArtifactClassLoadingExecutor` 读 `META-INF/banned-artifacts` → 匹配后 removeURL。

**状态**：[已验证实现]

### REQ-003：ClassLoader 深度内省

**产出**：`ClassLoaderUtils`（2138 行）——反射读 ClassLoader 私有 `classes` Vector、继承链 `findLoadedClass`、配合 `ClassDataRepository` 做 classpath 三维反向索引。

**状态**：[已验证实现]

---

## 二、URL 协议增强（net/，13 文件，~0% 重叠）

### REQ-004：动态 URLStreamHandlerFactory 注册

**问题**：JDK9+ `URL.setURLStreamHandlerFactory()` 只能调一次——但需要运行时动态注册新协议。

**产出**：`MutableURLStreamHandlerFactory` + 反射 `URL.factory` 绕过单次限制 + `ExtendableProtocolURLStreamHandler` sub-protocol 机制（`{p}:{sub1}:{sub2}://` 重构为 matrix 参数）。

**状态**：[已验证实现]

### REQ-005：自定义 URL 协议（classpath:/console:）

**产出**：`classpath.Handler`（ClassLoader.getResource 解析路径）、`console.Handler`（System.in/out 包装为 URLConnection）——注册到 java.net.URL 体系。Spring 的 `classpath:` 仅是 Resource 层虚拟前缀。

**状态**：[已验证实现]

### REQ-006：URL 工具

**产出**：`URLUtils`（1724 行）——归档定位（jar:/file: 解析）、sub-protocol、matrix 参数处理。

**状态**：[已验证实现]

---

## 三、进程管理（process/，6 文件，~0% 重叠）

### REQ-007：PID 获取 + 进程执行

**产出**：`ProcessIdResolver` SPI（3 种实现：ProcessHandle/VMManagementImpl/RuntimeMXBean）+ `ProcessExecutor`（stderr 合并防死锁 + Future.get(timeout) + 非 0 退出抛 IOException）+ `ProcessManager` 跟踪运行中进程。Spring core 无任何进程管理。

**状态**：[已验证实现]

---

## 四、反射工具（reflect/，21 文件，~50% 重叠）

### REQ-008：MethodUtils——方法反射 + 黑名单 + 覆写链

**产出**：
- `findMethod(Class, String, Class...)`——继承链查找
- **`banMethod/initBannedMethods/clearBannedMethods`**——系统属性 `microsphere.reflect.banned-methods` 配置方法黑名单，ban 后 findMethod 返回 null
- **`overrides(Method, Method)`**——JLS 8.4.8 覆写判断
- **`findNearestOverriddenMethod/findOverriddenMethod`**——覆写链查找
- **`invokeMethod(obj, "methodName", arg1, arg2)`**——运行时实参类型自动匹配调用（Spring 需要精确 Method 对象）
- **`isCallerSensitiveMethod`**——`@jdk.internal.reflect.CallerSensitive` 检测

**状态**：[已验证实现]

### REQ-009：ReflectionUtils——三层 caller class 检测

**产出**：
- `getCallerClass()`——三层降级：`sun.reflect.Reflection#getCallerClass` MethodHandle → `StackWalker` → `StackTraceElement`
- `readFieldsAsMap(Object)`——递归读全部非静态字段
- JDK9+ `isInaccessibleObjectException` 检测

**状态**：[已验证实现]

### REQ-010：AccessibleObjectUtils——JDK9+ 可访问性

**产出**：
- `trySetAccessible()`——JDK9+ `MethodHandle.trySetAccessible`，JDK8- 回退 `setAccessible`
- `canAccess()`
- JEP 396 `--add-opens` 提示——不可访问时自动生成 JVM 参数建议日志

**状态**：[已验证实现]

### REQ-011：泛型类型系统

**产出**：
- `JavaType`（1329 行）——与 Spring `ResolvableType` 对标（~90% 功能重叠）
- `TypeUtils`（1579 行）——`resolveActualTypeArguments` 继承链泛型传播算法（TypeArgument 数组逐层传播），独立于 Spring 链式对象
- `ParameterizedTypeImpl`（242 行）——可编程创建参数化类型

**状态**：[已验证实现]

### REQ-012：声明式反射体系（Definition 系列）

**产出**：7 文件——用"版本+类名+方法名+参数类名"字符串声明反射目标，运行时才解析——用于兼容性探测。Spring 无此设计，~100% 独特。

**状态**：[已验证实现]

---

## 五、泛型工具（lang/，15 文件，~30% 重叠）

### REQ-013：classpath 三维反向索引

**产出**：`ClassDataRepository`（251 行）——构造时全量扫描 bootstrap+system classpath，预建 className↔classPath↔packageName 三张只读 Map。Spring 按需 ASM 扫描，不预建索引。

**状态**：[已验证实现]

### REQ-014：Wrapper 解包体系 + Deprecation 元数据

**产出**：
- `Wrapper/DelegatingWrapper/WrapperProcessor`——递归解包（Spring 只有 `DecoratingProxy` 只读标记）
- `Deprecation`（279 行）——结构化弃用元数据（since/replacement/reason/link/level）
- `Prioritized`——~90% 重叠 Spring `Ordered`
- `MutableInteger`（351 行）——非线程安全可变 int

**状态**：[已验证实现]

---

## 六、集合工具（collection/，32 文件，~70% 重叠）

### REQ-015：Map/List/Set/Queue 全套工厂 + 容器适配器

**产出**：
- `MapUtils`（1446 行）：`flattenMap/nestedMap`（属性展平↔嵌套，Sp的ring 无）+ `newFixedHashMap(loadFactor=1.00)`
- `CollectionUtils`——`first/toIterable/singleton` 等方法集与 Spring 完全不同
- `ListUtils/SetUtils/QueueUtils`——`newFixedHashSet/newFixedLinkedHashSet` + `of()` 工厂
- `Lists/Sets/Maps`（3 个）——MethodHandle 反射调 JDK9+ `List.of()`，JDK 兼容层
- 22 个容器适配器：`EmptyDeque/SingletonDeque/ReversedDeque/ArrayStack`

**状态**：[已验证实现]

---

## 七、IO/事件/过滤器（46 文件，~50% 重叠）

### REQ-016：序列化 SPI + 文件监听

**产出**：
- `Serializer/Deserializer` SPI（8 文件）——类型化注册+优先级查找。Spring 只有静态 `SerializationUtils`，无 SPI 体系
- `StandardFileWatchService`（340 行）——通用文件监听服务（Spring core 无）
- `IOUtils`（549 行）——`readLines`，与 Spring `StreamUtils` ~70% 重叠

**状态**：[已验证实现]

### REQ-017：事件分发 + 过滤器组合

**产出**：
- `Event/EventListener/EventDispatcher`（10 文件）——结构等同 Spring 但 4 项独特机制：`Prioritized` 排序、SPI 自动加载监听器、`FilterOperator` AND/OR/XOR 组合、`ConditionalEventListener` 条件过滤
- filter/ 包 9 文件——Spring 完全无

**状态**：[已验证实现]

---

## 八、基础设施工具

### REQ-018：增强工具类群（util/management/ 等）

**独特能力（Spring 无等价）**：
- `ClassLoaderUtils`、`ArrayUtils`、`Version/Compatible`、`ShutdownHookUtils`、`TypeFinder`、`MethodHandleUtils`（371 行，反射 Lookup 私有构造器）、`ServiceLoaderUtils`（SPI 优先级排序）、`JmxUtils`（9 类 MXBean 全量获取+@DescriptorKey）、`BeanUtils`（递归 resolvePropertiesAsMap）

**日志 SPI**：`LoggerFactory`——ServiceLoader 自发现→Prioritized 排序→isAvailable() 过滤→SLF4J→JDK→JCL→NoOp 降级链。Spring `spring-jcl` 是硬编码选择。

**JSON 免依赖方案**：`JSONUtils`（1480 行）——`readValueAsBean`/`writeValueAsString`

**状态**：[已验证实现]

---

## 九、编译期工具链

### REQ-019：lang-model——编译期注解处理工具库

**产出**：16 个类操作 `javax.lang.model` 编译期元素。**核心类** `MethodUtils`（1165 行）——`getOverrideMethod()` 调用 `Elements.overrides()` 做编译期覆写检测、`findMethod` 按 Type/类型名匹配——这是反射版 `MethodUtils` 做不了的。Spring 没有任何 javax.lang.model 工具类。

**状态**：[已验证实现]

### REQ-020：@ConfigurationProperty 元数据生成管线

**产出**：`@ConfigurationProperty` 注解 + `ConfigurationPropertyAnnotationProcessor`（生成 `META-INF/microsphere/configuration-properties.json`）+ 元数据 SPI（Reader/Loader/Generator）。与 Spring Boot configuration-processor 同定位，实现不同。

**状态**：[已验证实现]

---

## 十、已知缺陷

| # | 缺陷 | 位置 |
|---|------|------|
| D01 | `misc/UnsafeUtils` 全 1174 行注释死代码——JEP 260 后弃用 | misc/UnsafeUtils.java |
| D02 | `StackTraceUtils` typo（Statck→Stack） | util/StackTraceUtils.java |
| D03 | `Serializers.typedSerializers` 普通 HashMap 并发 put 数据竞争 | io/Serializers.java |
| D04 | `AbstractEventDispatcher.dispatch` 并行模式所有 listener 共享一个任务（非每 listener 独立） | event/AbstractEventDispatcher.java |
| D05 | `StandardFileWatchService.stop()` 自旋等 `started.compareAndSet`——cancel(true) 后可能继续循环 | io/StandardFileWatchService.java |
| D06 | 测试覆盖缺口：8 个 `@Disabled` 永久跳过测试（含 classloading/management/performance 测试），`ModifierTest` 有 3 处 TODO 断言（BRIDGE/VARARGS/SYNTHETIC 修饰符位未验证） | test/ 目录 |

### 辅助模块：microsphere-java-test（19 文件）

共享测试库——提供测试模型（`Model/Parent/Ancestor/Color/PrimitiveTypeModel` 等）、测试服务接口（`TestService/GenericTestService`）、注解处理测试基础设施（`AbstractAnnotationProcessingTest/CompilerInvocationInterceptor`）。非生产代码，不纳入 REQ。

---

## 十一、发散需求

### REQ-N01：类路径冲突可视化报告

基于 `ClassDataRepository` 三维索引 + `ArtifactDetector` artifact 定位 → 生成"相同全限定类名在哪些 jar、哪个在前、哪个生效"的冲突报告。

### REQ-N02：MethodHandle 调用路径优化器

对同一方法测试反射/MethodHandle/invokeExact 三种调用路径吞吐量 → 返回最优路径 + 自动缓存。

### REQ-N03：类加载器层级拓扑可视化

遍历 `getLoadedClasses` + `getParent` 链 → 构建树形 ClassLoader 拓扑图。

### REQ-N04：编译期 API 版本兼容性检查器

基于 `lang-model` + `@Since` 注解 → 扫描所有 API 调用 → 对比目标基线 JDK 版本 → 生成不兼容 API 清单。

### REQ-N05：闭包内可变引用（Mutable* 系列扩展）

`MutableInteger` 应该扩展为 `MutableLong/MutableDouble/MutableReference`——当前只有 int 版。

---

## 十二、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v2 | 2026-08-03 | 完全重写：逐文件(342)+逐方法+Spring 6.2.17 对照。修正 v1"70%重叠"——实际 0~90% 随包剧烈变化。新增 lang-model 编译期工具链。 |
| v1 | 2026-08-02 | 初版（基于类名 grep，不准确，已被 v2 取代） |
