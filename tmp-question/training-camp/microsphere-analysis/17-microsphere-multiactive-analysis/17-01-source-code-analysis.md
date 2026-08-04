# 17-01：Microsphere-Multiactive 源码分析

> **核心命题**：异地多活架构的核心是"同区域优先调用"。31 个文件逐文件解读，从区域发现、优先路由、动态切换、信息传播四个角度拆解 microsphere-multiactive 的全部实现。

---

## 项目结构

microsphere-multiactive（31 个主文件，5 个子模块）

microsphere-multiactive-commons（6 文件）
纯 Java 核心，零 Spring 依赖
├── ZoneContext.java
├── ZoneConstants.java
├── ZoneResolver.java
├── ZonePreferenceFilter.java
├── ZoneAttachmentHandler.java
└── HttpUtils.java

microsphere-multiactive-spring（7 文件）
Spring 集成层
├── ZoneLocator.java
├── AbstractZoneLocator.java
├── CompositeZoneLocator.java
├── DefaultZoneLocator.java
├── ZoneContextChangedEvent.java
├── ZoneContextChangedListener.java
└── ZoneUtils.java

microsphere-multiactive-spring-boot（3 文件）
Spring Boot 自动装配
├── ZoneAutoConfiguration.java
├── ConditionalOnAvailabilityZoneAvailable.java
└── ConditionalOnAvailabilityZoneEnabled.java

microsphere-multiactive-spring-cloud（6 文件）
Spring Cloud 集成
├── ZonePreferenceServiceInstanceListSupplier.java
├── CustomizedLoadBalancerAutoConfiguration.java
├── CustomizedLoadBalancerClientConfiguration.java
├── CloudServerZoneResolver.java
├── ZoneAttachmentListener.java
└── ZoneCloudAutoConfiguration.java

microsphere-multiactive-netflix（6 文件）
Netflix Eureka + Ribbon 适配
├── EurekaInstanceInfoZoneResolver.java
├── RibbonServerZoneResolver.java
├── ZonePreferenceServerListFilter.java
├── DiscoveryClientServer.java
├── DiscoveryClientServerList.java
└── ZoneAttachmentPreRegistrationHandler.java

microsphere-multiactive-aws（3 文件）
AWS 区域定位
├── Ec2AvailabilityZoneEndpointZoneLocator.java
├── EcsContainerMetadataFileZoneLocator.java
└── EcsTaskMetadataEndpointV4ZoneLocator.java

---

## 第一部分：commons 包——纯 Java 核心

### ZoneContext.java（235 行）

文件位置：microsphere-multiactive-commons/src/main/java/io/microsphere/multiple/active/zone/ZoneContext.java

完整源码：

public class ZoneContext {
    private static final ZoneContext instance = new ZoneContext();
    private volatile boolean enabled = true;
    private volatile String zone = "defaultZone";
    private volatile boolean preferenceEnabled = false;
    private volatile int preferenceFilterOrder = 10;
    private volatile int preferenceUpstreamZoneReadyPercentage = 100;
    private volatile int preferenceUpstreamSameZoneMinAvailable = 5;
    private volatile String preferenceUpstreamDisabledZone = null;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public static ZoneContext get() { return instance; }
    public static String getCurrentZone() {
        return System.getProperty(CURRENT_ZONE_PROPERTY_NAME, DEFAULT_ZONE);
    }
}

逐行分析：

第 34 行  static final ZoneContext instance = new ZoneContext()
单例。为什么是单例而不是 Spring Bean？因为 ZoneContext 需要在非 Spring 代码中使用——Ribbon 的 ServerListFilter、Redis 连接工厂、MyBatis 拦截器。这些地方拿不到 Spring BeanFactory，但通过 ZoneContext.get() 全局能拿到。

第 36 行  volatile boolean enabled = true
区域功能总开关。默认 true。PropertyChangeSupport 触发变更事件。

第 38 行  volatile String zone = "defaultZone"
当前区域。"defaultZone" 是和 Eureka 默认区域名兼容的常量。赋值时用 trimAllWhitespace 去空格，防止配置时不留心多打的空格导致区域匹配失败。

第 40 行  volatile boolean preferenceEnabled = false
区域优先路由开关。默认 false！这是安全第一的设计——开启区域偏好后流量分布会改变。同区域实例少时流量全压过去，可能导致过载。默认关闭要求用户明确知道自己要做什么。

第 42 行  volatile int preferenceFilterOrder = 10
过滤器在 Spring Cloud LoadBalancer Filter 链中的顺序。默认 10，通常多个 Filter 按 order 排序执行。

第 44 行  volatile int preferenceUpstreamZoneReadyPercentage = 100
区域就绪百分比。这个值的含义：调用 ZonePreferenceFilter 时，遍历所有上游实例，统计有区域信息的实例占的比例。如果比例低于这个阈值，跳过偏好路由，返回全部实例。

默认 100% 极其保守。100 个实例中 1 个没有区域信息→就绪率 99% < 100%→不应用偏好。生产环境建议调到 80-90%。

第 46 行  volatile int preferenceUpstreamSameZoneMinAvailable = 5
同区域最小可用实例数。如果符合同区域的实例数量低于这个值，跳过偏好路由，返回全部实例。防止同区域只有 1-2 个实例时全压过去导致过载。

第 48 行  volatile String preferenceUpstreamDisabledZone = null
禁用的上游区域（逗号分隔）。机房故障时运维可以通过配置中心把故障机房设为禁用。

第 50 行  PropertyChangeSupport propertyChangeSupport
JavaBeans 属性变更事件支持。不是 Spring 的 ApplicationEvent。ZoneContext 不是 Spring Bean，不能用 Spring 事件机制。PropertyChangeSupport 是 Java 标准的事件机制，任何地方都能用。

每个 setter 方法都调 isPropertyChanged()：

private boolean isPropertyChanged(String name, Object oldVal, Object newVal) {
    boolean changed = !Objects.equals(oldVal, newVal);
    if (changed) {
        propertyChangeSupport.firePropertyChange(name, oldVal, newVal);
    }
    return changed;
}

只有值真正变化时才触发事件。避免重复设置相同值时产生大量无用事件。

第 232 行  getCurrentZone() 静态方法
public static String getCurrentZone() {
    return System.getProperty(CURRENT_ZONE_PROPERTY_NAME, DEFAULT_ZONE);
}

读系统属性，不是读单例字段。为什么？因为 ZoneLocator 定位到区域后写入 System property，这是全局可见的。即使 ZoneContext 还没初始化，其他组件也能通过这个静态方法获取区域。

设计权衡：单例
ZoneContext 的设计选择是：全局单例，通过 PropertyChangeSupport 传播变更事件，通过静态方法和 System property 双重路径暴露区域。

优势：任何代码都能读到当前区域（非 Spring 代码、测试代码、Ribbon Filter）。
代价：不可 mock。单元测试中不能替换 ZoneContext，只能通过 System.setProperty 间接设置。

---

### ZoneConstants.java（241 行）

文件位置：microsphere-multiactive-commons/src/main/java/io/microsphere/multiple/active/zone/ZoneConstants.java

这是常量接口（interface 常量模式）。声明了所有配置属性的名称和默认值。

核心常量分组：

Zone 基础属性

String ZONE_PROPERTY_NAME = "microsphere.availability.zone"
应用配置中的区域名。用法：application.yml 中配 microsphere.availability.zone=beijing-1。

String CURRENT_ZONE_PROPERTY_NAME = "microsphere.current.availability.zone"
系统属性中的当前区域。ZoneLocator 定位到区域后写入 System property，给 ZoneContext.getCurrentZone() 读取。

String DEFAULT_ZONE = "defaultZone"
默认区域。和 Eureka 的 DEFAULT_ZONE 兼容。

Zone 启用开关

String ZONE_ENABLED_PROPERTY_NAME = ZONE_PROPERTY_NAME + ".enabled"
boolean DEFAULT_ZONE_ENABLED = true

区域功能默认启用。

区域偏好属性

String PREFERENCE_ENABLED_PROPERTY_NAME
String PREFERENCE_FILTER_ORDER_PROPERTY_NAME
String PREFERENCE_UPSTREAM_ZONE_READY_PERCENTAGE_PROPERTY_NAME
String PREFERENCE_UPSTREAM_SAME_ZONE_MIN_AVAILABLE_PROPERTY_NAME
String PREFERENCE_UPSTREAM_DISABLED_ZONE_PROPERTY_NAME

boolean DEFAULT_ZONE_PREFERENCE_ENABLED = false
int DEFAULT_PREFERENCE_UPSTREAM_ZONE_READY_PERCENTAGE = 100
int DEFAULT_PREFERENCE_UPSTREAM_SAME_ZONE_MIN_AVAILABLE = 5
String DEFAULT_PREFERENCE_UPSTREAM_DISABLED_ZONE = null

