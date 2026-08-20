# N-25 HTTP 客户端与工具族 — 完备性问题集 (20 问)

## A. 机制理解 (5)
1. HttpClientFactory 族的替换机制?
2. NameThreadFactory 的命名规范?
3. SPI 加载器怎么用?
4. 线程池管理的关闭语义?
5. SPI 加载的类加载器?

## B. 源码实证 (6)
6. 工厂族类名? (grep http/)
7. ThreadPoolManager 语义? (grep executor/)
8. NacosServiceLoader 加载? (grep spi/)
9. HttpClientConfig 的配置项?
10. HttpRestResult 的语义?
11. Callback 的异步面?

## C. 推理深挖 (5)
12. 缓存族的装饰器链?
13. 任务引擎的延迟精度?
14. BeanHolder 的持有与关闭?
15. 任务引擎的延迟语义?
16. 缓存族的装饰器模式?

## D. 跨域扩展 (4)
17. 连接池的复用与关闭?
18. 线程池的拒绝策略?
19. SPI 优先级 (多实现) 的处理?
20. 缓存过期策略?

