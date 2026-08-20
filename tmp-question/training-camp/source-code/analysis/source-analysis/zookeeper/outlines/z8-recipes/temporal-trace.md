# Z-8 Recipes — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.x | recipes 骨架: 三模块 (election/lock/queue) + 顺序节点 + 前驱 watch 模式定型 (WriteLock 注释锚 "ZK will remove ephemeral files" L122-124 / "in the middle of creating" L216-218 / "come back in order ususally" L228) |
| 3.5.x | **Java 8 现代化**: ZNodeName Optional\<Integer\> + parseSequenceString (Optional.ofNullable, ZNodeName:43,63-74); LeaderElectionSupport 状态机细化 |
| 3.6.x | **负序号/前缀横线修复**: ZNodeName 双横线处理 (idx>0 && charAt(idx-1)=='-', L59-61) — "x--20" → seq=-20 (ZNodeNameTest testOrderWithSamePrefix) |
| 3.9.x | SpotBugs 注解面 (@SuppressFBWarnings NP_NULL_PARAM_DEREF_NONVIRTUAL, WriteLock:209-211); JUnit5 测试现代化 (junit-jupiter-engine, recipes pom) |

## 痕迹证据

- WriteLock.java:122-124: "we don't need to retry this operation in the case of failure as ZK will remove ephemeral files" — ephemeral 自动清理语义 (3.4 锚)
- WriteLock.java:216-218: "lets try look up the current ID if we failed in the middle of creating the znode" — 幂等创建恢复 (3.4 锚)
- WriteLock.java:228: "lets sort them explicitly (though they do seem to come back in order ususally :)" — 排序显式化 (3.4 锚)
- ZNodeName.java:59-61: 双横线负序号处理 — 3.6 修复面
- WriteLockTest.java:142-143: "workAroundClosingLastZNodeFails ... due to bug!" — 已知缺陷 workaround (非修复)
- LeaderElectionSupport.java:66-87: Javadoc caveats "best effort" + "poorly implemented process" — 边界承认 (3.4 锚)

## 推断标注

- "3.4.x 骨架" — recipes 随 ZK 3.4 引入 (公知版本线) (标注)
- "3.5.x Optional" — Java 8 Optional 引入年代推断 (标注)
- "3.6.x 负序号修复" — 双横线场景推断 (sessionId 横线前缀 + 负序列表述) (标注)
- "3.9.x SpotBugs/JUnit5" — pom 实证 (junit-jupiter-engine + spotbugs-annotations) (实证)
- git shallow (1 commit "Prepared 3.9.5") — 无考古, 全注释锚

## 对照线 (Curator 4.5, 阶段 4.5 深挖)

- LeaderElectionSupport → LeaderLatch (LeaderElectionSupport 状态机 vs Curator latch 语义 + 连接状态回调)
- WriteLock → InterProcessMutex (官方无可重入/无超时 vs Curator 可重入 + acquire(timeout) + 公平锁)
- DistributedQueue → Curator DistributedQueue (官方无 ack vs Curator 双 consumer 模式)
