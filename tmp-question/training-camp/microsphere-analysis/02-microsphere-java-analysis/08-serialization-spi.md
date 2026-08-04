# 序列化 -- Java 序列化的兴衰，以及「类型擦除下的分发」这个永恒难题

> 主题：为什么 Java 自带的序列化从「核心特性」变成「安全隐患」，业界演化出多少种替代方案，以及为什么「按类型选序列化器」这个需求在所有 Java 框架里都遇到同一个根本难题（类型擦除）。microsphere-io 的 Serializer/Deserializer 只是这条脉络上一个轻量实例。
> 关联：与 [§3 转换框架](./03-convert-framework.md) 共享「泛型自识别」机制，与 [§4 SPI+Prioritized](./04-spi-prioritized.md) 共享优先级排序。

---

## 一、Java 序列化:从「对象图持久化」到「安全隐患」

### 1.1 1996 年的设计初衷

Java 序列化（`ObjectOutputStream`/`ObjectInputStream`，JDK 1.1，1997）的设计目标有两个：

1. **对象图持久化**：把一个对象连同它引用的所有对象（整个对象图）转成字节流，存盘或传输。
2. **RMI 远程调用**：RMI（远程方法调用）需要把方法参数从客户端序列化到服务端--序列化是 RMI 的底层基础。

这套设计带着 90 年代「对象无处不在」的愿景--对象不仅能存在于内存，还能「流动」。`Serializable` 标记接口、`writeObject`/`readObject` 钩子、`serialVersionUID` 版本号、`transient` 排除字段，构成了一套「自描述」的序列化协议：字节流里编码了类描述符（`TC_OBJECT` + 类名 + 字段签名），反序列化时根据描述符还原对象。

### 1.2 四个原罪

这套「自描述」设计带来了四个摆脱不掉的包袱：

1. **强耦合 `Serializable`**：不实现这个标记接口就 `NotSerializableException`。第三方库的类（JPA Entity 代理、早期 `Optional`）常常没实现，你没法序列化。
2. **`serialVersionUID` 版本号地狱**：发送端和接收端 UID 不一致就 `InvalidClassException`。生产环境类改了一个字段，反序列化旧数据就崩。UID 解决不了「字段语义变化」（§4.2）。
3. **`readObject` 反序列化漏洞史**：字节流里编码了「要实例化什么类、调用什么方法」--`readObject` 会实例化任意类、调用 `readObject`/`readResolve` 等钩子。攻击者构造 gadget chain（如 Apache Commons Collections 2015 漏洞）可执行任意代码。这是反序列化漏洞重灾区。
4. **不可插拔**：序列化逻辑写死在 `ObjectOutputStream` 里。想给某类型换 JSON？只能外面包一层，无法替换底层。

### 1.3 业界多年的「废弃」呼声

Java 序列化的安全隐患严重到业界长期讨论废弃。Brian Goetz（Java 语言架构师）2018 年公开提及「deprecating serialization」。JDK 9 引入 `ObjectInputFilter`（JEP 290）作为缓解--允许在反序列化前过滤类名，但根本问题（`readObject` 能实例化任意类）未解。至今 Java 序列化仍可用，但生产环境的安全敏感场景已普遍改用 Kryo/Hessian/Protobuf 等不含「任意类实例化」语义的方案。

**这就是 microsphere-io 的 Serializer/Deserializer 框架的诞生背景**：把 Java 序列化降级为「最后手段」而非「默认选择」--通过 SPI 让用户能注册专用序列化器（String 用 UTF-8、POJO 用 JSON、特殊对象用 Kryo），Java 序列化只在「没有专用序列化器」时兜底。本文讲透这背后的四个永恒原理。


## 二、永恒原理一:序列化方案的「四维分类法」

业界序列化方案众多，怎么选？关键是看四个维度的取舍。

### 2.1 四个维度

| 维度 | 含义 | 为什么重要 |
|---|---|---|
| **schema 演进性** | 类改了字段（增/删/改类型）后，旧数据能否反序列化 | 长期运行系统的核心难题（§4） |
| **跨语言** | 字节流能否被非 Java 程序读取 | 多语言微服务必备 |
| **性能** | 序列化/反序列化速度 | 高频 RPC/缓存场景关键 |
| **紧凑度** | 字节流体积 | 网络传输/存储成本 |

**没有银弹**：四个维度互相冲突。schema 演进性好的（JSON）通常性能差、紧凑度低；性能好的（Kryo）通常 schema 演进性差、不跨语言。

### 2.2 主流方案在四维上的位置

