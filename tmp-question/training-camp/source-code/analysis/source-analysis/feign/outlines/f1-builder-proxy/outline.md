# F-1 Builder 门面 + 动态代理 — 一行 target() 背后: 装配、代理与 CRTP 泛型
> **ℹ 8-16 增强版 (备查)**: 本文件为 F-1 的 8-16 深化版, 独有内容 (configKey 规则/verify 四检查/equals 语义/MethodHandle 三路 Lookup/代理三分派) 已并入权威版 f1-builder (§5/§6) 与 f3-proxy (§1)。保留作增强参考, 非权威。

> 前置: [[F-5-URI模板]] + [[F-2-Contract]] + [[F-4-编解码]] + [[F-3-Client执行]] (全部已交付) | 引出: [[F-6-异步]] (AsyncBuilder 共享 BaseBuilder) + 阶段 5.6 OpenFeign (FactoryBean 消费) | 对照: MyBatis MapperProxy + Spring FactoryBean
> 🔴 A | 方案 A (全深度) | 闭环: q1(build 三阶段) q2(代理四步) q3(Capability 反射增强) q4(configKey)
> Pass 2 闭环: q1(internalBuild 装配) q2(newInstance 四步) q3(enrich 字段级) q4(FeignInvocationHandler 分派)

**读者处境**: `Feign.builder().encoder(new GsonEncoder()).target(GitHub.class, "https://api")` 一行怎么变成能调用的代理? 20 个配置方法怎么"记住"设置? Capability 怎么让第三方增强任意组件? default 方法怎么被代理?

### 1. 门面与 configKey — Feign 抽象类的最小面

场景: `Feign.builder()` 与 `Feign.configKey()` 是什么关系?
源码路径:
- Feign 抽象类 (Feign.java:34): 静态 builder() (L36) + **抽象 newInstance(Target)** (L95) — 门面只暴露两个核心操作
- **configKey 格式** (Feign.java:69-84): `SimpleName#method(ParamType1,ParamType2)` — Types.resolve 泛型解析 + getRawType().getSimpleName (L74-78); 无参数方法去尾逗号 (L80-82)
- configKey 用途: F-2 MethodMetadata.configKey + F-4 ErrorDecoder.methodKey (跨域一致键)
关键设计 (q4): **configKey 是"契约-执行-错误"三层的共享主键** — 注解解析 (F-2)、重试日志 (F-3 logRetry)、错误映射 (F-4 decode) 全用它关联; 格式不可变 (ErrorDecoder Javadoc 依赖)。 [模式: 全局标识符]

### 2. Builder 双链 — BaseBuilder 泛型 CRTP 与 Feign.Builder 覆写

场景: 20+ 配置方法怎么既链式又类型安全?
源码路径:
- **BaseBuilder\<B extends BaseBuilder\<B, T\>, T\> implements Cloneable** (BaseBuilder.java:41) — **CRTP 泛型**: thisB() 返回 B (L64-66), 子类方法覆写返回自身类型
- Feign.Builder extends BaseBuilder<Builder, Feign> (Feign.java:97): **21 个覆写方法**全部 `return super.xxx()` 保协变返回 (L101-214)
- Builder 特有字段: `client` (Feign.java:98) — BaseBuilder 无此字段 (client 是 Builder 层概念)
- BaseBuilder 15 字段面 (L43-61): 双拦截器列表×3 / contract / retryer / logger / encoder / decoder / queryMapEncoder / errorDecoder / options / invocationHandlerFactory / dismiss404 / propagationPolicy / capabilities
关键设计 (q1): **CRTP 让链式调用返回子类类型** — `Feign.builder().encoder(x).client(y)` 最后调用 target() (Builder 特有); AsyncBuilder 复用同一 BaseBuilder (F-6 引出的前提); clone() 是 Capability 增强的基础 (enrich 用克隆不污染原 Builder)。 [模式: CRTP 泛型]

### 3. build() 三阶段 — enrich → internalBuild 的装配流水线

