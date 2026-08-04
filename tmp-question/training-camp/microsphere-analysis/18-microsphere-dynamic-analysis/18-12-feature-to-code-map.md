# 18-12 功能→代码映射手册

按功能点查找对应的核心类和调用链路。

---

## 功能 1：配置读取与解析

**功能**：从 `application.properties` 中读取 JSON 配置，解析为 `DynamicJdbcConfig` 对象。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `DynamicJdbcConfigUtils` | `util/DynamicJdbcConfigUtils.java` | 配置读取入口 |
| `DynamicJdbcConfig` | `config/DynamicJdbcConfig.java` | 配置 POJO |
| `DataSourceConstants` | `datasource/constants/DataSourceConstants.java` | JDBC URL 属性名常量 |

### 调用链路

```
Environment.getProperty("microsphere.dynamic.jdbc.configs.xxx")
  → DynamicJdbcConfigUtils.getDynamicJdbcConfigs()
    → 扫描 microsphere.dynamic.jdbc.configs.* 前缀下的所有属性名
    → 对每个属性名，调 getDynamicJdbcConfig()
      → getDynamicJdbcConfigContent() 读取值（支持 inline JSON 或 classpath:）
      → Jackson ObjectMapper.readValue() 反序列化为 DynamicJdbcConfig
      → 强制 dynamic=true（如果用户写了 false）
      → 如果 name 未设置，从 propertyName 后缀自动生成
    → 返回 Map<String, DynamicJdbcConfig>
```

### 关键代码

`DynamicJdbcConfigUtils.java:69-118` — `getDynamicJdbcConfigs()` 和 `getDynamicJdbcConfig()`

### 配置来源

JSON 值可以来自：
- `application.properties` 中直接写 JSON 字符串
- `classpath:config/xxx.json` 外部文件
- Nacos/Apollo 等配置中心的属性推送（通过 `PropertySourcesChangedEvent`）

---

## 功能 2：6 步处理管道

**功能**：将 `DynamicJdbcConfig` 对象转换为 Spring Bean 定义和 Environment 属性。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `DynamicJdbcContextProcessor` | `context/DynamicJdbcContextProcessor.java` | 管道编排器 |
| `AnnotationConfigUtils` | Spring 内部（非本模块） | 注册基础设施 BeanPostProcessor |

### 调用链路

```
DynamicJdbcContextProcessor.process(config, propertyName, context)
  │
  ├─ Step 1: registerAnnotationConfigProcessors()
  │   → 注册 ConfigurationClassPostProcessor、AutowiredAnnotationBeanPostProcessor 等
  │   → 没有这些，子上下文无法处理 @Configuration、@Autowired 注解
  │
  ├─ Step 2: postProcessDynamicJdbcConfig()
  │   → 遍历 ConfigPostProcessor SPI（6 个实现）
  │   → DynamicJdbcConfigPostProcessor: 设置 name，注入 BeanFactory
  │   → DataSourcePropertiesConfigPostProcessor: 填充 DataSource 默认值
  │   → 其他 4 个 PostProcessor: 空操作
  │
  ├─ Step 3: validateDynamicJdbcConfig()
  │   → 遍历 ConfigValidator SPI（6 个实现）
  │   → 验证失败抛 ConfigValidationException，启动阻断
  │
  ├─ Step 4: processDynamic() [仅 dynamic=true 时]
  │   → 注册 DynamicDataSource 的 BeanDefinition（传入深克隆的 DynamicJdbcConfig，克隆包含完整配置）
  │   → 从原始 DynamicJdbcConfig 中移除 DataSource/HA/ShardingSphere 配置
  │   → 后果：Step 5 运行时原始 config 的 dataSource 已为空
  │   → 所以 DataSourceConfigurationPropertiesSynthesizer.supports() 返回 false（无数据源可合成）
  │   → DataSource 属性由 DynamicDataSource 内部通过克隆的配置自行处理
  │   → 但 Transaction/MyBatis/ShardingSphere 的合成不受影响（它们不依赖 dataSource 字段）
  │
  ├─ Step 5: processDynamicJdbcConfigurationProperties()
  │   → 遍历 ConfigConfigurationPropertiesSynthesizer SPI（5 个实现）
  │   → TransactionConfig 合成 spring.transaction.*
  │   → ShardingSphereConfig 合成 spring.shardingsphere.datasource.* + spring.shardingsphere.props.*
  │   → MyBatis/MP Config 合成 mybatis.base-packages / mybatis-plus.base-packages
  │   → 注册为 MapPropertySource 到 Environment
  │   → 如果是子上下文，添加 spring.autoconfigure.exclude 排除
  │
  └─ Step 6: registerDynamicJdbcConfigBeanDefinitions()
      → 遍历 ConfigBeanDefinitionRegistrar SPI（5 个实现）
      → 注册 DynamicJdbcConfig 自身、TransactionManager、ShardingSphere Mode/Rule、Mapper 扫描配置
```

