# MP-9 BaseMapper+IService — 17 方法面 + default 直连 + ServiceImpl 链式

> 前置: [[MP-2-metadata]] (TableInfo) | 复用: [[MP-1-injector]] (注入方法面) | 对照: [[M-3-mapper]] (MapperMethod 分发 vs default 直连) | 引出: [[MP-3-lambda-wrapper]] (链式构造器)
> 🟡 Working | 6 KP | [模式: 门面(Service 层)+模板+链式调用]
> Pass 2 闭环: q1(19 抽象方法面) q2(default 直连) q3(selectOne 双态) q4(ServiceImpl 骨架) q5(statementId 复用) q6(批量路径) q7(布尔语义) q8(链式入口)

**读者处境**: 继承 BaseMapper 就能 CRUD, 继承 ServiceImpl 就有 saveBatch/lambdaQuery — 为什么 baseMapper.insert(user) 不走 MapperMethod 分发也能执行?为什么 ServiceImpl 里能拿到 SqlSessionFactory?为什么 saveBatch 一次传 1 万条只发 10 次 SQL?这篇拆 MP 的用户入口层: 方法面、default 直连、Service 封装与链式。

### 1. 方法面 — 19 抽象 CRUD + default 便捷族

场景: BaseMapper 里哪些方法必须实现?哪些自带实现?
源码路径:
- `BaseMapper.java:108-565` — **19 个抽象方法**(q1, 实测穷举 L108-423): insert/deleteById/delete/updateById/update/selectById/selectBatchIds×2/selectCount/selectList×4/selectMaps×4/selectObjs×2 — **方法名与 MP-1 注入的 SqlMethod 一一对应**(statementId=接口名.方法名)
- `Mapper.java:23-25` — Mapper<T> 空标记接口(BaseMapper 继承它; isSupperMapperChildren 判断依据, MP-1)
- `BaseMapper.java:115-138` — **default 方法族**(q2): deleteById(Serializable)/deleteByIds/deleteByMap/selectOne/selectByMap/exists/insertBatch/updateBatchById/insertOrUpdate/selectPage/selectMapsPage — 自带实现
关键设计: 双轨方法面(q1): 抽象方法 = MP-1 注入的 MappedStatement(调用走 M-3 MapperMethod 分发); default 方法 = 自带 Java 实现(便捷封装); 方法面是注入方法名的用户侧镜像 — 一一对应无漂移。[模式: 抽象+默认双轨]
数据流: 抽象方法 → MapperMethod(SqlCommand 查 M-1) → 执行; default 方法 → 自带逻辑(常走 SqlSession 直连)。

### 2. default 直连 — 代理取 SqlSession 绕过 MapperMethod

场景: deleteById(Serializable) 与 deleteById(T) 的执行路径有什么不同?为什么能绕过?
源码路径:
- `BaseMapper.java:136-138` — **MybatisUtils.getMybatisMapperProxy(this)** → `sqlSession.delete(mapperInterface.getName()+DOT+SqlMethod.DELETE_BY_ID.getMethod(), obj)` — 代理里直接取 SqlSession, 构造 statementId 直调(q2)
- `BaseMapper.java:205-216` — deleteByIds: params 组装(entity 空实例+collection, **逻辑删除+填充联动** useFill)→sqlSession.delete(接口名+DELETE_BY_IDS)
- `BaseMapper.java:305-329` — selectOne 双态(q3): size==1 返回; >1 时 throwEx 抛 TooManyResultsException/false 取首个; exists=selectCount>0
关键设计: 直连机制(q2): default 方法在**代理对象(this=MapperProxy 代理)**里执行 — 用 MybatisUtils 从代理提取 SqlSession/MapperInterface, 手拼 statementId 直调 — 比 M-3 的 MapperMethod 分发更轻(跳过签名解析); 这是 M-3 default 方法 MethodHandle 机制的 MP 侧运用。[模式: 代理内直连]
数据流: deleteById(Serializable) → getMybatisMapperProxy(this) → sqlSession.delete(接口名+DELETE_BY_ID, obj) → M-2 Executor。

### 3. ServiceImpl — baseMapper 封装 + 工厂提取 + statementId 复用

场景: ServiceImpl 怎么拿到 mapper 和 SqlSessionFactory?批量保存怎么实现?
源码路径:
- `ServiceImpl.java:58-93` — baseMapper 字段(Spring 注入 M)+getBaseMapper 非空校验; **getSqlSessionFactory: MybatisUtils.getMybatisMapperProxy(baseMapper)→SqlSessionFactory(懒加载 volatile)**(q4)
- `ServiceImpl.java:74-132` — getEntityClass/currentModelClass: 泛型解析(GenericTypeUtils)
- `ServiceImpl.java:166-170` — getSqlStatement(SqlMethod): **SqlHelper.getSqlStatement(currentMapperClass, method)** — 复用 MP-1 注入的 statementId 命名空间(q5)
- `ServiceImpl.java:180-190` — saveBatch(q6): **@Transactional(rollbackFor=Exception)**+executeBatch→SqlHelper.executeBatch(批量 SqlSession, 分批提交 DEFAULT_BATCH_SIZE=1000)
- `SqlHelper.java:113-123` — retBool: 影响行数 int/long→boolean(q7)
关键设计: 三层解耦(q4/q5): Service 持有 mapper(注入), 工厂从代理提取(不依赖 Spring 上下文), statementId 复用注入命名空间 — Service 层方法完全镜像 BaseMapper 方法面 + 批量/事务增强; 批量路径(q6): 单会话批量执行+分批提交。[模式: 门面+延迟提取]
数据流: saveBatch(list) → @Transactional → executeBatch → SqlHelper 批量会话 → 每批 flushStatements → retBool。

### 4. 链式入口 — lambdaQuery/queryChain 与 MP-3 衔接

场景: service.lambdaQuery().eq("name", "x").list() 的链条从哪开始?
源码路径:
- `IService.java:596-655` — **queryChain/lambdaQuery/lambdaUpdate/updateChain** → `ChainWrappers.lambdaQueryChain(getBaseMapper(), getEntityClass())` → LambdaQueryChainWrapper(q8, MP-3 导航)
- `IService.java` — 677 行方法面: getById/list/page/save/update/remove 族 + lambda 变体
关键设计: 链式入口(q8): IService 的 default 方法返回 ChainWrappers 构造的 LambdaQueryChainWrapper — **链的终点是 mapper 方法**(wrapper.list() 内部调 getBaseMapper().selectList(wrapper)), 与 MP-3 条件构造器衔接。[模式: 链式调用]
数据流: lambdaQuery() → ChainWrappers.lambdaQueryChain(mapper, entityClass) → .eq().list() → baseMapper.selectList(wrapper) → MP-1 注入语句。

### 负面空间 — 用户层刻意不做的事

- **不做事务注解**: BaseMapper 无 @Transactional, 事务边界在 ServiceImpl 方法(@Transactional 批量)或外部切面 — 单方法 CRUD 由 SqlSession 会话级事务覆盖
- **不做 SQL 拼接**: 全部条件走 Wrapper(MP-3), 不开放字符串 SQL 拼接(防注入)
- **不做实体校验**: 非空/字段校验靠 MP-7 填充与业务层, BaseMapper 直通

→ 引出: 链式 wrapper 是 MP-3 Lambda 条件构造器的入口 — 下一篇拆 SerializedLambda 反序列化与 AbstractWrapper 条件段构建 → [[MP-3-lambda-wrapper]]
