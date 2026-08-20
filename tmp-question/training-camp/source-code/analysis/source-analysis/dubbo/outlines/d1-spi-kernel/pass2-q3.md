# 闭环笔记 q3: 自适应面 — @Adaptive + 动态生成

## 假设
自适应扩展 = URL 参数驱动选择; 无注解类时动态生成适配类。

## 验证过程
- **getAdaptiveExtension** (L610-): 双检锁 + cachedAdaptiveInstance + createAdaptiveInstanceError 缓存
- **createAdaptiveExtension** (L640-): getAdaptiveExtensionClass().newInstance + 注入 + init
- **getAdaptiveExtensionClass** (L652-): **cachedAdaptiveClass (@Adaptive 注解类) 优先** — 手工适配类 (如 AdaptiveExtensionFactory); 无 → **createAdaptiveExtensionClass**
- **AdaptiveClassCodeGenerator** (L1467): **动态生成适配类代码 + 编译** — 生成类含 URL 参数提取逻辑 (按 @Adaptive 的 URL 键名 → getExtension(url.getParameter(key)))
- **URL 总线**: 自适应方法签名含 URL 参数 — **URL 是扩展选择的载体** (全框架参数传递)

## 代码类型
Implementation (自适应面)

## 跨域关联
- D-5: RegistryFactory 自适应 (URL protocol 选择 zookeeper/nacos)
- 对照: Java SPI 无自适应 — Dubbo 独有

## 结论
自适应 = URL 参数驱动 + 注解类优先 + 动态代码生成编译; URL 总线贯穿。
源码位置: ExtensionLoader.java:610-660,1467; AdaptiveClassCodeGenerator
