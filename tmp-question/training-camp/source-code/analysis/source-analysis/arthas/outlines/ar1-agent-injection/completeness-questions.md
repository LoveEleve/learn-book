# 域 AR-1: Agent 注入与 SpyAPI — 全视角提问验证

> > 31 KP / 🔴5 + 🟡5 + 🟢4 | 30+ 文件 | 拆 3 篇文章
> 验证方法: 逐题检查 3 篇大纲是否覆盖了该视角的关注点

**大纲文件对照**:

| 篇 | 文件 | 覆盖范围 |
|:--:|------|------|
| 1 | `01-attach-paths.md` | 外部 attach 全链 + ArthasClassloader 隔离 + 进程内自 attach |
| 2 | `02-bootstrap-init.md` | 单例/构造 7 步 + initSpy 注入 + enhanceClassLoader + 参数链 |
| 3 | `03-server-bind-destroy.md` | bind() 启动 + 安全三道闸 + destroy() 销毁 + 生命周期时间线 |

---

## 维度 1: 开发者 (Developer)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| D1 | `loadAgent` 的参数为什么是 `coreJar路径;agentArgs` 两段?在哪切的? | ✅ 篇 1 §1 — AgentBootstrap.java:110-119 indexOf(';') |
| D2 | `ArthasClassloader` 的双亲委派和普通 CL 有什么不同?为什么能隔离? | ✅ 篇 1 §2 |
| D3 | 重复 attach 会发生什么?幂等怎么做? | ✅ 篇 1 §1 + 篇 2 §1 — isInited + 单例 |
| D4 | 配置参数(`--session-timeout 3600`)的完整传递链路? | ✅ 篇 2 §4 — String→Map→Configure |
| D5 | `appendToBootstrapClassLoaderSearch` 之后,为什么所有 CL 都能 loadClass 到 SpyAPI? | ✅ 篇 2 §2 |
| D6 | `SpyAPI` 为什么要放在 `java.arthas` 包里? | ✅ 篇 2 §2 |
| D7 | `stop` 之后字节码增强怎么被撤销的?Spy 引用怎么清? | ✅ 篇 3 §3 — destroy 链 + setNopSpy |
| D8 | `--session-timeout 3600` 从字符串到 `Configure.sessionTimeout` 字段经过哪三层转换? | ✅ 篇 2 §5 — FeatureCodec→BinderUtils→ArthasEnvironment |
| D9 | ArthasClassloader 和 AttachArthasClassloader 是两套策略吗? | ✅ 篇 1 §2 — 同一策略(child-first + sun./java. 走 parent) |
| D10 | 本机连接(127.0.0.1)也要密码吗?哪个配置决定? | ✅ 篇 3 §2 — LocalConnectionPrincipal + localConnectionNonAuth |

## 维度 2: 架构师/平台工程师 (Architecture)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| A1 | as.sh 外部 attach 和 Starter 进程内 attach 的本质差异?各适用什么场景? | ✅ 篇 1 §1/§3 |
| A2 | 0.0.0.0 监听时为什么强制生成密码?安全模型有几层? | ✅ 篇 3 §2 |
| A3 | `enhanceLoaders` 什么时候才需要?原理是什么? | ✅ 篇 2 §3 — issue #1596 |
| A4 | destroy 的"从外到里"顺序为什么重要?半启动失败如何回滚? | ✅ 篇 3 §3/§1 |
| A5 | 埋点(UserStatUtil)为什么用独立守护线程?上报失败会怎样? | ✅ 篇 3 §1 — 异步旁路,失败不影响主流程 |
| A6 | Configure 字段为什么禁止默认值?默认值由谁兜底? | ✅ 篇 2 §5 — 消费方兜底,保证优先级一致 |

## 维度 3: JVM/字节码工程师 (Bytecode)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| B1 | `ClassLoader_Instrument` 的 `@Instrument` 模板机制——模板字节码怎么织入 ClassLoader.loadClass? | ✅ 篇 2 §3 — ByteKit InstrumentTransformer |
| B2 | `retransformClasses(ClassLoader.class)` 对已加载类生效的原理? | ✅ 篇 2 §3 — 已加载类热替换 |
| B3 | SpyAPI 薄转发层设计——为什么注入点极小化? | ✅ 篇 2 §2 关键设计 |
| B4 | `transformerManager.destroy()` 与 removeTransformer 的关系? | ✅ 篇 3 §3 — 指向 AR-2 |

## 维度 4: 运维/SRE (Operations)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| O1 | attach 失败的表现是什么?如何判断"port binding failed"? | ✅ 篇 1 §1 + 篇 3 §1 — isBind 检查 |
| O2 | 停 arthas 的正确姿势(stop vs exit)?重启 arthas 的前提? | ✅ 篇 3 §3 — resetArthasClassLoader |
| O3 | 隧道模式(tunnel-server)的连接建立时机与失败处理? | ✅ 篇 3 §1 — await 10s |
| O4 | `disabledCommands=stop` 的意义(starter 场景)? | ✅ 篇 1 §3 + 篇 3 §2 |

## 维度 5: 面试者 (Interview)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| I1 | "讲一下 arthas attach 的原理" — 从 as.sh 到 AgentBootstrap 完整链路? | ✅ 篇 1 §1 |
| I2 | "为什么 arthas 不污染业务代码?" | ✅ 篇 1 §2 — ArthasClassloader |
| I3 | "被增强的方法为什么能调用 SpyAPI?" | ✅ 篇 2 §2 — Bootstrap 注入 |
| I4 | "arthas 停止后增强会消失吗?" | ✅ 篇 3 §3 |

---



## 审查结论

| 维度 | 问题数 | 全覆盖 | 状态 |
| 开发者 (Developer) | 10 | 10 | ✅ |
| 架构师/平台工程师 (Architecture) | 6 | 6 | ✅ |
| JVM/字节码工程师 (Bytecode) | 4 | 4 | ✅ |
| 运维/SRE (Operations) | 4 | 4 | ✅ |
| 面试者 (Interview) | 4 | 4 | ✅ |
| **合计** | **28** | **28** | **✅ 全覆盖** |

---

## 覆盖检查

- 31 KP 全部在 3 篇大纲中落地(每个机制有独立小节或关键设计)✅
- 5 个身份视角 × ≥2 问 ✅(共 18 问)
- 跨域桥: AR-0(attach/stop 使用)→ AR-2(TransformerManager/SpyAPI 调用侧)→ 全部铺好 ✅
- 关键悬念(隔离/注入/可逆)在每篇开头有场景句 ✅