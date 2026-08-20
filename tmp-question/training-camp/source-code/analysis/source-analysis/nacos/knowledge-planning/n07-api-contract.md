# N-07 API 契约面 — 知识规划 (KP)

> 🟡 B | 模块: api (274 文件 22,889 行) | 版本: 3.0.3 | 3 篇大纲

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | Naming 接口族 | NamingService:36 | 6 重载注册 + 订阅族 |
| 2 | Naming pojo | Instance/ServiceInfo | 双用模型 (传输+缓存) |
| 3 | 事件分层 | Event/NamingEvent/FuzzyWatchChangeEvent | 精确+模糊双通道 |
| 4 | Config 接口 | ConfigService:32 | 三读三写 + CAS |
| 5 | 监听抽象 | Listener/AbstractSharedListener | 内容/上下文/变更三态 |
| 6 | 过滤契约 | filter/ 族 | IConfigFilter 链 |
| 7 | 注解族 | @NacosValue/@NacosConfigurationProperties | 集成层消费 |
| 8 | 请求分型 | Request/InternalRequest/ServerRequest | 连接面/业务面 |
| 9 | 异步契约 | RequestFuture/RequestCallBack/PushCallBack | Future+回调 |
| 10 | 业务协议 | naming/remote + config/remote | 按域分包 + ability 协商 |

## 02 高频坑

1. AbstractSharedListener 契约在 api 非客户端
2. Instance.toInetAddr 是 InstancesDiffer 的 key
3. ConfigType/PropertyChangeType 枚举是合法值域
4. Payload 是序列化契约
5. fuzzyWatch 接口在 ConfigService, 实现在 worker
6. api/grpc/auto 是 proto 生成面
7. PreservedMetadataKeys 是跨模块常量契约

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 接口契约 | NamingService/ConfigService/NamingMaintainService |
| 数据契约 | pojo 族 / model 族 / ListView |
| 事件契约 | Event 族 / FuzzyWatch 族 / ConfigChangeEvent |
| 过滤契约 | IConfigFilter 链 / AbstractConfigFilter |
| 注解契约 | @NacosValue 等 5 注解 / NacosConfigConverter |
| 协议契约 | Request 分型 / Response / Future/Callback / ability |

## 04 跨域桥接

- → NC-1/NC-2: 实现方实证 (接口↔实现对应)
- → NC-3: Payload 序列化 / ability 协商路由
- → ALI-A2: 注解族消费方
- → 面试: "Nacos 对外契约" — 接口面 + 事件面 + 协议面
