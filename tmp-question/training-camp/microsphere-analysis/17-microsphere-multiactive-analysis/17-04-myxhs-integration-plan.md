# 17-04：my-xhs 异地多活整合方案

> **核心命题**：my-xhs 的 zone 包已有 21 个文件覆盖了核心路由和 DataSource 切换，缺的是区域自动定位（ZoneLocator）和动态重配置（ZoneContextChangedListener）。本方案用 7 个新文件补齐全部能力，对齐简历描述的"统一 Availability Zone 定位抽象、配置中心秒级切换与回滚"。
> **本文覆盖**：现状 vs 目标差距分析 + 7 个文件设计 + 修改现有文件 + 配置变化 + 测试策略。

---

## 第一部分：现状评估

### 已有——21 个文件

```
my-xhs-common/src/main/java/com/myxhs/common/zone/
├── ZoneContext.java                  # 单例区域状态
├── ZoneConstants.java                # 常量（已含 Locator 属性名）
├── ZoneContextAutoConfiguration.java # 自动配置
├── ZoneProperties.java               # 配置属性
├── ZoneResolver.java                 # 区域解析接口
├── ZonePreferenceFilter.java         # 区域优先过滤器（核心算法）
├── loadbalancer/
│   ├── ServiceInstanceZoneResolver.java      # ServiceInstance 解析器
│   ├── ZonePreferenceServiceInstanceListSupplier.java  # LoadBalancer 集成
│   └── ZoneLoadBalancerConfiguration.java    # LoadBalancer 配置
├── datasource/
│   └── DynamicDataSource.java        # 动态数据源（切换等待机制）
├── redis/                            # Redis 拦截器链完整实现
│   ├── config/RedisInterceptorAutoConfiguration.java
│   ├── event/RedisCommandEvent.java
│   ├── interceptor/EventPublishingRedisCommandInterceptor.java
│   ├── interceptor/RedisMethodContext.java
│   ├── interceptor/RedisMethodInterceptor.java
│   ├── wrapper/InterceptingRedisConnectionInvocationHandler.java
│   └── wrapper/RedisTemplateWrapper.java
└── test/
    ├── ZoneConstantsTest.java
    ├── ZoneContextTest.java
    ├── ZonePreferenceFilterTest.java
    └── ZoneResolverTest.java
```

### 已有能力

| 能力 | 状态 | 实现类 |
|------|------|--------|
| ZoneContext 单例管理 | ✅ | ZoneContext |
| 区域优先路由（10 步过滤算法） | ✅ | ZonePreferenceFilter |
| LoadBalancer 集成 | ✅ | ZonePreferenceServiceInstanceListSupplier + ZoneLoadBalancerConfiguration |
| DynamicDataSource 切换等待 | ✅ | DynamicDataSource（含活跃连接计数 + waitForActiveConnections） |
| Redis 拦截器链 | ✅ | 7 个 redis 文件完整实现 |
| 区域解析 SPI | ✅ | ZoneResolver |

### 缺失能力

| 能力 | 状态 | 涉及文件数 | 对应 microsphere 参考 |
|------|------|-----------|---------------------|
| 区域自动定位（ZoneLocator SPI） | ❌ | 4 个新文件 | ZoneLocator + AbstractZoneLocator + CompositeZoneLocator + DefaultZoneLocator |
| AWS 区域定位（EC2/ECS） | ❌ | 3 个新文件 | 3 个 AWS ZoneLocator |
| 动态重配置（配置中心秒级切换） | ❌ | 2 个新文件 + 改 1 个现有 | ZoneContextChangedEvent + ZoneContextChangedListener |
| 区域信息注册到 Nacos | ❌ | 1 个新文件 | ZoneAttachmentHandler |

### 和简历描述的差距

| 简历 | 状态 | 补齐方式 |
|------|------|---------|
| "统一 Availability Zone 定位抽象，无论 AWS EC2/ECS 还是本地" | ❌ | ZoneLocator SPI + 4 个定位器（Default + 3 个 AWS） |
| "服务调用、数据存储同区域访问" | ✅ | LoadBalancer + DynamicDataSource |
| "内建保护策略，防止单区域负载过高" | ✅ | same-zone-min-available |
| "配置中心秒级切换到非故障区域" | ❌ | ZoneContextChangedListener |
| "通过回滚配置，秒级恢复到同区域优先" | ❌ | originalZone 回退机制（在 ZoneContextChangedListener 中） |

