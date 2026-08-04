# 01-REQ：confucius-commons 完整需求规格（v3，源码已验证）

> **v3 重写说明**：v1/v2 基于未读源码的猜测。v3 逐文件（38 主文件 + 4 JVM Attach 文件）+ 逐方法 + 对照 microsphere-java-core（后代）验证后重写。
>
> 需求分三类：
> 1. **已实现需求**（REQ-001~006，6 项）
> 2. **待修复**（REQ-D01~D11，11 项）——已知 bug
> 3. **全新发散**（REQ-N01~N04，4 项）——基于 confucius 已有的独特能力发散
>
> **基准**：JDK 6（pom `maven.compiler.source/target=1.6`），SUN JDK 6+

---

## 项目定位

**confucius-commons 是 mercyblitz/小马哥 最早的 JDK 底层工具库（JDK 6 时代，~2012 年），是 microsphere-java-core（02）的前身。** 项目做的是 JDK 不提供的底层能力：通过 `sun.reflect.Reflection` 获取调用者类、用 `sun.misc.Unsafe` 做类型安全的字段操作、用 `com.sun.tools.attach.VirtualMachine` 附着到另一个 JVM 进程、用 `java.util.prefs.WindowsPreferences` 反射访问 Windows 注册表。

**与 microsphere-java-core（02）的关系：前身/后代。** confucius 的 `org.confucius.commons` 包在 microsphere 中被重命名为 `io.microsphere`，多数能力被扩展继承（ClassLoaderUtils 从 573 行→2138 行，ReflectionUtils 从 2 层→3 层 caller 检测，ProcessExecutor 从 `runtime.exec`→`ProcessBuilder`）。但 **4 项独特能力在 microsphere 中完全丢失**：Unsafe 完整实现（microsphere 是死代码）、JVM Attach、WindowsRegistry、编码感知 ResourceBundle。

**源码信息**：`/data/workspace/confucius-commons/`，`org.confucius.commons.*` 包，38 主文件 + 4 JVM Attach 文件。同一作者 mercyblitz。

---

## 一、JDK 内部反射与类加载

### REQ-001：ClassLoader 已加载类型查询

**问题**：JDK 6 没有 `ClassLoader.getDefinedPackages()`（JDK 9 才加），你想知道"当前 JVM 里加载了哪些类的 Class 对象？哪些类名？"——没有标准 API。

**产出**：`ClassLoaderUtils`（573 行）——反射读取 `ClassLoader.classes` 私有字段（`FieldUtils.getFieldValue(classLoader, "classes")`）→ 返回 `List<Class<?>>`；`findLoadedClass(String name)` 遍历 classloader 父链；`findLoadedClassesInClassPath(String classPath)` 按类路径（如 `WEB-INF/classes`）过滤。

**状态**：[已验证实现]

**跨版本演进**：microsphere 版（2138 行）继承并大扩展——新增 `getDefaultClassLoader/getCallerClassLoader/isPresent/resolveClass/findAllClassPathURLs/removeClassPathURL/URLClassLoader 管理`。

---

### REQ-002：类型分类与 classpath 扫描

**产出**：
- `ClassUtils`（322 行）——`getClassNamesInClassPath/InPackage` 从 classpath 目录读取文件名（`.class`→类名）。**静态初始化全量扫描**建 3 张 Map（classPath→classNames, className→classPath, packageName→classNames）——启动时一次性索引。
- `ClassPathUtils`（131 行）——`getBootstrapClassPaths()` 从 `RuntimeMXBean.getBootClassPath()` 获取；`getRuntimeClassLocation(Class)` 通过 `ProtectionDomain.getCodeSource().getLocation()` 获取。

**状态**：[已验证实现]

**跨版本演进**：microsphere 的 `ClassDataRepository`（lang/）同功能但**移除了静态全量扫描**（改进——大 classpath 下启动更安全），改用按需索引。

---

## 二、反射与 Unsafe

### REQ-003：调用者类信息获取

**问题**：JDK 6 时代 `sun.reflect.Reflection.getCallerClass(int)` 是唯一能获取"谁调了我"的 API——但它不是标准 API。

**产出**：`ReflectionUtils`（450 行）——**2 层 caller class 检测**：`sun.reflect.Reflection#getCallerClass(int)`（通过 `Method.invoke` 反射调用）→ StackTraceElement 兜底。额外提供 `getCallerPackage()`、`assertArrayIndex/assertArrayType/assertFieldMatchType`、`readFieldsAsMap`。

