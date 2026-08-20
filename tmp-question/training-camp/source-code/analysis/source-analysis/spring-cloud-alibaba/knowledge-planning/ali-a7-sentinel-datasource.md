# ALI-A7 Sentinel 数据源 — 知识规划 (KP)

> 🟡 B | 模块: sentinel-datasource (18) + starter custom (215) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 单源校验 | SentinelDataSourceHandler:82-88 | validFields.size()!=1 → 放弃 |
| 2 | 有效字段探测 | DataSourcePropertiesConfiguration:128-146 | 反射扫描六字段 |
| 3 | 初始化后装配 | SentinelDataSourceHandler:77-101 | SmartInitializingSingleton + preCheck |
| 4 | 动态 Bean 注册 | SentinelDataSourceHandler:201-213 | registerBeanDefinition + getBean 即初始化 |
| 5 | 转换器三分支 | SentinelDataSourceHandler:134-188 | custom 动态注册 / json-xml 内置引用 |
| 6 | 规则落位 | AbstractDataSourceProperties:100-108 | 七 RuleManager.register2Property |
| 7 | 规则枚举 | RuleType:42-67 | 七类型 + 对应 Rule 类 |
| 8 | 六数据源族 | config/ 六 Properties + factorybean/ | 配置外壳 + 工厂内核 |
| 9 | preCheck 钩子 | AbstractDataSourceProperties:96-98 | 子类自检扩展点 |
| 10 | 错误即指南 | SentinelDataSourceHandler:175-179 | 异常信息含支持类型 |

## 02 高频坑

1. 多数据源同配 = 整个放弃 (不是选一个)
2. custom dataType 必须配 converter-class, 否则抛
3. ruleType 缺省时 postRegister 无效 (无 switch 匹配)
4. 数据源名格式 `{name}-sentinel-{type}-datasource`
5. 内置 converter Bean 名 `sentinel-{json|xml}-{ruleType}-converter`
6. 六种数据源 (含 Consul), 七种规则类型 (含 GW_API_GROUP)

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 反射 | getValidField / parseBeanDefinition 字段 / getValidDataSourceProperties |
| 生命周期 | SmartInitializingSingleton / registerBeanDefinition / getBean 即初始化 |
| 转换 | json/xml 内置 / custom 动态注册 / Class.forName |
| 规则 | 七 RuleManager / register2Property 属性监听 |
| 工厂 | 六 FactoryBean / 配置外壳 |
| 错误 | 多源 log.error / 缺 converter 抛 / 不支持类型提示 |

## 04 跨域桥接

- → Sentinel 5.9: RuleManager/register2Property 内核
- ← ALI-A6: 规则消费者 (FlowRule 等)
- → Nacos 5.8: NacosDataSource 是 Nacos 客户端消费方
- → 面试: "Sentinel 规则怎么动态加载" — 六源 + 单源校验 + 七分派