---

## 第二部分：新增 7 个文件

### 文件 1：ZoneLocator.java

```
路径：my-xhs-common/src/main/java/com/myxhs/common/zone/locator/ZoneLocator.java
```

```java
package com.myxhs.common.zone.locator;

import org.springframework.core.env.Environment;

@FunctionalInterface
public interface ZoneLocator {
    boolean supports(Environment environment);
    String locate(Environment environment);
}
```

和 ZoneResolver 的区别：ZoneLocator 定位"当前应用在哪个区域"，启动时调用一次。ZoneResolver 解析"某个服务实例在哪个区域"，每次 LoadBalancer 调用时执行。

### 文件 2：AbstractZoneLocator.java

```
路径：my-xhs-common/src/main/java/com/myxhs/common/zone/locator/AbstractZoneLocator.java
```

```java
package com.myxhs.common.zone.locator;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;

public abstract class AbstractZoneLocator implements ZoneLocator, BeanNameAware, Ordered {
    protected int order;
    
    public AbstractZoneLocator(int order) {
        this.order = order;
    }
    
    @Override
    public final int getOrder() { return order; }
    
    @Override
    public final void setBeanName(String name) {}
}
```

每个 ZoneLocator 有 order 值，CompositeZoneLocator 按 order 排序后依次尝试，第一个非 null 结果生效。

### 文件 3：CompositeZoneLocator.java

```
路径：my-xhs-common/src/main/java/com/myxhs/common/zone/locator/CompositeZoneLocator.java
```

```java
package com.myxhs.common.zone.locator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

import static io.microsphere.multiple.active.zone.ZoneConstants.CURRENT_ZONE_PROPERTY_NAME;
import static io.microsphere.multiple.active.zone.ZoneConstants.DEFAULT_LOCATOR_FAST_FAIL;
import static io.microsphere.multiple.active.zone.ZoneConstants.LOCATOR_FAST_FAIL_PROPERTY_NAME;

@Slf4j
public class CompositeZoneLocator implements ZoneLocator {
    private final List<ZoneLocator> zoneLocators;
    private volatile String zone;

    public CompositeZoneLocator(List<ZoneLocator> zoneLocators) {
        this.zoneLocators = new ArrayList<>(zoneLocators);
        AnnotationAwareOrderComparator.sort(this.zoneLocators);
    }

    @Override
    public boolean supports(Environment environment) {
        for (ZoneLocator locator : zoneLocators) {
            if (locator.supports(environment)) return true;
        }
        return false;
    }

    @Override
    public String locate(Environment environment) {
        if (zone != null) return zone;

        boolean fastFail = environment.getProperty(
            LOCATOR_FAST_FAIL_PROPERTY_NAME, boolean.class, DEFAULT_LOCATOR_FAST_FAIL);

        for (ZoneLocator locator : zoneLocators) {
            try {
                if (locator.supports(environment)) {
                    String result = locator.locate(environment);
                    if (result != null) {
                        this.zone = result;
                        System.setProperty(CURRENT_ZONE_PROPERTY_NAME, result);
                        log.info("Zone located: {} by {}", result, locator.getClass().getSimpleName());
                        break;
                    }
                }
            } catch (Throwable e) {
                if (fastFail) throw new IllegalStateException("Zone locate failed", e);
                log.warn("ZoneLocator {} failed", locator.getClass().getSimpleName(), e);
            }
        }
        return zone;
    }
}
```

关键设计：

按 @Order 排序后逐个尝试，第一个非 null 结果生效
成功后写入 System property（ZoneContext.getCurrentZone() 能读到）
fast-fail 默认 false（非 AWS 环境，EC2 定位器超时不会阻止启动）
zone 字段为 volatile，缓存定位结果

### 文件 4：DefaultZoneLocator.java

```
路径：my-xhs-common/src/main/java/com/myxhs/common/zone/locator/DefaultZoneLocator.java
```

