# N-25 HTTP 客户端与工具族 — 知识规划 (KP)

> 🟡 B | 模块: common/http + executor + spi + cache + task | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类 | 一句话 |
|:--:|:--|:--|:--|
| 1 | Http 工厂族 | HttpClientFactory/AbstractApache | 可替换 |
| 2 | Bean 持有 | HttpClientBeanHolder | 单例 |
| 3 | 线程池 | ThreadPoolManager | 公共池 |
| 4 | 命名线程 | NameThreadFactory | 可观测 |
| 5 | SPI 入口 | NacosServiceLoader | 插件统一 |
| 6 | 缓存族 | Cache + decorators | 通用缓存 |
| 7 | 任务引擎 | NacosTaskProcessor | 延迟任务 |

## 02 高频坑
1. 序列化在 consistency/serialize 非 common
2. NameThreadFactory 全模块命名
3. NacosServiceLoader 是 SPI 统一入口
4. Http 工厂 Apache 抽象

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| Http | 工厂族 / BeanHolder / 工具 |
| 执行 | 线程池 / 命名 / 工厂 |
| 工具 | SPI / 缓存 / 任务 / 序列化 |

## 04 跨域桥接
- ← 全模块: 线程/SPI/任务消费
- → 面试: "Nacos 公共底座" — Http 工厂 + 命名线程 + SPI
