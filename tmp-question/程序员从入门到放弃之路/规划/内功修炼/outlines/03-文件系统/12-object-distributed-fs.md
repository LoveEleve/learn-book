# 12 — 对象存储与分布式文件系统: 从 S3 到 CephFS 到 GlusterFS

> Cluster B: 3 KPs | 依赖: 11-nfs-network-fs | 读者基线: 理解 NFS 单点模型与 FUSE 用户态模式

---

### 1. 对象存储 — S3 API 的无目录世界
  - S3 模型: `bucket → object_key → { metadata: Content-Type, ETag(md5), Last-Modified, x-amz-* }, data}` (HTTP REST API)
  - 操作: GET/PUT/DELETE/HEAD — 无需目录层次, 键值模型
  - Versioning: `?versionId` 参数 — 多版本共存
  - 优势: 无 fopen/fwrite 概念, 无碎片, 无 rename 竞争, 无 inode 耗尽 — 原子 PUT 要么全写要么没有

### 2. Haystack — Facebook 的 append-only log 照片存储
  - 写入: `write(needle_id, data)` → append-only log — 无索引开销
  - `needle_index`: hash(needle_id) → log offset — 内存常驻, 读时 O(1) 定位 (`src/backend/Directory.h → needle_index`)
  - 读取: `read(needle_id) → read_index → seek_log(lookup_offset) → read data`
  - Compact: GC 标记 dead needle → 写新 log → 迁移 live needle → 回收旧 log (`src/backend/Haystack.cc → compact_log`)
  - 设计哲学: 无目录树(无 dentry→inode 解析链), 只写一次多读(无需修改), 无碎片(无块分配器)

### 3. CephFS — CRUSH 算法无中心元数据定位
  - CRUSH: `hash(object_name) → PG → OSD` — O(1) 定位每个 object 的物理位置, 无需中心 MDS 查询 (`src/crush/CrushWrapper.h → CrushWrapper`)
  - RADOS 层: pool → PG(Placement Group) → OSD(`osd_stat_t`) — 对象最终存储在 OSD (`src/osd/OSD.h → OSDService`)
  - `ceph mds stat` — MDS 集群多活, 负责目录树元数据, 不负责数据定位 (`src/mds/MDSRank.h → MDSRank`)
  - `ceph df` — 集群容量, 不同于传统 `df` 查 SuperBlock 空闲计数 (`src/mon/Monitor.cc → Monitor`)
  - 一致性: `sync_object(oid, pguid)` — 写操作只发给 primary OSD → OSD 间 PG log 同步副本 (`src/osd/PG.h → PG`)

### 4. GlusterFS — DHT 对等架构无 MDS
  - DHT: hash(dir) → brick — elastic hash 算法, 无中心 MDS (`xlators/cluster/dht/src/dht-common.c → dht_lookup`)
  - `gluster volume create test-volume replica 2 server{1,2}:/brick` — 创建卷
  - `mount -t glusterfs server1:/test-volume /mnt` — FUSE 挂载
  - 无单点: 每个 brick 独立, 客户端承担 hash 计算, 数据定位无 RPC
  - 对比 CephFS: GlusterFS 纯对等无中心, CephFS 有 MDS(目录元数据)+OSD(数据)分层

### 5. 收束
  - 对象存储(S3/Haystack)放弃 POSIX 目录树, 换取极致扩展性 — append-only log 消除碎片与分配器
  - CephFS CRUSH 用确定性哈希替代中心索引, GlusterFS DHT 用客户端哈希替代 MDS — 两者的共同目标: 消除单点瓶颈
  - 至此, 从本地 ext2 磁盘布局到分布式哈希定位, 文件系统全谱系覆盖完成

---

### 核心悬念
**"从 ext2 的 block bitmap 到 CephFS 的 CRUSH 哈希, 都面临同一个问题: 如何把'从名到地址'的映射做得无限可扩展？本域覆盖了本地→网络→分布式三层答案。"**

→ 引出 04-网络 — TCP/IP 协议栈到容器网络的数据流动全景
