# ALI-A2 Nacos 配置动态刷新 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. 新旧两条刷新轨各自的"终点"是什么? 旧轨只换源, 新轨走全链路 — 差异的根源?
2. NacosConfigRefreshEventListener 为什么只认 NacosConfigRefreshEvent? 转发 RefreshEvent 的意义?
3. ready 门控 (AtomicBoolean) 在两个监听者中分别防什么?
4. SPECIFIC_BEAN 模式下 changeKey 与 prefix 的匹配为什么是 startsWith? 边界问题?
5. @NacosConfig 的 refreshed=false 与 @NacosConfigListener.initNotify=false 语义差异?

## B. 源码实证 (6)

6. NacosContextRefresher 的 REFRESH_COUNT 在哪里被读? (grep NacosPropertySourceLocator:164)
7. registerNacosListener 的防重复靠什么? (grep L116-117)
8. NacosPropertySourceRefreshListener 的仲裁条件具体是什么? (grep L98)
9. Smart rebinder 的 onApplicationEvent 双源判断逻辑? (grep L89-91)
10. NacosAnnotationProcessor 的 getOrder 值? (grep L69)
11. NacosRefreshHistory 的 MAX_SIZE 和摘要算法? (grep L38/L83-97)

## C. 推理深挖 (5)

12. 如果用户自定义了 ConfigurationPropertiesRebinder, Smart rebinder 还会装配吗? 为什么?
13. 新轨在时旧轨 listener 收到事件为什么不动 Environment? 数据会丢吗?
14. 快照 (putConfigSnapshot) 在刷新链路中的作用? 与 A1 的 getAndRemove 如何配对?
15. NacosPropertySourceRefreshListener 收集 beans 但仲裁不换源时, 这个收集有意义吗?
16. 为什么 Smart 默认不用 (ALL_BEANS)? "Minimize the possibility of making mistakes" 背后是什么风险?

## D. 跨域扩展 (4)

17. NacosConfigRefreshEvent → RefreshEvent → ContextRefresher 与 Commons SCC-8 的 RefreshEventListener 链路如何衔接?
18. Smart rebinder vs Commons SCC-2 的 ConfigurationPropertiesRebinder: 扩展了什么?
19. 旧轨"只换源不发事件" vs 新轨"全链路": 对 @RefreshScope Bean 的影响差异?
20. NacosAnnotationProcessor 的 groupKeyCache 双检 vs NacosConfigManager 单例双检: 两种并发模式对比?
