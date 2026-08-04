# URL 协议扩展 -- JDK 一个延续了二十五年的封闭设计，以及业界怎么撬开它

> 主题：为什么 `new URL("classpath:foo.properties")` 在原生 JDK 里不可行，URL/URLConnection 这个「过度抽象」是怎么来的，以及「全局可变单例的二次安装」「字符串前缀注册表」「封闭语法解析的外层改写」三个永恒原理。microsphere-net 只是业界抗争这条封闭设计的一个实例。
> 关联：本文与 [§4 SPI+Prioritized](./04-spi-prioritized.md) 同属「SPI 扩展点」主题，但破解的是 JDK 设计层面的硬约束，而非应用层 SPI。

---

## 一、URL/URLConnection:1996 年的「统一抽象」为什么成了败笔

要理解「为什么扩展 URL 协议这么难」，得先理解 URL 这套设计本身的历史包袱。

### 1.1 「万物皆 URL」的 1996 年设计哲学

`java.net.URL` 和 `java.net.URLConnection` 是 JDK 1.0（1996）的产物。那个年代的 Java 抱着一个宏大愿景：**用 URL 统一所有资源访问**--HTTP、FTP、file、jar、甚至未来的未知协议，都通过 `new URL("xxx://...").openConnection()` 拿到一个 `URLConnection`，再 `getInputStream` 读数据。

```java
// 1996 年的理想写法
URLConnection conn = new URL("http://example.com/foo").openConnection();
InputStream is = conn.getInputStream();   // 统一接口
```

这套设计的核心是**「协议 = URLStreamHandler」**：每个协议对应一个 `URLStreamHandler` 子类，负责解析该协议的 URL 语法、创建对应的 `URLConnection`。JDK 内置 `http`/`https`/`ftp`/`file`/`jar` 等几个 handler。

### 1.2 为什么这个抽象失败了

**泄漏的抽象**：`URLConnection` 试图做「所有协议的统一接口」，但 HTTP 的特性（请求方法、请求头、认证、分块传输、cookie、重定向）远超 `URLConnection` 基类能表达的范围。结果是 `HttpURLConnection` 加了几十个 HTTP 专有方法，调用方必须 `if (conn instanceof HttpURLConnection)` 强转--抽象彻底泄漏。

**`URL.equals` 做 DNS 查询**：`URL.equals`/`hashCode` 会比较主机的 IP 地址，意味着**调一次 `equals` 可能触发一次 DNS 解析**，在断网或 DNS 慢时会卡住。这是个著名的设计缺陷，导致 `URL` 不该被用作 `Map` 的 key（应该用 `URI`）。这个坑至今没修--因为修了会破坏向后兼容。

**handler 全局缓存**：JDK 把已创建的 `URLStreamHandler` 缓存在 `URL.handlers`（一个 `ConcurrentHashMap`）--一旦某协议的 handler 被创建过，后续即使你换了 factory，缓存仍然命中旧的。这给「运行时换 handler」埋了雷。

**URLConnection 不可重用**：一个 `URLConnection` 只能 `connect` 一次，状态机式的设计在连接池、重试场景下极其别扭。

### 1.3 协议扩展的两个入口都被「半封死」

JDK 给了两个扩展协议的入口，但都带着硬约束：

**入口一：`java.protocol.handler.pkgs` 系统属性（命名约定式）**

协议 `xxx` 的 handler 类必须是 `某个包前缀.xxx.Handler`，且继承 `URLStreamHandler`。三道暗坑：

1. 只支持单级协议--`jdbc:mysql://...` 里 `jdbc:mysql` 含冒号，JDK 的 `parseURL` 把整段当协议名，找 `jdbc:mysql.Handler` 类时冒号非法直接抛 `MalformedURLException`。
2. 多个包前缀无优先级，谁先生成有效类谁胜出--「谁先」取决于类加载顺序，跨环境不可控。
3. `Handler` 类必须有无参构造--无法在构造时注入状态（如不同的 `ClassLoader`）。

**入口二：`URL.setURLStreamHandlerFactory()`（工厂式）**

```java
URL.setURLStreamHandlerFactory(factory);   // 全局只允许调用一次
```

`URL` 内部静态字段 `factory` 第一次 set 后就锁死，第二次调用抛 `Error: factory already defined`。**全局只能装一次**--如果 Tomcat、Spring Boot、JBoss 都想装自己的 factory，后装的全部失败。这是「先发制人」式 API，谁先启动谁赢，根本没法协作。

### 1.4 跨语言对比:业界怎么躲这个坑

| 语言 | URL 解析与协议处理 | 是否耦合 |
|---|---|---|
| Java (JDK) | `URL` + `URLStreamHandler` + `URLConnection` 三者强耦合 | 协议处理绑死在 URL 类里 |
| Go | `net/url` 只做解析（`url.URL` 是 struct），网络访问走 `net/http.Client` 等独立模块 | 解析与访问完全分离 |
| Python | `urllib.parse` 只解析，`urllib.request` 有 handlers 但可独立替换 | 解析与访问分离，handler 可热插 |
| Node.js | `URL` 类（WHATWG）只解析，`http`/`https`/`fs` 各自独立 | 完全分离 |

**只有 Java 把「URL 解析」和「协议访问」焊死在一起**。Go/Python/Node 都把 URL 当纯数据结构解析，访问资源用独立模块--这样协议扩展就不是 URL 类的事，根本不存在「URL 协议扩展」难题。Java 的痛点是历史选择：1996 年选了「统一抽象」，三十年后改不动了。

