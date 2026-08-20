# F-1 Builder 门面 + 动态代理 — 知识规划 (KP)
> **ℹ 8-16 深化版 (备查)**: 本 KP 为同会话 8-16 深化产物, 与权威版 (f6-template/f3-proxy/f1-builder) 重复。内容已并入权威版对应域, 本文件保留作增强参考。

> 域: F-1 | 级别: 🔴 (Hub) | 方案: A | 大纲: outlines/f1-builder-proxy/outline.md (8 节)

## §01 域定位

Feign 装配核心 = 门面 (Feign.builder) + 泛型 Builder (CRTP BaseBuilder) + Capability 反射增强 + 动态代理 (ReflectiveFeign)。Hub 收束域: 全部组件 (F-2 契约/F-4 编解码/F-3 执行/F-5 模板) 在此装配, 同步/异步共享 (F-6 AsyncBuilder 同 BaseBuilder)。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| Feign.java | 260 | 门面 + configKey + Builder + internalBuild | 1,2,3 |
| BaseBuilder.java | 402 | CRTP 泛型 + 15 字段 + enrich 反射增强 | 2,4 |
| ReflectiveFeign.java | 212 | newInstance 四步 + FeignInvocationHandler + ParseHandlersByName + verify | 5,6,7 |
| Capability.java | 155 | 反射增强 SPI | 4 |
| InvocationHandlerFactory.java | 45 | InvocationHandler 工厂 + MethodHandler.Factory\<C\> | 5 |
| DefaultInvocationHandlerFactory.java | 28 | 默认工厂 | 7 |
| DefaultMethodHandler.java | — | MethodHandle 直调 default | 8 |
| Target.java | — | target 抽象 + HardCodedTarget | 5 |

## §05 闭环要点 (Pass 2 内化)

### q1 build 三阶段
build() = enrich().internalBuild() (BaseBuilder.java:385-388); internalBuild 装配 ResponseHandler 8 参 + SynchronousMethodHandler.Factory 10 参 + ReflectiveFeign 4 参 (Feign.java:217-231)。

### q2 代理四步
verify (接口+CF 校验) → ParseHandlersByName.apply (metadata→handler) → factory.create → Proxy.newProxyInstance + DefaultMethodHandler.bindTo。

### q3 Capability 反射增强
enrich 按返回值类型匹配 (Capability.java:38-56) + getFieldsToEnrich 7 类排除 (L366-383) + 拦截器逐元素+整体双增强。

### q4 configKey
`SimpleName#method(Param1,Param2)` (Feign.java:69-84) — F-2/F-3/F-4 共享主键。

## §06 负面空间 (6 条)

不做 Builder 线程安全 / 不缓存 newInstance / 不做代理缓存 / 不增强 final 类 (仅接口) / 不做方法拦截 AOP / 不做 Builder 序列化

## §07 交叉引用

- ← F-5 模板 (RequestTemplateFactoryResolver) + F-2 契约 (contract) + F-4 编解码 (encoder/decoder/errorDecoder) + F-3 执行 (SynchronousMethodHandler.Factory)
- → F-6 异步 (AsyncBuilder 同 BaseBuilder) + 阶段 5.6 OpenFeign (FactoryBean 消费)
- 另见: MyBatis MapperProxy / Spring BeanPostProcessor / Lombok @Builder
