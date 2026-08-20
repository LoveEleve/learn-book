# ALI-A9 Seata 分布式事务 — 知识规划 (KP)

> 🔴 A | 模块: spring-cloud-starter-alibaba-seata (8 文件, 381 行) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | Feign 透传 | SeataFeignRequestInterceptor:32-37 | getXID → header(KEY_XID), 空则 return |
| 2 | RestTemplate 透传 | SeataRestTemplateInterceptor:40-43 | 包装器 headers.add |
| 3 | Web 入站 bind | SeataHandlerInterceptor:45-52 | 本地空 + rpc 非空 → bind |
| 4 | 收尾 unbind | SeataHandlerInterceptor:59-65 | xid 有 + rpc 有 → unbind |
| 5 | 校验回绑 | SeataHandlerInterceptor:65-76 | 变更 warn + bind back |
| 6 | Retryer 接管 | SeataFeignBuilderBeanPostProcessor:22-25 | Feign.Builder → NEVER_RETRY |
| 7 | 全量注入 | SeataRestTemplateInterceptorAfterPropertiesSet:29-36 | 遍历所有 RT + 复制重设 |
| 8 | Web 装配 | SeataHandlerInterceptorConfiguration | WebMvcConfigurer 注册 |
| 9 | Feign 装配 | SeataFeignClientAutoConfiguration | 拦截器 + BPP |
| 10 | XID 内核 | RootContext (Seata) | ThreadLocal 唯一持有者 |

## 02 高频坑

1. Feign retryer 被强制 NEVER_RETRY (事务幂等)
2. afterCompletion 空 rpcXid 不清理 (防误清)
3. RestTemplate 注入是复制重设 (非直接 add)
4. XID 变更 warn 并回绑原值
5. 本地已有 xid 时不 bind (防止覆盖)
6. 三路共用 KEY_XID 常量

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 透传 | Feign header / RT header / Web bind — 出站入站闭环 |
| 线程上下文 | RootContext ThreadLocal / bind/unbind / 校验回绑 |
| 生命周期 | preHandle bind / afterCompletion unbind / 线程复用防污染 |
| 装配 | 三 AutoConfiguration / InitializingBean 全量 / WebMvcConfigurer |
| 约束 | NEVER_RETRY 强制 / 无注解侵入 |
| 可观测 | debug 日志链 / warn 变更 |

## 04 跨域桥接

- → Seata 内核: RootContext/GlobalTransactionScanner (XID 生命周期主导)
- → OpenFeign 5.6: RequestInterceptor 体系消费
- ↔ ALI-A6: 三路装配同构, 语义不同 (事务 vs 限流)
- → 面试: "Seata 怎么跨服务传播事务" — XID 三路透传 + 收尾校验