Locator 属性

String LOCATOR_FAST_FAIL_PROPERTY_NAME
String LOCATOR_TIMEOUT_PROPERTY_NAME
boolean DEFAULT_LOCATOR_FAST_FAIL = false
int DEFAULT_LOCATOR_TIMEOUT = 3000

设计注意点：@ConfigurationProperty 注解

每个属性上有 @ConfigurationProperty 注解，标注了来源（APPLICATION_SOURCE 表示来自 Spring 配置文件）。这个注解来自 microsphere-java 的配置系统，用于生成 IDE 自动补全和配置元数据。

例如 ZONE_ENABLED_PROPERTY_NAME:

@ConfigurationProperty(
    type = boolean.class,
    defaultValue = DEFAULT_ZONE_ENABLED_PROPERTY_VALUE,
    source = APPLICATION_SOURCE
)
String ZONE_ENABLED_PROPERTY_NAME = ZONE_PROPERTY_NAME + ENABLED_PROPERTY_NAME_SUFFIX;

这在 Spring Boot 的 application.yml 中能获得 IDE 自动补全、类型提示、默认值显示。

---

### ZoneResolver.java（24 行）

文件位置：microsphere-multiactive-commons/src/main/java/io/microsphere/multiple/active/zone/ZoneResolver.java

@FunctionalInterface
public interface ZoneResolver<E> extends Function<E, String> {
    default String apply(E entity) { return resolve(entity); }
    String resolve(E entity);
}

这是整个模块中代码最少但用途最广的接口。它的职责：从任意实体解析出所属区域。

为什么是泛型？因为不同场景需要从不同类型的实体解析区域：

实体类型	场景	ZoneResolver 实现
ServiceInstance	Spring Cloud 服务实例	CloudServerZoneResolver
Server	Ribbon 负载均衡的服务器	RibbonServerZoneResolver
InstanceInfo	Eureka 实例信息	EurekaInstanceInfoZoneResolver

ZonePreferenceFilter 持有 ZoneResolver 实例，调用 zoneResolver.resolve(entity) 从每个上游实例中读取区域，然后判断哪些实例属于当前区域。

设计决策：泛型接口 vs 类型特定接口

ZoneResolver 是泛型的。另一个方案是为每种实体类型写一个接口（ServiceInstanceZoneResolver、ServerZoneResolver）。泛型的优点：
1. ZonePreferenceFilter 只需要一个类型参数 <E>，不需要为每种实体类型写一个 Filter
2. 不需要强制类型转换
3. 每个 ZoneResolver 实现只需要关心 resolve() 方法

泛型的代价是 Java 的类型擦除，ZonePreferenceFilter 在运行时不能直接获取 E 的类型。

---

### ZonePreferenceFilter.java（185 行）

文件位置：microsphere-multiactive-commons/src/main/java/io/microsphere/multiple/active/zone/ZonePreferenceFilter.java

完整源码逐段分析：

构造函数

public class ZonePreferenceFilter<E> {
    private final ZoneContext zoneContext;
    private final ZoneResolver<E> zoneResolver;

    public ZonePreferenceFilter(ZoneContext zoneContext, ZoneResolver<E> zoneResolver) {
        this.zoneContext = zoneContext;
        this.zoneResolver = zoneResolver;
    }
}

ZonePreferenceFilter 不持有任何可变状态，是线程安全的。每次调用 filter() 都是自包含的，没有副作用（除了日志输出）。ZoneContext 的 volatile 字段保证了每次读到的都是最新值。

filter() 方法——核心算法

这是一套 10 步决策树。每一步如果决定"不应用偏好"，就返回原始实体列表（完全降级）。

第 1 步：空列表或单元素

if (totalSize <= 1) { return entities; }

实例数少于 2 时不需要过滤。单元素列表没有偏好选择，直接返回。
注意这里返回的是 entities（原始引用），不是 new ArrayList<>()。调用方可以直接使用返回的列表，不需要额外拷贝。

第 2 步：区域功能未启用

if (!zoneContext.isEnabled()) { return entities; }

enabled 是总开关。全局关闭时不做任何区域处理。

第 3 步：区域偏好未启用

if (!zoneContext.isPreferenceEnabled()) { return entities; }

preferenceEnabled 是偏好路由的开关。默认 false，需要显式开启。为什么还有个独立开关？因为 enabled 控制整个区域功能（包括区域发现、信息传播），preferenceEnabled 只控制路由偏好。可以启用区域功能但不启用偏好路由。

第 4 步：当前区域为无效值

final String zone = zoneContext.getZone();
if (isIgnored(zone)) { return entities; }

isIgnored 的定义：

protected boolean isIgnored(String zone) {
    return isBlank(zone) || DEFAULT_ZONE.equalsIgnoreCase(zone);
}

当前区域为空、或者为 "defaultZone" 时跳过。为什么要跳过 "defaultZone"？因为 "defaultZone" 是默认值，没有明确配置区域，不应该启用偏好路由——如果所有实例都是 defaultZone，偏好路由没有意义。

第 5 步：过滤禁用区域的实体

String disabledZone = zoneContext.getPreferenceUpstreamDisabledZone();
if (disabledZone != null) {
    targetEntities = filterDisabledZone(entities, disabledZone, totalSize);
    int currentSize = targetEntities.size();
    if (currentSize <= 1) { return entities; }
    totalSize = currentSize;
}

filterDisabledZone 的具体实现：

private List<E> filterDisabledZone(List<E> entities, String disabledZone, int totalSize) {
    String[] disabledZones = split(disabledZone, COMMA_CHAR);
    List<E> targetEntities = new LinkedList<>();
    for (int i = 0; i < totalSize; i++) {
        E entity = entities.get(i);
        String resolvedZone = resolveZone(entity);
        if (!isDisabledZone(resolvedZone, disabledZones)) {
            targetEntities.add(entity);
        }
    }
    return targetEntities;
}

如果禁用区域（disabled-zone）配置了 "shanghai-1,beijing-2"，所有属于这些区域的实例被移除。

关键细节：过滤后 totalSize 被重新赋值

totalSize = currentSize;

这影响后续的就绪率计算（第 7 步）的分母。原始 100 个实例，禁用了 10 个后 totalSize=90，就绪率分母是 90 不是 100。这是一个非常重要的细节：禁用区域的实例不参与就绪率统计。

第 6 步：按区域分组统计

for (int i = 0; i < totalSize; i++) {
    E entity = targetEntities.get(i);
    String resolvedZone = resolveZone(entity);
    if (resolvedZone != null) {
        zoneCount++;
        if (matches(zone, resolvedZone)) {
            sameZoneEntities.add(entity);
        }
    }
}

遍历过滤后的实例列表，使用 ZoneResolver 解析每个实例的区域，统计两个数量：
zoneCount：有区域信息的实例数
sameZoneEntities.size()：和当前区域匹配的实例数

实例的区域信息为 null 时不计入 zoneCount。这表示该实例没有 Zone metadata，可能配置出错。

第 7 步：上游区域就绪率检查

if (isUpstreamZoneNotReady(zoneCount, totalSize, upstreamReadyPercentage)) {
    return targetEntities;
}

isUpstreamZoneNotReady 的实现：

private boolean isUpstreamZoneNotReady(int zoneCount, int entitiesSize, int zonePercentThreshold) {
    int percent = (zoneCount * 100 / entitiesSize);
    return percent < zonePercentThreshold;
}

有区域信息的实例占总实例的比例是否小于阈值。如果 90 个实例中只有 80 个有区域信息，就绪率 88%（低于 100%），不应用偏好。

默认 100% 意味着：必须所有实例都有区域信息，否则偏好不启用。这个默认值非常保守。

第 8 步：同区域最小可用数检查

int sameZoneSize = sameZoneEntities.size();
if (sameZoneSize > 0) {
    int sameZoneMinAvailable = zoneContext.getPreferenceUpstreamSameZoneMinAvailable();
    if (isUnderSameZoneMinAvailableThreshold(sameZoneSize, sameZoneMinAvailable)) {
        return targetEntities;
    }
    return sameZoneEntities;
}
return targetEntities;

isUnderSameZoneMinAvailableThreshold 的实现：

private boolean isUnderSameZoneMinAvailableThreshold(int sameZoneSize, int zonePreferenceThreshold) {
    return sameZoneSize < zonePreferenceThreshold;
}

有几个边界情况：

sameZoneSize = 0：没有同区域实例→直接返回 targetEntities（所有非禁用实例）。
sameZoneSize > 0 但小于阈值（默认 5）：有同区域实例但太少→返回 targetEntities（不应用偏好，避免过载）。
sameZoneSize >= 阈值：返回 sameZoneEntities（只返回同区域实例）。

三种降级策略

整个 filter 方法有三级降级：

