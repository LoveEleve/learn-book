# C-8 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + harness 8/8 实证

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "registerService → Entry + 临时节点" — ServiceDiscoveryImpl.java:178-190 | 通过 ✅ (harness A: 节点存在 + ephemeralOwner≠0) |
| 2 | 事实 | §1 "start → reRegisterServices" — L141-148 | 通过 ✅ (源码核对) |
| 3 | 事实 | §1 "updateService MAX_TRIES=2" — L192-210 | 通过 ✅ (harness E: 端口更新生效) |
| 4 | 事实 | §2 "ServiceCache 基于 CuratorCacheBridge" — ServiceProviderImpl.java:77 builder.build() | 通过 ✅ (harness B: 注册后缓存可见) |
| 5 | 事实 | §3 "过滤链 downInstanceManager + isEnabled" — L79-82 | 通过 ✅ |
| 6 | 事实 | §3 "getInstance → strategy" — L125-127 | 通过 ✅ (harness G: RoundRobin 交替) |
| 7 | 事实 | §4 "DownInstanceManager apply: count < threshold 保留" — DownInstanceManager.java:56-60 | 通过 ✅ (harness F: 同实例 3 错后被排除 — **注意阈值语义: 错误数 ≥ errorThreshold 才排除, 且须同一实例**) |
| 8 | 事实 | §4 "purge 窗口过期恢复" — L63-82 | 通过 ✅ (源码核对; purge 触发 = timeout/2 周期检查) |
| 9 | 事实 | §5 "JsonInstanceSerializer" — details/JsonInstanceSerializer | 通过 ✅ |
| 10 | 过程 | API 修正: ServiceCache/ServiceProvider 由 discovery.serviceCacheBuilder() 获取 (非静态 builder); ServiceCacheBuilder 无 client() 方法 (client 由 discovery 隐式带) | 通过 ✅ (harness 编译修正) |
| 11 | 过程 | **harness 实证发现**: RoundRobin 轮询交替 (a-1/a-2); 降级排除后可用集 1 | 通过 ✅ |

**结论**: 11 项核对 0 修正 (2 项 API 使用面澄清)。harness 8/8 覆盖: A 注册 B 缓存 C 选择 E 更新 F 降级 G 轮询 D 注销。