### 关键代码

`DynamicJdbcContextProcessor.java:50-71` — `process()` 管道的 6 步定义

---

## 功能 3：DynamicDataSource 热替换

**功能**：运行时不重启，切换 DataSource（Zone 切换或配置变更时）。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `DynamicDataSource` | `datasource/DynamicDataSource.java` | 热替换 DataSource 壳 |
| `RefreshingDynamicDataSourceListener` | `DynamicDataSource.java` 内部类 | 监听配置变更事件 |
| `DynamicJdbcChildContext` | `context/DynamicJdbcChildContext.java` | 子上下文 |

### 调用链路

```
启动时：
  DynamicDataSource 的 BeanDefinition 在 Step 4 注册
  → Spring 在 finishBeanFactoryInitialization 时实例化
    → afterPropertiesSet()
      → initializeApplicationListeners() 注册 RefreshingDynamicDataSourceListener
      → 如果 delegate==null，调用 initializeDataSource()

initializeDataSource():
  createDynamicDataSourceConfig(config)
    → cloneDynamicJdbcConfig()（Jackson 序列化再反序列化）
    → setDynamic(false)（防止递归）
    → 移除 transaction/mybatis/mybatis-plus
    → 重命名为 "Dynamic#{name}#datasource"
  → new DynamicJdbcChildContext(dsConfig, propertyName, parentContext, idGenerator)
  → mergeParentEnvironment()
  → refresh()（完整 Spring 启动）
  → getDataSource(childContext)（获取唯一的 DataSource Bean）
  → synchronized(mutex)
    → delegate = latestDataSource
    → childContext = dynamicDataSourceChildContext
    → closeOldContext(async=true, delay=60s)

切换时：
  DynamicJdbcConfigChangedEvent
    → RefreshingDynamicDataSourceListener.onApplicationEvent()
      → 检查 propertyName 是否匹配
      → findParentContext() 三级匹配
      → initializeDataSource(config, propertyName, parentContext) 重建
```

### 关键代码

`DynamicDataSource.java:155-201` — `initializeDataSource()`
`DynamicDataSource.java:216-221` — `createDynamicDataSourceConfig()`
`DynamicDataSource.java:269-286` — `findParentContext()` 三级匹配
`DynamicDataSource.java:289-306` — `RefreshingDynamicDataSourceListener`

---

## 功能 4：Zone 感知的 HA 数据源

**功能**：根据当前 Zone 选择对应的数据库配置，Zone 切换时自动重建。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `DynamicJdbcConfig` | `config/DynamicJdbcConfig.java` | HA 配置选择 |
| `PropagatingDynamicJdbcConfigChangedEventListener` | `context/PropagatingDynamicJdbcConfigChangedEventListener.java` | Zone 变更事件转发 |
| `DynamicJdbcConfigChangedEvent` | `context/DynamicJdbcConfigChangedEvent.java` | 配置变更事件 |
| `ZoneContext` | 17-multiactive | 当前 Zone 值持有者 |

### 调用链路

