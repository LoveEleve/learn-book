# 闭环笔记 q3: MappedFileQueue — 文件序列管理

## 假设
文件列表 = 顺序追加的 1GB 段; 定位按 offset; 满文件滚动; 过期删除。

## 验证过程
- **滚动** (getLastMappedFile L299-322): 空 → createOffset = startOffset - (startOffset % mappedFileSize) (**按文件大小对齐**); 满 (isFull) → createOffset = 当前文件起始 + mappedFileSize; needCreate 才创建
- **shouldRoll** (L337-344): 当前文件满 或 wrotePosition+msgSize > fileSize → 滚动
- **定位**: findMappedFileByOffset (offset → 文件索引: offset/mappedFileSize 定位 + 边界修正)
- **删除** (deleteExpiredFile L210-231): 保留策略 (磁盘空间/时间 — destroyExpiredFiles 由 broker 定时触发)
- **提交/刷盘聚合**: commit (全部文件批量 commit) / flush (flushedWhere 推进) — 刷盘服务消费
- **getFlushedWhere/getCommittedPosition** (L290-298): 全局刷盘水位 — 组提交判定用
- **MultiPathMappedFileQueue** (5.x): 多盘路径分配 (RM-16 交叉)

## 代码类型
Implementation (文件序列容器)

## 跨域关联
- RM-3 (CommitLog): 写入入口
- RM-5 (Broker): 磁盘空间清理调度

## 结论
文件队列 = 对齐到 1GB 的段序列; 滚动 (满/越界) + offset 定位 (除法+边界) + 批量 commit/flush (水位推进) + 过期删除; 5.x 多盘扩展。
源码位置: MappedFileQueue.java:210-344; MultiPathMappedFileQueue.java
