# 运行时依赖识别 -- 当 `NoSuchMethodError` 出现时，怎么知道冲突的是哪个 jar

> 主题：依赖冲突诊断为什么是个永恒难题，「构建期分析」和「运行时扫描」两条路各自的取舍，jar 元数据为什么有可靠性层级，SPI 责任链作为降级链的普适设计。microsphere 的 `ArtifactDetector` 是运行时扫描路线的一个实例。
> 关联：本文是 microsphere-java 系列的开篇，SPI 责任链 + Prioritized 排序（[§4](./04-spi-prioritized.md)）是其元机制，运行时识别为运行时隔离（[§2](./02-banned-artifact-isolation.md)）提供前置能力。

---

## 一、依赖冲突诊断:一个「构建期 vs 运行时」的永恒分野

### 1.1 `NoSuchMethodError` 的根因

```java
java.lang.NoSuchMethodError: com.google.common.base.Preconditions.checkArgument(...)
```

这个运行时异常的根因几乎总是同一个：**classpath 上有同一个库的多个版本，JVM 加载了旧版本，但调用方期望的是新版本的方法**。guava 18.0 和 guava 30.0 同时在 classpath 上，JVM 先碰到了 18.0 的 `Preconditions`，调用了 30.0 才有的 `checkArgument` 重载--崩。

诊断这个问题的第一步是回答：**classpath 上到底有哪些 jar？每个 jar 是哪个 Maven 依赖（groupId:artifactId:version）引入的？**

### 1.2 两条诊断路径

这个问题有两种根本不同的解法，代表软件工程里一个永恒的分野--**构建期分析 vs 运行时扫描**：

| 维度 | 构建期分析 | 运行时扫描 |
|---|---|---|
| 时机 | `mvn compile` / `mvn dependency:tree` 时 | JVM 启动后，扫描实际 classpath |
| 信息源 | pom.xml + 依赖解析树 | classpath 上的实际 jar 文件 |
| 准确性 | 准确（Maven 解析传递依赖） | 反映实际运行状态 |
| 过期风险 | 高（运维可能手动替换 jar、动态加载插件） | 零（扫的就是当前状态） |
| 传递依赖 | 有（A->B->C 关系完整） | 无（只知道 classpath 上有谁，不知道谁引入的） |
| 运行时开销 | 零（构建期完成） | 有（启动时扫描 jar） |

**为什么两者不能互相替代**：

- 构建期分析的致命弱点是**过期**。`mvn dependency:tree` 生成的依赖树说 classpath 上应该是 guava:30.0，但运维手动把 `guava-18.0.jar` 扔进了 `lib/` 目录，或者某个插件动态加载了旧版本--构建期文件不知道。运行时崩了，你看构建期文件说「没问题」，但实际 classpath 上有冲突。
- 运行时扫描的致命弱点是**无传递依赖信息**。它告诉你 classpath 上有 guava:18.0 和 guava:30.0，但不告诉你「18.0 是哪个依赖传递引入的」。要排查根因还得回去看构建期依赖树。

**结论**：两者互补。构建期分析回答「应该有什么」，运行时扫描回答「实际有什么」。当两者不一致时，就是冲突的来源。microsphere 的 `ArtifactDetector` 走的是运行时扫描路线--本文讲透它背后的三个永恒原理。

### 1.3 为什么运行时识别这么难

你可能会想：`ClassLoader` 不就知道 classpath 上有哪些 jar 吗？直接问它不就行了？

**JDK 给你 jar 路径，但不给 Maven 坐标**。`ProtectionDomain.getCodeSource().getLocation()` 告诉你类来自 `file:///lib/spring-core-5.3.21.jar`--这是物理位置。但依赖冲突排查需要的是 `org.springframework:spring-core:5.3.21`--这是 Maven 坐标（GAV: groupId:artifactId:version）。

JDK 不知道 Maven 坐标，因为 **JDK 不关心构建工具**。jar 文件对 JDK 来说就是一个 zip 包，里面有什么类就加载什么。Maven/Gradle/Ant 是构建工具的事，JDK 不管。所以从 jar 路径到 Maven 坐标的映射，必须自己想办法--这就是运行时依赖识别的核心难题。


