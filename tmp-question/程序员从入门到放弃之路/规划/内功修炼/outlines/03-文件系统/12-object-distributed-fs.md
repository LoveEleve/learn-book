# 对象存储与分布式文件系统 — 从 S3 的 key 到 Ceph/Gluster 的多机寻址

> Cluster B: 3 KPs | 依赖: 11-nfs-network-fs | 读者基线: 理解 NFS 单点远端树与 FUSE 用户态模式
> 读者处境: 11 篇讲的是"一棵远端文件树如何投影到本地 VFS"；本篇把规模继续推大：当数据横跨几十台机器时，为什么有人干脆放弃 inode/目录树，改用 key、log、hash 和对象池
> 打开新视角: 分布式存储的核心问题仍是"名字到地址"，只是地址不再是 block number，而是 object key → placement → storage node；不同系统选择了不同的元数据与数据分层

---

### 概念依赖链

```
11 NFS/FUSE(远端树与用户态桥) → 本篇: 对象存储与分布式寻址
  ├─ §1 S3(key/value, 无 POSIX 目录语义)
  ├─ §2 Haystack(append-only log, 专用索引)
  ├─ §3 CephFS/RADOS(MDS 管名字, CRUSH/PG 管数据位置)
  └─ §4 GlusterFS(DHT/brick 对等分布)
先讲: 放弃目录树 → 专用 log → 元数据/数据分层 → 对等 hash
后续依赖: 04-网络(TCP/IP 与容器网络)
```

### 叙事顺序

1. 问题引入——如果文件数量从几百万增长到几十亿，仍然为每个对象维护 inode、目录项和中心索引，会发生什么？（**Aha: 分布式系统不是把 ext2 的 bitmap 放大，而是重新定义"文件名、目录、地址"**）
   - 过渡: 最彻底的第一步——连目录都不要了
2. S3——bucket + object key 的无目录模型
   - 过渡: 如果对象写入模式是"一次写、多次读"，连通用文件系统都可以不要
3. Haystack——append-only log + 内存索引
   - 过渡: 如果还要保留 POSIX 目录语义，数据位置和目录元数据能不能拆开？
4. CephFS/RADOS——MDS 管命名，CRUSH 管数据放置
   - 过渡: 还有一种路线连中心 MDS 都不要——客户端自己算 brick
5. GlusterFS——DHT + brick 对等架构
   - 过渡: 收束
6. 收束——四种系统对同一问题的不同答案

### 1. S3 — bucket + object key 的无目录模型

场景提示: 你要保存一张用户头像，在 S3 里并没有创建目录、分配 inode、更新父目录目录项这些步骤——对象到底由什么标识？ [写作时展开]

关键设计: S3 把文件系统的"路径"改成 bucket 内的 object key，把存储接口改成 HTTP API：

```[pseudocode]
PUT /bucket/user-123/avatar.png
  key = "user-123/avatar.png"   // 只是一个字符串, 不要求真实目录存在
  metadata = Content-Type / Last-Modified / user metadata
  body = object bytes

GET /bucket/user-123/avatar.png
HEAD /bucket/user-123/avatar.png
DELETE /bucket/user-123/avatar.png
```

Why: 为什么对象存储可以没有真正的目录树？——**因为它把寻址主键从"父目录 inode + 子目录项"换成了一个全局可路由的 key**：服务端可以按 key 做分片、复制和生命周期管理；客户端也不再依赖 `rename`、硬链接、目录锁这些 POSIX 语义。**注意：key 中的 `/` 通常只是命名约定/列表前缀，不等于 ext2 那种真实目录层级；ETag 也不应简单等同于 MD5，具体语义取决于上传方式和服务实现。** [man 7 http: HTTP 方法与资源语义；S3 API 的持久化边界由服务端响应/版本控制定义]

比喻锚点: S3 像超大型快递柜——不按城市街道维护门牌树，只给每个包裹一个全局取件码（object key）；前缀看起来像目录，但本质仍是取件码的一部分。 [写作时展开]

### 2. Haystack — 专用照片存储的 append-only log

场景提示: 社交图片服务里，照片通常"写入一次、读取很多次"；如果每张照片都走完整 inode/目录/bitmap 分配，元数据和随机写开销是否值得？ [写作时展开]

关键设计: Haystack 这类专用 blob 存储用 append-only log 换取简单写入，再用专用索引反查偏移：

```[pseudocode]
put(needle_id, data)
  → append(log, header + needle_id + data)
  → index[needle_id] = { log_id, offset, size }

get(needle_id)
  → index[needle_id]
  → seek(log_id, offset)
  → read(size)

delete/update
  → 标记旧 record 不再活跃
  → 后台 compact: 搬迁 live record → 重写新 log → 回收旧 log
```

Why: 为什么照片场景适合 append-only，而不是通用文件系统的原地更新？——**写路径顺序化后，磁盘布局和崩溃恢复都更简单**：新对象只追加，不需要在全盘寻找空洞；读取靠内存索引一次定位；更新则用新 record 覆盖逻辑版本，旧 record 交给 compact 回收。**代价**是内存索引、GC/compact 和故障恢复成为系统自己的责任。 [内核: 它放弃 01-06 篇的 inode/目录项/bitmap 三层账本, 改用 log offset + 专用 index]

