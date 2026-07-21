# eBPF开发指南从原理到应用

深入Linux核心，拓展性能视野，探索eBPF的无限潜能引领系统监控革新，揭秘eBPF技术的深度应用与前沿实践

丰生强 李泊冰 著

# eBPF开发指南从原理到应用

丰生强 李泊冰 著

# 图书在版编目（CIP）数据

eBPF开发指南：从原理到应用/丰生强，李泊冰著. --北京：人民邮电出版社，2024.12

ISBN 978-7-115-64360-5

I. $①\mathrm{e}\dots$ II. $①$ 丰… $②$ 李…III. $①$ Linux操作系统一程序设计IV. $①$ TP316.89

中国国家版本馆CIP数据核字(2024)第103518号

# 内容提要

本书详细介绍了eBPF核心技术及其应用。全书涵盖了eBPF基础知识、进阶应用和实际案例，包括eBPF的编程接口、架构及其在性能分析、安全监控和网络协议等方面的应用。读者将通过C、Go和Python等语言学习eBPF编程，并掌握其在系统监控、数据分析和性能提升方面的实用技巧。

本书适合不同层次的读者阅读，包括对操作系统或应用程序监控感兴趣的学生和初学者，希望利用eBPF进行内核代码调试和优化的Linux内核开发人员，使用eBPF监控系统事件和分析恶意软件的安全工程师和逆向工程师，通过eBPF收集性能数据以优化软件和系统性能的性能分析师和应用程序开发者，以及希望优化虚拟化软件性能和管理的虚拟化开发人员。

<table><tr><td colspan="2">◆ 著 丰生强 李泊冰
责任编辑 余洁
责任印制 王郁 焦志炜</td></tr><tr><td colspan="2">◆ 人民邮电出版社出版发行 北京市丰台区成寿寺路11号
邮编 100164 电子邮件 315@ptpress.com.cn
网址 https://www.ptpress.com.cn
山东华立印务有限公司印刷</td></tr><tr><td colspan="2">◆ 开本:800×1000 1/16
印张:28.75 2024年12月第1版
字数:660千字 2024年12月山东第1次印刷</td></tr></table>

定价：109.80元

读者服务热线：（010）81055410 印装质量热线：（010）81055316

反盗版热线：(010)81055315

广告经营许可证：京东市监广登字20170147号

![](images/a03be0cc8cf98b845f9894936ca8c23924f88a999f361e00fded4b5f715b5480.jpg)

# 丰生强

独立软件安全研究员，资深软件安全专家，ISC2016安全训练营独立讲师，拥有丰富的软件安全实战经验。自2008年起，在知名安全杂志《黑客防线》上发表技术文章，活跃于国内各大软件安全论坛，具有深厚的行业影响力。著有《Android软件安全与逆向分析》《macOS软件安全与逆向分析》等畅销图书，深受读者喜爱。

![](images/b2f7bc5ab72788aa998382900842aeb15689552e640c8c75e907035d73aad557.jpg)

# 李泊冰

安全专家，资深程序员，专注于移动安全研究。拥有近10年的行业经验，擅长软件安全攻防对抗，多次从零开始构建企业移动安全体系，具备丰富的实战经验。曾在国内多家知名互联网公司任职，涉及电商、短视频、游戏、杀毒软件等多个领域。业余时间致力于软件与系统底层技术的研究。

面对Linux系统监控与性能分析的复杂挑战，你是否感到困惑与无助？遭遇线上服务难题时，你是否渴望一把解锁内核与应用运行奥秘的钥匙？不用担心，eBPF技术便是你的新选择。本书将是你在这场探索之旅中的指南针，引领你穿越内核的复杂迷宫，发掘eBPF的强大功能。从基础到高级，从监控网络流量到优化应用性能，本书不仅传授理论，更提供实践指南。让我们携手，借助eBPF的力量，开启系统架构的新时代。

# 前言

在这个科技迅猛发展、变化莫测的时代，计算机技术的创新成果如同潮水般汹涌而来，让人目不暇接。在计算机技术众多领域中，eBPF无疑是近年来最为闪耀的成果之一。在探索eBPF应用于安卓移动安全领域的过程中，我被eBPF的精妙设计与可观测能力深深吸引，因此决定撰写一本关于eBPF技术的图书，旨在探讨这一技术的功能细节与应用场景，与志同道合的读者共同领略eBPF的魅力。

在国内出版一本高质量的原创技术图书是一个充满挑战的过程。从构思到成书，每一步都伴随着艰辛与不易。原创内容的广度与深度是备受关注的，内容不够“干”很容易受到读者的质疑，消极的评价与微薄的稿酬也常常会打击技术分享者的创作热情。为了不负读者的期待，面对日新月异的技术更新，保持内容的前沿与实用性是我在写作过程中始终坚持的原则。在学习与写作过程中，我阅读与借鉴了大量开源社区的优秀eBPF项目，从这片“代码的海洋”中深切感受到eBPF犹如一个正在迅速成长的孩童，在技术社区众多热情开发者的精心呵护下茁壮成长。笔者也将不遗余力，通过本书与大家分享我的实践经验与感想。

技术探索的道路总是充满了挑战。eBPF 作为一项深奥且强大的技术，其学习曲线颇为陡峭。在撰写本书的过程中，我深切体会到了探索未知技术的艰辛。为了让 eBPF 的相应工具软件能在安卓系统上正常运行，我经历了无数次的试验与纠错，反复实践与验证，其中每一步都是对知识深度与广度的挑战。然而，正是这些挑战铸就了技术的深度与精粹，也让我在这个过程中获得了巨大的成就感与满足感。

对技术的热爱是驱使我写下这本书的最大动力。在我看来，技术不仅仅是冰冷的代码和枯燥的理论，它更是一种创造力的体现，是连接人与世界的桥梁。通过这本书，我希望能够与读者分享我对eBPF技术的理解与感悟，也期待通过我的文字激发更多人对技术探索的兴趣与热情。

写作此书的过程是一次漫长而又充实的旅程。我深知，无论是对eBPF的深入剖析，还是对其应用场景的详细探讨，都不可能脱离广大技术社区的支持与帮助。在此，我要感谢每一位在这个过程中给予我支持和帮助的朋友、同行及社区成员，你们的鼓励与建议使得这本书更加完善。

最后，我希望这本书能成为读者了解和掌握eBPF技术的宝贵资料。无论你是初学者，还是希望深入了解eBPF的专业人士，我相信，在这本书的帮助下，你都能找到所需的知识和灵感。让我们一起在技术的海洋中遨游，探索更多的可能性。

如需进一步探讨eBPF相关内容，请关注我的微信公众号“软件安全与逆向分析”：feicong_sec。让我们一起学习，共同进步！

再次感谢你选择阅读这本书，愿它能够成为你在技术探索道路上的忠实伙伴，与你共同见证eBPF技术的辉煌未来。

丰生强

# 致 谢

在本书的创作旅程中，有许多人在背后默默提供了帮助，在这里，本书作者要向所有给予我们帮助的朋友致以最诚挚的谢意（以下排名不分先后）。

感谢eBPF的创始人AlexeiStarrovoitov，他创造了如此优秀的工具。

感谢国内外开源社区在eBPF技术领域慷慨分享的学习资料与工具案例，这些资源不仅促进了eBPF开源社区的繁荣发展，也为本书作者在技术探索上节省了宝贵时间。

感谢人民邮电出版社对本书内容的认可，特别要感谢本书编辑余洁女士对内容的严格把关，确保了本书的顺利出版。

感谢陈莉君教授、王彬、钱林松、段钢、栗永辉、梁家辉、李一默为本书编写推荐语，并提供了许多宝贵建议，他们都是作者在职业生涯中的良师益友。

感谢数篷科技信创负责人黄瀚（黄药师）一如既往地支持我的内容创作。

感谢陈恒奇（@chenhengqi）在eBPF移动安全领域给予的技术指导。

感谢作者的挚友、资深网络安全专家、Linux内核驱动专家邹冠群，为本书技术细节的实现提供了大量帮助。

感谢安全研究员和软件安全专家丁健海为本书的技术细节提供了参考资料。

感谢高级安全研究员孙璟凯为本书的技术细节提供了参考资料并审阅了部分章节。

感谢作者的挚友、资深逆向专家、看雪资深安全讲师刘杨（imy4ng）为本书的编写提供了很多宝贵的建议。

感谢资深安全专家、作者的挚友、知名Hook框架Dobby的作者初峻铭（jmpews）为本书提供了许多宝贵的建议，他在iOS上也实现了类似eBPF的框架。

感谢安全专家、作者多年挚友和同事胡雾，他逻辑思维缜密，总能一针见血地找到问题本质，他为本书的编写提供了很多宝贵的建议。

感谢安全专家、作者多年挚友和同事陈星任（A4r0n），他在逆向工程和移动安全领域的见解给了作者诸多灵感，也为本书的编写提供了很多宝贵的建议。

感谢资深安全专家、作者的挚友许雷永为本书的编写提供了很多创新建议。

感谢安全专家梅鹏涛（asmjmp0），他对代码混淆和保护有丰富的经验，并为本书eBPF汇编指令集的编写提供了宝贵的建议。

感谢小红书移动安全专家唐思廉（Tesila），他从0到1完成了小红书移动安全建设，与作者在本书技术项目的业务落地上有许多交流并提供建议。

感谢连续创业者和网络安全技术专家、国内逆向技术的先行者胡斌（超人）为本书的编写提供了很多宝贵的建议。

最后，感谢所有支持本书创作的朋友，你们的支持是本书作者创作的动力源泉。

# 资源与支持

# 资源获取

本书提供如下资源：

本书源代码：  
本书思维导图：  
- 异步社区7天VIP会员。

要获得以上资源，您可以扫描下方二维码，根据指引领取。

![](images/d4fafcb07df1852c7c29d7f84ec3f374735aa8269abf18d5e4fda76ef2011d06.jpg)

# 提交勘误信息

作者和编辑尽最大努力来确保书中内容的准确性，但难免会存在疏漏。欢迎您将发现的问题反馈给我们，帮助我们提升图书的质量。

当您发现错误时，请登录异步社区（https://www.epubit.com/），按书名搜索，进入本书页面，单击“发表勘误”，输入错误信息，单击“提交勘误”按钮即可（见下图）。本书的作者和编辑会对您提交的勘误信息进行审核，确认并接受后，您将获赠异步社区的100积分。积分可用于在异步社区兑换优惠券、样书或奖品。

![](images/731a88592292e505105ddae11367047c0f61066bb4f7f61d83c68cce08564274.jpg)

# 与我们联系

我们的联系邮箱是contact@epubit.com.cn。

如果您对本书有任何疑问或建议，请您发邮件给我们，并请在邮件标题中注明本书书名，以便我们更高效地做出反馈。

如果您有兴趣出版图书、录制教学视频，或者参与图书翻译、技术审校等工作，可以发邮件给本书的责任编辑（shejie@ptpress.com.cn）。

如果您所在的学校、培训机构或企业，想批量购买本书或异步社区出版的其他图书，也可以发邮件给我们。

如果您在网上发现有针对异步社区出品图书的各种形式的盗版行为，包括对图书全部或部分内容的非授权传播，请您将怀疑有侵权行为的链接通过邮件发送给我们。您的这一举动是对作者权益的保护，也是我们持续为您提供有价值的内容的动力之源。

# 关于异步社区和异步图书

“异步社区”（www.epubit.com）是由人民邮电出版社创办的IT专业图书社区，于2015年8月上线运营，致力于优质内容的出版和分享，为读者提供高品质的学习内容，为作译者提供专业的出版服务，实现作者与读者在线交流互动，以及传统出版与数字出版的融合发展。

“异步图书”是异步社区策划出版的精品IT图书的品牌，依托于人民邮电出版社在计算机图书领域30余年的发展与积淀。异步图书面向IT行业以及各行业使用IT技术的用户。

# 目录

# 第1章 eBPF概述

1.1 eBPF是什么  
1.2 eBPF发展历史· 2  
1.3 eBPF应用领域 4  
1.4 eBPF如何运行 5  
1.5 eBPF相关工具与库· 6

1.5.1 BCC 6   
1.5.2 bpftrace 7   
1.5.3 libbpf 8

1.6 初识eBPF程序 8  
1.7 本章小结

# 第2章 eBPF开发环境准备 10

2.1 Linux发行版本的选择 10  
2.2 编程语言的选择 12  
2.3 安装和配置Linux操作系统环境 13

2.3.1 Windows上安装和配置Linux 14   
2.3.2 macOS上安装和配置Linux 16   
2.3.3 其他环境安装· 17

2.4 以二进制方式安装eBPF开发工具与库· 20

2.4.1 安装BCC· 20  
2.4.2 安装bpftrace· 21  
2.4.3 安装libbpf 21

2.5 以源码方式安装eBPF开发工具与库

2.5.1 编译安装BCC 22  
2.5.2 编译安装 bpftrace… 23  
2.5.3 编译安装libbpf 24

2.6 本章小结· 24

# 第3章 Linux动态追踪技术 25

3.1 Linux动态追踪系统 25  
3.2 前端工具和库 26

3.2.1 strace与ltrace… 26   
3.2.2 DTrace 29   
3.2.3 SystemTap 30   
3.2.4 LTTng 30   
3.2.5 trace-cmd 31  
3.2.6 perf 31

3.3 数据采集机制 35

3.3.1 ptrace系统调用 36  
3.3.2 perf_event_open系统调用· 36   
3.3.3 BPF系统调用· 37  
3.3.4 其他子系统与内核模块· 37

3.4 跟踪文件系统 37

3.4.1 挂载位置 38  
3.4.2 目录详情· 38   
3.4.3 跟踪器 43  
3.4.4 跟踪选项 44   
3.4.5 环形缓冲区 47

3.5 Linux内核数据源 48

3.5.1 ftrace 49   
3.5.2 kprobe/kretprobe 70

3.5.3uprobe/uretprobe 74   
3.5.4 tracepoint 77

3.6 eBPF数据采集点 83  
3.7 本章小结· 84

# 第4章 eBPF程序入门 85

4.1 第一个eBPF程序 85

4.1.1 第一个BCC程序………85  
4.1.2 第一个C语言版本的eBPF程序· 86

4.2 eBPF程序功能解读… 91

4.2.1 加载eBPF字节码… 92  
4.2.2 BPF系统调用· 93  
4.2.3 attach kprobe 96   
4.2.4 perf_event_open系统调用· 96

4.3 eBPF授权协议 102  
4.4 eBPF指令集 103

4.4.1 eBPF寄存器 103  
4.4.2 eBPF指令编码… 104   
4.4.3 指令列表 105  
4.4.4 eBPF指令分析 109  
4.4.5 BCC中eBPF程序指令的生成· 110  
4.4.6 eBPF指令反汇编 112   
4.4.7 eBPF验证机制 117

4.5 libbpf 126

4.5.1 libbpf功能· 126  
4.5.2 libbpf 接口 … 127

4.6 libbpf 案例程序 ..... 128  
4.7 重写eBPF程序· 131

4.7.1 如何编译 132  
4.7.2 编译内核态程序 135  
4.7.3 编译生成skel头文件136  
4.7.4 编译用户态程序 141

4.8 本章小结 ………………………………………… 143

# 第5章 BCC 144

5.1 BCC工具集 145

5.1.1 tools工具集 146  
5.1.2 libbpf-tools 工具集 146

5.2 BCC常用的工具 147

5.2.1 opensnoop 147   
5.2.2 exitsnoop 149   
5.2.3 execsnoop 150

5.3 使用Python开发eBPF程序……152

5.3.1 BPF API 152   
5.3.2 opensnoop程序解读……157

5.4 使用libbcc开发eBPF程序……165

5.4.1 libbcc 的编译与安装 …… 166  
5.4.2 重写eBPF程序 ………………………………………… 167  
5.4.3 编译与测试 175

5.5 本章小结· 181

# 第6章 bpftrace 182

6.1 bptrace的功能和特性 182

6.1.1 工程结构 182  
6.1.2 探针类型 184  
6.1.3 特性 185  
6.1.4 主程序 185

6.2 bpftrace的脚本语法 191

6.3 探针类型 198

6.3.1 kprobe和kretprobe………198   
6.3.2uprobe和uretprobe… 200   
6.3.3 跟踪点 202  
6.3.4 USDT 204   
6.3.5 定时器事件 208  
6.3.6 软件与硬件事件 209  
6.3.7 内存监视点 211  
6.3.8 kfunc和kretfunc… 214   
6.3.9 迭代器 215  
6.3.10 开始块与结束块 217

6.4 bpftrace变量 217

6.4.1 内置变量 217  
6.4.2 基础变量 218  
6.4.3 关联数组 221

6.5 bpftrace函数 221

6.5.1 基础函数 221  
6.5.2 映射表相关函数 225

6.6 bpftrace的工作原理 226

6.7 bpftrace工具集 231  
6.8 本章小结 …………………………………………236

# 第7章 使用Golang开发eBPF程序…238

7.1 Go语言开发环境介绍 238  
7.2 使用libbpfgo开发eBPF程序· 239

7.2.1 搭建libbpfgo开发环境· 239  
7.2.2 开发eBPF程序 241

7.3 Cilium与ebpf-go… 244

7.3.1 搭建ebpf-go开发环境· 244  
7.3.2 使用ebpf-go开发eBPF程序· 245  
7.3.3 bpf2go和bpftool… 249

7.4 本章小结 ………………………………………… 255

# 第8章 BTF与CO-RE 256

8.1 什么是CO-RE· 257  
8.2 BTF详解 258

8.2.1 BTF数据结构 258  
8.2.2 BTF内核API… 261   
8.2.3 生成BTF信息… 262  
8.2.4 二进制中的BTF… 264  
8.2.5 BTF相关辅助函数 265

8.3 对BTF的处理 266  
8.3.1 编译器对BTF的处理…266

8.3.2 libbpf对BTF的处理…268

8.4 读取内核结构体字段· 269

8.4.1 案例一：直接访问结构体· 269  
8.4.2 案例二：使用bpf_getcurrent_task_btf· 270  
8.4.3 案例三：使用BPF_CORE_READ 271  
8.4.4 BTF相关的其他宏…273

8.5 低版本系统如何支持BTF……274

8.5.1 什么是BTFHub… 275  
8.5.2 生成最小化的BTF信息· 279  
8.5.3 编译运行BTF-App 280

8.6 本章小结· 285

# 第9章 eBPF程序的数据交换 286

9.1 eBPF程序的数据结构 286

9.1.1 什么是eBPF map……286   
9.1.2 map支持的数据类型…291

9.2 map操作接口· 294

9.2.1 eBPF map 相关的 API…294  
9.2.2 创建 map 299  
9.2.3 添加数据 300  
9.2.4 查询· 301  
9.2.5 遍历数据 301  
9.2.6 删除数据 302  
9.2.7 使用bpftool操作map…302

9.3 map 在内核中的实现 …………………… 306

9.3.1 创建map对象… 307  
9.3.2 map对象的生命周期…314  
9.3.3 eBPF对象持久化 315

9.4 ftrace的eBPF数据交换接口…317

9.4.1 bpf_trace_PRINTk 317   
9.4.2 封装的bpf_PRINTk宏……320   
9.4.3 trace日志的输出格式…321

9.5 perf事件· 322

9.5.1 perf事件的map类型…323   
9.5.2 内核态程序写入perf事件· 324  
9.5.3 用户态程序读取perf事件· 327  
9.5.4 BCC中perf事件处理…330

9.6 环形缓冲区 333

9.6.1 eBPF ringbuf 的 map 类型 334  
9.6.2 内核态程序如何使用 ringbuf 335  
9.6.3 用户态程序如何使用 ringbuf 344  
9.6.4 完整的数据交换实例…346

9.7 本章小结 …………………… 351

# 第10章 eBPF程序类型与挂载点 353

10.1 常见的eBPF程序类型 353  
10.1.1 跟踪和分析类 355  
10.1.2 网络类 356  
10.2 eBPF程序挂载点 357  
10.3 函数跟踪技术 358

10.3.1 内核态程序跟踪 358  
10.3.2 用户态程序跟踪 360

10.4 kprobe 361

10.4.1 内核中使用kprobe探针 361  
10.4.2 kretprobe 365   
10.4.3 eBPF中创建kprobe跟踪· 368

10.5uprobe 372

10.5.1 创建单行程序测试uprobe 372  
10.5.2 eBPF中创建uprobe跟踪· 373

10.5.3 bashreadline程序 377   
10.6 USDT 379   
10.6.1 在BCC中使用USDT· 379  
10.6.2 在libbpf中使用USDT· 384

10.7 本章小结· 387

# 第11章 eBPF内核辅助方法 388

11.1 如何查阅内核辅助方法……388  
11.2 辅助方法的实现原理 389  
11.3 eBPF内核辅助方法分类… 392

11.3.1 网络相关的辅助方法· 392  
11.3.2 数据处理类辅助方法· 396  
11.3.3 跟踪相关的辅助方法· 398  
11.3.4 系统功能性辅助方法· 399

11.4 常用的eBPF内核辅助方法…401  
11.5 本章小结· 404

# 第12章 Linux性能分析 405

12.1 CPU· 406

12.1.1 CPU基础知识… 406   
12.1.2 传统CPU分析工具…409  
12.1.3 eBPF相关分析工具…412  
12.1.4 CPU分析策略… 413

12.2 内存· 414

12.2.1 内存基础知识 414  
12.2.2 传统内存分析工具……419  
12.2.3 eBPF内存分析工具…419  
12.2.4 内存分析方法 420

12.3 磁盘I/O 420

12.3.1 磁盘I/O基础知识…420

12.3.2 传统分析工具 423  
12.3.3 BCC中的分析工具…423  
12.3.4 磁盘性能分析方法…423

12.4 网络 424

12.4.1 网络基础知识 424  
12.4.2 传统网络分析工具…426  
12.4.3 eBPF网络分析工具 426

12.5 常用分析方法和案例 427  
12.6 本章小结 428

# 第13章 eBPF实战应用 429

13.1 在网络安全中的应用 429  
13.2 在软件动态分析中的应用……432  
13.3 在安全环境增强中的应用……438  
13.4 在网络数据处理中的应用……441  
13.5 在系统与云原生安全中的应用 445  
13.6 本章小结· 446

# 第1章 eBPF概述

当笔者向从事计算机技术开发的朋友介绍eBPF在网络安全领域的应用场景时，通常需要先解释：eBPF是什么？它与传统的安全对抗技术有什么差异？学习这门技术能带来更多额外收益吗？哪些应用领域已经在研究并使用它了？它的运行和部署需要怎样的硬件与软件环境？

所有这些问题都是本章将向大家介绍的。

# 1.1 eBPF是什么

eBPF是extendedBerkeleyPacketFilter（扩展的伯利克数据包过滤器）的缩写，从字面意思理解，eBPF就是一个过滤器（Filter）。谈到过滤器，我们知道其重要的作用就是过滤，将我们想要的结果筛选出来。eBPF就是通过这个过滤机制，将Linux系统内部大量的数据（Packet）进行过滤，

从而筛选出符合自己需求的数据，达到观测、跟踪等各种目的。

eBPF的官方图标是一只蜜蜂，具有较高的辨识度，十分可爱，如图1-1所示。

我们知道，操作系统内核具有监视和控制整个系统的特权，因此它在实现可观测性、安全性、网

![](images/20e2928e1c7b09a438104a0aea00bf1eb7b10bd8581414fd2beaa39e3083a97f.jpg)  
图1-1 eBPF的官方图标

络功能、跟踪（tracing）等方面扮演着重要角色。以前，在Linux内核中实现这些功能非常困难，比如实现类似的观测，需要修改Linux内核源码或者加载额外的内核模块，这会导致内核代码愈加抽象，层层叠加，内核演变得越来越复杂；鉴于操作系统内核对稳定性和安全性的极高要求，一点微小的bug都可能引发致命的稳定性或安全性问题。因此与操作系统的其他功能模块相比，内核的创新速度比较缓慢，也因此难以演进。

eBPF的出现给Linux内核带来了创新，虽然对于Linux内核而言，这并不属于一项革命性技术。扩展设计的高效BPF字节码表示，配合全新设计的BPF字节码执行虚拟机，形成了一个功能强大的BPF子系统。所有的eBPF程序运行在这个虚拟机中，就像运行在沙盒环境中一样，不会对系统内核的稳定性造成任何伤害。eBPF虚拟机在保证系统稳定与安全的前提下，高效地执行BPF字节码，引入的BPF挂载点可以让eBPF程序在系统的各个角落中得以执行，对于多数程序来说，它的执行是无侵入性的。这种天才般的设计正在积极影响着内核，也推动着新技术的蓬勃发展。

# 1.2 eBPF 发展历史

eBPF最初是为内核数据包过滤设计的，经过多年的发展和演变，它的用途已经扩展到了其他领域，比如网络监控、系统性能、安全分析、软件安全等。而eBPF的发展远远没有结束，其灵活可扩展的特性为未来的演变和进化带来无穷的想象空间。下面我们回顾一下eBPF的发展历史。

- 1992年，Steven McCanne和Van Jacobson发表了一篇论文“The BSD Packet Filter: A New Architecture for User-level Packet Capture”，在文中作者描述了他们在UNIX内核中如何实现网络数据包过滤，这种新的技术比当时最先进的数据包过滤技术快了20倍。这也是BPF最早的雏形。可能因为诞生于加州大学伯克利分校，所以叫作伯利克数据包过滤器（Berkeley Packet Filter，BPF）。在他们的论文中，提供了一张结构图来帮助理解BPF，如图1-2所示。可以看到，早期BPF在过滤器和用户空间之间使用了一个缓冲区，以此来避免过滤器匹配的每个数据包在用户空间和内核空间之间进行“昂贵的”上下文切换。

![](images/3718a005da138ca8a8b5014032729ef907312c96e59d4cb340b1ae4ce6783aab.jpg)  
图1-2 BPF结构图

- 1997年，Linux2.1.75首次引入了BPF技术，将高性能的BSD包过滤机制带入Linux。随后BPF开始了漫长的不温不火的发展历史。  
- 2011年，Linux3.0中增加BPF即时编译器（BPFJIT)，替换了原本性能比较差的解释器，进一步优化了BPF指令运行的效率。这是一次非常大的更新，而此时的应用还仅局限于网络包过滤这个比较传统的领域。  
- 2014年，为了研究新的软件定义网络方案，Alexei Starovoitov为BPF带来了第一次革命性的更新，将eBPF扩展为一个通用的虚拟机，也就是eBPF。eBPF不仅扩展了寄存器的数量，引入了全新的eBPF映射存储，还在4.x内核中将原本单一的数据包过滤事件逐步扩展到了内核态函数、用户态函数、跟踪点、性能事件（perf events）及安全控制等。  
- 2015年，BCC（BPF Compiler Collection，BPF编译器集合）提供了一系列基于eBPF的工具和库函数，大大简化了eBPF程序的开发和运行。同年推出的Linux 4.1也开始支持kprobe和cls bpf，后者用于流量控制。  
- 2016年，Linux $4.7\sim 4.10$ 增加跟踪点、性能事件、XDP及egroups的支持，丰富了eBPF的事件源。同年，Cilium项目发布，它是一个开源的网络和安全解决方案，用于容器化应用程序。它使用Linux内核内置的eBPF功能为容器提供快速且高效的网络、安全和负载均衡。  
- 2017年，eBPF成为内核独立子模块，并支持KTLS、bpftool、libbpf等。同年，Netflix、Facebook及Cloudflare等公司开始将eBPF用于跟踪、DDoS防御、4层负载均衡等方面。  
- 2018年，eBPF新增了轻量级调试信息格式BTF及新的AF_XDP类型。同年，Cilium发布1.0版本，bpfftrace和bpffiler项目也正式发布。  
- 2019年，eBPF新增加了尾调用和热更新的支持，GCC也开始支持BPF编译。同年，Cilium 1.6发布基于eBPF的服务发现代理（完全替换基于iptables的kube-proxy）。  
- 2020年，Google和Facebook为eBPF新增对LSM和TCP拥塞控制的支持，主流云厂商开始通过SROV支持XDP。同年，微软基于eBPF开始为Windows监控工具、Sysmon增加Linux支持。  
- 2021年，微软发布Windows eBPF，并与Facebook、Google、Isovalent和Netflix等一起成立eBPF基金会。同年，eBPF开始支持内核函数调用，Cilium发布基于eBPF的Service Mesh（取代代理）。  
- 2022年，西安邮电大学发起首届中国eBPF大会。国内各大计算机厂商及相关领域的专家学者在大会上展示了eBPF在各个领域的运用场景。eBPF生态空前活跃。  
- 2023年，eBPF在国内外网络安全研究专家的探索下，在安卓系统平台有了更多的技术应用，包括网络数据包捕获、App性能优化、代码跟踪分析等。笔者也编写了第一份eBPF在安卓移动安全攻防领域的应用教程，并研制了App安全逆向分析的产品。  
- 2024年，Linux内核发布6.8版本。eBPF的程序类型与运行时环境在内核中已经得到大量的更新与完善，现在已可以在x86_64、aarch64、MIPS、PowerPC、RISC-V、LoongArch等众多处理器架构上运行。

直到今天，eBPF依旧是内核社区最活跃的子模块之一，仍然在不断地进行更新迭代，eBPF的应用场景也越来越广泛。未来我们可以看到更多的eBPF的创新案例，涉及网络安全、软件开发、性能优化、虚拟化、云技术等诸多领域。

# 1.3 eBPF应用领域

上面我们简单了解了eBPF的发展史。我们知道，可通过eBPF在操作系统内核中运行我们的沙盒程序，而无需修改操作系统内核。而eBPF发展至今，已经有了非常广泛的应用场景，它被用于各领域以解决不同的问题。

eBPF 的强大功能已辐射到 IT 应用的各个方面。按照 eBPF 官方网站上的划分，eBPF 应用领域可分为如下四大类。

# 1. 网络

eBPF最初就是为解决网络应用问题而存在的。根据网络流量所处的不同位置，eBPF的程序类型与功能边界又会有一些区别。比如在网络数据的最初站点，eBPF负责的岗哨为XDP，XDP功能较复杂，掌握该功能需要对Linux操作系统的网络模块比较熟悉，与XDP相关的网络数据包分流、优化也是一个大的课题。在流量控制（TC）层面，应用较多的有网络数据抓包、数据转发代理等。再往下，还有Netfilter部分，其中数据包过滤是防火墙重要的功能，Linux内核6.3版本也在积极推动BPF_PROG_TYPE_NETFILTER这种全新的eBPF程序类型。我相信，eBPF在网络方面的能力也会得到更进一步的增强。

# 2. 可观测性

可观测性伴随着eBPF出现在公众的视野，从名字上能看出它的最大特点是可观测，而不在于可修改。这是基于eBPF的Hook技术与现代化Hook框架最大的区别之一，eBPF强调的是以最低的侵入手段观测数据，而不是操纵数据。它只提供了少量的接口，以有限制地修改用户态数据。它通常只看不改，在云原生领域，eBPF技术应用十分广泛，我们将在第13章详细讨论eBPF在云原生领域的应用。

# 3. 跟踪与性能优化

程序运行的一个重要指标是运行时方法执行的指令数与耗时。Java类程序的JVM接口会提供相关的profiling接口。Linux性能调优早期依赖基于ftrace实现的一组性能分析工具。eBPF能在这个领域“横插一脚”，完全出于其技术特性优势。它在不影响程序执行的前提下，不注入任何代码，只是收集方法出入时的运行时性能指标，这种“优雅”天生就是为了性能优化而存在。BCC与bpftrace中大量的工作就是针对这类场景。

跟踪技术常用于软件动态分析。可观测性是eBPF最突出的特点，这个特点在实践中带来极小的环境修改代价，比如kprobes与uprobes会在代码段相应位置修改指令为断点指令。在实际使用过程中，除了进行代码段扫描和CRC校验，几乎没有任何其他的环境状态更改，所以可用于观测上下文状态。对于C/C++这类native型（代码直接编译为机器码）语言，eBPF无疑是应用最广泛的。BCC中提供的大量跟踪工具都是针对系统内核与native型编程语言的。eBPF的下断点方式需要用户态或内核态的地址，在断点命中时，观测寄存器与栈上的数据。这与传统的调试器和DBI工具功能相似。分析Java类编译型语言的上下文参数信息则相对困难一些，尽管通过USDT定义JVM的观测点能从侧面解决部分动态分析问题，但缺乏执行时上下文的详情，让软件动态分析的能力有所减弱。在这一点上eBPF可能需要结合外部库来解决。

# 4. 安全

eBPF功能类似于现代化Hook框架，但其代码实现更稳定、更底层。在安全领域，其应用目前多涉及高维对抗技术。云原生安全是eBPF应用最多、最早的领域。eBPF开源社区由大量的云原生领域的技术精英们主导，这些领域的应用扩展并深深影响着eBPF的技术发展趋势。其中，运行时环境增强是eBPF在云原生安全领域的重要能力体现。为了应对数据修改的要求，eBPF提供了少量的修改数据的能力。eBPF并不支持直接修改寄存器的内容，但借助bpfprobe_write系列的接口方法，配合内核错误注入选项，可以实现系统调用及部分函数的结构化参数与返回值的修改。这些内容的修改在很多情况下可以左右程序的执行逻辑，完成执行环境的流程修改与控制；同时，eBPF中提供的LSM程序类型，具备了系统资源的访问控制能力，这也是很多运行时环境增强项目得以产生的基石。近年来，不少研究团队探索eBPF在Linux系统上的Rootkit隐藏技术，以及相应的安全检测技术等，这些都将网络安全对抗提到一个新的高度。

eBPF在安卓系统上的安全应用也值得关注。安卓系统的内核源于Linux内核，扩展的部分更多是与特有的设备驱动与硬件层定义相关。在安卓系统上，eBPF功能并非简单地从Linux内核平移，它与同期发布的Linux内核版本及处理器架构的支持息息相关。eBPF的很多功能并不支持在aarch64架构的处理器上运行，安卓低版本的系统并不能正常地使能eBPF特性，直到安卓12换上5.10版本的内核后，eBPF才有了一些发挥空间。最新的安卓系统采用了6.8版本的主线内核，此时已经可以体验到绝大多数的eBPF特性了。在安卓系统上借助eBPF实现App性能优化、代码分析及网络数据处理，也只有在高版本系统中才可行，目前这还算一个新兴的技术领域。

# 1.4 eBPF如何运行

eBPF是事件驱动的，它使用了钩子技术（Hook)，当内核或者应用程序运行到某个Hook点的时候才启动。eBPF预定义的Hook点包括：

系统调用  
- 函数的入/出口  
- 内核跟踪函数  
$\bullet$ 网络事件

这些Hook点与传统Hook技术的区别是，前者需要Linux内核相应的子系统做相应的代码补充和调整，添加一个eBPF执行虚拟机。将eBPF代码编译成字节码后挂载到特定的内核地址，当Linux内核执行命中时，由eBPF虚拟机来加载执行。这种运行模式源于对传统BPF的补充，在后来的发展过程中，逐步扩展成eBPF特定的程序类型与eBPF挂载点。

不同的程序类型可以在不同的eBPF挂载点上执行。比如对于kprobe内核探测类型的eBPF程序，允许在特定的内核代码地址挂载一个或多个eBPF程序来观测执行时的上下文信息；而uptrobe用户探测程序则可以挂载到用户应用程序的任何代码位置。kprobe与uptrobe是Linux内核提供的动态跟踪技术的不同探针，在传统方式下使用它们会比较复杂，而eBPF让它们的使用变得非常灵活，我们会在第4章详细讨论。

总之，Linux eBPF技术是一种非常重要和实用的技术，它的应用范围广泛，并且发展速度非常快，它的新功能和新应用也正在不断涌现。我们可以期待其在各个相关领域出现更多功能和创新。

# 1.5 eBPF相关工具与库

eBPF 作为 BPF 的扩展版本，执行的实体同样为一条条的 BPF 指令。这种直接通过 BPF 指令来构建 eBPF 程序的方式，与使用汇编语言开发 Windows 系统程序一样，也是非常烦琐的。我们一般通过 BCC、bpftrace、libbpf 等项目间接使用 eBPF。这些项目提供了高级语言接口，如 C&C++, Python、Go 来支持 eBPF 程序逻辑的开发，然后使用 LLVM 将其编译成 eBPF 字节码，具体过程是在编译 eBPF 源程序时指定 clang-target bpf 参数。这些工具与库降低了 eBPF 的使用成本，本节就简单介绍 eBPF 相关的工具和库。在后续第 5 章会重点讲解 BCC，第 6 章会重点讲解 bpftrace。

# 1.5.1 BCC

BCC是BPF编译器集合，是最早开发BPF跟踪程序的高级框架。Linux3.15首次引入了BCC，BCC所使用的大部分内容需要Linux4.1及更高的版本的支持。BCC简化了用C语言编写eBPF程序的过程，通过类似于LLVM的编译器套件，还支持使用Python、Lua、 $\mathrm{C} + \mathrm{C} + +$ 等语言进行eBPF程序开发。