## 二、永恒原理一:jar 元数据的「可靠性层级」

既然 JDK 不给 Maven 坐标，就得从 jar 文件本身找。jar 文件里可能藏着坐标信息的元数据有三个来源，但它们的**可靠性差异巨大**--这就是运行时依赖识别的第一个永恒原理。

### 2.1 三个元数据源及其可靠性

**源一:`META-INF/maven/{groupId}/{artifactId}/pom.properties`（Maven 生成）**

Maven 打包时自动在 jar 内生成这个文件，内容是 GAV 坐标：

```
spring-core-5.3.21.jar
└── META-INF/
    └── maven/
        └── org.springframework/       ← groupId（用 / 替换 .）
            └── spring-core/           ← artifactId
                └── pom.properties
                    groupId=org.springframework
                    artifactId=spring-core
                    version=5.3.21
```

**可靠性：最高**。这是 Maven 官方机制，GAV 直接写在文件里，不需要推断。**覆盖率：仅 Maven 打包的 jar**。Gradle（不用 module metadata 时）、Ant、手动打的 jar 没有这个文件。

**源二:`META-INF/MANIFEST.MF` 的 `Implementation-Title`/`Implementation-Version`（jar 规范）**

jar 规范允许在 `MANIFEST.MF` 里声明实现信息：

```
Manifest-Version: 1.0
Implementation-Title: spring-core
Implementation-Version: 5.3.21
```

**可靠性：中**。这两个字段是 jar 规范的可选项--很多 jar 不设置。且字段语义不严格（`Implementation-Title` 可能是 `Spring Framework Core` 而非 `spring-core`）。**覆盖率：部分 jar**。规范要求但不强制，实际覆盖率取决于打包者的自觉。

**源三:jar 文件名推断（约定）**

`spring-core-5.3.21.jar` -> artifactId=`spring-core`, version=`5.3.21`。

**可靠性：最低**。文件名可以被随意重命名（`my-spring.jar`），且命名约定不统一（`spring-core-5.3.21.jar` vs `spring.core-5.3.21.jar`）。**覆盖率：几乎所有 jar**（都有文件名），但信息不可靠。

### 2.2 可靠性层级的意义

三个源形成了一个**可靠性递减、覆盖率递增**的层级：

```
pom.properties   最准但覆盖窄(Maven only)
     ↓ 降级
MANIFEST.MF      中等且覆盖部分(取决于打包者)
     ↓ 降级
文件名推断        最不准但覆盖广(几乎所有 jar)
```

**这是个普适的数据质量规律**：越准确的数据源覆盖率越窄（因为生成它需要满足更多条件），越宽泛的数据源越不准（因为依赖约定而非事实）。在「数据质量梯度」面前，没有单一数据源能同时满足准确和全覆盖--**必须组合使用**。

### 2.3 为什么不能只用最准的

「pom.properties 最准，只用它不就行了？」--不行。因为不是所有 jar 都有 pom.properties：

- **JDK 核心 jar**（rt.jar、jmods）--不是 Maven 打包，没有 pom.properties。
- **Gradle 打包的 jar**（不用 module metadata 时）--Gradle 不生成 pom.properties。
- **Ant 打包的 jar**--Ant 不管 Maven 元数据。
- **手动添加的 jar**--开发者直接 `cp mylib.jar lib/`，没有任何元数据。

如果只用 pom.properties，这些 jar 全部「无法识别」--返回 null。在依赖冲突排查时，**一个「无法识别」的 jar 可能正是冲突源**（比如手动扔进去的旧版本）。所以必须降级到 manifest、再降级到文件名，**宁可拿到不可靠的信息，也比一无所知好**--这就是降级链存在的根本理由。

### 2.4 边界:每个源的失效场景

**pom.properties 失效**：

- Maven 打包中断导致半写文件--`properties.load()` 抛 `IOException`，这个源失效。
- 文件存在但内容错误（手动改过）--GAV 不准，但不会报错，静默返回错误坐标。
- groupId 含特殊字符（罕见）--路径推断可能出错。

