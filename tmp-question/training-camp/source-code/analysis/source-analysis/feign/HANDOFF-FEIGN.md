# HANDOFF — Feign 源码分析交接文档 (6/6 域收官)

> **日期**: 2026-08-15 | **版本**: OpenFeign 13.14-SNAPSHOT (pom.xml 实证; 规划期执行计划基于 9.x/10.x 认知, 本阶段按 13.x 代际修正)
> **给新 AI**: 本文是 Feign 阶段的**唯一入口**。
> **源码**: `/data/workspace/source-code/code/spring/feign` | **规划**: FEIGN-PLAN.md (09 审计 v1)

---

## §零 状态速查

| 域 | 级别 | 方案 | 大纲节数 | 提问 | harness | 状态 |
|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| F-6 模板引擎 | 🔴 | A | 9 | 30 | MiniTemplate **5/5** | ✅ |
| F-2 契约解析 | 🔴 | A | 7 | 29 | MiniContract **5/5** | ✅ |
| F-4 编解码 | 🟡 | B | 6 | 20 | (🟡 无) | ✅ |
| F-5 拦截器链 | 🟡 | B | 7 | 27 | (🟡 无) | ✅ |
| F-3 代理与调用链 | 🔴 | A | 7 | 28 | MiniFeign **5/5** | ✅ |
| F-1 Builder 装配 | 🔴 | A | 5 | 27 | MiniBuilder **5/5** | ✅ |
| **合计** | 4🔴+2🟡 | | 41 节 | 161 问 | **20/20** | **6/6 收官** |

---

## §一 09 怀疑审计结论 (详细见 FEIGN-PLAN.md)

- **域清单 5→6**: 新增 **F-6 模板引擎** (定义特征: README "templatized request" 原话; RequestTemplate 1119 行 + template/ 10 文件)
- **代际修正 3 处**: ① Builder CRTP (Feign.Builder extends BaseBuilder<Builder, Feign>, 13.x) ② 契约 DeclarativeContract 注册表架构 (13.x, 未识别注解 warning) ③ 三拦截器体系 (MethodInterceptor/ResponseInterceptor 为 13.x @Experimental 新扩展)
- **补充 4 处**: 版本 13.14 / 来源 Retrofit / 时空溯源用 CHANGELOG (浅克隆) / 异步面 (AsyncFeign 763 行) 并入 F-3
- **13.x API 移除确认**: requestLine()/build() 已移除 (产出入口 request()+url()); alreadyEncoded 废弃 (统一幂等编码)

## §二 拓扑与依赖

**F-6 → F-2 → F-4 → F-5 → F-3 → F-1**

- F-2 依赖 F-6 (MethodMetadata.template); F-3 依赖 F-2+F-6+F-4+F-5 (执行链汇聚); F-1 依赖全部 (装配 Hub)

## §三 关键机制速查

### F-6 模板引擎
| 机制 | 锚点 |
|---|---|
| queries/headers 双 Map 结构 | RequestTemplate.java:56-57 |
| resolve 副本隔离 + query 烧进 uri | RequestTemplate.java:182-262 |
| 嵌套花括号字面量 | Template.java:289-300 |
| 表达式长度上限 10000 | Expressions.java:89-94 |
| 正则非法降级字面量 | Expressions.java:138-140 |
| 四位置编码策略矩阵 | UriTemplate.java:74-76 / QueryTemplate.java:132-150 / HeaderTemplate.java:126-137 / BodyTemplate.java:55-60 |
| stripCrlf 头注入防御 | HeaderTemplate.java:193-195 |
| 幂等编码 (isEncoded) | UriUtils.java:37-45, 111-141 |
| EXPLODED 重复键 | CollectionFormat.java:68-75 |
| **decodeSlash 反转语义** | QueryTemplate.java:137 |

### F-2 契约解析
| 机制 | 锚点 |
|---|---|
| BaseContract 模板方法 + 参数循环 | Contract.java:49-175 |
| 未消费参数 → body 推断 | Contract.java:138-158 |
| configKey 协变合并 | Contract.java:67-77 |
| DeclarativeContract 注册表 + warning | DeclarativeContract.java:32-35, 68-82 |
| @RequestLine 正则 | DefaultContract.java:32, 48-69 |
| @Param 名回退 -parameters | DefaultContract.java:100-104 |
| formParams 缺席推断 | DefaultContract.java:117-119 |
| MethodInfo 异步剥离 | MethodInfo.java:36-39 |

