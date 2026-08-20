# 闭环笔记 Q3 — RingHash: Ketama 一致性哈希 (XxHash64 + 虚拟节点)

假设: RingHash 实现 Ketama: 地址哈希到环, 每服务器按权重放虚拟节点, 请求按哈希顺时针找最近节点 — 增删节点只影响 1/N 请求。

验证过程:
- **算法注释** (RingHashLoadBalancer.java:60-64): "maps hosts onto a circle (the ring) by hashing its addresses. Each request is routed to a host by hashing some property of the request and finding the nearest corresponding host clockwise... the addition or removal of one host from a set of N hosts will affect only 1/N requests" — **一致性哈希的核心保证**
- **哈希函数**: `XxHash64.INSTANCE` (L71, xxHash64)
- **buildRing** (L323-350): 每服务器 `targetHashes += scale * normalizedWeight` (L339) → **虚拟节点循环** `while (currentHashes < targetHashes) { sb.append(i); hash = hashAsciiString(sb); ring.add(...) }` (L340-349) — **虚拟节点数 ∝ 权重**; "Per GRFC A61 use the first address for the hash" (L337, 取 EAG 第一个地址); 排序 (L350)
- **pickSubchannel** (L416-450): 请求哈希三来源: **CallOptions RPC_HASH_KEY** (xds config selector 生成, L425-428, 缺失 → RPC_HASH_NOT_FOUND L427) / **请求头哈希** (L430-433, requestHashHeader 配置) / **随机** (L435); `getTargetIndex(requestHash)` → **顺时针扫描** (L440: `(targetIndex + i) % ring.size()`)
- **粘性故障处理** (L443-447): "Per gRFC A61, because of sticky-TF with PickFirst's auto reconnect on TF, we ignore all TF subchannels and find the first ring entry in READY, CONNECTING or IDLE" — 跳过 TF 子流 (防抖)
- 默认子策略: LazyLoadBalancer(pickFirstLbProvider) (L73) — MultiChild 基类 (G-4 q5)
- 测试: subchannelLazyConnectUntilPicked (RingHashLoadBalancerTest.java:148)

代码类型: Algorithmic (一致性哈希)

结论: RingHash = **Ketama 变体**: xxHash64 + 权重虚拟节点 (scale 控制密度) + 请求哈希三来源 (配置/头/随机) + 顺时针最近节点 + 粘性故障跳过。**被放弃的方案: 模 N 哈希** — 增删节点导致全部请求重映射; 一致性哈希让影响面 = 1/N (L64 注释)。虚拟节点解决"权重不均 + 少量节点哈希倾斜"。 [跨域: G-4 MultiChild 基类/PickFirst 子策略; G-5 XdsNameResolver RPC_HASH_KEY 生成] [算法: 一致性哈希/xxHash64] (RingHashLoadBalancer.java:60-78,323-350,416-450)
