# 域 AR-5: OGNL 表达式 — 知识规划

> 源码路径: core/command/express/(Express.java + ExpressFactory.java + OgnlExpress.java + DefaultMemberAccess.java + ArthasObjectPropertyAccessor.java + ClassLoaderClassResolver.java + CustomClassResolver.java) + core/advisor/AdviceListenerAdapter.java + core/command/klass100/OgnlCommand.java + core/util/Constants.java
> 源码量: ~10 文件,核心 ~450 行
> 提取日期: 2026-08-10(v2 深审补充: 机制 8→10)
> 前置域: AR-0 篇 6(ognl 使用)/AR-2 篇 3(条件过滤执行点)/AR-2 篇 4(watch 条件)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| ExpressFactory.java:19-39 | **ThreadLocal 弱引用池**: `ThreadLocal<WeakReference<Express>>`(:19)——注释明确: 不能强引用(Express 由 ArthasClassLoader 加载,stop 后业务线程仍持有 ThreadLocalMap → 类加载器泄漏);`threadLocalExpress`(:23-31): 取引用 → 空则 new → `reset().bind(object)` 复用;`unpooledExpress(classloader)`(:33-39): 每次新建(tt -w/ognl 命令用,带 ClassLoaderClassResolver) | High |
| OgnlExpress.java:18-47 | **OGNL 封装**: `MEMBER_ACCESS = new DefaultMemberAccess(true)`(:19,全放开);`ArthasObjectPropertyAccessor`(:21,替换 Object.class 的属性访问器);构造 `OgnlContext(MEMBER_ACCESS, classResolver, null, null)`(:28);`get(express)` = `Ognl.getValue(express, context, bindObject)`(:34-38);`is` = get 且 Boolean 判真(:41-44);`bind(Object)` 设绑定对象(:46-49);`reset()` clear context | High |
| DefaultMemberAccess.java:20- | **私有访问控制**: `MemberAccess` 实现,allowPrivate/Protected/PackageProtected 三开关;`new DefaultMemberAccess(true)` 全开——`canAccess` 返回 true 无需反射 setAccessible | High |
| ArthasObjectPropertyAccessor.java:13- | **属性访问器增强**: 继承 OGNL 的 ObjectPropertyAccessor——支持**属性名含特殊字符**(如 map 的 `get(key)` 样式)与 getter 容错 | Medium |
| ClassLoaderClassResolver.java:12-28 | **类解析器**: `classLoader.loadClass(className)`——ognl 命令用指定 CL 解析类(多版本场景),`-c` 参数对应 | High |
| AdviceListenerAdapter.java:132-139 | **条件与输出执行点**: `isConditionMet(conditionExpress, advice, cost)`(:132-135)= `StringUtils.isEmpty(expr) || ExpressFactory.threadLocalExpress(advice).bind(Constants.COST_VARIABLE, cost).is(expr)`;`getExpressionResult`(:137-139)= `.get(expr)` | High |
| Constants.java:40 | **COST_VARIABLE**: `cost`——表达式里可用的计时变量 | Medium |
| OgnlCommand.java:31-40 | **ognl 命令**: `-x` 展开深度/`-c` classLoaderHash/`-e` 多条表达式;`@类@静态成员` / `@类@方法()` 语法 | High |
| Express.java | **接口契约**: get/is/bind(Object)/bind(String,Object)/reset——两种绑定(整体对象 vs 命名变量) | Medium |

| CustomClassResolver.java:15-44 | **默认类解析器(单例+缓存)**: `customClassResolver` 单例(:15);`ConcurrentHashMap` 缓存类名→Class(:17);`classForName`(:23-44): 先 **TCCL** 加载 → 失败 `Class.forName` → **类名无点号时尝试 `java.lang.` 前缀**(:37-39,如 `String`→`java.lang.String`)——OgnlExpress 默认解析器 | High |
| ArthasObjectPropertyAccessor.java:13-17 | **strict 写保护**: 只重写 `setPossibleProperty`——`GlobalOptions.strict` 时抛 `IllegalAccessError(STRICT_MESSAGE)`——**"只读不写"纪律的源码实现**(AR-0 篇 6 的安全规范在此落地) | High |

*10 个知识点(v1 8 → 补充 2)*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| 表达式执行管线(工厂→OgnlExpress→Ognl.getValue) | ExpressFactory, OgnlExpress, Express, Ognl.getValue | watch 条件/tt 搜索/ognl 命令共用 |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| 类加载器感知 | ClassLoaderClassResolver, CustomClassResolver, unpooledExpress |
| 私有访问与属性访问 | DefaultMemberAccess, ArthasObjectPropertyAccessor(strict 写保护) |
| 类解析 | CustomClassResolver(TCCL+java.lang 兜底), ClassLoaderClassResolver(-c 指定), unpooledExpress |
| 条件过滤执行点 | AdviceListenerAdapter, Constants |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| ognl 命令 | OgnlCommand |
| ThreadLocal 弱引用池 | ExpressFactory(单文件核心) |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| ThreadLocal 弱引用池(防 ClassLoader 泄漏) | "为什么用 WeakReference"——arthas 最经典的泄漏防御;面试加分 |
| OgnlExpress 封装与 isConditionMet 执行点 | "watch 的条件过滤在哪执行"——AR-2 全链的汇点 |
| DefaultMemberAccess 全放开 | "为什么 arthas 能读私有成员"——安全边界讨论 |
| 类加载器感知(-c/ClassResolver) | 多版本类场景;与 AR-0 篇 6 的 classLoaderHash 对应;TCCL 兜底链 |
| strict 写保护(ArthasObjectPropertyAccessor) | 面试"ognl 能改线上数据吗"——strict 模式的安全闸门 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| Express 接口契约 | 简单 |
| ognl 命令参数 | AR-0 已覆盖使用 |
| ArthasObjectPropertyAccessor | 边角增强 |
| 接口契约(Express) | 为什么: get/is/bind/reset 四方法 |
| 属性访问器增强 | 为什么: 容错 getter |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| cost 变量绑定 | 一行代码 |
| unpooledExpress vs threadLocal | 理解差异即可 |

---

| COST_VARIABLE | 为什么: 一行常量 |## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 表达式从哪来(工厂)→ 怎么执行(引擎)→ 在哪生效(命令与条件)。

### 依赖图

```
01 表达式引擎                       ← 无前置
  └─ 02 表达式的生效点              ← 依赖 01 (isConditionMet 调引擎)
```
### 教学顺序

01 表达式引擎(怎么求值)→ 02 生效点(在哪被调用)
### 文章拆分 (2 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-express-engine.md | 表达式引擎 | ExpressFactory(弱引用池)/OgnlExpress/DefaultMemberAccess/**CustomClassResolver 兜底链**/**strict 写保护** |
| 2 | 02-express-usage.md | 表达式的生效点 | isConditionMet/getExpressionResult/watch 条件/tt 搜索/ognl 命令/cost |

### 文章内机制顺序

```
01: ExpressFactory 弱引用池 → OgnlExpress 封装 → DefaultMemberAccess/CustomClassResolver → strict 写保护
02: isConditionMet(判) → getExpressionResult(取) → Advice 根对象 → ognl 命令/tt 搜索 → 全链路
```

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "为什么 stop 后 arthas 不会内存泄漏?" | WeakReference 打断 Thread→Express 强引用链 |
| "为什么表达式能读私有字段?" | DefaultMemberAccess(true) 全放开 |
| "params[0]>100 在哪一行代码被求值?" | AdviceListenerAdapter.isConditionMet |
