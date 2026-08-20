# C-2 类型转换全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Value("${server.port}") int port — "8080" 怎么变成 int？ | §3 (@Value 链路) + §1 (StringToNumberConverterFactory) |
| 2 | 我要把 String 转成自定义类型(如 Money)怎么做？ | §1 (Converter 三形态选型) |
| 3 | 为什么不用 Integer.parseInt 而要走 ConversionService？ | §2 (统一注册表+缓存+泛型) |
| 4 | 转换找不到转换器时发生什么？ | §2 (ConverterNotFoundException) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 新旧两套转换体系(PropertyEditor vs ConversionService)的优先级？为什么？ | §3 关键设计 (历史演进) |
| 6 | NO_MATCH 负缓存解决了什么问题？ | §2 (避免重复反射查找) |
| 7 | Converter/ConverterFactory/GenericConverter 三种形态各自适用场景？ | §1 (一对/一对多/任意对) |
| 8 | 泛型转换(List<String>→List<Long>)的信息怎么传递？ | §1 (TypeDescriptor) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | convert 内部"查找→执行"分几步？ | §2 (getConverter 缓存链) |
| 10 | 默认能转哪些类型？ | §2 (DefaultConversionService 默认组) |

## 覆盖: 10 问 / 3 身份 / 100%
