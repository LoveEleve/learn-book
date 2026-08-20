# N-24 事件中心 — 知识规划 (KP)

> 🔴 A | 模块: common/notify (10 文件 1,081 行) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 注册面 | NotifyCenter:45/67 | 类型→发布器 Map |
| 2 | SPI 工厂 | NotifyCenter:79-80 | 可扩展发布器 |
| 3 | 订阅注册 | NotifyCenter:164-165 | registerSubscriber |
| 4 | 发布路由 | NotifyCenter:280-295 | 按类型 |
| 5 | 发布器线程 | DefaultPublisher:41/94-111 | Thread + 队列 |
| 6 | 队列 | DefaultPublisher:55/71 | ArrayBlockingQueue |
| 7 | 分发 | DefaultPublisher:169-192 | 订阅者遍历 |
| 8 | 慢事件 | DefaultSharePublisher:107 | 共享发布器 |
| 9 | 分片 | ShardedEventPublisher | 分片版 |
| 10 | 订阅分型 | Subscriber/SmartSubscriber | 单/多类型 |

## 02 高频坑
1. 发布器是 Thread 非线程池 (单消费者)
2. SlowEvent 走共享发布器 (防线程爆炸)
3. 队列有界 — 满则 publish 失败
4. SmartSubscriber 多类型订阅
5. SPI 可换发布器实现
6. 发布器线程名 "nacos.publisher-{type}"

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 注册 | Map / SPI 工厂 / 共享注册 |
| 发布器 | Thread+队列 / 有界 / 分片 |
| 订阅 | Subscriber / Smart / 慢事件 |
| 分发 | receiveEvent / notifySubscriber |

## 04 跨域桥接
- ← NC-1/NC-2/NC-5: 全模块消费
- → 面试: "Nacos 事件中心" — 类型注册表 + 队列线程 + 慢事件共享
