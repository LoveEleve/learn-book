# 闭环笔记 q1: Redisson 初始化链 — 构造器五步 + register 注册表

## 假设
Redisson 构造器按"配置复制 → 连接管理器 → 命令执行器 → 附属服务 → 注册续期器"顺序组装, 附属服务 (EvictionScheduler/WriteBehindService/LockRenewalScheduler) 不属于 ServiceManager, 由 Redisson 构造器显式创建并经 register() 注册。

## 验证过程
- Redisson.java:66-86 (构造器): `Config configCopy = new Config(config)` → `ConnectionManager.create(configCopy)` → `createCommandExecutor(objectBuilder)` → `new EvictionScheduler(commandExecutor)` + `new WriteBehindService(commandExecutor)` → `connectionManager.getServiceManager().register(new LockRenewalScheduler(commandExecutor))`
- ServiceManager.java:766: `public void register(LockRenewalScheduler renewalScheduler)` — 字段 `renewalScheduler` (L156) 初值 null, 由 register 注入
- 为什么在构造器不在 ServiceManager: EvictionScheduler/WriteBehindService 需要 CommandAsyncExecutor (命令层产物); LockRenewalScheduler 需要 commandExecutor — 三者都依赖"连接管理器已就绪"后的产物, 而 ServiceManager 在连接管理器内部先创建 (ConnectionManager 构造中)
- Config copy 构造默认 Kryo5Codec (Config.java:165 — oldConf.getCodec()==null 时)
- RedissonObjectBuilder: referenceEnabled 才创建 (L72-74)

## 代码类型
Glue (装配) — 但 register() 是服务注册表模式 (Implementation)

## 跨域关联
- RD-4 (CommandAsyncExecutor) → 创建点在此链
- RD-2 (LockRenewalScheduler) → register 注入
- RD-5 (EvictionScheduler/WriteBehindService) → 构造期创建

## 结论
初始化链 = 五步固定序: configCopy → ConnectionManager.create → createCommandExecutor → EvictionScheduler/WriteBehindService → register(LockRenewalScheduler)。附属服务依赖命令层产物, 故不能在 ServiceManager 内创建; register() 是"服务注册表"模式 (ServiceManager 仅持有引用, 生命周期由 Redisson 控制)。
源码位置: Redisson.java:66-86, ServiceManager.java:156,766, Config.java:165

## 跨域发现

来源: RD-1 第二遍, 闭环笔记 q1
发现: s75-boot-redis 大纲写"连接/协议深入在阶段3" —— RD-1 篇1 (初始化链) 就是该承诺的**承接点**: s75 讲 Boot 装配 RedisTemplate 的条件选择 (Lettuce/Jedis), RD-1 讲 Redisson 的 ConnectionManager 替换 ConnectionFactory 后连接如何建立。两域形成真正的反向双链。
已对照验证: spring/outlines/s75-boot-redis/01-redis.md header "依赖 S-5 ... 连接/协议深入在阶段3, 本域只讲接线"; redisson-spring/redisson-spring-boot-starter/.../RedissonAutoConfigurationV4.java:31-33 (@AutoConfiguration(before=DataRedisAutoConfiguration))
传播操作: 第二级 — RD-7 大纲需显式承接此点 (见 REDISSON-PLAN §五 反向承接表)
