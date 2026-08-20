# S2-7 Bean 作用域 — singleton/prototype/request/session + Scoped Proxy

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 6文件/~617行
> 基线: S2-1 refresh() Step 11 — finishBeanFactoryInitialization 中 getBean 触发作用域判定

---

## §0.8

- 🟡 Working，1篇 — Scope 接口 → doGetBean 作用域分支 → Web Scopes(ThreadLocal) → Scoped Proxy(CGLIB/JDK)
- 设计模式: [模式: 策略模式]—Scope.get() 不同作用域实现不同生命周期; [模式: 代理模式]—Scoped Proxy 让短作用域 Bean 注入长作用域 Bean
- singleton/prototype 是 Special Case: singleton 存在 `singletonObjects` 中(不在 scopes map) — prototype 每次 createBean 不存任何容器

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Scope.java:74 | get(name, ObjectFactory) | **核心方法**: 从作用域获取或创建对象 — ObjectFactory 作为回调在初次创建时调用 — 返回作用域内的 bean 实例 | High |
| Scope.java:93 | remove(name) | 从作用域移除对象 — 触发 DestructionAwareBeanPostProcessor 回调 — request/session 作用域结束时 Spring 调用此方法销毁 bean | High |
| Scope.java:152 | getConversationId() | 对话标识 — request/session 作用域返回当前 requestId/sessionId — singleton/prototype 返回 null | High |
| AbstractBeanFactory.java:366-375 | doGetBean scope 分支 | **作用域判定**: mbd.getScope()→scopes map 查找 Scope→scope.get(beanName, createBean回调) — singleton/prototype 不走此分支(singleton 走 getSingleton → singletonObjects / prototype 在 createBean 后不缓存) | High |
| SimpleThreadScope.java:98行 | Thread Scope | **ThreadLocal 实现**: scope.get→threadScope.get()(ThreadLocal<Map>); 每次 get 返回同一个线程范围内的 bean → remove 清除 ThreadLocal 中的 bean 并触发销毁回调 | High |
| AbstractRequestAttributesScope.java:96行 | Request/Session Scope | **RequestContextHolder ThreadLocal**: get→RequestContextHolder.currentRequestAttributes()→getAttribute(name,SCOPE_REQUEST)→null→objectFactory.getObject()(createBean)→setAttribute | High |
| ScopedProxyFactoryBean.java:142行 | Scoped Proxy | **代理注入**: 将短作用域 Bean(request)注入长作用域 Bean(singleton) → singleton 持有的是 Proxy → 每次调用 Proxy 方法时通过 ScopedObject.getTargetFromScope() 获取当前 request/session 中的真实 bean | High |

---

## 02-04 聚合+分类+聚类

### 聚合 (P1≥5 / P2 2-4 / P3 1)

**P1 核心 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | AbstractBeanFactory.doGetBean scope 分支 — Scope.get(ObjectFactory) 回调模式 | 🔴 | **为什么🔴**: 作用域解析的核心 — singleton走singletonObjects / prototype直接createBean / 其他scope走scopes map→Scope.get() — 三个分支决定了Bean的生命周期管理方式 |
| P1-2 | Scope 接口四方法语义 — get/remove/conversationId/resolveContextualObject | 🔴 | **为什么🔴**: 理解每个作用域必须实现什么 — 为什么singleton有专门的singletonObjects而非通过Scope接口? 因为singleton需要处理循环依赖(三级缓存) — Scope接口太简单无法支持 |
| P1-3 | RequestContextHolder + Request/Session Scope — ThreadLocal存储当前请求属性 | 🔴 | **为什么🔴**: Web作用域的核心 — 同一个Bean(request scope)在不同HTTP请求中返回不同实例 — 但获取方式(getBean)完全相同 — 区别只在scope.get()内部通过RequestContextHolder.ThreadLocal区分当前请求 |

**P2 支持 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | ScopedProxyFactoryBean — CGLIB/JDK代理短作用域Bean | 🟡 | **为什么🟡**: 解决"request scope bean注入singleton bean"的注入时机矛盾 — singleton创建时request还不存在 → Proxy延迟到每次方法调用才获取真实bean — 是scope机制的配套代理基础设施 |
| P2-2 | SimpleThreadScope — ThreadLocal作用域 | 🟡 | **为什么🟡**: Web request/session作用域以外最常用的非单例作用域 — ThreadLocal语义简单 — 但不自动清理(需手动unbind) — 是Scope接口的最简实现参考 |

### 聚类 (1篇)

**1篇理由**: ~617行/6文件 — Scope机制虽涉及web/thread/proxy多个维度，但核心就一个概念: getBean时通过scope.get()根据作用域类型返回不同生命周期的Bean。singleton/prototype是special case不通过Scope接口 — 但理解singleton为何是special case本身就是一个重要的Why。1篇文章(~47行)覆盖Scope接口→doGetBean分支→Web Scope→Scoped Proxy。

**单篇结构**: §1 Scope接口 + singleton/prototype为何不走Scope → §2 doGetBean scope分支 + Request/Session/Thread Scope → §3 Scoped Proxy — 长作用域Bean持有短作用域Bean的代理
