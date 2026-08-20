# 闭环笔记 q4: extractParameters 提取链 + objectSet 重入修复

## 假设
ParameterHandler 收到的 parameter 可能是实体/Collection/数组/Map (多参数封装), 填充要覆盖所有形态, 且同一实体出现多次不能被重复填充。

## 验证过程
- MybatisParameterHandler.java:72-79 processParameter: 非 null + **非 SimpleType** + INSERT/UPDATE 命令 → extractParameters(parameter).forEach(this::process)
- L188-207 extractParameters 四分支:
  1. Collection → 直接返回
  2. Array (非 primitive 元素) → toCollection
  3. Map → **所有 value 都 toCollection 展开进 list** + `objectSet.add(v)` 去重 (同一对象多次出现只填一次)
  4. 其他 (实体) → singleton
- L210-221 toCollection: null→empty; 数组/Collection→列表; 其他→singleton — Map value 是任意对象也能包一层
- L81-109 process: Map 时找 `Constants.ENTITY` ("et") 键提实体; 否则 parameter 本身当实体; getTableInfo 定位 → 填充
- 重入修复 (ae5592621, 2023-09-29, I8506T/I82VLI "修复参数填充多次重入问题"): objectSet 去重 — updateById 的 Map 里 et 与 MP_OPTLOCK_ET_ORIGINAL 指向同一实体时, 不填两次
- CHANGELOG 91: "修复字段填充处理器可能会出现重入问题"

## 代码类型
Glue (参数形态适配) + Algorithmic (去重)

## 跨域关联
- M-2 (wrapCollection→wrapToMapIfCollection: Collection→{"collection","list"}/数组→{"array"}) → MP 不依赖 MyBatis 的包装, 自己从 Map 值展开
- MP-6 (beforeUpdate 也是 parameter Map + ENTITY 键提取) → 同一提取约定 Constants.ENTITY
- MP-9 (saveBatch 批量) → 批量填充的支撑

## 结论
提取链四分支覆盖 实体/Collection/数组/Map(值展开), objectSet 防重入 (同一实体在 Map 中出现多次只填一次)。与 MyBatis 的 wrapToMapIfCollection 不同 — MP 直接对 Map 所有值展开, 兼容 updateById 的 et+MP_OPTLOCK_ET_ORIGINAL 双键。
源码位置: MybatisParameterHandler.java:72-79, 81-109, 188-221
