# ALI-A1 Nacos Config 配置加载 — 时空溯源 (2025.0.0.0 实证)

> 仓库为浅克隆单提交 (d9d9be6a, 2025-10-17) — 演进证据取自代码内注释 (@since/issue 号/@Deprecated 语义/构造器弃用)

## 演化主线: 一个 SPI 插槽, 三个时代的叠加

| 时代 | 机制 | 证据 (2025.0.0.0 源码) | 演进信号 |
|:--|:--|:--|:--|
| 旧 | bootstrap + PropertySourceLocator | NacosPropertySourceLocator (@Order(0), locate 单方法) + spring.factories BootstrapConfiguration | 构造器 @Deprecated (L64-67): 推荐 NacosConfigManager 变体 — **旧构造器淘汰中** |
| 中 | 常轨 AutoConfiguration | AutoConfiguration.imports → NacosConfigSpringCloudAutoConfiguration 仍装配 NacosPropertySourceLocator (L50) | 与旧轨并存 |
| 新 | ConfigData (Boot 2.4+) | NacosConfigDataLocationResolver/DataLoader (services SPI) | **@since 2021.0.1.0** (L55/56) — 引入版本锚 |
| 属性面 | shared/extension → spring.config.import | NacosConfigProperties:427/501/502 @DeprecatedConfigurationProperty(reason = "use spring.config.import instead") | **"use spring.config.import instead" = 官宣迁移路径** |

## 关键事件锚

- **issue#2455** (NacosConfigDataLoader.java:110): REMOTE preference → Option.PROFILE_SPECIFIC — 远程配置覆盖本地同 key 的修复
- **issue#2906** (NacosConfigDataLoader.java:145): parseNacosData configName 改为 `group@dataId` 格式 — 解析上下文修复
- **快照机制**: NacosSnapshotConfigManager @author ruansheng @date **2024-01-22** (L27-28) — 2024 年引入的内存快照容灾层 (比对: Nacos 5.8 的磁盘快照 LocalSnapshot 是另一层)
- **ConfigService 静态化**: NacosConfigManager 注释 "Compatible with old design,It will be perfected in the future" (L62-64) — 静态单例是历史包袱的显式承认

## 三层容灾叠加 (时间上分层)

1. **进程内存快照** (2024, NacosSnapshotConfigManager) — 刷新区间插队读
2. **Nacos 客户端磁盘快照** (nacos-client 内部, 5.8 域) — 服务端不可达兜底
3. **emptyList 静默降级** (Builder L96-101) — 解析失败不阻塞启动
→ 演进方向: 容灾从"客户端自带"走向"集成层自建", 每层解决一个时代的问题。
