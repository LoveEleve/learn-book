# SCC-2 @RefreshScope 热刷新 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 跨域一致性)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 6 | 语义精确化 | invoke 里 getObject() 取代理 + **getTargetSource().getTarget() 拿目标** (L480-481) — 代理只做锁管理, 方法执行在目标上 (大纲原只说"反射调目标") | 已修 |
| 7 | **语义重大** | GenericScope.get 的 cache.put 实为 **StandardScopeCache.putIfAbsent** (StandardScopeCache.java:20-25) — wrapper 稳定不覆盖, 这是"二次 get 不重建"的根源; get 本身不用 ReadWriteLock (那是代理层 invoke 的锁) | 已修 |
| 8 | 补强 | copyEnvironment 默认源具体 = **[commandLineArgs, defaultProperties]** (L54-57, 注释"cli args 必须第一") + additionalPropertySourcesToRetain 扩展 (L65/115-117) | 已修 |
| 9 | **大纲缺口** | **ConfigurationPropertiesRebinder 未提** (context/properties/, 重绑定 @ConfigurationProperties) — 与 RefreshScope 重建是互补机制 (属性类重绑定 vs 业务 Bean 重建) | 已补 |
| 10 | 锚点漂移 | RefreshScope @ManagedResource 实际 L68/类声明 L69 (大纲写 L26-29 import 区) + @ManagedOperation L148/164 | 已修 |
| 11 | 验证通过 | EnvironmentChangeEvent 在 context/environment/ (SCC-1 bootstrap 也发布 — 跨域共享事件) / 负面空间 6 条全部成立 (无轮询/不跨上下文/作用域隔离) | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | refresh(String) 实际 **L150** (大纲写 L157-164 偏移); 遗漏 **refresh(Class) L140 类型级入口** | 已修 |
| 2 | 锚点漂移 | LegacyContextRefresher 类 **L42** (大纲写 L46) | 已修 |
| 3 | 锚点漂移 | RefreshScopeLifecycle 类 **L33** + start() **L48-52** (大纲写 L24-28 — 那是 import 区) | 已修 |
| 4 | 锚点漂移 | ConfigDataContextRefresher loadFactoryNames **L80** + Instantiator L83-88 (大纲写 L78-86) | 已修 |
| 5 | 验证通过 | GenericScope L73/L88/L126/L173/L369/L433/L449/L460-490 / RefreshScope L140-171 / ContextRefresher L92-106 / RefreshAutoConfiguration L69-70 / gh-349 L487 / gh-678 L69 | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniRefresh 11/11 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | **并发语义实证** | 写锁被读锁阻塞: reader 持读锁 200ms 时 destroy 未完成 → 读锁释放后完成 — **实证"刷新安全"机制** (旧 Bean 用完才清) | 通过 (11/11) |
| H2 | 设计边界 | 测试 5 曾试图"读锁内调 destroy" — ReentrantReadWriteLock 不支持升级会死锁; 真实设计 destroy 不经代理读锁 (refreshAll 外部调用) | 修正测试为完整链路验证 — 语义说明 |

## 锚点密度统计

- file:line 锚点数: **35+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 5 项检查 4 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (自动感知/字段级更新/跨上下文/原子性/singleton 不刷/异常恢复)
- [x] 每条有对照物 (Apollo 推送式/ConfigurationProperties 重绑定)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (配置变更自动生效/销毁重建时机/并发安全/scope 差异)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (代理读锁 → 双检懒创建 → 双阶段刷新 → 懒重建)
- [x] 边界交代: 读锁/写锁语义/新旧刷新器/refresh vs refreshAll

## 方法论教训

- **harness 并发断言是"刷新安全"的最佳实证** — 写锁阻塞读锁的时序测试比文字描述有力
- **ReentrantReadWriteLock 升级死锁是真实陷阱** — destroy 必须不经代理读锁 (refreshAll 外部调用), 微缩版设计要对照真实调用路径
- **类行号 vs import 行号** — RefreshScopeLifecycle L24-28 是 import 区, 类声明在 L33 — 锚点必须落在声明
