# 闭环笔记 q4: invoker 与元数据 — proxyFactory + Delegate

## 假设
导出的是 invoker 包装: proxyFactory 生成 + 元数据委托包装。

## 验证过程
- **proxyFactory.getInvoker** (doExportUrl L988): **ref (服务实现实例) → Invoker** — JavassistProxyFactory 默认 (L140 注释)
- **DelegateProviderMetaDataInvoker** (L991): 元数据委托包装 — 服务元数据 (提供者模型) 附加
- **protocolSPI** (L188): **Protocol 自适应扩展** — 按 URL protocol 参数选择 (dubbo/rest/tri 等) — **D-1 自适应消费面**
- **Wrapper 织入** (D-1 交叉): protocolSPI 实例经 ProtocolListenerWrapper/ProtocolFilterWrapper 包装 — export 时织入监听器/过滤器
- **exporters 收集** (L992-994): **CopyOnWriteArrayList 按 registerType 分组** — unexport/销毁面索引

## 代码类型
Implementation (invoker 面)

## 跨域关联
- D-1: 自适应 + Wrapper (核心消费)
- D-4: Filter 链 (export 织入)

## 结论
invoker = proxyFactory 生成 + Delegate 元数据包装 + 自适应 Protocol.export (Wrapper 织入) + exporters 分组收集。
源码位置: ServiceConfig.java:188,978-994
