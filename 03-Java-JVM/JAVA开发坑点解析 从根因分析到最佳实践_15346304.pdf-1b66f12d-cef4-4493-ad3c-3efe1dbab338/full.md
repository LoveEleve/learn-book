# Java开发坑点解析

# 从根因分析到最佳实践

朱晔著

# Java开发坑点解析

# 从根因分析到最佳实践

朱晔◎著

# 图书在版编目（CIP）数据

Java开发坑点解析：从根因分析到最佳实践/朱晔著.--北京：人民邮电出版社，2024.1ISBN978-7-115-63056-8

I. $①$ J…Ⅱ. $①$ 朱…ⅢI. $①$ JAVA语言一程序设计IV. $①$ TP312.8

中国国家版本馆CIP数据核字（2023）第203315号

# 内容提要

本书从整个Java后端研发的视角，通过大量的案例分析日常开发过程中可能遇到的150多个坑点及其解决方案，并讨论一些最佳实践。这些坑点涵盖编码（不仅涉及Java语法层面，还涉及多线程、连接池、数据库索引、事务、日志、Spring框架等层面）、系统设计、代码安全等方面。本书在剖析这些坑点时还会讲解排查思路和相关工具的使用，让读者不仅能了解常见的坑点，还能具备一定的问题分析能力，以便日后自行排查更多的坑点。

$\spadesuit$ 著 朱晔

责任编辑 杨海玲

责任印制 王郁 马振武

$\spadesuit$ 人民邮电出版社出版发行

北京市丰台区成寿寺路11号

邮编 100164

电子邮件

315@ptpress.com.cn

网址

https://www.ptpress.com.cn

北京市艺辉印刷有限公司印刷

$\spadesuit$ 开本： $775\times 1092$ 1/16

印张：30

2024年1月第1版

字数：778千字

2024年1月北京第1次印刷

定价：119.80元

读者服务热线：（010）81055256印装质量热线：（010）81055316

反盗版热线：（010）81055315

广告经营许可证：京东市监广登字20170147号

# 对本书的赞誉

程序员编写高质量、可维护、安全且高效的代码，通常需要大量的研究和经验，也需要避免许多技术陷阱。

当前，很多人对 Java 编程语言仍然存在一些认知误区，导致在使用方法和开发技术方面也存在很多误区。本书围绕 Java 软件开发过程中的 150 多个坑点展开分析，并给出解决方案，以期帮助开发人员写出高质量的代码，发挥 Java 在各业务系统中的优势。本书是一本不可多得的 Java 开发避坑宝典。

宋永柱 晖致医药CTO

本书从实际的业务场景出发，深入剖析了Java后端开发可能遇到的大量坑点及其解决方案。不仅如此，书中还分享了大量编码和设计的最佳实践，以及故障排查的思路和技巧。更重要的是，本书体现了朱晔作为一名技术专家对技术刨根问底的态度。相信书中的内容会让大家在感同身受的同时又能受益匪浅，无论你是Java开发者还是架构师，本书都值得一读。

张雪峰 前饿了么CTO

本书以独特的视角，从一个个的实际问题出发，揭示了架构师在日常工作中所遇到的诸多坑。这些坑往往容易被忽视，可能带来极高的业务风险，浪费开发人员宝贵的时间和精力。朱晔同学的这本书，不只阐述了问题，还深入探讨了其原理，更提供了实用、有效的解决方案和错误预防方法。本书可以让读者在解决实际问题的同时，提高编码技能，在工作中游刃有余。

本书汇集了作者近20年的实践经验，内容丰富，兼具理论和实战、深度和广度，既可以让资深的开发人员和架构师收获深厚的实践洞察、避免陷入相同的困境，也可以让Java初学者站在前人的肩膀上窥见业务开发中的各类问题，打下扎实的基础。

徐翎 前贝壳金服技术副总裁

Java 作为主流的编程语言，在业务系统研发领域有着广泛的应用。经过多年迭代，Java 形成了一套成熟而复杂的技术体系，要成为一名 Java 开发高手必须历经千锤百炼，面对挑战百折不挠，蹚过无数的坑才能一步步攀上高峰。成为高手并非一蹴而就，关键要积累实战经验，跟难题死磕到底。能力越强，填的坑越大。填完坑复盘总结乃至分享，点亮自身的技能点。

朱晔老师曾是我的同事，他在Java领域深耕多年，擅长应急排障、解决棘手问题并乐在其中，在公司和业界备受赞誉。我曾留意到他设计的面试问题清单，其中涵盖了众多分类细致的问题，展现了全面的技术视野与深入的理解力。如今他毫无保留地将宝贵经验编写成“避坑指南”，非常难得。

本书凝聚了朱晔老师从业多年的实践所得，是一本帮助大家在避坑、挖坑、填坑中成长的实用指南。书中讲解了150多个经典案例，覆盖了许多典型场景，从工作中最常见的问题出发，以点带面，将理论知识与实操技巧结合得恰到好处。相信每一位Java开发人员都能从中受益，

身在坑中自得其乐，追求卓越，不断突破自我，成为更优秀的技术专家。

史海峰 前贝壳金服小微企业生态CTO，公众号“IT民工闲话”主理人

本书从实践出发，结合作者多年参与众多高并发、大用户量项目实践中积累的经验，深刻且形象地剖析了Java后端开发中的种种“陷阱”和容易犯错的地方，非常适合有一定开发经验的程序员和构架师阅读。同时，本书并非单纯地从代码角度指出常见的问题或错误，而是贯彻了“授人以鱼，不如授之以渔”的思想，从整体架构的角度剖析错误的深层原因，而这正是大部分同类书籍无法带给读者的。

赖效纲 空中网集团COO，前盛大网络技术保障中心总监

这是一部极具实用价值的著作。作者凭借丰富的开发经验和对编程的深刻洞察，详尽总结了Java开发过程中的150多个常见问题，涉及基础库、业务代码、框架和架构设计等方面，并结合实际的业务场景提供了代码示例和分析及解决问题的思路。这本书对于设计并编写稳健和安全的系统有非常大的帮助。衷心推荐给广大读者！

胡志明 携程租车CTO

本书系统梳理了100多个案例，覆盖了150多个常见坑点。针对这些坑点，书中给出了错误实现和正确实现，并深入剖析了背后的原理。通过这种方式，读者不仅可以避免踩坑，更能深入理解问题的本质，提升技术能力。

朱晔的经验丰富且独到，他深知业务开发中的很多问题只有在特定条件下才会暴露，有些问题一旦暴露就会导致灾难性后果。这本书可以带你走近这些潜在的风险，让你意识到业务代码中隐藏的坑，避免陷入各种棘手的局面。

孔凡勇 前阿里云中小企业应用事业部技术负责人

本书包含 Java 程序员必须学习和掌握的全面的、实战的且重要的知识，推荐给所有“Javaer”。

程军 前饿了么技术总监

最早是在极客时间关注了朱晔的专栏“Java业务开发常见错误100例”，有坑点、有原理，又有代码，实战导向非常强。很高兴看到《Java开发坑点解析：从根因分析到最佳实践》最终编纂成书。相比专栏，本书内容更加翔实，编排也更为体系化。本书深入浅出，由点及面，有深度又有广度，既可以作为新手系统学习的教材，又能帮助老手查漏补缺，这是一本很好的、能帮助开发人员在真正遇到问题时快速定位问题的工具书。在此真诚地推荐给每一位读者。

施俊 AWS资深架构师

本书以实际案例来深入剖析 Java 编程、系统设计、性能优化等方面的意见问题和解决思路，是一本内容特别丰富的 Java 开发陷阱排查指南，非常适合中高级 Java 开发工程师进阶学习。无论你是想提升技术实力还是要解决实际开发中的难题，这都是一本非读不可的书。相信这些实

用的经验和见解一定会帮到你。

肖聘 前贝壳找房金融事业部技术总监

关于技术的书比比皆是，但是极少会如此细致地分析技术细节，从文字到案例无不体现了作者多年的积累。如果你想拓宽技术视野，学习更多务实的经验，此书可以说是不二之选。

姜伟 领健信息运维与基础架构负责人

本书不仅涵盖了Java开发的坑点，还覆盖了整个后端开发的很多知识点。更可贵的是，本书通过理论与实践相结合的方式真正做到了“授人以渔”，可以帮助开发人员深化避坑思维、提升解决问题的能力。在贝壳公司的开发实践中，我和团队遇到过日志打印大对象从而导致服务OOM的问题，从定位问题到解决问题花费了很长时间。本书提供的问题排查思路精巧，让我有耳目一新的感觉。推荐给每个要保障系统稳定性的技术团队，以及希望提升个人竞争力的开发者。

孔凤玉（Luna）贝壳找房高级经理

# 前言

看到本书的标题有些读者不禁要问了：“我写了好几年的Java代码了，已经算是一个老手了，使用Java进行开发会有很多坑吗？”请带着这个问题继续往下阅读吧。

据我观察，很多开发人员其实是没能意识到这些坑的存在，其中的原因包括如下几种。

- 意识不到坑的存在。例如，在高并发或用户量很大的情况下，所谓的服务器不稳定（出现内存溢出、高CPU占用、死锁、访问超时、资源不足）其实可能是代码问题导致的，但我们只是在运维层面通过改配置、重启、扩容等手段临时解决了，没有反推到开发层面去寻找根本原因。  
- 有些bug或问题只会在特定情况下暴露。例如缓存击穿、在多线程环境使用非线程安全的类，只有在多线程或高并发的情况才会暴露问题。  
- 有些性能问题不会导致明显的 bug，只会让程序运行缓慢、内存使用增加，但会在量变到质变的瞬间爆发。

正是因为没有意识到这些坑和问题而采用了错误的处理方式，问题一旦爆发，处理起来就会非常棘手，这是非常可怕的。下面这些场景你有没有感觉似曾相识呢？

- 有一个订单量很大的项目，每天总有上千份订单的状态或流程有问题，需要花费大量的时间来核对数据，修复订单状态。开发人员因为每天牵扯太多精力在排查问题上，根本没时间开发新需求。技术负责人为此头痛不已，无奈之下招了专门的技术支持人员。痛定思痛决定开启明细日志彻查这个问题，结果发现是自调用方法导致事务没生效。  
- 有个朋友告诉我，他们的金融项目计算利息的代码中，使用了float类型而不是BigDecimal类来保存和计算金额，导致给用户结算的每一笔利息都多了几分钱。好在日终对账及时发现了问题。试想一下，结算的有上千个用户，每个用户有上千笔小订单，如果等月终对账的时候再发现，可能已经损失了几百万元。  
- 某项目使用 RabbitMQ 做异步处理，业务处理失败的死信消息会循环不断地进入消息队列（message queue，MQ）并堆积。问题爆发之前，可能只影响了消息处理的时效性。但等 MQ 彻底瘫痪时，面对 MQ 中堆积的、混杂了死信和正常消息的几百万条数据，除了清空数据又能怎么办。但清空 MQ，就意味着要花费几小时甚至几十小时来补正常的业务数据，对业务影响时间很长。  
- 某项目出现了安全漏洞，数据库被黑客进行了拖库，导致大量的用户信息外泄，更可怕的是对用户的密码只进行了简单的MD5加密，大量简单的密码被“破解”导致几百万可用的用户账号在互联网上被贩卖，造成了不可估计的影响。  
- 某低并发的项目经常出现偶发的接口超时，但是这些接口都是简单的数据库查询。DBA反馈数据库没有慢查询，调整连接池大小也不能解决问题。开发人员百思不得其解，前后花了几周时间问题都没解决。最后架构师通过完善监控、分析代码，发现这件事并不简单，其根本原因是长事务导致连接被占用的时间过长，同时连接池配置得的确太小，开发人员期望适当调大连接池解决问题却因为Spring版本升级导致配置未生效。更因为

缺乏监控，参数修改没生效也没及时发现。种种原因加在一起导致了这个非常复杂、难以解决的bug。

像这些由一个小坑引发的重大事故，不仅会给公司造成损失，还会让员工因为陷入自责而影响工作状态，降低编码的自信心。我遇到过一位比较负责的核心开发人员，因为一个bug给公司带来数万元的经济损失，最后心理上承受不住提出了辞职。其实，很多时候不是我们不想从根本上解决问题，只是不知道问题到底出在哪里。要避开这些坑、找到这些不定时炸弹，第一步就是得知道它们是什么、在哪里、为什么会出现。而讲清楚这些坑点和相关的最佳实践，正是本书的主要内容。

# 本书的内容

本书从 Java 后端开发的视角，围绕 30 多个知识点引出 150 多个相关常见的坑点，涉及如下内容。

- Java本身相关的：字符串和数值包装类型、浮点数和科学计算、集合、空指针问题、异常处理、日志记录、I/O相关、日期时间、面向对象编程（Object-Oriented Programming, OOP）、反射、注解、泛型。  
- 业务代码编写相关的：线程安全、锁、线程池、连接池、HTTP请求的超时/重试/并发限制问题、序列化。   
- 框架使用相关的：Spring 声明式事务、Spring 的 IoC 和 AOP、Spring 的配置优先级。  
- 中间件和存储相关的：数据库索引、缓存、MQ、NoSQL。  
- 故障排查相关的：内存溢出（out of memory, OOM）、Kubernetes、生产就绪需要做的工作、指标监控。  
- 架构设计相关的：设计模式、接口设计、异步流程。  
- 安全相关的：XSS、SQL注入、防刷、防重、限量、加密、HTTPS。

在介绍每一个坑点的时候，我会力求按照“知识介绍 $\rightarrow$ 还原业务场景 $\rightarrow$ 错误实现 $\rightarrow$ 正确实现 $\rightarrow$ 原理分析 $\rightarrow$ 小总结”的形式来讲解，同时引出10多个工具的使用和10多条最佳实践（比如线程池的使用、连接池的使用、BigInt的使用、数据库索引、异常处理、日志记录、接口设计、指标监控设计、加密算法的使用等）。

# 本书的特点

本书有如下几个特点。

- 代入感强。我会通过一个个具体的案例来介绍坑点，而不是直接给出结论。例如，我们以“在生产环境中可能会遇到这样一个诡异的问题：有时获取到的用户信息是别人的”这个案例作为问题背景，进而引出 Tomcat 线程重用的坑点。有场景和案例，更容易让读者记住坑点及其影响，更能产生共鸣、激发思考。对于大部分案例，我都给出了执行结果的示意图，让读者能够充分感受到坑点的“威力”，并体验到问题解决后的“成就感”。  
- 实战性强。本书的大多数案例来自真实项目，配合案例演示的可执行的代码示例中不仅有错误实现（踩坑），还有修正后的正确实现（避坑）。本书的代码示例基本覆盖了各种中间件的使用，其代码量超过12000行，堪比一个小型Java项目。这套代码

示例可以作为很多技术问题测试的起点，读者可以修改其中的某些参数和场景以验证更多的知识点。同时，我在介绍坑点解决方法时给出的一些最佳实践（例如通过HandlerMethodArgumentResolver做参数的组装来实现自动注入用户信息），经过封装甚至可以形成一个小型的框架。

- 通俗易懂。针对案例涉及的复杂场景或者难解释的源码，我会配合示意图来给读者解释清楚。例如，在介绍MySQL索引和Spring相关坑点时，就有大量的示意图。我在讲解坑点时还会尽可能简单地讲述其中的知识点，并给出一些资料供读者进一步阅读。

- 授人以渔。我会尽可能地把分析问题的过程完整地呈现出来，而不是直接给出为什么。在本书中，我会穿插介绍如何使用诸如jvisualvm、jstack、jstat、jmap、jclasslib、jconsole、Wireshark、Arthas、MAT等工具来帮助我们定位坑点的根本原因，探究问题的本质。此外，书中还会介绍有关编码、设计与问题排查的一些方法论和比较好的实践。这样读者以后遇到问题时也能有解决问题的思路。

- 有广度。本书中的 Java 坑点不仅仅是围绕 Java 语法本身的坑点，而是覆盖了整个后端知识体系内使用 Java 语言进行编程相关的坑点，涉及架构、设计、安全、高并发、调优、问题排查、中间件和安全等方面。这就好比我们在谈论英语中常见的坑点时，并不会局限在谈论哪些单词容易拼错、哪些地方会有语法错误，更多的是站在整个语言层次的高度来谈论坑点，其内容会包括文化和表达方面的坑点。因此，日常开发语言不是 Java 的后端开发人员，也能从本书中受益。

- 有深度。我在分析坑点原因时往往会给出 JDK 或 Spring 等框架中的一些源码来证实问题。比如在分析声明式事务相关坑点时，我会进一步分析 Spring 的 TransactionAspectSupport 和 DefaultTransactionAttribute 类的实现来了解其在什么时候会回滚异常。如果说通过做实验看到坑点只是发现了这个现象的话，那么定位到源码中的相关实现才是看到了问题的本质。除了做一些源码分析，本书还会介绍一些调试的技巧来帮助读者克服恐惧以更快地找到相关源码实现。

# 阅读本书的方式

编程是一门实践科学，只看不练、不思考，效果通常不会太好。基于本书的内容和特点，我建议读者按照下面的方式深入学习。

- 对于每一个坑点，结合自己的项目经历回想一下是否遇到过类似的问题，当时是怎么发现和解决的。  
- 对于每一个坑点，实际运行调试一下源码，使用文中提到的工具和方法重现问题，眼见为实。再思考一下，除文内的解决方案和思路之外，是否还有其他修正方式。对于坑点根本原因中涉及的 JDK 或框架源码分析，读者可以找到相关类再系统阅读一下源码。  
- 记得思考本书“思考与讨论”中的问题，并进行相应的实践。这些问题，有的是对文章内容的补充，有的是额外容易踩的坑，读者可以在阅读答案之前自己先思考一下。  
- 此外，虽然本书说的是一些看似离散的坑点，但书中解决问题使用的思路是一个层层递进过程，内容也会出现前后的关联，更推荐读者按顺序阅读，这样可以获得最好的效果。  
- 正如前文提到的“某低并发的项目经常出现偶发的接口超时”这个例子，许多时候这些

坑点是可以组合的，组合后会形成更难排查的坑点。如果读者对每一个坑点有足够深的印象，那么在遇到问题时就能有更多的联想，因此推荐读者反复阅读本书。

本书中的配套源代码可以在仓库 https://github.com/JosephZhu1983/java-common-mistakes 中找到。

# 阅读本书后的收获

本书梳理的150多个坑点是我认为开发中可能遇到的比较严重、比较典型的坑点，但是后端开发涉及的点非常多，这些也只是冰山一角。理解了这些坑点，读者就能具备一定的问题分析和排查能力了。更进一步地，我希望读者能看到这些离散的知识点之间的关联，并将其连成线，读者做到这一点也就具备了阅读源码进而发现问题本质的能力。我更希望读者将本书作为一个起点，在日常工作中多积累多记录多思考，将线织成网进而把自己的技术思维架构提升一个层次。当你成长为一名带队经理或者架构师时，就能够更系统且全面地考虑整个系统的架构设计，能够在审核别人代码时发现更多可能存在的问题，能够在一线救火时更快地解决问题。

# 特别感谢

我要感谢家人们的支持和理解，让我能静心投入无数个周末的时间来完成本书的写作。我也要感谢王少泽，作为本书的技术审校，他审核了本书所有的文字和代码，也提供了一些坑点的素材。我还要感谢过去近20年我工作上的上级和导师赖效纲、姜岩、吴江华、张剑伟、张雪峰、高伟、丁其骏、徐翎、宋玉柱等，感谢他们在技术和为人处世上给予我的指引和帮助。

如果你在工作中也遇到一些坑点想与我分享，或是对本书有什么建议，欢迎通过邮件与我联系，我的邮箱是yzhu@live.com。

# 资源与支持

本书由异步社区出品，社区（https://www.epubit.com/）为您提供相关资源和后续服务。

# 配套资源

本书提供配套源代码。您可以扫描下方二维码，添加异步助手为好友，并发送“231303”获取配套源代码。

![](images/86465a7df4157f4001594a385e4ac8a2449bd6093f04a5e0c1bcccf77aef30de.jpg)

# 提交勘误

作者和编辑尽最大努力来确保书中内容的准确性，但难免会存在疏漏。欢迎您将发现的问题反馈给我们，帮助我们提升图书的质量。

当您发现错误时，请登录异步社区，按书名搜索，进入本书页面，单击“发表勘误”，输入勘误信息，单击“提交勘误”按钮即可（见下图）。本书的作者和编辑会对您提交的勘误进行审核，确认并接受后，您将获赠异步社区的100积分。积分可用于在异步社区兑换优惠券、样书或奖品。

![](images/80edd5a5d1f4253fe82e4924a75be2a6ab57b3e682d0ea2490404dbf7660d3e8.jpg)

# 与我们联系

我们的联系邮箱是contact@epubit.com.cn。

如果您对本书有任何疑问或建议，请您发邮件给我们，并请在邮件标题中注明本书书名，以便我们更高效地做出反馈。

如果您有兴趣出版图书、录制教学视频，或者参与图书技术审校等工作，可以发邮件给本书的责任编辑（yanghailing@ptpress.com.cn）。

如果您来自学校、培训机构或企业，想批量购买本书或异步社区出版的其他图书，也可以发邮件给我们。

如果您在网上发现有针对异步社区出品图书的各种形式的盗版行为，包括对图书全部或部分内容的非授权传播，请您将怀疑有侵权行为的链接通过邮件发给我们。您的这一举动是对作者权益的保护，也是我们持续为您提供有价值的内容的动力之源。

# 关于异步社区和异步图书

“异步社区”（www.epubit.com）是由人民邮电出版社创办的IT专业图书社区。异步社区于2015年8月上线运营，致力于优质学习内容的出版和分享，为读者提供优质学习内容，为作译者提供优质出版服务，实现作者与读者在线交流互动，实现传统出版与数字出版的融合发展。

“异步图书”是由异步社区编辑团队策划出版的精品IT专业图书的品牌，依托于人民邮电出版社30年余年的计算机图书出版积累和专业编辑团队，相关图书在封面上印有异步图书的LOGO。异步图书的出版领域包括软件开发、大数据、AI、测试、前端、网络技术等。

# 目录

# 第1章 Java8中常用的重要知识点

1.1 在项目中使用Lambda表达式和流操作  
1.2 Lambda 表达式 2  
1.3 使用Java8简化代码 4

1.3.1 使用流操作简化集合操作 4  
1.3.2 使用可空类型简化判空逻辑  
1.3.3 使用Java8的一些新类、新方法获得函数式编程体验

1.4 并行流 8   
1.5 流操作详解 11

1.5.1 创建流  
1.5.2 filter 14   
1.5.3 map 14   
1.5.4 flatMap 14   
1.5.5 sorted 15   
1.5.6 distinct 15   
1.5.7 skip和limit· 15   
1.5.8 collect 16   
1.5.9 groupingBy 17   
1.5.10 partitioningBy 19

1.6 小结 19  
1.7 思考与讨论 19

# 第2章 代码篇 ………………………………………… 23

2.1 使用了并发工具类库，并不等于就没有线程安全问题了 23

2.1.1 没有意识到线程重用导致用户信息错乱的 bug 23  
2.1.2 使用了线程安全的并发工具，并不代表解决了所有线程安全问题 25  
2.1.3 没有充分了解并发工具的特性，从而无法发挥其威力 … 28  
2.1.4 没有认清并发工具的使用场景，因而导致性能问题 30  
2.1.5 小结 32  
2.1.6 思考与讨论 32

2.2 代码加锁：不要让锁成为烦心事 33

2.2.1 加锁前要清楚锁和被保护的对象是不是一个层面的 35  
2.2.2 加锁要考虑锁的粒度和场景问题 36  
2.2.3 多把锁要小心死锁问题 37  
2.2.4 小结 40  
2.2.5 思考与讨论 40

2.3 线程池：业务代码中最常用也最容易犯错的组件 41

2.3.1 线程池的声明需要手动进行 41  
2.3.2 线程池线程管理策略详解 43  
2.3.3务必确认清楚线程池本身是不是复用的 47  
2.3.4 需要仔细斟酌线程池的混用策略 48  
2.3.5 小结 51  
2.3.6 思考与讨论 51  
2.3.7 扩展阅读 52

2.4 连接池：别让连接池帮了倒忙 54

2.4.1 注意鉴别客户端SDK是否基于连接池 55  
2.4.2 使用连接池务必确保复用 60  
2.4.3 连接池的配置不是一成不变的 64  
2.4.4 小结 67  
2.4.5 思考与讨论 67

2.5 HTTP调用：你考虑超时、重试、并发了吗 68

2.5.1 配置连接超时和读取超时参数的学问 69  
2.5.2 Feign和Ribbon配合使用，你知道怎么配置超时吗  
2.5.3 你知道Ribbon会自动重试请求吗  
2.5.4 并发限制了爬虫的抓取能力 75  
2.5.5 小结 77  
2.5.6 思考与讨论 78  
2.5.7 扩展阅读· 78

2.6 $20\%$ 的业务代码的Spring声明式事务可能都没处理正确 80

2.6.1 小心Spring的事务可能没有生效 80  
2.6.2 84   
2.6.3 请确认事务传播配置是否符合自己的业务逻辑· 86  
2.6.4 小结 89  
2.6.5 思考与讨论 90  
2.6.6 扩展阅读 93

2.7 数据库索引：索引不是万能药 94

2.7.1 InnoDB是如何存储数据的 95  
2.7.2 聚簇索引和二级索引 96   
2.7.3 考虑额外创建二级索引的代价 97  
2.7.4 不是所有针对索引列的查询都能用上索引 99  
2.7.5 数据库基于成本决定是否走索引 101  
2.7.6 小结 104  
2.7.7 思考与讨论 104

2.8 判等问题：程序里如何确定你就是你 … 105

2.8.1 注意 equals 和 $= =$ 的区别 … 106  
2.8.2 实现一个equals没有这么简单 110  
2.8.3 hashCode和equals要配对实现 112   
2.8.4 注意 compareTo 和 equals 的逻辑一致性 … 114

2.8.5 小心Lombok生成代码的坑 115  
2.8.6 小结 117  
2.8.7 思考与讨论 117  
2.8.8 扩展阅读 118

# 2.9 数值计算：注意精度、舍入和溢出问题 119

2.9.1 “危险”的 Double 120  
2.9.2 考虑浮点数舍入和格式化的方式 121  
2.9.3 用equals 做判等，就一定是对的吗  
2.9.4 小心数值溢出问题 123  
2.9.5 小结 125  
2.9.6 思考与讨论 125  
2.9.7 扩展阅读 126

# 2.10 集合类：坑满地的List列表操作 127

2.10.1 使用Arrays.asList把数据转换为List的3个坑 127  
2.10.2 使用List.subList进行切片操作居然会导致OOM 129  
2.10.3 一定要让合适的数据结构做合适的事情 ………………………………………… 132  
2.10.4 小结 136  
2.10.5 思考与讨论 137

# 2.11 空值处理：分不清楚的 null 和恼人的空指针 138

2.11.1 修复和定位恼人的空指针问题 ………………………………………… 138  
2.11.2 POJO中属性的null到底代表了什么 142  
2.11.3 小心MySQL中有关NULL的3个坑 146  
2.11.4 小结 147  
2.11.5 思考与讨论 147

# 2.12 异常处理：别让自己在出问题的时候变为盲人 149

2.12.1 捕获和处理异常容易犯的错 149  
2.12.2 小心 finally 中的异常 … 153  
2.12.3 需要注意JVM针对异常性能优化导致栈信息丢失的坑 155  
2.12.4 千万别把异常定义为静态变量 157  
2.12.5 提交线程池的任务出了异常会怎样 158  
2.12.6 小结 161  
2.12.7 思考与讨论 162  
2.12.8 扩展阅读 163

# 2.13 日志：日志记录真没你想象得那么简单 164

2.13.1 为什么我的日志会重复记录 … 165  
2.13.2 使用异步日志改善性能的坑 169  
2.13.3 使用日志占位符就不需要进行日志级别判断了吗 175  
2.13.4 小结 176  
2.13.5 思考与讨论 176  
2.13.6 扩展阅读 178

# 2.14 文件I/O：实现高效正确的文件读写并非易事 180

2.14.1 文件读写需要确保字符编码一致 180

2.14.2 使用Files类静态方法进行文件操作注意释放文件句柄 182  
2.14.3 注意读写文件要考虑设置缓冲区 184  
2.14.4 小结 187  
2.14.5 思考与讨论 187  
2.14.6 扩展阅读 188

# 2.15 序列化：一来一回，你还是原来的你吗

2.15.1 序列化和反序列化需要确保算法一致 191  
2.15.2 MyBatisPlus读取泛型List<T>JSON字段的坑 195   
2.15.3 注意 Jackson JSON 反序列化对额外字段的处理 … 198  
2.15.4 反序列化时要小心类的构造方法 ..... 200  
2.15.5枚举作为API接口参数或返回值的两个大坑 201  
2.15.6 小结 207  
2.15.7 思考与讨论 207

# 2.16 用好Java8的日期时间类，少踩一些“老三样”的坑 ………………………………………… 208

2.16.1 初始化日期时间 209  
2.16.2 “恼人”的时区问题 209  
2.16.3 日期时间格式化和解析 ………………………………………… 212  
2.16.4 日期时间的计算 ………………………………………… 215  
2.16.5 小结 217  
2.16.6 思考与讨论 218  
2.16.7 扩展阅读 219

# 2.17 别以为“自动挡”就不可能出现OOM 220

2.17.1 太多份相同的对象导致OOM 220  
表 2.17.2 使用 WeakHashMap 不等于不会 OOM ..... 223  
2.17.3 Tomcat参数配置不合理导致OOM 227   
2.17.4 小结 228  
$\S 2.17.5$ 思考与讨论 … 229  
2.17.6 扩展阅读 230

# 2.18 当反射、注解和泛型遇到OOP时，会有哪些坑· 231

$\S 2.18.1$ 反射调用方法不是以传参决定重载 ………………………………………… 231  
$\S 2.18.2$ 泛型经过类型擦除多出桥接方法的坑 … 232  
2.18.3 注解可以继承吗 237  
2.18.4 小结 239  
$\S 2.18.5$ 思考与讨论 … 239  
$\S 2.18.6$ 扩展阅读 … 243

# 2.19 Spring 框架：IoC 和 AOP 是扩展的核心 ..... 243

2.19.1 单例的Bean如何注入Prototype的Bean 244  
2.19.2 监控切面因为顺序问题导致Spring事务失效 247  
2.19.3 小结 255  
2.19.4 思考与讨论 255  
2.19.5知识扩展：同样注意枚举是单例的问题 256

2.20 Spring 框架：帮我们做了很多工作也带来了复杂度 258

2.20.1 Feign AOP 切不到的诡异案例 … 258  
2.20.2 Spring程序配置的优先级问题 264  
2.20.3 小结 273  
2.20.4 思考与讨论 273  
2.20.5 扩展阅读 275

# 第3章 系统设计 281

3.1 代码重复：搞定代码重复的3个绝招· 281

3.1.1 利用“工厂模式 $+$ 模板方法模式”，消除if...else...和重复代码 281  
3.1.2 利用“注解+反射”消除重复代码 287  
3.1.3 利用属性拷贝工具消除重复代码 291  
3.1.4 小结 293  
3.1.5 思考与讨论 293

3.2 接口设计：系统间对话的语言，一定要统一 294

3.2.1 接口的响应要明确表示接口的处理结果 ..... 294  
3.2.2 要考虑接口变迁的版本控制策略 300  
3.2.3 接口处理方式要明确同步还是异步 302  
3.2.4 小结 305  
3.2.5 思考与讨论 305  
3.2.6 扩展阅读 307

3.3 缓存设计：缓存可以锦上添花也可以落井下石 307

3.3.1 不要把Redis当作数据库 308  
3.3.2 注意缓存雪崩问题 309  
3.3.3 注意缓存击穿问题 312  
3.3.4 注意缓存穿透问题 314  
3.3.5 注意缓存数据同步策略 316  
3.3.6 小结 317  
3.3.7 思考与讨论 317  
3.3.8 扩展阅读 318

3.4 业务代码写完，就意味着生产就绪了吗… 320