### 1.5 JDK 9 模块化为什么没修这个坑

JDK 9（2017）模块化时，业界期待 URL 体系被重设计--结果只加了一个 `java.net.spi.URLStreamHandlerProvider`（模块声明式 SPI），没动 `URL`/`URLConnection` 本身。原因：**改不动**。`URL` 是 JDK 最被依赖的类之一，`new URL(...)` 散布在无数老代码里，改签名/语义会引发兼容性海啸。JDK 选择「加新入口（Provider SPI）但不修老的」--典型的「向后兼容优先于设计正确」。

**新入口的局限**：`URLStreamHandlerProvider` 要求 `module-info.java` 里 `provides ... with ...` 声明--**整个应用必须模块化**。对绝大多数还在 classpath 模式运行的 Spring Boot 应用不可行。所以这个「正确的新入口」实际用不上，老入口（`handler.pkgs` + `setURLStreamHandlerFactory`）还在扛大梁。

这就是 microsphere-net 这套类要撬的局--在不模块化、不改老代码的前提下，让 URL 协议处理变成可组合、可热插拔、支持子协议、SPI 自动发现。下面三个小节讲透背后的三个永恒原理。


## 二、永恒原理一:全局可变单例的「二次安装」难题

`URL.setURLStreamHandlerFactory` 的「只能装一次」不是孤例。JDK 里有一类 API 共享同一个设计模式--**全局可变单例**：一个进程内某个全局槽位只能被设置一次。

### 2.1 JDK 里的全局可变单例家族

| API | 槽位 | 设置次数限制 | 失败行为 |
|---|---|---|---|
| `URL.setURLStreamHandlerFactory` | `URL.factory` 静态字段 | 一次 | 抛 `Error` |
| `System.setSecurityManager` | `System.security` | JDK 17 前可多次，JDK 17+ 弃用 | -- |
| `Thread.setDefaultUncaughtExceptionHandler` | `Thread.defaultUncaughtExceptionHandler` | 多次（无限制） | -- |
| `System.setOut` / `setIn` / `setErr` | `System.out/in/err` | 多次（无限制） | -- |
| `JFrame.setDefaultLookAndFeelDecorated` | 静态字段 | 多次 | -- |

**为什么有些允许多次、有些只允许一次？** 「只允许一次」的 API 背后假设是「设置后系统进入稳定状态，再改会引发不可预测行为」。`URL.factory` 一旦设置，JDK 内部已经基于它创建了 handler、建立了缓存--再换会让缓存和实际 factory 不一致。这是「为不可变性而设计」的初衷，但忽略了「框架协作需要依次安装」这个真实需求。

### 2.2 「想装第二次」的三种解法

**解法一：JDK 默认（不让你装第二次）**

直接抛错。优点：安全。缺点：多个框架无法协作--先启动的赢，后启动的死。

**解法二：Composite 包装**

把全局槽位设成一个 `Composite`，它的内部维护一个 `List`。后续任何想注册的都往 List 里 `add`，不再触发全局 `set`。这是「**让单例变成聚合体**」的标准做法。

```java
// 抽象示意
Composite factory = new Composite();
URL.setURLStreamHandlerFactory(factory);   // 只这一次真 set
// 后续:
factory.add(otherFactory1);                 // 不再触发 set
factory.add(otherFactory2);
```

`Composite` 的 `createURLStreamHandler` 内部遍历 List，第一个返回非 null 的胜出--责任链模式（和 ArtifactDetector 的责任链同构，参见 [§1](./01-artifact-detection.md)）。

**解法三：反射清空再装**

如果全局槽位已被别人占（且别人不是 Composite），用反射把槽位字段强行设回 null，于是下次 `set` 又能成功。这是「**绕过 API 约束，直接改内部状态**」的激进做法。

microsphere 的组合是「**解法二 + 解法三**」。关键在「安装时槽位处于什么状态」决定走哪条路径--这是这个原理的精髓所在，需要按三种 case 逐一展开。

#### Case 1:槽位空（没人装过 factory）

```
读 URL.factory -> null
-> newComposite = new CompositeURLStreamHandlerFactory()
-> URL.setURLStreamHandlerFactory(newComposite)   // 唯一一次真 set,成功
-> newComposite.add(myFactory)
```

**为什么这条路最干净**：JDK 的 `set` 只在 `factory == null` 时允许，这是唯一无需反射的路径。`set` 成功后全局槽位指向 Composite，后续都走 `add`，不再触发 `set`。

**边界**：非线程安全。如果线程 A 读到 `null` 后、`set` 前，线程 B 抢先 `set` 了自己的 factory--A 的 `set` 会抛 `Error`。microsphere 没加锁--它假设 factory 安装在启动单线程阶段。这是个隐式约定。

#### Case 2:槽位已被普通 factory 占（别人装了，且不是 Composite）

这是最复杂的 case，也是「反射清空」真正派上用场的地方：

```
读 URL.factory -> oldFactory (非 null,非 Composite,比如 Tomcat 装的)
-> newComposite = new CompositeURLStreamHandlerFactory()
-> newComposite.add(oldFactory)            // ① 先把旧的搬进去,不能丢!
-> clearURLStreamHandlerFactory()          // ② 反射把 URL.factory 设回 null
-> URL.setURLStreamHandlerFactory(newComposite)  // ③ 现在 factory==null,set 成功
-> newComposite.add(myFactory)             // ④ 把自己的也加进去
```

