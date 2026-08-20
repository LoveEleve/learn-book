# N-08-01 客户端基础 — 认证插件面与 RAM 签名 (认证篇)

> 前置: [[NC-7-安全]] (ProtocolAuthService 服务端面) | 引出: N-08-02 (env/工具) | 对照: 客户端认证 SPI vs 服务端认证插件
> 🟡 B | 方案 B (重要域) | 闭环: q1(SPI 面) q2(登录处理器) q3(RAM 签名)

**读者处境**: SecurityProxy.login 背后的 ClientAuthService SPI 怎么组织? 阿里云 RAM 认证怎么签名?

### 1. 认证 SPI 面 — ClientAuthService 族

场景: 客户端认证怎么可插拔?
源码路径:
- **ClientAuthService** (plugin/auth/spi/client): 接口 — login + getLoginIdentityContext
- **AbstractClientAuthService** (plugin/auth/spi/client/AbstractClientAuthService.java:28): 抽象基类
- **NacosClientAuthServiceImpl** (client-basic/auth/impl/NacosClientAuthServiceImpl.java:40): 默认实现 — **getLoginIdentityContext** (L121)
- **LoginProcessor** (auth/impl/process/LoginProcessor.java:27) / **HttpLoginProcessor**: 登录处理器
- 消费方: **SecurityProxy.login 遍历 SPI 集合** (client/security/SecurityProxy.java:82-83: clientAuthPluginManager.getAuthServiceSpiImplSet())
关键设计 (q1): **"SPI 集合 = 多认证源并存"** — SecurityProxy 遍历所有 ClientAuthService SPI 逐个 login; 与 NC-7 服务端 ProtocolAuthService 形成客户端/服务端双 SPI 面。 [模式: 双 SPI 认证]

### 2. RAM 认证 — 阿里云签名族

场景: 阿里云 RAM 认证怎么工作?
源码路径:
- **RamClientAuthServiceImpl** (auth/ram/RamClientAuthServiceImpl.java:46): RAM 实现
- **CredentialService** (auth/ram/identify/CredentialService.java:31) + **CredentialWatcher** (identify/CredentialWatcher.java:42): 凭证加载与监控
- **Sts 族** (StsConfig/StsCredentialHolder/StsCredential): STS 临时凭证
- **ResourceInjector 族** (AbstractResourceInjector + Naming/Config/Ai/Lock 四实现): 按资源类型注入
- **签名工具** (SignUtil/SpasAdapter/CalculateV4SigningKeyUtil/RamUtil): 请求签名
关键设计 (q2): **"RAM = 凭证 + 注入 + 签名三步"** — 凭证 (含 STS 轮换) → 按资源注入 → 请求签名; 四类资源注入器 (Naming/Config/Ai/Lock)。 [模式: RAM 三步]

### 3. 测试与行为锚

场景: 认证边界?
源码路径:
- 测试: client-basic auth test
- 常量锚: IdentifyConstants/RamConstants/NacosAuthLoginConstant
关键设计 (q1): **"常量集中 = 协议稳定"** — 签名/凭证常量集中。 [模式: 常量契约]
