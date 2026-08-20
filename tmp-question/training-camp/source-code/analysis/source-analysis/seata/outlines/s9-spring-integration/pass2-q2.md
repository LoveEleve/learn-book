# 闭环笔记 q2: 扫描装配 — AbstractAutoProxyCreator + 双集合

## 假设
扫描器基于 Spring AOP 基础设施; 增强目标经集合/检查器双重筛选。

## 验证过程
- **继承** (GlobalTransactionScanner:87-88): **extends AbstractAutoProxyCreator** + CachedConfigurationChangeListener + InitializingBean + ApplicationContextAware + DisposableBean — Spring 代理基础设施
- **模式**: AT_MODE=1 / MT_MODE=2 / **DEFAULT_MODE=3 (AT+MT)** (L92-99); ORDER_NUM=1024
- **wrapIfNecessary** (L307-339): **doCheckers** (PROXYED/EXCLUDE/**FactoryBean 排除** L341-349) → **NEED_ENHANCE_BEAN_NAME_SET** 判定 → **DefaultInterfaceParser.parserInterfaceToProxy** (接口解析 → ProxyInvocationHandler) → **AdapterSpringSeataInterceptor** → 未代理: super.wrapIfNecessary (Spring 代理) / **已代理: findAddSeataAdvisorPosition + addAdvisor 按序插入** (L334-337) — 与既有 AOP 共存
- **双集合**: PROXYED_SET (已代理去重) + NEED_ENHANCE_BEAN_NAME_SET (需增强目标)
- **ScannerChecker 族 3 个** (scannercheckers/): ConfigBeansScannerChecker / PackageScannerChecker / ScopeBeansScannerChecker — 可插拔检查
- **findBusinessBeanNamesNeededEnhancement** (L541-560): BeanDefinition 扫描 (IGNORE_ENHANCE_CHECK_SET + doScannerCheckers) — 构造期预收集目标

## 代码类型
Architecture (扫描装配)

## 跨域关联
- S-4: 数据源代理 (DataSourceAutoProxyCreator 并行面)
- S-1: 拦截器接入模板

## 结论
扫描 = AbstractAutoProxyCreator + 检查器族 + 双集合 + 已代理 bean 按序织入。
源码位置: GlobalTransactionScanner.java:87-99,307-349,541-560