**MANIFEST.MF 失效**：

- jar 没设 `Implementation-Title`/`Implementation-Version`--这两个字段是可选的，很多 jar 不设。
- 字段值不是标准 artifactId（如 `Spring Framework Core` 而非 `spring-core`）--拿到了信息但和 Maven 坐标不匹配。

**文件名推断失效**：

- 文件名被重命名（`my-spring.jar`）--完全推断不出。
- 版本号格式非标准（`spring-core-RELEASE.jar`）--版本解析失败。
- 多个版本号（`spring-core-5.3.21-jdk11.jar`）--不知道哪个是版本。

**这些失效场景是降级链的动力**：每个源都可能失效，所以需要「失效就降级到下一个源」的机制。


## 三、永恒原理二:SPI 责任链作为「降级链」

多个可靠性不同的数据源怎么组合使用？答案是**降级链**--按可靠性从高到低依次尝试，第一个成功的胜出。这是 microsphere 用 SPI 责任链实现的模式。

### 3.1 降级链的普适模式

降级链不是 microsphere 的发明，是个普适设计模式。它的结构：

```
[最准源] -> 失败? -> [次准源] -> 失败? -> [最不准源] -> 失败? -> null
```

**关键约束**：链的顺序 = 质量顺序（最好的先试）。一旦更好的源成功，就不需要尝试更差的--因为更差的源不可能比更好的源更准。

**同构案例**：

| 场景 | 降级链 | 质量排序 |
|---|---|---|
| 依赖识别（microsphere） | pom.properties -> manifest -> 文件名 | 准确性递减 |
| 配置加载（Spring Boot） | 命令行参数 -> 系统属性 -> 环境变量 -> application.yml | 优先级递减 |
| DNS 解析 | 本地 hosts -> 本地 DNS 缓存 -> 递归 DNS 服务器 | 延迟递增 |
| 缓存（多级缓存） | L1 cache -> L2 cache -> L3 cache -> 内存 -> 磁盘 | 速度递减 |
| 异常处理 | 具体异常 -> 父类异常 -> Throwable | 精确度递减 |

**共同本质**：多个数据源/处理策略按质量排序，最好的先试，失败就降级。这是「**质量梯度调度**」--不是随机尝试，而是有意识地从高质量到低质量。

### 3.2 为什么用 SPI 责任链而不是 if-else

降级链最简单的实现是 if-else：

```java
Artifact artifact = resolveFromPomProperties(jar);
if (artifact == null) {
    artifact = resolveFromManifest(jar);
}
if (artifact == null) {
    artifact = resolveFromFilename(jar);
}
```

**为什么不用 if-else？** 因为 if-else 违反**开闭原则**--新增一种元数据源（如 Gradle module metadata）要改 `ArtifactDetector` 源码加一个 if 分支。SPI 责任链让每个 Resolver 独立实现接口、在 `META-INF/services/` 声明，框架自动发现并排序--新增源零改源码。

**SPI 责任链的执行逻辑**：

```java
// ArtifactDetector.detect(URL) 的核心逻辑
for (ArtifactResourceResolver resolver : artifactResourceResolvers) {
    Artifact artifact = resolver.resolve(classPathURL);
    if (artifact != null) {
        break;  // 第一个成功的胜出,不再继续
    }
}
```

`artifactResourceResolvers` 是通过 SPI 加载的所有 Resolver 实现，按 `Prioritized` 排序。第一个返回非 null 的胜出，后面的不再执行。

### 3.3 Prioritized 排序:链顺序 = 质量顺序

降级链的关键约束是「最好的先试」。microsphere 用 `Prioritized` 接口让每个 Resolver 声明自己的优先级，`ServiceLoaderUtils` 按 `Prioritized.COMPARATOR`（升序，数值小=排前=先试）排序：

```
MavenArtifactResourceResolver    priority=1  (最准,数值最小,最先试)
ManifestArtifactResourceResolver priority=5
ArchiveFileArtifactResourceResolver priority=9 (最不准,数值最大,最后兜底)
```

