# N-26 公共底座 — 完备性问题集 (20 问)

## A. 机制理解 (5)
1. JSON/Hessian 双序列化的选择?
2. 包扫描的自实现动机?
3. 常量面的集中语义?
4. Hessian 与 Jackson 的序列化差异?
5. 包扫描的类加载器策略?

## B. 源码实证 (6)
6. 序列化实现类名? (grep consistency/serialize)
7. 包扫描文件数? (grep packagescan)
8. 常量类清单? (grep constant/)
9. JacksonSerializer 的配置?
10. HessianSerializer 的类型注册?
11. 包扫描的资源类型?

## C. 推理深挖 (5)
12. ServerConfigChangeEvent 的触发?
13. 能力面的协商机制?
14. 序列化在协议里的应用点 (NC-5)?
15. 包扫描与 Spring 扫描的关系?
16. 能力 (ability) 面的语义?

## D. 跨域扩展 (4)
17. 序列化性能与兼容性的权衡?
18. 包扫描与 Spring 扫描的重叠?
19. 能力面的版本兼容?
20. 生命周期接口 (Closeable) 的关闭链?

