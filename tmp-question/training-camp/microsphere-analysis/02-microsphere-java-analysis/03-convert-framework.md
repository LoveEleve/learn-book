# 类型转换 -- 从 `Integer.parseInt` 到无容器转换框架

> 主题：类型转换有三个难度递增的层次（基本类型 / 对象 / 集合一对多），为什么 Spring `ConversionService` 在无容器场景下不可用，转换策略的三种发现机制（SPI / DI / 手动注册）各自的取舍。microsphere 的 `Converter` 是无容器 + SPI 发现路线的一个实例。
> 关联：泛型自识别机制与 [§8 序列化](./08-serialization-spi.md) §3 同构（类型擦除下的分发难题），Prioritized 排序与 [§4](./04-spi-prioritized.md) 共享元机制。

---

## 一、类型转换:看似简单实则三个层次

### 1.1 从 `Integer.parseInt` 说起

```java
public static void main(String[] args) {
    int port = Integer.parseInt(args[0]);      // String -> int
    long timeout = Long.parseLong(args[1]);     // String -> long
    boolean debug = Boolean.parseBoolean(args[2]); // String -> boolean
}
```

命令行参数全是 `String`，但业务需要 `int`/`long`/`boolean`。JDK 给了 `Integer.parseInt` 等基本类型转换方法，够用--但这只是类型转换的**最浅一层**。

### 1.2 类型转换的三个层次

类型转换看似一个需求，实际有三个难度递增的层次：

| 层次 | 转换方向 | 难度 | 代表方案 |
|---|---|---|---|
| 基本类型转换 | `String -> int/long/boolean/...` | 低 | `Integer.parseInt`、microsphere `StringToIntegerConverter` |
| 对象转换 | `Entity -> DTO`、`Map -> Bean` | 高 | MapStruct、BeanUtils.copyProperties |
| 集合一对多转换 | `String "1,2,3" -> List<Integer> {1,2,3}` | 中 | microsphere `MultipleConverter`（Spring `ConversionService` 做不了） |

**三个层次的难度差异**：

- **基本类型转换**：源和目标都是单一值，转换逻辑简单（`Integer.parseInt`）。难点在「统一入口」--不要每个类型手写一个 `parseXxx`。
- **对象转换**：源和目标有多个字段，需要字段映射、类型适配、可能嵌套。这是 MapStruct/BeanUtils 的领域，不在本文讨论范围。
- **集合一对多转换**：源是单一值（`String "1,2,3"`），目标是集合（`List<Integer>`）。需要「拆分 + 逐元素转换 + 收集」三步。**Spring `ConversionService` 不支持这个**--它是一对一的。

microsphere 的 `Converter` 框架覆盖第一层和第三层（基本类型 + 集合一对多），不覆盖第二层（对象转换交给 MapStruct/BeanUtils）。本文讲透背后的三个永恒原理。

### 1.3 Spring `ConversionService` 的容器依赖

Spring 的 `ConversionService` 是 Java 生态最常用的类型转换框架，但它有个隐含前提：**必须有 Spring 容器**。

```java
// ConversionService 的实例化链路:
ApplicationContext context = SpringApplication.run(App.class, args);
ConversionService cs = context.getBean(ConversionService.class);
// ↑ 必须先创建 ApplicationContext -> 配置 AutoConfiguration -> 扫描组件 -> 启动容器
```

如果你只是想验证 `java -jar app.jar --port=8080` 中 `8080` 是合法整数--在 Spring 容器启动之前--你只能手写 `Integer.parseInt()`。`ConversionService` 拿不到，因为它在容器里。

**三种场景同样无法用 Spring**：

- **命令行工具**：`main()` 启动，没有容器。
- **嵌入式 SDK**：给第三方用的 jar 包，不能要求对方启动 Spring。
- **框架启动参数校验**：`SpringApplication.run()` 之前就要校验配置参数。

microsphere 的 `Converter` 解决的就是这个「**无容器场景下的类型转换**」--通过 SPI + 静态方法工作，`Converter.convertIfPossible("8080", Integer.class)` 不需要任何容器。


## 二、永恒原理一:类型转换的「三个层次」与集合一对多的难题

### 2.1 基本类型转换:统一入口的需求