```java
package com.myxhs.common.zone.locator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import static com.myxhs.common.zone.ZoneConstants.DEFAULT_ZONE;
import static com.myxhs.common.zone.ZoneConstants.ZONE_PROPERTY_NAME;

@Slf4j
public class DefaultZoneLocator extends AbstractZoneLocator {
    public static final int ORDER = 40;

    public DefaultZoneLocator() {
        super(ORDER);
    }

    @Override
    public boolean supports(Environment environment) {
        return true;
    }

    @Override
    public String locate(Environment environment) {
        // 优先级：环境变量 > application.yml
        String zone = System.getenv("MYXHS_ZONE");
        if (zone != null && !zone.isEmpty()) {
            log.info("Zone located from env MYXHS_ZONE: {}", zone);
            return zone;
        }
        zone = environment.getProperty(ZONE_PROPERTY_NAME);
        if (zone != null && !zone.isEmpty()) {
            log.info("Zone located from property {}: {}", ZONE_PROPERTY_NAME, zone);
            return zone;
        }
        return DEFAULT_ZONE;
    }
}
```

读取顺序：环境变量 MYXHS_ZONE -> application.yml 中的 myxhs.availability.zone -> "defaultZone"。

环境变量优先：K8s 部署时通过 Downward API 注入 `topology.kubernetes.io/zone` 到 `MYXHS_ZONE`，不需要改 application.yml。

Order=40：在所有 AWS 定位器之后执行（AWS 定位器 order=5/10/15）。

### 文件 5~7：AWS 定位器（可选，按需启用）

仅部署在 AWS 时才需要。文件内容直接从 microsphere 的 aws 子模块复制：

| 文件 | 来源 | order |
|------|------|-------|
| `EcsContainerMetadataFileZoneLocator.java` | microsphere 同文件 | 5 |
| `EcsTaskMetadataEndpointV4ZoneLocator.java` | microsphere 同文件 | 10 |
| `Ec2AvailabilityZoneEndpointZoneLocator.java` | microsphere 同文件 | 15 |

路径前缀：`my-xhs-common/src/main/java/com/myxhs/common/zone/locator/aws/`

### 文件 8：ZoneContextChangedEvent.java

```
路径：my-xhs-common/src/main/java/com/myxhs/common/zone/event/ZoneContextChangedEvent.java
```

```java
package com.myxhs.common.zone.event;

import com.myxhs.common.zone.ZoneContext;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

import java.beans.PropertyChangeEvent;
import java.util.List;

@Getter
public class ZoneContextChangedEvent extends ApplicationContextEvent {
    private final ZoneContext zoneContext;
    private final List<PropertyChangeEvent> propertyChangeEvents;

    public ZoneContextChangedEvent(ApplicationContext context,
                                    ZoneContext zoneContext,
                                    List<PropertyChangeEvent> propertyChangeEvents) {
        super(context);
        this.zoneContext = zoneContext;
        this.propertyChangeEvents = propertyChangeEvents;
    }
}
```

Spring ApplicationEvent。DynamicDataSource 通过 @EventListener 监听这个事件（目前 my-xhs 的 DynamicDataSource 直接监听 PropertyChangeEvent，不需要改——但建议逐步迁移到 Spring 事件）。

### 文件 9：ZoneContextChangedListener.java

```
路径：my-xhs-common/src/main/java/com/myxhs/common/zone/listener/ZoneContextChangedListener.java
```

