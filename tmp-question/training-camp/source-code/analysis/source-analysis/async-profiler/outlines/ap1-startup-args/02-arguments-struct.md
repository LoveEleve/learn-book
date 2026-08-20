# 02. 参数的家: 枚举与 Arguments 结构 — 配置承载

> 🟡 Working | 12 KP 中的 3 个(7 枚举/Arguments 字段/单位解析)
> 读者处境: parse() 填了什么?为什么同一个参数在头文件和枚举里出现两次?

### 1. "七个枚举管住全宇宙" — 枚举体系

场景: 动作/输出/事件/时钟... 全用枚举,不落字符串。

- `Action`(arguments.h:30): 动作;`Counter`(:42): 计数;`Style`(:47);`CStack`(:58,注释: **更新枚举须同步 FlightRecorder** :57);`Clock`(:66);`Output`(:72);`JfrOption`(:83);`EventCategory`(:96)——共 8 个枚举
- [C++: enum + SHORT_ENUM 宏(紧凑存储)——配置的每个维度都是编译期类型,switch 匹配,零字符串比较]
- `CStack` 的注释是跨文件契约的例子: 枚举值被 FlightRecorder(AP-5)按顺序消费——改枚举必须同步

关键设计: **类型化配置**: 一切选项编译期枚举化——解析时 CASE 填枚举,消费时 switch 读枚举——比字符串配置快且防错。这回答了"为什么枚举在头文件里有一份,parse 里 CASE 一份"(解析器与消费者各持一端)。

### 2. "Arguments: 配置快照" — 结构字段

场景: 所有参数最后去哪了?

- `Arguments`(arguments.h:150): 字段全览——`_action/_event/_timeout/_loop/_interval/_alloc/_lock/_wall/_jstackdepth/_signal/_file/_log/_loglevel/_include/_exclude/_threads/_features/_output/_file_num`(:164-216)
- 构造器全部清零(:228-235)——**无默认值,由 parse 显式填充**(与 Arthas Configure"无默认值纪律"同哲学,AR-1 篇 2 §5)
- `_include/_exclude`: vector<const char*>——过滤栈帧列表

关键设计: **快照式传递**: Arguments 在 parse 后不再变——Engine(AP-2)/Writer(AP-5)并发读它无锁安全。这是采样器多线程架构的基础: 配置一次性,运行期只读。

### 3. "10ms 是怎么来的" — 单位与超时解析

场景: `-i 10ms`、`-d 1h`——字符串单位怎么转数值?

- `Arguments::parseUnits`(arguments.cpp:529): 遍历 Multiplier 表——支持 `us/ms/s/m/h` 后缀,乘数换算
- `parseTimeout`(arguments.cpp:553): 相对时间(`30s`)/绝对时间(`14:00`)
- [C++: Multiplier 是 {后缀, 倍率} 表——strstr 后缀匹配 + 数值换算;超时相对/绝对双格式]

关键设计: **人性化单位在前端**: 用户写 `10ms`,agent 内部统一纳秒/毫秒——单位解析在 parse 层完成,引擎只见数值。这层薄转换让命令行/API/Arthas 三种入口共享同一套人性化语法。

---

跨域桥: 无默认值纪律 = Arthas AR-1 篇 2 §5(Configure 同哲学);枚举的消费方 = AP-2(Engine)与 AP-5(FlightRecorder);CStack 契约 = AP-2(perfEvents 栈行走)。

**OpenJDK 关联**: [域 03 Arguments & Flags — outlines/03-arguments-flags/] — JVM 的 flags 枚举化与 Arguments 结构枚举化是同一设计(编译期类型化配置)。
