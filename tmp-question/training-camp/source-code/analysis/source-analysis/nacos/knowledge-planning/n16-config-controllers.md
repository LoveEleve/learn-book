# N-16 配置控制器面 — 知识规划 (KP)

> 🟡 B | 模块: config/server/controller (19) + aspect (4) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 配置入口 | ConfigController:113/161 | → OperationService |
| 2 | 九控制器 | controller/ 9 类 | 面分离 |
| 3 | 容量切面 | CapacityManagementAspect | 容量限制 |
| 4 | 变更切面 | ConfigChangeAspect | 变更记录 |
| 5 | 失败切面 | ConfigOpFailureAspect | 失败处理 |
| 6 | 日志切面 | RequestLogAspect | 请求日志 |
| 7 | 分代 | v2/v3 | 兼容 |

## 02 高频坑
1. 控制器零业务 (委托 service)
2. 四切面横切 (容量/变更/失败/日志)
3. beta/export 配置专属
4. RestResult 统一返回

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 入口 | 九控制器 / 委托 / 分代 |
| 切面 | 四件 / 横切点 |
| 契约 | RestResult / parameters |

## 04 跨域桥接
- ← N-15: OperationService 消费
- ↔ N-09: 命名/配置控制器对照
- → 面试: "配置 HTTP 入口" — 薄控制器 + 切面族
