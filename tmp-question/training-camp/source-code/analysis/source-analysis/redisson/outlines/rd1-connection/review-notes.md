# RD-1 主类+连接管理 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程 + 二次 REVIEW)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **行号偏差** | ConnectionManager.create 报错/连接位置: 初稿写 "L104-107 抛错 / L105-108 connect" — awk 穷举实测 **L104-106 = null 检查+throw; L107-108 = lazy 分支 connect; L110 = return** | 01-create L28-30 + pass2-q3/q7 修正 |
| 2 | **表述精确化** | detectCluster "连接成功后还有集群探测" — 实测是 EVAL 双 key 脚本 + CROSSSLOT 识别 (非主动探测集群模式), MasterSlave 模式下自动识别集群节点 | pass2-q3 + 02-connection S2 重写 "4.x 拓扑自适应" |
| 3 | **机制实证 (harness)** | lazyConnect 单飞锁 + AsyncSemaphore permit 精确性 → MiniConnectionManager 20/20 PASS: 并发只连一次/重入不死锁/失败重试/permit 超容量回滚 | harness 对照 L190-227, L44-59 语义 |
| 4 | **跨域错位 (REVIEW3)** | 原知识网络引用 R-26/R-11 (未产出) + B-10/B-12 (非实际目录); s75 "连接深入在阶段3" 待承接未回应 | §五重写 17 条真实域双向引用表 + 承接点表 (详见 REDISSON-PLAN REVIEW3) |
| 5 | **术语修正** | completeness ⚠️Q31 connectingThread 修饰符 — 实测 `volatile` (L70); Q11 server-times = Version.logVersion() (Redisson.java:67 Reduce) | 篇3 补 "volatile 线程身份"; 篇1 reader 处境补来源 |
| 6 | **池层无超时确认** | 假设 acquireConnection 有等待上限 — 实测返回 CompletableFuture<T> 无池层 timeout | 篇2 S3 改 "超时在命令层 timeout=3000ms (RD-4)" |
| 7 | 通过项 | 全锚点行号 grep/awk 验证 ~25 处: Redisson 66-86/119 ✅ / Config 165 ✅ / ConfigSupport 844 ✅ / ServiceManager 122-156/297/766 ✅ / MasterSlaveConnMgr 70/190-227/229-331/264-284 ✅ / MasterSlaveEntry 569/585/500-545 ✅ / DNSMonitor 53-283/152 ✅ / ConnectionsHolder 44-59/141/224/263 ✅ | 记录 |

## 07 五维度

### 维度1 功能正确性
- 五步初始化链冗余验证 (Redisson.java:66-86 逐行对照)
- lazyConnect 三重防护 harness 实证: CAS 定所有者 / connectingThread 重入截断 / isCompletedExceptionally 放行重试
- 池 permit 协议不变式 (counter==poolMax) 20/20 断言通过 — 4.6.1 tryRun 竞态教训复现 (超容量回滚)

### 维度2 性能
- 双池分离: 读走 slave 池 (响应不占主池 permit), ReadMode.SLAVE 降级 master 兜底
- borrow 零信号量 (free 队列 poll), 满才等 — 与 Hikari ConcurrentBag 线程本地表对照 (h02 80/20)
- RR 轮询 (rrCounter floorMod) 分摊多 entry; RoundRobinBalancer 分摊 slave

### 维度3 内存
- Config 防御性复制 (copy ctor) → 构造后不可变, 无热对象
- latch/java lang future 每请求对象化, 由 Netty EventLoop 承载

### 维度4 一致性/并发
- lazyConnect 单飞原子性: AtomicReference + volatile connectingThread 全可见性
- DNS 切换成功才提交 + 失败回滚 → 不出现双主
- changeMaster 旧 master 降级 slave (useMasterAsSlave) 保证至少一可用
- 连接池双队列 + AsyncSemaphore: 精确配对释放, 竞态修复史记录 (4.6.1)

### 维度5 边界/安全
- IllegalArgumentException 只对配置错误 (不重试), InterruptedException 吞掉前恢复中断标志 (L257)
- UDS 仅 single + EPOLL/KQUEUE 校验 (ServiceManager 构造)
- NAT/多 IP: 开源单 IP 直取 + Pro 警告 (DNSMonitor:110-116)
- lazy 懒连接失败: internalShutdown 回收已建 (doConnect catch L322)

## 完成状态

- [x] 三篇大纲 (01-create/02-connection/03-pubsub-dns) — 全部含 前置/复用/对照/引出 四行双链
- [x] completeness-questions 50 问 (6 身份, 大域基准) + 回填动作
- [x] harness MiniConnectionManager 20/20 PASS (模式工厂/单飞锁/permit 池)
- [x] 行号锚点穷举核对 (修正 4 处)
- [x] 跨域引用 REVIEW3 (REDISSON-PLAN §五重写)
- [ ] 待下一域 (RD-3) 开始前回填: completeness 尾部 ⚠️ 6 项深审回填