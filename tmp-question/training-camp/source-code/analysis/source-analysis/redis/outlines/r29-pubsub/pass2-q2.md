# 闭环笔记 q2: 订阅/退订 — 双向注册与引用计数

## 假设
订阅 = 双向注册 (client dict + server 镜像) + 引用计数保护; 退订摘空即删频道。

## 验证过程
- pubsubSubscribeChannel (pubsub.c:238-271):
  - **dictFindPositionForInsert 预定位** (L245) — 一次哈希找到插入位, 避免二次查找 (R-27 交叉)
  - 服务器侧 kvstoreDictAddRaw (L253): 频道已存在 → 复用 clients dict (L255-257); 新频道 → 创建 clientDictType dict + incrRefCount(channel) (L259-262)
  - 客户端侧 dictInsertAtPosition (L265) + incrRefCount (L266) — 双注册, 频道对象在两个 dict 各持一引用
  - 返回 0/1 + 总是回复订阅确认 (L269)
- pubsubUnsubscribeChannel (L275-307):
  - incrRefCount 保护 (L282-283, "channel may be just a pointer to the same object") → 删 client dict → 服务器侧删客户端 (L293) → **客户端表空则删频道** (L294-299, 注释 "otherwise it will be possible to abuse Redis PUBSUB creating millions of channels")
- pubsubSubscribePattern (L340-362): 同构 — c->pubsub_patterns dictAdd (L345) + server.pubsub_patterns 镜像 (L349-357)
- 批量退订 (L393-411, L431-448): **dictGetSafeIterator 安全迭代器** (L396, rehash 安全); **零订阅仍回 null** (L407-408, "We were subscribed to nothing? Still reply")
- cluster slot 迁移面: pubsubShardUnsubscribeAllChannelsInSlot (L310-337) — 槽迁移时逐客户端退订 + 零订阅退出 pubsub 模式 (L329-331)
- freeClient 清理 (networking.c:1548-1550,1645-1648): 断开时退订全部

## 代码类型
Mechanism (双向注册)

## 跨域关联
- R-3: dict 安全迭代器
- R-27: dictFindPositionForInsert 预定位
- R-28: freeClient 清理链
- R-15 (未来): slot 迁移面

## 结论
订阅 = 双表注册 (客户端↔服务器镜像) + 双引用; 退订 = 反向摘除 + 空频道即删 (防滥用); 断开自动清理。
源码位置: pubsub.c:238-311,340-448; networking.c:188-190,1548-1550
