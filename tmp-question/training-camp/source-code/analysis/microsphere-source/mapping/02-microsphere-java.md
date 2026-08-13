# microsphere-java 知识点提取

> 源码：`/data/workspace/java-training-camp/cloud-native-code/share/microsphere-java`（依赖链第 2 站，confucius-commons 之后）
> 提取时间：2026-08-12（批 1：annotations 8；批 2：constants 8 + beans 4；批 3：collection 32；批 4：convert 核心机制——52 文件按机制归组）
> 状态：批 1-4 已提取；MCP 索引已建（13165 节点/49189 边）
> 关联：与 confucius-commons 同作者（mercyblitz）——工具类主题重叠时交叉引用（06 纪律）

## 一、仓库定位

**microsphere 生态的标准库**（Foundation）——confucius-commons 的继任/扩展：字符串/集合/反射/类型转换/事件/类加载/并发/配置属性的全套 JDK 工具 + 注解 + 编译期处理器。README 实证 Features 12 项（:20-32）。核心维度：[工程问题]（工具/扩展机制）+ [规范]（注解/SPI）。

## 前置条件清单

读者需先掌握：1. confucius-commons 全部知识点（本仓库是其扩展——JDK 工具主题重叠）2. 注解机制（Retention/Target/注解处理器）3. JSR-305 空注解 4. Maven 多模块
未达前置者，先补：confucius-commons outline（`outline/01-confucius-commons-jdk知识大纲.md`）

## 掌握度

目标读者：中级偏上（读源码多、Spring 熟——HANDOVER 画像）
讲解策略：与 confucius-commons 交叉引用，不重复提取（06 纪律）

---

## 二、逐文件映射 + 原子记录

### 模块: `microsphere-java-annotations`（包 `io.microsphere.annotation`）

#### KP-101 `ConfigurationProperty` 配置属性注解（ConfigurationProperty.java:38-97）

- **维度**：[规范] | **权重**：[核心] | **深度**：🔴 | **优先级**：P2 | **过时**：[时间无关模式]（注解元模型） | **置信度**：High
- **前置**：注解元模型（name/type/defaultValue 设计）、配置属性概念
- **需求**：**声明式配置绑定**——在类字段上标注配置属性来源/名称/默认值，编译期生成元数据（配合 annotation-processor 模块）
- **自主实现**：注解六要素——`name`（:60 属性名）、`type`（:67 默认 String）、`defaultValue`（:74）、`required`（:81）、`description`（:88）、`source`（:95 来源数组）+ 三来源常量（:43/:48/:53 system-properties/environment-variables/application）
- **参考实现**：confucius/spring 无此物——这是 microsphere 自家设计的**注解驱动配置元模型**；与 Spring `@ConfigurationProperties` 对照：Spring 靠类名+字段名推断 + `@Value("${key}")`，microsphere 是**显式元模型**（name/type/defaultValue/source 全声明）
- **对比取舍**：**设计决策**——显式元模型 vs Spring 约定推断：microsphere 牺牲简洁换确定性（编译期可校验）；source 数组支持多来源优先级（:95）
- **测试佐证**：ConfigurationPropertyTest 存在（annotations 测试目录）
- **my-xhs**：**该用没用**——my-xhs 用 Spring `@ConfigurationProperties` 绑定；microsphere 的**显式元模型+多来源设计**可作为 my-xhs 自定义配置注解的参考（若需多环境来源优先级）

#### KP-102 空注解双包：`@Nonnull/@Nullable`（Nonnull.java:32-37 + Nullable.java:34-39）

- **维度**：[规范] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[过时→JSpecify/Spring Nullable]（JSR-305 底层）+ [时间无关模式]（空注解语义） | **置信度**：High
- **前置**：JSR-305（confucius KP-12）、TypeQualifierNickname 机制
- **需求**：空值契约标注——**基于 JSR-305 的类型限定器昵称**（TypeQualifierNickname）
- **自主实现**：直接 Spring `org.springframework.lang.Nullable` 或 JSpecify
- **参考实现**：microsphere 用 `@TypeQualifierNickname`（:35/:37）包装 `@javax.annotation.Nonnull`（:34）——**TypeQualifierNickname 是 JSR-305 的"注解组合"机制**（带 when=MAYBE :36 表达可空）
- **对比取舍**：**知识增量：TypeQualifierNickname**——JSR-305 的复合注解机制（组合注解+元注解传播）；但 JSR-305 停维护（同 confucius KP-12），Spring Nullable/JSpecify 是替代
- **my-xhs**：**不该用**——Spring 生态直接用 Spring Nullable（confucius KP-12 同判定）

#### KP-103 线程安全标注：`@ThreadSafe/@NotThreadSafe`（ThreadSafe.java:26-30 + NotThreadSafe.java:25-29）