**注意命名陷阱**（[§4](./04-spi-prioritized.md) 已勘误）：`Prioritized` 的 `MAX_PRIORITY = MIN_VALUE`（最高优先级 = 最小数值 = 排最前）。这里的 priority 值（1/5/9）都大于 `NORMAL_PRIORITY`(0)，意味着内置 Resolver 的优先级低于默认--这是「用户自定义 Resolver 优先于内置」的设计：用户注册一个 priority=0 的自定义 Resolver，会排在所有内置 Resolver 之前先被尝试。

**为什么 Maven=1 而非 0？** 因为 0 是 `NORMAL_PRIORITY`（默认值）。如果 Maven=0，用户注册一个默认优先级的 Resolver 就和 Maven 同级--排序不确定（稳定排序保持声明顺序，但声明顺序不可靠）。Maven=1 确保内置 Resolver 全部 > 0，给用户的 0 留出「优先于所有内置」的空间。**这是「用户优先于框架」的优先级哲学**。

### 3.4 降级链的边界:静默跳过 vs 失败中断

降级链有个设计决策：**某个 Resolver 抛异常时，是中断整个链还是静默跳过继续？**

microsphere 的选择是**静默跳过**：`StreamArtifactResourceResolver` 的父类 `resolve()` 方法捕获 `IOException`（如 pom.properties 损坏）并返回 null，责任链继续到下一个 Resolver。

**静默跳过的好处**：一个损坏的 pom.properties 不会导致整个 jar 被标记为「无法识别」--manifest 可能还能提供信息。

**静默跳过的坏处**：如果 jar 权限拒绝（`new JarFile(path)` 抛 `IOException`），`ClassUtils` 捕获并返回空集合--这个 jar **静默不出现在 artifact 列表中**。如果它就是冲突版本，排查会变得困难（你以为 classpath 上没有它，实际是扫描被权限挡了）。

**这是降级链的固有张力**：容错（静默跳过）vs 可观测性（失败要可见）。microsphere 选了容错，代价是排查时可能有「幽灵 jar」。生产环境应该加日志--Resolver 失败时打 warn 级日志，让「静默跳过」至少有迹可循。


## 四、永恒原理三:classpath 的「惰性初始化」与反射读取

ArtifactDetector 要扫描 classpath 上的所有 jar，但「classpath 上的所有 jar」怎么拿到？这涉及 JDK `URLClassPath` 的内部结构--第三个永恒原理。

### 4.1 JDK 给的两个入口及其局限

**入口一:`URLClassLoader.getURLs()`**

返回这个 ClassLoader 自己管理的 URL 数组。**局限**：只返回「当前 CL」的 URL，不返回父 CL 的。Bootstrap ClassLoader 加载的 `rt.jar` 不在其中。

**入口二:`System.getProperty("sun.boot.class.path")`**

返回 Bootstrap classpath（JDK 核心 jar 的路径）。**局限**：这是 Sun 私有属性，JDK 9+ 模块化后不再可用（模块化后 JDK 类在 `jmods/` 中，不在 jar 中）。

microsphere 组合两者：`findAllClassPathURLs` 从 `URLClassLoader.getURLs()` + Bootstrap classpath 收集所有 URL，然后用 `removeJdkClassPathURLs` 过滤掉 JDK 核心 jar（因为它们不是 Maven 依赖，不需要参与冲突排查）。

### 4.2 JDK 类库过滤的原理

```java
private void removeJdkClassPathURLs(Set<URL> classPathURLs) {
    Set<String> bootstrapClassPaths = getBootstrapClassPaths();
    Iterator<URL> it = classPathURLs.iterator();
    while (it.hasNext()) {
        URL url = it.next();
        if (bootstrapClassPaths.contains(url.getPath()) || url.getPath().contains(JAVA_HOME)) {
            it.remove();
        }
    }
}
```

**两个过滤条件**：

1. URL 路径在 Bootstrap classpath 列表中--直接匹配。
2. URL 路径包含 `JAVA_HOME`--路径匹配（兜底）。

