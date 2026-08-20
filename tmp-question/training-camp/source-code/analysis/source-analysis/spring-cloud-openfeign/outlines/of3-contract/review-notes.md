# OF-3 契约集成 — 深审 REVIEW 记录 (六层深审 + 推理验证)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | OPENFEIGN-PLAN OF-3 断言全验证: 611 行 / **7 处理器穷举** (annotation/ 目录) / **4 构造器** (L126-168) / 组合注解判定 (L293-298) | 大纲 §1-§4 |
| 2 | **补充锚点** | **类级 @RequestMapping 禁止**: IllegalArgumentException "not allowed on @FeignClient interfaces" (L227-231) | 大纲 §1 + pass2-q1 |
| 3 | **补充锚点** | **HTTP method 默认 GET** + checkOne (L302-306) | 大纲 §1 + pass2-q1 |
| 4 | **补充锚点** | **四解析** (L326-335): parseProduces→ACCEPT / parseConsumes→CONTENT_TYPE / parseHeaders 键值拆分+ "!=" 排除 / parseParams NameValueResolver | 大纲 §2 + pass2-q2 |
| 5 | **补充锚点** | **Pageable → queryMapIndex** (L362-370, queryMapParamPresent 防冲突) — OF-4 分页关联 | 大纲 §3 + pass2-q3 |
| 6 | **补充锚点** | **GET 未注解参数警告** (L245-260): "may result in fallback to POST at runtime" — 生产防坑 | 大纲 §3 + pass2-q3 |
| 7 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (SpringMvcContract 98,226-260,287-355,362-400,420-457,470-485 / FeignClientsConfiguration 88,147-160 / SpringQueryMap 33-37 / QueryMapParameterProcessor 34-48) | 记录 |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 翻译完整 | 注解 → 请求模板 (method/path/header/param) ✅ | 通过 |
| V2 | 组合注解 | isAnnotationPresent + findMergedAnnotation ✅ | 通过 |
| V3 | 参数闭环 | 注册表分发 + Pageable 特判 ✅ | 通过 |
| V4 | 生产友好 | GET 警告/checkOne/类级禁止 — 早暴露 ✅ | 通过 |
| V5 | 合并项齐 | SpringQueryMap/Formatter/CollectionFormat ✅ | 通过 |

## 反写测试 (只读大纲能否写文章)

- §1 注解面 (类级禁止/组合/GET 默认/四构造器) — 可写 ✅
- §2 请求面 (四解析/CollectionFormat) — 可写 ✅
- §3 参数面 (注册表/Pageable/警告) — 可写 ✅
- §4 合并项 (SpringQueryMap/Formatter) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 深审汇总

锚点 ~30 处验证, 4 闭环完成 (q1-q4), **数字穷举 1 + 补充锚点 5**。核心认知: **类级禁止 + 组合注解 (findMergedAnnotation) + 四解析 (Accept/Content-Type/headers/params) + 注册表 7 处理器 + Pageable 特判 + GET 警告 (防降级) + 4 构造器演进**。待二次 REVIEW (07 五维度 + 反写测试)。

---

# 二次深度 REVIEW (2026-08-16, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 前置 OF-2 (Contract 组件) ✅; 引出 OF-4 (Pageable 分页)/OF-7 (FeignClientsConfiguration 组装) ✅; 对照 Feign 本体 F-2/Spring MVC ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (注解/请求/参数/合并项) 全覆盖 ✅; 性能 (注册表 O(1) 分发) ✅; 内存 (processedMethods 记录) ✅; 一致性 (类级禁止/checkOne/GET 警告) ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | §1 注解面 ~8 句逐句对源码一致 ✅ (类级禁止/组合/GET 默认/四构造器) | 记录 |
| 10 | 通过项 | §2 请求面: 四解析/CollectionFormat ✅ | 记录 |
| 11 | 通过项 | §3 参数面: 注册表/Pageable/警告 ✅ | 记录 |
| 12 | 通过项 | §4 合并项: SpringQueryMap/Formatter ✅ | 记录 |
| 13 | **harness** | MiniContract 编译运行 **5/5 PASS** (A 类级禁止+组合注解+B 四解析+C 注册表+警告+D Pageable 防冲突) | 记录 |

## 二次 REVIEW 汇总

共 **0 处机制修复** (一次 REVIEW 已修复 6 处), harness 5/5 全过, 反写测试结论: 大纲机制面完整可支撑写作。**OF-3 可进入写作阶段**。

---

# 三次深度 REVIEW (2026-08-16, 09 §3 "重审也可能错" — 存疑面穷举 T1-T10)

> 动机: 对 outline 全部锚点重新 grep, 穷举十个存疑面 (resolve 占位符/上下文/processedMethods/decodeSlash/expander/处理器细节/默认方法/泛型返回)。