**为什么步骤①必须在②之前**：`clearURLStreamHandlerFactory` 把 `URL.factory` 设回 null 的瞬间，全局槽位空了--如果此时还没把 oldFactory 搬进 newComposite，oldFactory 就**永久丢失**了（没有别的地方持有它的引用）。其他框架（如 Tomcat）的协议处理会全部失效。所以必须「先搬运、再清空」。

**为什么步骤②必须用反射**：JDK 的 `set` 内部判断 `if (factory != null) throw Error`。要让 `set` 再次成功，必须让 `factory` 变回 null。但 JDK 没提供「unset」API--只能反射直接写 `URL.factory` 私有静态字段。这是「绕过 API 约束，直接改内部状态」。

**边界一（模块系统）**：反射写 `URL.factory`（`java.net.URL` 的私有字段）在 JDK 9 受模块系统限制，JDK 16+（`--illegal-access=deny` 默认）直接抛 `InaccessibleObjectException`。必须加 `--add-opens java.base/java.net=ALL-UNNAMED`。microsphere 不检测这个，失败时错误信息不直观。

**边界二（handler 缓存不清）**：即使反射清空了 `URL.factory`，`URL.handlers`（另一个 `ConcurrentHashMap` 缓存）仍然存在。之前 `new URL("http://...")` 已经把 `http` 的 handler 缓存了，换 factory 后 `http` 协议仍走旧 handler--缓存命中优先于 factory 创建。**这意味着「换 factory 对已用过的协议不生效」**。规避只能靠「在 URL 类被任何代码使用前就完成 factory 安装」。这是「全局可变单例二次安装」解法的固有约束--操作必须早于任何触发缓存的使用。

**边界三（窗口期不一致）**：步骤②清空到步骤③ set 之间有个极短的「factory==null 窗口」。这期间任何线程调 `new URL("xxx://...")` 会触发 JDK 创建新的默认 factory（JDK 内部有 fallback 逻辑），导致 newComposite 的 set 失败。microsphere 没处理这个竞态--它假设安装阶段没有并发 URL 创建。

#### Case 3:槽位已是 Composite（之前装过 microsphere 或别的 Composite）

```
读 URL.factory -> oldFactory (instanceof CompositeURLStreamHandlerFactory)
-> 直接 oldFactory.add(myFactory)           // 复用,不再 set!
```

**为什么这条路无需 set**：Composite 的核心价值就是「内部 List 可变，add 不触发全局 set」。这是 Case 1 一次 set 换来的长期收益--之后所有安装都走 add，零反射、零 set、零 Error 风险。

**边界**：`CompositeURLStreamHandlerFactory` 内部是 `LinkedList`，`add` 后要 `sort(factories, COMPARATOR)` 重新排序。`LinkedList.add` + `Collections.sort` 非线程安全--并发 add 可能丢数据或排序错乱。microsphere 没加同步，仍依赖「安装阶段单线程」约定。

#### 三种 case 的统一逻辑

把三种 case 串起来看，原理是：**「让全局槽位尽快变成 Composite，之后所有安装都走 Composite.add 不再触碰全局 set」**。Case 1 是理想路径（直接装 Composite）；Case 2 是补救路径（反射清空把非 Composite 强行变 Composite）；Case 3 是稳态路径（Composite 已就位，纯 add）。

**这套组合是「全局可变单例二次安装」的通用解法**，不止 URL--任何「全局单例只允许 set 一次」的 API 都能这么撬。Spring 的 `BeanFactory` 替换、Tomcat 的 `ServletContext` 替换都用了类似套路：先聚合、再清空、最后 set 一次。三个 case 对应「槽位三种可能状态」的穷举，缺一不可--只处理 Case 1 会在别人先装时崩；只处理 Case 2 会反复反射清空（性能差且危险）；只处理 Case 3 会在首次安装时 NPE。

### 2.3 反射操作 JDK 内部的代价

反射清空 `URL.factory` 写的是 `java.net.URL` 的私有静态字段。JDK 9 模块系统对反射加了限制--`java.base` 模块默认不开放 `URL` 的非 public 字段。JDK 16+（`--illegal-access=deny` 成默认）上，对 `URL.factory` 的反射写入会抛 `InaccessibleObjectException`。

**生产环境的应对**：必须加 JVM 参数 `--add-opens java.base/java.net=ALL-UNNAMED`。microsphere 没在代码里做这层检测--它假设部署文档写清楚了。这是「反射操作 JDK 内部」类方案的共同代价（参见 BannedArtifact 反射 `URLClassPath` 的同类问题，[§2](./02-banned-artifact-isolation.md)）。

**更隐蔽的代价：handler 缓存不被清**。即使反射清空了 `URL.factory`，`URL.handlers` 这个 `ConcurrentHashMap` 缓存仍然存在--之前 `new URL("http://...")` 已经把 `http` 的 handler 缓存了，换 factory 后 `http` 协议仍走旧 handler。**规避**：在最早启动阶段（`URL` 还没被使用过）就完成 factory 安装。这是「全局可变单例二次安装」解法的固有约束--操作必须早于任何触发缓存的使用。


## 三、永恒原理二:字符串前缀 -> 处理器 的注册表问题

