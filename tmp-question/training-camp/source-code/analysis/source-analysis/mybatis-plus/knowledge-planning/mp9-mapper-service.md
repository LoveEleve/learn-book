# MP-9 BaseMapper+IService — 17 方法面 + default 直连 + ServiceImpl 链式

> 项目: MyBatis-Plus | 🟡 Working / 1 篇 | BaseMapper(565, 19 抽象+大量 default)+Mapper(25 空接口)+IService(677)+ServiceImpl(310)+SqlHelper
> 基线: MP-PLAN MP-9 — 前置: **MP-1 (注入方法面) + MP-2 (TableInfo) + MP-3 (Lambda 链, 导航)** — 展开 方法面→default 直连→Service 层封装→链式

---

## §0.8

- 🟡 Working，1篇 — 方法面(**BaseMapper L108-565: **19 个抽象 CRUD 方法**(实测 L108-423: insert/deleteById/delete/updateById/update/selectById/selectBatchIds×2/selectCount/selectList×4/selectMaps×4/selectObjs×2) + 大量 default 方法[deleteById(Serializable) L115-138/deleteByMap/deleteByIds/selectOne/selectByMap/exists/insertBatch/updateBatchById/insertOrUpdate/selectPage/selectMapsPage]; Mapper<T> L23-25 空标记接口**) → default 直连(**default 方法族: MybatisUtils.getMybatisMapperProxy(this)→sqlSession.delete/update[绕过 MapperMethod, 直接构造 statementId=接口名+SqlMethod 名 L136-138]; deleteByIds 组装 params[entity/collection, 填充联动] L205-216; selectOne 双态 L305-314[size==1 返回/>1 按 throwEx 抛 TooManyResultsException 或取首个]; exists=selectCount>0 L325-329**) → Service 层(**ServiceImpl L58-310: baseMapper 字段+getBaseMapper L58-63; getSqlSessionFactory 从 MybatisMapperProxy 提取[懒加载 volatile L88-93]; getEntityClass/currentModelClass 泛型解析 L74-132; getSqlStatement=SqlHelper.getSqlStatement(接口名, SqlMethod)[MP-1 注入 id] L166-170; saveBatch=@Transactional+executeBatch[SqlHelper 批量会话] L180-190**; SqlHelper.retBool int/long→boolean L113-123) → 链式(**IService L596-655: queryChain/lambdaQuery/lambdaUpdate/updateChain → ChainWrappers → LambdaQueryChainWrapper[MP-3 导航]; IService 677 行方法面[getById/list/page/save/update/remove 族]**)
- 设计模式: [模式: 门面(Service 层)+模板+链式调用]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| BaseMapper.java:108-565 | 方法面 | **19 抽象 CRUD + 大量 default**: insert/deleteById/delete/updateById/update/selectById/selectBatchIds/selectCount/selectList/selectMaps/selectObjs 族 + default 便捷方法 | High |
| BaseMapper.java:115-138 | default 直连 | deleteById(Serializable): **MybatisUtils.getMybatisMapperProxy(this)→sqlSession.delete(接口名+DELETE_BY_ID)** — 绕过 MapperMethod 直接执行 | High |
| BaseMapper.java:205-216 | 批量参数 | deleteByIds: params 组装(entity/collection, 逻辑删除填充联动)→sqlSession.delete(接口名+DELETE_BY_IDS) | High |
| BaseMapper.java:305-329 | selectOne/exists | selectOne 双态(size==1 返回/throwEx 抛或取首个); exists=selectCount>0 | High |
| ServiceImpl.java:58-63 | baseMapper | baseMapper 字段注入+getBaseMapper 非空校验 | High |
| ServiceImpl.java:88-93 | 工厂提取 | getSqlSessionFactory: **MybatisUtils.getMybatisMapperProxy(baseMapper)→SqlSessionFactory 懒加载(volatile)** | High |
| ServiceImpl.java:166-170 | statementId | getSqlStatement: SqlHelper.getSqlStatement(currentMapperClass, SqlMethod) — MP-1 注入 id | High |
| ServiceImpl.java:180-190 | 批量 | saveBatch: @Transactional(rollbackFor)+executeBatch(SqlHelper 批量) | High |
| SqlHelper.java:113-123 | 布尔转换 | retBool: int/long 影响行数→boolean | High |
| IService.java:596-655 | 链式 | queryChain/lambdaQuery/lambdaUpdate → ChainWrappers → LambdaQueryChainWrapper(MP-3 导航) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 方法面是汇聚叶子 — 1篇 (~60行) 按"19 抽象方法面→default 直连机制→Service 封装→链式"展开; 注入方法名衔接 MP-1(引用), Lambda 链衔接 MP-3(导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 19 抽象方法面 + default 便捷族 | 🔴 | **为什么🔴**: 用户接触面 |
| P1-2 | default 直连 (代理取 SqlSession 绕过 MapperMethod) | 🔴 | **为什么🔴**: 核心机制 |
| P1-3 | ServiceImpl 封装 (baseMapper/工厂提取/statementId) | 🔴 | **为什么🔴**: 服务层骨架 |
| P2-1 | 批量 (saveBatch/executeBatch/retBool) | 🟡 | **为什么🟡**: 性能路径 |
| P2-2 | 链式 (lambdaQuery/queryChain, 导航 MP-3) | 🟡 | **为什么🟡**: 体验面 |
| P3-1 | 与 MP-1 注入方法面/MP-3 Lambda 协作 (引用/导航) | 🟢 | **为什么🟢**: 汇聚边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **方法面与直连** | 🔴 | 用户入口 |
| B | **Service 封装** | 🔴 | 服务骨架 |
| C | **批量与链式** | 🟡 | 增强面 |
| D | **汇聚边界** | 🟢 | 衔接 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 19 抽象方法面 | BaseMapper 定义 **19 个抽象 CRUD 方法**(实测穷举 L108-423: insert/deleteById/delete/updateById/update/selectById/selectBatchIds×2/selectCount/selectList×4/selectMaps×4/selectObjs×2) + 大量 default 便捷方法 — **抽象方法名与 MP-1 注入的 SqlMethod 一一对应**(statementId=接口名.方法名) | BaseMapper.java:108-565 |
| q2 | default 直连 | default 方法族(deleteById(Serializable)/deleteByIds 等)**: MybatisUtils.getMybatisMapperProxy(this) 提取代理→sqlSession.delete(接口名+SqlMethod 名)** — 绕过 MapperMethod 分发, 直接构造 statementId 调 SqlSession(M-3 default 方法 MethodHandle 机制的运用) | BaseMapper.java:115-138,205-216 |
| q3 | selectOne 双态 | 本地 selectOne: size==1 返回; >1 时 throwEx=true 抛 TooManyResultsException(与 M-3/M-2 语义一致)/false 取首个 — exists=selectCount>0 | BaseMapper.java:305-329 |
| q4 | ServiceImpl 骨架 | baseMapper 字段(Spring 注入)+getBaseMapper 非空校验; getSqlSessionFactory 从 MybatisMapperProxy 提取(懒加载 volatile) — Service 不直接依赖 Spring 上下文 | ServiceImpl.java:58-93 |
| q5 | statementId 复用 | getSqlStatement(SqlMethod)=SqlHelper.getSqlStatement(currentMapperClass, method) — **复用 MP-1 注入的 statementId 命名空间**(接口名.方法名) | ServiceImpl.java:166-170 |
| q6 | 批量路径 | saveBatch: @Transactional(rollbackFor=Exception)+executeBatch→SqlHelper.executeBatch(批量 SqlSession, 分批提交 DEFAULT_BATCH_SIZE=1000) | ServiceImpl.java:180-190 |
| q7 | 布尔语义 | SqlHelper.retBool: 影响行数 int/long→boolean(>0 true) — Service 层统一返回 boolean | SqlHelper.java:113-123 |
| q8 | 链式入口 | IService default: queryChain/lambdaQuery/lambdaUpdate → ChainWrappers → LambdaQueryChainWrapper(MP-3 条件构造器, 导航) | IService.java:596-655 |

→ 引出 MP-3: Lambda 条件构造器 — Service 链式的 queryChain/lambdaQuery 返回的 LambdaQueryChainWrapper 是 MP-3 的核心; 至此 MP 方法面闭环。
