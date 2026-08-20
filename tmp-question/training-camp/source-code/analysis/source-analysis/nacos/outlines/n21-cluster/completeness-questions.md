# N-21 集群管理面 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. ConcurrentSkipListMap 选型的理由 (成员有序)?
2. Lookup 三方式的选择逻辑 (工厂)?
3. 成员变更事件的通知面?
4. 成员表的有序性对选主/同步的意义?
5. Lookup 回调 (afterLookup) 的时机?

## B. 源码实证 (6)

6. ServerMemberManager 关键 API? (grep L76-92)
7. 自注册? (grep L169)
8. LookupFactory 类型判定? (grep L107-130)
9. 三 Lookup 实现? (grep lookup/)
10. MemberReportHandler 的职责? (grep remote/)
11. 模块健康检查? (grep health/)

## C. 推理深挖 (5)

12. 不健康节点上报任务的周期与语义?
13. 成员变更与 Distro 数据迁移的联动?
14. 单机模式 (StandaloneMemberLookup) 的成员集?
15. 文件 Lookup 的格式与刷新?
16. 地址服务器 Lookup 与客户端 ServerListProvider 的对称?

## D. 跨域扩展 (4)

17. 本域 vs N-04 客户端地址面: 服务端/客户端 Lookup 对称?
18. 成员事件 vs N-24 事件中心: 通知通道?
19. 集群健康 vs N-14: 状态机消费?
20. 本域 vs openjdk 的 JMX 集群: 集群管理规划对照?
