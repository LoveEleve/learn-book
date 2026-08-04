# Classpath 三重索引 —— 启��时扫描还是查询时扫描？

> 源码对应：`ClassUtils.java`（322行）、`ClassPathUtils.java`（131行）
> microsphere-java 继承：`ClassDataRepository.java`（几乎 1:1 复制）
> 核心取舍：用启动时间和内存（~8MB）换取 O(1) 查询性能

---

## 一、框架在运行时反复问的三个问题，答案都藏在三重索引中

### 1.1 三个高频查询

```
Q1: "com.example.UserService 这个类的 class 文件在哪个 jar 里？"
  → 日志中报了 NoClassDefFoundError，需要定位是哪个 jar 缺失

Q2: "/lib/spring-core-5.3.21.jar 这个 jar 里有哪些类？"
  → 框架启动时需要扫描所有 Bean 候选类

Q3: "com.example.service 这个包下面有哪些类？"
  → @ComponentScan 需要知道包下的所有类，逐一检查注解
```

JDK 没有直接回答这三个问题的 API。你能拿到 `ClassLoader.getResource("com/example/UserService.class")`——但这是「某个 CL 能找到的资源」，不是「查索引」。

### 1.2 两种方案

| | 惰性扫描（SimpleClassScanner） | 预计算索引（ClassUtils 三重索引） |
|---|---|---|
| 启动开销 | 零 | 扫描整个 classpath（200+ jar, 50K+ 类） |
| 查询 Q1（类→jar） | 用 `ProtectionDomain.getCodeSource()` → 必须先加载类 | O(1) Map.get(className) |
| 查询 Q2（jar→类） | O(N) 遍历 jar 的所有条目 | O(1) Map.get(jarPath) |
| 查询 Q3（包→类） | O(N) 遍历所有类名 → 检查包名前缀 | O(1) Map.get(packageName) |
| 内存占用 | 零 | ~8MB（5 万类） |

confucius-commons 选择了方案 2——**预计算索引**。这个方案在 microsphere-java 中被原样复制（`ClassDataRepository`），说明小马哥认为这个取舍是正确的。


## 二、永恒原理:预计算索引 vs 惰性查询

「框架反复问的三个问题」（§1.1）本质是三种查询：按类名查 jar、按 jar 查类、按包查类。这三种查询可以用两种根本不同的策略实现：

**预计算索引（confucius-commons 的选择）**：启动时遍历整个 classpath，构建三个索引（类名->jar、jar->类名集合、包->类名集合），之后查询 O(1)。代价是启动时间和内存（~8MB for 200jar/50000类）。

**惰性查询（Spring ClassPathBeanDefinitionScanner 的选择）**：不预计算，每次查询时遍历 classpath。代价是每次查询都慢（遍历所有 jar），但零启动开销、零内存。

| 维度 | 预计算索引 | 惰性查询 |
|---|---|---|
| 启动开销 | 高（遍历 + 建索引） | 零 |
| 查询性能 | O(1) | O(n)（n = classpath 条目数） |
| 内存占用 | ~8MB（50000类） | 零 |
| 适合场景 | 查询频繁（如运行时反复扫描） | 查询少（如启动时扫一次 Bean） |

**这是「空间换时间」的经典架构模式**，在数据库（B+树索引 vs 全表扫描）、搜索引擎（倒排索引 vs 顺序搜索）、缓存（预加载 vs 按需加载）中反复出现。confucius-commons 选预计算因为它定位是「运行时基础设施」--查询频繁（每次类加载诊断都要查），预计算的 O(1) 查询价值大于 8MB 内存代价。

## 三、三重索引的构建机制

构建过程分三步，每步处理一个维度：

**Step 1:收集 classpath**。两个来源：Bootstrap classpath（`RuntimeMXBean.getBootClassPath()`，JDK 6-8 返回 rt.jar 等，JDK 9+ 不再支持返回空）+ Application classpath（`RuntimeMXBean.getClassPath()`，即 `-cp` 指定的 jar/目录）。用 `File.pathSeparator` 分割（Linux `:` Windows `;`）。

**Step 2:逐条目提取类名**。每个 classpath 条目是目录或 jar：
- 目录（如 `target/classes/`）：`SimpleFileScanner` 递归扫描 `.class` 文件，路径转类名（`com/example/Foo.class` -> `com.example.Foo`）。
- jar 文件：`SimpleJarEntryScanner` 扫描 jar 内 `.class` 条目，同样路径转类名。
- 路径转类名：`/` -> `.`，去掉 `.class` 后缀，去前导 `.`。

