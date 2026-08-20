# OF-1 注册机制 — 深审 REVIEW 记录 (六层深审 + 推理验证)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | OPENFEIGN-PLAN OF-1 断言全验证: 503 行 / 5 组注解属性 / **10 个 BeanDefinition 属性穷举** (url/path/name/contextId/type/dismiss404/fallback/fallbackFactory/refreshableClient/qualifiers) / 3 构造参数 (Specification) | 大纲 §1-§4 |
| 2 | **补充锚点** | **name 三级回退**: getName serviceId→name→value (L327-335); getContextId 回退+占位符 resolve (L337-345) | 大纲 §3 |
| 3 | **补充锚点** | **refresh 联动**: refreshableClient = refresh-enabled 属性 (L500 默认 false); true → **setScope("refresh")** (L490) — OF-9 前置 | 大纲 §3 |
| 4 | **补充锚点** | **validateFallback 语义**: !isInterface (L83-87) — fallback 类必须实现 @FeignClient 接口 | 大纲 §3 |
| 5 | **补充锚点** | **getBasePackages 四级兜底**: value→basePackages→basePackageClasses→importingClass 包 (L393+) — 默认扫启动类包 | 大纲 §2 + pass2-q2 |
| 6 | **补充锚点** | **FeignClientSpecification implements NamedContextFactory.Specification** (L29) — SCC C-13 对接 (OF-7 输入) | 大纲 §4 + pass2-q4 |
| 7 | 行号验证 | 全函数 ~25 锚点 + 跨文件 grep (EnableFeignClients 50-88 / FeignClientsRegistrar 71,152-181,202,210-245,319-345,379-393,463-500 / FeignClientSpecification 29,46,62) | 记录 |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 触发链路 | @EnableFeignClients→@Import→Registrar→registerBeanDefinitions ✅ | 通过 |
| V2 | 扫描精确 | useDefaultFilters=false + AnnotationTypeFilter — 只收 Feign 客户端 ✅ | 通过 |
| V3 | 注册完整 | 10 属性 + validate + 懒/急双模式 ✅ | 通过 |
| V4 | 配置对接 | Specification → NamedContextFactory (OF-7) ✅ | 通过 |
| V5 | 刷新联动 | refresh-enabled → setScope(refresh) (OF-9) ✅ | 通过 |

## 反写测试 (只读大纲能否写文章)

- §1 注解入口 (5 属性/双注册) — 可写 ✅
- §2 扫描面 (过滤器/四级包) — 可写 ✅
- §3 注册面 (10 属性/name 回退/refresh 联动) — 可写 ✅
- §4 配置注册 (Specification/双注册) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 深审汇总

锚点 ~25 处验证, 4 闭环完成 (q1-q4), **数字穷举 1 + 补充锚点 5**。核心认知: **Import 机制触发 + 标准扫描器 + 四级包兜底 + 10 属性 BeanDefinition + name 三级回退 + Specification 对接子上下文 + refresh 联动 (OF-9 前置)**。待二次 REVIEW (07 五维度 + 反写测试)。

---

# 二次深度 REVIEW (2026-08-16, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 前置 无 (框架入口) ✅; 引出 OF-2 (FactoryBean 触发)/OF-7 (Specification 子上下文) ✅; 对照 @ComponentScan/MyBatis MapperScan ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (注解/扫描/注册/配置) 全覆盖 ✅; 性能 (懒注册可配/lazy-attributes-resolution) ✅; 内存 (扫描结果不缓存) ✅; 一致性 (name 回退/默认+客户端双注册) ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | §1 注解入口 ~6 句逐句对源码一致 ✅ (5 属性/双注册) | 记录 |
| 10 | 通过项 | §2 扫描面: useDefaultFilters=false/过滤器/四级包 ✅ | 记录 |
| 11 | 通过项 | §3 注册面: 10 属性/name 回退/refresh 联动/validate ✅ | 记录 |
| 12 | 通过项 | §4 配置注册: Specification 三参/双注册 ✅ | 记录 |
| 13 | **harness** | MiniFeignRegistration 编译运行 **5/5 PASS** (A 兜底包+B 过滤+C refresh 联动+name 回退+D 三参; 断言修正: 类名带注解标记模拟) | 记录 |

## 二次 REVIEW 汇总

共 **0 处机制修复** (一次 REVIEW 已修复 6 处), harness 5/5 全过, 反写测试结论: 大纲机制面完整可支撑写作。**OF-1 可进入写作阶段**。

---

# 三次深度 REVIEW (2026-08-16, 09 §3 "重审也可能错" — 存疑面穷举 T1-T7)

> 动机: 对 outline 全部锚点重新 grep, 穷举七个存疑面 (getUrl/getPath/接口候选判定/FeignClient 全属性/占位符解析/默认配置命名/规范去重)。