JDK 给了 `Integer.parseInt`/`Long.parseLong`/`Boolean.parseBoolean` 等方法，但它们散落在不同类里，没有统一入口。你想写一个通用方法「把 String 转成任意基本类型」，得 if-else：

```java
// 没有 Converter 框架时的写法
Object convert(String source, Class<?> targetType) {
    if (targetType == Integer.class) return Integer.parseInt(source);
    if (targetType == Long.class) return Long.parseLong(source);
    if (targetType == Boolean.class) return Boolean.parseBoolean(source);
    // ... 每加一个类型就要加一个 if
}
```

**问题**：违反开闭原则，每加一个类型要改源码。microsphere 的解法是 `Converter<S, T>` 接口 + SPI 自动发现--每个类型对（`String->Integer`）一个 Converter 实现，SPI 加载，泛型自识别类型对。

### 2.2 集合一对多转换:Spring `ConversionService` 的空白

Spring `ConversionService` 是一对一的：`String "1,2,3"` -> `Integer`（假设有合适的 Converter）。它**不能**把 `"1,2,3"` 转成 `List<Integer> {1,2,3}`--因为源是一个 String，目标是包含多个 Integer 的 List，这不是「一对一」。

```java
// Spring ConversionService 做不了:
List<Integer> list = conversionService.convert("1,2,3", List.class);
// -> 要么报错,要么返回 List<String> ["1,2,3"](整个字符串作为一个元素)
```

microsphere 的 `MultipleConverter` / `MultiValueConverter` 解决这个：

```java
// microsphere 的 MultipleConverter
List<Integer> list = Converter.convertIfPossible("1,2,3", List.class);
// -> [1, 2, 3]  (拆分 + 逐元素转 Integer + 收集到 ArrayList)
```

### 2.3 一对多转换的「拆分-转换-收集」三步

一对多转换的核心是三步：

```
"1,2,3"
  ↓ Step 1: 拆分(按分隔符)
["1", "2", "3"]
  ↓ Step 2: 逐元素转换(每个 String -> Integer)
[1, 2, 3]
  ↓ Step 3: 收集(到目标集合类型)
ArrayList<Integer> [1, 2, 3]
```

**Step 3 的关键**：目标集合类型不同（`List`/`Set`/`Queue`/`Deque`），需要不同的收集策略：

- `List` -> `ArrayList`
- `Set` -> `LinkedHashSet`（保序去重）
- `Queue` -> `LinkedList`
- `Deque` -> `ArrayDeque`

microsphere 用一个抽象基类 `StringToCollectionConverter<T extends Collection>` 处理公共逻辑（拆分 + 逐元素转换），子类只指定「创建什么集合」：

```java
// 抽象基类:处理拆分 + 逐元素转换
abstract class StringToCollectionConverter<T extends Collection> implements MultiValueConverter<String, T> {
    protected void convert(String source, Collection target, Class<?> elementType) {
        for (String element : source.split(",")) {
            target.add(convertElement(element.trim(), elementType));
        }
    }
}
// 子类:只指定集合类型
class StringToListConverter extends StringToCollectionConverter<List> { /* 创建 ArrayList */ }
class StringToSetConverter extends StringToCollectionConverter<Set> { /* 创建 LinkedHashSet */ }
// ... 14 个集合子类型各一个 Converter
```

**这是「模板方法模式」的经典应用**：公共算法（拆分-转换-收集）在基类，可变步骤（创建什么集合）在子类。14 个集合子类型共用一套算法，只差一行「new 什么集合」。

### 2.4 为什么 Spring 不做一对多

Spring `ConversionService` 不做一对多不是疏忽，是设计选择：

- **一对一语义清晰**：`convert(source, targetType)` 的语义是「把 source 转成 targetType 的一个实例」。一对多破坏这个语义（一个 source 变出多个 target）。
- **一对多需要额外信息**：拆分分隔符是什么（`,` 还是 `;`）？元素类型是什么（`List<Integer>` 还是 `List<String>`）？这些信息 `Class<List>` 不携带（Java 泛型擦除），需要额外参数。Spring 选择不做，把复杂度留给调用方。

