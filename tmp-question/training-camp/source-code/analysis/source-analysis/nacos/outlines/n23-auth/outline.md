# N-23 认证/权限深化 — IdentityContext 构建与资源解析

> 前置: [[NC-7-安全]] (ProtocolAuthService 面) | 对照: 身份/资源/权限三件套
> 🟡 B | 方案 B (重要域) | 闭环: q1(身份构建) q2(资源解析) q3(校验链)

**读者处境**: 认证的三件套 (身份/资源/权限) 怎么组织? gRPC/HTTP 身份构建差异?

### 1. 身份构建 — IdentityContextBuilder 双实现

场景: 请求身份怎么提取?
源码路径:
- **IdentityContextBuilder** (context/IdentityContextBuilder.java:26): 接口
- **GrpcIdentityContextBuilder** (context/GrpcIdentityContextBuilder.java:36): gRPC 身份构建
- **HttpIdentityContextBuilder** (context/HttpIdentityContextBuilder.java:37): HTTP 身份构建
- 消费: ProtocolAuthService.validateIdentity 前提取
关键设计 (q1): **"身份双构建 = 协议差异隔离"** — 从 gRPC metadata / HTTP header 提取登录上下文。 [模式: 身份构建]

### 2. 资源解析 — ResourceParser 六实现

场景: 请求资源怎么解析?
源码路径:
- **ResourceParser** (parser/ResourceParser.java:29) / **AbstractResourceParser** / **DefaultResourceParser**
- **grpc/** (AbstractGrpcResourceParser + ConfigGrpcResourceParser + NamingGrpcResourceParser): gRPC 资源
- **http/** (AbstractHttpResourceParser + ConfigHttpResourceParser + NamingHttpResourceParser + AiHttpResourceParser): HTTP 资源
- 按协议×领域 (config/naming/ai) 分型
关键设计 (q2): **"资源解析 = 协议×领域矩阵"** — 解析出资源 (namespace/service 等) 供权限判定。 [模式: 解析矩阵]

### 3. 校验与服务器身份 — Secured + ServerIdentity

场景: 权限校验与服务器身份?
源码路径:
- **@Secured** (annotation/Secured.java:37): 权限注解 (资源/动作)
- **ServerIdentityChecker** (serveridentity/ServerIdentityChecker.java:27) + **ServerIdentityCheckerHolder** / **DefaultChecker**: 服务器身份校验
- **NacosAuthConfigHolder** (config/NacosAuthConfigHolder.java:30): 认证配置
- **AuthErrorCode** (config/AuthErrorCode.java:24): 错误码
关键设计 (q3): **"Secured 注解 = 声明式权限"** — 资源动作声明, 校验器执行; 服务器身份防伪造节点。 [模式: 声明式权限]

### 4. 测试与行为锚

场景: 认证边界?
源码路径:
- 测试: auth test (ProtocolAuthServiceTest 等)
- 锚: Secured 注解字段
关键设计 (q1): **"注解即权限声明"**。 [模式: 声明契约]
