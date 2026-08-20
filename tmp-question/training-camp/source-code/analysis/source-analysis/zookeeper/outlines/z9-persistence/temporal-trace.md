# Z-9 持久化 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.x | 双文件骨架定型: FileTxnLog 格式 Javadoc (L60-96 注释锚) + FileSnap + FileTxnSnapLog restore 双路径; forceSync 默认 yes; snapCount 100000 |
| 3.5.x | ZOOKEEPER-1161 (datadir autocreate) + ZOOKEEPER-2325 (空库初始化) + ZOOKEEPER-2967 (目录交叉污染检查) + ZOOKEEPER-3056 (3.4 升级 trustEmptySnapshot) — 启动边界面补全 |
| 3.6.x | **快照压缩**: SnapStream StreamMode GZIP/SNAPPY (ZOOKEEPER_SHAPSHOT_STREAM_MODE); AtomicFileOutputStream 原子写; zxidDigest seal 段 (digest 特性 ZOOKEEPER-2305 面) |
| 3.9.x | **txnLogSizeLimitInKb** (FileTxnLog:122-147, 尺寸滚动); ZOOKEEPER-3781 (升级后强制首快照 shouldForceWriteInitialSnapshotAfterLeaderElection L237-239); lastProcessedZxid seal 段 |

## 痕迹证据

- FileTxnLog.java:60-96: Javadoc 文件格式全图 (3.4 锚)
- FileTxnSnapLog.java:119: "See ZOOKEEPER-1161" — datadir autocreate (3.5 锚)
- FileTxnSnapLog.java:163: "See ZOOKEEPER-2967" — 目录交叉污染 (3.5 锚)
- FileTxnSnapLog.java:285: "reported in ZOOKEEPER-2325" — 空数据库 (3.5 锚)
- FileTxnSnapLog.java:287: "ZOOKEEPER-3056 ... escape hatch ... upgrading from old versions (3.4.x, pre 3.5.3)" — trustEmptySnapshot (3.5 锚)
- FileTxnSnapLog.java:234: "to address ZOOKEEPER-3781 after upgrading from Zookeeper 3.4.x" — 强制首快照 (3.6+ 锚)
- FileTxnSnapLog.java:445-453: fuzzy snapshot 容错注释 (3.4 锚)
- FileTxnLog.java:122: txnLogSizeLimitInKb (3.9 锚, 默认 -1 实证)

## 推断标注

- "3.4.x 双文件骨架" — 公知版本线 (ZK 3.4 存储格式定型) (标注)
- "3.6.x 压缩/原子写" — 特性年代推断 (SnapStream/AtomicFileOutputStream) (标注)
- "3.9.x 尺寸滚动" — txnLogSizeLimitInKb 实证 (标注)
- git shallow (1 commit "Prepared 3.9.5") — 无考古, 全注释锚 + JIRA 编号

## 对照线 (阶段3 已交付)

- ES Translog: ZK txnlog ≈ ES translog (WAL); ES 有 translog 压缩/异步刷盘, ZK 无
- Redis AOF: ZK snapCount 随机快照 ≈ Redis AOF rewrite; Redis fsync 三档 (always/everysec/no), ZK 只 forceSync 布尔
