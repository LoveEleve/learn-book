# D-5 注册中心 — completeness-questions (全视角提问验证)

## 开发者视角

1. 注册中心接口有什么? (五方法契约)
2. 本地缓存怎么兜底? (saveProperties/loadProperties)
3. 失败怎么重试? (FailbackRegistry 4 任务)
4. 地址变化怎么通知? (subscribe/notify)
5. 动态目录怎么更新? (refreshInvoker 增量)
6. 空保护? (EMPTY_PROTOCOL → forbidden)
7. 有哪些实现? (ZK/Nacos/Multicast/Multiple)
8. 应用级服务发现? (ServiceDiscoveryRegistry 钩子)

## 架构师视角

9. 为什么注释即契约? (实现族共享行为规范)
10. 为什么本地缓存? (抖动期间继续工作)
11. 为什么分类存储? (地址/路由/配置互不干扰)
12. 为什么无限重试? (配合缓存兜底, 直到恢复)
13. 为什么首次通知阻塞? (启动竞态消除)
14. 为什么空保护 vs 缓存兜底双容错? (合法清空 vs 异常丢数据区分)
15. 为什么增量更新? (最小化 invoker 重建)
16. 为什么 CacheableFailbackRegistry 中间层? (ZK 特定优化)
17. 为什么 AddressListener 链? (3.x 地址可插拔)

## 学生视角

18. 什么是注册中心? (服务地址的协调系统)
19. 什么是 subscribe/notify? (推模式)
20. 什么是动态目录? (订阅驱动的 invoker 列表)
21. 什么是临时节点? (ZK 连接断自动删)
