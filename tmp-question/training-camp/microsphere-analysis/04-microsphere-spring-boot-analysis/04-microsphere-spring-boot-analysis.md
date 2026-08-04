# Microsphere-Spring-Boot 深度分析

> **核心命题**：在已经使用 Spring Boot 的前提下，还能在哪些地方做增强？
> **本文回答**：它暴露了 Spring Boot 的 4 个内部钩子，每个钩子对应一个通用知识点。

---

## 知识点 1：`BindListener` — 拦截 Spring Boot 的属性绑定生命周期

### 问题

Spring Boot 的 `Binder.bind()` 是一个**黑盒**——你传进去一个配置属性 Map 和一个目标对象，它返回绑定好的对象。中间发生了什么？你无法观察、无法干预。

### 小马哥的做法

Microsphere 设计了一个 **Spring Boot 原生支持但极少被使用的扩展点**：`ConfigurationPropertiesBindHandlerAdvisor`。

```java
// Spring Boot 提供的扩展接口（你没听过吧？）
public interface ConfigurationPropertiesBindHandlerAdvisor {
    BindHandler apply(BindHandler bindHandler);
}

// Microsphere 的实现：自动发现所有 BindListener bean，链式包裹
public class ListenableConfigurationPropertiesBindHandlerAdvisor 
    implements ConfigurationPropertiesBindHandlerAdvisor {
    
    public BindHandler apply(BindHandler originalHandler) {
        List<BindListener> listeners = getSortedBeans(beanFactory, BindListener.class);
        return new ListenableBindHandlerAdapter(originalHandler, listeners);
    }
}
```

### 知识点

**`ConfigurationPropertiesBindHandlerAdvisor` 是 Spring Boot 2.0 引入但几乎没人知道的扩展点**。它的作用是在 `Binder.bind()` 的处理器链中插入自定义逻辑。

**BindListener 的生命周期钩子**：

```
Binder.bind(configProps, target)
  ├── onStart(name, target, context)        ← "开始绑定属性 my.config.timeout"
  ├── 内部解析配置值...
  ├── onSuccess(name, target, context, val)  ← "绑定成功: timeout=3000"
  │   或
  ├── onFailure(name, target, context, err)  ← "绑定失败: timeout 必须是整数"
  └── onFinish(name, target, context, val)   ← "绑定完成"
```

**使用场景**：

1. **配置审计**：记录哪些配置被实际使用了、哪些配置写了但没被绑定
2. **动态值转换**：`onSuccess` 中把 `"3s"` 转成 `3000`（自定义 duration 格式）
3. **敏感信息脱敏日志**：`onSuccess` 中检测字段名含 `password`，打日志时替换为 `***`
4. **配置变更事件发布**：`onSuccess` 中比较新值和旧值，有变化就发布 `ConfigurationPropertiesBeanPropertyChangedEvent`

> **可迁移知识**：Spring Boot 的 `Binder` 不是一个"要么用要么不用"的零和游戏——它提供了多个扩展点（`BindHandler`、`ConfigurationPropertiesBindHandlerAdvisor`、`BindConverter`）让你插入自定义逻辑。Microsphere 的 `BindListener` 只是把这些扩展点**白盒化**了。

---

## 知识点 2：`ArtifactsCollisionFailureAnalyzer` — 在 Spring Boot 崩溃时给出可操作的诊断

### 问题

`NoSuchMethodError: com.google.common.base.Preconditions.checkArgument` —— 这是典型的 classpath 冲突。Spring Boot 启动失败，控制台打印 100 行堆栈，但**没人告诉你冲突的具体 jar 是哪个、怎么修**。

### 小马哥的做法

```java
public class ArtifactsCollisionFailureAnalyzer 
    extends AbstractFailureAnalyzer<ArtifactsCollisionException> {

    protected FailureAnalysis analyze(Throwable root, ArtifactsCollisionException cause) {
        return new FailureAnalysis(
            cause.getMessage(),  // 问题描述
            buildAction(cause),  // 操作建议
            cause                // 异常本身
        );
    }

    private String buildAction(ArtifactsCollisionException cause) {
        Set<String> artifacts = cause.getArtifacts();
        return "运行以下 Maven 命令分析冲突:\n" +
               "mvn dependency:tree -Dincludes=" + String.join(",", artifacts) + "\n" +
               "然后逐个排除冲突的依赖";
    }
}
```

