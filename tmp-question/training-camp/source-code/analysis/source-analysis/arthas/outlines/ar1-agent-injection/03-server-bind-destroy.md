# 03. 端口、密码、隧道与"有始有终" — bind() 启动与 destroy() 销毁

> 🔴 Deep | 31 KP 中的 2 个汇聚机制(bind / destroy)
> 读者处境: arthas 已寄生、SpyAPI 已就位——现在它要开门迎客(终端/隧道),最后还要保证撤得干净。

### 1. "开门迎客" — bind(): Server 启动全景

场景: 构造器走完 7 步,最后一步 `bind(configure)`——telnet/http 端口、密码、隧道、命令注册全在这里。

- 随机端口: `telnetPort/httpPort == 0` 时 `SocketUtils.findAvailableTcpPort()`(ArthasBootstrap.java:375-384)
- appName 探测(:386-389): `System.getProperty("project.name")` → 兜底 `spring.application.name`
- TunnelClient 按需启动(:392-400): 配了 `tunnelServer` 才连——`setAppName/setId(agentId)/setTunnelServerUrl` → `start()` + await 10s
- **0.0.0.0 强制安全**: 监听全网卡且无密码时自动生成 64 位随机密码并告警(:415-426,"External users can connect to your machine!")
- `SecurityAuthenticatorImpl(username, password)`(:428)→ `ShellServerImpl(options)`(:430,含 session-timeout×1000 :410-412)
- 注册终端: `HttpTelnetTermServer`(telnet :452)+ `HttpTermServer`(http :458,以及隧道模式下的本机监听 :464)
- 命令注册(:439-445): `BuiltinCommandPack(disabledCommands)` + 外部命令 resolver(`loadExternalCommandResolver` :520,commands 目录 jar)
- 收尾: `SpyAPI.init()`(:507,INITED=true,业务线程可正常回调)+ UserStatUtil 埋点(:499-504)
- 参数落点: session-timeout 秒×1000 → `ShellServerOptions.setSessionTimeout`(ShellServerOptions.java:22-43,**默认 3 小时**)
- 失败处理: catch → `destroy()`(:513-517)+ 抛错 → AgentBootstrap 的 isBind 检查失败报 "port binding failed"

关键设计: [模式: 服务组装器(bind 编排)+ 对称生命周期(destroy 与启动逐项对偶)+ 认证策略(SecurityAuthenticator 三主体)] bind 是**一个方法内完成的完整服务编排**——[Java: SocketUtils.findAvailableTcpPort 在绑定前探测可用端口(随机端口模式,common/SocketUtils.java:127);ShellServerImpl 基于 Vert.x 风格的事件循环,TermServer 是端口监听抽象]——端口/安全/隧道/终端/命令,任何一个失败都整体销毁回滚。
- **埋点旁路**: `UserStatUtil`(util/UserStatUtil.java:18-110)用单守护线程 `arthas-UserStat` 异步上报启动/命令统计(ip/version 静态捕获 :32-34;未配 stat-url 直接 return :57-58)——**上报失败绝不影响主流程**。

关键设计(续): `SpyAPI.init()` 放在**最后**(ArthasBootstrap.java:507): 前面任何一步失败,SpyAPI 保持未激活,业务侧 SpyAPI.atEnter 全是 NOPSPY 空转(零开销),不会"半启动状态"下被业务代码打到错误路径。

### 2. "安全的三道闸" — 密码 / SecurityAuthenticator / disabledCommands

场景: 生产机器,任何人 telnet 到 3658 就能操作你的 JVM——怎么防?

- 三道闸: ① 随机密码(0.0.0.0 时,ArthasBootstrap.java:415-426)② `SecurityAuthenticatorImpl`(:428,用户名+密码校验)③ `disabledCommands`(:432-438,BuiltinCommandPack 注册时过滤 `@Name` 匹配的命令)
- 隧道模式: tunnel-server 侧也要鉴权(agent-id 注册)

