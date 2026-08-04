# 18-09 配置系统源码解析

## 目录

- [配置的输入与输出](#配置的输入与输出)
- [JSON → POJO 反序列化](#json--pojo-反序列化)
- [ConfigPostProcessor 链如何处理 POJO](#configpostprocessor-链如何处理-pojo)
- [ConfigConfigurationPropertiesSynthesizer 链如何将 POJO 转为 Spring 属性](#configconfigurationpropertiessynthesizer-链如何将-pojo-转为-spring-属性)
- [属性别名系统](#属性别名系统)
- [JdbcURLAssembler 的 URL 标准化](#jdbcurlassembler-的-url-标准化)
- [META-INF/dynamic-jdbc/default.properties 详解](#meta-infdynamic-jdbcdefaultproperties-详解)
- [完整配置链路示例](#完整配置链路示例)

---

## 配置的输入与输出

整个配置系统的输入和输出：

```
输入（用户配置）：
  microsphere.dynamic.jdbc.configs.xxx=JSON 字符串
  
  或者：
  microsphere.dynamic.jdbc.configs.xxx=classpath:config/xxx.json

输出（Spring Environment 中的属性）：
  spring.datasource.url=...
  spring.datasource.username=...
  spring.datasource.hikari.maximumPoolSize=20
  spring.shardingsphere.datasource.names=...
  mybatis.base-packages=...
  mybatis-plus.base-packages=...
  spring.autoconfigure.exclude=...
```

中间的转换链路：

```java
文本配置 (JSON 字符串)
  │
  ▼
DynamicJdbcConfigUtils.getDynamicJdbcConfigs()
  │  → Jackson ObjectMapper.readValue()
  ▼
DynamicJdbcConfig (POJO)
  │
  ▼
DataSourcePropertiesConfigPostProcessor.postProcess()
  │  → 填充默认值（"前一个填充"行为）
  ▼
DynamicJdbcConfig (已填充的 POJO)
  │
  ▼
ConfigConfigurationPropertiesSynthesizer.synthesize()
  │  → DataSourceConfigurationPropertiesSynthesizer
  │  → ShardingSphereConfigConfigurationPropertiesSynthesizer
  │  → MybatisConfigConfigurationPropertiesSynthesizer 等
  ▼
Map<String, Object> (扁平属性)
  │
  ▼
MapPropertySource (合成 PropertySource)
  │
  ▼
ConfigurableEnvironment.getPropertySources().addAfter()
  ▼
Spring Environment（可供 Auto-Configuration 绑定）
```

---

## JSON → POJO 反序列化

### ObjectMapper 配置

```java
// DynamicJdbcConfigUtils 中的 Jackson ObjectMapper
private static final ObjectMapper objectMapper = new ObjectMapper();

static {
    objectMapper.registerModule(new JavaTimeModule());
}
```

### 字段映射

```java
// DynamicJdbcConfig.java 中的 @JsonProperty 映射
public class DynamicJdbcConfig implements BeanFactoryAware {

    // 普通名称 → 直接映射
    private @Nullable String name;

    // 布尔值 → 默认 true
    private boolean dynamic = true;
    private boolean primary = false;

    // 模块字段 → JSON key 映射
    @JsonProperty(DynamicJdbcConstants.DATASOURCE_MODULE)              // "datasource"
    private @Nullable List<Map<String, Object>> dataSource;

    @JsonIgnore  // 不参与序列化
    private List<Map<String, String>> dataSourcePropertiesList;

    @JsonProperty(DynamicJdbcConstants.HIGH_AVAILABILITY_DATASOURCE_MODULE)  // "ha-datasource"
    private @Nullable Map<String, List<Map<String, Object>>> highAvailabilityDataSource;

    @JsonIgnore
    private Map<String, List<Map<String, String>>> highAvailabilityDataSourcePropertiesMap;

    @JsonProperty(DynamicJdbcConstants.TRANSACTION_MODULE)              // "transaction"
    private @Nullable Transaction transaction;

    @JsonProperty(DynamicJdbcConstants.SHARDING_SPHERE_MODULE)          // "sharding-sphere"
    private @Nullable ShardingSphere shardingSphere;

    @JsonProperty(DynamicJdbcConstants.MYBATIS_MODULE)                  // "mybatis"
    private @Nullable Mybatis mybatis;

    @JsonProperty(DynamicJdbcConstants.MYBATIS_PLUS_MODULE)              // "mybatis-plus"
    private @Nullable MybatisPlus mybatisPlus;

    @JsonIgnore
    private BeanFactory beanFactory;

    @JsonIgnore
    private ZoneContext zoneContext;
}
```

### setter 处理

关键 setter 在反序列化时会额外处理：

```java
// 设置普通数据源时，同时生成展平的 dataSourcePropertiesList
public void setDataSource(@Nullable List<Map<String, Object>> dataSource) {
    this.dataSource = dataSource;
    this.dataSourcePropertiesList = DynamicJdbcUtils.flatPropertiesList(dataSource);
}

// 设置 HA 数据源时，同时生成展平的 dataSourcePropertiesMap
public void setHighAvailabilityDataSource(
        @Nullable Map<String, List<Map<String, Object>>> highAvailabilityDataSource) {
    this.highAvailabilityDataSource = highAvailabilityDataSource;
    this.highAvailabilityDataSourcePropertiesMap =
            DynamicJdbcUtils.flatPropertiesMap(highAvailabilityDataSource);
}
```

`DynamicJdbcUtils.flatPropertiesList` 将 `List<Map<String, Object>>` 转为 `List<Map<String, String>>`：

```java
public static List<Map<String, String>> flatPropertiesList(List<Map<String, Object>> sourceList) {
    if (sourceList == null) return null;
    List<Map<String, String>> result = new ArrayList<>(sourceList.size());
    for (Map<String, Object> source : sourceList) {
        Map<String, String> flat = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            flat.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
        }
        result.add(flat);
    }
    return result;
}
```

**为什么需要展平？**

JSON 中的值可能是任意类型（String、Integer、Boolean），但 DataSource 的配置属性最终都是字符串形式（`spring.datasource.url=jdbc:mysql://...`）。在 PostProcessor 阶段提前展平，避免后续处理时反复做类型转换。

### 配置读取流程

```java
public static Map<String, DynamicJdbcConfig> getDynamicJdbcConfigs(
        ConfigurableEnvironment environment) {

    String prefix = DYNAMIC_JDBC_CONFIGS_PROPERTY_NAME_PREFIX;
    // → "microsphere.dynamic.jdbc.configs"

    // 扫描 Environment 中所有以该前缀开头的属性名
    Set<String> propertyNames = findPropertyNamesByPrefix(environment, prefix);

    // 对每个属性名，读取 JSON 值并解析
    Map<String, DynamicJdbcConfig> configs = new HashMap<>(propertyNames.size());
    for (String propertyName : propertyNames) {
        String value = environment.getProperty(propertyName);
        if (StringUtils.isEmpty(value)) continue;

        DynamicJdbcConfig config = getDynamicJdbcConfig(environment, propertyName);
        configs.put(propertyName, config);
    }

    return unmodifiableMap(configs);
}

public static DynamicJdbcConfig getDynamicJdbcConfig(
        ConfigurableEnvironment environment, String propertyName) {

    // 读取配置内容（支持 inline JSON 和 classpath: 两种格式）
    String jsonContent = getDynamicJdbcConfigContent(environment, propertyName);

    // 解析 JSON
    DynamicJdbcConfig config = parseDynamicJdbcConfig(propertyName, jsonContent);

    // 强制开启 dynamic
    if (!config.isDynamic()) {
        config.setDynamic(Boolean.TRUE);
    }

    // 设置默认 name（如果未设）
    if (config.getName() == null) {
        config.setName(getDynamicJdbcConfigPropertyNameSuffix(propertyName));
    }

    return config;
}
```

### 支持两种 JSON 来源

```java
private static String getDynamicJdbcConfigContent(String value) {
    if (value == null) return null;

    // 方式 1: classpath 资源
    if (value.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
        return readResourceContent(value);
    }

    // 方式 2: inline JSON 字符串
    return value;
}
```

### 深克隆

```java
public static DynamicJdbcConfig cloneDynamicJdbcConfig(DynamicJdbcConfig source) {
    // 序列化为 JSON
    String json = objectMapper.writeValueAsString(source);
    // 反序列化为新对象
    return parseDynamicJdbcConfig(null, json);
}
```

深克隆通过 JSON 序列化+反序列化实现。`@JsonIgnore` 标注的字段（`dataSourcePropertiesList`、`highAvailabilityDataSourcePropertiesMap`、`beanFactory`、`zoneContext`）在序列化时被忽略，反序列化后为空——因为 null 值会通过 setter 触发展平重新计算。

---

## ConfigPostProcessor 链如何处理 POJO

### DynamicJdbcConfigPostProcessor

```java
public class DynamicJdbcConfigPostProcessor extends AbstractConfigPostProcessor {

    @Override
    public void postProcess(DynamicJdbcConfig config, String propertyName) {
        processName(config, propertyName);
        invokeAwareInterfaces(config, context);
    }

    private void processName(DynamicJdbcConfig config, String propertyName) {
        String name = config.getName();
        if (name == null) {
            // 用 propertyName 的后缀作为 name
            String suffix = DynamicJdbcConfigUtils
                    .getDynamicJdbcConfigPropertyNameSuffix(propertyName);
            config.setName(suffix);
        }
    }
}
```

`invokeAwareInterfaces` 来自 `io.microsphere.spring.util.BeanUtils`，它会检查对象是否实现了 `BeanFactoryAware`、`ApplicationContextAware`、`EnvironmentAware` 等接口并调用对应的方法：

```java
public static void invokeAwareInterfaces(Object object, ConfigurableApplicationContext context) {
    if (object instanceof BeanFactoryAware) {
        ((BeanFactoryAware) object).setBeanFactory(context.getBeanFactory());
    }
    if (object instanceof ApplicationContextAware) {
        ((ApplicationContextAware) object).setApplicationContext(context);
    }
    if (object instanceof EnvironmentAware) {
        ((EnvironmentAware) object).setEnvironment(context.getEnvironment());
    }
    // ...
}
```

`DynamicJdbcConfig` 实现了 `BeanFactoryAware`，所以会通过这个路径设置 `beanFactory` 字段。

### DataSourcePropertiesConfigPostProcessor（核心逻辑详见 18-08 的 SPI 对比表）

```java
public class DataSourcePropertiesConfigPostProcessor extends AbstractConfigPostProcessor
        implements InitializingBean, BeanClassLoaderAware, EnvironmentAware {

    private JdbcURLAssembler jdbcURLAssembler;
    private Map<String, String> defaultDataSourceProperties;

    @Override
    public void afterPropertiesSet() {
        this.defaultDataSourceProperties = initDefaultDataSourceProperties();
        // → {"type": "com.zaxxer.hikari.HikariDataSource"}
    }

    @Override
    public void postProcess(DynamicJdbcConfig config, String propertyName) {
        List<Map<String, String>> propsList = config.getDataSourcePropertiesList();
        int size = propsList == null ? 0 : propsList.size();
        if (size < 1) return;

        for (int i = 0; i < size; i++) {
            Map<String, String> props = propsList.get(i);
            Map<String, String> previous = getPreviousDataSourceProperties(propsList, i);
            processDataSourceProperties(props, previous, config, propertyName, i);
        }
    }

    private Map<String, String> getPreviousDataSourceProperties(
            List<Map<String, String>> list, int index) {
        // index=0 时使用默认值（只有 type=Hikari）
        // index>0 时使用前一个 DataSource 的完整配置
        return index < 1
                ? defaultDataSourceProperties
                : new HashMap<>(list.get(index - 1));
    }

    private void processDataSourceProperties(Map<String, String> props,
            Map<String, String> previous, ...) {
        processDataSourceName(props, previous, propertyName, index);
        processDataSourceType(props, previous);
        String jdbcURL = processDataSourceUrl(props);        // → 调用 JdbcURLAssembler
        processDataSourceDriverClassName(props, previous, jdbcURL);
        processDataSourceUserName(props, previous);
        processDataSourcePassword(props, previous);
        processDataSourceOthers(props, previous);            // → 剩余属性全部复制
    }
}
```

### 其他 4 个 ConfigPostProcessor

```java
public class TransactionConfigPostProcessor extends AbstractConfigurationConfigPostProcessor<DynamicJdbcConfig.Transaction> {
    // 空
}

public class ShardingSphereConfigPostProcessor extends AbstractConfigurationConfigPostProcessor<DynamicJdbcConfig.ShardingSphere> {
    // 空
}

public class MybatisConfigPostProcessor extends AbstractConfigurationConfigPostProcessor<DynamicJdbcConfig.Mybatis> {
    // 空
}

public class MybatisPlusConfigPostProcessor extends AbstractConfigurationConfigPostProcessor<DynamicJdbcConfig.MybatisPlus> {
    // 空
}
```

它们继承 `AbstractConfigurationConfigPostProcessor<C>`。这个父类通过 `ConfigurationCapable<C>` 接口反射获取 `DynamicJdbcConfig` 中对应泛型类型的 getter，然后调用模板方法 `postProcess(config, propertyName, module, configuration)`：

```java
// AbstractConfigurationConfigPostProcessor 的模板方法
@Override
public void postProcess(DynamicJdbcConfig config, String propertyName) {
    C configuration = getConfiguration(config);  // 反射调用对应的 getter
    String module = getModule();                 // 从 @Module 注解获取
    if (configuration != null) {
        postProcess(config, propertyName, module, configuration);
    }
}
```

但 4 个空实现什么都没做。

---

## ConfigConfigurationPropertiesSynthesizer 链如何将 POJO 转为 Spring 属性

### AbstractModuleConfigConfigurationPropertiesSynthesizer

```java
public abstract class AbstractModuleConfigConfigurationPropertiesSynthesizer
        extends AbstractConfigConfigurationPropertiesSynthesizer
        implements ModuleCapable {

    @Override
    public void synthesize(DynamicJdbcConfig config, Map<String, Object> properties) {
        String module = getModule();

        if (!supports(config, module, properties)) {
            return;  // 不支持的合成场景，跳过
        }

        // 模板方法：子类实现具体的合成逻辑
        synthesize(config, module, properties);

        // 通用逻辑：合成模块属性（扁平化 + 别名解析）
        synthesizeModuleProperties(config, module, properties);

        // 通用逻辑：合成排除配置
        synthesizeModuleExclusionAutoConfigurationProperty(module, properties);
    }

    protected boolean supports(DynamicJdbcConfig config, String module,
            Map<String, Object> properties) {
        return true;  // 默认全部支持，子类可重写
    }

    protected abstract void synthesize(DynamicJdbcConfig config, String module,
            Map<String, Object> properties);
}
```

### DataSourceConfigurationPropertiesSynthesizer

```java
public class DataSourceConfigurationPropertiesSynthesizer
        extends AbstractModuleConfigConfigurationPropertiesSynthesizer {

    @Override
    protected boolean supports(DynamicJdbcConfig config, String module,
            Map<String, Object> properties) {
        // 有 ShardingSphere 时不合成 DataSource 属性
        if (config.hasShardingDataSource()) {
            return false;
        }
        // 只合成单数据源场景
        return config.hasOnlySingleDataSource();
    }

    @Override
    protected void synthesize(DynamicJdbcConfig config, String module,
            Map<String, Object> properties) {
        List<Map<String, String>> propsList = config.getDataSourcePropertiesList();
        Map<String, String> dsProps = propsList.get(0);  // 取第一个

        // 1. Spring Boot DataSourceProperties 绑定
        // → spring.datasource.url, spring.datasource.username, ...
        synthesizeConfigurationProperties(module, DataSourceProperties.class,
                dsProps, properties);

        // 2. 连接池专属属性
        // → spring.datasource.hikari.* / spring.datasource.tomcat.*
        synthesizeDataSourceProperties(module, dsProps, properties);
    }

    private void synthesizeDataSourceProperties(String module,
            Map<String, String> dsProps, Map<String, Object> properties) {
        String type = getDataSourceType(dsProps);
        String prefix = dataSourcePropertiesPropertyNamePrefixes.get(type);
        // "com.zaxxer.hikari.HikariDataSource" → "spring.datasource.hikari"
        // "org.apache.tomcat.jdbc.pool.DataSource" → "spring.datasource.tomcat"
        // "org.apache.commons.dbcp2.BasicDataSource" → "spring.datasource.dbcp2"

        if (prefix != null) {
            dsProps.forEach((name, value) -> {
                if (!isDataSourcePropertiesPropertyName(name)) {
                    properties.put(prefix + "." + name, value);
                }
            });
        }
    }
}
```

### synthesizeConfigurationProperties

```java
// AbstractConfigConfigurationPropertiesSynthesizer
protected void synthesizeConfigurationProperties(String module,
        Class<?> configClass, Map<String, String> sourceProps,
        Map<String, Object> targetProps) {

    // 获取 @ConfigurationProperties 注解中的前缀
    String prefix = resolvePropertyNamePrefix(configClass);

    if (prefix != null) {
        // 将 source 中的属性按 configClass 的属性描述写入 target
        BeanWrapperImpl wrapper = new BeanWrapperImpl(configClass);
        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String propName = pd.getName();
            String value = sourceProps.get(propName);
            if (value != null) {
                targetProps.put(prefix + "." + propName, value);
            }
        }
    }
}
```

### ShardingSphereConfigConfigurationPropertiesSynthesizer

```java
public class ShardingSphereConfigConfigurationPropertiesSynthesizer
        extends AbstractConfigurationConfigConfigurationPropertiesSynthesizer<DynamicJdbcConfig.ShardingSphere> {

    @Override
    protected void synthesize(DynamicJdbcConfig config, String module,
            DynamicJdbcConfig.ShardingSphere ssConfig,
            Class<?> configPropsClass, Map<String, Object> properties) {

        // 1. 合成 DataSource 属性
        // → spring.shardingsphere.datasource.names = ds_0,ds_1
        // → spring.shardingsphere.datasource.ds_0.jdbcUrl = ...
        synthesizeDataSourceProperties(config, properties);

        // 2. 从 YAML 提取 props
        // → spring.shardingsphere.props.sql-show = false
        synthesizeYamlProps(ssConfig, properties);
    }

    private void synthesizeDataSourceProperties(DynamicJdbcConfig config,
            Map<String, Object> properties) {
        Map<String, Map<String, String>> dsMap = config.getDataSourcePropertiesMap();

        String names = String.join(",", dsMap.keySet());
        properties.put("spring.shardingsphere.datasource.names", names);

        for (Map.Entry<String, Map<String, String>> entry : dsMap.entrySet()) {
            String dsName = entry.getKey();
            for (Map.Entry<String, String> prop : entry.getValue().entrySet()) {
                String key = "spring.shardingsphere.datasource." + dsName + "." + prop.getKey();
                properties.put(key, prop.getValue());
            }
        }
    }

    private void synthesizeYamlProps(DynamicJdbcConfig.ShardingSphere config,
            Map<String, Object> properties) {
        String resource = config.getConfigResource();
        YamlRootConfiguration yamlConfig = DynamicJdbcUtils
                .loadShardingSphereYamlRootConfiguration(resource);

        if (yamlConfig != null && yamlConfig.getProps() != null) {
            yamlConfig.getProps().forEach((name, value) -> {
                properties.put("spring.shardingsphere.props." + name, value);
            });
        }
    }
}
```

### 模块属性合成（synthesizeModuleProperties）

```java
// AbstractModuleConfigConfigurationPropertiesSynthesizer
protected void synthesizeModuleProperties(DynamicJdbcConfig config,
        String module, Map<String, Object> properties) {

    // 从 Environment 获取该模块的配置属性
    Map<String, Object> moduleProps = DynamicJdbcPropertyUtils
            .getModuleProperties(getEnvironment(), module);

    // 展平嵌套的 Map
    Map<String, String> flattenProps = ConfigurationPropertiesFlatter
            .getInstance().flat(moduleProps);

    // 处理属性别名
    for (Map.Entry<String, String> entry : flattenProps.entrySet()) {
        String propertyName = entry.getKey();
        String propertyValue = entry.getValue();

        // 查找别名
        Set<String> aliases = DynamicJdbcPropertyUtils
                .getModulePropertyNameAliases(environment, module, propertyName);

        // 遍历原始属性和别名，由子类决定是否合成
        if (filterModuleProperty(module, propertyName, propertyName, propertyValue)) {
            properties.put(propertyName, propertyValue);
        }
        for (String alias : aliases) {
            if (filterModuleProperty(module, propertyName, alias, propertyValue)) {
                properties.put(alias, propertyValue);
            }
        }
    }
}

// DataSourceConfigurationPropertiesSynthesizer 中重写
@Override
protected boolean filterModuleProperty(String module, String sourcePropertyName,
        String synthesizePropertyName, String propertyValue) {
    // 只合成 DataSourceProperties 中的标准属性
    return isDataSourcePropertiesPropertyName(synthesizePropertyName);
}
```

---

## 属性别名系统

DataSource 模块的 URL 属性支持三个别名：

```properties
# default.properties
microsphere.dynamic.jdbc.modules.datasource.property-name-aliases.url=jdbcUrl,jdbc-url
microsphere.dynamic.jdbc.modules.datasource.property-name-aliases.jdbcUrl=url,jdbc-url
microsphere.dynamic.jdbc.modules.datasource.property-name-aliases.jdbc-url=url,jdbcUrl
```

用户配置以下任意一个都可以：

```json
{"url": "jdbc:mysql://host/db"}
{"jdbcUrl": "jdbc:mysql://host/db"}
{"jdbc-url": "jdbc:mysql://host/db"}
```

别名系统的实现：

```java
// DynamicJdbcPropertyUtils
public static Set<String> getModulePropertyNameAliases(
        Environment environment, String module, String propertyName) {
    String key = getModulePropertyNameAliasesPropertyName(module, propertyName);
    return environment.getProperty(key, Set.class, emptySet());
}
```

别名在 `AbstractModuleConfigConfigurationPropertiesSynthesizer.synthesizeModuleProperties()` 中处理：

```java
for (Map.Entry<String, String> entry : flattenProps.entrySet()) {
    String propertyName = entry.getKey();

    // 获取该属性的所有别名
    Set<String> aliases = getModulePropertyNameAliases(environment, module, propertyName);

    // 原始属性名也写入
    if (filterModuleProperty(module, propertyName, propertyName, propertyValue)) {
        properties.put(propertyName, propertyValue);
    }

    // 别名也写入
    for (String alias : aliases) {
        if (filterModuleProperty(module, propertyName, alias, propertyValue)) {
            properties.put(alias, propertyValue);
        }
    }
}
```

---

## JdbcURLAssembler 的 URL 标准化

```java
public class JdbcURLAssembler {

    private static final String JDBC_URL_PREFIX = "jdbc:";
    private static final String PROTOCOL_SEPARATOR = "//";

    private final ConfigurableEnvironment environment;
    private final String defaultScheme;       // 默认 "mysql//"
    private final Map<String, String> defaultQueryParams;
    // {"characterEncoding": "utf-8", "useSSL": "false", "useUnicode": "true"}

    public String assemble(String rawJdbcURL) {
        if (StringUtils.isBlank(rawJdbcURL)) {
            return rawJdbcURL;
        }

        // 1. 规范化
        String url = normalize(rawJdbcURL);
        // 2. 解析为 URI
        URI uri = URI.create(url);
        // 3. 解析查询参数
        MultiValueMap<String, String> queryParams = URLUtils.parseQueryParams(uri);
        // 4. 添加默认查询参数（如果未设置）
        setDefaultQueryParamsIfAbsent(queryParams);
        // 5. 重建 URL
        url = JDBC_URL_PREFIX + rebuildURL(uri, queryParams);
        return url;
    }

    private String normalize(String rawJdbcURL) {
        String url = StringUtils.trim(rawJdbcURL);
        String protocol = StringUtils.substringBetween(url, JDBC_URL_PREFIX, PROTOCOL_SEPARATOR);
        // 从 "jdbc:mysql://host/db" 中提取 "mysql"
        if (protocol != null) {
            // 已经有 scheme，去掉 "jdbc:" 前缀
            url = StringUtils.substring(rawJdbcURL, JDBC_URL_PREFIX_LENGTH);
        } else {
            // 没有 scheme，添加默认的
            url = defaultScheme + url;
            // "host/db" → "mysql//host/db"
        }
        return url;
    }

    private void setDefaultQueryParamsIfAbsent(MultiValueMap<String, String> queryParams) {
        defaultQueryParams.forEach((name, value) -> {
            if (!queryParams.containsKey(name)) {
                queryParams.add(name, value);
            }
        });
    }
}
```

---

## META-INF/dynamic-jdbc/default.properties 详解

### 加载方式

```java
// DynamicJdbcDefaultPropertiesPostProcessor
public class DynamicJdbcDefaultPropertiesPostProcessor
        implements DefaultPropertiesPostProcessor {
    @Override
    public void initializeResources(Set<String> defaultPropertiesResources) {
        defaultPropertiesResources.add(DEFAULT_PROPERTIES_LOCATION);
        // → "META-INF/dynamic-jdbc/default.properties"
    }
}
```

`DefaultPropertiesPostProcessor` 是 `microsphere-spring-boot` 的 SPI，在 `ApplicationEnvironmentPreparedEvent` 阶段执行。它扫描所有 `DefaultPropertiesPostProcessor` 实现注册的资源路径，将属性文件加载为默认 PropertySource（最低优先级，但可供后续覆盖）。

### default.properties 的结构

```properties
# 1. ShardingSphere 默认配置
spring.shardingsphere.mode.type = Memory
spring.shardingsphere.rules.sql-parser.sql-comment-parse-enabled = false

# 2. 禁用 baomidou 的 Dynamic DataSource（冲突避免）
spring.datasource.dynamic.enabled = false

# 3. 各模块的 Auto-Configuration base-packages
microsphere.dynamic.jdbc.modules.datasource.auto-configuration.base-packages=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration

microsphere.dynamic.jdbc.modules.ha-datasource.auto-configuration.banned-modules = sharding-sphere

microsphere.dynamic.jdbc.modules.transaction.auto-configuration.base-packages=\
  org.springframework.boot.autoconfigure.transaction.,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration

microsphere.dynamic.jdbc.modules.sharding-sphere.dynamic.enabled = true
microsphere.dynamic.jdbc.modules.sharding-sphere.auto-configuration.base-packages = \
  org.apache.shardingsphere.
microsphere.dynamic.jdbc.modules.sharding-sphere.auto-configuration.banned-modules = datasource

microsphere.dynamic.jdbc.modules.mybatis.auto-configuration.base-packages = org.mybatis.spring.boot.
microsphere.dynamic.jdbc.modules.mybatis.auto-configuration.banned-modules = mybatis-plus

microsphere.dynamic.jdbc.modules.mybatis-plus.auto-configuration.base-packages = com.baomidou.
microsphere.dynamic.jdbc.modules.mybatis-plus.auto-configuration.banned-modules = mybatis

# 4. URL 默认配置
microsphere.dynamic.jdbc.modules.datasource.url.default-scheme = mysql\://
microsphere.dynamic.jdbc.modules.datasource.url.default-query-params.characterEncoding = utf-8
microsphere.dynamic.jdbc.modules.datasource.url.default-query-params.useSSL = false
microsphere.dynamic.jdbc.modules.datasource.url.default-query-params.useUnicode = true

# 5. DataSource 属性别名
microsphere.dynamic.jdbc.modules.datasource.property-name-aliases.url = jdbcUrl,jdbc-url

# 6. ShardingSphere 默认连接池配置
microsphere.dynamic.jdbc.modules.sharding-sphere.default-properties.datasource.maxLifetime = 1800000
microsphere.dynamic.jdbc.modules.sharding-sphere.default-properties.datasource.maxPoolSize = 10

# 7. 多上下文配置
microsphere.dynamic.jdbc.multiple-context.auto-configuration.exclude = \
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,...

microsphere.dynamic.jdbc.multiple-context.bean-classes.expose = \
  javax.sql.DataSource,org.springframework.transaction.PlatformTransactionManager,...

microsphere.dynamic.jdbc.multiple-context.bean-classes.primary = \
  javax.sql.DataSource,org.springframework.transaction.PlatformTransactionManager,...
```

---

## 完整配置链路示例

以一个具体的 JSON 配置为例，跟踪其完整转换链路：

```json
{
  "name": "order-db",
  "datasource": [{
    "name": "master",
    "url": "localhost:3306/orders",
    "username": "root",
    "password": "root",
    "maximumPoolSize": 20
  }],
  "transaction": {
    "name": "order-tx"
  },
  "mybatis": {
    "base-packages": "com.example.orders.mapper"
  }
}
```

**第 1 步：JSON → DynamicJdbcConfig（Jackson）**

```
DynamicJdbcConfig.name = "order-db"
DynamicJdbcConfig.dynamic = true（强制）
DynamicJdbcConfig.dataSource = [{name=master, url=localhost:3306/orders, username=root, password=root, maximumPoolSize=20}]
DynamicJdbcConfig.transaction = {name=order-tx}
DynamicJdbcConfig.mybatis = {base-packages=com.example.orders.mapper}
```

**第 2 步：DataSourcePropertiesConfigPostProcessor 填充**

```
dataSource[0].name = "master"（已有，不变）
dataSource[0].type = "com.zaxxer.hikari.HikariDataSource"（默认值）
dataSource[0].url = "jdbc:mysql://localhost:3306/orders?characterEncoding=utf-8&useSSL=false&useUnicode=true"（JdbcURLAssembler 标准化后）
dataSource[0].driverClassName = "com.mysql.cj.jdbc.Driver"（从 URL 推导）
dataSource[0].username = "root"（已有，不变）
dataSource[0].password = "root"（已有，不变）
dataSource[0].maximumPoolSize = 20（已有，不变）
```

**第 3 步：ConfigValidator 验证**

全部通过。

**第 4 步：注册 DynamicDataSource BeanDefinition**

```
BeanDefinition: DynamicJdbcDynamicDataSource
  Constructor args:
    - DynamicJdbcConfig (深克隆，dynamic=false)
    - "microsphere.dynamic.jdbc.configs.order-db"
    - 当前 ApplicationContext
```

移除 DataSource 配置（clear datasource, clear ha-datasource, set shardingSphere null）。

**第 5 步：ConfigConfigurationPropertiesSynthesizer**

DataSourceConfigurationPropertiesSynthesizer 合成：

```properties
spring.datasource.url = jdbc:mysql://localhost:3306/orders?characterEncoding=utf-8&useSSL=false&useUnicode=true
spring.datasource.type = com.zaxxer.hikari.HikariDataSource
spring.datasource.username = root
spring.datasource.password = root
spring.datasource.driver-class-name = com.mysql.cj.jdbc.Driver
spring.datasource.hikari.maximumPoolSize = 20
```

注：第 2 步填充的 `driverClassName`（camelCase）通过 Spring Boot 的 relaxed binding 映射为 `spring.datasource.driver-class-name`（kebab-case）；`type` 字段同样合成到 `spring.datasource.type`。

MybatisConfigConfigurationPropertiesSynthesizer 合成：

```properties
mybatis.base-packages = com.example.orders.mapper
```

全部合成到 `SynthesizedPropertySource[microsphere.dynamic.jdbc.configs.order-db]` 中。

**第 6 步：ConfigBeanDefinitionRegistrar**

DynamicJdbcConfigBeanDefinitionRegistrar 注册：

```
BeanDefinition: DynamicJdbcConfigBean[order-db] → DynamicJdbcConfig (FactoryBean)
```

TransactionConfigurationConfigBeanDefinitionRegistrar 注册：

```
Bean alias: "order-tx" → PlatformTransactionManager
```

MybatisConfigurationConfigBeanDefinitionRegistrar 注册：

```
MybatisMapperScanConfiguration(base-packages="com.example.orders.mapper")
```

**最终 Environment 中的相关属性**：

```properties
# 来自 default.properties
spring.shardingsphere.mode.type = Memory
spring.datasource.dynamic.enabled = false

# 来自 Synthesizer
spring.datasource.url = jdbc:mysql://localhost:3306/orders?characterEncoding=utf-8&useSSL=false&useUnicode=true
spring.datasource.type = com.zaxxer.hikari.HikariDataSource
spring.datasource.username = root
spring.datasource.password = root
spring.datasource.driver-class-name = com.mysql.cj.jdbc.Driver
spring.datasource.hikari.maximumPoolSize = 20
mybatis.base-packages = com.example.orders.mapper
```
