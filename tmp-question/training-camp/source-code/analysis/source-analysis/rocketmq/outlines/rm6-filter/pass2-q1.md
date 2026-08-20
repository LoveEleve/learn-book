# 闭环笔记 q1: 表达式类型 — SQL92 vs TAG

## 假设
双类型: SQL92 全语法 vs TAG 简单 OR; isTagType 判定。

## 验证过程
- **ExpressionType** (common/filter/ExpressionType.java):
  - **SQL92** (L37): 语法文档 — 关键字 (AND/OR/NOT/BETWEEN/IN/TRUE/FALSE/IS/NULL) + 类型 (Boolean/String/Decimal/Float) + 文法 (AND/OR/>/>=/</<=/= /BETWEEN=≥A AND ≤B /IN='a' OR='b' 仅 String /IS NULL / =TRUE) — **ActiveMQ SQL92 风格**
  - **TAG** (L45-47): "tag1 || tag2 || tag3"; null/* = 全订阅
  - **isTagType** (L49-53): type==null || "" || TAG → true — **默认 TAG 兼容**
- **TAG 匹配**: 订阅字符串 → tag 哈希集合 (codeSet) → CQ tagsCode 哈希比对 (RM-3 交叉)

## 代码类型
Interface (类型契约)

## 跨域关联
- RM-3 (CQ): tagsCode 哈希 (TAG 匹配面)
- RM-8 (消费): 订阅注册

## 结论
双类型契约: SQL92 (属性级表达式) vs TAG (标签哈希集合); 默认 TAG (兼容); SQL92 语法为 ActiveMQ 风格文档。
源码位置: common/filter/ExpressionType.java:37-53
