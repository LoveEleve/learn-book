# 运行时依赖隔离 -- 当冲突的 jar 已经在 classpath 上，怎么把它「摘掉」

> 主题：依赖冲突的隔离有三条根本不同的路（构建期 shade / 运行时移除 / 架构级 OSGi-Module），各自的代价是什么；为什么类加载是不可逆的（ban 必须在加载前）；反射操作 `URLClassPath` 三个集合为什么必须同步。microsphere 的 `BannedArtifact` 是运行时移除路线的一个实例。
> 关联：本文承接 [§1 运行时依赖识别](./01-artifact-detection.md) 的识别结果，执行移除；与 [§7 URL 协议扩展](./07-url-stream-handler.md) 同属「反射操作 JDK 内部」类方案，共享 JDK 16+ `--add-opens` 代价。

---

## 一、依赖隔离:三条根本不同的路

[§1](./01-artifact-detection.md) 解决了「classpath 上有哪些 jar」的识别问题。本文解决下一个问题：**发现冲突后，怎么把冲突的 jar 从 classpath 中「摘掉」？**

这个问题有三条根本不同的解法，代表三个隔离维度。

### 1.1 三条路的对比

| 路线 | 时机 | 手段 | 侵入性 | 代表 |
|---|---|---|---|---|
| 构建期隔离 | `mvn package` | 重命名包（shade）/ 排除依赖（exclude） | 中（改构建配置） | Maven Shade Plugin |
| 运行时隔离 | JVM 启动时 | 从 ClassLoader 内部移除 URL | 低（启动时一次性） | microsphere BannedArtifact |
| 架构级隔离 | 应用设计时 | 每个模块/bundle 独立 ClassLoader | 极高（架构级变更） | OSGi / JDK 9 ModuleLayer / Tomcat WebAppCL |

**没有银弹**--三条路各自解决不同层面的问题，代价也不同：

- **构建期隔离**不改变运行时机制，但只解决「你自己的 jar 用哪个版本」，不能阻止用户的 classpath 上出现另一个版本。
- **运行时隔离**能在运行时移除冲突 jar，但必须反射操作 JVM 内部，受 JDK 版本和模块系统限制。
- **架构级隔离**最彻底（ClassLoader 级隔离），但要求整个应用按 OSGi/Module 架构设计，侵入性极高。

### 1.2 Maven shade 为什么解决不了

Maven Shade Plugin 的方案是「重命名包」--把 guava 的 `com.google.common` 改成 `myapp.shaded.guava`，打进你的 jar：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <configuration>
        <relocation>
            <pattern>com.google.common</pattern>
            <shadedPattern>myapp.shaded.guava</shadedPattern>
        </relocation>
    </configuration>
