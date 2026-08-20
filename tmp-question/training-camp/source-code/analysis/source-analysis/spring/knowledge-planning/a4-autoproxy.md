# A-4 自动代理 — AbstractAutoProxyCreator + BeanName/DefaultAspectJ 匹配策略

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 4文件/~1077行
> 基线: A-1 代理 + A-3 @AspectJ — 怎么决定哪些 Bean 需要代理？答案在 AbstractAutoProxyCreator.wrapIfNecessary

---

## §0.8

- 🟡 Working，1篇 — AbstractAutoProxyCreator.wrapIfNecessary → BeanNameAutoProxyCreator(name匹配) + DefaultAdvisorAutoProxyCreator(advisor匹配) + AspectJAwareAdvisorAutoProxyCreator
- 设计模式: [模式: 模板方法]—wrapIfNecessary 骨架 + getAdvicesAndAdvisorsForBean 子类覆写

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AbstractAutoProxyCreator.java:354 | wrapIfNecessary() | **模板核心**: ①isInfrastructureClass→skip ②shouldSkip→skip ③getAdvicesAndAdvisorsForBean→Object[] advisors ④空→return bean(不代理) ⑤非空→createProxy(target, advisors)→ProxyFactory.getProxy→返回代理 | High |
| AbstractAutoProxyCreator.java:415 | shouldSkip() | 检查AOP基础设施类(Advisor/Advice/AopInfrastructureBean)→跳过不代理 | High |
| BeanNameAutoProxyCreator.java:47+L149 | getAdvicesAndAdvisorsForBean+isMatch | **名称匹配**: setBeanNames("userService","order*")(即beanNames列表=通配符搭配)→getAdvicesAndAdvisorsForBean→`isMatch(beanName, mappedName)`→ant-style匹配→匹配→返回interceptorNames对应的advice | High |
| DefaultAdvisorAutoProxyCreator.java:39 | 自动代理 | 继承AbstractAdvisorAutoProxyCreator—遍历所有Advisor→Pointcut.matches(method,clazz)→true→eligible→isEligibleAdvisor→eligible Advisors→proxy | High |
| AspectJAwareAdvisorAutoProxyCreator.java:175行 | AspectJ排序 | 继承DefaultAdvisorAutoProxyCreator→额外提供@Aspect Advisor排序(使用AspectJ PartialOrder)→isEligibleAdvisor只保留最匹配的@AspectJ Advisor | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**P1 核心 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | AbstractAutoProxyCreator.wrapIfNecessary 模板方法 — shouldSkip→getAdvicesAndAdvisorsForBean→createProxy | 🔴 | **为什么**: 自动代理的核心——所有BPP(@Async/@Cacheable/@Transactional/@AspectJ)都是AbstractAutoProxyCreator的子类——wrapIfNecessary是"所有自动代理的统一入口" |
| P1-2 | 三种匹配策略: BeanName(name模式) vs DefaultAdvisor(Pointcut.matches) vs AspectJAwareAdvisor(@Aspect) | 🔴 | **为什么**: 不同BPP选择不同策略—@Async/@Cacheable用DefaultAdvisorAutoProxyCreator(通过Advisor.getPointcut)→@AspectJ用AspectJAwareAdvisorAutoProxyCreator(排序)—name模式是旧版 |

**1篇理由**: ~1077行/4文件—核心模板方法wrapIfNecessary(640行)+三种实现(437行)。1篇(~42行)。

**单篇结构**: §1 AbstractAutoProxyCreator.wrapIfNecessary 模板 + 三种子类匹配策略 → §2 典型BPP的auto-proxy链路(@Async/@Transactional/@AspectJ)