```
配置选择：
  DynamicJdbcConfig.getDataSourcePropertiesList()
    → 如果有 ha-datasource
      → zoneContext.getZone() 获取当前 zone
      → 从 highAvailabilityDataSourcePropertiesMap 中取对应 zone 的数据源列表
      → 如果该 zone 不存在，回退到 defaultZone
    → 如果是普通 datasource，直接返回

Zone 切换：
  ZoneContext.setZone("zone-b")
    → 发布 ZoneContextChangedEvent
      → PropagatingDynamicJdbcConfigChangedEventListener.onApplicationEvent()
        → isZoneChanged() 检查 zone 属性是否变化
        → 遍历所有 config，只处理 hasHighAvailabilityDataSource()=true 的
        → 重新读取配置：getDynamicJdbcConfig(environment, propertyName)
        → 发布 DynamicJdbcConfigChangedEvent
          → RefreshingDynamicDataSourceListener
            → initializeDataSource()（此时 getDataSourcePropertiesList() 返回新 zone 的配置）
```

### 关键代码

`DynamicJdbcConfig.java:229-244` — `getDataSourcePropertiesList()` Zone 选择
`PropagatingDynamicJdbcConfigChangedEventListener.java:74-84` — `onZoneContextChangedEvent()`

---

## 功能 5：配置热更新

**功能**：Nacos/Apollo 等配置中心推送变更时，自动重建 DataSource。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `PropagatingDynamicJdbcConfigChangedEventListener` | `context/PropagatingDynamicJdbcConfigChangedEventListener.java` | 监听配置变更 |
| `PropertySourcesChangedEvent` | 外部（17-configuration） | 配置属性变更事件 |

### 调用链路

```
外部配置变更（Nacos/Apollo）
  → PropertySourcesChangedEvent
    → PropagatingDynamicJdbcConfigChangedEventListener.onPropertySourcesChangedEvent()
      → event.getChangedProperties().keySet()
      → 遍历 key，检查是否匹配 dynamicJdbcConfigPropertyNames
      → 如果匹配：publishDynamicJdbcConfigChangedEvent(key)
        → getDynamicJdbcConfig(environment, propertyName) 读取最新 JSON
        → publishEvent(new DynamicJdbcConfigChangedEvent(context, config, propertyName))
          → RefreshingDynamicDataSourceListener
            → initializeDataSource() 重建
```

### 关键代码

`PropagatingDynamicJdbcConfigChangedEventListener.java:63-71` — `onPropertySourcesChangedEvent()`
`PropagatingDynamicJdbcConfigChangedEventListener.java:99-106` — `publishDynamicJdbcConfigChangedEvent()`

---

## 功能 6：MyBatis / MyBatis-Plus 隔离并存

**功能**：同一个进程里 MyBatis 和 MyBatis-Plus 各自的 Mapper 互不干扰。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `MybatisConfigValidator` | `mybatis/validation/MybatisConfigValidator.java` | 确保不与 MP 同时配置 |
| `MybatisPlusConfigValidator` | `mybatisplus/validation/MybatisPlusConfigValidator.java` | 确保不与 MyBatis 同时配置 |
| `MybatisConfigurationConfigBeanDefinitionRegistrar` | `mybatis/context/MybatisConfigurationConfigBeanDefinitionRegistrar.java` | 注册 MyBatis Mapper 扫描 |
| `MybatisPlusConfigurationConfigBeanDefinitionRegistrar` | `mybatisplus/context/MybatisPlusConfigurationConfigBeanDefinitionRegistrar.java` | 注册 MP Mapper 扫描 |
| `MybatisMapperScanConfiguration` | `mybatis/context/MybatisMapperScanConfiguration.java` | `@MapperScan` 定义 |
| `MybatisPlusMapperScanConfiguration` | `mybatisplus/context/MybatisPlusMapperScanConfiguration.java` | `@MapperScan` 定义 |
| `DynamicJdbcChildContext` | `context/DynamicJdbcChildContext.java` | 隔离容器 |

### 调用链路