把「URL 协议处理」抽象一下，本质是个注册表问题：**「字符串前缀（协议名） -> 处理器」的查找表**。这个抽象在 Java 生态里反复出现。

### 3.1 同构案例

| 场景 | 前缀 | 处理器 | 查找方式 |
|---|---|---|---|
| URL 协议 | `http`/`ftp`/`classpath` | `URLStreamHandler` | 精确匹配协议字符串 |
| Servlet | `/api/*` URL pattern | `Servlet` | 前缀匹配 + 精确匹配 + 通配符 |
| Spring MVC | `@RequestMapping("/foo")` | `HandlerMethod` | 路径模式匹配 |
| OSGi | service interface | service instance | 接口类型匹配 + ranking |
| SPI (ServiceLoader) | 接口类型 | 实现实例 | 接口类型精确匹配 |
| DNS | 域名 | IP 地址 | 最长后缀匹配 |

**它们共享同一个本质**：把一个标识符（字符串或类型）映射到一个处理器。区别在「匹配算法」--精确匹配、前缀匹配、最长后缀匹配、模式匹配。

#### 为什么不同场景用不同匹配算法

匹配算法的选择不是任意的，是由「标识符的结构」和「注册表的使用模式」共同决定的：

| 场景 | 标识符结构 | 使用模式 | 匹配算法 | 为什么这么选 |
|---|---|---|---|---|
| URL 协议 | 扁平字符串（`http`/`ftp`） | 注册时固定，查询时完全已知 | 精确匹配 | 协议名是原子标识，无需模糊--`http` 就是 `http`，不存在「类 http」 |
| Servlet URL pattern | 层级路径（`/api/users`） | 要覆盖「一类路径」（`/api/*`） | 前缀 + 通配符 | 路径有层级结构，`/api/*` 表示「api 下所有」--前缀匹配天然适合层级 |
| Spring MVC | 路径 + 模式（`/api/{id}`） | 要支持路径变量、正则 | 模式匹配 | 同一个路径要匹配多种实际值（`/api/123`、`/api/456`），必须模式化 |
| DNS | 层级域名（`www.example.com`） | 查询时要找「最具体的区域」 | 最长后缀匹配 | 域名是层级委托（`.com` -> `example.com` -> `www.example.com`），最长后缀 = 最具体区域 |
| SPI | 接口类型 | 注册时固定，查询时完全已知 | 精确匹配 | 类型是原子标识，和 URL 协议同构 |

**核心规律**：标识符是「原子」（URL 协议、接口类型）-> 精确匹配；标识符是「层级」（路径、域名）-> 前缀/最长后缀匹配；标识符要「泛化」（路径变量）-> 模式匹配。**匹配算法 = f(标识符结构)**。

#### URL 协议的「精确匹配」为什么不够用

URL 协议用精确匹配（`http` 字符串完全相等），这在 1996 年足够--那时协议就那么几个（http/ftp/file）。但现代需求出现了「同主协议、不同子类型」的场景：

- `jdbc:mysql` vs `jdbc:postgresql` -- 同是 jdbc，不同数据库
- `spring:beans` vs `spring:context` -- 同是 spring，不同资源
- `classpath:foo` vs `classpath*:foo`（Spring） -- 同是 classpath，不同扫描范围

这些场景下，「协议」不再是原子标识，而是「主协议 + 子协议」的层级结构。**精确匹配只认 `jdbc`，不认 `jdbc:mysql`**--这就是 §3.2 要讲的「注册表键定义和实际用法不匹配」。

URL 协议是其中最简单的--**精确匹配协议字符串**。`new URL("http://...")` 时 JDK 拿 `"http"` 这个字符串去注册表查 handler。问题来了：**「协议字符串」本身的定义是什么？**

### 3.2 URL 协议字符串的「封闭定义」

JDK 的 `URL.parseURL` 把协议段定义成「第一个冒号之前、不含特殊字符的部分」。这个定义是封闭的--**不允许协议段含冒号**。所以 `jdbc:mysql://...` 里 JDK 把 `jdbc:mysql` 整段当协议名去查 handler，而 handler 注册表里只有 `jdbc` 这一项，查不到 `jdbc:mysql` --失败。

这是「**注册表的键定义和实际用法不匹配**」的问题。注册表只接受单级键（`jdbc`），但用户想用多级键（`jdbc:mysql`）。

### 3.3 多级键的两种解法

**解法一:扩展键定义（改注册表）**

让注册表接受多级键--`jdbc:mysql` 作为完整键查 handler。但这需要改 JDK 的 `parseURL`，而 JDK 改不动（§1.5）。所以这条路在 JDK 内走不通。

**解法二:键降级（把多级键拆成单级键 + 子键）**

保持注册表只接受单级键，但把多级键拆解--主键 `jdbc` 进注册表查 handler，子键 `mysql` 用其他通道传给 handler。这是 microsphere 的选择，也是 JDBC 4.0 之前 `DriverManager` 的思路（`jdbc:mysql://...` 实际由 mysql 驱动注册的 `Driver` 处理，`Driver.acceptsURL` 自己解析子协议）。

**子键怎么传给 handler？** 三个选项：

