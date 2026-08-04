# 配置文档生成 -- 为什么 `@ConfigurationProperty` 不做运行时绑定

> 主题：配置有两个维度--运行时绑定（Spring `@ConfigurationProperties` 解决）和编译期文档（microsphere `@ConfigurationProperty` 解决），两者是不同问题；Annotation Processor 作为「编译期反射」的能力与边界；多源配置聚合与来源追踪。microsphere 的配置文档框架是编译期文档路线的一个实例。
> 关联：Annotation Processor 与 [§5 编译期处理](./05-config-property.md) 的 `@ConfigurationProperty` 共享编译期能力；运行时 Loader 复用 [§4 SPI+Prioritized](./04-spi-prioritized.md) 机制。

---

## 一、配置的两个维度:绑定 vs 文档

### 1.1 Spring `@ConfigurationProperties` 解决了什么

```java
@ConfigurationProperties(prefix = "my.config")
public class MyConfig {
    private int timeout;
    // getter/setter
}
// 配置 my.config.timeout=3000 -> MyConfig.timeout = 3000(运行时绑定)
```

Spring Boot 的 `@ConfigurationProperties` 解决的是**运行时绑定**：把 `application.yml` 中的值自动注入到 `MyConfig` bean 的字段。这是配置的「运行时」维度。

但它不解决两个问题：

1. **文档**：你的框架有哪些配置项？默认值是什么？不读代码不知道。
2. **来源追踪**：运行时某个配置值到底来自哪里--系统属性？环境变量？配置文件？

### 1.2 配置的「文档」维度

```java
@ConfigurationProperty(
    name = "microsphere.service-loader.cached",
    defaultValue = "false",
    description = "Whether to cache the loaded services",
    source = "System Properties"
)
public static final boolean SERVICE_LOADER_CACHED = 
    parseBoolean(System.getProperty("microsphere.service-loader.cached", "false"));
```

microsphere 的 `@ConfigurationProperty` 标注在 `static final` 字段上，声明四元信息：**名字、默认值、描述、来源**。这些信息在编译期被 Annotation Processor 提取，生成 JSON 文档（`META-INF/microsphere/configuration-metadata.json`）。

**文档维度的价值**：

- **开发者打开 jar 就能看到配置清单**--不用读源码。
- **CI diff 检测**：有人改了 `defaultValue`，生成的 JSON 会变，CI 能检测到配置变更。
- **IDE 集成**：未来 IDE 可以读这个 JSON 提供配置补全（类似 Spring Boot 的 `spring-configuration-metadata.json`）。

### 1.3 为什么 microsphere 不做运行时绑定

**分层设计，职责分离**。microsphere-java-core 是纯 Java 工具库，不依赖 Spring Boot。运行时绑定需要 `Environment` 抽象（Spring 的 `ConfigurableEnvironment`），这是 Spring 的能力，microsphere 不重复造。

microsphere-spring（上层模块）用 `ConfigurationBeanBinder` 做绑定（基于 Spring `Environment`），`@ConfigurationProperty` 专注文档--**文档在底层（microsphere-java-core），绑定在上层（microsphere-spring）**。这是「关注点分离」--一个注解解决一个问题。

### 1.4 Spring Boot 的配置文档方案

Spring Boot 有 `spring-configuration-metadata.json`--但它需要 `spring-boot-configuration-processor` 依赖（Spring Boot 自己的注解处理器），生成的是 Spring Boot 专用格式。microsphere 不依赖 Spring Boot，所以需要自建一套--编译期处理 + 运行时聚合，本文讲透背后的三个永恒原理。


## 二、永恒原理一:Annotation Processor 作为「编译期反射」

### 2.1 编译期 vs 运行时:两个维度处理注解

注解处理有两个时机：

| 时机 | 机制 | 能拿到什么 | 副作用 |
|---|---|---|---|
| 运行时 | 反射（`Class.getAnnotation`） | 运行时类信息 | 需要类被加载（运行时开销） |
| 编译期 | Annotation Processor | 编译期 AST（`Element`） | 可生成文件/代码（编译期完成，运行时零开销） |

**编译期处理的核心优势**：

