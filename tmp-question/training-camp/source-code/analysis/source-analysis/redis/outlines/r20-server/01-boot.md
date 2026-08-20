# R-20 上 — 启动与初始化: 从 argv 到就绪的事件循环

> 前置: [[R-33-zmalloc]] (OOM 装配) + [[R-3-Dict]] (seed) + [[R-1-object]] (共享池) | 引出: [[R-20-下]] (周期引擎) | 对照: [[R-14-sentinel]] (模式分支)
> 🔴 A | 4 KP | [模式: 依赖序编排+初始化矩阵+宏 DSL 配置]
> Pass 2 闭环: q1(main 管线) q2(initServer) q7(配置族) q8(生产面)

**读者处境**: 执行 redis-server 之后到监听端口, 中间发生了什么?为什么哨兵要先于配置文件解析?server.db 为什么是"分片"的?CONFIG SET 失败怎么保证不半途而废?这篇拆 Redis 的启动: 依赖序编排、初始化矩阵、配置宏 DSL、生产就绪面。

### 1. main 启动管线 — 依赖序的编排

场景: 什么必须最先?哨兵为什么特殊?
源码路径:
- `server.c:6917+` (main): 测试模式 (L6922) → **OOM handler 装配 (L6970, R-33)** → **哈希 seed (L6985-6987, R-3)** → **哨兵先于配置** (L7006-7012, 注释: 配置解析会填充哨兵结构) → check-rdb/aof 模式 (L7014-7021) → 配置加载 → initServer → 就绪 → aeMain (L7251)
关键设计: 依赖序 (q1): 内存层 → 安全层 → 模式分支 → 配置 → 服务初始化 — 每步为下一步铺路; 哨兵必须先建结构再读配置 (配置即填充)。[模式: 依赖序编排]
数据流: argv → 模式判定 → 配置 → initServer → 就绪 → aeMain。

### 2. initServer 矩阵 — 信号/事件/键空间

场景: 服务器初始化都装了什么?键空间为什么分片?
源码路径:
- `server.c:2591+` (initServer): 信号 (SIGHUP/SIGPIPE 忽略 + 优雅关闭 L2593-2596) → 线程面 → 状态初始化 → 共享对象 (L2653) → **aeCreateEventLoop (L2657)** → **键空间 kvstore 分片 (L2665-2680: cluster 模式 14bit 槽分片 + hexpires)** → list 族 → cron 注册 (L2757)
关键设计: 初始化矩阵 (q2): 信号/事件/数据面/客户端面全装配; **7.x 键空间从单 dict → kvstore 分片** (cluster 16384 槽分 14bit) — R-21 详述。[模式: 全量装配 + 分片化]
数据流: 配置生效 → 组件装配 → 事件循环建立 → 就绪。

### 3. 配置系统 — 宏 DSL 统一注册

场景: 600+ 配置项怎么管理?CONFIG SET 失败怎么办?
源码路径:
- `config.c:2244+` — createIntConfig 宏族: 展开为 standardConfig (类型/范围/默认/验证/apply)
- 配置族统计: 53 Bool + 41 Int + 36 String + 20 Enum + 13 SizeT + 9 Special
- 样例: databases=16 (L3147) / port → updatePort (L3148) / io-threads (L3149)
- **CONFIG SET 失败回滚** (L760-780): restoreBackupConfig — 备份→设置→apply→失败全恢复
关键设计: 宏 DSL (q7): 五项合一注册 (类型/范围/默认/验证/apply) — 启动解析与 CONFIG SET 共用一套; 失败整体回滚保一致性。[模式: 声明式配置 + 事务回滚]
数据流: redis.conf → 解析 → standardConfig 表 → 运行时 CONFIG SET 动态。

### 4. 生产就绪面 — systemd/OOM/亲和

场景: READY 通知谁发?卡死怎么发现?
源码路径:
- 就绪通知 (L7230-7240): `redisCommunicateSystemd("READY=1")` — systemd 监督
- CPU 亲和 (L7250) / **OOM score (L7251: setOOMScoreAdj(-1))** — OOM killer 协作
- **watchdog** (L1281): cron 缺席 → SIGALRM → 栈 dump — 卡死检测
关键设计: 运维契约 (q8): 就绪协议 + 资源策略 + 卡死检测 — 服务可用性交给外部体系。[模式: 运维集成]
数据流: 就绪 → 外部健康检查 → 运行期 watchdog 兜底。

### 负面空间 — 启动面刻意不做的事

- **不做并行初始化**: 全串行 (单线程启动, 无并发装配)
- **不做配置热加载**: 修改配置文件不自动生效 (CONFIG SET 才动态)
- **不做优雅降级**: 初始化失败即退出 (无"部分可用"模式)
- **不做启动自检全集**: 只在加载数据时校验 (RDB/AOF)

→ 引出: 周期引擎与命令表 — serverCron 时间分级 + 122 命令注册 → [[R-20-下]]