| 方案 | schema 演进 | 跨语言 | 性能 | 紧凑度 | 典型场景 |
|---|---|---|---|---|---|
| Java serialization | 差（UID 卡死） | ❌ | 差 | 差 | RMI、历史代码 |
| Kryo / FST | 差（字段序号敏感） | ❌ | 极高 | 极高 | 高频 RPC、缓存 |
| Hessian | 中 | ✅（Java/Python/C++） | 中 | 中 | Dubbo RPC |
| Protobuf | 好（tag 机制） | ✅ | 高 | 高 | gRPC、跨语言微服务 |
| Avro | 好（schema 协商） | ✅ | 高 | 高 | Kafka、Hadoop |
| Cap'n Proto | 好 | ✅ | 极高（零拷贝） | 高 | 极致性能场景 |
| MessagePack | 中 | ✅ | 中 | 中 | 跨语言轻量场景 |
| JSON (Jackson) | 好 | ✅ | 差 | 差 | Web API、可读场景 |

**演化趋势**：从「自描述 + 强类型 + 不可演进」（Java serialization）->「注册 + 强类型 + 弱演进」（Kryo）->「schema 分离 + tag 演进」（Protobuf/Avro）。每一代都在补上一代的演进性短板。

**microsphere 内置只覆盖两端**：`DefaultSerializer`（Java 序列化，自描述兜底）+ `StringSerializer`（UTF-8，跨语言轻量）。中间地带（Kryo/Protobuf/JSON）需要用户自己注册--这符合「框架提供机制，用户填策略」的设计哲学。


## 三、永恒原理二:类型擦除下的「分发」难题

microsphere 的 `Serializers` 要做「按对象类型选序列化器」--序列化 `String` 用 `StringSerializer`，序列化 `MyPojo` 用 `MyPojoSerializer`。这需求听起来简单，在 Java 里却有个根本障碍：**泛型擦除**。

### 3.1 类型擦除的麻烦

```java
public interface Serializer<S> {
    byte[] serialize(S source) throws IOException;
}

public class StringSerializer implements Serializer<String> {
    public byte[] serialize(String source) { return source.getBytes(UTF_8); }
}
```

`StringSerializer implements Serializer<String>`--编译期 `S` 绑定到 `String`。但运行时泛型被擦除，`StringSerializer` 的字节码里 `Serializer` 接口的 `S` 是 `Object`。**运行时你拿一个 `StringSerializer` 实例，无法直接问「你处理的 S 是什么类型」**--`getClass().getInterfaces()` 拿到的是 `Serializer`（原始类型），不是 `Serializer<String>`（参数化类型）。

**这是所有 Java 框架的共同难题**。任何「按泛型类型参数分发」的需求都撞到这堵墙。

### 3.2 业界的解法:从 Signature 属性恢复类型

JVM 虽然擦除了泛型，但**类的泛型签名保存在字节码的 `Signature` 属性里**（JVM 规范 §4.3.4）。`Class.getGenericInterfaces()` 能读出 `ParameterizedType`，其中 `getActualTypeArguments()` 返回类型参数。于是「恢复擦除的类型」有了标准路径：

```java
// 抽象示意
Type[] interfaces = StringSerializer.class.getGenericInterfaces();
// 找到 Serializer<String> 这个 ParameterizedType
// getActualTypeArguments()[0] -> String.class
```

**这套机制是 Java 反射的标准能力，但用法在不同框架里有不同的封装**：

| 框架 | 封装 | 用法 |
|---|---|---|
| microsphere | `TypeUtils.resolveTypeArgumentClasses` | 给 `Serializer`/`Converter`/`EventListener` 自识别类型参数 |
| Spring | `ResolvableType` / `GenericTypeResolver` | `@ConfigurationProperties` 泛型绑定、`BeanFactory` 类型匹配 |
| Guice | `TypeLiteral<T>` | 绑定时显式写 `new TypeLiteral<Set<String>>(){}` |
| Jackson | `TypeReference<T>` | `new TypeReference<List<Foo>>(){}` 反序列化泛型容器 |

**Guice/Jackson 的解法是「显式声明」**：用户写 `new TypeLiteral<Set<String>>(){}`（匿名内部类，泛型签名在编译期固定）。**Spring/microsphere 的解法是「反射自动恢复」**：从类的 `implements Serializer<String>` 签名读出来。前者要求用户多写代码，后者要求类的泛型签名必须显式写在 implements/extends 子句里（不能被继承链擦除）。

### 3.3 「反射恢复」的陷阱:继承链擦除

