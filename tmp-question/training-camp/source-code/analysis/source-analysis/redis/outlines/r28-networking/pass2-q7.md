# 闭环笔记 q7: freeClient — 释放链与角色联动

## 假设
freeClient = 全量清理链 (协议态/阻塞/watch/pubsub/IO) + 角色联动 (master 缓存/从库 RDB)。

## 验证过程
- freeClient (networking.c:1578-1740):
  - **PROTECTED → 转异步** (L1583-1586): 保护中的客户端不立即释放
  - 模块事件 (L1589-1596): disconnect 事件 + auth 变更通知
  - **async 队列摘除** (L1605-1609): CLOSE_ASAP 已在 clients_to_close → 摘除 (防双释放)
  - **master 缓存** (L1616-1623): 断 master → replicationCacheMaster (保留状态供部分重同步, R-9) — 协议错误/阻塞除外
  - 释放链:
    1. querybuf (L1631-1633)
    2. 阻塞态 (L1635-1639): unblockClient + bstate.keys
    3. watch (L1641-1643): unwatchAllKeys + watched_keys
    4. pubsub (L1645-1652): 全退订 + 三 dict
    5. reply 链表 + buf (L1654-1656)
    6. repl 缓冲引用 (L1657: freeReplicaReferencedReplBuffer)
    7. argv/original_argv (L1658-1659)
  - 内存记账移除 (L1668-1670)
  - unlinkClient (L1675): 关 socket + 摘事件 + 各链表
  - **从库联动** (L1679-1697): WAIT_BGSAVE_END 且无其他从库 → killRDBChild (L1687-1694); SEND_BULK 关 repldbfd
- freeClientAsync (L1742): 挂 clients_to_close 队列 (事件循环安全释放)
- freeClientsInAsyncFreeQueue (L1810): 批量处理
- unlinkClient (L1451-1520): 从 server.clients/pending 链表摘除 + connClose

## 代码类型
Mechanism (生命周期)

## 跨域关联
- R-9 (master 缓存/从库 RDB) / R-26 (阻塞) / R-16 (watch) / R-29 (pubsub)

## 结论
释放 = **"角色优先, 再清数据"**: master 缓存 (复用), 从库杀 RDB 子进程 (资源), 然后协议态 → 订阅面 → 内存面 → IO 面。异步释放队列保证事件循环内安全。
源码位置: networking.c:1451,1578-1810
