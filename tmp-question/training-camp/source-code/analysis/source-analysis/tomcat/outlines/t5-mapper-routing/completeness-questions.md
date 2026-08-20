# T-5 Mapper 路由 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

> 格式升级: 旧多视角 8 问 → 新 A/B/C/D 四节 20 问 (2026-08-17 查漏补缺)

### A. 机制理解 (5)
1. exactWrappers/wildcardWrappers/extensionWrappers 三个数组有什么区别? 各匹配什么 URI 模式?
2. 同一个 URI `/app/user` 同时匹配 exact `/user` 和 prefix `/*` — 哪个生效? 判定顺序?
3. Tomcat 为什么用 Container 树 + Mapper + Pipeline 三套独立结构? 不能合一吗?
4. Mapper 的路由表用数组+二分查找而非 HashMap — 为什么?
5. mappingData.host/context/wrapper 三个字段是谁填的? 填充时机?

### B. 源码实证 (6)
6. Mapper 类声明与 final 修饰? (grep catalina/mapper/Mapper.java:47)
7. findContextVersion 方法签名与 silent 参数语义? (grep Mapper.java:372)
8. wildcardWrappers 数组的扩容代码? (grep Mapper.java:449-452)
9. MapperListener.containerEvent() 怎么区分 ADD_CHILD_EVENT 和 ADD_MAPPING_EVENT? (grep catalina/mapper/MapperListener.java:147-178)
10. registerHost 方法位置? (grep MapperListener.java:292)
11. Mapper 的 map() 方法签名? (grep Mapper.java map)

### C. 推理深挖 (5)
12. MapperListener 启动时怎么构建路由表? 运行时热部署怎么更新? 为什么用事件而非轮询?
13. 数组+二分 vs HashMap — 路由表为什么选前者? 查询频率 vs 更新频率?
14. 上下文路径 (contextPath) 匹配的边界 — `/app` 和 `/app/` 怎么处理? URL 解码在哪步?
15. Host 多别名 (alias) 怎么匹配? 通配域名 (*.example.com) 支持吗?
16. Mapper 是线程安全的吗? 热部署更新路由表时正在查询的请求会看到什么?

### D. 跨域扩展 (4)
17. 本域 vs T-2 Adapter: mapper.map() 在 CoyoteAdapter.service() 的调用点与顺序?
18. 本域 vs Spring MVC: Spring 的 HandlerMapping (AntPathMatcher) vs Tomcat Mapper — 路由解析分层?
19. 本域 vs Nacos: Nacos 的寻址 (ServiceManager) vs Tomcat Mapper — 都是"名字→实例"映射?
20. 本域 vs openjdk: String 的 indexOf vs 二分查找 — 不同数据规模下查表策略?