3.4.1 准备工作：配置Spring Boot Actuator 321  
3.4.2 健康监测需要触达关键组件 322  
3.4.3 对外暴露应用内部重要组件的状态 327  
3.4.4指标是快速定位问题的“金钥匙” 330  
3.4.5 小结 339  
3.4.6 思考与讨论 339

3.5 异步处理好用，但非常容易用错 342

3.5.1 异步处理需要消息补偿闭环 342  
3.5.2 注意消息模式是广播还是工作队列 346  
3.5.3 别让死信堵塞了消息队列 351  
3.5.4 小结 355

3.5.5 思考与讨论 356

3.6 数据存储：NoSQL与RDBMS如何取长补短、相辅相成 358

3.6.1 取长补短之Redis vs MySQL 358   
3.6.2 取长补短之 InfluxDB vs MySQL 361  
3.6.3 取长补短之Elasticsearch vs MySQL 364   
3.6.4 结合 NoSQL 和 MySQL 应对高并发的复合数据库架构 … 369  
3.6.5 小结 371  
3.6.6 思考与讨论 371

# 第4章 代码安全问题 … 373

4.1 数据源头：任何客户端的东西都不可信任 373

4.1.1 客户端的计算不可信 373  
4.1.2 客户端提交的参数需要校验 375  
4.1.3 不能信任请求头里的任何内容 … 377  
4.1.4 用户标识不能从客户端获取 378  
4.1.5 小结 380  
4.1.6 思考与讨论 380

4.2 安全兜底：涉及钱时，必须考虑防刷、限量和防重 381

4.2.1 开放平台资源的使用需要考虑防刷 381  
4.2.2 虚拟资产并不能凭空产生无限使用 382  
4.2.3 钱的进出一定要和订单挂钩并且实现幂等 384   
4.2.4 小结 386  
4.2.5 思考与讨论 386  
4.2.6 扩展阅读 386

4.3 数据和代码：数据就是数据，代码就是代码 387

4.3.1 SQL注入能干的事情比你想象得更多 388  
4.3.2 小心动态执行代码时代码注入漏洞 393  
4.3.3 XSS 必须全方位严防死堵 396   
4.3.4 小结 403  
4.3.5 思考与讨论 403  
4.3.6 扩展阅读 404

4.4.4 如何正确地保存和传输敏感数据 405

4.4.1 如何保存用户密码 406  
4.4.2 如何保存姓名和身份证号码 409  
4.4.3 用一张图说清楚 HTTPS   
4.4.4 小结 418  
4.4.5 思考与讨论 419

# 第5章 Java程序故障排查 420

5.1 定位Java应用问题的排错套路 420

5.1.1 生产问题的排查很大程度依赖监控 420  
5.1.2 分析定位问题的套路 421

5.1.3 分析和定位问题需要注意的9个点 422  
5.1.4 小结 424  
5.1.5 思考与讨论 424

# 5.2 分析定位Java问题，一定要用好这些工具 425

5.2.1 使用JDK自带工具查看JVM情况 425  
5.2.2 使用 Wireshark 分析 SQL 批量插入慢的问题 433  
5.2.3 使用MAT分析OOM问题 438  
5.2.4 使用Arthas分析高CPU问题 444  
5.2.5 小结 448  
5.2.6 思考与讨论 449

# 5.3 Java程序从虚拟机迁移到Kubernetes的一些坑 452

5.3.1 Pod IP 不固定带来的坑 452  
5.3.2 程序因为OOM被杀进程的坑 453  
5.3.3内存和CPU资源配置不适配容器的坑 454   
5.3.4 Pod 重启以及重启后没有现场的坑 455  
5.3.5 小结 455  
5.3.6 思考与讨论 456

# 后记：写代码时，如何才能尽量避免踩坑· 457

# Java 8 中常用的重要知识点

目前，Java 8仍然是生产环境中使用非常广泛的 JDK 版本。Java 8 相比 Java 7 在代码可读性、简化代码方面增加了很多功能，如 Lambda 表达式、流操作（Stream API）、并行流（Parallel Stream）、可空类型（Optional<T> 类）、新日期时间类型等。本书的所有案例都充分使用了 Java 8 的各种特性来简化代码。因此，本章先介绍 Lambda 表达式、流操作、并行流和可空类型的基础知识，Java 8 的日期时间类型会在 2.16 节中讲解。

# 1.1 在项目中使用 Lambda 表达式和流操作

在业务代码开发中，使用Lambda表达式和流操作的地方有很多，如果你期望为老代码快速进行流化的优化，可以参考下面3个建议。

（1）从列表的操作开始，尝试使用流操作的filter和map方法实现遍历列表来筛选数据和转换数据（投影）的操作。这是流操作中非常基本的两个API。  
（2）使用高级的IDE写代码，以此找到可以利用Java8语言特性简化代码的地方。例如，对于IDEA，可以把使用Lambda表达式替换匿名类型的检测规则设置为Error级别严重程度，如图1-1所示。

![](images/d21e273c3fb4657ff065403e35b554c24ecf9dcdd1c38bb9e0f623edad50fc71.jpg)  
图1-1通过设置IDEA的Preferences|Editor|Inspections来设置Java新特性的探查

这样设置后，在运行IDEA的Inspect Code功能时，可以在Error级别的错误中看到这个问题，从而帮助我们养成使用Lambda表达式的习惯，如图1-2所示。

![](images/85c0dc2c043b28c105c70f41daf9e04e6ecf1bf71ce3ecaa950d1860b20c46c2.jpg)  
图1-2IDEA中InspectCode功能的扫描结果

（3）如果不知道如何把匿名类转换为Lambda表达式，可以借助IDE来重构，如图1-3所示。

![](images/3d61994d1103de8b2123fd1b64670b26d64c60dae85eb445bf12d0aa431fff22.jpg)  
图1-3IDEA给我们的优化提示，直接单击即可重构

反过来，如果你阅读本书案例中的Lambda表达式和流操作API比较吃力，同样可以借助IDE把Java8的写法转换为使用循环的写法（如图1-4所示），或者把Lambda表达式转换为匿名类（如图1-5所示）。

![](images/8e47a19d9bc5bc2be26a98d92bdcc60f88887ed715afee6e5ae814fee0ab8bcf.jpg)  
图1-4 借助IDEA反向把流操作API转换为循环

![](images/38ab769fb5d1ec315680d991bcf9e2b0197c66090819f9f604b19007e72b2a96.jpg)  
图1-5 借助IDEA反向把Lambda表达式转换为匿名类

# 1.2 Lambda 表达式

Lambda 表达式的初衷是进一步简化匿名类的语法（不过实现上，Lambda 表达式并不是匿名类的语法糖），使 Java 走向函数式编程。虽然匿名类没有类名，但还是要给出方法定义。这里有个例子，分别使用匿名类和 Lambda 表达式创建一个线程，打印字符串：

# //匿名类

```java
new Thread(new Runnable(){
    @Override
    public void run(){
        System.out.println("hello1");
    }
});  
} .start();  
//Lambda表达式  
new Thread() -> System.out.println("hello2").start();
```

那么，Lambda 表达式如何匹配 Java 的类型系统呢？答案就是，函数式接口。函数式接口是一种只有单一抽象方法的接口，使用 @FunctionalInterface 来描述，可以隐式地转换成

Lambda 表达式。使用 Lambda 表达式来实现函数式接口，不需要提供类名和方法定义，通过一行代码提供函数式接口的实例，就可以让函数成为程序中的“一等公民”，可以像普通数据一样作为参数传递，而不是作为一个固定的类中的固定方法。那么，函数式接口到底是什么样的？java.util.function 包中定义了各种函数式接口。例如，用于提供数据的 Supplier 接口，就只有一个抽象方法 get，没有任何入参、有一个返回值：

```java
@FunctionalInterface   
public interface Supplier<T> { /\*\* \* Gets a result. \* \* @return a result \*/ T get(); 
```

我们可以使用Lambda表达式或方法引用，来得到Supplier接口的实例：

//使用Lambda表达式提供Supplier接口实现，返回OK字符串Supplier<String> stringSupplier $=$ （）->"OK";//使用方法引用提供Supplier接口实现，返回空字符串Supplier<String> supplier $=$ String::new;

这样是不是很方便？下面再举几个使用Lambda表达式或方法引用来构建函数的例子：

```java
//Predicate接口的功能是输入一个参数，返回布尔值  
//我们通过and方法组合两个Predicate条件，判断值是否大于0并且是偶数 Predicate<Integer> positiveNumber = i -> i > 0; Predicate<Integer> evenNumber = i -> i % 2 == 0; assertTrue(+ve) Number.and(+evenNumber).test(2));
```

```txt
//Consumer接口的功能是消费一个数据。我们通过andThen方法组合调用两个Consumer，输出两行abcdefgConsumer<String> println = System.out::println;println.andThen println).accept("abcdefg");
```

//Function接口的功能是输入一个数据，计算后输出一个数据  
//我们先把字符串转换为大写，然后通过andThen组合另一个Function实现字符串拼接  
Function<String,String> upperCase $=$ String::toUpperCase;  
Function<String,String>duplicate $\equiv$ s->s≌cat(s);  
assertThat(upperCase.andThen(duplicate).apply("test"),is("TESTTEST"));

```javascript
//Supplier是提供一个数据的接口。我们实现获取一个随机数的方法 Supplier<Integer> random = () -> ThreadLocalRandom.current().NEXT(); System.out.println(random.get());
```

```txt
//BinaryOperator是输入两个同类型参数，输出一个同类型参数的接口  
//我们通过方法引用获得一个整数加法操作，通过Lambda表达式定义一个减法操作，然后依次调用  
BinaryOperator<Integer> add = Integer::sum;  
BinaryOperator<Integer> subtraction = (a, b) -> a - b;  
assertThat(subtraction.apply(add.apply(1, 2), 3), is(0));
```

Predicate、Function 等函数式接口，还使用 default 关键字实现了几个默认方法。这样一来，它们既可以满足函数式接口只有一个抽象方法的要求，又能为接口提供额外的功能：