**状态**：[已验证实现]

**跨版本演进**：microsphere 升级为 **3 层**——MethodHandle.invokeExact + StackWalker(JDK9) + StackTraceElement。`getCallerPackage()` 在 microsphere 中被移除。

### REQ-004：sun.misc.Unsafe 类型安全包装

**问题**：`sun.misc.Unsafe` 能做 Java 语言层次不允许的任何事（直接读写内存、绕过构造器创建实例）——但它是 JDK 内部 API，用起来不安全（字段名拼错→静默错；偏移量算错→SIGSEGV）。

**产出**：`UnsafeUtils`（1384 行，**活代码**——非死代码！）——类型安全的 Unsafe 包装：`getInt/putLong/getObject(instance, fieldName)` 按字段名存取（缓存偏移量 ConcurrentMap），`sizeof(Object)` 计算对象大小。静态初始化用 `AccessController.doPrivileged` 拿 `theUnsafe` 字段。

**状态**：[已验证实现]

**跨版本演进**：microsphere 的 `misc/UnsafeUtils`（1174 行）**全文件被注释为死代码**——JEP 260 后 microsphere 弃用了 Unsafe，confucius 的 1384 行活实现是**独一无二的遗产**。JDK 9+ 后 `Unsafe` 仍然可用但不推荐——现代化替代是 `VarHandle`（JDK 9+）或 `jdk.incubator.foreign.MemorySegment`（JDK 16+）。

---

## 三、进程与 JVM 附着

### REQ-005：外部进程执行

**产出**：`ProcessExecutor`（137 行）+ `ProcessManager`（49 行）——`runtime.exec(command)` 单字符串 + StringTokenizer 切词；1 秒轮询等待 `process.waitFor()`；`ProcessManager` 单例 ConcurrentMap 跟踪未完成进程。

**状态**：[已验证实现]

**已知缺陷**：`runtime.exec(String)` 不支持含空格的引号参数、轮询 sleep 1s、实例字段 `finished` 不可复用、stdout/stderr 分流存在死锁风险（用 `available()` 轮询排水而非独立线程消费）。

**跨版本演进**：microsphere 大改为 `ProcessBuilder` + `redirectErrorStream(true)` 合并流 + 单线程 executor + `future.get(timeout)`，默认超时 30s。

### REQ-006：JVM 进程附着

**问题**：你需要在运行时附着到本地 JVM 进程并执行操作（如触发 GC、获取诊断信息）。

**产出**：`VirtualMachineTemplate`——`VirtualMachine.attach(processId)` → `callback.doInVirtualMachine(vm)` → `finally vm.detach()`。模板方法模式，调用方只需实现 `VirtualMachineCallback`。`LocalVirtualMachineTemplate` 通过 `ManagementUtils.getCurrentProcessId()` 自附着。`HotSpotVirtualMachineCallback` 依赖 `sun.tools.attach.HotSpotVirtualMachine`。

**状态**：[已验证实现]——**microsphere 完全没有继承此能力**（全仓库 grep `com.sun.tools.attach` 无命中）。

---

## 四、平台工具

### 未独立成 REQ 的能力

- `WindowsRegistry`（846 行）——反射 `java.util.prefs.WindowsPreferences` 私有 native 方法实现注册表 get/set/remove/flush。microsphere 无对应。
- `Base64`（224 行）——OpenJDK prefs 私有 Base64 复制（JDK 8 前的解决方案，已被 `java.util.Base64` 取代）。
- `PropertyResourceBundleControl`（155 行）——编码感知 ResourceBundle（JDK 6 时代 `.properties` 文件必须 ISO 8859-1 编码，此类实现 UTF-8 读取）。JDK 9+ 内置 UTF-8 properties 加载后过时。
- `JarUtils`（278 行）——JarFile 提取/过滤/解析。
- `ServiceLoaderUtils`（114 行）——SPI 加载（microsphere 扩展至 911 行）。
- Filter 体系（9 文件）——FilterOperator AND/OR/XOR + 类型过滤器（microsphere 继承并继承 bug）。
- Scanner 体系（4 文件）——类/文件/Jar 扫描（microsphere 继承）。

---

## 五、已知缺陷

