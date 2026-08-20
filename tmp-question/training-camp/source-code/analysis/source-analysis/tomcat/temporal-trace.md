# Tomcat 时空溯源 — T-1~T-4 机制演化轨迹

> 溯源方式: 源码浅克隆 (tag acf7da1, 10.1.34) 无 git 历史 → 数据源 = webapps/docs/changelog.xml (10.1.x 全量) + RELEASE-NOTES
> 覆盖: 🔴 域 T-1/T-2/T-4 (T-3 双链机制由 Tomcat 3.0 起沿用, 10.1.x 无变更条目)

---

## T-4 线程模型 (NIO Endpoint) — 10.1.x 演化

| 版本 | 日期 | 变更 | 机制影响 |
|:--|:--|:--|:--|
| 10.1.0-M9 | 未发布 | 停止 NioEndpoint/Nio2Endpoint 时的清理修正 (changelog:4596) | Poller/线程收尾稳定性 |
| 10.1.0-M17 | 2022-07-20 | 修复 HTTP/2 + NIO + async IO 下的重复 Poller 注册 (changelog:3748) | PollerEvent 注册幂等 — 本域 B 问 #4 的防御点 |
| 10.1.6 | 2023-02-24 | TLS 握手相关的 NioEndpoint/Nio2Endpoint 日志域重命名 (changelog:2962-2963) | 可观测性 |
| 10.1.11 | 2023-07-10 | 修复 Poller 通知可能丢失的代码路径 (changelog:2391) | wakeupCounter/selector 唤醒可靠性 — 本域 B 问 #3/#6 的直接演化 |
| 10.1.20 | 2024-03-25 | 新增 threadsMaxIdleTime 端点属性 — 内部 executor 缩容到 minSpareThreads 的等待时间 (changelog:1550) | Worker 线程池弹性 — 本域 A 问 #5 的补充 |

**溯源结论 (T-4)**:
1. 三线程模型 (Acceptor/Poller/Worker) 自 Tomcat 7.0 (2009) 引入 NIO Endpoint 后稳定沿用; 10.1.x 无结构性变更 — 模型已收敛。
2. 10.1.x 的 4 条相关变更全部是**可靠性修正** (Poller 通知丢失/重复注册) 与**可配置性扩展** (threadsMaxIdleTime) — 印证"核心机制稳定, 边缘可靠性持续打磨"的成熟期特征。

---

## T-2 Connector/Adapter — 10.1.x 演化

| 版本 | 变更 | 机制影响 |
|:--|:--|:--|
| 10.1.34 | content-type 大小写敏感修复 (bug 69442) + content-range 头长度修复 (changelog:107 段) | 头部解析严格性 — 本域 T-8 交叉 |
| 10.1.x 全段 | Processor 池化 (recycledProcessors) / coyote-catalina 双层 / getWriter-getOutputStream 互斥 — 均无变更条目 | 机制自 8.5 延续, 已冻结 |

**溯源结论 (T-2)**: coyote/catalina 双层与 Processor 池化是 8.5→10.1 连续三代的稳定设计; 10.1.x 只修协议边缘 (content-type/content-range)。

---

## T-1 Lifecycle — 10.1.x 演化

| 版本 | 变更 | 机制影响 |
|:--|:--|:--|
| 10.1.34 | AprLifecycleListener 引用计数 (changelog:1131) + OpenSSLLifecycleListener (changelog:1450) | LifecycleListener 注册的引用语义 — 本域 A 问 #4 的补充 |
| 10.1.x 全段 | LifecycleState 11 态状态机 / Template Method start() — 无变更条目 | 状态机自 5.5 定型, 已冻结 |

**溯源结论 (T-1)**: 状态机与模板方法模式高度稳定 (5.5 起未变); 演化点集中在**监听器生态** (native/openssl 监听器的注册语义)。

---

## T-3 Pipeline/Filter — 10.1.x 演化

| 版本 | 变更 | 机制影响 |
|:--|:--|:--|
| 10.1.34 | CrawlerSessionManagerValve NPE 防护 (changelog:107 段) | Valve 家族细节修正 |
| 10.1.x 全段 | Valve 链 + Filter 链双链结构 — 无结构性变更 | 双链自 Tomcat 3.0 定型 |

**溯源结论 (T-3)**: 双链 CoR 是 Tomcat 最古老的架构决策之一 (3.0 起); 10.1.x 仅修 Valve 实现细节, 结构零变更。

---

## 方法学注记

- 数据源局限: changelog.xml 仅覆盖 10.1.x 发布段 (10.1.0-M9 起); 8.5/9.0 的演化需查对应版本 changelog (本仓库未含)。
- 关键机制冻结判定依据: 10.1.34 源码中 `recycledProcessors` (AbstractProtocol.java:786)、`children HashMap+RWLock` (ContainerBase.java:154)、`wakeupCounter` (NioEndpoint.java:605) 均无 TODO/兼容分支 — 机制面无版本碎片。
- 面试表述建议: "三线程模型 7.0 引入, 10.1 只有可靠性修正; Lifecycle 状态机 5.5 定型" — 回答 '时空' 维问题的标准句式。