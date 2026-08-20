# C-2 类型转换 — ConversionService (接口 → 注册表 → 容器衔接)

> 依赖 C-1 Resource | 🟡 Working | 6 KP | [模式: 策略 + 工厂 + 适配器]

**读者处境**: `@Value("${server.port}") int port` — 配置文件里是字符串 "8080", 怎么变成 int？`@RequestParam("page") int page` 也一样。Spring 有一套统一转换链, 不是 Integer.parseInt。

### 1. ConversionService 接口 + Converter 家族三形态 + TypeDescriptor

场景: "8080"→int、`List<String>`→`List<Long>`、Date→String — 转换方向五花八门。接口只问两个问题: 能转吗(canConvert)?怎么转(convert)?具体怎么转由注册的转换器决定。

源码路径:
- `ConversionService.java:29,45,76` — **接口**: canConvert(源类型, 目标类型) → convert(source, targetType) — 线程安全入口; 带 TypeDescriptor 的重载(L65)携带泛型信息
- `Converter.java:36,45` — **单对单**: `Converter<S,T>` — 一个源→一个目标, 最简单形态(如 StringToBooleanConverter)
- `StringToNumberConverterFactory.java:42,45` — **一对多**: `ConverterFactory<String, Number>` — getConverter(Class<T> targetType) 按目标类型动态产转换器 — Integer/Long/Short 一族只写一个工厂
- `TypeDescriptor.java:830(总行数)` — **类型描述**: 封装 source/target 的 Class+泛型+注解 — 泛型转换(List<Long>)的信息载体

关键设计: **Why 三种形态而非只有 Converter？** 数量不对称: String→N 个数字类型是"一对多" — 逐个注册 Converter 浪费且易漏, ConverterFactory 按需生产; GenericConverter 面向"任意对任意"(如集合互转)无法用泛型参数化表达。三形态覆盖三种扩展场景。[模式: 工厂 — 按目标类型产转换器]

数据流: canConvert(String.class, Integer.class) → 注册表查找 → StringToNumberConverterFactory 命中 → true → convert("8080", Integer.class) → factory.getConverter(Integer.class) 产 StringToIntegerConverter → convert → 8080。

### 2. GenericConversionService — 注册表、缓存与查找链

场景: 上面 canConvert/convert 内部怎么知道用哪个转换器？N 个转换器怎么组织、怎么查、查不到怎么办？

源码路径:
- `GenericConversionService.java:62,76` — **类与注册表**: this.converters(Converters 容器) — addConverter/addConverterFactory(通过接口的 getRequiredTypeInfo 反射读泛型参数, 自动登记源/目标)
- `GenericConversionService.java:226` — **getConverter()**: `ConverterCacheKey(源,目标)` → converterCache 查 → miss→converters.find(精确→子类兼容) → null→getDefaultConverter → 再无→**NO_MATCH 也入缓存**(负缓存防重复查找)
- `GenericConversionService.java:170` — **convert()**: 类型校验 → getConverter → ConversionUtils.invokeConverter → handleResult(Optional 包装等) / 无转换器→handleConverterNotFound 抛 ConverterNotFoundException
- `DefaultConversionService.java:43,89` — **默认注册组**: addScalarConverters(String→Number/Boolean/Enum/Character 等) + addCollectionConverters(集合/数组互转) + ObjectToObjectConverter(反射拷贝) + FallbackObjectToStringConverter + ObjectToOptionalConverter — Boot 默认用它在容器注册

关键设计: **Why NO_MATCH 也要缓存？** 每次 convert 都走"反射读泛型→遍历匹配"代价高 — 负缓存让"查不到"也 O(1); converterCache 键是 (源,目标) TypeDescriptor 对 — 与 W-5 消息转换器"首个可读即用"不同, 这里是"精确匹配优先"。[模式: 缓存 + 策略]

数据流: convert("1,2,3", List<Integer>) → getConverter(源=String, 目标=List<Integer>) → cache miss → converters.find 命中 StringToCollectionConverter(DefaultConversionService L91 addCollectionConverters 注册) → 入缓存 → invokeConverter: 拆分+元素 StringToInteger 转换 → [1,2,3]。

### 3. 容器衔接 — TypeConverterDelegate 与 @Value 链路

场景: @Value("${server.port}") int port — 完整链路: 占位符解析出 String "8080" → 类型转换 int — 但 Spring 里还有老一代 PropertyEditor, 两者怎么配合？

源码路径:
- `TypeConverterDelegate.java:117` — **convertIfNecessary()**: ①L121 `findCustomEditor(requiredType, propertyName)` — **PropertyEditor 优先** → ②L126-131 editor==null 且 conversionService.canConvert → conversionService.convert(失败记 conversionAttemptEx 走兜底) → ③L145+ 类型不匹配时的编辑器/转换兜底 → 最后 doConvertValue
- `AbstractBeanFactory.java:875,937` — **容器装配**: setConversionService(XML <bean conversionService>) / getTypeConverter() 统一 TypeConverter — 属性填充(AbstractAutowireCapableBeanFactory.applyPropertyValues)、@Value 依赖解析共用

关键设计: **Why PropertyEditor 优先于 ConversionService？** 历史演进: PropertyEditor 是 JDK 老机制, ConversionService 是 Spring 3 引入的新体系 — 为兼容老配置(XML 自定义 editor)保留前者优先; 新代码(注解驱动)几乎全走 ConversionService。**Why 失败 fallback 而非直接抛？** 编辑器/转换器都可能半匹配(如 String→Object 的 FallbackObjectToString), 需要两级兜底链。[模式: 优先级链]

数据流: @Value("${server.port}") int port → doResolveDependency: PropertyPlaceholderHelper 解析 ${server.port}→"8080"(见 C-3) → getTypeConverter().convertIfNecessary("8080", int) → TypeConverterDelegate: 无自定义 PropertyEditor → conversionService.canConvert(String,int)=true → convert("8080", int) → StringToNumberConverterFactory → 8080 → 注入。若自定义了 MyEditor(如 Date 格式) → L121 直接走 PropertyEditor。

→ 引出 0-3: Environment — @Value 的 ${...} 占位符是谁解析的？PropertySource 从哪来、优先级怎么排 (PropertyResolver/Environment)。