```java
package com.myxhs.common.zone.listener;

import com.myxhs.common.zone.ZoneConstants;
import com.myxhs.common.zone.ZoneContext;
import com.myxhs.common.zone.locator.CompositeZoneLocator;
import com.myxhs.common.zone.locator.ZoneLocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.beans.PropertyChangeEvent;
import java.util.*;

@Slf4j
public class ZoneContextChangedListener
        implements ApplicationListener<EnvironmentChangeEvent>, EnvironmentAware {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ZoneContext zoneContext;

    @Autowired(required = false)
    private ZoneLocator zoneLocator;

    private Environment environment;

    private static final List<String> ZONE_PROPERTY_NAMES = Arrays.asList(
        ZoneConstants.ZONE_PROPERTY_NAME,
        ZoneConstants.PREFERENCE_ENABLED_PROPERTY_NAME,
        ZoneConstants.PREFERENCE_FILTER_ORDER_PROPERTY_NAME,
        ZoneConstants.PREFERENCE_UPSTREAM_ZONE_READY_PERCENTAGE_PROPERTY_NAME,
        ZoneConstants.PREFERENCE_UPSTREAM_SAME_ZONE_MIN_AVAILABLE_PROPERTY_NAME,
        ZoneConstants.PREFERENCE_UPSTREAM_DISABLED_ZONE_PROPERTY_NAME
    );

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        // 只关心 myxhs.availability.zone.* 属性的变更
        Set<String> keys = event.getKeys();
        if (keys == null || keys.stream().noneMatch(k -> k.startsWith("myxhs.availability.zone"))) {
            return;
        }

        List<PropertyChangeEvent> changes = new LinkedList<>();
        PropertyChangeListener listener = changes::add;

        zoneContext.addPropertyChangeListener(listener);
        try {
            // 逐个读取属性，更新 ZoneContext
            String newZone = environment.getProperty(ZoneConstants.ZONE_PROPERTY_NAME);
            if (newZone != null) {
                handleZoneChange(newZone);
            }
            // 其他属性（preferenceEnabled, upstream 等）同理
        } finally {
            zoneContext.removePropertyChangeListener(listener);
        }

        if (!changes.isEmpty()) {
            applicationContext.publishEvent(
                new ZoneContextChangedEvent(applicationContext, zoneContext, changes));
        }
    }

    private void handleZoneChange(String zone) {
        if ("originalZone".equalsIgnoreCase(zone)) {
            if (zoneLocator != null) {
                zone = zoneLocator.locate(environment);
            } else {
                zone = ZoneConstants.DEFAULT_ZONE;
            }
        }
        zoneContext.setZone(zone);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @FunctionalInterface
    private interface PropertyChangeListener {
        void accept(PropertyChangeEvent event);
    }
}
```

关键设计：

只过滤 `myxhs.availability.zone` 前缀的属性变更，不处理无关属性
`originalZone` 回退机制：把 zone 设为 "originalZone" 时触发 ZoneLocator 自动重新定位
监听 EnvironmentChangeEvent（Nacos 配置变更时触发）
变更后发布 ZoneContextChangedEvent（供其他组件监听）

---

## 第三部分：修改现有 3 个文件

### 修改 1：ZoneContextAutoConfiguration.java

当前代码从 Nacos metadata 读取 zone：

```java
String zone = System.getProperty(ZoneConstants.CURRENT_ZONE_PROPERTY_NAME,
    environment.getProperty("spring.cloud.nacos.discovery.metadata.zone", "defaultZone"));
```

改为通过 CompositeZoneLocator 定位，兜底 Nacos：

```java
@Autowired(required = false)
private List<ZoneLocator> zoneLocators;

@Bean
public CompositeZoneLocator compositeZoneLocator() {
    if (zoneLocators == null || zoneLocators.isEmpty()) {
        // 没有自定义定位器，使用默认
        return new CompositeZoneLocator(Collections.singletonList(new DefaultZoneLocator()));
    }
    return new CompositeZoneLocator(zoneLocators);
}

@Bean
public ZoneContext zoneContext(ZoneProperties zoneProperties,
                                CompositeZoneLocator zoneLocator,
                                Environment environment) {
    ZoneContext zoneContext = ZoneContext.get();
    // ... 设置 enabled/preference/thresholds ...

    // 区域来源：ZoneLocator 优先，兜底 Nacos
    String zone = zoneLocator.locate(environment);
    if (zone == null || ZoneConstants.DEFAULT_ZONE.equalsIgnoreCase(zone)) {
        zone = environment.getProperty(
            "spring.cloud.nacos.discovery.metadata.zone", "defaultZone");
    }
    zoneContext.setZone(zone);
    return zoneContext;
}
```

### 修改 2：ZoneConstants.java

不需要修改。常量中已经定义了 LOCATOR_PROPERTY_NAME_PREFIX、LOCATOR_FAST_FAIL_PROPERTY_NAME、LOCATOR_TIMEOUT_PROPERTY_NAME。

### 修改 3：ZoneProperties.java

增加 Locator 配置：

```java
@NestedConfigurationProperty
private LocatorProperties locator = new LocatorProperties();

@Data
public static class LocatorProperties {
    private boolean fastFail = ZoneConstants.DEFAULT_LOCATOR_FAST_FAIL;
    private int timeout = ZoneConstants.DEFAULT_LOCATOR_TIMEOUT;
}
```

---

## 第四部分：Spring Factories 注册

新增文件 `src/main/resources/META-INF/spring.factories`（如果不存在）：

