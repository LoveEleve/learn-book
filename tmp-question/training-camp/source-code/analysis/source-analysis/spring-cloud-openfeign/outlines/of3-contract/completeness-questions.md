# OF-3 契约集成 — completeness-questions (全视角提问验证)

## 开发者视角

1. 类级为什么禁止? (路径歧义)
2. 组合注解? (@GetMapping 等)
3. HTTP method 默认? (GET)
4. produces/consumes? (Accept/Content-Type)
5. 7 处理器? (PathVariable 等)
6. Pageable 特判? (queryMapIndex)
7. GET 警告? (降级 POST)
8. SpringQueryMap? (对象展开)

## 架构师视角

9. 为什么 extends BaseContract? (模板方法复用)
10. 为什么组合注解? (Spring 风格无缝)
11. 为什么 checkOne? (配置错误早暴露)
12. 为什么注册表分发? (可扩展)
13. 为什么 Pageable 特判? (分页语义)
14. 为什么 GET 警告? (防运行时降级)
15. 为什么 Formatter 注册? (类型转换扩展)
16. 为什么 CollectionFormat? (集合编码控制)

## 学生视角

17. 什么是契约? (注解→请求模板)
18. 什么是组合注解? (@GetMapping=@RequestMapping+method)
19. 什么是处理器? (注解→参数映射)
20. 什么是 QueryMap? (对象展开查询参数)
