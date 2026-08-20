# 闭环笔记 q1: 共同模式 — 顺序节点 + 最小序号 + 前驱 watch

## 假设
三 recipes (选举/锁/队列) 共享同一模式: 顺序节点 + 最小序号判定 + 前驱 watch。

## 验证过程
- **顺序节点**: 选举/锁用 **EPHEMERAL_SEQUENTIAL** (会话绑定, 断开自动清理 — LeaderElectionSupport:176, WriteLock:196); 队列用 **PERSISTENT_SEQUENTIAL** (数据跨会话存续 — DistributedQueue:265) — 节点名 "<前缀><%010d 序列>" (Z-3 交叉)
- **最小序号判定** (三实现三种写法):
  - 选举: getChildren → LeaderOffer.IdComparator 按 Integer id 排序 → 找自己下标 i, i==0 → leader (LES:207-224)
  - 锁: getChildren → TreeSet\<ZNodeName\> 排序 → ownerId = first → headSet(idName) 判前驱 (WriteLock:229-253)
  - 队列: getChildren → TreeMap\<Long\> 按序列 → 最小者 = 队头 (DQueue:65-87)
- **前驱 watch** (两个触发面):
  - 选举: exists(前驱, this) — NodeDeleted → process() → determineElectionStatus 重跑 (LES:239,327-341)
  - 锁: exists(前驱, new LockWatcher()) — 删除事件 → lock() 重试 (WriteLock:239,158-168)
  - 队列: 不 watch 前驱 — 队头变化靠 take() 的 getChildren watcher (DQueue:232-242)
- **语义核心**: 序号全序 → "最小者胜" 天然互斥 — 无需互斥协议; 前驱 watch 把 O(N) 轮询降为 O(1) 事件驱动

## 代码类型
Architecture (分布式原语模式)

## 跨域关联
- Z-3: 顺序节点 %010d + ephemeral 会话清理
- Z-5: 临时节点会话绑定 (断开自动回收)
- Z-6: exists 前驱 watch 语义 (NONODE 也注册 = 创建 watch — ZooKeeper:321-323)
- Z-7: ZooKeeper 客户端 API 组合 (create/getChildren/getData/exists/delete)

## 结论
三 recipes = 同一原语: **全序序号 + 最小者胜 + 前驱事件驱动**; 差异只在节点生命周期 (临时=选举/锁, 持久=队列) 与触发面。
源码位置: LeaderElectionSupport.java:174-224,239; WriteLock.java:185-258; DistributedQueue.java:65-129,203-255
