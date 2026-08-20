# C-2 类型转换 — ConversionService (接口 → 注册表实现 → Bean 容器衔接)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | ConversionService(111行)+Converter(67行)+GenericConversionService(685行)+DefaultConversionService(183行)+TypeDescriptor(830行)+TypeConverterDelegate(650行)+StringToNumberConverterFactory(68行)+StringToBooleanConverter(59行)
> 基线: C-1 Resource — @Value 注入 Resource 只是转换的冰山一角 — 本域展开 String→任意类型的统一转换链; 原始执行计划 0-2

---

## §0.8

- 🟡 Working，1篇 — 接口契约(ConversionService: canConvert/convert + TypeDescriptor 描述源/目标类型) → Converter 家族(Converter/ConverterFactory/GenericConverter 三种形态) → 注册表实现(GenericConversionService: Converters 注册表+converterCache+getConverter 查找链) → 默认组(DefaultConversionService.addDefaultConverters) → Bean 衔接(TypeConverterDelegate: PropertyEditor 优先→ConversionService→兜底)
- 设计模式: [模式: 策略模式]—convert 按 (源,目标) 查找转换器; [模式: 工厂模式]—ConverterFactory 按目标类型产转换器; [模式: 适配器]—ConverterAdapter 包普通 Converter 为 GenericConverter

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ConversionService.java:29,45,76 | 接口 | **接口双方法**: canConvert(源,目标)(类型对) / convert(source, targetType) — 线程安全, 系统入口; TypeDescriptor 版(L65)带泛型信息 | High |
| Converter.java:36,45 | Converter 接口 | **单对单转换器**: Converter<S,T> 一个源类型→一个目标类型 — 最简单形态 | High |
| StringToNumberConverterFactory.java:42,45 | ConverterFactory | **一对多工厂**: ConverterFactory<String, Number> — getConverter(targetType) 按目标类型产转换器(如 Integer/Long 各一个 StringToX) — 比注册 N 个 Converter 省 | High |
| GenericConversionService.java:62,76 | 类/注册表 | **注册表**: this.converters(Converters 容器) — addConverter/addConverterFactory 注册; 支持 GenericConverter(任意对任意) | High |
| GenericConversionService.java:226 | getConverter() | **查找+缓存**: ConverterCacheKey(源,目标) → converterCache 查 → miss→converters.find(按精确/子类匹配) → getDefaultConverter(兜底) → 无→NO_MATCH 也缓存(负缓存) | High |
| GenericConversionService.java:170 | convert() | **执行**: getConverter → ConversionUtils.invokeConverter → handleResult(Optional 包装等) / handleConverterNotFound 抛 ConverterNotFoundException | High |
| DefaultConversionService.java:43,89 | 默认组 | **默认转换器集**: addScalarConverters(StringToNumber/Boolean/Enum/Character…)+addCollectionConverters(数组/集合互转)+ObjectToObjectConverter(反射拷贝)+FallbackObjectToStringConverter+ObjectToOptionalConverter | High |
| TypeConverterDelegate.java:117,126,131 | convertIfNecessary() | **Bean 容器入口**: ①L119 findCustomEditor(PropertyEditor 优先!) → ②L126-131 editor==null 且 conversionService.canConvert → conversionService.convert(失败记录 fallback) → ③L145+ 编辑器/类型检查兜底 — **PropertyEditor 优先于 ConversionService** | High |
| AbstractBeanFactory.java:875,937 | setConversionService/getTypeConverter | **容器装配**: XML <bean conversionService> 或 @Configuration ConversionServiceFactoryBean 注册 → 所有属性注入/参数绑定共用 getTypeConverter() | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+三形态转换器+注册表实现+容器衔接约 2600 行 — 但使用路径只有一条: "@Value/属性注入 → TypeConverterDelegate → ConversionService → 注册表 find → Converter". 1篇 (~47行) 按"契约→实现→衔接"展开; 若分 2 篇则"注册表查找"与"容器入口"割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | GenericConversionService 注册表+缓存查找 (Converters.find→getDefaultConverter→NO_MATCH 负缓存) | 🔴 | **为什么🔴**: 转换系统的核心引擎 — "精确→子类→兜底"查找顺序与缓存设计决定性能与扩展 |
| P1-2 | TypeConverterDelegate.convertIfNecessary (PropertyEditor 优先→ConversionService→兜底) | 🔴 | **为什么🔴**: Bean 容器与转换系统的唯一入口 — 新旧两套体系的优先级顺序是历史遗留的核心认知 |
| P1-3 | Converter 家族三形态 (Converter/ConverterFactory/GenericConverter) | 🔴 | **为什么🔴**: 自定义转换器的三种写法 — 选型(单对/批量/任意对)是日常扩展的决策点 |
| P2-1 | DefaultConversionService 默认组 (scalar+collection+ObjectToObject+FallbackObjectToString) | 🟡 | **为什么🟡**: "开箱即用"的转换能力清单 — 知道有什么可转, 才知道要自定义什么 |
| P2-2 | TypeDescriptor (源/目标类型描述 + 泛型携带) | 🟡 | **为什么🟡**: convert 的泛型参数(如 List<Long>)靠它传递 — 与 W-5 getJavaType 泛型解析同思路 |
| P3-1 | @Value 链路 (StringValueResolver→getTypeConverter().convertIfNecessary) | 🟢 | **为什么🟢**: 最常用的注入场景 — 收束示例 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **转换引擎** (接口+注册表+缓存查找+三形态) | 🔴 | 转换系统的核心 — 接口契约与查找算法 |
| B | **容器衔接** (TypeConverterDelegate + 装配) | 🔴 | 新旧转换体系在容器层的汇合点 — 顺序即优先级 |
| C | **默认与扩展** (默认组+TypeDescriptor+@Value) | 🟡 | 开箱能力 + 泛型细节 |

> **Cluster A (§1)**: ConversionService 接口双方法 + Converter 家族三形态 + TypeDescriptor
> **Cluster B (§2)**: GenericConversionService 注册表/缓存/查找 + DefaultConversionService 默认组 + StringToNumberConverterFactory 例子
> **Cluster C (§3)**: TypeConverterDelegate(PropertyEditor 优先) + AbstractBeanFactory 装配 + @Value 收束

→ 引出 0-3: Environment — @Value("${...}") 的占位符解析 (PropertySource/PropertyResolver) — 转换链的上游: 先取到 String, 再转类型

(End of file - total 62 lines)
