# 13-02：Spring 集成与测试基建

> **核心命题**：13-01 的拦截管道是纯 MyBatis 世界的（core 模块零 Spring 依赖）；本篇文章讲它怎么进 Spring：三套注解（`@EnableMyBatis` 注册工厂、`@MyBatisConfiguration` 构建 Configuration、`@EnableMyBatisExtension` 装配管道）+ Registrar 体系 + 自动配置，以及一套完整的 JUnit5 测试基建（`MyBatisTestExtension` + 10 个 resolver）。顺带给出 features.yaml 的"官方 MyBatis SPI 全景图"。

---

## 一、三套注解的分工（先看全景）

```
@EnableMyBatis（用户标注）
  → MyBatisBeanDefinitionRegistrar
    → 注册 SqlSessionFactoryBean（ifAbsent）
    → 注册 SqlSessionTemplate（ifAbsent）

@MyBatisConfiguration（用户标注，可省略）
  → MyBatisConfigurationBeanDefintionRegistrar
    → 注册官方 Configuration bean（ifAbsent）——注解几十个属性对应 Configuration 全部 setter

@EnableMyBatisExtension（用户标注 或 spring-boot 自动配置标）
  → MyBatisExtensionBeanDefinitionRegistrar
    → 三源扫描 ExecutorFilter / ExecutorInterceptor
    → 有货才注册 InterceptingExecutorInterceptor bean
    → SqlSessionFactoryBeanPostProcessor 把拦截器注入 SqlSessionFactoryBean.plugins
```

**职责边界**：`@EnableMyBatis` 管"工厂怎么建"、`@MyBatisConfiguration` 管"配置对象长什么样"、`@EnableMyBatisExtension` 管"管道装什么"——三件事独立，可以只开其中一部分。

---

## 二、Registrar 体系

### 2.1 ifAbsent 幂等（基类 MyBatisImportBeanDefinitionRegistrar）

```java
protected void registerBeanDefinitionIfAbsent(AnnotationAttributes attributes, BeanDefinitionRegistry registry,
                                              String beanName, Function<...> beanDefinitionFunction) {
    if (registry.containsBeanDefinition(beanName)) {
        logger.info("The BeanDefinition named '{}' already exists. Skipping registration.", beanName);
        return;   // ← 已存在就跳过
    }
    registerBeanDefinition(registry, beanName, beanDefinitionFunction.apply(attributes));
}
```

**幂等设计的意义**：与官方 `mybatis-spring-boot` 共存的前提——官方自动配置也会注册 `sqlSessionFactory`/`sqlSessionTemplate`（`@ConditionalOnMissingBean`），microsphere 的 `@EnableMyBatis` 先到先得、后到跳过，**两边不冲突**（AutoConfigureAfter 保证顺序，见第四节）。

### 2.2 @EnableMyBatis：SqlSessionFactoryBean 的注解化构建

```java
BeanDefinition buildSqlSessionFactoryBeanDefinition(AnnotationAttributes attributes) {
    checkConfigLocation(attributes);
    BeanDefinitionBuilder builder = genericBeanDefinition(SqlSessionFactoryBean.class);
    setBeanReferencePropertyValue(builder, attributes, "dataSource", DataSource.class);   // dataSource 属性 → Bean 引用（默认 "*" = primary）
    setConfiguration(builder, attributes);           // configLocation 或 Configuration bean
    setPropertyValue(builder, attributes, "mapperLocations");      // Mapper XML 位置（占位符解析）
    setPackagePropertyValue(builder, attributes, "typeAliasesPackage");
    setPropertyValue(builder, attributes, "typeAliasesSuperType", Object.class);
    setPackagePropertyValue(builder, attributes, "typeHandlersPackage");
    setPropertyValue(builder, attributes, "vfs", VFS.class);
    ...
}
```

**注意点**：
- `dataSource` 默认 `"*"`（`WILDCARD`）——**主 DataSource**，多数据源场景（18-dynamic）需显式指定 bean 名
- 所有字符串属性支持**占位符解析**（`${...}`——配置中心友好，呼应 15-configuration）
- `SqlSessionTemplate` bean 同样 ifAbsent 注册

### 2.3 @MyBatisConfiguration：注解 → 官方 Configuration bean

`MyBatisConfigurationBeanDefintionRegistrar` 把注解的几十个属性（`cacheEnabled`/`lazyLoadingEnabled`/`aggressiveLazyLoading`/`useColumnLabel`/...）逐一 set 到官方 `Configuration` bean，其中三个需要类型转换（Registrar 源码）：

