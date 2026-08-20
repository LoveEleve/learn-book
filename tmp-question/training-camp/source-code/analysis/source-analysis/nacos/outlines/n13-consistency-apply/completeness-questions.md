# N-13 一致性落点 — 完备性问题集 (20 问)

## A. 机制理解 (5)
1. Distro 组件五件套各管什么?
2. ClientSyncData 与 Distro 数据的关系?
3. 快照批量协议的语义?
4. 五件套 (processor/storage/agent/failed/verify) 的注册顺序?
5. ClientSyncData 与 DistroData 的转换?

## B. 源码实证 (6)
6. DistroClientDataProcessor 的注册? (grep v2/)
7. 组件注册表类? (grep DistroClientComponentRegistry)
8. 快照操作基类? (grep AbstractSnapshotOperation)
9. DistroClientDataProcessor 的 processData 分支?
10. DistroClientTransportAgent 的同步目标选择?
11. DistroClientTaskFailedHandler 的重试语义?

## C. 推理深挖 (5)
12. DistroClientVerifyInfo 的校验内容?
13. 快照批量读写与普通读写的差异?
14. verify 校验信息的校验周期与语义?
15. 批量读写与单个读写的取舍?
16. OldDataOperation 兼容什么?

## D. 跨域扩展 (4)
17. verify 任务与其他同步任务的频率对比?
18. 批量读写的批次上限?
19. OldDataOperation 兼容的数据格式?
20. 快照加载失败的降级?