- **维度**：[规范] | **权重**：[边缘] | **深度**：🟢 | **优先级**：P3 | **过时**：[时间无关模式] | **置信度**：High
- **前置**：并发语义标注概念
- **需求**：类级线程安全契约标注——文档性质
- **参考实现**：**注意细节**：①头部版权注释实证 **Brian Goetz（JCIP 作者）2005 版权**（ThreadSafe.java:1-6）——**这是 Java Concurrency in Practice 附带的注解移植**（不是 JSR-305！同 KP-35 的"JDK 移植"模式）；②`javax.annotation.concurrent.NotThreadSafe` 是 **JCIP 原始包**（非 JSR-305）——confucius 时代误判过同类；③**互相 import 对方**（ThreadSafe.java:9 import javax NotThreadSafe / NotThreadSafe.java:9 import javax ThreadSafe）——**未使用的 import**（各自只定义自己的注解，注释掉后残留）
- **对比取舍**：与 JCIP 原始注解重复定义（只是改了包名）；空注解无元数据价值；**移植残留**（未用 import）是质量瑕疵
- **my-xhs**：**不该用**——JCIP 注解/JSR-305 即可

#### KP-104 元注解三件：`@Experimental/@Immutable/@Since`（Experimental.java:36-38 / Immutable.java:31-34 / Since.java:42-68）

- **维度**：[规范] | **权重**：[边缘] | **深度**：🟢 | **优先级**：P3 | **过时**：[时间无关模式] | **置信度**：Medium
- **前置**：注解 Target/Retention 语义
- **需求**：API 成熟度/不变性/引入版本标注——库作者文档工具
- **参考实现**：@Experimental Target 多元素（ANNOTATION_TYPE/CONSTRUCTOR/FIELD/METHOD/TYPE :23-27）+ Retention SOURCE；@Immutable RUNTIME（:33）；**@Since 有属性结构**——`module()`（:61 模块名，测试实证 "microsphere-java-core"）+ `value()`（:66 版本号，测试实证 "1.0.0"），Target 全元素（:43-54 含 TYPE_USE/TYPE_PARAMETER/PACKAGE）
- **测试佐证**：SinceTest:36-37 实证 `since.module()=="microsphere-java-core"` + `since.value()=="1.0.0"`；ImmutableTest:36/NonnullTest:36/NullableTest:36 实证 getAnnotation 非空；ExperimentalTest:35 实证 SOURCE retention（getAnnotation 返回 null——测试类未标注故 null，弱断言）
- **my-xhs**：**不该用**——内部库无需成熟度标注

---

## 三、批 1 总结（annotations 模块）

- **主题**：microsphere 自研注解集——**配置元模型（核心）+ 空注解包装 + 线程安全/成熟度标注**
- **最高价值 KP**：KP-101（ConfigurationProperty 显式元模型 vs Spring 约定推断的设计对照——"该用没用"候选）+ KP-102（TypeQualifierNickname 是 confucius 未触及的 JSR-305 新机制）
- **与 confucius-commons 关系**：空注解/常量主题重叠 → 交叉引用（KP-102 ↔ confucius KP-12）
- **来源标注教训**：ThreadSafe/NotThreadSafe 是 **JCIP（Brian Goetz）移植**（版权头实证）——非 JSR-305；未用 import 残留
- **测试已扫**：6 个测试全部断言实证（批 1 完成）

## 四、my-xhs 判定汇总（批 1）

| KP | 判定 | 理由 |
|----|------|------|
| KP-101 | **该用没用** | Spring @ConfigurationProperties 够用；但显式元模型+多来源设计值得参考（设计知识保留） |
| KP-102~104 | **不该用** | Spring Nullable 覆盖；冗余/文档性质 |

## 五、待验证（批 2）

- ConfigurationProperty 与 annotation-processor 的联动（编译期生成什么）
- JSR-305 TypeQualifierNickname 在 Spring/生态的现代等价物

### 包: `io.microsphere.constants`（8 文件）

#### KP-105 常量接口家族（Constants.java:13-16 + 7 子接口）

- **维度**：[工程问题] | **权重**：[边缘] | **深度**：🟢 | **优先级**：P3 | **过时**：[过时→JDK 内置常量/枚举（confucius KP-11 同判定：常量接口是反模式）] | **置信度**：High
- **前置**：接口继承、常量反模式（confucius KP-11）
- **需求**：共享常量组织——**微改进**：`Constants` 聚合接口 extends 6 个子接口（FileConstants/PathConstants/PropertyConstants/ProtocolConstants/SeparatorConstants/SymbolConstants，:13-14）——**按域分接口再聚合**（confucius 是平铺 5 接口）
- **参考实现**：分层常量接口：文件扩展名（FileConstants:21-66——ZIP/JAR/WAR/EAR/CLASS + 扩展名组合 :56-61）、路径（PathConstants:18-38——char 与 String 双版本 SLASH_CHAR/SLASH）、协议（ProtocolConstants:24-59——file/http/https/ftp + 复用 FileConstants :44-59）、符号（SymbolConstants:30-333——**双字符表** char + String 两套 :184-333）、属性（PropertyConstants:30-35——`microsphere.` 前缀 :35 是生态配置命名空间）、分隔符（SeparatorConstants:26-41——**用 JDK 内置** `File.separator`/`File.pathSeparator`/`System.lineSeparator()` :31-41 静态导入）
- **对比取舍**：**相比 confucius 的关键进步**：①分隔符不再硬编码（confucius KP-11 用 `"/"` 字面量——这里静态导入 `File.separator` :31 跨平台正确）；②char/String 双版本供 API 复用（:18-23）；③ResourceConstants 定义**配置元数据文件约定**（:43-60——`configuration-properties.json` 是 annotation-processor 的产出物——与 KP-101 联动）
- **my-xhs**：**不该用**——常量接口反模式；JDK 内置 + my-xhs 自有常量