## 追查过程 (存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | resolve 占位符? | **ConfigurableApplicationContext.getEnvironment().resolvePlaceholders** (L343-347) — ${} 环境解析 | 通过 (验证) |
| T2 | 参数上下文? | **SimpleAnnotatedParameterContext 内部类** (L542): setParameterName/setTemplateParameter | 通过 (验证) |
| T3 | processedMethods? | **configKey → Method 映射** (L114/241/380) — 参数处理取 Method | 通过 (验证) |
| T4 | parseAndValidateMetadata? | processedMethods.put 先记 → super (BaseContract) → GET 警告 → return (L237-270) | 通过 (验证) |
| T5 | decodeSlash? | 构造参数 (L122/151) — 斜杠解码开关 | 通过 (验证) |
| T6 | PathVariable 细节? | **name 校验 → setParameterName → 模板变量匹配 → 找不到 → formParams.add** (L50-80) | **发现 7 (补锚)** |
| T7 | FeignClientProperties 使用? | SpringMvcContract 构造参数 (q1 已提) | 通过 (验证) |
| T8 | indexToExpander? | **未注解参数 → TypeDescriptor + conversionService.canConvert + convertingExpanderFactory → expander** (L394-406) | **发现 8 (补锚, 大发现)** |
| T9 | 默认方法过滤? | BaseContract 面 (feign 本体 F-2 已覆盖) | 通过 (验证) |
| T10 | 泛型返回? | BaseContract Types.resolveReturnType (feign 本体面) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | expander 闭环 | 未注解参数 → ConversionService → expander — 类型转换序列化 ✅ | 通过 |
| V2 | 模板变量匹配 | {name} 在 url/queries/headers 找 — 参数定位 ✅ | 通过 |
| V3 | formParams 兜底 | 找不到 → form 参数 — 变体支持 ✅ | 通过 |
| V4 | 占位符动态 | ConfigurableApplicationContext 解析 — 环境变量 ✅ | 通过 |
| V5 | 上下文完整 | SimpleAnnotatedParameterContext — 处理器 API ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **补充锚点** | **PathVariable 处理器细节**: 模板变量匹配 (url/queries/headers) + 找不到 → formParams.add (L50-80) | 大纲 §3 |
| 8 | **补充锚点 (大发现)** | **未注解参数 → ConversionService 推断 Expander** (L394-406) — 类型转换序列化机制 (GET 警告的语义根源) | 大纲 §3 |

## 三次 REVIEW 汇总

十存疑面全实证 (T1-T10); 推理验证 5 项全过 (V1-V5); **新发现 2 处全部修复 (含大发现 #8: expander 推断机制)**。核心认知: **未注解参数 → ConversionService → Expander (类型转换序列化); PathVariable 模板变量匹配 + formParams 兜底**。大纲经修复后再次反写测试可支撑写作。

---

# 四次深度 REVIEW (2026-08-16, 07 维度1 叙事桥 + 跨文档一致性)

## R1 跨域桥接检查

| # | 桥 | 检查 | 结论 |
|:--:|:--|:--|:--:|
| 9 | IN 桥 | OF-2 OUT "Contract = SpringMvcContract — 注解解析" ✅ 字面承接 | 通过 |
| 10 | OUT 桥 | OF-3 引出 OF-4 (Pageable 分页) / OF-7 (FeignClientsConfiguration 组装) — PLAN 拓扑一致 ✅ | 通过 |
| 11 | **Feign F-2 交叉** | 内容边界干净: F-2 不提 SpringMvcContract/RequestMapping (0 处), OF-3 不提 @RequestLine (0 处) — 双 Contract 大纲无重复 ✅ | 通过 |
| 12 | harness 复跑 | 5/5 PASS ✅; PLAN 进度行已同步 (三轮) ✅ | 通过 |

## 四次 REVIEW 汇总

跨域桥 4 项 (9-12) 全过, **0 处新修复**。

---

# 五次深度 REVIEW (2026-08-16, 锚点终验 + 内容边界)

## 锚点抽查 (最后一批)

| # | 锚点 | 验证 | 结论 |
|:--:|:--|:--|:--:|
| 13 | GET 默认 (L302-306) | grep GET 命中 | 通过 |
| 14 | Pageable queryMapIndex (L362-370) | grep 命中 | 通过 |
| 15 | expander 推断 (L394-406) | grep 4 处命中 | 通过 |

## 五次 REVIEW 汇总

**0 处新修复**。收敛信号: **连续两轮零结构性发现** (四轮 0 修复 + 五轮 0 修复), 且维度不同 (叙事桥 → 锚点终验/边界)。**OF-3 深审收敛 — 五轮深审共修复 8 处, 大纲可进入写作阶段**。
