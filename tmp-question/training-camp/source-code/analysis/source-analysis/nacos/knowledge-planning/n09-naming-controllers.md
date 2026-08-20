# N-09 命名控制器面 — 知识规划 (KP)

> 🟡 B | 模块: naming/controllers (20 文件) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 实例入口 | InstanceController:90/127 | → Operator |
| 2 | 注销 trace | InstanceController:154-156 | 入口事件 |
| 3 | 批量元数据 | InstanceController:213 | batchUpdate |
| 4 | 六控制器 | controllers/ 6 类 | 面分离 |
| 5 | 分代 | v2/v3 目录 | 兼容 |

## 02 高频坑
1. 控制器零业务 (全部委托 Operator)
2. v2/v3 分代共存
3. trace 在入口发布

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 入口 | 六控制器 / 委托 / 批量 |
| 分代 | v2 / v3 |
| 可观测 | trace 事件 |

## 04 跨域桥接
- ← NC-6: Operator 层
- → 面试: "Nacos 服务端入口" — 薄控制器 + 分代