### 包: `io.microsphere.beans`（4 文件）

#### KP-106 `BeanMetadata` 属性元数据缓存（BeanMetadata.java:45-53 等）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[时间无关模式]（Introspector 封装） | **置信度**：High
- **前置**：JavaBeans 规范（Introspector/PropertyDescriptor）、反射缓存模式
- **需求**：**属性描述符的缓存封装**——Introspector.getBeanInfo 昂贵（反射），按类缓存 PropertyDescriptor map
- **自主实现**：`Introspector.getBeanInfo` + `ConcurrentHashMap<Class, BeanMetadata>` 缓存——**同构**
- **参考实现**：microsphere 构造时 introspect（:53）+ 内部缓存 `propertyDescriptorsMap`（:51）；`@Immutable` 标注（:20 自注解）
- **对比取舍**：模式时间无关；`@Immutable` 自用注解是**自举**（microsphere 用自己的注解标注自己的类——:20 实证）
- **my-xhs**：**该用没用**——my-xhs 若需高性能反射属性访问（如动态表单/通用导出），可参考此缓存模式；Spring `BeanWrapperImpl` 已有内置缓存（替代）

#### KP-107 `BeanProperty` 属性值包装（BeanProperty.java:37-57 等）

- **维度**：[工程问题] | **权重**：[边缘] | **深度**：🟢 | **优先级**：P3 | **过时**：[时间无关模式] | **置信度**：High
- **前置**：PropertyDescriptor、属性读写
- **需求**：属性名/值/类型/描述符的不可变包装——BeanUtils 的中间数据结构
- **参考实现**：四字段（name/value/beanClass/descriptor :40-49）+ 构造校验（@Nonnull :40）；**Null 语义**：name/beanClass/descriptor 必须非空（@Nonnull），value 可空（@Nullable :42）——**空注解使用实证**（KP-102 的落地用法）
- **my-xhs**：不该用——Spring BeanWrapper 覆盖

#### KP-108 `BeanUtils` 属性解析核心（BeanUtils.java:86-148,203-272,449-514 等）

- **维度**：[工程问题] | **权重**：[核心] | **深度**：🔴 | **优先级**：P1 | **过时**：[时间无关模式]（递归解析设计） | **置信度**：High
- **前置**：JavaBeans 规范、递归、深度限制、属性路径
- **需求**：**Bean → 属性 Map 的递归解析**——嵌套对象/集合/数组展平，带深度限制防栈溢出
- **自主实现**：递归 + `maxResolvedDepth` 深度限制（默认 8——:107-123 配置化）+ Map/List/Set/Array 特判
- **参考实现**：microsphere **配置化深度**——`microsphere.bean.properties.max-resolved-depth` 系统属性（:107-112）+ 缓存大小 `microsphere.bean.metadata.cache.size`（:138-148）+ **BeanMetadata 并发缓存**（:150 `newConcurrentHashMap(BEAN_METADATA_CACHE_SIZE)`）；核心私有实现（:449-514）：**深度检查 `resolvedDepth.incrementAndGet() >= maxResolvedDepth` 返回空 map（:453-456——每次递归先自增深度，达到上限截断）**；类型分派（:483-512——primitive/simple/CharSequence/Number/enum/Class 直接返回 :487-495，array/list/set/queue/enumeration/map 各自转换 :497-512，POJO 递归 :512）
- **对比取舍**：**设计决策值得学**：①**深度限制是防栈溢出的手段而非防环**——`MutableInteger` 自增（:453），A→B→A 循环会在**达到深度上限时截断**（不会死循环，但会消耗完整深度预算——**无 visited 集，深度预算耗尽前会重复解析同对象**，这是与 Jackson/Spring 的差异，它们用对象标识防环）；②系统属性三层（属性名+默认值常量+运行时解析 :107-123）配置化完整模式；③`@Immutable` 返回（:203）；④**可解析类型白名单**（:487-495——Class 也直接返回 :494，避免递归进 Class 元数据）
- **测试佐证**：Javadoc 示例（Person/Address/Country 嵌套 :230-252）——maxResolvedDepth=2 截断嵌套
- **my-xhs**：**该用没用**——my-xhs 通用导出/属性展平场景可用；Spring `BeanMap`/`BeanWrapper` 有内置（替代）；**无 visited 防环需注意**（深度预算替代，性能弱于对象标识防环）

#### KP-109 `beans.ConfigurationProperty` 运行时配置属性（ConfigurationProperty.java:63-252）

