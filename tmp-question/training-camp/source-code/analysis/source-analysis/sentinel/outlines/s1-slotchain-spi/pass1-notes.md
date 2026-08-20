# S-1 ProcessorSlot 链 + SPI — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-17 | 源码: Sentinel 1.8.9 (slotchain 11 / spi 3 / init 3 / slots/logger 2 / log 14 / config 2)
> 09 域级审计: SENTINEL-PLAN S-1 (🔴 A) — 断言 "10 槽责任链 + 自有 SPI (@Spi order 权威)" 已 grep 验证

## 入口展开 (Level-1~2, 已读源码)

### Level-1: 链构建入口 — DefaultSlotChainBuilder.build()

```
DefaultSlotChainBuilder.build() (DefaultSlotChainBuilder.java L26-38, @Spi(isDefault=true)):
├── new DefaultProcessorSlotChain() (匿名头节点)
└── SpiLoader.of(ProcessorSlot.class).loadInstanceListSorted() (按 @Spi order 排序)
    └── 每槽 addLast → next 单向链表 (AbstractLinkedProcessorSlot.next)
```

### Level-2: SPI 加载器 — SpiLoader (542 行, 自有 SPI 非 JDK)

```
SpiLoader.of(service) (L114): SPI_LOADER_MAP 静态缓存 (ConcurrentHashMap, L79)
load() (L313-430):
├── loaded.compareAndSet(false, true) — 双检锁防重
├── ClassLoader: SentinelConfig.shouldUseContextClassloader() → TCCL / service.getClassLoader() / system (L319-325)
├── classLoader.getResources(SPI_FILE_PREFIX + service.getName()) — 多 jar 合并枚举 (L327)
└── 逐行解析 (L338-390): blank 跳过 / # 注释跳过 / 行内 # 截断
    └── classMap.put + 别名注册 (@Spi alias 多别名) + singletonMap 单例缓存
```

### Level-2: 链的消费方 — SlotChainProvider + CtSph

```
SlotChainProvider.newSlotChain() (58 行):
└── slotChainBuilder volatile 懒加载 (SPI: loadFirstInstanceOrDefault → DefaultSlotChainBuilder 兜底)
    └── 每次调用 build() 新建链 (测试实证: 两次 build 不同实例)

CtSph.lookProcessChain (L194-207):
└── chainMap.get(resourceWrapper) → 双检锁 (LOCK) → newSlotChain() → put
    └── chainMap.size() >= MAX_SLOT_CHAIN_SIZE (6000) → 返回 null (放弃检查!)
```

## 包结构 ↔ 域覆盖矩阵 (S-1 全部 35 文件)

| 包 | 文件 | 角色 |
|---|---|---|
| slotchain/ | 11 (AbstractLinkedProcessorSlot 58/DefaultProcessorSlotChain 83/SlotChainProvider 58/ProcessorSlotChain/ProcessorSlot/ProcessorSlotEntryCallback/ProcessorSlotExitCallback/ResourceWrapper 2/MethodResourceWrapper/StringResourceWrapper/SlotChainBuilder) | 链抽象 + 资源包装 |
| spi/ | 3 (SpiLoader 542/Spi/SpiLoaderException) | 自有 SPI 加载器 |
| init/ | 3 (InitExecutor 104/InitFunc/InitOrder) | 启动初始化 SPI (S-1 附, 生命周期面) |
| slots/logger/ | 2 (LogSlot 56/EagleEyeLogUtil) | 日志槽 (block 日志) |
| log/ | 14 (Logger SPI 族 + jul 实现) | 日志框架 (S-1 附) |
| config/ | 2 (SentinelConfig/SentinelConfigLoader) | 全局配置 (S-1 附) |
| slots/DefaultSlotChainBuilder | 1 (默认 builder) | 链构建 |

## 继承树/调用图

```
ProcessorSlot (接口)
└── AbstractLinkedProcessorSlot<T> (抽象, next 链表)
    ├── entry/fireEntry (transformEntry 泛型转换) / exit/fireExit
    └── 10 个具体槽: NodeSelectorSlot → ClusterBuilderSlot → LogSlot → StatisticSlot
        → AuthoritySlot → SystemSlot → (ParamFlowSlot 扩展) → FlowSlot
        → DefaultCircuitBreakerSlot → DegradeSlot

ProcessorSlotChain (抽象) ← DefaultProcessorSlotChain (匿名头节点 + addFirst/addLast)
SlotChainBuilder (接口) ← DefaultSlotChainBuilder (@Spi isDefault)
  SpiLoader.of(SlotChainBuilder.class).loadFirstInstanceOrDefault()

SpiLoader<S> (final, 542 行):
  SPI_LOADER_MAP 静态缓存 → classList/sortedClassList → classMap(别名) → singletonMap → loaded CAS
```

