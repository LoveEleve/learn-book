# F-1 Builder 门面 + 动态代理 — 完备性问题集 (20 问)
> **ℹ 8-16 增强版 (备查)**: 本文件为 F-1 的 8-16 深化版, 独有内容 (configKey 规则/verify 四检查/equals 语义/MethodHandle 三路 Lookup/代理三分派) 已并入权威版 f1-builder (§5/§6) 与 f3-proxy (§1)。保留作增强参考, 非权威。

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. CRTP 泛型 `<B extends BaseBuilder<B, T>>` 解决了什么问题? 为什么 thisB() 要强转?
2. build() 为什么是 enrich().internalBuild() 而非直接 build? 克隆的意义?
3. Capability.enrich 怎么按"返回值类型"匹配增强方法? 与接口实现差异?
4. newInstance 四步的顺序为什么 verify 在最前?
5. FeignInvocationHandler 的 equals 为什么比较 target 而非 proxy?

## B. 源码实证 (5)

6. configKey 格式的完整生成规则? 泛型参数怎么处理? (grep Feign.java:69-84)
7. getFieldsToEnrich 排除哪 7 类字段? 为什么排除 executorService? (grep BaseBuilder.java:366-383)
8. internalBuild 装配的 SynchronousMethodHandler.Factory 10 参各是什么? (grep Feign.java:217-231)
9. TargetSpecificationVerifier 的 CompletableFuture 校验三条件? (grep ReflectiveFeign.java:173-203)
10. DefaultMethodHandler 的三路 Lookup 各解决什么? (grep L54-62)

## C. 推理深挖 (5)

11. ParseHandlersByName 为什么"Contract 跳过 default, 代理层补回"? 分工的根因?
12. 如果 ConfigKey 格式变化, 哪些层会受影响? (F-2/F-3/F-4 跨域)
13. Capability 增强 interceptors 为什么"逐元素+整体"双增强? 漏掉整体会怎样?
14. ResponseMappingDecoder 的 mapAndDecode 与 Decoder 的关系? 为什么是装饰器?
15. 为什么 default 方法不能直接反射 invoke 而要 MethodHandle? (harness H3 实证)

## D. 跨域扩展 (5)

16. Feign Builder vs MyBatis MapperProxy 的代理创建差异?
17. Capability vs Spring BeanPostProcessor 的增强时机对比?
18. CRTP Builder vs Lombok @Builder 的泛型链式设计?
19. configKey vs MyBatis 的 statementId (namespace+id) 设计对照?
20. 给 Feign 加 newInstance 缓存, 应该在 Builder 还是 ReflectiveFeign? 为什么 Javadoc 只说"你应该缓存"?