关键设计: 密码是**自动生成而非默认空**——"安全默认值"原则: 监听 0.0.0.0 时宁可直接断掉隐式信任,逼迫显式配置;`disabledCommands` 在**注册阶段**就剔除(不是执行阶段拦截),被禁命令连命令表都不存在。

### 3. "撤得干净" — destroy(): 与启动对称的销毁链

场景: 你敲下 `stop`(AR-0 篇 1),arthas 要离开——但它的字节码增强、Transformer、Spy 引用都必须清掉。

- destroy()(ArthasBootstrap.java:838-888)完整链:
  1. `shellServer.close()`(:844)/`sessionManager.close()`/`httpSessionManager.stop()`(:847-853)
  2. `timer.cancel()`(:855)— dashboard 等定时器
  3. `tunnelClient.stop()`(:859)
  4. `executorService.shutdownNow()`(:865)— 命令执行线程池
  5. **`transformerManager.destroy()`(:868)** — 移除所有增强 Transformer(AR-2 关键: 这就是"增强被撤销"的机制)
  6. `instrumentation.removeTransformer(classLoaderInstrumentTransformer)`(:871)— 移除 ClassLoader 增强
  7. **`cleanUpSpyReference()`(:874)**: `SpyAPI.setNopSpy()` + `SpyAPI.destroy()`(:944-945)— Spy 引用清零,INITED=false
  8. 反射 `AgentBootstrap.resetArthasClassLoader()`(:951-953)— 全局 CL 置空,**允许下次重新 attach**
  9. `UserStatUtil.destroy()` + 移除 shutdown hook(:877-883)
- 触发方式: ① `stop` 命令 ② JVM 退出时的 shutdown hook(:186-192)

关键设计: **销毁是"从外向里"**: 先关服务面(shell/tunnel/线程),再摘增强面(transformer),最后清引用面(SpyAPI/ClassLoader)——顺序保证"摘钩"时不会有新的增强调用涌入。`setNopSpy` 与 `init` 对称: 业务字节码里的 SpyAPI 调用还在,但实例已被替换成 NOPSPY(空实现),此后零开销、永不报错——**这就是"增强的代码不清理也能安全运行"的兜底**。

### 4. 一个完整生命周期的时间线

```
attach(loadAgent/ByteBuddyAgent)
  → AgentBootstrap.agentmain(幂等检查)
    → ArthasClassloader 加载 core
      → ArthasBootstrap.getInstance(单例)
        → 构造 7 步(initSpy: SpyAPI 注入 Bootstrap)
        → bind(端口/密码/隧道/终端/命令/SpyAPI.init)
        → 用户操作(watch/trace → AR-2)
        → stop / shutdown hook
          → destroy(关服务 → 摘 transformer → setNopSpy → reset CL)
```

关键设计: 整条链的设计哲学是**"寄生可逆"**——attach 时每挂一样东西,destroy 都对应摘掉;摘不掉的就降级成 NOPSPY 空转。生产上"停掉 arthas 恢复原状"的承诺,靠的就是 destroy 的对称性。

---

跨域桥: `transformerManager.destroy()` 与 Enhancer 注册 = AR-2;TunnelClient/agent-id = AR-0 篇 1 的隧道配置;stop/reset 使用 = AR-0 篇 1 §3;SpyAPI.setNopSpy 后的 NOPSPY 语义 = AR-2 SpyAPI 全貌。

---

**OpenJDK 关联**:  [OpenJDK 域 47 Instrumentation — outlines/47-instrumentation/] — removeTransformer 的 JDK 侧语义;**另见** [OpenJDK 域 36 Attach — outlines/36-attach/] — 端口探测与 attach 生命周期。

### 核心悬念

**"stop 之后,watch/trace 织入的代码去哪了?"** — 字节码还在方法里(改不回原样),但 SpyAPI 的实例被换成了 NOPSPY——调用还在,零开销,永不报错。这个"摘不掉就降级"的设计,是下一步理解 AR-2 增强链的关键心态。

> → [AR-2 篇 2](../ar2-watch-trace/02-bytekit-enhancer.md)