| 通道 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| query 参数 | `jdbc://host/db?_sp=mysql` | 简单 | query 语义是「查询条件」，可能和用户原 query 冲突 |
| fragment | `jdbc://host/db#mysql` | 不影响 path/query | fragment 语义是「锚点」，且只能存一个 |
| matrix 参数 | `jdbc://host/db;_sp=mysql` | 语义上是「资源标识的一部分」，可多个 | matrix 参数 JDK 解析时会被丢进 path，需自定义解析 |

microsphere 选 matrix 参数--**REST 圈的矩阵参数语义正是「资源标识的一部分」**，子协议 `mysql` 标识「这是哪类资源」（mysql 资源 / postgres 资源）。这是对 URL 语义的精细考究，不是随便选的。

### 3.4 子协议责任链:多级值的多策略分发

主协议 `jdbc` 找到 handler 后，子协议 `mysql` 可能还要分发给不同的 `SubProtocolURLConnectionFactory`（如 mysql 工厂、postgres 工厂）。这又是「字符串前缀 -> 处理器」注册表--但这次键是子协议链，且可能有多个 factory 同时支持。

microsphere 用责任链：factory 列表按 `Prioritized` 排序，第一个 `supports() == true` 且 `create() != null` 的胜出。和 ArtifactDetector 的责任链（[§1](./01-artifact-detection.md)）、Converter 的 `findConverter`（[§3](./03-convert-framework.md)）同构--都是「排序后依次尝试直到第一个成功」。

**这是 microsphere 的通用模式**：注册表 + 优先级排序 + 责任链。SPI+Prioritized（[§4](./04-spi-prioritized.md)）是它的元机制，URL 协议扩展、Artifact 检测、Converter 查找、Logger factory 选定都是它的具体应用。区别在「选定即固定」（Logger）vs「责任链重试」（其他）。


## 四、永恒原理三:封闭语法解析的「外层改写」策略

§3.2 说 JDK 的 `parseURL` 是封闭算法（协议段不含冒号）。这种「**核心算法封闭不可改**」的场景在软件设计里很常见。解法是「**外层改写输入**」--不改核心算法，改喂给它的数据。

### 4.1 外层改写的普适模式

```
原始输入 -> [外层改写] -> 改写后输入 -> [封闭核心算法] -> 结果
```

只要改写后的输入满足核心算法的约束，核心算法就能正常工作。microsphere 的 `parseURL` 重写就是这个模式。下面用一段最小化的具体变换演示这个原理怎么运作--不是贴源码，是构造最能说明原理的例子。

#### 具体变换:从 `jdbc:mysql://...` 到 `jdbc://...;_sp=mysql`

原始 URL 字符串：

```
jdbc:mysql://localhost:3307/mydb?charset=UTF-8#top
   ↑______↑
   协议段含冒号,JDK parseURL 不认识,会抛 MalformedURLException
```

外层改写分四步：

**Step 1:定位子协议段**

外层先在 spec 里找 `://` 的位置，确定「协议段」和「主机段」的分界。`://` 之前（去掉主协议 `jdbc:` 后）的部分就是子协议段：

```
spec = "jdbc:mysql://localhost:3307/mydb?charset=UTF-8#top"
                                  ↑ "://" 在这里
subProtocol 段 = "mysql"          ← "jdbc:" 之后、"://" 之前
subProtocols = split("mysql", ':') = ["mysql"]
// 多级时: "mysql:postgres" -> ["mysql","postgres"]
```

**Step 2:构造 matrix 字符串**

把子协议数组编码成 matrix 参数（RFC 3986 的 path segment 语法 `;key=value`，可重复）：

```
matrix = ";_sp=mysql"
// 多级时: ";_sp=mysql;_sp=postgres"  (每个子协议一个 _sp)
```

**Step 3:拼新 spec,把 matrix 插到 path 后、query 前**

```
suffix = "://localhost:3307/mydb?charset=UTF-8#top"   ← "://" 之后的部分
newSpec = "jdbc" + suffix
        = "jdbc://localhost:3307/mydb?charset=UTF-8#top"
// 在 query "?" 前插入 matrix:
newSpec = "jdbc://localhost:3307/mydb;_sp=mysql?charset=UTF-8#top"
                              ↑________↑
                              matrix 参数,嵌在 path 段内
```

**关键细节：为什么插在 query 前？** matrix 参数按 RFC 3986 属于 path segment 的一部分（`/mydb;_sp=mysql` 是一个 path segment），必须出现在 query 之前。如果插到 query 后（`?charset=UTF-8;_sp=mysql`），`_sp` 会被 JDK 当成 query 参数而非 path 的一部分，语义就错了。

**Step 4:交给 JDK 的封闭算法解析**

```
super.parseURL(url, newSpec, ...)
-> URL(protocol="jdbc", host="localhost", port=3307,
       path="/mydb;_sp=mysql",       ← matrix 混在 path 里,JDK 不区分
       query="charset=UTF-8", ref="top")
```

JDK 的 `parseURL` 看到「协议段不含冒号」（`jdbc`），正常解析。子协议 `mysql` 被藏进了 path 的 matrix 参数里。之后 microsphere 用 `resolveSubProtocols` 从 path 里把 `;_sp=mysql` 解析回 `["mysql"]`--外层解析。

#### 为什么选 matrix 而非 query/fragment

这是对 URL 语义的精细考究，不是随便选的：

