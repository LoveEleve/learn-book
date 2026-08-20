# SW-2 OAP Bootstrap / Module SPI — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + 官方测试交叉)
- [x] 深读 `ModuleManager` / `ModuleDefine` / `ModuleProvider` / `BootstrapFlow`
- [x] 官方 `ModuleManagerTest` 对照：正常 init、配置 transport、module missing、cycle dependency
- [x] ServiceLoader 文件穷举：ModuleDefine 38 个、ModuleProvider 59 个服务文件
- [x] 生命周期因果链闭环：prepare → config creator → provider.prepare → dependency order → requiredCheck → start → afterCompleted
- [x] DuplicateProvider / ProviderNotFound / ModuleNotFound / CycleDependency / ServiceNotProvided 边界已归类
- [x] 同层 provider 顺序受 HashMap 初始顺序影响，未把它误写成稳定契约

## 构建限制
- `apm-network` submodule 初始化后 Maven compile 成功
- `library-module` 单测当前被外部 Maven 依赖阻塞：`org.apache.skywalking:library-util:10.4.0` 未在本地/aliyunmaven 可解析
- 这是构建环境/本地 reactor 依赖问题，不是 ModuleManager 测试断言失败

## 收敛判定
SW-2 核心源码与官方测试语义已闭环；harness 留待本地 reactor 依赖可解析后补跑，不伪造结果。

## 审查轮次: 第二轮 (2026-08-17, reactor 构建修正/官方测试全量回归)
- [x] 修正构建方法：必须使用 `-am` 同时构建 `library-util`，不能单独运行 library-module
- [x] `./mvnw -pl oap-server/server-library/library-module -am test` 通过
- [x] library-util 50 tests 通过；library-module 5 tests 通过，含 ModuleManager 正常 init/config/missing/cycle
- [x] 重新确认环境阻塞不是源码问题，已从交接结论中删除“当前被阻塞”的表述
- [x] ServiceLoader 数字、依赖排序、requiredCheck 双向校验和生命周期因果链再次复核

## 收敛判定 (第二轮终)
SW-2 无已知源码问题；官方模块测试与完整 reactor 构建均通过。
