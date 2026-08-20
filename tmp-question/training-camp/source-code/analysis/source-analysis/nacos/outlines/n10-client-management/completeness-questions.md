# N-10 客户端管理面 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. ClientService 门面为什么委托 ClientManager? 查询与状态为何分离?
2. 三管理器的分型依据 (gRPC/临时/持久)?
3. AbstractClient 的 generateSyncData 在集群同步里的角色?
4. ClientFactory 与 ClientManager 的分工?
5. 实例操作面为什么临时/持久双实现?

## B. 源码实证 (6)

6. ClientServiceImpl 怎么取连接详情? (grep L91)
7. ClientManager 接口的方法集? (grep 接口)
8. 三管理器的类名? (grep impl/)
9. AbstractClient.release 的语义? (grep L182)
10. 操作双实现 (Ephemeral/Persistent) 的规模与职责? (grep 503 行类)
11. NamingCleaner 族的实现? (grep v2/cleaner)

## C. 推理深挖 (5)

12. gRPC 连接断开时 ConnectionBasedClient 的释放链?
13. syncClientConnected 的集群同步客户端与本地客户端差异?
14. ClientAttributes 怎么驱动工厂分派?
15. ExpiredMetadataCleaner 的清理周期与条件?
16. NamingEventPublisher 与 NotifyCenter 的关系?

## D. 跨域扩展 (4)

17. 本域 clientId 模型 vs NC-1 客户端: 两端 clientId 闭合?
18. 三管理器 vs NC-1 双代理路由: 服务端/客户端分型对应?
19. ClientSyncData vs NC-5 Distro: 同步载体与协议?
20. 本域 vs openjdk 的线程/进程模型: 客户端对象管理的规划对照?