| 通道 | RFC 3986 语义 | 适不适合存子协议 |
|---|---|---|
| matrix 参数（`;_sp=mysql`） | path segment 的一部分，代表「资源标识的子分类」 | ✅ 子协议正是「这是哪类资源」（mysql 资源 / postgres 资源） |
| query 参数（`?_sp=mysql`） | 查询条件，代表「对资源的筛选」 | ❌ 子协议不是筛选条件，是资源身份的一部分 |
| fragment（`#mysql`） | 锚点，代表「资源内的位置」 | ❌ 且只能存一个，无法表达多级子协议 |

REST 圈用 matrix 参数表达「资源子分类」是有传统的--`/users;role=admin/123` 表示「在 admin 角色的用户集合里找 123」。microsphere 的 `_sp=mysql` 是同一语义：在 mysql 子协议的资源集合里找。这是比 query/fragment 都更准确的语义选择。

#### 外层改写的代价:matrix 不是 JDK 一等公民

matrix 参数在 JDK 体系里不是一等公民--`URL.getPath()` 返回 `"/mydb;_sp=mysql"`（matrix 混在 path 里，JDK 不区分）。这带来两个后果：

1. **读子协议必须外层解析**：不能直接 `url.getProtocol()` 拿子协议，必须 `resolveSubProtocols(url)` 从 path 里解析 matrix。这是「外层改写」的固有代价--核心算法的结果需要外层再加工才能用。
2. **序列化/比较时 matrix 可能丢**：`URL.toString()` 调用的是 `URLStreamHandler.toExternalForm`。JDK 默认的 `toExternalForm` 不保留 matrix（它只认 protocol/authority/path/query/ref，把 matrix 当 path 的一部分但拼接时可能丢）。microsphere 因此把 `toExternalForm`/`equals`/`hashCode` 全部 final 并重写，保证 matrix 参数在序列化/比较时不丢。

#### 边界:matrix 在什么情况会失效

**反例一：URL 被非 microsphere handler 先解析过**。如果某 URL 因为协议是 `file`（JDK 内置 handler），先被 JDK 内置 handler 解析，它的 `toExternalForm` 是 JDK 默认实现（不保留 matrix）。后续即使把这个 URL 对象交给 microsphere 处理，matrix 已经丢了--`resolveSubProtocols` 读不到子协议。**结论**：子协议 URL 必须从一开始就用 microsphere 注册的协议，不能混用内置 handler。

**反例二：matrix 解析依赖 path 完整性**。如果中间有代理/网关把 URL 重写（去掉了 path 的 matrix 部分），子协议信息丢失。这是「把元数据藏在 URL 里」方案的共同风险--元数据可能被中间环节剥离。

**外层改写的普适模式**就是这样的：让封闭核心算法能消化输入，但代价是「结果里藏了非标准位置的元数据，需要外层再解析，且对中间环节的剥离敏感」。这是「不改核心算法」这个便利的代价。

### 4.2 改写策略的备选方案

外层改写不止 matrix 参数一种。理论上可以把子协议搬到 URL 的任何位置：

| 改写策略 | 改写后 | 优点 | 缺点 |
|---|---|---|---|
| matrix 参数（microsphere） | `jdbc://host/db;_sp=mysql` | 语义正确、可多级 | 需自定义 matrix 解析 |
| query 参数 | `jdbc://host/db?_sp=mysql` | JDK 原生支持 | 与用户 query 冲突、语义不符 |
| fragment | `jdbc://host/db#mysql` | 不影响 path/query | 只能存一个、语义不符 |
| path 前缀 | `jdbc://host/_sp_mysql/db` | 简单 | 污染 path、路由受影响 |
| 完全自定义 URL 类 | 不用 `java.net.URL` | 自由 | 不兼容所有接受 URL 的 API |

microsphere 选 matrix 是语义最准确的。但 matrix 参数在 JDK 体系里不是一等公民--`URL.getPath()` 会包含 matrix（`/db;_sp=mysql`），需要额外 `resolveSubProtocols` 解析。**这是「外层改写」的固有代价：核心算法的结果需要外层再加工才能用**。

### 4.3 「完全绕开」是另一种解法

不跟封闭算法较劲，直接造一个新抽象。Spring 的 `Resource` 接口就是这条路：

```java
// Spring 不用 URL,自己造 Resource 抽象
Resource res = new ClassPathResource("foo.properties");
InputStream is = res.getInputStream();   // 不走 URL 体系
```

Apache Commons VFS 的 `FileObject`、Hadoop 的 `Path` 都是同类--**绕开 `java.net.URL`，造独立抽象**。

**绕开的代价**：新抽象不兼容老 API。`java.beans.XMLDecoder`、`ImageIO.read(URL)`、XSLT `Transformer` 这些只接受 `URL` 的老代码用不了 `Resource`/`FileObject`。microsphere 选择留在 URL 体系内做扩展，正是为了**兼容这些只认 URL 的老 API**--这是它和 Spring Resource 的根本分野。


## 五、microsphere 作为「协议扩展体系」的一个实例

讲完三个原理，microsphere-net 就是这三个原理的一次落地。这里只用最少的代码佐证。

**实例一：二次安装解法（§2 原理的落地）**

`CompositeURLStreamHandlerFactory` 内部维护 `List<URLStreamHandlerFactory>`，`createURLStreamHandler` 遍历责任链。`attachURLStreamHandlerFactory` 用反射清空 `URL.factory` 字段绕过「只能装一次」约束。双保险：先尝试 set，失败则反射清空再 set。

