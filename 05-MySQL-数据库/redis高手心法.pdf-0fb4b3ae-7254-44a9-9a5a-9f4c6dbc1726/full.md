# Redis

# 高手心法

李健青◎著

# 内容简介

本书共5章。其中，第1章从一条命令的执行开始，勾勒出Redis的数据存储原理和整体架构；第2章介绍了所有数据类型的实现原理和应用实战；第3章介绍了RDB快照、AOF、主从复制架构、哨兵集群和Redis Cluster的原理及使用方法；第4章介绍了Redis事务、内存管理、事件驱动、发布/订阅机制、客户端缓存和I/O多线程模型；第5章介绍了性能排查与解决问题的检查清单、使用规范、内存优化技巧、生产王者必备配置、缓存使用策略和分布式锁演进原理。

本书适合后端开发工程师、运维人员、系统架构师及刚入行的程序员阅读，用以掌握Redis内部原理并提升实战技巧。

未经许可，不得以任何方式复制或抄袭本书之部分或全部内容。  
版权所有，侵权必究。

图书在版编目（CIP）数据

Redis高手心法/李健青著.—北京：电子工业出版社，2024.7.—ISBN978-7-121-48345-5

I. TP311

中国国家版本馆CIP数据核字第2024SZ1581号

责任编辑：张晶

印刷：三河市良远印务有限公司

装 订：三河市良远印务有限公司

出版发行：电子工业出版社

北京市海淀区万寿路173信箱 邮编100036

开 本： $787\times 980$ 1/16 印张：20 字数：512千字

版 次：2024年7月第1版

印次：2024年7月第1次印刷

定价：100.00元

凡所购买电子工业出版社图书有缺损问题，请向购买书店调换。若书店售缺，请与本社发行部联系，联系及邮购电话：（010）88254888，88258888。

质量投诉请发邮件至zlts@phei.com.cn，盗版侵权举报请发邮件至dbqq@phei.com.cn。

本书咨询联系方式：faq@phei.com.cn。

# 前言

亲爱的读者，你好！首先，欢呼一下，我终于完成了这本书的编写工作！对于写书和写微信公众号文章，前者就像参加马拉松，后者更像在跑步机上短跑。

马拉松赛跑需要持久力、需要耐心、需要策略、需要坚韧不拔的精神。而短跑呢，只需要足够快。

在过去的几年里，我在微信公众号“码哥跳动”上一篇篇地写下了我的技术心得，一晃就有了156篇文章，其中45篇是关于Redis的。有很多读者是因为看了我的Redis专栏才关注的我的公众号，这让我倍感欣慰。

后来，有出版社的编辑老师找到我，希望我能整理出一本关于Redis的图书。我原本以为这会是个轻松的任务，然而实际上，我用了两年多的时间才完成。

为什么这么久呢？因为我希望把最好的内容呈现给你们。

公众号上的文章，有些地方还不够完善，所以我花了很多时间重新梳理了Redis的整体架构和源码，重新编写了原来的45篇Redis技术文章，从更深层次挖掘Redis的底层实现原理，并尽量用风趣幽默的语言解释难懂的技术点。

作为后端开发者的我深深懂得学习是一件比较难的事情，所以我想站在开发者的角度，用拟人化、场景化的诙谐幽默的言语，加上“撩人心弦”又准确的图片，让读者轻松愉快地学习Redis的实现原理和开发技巧。

这个过程是费时费力的，但是当看到最后的成果时，我觉得一切都是值得的。

本书基于当前最新的Redis7.0的源码讲解，建立了一个完整的Redis知识框架，从全局着眼整理Redis的知识体系，并结合难点给出了155张图，希望读者更容易理解。

在我看来，写书可比写公众号文章难多了。书中的语言要精准正确，句子不能存在语病，内容要循序渐进有层次，还要经过出版社编辑老师的多次审核、校正，书中的每段话、每个字都是我精心“雕琢”的成果。

编写本书的过程也让我有机会重新整理和深化对Redis的理解。我希望这本书能帮助你们修炼内功心法，快速掌握Redis技术原理。

我试图从Redis的第一人称视角出发，用风趣的言语为你们逐步揭开Redis的面纱，分享Redis升级之路的心法技巧和提高技术水平的方法论。

希望你们通过阅读本书，在精进Redis的道路上更上一层楼。

# 本书特色

本书从Redis源码目录结构、整体存储结构、一条指令的执行过程展开，分为以下章节。

$\odot$ 起势入门：带你构建一个源码可调式开发环境，从一条命令的执行开始，勾勒出Redis的数据存储原理和整体架构。  
$\odot$ 核心筑基——数据结构与心法：包含所有数据类型的实现原理和应用实战。  
- 不死之身——高可用：包括 RDB 快照、AOF、主从复制架构、哨兵集群和 Redis 集群的原理及使用方法。  
$\odot$ 结丹飞升——高级技能进阶：介绍Redis事务、内存管理、事件驱动、发布/订阅机制、客户端缓存和I/O多线程模型。  
◎ 元婴大成——出师实战：包括性能排查与解决问题的检查清单、使用规范、内存优化技巧、生产王者必备配置、缓存使用策略和分布式锁演进原理。

本书从Redis的视角与各路“神仙”对话，探讨每个技术点的原理，在探讨过程中或详或略地展开相关知识点。

本书语言诙谐幽默，并配以生动形象且准确的图片，用循循善诱的方式帮助读者建立一个完整的Redis知识框架和架构体系，构建系统观，让读者较为深入地了解Redis的工作原理。

干古无同局，叶底能否藏花，我们未来印证。愿此心法能让你学有所成，你来，我等着。

# 读者对象

以下是本书适合的读者对象。

◎ 后端开发工程师和运维人员：对于有一些使用经验，但是Redis功底相对薄弱、对Redis的底层运行原理了解不多的读者，阅读本书后可掌握高阶特性的原理和实战方法，合理并高效地运用Redis解决工作中的问题并进行性能调优，以及维护和构建高性能的Redis集群。  
系统架构师：从全局视角掌握Redis架构和原理，学习Redis高可用、高性能的设计思想，解决Redis性能难题。

◎ 刚入行的程序员：如果你不想仅停留在“面试八股文”的阶段，而是希望从更深层次掌握Redis内部原理和实战技巧，那么本书可以帮助你在面试或者工作中脱颖而出。

# 勘误和支持

由于作者水平有限，书中难免出现一些错误或者不准确的地方，恳请广大读者批评指正。

如果大家在阅读过程中产生疑问或者发现了错误，欢迎到微信公众号“码哥跳动”后台留言，我会认真回复每个人提出的问题。

# 致谢

感谢微信公众号“码哥跳动”的读者朋友，你们的鼓励和支持是我坚持下去的动力。

感谢电子工业出版社的张晶老师，她在本书的创作过程中提供了许多修改建议和帮助，没有她，本书无法顺利出版。

感谢我的家人和 Chaya 激励我完成本书的编写工作。

谨以此书献给一路关注我、支持我的读者和所有热爱编程的朋友，希望喜欢Redis的朋友都能通过阅读本书修炼心法，成为Redis高手！

李健青

2024.4

# 目录

# 第1章 起势入门 1

1.1 从头说起

1.1.1 Redis能做什么 1  
1.1.2 源码编译 2   
1.1.3 目录结构

1.2 整体架构 8

1.2.1 数据存储原理 10  
1.2.2 一条命令的执行过程. 14

# 第2章 核心筑基——数据结构与心法 ..... 23

2.1 字符串实现原理与实战 23

2.1.1 不同于C语言的字符串 23  
2.1.2 SDS的奥秘 24   
2.1.3 出招实战：分布式ID生成器 27

2.2 Lists 实现原理与实战 ..... 28

2.2.1 线性有序 28   
2.2.2 linker、ziplist、quicklist、unpack 演进 28   
2.2.3 出招实战：消息队列 36

2.3 Sets 实现原理与实战 ..... 40

2.3.1 无序和唯一 40  
2.3.2 intset. 41   
2.3.3 出招实战：共同好友 43

2.4散列表实现原理与实战 44

2.4.1 field-value pairs 集合 44  
2.4.2 dict and listpack 45   
2.4.3 出招实战：购物车 49

2.5 Sorted Sets 实现原理与实战 ..... 52

2.5.1 有序性和唯一性 52  
2.5.2 skiplist $^+$ dict and listpack 52   
2.5.3 出招实战：游戏排行榜 57

2.6Stream实现原理与实战 59

2.6.1 支持消费者组的轻量级MQ 59  
2.6.2 Radix Tree 的奥秘 60  
2.6.3 出招实战：实现消费者组特性的消息队列 67

2.7 Geospatial 实现原理与实战 ..... 76

2.7.1 基于位置服务 77  
2.7.2 GeoHash编码和底层数据结构 81  
2.7.3 出招实战：附近的人 86

2.8Bitmap实现原理与实战 90

2.8.1 Bitmap 90   
2.8.2 SDS数据结构构成的位数组 91  
2.8.3 出招实战：亿级用户登录判断、签到统计系统 94

2.9 HyperLogLog 实现原理与实战 ..... 97

2.9.1 基数统计 97   
2.9.2 稀疏矩阵和稠密矩阵 ..... 98   
2.9.3 出招实战：海量网页访问量统计 99

2.10 BloomFilter实现原理与实战 101

2.10.1 Bloom Filter 102   
2.10.2 位数组和哈希函数 ..... 102  
2.10.3 出招实战：缓存穿透解决方案 103

2.11 Redis高性能的原因 106

2.11.1 基于内存实现 ..... 107  
2.11.2 I/O多路复用模型 108  
2.11.3 单线程模型 ..... 110  
2.11.4 高效的数据结构 112  
2.11.5 全局散列表 ..... 113

# 第3章 不死之身——高可用 114

3.1 容机恢复，不丢数据稳如山 114

3.1.1 RDB 快照 ..... 114

3.1.2 AOF 118

3.2 主从复制架构 125

3.2.1 主从数据同步原理 127  
3.2.2 主从同步的缺点 135

3.3哨兵集群 136

3.3.1 嗨兵的任务 138  
3.3.2 嗅兵集群原理 143

3.4Redis集群 147

3.4.1Redis集群是什么 148  
3.4.2Redis集群的原理 150  
3.4.3 集群配置注意事项 164

# 第4章 结丹飞升——高级技能进阶 165

4.1Redis事务修炼手册 165

4.1.1 什么是事务的ACID 165  
4.1.2Redis如何实现事务 166  
4.1.3Redis事务满足ACID吗 168

4.2Redis内存管理 174

4.2.1 淘汰策略概述 174  
4.2.2 过期删除策略 183

4.3Redis事件驱动：文件和时间的协奏曲 185

4.3.1Redis server启动入口 186  
4.3.2 文件事件 190  
4.3.3 时间事件 191

4.4Redis发布/订阅机制深度解析 194

4.4.1 发布/订阅机制简介 194  
4.4.2 发布/订阅机制实战 195  
4.4.3 原理分析 199  
4.4.4 使用场景 204

4.5 性能必杀技之客户端缓存 205

4.5.1 为什么需要客户端缓存 205  
4.5.2 客户端缓存实现原理 ..... 207  
4.5.3 源码解析 211

4.6 性能必杀技之RedisI/O多线程模型 213

4.6.1 单线程模型真的只有一个线程吗 213  
4.6.2 线程模型的演化 214  
4.6.3 I/O多线程模型解读 215

# 4.7Redis内存碎片深度解析与优化策略 221

4.7.1 数据已删，释放的内存去哪了 221  
4.7.2 什么是内存碎片 223  
4.7.3 内存碎片的形成原因 223  
4.7.4 内存碎片解决之道 224

# 第5章元婴大成一出师实战 227

# 5.1Redis性能排查与解决问题的终极检查清单 227

5.1.1 性能基线测量 227  
5.1.2 慢命令监控 229   
5.1.3 解决性能问题的终极检查清单 230

# 5.2Redis很强，不懂使用规范就糟蹋了 237

5.2.1 key-value 使用规范 237  
5.2.2 命令使用规范 239  
5.2.3 数据存储使用规范 240  
5.2.4 SDK使用规范 240  
5.2.5 运维规范 241

# 5.3Redis内存优化必杀技，小内存存储大数据 241

5.3.1 key-value 优化 242  
5.3.2 小数据集合编码优化 243  
5.3.3 使用对象共享池 244  
5.3.4 使用bit或byte级别操作 244  
5.3.5 巧用Hashes类型优化 245  
5.3.6 使用内存碎片清理功能 246  
5.3.7 使用32位的Redis 246

# 5.4 生产王者必备配置详解 246

5.4.1 常规配置 247  
5.4.2 RDB 快照持久化 ..... 250  
5.4.3 主从复制 251  
5.4.4 安全 255  
5.4.5 内存管理 256

5.4.6 惰性释放 257   
5.4.7 AOF 持久化 259  
5.4.8Redis集群 261  
5.4.9 性能监控 264   
5.4.10 高级配置 264  
5.4.11 在线内存碎片清理 270  
5.4.12 绑定CPU 271   
5.4.13 sentinel.conf哨兵 271

# 5.5 缓存击穿、缓存穿透、缓存雪崩怎么解决 275

5.5.1 缓存击穿 275   
5.5.2 缓存穿透 277   
5.5.3 缓存雪崩 278

# 5.6Redis缓存策略与数据库一致性问题深度剖析 280

5.6.1 缓存策略 281  
5.6.2 缓存与数据库一致性是什么 ..... 287  
5.6.3 旁路缓存的问题分析 ..... 287  
5.6.4 数据库与缓存一致性解决方案 ..... 293  
5.6.5 总结 295

# 5.7Redis分布式锁演进原理与实战 296

5.7.1 为什么需要分布式锁 ..... 296  
5.7.2 入门级分布式锁 297  
5.7.3 释放别人的锁 299  
5.7.4 可重入锁 300  
5.7.5 正确配置锁过期时间 303  
5.7.6Redis部署方式对锁的影响 307  
5.7.7 红锁 307  
5.7.8 红锁的是与非 308

# 第1章 起势入门

# 1.1 从头说起

天下武功，无坚不摧，唯快不破！我的名字叫Redis，全称是RemoteDictionaryServer。

有人说，组“CP”，除了要了解她，还要制造机会让她了解你。

那么，作为开发工程师的你，是否愿意认真阅读此心法来了解我，从而提升系统性能呢？

我遵守BSD协议，这是由意大利人Salvatore Sanfilippo使用C语言编写的一个基于内存实现的键值型非关系（NoSQL）数据库，可作为数据库、缓存、消息队列、流处理引擎。我的特点是速度快，QPS的每秒请求数可以达到100000。

我提供了 String（字符串）、Hashes（散列表）、Lists（列表）、Sets（无序集合）、Sorted Sets（可根据范围查询的排序集合）、Bitmap（位图）、HyperLogLog（基数统计）、Geospatial（地理空间）和 Stream（流）等数据类型。

数据类型的使用技法和实现原理是筑基的必经之路，请好好修炼。

除此之外，我还具有主从复制、Lua脚本、LRU淘汰机制，以及事务和不同级别的磁盘持久化功能，并通过sentinel（哨兵）和Redis集群实现高可用，这部分内容是修炼的重中之重，高手必备。

# 1.1.1 Redis 能做什么

程许媛：“Redis，说了这么多，你能干啥？别王婆卖瓜，自卖自夸。”

# 缓存

这是我被使用的最多的场景，能极大提升应用程序的性能。当单个 MySQL 读/写压力比较大时，在读多写少的场景中，把热点数据存储在 Redis 中。

读取数据的步骤如下。

（1）发出从Redis缓存中读取数据的请求。  
（2）如果Redis缓存未命中，则从数据库中获取数据，并把数据写到Redis缓存中，让后续读取相同数据的请求命中Redis缓存，并将数据返回给调用者。  
（3）Redis 缓存命中，直接返回。

# 排行榜

使用MySQL等关系型数据库来实现排行榜非常麻烦，性能也差，直接使用Sorted Sets可以轻松搞定各种排行榜，你只需用score保存玩家的游戏得分，用member保存玩家ID，根据score排序便可实现一个排行榜。

# 消息队列

在一些不需要高可靠的，例如到货通知、未读消息、邮件发送的场景中，可以使用Lists或者Stream来实现一个简单消息队列。

# 分布式锁

Redisson 框架使用 Redis 实现了一套分布式锁解决方案。

# 计数器

Redis 的命令都是原子性的，你可以轻松地利用 INCR 和 DECR 命令来构建计数器系统。

更多的场景将在后续章节详细介绍。学完这些内容，我相信你定能强基健体，念头通达，升职加薪。

千古无同局，叶底能否藏花，我们未来印证。愿此心法能让你学有所成，你来，我等着。

# 1.1.2 源码编译

读完前面的内容，我相信你一定想继续了解Redis。本节会通过源码编译来安装Redis 7.0.5，让你在自己的机器上搭建一套可以Debug的源码环境。

这部分是后续原理分析的基础，推荐你在 macOS 或者 Linux 操作系统上部署环境，如果你的操作系统是 Windows，那么可以通过虚拟机安装 Linux 操作系统，再继续部署 Redis 环境。

程许媛：“我的电脑是 macOS 操作系统，你就用这个来演示吧。”

# 获取源码

获取源码的方式有两种，第一种是从官网下载Redis源码压缩包，如图1-1所示，再将压缩包解压得到一个文件夹。

![](images/e58be49bb39e561e29363bfd65bcfd3c5286228f4ee73e9cf0073bf5abafd277.jpg)  
图1-1

第二种方式是通过git clone获取源码。使用git clone https://github.com/redis/redis.git命令从GitHub上下载如图1-2所示的文件。

![](images/1df2cd813c55f56bef779f703f9e30cfdf6074370264d11ab6d3ff9b1fa853d9.jpg)  
图1-2

进入redis目录，使用git checkout切换到tag7.0.5分支。

git checkout tags/7.0.5 -b 7.0.5

# 编译Redis

在编译之前，需要安装一些环境依赖，Redis是C语言编写的，所以还需要安装gcc编译器。执行gcc-v命令判断是否安装了编译器，如图1-3所示。

![](images/f965beff64ebb1ffe8f514437c94e0e71a5faa88b5a8598db647db4cf36d3f2b.jpg)  
图1-3

没有安装gcc编译器，那么可以使用如下命令安装。

```batch
xcode-select --install 
```

一切准备就绪，进入Redis的源码目录，执行make命令，它的作用是根据项目中的Makefile文件中定义的规则和命令来执行相应的操作，从而生成目标文件、可执行文件或其他输出文件。

make CFLAGS $= \text{" -g -00"}$ MALLOC=jemalloc

命令后边的-O0 参数表示不要优化代码，以防在 Debug 时，IDE 里面的 Redis 源码与实际运行的代码对应不上。

MALLOC = jemalloc 表示在 macOS 操作系统上，Redis 使用 jemalloc 内存分配器来分配内存，Linux 操作系统默认使用该分配器。准备就绪直接使用 make 命令编译源码即可。

编译成功，将会看到Hint: It's a good idea to run 'make test';)的提示，如图1-4所示，这时可以运行单元测试，当然，这一步也可以省略。

![](images/e6c9b0296d98378181b007b2e69fbfd1cfe10d1ead807282bd8c10df4415fe34.jpg)  
图1-4

# 启动Redis

编译成功，在src源码目录下使用如下命令启动，结果如图1-5所示。

```txt
./redis-server ../redis.conf 
```

![](images/346d11f3943e0e54749e048dd75fc1206ffacfa161c54dd3712649939f555208.jpg)  
图1-5

# 代码调试环境搭建

为了方便阅读和Debug源码，极力推荐你使用CLion来阅读和调试Redis源码，我使用的是CLion2023.2。安装好以后，打开Redis源码目录即可，如图1-6所示。

![](images/a31b866229bd454f2c878a94de34ba7f67fce2571d5bfed12ce8f254ca585f9d.jpg)  
图1-6

在下拉菜单中选择redis-server，如图1-7所示。

![](images/984f49245d2b1ffbbc41e90fcb74a8cc27ee715f3b70428dce8d197aa2f69c85.jpg)  
图1-7

指定启动配置文件redis.conf的目录，如图1-8所示。

![](images/16013b6675b937bd5a6c2958cf5ef81bc1442b5cc8796385776833463ac8fc41.jpg)  
图1-8

在server.c的main()方法中加断点，Debug启动redis-server，进行源码Debug，如图1-9所示。

![](images/07ce5f333546aab0a1299efa362e10d92a61cdbf4a8d0bf279c93183a4449183.jpg)  
图1-9

大功告成，接下来我们就可以在Redis的知识海洋里“呛水”了。

# 1.1.3 目录结构

在“呛水”之前，我们先来了解一下Redis源码的整体目录结构，形成全局的认识，防止陷入细节或者无从下手。

# deps

该目录包括Redis依赖的第三方代码库。

© Jemalloc：内存分配器，默认情况下选择该内存分配器来代替 Linux 操作系统的 libc-malloc。libc-malloc 性能不高，且碎片化严重。  
©Hiredis：官方C语言客户端。  
- Linenoise：读线替换。Linenoise由Redis的作者开发，作为一个单独的项目进行管理，并根据需要进行更新。  
Lua：顾名思义，就是 Lua 相关的功能。  
$\odot$ hdr摄影作品：用于生成每个命令的延迟跟踪直方图。

# src

这是Redis源码的重要组成部分，包括commands和modules两个子目录，以及其余功能模块的源码，这是Redis最重要的目录。

modules 目录包含了实现 Redis module 的示例代码，commands 里面都是 JSON 格式的文件，包含了每个命令的元信息。

# tests

该目录下包括功能模块测试和单元测试的代码。

$\odot$ Cluster:Redis集群功能测试。  
$\odot$ sentinel:哨兵集群功能测试。   
$\odot$ Unit: 单元测试。  
$\odot$ Integration：主从复制功能测试。

除此以外，还有 assets、helpers、modules 和 support 4 个子目录，它们用来支撑测试功能。

# utils

该目录主要包括辅助性功能的脚本或者代码，例如用于创建Redis集群的脚本，Iru算法效果展示代码等。

此外，还有redis.conf和sentinel.conf这两个重要的文件，分别用于配置Redis实例和哨兵。

# 1.2 整体架构

通过源码编译构建出可调式环境之后，想必你想更深入了解我的整体架构，如图1-10所示。只有熟悉我的整体架构和所有模块，才能在遇到问题时直击本源、直捣黄龙，“一笑破苍穹”。

![](images/6c6fc04d899828942f84d37289c2f992644579a554a45cba3c5232e77ca48123.jpg)  
图1-10

应用层：client是官方提供的C语言开发的客户端，可以发送命令，进行性能分析和测试等。

◎ 网络层：事件驱动模型，基于 I/O 多路复用封装了一个短小精悍的高性能 ae 库，全称是 a simple event-driven programming library。

- 在 ae 库中，我通过 aeApiState 结构体对 poll、select、kqueue、evport 4 种 I/O 多路复用的实现进行适配，让上层调用方感知不到在不同操作系统实现 I/O 多路复用的差异。  
- Redis 中的事件可以分两大类：一类是网络连接、读、写事件；另一类是时间事件，例如定时执行 rehash、RDB 快照生成、过期 field-value pairs 清理操作。

命令执行层：负责执行客户端的各种命令，例如SET、DEL、GET等。  
◎ 内存层：为数据分配并回收内存，提供不同的数据结构保存数据。  
$\odot$ 持久化层：提供了RDB快照文件和AOF两种持久化策略，实现数据可靠性。  
高可用模块：提供了副本、哨兵、集群实现高可用。  
统计和监控：提供了一些监控工具和性能分析工具，例如监控内存使用、基准测试、内存碎片、Bigkey统计、慢指令查询等。

掌握了整体架构和模块后，进入 src 源码目录，使用以下命令启动 Redis。

```txt
./redis-server ./redis.conf 
```

我会把启动的所有服务抽象成一个redisServer，源码定义在server.h的redisServer结构体中。

这个结构体包含了存储field-valuepairs（key-value）的redisDb、redis.conf文件路径、命令列表、加载的Modules、网络监听、客户端列表、RDB或AOF加载信息、配置信息、RDB快照、主从复制、客户端缓存、数据结构压缩、发布/订阅、集群、哨兵等一些列Redis实例运行的必要信息。部分核心字段如下。

```c
struct redisServer {
    pid_t pid; /*主进程pid. */
   pthread_t main_thread_id; /*主线程id */
    char *configfile; /*redis.conf文件绝对路径*/
    redisDb *db; /*存储field-valuepairs数据的redisDb实例*/
    int dbnum; /*DB个数*/
    dict *commands; /*当前实例能处理的命令表，key是命令名，value是执行命令的入口*/
    aeEventLoop *el; /*事件循环处理*/
    int sentinel_mode; /*true表示作为哨兵实例启动*/
    /*网络相关*/
    int port; /*TCP监听端口*/
    list *clients; /*连接当前实例的客户端列表*/
    list *clients_to_close; /*待关闭的客户端列表*/
    client *current_client; /*当前执行命令的客户端*/
};
```

# 1.2.1 数据存储原理

以Redis7.0为例，server.h的redisDb结构体抽象了Redis核心存储结构。其中，redisDb\*db指针非常重要，它指向了一个长度为dbnum（默认为16）的redisDb数组，它是整个存储的核心，我用它来存储field-valuepairs。

redisDb

redisDb 结构体的定义如下。

```txt
typedef struct redisDb {
    dict *dict;
    dict *Expires;
    dict *blocking_keys;
    dict *ready_keys;
    dict *watched_keys;
    int id;
    long long avg ttl;
    unsigned long expires_cursor;
    list *defrag_future;
    clusterSlotToKeyMapping *slots_to_keys;
} redisDb; 
```

- dict 和 expires: dict 和 expires 是 Redis 最重要的两个属性, 它们的底层数据结构是字典, 分别用于存储 field-value pairs 数据和每个 key 的过期时间。

MySQL：“过期时间为什么要分开存储？”

好问题，之所以要分开存储，是因为并不是每个 key 都会配置过期时间，它不是 field-value pairs 的固有属性，分开后，不需要配置过期时间的 field-value pairs 的数据能节省内存开销。

- blocking_keys 和 ready_keys: blocking_keys 和 ready_keys 的底层数据结构是 dict 字典，主要用于实现 BLPOP 等阻塞命令。当客户端使用 BLPOP 命令阻塞等待取出列表元素时，我会把 key 写到 blocking_keys 中，value 是被阻塞的客户端。  
当下一次收到PUSH命令时，先检查blocking_keys中是否存在阻塞等待的key，如果存在就把key放到ready_keys中，在下一次Redis事件处理过程中，遍历ready_keys数据，并从blocking_keys中取出被阻塞的客户端响应。  
© watched_keys：用于实现 watch 命令、存储 watch 命令的 key。  
© Id:Redis 数据库的唯一 ID，一个 Redis 服务支持多个数据库，默认为 16 个。  
$\odot$ avg_ttl: 用于统计平均过期时间。  
© expires_cursor：统计过期事件循环执行的次数。  
© defrag_later: 保存进行碎片整理的 key 列表。

© slots_to_keys：仅用于集群模式，当使用集群模式时，只能有一个数据库 db 0。  
slotsto_keys用于记录在集群模式下，存储key与哈希桶映射关系的数组。

dict

Redis 使用 dict 结构来保存所有的 field-value pairs 数据，这是一个散列表，所以 key 查询的时间复杂度是 $O(1)$ 。

我们可以将散列表类比为 Java 中的 HashMap，它其实就是一个数组，数组中的元素叫作哈希桶。dict 结构体源码在 dict.h 中定义。

```c
struct dict {
   DUCTType *type;
    dictEntry **ht_table[2];
    unsigned long ht_used[2];
    long rehashidx;
    int16_t pauseerehash;
    signed char ht_size_exp[2];
}; 
```

其中，dictType *type、**ht_table[2]和long rehashidx是3个很重要的结构。

- dictType *type: 一个指向��结构的指针，表示字典的类型。dictType包含了一组函数指针，用于对field-value pairs进行操作，例如哈希函数、复制键、复制值等。  
- ht_table[2]: 大小为 2 的散列表数组。每个散列表都是一个指向字典数组的指针，数组的元素是 dictEntry 类型的，表示字典中的一个 field-value pairs。通常使用 ht_table[0]存储数据，当执行 rehash 时，使用 ht_table[1] 配合完成。  
© ht_used[2]: 两个散列表的使用情况, 表示当前散列表已经使用的槽位数量。  
- long rehashidx: 表示正在进行 rehash 操作的索引位置。当 rehashidx 的值为 -1 时，表示没有进行 rehash 操作。  
int16_t pauseerehash: 当其值大于 0 时表示 rehash 操作被暂停; 小于 0 时表示编码错误。  
signed char ht_size_exp[2]: 两个散列表的大小，以 2 的指数的形式表示。ht_size_exp数组的每个元素都对应散列表的指数。

重点关注ht_table数组，数组中的元素叫作哈希桶，保存了所有field-valuepairs，哈希桶的类型是dictEntry。

MySQL：“Redis支持那么多的数据类型，哈希桶怎么保存？”

哈希桶的玄机就在dictEntry中，每个dict都有两个ht_table，用于存储field-valuepairs

数据和实现渐进式 rehash。dictEntry 的结构如下。

```txt
typedef struct dictEntry {
    void *key;
    union {
        // 指向实际 value 的指针
        void *val;
        uint64_t u64;
        int64_t s64;
        double d;
    } v;
    // 散列表冲突生成的链表
    struct dictEntry *next;
    void *metadata[];
} dictEntry;
```

$\odot$ void *key:: 一个指向键的指针，表示字典中的键。  
union {...} v;：一个联合体，包含了字典条目的值，当它的值是 uint64_t、int64_t 或 double 类型时，就不再需要额外的存储，这有利于减少内存碎片（我为节省内存操碎了心）。当然，val 也可以是 void 指针、指向值的指针，以便能存储任何类型的数据。  
- struct dictEntry *next;：指向哈希桶中下一个条目的指针，允许多个条目存在于同一个哈希桶中，形成一个链表。ht_table 使用链地址法来处理键碰撞：当多个不同的键拥有相同的哈希值时，散列表用一个链表将这些键连接起来。  
$\odot$ void *metadata[];：一个灵活数组，用于存储元数据。元数据的大小由_dictType 的 dictEntryMetadataBytes 函数返回。这个数组允许存储额外的信息，以扩展字典条目的功能。

哈希桶是一个指向具体值的指针，以此存储不同类型的数据。

# redisObject

dictEntry 的 *val 指针指向的值实际上是一个 redisObject 结构体，这是一个非常重要的结构体。key 是 string 类型的，而 value 可以是 String、Lists、Sets、Sorted Sets、Hashes 等类型的。field-value pairs 的值都被包装成 redisObject 对象，redisObject 在 server.h 中定义。

```c
typedef struct redisobject{ unsigned type:4; unsigned encoding:4; unsigned lru:LRU BITS; int refcount; void \*ptr; }robj; 
```

$\odot$ type:4：记录了对象的类型，例如 String、Sets、Hashes、Lists、Sorted Sets 等，根据该类型可以确定对象是哪种数据类型，使用什么样的 API 操作。

- encoding:4：编码方式，表示ptr指向的数据类型的具体数据结构，即这个对象使用了什么数据结构作为底层保存数据。同一个对象使用不同编码实现，其内存占用存在明显差异，内部编码对内存优化非常重要。例如Aring串可以使用RAW编码或INT编码。  
- Iru:LRU BITS: LRU 策略下对象最后一次被访问的时间，如果是 LFU 策略，那么低 8 位表示访问频率，高 16 位表示访问时间。  
© refcount：表示引用计数，由于 C 语言并不具备内存回收功能，所以 Redis 在自己的对象系统中添加了这个属性，当一个对象的引用计数为 0 时，表示该对象已经不被任何对象引用，可以进行垃圾回收了。  
$\odot$ *ptr 指针：对象的指针，指向实际存储对象数据。根据对象的类型和编码不同，ptr 可能指向 String、Lists、Hashes 等具体的数据结构。

![](images/3b6a19be2f20006a758477d915272a7c91ade73a9c64121f19ec6dcdbf60c44d.jpg)  
图1-11是redisDb、dict、dictEntry和redisObejct的关系图。  
图1-11

注意，一开始的时候，只使用ht_table[0]这个散列表读/写数据，ht_table[1]指向NULL，当这个散列表容量不足，触发扩容操作时才会创建一个更大的散列表ht_table[1]。

接着使用渐进式 rehash 和定期迁移的方式将 ht_table[0] 的数据迁移到 ht_table[1] 上，全部迁移完成后，修改一下指针，让 ht_table[0] 指向扩容后的散列表，回收原来的散列表，ht_table[1] 再次指向 NULL。

# 1.2.2 一条命令的执行过程

MySQL：“知道了整体架构和各个模块后，一条Redis命令是如何执行的呢？”

要想了解一个技术的本质，需要从整体到细节，切不可不见森林就去看叶子，所以我先梳理出一个关键执行流程，如图1-12所示，以免你陷入细节不能自拔。

![](images/1d6a985dd55e0813ac1c986d30dadec4d43f2d3e305580c3f0f6495810988cf9.jpg)  
图1-12

该流程图重点展示的是在开启I/O多线程模型的情况下的命令执行过程。

# 创建连接接收客户端请求

默认在6379端口监听可读事件，通过ae事件驱动模块接收客户端发起的请求，入口在aeMain函数，这是一个基于I/O多路复用的while无限循环。

