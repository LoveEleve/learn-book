# D-2 Filter 拦截链 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 写一个自定义 Filter 要继承谁?有几种写法? | §4 (FilterAdapter 空实现 / 风格 A override / 风格 B 模板钩子) |
| 2 | 一次 stmt.execute 穿过几个 Filter 后谁真执行? | §2 (链尾 rawObject.execute L3010) |
| 3 | 每次 JDBC 操作都 new 链对象吗? | §3 (per-connection 缓存复用 L223/234) |
| 4 | filters: stat,wall 是怎么变成对象的? | §4 (FilterManager aliasMap L35 + loadFilter L99) |
| 5 | 链操作的对象是什么类型? | §3 (proxy/jdbc 的 Proxy 族) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么用递归不用迭代 for 循环? | §2 (Before/After 两侧时机需要调用栈) |
| 7 | 为什么 chain 要 per-connection 缓存? | §3 (5287 行对象避免每次 new, reset 归零) |
| 8 | 为什么 Filter 体系要 5 个类? | §1 (接口/空实现/模板/契约/实现 各司其职) |
| 9 | 防火墙为什么必须 override 而非模板钩子? | §4 (execute 前决定放行, 钩子拿不到拦截能力) |
| 10 | 与 Spring AOP CGLIB 的拦截有什么本质区别? | §1/§4 (显式链+Proxy 对象 vs 字节码代理) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 11 | pos 指针是干什么的? | §2 (链进度, nextFilter 前进) |
| 12 | 链尾有三类动作分别是什么? | §2 (裸执行/池借出/对象包装) |
| 13 | ConnectionProxyImpl 在链里是什么角色? | §3 (链的载体, 持有 createChain) |
| 14 | Filter 从哪加载? | §4 (配置别名+SPI+@AutoLoad) |
| 15 | 执行抛 Error 时统计钩子还会执行吗? | §1 (模板三路 catch L187-195, Error 也走 ErrorAfter 后重抛) |
| 16 | cloneChain 是谁用的? | §2 (预留扩展 API, 框架内部无调用点) |
| 17 | 链尾返回 ResultSet 和返回 int 的处理差别? | §2 (对象包 Proxy 可继续过链, 标量直通) |

## 覆盖: 14 问 / 3 身份 / 100%