## 追查过程 (存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | getUrl/getPath? | **SpEL 排除**: url 以 "#{" 开头且含 "}" → 不处理 (运行时表达式 URL) | **发现 9 (补锚)** |
| T2 | 接口怎么作候选? | **getScanner 匿名子类覆写 isCandidateComponent (L382-391): isIndependent && !isAnnotation → 接口可扫** (Spring 默认排除接口!) | **发现 10 (大发现)** |
| T3 | 默认配置命名? | "default.类名" (enclosingClass 细节 L162-167) | 通过 (验证) |
| T4 | @FeignClient 全属性? | **12 属性**: value/contextId/name/qualifiers/url/dismiss404/configuration/fallback/fallbackFactory/path/**primary 默认 true** | **发现 11 (补锚)** |
| T5 | 占位符 resolve? | getContextId resolve (ConfigurableBeanFactory) — 已覆盖 | 通过 (验证) |
| T6 | path 处理? | getPath hasText 判断 — 简单 | 通过 (验证) |
| T7 | Specification 去重? | 无 equals/hashCode 覆写 — 按名覆盖由容器 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 接口候选成立 | isIndependent && !isAnnotation — 接口扫描闭环 ✅ | 通过 |
| V2 | SpEL 合理 | "#{...}" 留运行时 — 动态 URL 支持 ✅ | 通过 |
| V3 | 属性完整 | 12 属性穷举 — 声明面完整 ✅ | 通过 |
| V4 | primary 语义 | 默认 true — 同类型多客户端主选 ✅ | 通过 |
| V5 | 扫描闭环 | 覆写候选 + 类型过滤器 — 精确且允许接口 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **补充锚点** | **getUrl SpEL 排除**: "#{" 开头 → 运行时表达式 URL (L) | 大纲 §1 |
| 10 | **补充锚点 (大发现)** | **isCandidateComponent 覆写**: isIndependent && !isAnnotation → **@FeignClient 接口可作扫描候选** (Spring 默认排除接口 — 关键机制) | 大纲 §2 + pass2-q2 |
| 11 | **补充锚点** | **@FeignClient 12 属性**: 含 primary 默认 true (声明面完整) | 大纲 §1 |

## 三次 REVIEW 汇总

七存疑面全实证 (T1-T7); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复 (含大发现 #10: 接口候选判定覆写)**。核心认知: **扫描器覆写 isCandidateComponent 允许接口 + @FeignClient 12 属性声明面 + SpEL URL 运行时解析**。大纲经修复后再次反写测试可支撑写作。

---

# 四次深度 REVIEW (2026-08-16, 07 维度1 叙事桥一致性 + 跨文档一致性)

## R1 跨域桥接检查 (07 维度1)

| # | 桥 | 检查 | 结论 |
|:--:|:--|:--|:--:|
| 12 | IN 桥 | OF-1 无前置 (框架入口) — 与 OPENFEIGN-PLAN 拓扑一致 ✅ | 通过 |
| 13 | OUT 桥 | OF-1 引出 OF-2 (FactoryBean 触发) / OF-7 (Specification 子上下文) — PLAN 拓扑一致 ✅ | 通过 |
| 14 | **SCC 交叉** | FeignClientSpecification implements NamedContextFactory.Specification → **scc13-named-context 域存在** (子上下文已规划) — 交叉可对接 ✅ | 通过 |
| 15 | Feign 本体交叉 | FeignClientFactoryBean 属本仓库 (非 feign 本体); 底座 (Feign.Builder/Contract) 在 5.1 — 归属清晰 ✅ | 通过 |

## R2 跨文档一致性

| # | 检查项 | 结果 |
|:--:|:--|:--|
| 16 | OPENFEIGN-PLAN OF-1 行 vs outline 4 节 — 覆盖 ✅ | 通过 |
| 17 | 三次 REVIEW 修正 (#9-#11) 同步: outline §1/§2 — 全部修正 ✅ | 通过 |
| 18 | pass1 待展开 5 项: 属性清单 (q3)/lazily 机制 (q3)/Specification (q4)/getScanner (q2)/name 解析 (q3) — 全部完成 ✅ | 通过 |
| 19 | harness (5 断言) vs 大纲机制: 兜底包/过滤/refresh/name 回退/三参 一致 ✅ | 通过 |
| 20 | 9 文件 + 1 harness 完整性 ✅; PLAN 进度行 ✅ | 通过 |
| 21 | 锚点行号复查: isCandidateComponent L382-391 / FeignClient.java 12 属性 / L490/L500 — 全部吻合 ✅ | 通过 |

## 四次 REVIEW 汇总

跨域桥 4 项 (12-15, 含 SCC 交叉) + 跨文档 6 项 (16-21) 全过, **0 处新修复**。收敛信号: 连续两轮零结构性发现 (二次 REVIEW 0 修复 + 本轮 0 修复), 且维度不同 (07 五维度 → 存疑面穷举 → 叙事桥)。**OF-1 深审收敛, 可进入写作阶段**。