```java
// 陷阱写法:泛型签名在继承链中被擦除
public abstract class BaseSerializer<T> implements Serializer<T> {}
public class MySerializer extends BaseSerializer<String> {}  // 看起来绑定了 String

// MySerializer.class.getGenericInterfaces() 拿到的是 BaseSerializer.class(原始类型)
// 因为 MySerializer 直接 implements 的不是 Serializer<String>,是 BaseSerializer(原始)
// 必须沿继承链向上:MySerializer -> BaseSerializer<String>(参数化) -> Serializer<T>(T->String)
```

`resolveTypeArgumentClasses` 必须沿继承链逐级解析泛型映射表（`T -> String`），才能最终确定 `Serializer` 的 `S` 是 `String`。**这套逻辑写错或继承链太深，会解析失败返回空列表**。

#### 具体展开:沿继承链解析的逐步过程

把上面的解析过程拆开看，每一步反射拿到的实际是什么：

```
目标:确定 MySerializer 实现的 Serializer<S> 里 S 是什么

Step 1:查 MySerializer 直接 implements 什么
  MySerializer.class.getGenericInterfaces() 
  -> [] (空!因为 MySerializer 没 implements 任何接口,只 extends BaseSerializer)

Step 2:查 MySerializer 的父类签名
  MySerializer.class.getGenericSuperclass() 
  -> BaseSerializer<String>   (ParameterizedType,这个签名在字节码里保留了)
  拿到 BaseSerializer 的类型参数 T = String

Step 3:查 BaseSerializer implements 什么
  BaseSerializer.class.getGenericInterfaces()
  -> Serializer<T>   (ParameterizedType,但 T 是 TypeVariable,不是具体类)

Step 4:用 Step 2 的映射(T -> String)替换 Step 3 的 T
  Serializer<T> 中的 T 用 String 替换
  -> Serializer<String>
  -> S = String ✅
```

**关键**：Step 2 拿到 `BaseSerializer<String>`（具体），Step 3 拿到 `Serializer<T>`（含未绑定变量 `T`）。必须把 Step 2 的映射表（`T -> String`）应用到 Step 3，才能消解 `T`。这就是「沿继承链逐级解析泛型映射表」的具体含义--每一级都可能引入新的类型变量，要逐级消解。

#### 为什么继承链会擦除

根源在字节码 `Signature` 属性的记录方式--**它只记录「直接」父类和接口的泛型签名，不记录传递闭包**。

```
MySerializer 的字节码 Signature:
  extends BaseSerializer<String>      ← 只记录直接父类,且记录了 String
  implements (无)                     ← MySerializer 没 implements Serializer

BaseSerializer 的字节码 Signature:
  implements Serializer<T>            ← 只记录直接接口,且 T 是变量(未绑定)
```

`MySerializer` 的字节码里**根本没有 `Serializer<String>` 这个签名**--它只有 `BaseSerializer<String>`。要得出 `Serializer<String>`，必须自己沿链推导（Step 2 + 3 + 4）。JVM 不提供「直接给我 MySerializer 的所有泛型接口」的 API--`getGenericInterfaces` 只返回直接声明的，传递的得自己算。

**这就是「反射恢复」路线的固有复杂度**：泛型签名是分散在继承链各级的，必须自己拼。Spring 的 `ResolvableType` 为此写了上千行代码处理各种边界（数组、嵌套泛型、类型变量循环引用等）。microsphere 的 `TypeUtils.resolveTypeArgumentClasses` 是简化版，能处理常见继承链，但复杂场景（如 `Serializer<Map<String, List<Integer>>>`）可能解析不出具体类型。

#### 边界:解析失败的静默后果

**反例一:继承链中间断了**

```java
public class MySerializer extends BaseSerializer {}  // BaseSerializer 没带 <String>
// BaseSerializer.class.getGenericSuperclass() -> BaseSerializer(原始类型,非 ParameterizedType)
// 拿不到 T 的绑定,解析失败 -> 返回空列表
```

`MySerializer extends BaseSerializer`（没写 `<String>`），`getGenericSuperclass` 返回原始类型 `BaseSerializer.class`，拿不到 `T` 的绑定。整个解析链断在这里。

**反例二:多层泛型擦除**

```java
class A<T> implements Serializer<T> {}
class B<T> extends A<T> {}
class C extends B<String> {}
// C -> B<String> -> A<T>(T->String) -> Serializer<T>(T->String) -> S=String
// 需要沿三层链解析,每一级都要维护映射表
```

层级越深，映射表的传递越容易出错。microsphere 的实现在三层以上时偶尔会解析失败（取决于具体类型变量命名和绑定方式）。

**静默后果**：解析失败时 `first(typeArguments)` 返回 null，Serializer 被存进 `typedSerializers.get(null)` 桶--**永远不会被命中**。`getMostCompatible(MyType.class)` 找不到精确桶，退到 `Object.class` 兜底（Java 序列化）。用户以为注册了专用 Serializer，实际走了 Java 序列化兜底--日志没报错，性能/语义都不对，排查极难。

