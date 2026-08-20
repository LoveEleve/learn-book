# 闭环笔记 q5: 消费读面 + 恢复面

## 假设
读 = queueOffset → CQ 单元 → CommitLog 定位; 恢复 = CQ 与 CommitLog 对齐截断。

## 验证过程
- **getMessage** (DefaultMessageStore:793+): 守卫 (shutdown/runningFlags.isReadable) → CQ 定位 (queueOffset → 单元) → 校验 → CommitLog selectMappedBuffer (maxMsgNums/maxTotalMsgSize 双限) → GetMessageResult (消息 + 状态: FOUND/NO_MATCHED/…)
- **异步面** (getMessageAsync): 5.x CompletableFuture (长轮询挂起 — RM-8 交叉)
- **恢复面** (ConsumeQueue.recover L123-181): 逐文件扫描 20B 单元 → 校验 offset/size/tagsCode → **推进 maxPhysicOffset** (L146) → 尾文件截断 (truncateDirtyFiles L181) — **CQ 与 CommitLog 对齐** (半写单元丢弃)
- **commitlog recover**: 按文件魔数/校验和定位有效写入点 (与 CQ 对齐截断 — RM-2 交叉)
- **一致性注释** (DefaultMessageStore:418): "eliminating the dispatch inconsistency between the commitLog and consumeQueue at the end of recovery"

## 代码类型
Implementation (读链 + 恢复)

## 跨域关联
- RM-8 (消费): getMessage 拉取
- RM-2 (MappedFile): selectMappedBuffer

## 结论
读 = 守卫 → CQ 定位 → CommitLog mmap 切片 → 双限 (条数/字节); 恢复 = CQ 单元校验推进 maxPhysicOffset + 尾文件截断对齐 (半写丢弃)。
源码位置: DefaultMessageStore.java:418,781-830; ConsumeQueue.java:123-181
