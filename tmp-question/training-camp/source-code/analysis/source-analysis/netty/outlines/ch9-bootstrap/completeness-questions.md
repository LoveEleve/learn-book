# Ch9 Bootstrap 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | doBind 三步: validate→initAndRegister→doBind0, 为什么 doBind0 要异步到 EventLoop? | 6.1 §2 |
| 2 | PendingRegistrationPromise 解决了什么竞态? 如果不用它会怎样? | 6.1 §4 |
| 3 | ServerBootstrapAcceptor 的六步子 Channel 初始化每一步做什么? | 6.2 §5 |
| 4 | ChannelInitializer 的 initMap 防重入 — 什么场景会触发重复 initChannel? | 6.2 §6 |
| 5 | Bootstrap.init vs ServerBootstrap.init 的区别是什么? | 6.2 §2,4 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | CRTP 泛型 `B extends AbstractBootstrap<B,C>` — 为什么不直接用 Object 返回? | 6.1 §1 |
| 7 | FailedChannel 降级 — 为什么不能返回 null? 对上层代码有什么影响? | 6.1 §5 |
| 8 | ServerBootstrap 父-子 EventLoop 分离 — worker 什么时候回退到 boss? | 6.2 §3 |

## 学生/新人视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | Bootstrap.clone() 为什么要深拷贝 options 但浅拷贝 handler? | 6.1 §6 |
| 10 | Client 和 Server 的 bind/connect 流程有什么对称性? | 6.2 §1 |

## 覆盖统计
| 角色 | 问题数 | 覆盖 |
|------|:--:|:--:|
| 开发者 | 5 | 6.1§2,4, 6.2§2,4-6 |
| 架构师 | 3 | 6.1§1,5, 6.2§3 |
| 学生 | 2 | 6.1§6, 6.2§1 |
| **合计** | **10** | **100%** |