```java
if ("lazyLoadTriggerMethods".equals(attributeName)) {
    attributeValue = ofSet(methods);          // String[] → Set
} else if ("proxyFactory".equals(attributeName)) {
    attributeValue = newInstance(proxyFactoryClass);   // Class → 实例
} else if ("variables".equals(attributeName)) {
    attributeValue = stringArrayToProperties(variables);  // "k=v" String[] → Properties
}
```

**价值**：官方 `mybatis-spring-boot` 的 `MybatisProperties` 也干类似的事（`@ConfigurationProperties` 绑定）——microsphere 用**注解属性 + 手动构建 BeanDefinition** 的路线，**不依赖官方属性类**（`features.yaml` 里 `MybatisProperties` 只登记未依赖）。

---

## 三、SqlSessionFactoryBeanPostProcessor：管道注入（反射 + 去重）

```java
class SqlSessionFactoryBeanPostProcessor extends GenericBeanPostProcessorAdapter<SqlSessionFactoryBean> {
    private final InterceptingExecutorInterceptor interceptingExecutorInterceptor;

    @Override
    protected void processBeforeInitialization(SqlSessionFactoryBean bean, String beanName) {
        Interceptor[] plugins = getFieldValue(bean, "plugins");     // ← 反射读私有字段
        if (contains(plugins, this.interceptingExecutorInterceptor)) {
            return;                                                 // ← 去重（防重复注入）
        }
        bean.addPlugins(this.interceptingExecutorInterceptor);
    }
}
```

**注入时机**：`SqlSessionFactoryBean` 初始化**之前**（BeanPostProcessor），把 `InterceptingExecutorInterceptor` 塞进 `plugins` 数组——之后 `SqlSessionFactoryBean` 构建 SqlSessionFactory 时（`Configuration.addInterceptor` 流程）它自然进入官方 `InterceptorChain`，13-01 的挂载链路闭环。

**问题**：反射读 `plugins` 私有字段（与 13-01 P1 同款模式）——官方 `SqlSessionFactoryBean` 升级改字段名即静默失效（null → 去重判断空指针或重复注入）。

---

## 四、自动配置与条件注解

### 4.1 两把开关 + 装配

```
microsphere.mybatis.enabled（默认 true，PropertyConstants）
  → @ConditionalOnMyBatisEnabled
    → @ConditionalOnMyBatisAvailable（meta：Enabled + @ConditionalOnClass(SqlSession/SqlSessionFactory)）
      → 用于 MyBatisAutoConfiguration / MyBatisCloudAutoConfiguration
```

与 16-gateway 的开关体系同构（`microsphere.xxx.enabled` + `ConditionalOnXxxAvailable` 两层）。

### 4.2 spring-boot：与官方共存的自动配置

```java
@ConditionalOnMyBatisAvailable
@EnableMyBatisExtension                       // ← 自动开启管道装配
@AutoConfigureAfter(name = {
        "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"   // 官方
})
public class MyBatisAutoConfiguration {
}
```

**AutoConfigureAfter 的含义**：官方 `MybatisAutoConfiguration` 先执行（其 `sqlSessionFactory` 等 bean 标注 `@ConditionalOnMissingBean`——本地 2.2.2 字节码验证；3.0.x 目标版本未本地验证，机制大概率一致）——microsphere 的 `@EnableMyBatis` 只在用户没用官方时兜底（ifAbsent）；官方在场时**管道照样装配**（`@EnableMyBatisExtension` 独立于 `@EnableMyBatis`）——**"工厂用官方的，管道用 microsphere 的"**，这是与官方共存的设计核心。

### 4.3 spring-cloud：空壳

`MyBatisCloudAutoConfiguration` 只有条件注解 + AutoConfigureAfter，**无任何内容**——与 13-01 探索一致（占位模块；真正的 Spring Cloud 能力（如配置中心驱动管道开关）未实现）。

### 4.4 features.yaml：官方 MyBatis SPI 全景图（学习地图）

`microsphere-mybatis-spring-cloud/src/main/resources/META-INF/config/default/features.yaml` 把 **19 个官方 MyBatis SPI 扩展点**全部登记为 actuator `/features` 报告（05 模块机制）：

```
mybatis: Executor / Interceptor / TypeHandler / LanguageDriver / DatabaseIdProvider /
  DataSourceFactory / SqlSessionFactory / TransactionFactory / Configuration / Cache /
  KeyGenerator / ProxyFactory / ParameterHandler / ResultContext / ResultSetHandler /
  StatementHandler / ResultHandler / SqlSource / ReflectorFactory
mybatis-spring: SqlSessionTemplate / MapperFactoryBean / MapperScannerConfigurer / ...
mybatis-spring-boot-autoconfigure: MybatisProperties / ConfigurationCustomizer / ...
microsphere-mybatis-core: ExecutorFilter / ExecutorInterceptor
```