**实例二：子协议注册表（§3 原理的落地）**

`ExtendableProtocolURLStreamHandler` 内部维护 `List<SubProtocolURLConnectionFactory>`（按 `Prioritized` 排序）。`openConnection` 时用 `resolveSubProtocols` 从 matrix 参数读回子协议链，责任链询问每个 factory 的 `supports`，第一个支持的 `create`。

**实例三：外层改写（§4 原理的落地）**

`parseURL` 重写检测到 `://` 前有冒号时，调 `reformSpec` 把子协议搬运到 matrix 参数 `_sp`。`toExternalForm`/`equals`/`hashCode` 全部 final 并重写，保证 matrix 参数不丢。

**双保险设计**：`ExtendableProtocolURLStreamHandler` 无参构造时校验类名必须是 `Handler`（JDK 命名约定），并把自己的包前缀追加到 `java.protocol.handler.pkgs` 系统属性。SPI（`ServiceLoaderURLStreamHandlerFactory`）是主路径，`handler.pkgs` 是备份路径--两条路都能让 `new URL("classpath:foo")` 找到 handler。这是「主路径 + 兜底」的工程稳健设计。

**两个内置协议**：

- `classpath:` -- 把 `ClassLoader.getResource` 包装成 URL，让 classpath 资源能喂给只认 URL 的老 API（这是 Spring `ClassPathResource` 解决不了的）。
- `console:` -- 把 `System.in`/`System.out` 包装成 URL，展示「任何输入输出源都能被建模成 URL」的极致。实战意义有限，更多是可扩展性的展示。


## 六、实例批判:这个实现的缺陷

作为原理的一个落地实例，microsphere-net 的实现也有瑕疵。列出来说明「即使原理正确，实现细节仍可能出错」。

1. **`classpath.Handler` 用 `Handler.class` 的 ClassLoader 而非 ContextClassLoader**：在 servlet 容器内（Tomcat webapp 独立 ClassLoader），`Handler` 类在 AppClassLoader，找不到 webapp 内的 classpath 资源。应改用 `ClassLoaderUtils.getDefaultClassLoader()`。
2. **`SubProtocolURLConnectionFactory` 的 `supports`/`create` 契约不严**：`supports` 返回 true 但 `create` 返回 null 时，责任链继续--但 `supports` 已声称「我支持」，最终可能走 `openFallbackConnection` 返回 null，调用方 NPE。应规定 `supports=true` 时 `create` 必须非 null 或抛异常。
3. **`clearURLStreamHandlerFactory` 在 JDK 16+ 失败**：未加 `--add-opens` 时反射写 `URL.factory` 抛 `InaccessibleObjectException`，导致整个 `attach` 失败。microsphere 没在代码里检测这个，错误信息不直观。
4. **handler 缓存不被清**（§2.3）：`URL.handlers` 缓存的旧 handler 在换 factory 后仍命中。规避只能靠「早安装」。
5. **matrix 参数被非 microsphere handler 解析时丢失**：如果某 URL 被 JDK 内置 handler（如 `file`）先解析过，它的 `toExternalForm` 不保留 matrix--后续再 cast 成 microsphere handler 时 matrix 已丢。子协议 URL 必须从一开始就用 microsphere 注册的协议，不能混用。

这些不是原理错误，而是原理在具体代码里的实现瑕疵。


## 七、与其他方案的原理对比

| 方案 | 是否扩展 URL 体系 | 二次安装解法 | 多级协议解法 | 反射 JDK 内部 | 依赖 |
|---|---|---|---|---|---|
| microsphere net | ✅（真 URL） | Composite + 反射清空 | matrix 参数外层改写 | 需要 | 零 |
| Spring Resource | ❌（独立抽象 `Resource`） | N/A | N/A | 不需要 | spring-core |
| Apache Commons VFS | ❌（独立抽象 `FileObject`） | N/A | ✅（`res:`/`zip:`/`sftp:` 自定义） | 不需要 | commons-vfs2 |
| JDK 9 URLStreamHandlerProvider | ✅ | 模块 SPI | ❌ | 不需要 | 需模块化 |
| JDBC DriverManager | ✅（用 URL 但不走 URLStreamHandler） | N/A | `Driver.acceptsURL` 自解析 | 不需要 | JDK |

**原理层面的取舍**：

- **Spring Resource / VFS 走「绕开」路线**（§4.3）：自由、不反射 JDK 内部，但不兼容只认 URL 的老 API。
- **microsphere 走「留在 URL 体系内扩展」路线**：兼容老 API，但必须反射 JDK 内部、必须外层改写语法。
- **JDK 9 URLStreamHandlerProvider 是「官方正确解」**，但要求模块化，对老应用不可行。
- **JDBC `DriverManager` 是另一条路**：JDBC URL（`jdbc:mysql://...`）根本不走 `URLStreamHandler`--`DriverManager.getConnection(url)` 自己解析 url 字符串，分发给注册的 `Driver`。这避开了 URL 体系的封闭设计，但牺牲了「URL 的统一抽象」（JDBC URL 不是真正的 `java.net.URL`）。

microsphere 的定位：**在「保持 JDK URL 语义」和「可组合可扩展」之间找平衡，代价是反射 JDK 内部**。如果你能接受独立抽象，Spring Resource / VFS 更稳；如果你必须用 URL（兼容老 API），microsphere 是少数能在不引入大依赖的前提下做到的方案。


