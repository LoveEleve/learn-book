# Z-3 DataTree — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.x | 基础树骨架: DataTree/DataNode/NodeHashMap + createNode/deleteNode/setData + ephemerals + ACL 缓存 (引用计数注释风格) |
| 3.5.x | **tree digest**: NodeHashMapImpl + DigestCalculator + pre/postChange + DIGEST_LOG (DIGEST_LOG_LIMIT=1024/INTERVAL=128, L169-173) — 跨节点一致性校验; TxnDigest 事务级校验 (L845) |
| 3.6.x | **containers/ttls 分类** (EphemeralType.get L484) + createContainer/createTTL OpCode — 容器/TTL 节点 |
| 3.9.x | **fuzzy snapshot 一致性**: ACL 先入缓存 (L446-457 注释) + cversion/pzxid replay 保护 (L470-478 注释) + pzxid 单调 (L548-553 注释) |

## 痕迹证据

- DataTree.java:169-173: DIGEST_LOG_LIMIT/INTERVAL (3.5 锚)
- DataTree.java:446-457: fuzzy snapshot ACL 注释 (3.9 面)
- DataTree.java:470-478: replay 模糊窗口 cversion 注释
- DataTree.java:548-553: pzxid 单调保护注释
- DataTree.java:484-494: EphemeralType (3.6 锚)
- DataTree.java:149: STAT_OVERHEAD_BYTES = 68B
- NodeHashMapImpl.java:31-120: digest 增量钩子

## 推断标注

- "3.4.x 骨架" — 公知版本线 (ZK 3.4 数据树定型) (标注)
- "3.5.x digest" — tree digest 随 KIP-xxx 类特性 3.5 引入推断 (标注); 3.5 引入事务校验为公知
- "3.6.x containers/ttls" — 容器节点 3.5.1/ttl 3.5.3 推断 (标注)
- git shallow (1 commit) — 无考古, 全注释锚
