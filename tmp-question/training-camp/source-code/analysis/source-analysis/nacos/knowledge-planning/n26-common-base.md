# N-26 公共底座 — 知识规划 (KP)

> 🟡 B | 模块: common 剩余 + consistency/serialize | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 序列化 | JacksonSerializer/HessianSerializer | JSON/Hessian |
| 2 | 工厂 | SerializeFactory | 切换 |
| 3 | 包扫描 | packagescan (32) | 自实现 |
| 4 | 常量 | constant/ 族 | 契约集中 |
| 5 | 事件 | ServerConfigChangeEvent | 配置事件 |
| 6 | 能力 | ability/ | 能力面 |
| 7 | 生命周期 | lifecycle/ | Closeable |

## 02 高频坑
1. 序列化在 consistency 非 common
2. 包扫描自实现 (非 Spring)
3. 常量集中契约
4. 能力面支撑协商

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 序列化 | 双实现 / 工厂 / Hessian 工厂 |
| 扫描 | resource / util / classreading |
| 契约 | 常量 / 事件 / 能力 / 生命周期 |

## 04 跨域桥接
- → NC-5: 协议序列化
- → 面试: "Nacos 公共底座" — 序列化 + 扫描 + 常量