**为什么需要第二个条件？** JDK 11+ 上 `rt.jar` 不存在了，Bootstrap classpath 属性可能不可用。但 JDK 类库仍在 `JAVA_HOME` 目录下（`jmods/` 或 `lib/`），路径包含 `JAVA_HOME` 就能过滤。**这是跨 JDK 版本的兼容性兜底**。

### 4.3 性能考量:无缓存的有意设计

ArtifactDetector 每次调用 `detect()` 都重新扫描所有 jar--没有缓存。一千个 jar 的项目每次扫描约 200-500ms。

**为什么有意不缓存？**

- **缓存失效复杂**：classpath 可能在运行时变化（动态加载插件、OSGi 热部署），缓存需要失效机制。JDK 没有「classpath 变更通知」，只能用 TTL 或手动刷新--都不可靠。
- **调用频率低**：ArtifactDetector 只在启动时调用一次（`BannedArtifactClassLoadingListener.starting()`），之后不再需要。缓存的代价（内存占用 + 失效逻辑）高于单次扫描的代价。
- **准确性优先**：无缓存意味着每次都反映当前 classpath 状态--这是运行时扫描的核心价值（§1.2）。缓存可能返回过期结果，违背「反映实际运行状态」的初衷。

**这是「准确性 vs 性能」的经典权衡**。microsphere 选准确性（无缓存、每次扫描），代价是 200-500ms 启动开销。如果你确认运行时 classpath 不变，可以加缓存提升性能--但要自己承担过期风险。


## 五、microsphere 作为「运行时依赖识别」的一个实例

讲完三个原理，microsphere 的 `ArtifactDetector` 就是这些原理的一次落地。

**实例一:可靠性层级 + 降级链（§2 + §3 原理的落地）**

四个 `ArtifactResourceResolver` 通过 SPI 加载，按 `Prioritized` 排序（Maven=1 -> Manifest=5 -> ArchiveFile=9）。`detect(URL)` 遍历责任链，第一个返回非 null 的胜出。每个 Resolver 对应一个可靠性层级：Maven 读 pom.properties（最准）、Manifest 读 MANIFEST.MF（中等）、ArchiveFile 从文件名推断（最不准）。降级链保证「最准的先试，失效就降级」。

**实例二:classpath 收集 + JDK 过滤（§4 原理的落地）**

`getClassPathURLs(boolean includedJdkLibraries)` 从 `URLClassLoader.getURLs()` + Bootstrap classpath 收集所有 URL，`removeJdkClassPathURLs` 用 Bootstrap 路径匹配 + `JAVA_HOME` 路径匹配双重过滤 JDK 核心 jar。`includedJdkLibraries=false` 时 JDK jar 不参与扫描（它们不是 Maven 依赖）。

**实例三:无缓存的有意设计（§4.3 原理的落地）**

每次 `detect()` 都全量扫描，不缓存结果。`BannedArtifactClassLoadingListener.starting()` 调一次，之后不再调。200-500ms 启动开销换取「结果准确反映当前 classpath」。

**生产案例:guava 多版本冲突排查**

```java
ArtifactDetector detector = new ArtifactDetector(currentCL);
List<Artifact> artifacts = detector.detect(false);  // 排除 JDK

// 筛选所有 guava
artifacts.stream()
    .filter(a -> a.getArtifactId().contains("guava"))
    .forEach(a -> System.out.println(a));

// 输出:
//   Artifact{artifactId='guava', version='18.0', location='file:///lib/guava-18.0.jar'}
//   Artifact{artifactId='guava', version='30.0-jre', location='file:///app/guava-30.0-jre.jar'}
// -> 两个版本冲突,旧版 18.0 需要被排除
```

后续由 `BannedArtifactClassLoadingListener` 自动移除冲突版本（详见 [§2](./02-banned-artifact-isolation.md)）。


## 六、实例批判:这个实现的缺陷

作为原理的一个落地实例，microsphere 的实现也有瑕疵。

