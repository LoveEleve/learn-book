# 01. 一条命令从回车到执行,经过了什么? — 命令注册与执行链

> 🔴 Deep | 37 KP 中的 7 个(命令体系支撑小节)
> 读者处境: 你在 arthas 终端敲下 `watch com.example.Service doBiz`,回车——命令怎么被"找到"、"解析"、"注入参数"然后真正跑起来?

### 1. "不需要注册表的命令" — @Name 注解声明

场景: arthas 有 49 个命令,它们不是在一张 if-else 表里——每个命令类自己"报名字"。

- 命令类声明: `@Name("watch")` + `@Summary` + `@Description`(monitor200/WatchCommand.java:22-23)——注解来自 `com.taobao.middleware.cli.annotations`
- 注册: `BuiltinCommandPack.initCommands()`(command/BuiltinCommandPack.java:48-129)收集命令类 → `clazz.getAnnotation(Name.class)`(:121)取名字 → 过滤 `disabledCommands` → `Command.create(clazz)`(:128)
- 数量: 47 个无条件 + 2 个条件(JFRCommand/ClassLoaderMetaspaceCommand,**JDK 里能找到 `jdk/jfr/Recording.class` 才注册**,BuiltinCommandPack.java:111-118)= 49 个
- 参数声明: `@Argument(index=3, argName="condition-express")`(WatchCommand.java:69)、`@Option(longName="x")` 等——参数元数据也全在注解里
- 匹配器家族(util/matcher/): `WildcardMatcher`(通配 `com.example.*`)/`RegexMatcher`(正则,watch 的 `-E`)/`GroupMatcher`(Or 组合,trace `-p`)/`TrueMatcher`/`FalseMatcher`——类名/方法名匹配全是**匹配器模式**,命令只声明用什么匹配器

关键设计: [模式: 注解驱动/元数据编程(命令=带注解的类)+ 责任链(管道 handler 链)+ 原型(命令实例一次性)] **注解声明式 vs 注册表式**: 新增命令 = 写一个带注解的类,零注册代码(缺点: 启动要扫描注解;优点: 命令自描述,help/解析/校验全部自动生成)。这是框架类项目的经典取舍——"约定优于配置"。

### 2. "从回车到进程" — 分词/Job/查找

场景: 命令名、参数、管道符混在一行字符串里,系统怎么拆?

- `ShellLineHandler.handle(line)`(shell/handlers/shell/ShellLineHandler.java:29): `CliTokens.tokenize(line)`(:36)词法切分 → 首 token 特判 shell 内建(exit/logout/jobs/fg/bg/kill,:37-59)→ 其余 `createJob(tokens)`(:62)
- `JobControllerImpl.createJob`(:80)→ `createProcess`(:146): 第一个 text token 查命令 `commandManager.getCommand(token.value())`(:154),查不到抛 `"xxx: command not found"`
- `InternalCommandManager.getCommand`(:36-47): 遍历所有 resolver(**跳过 `ShellInternalCommandResolver`** :38——内建命令不走普通命令链),按 `command.name()` 匹配(:123-135)
- **管道**: 同一行里 `findLastPipe`(:137,调用 :56)按 `|` 切段,每段一个 Process,后段消费前段的 stdoutHandlerChain(GrepHandler 等)

关键设计: **Job 模型**——一次回车 = 一个 Job(可后台/可结束),Job 里可含多个管道 Process。命令查找与执行分离: 查找只认名字(注册表语义),执行才解析参数——所以"命令不存在"的错误在 createProcess 就报,参数错误在 ProcessImpl 才报。

### 3. "参数注入的魔法" — CLI 解析与注解注入

场景: 你敲的 `-x 3 -n 5` 怎么变成 WatchCommand 对象里的字段?

- `ProcessImpl.run()`(shell/system/impl/ProcessImpl.java:315): `cli().parse(args2, false).isAskingForHelp()`(:351,检测 --help)→ `cli().parse(args2)`(:357)得 `CommandLine`
- 异步执行(:370-371): `ArthasBootstrap.getInstance().execute(new CommandProcessTask(process))`——命令跑在 arthas 自己的线程池(AR-1 构造器创建的 executorService)
- `AnnotatedCommandImpl.process()`(shell/command/impl/AnnotatedCommandImpl.java:73-86): `clazz.newInstance()` + **`CLIConfigurator.inject(process.commandLine(), instance)`(:81)**——把 CommandLine 按注解元数据反射注入命令实例字段 → `instance.process(process)`
- [Java: 反射注入的本质——@Option/@Argument 上的名字与 CommandLine 的值匹配,类型转换由注解框架完成(默认值、必填校验都在这一步)]

关键设计: **命令实例是"一次性"的**: 每次执行 new 一个全新实例再注入——天然无状态并发安全,多个终端同时跑同一个命令互不干扰。参数校验错误在这层抛(如 watch 的 `-M` sizeLimit 校验,WatchCommand.java:222)。

### 4. 链路总览

```
"watch -x 3 com.example.Service doBiz" (一行字符串)
  → ShellLineHandler.tokenize          → tokens
  → JobController.createProcess         → 按 "watch" 查命令(InternalCommandManager)
  → ProcessImpl.run                     → cli().parse → CommandLine
  → AnnotatedCommandImpl.process        → CLIConfigurator.inject → WatchCommand 实例
  → WatchCommand.process(process)       → 真正的命令逻辑(下一篇: 增强)
```

关键设计: 这条链把"字符串 → 对象 → 逻辑"分层——**shell 层(分词/Job/管道)与命令层(注解/执行)解耦**: 管道、重定向、后台执行对命令完全透明,命令只关心"process 参数里有什么"。

---

跨域桥: 命令执行后进入 `WatchCommand.process` → 下一篇(EnhancerCommand 模板与增强);shell 内建命令与 49 命令的区别(ShellInternalCommandResolver)= 本篇 §2;AR-0 的 `help` 输出 = 注解元数据自动生成。

---

**OpenJDK 关联**:  [OpenJDK 域 35 DCmd — outlines/35-dcmd/] — JVM 自己的诊断命令框架(jcmd),与 arthas 命令体系是两种实现路线的对比。

### 核心悬念

**"命令找到了、参数注入了——下一步,它怎么把监视代码塞进业务方法?"** — WatchCommand.process 只做一件事: 调 EnhancerCommand 的模板方法。真正的魔法在 enhance() 里——匹配类、过滤、注册 transformer、retransformClasses 热替换。

> → [02-bytekit-enhancer.md](02-bytekit-enhancer.md)