# 18-02 数据库框架共存问题

## 目录

- [为什么 MyBatis 和 MyBatis-Plus 不能共存](#为什么-mybatis-和-mybatis-plus-不能共存)
- [问题根源：Spring Boot Auto-Configuration 冲突](#问题根源spring-boot-auto-configuration-冲突)
- [框架共存的三种方案](#框架共存的三种方案)
- [18-dynamic 的共存方案：子上下文隔离](#18-dynamic-的共存方案子上下文隔离)
- [Transaction 管理器的共存](#transaction-管理器的共存)
- [ShardingSphere 与其他框架的共存](#shardingsphere-与其他框架的共存)
- [共存方案的代价](#共存方案的代价)

---

## 为什么 MyBatis 和 MyBatis-Plus 不能共存

这是 18-dynamic 模块要解决的核心问题之一。在传统的单 Spring 上下文应用中，MyBatis 和 MyBatis-Plus 如果同时使用 Auto-Configuration，无法在同一个容器中正常工作。原因不是技术上的不可能（可以手动配置实现共存，见方案 B），而是 Spring Boot Auto-Configuration 的设计导致的。

### 问题 1：竞争 DataSource 的 SqlSessionFactory

MyBatis 和 MyBatis-Plus 都会注册一个 `SqlSessionFactory` Bean：

```java
// MyBatis Auto-Configuration
@Bean
@ConditionalOnMissingBean
public SqlSessionFactory sqlSessionFactory(DataSource dataSource) { ... }

// MyBatis-Plus Auto-Configuration
@Bean
@ConditionalOnMissingBean
public SqlSessionFactory mybatisSqlSessionFactory(DataSource dataSource) { ... }
```

两者都有 `@ConditionalOnMissingBean`——意味着谁先加载谁生效，后加载的不会覆盖。但即使加载顺序确定了，它们各自需要注册的 **Bean 集合不同**，关键冲突在于 `SqlSessionFactory` 只能存在一个：

| 组件 | MyBatis 注册的 Bean | MyBatis-Plus 注册的 Bean | 冲突类型 |
|------|-------------------|------------------------|---------|
| SqlSessionFactory | ✅（bean名不同但类型相同） | ✅（bean名不同但类型相同） | `@ConditionalOnMissingBean` 二选一 |
| SqlSessionTemplate | ✅ | ✅ | 共享同一个 SqlSessionFactory |
| MapperScannerConfigurer | ✅（扫描 `@Mapper`） | ✅（扫描 `@Mapper`） | 各自绑定到不同 SqlSessionFactory |
| MybatisProperties | ✅ | ❌ | 不冲突 |
| MybatisPlusProperties | ❌ | ✅ | 不冲突 |

两个 MapperScannerConfigurer 各自扫描配置的 `base-packages`，每个 Mapper 接口被注册为 Bean 时绑定到创建它的 SqlSessionFactory。但由于只有一个 SqlSessionFactory 被创建（`@ConditionalOnMissingBean` 导致），另一个框架的 Mapper 绑定到了一个不存在的 SqlSessionFactory，运行时找不到对应的 SqlSessionFactory 而报错。

### 问题 2：SqlSessionFactory 绑定冲突

```java
// MyBatis 的 MapperScan
@MapperScan("com.example.mapper")
// MyBatis-Plus 的 MapperScan
@MapperScan("com.example.mapper")
```

即使你用不同的 `basePackages`（比如 MyBatis 扫 `mapper/`，MyBatis-Plus 扫 `other/`），MapperScannerConfigurer 在注册 Mapper Bean 时也存在绑定冲突。

每个 MapperScannerConfigurer 在扫描 Mapper 接口并注册 BeanDefinition 时，会将 Mapper 绑定到创建它的 `SqlSessionFactory`。如果两个框架的 MapperScannerConfigurer 各自注册了 Mapper 到不同的 SqlSessionFactory，但最终只有一个 SqlSessionFactory 被创建（由于 `@ConditionalOnMissingBean`），绑定到"不存在的" SqlSessionFactory 的 Mapper 在运行时找不到工厂而报错。

此外，Mapper 的 Bean 注册按 beanName（接口全限定名）去重，所以同一个接口不能被注册到两个 SqlSessionFactory——这意味着你不能让一个 Mapper 同时走 MyBatis 和 MyBatis-Plus。

### 问题 3：Configuration 对象冲突

MyBatis-Plus 在启动时会修改 `org.apache.ibatis.session.Configuration` 的配置（如添加 `MybatisPlusInterceptor`、全局配置等）。如果 MyBatis 先加载，MyBatis-Plus 后加载，MP 的某些配置可能覆盖 MyBatis 的，反之亦然。

核心冲突在于：**两个 ORM 框架都要控制同一个 SqlSessionFactory 的 Configuration，但 Configuration 没有隔离机制**。

### 问题 4：事务管理冲突

`DataSourceTransactionManager` 绑定到特定的 `DataSource`。如果 MyBatis 和 MyBatis-Plus 共享同一个 DataSource 和同一个 TransactionManager，`@Transactional` 在运行时不会区分事务归属——两个框架的 Mapper 调用都使用同一个事务。

这看似没问题，但实际情况复杂：MyBatis-Plus 有自己的事务拦截器（`MybatisPlusInterceptor` 中的事务相关逻辑），它假设自己控制的 SqlSessionFactory 中的事务行为。如果 MyBatis 先创建了 SqlSessionFactory，MP 的 SqlSessionFactory 没有被创建，MP 的事务拦截器逻辑不会被执行——但 MP 的 Mapper 可能被绑定到了 MyBatis 的 SqlSessionFactory 上，导致 MP 期望的事务行为（如自动填充、乐观锁的重试）在事务上下文中表现异常。

反之，如果 MP 的 SqlSessionFactory 覆盖了 MyBatis 的，MyBatis 的简单事务行为可能被 MP 的复杂事务逻辑影响。

---

## 问题根源：Spring Boot Auto-Configuration 冲突

问题的根本原因不是 MyBatis 或 MyBatis-Plus 代码本身有问题，而是 **Spring Boot 的 Auto-Configuration 机制在单 ApplicationContext 中工作时，假设了"每种类型的 Bean 只有一个"**。

### Auto-Configuration 的设计假设

Spring Boot 的 `@EnableAutoConfiguration` 的核心逻辑是：

1. 扫描所有 Auto-Configuration 类（从 `AutoConfiguration.imports`）
2. 对每个类，检查 `@Conditional` 条件（如 `@ConditionalOnClass`、`@ConditionalOnMissingBean`）
3. 匹配的类中的 `@Bean` 方法依次执行
4. 如果多个 `@Bean` 方法都定义同名的 Bean，**按加载顺序决定谁生效**

```java
// Spring Boot 内部的逻辑（简化）：
// ConfigurationClassBeanDefinitionReader 注册 @Bean 方法时
// 如果 beanName 已经存在，会根据 allowBeanDefinitionOverriding 决定是否覆盖
// 默认情况下，后加载的会覆盖先加载的
```

这意味着：
- 如果 MyBatis 和 MyBatis-Plus 都定义 `SqlSessionFactory` 类型的 Bean，**谁后加载谁覆盖**
- 覆盖后，所有注入 `SqlSessionFactory` 的地方都指向同一个实例
- 被覆盖的框架的 Mapper 可能绑定到了错误的 SqlSessionFactory，运行时 behavior 异常

加载顺序由 `@AutoConfigureOrder`、`@AutoConfigureAfter`、`@AutoConfigureBefore` 等注解控制，也受类全限定名的字母序影响。具体哪个框架覆盖哪个，取决于类路径扫描的顺序，是不可预测的。

### @ConditionalOnMissingBean 的限制

`@ConditionalOnMissingBean` 看起来能解决问题——先加载的框架注册了 Bean，后加载的就不注册了。但有两个问题：

**问题 A：谁先谁后不可控**

Auto-Configuration 的加载顺序由 `@AutoConfigureOrder`、`@AutoConfigureBefore`、`@AutoConfigureAfter` 注解控制。MyBatis 和 MyBatis-Plus 的 Auto-Configuration 类之间没有明确的顺序约定，所以谁先加载取决于 classpath 扫描顺序，不可预测。

```java
// MyBatis 的 MybatisAutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
// → 只在 DataSource 之后，没有和 MyBatis-Plus 的相对顺序

// MyBatis-Plus 的 MybatisPlusAutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
// → 同上，没有和 MyBatis 的相对顺序
```

**问题 B：`@ConditionalOnMissingBean` 是按类型匹配的**

```java
// 两个方法的返回类型都是 SqlSessionFactory
// 所以如果一个先注册了，另一个就不会注册
// 但未注册的框架的 MapperScannerConfigurer 等注解（如 @MapperScan）
// 仍然在运行，扫描到的 Mapper 无法绑定到不存在的 SqlSessionFactory
```

假设 MyBatis 先加载，MyBatis-Plus 的 `@ConditionalOnMissingBean` 检测到已有 `SqlSessionFactory` 类型的 Bean，就不创建了。但：

1. MyBatis-Plus 的 Mapper 扫描器（`MybatisPlusAutoConfiguration` 中）依赖于内部 `MybatisPlusProperties`
2. MyBatis-Plus 的 SQL 拦截器（分页、乐观锁等）需要注册到 Configuration 中
3. 如果 MyBatis-Plus 的 SqlSessionFactory 没有被创建，这些拦截器就不会被注册
4. 使用 MyBatis-Plus 的 Mapper 时，分页、乐观锁等功能不可用

反之亦然——MyBatis-Plus 先加载，MyBatis 的 `@ConditionalOnMissingBean` 就不创建了，MyBatis 的 Mapper 也不能正常工作。

**核心矛盾**：`@ConditionalOnMissingBean` 的语义是"这个 Bean 类型不存在时才创建"，但两个框架需要的不是"二选一"的 SqlSessionFactory，而是**各自独立**的 SqlSessionFactory。`@ConditionalOnMissingBean` 的设计假设就是"同类型的 Bean 只需要一个"，这对共存场景不适用。

### 完整冲突清单

| 资源类型 | 冲突原因 |
|---------|---------|
| `SqlSessionFactory` | 两个框架都定义，`@ConditionalOnMissingBean` 让后加载的不生效 |
| `MapperScannerConfigurer` | 两个框架都定义 Mapper 扫描，但扫描到的 Mapper 只能绑定到一个 SqlSessionFactory |
| `Configuration` (MyBatis 核心) | MyBatis-Plus 的拦截器、全局配置会修改它，两个框架不能共享同一个 Configuration 对象 |
| `MybatisProperties` / `MybatisPlusProperties` | 两个框架各自的 `@ConfigurationProperties` 前缀不同不冲突，但都依赖同一个 `DataSource` |
| `PlatformTransactionManager` | 两个框架共享同一个 DataSourceTransactionManager，无法区分事务归属 |
| `SqlSessionTemplate` | 两个框架各自注册，但都绑定到同一个 SqlSessionFactory |

---

## 框架共存的三种方案

解决了"为什么不能共存"和"问题根源"之后，看看有哪些可行的共存方案。

### 方案 A：各自独立的数据源 + 独立的 ApplicationContext

这就是 18-dynamic 的方案。

```
App 进程
  ├── ApplicationContext A
  │     ├── DataSource A
  │     ├── MyBatis + MyBatis-Config A
  │     ├── SqlSessionFactory A
  │     └── Mapper 扫描 A
  │
  └── ApplicationContext B
        ├── DataSource B
        ├── MyBatis-Plus + MyBatis-Plus-Config B
        ├── SqlSessionFactory B
        └── Mapper 扫描 B
```

每个 ORM 框架在自己的子上下文中运行完整的 Auto-Configuration，完全隔离。子上下文之间不共享任何 Bean。

**优点**：完全隔离，没有任何冲突。
**缺点**：每个子上下文需要独立的 DataSource 实例（但可以是同一个数据库的不同连接池）。

### 方案 B：同一个 DataSource + 多个 SqlSessionFactory

```
App 进程（单 ApplicationContext）
  ├── DataSource (共享)
  ├── SqlSessionFactory A (MyBatis)
  │     └── Mapper 扫描 A → 绑定到 A
  ├── SqlSessionFactory B (MyBatis-Plus)
  │     └── Mapper 扫描 B → 绑定到 B
  └── 两个 PlatformTransactionManager (同一 DataSource)
```

通过手动配置，绕过 Auto-Configuration，在同一个上下文中创建两个 SqlSessionFactory：

```java
@Configuration
public class DualORMConfig {

    // ----- MyBatis 的 SqlSessionFactory -----
    @Bean
    public SqlSessionFactory myBatisSqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.example.mybatis.entity");
        // 只扫描 MyBatis Mapper
        MyBatisConfiguration config = new MyBatisConfiguration();
        factoryBean.setConfiguration(config);
        return factoryBean.getObject();
    }

    // ----- MyBatis-Plus 的 SqlSessionFactory -----
    @Bean
    public SqlSessionFactory myBatisPlusSqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.example.mybatisplus.entity");
        MybatisPlusConfiguration config = new MybatisPlusConfiguration();
        config.addInterceptor(new PaginationInterceptor());
        factoryBean.setConfiguration(config);
        return factoryBean.getObject();
    }

    // 两个 SqlSessionTemplate，各绑一个 SqlSessionFactory
    @Bean
    public SqlSessionTemplate myBatisSqlSessionTemplate(
            @Qualifier("myBatisSqlSessionFactory") SqlSessionFactory factory) {
        return new SqlSessionTemplate(factory);
    }

    @Bean
    public SqlSessionTemplate myBatisPlusSqlSessionTemplate(
            @Qualifier("myBatisPlusSqlSessionFactory") SqlSessionFactory factory) {
        return new SqlSessionTemplate(factory);
    }
}
```

**优点**：共享同一个 DataSource，进程内资源少。
**缺点**：
- 必须手动配置，放弃 Auto-Configuration
- 两个框架的拦截器、全局配置容易互相影响
- `@Transactional` 注解不能区分走哪个框架

### 方案 C：不同的 Mapper 包路径 + 手动路由

```
App 进程（单 ApplicationContext）
  ├── DataSource (共享)
  ├── SqlSessionFactory (只一个，MP 的，因 MP 功能覆盖 MyBatis)
  │     ├── MyBatis Mapper 扫描
  │     └── MyBatis-Plus Mapper 扫描
  └── Mapper 接口中通过继承/组合判断用哪个框架的功能
```

利用 MyBatis-Plus 兼容 MyBatis 的特性——MP 的 SqlSessionFactory 可以执行 MyBatis 的 Mapper（因为 MP 的 Mapper 继承自 MyBatis 的 Mapper，MP 的 SqlSessionFactory 继承自 MyBatis 的 SqlSessionFactory）。这样实际只有一个 SqlSessionFactory，只是 Mapper 分为两类：继承 `BaseMapper`（有 MP 功能）和不继承的（纯 MyBatis）。

**优点**：最简单，不需要任何额外框架。
**缺点**：
- MP 的全局配置（拦截器、乐观锁、分页）会影响所有 Mapper——包括纯 MyBatis 的
- 如果只是想用 MyBatis 写纯 SQL 而不用 MP 的 CRUD 封装，可以。但如果两个框架的配置有冲突（比如不同的日志格式、不同的事务策略），就行不通了

---

## 18-dynamic 的共存方案：子上下文隔离

18-dynamic 采用了**方案 A**（独立子上下文），但做了一些工程化改进：

### 基本单元：DynamicJdbcConfig

```json
{
  "name": "order-db-mybatis",
  "dataSource": [{ "name": "order", "url": "jdbc:mysql://host/orders" }],
  "mybatis": { "base-packages": "com.example.orders.mapper" }
}
```

这个 JSON 定义了一个"数据库单元"——它指定了数据源 + ORM 框架 + 配置。每个单元运行在一个独立的 `DynamicJdbcChildContext` 中。

### 共存示例：两个 config，两个 ORM

```json
{
  "microsphere.dynamic.jdbc.configs.orders": {
    "name": "orders",
    "dataSource": [{ "name": "ds", "url": "jdbc:mysql://host1/orders" }],
    "mybatis": { "base-packages": "com.example.orders.mapper" },
    "transaction": { "name": "orders-tx" }
  },
  "microsphere.dynamic.jdbc.configs.users": {
    "name": "users",
    "dataSource": [{ "name": "ds", "url": "jdbc:mysql://host2/users" }],
    "mybatis-plus": { "base-packages": "com.example.users.mapper" },
    "transaction": { "name": "users-tx" }
  }
}
```

此时进程内有：

```
主 Spring Context
  ├── Bean: orders$dataSource (升迁自子上下文 A)
  ├── Bean: orders$transactionManager (升迁自子上下文 A)
  ├── Bean: users$dataSource (升迁自子上下文 B)
  ├── Bean: users$transactionManager (升迁自子上下文 B)
  │
  ├── DynamicJdbcChildContext[orders] (隔离区 A)
  │     ├── DataSource (HikariCP → host1/orders)
  │     ├── MyBatis Auto-Configuration 完整执行
  │     ├── SqlSessionFactory (MyBatis 版本)
  │     ├── Mapper: com.example.orders.mapper.*
  │     └── PlatformTransactionManager "orders-tx"
  │
  └── DynamicJdbcChildContext[users] (隔离区 B)
        ├── DataSource (HikariCP → host2/users)
        ├── MyBatis-Plus Auto-Configuration 完整执行
        ├── SqlSessionFactory (MyBatis-Plus 版本)
        ├── Mapper: com.example.users.mapper.*
        └── PlatformTransactionManager "users-tx"
```

### 子上下文如何做到隔离

`DynamicJdbcChildContext` 继承了 `AnnotationConfigApplicationContext`，是一个全新的 Spring 容器：

```java
DynamicJdbcChildContext childCtx = new DynamicJdbcChildContext(config, propertyName, parentContext);
childCtx.registerParentBeans();
childCtx.mergeParentEnvironment();
childCtx.refresh();  // 完整执行 Auto-Configuration
```

关键点在 `refresh()`——它会：

1. 调用 `postProcessBeanFactory`（准备 Environment、注册 Listener、注册配置类）
2. 执行 BeanFactoryPostProcessor（包括 `ConfigurationClassPostProcessor` —— 处理 `@Configuration`、`@Import`、`@ComponentScan`）
3. 注册 BeanPostProcessor
4. 初始化所有 singleton Bean
5. 触发 `ContextRefreshedEvent`

这意味着子上下文内的 `@EnableMyBatis`、`@MapperScan` 等注解会被**重新处理**，与父上下文完全独立。

**隔离的关键机制**：

| 隔离维度 | 实现方式 |
|---------|---------|
| Bean 定义隔离 | 每个子上下文有独立的 `DefaultListableBeanFactory` |
| Auto-Configuration 隔离 | `DynamicJdbcChildContextConfiguration` 只带 `@EnableDynamicJdbcAutoConfiguration`，通过 base-packages 匹配 + banned-modules 排除来控制哪些 Auto-Configuration 进入子上下文 |
| 属性隔离 | `mergeParentEnvironment()` 复制父 Environment 然后选择性覆盖 |
| 关闭隔离 | 父上下文关闭时自动关闭所有子上下文（`registerParentContextClosedEventListener`） |

### 不能共享同一个数据库的限制

18-dynamic 的子上下文隔离方案有一个根本限制：**每个子上下文需要一个独立的 DataSource 配置**。不是说必须不同的数据库，而是 DataSource 实例必须不同。

如果你想实现"同一个 DataSource，MyBatis 和 MyBatis-Plus 共存"，这个方案不合适。你需要方案 B（多个 SqlSessionFactory 共享同一个 DataSource）。

18-dynamic 的设计场景是**每个 Zone 的配置不同**——同城双活中 Zone A 的数据库和 Zone B 的数据库本身就是一个独立实例，所以需要独立的子上下文。而在这个上下文中，"顺便解决"了框架共存问题。

### ClassLoader 共享的限制

所有子上下文共享父上下文的 ClassLoader。这意味着：**同一个 JVM 中不能同时加载两个版本的 MyBatis**。如果子上下文 A 需要 MyBatis 3.5.x，子上下文 B 需要 3.6.x，它们在同一个 ClassLoader 中只能存在一个版本。这是 Java 类加载机制的限制，不是 18-dynamic 特有的。

在实践中，这意味着所有子上下文必须使用同一套 MyBatis/MyBatis-Plus 的 JAR 版本。框架共存是指"配置并存"（一个用 MyBatis、一个用 MP），不是"版本并存"（一个用 3.5.x、一个用 3.6.x）。如果确实需要版本隔离，需要为子上下文配置独立的 ClassLoader——18-dynamic 目前不支持。

### banned-modules：模块互斥的 Auto-Configuration 层控制

`banned-modules` 是 18-dynamic 的模块互斥机制，它在 Auto-Configuration 层面确保：当一个模块激活时，被禁止的模块的 Auto-Configuration 不会在同一个子上下文中运行。

#### 配置层：default.properties

```properties
# MyBatis 模块禁止 MyBatis-Plus 模块的 Auto-Configuration
microsphere.dynamic.jdbc.modules.mybatis.auto-configuration.banned-modules=mybatis-plus

# MyBatis-Plus 模块禁止 MyBatis 模块的 Auto-Configuration
microsphere.dynamic.jdbc.modules.mybatis-plus.auto-configuration.banned-modules=mybatis

# DataSource 模块禁止 ShardingSphere 模块的 Auto-Configuration
microsphere.dynamic.jdbc.modules.datasource.auto-configuration.banned-modules=sharding-sphere

# ShardingSphere 模块禁止 DataSource 模块的 Auto-Configuration
microsphere.dynamic.jdbc.modules.sharding-sphere.auto-configuration.banned-modules=datasource
```

#### 执行层：DynamicJdbcPropertyUtils

```java
public static Set<String> getModuleExclusionAutoConfigurationClassNames(
        ConfigurableApplicationContext context, String module) {

    // 读取当前模块的 banned-modules 配置
    Set<String> bannedModules = getModuleAutoConfigurationBannedModules(environment, module);

    // 对每个被禁止的模块，获取它的所有 Auto-Configuration 类名
    Set<String> exclusionClassNames = new LinkedHashSet<>();
    bannedModules.forEach(bannedModule -> {
        Set<String> bannedClassNames = getModuleAutoConfigurationClassNames(context, bannedModule);
        exclusionClassNames.addAll(bannedClassNames);
    });

    return exclusionClassNames;
}
```

#### 完整流程

```
假设 config 中配置了 mybatis 模块：

1. DynamicJdbcContextProcessor 执行到第 5 步（属性合成）
2. MybatisConfigConfigurationPropertiesSynthesizer 的 synthesize() 被调用
3. 内部调用 synthesizeModuleExclusionAutoConfigurationProperty(module)
4. → getModuleExclusionAutoConfigurationClassNames(context, "mybatis")
5.   → 读取 microsphere.dynamic.jdbc.modules.mybatis.auto-configuration.banned-modules
6.   → 得到 "mybatis-plus"
7.   → 查 mybatis-plus 模块的 Auto-Configuration 类名
8.     → 扫描 classpath，找到 com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration 等
9.   → 将这些类名加到 spring.autoconfigure.exclude 中
10. → MyBatis-Plus 的 Auto-Configuration 不会在本子上下文中运行
```

#### 为什么这是必要的

即使 config 中只配置了 `mybatis` 没有配置 `mybatis-plus`，如果没有 banned-modules，Subcontext 中仍然可能加载 MyBatis-Plus 的 Auto-Configuration——因为 Spring Boot 扫描 Auto-Configuration 是基于 classpath 的全量扫描，不是因为你的配置中声明了什么。

```java
// Spring Boot 的 AutoConfigurationImportSelector（2.x）
// 从 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// 加载 ALL auto-config classes——不管你的配置中有没有声明使用它们
List<String> configurations = getCandidateConfigurations(metadata, attributes);
```

banned-modules 确保：**"我的模块激活时，你的模块不要来"**。

#### 与 Validator 层互斥的关系

banned-modules 说的是 Auto-Configuration 层面的互斥，Validator 层说的是配置层面的互斥：

```java
// MybatisConfigValidator.java
protected void doValidate(DynamicJdbcConfig config, ..., DynamicJdbcConfig.Mybatis mybatis) {
    // 验证：同一个 DynamicJdbcConfig 中不能同时配置 mybatis 和 mybatis-plus
    if (config.hasMybatisPlus()) {
        errors.addError("'mybatis' and 'mybatis-plus' can't be configured for one DataSource");
    }
}
```

| 层面 | 互斥机制 | 检测时机 | 作用域 |
|------|---------|---------|--------|
| 配置层 | Validator | 启动时 | 同一个 DynamicJdbcConfig |
| Auto-Configuration 层 | banned-modules | 子上下文 refresh 时 | 同一个子上下文 |

两个层面的互斥确保了"在同一个子上下文中，绝对不会同时存在 MyBatis 和 MyBatis-Plus"。

---

## Transaction 管理器的共存

多 DataSource 场景下，Transaction 管理器也需要隔离。每个子上下文有其独立的 `PlatformTransactionManager`。

### 独立注册

```java
// TransactionConfigurationConfigBeanDefinitionRegistrar.java
public void register(DynamicJdbcConfig config, String propertyName, String module, 
                     DynamicJdbcConfig.Transaction transaction, BeanDefinitionRegistry registry) {

    String txBeanName = transaction.getName();
    if (StringUtils.hasText(txBeanName)) {
        // 给 DataSourceTransactionManager 注册别名
        registerBeanAliases(DataSourceTransactionManager.class, txBeanName);
    }

    // 注册 PlatformTransactionManagerCustomizer
    String customizers = transaction.getCustomizers();
    if (StringUtils.hasText(customizers)) {
        for (String customizerClassName : customizers.split(",")) {
            // 加载并注册 customizer
        }
    }
}
```

### Bean 升迁到父上下文

子上下文刷新后，`DynamicJdbcChildContextRefreshedListener` 将 TransactionManager 注册到父上下文：

```java
// 父上下文收到：
// orders$transactionManager (name = "orders-tx")
// users$transactionManager (name = "users-tx")

// 通过 PlatformTransactionManagerBeanNameGenerator 生成父 Bean 名
// 使用 transaction.name → "orders-tx"
```

父上下文中的服务在调用 `@Transactional("orders-tx")` 时，使用的是 orders 子上下文的 TransactionManager；调用 `@Transactional("users-tx")` 时，使用的是 users 子上下文的 TransactionManager。

### 事务隔离的局限

但由于两个 TransactionManager 绑定到不同的 DataSource，**跨子上下文的事务**（比如一个服务方法同时更新 orders 库和 users 库）会变成**分布式事务**。Spring 的 `DataSourceTransactionManager` 没有两阶段提交能力，所以跨库操作要么拆分为多次本地事务（最终一致），要么业务层自己实现 TCC/Saga。

即使配置了 ShardingSphere 并启用了其内置的 XA（Atomikos/Narayana）或 BASE（Seata）事务能力，也仅限于**同一个子上下文内的跨分片事务**——因为 XA 协调的是同一个子上下文中的多个分片 DataSource。跨子上下文的 DataSource 分属不同的 Spring 容器，XA 事务管理器无法协调。

---

## ShardingSphere 与其他框架的共存

ShardingSphere 与 MyBatis/MyBatis-Plus 的共存有两种模式：

### 模式 1：同子上下文共存

```json
{
  "dataSource": [{...多个分片数据源...}],
  "sharding-sphere": { "config-resource": "classpath:shardingsphere.yaml" },
  "mybatis": { "base-packages": "com.example.mapper" }
}
```

在同一个子上下文中，ShardingSphere 创建 `ShardingSphereDataSource`（包装多个真实数据源），MyBatis 的 SqlSessionFactory 创建时注入这个 `ShardingSphereDataSource`：

```
子上下文内：
  ShardingSphere → 创建 ShardingSphereDataSource
  MyBatis → SqlSessionFactory(DataSource=ShardingSphereDataSource)
```

因为 banned-modules 只禁止了 datasource 和 sharding-sphere 互斥（避免两套 DataSource Auto-Configuration 冲突），mybatis 和 sharding-sphere 没有禁止，所以可以共存。

### 模式 2：不同子上下文

```json
{
  "configs.shard-order": {
    "dataSource": [{...分片数据源...}],
    "sharding-sphere": { "config-resource": "classpath:shard-orders.yaml" },
    "mybatis": { "base-packages": "com.example.orders.mapper" }
  },
  "configs.user-center": {
    "dataSource": [{...单库数据源...}],
    "mybatis-plus": { "base-packages": "com.example.users.mapper" }
  }
}
```

shard-order 子上下文：ShardingSphere + MyBatis + 分片数据库
user-center 子上下文：MyBatis-Plus + 单库

完全隔离。

---

## 共存方案的代价

18-dynamic 的子上下文隔离方案不是免费的。它的代价是：

### 内存代价

每个子上下文是一个完整的 Spring 容器：
- 独立的 `DefaultListableBeanFactory`
- 独立的 `Environment`（包含合成的 PropertySource）
- 完整的 Auto-Configuration 处理结果
- 所有 singleton Bean 的实例
- 独立的连接池（`HikariDataSource`）

实际消耗取决于配置的模块数量：
- 仅 DataSource 子上下文：约 20-30MB（HikariCP 连接池 + BeanFactory + Environment）
- DataSource + MyBatis + Transaction：约 50-80MB（加上 SqlSessionFactory、Mappers、TransactionManager）
- DataSource + ShardingSphere + MyBatis：约 80-150MB（加上 ShardingSphere 规则对象、连接池多个分片）

同时，每个 HikariCP 连接池有一个 HouseKeeper 后台线程（用于闲置连接回收、连接超时检测），N 个子上下文就有 N 个 HouseKeeper 线程。线程数 = config 数量 × 连接池数（ShardingSphere 会创建多个分片连接池）。

### 启动时间代价

每个子上下文 refresh 需要 1-3 秒（加载 Auto-Configuration、创建 Bean、初始化连接池）。

N 个 config 并行初始化（线程池大小 = N）≈ `max(单个初始化耗时)` 秒——最慢的那个子上下文决定总时间。
N 个 config 串行初始化 ≈ N × 单个耗时 秒。
注意：DynamicDataSource 热替换时，每次也是创建完整的子上下文（因为 refresh 是完整生命周期），所以切换代价也是 1-3 秒。

### 连接池代价

每个子上下文有独立的 HikariDataSource，每个连接池有自己的一组连接到数据库。如果 config 数量多且每个都配置了连接池参数，数据库侧的连接数可能成为瓶颈。

假设 10 个 config，每个 minimum-idle=5，则至少有 50 个连接保持在数据库端。

### 运维代价

- 每个子上下文有独立的日志分类（context ID 前缀）
- 每个子上下文有独立的 Metrics（需要配置 Micrometer 区分 tag）
- 故障排查时需要检查每个子上下文的状态
- 连接池监控需要按子上下文分别查看

### 是否值得

是否值得取决于场景：

| 场景 | 是否建议用 18-dynamic | 原因 |
|------|---------------------|------|
| 多 Zone 多活，每个 Zone 独立数据库 | ✅ | zone 切换需要重建数据源，子上下文隔离是自然方案 |
| 同一 DataSource，需要 MyBatis + MP 共存 | ❌ | 方案 B（多 SqlSessionFactory 共享同一个 DataSource）更合适 |
| 多套完全独立的数据库+ORM | ✅ | 子上下文隔离保证完全独立 |
| 单数据库 + 单 ORM | ❌ | 太重了，普通 Spring Boot 就够 |
| ShardingSphere + 分库分表 | ✅ | ShardingSphere 需要完整 Auto-Configuration 支持 |

---

## 回到简历中的"ORM 共存"能力

这篇文档分析了 18-dynamic 如何解决 MyBatis 和 MyBatis-Plus 不能共存的根本问题。回到简历点：

| 简历声称的能力 | 实现方式 | 限制 |
|---------------|---------|------|
| "JDBC 框架隔离和并存，如 MyBatis、MyBatis-Plus 或其他 ORM 框架" | 通过 DynamicJdbcChildContext 子上下文隔离，每个框架运行在独立的 Spring 容器中 | ① 不能共享同一个 DataSource 实例（必须分别创建连接池） ② 不能隔离 ORM 框架版本（共享 ClassLoader） |

简历中描述的"并存"是真实的——MyBatis 和 MyBatis-Plus 确实可以在同一个进程中共存。但它的实现方式是"各跑各的容器"而不是"共享同一个容器"，这意味着：
- 每个 ORM 框架有自己的 SqlSessionFactory、DataSource、TransactionManager
- 它们的配置完全独立，不会互相影响
- 代价是额外的内存和资源消耗（每个子上下文 20-150MB）
- 适用于多 Zone 多活场景（每个 Zone 独立数据库），不适用于"同一数据库需要两种 ORM"的场景
