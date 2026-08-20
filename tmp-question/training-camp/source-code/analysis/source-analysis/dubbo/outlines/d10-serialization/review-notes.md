# D-10 序列化 — 深审 REVIEW 记录 (六层深审 + 推理验证)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划未覆盖 — **31 文件 3 模块穷举** (api 16 + hessian2 9 + fastjson2 6) + **SPI 注册表 4 项穷举** (default/wrapper/hessian2/fastjson2) + **ID 值穷举**: **HESSIAN2=2 / FASTJSON2=23** (Constants.java:20,35) | 大纲 §1 |
| 2 | **补充锚点** | **默认 hessian2 + 覆盖链**: DefaultSerializationSelector (系统属性 → 环境变量 → 默认) | 大纲 §2 + pass2-q2 |
| 3 | **补充锚点** | **optimizeSerialization 双端挂钩实证**: DubboProtocol:372 (export) / 453 (refer) — D-2 挂钩闭环 | 大纲 §2 + pass2-q2 |
| 4 | **补充锚点** | **DefaultMultipleSerialization**: 按 serializeType 字符串 SPI 选 + **convertHessian 兼容转换** (L25-50) | 大纲 §1 + pass2-q1 |
| 5 | **补充锚点** | **反序列化安全**: Fastjson2SecurityManager implements AllowClassNotifyListener (L38) + checkSerializable 动态开关 (L48,89) | 大纲 §4 + pass2-q4 |
| 6 | **补充锚点** | 类加载隔离: Hessian2ClassLoaderListener (hessian2) — ScopeModel 呼应 | 大纲 §3 + pass2-q3 |
| 7 | 行号验证 | 全函数 ~25 锚点 + 跨文件 grep (Serialization 36-37 / DefaultMultipleSerialization 25-50 / DefaultSerializationSelector 23-60 / DubboProtocol 372,453 / SerializationOptimizer / Fastjson2SecurityManager 38-90 / Hessian2Serialization 41 / Constants 20,35) | 记录 |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | SPI 解耦 | serialization= 参数 — 协议与序列化独立 ✅ | 通过 |
| V2 | 默认合理 | hessian2 二进制+对象图 — dubbo 场景 ✅ | 通过 |
| V3 | 双端优化 | export/refer 都预注册 — 双向一致 ✅ | 通过 |
| V4 | 安全防护 | 类检查 + 动态开关 — 防 RCE ✅ | 通过 |
| V5 | 类加载隔离 | Hessian2ClassLoaderListener — 模块安全 ✅ | 通过 |

## 反写测试 (只读大纲能否写文章)

- §1 抽象面 (SPI/ID 值/双标识/多序列化) — 可写 ✅
- §2 选择优化 (默认/覆盖链/优化器) — 可写 ✅
- §3 hessian2 (对象图/类加载/工厂) — 可写 ✅
- §4 fastjson2+安全 (SecurityManager/三选择) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 深审汇总

锚点 ~25 处验证, 4 闭环完成 (q1-q4), **数字穷举 1 + 补充锚点 5**。核心认知: **SPI 解耦 + ID 双标识 (hessian2=2/fastjson2=23) + 默认 hessian2 可全局覆盖 + 双端类预注册优化 + 反序列化安全一等公民 + 类加载隔离**。待二次 REVIEW (07 五维度 + 反写测试)。

---

# 二次深度 REVIEW (2026-08-16, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 前置 D-1 (SPI) / D-9 (protobuf 钩子) ✅; 引出 收尾 ✅; 对照 Java 序列化/protobuf/JSON ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (抽象/选择/实现/安全) 全覆盖 ✅; 性能 (类预注册/紧凑二进制) ✅; 内存 (对象图处理) ✅; 一致性 (双端优化/ID 稳定) ✅; **安全维度 (反序列化 RCE 防护) 唯一含 Security 的域** ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | §1 抽象面 ~8 句逐句对源码一致 ✅ (SPI/ID 值/双标识/多序列化) | 记录 |
| 10 | 通过项 | §2 选择优化: 默认/覆盖链/双端挂钩/优化器接口 ✅ | 记录 |
| 11 | 通过项 | §3 hessian2: 对象图/类加载/工厂/aot ✅ | 记录 |
| 12 | 通过项 | §4 fastjson2+安全: SecurityManager/动态开关/三选择 ✅ | 记录 |
| 13 | **harness** | MiniSerialization 编译运行 **5/5 PASS** (A SPI 选择/ID 值/兼容转换+B 默认/覆盖+C 循环引用对象图+D 安全开关) | 记录 |

## 二次 REVIEW 汇总

共 **0 处机制修复** (一次 REVIEW 已修复 6 处), harness 5/5 全过, 反写测试结论: 大纲机制面完整可支撑写作。**D-10 可进入写作阶段**。

---

# 三次深度 REVIEW (2026-08-16, 09 §3 "重审也可能错" — 存疑面穷举 T1-T7)

> 动机: 对 outline 全部锚点重新 grep, 穷举七个存疑面 (优化器流程/类注册/异常包装/对象图/附件/工厂管理/多序列化细节)。