### F-4 编解码
| 机制 | 锚点 |
|---|---|
| Encoder MAP_STRING_WILDCARD form 标志 | Encoder.java:71, 82 |
| 编码器不设 Content-Type | Encoder.java:55-67 文档 |
| DefaultDecoder 404/204 emptyValueOf | DefaultDecoder.java:27 |
| RetryAfterDecoder 双格式+小数 | ErrorDecoder.java:97-133 (#980) |
| **17 异常类穷举** (404 NotFound L345/500 InternalServerError L412) | FeignException.java:313-440 |
| Retry-After → RetryableException | DefaultErrorDecoder.java:48-58 |
| @ErrorCodes 注解驱动 (feign.error 包) | AnnotationErrorDecoder.java:32-48 |

### F-5 拦截器链
| 机制 | 锚点 |
|---|---|
| RequestInterceptor resolve 后应用 | RequestInterceptor.java:42-44 |
| MethodInterceptor 链末端 runWithRetry | SynchronousMethodHandler.java:59-65 |
| 可短路 | MethodInterceptor.java:39 |
| InvocationContext 404/500 裁决 | InvocationContext.java:76-86 |
| BasicAuth 预计算 headerValue | BasicAuthRequestInterceptor.java:28, 52 |
| Retryer 默认 100ms/×1.5/1s/5 | DefaultRetryer.java:28-30, 77-80 |

### F-3 代理与调用链
| 机制 | 锚点 |
|---|---|
| 必须接口校验 | ReflectiveFeign.java:150-152 |
| FeignInvocationHandler 内部类 | ReflectiveFeign.java:75-119 |
| retryer.clone 每请求独立 | SynchronousMethodHandler.java:69 |
| logAndRebuffer 流重建 | ResponseHandler.java:65-88 |
| MethodInfo 异步共用元数据 | AsynchronousMethodHandler.java:319 |
| Client 三形态 (Default/Proxied/第三方) | Feign.java:99 |
| IOException → RetryableException | FeignException.java:302-311 |

### F-1 Builder 装配
| 机制 | 锚点 |
|---|---|
| CRTP 泛型自引用 | BaseBuilder.java:41 |
| 18 字段默认值 | BaseBuilder.java:43-62 |
| enrich clone 副本 + Capability 流水线 | BaseBuilder.java:265-389; Capability.java:38-56 |
| build() final = enrich().internalBuild() | BaseBuilder.java:385-389 |
| internalBuild 装配 (ResponseHandler+SMH.Factory+ReflectiveFeign) | Feign.java:217-242 |

## §四 harness 实证发现 (极简复现, 全部离线)

1. **嵌套花括号**: `foo{bar{baz}}` → chunks=[foo, {bar{baz}}] 整块字面量 (MiniTemplate B)
2. **幂等编码**: "a b%20c" → "a%20b%20c" — %20 段跳过不二次编码 (MiniTemplate C)
3. **位置策略**: 同一 {token} header 不编码保留 "=", uri 编码成 %3D, body 保留未解析 (MiniTemplate D)
4. **重试数学**: attempt 从 1 起, 4 次重试后第 5 次抛 (DefaultRetryer 语义; MiniFeign C — 修正了初版 attempt 从 0 起的实现偏差)
5. **Capability 流水线**: reduce 顺序 = 后注册先外包 (MiniBuilder D)
6. **角色分配**: 未消费参数 → body; 多 body 报错 (MiniContract B)
7. **formParams**: 模板里没有的 @Param → form (MiniContract C)

## §五 过程教训 (本阶段新增)

- **探索代理超范围写文件**: 2 个幽灵目录 (f3-client-execution/f5-uri-template) + 1 个覆盖文件 (f4-codec outline) — explore 代理本应只读, 实际写入了产出物。处置: 幽灵目录删除 (独有机制 encodeSlash 反转/Client 三形态吸收到正式交付), f4 代理版保留为基底 (含 @ErrorCodes/17 异常类穷举, 修正编号引用后质量合格)。**教训: 派代理前必须明示"禁止写任何文件, 只返回文本"**。

## §六 遗留与待办

- [ ] 大纲→正式文章写作 (另行启动, 与阶段5.6 OpenFeign 对照)
- [ ] 阶段5.2 Dubbo (下一仓库, 7 域) — 代理/RPC 对照面
- [ ] Obsidian 知识图谱 (全局待办)
- [ ] 深审缺陷档案回填 (F-1~F-6 无新增类别)

## §七 文件路径

```
analysis/source-analysis/feign/
├── FEIGN-PLAN.md (09 审计表 + 拓扑)
├── knowledge-planning/ (f1~f6 共 6 个 KP)
├── outlines/
│   ├── f6-template/ (9 节 + 30 问 + review 17 项 + temporal-trace)
│   ├── f2-contract/ (7 节 + 29 问 + review 17 项)
│   ├── f4-codec/ (6 节 + 20 问 + review 9 项)
│   ├── f5-interceptor/ (7 节 + 27 问 + review 14 项)
│   ├── f3-proxy/ (7 节 + 28 问 + review 16 项)
│   └── f1-builder/ (5 节 + 27 问 + review 12 项)
└── harness/
    ├── f6-minitemplate/ (MiniTemplate.java 5/5)
    ├── f2-minicontract/ (MiniContract.java 5/5)
    ├── f3-minifeign/ (MiniFeign.java 5/5)
    └── f1-minibuilder/ (MiniBuilder.java 5/5)
```

**运行**: `javac Xxx.java && java Xxx` (纯 JDK, 无依赖)。

## §八 完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (5→6, +1 定义特征)
- [x] 09 审计 3 修正 + 4 补充 + 2 API 移除确认
- [x] 6/6 域交付: KP + 大纲 (41 节) + 提问 (161 问, 每域 5 身份) + 六层深审 (96 项核对) + 时空溯源 (F-6)
- [x] 极简复现 harness 4 个 **20/20 全 PASS**
- [x] 探索代理超范围产物清理 (幽灵目录 2 个删除 + 1 文件合并)
- [x] **REVIEW 修复 2026-08-15**: ① OUT 桥跳域 2 处 (f2→F-4/f4→F-5) ② f4 前置前向引用违规 1 处 ③ f2 temporal-trace 锚点超范围 1 处 (代理产物, Contract.java:336→L252-253) ④ f4 负面空间格式统一 4 条 ⑤ 深审 85→96 ⑥ f3 锚点 6→10 ⑦ harness 20/20 重跑 (详见 REVIEW-2026-08-15.md)
- [ ] 大纲文章写作 (下一阶段)
