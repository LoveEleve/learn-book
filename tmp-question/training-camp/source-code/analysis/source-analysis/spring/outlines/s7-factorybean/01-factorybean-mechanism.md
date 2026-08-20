# S1-7 FactoryBean — Spring 的"Bean 工厂代理"

> 依赖 S1-2 / S1-6 | 🟡 Working | 3 KP | [模式: Factory Method + Proxy]

**读者处境**: `@Autowired private UserMapper userMapper` — UserMapper 是一个接口，没有实现类。Spring 怎么注入？MyBatis 通过 `MapperFactoryBean` 实现了 `FactoryBean<UserMapper>` — `getObject()` 返回 JDK 动态代理(MapperProxy)。Dubbo `@Reference` 同理 — `ReferenceBean` 实现了 `FactoryBean`。

### 1. FactoryBean 接口 — 三个方法定义 Bean 创建契约

场景: `public class MapperFactoryBean<T> implements FactoryBean<T>` — `getObject()` 返回 `T` 类型的代理 — `getObjectType()` 返回 `T.class` — `isSingleton()` 返回 true。Spring 容器看到 `MapperFactoryBean` 已注册 — 但调用 `getBean("userMapper")` 时 — 返回的是 `getObject()` 的产物(代理) — 而非 `MapperFactoryBean` 自身。

源码路径:
- `FactoryBean.java:96` — **getObject()**: 返回创建的 Bean — MyBatis 返回 `MapperProxy`(JDK 动态代理) / Dubbo 返回 `RpcProxy`(RPC 调用代理)
- `FactoryBean.java:118` — **getObjectType()**: 返回声明的类型 — 用于 @Autowired `byType` 解析时确定候选类型 — `UserMapper.class` 而非 `MapperFactoryBean.class`
- `FactoryBean.java:145` — **isSingleton()**: 返回 true(且 FactoryBean 自身是容器单例) → getObject() 的产物被缓存于父类 `FactoryBeanRegistrySupport` 的 `factoryBeanObjectCache`(FactoryBeanRegistrySupport.java:44, put 在 :167) — 注意 **singletonObjects 里存的是 FactoryBean 自身**, 不是产物 — false → 每次 getBean 都调用 getObject() 创建新实例
- `AbstractBeanFactory.java:1857` — **getObjectForBeanInstance(beanInstance, name, beanName, mbd)**: doGetBean 中调用 — 判断 beanInstance 是否是 FactoryBean → 是→`isFactoryDereference(name)` 检查 `&` 前缀 → 有 `&`→返回 FactoryBean 自身 / 无 `&`→调用 `factory.getObject()` 返回产物

关键设计: **Why FactoryBean 不是 BPP？** BPP 的切入时机在 doCreateBean 内部 — 当 BPP 执行时，Bean 已经有一个"实例"存在(至少已实例化)。FactoryBean 改变了"什么是实例" — 容器中注册的是 FactoryBean 自身 — 但它创建的产物才被当作"Bean"。**BPP 加工 Bean，FactoryBean 定义 Bean 的来源。** [模式: Factory Method — FactoryBean 将 Bean 的创建委托给 getObject()]

数据流: `getBean("userMapper")`→`doGetBean`→singletonObjects 无→createBean→创建 `MapperFactoryBean` 实例→initializeBean→`addSingleton("userMapper", mapperFactoryBean)`(singletonObjects 存 FactoryBean 自身)→`getObjectForBeanInstance(mapperFactoryBean, "userMapper", ...)`→`instanceof FactoryBean`=true→`isFactoryDereference("userMapper")`=false(无 &)→调 `factoryBean.getObject()`→若 isSingleton=true→产物存入 `factoryBeanObjectCache`(FactoryBeanRegistrySupport.java:44)→返回 MapperProxy→后续 getBean 直接命中 singletonObjects 的 FactoryBean→再走 getObjectForBeanInstance→factoryBeanObjectCache 命中产物→直接返回。

### 2. MyBatis MapperFactoryBean — FactoryBean 的真实应用

场景: `@MapperScan("com.example.mapper")` — Spring 扫描所有 Mapper 接口 → 每个接口注册一个 `MapperFactoryBean` → `getObject()` 返回 JDK 动态代理(MapperProxy.invoke() → SqlSession.selectOne())。

源码路径:
- `MapperScannerRegistrar`(MyBatis Spring 集成) — `@MapperScan` 的 import 处理器 — 为每个 Mapper 接口注册 `MapperFactoryBean` 类型 BD
- `FactoryBeanRegistrySupport.java:167` — **产物缓存写入点**: `factoryBeanObjectCache.put(beanName, object)` — 第二次 getBean 命中直接返回产物

