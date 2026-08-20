# ALI-A9 Seata 分布式事务 — 时空溯源 (2025.0.0.0 实证)

> 仓库为浅克隆单提交 — 演进证据取自代码结构与注释

## 演化主线: 从单路透传到三路全覆盖

| 面 | 机制 | 证据 (2025.0.0.0 源码) | 演进信号 |
|:--|:--|:--|:--|
| Web | SeataHandlerInterceptor preHandle/afterCompletion | web/ (85 行) | bind/unbind + 校验回绑 — 最完整 (收尾校验是后演进特征) |
| RestTemplate | SeataRestTemplateInterceptor + AfterPropertiesSet | rest/ (48+51 行) | InitializingBean 全量注入 — 无注解侵入 |
| Feign | SeataFeignRequestInterceptor + Retryer 接管 | feign/ (39+43 行) | RequestInterceptor + BPP 强制 NEVER_RETRY |
| 内核 | RootContext ThreadLocal | @see io.seata.core.context.RootContext (SeataHandlerInterceptor:32) | XID 持有者外置 |

## 关键事件锚

- **KEY_XID 单一常量** (RootContext.KEY_XID, 三路共用): 透传头名统一 — 演进中未改名的稳定性锚
- **校验回绑** (SeataHandlerInterceptor:65-76): warn "xid in change during RPC" + bind back — 后期修复"线程复用 XID 串线"的强化特征
- **Retryer.NEVER_RETRY** (SeataFeignBuilderBeanPostProcessor): 分布式事务幂等约束 — 与 Feign 原生重试冲突的架构决策
- **RestTemplate 全量注入** (AfterPropertiesSet): 无注解驱动 — 与注解化 (A6) 相反的设计路线 (事务是全局语义)

## 架构递进逻辑

```
早期: 仅 Web 入站 bind (事务上下文建立)
中期: Feign/RestTemplate 出站透传 (跨服务传递)
后期: 收尾校验回绑 + Retryer 接管 (一致性与幂等强化)
```
→ 演进方向: 透传从"单向携带"走向"携带+清理+校验+约束"的完整事务上下文管理。