- **维度**：[规范] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[时间无关模式] | **置信度**：High
- **前置**：KP-101（注解）、元模型概念
- **需求**：**注解的运行时对象化**——@ConfigurationProperty（KP-101 注解）声明的属性在运行时的 Java 对象表示（name/type/value/defaultValue/required/description + Metadata.sources）
- **参考实现**：**注解↔对象双层设计**：注解（KP-101 编译期声明）+ 本类（运行时实例 :63-252）——属性字段（:68-98）+ 嵌套 `Metadata` 类（:219-252，sources 集合 :224）——**注解的元模型在运行时以对象承载**
- **对比取舍**：**知识增量：元模型双层**（编译期注解 + 运行时对象）——Spring 用 `Binder` + `ConfigurationProperties` 对象直接绑定，microsphere 是"注解→对象"显式映射；`equals/hashCode` 含 Metadata（:189/:200——值对象语义完整）
- **my-xhs**：**不该用**——Spring Binder 覆盖；但**元模型双层设计**是自定义配置框架的参考

---

## 六、深度 review 七项报告（批 1-2）

> 2026-08-12 批判性 review：批 1 修正 KP-103（JCIP 移植+未用 import）、KP-104（Since 属性+测试实证）；批 2 写入时验证 + KP-108 防环机制实证 + beans 测试补扫。

### 测试扫描记录（02 §2.1）

| 测试文件 | 验证了 | 结论 |
|---------|--------|------|
| BeanMetadataTest | propertyDescriptors size=9（:60）+ 描述符缓存 same（:54）/BeanInfo 非空（:54） | KP-106 缓存机制 ✓ |
| BeanPropertyTest | 属性包装 | KP-107 ✓ |
| BeanUtilsTest | 缓存 same（:70-71）+ 属性解析 Map（:121-125）+ **null bean 返回空 map（:175）+ 无 readMethod 对象抛 RuntimeException（:181）** | KP-108 边界行为 ✓ |
| ConfigurationPropertyTest（beans） | 属性对象字段 | KP-109 ✓ |
| annotations 6 测试（批 1） | 常量值/注解存在性/Since module+value | KP-101~104 ✓ |
| collection 测试（批 3a） | EmptyIteratorTest（hasNext false/next 抛 NoSuchElement/remove 抛 IllegalState :29-39——**remove 抛 IllegalStateException 非 Unsupported**）、ReadOnlyIteratorTest（:45-59）、DelegatingIteratorTest（:57-71）、ImmutableEntryTest（setValue 抛 UnsupportedOperation :56 + equals/hashCode :61-63）、SingletonIteratorTest（**继承 ReadOnlyIteratorTest 复用测试** :30-35）、EmptyQueueTest（引用 CollectionUtils.emptyQueue :26——**EmptyQueue 在 CollectionUtils 内部非独立类**） | KP-110~114 ✓ |
| CollectionUtilsTest（批 3b） | 判空/工厂断言 + **equals 跨类型相等实证**（List vs Set vs Queue 空集相等 :196-198 + null 对称 :192-195） | KP-116 ✓ |
| ListsTest/MapsTest/MapUtilsTest/SetUtilsTest/ListUtilsTest/QueueUtilsTest | of 工厂 + equals + 专用方法 | KP-117~120 ✓ |
| ConvertersTest / Converter 家族测试 | SPI 加载 + 转换断言 | KP-121~124 ✓ |

### 深度 review 七项

- [x] **① 源码行号精确核对**：批 2——KP-105:13-16/:21-66/:18-38/:24-59/:30-333/:30-35/:26-41/:43-60、KP-106:45-53/:20、KP-107:40-49、KP-108:86-148/:150/:203/:272/:449-514（防环实证 :453-456/:487-495/:512）、KP-109:63-252/:219-252——全部 grep 实证 ✓
- [x] **② 穷尽性**：批 1（8）+ 批 2（constants 8 + beans 4）= 20 文件覆盖；core 剩余 274 文件已按包规划 ✓
- [x] **③ 空节标注**：N/A ✓
- [x] **④ 过时三级**：9 个 KP（101-109）全部标注 ✓
- [x] **⑤ 重复内容**：KP-105 ↔ confucius KP-11（常量接口）交叉引用；KP-102 ↔ confucius KP-12 ✓
- [x] **⑤b 引用目标核对**：无跨文档断言 ✓
- [x] **⑥ 诚实标注**：KP-108 防环机制已实证（深度预算替代 visited 集——无死循环但重复解析）；测试边界（null bean/无 readMethod :175/:181）标注 ✓
- [x] **⑦ 命名空间迁移**：JCIP（javax.annotation.concurrent）非 JSR-305——来源分层已标注 ✓

### 包: `io.microsphere.collection`（批 3a：小文件 24/32）

> 主题：**JDK 集合的"缺失件"补全**——空集合/单例/不可变/委托/适配器家族。JDK9+ `List.of`/`Collections` 已覆盖大半。