1. **运行时零开销**：文档在 `mvn compile` 时生成，运行时只读文件，不反射。
2. **CI 可检测**：生成的 JSON 是编译产物，可纳入 CI diff--配置变更可见。
3. **不要求类被加载**：编译期处理不需要类在运行时被加载，适合「声明性配置」。

**编译期处理的局限**：

1. **只能处理编译期可见的注解**：运行时动态加的注解处理不了。
2. **不能访问运行时状态**：`System.getProperty()` 的值在编译期不可知。
3. **多 round 处理**：javac 可能多次调用 Processor，需要处理「processing over」边界。

### 2.2 两阶段处理:收集 + 写入

Annotation Processor 的一个关键设计是**两阶段处理**：

```java
@Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
        // 阶段2(最后一次 round):一次性写入 JSON 文件
        writeMetadata();
    } else {
        // 阶段1(前面的 rounds):遍历 AST 收集元数据
        resolveMetadata(roundEnv);
    }
    return false;
}
```

**为什么分两个阶段？** javac 可能多次调用 Processor--每个 round 处理一批文件。前面的 rounds 收集数据，最后一次 round（`processingOver() == true`）一次性写入 JSON--**避免写入不完整的文档**。

如果每个 round 都写文件，前面 rounds 写入的可能是部分数据（还没处理完所有类），最后还要覆盖--浪费 IO 且可能出现中间状态。两阶段保证「收集完整后一次性写入」。

### 2.3 Visitor 模式:遍历 AST 的标准做法

Annotation Processor 用 `ElementVisitor` 遍历 AST：

```java
void resolveMetadata(Set<? extends Element> elements) {
    for (Element element : elements) {
        element.accept(jsonElementVisitor, jsonBuilder);  // Visitor 模式
    }
}
```

**为什么用 Visitor 而非 if-else instanceof？** AST 的 `Element` 有多种子类型（`PackageElement`/`TypeElement`/`VariableElement`/`ExecutableElement`），Visitor 模式让每种类型有对应的 `visitXxx` 方法--类型分发由 Visitor 框架完成，不需要写 `if (element instanceof VariableElement)` 链。这是编译期处理的标准做法（javac 自己也用 Visitor 遍历 AST）。

### 2.4 编译期处理的边界

**边界一:编译失败容错**。`writeMetadata()` 抛异常时 javac 打印错误但继续编译--JSON 不生成。CI 应加对输出文件的断言检测（`assert file exists: META-INF/microsphere/configuration-metadata.json`）。

**边界二:生成的 JSON 没人消费时的价值**。即使没有运行时消费方，JSON 文件作为「配置项清单」本身就有文档价值--开发者打开 jar 就能看到框架有哪些配置、默认值是什么。这是「文档即编译产物」的价值。

**边界三:Processor 多 round 的不确定性**。javac 的 round 数量取决于「是否有新生成的文件需要处理」。microsphere 的两阶段设计保证无论多少 round，最终 JSON 是完整的。但如果某个 round 的 `resolveMetadata` 抛未捕获异常，后续 round 不再执行--JSON 可能不完整。


## 三、永恒原理二:配置的「来源追踪」难题

### 3.1 配置值的来源问题

运行时某个配置值 `timeout=3000` 到底来自哪里？

```
可能来源:
1. 命令行参数 --timeout=3000
2. 系统属性 -Dtimeout=3000
3. 环境变量 TIMEOUT=3000
4. application.yml timeout: 3000
5. 代码默认值 defaultValue = "3000"
```

**来源追踪的价值**：当生产环境配置出问题时，你需要知道「这个值是谁设的」。如果只知道 `timeout=3000` 不知道来源，排查困难--是运维设的？是环境变量？是代码默认值？

### 3.2 `@ConfigurationProperty` 的 `source` 字段

```java
@ConfigurationProperty(
    name = "microsphere.service-loader.cached",
    defaultValue = "false",
    description = "Whether to cache the loaded services",
    source = "System Properties"   ← 显式声明来源
)
```

`source` 字段显式声明这个配置项的来源（`System Properties`/`Environment Variables`/`Configuration File`）。编译期写入 JSON，运行时可读--**来源信息在声明时就固定，不依赖运行时探测**。

