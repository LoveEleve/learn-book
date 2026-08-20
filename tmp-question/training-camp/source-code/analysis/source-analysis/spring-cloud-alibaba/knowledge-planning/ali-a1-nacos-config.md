# ALI-A1 Nacos Config 配置加载 — 知识规划 (KP)

> 🔴 A | 模块: spring-alibaba-nacos-config (48) + spring-cloud-starter-alibaba-nacos-config (10) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | ConfigData 新轨 | NacosConfigDataLocationResolver:57/62/75 | `nacos:` 前缀 → URI 解析 → Resource (order=-1) |
| 2 | Bootstrap 旧轨 | NacosPropertySourceLocator:74 | PropertySourceLocator 实现, locate 三级递进 |
| 3 | 三级递进优先级 | NacosPropertySourceLocator:112-122 | 默认 → .后缀 → -profile (addFirst 反转) |
| 4 | 单例管理器 | NacosConfigManager:49-60 | 双检锁 + static ConfigService |
| 5 | 快照容灾 | NacosSnapshotConfigManager:47-52 | 读后即删, MAX 100 |
| 6 | 源仓库 | NacosPropertySourceRepository:66-71 | dataId+group 复合 key putIfAbsent |
| 7 | 刷新节流 | NacosPropertySourceLocator:164-169 | refreshCount!=0 && !refreshable → 复用 |
| 8 | URI 五元组 | NacosConfigDataResource:103-108 | group/dataId/suffix/refreshEnabled/preference |
| 9 | 解析链 | NacosDataParserHandler + NacosJsonPropertySourceLoader | 按扩展名分派 yaml/json/xml/properties |
| 10 | 缺失检查 | NacosConfigDataMissingEnvironmentPostProcessor | import 缺失大声报错, bootstrap 跳过 |

## 02 高频坑

1. 双模块同名类 (NacosConfigBootstrapConfiguration 在 core 和 starter 各一个) — 装配内容不同
2. dataId 在 URI 中只能一段, 多段抛 IllegalArgumentException
3. 空配置不占位 (addFirstPropertySource ignoreEmpty)
4. 快照单次消费 (getAndRemove) — 不是常驻缓存
5. 静态单例 + 静态 ConfigService — 进程内唯一, 测试注意状态污染
6. sharedConfigs/extensionConfigs 条目默认 refresh=false
7. preference=REMOTE 才加 PROFILE_SPECIFIC (默认 LOCAL)

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| SPI 插槽 | PropertySourceLocator (SCC-1) / ConfigDataLocationResolver / ConfigDataLoader |
| 优先级 | addFirst 反转 (locator) / PROFILE_SPECIFIC (loader) / ORDER=-1 (resolver) |
| 并发 | 双检锁 (Manager) / ConcurrentHashMap (Repository/Snapshot) |
| 容灾 | 内存快照 (自建) / 客户端磁盘快照 (nacos) / emptyList 静默 |
| 兼容 | @DeprecatedConfigurationProperty "use spring.config.import instead" / 双构造器 |
| 装配面 | AutoConfiguration.imports / spring.factories / services 三机制 |

## 04 跨域桥接

- → ALI-A2: 刷新节流条件是 A2 的入口 (refreshCount 是谁维护的)
- → SCC-1: NacosPropertySourceLocator 是 Commons PropertySourceLocator 的消费者实证
- → Nacos 5.8: 底层 configService 客户端面 (长轮询)
- → 面试: "Nacos 配置怎么加载/优先级怎么定/挂了怎么办" 三连
