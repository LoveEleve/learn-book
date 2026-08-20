# S-27 Metrics/Micrometer 编排 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 加自定义 tag 到所有指标? | §1 (MeterRegistryCustomizer) |
| 2 | 多个导出后端怎么注入? | §2 (Composite @Primary) |
| 3 | 公共 tag 怎么配(加到所有指标)? | §3 (management.metrics.tags) |
| 4 | 想过滤某些指标? | §3 (MeterFilter/PropertiesMeterFilter) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 customizers 先于 binders? | §1 (binder 依赖 customizer 的 tag/配置) |
| 6 | 为什么 Composite + Primary? | §2 (单注入点 + 多后端) |
| 7 | 为什么配置转 MeterFilter? | §3 (声明式配置全局生效) |
| 8 | 与 S-21 边界? | 边界 (Metrics 端点复用 S-21) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | MeterRegistryPostProcessor 做什么? | §1 (固定顺序应用) |
| 10 | 组合 registry 怎么工作? | §2 (广播到所有子 registry) |
| 11 | JVM 指标哪来的? | §3 (MeterBinder 绑定) |

## 覆盖: 11 问 / 3 身份 / 100%