**这和 Spring Boot 的区别**：Spring Boot 的 `spring-configuration-metadata.json` 不含 `source` 字段--Spring Boot 用 `Environment` 的 `PropertySource` 链在运行时追踪来源（命令行 > 系统属性 > 环境变量 > yml），但这是运行时行为，不在编译期文档里。microsphere 的 `source` 是声明性的，简单但不够动态。

### 3.3 来源追踪的两条路:声明性 vs 运行时探测

| 路线 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| 声明性（microsphere） | `source` 字段编译期固定 | 简单、文档可见 | 不反映运行时实际来源 |
| 运行时探测（Spring） | `Environment.getPropertySources()` 链 | 反映运行时实际来源 | 复杂、需要容器 |

**没有银弹**：声明性简单但可能不准（开发者写的 `source = "System Properties"` 可能和实际不符）；运行时探测准确但需要容器支持。microsphere 选声明性因为它是无容器基础库--没有 `Environment` 抽象可用。


## 四、永恒原理三:多源配置聚合与「错误隔离」

### 4.1 运行时:三层 Loader 聚合

编译期生成的 JSON 只是一个数据源。运行时可能还有其他配置元数据来源。microsphere 用 SPI 加载多个 `ConfigurationPropertyLoader`，聚合所有来源：

```java
static List<ConfigurationProperty> loadAll() {
    List<ConfigurationPropertyLoader> loaders = loadServicesList(ConfigurationPropertyLoader.class);
    List<ConfigurationProperty> all = new LinkedList<>();
    for (ConfigurationPropertyLoader loader : loaders) {
        try {
            List<ConfigurationProperty> loaded = loader.load();
            if (loaded != null) all.addAll(loaded);
        } catch (Throwable e) {
            logger.error("Failed to load via {}", loader.getClass(), e);  // 错误隔离
        }
    }
    return unmodifiableList(all);
}
```

**三个内置 Loader**：

| Loader | 数据来源 | 用途 |
|---|---|---|
| `ClassPathResourceConfigurationPropertyLoader` | classpath 上的 JSON 文件 | 加载编译期生成的元数据 |
| `MetadataResourceConfigurationPropertyLoader` | `META-INF/` 下的 Properties 文件 | 运行时补充配置 |
| `AdditionalMetadataResourceConfigurationPropertyLoader` | 额外的元数据资源 | 用户自定义补充 |

### 4.2 错误隔离:单个 Loader 失败不影响其他

```java
try {
    List<ConfigurationProperty> loaded = loader.load();
    if (loaded != null) all.addAll(loaded);
} catch (Throwable e) {
    logger.error("Failed to load via {}", loader.getClass(), e);  // catch + 继续
}
```

**错误隔离的设计**：一个 Loader 失败（如 JSON 格式错误、文件不存在）不阻止其他 Loader 工作。`catch (Throwable)` + `logger.error` + 继续迭代。

**为什么 `catch (Throwable)` 而非 `Exception`？** 因为 `load()` 可能抛 `Error`（如 `OutOfMemoryError` 读超大文件、`NoClassDefFoundError` 依赖类缺失）。`Exception` 捕获不了 `Error`。但 `catch (Throwable)` 也吞了 `OutOfMemoryError`--这在 JVM 快崩时可能掩盖问题。**这是「容错 vs 诊断」的权衡**，microsphere 选容错（一个 Loader 失败不影响其他）。

### 4.3 多源聚合的「同名冲突」问题

两个 Loader 返回同 `name` 的配置项时，`loadAll()` **不做去重**--两个都保留在列表中。调用方自行决定以哪个为准。

**为什么不自动去重？** 因为「哪个优先」取决于业务语义--编译期生成的（`ClassPathResource`）权威，还是运行时补充的（`MetadataResource`）更新？microsphere 不替你决定，把选择权交给调用方。**这是「机制 vs 策略」的分离**--框架提供聚合机制，优先级策略由调用方定。


## 五、microsphere 作为「配置文档生成」的一个实例

讲完三个原理，microsphere 的配置文档框架就是这些原理的一次落地。

**实例一:编译期文档生成（§2 原理的落地）**

