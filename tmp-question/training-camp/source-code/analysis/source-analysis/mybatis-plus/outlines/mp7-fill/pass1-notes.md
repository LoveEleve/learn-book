# MP-7 自动填充 — Pass 1 探索笔记

> 域: MP-7 自动填充 (core/handlers) | 🟡 方案 B | 2026-08-13
> 源码: mybatis-plus-core/src/main/java/com/baomidou/mybatisplus/core/
> 已读测试: MetaObjectHandlerTest (strictInsertFill1/2, strictUpdateFill1/2), MybatisParameterHandlerTest (Model 插入/批量/map 参数)

## 继承树/调用图

```
MetaObjectHandler (接口, 243 行)                      StrictFill<T,E> (52 行)
  ├─ insertFill(MetaObject)  [抽象]  ▲实现                ├─ fieldName
  ├─ updateFill(MetaObject)  [抽象]  │                   ├─ fieldType Class<T>
  └─ 16 个 default: openInsertFill×2  │                 └─ fieldVal Supplier<E>
     openUpdateFill×2               │
     setFieldValByName/getFieldValByName  │
     findTableInfo / strictInsertFill×3 / strictUpdateFill×3 │
     strictFill / fillStrategy / strictFillStrategy        │
                                                           │
H2MetaObjectHandler (h2 测试) ────────────────┘
  (since 2017-06-25, hubin)

调用链 (执行期):
BaseExecutor.doQuery/doUpdate (SimpleExecutor:48,62,75 每次执行 new)
  → configuration.newStatementHandler
    → BaseStatementHandler 构造 L70: newParameterHandler
      → LanguageDriver.createParameterHandler (LanguageDriver.java:44)
        → MybatisXMLLanguageDriver (MP 覆写):46 → new MybatisParameterHandler(ms, param, boundSql)
          → 构造 L64-70: super 先 (L65, DefaultParameterHandler 构造存参) → processParameter(parameter) L69 ← 填充 hook!
            → 仅 INSERT/UPDATE 命令 + 非 SimpleType (L74-75)
            → extractParameters 提取实体 (L188-207)
            → process (L81-109): TableInfo 定位 → newMetaObject
              → INSERT: populateKeys(主键) → insertFill(metaObject)
              → UPDATE: updateFill(metaObject)
          → super(mappedStatement, parameter, boundSql) ← DefaultParameterHandler 构造
            (填充在 setParameters 之前完成, 实体已被修改)

Spring 装配 (静态):
MybatisPlusAutoConfiguration:214 → getBeanThen(MetaObjectHandler.class, globalConfig::setMetaObjectHandler)
  → GlobalConfig.metaObjectHandler (config/GlobalConfig.java:84)
  → GlobalConfigUtils.getMetaObjectHandler(configuration) (toolkit/GlobalConfigUtils.java:110)
    → MybatisParameterHandler.insertFill/updateFill L129/L137

元数据侧 (MP-2):
@TableField(fill=INSERT/UPDATE/INSERT_UPDATE) → FieldFill 枚举 (annotation 模块)
  → TableFieldInfo.fieldFill (L149) → withInsertFill/withUpdateFill (L221-222)
  → TableInfo.setFieldList 聚合: isWithInsertFill→this.withInsertFill=true (TableInfo.java:493-508)
  → TableInfo.isWithInsertFill/isWithUpdateFill (strictFill 守门 + 注入 SQL 直拼守门)

SQL 侧 (MP-1):
TableInfo.getAllInsertSqlColumnMaybeIf/PropertyMaybeIf (L343-382)
  → TableFieldInfo.getInsertSqlColumnMaybeIf (L486-493): withInsertFill → 不生成 if 标签 (直拼!)
  → TableFieldInfo.getInsertSqlPropertyMaybeIf (L455-462): 同上
  → getSqlSet(ignoreIf) (L512-530): withUpdateFill → 不 if 包裹
```

## 基本元素分解

1. **执行期 hook 注入**: MybatisParameterHandler 构造时 processParameter — 用 LanguageDriver 扩展点换掉 DefaultParameterHandler (M-6 的 LanguageDriver 可插拔机制, 2016-03-11 已有)
2. **实体提取链**: extractParameters (Collection/Array/Map 值展开 + objectSet 去重) → process (Map 提取 et / 直接实体)
3. **双开关注门**: handler 级 openInsertFill() 双签名 + 元数据级 tableInfo.isWithInsertFill()
4. **填充策略族**: setFieldValByName (非 null 才 set) / fillStrategy (null 才填) / strictFillStrategy (null 才填 + supplier 非 null 才 set) / strictFill (三条件匹配)
5. **严格匹配**: StrictFill.of + strictFill 的 property+propertyType+fill 标记三重过滤 (fieldList stream)
6. **主键先行**: populateKeys (ASSIGN_ID/ASSIGN_UUID 才生成, IdType.getKey() >= 3)
7. **SQL 直拼**: withInsertFill/withUpdateFill 字段在注入 SQL 无 if 包裹 — 必有值断言

## 标记问题 (9 个)

1. 为什么填充 hook 在 ParameterHandler **构造时**执行而不是 setParameters 时? 与 DefaultParameterHandler 的关系? (M-2 取参四路)
2. strictFill 三条件匹配 (property+propertyType+fill) — 为什么类型必须精确匹配? 泛型 <T, E extends T> 子类值 (OjbkXx) 怎么匹配?
3. fill 字段 SQL 直拼 (无 if 标签) 的设计动机? 用户不实现 handler 会怎样?
4. extractParameters 的 Map 值全展开 + objectSet 去重 — 重入问题 (I8506T/I82VLI) 怎么修的?
5. openInsertFill 双签名 (3.5.6 加 MappedStatement) — 按 mapper 跳过填充的动机?
6. fillStrategy vs strictFillStrategy 区别? 幂等性怎么保证 (每次执行都 new ParameterHandler)?
7. 时序: populateKeys(主键) 与 insertFill 谁先? 乐观锁 beforeUpdate 与 updateFill 谁先?
8. 边界: update(wrapper) 无实体填充吗? 逻辑删除 (DELETE→UPDATE) 触发 updateFill 吗? SELECT 不处理?
9. MybatisUtils 在 HANDOFF 中被列为 MP-7 源码 — 它与填充无直接关系 (JSON handler + MapperProxy 工具), 实际作用?

## 测试要点 (已读 2 个)

- MetaObjectHandlerTest: strictInsertFill 4 类型 (String/Integer/LocalDate/LocalDateTime) + **子类值 OjbkXx extends Ojbk** (泛型守卫实证); strictUpdateFill 同
- MybatisParameterHandlerTest: SELECT 不填充; INSERT 填充 id(populateKeys)+insertOperator; Map 参数 (Constants.ENTITY=et) 提取; array/list 批量填充; entity 单参数
- H2MetaObjectHandler: **实体没有的字段 (testType1) 不会 set** — strictFill 匹配失败跳过
