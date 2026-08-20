# NC-5 一致性协议 Distro + SOFA-JRaft — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. ConsistencyProtocol 统一接口为上层提供什么? 双轨怎么分叉?
2. Distro 的"最终一致"靠哪些任务保证? (load/verify/sync)
3. JRaftServer 是集成面还是自研? 封装的哪些组件?
4. 写走 Raft 读走状态机的理由? 无 Leader 时怎么办?
5. AP/CP 选型在 Nacos 里谁决定? (配置 vs 注册)

## B. 源码实证 (6)

6. DistroProtocol 的成员构成? (grep L44-54)
7. onReceive 的处理器查找? (grep L164-173)
8. JRaftProtocol implements 什么? (grep L93-94)
9. JRaftServer.start 与 shutdown 的对称? (grep L190/L373-389)
10. leader 事件怎么更新元数据? (grep JRaftProtocol:133-140)
11. NacosWriteRequestProcessor 的处理路径? (grep processor/)

## C. 推理深挖 (5)

12. DistroVerifyTimedTask 定时校验与 onReceive 的关系?
13. 为什么 Distro 的组件按 resourceType 注册? 双资源怎么隔离?
14. Raft 提交失败 (NoLeader) 时, closure 的回调语义?
15. 快照 (JSnapshotOperation) 在重启恢复中的作用?
16. FailoverClosureImpl 的兜底语义?

## D. 跨域扩展 (4)

17. JRaftProtocol vs SofaJRaft 4.6 域: 集成面与内核的边界?
18. Distro AP vs ZK 4.3 的 ZAB: 两种 AP/CP 对照?
19. 一致性协议 vs ALI-A3 的 NacosServiceManager: 服务端与客户端分工?
20. 本域与执行计划 5.8 的 NC-3/NC-4 (AP/CP 域): 详细规划 vs 执行计划的一致性面?