BCC提供了非常多的开箱即用的工具和示例程序，图1-3列举了不同场景下BCC支持的各类工具。这些工具涵盖了对操作系统各种场景的跟踪，可以用于日常的分析工作。本书第5章讲解BCC时会简单介绍相关工具的使用案例，读者可以举一反三，根据自身需求灵活地选用相关的工具。

![](images/eeb88d431e4712034f9a89066020ee72258371826046d8a4a6703883f868be92.jpg)  
图1-3 BCC工具集

BCC 是一个开源项目，读者可以通过访问其源码地址来了解更多信息。

# 1.5.2 bpftrace

bpftrace 是一种高级跟踪语言，由 Alastair Robertson 创建，为创建 eBPF 程序提供了便捷的高级语言支持，适用于 Linux 4.x 及更高版本。它的设计受到 awk、C 语言，以及 DTrace 和 SystemTap 等跟踪器的启发。bpftrace 采用 LLVM 作为后端，将脚本编译为 eBPF 字节码，并利用 BCC 与 Linux BPF 进行交互，从而以 eBPF 方式灵活使用现有的 Linux 跟踪功能：内核动态跟踪（kprobe）、用户级动态跟踪（uprobe）和跟踪点。

bpftrace基于eBPF和BCC实现通过探针（probe）技术跟踪内核和程序的运行时信息，然后通过内建的图表方式展示出来，满足使用者不同的跟踪、性能分析等需求。

bpftrace 支持数万个探针，其中主要包含如下类型：

-uprobe/uretprobe   
- kprobe/kretprobe   
USDT   
tracepoint   
- hardware   
- software

- profile/interval

这些探针几乎涵盖了内核的方方面面，为Linux的可观测性提供了强力支持。关于这些探针类型，会在第6章和第10章详细讲解。

bpftrace 是一个开源项目，读者可以通过访问其源码地址，了解更多信息。

# 1.5.3 libbpf

我们知道，eBPF程序注入内核后可以访问所有的内核空间，这个能力非常强大。但同时这个强大的能力也带来了一些负担，如eBPF程序无法控制周围内核环境的内存布局，因此必须依赖独立开发、编译和部署的内核，从而导致可移植性问题。此外，内核类型和数据结构会不断变化，这些内核结构体中的字段可能会被重命名或删除。换句话说，不同内核发布版本中的所有内容都有可能发生变化。这些因素给eBPF的可移植性带来了挑战。

libbpf的出现正是为了解决eBPF的可移植性问题，使其可以像应用程序一样，一次编译后可放到不同的机器、不同内核版本的系统中运行。libbpf支持构建BPF CO-RE（Compile Once-Run Everywhere，一次编译、到处运行）应用程序。与BCC相比，它不需要将Clang/LLVM运行时部署到目标服务器，也不依赖于可用的内核开发头文件。关于BPF CO-RE的资料非常多，本书不过多阐述相关细节，有兴趣的读者可以阅读Andrii Nakryiko写的“BPF Portability and CO-RE”一文，了解更多的细节。

关于使用libbpf进行开发的内容会在本书第4章进行讲解，读者也可以访问libbpf的官网获取更多信息。

# 1.6 初识eBPF程序

接下来，我们通过两个简单的 bpftrace 代码，来了解一下 eBPF 的功能。由于 bpftrace 的安装和使用分别在第 2 章和第 6 章中讲解，读者此时无需理解代码的含义及程序的执行，只需要关注程序的输出结果，了解程序完成了什么样的功能，对其有一个初步的认识即可。

首先，看看 bpftrace 的 hello world 程序。代码非常简单，只有一句话：

```shell
$ sudo bpftrace -e 'BEGIN { printf("hello world\n"); }'
Attaching 1 probe...
hello world
^C 
```

在 shell 中执行上面一句话代码，运行之后会打印出“hello world”。代码中-e参数指定 bpftrace 代码；BEGIN 是一个特殊的探针，在程序开始执行的时候触发一次，当探针被命中，大括号“{}”里面的代码会被执行。上面的程序不会自己结束，需要读者按下 Ctrl+C 组合键来结束程序运行。

再看一个比较实用的例子，也是 bpftrace 官网文档中的第一个案例，主要作用是跟踪所有程序

系统调用（syscall）的次数。代码如下所示：

```txt
# 跟踪程序系统调用的次数
$ sudo bpftrace -e 'tracepoint:raw_syscalls:sys-enter { @[comm] = count(); }
Attaching 1 probe...
^C
@[tracker-miner-f]: 1
@[xdg-document-po]: 2
@[ibus-portal]: 2
@[gnome-keyring-d]: 3
@[packagekitd]: 4
@[wpa_supplicant]: 4
@[rsyslogd]: 4
@[pool-u disksd]: 6
@[pipwire]: 6
@[cron]: 7
@[journal-offline]: 10
@[udisksd]: 12
@[pool-gnome-shell]: 13
@[threaded-ml]: 14
... 
```

可以将上面 bptrace -e 指定的代码分为如下 4 个部分来理解。

- tracepoint: raw_syscalls: sys_entry: 这是一个内核跟踪点，sys_entry 表示调用 syscall 函数时就触发。也可以修改为 sys_end，即在 syscall 函数结束并返回的时候触发。  
- comm 是 bptrace 内建的指令，代表进程名称。  
- @[]代表一个映射，或者说是一个关联数组。  
- count()是一个计数器，用于统计 syscall调用的次数。

执行上面一句话代码，按 $\mathrm{Ctrl + C}$ 组合键后输出结果，显示了进程的名字及其调用syscall的次数。由于bpftrace是对全操作系统层面进行追踪，因此任何调用了syscall的应用都能检测到。

# 1.7 本章小结

本章主要分为两个部分。前半部分介绍了eBPF的基础概念、发展历史及使用场景，通过这些内容，我们对eBPF有了基本的认识。后半部分主要介绍与eBPF相关的开发工具和库，并通过提供的两段bpftrace代码，初步认识了eBPF程序。通过这些内容，我们初步认识了eBPF开发中常用的工具和库，掌握这些基础概念将方便我们学习和理解更深入的知识。

# 第2章 eBPF开发环境准备

工欲善其事，必先利其器。在学习Linux eBPF开发之前，我们需要先准备好eBPF的开发环境，这样才能事半功倍。本章将主要介绍如何搭建Linux eBPF的开发环境，并提供不同技术选型以适应不同读者的需求。

# 2.1 Linux发行版本的选择

由于Linux是开源和免费的，因此任何个人或组织都可以对其进行修改并重新发布。许多公司在此基础上进行了定制化修改，并重新包装和发布属于自己的Linux操作系统。这些经过再次修改和发布的Linux系统被称为Linux发行版。目前存在众多不同规模的Linux发行版，据估计数量达到数百甚至上千。我们可以根据个人偏好或工作环境来选择学习eBPF所使用的适合自己需求的

Linux发行版。下面推荐几个主流的Linux发行版供读者选择。

主流的Linux发行版有Debian、 Fedora、CentOS、RedHat、Ubuntu、openSUSE，如图2-1所示。

需要注意的是，我们需要确认当前使用的Linux内核版本是否支持eBPF。eBPF最早在Linux内核版本3.15中引入，并且陆续引入了许多新功能。理论上，越新的内核版本，其对eBPF特性支持得越好。

![](images/828c0bfe15cd4b839cd6be44f2e162c20a15d71f73a24b030513f431aa32ef00.jpg)  
图2-1 主流的Linux发行版

在进行开发时，如果没有使用最新的操作系统，就需要确保当前所用的Linux内核对eBPF有足够的支持。可以通过访问以下链接来获取Linux内核对eBPF支持程度的最新信息：https://github.com/iovisor/bcc/blob/master/docs/kernel-versions.md。

# 1. Ubuntu

Ubuntu 是一个基于 Debian 的 Linux 发行版，由南非人马克·沙特尔沃思（Mark Shuttleworth）开发，并由 Canonical 公司于 2004 年 10 月发布了第一个版本（Ubuntu 4.10 “Warty Warthog”）。Ubuntu 这个名称来自非洲南部祖鲁语或豪萨语中的“ubuntu”，意为“人性”或“我的存在是因为大家的存在”。

作为一个免费的操作系统，Ubuntu因其易用性成为最受欢迎的Linux发行版之一，并且对于初次接触Linux的用户来说也是首选之一。它拥有庞大而活跃的技术社区，用户可以方便地从社区获得帮助。它通常每半年更新一次，版本号采用年份加月份表示，例如Ubuntu22.04表示该版本发布于2022年4月。此外，每两年它会推出一个长期支持版本（LTS)，该版本将获得至少5年以上的支持。例如，Ubuntu20.04和22.04都属于长期支持版本。

考虑到 Ubuntu 对新手友好且网上资料较多，在本书后续内容中，除非特别说明，我们将基于 Ubuntu 来介绍 eBPF。本书选择的开发环境是 Ubuntu 22.04，其 Linux 内核版本为 5.15。

读者可以从 Ubuntu 官网获取 Ubuntu 操作系统镜像及更多帮助信息。

# 2. Debian

Debian 是一个完全自由的操作系统，也是目前世界上最大的非商业性 Linux 发行版之一。它由来自世界各地的 1000 多名计算机业余爱好者和专业人员在闲暇时间制作而成。Debian 是最遵循 GNU 规范的 Linux 系统，因此常常被称为 Debian GNU/Linux。

Debian系统主要分为3个版本分支：stable（稳定版）、testing（测试版）和 unstable（不稳定版）。其中，unstable是包含最新软件包的测试版本，但可能存在较多bug，适合桌面用户使用。testing版本对unstable版本进行了一定程度的测试，所以相对更加稳定。而stable版本通常用于服务器环境，更加注重稳定性和安全性，所以软件包更新速度较慢。所有这些版本都采用了Pixar公司出品的动画片《玩具总动员》中的角色名作为开发代号。当前稳定分支是Debian11.3（bullseye），其所使用的Linux内核版本号为5.10.0。

读者可以从Debian官网获取Debian操作系统镜像及更多帮助信息。

# 3. Fedora

Fedora Linux 是由 Fedora 项目社区开发、RedHat 公司赞助的，其目标是创建一套新颖、多功能且自由（开放源代码）的操作系统。 Fedora 是商业化的 RedHat Enterprise Linux 发行版的上游源码。 Fedora 对用户而言，是一套功能完备、更新快速的免费操作系统。而对赞助者 RedHat 公司而言，它是许多新技术的测试平台，经它测试被认为可用的技术最终会加入 RedHat Enterprise Linux 中。

自2014年12月发布 Fedora 21以来， Fedora 提供了针对个人计算机、服务器和云计算量身定制的3个不同版本，并从2022年11月发布的 Fedora 37扩展到针对容器化和物联网（IoT）的5个版本。

Fedora 以专注于创新、尽早集成新技术及与上游 Linux 社区密切合作而著称。在上游进行更改而不是专门针对 Fedora Linux 进行更改的做法可确保更改适用于所有 Linux 发行版。

Fedora Linux 的生命周期相对较短：每个版本通常至多支持 13 个月，大多数版本之间大约间隔 6 个月。当然， Fedora 的用户无需重新安装即可从一个版本升级到另一个版本。 Fedora Linux 中默认的桌面环境是 GNOME，默认的用户界面是 GNOME Shell。其他桌面环境，如 KDE Plasma、

Xfce、LXQt、LXDE、MATE、Cinnamon等也都可用。

读者可以从 Fedora 的官网获取 Fedora 操作系统镜像及更多帮助信息。

# 4. openSUSE

openSUSE是由Novell公司发起的开源项目，旨在推进Linux的广泛使用。openSUSE为Linux开发者和爱好者提供了开始使用Linux所需要的一切。该项目由SUSE等公司赞助，2011年Attachmate集团收购了Novell，并把Novell和SUSE作为两个独立的子公司运营。openSUSE操作系统和相关的开源程序会被SUSE Linux Enterprise（比如SLES和SLED）使用。

openSUSE对个人来说是完全免费的，包括使用和在线更新。自2015年起，openSUSE开始提供两个分支：Leap和Tumbleweed，读者可以根据自己的喜好选择。

读者可以从openSUSE的官网获取openSUSE操作系统镜像及更多帮助信息。

# 2.2 编程语言的选择

在第1章中我们简单介绍了eBPF，提到了通过BCC可以支持使用不同的编程语言来开发eBPF程序。因为BCC使用LLVM作为编译器，而LLVM是模块化、可重用的编译器及工具链技术的集合。它使用不同的编译器前端，将不同编程语言的代码编译成LLVMIR（LLVMIntermediateRepresentation），由LLVM编译器后端统一处理LLVMIR，并编译成不同处理器架构平台的汇编代码，从而达到支持多种编程语言的目的。所以我们可以使用不同的编程语言来编写eBPF程序。

编译为字节码的eBPF程序部分，我们称之为eBPF内核态部分，这一部分主要使用C语言开发。用户态部分负责加载eBPF字节码，可以使用C、Python、Go、Lua及Rust等语言。对于Rust语言，目前有相应的框架支持纯Rust语言开发eBPF内核与用户态程序。

# 1. C语言

C语言想必大家都非常熟悉。C语言问世几十年了，是面向过程的结构化语言。C语言的第一个标准是由ANSI发布的（C89），也称为标准C，大部分程序都由标准C编写，方便跨平台。目前最新的C语言标准是C18，于2018年6月发布。BCC项目中有很多案例程序是用C语言编写的，读者可以参考学习。本书4.1.2节会详细讲解如何使用C语言开发eBPF程序。

# 2. Python语言

Python 由荷兰数学和计算机科学研究学会的吉多·范罗苏姆于 20 世纪 90 年代初设计，作为 ABC 语言的替代品。Python 提供了高效的高级数据结构，还能简单有效地面向对象编程。Python 语法和动态类型及解释型语言的本质，使它成为多数平台上写脚本和快速开发应用的编程语言，

随着版本的不断更新和语言新功能的添加，它逐渐被用于独立的、大型项目的开发。Python发展至今，拥有了大量的三方库的支持，使用起来简单、容易上手。BCC项目中有大量的程序案例使用Python开发，本书会在5.3节讲解如何使用Python语言开发eBPF程序，以满足不同读者的使用需求。

# 3. Go语言

Go（又称Golang）是由Google的3位开发人员Robert Griesemer、Rob Pike及Ken Thompson开发的一种静态强类型、编译型语言。Go语言语法与C语言比较相近，但对于变量的声明有所不同。Go支持垃圾回收功能。Go的并行模型以东尼·霍尔的CSP（通信顺序进程）为基础，采取类似模型的其他语言还有Occam和Limbo，但它也具有Pi运算的特征，比如通道传输。Go1.8版本中开放了对插件（Plugin）的支持，意味着现在能从中动态加载部分函数。本书会在第7章讲解如何使用Golang开发eBPF程序。

# 4.Rust语言

Rust 是一门系统编程语言，专注于内存安全，尤其是并发安全，是支持函数式和命令式及泛型等编程范式的多范式语言。Rust 在语法上与 $\mathrm{C}++$ 类似，但是设计者目的是在保证性能的同时提供更好的内存安全。Rust 最初是由 Mozilla 研究院的 Graydon Hoare 设计创造的，然后在 Dave Herman、Brendan Eich 及其他一些人的贡献下逐步完善。Rust 的设计者们将在研发 Servo 网站浏览器布局引擎过程中积累的经验用于优化 Rust 语言和 Rust 编译器。读者可以通过使用 rebpf 与 aya 来使用 Rust 开发 eBPF 程序。

# 2.3 安装和配置Linux操作系统环境

读者在选择好自己的Linux发行版后，可以前往对应官方网站下载相应版本的ISO镜像安装包。以下是可能遇到的3种安装使用场景。

1）直接将Linux发行版安装到物理机器上：读者可以通过访问对应发行版官方网站获取镜像文件并进行安装。本书不赘述。  
2）在Windows下使用VMware进行Linux发行版的安装：读者可以选择使用VMware虚拟机软件，在Windows系统中创建一个虚拟环境，并在该环境中安装所选的Linux发行版镜像。  
3）在macOS下使用Parallel Desktop：针对macOS用户，建议使用Parallel Desktop软件来创建一个虚拟环境，并在其中安装所选的Linux发行版镜像。

本书主要以 Ubuntu 操作系统作为开发环境举例说明。在这里使用到的是 ubuntu-22.04.2-desktop-amd64.iso，读者可根据实际需要下载适合自己的版本和架构（比如 32 位或 64 位）的 ISO 镜像文件。

# 2.3.1 Windows上安装和配置Linux

Windows 操作系统下可以选择 WSL 方式与 VMware 虚拟化方式来安装和配置 Linux，这里推荐使用 VMware 安装 Linux 镜像。下载好之后创建虚拟机，其他配置项目可根据自己的机器性能自由配置。但是要注意，硬盘需要分配得大一点，不然后面 eBPF 相关的工具源码可能无法编译通过。笔者分配了 120GB。

安装完毕，可以使用 uname -a 命令查看 Ubuntu 的内核版本。

```txt
$ uname -a
Linux android-virtual-machine 5.15.0-52-generic #58-Ubuntu SMP Thu Oct 13 08:03:55
UTC 2022 x86_64 x86_64 x86_64 GNU/Linux 
```

安装完 Ubuntu 操作系统后，还需要进行配置，才能让使用更加顺畅。需要配置 VMTools，以及屏幕自适应和共享文件夹。

VMTools 安装命令如下：

```shell
sudo apt-get update -y  
sudo apt-get autoremove open-vm-tools  
sudo apt-get install open-vm-tools open-vm-tools-osk 
```

配置VMware自适应步骤如下。

1）在“虚拟机设置”中去掉“拉伸模式”（此步骤需要先关闭VMware中的Ubuntu），如图2-2所示。

![](images/27a1a1ead525359856b35c2b10fc2be6804cf04ba1d651a75ee2c599e06d0ab0.jpg)  
图2-2 VMware配置

2）启动系统后，在VMware的“查看”菜单栏中选择“自动调整大小” $\rightarrow$ “自动适应客户机”，即可完成对屏幕自适应的配置，如图2-3所示。

![](images/0d0c61e720da5591150767abc8507a4f8a3882f75ff342841f166b3f26ecdd6e.jpg)  
图2-3 屏幕自适应的配置

3）配置共享文件夹，以便与宿主机器进行文件共享，如图2-4所示。首先启用共享文件夹，然后指定主机的文件夹路径，这个文件夹用来在Windows主机和Ubuntu虚拟机之间进行文件共享。

![](images/35176e0095adc5de1f19e55b3c435ce86ba7eda2245bbcf55d62e37309a300bd.jpg)  
图2-4 配置文件夹共享

4）进入 Ubuntu 系统，按如下流程进行配置：

查看共享文件夹名称

$ vmware-hgfsclient

VM_Share

$ sudo mkdir /mnt/hgfs/VM

挂载共享文件夹

$ sudo mount -t fuse.vmhgfs-fuse .host:/VM_Share /mnt/hgfs/VM -o allow_other

# 卸载（如果不想使用该共享文件夹，则可以按照以下方式进行卸载）

$ sudo mount -a fuse.vmhgfs-fuse .host:/VM_Share /mnt/hgfs/VM

为了使用方便，还可以配置系统启动后自动挂载

$ sudo vim /etc/fstab

最后添加文件

.host:/VM_Share /mnt/hgfs/VM fuse.vmhgfs-fuse allow_other 0 0

这时就可以在/mnt/hgfs/VM目录下看到Windows文件夹的共享目录了。如图2-5所示是程序在VMware虚拟机与Windows系统路径中的显示。

![](images/0ca44f71e3f424f45ca59d98d5e34e02679da3d94664e0c31d404208cb2cf63c.jpg)  
图2-5 配置VMware文件夹共享目录

# 2.3.2 macOS上安装和配置Linux

macOS上推荐使用ParallelsDesktop安装Linux操作系统。使用ParallelsDesktop安装Linux操作系统非常方便，第一次启动后登录账户，会显示如图2-6所示界面，默认支持很多主流Linux发行版的安装。可以直接点击“下载Ubuntu Linux”，ParallelsDesktop会下载并安装目前最新的Ubuntu LTS操作系统。其他Linux发行版的下载安装与此类似。当然读者也可以自己下载指定的Linux安装镜像文件（*.iso），然后在ParallelsDesktop中选择“安装Windows或其他操作系统（从DVD或镜像文件）”，这样就可以安装指定版本的操作系统。笔者这里直接选择下载Ubuntu Linux。

![](images/4f0af40028fb0566b9f87a1c3b1090c090ee7c7edf3ca983078cb4256cf4f1af.jpg)  
图2-6 ParallelsDesktop安装系统界面

下载完毕，启动 Ubuntu。第一次启动会要求安装 Parallels Tools，如图 2-7 所示。

![](images/01ae49719c0f1d0184f9955b79a7ae654d971f8a3983018c9f998061fd2257bb.jpg)  
图2-7 Parallels Tools 安装界面

安装完重启即可自动拖拽文件和使用共享文件夹了。

# 2.3.3 其他环境安装

系统安装好后，还需要对操作系统进行配置才能正常使用。在Linux环境下，大部分环境和软件的安装都可以通过Shell命令完成。

# 1. 系统更新

在 Ubuntu 操作系统中，可以执行如下命令进行系统和软件更新。

```txt
Upgrade all packages to newest sudo apt update -y && sudo apt upgrade -y 
```

如果感觉更新速度比较慢，可以将源切换为国内的源。以清华源为例子，可以执行如下命令切换源。

```shell
修改源地址  
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak  
sudo apt update && sudo apt install gnupg ca-certificates apt-transport-https software  
-properties-common wget -y  
# x86_64:  
sudo sed -i "s@http://.*archive.ubuntu.com@https://mirrors.tuna.tsinghua.edu.cn@g" /etc/apt/sources.list  
sudo sed -i "s@http://.*security.ubuntu.com@https://mirrors.tuna.tsinghua.edu.cn@g" /etc/apt/sources.list  
# or arm64: 
```

```batch
sudo sed -i "s@http://.*ports.ubuntu.com@https://mirrors.tuna.tsinghua.edu.cn@g" /etc/apt/sources.list 
```

#更新

```shell
sudo apt update -y && sudo apt upgrade -y 
```

# 2. 安装Python环境及设置pip源

1）安装Python版本，通过安装python-is-python3，设置Ubuntu的默认Python环境为python3。

```shell
DEBIAN_FRONTEND="noninteractive" sudo apt-get install -y apt-utils python3 python3-pip python2 python-is-python3 
```

2）设置pip源为豆瓣源。

```shell
pip install -U pip  
mkdir ~/.pip  
touch ~/.pip/pip.conf  
echo -e '\n[install]\ntrusted-host=pypi.douban.com\n[global]\nindex-uri=http://pypi.douban.com/simple' > ~/.pip/pip.conf  
cat ~/.pip/pip.conf 
```

3）安装一个Python库，测试一下源是否设置成功。

```batch
pip install pytest 
```

# 3. 安装Docker容器环境

Docker 是一个开源的应用容器引擎，开发者可以打包他们的应用及依赖包到一个可移植的容器中，然后将其发布到任何流行的 Linux 或 Windows 操作系统机器上，将来可以使用 Docker 对一些源码进行编译。

sudo apt-get install docker.io -y sudo gpasswd -a ${\mathbb{S}}$ {USER} docker newgrp - docker sudo service docker restart

# 4. 安装和配置Golang环境

1）安装Golang。执行下面的命令：

```shell
wget https://go.dev/dl/gol.20.5-linux-amd64.tar.gz
# wget https://go.dev/dl/gol.20.5-linux-arm64.tar.gz
sudo rm -rf /usr/local/go && sudo mkdir -p /usr/local/go && sudo chmod 777 -R /usr/local/go
sudo tar -C /usr/local -xzf gol.20.5-linux-amd64.tar.gz
export PATH=$PATH:/usr/local/go/bin
go version 
```

2）设置Golang镜像。执行下面的命令：

```shell
export GO11MODULE=on  
export GOPROXY=https://GOPROxy.cn  
or  
echo "export GO11MODULE=on" >> ~/.profile  
echo "export GOPROXY=https://GOPROxy.cn" >> ~/.profile  
source ~/.profile
```

# 5. 安装Linux-tools工具

1）安装linux-tools。执行下面的命令：

```shell
sudo apt install linux-tools-\$\$(uname -r) 
```

2）查看安装的工具包。执行下面的命令：

```shell
sudo apt-file list linux-tools-\$\$(uname -r) 
```

查看结果如下：

```txt
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/acpidbg   
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/bpftool   
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/cpupal   
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/libperf-jvmti.so   
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/perf   
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/turbostat   
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/usbip   
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/usbipd   
linux-tools-5.15.0-52-generic: /usr/lib/linux-tools/5.15.0-52-generic/x86_energy_perf_   
policy   
linux-tools-5.15.0-52-generic: /usr/share/doc/linux-tools-5.15.0-52-generic/changelog.   
Debian.gz   
linux-tools-5.15.0-52-generic: /usr/share/doc/linux-tools-5.15.0-52-generic/copyright 
```

linux-tools有perf、bpftool等跟踪工具，部分工具会在3.2.6节中讲解。

安装 openssh 以便通过远程 ssh 访问 Linux 机器。执行下面命令：

```batch
sudo apt-get install openssh-server -y 
```

# 6. 安装编码环境

1）VSCode（Visual Studio Code）是一款由微软开发且跨平台的免费源代码编辑器。该软件支持语法高亮、代码自动补全、代码重构、查看定义等功能，并且内置了命令行工具和Git版本控制系统。用户可以更改主题和键盘快捷方式以实现个性化设置，也可以通过内置的扩展程序商店安装扩展程序，以拓展软件功能，进而支持不同编程语言的开发。读者可以用于C/C++、Python、Go等多种编程。

读者可以在官网下载 VSCode，或者执行如下命令安装：

```shell
sudo rm -f /etc/apt/keyrings/packages.microsoft.gpg  
wget -qO- https://packages.microsoft.com keys.microsoft.asc | gpg --dearmor > packages.microsoft.gpg  
sudo install -D -o root -g root -m 644 packages.microsoft.gpg /etc/apt/keyrings/packages.microsoft.gpg  
sudo sh -c 'echo "deb [arch=amd64,arm64,armhf signed-by=/etc/apt/keyrings/packages.microsoft.gpg] https://packages.microsoft.com/repository/code stable main" > /etc/apt/sources.list.d/vscode.list'  
sudo apt update -y && sudo apt install code 
```

2）Pycharm是Python的集成开发环境，有一整套可以帮助用户在使用Python语言进行开发时提高效率的工具，比如调试、语法高亮、项目管理、代码跳转、智能提示、自动完成、单元测试、版本控制等。  
3）GoLand是由JetBrains公司开发的IDE，旨在为Go开发者提供一个符合人体工程学的新的商业IDE。GoLand整合了IntelliJ平台（一个用于Java语言开发的集成环境，也可用于其他开发语言），提供了针对Go语言的编码辅助和工具集成。

# 2.4 以二进制方式安装eBPF开发工具与库

安装并配置好环境后，接下来就是安装与配置eBPF开发相关的工具与库了。安装方式分为从官方软件仓库中以二进制方式安装，以及以源码方式安装。

注意，二进制安装与源码安装选择其一即可。但不管用哪种方式安装，都可能导致安装的新版本与本机中的旧版本发生冲突，从而导致软件运行失败。

# 2.4.1 安装BCC

可以通过Linux的包管理器，直接安装与当前Linux内核版本相匹配的BCC工具与库。考虑到未来可能发生变化，针对其他不同的操作系统，可直接参考BCC官方文档进行安装，或者下载最新版源码进行安装。

# 1. Debian操作系统

BCC及其相关工具在Debian的主仓库中可用，可以通过如下命令进行安装。

```shell
echo deb http://cloudfront.debian.net/debian sid main >> /etc/apt/sources.list sudo apt-get install -y bpfcc-tools libppfcc libppfcc-dev linux-headers-\$\$(uname -r) 
```

# 2. Ubuntu 操作系统

BCC的二进制文件在Ubuntu Universe仓库和IOVisor的PPA中可用，只是它们的名称略有不同。

- iovisor包：bcc-tools   
- Ubuntu包：bpfcc-tools

这里我们选择 Ubuntu 包进行安装，命令如下。

```makefile
sudo apt-get install bpfcc-tools linux-headers-\$\$(uname -r) libbbpfcc-dev -y 
```

# 3. Fedora 操作系统

从 Fedora 30 以后，标准仓库中提供了 BCC 的二进制版本，可以通过如下命令进行安装。 Fedora 30 之前的版本不推荐使用。

```txt
sudo dnf install bcc 
```

# 2.4.2 安装bpftrace

考虑到未来可能发生变化，针对其他不同的操作系统，建议以 bpftrace 官方文档为准。

# 1．Ubuntu操作系统

Ubuntu 19.04之后的操作系统安装命令如下。

```shell
$ sudo apt-get install -y bpftrace 
```

对于 Ubuntu 16.04 及以上版本，bpftrace 也可以作为 snap 包（https://snapcraft.io/bpftrace）使用。但是 snap 提供的文件权限有限，通过 snap 方式安装的话，需要指定--devmode 选项，以避免出现文件访问问题。命令如下。

```powershell
$sudo snap install --devmode bpftrace
$sudo snap connect bpftrace:system-trace
```

# 2. Fedora 操作系统

对于 Fedora 28 以上版本，bpftrace 已包含在官方仓库中，只需要使用 dnf 进行安装即可，命令如下。

```txt
$sudo dnf install -y bpftrace 
```

# 2.4.3 安装libbpf

我们还需要安装libbpf，用于安装BCC开发时依赖的一些头文件或者库文件。

这里以 Ubuntu 为例，可以执行如下命令进行安装。

```shell
$ sudo apt-get install -y libbbpf-dev
$ sudo apt-get install -y apt-file
$ sudo apt-file update 
```

安装完毕，执行 apt-file list 命令，查看 libbpf-dev 相关的头文件和库文件的位置。笔者计算机上执行命令后的输出如下：

```shell
$ sudo apt-file list libbpf-dev
libbpf-dev: /usr/include/bpf/bpf.h
libbpf-dev: /usr/include/bpf/bpf_core_read.h
libbpf-dev: /usr/include/bpf/bpf_endian.h
libbpf-dev: /usr/include/bpf/bpf_helper.defs.h
libbpf-dev: /usr/include/bpf/bpfHelpers.h
libbpf-dev: /usr/include/bpf/bpf_tracing.h
libbpf-dev: /usr/include/bpf/btf.h
libbpf-dev: /usr/include/bpf/libbpf.h
libbpf-dev: /usr/include/bpf/libbpf_common.h
libbpf-dev: /usr/include/bpf/libbpflegacy.h
libbpf-dev: /usr/include/bpf/skel_internal.h
libbpf-dev: /usr/include/bpf/xsk.h
libbpf-dev: /usr/lib/x86_64-linux-gnu/libbpf.a
libbpf-dev: /usr/lib/x86_64-linux-gnu/libbpf.so
libbpf-dev: /usr/lib/x86_64-linux-gnu/pkconfig/libbpf.pcg
libbpf-dev: /usr/share/doc/libbpf-dev/changeLog.Debian.gz
libbpf-dev: /usr/share/doc/libbpf-dev/copyright 
```

# 2.5 以源码方式安装eBPF开发工具与库

以源码方式安装需要先在本机安装编译器套件环境，然后下载相应工具的源码，编译后并安装，进入系统。这种方式可以保证安装的版本是最新的，从而体验到工具的新版本特性。

# 2.5.1 编译安装BCC

