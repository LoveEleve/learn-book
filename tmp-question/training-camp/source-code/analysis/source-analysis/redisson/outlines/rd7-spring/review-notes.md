# RD-7 Spring 集成矩阵 — 深审 REVIEW 记录 (六层深审 + 07 五维度 + 二次 REVIEW)

## 二次 REVIEW (07 换维度: 反写/事实/锚点/跨域/完整)

| 轮 | 维度 | 发现 | 修复 |
|:--:|:--|:--|:--|
| R1 | 反写覆盖 | 6 闭环全落入 3 篇大纲 ✅ | 通过 |
| R2 | **事实核验 (版本对应)** | data-26→SD Redis 2.6.10 / data-30→3.0.12 / data-40→4.0.5 — 版本矩阵表述精确 ✅; 补 execute 反射失败处理 (PipelineException/InvalidDataAccessApiUsageException) | 篇3 补 |
| R3 | **锚点密度 (第七次复发)** | 裸行号 15 处; file:line 01=1/02=5/03=2 (01 严重不足) | **全补 → 01=4/02=14/03=16** ✅ |
| R4 | 跨域核验 | 全部引用目标存在; r24-string 留待 RD-8 补链 | 通过 |
| R5 | 完整性 | REDISSON-PLAN RD-7 主题 (四模块取代) 全覆盖 | 通过 |

> ⚠️ 铁律七连复发记录: 裸行号 RD-1/3/4/2/5/6/7。已列入完成检查单强制项。RD-8 开写前先 grep 自扫。

## 六层深审 (Pass 0-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **重点机制 (starter 取代链)** | Client→ConnectionFactory→RedisTemplate; 条件+时序双保险 | 篇1 |
| 2 | **Cache 包装** | RedissonCache 包装 RMapCache | 篇2-S1 |
| 3 | **getCache 双路** | config.ttl>0 → RMapCache else RMap | 篇2-S2 |
| 4 | **data 适配** | RedisConnectionFactory+Reactive; 操作集映射 | 篇3-S1 |
| 5 | **Transaction 模板** | AbstractPlatformTransactionManager 三模板 | 篇3-S2 |
| 6 | **版本矩阵** | 18 子模块编译期隔离 | 篇3-S3 |
| 7 | completeness ⚠️ 回填 | Q23 (configLocation YAML)/Q24 (Micrometer 计数) | 篇2 |
| 8 | 通过项 | 全锚点 awk 验证 ✅ | 记录 |

## 07 五维度

### 维度1 功能正确性
- starter 取代链; Cache 包装; data 适配; tx 模板 (四模块实证)

### 维度2 性能
- fastPut 命令; 反射分发; 编译期版本隔离

### 维度3 内存
- configMap/CacheConfig 轻量

### 维度4 一致性/并发
- 用户 Bean 优先; Cache 独立 TTL; 事务模板线程绑定

### 维度5 边界/安全
- @ConditionalOnMissingBean 防覆盖; 版本错配 NoSuchMethodError

## 完成状态

- [x] Pass 0-3 (方案 B): 6 闭环 + 3 篇 + 50 问
- [x] **二次 REVIEW (R1-R5) 修复 2 项** (execute 失败处理/锚点 15 处)
- [x] 铁律七连复发记录