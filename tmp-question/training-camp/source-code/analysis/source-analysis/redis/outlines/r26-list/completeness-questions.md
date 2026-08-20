# R-26 t_list+blocked — completeness-questions

## 开发者视角

1. list 的两种编码什么时候切换?
2. LPUSHX 不存在键返回什么?
3. LPOP COUNT 和 LPOP 回复格式差异?
4. 弹空 list 会发生什么?
5. 阻塞客户端有哪些状态字段?
6. blockForKeys 的双向注册怎么维护?
7. signalKeyAsReadyLogic 的三个快速返回?
8. 唤醒后命令怎么重新执行?

## 架构师视角

9. "空键不存在不变量" — 为什么唤醒只需 dbAdd 路径?
10. blocked_clients_by_type 计数在就绪队列的用途?
11. 双向节点关联 (list node 作 value) 的 O(1) 解链设计?
12. ready_keys dict 防重 — 脚本多 push 只醒一次的原理?
13. 新列表交换 (换 server.ready_keys) 支持 BLMOVE 连环唤醒?
14. 类型匹配 (L578) 防什么 (键被错误类型覆盖)?
15. 超时为什么用 cron 粒度而非精确时钟?
16. master 客户端为什么不能阻塞?

## 学生视角

17. BLPOP 一次完整生命周期 (阻塞→唤醒→重执行)?
18. LPUSH 3 次 (脚本内) 唤醒队列怎么去重?
19. 两个客户端 BLPOP 同一键, push 一个元素谁先醒?
20. BLMOVE 唤醒 BLMOVE 的连环场景?
21. 阻塞键表在 SWAPDB 时怎么处理 (R-21 连接)?