**Step 3:构建三个索引**：
- **反向索引**（类名 -> classPath）：`Map<String, String>`，O(1) 回答「这个类在哪个 jar」。Spring 的 `ClassPathResource` 也做类似映射但每次惰性查询。
- **正向索引**（classPath -> 类名集合）：`Map<String, Set<String>>`，O(1) 回答「这个 jar 有哪些类」。
- **聚合索引**（包名 -> 类名集合）：`Map<String, Set<String>>`，O(1) 回答「这个包下有哪些类」。通过遍历正向索引按包名前缀聚合。

三个索引共用同一份原始数据（classpath 遍历结果），只是不同的「键值映射」--这是「一份数据多个索引」的标准做法（数据库的表 + 多个索引是同构）。

## 四、内存代价的精确计算

### 3.1 类名字符串的内存开销

一个类名如 `"org.springframework.boot.autoconfigure.SpringBootApplication"` 在 Java 中占用的内存：

```
String 对象开销：对象头(12字节，压缩指针) + char[]引用(4字节) + hash(4字节) = 20字节
char[] 数组：数组头(16字节) + 46个字符 × 2字节 = 108字节
总计：~128 字节/一个类名
```

### 3.2 一个典型 200 jar、50,000 类的项目

```
classPathToClassNamesMap:
  200 entries × (50 字节 key + 250 类名 × 128 字节 avg)
  = 200 × (50 + 32,000)
  ≈ 6.4 MB

classNameToClassPathsMap:
  50,000 entries × (128 字节类名 + 50 字节 classPath)
  = 50,000 × 178
  ≈ 8.9 MB

packageNameToClassNamesMap:
  ~500 packages × (50 字节 packageName + references)
  ≈ 1.5 MB

总计: ~17 MB（不是之前粗略估算的 8MB——String 的内部开销被低估了）
```

**优化空间**：

1. **不索引 JDK 类**（Bootstrap classpath 上的类）：rt.jar 中有 10,000+ 个类，这些不太需要查「在哪个 jar」
2. **用 `String.intern()` 共享重复类名**：`String.intern()` 把相同内容的 String 指向同一个常量池对象
3. **用 fastutil 的 `Object2ObjectOpenHashMap` 替代 `HashMap`**：内存占用减少 30-40%

### 3.3 更新——microsphere-java ClassDataRepository 的优化

microsphere-java 把三个静态 Map 改成了 Singleton 实例的 final Map。这本身不减少内存，但允许**惰性初始化**——`getClassNamesInClassPath()` 中如果缓存中不存在就调用 `findClassNamesInClassPath()` 动态加载，而不是启动时一次性构建。

```java
// microsphere-java ClassDataRepository.java:133-140
public Set<String> getClassNamesInClassPath(String classPath, boolean recursive) {
    Set<String> classNames = classPathToClassNamesMap.get(classPath);
    if (isEmpty(classNames)) {
        classNames = findClassNamesInClassPath(classPath, recursive);
        // ← 惰性加载：首次访问这个 classPath 时才扫描
    }
    return classNames;
}
```


## 五、生产案例分析——当预计算索引 break 时

### 5.1 场景1：Spring Boot fat jar —— 为什么 `new JarFile("app.jar")` 不行

Spring Boot 的 fat jar 不是普通 jar——它是一个**嵌套 jar**：

```
myapp.jar
├── META-INF/
│   └── MANIFEST.MF          ← Main-Class: JarLauncher
├── BOOT-INF/
│   ├── classes/              ← 你的编译后的类
│   │   └── com/example/Application.class
│   └── lib/
│       ├── spring-core-5.3.21.jar   ← 内嵌 jar！
│       ├── spring-boot-2.7.4.jar
│       └── ...
└── org/springframework/boot/loader/
    ├── JarLauncher.class
    ├── LaunchedURLClassLoader.class
    └── jar/
        ├── JarFile.class      ← Spring Boot 自带的 JarFile（不是 java.util.jar.JarFile）
        └── JarEntry.class
```

**为什么 JDK 的 `JarFile` 无法处理？**

`java.util.jar.JarFile` 能打开普通 jar，遍历其条目 `com/example/Foo.class`。但对于 fat jar，它遍历到的条目是：

```
BOOT-INF/lib/spring-core-5.3.21.jar  ← 这是一个文件，不是目录！
```

JDK 的 `JarFile` 会返回这个 jar 文件本身作为条目——它不知道要把这个 jar 再打开、递归遍历其中的类。

**Spring Boot 怎么解决的？**

Spring Boot 自带了 `org.springframework.boot.loader.jar.JarFile`——它重写了 JDK 的 `JarFile`，能识别内嵌 jar 并自动递归打开它们。`JarLauncher` 在启动时创建 `LaunchedURLClassLoader`，把 `BOOT-INF/lib/` 下的每个内嵌 jar 注册为一个单独的 URL 条目：

