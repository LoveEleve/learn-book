# N-23 认证/权限深化 — 完备性问题集 (20 问)

## A. 机制理解 (5)
1. 身份双构建 (gRPC/HTTP) 的提取差异?
2. 资源解析的协议×领域矩阵?
3. Secured 注解的声明语义?
4. 身份构建失败的语义?
5. Secured 注解与资源解析的衔接?

## B. 源码实证 (6)
6. 身份构建器类名? (grep context/)
7. 资源解析器清单? (grep parser/)
8. Secured 字段? (grep annotation/)
9. GrpcIdentityContextBuilder 的提取源?
10. HttpIdentityContextBuilder 的 header 提取?
11. 资源解析器的注册方式?

## C. 推理深挖 (5)
12. ServerIdentityCheckerHolder 的检查链?
13. NacosAuthConfigHolder 的配置项?
14. 服务器身份校验的防伪语义?
15. 权限判定与资源解析的衔接?
16. 错误码的分级?

## D. 跨域扩展 (4)
17. 权限判定的缓存?
18. 服务器身份伪造的防护强度?
19. 认证失败的响应码?
20. 多租户下的身份隔离?

