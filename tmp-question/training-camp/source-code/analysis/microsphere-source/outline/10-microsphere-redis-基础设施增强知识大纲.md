# Microsphere Redis 基础设施增强知识大纲（redis 触发面）

> 来源：`mapping/10-microsphere-redis.md` 6 个 KP 的知识本体提炼
> 性质：**源码侧 outline（L1.5）**——mapping 是文件映射（过程），本大纲是知识本体（结果）
> 组织：按知识维度，非按文件；每个知识点标注来源 KP + 六元元数据 + my-xhs 落地实证
> 用途：①自学/面试知识图谱（Redis 可观测/复制/序列化）②与课程 L1 合并成 L3 的源码侧素材 ③my-xhs 对照（本仓库是 my-xhs zone/redis/ 族的上游）
> 覆盖核对：6/6 KP 全部归属（文末核对表）

---

## 一、命令级拦截与可观测（核心命题）[分布式问题]

> **核心命题**：SDR RedisConnection 代理挂载拦截层——每条命令的生命周期（before/after/error）可插入自定义逻辑，事件化广播。

### 1.1 拦截 SPI + 事件化 [🔴 P1] [时间无关模式]
- **来源**：KP-902 / KP-903
- **机制**：**三回调契约**（RedisCommandInterceptor——beforeExecute/afterExecute/onError）+ **成功才发布**（EventPublishingRedisCommandInterceptor:87——failure 分支不发成功事件——**事件语义正确**（vs 06 仓库 @After finally 缺陷对照））+ **连接级变体**（RedisConnectionInterceptor）+ **AOP 代理挂载**（RedisConnectionFactoryProxyBeanPostProcessor:74——泛型 BPP + 每连接工厂代理）+ **Template 按名替换**（:36）
- **my-xhs**：**已用（同名移植实证）**——zone/redis/ 7 文件（EventPublishingRedisCommandInterceptor/RedisCommandEvent/InterceptingRedisConnectionInvocationHandler/RedisTemplateWrapper/RedisInterceptorAutoConfiguration）

### 1.2 命令元数据 + Doclet 生成 [🟡 P2] [时间无关模式]
- **来源**：KP-905
- **机制**：**命令接口元数据仓库**（SpringRedisMetadataRepository:69——接口/方法/参数类型解析 + **写命令识别** :183）+ **Doclet 编译期生成**（SpringDataRedisMetadataGenerationDoclet:113——javadoc Doclet 扫描命令接口生成元数据——**APT 之外的第二种编译期生成路径**）
- **my-xhs**：该用没用——无读写分类

---

## 二、跨实例逻辑复制 [分布式问题]

### 2.1 Kafka 逻辑复制 [🔴 P1] [时间无关模式]
- **来源**：KP-904
- **机制**：**写操作经 Kafka 重放**（producer/consumer 双开关 :128/:134 + **按域配置复制范围**（domains.redis-templates :184-187——粒度控制）+ RedisConfigurationPropertyChangedEvent 配置变更响应 :55）
- **对比**：物理复制（Redis 主从）vs 应用级逻辑复制（跨集群可控——非事务一致）
- **my-xhs**：该用没用——无跨实例复制（多活 Redis 双写为差距）

---

## 三、序列化家族 [工程问题]

### 3.1 定长序列化 + 专业类型 [🔴 P1] [时间无关模式]
- **来源**：KP-901
- **机制**：**模板基座**（AbstractSerializer:52——serialize/deserialize final + **calcBytesLength 定长自校验** :136——防损坏数据）+ **长度前缀复合编码**（BoundarySerializer:52-62——字段+长度+内容）+ **Redis 专业类型族**（Geo/Point/Range/SortParameters/Weights——命令参数二进制化）+ **SPI 注册中心**（Serializers——02 模式）
- **my-xhs**：该用没用——Jackson JSON 覆盖；定长自校验模式可借鉴

---

## 四、配套与工程面 [工程问题]

### 4.1 @Enable 族/条件/动态连接 [🟢 P3] [时间无关模式]
- **来源**：KP-906
- **机制**：@EnableRedisConfiguration/Context/Interceptor 对称三件 + 条件族 + DynamicRedisConnectionFactory（动态连接）+ registrar 三件
- **拼写错误第 6 例**：RedisComandEventPartitioner（Comand 缺 p）
- **my-xhs**：已用（RedisInterceptorAutoConfiguration 对应物）

---

## 覆盖核对（6/6 + 对照）

| 维度 | 覆盖 KP | 数 |
|------|---------|:--:|
| 一、命令拦截与可观测 | 902,903,905 | 3 |
| 二、跨实例复制 | 904 | 1 |
| 三、序列化家族 | 901 | 1 |
| 四、配套工程 | 906 | 1 |

**去重后唯一 KP**：901-906 全部 = **6/6 ✓**
**无孤儿 KP** ✓
**my-xhs 对照**：已用 3（902/903/906——拦截/包装/自动配置移植）/ 该用没用 3（序列化/复制/元数据）
**历史 REQ 进度**：3/5 证实（REQ-001 拦截/REQ-002 复制/定位修正）；待验证 2（REQ-003 监控/D09 ValueHolder 缓存）
