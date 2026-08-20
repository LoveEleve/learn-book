# Dubbo 源码分析 — 交接文档 (阶段5.2)

> **日期**: 2026-08-16 | 版本 v1 | 阶段5.2 RPC 与服务治理
> **⚠️ 总入口**: 阶段级总览见 `../HANDOFF-STAGE5.md` (阶段 5 唯一总入口); 本文件是 **Dubbo 仓库分析唯一入口** — 新 AI 只读本文件 + `DUBBO-PLAN.md` 即可继续, 不必读其他文档
> **给新 AI**: 规划权威 = `DUBBO-PLAN.md` (11 域 12 篇, 含 09 怀疑审计表 + 二次 REVIEW 记录)。方法论权威 = `talk-method/source-code-analysis/methodology/zh/` (01-09, **09 对既有规划保持怀疑必读** — 本仓库 PLAN 就是 09 审计的产物)。
> **顺序背景**: 执行计划阶段5.2 原 7 域 (DB-1~DB-7) — 09 重审实测 **7→11 域** (新增 triple/remoting/序列化/元数据, 覆盖率 157%), D-1/D-2 已完成序号不动。
> **任务**: 11 域执行序 **D-1 ✅ → D-2 ✅ → D-3 → D-4 → D-5 → D-6 → D-7 → D-8a → D-8b → D-9 → D-10 → D-11** (拓扑+黑盒先行, 详见 PLAN §六)。严格一个域一个域, 禁止批量写。

---

## §零 当前状态速查

| 域 | 目录 | 类型 | 状态 |
|:--:|---|:--:|:--|
| D-1 SPI 微内核 | outlines/d1-spi-kernel | 🔴 | ✅ **六轮深审** (修复 18 处), harness 10/10, 反写测试过 |
| D-2 服务导出 | outlines/d2-service-export | 🔴 | ✅ **四轮深审** (修复 11 处), harness 12/12, 反写测试过 |
| D-3 服务引用 | outlines/d3-service-refer | 🔴 | ✅ **四轮深审** (修复 8 处, 含 1 处事实错误), harness 5/5, 反写测试过 |
| D-4 RPC 调用 | outlines/d4-rpc-invoke | 🔴 | ✅ **四轮深审** (修复 10 处, 含大发现: 集群级 Filter 链), harness 5/5, 反写测试过 |
| D-5 注册中心 | outlines/d5-registry | 🔴 (🟡→🔴) | ✅ **四轮深审** (修复 10 处), harness 5/5, 反写测试过 |
| D-6 负载均衡 | outlines/d6-loadbalance | 🟡 | ✅ **四轮深审** (修复 8 处, 含大发现: AdaptiveMetrics 公式), harness 5/5, 反写测试过 |
| D-7 集群容错 | outlines/d7-cluster | 🔴 (🟡→🔴) | ✅ **四轮深审** (修复 10 处, 三钩子兑现闭环), harness 5/5, 反写测试过 |
| D-8a 传输抽象+exchange | outlines/d8a-remoting | 🟡 | ✅ **四轮深审** (修复 9 处, 含 pipeline 五段), harness 5/5, 反写测试过 |
| D-8b HTTP 传输栈 (http12) | outlines/d8b-http12 | 🟡 | ✅ **四轮深审** (修复 9 处, 含批量执行队列+流控), harness 5/5, 反写测试过 |
| D-9 Triple 协议 | outlines/d9-triple | 🟡 | ✅ **四轮深审** (修复 9 处, 含 gRPC 内置服务), harness 5/5, 反写测试过 |
| D-10 序列化 | outlines/d10-serialization | 🟡 | ✅ **四轮深审** (修复 9 处, 含优化器归属), harness 5/5, 反写测试过 |
| D-11 元数据/服务发现 | outlines/d11-metadata | 🟡 | ✅ **四轮深审** (修复 9 处), harness 5/5, 反写测试过 |

**DUBBO-PLAN.md**: ✅ 定稿 + 二次 REVIEW (2026-08-16, R1 数字全量重跑 / R2 覆盖矩阵抓 6 处缺口 / R3 依赖方向重跑 / R4 反写测试, 修正 7 处 — 详见 PLAN §八)

---

## §一 DUBBO-PLAN 审计结论速查 (09 规范产物)

### 域清单 7→11 (157%)

执行计划 7 域全部保留; 新增 4 域 (remoting 拆 2 篇):

