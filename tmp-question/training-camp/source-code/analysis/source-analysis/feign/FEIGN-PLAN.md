# Feign — 知识网络化规划 (F-1~F-6, 09 怀疑审计后 v1)

> **日期**: 2026-08-15 | **依据**: issue/源码分析执行计划.md 阶段5.1 (5 域) + 09 对既有规划保持怀疑 全量重审
> **源码**: `/data/workspace/source-code/code/spring/feign` (**OpenFeign 13.14-SNAPSHOT**, pom.xml 实证; core 模块 90 主源文件 + 52 测试; 版本与执行计划规划期 (9.x/10.x) 存在**代际差异**)
> **定位**: 阶段 5.1 — RPC 与服务治理第一环 **声明式 HTTP 客户端 (Java→HTTP binder, 受 Retrofit 启发)**
> **知识网络**: 与阶段 5.2 Dubbo (代理/RPC 对照) + 5.3 gRPC (客户端生成对照) + 5.6 OpenFeign (Spring 封装消费 F-1~F-6) 互联

---

## 〇、09 怀疑审计表 (Feign, 2026-08-15) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 5 个** (F-1~F-5) | core 包扫描 (90 文件) | 未覆盖: **template/ (10 文件, RequestTemplate 1119 行模板引擎)**、interceptor/ (MethodInterceptor 98 + Invocation 105, 13.x 新扩展点)、AsyncFeign 族 (763)、auth/ (BasicAuth) | **修正** ⚠ 新增 **F-6 模板引擎** (定义特征: README "processes annotations into a **templatized request**"); MethodInterceptor/ResponseInterceptor/Capability 并入 F-5; 异步面并入 F-3 |
| F-1 "Feign Builder—Contract/Encoder/Decoder/Client/Logger" | Feign.java + BaseBuilder.java | **13.x 重构**: Feign.Builder extends **BaseBuilder** (泛型自引用 CRTP, BaseBuilder.java:41) — 配置字段 14+ (L43-59); internalBuild() 装配 ResponseHandler + SynchronousMethodHandler.Factory + ReflectiveFeign (Feign.java:217-242) | **接受+代际修正** ✅ |
| F-2 "Contract—@RequestLine/@Param/@Headers/@Body→MethodMetadata" | Contract.java + DefaultContract.java | **13.x 重构**: **DeclarativeContract (267)** 新解析架构 + **MethodInfoResolver (23)/MethodInfo (53)**; DefaultContract (153) 现继承 DeclarativeContract; @RequestLine/@Param/@Headers/@Body 仍为核心注解 | **接受+代际修正** ✅ |
| F-3 "ReflectiveFeign→FeignInvocationHandler→SynchronousMethodHandler" | ReflectiveFeign.java | FeignInvocationHandler 存在 (ReflectiveFeign.java:75 内部类); SynchronousMethodHandler (226) + **ResponseHandler (98, 13.x 新抽象)**; 异步面 AsyncFeign (285)/AsynchronousMethodHandler (322) 未在规划内 | **接受+补充** ✅ (异步面并入) |
| F-4 "Encoder/Decoder—Jackson/Gson/SpringEncoder/ResponseEntityDecoder" | codec/ 13 文件 | 接口族存在 (Encoder 89/Decoder 92/ErrorDecoder 135/JsonCodec); Jackson/Gson 是独立模块 (core 无依赖) | **接受** ✅ (实现族在各模块, core 只讲接口面) |
| F-5 "RequestInterceptor—BasicAuthRequestInterceptor/ForwardedForInterceptor" | interceptor/ + auth/ | RequestInterceptor 存在; **13.x 新增 MethodInterceptor (98)/ResponseInterceptor** — validation/http-cache 模块的扩展点 (AGENTS.md 实证); BasicAuthRequestInterceptor 在 auth/ (2 文件) | **接受+补充** ✅ (13.x 三拦截器体系) |
| 版本 | pom.xml | **13.14-SNAPSHOT** (规划期无版本声明; 规划基于 9.x/10.x 认知 — RequestTemplate 1119 行是 10.x 模板重构产物) | **补充** ✅ |
| 来源 | README | 受 **Retrofit** 启发 (README 原话); Netflix 开源 | **补充** ✅ |
| git 历史 | git log | **浅克隆单提交** — 时空溯源改用 CHANGELOG.md (13.14→10.7 全版本条目) | **补充** ✅ |
| async 面 (AsyncFeign 285/AsyncClient 79, 未规划) | 设计决策测试 | CompletableFuture 异步调用链 — 承载设计决策但为 Feign 主路径的变体 (SynchronousMethodHandler 的同构) | **并入 F-3** ✅ |
| template/ (10 文件, 未规划) | 设计决策测试 (00 §3) | **RequestTemplate (1119) + Template/Expressions/UriTemplate/QueryTemplate 族** — URI 模板表达式解析 (RFC 6570) 是"注解→模板化请求"的实现核心 (README 定义特征原话) | **新增 F-6** ⚠ 🔴 |

**覆盖率报告**: 既有规划 5 域 → 重审后 **6 域** (120%, +1 解释: template 模板引擎通过定义特征测试 — "templatized request" 是 Feign 的立身之本)。修正 **3 处** (Builder 13.x CRTP/契约 DeclarativeContract/三拦截器体系), 补充 **4 处** (版本 13.14/来源 Retrofit/CHANGELOG 时空溯源/异步面归属)。

---

## 一、入口点与主线