**知识价值**：这是"MyBatis 有哪些扩展点"的现成清单——配合 13-01 的 Executor 管道，可以回答"microsphere 覆盖了 19 个扩展点里的哪 2 个、为什么只覆盖这 2 个"（13-01 2.1 的能力边界）。

---

## 五、测试基建（54 主文件中 test 模块占 29 个——过半壁江山）

### 5.1 经典体系：Abstract*Test 分层

```
AbstractMyBatisTest（基类）
  @BeforeEach init():
    buildDefaultSqlSessionFactory()（H2 内存库）
    customize(factory) / customize(configuration)（子类钩子）
    initData()（跑 INIT_DB_SCRIPT_RESOURCE_NAME 建表脚本）
  → AbstractSqlSessionTest（doInSqlSession 辅助）
    → AbstractExecutorTest（MS_ID_* 常量 + doInExecutor）
    → AbstractMapperTest（doInMapper）
```

**测试体验**：`initData()` 每次测试前重跑建表脚本——**每个测试方法独立数据环境**（隔离性好，代价是慢）。

### 5.2 JUnit5 体系：MyBatisTestExtension + 10 个 resolver

```java
public class MyBatisTestExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback,
        TestInstancePostProcessor, ParameterResolver {
    private static final List<ComponentResolver> componentResolvers =
            loadServicesList(ComponentResolver.class);   // ← Java SPI 加载
    // 生命周期：beforeAll 建环境 → postProcessTestInstance 字段注入 → resolveParameter 参数注入
    //          → afterEach 清理（事务回滚）→ afterAll 关闭
}
```

- **10 个 resolver**（`META-INF/services` 注册）：Configuration/Connection/DataSource/Environment/Executor/MapperComponent/Properties/SqlSessionFactory/SqlSession/Transaction——按参数类型/字段类型自动注入
- **`@MyBatisTest` 组合注解**（`@ExtendWith(MyBatisTestExtension.class)` + configResource/environment 等属性）——类级标注即启用整套运行时
- **`@MyBatisRuntime` 是注入的前置条件**：`ComponentResolver.supportsParameter`（61 行）与字段注入（72 行）都要求 `isMyBatisRuntime(parameter/field)`（137 行：`isAnnotationPresent(MyBatisRuntime.class)`）——**不带注解的字段/参数不会被注入**（防止误注入测试类里的业务字段）
- `spring-test` 模块补 `MyBatisDataSourceTestConfiguration`（configuration bean + DataSource bean）

**分析价值**：这套测试基建本身是"JUnit5 扩展开发"的完整教材（生命周期回调 + 参数解析器 + SPI 发现 + 字段注入）——与 16 的"测试当行为文档"理念一致。

---

## 六、问题清单（已证）

| # | 问题 | 证据 |
|---|------|------|
| S1 | 拼写错误：`MyBatisConfigurationBeanDefintionRegistrar`（Defintion 少 i） | 类名（git 全历史未见修复） |
| S2 | spring-cloud 模块空壳（自动配置无内容） | `MyBatisCloudAutoConfiguration` 源码 |
| S3 | `SqlSessionFactoryBeanPostProcessor` 反射读 `plugins` 私有字段 | 源码（`getFieldValue`） |
| S4 | `@EnableMyBatis` 的 `dataSource="*"` 默认主数据源——多数据源场景（18-dynamic）必须显式指定，否则错绑 | 注解默认值 + Registrar 逻辑 |
| S5 | `@MyBatisConfiguration` 与官方 `MybatisProperties` 功能重叠（两条路线）——使用者需选边 | 两套配置体系对照 |

---

## 七、小结（引用要点）

- **三套注解三件事**：工厂注册（ifAbsent 幂等）/ Configuration 构建（注解属性 → setter）/ 管道装配（三源扫描 + 有货才注册）。
- **与官方共存的核心**：`AutoConfigureAfter` + `@ConditionalOnMissingBean`/ifAbsent——"工厂用官方的，管道用 microsphere 的"。
- **反射模式复现**：`SqlSessionFactoryBean.plugins` 反射读（S3）——与 13-01 P1、16-05 风险 B 同一作者惯用模式，升级脆弱性在生态里系统性存在。
- **测试基建是教材**：JUnit5 扩展 + SPI resolver 体系（test 模块占主代码一半）。
- **features.yaml 是地图**：19 个官方 SPI 扩展点清单，microsphere 只覆盖其中 2 个（ExecutorFilter/ExecutorInterceptor）——能力边界一目了然。