```
配置层互斥（Validator）：
  MybatisConfigValidator.doValidate()
    → 如果同一个 config 同时配置了 mybatis 和 mybatis-plus，报错

Auto-Configuration 层互斥（banned-modules）：
  Step 5 Synthesizer 将 banned-modules 追加到 spring.autoconfigure.exclude
    → mybatis 激活时，禁止 mybatis-plus 的 Auto-Configuration
    → mybatis-plus 激活时，禁止 mybatis 的 Auto-Configuration

隔离并存（不同 config）：
  config A: { "mybatis": { "base-packages": "com.a.mapper" } }
  config B: { "mybatis-plus": { "base-packages": "com.b.mapper" } }
  → A 运行在 DynamicJdbcChildContext[A] 中，B 运行在 DynamicJdbcChildContext[B] 中
  → 各自独立执行 Auto-Configuration，互不干扰

Mapper 扫描：
  MybatisMapperScanConfiguration（@MapperScan("${mybatis.base-packages}")）
    → Step 6 Registrar 注册
    → 子上下文 refresh 时执行 Mapper 扫描
    → 扫描到的 Mapper 绑定到当前子上下文的 SqlSessionFactory
```

### 关键代码

`MybatisConfigValidator.java` — 配置层互斥
`MybatisMapperScanConfiguration.java:14` — `@MapperScan` 注解
`MybatisConfigurationConfigBeanDefinitionRegistrar.java` — Mapper 扫描配置注册

### 已知限制

- 同一个子上下文内不能同时使用 MyBatis 和 MyBatis-Plus（Validator 阻止）
- 所有子上下文共享 ClassLoader，不能同时加载两个版本的 MyBatis

---

## 功能 7：ShardingSphere 集成

**功能**：在子上下文中集成 ShardingSphere 5.x 的分库分表能力。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `ShardingSphereConfigConfigurationPropertiesSynthesizer` | `shardingsphere/env/ShardingSphereConfigConfigurationPropertiesSynthesizer.java` | 合成分片数据源属性 |
| `ShardingSphereConfigurationConfigBeanDefinitionRegistrar` | `shardingsphere/context/ShardingSphereConfigurationConfigBeanDefinitionRegistrar.java` | 注册 Mode/Rule Bean |
| `ShardingSphereConfigValidator` | `shardingsphere/validation/ShardingSphereConfigValidator.java` | 验证 YAML 文件存在 |
| `ShardingSphereShutdownHookThreadFilter` | `shardingsphere/context/ShardingSphereShutdownHookThreadFilter.java` | 过滤 ShardingHook 线程 |
| `DynamicJdbcUtils` | `util/DynamicJdbcUtils.java` | 加载 ShardingSphere YAML |
| `SyncExecutionShutdownHookApplicationListener` | `env/SyncExecutionShutdownHookApplicationListener.java` | 同步执行 shutdown hook |

### 调用链路

```
Step 3（验证）：
  ShardingSphereConfigValidator
    → 检查 config-resource 是否为空
    → 检查 config-resource 指向的文件是否存在

Step 5（属性合成）：
  ShardingSphereConfigConfigurationPropertiesSynthesizer.synthesize()
    → synthesizeDataSourceProperties()
      → 从 DynamicJdbcConfig 的 dataSource 字段
      → 合成 spring.shardingsphere.datasource.names=ds_0,ds_1
      → 合成 spring.shardingsphere.datasource.ds_0.jdbcUrl=...
    → synthesizeYamlProps()
      → 加载 YAML：DynamicJdbcUtils.loadShardingSphereYamlRootConfiguration()
      → YamlEngine.unmarshal(yamlContent, YamlRootConfiguration.class)
      → 从 YAML 提取 props 部分
      → 合成 spring.shardingsphere.props.sql-show=false

Step 6（Bean 注册）：
  ShardingSphereConfigurationConfigBeanDefinitionRegistrar.register()
    → 再次加载 YAML
    → ModeConfigurationYamlSwapper.swapToObject() → ModeConfiguration Bean
    → YamlRuleConfigurationSwapperEngine.swapToRuleConfiguration() → RuleConfiguration Bean × N

子上下文 refresh：
  ShardingSphereAutoConfiguration（来自 shardingsphere-jdbc-core-spring-boot-starter）
    → 注入 ModeConfiguration + List<RuleConfiguration>
    → 从 Environment 绑定 spring.shardingsphere.* 属性
    → 创建 ShardingSphereDataSource

Shutdown hook：
  SyncExecutionShutdownHookApplicationListener（在 ApplicationStartedEvent 时注册）
    → 找到名称以 "DelayedShutdownHook-for-" 开头的线程
    → 在 ContextClosedEvent 时用 Thread.run() 同步执行
```