**这是「反射恢复」路线的固有风险**：失败是静默的，需要开发者自律（泛型签名必须显式写在 implements/extends 子句，且继承链不能断）。Guice/Jackson 的「显式声明」路线（`new TypeLiteral<Set<String>>(){}`）没有这个坑--类型在编译期就固定，不可能擦除。**这是 microsphere/Spring 路线相对 Guice/Jackson 路线的根本代价**：方便（自动恢复）换来了脆弱（继承链敏感 + 静默失败）。


## 四、永恒原理三:schema 演进--序列化的核心难题

序列化方案最容易被忽视、却最关键的维度是 schema 演进性。长期运行的系统里，类一定会改字段--今天加个 `phone` 字段，明天改 `name` 类型从 `String` 到 `Optional<String>`。**旧版本序列化的字节流，新版本类能否反序列化？** 这就是 schema 演进问题。

### 4.1 `serialVersionUID` 解决不了演进

Java 序列化的 `serialVersionUID` 机制被广泛误解为「解决版本兼容」。**它只解决「类身份匹配」，不解决「字段语义变化」**：

- UID 相同 + 字段增删：JDK 容忍（多余字段忽略，缺失字段给默认值）。这部分算「演进兼容」。
- UID 相同 + 字段类型变化（`int age` -> `long age`）：UID 不变但类型不匹配，反序列化抛 `ClassCastException`。UID 救不了。
- UID 不同：直接 `InvalidClassException`。

**UID 的真正问题是「全有或全无」**：要么整个类标「不兼容」（改 UID），要么整个类标「兼容」（UID 不变）--没有「这个字段兼容、那个字段不兼容」的细粒度控制。一旦某个字段类型变了，整个类的兼容性就崩。

### 4.2 真正的演进机制:tag 与 schema 协商

业界成熟方案的演进机制有两种：

**Protobuf 的 tag 机制**：每个字段有个数字 tag，序列化只写 tag + 值，不写字段名。

```protobuf
message User {
  string name = 1;   // tag=1
  int32 age = 2;     // tag=2
  string phone = 3;  // 新增字段,tag=3
}
```

- 新增字段（旧数据没有 tag=3）-> 新代码读旧数据时 `phone` 给默认值。✅ 兼容。
- 删除字段 -> 旧代码读新数据时忽略多余的 tag。✅ 兼容。
- 改字段类型 -> 只要 wire type 兼容（如 `int32` -> `int64`），仍可读。✅ 部分兼容。

**关键**：tag 是「字段身份」，比 Java 序列化的「字段名 + 类型」更稳定。tag 不变，字段名可以随便改。

**Avro 的 schema 协商**：序列化时不写字段名也不写 tag，只写值。schema（字段定义）通过「reader schema vs writer schema 协商」完成映射：

```json
// writer schema（旧版本）
{"type":"record","name":"User","fields":[{"name":"name","type":"string"}]}

// reader schema（新版本,加了 phone 字段）
{"type":"record","name":"User","fields":[{"name":"name","type":"string"},{"name":"phone","type":"string","default":""}]}
```

反序列化时 reader 拿到 writer schema，按字段名匹配--`name` 有，`phone` writer 没有但 reader 有默认值 `""`，填默认值。✅ 兼容。Avro 的优势是 schema 完全分离，可以演化得很灵活。

### 4.3 演进性的核心:向前兼容 vs 向后兼容

schema 演进有两个方向，它们是**不同的关注点**，常常被混淆。理解这两个方向是理解所有序列化方案演进性的前提。

#### 两个方向的具体含义

| 兼容方向 | 场景 | 谁需要兼容谁 | 序列化方案需支持 |
|---|---|---|---|
| 向后兼容（backward） | 新代码读旧数据 | 新代码兼容旧数据 | 「新字段在旧数据里没有时给默认值」 |
| 向前兼容（forward） | 旧代码读新数据 | 旧代码兼容新数据 | 「旧代码遇到未知字段时忽略而非报错」 |

**为什么这是两个不同的方向**：

- **向后兼容**解决的是「升级顺序」问题--新版本先部署，要能读老版本留下的旧数据（数据库里的、消息队列里的、缓存里的）。如果新代码读到旧数据时因为「缺字段」报错，升级就卡住了。
- **向前兼容**解决的是「滚动升级」问题--灰度发布时，集群里新旧版本并存，新版本产生的数据可能被老版本读到。如果老代码遇到新字段就崩，灰度发布就做不了。