数据流: `@MapperScan`→`MapperScannerRegistrar`→遍历 Mapper 接口: `UserMapper` → `BeanDefinitionBuilder.rootBeanDefinition(MapperFactoryBean.class)` → `bd.getPropertyValues().add("mapperInterface", UserMapper.class)` → `beanFactory.registerBeanDefinition("userMapper", bd)` → `getBean("userMapper")`→`doGetBean("userMapper")`→`getSingleton("userMapper")`→null→`createBean("userMapper")`→`doCreateBean`→创建 `MapperFactoryBean` 实例 → `initializeBean` → `addSingleton("userMapper", mapperFactoryBean)` → 存入 singletonObjects(存储的是 **FactoryBean 自身**) → `getObjectForBeanInstance(mapperFactoryBean, "userMapper", "userMapper", null)`→`mapperFactoryBean instanceof FactoryBean`=true→`name` 无 `&` 前缀→`object = mapperFactoryBean.getObject()`→`sqlSession.getMapper(UserMapper.class)`→JDK `Proxy.newProxyInstance`(MapperProxy) → 返回 MapperProxy → `AbstractBeanFactory`(L115 extends `FactoryBeanRegistrySupport`) 通过父类的 `factoryBeanObjectCache`(`FactoryBeanRegistrySupport.java:44`, ConcurrentHashMap 16)缓存 → `@Autowired UserMapper userMapper` 注入的是 MapperProxy → `userMapper.findById(1)` → `MapperProxy.invoke()` → `SqlSession.selectOne("com.example.UserMapper.findById", 1)` → JDBC。

关键设计: **Why MyBatis 用 FactoryBean 而非直接注册代理 Bean？** Mapper 接口在编译时没有实现类 — 只有运行时通过 SqlSession.getMapper 生成代理。FactoryBean 让"代理生成逻辑"封装在 getObject() 中 — 容器只需注册 MapperFactoryBean 一个 Bean — 产物(MapperProxy)与工厂(FactoryBean)分离: singletonObjects 存工厂, factoryBeanObjectCache 存产物 — 用户 @Autowired 拿到产物, 框架代码用 & 拿工厂。[模式: Factory Method — getObject() 是工厂方法]

### 3. & 前缀 — 基础设施代码的 Escape Hatch

场景: 容器管理代码需要知道 `userMapper` Bean 是不是 FactoryBean — 调用 `isFactoryBean("userMapper")` — 内部用 `&userMapper` 获取 FactoryBean 自身 — 检查 `instanceof FactoryBean`。

源码路径:
- `AbstractBeanFactory.java:1283-1285` — **transformedBeanName(name)**: 内部先 `BeanFactoryUtils.transformedBeanName(name)` 剥离 `&` 前缀得到真实 beanName → 再走 `canonicalName` 解析别名 — 该方法**不缓存任何 isFactoryDereference 标志**(`&` 判断由调用方在需要时实时调 `BeanFactoryUtils.isFactoryDereference(name)` 完成)
- `BeanFactoryUtils.java:74` — **isFactoryDereference(name)**: 检查 name 首字符是否等于 `FACTORY_BEAN_PREFIX_CHAR`(`&`)→ 是→返回 true(AbstractBeanFactory.java:434 只是它的调用点)

关键设计: **Why `&` 不是 Bean 名字的一部分？** `transformedBeanName` 剥离 `&` 后 — beanName 是 `"userMapper"` → 从 singletonObjects 取出的是 `MapperFactoryBean` 实例。然后 `getObjectForBeanInstance` 检查 `isFactoryDereference(name)` → 如果原始 name 有 `&` → 返回 MapperFactoryBean 自身 → 如果无 `&` → 返回 `getObject()` 的产物。**`&` 前缀不影响 Bean 的缓存和查找 — 只影响最终返回什么。**

数据流: `beanFactory.getBean("&userMapper")`→`doGetBean`→`transformedBeanName("&userMapper")`(L1283 剥离 &)→beanName="userMapper"→从 singletonObjects 取出 MapperFactoryBean→`getObjectForBeanInstance(mapperFactoryBean, "&userMapper", "userMapper", mbd)`→L1862 `isFactoryDereference("&userMapper")`=true→`instanceof FactoryBean`=true→**返回 MapperFactoryBean 自身**→容器代码检查 `instanceof FactoryBean`→确认它是工厂→完成。反之 `getBean("userMapper")` 无 & → 返回 getObject() 产物 — 同一个缓存条目, 两种返回。

→ spring-beans 层全部 7 域完成: BeanDefinition→BeanFactory→Bean生命周期→循环依赖→DI注入→BPP全景→FactoryBean。引出 spring-context 层: ApplicationContext / refresh() / @Configuration。