| # | 缺陷 | 位置 |
|---|------|------|
| D01 | `FilterOperator.XOR` 实现反转——`success=true` 初始→结果恒取反。microsphere 测试已固化 bug 输出 | lang/filter/FilterOperator.java |
| D02 | `PackageNameClassFilter.accept` 对默认包/匿名类 `getPackage()=null`→NPE | lang/filter/PackageNameClassFilter.java |
| D03 | `URLUtils.resolveParametersMap` 值含 `=` 被截断——`split("=")` 只取 `[1]` | lang/net/URLUtils.java |
| D04 | `ManagementUtils.initGetProcessIdMethod()` 非 HotSpot JVM 上静态初始化 NPE | lang/management/ManagementUtils.java |
| D05 | `JarUtils` 多处 `new JarFile(...)` 从不 close→文件句柄泄漏；`extract(URL...` null jarEntry NPE | lang/util/jar/JarUtils.java |
| D06 | `ProcessExecutor`——`runtime.exec(String)` 无引号支持；轮询 1s；不可复用；stdout/stderr 死锁风险 | lang/process/ProcessExecutor.java |
| D07 | `WindowsRegistry.windowsAbsolutePath` 第二行 `replace` 用原始 relativePath 非 resolvedPath | util/WindowsRegistry.java |
| D08 | `ReflectionUtils.getCallerClassInSunJVM` 空 `if` 死块 | lang/reflect/ReflectionUtils.java |
| D09 | `URLUtils.isJarURL`——JarFile new 成功=flag 恒 true | lang/net/URLUtils.java |
| D10 | `FileUtils.resolveRelativePath` 非锚定 contains→`/home/user2` 误匹配 `/home/user` | lang/io/FileUtils.java |
| D11 | **测试覆盖黑洞**：`AbstractTestCase` 标记 `@Ignore`——10 个测试类通过继承被永久跳过（ClassLoaderUtils/FilterUtils/ReflectionUtils/ManagementUtils/Scanner）。FilterUtilsTest 未测 XOR。UnsafeUtilTest/ProcessExecutorTest 等未继承但为 JUnit 3 风格（`junit.framework.Assert` + `setUp()`，无 `@Test` 注解 + 无 TestCase 继承→不会被执行） | test/ 目录 |

---

## 六、发散需求

### REQ-N01：VarHandle 替代 Unsafe（JDK 9+）

基于 confucius 1384 行活 Unsafe 实现，用 JDK 9+ `VarHandle` 重写——保留字段名语义 API（`getInt(obj, "field")`），底层改为 `VarHandle`。

### REQ-N02：ProcessExecutor 重构

用 `ProcessBuilder` + `redirectErrorStream` + `ExecutorService` 重写——解决死锁、参数引号、超时。

### REQ-N03：JarUtils 安全提取

修复文件句柄泄漏 + 添加路径穿越防护（Zip-Slip）。

### REQ-N04：PropertyChangeSupport 事件桩

基于 confucius 的 `ManagementUtils`/`ProcessManager` 监控能力——为 Unsafe 操作/进程执行/JVM Attach 提供统一的事件通知机制。

---

## 七、与 microsphere-java-core（02）对照

| 能力 | confucius (01) | microsphere (02) | 关系 |
|------|:---:|:---:|------|
| ClassLoader 内省 | 573 行，`findLoadedClassesInClassPath` | 2138 行，+`removeClassPathURL/resolveClass/URLClassLoader` | 继承且大扩展 |
| caller class 检测 | 2 层（sun.reflect + StackTrace） | 3 层（+MethodHandle + StackWalker） | 升级 |
| Unsafe 包装 | **1384 行活代码** | 1174 行全注释死代码 | **能力丢失** |
| JVM Attach | **4 文件真实 attach** | 无 | **能力丢失** |
| ProcessExecutor | `runtime.exec(String)` 单字符串 | `ProcessBuilder` + `ExecutorService` | 大改 |
| WindowsRegistry | **846 行** | 无 | **能力丢失** |
| FilterOperator XOR bug | ✅ 有 | ✅ 有（测试固化） | 逐字复制 |
| URLUtils `=` 截断 | ✅ 有 | ✅ 有 | 逐字复制 |

---

## 八、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v3 | 2026-08-03 | 完全重写：逐文件(38+4)+逐方法+对照 microsphere-java-core 后代。纠正 v2"虚构"谬误——Unsafe/JVM Attach 在 confucius 是真实实现 |
| v2 | 2026-08-02 | 基于微球源码的猜测（不准确） |
| v1 | 2026-08-02 | 初版 |
