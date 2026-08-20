# N-18 配置审计/监控 — 历史清理、内存监控与加密面

> 前置: [[N-15-配置存储]] (历史/缓存消费) | 对照: 配置面的可观测与治理
> 🟡 B | 方案 B (重要域) | 闭环: q1(历史面) q2(监控面) q3(治理面)

**读者处境**: 配置历史的清理? 内存监控? 加密过滤器?

### 1. 历史面 — HistoryService 与清理器

场景: 配置历史怎么管理与清理?
源码路径:
- **HistoryService** (service/HistoryService.java:45): 历史查询/记录
- **DefaultHistoryConfigCleaner** (dump/DefaultHistoryConfigCleaner.java:35): 历史清理
- **HistoryConfigCleanerConfig** (dump/HistoryConfigCleanerConfig.java:27): 清理配置
- model/ConfigHistoryInfo 族: 历史模型
关键设计 (q1): **"历史 = 有界留存"** — 查询与清理分离; 清理配置可调。 [模式: 有界历史]

### 2. 监控面 — monitor 七件

场景: 配置内存/响应怎么监控?
源码路径:
- **MetricsMonitor** (monitor/MetricsMonitor.java:28): 配置指标 (NC-7 三处之一)
- **MemoryMonitor** (monitor/MemoryMonitor.java:33): 内存监控
- **PrintMemoryTask/PrintGetConfigResponeTask** (monitor/): 定期打印任务
- **ResponseMonitor** (monitor/ResponseMonitor.java:27): 响应监控
- **ThreadTaskQueueMonitorTask** (monitor/ThreadTaskQueueMonitorTask.java:29): 线程队列监控
- **ConfigDynamicMeterRefreshService** (monitor/ConfigDynamicMeterRefreshService.java:36): 动态指标
- **collector/** (monitor/collector): 采集器
关键设计 (q2): **"监控七件 = 内存/响应/队列/动态"** — 定期打印 + 动态指标双通道。 [模式: 多维监控]

### 3. 加密面 — 配置加密过滤器

场景: 配置加密怎么落服务端?
源码路径:
- config 加密: ConfigEncryptionFilter (config/server/filter/) — NC-2 客户端加密链的服务端对面
- utils/ 加密工具 (31 文件)
- 与 NC-2 LocalEncryptedDataKeyProcessor 两端闭合
关键设计 (q3): **"加密切面 = 两端闭合"** — 服务端加密过滤与客户端解密链配对。 [模式: 端到端加密]

### 4. 测试与行为锚

场景: 监控边界?
源码路径:
- 测试: HistoryServiceTest (config test)
- 锚: PrintMemoryTask 周期
关键设计 (q1): **"定期打印 = 无监控系统兜底"**。 [模式: 日志兜底监控]