1. **静默跳过不可观测**（§3.4）：jar 权限拒绝或 pom.properties 损坏时静默跳过，无日志。冲突版本的 jar 可能「幽灵般」不出现在列表中。应加 warn 级日志。
2. **Gradle module metadata 不支持**：Gradle 打包的 jar 没有 pom.properties，降级到 manifest 或文件名--但 Gradle 有自己的 `META-INF/gradle/.../module-metadata.json`。microsphere 没有对应的 Resolver，说明它的 Maven 生态假设。用户可自己注册 `GradleArtifactResourceResolver` 补上。
3. **JDK 11+ 的 jmods 不被识别**：`removeJdkClassPathURLs` 用 `JAVA_HOME` 路径匹配过滤 JDK jar，但 JDK 11+ 的模块在 `jmods/` 目录--如果应用把 jmods 路径加进了 classpath（罕见但可能），过滤可能失效。
4. **无传递依赖信息**：ArtifactDetector 只知道 classpath 上有 guava:18.0，不知道它是被哪个依赖传递引入的。要排查根因还得看 `mvn dependency:tree`。这是运行时扫描路线的固有局限（§1.2），不是实现缺陷。

这些不是原理错误，而是原理在具体代码里的实现瑕疵或固有局限。


## 七、与其他方案的原理对比

| 方案 | 路线 | 信息来源 | 准确性 | 运行时开销 | 传递依赖 | 覆盖率 |
|---|---|---|---|---|---|---|
| ArtifactDetector (Maven) | 运行时扫描 | pom.properties | 最准 | 200-500ms | 无 | Maven jar |
| ArtifactDetector (降级链) | 运行时扫描 | MANIFEST + 文件名 | 中 | 同上 | 无 | 几乎所有 jar |
| `mvn dependency:tree` | 构建期分析 | pom.xml + 解析树 | 准但可能过期 | 零 | 有 | Maven 项目 |
| Gradle Module Metadata | 运行时扫描 | module-metadata.json | 准 | 同上 | 无 | Gradle jar |
| JDK 9 ModuleDescriptor | 运行时读取 | module-info.class | 准 | 零 | 无 | 模块化 jar（极少） |
| Maven Enforcer Plugin | 构建期分析 | pom.xml + 规则 | 准 | 零 | 有 | Maven 项目 |

**原理层面的取舍**：

- **构建期分析（mvn dependency:tree / Enforcer）** 赢在零运行时开销和传递依赖信息，输在可能过期。适合「构建期就能确定的冲突」。
- **运行时扫描（ArtifactDetector）** 赢在反映实际状态，输在开销和无传递依赖。适合「运维改过 jar / 动态加载插件」导致的冲突。
- **JDK 9 ModuleDescriptor** 是「官方未来方向」--模块化 jar 自带版本信息，零开销读取。但当前第三方 jar 极少模块化，覆盖率太低。如果未来 jar 普遍模块化，这条路线会取代 pom.properties。
- **Gradle Module Metadata** 和 pom.properties 是平行的--Gradle jar 用 Gradle metadata，Maven jar 用 pom.properties。microsphere 只支持后者，说明它的生态假设。

microsphere 的选择是「运行时扫描 + Maven 优先 + 降级链兜底」--运行时准确但有一次扫描开销。如果你在生产环境需要零启动开销，`mvn dependency:tree` 的构建期方案更合适。


## 八、面试要点

**Q1：「`NoSuchMethodError` 出现时，怎么排查是哪个 jar 冲突？」**

答案：两种路线。① 构建期分析：`mvn dependency:tree` 看依赖树，找同一库的多个版本。优点是有传递依赖信息（知道谁引入的），缺点是可能过期（运维改过 jar 就不准了）。② 运行时扫描：扫描实际 classpath 上每个 jar 的 Maven 坐标，找同一库的多个版本。优点是反映实际状态，缺点是无传递依赖信息且有扫描开销（200-500ms）。两者互补--构建期说「应该有什么」，运行时说「实际有什么」，不一致就是冲突源。microsphere 的 ArtifactDetector 走运行时扫描路线。

**Q2：「运行时怎么知道一个 jar 的 Maven 坐标？JDK 不是不提供这个信息吗？」**

