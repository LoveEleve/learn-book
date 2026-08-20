# G-1 temporal-trace — 时空溯源 (🔴 域)

> 浅克隆无 git 历史 (单提交 8f621c0), 无 CHANGELOG.md — 溯源改用代码内 @since/@ExperimentalApi/@Deprecated 注释 + 生成代码版本标记。

## 演进时间线 (1.0.0 → 1.83.1)

| 版本线索 | 证据 | 意义 |
|---|---|---|
| 1.0.0 | AbstractStub/ClientCalls/MethodDescriptor 全家族 @since 1.0.0 | 三形态 stub 与分派面是**首发设计** (2016), 骨架 10 年未变 |
| 1.1.0 | MethodDescriptor.java:163 (部分 API) | 早期扩展 |
| 1.8.0 | AbstractStub.java:166 (withExecutor 族) | 执行器配置加入链式面 |
| 1.13.0 | ProtoUtils metadataMarshaller (L84) | metadata 二进制 marshaller 补充 |
| 1.16.0 | ProtoUtils setExtensionRegistry (L43) | 扩展注册表 (protobuf 2 兼容时代) |
| 1.26.0 | AbstractStub.java:110,122 (newStub/StubFactory) | **StubFactory 函数式接口引入** — 生成代码从直接 new 转向工厂委托 |
| 1.56.0 | ProtoUtils marshallerWithRecursionLimit (L63) | **递归深度限制加入** — 安全面补强 (深度嵌套 DoS) |
| 1.83.1 | java_generator.cpp 生成代码 "version 1.83.1" (golden L6) | 生成标记随版本 |

## 进行中的演进 (issue 追踪)

| 演进 | 证据 | 状态 |
|---|---|---|
| **BlockingV2 (BlockingClientCall)** | @ExperimentalApi issue 10918 (ClientCalls.java:247,286,299) + compiler BLOCKING_V2_CLIENT_IMPL (java_generator.cpp:585) | **1.83 已进生成器** — V1 Iterator 泄漏 (L224 自述) 的修复路线 |
| withDeadlineAfter(Duration) | @ExperimentalApi issue 11657 (AbstractStub.java:155) | Duration API 现代化 |
| withOption 泛型化 | @ExperimentalApi issue 1869 (AbstractStub.java:205) | CallOptions.Key 扩展 |

## 设计稳定性判断

**核心面 10 年未动**: CRTP 三形态 (1.0.0) + ClientCalls 分派骨架 (1.0.0) + ServerCalls 状态机 (1.0.0) — 稳定架构; **演进都发生在边缘**: 工厂化 (1.26.0 StubFactory)、安全 (1.56.0 递归限制)、API 现代化 (10918 BlockingV2/Duration)。

**对照线索**: BlockingV2 未达稳定 API (仍 @ExperimentalApi) — 写书时以 V1 为主、V2 为"进行中演进"呈现, 符合 1.83.1 的真实状态。