| 新增域 | 包 | 文件 | 设计决策测试证据 |
|:--:|---|---|---|
| D-8a 传输抽象+exchange | dubbo-remoting-api + netty4 | 147 | Transporter SPI 门面 (Transporters.java, URL 自适应) + exchange 层 |
| D-8b HTTP 传输栈 | dubbo-remoting-http12 | 126 | HTTP/1.1+H/2 消息模型/编解码; triple→http12 **106 处 import** |
| D-9 Triple 协议 | dubbo-rpc-triple | 228 | 3.x 默认协议, gRPC 兼容, REST_ENABLED (TripleProtocol.java:73) |
| D-10 序列化 | dubbo-serialization | 31 | Serialization SPI + 选择器 + optimizeSerialization |
| D-11 元数据 | dubbo-metadata | 77 | MetadataService + 应用级服务发现基座 |

### 数字修正 (穷举验证)

| 断言 | 修正 | 证据 |
|:--|:--|:--|
| DB-6 负载均衡 5 算法 | **6** (+AdaptiveLoadBalance 3.x) | ls loadbalance/ = Abstract + 6 |
| DB-7 集群容错 8 实现 | **9** (+ZoneAwareCluster 3.x) | 9 实现 + Mock/Scope 装饰器 |
| DB-5 注册中心 2 实现 | **4 本地 + ServiceDiscoveryRegistry** | ZK/Nacos/Multicast/Multiple + 应用级 |

### 排除 15 项 (00 §3 thin-wrapper)

qos(85: 43 命令薄壳)/metrics(156: 与阶段6 Micrometer 重叠)/rest(113: 并入 triple REST_ENABLED)/config-spring(78)/spring-boot(69)/compatible(93)/configcenter(11)/native(26)/mcp(17)/filter-cache+validation(25)/auth+security 族(39)/reactive+mutiny(24)/传输变体 5 族(54)/zookeeper-curator5(9)/common 工具面(~150, threadpool 在 D-4/D-8 提) — 理由全在 PLAN §四。

### 依赖方向 (9 条 import 证据, 全部接受)

rpc→remoting (8/20/121) / triple→http12 (106) / registry→cluster (14, RegistryProtocol 在 registry 模块!) / config→registry (8) / registry→metadata (22) / cluster→registry (0) / serialization 经 SPI 解耦 (协议不直接 import)。

### 拓扑 (黑盒先行教学序, 保留原 1-7)

D-1→D-2→D-3→D-4→D-5→D-6→D-7→D-8a→D-8b→D-9→D-10→D-11 — 前向引用检查全过 (PLAN §六)。

---

## §二 D-1 已交付内容 (SPI 微内核, 🔴)

### 产出物

- `outlines/d1-spi-kernel/outline.md` — 大纲 (加载/创建/自适应/激活 4 面 + 代码类型 Architecture)
- `outlines/d1-spi-kernel/pass1-notes.md` + `pass2-q1~q4.md` + `temporal-trace.md` + `completeness-questions.md`
- `outlines/d1-spi-kernel/review-notes.md` — **六轮深审记录 (18 处修复)**
- `harness/d1-spi-kernel/MiniExtensionLoader.java` — 极简复现 (编译运行 10/10 PASS, 含自抓 1 处)

### 核心机制速查 (写作时仍须 re-grep, 行号是线索不是事实)

| 机制 | 锚点 (D-1 深审验证) |
|:--|:--|
| 入口双检查: getExtensionLoader — 接口 + @SPI, 否则 IllegalArgumentException; @SPI scope 参数 (默认 application) | ExtensionLoader.java:242; SPI.java:61-64 |
| 3 加载目录三级优先级: internal=**MAX** / dubbo=**NORMAL** / services=**MIN** (内部优先覆盖) | ExtensionLoader.java:988-998 |
| 懒加载 + 单例缓存 (putIfAbsent) + 失败缓存 (unacceptableExceptions) | L216-221, L280+ |
| injectExtension: setter 扫描 + @DisableInject 跳过 → **自适应对注入** (getAdaptiveExtension); 注入器本身也是自适应扩展 | L219-221, L280+ |
| Wrapper 链: **WrapperComparator 排序永不为 0** (HashSet 集合安全, 注释锚 L54-55) + reverse + @Wrapper matches/mismatches/order → 构造器逐层包装 = AOP | L226-246, L230-235 |
| initExtension: Lifecycle initialize (启动钩子); @Deprecated 废弃缓存 | L248 |
| getAdaptiveExtension 双检锁; 无 @Adaptive 类 → **AdaptiveClassCodeGenerator 动态生成五步**: URL 参数定位 → null 检查 → @Adaptive value 多键回退 (key1→key2) → extName 赋值 → getExtensionLoader().getExtension(extName) | L610+, L1467; Adaptive.java:44-47 |
| getActivateExtension: group 筛选 + **ActivateComparator 四层权重** (before/after 双向 → order → 类名兜底 L114-117) + isActive **OR 语义** + realValue 空 → getAnyMethodParameter 兜底 | L344-440, L471-495 |
| 3.x ExtensionDirector: scoped extension loader manager + ScopeModel + destroyed (作用域管理) | ExtensionDirector.java:28 |

