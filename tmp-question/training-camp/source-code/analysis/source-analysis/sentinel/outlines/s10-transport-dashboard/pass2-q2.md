# Pass 2 闭环笔记 Q2: CommandHandler 如何注册与分发

## 验证过程

- `CommandHandlerProvider.namedHandlers()` 用 `SpiLoader.of(CommandHandler.class).loadInstanceList()` 取全部 handler，再用 `@CommandMapping` 解析命令名 (`CommandHandlerProvider.java:34-55`)。
- 同时还会加载 `CommandHandlerInterceptor`，根据 `shouldIntercept(name)` 包装成 `InterceptingCommandHandler` (`CommandHandlerProvider.java:37-52`)。
- 没有 `@CommandMapping` 的 handler 会被忽略（命令名为空）。
- 因此命令分发不是手写 switch，而是：SPI 加载 handler → 读 annotation 取命令名 → 构建 `Map<String, CommandHandler>`。
- `CommandRequestExecution` 只是统一执行接口，真正的 HTTP/Netty/SpringMVC 命令中心会把请求解析为 `CommandRequest` 后分发到对应 handler (`CommandRequestExecution.java:18-28`)。

## 结论

CommandHandler 的注册是“SPI + 注解名”的组合：类通过 SPI 暴露，命令名由 `@CommandMapping` 提供，拦截器可按命令名包裹 handler。命令中心本身只做请求解析与 map 分发，不持有业务规则。