### 知识点

**Spring Boot 的 `FailureAnalyzer` 机制**：Spring Boot 在启动失败时，会遍历所有已注册的 `FailureAnalyzer`，每个 analyzer 检查异常链中是否有它能处理的异常类。第一个匹配的 analyzer 产生 `FailureAnalysis`，Spring Boot 用它生成友好的错误报告。

**Microsphere 的利用方式**：它注册了一个专门处理 `ArtifactsCollisionException` 的 analyzer。当 `BannedArtifactClassLoadingListener` 检测到冲突 artifact 时，抛出 `ArtifactsCollisionException`，analyzer 捕获后生成**可操作的建议**（`mvn dependency:tree` 命令）。

```java
// 在 spring.factories 中注册：
// org.springframework.boot.diagnostics.FailureAnalyzer=\
//   io.microsphere.spring.boot.diagnostics.ArtifactsCollisionFailureAnalyzer
```

**对比默认的 Spring Boot 错误报告**：

```
# Spring Boot 默认（用户看到的）:
Caused by: java.lang.NoSuchMethodError: com.google.common.base.Preconditions.checkArgument
    at ...
    at ...

# Microsphere 增强后（用户看到的）:
***************************
APPLICATION FAILED TO START
***************************

Description:
The following artifacts conflict in the classpath:
  - com.google.guava:guava:18.0
  - com.google.guava:guava:30.0

Action:
Analyze conflict Artifacts by running the following Maven command:
mvn dependency:tree -Dincludes=com.google.guava:guava:18.0,com.google.guava:guava:30.0
After analyzing the results, exclude them in the pom.xml file one by one!
```

> **可迁移知识**：错误信息决定了排障效率。好的错误信息包含三部分：① **问题是什么**（Description）；② **为什么发生**（碰撞的具体 artifact 和版本）；③ **怎么修**（可执行的命令）。不要只抛异常然后期望用户自己读堆栈——**把修复路径写入异常消息**。

---

## 知识点 3：`BannedArtifactClassLoadingListener` — 在 Spring Boot 启动的「最早时刻」介入

### 问题

如果两个版本的 guava 都已经在 classpath 上了，你必须在**任何类被加载之前**做出干预。一旦 `Preconditions.checkArgument` 被某个线程调用并解析到了错误的版本，`NoSuchMethodError` 就不可逆了。

### 小马哥的做法

```java
public class BannedArtifactClassLoadingListener 
    extends SpringApplicationRunListenerAdapter implements Ordered {
    
    public BannedArtifactClassLoadingListener(SpringApplication app, String... args) {
        super(app, args);
        setOrder(HIGHEST_PRECEDENCE);  // ← 最高优先级 = 最先执行
    }
    
    @Override
    public void starting() {
        if (bannedArtifactsEnabled()) {
            BannedArtifactClassLoadingExecutor executor = 
                new BannedArtifactClassLoadingExecutor(classLoader);
            executor.execute();  // ← 在 Spring Boot 加载任何 bean 之前执行
        }
    }
}
```

### 知识点

**Spring Boot 启动的 7 个阶段**（`SpringApplicationRunListener`）：

```
1. starting()         ← BannedArtifact 在这里执行！
2. environmentPrepared()
3. contextPrepared()
4. contextLoaded()
5. started()          ← 在这里类已经开始被加载了
6. ready()
7. failed()
```

`starting()` 是最早的钩子——此时 `Environment` 还没准备好，`ApplicationContext` 还没创建，**没有 bean 被初始化**。这是介入类加载的唯一窗口。

**为什么用 `HIGHEST_PRECEDENCE`？** 可能有多个 `SpringApplicationRunListener`。Microsphere 需要确保它的 banned artifact 检测在**所有其他 listener 之前**执行。如果另一个 listener 在 `starting()` 中加载了 `Preconditions` 类，那 banned artifact 就来不及了。

**防重复执行**：

