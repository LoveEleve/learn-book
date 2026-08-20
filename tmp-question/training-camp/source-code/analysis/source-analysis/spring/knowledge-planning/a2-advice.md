# A-2 Advice 链 — ReflectiveMethodInvocation.proceed 递归迭代

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 5文件/~500行
> 基线: A-1 代理 — JdkDynamicAopProxy.invoke/CglibAopProxy.intercept 都调用 proceed

---

## §0.8

- 🟡 Working，1篇 — ReflectiveMethodInvocation.proceed 递归 → 五种Advice(Before/Around/AfterReturning/Throws/Introduction)执行顺序
- 设计模式: [模式: 责任链模式]—proceed递归迭代advice链—每个advice决定是否调用proceed

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ReflectiveMethodInvocation.java:62+L160 | proceed() | **递归核心**: L160 `if(currentInterceptorIndex < interceptors.size-1)`→L164 `interceptor.invoke(this)`→advice内部调`invocation.proceed()`→L178递归→index递增→直到全部advice调完→L195 invokeJoinpoint→method.invoke(target) | High |
| MethodBeforeAdvice.java:31 | before() | 在proceed之前调用—method.invoke之前—可修改方法参数 | High |
| MethodInterceptor.java:61 | invoke() | AOP Alliance标准接口—invoke(MethodInvocation)→advice决定是否调proceed—Around advice的核心 | High |
| AfterReturningAdvice.java:31 | afterReturning() | 在proceed正常返回后调用—可修改返回值—若proceed抛异常→不调用 | High |
| ThrowsAdvice.java:51 | afterThrowing() | 在proceed抛异常后调用—接收异常对象—可决定是否重新抛/处理/转换 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**P1 核心 (1):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ReflectiveMethodInvocation.proceed 递归迭代 + 五种Advice执行顺序 | 🟡 | **为什么🟡**: 虽然重要但是核心实现(297行)概念简单——proceed的本质是"从index=0开始遍历advice列表→每个advice调用invocation.proceed()→递归直到列表结束→invokeJoinpoint" |

**1篇理由**: ~500行/5文件—核心就是proceed递归(297行)+四种advice接口(200行)。1篇(~40行)覆盖递归链+五种Advice顺序。

**单篇结构**: §1 ReflectiveMethodInvocation.proceed 递归 → §2 五种Advice执行顺序(Before→Around→Joinpoint→AfterReturning→Throws) + 调用链示意图
