# MP-9 BaseMapper+IService — completeness-questions

## 开发者视角

1. BaseMapper 里哪些方法是抽象(需注入)的?哪些是 default(自带实现)?怎么区分?
2. deleteById(Serializable) 和 deleteById(T) 的执行路径有什么不同?为什么一个走 MapperMethod 一个直连?
3. BaseMapper 的 default 方法怎么拿到 SqlSession?MybatisUtils.getMybatisMapperProxy 是干什么的?
4. selectOne 查出多条会怎样?throwEx 参数控制什么?
5. ServiceImpl 怎么拿到 SqlSessionFactory?为什么能从 mapper 代理里提取?
6. saveBatch 传 1 万条会发多少次 SQL?batchSize 参数怎么用?
7. service.removeById 返回 boolean 的底层转换是什么?retBool 干什么?
8. lambdaQuery().eq(...).list() 的完整链条是什么?最后调用谁?

## 架构师视角

9. 19 抽象方法名与 MP-1 注入的 SqlMethod 一一对应 — 这个"镜像设计"如何保证不漂移?如果两边不同步会怎样?
10. default 方法直连 SqlSession(绕过 MapperMethod)的设计取舍: 什么场景需要?代价是什么(失去签名解析/拦截)?
11. ServiceImpl 的 getSqlSessionFactory 从代理提取而非注入 — 与 Spring 的耦合关系?为什么这样设计?
12. saveBatch 的 @Transactional + executeBatch + 分批提交: 事务边界在哪?批量会话与普通会话的区别?
13. BaseMapper 双轨方法面(抽象+default)与 M-3 的 plain/default 方法分派(MethodHandle)如何呼应?
14. IService 677 行方法面与 BaseMapper 17 方法的关系?为什么 Service 层要重复一遍?

## 学生视角

15. userMapper.insert(user) 从接口到 SQL 的完整链路(注入期+执行期, 对比 MP-1/M-3)?
16. service.saveBatch(list) 的内部调用链(@Transactional→executeBatch→SqlHelper→批量 SqlSession)?
17. BaseMapper 的 exists 怎么实现?为什么不直接用 selectList?
18. Mapper<T> 空接口的作用?isSupperMapperChildren 为什么以它为判断依据(导航 MP-1)?
19. selectPage 返回 IPage — 分页参数怎么传给 SQL(导航 MP-5)?
20. ServiceImpl 的 getEntityClass 泛型解析怎么工作?为什么需要它?