```properties
# ZoneLocator implementations (auto-discovered by CompositeZoneLocator)
com.myxhs.common.zone.locator.ZoneLocator=\
com.myxhs.common.zone.locator.DefaultZoneLocator

# ZoneContextChangedListener (registered by ZoneContextAutoConfiguration)
com.myxhs.common.zone.listener.ZoneContextChangedListener
```

如果是 Spring Boot 3.x+，使用 `AutoConfiguration.imports` 机制：

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.myxhs.common.zone.ZoneContextAutoConfiguration
```

---

## 第五部分：配置变化

### application.yml 新增配置

```yaml
myxhs:
  availability:
    zone:
      enabled: true
      locator:
        fast-fail: false
        timeout: 3000
    preference:
      enabled: true
      upstream:
        zone-ready-percentage: 80
        same-zone-min-available: 5
```

### Nacos 配置（用于动态切换）

data-id: myxhs-zone.yml (group: DEFAULT_GROUP, refresh: true)

```yaml
# 正常状态
myxhs:
  availability:
    zone: beijing-1
    preference:
      upstream:
        disabled-zone:

# 故障切换
myxhs:
  availability:
    zone: shanghai-1
    preference:
      upstream:
        disabled-zone: beijing-1

# 恢复自动检测
myxhs:
  availability:
    zone: originalZone
    preference:
      upstream:
        disabled-zone:
```

### K8s 部署配置

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: my-xhs-service
          env:
            - name: MYXHS_ZONE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.labels['topology.kubernetes.io/zone']
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
```

---

## 第六部分：和现有 DynamicDataSource 的衔接

my-xhs 的 DynamicDataSource 已经在 `afterPropertiesSet()` 中注册了 PropertyChangeListener：

```java
@Override
public void afterPropertiesSet() {
    ZoneContext.get().addPropertyChangeListener(zoneChangeListener);
}
```

新增 ZoneContextChangedListener 触发 `ZoneContext.setZone("shanghai-1")` -> PropertyChangeSupport fires `zone` -> DynamicDataSource.onZoneChanged() -> switchDataSource()。DynamicDataSource 不需要任何修改。

---

## 第七部分：和现有 Redis 拦截器的衔接

my-xhs 有完整的 Redis 拦截器链（7 个文件），但不依赖 ZoneContext。区域切换不影响 Redis 拦截器的正常工作。

如果未来需要对 Redis 做区域感知（如区域切换后 RedisKey 前缀变化），可以在 Redis 拦截器中注入 ZoneContext：

```java
public class MyRedisInterceptor implements RedisCommandInterceptor {
    @Autowired
    private ZoneContext zoneContext;

    @Override
    public void beforeExecute(RedisMethodContext<RedisCommands> context) {
        String zone = zoneContext.getZone();
        // 根据 zone 决定 RedisKey 前缀
    }
}
```

当前不需要这个功能，Redis 拦截器保持现状即可。

---

## 第八部分：测试策略

### 单元测试

| 测试类 | 测试内容 |
|--------|---------|
| `DefaultZoneLocatorTest` | 环境变量优先于 application.yml；默认返回 "defaultZone" |
| `CompositeZoneLocatorTest` | 按 order 排序；第一个非 null 结果生效；fast-fail 行为；缓存行为 |
| `ZoneContextChangedListenerTest` | EnvironmentChangeEvent 触发属性更新；originalZone 触发重新定位；无关事件不处理 |

### 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
public class ZoneSwitchIntegrationTest {

    @Autowired
    private ZoneContext zoneContext;

    @Autowired
    private DynamicDataSource dynamicDataSource;

    @Test
    public void testZoneSwitch() {
        // 设置初始区域
        zoneContext.setZone("beijing-1");
        assertThat(dynamicDataSource.getCurrentZone()).isEqualTo("beijing-1");

        // 模拟配置变更
        // 通过 EnvironmentTestUtils 修改 Environment
        // 发布 EnvironmentChangeEvent
        EnvironmentChangeEvent event = new EnvironmentChangeEvent(
            Set.of("myxhs.availability.zone"));
        applicationContext.publishEvent(event);

        // 验证区域已切换
        assertThat(zoneContext.getZone()).isEqualTo("shanghai-1");
        assertThat(dynamicDataSource.getCurrentZone()).isEqualTo("shanghai-1");
    }
}
```

### 部署验证

```
验证 1：部署在 K8s 中，确认 ZoneLocator 读取 MYXHS_ZONE 环境变量
  检查：启动日志中 "Zone located from env MYXHS_ZONE" 是否输出