第 1 级：直接返回原始 entities（什么都不过滤）。触发条件：功能关闭、偏好关闭、区域为 defaultZone、禁用区域过滤后实体太少。
第 2 级：返回 targetEntities（禁用了指定区域后的列表）。触发条件：就绪率不达标、同区域实例太少。
第 3 级：返回 sameZoneEntities（只返回同区域实例）。触发条件：所有条件满足。

第 1 级和第 2 级的区别：第 1 级返回原始列表（包含禁用区域的实例），第 2 级已经排除了禁用区域。

降级策略的设计原则是 Fail-open：偏好路由失败时不应该阻断业务流量。无论哪个级别，都返回非空列表（除非原始列表本身就为空）。

getOrder() 方法

public int getOrder() {
    return zoneContext.getPreferenceFilterOrder();
}

ZonePreferenceFilter 本身也可以通过 getOrder() 参与排序。在 Ribbon 集成的 ZonePreferenceServerListFilter 中，通过 getOrder() 控制过滤器链中的执行顺序。

ZonePreferenceFilter 的设计总结
1. 纯函数式的过滤逻辑——不持有可变状态，每次调用独立
2. 三级降级——所有检查的降级方向都是"返回全部实例"，不是"返回空"
3. 边界感知——空列表、单元素、defaultZone、null 都正确处理
4. 参数隔离——所有参数来自 ZoneContext，可运行时动态修改

---

### ZoneAttachmentHandler.java（43 行）

文件位置：microsphere-multiactive-commons/src/main/java/io/microsphere/multiple/active/zone/ZoneAttachmentHandler.java

public class ZoneAttachmentHandler {
    private final ZoneContext zoneContext;

    public ZoneAttachmentHandler(ZoneContext zoneContext) {
        this.zoneContext = zoneContext;
    }

    public void attachZone(Map<String, String> metadata) {
        String zone = zoneContext.getZone();
        if (isNotBlank(zone)) {
            String propertyName = ZONE_PROPERTY_NAME;
            try {
                metadata.put(propertyName, zone);
                logger.info("The zone ['{}'] has been attached into meta-data [name : '{}']", zone, propertyName);
            } catch (Throwable e) {
                logger.warn("The zone ['{}'] can't be attached into meta-data [name : '{}']", zone, propertyName);
            }
        }
    }
}

attachZone 的作用是：在当前实例注册到服务发现中心之前，把区域信息写入注册 metadata。

为什么用 try/catch Throwable？因为 metadata 可能是不可变 Map（Eureka 的 InstanceInfo metadata 在某些情况下不可变）。如果 put 抛 UnsupportedOperationException，catch 住后只记录警告，不影响注册流程。区域信息的传播是可选的，不应该因为写不进去就阻止服务注册。

ZoneAttachmentHandler 被两条路径调用：
Spring Cloud 路径：ZoneAttachmentListener 监听 RegistrationPreRegisteredEvent
Netflix Eureka 路径：ZoneAttachmentPreRegistrationHandler 在 Eureka 注册前执行

---

### HttpUtils.java（52 行）

文件位置：microsphere-multiactive-commons/src/main/java/io/microsphere/multiple/active/zone/HttpUtils.java

public abstract class HttpUtils {
    public static String doGet(String url, int timeout) throws IOException {
        String content = null;
        if (isNotBlank(url)) {
            URLConnection urlConnection = new URL(url).openConnection();
            if (urlConnection instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
                urlConnection.setConnectTimeout(timeout);
                urlConnection.setReadTimeout(timeout);
                try (InputStream inputStream = httpURLConnection.getInputStream()) {
                    content = copyToString(inputStream, UTF_8);
                } finally {
                    httpURLConnection.disconnect();
                }
            }
        }
        return content;
    }
}

极其简单的 HTTP GET 客户端，用 JDK 的 HttpURLConnection。没有任何连接池、没有重试、没有 JSON 解析。用途只有一个：从 AWS 元数据端点（169.254.169.254）GET 区域信息。

为什么不用 Apache HttpClient 或 RestTemplate？因为 commons 模块零 Spring 依赖，而且 HttpUtils 只在 AWS ZoneLocator 中使用，是启动时的一次性请求，没有连接池的需要。引入完整的 HTTP 客户端就太重了。

注意事项：
连接超时和读取超时将相同的 time 参数
conn.disconnect() 在 JDK 9+ 中不再真正关闭连接（JDK 的 HttpURLConnection 行为），但代码保留了这个调用以确保兼容性
响应内容用 UTF-8 编码读取（AWS 元数据端点返回 ASCII，UTF-8 兼容）

---

## 第二部分：spring 包——Spring 集成层

### ZoneLocator.java（29 行）

文件位置：microsphere-multiactive-spring/src/main/java/io/microsphere/multiple/active/zone/spring/ZoneLocator.java

public interface ZoneLocator {
    boolean supports(Environment environment);
    String locate(Environment environment);
}

这是 SPI。任何部署环境只需要实现这两个方法：

supports(Environment)：当前环境是否支持这个定位器（如 EC2 环境中 ECS 文件存在、K8s 中环境变量存在）
locate(Environment)：返回区域名，无法定位时返回 null

ZoneLocator 的实现是自动发现机制。ZoneAutoConfiguration 通过 Spring FactoriesLoader 和 Spring Bean 发现所有 ZoneLocator 实现，组装成 CompositeZoneLocator。

和 ZoneResolver 的区别
ZoneLocator：定位"当前应用在哪个区域"。启动时调用一次，结果缓存。
ZoneResolver：解析"某个实体（如服务实例）在哪个区域"。每次负载均衡时调用，结果不缓存。

---

### AbstractZoneLocator.java（46 行）

文件位置：microsphere-multiactive-spring/src/main/java/io/microsphere/multiple/active/zone/spring/AbstractZoneLocator.java

public abstract class AbstractZoneLocator implements ZoneLocator, BeanNameAware, Ordered {
    protected final Logger logger = getLogger(getClass());
    protected String beanName;
    protected int order;

    public AbstractZoneLocator(int order) { setOrder(order); }

    @Override
    public final void setBeanName(String beanName) { this.beanName = beanName; }

    @Override
    public final int getOrder() { return order; }

    public final void setOrder(int order) { this.order = order; }
}

抽象基类，实现了 Ordered 和 BeanNameAware。每个 ZoneLocator 有 order 值，CompositeZoneLocator 按 order 排序后依次尝试。

BeanNameAware 让每个 ZoneLocator 知道自己的 Spring Bean 名称，用于日志输出。Ordered 用于排序——ECS 定位器 order=5 优先于 EC2 定位器 order=15。

空的 supports() 不会自动调用，需要子类实现。DefaultZoneLocator.supports() 总是返回 true。

---

### CompositeZoneLocator.java（112 行）

文件位置：microsphere-multiactive-spring/src/main/java/io/microsphere/multiple/active/zone/spring/CompositeZoneLocator.java

完整源码：

public class CompositeZoneLocator implements ZoneLocator {
    private final List<ZoneLocator> zoneLocators;
    private volatile String zone;

    public CompositeZoneLocator(List<ZoneLocator> zoneLocators) {
        Assert.notNull(zoneLocators, "...");
        this.zoneLocators = new ArrayList<>(zoneLocators);
        AnnotationAwareOrderComparator.sort(this.zoneLocators);
    }

    @Override
    public boolean supports(Environment environment) {
        for (ZoneLocator zoneLocator : zoneLocators) {
            if (zoneLocator.supports(environment)) { return true; }
        }
        return false;
    }

    @Override
    public String locate(Environment environment) {
        if (StringUtils.hasText(zone)) { return zone; }

        String zone = null;
        boolean fastFailEnabled = isFastFailEnabled(environment);
        ZoneLocator failedZoneLocator = null;

        for (ZoneLocator zoneLocator : zoneLocators) {
            try {
                if (zoneLocator.supports(environment)) {
                    zone = zoneLocator.locate(environment);
                    if (zone != null) {
                        logger.info("{} locates the zone : {}", zoneLocator, zone);
                        break;
                    } else {
                        logger.warn("{} can't locate the zone", zoneLocator);
                        if (fastFailEnabled) { failedZoneLocator = zoneLocator; break; }
                    }
                }
            } catch (Throwable e) {
                logger.error("{} failed to locate the zone", zoneLocator, e);
                if (fastFailEnabled) { failedZoneLocator = zoneLocator; break; }
            }
        }

        if (failedZoneLocator != null) {
            throw new IllegalStateException("The zone can't be located by " + failedZoneLocator);
        }

        this.zone = zone;
        if (this.zone != null) {
            System.setProperty(CURRENT_ZONE_PROPERTY_NAME, this.zone);
        }
        return zone;
    }
}

关键设计 1：缓存结果