#### KP-110 空集合家族（EmptyIterable/EmptyIterator/EmptyDeque + DelegatingIterator 基座）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[过时→`Collections.emptyIterator()`/`emptyList()`（JDK 一直有）+ `List.of`（JDK9）] | **置信度**：High
- **前置**：Iterator/Iterable 契约、空对象模式（Null Object）
- **需求**：**空对象模式**——避免 null 传播的 `emptyIterator()` 调用链（`EmptyIterator.INSTANCE` :63）
- **参考实现**：microsphere 分层——`EmptyIterator extends DelegatingIterator`（:58，**委托给 `Collections.emptyIterator`** :66——复用 JDK 再包一层）、`EmptyIterable extends IterableAdapter`（:44，**传 null 委托** :52-53 + 防御 defaultIfNull）、`EmptyDeque extends AbstractDeque`（:63，单例 :67）
- **对比取舍**：**嵌套委托链**（Empty→Delegating→JDK）是"多一层抽象"——**冗余但教学价值**：展示了空对象 + 委托 + 单例三模式组合；JDK 原生 `Collections.emptyXxx()` 已足够
- **my-xhs**：**不该用**——`Collections.emptyIterator()` 原生

#### KP-111 单例集合家族（SingletonIterator/SingletonEnumeration/SingletonDeque）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P3 | **过时**：[过时→`Collections.singletonList()`/`List.of(e)`（JDK9）] | **置信度**：High
- **前置**：Iterator 有状态遍历（hasNext 翻转）
- **需求**：单元素集合——**经典有状态迭代器实现**（hasNext 标记翻转 :43-58）
- **参考实现**：`SingletonIterator extends ReadOnlyIterator`（:39，**remove 抛异常继承**）+ hasNext 布尔翻转（:43/:50-58）；SingletonEnumeration 同构（:56-71）；SingletonDeque 组合（:69-75 委托 singletonIterator）
- **对比取舍**：**有状态迭代器是经典模式**（时间无关）；JDK9 `List.of` 覆盖
- **my-xhs**：不该用

#### KP-112 不可变/只读家族（ReadOnlyIterator/UnmodifiableIterator/UnmodifiableDeque/UnmodifiableQueue + ImmutableEntry）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[过时→`Collections.unmodifiableXxx()` + JDK9 `List.of`] | **置信度**：High
- **前置**：不可变语义、`@Immutable`（KP-104 应用）
- **需求**：**只读保护**——读操作委托、写操作抛异常
- **参考实现**：`ReadOnlyIterator`（:50——**final remove 抛 IllegalStateException** :53-54）；`UnmodifiableIterator extends ReadOnlyIterator`（:51，hasNext/next/forEachRemaining 委托 :59-70）；`UnmodifiableQueue/Deque`（:65/:60——**所有写方法抛 UnsupportedOperationException** :70-71）；`ImmutableEntry extends DefaultEntry`（:47——**setValue 抛异常** :53-55 + @Immutable 标注）
- **对比取舍**：**知识增量**：两类"只读"语义——**结构只读**（Unmodifiable：抛异常）vs **数据不可变**（Immutable：字段 final）；`@Immutable` 是声明式标注（编译期不可校验，文档性质）
- **my-xhs**：**不该用**——Collections.unmodifiableXxx 原生

#### KP-113 委托家族（DelegatingIterator/DelegatingQueue/DelegatingDeque + DelegatingWrapper 协议）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[时间无关模式]（委托模式） | **置信度**：High
- **前置**：组合优于继承、委托模式、JDBC Wrapper 模式
- **需求**：**组合替代继承**——包装器基座（空/单例/不可变都基于它）
- **参考实现**：`DelegatingIterator implements Iterator, DelegatingWrapper`（:41）；DelegatingQueue（:40，构造非空断言 :45——**assertNotNull + lambda 消息** :45 现代风格）；**DelegatingDeque 用 MethodHandle 缓存 reversed()**（:39——注释自证 "since Java 21" :38，`findPublicVirtual(Deque.class, "reversed")` 是 JDK21 新方法 `SequencedCollection.reversed()` 的反射桥，JDK17 下降级）
- **对比取舍**：**DelegatingWrapper 是解包协议而非标记**（修正前断言）——实证：`DelegatingWrapper extends Wrapper`（DelegatingWrapper.java:78），提供 **`unwrap(Class)`（:88-97）+ `isWrapperFor(Class)`（:100-103）默认方法**——这是 **JDBC Wrapper 模式的移植**（连接/语句解包协议）：`getClass().equals(type)` 直接返回自己（:89-90），否则递归委托（:92-93）；**框架可识别"包装器 vs 真实实现"并可解包**——比纯标记接口高一个层次
- **my-xhs**：**该用没用**——my-xhs 若做缓存/连接包装层（如 Redis/DataSource 包装）可参考 DelegatingWrapper 解包协议（JDBC Wrapper 同款）

#### KP-114 适配器家族（EnumerationIteratorAdapter/IterableAdapter/ArrayEnumeration + ArrayStack）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P3 | **过时**：[过时→JDK9 `Iterable.iterator()` 默认方法/`Enumeration` 已被 Iterator 取代] | **置信度**：High
- **前置**：适配器模式、旧 API 桥接（Enumeration→Iterator）
- **需求**：**遗留 API 桥接**——Enumeration（JDK1.0）→ Iterator（JDK1.2）单向适配
- **参考实现**：`EnumerationIteratorAdapter extends ReadOnlyIterator`（:55——enumeration 委托 :64-70）；`IterableAdapter implements Iterable`（:47——**Iterator→Iterable 反向适配** :56-57 + null 防御 :52）；`ArrayEnumeration`（:51——数组→Enumeration）
- **对比取舍**：适配器模式时间无关；Enumeration 是遗留 API 的教学样本
- **my-xhs**：不该用