**两者的难度不对称**：向后兼容相对容易（新代码知道自己加了什么字段，主动给默认值）；向前兼容难（老代码不知道未来会有什么字段，必须「宽容地忽略未知」）。所以很多方案「向后兼容强、向前兼容弱」。

#### 具体场景演示

假设 `User` 类从 v1 演进到 v2，加了 `phone` 字段：

```
v1: { name, age }
v2: { name, age, phone }
```

**向后兼容场景（v2 代码读 v1 数据）**：

```
v1 序列化的字节流: { name="张三", age=20 }          ← 没有 phone
v2 代码反序列化:
  name="张三" ✅ 有
  age=20 ✅ 有
  phone=?    ← v1 数据里没有,怎么办?
```

- **Java 序列化**：UID 不变时，`phone` 给 Java 默认值（`null`）。✅ 兼容。
- **Protobuf**：`phone` 给 `.proto` 里声明的默认值（`""`）。✅ 兼容。
- **Kryo**：按字段序号严格匹配，v2 的 `phone` 字段序号在 v1 数据里找不到，可能给默认值也可能崩（取决于 Kryo 版本和配置）。⚠️ 部分兼容。

**向前兼容场景（v1 代码读 v2 数据）**：

```
v2 序列化的字节流: { name="张三", age=20, phone="13800" }  ← 多了 phone
v1 代码反序列化:
  name="张三" ✅ 有
  age=20 ✅ 有
  phone=?    ← v1 代码里根本没有 phone 字段,怎么办?
```

- **Java 序列化**：v1 代码遇到未知字段 `phone`，UID 不变时**会尝试跳过**（按字段描述符跳过字节）。✅ 大多数情况兼容。但若 v2 改了字段**类型**（`int age` -> `long age`），v1 按 `int` 读 `long` 的字节，数据错乱或抛 `ClassCastException`。❌ 类型变化时崩。
- **Protobuf**：v1 代码遇到未知 tag（`phone` 的 tag=3 在 v1 `.proto` 里没有），**直接跳过**（wire type 已知，能算出字段长度）。✅ 完全兼容，连类型变化都部分兼容（`int32` -> `int64`）。
- **Kryo**：v1 代码遇到未知字段，可能崩（按序号读，序号错位）。❌ 不兼容。

#### 为什么 Protobuf/Avro 在演进性上胜出

**Protobuf 的关键设计：tag + wire type**。每个字段序列化时写 `tag + wire type + 值`，反序列化时即使遇到未知 tag，也能根据 wire type 算出字段长度跳过--**「宽容地忽略未知」是内建能力**。这同时实现了向前兼容（旧代码跳过新字段）和向后兼容（新代码给旧字段默认值）。

**Avro 的关键设计：schema 分离 + 协商**。字节流不含 schema，反序列化时 reader 拿到 writer schema，按字段名匹配--reader 有的字段在 writer 里找，找不到给 reader 声明的默认值；writer 有的字段 reader 没有，忽略。**演进由 schema 协商机制保证，不依赖字段序号或 tag**。

**Java 序列化的根本弱点：按字段描述符严格匹配**。字节流里编码了字段名 + 类型 + 顺序，反序列化时严格按这个描述符读。字段类型一变就崩，字段顺序一变也可能崩。UID 只能控制「整体是否兼容」，无法细粒度到字段。

#### microsphere 的位置

`DefaultSerializer` 用 Java 序列化兜底，继承了它的演进性弱点--向前兼容勉强（跳过未知字段）、字段类型变化时崩。但框架本身不限制用户注册 Protobuf/Kryo 序列化器--**演进性由用户选的实现决定**。这是「框架提供机制，演进性由策略决定」的分层：microsphere 负责「按类型路由到正确的序列化器」，序列化器负责「自己的演进性」。如果你注册了 Protobuf 序列化器给 `User` 类，`User` 的演进由 Protobuf 的 tag 机制保证，与 microsphere 无关。


## 五、永恒原理四:序列化/反序列化的根本不对称

序列化和反序列化看似对称（对象 ↔ 字节），实际上有个根本不对称，决定了所有序列化框架的 API 形态。

### 5.1 不对称的根源

- **序列化方向**：`对象 -> 字节`。源是「活的对象」，运行时知道它的类型（`obj.getClass()`）。**调用方不需要传类型**。
- **反序列化方向**：`字节 -> 对象`。源是「字节流」，**运行时不知道它原本是什么类型**（除非字节流里编码了类型信息）。

**这就是为什么序列化 API 是 `serialize(Object)`，反序列化 API 却有两种形态**：