microsphere 选择做一对多，代价是 Converter 接口更复杂（`MultiValueConverter` 继承体系）、语义不统一（一对一和一对多混在一个框架里）。**这是「覆盖面 vs 语义清晰」的权衡**。


## 三、永恒原理二:「容器依赖」与无容器场景的困境

### 2.1 的延伸:为什么无容器场景需要类型转换

「容器依赖」不只是类型转换的问题，是所有「框架级基础设施」的共同难题。Spring 提供了大量基础设施（类型转换、事件、配置绑定、AOP），但它们都**绑定在容器上**--容器没启动就用不了。

**无容器场景的清单**：

| 场景 | 为什么没有容器 | 需要什么基础设施 |
|---|---|---|
| `main()` 第一行 | 容器还没启动 | 命令行参数类型转换 |
| 命令行工具 | 不需要容器 | 全套类型转换 |
| 嵌入式 SDK | 不能要求用户启动容器 | 类型转换、事件、日志 |
| 框架启动参数校验 | `SpringApplication.run()` 之前 | 类型转换、配置校验 |
| 框架启动早期事件 | ApplicationContext 创建前 | 事件总线（[§6](./06-event-system.md)） |

**microsphere 的整体定位就是「无容器基础设施」**：类型转换（本文）、事件总线（[§6](./06-event-system.md)）、日志门面（[§10](./10-logging-abstraction.md)）、序列化（[§8](./08-serialization-spi.md)）--全部不依赖 Spring 容器，通过 SPI + 静态方法工作。这是 microsphere-java-core 作为「零依赖基础库」的核心价值。

### 3.2 无容器类型转换的设计约束

无容器场景对类型转换框架有几个约束：

1. **零依赖**：不能依赖 `spring-core`（基础库定位）。
2. **静态可用**：`Converter.convertIfPossible(...)` 必须是静态方法，不需要先 new 任何对象。
3. **自动发现**：不能手动注册（没有容器帮你注册），必须 SPI 自动发现所有 Converter。
4. **线程安全**：Converter 实例必须无状态（多线程共享），SPI 发现过程只读不可变列表。

这些约束决定了 microsphere Converter 的设计：`@FunctionalInterface` 接口 + SPI 自动发现 + 泛型自识别 + Prioritized 排序 + 静态方法入口。每个设计选择都是约束倒推的结果。


## 四、永恒原理三:转换策略的「发现机制」--SPI vs DI vs 手动注册

有了 Converter 实现后，怎么找到「sourceType -> targetType」对应的 Converter？这是「策略发现」问题，有三种解法。

### 4.1 三种发现机制

**机制一:DI 注入（Spring ConversionService）**

```java
@Autowired
private ConversionService conversionService;  // 容器注入
```

容器启动时扫描 `Converter` 实现，注册到 `ConversionService`。调用方通过 DI 拿到 `ConversionService`，调 `convert(source, targetType)`。**优点**：类型安全、容器管理生命周期。**缺点**：必须有容器。

**机制二:手动注册（Guava / Commons ConvertUtils）**

```java
// Commons ConvertUtils
ConvertUtils.register(new MyConverter(), Integer.class);  // 手动注册
ConvertUtils.convert("8080", Integer.class);              // 查注册表
```

调用方自己 new Converter、自己注册到注册表。**优点**：无容器、可控。**缺点**：每个调用方都要手动注册，重复代码；注册表是全局可变状态，线程安全难保证。

**机制三:SPI 自动发现（microsphere Converter）**

```java
// microsphere: SPI 自动发现 + 静态方法
Integer port = Converter.convertIfPossible("8080", Integer.class);
// 内部: loadServicesList(Converter.class) 从 META-INF/services 加载所有 Converter
//       按 Prioritized 排序
//       遍历找第一个 accept(sourceType, targetType) == true 的
```

`META-INF/services/io.microsphere.convert.Converter` 声明所有实现，SPI 加载、排序、匹配全自动。调用方只需调静态方法。**优点**：无容器、零配置、自动发现。**缺点**：SPI 加载有一次性开销（首次调用时扫描所有 jar 的 services 文件）。

### 4.2 泛型自识别:SPI 发现的关键

SPI 加载了所有 Converter 后，怎么知道每个 Converter 处理什么类型对？--泛型自识别。

