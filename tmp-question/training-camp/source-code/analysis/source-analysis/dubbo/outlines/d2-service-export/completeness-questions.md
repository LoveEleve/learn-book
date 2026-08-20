# D-2 服务导出 — completeness-questions (全视角提问验证)

## 开发者视角

1. 导出入口? (export → doExport → ... 6 层)
2. scope 三态? (NONE/LOCAL/REMOTE)
3. 本地导出? (injvm port 0)
4. 远程导出? (registry 遍历)
5. invoker 怎么生成? (proxyFactory)
6. protocol 怎么选? (自适应 URL 参数)
7. server 怎么启动? (Exchangers.bind)
8. exporter 存哪? (exporterMap)

## 架构师视角

9. 为什么 6 层链? (配置→URL→invoker→protocol 渐进)
10. 为什么本地始终导? (injvm 引用通道)
11. 为什么 serverMap 缓存? (同地址多服务共享)
12. 为什么 reset? (override 动态配置)
13. 为什么 Wrapper 织入? (export 时挂监听/过滤)
14. 为什么元数据发布? (元数据中心服务发现)
15. 对照 ZK Netty? (服务端网络层)
16. 为什么 RegisterType? (注册模式细分)

## 学生视角

17. 什么是导出? (服务发布)
18. 什么是 invoker? (可调用单元)
19. 什么是 exporter? (导出句柄)
20. 什么是 injvm? (本地调用)
