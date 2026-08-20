# ALI-A3 Nacos 服务发现+注册 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 getInstances "异常时 failureToleranceEnabled → ServiceCache" — 未提 getServices 的不对称 | 大纲补: getServices 失败默认返回 emptyList 不抛异常 (L87-88) |
| 2 | 规划未提 NacosAutoServiceRegistration 的事件重注册 | 大纲补: NacosDiscoveryInfoChangedEvent → restart (L107-115) |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿断言 "selectInstances 健康过滤在客户端" | 实测第三个参数 true 即服务端健康过滤 (NacosServiceDiscovery:58), 客户端 hostToServiceInstance 是二次过滤 — 双段式精确化 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "NacosServiceManager 是静态单例" (类比 ConfigManager) | volatile 实例字段 + 双检 — 可关停重建, 与 ConfigManager 静态单例不同 — 大纲 §5 明示差异 |

## 审 4: 跨项目概念转移 — 0 (SCC-3/4 契约对照已验证)

## 审 5: 覆盖率 — 0 缺漏 (7 节: 发现双链/过滤元数据/注册仪式/缓存/管理器/Registration/装配)

## 审 6: 跨层一致性 — 0 (harness 20/20)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| Instance 字段命名 isEnabled vs enabled 编译错 | harness 内部不一致 — 修正为 enabled (源码 Nacos 2.x 用 isEnabled() getter) |
| 初版未覆盖"getServices 不对称"与"unsupported status 忽略" | 补 2 断言 — 源码 L130-134 的 warn 语义实证 |

## 结论: 4 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | DiscoveryClient:47-48 (容错默认 false) + 68-74 (双通道) / ServiceDiscovery:58 (selectInstances true) + 89 (双过滤) / Registry:74 (registerInstance) + 140-148 (enabled 翻转) / AutoReg:56-61 (端口仲裁+Assert) + 107-110 (事件 restart) / ServiceCache:46-48 (静态) + 55-57 (unmodifiable) / ServiceManager:86-95 (双检) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | 六键元数据 ↔ A4 NacosBalancer 回读消费闭环; ServiceCache 写穿 ↔ SCC-3 契约面 | 通过 ✅ |
| 3 | 数字自洽 | 五方法/六键/双通道/2021.0.1.x 双锚全文一致 | 通过 ✅ |