### 关键代码

`ShardingSphereConfigConfigurationPropertiesSynthesizer.java` — 属性合成
`ShardingSphereConfigurationConfigBeanDefinitionRegistrar.java` — Bean 注册
`SyncExecutionShutdownHookApplicationListener.java` — shutdown hook 管理

### 已知限制

- YAML 被解析两次（Synthesizer + Registrar）
- 不支持 Cluster 模式在子上下文中的 ZK 连接生命周期管理
- 仅 Memory 模式推荐用于子上下文

---

## 功能 8：Bean 升迁

**功能**：子上下文中的 DataSource、TransactionManager、SqlSessionFactory 注册到父上下文。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `DynamicJdbcChildContextRefreshedListener` | `context/DynamicJdbcChildContextRefreshedListener.java` | 升迁入口 |
| `ParentContextBeanNameGenerator` | `context/ParentContextBeanNameGenerator.java` | SPI：生成父 Bean 名 |
| `PlatformTransactionManagerBeanNameGenerator` | `transaction/context/PlatformTransactionManagerBeanNameGenerator.java` | TransactionManager 命名 |

### 调用链路

```
子上下文 finishRefresh
  → ContextRefreshedEvent
    → DynamicJdbcChildContextRefreshedListener.onApplicationEvent()
      ├─ registerParentContextClosedEventListener()
      │   → 父上下文关闭时关闭本子上下文
      └─ registerParentBeansFromChildContext()（仅 registerParentBeans=true 时）
          → 遍历子上下文所有 BeanDefinition 名称
          → 跳过基础设施 Bean（BeanPostProcessor、environment 等）
          → 对每个业务 Bean：
            → childContext.getBean(beanName) 获取实例
            → generateParentBeanName()
              → 遍历 ParentContextBeanNameGenerator SPI
                → PlatformTransactionManagerBeanNameGenerator：用 transaction.name
              → 默认：childContextId + "$" + beanName
            → registerParentBean(parentBeanName, childBean)
              → 如果是 exposed 类（DataSource/TransactionManager/SqlSessionFactory）
                → registerBean(registry, name, bean, primary)
                → 其中 primary 由 dynamicJdbcConfig.isPrimary() + multipleContextPrimaryBeanClasses 决定
              → 如果不是 exposed 类
                → registerFactoryBean(registry, name, bean)
```

### 关键代码

`DynamicJdbcChildContextRefreshedListener.java:76-91` — `registerParentBeansFromChildContext()`
`DynamicJdbcChildContextRefreshedListener.java:117-124` — `registerParentBean()`
`DynamicJdbcChildContextRefreshedListener.java:137-153` — `isPrimaryBean()` 和 `isExposedBeanClass()`

### exposed 和 primary 的控制配置

```properties
# 哪些类型的 Bean 直接暴露
microsphere.dynamic.jdbc.multiple-context.bean-classes.expose=\
  javax.sql.DataSource,\
  org.springframework.transaction.PlatformTransactionManager,\
  org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers,\
  org.apache.ibatis.session.SqlSessionFactory

# 哪些类型的 Bean 支持 @Primary 标记
microsphere.dynamic.jdbc.multiple-context.bean-classes.primary=\
  javax.sql.DataSource,\
  org.springframework.transaction.PlatformTransactionManager,\
  org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers,\
  org.apache.ibatis.session.SqlSessionFactory
```

---

## 功能 9：Auto-Configuration 过滤

**功能**：子上下文只加载其模块需要的 Auto-Configuration，不加载无关的。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `DynamicJdbcAutoConfigurationImportSelector` | `autoconfigure/DynamicJdbcAutoConfigurationImportSelector.java` | 自定义 ImportSelector |
| `DynamicJdbcAutoConfigurationImportFilter` | `autoconfigure/DynamicJdbcAutoConfigurationImportFilter.java` | 包名前缀过滤 |
| `DynamicJdbcAutoConfigurationImportListener` | `autoconfigure/DynamicJdbcAutoConfigurationImportListener.java` | 缓存过滤结果 |
| `DynamicJdbcAutoConfigurationRepository` | `autoconfigure/DynamicJdbcAutoConfigurationRepository.java` | 缓存存储 |

