# 闭环笔记 q4: 本地事务 + END_TRANSACTION — 状态回传

## 假设
本地事务执行完 → 客户端 oneway 回传状态 → broker 校验后 commit 重投 / rollback 弃置。

## 验证过程
- **本地事务执行**: executeLocalTransaction (DefaultMQProducerImpl:1454-1459) — 仅 SEND_OK 分支; 返回 null → UNKNOW (L1460-1462); 异常 → localException (L1468-1472); FLUSH_DISK_TIMEOUT/FLUSH_SLAVE_TIMEOUT/SLAVE_NOT_AVAILABLE → 强制 ROLLBACK (L1475-1479) ⚠ 注: 此时消息可能已落盘
- **endTransaction** (L1508-1548): decodeMessageId(offsetMsgId) 取物理 offset → **endTransactionOneway (END_TRANSACTION=37, 超时=sendMsgTimeout)** (L1546-1547); broker 地址从 publish 表解析 (L1520-1521); fromTransactionCheck 未设 (false)
- **EndTransactionProcessor** (349): SLAVE 拒收 SLAVE_NOT_AVAILABLE (L64-68) → **三校验 checkPrepareMessage** (L220-248): PGROUP 一致 + tranStateTableOffset==queueOffset + commitLogOffset 一致 — 任一不符 SYSTEM_ERROR (防伪造/串消息)
- **rejectCommitOrRollback** (L199-213): 非回查 + 自定义免疫期超时 → **ILLEGAL_OPERATION (604)** — 过期事务拒绝
- **COMMIT 路径**: commitMessage (读半消息 by commitLogOffset) → endMessageTransaction (L250-273): REAL_TOPIC/REAL_QID 还原 + tagsCode 重算 (MULTI_TAGS_FLAG 判定) + 清 REAL_* + **PreparedTransactionOffset=半消息物理 offset (L144)** → sendFinalMessage (L275-348): PUT_OK/FLUSH_DISK_TIMEOUT/FLUSH_SLAVE_TIMEOUT/SLAVE_NOT_AVAILABLE 均 SUCCESS → **deletePrepareMessage 写 OP** (L149)
- **ROLLBACK 路径**: rollbackMessage → 校验 → deletePrepareMessage 写 OP (L176) — 半消息本体不动, 靠 OP 标记弃置 (对账跳过)
- **OP 消息**: RMQ_SYS_TRANS_OP_HALF_TOPIC + tag="d" (REMOVE_TAG) + body=半消息 queueOffset 列表 ("," 分隔) — TransactionalMessageUtil.java
- **5.x 批量 OP**: deletePrepareMessage 入 deleteContext 队列 (per queueId, 容量 20000ms 过期) → TransactionalOpBatchService 3s 周期攒批写 (4096B 上限) (TransactionalMessageServiceImpl:596-752)

## 代码类型
Implementation (状态机 + 校验 + 还原重投)

## 跨域关联
- RM-1 (协议): END_TRANSACTION=37 请求码; oneway 语义
- RM-3 (存储): PreparedTransactionOffset 段 / tagsCode 重算 (MessageExtBrokerInner.tagsString2tagsCode)
- RM-12 (HA): SLAVE 拒收 END — 事务回传仅主可处理 (交叉)

## 结论
END = 客户端 oneway 回传 (COMMIT/ROLLBACK/UNKNOW); broker 三校验防串, 免疫期校验防过期, commit 还原重投真实消息; 无论 commit/rollback 均写 OP 消息供回查对账。
源码位置: DefaultMQProducerImpl.java:1508-1548; EndTransactionProcessor.java:64-68,199-273,275-348