`@ConfigurationProperty` 标注在 `static final` 字段上。`ConfigurationPropertyAnnotationProcessor` 用两阶段处理（`resolveMetadata` 收集 + `writeMetadata` 写入），Visitor 遍历 AST 提取四元信息（name/defaultValue/description/source），生成 `META-INF/microsphere/configuration-metadata.json`。

**实例二:来源声明（§3 原理的落地）**

`source` 字段在注解里显式声明（如 `System Properties`），编译期写入 JSON。运行时读 JSON 就知道每个配置项的预期来源。

**实例三:多源聚合 + 错误隔离（§4 原理的落地）**

`ConfigurationPropertyLoader.loadAll()` 通过 SPI 加载三个 Loader，聚合所有来源。单个 Loader 失败 `catch (Throwable)` 隔离，不影响其他。同名冲突不去重，调用方决定优先级。

**生成的 JSON 格式**：

```json
[
  {
    "name": "microsphere.service-loader.cached",
    "type": "java.lang.Boolean",
    "defaultValue": "false",
    "description": "Whether to cache the loaded services",
    "source": "System Properties",
    "metadata": {
      "declaredClass": "io.microsphere.util.ServiceLoaderUtils",
      "declaredField": "SERVICE_LOADER_CACHED"
    }
  }
]
```


## 六、实例批判:这个实现的缺陷

1. **`source` 字段是声明性的，可能和实际不符**：开发者写 `source = "System Properties"` 但实际可能从环境变量读取。无运行时校验。
2. **同名冲突不去重**：两个 Loader 返回同 `name` 的配置项都保留，调用方可能拿到两个冲突的配置。应提供「按 Loader 优先级去重」选项。
3. **JSON 损坏时整个 Loader 被跳过**：`new JSONArray(corruptedContent)` 抛 `JSONException` -> 传播到 `loadAll` 的 catch -> 该 Loader 全部数据丢失。应 try 逐条解析而非整个 JSON 一次解析。
4. **编译失败静默**：`writeMetadata()` 抛异常时 JSON 不生成，javac 继续编译。CI 不检测就不知道文档缺失。
5. **无运行时消费方**：microsphere-java-core 只生成 + 聚合文档，不做运行时绑定。绑定在 microsphere-spring 的 `ConfigurationBeanBinder`--但本文不展开。

这些不是原理错误，而是原理在具体代码里的实现瑕疵。


## 七、与其他方案的原理对比

| 方案 | 编译期文档 | 运行时绑定 | 来源追踪 | 依赖 | 适合场景 |
|---|---|---|---|---|---|
| microsphere @ConfigurationProperty | ✅（AP 生成 JSON） | ❌（交给 Binder） | ✅（`source` 声明性） | 无 | 不依赖 Spring Boot 的框架 |
| Spring Boot @ConfigurationProperties | ✅（processor） | ✅（Binder） | ✅（`PropertySource` 链） | spring-boot.jar | Spring 应用 |
| SmallRye Config（MicroProfile） | ✅ | ✅ | ✅ | MicroProfile | Quarkus/WildFly |
| Typesafe Config（HOCON） | 🟡（reference.conf） | ✅ | ❌ | typesafe-config.jar | Akka/Play 生态 |
| JDK Preferences | ❌ | ✅ | ❌ | JDK | 桌面应用 |

**原理层面的取舍**：

- **Spring Boot @ConfigurationProperties** 是「文档 + 绑定」一体化--编译期 processor 生成 metadata，运行时 Binder 用 metadata 做绑定。但依赖 `spring-boot.jar`。
- **SmallRye Config** 是 MicroProfile 规范，「文档 + 绑定 + 注入」一体，但需要 MicroProfile 容器（Quarkus/WildFly）。
- **Typesafe Config** 走另一条路--`reference.conf` 既是配置文件也是文档（默认值和注释在一个地方），直观但不是编译期验证的。
- **microsphere @ConfigurationProperty** 专注文档，不做绑定--**分层设计，绑定交给上层**。这是「零依赖基础库」的定位决定的。