### 调用链路

```
1. 子上下文 refresh → @EnableDynamicJdbcAutoConfiguration
    → @Import(DynamicJdbcAutoConfigurationImportSelector.class)
      → selectImports()
        → 查缓存（ConcurrentHashMap<ClassLoader, String[]>）
        ├─ 缓存命中 → 直接返回
        └─ 缓存未命中 → super.selectImports()
           → 标准三阶段流程
             → 阶段 2: DynamicJdbcAutoConfigurationImportFilter.match()
               → 遍历 basePackages（从 Environment 读取各模块配置）
               → autoConfigurationClassName.startsWith(basePackage)
               → 同时检查 ClassUtils.isPresent(className, classLoader)
             → 阶段 3: AutoConfigurationImportEvent
               → DynamicJdbcAutoConfigurationImportListener.onAutoConfigurationImportEvent()
                 → filter::match 重新过滤
                 → DynamicJdbcAutoConfigurationRepository.cache(classLoader, matchedNames)
           → 再次读取缓存 → 命中 → 返回

2. 子上下文销毁 → DynamicJdbcAutoConfigurationImportSelector.destroy()
    → DynamicJdbcAutoConfigurationRepository.clear()
```

### 关键代码

`DynamicJdbcAutoConfigurationImportSelector.java:34-55` — `selectImports()` 缓存优先
`DynamicJdbcAutoConfigurationImportFilter.java:39-47` — `match()` 包名前缀 + class 存在检查
`DynamicJdbcAutoConfigurationImportListener.java:35-44` — `onAutoConfigurationImportEvent()` 缓存
`DynamicJdbcAutoConfigurationRepository.java:34-63` — `ConcurrentHashMap<ClassLoader, String[]>` 缓存

---

## 功能 10：模块互斥（banned-modules）

**功能**：确保互斥的模块（如 MyBatis ↔ MyBatis-Plus，DataSource ↔ ShardingSphere）不会在同一个子上下文中激活。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `DynamicJdbcPropertyUtils` | `util/DynamicJdbcPropertyUtils.java` | 读取 banned-modules 配置 |
| `AbstractConfigConfigurationPropertiesSynthesizer` | `env/AbstractConfigConfigurationPropertiesSynthesizer.java` | 将排除追加到 exclude |

### 调用链路

```
Step 5 Synthesizer：
  AbstractModuleConfigConfigurationPropertiesSynthesizer.synthesize()
    → synthesizeModuleExclusionAutoConfigurationProperty(module)
      → getModuleExclusionAutoConfigurationClassNames(context, module)
        → 读取 microsphere.dynamic.jdbc.modules.{module}.auto-configuration.banned-modules
        → 对每个被禁模块名，获取其 Auto-Configuration 类名
          → getModuleAutoConfigurationClassNames(context, bannedModule)
            → DynamicJdbcAutoConfigurationRepository 查询
      → appendCommaDelimitedPropertyValues(properties, "spring.autoconfigure.exclude", classNames)
    → 合成的 PropertySource 被加到 Environment
    → spring.autoconfigure.exclude 生效，排除列表中的 Auto-Configuration 不会运行
```

### 默认互斥关系

| 模块 | 禁止的模块 | 原因 |
|------|-----------|------|
| datasource | sharding-sphere | 两套 DataSource Auto-Configuration 冲突 |
| sharding-sphere | datasource | 同上（双向互斥） |
| ha-datasource | sharding-sphere | HA 数据源不支持 ShardingSphere |
| mybatis | mybatis-plus | 不能同时在同一个上下文用两个 ORM |
| mybatis-plus | mybatis | 同上（双向互斥） |

### 关键代码