场景: 20 个配置怎么变成完整的 ReflectiveFeign?
源码路径:
- **build() = enrich().internalBuild()** (BaseBuilder.java:385-388); internalBuild 抽象 (L389)
- **internalBuild 装配** (Feign.java:217-231): ① ResponseHandler 8 参 (含 responseInterceptorChain()) ② **SynchronousMethodHandler.Factory 10 参** (client/retryer/双拦截器/responseHandler/logger/logLevel/propagationPolicy/RequestTemplateFactoryResolver/options) ③ ReflectiveFeign 4 参 (contract/methodHandlerFactory/invocationHandlerFactory/AsyncContextSupplier)
- ResponseMappingDecoder (Feign.java:234-240): mapAndDecode 配置的响应映射 (ResponseMapper 先映射再 decode)
关键设计 (q1): **装配是"参数对象"而非"设置器"** — 全部组件打包进 MethodHandlerConfiguration (F-3) / Factory 构造器; internalBuild 是唯一装配点, 子类 (AsyncBuilder) 覆写它产出不同 Feign (F-6)。 [模式: 装配工厂]

### 4. Capability — 反射字段级增强与"逐元素+整体"双增强

场景: 第三方怎么增强任意组件而不用改 Feign?
源码路径:
- Capability.enrich (Capability.java:38-56): **反射调用** — 遍历 capability 类方法, 按 `method.getName().equals("enrich")` + **返回值类型可赋值**匹配 (L58-73), reduce 链式 (cap3(cap2(cap1(x))))
- enrich() (BaseBuilder.java:265-362): **capabilities 空 → 短路返回 thisB() 不克隆** (L266-268); 否则克隆 → getFieldsToEnrich 反射字段扫描 → **List 字段按泛型 ownerType 逐元素增强** (L276-286) / 非 List 整体增强; **responseInterceptors 空时构造默认拦截器再增强** (L303-310, 让 Capability 可给"不存在的拦截器"注入)
- getFieldsToEnrich 白名单 (L366-383): **8 个排除条件** — synthetic 字段 / capabilities / 三拦截器列表 (request/response/method, 逐元素+整体增强故整体排除) / executorService (调用方生命周期资源) / primitive 类型 / **enum 类型 (logLevel+propagationPolicy 两个枚举字段, L46/L60)**
- **拦截器三列表双增强** (L285-331): Request (L285-294) / Response (L298-314) / Method (L316-331) 各"逐元素 enrich + 整体 MethodInterceptors/ResponseInterceptors/RequestInterceptors 包装 enrich"; 响应链也整体增强 (responseInterceptorChain L391-402)
关键设计 (q3): **Capability 是"无接口侵入"的扩展点** — 通过反射匹配 enrich 方法而非接口实现; 返回值类型是匹配键 (能增强 Client 的 Capability 与增强 Contract 的互不干扰); 排除列表保护生命周期资源 (executorService 不该被替换)。 [模式: 反射增强]

### 5. newInstance — 代理创建四步与 Target 验证

场景: target() 之后代理怎么生成?
源码路径:
- Builder.target 双签名 (Feign.java:203-210): Class+url → HardCodedTarget / Target 直传 → build().newInstance(target)
- **ReflectiveFeign.newInstance 四步** (ReflectiveFeign.java:50-68): ① TargetSpecificationVerifier.verify ② ParseHandlersByName.apply ③ factory.create ④ **Proxy.newProxyInstance + DefaultMethodHandler.bindTo 循环**
- TargetSpecificationVerifier (ReflectiveFeign.java:173-203): **四检查** — ① 接口校验 ② **同步方法任意返回类型跳过** (非 CF 可赋值 continue, L183-185) ③ **CF 子类拒绝** (retType != CompletableFuture.class 抛, L187-189 — 必须精确 CF) ④ CF 非参数化/Wildcard 泛型拒绝 (L192-201); 同步/异步代理统一入口
- HardCodedTarget (Target.java:66-101): type/name/url 三要素 + apply 加 base url
关键设计 (q2): **verify 前置失败** — 接口非接口/CF 返回非法在代理创建前抛, 不等到调用期; apply 一次性注入 base url (F-3 targetRequest 调用 Target.apply)。 [模式: 前置校验]

### 6. ParseHandlersByName — 方法 → MethodHandler 映射