```java
// 形态一:字节流自带类型信息(Java serialization / Kryo writeClassAndObject)
Object deserialize(byte[] bytes);   // 不需传类型

// 形态二:字节流不带类型信息(Protobuf / JSON / String)
T deserialize(byte[] bytes, Class<T> type);   // 必须传类型
```

### 5.2 字节流是否编码类型信息是个根本取舍

**编码类型信息（自描述）**：

- Java serialization：`TC_OBJECT` + 类描述符（类名 + 字段签名 + UID）。
- Kryo `writeClassAndObject`：类名（或注册号）+ 字段值。
- Hessian：类型信息嵌入。

**优点**：反序列化不需传类型，`readObject` 直接还原。**缺点**：字节流大（每个对象都带类型）、有安全隐患（自描述 = 能实例化任意类 = RCE 风险）、跨语言难（类描述符是 Java 特有）。

**不编码类型信息（外部 schema）**：

- Protobuf：只写 tag + 值，类型由 `.proto` 文件外部定义。
- Avro：只写值，schema 在 reader/writer 间协商。
- JSON：结构自带但类型弱（`{}`/`[]`/字符串/数字），需要目标类型指导解析。

**优点**：字节流紧凑、安全（不能实例化任意类）、跨语言。**缺点**：反序列化必须传类型/schema，调用方负担重。

### 5.3 这决定了 microsphere 的 API 形态

```java
// microsphere Serializer/Deserializer
public interface Serializer<S> {
    byte[] serialize(S source);   // 序列化不需传类型(obj.getClass() 知道)
}
public interface Deserializer<T> {
    T deserialize(byte[] bytes);  // 反序列化也不显式传类型?
}
```

看似对称，实际 `Deserializer<T>` 的 `T` 是「实现类自己声明的目标类型」（通过泛型自识别，§3）--调用方调 `deserializers.getMostCompatible(String.class)` 时已经传了类型。所以 microsphere 走的是「不编码类型信息」路线--`DefaultDeserializer` 用 Java 序列化是个特例（Java 序列化字节流自带类型，所以 `DefaultDeserializer.deserialize(bytes)` 不需要类型参数也能工作），但其他 `Deserializer`（如 `StringDeserializer`）必须由调用方在 `getMostCompatible` 时指定类型。

**这是序列化框架设计的永恒约束**：你要么让字节流自描述（自描述 + 安全风险 + 跨语言难），要么让调用方传类型（紧凑 + 安全 + 跨语言，但 API 啰嗦）。没有两全。microsphere 的设计是「机制中立」--用户可以注册自描述的 `DefaultDeserializer`，也可以注册不自描述的 `StringDeserializer`，由用户按场景选。


## 六、microsphere 作为「序列化路由」的一个实例

讲完四个原理，microsphere-io 的 Serializer/Deserializer 就是这些原理的一次落地。

**实例一:泛型自识别 + SPI 自动发现（§3 原理的落地）**

`Serializers.loadSPI()` 用 SPI 加载所有 `Serializer` 实现，对每个用 `resolveTypeArgumentClasses`（§3.2）从泛型签名恢复 `S`，按目标类型分桶存进 `Map<Class, List<Serializer>>`。这套机制和 Converter（[§3](./03-convert-framework.md)）的 `getSourceType`/`getTargetType`、EventListener（[§6](./06-event-system.md)）的 `findEventType` 完全同构--microsphere 的「泛型自识别 + SPI + 类型注册表」是统一模式。

**实例二:兜底语义（[§10 日志](./10-logging-abstraction.md) §3.3 原理的落地）**

`getMostCompatible(type)` 找不到精确类型时退到 `Object.class` 的最低优先级--`DefaultSerializer`（Java 序列化）。这和日志的 NoOp 兜底同构：「最简陋方案永远最低优先级，作为最后防线」。Java 序列化作为兜底是合理的：它是唯一「能处理任意 `Serializable` 对象」的通用方案（自描述），专用序列化器（String/JSON/Kryo）覆盖不到的对象由它兜底。

**实例三:不对称性在 API 上的体现（§5 原理的落地）**

`Serializer<S>.serialize(S source)` 不需传类型（运行时 `obj.getClass()` 知道）。`Deserializers.getMostCompatible(Class<T>)` 必须传类型（字节流不自描述）。这个不对称是序列化本质决定的，不是 API 设计缺陷。`DefaultDeserializer` 例外--Java 序列化字节流自描述，所以 `deserialize(bytes)` 不需类型。

**默认实现**：`DefaultSerializer`/`DefaultDeserializer` 用 Java 序列化（自描述兜底），`StringSerializer`/`StringDeserializer` 用 UTF-8（跨语言轻量）。用户可注册 Kryo/Protobuf/JSON 序列化器覆盖--框架中立，策略由用户。