`DynamicJdbcPropertyUtils.java:152-161` — `getModuleExclusionAutoConfigurationClassNames()`
`AbstractConfigConfigurationPropertiesSynthesizer.java` — `synthesizeModuleExclusionAutoConfigurationProperty()`

---

## 功能 11：子上下文创建

**功能**：为每个 DynamicJdbcConfig 创建独立的 Spring 子上下文。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `DynamicJdbcContextApplicationListener` | `context/DynamicJdbcContextApplicationListener.java` | 入口 |
| `DynamicJdbcChildContext` | `context/DynamicJdbcChildContext.java` | 子上下文 |
| `DynamicJdbcChildContextConfiguration` | `context/DynamicJdbcChildContextConfiguration.java` | 子上下文配置类 |
| `DynamicJdbcChildContextIdGenerator` | `context/DynamicJdbcChildContextIdGenerator.java` | ID 生成 |
| `InitializeErrors` | `context/error/InitializeErrors.java` | 错误收集 |

### 调用链路

```
ApplicationPreparedEvent
  → DynamicJdbcContextApplicationListener.onApplicationEvent()
    → isDisable(context) 检查是否启用
    → processDynamicJdbcContext()
      → getDynamicJdbcConfigs(environment) 读取所有配置
      → 如果 size == 0，直接返回
      → registerPropagatingDynamicJdbcConfigChangedEventListener()
      → registerSyncExecutionShutdownHookApplicationListener()（有 ShardingSphere 时）
      → 如果 size == 1（单 config）：
        → processDynamicJdbcContext(entry, context)
          → 在当前上下文直接执行 6 步管道
      → 如果 size > 1（多 config）：
        → processDynamicJdbcChildContexts()
          → ThreadPoolExecutor(parallelism = configs.size())
          → 并行创建 N 个 DynamicJdbcChildContext
            → new DynamicJdbcChildContext(config, propertyName, parentContext)
            → childContext.registerParentBeans()（设标志）
            → childContext.mergeParentEnvironment()
            → childContext.refresh()
              → postProcessBeanFactory()
                → 设置 ClassLoader = parent.getClassLoader()
                → ConfigurationPropertySources.attach()
                → 注册 DynamicJdbcChildContextRefreshedListener
                → 注册 DynamicJdbcChildContextConfiguration
                → 执行 DynamicJdbcContextProcessor.process()（6 步管道）
          → 等待所有完成
          → 如果有错误，抛 DynamicJdbcInitializeException
          → appendExclusionAutoConfigurationProperty(context)
          → executor.shutdownNow()
```

### 关键代码

`DynamicJdbcContextApplicationListener.java:48-56` — `onApplicationEvent()` 入口
`DynamicJdbcContextApplicationListener.java:62-92` — `processDynamicJdbcContext()` 单/多 config 判断
`DynamicJdbcContextApplicationListener.java:116-160` — `processDynamicJdbcChildContexts()` 并行创建
`DynamicJdbcChildContext.java:92-107` — `postProcessBeanFactory()` 子上下文初始化

### 子上下文 ID 规则

```java
// 普通子上下文
DynamicJdbcChildContext[{config.name}]
// 示例: DynamicJdbcChildContext[orders]

// DynamicDataSource 创建的子上下文
Dynamic#DynamicJdbcChildContext[{config.name}]#datasource
// 示例: Dynamic#DynamicJdbcChildContext[orders]#datasource
```

---

## 功能 12：URL 标准化与默认值填充

**功能**：用户可以用三种字段名写 JDBC URL，可以省略 scheme，可以省略查询参数。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `JdbcURLAssembler` | `datasource/config/JdbcURLAssembler.java` | URL 组装 |
| `DataSourcePropertiesConfigPostProcessor` | `datasource/config/DataSourcePropertiesConfigPostProcessor.java` | 默认值填充 |
| `URLUtils` | `util/URLUtils.java` | URL 查询参数工具 |

### 调用链路

