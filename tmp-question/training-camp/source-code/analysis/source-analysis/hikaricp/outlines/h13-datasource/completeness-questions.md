# H-13 DriverDataSource 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 驱动怎么被找到/实例化? | §1 (className→classloader→jdbcUrl 多级) |
| 2 | getConnection 底层做什么? | §2 (driver.connect) |
| 3 | 带用户名密码的连接怎么创建? | §2 (clone props 注入 USER/PASSWORD) |
| 4 | 驱动没注册会怎样? | §1 (classloader 直接 newInstance) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么多级解析? | §1 (兼容各种驱动加载场景) |
| 6 | 为什么直接 delegate 不管理? | §2 (连接管理在池) |
| 7 | 为什么 clone props? | §2 (隔离认证, 防并发污染) |
| 8 | DriverDataSource 在池里角色? | §3 (池的叶子连接来源) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | DriverDataSource 是什么? | §1 (Driver 包成 DataSource 的桥) |
| 10 | 连接的原始来源? | §2/§3 (driver.connect) |
| 11 | 与 C-11 边界? | §3 (jdbc 内核复用) |

## 覆盖: 11 问 / 3 身份 / 100%
