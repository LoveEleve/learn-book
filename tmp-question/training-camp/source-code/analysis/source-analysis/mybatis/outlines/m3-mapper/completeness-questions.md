# M-3 Mapper 代理 — completeness-questions

## 开发者视角

1. mapper 接口方法没绑定 XML/注解时, 什么时候抛 "Invalid bound statement (not found)"?为什么是调用时才抛?
2. 为什么每个 SqlSession getMapper 拿到的是新代理, 但方法解析只做一次?methodCache 在哪里共享?
3. mapper 接口里写 toString()/equals() 会被拦截执行 SQL 吗?为什么?
4. 接口 default 方法(如带默认实现的 mapper 方法)怎么执行?和普通方法有什么不同?
5. 多参数方法不写 @Param 时, XML 里 #{id} 能用吗?#{param1} 什么时候可用?为什么推荐 @Param?
6. mapper 方法返回 boolean/int/List/Optional 时分别怎么转换?update 返回 boolean 的语义是什么?
7. mapper 方法返回原始类型(int 等)且查不到数据时会发生什么?
8. 单 List 参数传进 mapper 方法, XML 里 foreach collection 应该写什么?为什么?
9. @Param 名称和 #{param1} 通用名冲突时哪个生效?

## 架构师视角

10. MapperRegistry.addMapper 为什么必须先 put 再 parse?失败回滚机制 (loadCompleted) 解决什么问题?
11. MapperProxy 的三层结构 (Factory/Proxy/Invoker) 各承担什么?为什么 invoker 策略要分开?
12. default 方法的 MethodHandle 双版本兼容 (JDK8 Lookup 反射构造 vs JDK9 privateLookupIn) 解决什么问题?
13. SqlCommand 的父接口递归解析 (resolveMappedStatement) 支持什么继承场景?和 M-1 StrictMap 的关联?
14. ParamNameResolver 的命名优先级 (@Param > 实际参数名 > 索引名) 各依赖什么编译/运行时条件?为什么索引名被 gcode#71 保留?
15. 特殊参数 (RowBounds/ResultHandler) 为什么既跳过命名又不进 SQL?getUniqueParamIndex 抛错保护什么?
16. MapperAnnotationBuilder 的同名 XML 优先 (loadXmlResource) 设计 — 混合使用时注解和 XML 如何分工?

## 学生视角

17. userMapper.selectById(1) 的完整调用链 (代理 invoke→invoker→MapperMethod.execute→sqlSession→Executor) 每层做什么?
18. MapperMethod.execute 的 SELECT 五分支判定顺序是什么?为什么 void+ResultHandler 要最先判断?
19. ParamMap 相比普通 HashMap 改了什么?拼错参数名会怎样?
20. TypeParameterResolver.resolveReturnType 解决什么泛型问题?为什么 List<User> 需要解析?
21. MapperAnnotationBuilder 的注解语句解析失败时怎么延迟处理?和 M-1 的 incomplete 机制什么关系?
