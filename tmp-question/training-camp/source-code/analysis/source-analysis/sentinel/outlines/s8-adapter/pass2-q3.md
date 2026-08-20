# Pass 2 闭环笔记 Q3: SentinelResourceAspect 的入口、异常与 fallback

## 验证过程

- 切面解析目标方法和 `@SentinelResource`，资源名优先用 annotation value，否则用方法名，读取 `entryType/resourceType` (`SentinelResourceAspect.java:37-49`)。
- 正常路径：`SphU.entry(resourceName, resourceType, entryType, pjp.getArgs())` → `pjp.proceed()`。
- `BlockException` 先走 `handleBlockException`：优先调用 `blockHandler`，没有则回退到普通 fallback (`SentinelResourceAspect.java:50-53`, `AbstractSentinelAspectSupport.java:92-113`)。
- 业务异常路径：先检查 `exceptionsToIgnore`，命中则原样抛出；命中 `exceptionsToTrace` 才 `Tracer.trace` 并执行 fallback，否则原样抛出 (`SentinelResourceAspect.java:54-68`)。
- finally 中只要 Entry 创建成功就 `entry.exit(...)`，保证 entry/exit 配对 (`SentinelResourceAspect.java:69-73`)。
- fallback 方法通过 `ResourceMetadataRegistry` 缓存反射解析结果，支持同签名或追加 Throwable 参数。

## 结论

AspectJ 适配器把注解元数据翻译成 SphU entry；BlockException 与业务异常分两条处理链：blockHandler 优先处理规则拒绝，业务异常按 ignore/trace 配置决定是否统计与 fallback。Entry 的 exit 在 finally 统一完成。