`Feign.builder() (Feign.java:36-38) → BaseBuilder 配置 (14+ 字段) → internalBuild (L217-242) → ReflectiveFeign (212) → newInstance(Target) → JDK 动态代理 (FeignInvocationHandler, ReflectiveFeign.java:75) → SynchronousMethodHandler (226) → RequestTemplate (1119, 模板引擎 F-6) → Encoder (F-4) → Client → Response → Decoder (F-4) → ResponseHandler (98) → 用户对象` — 契约面: `Contract (254) → DeclarativeContract (267) → MethodMetadata (269)` — 拦截面: `RequestInterceptor/MethodInterceptor/ResponseInterceptor (F-5)`。

## 二、域清单 (6 域: 4🔴 + 2🟡)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| F-1 | **Builder 装配** | Feign.java (260) + BaseBuilder (402) + Capability | CRTP 泛型自引用 Builder/14+ 配置字段默认值/internalBuild 装配链/Capability 扩展 | 🔴 A |
| F-2 | **契约解析** | Contract (254) + DeclarativeContract (267) + DefaultContract (153) + MethodInfoResolver (23) + MethodInfo (53) + MethodMetadata (269) | @RequestLine/@Param/@Headers/@Body → MethodMetadata 流水线/继承解析/注解族 | 🔴 A |
| F-3 | **代理与调用链** | ReflectiveFeign (212) + InvocationHandlerFactory + SynchronousMethodHandler (226) + ResponseHandler (98) + 异步族 (763) | JDK 代理/方法分发/请求执行链/重试/异步变体 | 🔴 A |
| F-4 | **编解码** | codec/ (13 文件: Encoder 89/Decoder 92/ErrorDecoder 135/JsonCodec 族) | 编码/解码接口/ErrorDecoder 异常映射/默认实现 | 🟡 B |
| F-5 | **拦截器链** | RequestInterceptor + interceptor/ (MethodInterceptor 98/Invocation 105) + ResponseInterceptor + auth/ (2) | 三拦截器体系/请求生命周期/认证拦截器 | 🟡 B |
| F-6 | **模板引擎** (新增) | RequestTemplate (1119) + template/ (10 文件: Template 351/Expressions 295/UriTemplate/QueryTemplate/HeaderTemplate) | URI 模板表达式/RFC 6570/参数替换/占位符 | 🔴 A |

## 三、执行顺序 (拓扑: 模板 → 契约 → 编解码 → 拦截 → 执行 → 装配)

**F-6 → F-2 → F-4 → F-5 → F-3 → F-1**

> 拓扑理由: 模板引擎 (F-6, 叶子, 一切请求的载体) → 契约解析 (F-2, 注解→MethodMetadata, 产出含模板表达式) → 编解码 (F-4, 独立接口面) → 拦截器 (F-5, 独立接口面) → 代理与调用链 (F-3, 汇聚 F-2 元数据 + F-6 模板 + F-4 编解码 + F-5 拦截器) → Builder 装配 (F-1, 依赖全部的收束 Hub)。

## 四、知识网络图

```
← 复用: Spring (阶段2 IoC/AOP) + Netty (阶段1 对照) 
→ 引出: 阶段5.6 OpenFeign (Spring MVC 契约/FactoryBean 消费 F-1~F-6) + 阶段5.2 Dubbo (代理对照)
```

## 五、完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (5→6, +1 说明)
- [x] 09 审计: 修正 3 处 (Builder CRTP/DeclarativeContract/三拦截器), 新增 1 域 (F-6 模板引擎), 补充 4 处
- [x] 数字断言穷举 (90 文件/52 测试/1119 行 RequestTemplate/14+ Builder 字段实证)
- [x] **F-6 ✅ 2026-08-15** (harness MiniTemplate 5/5: {var}解析/嵌套花括号/幂等编码/位置策略/集合格式; 深审 17 项; 30 问; 时空溯源 CHANGELOG) — 8-16 补充: 8-15 版含独有发现 stripCrlf 头注入防御/JSON %7B%7D 还原/alreadyEncoded 废弃
- [x] **F-2 ✅ 2026-08-15** (harness MiniContract 5/5: 角色分配/body推断/formParams/body模板/configKey; 深审 17 项; 29 问)
- [x] **F-1 ✅ 2026-08-15** (方案 A Hub: CRTP/18 默认值/enrich/internalBuild/Capability; harness MiniBuilder 5/5; 深审 12 项) — **8-16 整合补充**: 新增 §5 configKey + §6 Capability 细节 (enrich 短路/List 逐元素/8 排除条件); 增强版 harness MiniBuilder-enhanced (8 场景含 MethodHandle/三分派/verify, 见 harness/f1-minibuilder/)
- [x] **F-3 ✅ 2026-08-15** (方案 A: 代理装配/执行链/重试/响应裁决/异步/Client/Target; harness MiniFeign 5/5; 深审 16 项) — **8-16 整合补充**: verify 四检查/equals 语义/MethodHandle 三路 Lookup (见 f3-proxy review-notes §17-19)
- [x] **F-4 ✅ 2026-08-15** (方案 B: 三接口契约/Default 族/ErrorDecoder/JsonCodec/FeignException/annotation-error-decoder; 深审 9 项)
- [x] **F-5 ✅ 2026-08-15** (方案 B: RequestInterceptor/MethodInterceptor/生命周期对比/ResponseInterceptor/Invocation/Retryer/聚合; 深审 14 项)

> **8-16 整合记录**: 发现 8-16 深化产物 (f5-uri-template/f3-client-execution/f1-builder-proxy) 与 8-15 权威版重复 — 独有内容已并入权威版对应域 (f1-builder §5/§6 + f3-proxy §1), 残留文件标注"深化版备查"保留; FEIGN-PLAN 检查单为 8-15 权威状态。阶段 5.1 Feign **6/6 全量交付** (8-15 权威 + 8-16 深化补充)。