microsphere 的定位：**不依赖 Spring Boot 的框架的配置文档方案**。如果你在 Spring Boot 应用里，直接用 `@ConfigurationProperties` 更好（文档 + 绑定一体）；如果你写不依赖 Spring Boot 的框架，microsphere 的 `@ConfigurationProperty` 是少数能在零依赖前提下做到编译期文档生成的方案。


## 八、面试要点

**Q1：「`@ConfigurationProperties` 和 `@ConfigurationProperty` 有什么区别？为什么不直接用 `@ConfigurationProperties`？」**

答案：`@ConfigurationProperties` 是 Spring Boot 的运行时绑定--把 yml 的值注入 bean 字段，需要 `spring-boot.jar`。`@ConfigurationProperty` 是编译期文档生成--提取配置项的 name/defaultValue/description/source 生成 JSON，不需要任何依赖。如果你的框架不依赖 Spring Boot（microsphere-java-core 是纯 Java 工具库），后者是唯一选择。两者解决不同问题--前者是绑定，后者是文档。microsphere 的绑定在 microsphere-spring 的 `ConfigurationBeanBinder`（基于 Spring Environment），`@ConfigurationProperty` 专注文档--分层设计，职责分离。

**Q2：「Annotation Processor 和运行时反射有什么区别？为什么选编译期？」**

答案：运行时反射在 JVM 启动后处理注解，需要类被加载（有运行时开销）。Annotation Processor 在 `mvn compile` 时处理，遍历编译期 AST（`Element`），生成文件/代码--运行时零开销。选编译期三个原因：① 运行时零开销（文档在编译期生成，运行时只读文件）。② CI 可检测（生成的 JSON 是编译产物，配置变更可 diff）。③ 不要求类被加载（适合声明性配置）。两阶段处理（`resolveMetadata` 收集 + `writeMetadata` 写入）保证「收集完整后一次性写入」，避免多 round 的中间状态。

**Q3：「配置的『来源追踪』为什么重要？microsphere 怎么做的？」**

答案：生产环境配置出问题时需要知道「这个值是谁设的」--命令行？系统属性？环境变量？yml？代码默认值？不追踪来源排查困难。两条路：① 声明性（microsphere）--`source` 字段编译期固定写入 JSON，简单但可能和实际不符。② 运行时探测（Spring）--`Environment.getPropertySources()` 链反映运行时实际来源，准确但需要容器。microsphere 选声明性因为它是无容器基础库，没有 `Environment` 抽象。这是「简单 vs 准确」的权衡。

**Q4：「多个 ConfigurationPropertyLoader 聚合时，一个失败了怎么办？同名冲突怎么处理？」**

答案：错误隔离--`catch (Throwable)` + `logger.error` + 继续迭代。一个 Loader 失败（JSON 损坏、文件不存在）不阻止其他 Loader 工作。`catch (Throwable)` 而非 `Exception` 因为可能抛 `Error`（OOM/NoClassDefFoundError）。同名冲突不去重--两个 Loader 返回同 `name` 的配置项都保留，调用方自行决定优先级。这是「机制 vs 策略」分离--框架提供聚合机制，优先级策略由调用方定。

**Q5：「如果让你改进 microsphere 的配置文档框架，你会改什么？」**

答案：四个方向。① `source` 字段应支持运行时校验（声明 `System Properties` 但实际从环境变量读取时 warn）。② 同名冲突应提供「按 Loader 优先级去重」选项，而非全部保留。③ JSON 损坏时应逐条解析而非整个 JSON 一次解析（一个坏配置项不影响其他）。④ 编译失败时应生成标记文件（如 `.metadata-failed`），让 CI 能检测文档缺失而非静默跳过。

---

> **与 SPI+Prioritized 的关联**：`ConfigurationPropertyLoader.loadAll()` 通过 `loadServicesList` 加载所有 Loader，是 [§4 SPI+Prioritized](./04-spi-prioritized.md) 的直接应用。
> **与 §10 日志的关联**：`@ConfigurationProperty` 标注的 `source = "System Properties"` 和 [§10](./10-logging-abstraction.md) 日志 factory 的 `THREAD_NAME_PREFIX` 配置共享同一套配置文档机制--日志框架的配置项也被 AP 提取进 JSON。