### D-1 负面空间 (06 条, 已写入大纲)

不动态卸载 / 不跨 classloader 隔离 / 不注解扫描 / 不循环依赖处理 / 不编译缓存 / 不多实例隔离

### D-1 深审教训 (REVIEW 抓到的真实错误类型)

数字穷举 1 (3 加载目录) + 语义标注 2 (注入自适应对/WrapperComparator 永不为 0) + 补充锚点 15 (ExtensionDirector/AdaptiveClassCodeGenerator/@Activate 条件对/Wrapper matches/优先级/排序权重/生成五步/scope+多键回退/isActive OR/废弃标记...)。**教训: "SPI 微内核" 远不止 @SPI 注解 — 3.x 的 ExtensionDirector + 动态代码生成是深水区, 写作时逐锚点验证。**

---

## §三 D-2 已交付内容 (服务导出, 🔴)

### 产出物

- `outlines/d2-service-export/outline.md` — 大纲 (export 链/scope/server/invoker 4 节 + 代码类型 Architecture)
- `outlines/d2-service-export/pass1-notes.md` + `pass2-q1~q4.md` + `temporal-trace.md` + `completeness-questions.md`
- `outlines/d2-service-export/review-notes.md` — **四轮深审记录 (11 处修复)**
- `harness/d2-service-export/MiniServiceExport.java` — 极简复现 (编译运行 12/12 PASS, A-D 4 面)

### 核心机制速查 (写作时仍须 re-grep)

| 机制 | 锚点 (D-2 深审验证) |
|:--|:--|
| export 6 层链: export → doExport → doExportUrls → 1Protocol → exportUrl → doExportUrl (数字穷举实证) | ServiceConfig.java:325-993 |
| scope 三态: NONE/LOCAL/REMOTE — **本地始终可导** (injvm port 0) | L650-690 |
| RegisterTypeEnum 3 值: AUTO/MANUAL/NEVER; REGISTER_KEY=false → MANUAL (注册模式细分) | §1 |
| protocolSPI = **Protocol 自适应实例** (D-1 消费面: export 时挂监听/过滤) | L188 |
| serverMap 缓存 + reset: 同地址多服务共享 server; 已存在 → server.reset (override 支持) | DubboProtocol.java:377-405 |
| EXT_PROTOCOL 附加协议: 主协议外追加 (IS_PU_SERVER/IS_EXTRA 标记) — 多协议导出面 | ServiceConfig L660-685 |
| optimizeSerialization: export 时序列化优化 (SerializationOptimizer) — 性能面 | DubboProtocol.java:373 |
| 回调服务导出: IS_CALLBACK_SERVICE + STUB_EVENT — 消费者反向导出 (双工) | DubboProtocol.java:351-360 |
| 存疑面实证: unexport (unexported 标志+exporters 遍历) / delay 定时导出 / executor ISOLATION 模式才生效 / metadata attachments | review-notes 四次 T1-T5 |

### D-2 负面空间 (06 条, 已写入大纲)

不热发布 / 不按需懒加载 / 不多租户 / 不服务降级 / 不端口协商 / 不导出回滚

### D-2 深审教训

补充锚点 5 (scope 三态/RegisterTypeEnum/protocolSPI/serverMap 缓存/optimizeSerialization/回调服务) + 数字穷举 1 (6 层链)。**教训: 导出不是"一行 Protocol.export" — 配置→URL→invoker→protocol 的 6 层渐进 + server 复用是 Dubbo 服务端设计的核心, 写作时按 6 层链逐层展开。**

---

## §四 方法论速查 (每域必走, 禁止跳过)

```
1. 读 DUBBO-PLAN.md 该域行 (核心主题/前置/置信度)
2. 09 审计: 对该域所有数字/行号断言先 grep 验证 (09 §7 域级复查: 数字/行号/默认值)
3. 00 §2 入口展开: 读入口方法体, Level-1/2 展开, 旁路扫描
4. 知识规划 (03-补充): 逐源提取 → 聚合 → 深度分类 → 聚类 (巨型域必须)
5. Pass 1-2: 六层深审 (数字穷举/补充锚点/语义标注/行号验证) + 推理验证 (反推)
6. outline.md: 每节四要素 (场景→技术描述 file:line+调用链→关键设计 why→跨层) + 负面空间 + 结尾桥
7. completeness-questions.md: 3 身份 (开发者/架构师/学生) 提问
8. 深审 REVIEW: 至少一轮逐锚点 re-grep + 一轮反写测试 (只读大纲能否写文章)
9. harness/ 极简复现: 编译运行全过 (D-1: 10/10, D-2: 12/12 参考)
10. 更新本文件 §零 状态表
```