```java
public class StringToIntegerConverter implements Converter<String, Integer> {
    public Integer convert(String source) { return Integer.parseInt(source); }
}
// Converter 接口的默认方法:
default Class<S> getSourceType() {
    return TypeUtils.resolveActualTypeArgumentClass(getClass(), Converter.class, 0);  // -> String.class
}
default Class<T> getTargetType() {
    return TypeUtils.resolveActualTypeArgumentClass(getClass(), Converter.class, 1);  // -> Integer.class
}
```

**这套机制和 [§8 序列化](./08-serialization-spi.md) §3 的 `resolveTypeArgumentClasses` 完全同构**--都是通过 `Class.getGenericInterfaces()` 读取字节码 `Signature` 属性恢复被擦除的泛型类型。陷阱也相同：继承链擦除会导致解析失败（`MyConverter extends BaseConverter<String, Integer>` 需要沿继承链解析）。详见 [§8 §3.3](./08-serialization-spi.md) 的逐步拆解。

**microsphere 的通用模式**：`Converter`、`Serializer`、`EventListener` 三个接口都用泛型自识别 + SPI 发现。这是 microsphere「极简接口 + 富注册表」风格的统一体现。

### 4.3 Prioritized 排序:同类型对的多个 Converter

如果一个类型对（`String -> Object`）有多个 Converter，选哪个？--按 `Prioritized` 排序，优先级高的先被遍历（[§4](./04-spi-prioritized.md)）。

**microsphere 内置的 52 个 Converter 实际上没有重叠**--每种类型对只有一个 Converter（`StringToIntegerConverter` 不会 `accept(String, Long)`）。所以优先级在内置 Converter 间不起作用。

**优先级在用户自定义场景起作用**：用户注册一个高优先级的 `StringToIntegerConverter`（priority=0，低于内置的 `NORMAL_PRIORITY`），会排在内置 Converter 之前先被 `accept` 检查--**用户自定义覆盖内置**。这是「用户优先于框架」的优先级哲学（与 [§1](./01-artifact-detection.md) §3.3 的 Resolver 优先级同构）。

### 4.4 三种发现机制的取舍

| 机制 | 容器依赖 | 自动发现 | 类型安全 | 适合场景 |
|---|---|---|---|---|
| DI 注入（Spring） | 需要 | 容器扫描 | ✅ | Spring 应用 |
| 手动注册（Guava/Commons） | 不需要 | ❌ 手动 | 取决于 API | 老旧项目 |
| SPI 自动发现（microsphere） | 不需要 | ✅ | ✅（泛型自识别） | 无容器场景、基础库 |

**选型原理**：有容器用 DI（最干净）；无容器且类型对固定用手动注册（最简单）；无容器且类型对可扩展用 SPI（最灵活）。microsphere 选 SPI 因为它是「零依赖基础库」，类型对应可扩展（用户可注册自定义 Converter）。


## 五、microsphere 作为「无容器类型转换」的一个实例

讲完三个原理，microsphere 的 `Converter` 就是这些原理的一次落地。

**实例一:三层次覆盖（§2 原理的落地）**

52 个内置 Converter 覆盖第一层（基本类型：`StringToIntegerConverter` 等 8 个）和第三层（集合一对多：`StringToListConverter` 等 14 个）。不覆盖第二层（对象转换）。内置 Converter 通过 `META-INF/services/io.microsphere.convert.Converter` 声明，SPI 自动加载。

**实例二:无容器静态入口（§3 原理的落地）**

`Converter.convertIfPossible(source, targetType)` 是静态方法，内部通过 SPI 加载所有 Converter（首次调用时加载，之后复用）。调用方不需要 new 任何对象，不需要容器--`main()` 第一行就能用。

**实例三:SPI + 泛型自识别 + Prioritized（§4 原理的落地）**

SPI 加载所有 Converter，`resolveActualTypeArgumentClass` 从泛型签名恢复 `S`/`T`，按 `Prioritized.COMPARATOR` 排序。`findConverter(sourceType, targetType)` 遍历排序后的列表，第一个 `accept(sourceType, targetType) == true` 的胜出。用户自定义 Converter 设 priority < `NORMAL_PRIORITY`(0) 可覆盖内置。


## 六、实例批判:这个实现的缺陷

