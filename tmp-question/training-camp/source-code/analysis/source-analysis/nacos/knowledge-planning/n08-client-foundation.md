# N-08 客户端基础 — 知识规划 (KP)

> 🟡 B | 模块: client-basic (51) + client/env+utils+monitor+logging+lock | 版本: 3.0.3 | 2 篇大纲

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 原型属性 | NacosClientProperties:36 | PROTOTYPE + derive 继承 |
| 2 | 三源搜索 | SourceType 族 | env/jvm/properties |
| 3 | 认证 SPI | AbstractClientAuthService:28 | 登录 + 上下文 |
| 4 | 登录处理器 | NacosClientAuthServiceImpl:40 | getLoginIdentityContext |
| 5 | SPI 遍历 | SecurityProxy:82-83 | 多认证源 login |
| 6 | RAM 凭证 | CredentialService/Watcher | 凭证加载监控 |
| 7 | STS | StsCredentialHolder | 临时凭证轮换 |
| 8 | 资源注入 | ResourceInjector 四实现 | Naming/Config/Ai/Lock |
| 9 | 前置校验 | ValidatorUtils.checkInitParam | init 首步 |
| 10 | 工具面 | ConcurrentDiskUtil/MetricsMonitor | 磁盘/监控/日志/锁 |

## 02 高频坑

1. NacosClientProperties 必须 PROTOTYPE.derive 创建
2. ClientAuthService 契约在 plugin/auth (非 client-basic)
3. RAM 认证含 STS/签名四类注入
4. 三源 (env/jvm/properties) 可搜索合并
5. checkInitParam 是 init 第一行
6. client-basic 不是只有 remote — env/auth/address/utils 全有

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 属性 | PROTOTYPE / derive / 三源 / 转换器 |
| 认证 | SPI 集合 / LoginProcessor / RAM / STS / 签名 |
| 校验 | ValidatorUtils / ParamUtil / PreInitUtils |
| 工具 | ConcurrentDiskUtil / LogUtils / AppNameUtils |
| 可观测 | MetricsMonitor / NacosLogging |
| 锁 | NacosLockService |

## 04 跨域桥接

- ← NC-1/NC-2/NC-3: 属性源头 (PROTOTYPE) 与校验 (checkInitParam)
- → NC-7: 认证双 SPI 对照
- → ALI-A3: NacosServiceManager 消费属性
- → 面试: "Nacos 客户端底座" — 原型属性 + SPI 认证 + 前置校验
