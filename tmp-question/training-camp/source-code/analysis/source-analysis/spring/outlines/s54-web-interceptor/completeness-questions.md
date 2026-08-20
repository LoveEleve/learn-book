# C-12 拦截器全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 登录校验应该写在哪个方法？ | §1 (preHandle, 返回 false 中断) |
| 2 | 多个拦截器执行顺序？post 为什么逆序？ | §1 (正进逆出, LIFO) |
| 3 | 统一加响应头在哪个方法？ | §2 (postHandle L1096, 渲染前) |
| 4 | 拦截器抛异常时 afterCompletion 还执行吗？ | §2 (L1109 兜底) |
| 5 | 怎么配置拦截器只对 /api/** 生效？ | §3 (addPathPatterns) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么 post/after 逆序、after 还限 interceptorIndex？ | §1 关键设计 (栈式+部分清理) |
| 7 | preHandle false 中断后发生了什么？ | §1 (触发 afterCompletion + 处理器不执行) |
| 8 | 拦截器 vs Servlet Filter 区别？ | §3 (时机与能力) |
| 9 | 拦截器顺序怎么控制？ | §3 (order) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 10 | afterCompletion 和 postHandle 的本质区别？ | §2 (无条件清理 vs 正常流程) |

## 覆盖: 10 问 / 3 身份 / 100%