</plugin>
```

**shade 解决的是「你自己的 jar 中用哪个版本」**--你的代码调用 `myapp.shaded.guava.Preconditions`，和用户的 `com.google.common.Preconditions` 是两个不同的类，不冲突。

**但 shade 阻止不了「用户的 classpath 上出现另一个版本」**。你的框架依赖 guava:30.0 并 shade 了一份，用户引入你的框架后，他项目里已有 guava:18.0（来自另一个旧依赖）。运行时 classpath 上同时存在 `guava-18.0.jar`（用户引入）和 `your-framework.jar`（内含 shade 的 guava 30.0）。**如果用户的依赖传递链中也有 shade 版的 guava，版本冲突又回来了**。

**shade 的其他代价**：jar 从 2MB 变成 3MB（多了重命名后的代码），调试时反编译看到的类名全是 `myapp.shaded.guava.Preconditions`--和线上日志对不上。

### 1.3 microsphere 为什么选运行时移除

microsphere 的选择是**运行时移除**：在框架启动时检查 classpath 上是否有 banned 版本的 artifact，如果有，从 ClassLoader 内部数据结构中移除对应的 URL--让 JVM 后续的类加载永远找不到这个 jar。

**这条路的好处**：

- **确定性**：不依赖构建配置，直接反映运行时状态。运维手动扔进 `lib/` 的旧版本 jar 也能被 ban。
- **低侵入**：启动时一次性操作，运行时零开销。不需要改构建配置，不需要改应用架构。
- **精细控制**：按 GAV 坐标 + 通配符匹配，能精确 ban 某个版本（`com.google.guava:guava:18.0`）或某个范围（`com.google.guava:guava:18.*`）。

**这条路的代价**：必须反射操作 JVM 内部（`URLClassPath` 的私有字段），受 JDK 版本和模块系统限制（JDK 16+ 需要 `--add-opens`）。这是「反射操作 JDK 内部」类方案的共同代价。

下面三个小节讲透运行时移除背后的三个永恒原理。


## 二、永恒原理一:类加载的「不可逆性」与 ban 的时序约束

运行时移除的第一个根本约束来自类加载机制本身--**类一旦被加载，就不会因为 jar 被移除而消失**。

### 2.1 类加载是不可逆的

```java
// 时序问题:
// 1. 线程A 加载了 com.google.common.base.Preconditions(来自 guava-18.0.jar)
//    -> Preconditions.class 被放进 Method Area,guava-18.0 的 Loader 被缓存
// 2. BannedArtifactClassLoadingExecutor.execute() 移除了 guava-18.0.jar 的 URL
//    -> URL 从 classpath 消失,后续 loadClass 找不到 guava-18.0
// 3. 线程A 调用的 Preconditions.checkArgument() -- 仍然是 18.0 的版本!
//    -> 已加载的类不会因 ban 消失
```

**JVM 没有「卸载类」的标准 API**。类一旦加载进 Method Area，就存在于 ClassLoader 的 `classes` 字典中，直到 ClassLoader 本身被 GC（这通常发生在 ClassLoader 卸载时，如 Tomcat webapp 重部署）。**ban 只能阻止「未来的类加载」，不能让「已加载的类」消失**。

### 2.2 ban 的时序约束:必须在类加载的最早阶段

因为类加载不可逆，ban 必须在**任何业务类被加载之前**执行。microsphere 选择在 `SpringApplicationRunListener.starting()` 阶段执行--此时：

- ApplicationContext 还没创建
- Bean 还没初始化
- 业务类还没被加载

只有 JDK 核心类和 Spring Boot 启动类被加载了。在这个阶段 ban，能保证冲突版本的业务类还没机会被加载。

**如果 ban 太晚**（比如在 Bean 初始化阶段），冲突版本的类可能已经被加载了--ban 移除了 URL 但类还在 Method Area，调用仍走旧版本。这就是「ban 了但没用」的幽灵问题。

### 2.3 这是所有运行时隔离方案的共同约束

「类加载不可逆」不是 microsphere 独有的，是 JVM 的根本机制。所有运行时隔离方案都必须面对这个时序约束：

| 方案 | ban 时机 | 能 ban 已加载的类吗 |
|---|---|---|
| microsphere BannedArtifact | `SpringApplicationRunListener.starting()` | ❌ 只能阻止未来加载 |
| Spring Boot ClassPathFilter | 启动时 | ❌ 同上 |
| 自定义 ClassLoader（Tomcat） | ClassLoader 创建时 | ✅ 新 CL 不加载被 ban 的类 |
| JDK 9 ModuleLayer | 模块层定义时 | ✅ 新模块层不包含被 ban 的模块 |

**只有「创建新 ClassLoader / 新 ModuleLayer」的架构级方案能完全规避不可逆性**--因为新 CL 从一开始就不加载被 ban 的类。运行时移除方案（microsphere）必须在「已有 CL 上操作」，受不可逆性约束。

**这个约束的普适性**：不只是 Java，任何带「运行时加载代码」能力的系统都有这个约束--Python 的 `sys.modules`、Node.js 的 `require.cache`、Go 的 plugin。一旦代码被加载到内存，移除源文件不能让内存中的代码消失。


## 三、永恒原理二:URLClassPath 的「三集合一致性」

运行时移除的第二个根本原理是 JVM `URLClassPath` 的内部数据结构--**三个集合必须同步移除**，否则 ban 不彻底。

### 3.1 URLClassPath 的三个集合

JVM 的 `URLClassLoader` 内部用 `URLClassPath` 对象管理 classpath，`URLClassPath` 用三个数据结构共同管理：

```
URLClassPath
├── urls / unopenedUrls   : 所有还没打开的 jar URL 列表(Stack)
├── path                  : 所有 classpath 条目(包括已打开和未打开)
└── loaders               : 每个 jar 对应一个 Loader 对象(负责实际从 jar 加载类)
```

**三个集合的职责**：

- `urls`/`unopenedUrls`：还没被打开解析的 jar URL。JVM 懒加载--只有第一次需要从某 jar 加载类时，才把它从 `urls` 弹出、创建 Loader、放进 `loaders`。
- `path`：所有 classpath 条目（包括已打开和未打开的）。用于 `toString()` 等展示。
- `loaders`：每个已打开的 jar 对应一个 `Loader` 对象。`Loader` 内部可能缓存了已加载的类。

### 3.2 为什么三个都要移除

如果你只移除 `urls` 而忘记 `path` 和 `loaders`：

```
只移除 urls:
  urls 里没有 guava-18.0.jar 了
  但 loaders 里还有 guava-18.0.jar 的 Loader
  -> Loader 内部缓存了已加载的 Preconditions.class
  -> 后续 loadClass("Preconditions") 仍从 Loader 缓存命中
  -> ban 失效! 加载的还是 18.0 版本