## 追查过程 (存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | ObjectInput/Output 契约? | writeAttachments (ObjectOutput.java:56 default) — 附件默认实现 | 通过 (验证) |
| T2 | hessian2 对象图? | Hessian2ObjectOutput.writeObject → mH2o.writeObject (L101-102 委托 hessian 库) | 通过 (验证) |
| T3 | 类注册? | **SerializableClassRegistry.registerClass(clazz) / (clazz, serializer) — 自定义序列化器也可注册** (kryo 提及) | **发现 8 (补锚)** |
| T4 | 优化器流程? | **optimizeSerialization 在协议基类 AbstractProtocol (L152-174)**: OPTIMIZER_KEY → isAssignableFrom 校验 → 实例化 → getSerializableClasses → registerClass 循环 | **发现 9 (大发现)** |
| T5 | 工厂管理? | Hessian2FactoryManager per-scope (够用) | 通过 (验证) |
| T6 | 附件? | writeAttachments default 方法 | 通过 (验证) |
| T7 | 异常包装? | **DefaultSerializationExceptionWrapper → ProxyObjectOutput 代理** (L49-60) — 序列化异常统一 | **发现 10 (补锚)** |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 优化归属正确 | AbstractProtocol 基类 — 所有协议共享 ✅ | 通过 |
| V2 | 校验严格 | isAssignableFrom — 配置错误早暴露 ✅ | 通过 |
| V3 | 自定义可注册 | registerClass(clazz, serializer) — 扩展面 ✅ | 通过 |
| V4 | 异常统一 | ProxyObjectOutput — 包装一致 ✅ | 通过 |
| V5 | 附件默认 | writeAttachments default — 实现可选覆盖 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **补充锚点** | SerializableClassRegistry.registerClass(clazz, serializer) — 自定义序列化器注册 (kryo 提及) | 大纲 §2 |
| 9 | **补充锚点 (大发现)** | **optimizeSerialization 在 AbstractProtocol (协议基类 L152-174)** — OPTIMIZER_KEY → 校验 → 实例化 → 注册循环; DubboProtocol 继承调用 | 大纲 §2 + pass2-q2 |
| 10 | **补充锚点** | DefaultSerializationExceptionWrapper → **ProxyObjectOutput 代理包装** (L49-60) — 异常统一 | 大纲 §1 |

## 三次 REVIEW 汇总

七存疑面全实证 (T1-T7); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复 (含大发现 #9: 优化器流程归属协议基类)**。核心认知: **优化流程 = OPTIMIZER_KEY → 校验 → 实例化 → 类注册 (协议基类共享); 自定义序列化器可注册; 异常 Proxy 包装**。大纲经修复后再次反写测试可支撑写作。

---

# 四次深度 REVIEW (2026-08-16, 07 维度1 叙事桥一致性 + 跨文档一致性)

## R1 跨域桥接检查 (07 维度1)

| # | 桥 | 检查 | 结论 |
|:--:|:--|:--|:--:|
| 11 | IN 桥 | D-10 前置 D-1/D-9 — D-9 OUT "→ D-10-序列化: PbUnpack/PackableMethod — protobuf 序列化深潜" ✅ 字面承接; D-1 SPI 基础 ✅ | 通过 |
| 12 | **D-2 挂钩闭环** | optimizeSerialization: D-2 export 挂钩 (L373) → D-10 深化 (AbstractProtocol 基类归属 L152-174) — 跨域一致 ✅ | 通过 |
| 13 | OUT 桥 | D-10 无后续域 (收尾链) — 序列化是传输/协议/安全三面汇合点 ✅; 对照 gRPC G-1 ✅ | 通过 |

## R2 跨文档一致性

| # | 检查项 | 结果 |
|:--:|:--|:--|
| 14 | PLAN §三 D-10 行 vs outline 4 节 — 覆盖 ✅ | 通过 |
| 15 | 三次 REVIEW 修正 (#8-#10) 同步: outline §1/§2 + pass2-q2 — 全部修正 ✅ | 通过 |
| 16 | pass1 待展开 5 项: DefaultMultipleSerialization (q1)/optimizeSerialization (q2+三次 REVIEW)/Selector (q2)/hessian2 细节 (q3)/安全 (q4) — 全部完成 ✅ | 通过 |
| 17 | harness (5 断言) vs 大纲机制: SPI/ID/兼容/默认/循环引用/安全 一致 ✅ | 通过 |
| 18 | HANDOFF §零 D-10 状态行 vs PLAN 进度行 vs review-notes — 一致 ✅ | 通过 |
| 19 | 锚点行号复查: AbstractProtocol L152-174 / SerializableClassRegistry / ProxyObjectOutput L49-60 — 全部吻合 ✅ | 通过 |

## 四次 REVIEW 汇总

跨域桥 3 项 (11-13, 含 D-2 挂钩闭环) + 跨文档 6 项 (14-19) 全过, **0 处新修复**。收敛信号: 连续两轮零结构性发现 (二次 REVIEW 0 修复 + 本轮 0 修复), 且维度不同 (07 五维度 → 存疑面穷举 → 叙事桥)。**D-10 深审收敛, 可进入写作阶段**。