#### KP-115 工具类（EnumerationUtils/Iterators/PropertiesUtils.flatProperties + ReversedDeque）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[时间无关模式]（equals/展平逻辑） | **置信度**：High
- **前置**：迭代器语义、属性展平（嵌套 key 规范化）
- **需求**：迭代器相等比较（元素序相等 :41-55）+ 属性嵌套展平（`a.b.c` 规范化）
- **参考实现**：`Iterators.equals`（:41-55——**逐元素比较 + 长度匹配**（:54 双 hasNext 检查）——完整相等语义）；`PropertiesUtils.flatProperties`（:76-96——递归展平 + `normalizePropertyName` 前缀拼接 :95 + @Immutable 返回 :75/:82）；`ReversedDeque.of`（:46-52——**ReversedDeque 幂等**（已反转不再反转 :47-49））
- **对比取舍**：Iterators.equals 是**正确实现**（长度+顺序）；flatProperties 与 Spring `Binder` 展平对照（Spring 用 `.` 分隔路径，microsphere normalizePropertyName 同思路）
- **my-xhs**：**该用没用**——属性展平（KP-115）在 my-xhs 配置转换/日志脱敏场景可用；Spring `Binder` 有内置

### 包: `io.microsphere.collection`（批 3b：大工具类 8/8）

#### KP-116 `CollectionUtils` 工厂/判空/相等工具（CollectionUtils.java:58-687）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[过时→JDK9+ `List.of`/`Collections`] | **置信度**：High
- **前置**：集合 API、空安全设计
- **需求**：集合判空/大小/相等/单例工厂的**空安全统一入口**——23 个静态方法（:58-687）
- **参考实现**：**判空三件套**（isEmpty :58/isNotEmpty :75/toIterable :98）；**转换链**（Collection→Iterable :98 / Iterator→Iterable :121 / Enumeration→Iterator :144 / Enumeration→Iterable :167）；**工厂**（singletonIterable :190 / emptyIterator **直接委托 JDK** `Collections.emptyIterator()` :303-306——不自造！/ emptyIterable 返回缓存 INSTANCE :329-331）；**equals 四判**（:411-450——同引用 :413/双空 :416/长度 :420/containsAll :422）
- **对比取舍**：**设计决策**：①empty 工厂委托 JDK 而非自造（:303）——避免重复实现；②**equals 是"集合语义相等"非"容器相等"**——测试实证跨类型相等（List vs Set vs Queue 空集相等 CollectionUtilsTest:196-198）+ null 对称处理（:192-195）；③containsAll 简化在重复元素场景**不精确**（陷阱范围窄：仅"长度相同 + 元素集相同但频次不同"的 multiset，如 {a,a,b} vs {a,b,b}——长度与 containsAll 都通过但语义不等）——**Set 场景正确、multiset 场景边缘缺陷** [测试无重复元素场景]
- **my-xhs**：**不该用**——JDK `Objects`/`Collections`/Spring `CollectionUtils` 覆盖；**equals 陷阱要注意**

#### KP-117 `Lists.ofList` 重载轰炸 + MethodHandle 工厂（Lists.java:101-533）

- **维度**：[性能优化] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[过时→`List.of`（JDK9 直接可用）] | **置信度**：High
- **前置**：MethodHandle、JDK9 工厂方法、重载设计
- **需求**：**JDK9 前无 `List.of` 的补位**——固定参数工厂
- **参考实现**：**双层设计**：①**重载轰炸**（ofList() :119 / ofList(e1) :152 ... ofList(e1..e9) :444——9 个公开重载避免 varargs 数组分配；另有 **of10MethodHandle**（:95——10 参 handle 已缓存但无对应公开重载 [待验证]））；②**varargs 兜底**（ofList(E...) :513）走 **MethodHandle 反射桥**（ofMethodHandle = `findPublicStatic(List.class, "of", Object[].class)` :101——**运行期探测 JDK9 List.of，存在则 invokeExact，否则降级 Arrays.asList** :519-525）
- **对比取舍**：**知识增量：MethodHandle 版本探测模式**——JDK 演进兼容的标准手法（Sets.of :42-49 / Maps.of :42-49 同款，三文件实证）；**探测-降级双保险实证**（MethodHandlesLookupUtils.findPublicStatic :93-130——方法不存在返回 NOT_FOUND :114-115 + Lookup 失败返回 null :125-129 带 trace 日志）；重载轰炸是 JDK9 前减少数组分配的性能手法（现在 `List.of` 已原生解决）
- **my-xhs**：**不该用**——`List.of` 原生；**MethodHandle 探测模式**是跨 JDK 兼容的参考

#### KP-118 `Sets.ofSet` / `Maps.ofMap` 同模式工厂（Sets.java:42-572 + Maps.java:42-605）

