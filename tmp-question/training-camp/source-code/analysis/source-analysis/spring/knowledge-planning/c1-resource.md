# C-1 Resource — 资源抽象 (Resource 接口 → 三实现 → ResourceLoader 工厂)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | Resource(212行)+AbstractResource(262行)+ClassPathResource(290行)+FileSystemResource(411行)+UrlResource(404行)+AbstractFileResolvingResource+DefaultResourceLoader(213行)+ResourceLoader(82行)
> 基线: 原始执行计划 0-1 — spring-core 第 0 层首域 — 一切配置文件/模板/静态资源的读取入口; 后续 Environment(@PropertySource)/消息/Web 静态资源都建立在本域之上

---

## §0.8

- 🟡 Working，1篇 — 接口契约(Resource: exists/getInputStream/getDescription + 继承 InputStreamSource) → 模板基类(AbstractResource: exists 兜底/默认异常/isReadable) → 三实现(ClassPathResource: classLoader/clazz 双路径; FileSystemResource: NIO Path; UrlResource: URLConnection) → 工厂(DefaultResourceLoader.getResource 五路分派: ProtocolResolver→/→classpath:→URL→path)
- 设计模式: [模式: 模板方法]—AbstractResource 固定 exists/getURL 默认行为, getInputStream 抽象; [模式: 策略模式]—ResourceLoader 按 location 前缀分派实现; [模式: 组合]—ProtocolResolver 扩展点

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Resource.java:56,64,210 | 接口 | **Resource 接口**: extends InputStreamSource(约 6 个方法) — exists/getURL/getURI/getFile/getDescription — "统一资源句柄"抽象, 不关心底层是 classpath 还是文件系统 | High |
| AbstractResource.java:48,56 | 类/exists() | **模板 exists()**: isFile()→getFile().exists() 优先; 否则 try getInputStream().close() — 用"能否打开流"兜底判定存在 — 子类可覆写(ClassPathResource 用 resolveURL) | High |
| AbstractResource.java:107,131 | getURL()/getFile() 默认异常 | **默认抛异常**: 未实现的能力抛 FileNotFoundException("cannot be resolved to URL/absolute file path") — 而非 UnsupportedOperationException — 调用方按 IOException 处理 | High |
| ClassPathResource.java:85,93 | 构造器 | **双构造**: (path, classLoader) / (path, clazz) — clazz 构造把类所在包转成资源路径前缀(L117) — classLoader null→ClassUtils.getDefaultClassLoader() | High |
| ClassPathResource.java:155,203 | exists()/getInputStream() | **classpath 语义**: exists=resolveURL()!=null(classLoader.getResource); getInputStream=classLoader.getResourceAsStream → null 抛 FileNotFoundException — 不依赖真实文件 | High |
| FileSystemResource.java:62,105,168,191 | 类/构造/exists/getInputStream | **NIO 实现**: 构造 StringUtils.cleanPath + Path.normalize; exists=Files.exists; getInputStream=Files.newInputStream — 基于 java.nio.file.Path | High |
| UrlResource.java:48,246 | 类/getInputStream | **URL 实现**: getInputStream=url.openConnection().getInputStream(); exists 走 AbstractFileResolvingResource(file:// 特判为 File / 其他 HEAD 请求) | High |
| DefaultResourceLoader.java:146 | getResource() | **五路分派**: ①L149 ProtocolResolver 逐个 resolve(扩展点, 如 dubbo:// 自定义协议) → ②L156 "/" 开头→getResourceByPath → ③L159 "classpath:" 前缀→ClassPathResource → ④L165 toURL 成功→FileUrlResource/UrlResource → ⑤L168 MalformedURLException→getResourceByPath | High |
| ResourceLoader.java:42,67 | 接口 | **加载器抽象**: getResource(String location)→Resource — CLASSPATH_URL_PREFIX="classpath:" 常量(L45) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+模板+3实现+工厂约 1900 行 — 骨架清晰: "统一抽象 → 默认实现 → 三具体策略 → 一个工厂分派"。1篇 (~45行) 按接口→模板→实现→工厂线性展开; 若分 2 篇则"哪个实现怎么选"与工厂分派逻辑割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | Resource 接口 + AbstractResource 模板 (exists 兜底/getURL/getFile 默认异常/getInputStream 抽象) | 🔴 | **为什么🔴**: 所有资源操作(读配置/模板/静态文件)的统一入口 — "句柄+能力默认实现"的模板设计是理解其余实现的基础 |
| P1-2 | ClassPathResource (classLoader/clazz 双构造 + getResourceAsStream + exists=resolveURL) | 🔴 | **为什么🔴**: 最常用的实现 (配置文件都在 classpath) — "无真实文件概念, 靠 classloader 解析"是关键认知 |
| P1-3 | DefaultResourceLoader.getResource 五路分派 (ProtocolResolver→/→classpath:→URL→path) | 🔴 | **为什么🔴**: location 字符串→Resource 的唯一入口 — 五路顺序是"特殊协议→最通用兜底"的经典分派 |
| P2-1 | FileSystemResource vs UrlResource (NIO Path vs URLConnection) | 🟡 | **为什么🟡**: 另两个实现 — 分别对应磁盘文件与远程/任意协议 — 与 classpath 实现对照 |
| P2-2 | ProtocolResolver 扩展点 (自定义协议前缀) | 🟡 | **为什么🟡**: ResourceLoader 的开放扩展 — 理解"框架预留口"的设计 |
| P3-1 | AbstractFileResolvingResource (file:// 特判 + HEAD 探测) | 🟢 | **为什么🟢**: 判断语义补充 — exists 的 HTTP 场景 (HEAD 请求) |
### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **抽象与模板** (Resource 接口 + AbstractResource 默认实现) | 🔴 | 统一抽象 + 默认行为 — 三实现共用骨架 |
| B | **三实现策略** (ClassPath/FileSystem/UrlResource) | 🔴 | 不同资源位置的具体策略 — ClassPath 最常用 |
| C | **工厂分派** (DefaultResourceLoader + ProtocolResolver) | 🔴 | location→Resource 的唯一入口 — 五路分派顺序即优先级 |
| D | **补充语义** (AbstractFileResolvingResource) | 🟢 | exists 的 HTTP 探测细节 |

> **Cluster A (§1)**: Resource 接口六方法 + AbstractResource 模板 (exists 兜底/默认异常/isReadable)
> **Cluster B (§2)**: 三实现 — ClassPathResource(双构造/取流)/FileSystemResource(NIO)/UrlResource(URLConnection) 对照
> **Cluster C (§3)**: DefaultResourceLoader.getResource 五路分派 + ProtocolResolver 扩展 + 与 @Value/@PropertySource 的衔接入口

→ 引出 0-2: 类型转换 — @Value("${...}") 读到的 String 如何转成目标类型 (ConversionService/Converter) — Resource 的 ResourceEditor 只是其一

(End of file - total 61 lines)
