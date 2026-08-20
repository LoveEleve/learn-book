# OF-1 注册机制 — @EnableFeignClients 扫描与装配

> 前置: 无 (框架入口) | 引出: [[OF-2-代理创建与装配]] [[OF-7-配置隔离]] | 对照: @ComponentScan 扫描 + MyBatis MapperScan
> 🔴 A | 8 KP | [模式: ImportBeanDefinitionRegistrar + 类路径扫描]
> Pass 2 闭环: q1(注解入口) q2(扫描面) q3(注册面) q4(配置注册)

**读者处境**: @EnableFeignClients 写一行注解, 所有 @FeignClient 接口怎么被发现并变成可注入的 Bean? 这篇拆 FeignClientsRegistrar (503) + 扫描器 + BeanDefinition 装配。

### 1. 注解与入口面 — @EnableFeignClients + registerBeanDefinitions

场景: 注解怎么触发注册?
源码路径:
- **@EnableFeignClients 5 组属性** (L50-88): value/basePackages/basePackageClasses/**defaultConfiguration**/clients (显式跳过扫描)
- ⚠ **@FeignClient 12 属性** (FeignClient.java): value/name/contextId/url/path/configuration/fallback/fallbackFactory/dismiss404/qualifiers/**primary (默认 true)** — 客户端声明面
- **FeignClientsRegistrar implements ImportBeanDefinitionRegistrar** (L71) + **registerBeanDefinitions** (L152-155): registerDefaultConfiguration → registerFeignClients
- ⚠ **getUrl SpEL 排除** (L): url 以 "#{" 开头且含 "}" → 不处理 (运行时表达式 URL)
关键设计 (q1): **Import 机制触发 + clients() 显式路径 + 默认配置独立注册**。[模式: 入口面]

### 2. 扫描面 — ClassPathScanningCandidateComponentProvider

场景: 扫描哪些包? 怎么找?
源码路径:
- **getScanner** (L379): new ClassPathScanningCandidateComponentProvider(**false**, environment) — 不用默认过滤器; ⚠ **覆写 isCandidateComponent** (L382-391): **isIndependent && !isAnnotation → 接口可作候选** (Spring 默认扫描器排除接口! 这是 @FeignClient 接口能被扫到的关键)
- **AnnotationTypeFilter(FeignClient.class)** (L179) — 只收 @FeignClient
- **getBasePackages 四级兜底** (L393+): value → basePackages → basePackageClasses → **importingClass 所在包** (默认扫启动类包)
- findCandidateComponents (L181)
关键设计 (q2): **Spring 标准扫描器复用 + 类型过滤器精确 + 四级包解析兜底**。[模式: 扫描面]

### 3. 注册面 — 懒/急双模式 + BeanDefinition

场景: 接口怎么变成 BeanDefinition?
源码路径:
- **懒/急分流** (L210-215): lazy-attributes-resolution (默认 false) → eagerly/lazilyRegister
- **eagerlyRegister** (L222): validate (fallback/fallbackFactory 校验 L83-87: **!isInterface 必须实现 @FeignClient 接口**) + genericBeanDefinition(FeignClientFactoryBean.class) + **10 属性** (url/path/name/contextId/type/dismiss404/fallback/fallbackFactory/**refreshableClient**/qualifiers)
- ⚠ **name/contextId 解析** (L327-345): **getName: serviceId→name→value 三级回退**; getContextId: contextId 属性→无则回退 getName→占位符 resolve
- ⚠ **refresh 联动** (L490,500): **refreshableClient = spring.cloud.openfeign.client.refresh-enabled (默认 false)**; true 时 **BeanDefinition.setScope("refresh")** — OF-9 机制前置
关键设计 (q3): **FactoryBean 作为 BeanDefinition 工厂 (代理创建在 OF-2) + 注册期校验早暴露 + refreshableClient 联动 OF-9**。[模式: 注册面]

### 4. 配置注册面 — FeignClientSpecification

场景: 每客户端配置怎么注册?
源码路径:
- **registerClientConfiguration** (L463-470): genericBeanDefinition(FeignClientSpecification) + **3 构造参数** (name/className/configuration) + "name.FeignClientSpecification" 命名
- **FeignClientSpecification implements NamedContextFactory.Specification** (L29) — SCC C-13 关联!
关键设计 (q4): **Specification 对接命名子上下文 (OF-7 输入) + 默认/客户端双注册 (继承结构)**。[模式: 配置面]

## 代码类型
Architecture (装配机制) + Spring 容器机制

## 负面空间 (OF-1, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不运行时扫描 | 启动时一次性注册 (q1) |
| 不自定义条件过滤 | 仅类型过滤器 (q1) |
| 不包外扫描 | 只扫声明包 (q2) |
| 不扫描结果缓存 | 每次启动全扫 (q2) |
| 不代理预创建 | 注册期只建 BeanDefinition (q3) |
| 不配置合并 | 默认/客户端各自独立 (q4) |

## 结尾桥 OUTBOUND

- → [[OF-2-代理创建与装配]]: BeanDefinition 触发 FeignClientFactoryBean.getObject → 代理创建
- → [[OF-7-配置隔离]]: FeignClientSpecification → NamedContextFactory 子上下文
- → 对照: @ComponentScan (同源扫描器) / MyBatis MapperScan (ImportBeanDefinitionRegistrar 同类)