我们可以通过编译源码的方式来安装BCC，但是随着BCC的更新迭代，不断引入一些新的特性，编译方式可能会发生改变，请以BCC官网编译方式为准。可以参考“Install and compile BCC”(https://github.com/iovisor/bcc/blob/master/INSTALL.md)。

下面以 Ubuntu 为例，介绍 BCC 的编译方式。首先读者需要安装编译 BCC 相关的依赖库，命令如下：

```shell
Build bcc for Jammy (22.04)   
sudo apt install -y bison build-essential cmake flex git libedit-dev \   
libllvm14 llvm-14-dev libclang-14-dev python3 zliblg-dev libelf-dev libfl-dev python3distutils   
# For bcc test   
sudo apt-get install -yiperf netperf arping net-tools python-is-python3 
```

然后从GitHub代码仓库下拉BCC源码，配置好相关编译，开始编译。执行的编译命令如下：

```shell
rm -rf bcc  
git clone https://github.com/iovisor/bcc.git  
mkdir bcc/build; cd bcc/build  
cmake .. -DENABLE_LLVM_SHARED=1  
make  
sudo make install  
cmake -DPYTHON_CMD=python3 -DENABLE_LLVM_SHARED=1  
pushd src/python/  
make  
sudo make install  
popd 
```

# 2.5.2 编译安装 bpftrace

与BCC一样，可以通过编译源码的方式来安装bpftrace。但是同样随着bpftrace的更新迭代，不断引入一些新的特性，编译方式可能会发生改变。请以bpftrace官网编译方式为准，可以参考“Building bpftrace”(https://github.com/iovisor/bpftrace/blob/master/INSTALL.md)。

以 Ubuntu 环境举例，需要先安装相关依赖项。

```shell
sudo apt-get update  
sudo apt-get install -y \
bison \
cmake \
flex \
g++ \
git \
libelf-dev \
 zlibg-dev \
libfl-dev \
systemtap-sdt-dev \
binutils-dev \
libcereal-dev \
llvm-dev \
llvm-routine \
libclang-dev \
clang \
libpcap-dev \
libgtest-dev \
libgmock-dev \
asciidoctor \
pahole 
```

需要注意以下两点：

- bpftrace 会依赖指定版本的 BCC 和 libbpf，而直接使用 git clone 的话会导致目录下的 BCC 和 libbpf 为空，因此执行 git clone 的时候要加上 --recursive 参数，保证 git 引起的其他项目分支被正常拉取。  
- bpftrace 的编译会下载很多额外的环境，而下载这些依赖会导致国内的网络阻塞。最好使用网络代理，再执行下面的命令下载与编译 bpftrace 代码。

```shell
rm -rf bpftrace  
git clone https://github.com/iovisor/bpftrace --recurse-submodules  
mkdir bpftrace/build; cd bpftrace/build;  
./build-libs.sh  
cmake -DCMAKE-built_TYPE=Release ..  
make -j8  
sudo make install 
```

# 2.5.3 编译安装libbpf

与BCC一样，可以通过编译源码的方式来安装libbpf。但是随着libbpf的更新迭代，不断引入一些新的特性，编译方式可能会发生改变。请以libbpf官网编译方式为准，可以参考“Building libbpf”(https://github.com/libbpf/libbpf)。

首先需要安装编译libbpf的相关依赖。

```txt
sudo apt-get install -y clang llvm libelf1 libelf-dev zlibg-dev  
sudo apt install pkgconf 
```

然后从GitHub开源仓库拉取代码，即可开始编译。相关编译命令如下。

```txt
git clone https://github.com/libbpf/libbpf  
cd libbpf/src  
make  
sudo make install 
```

# 2.6 本章小结

在开始学习一门技术之前，通常需要进行大量的准备工作和技术选型。本章主要介绍了开发eBPF之前的相关环境搭建和技术选型。我们提供了多个操作系统下eBPF环境搭建的指南，并介绍了相关的编程语言选择，还详细介绍了如何安装和编译与eBPF开发相关的工具和库。环境搭建是一个相对烦琐的步骤，需要读者亲自进行实践操作。

# 第3章 Linux动态追踪技术

Linux内核从最初的1万行代码成长到今天千万行级别的海量代码，被广泛地应用在个人PC和企业服务器上。在这样一个庞大的操作系统中，出现问题如何进行排查是一个令人头疼的问题。而且有些问题难以复现，可能在运行一段时间之后才出现。面对各种各样复杂的系统和应用程序问题，利用各种Linux动态跟踪技术，可以帮助我们灵活地解决这些问题。

在遇到传统系统与软件问题时，技术人员使用软件调试技术来解决。这需要依赖调试器动态观察软件的运行，判断代码路径与产生的数据是否符合预期。在软件运行异常时，技术人员需要控制和修改软件运行时的寄存器与参数，实时找到并修正软件问题。这种技术思路与调试方法至今仍然是很多工程师的首选。但软件产业发展迅猛，现代软件的复杂程度远远超过调试分析人员的想象。而且，解决问题的人员也往往不是软件开发人员，由于软件性质与功能特性，很多软件也并不满足调试器运行环境。比如在服务器上运行的大量K8s业务节点、开启加密并拒绝软件调试的恶意程序等。

软件跟踪技术更多指的是在不侵入或少量代码侵入的情况下，被动地观察软件运行时产生的数据，进而实现软件的行为判断与优化指导的技术。一个浅显易懂的例子是：调试器观察一个程序的字符串参数的方法是通过命令读取相应寄存器的值，而动态跟踪技术是通过“代码注入Hook”或内核调试接口与内核提供的探针注册观测等方式，实时获取软件运行到特定函数时的寄存器值，并输出内容。

代码注入 Hook 技术是在软件安全领域应用较多的一种技术，市面上也有成熟的框架。比如，Frida 可以实现多个系统平台动态地向软件中注入一部分代码逻辑，从而达到观测软件运行时行为与数据的目的。这部分内容本书不予讨论，有兴趣的读者可查找相关资料学习。

使用内核调试接口来动态跟踪软件的方式即ptrace系统调用，这是一个功能强大的系统调用，可以动态观测软件执行每一条指令时的上下文信息。在这方面，有成品工具strace。它的功能非常强大，但缺点是运行时效率较低，在阻止ptrace系统调用的软件中，它不能正常工作。

探针注册观测是Linux内核提供的一种跟踪能力，它是通过静态或动态地指定需要观测的目标函数地址，让内核在运行时采集上下文信息的一种技术，它也是Linux动态追踪系统的一部分。

# 3.1 Linux动态追踪系统

Linux中的动态追踪技术可以理解为一种高级的调试技术，利用该技术可以在内核态和用户态

下进行深入分析，方便开发者或系统管理者便捷地定位和处理问题。一开始Linux并没有整体设计跟踪技术，它们是在过去十多年的发展中，慢慢演化出来的。很多读者可能听说过strace、ltrace、kprobe、tracepoint、uprobe、ftrace、perf和eBPF等名词，这么多概念交织在一起，让初学者难以理解，然而这些名词彼此之间又有一定的联系，它们的运作都依赖于Linux提供的动态追踪技术。

Brendan Gregg 提出了一种有趣的分解，他将 Linux 动态跟踪系统分为以下 3 层架构。

- 前端工具和库：用户程序进行动态追踪的工具或者编写自定义动态追踪程序的框架或库。常见的工具与库有perf、LTTng、SystemTap、trace-cmd、funcgraph、bpftrace、BCC、libbpf、tetragon、Auditid、strace、ltrace。  
- 数据采集机制：为数据源收集数据的机制，包括perf接口、ptrace系统调用、eBPF、ftrace接口、Audit子系统等导出的控制接口。  
- 数据源：跟踪数据的来源，如各种探针（Probe）及系统Hook框架。如uprobe/uretprobe、kprobe/kretprobe、tracepoint/Raw TP、fentry/fexit、ftrace Hook框架、ptrace调试机制等。Linux动态跟踪系统的3层架构如图3-1所示。

![](images/4e23550512e58119baef3960114230ff805a0eb3481c018aad25080908bbcaa3.jpg)  
图3-1 Linux动态跟踪系统的3层架构

下面将分别描述Linux动态跟踪系统3层架构中每一层的细节。

# 3.2 前端工具和库

除了开发人员，一些系统性能优化与故障排除人员也会用到跟踪工具。本节主要介绍一些常见的跟踪工具。

# 3.2.1 strace与ltrace

首先来看最早的 strace。strace 是 Linux 的早期诊断调试工具。strace 是 Paul Kranenburg 为 SunOS 编写的，1992 年，Branko Lankester 将这个版本移植到 Linux 上。

我们一般使用strace跟踪系统调用或者信号产生的情况，strace是基于ptrace（process trace）系统调用实现的。ptrace的原理如下：ptrace的字面意思是进程跟踪，它提供了一种方法，可以让父进程（Tracer）观察和控制子进程（Tracee）的执行过程，并可以检查和修改子进程的内存和寄存器。当使用ptrace后，所有发送给被跟踪的子进程的信号（除了SIGKILL）都会被转发给父进程，

而子进程会被阻塞，这时子进程的状态就会被系统标注为TASK_TRACE。父进程收到信号后，就可以对停止下来的子进程进行检查和修改，处理完后，让子进程继续运行。以上“暂停-采集-恢复执行”的过程不断重复，父进程就可以完成对子进程的整个执行流的监控和修改。

因此，ptrace主要用于实现断点调试和系统调用跟踪。

与strace类似，ltrace用于跟踪库函数的使用情况。ltrace也是基于ptrace实现的，它通过对目标进程的ELF文件中的PLT（Procedure Linkage Table）表项设置软断点的方式，插入自己的监控流程，从而监控目标程序对应库函数的调用情况。

在使用strace之前，需要先安装它。下面分别是在CentOS/EulerOS和Ubuntu系统中安装strace的方式。

在CentOS/EulerOS系统中执行如下命令。

```txt
$ sudo yum install strace 
```

在 Ubuntu 系统中执行如下命令。

```txt
$ sudo apt-get install strace -y 
```

安装后可以执行strace -h命令来查看strace的帮助（也可以使用man strace命令来查看更多的帮助信息）。

```powershell
$ strace -h
Usage: strace [-ACdfhikqqrttTvVwxyxyzZ] [-I N] [-b execve] [-e EXPR]...
[-a COLUMN] [-o FILE] [-s STRSIZE] [-X FORMAT] [-O OVERHEAD]
[-S SORTBY] [-P PATH]... [-p PID]... [-U COLUMN] [--seccomp-bpf]
{ -p PID | [-DDD] [-E VAR=VAL]... [-u USERNAME] PROG [ARGS] }
or: strace -c[dfwzZ] [-I N] [-b execve] [-e EXPR]... [-O OVERHEAD]
[-S SORTBY] [-P PATH]... [-p PID]... [-U COLUMN] [--seccomp-bpf]
{ -p PID | [-DDD] [-E VAR=VAL]... [-u USERNAME] PROG [ARGS] } 
```

strace 命令参数比较多，一些常用参数的含义如表 3-1 所示。更多的命令参数可以参考 strace -h 的输出结果。

表 3-1 strace 参数含义  

<table><tr><td>参数</td><td>含义</td></tr><tr><td>-c</td><td>统计每个系统调用的执行时间、次数和出错的次数等信息</td></tr><tr><td>-d</td><td>输出trace关于标准错误的调试信息</td></tr><tr><td>-f</td><td>跟踪目标进程及目标进程创建的所有子进程</td></tr><tr><td>-ff</td><td>如果提供-o filename,则所有进程的跟踪结果输出到相应的filename.pid中,PID是各进程的进程号</td></tr><tr><td>-F</td><td>尝试跟踪vfork调用。在参数为-f时,vfork不被跟踪</td></tr><tr><td>-h</td><td>输出帮助信息</td></tr><tr><td>-i</td><td>输出系统调用的入口指针</td></tr><tr><td>-r</td><td>打印相对时间戳</td></tr><tr><td>-t</td><td>在输出的每一行前加上时间信息</td></tr><tr><td>- tt</td><td>在每行输出的前面显示毫秒级别的时间信息</td></tr><tr><td>- ttt</td><td>在每行输出的前面显示微秒级别的时间信息</td></tr><tr><td>-T</td><td>显示每次系统调用所花费的时间</td></tr><tr><td>-v</td><td>输出环境变量、stat、文件等信息</td></tr><tr><td>-V</td><td>输出 strace 工具的版本信息</td></tr><tr><td>-o</td><td>把 strace 的输出写入指定的文件</td></tr><tr><td>-s</td><td>当系统调用的某个参数是字符串时,最多输出指定长度的内容,默认是 32 字节</td></tr><tr><td>-p</td><td>跟踪指定进程 PID,要同时跟踪多个 PID,则重复多次-p 选项即可</td></tr><tr><td>-e</td><td>指定一个限定表达式,控制如何进行跟踪</td></tr></table>

其中-e参数稍微复杂一点，它可以指定一个限定表达式，使用指定的方式过滤跟踪程序。举例如下。

-e EXPR(一个限定表达式：OPTION $= [!]$ all或者OPTION $= [!]$ VAL1[,VAL2]...）

其中OPTION可以是trace、abbrev、verbose、raw、signal、read、write、fault、inject、status、quiet、kvm、decode-fds等。

例如：

```txt
-e trace=all # 跟踪所有系统调用，默认跟踪所有  
-e trace=file # 只跟踪有关文件操作的系统调用  
-e trace=process # 只跟踪有关进程控制的系统调用，如fork/exec/exit_group  
-e trace=network # 跟踪与网络有关的所有系统调用，如socket/sendto/connect  
-e strace=signal # 跟踪所有与系统信号有关的系统调用，如 kill/sigaction  
-e trace=ipc # 跟踪所有与进程通信有关的系统调用，如write/read/select/epoll
```

也可以这样跟踪多个系统调用：

只跟踪部分有关文件操作的系统调用-e trace $\equiv$ open,close,rean,write

比如，可以通过执行如下命令来跟踪执行ls过程中与file相关的系统调用。

```txt
$ strace -c -e trace=file ls
% time seconds uses/call calls errors syscall
0.00 0.000000 0 2 2 access 
```

<table><tr><td>0.00</td><td>0.000000</td><td>0</td><td>1</td><td>execve</td></tr><tr><td>0.00</td><td>0.000000</td><td>0</td><td>2</td><td>2 statfs</td></tr><tr><td>0.00</td><td>0.000000</td><td>0</td><td>7</td><td>openat</td></tr><tr><td>0.00</td><td>0.000000</td><td>0</td><td>7</td><td>newstatat</td></tr><tr><td>100.00</td><td>0.000000</td><td>0</td><td>19</td><td>4 total</td></tr></table>

笔者使用的是 Ubuntu 环境，在执行上述命令后，统计出了执行 ls 命令时与 file 相关的所有系统调用。由于用了 -c 参数，所以统计每个系统调用执行的时间、次数和出错的次数等信息。

有时候可能想知道系统调用的执行时间顺序，以及每次执行所花费的时间，那么可以加上-tt和-T参数。如下输出，每一行都是一条系统调用的执行信息，首先是执行的详细时间，然后是一个等号表达式，等号左边是系统调用的函数名及其参数，右边是该调用的返回值。最后的尖括号内是执行时间。

```txt
$ strace -tt -T -s 256 -e trace=file ls
23:02:15.788250 execve("/usr/bin/ls", ["ls"], 0x7fffec85e5cf0 /* 56 vars */ = 0 <0.000430>
23:02:15.789939 access("/etc/ld.so.preload", R_OK) = -1 ENOENT (No such file or directory)
<0.000153>
23:02:15.790398 openat(AT_FDCWD, "/etc/ld.so.cache", O_RDONLY|O_CLOEXEC) = 3 <0.000119>
23:02:15.790735 newstatat(3, "", {st_mode=S_IFREG|0644, st_size=67083, ...}, AT emptied_
PATH) = 0 <0.000130>
...
23:02:15.798558 newstatat(3, "", {st_mode=S_IFREG|0644, st_size=14575936, ...}, AT_
EMPTY_PATH) = 0 <0.000040>
23:02:15.799006 openat(AT_FDCWD, ".", O_RDONLY|O_NONBLOCK|O_CLOEXEC|O_DIRECTORY) = 3
<0.000041>
23:02:15.799112 newstatat(3, "", {st_mode=S_IFDIR|0755, st_size=4096, ...}, AT emptied_
PATH) = 0 <0.000040>
23:02:15.799824 +++ exited with 0+++ 
```

当然，我们可以看到strace在跟踪系统调用方面非常好用。但是strace也有其局限性。

- 每次执行系统调用都要通知父进程进行处理，需要进行额外的进程切换，在高频繁的系统调用场景下，性能开销比较大。  
- 当目标进程卡在用户态时，strace 就无法输出，所以不适合线上环境使用。

这个时候可以使用其他的一些动态跟踪手段。

# 3.2.2 DTrace

DTrace 可以算得上是现代动态追踪技术的鼻祖了，它是由 Sun 公司开发的一款用于定位系统性能问题和调试系统错误的动态跟踪工具，于 21 世纪初开始被用于 Solaris 操作系统。后来它被移植到 Linux、FreeBSD、NetBSD，以及 macOS 等操作系统上。iOS 上的 Instrument 工具也是基于 DTrace 实现的。

DTrace 可以由管理员和开发者使用，并且可以在实时生产系统上安全使用。使用 DTrace 可以

检查用户程序的行为和操作系统的行为。DTrace 拥有遍布于内核态和用户态程序的大量探针，这些探针可以监控关键函数的运行时状态。这些探针分布在执行流的各个关键路径上，只有在需要时才打开。同时 DTrace 可以通过 D 脚本语言创建定制程序来提供动态检测系统的能力，以及安全的探针执行环境。基于这些特性，Linux 系统在 DTrace 诞生后很多年都没有一套可以与之媲美的工具。我们可以通过 dtrace4linux 在 Linux 上使用 DTrace，但是 DTrace 对 Linux 的支持并不是很好，总会出现各种问题。现在通过 eBPF，Linux 终于具备了一套强大的底层探针底座，所以 eBPF 也被 Branden Gregg 称为 Linux 上的 DTrace。

# 3.2.3 SystemTap

SystemTap是与DTrace同时代的动态跟踪框架，它于2005年在RedHatEnterpriseLinux4Update

2中首次亮相，又经过4年的开发，SystemTap1.0于2009年发布。截至2011年，所有Linux发行版都完全支持SystemTap。

SystemTap基于kprobe提供的API来实现，定义了一个事件（event）和处理该事件的句柄（handler），当一个特定的事件发生时，内核运行该处理句柄，就像快速调用一个子函数一样，处理完之后恢复内核原始状态。同时SystemTap提供了一套编程语言，允许用户编程，然后它将程序翻译成C语言并编译成内核模块执行。SystemTap从功能上来说是非常强大的，但是SystemTap与DTrace相比易用性太差，它不像DTrace开箱即用，依赖非常多，且由新人编写的程序容易造成内核崩溃。SystemTap的执行流程如图3-2所示。SystemTap不在本书的讨论范围内，有兴趣的读者可以自行了解相关信息。

![](images/6015dfc50ed7f87620df505885f1e9b811765931def4978eaf96d94b6b6e4a62.jpg)  
图3-2 SystemTap的执行流程

# 3.2.4 LTTng

LTTng（The Linux Trace Toolkit Next Generation）是一套开源的跟踪工具，用于跟踪Linux内核、用户程序和用户库。该项目由Mathieu Desnoyers发起，并于2005年首次发布。其前身是Linux Trace Toolkit。LTTng使用Linux内核的Tracepoint工具，以及其他信息源，如kprobe和perf性能监控计数器。它旨在将性能影响降至最低，在没有跟踪的情况下对系统的影响几乎为零，对于调试大范围内的bug比较有用。如今LTTng已支持多个发行版（Ubuntu、Dibian、 Fedora、OpenSUSE、

Arch等）和多种架构，此外官方称还支持Android和FreeBSD系统。读者可以访问其官网以了解更多信息。

# 3.2.5 trace-cmd

trace-cmd是ftrace框架的前端工具，最早是作为ftrace的一个补充工具。尽管TraceFS的接口比较简单，但通过TraceFS使用ftrace仍比较麻烦，需要手动地往配置文件里面写入Trace配置信息。关于如何配置，3.4节会详细介绍。

trace-cmd 与 ftrace 一起工作会更加方便，此时不用编写各种命令以从各种文件中读取结果。随着时间的推移，trace-cmd 逐渐发展成为一种独立的内核跟踪工具。在 2011 年 12 月，它成为 Linux 内核源代码的一部分，并在此后得到了更多的开发和改进。

# 3.2.6 perf

perf是Linux中的性能分析工具。它最初叫作Performance Counter，在Linux内核版本2.6.31中第一次亮相。在Linux内核版本2.6.32中它正式改名为Performance Event。perf提供了很多子命令，可对整个系统（内核和用户态代码）进行统计分析，从而全面理解应用程序中的性能瓶颈，因而在Linux性能分析和观测上有着广泛的应用。

perf可以利用PMU（PerformanceMonitoringUnit）、tracepoint和内核中的特殊计数器进行性能统计。其中，PMU是处理器中的部件，用于针对某种硬件事件设置计数器，设置完后，处理器就开始统计该事件的发生次数，进而可以观测程序中与CPU有关的事件（执行指令数、捕获异常数、时钟周期数等）、与Cache有关的事件（data/inst/L1/L2 Cache访问次数、miss次数等），以及与TLB有关的事件等。

tracepoint是预埋在内核源码中的Hook点，代码执行到Hook点时会触发，这些Hook点可以被Linux的各种跟踪工具所使用，perf会将tracepoint产生的时间记录下来，这些tracepoint对应的sysfs节点在/sys/kernel debug/tracing/events目录下。通过perf采集到的记录，我们可以了解程序执行期的行为，进而可以分析程序及进行性能调优。图3-3展示了perf的所有事件源。

接下来看一下perf命令工具的使用。Linux perf是一个轻量级命令行实用程序，用于分析和监视Linux系统上的CPU性能。

以 Ubuntu 操作系统为例，安装 linux-tools 工具包，其中就带有 perf 工具。

```shell
sudo apt install linux-tools-\$ (uname -r) 
```

perf目前拥有31个子命令，用于收集、跟踪和分析CPU事件数据。我们可以使用--help参数来查看详细命令的描述信息。

```txt
$ perf --help 
```

```txt
usage: perf [--version] [--help] [OPTIONS] COMMAND [ARGS] 
```

![](images/837a60a554894f55a0e5d33cee1aa3fec643bd8cab52823272e518372e4d9de2.jpg)  
图3-3perf事件源（图片来自brendangregg的博客）

读者也可以执行如下命令来查看子命令的详细用法。

```txt
perf help [子命令] 
```

关于31个子命令的简单描述如表3-2所示。

表 3-2 perf 子命令  

<table><tr><td>序号</td><td>命令</td><td>说明</td></tr><tr><td>1</td><td>anotate</td><td>解析perf record生成的perf.data文件，显示被注释的代码</td></tr><tr><td>2</td><td>archive</td><td>根据数据文件记录的 build-id，将所有被采样的 elf 文件打包。利用此压缩包，可以在任何机器上分析数据文件中记录的采样数据</td></tr><tr><td>3</td><td>bench</td><td>perf中内置的 benchmark, 目前包括两套针对调度器和内存管理子系统的 benchmark</td></tr><tr><td>4</td><td>buildid-cache</td><td>管理 perf 的 build_id 缓存，每个 elf 文件都有一个独一无二的 build_id。build_id 被 perf 用来关联性能数据与 elf 文件</td></tr><tr><td>5</td><td>buildid-list</td><td>列出数据文件中记录的所有 build_id</td></tr><tr><td>6</td><td>c2c</td><td>调试 Cache to Cache 的伪共享问题,用于 Shared Data C2C/HITM 分析,可以追踪缓存行竞争问题</td></tr><tr><td>7</td><td>config</td><td>用于读取和设置.perfconfig 配置文件</td></tr><tr><td>8</td><td>daemon</td><td>在后台运行会话记录</td></tr><tr><td>9</td><td>data</td><td>数据文件相关处理</td></tr><tr><td>10</td><td>diff</td><td>对比两个数据文件的差异。能够给出每个符号(函数)在热点分析上的具体差异</td></tr><tr><td>11</td><td>evlist</td><td>列出数据文件 perf.data 中的所有性能事件</td></tr><tr><td>12</td><td>ftrace</td><td>内核 ftrace 功能的简化封装,可以跟踪指定进程的内核函数调用栈</td></tr><tr><td>13</td><td>inject</td><td>读取 perf record 工具记录的事件流,并将其定向到标准输出</td></tr><tr><td>14</td><td>iostat</td><td>显示 I/O 性能指标</td></tr><tr><td>15</td><td>kallsyms</td><td>查找运行中的内核符号</td></tr><tr><td>16</td><td>kmem</td><td>针对内核内存(slab)子系统进行追踪测量的工具</td></tr><tr><td>17</td><td>kvm</td><td>用于测试 KVM 客户机的性能参数</td></tr><tr><td>18</td><td>list</td><td>列出 event 事件</td></tr><tr><td>19</td><td>lock</td><td>分析内核锁统计信息</td></tr><tr><td>20</td><td>mem</td><td>测试内存存取性能数据</td></tr><tr><td>21</td><td>record</td><td>运行一个命令,并将数据保存到 perf.data 中,随后可以使用 perf report 进行分析</td></tr><tr><td>22</td><td>report</td><td>读取当前目录的 perf.data,显示 perf 数据</td></tr><tr><td>23</td><td>sched</td><td>分析调度器性能</td></tr><tr><td>24</td><td>script</td><td>执行测试脚本</td></tr><tr><td>25</td><td>stat</td><td>完整统计应用的整个生命周期的信息</td></tr><tr><td>26</td><td>test</td><td>用于可用性测试</td></tr><tr><td>27</td><td>timechart</td><td>生成图标</td></tr><tr><td>28</td><td>top</td><td>类似于 Linux 中的 top 命令,查看整体性能</td></tr><tr><td>29</td><td>version</td><td>查看版本信息</td></tr><tr><td>30</td><td>probe</td><td>定义新的动态跟踪点</td></tr><tr><td>31</td><td>trace</td><td>跟踪系统调用</td></tr></table>

下面看看一些常见的perf子命令。

top

top 可以实时显示系统当前的性能统计信息，可以用来查找和定位损耗性能的程序。

$ sudo perl top

Samples: 8K of event 'cpu-clock:pppH',4000 Hz, Event count (approx.): 99712896 lost:

```txt
0/0 drop: 0/0  
Overhead Shared Object Symbol  
10.25% [kernel] [k] _rawspin_unlockrestore  
4.52% perf [. ] __symbols__insert  
2.15% [kernel] [k] iowrite16 
```

# stat

stat的作用是执行一个命令并收集其运行过程中的数据，它可以提供一个程序或者系统运行情况的总体概览。

```txt
$ sudo perf stat -a sleep 5 
```

```txt
Performance counter stats for 'system wide': 
```

```txt
10,005.24 msec cpu-clock # 2.000 CPUs utilized  
4,844 context-switches # 484.146 /sec  
323 cpu-migrations # 32.283 /sec  
2,959 page-faults # 295.745 /sec  
<not supported> cycles  
<not supported> instructions  
<not supported> branches  
<not supported> branch-misses 
```

```txt
5.002797237 seconds time elapsed 
```

在这个例子中，我们执行stat来提供系统5秒内的整体概览，将运行过程中的一些指标进行汇总显示。其中指标的含义如表3-3所示。

表 3-3 perf stat 指标含义  

<table><tr><td>指标名称</td><td>说明</td></tr><tr><td>msec cpu-clock</td><td>运行perf的这段时间内的CPU利用率，该值高说明程序的多数时间花费在CPU计算上而非I/O上</td></tr><tr><td>context-switches</td><td>上下文切换次数，前半部分是切换次数，后面是平均每秒发生次数。应避免频繁地进行上下文切换，这样会损耗性能</td></tr><tr><td>page-faults</td><td>发生缺页的次数</td></tr><tr><td>cpu-migrations</td><td>进程运行过程中发生的CPU迁移次数，即被调度器从一个CPU转移到另一个CPU上运行的次数</td></tr><tr><td>cycles</td><td>处理器时钟，一条机器指令可能需要多个cycles</td></tr><tr><td>instructions</td><td>任务在执行期间完成的CPU指令数</td></tr><tr><td>branches</td><td>任务在执行期间发生的分支预测的次数</td></tr><tr><td>branch-misses</td><td>任务在执行期间发生的分支预测失败的次数</td></tr></table>

除此之外我们还可以加上-e参数，指定自己感兴趣的事件或者系统调用。执行如下命令来指定在采样5分钟内，与sys-enter相关的系统调用发生了多少次。

```txt
$ sudo perf stat -e 'syscalls:sys-enter_*' -a sleep 5
Performance counter stats for 'system wide':
...
0 syscalls:sys-enter_SOCKET
0 syscalls:sys-enter_SOCKETpair
0 syscalls:sys-enter_bind
152 syscalls:sys-enter_epoll_wait
472 syscalls:sys-enter_epoll_pwait
0 syscalls:sys-enter_dup
...
0 syscalls:sys-enter_select
11 syscalls:sys-enter_pselect6
621 syscalls:sys-enter POLL
0 syscalls:sys-enter_ppoll
0 syscalls:sys-enter_getdents
0 syscalls:sys-enter_getdents64
3,200 syscalls:sys-enter_iocl
0 syscalls:sys-enter_fcntl
...
5.014986809 seconds time elapsed
```

# record/report

record 会生成相关的统计信息，它不会将结果显示出来，而是将结果输出到文件中，然后通过 script 或者 report 进行解析。与 stat 一样，也可以用 -e 跟踪指定的事件。例如，我们可以统计 2 秒内系统打开的文件。

```batch
sudo perf record -e 'syscall1:sys-enter_openat' -aR sleep2 
```

# 3.3 数据采集机制

Linux内核提供了一系列接口，用于用户态的数据交换。这些接口包括ptrace用户态编程接口、perf接口、ftrace接口、eBPF和Audit子系统等。

ptrace 用户态编程接口是一组编程接口，主要用于 ptrace 系统调用与底层系统内核接口通信。perf 接口依靠 perf_event_open 系统调用，与内核进行通信来采集 perf_event。ftrace 接口使用 TraceFS 导出的可配置文件接口来设置 Probe，数据多是由 trace_pipe 这类 ftrace 输出接口传递。

在很长一段时间里，eBPF由BPF系统调用加载eBPF字节码后，通过编码ftrace接口来设置Probe。但阅读libbpf、BCC、bpftrace这类工具的代码实现可以发现，在新版本的内核中，Probe

的设置改成了perf接口，也就是采用perf_event_open系统调用来采集perf_event，它通过ioct1与perf的文件句柄进行关联。当然，代码中也保留了低版本内核采用的ftrace接口方式。对细节感兴趣的读者，可以阅读libbpf仓库下src/libbpf.c文件中bpf.program__attach_uprobe()方法的实现。

# 3.3.1 ptrace系统调用

ptrace 是一个特殊的系统调用，它在 Linux 和 UNIX 类操作系统中提供了对其他进程的跟踪和控制能力。ptrace 主要用于调试、监视和分析目标进程的行为。

以下是ptrace的常见用途。

- 进程跟踪：通过ptrace可以追踪目标进程的执行过程，包括指令级别的单步执行、读取/写入寄存器或内存等。这对调试程序或观察其运行时状态非常有用。  
- 断点设置与处理：使用ptrace可以在目标进程中设置断点，并在断点处停止目标进程的执行。一旦到达断点，父进程会收到通知并做出相应处理，比如修改寄存器值、检查内存内容等。  
- 内存读写：通过ptrace PEEKDATA/POKEDATA系统调用，在不干扰目标进程正常运行的情况下，父进程可以读取或修改目标进程的内存数据。  
- 代码注入与函数挂钩：利用ptrace可以向目标进程注入代码，并改变其行为。这种技术被广泛应用于动态链接库注入、函数替换及软件漏洞分析等领域。

需要注意的是，ptrace 是一个强大而敏感的系统调用，一般只有特权进程（如 root 用户或具有相应权限的用户）才能使用它。此外，滥用 ptrace 可能会对系统安全性产生负面影响，因此在实际使用时需要谨慎操作。

strace 这个强大的跟踪工具就是基于 ptrace 实现的。在 Linux 系统上，GDB 的底层实现也依赖于它。

# 3.3.2 perf_event_open系统调用

perf_event_open是Linux系统中的一个系统调用，它提供了一种性能计数器接口，用于收集和分析程序在运行时的性能数据。通过perf_event_open系统调用，可以监控各种硬件事件（例如指令执行、缓存命中率等）和软件事件（例如函数调用次数、上下文切换次数等），从而深入了解程序的性能特征。

以下是perf_event_open的一些主要用途。

- 性能分析：通过使用perf_event_open，可以测量目标进程或线程在运行期间发生的硬件和软件事件数量，并获得与CPU、内存、I/O等相关的详细统计信息。这有助于识别瓶颈、优化算法或代码，并改善应用程序的性能。  
调试：借助perf_event_open，开发者可以编写自定义调试工具来跟踪和记录关键事件，如

函数调用堆栈跟踪、内存访问追踪等。这对定位问题和进行故障排除非常有帮助。

- 系统监控：利用perf_event_open，可以实时监控整个系统或单个进程/线程的资源利用情况，包括CPU占用率、内存使用、I/O操作等。这对系统性能调优和资源管理非常有用。

使用perf_event_open需要一定的编程知识，需要在 $\mathrm{C / C + + }$ 或其他支持该系统调用的语言中进行相关代码开发。具体而言，需要创建一个perf_event_attr结构体来描述所需监控事件，并通过perf_event_open调用打开并启动计数器，然后通过读取文件描述符可获取相应的性能数据。

需要注意的是，在使用perf_event_open时，要确保以适当的权限运行程序（通常需要root权限或CAP_SYSADMIN权限），以便访问底层硬件计数器。

在第4章中将会继续讨论如何使用perf_event_open系统调用。

# 3.3.3 BPF系统调用

BPF 是一个强大的系统调用，它在 Linux 内核中提供了一种灵活和高效的数据包过滤和处理机制。BPF 技术最初是由美国伯克利大学开发出来的，现在已经成为 Linux 内核的一个重要组件。

BPF系统调用的最初设计用途是数据包过滤，配合全新设计的BPF字节码执行虚拟机环境，将BPF程序加载到内核中执行，以实现网络数据包的实时过滤。随着eBPF的快速发展，BPF系统调用被扩展成一个功能强大的Hook框架。BPF字节码可以被挂载到Linux系统内核及用户空间等多个场景下执行。

需要注意的是，BPF技术非常强大且处于底层，正确地使用它需要深入理解网络协议、操作系统内核及相关安全风险。因此，在实际应用时请仔细阅读官方文档和参考资料，并确保遵循最佳实践。

# 3.3.4 其他子系统与内核模块

Audit子系统被广泛应用于企业IDS（入侵检测系统）开发中，比如微信出品的相关Linux安全产品软件，在底层就是通过Audit子系统来实现对系统的安全审计；国内一些安全厂商也是结合内核模块与Audit子系统来完成相应的安全功能。

在eBPF被广泛使用后，基于eBPF实现的安全软件逐渐增多，一些基于内核模块实现的安全防护产品也在积极考虑使用eBPF技术来实现。

# 3.4 跟踪文件系统

TraceFS 又称为跟踪文件系统，它提供了与用户态下访问控制内核跟踪相关的探针功能。TraceFS 提供了丰富、强大的跟踪和性能分析工具和接口，可以在系统分析、性能优化、问题调试等方面提供很大的帮助。需要注意的是，由于 Trace 机制的开销可能会引起系统性能损失，因此在

生产环境中需要谨慎使用。

读者可以执行如下命令，确保当前系统支持 TraceFS。如果不支持则需要重新编译内核并启用 TraceFS 模块。与跟踪相关的内核配置在 3.4.4 节介绍。

```txt
$ grep -i tracefs /proc/filesystem
nodev tracefs 
```

# 3.4.1 挂载位置

我们可以通过mount命令查看当前系统的TraceFS挂载在哪个目录下：

```txt
$ mount | grep tracefs
tracefs on /sys/kernel/tracing type tracefs (rw,nosuid,nodev,noexec,relatime) 
```

这里需要注意的是，在Linux内核4.1版本之前，TraceFS是挂载在debugfs下的。

```txt
$ mount | grep debugfs /sys/kernel-debug/tracing 
```

debugfs与procs和sysfs类似，procs提供与进程相关的信息，sysfs将内核对象导出到用户空间进行配置，debugfs是专为调试设计的文件系统，这些文件系统并不实际存储在硬盘上，而是在系统内核运行起来后创建的基于内存的文件系统。debugfs在Linux内核2.6.10-rc3版本中出现，由Greg Kroah-Hartman设计实现。

# 3.4.2 目录详情

TraceFS目录下有非常多的子目录，这些子目录有不同的配置或者功能。笔者机器的子目录中有如下一些文件。

```txt
# ubuntu 22.04, 内核版本5.19.17
/sys/kernel/tracing# ls
available_events hwlat_detector set_event.pid trace_clock
available_filter-functions instances set_ftrace_filter trace Marker
available_tracers kprobe_events set_ftrace_notrace trace Marker_raw
buffer_percent kprobe_profile set_ftrace_notrace pid trace options
buffer_size_kb max_graph_depth set_ftrace.pid trace_pipe
buffer_total_size_kb options set_graph_function trace stat
current_tracer per_cpu set_graph_notrace tracing_cpusmask
dynamic_events printfFormats snapshot tracing_max-latency
dyn_ftrace_total_info README stack_max_size tracing_on
enabled-functions saved_cmdlines stack_trace tracing thresh
error_log saved_cmdlines_size stack_trace_filter uprobe_events
events saved_tgids synthetic_events uprobe_profile
free_buffer set_event timestamp_mode
function_profile_enable set_event_notrace.pid trace 
```

其中有6个目录和48个文件。读者可以阅读目录下的README 文件来简单了解每个目录和文件的含义。内核一直在更新变化，TraceFS也会随之引入新的特性，每个文件和文件中的内容以Linux内核官方文档为准。

笔者整理的关于 TraceFS 根目录下相关文件和目录的描述如表 3-4 所示。读者无需记忆，使用的时候查询即可。

表 3-4 TraceFS 根目录下相关目录和文件的说明  

<table><tr><td>文件名</td><td>类型</td><td>说明</td></tr><tr><td>events</td><td>目录</td><td>跟踪事件目录,其中包含已编译到内核中的事件跟踪点(也称为静态跟踪点)。它显示了存在哪些事件跟踪点及它们如何按系统分组。在各个级别上有 enable 文件,当向其写入“1”时,可以启用跟踪点</td></tr><tr><td>hwlat_detector</td><td>目录</td><td>硬件延迟检测器的目录</td></tr><tr><td>instances</td><td>目录</td><td>创建多个 ring buffer(环形缓冲区)的方法,可以让不同的 event 使用不同的 ring buffer</td></tr><tr><td>options</td><td>目录</td><td>目录中包含每个可用于跟踪选项的文件(也在 trace_options 中)。可以通过向具有相应选项名称的对应文件中分别写入“1”或“0”来设置或清除选项</td></tr><tr><td>per_cpu</td><td>目录</td><td>包含每个 CPU 的跟踪信息的目录</td></tr><tr><td>trace_stat</td><td>目录</td><td>保存不同的追踪统计信息</td></tr><tr><td>available_events</td><td>文件</td><td>记录当前可用的可追踪事件</td></tr><tr><td>available_filter-functions</td><td>文件</td><td>记录当前可以跟踪的内核函数</td></tr><tr><td>available_tracers</td><td>文件</td><td>记录当前可以用的 tracer</td></tr><tr><td>buffer_percent</td><td>文件</td><td>用于设置预分配缓冲区的百分比。该值是一个整数,表示预分配缓冲区大小占缓冲区总大小的百分比。例如,buffer_percent 设置为50,则 ftrace 会在启动时预分配缓冲区的一半大小。预分配缓冲区可以让 ftrace 更加高效地进行跟踪操作,但也会占用更多的内存资源。通常情况下,可以设置为50~90</td></tr><tr><td>buffer_size_kb</td><td>文件</td><td>用于设置单个CPU所使用的跟踪缓存的大小</td></tr><tr><td>buffer_total_size_kb</td><td>文件</td><td>用于显示和设置跟踪缓冲区的总大小,单位为KB</td></tr><tr><td>current_tracer</td><td>文件</td><td>用于设置或者显示当前使用的跟踪器列表</td></tr><tr><td>dynamic_events</td><td>文件</td><td>创建、附加、删除、显示通用动态事件,向此文件写入以定义或取消定义新的跟踪事件,这些事件可以是内核事件、用户空间事件或硬件事件等</td></tr><tr><td>dyn_ftrace_total_info</td><td>文件</td><td>显示 available_filter functins 中跟踪函数的数目</td></tr><tr><td>enabled-functions</td><td>文件</td><td>用于调试 ftrace,该文件显示所有已附加回调的函数及已附加的回调数量。注意,回调也可能调用多个函数,这些函数不会在此计数中列出</td></tr><tr><td>error_log</td><td>文件</td><td>失败命令的错误日志</td></tr><tr><td>free_buffer</td><td>文件</td><td>如果一个进程正在执行跟踪操作,当该进程结束时(即使它被信号杀死),希望缩小“释放”环形缓冲区,可以使用这个文件来实现。关闭该文件时,环形缓冲区的大小将调整为最小值。如果一个正在跟踪的进程也打开了这个文件,在进程退出时,此文件的文件描述符将被关闭,并且环形缓冲区将被“释放”。如果设置了 disable_on_free 选项,则可能停止跟踪</td></tr><tr><td>function_profile_enabled</td><td>文件</td><td>当设置后,它将启用所有函数跟踪器和函数图跟踪器。它将保存被调用的函数数量的直方图,如果配置了函数图跟踪器,它还将跟踪这些函数花费的时间。直方图内容可以在以下文件中显示:trace.stats / function (function0、function1 等)</td></tr><tr><td>kprobe_events</td><td>文件</td><td>创建、附加、删除、显示内核动态事件,向此文件写入以定义或取消定义新的跟踪事件</td></tr><tr><td>kprobe_profile</td><td>文件</td><td>动态跟踪点统计信息。请参阅 kprobetrace.txt(https://www.kernel.org/doc/Documentation/trace/kprobetrace.txt)</td></tr><tr><td>max_graph_depth</td><td>文件</td><td>使用 function_graph 跟踪器时,这是它将跟踪到函数的最大深度。将其设置为 1 ,将只显示从用户空间调用的第一个内核函数</td></tr><tr><td>printkFormats</td><td>文件</td><td>这是针对读取原始格式文件的工具。如果环形缓冲区中的事件引用了一个字符串,那么只会将指向该字符串的指针记录到缓冲区中,而不是记录字符串本身。此文件显示了字符串及其地址,以便工具将指针映射到相应的字符串内容</td></tr><tr><td>saved_cmdlines</td><td>文件</td><td>这是 ftrace 建立的一个 Cache,用来记录进程 PID 和 comms 之间的映射关系,在输出时能根据 PID 快速找到进程的 comms。如果进程的 comms 没有缓存,使用空白填充 &lt; ... &gt;</td></tr><tr><td>saved_cmdlines_size</td><td>文件</td><td>默认情况下会保存 128 个 comms。要增加或减少缓存的 comms 数量,可将要缓存的 comms 数目作为参数传递给该文件。可以使用 echo 命令实现此操作</td></tr><tr><td>saved tgids</td><td>文件</td><td>如果设置了 record-tgid 选项,则在每次调度上下文切换时,任务组 ID (TGID)将保存在一个表中,该表将线程的 PID 映射到 TGID。默认情况下,record-tgid选项被禁用</td></tr><tr><td>set_event</td><td>文件</td><td>将事件写入此文件中,将会启用该事件</td></tr><tr><td>set_event_no trace pid</td><td>文件</td><td>让事件不跟踪具有此文件中列出的 PID 的任务。请注意,如果 sched_switch 或 sched_wakeup 事件也跟踪应跟踪的线程,则它们也将跟踪未在此文件中列出的线程,即使线程的 PID 在文件中也是如此。要在 fork 上添加此文件中的任务子级 PID,请启用 event-fork 选项。当任务退出时,该选项还将导致任务的 PID 从此文件中删除</td></tr><tr><td>set_event.pid</td><td>文件</td><td>只有在该文件中列出 PID 的任务才会被跟踪。但需要注意的是,sched_SWITCH和 sched_wakeup 也会跟踪该文件中列出的事件。如果想要自动将该文件中任务的子 PID 加入跟踪列表,启用 event-fork 选项。此选项还会在任务退出时自动从跟踪列表中移除任务的 PID</td></tr><tr><td>set_ftrace_filter</td><td>文件</td><td>当配置动态 ftrace 时,代码将被动态修改,以禁用函数分析器 (mcount) 的调用。这样可以在几乎没有性能开销的情况下进行跟踪配置。同时,这也会影响是否启用或禁用特定函数的跟踪。在此文件中写入函数名称,将限制跟踪范围仅包括这些函数。这会影响 function 和 function_graph 跟踪器,从而也会影响函数分析(参见 function_profile_enabled 文件)。available_filter-functions 中列出的函数名称可以写入此文件中</td></tr><tr><td>set_ftrace_no trace</td><td>文件</td><td>这个功能的作用与 set_ftrace_filter 相反。在这里添加的任何函数都不会被追踪。如果一个函数同时存在于 set_ftrace_filter 和 set_ftrace_notrace 中,那么该函数将不会被追踪。换句话说,set_ftrace_notrace 可以用来排除不想跟踪的函数</td></tr><tr><td>set_ftrace_notrace.pid</td><td>文件</td><td>这个选项可以让函数跟踪器忽略在一个特定文件中列出的进程ID所对应的线程。如果将function-fork选项设置为开启状态,在一个被列入该文件的PID对应的任务进行fork操作时,子进程的PID将自动添加到该文件中,并且子进程也不会被函数跟踪器跟踪。此外,该选项还会从文件中移除已经退出的任务的PID。当一个PID同时出现在该文件和set_ftrace.pid文件中时,该文件的优先级更高,因此该线程将不会被跟踪</td></tr><tr><td>set_ftrace.pid</td><td>文件</td><td>此选项允许仅追踪在一个特定文件中列出的进程ID所对应的线程。如果将function-fork选项设置为开启状态,在一个被列入该文件的PID对应的任务进行fork操作时,子进程的PID将自动添加到该文件中,并且子进程也会被函数跟踪器跟踪。此外,该选项还会从文件中移除已经退出的任务的PID</td></tr><tr><td>set_graph_function</td><td>文件</td><td>如果在该文件中列出函数,则graph_function追踪器只会跟踪这些函数及它们调用的其他函数。注意,set_ftrace_filter和set_ftrace_notrace仍然会影响正在被跟踪的函数</td></tr><tr><td>set_graph_notrace</td><td>文件</td><td>与set_graph_function类似,但当函数被执行后,它会禁用graph_function追踪直到函数退出。这使得可以忽略特定函数调用的跟踪函数</td></tr><tr><td>snapshot</td><td>文件</td><td>显示snapshot缓存中的内存,类似trace文件。snapshot对应一块独立的ring buffer,用来快照ring buffer中的内容</td></tr><tr><td>stack_max_size</td><td>文件</td><td>当堆栈跟踪器被激活时,该命令将显示它所遇到的最大堆栈大小</td></tr><tr><td>stack_trace</td><td>文件</td><td>stack tracer遭遇的最大的堆栈的具体回调情况</td></tr><tr><td>stack_trace_filter</td><td>文件</td><td>stack tracer的filter,指示哪些函数可以被stack tracer跟踪</td></tr><tr><td>synthetic_events</td><td>文件</td><td>合成事件是动态创建的事件,通过匹配另外两个事件触发。一个事件是开始事件,并记录了某些字段值;当执行第2个事件时,如果它有与开始事件的某些字段值相匹配的字段,则会触发合成事件。除了匹配的字段,合成事件还可以传递其他字段值进行计算或者作为参数使用</td></tr><tr><td>timestamp_mode</td><td>文件</td><td>某些跟踪器可能会更改记录跟踪事件到事件缓冲区时使用的时间戳模式。具有不同模式的事件可以共存于一个缓冲区中,但在记录事件时有效的模式决定该事件所使用的时间戳模式。默认的时间戳模式是delta delta:时间戳是相对于每个缓冲区时间戳的差值absolute:时间戳是完整的时间戳,而不是相对于其他值的差值。因此占用更多空间,效率较低</td></tr><tr><td>trace</td><td>文件</td><td>该文件保存了跟踪的输出内容,以较好的可读格式呈现。使用O_TRUNC标志打开该文件进行写操作时会清除ring buffer中的内容。需要注意的是,在这个文件打开时,ring buffer是临时关闭的。读操作并不会清除ring buffer中的数据,可以重复读此文件</td></tr><tr><td>trace_clock</td><td>文件</td><td>当系统中发生某个事件并被记录到环形缓冲区时,就会在该事件上添加一个时间戳。这个时间戳来源于指定的时钟。ftrace是Linux系统中的一个性能分析工具,它默认使用本地时钟来给事件打上时间戳。本地时钟很快且每个CPU都有自己的本地时钟,本地时钟可能无法与其他CPU上的本地时钟同步,这将导致事件的时间戳有误差</td></tr><tr><td>trace Marker</td><td>文件</td><td>该文件运行用户态直接写内容到 ring buffer。通常用来同步用户态和内核态的事件</td></tr><tr><td>trace Marker raw</td><td>文件</td><td>与 trace Marker 类似,但用于写入二进制数据,可以使用工具从 trace_pipe_raw 中解析数据</td></tr><tr><td>trace options</td><td>文件</td><td>用来控制 trace 文件的输出格式,不设置则由 trace 自动配置</td></tr><tr><td>trace_pipe</td><td>文件</td><td>文件内容和 trace 文件是一样的,区别在于:并行读,读操作不会“disable”写操作;只支持读一次,该读操作会清除 ring buffer 中的数据,再次读则没有内容了</td></tr><tr><td>tracing_cpumask</td><td>文件</td><td>能让用户只在特定 CPU 上进行跟踪的掩码。格式为一个十六进制字符串,表示CPU 的编号</td></tr><tr><td>tracing_max-latency</td><td>文件</td><td>一些跟踪器会记录最大延迟,如中断被禁用的最长时间,最长时间将保存在这个文件中。最大跟踪也将被存储,并通过 trace 显示。仅当延迟大于此文件中的值(以微秒为单位)时,才会记录新的最大跟踪。向这个文件中发送一个时间值,只有当延迟大于此文件中的值时才会记录延迟</td></tr><tr><td>tracing_on</td><td>文件</td><td>设置是否启用 ring buffer,0 是禁用,1 则是启用。注意这个只是 ring buffer 的控制开关,但是各种插桩函数仍然会被调用,开销依然有</td></tr><tr><td>tracing thresh</td><td>文件</td><td>一些延迟跟踪器将在延迟大于此文件中的数字时记录跟踪。仅当文件中包含大于 0 的数字时才处于活动状态(单位为微秒)</td></tr><tr><td>uprobe_events</td><td>文件</td><td>在程序中添加 uprobe 动态跟踪点</td></tr><tr><td>uprobe_profile</td><td>文件</td><td>uprobe 的统计功能</td></tr></table>

以events结尾的文件保持着当前TraceFS中正在监测的函数列表，其中dynamic_events包含了其他所有以events结尾的文件的内容，如kprobe_events、uprobe_events等。available_events是特殊的，这个文件描述了跟踪点的列表，即在available_filter-functions的基础上挑选的稳定的API的列表，由于Linux内核开发者维护，在内核版本发生改变时，这些API没有发生变化。所以跨内核版本编写程序时，可以多使用available_events中列举的函数。

events 目录分类存放了所有可被监测的点，每个目录代表不同的分类。

```shell
cd /sys/kernel/tracing/events   
/sys/kernel/tracing/events# ls   
alarmtimer enable hvmon mce pagemap scsi   
v412   
amd_cpu error_report hyperv mctp page_pool signal vb2   
svc exceptions i2c mdio percpu skb virtiogpu   
block ext4 initcall migrate power smbus vmscan   
bpf_test_run fib intel_iommu mmap sock vsyscall 
```

```txt
bpf_trace fib6 interconnect mmap_lock pwm spi  
bridge filelockIOCOSTMMCqdisc swiotlb  
cgroup filemap iomap module ras sync_trace  
writeback  
clk fs_dax iommu mptcp raw_syscalls syscalls  
x86_fpu  
compaction ftrace io_uring msr rcu task  
xdp  
cpuhp gpio IRQ napi regmap tcp  
xen  
cros_ec hda IRQ_matrix neigh regulator thermal  
xhci-hcd  
dev hda_controllerIRQ_vectors net resctrl thermal_  
power_allocator  
devfreq hda_intel jbd2 netlink rpm thp  
devlink header_event kmem nmi rseq timer  
dma_fence header_page libataOOMrtc tlb  
drm huge_memory lock page_isolation sched udp 
```

比如syscalls表示系统调用相关的监测点，power目录与电源相关。进入syscalls目录，可以看到很多包含enter和exit的目录，enter表示进入某个系统调用，exit表示退出某个系统调用时的监测点。这个目录下的enable是总开关，控制syscalls目录下所有跟踪点的开启或者关闭。命令与显示结果如下。

```txt
/sys_kernel/tracing/events/syscalls# ls enable sys-enter_removexattr sys_exit_iopl filter sys-enter_rename sys_exit_ioprio_get sys-enter_accept sys-enter_renameat sys_exit_ioprio_set sys-enter_accept4 sys-enter_renameat2 sys_exit_io_setup 
```

还可以进入某个子目录，子目录也有 enable，单独控制当前跟踪点的开启或者关闭。filter 文件用于过滤进程。命令及显示结果如下。

```txt
/sys/kernel/tracing/events/syscalls/sys-enter_accept#ls enable filter format hist id inject trigger 
```

关于 TraceFS 文件系统的更多信息，可以访问 Linux 内核文档，里面详细讲解了每个文件的使用，读者可以获取更多的帮助。

# 3.4.3 跟踪器

跟踪器称为Tracer。在available_tracers文件中记录了当前可用的Tracer，笔者的计算机中有如下种类的Tracer，需要注意的是，current_tracer必须是available_tracers中支持的Tracer。

```shell
$ sudo cat /sys/kernel/tracing/available_tracers
hwlat blk mmiotrace function_graph wakeup_d1 wakeup_rt wakeup function nop 
```

关于上述Tracer的详细描述如表3-5所示。

表 3-5 Tracer 含义  

<table><tr><td>跟踪器</td><td>说明</td></tr><tr><td>hwlat</td><td>hardware latency 的缩写，硬件延迟相关跟踪器</td></tr><tr><td>blk</td><td>用于跟踪块设备（block device）的 I/O 操作，包括读写操作、请求队列等</td></tr><tr><td>mmiotrace</td><td>用于跟踪和分析内存映射 I/O（memory-mapped I/O）操作</td></tr><tr><td>function_graph</td><td>与 function 跟踪器类似，但会显示调用链（call graph）。这个跟踪器可以帮助了解函数调用的路径和耗时</td></tr><tr><td>* wakeup_dI * wakeup_rt * wakeup</td><td>用于跟踪进程的唤醒（wakeup）情况，可以查看进程何时被唤醒、唤醒的原因等</td></tr><tr><td>function</td><td>用于记录函数的调用和返回</td></tr><tr><td>nop</td><td>占位符（tracer），不会进行任何跟踪操作</td></tr></table>

这里需要注意的是，events 只有在“nop tracer”下才会起作用，同时多个 Tracer 不能共享，同一时候只有一个 Tracer 生效。

current_tracer: 用于设置或者显示当前使用的跟踪器列表，它的值可以是表3-5中的任何一个。系统启动缺省值为nop，可以通过echo写入跟踪器名称来设置current_tracer。

```txt
$ echo function > /sys/kernel/tracing/current_tracer 
```

# 3.4.4 跟踪选项

跟踪选项的设置位于trace_options文件（或options目录），用于控制在跟踪输出中打印什么内容或操作跟踪器。可以通过以下cat命令来查看支持的设置。

```txt
$ cat trace_options
print-parent
nosym-offset
nosym-addr
noverbose
noraw
nohex
nobin
noblock
trace_PRINTk
 annotate
nouseracktrace
nosym-userobj 
```

```txt
noprintk msg-only   
context-info   
nolatency-format   
record-cmd   
norecord-tgid   
overwrite   
nodisable_on_free   
irq-info   
markers   
noevent-fork   
nopause-on-trace   
hash-ptr   
function-trace   
nofunction-fork   
nodisplay-graph   
nostacktrace   
notest_nop_accept   
notest_nop_refuse 
```

如果要关闭某个选项，可以在该配置项前面加上前缀“no”，比如：

```txt
echo noprint-parent > trace_options 
```

同理，要启动某个选项，只需要去掉“no”的前缀即可。

```shell
echo print-parent > trace_options 
```

相关配置选项的说明如表3-6所示，可以根据自己的需求灵活配置。

表 3-6 跟踪设置选项  

<table><tr><td>设置选项</td><td>选项说明</td></tr><tr><td>print-parent</td><td>在函数跟踪过程中,显示调用(父)函数及被跟踪的函数</td></tr><tr><td>sym-offset</td><td>不仅显示函数名称,还显示函数中的偏移量。例如,不仅可以看到“ktime_get”,还可以看到“ktime_get+0xb/0x20”</td></tr><tr><td>sym-addr</td><td>显示函数地址。例如simple_strtoul</td></tr><tr><td>verbose</td><td>显示函数详情信息</td></tr><tr><td>raw</td><td>显示裸数据。如果用户应用知道裸数据的解析方法,可以在用户态解析,优于在内核解析</td></tr><tr><td>hex</td><td>使用十六进制格式显示数据</td></tr><tr><td>bin</td><td>使用二进制格式显示数据</td></tr><tr><td>block</td><td>设置后,在轮询时读取trace_pipe不会阻塞</td></tr><tr><td>trace_PRINTk</td><td>禁止trace_PRINTk写数据到环形缓冲区</td></tr><tr><td>annotate</td><td>设置后,将显示新的CPU缓冲区何时开始</td></tr><tr><td>userstacktrace</td><td>记录用户空间的堆栈回调</td></tr><tr><td>sym-userobj</td><td>当启用userstacktrace时,查找地址属于哪个对象,并打印相对地址。当地址随机化(ASLR)开启时尤其有用</td></tr><tr><td>printk-msg-only</td><td>当设置时,trace_printk()将只显示格式而不显示其参数</td></tr><tr><td>context-info</td><td>仅显示事件数据,隐藏comm、PID、时间戳、CPU和其他有用的数据</td></tr><tr><td>latency-format</td><td>更改跟踪输出。启用时,跟踪会显示有关延迟的其他信息</td></tr><tr><td>record-cmd</td><td>当启用任何事件或跟踪器时,会在sched_SWITCH跟踪点中启用钩子来使用映射的PID和任务名 Comm)填充 comm 缓存。但是这可能会导致一些开销,如果只关心PID而不是任务名称,则禁用此选项可以降低跟踪的影响</td></tr><tr><td>record-tgid</td><td>当启用任何事件或跟踪器时,会在sched_SWITCH跟踪点中启用钩子来填充映射到PID的线程组ID (TGID)缓存</td></tr><tr><td>overwrite</td><td>控制跟踪缓冲区已满时要发生的情况。如果启用,则最早的事件将被丢弃并覆盖;如果关闭,则最新的事件将被丢弃</td></tr><tr><td>disable_on_free</td><td>当 free_buffer 关闭时,跟踪会停止 (tracing_on 设置为0)</td></tr><tr><td>irq-info</td><td>显示中断和抢占数 (preempt count)、需要重新调度数据</td></tr><tr><td>markers</td><td>当设置时,trace Marker是可以写的(仅仅是 root 用户);当禁用时,尝试写入trace Marker会返回EINVAL错误</td></tr><tr><td>event-fork</td><td>当设置时,如果任务的 PID 列在 set_event.pid 中,那么在这些任务进行 fork 时,它们的子进程的 PID 也会被加入 set_event.pid 中。另外,当具有 set_event.pid 中的 PID 的任务退出时,它们的 PID 也会从该文件中删除</td></tr><tr><td>pause-on-trace</td><td>当设置时,打开读取跟踪文件将暂停写入环形缓冲区(就像 tracing_on 被设置为0一样)。这模拟了跟踪文件的原始行为。关闭文件后,跟踪将再次启用</td></tr><tr><td>hash-ptr</td><td>当设置时,事件 printk 格式中的“%p”将显示散列指针值而不是实际地址。如果要查找散列值与跟踪日志中实际值对应的情况,这将非常有用</td></tr><tr><td>function-trace</td><td>启用此选项(默认情况下是启用的),延迟跟踪器将启用函数跟踪。禁用该选项,延迟跟踪器不会跟踪函数。可以在执行延迟测试时降低跟踪器的开销</td></tr><tr><td>function-fork</td><td>当设置时,如果任务的 PID 列在 set_ftrace.pid 中,那么在它们执行 fork 时,子进程的 PID也会被加入 set_ftrace.pid 中。另外,当具有 set_ftrace.pid 中的 PID 的任务退出时,它们的 PID 也会从该文件中删除</td></tr><tr><td>display-graph</td><td>如果设置此选项,延迟跟踪器 (irqsoft、wakeup 等)会使用函数图跟踪而不是函数跟踪</td></tr><tr><td>stacktrace</td><td>启用此选项,在记录任何跟踪事件后会同时记录一个堆栈跟踪</td></tr></table>

除了trace_options文件，/sys/kernel/tracing/options目录中也保留了一份与上面配置一一对应的文件，用于设置相关的跟踪选项。命令及显示如下。

/sys/kernel/tracing/options# ls

annotaf funcgraph-duration hex stacktrace

bin funcgraph-irqs IRQ-info sym-addr

blk_cgname funcgraph-overhead latency-format sym-offset

blk_cgroup funcgraph-overrun markers sym-userobj

```shell
blk_classic funcgraph-proc overwrite test_nop_accept block funcgraph-tail pause-on-trace test_nop_refuse context-info func-no-repeats printk msg-only trace_PRINT disable_on_free func_stack_trace print-parent userstacktrace display-graph function-fork raw verbose event-fork function-trace record-cmd funcgraph-abstime graph-time record-tgid funcgraph-cpu hash-ptr sleep-time 
```

可以通过写入0或者1来开启或者关闭相应的跟踪选项。例如，以下命令可以关闭function-trace。

```txt
echo 0 > options/function-trace 
```

# 3.4.5 环形缓冲区

在 TraceFS 中，ring buffer 即一种环形缓冲区，用于在内核空间和用户空间之间传输跟踪事件。当一个跟踪事件被写入 ring buffer 中时，它被添加到缓冲区的尾部。默认情况下，如果缓冲区已满，则新的跟踪事件会覆盖最早的事件。因此，通过使用 ring buffer 可以实现对正在进行的系统活动进行真正的连续记录。

同时 ring buffer 的容量是可配置的，可以使用 buffer_size_kb 和 buffer_total_size_kb 来配置跟踪缓冲区大小。

- buffer_size_kb：用于设置单个CPU所使用的跟踪缓存区的大小。默认情况下，每个CPU的跟踪缓冲区大小相同，显示的数字是单个CPU缓冲区的大小，而不是所有缓冲区大小的总和。跟踪缓冲区是以页为单位分配的（Linux系统中，一个内存分页大小通常是4KB）。例如：

```txt
$ sudo cat /sys/kernel/tracing/buffer_size_kb
7 (expanded: 1408) 
```

- buffer_total_size_kb: 用于显示和设置跟踪缓冲区的总大小, 单位为 KB。buffer_total_size_kb 显示的是 CPU 跟踪缓冲区的总和。该值应该根据需要进行设置, 以确保缓冲区能够容纳所需的跟踪数据, 如果设置得过小, 有可能导致 ftrace 无法存储所有的跟踪数据, 从而丢失有用的信息; 如果设置得过大, 则会浪费系统资源。例如:

```txt
$ sudo cat /sys/kernel/tracing/buffer_total_size_kb
total_size_kb
224 (expanded: 45056) 
```

上一条命令中的buffer_size_kb是7个分页，即28KB，也就是一个CPU跟踪缓冲区为28KB，笔者机器中有8个CPU核心，总的大小为 $28 \times 8 = 224\mathrm{KB}$ 。

还可以在tracing/per_cpu/cpu0/buffer_size_kb目录中单独指定CPU跟踪缓冲区的大小。

关于 TraceFS 就介绍到这里，更多内容可以查阅 Linux 内核文档中 ftrace 部分的内容，里面详细介绍了 TraceFS 的各种细节。

# 3.5 Linux内核数据源

下面看一下Linux内核产生数据的一些框架与接口，这里统称为“Linux内核数据源”。

# 1. ftrace

ftrace是内建于Linux内核的跟踪框架，旨在帮助开发人员和系统设计者弄清楚内核内部正在发生的事情，可用于调试或分析延迟和发生在用户空间之外的性能问题。它主要由Steven Rostedt开发，在2008年10月9日发布的内核版本2.6.27中被合并到Linux内核主线。虽然它的原名“function tracer”源自能够记录与内核运行时执行的各种函数调用相关的信息，但实际上ftrace可跟踪范围更广的内核内部操作。

凭借各种跟踪器插件，ftrace还可以针对不同的静态跟踪点进行跟踪，如调度事件、中断、内存映射I/O、CPU电源状态，以及与文件系统和虚拟化相关的操作。此外，它可以动态跟踪内核函数调用，还可以计算Linux内核中各种函数的执行延迟，如中断或抢占而被禁用的时间。

# 2. kprobe/kretprobe

kprobe（kernel probe）是Linux内核中的一个动态调试和性能分析工具，它允许开发者在内核代码执行期间插入自定义的探测点。通过使用kprobe，可以在关键函数或代码路径上设置断点，并收集相关信息以进行调试、跟踪和性能优化。与之对应的kretprobe的功能也一样，只是kretprobe作用于函数的返回。

kprobe的主要用途如下。

- 动态调试：kprobe 提供了一种无须重新编译内核即可插入断点并观察系统行为的方法。可以选择任意一个内核函数，在其进入、退出或返回时设置相应的探测器，并将处理程序与之关联。这样就可以实时监视变量值、堆栈跟踪等信息，帮助定位问题并进行故障排除。  
- 性能分析：利用kprobe，开发者可以测量特定代码路径或函数的执行时间、频率等指标，并收集硬件事件（如缓存命中率）来评估系统性能瓶颈。这对优化算法、改进数据结构及消除不必要的资源消耗非常有帮助。  
- 事件追踪：借助 kprobe 技术，可以捕获和记录系统范围内发生的重要事件（如上下文切换、中断响应等）。这有助于分析系统行为、识别瓶颈和优化资源利用。

使用 kprobe 需要掌握一定的内核知识和编程技能。可以通过在代码中注册 kprobe 探测器，并指定相关处理程序来设置断点。通常使用 register_kprobe()函数进行注册，以及定义一个适当的处理程序来执行所需的操作或收集数据。

在eBPF流行之前，很多安全软件产品的底层原理就是使用register_kprobe()函数在内核中添加

一些敏感内核方法的探测器。在相应方法被执行时，命中断点，做相应的逻辑处理。

# 3.uprobe/uretprobe

uprobe与uretprobe可以理解为用户态版本的kprobe与kretprobe。它们的运行机制是一样的，都是基于软件断点的实现，只是前者作用于用户态地址，在用户态软件分析领域，uprobe/uretprobe有着大量高效的实践。

# 4. 跟踪点

tracepoint称为跟踪点，是预埋在内核源码中的静态探测点，代码执行到静态探测点时会触发调用对应的插桩函数，从而达到观测内部函数运行的目的。这些静态观测点分布于内核的各个子系统中，用于Linux的各种跟踪工具。Linux内核2.6.32及以后的版本支持使用tracepoint。

# 5.perf事件

perf事件供perf_event_open系统调用来采集。后者可以编码关注硬件计数器、软件事件、性能统计、事件采样，或者直接开启动态追踪。由于它的功能比ftrace更加强大，效率更高，目前在实现数据采集时，eBPF的很多功能都优先使用perf_event_open系统调用，而不是通过TraceFS与相应的ftrace接口沟通。

# 6.eBPF

从严格意义上来说，eBPF本身并不能理解为数据源，因为它是依附其他数据源接口进行数据采集的。eBPF数据源将在3.6节详细讨论。

# 3.5.1 ftrace

前文介绍了 ftrace 的背景知识，以及 Linux 的 TraceFS。ftrace 是一个拥有很多功能的跟踪工具，也有非常多的使用方式，如下。

- 通过 TraceFS，使用 echo 类似的命令进行控制。  
- 通过前端工具如trace-cmd、KernelShark、perf-tools工具集等使用。  
通过高级语言编程进行控制。

本小节主要介绍ftrace的原理及ftrace hook是如何工作的，从而全面了解ftrace的内部机制和原理，以及如何编写ftrace hook的内核模块。

# 1. 动静态插桩技术

在介绍ftrace的原理前，我们先了解两个概念：静态插桩和动态插桩。ftrace是灵活运用了动静态插桩技术的跟踪框架。

- 静态插桩技术：即在内核代码中预先插入代码，随着内核代码一起编译。在内核的一些关键地方设置一些静态探测点，通过开关的方式启动或者关闭，像tracepoint（见3.5.4节）及USDT（见6.3.4节）都是静态插桩的方式。  
- 动态插桩技术：简单点说就是可以在程序运行时动态插入探针代码的技术。一般在内核或者应用函数的开始或者结束的位置进行插桩，在代码编译时在插桩处预留若干字节的代码，然后在使用时替换成特定的指令，比如跳转到执行探测操作的代码，探测完毕再返回原函数继续执行。后文介绍的uptrobe和kprobe都是这样的技术。

动态插桩技术有一个缺点，就是随着操作系统版本的迭代，被插桩的函数有可能被重命名或移除，这会导致动态插桩代码需要做大量的版本判断，尤其是操作系统升级的时候，可能会出现无法工作的情况。所以一般情况下，要开发自己的探测工具，优先尝试使用静态插桩技术，如果不能满足需求，再使用动态插桩技术来实现。

# 2. mcount机制

首先了解一下GCC的mcount机制，因为ftrace在后面使用了这种技术。mcount是GCC的一个特性，编译时在函数入口处插入call mcount指令，从而通过重载mcount函数来完成对任意函数的跟踪统计。

接下来，实现一个重载后的mcount函数。这个函数只是简单地输出“Hello World”字符串，并使用gcc-c命令将其编译成.o目标文件。

```txt
// gcc -c mcount.c
#include <stdio.h>
void mcount() {
    printf("Hello World\n");
} 
```

然后编写如下测试代码，在main函数中调用add方法，执行一次加法运算后返回结果。

```cpp
// gcc -c main.c -pg  
// gcc mcount.o main.o -o test  
// ./test  
#include <stdlib.h>  
#include <stdio.h>  
extern void mcount(void);  
int add(int a, int b) { printf("add:%d\n", a + b); return a + b; }  
int main() { 
```

```txt
int c = add(5, 6);  
printf("main:%d\n", c);  
return c; 
```

使用GCC编译，并添加-pg参数。链接后执行以下命令：

```powershell
$ gcc -c main.c -pg
$ gcc mcount.o main.o -o test
$ ./test
Hello World
Hello World
add:-937313679
main:-937313679 
```

可以看到mcount函数内部的输出，这里输出了一个负数。我们可能会有两点疑问：

- 发生了什么，mcount如何被调用了？  
- 为什么 printf 会输出负数？

使用IDA反汇编最终生成以下程序：

```asm
mcount proc near  
endbr64 ;  
push rbp  
mov rbp, rsp  
lea rax, s ; "Hello World"  
mov rdi, rax ; s  
call _puts ; 调用_puts  
nop ;  
pop rbp  
retn  
mcount endp  
; __int64__fastcall add(int, int)  
add proc near  
var_8 = dword ptr -8  
var_4 = dword ptr -4  
endbr64  
push rbp  
mov rbp, rsp  
sub rsp, 10h;  
db 67h  
call mcount;  
mov [rbp+var_4],EDI  
mov [rbp+var_8],esi  
mov edx,[rbp+var_4] 
```

```asm
mov eax, [rbp+var_8]  
add eax, edx;  
mov esi, eax  
lea rax, format; "add:%d\n"  
mov rdi, rax;  
mov eax, 0  
call _printf;  
mov edx, [rbp+var_4]  
mov eax, [rbp+var_8]  
add eax, edx  
leave;  
retn;  
add endp  
int __cdecl main(int argc, const char **argv, const char **envp)  
public main  
main proc near  
var_4 = dword ptr -4  
endbr64  
push rbp  
mov rbp, rsp  
sub rsp, 10h;  
db 67h  
call mcount;  
mov esi, 6  
movEDI, 5  
call add;  
mov [rbp+var_4], eax  
mov eax, [rbp+var_4]  
mov esi, eax  
lea rax, aMainD;  
mov rdi, rax;  
mov eax, 0  
call _printf;  
mov eax, [rbp+var_4]  
leave;  
retn; 
```

可以看到，main函数和add函数前面都加入了call mcount，这是因为使用gcc-pg编译程序，会在每个函数调用之前自动插入call mcount。又因为GCC默认使用了fastcall，即通过寄存器传递参数，main函数将参数压入esi和edi中，而mcount函数调用了printf函数，修改了esi和edi寄存器的值，从而add函数中参与计算的值被污染了，出现了输出负数的情况。

# 3. ftrace原理

ftrace 由两大部分组成：ftrace 框架（framework）和一系列 tracer（跟踪器）。ftrace 框架是整个 ftrace 跟踪系统的核心部分，它提供了一组 API 用于控制、管理跟踪事件，并提供了在内核中注册、注销和控制跟踪器的机制。tracer 是特定类型的内核跟踪器，每个 tracer 完成不同的功能，由 ftrace 框架统一管理。ftrace 中的跟踪信息保存在环形缓冲区中，ftrace 利用跟踪文件系统，提供了一系列控制文件，以供跟踪工具使用。ftrace 架构如图 3-4 所示。

![](images/b7cb91397eaae12309b04d2d8ed65206783a64dffdb454bf1a0d0f1594bcd4b3.jpg)  
图3-4 ftrace架构图

下面根据ftrace相关内核源码，从内核编译阶段、内核初始化阶段、ftrace启动后三个阶段描述ftrace原理。

# （1）内核编译阶段

GCC 4.6 新增加了对 -pg -mfentry 的支持，开启 ftrace 相关内核编译选项后，会在每个可跟踪的函数前插入 call fentry 指令，这个指令就相当于上面的 call mcount。在内核编译过程中，内核会通过 scripts/recordmcount.pl 脚本处理生成的.o 文件，在.o 文件中插入一个 __mcount_location 段，通过链接时的重定向，在这个段中插入所有 mcount 的函数地址。同时它在 recordmcount.pl 脚本中过滤了 ftrace.o，没有为其添加这个段，所以 ftrace 不会修改自身代码。

```txt
$ cd /usr/src/linux-source-5.19.0/linux-source-5.19.0/apple
$ objdump -r kexec.o
RELOCATION RECORDS FOR [.text]: 
```

```txt
0000000000000001 R_X86_64_PLT32 __fentry__-0x000000000000004  
0000000000000291 R_X86_64_PLT32 __fentry__-0x000000000000004  
0000000000000441 R_X86_64_PLT32 __fentry__-0x000000000000004  
000000000000551 R_X86_64_PLT32 __fentry__-0x000000000000004 
```

```txt
RELOCATION RECORDS FOR [__mcount_location]:  
OFFSET TYPE VALUE  
0000000000000000 R_X86_64_64 .text  
0000000000000008 R_X86_64_64 .text+0x000000000000290  
0000000000000010 R_X86_64_64 .text+0x000000000000440  
000000000000018 R_X86_64_64 .text+0x000000000000550  
... 
```

如果内核配置打开了CONFIG_FUNCTION tracer，那么在编译模块时也会增加-pg -mfentry，并将受到影响的函数地址保存在mcount_location段中。在开启CONFIG_DYNAMIC_FTRACE后，fentry会被替换成nop，以避免额外的性能开销。如果不开启CONFIG_DYNAMIC_FTRACE（如下代码），_fentry_会判断ftrace_trace_function是否为ftrace Stub（默认会设置成ftrace Stub)，如果不是则执行trace部分代码。ftrace_trace_function后面也会赋值ftrace ops_listFunc，调用注册好的跟踪器。

```asm
ifdef CONFIG_DYNAMIC_FTRACE
SYM FUNC_START(_fentry_)
retq
SYM FUNC_END(_fentry_)
EXPORT_SYMBOL(_fentry_)
#else /* ! CONFIG_DYNAMIC_FTRACE */
SYM FUNC_START(_fentry_)
cmpq $ftrace_stub, ftrace_trace_function
jnz trace
fgraph_trace:
...
SYM INNER LABEL(ftrace_stub, SYM_LGLOBAL)
retq
trace:
...
jmp fgraph_trace
SYM FUNC_END(_fentry_)
EXPORT_SYMBOL(_fentry_)
#endif /* CONFIG_DYNAMIC_FTRACE */ 
```

内核的链接脚本 include/asm-generic/vmlinux_lds.h 中 MCOUNT_REC 宏的 __mcount_location 段的内容放在 .init.data 段中，并且通过 __start_mcount_location 和 __stop_mcount_location 两个全局变量访问。这时分布在内核各个子系统的探针可以通过 __start_mcount_location 找到。

```c
//https://github.com/torvalds/linux/blob/v5.15/include/asm-generic/vmlinux_lds.h
#ifdef CONFIG_FTRACE_MCOUNT Recorder
#define MCOUNT_REC()
    . = ALIGN(8);
    __start_mcount_loc =.;
    KEEP(*(_mcount_loc))\ 
    KEEP(*(_patchable_function_entries))\ 
    __stop_mcount_loc = ;
    ftrace_stub_graph = ftrace_stub; 
```

# （2）内核初始化阶段

在内核初始化过程中，start_kernel调用ftrace_init，ftrace_init中的ftrace_process合资公司会将所有可跟踪函数中的callcount全部替换成nop指令，因为nop指令的开销低于call指令，这样对内核性能几乎没有影响。

//https://github.com/torvalds/linux/blob/v5.15/kernel/trace/ftrace.c  
void__initftrace_init(void){count $=$ _stop_mcount_location - start_mcount_location;if(!count){pr_info("ftrace:No functions to be traced?\n");goto failed;1//ftrace_processlocations $\rightharpoondown$ ftrace_update_code->ftrace_nop_initize->ftrace_  
init_nop将callmcount替换成nop指令ret $=$ ftrace_processlocations(NULL,_start_mcount_location,_stop_mcount_location);

//调用链路如下：

```c
// start_kernel->ftrace_init->ftrace_processlocate->ftrace_update_code->ftrace_nop initialize->ftrace_init_nop
```

# （3）ftrace启动后

当 ftrace 启动后，上面被替换的 nop 指令会被替换成 ftrace_caller()或者 ftraceRegs_caller()。例如，set_ftrace_filter 就会触发这个替换过程。它首先通过 trace_create_file 在 tracefs 中创建 set_ftrace_filter 文件，其中对应的文件操作指针是 ftrace_filter_fops，这是一个 Linux 字符设备驱动，例如. write =

ftrace_filter_write 定义了向 set_ftrace_filter 文件写入数据的回调函数，.release = ftrace_regex_release 定义了释放文件资源的回调函数。

static const struct file_operations ftrace_filter_fops = { .open $=$ ftrace_filter_open, .read $=$ seq_read, .write $=$ ftrace_filter_write, .llseek $=$ tracing_lseek, .release $=$ ftrace regex_release,   
}；   
void ftrace_create_filter_files(struct ftraceOps \*ops, struct dentry \*parent)   
{ trace_create_file("set_ftrace_filter",0644，parent, ops,&ftrace_filter_fops); trace_create_file("set_ftrace.notrace",0644，parent, ops,&ftrace.notrace_fops);   
}

ftrace_regex_release 会执行 ftrace_caller()的替换过程，其调用链路如下：

```txt
ftrace_regex_release  
-> ftrace_hash_move_and_updateOps  
-> ftrace ops_update_code  
-> ftrace_run_modify_code(FTRACE_UPDATE_CALLS)  
-> ftrace_run_update_code  
-> arch_ftrace_update_code  
-> ftrace_modify_all_code(FTRACE_UPDATE_CALLS)  
-> ftrace_replace_code(mod_flags | FTRACE_MODIFY_ENABLE_FL)  
-> __ftrace_replace_code(dyn_ftrace, true)  
-> ftrace.make_call  
-> ftrace_modify_code_direct 
```

相关源码路径如下：

```txt
https://github.com/torvalds/linux/blob/v5.15/kernel/trace/ftrace.c  
https://github.com/torvalds/linux/blob/v5.15/arch/x86/kernel/ftrace.c 
```

被替换的 ftrace caller 定义在 ftrace_64.S 中，使用 SYM FUNC_START 可以将函数映射到内核的符号表中，使用 SYM INNER LABEL 创建一个内部符号，以便在其他代码中引用。ftrace caller 的 ftrace_call 中调用了 ftrace_stub。

```txt
// https://github.com/torvalds/linux/blob/v5.15/arch/x86/kernel/ftrace_64.S 
```

```asm
// ftrace caller   
SYM FUNC_START(ftrace_caller)   
... SYM INNER LABEL(ftrace_caller_op_ptr, SYM_LGLOBAL)   
... SYM INNER LABEL(ftrace_call, SYM_LGLOBAL) call ftrace_stub   
... SYM INNER LABEL(ftrace_caller_end, SYM_LGLOBAL)   
// ftraceRegs_caller   
SYM FUNC_START(ftraceRegs_caller)   
... SYM INNER LABEL(ftraceRegs.caller_op_ptr, SYM_LGLOBAL)   
... SYM INNER LABEL(ftraceRegs_call, SYM_LGLOBAL) call ftrace_stub   
... SYM INNER LABEL(ftraceRegs.caller_jmp, SYM_LGLOBAL)   
... SYM INNER LABEL(ftraceRegs.caller_end, SYM_LGLOBAL)   
... SYM FUNC_END(ftraceRegs_caller)   
// ftrace stub   
SYM INNER LABEL ALIGN(ftrace_stub, SYM_L_WEAK) UNWIND_HINT FUNC retq 
```

“echo function $\rightarrow$ current_tracer” 的执行过程与上面一样，current_tracer 对应的文件操作指针是 set_tracer_fops。

//https://github.com/torvalds/linux/blob/v5.15/kernel/trace/trace.c   
static const struct file_operations set_tracer_fops $=$ { .open $=$ tracing_open_generic, .read $=$ tracing_set_trace_read, .write $=$ tracing_set_trace_write, .llseek $=$ generic_file_11seek,   
}； trace_create_file("current_tracer",0644,d_tracer, tr，&set_tracer_fops);

在执行“echo function $\rightarrow$ current_tracer”时，调用了tracing_set_trace_write，触发function_trace_init进

行初始化，调用register_ftrace_function完成向系统注册新的函数追踪器。register_ftrace_function的一个关键入参是ftrace ops，这是一个结构体，里面保存了需要注册的新的函数跟踪器地址，register_ftrace_function会将ftrace ops存储到ftrace ops_list的全局链表中，同时调用ftrace_run_update_code判断函数入口是否需要更新，从而将ftrace_call替换为ftrace ops_list_func。

```c
//写入current_tracer时回调到tracing_set_trace_write  
tracing_set_trace_write  
->tracing_set_tracer  
->tracer_init  
->t->init//根据不同的tracer调用对应的init，比如function_trace_init 
```

# //初始化

```erlang
function_trace_init(kernel/trace/trace-functions.c) ->tracing_start_function_trace ->register_ftrace_function ->register_ftrace_function 
```

# //导出函数，用于向系统注册新的函数追踪器

```txt
register_ftrace_function  
-> ftrace/startup  
-> __register_ftrace_function  
-> update_ftrace_function  
-> func = ftrace ops_list_func;  
ftrace_trace_function = func; 
```

```c
//判断函数入口是否需要更新，将ftrace_call替换为ftrace ops_list_func  
register_ftrace_function  
->ftrace/startup  
->ftrace/startup_enable  
->ftrace_run_update_code  
->arch_ftrace_update_code  
->ftrace_modify_all_code  
->ftrace_update_ftrace_func(ftrace OPS_list_func)  
//https://github.com/torvalds/linux/blob/v5.15/arch/x86/kernel/ftrace.c  
int ftrace_update_ftrace_func(ftrace_func_t func){  
    unsigned long ip;  
    const char *new;  
    ip = (unsigned long) (&ftrace_call);  
    new = ftrace_call_replace(ip, (unsigned long) func);  
    text_poke_bp((void *)ip, new, MCOUNT_INSN_SIZE, NULL);  
    ip = (unsigned long) (&ftraceRegs_call);  
    new = ftrace_call_replace(ip, (unsigned long) func); 
```

```c
text_poke_bp((void *)ip, new, MCOUNT_INSN_SIZE, NULL); return 0; 
```

最终 ftrace ops_list_func 会调用 __ftrace ops_list_func，后者会遍历全局 ftrace ops 链表 ftrace ops_list，执行注册的函数追踪器，完成相应的跟踪功能。

```c
static void ftraceOps_list_func(unsigned long ip, unsigned long parent_ip, struct ftraceOps *op, struct ftraceRegs *fregs) { _ftraceOps_list_func(ip, parent_ip, NULL, fregs); }   
static nokprobeINLINE void _ftraceOps_list_func(unsigned long ip, unsigned long parent_ip, struct ftraceOps *ignored, struct ftraceRegs *fregs) { ... do_for_each_ftrace_op(op, ftraceOps_list) { /\* Stub函数不需要被调用或者测试 \*/ if (op->flags & FTRACE OPS_FL_STUB) continue; //当前函数是否符合这个ftrace ops，符合则执行op->func if (! (op->flags & FTRACE OPS_FL_RCU) || rcu_is_watching()) && ftraceOps_test(op, ip, regs)) { if (FTRACE_WARNING_ON(!op->func)) { pr.warn("op=%p %pS\n", op, op); goto out; } op->func(ip, parent_ip, op, fregs); } while_for_each_ftrace_op(op);   
out: preempt_enable_notrace(); trace_clear_recursion(bit); 
```

# 4. 基于 ftrace hook 实现的内核模块

知道了ftrace的基本流程和原理后，现在学习如何编写ftrace hook的内核模块。首先从简单的开始。内核源码中就有一个简单的案例samples/ftrace/ftrace-direct

```c
//https://github.com/torvalds/linux/blob/v5.15/samples/ftrace/ftrace-direct.c  
//SPDX-License-Identifier:GPL-2.0-only  
#include <linux/module.h>  
#include <linux/sched.h> /*for wake_up_process()*/ 
```

```c
include <linux/ftrace.h>   
void my_direct_func(struct task_struct \*p){ trace_printk("waking up %s-%d\n", p->comm, p->pid);   
}   
extern void my_tramp(void \*);   
asm ( .pushsection .text, \\"ax\\", @progbits\n" .type my_tramp, @function\n" .globl my_tramp\n" my_tramp:" pushq %rbp\n" movq %rsp, %rbp\n" pushq %rdi\n" call my_direct_func\n" popq %rdi\n" leave\n" ret\n" size my_tramp, -my_tramp\n" .popsection\n");   
static int __init ftrace_direct_init(void) { return register_ftrace_direct((unsigned long)wake_up_process, (unsigned long)my_tramp);   
}   
static void __exit ftrace_direct_exit(void) { unregister_ftrace_direct((unsigned long)wake_up_process, (unsigned long)my_tramp);   
}   
module_init(ftrace_direct_init);   
module_exit(ftrace_direct_exit);   
MODULE_AUTHOR("Steven Rostedt");   
MODULE_DESCRIPTION("Example use case of using register_ftrace_direct());   
MODULE牌照("GPL"); 
```

可以看到，上面的代码非常简单。内核模块加载时调用register_ftrace_direct，这会将探针函数my_tramp注册到跟踪函数wake_up_process，用于唤醒处于睡眠状态的进程，使进程由睡眠状态变为运行状态，从而能够被CPU重新调度执行。my_tramp是一段汇编代码，里面会调用my_direct_func，完成相应的跟踪操作，这里调用的是trace_PRINTk。

可以通过“make M=[模块相对路径]”的方式，单独编译 ftrace 的样例模块。

```shell
$ cd /usr/src/linux-source-5.19.0/linux-source-5.19.0
$ make M=samples/ftrace clean
$ make M=samples/ftrace
...
LD [M] samples/ftrace/ftrace-direct.ko
BTF [M] samples/ftrace/ftrace-direct.ko 
```

有的可能会输出警告信息，跳过了BTF的生成，这是因为执行make时会读取当前目录的生成，如果读取到，就会将其BTF信息写入.ko文件中，若当前目录中没有这个文件，可以从系统lib目录复制过来，重新编译即可。注意，这里没有编译替换内核，默认vmlinux文件是不存在的，vmlinux是内核源码编译后生成的。要保持vmlinux的版本和当前系统一致（uname-r），否则内核模块将无法正常加载，并输出“failed to validate module [ftrace_direct] BTF：-22”。当然忽略这个警告也可以正常加载执行。

```txt
$ cp /lib/modules/5.19.0-38-generic/build/vmlinux 
```

然后重新清除再编译。

```txt
$ make M=samples/ftrace clean
$ make M=samples/ftrace 
```

加载、运行一下刚刚编译的内核模块。

```txt
$ sudo inmod samples/ftrace/ftrace-direct.ko 
```

使用 sudo dmesg 查看输出，可以看到内核模块成功地加载到内核中了。

别忘记开启trace，开启后可以看到模块的输出。

```txt
$ sudo bash -c 'echo 1 > /sys/kernel debug/tracing/tracing_on'
$ sudo cat /sys/kernel debug/tracing/trace_pipe
...
sudo-9433 [005] d..1. 2887.611570: my_direct_func: waking up kworker/
u256:2-5270
kworker/u256:2-5270 [000] d..1. 2887.611578: my_direct_func: waking up kworker/
u256:0-9404
kworker/u256:2-5270 [000] d..1. 2887.611588: my_direct_func: waking up kworker/
u256:0-9404
kworker/u256:2-5270 [000] d..1. 2887.611598: my_direct_func: waking up sudo-9433
cat-9435 [004] d..1. 2887.611614: my_direct_func: waking up kworker/
u256:2-5270
sudo-9433 [005] d..1. 2887.611624: my_direct_func: waking up kworker/
u256:2-5270 
```

```txt
kworker/u256:0-9404 [007] d..1. 2887.611632: my_direct_func: waking up sudo-9433 
```

停止监测后，关闭trace，并使用rmmod卸载对应的内核模块。

```shell
sudo bash -c 'echo 0 > /sys/kernel/debug/tracing/tracing_on' sudo rmmod samples/ftrace/ftrace-direct.ko 
```

当然上述案例代码非常简单，实际应用的代码会稍微复杂。下面看一个完整的案例。

```txt
//ftrace hook.c   
//https://github.com/ilammy/ftrace-hook #define pr fmt(fmt) "ftrace hook:" fmt 
```

```c
include <linux/ftrace.h> #include <linux/kallsyms.h> #include <linux/kernel.h> #include <linux/linkage.h> #include <linux/module.h> #include <linux/slab.h> #include <linux/uaccess.h> #include <linux/version.h> #include <linux/kprobes.h> 
```

```txt
MODULE_DESCRIPTION("Example module hooking clone() and execve() via ftrace");  
MODULE_AUTHOR("ilammy");  
MODULE_LICENSE("GPL");
```

```c
if LINUX_VERSION_CODE >= KERNEL_VERSION(5,7,0)  
static unsigned long lookup_name(const char *name) {  
    struct kprobe kp = {  
        .symbol_name = name;  
    };  
    unsigned long retval;  
    if (register_kprobe(&kp) < 0) return 0;  
    retval = (unsigned long) kp(addr;  
    unregister_kprobe(&kp);  
    return retval;  
}  
#else  
static unsigned long lookup_name(const char *name) {  
    return kallsyms.lookup_name(name);  
}  
#endif 
```

if LINUX_VERSION_CODE < KERNEL_VERSION(5,11,0) #define -FTRACE_OPS_FL_RECURSION FTRACE_OPS_FL_RECURSIONSAFE #endif #if LINUX_VERSION_CODE < KERNEL_VERSION(5,11,0) #define ftraceRegs ptRegs static __always_inline struct ptRegs *ftrace_getRegs(struct ftraceRegs *fregs) { return fregs; } #endif /** \*以下两种方式可以防止钩子发生恶意递归循环 \* $\bullet$ 使用函数返回地址检测递归循环USE_FENTRY_OFFSET $= 0$ \* $\bullet$ 通过跳过ftrace调用来避免递归循环USE_FENTRY_OFFSET $= 1$ /\* #define USE_FENTRY_OFFSET 0 //使用者只需要设置name、function、original等字段 struct ftrace hook{ const char \*name; //需要挂钩的函数名称 void \*function; //执行替换的函数指针 void \*original; //保持指向原函数指针的位置 unsigned long address; //函数入口地址 struct ftrace ops ops; //此函数钩子 }; static int fh Resolve hook_address(struct ftrace hook \*hook){ hook->address $=$ lookup_name-hook->name); if(!hook->address){ prDebug("unresolved symbol:%s\n",hook->name); return -ENOENT; } #if USE_FENTRY_OFFSET \*(unsigned long\*) hook->original) $=$ hook->address + MCOUNT_INSN_SIZE; #else \*(unsigned long\*) hook->original) $=$ hook->address; #endif return 0;

static void notrace fh_ftrace_thunk(unsigned long ip, unsigned long parent_ip, struct ftraceOps *ops, struct ftraceRegs *fregs)   
{ struct ptRegs \*regs $=$ ftrace_getRegs(fregs); struct ftrace hook \*hook $=$ container_of(ops, struct ftrace-hook, ops); #if USE_FENTRY_OFFSET regs->ip $=$ (unsigned long)hook->function; #else if (!within_module(parent_ip, THIS_MODULE)) regs->ip $=$ (unsigned long)hook->function; #endif } /\*\* \*安装单个Hook，成功返回0，失败返回负值 \*/ int fh_install hook(struct ftrace hook \*hook){ int err; err $=$ fh Resolve hook address (hook); if (err) return err; //因为修改rip寄存器的值，所以需要设置FTRACE_OPS_FL_IPMODIFY和FTRACE_OPS_FL_SAVE_REGS //修改将导致anti-recursion保护失效，因此需要使用FTRACE_OPS_FL_RECURSION hook->ops FUNC $=$ fh_ftrace_thunk; hook->ops.flags $=$ FTRACE_OPS_FL_SAVE_REGS | FTRACE_OPS_FL_RECURSION | FTRACE_OPS_FL_IPMODIFY; err $=$ ftrace_set_filter_ip(&hook->ops, hook->address, 0, 0); if(err）{ prDebug("ftrace_set_filter_ip() failed:%d\n",err); return err; } err $=$ register_ftrace_function(&hook->ops); if(err）{ prDebug("register_ftrace_function() failed:%d\n",err); ftrace_set_filter_ip(&hook->ops, hook->address, 1, 0); return err; }

return 0;   
1   
//注销Hook   
void fh_remove hook(struct ftrace hook \*hook){ int err; err $=$ unregister_ftrace_function(&hook->ops); if(err）{ prDebug("unregister_ftrace_function()failed:%d\n",err); } err $=$ ftrace_set_filter_ip(&hook->ops, hook->address,1,0); if(err）{ prDebug("ftrace_set_filter_ip()failed:%d\n",err);   
}   
\*/   
\*注册和启用所有的Hook，成功返回0，失败返回负数 \*/   
intfh_installHooks(struct ftrace-hook \*hooks,size_t count){ int err; size_t i; for $(i = 0;i <   \text{count};i + + )$ { err $=$ fh_install hook(&hooks[i]); if(err) goto error; } return 0;   
error: while $(i! = 0)$ { fh_remove hook(&hooks[-i]); } return err;

```c
// 卸载所有的Hook  
void fh_removeHooks(struct ftrace hook *hooks, size_t count) {  
    size_t i;  
    for (i = 0; i < count; i++) 
```

```c
fh_remove hook(&hooks[i]);   
}   
#	define CONFIG_X86_64   
error Currently only x86_64 architecture is supported   
endif   
#defined (CONFIG_X86_64) && (LINUX_VERSION_CODE >= KERNEL_VERSION(4,17,0)) #define PTREGS_SYSCALL_STUBS 1   
endif   
//尾递归优化可能会干扰基于堆栈返回地址的递归检测。为避免机器死机，应禁用尾递归优化 #if!USE_FENTRY_OFFSET   
#pragma GCC optimize(-fno-optimize-sibling-calls")   
endif   
#ifdef PTREGS_SYSCALL_STUBS   
static asmlinkage long (*real_sys_clone)(struct ptRegs *regs);   
static asmlinkage long fh_sys_clone(struct ptRegs *regs){ long ret; pr_info("clone() before\n"); ret = real_sys_clone(regs); pr_info("clone() after:%ld\n",ret); return ret;   
}   
#else   
//这是指向原系统调用处理程序execve指针 static asmlinkage long (*real_sys_clone)(unsigned long clone_flags, unsigned long newsp,int __user *parent_tidptr, int __user *child_tidptr, unsigned long TLS);   
static asmlinkage long fh_sys_clone(unsigned long clone_flags, unsigned long newsp,int __user *parent_tidptr, int __user *child_tidptr, unsigned long TLS) { long ret; pr_info("clone() before\n"); ret = real_sys_clone(clone_flags,newsp,parent_tidptr, child_tidptr,tls); 
```

pr_info("clone() after: %ld\n", ret); return ret;   
}   
#endif   
static char \*DuplicateFilename(const char _user \*filename) { char \*kernelFilename; kernelFilename $=$ kmalloc(4096,GFP_KERNEL); if(!kernelFilename) return NULL; if(strncpy_from_user(kernelFilename, filename, 4096) < 0) { kfree(kernelFilename); return NULL; } return kernelFilename;   
}   
#ifndef PTREGS_SYSCALL_STUBS   
static asmlinkage long (\*real_sys_execute) (struct ptRegs \*regs);   
static asmlinkage long fh_sys_execute(struct ptRegs \*regs) { long ret; char \*kernelFilename; kernelFilename $=$ duplicateFilename((void*) regs->di); pr_info("execute() before: %s\n", kernelFilename); kfree(kernelFilename); ret $=$ real_sys_execute(regs); pr_info("execute() after: %ld\n", ret); return ret;   
}   
#else   
static asmlinkage long (\*real_sys_execute) (const char __user \*filename, const char __user \*const __user \*argv, const char __user \*const __user \*envp);

// 这个就是挂钩上去的函数，这个函数可以在原始函数之前、之后或代替原始函数执行的任意代码

static asmlinkage long fh_sys_execute(const char __user *filename, const char __user *const __user *argv, const char __user *const __user *envp) { long ret; char *kernel Filename; kernelFilename $=$ duplicateFilename(filename); pr_info("execve() before: $\% \mathrm{s}\backslash \mathrm{n}^{\prime \prime}$ , kernelFilename); kfree(kernelFilename); ret $=$ real_sys_execute(filename, argv,envp); pr_info("execve() after: $\% \mathrm{ld}\backslash \mathrm{n}^{\prime \prime}$ ,ret); return ret;   
1 #endif   
//x86_x64架构的系统调用有特殊的命名约定，如果要移植到其他处理器架构，需要重新修改此处代码 #ifdef PTREGS_SYSCALL_STUBS #define SYSCALL_NAME(name）（"x64" name) #else #define SYSCALL_NAME(name） (name) #endif   
#define HOOK(_name,_function,_original) { .name $=$ SYSCALL_NAME(_name), .function $=$ (_function), .original $=$ (_original),   
} static struct ftrace hook demo hooks[] $=$ { HOOK("sysclone",fh_sysClone,&real_sysClone), HOOK("sys_execute",fh_sys_execute,&real_sys_execute),   
}; static int fh_init(void){ int err; err $=$ fh_installHooksdemoHooks,ARRAY_SIZEdemoHooks); if(err) return err;

```c
pr_info("module loaded\n"); return 0;   
}   
module_init(fh_init);   
static void fh_exit(void) { fh_removeHooksdemoHooks,ARRAY_SIZEdemoHooks); pr_info("module unloaded\n");   
}   
module_exit(fh_exit); 
```

下面解读一下上面的代码。加载内核模块后，首先程序定义 ftrace hook 结构体来描述 Hook 函数，只需要填写 name、function 和 original 字段，将需要挂钩（hook）的函数填写好并保存到 ftrace hook 的结构体数组（demo hooks）中，然后调用 fh_install hook 依次注册 Hook 函数，这个注册分为如下步骤。

1）调用fh Resolve hook address 找到需要挂钩的函数的地址，这里做了一个兼容性版本的 lookup_name 来完成这个工作，5.7 以前的内核版本使用 kallsyms.lookup_name，以后的则使用 register_kprobe 将函数符号转换成内存地址。  
2）设置ftrace hook结构体中的ftrace ops。ftrace ops用于告诉ftrace应调用哪个函数作为回调，以及回调将执行哪些保护操作而不需要ftrace处理。

因为修改了 rip 寄存器的值，所以需要设置 FTRACE_OPS_FL_IPMODIFY 和 FTRACE_OPS_FL_SAVERegs。但是修改将导致 anti-recursion 保护失效，因此需要使用 FTRACE_OPS_FL_RECURSION。

hook->ops funct $=$ fh_ftrace_thunk;   
hook->ops.flags $=$ FTRACE_OPS_FL_save_REGS |FTRACE_OPS_FL_RECURSION |FTRACE_OPS_FL_IPMODIFY;register_ftrace_function

接着看fh_ftrace_thunk跳板函数，这里使用container_of来获取ftrace hook的地址，container_of可以根据结构体成员变量的地址获取这个结构体的地址。修改IP指令指针寄存器的值为hook->function，这个寄存器里存放下一条要运行的CPU指令。若程序没有使用USE_FENTRY_OFFSET，当探测器函数fh_sys_execute调用原始函数时，原始函数将被ftrace再次跟踪，从而导致无穷无尽的递归。这里有一种巧妙的设计，首次调用时parent_ip指向的是内核中的某个地址，第2次调用时则指向探测器函数fh_sys_execute内部，可通过within_module判断parent_ip是否在模块中，以防止递归调用。

```c
static void notrace fh_ftrace_thunk(unsigned long ip, unsigned long parent_ip, struct ftraceOps *ops, struct ftraceRegs *fregs) { struct ptRegs *regs = ftrace_getRegs(fregs); 
```

struct ftrace hook \*hook $=$ container_of(ops, struct ftrace hook, ops);   
#if USE_FENTRY_OFFSET regs->ip $=$ (unsigned long)hook->function;   
#else if (!within_module(parent_ip, THIS_MODULE)) regs->ip $=$ (unsigned long)hook->function;   
#endif   
}

最后调用了 ftrace_set_filter_ip 为所需的函数打开 ftrace 实用程序。接着调用 register_ftrace_function, 用来注册回调, 以及替换 ftrace_call 为 ftrace ops_list_func。当函数执行到 sys_execute（挂钩的函数）时, ftrace ops_list_func 会调用注册后的回调函数。

编译加载，可以看到成功执行了。

```txt
$ make
...
$ sudo insmod ftrace hook.ko
$ sudo dmesg --follow
[183845.515993] ftrace hook: module loaded
[183845.517312] ftrace hook: clone() before
[183845.517480] ftrace hook: clone() after: 489036
[183845.563187] ftrace hook: execve() before: /bin/sh
[183845.563331] ftrace hook: execve() after: 0
[183845.564080] ftrace hook: clone() before
[183845.564144] ftrace hook: clone() after: 489038
[183845.564185] ftrace hook: clone() before
[183845.564244] ftrace hook: clone() after: 489039
...
$ sudo rmmod ftrace hook.ko 
```

# 3.5.2 kprobe/kretprobe

kprobe是Linux内核动态跟踪工具，它可以通过在内核代码中插入探针，达到动态跟踪内核操作的目的。2004年，kprobe正式加入Linux内核2.6.9版本中。kprobe可以对任意内核函数进行插桩，不仅如此，它还可以对函数内部的指令进行插桩，并且可以实时地在系统中启用。

kprobe 的工作原理与调试器相似，以 cpus_write_lock 为例，我们跟踪“mov rbp,rsp”指令的调用情况，如图 3-5 所示。

1）将插桩的目标地址和地址中的机器码指令保存到kprobe结构体中，这里是将“mov rbp,rsp”的内存地址和机器码指令（4889E5）保存到kprobe结构体中并注册。  
2）以单步中断指令覆盖目标地址，在x86架构上是“int3”断点指令。  
3）当代码执行到“int 3”时会触发中断，断点处理程序会判断这个断点地址是否由kprobe注

册，如果是则跳到注册的处理函数并执行。

![](images/4d5a98d41bd9ef9946cf8efd9061dd48c2e7561c3d49e8f5d2f90f84a05986f4.jpg)  
图3-5 kprobe的工作原理

4）当处理函数执行完毕，恢复原来的指令，同时设置“单步”（single-step），将rip寄存器重新指向原来的地址，执行之前的指令，也就是“mov rbp,rsp”。  
5）执行完毕后，由于设置了单步调试，所以会再次陷入异常，进入断点处理程序，将跟踪地址的指令再次替换成“int3”，继续执行。  
6）当不再需要kprobe跟踪时，原始的指令会重新写至目标地址。

早期使用 kprobe 时需要编写内核模块，通常使用 C 语言编写入口函数，再通过 register_kprobe 函数注册，使用完毕再调用 unregister_kprobe 进行卸载。随着 Linux 内核的发展，现在可以利用 TraceFS 或者 eBPF，这里主要介绍在 TraceFS 下通过 kprobe_events 方式使用 kprobe。

kprobe命令参数如下，说明见表3-7。

表 3-7 kprobe 参数说明  

<table><tr><td colspan="2">参数</td><td>说明</td></tr><tr><td colspan="2">GRP</td><td>组名。如果省略,则使用 kprobes</td></tr><tr><td colspan="2">EVENT</td><td>事件名称。如果省略,事件名称将基于 SYM + offs 或 MEMADDR 生成</td></tr><tr><td colspan="2">MOD</td><td>给定 SYM 的模块名称</td></tr><tr><td colspan="2">SYM[+offs]</td><td>插入探针的符号+偏移量</td></tr><tr><td colspan="2">SYM%return</td><td>符号的返回地址</td></tr><tr><td colspan="2">MEMADDR</td><td>插入探针的地址</td></tr><tr><td colspan="2">MAXACTIVE</td><td>可以同时探测的指定函数的最大实例数,默认值为0</td></tr><tr><td rowspan="4">FETCHARGS</td><td>%REG</td><td>获取寄存器 REG</td></tr><tr><td>@ADDR</td><td>获取 ADDR 处的内存(ADDR 应在内核中)</td></tr><tr><td>@SYM[+\\-offs]</td><td>在 SYM 特定偏移处获取内存(SYM 应为数据符号)</td></tr><tr><td>$stackN</td><td>获取堆栈的第 N 个条目 (N≥0)</td></tr><tr><td rowspan="8">FETCHARS</td><td>$stack</td><td>获取堆栈地址</td></tr><tr><td>$largN</td><td>获取第N个函数参数(N≥1)。仅适用于函数入口探针(即offs=0)</td></tr><tr><td>$retval</td><td>获取返回值。仅适用于返回探针</td></tr><tr><td>$comm</td><td>获取当前任务 comm</td></tr><tr><td>+|-[u]OFFS(FETCHARG)</td><td>在 FETCHARG特定偏移地址处获取内存。这对于获取数据结构的字段很有用。“u”表示用户空间解引用</td></tr><tr><td>\IMM</td><td>将立即值存储到参数中</td></tr><tr><td>NAME = FETCHARG</td><td>将 FETCHARG 的参数名称设置为 NAME</td></tr><tr><td>FETCHARG: TYPE</td><td>将类型 TYPE 设置为 FETCHARG 的类型。当前支持基本类型(u8/u16/u32/u64/s8/s16/s32/s64)、十六进制类型(x8/x16/x32/x64)、char、string、ustring、symbol、symstr 和位域</td></tr></table>

- p[:[GRP/][EVENT]] [MOD:]SYM[+offs]MEMADDR [FETCHCHARGS]: 设置探针。   
- r[MAXACTIVE][: [GRP/][EVENT]] [MOD:]SYM[+0] [FETCHARGS]: 设置返回探针。   
- p[:[GRP/][EVENT]] [MOD:]SYM[+0]%eturn [FETCHARGS]:设置返回探针。   
- :[GRP]/[EVENT]: 清除探针。

available_filter-functions 保存了所有可以被 kprobe 探测的函数，可以通过 cat 命令查找需要被探测的内核函数。

```txt
cat /sys/kernel/tracing/available_filter-functions | grep openat  
__audit_openat2怎么做  
do_sys_openat2  
_x64_sys_openat2  
__ia32_sys_openat2  
_x64_sys_openat  
__ia32_compat_sys_openat  
__ia32_sys_openat  
path_openat  
__io_openat Prep  
io_openat2 Prep  
io_openat Prep  
io_openat2  
io_openat 
```

还可以通过如下命令执行：

```shell
$ sudo bpftrace -l "kprobe:*" | grep openat
kprobe:__audit_openat2怎么做
kprobe:__ia32_compat_sys_openat 
```

```txt
kprobe:__ia32_sys_openat  
kprobe:__ia32_sys_openat2  
kprobe:__io_openat Prep  
kprobe:__x64_sys_openat  
kprobe:__x64_sys_openat2  
kprobe:do_sys_openat2  
kprobe:io_openat  
kprobe:io_openat2  
kprobe:io_openat2 Prep  
kprobe:io_openat Prep  
kprobe:path_openat 
```

为探测器添加新事件，可以将其写入/sys/kernel/tracing/kprobe_events 文件。例如通过如下命令，可以在 do_sys_openat2 函数的顶部设置一个 kprobe，并将第 $1 \sim 4$ 个参数记录为 myprobe 事件。

```txt
$ sudo su
$ echo 'p:myprobe do_sys_openat2 dfd=%ax filename=%dx flags=%cx mode=+4 ($stack)' > /
sys/kernel/tracing/kprobe_events 
```

或者通过如下方式，在do_sys_openat2函数的返回点设置一个kretprobe，并将返回值记录为myretprobe事件，注意追加写入是“>>”。

```perl
echo 'r:myretprobe do_sys_openat2 $retval' >> /sys/kernel/tracing/kprobe_events 
```

可以看到，两个事件都写进去了。

```txt
# cat /sys/kernel/tracing/kprobe_events
p:kprobes/myprobe do_sys_openat2 dfd=%ax filename=%dx flags=%cx mode=+4 ($stack)
r64:kprobes/myretprobe do_sys_openat2 arg1=$reval 
```

此时events/kprobes目录下会生成myprobe和myretprobe目录。

```txt
cd /sys/kernel/tracing/events/kprobes /sys/kernel/tracing/events/kprobes# ls enable filter myprobe myretprobe 
```

若需要启用这些断点，可以向指定事件目录下的 enable 目录写入 1，写入 0 为停用。

```shell
echo 1 > /sys/kernel/tracing/events/kprobes/myprobe enable  
echo 1 > /sys/kernel/tracing/events/kprobes/myretprobe enable 
```

通过如下命令可以间隔的方式开始跟踪：

```txt
#打开traceing_on  
#echo 1 > /sys/kernel/tracing/tracing_on  
#等待一会儿... 
```

```txt
关闭traceing_on  
# echo 0 > /sys/kernel/tracing/tracing_on 
```

可以通过tracing/trace文件查看输出内容。

```txt
# cat /sys/kernel/tracing/trace
# tracer: nop
# entries-in-buffer/entries-written: 1932/1932 #P:4
#
#
#
#
#
#
#
#
#
#
#
TASK-PID CPU# TIMITAMP FUNCTION
| | | | | | | | | | ... 
lpstat-106264 [003] ..... 37797.849483: myprobe: (do_sys_openat2+0x0/0x180)
dfd=0x0 filename=0xfffffa66a4dcdbeb8 flags=0x88000 mode=0x88000FFFFFF
lpstat-106264 [003] ..... 37797.849488: myretprobe: (x64_sys_openat+0x55/0xa0 <- do_sys_openat2) arg1=0x9
lpstat-106264 [003] ..... 37797.849554: myprobe: (do_sys_openat2+0x0/0x180)
dfd=0x0 filename=0xfffffa66a4dcdbe18 flags=0x88000 mode=0x88000FFFFFF
lpstat-106264 [003] ..... 37797.849562: myretprobe: (x64_sys_openat+0x55/0xa0 <- do_sys_openat2) arg1=0x9
<...>-106265 [002] ..... 37797.849568: myprobe: (do_sys_openat2+0x0/0x180)
dfd=0x0 filename=0xfffffa66a4e00bec8 flags=0x88000 mode=0x88000FFFFFF 
```

当不使用 kprobe 时，可通过如下方式卸载 kprobe。

```txt
禁用当前注册的kprobe  
echo \(0 > /sys/kernel/tracing/events/kprobes/myprobe/enab1e\) echo \(0 > /sys/kernel/tracing/events/kprobes/myretprobe/enable 
```

```txt
清除某个断点  
echo --:myprobe >> /sys/kernel/tracing/kprobe_events
```

```txt
清除所有断点  
echo > /sys/kernel/tracing/kprobe_events
```

# 3.5.3uprobe/uretprobe

>uprobe提供了用户态程序的动态插桩技术，uprobe于2012年被合并到Linux内核3.5版本中。

transpose 的实现原理与 kprobe 类似，也是通过设置断点的方式来处理的，transpose 可以在函数入口、特定的偏移、函数返回处进行插桩。transpose 可以通过 TraceFS 的 transpose_event 和 perf_event_open 来使用，BPF 跟踪工具支持 perf_event_open 的方式（在 Linux 内核 4.17 以后的版本中支持）。下面通过 TraceFS 的方式来使用 transpose，以跟踪/bin/bash 调用 readline 为例。

1）找到readline的函数偏移。可以使用readelf解析/bin/bash来获取readline函数在/usr/bin/bash的ELF文件中的偏移。ELF是Linux操作系统的可执行文件格式，一般在Linux下可以通过readelf命令来解析ELF文件，通过-s选项可以显示所有符号表中的项，这样就可以得到readline的偏移量为 $0\mathrm{x}000000000000d5690$

```txt
readelf -s /bin/bash | grep headline | grep FUNC  
340: 00000000000d52f0 914 FUNC GLOBAL DEFAULT 16 headline_interna[...]  
841: 0000000000d42d0 608 FUNC GLOBAL DEFAULT 16 headline_interna[...]  
891: 0000000000097e40 221 FUNC GLOBAL DEFAULT 16 posix_readline_i[...]  
912: 0000000000d5690 201 FUNC GLOBAL DEFAULT 16 readline  
1284: 0000000000095630 29 FUNC GLOBAL DEFAULT 16 initialize_readline  
2101: 0000000000d4530 746 FUNC GLOBAL DEFAULT 16 readline_interna[...] 
```

2）将偏移地址、跟踪函数名称、程序路径等信息注册到uprobe_events中，同时跟踪ip和ax寄存器的值。

```txt
# 注册 readline 到uprobe_events
$ sudo bash -c 'echo p:readline /bin/bash:0x00000000000d5690 %ip %ax > /sys/kernel/tracing/uprobe_events' 
```

```txt
# 在/sys/kernel/tracing/events/uprobes下创建readline目录
$ sudo su
$ cd /sys/kernel/tracing/events/uprobes
$ ls
enable filter readline
```

```txt
# 查看注册事件
$ sudo cat /sys/kernel/tracing/uptrobe_events
p:uprobes/readline /bin/bash:0x00000000000d5690 arg1=%ip arg2=%ax 
```

其中uprobe使用的参数部分如下。

- p[:[GRP/][EVENT]] PATH:OFFSET [FETCHARGS]: 设置uprobe。  
- r[:[GRP]/[EVENT]] PATH:OFFSET [FETCHARS]: 设置 uretprobe。  
- p[:[GRP/][EVENT]] PATH:OFFSET%return [FETCHARGS]: 设置 uretprobe 的另一种方式。  
- :[GRP]/[EVENT]: 清除uptrobe和uretprobe。

其中 $p$ 表示 trace 函数， $r$ 表示 trace 函数的返回。参数的含义如表 3-8 所示。

表 3-8uprobe 参数含义  

<table><tr><td colspan="2">参数</td><td>说明</td></tr><tr><td colspan="2">GRP</td><td>组名。如果省略, uprob es 是默认值</td></tr><tr><td colspan="2">EVENT</td><td>事件名。如果省略,事件名将基于“路径+偏移”生成</td></tr><tr><td colspan="2">PATH</td><td>可执行文件或库的路径</td></tr><tr><td colspan="2">OFFSET</td><td>插入探针的偏移量</td></tr><tr><td colspan="2">OFFSET%return</td><td>插入返回探针的偏移量</td></tr><tr><td rowspan="11">FETCHARS</td><td>%REG</td><td>获取指定的寄存器值</td></tr><tr><td>@ADDR</td><td>获取指定的地址 ADDR 的内存(此处的 ADDR 应该在用户空间)</td></tr><tr><td>@+OFFSET</td><td>获取偏移地址 OFFSET 的内存(OFFSET 来自与 PATH 相同的文件)</td></tr><tr><td>$stackN</td><td>获取堆栈的第 N 个条目 (N≥0)</td></tr><tr><td>$stack</td><td>获取堆栈地址</td></tr><tr><td>$retval</td><td>获取返回值。仅适用于返回探针</td></tr><tr><td>$lcomm</td><td>获取当前任务 comm</td></tr><tr><td>+[-[u]OFFS(FETCHARG)</td><td>在 FETCHARG 特定偏移地址处获取内存。这对于获取数据结构的字 段很有用。“u”表示用户空间解引用。请参阅“ref: user_mem_access”</td></tr><tr><td>\IMM</td><td>将立即值存储到参数中</td></tr><tr><td>NAME=FETCHARG</td><td>将 NAME 设置为 FETCHARG 的参数名称</td></tr><tr><td>FETCHARG:TYPE</td><td>将 FETCHARG 的类型设置为 TYPE。目前支持基本类型(u8/u16/ u32/u64/s8/s16/s32/s64)、十六进制类型(x8/x16/x32/x64)、string 和 bitfield</td></tr></table>

注意：在 FETCHARS 参数部分，每个探针最多可以有 128 个参数。

3）在写入事件后，启用这个探测点。

```shell
$ sudo bash -c 'echo 1 > /sys/kernel/tracing/events/uprobes/readline/disable' 
```

4）启动tracing_on。

```shell
$ sudo bash -c 'echo 1 > /sys/kernel/tracing/tracing_on' 
```

通过trace_pipe查看输出。

```txt
$ sudo cat /sys/kernel/tracing/trace_pipe
bash-20609 [000] DNZff 6592.313078: readline: (0x557867405690) arg1=0x557867405690
arg2=0x5578674571bd
bash-20609 [001] DNZff 6643.052472: readline: (0x557867405690) arg1=0x557867405690
arg2=0x5578674571bd
bash-20609 [002] DNZff 6665.065396: readline: (0x557867405690) arg1=0x557867405690
arg2=0x5578674571bd
bash-20609 [003] DNZff 6722.082264: readline: (0x557867405690) arg1=0x557867405690 
```

```txt
arg2=0x5578674571bd 
```

5）在结束跟踪后，可以按照如下方式注销uprobe跟踪。

```shell
// 关闭trace_on
$ sudo bash -c 'echo 0 > /sys/kernel/tracing/tracing_on' 
```

```shell
// 关闭 readline/enable
$ sudo bash -c 'echo 0 > /sys/kernel/tracing/events/uprobes/readline/enable' 
```

```txt
//取消注册 
```

```shell
$ sudo bash -c 'echo --:readline /bin/bash:0x000000000000d5690 >> /sys/kernel/tracing/uptrobe_events' 
```

# 3.5.4 tracepoint

从Linux的历史来看，人们一直希望向内核中添加静态跟踪点，从而在内核中的特定位置记录数据，以便日后进行检索，其最早实现的版本是2008年内核2.6版本提交的一个补丁，由Linux内核维护人员Mathieu Desnoyers提供。低性能开销的跟踪钩子称为Trace Markers。Trace Markers记录的信息以printf格式嵌入内核中，尽管它们通过宏巧妙地解决了性能问题，但这使得一些内核开发人员感到不满，因为这使内核代码看起来像是散布了调试代码。后来Mathieu Desnoyers提出了“跟踪点”（tracepoint）的概念，跟踪点是预埋在内核源码中的静态探测点，这些探测点提供了一个“钩子”（hook），可以在运行时调用我们提供的函数，这个函数称为探针（probe）或者桩函数，而跟踪点可以处于开启或关闭状态。当跟踪点关闭时，对系统性能几乎没有影响；当跟踪点处于打开的状态时，每次执行到跟踪点都会调用我们提供的函数，并在调用者的执行上下文中运行。执行完毕，探针函数将返回调用处并继续执行。可以在重要的代码地方放置跟踪点，以便进行跟踪和探测。

接下来看看如何使用tracepoint。首先在需要引入跟踪点的子系统的某个模块的头文件中引入tracepoint.h，这里一个关键的宏是DECLARE_TRACE，比如在include/trace/events/subsys.h中引入，具体代码如下：

```c
#undef TRACE_SYSTEM
#define TRACE_SYSTEM subsys
#if !defined(_TRACE_SUBSYS_H) || defined(TRACEHEADERMulti_READ)
#define _TRACE_SUBSYS_H
#include <linux/tracepoint.h>
DECLARE_TRACE(subsys_eventname, TP-ProTO(int firstarg, struct task_struct *p), TP_args(firstarg, p)); 
```

```c
endif /\* _TRACE_SUBSYS_H\*/ /\* This part must be outside protection \*/ #include <trace/define_trace.h> 
```

然后在对应的C文件中添加如下跟踪语句：

```c
include <trace/events/subsys.h>   
#define CREATE_TRACE_POINTSS   
DEFINE_TRACE(subsys_eventname);   
void somefct(void) { ...   
// 跟踪点，需要收集信息的位置，trace_subsys_eventname会调用callback函数 trace_subsys_eventname(arg，task);   
1   
// 实现自己的钩子函数并注册到内核   
void callback(..）{}   
register_trace_subsys_eventname(callback);
```

DECLARETRACE宏的参数说明如表3-9所示。

表 3-9DECLARE_TRACE宏的参数说明  

<table><tr><td>参数</td><td>说明</td></tr><tr><td>subsys_eventname</td><td>事件的唯一标识符。subsys 是需要跟踪的子系统的名称， eventname 是要跟踪的事件的名称</td></tr><tr><td>TP-ProTO(int firstarg, struct task_struct *p)</td><td>跟踪点调用的函数的原型，也就是桩函数</td></tr><tr><td>TP_args(firstarg, p)</td><td>参数名称，支持变长参数</td></tr></table>

如果要在内核中使用跟踪点，则可以使用EXPORT_TRACEPOINT_SYMBOLGPL()或EXPORT_TRACEPOINT_SYMBOL()来导出定义的跟踪点。

```txt
//https://github.com/torvalds/linux/blob/v5.15/include/linux/tracepoint.h #defineEXPORT_TRACEPOINT_SYMBOL_GPL(name)\ EXPORT_SYMBOLGPL(Tracepoint##name)； \ EXPORT_SYMBOLGPL(traceiter##name)； \ EXPORT_STATIC_CALL_GPL(tp_func##name) #defineEXPORT_TRACEPOINT_SYMBOL(name）\ EXPORT_SYMBOL(Tracepoint##name)； \ EXPORT_SYMBOL( traceiter##name)； \ EXPORT_STATIC_CALL(tp_func##name) 
```

下面分析一下tracepoint的实现原理。在内核源码目录include/linux/tracepoint.h中可以找到关于DEFINE_TRACE宏的定义，这里截取相应的代码片段。

define DEFINE_TRACEFN(_name,_reg,_unreg, proto, args)   
static const char _tpstrtab@@name[]   
__section("___tracepoints_strings") $=$ #_name;   
extern struct static_call_key STATIC_CALL_KEY(tp_func@@name);   
int __traceiter@@name(void \*data, proto);   
struct tracepoint __tracepoint@@name __used   
__section("___tracepoints") $=$ { .name $=$ _tpstrtab@@name, .key $=$ STATIC_KEY_INITFalse, .static_call_key $=$ &STATIC_CALL_KEY(tp_func@@name), .static_call_tramp $=$ STATIC_CALL_TRAMP_ADDR(tp_func@@name), .iterator $=$ &__traceiter@@name, .regfunc $=$ _reg, .unregfunc $=$ _unreg, .funcs $=$ NULL }; _TRACEPOINT_ENTRY(_name); int __nocfi __traceiter@@name(void \*data, proto) { struct tracepoint_func \*it_func_ptr; void \*it_func; it_func_ptr = rcu_dereference_raw((&__tracepoint@@name)->funcs); if (it_func_ptr) { do { it_func $=$ (it_func_ptr)->func; _data $=$ (it_func_ptr)->data; ((void \*(void \*, proto))(it_func))(_data, args); } while ((++it_func_ptr)->func); } return 0; } DEFINESTATICCALL(tp_func@@name, __traceiter@@name);   
#define DEFINE_TRACE(name, proto, args)   
DEFINE_TRACEFN(name, NULL, NULL, PARAMETERS(proto), PARAMETERS(args));

这个宏首先定义了tracepoint的全局结构体，以及tracepoint操作要用到的若干公共函数，并用到了##宏连接，为不同的跟踪点生成唯一的函数名及相关全局变量。structtracepoint定义在include/linux/tracepoint-defs.h中。

```txt
struct tracepoint {
    const char *name; // 跟踪点名称
```

```c
struct static_key key; // 跟踪点状态，1 表示开启，0 表示关闭
struct static_call_key *static_call_key;
void *static_call_tramp;
void *iterator;
int (*regfunc)(void); // 该函数指针指向跟踪点注册函数，用于将跟踪点注册到跟踪子系统中
void (*unregfunc)(void); // 该函数指针指向跟踪点注销函数，用于将跟踪点从跟踪子系统中注销
struct tracepoint_func __rcu *funcs; // 当前 tracepoint 中所有的桩函数列表
```

每个跟踪点都必须在启动时注册，以使跟踪子系统知道可用的跟踪点。注册函数会被跟踪子系统调用，从而将跟踪点添加到跟踪点列表中。在不再需要跟踪点时应该将其注销，以释放跟踪子系统中的资源。注销函数会被跟踪子系统调用，从而将跟踪点从跟踪点列表中移除。

DEFINE_TRACE宏通过register_trace##name函数完成对_tracepoint##name结构体的初始化，这个过程的主要目的是为特定的跟踪点提供探测器（probe，需要调用的挂钩函数），将探测器连接到跟踪点。register_trace##name调用了tracepoint Probe register，其源码在kernel/tracepoint.c中。

// Connect a probe to a tracepoint   
int tracepointProbe_register(struct tracepoint \*tp, void \*probe, void \*data)   
{ return tracepoint Probe register_prio(tp, probe, data, TRACEPOINT_DEFAULT_PRIO);   
}   
EXPORT_SYMBOL_GPL(tracepoint_probe/register);   
int tracepoint_probe_register_prio(struct tracepoint \*tp, void \*probe, void \*data,int prio)   
{ struct tracepoint_func tp_func; int ret; mutex_lock(&tracepoints_mutex); tp_funcfunc $=$ probe; tp_func.data $=$ data; tp_func.prio $=$ prio; ret $=$ tracepoint_add_func(tp,&tp_func,prio); mutex_unlock(&tracepoints_mutex); return ret;

tracepoint ProbeRegister 中调用了 tracepoint probe register_prio，构造了 tracepoint_func，最终调用 func_add 以添加到 struct tracepoint::funcs 桩函数列表中，也就是说一个 tracepoint 上可以注册多个探测器（Hook 函数）。这时可能有一个疑问，这么多 Hook 函数，谁先执行呢？tracepoint probe register_prio 中传入了 TRACEPOINT_DEFAULT_PRIO=10，这个值会一直传递到 func_add, func_add 函数会判断优先级，prio 数值越大，则插入列表中越靠前的位置；如果优先级相同，则先注册的放前面。相关代码如下：

static struct tracepoint_func *   
func_add(struct tracepoint_func **funcs, struct tracepoint_func *tp_func, int prio)   
{ struct tracepoint_func \*old, \*new; int iter_probes; int nr_probes $= 0$ int pos $= -1$ ... if (old) { nr_probes $= 0$ for (iter_probes $= 0$ ; old[iter_probes].func; iter_probes++) { if (old[iter_probes].func $= =$ tp_stub_func) continue; if (pos $<  0$ && old[iter_probes].prio $<$ prio) pos $=$ nr_probes++; new[nr_probes $+ + ] =$ old[iter_probes]; } if (pos $<  0$ pos $= \mathrm{nr\_probes} + +$ . } else { pos $= 0$ . nr_probes $= 1$ . } new[pos] $= *\mathrm{tp\_}$ func; new[nr_probes].func $=$ NULL; \*funcs $=$ new; debug_print_probes(*funcs); return old;

当然，最后别忘记，在不使用这些挂钩函数后，通过unregister_trace__name删除probe到tracepoint的连接。

接着看一下trace_nameProto)宏函数。代码如下：

```cpp
static inline void trace__name.proto)  
{  
    if (static_keyFalse(&__tracepoint__#name.key))  
        _DO_TRACE(name, TP_args(args), TP_CONDITION(cond), 0);  
    if (IS_ENABLED(CONFIG_LOCKDEP) && (cond)) { rcu_read_lock_sched_notrace();  
} 
```

```c
rcu_dereference_sched(_tracepoint__name.funcs); rcu_read_unlock_sched_notrace(); } 
```

这个函数首先判断tracepoint是否开启，如果开启则调用_DO_TRACE遍历执行tracepoint::funcs中的桩函数列表。然后由第2个if语句判断CONFIG_LOCKDEP是否配置，且条件cond是否成立，是则启动RCU读取锁定。这样做可以保证多个CPU可以同时读取共享数据结构，而不用担心跟踪操作中的竞态问题。

从上面的分析可以看出，tracepoint 的机制比较简单，就是把探测器（Hook 函数）的函数指针保存在一个函数指针列表中，当执行到预先埋点的 tracepoint 时，依次遍历执行这个函数指针列表，从而调用自定义的探测器来完成对应的探测工作。

查看系统支持的tracepoint列表的方式有很多，可以通过TraceFS的events目录查看，系统中定义的tracepoint都在这个目录下面。比如，查看syscalls子目录：

```shell
#/sys/kernel debug/tracing/events   
#/sys/kernel/tracing/events   
$ sudo ls /sys/kernel/tracing/events/syscalls   
...   
sys-enter_vmsplice sys_exit_wait4   
sys-enter_wait4 sys_exit_waitid   
sys-enter_waitid sys_exit_write   
sys-enter_write sys_exit_writev 
```

或者通过perf命令：

```txt
$ sudo perl list tracepoint 
```

还可以通过bpftrace命令查看：

```shell
$ sudo bpftrace -l tracepoint:* 
```

每个tracepoint会按照自己定义的格式进行输出。

```txt
$ sudo cat /sys/kernel debug/tracing/events/power/clock_enable/format
name: clock_enable
ID: 458
format:
field: unsigned short common_type; offset:0; size:2; signed:0;
field: unsigned char common_flags; offset:2; size:1; signed:0;
field: unsigned char common_preempt_count; offset:3; size:1; signed:0;
field:int common pid; offset:4; size:4; signed:1;
field:_data_LOC char[] name; offset:8; size:4; signed:1; 
```

```c
field:u64 state; offset:16; size:8; signed:0; field:u64 cpu_id; offset:24; size:8; signed:0; print fmt:"%s state=%lu cpu_id=%lu", __get_str(name), (unsigned long)REC->state, (unsigned long)REC->cpu_id 
```

# 3.6 eBPF数据采集点

eBPF提供了多种程序类型，这些程序被挂载到不同的数据采集点。这些数据采集点被称为eBPF的程序挂载点，当内核执行到这些挂载点时，会触发eBPF虚拟机去执行挂载的eBPF字节码。通常，注入的eBPF字节码程序中会包含执行时捕获上下文的参数信息。从字节码注入到数据采集这个过程来看，eBPF与传统Hook注入技术的思路相同，只是实现的技术框架与细节不同。

想要深入理解eBPF数据源，就需要了解eBPF的发展轨迹与更新历史，以及明白eBPF字节码是如何挂载到不同的内核节点与子系统上的。这一部分内容将在第4章和第10章详细介绍。

不同的eBPF程序类型实现不同内核子系统数据的采集，一个eBPF程序可以挂载到一个或多个类型挂载点，可以说eBPF挂载点具体体现了Linux内核子系统的不同执行环节。

libbpf仓库维护了一份eBPF的程序类型与eBPF程序挂载点的对应表（https://github.com/libbpf/libbpf/blob/master/docs/program_types.rst）。

下面介绍几个应用比较广泛的eBPF程序类型。

# 1. 网络套接字挂载点

在eBPF发展之初，还没有出现BPF系统调用时，BPF已经存在一段时间了。那时候网络数据包的过滤依赖的是setsockopt()函数，它为指定的套接字对象指定SO attaches_FILTER标志来附加BPF字节码。这种形式一直持续到BPF系统调用出现，相应的标志改成了SO attaches_BPF，加载进内核被执行的一段代码是由SEC("socket")标注的eBPF程序。这也是最早的eBPF数据源，即网络套接字挂载点。libbpf库将这种eBPF程序类型定义为BPF_PROG_TYPE_SOCKET_FILTER。

# 2. 内核探针

在支持数据包过滤后，eBPF尝试支持kprobe和kretprobe等探针点的挂载，目标是让eBPF代码可以监控特定内核函数的调用和返回。这使得开发者可以跟踪关键代码路径、参数值和返回结果，并进行故障排除或性能优化。这个功能非常强大，为eBPF社区的快速发展打下了很好的基础。而如何将eBPF字节码挂载到内核探针上执行，是需要细心的设计与实现的。eBPF的设计思路是将eBPF字节码通过BPF系统调用加载进内核，返回一个FD（文件描述符），将这个FD与内核探针具体的挂载目标（内核地址或内核函数）对应起来，这样挂载目标就会执行对应的eBPF字节码。具体的方法是配置TraceFS接口来设置要挂载的目标内核方法，写入相应的配置后，使用perf_event_open()来打开perf事件的采集，返回一个FD，这个FD与前面BPF系统调用返回的文件FD对应

起来，就完成了整个对接工作。这个对接的接口是 ioctl，对应的标志是 PERF_EVENTIOC_SET_BPF。需要注意的是，后期为了统一所有的挂载点与数据源接口的连接与销毁操作，eBPF 引入了 BPF Link 机制。libbpf 库将这种 eBPF 程序类型定义为 BPF_PROG_TYPE_KPROBE。

# 3. 跟踪点

跟踪点与内核探针的加载机制几乎是一样的，区别只在于跟踪点的一些信息是通过 TraceFS 的 events 路径来配置的，这个路径一般位于 /sys/kernel debug/tracing/events/目录下特定的 format 文件。跟踪点包括原始跟踪点与 WRITABLE 跟踪点，libbpf 库将这类 eBPF 程序类型定义为 BPF_PROG_TYPE_TRACEPOINT、BPF_PROG_TYPE_raw_tracePOINT、BPF_PROG_TYPERAW_TRACEPOINT_WRITABLE。

# 4. 网络子系统相关

内核4.4版本中引入了eBPF对TC子系统上流量分类与过滤的支持。后来在4.8版本中又引入了XDP，在更加底层的位置引入了数据包的转发、过滤与控制功能。当然，这些功能需要内核与eBPF程序共同配合来完成。这一部分的eBPF程序就非常多了，在libbpf库中，它们一部分以BPF_PROG_TYPE_SCHED开头命名，一部分以BPF_PROG_TYPE_SK开头命名，对于XDP类型，还有一个专门的BPF_PROG_TYPE_XDP程序类型。

# 5. CGROUP相关

eBPF从内核4.10版本开始精细化控制支持字节码挂载到CGROUP相关的网络接口上，用于扩展eBPF与容器相关的可观测性。libbpf库将这类eBPF程序命名为以BPF_PROG_TYPE_CGROUP_开头的一系列程序。

# 6. LSM

从内核5.7版本开始，eBPF支持将字节码挂载到LSM子系统上，以便对系统资源进行安全访问控制。在libbpf库中这类程序命名为BPF_PROG_TYPE LSM。

eBPF仍然处于快速发展与功能完善的阶段，它所支持的数据源也会越来越丰富。

# 3.7 本章小结

本章首先简单介绍了Linux的各个跟踪框架及其发展历史，让读者对Linux跟踪技术有一个初步的认识，然后详细介绍了tracepoint、ftrace、uprobe、kprobe等的使用方法和原理，为后续学习eBPF打下基础。了解这些Linux跟踪技术及其实现原理，有助于学习和理解eBPF相关知识。

# 第4章 eBPF程序入门

经过前3章的学习，我们已经了解了eBPF和Linux跟踪技术，并准备好了eBPF开发相关的环境。本章开始讲解与eBPF开发相关的内容，先从第一个简单eBPF程序开始，带着读者详细剖析eBPF程序涉及的知识点，为后面的学习打下基础。

# 4.1 第一个eBPF程序

第一个eBPF程序的编写原则，首先是代码要简洁易懂，其次是要尽可能地体现程序的加载与运行机制。本节将编写两个简单的程序，一个是BCC版本的eBPF程序，另一个是使用C语言编写的原生eBPF程序，然后再分别看看这两个程序如何加载和运行。

# 4.1.1 第一个BCC程序

先从简单的入手，使用Python让我们可以非常快速地进行eBPF开发。下面看看eBPF Python版本的HelloWorld程序，这里使用BCC框架编写。

```python
#!/usr/bin/python
# Copyright (c) PLUMgrid, Inc.
# Licensed under the Apache License, Version 2.0 (the "License")
# run in project examples directory with:
# sudo ./hello_world.py"
# see trace_fields.py for a longer example
from bcc import BPF
# This may not work for 4.17 on x64, you need replace kprobe_sysClone with kprobe_x64_sysClone
BPF(text='int kprobe_sysClone(void *ctx) { bpf_trace_printk("Hello, World!\n"); return 0; }').traceprint() 
```

整个程序去掉 Shebang（!/usr/bin/python）与注释部分以后，简洁到只有两行内容：首先创建一个 BPF 对象，然后调用这个对象里面的 trace_print()实例方法。

- 其中“text $=$ ”后面的内容定义了一个C语言的内联的eBPF程序。在创建eBPF对象时，

会将这段内联代码编译成BCC字节码。

- kprobe_sysClone(): 这是通过 kprobe 进行内核动态跟踪的跟踪点，这个格式一般以 kprobe 开头，后面是具体跟踪的内核函数名称，本例中为 sysClone()。sysClone 在 Linux 进程创建过程中调用，负责进程的复制。  
- bpf_trace_printk(): 这是 eBPF 的内核辅助函数，将传入的信息写入 trace_pipe（/sys/kernel/ debug/tracing/trace_pipe）中。  
- trace_print: eBPF 中的实例方法，它的作用是读取 bpf_trace_printk 写入 trace_pipe 中的内容。

代码非常简单，内容却不少。将上述代码保存为hello_world.py，打开终端，开启两个shell窗口，其中一个执行sudo python hello_world.py（注意这里必须加上sudo，不然会执行失败，显示没有相应的权限），另外一个shell窗口随便执行一个命令，比如ls，会看到类似如下Hello World的输出（如果没有输出，参考2.4.1节中BCC环境的安装）。

```txt
$ sudo python hello_world.py
b' bash-63926 [000] d...1 82644.970525: bpf_trace_printk: Hello, World!"
b' bash-63926 [000] d...1 82649.637351: bpf_trace_printk: Hello, World!"
b' bash-63926 [001] d...1 82651.159453: bpf_trace_printk: Hello, World!"
b' bash-63926 [001] d...1 82651.966271: bpf_trace_printk: Hello, World!' 
```

输出的内容从左到右的含义分别如下。

- bash-63926: bash 是进程名称, 有时候也会缩写成 $< \cdots >$ , 63926 是进程的 PID。  
- [000]或者[001]：表示运行在哪个CPU核心上面。  
- 82644.970525：系统启动的时间戳。

# 4.1.2 第一个C语言版本的eBPF程序

下面再看一个C语言版本的eBPF程序。代码如下：

```c
// clang hello_world.c -o hello_world -lbcc  
#include <errno.h>  
#include <fcntl.h>  
#include <limits.h>  
#include <linux/bpf.h>  
#include <linux/perf_event.h>  
#include <linux/version.h>  
#include <signal.h>  
#include <stdio.h>  
#include <stdlib.h>  
#include <string.h> 
```

include <sys/ioct1.h>   
#include <sys/syscall.h>   
#include <unistd.h>   
#include <bpf/bpf.h>   
#include <bcc/libbbpf.h>   
//https://github.com/torvalds/linux/blob/master/samples/bpf/bpf_insn.h   
#define LOGBuf_SIZE 65536   
char bpf_log_buf[LOGBuf_SIZE];   
static inline u64 ptr_to_u64(const void \*ptr){ return (u64)(unsigned long)ptr;   
}   
int bpf_prog_load(enum bpf_prog_type type, const struct bpf_insn *insns, int insn_cnt, const char \*license){ union bpf_attr attr; memset(&attr,0,sizeof(attr)); attrprog_type $=$ type; attr.insns $=$ ptr_to_u64(insns); attr.insn_cnt $=$ insn_cnt; attr licensed $=$ ptr_to_u64(license); attr.log_buf $=$ ptr_to_u64(bpf_log_buf); attr.log_size $=$ LOGBuf_SIZE; attr.log_level $= 1$

```javascript
// 根据man手册，若程序类型为BPF_PROG_TYPE_KPROBE，必须定义kern_version，其中LINUX_VERSION_CODE的定义在<linux-version.h>中attr.kern_version = LINUX_VERSION_CODE;
```

```txt
// 如果该函数返回一个非零值，可以通过打印 bpf_log_buf 的内容进行调试。libbpf.c 提供了 bpfprint_hints() 函数来协助调试
```

return syscall(_NR_bpf, BPF_PROG_LOAD, &attr, sizeof Attr));   
}   
int wait_for_sig_int() { sigset_t set; sigemptyset(&set); int rc $=$ sigaddset(&set,SIGINT); if $(\mathrm{rc} <   0)$ { perror("Error calling sigaddset()); return 1;

```c
rc = sigprocmask(SIG_BLOCK, &set, NULL); if (rc < 0) { perror("Error calling sigprocmask()); return 1; } int sig; rc = SIGwait(&set, &sig); if (rc < 0) { perror("Error calling sigwait())); return 1; } else if (sig == SIGINT) { fprintf(stderr, "SIGINT received!\n"); return 0; } else { fprintf(stderr, "Unexpected signal received: %d\n", sig); return 0; } } /* * * * 来自libbpf.c的bpf.attach_tracing_event()的移植版本 */ int attach_tracing_event(int prog_fd, const char *event_path, int *pfd) { int efd; ssize_t bytes; char buf[PATH_MAX]; struct perf_event_attr attr = {}; // 调用者没有提供有效的Perf Event FD。使用提供的debugfs事件路径创建一个新的FD snprintf(buf, sizeof(buf), "%s/id", event_path); efd = open(buf, O_RDONLY, 0); if (efd < 0) { fprintf(stderr, "open(%s): %s\n", buf, strlen(errno)); return -1; } bytes = read(efd, buf, sizeof(buf)); if (bytes <= 0 || bytes >= sizeof(buf)) { fprintf(stderr, "read(%s): %s\n", buf, strlen(errno)); close(efd); return -1; } close(efd); buf[bytes] = '\0'; 
```

attr.config = strtok(buf, NULL, 0);   
attr.type = PERF_TYPE_TRACEPOINT;   
attr.sample_period = 1;   
attr.wakeup_events = 1;   
\*pfd $=$ syscall(_NR_perf_event_open,&attr,-1/\*pid\*/,0/\*cpu\*/, -1/\*group_fd\*/,PERF_FLAG_FD_CLOEXEC); if $(^{*}\mathrm{pfd} <   0)$ { fprintf(stderr, "perf_event_open(%s/id): %s\n", event_path, strerror(errno)); return -1; } if (ioct1(\*pfd, PERF_EVENTIOC_SET_BPF, prog_fd) < 0) { perror("ioct1(PERF_EVENTIOC_SET_BPF))"); return -1; } if (ioct1(\*pfd, PERF_EVENTIOC_ENABLE, 0) < 0) { perror("ioct1(PERF_EVENTIOC_ENABLE))"); return -1; } return 0;   
}   
/\*\* \* 这是来自bpf.attach_kprobe()的简易实现，该函数在libbpf.c中 \*/ int attach_kprobe(int prog_fd, const char \*ev_name, const char \*fn_name){ static char \*event_type $=$ "kprobe"; int kfd $=$ open("/sys/kernel debug/tracing/kprobe_events",O_WRONLY|O_APPEND,0); if (kfd < 0) { perror("Error opening /sys/kernel debug/tracing/kprobe_events"); return -1; } char buf[256]; char event alias[128]; //别名加上pid的原因见https://github.com/iovisor/bcc/issues/872 snprintf(event alias,sizeof(event alias),"\%s_bcc_%d",ev_name,evpid()); int BPF_PROBE_ENTRY $= 0$ int BPF_PROBE_RETURN $= 1$

//假设函数的偏移量为0   
```txt
int attach_type = BPF_PROBE_ENTRY;  
snprintf(buf, sizeof(buf), "%c:%ss/%s %s",  
    attach_type == BPF_PROBE_ENTRY ? 'p' : 'r', event_type, event alias,  
    fn_name); 
```

//写入类似如下的字符串到kprobe_events文件中  
```c
//p:kprobes/p_do_sys_open_bcc_<pid>do_sys_open  
if (write(kfd, buf, strlen(buf)) < 0) {  
    if (errno == ENOENT) {  
        //write(2)函数没有提到ENOENT错误，这可能是与该内核文件描述符有关的一些特殊问题  
        fprintf(stderr, "cannot attach kprobe, probe entry may not exist\n");  
    }else{  
        fprintf(stderr, "cannot attach kprobe, %s\n", strlen(errno));  
    }  
close(kfd);  
return -1;  
}  
close(kfd);
```

// 设置buf为如下路径  
```c
// /sys/kernel debug/tracing/events/kprobes/p_do_sys_open_bcc_<pid>snprintf(buf, sizeof(buf), "/sys/kernel debug/tracing/events/%ss/%s", event_type, event Alias); int pfd = -1; // 从buf路径中读取事件ID，使用该ID创建PerfEvent事件，并更新pfd的值 if (attach_tracing_event(prog_fd, buf, &pfd) < 0) { return -1; } return pfd;   
}   
int main(int argc, char **argv) { struct bpf_insn prog[] = { BPF_MOV64_IMM(BPF_REG_1, 0xa21), /* !!\n*/ BPF_STX_MEM(BPF_H, BPF_REG_10, BPF_REG_1, -4), BPF_MOV64_IMM(BPF_REG_1, 0x646c726f), /* 'orld' */ BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -8), BPF_MOV64_IMM(BPF_REG_1, 0x57202c6f), /* 'o, W' */ BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -12), BPF_MOV64_IMM(BPF_REG_1, 0x6c6c6548), /* 'Hell' */ BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -16), BPF_MOV64_IMM(BPF_REG_1, 0), BPF_STX_MEM(BPF_B, BPF_REG_10, BPF_REG_1, -2), 
```

BPF_MOV64_REG(BPF_REG_1, BPF_REG_10), BPF_ALU64_IMM(BPF_ADD, BPF_REG_1, -16), BPF_MOV64_IMM(BPF_REG_2, 15), BPF_raw_insN(BPF_JMP | BPF_CALL, 0, 0, 0, BPF FUNC_trace_printk), BPF_MOV64_IMM(BPF_REG_0, 0), BPF_EXIT_insN(),   
}；   
int insn_cnt $=$ sizeof(prog)/sizeof(struct bpf_insn); int prog_fd $\equiv$ bpf_prog_load(BPFProg_TYPE_KPROBE，prog，insn_cnt,"GPL"); if(prog_fd $= = -1$ { perror("Error calling bpf_prog_load()); return 1;   
}   
int perf_event_fd $=$ attach_kprobe(prog_fd, "hello_world", "do_unlinkat"); if(perf_event_fd $<  0$ { perror("Error calling attach_kprobe()"); close(prog_fd); return 1;   
}   
system("cat /sys/kernel debug/tracing/trace_pipe"); int exit_code $=$ wait_for_sig_int(); close(perf_event_fd); close(prog_fd); return exit_code;

可通过Clang编译代码，以sudo方式执行程序后，可以打开另一个shell来执行一些命令，就可以看到HelloWorld的输出了。

```yaml
$ clang hello_world.c -o hello_world -lbcc
sudo ./hello_world
dconf-service-2732 [003] d...31 27574.145245: bpf_trace_printk: Hello, World!
dconf-service-2732 [002] d...31 27574.148024: bpf_trace_printk: Hello, World!
<...>-787 [001] d...31 27606.574273: bpf_trace_printk: Hello, World! 
```

# 4.2 eBPF程序功能解读

上面的C语言代码比较多，先从main()函数开始。首先定义了名为prog的bpf_insn数组，每一条eBPF指令都以一个bpf_insn结构体来表示。eBPF指令会在4.4节讲解。目前可以忽略具体含

义，当然在实际开发过程中，直接使用eBPF指令开发也是非常烦琐的，一般使用高级开发框架如libbpf或ebpf-go等，配合C语言或者Golang进行开发。在编译器自动生成这些eBPF指令中间文件后，由高级开发框架提供的接口来完成字节码加载与附加操作。

__BPF FUNC Mapper 宏定义了 eBPF 的辅助函数列表，FN 宏传入的其实是一个__BPFuckenFN宏，它在展开时，会为每个函数声明加上字符串“BPF FUNC_”，比如trace_PRINTk展开后表示的是BPF FUNC_trace_PRINTk。__BPF FUNC Mapper宏声明如下：

```c
define_BPF FUNC Mapper(FN)  
FN(unsigned),  
FN(mapLOOKUP_element),  
FN(map_update_element),  
FN(map_delete_element),  
FN(probe_read),  
FN(ktime_get_ns),  
FN(trace_printk), 
```

随后调用了bpf_prog_load函数加载prog指令数组到内核，bpf_prog_load返回一个程序的文件描述符，bpf_prog_load主要传入以下4个参数。

- 第1个参数：BPF_PROG_TYPE_KPROBE，这是Linux内核所支持的eBPF程序类型。  
- 第2个参数：prog，指令数组。  
- 第3个参数：insn_cnt，数组大小。  
- 第4个参数：“GPL”，GPL的授权协议字符串。授权协议将在4.3节中讲解。

然后调用了attach_kprobe，这个函数是libbpf框架的bpf.attach_kprobe的简单手工实现，用于动态加载eBPF到kprobe探针点上，通过将eBPF代码附加到特定内核函数调用的入口处，返回一个FD，传入3个参数。

- 第1个参数：bpf_prog_load返回的文件描述符。  
- 第2个参数：传入了一个“hello_world”字符串，作为kprobe_events的别名。  
- 第3个参数：传入了一个“do_unlinkat”字符串，这是一个kprobe探针。

最后，通过system()调用cat命令来读取trace_pipe文件的输出，然后等待组合键 $\mathrm{Ctrl + C}$ 的终止信号的到来，从而释放资源，退出程序。

当系统执行do_unlinkat时，就会执行挂钩的hello_world方法，从而可以在trace_pipe中读取到输出内容。

# 4.2.1 加载eBPF字节码

现在看看bpf_prog_load函数内部的实现，它首先初始化了bpf_attr，将prog指令数组、eBPF程序类型、授权协议、内核版本等信息设置到bpf_attr结构体中。需要注意的是，在使用bpf_attr结构体之前必须将其清零，如果不这样做，调用syscall执行BPF系统调用（声明为__NR_bpf)，

并传入bpf_prog_load时可能会导致EINVALID错误。BPF系统调用执行加载eBPF字节码成功后，返回一个与eBPF程序关联的文件描述符。

其中每个Linux支持的eBPF程序类型是不一样的。本书以Linux5.15.0内核举例，支持的eBPF程序类型如下（关于bpf_prog_type的定义，读者可以查看include/uapi/linux/bpf.h文件内容）：

```txt
//https://github.com/torvalds/linux/blob/v5.15/include/uapi/linux/bpf.h
enum bpf_prog_type {
    BPF_prog_TYPE_UNSPEC,
    BPF_prog_TYPE_SOCKET_FILTER,
    BPF_prog_TYPE_KPROBE,
    BPF_prog_TYPE_SCHED_CLS,
    BPF_prog_TYPE_SCHED_ACT,
    BPF_prog_TYPE_TRACEPOINT,
    BPF_prog_TYPE_XDP,
    BPF_prog_TYPE_PERF_EVENT,
    BPF_prog_TYPE_CGROUP_SKB,
    BPF_prog_TYPE_CGROUP_SOCKET,
    BPF_prog_TYPE_LWT_IN,
    BPF_prog_TYPE_LWT_OUT,
    BPF_prog_TYPE_LWT_XMIT,
    BPF_prog_TYPE_SOCKET_OPS,
    BPF_prog_TYPE_SK_SKB,
    BPF_prog_TYPE_CGROUP_DEVICE,
    BPF_prog_TYPE_SK_MSG,
    BPF_prog_TYPE_raw_tracePOINT,
    BPF_prog_TYPE_CGROUP_SOCKET_ADDR,
    BPF_prog_TYPE_LWT_SEG6LOCAL,
    BPF_prog_TYPE_LIRC_MODE2,
    BPF_prog_TYPE_SK_REUSEPORT,
    BPF_prog_TYPE_FLOW_DISSECTOR,
    BPF_prog_TYPE_CGROUP_SYSCTL,
    BPF_prog_TYPERaw_tracePOINT_WRITABLE,
    BPF_prog_TYPE_CGROUP_SOCKETOPT,
    BPF_prog_TYPE_TRACING,
    BPF_prog_TYPE structuresOPS,
    BPF_prog_TYPE_EXT,
    BPF_prog_TYPE LSM,
    BPFProgTypeSKLOOKUP,
    BPFProgTypeSYSCALL,
};
```

# 4.2.2 BPF系统调用

我们知道，系统调用是Linux操作系统提供的一组接口，供用户程序与内核进行交互和通信。

这里展开介绍BPF系统调用，其中调用号是__NR_bpf，BPF_PROG_LOAD是操作码，每种操作都通过attr传递对应的参数。

```txt
syscall(_NR_bpf, BPF_PROG_LOAD, &attr, sizeof Attr)); 
```

这里的操作码定义在linux/bpf.h头文件中。

```txt
enum bpf_cmd {
    BPF_MAP_CREATE,
    BPF_MAPLOOKUP_ELEM,
    BPF_MAP_UPDATE_ELEM,
    BPF_MAP_DELETE_ELEM,
    BPF_MAP_GET_NEXT_KEY,
    BPF_PROG_LOAD,
    BPF_OBJ_PIN,
    BPF_OBJ_GET,
    BPF_PROG_ATTACH,
    BPF_PROG_DETACH,
    BPF_PROG_TEST_RUN,
    BPF_PROG Runs = BPF_PROG_TEST Runs,
    BPF_PROG_GET_NEXT_ID,
    BPF_MAP_GET_NEXT_ID,
    BPF_PROG_GET_FD_BY_ID,
    BPFMAP_GET_FD_BY_ID,
    BPF_OBJ_GET_INFO_BY_FD,
    BPF_PROG_QUERY,
    BPF_raw_tracePOINT_OPEN,
    BPF_BTF_LOAD,
    BPF_BTF_GET_FD_BY_ID,
    BPF_TASK_FD_QUERY,
    BPF_MAP LOOKUP_AND_DELETE_ELEM,
    BPF_MAP_freeze,
    BPF_BTF_GET_NEXT_ID,
    BPFMAP LOOKUPBatch,
    BPFMAP LOOKUP_AND_DELETEBatch,
    BPFMAP_UPDATEBatch,
    BPFMAPDELETEBatch,
    BPFLINK_create,
    BPFLINK_UPDATE,
    BPFLINK_GET_FD_BY_ID,
    BPFLINK_GET_NEXT_ID,
    BPF_ENABLE_STAT,
    BPFIterCreate,
    BPFLINK_DETACH,
    BPF_PROG_BIND_MAP,
}; 
```

其中BPF_PROG_LOAD主要用于验证和加载一个eBPF程序，返回一个与该程序相关的文件描述符，同时close-on-exec文件描述符标志自动启用新文件描述符。如果发生错误，返回-1，此时errno会设置。

BPF系统调用会传递一个bpf_attr，这是一个联合体的定义，在内核代码include/uapi/linux/bpf.h中，关于BPF_PROG_LOAD的结构体如下，用于描述BPF_PROG_LOAD的相关信息，如eBPF指令数组、eBPF程序类型、验证器的详细级别、内核版本等。由于这个联合体的定义非常大，这里不详细讲解每个结构具体的含义，本书其他章节引用到了具体的字段时再说明。

```c
union bpf_attr{   
struct{ /\*BPF_PROG_LOAD命令使用\*/ _u32 prog_type; /\*eBPF程序类型\*/ _u32 insn_cnt; _aligned_u64 insns; _aligned_u64 license; _u32 log_level; /\*验证器的详细级别\*/ _u32 log_size; /\*buff大小\*/ _aligned_u64 log_buf; /\*用户缓冲区\*/ _u32 Kern_version; /\*内核版本\*/ _u32 prog_flags; char prog_name[BPF_OBJ_NAME_LEN]; _u32 prog_ifindex; //对于某些程序类型，必须在加载时知道预期的附加类型 _u32 expected Attached_type; _u32 prog_btf_fd; /\*指向BTF类型的文件描述符指针\*/ _u32 func_info_rec_size; /\*用户空间bpf_func_info大小\*/ _aligned_u64 func_info; /\*函数信息\*/ _u32 func_info_cnt; /\*bpf_func_info记录个数\*/ _u32 line_info_rec_size; /\*用户空间bpf_func_info大小\*/ _aligned_u64 line_info; /\*行信息\*/ _u32 line_info_cnt; /\*bpf_line_info记录个数\*/ _u32 attach_btf_id; /\*要附加的内核BTF类型\*/ union{ //用于检查具体是bpf prog还是btf u32 attachProg_fd; u32 attach_btf_obj_fd; }； _u32 :32; /\*填充\*/ _aligned_u64 fd_array; /\*FD数组\*/ 1   
}__attribute_((aligned(8))）;
```

# 4.2.3 attach_kprobe

接着展开介绍attach_kprobe的实现，这个函数是libbpf中bpf.attach_kprobe()的简易实现。关于bpf.attach_kprobe()的实现在libbpf框架的libbpf.c文件中。

attach_kprobe 实现了一个简化版本的 bpf.attach_kprobe()函数，该函数用于动态附加 eBPF kprobe 探针。它打开了 /sys/kernel debug/tracing/kprobe_events 文件，在其中写入要附加的 kprobe 探针的信息，例如要监听哪个内核函数、使用的 eBPF 程序的文件描述符等。然后使用 attach_tracing_event() 函数将 perf 事件附加到 kprobe 上，实现对指定内核函数的跟踪和监视。如果函数执行成功，则返回 perf 事件的文件描述符，否则返回 -1。其中，event Alias 是一个附加到 kprobe 上的别名，其名称基于进程 ID 和待跟踪函数的名称。

最后再来看看attach_tracing_event()函数的实现，该函数将eBPF程序附加到指定的跟踪事件上。它从debugfs路径中读取事件ID，并使用该ID创建一个新的perf事件文件描述符。然后该函数使用ioctl()函数将eBPF程序附加到perf事件上，并启用该事件以开始跟踪。

# 4.2.4 perf_event_open系统调用

第一个eBPF程序中调用了perf_event_open系统调用，来看看相关的介绍。通过perf_event_open系统调用，创建了一个文件描述符，探测相关性能信息，每个文件描述符对应一个被探测的事件，这些可以被组合在一起同时探测多个事件。这些事件可以分别通过ioct1和prctl两种方式启用和禁用。perf事件分为两种类型：计数和采样。计数事件用于计算发生的所有事件的总数，通常，计数事件的结果是通过read调用收集的；采样事件会定期将测量值写入缓冲区，然后通过mmap进行访问。perf_event_open系统调用的声明如下。

```c
include <linux/perf_event.h>   
#include <linux/hw_breakpoint.h>   
#include <sys/syscall.h>   
#include <unistd.h>   
int syscall(SYS_perf_event_open，struct perf_event_attr\*attr, pid_t pid，int cpu，int group_fd，unsigned long flags); 
```

在使用过程中，一般可以将上面的syscall包装成如下包装函数：

static long perf_event_open(struct perf_event_attr \*hw_event, pid_t pid, int cpu, int group_fd, unsigned long flags) { int ret; ret $=$ syscall(_NR_perf_event_open,hw_event,pid,cpu, group_fd, flags); return ret;

参数部分的含义如下。

- pid 和 cpu 参数的含义如表 4-1 所示。

表 4-1 pid 和 cpu 参数  

<table><tr><td>参数情况</td><td>说明</td></tr><tr><td>pid == 0 且 cpu == -1</td><td>在任何CPU上跟踪调用进程/线程</td></tr><tr><td>pid == 0 且 cpu ≥ 0</td><td>只在指定CPU上运行时跟踪调用进程/线程</td></tr><tr><td>pid &gt; 0 且 cpu == -1</td><td>跟踪任何CPU上指定的进程/线程</td></tr><tr><td>pid &gt; 0 且 cpu ≥ 0</td><td>只在指定CPU上运行时跟踪指定进程/线程</td></tr><tr><td>pid == -1 且 cpu ≥ 0</td><td>跟踪指定CPU上的所有进程/线程。需要CAP_PERFMON(Linux 5.8及以上版本)或CAP_SYS_admin能力或/proc/sys/core/perf_event_paranoid的值小于1</td></tr><tr><td>pid == -1 且 cpu == -1</td><td>这个设置是无效的，将返回错误</td></tr></table>

- group_fd 参数允许创建事件组。事件组有一个事件作为组长。先创建组长，其 group_fd 设置为 -1。组里面的其余成员通过随后的 perf_event_open()调用来创建，其 group_fd 设置为组长的文件描述符（创建一个单独的事件时 group_fd 设置为 -1，被视为只有一个成员的组）。事件组作为一个整体被调度到 CPU 上，即只有当组内所有事件可以被放入 CPU 时，整个组才会被 CPU 调度。

- flags参数的含义如表4-2所示。

表 4-2 flags 参数  

<table><tr><td>参数</td><td>说明</td></tr><tr><td>PERF_FLAG_FD_CLOEXEC</td><td>该标志启用创建的事件文件描述符的close-on-exec标志,以便在执行 execve后自动关闭文件描述符。在创建时设置close-on-exec标志而不是后来用fcntl设置,可以避免可能的竞态条件(自Linux 3.14起)</td></tr><tr><td>PERF_FLAG_FD_NO_GROUP</td><td>该标志告诉事件忽略group_fd参数</td></tr><tr><td>PERF_FLAG_FD_OUTPUT</td><td>该标志重新路由事件的抽样输出,将其包含在由group_fd指定的事件的 mmap缓冲区中(自Linux 2.6.35起出现了问题)</td></tr><tr><td>PERF_FLAG_PID_CGROUP</td><td>该标志激活每个容器的系统范围监视。容器是用于更细粒度控制(CPU、内存等)的一组资源的抽象。在此模式下,仅当运行在受监视的CPU上的线程属于指定的容器(cgroup)时,才会测量事件。该cgroup通过其在cgroupfs文件系统中打开的目录上的文件描述符来标识。例如,如果要监视的cgroup名称为test,则必须将在/dev/cgroup/test上打开的文件描述符(假设cgroupfs挂载在/dev/cgroup)作为pid参数传递。cgroup监视仅适用于系统范围事件,因此可能需要额外的权限(自Linux 2.6.39起)</td></tr></table>

- perf_event_attr: 为创建的事件提供详细配置信息。其结构体信息如下。

```javascript
structperf_event_attr{ u32type; /\*perf事件类型\*/ 
```

_u32 size; /\*当前结构体大小\*/  
_u64 config; /\*特定的配置，不同的事件类型有不同的配置\*/union{u64 sample_period; /\*采样周期\*/u64 sample_freq; /\*采样频率\*/};_u64 sample_type; /\*在采样时，需要保存哪些数据\*/_u64 read_format; /\*在统计时读取数据的格式\*///bit位标记_u64disabled:1,\*/如果初始化为disabled，后续可以通过ioct1/prct1来使能\*/inherit :1, /\*如果该标志被设置，event进程对应的子进程也会进行统计\*/pinned:1,\*/如果该标志被设置，event和CPU绑定\*/exclusive:1,\*/如果该标志被设置，指定当这个group在CPU上时，它应该是唯一使  
用CPU计数器的group\*/exclude_user:1,\*/不统计user\*/exclude_kernel:1,\*/不统计kernel\*/exclude_hv:1,\*/不统计hypervisor\*/exclude_idle:1,\*/不统计idle\*/mmap:1,\*/允许记录PORT_EXEC mmap\*/comm:1,\*/允许记录创建进程时的comm数据\*/freq:1,\*/确定采样模式是freq还是period\*/inherit_stat:1,\*/每个任务计数\*/enable_on_exec:1,\*/下次执行启用\*/task:1,\*/跟踪fork/exit\*/watermark:1,\*/wakeup.watermark\*/precise_ip:2,\*/限制滑动\*/mmap_data:1,\*/非执行mmap数据\*/sample_id_all:1,\*/sample_type的所有事件\*/exclude_host:1,\*/不在host中计数\*/excludeguest:1,\*/不在guest中计数\*/exclude_callchain_kernel:1, /\*排除内核调用链\*/exclude_callchain_user: $1$ /\*排除用户调用链\*/mmap2:1,\*/包括具有inode数据的mmap\*/comm_exec:1,\*/标记与exec相关的comm事件\*/use_clockid:1,\*/使用时钟ID用于时间字段\*/context_SWITCH:1,\*/上下文切换数据\*/write_backward:1,\*/从末尾到开头写环形缓冲区\*/namespaces:1,\*/包含命名空间数据\*/ksymbol:1,\*/包括ksymbol事件\*/bpf_event:1,\*/包括bpf事件\*/aux_output:1,\*/生成AUX记录而不是事件\*/

```c
cgroup : 1, /* 包含 cgroup 事件 */
text_poke : 1, /* 包括 text poke 事件 */
____reserved_1 : 30;
union {
    __u32 wakeup_events; /* 每 n 个事件唤醒 */
    __u32 wakeup-watermark; /* 唤醒前的字节数 */
};
__u32 bp_type; /* 断点类型 */
union {
    __u64 bp_addr; /* 断点地址 */
    __u64 kprobe_func; /* 用于 perf_kprobe */
    __u64uprobe_path; /* 用于 perfuprobe */
    __u64 config1; /* config 扩展 */
};
union {
    __u64 bp_len; /* 断点长度 */
    __u64 kprobe_addr; /* with kprobe_func == NULL */
    __u64 probe_offset; /* 用于 perf_[k,u]probe */
    __u64 config2; /* config1 扩展 */
};
__u64 branch_sample_type; /* 枚举 perf_branch_sample_type */
__u64 sampleRegs_user; /* 用户寄存器在采样中转储 */
__u32 sample_stack_user; /* 转储堆栈大小 */
__s32 clockid; /* 用于时间字段的时钟 */
__u64 sampleRegs_intr; /* 在示例中转储的 regs */
__u32 aux WATERmark; /* 唤醒前的辅助字节 */
__u16 sample_max_stack; /* 调用链中的最大帧 */
__u16 __reserved_2; /* 对齐到 u64 */
};
```

perf可以利用PMU（PerformanceMonitoringUnit）、tracepoint和内核中的特殊计数器来进行性能统计，它将这些计数器包装到perf事件中，通过perf_pmu register进行注册，最后通过perf_event_open系统调用暴露给用户空间进行使用。

其中perf支持的事件类型如下：

//https://github.com/torvalds/linux/blob/v5.19/include/uapi/linux/perf_event.h  
enum perf_type_id{PERF_TYPE_HARDWARE $= 0$ //硬件PERF_TYPE_SOFTWARE $= 1$ //软件PERF_TYPE_TRACEPOINT $= 2$ //跟踪点

```c
PERF_TYPE_HW_CACHE = 3, // 硬件 Cache  
PERF_TYPERaw = 4, // RAW  
PERF_TYPE_BREAKPOINT = 5, // 断点  
PERF_TYPE_MAX,
```

通过如下命令查看内核源码中perf_pmu register注册的事件类型。

```txt
# kernel目录
$ cd linux-source-5.19.0
$ grep -r "perf_pmu_register" kernel/ | grep "\"
grep: kernel/events/hw_breakpoint.o: binary file matches
grep: kernel/events/core.o: binary file matches
kernel/events/hw_breakpoint.c: perf_pmu register(&perf_breakpoint, "breakpoint", PERF_TYPE_BREAKPOINT);
kernel/events/core.c: perf_pmu register(&perf_tracepoint, "tracepoint", PERF_TYPE_TRACEPOINT);
kernel/events/core.c: perf_pmu register(&perf_kprobe, "kprobe", -1);
kernel/events/core.c: perf_pmu register(&perf_uprobe, "uprobe", -1);
kernel/events/core.c: perf_pmu register(&perf_swevent, "software", PERF_TYPE_SOFTWARE);
# arch目录
$ grep -r "perf_pmu_register" arch/ | grep "\"
grep: arch/x86/events/msr.o: binary file matches
arch/arc/kernel/perf_event.c: return perf_pmu_register(&arc_pmu->pmu, "arc_pct", PERF_TYPEeous);
arch/extensa/kernel/perf_event.c: ret = perf_pmu_register(&xtensa_pmu, "cpu", PERF_TYPEeous);
arch/csky/kernel/perf_event.c: ret = perf_pmu_register(&csky_pmu.pmu, "cpu", PERF_TYPEeous);
arch/alpha/kernel/perf_event.c: perf_pmu_register(&pmu, "cpu", PERF_TYPEeous);
arch/powerpc/perf/8xx-pmu.c: return perf_pmu_register(&mpc8xx_pmu, "cpu", PERF_TYPEeous);
arch/powerpc/perf/core-book3s.c: perf_pmu_register(&power_pmu, "cpu", PERF_TYPEeous);
arch/powerpc/perf/core-fsl-emb.c: perf_pmu_register(&fsl_emb_pmu, "cpu", PERF_TYPEeous);
arch/mips/kernel/perf_event_mipsxx.c: perf_pmu_register(&pmu, "cpu", PERF_TYPEeous);
arch/sparc/kernel/perf_event.c: perf_pmuRegister(&pmu, "cpu", PERF_TYPEeous);
arch/sh/kernel/perf_event.c: perf_pmuRegister(&pmu, "cpu", PERF_TYPEeous);
...
grep: arch/x86/boot/compressed/vmlinx.bin: binary file matches
arch/s390/kernel/perf_cpf.c: rc = perf_pmuRegister(&cpumf_pmu, "cpum_cpf", -1);
arch/s390/kernel/perf_cpf.c: rc = perf_pmuRegister(&cf_diag, "cpum_cdf.diag", -1);
arch/s390/kernel/perf_cpf.sfc.c: err = perf_pmuRegister(&cpumf_sampling, "cpum_ 
```

sf",PERF_TYPERaw);

arch/s390/kernel/perf_pai_crypto.c: rc = perf_pmu_register(&paicrypt, "pai_crypto", -1);

perf_pmu register 是非常重要的函数，它将注册的 PMU 加入全局链表 pmus 中进行统一管理，从上面可以看出 perf 注册了各种事件类型，如 kprobe、uprobe、software、cpu 等。

可以通过strace命令跟踪perf命令，这里只关注openat和perf_event_open，它们分别跟踪perf打开了哪些文件及注册了哪些采样事件。

```shell
$ sudo strace -e openat,perf_event_open perf record -e 'syscalls:sys-enter_openat' -aR sleep 2 
```

输出比较多，perf_event_open和openat分别显示

#perf_event_open相关（截取部分内容）

```prolog
perf_event_open({type=PERF_TYPE_Hardware, size=PERF_ATTR_SIZE.Ver7, config=PERF_COUNT_HW_INSTRUCTIONS, sample_period=0, sample_type=0, read_format=0, exclude_kernel=1, exclude_hv=1, precise_ip=0 /* arbitrary skid */, excludeguest=1, ..., -1, 0, -1, PERF_FLAG_FD_CLOEXEC) = -1 ENOENT (No such file or directory)
perf_event_open({type=PERF_TYPE_SOFTWARE, size=PERF_ATTR_SIZE.Ver7, config=PERF_COUNT_SW_CPU_CLOCK, sample_period=0, sample_type=0, read_format=0, exclude_kernel=1, exclude_hv=1, precise_ip=0 /* arbitrary skid */, excludeguest=1, ..., -1, 0, -1, PERF_FLAG_FD_CLOEXEC) = 5
perf_event_open({type=PERF_TYPE_TRACEPOINT, size=PERF_ATTR_SIZE.Ver7, config=651, sample_period=1, sample_type=PERF_SAMPLE_IP|PERF_SAMPLE_TID|PERF_SAMPLE_TIME|PERF_SAMPLE_ID|PERF_SAMPLE_CPU|PERF_SAMPLE_PERIOD|PERF_SAMPLE_POWER, read_format=PERF.format_ID, disabled=1, inherit=1, precise_ip=0 /* arbitrary skid */, sample_id_all=1, excludeguest=1, ..., -1, 0, -1, PERF_FLAG_FD_CLOEXEC) = 5
...
perf_event_open({type=PERF_TYPE_SOFTWARE, size=PERF_ATTR_SIZE.Ver7, config=PERF_COUNT_SW_DUMMY, sample_freq=4000, sample_type=PERF_SAMPLE_IP|PERF_SAMPLE_TID|PERF_SAMPLE_TIME|PERF_SAMPLE_ID|PERF_SAMPLE_CPU|PERF_SAMPLE_PERIOD|PERF_SAMPLE_POWER, read_format=PERF-format_ID, inherit=1, mmap=1, comm=1, freq=1, task=1, precise_ip=0 /* arbitrary skid */, sample_id_all=1, mmap2=1, comm_exec=1, ksymbol=1, bpf_event=1, ..., -1, 0, -1, PERF_FLAG_FD_CLOEXEC) = 10
perf_event_open({type=PERF_TYPE_SOftware, size=PERF_ATTR_SIZE.Ver7, config=PERF_COUNT_SW_DUMMY, sample_freq=4000, sample_type=PERF_SAMPLE_IP|PERF_SAMPLE_TID|PERF_SAMPLE_TIME|PERF_SAMPLE_ID|PERF_SAMPLE_CPU|PERF_SAMPLE_PERIOD|PERF_SAMPLE_POWER, read_format=PERF-format_ID, inherit=1, mmap=1, comm=1, freq=1, task=1, precise_ip=0 /* arbitraryskid */, sample_id_all=1, mmap2=1, comm_exec=1, ksymbol=1, bpf_event=1, ..., -1, 2, -1, PERF_FLAG_FD_CLOEXEC) = 12 
```

openat打开的重要文件：

```txt
openat(AT_FDCWD, "/sys/bus/event_source/devices/", O_RDONLY|O_NONBLOCK|O_CLOEXEC|O_DIRECTORY) = 17 
```

```txt
openat(AT_FDCWD, "/sys/bus/event_source/devices/", O_RDONLY|O_NONBLOCK|O_CLOEXEC|O_DIRECTORY) = 17 
```

perf 打开 openat("/sys/bus/event_source/devices") 这样一个目录，这个目录是 PMU 在内核中的一个实例，这个目录下面列举各种事件类型，都是通过注册存放在这个目录下面的。

```shell
$ cd /sys/bus/event_source/devices
$ ls
breakpoint kprobe msr power software tracepointuprobe 
```

再回过头来看第一个eBPF程序中关于perf_event_open相关的代码，attach_tracing_event中通过perf_event_open系统调用注册了一个采样类型为PERF_TYPE_TRACEPOINT的事件，返回了一个fd文件描述符，通过perf_event产生的数据，最后返回给这个fd描述符，通过读取这个fd描述符，就可以获取监测的结果。

读者可以尝试使用strace工具跟踪本章第一个Python版本的eBPF程序。并观察其输出。命令如下。

```batch
sudo strace -e openat,perf_event_open hello_world.py 
```

# 4.3 eBPF授权协议

eBPF内核代码与常规的Linux内核代码紧密耦合，即它们具有相同的特权和地址空间。这意味着eBPF内核代码需要遵守适用的内核组件的许可限制。对eBPF而言，这些内核组件是辅助函数。而eBPF辅助函数允许调用内核函数。这些辅助函数可以是GPL或非GPL，并且它们必须明确定义其许可证。例如：

static const struct bpf_funcProto bpfprobe_read.proto $=$ { .func $=$ bpfprobe_read, .gpl_only $=$ true, .ret_type $=$ RET_integer, .arg1_type $=$ ARG_PTR_TO_UNINIT_MEM, .arg2_type $=$ ARGCONST_SIZE_OR_ZERO, .arg3_type $=$ ARG_ANYING, }；

在代码中，辅助函数的gpl_only设置成了true，这意味着，使用bpfprobe_read的任何eBPF代码也需要声明为GPL，声明代码如下所示。

```txt
char __license[](__attribute__(section("license"), used)) = "GPL"; 
```

在第一个eBPF的C语言代码中，在加载eBPF字节码时传递了GPL的授权协议字符串，因为eBPF字节码调用的bpf_trace_printk是一个GPL协议的辅助函数，因此在加载时需要指明GPL的

授权协议。

```c
int insn_cnt = sizeof(prog) / sizeof(struct bpf_insn);  
int prog_fd = bpf_prog_load(BPF_prog_TYPE_KPROBE, prog, insn_cnt, "GPL");  
if (prog_fd == -1) {  
    perror("Error calling bpf_prog_load());  
    return 1;  
} 
```

如果使用了GPL协议的辅助函数，却没有指定GPL，内核验证器将输出如下错误信息：

```txt
cannot call GPL-restricted function from non-GPL compatible program 
```

那么什么是GPL？GPL（GNU通用公共许可证）是一种开源软件许可证，它规定了使用、分发和修改该软件的条件。GPL授权协议要求任何基于GPL许可证的软件修改或衍生工作都必须以GPL许可证发布，这就意味着这些衍生工作也必须是开源的，并能够自由地被用户访问和使用。此外，任何使用或分发GPL软件的人必须向用户提供源代码和许可证，并且对这些代码的修改也必须公开发布。GPL旨在确保软件自由和共享，从而保护用户和程序员的权利。

# 4.4 eBPF指令集

eBPF指令集是RISC指令集，也就是常说的精简指令集。由于我们 $80\%$ 的时间都在用 $20\%$ 的简单指令，在RISC架构中，CPU选择把指令精简到 $20\%$ 的简单指令，而原先的复杂指令则可以通过简单指令的组合来实现。

# 4.4.1 eBPF寄存器

eBPF提供了一组特定的寄存器，用于存储和操作数据。目前eBPF规范设计了11个64位的寄存器，寄存器被命名为 $\mathrm{r0}\sim \mathrm{r10}$

寄存器的使用说明如表4-3所示。

表 4-3 eBPF 寄存器使用说明  

<table><tr><td>寄存器</td><td>使用说明</td></tr><tr><td>r0</td><td>函数调用的返回值，以及eBPF程序的退出值</td></tr><tr><td>r1～r5</td><td>函数调用的参数，一般r1寄存器指向程序的上下文</td></tr><tr><td>r6～r9</td><td>通用寄存器，函数调用时将保留这些寄存器</td></tr><tr><td>r10</td><td>只读寄存器，帧指针，用于访问栈</td></tr></table>

$r0\sim r5$ 是临时寄存器，如果eBPF程序希望在函数调用后寄存器值不变，需要自己保存和恢复

寄存器值。

# 4.4.2 eBPF指令编码

eBPF有以下两种指令编码模式。

- 基础编码：拥有64位的固定长度。  
- 宽指令编码：在基础编码后增加一个64位的立即数，这样指令的长度将增加到128位。

所有的指令编码方式相似，指令编码格式如表4-4所示。

表 4-4 eBPF 指令编码格式  

<table><tr><td>imm</td><td>offset</td><td>src_reg</td><td>dst_reg</td><td>opcode</td></tr><tr><td>32 位（MSB）</td><td>16 位</td><td>4 位</td><td>4 位</td><td>8 位（LSB）</td></tr></table>

- imm: 有符号立即数。  
- offset: 有符号偏移量。  
- src_reg：源寄存器，这里一般为（r0～r10）。  
- dst_reg: 目的寄存器。  
- opcode: 需要执行的操作码。

值得注意的是：

1）大多数指令不会使用所有的字段，如果没有使用，字段将会被设置为0。  
2）宽指令编码目前只有64位立即数指令在使用。

接下来看看opcode。opcode描述了具体的eBPF指令，占8位。opcode的低3位表示指令类型。当指令类型为LD/LDX/ST/STX时，操作码有如下结构：

msb 1sb $+ - - - + - - - +$ mde|sz|cls| $+ - - - + - - - +$ 3|2|3

其中，sz区域表示目标内存区域的大小，mde区域是内存访问模式，uBPF（用户态BPF）程序只支持通用MEM访问模式。

当指令类型为ALU/ALU64/JMP时，操作码结构如下：

```shell
msb 1sb +- -+-+ op |s|c1s| +- -+-+ 4 |1|3 
```

如果s是0，那么源操作数就是立即数；如果s是1，那么源操作数就是src。op部分指明要执

行哪一个ALU或者分支操作。

这些指令类型的描述如表4-5所示。

表 4-5 指令类型  

<table><tr><td>指令类型</td><td>值</td><td>描述</td><td>引用</td></tr><tr><td>BPF_LD</td><td>0x00</td><td>只用于宽指令,从imm64中加载数据到寄存器</td><td>load和store指令</td></tr><tr><td>BPF_LDX</td><td>0x01</td><td>从内存中加载数据到dst_reg</td><td>load和store指令</td></tr><tr><td>BPF_ST</td><td>0x02</td><td>把imm32数据保存到内存中</td><td>load和store指令</td></tr><tr><td>BPF_STX</td><td>0x03</td><td>把src_reg寄存器中的数据保存到内存</td><td>load和store指令</td></tr><tr><td>BPF_ALU</td><td>0x04</td><td>32位算术运算</td><td>算术和跳转指令</td></tr><tr><td>BPF_JMP</td><td>0x05</td><td>64位跳转操作</td><td>算术和跳转指令</td></tr><tr><td>BPF_JMP32</td><td>0x06</td><td>32位跳转操作</td><td>算术和跳转指令</td></tr><tr><td>BPF_ALU64</td><td>0x07</td><td>64位算术运算</td><td>算术和跳转指令</td></tr></table>

# 4.4.3 指令列表

eBPF支持的指令不多，主要分为算术指令、交换指令、内存指令和分支指令4类。

# 1. 算术指令

算术指令主要用于算术运算，比如常见的加、减、乘、除、模。它又可以细分为64位与32位运算的版本。

64位算术指令如表4-6所示

表 4-6 64 位算术指令  

<table><tr><td>操作码</td><td>助记符</td><td>伪代码</td></tr><tr><td>0x07</td><td>add dst, imm</td><td>dst += imm</td></tr><tr><td>0x0f</td><td>add dst, src</td><td>dst += src</td></tr><tr><td>0x17</td><td>sub dst, imm</td><td>dst -= imm</td></tr><tr><td>0x1f</td><td>sub dst, src</td><td>dst -= src</td></tr><tr><td>0x27</td><td>mul dst, imm</td><td>dst *= imm</td></tr><tr><td>0x2f</td><td>mul dst, src</td><td>dst *= src</td></tr><tr><td>0x37</td><td>div dst, imm</td><td>dst /= imm</td></tr><tr><td>0x3f</td><td>div dst, src</td><td>dst /= src</td></tr><tr><td>0x47</td><td>or dst, imm</td><td>dst |= imm</td></tr><tr><td>0x4f</td><td>or dst, src</td><td>dst |= src</td></tr><tr><td>0x57</td><td>and dst, imm</td><td>dst &amp;= imm</td></tr><tr><td>0x5f</td><td>and dst, src</td><td>dst &amp;= src</td></tr><tr><td>0x67</td><td>lsh dst, imm</td><td>dst &lt;= imm</td></tr><tr><td>0x6f</td><td>lsh dst, src</td><td>dst &lt;= src</td></tr><tr><td>0x77</td><td>rsh dst, imm</td><td>dst &gt;= imm (logical)</td></tr><tr><td>0x7f</td><td>rsh dst, src</td><td>dst &gt;= src (logical)</td></tr><tr><td>0x87</td><td>neg dst</td><td>dst = -dst</td></tr><tr><td>0x97</td><td>mod dst, imm</td><td>dst %=imm</td></tr><tr><td>0x9f</td><td>mod dst, src</td><td>dst %=src</td></tr><tr><td>0xa7</td><td>xor dst, imm</td><td>dst ^=imm</td></tr><tr><td>0xaf</td><td>xor dst, src</td><td>dst ^=src</td></tr><tr><td>0xb7</td><td>mov dst, imm</td><td>dst = imm</td></tr><tr><td>0xbf</td><td>mov dst, src</td><td>dst = src</td></tr><tr><td>0xc7</td><td>arsh dst, imm</td><td>dst &gt;= imm (arithmetic)</td></tr><tr><td>0xcf</td><td>arsh dst, src</td><td>dst &gt;= src (arithmetic)</td></tr></table>

32位算术指令如表4-7所示。这些指令仅使用操作数的低32位，并将目标寄存器的高32位清零。

表 4-7 32 位算术指令  

<table><tr><td>操作码</td><td>助记符</td><td>伪代码</td></tr><tr><td>0x04</td><td>add32 dst, imm</td><td>dst += imm</td></tr><tr><td>0x0c</td><td>add32 dst, src</td><td>dst += src</td></tr><tr><td>0x14</td><td>sub32 dst, imm</td><td>dst -= imm</td></tr><tr><td>0x1c</td><td>sub32 dst, src</td><td>dst -= src</td></tr><tr><td>0x24</td><td>mul32 dst, imm</td><td>dst *= imm</td></tr><tr><td>0x2c</td><td>mul32 dst, src</td><td>dst *= src</td></tr><tr><td>0x34</td><td>div32 dst, imm</td><td>dst /= imm</td></tr><tr><td>0x3c</td><td>div32 dst, src</td><td>dst /= src</td></tr><tr><td>0x44</td><td>or32 dst, imm</td><td>dst |= imm</td></tr><tr><td>0x4c</td><td>or32 dst, src</td><td>dst |= src</td></tr><tr><td>0x54</td><td>and32 dst, imm</td><td>dst &amp;= imm</td></tr><tr><td>0x5c</td><td>and32 dst, src</td><td>dst &amp;= src</td></tr><tr><td>0x64</td><td>lsh32 dst, imm</td><td>dst &lt;&lt;= imm</td></tr><tr><td>0x6c</td><td>lsh32 dst, src</td><td>dst &lt;&lt;= src</td></tr><tr><td>0x74</td><td>rsh32 dst, imm</td><td>dst &gt;&gt;&gt;= imm (logical)</td></tr><tr><td>0x7c</td><td>rsh32 dst, src</td><td>dst &gt;&gt;&gt;= src (logical)</td></tr><tr><td>0x84</td><td>neg32 dst</td><td>dst = -dst</td></tr><tr><td>0x94</td><td>mod32 dst, imm</td><td>dst %= imm</td></tr><tr><td>0x9c</td><td>mod32 dst, src</td><td>dst %= src</td></tr><tr><td>0xa4</td><td>xor32 dst, imm</td><td>dst ^=imm</td></tr><tr><td>0xac</td><td>xor32 dst, src</td><td>dst ^=src</td></tr><tr><td>0xb4</td><td>mov32 dst, imm</td><td>dst = imm</td></tr><tr><td>0xbc</td><td>mov32 dst, src</td><td>dst = src</td></tr><tr><td>0xc4</td><td>arsh32 dst, imm</td><td>dst &gt;&gt;&gt;=imm (arithmetic)</td></tr><tr><td>0xcc</td><td>arsh32 dst, src</td><td>dst &gt;&gt;&gt;=src (arithmetic)</td></tr></table>

# 2. 交换指令

交换指令用于操作数与源操作数之间的数据交换，比如两个寄存器的值交换、对寄存器值取半或扩展等。交换指令的操作码与对应伪代码如表4-8所示。

表4-8 交换指令  

<table><tr><td>操作码</td><td>助记符</td><td>伪代码</td></tr><tr><td>0xd4 (imm = 16)</td><td>le16 dst</td><td>dst =hte16.dst)</td></tr><tr><td>0xd4 (imm = 32)</td><td>le32 dst</td><td>dst =hte32.dst)</td></tr><tr><td>0xd4 (imm = 64)</td><td>le64 dst</td><td>dst =hte64.dst)</td></tr><tr><td>0xdc (imm = 16)</td><td>be16 dst</td><td>dst =hte16.dst)</td></tr><tr><td>0xdc (imm = 32)</td><td>be32 dst</td><td>dst =hte32.dst)</td></tr><tr><td>0xdc (imm = 64)</td><td>be64 dst</td><td>dst =hte64.dst)</td></tr></table>

# 3. 内存指令

内存指令完成数据的读写操作。比如读取内存的数据到寄存器，写寄存器数据到内存。内存指令的操作码与对应伪代码如表4-9所示。

表 4-9 内存指令  

<table><tr><td>操作码</td><td>助记符</td><td>伪代码</td></tr><tr><td>0x18</td><td>lddw dst, imm</td><td>dst = imm</td></tr><tr><td>0x20</td><td>ldabsw src, dst, imm</td><td rowspan="8">见内核文档</td></tr><tr><td>0x28</td><td>ldabsh src, dst, imm</td></tr><tr><td>0x30</td><td>ldabsb src, dst, imm</td></tr><tr><td>0x38</td><td>ldabsdw src, dst, imm</td></tr><tr><td>0x40</td><td>ldindw src, dst, imm</td></tr><tr><td>0x48</td><td>ldindh src, dst, imm</td></tr><tr><td>0x50</td><td>ldindb src, dst, imm</td></tr><tr><td>0x58</td><td>ldinddw src, dst, imm</td></tr><tr><td>0x61</td><td>ldxw dst, [src+off]</td><td>dst = *(uint32_t*) (src + off)</td></tr><tr><td>0x69</td><td>ldxh dst, [src+off]</td><td>dst = *(uint16_t*) (src + off)</td></tr><tr><td>0x71</td><td>ldxb dst, [src+off]</td><td>dst = *(uint8_t*) (src + off)</td></tr><tr><td>0x79</td><td>ldxdw dst, [src+off]</td><td>dst = *(uint64_t*) (src + off)</td></tr><tr><td>0x62</td><td>stw [dst+off], imm</td><td>*(uint32_t*) (dst + off) = imm</td></tr><tr><td>0x6a</td><td>sth [dst+off], imm</td><td>*(uint16_t*) (dst + off) = imm</td></tr><tr><td>0x72</td><td>stb [dst+off], imm</td><td>*(uint8_t*) (dst + off) = imm</td></tr><tr><td>0x7a</td><td>stdw [dst+off], imm</td><td>*(uint64_t*) (dst + off) = imm</td></tr><tr><td>0x63</td><td>stxw [dst+off], src</td><td>*(uint32_t*) (dst + off) = src</td></tr><tr><td>0x6b</td><td>stxh [dst+off], src</td><td>*(uint16_t*) (dst + off) = src</td></tr><tr><td>0x73</td><td>stxb [dst+off], src</td><td>*(uint8_t*) (dst + off) = src</td></tr><tr><td>0x7b</td><td>stxdw [dst+off], src</td><td>*(uint64_t*) (dst + off) = src</td></tr></table>

# 4. 分支指令

分支指令根据上一条指令的结果，判断执行指令时是否需要跳转。分支指令的操作码与对应伪代码如表4-10所示。

表 4-10 分支指令  

<table><tr><td>操作码</td><td>助记符</td><td>伪代码</td></tr><tr><td>0x05</td><td>ja +off</td><td>PC += off</td></tr><tr><td>0x15</td><td>jeq dst, imm, +off</td><td>PC += off if dst == imm</td></tr><tr><td>0x1d</td><td>jeq dst, src, +off</td><td>PC += off if dst == src</td></tr><tr><td>0x25</td><td>jgt dst, imm, +off</td><td>PC += off if dst &gt; imm</td></tr><tr><td>0x2d</td><td>jgt dst, src, +off</td><td>PC += off if dst &gt; src</td></tr><tr><td>0x35</td><td>jge dst, imm, +off</td><td>PC += off if dst &gt;= imm</td></tr><tr><td>0x3d</td><td>jge dst, src, +off</td><td>PC += off if dst &gt;= src</td></tr><tr><td>0xa5</td><td>jlt dst, imm, +off</td><td>PC += off if dst &lt; imm</td></tr><tr><td>0xad</td><td>jlt dst, src, +off</td><td>PC += off if dst &lt; src</td></tr><tr><td>0xb5</td><td>jle dst, imm, +off</td><td>PC += off if dst &lt;= imm</td></tr><tr><td>0xbd</td><td>jle dst, src, +off</td><td>PC += off if dst &lt;= src</td></tr><tr><td>0x45</td><td>jset dst, imm, +off</td><td>PC += off if dst &amp; imm</td></tr><tr><td>0x4d</td><td>jset dst, src, +off</td><td>PC += off if dst &amp; src</td></tr><tr><td>0x55</td><td>jne dst, imm, +off</td><td>PC += off if dst != imm</td></tr><tr><td>0x5d</td><td>jne dst, src, +off</td><td>PC += off if dst != src</td></tr><tr><td>0x65</td><td>jsgt dst, imm, +off</td><td>PC += off if dst &gt; imm (signed)</td></tr><tr><td>0x6d</td><td>jsgt dst, src, +off</td><td>PC += off if dst &gt; src (signed)</td></tr><tr><td>0x75</td><td>jsge dst, imm, +off</td><td>PC += off if dst &gt;= imm (signed)</td></tr><tr><td>0x7d</td><td>jsge dst, src, +off</td><td>PC += off if dst &gt;= src (signed)</td></tr><tr><td>0xc5</td><td>jslt dst, imm, +off</td><td>PC += off if dst &lt; imm (signed)</td></tr><tr><td>0xCD</td><td>jslt dst, src, +off</td><td>PC += off if dst &lt; src (signed)</td></tr><tr><td>0xd5</td><td>jsle dst, imm, +off</td><td>PC += off if dst &lt;= imm (signed)</td></tr><tr><td>0xDD</td><td>jsle dst, src, +off</td><td>PC += off if dst &lt;= src (signed)</td></tr><tr><td>0x85</td><td>call imm</td><td>Function call</td></tr><tr><td>0x95</td><td>exit</td><td>return r0</td></tr></table>

关于eBPF指令集的更多信息可以访问Linux内核文档（https://docs.kernel.org/bpf/instructionset.html），或者IO Visor的BPF文档（https://github.com/iovisor/bpf-docs/blob/master/eBPF.md）。

# 4.4.4 eBPF指令分析

在内核源码中，每个指令使用bpf_insn结构体进行描述。

```c
//https://github.com/torvalds/linux/blob/v5.15/include/uapi/linux/bpf.h
struct bpf_insn {
    __u8     code; /* 操作码 */
    __u8     dst_reg:4; /* 目标寄存器 */
    __u8     src_reg:4; /* 源寄存器 */
    __s16     off; /* 有符号偏移 */
    __s32     imm; /* 有符号立即数 */
};
```

为了方便使用，在include/linux/filter.h中使用宏定义的方式初始化了bpf_insn数组，例如如下BPF_STX_MEM的定义。

```txt
define BPF_STX_MEM(SIZE, DST, SRC, OFF)  
((struct bpf_insn) {  
    .code = BPF_STX | BPF_SIZE(SIZE) | BPF_MEM,  
    .dst_reg = DST,  
    .src_reg = SRC,  
    .off = OFF,  
    .imm = 0} 
```

我们以第一个eBPF指令为案例，分析每条指令的含义。

```c
struct bpf_insnp prog[] = {
    BPF_MOV64_IMM(BPF_REG_1, 0xa21), /* ':!\n' */
}; 
```

```c
BPF_STX_MEM(BPF_H, BPF_REG_10, BPF_REG_1, -4),  
BPF_MOV64-imm(BPF_REG_1, 0x646c726f), /* 'orld' */  
BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -8),  
BPF_MOV64-imm(BPF_REG_1, 0x57202c6f), /* 'o, W' */  
BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -12),  
BPF_MOV64-imm(BPF_REG_1, 0x6c6c6548), /* 'Hell' */  
BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -16),  
BPF_MOV64-imm(BPF_REG_1, 0),  
BPF_STX_MEM(BPF_B, BPF_REG_10, BPF_REG_1, -2),  
BPF_MOV64_REG(BPF_REG_1, BPF_REG_10),  
BPF_ALU64-imm(BPF_ADD, BPF_REG_1, -16),  
BPF_MOV64-imm(BPF_REG_2, 15),  
BPF_raw_insN(BPF_JMP | BPF_CALL, 0, 0, 0, BPF FUNC_trace_PRINT),  
BPF_MOV64-imm(BPF_REG_0, 0),  
BPF_EXIT_insN(), 
```

BPF_MOV64_IMM将立即数存储到对应的寄存器，第1行代码BPF_MOV64_IMM(BPF_REG_1, 0xa21)将“!n”的ASCII数值写入r1寄存器中，接着使用BPF_STX_MEM(BPF_H, BPF_REG_10, BPF_REG_1, -4)将r1的值存储到r10-4的位置，以此类推将“Hello, World!\n”字符串写入栈中，随后将字符串的指针和字符串长度赋值给r1和r2寄存器，作为参数调用了BPFFunc_trace_PRINTk辅助函数。这个只是一个调用号，调用号为6。这个辅助函数的原型如下：

```c
//https://github.com/torvalds/linux/blob/v5.15/include/uapi/linux/bpf.h //long bpf_trace_printk(const char \*fmt,u32 fmt_size，...）; #define_BPF FUNC Mapper(FN)\\ FN(unsigned),FN(mapLOOKUP_element),FN(map_update_element),FN(map_delete_element),FN(probe_read),FN(ktime_get_ns),FN(trace_PRINTk),FN(get_prandom_u32),FN(get_smp Processor_id),FN(skbstore_bytes),FN(13_csum_replace),… 
```

最后给r0寄存器赋值，r0作为返回值，调用BPF_EXIT_INSN程序返回。

# 4.4.5 BCC中eBPF程序指令的生成

在第一个eBPF程序中，使用宏汇编指令来编写eBPF程序无疑非常麻烦。那么有没有办法可

以自动生成eBPF指令呢？答案是“有”。

我们可以使用Python来生成指令。

1）让第一个eBPF的Python程序在Python的shell环境中执行，然后使用BPF类中的dum_func的成员方法将其转储为eBPF程序的十六进制代码。

```txt
$ sudo python3
>>> from bcc import BPF
>>> b = BPF(text='int kprobe_sysClone(void *ctx) { bpf_trace_printk("Hello, World! \
\n"); return 0; }')
>>> b.dump_func('kprobe_sysClone').hex()
'b7010000210a00006b1afcff00000000b70100006f726c64631af8ff000000001801000048656c6c000
000006f2c20577b1af0ff0000000b7010000000000731afeeff00000000bfa1000000000000701000
0fFFFFFFb70200000f0000085000000600000b7000000000009500000000000' 
```

2）复制上面的Hex字符串（不要复制单引号），然后打开010Editor，新建一个Hex文件，在工具栏选择Edit->PasteFrom->PastefromHexText，执行效果如图4-1所示。

![](images/3da22d494e5d29f5d1884a493d6db95984b6364be2849ac4a5cba5860342f986.jpg)  
图4-1 010Editor新建文件

3）全选所有内容，选择Edit $\rightharpoondown$ Copyas $\rightharpoonup$ CopyasCCode，这样就得到了一个C语言定义的char数组。

```csv
unsigned char hexData[120] = {  
0x87, 0x01, 0x00, 0x00, 0x21, 0x0A, 0x00, 0x6B, 0x1A, 0xFC, 0xFF, 0x00, 0x00,  
0x00, 0x00,  
0x87, 0x01, 0x00, 0x6F, 0x72, 0x6C, 0x64, 0x63, 0x1A, 0xF8, 0xFF, 0x00, 0x00,  
0x00, 0x00,  
0x18, 0x01, 0x00, 0x48, 0x65, 0x6C, 0x6C, 0x1A, 0xF8, 0xFF, 0x12C,  
0x20, 0x57,  
0x7B, 0x1A, 0xF0, 0xFF, 0x00, 0x1A, 0xF8, 0x1A, 0xF8, 0x1A, 0xF8, 0x1A,  
0x1A, 0xF8, 0xFF, 0x1A, 0xF8, 0xA1, 0xF8, 0xA1, 0xF8, 0xA1,  
0x1A, 0xF8, 0xFF, 0x1A, 0xF8, 0xB7, 0xA1, 0xF8, 0xA1,  
0x1A, 0xF8, 0xFF, 0x1A, 0xF8, 0xB7, 0xA1, 0xF8, 0xA1,  
0x1A, 0xF8, 0xFF, 0x1A, 0xF8, 0xB7, 0xA1, 1A1, 1A1,  
1A1, 1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1,  
1A1, 
```

4）使用这个数组替换第一个eBPF程序，并观察执行后的效果。两者的输出是一样的，都是“Hello World”字符串。

# 4.4.6 eBPF指令反汇编

本小节主要介绍与eBPF汇编指令相关的工具，以及如何进行反汇编。

Linux 内核源码树目录下面有很多与 eBPF 指令相关的源码，这些.c 源码文件都会编译成独立的工具使用。可以单独编译这些工具进行使用，编译完毕后，会在目录下生成 3 个可执行文件 bpf_asm、bpfDBG 和 bpf_jit_disasm。

```shell
$ cd /usr/src/linux-source-5.19.0/linux-source-5.19.0/tools/bpf
$ ls
bpf_asm.c bpfDBG.c bpf_exp.l bpf_exp.y bpf jot_disasm.c bpftool Makefile
resolve_btfids runqslower
$ make -j8
$ ls
bpf_asm bpfDBG bpf_exp.l bpf_exp.y bpf_exp.yacc.o bpf jot_ _
disasm.o Makefile
bpf_asm.c bpfDBG.c bpf_exp.lex.c bpf_exp.yacc.c bpf jot_disasm bpfftool
resolve_btfids
bpf_asm.o bpfDBG.o bpf_exp.lex.o bpf_exp.yacc.h bpf jot_disasm.c FEATURE-DUMP.
bpf runqslower 
```

# 1. bpf_asm

这是一个eBPF汇编器，可以将eBPF汇编程序转换为eBPF字节码。bpf_asm.c的代码非常短，里面调用了bpf_asm.compile接口来完成这个转换过程。

include<stdio.h>   
#include<stdio.h>   
#include<string.h>   
extern void bpf_asm.compile(FILE *fp，bool cstyle);   
int main(int argc，char \*\*argv){ FILE \*fp $=$ stdin; bool cstyle $=$ false; int i; for $(\mathrm{i} = 1$ ;i $<$ argc； $\mathrm{i + + })$ { //-c参数将BPF汇编代码编译成C格式 if(!strcmp(-c"，argv[i]，2)){ style $=$ true;

continue;   
}   
fp $=$ fopen(argv[i]，"r"); if(!fp){ fp $\equiv$ stdin; continue; 1 break;   
} bpf_asm.compile(fp，cstyle); return0;

可以编写一小段汇编代码，保存为test.s。

```txt
ldh [12]；将skb第12、13字节处的内容加载到寄存器  
jne #0x806，drop；如果寄存器的值不等于0x806，则跳转到drop  
ret #-1  
drop: ret #0
```

然后使用bpf_asm生成opcode。

```csv
$ ./bpf_asm test.s
4,40 0 0 12,21 0 1 2054,6 0 0 4294967295,6 0 0 0, 
```

```txt
# 也可以加上-c命令,输出类似c格式
$ ./bpf_asm -c test.s
{ 0x28, 0, 0, 0x0000000c},
{ 0x15, 0, 1, 0x00000806},
{ 0x06, 0, 0, 0xxxxxx},
{ 0x06, 0, 0, 000000000} 
```

# 2. bpfDBG

这是eBPF程序的默认调试器，提供了eBPF指令的加载、运行和调试功能，里面实现了cBPF的反汇编器。如果需要编写一个反汇编器，可以参考里面的源码。

bpfDBG支持的命令如表4-11所示。

表 4-11 bpfDBG 支持的命令  

<table><tr><td>命令</td><td>说明</td></tr><tr><td>load bpf</td><td>加载 bpf_asm 的输出文件</td></tr><tr><td>load pcap</td><td>加载 tcpdump -ddd 的输出文件</td></tr><tr><td>run</td><td>运行加载 eBPF 程序</td></tr><tr><td>run []</td><td>对 pcap 内的前 n 个包执行过滤</td></tr><tr><td>disassemble</td><td>反汇编当前加载的 eBPF 程序</td></tr><tr><td>dump</td><td>以类 C 风格打印加载的 eBPF 程序</td></tr><tr><td>breakpoint</td><td>断点, 可以对当前加载的指定代码行下断点</td></tr><tr><td>step</td><td>从当前 pc offset 开始, 单步执行</td></tr><tr><td>select</td><td>选择从第 n 个包开始执行</td></tr><tr><td>quit</td><td>退出调试器</td></tr></table>

以上面提到的bpf_asm生成的opcode为例进行实践。

```txt
$ ./bpfDBG
> load bpf 4,40 0 0 12,21 0 1 2054,6 0 0 4294967295,6 0 0 0
> disassemble
10: ldh [12]
11: jeq #0x806, 12, 13
12: ret #0xxxxxxxx
13: ret #0
> breakpoint 1
breakpoint at: 11: jeq #0x806, 12, 13
> quit 
```

# 3. bpf_jit_disasm

Linux内核内置了一个BPFJIT编译器，支持x86_64、SPARC、PowerPC、ARM、ARM64、MIPS、RISC-V和s390。编译内核时需要打开CONFIG_BPF_JIT。

执行如下命令启用bpf_jit_enable。

```txt
$ echo 1 > /proc/sys/net/core/bpf_jit_enable 
```

或者使用sysctl。

```txt
sysctl net.core.bpf_jit_enable=1 
```

如果想每次编译过滤器时都将生成的opcode镜像打印到内核日志中，可进行如下设置。设置成功之后，可以通过dmesg查看JIT输出。

```txt
$ echo 2 > /proc/sys/net/core/bpf_jit_enable 
```

或者使用sysctl。

```txt
sysctl net.core.bpf_jit_enable=2 
```

值得注意的是：在编译期间，如果设置了CONFIG_BPF_JIT_ALWAYS_ON，bpf_jit_enable就会永久性地设为1，再设置成其他值时就会报错。

```txt
$ sysctl net.core.bpf_jit_enable=2 
```

```python
sysctl: setting key "net.core.bpf_jit_enable": Invalid argument 
```

一般默认开启了CONFIG_BPF_JIT_ALWAYS_ON，因为并不推荐将最终的JIT镜像打印到内核日志。这里可以通过重新编译替换内核的方式关闭这个选项，不过通常推荐通过bpftool tools/bpf/bpftool来查看镜像内容，这样不用重新编译内核即可获取JIT反汇编。

# 4. bpftool反汇编

可以通过 bpftool 查看当前运行程序的 JIT 反汇编代码。运行第一个 C 语言版本的 eBPF 程序的命令如下：

```txt
$ sudo ./hello_world 
```

```txt
dconf-service-2732 [003] d..31 27574.145245: bpf_trace_printk: Hello, World! 
```

```txt
dconf-service-2732 [002] d..31 27574.148024: bpf_trace_printk: Hello, World! 
```

可以通过 bpftool 命令查看当前所有运行的 eBPF 程序，其中包含了很多 cgroup_SKb 类型的 eBPF 程序，这些是系统默认的。我们注册的是 kprobe 类型，可以看到 ID 为 183。

```txt
$ sudo bpftool prog list 
```

```txt
# 
```

```txt
174: cgroup_skb tag 6deef7357e7b4530 gpl  
loaded_at 2023-04-22T12:18:25+0800 uid 0  
xlated 64B jited 55B memlock 4096B 
```

```txt
183: kprobe tag 9abf0e9561523153 gpl  
loaded_at 2023-04-22T19:23:53+0800 uid 0  
xlated 128B jited 79B memlock 4096B 
```

通过bpftool的progdump子命令传入上面的ID，即可查看编译的反汇编内容。

```txt
$ sudo bpftool prog dump xlated id 183
0: (b7) r1 = 2593
1: (6b) * (u16 *) (r10 -4) = r1
2: (b7) r1 = 1684828783
3: (63) * (u32 *) (r10 -8) = r1
4: (b7) r1 = 1461726319
5: (63) * (u32 *) (r10 -12) = r1
6: (b7) r1 = 1819043144
7: (63) * (u32 *) (r10 -16) = r1
8: (b7) r1 = 0 
```

9: (73) $\star (\mathrm{u8}^{\star})(\mathrm{r10 - 2}) = \mathrm{r1}$ 10: (bf) $r1 = r10$ 11: (07) $r1 += -16$ 12: (b7) $r2 = 15$ 13: (85) call bpf_trace_printk#-70832  
14: (b7) $r0 = 0$ 15: (95) exit

最后，还可以查看上面由JIT翻译的本机代码。运行如下代码，可以看到eBPF指令被翻译成Intel x64汇编代码了。

```asm
sudo apt install libbfd-dev
cd /usr/src/linux-source-5.19.0/linux-source-5.19.0/tools/bpf/bpftool
sudo ./bpftool prog dump jited id 43
bpf_prog_9abf0e9561523153:
0: nop1 0x0(%rax, %rax, 1)
5: xchg %ax, %ax
7: push %rbp
8: mov %rsp, %rbp
b: sub $0x10, %rsp
12: mov $0xa21, %edi
17: mov %di, -0x4 (%rbp)
1b: mov $0x646c726f, %edi
20: mov %edi, -0x8 (%rbp)
23: mov $0x57202c6f, %edi
28: mov %edi, -0xc (%rbp)
2b: mov $0x6c6c6548, %edi
30: mov %edi, -0x10 (%rbp)
33: xor %edi, %edi
35: mov %dil, -0x2 (%rbp)
39: mov %rbp, %rdi
3c: add $0xxxxxxxxxxxxxx0, %rdi
40: mov $0xf, %esi
45: call 0xFFFFFFddab5d22c
4a: xor %eax, %eax
4c: leave
4d: ret
4e: int3 
```

# 5. llvm-objectdump 反汇编

还可以利用llvm-objectdump对编译生成的内容进行反汇编。

```txt
$ cd ~/.libbbpf-bootstrap/examples/c $ llvm-objectdump -d minimal.bpf.o 
```

# 4.4.7 eBPF验证机制

基于前面的内容我们知道，eBPF程序编译完毕后，通过BPF系统调用将eBPF字节码载入内核中执行。执行eBPF程序需要root权限或者通过Linux capability把CAP_BPF的特权赋予普通进程。CAP_BPF可以让非root程序使用BPF_PROG_TYPE_SOCKET_FILTER类型的eBPF程序，可以通过setcap给某个程序添加特定的capability实现。如果没有特定权限，系统会报错“Permission denied”。

```txt
sudo setcap cap_bpf + eip [ebpf_path] 
```

除此之外，eBPF程序要嵌入内核中执行，因此eBPF程序的安全性是非常重要的，需要避免因为eBPF程序的错误导致内核崩溃或卡死。所以每个载入内核的eBPF程序都需要经过eBPF验证器（Verifier）的检查，通过之后才能加载到内核。

1）eBPF会对授权协议进行检测，如果使用了GPL授权的函数却没有声明GPL，则会报“non-GPL licence”错误。

2）eBPF程序需要在有限的时间内完成，不然可能导致内核卡死。早期版本的验证器是拒绝任何形式的循环存在的，整个程序必须是一个DAG（有向无环图）。而在Linux内核5.3版本以后，验证器允许有限次数的循环，它会通过模拟执行eBPF程序来判断是否在有限次循环后执行到了返回处（bpf_exit）。

3）对eBPF程序的大小也有一定的限制。早期的eBPF程序只允许4096个eBPF指令，当实现比较复杂的eBPF程序时这显然是不够用的。在Linux内核5.2版本以后，这个限制改为100万条指令。

4）对eBPF程序的栈也有大小限制，目前限制的大小是512KB。

5）验证器会校验辅助函数的参数是否合法，并校验寄存器的合法性，以及是否存在无效的使用方式和回传数值，是否非法存取修改数据，对无效的instruction参数等也会进行检查，并拒绝存在无法执行的指令。

通过验证器的检查后，eBPF程序会被送到JIT编译器做二次编译，编译成本地代码并执行。

eBPF 验证器校验主要分为以下两个步骤。

1）进行DAG检查和其他CFG验证，以免程序中出现循环和无法访问的指令。  
2）检查所有可能的路径，在每个路径上模拟执行指令的过程，并观察寄存器和堆栈的状态变化。

这些验证操作可以确保程序不会引起安全问题。

下面通过案例来模拟不同的错误场景。

首先改造第一个eBPF程序，将之前授权协议的GPL字符串填空。同时准备了7种会验证失败的情况，可以通过Clang编译执行后观测结果。

```c
// clang verifier.c -o verifier -static  
#include <stdio.h>  
#include <stdlib.h> 
```

include<stdio.h> #include<string.h> #include<unistd.h> #include<sys/types.h> #include<sys/ip送.h> #include<sys/ip送.h> #include<sys/ip送.h> #include<sys/ip送.h> #include<sys/ip送.h> #include<sys/ip送.h> #include<sys/ip送.h> ##include<sys/ip送.h> //#include <bpf/bpf.h> #include <bcc/libbpf.h> int socks[2] $= \{-1\}$ int bpf(int cmd,union bpf_attr \*attr){ return syscall(_NR_bpf,cmd,attr,sizeof(\*attr)); } int bpf_prog_load(union bpf_attr \*attr){ return bpf(BPF_prog_LOAD,attr); } union bpf_attr\* create_bpf_prog(struct bpf_insn \*insns，unsigned int insn_cnt){ union bpf_attr \*attr $=$ (union bpf_attr \*) malloc(sizeof(union bpf_attr)); attr->prog_type $=$ BPFProg_TYPE_SOCKET_FILTER; attr->insn_cnt $=$ insn_cnt; attr->insns $=$ (uint64_t) insns; attr->license $=$ (uint64_t)""; return attr; } int attach_SOCKET(int prog_fd){ if(socks[0] $= = -1$ && socketpair(AF_UNIX,SOCK_DGRAM,0,socks) $<  0$ ){ perror("socketpair"); exit(1); } if(setsockoptARDS[O],SOL_SOCKET,SO_ATTACH_BPF,&prog_fd,sizeof(prog_fd） $<  0$ { perror("setsockopt"); exit(1); } return 0; } void setup_bpf_prog(struct bpf_insn \*insns，uint insncnt){

char log_buffer[0x1000];   
union bpf_attr \*prog $=$ create_bpf_prog(insns, insncnt); prog->log_level $= 2$ . prog->log_buf $=$ (uint64_t) log_buffer; prog->log_size $=$ sizeof(log_buffer); strncpy(prog->prog_name,"stderr",16); int prog_fd $=$ bpf_prog_load(prog); printf("%ld\n", strlen(log_buffer)); puts(log_buffer); if(prog_fd $<  0$ { perror("prog_load"); exit(1); } attach_SOCKET(prog_fd);   
void run_bpf_prog(struct bpf_insn \*insns, uint insncnt){ int val $= 0$ . setup_bpf_prog(insns, insncnt); write(socks[1],&val,sizeof(val));   
}   
static __always_inline int bpf_syscall(int cmd,union bpf_attr \*attr, unsigned int size) { return syscall(_NR_bpf, cmd, attr, size);   
}   
static __alwaysINLINE int bpf_create_map(index bpf_map_type map_type, unsigned int key_size, unsigned int value_size, unsigned int max_entries) { union bpf_attr Attr = { .map_type $=$ map_type, .key_size $=$ key_size, .value_size $=$ value_size, .max_entries $=$ max_entries, };

return bpf_syscall(BPF_MAP_CREATE, &attr, sizeof Attr);   
}   
int main(){ struct bpf_insn insns[] $=$ { BPF_MOV64_IMM(BPF_REG_1,0xa21), /\*!!\n'\*/ BPF_STX_MEM(BPF_H,BPF_REG_10,BPF_REG_1,-4), BPF_MOV64_IMM(BPF_REG_1,0x646c726f), /\* 'orld' \*/ BPF_STX_MEM(BPF_W,BPF_REG_10,BPF_REG_1,-8), BPF_MOV64_IMM(BPF_REG_1,0x57202c6f), /\*o,W\*/ BPF_STX_MEM(BPF_W,BPF_REG_10,BPF_REG_1,-12), BPF_MOV64_IMM(BPF_REG_1,0x6c6c6548), /\* 'Hell' \*/ BPF_STX_MEM(BPF_W,BPF_REG_10,BPF_REG_1,-16), BPF_MOV64_IMM(BPF_REG_1,0), BPF_STX_MEM(BPF_B,BPF_REG_10,BPF_REG_1,-2), BPF_MOV64_REG(BPF_REG_1,BPF_REG_10), BPF_ALU64_IMM(BPF_ADD,BPF_REG_1,-16), BPF_MOV64_IMM(BPF_REG_2,15), BPF_raw_insn(BPF_JMP |BPF_CALL,0,0,0,BPF FUNC_trace_printk), BPF_MOV64_IMM(BPF_REG_0,0), BPF_EXIT_INSN(), }；   
//寄存器从来没有被写过，它是不可读的 struct bpf_insn insns2[] $=$ { BPF_MOV64_REG(BPF_REG_0,BPF_REG_2), BPF_EXIT_INSN(), }；   
//程序在访问map元素前没有检查map.lookup Elem()的返回值 struct bpf_insn insns3[] $=$ { BPF_ST_MEM(BPF_DW,BPF_REG_10,-8,0), BPF_MOV64_REG(BPF_REG_2,BPF_REG_10), BPF_ALU64_IMM(BPF_ADD,BPF_REG_2,-8), BPF_LD_MAP_FD(BPF_REG_1,0), BPF_raw_insn(BPF_JMP |BPF_CALL,0,0,0,BPF FUNC_map.Lookup Elem), BPF_ST_MEM(BPF_DW,BPF_REG_0,0,0), BPF_EXIT_INSN() };   
//程序退出前没有初始化r0 struct bpf_insn insns4[] $=$ { BPF_EXIT_INSN() };   
//r10是只读的

```c
struct bpf_insn insns5[] = { BPF_MOV64_REG(BPF_REG_10, BPF_REG_1), BPF_EXIT_INSN(), }; //最后一条指令必须是BPF_EXIT_INSN struct bpf_insn insns6[] = { BPF_MOV64_IMM(BPF_REG_0, 0), }; int ret = 0; ret = bpf_create_map(BPF_MAP_TYPE_ARRAY, sizeof uint32_t), getpagesize(), 1); if (ret < 0) { printf("Failed to create comm map: %d (%s)\n", ret, strlen(-ret)); return ret; } int comm_fd = ret; if ((ret = bpf_create_map(BPFMAP_TYPE_RINGBUF, 0, 0, getpagesize())) < 0) { printf("Could not create ringbuf map: %d (%s)\n", ret, strlen(-ret)); return ret; } int ringbuf_fd = ret; struct bpf_insn insns7[] = { // r9 = r1 BPF_MOV64_REG(BPF_REG_9, BPF_REG_1), // r0 = bpf.lookup Elem(ctx->comm_fd, 0) BPF_FD_MAP_FD(BPF_REG_1, comm_fd), BPF_ST_MEM(BPF_DW, BPF_REG_10, -8, 0), BPF_MOV64_REG(BPF_REG_2, BPF_REG_10), BPF_ALU64_IMM(BPF_ADD, BPF_REG_2, -4), BPF_raw_INSN(BPF_JMP | BPF_CALL, 0, 0, 0, BPF FUNC_mapLOOKUP Elem), // if (r0 == NULL) exit(1) BPF_JMP_IMM(BPF_JNE, BPF_REG_0, 0, 2), BPF_MOV64_IMM(BPF_REG_0, 1), BPF_EXIT_INSN(), // r8 = r0 BPF_MOV64_REG(BPF_REG_8, BPF_REG_0), // r0 = bpf_ringbuf_reserved(ctx->ringbuf_fd, PAGE_SIZE, 0) BPF_FD_MAP_FD(BPF_REG_1, ringbuf_fd), BPF_MOV64_IMM(BPF_REG_2, PAGE_SIZE), BPF_MOV64_IMM(BPF_REG_3, 0x00), BPF_raw_INSN(BPF_JMP | BPF_CALL, 0, 0, 0, BPF FUNC_ringbuf_reserved), 
```

BPF_MOV64_REG(BPF_REG_1，BPF_REG_0)，BPF_ALU64_IMM(BPF_ADD，BPF_REG_1，1)，//这里提示算术出错了//if $(\mathrm{r}0! = \mathrm{NULL})$ {ringbuf_discard(r0,1)；exit(2);}BPF_JMP_IMM(BPF_JEQ，BPF_REG_0，0，5)，BPF_MOV64_REG(BPF_REG_1，BPF_REG_0)，BPF_MOV64_IMM(BPF_REG_2，1)，BPF_raw_INSN(BPF_JMP|BPF_CALL，0，0，0,BPF FUNC_ringbuf_discard)，BPF_MOV64_IMM(BPF_REG_0，2)，BPF_EXIT_INSN()，// $r7 = r1 + 8$ BPF_MOV64_REG(BPF_REG_7，BPF_REG_1)，BPF_ALU64_IMM(BPF_ADD，BPF_REG_7，8)，BPF_EXIT_INSN()，};//run_bpf_prog(insns,sizeof(insns)/sizeof(insns[0]);//run_bpf_prog(insns2,sizeof(insns2)/sizeof(insns2[0]）);//run_bpf_prog(insns3,sizeof(insns2)/sizeof(insns3[0]）);//run_bpf_prog(insns4,sizeof(insns2)/sizeof(insns4[0]））;//run_bpf_prog(insns5,sizeof(insns2)/sizeof(insns5[0]））; //run_bpf_prog(insns6,sizeof(insns2)/sizeof(insns6[0]））;run_bpf_prog(insns7,sizeof(insns7)/sizeof(insns7[0]））;

1）授权协议认证失败。在eBPF使用了GPL授权协议的辅助函数后，需要指明当前程序使用了GPL协议，如果没有，将会报错。

```c
struct bpf_insn insns[] = {
    BPF_MOV64_IMM(BPF_REG_1, 0xa21), /* !\n*/
    BPF_STX_MEM(BPF_H, BPF_REG_10, BPF_REG_1, -4),
    BPF_MOV64_IMM(BPF_REG_1, 0x646c726f), /* 'orld' */
    BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -8),
    BPF_MOV64_IMM(BPF_REG_1, 0x57202c6f), /* 'o, W' */
    BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -12),
    BPF_MOV64_IMM(BPF_REG_1, 0x6c6c6548), /* 'Hell' */
    BPF_STX_MEM(BPF_W, BPF_REG_10, BPF_REG_1, -16),
    BPF_MOV64_IMM(BPF_REG_1, 0),
    BPF_STX_MEM(BPF_B, BPF_REG_10, BPF_REG_1, -2),
    BPF_MOV64_REG(BPF_REG_1, BPF_REG_10),
    BPF_ALU64_IMM(BPF_ADD, BPF_REG_1, -16),
    BPF_MOV64_IMM(BPF_REG_2, 15),
} 
```

```c
BPF_raw_insN(BPF_JMP | BPF_CALL, 0, 0, 0, BPF_FUNC_trace_printk),  
BPF_MOV64_IMM(BPF_REG_0, 0),  
BPF_EXIT_insN(), 
```

运行后得到如下错误输出，可以看到在第13行调用bpf_trace_PRINT处报错“cannot call GPL-restricted function from non-GPL compatible program”。随后prog_load提示“Invalid argument”错误，程序退出。报错的原因在注释中说明了，这里不重复说明。

```ini
$ sudo ./verifyer
[sudo] password for ubuntu:
1102
func#0 @0
0: R1=ctx(off=0,imm=0) R10=fp0
0: (b7) r1 = 2593 ; R1_w=2593
1: (6b) * (u16 *) (r10 -4) = r1 ; R1_w=2593 R10=fp0 fp-8=?mm?????
2: (b7) r1 = 1684828783 ; R1_w=1684828783
3: (63) * (u32 *) (r10 -8) = r1 ; R1_w=1684828783 R10=fp0 fp-8_w=1684828783
4: (b7) r1 = 1461726319 ; R1_w=1461726319
5: (63) * (u32 *) (r10 -12) = r1 ; R1_w=1461726319 R10=fp0 fp-16=mmm?????
6: (b7) r1 = 1819043144 ; R1_w=1819043144
7: (63) * (u32 *) (r10 -16) = r1 ; R1_w=1819043144 R10=fp0 fp-16_w=1819043144
8: (b7) r1 = 0 ; R1_w=0
9: (73) * (u8 *) (r10 -2) = r1
lastidx 9 firstidx 0
regs=2 stack=0 before 8: (b7) r1 = 0
10: R1_w=P0 R10=fp0 fp-8_w=?0mmmmmm
10: (bf) r1 = r10 ; R1_w=fp0 R10=fp0
11: (07) r1 += -16 ; R1_w=fp-16
12: (b7) r2 = 15 ; R2_w=15
13: (85) call bpf_trace_printk#6
cannot call GPL-restricted function from non-GPL compatible program
processed 14 insns (limit 1000000) max_states_per_insn 0 total_states 0 peak_states
0 mark_read 0
prog_load: Invalid argument 
```

2）读取未初始化的寄存器，即寄存器从来没有被写过，则它是不可读的。

```c
struct bpf_insn insns2[] = {
    BPF_MOV64_REG(BPF_REG_0, BPF_REG_2),
    BPF_EXIT_INSN()
}; 
```

运行程序会报错如下：

```txt
$ sudo ./verifyer
166
func#0 @0
0: R1=ctx(off=0,imm=0) R10=fp0
0: (bf) r0 = r2
R2 !read.ok
processed 1 insns (limit 10