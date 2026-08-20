# ALI-A9 Seata 分布式事务 — XID 的三路透传与收尾校验

> 前置: [[ALI-A6-三路限流]] (三路透传对照) | 引出: [[Seata]] (RootContext/全局事务内核) | 对照: OpenFeign 5.6 (RequestInterceptor 面) + SCC-5 (RestTemplate 拦截器链)
> 🔴 A | 方案 A (全深度) | 闭环: q1(透传三路) q2(bind/unbind) q3(收尾校验) q4(装配面)

**读者处境**: 一个分布式事务跨 A→B 两个服务, XID 怎么从 A 的客户端传到 B 的上下文? 请求结束 XID 怎么清? 为什么 Feign 的 retryer 会被强制改成 NEVER_RETRY?

### 1. 透传三路 — Feign/RestTemplate/Web 的 XID 携带

场景: 出站请求怎么带上 XID?
源码路径:
- **Feign 路**: SeataFeignRequestInterceptor (feign/SeataFeignRequestInterceptor.java:28): implements RequestInterceptor — apply: `RootContext.getXID()` (L32) + **无 XID 直接 return** (L33-35) + `template.header(RootContext.KEY_XID, xid)` (L37)
- **RestTemplate 路**: SeataRestTemplateInterceptor (rest/SeataRestTemplateInterceptor.java:33): intercept — getXID + **requestWrapper.getHeaders().add(KEY_XID, xid)** (L43, 用包装器不可变转可变)
- **Web 入站**: SeataHandlerInterceptor.preHandle (web/SeataHandlerInterceptor.java:40-57): 本地 xid 空 && rpcXid 非空 → **RootContext.bind(rpcXid)** (L52)
- 三路共用 RootContext (Seata 内核) 与 KEY_XID 常量
关键设计 (q1): **"三路透传 = 同源 XID 单向传递"** — 出站 (Feign/RT) 读 RootContext 写 header, 入站 (Web) 读 header 写 RootContext; RootContext 是 XID 的唯一持有者, 集成层只做搬运。 [模式: 透传搬运]

### 2. 收尾校验 — afterCompletion 的 unbind 与一致性检查

场景: 请求结束后 XID 残留怎么办?
源码路径:
- **afterCompletion** (SeataHandlerInterceptor.java:59-80): RootContext 有 xid 且 rpcXid 非空才处理 (L60-64) → **RootContext.unbind()** (L65) → **一致性校验**: rpcXid != unbindXid → warn "xid in change during RPC" (L69-70) → **回绑原 xid** (L71-76, 防线程复用污染)
- rpcXid 空 → return (L63-65, 本请求无事务透传不清理 — 防误清他人)
关键设计 (q2): **"收尾 = 校验 + 回绑双保险"** — unbind 后比较, 不一致说明期间 XID 被改 (嵌套调用?), warn 并回绑; 线程池复用下防 XID 串线。 [模式: 校验回绑]

### 3. Retryer 接管 — SeataFeignBuilderBeanPostProcessor 的强制改造

场景: Feign Builder 的 retryer 为什么被改?
源码路径:
- **SeataFeignBuilderBeanPostProcessor** (feign/SeataFeignBuilderBeanPostProcessor.java:31): BeanPostProcessor — postProcessAfterInitialization: **Feign.Builder 实例 → retryer(Retryer.NEVER_RETRY)** (L22-25) + log "change the retryer"
- 理由: **全局事务中重试会重复执行已提交的本地分支** — 幂等性破坏 (Seata 设计决策)
- 装配: SeataFeignClientAutoConfiguration (feign/ 43 行)
关键设计 (q3): **"事务场景禁用重试 = 幂等保障"** — 分布式事务下 Feign 重试 = 已提交分支重复执行, 必须 NEVER_RETRY; 集成层强制, 用户不可覆盖。 [模式: 事务优先约束]

### 4. 装配面 — 三 AutoConfiguration 与拦截器注入

场景: 三路拦截器怎么装配?
源码路径:
- **Feign 面**: SeataFeignClientAutoConfiguration — SeataFeignRequestInterceptor Bean + SeataFeignBuilderBeanPostProcessor
- **RestTemplate 面**: SeataRestTemplateAutoConfiguration + **SeataRestTemplateInterceptorAfterPropertiesSet** (rest/...: InitializingBean — afterPropertiesSet 遍历**所有** RestTemplate Bean (L29-30, @Autowired Collection) + interceptors 复制重设 (L32-36) — 全量注入)
- **Web 面**: SeataHandlerInterceptorConfiguration (web/ 34 行) — WebMvcConfigurer 注册拦截器
关键设计 (q4): **"全量注入 = 无注解侵入"** — RestTemplate 面遍历所有实例注入 (无需 @SeataRestTemplate), 与 A6 的注解驱动形成对照: 事务透传是全局的, 限流是选择性的。 [模式: 全量织入]

### 5. 测试与行为锚

场景: 透传的边界行为?
源码路径:
- 测试: SeataFeignTests / SeataRestTemplateTests (test 目录)
- 注释锚: "bind {} to RootContext" (L54) / "xid in change during RPC" (L69) — 行为可观测
- @see io.seata.core.context.RootContext (L32) — 内核引用锚
关键设计 (q1): **"日志锚 + 内核引用"** — debug 日志记录每一步 bind/unbind, 变更 warn — 透传链路可观测。 [模式: 链路可观测]