比喻锚点: Haystack 像仓库流水账——新货永远写在账本末尾，查询靠一本内存目录定位页码；旧货不现场擦除，而是在夜间盘点时集中整理。 [写作时展开]

### 3. CephFS / RADOS — MDS 管名字，CRUSH 管数据位置

场景提示: 既要保留 POSIX 目录、权限和 rename，又要让文件内容扩散到大量 OSD，单一元数据服务器和单一数据索引会不会成为瓶颈？ [写作时展开]

关键设计: Ceph 把"命名空间"和"对象放置"拆成两层：CephFS 的 MDS 负责文件系统元数据，RADOS 负责对象数据，CRUSH 根据对象名和集群 map 计算 PG/OSD：

```[pseudocode]
CephFS: pathname / inode / directory / permission
  → MDS 集群维护元数据

file data
  → 切成 RADOS objects
  → CRUSH(object_name, cluster_map)
  → PG
  → primary OSD + replica/EC chunks

client
  → 先向 MDS 获取布局/权限信息
  → 数据面直接与相应 OSD 交互(按配置和协议)
```

Why: 为什么 CephFS 不能简单说成"CRUSH 取代 MDS"？——**CRUSH 只解决数据对象的确定性放置，不负责目录树、权限、rename 等 POSIX 元数据**：MDS 仍然存在，只是它不需要为每个数据块维护一个中心位置索引；RADOS/CRUSH 把数据面扩展出去，MDS 专注命名空间。**这是"元数据中心化程度降低"，不是"元数据中心完全消失"。** [内核: 11 篇 NFS 也把名字/属性与数据 RPC 分开；Ceph 把这种分层进一步扩展成 MDS + OSD]

比喻锚点: Ceph 像物流系统：MDS 是总目录办公室，告诉你包裹属于哪个订单、有没有权限；CRUSH 是分拣规则，拿着包裹编号和当前仓库地图，直接算出应该去哪个仓库。 [写作时展开]

### 4. GlusterFS — DHT + brick 的对等分布

场景提示: 如果连 CephFS 的 MDS 都不想维护，能不能让客户端根据路径/文件名自己算应该访问哪个 brick？ [写作时展开]

关键设计: GlusterFS 用 translator stack 和 DHT 把目录/文件分布到 bricks；它不是“没有任何元数据”，而是把定位逻辑更多放到客户端和分布式卷配置中：

```[pseudocode]
client path lookup
  → DHT translator 根据 path hash / layout 选择 brick
  → 向目标 brick 发起 lookup/read/write

replica volume
  → 同一文件/brick placement 按副本策略写多个 brick
  → 故障时由副本与 self-heal 机制恢复可见性

mount
  → FUSE 客户端连接 Gluster volume
  → VFS 请求进入 translator stack
```

Why: 为什么 GlusterFS 能减少中心 MDS 瓶颈？——**因为客户端可以依据分布布局直接判断目标 brick**；但这不等于“完全无 RPC”或“没有一致性成本”：首次发现卷配置、目录布局变化、复制、自愈、rename 等仍需要协议协作。**它用客户端计算和对等 brick 换取了不同的运维与一致性复杂度。**

比喻锚点: GlusterFS 像每个快递员都拿着同一份分区地图——不必每次问中央调度台，但地图变化、丢件补发、多人同时改地址时，快递员之间仍要同步。 [写作时展开]

### 5. 收束

回到"如何把名字映射到可扩展地址"：
- S3：bucket + key，放弃 POSIX 目录语义，换取 API 与水平扩展
- Haystack：needle → log offset，针对一次写多次读优化
- CephFS/RADOS：MDS 管命名，CRUSH/PG/OSD 管数据放置
- GlusterFS：DHT + brick，把更多定位工作交给客户端与对等节点

四种模型的共同变化：

```[pseudocode]
ext2: inode/dirent → block bitmap → physical block
NFS:  path → remote file handle → RPC server
S3:  bucket + object key → object placement
Ceph: pathname(MDS) + object name(CRUSH) → PG/OSD
Gluster: path/layout hash → brick
```

**Aha Moment**: "从 ext2 的 block bitmap 到 Ceph 的 CRUSH，它们其实一直在回答同一个问题：**给定一个名字，怎样不扫描全世界就找到数据**。区别只是寻址单位从 inode/block，升级成 key/object/PG/OSD；规模越大，越必须把目录语义、数据放置、复制与一致性拆成不同层。"
**回答读者三问**: ①对象存储为什么不需要 inode=主键变成 bucket+key；②Ceph 为什么既有 MDS 又有 CRUSH=MDS 管名字，CRUSH 管数据放置；③Gluster 为什么不等于零中心=减少中心索引，但仍有布局、复制和自愈协作。

---

### 核心悬念

**"从 ext2 的物理块到 Ceph 的 PG/OSD，数据最终都要跨网络移动——04-网络篇会从 TCP/IP、拥塞控制、epoll 一路讲到容器网络：一次对象读请求，究竟如何穿过协议栈抵达正确机器？"**

→ 引出 04-网络 — TCP/IP 协议栈到容器网络——文件系统阶段收束，下一阶段进入网络数据流。