# C-7 持久节点与组成员 — completeness-questions (全视角提问验证)

## 开发者视角

1. PersistentNode.start() 后怎么知道创建成功了? waitForInitialCreate 怎么用?
2. setData 什么时候被拒绝? authFailure/parentCreationFailure 是啥?
3. PersistentNode 节点被别人删了会怎样? 自动重建吗?
4. PersistentTtlNode 的 ttl 参数含义? touch 频率怎么定?
5. GroupMember.getCurrentMembers() 返回什么? 自己一定在里面吗?
6. GroupMember 换 payload 用哪个方法?

## 架构师视角

7. 为什么三种触发重建? 每种覆盖什么故障场景?
8. createNode 的路径复用/剥后缀/降级三态分别为什么?
9. close 后迟到回调怎么防孤儿节点? guaranteed 删除在哪兜底?
10. CONTAINER + TTL 父子分层解决什么问题? 直接父节点 TTL 的缺点?
11. GroupMember 为什么复用 CuratorCache 而不是自己写 watcher?
12. 与 Eureka/Consul 的注册保活机制对比?
13. 会话过期 vs TTL 过期: 两种"消失"语义的适用场景?
14. PersistentNode 与 C-3 锁的 ephemeral 语义有什么呼应?

## 学生视角

15. 什么是临时节点? 会话断了会怎样?
16. 什么是心跳? 为什么要心跳?
17. 什么是组成员? 和注册中心什么关系?
18. 什么是 CONTAINER 节点? 什么时候回收?