## 八、面试要点

**Q1：「`URL.setURLStreamHandlerFactory` 为什么只能调一次？多个框架都想装自己的协议处理器怎么办？」**

答案：根源是 `URL` 是 1996 年设计的「万物皆 URL」统一抽象，`factory` 是全局可变单例，JDK 假设「设置后系统进入稳定状态再改会不一致」。但真实需求是多个框架要协作依次安装。解法是 Composite 包装：把全局 factory 设成一个 `CompositeURLStreamHandlerFactory`，内部维护 `List<URLStreamHandlerFactory>`，`createURLStreamHandler` 责任链遍历。后续框架往 List 里 add，不再触发全局 set。如果槽位已被普通 factory 占，反射清空 `URL.factory` 字段再 set Composite。代价是 JDK 16+ 需要 `--add-opens java.base/java.net=ALL-UNNAMED`，且 handler 缓存（`URL.handlers`）不被清--必须早安装。

**Q2：「`new URL("jdbc:mysql://host/db")` 在 JDK 里会 MalformedURLException，本质原因是什么？怎么解？」**

答案：本质是「注册表的键定义和实际用法不匹配」。JDK 的 `URL.parseURL` 把协议段定义成「不含冒号」，所以 `jdbc:mysql` 被整段当协议名查 handler，而 handler 注册表只有 `jdbc` 这一项--查不到。三种解法：① 扩展键定义（改 parseURL，JDK 改不动）。② 键降级--主协议 `jdbc` 进注册表查 handler，子协议 `mysql` 用其他通道传递（microsphere 选 matrix 参数 `_sp`，语义上 matrix 是「资源标识的一部分」）。③ 绕开 URL 体系（JDBC `DriverManager` 自己解析 url 字符串，不走 URLStreamHandler）。microsphere 在 `parseURL` 里做外层改写：把 `jdbc:mysql://host/db` 改写成 `jdbc://host/db;_sp=mysql`，让 JDK 解析合法的 `jdbc` 协议，子协议存进 matrix 参数。

**Q3：「为什么 Java 的 URL 类设计被批评？跨语言对比有什么不同？」**

答案：Java 把「URL 解析」和「协议访问」焊死在一起--`URL`+`URLStreamHandler`+`URLConnection` 三者强耦合。`URLConnection` 试图统一所有协议但泄漏严重（`HttpURLConnection` 加了几十个 HTTP 专有方法，必须强转）。`URL.equals`/`hashCode` 做 DNS 查询会卡住，不能当 Map key。Go/Python/Node 都把 URL 当纯数据结构解析，资源访问用独立模块（`net/http.Client`/`urllib.request`/`http`）--这样协议扩展就不是 URL 类的事。Java 是 1996 年选了「统一抽象」，三十年改不动。JDK 9 模块化只加了 `URLStreamHandlerProvider`（要求模块化），没修老 URL--向后兼容优先。

**Q4：「什么是『全局可变单例的二次安装』难题？除了 URL 还有哪些例子？」**

答案：进程内某全局槽位只能被设置一次的 API 模式。`URL.setURLStreamHandlerFactory` 是典型，槽位是 `URL.factory` 静态字段。这类 API 假设「设置后稳定」，但忽略了「多框架协作需依次安装」的真实需求。三种解法：① JDK 默认抛错（不让你装第二次）。② Composite 包装--把单例变聚合体，后续 add 进 List。③ 反射清空槽位字段再 set。microsphere 是②+③组合。这个模式不止 URL--任何「全局单例只允许 set 一次」的 API 都能这么撬，比如 Tomcat `ServletContext` 替换、Spring `BeanFactory` 替换。代价是反射 JDK 内部受模块系统限制，且必须早于任何触发缓存的使用。

**Q5：「如果让你设计一个让 `new URL("classpath:foo")` 可用的方案，你会怎么做？和 microsphere 比有什么改进？」**

答案：原理层照搬二次安装解法 + 子协议注册表 + 外层改写。改进点：① `classpath.Handler` 应该用 `ContextClassLoader` 而非 `Handler.class` 的 ClassLoader，否则在 servlet 容器找不到 webapp 资源。② `SubProtocolURLConnectionFactory` 的 `supports`/`create` 契约应该用接口默认方法加强校验（`supports=true` 时 `create` 必须非 null 或抛异常）。③ `clearURLStreamHandlerFactory` 应该检测 JDK 版本和 `--add-opens` 配置，失败时给出明确错误而非让 `InaccessibleObjectException` 冒泡。④ 考虑提供 `URL.handlers` 缓存清理的开关（虽然 JDK 不提供清理 API，但可以反射清空）。核心思路：二次安装和子协议是正确原理，实现细节有优化空间。

---

> **与 SPI+Prioritized 的关联**：net 包的 `CompositeURLStreamHandlerFactory`、`CompositeSubProtocolURLConnectionFactory` 都用 `Prioritized.COMPARATOR` 排序内部成员，是 [§4 SPI+Prioritized](./04-spi-prioritized.md) 的直接应用。
> **与 BannedArtifact 的关联**：两者都反射操作 JDK 内部（`URL.factory` vs `URLClassPath.loaders`），都受 JDK 9 模块系统限制，都需要 `--add-opens`--是「反射 JDK 内部」类方案的同类代价，参见 [§2](./02-banned-artifact-isolation.md)。