private volatile String zone;
if (StringUtils.hasText(zone)) { return zone; }

zone 字段被缓存。第一次定位成功后，后续调用直接返回缓存值。因为区域在应用生命周期内通常不会改变（除非通过 ZoneContext.setZone() 强制修改）。

volatile 保证了多线程可见性。CompositeZoneLocator 是线程安全的。

关键设计 2：第一个非 null 结果

每个 ZoneLocator 按 order 排序。ECS 文件定位器（order=5）优先于 EC2 端点定位器（order=15）。如果 ECS 定位器返回了一个非 null 区域，循环 break，后续定位器不被执行。

这实现了"环境特定优先于通用配置"的原则。

关键设计 3：fast-fail

默认 false。在非 AWS 环境中，EC2 元数据端点（169.254.169.254）的连接超时会抛异常。fast-fail=false 时，异常被 catch 住，继续尝试下一个定位器。如果 true，任何一个定位器失败就抛异常，阻止应用启动。

fast-fail=false 是生产环境的合理默认值——你不想因为一个区域定位服务不可用就导致应用启动失败。

关键设计 4：System.setProperty

定位成功后，区域被写入 System property。这实现了 ZoneContext.getCurrentZone() 的静态访问路径——即使 ZoneContext 还没有被 Spring 初始化，其他组件也能通过 System.getProperty 读到区域。

---

### DefaultZoneLocator.java（40 行）

文件位置：microsphere-multiactive-spring/src/main/java/io/microsphere/multiple/active/zone/spring/DefaultZoneLocator.java

public class DefaultZoneLocator extends AbstractZoneLocator {
    public static final int DEFAULT_ORDER = 20;

    public DefaultZoneLocator() { super(DEFAULT_ORDER); }

    @Override
    public boolean supports(Environment environment) { return true; }

    @Override
    public String locate(Environment environment) {
        String zone = environment.getProperty(ZONE_PROPERTY_NAME);
        if (StringUtils.hasText(zone)) {
            logger.info("The zone ['{}'] was located from the Spring Property [name: '{}']", zone, ZONE_PROPERTY_NAME);
        } else {
            zone = DEFAULT_ZONE;
        }
        return zone;
    }
}

supports() 永远返回 true。这意味着 DefaultZoneLocator 总是会参与定位，但优先级最低（order=20）。

如果 application.yml 中配了 microsphere.availability.zone，返回配置值；否则返回 "defaultZone"。

为什么 order=20 而不是 order=100？EC2 定位器 order=15，DefaultZoneLocator order=20，只差了 5 个优先级。这确保 DefaultZoneLocator 在所有 AWS 定位器之后尝试，但不会因为 order 差距太大而导致 AWS 定位器即使不可用也要等待很久。

---

### ZoneContextChangedEvent.java（46 行）

文件位置：microsphere-multiactive-spring/src/main/java/io/microsphere/multiple/active/zone/spring/event/ZoneContextChangedEvent.java

public class ZoneContextChangedEvent extends ApplicationContextEvent {
    private final ZoneContext zoneContext;
    private final List<PropertyChangeEvent> propertyChangeEvents;

    public ZoneContextChangedEvent(ApplicationContext context, ZoneContext zoneContext,
                                    List<PropertyChangeEvent> propertyChangeEvents) {
        super(context);
        this.zoneContext = zoneContext;
        this.propertyChangeEvents = unmodifiableList(propertyChangeEvents);
    }

    public ZoneContext getZoneContext() { return zoneContext; }
    public List<PropertyChangeEvent> getPropertyChangeEvents() { return propertyChangeEvents; }
}

这是 Spring ApplicationEvent。ZoneContextChangedListener 在检测到 ZoneContext 属性变化后发布这个事件。其他模块（redis、datasource）通过 @EventListener 监听这个事件来触发区域切换的后续动作。

propertyChangeEvents 列出了所有发生了变化的属性及其新旧值，供监听者精确判断需要重新配置的内容。

---

ZoneContextChangedListener.java（251 行）

文件位置：microsphere-multiactive-spring/src/main/java/io/microsphere/multiple/active/zone/spring/event/ZoneContextChangedListener.java

完整源码：

public class ZoneContextChangedListener implements SmartApplicationListener,
        ApplicationContextAware, EnvironmentAware {

    private static final String APPLICATION_STARTED_EVENT_CLASS_NAME =
        "org.springframework.boot.context.event.ApplicationStartedEvent";
    private static final String ENVIRONMENT_CHANGE_EVENT_CLASS_NAME =
        "org.springframework.cloud.context.environment.EnvironmentChangeEvent";

    private static final Class<?> APPLICATION_STARTED_EVENT_CLASS =
        resolveClass(APPLICATION_STARTED_EVENT_CLASS_NAME, classLoader);
    private static final Class<?> ENVIRONMENT_CHANGE_EVENT_CLASS =
        resolveClass(ENVIRONMENT_CHANGE_EVENT_CLASS_NAME, classLoader);

    private static final boolean IS_SPRING_CLOUD_APPLICATION =
        ENVIRONMENT_CHANGE_EVENT_CLASS != null && APPLICATION_STARTED_EVENT_CLASS != null;
    private static final boolean IS_SPRING_BOOT_APPLICATION =
        ENVIRONMENT_CHANGE_EVENT_CLASS == null && APPLICATION_STARTED_EVENT_CLASS != null;

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        if (IS_SPRING_CLOUD_APPLICATION) {
            return ENVIRONMENT_CHANGE_EVENT_CLASS.isAssignableFrom(eventType)
                || APPLICATION_STARTED_EVENT_CLASS.isAssignableFrom(eventType);
        } else if (IS_SPRING_BOOT_APPLICATION) {
            return APPLICATION_STARTED_EVENT_CLASS.isAssignableFrom(eventType);
        } else {
            return ContextRefreshedEvent.class.isAssignableFrom(eventType);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        tryChangeZoneContext();
    }

    private final Map<String, Consumer<String>> propertyChangedHandlers = new HashMap<>();

    private void initPropertyChangedHandlers() {
        propertyChangedHandlers.put(ZONE_PROPERTY_NAME, this::changeZone);
        propertyChangedHandlers.put(ZONE_ENABLED_PROPERTY_NAME, this::changeEnabled);
        propertyChangedHandlers.put(PREFERENCE_ENABLED_PROPERTY_NAME,
            this::changeZonePreferenceEnabled);
        // ... 其他属性同理
    }

    private void tryChangeZoneContext() {
        List<PropertyChangeEvent> propertyChangeEvents = new LinkedList<>();
        PropertyChangeListener listener = propertyChangeEvents::add;
        zoneContext.addPropertyChangeListener(listener);
        for (String propertyName : ZONE_CONTEXT_PROPERTY_NAMES) {
            Consumer<String> handler = propertyChangedHandlers.get(propertyName);
            if (handler != null) {
                handler.accept(propertyName);
            }
        }
        zoneContext.removePropertyChangeListener(listener);
        if (!propertyChangeEvents.isEmpty()) {
            context.publishEvent(
                new ZoneContextChangedEvent(context, zoneContext, propertyChangeEvents));
        }
    }

    private void changeZone(String propertyName) {
        String zone = getProperty(propertyName, ORIGINAL_ZONE);
        if (ORIGINAL_ZONE.equalsIgnoreCase(zone)) {
            zone = revertOriginalZone();
        }
        zoneContext.setZone(zone);
    }

    private String revertOriginalZone() {
        String originalZone = DEFAULT_ZONE;
        if (zoneLocator.supports(environment)) {
            originalZone = zoneLocator.locate(environment);
        }
        return originalZone;
    }
}

这是整个模块中最重要的机制之一（251 行，第三大的文件）。它的职责：监听 Spring 事件，把 Environment 中的区域配置同步到 ZoneContext，并发布 ZoneContextChangedEvent 供其他模块消费。

反射加载 Event 类名

开头的两行反射：

private static final Class<?> APPLICATION_STARTED_EVENT_CLASS =
    resolveClass(APPLICATION_STARTED_EVENT_CLASS_NAME, classLoader);
private static final Class<?> ENVIRONMENT_CHANGE_EVENT_CLASS =
    resolveClass(ENVIRONMENT_CHANGE_EVENT_CLASS_NAME, classLoader);

为什么用反射而不是直接 import？因为 ApplicationStartedEvent 在 spring-boot 包中，EnvironmentChangeEvent 在 spring-cloud-context 包中。这个模块依赖 spring-cloud-context 不是必须的——如果应用只引入了 Spring Boot（没有 Spring Cloud），classpath 中不存在 EnvironmentChangeEvent。反射避免了 ClassNotFoundException，让这个模块在没有 Spring Cloud 的环境中也能运行。

三种事件类型

supportsEventType 的实现：