```java
private static final ConcurrentMap<SpringApplication, Boolean> processedMap = new ConcurrentHashMap<>();

public void starting() {
    if (isProcessed()) return;  // 已经处理过了，跳过
    banArtifacts();
    markProcessed();
}
```

Spring Boot 在测试中可能会多次启动 `SpringApplication`（`@SpringBootTest` 每个测试类启动一次）。没有这个防重复逻辑，banned artifact 会重复执行——虽然功能上无害，但不必要的 `ClassLoader` 反射操作应该避免。

> **可迁移知识**：Spring Boot 的事件体系不只是 `ApplicationEvent`（`@EventListener`）。`SpringApplicationRunListener` 是更底层的钩子——它在 `ApplicationContext` 创建**之前**就能执行。如果你需要在"最早的时机"做事情（类加载干预、环境变量注入、JVM 参数校验），这是唯一的选择。

---

## 知识点 4：`ConfigurableAutoConfigurationImportFilter` — 控制哪些自动配置生效

### 问题

Spring Boot 的自动配置通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（Spring Boot 3.0+）或 `spring.factories`（2.x 及之前）声明。**所有匹配 classpath 条件的自动配置类都会被加载**。你不能在运行时动态排除某些自动配置。

### 小马哥的做法

```java
public interface ConfigurableAutoConfigurationImportFilter {
    boolean match(String autoConfigurationClassName);
}

// 实现：通过配置列表过滤
// 系统属性: microsphere.autoconfigure.exclude=com.example.FooAutoConfiguration
```

### 知识点

**Spring Boot 自身的过滤机制**：`@SpringBootApplication(exclude = FooAutoConfiguration.class)` 可以排除自动配置，但这是**编译期硬编码**的。Microsphere 的过滤器是**运行时可配置的**——通过系统属性或配置文件动态决定哪些自动配置加载。

**为什么需要这个？** 场景：你的框架引入了一个依赖（如 `spring-boot-starter-data-redis`），Spring Boot 会自动配置 Redis。但在某些环境下（如本地开发），你不想连接 Redis。传统做法是 `@SpringBootApplication(exclude = ...)` 或 `spring.autoconfigure.exclude`。Microsphere 做的是**让框架内部也能控制**——不是让用户配置，而是让框架作者决定"我引入的模块中，哪些自动配置应该对用户透明地跳过"。

> **可迁移知识**：Spring Boot 的自动配置是"全有或全无"的吗？不是。它提供了三个层次的过滤：① `@SpringBootApplication(exclude=...)`（用户选择）；② `AutoConfigurationImportFilter`（框架选择，Microsphere 用这个）；③ `@ConditionalOn*`（条件选择）。理解这三个层次，就知道从哪里切入做定制。

---

## 总结：Microsphere-Spring-Boot 的 4 个知识点

| # | 知识点 | Spring Boot 的钩子 | Microsphere 做了什么 | 价值 |
|---|---|---|---|---|
| 1 | BindListener | `ConfigurationPropertiesBindHandlerAdvisor` | 给 `Binder.bind()` 加了生命周期钩子 | 可观察、可干预的配置绑定 |
| 2 | FailureAnalyzer | `FailureAnalyzer` + `spring.factories` | 把 classpath 冲突变成可操作的 mvn 命令 | 错误诊断即文档 |
| 3 | 最早启动干涉 | `SpringApplicationRunListener.starting()` | 在 bean 创建前 ban 掉冲突 artifact | 防止不可逆的类加载错误 |
| 4 | 自动配置过滤 | `AutoConfigurationImportFilter` | 运行时配置驱动的自动配置排除 | 框架级自动配置控制 |

**与前三个仓库的关系**：

```
confucius-commons  →  "JDK 缺什么框架工具"
microsphere-java   →  "框架基础设施应该有什么"
microsphere-spring →  "不依赖 Spring Boot 怎么复刻它的能力"
microsphere-spring-boot →  "已经用了 Spring Boot，还能在哪儿做增强"
```

这是一个**层层递进**的体系——从补 JDK 的缺口，到建设框架基础设施，到复刻 Spring Boot 能力，到增强 Spring Boot 本身。