## 七、实例批判:这个实现的缺陷

作为原理的一个落地实例，microsphere-io 的实现也有瑕疵。

1. **`resolveTypeArgumentClasses` 失败静默进 null 桶**（§3.3）：泛型签名被继承链擦除时解析返回空，Serializer 被存进 `typedSerializers.get(null)` 桶，永远不被命中。应 fail-fast 抛异常而非静默。
2. **`getMostCompatible` 不沿继承链向上**：注册了 `Serializer<Number>`，序列化 `Integer` 对象时 `getMostCompatible(Integer.class)` 找不到精确桶，直接退到 `Object.class`--`Number` 桶被跳过。应沿继承链 `Integer -> Number -> Object` 逐级查找。
3. **`DefaultDeserializer` 不设 `ObjectInputFilter`**（§1.2）：反序列化不可信字节流有 RCE 风险。JDK 9+ 的 `ObjectInputFilter` 是安全刚需，应支持配置。
4. **`getMostCompatible` 取最高优先级而非「依次尝试」**：用户注册了 Jackson（高优先级）和 Kryo（中优先级），Jackson 处理不了的循环引用对象直接抛异常，不会退化到 Kryo。应是「按优先级依次尝试，第一个不抛异常的胜出」。
5. **线程安全约定隐式**：`typedSerializers` 是 `HashMap`，`loadSPI` 必须在启动时单线程调用。这个约定没在文档里写明，运行时多线程调 `loadSPI` 会出问题。

这些不是原理错误，而是原理在具体代码里的实现瑕疵。


## 八、与其他方案的原理对比

| 方案 | 类型分发机制 | 兜底策略 | 字节流自描述 | 演进性 | 依赖 |
|---|---|---|---|---|---|
| microsphere Serializer | 泛型自识别 + SPI | `Object.class` 最低优先级（Java 序列化） | 取决于实现 | 取决于实现 | 零 |
| Spring Serializer | DI 手动注入 | 无（必须显式指定） | 取决于实现 | 取决于实现 | spring-core |
| Kryo 直接用 | 手动 new | 无 | ✅（writeClassAndObject） | 差 | kryo.jar |
| Protobuf 直接用 | 手动 new + `.proto` | 无 | ❌（schema 外部） | 好 | protobuf.jar |
| Hessian（Dubbo 内置） | RPC 框架路由 | 无 | ✅ | 中 | hessian.jar |

**原理层面的取舍**：

- **Spring Serializer 假设「你在容器内」**：DI 给你注入一个具体的 Serializer，调用方自己知道用哪个。简单但要求容器环境。
- **microsphere 假设「你在 main() 第一行，没容器」**：必须按运行时类型自动路由--这是 SPI + 泛型自识别 + 兜底的组合动机。代价是 API 复杂度上升。
- **Kryo/Protobuf 直接用是「单策略」**：不路由，调用方自己选一个用死。简单但混合类型场景（缓存里既有 String 又有 POJO）要写 if-else。
- **Hessian 走 RPC 框架路由**：Dubbo 内部按接口类型选序列化器，和 microsphere 的「按对象类型选」是不同维度的路由。

microsphere 的定位：**「按对象类型自动路由」的序列化入口**，让上层（缓存、RPC、MQ）只需 `serializers.getMostCompatible(obj.getClass()).serialize(obj)`，无需关心底层用哪种引擎。引擎切换通过 SPI 声明完成，零代码改动。这是「机制中立」的设计--不绑定具体序列化方案，由用户按四维取舍（§2）选策略。


## 九、面试要点

**Q1：「Java 自带的序列化有什么问题？为什么业界长期想废弃它？」**

答案：四个原罪。① 强耦合 `Serializable`，第三方类没实现就序列化不了。② `serialVersionUID` 版本号地狱--只解决「类身份匹配」不解决「字段语义变化」。③ `readObject` 反序列化漏洞--字节流自描述能实例化任意类，Apache Commons Collections 2015 gadget chain 是经典。④ 不可插拔，序列化逻辑写死在 `ObjectOutputStream`。Brian Goetz 2018 年公开提及废弃序列化，JDK 9 引入 `ObjectInputFilter`（JEP 290）作为缓解但根本问题未解。生产环境安全敏感场景已普遍改用 Kryo/Hessian/Protobuf 等不含「任意类实例化」语义的方案。

**Q2：「序列化方案怎么选？业界有哪些主流方案？」**