Spring Cloud 应用：监听 ApplicationStartedEvent（启动时初始化）+ EnvironmentChangeEvent（运行时配置变更）
Spring Boot 应用（无 Cloud）：监听 ApplicationStartedEvent（启动时初始化）
非 Boot Spring：监听 ContextRefreshedEvent（启动时初始化）

这是分层设计的结果——commons 模块零 Spring 依赖，spring 模块依赖 Spring Framework，spring-boot 模块依赖 Spring Boot，spring-cloud 模块依赖 Spring Cloud。ZoneContextChangedListener 在 spring 模块中，不需要知道 Spring Cloud 的存在。它只在 classpath 存在 EnvironmentChangeEvent 时才注册对这个事件的监听。

属性变更处理链

tryChangeZoneContext 的执行流程：

注册临时 PropertyChangeListener：
zoneContext.addPropertyChangeListener(listener);
listener 的实现：events::add（把每次属性变更事件加入列表）

遍历所有区域相关属性并尝试更新：
for (String propertyName : ZONE_CONTEXT_PROPERTY_NAMES) {
    Consumer<String> handler = propertyChangedHandlers.get(propertyName);
    handler.accept(propertyName);
}

每个 handler 从 Environment 中读取当前值，设置到 ZoneContext 中。如果值没变（ZoneContext 的 setter 内部调 isPropertyChanged），不会触发 PropertyChangeEvent。

收集变更事件并发布：
if (!propertyChangeEvents.isEmpty()) {
    context.publishEvent(new ZoneContextChangedEvent(context, zoneContext, propertyChangeEvents));
}

只有真正发生了变化才发布事件。连续两次改同一个值不会重复触发。

originalZone 回退机制

private void changeZone(String propertyName) {
    String zone = getProperty(propertyName, ORIGINAL_ZONE);
    if (ORIGINAL_ZONE.equalsIgnoreCase(zone)) {
        zone = revertOriginalZone();
    }
    zoneContext.setZone(zone);
}

如果配置中心把 microsphere.availability.zone 改成 "originalZone"，会触发重新自动定位。在运维场景中，手动切换到 shanghai-1 后想切回自动检测时使用。

changeZone 的完整执行：
从 Environment 读取 microsphere.availability.zone 的值
如果值是 "originalZone"：重新调用 ZoneLocator.locate() 自动检测
否则：直接使用配置的值

这个机制的关键：ZoneLocator 接口的支持。如果 ZoneLocator.locate() 的环境检测逻辑在运行时可能返回不同的值（比如从 K8s 环境变量读取），originalZone 回退就能工作。如果 zone 是在部署时硬编码到 application.yml 中的，回退后也是读同一个 yml，没有实际效果。

---

### ZoneUtils.java（39 行）

文件位置：microsphere-multiactive-spring/src/main/java/io/microsphere/multiple/active/zone/spring/ZoneUtils.java

public abstract class ZoneUtils {
    public static final String ZONE_CONTEXT_BEAN_NAME = "zoneContext";
    public static final String ZONE_LOCATOR_BEAN_NAME = "zoneLocator";

    public static ZoneContext getZoneContext(ConfigurableListableBeanFactory beanFactory) {
        return beanFactory.getBean(ZONE_CONTEXT_BEAN_NAME, ZoneContext.class);
    }

    public static ZoneLocator getZoneLocator(ConfigurableListableBeanFactory beanFactory) {
        return beanFactory.getBean(ZONE_LOCATOR_BEAN_NAME, ZoneLocator.class);
    }
}

两个静态方法，通过 Bean 名称从 BeanFactory 中获取 ZoneContext 和 ZoneLocator。用于非 DI 场景（如 BeanPostProcessor 中）。

为什么定义 Bean 名称常量？因为多个地方需要引用相同的 Bean 名称（ZoneAutoConfiguration 中 @Bean(name = ZONE_LOCATOR_BEAN_NAME)），用常量避免硬编码字符串不一致。

---

## 第三部分：spring-boot 包——自动装配

### ConditionalOnAvailabilityZoneEnabled.java（43 行）

文件位置：microsphere-multiactive-spring-boot/src/main/java/io/microsphere/multiple/active/zone/spring/boot/condition/ConditionalOnAvailabilityZoneEnabled.java

@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Documented
@ConditionalOnProperty(name = ZONE_ENABLED_PROPERTY_NAME, matchIfMissing = true)
public @interface ConditionalOnAvailabilityZoneEnabled {}

matchIfMissing = true：即使 application.yml 中没有配置 microsphere.availability.zone.enabled，也默认启用。这保持了"区域功能默认开启"的行为。

ZONE_ENABLED_PROPERTY_NAME 的值是 "microsphere.availability.zone.enabled"。如果用户显式设为 false，则自动装配被跳过。

---

### ConditionalOnAvailabilityZoneAvailable.java（48 行）

文件位置：microsphere-multiactive-spring-boot/src/main/java/io/microsphere/multiple/active/zone/spring/boot/condition/ConditionalOnAvailabilityZoneAvailable.java

@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Documented
@ConditionalOnClass(value = { ZoneContext.class, ZoneLocator.class })
@ConditionalOnAvailabilityZoneEnabled
public @interface ConditionalOnAvailabilityZoneAvailable {}

组合注解。两个条件同时满足才加载：
classpath 中有 ZoneContext.class 和 ZoneLocator.class（commons 和 spring 模块在类路径中）
区域功能启用（@ConditionalOnAvailabilityZoneEnabled）

ZoneCloudAutoConfiguration 使用 @ConditionalOnAvailabilityZoneAvailable，确保只有 commons 和 spring 模块都引入时才激活 Cloud 级别的自动配置。

---

### ZoneAutoConfiguration.java（57 行）

文件位置：microsphere-multiactive-spring-boot/src/main/java/io/microsphere/multiple/active/zone/spring/boot/autoconfigure/ZoneAutoConfiguration.java

@Configuration(proxyBeanMethods = false)
@ConditionalOnAvailabilityZoneAvailable
@Import(value = {ZoneContextChangedListener.class})
public class ZoneAutoConfiguration {

    @Bean(name = ZONE_CONTEXT_BEAN_NAME)
    @ConditionalOnMissingBean
    public ZoneContext zoneContext() { return ZoneContext.get(); }

    @Primary
    @Bean(name = ZONE_LOCATOR_BEAN_NAME)
    @ConditionalOnMissingBean
    public CompositeZoneLocator zoneLocator(
            Collection<ZoneLocator> zoneLocatorBeans,
            ConfigurableApplicationContext context) {

        List<ZoneLocator> zoneLocators = loadFactories(context, ZoneLocator.class);
        List<ZoneLocator> allZoneLocators = new ArrayList<>(zoneLocatorBeans.size() + zoneLocators.size());
        allZoneLocators.addAll(zoneLocatorBeans);
        allZoneLocators.addAll(zoneLocators);
        sort(allZoneLocators);
        return new CompositeZoneLocator(allZoneLocators);
    }
}

ZoneContext @Bean

@ConditionalOnMissingBean：用户可以自定义 ZoneContext 覆盖默认实现。目前 ZoneContext 是单例，这个 @Bean 只是把单例作为 Spring Bean 暴露，让其他组件可以通过 @Autowired 注入。

ZoneLocator 的发现两来源

Spring Bean（zoneLocatorBeans）：用户在 @Configuration 中自定义的 ZoneLocator
Spring Factories（loadFactories 调用）：src/main/resources/META-INF/spring.factories 中注册的 ZoneLocator（AWS/ECS/Default 定位器）

merge 后按 @Order 排序，构建 CompositeZoneLocator。

@Import(value = {ZoneContextChangedListener.class})

直接 @Import 注册 ZoneContextChangedListener，不需要通过 @ComponentScan 发现。因为 ZoneContextChangedListener 不在 commons 的默认包扫描路径下。

---

## 第四部分：spring-cloud 包——Spring Cloud 集成

### ZonePreferenceServiceInstanceListSupplier.java（52 行）

文件位置：microsphere-multiactive-spring-cloud/src/main/java/io/microsphere/multiple/active/zone/spring/cloud/loadbalancer/ZonePreferenceServiceInstanceListSupplier.java

public class ZonePreferenceServiceInstanceListSupplier
        extends DelegatingServiceInstanceListSupplier {

    private final ZonePreferenceFilter<ServiceInstance> zonePreferenceFilter;

    public ZonePreferenceServiceInstanceListSupplier(
            ServiceInstanceListSupplier delegate,
            ZonePreferenceFilter<ServiceInstance> zonePreferenceFilter) {
        super(delegate);
        this.zonePreferenceFilter = zonePreferenceFilter;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return getDelegate().get().map(this::filteredByZone);
    }

    private List<ServiceInstance> filteredByZone(List<ServiceInstance> serviceInstances) {
        return zonePreferenceFilter.filter(serviceInstances);
    }
}

