# SW-2 OAP Bootstrap / Module SPI — Pass 0 发现

> 首批深读: `ModuleManager`(98) / `ModuleDefine`(123) / `ModuleProvider`(161) / `BootstrapFlow`(121)
> 日期: 2026-08-17

## 1. 域职责
- SkyWalking OAP 的模块内核：ServiceLoader 发现 module/provider、配置 transport、依赖排序、prepare/start/afterCompleted 生命周期
- 这是所有 receiver/analyzer/storage/query 插件共同依赖的 bootstrap/SPI 层

## 2. ModuleManager.init 主链
1. 从 `ApplicationConfiguration.moduleList()` 取启用模块名
2. `ServiceLoader.load(ModuleDefine.class)` 发现所有 module define
3. 只选择 moduleList 中的 define
4. 调 `ModuleDefine.prepare(...)`
5. `isInPrepareStage=false`
6. 若 moduleList 仍有未匹配名称 → `ModuleNotFoundException`
7. 构建 `BootstrapFlow`
8. `start(this)`：按依赖序调用 provider.requiredCheck + start
9. `notifyAfterCompleted()`：全部 provider 完成后回调

## 3. ModuleDefine.prepare
- 用 `ServiceLoader<ModuleProvider>` 遍历 provider
- 按 configuration provider name + provider.module() 匹配
- 一个 module 只能加载一个 provider；多个命中 → `DuplicateProviderException`
- 找不到 provider → `ProviderNotFoundException`
- 若 provider 有 ConfigCreator：
  - 反射创建 config bean
  - `copyProperties(...)` 注入 YAML module/provider 配置
  - `creator.onInitialized(config)`
- 最后调用 provider.prepare()

## 4. ModuleProvider 契约
- provider 必须声明:
  - `name()`
  - `module()`
  - `newConfigCreator()`
  - `prepare()`
  - `start()`
  - `notifyAfterCompleted()`
  - `requiredModules()`
- provider 在 prepare 阶段只能初始化不依赖其他模块的资源
- start 阶段模块互操作已准备好
- provider 通过 `registerServiceImplementation(...)` 注册服务实现
- `requiredCheck(...)` 双向校验:
  - 要求的 service 必须全部存在
  - 实现数量不能超过 ModuleDefine 声明数量

## 5. BootstrapFlow 依赖排序
- 先验证每个 loaded provider 的 requiredModules 都已加载；缺失 → `ModuleNotFoundException`
- 用迭代式拓扑排序构造 startupSequence
- 无依赖 provider 先进入序列
- 只有所有 required module 已在 startupSequence 中，provider 才加入
- 一轮没有任何 provider 被移除 → `CycleDependencyException`
- start 顺序：provider.requiredCheck → provider.start
- 全部 start 成功后，再统一 notifyAfterCompleted

## 6. SPI 现实结构
- module/provider 主要通过 `META-INF/services` 注册
- 生产仓库存在大量 ModuleDefine/ModuleProvider 服务文件
- 因此“配置中 module name”与“ServiceLoader 可见实现”是双重发现约束
- provider 配置不是普通构造器注入，而是 ModuleDefine.prepare 中 ConfigCreator + YAML copyProperties

## 7. Pass 1 待验证
- Q1: ModuleManager.prepare 阶段 provider 选择是否允许同 module 多 provider 配置，Duplicate 的触发边界
- Q2: BootstrapFlow 拓扑排序是否存在顺序不稳定（HashMap loadedModules）
- Q3: `requiredCheck` 的“实现数量不能超过 services 声明”对多服务 provider 的精确语义
- Q4: provider.prepare/start/notifyAfterCompleted 异常时的部分启动状态与清理策略
- Q5: ServiceLoader 文件实际模块数量与 application moduleList 的关系
- Q6: configuration copyProperties 失败、无 config creator、空 module config 的行为