答案：JDK 给 jar 路径（`ProtectionDomain.getCodeSource().getLocation()`）但不给 Maven 坐标--JDK 不关心构建工具。坐标信息藏在 jar 内的元数据里，有三个源，可靠性递减：① `META-INF/maven/{groupId}/{artifactId}/pom.properties`（Maven 生成，最准但仅 Maven jar 有）。② `META-INF/MANIFEST.MF` 的 `Implementation-Title`/`Implementation-Version`（jar 规范可选字段，中等但部分 jar 不设）。③ 文件名推断（最不准但几乎所有 jar 都有）。这是「数据质量梯度」--越准的源覆盖越窄，必须组合使用。

**Q3：「多个可靠性不同的元数据源怎么组合？为什么用 SPI 责任链而非 if-else？」**

答案：降级链--按可靠性从高到低依次尝试，第一个成功的胜出。链顺序 = 质量顺序（最好的先试），因为更差的源不可能比更好的源更准。用 SPI 责任链而非 if-else 的原因是开闭原则--if-else 新增源要改源码，SPI 链条让每个 Resolver 独立实现接口并在 `META-INF/services/` 声明，框架自动发现并按 `Prioritized` 排序。新增 Gradle module metadata 支持只需新增一个 Resolver + SPI 声明，零改源码。降级链是普适模式--配置加载（命令行->系统属性->yml）、DNS 解析（hosts->缓存->递归）、多级缓存都是同构。

**Q4：「降级链中某个 Resolver 抛异常怎么办？中断还是跳过？」**

答案：microsphere 选静默跳过--`StreamArtifactResourceResolver` 的父类捕获 `IOException` 返回 null，责任链继续到下一个 Resolver。好处是容错（一个损坏的 pom.properties 不会让整个 jar 无法识别）。坏处是不可观测（jar 权限拒绝时静默不出现在列表中，冲突版本的 jar 可能「幽灵般」消失）。这是「容错 vs 可观测性」的固有张力。生产环境应该加 warn 级日志，让静默跳过有迹可循。这个选择和 Spring Boot 配置加载的「fail-fast vs ignore」是同类决策。

**Q5：「ArtifactDetector 为什么不缓存扫描结果？每次都扫描不慢吗？」**

答案：有意不缓存，三个原因。① 缓存失效复杂--classpath 可能运行时变化（动态加载插件），JDK 没有 classpath 变更通知，TTL 或手动刷新都不可靠。② 调用频率低--只在启动时调一次（`BannedArtifactClassLoadingListener.starting()`），之后不再需要，缓存的内存代价高于单次扫描的时间代价。③ 准确性优先--无缓存每次反映当前 classpath，这是运行时扫描的核心价值（反映实际状态）。这是「准确性 vs 性能」的经典权衡。200-500ms 启动开销换取准确性，在生产环境是可接受的。

**Q6：「运行时扫描和构建期分析（mvn dependency:tree）各有什么局限？为什么需要两者？」**

答案：运行时扫描的局限是无传递依赖信息--知道 classpath 上有 guava:18.0 但不知道谁引入的。构建期分析的局限是可能过期--运维手动替换 jar 或动态加载插件后，构建期文件不反映实际状态。两者互补：构建期说「应该有什么 + 谁引入的」，运行时说「实际有什么」。当两者不一致时（构建期说 guava:30.0，运行时发现有 18.0），就是冲突源。JDK 9 ModuleDescriptor 是未来方向（模块化 jar 自带版本，零开销读取），但当前第三方 jar 极少模块化，覆盖率太低。

---

> **与 SPI+Prioritized 的关联**：ArtifactDetector 的 Resolver 链是 [§4 SPI+Prioritized](./04-spi-prioritized.md) 的直接应用--SPI 加载 + Prioritized 排序 + 责任链。注意 `MAX_PRIORITY = MIN_VALUE`（最高优先级 = 最小数值 = 排最前），Maven=1/Manifest=5/ArchiveFile=9 是「数值小=先试=质量高」。
> **与 BannedArtifact 的关联**：ArtifactDetector 的识别结果为 [§2 BannedArtifact](./02-banned-artifact-isolation.md) 的运行时隔离提供前置--先识别「classpath 上有什么」，再决定「移除什么」。