- **维度**：[性能优化] | **权重**：[支撑] | **深度**：🟡 | **优先级**：P2 | **过时**：[过时→`Set.of`/`Map.of`（JDK9）] | **置信度**：High
- **前置**：同 KP-117
- **需求**：Set/Map 固定参数工厂——同 Lists 的 JDK9 补位
- **参考实现**：**多 MethodHandle 按参数数缓存**（Sets:44/:47/:49——of0/of1... 每参数一个 handle；Maps 同构 :44/:47/:49）——比 Lists 更细粒度（按 arity 分派）；Map 还有 **of(Object... keyValuePairs) 交替解析**（Maps:360/:448——键值交替校验）
- **对比取舍**：与 KP-117 同模式；**按 arity 缓存多个 MethodHandle** 是更精细的版本探测（Lists 只缓存 varargs 版）
- **my-xhs**：**不该用**——`Set.of`/`Map.of` 原生

#### KP-119 `MapUtils` 映射工具大全（MapUtils.java:61-1445）

- **维度**：[工程问题] | **权重**：[核心] | **深度**：🟡 | **优先级**：P1 | **过时**：[时间无关模式]（工厂/加载因子设计） | **置信度**：High
- **前置**：HashMap 加载因子语义、ConcurrentHashMap、LinkedHashMap accessOrder
- **需求**：Map 全套工具——工厂（newHashMap/newLinkedHashMap/newConcurrentHashMap/newTreeMap）+ 判断（isMap/isEmpty）+ of 工厂
- **参考实现**：**加载因子常量设计**（MIN_LOAD_FACTOR :61 / **FIXED_LOAD_FACTOR = 1.00f** :66——**固定 1.0 加载因子防扩容**：预知容量场景用满负载避免 resize 浪费）；**isMap 双版本**（Object :85 / Class :106——instanceof vs isAssignableFrom）；**newLinkedHashMap 带 accessOrder 参数**（:655——LRU 支持）；of 工厂多形态（:207-448）
- **对比取舍**：**知识增量：FIXED_LOAD_FACTOR=1.0 的设计**——已知确切容量时用满加载因子消除扩容（时间换空间）；accessOrder 参数化（:655）暴露 LinkedHashMap 的 LRU 能力
- **my-xhs**：**该用没用**——my-xhs 若做缓存（LRU）可参考 accessOrder 参数化；Spring `MapFactory` 有部分覆盖

### 包: `io.microsphere.convert`（批 4：核心机制 52 文件归组）

> 主题：**类型转换 SPI 体系**——README Features 明示 "Extensible Converter SPI"。结构：Converter 接口 + AbstractConverter 模板 + Converters 注册中心 + StringConverter 枢纽 + 38 个具体转换器 + multiple 子包 14 个集合转换器。

#### KP-121 `Converter` SPI 接口（Converter.java:71-166）

- **维度**：[工程问题] | **权重**：[核心] | **深度**：🔴 | **优先级**：P1 | **过时**：[时间无关模式]（转换 SPI 设计） | **置信度**：High
- **前置**：SPI（confucius KP-32）、泛型类型推断、Prioritized 优先级
- **需求**：**类型转换抽象**——S→T 的转换契约，泛型声明源/目标类型
- **参考实现**：**三个设计决策**：①`@FunctionalInterface`（:71）+ `extends Prioritized`（:72——优先级接口）；②**泛型推断**——`getSourceType()`/`getTargetType()` 用 `resolveActualTypeArgumentClass(getClass(), Converter.class, 0/1)`（:99-110——从泛型实参反射解析源/目标类型，**免注册类型声明**）；③**accept 判定**（:81-83——`isAssignableFrom` 双向检查，SPI 匹配用）；静态工具（getConverter :135 / convertIfPossible :159——**失败返回 null 而非抛异常** :164）
- **对比取舍**：**知识增量**：泛型实参反射解析类型（免配置）——Spring 的 `GenericTypeResolver`/`ResolvableType` 同思路；**convertIfPossible 的"失败返回 null"语义**（:159-165）是设计决策（宽松转换 vs 严格异常）——测试实证（ConverterTest:50——`convertIfPossible("1", Date.class)` 返回 null）；**Prioritized 与 Spring `Ordered` 同构**（Prioritized.java:56 extends Comparable + COMPARATOR 静态比较器 :61-76——处理"一方非 Prioritized"降级 + MAX/MIN/NORMAL 三常量 :81-91）
- **my-xhs**：**该用没用**——my-xhs 若做类型转换层（配置/协议转换）可参考；Spring `ConversionService`/`TypeConverter` 有完整替代

#### KP-122 `AbstractConverter` 模板方法（AbstractConverter.java:88-146）

