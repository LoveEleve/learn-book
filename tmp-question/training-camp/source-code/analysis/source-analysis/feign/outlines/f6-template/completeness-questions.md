# F-6 模板引擎 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. RequestTemplate 的 queries/headers 用什么 Map? 为什么?
2. resolve() 做了什么? resolved 标志防什么?
3. request() 未 resolve 会怎样?
4. {var} 表达式怎么解析? 嵌套花括号怎么办?
5. 表达式长度上限多少? 超了会怎样?
6. header 展开为什么不做 URI 编码? stripCrlf 防什么?
7. CollectionFormat 五种格式分别什么语义? 默认哪个?
8. Request 为什么不可变? requestTemplate() 有什么用?

## 架构师视角

9. 为什么四类位置 (uri/query/header/body) 编码策略不同? 语义差异?
10. 幂等编码怎么实现? isEncoded 检测的边界?
11. RFC 6570 Level 2/3 运算符前缀支持哪些? 为什么只实现 Simple+PathStyle?
12. resolve 后 query 烧进 uri 的设计? 为什么不保持独立结构?
13. 模板拷贝 (from) 的并发语义? 多线程安全吗?
14. 表达式正则非法降级为字面量的动机? 什么场景会触发?
15. JSON body 的 %7B 还原机制? 为什么必须还原?
16. fragment 保留 vs 剥离的取舍?

## SRE/运维视角

17. 参数含中文/特殊字符怎么排查编码问题?
18. 模板展开慢 (大参数) 怎么优化? maxLength 怎么调?
19. header 注入 (CRLF) 攻击防护在哪? 还会漏吗?
20. 请求日志里模板 vs 展开后请求怎么区分?

## 研究者视角

21. vs Retrofit: @Path/@Query vs Feign {var} 模板, 差异?
22. vs RFC 6570: Feign 方言的偏离 ($ 和正则修饰符) 为什么?
23. vs URI 模板 TCK: Feign 通过多少 Level?
24. 表达式分词器 vs 正则解析: 为什么逐字符扫描?
25. vs Handlebars/Mustache: 模板引擎的 HTTP 特化?

## 学生视角

26. 什么是模板? 为什么请求要模板化?
27. 什么是占位符? {var} 什么意思?
28. 什么是 URL 编码? 为什么空格要变 %20?
29. 什么是幂等? 编码为什么不能重复?
30. 什么是不可变对象? 为什么安全?