@FunctionalInterface   
public interface Function<T, R> { R apply(T t); default $<  \mathrm{V}>$ Function<V, R> compose(Function<? super V, ? extends T> before) {

ObjectsrequireNonNull(before); return(Vv) $\rightharpoondown$ apply(before.apply(v));   
}   
default $<  V>$ Function<T,V> andThen(Function<? super R,? extends V> after){ ObjectsrequireNonNull(after); return(Tt） $\rightharpoonup$ after.apply(aply(t));   
}

很明显，Lambda表达式给复用代码提供了更多可能性：我们可以把一大段逻辑中变化的部分抽象成为函数式接口，由外部方法提供函数实现，重用方法内的整体逻辑处理。需要注意的是，在自定义函数式接口之前，可以先确认下java.util.function包中的43个标准函数式接口是否能满足需求，我们要尽可能重用这些接口，以提高代码的可读性。

# 1.3 使用Java8简化代码

本节将通过几个具体的例子，讲解使用Java8简化代码的3个重要方面：

- 使用流操作简化集合操作；  
- 使用可空类型简化判空逻辑；  
- JDK 8 结合 Lambda 表达式和流操作对各种类的增强。

# 1.3.1 使用流操作简化集合操作

Lambda 表达式可以用简短的代码实现方法的定义，为复用代码提供了更多可能性。利用这个特性，我们可以把集合的投影、转换、过滤等操作抽象成通用的接口，然后通过 Lambda 表达式传入其具体实现。这就是流操作。这里有一个具体的例子，用一段 20 行左右的代码，实现了如下的逻辑：

- 把整数列表换为 Point2D 列表；  
- 遍历 Point2D 列表过滤出 Y 值 $>1$ 的对象；  
- 计算 Point2D 点到原点的距离；  
- 累加所有计算出的距离，并计算距离的平均值。

实现代码如下：

private static double calc(List<String> ints) { //临时中间集合 List<Point2D> point2DList $=$ new ArrayList<>(); for (Integer i : ints) { point2DList.add(new Point2D.Double((double) i % 3, (double) i / 3)); } //临时变量，纯粹是为了获得最后结果需要的中间变量 double total $= 0$ int count $= 0$ for (Point2D point2D : point2DList) { //过滤 if (point2D/Y() > 1) { //计算距离 double distance $=$ point2D_distance(0, 0); total $+ =$ distance; count++;

}  
1 //注意count可能为0的可能return count $>0$ ?total /count:0;

现在使用流操作配合Lambda表达式来简化这段代码。简化后用一行代码就可以实现这样的逻辑，更重要的是代码可读性更强了，通过方法名就可以知晓大概是在做什么事情。例如：

- map 方法传入的是一个 Function，可以实现对象转换；  
- filter 方法传入的是一个 Predicate，实现对象的布尔判断，只保留返回 true 的数据；  
- mapToDouble 用于把对象转换为 double 类型；  
- 通过 average 方法返回一个 OptionalDouble，代表可能包含值也可能不包含值的可空 double。具体实现参考如下代码：

List<Integer>ints $=$ Arrays.asList(1,2,3,4,5,6,7,8); double average $=$ calc(ints); double streamResult $=$ ints.stream() .map(i->new Point2D.Double((double)i $\%$ 3,(double)i/3)) .filter(point->point.getY() >1) .mapToDouble(point->point_distance(0,0)) .average()) .else(0); //如何用一行代码来实现，比较一下可读性 assertThat(average,is(streamResult));

那么，OptionalDouble 又是怎么回事儿？

# 1.3.2 使用可空类型简化判空逻辑

类似 OptionalDouble、OptionalInt 和 OptionalLong 这样的对象都是服务于基本类型的可空类型。此外 Java 8 还定义了用于引用类型的 Optional<T> 类。使用 Optional<T> 类，不仅可以避免使用流操作进行级联调用的空指针问题，更重要的是它提供了一些实用的方法帮我们避免判空逻辑。如下是一些例子，演示了如何使用 Optional<T> 类来避免空指针，以及如何使用它的流式 API 来简化冗长的 if...else...判空逻辑：

@Test(expected $=$ IllegalArgumentException.class)   
public void optional() { //通过get方法获取Optional中的实际值 assertThat(Optional.of(1).get(),is(1)); //通过ofNullable来初始化一个null，通过或其他方法实现Optional中无数据时返回一个默认值 assertThat(Optional.ofNullable(null).或其他("A")，is("A")); //OptionalDouble是基本类型double的Optional对象，isPresent判断有无数据 assertFalse(OptionalDouble.empty().isEmpty()); //通过map方法可以对Optional对象进行级联转换，不会出现空指针，转换后还是一个Optional assertThat(Optional.of(1).map(Math::incrementExact).get(),is(2)); //通过filter实现Optional中数据的过滤，得到一个Optional，然后级联使用或其他提供默认值 assertThat(Optional.of(1).filter(integer->integer%2==0).或其他(null)，is (nullValue())); //通过或其他Throw实现无数据时抛出异常 Optional.empty().或其他Throw(ILlegalArgumentException::new);

Optional 类的常用方法，如表 1-1 所示。

表 1-1 Optional<T> 类的常用方法  

<table><tr><td>方法</td><td>作用</td><td>方法</td><td>作用</td></tr><tr><td>empty</td><td>返回一个空的 Optional</td><td>ifPresent</td><td>有值，就使用这个值调用 Consumer 函数消费值</td></tr><tr><td>orElse</td><td>有值则返回，否则返回默认值</td><td>isPresent</td><td>是否有值</td></tr><tr><td>orElseGet</td><td>有值则返回，否则返回 Supplier 函数提供的值</td><td>get</td><td>如果值存在则获取值，否则抛出 NoSuchElementException</td></tr><tr><td>orElseThrow</td><td>有值则返回，否则返回 Supplier 函数生成的异常</td><td>map</td><td>如果有值，则应用传入的 Function 函数</td></tr><tr><td>of</td><td>将值进行 Optional 包装，如果值为 null 抛出 NullPointerException</td><td>filter</td><td>如果有值并且匹配 Predicate，则返回包含值的 Optional，否则返回空 Optional</td></tr><tr><td>ofNullable</td><td>将值进行 Optional 包装，如果值为 null 则生成空的 Optional</td><td></td><td></td></tr></table>

# 1.3.3 使用Java8的一些新类、新方法获得函数式编程体验

除流操作之外，Java 8 中还有很多类实现了函数式的功能。例如，要通过 HashMap 实现一个缓存的操作，在 Java 8 之前我们可能会写出这样的 getProductAndCache 方法：先判断缓存中是否有值；如果没有值，就从数据库搜索取值；最后把数据加入缓存。

```java
private Map<Long, Product> cache = new ConcurrentHashMap<>();  
private Product getProductAndCache(Long id) {  
    Product product = null;  
    // 键存在，返回值  
    if (cache.containsKey(id)) {  
        product = cache.get(id);  
    } else {  
        // 键不存在，则获取值  
        // 需要遍历数据源查询获得 Product  
        for (Product p : ProductGetData()) {  
            if (p.getId().equals(id)) {  
                product = p;  
                break;  
            }  
        }  
        // 加入 ConcurrentHashMap  
        if (product != null) {  
            cache.put(id, product);  
        }  
    }  
    return product;  
} 
```

在Java8中，利用ConcurrentHashMap的computeIfAbsent方法，就可以省去写烦琐的if...else...代码实现相同的效果，如下代码所示：

```java
private Product getProductAndCacheCool(Long id) { //当键不存在的时候提供一个Function来代表根据键获取值的过程 return cache.computeIfAbsent(id,i-> Product的数据().stream() .filter(p->p.getId().equals(i))//过滤 .findFirst() //找第一个，得到Optional<Product> .或其他(null)); //如果找不到Product，则使用null } @Test public void coolCache() { getProductAndCacheCool(1L); getProductAndCacheCool(100L); System.out.println(cache); assertThat(cache.size(),is(1)); assertTrue(cache.containsKey(1L)); }
```

又如，利用files.walk返回一个Path的流，通过两行代码就能实现“递归搜索+grep”的操作。整个逻辑是：递归搜索文件夹，查找所有的.java文件；然后读取文件中的每一行内容，用正则表达式匹配public class关键字；最后输出文件名和这行内容。

```java
@Test
public void filesExample() throws IOException {
    // 无限深度，递归遍历文件夹
    try (Stream< path> pathStream = Files.walk(Paths.get().)) {
        pathStream.filterFiles::isRegularFile) //只查找普通文件
        .filter(FileSystems.getDefinition().getPathMatcher("glob:**/*.java}): matches) //搜索.java源码文件
        .flatMap(ThrowingFunction unchecked(path -> Files.readAllLines(path).stream() //读取文件内容，转换为Stream< List>
        .filter(line -> Pattern.compile("public class").matches(line). find()) //使用正则过滤带有public class的行
        .map(line -> path.getName() + " >> " + line))
        //把这行文件内容转换为文件名+行
       .forEach(System.out::println); //打印所有的行
}
```

输出结果如图1-6所示

```java
EqualityMethodController.java >> public class EqualityMethodController {
    CommonMistakesApplication.java >> public class CommonMistakesApplication {
        AnnotationInheritanceApplication.java >> public class AnnotationInheritanceApplication {
            GenericAndInheritanceApplication.java >> public class GenericAndInheritanceApplication {
                CommonMistakesApplication.java >> public class CommonMistakesApplication {
                    BaseController.java >> public class BaseController {
                        TestController.java >> public class TestController extends BaseController {
                            ReflectionIssueApplication.java >> public class ReflectionIssueApplication {
                                Utilities.java >> public class Utilities {
                                    CommonMistakesApplication.java >> public class CommonMistakesApplication {
                                    ConcurrentHashMapMisuseController.java >> public class ConcurrentHashMapMisuseController {
                                    CommonMistakesApplication.java >> public class CommonMistakesApplication {
                                    ConcurrentHashMapPerformanceController.java >> public class ConcurrentHashMapPerformanceController {
                                    CopyOnWriteListMisuseController.java >> public class CopyOnWriteListMisuseController {
                                    CommonMistakesApplication.java >> public class CommonMistakesApplication {
                                    ThreadLocalMisuseController.java >> public class ThreadLocalMisuseController { 
```

图1-6 使用流操作递归查找文件内容的输出

还有一个小技巧：files>AllLines 方法会抛出一个受检异常（IOException），这时可以使

用一个自定义的函数式接口，并用ThrowingFunction包装这个方法，把受检异常转换为运行时异常，让代码更清晰。

```java
@FunctionalInterface
public interface ThrowingFunction<T, R, E extendsThrowable> {
    static <T, R, E extendsThrowable> Function<T, R> unchecked(ThrowingFunction<T, R, E> f) {
        return t -> {
            try {
                return f.apply(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
    R apply(T t) throws E;
} 
```

如果用Java7实现类似逻辑大概需要几十行代码，读者可以自行尝试一下。

# 1.4 并行流

除了串行流，Java8还提供了并行流的功能：通过parallel方法，一键把流转换为并行操作提交到线程池处理。例如，通过线程池来并行消费处理 $1\sim 100$

```java
IntStream.rangeClosed(1, 100).parallel().forEach(i -> {
    System.out.println(LocalDateTime-now() + " : " + i);
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
} 
```

并行流不确保执行顺序，并且因为每次处理耗时 $1\mathrm{s}$ ，所以在8核的机器上，是按照1s输出一次、一次输出8个数字，如图1-7所示。

![](images/6016773562c4d9d010cb147994e871b8f947b9185d312caa6564871250863f70.jpg)  
图1-7 测试parallel方法的输出

本书中有很多类似使用 threadCount 个线程对某个方法总计执行 taskCount 次操作的案例，用于演示并发情况下的多线程问题或多线程处理的性能。除了会用到并行流，我们有时也会使用线程池或直接使用线程进行类似操作。

下面是实现此类操作的5种方式。为了测试这5种实现方式，本节设计了一个场景：使用20个线程以并行方式总计执行10000次操作。因为单个任务单线程执行需要 $10\mathrm{ms}$ ，也就是每秒吞吐量是100个操作，那20个线程QPS是2000个操作，执行完10000次操作最少耗时 $5\mathrm{s}$ 。任务代码如下：

```java
private void increment(AtomicInteger atomicInteger) {  
    atomicIntegerincrementAndGet();  
    try {  
        TimeUnit.MILLISECONDS.sleep(10);  
    } catch (InterruptedException e) {  
        e.printStackTrace();  
    } 
```

现在测试这5种方式是否都可以利用更多的线程并行执行操作。

（1）使用线程。直接把任务按照线程数均匀分配到不同的线程执行，使用 CountDownLatch 来阻塞主线程，直到所有线程都完成操作。这种方式，需要我们自己分割任务。实现代码如下：

```txt
private int thread(int taskCount, int threadCount) throws InterruptedException { // 总操作次数计数器  
AtomicInteger atomicInteger = new AtomicInteger();  
// 使用 CountDownLatch 来等待所有线程执行完成  
CountDownLatch countDownLatch = new CountDownLatch(threadCount);  
// 使用 IntStream 把数字直接转为 Thread  
IntStream.closeClosed(1, threadCount).mapToObj(i -> new Thread()) -> {  
// 手动把 taskCount 次操作分成 taskCount 份，每份有一个线程执行  
IntStream.closeClosed(1, taskCount / threadCount).forEach(j -> increment(atomicInteger));  
// 每一个线程处理完自己那部分数据之后，countDown一次  
countDownLatch.countDown();  
} ).forEach(Thread::start);  
// 等到所有线程执行完成  
countDownLatch await();  
// 查询计数器当前值  
return atomicInteger.get();
```

（2）使用Executors.newFixedThreadPool来获得固定线程数的线程池，使用execute提交所有任务到线程池执行，最后关闭线程池等待所有任务执行完成。实现代码如下：

```java
private int threadpool(int taskCount, int threadCount) throws InterruptedException { //总操作次数计数器  
    AtomicInteger atomicInteger = new AtomicInteger();  
    //初始化一个线程数量 = threadCount 的线程池  
    ExecutorService executorService = Executors.newFixedThreadPool(taskCount);  
    //所有任务直接提交到线程池处理  
    IntStream rangeClosed(1, taskCount).forEach(i -> executorService.exec((i -> increment(atomicInteger)));  
    //提交关闭线程池申请，等待之前所有任务执行完成  
    executorService.shutdown();  
    executorService awaitTermination(1, TimeUnit.HOURS);  
    //查询计数器当前值  
    return atomicInteger.get(); 
```

（3）使用ForkJoinPool而不是普通线程池执行任务。ForkJoinPool和传统的ThreadPoolExecutor区别在于，前者对于 $n$ 并行度有 $n$ 个独立队列，后者是共享队列。如果有大量执行耗时比较短的任务，ThreadPoolExecutor的单队列就可能会成为瓶颈。这时，使用ForkJoinPool性能会更好。

因此，ForkJoinPool更适合把大任务分割成许多小任务并行执行的场景，而ThreadPoolExecutor适合许多独立任务并发执行的场景。

如下代码所示，先自定义一个具有指定并行数的ForkJoinPool，再通过这个ForkJoinPool并行执行操作：

```txt
private int forkjoin(int taskCount, int threadCount) throws InterruptedException { // 总操作次数计数器  
AtomicInteger atomicInteger = new AtomicInteger();  
// 自定义一个并行度 = threadCount 的 ForkJoinPool  
ForkJoinPool forkJoinPool = new ForkJoinPool(taskCount);  
// 所有任务直接提交到线程池处理  
forkJoinPool.exec((   ) -> IntStream.closeClosed(1, taskCount).parallel().forEach(i -> increment(atomicInteger));  
// 提交关闭线程池申请，等待之前所有任务执行完成  
forkJoinPool.shutdown();  
forkJoinPool awaitTermination(1, TimeUnit.HOURS);  
// 查询计数器当前值  
return atomicInteger.get();
```

（4）直接使用并行流，并行流使用公共的ForkJoinPool，也就是ForkJoinPool(common Pool()。公共的ForkJoinPool默认的并行度是CPU核心数-1，原因是对于CPU绑定的任务分配超过CPU个数的线程没有意义。因为并行流还会使用主线程执行任务，也会占用一个CPU内核，所以公共的ForkJoinPool的并行度即使减去1也能用满所有CPU内核。如下代码所示，我们通过配置强制指定（增大）了并行数，但因为使用的是公共ForkJoinPool，所以可能会存在干扰：

private int stream(int taskCount, int threadCount) { //设置公共的ForkJoinPool的并行度 System.setProperty("java.util.concurrent.ForkJoinPool/common.parallaxm", String.valueOf(threadCount)); //总操作次数计数器 AtomicIntegeratomicInteger $=$ new AtomicInteger(); //由于设置了公共的ForkJoinPool的并行度，因此直接使用parallel提交任务即可 IntStream.closeClosed(1，taskCount).parallel().forEach(i->increment (atomicInteger)); //查询计数器当前值 return atomicInteger.get(); }

（5）使用CompletableFuture。CompletableFuture.runAsync方法可以指定一个线程池，一般会在使用CompletableFuture的时候用到。实现代码如下：

private int completableFuture(int taskCount, int threadCount) throws扰乱Exception, ExecutionException { // 总操作次数计数器 AtomicIntegeratomicInteger $=$ new AtomicInteger(); //自定义一个并行度 $\equiv$ threadCount的ForkJoinPool ForkJoinPool forkJoinPool $=$ new ForkJoinPool(threadCount); //使用CompletableFuture.runAsync通过指定线程池异步执行任务 CompletableFuture.runAsync() -> IntStream.close(1，taskCount). parallel().forEach(i -> increment(atomicInteger)), forkJoinPool).get(); //查询计数器当前值 return atomicInteger.get(); }

这5种方法都可以实现类似的效果，如图1-8所示，这5种方式执行完10000个任务的耗时都在 $5.4\mathrm{s}$ 到 $6\mathrm{s}$ 之间（这里的结果只是证明并行度的设置是有效的，并不是性能的比较）。

![](images/1796a0fdeb4b8c11282be71891a28b7265eb2709fa2eec796ac6e943cc5d7e5a.jpg)  
图1-85种任务并行执行方式的执行时间统计

如果程序对性能特别敏感，建议根据场景通过性能测试选择适合的模式。一般而言，使用线程池（第二种）和直接使用并行流（第四种）的方式在业务代码中比较常见。但需要注意的是，我们通常会重用线程池，而不会像示例中那样在业务逻辑中直接声明新的线程池等操作完成后再关闭。还需要注意的是，示例中是先运行 stream 方法再运行 forkjoin 方法，对公共的 ForkJoinPool 默认并行度的修改才能生效。这是因为 ForkJoinPool 类初始化公共线程池是在静态代码块里，加载类时就会进行的，如果 forkjoin 方法中先使用了 ForkJoinPool，即便 stream 方法中设置了系统属性也不会起作用。因此我的建议是，设置 ForkJoinPool 公共线程池默认并行度的操作，应该放在应用启动时执行。

# 1.5 流操作详解

流操作用于对集合进行投影、转换、过滤、排序等。更进一步地，这些操作能链式串联在一起使用，类似于SQL语句，可以大大简化代码。流操作是Java8中非常重要的一个新特性，也是本书大部分代码都会用到的操作。如果读者感觉有些案例不好理解，可以对照代码逐一到源码中查看流操作的方法定义和JDK中的代码注释。本书涉及的流操作如表1-2所示。

表 1-2 流操作清单  

<table><tr><td>方法</td><td>中文</td><td>操作类型</td><td>类比 SQL</td><td>使用的类型/函数式接口</td><td>作用</td></tr><tr><td>filter</td><td>筛选/过滤</td><td>中间</td><td>WHERE</td><td>Predicate&lt;T&gt;</td><td>对流过滤,使元素符合传入条件</td></tr><tr><td>map</td><td>转换/投影</td><td>中间</td><td>SELECT</td><td>Function&lt;T,R&gt;</td><td>使用传入的函数,对流中每个元素进行转换</td></tr><tr><td>flatMap</td><td>展开/扁平化</td><td>中间</td><td>N/A</td><td>Function&lt;T,Stream&lt;R&gt;&gt;</td><td>相当于 map+flat,通过 map 把每一个元素转换为一个流,然后把所有流链接到一起扁平化展开</td></tr><tr><td>sorted</td><td>排序</td><td>中间</td><td>ORDER BY</td><td>Comparator&lt;T&gt;</td><td>使用传入的比较器,对流中的元素排序</td></tr><tr><td>distinct</td><td>去重</td><td>中间</td><td>DISTINCT</td><td>long</td><td>对流中元素去重(使用 Object.equals 判重)</td></tr><tr><td>skip &amp; limit</td><td>分页</td><td>中间</td><td>LIMIT</td><td>long</td><td>跳过流中部分元素以及限制元素数量</td></tr><tr><td>collect</td><td>收集</td><td>终结</td><td>N/A</td><td>Collector&lt;T,A,R&gt;</td><td>对流进行终结操作,把流导出成为我们需要的数据结构</td></tr><tr><td>forEach</td><td>遍历</td><td>终结</td><td>N/A</td><td>Consumer&lt;T&gt;</td><td>对每个元素遍历进行消费</td></tr><tr><td>anyMatch</td><td>是否有元素匹配</td><td>终结</td><td>N/A</td><td>Predicate&lt;T&gt;</td><td>使用谓词 (predicate) 判断是否有任何一个元素满足匹配</td></tr><tr><td>allMatch</td><td>是否所有元素匹配</td><td>终结</td><td>N/A</td><td>Predicate&lt;T&gt;</td><td>使用谓词判断是否所有元素都满足匹配</td></tr></table>

本节会围绕订单场景介绍如何使用流操作的各种API完成订单的统计、搜索、查询等功能。读者可以结合代码中的注释理解案例，也可以自己运行源码观察输出。如下代码所示，先定义一个订单类、一个订单商品类和一个顾客类，用作后续示例代码的数据结构：

```java
// 订单类
@Data
public class Order {
    private Long id;
    private Long customerId; // 顾客ID
    private String customerName; // 顾客姓名
    private List<Items> orderItemList; // 订单商品明细
    private Double totalPrice; // 总价格
    private LocalDateTime placedAt; // 下单时间
}
// 订单商品类
@Data
@AllArgs Constructor
@NoArgs Constructor
public class OrderItem {
    private Long productId; // 商品ID
    private String/productName; // 商品名称
    private Double productPrice; // 商品价格
    private Integer productQuantity; // 商品数量
}
// 顾客类
@Data
@AllArgs Constructor
public class Customer {
    private Long id;
    private String name; // 顾客姓名 
```

我们还会在测试类中定义一个 orders 字段，填充一些模拟数据，类型是 List<Order>，本节将会用到这个字段。

# 1.5.1 创建流

要使用流，就要先创建流。创建流一般有如下5种方式

- 方式1：通过stream方法把List或数组转换为流。  
- 方式2：通过Stream.of方法直接传入多个元素构成一个流。  
- 方式3：通过Streamiterate方法使用迭代的方式构造一个无限流，然后使用limit限制流元素的个数。  
- 方式4：通过Stream_generate方法从外部传入一个提供元素的Supplier来构造无限流，然后使用limit限制流元素的个数。  
- 方式5：通过IntStream或DoubleStream构造基本类型的流。

创建流的代码如下：

```java
//方式1：通过stream方法把List或数组转换为流
@Test
public void stream()
{
    Arrays.asList("a1", "a2", "a3").stream().forEach(System.out::println);
    Arrays.stream(new int[]{1, 2, 3}).forEach(System.out::println);
} 
```

```java
//方式2：通过Stream.of方法直接传入多个元素构成一个流
@Test
public void of()
{
    String[] arr = {"a", "b", "c)};
    Stream.of(arr).forEach(System.out::println);
    Stream.of("a", "b", "c").forEach(System.out::println);
    Stream.of(1, 2, "a").map(item -> item_CLASS(). getName());
        out::println);
} 
```

```java
//方式3：通过Streamiterate方法使用迭代的方式构造一个无限流，然后使用limit限制流元素的个数 @Test   
public void iterate() { Streamiterate(2, item -> item \* 2).limit(10).forEach(System.out::println); Streamiterate(BigInteger.ZERO, n -> n.add(BigInteger.TEN)).limit(10).forEach(System.out::println); }
```

//方式4：通过Stream.generator方法从外部传入一个提供元素的Supplier来构造无限流，然后使用limit限制流元素的个数

```txt
@Test   
public void generate()   
{ Stream.generator(（）->"test").limit(3).forEach(System.out::println); Stream.generator(Math::random).limit(10).forEach(System.out::println); }
```

//方式5：通过IntStream或DoubleStream构造基本类型的流

```java
@Test   
public void primitive()   
{ //演示IntStream和DoubleStream   
InputStream(range(1,3).forEach(System.out::println);   
InputStream(range(0,3).mapToObj(i->"x").forEach(System.out::println);   
InputStream(rangeClosed(1,3).forEach(System.out::println);   
DoubleStream.of(1.1,2.2,3.3).forEach(System.out::println); 
```

// 各种转换，后面注释代表的是输出结果

```java
System.out.println(IntStream.of(1, 2)..toArray().getClass()); //class [I System.out.println(IntStream.of(1, 2).mapToInt(Integer::intValue)..toArray().getClass())); //class [I System.out.println(IntStream.of(1, 2). boxed()..toArray().getClass()); //class [Ljava.lang.Object; System.out.println(IntStream.of(1, 2).asDoubleStream()..toArray(). Class()); //class [D System.out.println(IntStream.of(1, 2).asLongStream()..toArray(). Class()); //class [J 
```

//注意基本类型流和装箱后的流的区别

```txt
Arrays.asList("a", "b", "c").stream() // Stream<String> .mapToInt(String::length) // IntStream .asLongStream() // LongStream .mapToDouble(x -> x / 10.0) // DoubleStream .boxed() // Stream<Double> .mapToLong(x -> 1L) // LongStream .mapToObj(x -> ""// Stream<String> .collect(Collectors.list()); 
```

# 1.5.2 filter

filter 方法可以实现过滤操作，类似于 SQL 中的 WHERE。我们可以使用一行代码，通过 filter 方法实现查询所有订单中最近半年总价格大于 40 元的订单，通过连续叠加 filter 方法进行多次条件过滤：

//最近半年总价格大于40的订单

```rust
orders.stream()
.filter(Object::nonNull) //过滤null值
.filter(order -> order.getPlacedAt().isAfter(LocalDateTime但现在())
.minusMonths(6)) //最近半年的订单
.filter(order -> order.getTotalPrice() > 40) //总价格大于40的订单
.forEach(System.out::println); 
```

如果不使用流操作的话，必然需要一个中间集合来收集过滤后的结果，而且所有的过滤条件会堆积在一起，代码冗长且不易读。

# 1.5.3 map

map 操作可以做转换（或者说投影），类似于 SQL 中的 SELECT。为了对比，本书用两种方式统计订单中所有商品的数量，前一种是通过两次遍历实现，后一种是通过两次 “mapToLong+sum 方法” 实现：

//计算所有订单商品数量

//通过两次遍历实现

```javascript
LongAdder longAdder = new LongAdder();  
orders.stream().forEach(order -> order.getOrderItemList().forEach(orderItem -> longAdder.add(orderItem-productQuantity())); 
```

//使用两次“mapToLong+sum方法”实现

```rust
assertThat(longAdder.longValue(), is Orders.stream().mapToLong(order -> order.getListItemList().stream())
. mapToLong(OrderItem::getProductQuantity).sum().sum()); 
```

显然，后一种方式无须中间变量longAdder，更直观。再补充一下，使用for循环生成数据属于常用操作，也是本书会大量用到的。我们可以用一行代码使用IntStream配合mapToObj替代for循环来生成数据，如生成10个Product元素构成List，代码如下：

```javascript
//使用mapToObj方法把IntStream转换为Stream<Project>  
System.out.println(IntStream.closeClosed(1,10)  
    .mapToObj(i->new Product(long)i, "product"+i, i*100.0))  
    .collect(toList())); 
```

# 1.5.4 flatMap

flatMap 展开或者叫扁平化操作，相当于“map+flat”，通过 map 把每个元素替换为一个流，然后展开这个流。例如，要统计所有订单的总价格，可以有如下两种方式。

- 直接通过原始商品列表的商品个数 $\times$ 商品单价统计，可以先把订单通过 flatMap 展开成商品清单，也就是把 Order 替换为 Stream<OrderItem>，然后对每个 OrderItem 用 mapToDouble 转换获得商品总价，最后进行一次 sum 求和。  
- 利用flatMapToDouble方法把列表中每一项展开替换为一个DoubleStream，也就是直接把

每个订单转换为每个商品的总价，然后求和。

实现代码如下：

//方式1：直接展开订单商品进行价格统计

```txt
System.out.println(orderStream())
    .flatMap(order -> order.getOrderItemList().stream())
    .mapToDouble(item -> item-productQuantity() * item-productPrice().sum());
//方式2：flatMap+mapToDouble=flatMapToDouble
System.out.println(order.stream())
    .flatMapToDouble(order -> order.getOrderItemList().stream())
    .mapToDouble(item -> item-productQuantity() * item-productPrice().sum()); 
```

这两种方式可以得到相同的结果，并无根本区别。

# 1.5.5 sorted

sorted 操作可被用于行内排序的场景，类似 SQL 中的 ORDER BY。例如，要实现总金额大于 50 元的订单按价格倒序取前五，可以通过 Order::getTotalPrice 方法引用直接指定需要排序的依据字段，通过 reversed() 实现倒序：

//总金额大于50的订单，按照订单价格倒序取前五

```rust
orders.stream().filter(order -> orderTOTALPrice() > 50)  
    .sorted(comparing(Order::getTotalPrice).reversed())  
    .limit(5)  
    .forEach(System.out::println);
```

# 1.5.6 distinct

distinct 操作的作用是去重，类似于 SQL 中的 DISTINCT。例如，要实现以下功能。

- 查询去重后的下单顾客姓名。使用 map 从订单提取购买顾客姓名，然后使用 distinct 去重。  
- 查询购买过的商品名称。使用“flatMap+map”提取订单中所有的商品名称，然后使用distinct去重。

实现代码如下：

//去重后的下单顾客姓名

```javascript
System.out.println Orders.stream().map(order -> order.getCustomerName());
distinct().collect(Joining["", "")); 
```

//购买过的商品名称

```txt
System.out.println(order.stream())
. flatMap(order -> order.getListItem().stream())
. map(OrderItem::getProductName)
. distinct().collect(joining("","")); 
```

# 1.5.7 skip和limit

skip 和 limit 操作用于分页，类似 MySQL 中的 LIMIT。其中，skip 实现跳过一定的项，limit 用于限制项总数。比如下面的两段代码：

```javascript
// 按照下单时间排序，查询前两个订单的顾客姓名和下单时间 orders.stream()
```

```txt
sorted Comparing(Order::getPlacedAt) .map(order -> order.getCustomerName() + "@" + order.getPlacedAt()) limit(2).forEach(System.out::println); //按照下单时间排序，查询第三个和第四个订单的顾客姓名和下单时间 orders.stream() sorted Comparing(Order::getPlacedAt) .map(order -> order.getCustomerName() + "@" + order.getPlacedAt()) skip(2).limit(2).forEach(System.out::println);
```

# 1.5.8 collect

collect是收集操作，对流进行终结（终止）操作，把流导出为我们需要的数据结构。“终结”是指，导出后无法再串联使用其他中间操作，如filter、map、flatmap、sorted、distinct、limit、skip。在流操作中，collect是最复杂的终结操作，比较简单的终结操作有forEach、ToArray、min、max、count、anyMatch等，读者可以查询JDK文档，搜索terminal operation或intermediate operation了解它们的用法。

下面有6个案例，用来演示几种比较常用的collect操作。

- 案例1：实现了字符串拼接操作，生成一定位数的随机字符串。  
- 案例2：通过Collectors.toSet静态方法收集为Set去重，得到去重后的下单顾客姓名，再通过Collectors joining静态方法实现字符串拼接。  
- 案例3：通过Collectors.toCollection静态方法获得指定类型的集合，比如把List<Order>转换为LinkedList<Order>。  
- 案例4：通过Collectors.map静态方法将对象快速转换为Map，键是订单号、值是下单的顾客姓名。  
- 案例5：通过Collectors.map静态方法将对象转换为Map。键是下单的顾客姓名，值是下单时间，一个顾客可能多次下单，所以直接在这里进行了合并，只获取最近一次的下单时间。  
- 案例6：使用Collectors-summingInt方法对商品数量求和，再使用Collectors.averagingInt方法对结果求平均值，以统计所有订单的平均商品数量。

//案例1：生成一定位数的随机字符串

```txt
System.out.println(random.ints(48, 122)  
    .filter(i -> (i < 57 || i > 65) && (i < 90 || i > 97))  
    .mapToObj(i -> (char) i)  
    .limit(20)  
    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)  
    . toString(); 
```

//案例2：所有下单的顾客，使用toSet去重后实现字符串拼接

```javascript
System.out.println(order.stream())
.map(order -> order.getCustomerName()).collect(toSet())
.stream().collect(joining[""," ["["]]")); 
```

//案例3：使用toCollection收集器指定集合类型

```javascript
System.out.println Orders.stream().limit(2).collect(toCollection(LinkList::new)).getClass()); 
```

//案例4：使用 toMap 获取“订单号 + 下单顾客姓名”的Map

```rust
orders.stream()
    .collect(toMap(Order::.: getId, Order:::getCustomerName))
    .enterSet().forEach(System.out:::println); 
```

//案例5：使用toMap获取“下单顾客姓名 $^+$ 最近一次下单时间”的Maporders.stream()collect(toMap(Order::getCustomerName，Order::getPlacedAt，(x，y）->x.isAfter(y)?x：y))．entryset().forEach(System.out::println);

```txt
//案例6：使用summingInt对商品数量求和，使用averagingInt统计订单的平均商品数量System.out.println Orders.stream().collect(averagingInt(order ->order.getListItemList().stream())
			collect( summingInt(OrderItem::getProductQuantity))))；
```

使用流操作方式的话，这6个操作一行代码就可以实现，否则需要几行甚至十几行代码。Collectors类的一些常用静态方法，如表1-3所示。

表 1-3 Collectors 类的一些常用静态方法  

<table><tr><td>方法</td><td>返回类型</td><td>作用</td></tr><tr><td>toList</td><td>List&lt;T&gt;</td><td>把流中的元素收集成为一个List</td></tr><tr><td>toSet</td><td>Set&lt;T&gt;</td><td>把流中的元素收集成为一个Set，去重</td></tr><tr><td>toCollection</td><td>Collection&lt;T&gt;</td><td>把流中的元素收集成为指定的集合</td></tr><tr><td>counting</td><td>Long</td><td>计算流中的元素个数</td></tr><tr><td>summingInt</td><td>Integer</td><td>对流中元素的某个整数属性求和</td></tr><tr><td>averagingInt</td><td>Double</td><td>对流中元素的某个整数属性求平均值</td></tr><tr><td>joining</td><td>String</td><td>连接流中元素 toString后的字符串</td></tr><tr><td>minBy</td><td>Optional&lt;T&gt;</td><td>使用指定的比较器选出最小元素</td></tr><tr><td>maxBy</td><td>Optional&lt;T&gt;</td><td>使用指定的比较器选出最大元素</td></tr><tr><td>collectingAndThen</td><td>根据收集器的返回</td><td>包裹另一个收集器，对结果进行转换</td></tr><tr><td>groupingBy</td><td>Map&lt;K,List&lt;T&gt;&gt;</td><td>根据元素的一个属性值对元素分组，属性值作为键</td></tr><tr><td>partitioningBy</td><td>Map&lt;Boolean,List&lt;T&gt;&gt;</td><td>根据流中元素应用谓词的结果，将元素分成 true 和 false 两个区</td></tr></table>

针对比较复杂的 groupingBy 和 partitioningBy，将会在接下来的两节中介绍。

# 1.5.9 groupingBy

groupingBy是分组统计操作，类似SQL中的GROUP BY子句。它和partitioningBy都是特殊的收集器，同样也是终结操作。分组操作比较复杂，本书准备了8个案例。

- 案例1：按照顾客姓名分组，使用Collectors.counting方法统计每个人的下单数量，再按照下单数量倒序输出。  
- 案例2：按照顾客姓名分组，使用Collectors-summingDouble方法统计订单总金额，再按总金额倒序输出。  
- 案例3：按照顾客姓名分组，使用两次Collectors-summingInt方法统计商品数量，再按总数量倒序输出。  
- 案例4：统计被采购最多的商品。先通过flatMap把订单转换为商品，然后把商品名作为键、Collectors-summingInt作为值分组统计采购数量，再按值倒序获取第一个键值对，最后查询键就得到了售出最多的商品。  
- 案例5：同样统计采购最多的商品。相比案例4排序Map的方式，这次直接使用Collectors.maxBy

收集器获得最大的键值对。

- 案例6：按照顾客姓名分组，统计顾客下的总价格最高的订单。键是顾客姓名，值是Order，直接通过Collectors.maxBy方法拿到总价格最高的订单，然后通过collectingAndThen实现Optional.get的内容提取，最后遍历键/值即可。  
- 案例7：根据下单年月分组统计订单号列表。键是格式化成年月后的下单时间，值直接通过Collectors.mapping方法进行了转换，把订单列表转换为订单号构成的List。  
- 案例8：根据下单年月和顾客姓名两次分组统计订单号列表，相比案例7多了一次分组操作，第二次分组是按照顾客姓名进行分组。

具体实现代码如下：

//案例1：按照顾客姓名分组，统计下单数量

```java
System.out.println Orders.stream().collect(groupingBy(Order::getCustomerName, counting()))).entrySet().stream().sorted(Map.Entry<String, Long>comparingByValue().reversed().collect(toList())); 
```

//案例2：按照顾客姓名分组，统计订单总价格

```java
System.out.println Orders.stream().collect(groupingBy(Order::getCustomerName, summingDouble(Order::getTotalPrice)))
    .entrySet().stream().sorted(Map.Entry<String, Double>comparingByValue());
    reversed()).collect(toList())); 
```

//案例3：按照顾客姓名分组，统计商品数量

```cpp
System.out.println(order.stream().collect(groupingBy(Order::getCustomerName, summingInt(order -> order.getListItem().stream())
            .collect(summingInt(OrderItem::getProductQuantity)))
            .entrySet().stream().sorted(Map.Entry<String, Integer>comparingByValue().reversed()).collect(toList())); 
```

//案例4：统计最受欢迎的商品，倒序后取第一个

```txt
orders.stream()
. flatMap(order -> order.getListItemList().stream())
. collect(groupingBy(OrderItem::getProductName, summingInt(OrderItem::getProductQuantity)))
. entrySet().stream()
. sorted(Map.Entry<String, Integer>comparingByValue().reversed())
. map(Map.Entry::getKey)
. findFirst()
. ifPresent(System.out::println); 
```

//案例5：统计最受欢迎的商品的另一种方式，直接利用maxBy

```kotlin
orders.stream()
. flatMap(order -> order.getListItemList().stream())
. collect(groupingBy(OrderItem::getProductName, summingInt(OrderItem::getProductQuantity)))
. entrySet().stream()
. collect(maxBy(Map.Entry.comparingByValue|)
. map(Map.Entry::getKey)
. ifPresent(System.out::println); 
```

//案例6：按照顾客姓名分组，选顾客下的总价格最大的订单

```txt
orders.stream().collect(groupingBy(Order::getCustomerName, collectingAndThen(maxBy (comparingDouble(Order::getTotalPrice)), Optional::get))) .forEach((k, v) -> System.out.println(k + "#" + v.getTotalPrice() + "@" + v.getPlacedAt())); 
```

//案例7：根据下单年月分组，统计订单号列表

```java
System.out.println(orderStream().collect 
```

```javascript
( groupingBy(order -> order.getPlacedAt().format(DateTimeFormatter. ofPattern("yyyyMM"))),mapping(order -> order.getId(),toList()))); 
```

//案例8：根据“下单年月 $^+$ 用户名”两次分组，统计订单号列表

```javascript
System.out.println(order.stream().collect(groupingBy(order -> order.getPlacedAt().format(DateTimeFormatter.ofPattern("yyyyMM"))), groupingBy(order -> order.getCustomerName(), mapping(order -> order.getId(), toList())); 
```

如果不借助流操作，而使用普通的Java代码，实现这些复杂的操作可能需要几十行代码。

# 1.5.10 partitioningBy

partitioningBy 用于分区，分区是特殊的分组，只有 true 和 false 两组。例如，我们把用户按照是否下单进行分区，给 partitioningBy 方法传入一个 Predicate 作为数据分区的区分，输出是 Map<Boolean, List<T>>：

```txt
public static <T>   
Collector<T,?,MapBoolean，List<T>> partitioningBy(Predicate<? super T> predicate){ return partitioningBypredicate,toList();   
} 
```

测试一下，partitioningBy配合anyMatch，可以把用户分为下过订单和没下过订单两组：

//根据是否有下单记录进行分区

```rust
System.out.println(Customer的数据().stream().collect(
partitioningBy/customer -> orders.stream().mapToLong(Order:::getCustomerId)
.anyMatch(id -> id == customer.getId())); 
```

# 1.6 小结

本章讲解的Lambda表达式、可空类型、流操作、并行流是Java8中非常重要的几个特性，可以帮助我们写出简单易懂、可读性更强的代码。特别是使用流操作的链式编程，可以用一行代码完成之前几十行代码才能完成的工作。流操作的API博大精深，但又有规律可循。其中的规律主要就是，厘清这些API传参的函数式接口定义，就能搞明白到底是需要我们提供数据、消费数据，还是转换数据等。而掌握流操作的方法就是多测试多练习，以强化记忆、加深理解。

# 1.7 思考与讨论

1. 对于 1.4 节中并行消费处理 1~100 的例子，如果把forEach 替换为forEachOrdered，会发生什么？

forEachOrdered会让并行流丧失部分的并行能力，主要原因是forEach遍历的逻辑无法并行起来（需要按照循环遍历，无法并行）。下面比较一下下面的3种写法：

// 模拟消息数据需要 1s

```txt
private static void consume(int i) { try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); } System.out.print(i); 
```

```java
} //模拟过滤数据需要1s private static boolean filter(int i) { try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); } return i % 2 == 0; } @Test public void test() { System.setProperty("java.util.concurrent.ForkJoinPool/common.parallaxm", String.valueOf(10)); StopWatch stopWatch = new StopWatch(); stopWatch.start("stream"); stream(); stopWatch.stop(); stopWatch.start("parallelStream"); parallelStream(); stopWatch.stop(); stopWatch.start("parallelStreamForEachOrdered"); parallelStreamForEachOrdered(); stopWatch.stop(); System.out.println(stopWatchprettyPrint()); } //写法1：filtre和forEach串行 private void stream() { IntStream rangeClosed(1, 10) .filter(ForEachOrderedTest::filter) .forEach(ForEachOrderedTest::consume); } //写法2：filter和forEach并行 private void parallelStream() { IntStream rangeClosed(1, 10).parallel() .filter(ForEachOrderedTest::filter) .forEach(ForEachOrderedTest::consume); } //写法3：filter并行而forEach串行 private void parallelStreamForEachOrdered() { IntStream rangeClosed(1, 10).parallel() .filter(ForEachOrderedTest::filter) .forEachOrdered(ForEachOrderedTest::consume); } 
```

# 得到输出如下：

<table><tr><td>ns</td><td>%</td><td>Task name</td></tr><tr><td>15119607359</td><td>065%</td><td>stream</td></tr><tr><td>2011398298</td><td>009%</td><td>parallelStream</td></tr><tr><td>6033800802</td><td>026%</td><td>parallelStreamForEachOrdered</td></tr></table>

从上述输出中可以看到：

- stream 方法的过滤和遍历全部串行执行，总时间是 $15 \mathrm{~s}$ （即 $10 \mathrm{~s} + 5 \mathrm{~s}$ ）；  
- parallelStream 方法的过滤和遍历全部并行执行，总时间是 $2s$ （即 $1s + 1s$ ）；  
- parallelStreamForEachOrdered方法的过滤并行执行，遍历串行执行，总时间是6s（即 $1\mathrm{s} + 5\mathrm{s}$ ）。

2. 使用流操作可以非常方便地对列表做各种操作，那如何在整个过程中观察数据的变化呢？例如进行“filter+map”操作，如何观察 filter 后 map 的原始数据呢？

要想观察使用流操作对列表的各种操作的过程中的数据变化，主要有下面两个办法。

（1）使用peek方法。比如如下代码对数字 $1\sim 10$ 进行了两次过滤，分别是找出大于5的数字和找出偶数，我们通过peek方法把两次过滤操作之前的原始数据保存了下来：

```txt
List<Integer> first Peek = new ArrayList<>();  
List<Integer> second Peek = new ArrayList<>();  
List<Integer> result = IntStream(rangeClosed(1, 10) .boxed() .peek(i -> first Peek.add(i)) .filter(i -> i > 5) .peek(i -> second Peek.add(i)) .filter(i -> i % 2 == 0) .collect(CollectionstoList()); System.out.println("first Peek: " + first Peek); System.out.println("second Peek: " + second Peek); System.out.println("result: " + result); 
```

最后得到输出如下：

```yaml
first Peek: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]  
second Peek: [6, 7, 8, 9, 10]  
result: [6, 8, 10] 
```

可以看到第一次过滤之前是数字 $1\sim 10$ ，第一次过滤后变为6\~10，最终输出6、8、10：

（2）借助IDEA的Stream的追踪功能。具体使用方式可以在搜索引擎搜索“IDEA analyze Java Stream operations”关键字来查看相关文档，效果类似图1-9所示。

![](images/3c67e8639f623314beb54410b35788c78202d401a28f9bea76da4f1c7ca411f0.jpg)  
图1-9IDEA的流操作追踪（StreamTrace）功能

3. Collectors 类提供了很多现成的收集器，那如何实现自定义的收集器呢？例如，实现一个 MostPopularCollector，来得到 List 中出现次数最多的元素，满足下面两个测试用例：

```txt
assertThat Stream.of(1, 1, 2, 2, 3, 4, 5, 5).collect(new MostPopularCollector<>(); get(), is(2));  
assertThat Stream.of('a', 'b', 'c', 'c', 'd').collect(new MostPopularCollector<>(); get(), is('c')); 
```

我的实现思路和方式是，通过一个 HashMap 来保存元素的出现次数，最后在收集的时候找出 Map 中出现次数最多的元素，代码如下：

```java
public class MostPopularCollector<T> implements Collector<T, Map<T, Integer>, Optional<T>> { // 使用 HashMap 保存中间数据 @Override public Supplier<Map<T, Integer>> supplier() { return HashMap::new; } // 每次累计数据则累加值 @Override public BiConsumer<Map<T, Integer>, T> accumulator() {
```

```java
return(acc, elem) -> acc.merge elem, 1, (old, value) -> old + value);  
}  
//合并多个Map就是合并其值  
@override  
public BinaryOperator<T, Integer>> combiner() {  
    return(a, b) -> Streamlining(a.entrySet().stream(), b.entrySet().stream()).collect(CollectionGroupingBy(Map.Entry::getKey, summingInt(Map.Entry::getKey));  
}  
//找出Map中值最大的键  
@override  
public Function<T, Integer>, Optional<T>> finisher() {  
    return(acc) -> combination().stream();  
    .reduce(BinaryOperator.maxBy(Map.Entry.comparingByKey()));  
    .map(Map.Entry::getKey);  
}  
@override  
public Set<Characteristics> characteristics() {  
    return Collections.emptySet();  
} 
```

# 代码篇

本章着重从Java业务代码编写的角度，介绍编码过程中针对Java语言本身及常用框架会遇到的一些坑点。本章涉及以下知识点。

- 多线程相关：ThreadLocal、并发包中的一些工具、加锁、死锁、线程池。  
- I/O 和数据库相关：线程池、连接池、Spring 声明式事务、数据库索引。  
- JDK/JVM相关：包装类型、判等、数值计算、集合操作、I/O操作、日期时间、OOM问题、反射、泛型、注解、日志、异常、空指针问题。  
- Spring 相关：Bean、IoC、AOP、配置、Spring Cloud Feign。

这些知识点不仅仅局限于Java语言和语法本身，还会站在使用Java进行后端业务代码编写的角度讲解其中的一些易错点。其中每一个易错点都有案例有背景，并且会尝试分析问题的根源，最后给出解决方式。在阅读本章的过程中，你还可以学习如何通过引入一些工具来帮助我们定位和解决问题。

# 2.1 使用了并发工具类库，并不等于就没有线程安全问题了

在代码审核讨论时，我们有时会听到有关线程安全和并发工具的一些片面的观点和结论，比如“把 HashMap改为 ConcurrentHashMap，就可以解决并发问题了”“要不我们试试无锁的CopyOnWriteArrayList吧，性能更好”。事实上，这些说法都不太准确。

的确，为了方便开发者进行多线程编程，现代编程语言会提供各种并发工具类。但是，如果没有充分理解它们的使用场景、解决的问题，以及最佳实践，盲目使用就可能会导致一些坑，小则损失性能，大则无法确保多线程情况下业务逻辑的正确性。

需要先说明一下，这里的“并发工具类”是指用来解决多线程环境下并发问题的工具类库。一般而言并发工具包括同步器和并发容器两大类，业务代码中使用并发容器的情况会多一些，所以本节的例子会侧重并发容器。

# 2.1.1 没有意识到线程重用导致用户信息错乱的bug

在生产环境上我们可能会遇到这样一个诡异的问题：有时获取到的用户信息是别人的。查看代码后，发现问题出在使用了 ThreadLocal 来缓存获取到的用户信息。

ThreadLocal适用于变量在线程间隔离，而在方法或类间共享的场景。如果用户信息的获取比较昂贵（例如从数据库查询用户信息），那么在ThreadLocal中缓存数据是比较合适的做法。但是为什么会出现用户信息错乱的bug呢？

下面看一个具体的案例。使用 Spring Boot 创建一个 Web 应用程序，使用 ThreadLocal 存放一个 Integer 的值，来暂且代表需要在线程中保存的用户信息，这个值初始是 null。在业务逻辑中，先从 ThreadLocal 获取一次值，然后把外部传入的参数设置到 ThreadLocal 中，来模拟从当

前上下文获取到用户信息的逻辑，随后再获取一次值，最后输出两次获得的值和线程名称。具体实现代码如下：

```java
private static final ThreadLocal<Integer> currentUser = ThreadLocal.withInitial() -> null);  
@GetMapping("wrong")  
public Map wrong (@RequestParam("UserID") Integer userId) { //设置用户信息之前先查询一次ThreadLocal中的用户信息 String before = Thread.currentThread()..getName() + ": " + currentUser.get(); //设置用户信息到ThreadLocalcurrentUser.setUserId); //设置用户信息之后再查询一次ThreadLocal中的用户信息String after = Thread.currentThread(). getName() + ": " + currentUser.get(); //汇总输出两次查询结果Map result = new HashMap(); result.put("before", before); result.put("after", after); return result; 
```

按理说，在设置用户信息之前第一次获取的值始终应该是 null，但读者要意识到程序运行在 Tomcat 中，执行程序的线程是 Tomcat 的工作线程，而 Tomcat 的工作线程是基于线程池的。顾名思义，线程池会重用固定的几个线程，一旦线程重用，那么很可能第一次从 ThreadLocal 获取的值是之前其他用户的请求遗留的值。这时，ThreadLocal 中的用户信息就是其他用户的信息。

为了更快地重现这个问题，可以在配置文件中设置一下Tomcat的参数，把工作线程池最大线程数设置为1，这样始终是同一个线程在处理请求。

```txt
server.tomcat.max-threads=1 
```

运行程序后先让用户1来请求接口，第一次和第二次获取到的用户ID分别是null和1，如图2-1所示，符合预期。

![](images/e3243f359c807c13349ead023534fde87f4d66a61c3555a36877e55608886a62.jpg)  
图2-1 用户1两次请求接口获取到的用户ID

随后用户2来请求接口，这次就出现了bug，第一和第二次获取到的用户ID分别是1和2，显然第一次获取到了用户1的请求遗留的信息，原因就是Tomcat的线程池重用了线程。从图2-2中可以看到，两次请求的线程都是同一个线程：http-nio-8080-exec-1。

![](images/7be6c84c97fcb36cbcb77f127fe4542f7eb6e7ffe547056d1e20e1331f147c24.jpg)  
图2-2 用户2两次请求接口获取到的用户ID

由此可见，在写业务代码时首先要理解代码会跑在什么线程上。

- 我们可能会抱怨学多线程没用，因为代码里没有开启使用多线程。但其实，可能只是我们

没有意识到，在Tomcat这种Web服务器下跑的业务代码本来就运行在一个多线程环境（否则接口也不可能支持这么高的并发），并不能认为没有显式开启多线程就不会有线程安全问题。

- 因为线程的创建比较昂贵，所以Web服务器往往会使用线程池来处理请求，这就意味着线程会被重用。这时，使用类似ThreadLocal工具来存放一些数据时，需要特别注意在代码运行完后显式地去清空设置的数据。如果在代码中使用了自定义的线程池，同样会遇到这个问题。

修正这段代码的方案是，在代码的finally代码块中显式清除ThreadLocal中的数据。这样新的请求过来即使使用了之前的线程也不会获取到错误的用户信息了。修正后的代码如下：

@GetMapping("right")   
public Map right(@RequestParam("UserID") Integer userId) { String before $=$ Thread.currentThread()..getName() +"::" $^+$ currentUser.get(); currentUser.setUserId); try { String after $=$ Thread.currentThread()..getName() $^+$ ":" $^+$ currentUser.get(); Map result $=$ new HashMap(); result.put("before",before); result.put("after",after); return result; }finally{ //在finally代码块中删除ThreadLocal中的数据，确保数据不串 currentUser.remove(); 1

重新运行程序后，再也不会出现第一次查询用户信息查询到之前用户请求的遗留信息的bug，如图2-3所示。

![](images/d07b608edab0cdf9920a044c79a3763304398c9454cf17cfbe12efa87c3b29cd.jpg)  
图2-3 程序修正后，用户两次请求接口获取到的用户ID

ThreadLocal是利用独占资源的方式，来解决线程安全问题，如果确实需要有资源在线程之前共享，应该怎么办？这时就需要用到线程安全的容器了。

# 2.1.2 使用了线程安全的并发工具，并不代表解决了所有线程安全问题

JDK 1.5 后推出的 ConcurrentHashMap，是一个高性能的线程安全的哈希表容器。“线程安全”这 4 个字特别容易让人误解，因为 ConcurrentHashMap 只能保证提供的原子性读写操作是线程安全的。

我在相当多的业务代码中看到过这个误区，例如下面这个场景。有一个包含900个元素的Map，现在再补充100个元素进去，这个补充操作由10个线程并发进行。开发人员误以为使用了 ConcurrentHashMap 就不会有线程安全问题，于是不假思索地写出了下面的代码：在每一个线程的代码逻辑中先通过 size 方法拿到当前元素数量，计算 ConcurrentHashMap 目前还需要补充多少元素，并在日志中输出这个值，然后通过 putAll 方法把缺少的元素添加进去。

为方便观察问题，我们输出了这个Map一开始和最后的元素个数：

```java
//线程个数
private static int THREAD_COUNT = 10;
//总元素数量
private static int ITEM_COUNT = 1000;
//帮助方法，用来获得一个指定元素数量模拟数据的 ConcurrentHashMap
private ConcurrentHashMap<String, Long>GetData(int count) {
return LongStream.closeClosed(1, count)
 boxed()
collect(Collectors.toConcurrentMap(i -> UUID.randomUUID().substring(), Function identity(), (o1, o2) -> o1, ConcurrentHashMap::new));
}
@GetMapping("wrong")
public String wrong() throws扰乱Exception {
ConcurrentHashMap<String, Long> concurrentHashMap = Data(IDividualCount - 100);
//初始900个元素
log.info("init size:{}", concurrentHashMap.size());
ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
//使用线程池并发处理逻辑
forkJoinPool.execute() -> IntStream.closeClosed(1, 10).parallel().forEach(i -> {
//查询还需要补充多少个元素
int gap = ITEM_COUNT - concurrentHashMap.size();
log.info("gap size:{}", gap);
//补充元素
concurrentHashMap.putAll(getData(gap));
});
//等待所有任务完成
forkJoinPool.shutdown();
forkJoinPool awaitTermination(1, TimeUnit.HOURS);
//最后元素个数会是1000吗
log.info("finish size:{}", concurrentHashMap.size());
return "OK";
} 
```

访问接口后程序输出的日志内容，如图2-4所示

```txt
INFO 18254 --- [nio-8080-exec-1] o.g.t.c.t.ConcurrentHashMapMisuse : init size:900  
INFO 18254 --- [Pool-6-worker-4] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:100  
INFO 18254 --- [Pool-6-worker-6] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:100  
INFO 18254 --- [Pool-6-worker-9] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:100  
INFO 18254 --- [Pool-6-worker-8] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:100  
INFO 18254 --- [ool-6-worker-11] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:100  
INFO 18254 --- [Pool-6-worker-2] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:100  
INFO 18254 --- [Pool-6-worker-1] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:36  
INFO 18254 --- [Pool-6-worker-4] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [Pool-6-worker-4] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [ool-6-worker-13] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:-236  
INFO 18254 --- [nio-8080-exec-1] o.g.t.c.t.ConcurrentHashMapMisuse : finish size:1536 
```

图2-4 访问接口后程序输出的日志

从图2-4中可以看到：

- 初始大小 900 符合预期，还需要填充 100 个元素；  
- 名为 worker-1 的线程查询到当前需要填充的元素个数是 36，竟然还不是 100 的倍数；  
- 名为 worker-13 的线程查询到需要填充的元素数是负的，显然已经过度填充了；  
- 最后 HashMap 的总项目数是 1536，显然不符合填充满 1000 的预期。

针对这个场景，我们可以举一个形象的例子。ConcurrentHashMap 就像是一个大篮子（容器），

现在这个篮子里有900个橘子（元素），我们期望把这个篮子装满1000个橘子，也就是再装100个橘子。有10个工人（工作线程）来干这件事儿，大家先后到岗后会计算还需要补多少个橘子进去，最后把橘子装入篮子。

ConcurrentHashMap 这个篮子本身，可以确保多个工人在装东西进去时，不会相互影响干扰，但无法确保工人A看到还需要装100个橘子但是还未装的时候，工人B就看不到篮子中的橘子数量。更值得注意的是，往这个篮子装100个橘子的操作不是原子性的，在别人看来可能会有一个瞬间篮子里有964个橘子，还需要补36个橘子。

回到 ConcurrentHashMap，我们需要注意 ConcurrentHashMap 对外提供的方法或能力的限制。

- 使用了 ConcurrentHashMap，不代表对它的多个操作之间的状态是一致的，是没有其他线程在操作它的，如果需要确保需要手动加锁。  
- size、isEmpty 和 containsValue 等聚合方法，在并发情况下可能会反映 ConcurrentHashMap 的中间状态。因此在并发情况下，这些方法的返回值只能用作参考，而不能用于流程控制。显然，利用 size 方法计算差异值，是一个流程控制。  
- 诸如putAll这样的聚合方法也不能确保原子性，在putAll的过程中去获取数据可能会获取到部分数据。

代码的修改方案很简单，给整段逻辑加锁即可：

@GetMapping("right")   
public String right() throws InterruptedException { ConcurrentHashMap<String, Long> concurrentHashMap $=$ GetDataITEM_COUNT-100); log.info("init size:{}",concurrentHashMap.size()); ForkJoinPool forkJoinPool $=$ new ForkJoinPool(THREAD_COUNT); forkJoinPool.execue（（） $\rightharpoondown$ IntStream rangeClosed(1，10).parallel().forEach(i->{ //下面的这段复合逻辑需要锁一下这个ConcurrentHashMap synchronized (concurrentHashMap){ int gap $=$ ITEM_COUNT- concurrentHashMap.size(); log.info("gap size:{}",gap); concurrentHashMap.putAll(getData(gap)); } ））； forkJoinPool.shutdown(); forkJoinPool awaitTermination(1，TimeUnit.HOURS); log.info("finish size:{}",concurrentHashMap.size()); return "OK";

重新调用接口，程序的日志输出如图2-5所示，符合预期。

```txt
INFO 18254 --- [nio-8080-exec-1] o.g.t.c.t.ConcurrentHashMapMisuse : init size:900  
INFO 18254 --- [Pool-7-worker-9] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:100  
INFO 18254 --- [ool-7-worker-15] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [Pool-7-worker-8] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [Pool-7-worker-4] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [Pool-7-worker-9] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [Pool-7-worker-1] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [ool-7-worker-13] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [Pool-7-worker-2] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [Pool-7-worker-6] o.g.t.c.t.ConcurrentHashMapMisuse : gap size:0  
INFO 18254 --- [nio-8080-exec-1] o.g.t.c.t.ConcurrentHashMapMisuse : finish size:1000 
```

图2-5 整段逻辑加锁后，程序输出的日志

从图中可以看到，只有一个线程查询到了需要补100个元素，其他9个线程查询到不需要补元素，最后Map大小为1000。那么，使用ConcurrentHashMap全程加锁，是不是还不如使用普通的HashMap呢？

不是的。ConcurrentHashMap 提供了一些原子性的简单复合逻辑方法，用好这些方法就可以发挥其威力。这就引申出代码中另一个常见的问题：在使用一些类库提供的高级工具类时，开发人员可能还是按照旧的方式去使用这些新类，因为没有使用其特性，所以无法发挥其威力。

# 2.1.3 没有充分了解并发工具的特性，从而无法发挥其威力

下面是一个使用Map来统计键出现次数的场景，这个逻辑在业务代码中非常常见。

- 使用 ConcurrentHashMap 来统计，键的范围是 10。  
- 使用最多10个并发，循环操作1000万次，每次操作累加随机的键。  
- 如果键不存在的话，首次设置值为1。

具体代码如下：

```java
//循环次数
private static int LOOP_COUNT = 10000000;
//线程数量
private static int THREAD_COUNT = 10;
//元素数量
private static int ITEM_COUNT = 10;
private Map<String, Long>正常使用() throws InterruptedException {
    ConcurrentHashMap<String, Long> freqs = new ConcurrentHashMap<(ITEM_COUNT);
    ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
    forkJoinPool.execute()( -> IntStream(rangeClosed(1, LOOP_COUNT).parallel());
    forEach(i -> {
        //获得一个随机的键
        String key = "item" + ThreadLocalRandom.current().NEXT(ITEM_COUNT);
        synchronized (freqs) {
            if (freqs.containsKey(key)) {
                //键存在则+1
                freqs.put(key, freqs.get(key) + 1);
            } else {
                //键不存在则初始化为1
                freqs.put(key, 1L);
            }
        }
    })};
    forkJoinPool.shutdown();
    forkJoinPool awaitTermination(1, TimeUnit.HOURS);
    return freqs;
} 
```

吸取之前的教训，直接通过锁的方式锁住Map，然后做判断、读取现在的累计值、加1、保存累加后值的逻辑。这段代码在功能上没有问题，但无法充分发挥ConcurrentHashMap的威力，改进后的代码如下：

```java
private Map<String, Long> gooduse() throws扰乱Exception {
    ConcurrentHashMap<String, LongAdder> freqs = new ConcurrentHashMap<(ITEM_COUNT);
    ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
    forkJoinPool.execute()( -> IntStream rangeClosed(1, LOOP_COUNT).parallel());
    forEach(i -> {
        String key = "item" + ThreadLocalRandom.current().NEXT(ITEM_COUNT); 
```

```txt
//利用computeIfAbsent()方法实例化LongAdder，利用LongAdder进行线程安全计数  
freqs.computeIfAbsent(key，k->new LongAdder().increment();  
}）；  
forkJoinPool.shutdown();  
forkJoinPool awaitTermination(1，TimeUnit.HOURS);  
//因为我们的值是LongAdder而不是Long，所以需要做一次转换才能返回  
return freqs.entrySet().stream().collect(Collectors.map(e->e.getKey()，e->e.getValue().longValue()))；
```

在这段改进后的代码中，我们巧妙地利用了如下两点。

- 使用 ConcurrentHashMap 的原子性方法 computeIfAbsent 来做复合逻辑操作，判断键是否存在值，如果不存在则把 Lambda 表达式运行后的结果放入 Map 作为值，也就是新创建一个 LongAdder 对象，最后返回值。  
- 由于 computeIfAbsent 方法返回的值是 LongAdder，是一个线程安全的累加器，因此可以直接调用其 increment 方法进行累加。

这样在确保线程安全的情况下达到了极致性能，把本节一开始forEach循环中那段7行的代码替换为1行了。下面通过一个简单的测试比较一下修改前后两段代码的性能：

GetMapping("good")   
public String good() throws扰乱Exception { StopWatch stopWatch $=$ new StopWatch(); stopWatch.start("normaluse"); Map<String, Long> normaluse $=$ normaluse(); stopWatch.stop(); //校验元素数量 Assert.isTrue(normaluse.size() $= =$ ITEM_COUNT, "normaluse size error"); //校验累计总数 Assert.isTrue(normaluse entrySet().stream() .mapToLong(item->item.getValue()) .reduce(0, Long::sum) $= =$ LOOP_COUNT, "normaluse count error"); stopWatch.start("gooduse"); Map<String, Long> gooduse $=$ gooduse(); stopWatch.stop(); Assert.isTrue(gooduse.size() $= =$ ITEM_COUNT, "gooduse size error"); Assert.isTrue(gooduse entrySet().stream() .mapToLong(item->item.getValue()) .reduce(0, Long::sum) $= =$ LOOP_COUNT, "gooduse count error"); log.info(stopWatchprettyPrint()); return "OK";

这段测试代码并无特殊之处，使用StopWatch来测试两段代码的性能（需要说明的是，为了更

简单，本书使用StopWatch进行了性能测试。如果需要进行更规范和更微观的微基准测试，需要使用JMH（Java Microbenchmark Harness）等专业工具）。最后跟了一个断言来判断Map中元素的个数及所有值的和是否符合预期来校验代码的正确性。测试结果如图2-6所示

![](images/36301c8a85cfa576fae46e1170d516827a728bbc767b3cde9fda0a20ec869b7b.jpg)  
图2-6 修改前后两段代码的性能对比

可以看到，优化后的代码，与使用锁来操作 ConcurrentHashMap 的方式相比，性能提升了 10 倍。  
computeIfAbsent 为什么如此高效呢？

答案就在源码最核心的部分，也就是Java自带的Unsafe实现的CAS。它在虚拟机层面确保了写入数据的原子性，比加锁的效率高得多：

static final $<  \mathrm{K},\mathrm{V}>$ boolean casTabAt(Node $<  \mathrm{K},\mathrm{V}>$ [] tab,int i, Node $<  \mathrm{K},\mathrm{V}>$ c,Node $<  \mathrm{K},\mathrm{V}>$ v){ return U compareAndSetObject(table，((long)i<<ASHIFT)+ABASE,c,v); }

像 ConcurrentHashMap 这样的高级并发工具的确提供了一些高级 API，只有充分了解其特性才能最大化其威力，而不能因为其足够高级、酷炫盲目使用。

# 2.1.4 没有认清并发工具的使用场景，因而导致性能问题

除了 ConcurrentHashMap 这样通用的并发工具类，java.util.concurrent 工具包中还有些针对特殊场景实现的生面孔。一般来说，针对通用场景的通用解决方案，在所有场景下性能都还可以，属于“万金油”；而针对特殊场景的特殊实现，会有比通用解决方案更高的性能，但一定要在它针对的场景下使用，否则可能会产生性能问题甚至是 bug。

之前在排查一个生产性能问题时，我发现一段简单的非数据库操作的业务逻辑消耗了超出预期的时间，在修改数据时操作本地缓存比回写数据库慢许多。查看代码发现，开发人员使用了CopyOnWriteArrayList来缓存大量的数据，而数据变化又比较频繁。

CopyOnWrite 是一个时髦的技术，不管是 Linux 还是 Redis 都会用到。在 Java 中，CopyOnWriteArrayList 虽然是一个线程安全的 ArrayList，但因为其实现方式是，每次修改数据时都会复制一份数据出来，所以有明显的适用场景，即读多写少或者希望无锁读的场景。

如果要使用CopyOnWriteArrayList，那一定是因为场景需要而不是因为足够酷炫。如果读写比例均衡或者有大量写操作的话，使用CopyOnWriteArrayList的性能会非常糟糕。下面通过一段测试代码来比较一下使用CopyOnWriteArrayList和普通加锁方式ArrayList的读写性能：

//测试并发写的性能  
```java
GetMapping("write")   
public Map testWrite() { List<Integer> copyOnWriteArrayList = new CopyOnWriteArrayList<>(); List：<Integer> synchronizedList = Collections.synchronizedList(new ArrayList<>()); StopWatch stopWatch = new StopWatch(); int loopCount = 100000; stopWatch.start("Write:copyOnWriteArrayList"); //循环10万次并发往CopyOnWriteArrayList写入随机元素 IntStream.closeClosed(1,loopCount).parallel().forEach(_ ->copyOnWriteArray List.add( ThreadLocalRandom.current().NEXT( loopCount ))); stopWatch.stop(); stopWatch.start("Write:synchronizedList"); //循环10万次并发往加锁的ArrayList写入随机元素 IntStream.closeClosed(1,loopCount).parallel().forEach(_ -> synchronizedList. add( ThreadLocalRandom.current().NEXT( loopCount ))); stopWatch.stop(); log.info(stopWatchprettyPrint()); Map result = new HashMap(); result.put("copyOnWriteArrayList",copyOnWriteArrayList.size()); result.put("synchronizedList", synchronizedList.size()); return result; } 
```

```java
//帮助方法用来填充List
private void addAll(List<Integer> list) {
    list.addAll(IntStream范围内(1, 1000000).boxed().collect(Collectors.list));
}
//测试并发读的性能
@GetMapping("read")
public Map testRead() {
    //创建两个测试对象
    List<Integer> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
    List<Integer> synchronizedList = Collectionsious.synchronizedList(new ArrayList());
    //填充数据
    addAll(copyOnWriteArrayList);
    addAll(synchronizedList);
    StopWatch stopWatch = new StopWatch();
    int loopCount = 1000000;
    int count = copyOnWriteArrayList.size();
    stopWatch.start("Read:copyOnWriteArrayList");
    //循环100万次并发从CopyOnWriteArrayList随机查询元素
    IntStream范围内(1, loopCount).parallel().forEach(_ -> copyOnWriteArrayList.get( ThreadLocalRandom.current().NEXT(count));
    stopWatch.stop();
    stopWatch.start("Read:synchronizedList");
    //循环100万次并发从加锁的ArrayList随机查询元素
    IntStream范围内(0, loopCount).parallel().forEach(_ -> synchronizedList.get( ThreadLocalRandom.current().NEXT(count));
    stopWatch.stop();
    log.info(stopWatchprettyPrint());
    Map result = new HashMap();
    result.put("copyOnWriteArrayList", copyOnWriteArrayList.size());
    result.put("synchronizedList", synchronizedList.size());
    return result;
} 
```

在这段代码中我们针对并发读和并发写分别写了一个测试方法，测试两者一定次数的写或读操作的耗时。在大量写的场景（10万次add操作）下，CopyOnWriteArrayList的性能几乎是同步的ArrayList的1/100，如图2-7所示。

![](images/67bc731e643537ca65d2a03968acb123d058e8bc7088e45976414495c63bd67b.jpg)  
图2-7在大量写的场景下，CopyOnWriteArrayList和ArrayList的性能对比

在大量读的场景下（100万次get操作），CopyOnWriteArrayList又比同步的ArrayList快5倍以上，如图2-8所示。

![](images/681f069e5de5e9a0eb5dcab9e8d6ffc0fa60f4b17be23ad65e6df6b7d125f619.jpg)  
图2-8在大量读的场景下，CopyOnWriteArrayList和ArrayList的性能对比

为什么在大量写的场景下，CopyOnWriteArrayList会这么慢呢？答案就在源码中。以add方法为例，每次add时都会用Arrays.copyOf创建一个新数组，频繁add时内存的申请释放消耗会很大：

/\*\*   
\* Appends the specified element to the end of this list.   
\*   
\* @param e element to be appended to this list   
\* @return \{@code true} (as specified by \{@link Collection#add\})   
\*/   
public boolean add(E e) { synchronized (lock) { Object[] elements $=$ getArray(); int len $=$ elements.length; Object[] newElements $=$ Arrays.copyOfelements, len + 1); newElements[len] $= \mathrm{e}$ . setArray(newElements); return true; }

# 2.1.5 小结

在使用并发工具类库的时候，有下面4类常见的坑。

- 只知道使用并发工具，并不清楚当前线程的来龙去脉，解决多线程问题却不了解线程。例如，使用 ThreadLocal 来缓存数据，认为 ThreadLocal 在线程之间做了隔离不会有线程安全问题，没想到线程重用导致数据串了。请务必记得，在业务逻辑结束之前清理 ThreadLocal 中的数据。  
- 误以为使用了并发工具就可以解决一切线程安全问题，期望通过把线程不安全的类替换为线程安全的类来一键解决问题。例如，认为使用了 ConcurrentHashMap 就可以解决线程安全问题，没对复合逻辑加锁导致业务逻辑错误。如果希望在一整段业务逻辑中对容器的操作都保持整体一致性的话，需要加锁处理。  
- 没有充分了解并发工具的特性，还是按照老方式使用新工具导致无法发挥其性能。例如，使用了 ConcurrentHashMap，但没有充分利用其提供的基于 CAS 安全的方法，还是使用锁的方式来实现逻辑。你可以阅读 ConcurrentHashMap 的文档，看一下相关原子性操作 API 是否可以满足业务需求，如果可以则优先考虑使用。  
- 没有掌握工具的适用场景，在不合适的场景下使用了错误的工具导致性能更差。例如，没有理解CopyOnWriteArrayList的适用场景，把它用在了读写均衡或者大量写操作的场景下，导致性能问题。对于这种场景，你可以考虑使用普通的List。

这4类坑容易踩到的原因可以归结为，在使用并发工具时，并没有充分理解其可能存在的问题、适用场景等。我还有两点建议。

- 一定要认真阅读官方文档（如Oracle JDK文档）。充分阅读官方文档，理解工具的适用场景及其API的用法，并做一些小实验。了解之后再去使用，就可以避开大部分坑。  
- 如果代码运行在多线程环境下，那么就会有并发问题，并发问题不那么容易重现，可能需要使用压力测试模拟并发场景，来发现其中的bug或性能问题。

# 2.1.6 思考与讨论

1. 可以把 ThreadLocalRandom 的实例设置到静态变量中，在多线程情况下重用吗？

答案是不能。ThreadLocalRandom文档里有这么一条：

Usages of this class should typically be of the form: ThreadLocalRandom.current().nextX(...) (where X is Int, Long, etc). When all usages are of this form, it is never possible to accidentally share a ThreadLocalRandom across multiple threads.

为什么规定要这样使用 ThreadLocalRandom.current().nextX(...) 呢？

current() 的时候初始化一个初始化种子到线程，每次 nextseed 再使用之前的种子生成新的种子：

```txt
UNSAFE.putLong(t = Thread.currentThread(), SEED, r = UNSAFE.getLong(t, SEED) + GAMMA); 
```

如果通过主线程调用一次 current 生成一个 ThreadLocalRandom 的实例保存起来，那么其他线程来获取种子的时候必然取不到初始种子，必须是每一个线程自己用的时候初始化一个种子到线程。读者可以在 nextSeed 方法上设置一个断点来测试：

```txt
UNSAFE.getLong Thread.currentThread(),SEED); 
```

# 2. ConcurrentHashMap 的 putIfAbsent 方法和 computelfAbsent 方法有什么区别？

computeIfAbsent 方法和 putIfAbsent 方法都是判断值不存在时为 Map 赋值的原子方法，它们的区别包括如下 3 点。

- 当键存在时，如果值的获取比较昂贵的话，putIfAbsent 方法就会白白浪费时间在获取这个昂贵的值上（这个点特别注意），而 computeIfAbsent 方法则会因为传入的是 Lambda 表达式而不是实际值不会有这个问题。  
- 当键不存在时，putIfAbsent方法会返回null，这时要小心空指针的问题；而computeIfAbsent方法会返回计算后的值，不存在空指针的问题。  
- 当键不存在的时候，putIfAbsent方法允许存放null进去，而computeIfAbsent方法不能（当然，此条针对 HashMap，ConcurrentHashMap方法不允许存放null进去）。

本书配套代码的 ciavspia 目录中提供了一段代码来验证这 3 点，读者可以自行查看。

# 2.2 代码加锁：不要让锁成为烦心事

锁是缓解线程安全问题的另一种重要手段，本节将讲解其在使用上的常见坑点。

我曾见过一个有趣的案例。某天，一位开发人员在群里说：“不可思议，好像遇到了一个JVM的bug。”紧接着他贴出了这样一段代码：

@Slf4j   
public class Interesting { volatile int a $= 1$ . volatile int b $= 1$ public void add() { log.info("add start"); for (int i $= 0$ ;i $<  10000$ ; $\mathrm{i + + }$ ）{ a++; b++; } log.info("add done"); } public void compare() { log.info("compare start");

for (int i = 0; i < 10000; i++) { //a始终等于b吗 if $(\mathbf{a} <   \mathbf{b})$ { log.info("a:{},b:{},{}","a,b,a $>$ b); //最后的a>b应该始终是false吗 } } log.info("compare done"); }

这段代码的大致逻辑是，Interesting类里有两个int类型的字段a和b，add方法循环1万次对a和b进行 $++$ 操作，另一个compare方法同样循环1万次判断a是否小于b，条件成立就打印a和b的值，并判断 $a > b$ 是否成立。他新建了两个线程来分别执行add和compare方法：

```txt
Interesting interesting = new Interesting();  
new Thread() -> interesting.add().start();  
new Thread() -> interestingcompare().start(); 
```

按道理，a和b同样进行累加操作应该始终相等，compare方法中的第一次判断应该始终不会成立，不会输出任何日志。但是，执行代码后发现不但输出了日志，而且更诡异的是，compare方法在判断 $a < b$ 成立的情况下还输出了 $a > b$ 也成立，如图2-9所示。

```powershell
[Thread-30] [INFO ] [o.g.t.c.lock.demol.Interesting :12 ] - add start  
[Thread-31] [INFO ] [o.g.t.c.lock.demol.Interesting :21 ] - compare start  
[Thread-31] [INFO ] [o.g.t.c.lock.demol.Interesting :24 ] - a:5670,b:5678,false  
[Thread-30] [INFO ] [o.g.t.c.lock.demol.Interesting :17 ] - add done  
[Thread-31] [INFO ] [o.g.t.c.lock.demol.Interesting :24 ] - a:7907,b:7913,false  
[Thread-31] [INFO ] [o.g.t.c.lock.demol.Interesting :28 ] - compare done 
```

图2-9 疑似JVM的bug的代码段输出

群里一位开发人员看到这个问题，说：“这哪是JVM的bug，分明是线程安全问题。很明显这是在操作两个字段a和b，有线程安全问题，应该为add方法加上锁，确保a和b的++是原子性的，就不会错乱了。”随后，他为add方法加上了锁：

```txt
public synchronized void add() 
```

但是，加锁后问题并没有解决。

为什么？我们先细想一下，为什么锁可以解决线程安全问题。因为只有一个线程可以拿到锁，所以加锁后的代码中的资源操作是线程安全的。但是，这个案例中的add方法始终只有一个线程在操作，显然只为add方法加锁是没用的。

之所以出现这种错乱，是因为两个线程是交错执行add和compare方法中的业务逻辑，而且这些业务逻辑不是原子性的：a++和b++操作可以穿插在compare方法的比较代码中。更加需要注意的是，a<b这种比较操作在字节码层面是加载a、加载b和比较3步，代码虽然是一行但也不是原子性的。

正确的做法应该是，为add和compare都加上方法锁，确保add方法执行时，compare无法读取a和b：

```txt
public synchronized void add() public synchronized void compare() 
```

所以使用锁解决问题之前一定要厘清，我们要保护的是什么逻辑，多线程执行的情况又是怎样的。

# 2.2.1 加锁前要清楚锁和被保护的对象是不是一个层面的

除了没有分析清楚线程、业务逻辑和锁三者之间的关系随意添加无效的方法锁，还有一种比较常见的错误是，没有厘清锁和要保护的对象是不是一个层面的。静态字段属于类，只有类级别的锁才能保护；而非静态字段属于类实例，实例级别的锁就可以保护。

先看看这段代码有什么问题：在类 Data 中定义了一个静态的 int 字段 counter 和一个非静态的 wrong 方法，实现 counter 字段的累加操作：

```java
class Data {
    @Setter
    private static int counter = 0;
    public static int reset() {
        counter = 0;
        return counter;
    }
    public synchronized void wrong() {
        counter++;
    }
} 
```

写一段代码测试下：

@GetMapping("wrong")   
public int wrong(@RequestParam(value $=$ "count", defaultValue $=$ "1000000") int count){ Data.reset(); //多线程循环一定次数调用Data类不同实例的wrong方法 IntStream.closeClosed(1,count).parallel().forEach(i->newData().wrong()); return Data.getCounter();   
}

因为默认运行100万次，所以执行后应该输出100万，但页面输出的是639242，如图2-10所示。

![](images/47dffdf5e97f7381ddcaa50f72ff82148b3fc1ec1f4b29ce40715e14b2db99d4.jpg)  
图2-10 锁不住的问题

为什么会出现这个问题？

在非静态的 wrong 方法上加锁，只能确保多个线程无法执行同一个实例的 wrong 方法，却不能保证不会执行不同实例的 wrong 方法。而静态的 counter 在多个实例中共享，所以必然会出现线程安全问题。厘清思路后，修正方法就很清晰了：在类中定义一个 Object 类型的静态字段，并在操作 counter 之前对这个字段加锁。具体实现代码如下：

```java
class Data {
    @Setter
    private static int counter = 0;
    private static Object locker = new Object();
    public void right() {
        synchronized (locker) {
            counter++;
        }
    }
} 
```

```txt
} 
```

那么，把wrong方法定义为静态是不是就可以了，这个时候锁是类级别的。可以，但我们不可能为了解决线程安全问题而改变代码结构，把实例方法改为静态方法。我们还可以从字节码和JVM的层面继续探索一下，代码块级别的synchronized和方法上标记synchronized关键字，在实现上有什么区别。

# 2.2.2 加锁要考虑锁的粒度和场景问题

在方法上加 synchronized 关键字实现加锁确实简单，我曾看到一些业务代码中几乎所有方法都加了 synchronized。滥用 synchronized 有如下问题。

- 没必要。通常情况下 $60\%$ 的业务代码是三层架构的，数据经过无状态的 Controller、Service 和 Repository 流转到数据库，没必要使用 synchronized 来保护什么数据。  
- 可能会极大地降低性能。使用 Spring 框架时，默认情况下 Controller、Service 和 Repository 是单例的，加上 synchronized 会导致整个程序几乎就只能支持单线程，造成极大的性能问题。

即使我们确实有一些共享资源需要保护，也要尽可能降低锁的粒度，仅对必要的代码块甚至是需要保护的资源本身加锁。例如，业务代码中，有一个ArrayList因为会被多个线程操作而需要保护，又有一段比较耗时的操作（代码中的slow方法）不涉及线程安全问题，应该如何加锁呢？错误的做法是，给整段业务逻辑加锁，把slow方法和操作ArrayList的代码同时纳入synchronized代码块。更合适的做法是，把加锁的粒度降到最低，只在操作ArrayList的时候给它加锁。具体代码如下：

```java
private List<Integer> data = new ArrayList<>(); 
```

//不涉及共享资源的慢方法  
```java
private void slow() { try { TimeUnit.MILLISECONDS.sleep(10); } catch (InterruptedException e) { } 
```

//错误的加锁方法  
@GetMapping("wrong")   
public int wrong() { long begin $=$ System.currentTimeMillis(); InputStream.close(1，1000).parallel().forEach(i->{ //加锁粒度太粗了 synchronized (this){ slow(); data.add(i); } ）； log.info("took:{}”，System.currentTimeMillis() - begin); return data.size();

//正确的加锁方法  
@GetMapping("right")   
public int right(){ long begin $=$ System.currentTimeMillis();

```javascript
IntStream(rangeClosed(1, 1000).parallel().forEach(i -> {
    slow();
    // 只对ArrayList加锁
    synchronized (data) {
        data.add(i);
    }
});
```

执行这段代码，同样是1000次业务操作，正确加锁的代码耗时约 $1.4\mathrm{s}$ ，而对整个业务逻辑加锁的代码耗时约 $11\mathrm{s}$ ，如图2-11所示。

```txt
[http-nio-45678-exec-1] [INFO] [.g.t.c.l.d.LockGranularityController:36 ] - took:11145  
[http-nio-45678-exec-3] [INFO] [.g.t.c.l.d.LockGranularityController:49 ] - took:1403 
```

图2-11 锁粒度太粗的问题

如果精细化考虑了锁的应用范围后性能还无法满足需求的话，我们就要考虑另一个维度的粒度问题了：区分读写场景以及资源的访问冲突，考虑使用悲观方式的锁还是乐观方式的锁。

在一般业务代码中，很少需要进一步考虑这两种更细粒度的锁，所以我只提供几个大概的结论，你可以根据需求来考虑是否有必要进一步优化。

- 对于读写比例差异明显的场景，考虑使用ReentrantReadWriteLock细化区分读写锁，来提高性能。  
- 如果你的 JDK 版本高于 1.8、共享资源的冲突概率也没那么大，考虑使用 StampedLock 的乐观读的特性，进一步提高性能。  
- JDK 里 ReentrantLock 和 ReentrantReadWriteLock 都提供了公平锁的版本，在没有明确需求的情况下不要轻易开启公平锁特性。在任务很轻的情况下开启公平锁可能会让性能下降至 $1\%$ 左右。

# 2.2.3 多把锁要小心死锁问题

锁的粒度够用就好，意味着程序逻辑中有时会存在一些细粒度的锁。但一个业务逻辑如果涉及多把锁，容易产生死锁问题。

我曾遇到过这样一个案例：下单操作需要锁定订单中多个商品的库存，拿到所有商品的锁之后进行下单扣减库存操作，全部操作完成之后释放所有的锁。代码上线后发现，下单失败概率很高，失败后需要用户重新下单，极大地影响了用户体验，进而影响了销量。

经排查发现是死锁引起的问题，背后原因是扣减库存的顺序不同导致并发的情况下多个线程可能相互持有部分商品的锁，又等待其他线程释放另一部分商品的锁，于是出现了死锁问题。我们剖析一下核心的业务代码。

首先，定义一个商品类型，包含商品名、库存剩余和商品的库存锁3个属性，每一种商品默认库存1000个；其次，初始化10个这样的商品对象来模拟商品清单。

```java
@Data
@RequiredArgsConstructor
static class Item {
    final String name; //商品名
    int remaining = 1000; //库存剩余
    @ToStringExclude //ToString不包含这个字段
    ReentrantLock lock = new ReentrantLock(); //商品的库存锁 
```

再次，写一个方法模拟在购物车进行商品选购，每次从商品清单（items字段）中随机选购3个商品（为了简化逻辑，我们不考虑每次选购多个同类商品的逻辑，购物车中不体现商品数量）：

```java
private List<item> createCart() { return IntStream.close(1, 3) .mapToObj(i -> "item" + ThreadLocalRandom.current().NEXT(items.size())) .map(name -> items.get(name)).collect(Collectors.list()); } 
```

在下单代码中首先声明一个List来保存所有获得的锁，然后遍历购物车中的商品依次尝试获得商品的锁，最长等待10s，获得全部锁之后再扣减库存；如果有无法获得锁的情况，则解锁之前获得的所有锁，返回false下单失败。

```java
private boolean createOrder(List<item> order) { //存放所有获得的锁 List<ReentrantLock> locks = new ArrayList<>(); for(Item item : order) { try { //获得锁10s超时 if (item.lock.tryLock(10, TimeUnit.SECONDS)) { locks.add(item.lock); } else { locks.forEach(ReentrantLock::unlock); return false; } catch (InterruptedException e) { } } //锁全部拿到之后执行扣减库存业务逻辑 try{ order.forEach(item -> itemremaining--); }finally{ locks.forEach(ReentrantLock::unlock); } return true; } 
```

接下来写一段代码测试这个下单操作。模拟在多线程情况下进行100次创建购物车和下单操作，最后通过日志输出成功的下单次数、总剩余的商品个数、100次下单耗时，以及下单完成后的商品库存明细。

@GetMapping("wrong")   
public long wrong() { long begin $=$ System.currentTimeMillis(); //并发进行100次下单操作，统计成功的下单次数 long success $=$ IntStream-CNqed(1,100).parallel() .mapToObj(i->{ List<item> cart $\equiv$ createCart(); return createOrder cart); 1） .filter(result->result) .count(); log.info("success:{}totalRemaining:{}took:{}ms items:{}", success, items entrySet().stream().map(item->item.getValue().remaining).reduce(0, Integer::sum), SystemcurrentTimeMillis(-begin,items);

```txt
return success; 
```

运行程序，输出图2-12所示的日志。

```json
[2019-12-01 14:17:53.674] [http-nio-45678-exec-1] [INFO] [.g.t.c.lock demo3DeadLockController:73 ] - success:65 totalRemaining:9805 took:50031ms items:item0=DeadLockController.Item(name=Item0, remaining=974), item2=DeadLockController.Item(name=Item2, remaining=985), item1=DeadLockController.Item(name=Item1, remaining=984), item8=DeadLockController.Item(name=Item8, remaining=984), item7=DeadLockController.Item(name=Item7, remaining=969), item9=DeadLockController.Item(name=Item9, remaining=987), item4=DeadLockController.Item(name=Item4, remaining=986), item3=DeadLockController.Item(name=Item3, remaining=979), item6=DeadLockController.Item(name=Item6, remaining=984), item5=DeadLockController.Item(name=Item5, remaining=973)] 
```

图2-12 死锁导致的下单失败

从图中可以看到，100次下单操作成功了65次，10种商品总计10000件，库存剩余总计为9805件，消耗了195件，符合预期（65次下单成功，每次下单包含3件商品），总耗时 $50\mathrm{s}$ 。

为什么会这样呢？使用JDK自带的VisualVM工具来跟踪一下，重新执行方法后不久就可以看到，线程Tab中提示了死锁问题，根据提示点击右侧线程Dump按钮进行线程抓取操作，如图2-13所示。

![](images/7c8a2a345257f890ce22487420a61b295730ba5be3b736140b11ea7f00b73444.jpg)  
图2-14 死锁相关的日志

图2-13 通过VisualVM来探查死锁并进行线程Dump

查看抓取出的线程栈，在页面中部可以看到如图2-14所示的日志。

```txt
Found one Java-level deadlock:
>>>   
"ForkJoinPool.commonPool-worker-6": waiting for ownable synchronizer 0x00000076d595788, (a java.util.concurrent.locks.ReentrantLock\\(NonfairSync), which is held by "ForkJoinPool.commonPool-worker-4"   
"ForkJoinPool.commonPool-worker-4": waiting for ownable synchronizer 0x00000076d596318, (a java.util.concurrent.locks.ReentrantLock\\)NonfairSync), which is held by "ForkJoinPool.commonPool-worker-3"   
"ForkJoinPool.commonPool-worker-3": waiting for ownable synchronizer 0x00000076d596048, (a java.util.concurrent.locks.ReentrantLock\\)NonfairSync), which is held by "ForkJoinPool.commonPool-worker-4" 
```

显然是出现了死锁，线程4在等待的一把锁被线程3持有，线程3在等待的另一把锁被线程4持有。为什么会有死锁问题呢？仔细回忆一下购物车添加商品的逻辑：随机添加了3种商品，假设一个购物车中的商品是item1和item2，另一个购物车中的商品是item2和item1，一个线程先获取到了item1的锁，同时另一个线程获取到了item2的锁，然后两个线程接下来要分别获取item2和item1的锁，这个时候锁已经被对方获取了，只能相互等待一直到10s超时。其实，避免死锁的方案很简单，为购物车中的商品排一下序，让所有的线程一定是先获取item1的锁再获取item2的锁。这样只需要修改一行代码，对createCart获得的购物车按照商品名进行排序即可：

@GetMapping("right")   
public long right() { long success $=$ IntStream.closeClosed(1, 100).parallel() .mapToObj(i->{ List<item> cart $=$ createCart().stream() .sorted(Comparator.comparing(Item::getName)) .collect(Collectors.list()); return createOrder(cart);

```javascript
} .filter(result->result) .count(); return success; 
```

测试一下 right 方法，不管执行多少次都是 100 次成功下单，而且性能相当高，事务处理能力达到 3000TPS 以上，如图 2-15 所示。

![](images/9158a3d9ecc28ccfab8a7e43fb921d18182947d7116e5e0eb394a951c5d8c36f.jpg)  
图2-15 使用wrk工具压测改进后的代码

在这个案例中，虽然产生了死锁问题，但是因为尝试获取锁的操作并不是无限阻塞的，所以没有造成永久死锁，之后的改进方案就是避免循环等待，通过对购物车的商品进行排序来实现有顺序的加锁。

# 2.2.4 小结

使用锁来解决多线程情况下线程安全问题的坑，需要重点关注如下3点。

- 使用 synchronized 加锁虽然简单，但首先要弄清楚共享资源是类还是实例级别的、会被哪些线程操作，synchronized 关联的锁对象或方法又是什么范围的。  
- 加锁尽可能要考虑粒度和场景，锁保护的代码意味着无法进行多线程操作。对于Web类型的天然多线程的项目，对方法进行大范围加锁会显著降低并发能力，要考虑尽可能地只为必要的代码块加锁，降低锁的粒度；而对于要求超高性能的业务，还要细化考虑锁的读写场景，以及悲观方式的锁优先还是乐观方式的锁优先，尽可能针对明确场景精细化加锁方案，可以在适当的场景下考虑使用ReentrantReadWriteLock、StampedLock等高级的锁工具类。  
- 业务逻辑中有多把锁时要考虑死锁问题，通常的规避方案是，避免无限等待和循环等待。

此外，如果业务逻辑中锁的实现比较复杂，要仔细看看加锁和释放是否配对，是否有遗漏释放或重复释放的可能性；并且对于分布式锁要考虑锁自动超时释放了而业务逻辑却还在进行的情况，如果别的线程或进程拿到了相同的锁，可能会导致重复执行。

为了方便演示，本节的案例是在Controller的逻辑中创建新的线程或使用线程池进行并发模拟，我们当然可以意识到哪些对象是并发操作的，但是对于Web应用程序的天然多线程场景，我们可能更容易忽略这点，并且也可能因为误用锁降低应用整体的吞吐量。如果你的业务代码涉及复杂的锁操作，强烈建议Mock相关外部接口或数据库操作后对应用代码进行压测，通过压测排除锁误用带来的性能问题和死锁问题。

# 2.2.5 思考与讨论

1. 我曾遇到过这样一个坑：开启了一个线程无限循环来运行一些任务，并用一个 bool 类型的变量来控制循环的退出，默认为 true 代表执行，一段时间后主线程将这个变量设置为了

false。如果这个变量不是 volatile 修饰的，子线程可以退出吗？

答案是不能退出。例如，在下面的代码中，3s后另一个线程把b设置为false，但是主线程无法退出：

private static boolean b = true;   
public static void main(String[] args) throws InterruptedException { new Thread( $)\rightarrow$ { try{ TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e){ } b=false; }）.start(); while(b){ TimeUnit MILLisecondsssleep(0); } System.out.println("done");

其实，这是可见性的问题。虽然另一个线程把b设置为了false，但是因为这个字段在CPU缓存中，另一个线程（主线程）还是读不到最新的值。使用volatile关键字，可以让数据刷新到主内存中。准确地说，让数据刷新到主内存中是做下面两件事情。

（1）将当前处理器缓存行的数据，写回到系统内存。  
（2）这个写回内存的操作会导致其他CPU里缓存了该内存地址的数据变为无效。

当然，使用AtomicBoolean等关键字来修改变量b也可以。但与volatile相比，AtomicBoolean等关键字除了确保可见性，还提供了CAS方法，具有更多的功能，在本例的场景中用不到。

2. 在2.2.4节中还提到了代码加锁的两个坑，一是加锁和释放没有配对的问题，二是锁自动释放导致的重复逻辑执行的问题。你有什么方法来发现和解决这两种问题吗？

针对加锁和解锁没有配对的问题，我们可以用代码质量工具或代码扫描工具（如Sonar）来排查。这个问题在编码阶段就能发现。

针对分布式锁超时自动释放的问题，可以参考Redisson的RedissonLock的锁续期机制。锁续期是每次续一段时间，如30s，然后10s执行一次续期。虽然是无限次续期，但即使客户端崩溃了也没关系，不会无限期占用锁，因为崩溃后无法自动续期自然最终会超时。

# 2.3 线程池：业务代码中最常用也最容易犯错的组件

在程序中，我们会用各种池化技术（如线程池、连接池、内存池等）被用来缓存创建昂贵的对象。通常情况下是预先创建一些对象放入池中，使用的时候直接取出使用，用完归还以便复用；还会通过一定的策略调整池中缓存对象的数量，实现池的动态伸缩。

由于线程的创建比较昂贵，随意、没有控制地创建大量线程会造成性能问题，因此短平快的任务一般考虑使用线程池来处理。本节将通过3个生产事故来讲述使用线程池的注意事项。

# 2.3.1 线程池的声明需要手动进行

Java中的Executors类定义了一些快捷的工具方法，来帮助我们快速创建线程池。《阿里巴巴Java开发手册》一书中提到，禁止使用这些方法创建线程池，而应该手动new ThreadPoolExecutor创建线程池。这一条规则的背后，是大量严重的生产事故，最典型的就是

newFixedThreadPool 和 newCachedThreadPool，可能因为资源耗尽导致OOM问题。为什么使用newFixedThreadPool可能会出现OOM问题？

下面写一段测试代码来初始化一个单线程的FixedThreadPool，循环1亿次向线程池提交任务，每个任务都会创建一个比较大的字符串然后休眠 $1\mathrm{h}$ ：

@GetMapping("OOM1")   
public void oom1() throws InterruptedException {   
ThreadPoolExecutor threadPool $=$ (ThreadPoolExecutor) Executors.newFixedThreadPool(1); //打印线程池的信息   
printStats(threadPool);   
for (int $\mathrm{i} = 0$ .i<100000000; $\mathrm{i + + }$ ）{ threadPool.execute（（）->{ String payload $=$ IntStream.closeClosed(1，1000000） .mapToObj（_->"a") .collect(Collectors.join()) $^+$ UUID.randomUUID().toString(); try{ TimeUnit.HOURS.sleep(1); } catch (InterruptedException e){ } log.infoields);   
}；   
threadPool.shutdown();   
threadPool awaitTermination(1，TimeUnit.HOURS);   
}

执行程序后不久，日志中就出现了如下内存溢出错误（OutOfMemoryError）：

```txt
Exception in thread "http-nio-45678-ClientPoller" java.lang.OfMemoryError: GC overhead limit exceeded 
```

翻看newFixedThreadPool方法的源码不难发现，线程池的工作队列直接新建了一个LinkedBlockingQueue，而默认构造方法的LinkedBlockingQueue是一个Integer.MAX_VALUE长度的队列，可以认为是无界的。

```java
public static ExecutorService newFixedThreadPool(int nThreads) { return newThreadPoolExecutor(nThreads, nThreads, OL, TimeUnit.MILLisecondNS, new LinkedBlockingQueue<Runnable>();   
public class LinkedBlockingQueueE extends AbstractQueueE implements BlockingQueueE,java.io.Serializable { /\*\* \* Creates a {@code LinkedBlockingQueue} with a capacity of \* {@link Integer#MAX_VALUE}. \*/ public LinkedBlockingQueue() { this(Integer.MAX_VALUE); } 
```

虽然使用newFixedThreadPool可以把工作线程控制在固定的数量上，但是任务队列是无界

的。如果任务较多并且执行较慢，队列可能会快速积压撑爆内存导致OOM。

把刚才的例子稍微改一下：改为使用newCachedThreadPool方法来获得线程池。程序运行不久后，同样看到了如下OOM异常：

```txt
[11:30:30.487] [http-nio-45678-exec-1] [ERROR] [.a.c.c.C.[.][.][.][dispatcherservlet]:175] - Servlet.service() for servlet [dispatcherservlet] incontext with path [] threw exception [Handler dispatch failed; nested exception is java.lang OutOfMemoryError: unable to create new native thread] with root cause java.lang.OutOfMemoryError: unable to create new native thread 
```

可以看到，这次OOM的原因是无法创建线程，newCachedThreadPool的源码如下：

```java
public static ExecutorService newCachedThreadPool() { return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(); 
```

可以看到，这种线程池的最大线程数是Integer.MAX_VALUE，可以认为是没有上限的，而其工作队列SynchronousQueue是一个没有存储空间的阻塞队列。这意味着，只要有请求到来，就必须找到一条工作线程来处理，如果当前没有空闲的线程就再创建一条新的。

由于我们的任务需要 $1\mathrm{h}$ 才能执行完成，大量的任务进来后会创建大量的线程。我们知道线程是需要分配一定的内存空间作为线程栈的，如1MB，因此无限制创建线程必然会导致OOM。

其实，大部分Java开发人员知道这两种线程池的特性，只是抱有侥幸心理，觉得只是使用线程池做一些轻量级的任务，不可能造成队列积压或开启大量线程。现实往往是残酷的。我就遇到过这么一个事故：用户注册后，我们调用一个外部服务去发送短信，发送短信接口正常时可以在 $100\mathrm{ms}$ 内响应TPS100的注册量，CachedThreadPool能稳定在占用10个左右线程的情况下满足需求。在某个时间点，外部短信服务不可用了，我们调用这个服务的超时又特别长，如 $1\mathrm{min}$ ， $1\mathrm{min}$ 可能就进来了6000用户产生6000个发送短信的任务，需要6000个线程，没多久就因为无法创建线程导致了OOM，整个应用程序崩溃。

因此，我同样不建议使用Executors提供的两种快捷的线程池，原因如下。

- 需要根据自己的场景、并发情况来评估线程池的几个核心参数，包括核心线程数、最大线程数、线程回收策略、工作队列的类型和拒绝策略，确保线程池的工作行为符合需求，一般都需要设置有界的工作队列和可控的线程数。  
- 任何时候，都应该为自定义线程池指定有意义的名称，以方便排查问题。当出现线程数量暴增、线程死锁、线程占用大量CPU、线程执行出现异常等问题时，我们往往会抓取线程栈。此时，有意义的线程名称，就可以方便我们定位问题。

除了建议手动声明线程池，我还建议用一些监控手段来观察线程池的状态。线程池这个组件往往会表现得任劳任怨、默默无闻，除非是出现了拒绝策略，否则压力再大都不会抛出一个异常。如果我们能提前观察到线程池队列的积压，或者线程数量的快速增长，往往可以提早发现并解决问题。

# 2.3.2 线程池线程管理策略详解

2.3.1节的示例中用一个printStats方法实现了最简陋的监控，每秒输出一次线程池的基本内部信息，包括线程数、活跃线程数、完成了多少任务，以及队列中还有多少积压任务等，代码如下：

```java
private void printStats(ThreadPoolExecutor threadPool) {  
Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate() -> {  
    log.info("************************");  
    log.info("Pool Size: {}, threadPool.getPoolSize());  
    log.info("Active Threads: {}, threadPool.getActiveCount());  
    log.info("Number of Tasks Completed: {}, threadPool.getCompletedTaskCount());  
    log.info("Number of Tasks in Queue: {}, threadPool.getQueue().size());  
    log.info("************************");  
}, 0, 1, TimeUnit.SECONDS); 
```

我们可以使用这个方法来观察线程池的基本特性。

首先，自定义一个线程池。这个线程池具有2个核心线程、5个最大线程、使用容量为10的ArrayBlockingQueue阻塞队列作为工作队列，使用默认的AbortPolicy拒绝策略，也就是任务添加到线程池失败会抛出RejectedExecutionException。此外，我们借助了Jodd类库的ThreadFactoryBuilder方法来构造一个线程工厂，实现线程池线程的自定义命名。

然后，写一段测试代码来观察线程池管理线程的策略。测试代码的逻辑为，每次间隔1s向线程池提交任务，循环20次，每个任务需要10s才能执行完成，代码如下：

GetMapping("right")   
public int right() throws InterruptedException { //使用一个计数器跟踪完成的任务数 AtomicIntegeratomicInteger $=$ new AtomicInteger(); /\* \*创建一个具有2个核心线程、5个最大线程，使用容量为10的ArrayBlockingQueue阻塞队列 \*作为工作队列的线程池，使用默认的AbortPolicy拒绝策略 \*/ ThreadPoolExecutor threadPool $=$ new ThreadPoolExecutor( 2,5, 5,TimeUnit.SECONDS, new ArrayBlockingQueue<> (10), new ThreadFactoryBuilder().setTitle("demo-threadpool-%d").get(), new ThreadPoolExecutor.AbortPolicy()); printStats(threadPool); //每隔1s提交一次，一共提交20次任务 IntStream(rangeClosed(1,20).forEach(i->{ try{ TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e){ e.printStackTrace(); } int id $=$ atomicInteger_incrementAndGet(); try{ threadPool.submit((）->{ log.info("\{\}started",id); //每个任务耗时10s try{ TimeUnit.SECONDS.sleep(10); } catch (InterruptedException e){ } log.info("\{\}finished",id); } catch(Exception ex){ //提交出现异常的话，打印出错信息并为计数器减一 log.error("error submittingting task\}",id,ex);

```javascript
atomicInteger.decrementAndGet();
}
}); 
```

60s后页面输出了17，有3次提交失败了，如图2-16所示。

![](images/59cb5d93c5558b5fc05e1c05953467555d7e973e12a4a0b80efaf488195be7a5.jpg)  
图2-16 线程池测试

并且日志中也出现了3次类似下面的错误信息：

```txt
[14:24:52.879] [http-nio-45678-exec-1] [ERROR]  
[t.c.t.demol.ThreadPoolOOMController:103] - error submitting task 18  
java.util.concurrent-RejectedExecutionException: Taskjava.util.concurrent.FutureT  
ask@163a2dec rejected from java.util.concurrent.ThreadPoolExecutor@18061ad2[Runn  
ng, pool size = 5, active threads = 5, queued tasks = 10, completed tasks = 2] 
```

把 printStats 方法打印出的日志绘制成图表，得出如图 2-17 所示的曲线：

- 标记有圆形记号的折线代表完成任务数，随着时间的推移数量慢慢增长，一直到17（3个任务没有提交成功）；  
- 标记有向上和向下箭头的折线分别代表池大小和活跃线程数，随着时间的推移慢慢增长到线程池最大线程数5；  
- 标记有方块的是队列任务数量，随着时间的推移队列中任务堆积到最大值 10。

![](images/09f49041c7b52e84d50574940ecaa1d73210a07ae20e2ac8744b134d14bf582d.jpg)  
图2-17 线程池一些核心数值随着时间的变化

通过观察这些曲线来分析线程池默认的工作行为，可以总结为如下几点：

- 不会初始化corePoolSize个线程，有任务来了才创建工作线程；  
- 当核心线程满了之后不会立即扩容线程池，而是把任务堆积到工作队列中；  
- 当工作队列满了后扩容线程池，一直到线程个数达到 maximumPoolSize 为止；

- 如果队列已满且达到了最大线程后还有任务进来，按照拒绝策略处理；  
- 当线程数大于核心线程数时，线程等待keepAliveTime后还是没有任务需要处理的话，收缩线程数到核心线程数。

了解这个策略，有助于我们根据实际的容量规划需求，为线程池设置合适的初始化参数。当然，我们也可以通过一些手段来改变这些默认工作行为，比如：

- 声明线程池后立即调用prestartAllCoreThreads方法，来启动所有核心线程；  
- 传入 true 给 allowCoreThreadTimeOut 方法，来让线程池在空闲的时候同样回收核心线程。

不知道你有没有想过：Java线程池是先用工作队列来存放来不及处理的任务，满了之后再扩容线程池。当工作队列设置得很大时，最大线程数这个参数显得没有意义，因为队列很难满，或者到满的时候再去扩容线程池已经于事无补了。

那么，有没有办法让线程池更激进一点，优先开启更多的线程，而把队列当成一个后备方案呢？比如在这个例子中任务执行得很慢，需要 $10\mathrm{s}$ ，如果线程池可以优先扩容到5个最大线程，那么这些任务最终都可以完成，而不会因为线程池扩容过晚导致慢任务来不及处理。我给出的大致思路如下：

- 线程池在工作队列满了无法入队的情况下会扩容线程池，那么我们是否可以重写队列的offer方法，造成这个队列已满的假象呢？  
- 由于我们修改了队列，在达到了最大线程后势必会触发拒绝策略，那么能否实现一个自定义的拒绝策略处理程序，这个时候再把任务真正插入队列呢？

完整的实现代码及相应的测试代码如下：

GetMapping("better")   
public int better() throws InterruptedException { //这里开始是激进线程池的实现 BlockingQueue<Runnable> queue $=$ new LinkedBlockingQueue<Runnable>(10){ @Override public boolean offer(Runnable e) { //先返回false，造成队列满的假象，让线程池优先扩容 return false; }； ThreadPoolExecutor threadPool $=$ new ThreadPoolExecutor( 2,5, 5, TimeUnit.SECONDS, queue,new ThreadFactoryBuilder(). setNameFormat("demo-threadpool-%d").get(), (r, executor) $\rightarrow$ { try{ //等出现拒绝后再加入队列 //如果希望队列满了阻塞线程而不是抛出异常，那么可以注释掉下面3行代码 //修改为 executor.getQueue().put(r); if(!executor.getQueue().offer(r,0, TimeUnit.SECONDS)) { throw new RejectedExecutionException("ThreadPool queue full, failed to offer" $^+$ r.toString(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); }）; //激进线程池实现结束

printStats(threadPool); //每秒提交一个任务，每个任务耗时10s执行完成，一共提交20个任务

//任务编号计数器  
```java
AtomicInteger atomicInteger = new AtomicInteger();   
IntStream(rangeClosed(1, 20).forEach(i -> { try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); } int id = atomicInteger_incrementAndGet(); try { threadPool.submit((   ) -> { log.info("\{ started", id); try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException e) { } log.info("\{ finished", id); } catch (Exception ex) { log.error("error submittingting task \{\}", id, ex); atomicInteger decrerementAndGet(); } }); TimeUnit.SECONDS.sleep(60); return atomicInteger.intValue(); 
```

使用这个激进的线程池可以处理完这20个任务，因为优先开启了更多线程来处理任务。

```txt
[10:57:16.092] [demo-threadpool-4] [INFO] [o.g.t.c.t.t.ThreadPoolOOMControl1 er:157] - 20 finished  
[10:57:17.062] [pool-8-thread-1] [INFO] [o.g.t.c.t.t.ThreadPoolOOMControl1 er:22] - ===============  
[10:57:17.062] [pool-8-thread-1] [INFO] [o.g.t.c.t.t.ThreadPoolOOMControl1 er:23] - Pool Size: 5  
[10:57:17.062] [pool-8-thread-1] [INFO] [o.g.t.c.t.t.ThreadPoolOOMControl1 er:24] - Active Threads: 0  
[10:57:17.062] [pool-8-thread-1] [INFO] [o.g.t.c.t.t.ThreadPoolOOMControl1 er:25] - Number of Tasks Completed: 20  
[10:57:17.062] [pool-8-thread-1] [INFO] [o.g.t.c.t.t.ThreadPoolOOMControl1 er:26] - Number of Tasks in Queue: 0  
[10:57:17.062] [pool-8-thread-1] [INFO] [o.g.t.c.t.t.ThreadPoolOOMControl1 er:28] - =============== 
```

# 2.3.3 务必确认清楚线程池本身是不是复用的

我曾遇到过这样一个事故：某项目的生产环境时不时有报警提示线程数过多（超过2000个），收到报警后查看监控发现，瞬时线程数比较多但过一会儿又会降下来，线程数抖动很厉害，而应用的访问量变化不大。

为了定位问题，我们在线程数比较高的时候抓取线程栈，抓取后发现内存中有1000多个自定义线程池。一般而言，线程池肯定是复用的，有5个以内的线程池都可以认为正常，而1000多个线程池肯定不正常。

在项目代码里，我们没有搜到声明线程池的地方，搜索 execute 关键字后定位到原来

是业务代码调用了一个类库来获得线程池，类似如下的业务代码调用 ThreadPoolHelper 的 getThreadPool 方法来获得线程池，然后提交数个任务到线程池处理，看不出什么异常：

@GetMapping("wrong")   
public String wrong() throws InterruptedException { ThreadPoolExecutor threadPool $=$ ThreadPoolHelper.getThreadPool(); IntStream rangeClosed(1, 10).forEach(i -> { threadPool.execute()) -> { try{ TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { } }）; }）; return "OK";

但是，来到 ThreadPoolHelper 的实现让人大跌眼镜，getThreadPool 方法居然是每次都使用 Executors.newCachedThreadPool 来创建一个线程池：

```java
class ThreadPoolHelper {
    public static ThreadPoolExecutor getThreadPool() {
        // 线程池没有复用
        return (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }
} 
```

2.3.1节中提到过，newCachedThreadPool会在需要时创建必要多的线程，业务代码的一次业务操作会向线程池提交多个慢任务，这样执行一次业务操作就会开启多个线程。如果业务操作并发量较大的话，的确有可能一下子开启几千个线程。那么，在监控中为什么能看到线程数量会下降，而没有撑爆内存呢？

回到newCachedThreadPool的定义，它的核心线程数是0，而keepAliveTime是60s，也就是在60s之后所有的线程都是可以回收的。就是因为这个特性，我们的业务程序没死得太难看。

要修复这个bug也很简单，使用一个静态字段来存放线程池的引用，返回线程池的代码直接返回这个静态字段即可。切记我们的最佳实践，手动创建线程池。修复后的ThreadPoolHelper类如下：

```java
class ThreadPoolHelper {
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 50, 2, TimeUnit.SECONDS, new ArrayBlockingQueue<> (1000), new ThreadFactoryBuilder(). setNameFormat("demo-threadpool-%d").get());
    public static ThreadPoolExecutor getRightThreadPool() {
        return threadPoolExecutor;
    }
} 
```

# 2.3.4 需要仔细斟酌线程池的混用策略

线程池的意义在于复用，那是不是意味着程序应该始终使用一个线程池呢？当然不是。通过2.3.1节我们知道，要根据任务的“轻重缓急”来指定线程池的核心参数，包括线程数、回收

策略和任务队列。

- 对于执行比较慢、数量不大的 I/O 任务，或许要考虑更多的线程数，而不需要太大的队列。  
- 对于吞吐量较大的计算型任务，线程数量不宜过多，可以是CPU核数或核数 $\times 2$ （理由是，线程一定调度到某个CPU进行执行，如果任务本身是CPU绑定的任务，那么过多的线程只会增加线程切换的开销，并不能提升吞吐量），但可能需要较长的队列来做缓冲。

之前我也遇到过这么一个问题，业务代码使用了线程池异步处理一些内存中的数据，但通过监控发现处理得非常慢，整个处理过程都是内存中的计算不涉及I/O操作，也需要数秒的处理时间，应用程序CPU占用也不是特别高，有点不可思议。经排查发现，业务代码使用的线程池，还被一个后台的文件批处理任务用到了。或许是够用就好的原则，这个线程池只有2个核心线程，最大线程也是2，使用了容量为100的ArrayBlockingQueue作为工作队列，使用了CallerRunsPolicy拒绝策略。

```java
private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 2, 1, TimeUnit.HOURS, new ArrayList BlockingQueue<> (100), new ThreadFactoryBuilder().setNameFormat("batchfileprocess-threadpool-%d").get(), new ThreadPoolExecutor CallerRunsPolicy()); 
```

下面模拟一下文件批处理的代码，在程序启动后通过一个线程开启死循环逻辑，不断向线程池提交任务，任务的逻辑是向一个文件中写入大量的数据：

```java
@PostConstruct
public void init() {
    printStats(threadPool);
    new Thread() -> {
        // 模拟需要写入的大量数据
        String payload = IntStream.closeClosed(1, 1_000_000)
        .mapToObj(_ -> "a")
        .collect(Collectors.join(""); 
    while (true) {
        threadPool.exec() -> {
            try {
                // 每次都是创建并写入相同的数据到相同的文件
                Files.write(Paths.get("demo.txt"), Collections.
                singletonList(LocalTime但现在().toString() + ": "+ payload), UTF_8, CREATE, TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info("batch file processing done");
            };
        }
    ).start();
} 
```

可以想象，这个线程池中的2个线程任务是相当重的。通过printStats方法打印出的日志如图2-18所示。

```txt
[16:10:32.062] [batchfileprocess-threadpool-0] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:86 ] - batch file processing done  
[16:10:32.064] [batchfileprocess-threadpool-1] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:86 ] - batch file processing done  
[16:10:32.066] [batchfileprocess-threadpool-0] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:86 ] - batch file processing done  
[16:10:32.066] [Thread-4] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:86 ] - batch file processing done  
[16:10:32.069] [batchfileprocess-threadpool-1] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:86 ] - batch file processing done  
[16:10:32.069] [pool-4-thread-1] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:44 ] - Active Threads: 2  
[16:10:32.069] [pool-4-thread-1] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:45 ] - Number of Tasks Completed: 1540  
[16:10:32.070] [pool-4-thread-1] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:46 ] - Active Threads: 2  
[16:10:32.070] [pool-4-thread-1] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:47 ] - Number of Tasks in Queue: 99  
[16:10:32.070] [pool-4-thread-1] [INFO ] [g.t.c.t.d,…ThreadPoolMixuseController:50 ] - 
```

图2-18 观察线程池的负荷

可以看到，线程池的2个线程始终处于活跃状态，队列也基本处于打满状态。因为开启了CallerRunsPolicy拒绝处理策略，所以当线程满载队列也满的情况下，任务会在提交任务的线程，或者说调用execute方法的线程执行，也就是说不能认为提交到线程池的任务就一定是异步处理的。如果使用了CallerRunsPolicy策略，那么有可能异步任务变为同步执行。从图2-18的第四行也可以看到这点。这也是CallerRunsPolicy拒绝策略比较特别的原因。

不知道写代码的人为什么设置这个策略，或许是测试时发现线程池因为任务处理不过来出现了异常而又不希望线程池丢弃任务，所以最终选择了这样的拒绝策略。不管怎样，这些日志足以说明线程池是饱和状态。

可以想象，业务代码复用这样的线程池来做内存计算，命运一定是悲惨的。写一段代码测试下，向线程池提交一个简单的任务，这个任务只是休眠 $10\mathrm{ms}$ 并没有其他逻辑：

```java
private Callable<Integer>calcTask(){ return（）->{ TimeUnit.MILLISECONDS.sleep(10); return 1; };   
GetMapping("wrong")   
public int wrong()throws ExecutionException，InterruptedException{ return threadPool.submit(calcTask().get();   
} 
```

使用wrk工具对这个接口进行一个简单的压测，TPS约为75（如图2-19所示），性能的确非常差。

![](images/54a687ec14cfcec1522cba783541a1a38237a6c12c1cd081b76c774b0943d3fc.jpg)  
图2-19 使用wrk工具压测混用的线程池

细想一下，问题其实没有这么简单。因为原来执行I/O任务的线程池使用的是CallerRunsPolicy策略，所以直接使用这个线程池进行异步计算，当线程池饱和的时候计算任务会在执行Web请求的Tomcat线程执行，这时就会进一步影响其他同步处理的线程，甚至造成整个应用程序崩溃。

解决方案很简单，使用独立的线程池来做这样的“计算任务”即可。计算任务加了双引号，是因为模拟代码执行的是休眠操作，并不属于CPU绑定的操作，更类似I/O绑定的操作，如果线程池线程数设置太小会限制吞吐能力：

```java
private static ThreadPoolExecutor asyncCalcThreadPool = new ThreadPoolExecutor(200, 200, 1, TimeUnit.HOURS, newArrayBlockingQueue<> (1000), new ThreadFactoryBuilder(). setNameFormat("asynccalc-threadpool-%d").get()); @GetMapping("right") public int right() throws ExecutionException,扰乱Exception { return asyncCalcThreadPool.submit(calcTask()).get(); } 
```

使用单独的线程池改造代码后再来测试一下性能，TPS提高到了1727.68s，如图2-20所示。

![](images/66574bfead2bcbe3d0f88a23ed53be52701aebe63b01c991f38c6b8f1f952eec.jpg)  
图2-20 使用wrk工具压测独立的线程池

可以看到，盲目复用线程池混用线程的问题在于，别人定义的线程池属性不一定适合你的任务，而且混用会相互干扰。这就好比我们往往会用虚拟化技术来实现资源的隔离，而不是让所有应用程序都直接使用物理机。

针对线程池混用问题，我再补充一个坑：Java 8 的并行流功能，可以让我们很方便地并行处理集合中的元素，其背后是共享同一个 ForkJoinPool，默认并行度是“CPU 核数 -1”。对 CPU 绑定的任务来说，使用这样的配置比较合适，但是，如果集合操作涉及同步 I/O 操作的话（比如数据库操作、外部服务调用等），就建议自定义一个 ForkJoinPool（或普通线程池）。读者可以参考 2.1 节中的相关示例。

# 2.3.5 小结

实践中，许多应用程序的性能问题都源自线程池的配置和使用不当。请牢记下面3个最佳实践。

- 使用线程池时，一定要根据场景和需求配置合理的线程数、任务队列、拒绝策略、线程回收策略，并对线程进行明确地命名以方便排查问题。  
- 既然使用了线程池就需要确保线程池是在复用的，每次新建一个线程池可能比不用线程池还糟糕。如果没有直接声明线程池而是使用其他人提供的类库来获得一个线程池，请务必查看源码，以确认线程池的实例化方式和配置是符合预期的。  
- 复用线程池不代表应用程序始终使用同一个线程池，应该根据任务的性质来选用不同的线程池。特别注意I/O绑定的任务和CPU绑定的任务对于线程池属性的偏好，如果希望减少任务间的相互干扰，考虑按需使用隔离的线程池。

此外还需要注意一点，线程池作为应用程序内部的核心组件往往缺乏监控（如果使用类似RabbitMQ这样的MQ中间件，运维人员一般会帮我们做好中间件监控），这就导致往往到程序崩溃后才发现线程池的问题，很被动。本书第3章将重新谈及这个问题。

# 2.3.6 思考与讨论

在2.3.3节中，我们改进了ThreadPoolHelper使其能够返回复用的线程池。如果不小心每次

都新建了这样一个自定义的线程池（核心线程数为10，50最大线程数为50，2s回收），反复执行测试接口线程，最终可以被回收吗？会出现OOM问题吗？

默认情况下核心线程不会回收，并且 ThreadPoolExecutor 也回收不了，所以会因为创建过多线程导致 OOM。看一下 ThreadPoolHelper 的源码，工作线程 Worker 是内部类，只要它活着，换句话说就是线程在跑，就会阻止 ThreadPoolExecutor 回收。

```java
public class ThreadPoolExecutor extends AbstractExecutorService { private final class Worker extends AbstractQueuedSynchronizer implements Runnable { } 
```

因此，我们不能认为 ThreadPoolExecutor 没有引用，就能回收。

# 2.3.7 扩展阅读

线程池的一个典型使用场景是异步执行一些子任务，等到所有任务都处理完成后，继续主线程的操作（如汇总这些任务的结果）。实现这种模式的3种方式如下所示。

（1）把任务逐一提交到线程池后，逐一等待每个任务完成：

```java
private static void test1() {
    long begin = System.currentTimeMillis();
    List<Future<Integer>> futures = IntStream(rangeClosed(1, 4)
        .mapToObj(i -> threadPool.submit(getAsyncTask(i)))
        .collect(Collectors.list());
    List<Integer> result = futures.stream().map(future -> {
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }).collect(Collectors.list());
    log.info("result {} took {}, ms", result, System.currentTimeMillis() - begin);
} // 提供一个异步任务的定义
private static Callable<Integer> getAsyncTask(int i) {
    return () -> {
        TimeUnit.SECONDS.sleep(i);
        return i;
} 
```

这个例子中有4个子任务，它们分别休眠1s、2s、3s和4s，然后分别返回整数1、2、3、4。如果串行执行这些子任务需要10s，而并行执行这些子任务所需要的时间则是耗时最长任务的时间，也就是4s。执行程序可以看到如下输出：

```txt
result [1, 2, 3, 4] took 4113 ms 
```

总的执行时间差不多是4s，并且我们拿到了所有任务的结果。不过这种方式不容易为总的任务执行时间设置一个超时时间。虽然future.get()的时候我们可以设置一个超时时间，但那是单一任务的执行时间，而且在某个任务进行future.get()时，可能已经任务执行一段时间了。所以，这个超时时间其实并不是单一任务的超时时间。

（2）每个子任务完成后自己去更新一个 CountDownLatch，主线程直接等待 CountDownLatch 全部倒计时完成即可，然后可以设置一个总的等待时间：

```java
private static void test2() {
    long begin = System.currentTimeMillis();
    int count = 4;
    List<Integer> result = new ArrayList<> (count);
    // 申请一个初始值为4的倒计时器
    CountDownLatch countDownLatch = new CountDownLatch(count);
    IntStream.countClosed(1, count).forEach(i -> threadPool.execute(executetheasyncTask(i, countDownLatch, result));
    try {
        // 总共等待5s
        countDownLatch await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    log.info("result{} took {}, result, System/countdownmillis()-begin");
}
// 提供一个异步任务的定义
private static Runnable executeAsyncTask(int i, CountDownLatch countDownLatch, List<Integer> result) {
    return () -> {
        try {
            TimeUnit.SECONDS.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 直接把值加入结果
        synchronized (result) {
            result.add(i);
        }
        // 倒计时一次
        countDownLatch.countDown();
    };
} 
```

我们一共等待5s，这是完全够用的，可以得到如下输出：

```txt
result [1, 2, 3, 4] took 4103 ms 
```

如果我们期望主线程只等待 $3.5\mathrm{s}$ ，可以改一下countDownLatch await（5, TimeUnit.SECONDS）  
那行代码：

```javascript
countDownLatch await(3500, TimeUnit.MILLisecondNS); 
```

再执行一次得到的输出是：

```txt
result [1, 2, 3] took 3605 ms 
```

输出也符合预期，因为只等待了3.5s，所以第4个任务显然来不及执行完毕。

（3）JDK8以上的版本可以方便地使用CompletableFuture组合Future对象：

```java
private static void test3() throws ExecutionException, InterruptedException { long begin = System.currentTimeMillis(); List<Integer> result = new ArrayList<>(); List<CompletableFuture<Integer>> futures = IntStream(rangeClosed(1, 4) .mapToObj(i -> CompletableFuture supplyAsync(getAsyncTaskSupplier(i)) .whenComplete((r, t) -> result.add(r))) .collect(Collectors.list()); 
```

```java
try{ //使用CompletableFuture.allOf方法等待所有异步任务结束 CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])) .get(3，TimeUnit.SECONDS); }catch（TimeoutException e）{ e.printStackTrace(); } log.info("result{}took{}ms"，result，System.currentTimeMillis() - begin);   
} private static Supplier<Integer> getAsyncTaskSupplier(int i){ return（）->{ try{ TimeUnit.SECONDS.sleep(i); } catch（InterruptedException e）{ throw new RuntimeException(e); } return i; 1; 
```

# 2.4 连接池：别让连接池帮了倒忙

连接池一般对外提供获得连接、归还连接的接口给客户端使用，并暴露最小空闲连接数、最大连接数等可配置参数，在内部则实现连接建立、连接心跳保持、连接管理、空闲连接回收、连接可用性检测等功能。连接池的结构如图2-21所示。

![](images/dfb76e5fe4233a48f1e9f93d86cafddda11aaaab1340e39c6738b97581ea12c2.jpg)  
图2-21 连接池的结构

业务项目中经常会用到的连接池，主要是数据库连接池、Redis 连接池和 HTTP 连接池。本节将以它们为例，讲解使用和配置连接池容易出错的地方。

# 2.4.1 注意鉴别客户端SDK是否基于连接池

在使用三方客户端进行网络通信时，首先要确定客户端SDK是不是基于连接池技术实现的。我们知道，TCP是面向连接的基于字节流的协议：

- 面向连接意味着连接需要先创建再使用，创建连接的三次握手有一定开销；  
- 基于字节流意味着字节是发送数据的最小单元，TCP协议本身无法区分哪几字节是完整的消息体，也无法感知是否有多个客户端在使用同一个TCP连接，TCP只是一个读写数据的管道。

如果客户端SDK没有使用连接池，而直接是TCP连接，那么就需要考虑每次建立TCP连接的开销，并且因为TCP基于字节流，在多线程的情况下对同一连接进行复用，可能会产生线程安全问题。

涉及TCP连接的客户端SDK对外提供API，有如下3种方式。

- 连接池和连接分离的API：有一个XXXPool类负责连接池实现，先从其获得连接XXXConnection，然后用获得的连接进行服务器端请求，完成后使用者需要归还连接。通常，XXXPool是线程安全的，可以并发获取和归还连接，而XXXConnection是非线程安全的。对应到图2-21中，XXXPool就是中间“连接池”那个框，左边的客户端是我们自己的代码。  
- 内部带有连接池的 API：对外提供一个 XXXClient 类，通过这个类可以直接进行服务器端请求；这个类内部维护了连接池，SDK 使用者无须考虑连接的获取和归还问题。一般而言，XXXClient 是线程安全的。对应到图 2-21 中，整个 API 就是最外面的那个大框的部分。  
- 非连接池的API：一般命名为XXXConnection，以区分其是基于连接池还是单连接的，而不建议命名为XXXClient或XXX。直接连接方式的API基于单一连接，每次使用都需要创建和断开连接，性能一般，且通常不是线程安全的。对应到图2-21中，这种形式相当于没有“连接池”那个框，客户端直接连接服务器端创建连接。

在面对各种三方客户端的时候，只有先识别出其属于哪一种，才能厘清使用方式。

虽然上面提到了SDK一般的命名习惯，但不排除有一些客户端特立独行，因此在使用三方SDK时，一定要先查看官方文档了解其最佳实践，或是在类似Stackoverflow的网站搜索“XXXthreadsafe/singleton”看看大家的回复，也可以一层一层地往下看源码，直到定位到原始套接字来判断套接字和客户端API的对应关系。

明确了SDK连接池的实现方式后，我们就大概知道了使用SDK的最佳实践。

- 如果是分离方式，那么连接池本身一般是线程安全的，可以复用。每次使用需要从连接池获取连接，使用后归还，归还的工作由使用者负责。  
- 如果是内置连接池，SDK会负责连接的获取和归还，使用的时候直接复用客户端。  
- 如果SDK没有实现连接池（大多数中间件、数据库的客户端SDK都会支持连接池），那么通常不是线程安全的，而且短连接的方式性能不会很好，使用的时候需要考虑是否自己封装一个连接池。

以 Java 中用于操作 Redis 最常见的库 Jedis 为例，从源码角度分析下 Jedis 类到底属于哪种类型的 API，直接在多线程环境下复用一个连接会产生什么问题，以及如何用最佳实践来修复这个问题。

首先，向Redis初始化两组数据，即键为a，值为1；键为b，值为2。

```java
@PostConstruct
public void init() {
    try (Jedis jedis = new Redis("127.0.0.1", 6379)) {
        Assert.isTrue("OK".equals(jedis.set("a", "1"))), "set a = 1 return OK");
        Assert.isTrue("OK".equals(jedis.set("b", "2"))), "set b = 2 return OK");
    }
} 
```

然后，启动两个线程，共享操作同一个Jedis实例，每个线程循环1000次，分别读取键为a和b的值，判断是否分别为1和2。

```java
Jedis jedis = new Redis("127.0.0.1", 6379);  
new Thread() -> {  
    for (int i = 0; i < 1000; i++) {  
        String result = jedis.get("a");  
        if (!result.equals("1")) {  
            log.warn("Expect a to be 1 but found {}, result);  
            return;  
        }  
    }  
});start();  
new Thread() -> {  
    for (int i = 0; i < 1000; i++) {  
        String result = jedis.get("b");  
        if (!result.equals("2")) {  
            log.warn("Expect b to be 2 but found {}, result);  
            return;  
        }  
    }  
});start();  
TimeUnit.SECONDS.sleep(5); 
```

执行程序多次，可以看到日志中出现了各种奇怪的异常信息，有的是读取键为b的值读取到了1（错误1），有的是流非正常结束（错误2），还有的是连接关闭异常（错误3）。

# //错误1

```txt
[14:56:19.069] [Thread-28] [WARN] [.t.c.c.redis.JedisMisreuseController:45 ] - Expect b to be 2 but found 1 // 错误 2 redis.client.s.jedis.exceptions.Jedis_ConnectionException: Unexpected end of stream. at redis.client.s.jedis.util.RedisInputStreamensureFill(RedisInputStream.java:202) at redis.client.s.jedis.util.RedisInputStream.readLine(RedisInputStream.java:50) at redis.client.s.jedis.ProTOCOL-processError(Protocol.java:114) at redis.client.s.jedis.ProTOCOL process(Protocol.java:166) at redis.client.s.jedis.ProTOCOL.read(Protocol.java:220) at redis.client.s.jedis.Connection.readProtocolWithCheckingBroken(Connection.java:318) at redis.client.s.jedis.Connection.getBinaryBulkReply(Connection.java:255) at redis.client.s.jedis.Connection.getBulkReply(Connection.java:245) at redis.client.s.jedis.Jedis.get(Jedis.java:181) at javaprogramming commonmistakes.connectpool.redis.JedisMisuseController. lambda\$wrong\$1 (JedisMisuseController.java:43) at java.lang.Thread.run (Thread.java:748) 
```

# //错误3

```txt
java.io.IOException: Socket Closed
at java.net.AbstractPlainSocketImpl.getOutputStream(AbstractPlainSocketImpl.
java:440)
at java.net.Socket\$3.run(Socket.java:954)
at java.net.Socket\$3.run(Socket.java:952)
at java.security.AccessController.doPrivileged(Native Method)
at java.net.Socket.getOutputStream(Socket.java:951) 
```

```txt
atredis.clientj-jedis.Connection.connect(Connection.java:200) ...7 more 
```

分析一下Jedis类的源码：

public class Jedis extends BinaryJedis implements JedisCommands, MultiKeyCommands, AdvancedJedisCommands, ScriptingCommands, BasicCommands, ClusterCommands, SentinelCommands, ModuleCommands {   
}   
public class BinaryJedis implements BasicCommands, BinaryJedisCommands, MultiKey BinaryCommands,AdvancedBinaryJedisCommands,BinaryScriptingCommands,Closeable{ protected Client client $=$ null;   
}   
public class Client extends BinaryClient implements Commands {   
}   
public class BinaryClient extends Connection {   
}   
public class Connection implements Closeable { private Socket socket; private RedisOutputStream outputStream; private RedisInputStream inputStream;

可以看到，Jedis继承了BinaryJedis，BinaryJedis中保存了单个Client的实例，Client最终继承了Connection，Connection中保存了单个Socket的实例，和套接字连接对应的两个读写流（RedisInputStream和RedisOutputStream）。因此，一个Jedis对应一个Socket连接。类的层级结构如图2-22所示。

![](images/171f25071225601513a806633bf9549cbc8425f35efb6dff01f28eb77fb9954d.jpg)  
图2-22Jedis相关类的层级结构

BinaryClient封装了各种Redis命令，其最终会调用基类Connection的方法，使用Protocol类发送命令。看一下Protocol类的sendCommand方法的源码，可以发现其发送命令时是直接操作RedisOutputStream写入字节：

```java
private static void sendCommand(final RedisOutputStream os, final byte[] command, final byte[]... args) { try { os.write(ASTERISK_BYTE); os.writeIntCrLf(args.length + 1); os.write(DOLLAR_BYTE); os.writeIntCrLf(command.length); os.write commanded); os.writeCrLf(); for (final byte[] arg : args) { os.write(DOLLAR_BYTE); os.writeIntCrLf(arg.length); os.write(arg); os.writeCrLf(); } catch (IOException e) { throw new Jedis_ConnectionException(e); } 
```

我们在多线程环境下复用Jedis对象，其实就是在复用RedisOutputStream。如果多个线程在执行操作，那么既无法确保整条命令以一个原子操作写入套接字，也无法确保写入后、读取前没有其他数据写到远端。

看到这里我们也可以理解了，为什么多线程情况下使用Jedis对象操作Redis会出现各种奇怪的问题。例如，写操作互相干扰，多条命令相互穿插的话，必然不是合法的Redis命令，Redis会关闭客户端连接，导致连接断开；又例如，线程1和线程2先后实现了jedis.get("a")和jedis.get("b")操作，Redis也返回了值1和2，但是线程2先读取了数据1就会出现数据错乱的问题。

修复方式是，使用Jedis提供的另一个线程安全的类JedisPool来获得Jedis的实例。JedisPool可以声明为static在多个线程之间共享，扮演连接池的角色。使用时，按需使用try-with-resources模式从JedisPool获得和归还Jedis实例：

```java
private static JedisPool JedisPool = new JedisPool("127.0.0.1", 6379);
new Thread() -> {
    try (Jedis Jedis = JedisPool.getResource()) {
        for (int i = 0; i < 1000; i++) {
            String result = Jedis.get("a");
            if (!result.equals("1")) {
                log.warn("Expect a to be 1 but found {}, result");
            }
        }
    }
});start();
new Thread() -> {
    try (Jedis Jedis = JedisPool.getResource()) {
        for (int i = 0; i < 1000; i++) {
            String result = Jedis.get("b");
            if (!result.equals("2")) {
                log.warn("Expect b to be 2 but found {}, result");
            }
        }
    }
});start(); 
```

这样修复后，代码不再有线程安全问题了。此外，我们最好通过shutdownhook，在程序退出之前关闭JedisPool：

```java
@PostConstruct   
public void init() { Runtime.getRuntime().addShutdownHook(new Thread() -> { jedisPool.close(); }）;   
} 
```

看一下Jedis类close方法的实现可以发现，如果Jedis是从连接池获取的话，那么close方法会调用连接池的return方法归还连接：

```txt
public class Jedis extends BinaryJedis implements JedisCommands, MultiKeyCommands, AdvancedJedisCommands, ScriptingCommands, BasicCommands, ClusterCommands, SentinelCommands, ModuleCommands {
protected JedisPoolAbstract dataSource = null;
@override
public void close() {
if (dataSource != null) {
JedisPoolAbstract pool = this.dataSource;
this.dataSource = null;
if (client.isBroken()) {
pool.returnBrokenResource(this);
} else {
pool.returnResource(this);
}
} else {
super.close();
}
} 
```

如果不是，则直接关闭连接，最终调用Connection类的disconnect方法来关闭TCP连接：

```java
public void disconnect() { if (isConnected()) { try { OutputStream.flush(); socket.close(); } catch (IOException ex) { broken = true; throw new JegisdictionException(ex); } finally { IOutils.closeQuietly(socket); } } 
```

可以看到，Jedis可以独立使用，也可以配合连接池使用，这个连接池就是JedisPool。JedisPool的实现如下：

public class JedisPool extends JedisPoolAbstract { @Override public Jedis getResource() { Jedis jedis $=$ super.getResource(); jedis.setDataSource(this); return jedis; }

```java
@override protected void returnResource(final Jedis resource) { if (resource != null) { try { resource.resetState(); returnResourceObject(resource); } catch (Exception e) { returnBrokenResource(resource); throw new JedisException("Resource is returned to the pool as broken", e); } } } public class JedisPoolAbstract extends Pool<Jedis> { public abstract class Pool<T> implements Closeable { protected GenericObjectPool<T> internalPool; } 
```

JedisPool的getResource方法在拿到Jedis对象后，将自己设置为了连接池。连接池JedisPool继承了JedisPoolAbstract，而后者继承了抽象类Pool，Pool内部维护了Apache Common的通用池GenericObjectPool。JedisPool的连接池就是基于GenericObjectPool的。

所以，Jedis的API实现属于本节开始说的3种类型中的第一种，也就是连接池和连接分离的API，JedisPool是线程安全的连接池，Jedis是非线程安全的单一连接。

# 2.4.2 使用连接池务必确保复用

2.3节介绍线程池的时候强调过，池一定是用来复用的，否则其使用代价会比每次创建单一对象更大。对连接池来说更是如此，有以下几个原因。

- 创建连接池的时候很可能一次性创建了多个连接，考虑到性能，大多数连接池会在初始化的时候维护一定数量的最小连接（毕竟初始化连接池的过程一般是一次性的），可以直接使用。如果每次都按需创建连接池，那么很可能你每次只用到一个连接，但实际创建了 $N$ 个连接。  
- 连接池一般会有一些管理模块，也就是图2-21中的下方虚线框内的部分。举个例子，大多数的连接池都有闲置超时的概念。连接池会检测连接的闲置时间，定期回收闲置的连接，把活跃连接数降到最低（闲置）连接的配置值，减轻服务器端的压力。一般情况下，闲置连接由独立线程管理，启动了空闲检测的连接池相当于还会启动一个线程。此外，有些连接池还需要独立线程负责连接保活等功能。因此，启动一个连接池相当于启动了N个线程。

除了使用代价，连接池不释放还可能会引起线程泄漏。本节将以ApacheHttpClient为例，讲解连接池不复用带来的问题。

首先，创建一个CloseableHttpClient，设置使用PoolingHttpClientConnectionManager连接池并启用空闲连接驱逐策略，最大空闲时间为 $60\mathrm{s}$ ，然后使用这个连接来请求一个会返回OK字符串的服务器端接口：

@GetMapping("wrong1")   
public String wrong1() { CloseableHttpClient client $=$ HttpClients(custom() .setConnectionManager(new PoolingHttpClientConnectionManager())

```java
.egovs(60, TimeUnit.SECONDS).build(); try (CloseableHttpResponse response = client.execute(new HttpGet("http://127. 0.0.1:45678/httpclientnotreuse/test")) { return EntityUtilis.toString(response.getEntity()); } catch (Exception ex) { ex.printStackTrace(); } return null; 
```

访问这个接口几次后查看应用线程情况，有大量叫作Connectionevictor的线程（如图2-23所示），且这些线程不会销毁。

```txt
+ ~ jstack 91133 | grep evictor
"Connection evictor" #121 daemon prio=5 os_prio=31 tid=0x00007f87a2d68000 nid=0xdc03 waiting on condition [0x00007000169d4000]
"Connection evictor" #120 daemon prio=5 os_prio=31 tid=0x00007f87a0054800 nid=0xdb03 waiting on condition [0x00007000168d1000]
"Connection evictor" #119 daemon prio=5 os_prio=31 tid=0x00007f879fad6000 nid=0x11d03 waiting on condition [0x00007000167ce000]
"Connection evictor" #118 daemon prio=5 os_prio=31 tid=0x00007f879fad5800 nid=0x11f03 waiting on condition [0x00007000166cb000]
"Connection evictor" #117 daemon prio=5 os_prio=31 tid=0x00007f87a1fd3800 nid=0xda03 waiting on condition [0x00007000165c8000]
"Connection evictor" #116 daemon prio=5 os_prio=31 tid=0x00007f87a0053800 nid=0xd903 waiting on condition [0x00007000164c5000]
"Connection evictor" #115 daemon prio=5 os_prio=31 tid=0x00007f879fad4800 nid=0xd803 waiting on condition [0x00007000163c2000]
"Connection evictor" #114 daemon prio=5 os_prio=31 tid=0x00007f87a858800 nid=0xd6D3 waiting on condition [OxOoOoO7Ooo162bfOoo]
"Connection evictor" #113 daemon prio=5 os_prio=31 tid=OxOoOoO7f879f9498DOD=OxD4D3 waiting on condition [OxOoOoO7Ooo161bcOoo]
"Connection evictor" #112 daemon prio=5 os_prio=31 tid=OxOoOoO7f87a387d8DOD=OxD23D3 waiting on condition [OxOoOoO7Ooo16B9Doo]
"Connection evictor" #111 daemon prio=5 os_prio=31 tid=OxOoOoO7f87a3b4aDoo nid=OxD2D3 waiting on condition [OxOoOoO7Ooo15fb6Doo]
"Connection evictor" #11D daemon prio=5 os_prio=31 tid=OxOoOoO7f87a3b4BDOOD=OxD1D3 waiting on condition [OxOoOoO7Ooo15eb3Doo]
"Connection evictor" #1o9 daemon prio=5 os_prio=31 tid=OxOoOoO7f879f9C98DOD=OxfFOT3 waiting on condition [OxOoOoO7Ooo15dbDoo]
"Connection evictor" #1o8 daemon prio=5 os_prio=31 tid=OxOoOoO7f87a2461DOO nid=Ox126D3 waiting on condition [OxOoOoO7Ooo15cadDoo]
"Connection evictor" #1o7 daemon prio=5 os_prio=31 tid=OxOoOoO7f87a322a8DOD=OxcCOb3 waiting on condition [OxOoOoO7Ooo15baqDoo]
"Connection evictor" #1o6 daemon prio=5 os_prio=31 tid=OxOoOoO7f879fae1DOO nid=Ox128D3 waiting on condition [OxOoOoO7Ooo15aa7Doo] 
```

图2-23 通过jstack工具观察到大量的Connectionevictor线程

对这个接口进行几秒的压测（压测使用wrk，1个并发1个连接），如图2-24所示，已经建立了3000多个TCP连接到45678端口（其中有1个是压测客户端到Tomcat的连接，大部分都是HttpClient到Tomcat的连接）。

![](images/7a655a9a40d8aee08035d9d9f69f7e1aa0dde25adbbd883b0edd5f95370eb2bc.jpg)  
图2-24 使用lsof工具查看连接到45678端口的TCP连接数量

好在有了空闲连接回收的策略，60s之后连接处于CLOSE_WAIT状态，最终彻底关闭，如图2-25所示。

![](images/a136c87ebd49792daf7671cf9c534f99b79ff78c85987338d791870fc04a0759.jpg)  
图2-25 使用lsof工具观察45678端口的具体连接

这两点证明，CloseableHttpClient属于第二种模式，即内部带有连接池的API，其背后是连接池，最佳实践一定是复用。复用方式很简单，把CloseableHttpClient声明为static，只创建一次，并且在JVM关闭之前通过addShutdownHook钩子关闭连接池，在使用的时候直接使用CloseableHttpClient即可，无须每次都创建。

首先，定义一个 right 接口来实现服务器端接口调用：

private static CloseableHttpClient:httpClient $\equiv$ null;   
static{ //当然，也可以把CloseableHttpClient定义为Bean //然后在@PreDestroy标记的方法内close这个HttpClient httpClient $\equiv$ HttpClients(custom().setMaxConnPerRoute(1).setMaxConnTotal(1).

```java
evictIdleConnections(60, TimeUnit.SECONDS).build(); Runtime.getRuntime().addShutdownHook(new Thread()) -> { try {HttpClient.close(); } catch (IOException ignored) { } }); @GetMapping("right") public String right() { try (CloseableHttpResponse response =HttpClient.execute(new HttpGet("http://127.0.0.1:45678/httpclientnotreuse/test")) { return EntityUtils.toString(response.getEntity()); } catch (Exception ex) { ex.printStackTrace(); } return null; } 
```

然后，重新定义一个 wrong2 接口，修复之前按需创建 CloseableHttpClient 的代码，每次用完之后确保连接池可以关闭：

@GetMapping("wrong2")   
public String wrong2() { try(CloseableHttpClient client $=$ HttpClients.custom() .setConnectionManager(new PoolingHttpClientConnectionManager()) .evictIdleConnections(60，TimeUnit.SECONDS).build(); CloseableHttpResponse response $=$ client.execute(new HttpGet("http://127. 0.0.1:45678/httpclientnotreuse/test")){ return EntityUtils.toString(response.getEntity()); } catch(Exception ex){ ex.printStackTrace(); } return null;

使用wrk对wrong2和right两个接口分别压测 $60\mathrm{s}$ ，两种使用方式在性能上有很大差异，每次创建连接池的QPS约为 $337\mathrm{s}$ ，而复用连接池的QPS约为 $2022\mathrm{s}$ ，如图2-26所示。

![](images/ab8c9f9f18508dd3b5d2d79a6127441349e73e02e7433a242dbea83cc63c83b5.jpg)  
图2-26 使用wrk工具通过压测对比复用连接池带来的性能优势

如此大的性能差异显然是因为TCP连接的复用。读者应该注意到了，刚才定义连接池时，我将最大连接数设置为1。所以，复用连接池方式复用的始终应该是同一个连接，而新建连接

池方式应该是每次都会创建新的TCP连接。下面通过网络抓包工具Wireshark来证实这一点。

如果调用 wrong2 接口每次创建新的连接池来发起 HTTP 请求，从图 2-27 中 Wireshark 的结果中可以看到，每次请求服务器端 45678 的客户端端口都是新的。这里发起了 3 次请求，程序通过HttpClient 访问服务器端 45678 端口的客户端端口号分别是 51677、51679 和 51681。

![](images/b35f4a8fdc6a3aefa884a0e1a4e4766bd5f23ccf2b4bca4b9e0ed249ea12b615.jpg)  
图2-27 使用Wireshark工具观察连接不复用问题

也就是说，每次都是新的TCP连接，去掉HTTP这个过滤条件也可以看到完整的TCP握手、挥手的过程，如图2-28所示。

![](images/359c5acda30f95b69e90f4a3779fde5215f71dfa353c26caef8f02306da8e4f4.jpg)  
图2-28 使用Wireshark工具查看完整的TCP连接和断开的过程

而复用连接池方式的接口 right 的表现就完全不同了。如图 2-29 所示，第二次 HTTP 请求 41 号请求的客户端端口 61468 和第一次连接 23 号请求的端口是一样的，Wireshark 也提示了整个 TCP 会话中，当前 41 号请求是第二次请求，前面一次是 23 号后面一次是 75 号：

![](images/0ee302cb6763df2657dd49fb4cda2ba2a13fc0dfda46d289d8bcf447cdd3bfe4.jpg)  
图2-29 使用Wireshark工具观察连接的复用

只有TCP连接闲置超过60s后才会断开，连接池会新建连接。读者可以尝试通过Wireshark观察这一过程。

# 2.4.3 连接池的配置不是一成不变的

为方便根据容量规划设置连接处的属性，连接池提供了许多参数，包括最小（闲置）连接数、最大连接数、闲置连接生存时间、连接生存时间等。其中，最重要的参数是最大连接数，它决定了连接池能使用的连接数量上限，达到上限后新来的请求需要等待其他请求释放连接。

但是，最大连接数不是设置得越大越好。如果设置得太大，客户端需要耗费过多的资源维护连接，更重要的是由于服务器端对应的是多个客户端，每一个客户端都保持大量的连接，会给服务器端带来更大的压力。这个压力又不仅仅是内存压力，可以想一下如果服务器端的网络模型是一个TCP连接一个线程，那么几千个连接意味着几千个线程，如此多的线程会造成大量的线程切换开销。当然，连接池最大连接数设置得太小，很可能会因为获取连接的等待时间太长，导致吞吐量低下，甚至超时无法获取连接。

接下来，我们模拟压力增大导致数据库连接池打满的情况，来练习如何确认连接池的使用情况，以及有针对性地进行参数优化。

首先，定义一个用户注册方法，通过@Transactional注解为方法开启事务。这个用户注册方法包含了 $500\mathrm{ms}$ 的休眠，一个数据库事务对应一个TCP连接，所以占用数据库连接的时间会

超过 $500\mathrm{ms}$

@Transactional   
public User register(){ User user $=$ newUser(); user.setName("new-user- $^+$ System.currentTimeMillis()); userRepository.save(user); try{ TimeUnit.MILLISECONDS.sleep(500); } catch (InterruptedException e){ e.printStackTrace(); } return user;

随后，修改配置文件启用register-mbeans，使Hikari连接池能通过JMX MBean注册连接池相关的统计信息，方便观察连接池。

```txt
springdatasource.hikari.register-mbeans=true 
```

启动程序并通过JConsole连接进程后，默认情况下最大连接数为10，如图2-30所示。

![](images/fd89c3191c305e10d45c4f641a4539ba431e86ab6c8a62760aba9d7c66725d02.jpg)  
图2-30 使用JConsole工具查看连接池相关的MBean

使用wrk对应用进行压测，连接数一下子从0到了10，有20个线程在等待获取连接，如图2-31所示。

![](images/b06b406bec41d9bfb597ed80383bce8c2ec51e6c5ac10d2cf1cb42a5ba812588.jpg)  
图2-31 使用JConsole工具可以发现连接池存在打满现象

不久就出现了无法获取数据库连接的异常，如下所示：

```txt
[15:37:56.156] [http-nio-45678-exec-15] [ERROR] [.a.c.c.C.[.][/].  
[dispatcherServlet]:175 ] - Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is org.springframework.dao.DataSourceResourceFailureException: unable to obtain isolated JDBC connection; nested exception is org.hibernate. exception.JDBCConnectionException: unable to obtain isolated JDBC connection] with root cause java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30000ms. 
```

可以看到，数据库连接池是HikariPool。解决方式很简单，修改配置文件，调整数据库连接池最大连接参数为50即可：

```txt
spring.datasource.hikari.maximum-pool-size=50 
```

然后，再观察这个参数是否适合当前压力，满足需求的同时也不占用过多资源，如图2-32所示。从监控来看这个调整是合理的，有一半的富余资源，再也没有线程需要等待连接了。

![](images/bfa56c8f69dedd6b37afc855f62c5d74376fcd2b2a1d0624006b938a7e9c3691.jpg)  
图2-32 扩容后再次使用JConsole工具观察到连接池已经不存在打满问题

在这个演示示例中，我知道压测大概对应使用25个左右的并发连接，所以直接把连接池最大连接数设置为了50。在真实情况下，只要数据库可以承受，读者可以选择在遇到连接超限的时候先设置一个足够大的连接数，然后观察最终应用的并发，再按照实际并发数留出一半的裕量来设置最终的最大连接。其实，看到错误日志后再调整已经有点儿晚了。更合适的做法是，对类似数据库连接池的重要资源进行持续检测，并设置一半的使用量作为报警阈值，出现预警后及时扩容。

为了演示效果，本示例才通过JConsole查看参数配置后的效果，在生产环境上需要把相关数据对接到指标监控体系中持续监测。

要强调的是，修改配置参数务必验证是否生效，并且在监控系统中确认参数是否生效、是否合理。之所以要“强调”，是因为这里有坑。我遇到过这样一个事故。应用准备针对大促活动进行扩容，把数据库配置文件中 Druid 连接池的最大连接数 maxActive 从 50 提高到了 100，修改后并没有通过监控验证，结果大促当天应用因为连接池连接数不够爆了。经排查发现，当时修改的连接数并没有生效。原因是，应用虽然一开始使用的是 Druid 连接池，但后来框架升级了，把连接池替换为了 Hikari 实现，原来的那些配置其实都是无效的，修改后的参数配置当然也不会生效。所以，对连接池进行调参，一定要眼见为实。

# 2.4.4 小结

本节以Redis连接池、HTTP连接池、数据库连接池为例，讲解了有关连接池实现方式、使用方式和参数配置的3个问题。

- 客户端SDK实现连接池的方式，包括池和连接分离、内部带有连接池和非连接池3种。要正确使用连接池，就必须首先鉴别连接池的实现方式。例如，Jedis的API实现的是池和连接分离的方式，而ApacheHttpClient是内置连接池的API。  
- 对于使用方式其实就两点，一是确保连接池是复用的，二是尽可能在程序退出之前显式关闭连接池释放资源。连接池设计的初衷是保持一定量的连接，这样连接可以随取随用。从连接池获取连接虽然很快，但连接池的初始化会比较慢，需要做一些管理模块的初始化并初始最小闲置连接。一旦连接池不是复用的，其性能会比随时创建单一连接更差。  
- 连接池参数配置中，最重要的是最大连接数，许多高并发应用往往因为最大连接数不够导致性能问题。但是，最大连接数不是设置得越大越好，而是够用就好。需要注意的是，针对数据库连接池、HTTP连接池、Redis连接池等重要的连接池，务必建立完善的监控和报警机制，根据容量规划及时调整参数配置。

# 2.4.5 思考与讨论

1. 有了连接池之后，获取连接是从连接池获取，没有足够连接时连接池会创建连接。这时，获取连接操作往往有两个超时时间：一个是从连接池获取连接的最长等待时间，通常叫作请求连接超时 connectRequestTimeout 或连接等待超时 connectWaitTimeout；一个是连接池新建 TCP 连接三次握手的连接超时，通常叫作连接超时 connectTimeout。针对 JedisPool、Apache HttpClient 和 Hikari 数据库连接池，假设我们希望设置连接超时 5s、请求连接超时 10s，如何设置这两个参数呢？

针对Hikari，设置两个超时时间的方式是修改数据库连接字符串中的connectTimeout属性和配置文件中的hikari配置的connection-timeout：

```txt
springdatasource.hikari.Connection-timeout=10000 
```

springdatasource.url $\equiv$ jdbc:mysql://localhost:6657/common_mistakes?connectTimeout= 5000&characterEncoding $\equiv$ UTF-8&useSSL $\equiv$ false&rewriteBatchedStatements $\equiv$ true

针对Jedis，是设置JedisPoolConfig的MaxWaitMillis属性和设置创建JedisPool时的timeout属性：

```txt
JedisPoolConfig config = new RedisPoolConfig();  
config.setMaxWaitMillis(10000);  
try (JedisPool jedisPool = new RedisPool(config, "127.0.0.1", 6379, 5000); 
```

```javascript
Jedis jedis = jedisPool.getResource(); { return jedis.set("test", "test"); } 
```

针对HttpClient，是设置RequestConfig的ConnectTimeout和ConnectionRequestTimeout属性：

```java
RequestConfig requestConfig = RequestConfig.custom()
    .setConnectTimeout(5000)
    .setConnectionRequestTimeout(10000)
    .build();
HttpGet httpGet = new HttpGet("http://127.0.0.1:45678/twotimeoutconfig/test");
HttpGet.setConfig(requestConfig);
try (CloseableHttpResponse response =HttpClient.execUTE(httpGet)) {
    return EntityUtiles.toString(response.getEntity());
} catch (Exception ex) {
    ex.printStackTrace();
}
return null; 
```

2. 对于带有连接池的SDK的使用，最主要的是鉴别其内部是否实现了连接池，如果实现了连接池要尽量复用Client。对非关系数据库中的MongoDB来说，使用MongoDB Java驱动时，MongoClient类应该是每次都创建还是复用呢？

官方文档里有下面这么一段话：

```txt
Typically you only create one MongoClient instance for a given MongoDB deployment (e.g. standalone, replica set, or a sharded cluster) and use it across your application. However, if you do create multiple instances: All resource usage limits (e.g. max connections, etc.) apply per MongoClient instance. To dispose of an instance, call MongoClient.close() to clean up resources. 
```

MongoClient类应该尽可能复用（一个MongoDB部署只使用一个MongoClient），不过复用不等于在任何情况下就只用一个。正如文档中所说，每一个MongoClient示例有自己独立的资源限制。

# 2.5 HTTP调用：你考虑超时、重试、并发了吗

与执行本地方法不同，进行HTTP调用本质上是通过HTTP协议进行一次网络请求。网络请求必然有超时的可能性，因此必须考虑如下3点。

- 框架设置的默认超时是否合理。  
- 因为网络不稳定，超时后的请求重试是一个不错的选择，但需要考虑服务器端接口的幂等性设计是否允许重试。  
- 框架是否会像浏览器那样限制并发连接数，以避免在服务并发很大的情况下，HTTP调用的并发数限制成为瓶颈。

Spring Cloud 是 Java 微服务架构的代表性框架。如果使用 Spring Cloud 进行微服务开发，就会使用 Feign 进行声明式的服务调用。如果不使用 Spring Cloud 而直接使用 Spring Boot 进行微服务开发的话，可能会直接使用 Java 中非常常用的 HTTP 客户端 Apache HttpClient 进行服务调用。本节将讲述使用 Feign 和 Apache HttpClient 进行 HTTP 接口调用时，可能会遇到的超时、重试和并发相关的坑。

# 2.5.1 配置连接超时和读取超时参数的学问

对于 HTTP 调用，虽然应用层走的是 HTTP 协议，但网络层面始终是 TCP/IP 协议。TCP/IP 是面向连接的协议，在传输数据之前需要建立连接。几乎所有的网络框架都会提供如下两个超时参数。

- 连接超时参数 ConnectTimeout，让用户配置建连阶段的最长等待时间。  
- 读取超时参数 ReadTimeout，用来控制从套接字上读取数据的最长等待时间。

这两个参数看似是网络层偏底层的配置参数，不足以引起开发人员的重视，但是正确理解和配置这两个参数对业务应用特别重要，毕竟超时不是单方面的事情，需要客户端和服务器端对超时有一致的估计，协同配合才能平衡吞吐量和错误率。

连接超时和连接超时参数相关的误区有两个。

- 连接超时配置得特别长，比如 $60\mathrm{s}$ 。一般来说，TCP三次握手建立连接需要的时间非常短，通常在毫秒级最多到秒级，不可能需要十几秒甚至几十秒。如果很久都无法建立连接，很可能是网络或防火墙配置的问题。这种情况下，如果几秒连接不上，那么可能永远也连接不上。因此，设置特别长的连接超时意义不大，建议将其配置得短一些（如 $1 \sim 5\mathrm{s}$ ）。如果是纯内网调用的话，这个参数可以设置得更短，在下游服务离线无法连接的时候，可以快速失败。  
- 排查连接超时问题却没厘清连的是哪里。通常情况下，我们的服务会有多个节点，如果别的客户端通过客户端负载均衡技术来连接服务器端，那么客户端和服务器端会直接建立连接，此时出现连接超时大概率是服务器端的问题；而如果服务器端通过类似 Nginx 的反向代理来负载均衡，客户端连接的其实是 Nginx，而不是服务器端，此时出现连接超时应该排查 Nginx。

读取超时和读取超时参数则会有更多误区，我将其归纳为如下3个。

（1）认为出现了读取超时，服务器端的执行就会中断。

我们来测试一下。定义一个接口 client，内部通过HttpClient调用服务器端接口 server，客户端读取超时2s，服务器端接口执行耗时5s。

```java
@RestController   
@RequestMapping("clientreadtimeout")   
@Slf4j   
public class ClientReadTimeoutController { private String getResponse(String url, int connectTimeout, int readTimeout) throws IOException { return Request.Get("http://localhost:45678/clientreadtimeout" +url) .connectTimeout.connectTimeout) .socketTimeout(readTimeout) .execute() .returnContent(); .string();   
}   
@GetMapping("client")   
public String client() throws IOException { log.info("client1 called"); //服务器端5s超时，客户端读取超时2s return getResponse("/server?timeout=5000",1000，2000);   
}   
@GetMapping("server") 
```

```java
public void server(@RequestParam("timeout") int timeout) throws InterruptedException {
    log.info("server called");
    TimeUnit.MILLSECONDS.sleep_timeout);
    log.info("Done");
} 
```

调用 client 接口后，从日志中可以看到，客户端 2s 后出现了 SocketTimeoutException，原因是读取超时，服务器端却丝毫没受影响在 3s 后执行完成：

```txt
[11:35:11.943] [http-nio-45678-exec-1] [INFO ]
.[.t.c.c.d ClientReadTimeoutController:29 ] - client1 called
[11:35:12.032] [http-nio-45678-exec-2] [INFO ]
.[.t.c.c.d ClientReadTimeoutController:36 ] - server called
[11:35:14.042] [http-nio-45678-exec-1] [ERROR]
[a.c.c.c.[.][/].[dispatcherservlet]:175 ] - Servlet.service() for servlet
[dispatcherservlet] in context with path [] threw exception
java.net.SocketTimeoutException: Read timed out
at java.net.SocketInputStream(socketRead0 (Native Method)
...
[11:35:17.036] [http-nio-45678-exec-2] [INFO ] [.t.c.c.d ClientReadTimeoutControl 
```

类似 Tomcat 的 Web 服务器都是把服务器端请求提交到线程池处理的，只要服务器端收到了请求，网络层面的超时和断开便不会影响服务器端的执行。因此，出现读取超时不能随意假设服务器端的处理情况，需要根据业务状态考虑如何进行后续处理。

（2）认为读取超时只是网络层面的概念，是数据传输的最长耗时，因此将其配置得非常短（如 $100\mathrm{ms}$ ）。

其实，发生了读取超时，网络层面无法区分是服务器端没有把数据返回给客户端，还是数据在网络上耗时较久或丢包。但是因为TCP是先建立连接后传输数据，对于网络情况不是特别糟糕的服务调用，通常可以认为出现连接超时是网络问题或服务不在线，而出现读取超时是服务处理超时。确切地说，读取超时指的是，向套接字写入数据后我们等到套接字返回数据的超时时间，其中包含的时间或者说绝大部分的时间是服务器端处理业务逻辑的时间。

（3）认为超时时间越长任务接口成功率就越高，将读取超时参数配置得太长。

进行 HTTP 请求一般是需要获得结果的，属于同步调用。如果超时时间很长，在等待服务器端返回数据的同时，客户端线程（通常是 Tomcat 线程）也在等待，当下游服务出现大量超时的时候，程序可能也会受到拖累创建大量线程，最终崩溃。对定时任务或异步任务来说，读取超时配置得长些问题不大。但面向用户响应的请求或是微服务短平快的同步接口调用，并发量一般较大，我们应该设置一个较短的读取超时时间，以防止被下游服务拖慢，通常不会设置超过 30s 的读取超时。

如果把读取超时设置为2s，服务器端接口需要3s，岂不是永远都拿不到执行结果了？的确是这样，因此设置读取超时一定要根据实际情况，过长可能会让下游抖动影响到自己，过短又可能影响成功率。甚至，有些时候我们还要根据下游服务的SLA，为不同的服务器端接口设置不同的客户端读取超时。

# 2.5.2 Feign和Ribbon配合使用，你知道怎么配置超时吗

2.5.1节中强调了根据自己的需求配置连接超时和读取超时的重要性，你是否尝试过为Spring Cloud的Feign配置超时参数呢，有没有被各种资料绕晕呢？

在我看来，为Feign配置超时参数的复杂之处在于，Feign自己有两个超时参数，它使用的负载均衡组件Ribbon本身还有相关配置。那么，这些配置的优先级是怎样的，又有哪些坑呢？

为测试服务器端的超时，假设有如下这么一个服务器端接口，它什么都不干只休眠 $10\mathrm{min}$ ：

```txt
@PostMapping("/server") public void server() throws InterruptedException { TimeUnit.MINUTES.sleep(10); } 
```

在配置文件仅指定服务器端地址的情况下：

```txt
clientsdk. ribbon. listOfServers=localhost:45678 
```

得到如下输出：

```txt
[15:40:16.094] [http-nio-45678-exec-3] [WARN] [o.g.t.c.h.f.FeignAndRibbonController 在我 :26 ] - 执行耗时：1007ms 错误；Read timed out executing POST http://clientsdk/ feignandribbon/server 
```

从这个输出中，可以得到如下5个结论。

（1）默认情况下Feign的读取超时是1s，如此短的读取超时算是坑点一。

分析一下源码。打开RibbonClientConfiguration类后，会看到DefaultClientConfigImpl被创建出来之后，ReadTimeout和ConnectTimeout被设置为1s：

/\*\* \*Ribbon client default connect timeout.   
public static final int DEFAULT_connect_TIMEOUT $= 1000$ .   
/\*\* \*Ribbon client default read timeout.   
public static final int DEFAULT_READ_TIMEOUT $= 1000$ @Bean   
@ConditionalOnMissingBean   
public IClientConfig ribbonClientConfig() { DefaultClientConfigImpl config $=$ new DefaultClientConfigImpl(); config.loadProperties(this.name); config.set(CommonClientConfigKey.ConnectTimeout, DEFAULT_CONNECT_TIMEOUT); config.set(CommonClientConfigKey.ReadTimeout, DEFAULT_READ_TIMEOUT); config.set(CommonClientConfigKey.GZipPayload, DEFAULT_GZIP_PAYLOAD); return config;

如果要修改 Feign 客户端默认的两个全局超时时间，可以设置 feign.client.config.default.readTimeout 和 feign.client.config.default.connectTimeout 参数：

feign.client.config.default.readTimeout $= 3000$ feign.client.config.default.connectTimeout $= 3000$

修改配置后重试，得到如下日志：

```txt
[15:43:39.955] [http-nio-45678-exec-3] [WARN ]
[o.g.t.c.h.f.FeignAndRibbonController :26 ] - 执行耗时：3006ms 错误：Read timed out
executing POST http://clientsdk/feignandribbon/server 
```

可见，3s读取超时生效了。注意：这里有一个大坑，如果你希望只修改读取超时，可能会只配置这么一行：

```txt
feign.client.config.default.readTimeout=3000 
```

测试一下就会发现，这样的配置是无法生效的！

（2）如果要配置Feign的读取超时，就必须同时配置连接超时，才能生效，也是坑点二。

打开 FeignClientFactoryBean 可以看到，只有同时设置 ConnectTimeout 和 ReadTimeout, RequestOPTIONS 才会被覆盖：

```java
if (config.getConnectionTimeout() != null && config.getConnectionTimeout() != null) { builder(options(new Request.Options(config.getConnectionTimeout(), config.getConnectionTimeout())); } 
```

更进一步，如果希望针对单独的FeignClient设置超时时间，可以把default替换为Client的name：

feign.client.config.default.readTimeout $= 3000$ feign.client.config.default.connectTimeout $= 3000$ feign.client.config.clientsdk.readTimeout $= 2000$ feign.client.config.clientsdk.connectTimeout $= 2000$

（3）单独的超时可以覆盖全局超时。这符合预期，不算坑点：

```txt
[15:45:51.708] [http-nio-45678-exec-3] [WARN ]
[o.g.t.c.h.f.FeignAndRibbonController :26 ] - 执行耗时：2006ms 错误：Read timed out
executing POST http://clientsdk/feignandribbon/server 
```

（4）除了可以配置Feign，也可以配置Ribbon组件的参数来修改两个超时时间。和Feign的配置不同，Ribbon的配置参数首字母要大写，这是坑点三：

```txt
ribbon.ReadTimeout=4000  
ribbon.ConnectTimeout=4000 
```

可以通过日志证明参数生效：

```txt
[15:55:18.019] [http-nio-45678-exec-3] [WARN ]
[o.g.t.c.h.f.FeignAndRibbonController :26 ] - 执行耗时：4003ms 错误：Read timed out
executing POST http://clientsdk/feignandribbon/server 
```

如果同时配置Feign和Ribbon的参数，最终谁会生效？如下代码的参数配置：

```python
clientsdk. ribbon.listOfServers=localhost:45678  
feign.client.config.default.readTimeout=3000  
feign.client.config.default.connectTimeout=3000  
ribbon.ReadTimeout=4000  
ribbon.ConnectTimeout=4000 
```

日志输出证明，最终生效的是Feign的超时：

```txt
[16:01:19.972] [http-nio-45678-exec-3] [WARN ]
[o.g.t.c.h.f.FeignAndRibbonController :26 ] - 执行耗时：3006ms 错误：Read timed out
executing POST http://clientsdk/feignandribbon/server 
```

（5）同时配置Feign和Ribbon的超时，以Feign为准。这有点反直觉，因为Ribbon更底层所以我们往往认为它的配置会生效，但其实不是。

在 LoadBalancerFeignClient 源码中可以看到，如果 Request.Options 不是默认值，就会创建一个 FeignOptionsClientConfig 代替原来 Ribbon 的 DefaultClientConfigImpl，导致 Ribbon 的配置被 Feign 覆盖：

```java
IClientConfig getClientConfig.Request.Options options, String clientName) {  
    IClientConfig requestConfig;  
    if (options == DEFAULT_OPTIONS) {  
        requestConfig = this.clientFactory.getClientConfig(clientName);  
    }  
    else {  
        requestConfig = new FeignOptionsClientConfig(options);  
    }  
    return requestConfig; 
```

但如果这么配置最终生效的还是Ribbon的超时（4s），就容易让人产生Ribbon覆盖了Feign的错觉，其实这还是因为坑点二，单独配置Feign的读取超时并不能生效：

```txt
clientsdk. ribbon.listOfServers=localhost:45678 feign.client.config.default.readTimeout=3000 feign.client.config clientsdk.readTimeout=2000 ribbon.ReadTimeout=4000 
```

# 2.5.3 你知道Ribbon会自动重试请求吗

一些 HTTP 客户端往往会内置一些重试策略，其初衷是好的，毕竟因为网络问题导致丢包虽然频繁但持续时间短，往往重试下第二次就能成功，但一定要小心这种自作主张是否符合我们的预期。之前遇到过一个短信重复发送的问题，但反复确认短信服务的调用方用户服务，代码里没有重试逻辑。那问题究竟出在哪里了？我们重现一下这个案例。

首先，定义一个GET请求的发送短信接口，里面没有任何逻辑，休眠2s模拟耗时：

```java
@RestController   
@RequestMapping("ribbonretryissueserver")   
@Slf4j   
public class RibbonRetryIssueServerController { @GetMapping("sms") public void sendSmsWrong(@RequestParam("mobile") String mobile, @RequestParam ("message") String message, HttpServletRequest request) throws错了Exception{ //输出调用参数后休眠2s log.info({} is called，{}=>{}"，request.getParameter(). toString()，mobile，message); TimeUnit.SECONDS.sleep(2); } 
```

Feign 内部有一个 Ribbon 组件负责客户端负载均衡，通过配置文件设置其调用的服务器端为两个节点：

```txt
SmsClient. ribbon. listOfServers=localhost:45679,localhost:45678 
```

写一个客户端接口，通过Feign调用服务器端：

```java
@RestController   
@RequestMapping("ribbonretryissueclient")   
@Slf4j   
public class RibbonRetryIssueClientController { @Autowired private SrmsClient smsClient; @GetMapping("wrong") 
```

```java
public String wrong() { log.info("client is called"); try{ //通过Feign调用发送短信接口 smsClient.sendSmsWrong("13600000000", UUID.randomUUID().toString()); } catch(Exception ex) { //捕获可能出现的网络错误 log.error("send sms failed:{}",ex.getMessage()); } return "done"; } 
```

在45678和45679这两个端口上分别启动服务器端，然后访问45678的客户端接口进行测试。因为客户端和服务器端Controller在一个应用中，所以45678同时扮演了客户端和服务器端的角色。在端口号为45678的服务器端的日志中可以看到，第29秒时客户端收到请求开始调用服务器端接口发短信，同时服务器端收到了请求，2s后客户端输出了读取超时的错误信息：

```ini
[12:49:29.020] [http-nio-45678-exec-4] [INFO ]
[c.d.RibbonRetryIssueClientController:23 ] - client is called
[12:49:29.026] [http-nio-45678-exec-5] [INFO ]
[c.d.RibbonRetryIssueServerController:16 ] - http://localhost:45678/
ribbonretryissueserver/sms is called, 1360000000 => a2aa1b32-a044-40e9-8950-
7f0189582418
[12:49:31.029] [http-nio-45678-exec-4] [ERROR] [c.d.RibbonRetryIssueClientController:
27 ] - send sms failed : Read timed out executing GET http://SmsClient/
ribbon retry issueserver/sms?mobile=1360000000&message=a2aa1b32-a044-40e9-8950-
7f0189582418 
```

而在另一个端口号为45679的服务器端的日志中还可以看到一条请求，30s时（也就是客户端接口调用后的1s）收到请求：

```txt
[12:49:30.029] [http-nio-45679-exec-2] [INFO] [c.d.RibbonRetryIssueServerController:16] - http://localhost:45679/ribbonretryissueserver/sms is called, 1360000000 => a2aalb32-a044-40e9-8950-7f0189582418 
```

客户端接口被调用的日志只输出了一次，而服务器端的日志输出了两次。虽然Feign的默认读取超时时间是1s，但客户端2s后才出现超时错误。这说明客户端自作主张进行了一次重试，导致短信重复发送。Ribbon的源码如下，可以发现，MaxAutoRetriesNextServer参数默认为1，也就是GET请求在某个服务器端节点出现问题（如读取超时）时，Ribbon会自动重试一次：

// DefaultClientConfigImpl   
public static final int DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER $= 1$ public static final int DEFAULT_MAX_AUTO_RETRIES $= 0$ ：   
//RibbonLoadBalancedRetryPolicy   
public boolean canRetry_LoadBalancedRetryContext context）{ HttpMethod method $=$ context.request().getMethod(); return HttpMethod.GET $= =$ method || lbContext.isOkToRetryOnAllOperations();   
}   
@override   
public boolean canRetrySameServer_LoadBalancedRetryContext context）{ return sameServerCount $<$ lbContext.getRetryHandler(). getMaxRetriesOnSameServer()&& canRetry(context);   
}   
@override

public boolean canRetryNextServer_LoadBalancedRetryContext context) { // this will be called after a failure occurs and we increment the counter // so we check that the count is less than or equals to too make sure // we try the next server the right number of times return nextServerCount $<  =$ lbContext.getRetryHandler(). getMaxRetriesOnNextServer()&& canRetry(context); }

解决办法有以下两个。

- 把发短信接口从GET改为POST，还有一个API设计问题，有状态的API接口不应该定义为GET。根据HTTP协议的规范，GET请求用于数据查询，而POST才是把数据提交到服务器端用于修改或新增。选择GET还是POST的依据，应该是API的行为而不是参数大小。这里的误区是，GET请求的参数包含在UrlQueryString中，会受浏览器长度限制，所以一些开发人员会选择使用JSON以POST提交大参数，使用GET提交小参数。  
- 将MaxAutoRetriesNextServer参数配置为0，禁用服务调用失败后在下一个服务器端节点的自动重试。在配置文件中添加如下一行即可：

```gitattributes
ribbon.MaxAutoRetriesNextServer=0 
```

那么，问题出在用户服务还是短信服务呢？在我看来，双方都有问题。就像之前说的，GET请求应该是无状态或者幂等的，短信接口可以设计为支持幂等调用；而用户服务的开发人员，如果对Ribbon的重试机制有所了解，就能在排查问题上少走些弯路。

# 2.5.4 并发限制了爬虫的抓取能力

除了超时和重试的坑，进行 HTTP 请求调用还有一个常见的问题是，并发数的限制导致程序的处理能力上不去。

我之前遇到过一个爬虫项目，整体爬取数据的效率很低，增加线程池数量也无济于事，只能堆更多的机器做分布式的爬虫。我们模拟下这个场景，看看问题出在了哪里。

假设要爬取的服务器端是这样的一个简单实现，休眠1s返回数字1：

```java
@GetMapping("server")   
public int server() throws扰乱Exception { TimeUnit.SECONDS.sleep(1); return 1; 
```

爬虫需要多次调用这个接口进行数据抓取，为了确保线程池不是并发的瓶颈，我们使用一个没有线程上限的 newCachedThreadPool 作为爬取任务的线程池（再次强调，除非非常清楚自己的需求，否则不要使用没有线程数量上限的线程池），使用HttpClient 实现 HTTP 请求，把请求任务循环提交到线程池处理，等待所有任务执行完成后输出执行耗时：

```java
private int sendRequest(int count, Supplier<CloseableHttpClient> client) throws扰乱Exception {
    // 用于计数发送的请求个数
    AtomicInteger atomicInteger = new AtomicInteger();
    // 把使用HttpClient从server接口查询数据的任务提交到线程池并行处理
    ExecutorService threadPool = Executors.newCachedThreadPool();
    long begin = System.currentTimeMillis();
    IntStream rangeClosed(1, count).forEach(i -> {
        threadPool.exec()(-> {
            try (CloseableHttpResponse response = client.get().execute(new HttpGet("http://127.0.0.1:45678/routedommit/server)))
        }
    }
}; 
```

```txt
java.lang.Integer addAndGet(Integer.parseIntEntity Utilities.toString(responseEntity())));   
} catch (Exception ex) {   
    ex.printStackTrace();   
}   
});   
}）;   
//等count个任务全部执行完毕   
threadPool.shutdown();   
threadPool awaitTermination(1, TimeUnit.HOURS);   
log.info("发送{}次请求，耗时{}ms", atomicInteger.get(), System.currentTimeMillis Millis() - begin);   
return atomicInteger.get(); 
```

使用默认的PoolingHttpClientConnectionManager构造的CloseableHttpClient，测试爬取10次的耗时：

```java
static CloseableHttpClient httpClient1;   
static { httpClient1 = HttpClients(custom().setConnectionManager(new PoolingHttpClientConnectionManager().build();   
}   
@GetMapping("wrong")   
public int wrong(@RequestParam(value = "count", defaultValue = "10") int count) throws InterruptedException { return sendRequest(count, () -> httpClient1); 
```

虽然一个请求需要1s执行完成，但是我们的线程池是可以扩张为使用任意数量线程的。按道理说，10个请求并发处理的时间基本相当于1个请求的处理时间，也就是1s，但日志中显示实际耗时约5s：

```txt
[12:48:48.122] [http-nio-45678-exec-1] [INFO] [o.g.t.c.h.r.RouteLimitController : 54 ] - 发送 10 次请求，耗时 5265 ms 
```

查看PoolingHttpClientConnectionManager的源码，可以注意到有两个重要参数。

- defaultMaxPerRoute=2，也就是同一个主机/域名的最大并发请求数为2。本爬虫项目需要10个并发，显然是默认值太小限制了爬虫的效率。  
- maxTotal=20，也就是所有主机整体最大并发为20，这也是HttpClient整体的并发度。本爬虫项目的请求数是10（最大并发是10），20不会成为瓶颈。举一个例子，使用同一个HttpClient访问10个域名，defaultMaxPerRoute设置为10，为确保每一个域名的并发数都能达到10，需要把maxTotal设置为100。

具体的实现代码如下：

```txt
public PoolingHttpClientConnectionManager( final HttpClientConnectionOperator httpClientConnectionOperator, final HttpConnectionFactory<HttpRote, ManagedHttpClientConnection> connFactory, final long timeToLive, final TimeUnit timeUnit) { this.pool = new CPool(new InternalConnectionFactory( this.configData, connFactory), 2, 20, timeToLive, timeUnit); } 
```

```java
public CPool( final ConnFactory<HttpRoute, ManagedHttpClientConnection>connFactory, final int defaultMaxPerRoute, final int maxTotal, final long timeToLive, final TimeUnit timeUnit) { } 
```

HttpClient是Java中非常常用的HTTP客户端，这个问题也经常出现。你可能会问，为什么默认值限制得这么小。其实，这不能完全怪HttpClient，很多早期的浏览器也限制了同一个域名两个并发请求。对于同一个域名并发连接的限制，其实是HTTP1.1协议要求的，HTTP1.1协议规范中有这么一段话：

Clients that use persistent connections SHOULD limit the number of simultaneous connections that they maintain to a given server. A single-user client SHOULD NOT maintain more than 2 connections with any server or proxy. A proxy SHOULD use up to $2^{*}N$ connections to another server or proxy, where N is the number of simultaneously active users. These guidelines are intended to improve HTTP response times and avoid congestion.

HTTP 1.1 协议是 1997 年制定的，现在 HTTP 服务器的能力强很多了，所以有些新的浏览器没有完全遵从“同一个主机/域名的最大并发请求数为 2”这个限制，将最大并发请求数改为了 8 甚至更大。如果需要通过 HTTP 客户端发起大量并发请求，那么不管使用什么客户端，都必须确认客户端的实现默认的并发数是否满足需求。既然知道了问题所在，我们就尝试声明一个新的 HttpClient 放开相关限制，设置 maxPerRoute 为 50、maxTotal 为 100，并修改刚才的 wrong 方法，使用新的客户端进行测试：

```javascript
httpClient2 = HttpClients(custom().setMaxConnPerRoute(10).setMaxConnTotal(20).build(); 
```

输出如下，10次请求在1s左右执行完成。可以看到，因为放开了Host最大并发数为2的默认限制，爬虫效率得到了大幅提升：

```txt
[12:58:11.333] [http-nio-45678-exec-3] [INFO]  
[o.g.t.c.h.rRouteLimitController 54 ] - 发送10次请求，耗时1023ms 
```

# 2.5.5 小结

连接超时代表建立TCP连接的时间，读取超时代表等待远端返回数据的时间，也包括远端程序处理的时间。在解决连接超时问题时，我们要搞清楚连接的是谁；在遇到读取超时问题时，我们要综合考虑下游服务的服务标准和自己的服务标准，设置合适的读取超时时间。此外，在使用诸如Spring Cloud Feign等框架时务必确认，连接和读取超时参数的配置是否正确生效。

对于重试，因为 HTTP 协议认为 GET 请求是数据查询操作，是无状态的，又考虑到网络丢包比较常见，有些 HTTP 客户端或代理服务器会自动重试 GET/HEAD 请求。如果你的接口设计不支持幂等，需要关闭自动重试。但更好的解决方案是，遵从 HTTP 协议（在搜索引擎搜索“RFC 9110”可以找到 HTTP 协议的规范）的建议来使用合适的 HTTP 方法。

包括HttpClient在内的HTTP客户端和浏览器，都会限制客户端调用的最大并发数。如果你的客户端有比较大的请求调用并发，例如做爬虫，或是扮演类似代理的角色，又或是程序本身并发较高，如此小的默认值很容易成为吞吐量的瓶颈，需要及时调整。

# 2.5.6 思考与讨论

# 1. 为什么很少看到“写入超时”的概念？

其实写入操作只是将数据写入TCP的发送缓冲区，已经发送到网络的数据依然需要暂存在发送缓冲区中，只有收到对方的ACK后，操作系统内核才从缓冲区中清除这一部分数据，为后续发送数据腾出空间。

如果接收端从套接字读取数据的速度太慢，可能会因为发送端发送缓冲区满导致写入操作阻塞，产生写入超时。但是，因为有滑动窗口的控制，通常不太容易发生发送缓冲区满导致写入超时的情况。相反，读取超时包含了服务器端处理数据执行业务逻辑的时间，所以读取超时是比较容易发生的。

这也就是为什么我们一般会比较重视读取超时而不是写入超时的原因了。

# 2. Nginx 也有类似 AutoRetriesNextServer 的重试功能，你了解吗？

关于 Nginx 的重试功能，你可以通过在搜索引擎搜索“Nginx proxy_next_upstream”了解 proxy_next_upstream 配置。

proxy_next_upstream 用于指定在什么情况下 Nginx 会将请求转移到其他服务器上，其默认值是“error timeout”，即发生网络错误和超时，才会重试其他服务器。也就是说，默认情况下，服务返回 500 状态码是不会重试的。

如果我们想在请求返回500状态码时也进行重试，可以按如下方式配置：

```txt
proxy_next_upstream error timeout http_500; 
```

需要注意的是，proxy_next_upstream配置中有一个选项non_idempotent，一定要小心开启。通常情况下，如果请求使用非等幂方法（POST、PATCH），请求失败后不会再到其他服务器进行重试。但是，加上non_idempotent这个选项后，即使是非幂等请求类型（如POST请求），发生错误后也会重试。

# 2.5.7 扩展阅读

包括HttpClient在内的HTTP客户端和浏览器，都会限制客户端调用的最大并发数，其实是一个容量问题的坑点。我将Java开发中还会遇到的容量限制问题总结为5类，如图2-33所示。

![](images/7cf955d3521b14eeb3e93aaba3fd2828415349e48decc1a243cdce48c6950050.jpg)  
图2-33 容量问题层次关系拆解

（1）操作系统/物理机/容器。操作系统/物理机/容器或它们对应的基础设的资源一般是运维人员在负责，我们需要在申请新资源时和运维人员一起确定资源在这个层面没有瓶颈。

- 操作系统参数设置：现在服务器配置都很高了，默认参数不一定可以释放服务器性能，新的服务器往往需要进行初始优化，放开一些限制，如文件句柄等。

- 物理机或容器的一些资源限制：如带宽是否给足了，CPU、内存、磁盘空间是否足够。

（2）Web容器/网络外壳。Web容器/网络外壳是指Tomcat这样的容器，也就是运行JavaWeb程序的容器。我们需要考虑一些资源限制类参数。

- 最大连接数：决定最多服务于多少客户端，对于 NIO（非阻塞 IO，Non-Blocking IO）通常不要设置过小。  
- 最大线程数：决定并发处理能力，对于NIO线程和网络连接不是1对1，通常线程数不用太大，一般Web服务一个CPU核心对应的线程数超过500个，服务就已经崩溃。

以上两类是程序外部环境或容器的资源限制，下面三类则是程序内部的资源限制。

（3）数据库连接池。最重要的参数就是最大连接数，设置得太小，慢查询或者大事务多了，并发连接数会上去就会不够用；设置得太大，单一服务可能会影响整个数据库。

（4）HttpClient。

- 一个域名并发连接数：一般来说，设置为2对目前微服务架构来说太少了，需要设置得大点，比如Feign默认的50。  
- 所有域名并发连接数：一般来说，设置为20对目前微服务架构来说太少了，可以设置得大点，比如Feign默认的200。

（5）业务线程池。线程池的如下3个重要参数可能会成为限制。

- 核心线程数：一般设置为 $N$ 或 $2N(N =$ 核数）设置得过小处理能力不足，设置得过大不仅没意义，还可能影响整个服务。  
- 最大线程数：一般可以设置为 $1 \sim 3$ 倍的核心线程数。如果阻塞队列很大可能永远达不到最大线程，这种情况下可以考虑弹性线程池。  
- 阻塞队列长度：一般设置为 $500\sim 2000$ 。设置过小，线程抖动厉害且容易拒绝服务；设置太大，可能OOM丢数据带来影响也大。

操作系统层面还可能踩的坑是：使用supervisord来管理进程（supervisord是一个常用的进程管理工具，不仅可以简化进程的启动操作，还可以在进程意外退出时自动重启）时，supervisord中的参数minfds（默认1024）和minprocs（默认200）决定了supervisord进程及守护的子进程的最大处理器数和最大打开文件数，并且这个限制不受系统ulimit影响。这两个参数的默认值非常小，可能会出现非常大的限制问题。这个坑告诉我们，在遇到限制问题时要层层剥茧，如下所示。

- Java 代码本身使用的核心组件，如连接池、线程池、HttpClient 是否有限制？  
- Java 代码运行在 Tomcat 容器中，Tomcat 可能有限制？  
- Java 代码通过 supervisord 启动，supervisord 可能有限制？  
- Java 代码也会运行在操作系统以及物理机/容器/Pod 中，它们是否有限制？  
- 物理机/容器/Pod使用的底层网络资源或机房资源（我遇到过机房功率限制导致服务器降频，进而引发性能问题的案例）是否有限制？

在新的设备、操作系统、代码上线之前，我建议做压测来确认层层链路是否有意料之外的瓶颈。在压测的时候可以观察各种监控面板，当监控曲线在慢慢上升后呈现一条直线的时候，就可能遇到了限制类容量问题，需要关注和分析。

# 2.6 $20\%$ 的业务代码的Spring声明式事务可能都没处理正确

Spring针对Java Transaction API(JTA)、JDBC、Hibernate和Java Persistence API(JPA)等事务API，实现了一致的编程模型，而Spring的声明式事务功能更是提供了极其方便的事务配置方式。配合Spring Boot的自动配置，大多数Spring Boot项目只需要在方法上标记@Transactional注解，即可一键开启方法的事务性配置。

据我观察，大多数业务开发人员都有事务的概念，也知道如果整体考虑多个数据库操作要么成功要么失败时，需要通过数据库事务来实现多个操作的一致性和原子性。但实践中大多仅限于为方法标记 @Transactional，不会关注事务是否有效、出错后事务是否正确回滚，也不会考虑复杂的业务代码中涉及多个子业务逻辑时，如何正确处理事务。

事务没有被正确处理，一般来说不会过于影响正常流程，也不容易在测试阶段被发现。但是，当系统越来越复杂、压力越来越大之后，就会出现大量的数据不一致问题，随后就需要投入大量的人工介入去查看和修复数据。

所以说，一个成熟的业务系统和一个基本可用的业务系统，在事务处理细节上的差异非常大。要确保事务的配置符合业务功能的需求，往往不仅是技术问题，还涉及产品流程和架构设计的问题。本节标题中的“ $20\%$ ”在我看来还是比较保守的。

# 2.6.1 小心 Spring 的事务可能没有生效

在使用 @Transactional 注解开启声明式事务时，最容易忽略的问题是，事务很可能并没有生效。下面是一个示例。定义一个具有 ID（id）和姓名（name）属性的 UserEntity，也就是一个包含两个字段的用户表：

@Entity   
@Data   
public class UserEntiy{ @Id @GeneratedValuestrategy $\equiv$ AUTO) private Long id; private String name; public UserEntiy() { public UserEntiy(String name){ this.name $\equiv$ name; }

为方便理解使用 Spring JPA 做数据库访问实现一个 Repository，新增一个根据用户名查询所有数据的方法：

```txt
@Repository   
public interface UserRepository extends JpaRepository<userEntity, Long> { List<userEntity> findByName(String name); } 
```

定义一个类UserService，负责业务逻辑处理。定义一个入口方法createUserId1来调用另一个私有方法createUserId，私有方法上标记了@Transactional注解。当传入的用户名包含test关键字时判断为用户名不合法，抛出异常，让用户创建操作失败，并期望事务可以回滚，具体代码如下：

```java
@Service   
@Slf4j   
public class UserService { @Autowired private UserRepository userRepository; //一个入口方法供Controller调用，内部调用事务性的私有方法 public int createUserWrong1(String name) { try{ this.createUserPrivate(new UserEntity(name)); } catch (Exception ex){ log.error("create user failed because \{\}","ex.getMessage()); } return userRepository命名为(name).size();   
//标记了@Transactional的private方法 @Transactional private void createUserPrivate(UserEntity entity）{ userRepository.save(entity); if (entity.getName().contains("test")) throw new RuntimeException("invalid username!");   
//根据用户名查询用户数 public int getUserCount(String name) { return userRepository命名为(name).size(); 
```

如果不清楚 @Transactional 的实现方式，只考虑代码逻辑，这段代码看起来没什么问题。下面是 Controller 的实现，调用 UserService 的入口方法 createUserId1:

```java
@Autowired   
private UserService userService;   
@GetMapping("wrong1")   
public int wrong1(@RequestParam("name") String name) { return userService.createUserWrong1(name); 
```

调用接口后发现，即便用户名不合法，用户也能创建成功。刷新浏览器，多次发现有十几个非法用户注册。这时可以得到 @Transactional 生效的原则 1，除非特殊配置（如使用 AspectJ 静态织入实现 AOP），否则只有定义在 public 方法上的 @Transactional 才能生效。原因是，Spring 默认通过动态代理的方式实现 AOP，对目标方法进行增强，private 方法无法代理到，Spring 自然无法动态增强事务处理逻辑。

下面是一种很简单的修复方式，把标记了事务注解的createUserPrivate方法改为public方法。在UserService中定义一个入口方法createUserWrong2，来调用这个public方法再次尝试：

```java
public int createUserWrong2(String name) { try { this.createUserPublic(new UserEntity(name)); } catch (Exception ex) { log.error("create user failed because {}, ex.getMessage()); } return userRepository命名为(name).size(); } 
```

```java
//标记了@Transactional的public方法   
@Transactional   
public void createUserPublic(UserEntity entity){ userRepository.save(entity); if (entity.getName().contains("test")) throw new RuntimeException("invalid username!");   
} 
```

测试发现，调用新的createUserWrong2方法事务同样不生效。这时可以得到@Transactional生效的原则2，必须通过代理过的类从外部调用目标方法才能生效。

Spring 通过 AOP 技术对方法进行增强，要调用增强过的方法必然是调用代理后的对象。尝试修改 UserService 的代码：注入一个 self，再通过 self 实例调用标记有 @Transactional 注解的 createUserPublic 方法。如图 2-34 所示，设置断点可以看到，self 是由 Spring 通过 CGLIB（code generation library）方式增强过的类：

- CGLIB 通过继承方式实现代理类, private 方法在子类不可见, 自然也就无法进行事务增强;  
- this指针代表对象自己，Spring不可能注入this，所以通过this访问方法必然不是代理。

![](images/7f40a9e0b17fc68d99b00e71c8938308e4e0c4d453ce0f8d4b95a35cd2005f71.jpg)  
图2-34 对比this和self指向的对象

把 this 改为 self 后测试发现，在 Controller 中调用 createRight 方法可以验证事务是生效的，非法的用户注册操作可以回滚。虽然在 UserService 内部注入自己调用自己的 createPublic 方法可以正确实现事务，但更合理的实现方式是，让 Controller 直接调用之前定义的 UserService 的 createPublic 方法，因为注入自己调用自己很奇怪，也不符合分层实现的规范，实现代码如下：

```java
@GetMapping("right2")   
public int right2(@RequestParam("name") String name) { try { userService.createUserPublic(new UserEntity(name)); } catch (Exception ex) { 
```

```javascript
log(error("create user failed because \{\} ", ex.getMessage()); } returnuserService.getUserCount(name); 
```

我们再通过图2-35来回顾一下this自调用、通过self调用和在Controller中调用UserService3种实现的区别。

![](images/7d8d34d19955a6b949cbd1b483443a6f7cbb6dcda26ad2246d88a78006cb6a95.jpg)  
图2-35 图解this自调用、通过self调用和在Controller中调用UserService3种实现的区别

可以看到，通过this自调用，没有机会走到Spring的代理类；后两种改进方案调用的是Spring注入的UserService，通过代理调用才有机会对createUserService方法进行动态增强。

我还有一个小技巧，在开发时打开相关的Debug日志，以方便了解Spring事务实现的细节，并及时判断事务的执行情况。

我们的示例代码使用JPA进行数据库访问，可以这么开启Debug日志：

```txt
logging.level.org.springframework.orm.jpa=DEBUG 
```

开启日志后，再比较一下在UserService中通过this调用和在Controller中通过注入的UserServiceBean调用createUserService区别。很明显，this调用因为没有走代理，事务没有在createUserService方法上生效，只在Repository的save方法层面生效：

```txt
// 在UserService中通过this调用public的createUserInfo
```

```txt
[10:10:19.913] [http-nio-45678-exec-1] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :370 ] - Creating new transaction with name [org.springframework.data.jpaRepository.supportSimpleJpaRepository.save]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT //在Controller中通过注入的UserServiceBean调用createUserInfoPublic  
[10:10:47.750] [http-nio-45678-exec-6] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :370 ] - Creating new transaction with name [javaprogramming(commonmistakes transaction.demol.UserService.createUserInfoPublic]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT 
```

你可能还会考虑一个问题，这种在Controller里处理异常的实现方式显得有点烦琐，还不如直接把createUserId2方法加上@Transactional注解，然后在Controller中直接调用这个方法。这样既能从外部（Controller中）调用UserService中的方法，方法又是public的能够被动态代理AOP增强。但是，这种方法很容易踩第二个坑，即因为没有正确处理异常，导致事务即便生效也不一定能回滚。

# 2.6.2 事务即便生效也不一定能回滚

通过AOP实现事务处理可以理解为，使用try...catch...来包裹标记了@Transactional注解的方法，当方法出现了异常并且满足一定条件的时候，可以在catch里面设置事务回滚，没有异常则直接提交事务。这里的“一定条件”，主要包括如下两点。

（1）只有异常传播出了标记了@Transactional注解的方法，事务才能回滚。在Spring的TransactionAspectSupport里有个invokeWithinTransaction方法，里面就是处理事务的逻辑：

```txt
try { // This is an around advice: Invoke the next interceptor in the chain. // This will normally result in a target object being invoked. retVal = invocation已完成WithInvocation(); } catch (Throwable ex) { // target invocation exception completeTransactionAfterThrowing(txInfo, ex); throw ex; } finally { cleanupTransactionInfo(txInfo); } 
```

可以看到，只有捕获到异常才能进行后续事务处理。

（2）默认情况下，出现RuntimeException（非受检异常）或Error的时候，Spring才会回滚事务。打开Spring的DefaultTransactionAttribute类能看到如下代码块：

$\star$ The default behavior is as with EJB: rollback on unchecked exception $\star$ (@linkRuntimeException}), assuming an unexpected outcome outside of any $\star$ business rules. Additionally, we also attempt to rollback on (@link Error) which $\star$ is clearly an unexpected outcome as well. By contrast, a checked exception is $\star$ considered a business exception and therefore a regular expected outcome of the $\star$ transactional business method, i.e. a kind of alternative return value which $\star$ still allows for regular completion of resource operations. $\star$ <p>This is largely consistent with TransactionTemplate's default behavior, $\star$ except that TransactionTemplate also rolls back on undeclared checked exceptions $\star$ (a corner case). For declarative transactions, we expect checked exceptions to be $\star$ intentionally declared as business exceptions, leading to a commit by default. $\star$ @see org.springframework.transaction.supportTransactionTemplate#execute $\star/$ @Override public boolean rollbackOn(Throwable ex) { return (ex instanceof RuntimeException || ex instanceof Error); }

通过注释也能看到Spring这么做的原因：受检异常一般是业务异常，或者说是类似另一种方法的返回值，出现这样的异常时业务还可能完成，所以不会主动回滚；而Error或RuntimeException代表了非预期的结果，应该回滚。我再分享两个反例，重新实现一下UserService中的注册用户操作。

- 在createUserId方法中抛出一个RuntimeException，但由于方法内catch了所有异常，异常无法从方法中传播出去，事务自然无法回滚。  
- 在createUserWrong2方法中，注册用户的同时会有一次otherTask文件读取操作，如果文件读取失败，我们希望用户注册的数据库操作回滚。虽然这里没有捕获异常，但因为

otherTask 方法抛出的是受检异常，createUserId2 传播出去的也是受检异常，事务同样不会回滚。

实现代码如下：

```java
@Service   
@Slf4j   
public class UserService { @Autowired private UserRepository userRepository; //异常无法传播出方法，导致事务无法回滚 @Transactional public void createUserWrong1(String name) { try { userRepository.save(new UserEntity(name)); throw new RuntimeException("error"); } catch (Exception ex) { log.error("create user failed",ex); 1   
//即使抛出了受检异常也无法让事务回滚 @Transactional public void createUserWrong2(String name) throws IOException { userRepository.save(new UserEntity(name)); otherTask();   
}   
//因为文件不存在，一定会抛出一个IOException private void otherTask() throws IOException { Files.readAllLines(Paths.get("file-that-not-exist"));   
} 
```

Controller中的实现，仅仅是调用UserService的createUserId和createUserId2方法。这两个方法的实现和调用，虽然完全避开了事务不生效的坑，但因为异常处理不当，导致程序没有如我们期望的文件操作出现异常时回滚事务。我们看一下修复方式，以及如何通过日志来验证是否修复成功。针对这两种情况，对应的修复方法如下。

第一，如果你希望自己捕获异常并处理的话，可以手动设置让当前事务处于回滚状态：

```java
@Transactional
public void createUserRight1(String name) {
    try {
        userRepository.save(new UserEntity(name));
        throw new RuntimeException("error");
    } catch (Exception ex) {
        log.error("create user failed", ex);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
} 
```

运行后可以在日志中看到“Rolling back”，确认事务回滚了。同时，我们还注意到“Transactional code has requested rollback”的提示，表明手动请求回滚：

```txt
[22:14:49.352] [http-nio-45678-exec-4] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :698] - Transactional code has requested rollback  
[22:14:49.353] [http-nio-45678-exec-4] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :834] - Initiating transaction rollback 
```

```txt
[22:14:49.353] [http-nio-45678-exec-4] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :555 ] - Rolling back JPA transaction on EntityManager [SessionImpl(190671 9643<open>)] 
```

第二，在注解中声明，期望遇到所有的异常都回滚事务（来突破默认不回滚受检异常的限制）：

@Transactional(rollbackFor $\equiv$ Exception.class)   
public void createUserRight2(String name) throws IOException { userRepository.save(new UserEntity(name)); otherTask();   
}

运行后同样可以在日志中看到回滚的提示：

```txt
[22:10:47.980] [http-nio-45678-exec-4] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :834 ] - Initiating transaction rollback  
[22:10:47.981] [http-nio-45678-exec-4] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :555 ] - Rolling back JPA transaction on EntityManager [SessionImpl(141932 9213<open>)] 
```

这个例子是一个复杂的业务逻辑，其中有数据库操作、I/O 操作，在 I/O 操作出现问题时希望让数据库事务也回滚，以确保逻辑的一致性。在有些业务逻辑中，可能会包含多次数据库操作，我们不一定希望将两次操作作为一个事务来处理，这时候就需要仔细考虑事务传播的配置，否则也可能踩坑。

# 2.6.3 请确认事务传播配置是否符合自己的业务逻辑

有这么一个场景：一个用户注册的操作，会插入一个主用户到用户表，还会注册一个关联的子用户。我们希望将子用户注册的数据库操作作为一个独立事务来处理，即使失败也不会影响主流程，即不影响主用户的注册。定义一个实现类似业务逻辑的类UserService：

```java
@Autowired   
private UserRepository userRepository;   
@Autowired   
private SubUserService subUserService;   
@Transactional   
public void createUserWrong(UserEntity entity) { createMainUser(entity); subUserService.createSubUserWithExceptionWrong(entity);   
}   
private void createMainUser(UserEntity entity) { userRepository.save-entity); log.info("createMainUser finish"); 
```

SubUserService的createSubUserWithExceptionWrong方法的实现正如其名，因为最后抛出了一个运行时异常（错误原因是用户状态无效），所以子用户的注册肯定是失败的。我们期望子用户的注册作为一个事务单独回滚，不影响主用户的注册，这样的逻辑可以实现吗？

```txt
@Service   
@Slf4j   
public class SubUserService { @Autowired 
```

```java
private UserRepository userRepository;   
@Transactional   
public void createSubUserWithExceptionWrong(UserEntity entity) { log.info("createSubUserWithExceptionWrong start"); userRepository.save(entity); throw new RuntimeException("invalid status"); } 
```

在 Controller 里实现一段测试代码，调用 UserService：

```java
@GetMapping("wrong")   
public int wrong(@RequestParam("name") String name) { try {人民服务.createUserWrong(new UserEntity(name)); } catch (Exception ex) { log.error("createUserWrong failed,reason:{}",ex.getMessage()); } return userService.getUserCount(name); 
```

调用后可以在日志中发现，很明显事务回滚了，最后Controller打出了创建子用户抛出的运行时异常：

```txt
[22:50:42.866] [http-nio-45678-exec-8] [DEBUG][o.s.orm.jpa.JpaTransactionManager :555 ] - Rolling back JPA transaction onEntityManager [SessionImpl(103972212<open>)] [22:50:42.869] [http-nio-45678-exec-8] [DEBUG][o.s.orm.jpa.JpaTransactionManager :620 ] - Closing JPA EntityManager [SessionImpl(103972212<open>)] after transaction [22:50:42.869] [http-nio-45678-exec-8] [ERROR][t.dTransactionPropagationController: 23 ] - createUserWrong failed, reason:invalid status 
```

读者可能马上就会意识到不对，因为运行时异常逃出了 @Transactional 注解标记的 create用户体验方法，Spring 当然会回滚事务了。如果我们希望主方法不回滚，应该把子方法抛出的异常捕获了。

也就是这么改，把subUserService.createSubUserWithExceptionWrong包裹上catch，这样外层主方法就不会出现异常了：

```txt
@Transactional
public void createUserWrong2(UserEntity entity) {
createMainUser(entity);
try{
subUserService.createSubUserWithExceptionWrong(entity);
} catch (Exception ex) {
// 虽然捕获了异常，但是因为没有开启新事务而当前事务已被标记为 rollback，所以最终还是会回滚
log.error("create sub user error:{}", ex.getMessage());
}
} 
```

运行程序后可以看到如下日志：

```txt
[22:57:21.722] [http-nio-45678-exec-3] [DEBUG] [o.s.orm.jpa.JpaTransactionManager: 370 ] - Creating new transaction with name [javaprogramming(commonmistakes).(transaction demo3.UserService.createUserWrong2]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT [22:57:21.739] [http-nio-45678-exec-3] [INFO] [t.c交易例.demo3.SubuserService:19 ] - createSubuserWithExceptionWrong start [22:57:21.739] [http-nio-45678-exec-3] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :356 ] - Found thread-bound EntityManager [SessionImpl(1794007607<open>)] for JPA transaction 
```

```txt
[22:57:21.739] [http-nio-45678-exec-3] [DEBUG] [o.s.orm.jpa. JpaTransactionManager: 471 ] - Participating in existing transaction  
[22:57:21.740] [http-nio-45678-exec-3] [DEBUG] [o.s.orm.jpa. JpaTransactionManager:843 ] - Participating transaction failed - marking existing transaction as rollback-only  
[22:57:21.741] [http-nio-45678-exec-3] [DEBUG] [o.s.orm.jpa. JpaTransactionManager :580 ] - Setting JPA transaction on EntityManager [SessionImpl(1794007607<open>)] rollback-only  
[22:57:21.742] [http-nio-45678-exec-3] [ERROR] [.g.t.c transactiondemo3.UserService :37 ] - create sub user error:invalid status  
[22:57:21.742] [http-nio-45678-exec-3] [DEBUG] [o.s.orm.jpa. JpaTransactionManager: 741 ] - Initiating transaction commit  
[22:57:21.742] [http-nio-45678-exec-3] [DEBUG] [o.s.orm.jpa. JpaTransactionManager :529 ] - Committing JPA transaction on EntityManager [SessionImpl(1794007607<open>)]  
[22:57:21.743] [http-nio-45678-exec-3] [DEBUG] [o.s.orm.jpa. JpaTransactionManager :620 ] - Closing JPA EntityManager [SessionImpl(1794007607<open>)] after transaction  
[22:57:21.743] [http-nio-45678-exec-3] [ERROR] [t.d. TransactionPropagationController :33 ] - createUserWrong2 failed, reason:Transaction silently rolled back because it has been marked as rollback-only org.springframework_transaction. UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only 
```

需要注意以下几点。

- 观察22:57:21.722时的输出可以看到，对createUserId wrong2方法开启了异常处理。  
- 观察22:57:21.741时的输出可以看到，子方法因为出现了运行时异常，标记当前事务为回滚。  
- 随后的那行 ERROR 日志提示主方法的确捕获了异常，打印出了“create sub user error”字样。  
- 紧接着，主方法提交了事务。  
- 奇怪的是，观察22:57:21.743时的ERROR日志输出可以看到，Controller里出现了一个UnexpectedRollbackException，异常描述提示最终这个事务回滚了，而且是静默回滚的。之所以说是静默，是因为createUserId wrong2方法本身并没有出异常，只不过提交后发现子方法已经把当前事务设置为了回滚，无法完成提交。

这挺反直觉的。2.6.2节提到出了异常事务不一定回滚，这里说的却是不出异常事务也不一定可以提交。原因是，主方法注册主用户的逻辑和子方法注册子用户的逻辑是同一个事务，子逻辑标记了事务需要回滚，主逻辑自然也不能提交了。

看到这里，修复方式就很明确了，想办法让子逻辑在独立事务中运行，也就是改一下 SubUserService 注册子用户的方法，为注解加上 “propagation = Propagation.REQUIRES_NEW” 来设置 REQUIREMENTS_NEW 方式的事务传播策略，也就是执行到这个方法时需要开启新的事务并挂起当前事务：

@Transactional(propagation $=$ Propagation.REQUIRES_NEW)   
public void createSubUserWithExceptionRight(UserEntity entity) { log.info("createSubUserWithExceptionRight start"); userRepository.saveentity); throw new RuntimeException("invalidstatus");

主方法没什么变化，同样需要捕获异常，防止异常漏出去导致主事务回滚，重新命名为createUserRight。

```java
@Transactional
public void createUserRole(UserEntity entity) {
createMainUser(entity); 
```

```javascript
try{ subUserService.createSubUserWithExceptionRight(entity); } catch (Exception ex) { //捕获异常，防止主方法回滚 log.error("create sub user error:{}", ex.getMessage()); } 
```

改造后，重新运行程序可以看到如下的关键日志：

- 23:17:20.935时的输出提示我们针对createUserRight方法开启了主方法的事务；  
- 23:17:21.079时的输出提示创建主用户完成；  
- 23:17:21.082时的输出显示主事务挂起了，并针对createSubUserWithExceptionRight方法（创建子用户的逻辑）开启了一个新的事务；  
- 23:17:21.153时的输出提示子方法事务回滚；  
- 23:17:21.160时的输出提示子方法事务完成，继续主方法之前挂起的事务；  
- 23:17:21.161时的ERROR日志输出提示主方法捕获到了子方法的异常；  
- 最后的两条日志提示主方法的事务提交了，随后我们在 Controller 里没能看到静默回滚的异常。

```txt
[23:17:20.935] [http-nio-45678-exec-1] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :370 ] - Creating new transaction with name [javaprogramming(commonmistakes.transaction. demo3.UserService.createUserRight]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT [23:17:21.079] [http-nio-45678-exec-1] [INFO] [.g.t.c transactiondemo3.UserService :55 ] - createMainUser finish [23:17:21.082] [http-nio-45678-exec-1] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :420 ] - Suspending current transaction, creating new transaction with name [javaprogramming(commonmistakes transactiondemo3. SubUserService.createSubUserWithExceptionRight] [23:17:21.153] [http-nio-45678-exec-1] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :834 ] - Initiating transaction rollback [23:17:21.160] [http-nio-45678-exec-1] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :1009 ] - Resuming suspended transaction after completion of inner transaction [23:17:21.161] [http-nio-45678-exec-1] [ERROR] [.g.t.c transactiondemo3.UserService: 49 ] - create sub user error invalid status [23:17:21.161] [http-nio-45678-exec-1] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :741 ] - Initiating transaction commit [23:17:21.161] [http-nio-45678-exec-1] [DEBUG] [o.s.orm.jpa.JpaTransactionManager :529 ] - Committing JPA transaction on EntityManager [SessionImpl(396441411<open>)] 
```

运行测试程序结果如图2-36所示，getUserCount得到的用户数