客户端发来“SET key 码哥字节”请求，触发可读事件，调用 readQueryFromClient 执行流程，这个流程会判断是否启用 I/O 多线程来选择不同分支处理。

# 开启I/O多线程模式

主线程首次调用 readQueryFromClient 时会先执行 postponeClientRead，将可读事件的 client 放入 redisServer.client_pending_read 列表。

在主线程每次进入aeApiPoll函数阻塞等待可读可写事件之前，会先调用beforeSleep函数。

这个函数内部会调用 handleClientsWithPendingReadsUsingThreads 函数，它的核心逻辑是把可读事件的 client 按照 Round Robin 的方式分配到每个 I/O 线程关联的 io Threads_list 队列中，I/O 线程从队列中获取任务并执行，也就是从 socket 读取和解析命令。

handleClientsWithPendingReadsUsingThreads 的源码如下。

int handleClientsWithPendingReadsUsingThreads(void) { //1. I/O多线程模型是否启用，如果未启用则停止执行 if(!server.io Threads.active || !server.io Threads_do_reads) return 0; int processed = listOfLength(server.cliento_pending_read); if (processed == 0) return 0; /*2.将server.cliento_pending_read中的client分配到不同的io Threads_list*/ listIter li; ListNode \*ln; listRewind(server.cliento_pending_read,&li); int item_id $= 0$ while(( $\mathrm{ln} =$ listNext(&li))）{ client \*c $=$ listOfValue(ln); int target_id $=$ item_id%server.io Threads_num; listOfNodeTail(io Threads_list[target_id],c); item_id++; }

$\text{串}$ 3.配置全局变量io Threads_op为IOThreads_OP_READ，告诉I/O线程此次处理的是可读事件\*/

```txt
io Threads_op = IOThreads_OP_READ;  
for (int j = 1; j < server.io Threads_num; j++) {  
    int count = length(io Threads_list[j]); 
```

setIOPendingCount(j,count);   
}   
/*4.主线程处理io Threads_list[0]中的任务，调用readQueryFromClient读取数据并解析命令，清空io Threads_list[0]列表*/   
listRewind(io Threads_list[0]&li); while((ln $=$ listNext(&li)){ client \*c $=$ listOfValue(ln); readQueryFromClient(c->conn); } listEmpty(io Threads_list[O]);   
/\*5.主线程阻塞，等待所有I/O线程完成数据读取和命令解析\*/ while(1){ unsigned long pending $= 0$ for(intj $= 1$ j $<$ server.io Threads_num;j++) pending $+ =$ getIOPendingCount(j); if (pending $= = 0$ ) break;   
}   
//6.全局变量io Threads_op配置为IOThreads_OP_IDLE，表示当前I/O线程空闲io Threads_op $=$ IOThreads_OP_IDLE;   
/\*7.主线程从clients_pending_read中取出client并执行读取完成且解析好的命令\*/ while(listLength(server.clientPending_read))（ //省略部分代码： if (processPendingCommandAndInputBuffer(c) $= =$ C_ERR){ continue; } //省略部分代码   
server.stat_io_reads processed $+ =$ processed; return processed;

# 未开启I/O多线程模型

如果未开启I/O多线程模型，主线程则自己完成读取命令、解析命令、执行命令和发送结果给客户端的全部流程。

# 读取和解析命令

主线程和 I/O 线程从绑定的 io_treads_list 任务队列中取出任务并处理。主线程和 I/O 线程再次进入 readQueryFromClient 流程。

需要注意的是，在这次执行 readQueryFromClient 流程前，client 状态已经被配置为

CLIENT_PENDING_READ，不会把 client 再次加入 clients_pending_read 队列，而是进入真正的执行流程，读取 socket 并解析命令。

主线程在完成 io Threads_list[0] client 的读取和解析后，会阻塞等待全部 I/O 线程完成，直到全部命令解析完成，才会真正执行命令。

# 执行命令

主线程等所有 I/O 线程完成 socket 读取和命令解析，接下来到了最重要的一步——执行命令。

回到 handleClientsWithPendingReadsUsingThreads 函数，在完成了任务分配、命令读取和解析之后，主线程会进入一个 while 循环，从 server.client_pending_read 队列中，每取出一个 client 就调用一次 processPendingCommandAndInputBuffer 函数，执行这个 client 已经解析好的命令。

函数内部会调用processCommandAndResetClient函数执行实际的命令。

int processPendingCommandAndInputBuffer(client \*c){ if(c->flags&CLIENT_PENDING_COMMAND) { c->flags $= =$ ~CLIENT_PENDING_COMMAND; //主线程执行命令 if(processCommandAndResetClient(c) $= =$ C_ERR){ return C_ERR; } //省略部分代码 return C_OK;

processCommandAndResetClient函数的源码如下。

int processCommandAndResetClient(client \*c)（//省略部分代码if (processCommand(c) $= =$ C_OK){commandProcessed(c);updateClientMemUsageAndBucket(c);}//省略部分代码returndeadclient？C_ERR：C_OK;

重点看processCommand(c)函数，其核心逻辑是从server COMMANDs这个字典中查找命令对应的redisCommand实例。经过系列检查，如果不通过就给客户端返回错误信息。

如果通过就调用 $c->cmd->proc$ 函数处理真正的命令，例如 SET 命令，proc 就指向 setCommand 函数。

/\*Exec the command \*/   
if (c->flags &CLIENTMulti && c->cmd->proc != execCommand && c->cmd->proc != discardCommand && c->cmd->proc != multiCommand && c->cmd->proc != watchCommand && c->cmd->proc != quitCommand && c->cmd->proc != resetCommand) //1.命令入队 queueMultiCommand(c, cmd_flags); //2.入队后响应客户端" $^+$ QUEUED"字符串 addReply(c,shared.queueed); }else{ //3.不需要入队，直接执行命令调用call函数 call(c,CMD_CALL_FULL); c->woff $=$ server master_repl_offset; if(listLength(server.ready_keys)) handleClientsBlockedOnKeys();

函数执行流程如图1-13所示。

![](images/d184b1cd9a0e2d6d4aa9085495c7c03c7484d7bf4b50beca38b24595fe881bc8.jpg)  
图1-13

回头再看processCommandAndResetClient函数，发现returndeadclient？C_ERR：C_OK；成功返回1，错误返回0。

MySQL：“我并没有看到将命令产生的返回值写回客户端的代码，你是如何将命令产生的返回值写回客户端的？”

通过执行流程图可以看出，在开启I/O多线程模型时，我是通过I/O线程将命令的返回值

写回客户端的。

# 发送结果给客户端

我以 String 类型的 GET 命令为例，源码文件是 t_string.c。getCommand 直接调用 getGenericCommand 函数。

intgetGenericCommand(client\*c){ robj\*o; //从数据库中查询key对应的value值，并赋值给0 if $(\mathrm{o} =$ lookupKeyReadOrReply(c,c->argv[1],shared_null[c->resp]） $= =$ NULL) return C_OK; //一些类型校验 if(checkType(c,o,OBJ_STRING)) { return C_ERR; } //对返回值进行编码并返回 addReplyBulk(c,o); return C_OK;   
}   
voidgetCommand(client\*c){ getGenericCommand(c);

重点在于 addReplyBulk(c,o)，执行命令完毕后，进入响应客户端阶段，主线程调用 addReply 函数把执行结果响应给客户端。

```txt
void addReply(client *c, robj *obj) {
    if (prepareClientToWrite(c) != C_OK) return;
    ...
} 
```

内部调用prepareClientToWrite，把执行结果放入clients_pending_write可写队列。在进入下一次事件循环时，beforeSleep函数内部会调用handleClientsWithPendingWritesUsingThreads函数把clients_pending_write队列任务分配给I/O线程和主线程。

任务分配完成之后，I/O 线程和主线程会调用 writeToClient 函数把命令的执行结果发送到客户端，writeToClient 函数的核心是一个 while 循环，内部会不断调用 _writeToClient 函数，向底层的 Socket 连接里写数据。

int handleClientsWithPendingWritesUsingThreads(void) { //1.没有客户端需要处理，return0 int processed $=$ listOf(server.client_pending_write); if(rocessed $\equiv$ 0) return 0; //2.如果没有启用I/O线程或者只有少量client需要处理，则主线程同步，不使用I/O线程 if (server.io Threads_num $= = 1$ ||stopThreadedIOIfNeeded()){ return handleClientsWithPendingWrites();

）

/* 3. 开启了 I/O 多线程模式，如果没有激活则调用 startThreadedIO 激活 */
if (!server.io Threads.active) startThreadedIO();

/* 4. 将 clients_pending_write 队列 client 分配到不同 io Threads_list 列表中，主线程负责 io Threads_list[0] 的任务 */

listIter li;   
node \*ln;   
listRewind(server客户的 pending_write,&li);   
int item_id $= 0$ while((1n $=$ listNext(&li))) { client \*c $=$ listOfValue(ln); c->flags $\& =$ -CLIENT_PENDING_WRITE; //省略部分代码 int target_id $=$ item_id%server.io Threads_num; listAddNodeTail(io Threads_list[target_id],c); item_id++;

/* 5. 修改全局变量，标识可写 */

```txt
io Threads_op = IOThreads_OP_WRITE;  
for (int j = 1; j < server.io Threads_num; j++) {  
    int count = length(io Threads_list[j]);  
    setIOPendingCount(j, count);  
} 
```

/* 6. 主线程调用 writeToClient 函数把 io Threads_list[0] 的 client 执行结果写回客户端 */

listRewind(o Threads_list[0],&li); while((ln $=$ listNext(&li))）{ client \*c $=$ listOfValue(ln); writeToClient(c,0);

//7.清空io Threads_list[0]

```txt
isEmpty(o Threads_list[0]); 
```

/* 8. 主线程等待其他 I/O 线程完成客户端响应 */

while(1) { unsigned long pending $= 0$ for (int $\mathrm{j} = 1$ .j $<  _{i}$ server.io Threads_num; j++) pending $+ =$ getIOPendingCount(j); if (pending $= 0$ ) break;

//9.将全局变量配置为IO_THREAD_OP_IDLE表示线程空闲

```txt
io Threads_op = IOThreads_OP_IDLE; 
```

/* 10. 检查 clients_pending_write 中的 client 是否还有数据要响应给客户端，如果有则调用 CT_Socket.set_writehandler 函数将 sendReplyToClient 函数配置为 connection-> writehandler 回调函数*/

listRewind(server.client_pending_write,&li); while((ln $=$ listNext(&li))){ client \*c $=$ listOfValue(ln); updateClientMemUsageAndBucket(c); if (clientHasPendingReplies(c)) { installClientWriteHandler(c); }   
1   
// 11. 清空 server.client_pending_write 队列   
isEmpty(server.client_pending_write);   
server.stat_ioWrites processed $+=$ processed;   
return processed;

以上代码的主要步骤如下。

（1）查询 server.clienti pending_write 队列长度，为空则停止后续流程。  
（2）如果没有启用I/O线程或者只有少量client需要处理，则主线程同步调用handleClientsWithPendingWrites函数完成全部client的写回响应，不开启I/O线程。如果不满足以上条件，则继续接下来的步骤。  
（3）开启了I/O多线程模式，如果没有激活则调用startThreadedIO激活I/O线程。  
（4）循环遍历 clients_pending_write 队列，使用 Round-Robin 算法将 client 分配到每个 I/O 线程绑定的 io Threads_list 列表中。主线程负责 io Threads_list[0] 的任务。  
（5）修改全局变量io Threads_op $=$ IOThreads_OP_WRITE，通知I/O线程处理的是可写事件。I/O线程会执行writeToClient函数将client的响应写回客户端。  
（6）主线程也会调用 writeToClient 函数把 io Threads_list[0] 的 client 执行结果写回客户端。  
（7）主线程执行 listEmpty(o Threads_list[0])清空 io Threads_list[0] 列表。  
（8）主线程阻塞，等待其他 I/O 线程把各自绑定的 io Threads_list 队列的 client 的响应写回客户端的任务执行完毕。  
（9）将全局变量io Threads_op配置为IOThreads_OP_IDLE，表示I/O线程空闲。

# Redis高手心法

（10）检查 clients_pending_write 中的 client 是否还有数据要响应给客户端。如果有则调用 CT_Socket.set_writehandler 函数将 sendReplyToClient 函数配置为 connection->writehandler 回调函数。当连接事件可写时，主线程会调用 sendReplyToClient 函数，内部会调用 writeToClient 函数将 client 执行结果数据写回客户端。  
（11）调用isEmpty(server.client_pending_write)清空 clients_pending_write队列。

一条Redis命令的执行过程到此结束，完结撒花。

# 第2章 核心筑基——数据结构与心法

我是Redis，给开发者提供了String（字符串）、Lists（列表）、Sets（无序集合）、Hashes（散列表）、Sorted Sets（可根据范围查询的排序集合）、Stream（流）、Geospatial（地理空间）、Bitmap（位图）、HyperLogLog（基数统计）和Bloom Filter（布隆过滤器）等数据类型。

接下来我要介绍的是每种数据类型的使用技巧和使用场景，以及这些数据类型的底层数据结构原理。

# 2.1 字符串实现原理与实战

# 2.1.1 不同于 C 语言的字符串

字符串类型的使用场景最为广泛，例如计数器、缓存、分布式锁，以及存储登录后的用户信息，key 保存 token，value 存储登录用户对象的 JSON 字符串。

```json
SET user:token:666 {"name": "码哥", "gender": "M", "city": "shenzhen"} 
```

接下来，我先带你深入了解字符串类型的底层数据结构和使用场景。

```txt
MySQL：“你都是用C语言开发出来的，C语言本就有字符串，吓唬谁呢？！”
```

格局能不能打开一点儿，我并没有直接使用 C 语言的字符串，而是自己搞了一个 SDS 的结构体来表示字符串。SDS 的全称是 Simple Dynamic String，中文叫作简单动态字符串。

```txt
MySQL: “搞SDS的目的是啥？”
```

为了支持丰富和高性能的字符串操作函数、保存二进制格式数据、节省内存，以及实现“既要又要还要”的目标。先看C语言字符串数组的结构，例如通过char *s = "MageByte"定义字符串变量，如图2-1所示。

$$
\text {c h a r} ^ {*} \mathrm {s} = ^ {\prime \prime} \text {M a g e B y t e} ^ {\prime \prime}
$$

![](images/4581cdc554a3cce2a6be04ea94bf9685b312e0a93fa66f57a27729a8f41083c4.jpg)  
图2-1

注意，数组的最后一个字符串是“\0”，它表示字符串的结束。因为C语言标准库string.h中的字符串有以下不足，所以我设计了SDS。

C 语言使用 char* 字符串数组来实现字符串，在创建字符串时就要需要手动检查和分配字符串空间。由于没有 length 属性记录字符串长度，想要获取一个字符串长度就要从头遍历，直到遇到 \0，唯快不破的我是不能容忍的。  
- 无法做到“安全的二进制格式数据存储”，图片等二进制格式数据无法保存。无法存储 \0 这种特殊字符是因为 \0 在 C 语言字符串中表示结尾。  
- 字符串的扩容和缩容。char 数组的长度在创建字符串的时候就确定下来，如果要追加数据，则要重新申请一块空间，把追加后的字符串内容拷贝进去，再释放旧的空间，十分消耗资源。

# 2.1.2 SDS的奥秘

MySQL：“说说SDS结构体吧，你是如何解决这些问题的。”

为了存储字符串的实际内容，需要一个 char 类型数组，用一个 int 类型的 len 字段记录 char 数组使用了多少字节。

除此之外，还要有一个int类型的alloc字段记录分配的char数组总长度，alloc-len就等于char类型的buf数组未使用的字节数，如图2-2所示。

![](images/7974f8d3eb5ca63aaaad05a6631b15b882b550c8b08cc8d6367d60fdce4f9ae4.jpg)  
图2-2

SDS 也遵循 C 语言的字符串以空字符“\0”结尾的惯例，空字符的大小不计算在 SDS 的 len 字段中。

# O(1) 时间复杂度获取字符串长度

SDS中的len字段保存了字符串的长度，实现了 $O(1)$ 时间复杂度获取字符串长度。SDS结构有一个flags字段，表示的是SDS类型。实际上SDS一共设计了5种类型，分别是sdshdr5、sdshdr8、sdshdr16、sdshdr32和sdshdr64，区别在于数组的len长度和分配空间长度alloc不同。例如sdshdr8如下。

```c
struct __attribute__((__packed__)) sdshdr8 {
    uint8_t len;
    uint8_t alloc;
    unsigned char flags;
    char buf[];
}; 
```

len 和 alloc 字段都是 uint8_t 类型的，在 Java 中 int 是 32 位的，而 C 语言中的 int 值不同，其中 uint8_t 是占 8 位的无符号 int 值，能表示的最大值是 $2^{\wedge}8 - 1$ ，那么它的 buf 数组的最大长度就是 $2^{\wedge}8 - 1$ 。

# 节省内存

之所以这么设计，是因为使用不同的SDS类型保存不同大小的字符串可以节省内存。

MySQL：“SDS能存储多大的字符串？”

alloc 表示当前 SDS 结构允许容纳的最大字符长度，例如 uint32_t alloc 的取值范围是 $0 \sim 2^{\wedge} 32$ 。理论上，char 数组的最大长度为 4294967296，1 个 char 字符占用 1 字节，可以存储 4GB，更不用说 sdshdr64 了。这些都是理论值，实际上 Redis 内部会限制最大的字符串长度为 512MB。

# 编码格式

我还对字符串类型的数据采用了三种编码格式来存储，分别是 int、embstr 和 raw，你可使用 OBJECT encoding key 来查找对象所使用的编码类型。编码选择流程如图 2-3 所示。

© int 编码：8 字节的长整型，值是数字类型且数字的长度小于 20。  
$\odot$ embstr编码：长度小于或等于44字节的字符串。  
© raw 编码：长度大于 44 字节的字符串。

MySQL: “_attribute_((_packed_))是什么？”

我使用了专门的编译优化手段来节省内存空间。它的作用就是告诉编译器，不要使用字节对齐的方式，而是采用紧凑的方式分配内存。在默认情况下，编译器会按照8字节对齐的方式分配内存，即使这个变量的大小不到8字节。一旦使用了_attribute_（(packed_）定义结构体，编译器就会按照实际占用来分配内存空间。

![](images/dd96d99ed0b506e407aaf62c92c05202deed3869085ae648a8fbd527e713aa28.jpg)  
图2-3

# 二进制格式数据安全

SDS不仅可以存储字符串类型数据，还能存储二进制格式数据。SDS并不是通过“\0”来判断字符串结束的，而是采用len标志结束，所以可以直接存储二进制格式数据。

# 空间预分配

在需要对SDS的空间进行扩容时，不仅仅分配所需的空间，还会分配额外的未使用空间。通过预分配策略，减少了执行字符串增长所需的内存重新分配次数，以及操作字符串增加带来的性能损耗。

# 惰性空间释放

当对SDS进行缩短操作时，程序并不会回收多余的内存空间，如果后面需要append追加操作，则直接使用buf数组alloc-len中未使用的空间。

通过惰性空间释放策略，避免了减小字符串所需的内存重新分配操作，为未来的增长操作提供了优化空间。

# 2.1.3 出招实战：分布式ID生成器

我相信你会经常遇到需要生成唯一ID的场景，例如标识每次请求、生成一个订单编号、创建用户。

分布式ID生成器需要满足以下特性。

趋势递增，MySQL是最常用的数据库，如果ID不是趋势递增，那么B+树为了维护ID的有序性，会频繁地在索引的中间位置插入节点，从而影响后面节点的位置，甚至频繁导致页分裂，这对于性能的影响是极大的。  
$\odot$ 全局唯一性，ID不唯一就会出现主键冲突。  
$\odot$ 高性能，生成ID是高频操作，如果性能缓慢，系统的整体性能就会受到限制。  
高可用，也就是在给定的时间间隔内，一个系统总的可用时间占的比例。  
存储空间小，例如对于MySQL的InnoDBB+树，普通索引（非聚集索引）会存储主键值，主键越大，每个Page可以存储的数据就越少，访问磁盘I/O的次数就会增加。

Redis 集群能保证高可用和高性能，为了节省内存，ID 可以使用数字的形式，并且通过递增的方式来创建新的 ID。未来防止重启数据丢失，你还需要把 Redis AOF 持久化开启。

MySQL：“开启 AOF 持久化，为了性能配置 everysec 策略还是有可能丢失 ls 的数据，你还可以使用一个异步机制将生成的最大 ID 持久化到一个 MySQL。”

好主意，在生成ID之后发送一条消息到MQ消息队列中，把值持久化到MySQL中。我提供了INCR命令，它能把键中存储的数字加1并返回客户端。如果键不存在，则先将值初始化成0，再执行加1操作并返回给客户端。该命令的值限制在64位有符号数字之内。

# 设计思路

假设订单ID生成器的键是“counter:order”，当应用服务启动时先从数据库中查询出最大值M。执行EXISTScounter:order判断是否存在键。

（1）如果Redis中不存在键“counter:order”，则执行SET counter:order M将值M写入Redis。  
（2）如果Redis中存在键“counter:order”，值为N，则比较M和N的值，执行SETcounter:order max(M,N)将最大值写入Redis，如果相等则不操作。  
（3）应用服务启动完成后，在每次需要生成ID时，应用程序就向Redis服务器发送INCRcounter:order命令。  
（4）应用程序将获取到的ID值发送到MQ消息队列，消费者监听队列把值持久化到MySQL。

出招实战：分布式ID生成器

![](images/e28b9704ad8c42a938655474627cd5a1e4099aee329bfcb1bd7a7d226c23d44a.jpg)  
图2-4

# 2.2 Lists 实现原理与实战

# 2.2.1 线性有序

Redis的Lists与Java中的LinkedList类似，是一种线性的有序结构，按照元素被推入列表中的顺序存储元素，满足先进先出的需求。你可以把它当作队列、栈来使用。

# 2.2.2 linkedlist、ziplist、quicklist、unpack 演进

在C语言中，并没有现成的链表结构，所以Antirez为我专门设计了一套实现方式。关于Lists类型的底层数据结构，可谓英雄辈出，Antirez大佬一直在优化，创造了多种用来保存的数据结构。包括早期作为Lists的底层实现的链接（双端链表）和ziplist（压缩列表），

Redis 3.2 引入的由 linkedlist 和 ziplist 组成的 quicklist，以及 7.0 版本中取代了 ziplist 的 listpack。

MySQL：“为什么弄了这么多数据结构呀？”

Antirez 所做的这一切都是为了在内存空间开销与访问性能之间做取舍和平衡，跟着我去“吃透”每个类型的设计思想和不足，你就明白了。

# 链接list

在Redis3.2之前，Lists的底层数据结构由linkslist或者ziplist实现，优先使用ziplist存储。当Lists对象满足以下两个条件时，将使用ziplist存储链表，否则使用linkslist。

$\odot$ 链表中的每个元素占用的字节数小于 64。  
链表的元素数量小于512个。

```txt
链表的节点使用adlist.h/listNode结构来表示。typedef struct ListNode{//前驱节点struct ListNode \*prev; //后驱节点struct ListNode \*next; //指向节点的值void \*value;   
} ListNode;
```

node之间通过prev和next指针组成linkedlist。

```c
typedef struct list{ //头指针 listingNode \*head; //尾指针 listingNode \*tail; //节点值复制函数 void \*（\*dup)(void \*ptr); //节点值释放函数 void \*free)(void \*ptr); //节点值是否相等 int \*match)(void \*ptr,void \*key); //链表的节点数量 unsigned long len;   
}list;
```

linkslist 的结构如图 2-5 所示。

![](images/e18aa811df731fd79b4e443ccf78c9abc528f19d718ed8a5e34a7193d22db886.jpg)  
图2-5

Redis 的链表实现的特性如下。

双端：链表节点带有 prev 和 next 指针，获取某个节点的前置节点和后继节点的复杂度都是 $O(1)$ 。  
无环: 链表头节点的 prev 指针和尾节点的 next 指针都指向 NULL, 对链表的访问以 NULL 结束。  
$\odot$ 带表头指针和表尾指针：通过list结构的head指针和tail指针，程序获取链表的头节点和尾节点的复杂度为 $O(1)$ 。  
◎ 使用链表结构的 len 属性记录节点数量，获取链表中节点数量的复杂度为 $O(1)$ 。

# MySQL: “看起来没什么问题呀, 为什么还要 ziplist 呢?”

你知道的，我在追求快和节省内存的方向上无所不及，有两个原因导致了ziplist的诞生。

◎ 普通的 linkedlist 有 prev、next 两个指针，在数据很小的情况下，指针占用的空间会超过数据占用的空间，这就离谱了，是可忍，孰不可忍。  
© LinkedInist 是链表结构，在内存中不是连续的，遍历的效率低下。

# ziplist

为了解决上面两个问题，Antirez创造了ziplist，这是一种内存紧凑的数据结构，占用一块连续的内存空间，能够提升内存使用率。

当一个 Lists 只有少量数据，并且每个列表项要么是小整数值，要么是长度比较短的字符串时，我就会使用 ziplist 作为 Lists 的底层数据结构存储数据。

ziplist 有多个 entry 节点，可以存放整数或者字符串，结构如图 2-6 所示。

![](images/bb2bebc34aed48f15d80e88f4ff9a80d0018ff9a24393f24a1562d1b9169c4fe.jpg)  
图2-6

© zlbytes：占用4字节，记录整个ziplist占用的总字节数。  
© ztlail：占用4字节，指向最后一个entry偏移量，用于快速定位最后一个entry。  
© zlen: 占用 2 字节, 记录 entry 总数。  
$\odot$ entry: Lists 的元素。  
©zlend:ziplist 结束标志，占用1字节，值等于255。

因为ziplist头尾元数据的大小是固定的，并且zlen记录了ziplist头部最后一个元素的位置，所以，能以 $O(1)$ 的时间复杂度找到ziplist中第一个或最后一个元素。而在查找中间元素时，只能从Lists头或者Lists尾遍历，时间复杂度是 $O(N)$ 。存储数据的entry结构如图2-7所示。

![](images/42e7ea56fd5c9980a96020157043b8848b175c43d4d174ca28834d44daad95ac.jpg)

![](images/9885040f58e6b39d67856e0ed1eafb41f4fe20eb48f4c37e2a1654027c4d1f1d.jpg)  
图2-7

entry 通常由 <prevlen>、<encoding> 和 <entry-data> 3 部分构成。

prevlen

记录前一个 entry 占用的字节数，逆序遍历就是通过这个字段确定向前移动多少字节拿到上一个 entry 的首地址的。这部分会根据上一个 entry 的长度进行变长编码（为节省内存操碎了心），变长方式如下。

$\odot$ 前一个 entry 的字节数小于 254（255 用于 zlend），prevlen 的长度为 1 字节，值等于上一个 entry 的长度。  
前一个 entry 的字节数大于或等于 254,prevlen 占用 5 字节，第 1 字节配置为 254 作为一个标识，后面 4 字节组成一个 32 位的 int 值，用于存放上一个 entry 的字节长度。

encoding

表示当前 entry 的类型和长度, 前两位用于表示类型, 当前两位的值为 11 时表示 entry 存放的是 int 类型数据, 在其他情况下表示存储的是字符串。

entry-data

实际存放数据的区域，需要注意的是，当 entry 中存储的数据是 int 类型时，encoding 和 entry-data 会合并到 encoding 中，没有 entry-data 字段。此刻结构就变成了由 <prevlen> 和 <encoding> 组成。

MySQL：“为什么说ziplist省内存？”

（1）与 linkedlist 相比，少了 prev、next 指针。  
（2）通过encoding字段针对不同编码来细化存储，尽可能做到按需分配，当entry存储的是int类型时，encoding和entry-data会合并到encoding，省掉了entry-data字段。  
（3）每个entry-data占据的内存大小不一样，为了解决遍历问题，增加了prevlen记录上一个entry的长度。遍历数据的时间复杂度是 $O(1)$ ，但是对数据量很小的情况影响不大。

MySQL：“听起来很完美，为什么还要搞quicklist？”

“既要又要还要”的需求是很难实现的，ziplist节省了内存，但是也有不足。

$\odot$ 不能保存过多的元素，否则查询性能会大大降低，导致 $O(N)$ 时间复杂度。  
© ziplist 的存储空间是连续的，当插入新的 entry 时，内存空间不足就需要重新分配一块连续的内存空间，引发连锁更新的问题。

# 连锁更新

每个 entry 都用prevlen记录上一个entry的长度，在当前entry B前面插入一个新的entry A时，会导致B的prevlen改变，也会导致entry B的大小发生变化。entry B后一个entry C的prevlen也需要改变。以此类推，就可能导致连锁更新，如图2-8所示。

![](images/cecff9c22d0789fbbfa051a78bdca03ea4c11eccf18095b6995a5709e3258175.jpg)  
图2-8

连锁更新会导致多次重新分配ziplist的内存空间，直接影响ziplist的查询性能。于是，Redis3.2引入了quicklist。

# quicklist

quicklist 是综合考虑了时间效率与空间效率引入的新型数据结构。它结合了 linkedlist 与 ziplist 的优势，本质还是一个链表，只不过链表的每个节点都是一个 ziplist。

数据结构定义在quicklist.h文件中，链表由quicklist结构体定义，每个节点都由quicknode结构体定义（源码版本为6.2，7.0版本使用listpack取代了ziplist）。quicklist是一个双向链表，所以每个quicknode都有前序节点指针(*prev)和后序节点指针(*next)。由于每个节点都是ziplist，所以还有一个指向ziplist的指针\*zl。

typedef struct quicklistNode{ //前序节点指针 struct quicklistNode \*prev; //后序节点指针 struct quicklistNode \*next; //指向ziplist的指针 unsigned char \*zl; //ziplist字节大小 unsigned int sz; //ziplist元素个数 unsigned int count :16; //编码格式， $1 =$ RAW，代表未压缩原生ziplist，2=LZF，代表压缩存储 unsigned int encoding:2; //节点持有的数据类型，默认值为2，表示ziplist unsigned int container :2; //节点持有的ziplist是否经过解压，1表示已经解压过，下一次操作需要重新压缩 unsigned int recompress :1; //ziplist数据是否可压缩，太小的数据不需要压缩 unsigned int attempted_compress :1; //预留字段 unsigned int extra :10; }quicklistNode;

quicklist 作为链表，定义了头、尾指针，用于快速定位链表头和链表尾。

```c
typedef struct quicklist (
    // 链表头指针
    quicksortNode *head;
    // 链表尾指针
    quicksortNode *tail;
    // ziplist 的总 entry 个数
    unsigned long count;
    // quicksortNode 个数
    unsigned long len;
    int fill : OL capacities;
```

```txt
unsigned int compress:QL_COMP BITS; unsigned int bookmark_count:QL_BM BITS; //柔性数组，给节点添加标签，通过名称定位节点，实现随机访问的效果 quicklistBookmark bookmarks[];   
}quicklist; 
```

结合quicklist和quicknode的定义，quicklist的结构如图2-9所示。

![](images/e0f70ded993d4c11107d7a876fc4d35384233fcbeaa7ef52c1da242cc90c0605.jpg)  
图2-9

从结构上看，quicklist是ziplist的升级版，优化的关键点在于控制好每个ziplist的大小或者元素个数。

- quicknode 的 ziList 越小，可能造成越多的内存碎片，极端情况是每个 ziList 只有一个 entry，退化成了 linkedlist。  
- quickliste 的 ziplist 过大，极端情况下会造成一个 quicklist 只有一个 ziplist，退化成了 ziplist。连锁更新的性能问题就会暴露无遗。

合理配置很重要，Redis 提供了 list-max-ziplist-size -2，当 list-max-ziplist-size 为负数时表示限制每个 quicknode 的 ziplist 的内存大小，超过这个大小就会使用 linkedlist 存储数据，每个值的含义如下。

-5：每个quicklist节点上的ziplist最大为 $64\mathrm{kb} <   - -$ 正常环境不推荐  
-4: 每个quicklist节点上的ziplist最大为 $32\mathrm{kb} < - - -$ 不推荐  
-3: 每个quicklist节点上的ziplist最大为 $16\mathrm{kb} < -$ 可能不推荐  
-2: 每个quicklist节点上的ziplist最大为 $8\mathrm{kb} < -$ 不错  
-1: 每个quicklist节点上的ziplist最大为 $4\mathrm{kb} < \dots$ 不错

默认值为-2，也是官方最推荐的值，当然你可以根据自己的实际情况进行修改。

MySQL：“搞了半天还是没能解决连锁更新的问题嘛。”

别急，饭要一口一口吃，路要进一步一步走，步子迈大了容易摔跟头。

ziplist 是紧凑型数据结构，可以有效利用内存。但是每个 entry 都用 prevlen 保留了上一个 entry 的长度，所以在插入或者更新时可能出现连锁更新影响效率。

于是 Antirez 又设计出了“链表 + ziplist”组成的 quicklist 来避免单个 ziplist 过大，缩小连锁更新的影响范围。可毕竟还是使用了 ziplist，本质上无法避免连锁更新的问题。于是，5.0 版本设计出另一个内存紧凑型数据结构 listpack，并在 7.0 版本中替换掉 ziplist。

# listpack

出现zippack的原因是用户上报了一个Redis崩溃的问题，但是Antirez并没有找到崩溃的明确原因，猜测可能是ziplist结构导致的连锁更新，于是他想设计一种简单、高效的数据结构来替换ziplist。

MySQL: “unpack 是什么？”

unpack也是一种紧凑型数据结构，用一块连续的内存空间来保存数据，并且使用多种编码方式来表示不同长度的数据来节省内存空间。

源码文件 listpack.h 对 listpack 的解释是 A lists of strings serialization format, 意思是一种字符串列表的序列化格式, 可以把字符串列表进行序列化存储, 可以存储字符串或者整形数字。

listpack 的结构包括 tot-bytes、num-elements、elements 和 listpack-end-byte 4 部分，如图 2-10 所示。

![](images/d7c16fb0ce79b286790b300b82d69da74a03b362714aeb8b9fd04973cf85f707.jpg)  
图2-10

© tot-bytes: 也就是 total bytes, 占用 4 字节, 记录 listpack 占用的总字节数。  
$\odot$ num-elements：占用2字节，记录listpackelements的个数。  
elements: listpack 元素，保存数据的部分。  
©扎实推进：结束标志，占用1字节，值固定为255。

MySQL：“好家伙，这跟ziplist有什么区别？别以为换了个名字，换个马甲我就不认识了。”

听我说完！listpack 确实也是由元数据和数据组成的，它们最大的区别是 elements 部分，为了解决 ziplist 连锁更新的问题，element 不再像 ziplist 的 entry 保存前一项的长度，如图 2-11 所示。

![](images/f540c7692a6f5c84df393abb0245cbca21ad7a48a6a69a87697b4b8b75756a13.jpg)  
图2-11

© encoding-type: 存的是 element-data 部分的编码类型和长度, 是一个变长结构。  
element-data: 实际存放的数据。  
element-tot-len: encoding-type + element-data 的总长度，不包含自身的长度。

每个 element 只记录自身的长度，当修改或者新增元素时，不会影响后续 element 的长度，解决了连锁更新的问题。从 linkedlist、ziplist 到“链表 + ziplist”构成的 quicklist，再到 listpack，可以看到，设计的初衷都是能够高效地使用内存，同时避免性能下降。

# 2.2.3 出招实战：消息队列

学完了Lists的底层数据结构，终于到我（Redis）大显身手上才艺搞实战的环节了。

分布式系统中必备的一个中间件就是消息队列，它可以进行服务间异步解耦、流量消峰、实现最终一致性。目前市面上已经有 RabbitMQ、RchetMQ、ActiveMQ 和 Kafka 等消息队列，有人会问：“Redis 适合做消息队列吗？”

在回答这个问题之前，你先从本质思考。

消息队列提供了什么特性？  
© Redis 如何实现消息队列？是否满足存取需求？

我将结合消息队列的特点，分析将Redis的Lists作为消息队列的实现原理，把这章学到的运用到项目中。学会了这些，今年的优秀员工奖就是你的了。

# 什么是消息队列

消息队列是一种异步的服务间通信方式，适用于分布式和微服务架构。消息在被处理和删除之前一直存储在队列上。

它基于先进先出（FIFO）的原则，允许发送者（生产者）向队列中发送消息，而接收者（消

费者）则可以从队列中获取消息并进行处理。

消息队列通常被用于解耦应用程序的各个组件，实现异步通信、削峰填谷、解耦合、流量控制等功能。如图2-12所示。

![](images/2bbaa5642beaf999989fb25239f334a371a0d7f23ca9e7bc2bfc97c2a4886d68.jpg)  
图2-12

$\odot$ Producer：消息生产者，负责产生和发送消息到 Broker。  
Broker：消息处理中心。负责消息存储、确认、重试等，一般会包含多个 queue。  
Consumer：消息消费者，负责从Broker中获取消息，并进行相应处理。

MySQL：“消息队列的使用场景有哪些呢？”

消息队列在实际应用中包括如下4个场景。

- 应用耦合：发送方、接收方的系统之间不需要互相了解，只需要认识消息。多应用间通过消息队列对同一消息进行处理，避免调用接口失败导致整个过程失败。  
◎ 异步处理：多应用对消息队列中的同一消息进行处理，应用间并发处理消息，相比串行处理，减少处理时间。  
◎ 限流削峰：广泛应用于秒杀或抢购活动中，避免流量过大导致应用系统“挂掉”的情况。  
消息驱动的系统：系统中有消息队列、消息生产者和消息消费者，生产者负责产生消息，消费者（可能有多个）负责处理消息。

# 消息队列满足哪些特性

（1）消息有序性：消息是异步处理的，但是消费者需要按照生产者发送消息的顺序来消费，避免出现后发送的消息被先处理的情况。  
（2）重复消息处理：当因为网络问题出现消息重传时，消费者可能收到多条重复消息。同样的消息重复多次可能造成同一业务逻辑被多次执行，在这种情况下，应用系统需要确保幂等性。  
（3）可靠性：保证一次性传递消息。如果发送消息时接收者不可用，那么消息队列会保留消息，直到成功地传递它。当消费者重启后，可以继续读取消息进行处理，防止消息遗漏。  
（4）LPUSH：生产者使用LPUSH key element[element...]将消息插入队列头部，如果key不存在则会创建一个空的队列再插入消息。如下，生产者向队列queue先后插入了Java、码

哥字节、Go，返回值表示插入队列的消息个数。

```txt
> LPUSH queue Java 码哥字节 Go (integer) 3 
```

MySQL：“如果生产者发送消息的速度很快，消费者处理不过来，则会导致消息积压，占用过多的内存。”

确实，Lists 并没有提供类似于 Kafka 的消费者组（Consumer Group），由多个消费者组成一个消费者组来分担处理队列消息的任务。不过从 5.0 版本开始，Redis 提供了 Stream 数据类型，后面我会介绍。

# RPOP

消费者使用RPOP key依次读取队列的消息，先进先出，所以Java会先被消费者读取。如图2-13所示。

![](images/fc5a2cbd42922505a689695c3e5836281be18c830a7d1253c2d5fb233005c0c9.jpg)  
图2-13

# 实时消费问题

谢霸戈：“这么简单就实现了？”

别高兴得太早，LPUSH、RPOP存在性能风险，当生产者向队列插入数据时，Lists并不会主动通知消费者及时消费。

谢霸戈：“那我写一个while(true)不停地调用RPOP命令，当有新消息时就消费！”

程序需要不断轮询并判断是否为空再执行消费逻辑，这就会导致即使没有新消息写入队列，消费者也在不停地调用RPOP命令占用CPU资源。

谢霸戈：“如何避免循环调用导致的CPU性能损耗呢？”

请叫我贴心哥Redis，我提供了BLPOP、BRPOP阻塞读取的命令，消费者在读取队列没有数据时会自动阻塞，直到有新的消息写入队列，才继续读取新消息执行业务逻辑。

```txt
BRPOP queue 0 
```

参数0表示阻塞等待绵绵无绝期，哪怕烟花易冷人事易分，雨纷纷旧故里草木深，斑驳的城门盘踞着老树根，直到“心上人”来。

# 重复消费解决方案

消息队列自动为每条消息生成一个全局ID。  
◎ 生产者为每条消息创建一个全局ID，消费者把处理过的消息ID记录下来判断是否重复。

其实这就是幂等，对于同一条消息，消费者收到后处理一次的结果和处理多次的结果是一样的。

# 消息可靠性解决方案

谢霸戈：“消费者读取消息，处理过程中宕机了就会导致消息没有处理完成，可是数据已经不在队列中了，怎么办？”

这种现象的本质就是消费者在处理消息时崩溃了，无法再读取消息，缺乏一个消息确认的可靠机制。我提供了BRPOPLPUSH source destination timeout命令，含义是以阻塞的方式从source队列读取消息，同时把这条消息复制到另一个destination队列中（备份），并且是原子操作。

不过这个命令从6.2版本起被BLMOVE取代。接下来，上才艺！生产者使用LPUSH把消息依次存入order:pay队列队头（左端）。

```txt
LPUSH order:pay "谢霸戈"  
LPUSH order:pay "肖菜姬" 
```

消费者消费消息时在 while 循环使用 BLMOVE，以阻塞的方式从队列 order:pay 队尾（右端）弹出消息“谢霸戈”，同时把该消息复制到队列 order:pay:back 队头（左端），该操作是原子性的，最后一个参数 timeout = 0 表示持续等待。

```txt
BLMOVE order:pay order:pay:back RIGHT LEFT 0 
```

如果消费消息“谢霸戈”成功，就使用LREM把队列order:pay:back中的“谢霸戈”消息删除，从而实现ACK确认机制。

```dockerfile
LREM order:pay:back 0 "谢霸戈" 
```

0是count参数对应的值，该参数的含义如下。

$\odot$ count $>0$ ，从表头（左端）向表尾（右端），依次删除count个value。

$\odot$ count $< 0$ ，从表尾（右端）向表头（左端），依次删除count个value。  
$\odot$ count $= 0$ ，删除所有的value。

如果消费异常，那么应用程序使用 BRPOP order:pay:back 从备份队列再次读取消息即可，如图 2-14 所示。

![](images/e7e8eb89f82517e0dcebec48a02e5d2e345c9ac7dfad4c236e79416fd54e315b.jpg)  
图2-14

# 2.3 Sets 实现原理与实战

Sets 的功能类似 Java 中的 HashSet，是通过散列表实现的，所以添加、删除、查找元素的时间复杂度是 $O(1)$ 。

# 2.3.1 无序和唯一

Sets 是字符串类型的无序集合，集合中的元素是唯一的，不会出现重复的数据。Java 的 HashSet 底层是用 HashMap 实现的，Sets 的底层数据结构是用散列表实现的，散列表的 key 存储的是 Sets 中元素的 value，散列表的 value 指向 NULL。

不同的是，当元素内容都是64位以内的十进制整数，并且元素个数不超过set-max-intset-entries配置的值（默认为512）时，Sets会使用更加省内存的intset（整形数组）来存储，如图2-15所示。

![](images/28de635ec4c7bf7099ad75ef2cba2a74d3529f52cf9edc6e9bed324e1b8c7832.jpg)

![](images/931628f78418487ee439d4d35efc8226656d6fa837904ce7f18a542b0608c496.jpg)  
图2-15

# 使用场景

当你需要存储多个元素，并且要求不能出现重复数据，无须考虑元素的有序性时，可以使用 Sets。Sets 还支持在集合之间做交集、并集、差集操作，例如统计如下场景中多个集合元素的聚合结果。

统计多个元素的共有数据（交集）。  
对于两个集合，统计其中的一个独有元素（差集）。  
统计多个集合的所有元素（并集）。

常见的使用场景如下。

$\odot$ 社交软件中共同关注：通过交集实现。  
每日新增关注数：对近两天的总注册用户量集合取差集。  
◎ 打标签：你可以为自己收藏的每一篇文章打标签，例如微信收藏功能，这样可以快速地找到被添加了某个标签的所有文章。

# 2.3.2 intset

先看intset结构，结构体定义在源码intset.h中。

```c
typedef struct intset{ uint32_t encoding; uint32_t length; int8_t contents[]; }intset; 
```

长度：记录整数集合存储的元素个数，其实就是contents数组的长度。  
© contents: 真正存储整数集合的数组, 是一块连续内存区域。每个元素都是一个数组元

素，数组中的元素会按照值的大小从小到大存储，并且不会有重复元素。

$\odot$ encoding：编码格式，决定数组类型，一共有3种不同的值，如图2-16所示。

- INTSET_ENC_INT16: 表示 contents 数组的存储元素是 int16_t 类型的, 每 2 字节表示一个整数元素。  
- INTSET_ENC_INT32: 表示 contents 数组的存储元素是 int32_t 类型的, 每 4 字节表示一个元素。  
- INTSET_ENC_INT64: 表示 contents 数组的存储元素是 int64_t 类型的, 每 8 字节表示一个元素。

![](images/924645a0a478ba146f3a47c46970fdc7d5ec55c0be129476d719b99222d3c1b1.jpg)  
图2-16

MySQL：“如果在一个int16 t类型的intset中插入一个int64_t类型的值会怎样？

这个问题问得好。这种情况会触发intset升级，也就是Sets的所有元素都会转换成int64_t类型，步骤如下。

（1）根据新元素的类型和 Sets 元素的数量，计算包括新添加的元素在内的新的空间大小，对底层数组空间扩容，重新分配空间。  
（2）将intset中原有的元素都转换成新元素类型，把转换后的元素按照从大到小的顺序放到正确的位置上，需要保证intset元素的有序性。  
（3）将encoding的值修改为length + 1。

因此，每次向intset添加新元素都可能引起升级，升级又会对原始数据进行类型转换，时间复杂度是 $O(N)$ 。

MySQL：“如果删除刚刚添加的 int64_t 类型元素，那么会执行降级操作吗？”

intset 不支持降级操作。

MySQL：“Sets是无序集合，为何在存储整形数字的场景中contents数组元素需要有序？”

数组有序有助于使用二分法提高查询元素的效率。当 insetFind 函数的返回值为 0 时，表示 intset 中没有目标数据；当 insetFind 函数的返回值为 1 时，表示存在目标数据。方法内部

会调用intsetSearch函数使用二分法来查找数据。

static uint8_t insetSetSearch(intset \*is, int64_t value, uint32_t \*pos) (int min $= 0$ max $=$ intrrev32ifbe(is->length)-1,mid=-1; int64_t cur $= -1$ //省略一些检查代码 while(max >=min）{ mid $=$ ((unsigned int)min+(unsigned int)max) $>>1$ cur $=$ _*_*_*(is,mid); if(value $>$ cur){ min $=$ mid+1; }elseif(value $<$ cur){ max $=$ mid-1; }else{ break; }   
} //修改pos指针 if(value $= =$ cur）{ if(pos)\*pos $=$ mid; return 1; }else{ if(pos)\*pos $=$ min; return 0;   
}

如果查找到目标值，那么pos指针记录目标值的位置；如果查找不到目标值，那么pos指针记录的就是这个目标值插入intset的位置。

# 2.3.3 出招实战：共同好友

三国天下有限公司开发了一款名为“三国恋”的社交App，需要实现共同好友功能，这个场景就能通过交集来实现。为每个用户创建一个Sets集合，将账号名作为集合的key，集合value存储该账号的好友。如下命令构建刘备和曹操的好友集合。

```txt
SADDuser：刘备赵子龙张飞关羽貂蝉 SADDuser：曹操貂蝉夏侯惇典韦张辽
```

想要知道两个人的共同好友，也就是两个集合的交集，只需要使用 SINTERSTORE 命令。

SINTERSTORE user: 曹刘好友 user: 刘备 user: 曹操

命令执行后，刘备与曹操两个集合的交集数据就存储到了“user:曹刘好友”集合中。使用SMEMBERS查看曹操与刘备的共同好友，如图2-17所示。

```txt
redis> SMEMBERS user: 曹刘好友  
1) "貂蝉" 
```

好家伙，他们都喜欢貂蝉，你喜不喜欢呢？

![](images/8cfa096a67fefba975f79a954f64205cb9904325eabc7d83c45f66f493f88b15.jpg)  
图2-17

# 2.4 散列表实现原理与实战

# 2.4.1 field-value pairs 集合

散列表是一种 field-value pairs 集合类型，类似于 Java 中的 HashMap。一个 field 对应一个 value。

Redis的散列表的底层数据结构通常是dict，由数组和链表构成，数组元素占用的槽位叫作哈希桶，当出现散列冲突时就会在这个桶下挂一个链表，用“拉链法”解决散列冲突的问题，如图2-18所示。

![](images/4d5184ced6a821574d21577386788b9189e15cd7e8c8bf3779b8b04d23aac79a.jpg)  
散列表底层数据结构  
图2-18

# 2.4.2 dict和listpack

散列表的底层存储数据结构实际上有两种。

$\odot$ dict数据结构。  
©扎实推进（7.0版本之前使用zipist）数据结构。

在通常情况下，使用dict数据结构存储数据，每个field-valuepairs构成一个dictEntry节点。只有同时满足以下两个条件，才会使用listpack数据结构来代替dict，按照field在前value在后、紧密相连的方式依次把每个field-valuepairs放到列表的表尾。

每个field-valuepairs中的field和value的字符串的字节数都小于hash-max-listpack-value配置的值（默认为64）。  
$\odot$ field-value pairs数量小于hash-max-listpack-entries配置的值（默认为512）。

每次向散列表写数据时，都会调用 t_hash.c 中的 hashTypeConvertListpack 函数来判断是否需要转换底层数据结构。

当插入和修改的数据不满足以上两个条件时，就把散列表底层存储的数据结构转换成dict。需要注意的是，不能由dict退化成listpack。

虽然使用了 listpack 就无法实现 O(1) 时间复杂度操作数据，但是能大大减少内存占用，而且由于数据量比较小，性能不会有太大差异。

散列表使用 listpack 存储数据时的情况如图 2-19 所示。

![](images/81e235d875b8ad931c4a1efdd78aec64e2cee11afd8445050ffb879902a184b8.jpg)  
图2-19

接下来带你揭秘dict到底是什么样子的。Redis数据库就是一个全局散列表。在正常情况下，我只会使用ht_table[0]散列表，图2-20是一个没有进行rehash的dict字典。

![](images/42967b35e8fb921f19656536c93f027eae0a2d795fdeef95e004e3cbcfcc7a39.jpg)  
图2-20

在源码dict.h中使用dict结构体表示Redis全局散列表。

```c
struct dict {
    typedef *type;
    // 真正存储数据的地方，分别存放两个指针
    dictEntry **ht_table[2];
    unsigned long ht_used[2];
    long rehashidx;
    int16_t pauserehash;
    signed char ht_size_exp[2];
};
```

- dictType *type: 存放函数的结构体，定义了一些函数指针，可以通过配置自定义函数，实现在dict的key和value中存放任何类型的数据。  
- dictEntry **ht_table[2]: 存放大小为 2 的散列表指针数组，每个指针指向一个 dictEntry 类型的散列表。  
© ht_used[2]：记录每个散列表使用了多少槽位。  
© rehashidx: 标记是否正在执行 rehash 操作，-1 表示没有进行 rehash。如果正在执行 rehash 操作，那么其值表示当前执行 rehash 操作的 ht_table[0] 散列表 dictEntry 数组的索引。  
$\odot$ pauserehash：表示rehash的状态，大于0时表示rehash暂停，等于0时表示继续执行，小于0时表示出错。

继续看dictEntry，数组中每个元素都是dictEntry类型的，就是它存放了field-valuepairs。

```txt
typedef struct dictEntry {
    void *key;
    union {
        void *val;
        uint64 t u64;
        int64 t s64;
        double d;
    } v;
struct dictEntry *next;
} dictEntry; 
```

$\odot$ *key 指针指向 field-value pairs 中的 field，实际上指向一个 SDS 实例。  
$\odot$ v 是一个 union 联合体，表示 field-value pairs 中的 value，同一时刻只有一个字段有 value，用联合体的目的是节省内存。

- *val: value 是非数字类型时使用该指针存储。  
- uint64_t u64: value 是无符号整数时使用该字段存储。  
- int64_t s64: value 是有符号整数时使用该字段存储。  
- doubled: value 是浮点数时使用该字段存储。

$\odot$ *next是指向下一个节点的指针，当散列表数据增加时，可能出现不同的field得到的哈希值相等，也就是多个field对应一个哈希桶的情况，这就是哈希冲突。Redis使用拉链法，也就是用链表将数据串起来。

MySQL：“为什么 ht_table[2] 存放了两个指向散列表的指针？用一个散列表不就够了么。”

默认使用ht_table[0]读/写数据，当散列表的数据越来越多时，哈希冲突严重会导致哈希桶的链表比较长，使查询性能下降。当散列表保存的field-valuepairs太多或者太少时，需要通过rehash对散列表进行扩/缩容。

# 扩容和缩容

Redis扩容和缩容的步骤如下。

（1）为了提高性能，减少哈希冲突，会创建一个大小等于ht_used[0] $\star 2$ 的散列表ht_table[1]，也就是每次扩容时根据散列表ht_table[0]已使用空间扩大一倍创建一个新散列表ht_table[1]。反之，如果是缩容操作，就根据ht_table[0]已使用空间缩小一半创建一个新的散列表。  
（2）重新计算field-valuepairs的哈希值，得到这个field-valuepairs在新散列表ht_table[1]中的桶位置，将field-valuepairs迁移到新的散列表上。  
（3）所有field-valuepairs迁移完成后，修改指针，释放空间。把ht_table[0]指针指向扩容后的散列表，回收原来小的散列表的内存空间，把ht_table[1]指针指向NULL，为下次扩/缩容做准备。

MySQL：“什么时候会触发扩容？”

$\odot$ 当前没有执行bgsave或者BGREWRITEAOF命令，同时负载因子大于或等于1。也就是当前没有RDB子进程和AOF重写子进程在工作，毕竟这两个操作比较容易对性能造成影响，就不扩容“火上浇油”了。  
$\odot$ 正在执行bgsave或者BGREWRITEAOF命令，负载因子大于或等于5。这时哈希冲突太严重了，再不触发扩容，查询效率就太低了。

负载因子 $=$ 散列表存储的dictEntry节点数量／哈希桶个数。在理想情况下，每个哈希桶存储一个dictEntry节点，这时负载因子 $= 1$ 。

MySQL：“需要迁移的数据量很大，rehash 操作岂不是会长时间阻塞主线程？”

为了防止阻塞主线程造成性能问题，我并不是一次性把全部的 key 迁移，而是分多次将迁移操作分散到每次请求中，避免集中式 rehash 造成长时间阻塞，这个方式叫渐进式 rehash。

在执行渐进式rehash期间，dict会同时使用ht_table[0]和ht_table[1]两个散列表，rehash的具体步骤如下。

（1）将rehashidx配置为0，表示rehash开始执行。  
（2）在rehash期间，服务端每次处理客户端对dict散列表执行添加、查找、删除或者更新操作时，除了执行指定操作，还会检查当前dict是否处于rehash状态，如果是，就把散列表ht_table[0]上索引位置为rehashidx的哈希桶的链表的所有field-valuepairs rehash到散列表ht_table[1]上，该哈希桶的数据迁移完成，将rehashidx的值加1，表示下一次要迁移的哈希桶所在的位置。  
（3）当所有的 field-value pairs 迁移完成后，将 rehashidx 配置为 -1，表示 rehash 操作已完成。

MySQL：“在rehash过程中，字典的删除、查找、更新和添加操作，要在两个ht_table中都做一遍吗？”

删除、修改和查找可能会在两个散列表中进行，第一个散列表中没找到就到第二个散列表中查找。但是增加操作只会在新的散列表上进行。

MySQL：“如果请求比较少，岂不是要长时间使用两个散列表。”

好问题，在RedisServer初始化时，会注册一个时间事件，定时执行serverCron函数，其中包含rehash操作用于辅助迁移，以避免这个问题。

# 2.4.3 出招实战：购物车

在线购物 App 的购物车应具备如下功能，如图 2-21 所示。

$\odot$ 添加商品到购物车。  
$\odot$ 浏览购物车的所有商品。  
$\odot$ 更新某个商品的数量（增加或者减少）并查看商品信息（价格、图片、描述等）。  
删除商品。  
清空购物车。

![](images/cd7d5e3a2b12fb893a547915aa4a4136608b228efcb6e4a74add77fc839ca61f.jpg)  
图2-21

这里仅讨论购物车的模型设计，不涉及购物车与数据库的同步、购物车与订单的关系等问题。为每个用户创建一个散列表存储购物车信息，key = shoppingCart:用户ID，value就是购物车信息，如图2-22所示。

![](images/991365a9fafaddda44f32d2c95eb007cae9c35aa7d2561f611e54bf5d8a9f966.jpg)  
图2-22

# 添加商品

将商品的编码作为 field，购买数量作为 value，如果要添加商品就向集合中新增 field-value pairs。假设用户的 ID 是 660，鼠标的商品编码为 SUPPLY，耳机的商品编码为 WF-1000XM4，可以通过如下命令添加两个商品到购物车。

```txt
HMSET shoppingCart:660 SUPPLY 1 WF-1000XM4 1 
```

# 修改商品数量

多买一个 WF-1000XM4 降噪耳机，买俩！

```txt
HINCRBY shoppingCart:660 WF-1000XM4 1 
```

上面命令的含义是对 key 为 shoppingCart:660 的散列表中 field 是 WF-1000XM4 的 value 与给定值 1 做相加操作。如果想减少商品数量，就把参数改为 -1。

# 查看商品总量

查看购物车商品总数量，只要知道散列表中有多少个field-valuepairs即可。

```txt
HLEN shoppingCart:660 
```

# 全选

获取购物车中所有商品的商品编码和数量。

```txt
> HGETALL shoppingCart:660  
SUPPLY  
1  
WF-1000XM4  
2 
```

# 每个field后面紧跟value。

# 删除商品

删除购物车 shoppingCart:660 中编号为 SUPPLY 的鼠标，也就是删除散列表中 field = SUPPLY 的 field-value pairs。

```txt
HDEL shoppingCart:660 SUPPLY 
```

# 清空购物车

把整个散列表删除。

```batch
DEL shoppingCart:660 
```

# 查询商品明细

MySQL：“当前设计并没有提升购物车查询性能呀，还要使用商品编号去数据库查询商品明细信息（价格、图片地址、文字描述等）。”

问得好，商品明细信息是不会随着用户购物车中的商品变化的，本着分离变与不变的原则，你可以开辟一个独立的散列表专门保存商品明细信息，让查询商品明细的请求先从这个散列表中获取数据，当获取不到时再查询数据库，并把从数据库查到的数据写到这个散列表中。field保存商品编码，value存储商品明细信息的JSON字符串，如图2-23所示。

![](images/d2bc939afa2db6f80fd4efed98f846f4380f0b84eab91307165e2e4ae0361f4c.jpg)  
图2-23

按照如下命令创建一个名为 goods:info 的散列表，并把商品编号为 WF-1000XM4 的降噪耳机的图片地址、价格、描述信息序列化成 JSON 字符串保存到 value 中。

```txt
HSETNX "goods:info" "WF-1000XM4"
"[\\"price\":1899,\\"url\":\"https://www.xxx.com/ughgg\",\"description\":\"真无线蓝牙
降噪耳机\}")" 
```

HSETNX命令的作用是只有当field不存在时才配置value，否则“啥也不干”。

# 查询流程

（1）从散列表 shoppingCart:660 中查到商品的编码和价格。  
（2）根据商品编码去散列表 goods:info 中查找商品明细信息，如果查找不到则从数据库中查找，并把查到的数据通过 HSETNX 命令写到散列表中，以便下次查询时从 Redis 中获取，提高性能。

# 2.5 Sorted Sets 实现原理与实战

# 2.5.1 有序性和唯一性

Sorted Sets 与 Sets 类似，是一种集合类型，这种集合中不会出现重复的 member（数据）。它们之间的区别在于，Sorted Sets 中的元素由两部分组成，分别是 member 和 score（分数）。

member 会关联一个 double 类型的 score，Sorted Sets 默认会根据这个 score 对 member 从小到大进行排序，如果 member 关联的 score 相同，则按照字符串的字典顺序排列，如图 2-24 所示。

![](images/9906de8db70513f353bdc59417769d153109e6846bd29bc467340b309e8d2f0f.jpg)  
图2-24

常见的使用场景如下。

$\odot$ 排行榜，例如维护大型在线游戏中根据分数排名的Top10有序列表。  
$\odot$ 速率限流器，根据排序集合构建滑动窗口速率限制器。  
$\odot$ 延迟队列，使用score存储过期时间，从小到大排序，最靠前的就是最先到期的数据。

# 2.5.2 skiplist + dict 和 listpack

Sorted Sets 底层通过两种方式存储数据。

©扎实推进（在7.0版本之前是ziplist）：使用条件是集合元素个数小于或等于zset-max-listpack-entries的配置值（默认为128），且member占用字节数小于zset-max-listpack-value的配置值（默认为64）。将member和score紧凑排列作为zippack的一个元素存储。  
- skiplist + dict：当不满足上述条件时，将数据分别存储在 skiplist（跳表）和 dict 中，是一种用空间换时间的思想。散列表的 key 存储的是元素的 member，value 存储的是 member 关联的 score。

MySQL: “也就是说，listpack 适用于元素个数不多且元素占用空间不大的场景。”

对，使用 listpack 存储的目的就是节省内存。Sorted Sets 能支持高效的范围查询，正是因为采用了 skiplist，例如 ZRANGE 命令的时间复杂度为 $O(\lg n) + m$ ， $n$ 是 member 的个数， $m$ 是返回结果数。需要注意的是，应该避免返回大量结果。

而使用dict的目的是实现以 $O(1)$ 时间复杂度查询单个元素，例如ZSCOREkeymember命令。总而言之，Sorted Sets在插入或者更新时，会同时向skiplist和dict中插入或更新对应的数据，以保证skiplist和dict的数据一致。

MySQL：“这个方式很巧妙呀，skiplist用来根据score进行范围查询或单个查询，dict则用于实现以 $O(1)$ 时间复杂度查询对应score，满足高效范围查询和单元素查询的要求。”

Sorted Sets 实现源码主要在以下两个文件中。

$\odot$ 结构定义在 server.h 中。  
$\odot$ 功能实现在 t_zset.c 中。

一起来看 skiplist + dict 数据结构如何存储数据。

skiplist $^+$ dict

MySQL：“说说什么是skiplist吧。”

skiplist 的本质是一种可以进行二分查找的有序链表。skiplist 在原有的有序链表上增加了多级索引，通过索引来实现快速查找。

skiplist不仅能提高搜索性能，还可以提高插入和删除操作的性能。它在性能上和红黑树、AVL树不相上下，但是原理和实现比红黑树简单。

回顾链表，如图2-25所示，它的痛点是查询很慢，时间复杂度为 $O(n)$ ，唯快不破的Redis是不能忍的。

![](images/5c6397f328f7bcb461c5b5aa363b73a53b4c8f39d8a44b24c31cea2adacee5db.jpg)  
图2-25

如果在有序链表的每相邻两个节点增加一个“跳跃”指向下下个节点的指针，那么查找的时间复杂度就可以降低为原来的一半，如图2-26所示。

![](images/9857965404cd397cdb4fe6f67099a1b4b1b95f004a0ccb44eaadd23546b69b88.jpg)  
图2-26

这样level0和level1分别形成两个链表，level1的链表只有2个节点（6、26）。

skiplist节点查找

通常，数据查找是从顶层开始的，如果节点保存的值比待查数据的值小，skiplist就继续访问该层的下一个节点。

如果遇到比待查数据的值大的节点，就跳到当前节点的下一层的链表继续查找。例如现在想查找17，查找的路径如图2-26中虚线箭头所示。

![](images/24098638096bf545042ed7afb1b876f24d1f7a5f8ce0b65a8430d984bbef2159.jpg)  
图2-27

© 从 level1 开始，17 大于 6，继续与下一个节点比较。  
$17 < 26$ ，回到原节点，跳到当前节点的level0层链表，与下一个节点比较，找到目标17。

skiplist正是受这种多层链表的启发设计出来的。根据上面的生成链表，上层链表的节点个数是下面一层的一半，这样的查找过程类似于二分查找，时间复杂度为 $O(\mathrm{Ign})$ 。

但是，这种方式在插入数据时有很大问题，每次新增一个节点，就会打乱相邻的两层链表节点个数为 $2:1$ 的关系，如果要维持这个关系，就需要调整链表，时间复杂度是 $O(n)$ 。

为了避免这个问题，skiplist不要求上下相邻的两层链表的节点个数有严格的比例关系，而是为每个节点随机出一个层数，这样插入节点时只需要修改前后的指针。

图2-28是一个有4层链表的skiplist，假设我们要查找26，图中虚线箭头就是查找路径。

![](images/12835c758f538e8746332ba99a5d38249e2ac7c56c5c1aa8e908f5ecf70f7083.jpg)  
图2-28

对经典skiplist有一个直观的印象后，再来看Redis中skiplist的实现细节，Sorted Sets数

据结构的定义如下。

```c
typedef struct zset{ dict \*dict; zskiplist \*zsl; }zset; 
```

zset 结构体中有两个变量,分别是 dict 和 zskiplist。dict 在前文已经讲过,重点看 zskiplist。

```txt
typedef struct zskiplist{ //头、尾指针便于双向遍历 struct zskiplistNode \*header，\*tail; //当前skiplist包含元素个数 unsigned long length; //表内节点的最大层级数 int level;   
}zskiplist;
```

$\odot$ zskiplistNode *header, *tail：头、尾指针，用于实现双向遍历。  
length: 链表包含的节点总数。需要注意的是, 新创建的 zskiplist 会生成一个空的头指针, 它不包含在 length 的计数中。  
$\odot$ level：表示 skiplist 中节点的最大层级数，skiplist 中的节点可以拥有多个层级，每个层级是一个链表结构，通过不同层级的指针可以实现跳跃式查询。

继续看skiplist中每个节点，由zskiplistNode结构体来表示。

```c
typedef struct zskiplistNode {
    fds ele;
    double score;
    struct zskiplistNode *backward;
    struct zskiplistLevel {
        struct zskiplistNode *forward;
        unsigned long span;
    } level {};
} zskiplistNode; 
```

- ele 和 score 属性：Sorted Sets 既要保存元素，又要保存元素的权重，使用 SDS 类型的 ele 存储实际内容，double 类型 score 保存权重。  
$\odot$ *backward：后退指针，指向该节点的上一个节点，便于从尾节点实现倒序查找。注意，每个节点只有一个后退指针，只有level0层链表是双向链表。  
$\odot$ level[]：zskiplistLevel 结构体类型的柔性数组。skiplist 是一个多层的有序链表，每一层的节点由指针链接起来，所以数组中每个元素都代表 skiplist 的一层。

$\bullet$ *forward：前进指针。  
- span: 跨度, 用来记录节点在该层的 *forward 指针到指针指向的下一个节点之间跨

越了level0层的节点数。可以计算元素排名（rank），例如查找 $\mathrm{ele} =$ 肖菜姬、score $=$ 17的排名，只需要把查找路径经过的节点的span相加即可，如将图2-29中虚线路径的span相加， $\mathrm{rank} = (2 + 2) - 1 = 3$ （减1是因为rank从0开始）。如果要按照从大到小的顺序计算排名，那么只需用skiplist的长度减去查找路径上的span累加值，即 $4 - (2 + 2) = 0$ 。

![](images/9de44e4f1a4fce57ffc0e17755bf4431dd337349ce442854ca54ac130a205e16.jpg)  
图2-29

# listpack

MySQL：“根据zset结构体的定义可知，它分别使用了dict、skiplist两种数据结构，listpack的影子都见不着呀。”

这个问题问得好，使用 listpack 存储的细节在源码文件 t_zset.c 中的 zaddGenericCommand 函数中体现，部分代码如下，其内部会判断是否使用 listpack 来存储。

voidzaddGenericCommand(client*c,intflags){ //省略部分代码 //key不存在则创建sorted set obj $=$ lookupKeyWrite(c->db,key); if(checkType(c,zobj,OBJ_ZSET))goto cleanup; if(zobj $\equiv$ NULL）{ if(xx)goto reply_to_client; //如果zset_maxlistpack_entries $\equiv = 0$ 或者 //元素字节大小大于zset_maxlistpack_value配置 //则使用skiplist $^+$ dict存储，否则使用listpack if (server.zset_maxlistpack_entries $= = 0$ || server.zset_maxlistpack_value<sdslen(c->argv[ scoreidx+1]->ptr))

```txt
zobj = createZsetObject();
} else {
    zobj = createZsetListpackObject();
}
dbAdd(c->db, key, zobj);
} // 省略部分代码 
```

listpack 是一块由多个数据项组成的连续内存。采用 listpack 插入 member-score 数据时，每个 member-score 数据对紧凑排列。如图 2-30 所示。

![](images/572960b0a4e0a8604e250f8d96a3bb42b0917055c5e1b7de7aaf78b8e5deb487.jpg)  
图2-30

listpack最大的优势就是节省内存，但只能按顺序查找元素，时间复杂度是 $O(n)$ 。正因如此，才能在少量数据的情况下，既节省内存，又不影响性能。每一步查找前进两个数据项，也就是跨越一个member-score数据对。

# 2.5.3 出招实战：游戏排行榜

很多地方都会用到排行榜功能，例如微博热榜、知乎热榜、电影排行榜、游戏战力排行榜等。我教你使用 Sorted Sets 实现一个实时游戏高分排行榜。

玩家的得分越高，排名越靠前，如果分数相同则先达到该分数的玩家排在前面，游戏排行榜提供的功能如下。

按照分数从高到低排名，查询前 $N$ 位玩家的信息。  
© 新注册玩家，需要把新玩家信息添加到排行榜中。  
© 能查看某个玩家的排名和分数。

Sorted Sets 的每个元素都由 member 和 score 两部分组成，可以利用 score 进行排序，正好满足我们的需求。用 score 保存玩家的游戏得分，member 保存玩家 ID。

程许媛：“分数相同，先达到该分数的排在前面，也就是说，在游戏分数相同的情况下，时间戳越小，排名越靠前，怎么实现？”

这个问题问得好，既然时间也会影响排名，就把时间戳考虑到 score 中。

程许媛：“有问题！分数越大，排名越靠前；而时间戳越小，排名越靠前。两个规则是相反的，怎么结合在一起。”

好问题，这时候你可以指定一个非常大的时间作为基准时间，例如这个时间就是你当年信誓旦旦的对那个女孩说的“如果非要在这份爱上加一个期限，我希望是……一万年”，也就是 $2024 + 10000$ 年。

时间排序值 $=$ （基准时间-玩家达到分数时间）/基准时间

以上公式得到的结果一定小于1，正好可以作为score的小数部分。越早达到，这个值就越大，满足排序要求。

```c
score = 玩家游戏分 + [(基准时间-玩家获得某分数时间)/基准时间]

通过上面的公式，可以实现当分数相同时，用时越短排名越靠前的功能。

```txt
private double calcScore(int playerScore, long playerScoreTime) {
    return playerScore + (BASE_TIME - playerScoreTime) * 1.0 / BASE_TIME; 
```

© playerScore：玩家游戏分。  
© playerScoreTime：玩家获得某分数时间，单位为秒。  
$\odot$ BASE_TIME：基准时间，单位为秒。

当需要获取玩家的游戏分数时，取整数位即可。接下来演示如何使用zset命令实现排行榜。假设BASE_TIME为2023年1月1日0时0分0秒的时间戳秒数 $= 317242022400$

# 更新排行榜

使用命令ZADD key score member [score member...]新增或者更新玩家排行榜。如下命令表示新增了4个玩家信息到排行榜。leaderboard:339作为key，表示区服339战力排行榜，玩家2和玩家3的战力都是500，玩家3比玩家2先到达500战力。

```txt
redis> ZADD leaderboard:339 2500.994707057989 player:1 (integer) 1  
redis> ZADD leaderboard:339 500.99470705798905 player:2 (integer) 1  
redis> ZADD leaderboard:339 500.9947097814618 player:3 (integer) 1  
redis> ZADD leaderboard:339 987770.994707058 player:4 (integer) 1 
```

假设玩家4的女朋友不在家，他天天玩游戏，战力提升到1987770。player:4的score更新为1987770.994707055。

```html
ZADD leaderboard:339 1987770.994707055 player:4 
```

获取Top3玩家排行信息

ZRANGE 命令可以按照排名、score、字典排序进行范围查询。语法使用规则如下。

```sql
ZRANGE key start stop [BYSCORE | BYLEX] [REV] [LIMIT offset count] [WITHSCORES] 
```

默认按照 score 由低到高排序，如果分数相同则根据 member 字典排序。

© REV: 可选参数，按照 score 由高到低逆序排列。  
$\odot$ LIMIT offset count：可选参数，类似于MySQL的分页功能，offset是查询的起始位置，count是条数。需要注意的是，count为负数则返回所有符合条件的数据。  
© WITHSCORES：可选参数，返回 score 和 member，返回的格式是 member 1, score 1,..., member N, score N。

你可以使用 REV 来实现逆序，WITHSCORES 返回 member 和 score。如下命令从 key 为 leaderboard:339 的 Sorted Sets 中按照 score 逆序获取 3 个元素。

```txt
> ZRANGE leaderboard:339 0 2 REV WITHSCORES  
player:4  
1987770.9947070549  
player:1  
2500.9947070579892  
player:3  
500.99470978146178 
```

# 获取指定玩家排名

我提供了 ZREVRANK 命令，用于返回指定 member 的排名，需要注意的是，排名从 0 开始。如下命令查找 player:4 的排名，0 表示第一。

```txt
> ZREVRANK leaderboard:339 player:40 
```

# 2.6 Stream 实现原理与实战

我在2.1.2节说过，使用Lists实现消息队列有很多局限性。

$\odot$ 没有 ACK 机制。  
© 没有类似 Kafka 的消费者组（Consumer Group）概念。  
消息堆积。  
$\odot$ Lists是线性结构，查询指定数据需要遍历整个列表。

# 2.6.1 支持消费者组的轻量级MQ

Stream是Redis5.0专门为消息队列设计的数据类型，借鉴Kafka的消费者组的设计思路，提供消费者组的概念，同时提供消息的持久化和主从复制机制。客户端可以访问任何时刻

的数据，并且能记住每个客户端的访问位置，从而保证消息不丢失。

以下是Stream类型的主要特性。

$\odot$ 使用RadixTree和lismpack结构来存储消息。  
$\odot$ 序列化生成消息ID。  
© 借鉴 Kafka 消费者组的概念，将多个消费者划分到不同的消费者组中。当消费同一个Stream 时，同一个消费者组中的多个消费者可以并行但不重复消费，提升消费能力。  
$\odot$ 支持多播（多对多）、阻塞和非阻塞读取。  
ACK 确认机制，保证了消息至少被消费一次。  
◎ 可配置消息保存上限阈值，我会把历史消息丢弃，防止内存占用过大。

需要注意的是，RedisStream是一种超轻量级的MQ，并没有完全实现消息队列的所有设计要点，所以它的使用场景需要考虑业务的数据量和对性能、可靠性的需求。

对于系统消息量不大、可以容忍数据丢失的场景，使用RedisStream作为消息队列就能享受高性能快速读/写消息的优势。

# 2.6.2 Radix Tree 的奥秘

每个Stream都有唯一的名称，作为Stream在Redis中的key。Stream支持将xadd命令添加数据到Stream中，如果Stream不存在会自动创建一个Stream。

Stream 存储在 Radix Tree 树上，树上的节点存储一个 field-value pairs, key 存储消息 ID, value 存储的是指向保存消息内容的 listpack 指针。

Stream 就像一个仅追加内容的消息链表，把消息一个个串起来，每个消息都有唯一的 ID 和消息内容，消息内容由多个 field-value pairs 组成。Stream 底层使用 Radix Tree 和 listpack 数据结构存储数据。

为了便于理解，我将 Radix Tree 变形，使用列表来体现 Stream 中消息的逻辑有序性，如图 2-31 所示。

这张图涉及很多概念，但是你不要慌。我一步步拆开说，最后你再回头看就懂了。

Consumer Group：消费者组，每个消费者组可以有一个或者多个消费者，消费者之间是竞争关系。不同消费者组的消费者之间无任何关系。  
@ *pel: 全称是 Pending Entries List，记录了当前被客户端读取但是还未 ACK（Acknowledge character，确认字符）的消息。如果客户端未 ACK，*pel 的消息 ID 就会越来越多。

![](images/4de7e5a752356178b156d4eae39059ca403162fdd5aa76d8ca2936aa22e52654.jpg)  
图2-31

# Stream 结构

stream.h 源码中的 Stream 结构体如下。

```c
typedef struct stream {
    rax *rax;
    uint64_t length;
    streamID last_id;
    streamID first_id;
    streamID max_deleted_entry_id;
    uint64_t entries-added;
    rax *cgroups;
} stream;
typedef struct streamID {
    uint64_t ms;
    uint64_t seq;
} streamID; 
```

$\odot$ *rax: rax 的指针，指向一个 Radix Tree, key 存储消息 ID, value 实际上指向一个 listpack 数据结构，存储了多条消息，每条消息的 ID 都大于或等于这个 key 的消息 ID。

长度：该Stream的消息条数。  
- streamID: 结构体, 消息 ID 的抽象, 一共占 128 位, 内部维护毫秒时间戳 (字段 ms) 和 1 毫秒内的自增序号 (字段 seq), 用于区分同一毫秒内插入的多条消息。  
© last_id: 当前 Stream 最后一条消息的 ID。  
© first_id: 当前Stream第一条消息的ID。  
$\odot$ max_deleted_entry_id：当前Stream被删除的最大的消息ID。  
$\odot$ entries-added：添加到Stream中的消息总数，entries-added $=$ 已删除消息条数 $+$ 未删除消息条数。  
$\odot$ *cgroups: rax 指针，指向一个 Radix Tree，记录当前 Stream 的所有消费者组，每个消费者组的名称都是唯一标识，并作为 Radix Tree 的 key，消费者组实例作为 value。

# 消费者组

每个Stream可以有多个消费者组，一个消费者组可以有多个消费者同时对组内消息进行消费。消费者组由streamCG结构体定义，源码如下。

```c
/\*ConsumerGroup\*/ typedefstruct streamCG{ streamIDlast_id; longlongentries_read; rax \*pel; rax \*consumers;   
}streamCG; 
```

结构体中每个属性代表的含义如下。

© last_id: 该消费者组的消费者已经读取但还未 ACK 的最后一条消息 ID。  
$\odot$ *pel: pending entries list 的简写，指向一个 Radix Tree 的指针，保存着消费者组中所有消费者读取但还未 ACK 的消息，就是它实现了 ACK 机制。该树的 key 是消息 ID, value 关联一个 streamNACK 实例。  
$\odot$ *consumers: Radix Tree 指针，表示消费者组中的所有消费者，key 是消费者名称，value 指向一个 streamConsumer 实例。

# streamNACK

每个 streamCG 持有一个 *pel 指针，指向一个 streamNACK 实例，streamNACK 结构体用于抽象消费者已经读取，但是未 ACK 的消息 ID 的相关信息。streamNACK 结构体的源码如下。

```txt
/* Pending (yet not acknowledged) message in a consumer group. */ typedef struct streamNACK {
    mstime_t delivery_time;
    uint64_t delivery_count; 
```

```txt
streamConsumer *consumer;
} streamNACK; 
```

每个属性代表的含义如下。

© delivery time: 该消息最后一次推送给消费组的时间戳。  
$\odot$ delivery_count: 消息被推送的次数。  
$\odot$ *consumer：消息推送的消费者客户端。

# streamConsumer

消费者组中对消费者的抽象使用了定义 streamCG 结构体中的*consumers 指针，它指向 streamConsumer 结构体，用于表示消费者。streamConsumer 的源码如下。

```txt
/\*A specific consumer in a consumer group. \*/ typedef struct streamConsumer{ mtime_t seen_time; sds name; rax \*pel; } streamConsumer; 
```

每个属性代表的含义如下。

© seen_time: 消费者最近一次被激活的时间戳。  
name: 消费者名称。  
$\odot$ *pel: RadixTree 指针，对于同一个消息而言，streamCG->pel 与 streamConsumer ->pel 的 streamNACK 实例是同一个。

streamCG、streamNACK、streamConsumer之间的关系如图2-32所示。

```txt
肖莱姬：“Redis 你好，Stream 如何结合 Radix Tree 和 listpack 结构来存储消息？为什么不使用 dict 来存储，将消息 ID 作为 dict 的 key，dict 的 value 则存储消息 field-value pairs 内容？”
```

在回答之前，先将几条消息插入Stream，让你对Stream消息的存储格式有个大体认知。该命令的语法如下。

```txt
XADD key id field value [field value ...] 
```

Stream 中的每个消息可以包含不同数量的 field-value pairs，在成功写入消息后，我会把消息的 ID 返回给客户端。

![](images/6bbf7198475ba67cde5d49b980b2637f4b342da2157a7fb1e997613a218a815a.jpg)  
图2-32

执行如下命令把用户购买书籍的消息存放到 hotlist:books 队列中，消息内容主要包括 payerID、amount 和 orderID。

```txt
> XADD hotlist:books * payerID 1 amount 69.00 orderID 9  
1679218539571-0  
> XADD hotlist:books * payerID 1 amount 36.00 orderID 15  
1679218572182-0  
> XADD hotlist:books * payerID 2 amount 99.00 orderID 88  
1679218588426-0  
> XADD hotlist:books * payerID 3 amount 68.00 orderID 80  
1679218604492-0 
```

hotlist:books 是 Stream 的名称，后面的“*”表示让 Redis 为插入的消息自动生成唯一 ID，你也可以自定义。

消息ID由以下两部分组成。

$\odot$ 当前毫秒内的时间戳。  
顺序编号。起始值为 0，用于区分同一时间内产生的多个命令。

肖莱姬：“如何理解Stream是一种只执行追加操作（append only）的数据结构？”

通过将元素 ID 与时间进行关联，并强制要求新元素的 ID 必须大于旧元素的 ID，Redis 从逻辑上将 Stream 变成了一种只执行追加操作的数据结构。

用户可以确信，新的消息和事件只会出现在已有消息和事件之后，一切都是有序进行的。

肖莱姬：“插入的消息ID大部分相同，例如上例中4条消息的ID的前缀都是1679218。另外，每条消息field-valuepairs的field通常是一样的，例如上例中4条消息的field都是payerID、amount和orderID。使用dict存储会有很多冗余数据，你这么抠门，所以不使用dict对不对？”

没毛病，小老弟很聪明。为了节省内存，我使用了 Radix Tree 和 listpack。Radix Tree 的 key 存储消息 ID，value 则使用 listpack 数据结构存储多个消息，listpack 中的消息 ID 都大于或等于 key 存储的消息 ID。

我在前面已经讲过，listpack非常节省内存。而RadixTree数据结构的最大特点是适合保存具有相同前缀的数据，从而节省内存。那么，RadixTree到底是怎样的数据结构？继续往下看。

# Radix Tree

Radix Tree（前缀树）也被称为 Radix Treie，或者 Compact Prefix Tree，用于高效地存储和查找字符串集合。它将字符串按照前缀拆分成一个个字符，并将每个字符作为一个节点存储在树中。

当插入一个field-valuepairs时，Redis会将field拆分成一个个字符，并根据字符在RadixTree中的位置找到合适的节点，如果该节点不存在，则创建新节点并添加到RadixTree中。

当所有字符添加完毕后，将值对象指针保存到最后一个节点中。当查询一个field时，Redis按照字符顺序遍历Radix Tree，如果发现某个字符不存在于树中，则表示field不存在；如果最后一个节点表示一个完整的field，则返回对应的值对象。

图2-33展示了一个简单的RadixTree，将根节点到叶子节点的路径对应的字符拼接起来，就得到了两个field（他说气堡了、他说气炸了）。

你应该已经发现，这两个field拥有公共前缀（他说气），RadixTree实现了共享，这样就可以避免相同字符串被重复存储。如果采用dict的方式保存，那么field的相同前缀就会被多次存储，导致内存浪费。

![](images/7f2fcdd596747c06aa28318ea73bdf5e9bebcb287f91bee8ecd1d28916280348.jpg)  
图2-33

# RadixTree改进

每个节点只保存一个字符，一是浪费内存空间，二是在进行查询时，还需要逐一匹配每个节点表示的字符，对查询性能也会造成影响。所以，Redis并没有直接使用标准Radix Tree，而是做了一次变形——Compact Prefix Tree（压缩前缀树）。

通俗来讲，当多个field具有相同的前缀时，就将相同前缀的字符串合并在一个共享节点中，从而减少存储空间。图2-34展示了几个field（test、toaster、toasting、slow、slowly）在RadixTree上的布局。

![](images/f5b20eb198bef751cc613f1ed499e05e2b6cb562c78996e4ec88395679c7a3ce.jpg)  
图2-34

由于 Compact Prefix Tree 可以共享相同前缀的节点，所以在存储一组具有相同前缀的 field 时，Redis 的 Radix Tree 相比其他数据结构（如 dict）占有更少的空间，并具有更快的查询速度。Radix Tree 节点的数据结构由 rax.h 文件中的 raxNode 定义。

```c
typedef struct raxNode {
    uint32_t iskey:1;
    uint32_t isnull:1;
    uint32_t iscompr:1;
    uint32_t size:29;
    unsigned char data[];
} raxNode; 
```

每个属性的含义如下。

$\odot$ iskey: 从 Radix Tree 根节点到当前节点组成的字符串是否是一个完整的 field。如果是，则 iskey 的值为 1。  
- isnull: 当前节点是否为空节点。如果是, 则不需要为该节点分配指向 value 的指针内存。  
© iscompr：是否为压缩节点。  
- size：当前节点的大小，会根据节点类型而改变。对于压缩节点，该值表示压缩数据的长度；对于非压缩节点，该值表示节点的子节点个数。  
© data[]：实际存储的数据，具体的存储内容根据节点类型不同而有所不同。

- 压缩节点：data 数组存储被压缩的字符串。  
- 非压缩节点：data 数据包含子节点对应的合并字符串、指向子节点的指针。  
- value 指针指向一个 listpack 实例，里面保存了消息的实际内容。

Radix Tree最大的特点就是适合保存具有相同前缀的数据，实现节省内存的目标，以及支持范围查找。这也是Stream采用RadixTree作为底层数据结构的原因。

# 2.6.3 出招实战：实现消费者组特性的消息队列

废话少说，实战。

周五下班前，靠“卷团队”获得业绩的领导李易卷为了实现KPI，获得更多的股票和年终奖，就对开发负责人张无剑说：“我建议明天还是赶一赶进度，隔壁老王也过来加班，否则下周风险会增加，你动员一下团队冲一冲！”。

张无剑担心自己和同事长时间处于高度紧张的状态下，会产生焦虑、烦躁等情绪，从而肝气不舒、脾胃失调。为了缓解压力，他打开美团 App，请组内成员一起吃大餐。顺便提一句，他的英文名叫 Double Joy，意为双倍快乐。

# 异步并行处理

下单的过程除了需要生成订单核心业务流程，还涉及赠送积分、优惠券发放，以及发送下单成功通知等一系列业务。假设每个业务节点耗时 $100\mathrm{ms}$ ，则串行处理需要 $400\mathrm{ms}$ ，而并行处理只需要 $200\mathrm{ms}$ 。消息队列可以起到异步并行处理的作用，从而减少请求响应时间，提高系统吞吐量，如图2-35所示。

![](images/868f03f9a9409170a3eb5efa5d9b1a59d231309d27a2b4374ee1fb8f65465119.jpg)  
图2-35

# 应用解耦

此外，通过消息队列，还实现了应用解耦，例如用户下单后，订单系统需要通知积分系统，订单系统将消息写入消息队列，积分系统订阅消息进行积分即可。

# 普通消费队列

# XADD 插入消息

XADD命令的主要作用是将消息有序插入末尾，并自动生成全局唯一ID。张无剑的下单请求到了订单中心生成订单后，可通过XADD将订单创建完成的消息发送到消息队列，让其他服务监听消息异步执行。

Stream 的每个元素都由 field-value pairs 构成，XADD 的语法如下。

```dockerfile
XADD streamName id field value [field value ...] 
```

如果订单系统执行如下命令，就是向名称为 order:doubleJoy 的消息队列插入一条消息，消息的内容表示张无剑的订单 ID 是 1，该订单买的是海鲜大餐（seafood），套餐编号是 68，消费金额是 598。消息的内容由 3 个 field-value pairs 组成，分别是 orderID -> 1、seafood -> 68 amount -> 598。

XADD order:doubleJoy $\star$ orderID 1 seafood 68 amount 598 "1685782062437-0"

队列名称后面的 *表示让 Redis 为插入的消息自动生成唯一 ID。当然，你也可以不用 *, 在名称后边设定一个自定义的 ID，只要保证这个 ID 全局唯一即可。

消息ID由两部分组成。

◎ 时间戳：插入消息时，精确到毫秒的当前服务器时间。  
顺序编号：起始值为0，用于区分同一时间内产生的多个命令。

将元素ID与时间进行关联，并强制要求新元素的ID必须大于旧元素的ID，Redis从逻辑上将Stream变成了一种只执行追加操作的数据结构。

新消息和事件只会出现在已有消息和事件之后，就像现实世界里新事件总是发生在已有事件之后一样，一切都是有序进行的。使用XLEN命令可以查看当前Stream有多少条消息。

```txt
XLEN order:doubleJoy (integer)1 
```

# XREAD 读取消息

张无剑：“积分系统如何读取队列的消息进行消费呢？”

如下命令的含义是：客户端用阻塞等待读取的方式从队头读取1个消息。

```txt
XREAD COUNT 1 BLOCK 0 STREAMS order:doubleJoy 0-0  
1) 1) "order:doubleJoy"  
2) 1) 1) "1685785480628-0"  
2) 1) "orderID"  
2) "1"  
3) "seafood"  
4) "68"  
5) "amount"  
6) "598" 
```

该命令可以同时对多个Stream进行读取。

```txt
XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...] 
```

$\odot$ COUNT：从每个Stream中最多读取的元素个数。  
$\odot$ BLOCK：阻塞读取，当消息队列没有新消息插入时，则阻塞等待，0表示无限等待，单位是毫秒。  
ID：消息ID，在读取消息时可以指定ID，并从这个ID的下一条消息开始读取。

- 0-0 表示从第一个元素开始读取。  
• “$”符号表示读取最新插入的消息。

如果想使用XREAD进行顺序消费，那么每次读取后要记住返回的消息ID，下次调用XREAD时将上一次返回的消息ID作为参数传递就可以继续消费后续的消息了。例如执行下面的命令，从ID为1685785480628-0的消息开始，读取下一条消息。

# Redis高手心法

```txt
XREAD COUNT 1 BLOCK 0 STREAMS order:doubleJoy 1685785480628-0  
1) 1) "order:doubleJoy"  
2) 1) 1) "1685785502168-0"  
2) 1) "orderID"  
2) "2"  
3) "茅台飞天"  
4) "79"  
5) "amount"  
6) "598" 
```

张无剑：“如何使用客户端阻塞等待的方式读取最新插入的消息？”

如下命令最后的“$”符号表示从尾部读取最新插入的消息,BLOCK 0 表示阻塞等待。需要注意的是,如果没有新消息插入,则一直阻塞等待。

```txt
XREAD BLOCK 0 STREAMS order:doubleJoy $ 
```

毫无疑问，这里不会返回任何消息，除非现在有新消息插入。

张无剑：“这么容易就实现消息队列了吗？说好的ACK机制呢？”

我们可以在不定义消费者组的情况下通过XREAD单独消费，将Stream当成普通队列使用。通过XREAD读取的数据其实并没有被删除，当重新执行XREAD BLOCK 0 STREAMS order:doubleJoy 0-0命令时又会重新读取所有数据。

```txt
XREAD BLOCK 0 STREAMS order:doubleJoy 0-0  
1) 1) "order:doubleJoy"  
2) 1) 1) "1685785480628-0"  
2) 1) "orderID"  
2) "1"  
3) "seafood"  
4) "68"  
5) "amount"  
6) "598"  
2) 1) "1685785502168-0"  
2) 1) "orderID"  
2) "2"  
3) "茅台飞天"  
4) "79"  
5) "amount"  
6) "598"  
3) 1) "1685785517633-0"  
2) 1) "orderID"  
2) "3"  
3) "鲍鱼"  
4) "88"  
5) "amount"  
6) "598" 
```

一个Stream可以有多个客户端（消费者）等待数据。在默认情况下，每条新消息都会被发

送到Stream中等待数据的消费者。

所有消息都无限期地存储在Stream中（除非用户明确要求删除消息），不同的消费者会通过收到的最后一条消息的ID来确定下一条要读取的消息。

# 消费者组

RedisStream的消费者组允许用户将一个Stream从逻辑上划分为多个不同的Stream，并让消费者组的消费者处理。如图2-36所示。

![](images/a16cc919d5edd88babd2d53c95d94cdfee999bd8cb6758496b098b9e277ccfcd.jpg)  
图2-36

支持多播的可持久化的消息队列借鉴了Kafka的设计。Stream高可用是建立在主从复制的基础上的，和其他数据结构一样，Stream也会被异步复制到副本并持久化到AOF和RDB文件中。也就是说，在哨兵和集群环境下，Stream是可以支持高可用的。

使用Stream作为消息队列有以下几个特性。

每个消费者组的状态都是独立的，互不影响，同一组Stream消息会被所有消费者组消费。  
- 一个消费者组可以由多个消费者组成，消费者之间是竞争关系，任意一个消费者读取消息都会使last Deliverd_id向前移动。  
每个消费者有一个*pel 变量，用于记录当前消费者读取了但是还未 ACK 的消息。它用来保证消息至少被客户端消费了一次。

消费者组的主要命令如下。

© XGROUP: 用于创建、销毁和管理消费者组。

© XREADGROUP：用于通过消费者组从Stream中读取消息。  
© XACK: 允许消费者将待处理消息标记为已正确处理，可以移除。  
XPENDING：显示已读取，但未 ACK 的消息的相关信息。  
© XINFO：查看Stream和消费者组的相关信息。

# XGROUP CREATE 创建消费者组

XGROUP CREATE 命令的语法如下，<> 标记的是必备参数，[] 标记的是可选参数。

```powershell
XGROUP CREATE $streamName $groupId <id | $> [MKSTREAM][ENTRIESREAD] 
```

© streamName：指定队列的名称。  
$\odot$ groupName：指定消费者组的名称。  
© <id | $>: 指定消费者组在Stream的ID, 它决定了消费者组从哪个ID之后开始读取消息, 0-0表示从队头开始读取,$表示从现在开始从队尾读取新插入的消息, 你也可以自定义ID。  
- MKSTREAM：在默认情况下，XGROUP CREATE 命令在 Stream 不存在时返回错误。使用可选 MKSTREAM 子命令作为最后一个参数来自动创建 Stream。

为消息队列 order:doubleJoy 创建 pointsGroup 和 couponGroup 两个消费者组，分别代表“积分服务消费者组”和“优惠券服务消费者组”。

```txt
XGROUP CREATE order:doubleJoy pointsGroup 0-0 MKSTREAM  
XGROUP CREATE order:doubleJoy couponGroup 0-0 MKSTREAM 
```

# XREADGROUP 读取消息

我是Redis，作为贴心哥，我为开发者提供了XREADGROUP命令来实现消费者组的组内消费者消费消息。

例如，积分服务的pointsGroup消费者组的消费者consumer1从名称为order:doubleJoy的Stream的队头阻塞读取一条消息的命令如下。

```txt
XREADGROUP GROUP pointsGroup consumer1 COUNT 1 BLOCK 0 STREAMS order:doubleJoy >   
1) 1) "order:doubleJoy"   
2) 1) 1) "1685785480628-0"   
2) 1) "orderID"   
2) "1"   
3) "seafood"   
4) "68"   
5) "amount"   
6) "598" 
```

语法如下。

```powershell
XREADGROUP GROUP $groupId $consumerName [COUNT count] [BLOCK milliseconds]
[NOACK] STREAMS streamName [streamName ...] id [id ...] 
```

该命令与XREAD大同小异，区别在于新增了GROUP.groupNameconsumerName选项。这两个参数分别用于指定Stream的消费者组及该消费者组中负责处理消息的消费者。

©roupName：消费者组名称。  
consumerName：消费者组的消费者名称。  
- NOACK：如果你可以接受消息偶尔丢失的情况，那么NOACK子命令不会将消息添加到PEL（Rending Entries List），相当于读取消息时就执行ACK。  
© BLOCK：阻塞读取，单位是毫秒。为0表示无限阻塞等待。

在使用XREADGROUP时，要在STREAMS选项中指定ID，有两种配置方式。

通常使用>这个配置，消费者只接收自上次读取后产生的新消息，其实就是从消费者组的last_id开始一个个读取消息，这些都是未分配给其他消费者的消息。也就是说，只获取比上次读取的消息ID更大的消息。  
0 或者其他有效 ID，仅返回所有 ID 大于指定 ID 的未 ACK 的历史消息，不包含新消息。

需要注意的是，XREADGROUP 实际是一个写命令，看起来是从 Stream 中读取数据的，但副作用是修改消费者组的 last Delivered_id，所以它只能在 master 实例上调用。

张无剑：“当消息传递给消费组的消费者时会执行哪些步骤？”

Stream 内部有一个队列 PEL 保存每个消费者读取但是还没有执行 ACK 的消息。

如果该消息从未被任何消费者读取过，那么消费者会创建一个 PEL，并把 ID 保存在 PEL 中标记为待处理。  
消费者接收消息，处理业务逻辑。  
消息处理完成后，消费者可以选择执行XACK确认或者拒绝该消息。

- 如果消费者执行 XACK 确认消息，则表示处理成功，从 PEL 中移除该 ID。  
- 如果消费者拒绝消息，则表示处理失败或者错误。消息依然保存在 PEL 中，可以由同一或其他消费者重新处理。

如果消息队列中的消息被消费者组的一个消费者消费了，这条消息就不会再被这个消费者组的其他消费者读取到。

例如，consumer2执行读取操作，读取到的是orderID $= 2$ 的消息，因为consumer1已经把orderID $= 1$ 的消息消费过了。

```txt
XREADGROUP GROUP pointsGroup consumer2 COUNT 1 BLOCK 0 STREAMS order:doubleJoy >  
1) "order:doubleJoy"  
2) 1) 1) "1685785502168-0"  
2) 1) "orderID"  
2) "2" 
```

```txt
3) "茅台飞天"  
4) "79"  
5) "amount"  
6) "598" 
```

消费者组的作用之一就是让组内的多个消费者读取消息，从而实现负载均衡。例如，一个消费者组有三个消费者C1、C2、C3和一个包含消息1、2、3、4、5、6、7的Stream，如图2-37所示。

![](images/1dc940a0db8fc4c46d01e24e0bd0d21a807517b084b7dbe898e388873237da45.jpg)  
图2-37

# XACK 确认消息

当消费者接收消息时，如果消息需要 ACK，则Stream会为每条消息创建对应的streamNACK实例，并记录到消费者组和消费者的PEL中。

消费者消费成功，需要使用XACK命令对消息进行确认才会将消息从PEL中清除。如果XREADGROUP命令携带NOACK子命令，则消息无须确认，也就意味着不会进入PEL。

一旦消费者成功处理了一条消息，就应该调用XACK，这样这条消息就不会被再次读取，同时这条消息的PEL记录也被清除，从Redis服务器释放内存。

如下命令表示，确认名称为 order:doubleJoy 的 Stream 的 pointsGroup 消费者组的消息 ID 为 1685785480628-0。

```txt
XACK order:doubleJoy pointsGroup 1685785480628-0 
```

# XPENDING 查看已读末 ACK 消息

张无剑：“在消费过程中，消费者A读取了消息，还没执行业务逻辑就崩溃了，如何实现消息至少能消费一次？”

问得好，除了使用XREADGROUP GROUP pointsGroup consumer2 COUNT 1 BLOCK 0 STREAMS order:doubleJoy > 正常读取新消息，你还可以再执行一条新命令：指定实际的ID

值或者 0 来替换 $>$ 这个参数，意思是让当前消费者读取分配给自己但是还未 ACK 的历史消息，保证 At Least Once 的语义。

张无剑：“如果消费者运行的服务器被回收，再也不启动，这个消费者对应的PEL未ACK的消息该如何处理？”

为了保证在消费者消费发生故障或者宕机重启后，未 ACK 的消息依然可以被其他消费者消费，Stream 提供了 XPENDING 命令，专用于查询消费者组中未 ACK 的消息的相关信息。例如查看 order:doubleJoy 队列中，消费者组 pointsGroup 每个消费者未 ACK 的消息信息。

```txt
XPENDING order:doubleJoy pointsGroup  
1) (integer) 2  
2) "1685785480628-0"  
3) "1685785502168-0"  
4) 1) 1) "consumer1"  
2) "1"  
2) 1) "consumer2"  
2) "1" 
```

© 1): 已读取未 ACK 消息条数。  
© 2) ~ 3): 消费者组pointsGroup中所有消费者已读取的消息的最小和最大ID。  
© 4): 当前消费者组的消费者信息, 可以看到例中两个消费者 (consumer1 和 consumer2) 已读取但是未 ACK 的消息条数。

张无剑：“上面的信息比较笼统，我想知道更多细节，怎么办？”

你的问题真多，XPENDING命令可以提供更多的参数来获取更多的信息，完整的命令如下。

```txt
XPENDING <key> <groupId> [IDLE <min-idle-time>] <start-id> <end-id> <count> [<consumer-name>] 
```

你这么聪明，有的参数一看就知道是啥意思了，我就不啰唆了，只重点介绍几个特别的。

[IDLE <min-idle-time>]：可选参数，可以对 PEL 进行筛选，只返回指定时间内处于空闲状态的消费者组的未 ACK 的消息。<min-idle-time> 是一个整数，单位为毫秒。例如，你想要获取在最近 5 秒内没有接收新消息的消费者组 mygroup 的未 ACK 的消息，可以使用 XPENDING mystream mygroup IDLE 5000 命令。时间越长，越能说明这个消费者组在“摸鱼”。  
$\odot$ <start-id>：指定的起始消息ID。命令将返回在此ID之后但在<end-id>之前的消息，可以用-表示从最早一条消息开始获取消息。  
$\odot$ <end-id>：指定的结束消息ID。命令将返回在<start-id>之后但在此ID之前的消息，配置为 $+$ 等同于指定了当前Stream中最新消息的ID作为end-id。  
$\odot$ <count>：表示要返回的消息数量。

© [<consumer-name>]: 可选参数，用于指定只返回属于特定消费者的未 ACK 的消息。

```txt
XPENDING order:doubleJoy pointsGroup IDLE 5000 - + 10  
1) 1) "1685785480628-0"  
2) "consumer1"  
3) (integer) 1113535381  
4) (integer) 1  
2) 1) "1685785502168-0"  
2) "consumer2"  
3) (integer) 1112153314  
4) (integer) 1  
127.0.0.1:6379> 
```

每个未 ACK 的消息响应体对应一个子数组，每个子数组都包含以下信息。

消息的唯一ID。  
消费者名称,获取该消息但是还未 ACK 的消费者名称,我把它称作消息的当前所有者。  
消费者的空闲时间，即从上次消息传递给该消费者到现在经过的毫秒数。  
该消息已传递给消费者的次数。

第二个未 ACK 的消息的信息与第一个类似，以此类推。如果你想查看 consumer1 消费者的信息，在末尾新增一个参数表示消费者即可。

```txt
XPENDING order:doubleJoy pointsGroup IDLE 5000 - + 10 consumer1  
1) 1) "1685785480628-0"  
2) "consumer1"  
3) (integer) 1186071318  
4) (integer) 1 
```

张无剑：“时钟回拨会导致消息ID重复吗？”

根据上文，我们已经知道消息ID由时间戳和序号两部分组成。时间戳精确到毫秒，序号是时间戳所在时间对应的消息序号。

每个Stream都维护了一个latestgenerated_id属性，记录最后一个消息ID。如果发现时间戳倒退（小于latestgenerated_id所记录的ID），则采用时间戳不变、序号递增的方式来生成新消息ID，从而保证ID单调递增。

# 2.7 Geospatial 实现原理与实战

产品经理跟我说，他有一个idea：所谓“花有重开日，人无再少年”，他想为广大少男少女开发一款App，提供一个连接彼此的机会。用户登录后，基于地理位置就能发现附近的那个Ta。

记忆中的一个夜晚，她在人群中轻盈地移动，那高挑的身影像一个飘逸的音符，她的眼神

清澈而灵动，双眸中映出来自银河系的星光。

# 2.7.1 基于位置服务

在邂逅女神之前，先了解一下什么是基于位置服务（Location Based Services，LBS）。

经纬度是由经度与纬度组成的坐标系统，又称地理坐标系统。经度的范围在 $(-180, 180]$ ，以本初子午线（英国格林尼治天文台）为0经度线，东正西负；纬度的范围在 $[-90, 90]$ ，以赤道为0纬度线，北正南负。

LBS是围绕用户当前地理位置的数据而展开的服务，为用户提供精准的“邂逅”服务。LBS的特点如下。

以“我”为中心，搜索附近的Ta。  
$\odot$ 以“我”当前的地理位置为准，计算出别人和“我”之间的距离。  
◎ 按“我”与别人距离的远近排序，筛选出离“我”最近的用户。

# MySQL 实现

以登录用户为中心、 $R$ 为半径画圆，那么圆形区域内的用户就是我们想要邂逅的“附近的人”。

张无剑：“Redis 老哥，我想到可以把经纬度存储到 MySQL 中。”

```sql
CREATE TABLE `nearby_user` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL COMMENT '名称',
    `longitude` double DEFAULT NULL COMMENT '经度',
    `latitude` double DEFAULT NULL COMMENT '纬度',
    `create_time` datetime DEFAULT NULL ON UPDATE CURRENT Timestamp COMMENT '创建时间',
