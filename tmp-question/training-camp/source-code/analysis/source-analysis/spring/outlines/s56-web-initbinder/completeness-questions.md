# C-14 @InitBinder 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 表单对象怎么被填充的？类型转换在哪做？ | §1 (DataBinder.bind + TypeConverter) |
| 2 | @InitBinder 里能做什么定制？ | §2 (setDisallowedFields/addValidators/registerCustomEditor) |
| 3 | @DateTimeFormat 怎么让日期自动转换？ | §3 (FormattingConversionService 注解驱动) |
| 4 | @RequestBody 校验失败怎么报错？ | §1 (BindingResult + C-13 MethodArgumentNotValidException) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用工厂+方法回调而非子类覆写？ | §2 关键设计 (声明式定制) |
| 6 | 本地和全局 @InitBinder 怎么合并？ | §2 (initBinderCache + adviceCache) |
| 7 | @DateTimeFormat 与 C-2 转换体系的关系？ | §3 (注解化扩展) |
| 8 | BindingResult 存在的意义？ | §1 (转换失败不中断, 汇总报错) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | binder 是每个请求建一个吗？ | §2 (createBinder 每请求每对象) |
| 10 | @InitBinder 方法什么时候被调用？ | §2 (创建 binder 后遍历应用) |

## 覆盖: 10 问 / 3 身份 / 100%