作为原理的一个落地实例，microsphere 的实现也有瑕疵。

1. **`convertIfPossible` 不做 null 保护**：`Converter.convertIfPossible(null, Integer.class)` -> `getConverter(null.getClass())` -> `NullPointerException`。调用方必须自己做 null 检查。
2. **异常直接传播**：`StringToIntegerConverter.convert("abc")` -> `NumberFormatException` 直接抛到调用方，不做 catch。`convertIfPossible` 名字暗示「可能失败」，但实际不 catch--语义和名字不符。
3. **空字符串行为不直观**：`Integer.parseInt("")` 抛 `NumberFormatException`，`StringToIntegerConverter` 不做特殊处理--和 `Integer.parseInt` 行为一致，但用户可能期望空字符串返回 null 或 0。
4. **同优先级排序不可靠**：`Collections.sort` 稳定排序--同优先级保持声明顺序。但声明顺序取决于 SPI 文件的加载顺序（文件系统决定），跨环境不可靠。应避免两个 Converter 用相同 priority。
5. **52 个内置 Converter 无重叠**：优先级机制在内置 Converter 间不起作用（每种类型对只有一个）。优先级只在「用户自定义覆盖内置」时有用--文档应说明这一点。

这些不是原理错误，而是原理在具体代码里的实现瑕疵。


## 七、与其他方案的原理对比

| 方案 | 发现机制 | 容器依赖 | 一对多 | 类型安全 | 适合场景 |
|---|---|---|---|---|---|
| microsphere Converter | SPI + 泛型自识别 | 无 | ✅ `MultipleConverter` | ✅ | 无容器场景、基础库 |
| Spring ConversionService | DI 容器扫描 | 需要 | ❌ | ✅ | Spring 应用 |
| Guava Converter | 手动 new | 无 | ❌（要求双向） | ✅ | 双向转换 |
| Commons ConvertUtils | 手动注册 | 无 | ❌ | ❌（Object 返回值） | 老旧项目 |
| MapStruct | 编译期注解处理 | 无 | ❌ | ✅（编译期验证） | Entity↔DTO 对象转换 |
| JDK Function | 手动管理 | 无 | ❌ | ❌（无类型检查） | 最简单的 String->int |

**原理层面的取舍**：

- **Spring ConversionService** 假设「你在容器内」：DI 给你注入 `ConversionService`，容器管理 Converter 生命周期。简单但要求容器。不支持一对多（设计选择，非缺陷）。
- **Guava Converter** 强制双向：`Converter<A,B>` 要求同时提供 A->B 和 B->A。很多转换不需要双向（如 `String -> File` 的反转换无意义），强制双向是过度设计。
- **MapStruct** 走编译期：注解处理器在编译期生成转换代码，运行时零反射。适合 Entity↔DTO 的复杂对象转换（第二层），但不适合简单类型转换（第一层不需要编译期代码生成）。
- **JDK Function** 最简：`Function<String, Integer> toInt = Integer::parseInt`。零依赖、JDK 内置。但没有 `accept()` 类型匹配检查，没有自动发现--需要手动管理 Function 对象。

microsphere 的定位：**无容器场景下的类型转换，覆盖基本类型 + 集合一对多，SPI 自动发现**。它不替代 Spring（Spring 在容器内更好用）、不替代 MapStruct（MapStruct 在对象转换更好用），而是填补「无容器 + 基本类型 + 集合」这个空白。


## 八、面试要点

**Q1：「Spring 的 ConversionService 很好用，为什么 microsphere 还要自己写一套 Converter？」**

答案：Spring ConversionService 需要 ApplicationContext--`ctx.getBean(ConversionService.class)` 依赖容器。但 main() 第一行、命令行工具、嵌入式 SDK、框架启动参数校验（`SpringApplication.run()` 之前）这些场景没有容器。microsphere 的 Converter 通过 SPI + 静态方法工作--`Converter.convertIfPossible("8080", Integer.class)` 不需要任何容器。microsphere-java-core 定位是零依赖基础库，不能依赖 spring-core。

**Q2：「类型转换有几个层次？Spring ConversionService 能覆盖哪些？」**