PRIMARY KEY (`id`)  
) ENGINE=InnoDB DEFAULT CHARACTER=utf8mb4;
```

张无剑：“总不能把数据库中所有“女神”的经纬度数据都查询出来，一一计算她们与自己的距离，再根据距离排序吧？这个时间复杂度太高，计算量太大了。”

你可以分别把男女坐标的数据存放到不同的表中，以此减少表中的数据量。通过一个矩形区域来过滤“女神”的坐标，再计算矩形区域内数据的距离并排序，计算量会明显降低。

张无剑：“如何划分这个矩形区域呢？”

在圆形外套一个矩形，将用户经度、纬度的最大值和最小值作为查询语句的筛选条件，就很容易将矩形内的“女神”信息搜索出来，如图2-38所示。

![](images/dce2d54d9898e97b5a890cce53b0550749501e1516d2ff276b1f83fb15b59564.jpg)  
图2-38

为了满足高性能的矩形区域算法，数据表需要为经纬度坐标加上复合索引(longitude, latitude)，以最大程度优化查询性能。

张无剑：“多出来的阴影部分怎么办？这些不是目标区域。”

多出来的这部分区域内的用户到圆点的距离一定比圆的半径 $R$ 大，那么我们就计算用户中心点与正方形内所有用户数据的距离，筛选出所有距离小于或等于半径的用户，即符合要求的附近的人。

再说了，你不是想邂逅更多的“女神”吗？不需要那么精确。

使用一个第三方类库根据经纬度和距离来计算外接矩形，根据两个用户的经纬度计算两点之间的距离。

```xml
<dependency> <groupId>com.spatial4j</groupId> <artifactId>spatial4j</artifactId> <version>0.5</version> </dependency> 
```

执行步骤如下。

（1）根据用户的经纬度、搜索距离获取外接正方形。  
(2) 执行 SQL 语句, 查询出经纬度在正方形范围内的数据。  
（3）剔除超过指定距离的用户数据（不需要很精确，如果想要更多配对，那么不用执行该步骤）。

```txt
获取指定距离的人
```

\* @param distance 搜索距离范围，单位为千米\* @param userLng 当前用户的经度\* @param userLat 当前用户的纬度\*/public String nearBySearch(double distance, double userLng, double userLat) (//1.获取外接正方形Rectangle rectangle $=$ getRectangle(distance, userLng, userLat);//2.获取位置在正方形内的所有用户List<User> users $\equiv$ userMapper.selectUser(rectangleMinMax(),rectangleMinMax(), rectangleMinMax(), rectangleMinMax(), rectangleMinMax());//3.剔除半径超过指定距离的用户users $\equiv$ users.stream(）.filter(a->getDistance(a.getLongitude(),a.getLatitude(),userLng, userLat) $< =$ distance).collect(Collectors.list());return JSON.toString/users);1//获取外接矩形private Rectangle getRectangle(double distance,double userLng,double userLat){return spatialContext.getDistCalc().calcBoxByDistFromPt(spatialContext.makePoint(userLng, userLat),distance $\ast$ DistanceUtilities.KM_TO_DEG,spatialContext,null);1/\*\*\* \*球面中，两点间的距离\* @param longitude 经度1\* @param latitude 纬度1\* @param userLng 经度2\* @param userLat 纬度2\* @return 返回距离，单位为千米\*/private double getDistance(Double longitude,Double latitude,double userLng,double userLat){return spatialContext.calcDistance(spatialContext.makePoint(userLng, userLat);spatialContext.makePoint(longitude,latitude))\*DistanceUtilities.DEg_TO_KM;

getDistance 方法可以获取两点之间的距离，所以用户间距离的排序可以在业务代码中实现，SQL 语句也非常简单。

```sql
SELECT \* FROM nearby_user WHERE (longitude BETWEEN#{minlng}AND#{maxlng}) AND (latitude BETWEEN#{minlat}AND#{maxlat}) 
```

但是数据库的查询性能毕竟有限，如果“附近的人”查询请求非常多（高并发场景），那么

这可能不是一个很好的方案。

# 尝试Redis散列表末果

我们一起分析一下LBS数据的特点。

$\odot$ 每个“女神”都有一个ID编号，ID对应经纬度信息。  
◎ “靓仔”登录 App 查找附近的人时，App 根据“靓仔”的经纬度获取指定范围内的“女神”信息。  
- 获取到符合位置要求的“女神”ID列表后，再根据ID从数据库查询“女神”列表返回给用户。

数据特点就是一个“女神”（用户）对应一组经纬度，让我想到了Redis的散列表，也就是一个field（“女神”ID）对应一个value（经纬度），如图2-39所示。

![](images/a6b062c25e496806be1513a4feef7cfbcb2913cf5205a0170ee8502fd0dbed86.jpg)

![](images/e37ccec5dc4add80b624d78b6704f11452b7ade8a73a64a065c76630097abbad.jpg)  
图2-39

散列表看起来好像可以实现，但是LBS应用除了记录经纬度，还需要对散列表中的数据进行范围查询，将经纬度换算成距离并排序。而散列表中的数据是无序的，显然不可取。

# Sorted Sets 初见端倪

在 Sorted Sets 中，每个元素都由两部分组成，分别是 member 和 score。可以根据权重分数对 member 排序，这样看起来就满足需求了。例如，member 存储“女神”ID，score 是该“女神”的经纬度信息，如图 2-40 所示。

张无剑：“还有一个问题，Sorted Sets 中元素的权重值是一个浮点数，经纬度是经度和纬度两个值，如何将它们转换成一个浮点数呢？”

思路对了，为了实现对经纬度的比较，Redis采用业界广泛使用的GeoHash编码，分别对经度和纬度进行编码，再把经纬度的编码组合成一个最终编码，这样就实现了将经纬度转换成一个值。而Redis的GEO类型的底层数据结构就是用Sorted Sets实现的。

key   
value   
![](images/1bba88f6e532ec0923f64b6c21547cd749ad4c8c7a52ec5f271b04897d395334.jpg)  
Sorted Sets 类型, member 对应 ID, score 是经纬度

![](images/668f37d4135e2ebcd8a30c2d04183846c5e4e9b909cd005a85bf38c31ed21ad9.jpg)  
图2-40

# 2.7.2 GeoHash 编码和底层数据结构

# GeoHash 编码

GeoHash编码是将二维经纬度编码转换为一维，为地址位置分区的一种算法。其核心思想是区间二分：将地球编码看成一个二维平面，然后将这个平面递归均分为更小的子块。这个过程可以分为三步。

（1）将经、纬度分别变成一个 $N$ 位二进制数。  
(2) 将经、纬度的二进制数合并。  
（3）按照Base32进行编码。

# 经纬度编码

GeoHash 编码会把一个经度编码成一个 $N$ 位的二进制数，例如对经度范围 $(-180,180]$ 做 $N$ 次二分区操作，其中 $N$ 可以自定义。

在进行第一次二分区时，经度范围(-180,180]会被分成(-180,0)和[0,180]两个子区间（我称之为左、右分区）。

此时，我们可以查看一下要编码的经度值落在了左分区还是右分区。如果落在左分区，就用0表示；如果落在右分区，就用1表示。这样一来，每做完一次二分区，我们就可以得到1位编码值（不是0就是1）。

再对经度值所属的分区做一次二分区，查看经度值落在了二分区后的左分区还是右分区，然后按照刚才的规则再做1位编码。当做完 $N$ 次二分区后，经度值就可以用一个 $N$ 位的数来表示了。

所有的地图元素坐标都被放置于唯一的方格中，分区次数越多，方格越小，坐标越精确。

然后对这些方格进行整数编码，距离越近的方格编码越接近。

编码之后，每个地图元素的坐标都将变成一个整数，通过这个整数可以还原出元素的坐标，整数越长，还原出来的坐标值的损失就越小。对于“附近的人”这个功能而言，损失的一点精度可以忽略不计。

例如，对经度值169.99进行4位编码（ $N = 4$ ，做4次分区），把经度区间 $(-180,180]$ 分成了左分区 $(-180,0)$ 和右分区[0,180]。

© 169.99 属于右分区，使用 1 表示第一次分区编码。  
◎ 再将 169.99 经过第一次划分所属的 [0, 180] 区间继续分成 [0, 90) 和 [90, 180], 169.99 依然在右区间，编码为 1。  
$\odot$ 将[90,180]分为[90,135)和[135,180],这次落在左分区,编码为0。

纬度的编码思路与经度一样，不再赘述。

# 合并经纬度编码

假如计算的经、纬度编码分别是11011和00101，经纬度组合编码的第0位由经度编码第0位的值决定，经纬度组合编码的第1位由纬度编码第0位的值决定，以此类推，如图2-41所示。

![](images/5826fc20385e8c1a34daed55830aa87131c23f1290e7d285dcc55cdeaa21ca1c.jpg)  
图2-41

其实质是二分法。不断地将经度、纬度范围进行二分，输出1/0，偶数位放经度，奇数位放纬度，把两串编码组合成一串二进制格式编码（二分次数越多，输出的bit串越长），然后将这一串二进制格式编码按照5位一组进行Base32编码，得到最终结果。

这样，经纬度（35.679,114.020）就可以使用1010011011表示，将这个值作为Sorted Sets的权重就可以实现排序。每个地理位置的坐标都由geo.h的geoPoint结构体定义，所有的坐标信息都存放在geoArray数组中。

```txt
typedef struct geoPoint{ double longitude; double latitude; double dist; double score; char \*member; } geoPoint; 
```

```txt
typedef struct geoArray{ struct geoPoint \*array; size_t buckets; size_t used; } geoArray; 
```

# 添加地理位置的原理

添加地理位置到Geospatial的核心源码在geo.c文件的geoaddCommand中，主要步骤如下，为了便于理解，我省略了部分代码。

```txt
void geoadCommand(client *c) { int xx = 0, nx = 0, longidx = 2; int i; 
```

# //1.解析可选命令可选参数

```txt
while (longidx < c->argc) { char *opt = c->argv(longidx) ->ptr; if (!strcasecmp(opt, "nx")) nx = 1; else if (!strcasecmp(opt, "xx")) xx = 1; else if (!strcasecmp(opt, "ch")) { /* Handle in zaddCommand. */ } else break; longidx++; } 
```

# //省略部分代码

```c
/* 2. 创建一个参数数组，用于构建调用 ZADD 命令所需的参数和命令 */
int elements = (c->args - longidx) / 3;
int argc = longidx + elements * 2; /* ZADD key [CH] [NX|XX] score ele ... */
probj **argv = zmallocargc*sizeof(robj));
argv[0] = createRawStringObject("zadd", 4);
for (i = 1; i < longidx; i++) {
    argv[i] = c->argv[i];
    incrRefCountargv[i]);
} 
```

# // 省略部分代码

// 3. 将经纬度转换成 GeoHash 编码作为 zset 的 score 部分

```txt
GeoHashBits hash;  
geohashEncodeWGS84(xy[0], xy[1], GEO_STEP_MAX, &hash);  
GeoHashFix52Bits bits = geohashAlign52Bits(key);  
robj *score = createObject(OBJ_STRING, sdsfromlonglong(bits));  
robj *val = c->argv(longidx + i * 3 + 2);  
argv(longidx+i*2) = score;  
argv(longidx+1+i*2) = val; 
```

```javascript
incrRefCount(val);   
1   
//4.使用replaceClientCommandVector替换客户端的命令参数向量，然后调用zaddCommand;   
//来实际执行ZADD命令，将位置成员添加到有序集合中 replaceClientCommandVector(c,argc,argv); zaddCommand(c);
```

（1）解析命令可选参数。例如NX、XX、CH等。  
（2）创建一个参数数组，用于构建调用ZADD命令所需的参数。  
（3）将一组经度和纬度坐标转换为 GeoHash 编码，并将编码后的结果和相关的值作为有序集合的 score 和 member，构建成一个 Redis 命令的参数数组 argv。这样就可以通过执行相应的命令将这些坐标和值添加到 Sorted Sets 有序集合中。这部分是核心，我逐步解释。

$\odot$ GeoHashBits hash; 结构体变量，用于存储 GeoHash 编码的结果。

$\odot$ geohashEncodeWGS84(xy[0], xy[1], GEO_STEP_MAX, &hash); 函数调用，用于将 WGS84 坐标（经度和纬度）编码为 GeoHash。xy[0] 是经度，xy[1] 是纬度， GEO_STEP_MAX 可以指定编码的精度。函数执行后，编码结果将存储在 hash 变量中。

$\odot$ GeoHashFix52Bits bits = geohashAlign52Bitshash); 函数调用，用于将 GeoHash 编码对齐为 52 位。GeoHash 实际上是一个可变长度的编码，在 Redis 的世界里，通常会使用 52 位的固定长度编码。函数执行后，固定长度的编码结果将存储在 bits 变量中。

$\odot$ robj *score = createObject(OBJ_STRING, sdsfromlonglong(bits)); 这行代码创建了一个Redis对象score，其类型是String，并将GeoHash编码结果bits转换为字符串格式。GeoHash编码通常是二进制格式数据，为了在Redis中使用，需要将其转换为字符串类型的对象。

$\odot$ robj *val = c->argv(longidx + i * 3 + 2); 这行代码从输入参数 c->argv 中获取与当前元素相关的值（value）。在这行代码的上下文中，c->argv 是一个包含 Redis 命令的参数的数组。

$\odot$ argv[longidx+i\*2] = score; 和 argv[longidx+1+i\*2] = val; 这两行代码将前面创建的 score 对象和获取的值对象 val 分别放入 Redis 命令的参数数组 argv 中。在这段代码的上下文中，这个过程是为了构建一个可以传递给 Redis 命令的参数数组。

$\odot$ incrRefCount(val); 这行代码增加了值对象 val 的引用计数。在 Redis 的对象引用计数机制中，当一个对象被引用时，需要增加其引用计数。这里的上下文是为了确保在参数数组 argv 使用这个对象时，引用计数正确。

(4) 使用 replaceClientCommandVector 替换客户端的命令参数, 然后调用 zaddCommand

来实际执行ZADD命令，将位置成员添加到Serted Sets中。

# 地理位置信息查询

添加地理位置信息到Geospatial的核心源码在geo.c文件的geoaddCommand中，主要步骤如下，作为贴心哥，我省略部分代码以便你理解。

void georadiusGeneric(client \*c, int srcKeyIndex, int flags) { //省略部分源码   
//1.从命令参数中解析出相关的选项和参数，包括搜索的中心点坐标、搜索半径或区域尺寸、排序//方式和返回结果的数量等，大家不用在意这里的细节intwithdist $= 0$ ，withhash $= 0$ ，withcoords $= 0$ ：intfrommember $= 0$ ，fromloc $= 0$ ，byradius $= 0$ ，bybox $= 0$ ：intsort $=$ SORT_NONE;intany $= 0$ ：  
if(c->argc>base_args){intremaining $= c - >$ argc-base_args;for(inti $= 0$ i<remaining;i++){char \*arg=c->argv[base_args+i]-ptr;if(!strcasecmp(arg,"withdist")){withdist $= 1$ ·}else if(!strcasecmp(arg,"withhash")){withhash $= 1$ ·}else if(!strcasecmp(arg,"withcoord")){withcoords $= 1$ ·}else if(!strcasecmp(arg,"any")){any $= 1$ ·}else if(!strcasecmp(arg,"asc")){sort $=$ SORT ASC;}else if(!strcasecmp(arg,"desc")){sort $=$ SORT_DESC;}else if(!strcasecmp(arg,"count") && (i+1)< remaining){if(getLongLongFromObjectOrReply(c,c->argv[base_args+i+1],&count，NULL) != C_OK) return;if(count <= 0）{addReplyError(c,"COUNT must be >0");return;1 $\mathrm{i + + }$ ：}else if(!strcasecmp(arg,"store") &&(i+1)< remaining&&！(flags&RADIUS_NOSTORE) &&  
1//省略部分代码  
1//省略部分代码  
//2.根据给定的搜索条件计算出所有邻近的geohash区域，可能包含符合搜索条件的地理位置GeoHashRadius georadius = geohashCalculateAreasByShapeWGS84(&shape);

//3.遍历指定的有序集合（zset），根据计算得到的 geohash 区域信息找出所有符合条件的地 // 理位置，并将它们存储在一个 geoArray 结构中 geoArray \*ga $=$ geoArrayCreate(); membersOfAllNeighbors(zobj, &georadius, &shape, ga, any ? count : 0); //4.如果没有指定存储目标键（storekey)，则将搜索结果返回客户端 //否则，将搜索结果存储在一个新的 Sorted Sets 中，存储键为 storekey，并返回结果数量 //省略部分代码 //4.1如果没有指定存储目标键（storekey），则将搜索结果返回客户端 if $\text{一}$ (storekey $= =$ NULL）{//省略部分代码 addReplyArrayLen(c, returned_items); //省略部分代码 } else { //省略部分代码 //4.2否则，将搜索结果存储在一个新的有序集合中，存储键为storekey，并返回结果数量 for $(i = 0;i <$ returned_items; i++) { //省略部分代码 znode $=$ zslInsert( $\mathrm{zs - > zsl}$ ,score,gp->member); serverAssert(dictAdd( $\mathrm{zs - > }$ dict,gp->member,&znode->score) == gp->member $=$ NULL; } if (returned_items){ //判断是否需要使用 listpack 数据结构来存储 zsetConvertToListIfNeeded(zobj,maxelelen,totelelen); setKey(c,c->db,storekey,zobj,0); decrRefCount(zobj); } else if (dbDelete(c->db,storekey)){ //省略部分代码 } addReplyLongLong(c, returned_items); } geoArrayFree(ga);

# 2.7.3 出招实战：附近的人

Redis Geospatial 数据类型采用了 GeoHash 编码算法，把 GeoHash 编码合并的经纬度值作为 Sorted Sets 元素的 score 权重。你可以使用 Geospatial 提供的两个命令来实现“附近的人”这个功能。

GEOADD：把地理位置信息（longitude, latitude, name）添加到集合中，注意，经度位于纬度之前。  
© GEOSEARCH：搜索位于给定形状指定的区域内的地理位置数据，除了支持在圆形区域内搜索，还支持在矩形区域内搜索。该命令从 6.2.0 版本开始提供，用于代替已弃用的 GEORADIUS 和 GEORADIUSBYMEMBER 命令。

# GEOADD

你可以使用 GEOADD 命令将登录 App 的“女神”的地理位置添加到集合中，例如一次添加多个用户（“李淑芬”“貂蝉”“Chaya”）的地理位置信息到集合中。

```txt
GEOADD girl:localtion 13.361389 38.115556 "李淑芬" 15.087269 37.502669 "貂蝉" 15.087269 37.502669 "Chaya" 
```

下面解释语法，帮你掌握真本事。

```txt
GEOADD key [NX | XX] [CH] longitude latitude member [longitude latitude member ...] 
```

key: 该例中“girl:localtion”就是 key。  
NX：不更新已存在的元素，只添加新元素。  
© XX：只更新已存在的元素，不添加新元素。  
© CH：返回值为修改的元素总数，包括添加的新元素和更新坐标的已存在元素。  
$\odot$ longitude：经度。  
$\odot$ latitude：纬度。  
© member: 元素内容，与地理位置关联的数据，可以是任何字符串。

# 删除地理位置

张元剑：“如何删除下线用户的经纬度信息呢？”

这个问题问得好，Geospatial 是基于 Sorted Sets 实现的，可以借用 ZREM 命令删除地理位置信息。例如删除“李淑芬”的地理位置信息。

ZREM girl:location "李淑芬"

# GEOSEARCH

张无剑：“现在，我登录了App，如何根据我的经纬度信息查找附近指定范围内的‘女神’呢？”

别着急，Geospatial 提供了 GEOSEARCH 用于搜索指定地理位置的数据。假设你的经纬度是（15.08726937.502669），需要获取附近 10 千米的“女神”数据，并由近到远排序。

GEOSEARCH girl:localtion FROMLONLAT 15.087269 37.502669 BYRADIUS 10 KM ASC

```txt
WITHCOORD WITHDIST  
1) 1) "李淑芬"  
2) "0.0002"  
3) 1) "15.08726745843887329"  
2) "37.5026842333162032" 
```

该命令和响应信息有些复杂，我来分别解释一下，不要慌。

```txt
GEOSEARCH key  
<FROMMEMBER member | FROMLONLAT longitude latitude>  
<BYRADIUS radius <M | KM | FT | MI> | BYBOX width height <M | KM | FT | MI>>  
[ASC | DESC]  
[COUNT count [ANY]]  
[WITHCOORD] [WITHDIST] [WITHHASH] 
```

Geospatial 数据类型中的查询 GEOSEARCH 命令有很多参数，可以满足不同的需求。

例如，支持以 member 或者经纬度为中心点进行范围搜索，这是一个必填参数。

© FROMMEMBER member: 将 member 作为中心点，例如当前登录用户“码哥”。  
© FROMOLONLAT longitude latitude：将给定的经纬度作为中心点进行搜索。

除此之外，GEOSEARCH命令还支持将不同的形状作为搜索区域，这是一个必填参数。

© BYRADIUS：允许用户在给定的半径（radius）内搜索，单位可以是M|KM|FT|MI。  
$\odot$ BYBOX：允许用户在一个由高度（height）和宽度（width）确定的轴对齐的矩形内搜索，单位可以是M|KM|FT|MI。

搜索的数据还可以排序返回，这是一个可选参数。

$\odot$ ASC：以当前用户经纬度为中心点，将数据由近到远排序。  
$\odot$ DESC：以当前用户经纬度为中心点，将数据由远到近排序。

COUNT 选项表示指定返回的数据数量，防止附近“女神”太多，从而节省带宽资源。如果需要更多“女神”列表，那么可以不加限制。

除此之外，你还可以控制命令返回的格式，这是一个可选参数。如果没有配置该参数，则命令只返回一个数组，数组的元素是 member，例如返回 ["李淑芬","貂蝉","Chaya"]。

- WITHDIST: 返回匹配数据项与指定中心点的距离，距离的单位与 BYRADIVS 或 BYBOX 的距离单位相同。  
© WITHCOORD: 返回匹配数据的经度和纬度。  
© WITHHASH: 返回匹配数据的原始 GeoHash 编码，以 52 位无符号整数的形式表示，用户一般对这个没兴趣。

在Redis源码中，定义了一个结构体GeoShape，用于表示地理空间搜索的形状和相关参数。

```txt
typedef struct { int type; double xy[2]; double conversion; double bounds[4]; union { double radius; struct { double height; double width; } r; } t;   
} GeoShape; 
```

int type; 搜索类型，可以是圆形搜索或矩形搜索。通常使用预定义的常量或枚举进行标识，例如 CIRCULAR_TYPE 和 RECTANGLE_TYPE。  
double xy[2]; 搜索中心点的经纬度坐标。xy[0]存储经度（longitude），xy[1]存储纬度（latitude）。  
- double conversion; 搜索半径或矩形的高度和宽度的单位转换因子。通常以千米为单位，因此 conversion 可能为 1000，将距离的单位转换为米（即 $1 \, \text{km} = 1000 \, \text{m}$ ）。  
$\odot$ double bounds[4]：搜索区域的边界框（bounding box）。bounds[0] 和 bounds[1] 分别表示最小经度和最小纬度，bounds[2] 和 bounds[3] 分别表示最大经度和最大纬度。这个边界框用于将搜索结果限制在指定范围内。  
union{...}t;联合体（union），用于根据搜索类型存储不同的参数。

- 如果 type 是圆形搜索（CIRCULAR_TYPE），则使用 radius 存储搜索半径。这个圆形搜索将在以 xy 为中心，以 radius 为半径的圆内进行搜索。  
- 如果 type 是矩形搜索（RECTANGLE_TYPE），则使用 r 结构体存储搜索矩形的高度和宽度。这个矩形搜索将在以 xy 为中心，高度为 r.height、宽度为 r.width 的矩形内进行搜索。

GeoShape 结构体可以灵活地表示不同形状和尺寸的地理空间搜索，并根据实际需求使用圆形或矩形搜索来查找地理位置的数据。Geospatial 本身并没有设计新的底层数据结构，而是直接使用了 Sorted Sets。

使用 GeoHash 编码把经纬度转换成 Sorted Sets 中的元素权重，这其中的两个关键机制就是对二维地图进行区间划分和对区间进行编码。

一组经纬度落在某个区间后，就用区间的编码值来表示，并把编码值作为 Sorted Sets 元素的权重分数。在一个地图应用中，车的数据、餐馆的数据、人的数据可能会有上千万条，如果使用 Redis 的 Geospatial 来保存，那么将出现 BigKey。

在Redis的集群环境中，集合可能从一个节点迁移到另一个节点，如果单个key的value

过大，则会对集群的迁移工作造成较大的影响。在集群环境中，单个 key 对应的数据大小不宜超过 5MB，否则会导致集群迁移卡顿，影响线上服务正常运行。所以，建议单独部署一个 Redis 集群用于保存 Geospatial 的数据对外提供服务。

如果数据有过亿条甚至更多，就需要对Geospatial数据进行拆分，可以按国家、省、市拆分，甚至可以按区拆分。这样就可以显著减小单个Sorted Sets的大小。

# 2.8 Bitmap 实现原理与实战

在移动应用的业务场景中，我们需要保存这样的信息：一个 key 关联了一个数据集合。

常见的场景如下。

用户在线状态统计：可以使用Bitmap来记录用户的在线状态，其中每位表示一个用户的在线状态（在线为1，离线为0）。这样可以高效地统计在线用户数量和在线用户的分布情况。  
$\odot$ 用户签到记录：Bitmap 可以用于记录用户的签到情况，其中每位表示一个日期（已签到为 1，未签到为 0）。这样可以轻松统计用户的连续签到天数、活跃用户数等信息。  
- 页面点击量统计：Bitmap 可以用于统计网站的页面点击量，其中每位表示一个页面的点击情况（点击为 1，未点击为 0）。这样可以快速获取每个页面的点击量及总点击量。

在通常情况下，我们面临的用户数量及访问量都是巨大的，例如百万、千万级别的用户数量，或者千万级别、甚至亿级别的签到信息统计。所以，我们必须选择能够高效统计大量数据的集合类型。

# 2.8.1 Bitmap

Bitmap（位图）是Redis提供的一种特殊的数据结构，用于处理bit级别的数据。

实际上面向 bit 的操作是在字符串类型上定义的，将 Bitmap 存储在字符串中，每个字符都是由 8 bit 组成的数组，其中的每位只能是 0 或 1。字符串类型的最大容量是 512MB，所以一个 Bitmap 最多可配置 2^32 个不同位。

Bitmap 解决的是二值状态统计场景问题。也就是集合中的元素的值只有 0 和 1 两种，在签到打卡和用户是否登录的场景中，只需记录签到（1）或未签到（0）、已登录（1）或未登录（0）。

假如我们使用Redis的字符串类型判断用户是否登录（key->UserID，value->0表示下线，value->1表示登录），如果以字符串的形式存储100万个用户的登录状态，那么需要存储100万个字符串，内存开销太大。

张元剑：“为何字符串类型内存开销大呢？”

字符串类型底层使用SDS结构存储数据，除了记录实际数据，还需要额外的len和alloc等信息，如图2-42所示。

![](images/62b072abcbaee1e7270e90c2e625de7b41c3a5c386e7c009cd9e24d5f39b8244.jpg)  
图2-42

len: 占 4 字节, 表示 buf 的已用长度。  
alloc：占4字节，表示buf实际分配的长度，通常大于len。  
$\odot$ char buf[]：字节数组，保存实际的数据，Redis 自动在数组最后加上一个“\0”，额外占用 1 字节的开销。

所以，SDS中除了char buf[]保存实际的数据，还有len与alloc的额外开销。另外，Redis的数据类型有很多，对于不同的数据类型要记录一些元数据（例如最后一次访问的时间、被引用的次数等）。Redis使用RedisObject结构体来统一记录这些元数据，ptr指针指向实际数据，如图2-43所示。

![](images/523208341aca90bc4c262193db731c60c756434cee6691b4ab687c25a6aed12f.jpg)  
图2-43

Bitmap 可以用来实现二值状态场景。例如，用 1 bit 表示登录状态，一亿个用户也只占用一亿 bit 内存，约为 12 MB（100000000 / 8/1024/1024）。

估算占用空间的公式是：(offset/8/1024/1024) MB

# 2.8.2 SDS数据结构构成的位数组

Bitmap 的底层使用字符串类型的 SDS 数据结构来保存位数组,Redis 把每字节数组的 8

bit利用起来，每位表示一个元素的二值状态（不是0就是1）。

可以将Bitmap看作一个以bit为单位的数组，数组的每位只能存储0或者1，数组的每位下标在Bitmap中叫作offset偏移量。

为了直观展示，我们可以将buf数组的每个槽位中的字节用一行表示，每行有8bit，8个格子分别表示字节中的8位，如图2-44所示。

![](images/52dfc572c0c2abfd0983799b03f2727da434627b3b9edcabbb2b22bb6b8f678b.jpg)  
图2-44

8 bit 组成 1 byte，所以 Bitmap 会极大地节省存储空间。这就是 Bitmap 的优势。

# 配置Bitmap offset value原理

Bitmap 提供了 SETBIT 命令用于设置或者清空 Bitmap 集合的 offset 位置的 bit 的值（只能是 0 或者 1）。

命令的实现在源码 bitops.c 文件的 setbitCommand 方法中，我省略了一些代码，以便大家理解。

```javascript
/\*SETBITkeyoffsetbitvalue\*/   
voidsetbitCommand(client*c){ //省略部分代码 //1.命令参数中解析出偏移量bitoffset，表示要配置的bit在Bitmap中的位置 if(getBitOffsetFromArgument(c,c->argv[2],&bitoffset,0,0)！=C_OK) return; //2.从命令参数中解析出bit的值on，这个值只能是0或1，表示要配置的位值 if(getLongFromObjectOrReply(c,c->argv[3],&on,err)！=C_OK) return; /\*3.如果on不是0或1，即不是有效的位值，将返回错误回复并结束\*/ if(on&~1){ addReplyError(c,err);
```

return;   
1 //4.查找并返回键c->argv[1]对应的字符串对象o，字符串对象在Redis中用于存储   
Bitmap int dirty; if((o $=$ lookupStringForBitCommand(c,bitoffset,&dirty)) $\equiv$ NULL) return; /\*5.计算偏移量bitoffset对应的字节索引byte和位索引bit.由于Redis中的Bitmap 是按字节存储的，所以需要计算偏移量对应的byte位置和bit位置\*/ byte $=$ bitoffset>>3; byteval $= ((\mathrm{uint8\_t^{\star}})\mathrm{o - > ptr})$ [byte]; bit $= 7-$ (bitoffset&0x7); //6.获取字节中指定位的当前值bitval bitval $=$ byteval&（1<<bit); /\*7.比较当前位置bitval与要配置的位值on是否相同。如果位值有变化，或者该位是新创建 的，或者Bitmap长度发生变化，那么进行位值更新\*/ if(dirty||(!bitval！ $= \text{on}$ ）{ /\*Updatebytewithnewbitvalue.\*/ byteval $\& = \sim (1 <   <   \text{bit})$ ： byteval $\mid =$ ((on&0x1）<<bit); ((uint8_t\*)o->ptr)[byte] $=$ byteval; signalModifiedKey(c,c->db,c->argv[1]); //省略部分代码 server.dirty++; 1 /\*8.返回配置前的位值bitval作为回复\*/ addReply(c，bitval？shared.cone：shared.czero);

# 获取 offset value 原理

获取 key 关联的 Bitmap 在 offset 处的 bit 的值，当 key 不存在时，返回 0。源码 bitops.c 的 getbitCommand 方法实现了 GETBIT 命令。

/*GETBITkeyoffset*/  
voidgetbitCommand(client*c）{//省略部分代码//1.从命令参数中解析出偏移量bitoffset，表示要获取的bit在Bitmap中的位置if(getBitOffsetFromArgument(c,c->argv[2]&bitoffset,0,0)！=C_OK)return;//2.查找并返回键c->argv[1]对应的字符串对象o。如果键不存在，或者类型不是字符串对//象，则返回零值的回复if（o $=$ lookupKeyReadOrReply(c,c->argv[1],shared.czero)) $\equiv =$ NULL||checkType(c,o,OBJ_STRING))return;  
//3.计算偏移量bitoffset对应的字节索引byte和位索引bit.由于Redis中的Bitmap//是按字节存储的，所以需要计算偏移量对应的byte位置和bit位置

byte $=$ bitoffset>>3;   
bit $= 7$ - (bitoffset&0x7);   
//4.根据字符串对象类型，从字节中获取指定位的值bitval if(sdsEncodedObject(o)){ if(byte $<$ sdslen(o->ptr)) bitval $=$ {（uint8_t\*）o->ptr)[byte] & (1<<bit); }else{ if(byte $<$ (size_t)ll2string(llbuf,sizeof(llbuf)，(long)o->ptr)) bitval $=$ llbufBYTE] & (1<<bit);   
//5.将获取到的位值bitval作为回复发送给客户端 addReply(c,butval？shared.cone：shared.czero);

# 2.8.3 出招实战：亿级用户登录判断、签到统计系统

# 用户登录判断

Bitmap 提供了 GETBIT、SETBIT 操作，通过一个偏移值 offset 对 bit 数组的偏移值 offset 的 bit 进行读/写操作，需要注意的是，offset 从 0 开始。

可以使用key = login_status关联一个Bitmap集合，表示存储用户登录状态数据，将用户ID作为offset，如果用户在线就配置为1，否则配置为0。通过GETBIT判断对应的用户是否在线。5亿个用户只需要60MB的空间。

SETBIT命令

```txt
SETBIT <key><offset><value> 
```

配置或者清空指定的 key 关联的 Bitmap 在 offset 处的 bit 的值（只能是 0 或者 1）。

GETBIT 命令

```txt
GETBIT <key> <offset> 
```

获取 key 关联的 Bitmap 在 offset 处的 bit 的值, 当 key 不存在时, 返回 0。举个例子,假如你要判断 $\mathrm{{ID}} = {10086}$ 的用户的登录情况。

第一步，用户登录时，执行以下命令，表示用户已登录。

```txt
SETBIT login_status 10086 1 
```

第二步，检查该用户是否登录，返回1表示已登录。

```sql
GETBIT login_status 10086 
```

第三步，登出，将 offset 对应的值配置为 0。

```txt
SETBIT login status 10086 0 
```

# 用户每月签到情况

在签到统计中，每个用户每天的签到用1bit表示，一个月最多只有31天，占用31bit。

考虑到每月要重置连续签到次数，为每个登录用户的每个月创建一个Bitmap集合，到期后删除以节省内存。key = uid:sign:{userld}:{yyyyMM}。将月份的日期值-1作为offset（因为Bitmap offset从0开始，所以offset=日期-1），如果签到就把这个offset的bit配置为1。

第一步，执行下面的命令表示记录用户在2023年7月1日和2023年7月29日打卡签到。

```txt
SETBITuid:sign:89757:20230701 SETBITuid:sign:89757:202307281 
```

执行以上两个命令后，Bitmap 的数据就是 10000000000000000000000000000001000（一共 32 bit）。需要注意的是，虽然你在 offset = 28 的位置配置 bit = 1，但实际上 Bitmap 占用了 32 bit，Bitmap 占用的 bit 数是 byte 的整数倍。

第二步，判断编号89757的用户在2023年7月29日是否打卡签到。

```txt
GETBIT uid:sign:89757:202307 28 
```

第三步，统计该用户在7月的签到次数，使用BITCOUNT命令。该命令用于统计在给定的bit数组中，值等于1的bit的数量。

```txt
BITCOUNT uid:sign:89757:202307 
```

这样我们就可以统计用户每个月的打卡情况了，是不是很赞？

张无剑：“如何统计每个月首次签到日期呢？”

Bitmap 提供了 BITPOS key bit [start [end [BYTE | BIT]]] 命令，返回数据表示 Bitmap 中第一个值为 1 或者 0 的位置。

需要注意的是，该命令会遍历整个Bitmap，你可以通过可选的start参数和end参数指定要检测的范围。我们可以通过执行以下命令来获取UserID $= 89757$ 在2023年7月首次打卡的日期。

```txt
>BITPOS uid:sign:89757:202307 1 (integer) 0 
```

需要注意的是，我们需要将返回的 value + 1 作为首次签到日期，因为 offset 从 0 开始，所以首次签到的日期是 2023 年 7 月 1 日。

使用Bitmap之后会节省很多内存，我来给你做一个简单计算。

$\odot$ 一个用户连续签到一个月产生31bit数据，大约4byte（每个月都按31天算，别说

我欺负关系数据库）。

$\odot$ 一个用户连续签到一年产生48byte数据。  
1000万个用户连续签到一年产生 $4.8 \times 10^{8}$ byte（ $4.8 \times 10^{8} \div 1024 \div 1024 \approx 457.76\mathrm{MB}$ ）数据。

使用关系数据库，1000万个用户连续签到一年大约会产生68.66TB数据，而使用Bitmap只会产生457.76MB数据。

谢霸戈：“同城约会 App 上线后，运营人员提出每月连续签到时间越长，发放奖励越多，结果场面异常火爆，老板赢‘麻’了。但是问题来了，如何统计一个用户的每月签到详情呢（每月签到情况、连续签到天数）？”

Bitmap 提供了 BITFIELD 命令，这个命令可以通过一次调用对多 bit 进行操作。其语法比较复杂，我解释一下。

```txt
BITFIELD key  
[GET type offset | [OVERFLOW <WRAP | SAT | FAIL>]  
<SET type offset value | INCRBY type offset increment>  
[GET type offset | [OVERFLOW <WRAP | SAT | FAIL>]  
<SET type offset value | INCRBY type offset increment>  
...] 
```

其中，key是要操作的Redis字符串的键，存储Bitmap数据。

在BITFIELD命令中，你可以通过连续的子命令链对同一个字符串进行多个位操作。每个子命令由一个或多个参数组成，指定要执行的操作、位字段的类型、偏移量、值等。

以下是每个子命令的详细说明。

- GET type offset: 从 Bitmap 中获取位字段（bit field）的值。type 表示位字段的数据类型，offset 表示位偏移量（从 0 开始）。你可以指定多个 GET 操作。  
- SET type offset value: 配置位字段的值。type 表示位字段的数据类型，offset 表示位偏移量，value 表示要配置的值。你可以指定多个 SET 操作。  
© INCRBY type offset increment: 将指定位字段的值递增指定的增量。type 表示位字段的数据类型, offset 表示位偏移量, increment 表示递增的数量。你可以指定多个 INCRBY 操作。  
OVERFLOW <WRAP | SAT | FAIL>: 当位操作导致溢出时的处理方式。你可以选择 WRAP（环绕）、SAT（饱和）或 FAIL（失败）。这个参数对应整个子命令链中的溢出处理方式。

使用的数据类型（type）如下。

$\odot u < N >$ ：无符号整数类型，其中 $< N>$ 是位数。例如，u8表示8位无符号整数。

© i<N>: 有符号整数类型, 其中 <N> 是位数。例如, i16 表示 16 位有符号整数。  
◎ N：使用默认类型，可以是 u 或 i。

如下命令表示获取从2023年7月offset位置为0开始，连续31天的签到情况，返回值是无符号十进制的。

```javascript
>BITFIELDuid:sign:89757:202307GETu310 1073741828 
```

需要注意的是，一个月最多有31天，因此保存签到数据的Bitmap最大只需要31bit。实际上在29号签到时，Bitmap会占用32bit，因为其底层是SDS数据结构，1byte由8bit组成，其他位置会自动补0。

谢霸戈：“命令返回的是一个十进制数，我哪知道bit到底是0还是1呀？”

你只需要把这个十进制数字和1做与运算就可以了。

# 2.9 HyperLogLog 实现原理与实战

在移动互联网的业务场景中，数据量很大，系统需要保存这样的信息：一个 key 关联了一个数据集合，同时将这个数据集合以统计报表的形式呈现给运营人员。例如：

统计一个 App 的日活、月活人数。  
统计一个页面每天被多少个不同账户（Unique Visitor，UV）访问。  
统计用户每天搜索不同词条的个数。  
统计注册IP地址数。

谢霸戈：“通常情况下，系统面临的用户数量及访问量都是巨大的，例如百万、千万级别的用户数量，或者千万级别、甚至亿级别的访问信息，怎么处理这种情况呢？”

这些就是典型的 HyperLogLog（基数统计）应用场景。基数统计指统计一个集合中不重复元素的数量，这些不重复的元素被称为基数。

# 2.9.1 基数统计

HyperLogLog 是一种概率数据结构，用于估计集合的基数。每个 HyperLogLog 最多消耗 12KB 内存，在标准误差 $0.81\%$ 的前提下，可以计算 2^64 个元素的基数。其主要特点如下。

高效存储：HyperLogLog 的内存消耗是固定的，与集合中的元素数量无关。这使得它特别适用于处理大规模数据集，因为它不需要存储所有不同的元素，只需要存储估计基数所需的信息。

$\odot$ 概率估计：HyperLogLog 提供的结果是概率性的，不是精确的基数计数。它通过哈希函数将输入元素映射到 Bitmap 中的某些位置，并基于 Bitmap 的统计信息来估计基数。由于这是一种概率方法，因此可能存在一定的误差，但在实际应用中，这个误差通常是可接受的。  
高速计算：HyperLogLog 可以在常量时间内计算估计的基数，无论集合的大小如何。这意味着它的性能非常好，不会受到集合大小的影响。

# 2.9.2 稀疏矩阵和稠密矩阵

# 基本原理

HyperLogLog 是一种概率数据结构，它使用概率算法来统计集合的近似基数，而概率算法的本质是伯努利过程。

伯努利过程可以看作一个抛硬币实验。在抛硬币时，正面朝上和反面朝上的概率都是 $1 / 2$ 。伯努利过程就是一直抛硬币，直到正面朝上，并记录下抛掷次数 $k$ 。

例如，在第一次抛掷时硬币正面朝上，那么 $k$ 为1；如果第一次和第二次抛掷都是反面朝上，直到第三次才出现正面，那么 $k$ 为3。

对于 $n$ 次伯努利过程，我们会得到 $n$ 个出现正面的投掷次数值 $k_{1}, k_{2}, \ldots, k_{n}$ ，其中最大值记为 $k_{\max}$ ，而 $2^{\wedge} k_{\max}$ 就是 $n$ 的估计值。也就是说，你可以根据最大投掷次数近似推算出进行了多少次伯努利过程。

所以 HyperLogLog 的基本思想是利用集合中数字的比特串的第一个 1 出现位置的最大值来预估整体基数，但是这种预估方法存在较大误差，为了改善这种情况，HyperLogLog 引入了分桶平均的概念，计算 $m$ 个桶的调和平均值。

Redis 内部使用字符串 Bitmap 来存储 HyperLogLog 所有桶的计数值, 一共包括 2^14 个桶, 也就是 16384 个桶。每个桶都是一个 6 bit 的数组。

这段代码描述了Redis HyperLogLog数据结构的头部定义（hyperLogLog.c中的hlldr结构体）。

```c
struct hllhdr {
    char magic[4];
    uint8_t encoding;
    uint8_t notused[3];
    uint8_t card[8];
    uint8_t registers++;
}; 
```

© magic[4]: 4 字节的字符数组，用来表示数据结构的标识符。在 HyperLogLog 中，它

的值始终为HYLL，用来标识这是一个HyperLogLog数据结构。

◎ encoding: 1 字节的字段，用来表示 HyperLogLog 的编码方式。可以取下面两个值之一。

- HLL_DENSE：使用稠密表示方式。  
- HLL_SPARSE：使用稀疏表示方式。

© notused[3]: 3 字节的字段，用于未来扩展，要求这些字节的值必须为 0。  
© card[8]: 8 字节的字段, 用来存储缓存的基数 (基数估计的值)。  
© registers[]：长度可变的字节数组，用来存储 HyperLogLog 的数据，一共有 16384 个桶，每个桶占据 6 bit，我们都知道 1 字节由 8 bit 组成，这种 6 bit 排列的结构会导致一些桶跨越字节边界，我们需要将 1 或 2 字节进行适当的移位拼接才可以得到实际的计数值。

HyperLogLog 的结构如图 2-45 所示。

![](images/943a40ffaf41efec3b9e149ad2b49e184ae94856a5438d135bb1ce798a55641a.jpg)  
图2-45

Redis 对 HyperLogLog 的存储进行了优化，在计数较小时，大多数桶的计数值是 0，采用稀疏矩阵存储，占用空间很小。

只有在计数很大、稀疏矩阵占用的空间超过了阈值时才会转变成稠密矩阵，占用12KB空间。

# 2.9.3 出招实战：海量网页访问量统计

HyperLogLog 的主要使用场景是基数统计，例如统计微信公众号的文章每天被多少用户访问过，一个用户一天访问多次只能记一次。对于这种场景，为了节约成本，其实只需要计算一个大概值，没必要算出精确值。

对于上面的场景，可以使用 Sets、Bitmap 和 HyperLogLog 来解决。

Redis高手心法

Sets：统计精度高，对于少量的数据统计建议使用，大量的数据统计会占用很大的内存空间。

Bitmap：位图算法，统计精度高，内存占用比 Sets 少，但是在统计大量数据时还是会占用较大内存。

HyperLogLog：存在一定误差，占用内存少（稳定占用12KB左右）。

# 使用 Sets 实现

一个用户一天内多次访问同一网站只能记一次，所以很容易就想到通过 Sets 来实现。

例如，微信昵称叫 Chaya 的“小姐姐”访问《爱一个人总要掉眼泪》这篇文章时，我把“Chaya”存到 Sets 中。

```txt
SADD 爱一个人总要掉眼泪:uv码哥 Chaya 赵小因 Chaya (integer) 3
```

Chaya 多次访问这篇文章, Sets 的去重特性保证集合中只有一个记录。接着, 通过 SCARD 命令, 统计页面 UV。命令返回这个集合的元素个数 (微信昵称个数)。

```txt
SCARD 爱一个人总要掉眼泪：uv (integer) 3
```

# 使用 HyperLogLog 实现

Chaya: “Sets 虽好, 但如果文章的阅读量达到千万级别, 一个集合就保存了千万个用户的 ID, 消耗的内存也太大了。”

不要怕，“只要思想不滑坡，办法总比困难多”，这些是典型的 HyperLogLog 应用场景。

HyperLogLog 的优点在于它所需的内存并不会因为集合的大小而改变，无论集合包含多少个元素，HyperLogLog 进行计算所需的内存总是固定的，并且是非常少的。

HyperLogLog 使用起来太简单了。PFADD、PFCOUNT、PFMERGE 三个命令打天下。

# PFADD

用户每访问一次页面，就调用PFADD命令将用户ID添加到HyperLogLog中。一共有三个用户访问了这页面，其中Chaya访问了两次，但只记一次。

```txt
PFADD 爱一个人总要掉眼泪：uv 码哥 Chaya 赵小因 Chaya
```

如果执行命令后 HyperLogLog 估计的近似基数发生变化，则 PFADD 返回 1，否则返回 0。如果指定的 key 不存在，那么该命令会自动创建一个空的 HyperLogLog 结构。

PFADD 命令并不会一次性分配 12KB 内存，而是随着基数的增加逐渐增加分配的内存。

# PFCOUNT

接下来，通过PFCOUNT命令获取文章《爱一个人总要掉眼泪》的UV值，可以看到返回值是3，符合预期。

> PFCOUNT 爱一个人总要掉眼泪:uv  
3

# PFMERGE 合并统计

Chaya: “运营人员又提了一个需求：对文章进行标签分类，要把情感类文章的几个页面的数据合并统计。”

页面的 UV 访问量也需要合并，这时 PFMERGE 就派上用场了：同样的用户访问这两个页面只记一次。

如下命令把“爱一个人总要掉眼泪:uv”和“爱情是幸福和不委屈:uv”两个HyperLogLog集合数据合并到“情感分类文章:uv”这个集合中。

PFADD 爱情是幸福和不委屈:uv Chaya 赵小因幸运草

合并两个页面 UV

PFMERGE情感分类文章：uv爱一个人总要掉眼泪：uv爱情是幸福和不委屈：uv

接着，执行“PFCOUNT 情感分类文章:uv”统计合并后的数据。

> PFCOUNT 情感分类文章：uv

4

将多个 HyperLogLog 合并（merge）为一个 HyperLogLog，合并后的 HyperLogLog 的基数接近于所有输入 HyperLogLog 的可见集合（observed set）的并集。

# 2.10 BloomFilter实现原理与实战

MySQL：“Redis老哥，我遇到难题了。程序员要开发一个浏览新闻资讯或视频的‘明日头条’App，需要实现每次推荐给同一用户的内容不重复，并过滤看过的内容。这个App系统并发量特别大，我快扛不住了，怎么办？”

如果把所有历史记录都存储在 MySQL 中，那么去重时就需要频繁地对数据库进行 exists 查询，当系统并发量很大时，数据库很难扛住压力。

MySQL：“可以使用Redis缓存吗，把浏览数据存储在Redis中。”

万万不可，这么多的历史记录要浪费多大的内存空间？！你可以使用BloomFilter解决这个问题，又快又省内存，互联网开发必备！

当数据量大，又需要去重时可以考虑使用BloomFilter，它适用于如下场景。

$\odot$ 解决Redis缓存穿透问题。  
实现邮件黑名单过滤。  
$\odot$ 过滤爬虫爬过的网站。  
◎ 不重复推荐新闻。

# 2.10.1 Bloom Filter

Bloom Filter 是由 Burton Howard Bloom 于 1970 年提出的，它是一种 space efficient 的概率型数据结构，用于判断一个元素是否在集合中。通常用于快速判断某个元素是否可能存在一个大型数据集中，而无须实际存储整个数据集。

如果BloomFilter给出的响应是某个数据不存在，那么这个数据一定不存在；当给出的响应是某个数据存在时，要注意这个数据可能不存在。

散列表也能用于判断元素是否在集合中，但是Bloom Filter只需要散列表的1/8或1/4的空间复杂度就能解决同样的问题。Bloom Filter可以插入元素，但不可以删除已有元素。

# 2.10.2 位数组和哈希函数

Redis的BloomFilter的实现基于一个位数组（bitarray）和一组不同的哈希函数，其实现过程如图2-46所示。

（1）分配一块内存空间给位数组，这个位数组的长度是固定的，通常由用户指定，决定了Bloom Filter 的容量。每个位的初始值都为0。  
（2）添加元素时，采用 $k$ 个相互独立的哈希函数对数组X做哈希计算得到 $k$ 个哈希值，这些哈希函数应该是独立的、均匀分布的，以减小冲突的可能性。分别把 $k$ 个哈希值与位数组长度取模映射的数组位置设置为1。  
（3）检测数组X是否存在，仍然用这 $k$ 个哈希函数分别对数组X做计算得到 $k$ 个哈希值，分别对应数组X的 $k$ 个位置，判断这个位置的值，如果全部为1，则表示X可能存在，否则表示X不存在。

哈希函数会出现碰撞，所以Bloom Filter会存在误判。误判率指Bloom Filter判断某个key存在，但它实际不存在的情况。

误判率主要受位数组的大小和哈希函数的数量影响。较大的位数组和更多的哈希函数可以降低误判率，但也会增加存储开销和计算开销。

![](images/8359bddb193f4c85f53c8d11bf2b1c9a5617b526b1171eafb1b96e943f0b8ee8.jpg)  
图2-46

MySQL：“为什么不允许删除元素呢？”

Bloom Filter 的设计目的是快速检查元素存在的可能性，而不是支持元素的删除操作，删除意味着需要将对应的 k bit 配置为 0，其中有可能包括其他元素对应的 bit。

# 2.10.3 出招实战：缓存穿透解决方案

Bloom Filter 不是我的标准功能，而是通过拓展实现的，官方从 Redis 4.0 开始提供了插件机制，Bloom Filter 正式登场。

你可以下载官方提供的可拓展模块，或者从GitHub上下载源码自己编译。接下来我以下载源码自行编译的方式来说明如何集成BloomFilter，这里以2.2.14版本为例。

# 下载源码

在GitHub代码库RedisBloom/RedisBloom/releases/tag/v2.2.14下载软件包。

# 解压编译

运行 tar 命令解压下载的软件包。

```batch
tar -zxf RedisBloom-2.2.14.tar 
```

切换到解压出来的软件目录，执行make命令编译插件。

```txt
cd RedisBloom-2.6.3  
make 
```

编译成功，会看到redisbloom.so文件。

# 安装集成

修改redis.conf文件，新增loadmodule配置，并重启Redis。

```txt
loadmodule /opt/app/RedisBloom-2.2.14/redisbloom.so 
```

如果是集群，则每个实例的配置文件都需要加入配置。

指定配置文件并启动Redis。

```txt
redis-server /opt/app/redis-6.2.6/redis.conf 
```

加载成功的页面如图2-47所示。

```txt
9189:H 12 Sep 2023 20:40:42.471 / WARNING: The TCP backlog setting of Sill cannot be enforced because kern.ipc.sanaxconn is set to the lower value of 126   
9189:H 12 Sep 2023 20:40:42.471 h Server initialized   
9189:H 12 Sep 2023 20:40:42.536 \* Module 'bf' loaded from /Users/magebta/documents/develop/RedisBloom-2,2.14/redisbloom.so   
9189:H 12 Sep 2023 20:40:42.539 \* Loading RDB produced by version 7.0.12   
9189:H 12 Sep 2023 20:40:42.539 \* RDB age 197630 seconds   
9189:H 12 Sep 2023 20:40:42.537 \* RDB memory usage when created 1.36 Mb   
9189:H 12 Sep 2023 20:40:42.537 \* Done loading RDB, keys loaded: 5, keys expired: 6.   
9189:H 12 Sep 2023 20:40:42.537 \* OB loaded from disk: 0.001 seconds   
9189:H 12 Sep 2023 20:40:42.537 \* Ready to accept connections 
```

图2-47

# 缓存穿透预防

Bloom Filter 可以解决缓存穿透问题，缓存穿透意味着有特殊请求在查询一个不存在的数据，即数据既不存在于 Redis，也不存在于数据库。当用户购买商品创建订单时，就向 Queue（消息队列）发送消息，把订单 ID 添加到 Bloom Filter，如图 2-48 所示。

![](images/83edec496bbee5dad949bfaf6930f0c2e60a64a96dc0f499b4af4c997c43647f.jpg)  
图2-48

# 创建过滤器

通过 BF.RESERVE orders 0.1 1000000 命令手动创建一个名为 orders error_rate = 0.1，容量为 1000000 的 Bloom Filter。BF.RESERVE 命令的语法如下。

```txt
BF.RESERVE key error_rate capacity [EXPANSION expansion]  
[NONSCALING] 
```

key:BloomFilter的名字。  
◎ error rate：期望的错误率，默认为0.1，值越低，需要的空间越大。  
© capacity: 初始容量，默认为 100，当实际元素的数量超过初始容量时，误判率上升。  
- EXPANSION: 可选参数，当添加到 Bloom Filter 中的数据达到初始容量后，Bloom Filter

会自动创建一个子过滤器，子过滤器的大小是上一个过滤器的大小乘以expansion。expansion的默认值是2，也就是说BloomFilter默认进行2倍扩容。

- NONSCALING：可选参数，配置此项后，当添加到 Blom Filter 中的数据达到初始容量后，不会扩容过滤器，并且会抛出异常（(error) ERR non scaling filter is full）。

Bloom Filter 的扩容是通过增加层数来完成的。每增加一层，在查询时就可能遍历多层 Bloom Filter，每一层的容量都是上一层的 2 倍（默认）。

如果使用Redis自动创建的BlomFilter，那么默认的error_rate是0.1，capacity是100。

Bloom Filter 的 error_rate 越小，需要的存储空间就越大，对于不需要过于精确的场景，error_rate 配置得稍大一点儿也可以。

Bloom Filter 的 capacity 配置得过大，会浪费存储空间；配置得过小，会影响准确率，所以在使用之前一定要尽可能地精确估计好元素数量，还需要加上一定的冗余空间以避免实际元素的数量高出配置值很多。

# 添加数据到BloomFilter

```txt
BF. ADD {key} {item} BF. ADD orders 10086 (integer) 1 
```

使用 BF_ADD 向名为 orders 的 Bloom Filter 添加 10086 这个元素。

如果是多个元素同时添加，则使用BF.MADDkey{item...}。

```txt
BF.MADD orders 10087 10089  
1) (integer) 1  
2) (integer) 1 
```

# 判断元素是否存在

BF EXISTS 判断一个元素是否存在于 BloomFilter，返回值为 1 表示存在，返回值为 0 表示不存在。

```txt
BF EXISTS (key) {item} BF EXISTS orders 10086 (integer) 1 
```

如果需要批量检查多个元素是否存在于Bloom Filter，则使用BF.MEXISTS，返回值是一个数组。

```txt
BF.MEXISTS {key} {item} BF.MEXISTS orders 100 10089 1) (integer) 0 2) (integer) 1 
```

只需要通过BF.RESERVE、BF.add、BF EXISTS三个命令就能避免缓存穿透问题。

Chaya: “如何查看创建的Bloom Filter信息呢？

这个问题问得好，好奇心还是要有的。用BF.INFO key查看。

```txt
BF.INFO orders   
1) Capacity   
2)(integer) 10000000   
3Size   
4)(integer)7794184   
5) Number of filters   
6)(integer)1   
7) Number of items inserted   
8)(integer)3   
9) Expansion rate   
10)(integer)2 
```

$\odot$ Capacity：预设容量。  
Size: 实际占用情况，但如何计算待进一步确认。  
Number of filters: 过滤器层数。  
Number of items inserted: 实际插入的元素数量。  
© Expansion rate: 子过滤器扩容系数（默认为 2）。

# 2.11 Redis高性能的原因

我如今已经成为软件系统必备的中间件之一，是面试官青睐的对象。本节从面试角度提炼知识点，带你融会贯通。

学习新技术时，如果只接触零散的技术点，没有在脑海里建立完整的知识体系，就会很吃力，出现“看起来好像会，过后就忘记”的情况。

65 哥前段时间去面试某大厂，被问到“Redis 的性能为什么这么强”。

65哥：“额……因为它是基于内存操作数据的，内存速度很快。”

面试官：“还有呢？”

65哥：“没了呀。”

很多人仅仅知道Redis基于内存实现，并不了解其核心原因。今日，我带你一起探索真正的原因。

为了让我的性能一骑绝尘，创始人 Antirez 对我的各方面都进行了优化。下次面试的时候，

面试官如果问起Redis的性能为什么如此高，可不能只傻傻地说单线程和内存存储了。

根据官方数据，Redis的每秒请求数（Qequests Per Second，QPS）可以达到100000，有兴趣的读者可以参考官方的基准程序测试报告《How fast is Redis？》，如图2-49所示。

![](images/6258c2adb2034d6c621c55b6258d6d1dc7f1bab09801f7f622e638de58d690aa.jpg)  
图2-49

Redis 的性能强大主要有以下原因。

$\odot$ 基于内存实现。  
$\odot$ 使用I/O多路复用模型。  
单线程模型。  
$\odot$ 高效的底层数据结构。  
$\odot$ 全局散列表。

# 2.11.1 基于内存实现

65. 哥：“这个我知道，Redis 是基于内存的数据库，就像段誉的‘凌波微步’，完全吊打磁盘的速度。对于磁盘数据库来说，首先要将数据通过 I/O 操作读取到内存里。”

没错，读、写操作都是在内存上完成的，下面分别对比一下内存操作与磁盘操作的差异。

图2-50是磁盘操作调用栈流程。

# 内存操作

内存直接由CPU控制，也就是由CPU内部集成内存控制器，所以说内存是直接与CPU对接的，享受与CPU通信的“最优带宽”。Redis将数据存储在内存中，读/写操作不会被磁盘的I/O速度限制。

![](images/d73876fd0b2ef3f8c2e9bb5381a509e6358a746bf7ac49923dd2023cd79ad3d1.jpg)  
图2-50

# 2.11.2 I/O多路复用模型

Redis采用I/O多路复用技术并发处理连接。采用epoll + 自己实现的简单的事件框架。将epoll中的读、写、关闭、连接都转化成事件，再利用epoll的多路复用特性实现一个ae高性能网络事件处理框架，绝不在I/O上浪费一点时间。

65哥：“那什么是I/O多路复用呢？”

在解释 I/O 多路复用之前，我们先了解下基本 I/O 操作会经历什么。

一个基本的网络 I/O 模型处理 get 请求时，会经历以下过程。

（1）服务端bind/listen绑定IP地址并监听指定端口的请求，与客户端建立accept。  
（2）从socket中读取请求recv。  
（3）解析客户端发送的请求 parse。  
（4）执行get命令。

（5）Send执行相应客户端数据，也就是向socket写回数据。

其中，bind、accept、recv、parse 和 send 属于网络 I/O 处理，而 get 命令属于键-值数据操作。既然 Redis 是单线程的，那么，最基本的实现就是在一个线程中依次执行上述操作（Redis 6.0 引入 I/O 多线程模型，图 2-51 描述的是 6.0 之前的版本）。

其中的关键是accept和.recv会出现阻塞，当Redis监听到一个客户端有连接请求，但一直未能成功建立连接时，会阻塞在accept函数，导致其他客户端无法和Redis建立连接。

类似地，当Redis通过recv函数从一个客户端读取数据时，如果数据一直没有到达，那么Redis就会一直阻塞在recv函数，如图2-51所示。

![](images/674561e97c4bc049bc379c66b70e1f1902f13cf46e7f9d332a7613c4704aa543.jpg)  
图2-51

阻塞的原因是使用了传统阻塞 I/O，也就是在执行 read、accept、recv 等函数时会一直阻塞等待，如图 2-52 所示。

![](images/b74185ce93c20dd56f01304452d542874d35490dc3cd2705a5a3f784f788836f.jpg)  
图2-52

# I/O 多路复用

“多路”指多个 socket 连接，“复用”指共同使用一个线程。多路复用主要有 select、poll 和 epoll 三种技术。epoll 的基本原理是，内核不监视应用程序本身的连接，而是监视应用程序的文件描述符。

客户端在运行时会生成具有不同事件类型的套接字。在服务器端，I/O多路复用程序（I/O多路复用模块）会将消息放入队列（图2-53中的I/O多路复用程序的socket队列），然后通过文件事件分派器将其转发到不同的事件处理器。

![](images/991f0009327f118553974efbabdf497f4010a519056ebc10f17bbc40be2d753f.jpg)  
图2-53

简单来说，在单线程条件下，内核会一直监听 socket 上的连接请求或者数据请求，一旦有请求到达就交给 Redis 线程处理，这就实现了一个 Redis 线程处理多个 I/O 流的效果。

select/epoll 提供了基于事件的回调机制，即针对不同事件调用不同的事件处理器。所以Redis一直在处理事件，响应性能得到了提升。

Redis 线程不会阻塞在某一个特定的监听或已连接套接字上，也就是说，不会阻塞在某一个特定的客户端请求处理上。正因如此，Redis 可以同时和多个客户端连接并处理请求，从而提升并发能力。

# 2.11.3 单线程模型

65哥：“为什么Redis不采用多线程并行执行，以充分利用CPU呢？”

单线程指Redis的网络I/O以及field-valuepairs命令读/写是由一个线程来执行的。Redis的持久化、集群数据同步、异步删除等操作都是其他线程执行的。

不过Redis从6.0版本开始支持多线程模型，需要注意的是，Redis多I/O线程模型只用来处理网络读/写请求，Redis的读/写命令依然是单线程处理的。

# 多线程的弊端

使用多线程，通常可以增加系统吞吐量，充分利用CPU资源。

但是如果没有良好的系统设计，就可能出现图2-54所示的场景：在增加线程数量的初期吞吐量随之增加，当进一步增加线程数量时，系统吞吐量几乎不再增加，甚至下降！

![](images/51fcf470e56fed291c109439dced282a018efedb894d5fea2ed9ae83e84d7aeb.jpg)  
图2-54

在运行每个任务之前，CPU需要知道任务在何处加载并开始运行。也就是说，系统需要帮助它预先配置CPU寄存器和程序计数器，这称为CPU上下文。

这些上下文存储在系统内核中，并在重新计划任务时再次加载。这样，任务的原始状态将不会受到影响，并且看起来是连续运行的。在切换上下文时，我们需要完成一系列工作，这会消耗大量资源。

另外，当多线程并行修改共享数据时，为了保证数据正确，需要采用加锁机制，这会带来额外的性能开销，面临共享资源的并发访问控制问题。

引入多线程开发，就需要使用同步原语来保证共享资源的并发读/写，这增加了代码复杂度和调试难度。

# 单线程高性能的原因

Redis 选择使用单线程处理命令以及高性能的主要原因如下。

不会因为创建线程消耗性能。  
避免上下文切换引起的CPU消耗，没有多线程切换的开销。

(2)避免了线程之间的竞争问题, 例如添加锁、释放锁、死锁等, 不需要考虑各种锁问题。  
代码更清晰，处理逻辑简单。

Chaya：“单线程是否可以充分利用CPU资源呢？”

官方答复如下。

◎ 使用Redis时，几乎不存在CPU成为瓶颈的情况，Redis的性能瓶颈主要受限于内存和网络。  
在一个普通的Linux操作系统上，通过使用pipelining，Redis每秒可以处理100万个请求，所以如果应用程序主要使用复杂度为 $O(N)$ 或 $O(\lg N)$ 的命令，那么不会占用太多CPU。  
- 使用了单线程后，可维护性高。多线程模型虽然在某些方面表现优异，但是它引入了程序执行顺序的不确定性，带来了并发读/写的一系列问题，增加了系统复杂度，同时可能存在线程切换，甚至存在加锁、解锁、死锁造成的性能损耗。

Antirez 大佬给我设计了 AE 事件模型以及 I/O 多路复用等技术，处理性能非常高，因此没有必要使用多线程。

单线程机制让Redis内部实现的复杂度大大降低，渐进式Rehash、Lpush等线程不安全的命令都可以无锁进行。

# 2.11.4 高效的数据结构

65 哥：“为了提高检索速度，MySQL 使用了 B+Tree 数据结构，所以 Redis 速度快应该也跟数据结构有关。”

回答正确，这里所说的数据结构并不是Redis提供给我们使用的5种数据类型String、Lists、Hashes、Sets和Sorted Sets，常见的应用场景如下。

$\odot$ String: 缓存、计数器、分布式锁等。  
$\odot$ Lists: 链表、队列、微博关注人的时间轴列表等。  
© Hashes：用户、订单信息。  
© Sets: 去重、赞、踩、共同好友等。  
© Sorted Sets: 访问量排行榜、点击量排行榜等。

为了在性能和内存之间取得平衡，有的数据类型底层使用了不止一种数据结构，如图2-55所示。

![](images/f2644f574cec34b14501af1dc47be46da68954d478f217eccba718bab342a3ee.jpg)  
图2-55

# 2.11.5 全局散列表

Redis 通过一个散列表来保存所有的 key-value，散列表的本质就是数组 + 链表，数组的槽位被叫作哈希桶。每个桶的 entry 保存指向具体 key 和 value 的指针。

key 是 String 类型，value 的数据类型可以是 5 种中的任意一种。如图 2-56 所示。

![](images/f574087195c74d04b0f01d22eae7be94b75c99e005a8b822362c99cae66b2518.jpg)  
Redis全局散列表  
图2-56

我们可以把Redis看作一个全局散列表，而全局散列表的时间复杂度是 $O(1)$ 。通过计算每个键的哈希值，可以知道对应的哈希桶位置，再通过哈希桶的entry找到对应的数据，这也是Redis“快”的原因之一。

# 第3章 不死之身——高可用

# 3.1 実机恢复，不丢数据稳如山

我（Redis）对数据读/写操作的速度快到令人发指，很多程序员把我当作缓存系统来使用，用于提高系统的读取性能。然而，“快”是需要付出代价的：内存无法持久化，一旦断电或者宕机，我保存在内存中的数据将全部丢失。

在这种情况下，没有了我这位高性能缓存大佬的支持，大量流量被发到MySQL，可能带来更严重的问题。

MySQL：“你赶紧重启，然后从我这里获取数据加载到内存！”

不行呀！如果有大量数据需要恢复，会给你造成更大的压力。

MySQL：“那怎么办？”

别怕，我有两大撒手锏，可以实现数据持久化，做到宕机快速恢复，“不丢数据稳如山”，无须从数据库中慢慢恢复数据。它们就是RDB快照和AOF（Append Only File）。

MySQL：“别磨叽了，赶紧开‘搞’吧，我快扛不住了！”

# 3.1.1 RDB 快照

当数据存储在内存中时，我会把内存中的数据写到磁盘上实现持久化，然后在重启时把磁盘的快照数据快速加载到内存中，这样就能实现重启后正常提供服务。

MySQL：“我有一个建议，每次对内存执行写命令都同时写入磁盘。”

你的建议很好，下次不要建议了。

这个方案有一个致命问题：每次写命令不仅写内存还写磁盘，磁盘的性能相对内存而言太差，会导致我的性能大大降低，让我快不起来了。”

# MySQL：“那你如何规避这个问题呢？”

程序员通常把我当作缓存系统使用，对一致性的要求不高，我不需要把所有的数据都保存下来，使用RDB快照的方式来实现宕机快速恢复。

在快速执行大量写命令的过程中，我的内存数据会一直变化。RDB 快照指的就是 Redis 内存中某一刻的数据，好比我们在拍照时，会把某个瞬间的画面定格下来。我把某一刻的数据以文件的形式“拍”下来，写到磁盘上。这个文件叫作 RDB 文件，是 Redis Database 的缩写。

我只需要定时执行RDB快照，就不必在每次执行写命令时都写入磁盘，既实现了快，又实现了持久化。当宕机后重启数据恢复时，直接将磁盘的RDB文件读入内存即可。如图3-1所示。

![](images/eb02b22a83e76d15fafd50e7046d3c1689a786d2cce7e396d6a8414bffe195a6.jpg)  
图3-1

# 1.RDB生成策略

# MySQL：“什么时候触发RDB快照操作呢？”

我用的是单线程模型执行读/写命令，所以需要尽可能避免阻塞RDB文件生成，以免阻塞主线程，先看看有哪些情况会触发RDB快照持久化操作。

有两种情况会触发RDB快照持久化。

$\odot$ 手动触发：执行 save 或 bgsave 命令。  
© 自动触发：一共有四种情况会自动触发执行 bgsave 命令生成 RDB 文件，后文细说。

# 手动触发

我提供了两个命令用于手动生成RDB文件。

save：主线程执行，会阻塞。  
© bgsave：调用 glibc 的函数 fork 产生一个子进程用于写入临时 RDB 文件，RDB 快照持久化完全交给子进程来处理，完成后自动结束，父进程可以继续处理客户端请求，

阻塞只发生在fork阶段，时间很短，生成RDB文件的默认配置使用的就是该命令。当子进程写完新的RDB文件后，会替换旧的RDB文件。

# 自动触发

程序员总不能半夜起来手动执行命令生成RDB文件，我会在以下4种情况下自动触发bgsave操作生成RDB文件。

在redis.conf 中配置 save_m n: 在 m 秒内至少有 n 个 key 更改, 自动触发 bgsave 生成 RDB 文件。  
◎ 主从复制：从节点需要从主节点进行全量复制时会触发 bgsave 操作，把生成的 RDB 文件发送给从节点。  
© 执行 debug reload 命令重新加载 Redis 会触发 bgsave 操作。  
在默认情况下执行shutdown命令时，如果没有开启AOF持久化，那么也会触发bgsave操作。

如果配置为 save "", 则表示关闭 RDB 快照功能。聪明的程序员可根据实际请求压力调整 RDB 快照周期执行策略。

# 其他配置

我还提供了其他用于控制生成RDB文件的配置。

```txt
文件名称  
dbfilename dump.rdb  
#文件保存路径  
dir /opt/app/redis/data/  
#当持久化出错时，主进程是否停止写入  
stop-writes-on-bgsave-error yes  
#是否压缩  
rdbcompression yes  
#导入时是否检查  
rdbchecksum yes
```

stop-writes-on-bgsave-error

我曾提到，在RDB文件生成的过程中，主线程依然可以接收客户端的写命令，但这是在RDB快照操作正常的前提下。

如果生成RDB文件期间出现异常，例如操作系统权限不够、磁盘已满，该配置被配置为yes，我就会禁止执行写操作。当出现RDB快照错误时，允许执行写操作。

rdbcompression

如果启用LZF压缩算法对字符串类型的数据进行压缩，生成RDB文件，则配置为yes。

rdbchecksum

从Redis5.0开始，RDB文件的末尾会有一个64位的CRC校验码，用于验证整个RDB文件的完整性。这个功能大概会损失 $10\%$ 左右的性能，但是能获得更高的数据可靠性，追求极致性能的程序员可将它配置为no。

# 2. 写时复制

MySQL：“在实际生产环境中，程序员通常会给你配置6GB的内存，将这么大的内存数据生成RDB文件落到磁盘的过程会需要较长时间。你如何做到在继续处理写命令请求的同时保证RDB文件与内存中的数据一致呢？”

作为唯快不破的NoSQL数据库“扛把子”，我在对内存数据做RDB快照时，并不会暂停写操作（读操作不会造成数据的不一致）。

我使用了操作系统的多进程写时复制技术（Copy On Write，COW）来实现RDB快照持久化。在持久化时我会调用操作系统glibc函数fork产生一个子进程，RDB快照持久化完全交给子进程来处理，主进程继续处理客户端请求。

在子进程刚刚产生时，它和父进程共享内存里面的代码段和数据段，你可以将父子进程想象成一个连体婴儿，共享身体。

这是Linux操作系统的机制，为了节约内存资源，尽可能让它们共享。在进程分离的一瞬间，内存的增长几乎没有明显变化。

bgsave 子进程可以共享主线程的所有内存数据，所以能读取主线程的数据并写入 RDB 文件。

如果主线程对这些数据进行读操作，那么主线程和 bgsave 子进程互不影响。当主线程要修改某个 field-value pairs 时，这个数据会把发生变化的数据复制一份，生成副本。

接着，bgsave子进程会把这个副本数据写入保存在磁盘的RDB文件中，从而保证数据的一致性，如图3-2所示。

MySQL：“在执行快照期间，你崩溃了怎么办？”

只要数据没有全部写到磁盘中，这次RDB快照就不算成功，崩溃恢复时只能将上一个完整的RDB文件作为恢复文件。

MySQL：“那我建议你每秒执行一次RDB快照，这样宕机最多丢失一秒的数据。”

你的建议很好，下次真的不要再建议了。

![](images/14874971cb669a66ede2ca5efaf100c34c0b2c16bb0b5f8b8b8d8f4aa5967a1b.jpg)  
图3-2

这个方法是错误的，执行bgsave操作时不阻塞主线程，但是，如果频繁地执行全量快照，就会带来两方面的开销。

$\odot$ 频繁生成RDB文件，磁盘压力过大。会出现上一个RDB文件还未生成完，下一个又开始生成的情况，陷入死循环。  
© bgsave 子进程是由主线程 fork 出来的，虽然 bgsave 不会阻塞主线程，但是 fork 会阻塞主线程。内存越大，阻塞时间越长。

# 3. 优缺点

RDB文件的优点如下。

- RDB 文件采用二进制格式数据 + 数据压缩的方式写磁盘，文件体积远小于内存大小，适用于备份和全量复制。  
© RDB 文件加载恢复数据的速度远远快于 AOF 文件。

RDB 文件的缺点如下。

◎ 实时性不够，无法做到秒级持久化。  
通过bgsave调用fork函数创建子进程，子进程属于重量级操作，频繁执行成本高。

针对RDB文件不适合实时持久化等问题，我提供AOF持久化方式来破解。

# 3.1.2 AOF

AOF（Append Only File）持久化记录的是服务器接收的每个写操作，在服务器启动，重放还原数据集。

AOF采用的是写后日志模式，即先写内存，后写日志，如图3-3所示。

![](images/f491ba93ecd828b428f57d607bea577d68537de25e45b99597032e0dc4fc9d01.jpg)  
图3-3

还有一个写前日志（Write Ahead Log）：在实际写数据之前，将修改的数据写到日志文件中，再修改数据。

例如，MySQL InnoDB 存储引擎，在实际修改数据前先记录修改 redo log，再修改数据。

在默认情况下，我并不会开启 AOF 持久化，程序员可以通过配置 redis.conf 文件将其开启。

```txt
yes开启AOF持久化，默认是no appendonly yes #AOF持久化的文件名，默认是appendonly.aof appendfilename"appendonly.aof" #AOF文件的保存位置和RDB文件的位置相同，都是通过dir参数配置的 dir./
```

# 1.日志格式

当我接收到 set key MageByte 命令将数据写到内存后，会按照如下格式写入 AOF 文件，如图 3-4 所示。

◎ *3: 表示当前命令分为三部分,每部分都以 $ + 数字开头,后面紧跟该部分具体的命令、键、值。  
② 数字:表示该部分的命令、键、值占用的字节大小。例如,$3表示该部分包含3字节,也就是 SET 命令。

![](images/1536ee483587b3a92f5735aeb6611c50402cfbd899e8a9ab3223405c784198b3.jpg)  
图3-4

# 2. 写回策略

为了提高文件的写入效率，当系统调用 write 函数时，通常会将待写入的数据暂存在一个内存缓冲区里，当缓冲区的空间被填满或者超过了指定的时限后，才真正将缓冲区中的数据写入磁盘。

这种做法虽然提高了效率，但也为写入数据带来了安全问题，如果计算机宕机，那么保存在内存缓冲区里的数据将会丢失。

为此，系统提供了 fsync 和 fdatasync 两个同步函数，它们可以强制操作系统立即将缓冲区中的数据写入硬盘；从而确保写入数据的安全性。

Redis 提供的 AOF 配置项 appendfsync 写回策略直接决定 AOF 持久化功能的效率和安全性。

© always:同步写回,写命令执行完毕立刻将 aof_buf 缓冲区中的内容刷写到 AOF 文件。  
◎ everysec: 每秒写回，写命令执行完，日志只会写到 AOF 文件缓冲区，每隔一秒就把缓冲区的内容同步到磁盘。  
- no：操作系统控制，写命令执行完，把日志写到 AOF 文件内存缓冲区，由操作系统决定何时同步到磁盘。

没有两全其美的策略，我们需要在性能和可靠性上进行取舍。always 可以做到数据不丢失，但是每个写命令都需要写入磁盘，性能最差。

everysec避免了同步写回的性能开销，发生宕机时可能有一秒未写入磁盘的数据丢失，在性能和可靠性之间做了折中。

no 的性能最好，但是可能丢失很多数据。

# 3. AOF 重写瘦身

MySQL：“随着写入操作的执行，AOF文件过大怎么办？文件越大，数据恢复就越慢。”

为了解决 AOF 文件体积膨胀的问题，创造我的 Antirez 老哥设计了一个撒手锏——AOF 重写机制（AOF Rewrite），对文件进行瘦身。

例如，使用INCR counter实现一个自增计数器，初始值为1，递增1000次的目标是1000，在AOF中保存1000次命令。

在重写时并不需要其中的999个写操作，重写机制有“多变一”功能，将旧日志中的多条命令重写后就变成了一条命令。

其原理是开辟一个子进程将内存中的数据转换成一系列Redis的写操作命令，写到一个新的AOF日志文件中。再将操作期间新增的AOF日志追加到这个新的AOF日志文件中，追加完毕立即用其替代旧的AOF日志文件，瘦身工作就完成了，如图3-5所示。

![](images/60e443ec2589fa65417ef1a1768315eccbf63ed9be532cca5e36e6f2cf723285.jpg)  
图3-5

我提供了 bgwriteaof 命令用于对 AOF 日志进行瘦身。程序员不可能随时随地使用该命令重写文件，否则都没有时间谈恋爱。

所以，我还提供了以下两个配置，实现自动重写策略，解放程序员的双手。

触发重写AOF配置

auto-aof-rewrite-percentage 100

auto-aof-rewrite-min-size 64mb

© auto-aof-rewrite-percentage: 如果当前 AOF 文件的大小超过了上次重写后的 AOF 文件大小, 则开始重写 AOF。  
© auto-aof-rewrite-min-size：触发 AOF 文件重写的最小值。如果 AOF 文件的大小小于这个值，则不触发重写操作。

注意，程序员手动执行 bgwriteaof 命令并不受这两个条件限制。

MySQL：“AOF重写会阻塞主线程吗？”

AOF 重写通过主线程 fork 出一个 bgrewriteaof 子进程，把主线程的内存复制一份给 bgrewriteaof 子进程，子进程就能在不影响主线程的情况下，将内存中的数据生成写操作并记录到重写日志。

因此，在 AOF 重写时，阻塞主线程只发生在主线程 fork 子进程那一刻。

# 重写过程

MySQL：“在重写日志时，有新数据写入内存怎么办？”

总的来说，重写过程中会出现两份日志和一份数据拷贝。分别是旧的 AOF 文件、新的 AOF 文件和 Redis 数据拷贝。

Redis会将重写过程中接收到的写操作同时记录到旧的AOF缓冲区和新的AOF缓冲区，这样新的AOF日志也会保存最新的操作。等到复制数据的所有操作记录重写完成后，新的AOF缓冲区记录的最新操作也会写到新的AOF文件中。

每次 AOF 重写时，Redis 都会先执行内存复制操作，让 bgwriteaof 子进程拥有此时的 Redis 内存快照，子进程遍历 Redis 内存快照中的全部 field-value pairs，生成重写记录。如图 3-6 所示。

![](images/bf473a59592ff02f2fe0160d24fd0220c7d1822cee104e50011ccaca46958300.jpg)

![](images/0da27b0d4ab3b10bbaec71aeafd02879c425d62c0bcf541beda7858e7d2f650f.jpg)  
图3-6

使用两个日志，以确保在重写过程中不会丢失新写入的数据，并且保证数据一致性。

MySQL：“为什么AOF重写不复用旧的AOF文件？”

这个问题问得好，有以下两个原因。

◎ 一个原因是父子进程写同一个文件必然会产生竞争问题，控制竞争就意味着会影响父进程的性能。  
如果 AOF 重写过程失败，那么旧的 AOF 文件相当于被污染了，无法用于恢复。所以 Redis AOF 重写一个新文件，如果重写失败，则直接删除这个文件，不会对旧的 AOF 文件产生影响。当重写完成后，直接替换旧文件即可。

# Multi-Part AOF 机制

MySQL: “在 AOF Rewrite 过程中, 主进程除了把写命令写入 AOF 缓冲区, 还要把写命令写入 AOF 重写缓冲区。一份数据要写入两个缓冲区, 还要写入两个 AOF 文件, 产生两次磁盘 I/O, 太浪费了。”

上述的 AOF Rewrite 操作是 Redis 7.0 之前的逻辑，俗话说得好，“只要思想不滑坡，办法总比困难多”。为了解决性能问题，7.0 版本开始引入 Multi-Part AOF 机制。

除了这个问题，7.0 之前版本的 AOF Rewrite 操作其实还有以下几点性能问题。

◎ 开辟 AOF Rewrite 缓冲区，存放 AOF 重写期间的所有日志，在写命令密集的场景中，AOF Rewrite 缓冲区会占据大量的内存。  
© AOF Rewrite 结束后，由主线程把 AOF Rewrite 缓冲区的数据写入磁盘，缓冲区过大会阻塞命令执行，造成 Redis 耗时尖刺。  
© AOF Rewrite 需要主子进程进行复杂的通信，实现逻辑复杂。

Multi-Part AOF 机制就是把单个 AOF 文件拆分成多个，包括三种不同的类型，不同类型的 AOF 文件有不同的职责。

© Base AOF 文件：子进程执行 AOF Rewrite 操作时生成的文件，有且只有一个。  
© Incr AOF 文件：增量 AOF 文件，在 AOF Rewrite 开始时由主进程创建，用于保存在 AOF 重写期间收到的写操作，可能存在多个这样的文件。  
© History AOF 文件：历史版本的 Base AOF 文件和 Incr AOF 文件。AOF Rewrite 操作执行完成后，原来的 Base AOF 文件和 Incr AOF 文件被标记成 History AOF 文件，并被 Redis 自动删除。

当进行 AOF Rewrite 操作时，Redis 主进程会新建一个 Incr AOF 文件，用于保存整个 AOF Rewrite 操作期间的 AOF 文件，不再写入旧的 Incr AOF 文件。

接着，主线程 fork 出一个子进程，用于执行 AOF Rewrite 操作。子进程会生成一个新的 Base AOF 文件，执行一次内存拷贝，拥有此时的 RDB 文件，遍历 Redis 中的全部 field-value pairs，生成重写记录，写入 Base AOF 文件。

新生成的Base AOF文件与新建的Incr AOF文件结合在一起，就包含了当前Redis的所有数据。AOF Rewrite操作结束后，主线程会使用一个manifest文件来维护这些AOF文件的信息，其实就是记录新生成的Base AOF文件与新建的Incr AOF文件信息，同时把之前的Base AOF和Incr文件标记成History。

你会发现，在整个 AOF Rewrite 操作过程中，不再重复写 AOF 文件，也没有使用 AOF Rewrite 缓冲区暂存日志，如图 3-7 所示。

![](images/c7467f0c3f0c602378a9edc6158b2f34529e6706f988107d028ab1bd17e1a4a9.jpg)  
图3-7

# 4.AOF优缺点

AOF的主要优点如下。

$\odot$ 持久化实时性高。  
◎ 是一种追加日志，不会出现磁盘寻道问题，也不会在断电时出现损坏问题。即使由于某种原因（例如磁盘已满）中断，redis-check-aof工具也能够轻松修复它。  
◎ 易于理解和解析的格式，包含所有操作的日志。  
◎ 写操作执行成功才记录日志，避免了命令语法检查开销，同时不会阻塞当前写命令。

AOF的主要缺点如下。

◎ 由于 AOF 记录的是一个个命令，因此在故障恢复时要执行所有命令，如果文件太大，那么整个恢复过程会非常缓慢。  
$\odot$ 文件系统对文件大小有限制，不能保存过大的文件，文件变大，追加效率也会变低。  
命令执行完成，如果在写日志之前宕机，那么会丢失数据。  
AOF避免了当前命令的阻塞，但是可能会给下一个命令带来阻塞的风险。AOF文件由主线程执行，在日志写入磁盘过程中，如果磁盘压力过大就会导致写入过程很慢，从而导致后续的写命令阻塞。

MySQL：“两种持久化方式都有优缺点，可不可以结合一下做到更好呢？”

在重启Redis时，我们很少使用RDB文件来恢复内存状态，因为会丢失大量数据。我们通常使用AOF文件重放，但是重放AOF文件的性能相对RDB文件要慢很多，在Redis实例很大的情况下，这样启动需要花费很长时间。

Antirez 在 4.0 版本中给我提供了一个混合使用 AOF 文件和 RDB 快照的方法。简单来说，RDB 文件以一定的频率执行，使用 AOF 文件记录两次快照之间的所有写操作。

如此一来，就不需要频繁执行快照，避免了fork对主线程的性能影响，AOF文件不再是全量的，而是生成RDB文件时间的增量AOF文件，这样日志就会很小，不需要重写。

MySQL：“如何从RDB文件和AOF文件中恢复数据呢？”

如果一台服务器上既有RDB文件，又有AOF文件，那么当我重新启动的时候，将优先选择AOF文件来恢复数据，因为它保存的数据更完整。

如果 AOF 文件不存在，则加载 RDB 文件。恢复流程图 3-8 所示。

![](images/13ac4fdf61e410a912c50bd33e5a1125926c9831622af94dffb9991deb28f36f.jpg)  
图3-8

# 3.2 主从复制架构

高可用有两个含义：一是数据尽量不丢失；二是尽可能提供服务。AOF 和 RDB 快照保证了数据持久化尽量不丢失，而主从复制就是增加副本，将一份数据保存到多个实例上，即使有

一个实例宕机，其他实例依然可以提供服务。

本节就带你全方位吃透Redis高可用技术解决方案之主从复制架构。

Chaya：“有了RDB快照和AOF文件，再也不怕宏机丢失数据了，但是Redis实例宕机了怎么办？如何实现高可用呢？”

我提供了主从模式，通过主从复制，将一份冗余数据复制到其他Redis服务器，实现高可用。当只有一台服务器时，一旦宕机就无法提供服务，那么如果有多台服务器，是不是就可以解决问题了？我们将前者称为master（主节点），后者称为slave（从节点），数据的复制是单向的，只能由master到slave。

在默认情况下，每台Redis服务器都是master，且一个master可以有多个slave（或没有slave），但一个slave只能有一个master。

Chaya: "master 与 slave 之间的数据如何保证一致性呢?"

© 读操作：master 和 slave 都可以执行。  
◎ 写操作：master 先执行，之后将写操作同步到 slave。

为了保证副本数据的一致性，主从架构采用了读/写分离的方式，master在执行修改操作时，会把相应的写命令同步给slave，slave回放这些命令，就可以保证自己的主从数据一致。

![](images/02a5e16180adf59ef44ec67d7a53ed08bad4b1956b5982f70df732dd0b44c5d5.jpg)  
图3-9

Chaya: "master 和 slave 都可以执行写命令不是更好吗?

我们可以假设 master 和 slave 都可以执行写命令，那么当同一份数据被分别修改了多次，每次的修改请求被发送到不同的主从实例上时，就会导致实例的副本数据不一致。

如果为了保证数据一致，Redis需要加锁协调多个实例的修改。

Chaya：“主从复制还有其他作用吗？”

$\odot$ 故障恢复：当master宕机时，其他节点依然可以提供服务。  
负载均衡：master 提供写服务，slave 提供读服务，分担压力。

◎ 高可用基石：是哨兵和集群实施的基础。

# 3.2.1 主从数据同步原理

Chaya: "master 和 slave 的同步是如何完成的呢? master 的数据是一次性传给 slave, 还是分批同步? 主从正常运行期间又怎么同步呢? 要是 master 和 slave 间的网络断连了, 重新连接后数据还能保持一致吗?"

你问题怎么这么多？不要急。我知道你想安心地与恋人相会，不受Redis宕机导致的服务报警的干扰。主从数据同步分为4种情况。

© master 和 slave 第一次全量同步。  
© master 和 slave 正常运行期间的数据同步。  
© master 和 slave 网络断开重连同步。  
$\odot$ 无盘复制。

在介绍实现原理之前，先看下如何配置主从复制，每个配置的具体解释，会在后面的章节中给出。

```txt
建立主从关系命令，配置该节点为其他节点的 slave  
replicaof <masterip> <masterport>  
# slave 只读  
replica-read-only yes  
# 积压缓冲区大小，在 master 与 slave 断线重连后，如果是增量复制  
# master 就从缓冲区里取出数据复制给 slave  
repl-backlog-size 128mb
```

# 1. 全量同步

Chaya: “先从主从实例第一次同步说起吧。”

主从库第一次复制过程大体可以分为3个阶段：建立连接阶段（准备阶段）、同步数据阶段、发送同步期间接收的新写命令到slave阶段。

直接给出图3-10，方便你有一个全局的认知，后面我会给出具体介绍。

# 建立连接

第一阶段的主要作用是在master和slave之间建立连接，为数据全量同步做好准备。建立信任，才能开始同步，就好像Chaya你跟你的爱人建立信任才会牵手接吻说风月。

slave 和 master 建立连接后，根据配置信息得到 master 的连接地址，就发送 sync 命令并告诉 master 即将进行同步，主库确认回复后，主从库就进入下一阶段的同步了。

![](images/9fd38d49bdbf214cb231a36a6f4b0d2ede9d34d3624a8712cebae20baa1a7e92.jpg)  
图3-10

Chaya: “slave怎么知道master的信息并建立连接呢？”

在 slave 的配置文件中的 replicaof 配置项中配置了 master 的 IP 地址和 port, slave 就知道自己要和哪个 master 进行连接了。

slave 内部维护了 masterhost 和 masterport 两个字段,用于存储 master 的 IP 地址和 port 信息。从库发送的 psync 命令包含主库的 replid 和复制进度 offset 两个参数。

```txt
PSYNC <runID> <offset> 
```

© runID: 每个Redis实例启动时都会自动生成一个唯一标识ID，在第一次主从复制时，slave还不知道主库runID，所以runID会被配置为“?”。  
$\odot$ repl_offset：记录当前复制进度偏移量，slave记录的偏移量与master记录的偏移量之间的数据差，就是需要复制的增量数据。第一次主从复制，将repl_offset配置为-1，表示全量复制。

master收到slave的PSYNC命令后，一看是“?”和“-1”，表示第一次要进行全量复制，并向slave回复 $+\mathrm{FULLRESYNC}$ <runID> <repl_offset>。

# 同步数据

进入第二阶段，Redis master 执行 bgsave 命令生成 RDB 文件，并将文件发送给 slave，这就是 master 对 slave 的“爱意”。

slave 收到 RDB 文件并将其保存到磁盘，清空当前数据库的数据，再加载 RDB 文件数据到内存中，同时会把 master 的 runID 和 master_offset 记录下来。

Chaya：“我的网络是万兆专线，能不能直接传输，不使用磁盘作为中间存储？

你真“6”，确实可以。通常全量同步需要在磁盘上创建RDB文件，把RDB文件传输到slave之后保存到磁盘再重新加载到内存。

如果磁盘比较慢，那么对于 master 服务器来说可能是一个压力很大的操作，Redis 2.8.18 是第一个支持无盘复制的版本。如果网络“快到飞起”，那么确实可以开启无盘复制的方式，master 直接通过网络将 RDB 文件发送到 slave。

无磁盘复制不仅提高了性能，还通过在完全重新同步期间消除写入和读取RDB文件到磁盘的需求，简化了复制过程。

发送同步期间接收的新写命令到 slave

master 为每个 slave 开辟一块 replication buffer 缓冲区。

Chaya: “这个缓冲区有什么用？”

RDB 文件保存的只是某一时刻 Redis 的内存快照，此后 master 接收到的写命令没有传输到 slave，所以 master 为每个 slave 开辟一块 replication buffer 缓冲区记录从生成 RDB 文件开始收到的所有写命令。

第三阶段，slave加载RDB文件完成后，master把replication buffer缓冲区的数据发送到slave，主从数据保持一致。

我会为每个连接到 master 的 slave 开辟一个 replication buffer 缓冲区，因为每个 slave 开始同步的时刻可能不一样，所以要分别配置缓冲区。

只要 slave 和 master 建立好连接，对应的缓冲区就会被创建，断开连接，这个缓冲区就会被释放。

一个在 master 端上创建的缓冲区，存放的数据是下面三个时间内所有的 master 数据写操作。

$\odot$ master 执行 bgsave 产生 RDB 文件期间的写操作。  
© master 发送 RDB 文件到 slave 网络传输期间的写操作。  
© slave 加载 RDB 文件把数据恢复到内存期间的写操作。

无论是和客户端通信，还是和 slave 通信，Redis 都分配一个内存 buffer 进行数据交互，客户端是一个 client，slave 也是一个 client。每个 client 连上 Redis 后，Redis 都会为其分配一个专有 client buffer，所有数据交互都是通过这个 buffer 进行的。

master 先把数据写到这个 buffer 中，然后通过网络发送出去，这样就完成了数据交互。

无论主从在增量同步还是全量同步，master 都会为其分配一个 buffer，只不过这个 buffer 专门用来将写命令传播到从库，以保证主从数据一致，我们通常把它叫作 replication buffer。

# 2.增量同步

Chaya: "master和slave的网络断开重连了怎么办？要重新进行全量复制吗？"

其实在上面整个过程完成之后，全量复制就完成了，只要连接不中断，就会持续进行基于长连接的命令传播复制。

在Redis2.8之前，如果主从复制在命令传播时出现了网络闪断，slave就会和master重新进行一次全量复制，开销非常大。

Chaya，你跟你的爱人偶尔吵架闹小矛盾，只是一时断开联系，不可能完全忘了对方，气消了之后依然会接吻表达爱意，对吧？

从Redis2.8开始，我也做了优化，在网络断开重连后，slave会尝试采用增量复制的方式继续同步。

增量复制用于网络中断等情况后的复制，只将中断期间 master 执行的写命令发送给 slave，与全量复制相比更加高效。

repl.Backlog

断开重连增量复制的实现奥秘就是 repl.Backlog 缓冲区，它是一个定长的环形数组，如果数组内容满了，就会从头开始覆盖前面的内容。不管在什么时候，master 都会将写命令记录在 repl.Backlog 中，它记录 master 接收的新的写请求数据的偏移量和新写命令，这样在 slave 重新连接后，就可以获取未同步的命令发送给 slave 了。

master 使用 master_repl_offset 记录自己写到的位置偏移量，slave 则使用 slave_repl_offset 记录已经读取到的偏移量。

master 收到写操作，偏移量会增加。slave 持续执行同步的写命令后，replbackslog 的已复制的偏移量 slave_repl_offset 也在不断增加。

在正常情况下，这两个偏移量基本相等，在网络断连阶段，master可能收到新的写操作命令，所以master_repl_offset会大于slave_repl_offset，如图3-11所示。

![](images/b50cf3b2ed47ace6c670057b12f75d8b0ba924f396b5480469b5f80f17f3241b.jpg)  
图3-11

当主从断开重连后，slave 会先发送 psync 命令给 master，同时将自己之前保存的 master 的 runID、slave_repl_offset 发送给 master。

master 只需把 master_repl_offset 与 slave_repl_offset 之间的差异命令同步给从库即可。增量复制执行流程如图 3-12 所示。

![](images/45328b808ab44af520e14d7edc704941724c571eb96758914717feba8fd47fc0.jpg)  
图3-12

需要注意的是，在进行主从复制时，master接收到的写操作在写入replicationbuffer的同时，还会写入repl.Backlog的缓冲区。

© replication buffer: 对于每个与 master 连接的 slave, master 都会开辟一个 replication buffer 给 slave 独占。  
$\odot$ repl_backlog 是一个环形缓冲区，整个 master 进程中只会存在一个，由所有的 slave 共用。repl_backlog 的大小通过 repl_backlog-size 参数配置，默认为 1MB，可以根据每秒产生的命令，以及 master 执行 rdb bgsave、master 发送 RDB 文件到 slave 和 slave 加载 RDB 文件的时间之和来估算积压缓冲区的大小，repl_backlog-size 值不小于这两者的乘积。

总的来说，replication buffer 是主从在进行全量复制时，master 用于和 slave 连接的客户端的 buffer，master 会为每个 client 开辟一块，用于传输命令。

repl.Backlog_buffer是master专门用于持续保存写操作的buffer，目的是支持从库增量复制。

repl.Backlog_buffer在Redis服务器启动后一直接收写操作命令，这是所有slave共享的。master和slave会各自记录自己的复制进度，所以，不同的slave在恢复时会把自己的复制进度（slave_repl_offset）发给master，'master就可以和它独立同步了，如图3-13所示。

![](images/54d60def4df321462afb09b97d0aa295f186451901a85861e5744afa312c00d6.jpg)  
图3-13

Chaya: “如果 repl_backlog 太小, slave 还没读取到就被 master 的新写操作覆盖了, 怎么办呢?”

一旦被覆盖就会执行全量复制，我们可以调整 repl_backlog_size 这个参数用于控制缓冲区大小，计算公式如下。

```txt
repl.Backlog = second * write_size_per_second 
```

$\odot$ second：从服务器断开重连主服务器所需的平均时间。  
$\odot$ write_size_per_second: master 平均每秒产生的命令数据量大小（写命令和数据大小总和）。

例如，如果master服务器平均每秒产生1MB的写数据，而slave断线之后平均要5秒才能重新连接上master，那么复制积压缓冲区的大小就不能低于5MB。

安全起见，可以将复制积压缓冲区的大小设为 $2^{\star}$ second * write_size_per_second，这样可以保证绝大部分断线情况都能用部分重同步来处理。

# 3. 正常运行期间的同步

Chaya：“完成全量同步后，正常运行期间主从如何同步呢？”

当主从完成了全量复制后，它们之间会一直维护一个网络连接，master通过这个连接将后

续收到的命令再传播给 slave, 这个过程也被称为基于长连接的命令传播, 使用长连接的目的就是避免频繁建立连接导致的开销。

# 4. 缓冲区演化

Chaya: “我发现一个问题：不管是全量复制还是增量复制，当写请求到达 master 时，命令会分别写入所有 slave 的 replication buffer 及 repl_backlog。重复保存太浪费内存了。”

确实，master 为每个 slave 开辟的 replication buffer 存储的内容是一样的。此外，repl.Backlog 的内容也与 slave 的 replication buffer 有重复。

所以，在7.0版本中，我对上述问题进行了优化，采用了共享缓冲区的设计解决重复保存数据的问题。

既然存储内容是一样的，那么本着“勤俭持家”的原则，最直观的想法就是在命令传播时，将这些写命令放在一个全局的复制缓冲区中，多个slave共享这份数据，不同slave引用缓冲区的不同内容，这就是共享缓冲区的核心思想。

共享缓冲区方案将 replication buffer 数据切割成多个 16KB 的数据块（replBufBlock），并使用 redisServer.repl_buffer_blocks 链表维护，如图 3-14 所示。

![](images/f3f78545f64ea02f5dd01559bcaf9b8aed6ca06dd9b9fbbc272740823fe4d648.jpg)  
图3-14

replBufBlock 的定义如下面的源码所示。

```txt
typedef struct replBufBlock ( int refcount; long long id; long long repl_offset; size_t size, used; char buf[];   
} replBufBlock; 
```

© refcount: 当前 replBufBlock 被引用的次数，当降为 0 时表示可以回收。  
© id: block 的唯一标识，单调递增。  
$\odot$ repl_offset: 记录 buf 第一字节对应的 offset，表示从该块的哪个位置开始向副本发送数据。  
$\odot$ size 和 used: 描述缓冲块的总大小和已使用的空间。  
$\odot$ buf[]：缓冲块内部的实际数据存储，用于保存要发送到副本的复制数据。

master向slave传播命令时，可以直接从redisServer repl_buffer_blocks链表定位到需要传输给slave的replBufBlock，接着让slave的client->ref_repl_buf_node指针指向这个replBufBlock实例，将这块命令传播给slave，避免为每个slave创建一块缓冲区，存储重复的内容。

除此之外，repl.Backlog复用了redisServer.repl_buffer_blocks链表的数据，repl.Backlog中有一个blocks_index字段维护了一个rax树，它的key是replBufBlock的起始repl_offset，value指向相应的replBufBlock实例，如图3-15所示。

![](images/f4b5d6ea5ece5f8bdb70238f073668d406c4a303ede6a81e6efe715b7e4784ee.jpg)  
图3-15

# 5. 如何确定执行全量同步还是部分同步

从Redis2.8开始，slave可以发送psync命令请求同步数据，此时根据master和slave当前状态的不同，同步方式可能是全量复制或部分复制，如图3-16所示。本例基于Redis2.8及之后的版本。

![](images/ad681b2a0890ef7324fe1ea85b4b3b0a651b04234c00f53d805cace18f1d0a9b.jpg)  
图3-16

$\odot$ slave 根据当前状态，发送 sync 命令给 master。

- 如果 slave 从未执行过 replicaof，则发送 psync? -1，向 master 发送全量复制请求。  
- 如果 slave 之前执行过 replicaof，则发送 psync <runld> <offset>, runld 是上次复制保存的 master runld, offset 是上次复制截止时 slave 保存的复制偏移量。

$\odot$ master根据接收到的psync命令和当前服务器状态，决定执行全量复制还是部分复制。

- 当 master_runID 与 slave 发送的 runID 相同，且 slave 发送的 slave_repl_offset 之后的数据在 replbackslog_buffer 缓冲区中都存在时，回复 CONTINUE，表示进行部分复制，slave 等待 master 发送其缺少的数据即可。  
- 当 master runID 与 slave 发送的 runID 不同，或者 slave 发送的 slave_repl_offset 之后的数据已不在 master 的 repl.Backlog_buffer 缓冲区中（在队列中被挤出了）时，回复 slave FULLRESYNC <runID> <offset>，表示要进行全量复制，其中 runID 表示 master 当前的 runID，offset 表示 master 当前的 offset，slave 保存这两个值，以备使用。

当一个 slave 节点和 master 断连时间过长，导致 master repl.Backlog 的 slave_repl_offset 位置上的数据被覆盖时，master 和 slave 将进行全量复制。

# 3.2.2 主从同步的缺点

# 1. 主从复制无限循环

replication buffer 由 client-output-buffer-limit slave 配置，如果这个值太小则会导致主从复制连接断开。

◎ 当 master-slave 复制连接断开时，master 会释放连接相关的数据，replication buffer 中的数据也就丢失了，此时主从之间重新开始复制过程。  
- 还有一个更严重的问题，主从复制连接断开会导致主从重新执行 bgsave，以及 RDB 文件重传操作无限循环的情况。

当master数据量较大，或者master和slave之间的网络延迟较大时，可能导致该缓冲区的大小超过限制，此时master会断开与slave之间的连接。

这种情况可能引起“全量复制 $\rightarrow$ replication buffer 溢出导致连接中断 $\rightarrow$ 重连”的循环。

# 2. 内存过大

如果Redis单机内存达到10GB，则单个slave的同步时间处于分钟级别，当slave较多时，恢复的速度会更慢。

当数据量过大，全量复制阶段 master fork 子进程和保存 RDB 文件耗时过大时，slave 长时间接收不到数据会触发超时，master 和 slave 的数据同步同样可能陷入“全量复制 $\rightarrow$ 超时导致复制中断 $\rightarrow$ 重连”的循环。

# 3. 主从不一致问题

Chaya: “主从模式下, slave 可以执行客户端写请求吗？”

我的建议是将 slave 配置为 read-only 模式，也就是只读，否则有可能导致主从数据不一致。

此外，通常还会给 slave 添加 eplica-ignore-maxmemory no 配置，不让 slave 执行内存淘汰的操作，而是由 master 来决定是否淘汰，field-value pairs 失效 master 会发送 DEL 命令给 slave。

# 3.3 嗅兵集群

通过之前的学习，你已知道Redis主从复制是高可用的基石，某个slave宕机依然可以将请求发送给master或者其他slave，但是如果master宕机，则只能响应读操作，写请求无法再执行。

所以主从复制架构面临一个严峻问题：master宕机，无法执行写操作，无法自动选择将一个slave切换为master，也就是无法实现自动故障切换。

Chaya: “还记得那晚我与男友约会, 眼前是橡树的绿叶, 白色的竹篱笆。好想告诉我的他,这里像幅画, 一起手牵手么么哒 (此处省略 10000 字)。”

“Redis忽然宕机，男友总不能把我推开，停止甜蜜，然后打开电脑手工进行主从切换，再通知其他程序员把地址改成新master的信息上线。”

如此一折腾，你心里的雨倾盆地下，万万使不得。所以必须有一个高可用的方案，为此，我提供一个高可用方案——哨兵（sentinel）。

吃瓜群众：“Redis大佬，虽然我没有女朋友，但是未雨绸缪，我要掌握这个哨兵模式，防止当我与女朋友约会时被打扰，你快说说什么是哨兵以及哨兵的实现原理吧。”

先来看看哨兵是什么？搭建哨兵集群的方法我就不细说了，假设三个哨兵组成一个哨兵集群，三个数据节点构成一个一主两从的Redis主从架构，如图3-17所示。

![](images/07e9e862055f555a7f36c2cc7bbe504cb071422f571e69f22566433220d30df3.jpg)  
图3-17

Redis哨兵集群高可用架构有三种角色，分别是master、slave和sentinel。

$\odot$ sentinel 之间互相通信，组成一个集群实现哨兵高可用，选举出一个 leader 执行故障迁移操作。  
$\odot$ master 与 slave 之间通信，组成主从复制架构。  
$\odot$ sentinel 与 master/ slave 通信，是为了对该主从复制架构进行管理，包括监视（Monitoring）、通知（Notification）、自动故障切换（Automatic Failover）、配置提供者（Configuration Provider）。

```txt
# sentinel.conf
# sentinel monitor <master-name> <ip> <redis-port> <quorum>
sentinel monitor mymaster 127.0.0.1 6379 2 
```

哨兵监控的 master 的名字叫作 mymaster, master 的 IP 地址是 127.0.0.1, 端口是 6379。quorum 是关键参数，它的作用如下。

◎ 指定在标记 master 故障并尝试执行故障切换时需要一定数量达成一致意见的哨兵进程。大白话就是需要多少个哨兵进程认为 master 宕机，真正标记 master 宜机才能启动故障切换过程。  
对于多个哨兵，需要选出一个 leader 来执行实际的故障自动转移操作，当某个哨兵的票数超过 quorum 时，就选举这个哨兵为 leader，负责自动故障切换。quorum 的值一般取消哨兵个数的一半以上 $(n / 2 + 1)$ 比较合理。

哨兵只要配置 master 信息即可与三个角色建立联系。

Chaya：“为什么哨兵只需要配置master信息就可以与三个角色建立联系？”

$\odot$ 喊兵可以通过master获取slave的信息，并与slave建立连接。master与slave是主从关系，通过info命令就可以通过master获取slave的IP地址和port、runid等信息。  
通过上面的步骤，哨兵与master和所有的slave建立连接，哨兵之间的互相感知则通过Redis的发布/订阅机制实现。每个哨兵通过发布/订阅master的__sentinel_:hello频道发布和接收信息，以此感知其他哨兵的存在并建立连接。

# 3.3.1 嗅兵的任务

哨兵是Redis的一种运行模式，它专注于对Redis实例（master、slave）运行状态的监控，并能够在master发生故障时通过一系列的机制实现选主及主从切换，实现自动故障切换，确保整个Redis系统的可用性。

Chaya 可以安心地与爱人在欢乐港湾约会，尽情享受甜蜜，哪怕是吵架都那么醉人，不再需要担心Redis忽然宕机带来的烦恼。

我们先从全局看哨兵，简要地了解它的整个运作流程，接着针对每个任务详细分析，Redis哨兵的主要职责如下。

◎ 监控（Monitoring）：Redis的哨兵不断检查master和slave实例是否按预期工作。它监视实例的健康状态，包括master和所有slave。  
◎ 自动故障切换（Automatic Failover）：如果 master 出现故障或不按预期工作，Redis 的哨兵则启动自动故障切换流程。在此过程中，一个 slave 会被晋升为新的 master。

通知（Notification）：让 slave 执行 replicaof 命令与新的 master 同步数据；并且通知客户端与新的 master 建立连接，如图 3-18 所示。  
配置提供者（Configuration Provider）：哨兵充当了客户端服务发现的权威来源。客户端连接到任何一个哨兵以获取新的 master 的地址，确保能够连接到正确的实例。

![](images/1ab5b6fe62fda7a438854f6e7f73feba94b5ba4ab0ecf7371ecd71180336d20e.jpg)  
图3-18

哨兵也是一个Redis进程，只是不对外提供读/写服务，哨兵通常被配置为单数，原因如下。

# 1. 监控

八卦一下，Chaya，你的恋人用什么方式来了解你每天的喜怒哀乐呢？

Chaya：“这很简单，他每天给我发微信消息、打电话或者打视频，若是哪天我不接电话，或者他发送微信消息时出现红色感叹号，就说明我把他拉黑了。”

哨兵与各个角色节点建立连接后，通过PING、INFO、PUBLISH/SUBSCRIBE命令来监控所有实例的健康状态，当然，它不会说情话。

哨兵默认会以每秒一次的频率向所有的 master、slave、哨兵发送 PING 命令，这个其实是一个心跳检测，用于探测实例是否存活。

◎ PING：所有节点之间通过发送PING命令确认对方是否在线，默认每秒发送一次。  
© INFO：哨兵向 master、slave 发送该命令，用于获取 slave 的详细信息。  
PUBLISH/SUBSCRIBE：哨兵会订阅 master 和 slave 的 __sentinel__:hello 频道，并通过该频道发布自己的信息，这样其他哨兵之间就可以建立联系。

如果一个 master 实例距离最后一次有效回复 PING 命令的时间超过 down-after-milliseconds 选项所指定的值，这个 master 实例就会被哨兵标记为“主观下线”。

如果 slave 没有在指定时间内响应哨兵的 PING 命令，则直接被标记为“主观下线”。

只有当大于或等于法定个数（quorum）的哨兵节点认为该master主观下线时，才能将该master改为客观下线。接着才会开启自动故障切换流程。

PING命令的回复有两种情况。

$\odot$ 有效回复：返回 $+\mathrm{PONG}$ 、-LOADING和-MASTERDOWN中的任何一种。  
$\odot$ 无效回复：有效回复之外的回复，或者不在指定时间内返回任何回复。

Chaya: “主观下线和客观下线的作用是什么？”

主要是为了避免出现哨兵误判 master 运行的情况，一旦出现误判，就会出现 master 实际没有下线，可是哨兵误以为其已经下线的情况，接着就会启动主从故障切换流程，之后的选主和通知操作都会消耗大量资源。

误判一般会发生在集群网络压力较大、网络拥塞或者是 master 本身压力较大的情况下。

既然一个哨兵容易误判，那就使用多个哨兵进行投票判断。哨兵机制也是类似的，采用多实例组成的集群模式进行部署，就是哨兵集群。引入多个哨兵实例一起进行判断，就可以避免单个哨兵因为自身网络状况不好，而误判主库下线的情况。

同时，多个哨兵的网络同时不稳定的概率较小，由它们一起做决策，也能降低误判率。

# 主观下线

主观下线（Subjectively Down，SDOWN）指一个哨兵认为一个Redis实例已经不可用或者已经下线，这有可能是网络不通、心跳超时或连接失败等原因导致的。

例如对于 master 或者 slave，在 down-after-milliseconds 指定的毫秒数之内，如果没有向哨兵发送的 PING 命令回复，或者返回一个错误，那么哨兵会将这个服务器标记为主观下线。

需要注意的是，Redis的哨兵的主要目标是确保master的高可用性，而不是slave的高可用性。因此，主观下线和客观下线的主要关注点通常是master。slave通常不会被单独标记为客观下线，因为它们不承担master的关键角色，它们的主要责任是复制数据。

# 客观下线

判断 master 是否下线不能只由一个哨兵说了算，只有过半的哨兵判断 master 主观下线，才能将 master 标记为客观下线，如图 3-19 所示。

之前提到过 sentinel monitor <master-name> <ip> <redis-port> <quorum> 的配置，参数 quorum 是判断客观下线的依据之一，意思是至少有 quorum 个哨兵判定这个 master 主观下线，才会将这个 master 标记为客观下线。

只有 master 被判定为客观下线，才会进一步触发哨兵执行主从切换流程。

![](images/187620c783b6e3a1846851fd59ac64f228917d4c379acaf5490cd5c4c4b07109.jpg)

![](images/e340f3a801774f691cc0c76a7acf16db827fb68d5c5c9f056a5ac183d3b7435b.jpg)  
图3-19

# 2. 自动故障切换

Chaya: “一旦判断 master 客观下线，就在 slave 中选一个作为新的 master 吗？”

哨兵的第二个任务是选择一个 slave 作为新的 master，并对外提供服务。之后其他 slave 会与新的 master 进行主从复制，这个过程叫作自动故障切换，如图 3-20 所示。

![](images/4a06b341622a0c1ffe0f2d1b82563ddf1b4b258c30177cf2d4dfdcadc44ef9ac.jpg)  
图3-20

吃瓜群众：“如何从众多 slave 中选出一个做 master 呢？”

Chaya: “我觉得筛选过程就像找恋爱对象，每个人心中都有标尺，会通过直觉、习惯和自

己的标准从所有的追求者中选择一个最适合自己的。”

类似地，Redis有自己的筛选规则，按照一定的筛选条件和打分策略，选出一个“节点”担任master。

# 筛选条件

Chaya: “有哪些筛选条件？”

$\odot$ 下线或网络断连的 slave 直接丢弃。  
$\odot$ 网络无异常：slave最后一次响应PING命令的时间不能超过5倍PING周期；slaveINFO（每10s发送一次INFO命令）的信息更新时间不能超过3倍INFO刷新周期。  
$\odot$ 评估过往的网络状态：slave与master断开连接，断连时间不能超过（现在-master被标记为下线的时间）+（master的down-after-milliseconds配置项的值乘以10），单位是毫秒。

总之，下线或者网络经常断开的 slave 不能要。如果新的 master 很快出现网络故障，就又得重新选择新的 master，这不“闹着玩”吗，得排除掉！

# 打分

过滤掉不合适的 slave 之后, 使用快速排序对 slave 列表进行打分, 按照以下排序找出“王者”。

（1）slave优先级：通过replica-priority100配置项，给不同的slave配置不同优先级，默认是100，值越低，优先级越高，配置为特殊值0表示不会晋升为master。  
（2）更大复制偏移量（processed replication offset）：已复制的数据量越多，slave_repl_offset与master_repl_offset的差值就越小。  
（3）slave runID：在优先级和复制进度都相同的情况下，runID 最小的 slave 得分最高，该 slave 会被选为新的 master。

哨兵向筛选出来的 slave 发送 slave no one 命令，使得该 slave 成为新的 master，哨兵并不关心命令返回的结果，它会发送 info 命令给 slave，并根据命令的回复内容确认 slave 是否成功转换为 master。

Chaya: “旧的 master 重新恢复正常时要怎么处理？”

新的不去，旧的不来，有些人一旦错过就不在，既然已经错过，相逢也只能是过客。原master恢复正常，重新连接哨兵，这时集群已经有新的master，所以旧的master被哨兵降级为slave。

# 3. 通知

新的 master 出现后，哨兵还有一件重要的事情要做——将新的 master 的连接信息通过 slave 命令发送给其他 slave，通知 slave 执行 replaceof 命令和新的 master 建立连接进行主从复制。

接着，哨兵会定时给 slave 发 INFO 命令，从 INFO 命令的回复内容来确认 slave 是否与新的 master 成功建立连接。检测到所有 slave 都与新的 master 建立连接，自动故障切换就完成了。

如果还有剩余 slave 没有连上新的 master，则哨兵还会再做一次努力，再次向这些 slave 发送 slave 命令，要求他们与新的 master 建立连接。

# 4. 配置提供者

Redis 客户端只需要跟哨兵打交道，就可以无感知地连接到新的 master，最重要的原因是哨兵提供了一些 API 来检查主从节点的运行状况。

# 3.3.2 喊兵集群原理

如果只有一个哨兵就会存在单点故障问题。Redis sentinel 是一个分布式系统，由多个哨兵协作组成集群实现高可用。

◎ 当多个哨兵达成一致认为某个 master 不可用时, 才执行故障迁移, 降低了误报的概率。  
$\odot$ 不需要所有哨兵都可用，哨兵集群依然可以正常工作。

Chaya: “哨兵是如何感知其他哨兵节点的呢? 又如何知道 slave 节点的信息并监控呢? 当 master 不可用时, 到底由哪个哨兵来执行自动故障切换呢?”

# 1. 发布/订阅机制

# 哨兵互相发现

哨兵之间可以互相感知发现，这归功于Redis的发布/订阅机制。当哨兵与master建立连接后，使用发布/订阅机制在特殊的频道发布自己的信息，例如IP地址和端口，同时订阅该频道获取其他哨兵发布的消息。

master 有一个 __sentinel__:hello 的专用通道，用于哨兵之间发布和订阅消息。可以比喻为哨兵利用 master 建立的 __sentinel__:hello 微信群发布自己的消息，同时关注其他哨兵发布的消息，如图 3-21 所示。

![](images/aaa064bdfd3d6623bdee7d94e37967bc86f28cc92908f553e41b5bdb5ae5b55f.jpg)  
图3-21

哨兵如何感知并监控 slave

哨兵之间建立连接形成集群还不够，哨兵还需要跟所有 slave 建立连接，否则无法监控它们。除此之外，如果发生了主从切换也需要通知 slave 重新与新的 master 建立连接进行数据同步。

哨兵向master发送INFO命令，master接收到命令后，将slave列表告诉哨兵。哨兵根据master响应的slave名单信息与所有salve建立连接，并且根据这个连接持续监控slave，剩下的哨兵也基于此实现监控，如图3-22示。

![](images/c5117b9f4fe39b6b979b1f92bbd6e72eb35c6badafca4ea5fc92eca75a66f67b.jpg)  
图3-22

# 2. 选择哨兵执行主从切换

Chaya: "master不可用后，如何选择一个哨兵来执行自动故障切换呢？

任何哨兵判断 master 主观下线后，都会向其他哨兵发送 is-master-down-by-addr 命令，其他哨兵收到命令后则根据自己与 master 之间的连接状况分别响应 Y 或者 N，Y 表示赞成，N 表示反对。

如果某个哨兵获得了大多数哨兵的赞成票，就标记 master 为客观下线。

例如，一共有3个哨兵组成集群，那么quorum就可以配置为2，当一个哨兵获得了2张赞成票（包含自己的1票）时，就可以标记master“客观下线”。

获得多数赞成票的哨兵向其他哨兵发送 SENTINEL is-master-down-by-addr <masterip> <masterport> <sentinel.current_epoch> * 命令，声明自己想要执行主从切换并开始拉票。其他哨兵则进行投票，投票过程叫作 leader 选举，选举的过程借鉴了分布式系统中的 Raft 协议。

简单地说，哨兵标记当前 master 客观下线后，通过投票的方式从哨兵集群中选举出一个哨兵作为 leader 角色执行故障切换。

Chaya: “我发现判断 master 是否客观下线和哨兵拉票选举 leader 是同样的命令。”

没错，is-master-down-by-addr命令有两个作用：一是询问其他哨兵是否认为某个master已经主观下线；二是开始进行自动故障切换时，当前哨兵向其他哨兵实例进行"拉票”，让其他哨兵选举自己为leader。

哨兵想要成为 leader 没那么简单，得有两把“刷子”。需要满足以下条件。

© 获得其他哨兵过半的投票。  
投票的数量大于或等于quorum的值。

如果sentinel集群有2个实例，此时，一个哨兵要想成为leader，那么必须获得2票，而不是1票。所以，如果有一个哨兵宕机了，那么此时的集群是无法进行主从库切换的。因此，通常我们至少会配置3个哨兵实例。

![](images/5c1cb6aa3c8f70ee6882bc28138cfafe353e95c4f40eee12e8029348ef2d3112.jpg)  
图3-23

# 3. 发布/订阅机制

在Redis中，发布/订阅（Pub/Sub）机制发布不同事件，让客户端订阅消息。哨兵提供的消息订阅频道有很多，不同频道包含了主从库切换过程中的不同关键事件。

master 相关

与master相关的消息订阅频道。

$\odot$ +sdown：节点处于主观下线状态。   
- sdown：节点不再处于主观下线状态。   
$\odot$ +odown：节点进入客观下线状态。   
- -odown：节点退出客观下线状态。  
$\odot$ +switch-master: master 地址发生了变化。

# slave 相关

与 slave 相关的消息订阅频道。

© +slave-reconf-sent: leader哨兵发送 REPLICAOF命令重新配置从库。  
$\odot$ +slave-reconf-inprog: slave 配置了新的 master, 但是尚未同步。  
© +slave-reconf-done: slave 配置了新的 master，并完成了数据同步。

Redis 的发布/订阅机制尤其重要，有了发布/订阅机制，哨兵和哨兵之间、哨兵和 slave 之间、哨兵和客户端之间就都能建立起连接了，各种事件的发布也是通过这个机制实现的。

# 3.4 Redis集群

Chaya：“感谢Redis大佬，自从用上了哨兵集群实现自动故障切换后，男朋友终于可以开心地跟我约会，不怕Redis忽然宕机了。  
“可是他最近遇到一个糟心的问题：Redis 数据库需要保存 800 万个 field-value pairs，占用 20 GB 内存。于是他使用了一台 32GB 内存的主机部署，但是 Redis 响应有时候非常慢，使用 INFO 命令查看 latest_fork_use 指标（最近一次 fork 耗时），发现特别高。”

latest_fork_use 指标高是 RDB 持久化机制导致的，fork 子进程完成 RDB 文件持久化操作，其耗时与 Redis 数据量正相关。fork 子进程执行时会阻塞主线程，数据量过大会导致主线程阻塞时间过长，所以出现了 Redis 响应慢的现象。

Chaya: “除此之外，随着数据量越来越大，主从架构升级单个实例时很容易使硬件性能达到瓶颈。在高并发情况下，虽然设置了读/写分离，但是写请求压力都集中在单个 master 上，快拉不住了。”

除了使用大内存主机，我们还可以使用切片集群。俗话说，“众人拾柴火焰高”，一台机器无法保存所有数据，那就多台分担。

Redis 集群主要解决大数据量存储导致的响应变慢问题，同时便于横向拓展。Redis 数据的两种扩展方案是垂直扩展（scale up）和水平扩展（scale out）。

垂直扩展：升级单个Redis的硬件配置，例如增加内存容量、磁盘容量和使用更强大的CPU。  
◎ 水平扩展：横向增加Redis的实例个数，每个节点负责一部分数据。

例如，当需要24GB内存、150GB磁盘的服务器资源时，两种实现方案如图3-24所示。

![](images/91374e9e22ac4b49626c5d4415c055ad5455418f9ef55e67e5e6fe41844715f6.jpg)  
图3-24

在面向百万、千万级别规模的用户时，横向扩展Redis集群会是一个非常好的选择。

# Chaya: “这两种方案有什么优缺点呢？

垂直扩展部署简单，但是当数据量大时，使用RDB快照文件实现持久化会造成阻塞导致响应变慢。另外受限于硬件和成本，扩展内存的成本太大。  
$\odot$ 水平扩展较为便捷，不需要担心单个实例的硬件和成本的限制。但是，Redis 集群会涉及多个实例的分布式管理问题，需要解决如何将数据合理分配到不同实例的问题，同时要让客户端能正确访问到实例上的数据。

# 3.4.1 Redis 集群是什么

Redis 3.0 开始提供 Redis 集群, Redis 集群是一种分布式数据库方案, 通过分片 (sharding) 进行数据管理 (分治思想的一种实践), 并提供复制和自动故障切换功能。

Redis 集群并没有使用一致性哈希算法，而是将数据划分为 16384 个 slot，每个节点（Node）负责一部分 slot，slot 的信息存储在节点中。

Redis 集群是去中心化的架构，如图 3-25 所示，该集群由 3 个 Redis 的 master 组成（省略每个 master 对应的 slave），每个节点负责整个集群的一部分数据，每个节点负责的数据量可以不一样。

![](images/71471b6539d8839b9e3e1a70c6635b4d0c4289d5da27e09db4