- **维度**：[工程问题] | **权重**：[核心] | **深度**：🔴 | **优先级**：P1 | **过时**：[时间无关模式]（模板方法） | **置信度**：High
- **前置**：模板方法模式、异常包装、优先级解析
- **需求**：**转换模板**——空值保护 + 异常统一包装 + 优先级自动解析
- **参考实现**：**四层设计**：①**模板方法**（convert :101-113——null 直接返回 :102-104 + doConvert 抽象 :127 + **Throwable 统一包装 RuntimeException** :108-111 带 warn 日志）；②**优先级自动解析**（resolvePriority :139-146——**类型继承深度编码**：`getAllClasses(sourceType).size() << 16 | getAllClasses(targetType).size()`（:143-145，负号使"更具体类型"优先级更高）——**源类型深度占高 16 位、目标低 16 位**，父类转换器自动排后）；③**null 安全**（@Nonnull/@Nullable 标注使用 :20-21）；④**final 防重写**（convert 标 final :101——模板不可改，子类只实现 doConvert）
- **对比取舍**：**模板方法 + final 防重写**是教科书正确用法（Spring `PropertyEditorSupport`/`ConversionService` 同构）；**继承深度编码优先级**是巧妙的自动排序（免手工指定优先级——子类多、优先级自然低）；异常包装（:108-111）保留原始异常链
- **my-xhs**：**该用没用**——模板方法模式参考（任何"处理链"场景）

#### KP-123 `Converters` SPI 注册中心（Converters.java:42-77）

- **维度**：[工程问题] | **权重**：[核心] | **深度**：🔴 | **优先级**：P1 | **过时**：[时间无关模式]（SPI 注册） | **置信度**：High
- **前置**：SPI 加载（confucius KP-32 + 本仓库缓存版）、ConcurrentHashMap computeIfAbsent、双键缓存
- **需求**：**转换器注册与查找**——SPI 加载全部 Converter + 按 (sourceType,targetType) 键缓存
- **参考实现**：**三层设计**：①**启动全量加载**（initConvertersCache :48-60——`loadServicesList(Converter.class, classLoader, true)` SPI 加载 :71-73 + **双键缓存** `Entry<Class,Class>` :55 + computeIfAbsent 分组 :56-57）；②**懒查找**（findConverter :62-69——**缓存未命中时按 accept 过滤再缓存** :64-67——懒加载过滤链）；③**取首个**（first(converters) :68——**SPI 顺序 + Prioritized 排序**决定胜负）
- **对比取舍**：**知识增量：双键缓存（Entry 键）+ 懒过滤**——"启动分组 + 懒补充"双层缓存策略；与 Spring `ConversionService` 的 `ConverterRegistry`（注册式）对照——microsphere 是 **SPI 自动发现式**（无需注册，classpath 扫描）；**生态演进实证**：confucius KP-32 的 loadServicesList 无缓存（每次重新加载），本仓库**加了缓存开关**（ServiceLoaderUtils:140 `servicesCache` + :438/:468 重载 + `microsphere.service-loader.cached` 系统属性 :112）——**同主题两代实现的演进**
- **my-xhs**：**该用没用**——SPI 自动发现转换器是自定义协议转换层的参考；Spring ConversionService 是替代

#### KP-124 转换器实现家族（38 个具体类 + multiple 14 个）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟢 | **优先级**：P3 | **过时**：[时间无关模式]（转换逻辑） | **置信度**：High
- **前置**：各类型转换语义（Number/String/Array/Collection）
- **需求**：具体类型转换——Number↔各类型（NumberToInteger 等 7 个）、Object→X（ObjectToBoolean 等 10 个）、String→X（StringToBoolean 等）、String→Collection（multiple 14 个）
- **参考实现**：**字符串枢纽**——`StringConverter<T> extends Converter<String,T>`（StringConverter.java:26——**String 是转换中枢**，几乎可转所有类型）；**集合转换家族**（multiple/MultiValueConverter :36-85——**accept(sourceType, multiValueType) 双类型判定** :45 + SPI 加载 :73-80 + sorted 优先级 :77）；StringTo*Converter 14 个（Queue/Deque/BlockingQueue/Set/SortedSet/NavigableSet/List/Collection/Iterable/Array）
- **对比取舍**：**String 枢纽设计**（String 作中间表示——CSV/JSON 通吃）vs Spring 的 `ConversionService`（直接 S→T 图）；**multiValue 双类型 accept**（源 + 集合类型双匹配）是集合转换的精确判定
- **my-xhs**：**该用没用**——若 my-xhs 做配置格式转换（String→任意）可参考 String 枢纽；Spring `DefaultFormattingConversionService` 覆盖

#### KP-120 `QueueUtils`/`SetUtils`/`ListUtils` 专用工具（QueueUtils.java:52-146 + SetUtils.java:74-890 + ListUtils.java:73-738）

- **维度**：[工程问题] | **权重**：[支撑] | **深度**：🟢 | **优先级**：P3 | **过时**：[时间无关模式] | **置信度**：High
- **前置**：队列/集合/列表 API
- **需求**：各集合类型的专用判空/转换/首尾访问
- **参考实现**：QueueUtils（EMPTY_DEQUE 常量 :52 + isQueue :70/isDeque :108 + emptyQueue/emptyDeque :127/:146）；SetUtils（ofSet 多形态 :125-234）；ListUtils（isList :73/:90 + first/last :111/:132——**Deque 场景用 peekFirst 语义**）
- **my-xhs**：**不该用**——JDK/Spring 覆盖