这是 Spring Cloud LoadBalancer 的集成点。

Spring Cloud LoadBalancer 的 ServiceInstanceListSupplier 是一个 Reactive 接口（返回 Flux）。ZonePreferenceServiceInstanceListSupplier 扩展 DelegatingServiceInstanceListSupplier，在 get() 方法中：
调用上游 supplier 获取原始的 ServiceInstance 列表（Flux<List<ServiceInstance>>）
用 .map() 对列表进行区域偏好过滤
apply 到 ZonePreferenceFilter.filter()

map 操作是惰性的——只有当消费者订阅 Flux 时才执行过滤。这在 WebFlux 的响应式链路中不会阻塞。

---

### CustomizedLoadBalancerClientConfiguration.java（102 行）

文件位置：microsphere-multiactive-spring-cloud/src/main/java/io/microsphere/multiple/active/zone/spring/cloud/loadbalancer/CustomizedLoadBalancerClientConfiguration.java

@Configuration(proxyBeanMethods = false)
public class CustomizedLoadBalancerClientConfiguration {

    @Bean
    public ZonePreferenceFilter<ServiceInstance> zonePreferenceFilter(ZoneContext zoneContext) {
        return new ZonePreferenceFilter<>(zoneContext, CloudServerZoneResolver.INSTANCE);
    }

    @ConditionalOnReactiveDiscoveryEnabled
    @Order(193827465)
    static class ReactiveConfiguration {
        @Bean @ConditionalOnMissingBean
        @Conditional(OptimizedZoneConfigurationCondition.class)
        public ServiceInstanceListSupplier optimizedZonePreferenceDiscoveryClientServiceInstanceListSupplier(
                ConfigurableApplicationContext context,
                ZonePreferenceFilter<ServiceInstance> zonePreferenceFilter) {
            return ServiceInstanceListSupplier.builder()
                    .withDiscoveryClient()
                    .withCaching()
                    .with((ctx, delegate) ->
                            new ZonePreferenceServiceInstanceListSupplier(delegate, zonePreferenceFilter))
                    .build(context);
        }
    }

    @ConditionalOnBlockingDiscoveryEnabled
    @Order(193827466)
    static class BlockingConfiguration {
        // 同上，但用 .withBlockingDiscoveryClient()
    }

    static class OptimizedZoneConfigurationCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(
                    context.getEnvironment(), "configurations", "optimized-zone-preference");
        }
    }
}

这里的 ZonePreferenceFilter 是用 CloudServerZoneResolver（从 ServiceInstance.metadata 中读取区域）创建的。

两个内部配置类：ReactiveConfiguration（WebFlux）和 BlockingConfiguration（Spring MVC），共用 ServiceInstanceListSupplier.builder() 链式构建。

OptimizedZoneConfigurationCondition：

需要配置 spring.cloud.loadbalancer.configurations=optimized-zone-preference 才激活。这是 opt-in 设计，避免区域偏好路由意外改变生产环境的路由行为。

为什么需要显式 opt-in？因为区域偏好路由会改变负载均衡行为，可能会让原本分布在多个区域的流量突然集中在同一个区域。在不知情的情况下开启可能会导致：

同区域实例过载
其他区域实例空闲
跨区域调用的故障转移失效

Order 的值 193827465 和 193827466 是一个足够大的随机值，确保这个 Filter 在默认 Filter 链的最后执行。

---

### CustomizedLoadBalancerAutoConfiguration.java（21 行）

文件位置：microsphere-multiactive-spring-cloud/src/main/java/io/microsphere/multiple/active/zone/spring/cloud/loadbalancer/CustomizedLoadBalancerAutoConfiguration.java

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "microsphere.spring.cloud.loadbalancer.customized", havingValue = "true")
@ConditionalOnClass(name = { "org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient" })
@LoadBalancerClients(defaultConfiguration = CustomizedLoadBalancerClientConfiguration.class)
public class CustomizedLoadBalancerAutoConfiguration {
}

21 行，"最轻量"的自动配置。只有一个作用：通过 @LoadBalancerClients(defaultConfiguration = ...) 把 CustomizedLoadBalancerClientConfiguration 作为所有 LoadBalancerClient 的默认配置。

@ConditionalOnProperty 需要显式设置 microsphere.spring.cloud.loadbalancer.customized=true 才能启用。再次 opt-in。

---

### CloudServerZoneResolver.java（31 行）

文件位置：microsphere-multiactive-spring-cloud/src/main/java/io/microsphere/multiple/active/zone/spring/cloud/CloudServerZoneResolver.java

public class CloudServerZoneResolver implements ZoneResolver<ServiceInstance> {

    public static final CloudServerZoneResolver INSTANCE = new CloudServerZoneResolver();

    @Override
    public String resolve(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        if (metadata != null) {
            return metadata.get(ZONE_PROPERTY_NAME);
        }
        return null;
    }
}

从 ServiceInstance.getMetadata() 中读取 microsphere.availability.zone。这个 metadata 是服务注册时 ZoneAttachmentHandler 写入的。

单例 INSTANCE 模式（和 ZoneContext 类似），因为 CloudServerZoneResolver 没有状态，不需要多个实例。

---

### ZoneAttachmentListener.java（39 行）

文件位置：microsphere-multiactive-spring-cloud/src/main/java/io/microsphere/multiple/active/zone/spring/cloud/event/ZoneAttachmentListener.java

public class ZoneAttachmentListener
        implements ApplicationListener<RegistrationPreRegisteredEvent>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(RegistrationPreRegisteredEvent event) {
        Registration registration = event.getRegistration();
        Map<String, String> metadata = registration.getMetadata();
        ZoneAttachmentHandler handler = applicationContext.getBean(ZoneAttachmentHandler.class);
        handler.attachZone(metadata);
    }
}

监听 RegistrationPreRegisteredEvent（05-spring-cloud 模块发布的事件），在服务注册前调用 ZoneAttachmentHandler.attachZone() 写入区域信息。

@ConditionalOnBean(type = "io.microsphere.spring.cloud.client.service.registry.aspect.EventPublishingRegistrationAspect")

ZoneCloudAutoConfiguration 中创建这个 Bean 时带条件——只有 EventPublishingRegistrationAspect 在 classpath 中（即 05-spring-cloud 模块可用）才创建。

---

### ZoneCloudAutoConfiguration.java（55 行）

文件位置：microsphere-multiactive-spring-cloud/src/main/java/io/microsphere/multiple/active/zone/spring/cloud/autoconfigure/ZoneCloudAutoConfiguration.java

@Configuration(proxyBeanMethods = false)
@ConditionalOnAvailabilityZoneAvailable
@ConditionalOnAutoServiceRegistrationEnabled
@ConditionalOnClass(name = {
    "org.aspectj.lang.annotation.Aspect",
    "io.microsphere.spring.cloud.client.service.registry.event.RegistrationPreRegisteredEvent",
    REGISTRATION_CLASS_NAME
})
@ConditionalOnBean(ZoneContext.class)
@AutoConfigureAfter(value = { ZoneAutoConfiguration.class }, name = {
    "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration",
    "io.microsphere.spring.cloud.client.service.registry.autoconfigure.ServiceRegistryAutoConfiguration"
})
@Import(value = { ZoneAttachmentHandler.class })
public class ZoneCloudAutoConfiguration {

    @Bean
    @ConditionalOnBean(type = "io.microsphere.spring.cloud.client.service.registry.aspect.EventPublishingRegistrationAspect")
    @ConditionalOnMissingBean
    public ZoneAttachmentListener zoneAttachmentListener() {
        return new ZoneAttachmentListener();
    }
}

五个条件同时满足才激活：

@ConditionalOnAvailabilityZoneAvailable：ZoneContext 和 ZoneLocator 在 classpath，且区域功能启用
@ConditionalOnAutoServiceRegistrationEnabled：自动服务注册启用
@ConditionalOnClass：classpath 中有 AspectJ、RegistrationPreRegisteredEvent、Registration 类
@ConditionalOnBean(ZoneContext.class)：ZoneContext Bean 已注册
@ConditionalOnBean(EventPublishingRegistrationAspect.class)：AOP 切面已存在

@AutoConfigureAfter 确保在所有前置自动配置完成之后才执行：ZoneAutoConfiguration（Core）、EurekaClientAutoConfiguration（Eureka 注册）、ServiceRegistryAutoConfiguration（通用注册）。

@Import(ZoneAttachmentHandler.class) 注册 ZoneAttachmentHandler（但不是 @Bean，是通过 @Import 直接创建定义）。这种方式让 ZoneAttachmentHandler 的实例化由 Spring 管理。

---

## 第五部分：netflix 包——Eureka + Ribbon 适配

### EurekaInstanceInfoZoneResolver.java（24 行）