**锚点纪律**: 大纲/KP 行号是"线索不是事实" — 每次写作/深审都重新 grep (09 核心教训; 本仓库 D-1 六轮/D-2 四轮 REVIEW 每次都 re-grep, 仍有新发现)。

---

## §五 高频坑 (Dubbo 特有, 先读再干)

1. **源码是 3.3.7-SNAPSHOT (3.3 分支), 不是 2.x!** 认知差异最大处:
   - `ServiceConfig.export(RegisterTypeEnum)` (带参, L325) / `ReferenceConfig.get(boolean check)` (L230)
   - ExtensionLoader 之上有 **ExtensionDirector** (3.x 作用域); ScopeModel/FrameworkModel/ApplicationModel/ModuleModel 在 dubbo-common (rpc.model 包)
   - **RegistryProtocol 在 dubbo-registry-api** (integration 包), 不在 config 模块 — 注册中心域要讲它
   - **triple 是默认协议** (228 文件), dubbo-rpc-dubbo (旧协议) 只有 25 文件
   - REST 面已并入 triple (H2_SETTINGS_REST_ENABLED), 独立 rest 模块是旧适配
2. **文件统计必须排除 target**: `find X -path "*/src/main/*"` — 首版 PLAN 曾把 rest 数成 141 (混入编译产物), 实际 113。
3. **源码是 grafted (浅克隆)**: `git log` 时空溯源受限 (只有单 commit) — 04 方案 A 时空溯源需降级或注明。
4. **行号漂移**: 本仓库 HEAD 2026-07-10, 大纲定稿 2026-08-15/16 — 每次写作 re-grep。
5. **qos 是排除项但数字易错**: command/impl 43 个命令 (不是 9) — 别在文章里引用。
6. 接口包 `org.apache.dubbo.rpc.*` 横跨多模块 (dubbo-rpc/cluster/registry/config 共用) — 判断模块归属看文件路径而非包名。

---

## §六 文件路径速查

| 东西 | 路径 |
|:--|:--|
| 域规划 (唯一权威) | `dubbo/DUBBO-PLAN.md` (11 域 12 篇 + 审计表) |
| 本交接文档 | `dubbo/HANDOFF-DUBBO.md` (本文) |
| 知识规划 (11 份) | `dubbo/knowledge-planning/d1~d11-*.md` (头部+§0.8+01提取+02-04聚合分类+05闭环) |
| 源码 | `/data/workspace/source-code/code/spring/dubbo` (3.3.7-SNAPSHOT) |
| D-1 大纲+深审 | `dubbo/outlines/d1-spi-kernel/` (9 文件) |
| D-2 大纲+深审 | `dubbo/outlines/d2-service-export/` (9 文件) |
| harness | `dubbo/harness/d1-spi-kernel/` `dubbo/harness/d2-service-export/` |
| 方法论 (01-09) | `talk-method/source-code-analysis/methodology/zh/` |
| 阶段总览 | `../HANDOFF-STAGE3.md` |
| 执行计划 (原始 7 域, 已审计修正) | `issue/源码分析执行计划.md` 阶段5.2 |
| MCP 索引 (已就绪) | project=`data-workspace-source-code-code-spring-dubbo` (69088 节点) |

---

## §七 下一步行动 (阶段收尾 — 11 域全部完成)

```
✅ 全部 11 域定稿 + 深审收敛 (outlines/ 12 目录 + harness/ 12 目录 + knowledge-planning/ 11 份):
  D-1 SPI 微内核 (六轮) | D-2 服务导出 (四轮) | D-3 服务引用 (四轮) | D-4 RPC 调用 (四轮)
  D-5 注册中心 (四轮) | D-6 负载均衡 (四轮) | D-7 集群容错 (四轮) | D-8a 传输抽象 (四轮)
  D-8b HTTP 传输栈 (四轮) | D-9 Triple 协议 (四轮) | D-10 序列化 (四轮) | D-11 元数据 (四轮)
  + knowledge-planning/ 11 份 KP 已 REVIEW (R1 锚点 re-grep 全过 / R2 转录一致性全过 / R3 引出链全过 / R4 数量对应全过, 0 处修复)

收尾工作 (按阶段节奏建议):
1. 每域可进入写作阶段 (大纲反写测试全过, 锚点已验证; 写作时行号仍须 re-grep)
2. 阶段 5 收尾对照: gRPC (G-1~G-3) — D-9 Triple 协议对照; Sentinel — 治理面对照
3. 本文件 §零 状态表已同步 (11/11 ✅)
```

**环境**: 本仓库 MCP 索引已就绪 (检索用 search_graph/trace_path 优先, 降级链 JavaLens→codegraph→grep 按 05 规范; 本会话实际以 codebase-memory + grep 完成全部验证)。
