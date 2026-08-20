# F-1 Builder 装配 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. BaseBuilder 泛型签名什么意思? 为什么是 CRTP?
2. 18 个配置字段的默认值分别是什么?
3. build() 的流程? enrich 和 internalBuild 分别做什么?
4. Capability 怎么注册? enrich 反射怎么工作?
5. target(Class, url) 和 target(Target) 区别?
6. closeAfterDecode/decodeVoid/dismiss404 分别控制什么?

## 架构师视角

7. CRTP vs 普通 Builder: 链式类型安全的价值?
8. clone 副本装饰为什么必要? 原 Builder 复用场景?
9. Capability reduce 流水线的叠加语义? 多个能力怎么协作?
10. 装配为什么只产生两个新对象 (ResponseHandler + Factory)?
11. 默认值全"安全保守"的设计哲学?
12. 13.x internalBuild vs 旧版 build 的差异?
13. Capability 反射 invoke 的意义? 为什么不用接口?

## SRE/运维视角

14. 监控横切 (Metrics) 怎么挂? 影响什么?
15. build 后还能改配置吗? 复用 Builder 注意什么?
16. 组装错误什么时候暴露? 怎么排查?
17. 多客户端共享 Builder 的线程安全?

## 研究者视角

18. vs Retrofit.Builder: 配置面差异?
19. vs Spring Boot 自动装配: 显式 vs 隐式?
20. CRTP 在 Java 生态的应用 (其他框架)?
21. Capability vs Decorator 模式/SPI?
22. 13.x Builder 重构 (BaseBuilder 提取) 的动机?

## 学生视角

23. 什么是 Builder 模式? 为什么链式?
24. 什么是默认值? 为什么"开箱即用"?
25. 什么是泛型? 自引用泛型什么意思?
26. 什么是装饰器? 包住原组件加功能?
27. 什么是 clone? 复制一份为什么安全?