```

**三个集合必须同步移除**：

```java
// AbstractURLClassPathHandle.removeURL 的核心逻辑(简化)
synchronized (urls) {
    for (Object loader : loaders) {
        URL base = getFieldValue(loader, getBaseField());
        if (Objects.equals(resolveBasePath(base), resolveBasePath(url))) {
            iterator.remove();      // 从 loaders 移除这个 Loader
            urls.remove(url);       // 从 urls 移除 URL
            path.remove(url);       // 从 path 移除 URL
            return true;
        }
    }
}
```

**这是「数据结构一致性」的普适问题**：当一个数据被多个数据结构索引（如数据库的表 + 索引 + 缓存），更新时必须同步所有索引，否则出现不一致。`URLClassPath` 的三个集合就是同一份 classpath 数据的三个「索引」--移除时三个都要更新。

### 3.3 `synchronized(urls)` 的同步保护

为什么用 `synchronized(urls)`？因为 JVM 的类加载是多线程的--两个线程可以同时加载不同的类。如果线程 A 正在用 `urls` 查找 `Foo.class`，线程 B 同步移除了 `urls` 中的一个 URL--没有同步保护，A 可能读到损坏的集合状态（`Stack` 非线程安全）。

**为什么用 `urls` 作为锁而非 `this`？** 因为 `urls` 是 `URLClassPath` 内部自己的锁对象--JDK 自己的类加载代码也用 `synchronized(urls)` 保护对 `urls` 的访问。microsphere 用同一个锁对象，保证与 JDK 内部代码的互斥--**复用 JDK 的锁而非自创新锁**，这是反射操作内部时的安全实践。

### 3.4 `initializeLoaders`:触发懒加载的技巧

`URLClassPath` 的 `loaders` 集合是**懒初始化**的--只有第一次查找资源时才为每个 jar 创建 Loader。如果在 `loaders` 还没初始化时就调 `removeURL`，`loaders` 是空的，遍历不到任何 loader，ban 静默失败。

microsphere 的 `initializeLoaders` 用了个巧妙技巧：

```java
// 触发 URLClassPath 填充 loaders 集合
urlClassLoader.findResource("just-for-initializing-loaders");  // 不存在的资源
```

`findResource` 会遍历 `URLClassPath` 中的所有 URL，为每个 jar 创建 Loader 并填充到 `loaders`。查找一个不存在的资源不影响功能，但触发了懒加载初始化。**这是「利用副作用触发初始化」的技巧**--和 §7 URL 扩展里 `clearURLStreamHandlerFactory` 反射清空 factory 是同类「非常规手段」。

**如果跳过这一步**：`removeURL` 遍历 loaders 找匹配的 Loader -> `loaders` 集合为空 -> 循环体不执行 -> 返回 false。用户的 banned 版本没有被移除--**且不会有任何报错**。这是「静默失败」的典型。


## 四、永恒原理三:JDK 版本适配与「反射操作内部」的代价

`URLClassPath` 是 JDK 内部类（`sun.misc` / `jdk.internal`），它的包名和字段名在 JDK 9 模块化时改过一次。microsphere 必须适配这个变化。

### 4.1 JDK 8 vs JDK 9+ 的差异

```java
// JDK 8: ClassicURLClassPathHandle
类名: sun.misc.URLClassPath
urls 字段名: urls