答案：三个层次。① 基本类型转换（String->int/long/boolean）--JDK 的 parseInt 能做，Spring 也能做。② 对象转换（Entity->DTO）--MapStruct/BeanUtils 的领域，Spring 的 Converter 也能做但不如 MapStruct。③ 集合一对多转换（String "1,2,3" -> List<Integer>）--Spring ConversionService 做不了，因为它是一对一的。microsphere 的 MultipleConverter 用「拆分-逐元素转换-收集」三步解决一对多，14 个集合子类型（List/Set/Queue/Deque...）各一个 Converter，用模板方法模式复用公共逻辑。

**Q3：「Converter 怎么知道自己的源类型和目标类型？泛型不是被擦除了吗？」**

答案：泛型自识别。和 [§8 序列化](./08-serialization-spi.md) §3 的 `resolveTypeArgumentClasses` 完全同构--通过 `Class.getGenericInterfaces()` 读取字节码 `Signature` 属性，恢复被擦除的泛型类型参数。`StringToIntegerConverter implements Converter<String, Integer>` 的泛型签名保存在字节码里，运行时可读出。陷阱是继承链擦除--`MyConverter extends BaseConverter<String, Integer>` 需要沿继承链逐级解析泛型映射表，失败时静默返回空。

**Q4：「转换策略的发现机制有几种？microsphere 选 SPI 的原因是什么？」**

答案：三种。① DI 注入（Spring）--容器扫描 Converter 实现，注册到 ConversionService，调用方通过 DI 拿到。优点类型安全、容器管理生命周期，缺点必须有容器。② 手动注册（Guava/Commons）--调用方自己 new Converter 注册到注册表。优点无容器可控，缺点重复代码、注册表是全局可变状态。③ SPI 自动发现（microsphere）--`META-INF/services` 声明实现，SPI 加载排序匹配全自动。优点无容器零配置，缺点首次 SPI 扫描有开销。microsphere 选 SPI 因为它是零依赖基础库，类型对应可扩展（用户可注册自定义 Converter），且无容器场景下 DI 不可用、手动注册太繁琐。

**Q5：「如果两个 Converter 都 accept 同样的类型对，哪个会被选中？」**

答案：按 `Prioritized` 排序，优先级高（数值小）的先被遍历，第一个 `accept == true` 的胜出（[§4](./04-spi-prioritized.md)）。microsphere 内置 52 个 Converter 实际上没有重叠--每种类型对只有一个，优先级在内置间不起作用。优先级在「用户自定义覆盖内置」时起作用：用户注册一个 priority < `NORMAL_PRIORITY`(0) 的自定义 Converter，会排在内置之前先被检查--用户优先于框架。注意 `MAX_PRIORITY = MIN_VALUE`（最高优先级 = 最小数值），命名陷阱见 [§4 勘误](./04-spi-prioritized.md)。

**Q6：「microsphere Converter 和 MapStruct 各适合什么场景？」**

答案：两者解决不同层次的类型转换。microsphere Converter 覆盖第一层（基本类型 String->int）和第三层（集合一对多 String->List<Integer>），通过 SPI 运行时发现，适合无容器场景的简单类型转换。MapStruct 覆盖第二层（对象转换 Entity->DTO），通过注解处理器在编译期生成转换代码，运行时零反射，适合复杂对象转换。两者不冲突--microsphere 处理简单类型 + 集合，MapStruct 处理复杂对象。选 microsphere：无容器 + 基本类型 + 集合一对多。选 MapStruct：Entity↔DTO 字段映射。

---

> **与 §8 序列化的关联**：`Converter<S,T>` 和 `Serializer<S>`（[§8](./08-serialization-spi.md)）共享同一套设计哲学--泛型自识别 + SPI + Prioritized。两者的 `resolveTypeArgumentClasses`/`resolveActualTypeArgumentClass` 调的都是 `reflect/TypeUtils`，底层共用。陷阱也相同（继承链擦除导致静默失败）。
> **与 §6 事件系统的关联**：`Converter`、`Serializer`、`EventListener` 三个接口都用泛型自识别 + SPI 发现，是 microsphere「极简接口 + 富注册表」风格的统一体现。`EventListener` 的 `findEventType`（[§6](./06-event-system.md)）和 `Converter` 的 `getSourceType` 是同一机制。
