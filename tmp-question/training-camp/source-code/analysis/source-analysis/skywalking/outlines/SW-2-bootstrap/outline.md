# SW-2 OAP Bootstrap / Module SPI — outline 收敛版

> 核心文件: `ModuleManager.java` / `ModuleDefine.java` / `ModuleProvider.java` / `BootstrapFlow.java`
> 日期: 2026-08-17

## 一、ModuleManager.init
- 从 `ApplicationConfiguration.moduleList()` 取启用模块
- 用 `ServiceLoader<ModuleDefine>` 发现 module define
- 只准备配置中命中的 module
- moduleSet 仍有剩余 → `ModuleNotFoundException`
- prepare 阶段结束后建立 `BootstrapFlow`
- start 顺序：`provider.requiredCheck()` → `provider.start()`
- 全部 provider start 后统一 `notifyAfterCompleted()`

## 二、ModuleDefine.prepare
- 用 `ServiceLoader<ModuleProvider>` 按 provider.name + provider.module() 匹配
- 同一 module 命中多个 provider → `DuplicateProviderException`
- 一个 module 找不到 provider → `ProviderNotFoundException`
- ConfigCreator 非空时：反射创建 ModuleConfig、copy YAML properties、回调 onInitialized
- 最后调用 provider.prepare()

## 三、ModuleProvider
- provider 生命周期：prepare → start → notifyAfterCompleted
- `requiredModules()` 声明模块依赖
- `registerServiceImplementation` 校验 serviceType.isInstance(service)
- `requiredCheck` 做双向校验：
  - ModuleDefine 要求的 service 必须全部实现
  - 实现数量不能多于 ModuleDefine 要求数量
- `getService` 找不到实现直接抛 `ServiceNotProvidedException`

## 四、BootstrapFlow
- 先校验 requiredModules 是否都 loaded
- 迭代构造拓扑启动序列
- 无依赖 provider 先进入
- 所有依赖已进入 startupSequence 后才能进入当前 provider
- 一整轮无 provider 被移除 → `CycleDependencyException`
- 依赖排序结果来自 `loadedModules.values()`，其初始迭代顺序受 HashMap 影响；依赖关系正确性不受影响，但同层 provider 的启动顺序不应被当成稳定契约

## 五、SPI 与协议边界
- 生产模块通过 `META-INF/services/ModuleDefine` 与 `ModuleProvider` 注册
- 配置 module/provider 名称与 ServiceLoader 可见实现必须同时匹配
- SW-1 command model 的消费方由 OAP server-core/receiver provider 承接；SW-2 只分析 module/SPI/bootstrap，不吞并 command 业务

## 六、验证状态
- 官方 `ModuleManagerTest` 覆盖：
  - 正常 init
  - ModuleConfig transport
  - module missing
  - cycle dependency
- 当前运行 library-module 单测被 Maven 依赖解析阻塞：本地未安装 `library-util:10.4.0`，不是测试断言失败
- Pass 0/1/2 已完成源码闭环；harness 暂不伪造，待补全本地 reactor 构建依赖后验证