验证 2：Nacos 配置中心改 zone
  检查：ZoneContext 日志 "zone property changed"
  检查：DynamicDataSource 是否切换 DataSource

验证 3：same-zone-min-available 触发
  检查：同区域实例 < 5 时，ZonePreferenceFilter 日志输出 "Not enough same zone entities"
```

---

## 第九部分：实施计划

### 阶段一：核心能力（预计 2 天）

| 任务 | 文件 | 开发者 |
|------|------|--------|
| 1.1 ZoneLocator SPI + AbstractZoneLocator | 2 个新文件 | 1人 |
| 1.2 CompositeZoneLocator | 1 个新文件 | 1人 |
| 1.3 DefaultZoneLocator（环境变量 + 配置） | 1 个新文件 | 1人 |
| 1.4 修改 ZoneContextAutoConfiguration | 修改 1 个文件 | 1人 |
| 1.5 单元测试 | 3 个测试文件 | 1人 |

### 阶段二：动态切换（预计 1 天）

| 任务 | 文件 | 开发者 |
|------|------|--------|
| 2.1 ZoneContextChangedEvent | 1 个新文件 | 1人 |
| 2.2 ZoneContextChangedListener | 1 个新文件 | 1人 |
| 2.3 Spring Factories 注册 | 修改 1 个文件 | 1人 |
| 2.4 集成测试 | 扩展现有 ZoneContextTest | 1人 |

### 阶段三：可选增强（预计 1 天）

| 任务 | 文件 | 备注 |
|------|------|------|
| 3.1 AWS 定位器（3 个文件） | 复制 + 微调 | 仅 AWS 环境需要 |
| 3.2 演练脚本 | 新增文档 | 每季度执行 |
| 3.3 监控面板 | Grafana JSON | 导出配置 |

### 文件汇总

| 操作 | 数量 | 文件 |
|------|------|------|
| 新增文件 | 7 | ZoneLocator, AbstractZoneLocator, CompositeZoneLocator, DefaultZoneLocator, ZoneContextChangedEvent, ZoneContextChangedListener, spring.factories |
| 修改文件 | 1 | ZoneContextAutoConfiguration |
| 不修改 | 14 | ZoneContext, ZoneConstants, ZoneProperties, ZonePrefenceFilter, ZoneResolver, DynamicDataSource, LoadBalancer 3个, Redis 7个 |
| 总计 | 22 | 完成后的 zone 包 |

---

## 第十部分：和简历的逐条对应

| 简历 | 实现 | 涉及文件 | 状态 |
|------|------|---------|------|
| "统一 Availability Zone 定位抽象" | ZoneLocator SPI + 4 个实现 | ZoneLocator, AbstractZoneLocator, CompositeZoneLocator, DefaultZoneLocator | ⏳ 新增 |
| "无论 AWS EC2/ECS 还是本地" | DefaultZoneLocator（环境变量）+ AWS 定位器 | DefaultZoneLocator + 3 个 AWS 文件 | ⏳ 新增（AWS 可选） |
| "服务调用同区域访问" | ZonePreferenceFilter + ZonePreferenceServiceInstanceListSupplier | 已有文件 | ✅ |
| "数据存储同区域访问" | DynamicDataSource + ZoneContextChangedEvent | DynamicDataSource（已有）+ ZoneContextChangedEvent | ✅ |
| "内建保护策略，防止单区域负载过高" | same-zone-min-available + zone-ready-percentage | ZonePreferenceFilter（已有） | ✅ |
| "配置中心秒级切换到非故障区域" | ZoneContextChangedListener | ZoneContextChangedListener（新增）| ⏳ |
| "通过回滚配置，秒级恢复到同区域优先" | originalZone 回退机制 | ZoneContextChangedListener（新增）| ⏳ |
| "节点分布在北美、中国大陆、欧洲" | ZoneLocator 自动检测 + K8s Downward API | DefaultZoneLocator + K8s 配置 | ⏳ |
| "通过响应时间监控对比前后，整体减少 10-30%" | 区域偏好命中率监控 | 运维配置 | ⏳ |