场景: 接口 20 个方法怎么映射到 20 个 handler?
源码路径:
- ParseHandlersByName.apply (ReflectiveFeign.java:137-157): **metadataList 遍历 → createMethodHandler** → LinkedHashMap<Method, MethodHandler>
- Object 方法跳过 (L139-141); **isIgnored → 抛 IllegalStateException 的 handler** (L163-167, F-2 isIgnored 联动)
- **default 方法补充循环** (L151-156): Util.isDefault → DefaultMethodHandler — Contract (F-2) 跳过的 default 在此补回!
- createMethodHandler → factory.create (L170) — SynchronousMethodHandler.Factory (F-3) 或异步 Factory (F-6)
关键设计 (q2): **Contract 跳过 default, ParseHandlersByName 补回** — 分工: Contract 只管 HTTP 注解方法 (F-2 四过滤), 代理层把 default 方法用 MethodHandle 直调; 这是"注解方法 + default 方法"双路径的装配点。 [模式: 双路径装配]

### 7. FeignInvocationHandler — equals/hashCode/toString 三分派

场景: 代理上调用 equals/hashCode 会不会发 HTTP?
源码路径:
- FeignInvocationHandler (ReflectiveFeign.java:75-105): **三分派** — equals (L87-93) / hashCode (L95-96) / toString (L97-98) 直调对象语义; 其他方法 → dispatch.get(method).invoke(args)
- **equals 特殊**: `Proxy.getInvocationHandler(args[0])` 取对端 handler (L89-91), 非代理/异常 → false (L92-93); 对端是 FeignInvocationHandler → **target.equals(other.target)** (L111-117) — HardCodedTarget.equals 比较 **type+name+url 三要素** (Target.java:109-114, hashCode 17×31 组合 L116-120)
- dispatch 无此方法 → UnsupportedOperationException (L99-101)
- 代理 equals 语义: **同 target 即相等** (跨代理实例)
关键设计 (q2): **Object 三方法不触发 HTTP** — 否则日志框架调 toString 会发请求; equals 按 target 语义 (缓存/集合去重场景); 未映射方法显式报错而非静默。 [模式: 代理对象语义]

### 8. DefaultMethodHandler — MethodHandle 直调 default 方法

场景: 接口 default 方法怎么"原样执行"?
源码路径:
- DefaultMethodHandler (DefaultMethodHandler.java:44-50): **unreflectSpecial(defaultMethod, declaringClass)** (L45) — MethodHandle 绑定接口实现
- **三路 Lookup** (L54-62): safeReadLookup → androidLookup → legacyReadLookup (Java 9+ 模块封装兼容, 对照 F-2)
- bindTo (L128-133): handle = unboundHandle.bindTo(proxy) — newInstance 第四步循环调用
- invoke → handle.invokeWithArguments (L137+)
- **历史注释** (L28-37): "Java 7 MethodHandle" 旧注释 + @IgnoreJRERequirement — 长期演进痕迹
关键设计 (q2): **MethodHandle 而非反射 invoke** — unreflectSpecial 绑定接口默认实现, bindTo 注入 proxy 作为 this; default 方法可访问接口私有状态 (若 Java 9+); 与 F-2 Contract 跳过的 default 方法闭环。 [模式: MethodHandle 直调]

## 代码类型
Architecture (装配核心) + Metaprogramming (反射增强 + 动态代理)

## 负面空间 — Builder/代理刻意不做的事

- **不做 Builder 线程安全**: 配置期非线程安全 (build 后不可变), 对比 Spring 的线程安全 BeanFactory
- **不缓存 newInstance 结果**: Javadoc 明示 "You should cache this result" (Feign.java:88-89) — 每次 target() 重新反射
- **不做代理缓存**: 每 target 新代理, 无池化 (对比 JDK Proxy 缓存)
- **不增强 final 类**: 只能代理接口 (verify 强制 isInterface), 无 CGLIB
- **不做方法拦截 AOP**: MethodInterceptor 只包 HTTP 调用, 非任意方法 (对比 Spring AOP)
- **不做 Builder 序列化**: build 后不可逆, 无配置持久化 (对比 Boot ConfigurationProperties)

→ 引出: 异步版怎么复用这套装配? → F-6 异步客户端
