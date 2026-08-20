# OF-9 动态刷新 — completeness-questions (全视角提问验证)

## 开发者视角

1. 刷新后 URL 怎么变? (url() 覆写)
2. RefreshableUrl 从哪来? (FactoryBean)
3. 无配置会怎样? (null 容错)
4. 懒加载? (url() 首次计算)
5. SpEL 排除? (#{} 留运行时)
6. 前缀补全? (http://)
7. URI 校验? (malformed 抛)
8. 按名获取? (RefreshableUrl-contextId)

## 架构师视角

9. 为什么动态覆写? (每次取最新)
10. 为什么 FactoryBean? (容器管理)
11. 为什么 @RefreshScope 联动? (配置刷新)
12. 为什么 null 容错? (不炸)
13. 为什么懒计算? (AOT 兼容)
14. 为什么 URL 解析统一? (一处规范化)
15. 为什么三态? (直连/动态/懒加载)
16. 为什么按 contextId 命名? (唯一)

## 学生视角

17. 什么是动态刷新? (配置变了地址变)
18. 什么是懒加载? (用到才算)
19. 什么是 FactoryBean? (工厂)
20. 什么是 SpEL? (运行时表达式)
