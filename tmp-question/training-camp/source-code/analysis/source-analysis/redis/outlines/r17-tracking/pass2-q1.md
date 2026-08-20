# 闭环笔记 q1: 双层 rax 表结构与标志位

## 假设
TrackingTable = rax[key → rax[clientID]] 嵌套; 7 个跟踪标志位控制行为面。

## 验证过程
- **表结构** (tracking.c:12-23): "The tracking table is constituted by a radix tree of keys, each pointing to a radix tree of client IDs" — 双层 rax 权威注释
- **惰性创建** (L174-178): 首个客户端开启 tracking 时 TrackingTable=raxNew() + PrefixTable + TrackingChannelName 一并创建; 键修改时 TrackingTable==NULL 直接 return (L354)
- **key→ids**: trackingRememberKeys L229-236: raxFind 未命中 → raxNew() 建 ids 子树 + raxTryInsert 挂载 (assert inserted==1); 命中 → 复用
- **id 存储** (L237): `raxTryInsert(ids,(unsigned char*)&tracking->id,sizeof(tracking->id),...)` — **客户端 ID (uint64) 直接作 rax 二进制 key**, 8B 原生字节序 (trackingInvalidateKey L370-371 反向 memcpy 回读)
- **字节序不对称**: 对比 clients_index (networking.c:85, 90) 用 `htonu64` (网络序) 存储 — tracking 表内部自洽 (同文件同序), 与 clients_index 序不同但互不混用 (查表各走各的: lookupClientByID L1833 再 htonu64)
- **计数**: TrackingTableTotalItems = 全表 ID 总数 (L25-28 注释: 服务器 CSC 内存量的提示); 递增仅在 raxTryInsert 成功 (L238, 同客户端同键不重复计数)
- **统计面**: trackingGetTotalKeys (raxSize(TrackingTable)) / GetTotalItems / GetTotalPrefixes (L636-648) → INFO server.c:5905-5907
- **标志位 7 个** (server.h:361-370): TRACKING(1<<31)/BROKEN_REDIR(32)/BCAST(33)/OPTIN(34)/OPTOUT(35)/CACHING(36)/NOLOOP(37); CLIENT_PUSHING(1<<46, L385)
- **客户端字段** (server.h:1249-1250): client_tracking_redirection (uint64) + client_tracking_prefixes (rax)
- **内存记账**: 前缀表低估记账 (networking.c:3874-3876: numnodes×(sizeof(raxNode)×sizeof(raxNode*))); 主表节点 zmalloc 自然进 used_memory; clientMemUsage 的 tracking 面仅前缀

## 代码类型
Implementation (双层 rax 容器 + 记账)

## 跨域关联
- R-10a (rax): 压缩节点/原子子 — 本域消费 rax 的二进制 key + 迭代器
- R-21 (db): signalModifiedKey 是失效入口
- R-28 (networking): lookupClientByID / clients_index / CLIENT_PUSHING

## 结论
表 = 双层 rax (key→ID 集合), 惰性创建, ID 原生序 8B 二进制 key; 7 标志位 + 2 客户端字段; 字节序与 clients_index 刻意不同 (内部自洽)。INFO 三统计。
源码位置: tracking.c:12-38,164-193,201-241,636-648; server.h:361-370,1249-1250; networking.c:85-95,1832-1837,3874-3876
