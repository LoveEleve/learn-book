# S-15 AOT/Native Image 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | AOT 怎么触发?什么时候跑? | §1/§3 (编译期插件) |
| 2 | AOT 为什么要运行 main? | §1 (完整容器收集 hints) |
| 3 | Native Image 怎么用 AOT 产物? | §3 (免反射扫描) |
| 4 | main 不是 SpringApplication 会怎样? | §2 (IllegalStateException) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 withHook+AbandonedRunException? | §2 (拦截 main 副作用) |
| 6 | 与 s21 的边界? | §1/§3 (管线在 s21) |
| 7 | 为什么编译期收集 hints? | §3 (Native 无法动态反射) |
| 8 | 容器怎么"拿"出来? | §2 (异常传值) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | RuntimeHints 有什么? | §3 (reflection/resources/...) |
| 10 | 钩子机制的本质? | §2 (运行用户代码拦副作用) |

## 覆盖: 10 问 / 3 身份 / 100%
