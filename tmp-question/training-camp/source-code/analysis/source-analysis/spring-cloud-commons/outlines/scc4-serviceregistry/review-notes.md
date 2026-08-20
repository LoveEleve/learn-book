# SCC-4 服务注册抽象 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 跨域一致性)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 6 | **语义补全** | **shouldRegisterManagement 三条件** (L176-184): ① registerManagement 属性默认 true ② getManagementPort() != null ③ **ManagementServerPortUtils.isDifferent — 管理端口与主端口不同** — 大纲原只说"管理注册面" | 已修 §3 |
| 7 | **钩子分离** | RegistrationManagementLifecycle **新增 4 个管理专用方法** (*StartRegisterManagement/*StopRegisterManagement) + registrationManagementLifecycles **独立字段** (L70, 构造器注入 L82-86) — 管理钩子与主钩子分离 | 已修 §5 |
| 8 | **默认值修正** | **failFast 默认 false** (AutoServiceRegistrationProperties.java:29-31) — 默认无实现不报错静默; enabled/registerManagement 默认 true (L25-28) | 已修 §6 |
| 9 | 遗漏装配 | **ServiceRegistryAutoConfiguration** — ServiceRegistryEndpoint 装配 (@ConditionalOnBean(ServiceRegistry) + @ConditionalOnAvailableEndpoint) | 已修 §6 |
| 10 | 验证通过 | onApplicationEvent management 跳过语义 / 负面空间"不做心跳续约" (serviceregistry 包零心跳) / SCC-3→SCC-4 autoRegister 联动 | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | ServiceRegistry 类 **L26** (写 L31-64) + register L33 + deregister L39 + close L44 + setStatus L53 + getStatus L62 | 已修 |
| 2 | 锚点漂移 | start() 事件: InstancePreRegisteredEvent **L153** + register L156 + InstanceRegisteredEvent **L169** + running CAS **L170** (写 L152-168) | 已修 |
| 3 | 锚点漂移 | stop() **L294-311** (写 L298-310) + deregister L299 + close **L311** | 已修 |
| 4 | 锚点漂移 | RegistrationLifecycle **L27** (写 L25-56) + DEFAULT_ORDER L32; failFast 抛 **L42** (写 L36-40) | 已修 |
| 5 | 验证通过 | Abstract L49-50 / onApplicationEvent L111-119 / 抽象方法 L190-258 / 装配 L30 | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniRegistry 17/17 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | 验证通过 | 触发链完整: WebServer 事件 → port CAS → start → Pre 事件 → 钩子 → register → Registered 事件 → running 双检 | 17/17 |
| H2 | 验证通过 | running 双检防重复: 二次事件不重复注册 — 真实语义实证 | 17/17 |

## 锚点密度统计

- file:line 锚点数: **25+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 5 项检查 4 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (客户端内建/心跳续约/重试注册/失败降级/多实例/注销保护)
- [x] 每条有对照物 (Nacos BeatReactor/Eureka Renew)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (自动注册/触发时机/钩子/管理端口/无实现启动)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (事件 → start → 仪式 → register → 事件 → stop 镜像)
- [x] 边界交代: management 跳过/双检/isEnabled/failFast

## 方法论教训

- **"仪式化流程"的锚点必须逐行对** — start() 的 Pre 事件/钩子/register/Registered 事件/running CAS 五步行号全漂移, 手写行号不可信
- **harness 的 running 双检实证** — 二次 WebServer 事件不重复注册是真实语义, 测试天然覆盖
