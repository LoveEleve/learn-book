# OF-3 契约集成 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | SpringMvcContract 骨架: extends BaseContract + processAnnotationOnMethod (@RequestMapping) + 参数循环 |
| 2.1+ | AnnotatedParameterProcessor 注册表 (可扩展); Pageable 特判 (queryMapIndex); GET 未注解参数警告 |
| 3.x | SpringQueryMap (对象查询参数); FeignFormatterRegistrar → ConversionService; CollectionFormat 双级; 组合注解 findMergedAnnotation 完善 |
| 4.x | 类级 @RequestMapping 禁止显式化; 四解析 (produces/consumes/headers/params) 完善 |

## 痕迹证据

- SpringMvcContract.java:98: extends Contract.BaseContract (2.x 锚)
- SpringMvcContract.java:226-231: 类级禁止 "@RequestMapping annotation not allowed on @FeignClient interfaces" (4.x 锚)
- SpringMvcContract.java:245-260: GET 警告 "[OpenFeign Warning]...may result in fallback to POST at runtime" (2.1+ 锚)
- SpringMvcContract.java:362-370: Pageable → queryMapIndex (2.1+ 锚)
- SpringMvcContract.java:438-439: parseHeaders "// TODO: only supports one header value per key" (历史注释锚)
- FeignClientsConfiguration.java:88,152-160: FeignFormatterRegistrar 装配 (3.x 锚)
- SpringQueryMap.java:33-37: 标记注解 (3.x 锚)

## 推断标注

- "2.x 骨架" — Spring Cloud OpenFeign 公知版本线 (标注)
- "2.1+ 警告/Pageable" — 注释锚实证 (实证)
- "3.x 合并项" — 类/注解实证 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- Feign 本体 F-2 Contract: BaseContract 底座 (parseAndValidateMetadata/参数循环) — 底座对照
- Spring MVC @RequestMapping: 服务端注解语义 vs Feign 客户端翻译 — 语义对照
- gRPC (G-2): 服务端方法解析 vs Feign 契约 — 客户端对照