文件位置：microsphere-multiactive-netflix/src/main/java/io/microsphere/multiple/active/zone/netflix/eureka/EurekaInstanceInfoZoneResolver.java

public class EurekaInstanceInfoZoneResolver implements ZoneResolver<InstanceInfo> {
    @Override
    public String resolve(InstanceInfo instanceInfo) {
        Map<String, String> metadata = instanceInfo.getMetadata();
        return metadata == null ? null : metadata.get(ZONE_PROPERTY_NAME);
    }
}

从 Eureka 的 InstanceInfo.metadata 中读取区域。和 CloudServerZoneResolver 功能相同，只是实体类型不同——一个是 ServiceInstance，一个是 InstanceInfo。

Netflix 模块中还有 ZoneAttachmentPreRegistrationHandler 负责在 Eureka 注册前写入区域（在 ZoneAttachmentHandler 部分已分析）。

---

### RibbonServerZoneResolver.java（35 行）

文件位置：microsphere-multiactive-netflix/src/main/java/io/microsphere/multiple/active/zone/netflix/ribbon/RibbonServerZoneResolver.java

public class RibbonServerZoneResolver<T extends Server> implements ZoneResolver<T> {
    @Override
    public String resolve(T server) {
        return server.getZone();
    }
}

和上面两个解析器不同，RibbonServerZoneResolver 不是从 metadata 中读区域，而是直接调 Server.getZone()。Ribbon 的 Server 类本身就有 zone 字段（setZone/getZone）。这个字段由 DiscoveryClientServer（本模块中的另一个类）在构造时设置。

为什么 Ribbon 的 Server 有独立的 zone 字段？因为 Ribbon 是比 Spring Cloud 更早的负载均衡库，它在 Spring Cloud 出现之前就有了自己的 Server 模型。zone 字段在 Server 类中一直存在。

---

### ZonePreferenceServerListFilter.java（45 行）

文件位置：microsphere-multiactive-netflix/src/main/java/io/microsphere/multiple/active/zone/netflix/ribbon/ZonePreferenceServerListFilter.java

public class ZonePreferenceServerListFilter<T extends Server> implements ServerListFilter<T> {
    private ZonePreferenceFilter<T> filter;

    public ZonePreferenceServerListFilter(ZonePreferenceFilter<T> filter) {
        this.filter = filter;
    }

    @Override
    public List<T> getFilteredListOfServers(List<T> servers) {
        return filter.filter(servers);
    }
}

Ribbon 的 ServerListFilter 适配器。将 ZonePreferenceFilter 适配为 Ribbon 的 ServerListFilter 接口。

Ribbon 的过滤链在 loadBalancer.getAllServers() 时触发。ZonePreferenceServerListFilter 在过滤链中按 getOrder() 排序执行。

这个适配器类的工作量极低（45 行），因为核心逻辑全部在 ZonePreferenceFilter 中。

---

### DiscoveryClientServer.java（86 行）

文件位置：microsphere-multiactive-netflix/src/main/java/io/microsphere/multiple/active/zone/netflix/spring/cloud/ribbon/DiscoveryClientServer.java

public class DiscoveryClientServer extends Server {
    private final ServiceInstance serviceInstance;
    private final ServiceInstanceMetaInfo metaInfo;

    public DiscoveryClientServer(ServiceInstance serviceInstance) {
        super(serviceInstance.getScheme(), serviceInstance.getHost(), serviceInstance.getPort());
        this.serviceInstance = serviceInstance;
        this.metaInfo = new ServiceInstanceMetaInfo(serviceInstance);
        this.setZone(resolveZone(serviceInstance));
    }

    private String resolveZone(ServiceInstance serviceInstance) {
        return CloudServerZoneResolver.INSTANCE.resolve(serviceInstance);
    }

    static final class ServiceInstanceMetaInfo implements MetaInfo {
        private final ServiceInstance serviceInstance;

        @Override
        public String getAppName() { return serviceInstance.getServiceId(); }

        @Override
        public String getInstanceId() { return serviceInstance.getInstanceId(); }
    }
}

把 Spring Cloud 的 ServiceInstance 适配为 Ribbon 的 Server。构造时用 CloudServerZoneResolver 从 ServiceInstance 的 metadata 中解析区域，设置到 Server.zone 字段。

setZone(resolveZone(serviceInstance)) 是关键一行。Ribbon 的 Server.zone 字段通常由 Ribbon 自己的配置（NIWSServerListFilter）设置。通过 DiscoveryClientServer，Spring Cloud 中的区域信息（在 Nacos 中配置的 metadata）被传递到 Ribbon 的 Server 模型中。

ServiceInstanceMetaInfo 实现了 Server.MetaInfo 接口，暴露 appName 和 instanceId，供 Ribbon 的统计组件使用。

---

### DiscoveryClientServerList.java（56 行）

文件位置：microsphere-multiactive-netflix/src/main/java/io/microsphere/multiple/active/zone/netflix/spring/cloud/ribbon/DiscoveryClientServerList.java

public class DiscoveryClientServerList implements ServerList<DiscoveryClientServer> {
    private final DiscoveryClient discoveryClient;
    private final String serviceName;

    @Override
    public List<DiscoveryClientServer> getInitialListOfServers() {
        return getUpdatedListOfServers();
    }

    @Override
    public List<DiscoveryClientServer> getUpdatedListOfServers() {
        return discoveryClient.getInstances(serviceName)
                .stream()
                .map(DiscoveryClientServer::new)
                .collect(Collectors.toList());
    }
}

把 Spring Cloud 的 DiscoveryClient 适配为 Ribbon 的 ServerList。每次 Ribbon 需要刷新服务器列表时，调用 getUpdatedListOfServers()：

从 DiscoveryClient 获取当前服务的所有 ServiceInstance
每个 ServiceInstance 用 DiscoveryClientServer 包装为 Ribbon Server（构造时设置 zone）

---

### ZoneAttachmentPreRegistrationHandler.java（36 行）

文件位置：microsphere-multiactive-netflix/src/main/java/io/microsphere/multiple/active/zone/netflix/eureka/ZoneAttachmentPreRegistrationHandler.java

public class ZoneAttachmentPreRegistrationHandler implements PreRegistrationHandler {
    private final ApplicationInfoManager applicationInfoManager;
    private final ZoneAttachmentHandler zoneAttachmentHandler;

    @Override
    public void beforeRegistration() {
        InstanceInfo instanceInfo = applicationInfoManager.getInfo();
        Map<String, String> metadata = instanceInfo.getMetadata();
        zoneAttachmentHandler.attachZone(metadata);
    }
}

Eureka 的 PreRegistrationHandler SPI。在 Eureka 客户端注册之前执行，将当前区域写入 Eureka 实例的 metadata。

Eureka 的 PreRegistrationHandler 是 AbstractDiscoveryClientOptionalArgs 的一部分，通过 AbstractDiscoveryClientOptionalArgs.setPreRegistrationHandler() 注册。这个 SPI 提供了在注册前修改 InstanceInfo 的切入点——ZoneAttachmentPreRegistrationHandler 利用这个切入点把区域信息注入 metadata。

ZoneAttachmentPreRegistrationHandler 和 ZoneAttachmentListener（Spring Cloud 版本）的区别：
ZoneAttachmentPreRegistrationHandler：Eureka 原生 SPI，在 Eureka 客户端内部触发
ZoneAttachmentListener：Spring Cloud AOP，在 RegistrationPreRegisteredEvent 触发

当 Eureka + Spring Cloud 同时使用时，两条路径都会执行（ZoneAttachmentHandler.attachZone 被调两次）。两次调用是幂等的——第一次写入，第二次覆盖为相同值，不会产生问题。

---

## 第六部分：aws 包——AWS 区域定位

### Ec2AvailabilityZoneEndpointZoneLocator.java（66 行）

文件位置：microsphere-multiactive-aws/src/main/java/io/microsphere/multiple/active/zone/spring/aws/Ec2AvailabilityZoneEndpointZoneLocator.java

public class Ec2AvailabilityZoneEndpointZoneLocator extends AbstractZoneLocator
        implements EnvironmentAware {

    public static final String DEFAULT_AVAILABILITY_ZONE_ENDPOINT_URI =
        "http://169.254.169.254/latest/meta-data/placement/availability-zone";

    public static final int DEFAULT_ORDER = 15;

    @Override
    public boolean supports(Environment environment) { return true; }

    @Override
    public String locate(Environment environment) {
        String uri = getAvailabilityZoneEndpointURI(environment);
        String zone = null;
        if (StringUtils.hasText(uri)) {
            try {
                zone = HttpUtils.doGet(uri, timeout);
            } catch (Throwable e) {
                logger.error("Request Amazon EC2 Availability Zone Endpoint[URI : '{}'] failed", uri, e);
            }
        }
        return zone;
    }

    private String getAvailabilityZoneEndpointURI(Environment environment) {
        return environment.getProperty(
            "EC2_AVAILABILITY_ZONE_ENDPOINT_URI", DEFAULT_AVAILABILITY_ZONE_ENDPOINT_URI);
    }
}

