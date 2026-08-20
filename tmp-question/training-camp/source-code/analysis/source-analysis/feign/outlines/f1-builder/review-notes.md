# F-1 review-notes — 六层深审记录 (2026-08-15)

## 审法: 本审源码精读 + 极简复现 harness

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "CRTP class BaseBuilder<B extends BaseBuilder<B, T>, T>" — BaseBuilder.java:41; Feign.Builder extends BaseBuilder<Builder, Feign> (Feign.java:97) | 通过 ✅ |
| 2 | 事实 | §2 "18 字段默认值" — BaseBuilder.java:43-62 穷举 (contract=DefaultContract L47/retryer=DefaultRetryer L48/encoder L50/decoder L51/closeAfterDecode=true L52/queryMapEncoder=FIELD L54/errorDecoder L55/propagationPolicy=NONE L60) | 通过 ✅ |
| 3 | 事实 | §3 "enrich clone 副本 L271 + 逐字段 Capability.enrich L288-354" — BaseBuilder.java:265-389 | 通过 ✅ |
| 4 | 事实 | §3 "build() final = enrich().internalBuild()" — L385-389 | 通过 ✅ |
| 5 | 事实 | §3 "Capability.reduce 流水线" — Capability.java:38-56; 反射 invoke L58-74 | 通过 ✅ |
| 6 | 事实 | §4 "internalBuild 装配" — Feign.java:217-242 (ResponseHandler L218-227/SMH.Factory L228-239/ReflectiveFeign L240-241) | 通过 ✅ |
| 7 | 事实 | §4 "RequestTemplateFactoryResolver 三选一" — L40-48 (formParams/bodyIndex/alwaysEncodeBody) | 通过 ✅ |
| 8 | 事实 | §5 "Capability 全套 enrich" — Capability.java:76-154 (20 个 default enrich) | 通过 ✅ |
| 9 | 事实 | §5 "addCapability 注册" — Feign.java:204-206 | 通过 ✅ |
| 10 | 数字 | "18 个字段" — L43-62 逐行数 = 18 | 通过 ✅ |
| 11 | 结构 | 负面空间 "不做运行时修改" 与 clone 副本装饰自洽 | 通过 ✅ |
| 12 | 过程 | **13.x 代际确认**: BaseBuilder 提取 (旧版 Feign.Builder 全字段内联) — internalBuild 抽象允许异步 Builder 复用配置面 (AsyncFeign.Builder 同继承) | 通过 ✅ |

**结论**: 12 项核对 0 修正。harness 验证 CRTP/默认值/clone 装饰语义。

## harness 设计 (MiniBuilder — Builder 装配极简复现)

- A. CRTP: 链式调用返回自身类型 (编译期验证)
- B. 默认值: 未配置组件用默认
- C. clone 装饰: 副本修改不影响原 Builder
- D. Capability 流水线: 多能力叠加顺序
- E. build 装配: 组件汇聚成"执行器"

---

## 8-16 补充 REVIEW (深化整合 — 独有发现并入)

> 背景: 8-16 深化审查的独有新增发现 (并入大纲 §4/§5/§6), 与 8-15 权威版融合。

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 13 | 事实新增 | **configKey 格式规则** (Feign.java:69-84): `SimpleName#method(ParamType1,ParamType2)` — Types.resolve + getRawType().getSimpleName (L74-78); 无参方法去尾逗号 (L80-82); 三层消费 (F-2 MethodMetadata/F-3 logRetry/F-4 methodKey) | 新增 §5 ✅ |
| 14 | 事实新增 | **enrich() 短路**: capabilities 空 → 返回 thisB() 不克隆 (BaseBuilder.java:266-268); **List 字段按 ownerType 逐元素增强** (L288-354); **responseInterceptors 空时构造默认拦截器再增强** (L303-310) | 并入 §6 ✅ |
| 15 | 事实新增 | **getFieldsToEnrich 8 排除条件** (L366-383): synthetic/capabilities/三拦截器列表/executorService/primitive/**enum (logLevel+propagationPolicy L46/L60)** — 大纲原写 7 类, 修正 8 | 修正 ✅ |
| 16 | 数字 | Capability 20 个 default enrich (L76-154) 与 8-15 记录一致 | 通过 ✅ |
| 17 | 结构 | 新增 §5 configKey 后 f1-builder 为 6 节, 负面空间/代码类型不变 | 通过 ✅ |

---

## 8-16 深度 REVIEW 第三轮 (07 维度 1/3/4/5: 叙事桥 + 前向引用 + 横切覆盖 + 负面空间/开篇)

| # | 维度 | 发现 | 处置 |
|---|---|---|---|
| 18 | 桥 (维度1) | **f2-contract 前置含 Z-7-ClientAPI 死链** (ZK 阶段域, 与 Feign 无依赖) | 移除, 前置只剩 F-6 ✅ |
| 19 | 桥 (维度1) | **f6-template 前置含 Z-7-ClientAPI 死链** (模板引擎与 ZK 无关) | 改为"无 (叶子域)" ✅ |
| 20 | 桥 (维度1) | f2-contract 引出补 F-4 (拓扑后继域), 与尾部桥一致 | 修正 ✅ |
| 21 | 前向引用 (维度3) | 6 域正文引用 F-X 全部在 1-6 范围, 无死链; f5 的 "F-5" 仅标题自身编号 | 通过 ✅ |
| 22 | 横切 (维度4) | **configKey 消费缺口**: f3-proxy (logRetry 4 处) 和 f4-codec (methodKey Javadoc) 各 0 次提及 | f3 §4/f4 §1 补 configKey 消费句 ✅ |
| 23 | 横切 (维度4) | RetryableException 覆盖: f3(2)/f4(3)/f5(1) 三消费域全覆盖; f1/f2/f6 不消费无需覆盖 | 通过 ✅ |
| 24 | 横切 (维度4) | Request/Response 对象覆盖: f3 最多 (6/3), f6 产 Request, f4 消费, f1 装配域无 — 合理 | 通过 ✅ |
| 25 | 负面空间 (维度5) | f2 (5 条) / f5 (5 条) 全部与源码一致 (无缓存/短路 Javadoc L39 实证) | 通过 ✅ |
| 26 | 开篇 (维度5) | 6/6 域读者处境全部场景化 (具体代码/问题开头) | 通过 ✅ |
