# W-5 HttpMessageConverter 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @ResponseBody 返回对象时, 响应 Content-Type 怎么定？为什么有时 406？ | §2 (Accept 协商→交集→406) |
| 2 | 我想新增一种数据格式(如 XML)支持 @RequestBody — 需要实现什么？ | §1 (三抽象点 supports/readInternal/writeInternal) + §2 (configureMessageConverters 注册进链) |
| 3 | Jackson 转换器的 ObjectMapper 从哪来？怎么换成自定义的？ | §3 (默认构造 Jackson2ObjectMapperBuilder / 自定义构造 L71) |
| 4 | 请求没有 Content-Type 时 @RequestBody 会怎样？ | §2 (octet-stream 兜底 L169, 空体返回 null) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么读方向按 Content-Type 直接匹配、写方向要先协商 Accept？ | §2 关键设计 (事实 vs 偏好) |
| 6 | 为什么默认 Spring MVC 顶层没有 JSON 转换器？@RequestBody JSON 的 415 从哪来？ | §2 (initMessageConverters 仅 3 个顶层 / AllEncompassingForm 只服务 multipart / Boot 自动装配) |
| 7 | 接口为什么把"判断"(canRead/canWrite)与"动作"(read/write)分离？ | §1 (双维度匹配 + 模板) |
| 8 | 自定义转换器 vs @ResponseBodyAdvice — 两个扩展点各自解决什么问题？ | §1 (新格式) vs §2 (现有格式的拦截) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | `List<User>` 的泛型信息是怎么传到 Jackson 的？ | §3 (GenericTypeResolver→getJavaType) |
| 10 | 转换器链遍历时为什么"首个可读即用"而不是挑最优？ | §2 (读链路 break, 注册顺序即优先级) |

## 覆盖: 10 问 / 3 身份 / 100%
