# Pass 2 闭环笔记 XJ-6: GLUE 模式的真实边界

## 初始假设
- GLUE 模式只是 Groovy 动态脚本执行，和普通 handler 差不多。

## 验证过程
- `GlueFactory` 是入口：
  - 默认实例是 `GlueFactory`
  - `refreshInstance(1)` 可切到 `SpringGlueFactory`
  - `loadNewInstance(codeSource)` 用 `GroovyClassLoader.parseClass(codeSource)` 编译源码，按 MD5 缓存 Class，再 new 原型 handler (`GlueFactory.java:16-64`)。
- `SpringGlueFactory` 覆写 `injectService`，会扫描字段上的 `@Resource` / `@Autowired` / `@Qualifier`，从 `XxlJobSpringExecutor.getApplicationContext()` 注入依赖 (`SpringGlueFactory.java:20-68`)。
- `GlueJobHandler` 不是直接执行源码，而是包装一个真实 `IJobHandler`，在 execute 前写一行 glue.version 日志，再委托 `init/execute/destroy` (`GlueJobHandler.java:11-33`)。
- `ScriptJobHandler` 是另一条路径：
  - 启动时清理旧 script 文件
  - 把 glue source 落盘到 `gluesource/<jobId>_<timestamp>.<suffix>`
  - 通过 `ScriptUtil.execToFile` 运行脚本，并把 stdout/stderr 写入 job log (`ScriptJobHandler.java:16-80`)。
- `ExecutorBizImpl.run` 根据 `glueType` 决定：
  - BEAN / 普通 handler：`MethodJobHandler`
  - GLUE(Java/Groovy 源码)：`GlueFactory` + `GlueJobHandler`
  - Script 类：`ScriptJobHandler`

## 结论

XJ-6 不是单一“动态脚本执行”，而是三层边界：
1. `GlueFactory`：源码编译/缓存/原型实例化
2. `SpringGlueFactory`：Spring 依赖注入增强
3. `GlueJobHandler` / `ScriptJobHandler`：分别处理 JVM 内 handler 包装与外部脚本落盘执行

它与普通 `MethodJobHandler` 的分界必须写清，不能把所有 handler 路径混成一类。