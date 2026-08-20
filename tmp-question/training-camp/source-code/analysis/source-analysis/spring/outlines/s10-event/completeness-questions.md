# S2-3 事件机制 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | multicastEvent 的异步双条件(executor!=null + supportsAsyncExecution)为什么不能简化为单条件？ | 篇1 §1 |
| 2 | getApplicationListeners 的 beanName 路径为什么要分早期过滤(不实例化)和二次检查？ | 篇1 §2 |
| 3 | @EventListener 方法中的参数(非事件对象)是怎么注入的？ | 篇2 §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | EventListenerMethodProcessor 为什么是 SmartInitializingSingleton 而非 BeanFactoryPostProcessor 或 BeanPostProcessor？ | 篇2 §1 |
| 5 | @EventListener 最终也是注册为 ApplicationListener 到同一个 Multicaster — 为什么不创建第二个广播器？ | 篇2 §3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | publishEvent 和 @EventListener 的关系是什么？一个事件的发布到处理经过了哪几层？ | 篇1 §1→篇2 §2 |
| 7 | Sync vs Async 事件处理有什么区别？@Async 注解加在 @EventListener 方法上是如何生效的？ | 篇1 §1 |

## 覆盖: 7 问 / 3 身份 / 100%
