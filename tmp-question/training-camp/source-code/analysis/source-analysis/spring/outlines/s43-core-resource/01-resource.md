# C-1 Resource — 资源抽象 (接口 → 模板 → 三实现 → 工厂)

> 依赖 — 原始计划 0-1 (spring-core 首域) | 🟡 Working | 6 KP | [模式: 模板方法 + 策略模式]

**读者处境**: `@Value("classpath:application.yml")`、`new ClassPathXmlApplicationContext("beans.xml")` — 字符串位置怎么变成可读的资源？classpath: 和 file: 和 http: 的区别在哪？为什么 Spring 不直接用 java.io.File？

### 1. Resource 接口 + AbstractResource 模板 — 统一资源句柄与默认行为

场景: 读配置文件 `classpath:config/app.properties` — Spring 需要"一个能表示任何位置资源的统一句柄": classpath、磁盘、HTTP 都能 exists() 判断存在、getInputStream() 读内容 — 但各自实现不同。

源码路径:
- `Resource.java:56,64,210` — **接口**: extends InputStreamSource — exists()/getURL()/getURI()/getFile()/getDescription() — "资源句柄"抽象, 不暴露底层来源
- `AbstractResource.java:48,56` — **模板 exists()**: isFile()→getFile().exists() 优先; 否则 try getInputStream().close() — 用"能否打开流"兜底判定存在 — 子类可覆写(ClassPathResource 改用 resolveURL)
- `AbstractResource.java:107,131` — **两处默认异常**: getURL()/getFile() 默认抛 `FileNotFoundException("cannot be resolved to URL/absolute file path")` — 用 IOException 表达"不支持该能力", 调用方统一按 IO 失败处理
- `AbstractResource.java:82` — **isReadable()**: 默认 return exists() — 语义"存在即可读"

关键设计: **Why 抽象 Resource 而非直接用 java.io.File/URL？** 位置类型无限(war 内/WEB-INF 下/网络), File/URL 各有局限; Resource 把"**句柄**"与"**读取方式**"解耦: 上层只依赖 exists/getInputStream/description, 具体来源由实现决定。**Why exists() 用"开流探测"兜底？** 无 file 语义的实现(如 classpath 无真实文件)无法用 File.exists — 退而求其次: 能打开流即存在。[模式: 模板方法 — 骨架默认 + 子类覆写]

数据流: `new ClassPathResource("config/app.properties")` → exists() → ClassPathResource L155 覆写: resolveURL()(classLoader.getResource) 非 null → true → getInputStream() → L203: classLoader.getResourceAsStream("config/app.properties") → InputStream → 读配置。FileSystemResource 则走模板: isFile()=true → getFile().exists()。

### 2. 三实现对照 — ClassPathResource / FileSystemResource / UrlResource

场景: `classpath:beans.xml`(jar 内) / `file:/etc/conf/app.yml`(外部配置) / `https://host/config.json`(远程) — 同一接口, 三种来源, 三种实现策略。

源码路径:
- `ClassPathResource.java:85,93,117` — **双构造**: (path, classLoader) / (path, clazz) — clazz 版把类包名转路径前缀(如 com.foo.App→com/foo/ + path); classLoader null→ClassUtils.getDefaultClassLoader()
- `ClassPathResource.java:155,203,215` — **classpath 语义**: exists=resolveURL()!=null; getInputStream=clazz/classLoader.getResourceAsStream → null 抛 FileNotFoundException("cannot be opened because it does not exist") — 全程无真实 File 概念
- `FileSystemResource.java:62,105,168,191` — **NIO 实现**: 构造 StringUtils.cleanPath+getPath().normalize(); exists=Files.exists(filePath); getInputStream=Files.newInputStream — 磁盘文件, 可写(WritableResource)
- `UrlResource.java:48,246` — **URL 实现**: getInputStream=url.openConnection().getInputStream(); file:// 走 AbstractFileResolvingResource 特判为 File

关键设计: **Why ClassPathResource 是"最常用"？** 配置文件随 jar 分发 — 无绝对路径概念, 依赖 classLoader 的 getResourceAsStream 定位; jar 内文件并非"真实 File", 所以 exists 靠 resolveURL 而非 File.exists — 这也是它在 AbstractFileResolvingResource 之外独立覆写 exists 的原因。[模式: 策略 — 同接口三实现]

数据流: `file:/etc/conf/app.yml` → FileSystemResource: exists→Files.exists(/etc/conf/app.yml)→true → getInputStream→Files.newInputStream→读。`https://host/config.json` → UrlResource: exists→AbstractFileResolvingResource L49: 非 file URL→URLConnection 发 HEAD 请求→200→true → getInputStream→openConnection().getInputStream()。`classpath:config/app.properties`(上面已述)→classLoader 解析。

### 3. DefaultResourceLoader 工厂 — location 字符串的五路分派

场景: `@Value("classpath:config/app.properties") Resource r` 注入时 — 字符串怎么变成 Resource？Spring 的 ResourceEditor 调用 ResourceLoader.getResource(location)。

源码路径:
- `DefaultResourceLoader.java:146` — **getResource(location)**: ①L149-153 ProtocolResolver 逐个 resolve(自定义协议扩展点, 非 null 即返回) → ②L156-157 "/" 开头→getResourceByPath → ③L159-161 `classpath:` 前缀→new ClassPathResource(substring, classLoader) → ④L165 toURL 成功→file 协议?FileUrlResource : UrlResource → ⑤L168-170 MalformedURLException→getResourceByPath(相对路径)
- `ResourceLoader.java:42,45,67` — **接口**: getResource(String)→Resource; CLASSPATH_URL_PREFIX="classpath:" 常量

关键设计: **Why 五路分派顺序如此？** 从"最特殊"到"最通用": 自定义协议(用户扩展) → 绝对路径 / 显式 classpath: (无歧义) → 合法 URL(file/http) → 其余按相对路径处理 — 分派即优先级, 后路兜前路。**Why ProtocolResolver 在首位？** 让框架外的协议(如 dubbo:///mybatis 映射)无需改 Spring 核心代码即可接入 — 与 W 系列 bean 后处理器同一"开闭原则"思路。[模式: 工厂方法 + 责任链(协议解析)]

数据流: "@Value(\"classpath:config/app.properties\")" → ResourceEditor→DefaultResourceLoader.getResource → 无 ProtocolResolver → 非 "/" 开头 → 是 classpath: → L160 new ClassPathResource("config/app.properties", classLoader) → Resource。若注入 "file:/etc/app.yml" → ④ toURL→isFileURL→FileUrlResource(file:// 可写 URL 资源)。若 "/WEB-INF/web.xml"(Servlet 容器)→ ② getResourceByPath(ServletContextResource)。

→ 引出 0-2: 类型转换 — Resource 只是 @Value 注入的一种类型 — String→任意类型(含 Resource)的统一转换链: ConversionService/Converter — ResourceEditor 只是注册进去的一个边角。
