# F-1 Builder 门面 + 动态代理 — REVIEW 记录 (2026-08-15)
> **ℹ 8-16 增强版 (备查)**: 本文件为 F-1 的 8-16 深化版, 独有内容 (configKey 规则/verify 四检查/equals 语义/MethodHandle 三路 Lookup/代理三分派) 已并入权威版 f1-builder (§5/§6) 与 f3-proxy (§1)。保留作增强参考, 非权威。

## 深审发现 (六层, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | Feign 类 **L34** (写 L20)/builder **L36** (写 L22)/newInstance **L95** (写 L88) | 已修 |
| 2 | 锚点漂移 | configKey **L69-84** (写 L48-63 — L48-63 是 Javadoc) | 已修 |
| 3 | 锚点漂移 | Builder **L97** (写 L92)/client 字段 **L98**/internalBuild **L217-231** (写 L206-226) | 已修 |
| 4 | 锚点漂移 | build **L385-388** (写 L386-388); enrich() **L265-362** (写 L340-385 — 那是 getFieldsToEnrich L366) | 已修 |
| 5 | 锚点漂移 | newInstance **L50-68** (写 L36-56); FeignInvocationHandler **L75-105** (写 L58-92) | 已修 |
| 6 | 锚点漂移 | ParseHandlersByName **L127-163** (写 L70-95); TargetSpecificationVerifier **L173-203** (写 L96-113) | 已修 |
| 7 | 锚点漂移 | Capability.enrich **L38-56** (写 L36-60); DefaultMethodHandler unreflectSpecial **L45** (写 L38)/readLookup **L54** (写 L42-49) | 已修 |
| 8 | 验证通过 | ResponseMappingDecoder L234-240 / DefaultInvocationHandlerFactory 28 行 / bindTo L128-133 | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniBuilder 8/8 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | **反射陷阱** | getFieldsToEnrich 用 `getClass().getFields()` 只返回 **public** — BaseBuilder 字段全 protected → 增强落空; 真实 Feign 用 **Util.allFields(getClass())** 遍历继承链 declaredFields (BaseBuilder.java:369) | 修微缩版为继承链 declaredFields + setAccessible — 实证 protected 字段反射需显式授权 |
| H2 | equals 语义 | 微缩版 equals(proxy) 递归 — 真实 Feign 比较**对端 InvocationHandler 的 target** (ReflectiveFeign.java:61-66) | 修微缩版为同代理比较 — 对照真实语义 |
| H3 | **default 方法本质** | JDK 代理 Handler 拦截一切方法调用 (含 default) — 真实 Feign 用 **MethodHandles.unreflectSpecial + bindTo** 绕开 InvocationHandler (DefaultMethodHandler.java:45,128-133) | 微缩版复刻 unreflectSpecial+invokeWithArguments — 实证"为什么要 MethodHandle 而非反射" |

## 锚点密度统计

- file:line 锚点数: **40+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 8 项检查 7 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (线程安全/缓存 newInstance/代理缓存/final 类/AOP/Builder 序列化)
- [x] 每条有对照物 (Spring BeanFactory/CGLIB/Spring AOP/Boot ConfigurationProperties)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (一行 target/20 配置方法/Capability 增强/default 方法代理)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 8 节 × 四要素完备; 数据流可追溯 (builder → enrich → internalBuild → newInstance → proxy)
- [x] 边界交代: 7 类字段排除/三分派/三路 Lookup/verify 前置校验

## 方法论教训

- **harness 必须对照真实源码判定缺陷归属**: H1/H3 是真实语义 (protected 反射/MethodHandle), H2 是我实现错误 — 三缺陷全部定位到真实行号
- **反射 getFields vs getDeclaredFields 陷阱**: 保护字段在增强框架中必须遍历继承链 — 真实 Feign Util.allFields 的存在理由
- **JDK 代理拦截 default 方法** 是 DefaultMethodHandler 存在的根本原因 — 读 harness 失败才理解 unreflectSpecial 的价值