```java
// Spring Boot JarLauncher 的核心逻辑（简化）：
JarFile jarFile = new JarFile("myapp.jar");
JarEntry libEntry = jarFile.getJarEntry("BOOT-INF/lib/");

// 遍历 BOOT-INF/lib/ 下的每个嵌套 jar
for (JarEntry nestedJar : jarFile.getNestedJars(libEntry)) {
    URL url = new URL("jar:file:myapp.jar!/BOOT-INF/lib/" + nestedJar.getName() + "!/");
    classLoader.addURL(url);  // 每个嵌套 jar 注册为独立的 URL
}
```

`LaunchedURLClassLoader` 使用这些 URL 来解析类——每个嵌套 jar 都有自己独立的 classpath 条目。

**confucius-commons 怎么处理 fat jar？**

答案是：**不能直接处理**。`ClassUtils.findClassNamesInClassPath("myapp.jar", true)` 调用 `new JarFile("myapp.jar")`——这是 JDK 的 `JarFile`，无法递归到嵌套 jar。你需要：

```java
// 方案1：使用 Spring Boot Loader 的 JarFile
import org.springframework.boot.loader.jar.JarFile;

JarFile bootJar = new JarFile("myapp.jar");
for (JarEntry entry : bootJar.entries()) {
    if (entry.getName().endsWith(".class")) {
        // 处理类
    }
}

// 方案2：在 fat jar 场景降级——只扫描 BOOT-INF/classes/
// 放弃 BOOT-INF/lib/ 中的类（通常是第三方依赖，不需要索引）
```

### 5.2 场景2：类加载期间触发了自身的死锁

`ClassUtils` 的三个静态 Map 在**类加载期间**构建。类加载本身需要这个 ClassLoader 的锁。如果在构建索引时扫描的 jar 中的某个类恰好也需要同一个 ClassLoader 的锁——死锁。

```java
// 死锁场景：
// 1. 线程A: 加载 ClassUtils.class → 持有 AppCL 的锁
// 2. ClassUtils static {} 块扫描 classpath → 访问 spring-core.jar
// 3. spring-core.jar 中某类的加载 → 也需要 AppCL 的锁 → 死锁！
```

**实际概率**：很低。因为 `findClassNamesInJarFile()` 只遍历 jar 条目名字，不加载类——所以不会触发其他类的加载。死锁只在扫描过程调用了 `Class.forName()` 或 `loadClass()` 才会发生——confucius-commons 没有这样做。

### 5.3 场景3：classpath 包含不存在的路径

```java
// 如果 CLASSPATH 中有一条不存在的路径（已删除的 jar）
// → ClassUtils.findClassNamesInClassPath("ghost.jar", true)
// → File("ghost.jar").exists() → false
// → 返回空的 classNames 集合
// → 三重索引中缺失了这个 jar 的所有类
// → 查询 "com.example.MissingClass" → 找不到 classPath
// → 返回 null → 调用方以为是类不存在，实际是 jar 不存在
```

防御：`findClassNamesInJarFile()` 中应该先检查 jar 是否存在：

```java
if (!jarFile.exists()) {
    return Collections.emptySet();  // 至少不要抛异常
}
```


## 六、面试要点

**Q1：「你在运行时怎么知道某个类来自哪个 jar？不加载这个类本身。」**

答案：两种方式。① 启动时扫描所有 jar 的条目，根据文件名推断类名（`com/example/Foo.class → com.example.Foo`），建立类名→jar 的反向索引。② 运行时用 `ProtectionDomain.getCodeSource().getLocation()`——但必须先加载类。confucius-commons 选了方案①——预计算索引。

**Q2：「三重索引（classPath→类/类→classPath/包→类）的内存开销多大？值不值得？」**

答案：5 万类的项目约 17MB。如果框架频繁查询类元数据（如 Spring 的 @ComponentScan），启动时一次构建后续 O(1) 比每次 O(N) 扫描更优——用内存换时间。如果只查询少数几个类（如一次启动时检查），惰性扫描更优。

追问：「String 对象在内存中到底占多少字节？为什么 50 字符的类名可能占 128 字节？」
回答：String 对象开销（12字节对象头 + 4字节引用 + 4字节 hash + 4字节压缩）= 约 24 字节。内部的 `char[]` 额外占 16字节数组头 + 字符数×2字节。50 字符的类名 = 24 + 16 + 100 = ~140 字节。加上 HashMap 的 Entry 开销（32字节），每个类名条目约 170 字节。

**Q3：「为什么不扫 JDK 核心 jar（rt.jar）？怎么判断哪些是 JDK 的 jar？」**

答案：JDK 的 jar 被 Bootstrap ClassLoader ���载——`ClassPathUtils.getBootstrapClassPaths()` 返回它们的路径（`rt.jar`、`tools.jar` 等）。跳过这些 jar 减少约 40% 的索引数据量——因为 JDK 的类不太需要「在哪个 jar」这个诊断信息。而且扫描 rt.jar（20,000+ 个类）的启动开销很大——约 500ms-1s。
