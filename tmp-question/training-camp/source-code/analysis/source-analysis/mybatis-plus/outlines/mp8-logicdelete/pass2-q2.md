# 闭环笔记 q2: 双值配置与值渲染 — value/delval/全局兜底/NULL 特例/引号

## 假设
逻辑删除两个值 (未删除/已删除) 有三级来源 (注解>全局>默认) + 特殊值渲染 (字符串 "NULL"→SQL NULL 语义, 字符串值加引号)。

## 验证过程
- TableFieldInfo.initLogicDelete L406-430: `tableLogic.value()` 非空→logicNotDeleteValue 用注解值; 空→dbConfig.getLogicNotDeleteValue() (全局); delval 同理 → logicDeleteValue; 无注解时全局 logicDeleteField 属性名匹配兜底
- GlobalConfig.DbConfig L180-186: logicDeleteField (默认 null, 全局属性名); **logicDeleteValue = "1" (默认已删除); logicNotDeleteValue = "0" (默认未删除)**
- TableInfo.formatLogicDeleteSql L450-470:
  - `NULL.equalsIgnoreCase(value)` → 查询: `column IS NULL`; 删除: `column=NULL` — **字符串 "NULL" (不区分大小写) 渲染成 SQL NULL 语义** (CHANGELOG 466: "支持字符串 'null'"; H2 测试 logicNotDeleteValue="NULL" 实证)
  - `isCharSequence()` → `'%s'` 加单引号; 否则裸值 `%s` (数字/函数如 NOW())
  - H2 测试实证: logicDeleteValue="NOW()" → SET deleted=NOW(); logicNotDeleteValue="NULL" → WHERE deleted IS NULL
- 组合注解穿透 (TableInfoHelperTest:104-110): @MyTableLogic(value="false", delval="true") 组合注解 → 值断言 (AnnotationUtils 递归穿透, MP-2 交付)

## 代码类型
Interface (注解契约) + Algorithmic (值渲染)

## 跨域关联
- MP-2 (initLogicDelete 在 TableFieldInfo 构造; AnnotationUtils 组合注解穿透) → 双值来源
- annotation 模块 (@TableLogic 40 行) → 载体

## 结论
双值三级来源 (注解 value/delval > 全局 DbConfig > 默认 "0"/"1"), 渲染规则: "NULL" 字符串→SQL NULL 语义 (IS NULL / =NULL), 字符串类型→单引号包裹, 其他 (数字/函数/时间)→裸值。全局配置可注入函数 (NOW())。
源码位置: TableFieldInfo.java:406-430; GlobalConfig.java:180-186; TableInfo.java:450-470