从 AWS EC2 实例的元数据端点获取可用区。169.254.169.254 是 AWS 提供的 link-local 地址，仅在同个 EC2 实例内可访问。这是 AWS 元数据服务的标准地址。

supports() 返回 true，所以 EC2ZoneLocator 总会参与定位（但 DefaultZoneLocator 也会参与，因为优先级 order=15 < 20，EC2 定位器先执行，如果超时返回 null，DefaultZoneLocator 兜底）。

"169.254.169.254/latest/meta-data/placement/availability-zone" 的返回值通常是 us-east-1a、cn-beijing-b 等。AWS 为每个 EC2 实例自动注入这些元数据，不需要任何配置。

非 AWS 环境中 169.254.169.254 的连接行为：操作系统看到 169.254.x.x（link-local 地址段）时会尝试连接本地链路。如果本地链路上没有设备响应，连接会在 3 秒后超时（默认 timeout 配置）。这就是为什么 fast-fail 默认 false——在非 AWS 环境中，EC2 定位器超时后不会阻止应用启动。

---

### EcsContainerMetadataFileZoneLocator.java（75 行）

文件位置：microsphere-multiactive-aws/src/main/java/io/microsphere/multiple/active/zone/spring/aws/EcsContainerMetadataFileZoneLocator.java

public class EcsContainerMetadataFileZoneLocator extends AbstractZoneLocator {
    public static final String METADATA_FILE_ENV_NAME = "ECS_CONTAINER_METADATA_FILE";
    public static final int DEFAULT_ORDER = 5;

    @Override
    public boolean supports(Environment environment) {
        return environment.containsProperty(METADATA_FILE_ENV_NAME);
    }

    @Override
    public String locate(Environment environment) {
        String metadataFilePath = environment.getProperty(METADATA_FILE_ENV_NAME);
        String zone = null;
        if (StringUtils.hasText(metadataFilePath)) {
            File metadataFile = new File(metadataFilePath);
            if (metadataFile.canRead()) {
                try (InputStream inputStream = new FileInputStream(metadataFile)) {
                    String json = copyToString(inputStream, UTF_8);
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map metadata = objectMapper.readValue(json, Map.class);
                    Object zoneValue = metadata.get("AvailabilityZone");
                    zone = zoneValue == null ? null : String.valueOf(zoneValue);
                }
            }
        }
        return zone;
    }
}

supports() 检查 ECS_CONTAINER_METADATA_FILE 环境变量是否存在。这个环境变量由 AWS ECS 容器代理自动注入（版本 1.15.0+）。文件路径示例：/var/lib/ecs/data/ecs-container-metadata-XXXX.json。

为什么 order=5（最高优先级）？ECS 容器可能运行在 EC2 实例上，也满足 EC2 定位器的条件。但 ECS 的 metadata 文件提供了更精确的区域信息（容器级），EC2 元数据端点提供的是宿主机级的信息。ECS 优先。

如果 ECS 容器代理版本低于 1.15.0，EnvironmentVariable 不存在，supports() 返回 false，CompositeZoneLocator 跳过这个定位器。

为什么用 Jackson ObjectMapper 解析 JSON？ECS 的 metadata 文件是 JSON 格式，包含 AvailabilityZone、DockerId、ContainerARN、Cluster 等字段。只需要读 AvailabilityZone。ObjectMapper 是常见的 JSON 解析库，函数式读取 Map 而不定义 POJO。

---

### EcsTaskMetadataEndpointV4ZoneLocator.java（77 行）

文件位置：microsphere-multiactive-aws/src/main/java/io/microsphere/multiple/active/zone/spring/aws/EcsTaskMetadataEndpointV4ZoneLocator.java

public class EcsTaskMetadataEndpointV4ZoneLocator extends AbstractZoneLocator
        implements EnvironmentAware {

    public static final String METADATA_URI_V4_ENV_NAME = "ECS_CONTAINER_METADATA_URI_V4";
    public static final int DEFAULT_ORDER = 10;

    @Override
    public boolean supports(Environment environment) {
        return environment.containsProperty(METADATA_URI_V4_ENV_NAME);
    }

    @Override
    public String locate(Environment environment) {
        String uri = environment.getProperty(METADATA_URI_V4_ENV_NAME);
        // 请求 URI/task 端点，解析 JSON 中的 AvailabilityZone 字段
        String taskURL = uri + "/task";
        String json = HttpUtils.doGet(taskURL, timeout);
        Map metadata = objectMapper.readValue(json, Map.class);
        zone = String.valueOf(metadata.get("AvailabilityZone"));
        return zone;
    }
}

ECS Task Metadata V4 是 ECS 容器代理 1.39.0+ 版本引入的新版元数据端点。和 EcsContainerMetadataFileZoneLocator 的不同：

EcsContainerMetadataFileZoneLocator：读本地文件（ECS_CONTAINER_METADATA_FILE）
EcsTaskMetadataEndpointV4ZoneLocator：请求 HTTP 端点（ECS_CONTAINER_METADATA_URI_V4/task）

V4 端点是更现代化的方式，但文件方式更稳定（不依赖网络可达性）。两个定位器可以重叠存在（同一个 ECS 任务中可能有文件也有 V4 端点），通过 order（5 vs 10）确保文件定位器优先。

V4 端点是 AWS 在 2020 年左右推出，提供了比 V3 和文件方式更丰富的元数据（任务级、容器级、标签等）。microsphere 只用了 AvailabilityZone 这个基本字段。

---

## 总结

### 31 个文件的职责分布

commons（6 文件）：核心数据模型和算法，Zero Spring 依赖
  ZoneContext：全局区域状态（单例）
  ZoneConstants：所有配置名称和默认值
  ZoneResolver：区域解析 SPI
  ZonePreferenceFilter：10 步路由决策算法
  ZoneAttachmentHandler：区域信息写入 metadata
  HttpUtils：简单的 HTTP GET 工具

spring（7 文件）：Spring 集成入口
  ZoneLocator：区域定位 SPI
  AbstractZoneLocator：定位器抽象基类
  CompositeZoneLocator：组合定位器（链式尝试 + 缓存 + fast-fail）
  DefaultZoneLocator：从 application.yml 读取
  ZoneContextChangedEvent：区域变更事件
  ZoneContextChangedListener：动态配置监听器（251 行，核心）
  ZoneUtils：Bean 名称和查找工具

spring-boot（3 文件）：自动装配
  ZoneAutoConfiguration：装配 ZoneContext + CompositeZoneLocator
  ConditionalOnAvailabilityZoneEnabled：开关条件
  ConditionalOnAvailabilityZoneAvailable：可用条件

spring-cloud（6 文件）：Spring Cloud 集成
  ZonePreferenceServiceInstanceListSupplier：LoadBalancer 适配
  CustomizedLoadBalancerClientConfiguration：LoadBalancer 配置
  CloudServerZoneResolver：ServiceInstance 区域解析
  ZoneAttachmentListener：注册事件监听
  ZoneCloudAutoConfiguration：Cloud 自动配置

netflix（6 文件）：Netflix 生态适配
  EurekaInstanceInfoZoneResolver：Eureka InstanceInfo 区域解析
  RibbonServerZoneResolver：Ribbon Server 区域解析
  ZonePreferenceServerListFilter：Ribbon 过滤适配
  DiscoveryClientServer：Spring Cloud -> Ribbon 适配
  DiscoveryClientServerList：DiscoveryClient -> ServerList 适配
  ZoneAttachmentPreRegistrationHandler：Eureka 注册前写入

aws（3 文件）：AWS 环境定位
  Ec2AvailabilityZoneEndpointZoneLocator：EC2 元数据端点
  EcsContainerMetadataFileZoneLocator：ECS 元数据文件
  EcsTaskMetadataEndpointV4ZoneLocator：ECS V4 元数据端点

### 核心脉络：4 个能力阶段

区域发现（ZoneLocator -> DefaultZoneLocator -> 3 个 AWS 定位器）-> 应用启动时自动定位
  ZoneAttachmentHandler -> 注册时将区域写入再往中心 metadata
    CloudServerZoneResolver -> 其他服务解析区域
      区域优先路由（ZonePreferenceFilter -> ZonePreferenceServiceInstanceListSupplier/ZonePreferenceServerListFilter）-> 每次负载均衡时执行
  ZoneContextChangedListener -> 配置中心变更时重新读取
    信息传播（ZoneAttachmentHandler -> 下次注册时写入新区域）
      ZoneContextChangedEvent -> 触发资源切换（Redis/DataSource）