## 基本元素分解

1. **链抽象 ProcessorSlot + AbstractLinkedProcessorSlot**:next 单向链表;fireEntry→transformEntry→entry 泛型转换 / fireExit→exit 直传 (双向不对称) — AbstractLinkedProcessorSlot.java:21-50
2. **链容器 DefaultProcessorSlotChain**:匿名头节点 (entry 即 fireEntry)+ addFirst/addLast — DefaultProcessorSlotChain.java:31-64
3. **SPI 加载器 SpiLoader**:自有 SPI,类缓存 + order 排序 + 别名 + 单例 + 双检锁 — SpiLoader.java:73-148
4. **@Spi 注解**:order()/isDefault()/alias() — Spi.java:44-49
5. **链工厂 SlotChainProvider**:volatile builder 懒加载 (SPI 解析一次, 链每请求新建) — SlotChainProvider.java:27-52
6. **链缓存 CtSph.chainMap**:按资源缓存链 + MAX_SLOT_CHAIN_SIZE 6000 上限 — CtSph.java:51-54, 194-207
7. **构建器 DefaultSlotChainBuilder**:@Spi(isDefault=true) + loadInstanceListSorted — DefaultSlotChainBuilder.java:26-38
8. **启动初始化 InitExecutor/InitFunc**:@InitOrder 排序初始化 (S-1 附) — InitExecutor.java
9. **日志槽 LogSlot**:fireEntry 后 catch BlockException → EagleEyeLogUtil.log — LogSlot.java:28-45
10. **资源包装 ResourceWrapper**:String/Method 两实现 — equals/hashCode 决定 chainMap key

## 标记问题 (8 个)

1. SpiLoader 排序:order 相同怎么办?比较器有二次 key 吗?— SpiLoader.java:417 (compare 方法体未读全)
2. loadFirstInstance 与 loadFirstInstanceOrDefault 差异:defaultClass 何时参与?— SpiLoader.java:211-244
3. alias 别名机制:如何从别名反查类?classMap key 是什么?— SpiLoader.java:284-312
4. SentinelConfig.shouldUseContextClassloader:什么时候默认 false?配置键是什么?— SentinelConfig.java (csp.sentinel.*)
5. InitExecutor 执行时机:谁调用?与 @InitOrder 的关系?InitFunc 在哪些地方注册?— InitExecutor.java + 全仓库 SPI InitFunc 文件
6. LogSlot 为何放在链第 3 位 (order=-8000)?block 日志只记 StatisticSlot 之后的槽?— 链序语义
7. chainMap 6000 上限:超过后行为是"放弃检查"还是"重建"?— CtSph.java:201-207
8. EagleEyeLogUtil 与 log/ 的关系:三个日志体系 (log 14/eagleeye 15/logger 2) 如何分工?— logger 包 + EagleEyeLogUtil
9. [追加] DefaultProcessorSlotChain.addFirst 谁在用?HotParamSlotChainBuilder 兼容逻辑?— extension 链构建器

## 09 域级审计表 (SENTINEL-PLAN S-1 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "10 槽责任链" | SPI 配置 9 内置 + extension ParamFlowSlot; order 排序实证 (-10000→-1000) | 接受 |
| "执行序权威 = @Spi order" | DefaultSlotChainBuilder.loadInstanceListSorted (L32) + Constants.ORDER_*_SLOT (L76-84) | 接受 |
| "SlotChainProvider 缓存 SPI builder" | volatile slotChainBuilder 懒加载 (L33-37) | 接受 |
| "CtSph.chainMap 按资源缓存" | chainMap.get + 双检锁 + MAX_SLOT_CHAIN_SIZE (L194-207) | 接受 |
| "SpiLoader 542 行" | wc -l = 542 | 接受 |
| "InitExecutor 104 行" | wc -l = 104 | 接受 |
| "SPI 配置只定加载集合" | load() 逐行解析 (L338+) | 接受 |

## 待展开 (下一层)

1. SpiLoader 比较器二次排序 key (order 相同)
2. alias 别名注册细节 (classMap key 格式)
3. InitExecutor 触发链 (谁在启动时调用)
4. LogSlot 的 EagleEyeLogUtil 完整机制
5. HotParamSlotChainBuilder (extension 兼容层) 与 DefaultSlotChainBuilder 关系