答案：四维分类法--schema 演进性 / 跨语言 / 性能 / 紧凑度，四者互相冲突没有银弹。Java serialization 演进差不跨语言性能差；Kryo 性能极高但不跨语言演进差；Protobuf/Avro 演进好跨语言性能高（Protobuf 用 tag 机制，Avro 用 schema 协商）；JSON 跨语言可读但性能紧凑度差。演化趋势是从「自描述 + 强类型 + 不可演进」->「注册 + 强类型 + 弱演进」->「schema 分离 + tag 演进」。选型看场景：长期演进微服务选 Protobuf/Avro，高频 RPC 选 Kryo，Web API 选 JSON。

**Q3：「为什么 Java 里『按类型选序列化器』这么难？泛型不是有吗？」**

答案：泛型擦除。`StringSerializer implements Serializer<String>` 编译期 `S` 绑定 `String`，运行时擦除成 `Object`。运行时拿一个 `StringSerializer` 实例，无法直接问「你处理的 S 是什么类型」。解法是从字节码的 `Signature` 属性恢复--`Class.getGenericInterfaces()` 返回 `ParameterizedType`，`getActualTypeArguments()` 拿到类型参数。这是所有 Java 框架的共同难题：microsphere 用 `TypeUtils.resolveTypeArgumentClasses`，Spring 用 `ResolvableType`，Guice/Jackson 用 `TypeLiteral`/`TypeReference`（显式声明而非反射恢复）。陷阱是继承链擦除--`MySer extends BaseSer<String>` 必须 `resolveTypeArgumentClasses` 沿继承链逐级解析泛型映射表，失败时静默返回空。

**Q4：「`serialVersionUID` 真能解决版本兼容吗？序列化的演进难题本质是什么？」**

答案：不能。UID 只解决「类身份匹配」--UID 不一致直接 `InvalidClassException`。UID 一致时字段增删 JDK 容忍（多余忽略、缺失给默认值），但**字段类型变化**（`int age` -> `long age`）UID 救不了，反序列化抛 `ClassCastException`。UID 是「全有或全无」--没有「这个字段兼容、那个字段不兼容」的细粒度控制。真正的演进机制是 Protobuf 的 tag（字段身份是数字 tag，字段名可随便改）和 Avro 的 schema 协商（reader/writer schema 按字段名匹配，新字段给默认值）。演进性是序列化的核心难题，比性能更重要--长期运行的系统类一定会改字段。

**Q5：「序列化和反序列化为什么 API 不对称？一个要传类型一个不要？」**

答案：根本不对称在「源」--序列化源是活对象（运行时 `obj.getClass()` 知道类型），反序列化源是字节流（运行时不知道类型，除非字节流编码了类型信息）。这决定了反序列化 API 两种形态：① 字节流自描述（Java serialization/Kryo writeClassAndObject）-> `deserialize(bytes)` 不需传类型，但字节流大且有 RCE 风险；② 字节流不带类型（Protobuf/JSON/String）-> `deserialize(bytes, Class<T>)` 必须传类型，紧凑安全但 API 啰嗦。这是序列化的永恒约束，没有两全。microsphere 的 `DefaultDeserializer`（Java 序列化）走①，`StringDeserializer` 走②，由用户按场景选。

**Q6：「如果让你设计一个序列化框架，相比 microsphere 你会怎么改进？」**

答案：原理层照搬泛型自识别 + SPI + 兜底。改进点：① `resolveTypeArgumentClasses` 失败应 fail-fast 抛异常而非静默进 null 桶。② `getMostCompatible` 应沿继承链向上匹配（`Integer -> Number -> Object`），而非「精确匹配或全兜底」。③ `getMostCompatible` 应是「按优先级依次尝试，第一个不抛异常的胜出」而非「直接取最高优先级」。④ `DefaultDeserializer` 应支持 `ObjectInputFilter`（JDK 9+）配置，反序列化不可信数据是安全刚需。⑤ 应支持 Protobuf/Avro 的 schema 演进机制--演进性是长期系统核心，当前 microsphere 只提供「按类型路由」，不提供「演进保证」，演进性完全由用户选的实现决定。

---

> **与转换框架的关联**：`Serializer`/`Deserializer` 和 `Converter<S,T>`（[§3](./03-convert-framework.md)）是同一套设计哲学的两次应用--泛型自识别 + SPI + Prioritized。Converter 解决类型间转换，Serializer 解决对象↔字节。两者共用 `reflect/TypeUtils` 的泛型解析底层。
> **与日志/事件系统的关联**：「按优先级选第一个」+「最低优先级兜底」是 microsphere 通用模式--序列化的 `Object.class` 兜底（本文）、日志的 NoOp 兜底（[§10](./10-logging-abstraction.md)）、ArtifactDetector 的 ArchiveFile 兜底（[§1](./01-artifact-detection.md)）同构。
