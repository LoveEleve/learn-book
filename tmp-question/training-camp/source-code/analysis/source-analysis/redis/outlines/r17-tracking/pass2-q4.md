# 闭环笔记 q4: BCAST 广播面 — 前缀聚合与批量发送

## 假设
BCAST 模式不记键级表; 键修改 → 前缀匹配聚合 → 事件循环周期末每前缀一条批量消息。

## 验证过程
- **bcastState** (tracking.c:31-38): keys (本周期修改的键) + clients (订阅该前缀的客户端) 双 rax — 每前缀一份
- **PrefixTable**: rax[prefix → bcastState]; 空串前缀 "" = 全量广播 (L183: numprefix==0 自动注册空前缀; 测试 L61-69 "BCAST with the empty prefix")
- **注册** (L136-155): enableBcastTrackingForPrefix — 首客户端建 bcastState; raxTryInsert(bs->clients, &c 8B) 去重; 客户端侧 client_tracking_prefixes 反向记录 (退订时用)
- **前缀冲突检查** (L83-132): checkPrefixCollisionsOrReply — stringCheckPrefix (memcmp min_len) — **两两互斥 (任一前缀不互为前缀)**; 检查对象: 新输入内部 (L117-129) + 与既有客户端前缀 (L96-116); 错误消息 "Prefixes for a single client must not overlap" (测试 L398-414)
- **修改聚合** (L319-335): trackingRememberKeyToBroadcast — 全量扫 PrefixTable: `ri.key_len > keylen → continue` + `ri.key_len != 0 && memcmp(ri.key,keyname,ri.key_len) != 0 → continue` (**前缀 = key 开头子串, O(前缀数)**); 命中 → raxInsert(bs->keys, keyname, **c 指针作 value**) — value 记录最后修改者, NOLOOP 过滤用
- **周期发送** (L586-632): trackingBroadcastInvalidationMessages (beforeSleep server.c:1724, 每个事件循环周期): 每前缀 keys 非空 → ① 构建公共协议 trackingBuildBroadcastReply(NULL,keys) (L541-581: *N\r\n$len\r\nkey 预序列化) ② 逐客户端: NOLOOP → 个性化构建 (排除 ri.data==c 即自己改的键, L610-616) / 否则共享 proto (L617-618) ③ sendTrackingMessage(proto=1) ④ 清空 keys 重建 (L628-629) — **聚合=每前缀一条消息** (测试 L71-88 断言两前缀两条消息)
- **NOLOOP 两种实现路径**: 非 BCAST (L385-391 发送时逐键跳过) vs BCAST (L610-616 构建时排除 — 因为发送对象是整批客户端)
- **trackingInvalidateKey 的 BCAST 消费**: 只调度 (L359-360), 不发 — 表内 BCAST 客户端跳过 (L380)
- **内存承诺**: BCAST 模式主表零内存 (redis.conf L864-865; enableTracking 不建主表条目)
- **退订** (L49-71): disableTracking — 遍历客户端前缀逐个 raxRemove(bs->clients); 最后一个客户端 → 释放整个 bcastState + 删 PrefixTable 条目

## 代码类型
Implementation (广播聚合批处理)

## 跨域关联
- R-10a (rax): rax 迭代器 (raxSeek "^") + 二进制 key
- R-2 (events): beforeSleep 编排 (每周期发送)
- R-28 (networking): sendTrackingMessage / CLIENT_PUSHING

## 结论
BCAST = 前缀→(keys+clients) 状态; 修改时 O(前缀数) 匹配聚合; 事件循环周期末每前缀一条批量消息 (公共 proto 共享 + NOLOOP 个性化); 空前缀 = 全量。键级表零内存是 BCAST 的核心卖点。
源码位置: tracking.c:31-38,83-155,319-335,541-632; server.c:1724; redis.conf:864-865
