# F-2 Contract 注解解析 — 时空溯源 (CHANGELOG 实证, git shallow)

> git shallow (1 commit) 无法 checkout 早期版本; 溯源以 CHANGELOG.md + 代码内注释为锚 (方法论铁律 4)。

## 版本演进链 (CHANGELOG.md 实证)

| 版本 | 事件 | 证据 |
|:--:|:--|:--|
| 10.5 | **Declarative contracts (#1060)** — Contract 从"过程式解析"重构为"声明式注册表"; DeclarativeContract/BaseContract 分层定型 | CHANGELOG.md:46 |
| 10.5 | Spring4 contract (#1069) — 同期 Spring 契约模块化 (归 5.6) | CHANGELOG.md:45 |
| 10.6 | "Add composed Spring annotations support (#1090)" — 组合注解支持 (Spring 面) | CHANGELOG.md:44 |
| 10.8 | "async feign variant (#1174)" — CompletableFuture 异步面 → MethodInfo 异步返回类型检测的动因 (F-6 关联) | CHANGELOG.md:10 |
| 13.14 | "Add support for the HTTP QUERY method (RFC 10008)" — HttpMethod 枚举新增 QUERY; DefaultContractTest httpMethods 含 query 断言 (L63) | CHANGELOG.md:1-3 |

## 代码内历史锚

- **10.5 分水岭**: Contract.Default (旧实现) 整体 @Deprecated (Contract.java:252-253, class Default extends DefaultContract) → DefaultContract 继承 DeclarativeContract; BaseContract 内保留旧版 Javadoc 痕迹 ("Called by parseAndValidateMetadata twice")
- **kotlin 特判**: "kotlin.coroutines.Continuation".equals(parameterTypes[i].getName()) (Contract.java:132-133) — Kotlin 协程挂起函数兼容 (10.x 期间加入, 字符串比较防编译期依赖)
- **告警机制**: addWarning 累积 + 异常拼接 — 10.5 重构引入"编译器式诊断"的痕迹 (warnings() 贯穿所有 checkState 消息)
- **MethodInfo @Experimental**: MethodInfoResolver (23 行) 与 MethodInfo (53 行) 均为 @Experimental — 异步返回类型解析仍在演化 (13.x 现状)

## 版本相关性结论

- **契约面分水岭 = 10.5**: 之前 Contract 是单一大类 (processAnnotationOn* 全在一个类), 之后 DeclarativeContract 注册表化 — 扩展契约从"改解析代码"变为"注册处理器" (Spring/JAX-RS/GraphQL 契约都是注册表消费者)
- **13.x 无契约面重大变更**: 13.12 CollectionFormat 扩展到 form (F-5 面), 13.14 QUERY 方法 — 契约骨架 (BaseContract 主循环) 自 10.5 定型后稳定
- **MethodInfo 是异步演化的契约层投影**: 10.8 异步客户端 → 契约需识别 CompletableFuture 包装 → MethodInfo 解包 (13.x @Experimental)
- **参数名 -parameters 提示**: 编译期 flag 依赖 — JDK8 后标准路径, 提示信息 (DefaultContract.java:101-108) 是长期演进产物