```
Step 2 PostProcessor：
  DataSourcePropertiesConfigPostProcessor.processDataSourceUrl()
    → 从 dataSourceProperties 中查找 url / jdbcUrl / jdbc-url（三选一）
    → JdbcURLAssembler.assemble(url)
      → normalize()
        → 从 URL 中提取 jdbc: 和 // 之间的 scheme（如 mysql）
        → 如果提取到 scheme（说明已有 jdbc: 前缀），去掉 jdbc: 前缀保留 scheme://host/db
        → 如果没有 scheme（只有 host/db），添加默认 scheme（默认 "mysql//"）
      → URI.create(url)
      → URLUtils.parseQueryParams(uri) 解析查询参数
      → setDefaultQueryParamsIfAbsent()
        → defaultQueryParams 中未设置的参数追加到 URL
        → 默认参数：characterEncoding=utf-8, useSSL=false, useUnicode=true
      → 重建 URL（JDBC_URL_PREFIX + rebuiltURL）
    → 写回 dataSourceProperties

默认值填充：
  DataSourcePropertiesConfigPostProcessor
    → type 未设置 → 默认 com.zaxxer.hikari.HikariDataSource
    → driverClassName 未设置 → 从 URL 自动推导（DatabaseDriver.fromJdbcUrl()）
    → username 未设置 → 从上一个数据源继承 → 从 Environment 读取默认用户名
    → password 未设置 → 同上
    → name 未设置 → 自动生成 "{propertyName}-datasource-{index}"
    → 其余属性 → 从上一个数据源复制（processDataSourceOthers）
```

### 关键代码

`JdbcURLAssembler.java:43-64` — `assemble()` 标准化流程
`DataSourcePropertiesConfigPostProcessor.java:41-58` — `postProcess()` 填充入口
`DataSourcePropertiesConfigPostProcessor.java:67-76` — `processDataSourceProperties()` 逐个字段填充

---

## 功能索引：按类名查找

| 类名 | 负责的功能 |
|------|-----------|
| `DynamicJdbcContextApplicationListener` | 入口、子上下文创建、事件监听器注册 |
| `DynamicJdbcContextProcessor` | 6 步管道编排 |
| `DynamicJdbcChildContext` | 子上下文初始化、Environment 合并 |
| `DynamicJdbcChildContextRefreshedListener` | Bean 升迁、子上下文关闭传播 |
| `DynamicDataSource` | 热替换、延迟关闭、事件响应 |
| `DataSourcePropertiesConfigPostProcessor` | 默认值填充、URL 标准化、"前一个填充" |
| `DynamicJdbcConfig` | 配置 POJO、Zone 感知选择 |
| `DynamicJdbcConfigUtils` | JSON 解析、克隆、命名生成 |
| `DynamicJdbcPropertyUtils` | 属性读取、banned-modules、别名解析 |
| `DynamicJdbcUtils` | ShardingSphere YAML 加载、属性展平 |
| `JdbcURLAssembler` | JDBC URL 标准化 |
| `ConfigurationPropertiesFlatter` | 嵌套属性展平 |
| `DynamicJdbcAutoConfigurationImportSelector` | Auto-Configuration 选择、缓存 |
| `DynamicJdbcAutoConfigurationImportFilter` | 包名前缀匹配过滤 |
| `DynamicJdbcAutoConfigurationImportListener` | 缓存过滤结果 |
| `DynamicJdbcAutoConfigurationRepository` | 缓存存储 |
| `PropagatingDynamicJdbcConfigChangedEventListener` | Zone 切换转发、配置热更新转发 |
| `ShardingSphereConfigConfigurationPropertiesSynthesizer` | ShardingSphere 属性合成 |
| `ShardingSphereConfigurationConfigBeanDefinitionRegistrar` | ShardingSphere Bean 注册 |
| `SyncExecutionShutdownHookApplicationListener` | ShardingSphere shutdown hook |
| `MybatisConfigurationConfigBeanDefinitionRegistrar` | MyBatis Mapper 扫描注册 |
| `MybatisPlusConfigurationConfigBeanDefinitionRegistrar` | MyBatis-Plus Mapper 扫描注册 |
| `DynamicJdbcDefaultPropertiesPostProcessor` | default.properties 加载 |
| `OnceApplicationPreparedEventListener` | 防重复处理（ConcurrentSkipListSet） |
| `InitializeErrors` | 并行初始化错误收集 |