// JDK 9+: ModernURLClassPathHandle
类名: jdk.internal.loader.URLClassPath
urls 字段名: unopenedUrls   // 暗示这些 URL 是「还没被打开解析的」
```

**两个关键差异**：

1. **类名**：`sun.misc.URLClassPath` -> `jdk.internal.loader.URLClassPath`（Jigsaw 模块系统重命名）。
2. **字段名**：`urls` -> `unopenedUrls`（语义更清晰--这些 URL 是「还没被打开解析的」，打开后的 URL 在 `loaders` 中管理）。

**运行时选择**：`AbstractURLClassPathHandle.supports()` 通过 `Class.forName(className)` 判断当前 JVM 是否支持该 Handle--两个 Handle 在同一 JVM 上有且仅有一个返回 true。**这是「运行时能力探测」的标准做法**（和 [§10 日志](./10-logging-abstraction.md) 的 `isAvailable` 类探测同构）。

### 4.2 反射操作内部的代价

`URLClassPath` 虽然是内部类，但它的**对外行为（字段名、数据结构）极其稳定**--从 JDK 1.2 首次引入到 JDK 21，`ucp`、`urls`/`unopenedUrls`、`path`、`loaders` 这些字段的概念一直存在--只是包名和个别字段名在 JDK 9 模块化时改了一次。

**但 JDK 9 模块系统对反射加了限制**：

- JDK 9-15：默认 `--illegal-access=permit`，反射内部类可用但有警告。
- JDK 16+：默认 `--illegal-access=deny`，反射 `jdk.internal` 类直接抛 `InaccessibleObjectException`。

**生产环境必须加 JVM 参数**：

```
--add-opens java.base/jdk.internal.loader=ALL-UNNAMED
--add-opens java.base/sun.misc=ALL-UNNAMED   (JDK 8 兼容)
```

**这是「反射操作 JDK 内部」类方案的共同代价**（参见 [§7 URL factory 反射](./07-url-stream-handler.md) §2.3、BannedArtifact 本文 §4）。microsphere 不在代码里检测这个--它假设部署文档写清楚了。JDK 大版本升级时需验证反射是否仍可用。

### 4.3 为什么这个反射能成功

JVM 的 `URLClassPath` 虽然是内部类，但它的数据结构极其稳定--从 JDK 1.2 到 JDK 21，三个集合（`urls`/`path`/`loaders`）的概念一直存在。microsphere 通过 Classic/Modern 两个 Handle 适配了 JDK 9 的唯一一次变化。

**这是「内部 API 的稳定性 vs 官方支持的缺失」的张力**：JDK 不保证内部 API 稳定（随时可能改），但事实上 `URLClassPath` 的核心结构 20 多年没大变。microsphere 赌的就是这个「事实稳定性」--如果 JDK 某天大改 `URLClassPath`，这套方案会失效。**这是反射操作内部的固有风险**：你依赖了不保证稳定的东西。


## 五、microsphere 作为「运行时依赖隔离」的一个实例

讲完三个原理，microsphere 的 `BannedArtifact` 就是这些原理的一次落地。

**实例一:ban 的时序（§2 原理的落地）**

`BannedArtifactClassLoadingListener` 实现 `SpringApplicationRunListener`，在 `starting()` 阶段（Spring 容器启动最早阶段）触发 `BannedArtifactClassLoadingExecutor.execute()`。此时业务类还没被加载，ban 能生效。

**实例二:三集合移除（§3 原理的落地）**

`AbstractURLClassPathHandle.removeURL` 反射获取 `URLClassPath` 的三个集合（`urls`/`path`/`loaders`），遍历 `loaders` 找 base URL 匹配的 Loader，`synchronized(urls)` 保护下同步移除三个集合中的对应条目。`initializeLoaders` 先调 `findResource("just-for-initializing-loaders")` 触发懒加载初始化。

**实例三:JDK 版本适配（§4 原理的落地）**

`ClassicURLClassPathHandle`（JDK 8，`sun.misc.URLClassPath` + `urls`）和 `ModernURLClassPathHandle`（JDK 9+，`jdk.internal.loader.URLClassPath` + `unopenedUrls`）两个 Handle，运行时 `supports()` 通过 `Class.forName` 判断哪个可用。

**配置格式与多模块聚合**：

```properties
# META-INF/banned-artifacts(每个 jar 一个文件,ClassLoader.getResources 找到所有)
com.google.guava:guava:18.0
com.google.guava:guava:18.*
commons-logging:commons-logging:*
```

三列格式 `groupId:artifactId:version`，支持 `*` 通配符。`loadBannedArtifactConfigs` 用 `ClassLoader.getResources("META-INF/banned-artifacts")` 加载所有 jar 的 banned 清单并聚合--**不同模块可以在各自 jar 中声明各自要 ban 的 artifact**，Executor 聚合所有清单。

**执行流程**：

```
1. loadBannedArtifactConfigs(cl)   -> 从 META-INF/banned-artifacts 加载 banned 清单
2. artifactDetector.detect(false)  -> 扫描 classpath 上所有 artifact(排除 JDK)
3. 逐个比对 -> 如果 detected artifact 匹配 banned artifact -> removeClassPathURL
```

识别（[§1](./01-artifact-detection.md)）+ 移除（本文）形成完整的运行时依赖隔离链。


## 六、实例批判:这个实现的缺陷

作为原理的一个落地实例，microsphere 的实现也有瑕疵。

1. **`removeURL` 静默失败不验证**：`removeClassPathURL` 后没有重新扫描 classpath 确认被 ban 的 artifact 不再出现。如果 `removeURL` 失败（非 URLClassLoader、loaders 未初始化、URL 不匹配），ban 静默不生效。生产环境应加验证步骤。
2. **`initializeLoaders` 失败静默**：如果 ClassLoader 不是 URLClassLoader（如自定义 CL），`findResource` 触发不了 `URLClassPath` 的初始化，`loaders` 为空，`removeURL` 遍历不到任何 loader 返回 false。静默失败。
3. **已加载类无法 ban**（§2.1）：如果 ban 执行前已有线程加载了冲突版本的类，ban 只能阻止未来加载，已加载的类仍在 Method Area。这是运行时移除路线的固有局限，非实现缺陷。
4. **JDK 16+ 反射限制**（§4.2）：未加 `--add-opens` 时反射 `jdk.internal.loader.URLClassPath` 抛 `InaccessibleObjectException`，整个 ban 流程失败。microsphere 不检测这个，错误信息不直观。
5. **GAV 通配符匹配的边界**：`com.google.guava:guava:18.*` 匹配 `18.0`/`18.1` 但不匹配 `18.0-jre`（`*` 不跨 `.`? 取决于实现）。通配符语义需要文档明确。

这些不是原理错误，而是原理在具体代码里的实现瑕疵或固有局限。


## 七、与其他方案的原理对比

| 方案 | 隔离路线 | 是否反射 JDK 内部 | 能否按版本 ban | 粒度 | 侵入性 | 已加载类问题 |
|---|---|---|---|---|---|---|
| BannedArtifact（microsphere） | 运行时移除 | 需要 | ✅（GAV 匹配） | jar 级别 | 低（启动一次性） | ❌ 有 |
| Maven Shade | 构建期重命名 | 不需要 | ❌（按包名） | 类级别 | 中（改构建） | N/A |
| JDK 9 ModuleLayer | 架构级 | 不需要 | ❌（模块名） | 模块级别 | 高（需模块化） | ✅ 无 |
| `--limit-modules` | 启动参数 | 不需要 | ❌ | 模块名级别 | 极低（启动参数） | ✅ 无 |
| OSGi / Tomcat WebAppCL | 架构级 | 不需要 | ❌ | ClassLoader 级别 | 极高（架构级） | ✅ 无 |

**原理层面的取舍**：

- **Maven Shade 走构建期**：不反射 JDK 内部，但只解决「你自己的 jar 用哪个版本」，不能阻止用户的 classpath 上出现另一个版本。且 shade 后调试困难。
- **JDK 9 ModuleLayer 走架构级**：不反射、能彻底隔离（新模块层不含被 ban 模块），但要求整个应用模块化--对绝大多数 classpath 模式运行的 Spring Boot 应用不可行。
- **OSGi/Tomcat 走架构级**：ClassLoader 级隔离最彻底（每个 bundle/webapp 独立 CL），但要求整个应用按 OSGi/Tomcat 架构设计，侵入性极高。
- **microsphere 走运行时移除**：在已有 CL 上操作，低侵入、能按版本 ban，但必须反射 JDK 内部且受「已加载类不可逆」约束。

microsphere 的定位：**「运行时的精细控制，代价是反射操作 JVM 内部 + 已加载类约束」**。如果你能接受架构级变更，ModuleLayer/OSGi 更彻底；如果你只想在启动时 ban 掉冲突版本且不改架构，microsphere 是少数能做到的方案。


## 八、面试要点

**Q1：「你有个框架依赖 guava:30，用户的项目里已经有 guava:18。怎么防止 NoSuchMethodError？有哪些方案？」**

答案：三条路。① 构建期隔离--Maven shade 重命名包，你的 jar 内含 shade 的 guava 30.0，和用户的 18.0 是不同的类不冲突。但 shade 阻止不了用户 classpath 上出现另一个版本，且 jar 变大、调试困难。② 架构级隔离--OSGi/ModuleLayer，每个模块独立 ClassLoader，最彻底但要求架构级变更。③ 运行时移除（microsphere）--启动时 ArtifactDetector 扫描 classpath 找到 guava:18（[§1](./01-artifact-detection.md)），BannedArtifactClassLoadingExecutor 从 ClassLoader 内部移除 guava-18.0.jar 的 URL，guava:18 对后续类加载不可见。代价是反射操作 JVM 内部（`URLClassPath`），JDK 16+ 需要 `--add-opens`。

**Q2：「运行时移除一个 jar，为什么不能只从 classpath 列表里删掉？要操作什么数据结构？」**

答案：JVM 的 `URLClassPath` 用三个集合共同管理 classpath：`urls`/`unopenedUrls`（还没打开的 jar URL）、`path`（所有 classpath 条目）、`loaders`（每个 jar 的 Loader 对象）。三个必须同步移除--如果只移除 `urls` 留下 `loaders`，Loader 中可能缓存了已加载的类，后续 `loadClass` 仍从 Loader 缓存命中旧版本，ban 失效。这是「数据结构一致性」问题--同一份数据被多个索引引用，更新时必须同步所有索引。移除时还要 `synchronized(urls)` 保护，因为 JDK 自己的类加载代码也用这个锁，复用 JDK 的锁而非自创新锁是反射操作内部的安全实践。

**Q3：「ban 一个 jar 后，已经从它加载的类会怎样？ban 还有效吗？」**

答案：已加载的类不会消失。JVM 没有「卸载类」的标准 API--类一旦加载进 Method Area，存在于 ClassLoader 的 `classes` 字典中，直到 ClassLoader 被 GC。ban 只能阻止「未来的类加载」，不能让「已加载的类」消失。所以 ban 必须在类加载的最早阶段执行--microsphere 在 `SpringApplicationRunListener.starting()` 阶段，此时业务类还没被加载。如果 ban 太晚（Bean 初始化阶段），冲突版本的类可能已被加载，ban 移除了 URL 但类还在，调用仍走旧版本。这是运行时移除路线的固有约束，只有架构级方案（新 ClassLoader/新 ModuleLayer）能完全规避。

**Q4：「JDK 升级后，运行时移除方案会失效吗？怎么防御？」**

答案：JDK 9 从 `sun.misc.URLClassPath` 改为 `jdk.internal.loader.URLClassPath`，`urls` 字段改为 `unopenedUrls`。microsphere 用 Classic/Modern 双 Handle 适配--`supports()` 通过 `Class.forName` 运行时判断当前 JVM 支持哪个 Handle。下次 JDK 大变更时需新增 Handle。更严峻的是 JDK 16+ `--illegal-access=deny` 默认值阻止反射 `jdk.internal` 类--必须加 `--add-opens java.base/jdk.internal.loader=ALL-UNNAMED`。microsphere 不检测这个，失败时 `InaccessibleObjectException` 直接冒泡，错误信息不直观。这是「反射操作 JDK 内部」类方案的共同代价。

**Q5：「`initializeLoaders` 为什么要调 `findResource("just-for-initializing-loaders")`？不调会怎样？」**

答案：`URLClassPath` 的 `loaders` 集合是懒初始化的--只有第一次查找资源时才为每个 jar 创建 Loader 并填充 `loaders`。如果不调 `initializeLoaders`，`loaders` 可能为空，`removeURL` 遍历不到任何 loader 返回 false，ban 静默失败。`findResource("just-for-initializing-loaders")` 查找一个不存在的资源，不影响功能，但触发了 URLClassPath 遍历所有 URL 创建 Loader 的副作用。这是「利用副作用触发初始化」的技巧。如果 ClassLoader 不是 URLClassLoader，`findResource` 触发不了 URLClassPath 初始化，loaders 仍为空，静默失败。

**Q6：「运行时移除和 Maven shade 各有什么局限？什么时候选哪个？」**

答案：shade 的局限是只解决「你自己的 jar 用哪个版本」，不能阻止用户 classpath 上出现另一个版本；且 jar 变大、调试困难。运行时移除的局限是必须反射 JDK 内部（受模块系统限制）、受「已加载类不可逆」约束（必须早 ban）、有无传递依赖信息的问题（不知道谁引入了冲突版本）。选 shade：你的框架只关心自己用哪个版本，不关心用户的 classpath。选运行时移除：你要确保用户的 classpath 上没有冲突版本（如框架强制要求某版本，ban 掉用户引入的旧版本）。两者可组合--shade 自己的依赖 + 运行时 ban 用户的冲突版本。

---

> **与 §1 的关联**：本文承接 [§1 运行时依赖识别](./01-artifact-detection.md)--先识别 classpath 上有什么，再决定移除什么。ArtifactDetector 的降级链（pom.properties -> manifest -> 文件名）为 BannedArtifact 的 GAV 匹配提供坐标信息。
> **与 §7 的关联**：本文和 [§7 URL 协议扩展](./07-url-stream-handler.md) 同属「反射操作 JDK 内部」类方案--本文反射 `URLClassPath`，§7 反射 `URL.factory`。两者都受 JDK 9 模块系统限制，都需要 `--add-opens`，都是「赌内部 API 事实稳定性」的方案。
