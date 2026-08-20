# 闭环笔记 q2: scope 三态 + 本地导出

## 假设
导出范围由 scope 配置决定: 本地 (injvm) 始终/远程 (registry) 按需。

## 验证过程
- **scope 三态** (exportUrl L650-690): **SCOPE_NONE → 不导出** / **SCOPE_LOCAL → 仅本地** / **SCOPE_REMOTE → 仅远程** / 默认 → 双导出
- **exportLocal** (L1010-1020): **LOCAL_PROTOCOL (injvm) + LOCALHOST + port 0** + EXPORTER_LISTENER — 本地引用通道 (同 JVM 调用)
- **exportRemote** (L730-770): 遍历 registryURLs → **doExportUrl(registryURL + EXPORT_KEY 属性)**; **EXT_PROTOCOL 附加协议** (IS_PU_SERVER + IS_EXTRA 标记); **MetadataUtils.publishServiceDefinition** (元数据中心发布)
- **isOnlyInJvm** (L1025-1030): 单协议且 injvm → 只本地
- **DYNAMIC_KEY 继承** (L745): 注册动态性继承注册中心配置

## 代码类型
Implementation (scope 面)

## 跨域关联
- D-5: registryURL 消费 (注册)
- D-4: injvm 本地调用 (in-process)

## 结论
scope 三态 + 本地始终可导 (injvm port 0) + 远程按 registry 遍历 + 元数据发布。
源码位置: ServiceConfig.java:650-690,730-770,1010-1030
