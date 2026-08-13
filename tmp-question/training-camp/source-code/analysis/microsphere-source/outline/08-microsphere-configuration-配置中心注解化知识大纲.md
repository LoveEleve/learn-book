# Microsphere Configuration 配置中心注解化知识大纲（configuration 触发面）

> 来源：`mapping/08-microsphere-configuration.md` 3 个 KP 的知识本体提炼
> 性质：**源码侧 outline（L1.5）**——mapping 是文件映射（过程），本大纲是知识本体（结果）
> 组织：按知识维度，非按文件；每个知识点标注来源 KP + 六元元数据 + my-xhs 落地实证
> 用途：①自学/面试知识图谱（配置中心注解化集成）②与课程 L1 合并成 L3 的源码侧素材 ③my-xhs 对照
> 覆盖核对：3/3 KP 全部归属（文末核对表）

---

## 一、配置中心注解化三件套模式 [分布式问题]

> **核心命题**：一个配置中心 = 注解 + Attributes + Loader 三件套（03 仓库 @PropertySource 扩展基座的生态应用）——更换配置中心只换注解。

### 1.1 三件套模式（nacos/etcd/zk 同构）[🔴 P1] [时间无关模式]
- **来源**：KP-701（Nacos）/ KP-703（Etcd/ZK）
- **机制**：**注解**（@Target(TYPE) + @Retention(RUNTIME) + @Inherited + **@PropertySourceExtension**（03 仓库元注解 :49）+ **@Import(Loader)** :50——**注解→加载器绑定**）+ **@AliasFor 透传**（name/autoRefreshed——autoRefreshed 默认 true :66）；**Attributes 载体**（extends PropertySourceExtensionAttributes——注解属性解析——etcd: keys/endpoints vs zk: connectString/paths——**连接模型差异封装**）；**Loader**（extends PropertySourceExtensionLoader<A, Attr>——resolveResources 从配置中心拉取 → Resource 数组）
- **生态复用实证**：PropertySourceExtension（03 仓库 KP-3.1 的 10 属性元注解——本仓库是消费者）
- **my-xhs**：该用没用——官方 Starter 覆盖单中心；三件套可借鉴（多中心统一抽象）

### 1.2 两种扩展模式并存（Loader vs Registrar）[🔴 P1] [时间无关模式]
- **来源**：KP-702（Apollo）
- **机制**：**Registrar 模式**（ApolloPropertySource @Import(Registrar) :57——**ImportBeanDefinitionRegistrar + BeanFactoryPostProcessor + ApplicationContextAware 三合一** :71-72——启动早期注入）+ **配置热更新**（onChanged 监听 ConfigChangeEvent :121 + **clonePropertySource 克隆替换** :168——**属性级热更新**（不重建上下文））+ **官方注解透传**（@AliasFor EnableApolloConfig :103）+ 占位符缺省（`${APP_ID:default}` :69）
- **两种扩展风格对照**：Loader 模式（资源加载——nacos/etcd/zk）vs Registrar 模式（Bean 注册+后处理——apollo 需要早注入+热更新监听）
- **热更新粒度对照（跨仓库）**：Apollo PropertySource 克隆替换（属性级）vs dynamic 子上下文重建（Bean 级）
- **my-xhs**：该用没用——未用 Apollo——克隆替换热更新模式可借鉴

---

## 二、配套与工程面 [工程问题]

### 2.1 自研客户端 + 资源清理 [🟡 P2] [时间无关模式]
- **来源**：KP-701
- **机制**：**自研 Nacos OpenAPI 客户端**（OpenApiTemplateClient——pom microsphere-nacos-openapi——不依赖官方 SDK）+ **JVM 关闭钩子清理**（静态缓存 + shutdown hook :52-69）
- **取舍**：自研轻量可控 vs 官方 SDK 兼容性维护成本
- **my-xhs**：不该用——官方 SDK 覆盖

### 2.2 拼写错误先例（第 4/5 例）[🟢 P3] [时间无关模式]
- **来源**：KP-701
- **机制**：`NacosPorpertySource`（Porperty——**注解类名拼错**——测试也拼错——**错误固化**）+ `configCientCache`（Cient）——API 稳定约束先例（StacKTrace/Pattens/FILER 之后）
- **my-xhs**：不该用——无独立知识（API 命名纪律教训）

---

## 覆盖核对（3/3 + 对照）

| 维度 | 覆盖 KP | 数 |
|------|---------|:--:|
| 一、三件套模式 | 701,702,703 | 3 |
| 二、配套工程 | 701（自研客户端/拼写） | — |

**去重后唯一 KP**：701-703 全部 = **3/3 ✓**
**无孤儿 KP** ✓
**my-xhs 对照**：该用没用 2 / 不该用 1——Nacos 官方 Starter 覆盖；**跨仓库知识增量** = 热更新粒度对照（属性级 vs Bean 级）。
