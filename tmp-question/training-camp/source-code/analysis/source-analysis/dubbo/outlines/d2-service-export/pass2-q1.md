# 闭环笔记 q1: export 链 — 6 层调用

## 假设
服务导出 = export → doExport → doExportUrls → 1Protocol → exportUrl → doExportUrl 六层。

## 验证过程
- **export()** (L326): synchronized + exported/unexported 标志 + delay 支持 (延迟导出, L350-356 注释)
- **doExportUrls** (L582): **ConfigValidationUtils.loadRegistries** + 遍历 protocols (多协议导出)
- **doExportUrlsFor1Protocol** (L628-655): **buildAttributes (map 组装)** → 去 null → **buildUrl** (protocol 名 → URL) → processServiceExecutor (executor 隔离面) → exportUrl
- **exportUrl** (L650-690): scope 三态决策 (q2)
- **doExportUrl** (L978-993): **proxyFactory.getInvoker(ref, interfaceClass, url)** → **DelegateProviderMetaDataInvoker** → **protocolSPI.export(invoker)** → exporters (CopyOnWriteArrayList, registerType 分组)
- **registerType 修正** (L980-986): REGISTER_KEY=false → MANUAL; NEVER/MANUAL/AUTO_BY_DEPLOYER → URL 加 REGISTER_KEY=false

## 代码类型
Architecture (导出链)

## 跨域关联
- D-1: protocolSPI 自适应 + Wrapper 织入
- D-4: Invoker (服务端 Invoker 面)
- D-5: registryURL (注册面)

## 结论
export = 6 层链 + proxyFactory 生成 invoker + 自适应 Protocol.export + exporters 收集。
源码位置: ServiceConfig.java:326,577-622,628-690,978-993
