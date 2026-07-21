# 深入浅出

![](images/1862f2a150d0a08cb0c86d1c4ccfe45d2413cf4840c8bbb948e54f4ffcf2ad58.jpg)

# Linux TCP/IP

罗钰 编著

# 协议栈

学Linux TCP/IP,从这里开始！

![](images/60791b764fd549901b5bb40183eedba7b1dd6b12827e047b7e48eb51f4da2ae8.jpg)

从12.8内核基进，引速者快入门

![](images/101ea5ee1e6fb11e5457dff7a6c269ca8da66fbf8f58ac02e69f4993b53bda57.jpg)

运用大量示意图，清晰地画出之间的四用关系

![](images/a59c1c930a510e961b74448a5506df6994dad67d427e96ac4db326ef49fe74be.jpg)

详细分析信息代码李明思 代码注册号

# 作者介绍

罗钰：贵州人，国防科学技术大学硕士毕业，多年Linux底层开发经验，精通软件分析与设计。TCP/IP协议、曾开发Windows/Linux/Vxworks/FreeBSD等平台的设备驱动，开发过以太网芯片驱动、二层协议以及OSPF路由协议，对MPLS架构设计有非常丰富的经验，擅长编译器、CPU技术，近年来一直致力于无线网络产品系统的分析与设计工作。

本书首先介绍了Linux内核源码的整体概况及协议栈初始化过程，然后结合配置、用户使用协议栈的方法，采取深入浅出、由上及下的策略对协议栈代码进行分解和注释，涵盖以下内容：

操作系统、网络、协议栈、代码  
内核系统初始化  
配置网络系统  
网络层的实现  
传输层的实现   
Select系统调用的实现机制  
数据链路层协议实现

剖面设计：黄凯

分类建议：计算机/操作系统/Linux

人民邮电出版社网址：www.ptpress.com.cn

![](images/f763fc3dd898a663a108965b63040d773e34679d42ac56c2f4a25c3022856893.jpg)

![](images/029767f366cb94d582c19d8b10d94850e1c564dd0af5718402000860a4e456b1.jpg)

ISBN 978-7-115-21627-4

定价：49.00元

# 深入浅出 Linux TCP/IP 罗钰 编著 协议栈

# 图书在版编目（CIP）数据

深入浅出Linux TCP/IP协议栈 / 罗钰编著. — 北京：人民邮电出版社，2010.1  
ISBN 978-7-115-21627-4

I. ①深…Ⅱ. ①罗…Ⅲ. ①Linux操作系统②计算机网络一通信协议Ⅳ. ①TP316.89②TN915.04

中国版本图书馆CIP数据核字（2009）第205207号

# 内容提要

本书主要对Linux 2.6.18内核协议栈源代码做了一些基本的分析，这些分析基于作者在操作系统方面的研究和网络协议开发过程中的经验和笔记，编写本书的目的主要是使读者能够在尽可能短的时间内掌握Linux内核协议栈的工作机理，为移植和扩展协议栈打下基础。

本书首先介绍了内核源码的整体概况及协议栈初始化过程，然后结合配置、用户使用协议栈的方法，采取深入浅出、由上及下的策略对协议栈的代码进行了分解和注释。最后还介绍了通信界里较流行的 VLAN 技术和 LACP 协议。

本书适合Linux网络开发人员以及对Linux内核感兴趣的读者阅读。

# 深入浅出LinuxTCP/IP协议栈

<table><tr><td>编著罗任</td></tr><tr><td>责任编辑黄焱</td></tr><tr><td>人民邮电出版社出版发行 北京市崇文区夕照寺街14号</td></tr><tr><td>邮编100061 电子函件315@ptpress.com.cn</td></tr><tr><td>网址http://www.ptpress.com.cn</td></tr><tr><td>三河市海波印务有限公司印刷</td></tr><tr><td>开本:787×1092 1/16</td></tr><tr><td>印张:22</td></tr><tr><td>字数:524千字 2010年1月第1版</td></tr><tr><td>印数:1-3500册 2010年1月河北第1次印刷</td></tr></table>

ISBN 978-7-115-21627-4

定价：49.00元

读者服务热线：（010）67132692印装质量热线：（010）67129223

反盗版热线：（010）67171154

# 序

早在2001年年初我创办《永远的UNIX》（fanqiang.com）网站时，就梦想有一天能将众多技术高手的工作经验集结成册，使广大网友在实际应用中能随手翻查。那时候国内爱好者的学习热情很高，为了给网友提供一个更易于互动交流的平台，我在2001年年底创建了www.chinaunix.net社区网站（简称CU）。

经过8年的发展，在广大CUer的支持下，CU社区的注册用户已经超过百万，ChinaUnix已经成为全球最大、人气最旺的以交流Linux、UNIX和开源技术为主的中文社区。CU网站聚集了大量富有工作经验的系统架构师、软件工程师、DBA和网络架构工程师等，她已经成为广大开源技术爱好者学习、工作和生活中不可缺少的伙伴。

随着网站一点点壮大，网友也不断成长起来。大量的版主利用业余时间义务解答网友问题、维护论坛版块秩序、服务广大网友。很多网友将自己的学习、工作经验发帖分享出来，更有网友花费大量时间将自己多年的经验整理成书，供更多的人学习、分享。他们的成长是国内技术社区成长的基础，也必将推动国内技术社区的进一步发展。

今天，在CU管理员瑞儿mm（周平）、人民邮电出版社的李大徽经理、黄焱编辑和广大CU作者们的共同努力下，这套“ChinaUnix技术图书大系”终于和大家见面了，非常感谢人民邮电出版社的鼎立支持！人民邮电出版社是工业和信息化部主管的大型专业出版社，他们出版了一系列优秀的Linux/UNIX图书，在读者中有着巨大的影响力。希望“ChinaUnix技术图书大系”的出版能够为Linux、UNIX和开源技术在中国的普及、推广做出应有的贡献。

借此序，再次感谢一直以来关心和支持过ChinaUnix的网友、版主以及所有朋友们！

ChinaUnix 网站创始人 张强 (CU 网名: fanqiang)

2009年7月

# 前言

这是一本介绍协议栈实现源代码的书，不是介绍“协议”的书。很多人对网络非常感兴趣，但却不理解内部是如何运作的，或知之不多，那么最终的结果就是行之不远。本书尝试用浅显的语言和合理的安排带领读者到Linux内核网络模块的代码丛林中一游，既让你有所知，也让您有所思。

为什么选定 Linux 来介绍网络实现？首先，某些操作系统不是开源的，本人看不到也不能拿来解说；其次，Linux 的市场占有率还是比 BSD 要高的；最后，Linux 是经过成千上万用户使用过并且还将不断发展的。目前 Linux 在服务器市场上已经证明其设计的精巧和健壮，特别是当内核也从非抢占式发展成为抢占式后，嵌入设备市场上也将要掀起一股风浪。于是 Linux 内核分析的资料层出不穷，但有的太老（内核的代码还使用 2.2 的），有的对网络部分的分析不甚详细，于是作者萌发了分析整个 Linux 网络协议栈的想法。希望能在研究一些经典代码时发现与时俱进的部分，抛砖引玉，吸引更多的人参与到研究网络协议栈的实现技巧以及移植工作上，而不用对照枯燥的 RFC 文档和代码。

本人做过一些网络通信产品开发，所以对网络内部实现很感兴趣，于是在学习和工作时间之余记录下分析和调试Linux的网络协议栈的心得体会。

2005年开始计划写这本书，当时研究的内核版本是2.6.5，但是现在我使用RHELS作为研究平台时，内核却升级到了2.6.18版本，协议栈的部分代码就已经发生了很多变化，最新的内核版本已经是2.6.26版本，再一次感叹计划赶不上变化的同时，还是锁定了这个版本，毕竟这是知名公司服务器使用的内核版本，有问题还可以到社区寻找援助。

本书的整体风格是比较随性的记录。我的目标读者是广大对 Linux 和 TCP/IP 协议栈实现感兴趣的读者，而且要有一定研发经验和编程基础。只对协议感兴趣的读者，本书是不太适合的，要看最好是去看 RFC 或者相关的手册。

编者

2009年10月

# 目录

# 第1章 操作系统、网络、协议栈、代码

1.1 Linux操作系统介绍

1.1.1 Linux操作系统架构简介  
1.1.2 网络协议发展介绍

1.2本书的组织和安排

1.2.1 基本的数据结构和计算机术语 8  
1.2.2 图片风格演示  
1.2.3 本书的组织

# 第2章 内核系统初始化 14

2.1 系统初始化流程简介 15  
2.2 内核文件解读 19

2.2.1 ELF文件格式 19  
2.2.2 Link Scripts知识 22   
2.2.3 Linux内核镜像解析 23

2.3 中断及任务调度管理 37

2.3.1 中断及软中断模型 32  
2.3.2 各种语境下的切换 43  
2.3.3 内核下的同步与互斥 44  
2.3.4 各种异步手段 47

2.4虚拟文件系统 49  
2.5 网络协议栈各部分初始化 52

2.5.1 网络基础系统初始化 53  
2.5.2 网络内容管理 54  
2.5.3 网络文件系统初始化 63  
2.5.4 网络协议初始化 65  
2.5.5 初步了解路由系统 74

2.6 Linux 设备管理 75

2.6.1 底层PCI模块的初始化 78  
2.6.2 网络设备接口初始化例程 80

# 第3章 配置网络系统 92

# 3.1 配置过程分析 93

3.1.1 配置是如何下达到内核的？ 93  
3.1.2 socket系统调用 95  
3.1.3 ioct1 代码的实现  
3.1.4 loopback 接口的配置过程 113  
3.1.5 IP别名的实现 115

# 3.2 回顾FIB系统初始化 119

3.3 深入FIB系统 122  
3.4 FIB系统发生了什么样的变化 132  
3.5 直接访问路由表 149  
3.6 接口状态变化的处理过程 151

# 第4章 网络层实现的初步研究 154

4.1 从ping127.0.0.1开始旅程 155  
4.2 再次相遇 Socket 系统调用 157  
4.3 IP数据报文格式 158  
4.4 send系统调用 159  
4.5 在路由系统中游历 164

4.5.1 查找出口 164  
4.5.2 当目的地址是远端主机时 177  
4.5.3 创建对应路由cache表项 180  
4.5.4 创建对应邻居表项 184

# 4.6 回到发送的路径 189

4.6.1 IP层发送过程 189  
4.6.2 揭密hh_cache 193

# 4.7 ARP的作用 202

4.7.1 ARP的机制 202  
4.7.2 ARP报文格式 203  
4.7.3 Linux ARP协议的实现 204

# 4.8 到达设备驱动层 219

4.8.1 数据链路层帧格式 219  
4.8.2 Loopback 设备的发送过程 224

# 4.9 接收过程：从中断到路由系统 225

# 4.10 ICMP 240

4.10.1 ICMP报文格式 241  
4.10.2 ping本机地址及回环地址 242

4.10.3 ping外部地址 243

4.11 从内核到用户 249

# 第5章 传输层实现的研究 ………………………………………… 253

5.1 进一步到 UDP 254

5.1.1 UDP用户代码 254  
5.1.2 UDP数据报文格式 255  
5.1.3 服务器端bind的实现 255  
5.1.4 接收代码 261  
5.1.5 释放 UDP 的 socket ..... 264

5.2 更高阶的TCP 266

5.2.1 TCP用户代码 266  
5.2.2 TCP数据报文格式 266  
5.2.3 TCP栈及socket的初始化 268  
5.2.4 服务器端bind和listen的实现 271  
5.2.5 服务器端 accept 的实现 ..... 276  
5.2.6 客户端 connect 的实现——发起三次握手 ..... 278  
5.2.7 TCP报文的接收 286   
5.2.8 三次握手的实现 291  
5.2.9 内核收到报文转到用户态 295  
5.2.10 释放TCP的socket 300

5.3 TCP拥塞控制 303

5.3.1 TCP拥塞控制机制介绍 305  
5.3.2 Linux内核拥塞控制功能的实现 307

# 第6章 Select系统调用的实现机制 310

6.1 如何使用select 32  
6.2 Select的内核实现 313

# 第7章 数据链路层协议实现 318

7.1 基本的2层知识 319  
7.2 Linux桥实现的基本框架 320

7.3 Vlan 321

7.3.1 VLAN概念 321   
7.3.2 Linux下VLAN——存在巨大的缺陷 323

7.4 LACP协议 330

7.4.1 聚合端口简介 330  
7.4.2 LACP在Linux中的实现 333

7.5 2层功能总结 339

后记 341

参考文献 342

# TINUX

本章先从大的两个方面切入协议栈。一方面是协议栈的载体——操作系统，没有操作系统的支持，协议栈只是一些理论上的垃圾，而且，在当前网络化的世界里，不能提高操作系统网络能力的协议栈肯定会害死操作系统供应商甚至服务供应商；另一方面是网络协议的发展及制订，没有若干组织和大公司的参与，我们今天的网络协议会五花八门。

下面将为读者回忆一些计算机及软件方面的基本知识，并介绍本文的编写风格。

# 第1章

# 操作系统、网络、协议栈、代码

![](images/6bc1f9a3e51f92a0f4ba67c72209a993fd06da932f544088739a7acc4ba13a1d.jpg)

# 1.1 Linux操作系统介绍

# 1.1.1 Linux 操作系统架构简介

可以说，Linux 是 21 世纪初最火的操作系统之一。注意，我只是说它是最“火”的，而不是最“好”的。最好的定义对于每个人都不一样，为避免产生口水仗，我不在书中对 Linux 进行评价。下面先介绍一下 Linux 的架构。

Linux 肯定是一款大内核操作系统，Linus Tovals 和 Tanenbaum 的网上争论余音绕梁，相信知道此事的读者一定还记得 Linus 支持大内核的建议吧。虽然说 Linux 是以大内核的方式运行，但编译方式已经吸收了 Windows 的动态加载模块的特点。也就是说，20 世纪 80 年代的大内核和现在说的大内核不完全是一个概念。那时候不仅运行时候的所有驱动、文件系统、包括本书要讨论的协议栈都要运行在内核态，而且编译的时候，编译的文件必须同时被编译到一个大的二进制文件中。如今的内核，在编译的时候，可以有选择地加入或减去某部分代码，使其编译出来的内核变“小”，而一些驱动程序和模块可以在系统起动以后再加载。那么就单纯的那个内核镜像而言，真的还不算“大”，这个时候要说 Linux 是大还是小还真有点难。

![](images/a10075d42e743bf120d94a828b373695dd0286ca94fb7f5a06c4b52c773c6098.jpg)

操作系统内核可能是微内核，也可能是大内核（后者有时称之为宏内核 Macrokernel）。按照类似封装的形式，这些术语定义如下。

- 微内核（Microkernel kernel）——在微内核中，大部分内核都作为独立的进程在特权状态下运行。它们通过消息传递进行通信。在典型情况下，每个概念模块都有一个进程。因此，如果在设计中有一个系统调用模块，那么就必然有一个相应的进程来接收系统调用，并和能够执行系统调用的其他进程（或模块）通信以完成所需任务。在这些设计中，微内核部分经常只不过是一个消息转发站，当系统调用模块要给文件系统模块发送消息时，消息直接通过内核转发。这种方式有助于实现模块间的隔离。在一些微内核的设计中，更多的功能，如I/O等，也都被封装在内核中了。但是最根本的思想还是要保持微内核尽量小，这样只需要把微内核本身进行移植就可以完成将整个内核移植到新的平台上。其他模块都只依赖于微内核或其他模块，并不直接依赖硬件。微内核设计的一个优点是在不影响系统其他部分的情况下，可以在系统运行时将开发出的新系统模块或者需要替换现有模块的模块直接而且迅速的加入系统。另外一个优点是不需要的模块将不会被加载到内存中，因此微内核就可以更有效的利用内存。

- 大内核（Monolithic kernel）——单内核是一个很大的进程，它的内部又可以被分为若干模块（或者是层次或其他），但是在运行的时候，它是一个独立的二进制大映象。其模块间的通信是通过直接调用其他模块中的函数实现的，而不是消息传递。

单内核的支持者声称微内核的消息传递开销引起了效率的损失。微内核的支持者则认为因此而增加的内核设计的灵活性和可维护性可以弥补任何损失，就像Linux内核是微内核和单一内核的混合产物一样。Linux内核基本上是单一的，但是它并不是一个纯粹的集成内核。为什么Linux必然是单内核的呢？一个方面是历史的原因：在Linux的观点看来，通过把内核以单一的方式进行组织并在最初始的空间中运行是相当容易的事情。这种决策避免了有关消息传递体系结构，计算模块装载方式等方面的相关工作（内核模块系统在随后的几年中又进行了不断的改进）。

![](images/fcd4aa7e2a2aff579e20227201b61a95bccda2e5c6dcabd9480f2a19de34b046.jpg)

如果 Linux 是纯微内核设计，那么向其他体系结构上的移植将会比较容易。实际的情况是，Linux 内核的移植虽然不是很简单，但也绝不是不可能的。虽然这比微内核的移植需要更多的代码，但是 Linux 的支持者将会提出，这样的 Linux 内核移植版本比微内核更能够有效的利用底层硬件，因而移植过程中的额外工作是能够从系统性能的提高上得到补偿的。

这不是自相矛盾吗？有的读者可能会疑惑了。其实，大内核和小内核之争已经过时了，连 Windows 也号称过微内核，那么有必要这么认真吗？其实要记住，除了嵌入式，没有一款商用（包括开源）的操作系统是以真正学术上的微内核方式存在。也就是说，所有的商用操作系统都是把驱动、文件系统、协议栈等塞入内核之中，原因在于对安全和效率方面的考虑。

把这些模块放入内核中确实提高了安全性和效率，但也引入了新的问题，比如说进程之间的调度，进程之间同步互斥等。Linux 内核在发展初期由于没有考虑到这些问题，以至于在实时性能上为人诟病。不过本书的内容与实时性关系不大，如果读者希望能进一步了解的话，可以和笔者讨论。

![](images/a290fcb96ffa48dbfc06503836b15df000ed1017261c594ec7f54f32eeef108c.jpg)  
图1-1所示是Linux整体的一个层次图。  
图1-1 操作系统架构图

IP Stack 就是本书要分析的模块，坦白地说，这个模块并不是一个操作系统所最需要的部分，因为并不是每台计算机都要上网、聊天、收发邮件、联机对战……什么？还有不能聊天的计算机吗？嗯，笔者的计算机目前正是如此。

从这个模块的位置来看，它牵扯到内核中大部分模块，可以说不了解其他任何一个部分，对了解该协议栈的工作行为都有困难。这也是IP Stack的难点：通过RFC去单独理解IP协议，没有太大困难，但要真正看懂在Linux是如何实现的，则还需要比较广博的操作系统知识。

了解了 IP Stack 在操作系统内的位置后，那么下面开始介绍网络了。

# 1.1.2 网络协议发展介绍

由计算机网络迅猛发展而形成的网络化是推动信息化、数字化和全球化的基础和核心，

正是基于计算机网络的各种网络应用系统通过在网络中对数字信息的综合采集、存储、传输、处理和利用而在全球范围把人类社会更紧密地联系起来，并以不可抗拒之势影响和冲击着人类社会政治、经济、军事和日常工作、生活的各个方面。因此，计算机网络将注定成为21世纪全球信息社会最重要的基础设施。

计算机网络技术也在发展，虽然十年前各路专家都在预测各种各样神奇的技术，但没有想到的，当时被认为最“简单”、最“幼稚”的IP网络笑到了最后。可以得出结论的是：“IP化”是现在各种体系的最终解决方案，甚至在最近几年，“以太化”（以以太网技术为基础的网络设施架设）是最流行的研究方向。

感谢您选择了这本书，这两者都是本书要讨论的重点。这两个网络明星到底有什么优点让各路神仙拜倒在脚下，不是本书能说得清楚的，但它们内部做了什么，本书将会为您一一揭示。

在互联网30年的历史中，实际上曾经出现过多种网络协议，从物理层到应用层，很多公司和组织发明了自己的网络协议，其实有的性能还不错，只是由于生不逢时或商业原因，消失在大家的视野外。最终生存并繁荣到现在的只有3个：TCP/IP的网络结构、IP路由、以太网。其中的IP协议，由于其健壮性和简单性，被军方和学校发展成为一套协议族，就是我们常说的TCP/IP协议族。

TCP/IP 的起源可以上溯到 1975 年美国 DARPA（Defense Advanced Research Projects Agency 国防部远景规划局）支助的研究计划。那是个试验性的网络 ARPANET，在证实成功以后，于 1975 年转入正常运行。1983 年，新的 TCP/IP 协议族被作为标准采用，并且网络中的所有主机必须使用它，后来 ARPANET 最终成长为 Internet 这个“怪兽”（ARPANET 本身于 1990 年停止使用）。

TCP/IP本身是一个协议族，还包含了ARP、ICMP、UDP等协议。它从开始提出到现在，广泛使用已经差不多30年了，在这段时间内，不管是大型组织还是公司都提出其他类型的网络协议栈试图取代TCP/IP，但都没有成功，反而是IP逐渐蚕食其他网络市场的份额，比如IPX。下一代IP网络将采用IP网的核心技术（分组交换、不面向连接），结合电信网的设计理念，建立一个更大、更快、更安全、可信任、为用户提供灵活业务的可管理网络，为运营商提供一个可以达到电信网服务质量保证的IP网，建立可赢利的商业模式。

那么IP网络的优点在那呢？正如我前面所说，它简单。是的，套用一句广告：简单，但不简约。不简约的是它的代码，它是一堆小协议和数据的集合，其复杂性给人类带来的好处就在于管理简单，因为“智能”的部分由它来做了。

前几年主流的网络教材上会提到ISO提出网络分为7层的概念：应用层，表示层、会话层、传输层、网络层、数据链路层、物理层。每一层都分配了一组特定的功能，每一层都使用下层提供的服务，也为上一层提供服务，如图1-2所示。

由于提出 OSI 这套体系时 TCP/IP 已经成为工业事实标准, 由于前者的复杂性较大并且后者居然也可以工作的很好, 人们也懒得去再开发一套新的协议, 于是 OSI 的分层架构已经成为一种教学用标准——告诉纯洁的学生们。（网络）世界是分层的！

与 OSI 对照就会发现 TCP/IP 的 4 层架构基本上和 OSI 的分层理念相似, 人们把 TCP/IP

协议族根据这种体制从底往上分别叫做：物理层、链路层、网络层、传输层、应用层。

![](images/04bc53c1009ba0d98acdadac322d9d7461820aa6877ff899469a1faee23a08a7.jpg)  
图1-2 OSI七层网络模型及TCP/IP关系

物理层是实际的传输介质，大家可以理解为网线、光纤，甚至无线连接。其实经典的TCP/IP协议栈是没有提出物理层这个概念的，在以太网环境下我们称其MAC层。链路层指两个相互连接的实体之间的协议集合，本书中将链路层简称为2层或L2，常用的L2协议有VLAN、LACP、STP等。网络层用来进行路由寻址、数据转发，本书简称其为3层或L3，就是IP层。而传输层即为4层或L4，是用来管理应用程序收发报文的过程，比如我们常用的TCP和UDP。

这些层次中最重要、最复杂的就是大名鼎鼎的网络层即 IP 层。从图 1-3 可以看到，我们的 TCP/IP 协议栈包含了图中若干部分，IP 是连接 2 层和 4 层的中枢，IP 协议栈的中心就在于，使用 IP 协议将数据包发送到任何网络，而且数据包到达的时机可以不同。它的主要功能有以下 3 个。

(1) 在一个或多个网络上找到一条连接两个主机系统的路径。  
(2) 将数据单元分段成为路径上各网络能处理的尺寸, 还要保证作为接收方的时候能重组这些分段报文。  
（3）避免在传输路径上出现阻塞。

由上所述, 可以推断出 IP 层不仅作为数据在系统 (上下方向) 中传送的纽带, 也作为机器间数据传输 (水平方向) 的判断者, 这就无形中增加了实现 IP 层的难度。竖线即表示本机

系统的数据流，横线表示计算机间的数据流。L2 在本书中代表以太网功能，它就是 2 层功能的一个具体实现。L2 协议包括了 PPP 和 SLIP 及 Ethernet，前两者不属于本书范畴，只需知道它们和 Ethernet 一样，属于底层的传送协议即可。ARP 属于 2 层和 3 层之间的一个连接层，而且此图将 ARP 和 L2 层并列画在同层只是一种简化，实际上 ARP 是建立在 L2 层上的协议。我们可以认为它是 2.5 层的协议。

![](images/4d69361f2a5c2aa7ab44b996f2c58c5312c01f9caad20dabcc98a0a549b8139c.jpg)  
图1-3 IP为什么重要

图1-3中所示的IP和IP相连，实际只是逻辑的概念，实际数据的传输还是通过下层协议再到物理层进行。远非图中画的这么简单。

![](images/adad34c3ac9993c40ffc553674ec5efd9513e647e9128f4eb29c27cb4669ff1a.jpg)

世界上的每一台机器通过协议栈相连，其基本形式大概也就是如图 1-3 所示，只不过有的实现是 Linux，有的是 UNIX/BSD，有的是 Windows 系统提供的协议栈，当然还有一般人看不见的网络设备中厂商自己实现的协议栈。

每一类软件模块都不能独立存在，必定依托系统其他模块的支持才能工作，协议栈更是如此。由于协议栈是在内核中实现，所以，必须搞清楚操作系统中是如何支持协议栈的，图1-4所示可以作为一个参考。

图1-4中对于大多数人比较眼熟，但我还是要着重提到两个不太为人注意的模块，一个是glibc库，对于大多数从事软件编程2年以下的读者，这个部分比较难理解。其实我们几乎无时无刻不与这部分打交道，比如说不管是应用层软件开发，还是嵌入式软件开发，我们用过的malloc函数、 strcpy函数都是这个库提供的。不仅如此，网络编程中用到的API接口也是这个库提供的。至于怎么提供，下文提及。另一个要强调的模块是INET，它不属于TCP/IP体系必须的一个部分，但TCP/IP层的接口要通过INET层才能访问操作，这一操作是在网络初始化时就已经注册到BSD风格的socket层的。所谓BSD风格就是我们常说的socket、bind、connect、listen、send和recv等系统接口的调用风格。不管是类UNIX还是Windows操作系统都必须实现这些接口，这些接口内部不仅支持你上网（就是AF_INET），还支持你的应用程序之间的通信（比如AF_INET），或者内核与用户之间通信（AF_NETLINK），甚至一些少见的协议（比如AF_IPX），但就本书的内容显然属于AF_INET层的范围，它封装了TCP/IP的接口。

![](images/d3ff1d7789b388098297ff25c97a0d75ea5bf6da0ca43eb899676221e4ccf78e.jpg)  
图1-4 真实操作系统协议栈实现

从图 1-4 中可以看到 IP 层并不是完全在 TCP 之下。换言之，应用程序是可以绕过 TCP 层而直接与 IP 层协作，比如我们常用的 ping 命令。

在这里要聊聊Linux协议栈的历史。好像大家都说Linux协议栈都起源于BSD协议栈，但从代码组织和风格上看，似乎这两者没有什么联系。从代码注释上和因特网上搜查到的信息都会告诉你第一个实现协议栈的作者是Ross Biro，是在0.96~0.99版本之间（1992年左右）加入的，它就是Net-1版本。后来Fred Van Kempan和Alan Cox接手了前者的工作重写了很多代码后在Linux 1.0发布之前提交了Net-2版本。Net-3是在Linux 1.2版本中发布的，一直用到2.0，目前Linux 2.x之后的代码属于Net-4版本，整个协议栈成为地球上自认为很厉害的编程高手比拼内力的结果。

Net-4 协议栈提供了大量的设备驱动和高级的特性，比如 SLIP 和 PPP，PLIP，IPX 等协议。

![](images/c926aa8427bf5cf327a4ab2c7a75da0b37cd28aa32cf5b5aecedd8e49c6946e1.jpg)

# 本书的组织和安排

读者一般都玩过搭积木这种游戏，也都知道积木其实也就是几种类型的“块”组成，在搭积木的时候我们应该关心要建造的城堡的形状而不是积木“块”的材质和硬度等一些非常细节的东西。分析代码也是这样。Linux 内核大量采用了几种通用的数据结构，正如同积木块，如果每遇到一次这种代码就分析一次，估计本书篇幅会太大，于是单独拿出来先对其分析，记住和熟悉其基本操作方式，只要形成了思维定势，那么我们对 Linux 内核的理解难度会降低。

# 1.2.1 基本的数据结构和计算机术语

本书的读者群应该是掌握了基本的Linux编程技术并对网络协议有一定了解的中高级开发人员，在讲述各章节的内容中，许多浅显的技术将会一带而过，不花太多的笔墨。但以下一些编码技术和术语，需要着重强调。

链表结构

链表数据结构的定义很简单（节选自[include/linux/list.h]，以下所有代码，除非加以说明，其余均取自该文件）：

```txt
struct list_head { struct list_head \*next, \*prev; 
```

list_head 结构包含两个指向 list_head 结构的指针 prev 和 next，由此可见，内核的链表具备双链表功能，实际上，通常它都组织成双循环链表。不过它和传统教科书上介绍的双链表结构模型不同，这里的 list_head 没有数据域。在 Linux 内核链表中，不是在链表结构中包含数据，而是在数据结构中包含链表节点。也就是说，开发者预计某种数据结构将要组成链表时，它的成员中必含一个 struct list_head 成员。

由链表节点到数据项变量

我们知道，Linux链表中仅保存了数据项结构中list_head成员变量的地址，那么我们如何通过这个list_head成员访问到它的所有者呢？Linux为此提供了一个list_entry（ptr,type, member）宏，其中ptr是指向该数据中list_head成员的指针，type是数据项的类型，member则是数据项类型中的某一个成员，例如，有一个由proto{}组成的链表，名字叫proto_list，而每个proto{}结构中的list_head{}成员名字叫node。我们要访问这个链表中第一个节点，则如此调用：

```erlang
list_entry (proto_list->next, struct proto, node) 
```

list_entry 的使用相当简单，相比之下，它的实现则有一些难懂，先告诉大家的是，Linux 内核用的 GCC 编译器使用了一些比较新的关键字，比如 typeof，所以下面的代码是无法在 VC 等编译器上通过：

```c
define list_entry (ptr, type, member) container_of (ptr, type, member) #define container_of (ptr, type, member) { const sizeof ((type * 0) ->member) * _nptr = (ptr); (type *) (char *) _nptr - offsetof (type, member)) }; } #define offsetof (TYPE, MEMBER) (size_t) & ((TYPE *) 0) ->MEMBER) 
```

读者应该注意到代码中底纹较深的一个词：typeof，正是利用typeof，内核中许多技巧得以实现。在这里，先求得结构成员在与结构中的偏移量，然后根据成员变量的地址反过来得出属主结构变量的地址。将来我们在代码片断中看到一些底纹较深的函数或变量，都是特意提醒读者要注意的地方。

要记住的是 container_of() 和 offsetof() 并不仅用于链表操作，这里最有趣的地方是 ((type *) 0) → member，它将 0 地址强制“转换”为 type 结构的指针，再访问到 type 结构中的 member 成员。对于给定一个结构，offsetof (type, member) 是一个常量，list_entry（）

正是利用这个不变的偏移量来求得链表数据项的变量地址。

hlist

图1-5中显示出关于Linux内核中list和hlist两者的区别，hlist是hash list的简称，即用拉链法实现的hash数据结构。它由2部分组成：hash数组和冲突链。当节点第一次要插入hash表的时候，它必定是先插入hash数组中，而以后要插入的节点如果发生了冲突，则可以挂在数组后面，形成一条链表。当然也可以插入数组，原先在数组中的节点被挤出来形成一条链表。

![](images/b76bebc666f4c9104d450e65f88517e40c5f738cdf43c6a2822338906d0454fe.jpg)

![](images/be1a69b27834e592144c9348bb53aad094dc3efef800aaa79465dade920fff28.jpg)  
图1-5 list和hlist的区别

也许 Linux 链表设计者认为双头（next、prev）的双链表对于 HASH 表来说“过于浪费”，因而另行设计了一套用于 HASH 表应用的 hlist 数据结构——单指针表头双循环链表。

代码段1-1 list.h hist_head的定义  
struct hlist_head { struct hlist_node \*first;   
};   
struct hlist_node{ struct hlist_node \*next, $^{\star \star}$ pprev; 1

struct hlist_head 仅仅用了一个指针，占 4 个字节，因为 hash 数组往往相当大，这样可以节省 4 个字节乘以数据大小的空间。从图 1-5 中可以看出，hlist 的表头仅有一个指向首节点的指针，而没有指向尾节点的指针，这样在可能是海量的 HASH 表中存储的表头就能减少一半的空间消耗。笔者习惯上称 hash 数组为 hash 表，而将冲突节点链表称为 hash 链。

可以说 Linux 内核大部分数据是用 hash 表组成，所以在这里特别要提醒不太懂 hash 表算法的读者，还是不要往下看了，特别是网络部分中很多结构是用 hash 表组织的，为了节省您宝贵的时间，您要么把 HASH 算法复习一下，要么把本书放回书架吧。

- likely 和 unlikely 的含义

Linux 内核中有这么两个宏 likely 和 unlikely，它们的字面意思是“可能地”和“不太可能地”，其分别定义为：

```txt
define likely(x) __builtinbaoepect(!!(x),1)
	#define unlikely (x) __builtinbaoepect(!!(x),0) 
```

_built_in expect 不是标准的 C 语言，而是 GCC 编译器内置的关键字，用来优化生成的汇编代码。它告诉编译器，记住，是编译器本身，其后面的代码该如何被编译了。请看下面的代码示例：

<table><tr><td>源码</td><td>没有优化的汇编伪码</td><td>优化后的汇编代码</td></tr><tr><td>1 if (likely(con1)</td><td>if con1</td><td>if con1</td></tr><tr><td>2 do Thing1()</td><td>jmp do Thing1</td><td>jmp do thing1</td></tr><tr><td>3 if (unlikely(con2)</td><td>if !con2</td><td>if con2</td></tr><tr><td>4 do Thing2()</td><td>jmp do Thing2</td><td>jmp do thing3</td></tr><tr><td>5 else</td><td>if con2</td><td>if !con2</td></tr><tr><td>6 do Thing3()</td><td>jmp do Thing3</td><td>jmp do thing2</td></tr></table>

在这里要涉及CPU的流水线概念，因为一般情况下每一条汇编代码都是顺序生成的，do Thing2的代码会先装入流水线，但是它“不太可能”被执行，所以流水线必须要排空，然后才把do Thing3()的代码装入，这样导致一些性能损失。但是在这里加上了unlikely指令后，第6行的汇编代码会被GCC放到第4行代码之前，这样在程序执行的时候就“很有可能”地先执行do Thing3()函数，而do Thing2()几乎就不需要装入流水线，提高了性能。

不仅内核可以使用这个编译指令，用户态的程序编译也可以用gcc -O2优化编译选项来编译接下来的C用户空间程序，当存在最最可能发生的分支情形时，使用likely（）；而当存在最最不可能发生的分支情形时，使用unlikely（）。当然，VC6肯定是没有这样的指令了。

介绍这个技术有啥意图呢？因为本书涉及的代码非常多，不可能每条代码都分析。为了提高效率和抓住重点，我们可以放弃一些代码分支，而凡是用unlikely修饰的分支，我们可以坚决的丢弃，因为它们“不太可能”被执行。

# 1.2.2 图片风格演示

每一个程序员都有自己分析代码的一套方法，但是初学者在面对浩如烟海的代码时，难免心中发怵，其实大可不必。程序是人写的，既然别人都能写出来，那么光看还是比写要轻松的多吧。关键是如何看，我的经验是抓住头、甩开尾、顺藤摸瓜和图表结合这“擒敌”四招。

抓住头。不管什么架构，不管什么项目，总有开头，如果没被执行的代码，写的再有趣，那也是没用的代码，只能把自己搞晕。如果代码被执行，那必有其调用者，依此类推，总能找到其根。然后再根据这个根反向追踪它是如何调用子函数的。

甩开尾。很多模块在处理错误时或者正常退出时都有一部分清理内存、清理资源的代码，在不影响大局的前提下，可以先放弃分析这些代码的作用，这样把有限的精力放在代码主干上。

顺藤摸瓜很简单，见到函数就跟进去，那么最后就是要做记录了。我相信世界上有很多人是过目不忘的记性，但是更多人的记忆力是有限的，那么就要做笔记，画出函数调用图或者消息时序图，是代码的思想抽象的体现。如果执着于一行代码一个算法，就有可能只见树木不见森林。

本书中所有图示都是笔者画出的，有一些独特的表示方式需要给读者做解释。在每一章/节的开头，提供了关于此章的函数调用树。这种调用树在代码分析过程中经常可以看到，它简单易懂，从宏观上体现了开发人员的基本思路。对于简单的代码流程可以快速跨过。而对关键的代码段进行分析，指出其算法，给出注释。

这样两种粗细结合的代码分析过程在笔者从事软件开发过程中起了很大的帮助, 相信也能给读者以帮助, 尽早理解协议栈工作流程。

# 函数调用树

由于本书中不可能引用内核中所有代码，所以对比较简单的代码的分析采用了调用树的方式。目前我还没有在哪一本书刊或杂志看到过这样一种代码示意图，这也算本人的一个原创吧。现在先给大家演示一个，如图1-6所示。

![](images/4ffa57b877c6d9df41ac99fe619a12ac15a6b9703fc81ca3d86e1621b1343eea.jpg)  
图1-6 函数调用树的演示

对于函数调用，我们采用第①种方法表示。如果采用第②种比较传统的方式，那么C_Function将放在何处呢？而且，在大部分代码中，有很多对错误进行判断的代码分支，如果我们要完全画出这些错误处理分支，那么工作量是非常巨大的，而且是没有必要的，最关键的是影响读者理解主要的脉络。

所以开发经验告诉我，传统的表示方法是不适合大范围、大跨步了解一个原有系统结构的。那么这种图解方式算是对我“擒敌”四招的一个体现吧。

书中还有一些序列图，我想大家应该不会产生理解上的困难吧。

# 1.2.3 本书的组织

从一定角度看，Linux本身就是网络的代名词。它的开发和发展都是通过程序员在网络上，尤其是在互联网和新闻组上交流信息、程序代码完成的。Linux从1991年出现到现在，网络方面的代码一直是至关重要的部分，代码间关系相当复杂。从在内核代码根目录单独建立了net目录，足以看出网络在Linux中是和文件系统一样重要。当然，网络相关的代码在drivers/net中还有一部分。

网络协议栈的内容实际上包括了除TCP/IP在内的协议，当前基于TCP/IP的协议不下百种，这从一个方面说明了TCP/IP网络强大的影响力，也给作者带来了分析的难度：要不要分析诸如SLIP，PPP，SIP，OSPF等协议？要怎样才能将这些协议从TCP/IP代码中剥离？如果不介绍这些协议代码会对协议栈的理解有什么影响？

下面是 Linux 内核源代码的目录树，加粗的字体部分就是 Linux 内核实现的协议模块，很显然，仅仅是网络部分本书就不能一一分析给读者，只能寄希望读者能举一反三、融会贯通了。

```txt
Linux-2.6.10  
|_arch  
|_config  
|_documentation  
|_Drivers  
|_net  
|_..  
|_fs  
|_include  
|_init  
|_ipc  
|_kernel  
|_lib  
|_nn  
|_net  
|_802  
|_802.q  
|_bridge  
|_core  
|_ethernet  
|_ipv4  
|_ipv6  
|_llc  
|_netlink  
|_packet  
|_sched  
|_...  
|scripts  
|_security  
|_sound  
|user 
```

本书的组织如下：

第2章，首先介绍操作系统的部分基本知识，通过分析其启动过程，引入ELF文件格式，为追踪代码的起始点打下基础。然后介绍Linux内核代码中一些基本知识，比如中断、互斥锁、VFS、设备管理等，这些知识是理解协议栈必不可少的基石，脱离这些知识点讨论协议栈，只能是浮沙之上筑高台。接着重点分析协议栈的初始化，为分析、理解栈的概念打下基础。

第3章，接下来，由简入难，先介绍配置IP地址的过程，引入socket和ioct1系统调用，然后进入最复杂的FIB系统，图解配置过程中FIB表的创建和存储，使读者对Linux内核路由系统有一个印象深刻的体会。

第4章，内核路由系统建立之后要研究IP层的运转机制，我们采用最简单的Ping操作为读者介绍IP报文格式、send和recv系统调用的实现，并着重分析路由系统和ARP以及邻居系统之间的协作，完整的再现一个ICMP报文从产生到消亡的过程。

第5章，在熟悉IP层工作原理之后，我会介绍UDP和TCP内部的实现，引入bind、listen、accept、connect的实现，使读者完完全全掌控套接字本质。最后，引入拥塞控制的基本概念，让读者了解Linux内核协议栈架构的精妙之处。

第6章，这一章比较短，主要是介绍select实现机制，这是很多嵌入式系统实现多任务的基础，希望读者能引起重视。

第7章，重点介绍数据链路层的协议：VLAN、LACP。这些二层协议是当前网络设备的基本运行协议，了解它们的存在及目前存在的缺陷也能对协议栈有深入的体会，也能加深协议栈的架构设计。

尽管笔者有近十年的软件开发经验，但是在分析协议栈的代码时还是遇到很多困难，不要说把整个内核说得一清二楚，就是单独把协议栈拎出来写成一本书也是费煞苦心。因为各种结构和函数相互纠缠在一起，在介绍某一个模块时真是顾头不顾腕，这也是为什么要花这么久才成书的原因。

所以，强烈的建议读者们，在阅读本书时，以协议层次之间传递的数据流为根本，作为理解协议栈工作原理的主线，再辅以函数调用序列分析，相信能在阅读本书后对协议栈有清晰的理解。出于能力和时间关系，决定舍弃大部分协议，不在本书中介绍关于驱动程序、包封装的协议，或协议的具体实现。本书侧重介绍的是Linux中TCP/IP网络协议栈的实现，讲解各种机制的实现。由于内核中涉及的技术非常多，而且是一环扣一环，再次强烈的建议读者按顺序读完每一章。

由于网络协议栈涉及的东西实在太多，只能挑一些最常见的内容去说，而不能面面俱到，因为我还没有那么广的阅历，也没有那么多时间全部写就。

# TINUX

# 第2章

# 内核系统初始化

大凡拿到 Linux 发行版的人，必先想编译一次内核源代码以图一快。然后尝试裁减内核，然后查看源码，然后修改，那么现在先给一个初步的关于 Linux 内核源代码编译及裁减的介绍，因为对于研究内核源代码，不了解其编译的内部机制，就不可能精通其代码执行的起源。

![](images/e93aa08e27ca0d4ffef4706c9ced40713a609ab025db4d90057c8fd8ee879d49.jpg)

先来看看如何编译源代码。

首先，进入Linux源码目录，键入make menuconfig，会出现一个类似菜单的界面让用户选择是否裁掉或加进某模块。我们关心的网络模块界面如图2-1所示。

![](images/77319a536caf9be2e66e753f1d376309ca09b3777e3aeb9a84a5b105f5fc9902.jpg)  
图2-1 Linux内核编译——网络选项部分

在这幅图中，有的选项前是“*”，有的是“M”，有的是空，这表示什么呢？即当选择“*”的时候，模块被编译进内核，在系统启动的时候，被主调函数调用执行；而选择“M”的时候，表示被编译成一个.ko文件，放在某个目录下，系统启动的时候依靠脚本把这些目标文件装入内核。为什么要有这样的区分？是因为这两种编译方式决定了内核的大小，也决定了内核模块被初始化的过程，这些内容在其他书中有介绍，下面的章节还会提及。不过，还是先来研究系统启动。

# 2.1

# 系统初始化流程简介

Linux系统的启动，指的是从系统加电后直至系统控制台显示“login:”登录提示符为止的系统运行阶段。与这部分动作密切相关的代码主要是：

- 4个汇编程序：bootsect.S、setup.S、head.S和entry.S。  
init目录下的main.c文件。

本节介绍的程序流程不多，这是由于汇编代码在协议栈中没有太多关系，我们可以直接分析C语言编写的初始化代码。另一方面，研究汇编没有必要。

下面对init/main.c中的start_kernel函数进行分析（这是Linux2.6.5内核的启动流程图，依据此版本画出图2-2）。

![](images/b60de9699cda02e8ce7676a526688963128515296a0bb9f0019783cf7987631d.jpg)  
图2-2 系统启动函数序列图

在系统启动过程中，要关注以下几个方面：

（1）中断系统及调度系统。  
（2）文件系统的初始化。  
（3）设备管理系统的初始化。  
（4）网络协议的初始化。

这 4 点分别是本章的 4 个小节, 但在介绍这 4 个方面的内容之前必须提前介绍关于 ELF 文件格式的基本常识。最后一节会讲解网络协议栈本身的初始化。为什么要做这样的安排? 因为网络系统本身关系到设备、文件系统、任务调度等方面, 如果这些具体的问题没有搞清楚, 理解协议栈本身是比较困难的。而关于 ELF 文件格式的内容则是更加重要, 因为仅仅理解代码并不困难, 而要理解编译器在生成内核的过程中做了什么事是区别内核与普通可执行

文件的关键。

不过，在图2-2中的函数中没有发现与网络相关，那么它隐藏在哪呢？再看看rest_init函数吧。下面是分析init/main.c中rest_init函数，其函数调用树如图2-3所示。

![](images/2cc0e7257b3d03b572cd45577db918591ec2fdb8bce20b733676eff364ecc306.jpg)  
图2-3 rest_init函数调用树

在此函数中，还是没看到关于网络初始化相关代码，但你看到那个 kernel_thread 函数了吗？那个函数创建一个内核线程，原型如下：

```c
int kernel_thread (int (*fn) (void *), void * arg, unsigned long flags) 
```

此函数定义在 arch/i386/kernel/process.c 中, 它利用 linux/i386 的 do_fork 函数创建一个新的内核态线程, Linux 的内核线程是没有虚拟存储空间的进程, 它们运行在内核中, 直接使用物理地址空间。

在这里，kernel_thread 创建的新的内核线程是 init，然后返回，执行 unlock_kernel（与 start_kernel 中的 lock_kernel 对应），接着执行 cpu_idle（），这实际是执行初始化主线程的归宿：它观察自己是否处于 TIF NEED_RESCHED——在 need_resched 实现，如果不是，就让自己睡眠，否则完成 schedule()函数。TIF 即 Thread Information Flag 的意思。

现在注意力转到init函数。Linux内核代码中有多个模块定义了init函数，这是因为许多设备驱动程序自己实现了名字叫做init的函数，大多数都是我们不关心的内容，所以必须找到正确的那个函数。众里寻它千百度，蓦然回首，它居然就在init/main.c中。图2-4所示即为init函数调用图。

init 线程好像调用了许多函数, 但终于只在 do basic_setup 中看到了关于和网络有关系的初始化函数——sock_init(), 但是我们跟踪进这个函数, 发现它只是为网络创建了执行环境,并为协议栈申请了内存空间。我们将在 2.5.1 小节详细分析此函数。但是协议栈本身在哪里被初始化呢?

在这样的函数调用树中找不到直接关于网络的初始化函数，我们将注意力移到

do_initcalls 这个函数，在此函数定义的 C 文件中，有这样两个变量 __initcall_start 和 __initcall_end，它们是如代码段 2-1 所示定义的。

![](images/32c02002aa763037ea86cbf4b59f7fd28bfdc50cc237188db40de70daaaffeab.jpg)  
图2-4init函数调用关系树

代码段2-1 main.c do_initcalls函数

```txt
1. extern initcall_t __initcall_start, __initcall_end;  
2.  
3. static void __init do_initcalls (void)  
4. {  
5. initcall_t *call;  
6. int count = preempt_count();  
/*从__initcall_start这个变量开始遍历，直到遇到__initcall_end这个变量*/  
7. for (call = &initcall_start; call < &initcall_end; call++)  
8. {  
9. char *mag;  
10.  
/*调用初始化函数，我们目前是无法从代码上直接看到call是什么函数*/  
11. (*call)();  
12.  
13. mag = NULL;  
14.  
15. }  
16.  
17. /*向内核veentd_wq发消息以确保没有运行完的初始化任务，请参考2.3.4节*/  
18. flush Scheduled_work();  
19. }
```

我们用C源码分析工具没有观察到__initcall_start和__initcall_end是在哪个C文件和H文件中被定义的。难道没有定义它们编译器就让这种“错误”蒙混过关了吗？我们注意到call变量是initcall_t类型的，这是一种什么样的类型呢？只好到跟initcall_t相关的文件中去找。于是在include/linux目录下发现init.h文件定义了这个类型变量，下面章节我们会介绍具体的定义，为了介绍为什么要定义这样一种类型，那我们必须得知道一个不得不说的话题：ELF文件格式。下面的章节详细讲解关于这方面的知识，这对于了解操作系统工作也是重要的。

# 2.2 内核文件解读

为了解决initcall_t到底是什么变量类型则必须提及C语言中一个比较少见的内容——可执行文件格式，只有了解了这种文件格式才能具体知道initcall_t的意义。如果有读者对ELF文件格式比较了解的话请跳过该节，如果不太了解，那么得耐着性子把这两节看完，否则对于Linux内核模块的初始化不能完全搞懂。

# 2.2.1 ELF文件格式

ELF 是 *nix 系统上可执行文件的标准格式，它取代了 out 格式的可执行文件，原因在于它的可扩展性。ELF 格式的可执行文件可有多个 section（中文里面 section 可以叫做节也可以叫段，而 segment 亦然，为避免歧义，这里坚持用英文表示）。DWARF（Debugging With Attribute Record Format）是经常碰到的名词，它在 ELF 格式的可执行文件中。

ELF 文件有三种不同的形式。

- Relocatable: 由编译器和汇编器生成，由 linker 处理它。

# 深入浅出LinuxTCP/IP协议栈

- Executable: 所有的重定位和符号解析都完成了, 也许共享库的符号要在运行时刻解析。  
- Shared Object: 包含 linker 需要的符号信息和运行时刻所需的代码。

ELF 文件有双重性质：一方面，编译器、汇编器、连接器都把它看作是逻辑段（sections）的集合，另一方面 loader 把它看作段（segments）的集合。Section 是给 linker 做进一步处理的，而 segments 是被映射到内存中去的。系统如何创建一个进程的内存映像是通过一个程序头表（program header table）确定的，一个 segment 可以由几个 sections 组成。为了定位不同 segment/section，可执行文件用一个 table 来记录各个 segment/section 的位置和描述，一个 section 头表（section header table）包含了描述文件 sections 的信息。每个 section 在这个表中有一个入口，每个入口给出了该 section 的名字，大小等信息。Relocatable 有 section table, Executable 有 program header table。而 Shared Object 两者都有。

![](images/8a2af48f8c81ce4813b2bb08a1fe142b2207b034e3774e161b237239b6c83fc3.jpg)  
图2-5 ELF文件格式

图2-5显示了从不同的角度来解释segment和section的不同。

当 as 生成一个目标文件时，它假设程序段是从地址 0 开始，Id 则把最后的地址赋给这个程序段，以至于不同的程序段不会相互覆盖。Id 把程序移动到各自的运行时地址，指定 section 的运行时地址叫重定位（relocation）。

as 输出的目标文件至上有 3 个 section，任何一个都有可能为空，它们是 text、data、bss 段。可以不写诸如.text 或.data 段，但目标文件中还是存在这些段，只不过是空的，在目标文件中段是如图 2-6 所示。

![](images/db02587c6793bace37322300e443b0519d241d04563ab3b9e495233c1d2ff9b3.jpg)  
图2-6 普通的ELF段排列

为了让 Id 能正确重定位各段, as 生成一些重定位所需的信息。

实际上as用的每一个地址都是以这样的形式：（section）+（offset into section）表示，ld把所有相同的section放到连续的地址里。还可以用subsections把一个大的section分成多个小的section。可以用标号来区分，这里就不细说了。

普通情况下ld处理4种段：

```txt
named section: text section data section 
```

(这两个段放着用户的程序, 它们是分开的但是却是相等的段。只是在运行的时刻, text 段不能被改变)

```txt
bs8 section absolute section 
```

（这个段的0地址总是被重定位到运行地址0）

```txt
undefined section 
```

（用来放置不在前面几个段里的数据）

对于一个在 text、data、bss 中的符号而言，它的值就是从段首到它的偏移，于是，当 ld 在连接各段时就改变了 label 的值。

对于没有定义（undefined）的值，ld 尽量从外部其他文件引入并确定其值。

不同的 sections 保存着程序和控制信息。下面列表中的 section 被系统使用，指示了类型和属性。

data: 这个 sections 保存着初始化了的数据, 那些数据存在于程序内存映像中。  
- bss: 该sectiopn保存着未初始化的数据，这些数据存在于程序内存映像中。通过定义，当程序开始运行，系统初始化那些数据为0。该section不占文件空间。  
- .comment: 该 section 保存着版本控制信息。  
• init: 该 section 保存着可执行指令, 它构成了进程的初始化代码。因此, 当一个程序开始运行时, 在 main 函数被调用之前 (C 语言称为 main), 系统安排执行这个 section 的中的代码。  
- .text: 该 section 保存着程序的 “text” 或者说是可执行指令。  
dynamic: 该 section 保存着动态连接的信息。  
-dtnstr: 保存动态连接时需要的字符串。  
-dynsym: 保存动态符号表如“symbol table”的描述。  
- interp: 保存程序的解释程序（interperter）的路径。  
- line: 包含编辑字符的行数信息, 它描述源代码与机器代码之间的对应关系。  
- rel<name>和.rela<name>: 保存重定位的信息。  
- .rodata 和 .rodatal: 保存只读数据, 在进程映像中构造不可写的段。

前缀是点（）的section名是系统保留的，

虽然存在许多类型的ELF节，但就链接编辑阶段而言可将所有节都归为两种类别：

(1) 包含程序数据的节, 其解释仅对应用程序有意义, 如程序指令.text 以及关联的数据.data 和.bss。

(2) 包含链接编辑信息（如.symtab 和.strtab 中的符号表信息以及诸如.relax 的重定位信息）的节。

# 2.2.2 Link Scripts 知识

当创建或增加一个进程映像的时候，系统在理论上将复制一个文件的段到一个虚拟的内存段。我们通常有一个疑问，为什么编出来的代码肯定是在用户地址空间运行，而内核编出来的代码却一定是运行在内核空间？如果我们把目光仅仅盯在 C 文件或 h 文件甚至 Makefile，估计想破脑袋也不知道为什么会这样。其实我们经常忽视了链接器的作用。不能简单地认为链接器仅仅完成将各 obj 文件拼在一起的任务，它还指定每个段被装入内存的真正地址。

链接器（Linker）其实有自己的一套语言规范，其目的是描述输入文件中的 sections 是如何映射到输出文件中，并控制输出文件的内存排列。如果你从来没有看到过 ld script，那么请用 ld -verbose 查看输出结果，那就是 ld script。只是它是内置在链接器中，而且 ld 就是使用这个缺省的 script 去生成输出我们平时应用程序 obj，所以如果是用缺省的 ld script 生成内核，那么它肯定也只能运行在用户空间。

我们已经知道每一个目标文件有一个 sections 的列表，在输入文件 input section 中叫 output section，每个 section 有名字和大小。大多数 section 有相关的数据块，就是 section contents。一个 section 可以标记为 loadable，意味着输出文件在运行时可以把这一 section 装入内存。没有内容的 section 可以叫 allocatable，表示这块区域放在内存的某个地方，但没有什么特殊的东西放在里面（一般都是被初始化为 0），一个既不是 loadable 也不是 allocatable 的 section 一般是包含一堆调试信息。

每一个 loadable 和 allocatable 的 section 有两个地址。第一个是 VMA，即虚存地址。这是输出文件运行时的地址。第二个是 LMA，即装入内存地址。这是 section 被装入的地址。在多数情况下，这两个地址是相同的。它们不同的例子是：当数据 section 被装入 ROM，当程序开始执行时被复制到 RAM（这个技术通常用来初始化基于 ROM 系统的全局量）。

我们常会见到 AT 命令, 这个命令就是改变 section 的加载地址 (LMA) 的值。跟在关键字 “AT” 后面的表达式 IMA 指定节的载入地址。

可以用dumpobj -h去查看目标文件的section信息，每个目标文件有符号表，每个符号有一个名字，且每个有定义的符号有一个地址及其他信息。可以用nm查看符号表信息，也可以用objdump-t命令查看。

最简单的 script 只有一个 SECTIONS 命令。它描述输出文件的内存排列。

例子：

SECTIONS $\cdot = 0\times 10000$ /\*代码被装入到此地址\*/.textSIZEOFHEADERS:（\*（.init）/\*放在init段的代码\*/\*（.text）/\*放在text段的代码\*/\*（.fini）/\*放在fini段的代码\*/

$= 0\times 8000000$ ：/数据被装入到此地址\*/.data：（（.data））.bss：（.bss）

在上例中，第3行的“.”是一个特殊符号，用来做定位计数器。它根据输出段的大小增长。在SECTIONS开始时它等于0。

“*”是一个通配符，匹配所有的文件，表达式“*（.text）”表示所有输入文件的“.text”段。输出文件的代码段包含所有输入文件的.init 和.text 及.fini。在普通应用程序二进制文件中，init 和.fini 是编译器为用户代码加入的，其内容根据操作系统的不同而不同。它们分别提供运行时初始化代码块和终止代码块，编译器通常提供.init 和.fini 节以及添加到输入文件列表开头和末尾的文件。当然，您也可以提供自己的初始化代码和终止代码，但必须保证正确封装和标记此代码，以便运行时链接程序可以正确识别并使用代码。那么请在这里记住，Linux 内核就是通过提供特殊的初始化代码使内核成其为“内核”，我们下一节会看到。

“.data”段的起始位置在0x80000000，在linker放置“.data”后，定位计数器的值等于0x8000000加上“.data”的大小。然后linker会把.bss段放在.data之后。注意：linker可能会在.data和.bss段之间划出一个gap。

程序中执行的第一条指令叫entry point，可以用ENTRY指定入口点。如ENTRY（symbol）, linker有几种方法设置入口点：

（1）在命令行中输入-e entry。  
（2）在linkerscript文件中指定ENTRY（symbol）。  
（3）如果定义了start，则start的值就是入口点。  
（4）.text的第一个字节。  
（5）地址0。

在一些目标文件里，公共符号不属于某个特别的段，linker 认为它们属于一个叫 COMMON 的段，大多数情况下，输入文件里的公共符号被放在输出文件的.bss 段中，如

```txt
.bas \*(.bss) \* (CONNON) 
```

# 2.2.3 Linux 内核镜像解析

下面我们就拿Linux内核源代码作为印证以上理论的例子，见代码段2-2。先回顾include/linux目录下这么一个init.h文件，它不仅定义了initcall_t类型变量，还定义了一些常规C语言编程中未见过的类型。

代码段2-2 init.h  
1. $\text{一} ^ { \text{一} }$ 用法：  
2. \*对于函数，应该在函数名之前加一个_init，如下：  
3. \*  
4. static void _init initme (int x, int y)  
5. \*  
6. extern int z; $z = x\ast y$ 7. \*  
8. \*如果函数在其他地方有原型，那么可以在括号和分号之间加_init，如下：  
9. \*extern int initialize_foobar_device(int, int, int)_init;

```txt
10. \*   
11. \*对于初始化的数据，应该在变量和等号之间加一个_initdata，如下：   
12. \*static int init_variable __initdata = 0;   
13. \*static char linux_logo[] __initdata = (Ox32,0x36,...);   
14. \*记住不要在文件范围以外初始化数据，比如在函数中，否则gcc会把这些数据放在bss段中而不是init   
段中   
15. \*   
16. \*而且要注意：这些数据不能是const类型   
17. \*/   
18. /\*这里要碰到一个_attribute关键字，这是告诉编译器要做一些特殊的操作。在这里，它和_section_ 共同指示凡是被它们修饰的函数或变量应该放在特殊的section中，不能任由编译器自己决定这些函数放在哪\*/   
19. #define __init __attribute_（（section_（".init.text"）））   
20. #define __initdata __attribute_（（section_（".init.data"）））   
21. #define __exit data __attribute_（（section_（".exit.data"）））   
22. #define __exit __call__ attribute __used__ attribute_ （（section_（".exit.text"）））   
23.   
24. #define __sched __attribute_（（section_（".sched.text"）））   
25.   
26. #ifdef MODULE   
27. #define __exit __attribute_（（section_（".exit.text"）））   
28. #else   
29. #define __exit __attribute __used__ attribute_ （（section_（".exit.text"）））   
30. #endif   
31.   
/\*下面三行是汇编指令\*/   
32. #define _INIT .section ".init.text", "ax"   
33. #define _FINIT .previous   
34. #define _INITDATA .section ".init.data", "aw"   
/\*系统中没有定义_ASSEMBLY 室，所以会编译下面的代码\*/   
35. #ifndef _ASSEMBLY_   
/\*这就是前面我们提到的函数类型定义，它是一个参数为空，返回值为int类型的函数，还有exitcall_t 类型函数类型定义，参数为空，返回值也为空。它们使用了标准的 typedef用法，没有什么可以研究的\*/   
36. typedef int (*initcall_t) (void)；   
37. typedef void (*exitcall_t) (void)；   
38.   
39. extern initcall_t __con_initcall start, __con_initcall_end;   
40. extern initcall_t __security __initcall start, __security __initcall_end;   
41. #endif   
42.   
43. #ifndef MODULE   
44.   
45. #ifndef _ASSEMBLY_   
46.   
/\*这些初始化函数被分成了几组，组内的执行顺序由编译器在连接阶段决定，考虑到向后兼容。 _init_call 被定义成设备初始化subsection。   
\*/   
47.   
48. #define _define_initcall(level,fn) \\   
49. static initcall_t __initcall__#fn __attribute __used_ \\   
50. __attribute_（（section_（".initcall" level“.init"））= fn   
51.   
52. #define core_initcall (fn) _define_initcall("1",fn)   
53. #define postcore_initcall (fn) _define_initcall("2",fn)
```

```txt
54. #define arch_initcall (fn) __define_initcall ("3", fn)
55. #define subsys_initcall (fn) __define_initcall ("4", fn)
56. #define fe_initcall (fn) __define_initcall ("5", fn)
57. #define device_initcall (fn) __define_initcall ("6", fn)
58. #define late_initcall (fn) __define_initcall ("7", fn)
/* 我们常见的__initcall宏实际指的是device_initcall，它是第6个子section */
59. #define __initcall (fn) device_initcall (fn)
60.
61. #define __exitcall (fn) \
62. static exitcall_t __exitcall__##fn __exit_call = fn
63.
64. #define console_initcall (fn) \
65. static initcall_t __initcall__##fn \
66. __attribute_used __attribute_((section_("con_initcall-init"))))=fn
67.
68.
69. struct obs_kernel_paran (
70. const char *str;
71. int (*setupFUNC)(char *);
72. );
73.
74.
75. #endif /* ASSEMBLY */
76.
/* module_init() - 声明驱动程序初始化入口函数的宏，它实际就是上面提到的device_initcall
@x: 此参数是内核boot时或将模块插入内核时将要调用的驱动程序函数
凡是被module_init()“修饰”过的函数只能在两种情况下被调用：一种是被do_initcalls调用，
一种是在模块插入到系统中时被调用（如果它是模块方式），每个模块只有一个被module_init修饰的函数入口
*/
77. #define module_init (x) __initcall (x);
78.
/* module_exit() - 声明驱动程序退出函数的宏,
@x: 此参数是驱动程序被卸载时内核将要调用的函数
module_exit() 会封装驱动程序的clean-up代码，如果驱动程序静态编译到内核中，这个宏没有意义。
每个模块也只有一个被module_exit修饰的函数
*/
79. #define module_exit (x) __exitcall (x);
80.
81. #else /* MODULE */
82.
83. /* Don't use these in modules, but some people do... */
84. #define core_initcall (fn) module_init (fn)
85. #define postcore_initcall (fn) module_init (fn)
86. #define arch_initcall (fn) module_init (fn)
87. #define subsys_initcall (fn) module_init (fn)
88. #define fs_initcall (fn) module_init (fn)
89. #define device_initcall (fn) module_init (fn)
90. #define late_initcall (fn) module_init (fn)
91.
92. #define security_initcall (fn) module_init (fn)
93.
/* 每个模块都必须使用一个module_init */
94. #define module_init (initfn) \
95. static inline initcall_t __inittest (void) \
96. | return initfn;
```

```c
97. int init_module(void) __attribute__ (alias (#initfn));  
98.  
99. /* This is only required if you want to be unloadable. */  
100. #define module_exit (exitfn)  
101. static inline exitcall_t __exittest (void)  
102. (return exitfn; )  
103. void cleanupModule(void) __attribute__ ((alias (#exitfn)));  
104.  
105. #define __setup-param (str, unique_id, fn) /* nothing */  
106. #define __setup_null-param (str, unique_id) /* nothing */  
107. #define __setup (str, func) /* nothing */  
108. #define __obsolete_setup (str) /* nothing */  
109. #endif  
110.  
/* 下面的宏开关意味着如果不支持模块启动，那么系统启动时可能会执行初始化，否则在加载模块的时候初始化*/  
111. #ifdef CONFIG MODULES  
112. #	define __init_or_module  
113. #	define __initdata_or_module  
114. #else  
115. #	define __init_ormodule __init  
116. #	define __initdata_ormodule __initdata  
117. #endif /*CONFIG MODULES*/  
118.  
119. #ifdef CONFIG_HOTPLUG  
120. #	define __devinit  
121. #	define __devinitdata  
122. #	define __devexit  
123. #	define __devexitdata  
124. #else  
125. #	define __devinit __init  
126. #	define __devinitdata __initdata  
127. #	define __devexit __exit  
128. #	define __devexitdata __exitdata  
129. #endif  
130.  
131. /* 根据配置选项，被标识为__devexit的函数可能会在编译内核的连接阶段被丢弃。  
132. */  
133. #if defined (MODULE) || defined (CONFIG HOTPLUG)  
134. #	define __devexit_p(x) x  
135. #else  
136. #	define __devexit_p(x) NULL  
137. #endif  
138.  
139. #ifdef MODULE  
140. #	define __exit_p(x) x  
141. #else  
142. #	define __exit_p(x) NULL  
143. #endif 
```

这个文件有一些关于section的定义比如在第16行和30行，所以本质上initcall_t及其他类型的类型变量实际是宏，通过对这个文件的宏替换可以大概了解这些宏的含义。例如，代码中如果含有_initXXX()这么一个函数定义，就会知道那个XXX函数属于初始化的时候就

被调用的，它被放在.init.text节中。

前面说到 ELF 文件格式时曾说了 Link Script 会把特定类型的段放在特定位置让 loader 装入到特定的内存地址, 那么对于被 init 等宏修饰的函数及全局变量肯定被放在了特定位置, 但如何让 basic_init 函数去调用它, 我们还得再分析 arch/i386/kernel 目录下的 vmlinux_lds.S 文件, 它就是使内核成为内核的 ld script, 其主要目的是对输出文件中段进行排序, 并定义相关的符号名。在这个文件中便定义了 __initcall_start 和 __initcall_end。记住了, 不止是 C 文件和 H 文件可以定义变量。而且也不是像 C 语言那样定义一个变量还要指定类型, 编译器有足够的智商把这个文件中的变量定义为需要的类型, 一般情况下会定义为整型变量。

我们还得再分析arch:/386/kernel目录下的vmlinux.lds.S文件，见代码段2-3。

代码段 2-3 vmlinux.Ids.S Linux 内核 Id scripts  
/\*正是这个ld script创建了Linux内核\*/   
1.   
2.#include <asm-generic/vnlinux.Ids.h>   
3.#include <asm/thread_info.h>   
4.   
5.#include<linux/contig.h>   
6.#include<asm/page.h>   
7.#include<asm/asm-offsets.h>   
8.   
9.OUTPUT.Format("elf12-1386","elf32-1386","elf32-1386")   
10.OUTPUT_ARCH(i386）/\*输出格式是i386的\*/   
11.ENTRY（startup_32）/\*定义startup_32作为入口点\*/   
/\*下面的jiffies是本书经常遇到的全局变量，在kernel2.4，jiffies是32位无符号数：kernel2.6, jiffies是64位无符号数，而后者是在timer.c中定义的: u64 jiffies_64_cacheline_aligned_in_smp $\equiv$ INITIAL_JIFFIES;每次它在时钟中断函数do_timer (）里加1。因为1秒钟内增加的时钟中断次数等于Hz（在普通PC机的Linux内核中定义了Hz为100，也就是说ls 内有100个时钟中断到达系统），所以jiffiesl秒内增加的值也是Hz，而在I386架构下，Hz一般为100，所以每 秒钟jiffies就加上100.   
\*/   
12.jiffies $=$ jiffles_64;   
13.SECTIONS   
14.(   
15. $\cdot =$ _PAGE_OFFSET+0x100000；定义当前段的偏移量   
16./代码段必定是只读的\*/   
17._text=.; /\*定义符号_text为当前位置\*/   
18.text{   
19.\*(.text)/\*将所有输入文件中.text段合并到这里，下面以此类推\*/   
20.SCHED TEXT   
21.\*(.fixup)   
22.\*(.gpu,warning)   
23. $\mid = 0\times 9090$ /\*合并中的空隙用0x9090填充\*/   
24.   
25._entry.text:\{*\*.entry.text）}   
26._text=.; /\*text section结束\*/   
....../\*只读数据段\*/   
27.RODATA   
28.   
29.\*/可写数据段\*/   
30._data:\{ /\*Data\*/

```txt
31. \*(.data)   
32. CONSTRUCTORS   
33. 1   
/ \(^ { \text{喜} }\) ALIGN（exp）关键字计算当前\*."值对齐到exp边界后的地址\*/   
34. . \(=\) ALIGN(32);   
35. .data.cacheline_aligned: \(\begin{array}{rl}{[\ast(\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot\cdot} & \\ _edata \(= \ldots\) /\*数据section在此结束\*/   
37. . \(=\) ALIGN（THREAD_SIZE）;/\*init_task\*/   
38. .data.init_task:\{ \*(.data.init_task）]   
39.   
40. /\*will be freed after init \*/   
41. . \(=\) ALIGN（PAGE_SIZE_asm）； /\*下面是初始化代码和数据\*/   
42. _init_begin \(= \ldots\)   
43. .init.text：[   
44. _sinittext \(= \ldots\)   
45. \*（.init.text）   
46. _einittext \(= \ldots\)   
47. 1   
48. .init.data: \(\left[\ast (\text{，init.data})\right]\)   
49. . \(=\) ALIGN(16);   
50. _setup_start \(= \ldots\)   
51. .init_setup: \(\left[\ast (\text{，init_setup})\right]\)   
52. _setup_end \(= \ldots\)   
53. _start param \(= \ldots\)   
54. _param : \(\left[\ast (\text{，param})\right]\)   
55. _stop param \(= \ldots\)   
/\*这就是我们日思夜想的那两个全局变量，其内存安排见图2-7\*/   
56. _initcall_start \(= \ldots\)   
57. .initcall.init: [   
58. \*（.initcall11.init）   
59. \*（.initcall12.init）   
60. \*（.initcall13.init）   
61. \*（.initcall14.init）   
62. \*（.initcall15.init）   
63. \*（.initcall16.init）   
64. \*（.initcall17.init）   
65. 1   
66. _initcall_end \(= \ldots\)   
67..   
......   
68. .exit.text:\*\*.exit.text）]   
69. .exit.data:\*\*.exit.data）]   
70. . \(=\) ALIGN（PAGE_SIZE_asm）;   
71. _per_cpu_start \(= \ldots\)   
72. .data.percpu : \(\left[\ast (\text{，data.percpu})\right]\)   
73. _per_cpu_end \(= \ldots\)   
74. . \(=\) ALIGN（PAGE_SIZE_asm）;   
75. _init_end \(= \ldots\) ：  
/\*以上的内存段在初始化结束之后（即这些代码被执行一次以后）就被释放出来\*/   
76....   
/\*这里存放未初始化的数据\*/   
77. __bsa start \(= \ldots\) /\*BSS\*/   
78. .bss : [
```

80. \*（.bss）  
81. 1  
82. .-ALIGN（4）;  
83. __bsstop $=$ .;  
84.  
85. _end $=$ .;  
86.  
87. /\*这个地方时内核创建早期boot页表的起始位置\*/  
88. .= ALIGN（4096）;  
89. pgD $=$ .;  
90.  
91. /\*这个section会故丢弃\*/  
92. /DISCARD/；  
93. \*(.exitcall.EXIT)  
94.   
95.  
96. /\*关于debug信息的sections，省略\*/  
97.   
98.

看到了吗？当编译器编译整个源代码的时候，它会把所有定义为__init的函数放在以__initcall_start开始、以__initcall_end结束的节中，在basic_init中会逐个的调用该节里所有的函数。

![](images/9c9fde5c255807c700d9b160d85fbb18f04408953d3f42ed44ee4490a90b7b69.jpg)  
图2-7 内核可执行文件磁盘和内存镜像

不信？让我们编译完内核，在源文件根目录使用 objdump -t vmlinux |grep_initcall 输出信息，就看到我们想看到的东西（代码段 2-4 所示是经过处理的符号信息）。

# 代码段2-4 内核镜像输出init的打印

```txt
1. c12946d4 1 0 4 __initcall_cpfreq_tsc 第一个被初始化的函数，它的地址正好是__initcall_start 的地址
```

```txt
2. 1 0 4 __initcall_keysfs_init
4. 1 0 4 __initcall_sock_init 网络基础设施比如内存系统的初始化
5. c12946fc 1 0 4 __initcall_netlinkProto_init
6. c1294700 1 0 4 __initcall_netlink Proto_init
7. 1 0 4 __initcallProto_init
8. c1294770 1 0 4 __initcall_net_dev_init 网络设备的初始化
9. c1294774 1 0 4 __initcall_wireless_nlevent_init
10. c1294778 1 0 4 __initcall_wireless_nlevent_init
11. c129477c 1 0 4 __initcall_pktsched_init
12. 1 0 4 __initcall_init_pipe_FS 管道系统的初始化
13. c129478c 1 0 4 __initcall_init_chr_init 字符设备的初始化
14. c1294790 1 0 4 __initcall_time_init_device
15. 1 0 4 __initcall_8259A_init_sysfs 中断控制器的初始化
16. c12947a4 1 0 4 __initcall_init_posix_TIMER 定时器时钟初始化
17. c12947a8 1 0 4 __initcall_time_init_device
18. 1 0 4 __initcall_8259A_init_sysfs 中断控制器的初始化
19. c12947b0 1 0 4 __initcall_8259A_init_sysfs 中断控制器的初始化
20. 1 0 4 __initcall_init_posix_TIMER 定时器时钟初始化
21. c12947e8 1 0 4 __initcall_init_posix_TIMER 定时器时钟初始化
22. 1 0 4 __initcall_eventpoll_init 事件轮讯机制初始化
23. c1294848 1 0 4 __initcall_eventpoll_init 事件轮讯机制初始化
24. 1 0 4 __initcall_init_ext2_FS EXT2 文件系统的初始化
25. c1294864 1 0 4 __initcall_init_ext2_FS EXT2 文件系统的初始化
26. c1294888 1 0 4 __initcall_init 这不是 init/main.c 文件中的初始化函数
27. 1 0 4 __initcall_pcl_init PCI系统总线的初始化
28. c12948a8 1 0 4 __initcall_pcl_init PCI系统总线的初始化
29. c12948ac 1 0 4 __initcall_pcl_systems_init 老实的网络设备驱动程序的初始化
30. c12948b0 1 0 4 __initcall_pcl_process_init
31. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . ..
32. c1294918 1 0 4 __initcall_topology_sysfs_init
33. c12949c 1 0 4 __initcall_rd_init
34. c1294920 1 0 4 __initcall_net_oldeva_init 老实的网络设备驱动程序的初始化
35. ......
36. c1294974 1 0 4 __initcallGeneric_Ide_init IDE硬盘设备的初始化
37. .. .
38. c12949b4 1 0 4 __initcall_movedev_init 鼠标设备的初始化
39. ......
40. c12949e0 1 0 4 __initcall_arUnix_init
41. ......
42. c12949e4 1 0 4 __initcallpacket_init
43. ......
44. c1294a30'1 O O _initcall_net_random_reseed 最后一个被调用的初始化函数，它的地
址正好是__initcall_end的前面4个字节
下面是一些处于初始化section的函数，特地在这里列出
20.
21. c12946d4 g "ABS" 00000000 _initcall_start" 这就是我们要找的那个
22. c1294a34 g "ABS" 00000000 _initcall_end" 全局变量，请注意它们的地址
```

```csv
30. c03f5f90 1 d ".security_initcall.init 0000000"  
31. ...  
32. c03d0510 1 F ".init.text 00000025 init_setup"  
33. c03f5840 1 O ".initsetup 0000000c _setup_init_setup"  
34. c0100220 1 F ".text 0000002a rest_init"  
35. c0100290 1 F ".text 00000151 init"  
36. c03d0570 1 F ".init.text 00000091 do_early_parans"  
37. c03d0840 1 F ".init.text 000000bc do_initcalls"  
38. c03d0900 1 F ".init.text 000001f do基礎_setup"  
39. c0100260 1 F ".text 0000029 run_init_process"  
40. c010b090 1 F ".text 00000127 init_intel"  
41. c01c8340 g F ".text 0000094 kobject_init"  
42. c0248e90 g F ".text 0000089 device_initilize"  
43. c02f09c9 g F ".text 000001lc tcp_select_initial_window"  
44. c03e93f9 g F ".init.text 0000053 loopback_init"  
45. c01254d9 g F ".text 0000015 init_timer"  
46. c03d7aao g F ".init.text 0000062 init_IRQ"  
47. c03eb65o g F ".init.text 000006d skb_init"  
48. c03df27o g F ".init.text 00003eb knem_cache_init"  
49. c031f15o g F ".text 0000033 klist_init"  
5O. a58o8bbf g F "ABS* 00000OO tasklet_init"  
51. cO2e57cO g F ".text 0OooOOO7a inet_cak_init_xmit_TIMER"  
52. cO39ba8o g O ".data 0OooOOO48 tcp_init_congestionOps"  
53. cO3dd59o g F ".init.text 0OooOOO27 init timers"  
54. cO12d46o g F ".text 0OooOOO1f init_work queues"  
55. cO3dd41o g F ".init.text 0OooOOO1f softirq_init"  
56. cO3ac4Dg G F ".init.text 0OooOOOa tcp4Proc_init"  
57. cO3ab99o g F ".init.text 0OooOOB9 rtnetlink_init"  
58. cO12leFg G F ".text 0OooOOO1b tasklet_init"  
59. cO3ecOdo g F ".init.text 0OooOO329 tcp_init"  
6O. 45e55588 g F "ABS* 0OooOOOO _inet_csk_init_xmit_TIMER"  
6I. cO377b4c g O ".data 0OooOOO14 _init_timer_base"  
62. cO1faooo g F "ABS* 0OooOOOO _init_end"  
63. cO3dd43o g F ".init.text 0OooOOO2e spawn_ksoftirqd"  
64. cO3f266o g F ".init.data 0OooOOOO vsyscall_int8O_end"  
65. cO3ecO9o g F ".init.text 0OooOOOF ip_init"  
66. cO2f3e5o g F ".text 0OoooOLb tcp_init_xmit_TIMER"  
67. cO3dO67o g F ".init.text 0Oooo1b8 start_kernel"  
68. cO3dd57o g F ".init.text 0Oooo1a eyectl_init"  
69. cO3ebd4o g F ".init.text 0Oooo2ab ip_rt_init"  
7O. cO3ecB9o g F ".init.text 0OoooOOa fib Rules_init"  
7I. cO3d64o g F ".text 0OoooOEe fib hash_init"  
72. cO3ec4Dg G F ".init.text 0OoooOS arp_init"  
73. cO2ea28o g F ".text 0OoooOO3d tcp_init_cwnd"  
74. cO3ec4IO g F ".init.text 0OoooOO85 tcp_v4_init"  
75. cO3edO5c G O ".init.data 0OoooOO4 root_device_name"  
76. cO3ecSeo G F ".init.text 0OoooOO4o devinet_init"  
77. aeOoo5cb g F "ABS* 0OoooOO _tcp_init_xmit_TIMER"  
78. cO3eb6cG G F ".init.text 0OoooOO97 netdev.boot_setup"  
79. cO2cf59o G F ".text 0Oooo27d neigh_table_init"  
8O. 9a72c28g F "ABS* 0OoooOO _tcp_init_congestion OPS"  
8I. cO3dd38o g F ".init.text 0DoooOO47 profile_init"  
82. cO3e8bbQ g F ".init.text 0DoooOOa devices_init"  
83. 231bbdfIg F "ABS* 0DoooOO _neigh_table_init"  
84. cO3f1ddQ g "init.data 0DoooOO O vyscall int8O start" 
```

```txt
85. c03e0ae0 g F ".init.text 000000a init_rootfs"  
86. c01064c0 g F ".text 000000Ba init_8259A"  
87. lac12162 g "ABS" 0000000 _inode_init_once" 
```

说明：第一列是所有符号的装入地址。第二列表示符号的全局性和可读写性，1表示local，凡是用static修饰的都是这种属性；g表示global，内核源代码中全局变量属于这一类，还有用EXPORT_SYMBOL（这个宏在linux/module.h中定义）修饰的变量和函数也都是属于global；w表示writable。第三列表示此符号的本质是什么，O表示是变量（一般是全局变量），F表示函数，d表示可执行文件中的section。第四列和第五列比较复杂，如果符号属于d，那么第五列表示该section默认值，一般都为0；如果属于O或F，那么第四列表示符号所属的section，第五列则表示该符号所占用的内存大小。还有一类最特殊，就是用ABS修饰的符号，“ABS*表示绝对（absolute），这意味着不能将该值更改为其他的连接，我们关注的__initcall_start和__initcall_end就属于这一类。所以do_initcalls就是用来调用所有使用__initcall标记过的函数的。

系统初始化各模块有两种方式，我们一般都可以在编译内核的时候控制，这两种方式分别是：一种是嵌入内核中的，另一种是以模块加载方式。前者将设备驱动模块嵌入整个Linux内核（vmlinux）中，系统启动的时候会从.init代码段执行它们的初始化函数，所以我们可以在上面那个vmlinux文件中找到_initcall_xxx_drv_init函数。以上就是关于这种方式的分析。后一种就是将设备驱动模块编译成独立的可执行文件，以.ko为后缀名放在/lib/modules/2.6.xxxx/kernel/目录下。当系统启动时，内核启动代码执行/etc/rc.d/rc.sysinit教本，其中的代码会执行sys_init_module内核函数把它们加载到内核中。

不管是什么方式加载，每个驱动程序执行的第一行代码是以module_init定义的函数。比如module_init（e100_init_module），这里第一行被系统执行的函数是e100_init_module。

从很多的资料上得出一个信息: 凡是用__init 修饰过的函数在被调用一次后, 其占用的内存区会被清除掉, 以便让其他代码可以使用。我没有时间来印证, 希望有读者可以给出正确的答案。

# 2.3 中断及任务调度管理

Linux书籍中常说的BottomHalf已然不见了，它们被转成tasklets，这是支持SMP的。但其思想基本一致。

# 2.3.1 中断及软中断模型

在此不会对中断及异常的原理和机制做深入的介绍。但必须要作出一些说明，因为这是理解Linux内核与其他嵌入式/实时操作系统的不同之处，以及理解网络协议栈收报文的基础。

Linux 支持 CPU 的外部硬件中断和内部中断。严格来说，内部中断包含系统调用陷入和异常，在一般的嵌入式操作系统（比如 VxWorks）中是没有系统调用这个概念的，所以对于

一直从事嵌入式软件开发的人初次进入到大型操作系统（比如 Linux 和 Windows）开发环境中时，会面临内核空间与用户空间概念上的困惑。其实说到底，所谓系统调用就是软件有计划地调用 CPU 提供的特殊指令，触发 CPU 内部产生一个中断，于是完成一次核内与核外运行空间的切换，具体可以参考其他书籍。而所谓异常就是软件无意的执行了一个非法指令（比如被 0 除）从而造成 CPU 内部引发一次中断。

外部中断特指外部设备发出的中断信号。但这几种中断的CPU处理过程基本相同，即：在执行完当前指令后，或在执行当前指令期间，根据中断源所提供的“中断向量”，在内存中找到相应的ISR（中断服务例程）然后调用之。

不管是内部还是外部中断，系统都会根据接收到的中断信息，查询idt表。idt表依照中断源的位置按序组成，并对应中断服务程序（以及异常处理程序）的入口地址。Linux系统在初始化页式虚存管理的初始化以后，便调用trap_init和init_IRQ两个函数进行中断机制的初始化。我们只介绍init_IRQ。

# 1. 中断系统和软中断

在很多书中都提到这两个名词，开始我一直没看懂这些书想表达的含义，后来有一天我发现我居然顿悟了。就让我试图给大家解释一下，看看是不是这样的。IRQ 是设备相关的号码，一般生产厂商都会使自己的设备分配到一个合适的号码。而中断向量就纯粹是操作系统中关于如何处理中断的内存组织结构，它们之间存在某种映射关系，这种关系是由 CPU 体系结构以及操作系统决定的。那么在 IA32 体系的 Linux 中，是一种直接映射的关系，所有的 IRQ 号产生的中断全部映射到从 INT_vec[32]开始的内存中。为什么要从第 32 个单元开始呢？

![](images/3a68365d91c22da89c798ae133d3cbb2e716ce2354a33dd6c4aba813fff9f8f9.jpg)  
图2-8 中断向量和中断请求号之间的关系

图2-8中彩色部分都是系统能处理的中断，intelCPU保留使用的中断向量是 $0\sim 32$ ，根

本不可能有哪一种设备会使用这个区域的中断向量, 这一部分就是我们常说的异常处理函数,还有一个比较特殊的中断向量号 0x80 (即 128) 就是系统调用号, 由于不可能由外部设备引发这类中断, 它们就被统称为内部中断, 这就是为什么要从第 32 个单元开始的原因。内核用调用 trap_init 函数挂接与之相应的中断处理函数。接着系统调用 init_IRQ 函数来初始化外部中断向量, 其中断处理函数的挂接由各驱动程序自己完成。由图 2-8 中可以看出中断向量和中断请求号是相关但却不是一个东西: 前者是内核中存在的一块内存, 专门存放中断处理函数的地址 (指逻辑上, 具体实现比较复杂), 而后者就是一个概念, 在内核中不必存在这么一种变量, 它可能就是中断向量的下标。

在 Linux 2.6 的内核中，中断相关的宏已经变化了，Linux 2.4 内核中的中断概念请看《Linux 内核源代码情景分析》。在 Linux 2.6 内核的 entry.s 文件中，有一个 interrupt 的定义，它放在 data 节中，然后，在 include/asm-i386/hw_irq.h 中引用这个变量，最后在 arch/i386/kernel/i8259.c 中初始化这个变量。

代码段 2-5 两个代码片断是 Linux 2.4 内核关于这个 interrupt 变量的初始化。

代码段2-5 Linux2.4中断定义宏  
```c
1 #define IRQ(x,y)  
2 IRQx#yInterrupt  
3  
4 #define IRQLIST_16(x)  
5 IRQ(x,0), IRQ(x,1), IRQ(x,2), IRQ(x,3),  
6 IRQ(x,4), IRQ(x,5), IRQ(x,6), IRQ(x,7),  
7 IRQ(x,8), IRQ(x,9), IRQ(x,a), IRQ(x,b),  
8 IRQ(x,c), IRQ(x,d), IRQ(x,e), IRQ(x,f)  
9  
10 void (*interrupt[NR_IRQS])(void) =  
11 IRQLIST_16(DxO), 
```

经过编译器的预处理，interrupt这个函数指针数组变成：  
```c
1 void（\*interrupt[NR_IRQS]）（void）-  
2 IRQ0x00_interrupt,  
3 IRQ0x01_interrupt,  
4 IRQ0x02_interrupt,  
5 ...  
6 IRQ0x0f_interrupt,  
7 1 
```

从代码中看出，这样的初始化不太灵活，扩展性比较差。代码段2-6给出Linux2.6内核关于interrupt的使用方式。

首先在entry.S中的汇编代码如下。

代码段2-6 entry.S 中断向量表的定义  
```txt
1 /\*   
2 \*Build the entry stubs and pointer table with some assembler magic.   
3 \*/   
4 .data   
5 ENTRY (interrupt)   
6 .text 
```

```txt
7 注意这里要重复NR_IRQ次，  
8 vector=0  
9 ENTRY（irq_entries_start） #define CONFIG_PCI_M3I  
10 .rept NR_IRQS #define NR_IRQS FIRST_SYSTEM_VECTOR  
11 ALIGN #define NR_IRQVectors NR_IRQS #else  
12 1: pushl \$vector-256 #define CONFIG_X86_IO_APIC  
13 jmp common_interrupt #define NR_IRQS 224 PC机一般都是这个定义  
14 .data #if (224 >= 32 * NR_CPU)  
15 .long Ib #define NR_IRQVectORS NR_IRQS  
16 .text #else  
17 vector-vector+1 #define NR_IRQVectORS (32 * NR_CPU)  
18 .endr #endif  
19 #else  
20 ALIGN #define NR_IRQS 16  
21 common_interrupt: #define NR_IRQVectORS NR_IRQS  
22 SAVE_ALL #endif  
23 movl teap,teax #endif  
24 call do_IRQ  
25jmp ret_from_intr
```

在hw_irq.h中有这样的定义：extern void（*interrupt[NR_IRQS])(void)；在此，NR_IRQS是224。具体的初始化代码如代码段2-7所示。

代码段2-7 18259.c init_IRQ函数  
```c
1 lvoid __init init_IRQ(void)  
2  
3 int i;  
4  
5 /* all the set up before the call gates are initialised */  
6 pre_intr_init-hook();  
7  
8 /*  
9 * 扫描整个中断向量表  
10 */  
11 for (i = 0; i < (NRVectors - FIRST_EXTERNAL_VECTOR); i++) {  
12 int vector = FIRST_EXTERNAL_VECTOR + i;  
13 if (i >= NR_IRQS)  
14 break;  
/* 如果是系统调用中断号，就初始化这个中断号 */  
15 if (vector != SYSCALL_VECTOR)  
16 set_intr_gate.vector, interrupt[i]);  
17 }  
18   
19 */  
20 * 设置时钟频率为HZ(100)，到此为止，我们已经有了一个象样的中断向量：  
21 */  
22 setup_pit_timer();  
23  
24   
25  
26 IRQCtx_init(amp Processor_id());  
27}
```

表 2-1 是关于中断上下文的一些宏, 说明中断处理到达一种什么样的程度。

表2-1  
IRQ状态表  

<table><tr><td>IRQ_INPROGRESS</td><td>1</td><td>当前还在中断上下文中</td></tr><tr><td>IRQ DISABLED</td><td>2</td><td>中断被禁止</td></tr><tr><td>IRQ_PENDING</td><td>4</td><td>中断被挂住</td></tr><tr><td>IRQ_REPLAY</td><td>8</td><td>继续中断处理</td></tr><tr><td>IRQ_AUTODETECT</td><td>16</td><td>自动检测中断请求</td></tr><tr><td>IRQ_WAITING</td><td>32</td><td>对于自动检测中断,此时可能还没有看到中断到来</td></tr><tr><td>IRQ_LEVEL</td><td>64</td><td>使用中断优先级别(Linux没有使用这项特性)</td></tr><tr><td>IRQ_MASKED</td><td>128</td><td>该中断被屏蔽了,将来不希望看到</td></tr><tr><td>IRQ_PER_CPU</td><td>256</td><td>每个CPU都有一个IRQ</td></tr></table>

在include/linuxirq.h文件中，有关于中断控制器的描述，中断控制器描述符包含了所有低层硬件的信息。

其中一个实例是 i8259A_irq_type, 它被定义在 arch/i386/kernel/i8259.c 中:

```c
1 static struct hw_interrupt_type i8259A_irq type = {  
2     "XT-PIC",  struct hw_interrupt_type {  
3         const char * typename;  
4         unsigned int (*startup) (unsigned intirq);  
5         void (*shutdown) (unsigned intirq);  
6         void (*enable) (unsigned intirq);  
7         void (*disable) (unsigned intirq);  
8         void (*end) (unsigned intirq);  
9         void (*set_affinity) (uintirq, cpumask_tdest);  
10 }; 
```

下面这个数组的定义利用了GCC编译器的特点，只定义了一个单元的值，然后使用[0...NR_IRQS-1]使整个数组的值都初始化为同样的值：

```c
1 /* typedef struct iq_desc {
    unsigned int status; /* IRQ状态 */
    hw_irq_controller *handler;
    struct irgaction *action; /* IRQ行为列表 */
    unsigned int depth; /*禁止嵌套irq */
    unsigned int IRQ_count;
    /*用来检测被阻塞的中断*/
    unsigned intIRQ_unhandled;
    spinlock_t lock;
} _chachelineAligned iq_dase_t; 
```

现在让我们从整体上看中断和软中断的处理过程，从图2-9中可以看到do_IRQ是直接被调用的。

每个外部中断都会调用do_IRQ，此函数根据当时的EAX寄存器（i386体系）值来判断当前是哪个IRQ去调用_do_IRQ，如代码段2-8所示。

![](images/6343da1d06eb10c3487325cd8d45c0982850841c3b6052508bcbcffc67d14580.jpg)  
图2-9do_IRQ函数调用树

代码段2-8 handle.c do_IRQ函数  
1 $\text{巧} ^ { \text{巧} }$ 2 \*do_IRQ = original all in one highlevel IRQ handler   
3 \*irq: 就是do_IRQ传入的EAX寄存器值   
4 \* @regs:指向发生中断时寄存器集合   
5   
6 \*do_IRQ 处理所有正常的设备IRQ，它处理每一种中断类型。而特殊的SNF CPU有其自身的处理函数   
7 ++/   
8 fastcall unsigned int _do_IRQ(unsigned intirq,struct ptRegs\*regs)   
9   
10 structirq_desc \*desc $=$ irq_desc $^+$ irq;   
11 structirqaction \*action;   
12 unsigned int status;   
13   
14 if(CHECK_IRQ_PER_CPU desc->status）{   
15 return_t action_rat;   
16 /\*处理IRQ事件，见下文\*/   
17 action_ret - handle_IRQ_event（irq，regs，desc->action）;   
18 return 1;   
19 }   
20   
21 spin_lock (sdesc->lock);   
22   
23 /\*   
24 \*REPLAY is when Linux resends an IRQ that was dropped earlier   
25 \*WAITING is used by probe to mark irqs that are being tested   
26 /\*   
27 status $=$ desc->status&-(IRQ_REPLAY|IRQ_WAITING);   
28 status |= IRQ_PENDING; /\*we_want_to_handle it\*/   
29   
30 /\*   
31 \*如果这个IRQ被某些原因禁止，那么我们不能使用这个action   
32 /\*   
33 action $\equiv$ NULL;   
34 desc->status $=$ status;   
35 ...   
36   
37 /\*   
38 \* Edge triggered interrupts need to remember pending events.   
39 \*This applies to any hw interrupts that allow a second

40 \* instance of the same irg to arrive while we are in do_IRQ   
41 \* or in the handler. But the code here only handles the _second_   
42 \* instance of the irq, not the third or fourth. So it is mostly   
43 \* useful for irq hardware that does not mask cleanly in an   
44 \* SMP environment.   
45 \*/   
46 for ; 1   
47 irqreturn_t action_ret; /\*处理IRQ事件，见下文\*/   
48 action_ret = handle_IRQ_event (irq, regs, action);   
49   
50 desc->status $= -$ IRQ_PENDING;   
51 1   
52 desc->status $= -$ IRQ_INPROGRESS;   
53   
54 out:   
55 ....   
56 return 1;

把 action 传入了 handle_IRQ_event，如代码段 2-9 然后在其中执行 action->handler，此 handle 就是每个设备驱动程序挂接的 ISR。

代码段2-9 handle.c handle_IRQ_event函数  
1 /\*   
2 \*函数返回值告诉我们对此中断是否还有要处理的工作，如果有的话还得告诉软中断机制完成这些工作   
3 \*/   
4 asmlinkage int handle_IRQ_event(unsigned int IRQ,   
5 struct ptRegs *regs, struct irqaction \*action)   
6 1   
7 rqreturn_t ret, retval $=$ IRQ_NONE;   
8 unsigned int status $= 0$ 9   
10 handle_dynamic Tick (action); /\*当调用此函数时看看是否禁止了硬件中断\*/   
11 if(! (action->flags&IRQF_DISABLE))   
12 local_irq_enable_in_hardirq(); /\*注意这里是一个循环，如果某个中断被指定为SHARED. 那么很有可能多个中断服务程序要处理此中断。\*/   
13 do{   
14 ret $=$ action->handler (irq, action->dev_id, regs);   
15 if(ret==IRQ_HANDLED)   
16 status |= action->flags;   
17 retval $| =$ ret;   
18 action $=$ action->next;   
19 while (action);   
20 local_irq_disable();   
21 return retval;

上面介绍的是硬件的中断，而通常说的软件中断（不是软中断）是怎么工作的呢？其实很简单，图2-10所示的那些曲线箭头表示代码的执行路径。要注意的是在socket（）函数的实现过程中，那些mov指令就是告诉内核要跳转的系统调用函数以及用户待传入内核的参

数地址。不同的CPU结构上会使用不同的寄存器，这里就不详细说明了。

![](images/6ddfbbac600f93994061c739f8fdbfc9917fcf803cdbc84854d3acb19e491082.jpg)  
图2-10 系统调用发生的情况

所以，软件中断的处理方式和硬件的处理路径是完全不一样的，它不必经过do_IRQ这个函数，而是直接跳转到内核中的代码执行sys_SOCKETcall。在Linux 2.6.18的内核中，BSD网络接口的方式已经变成了使用sys_SOCKETcall来解复用不同的系统调用，这样做的好处是减少系统调用表的大小，可以集中管理网络方面的API。sys_SOCKETcall函数如代码段2-10所示。

代码段2-10 socketcsys_SOCKETcall函数  
```c
1 asmlinkage long sys_SOCKETcall (int call, unsigned long _user *args)  
2 {  
3 unsigned long a[6];  
4 unsigned long a0, al;  
5 int err;  
6 copy_from_user (a, args, nargs(call));  
7 .......  
8  
9 a0 = a[0];  
10 al = a[1];  
11  
12 switch (call)  
13 {  
14 case SYS_SOCKET;  
15 err = sys_SOCKET(a0, al, a[2]);  
16 break;  
17 case SYS_BIND;  
18 err = sys_bind(a0, (struct sockaddr _user *) al, a[2]);  
19 break;  
20 case SYS_connect; 
```

21 err $=$ sys_connect(a0，(struct sockaddr_user\*）al,a[2]);   
22 break;   
23 case SYS_LISTEN:   
24 err $=$ sys.listen(a0,al);   
25 break;   
26 case SYS_ACCEPT:   
27 err $=$ sys.accept(a0，(struct sockaddr_user\*)al，（int_user\*)   
a[2]）;   
28 break;   
29 .......   
30 case SYS_SEND:   
31 err $=$ sys.send(a0，（void_user\*)al，a[2]，a[3]）;   
32 break;   
33 case SYS_SENDTO:   
34 err $=$ sys.sendto(a0，（void_user\*)al，a[2]，a[3]，   
35 (struct sockaddr_user\*)a[4]，a[5])；   
36 break;   
37 case SYS_recv:   
38 err $=$ sys_recv(a0，（void_user\*)al，a[2]，a[3]）;   
39 break;   
40 case SYS_recvFROM:   
41 err $=$ sys_recvfrom(a0，（void_user\*)al，a[2]，a[3]，   
42 (struct sockaddr_user\*)a[4]，（int_user\*)a[5]）;   
43 break;   
44 .......   
45 case SYSSendMSG:   
46 err $=$ sys_sendmsg(a0，（struct msghdr_user\*）al，a[2]）;   
47 break;   
48 case SYS_recvMSG:   
49 err $=$ sys_recvmsg(a0，（struct msghdr_user\*）al，a[2]）;   
50 break;   
51 default:   
52 err $=$ -EINVVAL;   
53 break;   
54 1   
55 return err;

目前为止，我们讨论的都是真正的中断，那么什么是软中断呢？在中断即将退出的时候会调用irq_exit，它内部会判断是否还有中断要处理，如果已经没有了就调用invokesoftirqq，这是一个宏，它被定义成dosoftirqq，此函数最终调用dosoftirqq，这也就是说，实际上软中断是在处理完所有中断之后才会处理的。而且处理软中断的时候还是处于中断上下文中。不过有一些限制，详见下面对dosoftirqq代码的分析。

在目前 Linux 内核中定义了 6 种软中断, 而且告诫我们不要轻易的再定义新的软中断, 原话如下:

```txt
PLEASE, avoid to allocate new softirqs, if you need not_really_high frequency threaded job scheduling. For almost all the purposes tasklets are more than enough. F.e. all serial device BHa et al. should be converted to tasklets, not to softirqs. 
```

虽然系统中定义了6种软中断，但在start_kernel函数中调用的softirq_init，只初始化了

2个软中断类型，见代码段2-11所示。另外和本书有关系的软中断向量NET_RX_SOFTIRO和NET_TX_SOFTIRO的处理函数将会在net_dev_init中挂接。

代码段2-11 softirq.c softirq_init函数  

<table><tr><td>1</td><td>void __init softirq_init(void)</td></tr><tr><td>2</td><td>{</td></tr><tr><td>3</td><td>opensoftirq (TASKLET_SOFTIRQ, tasklet_action, NULL);</td></tr><tr><td>4</td><td>opensoftirq (HI_SOFTIRQ, tasklet_hi_action, NULL);</td></tr><tr><td>5</td><td>}</td></tr></table>

软中断向量0（即HI_SOFTIRQ）用于实现高优先级的软中断，软中断向量3（即TASKLET_SOFTIRQ）则用于实现诸如Tasklet这样的一般性软中断。

Tasklet 机制是一种较为特殊的软中断。Tasklet 一词的原意是“小片任务”的意思，这里是指一小段可执行的代码，且通常以函数的形式出现。软中断向量 HI_SOFTIRQ 和 TASKLET_SOFTIRQ 均是用 Tasklet 机制来实现的。

从某种程度上讲，Tasklet机制是Linux内核对BH机制的一种扩展。在Linux 2.4内核引入了Softirq机制后，原有的BH机制正是通过Tasklet机制这个桥梁来纳入Softirq机制的整体框架中的。正是由于这种历史的延伸关系，使得Tasklet机制与一般意义上的软中断有所不同，而呈现出以下两个显著的特点。

1. 与一般的软中断不同，某一段 Tasklet 代码在某个时刻只能在一个 CPU 上运行，而不像一般的软中断服务函数（即 softirq_action 结构中的 action 函数指针）那样在同一时刻可以被多个 CPU 并发地执行。  
2. 与 BH 机制不同, 不同的 Tasklet 代码在同一时刻可以在多个 CPU 上并发地执行, 而不像 BH 机制那样必须严格地串行化执行 (也即在同一时刻系统中只能有一个 CPU 执行 BH 函数)。

Bottom Half 机制在新的 Softirqq 机制中被保留下来，并作为 Softirqq 框架的一部分。其实现也似乎更为复杂些，因为它是通过 Tasklet 机制这个中介桥梁来纳入 Softirqq 框架中的。实际上，软中断向量 HI_SOFTIRQ 是内核专用于执行 BH 函数的。

Softirq_init函数调用了opensoftirq，如代码段2-12所示。

代码段2-12 softirq.c opensoftirq函数  

<table><tr><td>1</td><td>void opensoftirq(int nr, void (*action) (struct softirq_action*), void *data)</td></tr><tr><td>2</td><td>{</td></tr><tr><td>3</td><td>softirq_vec[nr].data = data;</td></tr><tr><td>4</td><td>softirq_vec[nr].action = action;</td></tr><tr><td>5</td><td>}</td></tr></table>

我们会发现 do_sofirq 函数内部最多只处理 10 个软中断, 如果系统内部的软中断事件太多,那么就会通知 ksoftirqd 内核线程处理软中断。这样, 就不会占用太多的中断上下文执行时间。

上面介绍了软中断的初始化，下面介绍软中断的实现机制。我们看到软中断最终是通过invokesoftirq调用的，但实际上它不是函数，而是一个宏，被定义成_dosoftirq，那么dosoftirq就是软中断处理函数，而是一个密，被定义成_dosoftirq。那么dosoftirq就是软中断处理函数的入口，见代码段2-13所示。

代码段2-13 softirq.c __dosoftirq函数  
```c
1 asmlinkage void _do_softirq(void)  
2 {  
3 struct softirq_action *h;  
4 __u32 pending;  
5 int max_restart = MAX_SOFTIRQ_RESTART; /*这个宏的值是10*/  
6 int cpu;  
7 pending = local_softirq_pending();  
8 pending = local_softirq_pending();  
9 restart;  
11  
12 local_irq_enable();  
13 h = softirq_vec;  
/*这里的action就是上面open_softirq中注册的回调函数*/  
14 do {  
15 if (pending & 1) {  
16 h->action(h);  
17 }  
18 h++;  
19 pending >= 1;  
20 while (pending);  
21  
22 local_irq_disable();  
23 pending = local_softirq_pending();  
24 if (pending && --max_restart)  
25 if (pending && --max_restart)  
26 goto restart;  
/*如果已经处理了10个帧中断，还是有软中断事件没有处理，那么就通知软中断内核守护进程（内部调用 wake_up_process函数）*/  
27 if (pending)  
28 wakeup_softirqd();  
29 ...  
30  
31 account_system_vtime(current);  
32 __local_bh_enable();  
33 } 
```

ksoftirqd 内核线程属于 Linux 系统必需的部分，它在系统初始化的时候就被创建了，大家在裁减 Linux 时注意不要把这部分代码给删除了。

关于这两个软中断的技术，可以查阅相关文档，而且我们在后面关于报文接收的章节中还要提到软中断，这里就不多说了。

# 2. 设备驱动挂接ISR

设备驱动程序要处理硬件中断，必须挂接ISR，挂接一个ISR可以用如代码段2-14所示的request_irq函数。

代码段2-14 manage.c request_irq函数  
1 /\*\*   
2 \*request_irq - allocate an interrupt line   
3 \*irq:要申请的中断号   
4 \*Ehandler：就是传说中的ISR   
5 \* @irqflags: 中断类型标志   
6 \*@devname：该设备的名字，可以由人看懂的字符   
7 \*@dev_id：似乎是一个ID，但是实际上在我们要讨论的网络设备中就是net_device()结构的一个指针   
8 \*   
9\*Flags:   
10 \* SA_SHIRQ 中断是共享的   
11 \* SA_INTERRUPT 需要禁止本地中断\*   
12 \*   
13 \*/   
14   
15 int request_irq(unsigned int IRQ,   
16IRQreturn_t（*handler）（int，void \*,struct ptRegs \*)，   
17 unsigned long IRQflags,   
18 const char \* devname,   
19 void *dev_id)   
20   
21 int retval;   
22 structirqaction \* action;   
23   
24 ....   
25   
26 action $=$ （structirqaction\*）   
27 kmalloc（sizeof（structirqaction），GFPAtomic);   
28 ....   
/\*此handler就是刚才介绍的handle_IRQ_event函数中要处理的handler.\*/   
29 action->handler $=$ handler;   
30 action->flags $=$ IRQflags;   
31 action->mask $= 0$ ·   
32 action->name $=$ devname;   
33 action->next $=$ NULL;   
34 action->dev_id $=$ dev_id;   
35   
36 retval $=$ setup_irq（irq，action）；   
37   
38 return retval;

要注意的是在挂接ISR之前要正确的初始化设备，并且要保证用正确的顺序挂接中断。里面调用setup_irq就是把第27行创建的irqaction{}挂接到对应中断的链表上，以至于handle_IRQ_event能根据irq号直接找到对应handler。如何实现就不详细说了，有兴趣的同学可以自己研究研究。

# 2.3.2 各种语境下的切换

某一个进程只能运行在用户方式（user mode）下或内核方式（kernel mode）下。用户程序运行在用户方式下，而系统调用运行在内核方式下。Linux 2.6 调度系统从设计之初就把开

发重点放在更好满足实时性和多处理机并行性上，并且基本实现了它的设计目标。

新调度系统的特性概括为如下几方面。

（1）继承和发扬Linux2.4版调度器的特性：

$\bullet$ 交互式作业优先；  
- 轻载条件下调度/唤醒的高性能；  
$\bullet$ 公平共享；  
基于优先级调度：  
高CPU使用率：  
- SMP高效亲和;   
实时调度和CPU绑定等调度手段。

(2) 新特性:

- O（1）调度算法，调度器开销恒定（与当前系统负载无关），实时性能更好；  
高可扩展性，锁粒度大幅度减小；  
新设计的SMP亲和方法：  
优化计算密集型的批处理作业的调度；  
重载条件下调度器工作更平滑：  
- 子进程先于父进程运行等其他改进。

在 Linux 2.6 中，就绪队列定义为一个复杂得多的数据结构 struct runqueue，并且，尤为关键的是，每一个 CPU 都将维护一个自己的就绪队列，这将大大减小竞争。

对于某一个特定的进程，它必定处于表2-2所示状态中的一个。

表2-2  
任务状态的值  

<table><tr><td>宏定义</td><td>值</td><td>含义</td></tr><tr><td>TASK_RUNNING</td><td>0</td><td>正在运行的进程（是系统的当前进程）或准备运行的进程（在 Running 队列中，等待被安排到系统的 CPU）。处于该状态的进程实际参与了进程调度</td></tr><tr><td>TASK_INTERRUPTIBLE</td><td>1</td><td>处于等待队列中的进程，待资源有效时唤醒，也可由其他进程被信号中断，唤醒后进入就绪状态</td></tr><tr><td>TASK_UNinterruptIBLE</td><td>2</td><td>处于等待队列中的进程，直接等待硬件条件，待资源有效时唤醒，不可由其他进程通过信号中断、唤醒</td></tr><tr><td>TASK_STOPPED</td><td>4</td><td>进程被暂停，通过其他进程的信号才能唤醒。正在调试的进程可以在该停止状态</td></tr><tr><td>TASK_ZOMBIE</td><td>8</td><td>终止的进程，是进程结束运行前的一个过度状态（僵死状态），虽然此时已经释放了内存、文件等资源，但是在 Task 向量表中仍有一个 task_struct 数据结构项。它不进行任何调度或状态转换，等待父进程将它彻底释放</td></tr></table>

# 2.3.3 内核下的同步与互斥

同步与互斥是有区别但又互相联系，在经典的操作系统教材中两个术语可以互换，为什么？因为同步是建立在互斥的基础之上的。只有实现了互斥功能，才能实现同步机制，它们之间的关系有点类似于TCP和IP的关系。在大型的操作系统实现中，这两者被严格的区分开来。同步一般用semaphore表示，互斥一般用spin lock来表示。Windows内核源代码也是

如此，linux 内核中亦然。主要的区别是在Semaphore 机制中，当某进程进不了临界区时系统会调度其他进程运行，而 spin_lock 必须使所有 CPU 空转（在 cmp 中是这样，但在单一 CPU 环境下则是空语句，我们会在下面呈现给大家看）。在 VxWorks5.5 之前的操作系统中，则没有提供 spin_lock 的机制，可能之前没有考虑嵌入式系统居然还有多 CPU 的情况。现在很多大型电信设备都开始实现类似 SMP 的架构设计，那么这种机制就是必要的了。

为了正确使用同步与互斥机制，我们必须知道，内核中主要有如下的执行路径：

(1) 用户进程的内核态, 此时有进程 context, 主要是代表进程在执行系统调用等。  
(2) 中断或者异常或者自陷等, 从概念上说, 此时没有进程 context, 不能进行 context switch。  
(3) 软中断, 从概念上说, 此时也没有进程 context。  
（4）同时，相同的执行路径还可能在其他的CPU上运行。

这样，考虑这4个方面的因素，通过判断我们要互斥的数据会被这4个因素中的哪几个来存取，就可以决定具体使用哪种形式的锁。

# 1. 中断相关的锁

local_irq_disable/local_irq_enable，表示只是对当前执行上下文的CPU进行开/关中断。如果在多CPU情形下，不保证其他CPU会响应中断。

# 2. spin_lock/spin_inlock

Spin_lock 采用的方式是让一个进程运行，另外的进程忙等待，在 SMP（对称多处理器系统）上会极大影响性能，因为某一个 CPU 上的某一个进程调用了 spin_lock 会导致所有 CPU 上的所有进程阻塞。但是目前大多数情况下都是单 CPU 系统（多核不是多 CPU，这个情况比较复杂，不在本书研究范围之内），虽然多宏观上看操作系统是多并发多进程的，实际上从微观上看，任意的某一时刻只有一个进程在运行。所以在 UP（单处理器）系统中，spin_lock 和 spin_unlock 就被定义成空的了。本文并不打算涉及 SMP 及多核等技术的讨论，有兴趣的读者可以自己去研究。在本书中，凡是遇到了 spin_lock 及其变体，基本都无视而过。

spinlock_XXX有很多形式，有

```txt
spin_lock(/spin_unlock().  
spin_lock_irq(/spin_unlock_irq().  
spin_lock_irqsave/spin_unlock_irqrestore().  
spin_lock_bh(/spin_unlock_bh().  
local_bh_disable/local_bh_enable. 
```

那么，在什么情况下具体用哪个，这要看是在什么内核执行路径中，以及要与哪些内核执行路径相互斥。例如：

如果只要和其他CPU互斥，则用spin_lock/spin_unlock。  
如果要和irq及其他CPU互斥，则用spin_lock_irq/spin_unlock_irq。  
- 如果既要和irq及其他CPU互斥，又要保存EFLAG的状态，则用spin_lock_irqsave/spin_unlock_irqrestore。  
如果要和bh及其他CPU互斥，则用spin_lock bh/spin_unlock bh。  
- 如果不需要和其他CPU互斥，只要和bh互斥，则用local bh disable/local bh enable。本来不想讲述太多关于spin_lock的东西，但是由于在检查其代码的时候花费了我大量的

时间，如果不记下来，以后又忘记了，多可惜！于是，我不得不花点笔墨来描述其结构。

Spin_lock 在代码中有好多定义，但其实就是_spin_lock，它在 spinlock.c 中实现，（为什么选择“Preempt”了还是要定义成：

```c
void _lockfunc _spin_lock (spinlock_t *lock)  
{  
    preempt_disable();  
    _rawSpin_lock (lock);  
} 
```

有待进一步研究), _rawspin_lock 会被定义成

define_raw_spin_lock (lock) __raw_spin_lock (& (lock) ->raw_lock), 注意,那个#号和 define 中间有一个空格, 会让我们有一个错觉, 似乎这是一句不会被编译的代码,但是它确实是符合 C 编译要求的语句。然后我们回到 spinlock.h 的上部, 发现这么几句代码:

```c
if defined (CONFIG_SMP)  
include <asm/spinlock.h>  
else  
include <linux/spinlock_up.h>  
endif 
```

那么根据我的计算机情况，是单核，不是SMP，而且是UP，所以我们会在spinlock_up.h中找到_rawSpin_lock的定义：

```txt
define __rawSpin_lock (lock) do { (void) (lock); } while (0) 
```

看到没有？这就是 spin_lock 在单一处理器上的实现，它是一个空语句。什么都不执行！这也就是本书中为什么无视它的原因。

# 3. down/up 信号

内核中的Semaphore 机制。它主要通过 down() 和 up() 两个操作实现。Down() 用于获取资源，而 up() 是释放资源。一个任务通过调用 down() 获取资源，而代表该资源的信号量表示“没有可用资源”的时候，进程转入等待状态，直到占有资源的进程调用 up() 释放资源后才能被唤醒。进程的等待与唤醒通过等待队列实现。Up() 释放一个资源实例，并且唤醒一个等待进程。若此时还有其他进程处于等待状态的，down() 返回前的 wake_up() 会暂时地唤醒等待进程，将 count、sleepers 再调整为 $(-1,1)$ ，表示暂时无足够资源提供，然后又进入等待状态。

可以看出，semaphore 和 spin_lock 机制解决的都是两个进程的互斥问题，都是让一个进程退出临界区后另一个进程才进入的方法，不过 semaphore 机制实行的是让进程暂时让出 CPU，进入等待队列等待的策略，而 spin_lock 实行的却是一个进程在原地空转，等着另一个进程结束的策略。

# 4. RCU读写锁

Linux2.6 还引入了新的锁机制 RCU（Read-Copy Update）的实现机制，RCU（Read-Copy Update）顾名思义就是读-复制修改，它是基于其原理命名的。对于被 RCU 保护的共享数据结构，读操作不需要获得任何锁就可以访问它，但写操作在访问它时首先复制一个副本，然后对副本进行修改，最后使用一个回调（callback）机制在适当的时机把指向原来数据的指针重新指向新的被修改的数据。这个时机就是所有引用该数据的 CPU 都退出对共享数据的操作。

因此RCU实际上是一种改进的rwlock，读操作几乎没有什么同步开销，它不需要锁，不使用原子指令，因此不会导致锁竞争、内存延迟以及流水线停滞。不需要锁也使得使用更容易，因为死锁问题就不需要考虑了。写操作的同步开销比较大，它需要延迟数据结构的释放，复制被修改的数据结构，它也必须使用某种锁机制同步并行的其他写操作的修改操作。读操作必须提供一个信号给写操作，以便写操作能够确定数据可以被安全地释放或修改的时机。有一个专门的垃圾收集器来探测读操作的信号，一旦所有的读操作都已经发送信号告知它们都不在使用被RCU保护的数据结构，垃圾收集器就调用回调函数完成最后的数据释放或修改操作。RCU与rwlock的不同之处是：它既允许多个读操作同时访问被保护的数据，又允许多个读操作和多个写操作同时访问被保护的数据（注意：是否可以有多个写操作并行访问取决于写操作之间使用的同步机制），读操作没有任何同步开销，而写操作的同步开销则取决于使用的写操作间同步机制。但RCU不能替代rwlock，因为如果写操作比较多时，对读操作的性能的提高不能弥补写操作导致的损失。

# 2.3.4 各种异步手段

# 1. Timer（定时器函数）

作为一个有经验的开发者，一定知道所谓的定时器函数，其实都是一些回调函数而已，只不过本书中涉及的定时器都是在内核中执行，内核中的 Timer 不是线程，它们运行在中断级，所以 timer 函数不应该做任何精细的工作。如果需要进一步处理，那么应该在 tasklet 里完成，因为 tasklet 可以被中断抢占。创建 Timer 的方式有好几种，出于篇幅的原因，这里只举一个在协议栈经常使用的方式：

（1）定义一个timer list{} 结构比如名叫atimer;  
（2）调用init timer(&atimer);  
(3) 指定 atimer.expres 为执行周期, atimer.function 为回调函数, timer.data 为回调函数的参数;  
（4）执行add_timer(&atimer);  
(5) 当执行 atimerExpires 之后, 执行回调, 并且在回调函数中再执行 (4), 依次重复。

为什么要采用这种方式而非其他，是因为要保证这种 Timer 的优先级高于其他方式创建的 Timer。

其内在机制是怎么运作的呢？答案是：软中断！其调用关系如下：

run_timersoftiring $\rightarrow$ runtimers

在_run timers函数中，先扫描用上面方法创建的timer，然后再扫描其他的timer。

# 2.work queue（工作队列）

work queue 是内核很早就增加的功能，类似于 timer，指定一个回调，然后挂接到一个特殊的队列，让系统在适当的时机调用它们。它与进程调度机制紧密结合，能够用于实现内核中异步事件通知机制。其中一种使用方法如下：

(1) 定义一个 work:DECLARE_WORK (my_work, my_event_callback_func(NULL).  
(2) 调用 schedule_work (my_work), 那么 my_work 会在将来某个时刻被处理。

schedule_work 最终会调用 wake_up 函数去唤醒以 queue 作为等待队列头的所有等待队列

对应的进程。要注意凡是调用这个函数去挂接 work struct{} 时，实际上是挂到一个全局队列 keventd_wq 上。在 do_basic_setup 时候 Linux 曾经调用 init_workqueues 去创建一个名为 keventd_wq 的全局对象，刚才我们看到的 schedule_work 函数就是把 my_work 挂接到此对象上。在创建 keventd_wq 的时候就创建了一个内核线程。

init_workqueue 的内部过程如图 2-11 所示。

![](images/72573dd396a4c88c9b06aa8d3f0eab155a275af7cb95736eaeabf72a1d5863be.jpg)  
图2-11 工作队列的创建

其中创建出来的 worker_thread 的内部如图 2-12 所示。

![](images/eba481b3185efb5513b05af3d18bb91f89e8b15a205f45b5a0caf648bf89851b.jpg)  
图2-12 工作者线程的函数调用图

内核线程是被 kernel_thread [arch/i386/kernel/process]创建的，它又通过调用著名的 clone 系统调用[arch/i386/kernel/process.c]（类似 fork 系统调用的所有功能都是由它最终实现）。

下面是一些最常用的内核线程（你可以用ps-x命令）：

```txt
PID COMMAND  
1 init  
3 ksoftirqd/0  
4 events/0  
6 kthread  
88 bdflush  
90 swapd0 
```

前面第一列是进程/线程的ID，读者看到ID为第4号的内核线程events，就是本书要介绍的协议栈常用的worker queue。

# 3. 通知链

Linux内核协议栈大量使用通知链这种技术，用法如下：

（1）定义一个通知链 BLOCKING_NOTIFY_HEAD (my_event_chain)。  
（2）定义一个通知块 structnotifier_blockmy_event_block $=$ {notifier_call $\equiv$ my_event_process_function,}；  
（3）使用blocking.notifier_chain.register（my_event_block）把这个通知块挂接到my_event_chain。  
（4）另一处代码调用blocking_notify_call_chain（my_event_chain），那么就把一个事件发送到对应的通知块上，对应的处理函数就会处理事件。

内部实现是blocking_notify_call_chain 内部调用notify_call_chain，这个函数内部会遍历通知链，然后以此执行通知块上的回调函数，如代码段2-15所示。

代码段2-15 sys.c notifier_call_chain函数  
```c
1 int notification_call_chain(struct notification_block **n, unsigned long val, void *v)
2 {
3     int ret=NOTIFY_DONE;
4         struct notification_block *nb = *n;
5
6     while (nb)
7     {
8         ret=nb->notifier_call(nb, val, v);
9     /* 如果执行环境处于调试中断中，那么可以退出遍历 */
10         if (ret&NOTIFY_STOP_MASK)
11         return ret;
12     }
13         nb=nb->next;
14     }
15     return ret;
16 } 
```

从代码上看，所谓通知链并不是真正的“通知”方式，而是直接回调方式，这就要保证不写出递归很多次的代码，以免内核栈被“撑死”。

# 2.4 虚拟文件系统

VFS（虚拟文件系统）在Linux及UNIX家族中是非常重要的概念，可以说它是操作系统的骨架。每个单独的技术，比如设备驱动程序、文件、管道甚至将要介绍的协议栈，都作为其附着物存在于内核中。不过限于篇幅，就不一一阐述了。与任何一种UNIX一样，Linux并不通过设备标识访问某个文件系统（如DOS那样），而是将它们“捆绑”在一个树型结构中，文件系统安装时（mount），Linux将它挂到树的某个节点（即目录），文件系统的所有文件就是该目录下的文件或子目录，直到文件系统卸载（umount），这些文件或子目录才自然脱离。

VFS在大多数文档中作为Virtual File System的缩写，还不足以点明VFS的作用，如果用Virtual Filesystem Switcher来定义VFS可能更明白它的用途，这就是虚拟文件系统切换器。VFS只存在与内存中，它在系统启动时被创建，系统关闭时注销。VFS的作用就是屏蔽各类文件系统的差异，给用户、应用程序，甚至Linux其他管理模块提供统一的接口集合。管理VFS数据结构的组成部分主要包括超级块和inode。

为什么在研究协议栈的书中要介绍VFS呢？因为Linux将网络接口也作为一个文件去操作，如果对文件系统不了解，就不会很好地解释为什么同样的发送过程，可以用send()，也可以用write()系统调用。具体的形式可以参阅图2-13。

VFS 是物理文件系统与服务之间的一个接口层，它对 Linux 的每个文件系统的全部细节进行抽象，使得不同的文件系统在 Linux 核心以及系统中运行的进程看来都是相同的。严格的说，VFS 并不是一种实际的文件系统。它只存在于内存中，不存在于任何外存空间。VFS 在系统启动时建立，在系统关闭时消亡。

VFS 使 Linux 同时安装、支持许多不同类型的文件系统成为可能。VFS 拥有关于各种特殊文件系统的公共界面，当某个进程发布了一个面向文件的系统调用时，内核将调用 VFS 中对应的函数，这个函数处理一些与物理结构无关的操作，并且把它重定向为真实文件系统中相应的函数调用，后者用来处理那些与物理结构相关的操作。图 2-13 所示就是逻辑上对 VFS 及其下层实际文件系统的组织图，可以看到用户层只能与 VFS 打交道，而不能直接访问实际的文件系统，比如 EXT2、EXT3、PROC，换句话说，就是用户层不用也不能区别对待这些真正的文件系统，不过， SOCKET 虽然也属于 VFS 的管辖范围，但是有其特殊性，就是不能像打开大部分文件系统下的“文件”一样打开 socket，它只能被创建，而且内核中对其有特殊性处理。第 3 章我们会看到是如何特殊的。

![](images/5092b6bbfeaaed639cfe1a69da609d2de267985bd36fd1469809a1e7d111bada.jpg)  
图2-13 VFS与底层各模块关系

VFS描述文件系统使用超级块和inode的方式，所谓超级块就是对所有文件系统的管理机构，每种文件系统都要把自己的信息挂到super_blocks这么一个全局链表上。内核中是分成2个步骤完成的。首先每个文件系统必须通过register_filesystem函数将自己的file_system_type挂接到file_systems这个全局变量上，然后调用kernmount函数把自己的文件相关操作函数集合表挂到super_blocks上。每种文件系统类型的读超级块的例程（get_sb）（Linux 2.4.0的子例程名为readSUPER）必须由自己实现。图2-14所示就是内核中这几种数据

结构的基本关系图。可以总结出，一个实际的文件系统要能工作必须创建三种数据类型“super_block, file_system_type, super_operations”的三元组。我们会在后面的网络文件系统初始化一节中再次研究初始化过程。

![](images/b033b1af423d930adf550bf6dc102f4ac182cbe8d7117bf8d75aa90a4ca7c7a5.jpg)  
图2-14 super_blocks和file_systems链表

文件系统由子目录和文件构成。每个子目录和文件只能由惟一的 inode 描述。inode 是 Linux 管理文件系统的最基本单位，也是文件系统连接任何子目录、文件的桥梁。VFS inode 的内容取自物理设备上的文件系统，由文件系统指定的操作函数（i_op 属性指定）填写。VFS inode 只存在于内存中，可通过 inode 缓存访问。

Linux 即支持多种类型的文件系统, 又保持极高的时空性能, 究其原因, 在于各种复杂缓存的作用。比如 inode 缓存。随着文件的读入和写出, 这些文件的 inode 构成一条链表。一般通过对 inode 链表的线形搜索, 可以找到任一表示某设备上一个文件或子目录的 inode。从效率方面, VFS 为已分配的 inode 构造缓存和 hash 表。

Linux 下有很多种文件系统，可以通过查看文件属性推出某文件属于某个文件系统编制，使用#ls -la 命令，列出的文件信息第一栏有这样的表示方法：drwx。这是文件的 Mode 字段。大部分文件系统的表面区别大致如下。

普通文件

Mode 字段的第一个字符用横线表示，例如-rw-rw-rw，这类普通文件包含的数据内核并不感兴趣，所以不会对其文件内容改写（用户改写是另一回事）

目录文件

Mode字段的第一个字符用“d”表示，例如drw----，目录文件存放着文件名和文件缩引

节点之间的关联关系。

- 块设备

Mode字段的第一个字符用“b”表示，例如brw----，这些文件表示一个硬件设备，通常读取这些设备的数据是通过块操作进行，比如磁盘驱动和磁带驱动文件。这些文件一般放在/dev目录下。而且用户不能用文本查看的方式打开它们。

字符设备

Mode字段的第一个字符用“e”表示，例如cw---，这些文件也表示一个硬件设备，通常读取这些设备的数据是通过字节流方式操作进行，比如终端输入设备驱动和串口驱动文件。这些文件一般也放在/dev目录下。用户也不能用文本查看的方式打开它们。还有一种伪设备或设备驱动程序也是字符设备，但它们并不表示一个硬件，只是用于特殊目的，比如某个程序想和内核沟通，可以采取这样的方法。

链接（link）

Mode 字段的第一个字符用 “1” 表示, 例如 Irw----。这表示这个文件是指向另一个文件的 “指针” (和内存中的指针不一样)。

$\bullet$ 命名管道

Mode字段的第一个字符用“p”表示，例如prw----。管道也可以看作一个文件，一般用来做进程间数据通信，与设备文件比较相似，它的工作方式是FIFO。

套接字

终于到了和我们比较相关连的文件类型了，其Mode字段的第一个字符用“s”表示，例如srw----。不过，在这里套接字文件是用来给不同计算机间不同进程间进行消息交互的，似乎有点与我们的协议栈没太多关系。

还有一些特殊的文件，不代表文件也不代表设备，只是用来提供内核数据和地址空间的访问，如/proc，这个反而是我们要经常关注的文件系统。

![](images/90c91b69bffba100190f0a2731e0089cdefc5cc0093bfa8b3a7c6a97f81ae9f6.jpg)

以上章节是理解内核的基础，限于篇幅，不能再讲得太多，否则喧宾夺主。这里要强调的是，以上每一小节，都可以写出很多页甚至单独成书。希望有兴趣的读者自行到Internet上去搜索相关内容，以下内容和网络具体实现的关系则更加密切。

![](images/e33f77594ee7c3be2aeb2b30ad4e91dd2f9612a91234b3cdb8025e855d90e0a3.jpg)

# 网络协议栈各部分初始化

协议栈本色的初始化这部分在 Linux 2.6 早期和后期是不太一样的，早期的是通过函数直接调用的方式，后期更加依赖于使用 init.h 中定义的初始化宏来进行，即放入特定的代码段去执行。所以再次强调关于内核镜像研究的重要性。我们先给出初始化大致的顺序，大家记住这个顺序就可以对初始化有一个全面的了解。

（1）core_initcall: sock_init;  
(2) fs_initcall: init_init;   
(3) subsystems_initcall: net_dev_init;

（4）device_initcall:设备驱动初始化。

读者可以看到设备驱动初始化是分别被 core_initcall、fs_initcall、subsys_initcall、device_initcall 函数修饰的，根据前文对初始化 section 的描述，这 4 个函数放在不同的 section，而且执行顺序是从（1）~（4）的。所以我们对初始化的叙述也是按照这个顺序进行。切记，分析代码万不可颠三倒四。

# 2.5.1 网络基础系统初始化

先看看初始化第一个步完成什么功能。使用core_initcall初始化宏修饰sock_init函数，见代码段2-16，这个宏指定了sock_init函数放在级别为1的代码段中，也就是说它是最先被执行的（在早期的代码中是先从start_kernel函数开始，调用kernel_thread启动了init进程（在同一个文件里），再调用sock_init）。此函数只是分配一些内存空间，以及创建了一个sockfs_type的文件系统。在do basics_setup中调用sock_init先于Internet协议注册被调用，因为基本上socket初始化必须在每一个TCP/IP成员协议能注册到socket层之前完成。

代码段2-16 socket.csock_init函数  
```txt
1. void __init sock_init(void)  
2.  
3. int i;  
4.  
5. /* 在此只是将此数组初始化为 NULL，真正的初始化要等到INET_init进行*/  
6. for (i = 0; i < NEROTO; i++)  
7. net_families[i] = NULL;  
8.  
9.  
10.  
/* 此函数是为了 socket buffer 设置 slab cache。*/  
11. skb_init();  
12.  
13. /*  
14. * Initialize the protocols module.  
15. */  
/* 然后为 socket 建立伪文件系统，第一步是设置 socket inode cache。*/  
16. init_inodecache();  
17. register_filesystem(&sockfs_type);  
18. sock_cnt = Kernmount(&sockfs_type);  
19. /* 当执行 do_initcalls 时才真正初始化那些特定协议协议，比如执行inet_init */  
20. ...  
21. }
```

所谓基础设施，就是协议栈要运行所需的基本环境，当我们把协议栈看作一个小模块时，它必须与系统中其他部分打交道。比如内存管理（在skb_init中完成）、和上下层之间的联系（在创建socket文件系统的过程中完成）。本小节只是引入了这两部分的概念，后面的章节会一一介绍。

# 2.5.2 网络内存管理

关于内核中网络部分的内存管理，凡涉及内核的 Linux 参考书都会提及，在本书中不会对它大书特书，只是给出一些基本的概念，读者只要能基本理解网络内部内存的管理思路即可。

# 1. skBuff结构

数据包在应用层称为 data，在 TCP 层称为 segment，在 IP 层称为 packet，在数据链路层称为 frame。Linux 内核中 sk_buffer{} 结构来存放数据，在不同的层次会使用上面几种术语来称呼，但这没有什么太大区别。sk_buffer{} 在 INET Socket 和它以下的层次中用来存放网络接收到或需要发送的数据，因此它需要设计得要有足够的扩展性，从而可以支持不同层次、相同层次不同类型的网络协议。另外，还必须高效、紧凑。

Linux内核网络内存管理数据的关系如图2-15所示。

![](images/7a87b03a8c2df3a2888100d9dd36e900cf8cf466dd2a29a7978a75f30d9e765d.jpg)  
图2-15 sock和sk_buffer的关系

图中有一个问号，表示 data 这个指针指向的位置是可变的，它有可能随着报文所处的层次而变动。当接收报文时，从网卡驱动开始，通过协议栈层层往上传送数据报，通过增加 skb→data 的值，来逐步剥离协议首部；而要发送报文时，各协议创建 sk BUFF{}，在经过各下层协议时，通过减少 skb→data 的值来增加协议首部。不仅有这种手段，我们还可以通过 h、nh、mac 这 3 个联合指针，我们可以访问到这些协议首部，从而利用其提供的有效

信息。

先看到的是sk_bufferhead{}结构，它是每个数据流的“头”，凡是能保存数据的地方，都用这个结构来指向真正的数据，如代码段2-17所示。

代码段2-17 skbuff.h sk_buffer头结构  
```c
struct sk_buffer_head {
    /* These two members must be first. */
    /* 这两个成员必须放在前面。原因在于对sk_bufferhead进行操作的时候，可以用sk_buffer结构做类型的强制转换来完成，反过来一样 */
    struct sk_buffer *next;
    struct sk_buffer *prev;
    __u32 qlen; /* 该sk_bufferhead结构引导的一个链表的节点的个数 */
};
```

代码段2-18所示的结构才是真正指向数据区的“指针集合”，其中head成员指向真正的数据区。

代码段2-18 skbuff.h sk_buffer 结构  
```c
struct sk_buff {
    /* 下面这两个成员必须放在最前面. */
    struct sk_buff *next;
    struct sk_buff *prev;
    struct sk_buff_head) *list;
    struct sock *sk;
    struct timeval stamp;
    struct net_device *dev;
    struct net_device *real_dev;
/* 下面是关于第4层/传输层首格式，只用h表示这个联合的名字，（其实我觉得可以叫th，即transport header）*/
union {
    struct tcphdr *th;
    struct udphdr *uh;
    struct icmphdr *icmph;
    struct igmphdr *igmph;
    struct iphdr *ipiph;
    struct ipv6hdr *ipv6h;
    unsigned char *raw;
} h;
/* 下面是关于第3层/网络层首格式，就是传说中的IP头，所以联合的名字叫nh，即networkheader*/
union {
    struct iphdr *iph;
    struct ipv6hdr *ipv6h;
    struct arphdr *arph;
    unsigned char *raw;
} nh;
/* 下面就是MAC层的头*/
union {
    struct ethhdr *ethernet;
    unsigned char *raw;
} mac;
```

以上这种安排也体现了报文各层头部的逻辑关系

这个是路由cache的指针，后面的章节会着重介绍\*/

struct dat_entry \*dat;

这个是xfm相关的成员，不必关心

struct sec_path *sp;

/* 这是控制缓冲区，对于每一层都很方便，你可以把自己的协议相关的私有数据放在这个数组里。

如果你想在穿越多层协议时保持这些数据你必须先调用skbclone，

在VLAN代码中会看到如何借用它的，这里先画出了基本用法，如图2-16所示。

char cb[48];

unsigned int len,

data len,

mac_1en,

CSUN:

unsigned char local_df,

cloned, /*指示此sk_buffer[]是否被“克隆”过，*/

/* 当接收一个报文时，创建一个sk_buffer[]，然后根据地址类型pkt_type指定该skb实际属于哪一种的报文类型，然后上层协议栈采取相应的处理方式处理该skb，见表2-3*/

pkt_type,

ip_summed;

u32

priority:

/* 可以用 eth_type_trans 函数获取 protocol 的值，如果以太网头大于 1536，那么就返回以太网的 h.proto 值，表 2-4 列出了常用值 */

unsigned short protocol,

void (*destructor)(struct sk BUFF *skb);

ifdef CONFIGNETSCHED

32

#

/* traffic control index */

下面这些成员必须放在最后，在alloc_skb（）函数中（创建sk BUFF），为了提高性能只将上面各部分全部清0，而下面的部分可以在后面指定。

unsigned int truesize;

atomic_t users; /* 每引用或“克隆”一次sk_buffer()结构的时候，都要自加1*/

unsigned char *head,

*dataA,

tail.

end

1:

![](images/f0e155fe0b42b8633cd1e8d499cc5e2a7fcbc4f7ce2348a0f636ce40c8870d9c.jpg)  
图2-16 skb中的cb[48]与VLAN的结合

表2-3  
sk_buff $\rightarrow$ pkt_type的值   

<table><tr><td>宏</td><td>值</td><td>说明</td></tr><tr><td>PACKET_HOST</td><td>0</td><td>该报文的目的地是本机</td></tr><tr><td>PACKET_BROADCAST</td><td>1</td><td>广播数据包，该报文的目的地是所有主机</td></tr><tr><td>PACKET Multicast</td><td>2</td><td>组播数据包</td></tr><tr><td>PACKET_OTHERHOST</td><td>3</td><td>到其他主机的数据包，在VLAN接口接收数据时有重要的作用</td></tr><tr><td>PACKET_OUTGOING</td><td>4</td><td>它不是“发送到外部主机的报文”，而是指接收的类型，这种类型用在AF_PACKET的套接字上，这是Linux的扩展</td></tr><tr><td>PACKET_LOOPBACK</td><td>5</td><td>MC/BRD的loopback帧（用户层不可见）</td></tr></table>

表2-4  
sk_buff $\rightarrow$ protocol的值   

<table><tr><td>宏</td><td>值</td><td>说 明</td></tr><tr><td>ETH_P_802_2</td><td>0x0004</td><td>真正的 802.2 LLC, 当报文长度小于 1536 时</td></tr><tr><td>ETH_P_LOOP</td><td>0x0060</td><td>以太网环回报文</td></tr><tr><td>ETH_P_IP</td><td>0x0800</td><td>IP 报文</td></tr><tr><td>ETH_P_ARP</td><td>0x0806</td><td>ARP 报文</td></tr><tr><td>BOND_ETH_P_LACPDU</td><td>0x8809</td><td>LACP 协议报文</td></tr><tr><td>ETH_P_8021Q</td><td>0x8100</td><td>VLAN 报文</td></tr><tr><td>ETH_P_MPLS_IC</td><td>0x8847</td><td>MPLS 单播报文</td></tr></table>

len 是指数据包全部数据的长度，包括 data 指向的数据和 end 后面的分片的数据的总长，data_len 只包括分片的数据的长度。而 truesize 的最终值是 len + sizeof(struct sk_buffer)。

如果一个sk_buffer()是“克隆”得来的，那么它的clone成员和源socket()的clone成员都是1。通过sk_broker()函数完成一次“克隆”操作，“克隆”过后的sk_buffer()不存在于链表中，也不和某一个特定的INET socket相关联，通过检查成员users可以知道这个sk_buffer结构是否被“克隆”过。“克隆”数据包的好处在于可以提高数据使用的效率。

sk_buffer_type 是指该数据包的类型，定义如下。

为了使用套接字缓冲区，内核创建了两个后备高速缓存（lookaside cache），它们分别是skbuff_head_cache和skbuff_fclone_cache，协议栈中所使用到的所有的sk_buffer结构都是从这两个后备高速缓存中分配出来的。两者的区别在于skbuff_head_cache在创建时指定的单位内存区域的大小是sizeof（struct sk_buffer），可以容纳任意数目的struct sk_buffer，而skbuff_fclone_cache在创建时指定的单位内存区域大小是2*sizeof（struct sk_buffer）+sizeof(atomic_t)，它的最小区域单位是一对strect sk_buffer和一个引用计数，这一对sk_buffer是克隆的，即它们指向同一个数据缓冲区，引用计数值是0、1或2，表示这一对sk_buffer中有几个已被使用。

创建一个套接字缓冲区，最常用的操作是alloc Skylb，它在skbuffhead_cache中创建一个struct sk_buffer，如果要在skbuff_fclone_cache中创建，可以调用_alloc Skylb，通过特定参数进行。

struct sk_buffer 的成员 head 指向一个已分配的空间的头部，该空间用于承载网络数据，end 指向该空间的尾部，这两个成员指针从空间创建之后，就不能被修改。data 指向分配空间中数据的头部，tail 指向数据的尾部，这两个值随着网络数据在各层之间的传递、修改，会被不断改动。所以，这 4 个指针指向共同的一块内存区域的不同位置，该内存区域由 __alloc_scrb 在创建缓冲区时创建。

那指向的这块内存区域有多大呢？一般由外部根据需要传入。外部设定这个大小时，会根据实际数据量加上各层协议的首部，再加15（为了处理对齐）传入，在_alloc_skb中根据各平台不同进行长度向上对齐。但是，我们另外还要加上一个存放结构体struct skb_shared_info的空间，也就是说end并不真正指向内存区域的尾部，在end后面还有一个结构体struct skb_shared_info，下面是其定义：

```c
struct skb_shared_info {
    atomic_t dataref; //引用计数
    unsigned short nr_frags; //数据片段的数量
    unsigned short tao_size;
    unsigned short tso_segs;
    unsigned short ufo_size;
    unsigned int ip6_frag_id;
    struct sk_buffer *frag_list; //数据片段的链表
    skb_frag_t fraga[MAX_SKB_FRAGS]; //每一个数据片段的长度。
}; 
```

这个结构体存放分隔存储的数据片段，将数据分解为多个数据片段是为了使用分散/聚集I/O。

如果是在skbuff_fclone_cache中创建，则创建一个struct sk_buffer后，还要把紧邻它的一个struct sk_buffer的fclone成员置标志SKB_FCLONE_UNAVAILABLE，表示该缓冲区还没有被创建出来，同时置自己的fclone为SKB_FCLONE_ORIG，表示自己可以被克隆。最后置引用计数为1。

最后，true size 表示缓存区的整体长度，置为 sizeof(struct sk_buffer) + 传入的长度，不包括结构 struct skb_shared_info 的长度。

# 2.内存管理函数

在sk_buffer{}中的4个指针data、head、tail、end初始化的时候，data、head、tail都是指向申请到的数据区的头部，end指向数据区的尾部。在以后的操作中，一般都是通过data和tail来获得在sk_buffer中可用的数据区的开始和结尾。而head和end就表示sk_buffer中存在的数据包最大可扩展的空间范围。表2-5中是常见的一些skb内存管理函数。

表2-5  
skb内存管理函数  

<table><tr><td>alloc_skb</td><td>申请一个sk_buffer()结构，并且其中申请了真正的数据区</td></tr><tr><td>kfree_skbmem</td><td>释放sk_buffer()数据区，还要根据clone是否清除sk_buffer()本身</td></tr><tr><td>kfree_skb</td><td>封装了kfree_skbmem。也能释放skb</td></tr><tr><td>dev_alloc_skb</td><td>在真正要发送数据的时候，要针对底层的协议申请出一个sk_buffer()空间来存放需要发送的数据包。这个函数内部调用alloc_skb(length+16,...)函数，在这里，除了length的长度空间之外，还要多申请16个字节的长度，这是为了存放以太网上硬件头而预留的空间。RFC规定以太网硬件长度是14个字节，但为了让硬件头后面的IP头和32位地址对齐，就申请了16个字节的空间给硬件头使用。空出两个字节</td></tr><tr><td>skbPut</td><td>将数据添加到现有数据尾部，tail指针往右移，len要增加移动的数量</td></tr><tr><td>skb.push</td><td>把data指针往左移。增加报文头，只是把data减去sizeof(struct报文头)，同时len加上这个值，这样，在逻辑上，skb包含报文头了，通过skh-&gt;h或nh或mac还能找到它</td></tr><tr><td>skb_headroom</td><td>得到该sk_buff数据区头部的空闲区间大小</td></tr><tr><td>skb_tailroom</td><td>得到该sk_buff数据区尾部的空闲区间大小</td></tr><tr><td>skb_reserved</td><td>空出一部分空间在数据区的头部</td></tr><tr><td>skbPull</td><td>把data指针往右移，剥掉报文头，只是把data加上sizeof(struct报文头)，同时len减去这个值，这样，在逻辑上，skb已经不包含报文头了，但通过skh-&gt;h或nh或mac还能找到它</td></tr><tr><td>skb_trim</td><td>把tail指针往左移，剥掉数据区的尾部数据，len减去移动的数量</td></tr></table>

我们不仅要知道如何处理一个skb，还要知道如何处理关于sk_buffer链表的操作函数，它们是更高一层次的应用，如果不了解这些，就不清楚协议层次之间数据传递的实现，表2-6列出了一些管理链表的函数。

表2-6  
skb链表管理函数  

<table><tr><td>skb_insert</td><td>在链表中插入一个sk_buffer结构，不涉及sk_buffer_head结构</td></tr><tr><td>skb_apinned</td><td>在链表中指定一个sk_buffer()后插入一个sk_buffer，也不涉及sk_buffer_head结构</td></tr><tr><td>skb_queue_head</td><td>在链表头增加一个sk_buffer节点</td></tr><tr><td>skb_queueTAIL</td><td>在链表尾增加一个sk_buffer节点</td></tr><tr><td>skb_unlink</td><td>从链表中删除一个sk_buffer节点</td></tr><tr><td>skb_dequeue</td><td>从链表头取出一个sk_buffer节点，并且删除掉此节点</td></tr><tr><td>skb_dequeueTAIL</td><td>在链表尾取出一个sk_buffer节点，并且从链表中删除</td></tr></table>

本书只分析一下skb是如何分配的。一般来讲，一个套接字缓冲区总是属于一个套接字，所以，除了调用alloc_skb函数创建一个套接字缓冲区，套接字本身还要对sk_buff进行一些操作，以及设置自身的一些成员值。下面我们来分析这个过程。alloc_skb调用了_alloc_skb函数（如代码段2-7所示），记住，其传入的最后一个参数fclone总是0。不过总有例外，alloc_skb_fclone函数传入的是1，那么预示着内存的分配是从clone区分配。

代码段2-19skbuff.calloc_skb函数  
```c
1. /\*\*  
2. \* __alloc_skb - 申请一块网络缓冲区  
3. \* @size: 要申请的大小  
4. \* @gfp_mask: 申请码  
5. \* @clone: 是否申请一个可被克隆的 cache。如果是可被克隆的，那么还将申请一个特克隆的 skb  
6. \*  
7. \* 返回的 buffer 对象不包含 headrom 和 tail room 空间的大小。这个对象含有一个引用值，其值为 1  
8. \* 如果失败则返回 NULL。当在中断里申请缓冲区时必须传入 GFP_AROMIC 作为 gfp_mask 的参数，这意味着不允许等待并且使用紧急 pool 中的缓冲区。  
9. \*/  
10. struct sk_buffer * __alloc_skb(unsigned int size, gfp_t gfp_mask,
```

int fclone)   
12.   
13. kmem_cache_t *cache;   
14. struct skb_shared_info *shinfo;   
15. struct sk_buffer *skb;   
16. u8 *data; /\*由于fclone总是0，所以肯定是在后者中分配空间\*/   
17. cache $=$ fclone？skbuff_fclone_cache：skbuff_head_cache;   
18.   
19. $\text{一}$ 分配sk_buffer的空间，注意，只是“sk_buffer”\*/   
20. skb $=$ kmem_cache_alloc（cache,gfp_mask&--GFP_DMA）;   
21.   
22. /\*这里才是分配上图中data的数据区。Size must match skb_add_mtu（）\*/   
23. size $=$ SKB_DATAALIGN（size）;   
24. data $=$ __kmalloc（size+sizeof(struct skb_shared_info），gfp_mask）;   
25.   
26. mmemset（skb,0,offsetof(struct sk_buffer,truesize）);   
27. skb->truesize $=$ size+sizeof(struct sk_buffer); 在这里设置引用值   
28. atomic_set(&skb->users,1); 现在head.data,tail指针全指向data地址   
29. skb->head = data;   
30. skb->data= data;   
31. skb->tail = data;   
32. skb->end = data + size;   
33. /\*这里shinfo等于skb→end指针\*/ Generic Segmentation Offload (GSO)： 协议栈的效率提高一个策略：尽可能晚的推迟分段 (segmentation)，最理想的是在网卡驱动里分段，在网 卡驱动里把大包（super-packet)拆开，组成SG list， 或在一块预先分配好的内存中重组各段，然后交给网卡。   
34. shinfo $=$ skb_shinfo（skb）;   
35. atomic_set(Shinfo->dataref,1)   
36. shinfo->nr_fragments $= 0$ ： shinfo->gso_size $= 0$ ：   
37. shinfo->gso_segs $= 0$ ：   
38. shinfo->gso_type $= 0$ ：   
39. shinfo->gso_type $= 0$ ：   
40. shinfo->ip6_frag_id $= 0$ ： shinfo->frag_list $=$ NULL;   
42.   
43 if（clone）{ /\*定义一个指针指向内存中紧邻它的下一个节点\*/   
44. struct sk_buffer \*child $=$ skb+1; atomic_t \*fclone_ref=(atomic_t \*)（child+1）; /\*这个标志的值是1，而缺省的是0，即SKB_FCLONE_UNAVAILABLE，说明正常情况下skb 不是clone的，这会导致在kfree_skbmen的行为完全不一样。 另一个值SKB_FCLONE clones是2\*/   
46. skb->fclone $=$ SKB_FCLONE_ORIG;   
47. atomic_set（fclone_ref,1）; /\*置下一个单元的标志\*/   
48. child->fclone $=$ SKB_FCLONE_UNAVAILABLE;   
49. }   
50.out:   
51. return skb;   
52. nodata:   
53. kmem_cache_free（cache,skb）;   
54. skb $=$ NULL;   
55. goto out;

注意，sk_buffer[]来自skbuffer_head_cache或skbufferClone_cache，data区来自普通的内核区。

下面解释一下alloc_skb_fclone的操作。此函数申请一个skb（sk_buffer+data），但是这个skb申明自己是可以被克隆的。内存中可看到的如图2-17所示。

![](images/4630596ba3e3f5765d4306120e7b7bfeb4017019e469bbe0bd01fea26b3f49a4.jpg)  
图2-17 skbuff_fclone_cache中的内存操作

这便是原本sk_buffer的操作结果。用户真正需要的skb被设置为克隆原本的标志，而且在这个sk_buffer的后面一个区域被设置为SKB_FCLONE_UNAVAILABLE（0）。当用户需要clone一个skb时，就调用skbclone，把克隆原本的skb_buffer后面的区域设置为CLONE副本标志。

但是当某skb_buffer不可以被克隆时，那么就从skbbuffer_head_cache中申请内存，比如图2-18所示的n-1块。而且，读者请注意，如果某skb_buffer确实是可以被克隆，但在它之前有一块区域已经不是SKB_FCLONE_UNAVAILABLE了，也许是SKB_FCLONE_ORIG，也许是SKB_FCLONE clones，那么还是得从skbbuffer.head_cache中分配内存。

不同的skb cache中的内存操作如图2-18所示。

![](images/b4ba8dd1338f04b067c30c88072f499adef6bcb7ca3f21a995ca802a1d8e505b.jpg)  
图2-18 不同skb cache中的内存操作

说了申请和克隆，然后说说释放过程，见代码段2-20。

代码段2-20skbuff.ckfree_skbmem函数

释放一个skbuff内存，但是不清除其状态

3 \*/   
4 void kfree_skbmem(struct sk_buffer \*skb)   
5 {   
6 struct sk_buffer \*other;   
7 atomic_t \*fclone_ref;   
8   
9 skb_release_data (skb);   
10 switch (skb->fclone) [   
11 case SKB_FCLONE_UNAVAILABLE:   
12 kmem_cache_free (skbuff_head_cache, skb);   
13 break;   
14   
15 case SKB_FCLONE_ORIG:   
16 fclone_ref = (atomic_t \*) (skb + 2); /\*如果引用值大于0，说明还有副本存在，那么什么都不做，返回。如果没有副本了，就释放原本内存\*/   
17 if (atomic_dec_and_test (fclone_ref))   
18 kmem_cache_free (skbuff_fclone_cache, skb);   
19 break;   
20   
21 case SKB_FCLONE_CLAMP:   
22 fclone_ref = (atomic_t \*) (skb + 1);   
23 other $=$ skb-1; /\*把自己的 fclone 标志简单地设置为0即可，而不用释放，因为本来也没有“申请”内存\*/   
24 /\*The clone portion is available for fast-cloning again.\*/   
25 skb->fclone $=$ SKB_FCLONE_UNAVAILABLE; /\*如果所有副本和原本都不存在了，那么就删除原本。这隐含说明了原本曾经尝试过删除自己（但未能完全成功，请参考前几行代码）\*/   
26 if (atomic_dec_and_test (fclone_ref))   
27 kmem_cache_free (skbuff_fclone_cache, other);   
28 break;   
29 ；

好，到此可以得出skbclone和skb_copy的区别：前者基本在skbuff_fclone_cache中分配内存，除非一定要对一个不是可以被克隆的对象进行克隆，才会在skbuff_head_cache中分配内存，而且只是sk_buffer{}结构的复制，没有涉及真正数据区（data）的复制；后者必定在skbuff_head_cache中进行，不仅复制sk_buffer{}，而且复制了数据区。

以上是单纯得skb操作，每一个上层协议都实现了自己对skb得特殊处理。比如TCP，如果检查到待发送数据报没有传输层协议头（不是传输层的TCP或UDP数据报），套接字创建缓冲区的函数是sock_alloc_send_skb，对于非传输层协议包，不使用分散/聚集IO，所以，置data_len为0。

要注意的是Linux 2.6.18去掉了上层协议对上述函数的封装函数。比如说TCP，它的相应函数分别是tcp_alloc_pskb和tcp_free_skb函数。

在这里先给出对skb的报头基本操作，Linux用几种数据结构分别代表不同层次的报文头：ethhdr{}、iphdr{}、udphdr{}、tcphdr{}。图2-19演示了使用不同的函数访问skb数据不同段。

![](images/4933314847ad27130405b4fff2a6d7ca6a2cdb732e1433e2c07c37958ad04b24.jpg)  
图2-19 各协议层函数对网络报文头的理解

不过不管如何转换，sk_bufferhead总是指向数据的最开始。我们介绍内存管理也就到此为止了，本书余下部分不想再在这个方面讲叙。

# 2.5.3 网络文件系统初始化

在linux系统中，socket属于文件系统的一部分，网络通信可以被看作对文件的读取。这种特殊的文件系统叫sockfs。每个文件系统都要自己准备inode类型的缓冲区，即inodecache，它主要是存放inode节点的。上一节中提到在sock_init函数中先调用init inodecache，为创建socket文件系统做好内存准备。不过要注意的是在Linux内核中存在init inodecache多个定义，但都是静态型，即只能由该.c文件中的函数调用，在socket.c中，就定义了这么一个函数，所以sock_init调用就是这个函数，代码如代码段2-21所示。

代码段2-21 socket.cinit_inodecache函数  
```c
1 static int init_inodecache(void)   
2 {   
3 sock_inode_cachep = kmem_cache_create("sock_inode_cache", sizeof(struct socket_alloc),   
4 0, SLAB_HWCAHE allegiance|SLAB_RECLAIM accountsont,   
5 init_once, NULL);   
7 .//错误处理   
8 return 0;   
9 }
```

为文件系统准备 inode 缓存部分了，下面进入初始化文件系统。

首先是调用register_filesystem（&sockfs_type）；把文件系统类型注册到file_systems链表上，然后调用kernmount（&sockfs_type）；把该文件系统注册到super_blocks上。

在系统初始化的时候要通过kern_block安装此文件系统。所谓创建一个套接字就是在sockfs文件系统中创建一个特殊文件。super_block里面有一个字段s_op是用来指向某文件系统内部的支持函数，这个字段的类型为super operation。这个结构定义了12个函数指针，这些指针是要让VFS来调用的。因此这是VFS和文件系统之间的一个接口，经由这层接口，超级块可以控制文件系统下的文件或目录。有趣的是，在Linux 2.4.0内核中，这个结构只有

10 个函数指针, 新增的 2 个偏偏被 socketops 使用, 而其他的大部分函数指针都没有被用到。要注意的是这个结构不是用户层操作 socket 内部的接口函数集合。有的读者可能有编写设备驱动程序的经验, 知道如果要实现某型设备的驱动基本上要提供一个基于 file_operations{} 结构的全局变量, 用户层的 open、read、write 等操作都会映射到这个结构里的成员函数。但是在这里, socketops 不是提供这样的功能的, 它是专门给 super_blocks 用来创建 inode 的。对于 file_operations {}, 我们会在介绍 socket() 系统调用的时候作出详细说明。

```python
static struct superoperations sockfsOps = {alloc inode = sock_alloc inode, destroy_inode = sockdestroy_inode, statfs = simplestats, } 
```

那么这个数据结构是如何挂到 super_blocks 上的呢？现在看看 sock_mnt = Kernmount(&sock.fs_type); 内部发生了什么，图 2-20 演示了它的调用树。

看看do_kernmount内部代码（见代码段2-22），这个函数在不同内核版本中不太一样，但基本操作是一样的。

![](images/37e64c7340d8a6a9de3d8406b62f79b0472f56ccd19e1337f3219702df458f81.jpg)  
图2-20 kern_node函数调用树

代码段2-22 super.c do_kernmount函数  
```c
1. struct vfsamount *  
2. do_kernount(const char *fstype, int flags, const char *name, void *data)  
3. {  
4. struct file_system_type *type = get_f8_type(fstype);  
5. struct super_block *sb = ERR_PTR(-ENOMEM);  
6. struct vfsamount *mnt;  
7. int error;  
8.  
9. /*创建VFS节点*/  
10. mnt = alloc_vfsmnt(name);  
11. if (data) {  
13. secdata = alloc(secdata());  
14. error = security_ab_copy_data(type, data, secdata);  
15. }  
17. sb = type->get_sb(type, flags, name, data);  
18. static int sockfa_get_sab(struct file信息系统_type *f8_type, int flags, const char *dev_name, void *data, struct vfsamount *mnt)  
20. mnt->mnt_ab = sb;  
21. mnt->mnt_root = dget(ab->s_root);  
22. mnt->mntmountpoint = sb->s_root;  
23. mnt->mnt_parent = mnt;  
24. up_write(&sb->s_amount);  
25. put_filesystem(type);  
26. return mnt;  
27.  
28. .../*出错处理*/  
29.} 
```

type $\rightharpoonup$ get_sb实际上就是sockfs_get_sb，如图2-21所示的调用树，我们在前面介绍虚拟文件系统中已经看到它的身影了。现在研究它的实现，它就是把sockfsOps所属的super_block结构挂接到全局链表super_blocks中，通过这么一个操作，形成了前面的图中显示的三元组：<super_block，sockfs_type，sockfsOps>。这样，协议栈与用户层的连接关系基本确定。

![](images/c75ca3900e160750af15ca9576b3a6daf4b7b018d2190462b1ab06f62c17bfa2.jpg)  
图2-21 sockls_get_sb函数调用树

new_inode 先创建一个 inode，然后将它设置为根节点。再创建一个 dentry 结构，与之对应起来，这些内容可以参考其他关于 VFS 实现细节的资料。不管怎样，Linux 的 socket 文件系统算是建立起来了。在下一章我们还会温习这部分内容。

# 2.5.4 网络协议初始化

初始化第二个大步骤就是使用 fs_initcall 初始化宏修饰 init_init 函数，它初始化和协议本身相关的东西，如图 2-22 所示。第二步才真正涉及到“栈”的概念。到此我们才真正的开始网络协议的初始化。在这之前必须知道一些概念——地址族和套接字类型。大家都知道所谓的套接字都有地址族，实际就是套接字接口的种类，每种套接字种类有自己的通信寻址方法。Linux 将不同的地址族抽象统一为 BSD 套接字接口，应用程序关心的只是 BSD 套接字接口，通过参数来指定所使用的套接字地址族。

Linux 内核中为了支持多个地址族，定义了这么一个变量：static struct net.proto_family *net_families[NPROTO]，NPROTO 等于 32，也就是说 Linux 内核支持最多 32 种地址族。不过目前已经够用了，我们常用的不外乎就是 PF_INET(1)、PF_INET(2)、PF_NETLINK(16)，Linux 还有一个自有的 PF_PACKET(17)，即对网卡进行操作的选项。它们都通过代码段 2-23 定义的 net.proto_family 结构来指明，这个结构没有太多的成员。

代码段2-23 af_inet.cinet_familyOps结构  

<table><tr><td>1. struct net.proto_family | 
2. int family;  /*这个值就是地址簇的标识*/ 
3. int (*create) (socket *sock, int protocol); 
4. ... 
5. }</td><td>static struct net.proto_familyinet_familyopers = { 
 familyINET, 
 .family = PF_INET, 
 .create =INET_create, 
 };</td></tr></table>

在 PF_INET 地址族之内, BSD 套接字还定义了多种我们熟知套接字类型, 如流 (stream), 数据报 (datagram), 原始包 (raw) 等。

为了支持多种套接字类型，内核中是有多种相应的全局变量与之对应，而不是只有一种。比如有 proto{}结构类型的，有 inlet_protocol{}结构类型的，有 inlet_protosw{}结构类型的，有 protoOps{}结构类型的，4 者的关系会在以后介绍。

注意，网络协议的初始化是在网络设备的初始化之前完成的，在Linux系统中并不是说网络设备不存在就不需要网络协议了，而是在没有网络设备存在的时候，照样可以完成网络的工作，只不过网络系统物理上只存在于本机一台机器中而已。

tcp_v4_init()和tcp_init()的不同：前者什么都不做（即不在本书的讨论范围内），而后者才是用来初始化TCP协议需要的各项hash表和sysctl_xxx全局配置项的。

arp_init完成系统nearbour表的初始化。

ip_rt_init初始化IP路由表rt_hash_table，我们会在后面的章节详细介绍ARP、邻居系统、路由表管理系统。

如图2-22所示，一进入初始化就调用proto register 3 次，先后为tcp、udp、raw的proto{}结构申请空间并将其挂到一个全局链表proto_list上。这3个proto全局变量非常重要，是连接传输层和IP层的纽带。如代码2-24所示，将它们列在一起，以示比较，其中一些加深的函数是本书要重点关注的。

代码段2-24 tcp_prot, udp_prot, raw_prot结构  

<table><tr><td>1.</td><td colspan="2">struct proto tcpprot = {</td></tr><tr><td>2.</td><td>.name</td><td>= &quot;TCP&quot;,</td></tr><tr><td>3.</td><td>owner</td><td>= THISMODULE,</td></tr><tr><td>4.</td><td>close</td><td>= tcp_close,</td></tr><tr><td>5.</td><td>connect</td><td>= tcp_v4_connect,</td></tr><tr><td>6.</td><td>accept</td><td>= inlet_csk.accept,</td></tr><tr><td>7.</td><td>ioct1</td><td>= tcp_ioct1,</td></tr><tr><td>8.</td><td>init</td><td>= tcp_v4_init_sook,</td></tr><tr><td>9.</td><td>sendmsg</td><td>= tcp_sendmsg,</td></tr><tr><td>10.</td><td>recvmag</td><td>= tcp_recvmag,</td></tr><tr><td>11.</td><td>backlog_rcv</td><td>= tcp_v4_do_rcv,</td></tr><tr><td>12.</td><td>hash</td><td>= tcp_v4_hash,</td></tr><tr><td>13.</td><td>get_port</td><td>= tcp_v4_get_port,</td></tr><tr><td colspan="3">14. };</td></tr><tr><td>1.</td><td colspan="2">struct proto udpprot = {</td></tr><tr><td>2.</td><td>.name</td><td>= &quot;UDP&quot;,</td></tr><tr><td>3.</td><td>owner</td><td>= THIS MODULE,</td></tr><tr><td>4.</td><td>close</td><td>= udp_close,</td></tr><tr><td>5.</td><td>connect</td><td>= ip4_datagram_connect,</td></tr><tr><td>6.</td><td>disconnect</td><td>= udpdisconnect,</td></tr><tr><td>7.</td><td>ioct1</td><td>= udp_ioct1,</td></tr><tr><td>8.</td><td>sendmsg</td><td>= udp_sendmsg,</td></tr><tr><td>9.</td><td>recvmag</td><td>= udp_recvmag,</td></tr></table>

```txt
10. .sendpage = udp_sendpage,  
11. .backlog_rcv = udp_queue_rcv_skb,  
12. .hash = udp_v4_hash,  
13. .unhash = udp_v4_unhash,  
14. .get_port = udp_v4_get_port,  
15. };  
1. struct proto rawprot = {  
2. .name = "RAW",  
3. .owner = THISMODULE,  
4. .close = raw_close,  
5. .connect = ip4_datanram_connect,  
6. .disconnect = udp_disconnect,  
7. .ioct1 = raw_ioct1,  
8. .init = raw_init,  
9. .setsockopt = raw_setsockopt,  
10. .getsockopt = raw_getsockopt,  
11. .sendmsg = raw_sendmsg,  
12. .recvmag = raw_recvmag,  
13. .bind = raw_bind,  
14. .backlog_rcv = raw_rcv_skb,  
15. .hash = raw_v4_hash,  
16. .unhash = raw_v4_unhash,  
17. }; 
```

![](images/4437d9fb3cf0b270e62c4a4b03ab061a50f80d881f7afab19d10082af0aa64fd.jpg)  
图2-22 inset_init调用树

再看 sock_register 函数，它把 inlet_familyOps 塞入 net_families 数组中，这个结构是应付从上到下方向的关系：用户创建 socket 时，先指定 INET 地址族，在指定套接字类型。换句话说这是数据流发送的流向。

下面这个结构是应付从下到上即数据流接收方向的关系。大家都知道，socket 层必须区分哪一个用户应该接收这个包，这叫做 socket 解复用。下面是初始化第二个方面的必要步骤：注册接收函数。

代码段2-25 protocol.c inset_add_protocol函数  
1. /\*   
2. 在inet_init函数中调用了3次，分别传入icmp_protocol，udp_protocol，tcp_protocol，它们的协议处理函数被加入到一个hash表中   
3. 这3个实体内容如下：   
4. \*/   
5. static struct inet_protocol tcp_protocol $=$ { 6. .handler $=$ tcp_v4_rcv, 7. errhandler $=$ tcp_v4_err, 8. .no_policy $= 1$ 9. }；   
10. static structINET_protocoludp_protocol $=$ { 11. handler $=$ udp_rcv, 12. errhandler $=$ udp_err, 13. .no_policy $= 1$ 14. }；   
15. static structINET_protocol icmp_protocol $=$ 16. .handler $=$ icmp_rcv, 17. ;   
18. \*/   
19. intinet_add_protocol(structINET_protocol\*prot，unsigned charprotocol)   
20. {   
21. int hash,re;   
22.   
23. hash $\equiv$ protocol&（MAX_INET_PROTOS-1）;   
24.   
25. if (inet_protos[hash]) {   
26. ret $= -1$ 27. }else{   
28. .inet_protos[hash] $\equiv$ prot;   
29. ret $= 0$ 30. }   
31.   
32. return ret;   
33. }

在这段代码相关的内容中，我们特别要记住那3个结构的接收函数指针：tcp_v4_rcv, udp_rcv, icmp_rcv，我们将在数据接收过程中详细介绍这几个函数。

Linux 区分永久和非永久协议。永久协议包括像 UDP 和 TCP，这是 TCP/IP 协议实现的基本部分，去掉一个永久协议是不允许的。所以，UDP 和 TCP 是不能 unregistered。此机制由两个函数和一个维护注册协议的数据结构组成。一个负责注册协议，另一个负责注销。每一个注册的协议都放在一个表里，叫协议切换表。表中的每一个入口是一个 inlet_protosw 的

实例。先看基于这个结构产生的3个实例。

代码段2-26 af_inet.cinetsw_array变量  
1. $\text{巧}$ 系统启动时把这个数组里所有的单元插入链表里  
2. $\text{巧}$ 3. static struct inlet_protoow inetsw_array[] =   
4.   
5.   
6. .type = SOCK_STREAM,   
7. .protocol = IPRTO_TCP,   
8. .prot = &tcp_prot,   
9. .ops = &inet_STREAM OPS,   
10. .capability = -1,   
11. .no_check = 0,   
12. .flags = INET_PROTOW_PERMANENT   
13.   
14.   
15.   
16. .type = SOCK_DGRAM,   
17. .protocol = IPRTO_UDE,   
18. .prot = UDP_prot,   
19. .ops = &inet_dgram OPS,   
20. .capability = -1,   
21. .no_check = UDP_CSUM_DEFAULT,   
22. .flags = INET_PROTOW_PERMANENT,   
23.   
24.   
25.   
26. .type = SOCK_RAN,   
27. .protocol = IPRTO_IP, /\* wild card */   
28. .prot = &raw_prot,   
29. .ops = &inet_dgram OPS,   
30. .capability = CAP_NET_RAN,   
31. .no_check = UDP_CSUM_DEFAULT,   
32. .flags = INET_PROTOW_REUSE,   
33.

也就是说在inet_init中循环调用了inet_register_protosw（见代码段2-27）3次。传入的参数就是上面列出的inetsw_array[]的每个单元，分别对应TCP、UDP、RAW协议。这inetsw将来会在创建socket的时候用到。

代码段2-27 af_inet.cinet_register_protosw函数  
```c
1. #define INETSM_ARRAY_LEN (sizeof (inetsw_array) / sizeof(struct inet_protocol))  
2.  
3. void inet_register_protocol (struct inet_protocol *p)  
4.  
5. struct list_head *lh;  
6. struct inet_protocol *answer;  
7. int protocol = p->protocol;  
8. struct list_head *last_ptr;  
9.  
10. ... 
```

11. /\*If we are trying to override a permanent protocol, bail. \*/   
12. answer $=$ NULL;   
13. last_perm $=$ ainetsw[p->type];   
14. list_for_each (lh, &inetsw[p->type]) {   
15. answer $=$ list_entry (lh, struct inet_protocol, list);   
16.   
17. /\* Check only the non-wild match. \*/   
18. if (INET_PROTOTSW_PERMANENT & answer->flags) {   
19. if (protocol == answer->protocol)   
20. break;   
21. last_perm = lh; enum sock_type { BOCK_STREAM $\equiv$ 1,   
22. } SOCK_DGRAM $\equiv$ 2,   
23. answer $=$ NULL; SOCK_RAM $\equiv$ 3,   
34. !   
25. if (answer) . . . . .   
26. goto outpermanent; SOCK_PACKET $\equiv$ 10,   
27. list_add_rcu(@p->list, last_perm);   
28.out: #define SOCK_MAX [SOCK_PACKET + 1]   
29. synchronize_net(); struct list_head inetsw[SOCK_MAX];   
30. return;   
31. ....//错误处理，退出   
32.

现在看看inet_STREAMOps、inet_dgramOps、inet_sockrawOps的结构，它们都是proto ops的全局变量，如代码段2-28所示。

代码段2-28 af_inet.c inlet_STREAMOps、inet_dgramOps、inet_sockrawOps结构

```txt
1. struct protoOps inletStreamOps = 1
2. .family = PF_INET,
3. .release = inlet_release,
4. .bind = inlet_bind,
5. .connect = inletStream_connect,
6. .socketpair = sock_no_SOCKETpair,
7. .accept = inlet_accept,
8. .poll = tcp POLL,
9. .ioct1 = inlet_ioct1,
10. .listen = inletListen,
11. .shutdown = inlet_shutdown,
12. .setsockopt = inlet_setsockopt,
13. .getsockopt = inlet_getsockopt,
14. .sendmsg = inlet_sendmsg,
15. .recvmsg = inlet_recvmsg,
16. .mmap = sock_no_mmap,
17. .sendpage = tcp_sendpage
18. }
19.
20. struct protoOps inlet_dgram OPS = { 
```

```c
struct protoopers {
    int family;
    int (*release) (socket *sock);
    int (*bind) (socket *sock, sockaddr)
    *nyaddr, int sockaddr_len);
    int (*connect) (socket *sock, sockaddr)
    *vaddr, int sockaddr_len, int flags);
    int (*accept) (socket *sock, socket)
    *newsock, int flags);
    uint (*poll) (file *file, socket *sock,
            poll_table_struct *wait);
    int (*ioctl) (socket *sock, uint cmd,
           ULONG arg);
    int (*listen) (socket *sock, int len);
    int (*sendmsg) (kiobc *iocb, socket)
    *sock, msghdr *m, size_t total_len);
    int (*recvmsg) (kiobc *iocb, socket)
    *sock, meghdr *m, size_t total_len, int flags);
    size_t (*sendpage) (socket *sock, page)
    *page, int offset, size_t size, int flags);
}; 
```

```txt
21. .family = PF_INET,
22. .release = inet_release,
23. .bind = inet_bind,
24. .connect = inet_dgram_connect,
25. .socketpair = sock_no_SOCKETpair,
26. .accept = sock_no.accept,
27. .getname = inet_getname,
28. .poll = datagram_poll,
29. .ioct1 =INETIoct1,
30. .listen = sock_no.listen,
31. .shutdown = inet_shutdown,
32. .setsockopt =inet_setsockopt,
33. .getsockopt =inet_getsockopt,
34. .sendmsg =INET_sendmsg,
35. .recvmsg =inet_recvmsg,
36. .mmap = sock_no_mmap,
37. .sendpage =inet_sendpage,
38. 1;
39. /
40. * 对于SOCK_RAM sockets，除了没有udp poll外其他必须和inet_dgram OPS一样
41. */
42. static const struct protoOpsINET_sock OPS =
43. .family = PF_INET,
44. .owner = THISMODULE,
45. .release =INET_release,
46. .bind =inet_bind,
47. .connect =inet_dgram_connect,
48. ...
49. ]; 
```

以上是关于协议栈框架的搭建，看起来似乎完美了，但是对于IP层接收流程，则还不够。因为对于发送过程，直接调用IP层函数，而对于内核接收过程则分为2个层次：第一层需要有一个接收函数解复用传输协议报文，我们已经介绍了，而第二层需要一个接收函数解复用网络层报文。对报文感兴趣的底层协议目前有两个，一个是ARP，一个是IP，报文从设备层送到上层之前，必须区分是IP报文还是ARP报文。然后才能往上层送。这个过程由一个数据结构来抽象，叫packet_type{}，其定义如代码段2-29所示。

代码段2-29 detdevice.h packet_type结构定义  
```c
1. struct packet_type {
2. unsigned short type; /* This is really htons (ether_type). */
dev 是指向我们希望接收到包的那个接口的 net device 结构。如果是 NULL，则我们会从任何一个网络接口上收
到包
3. struct net_device *dev; /* NULL is wildcard here */
4. int (*func) (struct sk_buff *, struct net_device *, struct packet_type );
5. void *afPacket Priv;
如果某个 packet_type()被注册到系统中，那么它就被挂接到全局链表中（有 2 个，见下面的解说）,
list 就是代表链表节点
6. struct list_head list;
7. };
```

inet_init函数最后调用了一个dev_add-pack函数（见代码段2-30），不仅是inet_init函数调用，有一个很重要的模块也调用了它，就是ARP模块，会在后面的章节看到它是如何调

用dev_add-pack函数的。

代码段2-30 dev.cdev_add-pack函数  
1. /\*   
2. \* For efficiency   
3. \*/   
4.   
5. int netdev_nit;   
6.   
7. \*\*   
8. \* dev_add-pack - add packet handler0   
9. \* @pt: packet_type declaration   
10. \*   
11. \* 传入的spacket_type 被插入到内核链表。   
12. \*   
13. \* 此函数不能被软中断打断，因为并不能保证所有的CPU在接收报文的过程中看到新的报文类型   
14. \*/ static struct packet_type arpPacket_type = { .type $=$ _constant_htons(ETH_P_ARP), .func $=$ arp_rcv,   
19. static struct packet_type ippacket_type $=$ { .type $=$ _constant_htons(ETH_P_IP), .func $=$ ip_rcv,

对于arp和ip报文都不是ETH_P_ALL，所以挂到了ptype_base这个bash表中。

```txt
20. if (pt->type ==hteos(ETH_P_ALL)) {  
21. netdev_ntt++;  
22. list_add_rcu(&pt->list, &ptype_all);  
23. } else {  
24. hash = ntohs(pt->type) & 15;  
25. list_add_rcu(&pt->list, &ptype_base[hash]);  
26. }  
27. spin_unlock bh(&ptype_lock);  
28. } 
```

报文处理函数放置在一个链表中，这是因为多个协议可能为同一种包注册不同的处理函数。由于多个协议可能接收同一种包，所以它们在复制包之前不能修改其中的内容。在Linux内核中IP协议栈很少有协议是ETH_P_ALL类型的，即接收所有报文，因为这会影响转发性能。

![](images/f68514be59a42584115f76e5cce004001b63a223b6515407f1c00a6e03a0d79c.jpg)

有人讲过Linux中有一个能抓包的特性，方法如下：fd = socket（PF_SOCKET, mode, htons(ETH_P_ALL)）；如此这般就能把所有的报文收上来。实际上内核就是把一个type为ETH_P_ALL的prot hook挂接到pkt_all链表上，其报文处理函数为packet_rcv，感兴趣的读者可以在研究完socket系统调用的章节后再去看af_packet.c中的packet_create函数。

综合前面分析的初始化过程，网络协议栈的框架已经基本搭起来了，从上面的代码段中可以看到 Linux 内核用数个全局变量完成了 INET 层和传输层的搭建工作。记住，在这里都还只是 INET 层和传输层的组织架构，而 IP 层则没有全局变量去代表，只有函数，我们将在以后的过程中对其解剖。协议栈的具体形式如图 2-23 所示。

![](images/c43225166a1d58b86b2855fc851024f6fbf21cea4b4ce46698aa4f71f951b412.jpg)  
图2-23 协议栈的具体形式

综上所述，我们可以推断出在本节讲叙的初始化过程中出现的各种重要的数据结构之间的关系了，正如图2-23所示，从左往右看，是用户界面的角度，分别代表了标识一个套接字的三元组：<地址族，类型，具体协议>，正好是调用socket系统函数的3个参数。通过内核中这3个数据结构，我们就可以创建sock{}结构。从右到左看，是内核中3个重要的数据结构，从上到下分别是socket{}、sock{}、sk BUFF{}，正好是数据流的连接通道。比如，发送报文时，数据会由socket{}通过相应的protoopers{}把数据传给sock{}，sock{}又通过proto{}把数据传到sk BUFF；反过来，当收到报文时，sk BUFF{}通过net_protocol{}把数据传给sock{}。后者又通过proto{}把数据传给socket{}，socket{}最后把数据传给用户层。通过这几个重要的数据结构和变量，可以看出了“栈”的模式。

所以，这几个数据结构结合非常紧密，不过它们的名字比较容易混淆，所以说，开发Linux的内核源代码要小心。

# 2.5.5 初步了解山路系统

由于Linux内核中采用了FIB（Forward Information Base）这个名词代替了Routing Database，这里要先给出一些路由及路由器的概念。

![](images/9e2ac7e471e4c2faf519c3b6752c96596eb95dbff44731cc2ddca5cee8c07b0c.jpg)

通常，路由表（Routing Table）和转发表（Forwarding Table）是可以互换的名词，但是严格地说，这两者是有区别的，特别是在专业路由器的软件系统中。差异如下。

首先，路由表通常是由路由算法（这里先不提管理手段）经过邻居路由器间的信息交换算出来的，每条路由表项把IP前缀映射到下一跳。而路由器参考转发表以确定输出接口。

其次，路由表一般包含IP前缀和下一跳IP地址，而转发表包含IP前缀到输出接口的映射关系，甚至是下一跳网关的MAC地址和一些流量统计。

而且，转发表用来优化在一堆IP地址集合里的搜索，而路由表是专门用来处理网络拓扑变化。

最后，每一个报文的处理都要经过转发表，它可以用硬件 cache 实现，但是路由表通常用软件实现。

传统意义的转发表和路由表您现在是知道了，但是且慢，Linux 内核中存在的 FIB 实际上是上文中的路由表，而转发表却用 rtable（route table）表示。记住了，它们的概念是相反的。为什么要这样原因不详。可能是内核开发者不想和应用层的路由数据库发生概念上的冲突吧。Linux 内核叫做 rtable 的数据结构的，只是 FIB 的一份 cache 而己，其关系如同计算机中内存和 CPU cache 的关系。

系统中路由一般采取的手段是：先到路由缓存中查找表项，如果能查找到，那么就直接将对应的一项取出作为路由的规则；如果查不到，那么就到FIB中根据规则换算出来，并且增加一项新的，在路由缓存中将项目添加进去。所以在研究Linux代码时，应该注意这一点，不能抓着RouteTable不放而忽视了FIB。

FIB是内核中最重要的路由结构。FIB存放着用来给本地流量和外发报文做内部的路由，也能让内核外的应用程序通过路由socket从内核中获取路由信息。本质上它是一个表，包含上层的地址信息和底层的设备信息。通过对FIB数据的查找和换算，一定能够获得路由一个地址的方法。当报文进入路由系统时，系统用报文的目标地址和最精确的网络掩码比较，如果不匹配，就转到另一个较精确的掩码入口和其比较。当完成比较后，IP层复制到远地主机的“direction”到路由快表中，并沿着这条路径发送数据。

Linux 能被配置成支持多个 FIB/路由表，在这里，FIB 表就是路由表，一个 FIB 表包含多个路由条目，以下的语境中，你可以把“FIB 表”当作一个数据库的代名词。缺省情况下只配置有 2 个表（即 2 个数据库）。在大多数情况下，Linux 内核不需要基于策略的路由，特别是嵌入式系统。所以内核不需要配置多个表。如果这样，有两个预定义的全局指针指向两个表，一个是 local table，一个是 main table，local 表存放着到本机分配的地址的路由上，比如分配给网络接口设备的地址和环回地址，main 表存放到外部节点的路由。

由于这个系统相当复杂，这部分的初始化内容放到下一章去介绍，在此时，只需记住了FIB表就是路由表，Linux的路由表就是FIB表。还有，是ip_init函数调用了ip_rt_init去初始化路由系统。

# 2.6 Linux 设备管理

设备初始化是我们要分析的第三和第四个大步骤，这个部分要涉及一些设备驱动的背景知识。

设备管理的目标是能对所有的外设进行良好的读、写、控制等操作。但是如果众多设备没有一个统一的接口，则不利于开发人员的工作。因此Linux采用了类似UNIX的方法，使用设备文件来实现这个统一接口。由此可见，设备文件的相关概念是设备管理的最基础部分。

要让操作系统感知到设备的存在，必须提供一个注册机制，使操作系统能识别设备对应的驱动程序，目前采用基于主、次设备号的方式来管理设备。Linux习惯上将设备文件放在目录/dev或其子目录之下，设备文件名通常由两部分组成，第一部分通常较短，可能只有2或3个字母组成，例如普通硬盘如IDE接口的为“hd”，SCSI硬盘为“sd”，软盘为“fd”，并口为“lp”，第二部分通常为数字或字母，用来区别设备实例。例如/dev/hda、/dev/hdb、/dev/hdc表示第一、二、三块硬盘；而/dev/hda1、/dev/hda2、/dev/hda3表示第一硬盘的第一、第二、第三分区。

这种机制就是Linux 2.4的版本中的注册与管理方式——devfs管理方式，如果在编译内核时选中CONFIG_DEVFS_FS，那么就可以利用这种设备管理方式。devfs挂载于/dev目录下，提供了一种类似于文件的方法来管理位于/dev目录下的所有设备，我们知道/dev目录下的每一个文件都对应的是一个设备，至于当前该设备存在与否先且不论，而且这些特殊文件是位于根文件系统上的，在制作文件系统的时候我们就已经建立了这些设备文件，因此通过操作这些特殊文件，可以实现与内核进行交互。但是devfs文件系统有一些缺点，例如：不确定的设备映射，有时一个设备映射的设备文件可能不同，例如我的U盘可能对应sda有可能对应sdb；没有足够的主/辅设备号，当设备过多的时候，显然这会成为一个问题；/dev目录下文件太多而且不能表示当前系统上的实际设备；命名不够灵活，不能任意指定，容易造成设备之间冲突而不能正常初始化驱动。

本人在构思这本书的时候 devfs 管理方式还在大行其道，但是时隔 3 年，内核源代码中已经不采用这种技术，转而采用 sysfs 技术。引入了一个新的文件系统 sysfs，它挂载于/sys目录下，跟 devfs 一样它也是一个虚拟文件系统，也是用来对系统的设备进行管理的，它把实际连接到系统上的设备和总线组织成一个分级的文件，用户空间的程序同样可以利用这些信息以实现和内核的交互，该文件系统是当前系统上实际设备树的一个直观反应，sysfs 的工作就是把系统的硬件配置视图导出给用户空间的进程。在 Linux 2.6.18 内核中，必须选中 General Setup $\rightarrow$ Configure Standard Kernel features（For small systems），在 File System $\rightarrow$ Pseudo filesystems 菜单内部出现“sysfs file system support”。

不管如何进步，其中心思想是建立一种分层的体制，块设备是一种class，字符设备是一种class，而网络设备也是一种class。驱动程序开发者如果知道自己的设备属于哪一种class，那么就把其驱动程序挂到相应的class上，让内核为驱动程序分配名字和设备号，如果不确定是哪种class，还可以自己建立class类别。Linux用户可以到/sys下观察一下系统中有哪些内

核模块及驱动，然后和/dev下的文件做一个对比就发现，sys目录确实将各种设备进行了归类，条理清晰的多。用户空间的工具udev就是利用了sysfs提供的信息来实现所有devfs的功能的，但不同的是udev运行在用户空间中，而devfs却运行在内核空间，而且udev不存在devfs那些先天的缺陷。很显然，sysfs将是未来发展的方向。

那么 sysfs 是怎么认出系统中存在的设备以及应该使用什么设备号呢？对于已经编入内核的驱动程序，当被内核检测到的时候，会直接在 sysfs 中注册其对象；对于编译成模块的驱动程序，当模块载入的时候才会这样做。一旦挂载了 sysfs 文件系统（挂载到 /sys），内建的驱动程序在 sysfs 注册的数据就可以被用户空间的进程使用，并提供给 udev 以创建设备节点。

关于设备管理的内容，我只想说这么多，因为要牵扯到许多的配置文件和环境变量，由于每个版本的Linux的设备管理在配置文件和环境变量的设置上多少有些不同，它们的进化又非常快，我觉得只要不影响本文的理解，就可以先跨过这部分内容。下面我们开始进入内核设备管理系统。

Linux在设备驱动程序的实现上又分为两层：

抽象设备层（又叫核心模块）：  
特定设备驱动程序。

抽象硬件层。这一层主要提供一些设备无关的处理流程，也提供一些公用的函数给底层的 device driver 调用。它为网络协议提供统一的发送、接收接口。这主要是通过 net_device 结构。是上层的、与设备无关的，这部分根据输入/输出请求，通过特定设备驱动程序接口，来与设备进行通信。

特定设备驱动程序。这是一种下层的、与设备有关的，常称为设备驱动程序，它直接与相应设备打交道，并且向上层提供一组访问接口；当一个网络设备的初始化程序被调用时，它返回一个状态指示它所驱动的控制器是否有一个实例。

初始化的第三个大步骤就是抽象设备层的初始化。这由 net_dev_init 函数完成，此函数由下面的宏修饰，如下：

subsys initcall (net dev init);

这个宏定义请参见前面说的init.h，它被定义为：define_initcall("4",fn)

所以它是在core_initcall和fs_initcall之后被调用的。

在 Linux2.4 内核中 net_dev_init 函数就是对实际底层网络设备的初始化例程，如代码段 2-31 所示。但是我们要注意的是现在的这个函数在 Linux2.6 内核中已经不对特定设备进行初始化了，只是为网络设备设置一些基础功能。比如 proc 文件系统、sysfs 系统、全局设备和索引表、设置软中断回调等。不过我个人觉得最重要的是对 queue 的各项成员的初始化。

# 代码段2-31 dev.c net_dev_init函数

```txt
1. /\*  
2. \* 初始化可以存放设备相关信息的全局数据结构，在此时设备一般还没有被初始化。  
3. \* 此函数由单一线程在boot的时候调用，不需要获取rtnl信号量  
4. \*/  
5. static int _init net_dev_init(void)  
6.
```

```c
7. int i, rc = -ENCMEM;  
8. if (devProc_init())  
9. goto out;  
10.  
11. if (netdev_sysfa_init())  
12. goto out;  
13.  
14. INIT_LIST_HEAD (&type_all);  
15. for (i = 0; i < 16; i++)  
16. INIT_LIST_HEAD (&type_base[i]);  
17.  
18. for (i = 0; i < ARRAY_SIZE(dev_name_head) ; i++)  
19. INIT_LIST_HEAD (&dev_name_head[i]);  
20.  
21. for (i = 0; i < ARRAY_SIZE(dev_index_head) ; i++)  
22. INIT_LIST_HEAD (&dev_index_head[i]);  
23.  
24. /*  
25. * Initialize the packet receive queues,  
26. */ 
```

每个CPU有自己的一个队列，本书不打算讨论多CPU的情形，所以读者请将NR_CPUS看成1即可。

27. for (i = 0; i < NR_CPU; i++) {有一个全局变量叫per_cpusoftnet_data，定义为CPU软中断（包括接收和发送）的数据队列，这个变量是通过DEFINE_PER_CPU(struct softnet_data,softnet_data)=[NULL];定义的  
29. queue $=$ &per_cpu(softnet_data,i);  
30. skb_queue_head_init(&queue->input_pkt_queue);  
31. queue->throttle $\equiv$ 0;  
32. queue->cng_level $\equiv$ 0;  
33. queue->avgblog $\equiv$ 10; /* arbitrary non-zero */  
34. queue->completion_queue $\equiv$ NULL;  
35. INIT_LIST_HEAD(&queue->poll_list);  
36. set_bit(_LINK_STATE_START,&queue->backlog_dev.state);  
37. queue->backlog_dev.weight $\equiv$ weight_p;  
38. queue->backlog_dev POLL $\equiv$ process_backlog;  
39. atomic_set(&queue->backlog_dev.rafcnt,1);  
40. }  
41.  
42. dev.boot_phase $\equiv$ 0;  
43.  
44. opensoftirq(NET_TXSoftIRQ,net_tx_action,NULL);  
45. opensoftirq(NET_RXSoftIRQ,net_rx_action,NULL);  
46.  
47. dst_init();void __init dst_init(void){register_netdeviceNotification(&dst_devNotification);}其中的参数是通知链dst_devNotification，它在初始化过程中没有什么特别重要的用处，但是在删除设备接口的时候非常重要，因为它只响应删除事件

这个 queue 之所以重要，在于我们将来在接收报文的章节中要对它大书特书，不然，就不可能知道 Linux 网络接收过程的整个环节。不过，在此处，读者只需记住 2 点：

1. 这个 queue 有一个名叫 backlog_dev 的设备，其 poll 函数指针指向了一个叫做 processbackslog 的函数：  
2. 我们设置的接收软中断 (RX_SOFTIRQ) 将要和这个 queue 以及 back_log 设备打交道。

# 2.6.1 底层PCI模块的初始化

虽然我们不必太关心设备管理是如何实现的，但是我们应该知道我们的驱动程序是如何与设备搭上关系的。在目前的主机上，PCI总线是用得最广泛的总线技术，而基于PCI总线的网卡设备已经是市场主流，我们就研究一下PCI网卡是如何被操控的，以此可以推断出在不同总线技术下驱动程序的实现基础。

我们已经知道万事都得有头，驱动程序也不例外。在之前提到的4个大的步骤里，驱动程序开发人员使用module_init宏来修饰自己驱动程序的第一个函数，促使初始化函数放在第6段initcall段中（参考2.2.3节和2.5节）。驱动程序必须遵守一套开发框架，如果是PCI驱动，那么为了做到这一点必须调用一个函数：pci_module_init，它就是整个驱动程序开始工作的第一步，它也能替你完成底层关于PCI操作，如图2-24所示。不过在Linux 2.6中它已经变成include/linux目录下的pci.h中的宏，它被定义成pci register driver。其参数是一个pci Driver{}类型的结构体，此结构由驱动开发人员定义。下面还会介绍它。

![](images/1ed528883a7eb520b05931166e227e18e99de7c6e104470306a18ebcb9f1d665.jpg)  
图2-24 pol_module_init函数调用树

根据图2-24顺藤摸瓜，最终会找到driver_att函数。先仔细分析代码段2-32。

代码段2-32 dd.c driver.attach函数  
1. /\*\*   
2. \* driver Attached -尝试着把驱动绑定到设备上   
3. \* Edrv: 代表驱动程序的结构.   
4.   
5.遍历总线上挂着的设备链表，并尝试匹配每个驱动，如果bus_nacth（）返回0并且dev->driver被设置了，那么也就找到了匹配对。注意，我们忽略返回值为-ENODEV的结果，因为很有可能一个驱动程序不用绑定到一个设备上，这种情况其实很常见，主要是某些需求要求运行在内核，但又没有实际的设备与之对应，人们以内核模块的方式将这种代码植入内核——比如病毒。   
6.\*/   
7.void driver Attached(struct device_driver\* DRV)   
8.{   
9. struct bus_type \* bus $=$ DRV->bus;   
10. struct list_head \* entry;   
11. int error;   
12.   
13.if(!bus->match)   
14. return;   
/\*遍历整个总线链表，找到与设备匹配的驱动程序，这说明总线上挂的不止一种设备\*/   
15. list_for_each (entry, sbus->devices.list) {   
16. struct device \* dev $=$ container_of (entry,struct device,bus_list);   
17. if(!dev->driver) {   
18. error $=$ bus_MATCH (dev, DRV);   
19. .   
20. }   
21. }   
22.}

我们就没有必要去研究bus_math内部做了什么了，要把它说明白就偏离了本书的主线。我们只给出这个函数的大致流程，如图2-25所示。

![](images/24ad41c7c34020195daf578d4d6a97511d1f3358a3e7580fe6e0210f6644fbf4.jpg)  
图2-25 bus_MATCH函数调用树

图2-25显示了bus_MATCH函数调用树，它最后会调用_pci_device Probe，这个函数非常

重要, 和所有驱动程序有非常大的关系, 代码段 2-33 是其实现。

代码段2-33pci_driver.c __pci_deviceprobe函数  
```c
1. /\*\*  
2. \*如果成功就返回0，否则返回非0值。  
3. \*side-effect:pci_dev->driver is set to drv when drv claimspci_dev。  
4. \*/  
5. static int  
6. __pci_deviceProbe(structpci_driver\*drv,structpci_dev\*pci_dev)  
7.{  
8. int error = 0;  
9.  
10. if(!pci_dev->driver&&drv->probe){  
11. error =pci_device probesStatic(drp,pci_dev);  
12. if(error=-ENODEV)  
13. error =pci_device probe dynamic(drp,pci_dev);  
14. }  
15. return error;  
16. } 
```

不管是哪一种 probe，都最终调用 $\mathrm{d}\mathrm{r}\mathrm{v} \Rightarrow \mathrm{p}\mathrm{o}\mathrm{b}\mathrm{e}$ ，这是一个函数指针，它是由驱动程序的开发者设置的。下面就以网络设备的驱动程序为例，看看此 probe 是如何被指定的，它完成了什么工作。图 2-26 表示 $\mathrm{d}\mathrm{v} \Rightarrow \mathrm{p}\mathrm{o}\mathrm{b}\mathrm{e}$ 可以被两个函数调用，分别是pci_device Probe(static)和pci_device ProbeDynamic函数，它们先分别调用pci_MATCH_device 和pci_MATCH_one_device函数，最后才会调用它。

![](images/4cb0e1e2b06e680ae625a86ab1180a94f6d6014bd9ec1065baadd4c73b5e89f0.jpg)  
图2-26 dv->probe的被调用关系树

在_pci register driver 调用 driver register 之后会调用 pci_create_newid_file 函数，它会调用 sysfs_create_file 函数为当前的驱动程序创建相应的文件，文件放在/sys 目录下。请参考前文所述。

# 2.6.2 网络设备接口初始化例程

net_dev_init为我们准备好了网络设备的基础功能部件，那特定的设备驱动程序在哪被初始化呢？

先别急，我们得先知道驱动程序是如何被装入内存的。我们上面章节曾分析过 ELF 格式，每个驱动程序编写者通过设置自己的驱动程序入口函数——比如 xxx_init_module——为 module_init 类型的，那么在编译以后入口函数会放在特殊的 text 段中以至于系统在启动时

候能找到这个入口函数，就这样，系统遍历并执行各个入口函数，让这些驱动程序完成基本的加载。图 2-27 中就是驱动程序被装入内存中的实现过程，其中包括我们熟知的 loopback 接口，它也作为一种设备，在这个阶段被装入内存，而且，它还主动完成了其他驱动程序没有做的事——register_netdev。

![](images/ee4a3b85510a1127433790a1efa4a08e4ed9bef03b70325d8547a975f10825b9.jpg)  
图2-27 系统装入各驱动程序的步骤

每个驱动程序调用pci_module_init函数，传入一个pci_driver{}结构，如代码段2-34所示。如我们假设有一个设备，给它取一个“xxx”的名字。

代码段2-34pci_driver结构和pci_device_id结构  
```c
static struct pcidriver xxx_driver = {  
    name = "xxx",  /*驱动模块的名字，可任由开发者定义*/  
    /*这是一个pci_device_id()结构的数组，必须和设备的硬件信息一致*/  
    id_table = xxx_pci_tbl,  
    /*比如Intel的vendorID为0x8086  
BROADCOM的vendorID是0x14e4 */  
    struct pcid_device_id {  
        __u32 vendor, device; /*厂商或设备ID*/  
        __u32 subvendor, subdevice; /*子系统ID*/  
        __u32 class, class_mask;  
        kernel_ulong_t driver_data; /*驱动程序私有数据*/  
    };  
};
```

有的设备驱动定义其.probe函数名字比较有特色，笔者个人的笔记本网卡驱动就定义成eepro100_init_one。

不过要注意的是这只是加载，并不是对设备进行初始化。对于Linux初学者会有错觉。一个明证就是：我们在配置内核的编译时，比如在配置网络设备一节时，会看到多种网络设

备驱动被编译到内核中，但是实际上能起作用的就只是与主机网口真正匹配的驱动程序能工作。也就是说，那些没有相应设备的驱动程序根本就找不到自己的设备，也就无所谓“初始化”了。举个例子，本人主机网卡是 Ether Express Pro 100，但在在配置编译选项时，如果选择同时编译 Ether ExpressPro100 和 Intel Pro/100+ 并都是内嵌入内核时，系统会先初始化前者，而后者只执行到 pcj bus match 就返回了 0——没有能够找到相应的设备。

那么真正的初始化在哪呢？

结合上一节介绍的底层PCI模块的初始化一节中，我们知道每个驱动程序必须设置代码中的device id{}结构。驱动程序把这个结构传入PCI数据库，当PCI开始工作以后，它会扫描总线和设备的device id，然后查找数据库中的每个驱动，如果与之匹配，那么就会调用之前注册到PCI库中的dev $\Rightarrow$ probe（）函数。每个驱动程序要自己写probe()回调函数，完成检查寄存器和真正初始化设备的工作。其通用的工作流程如图2-28所示。

![](images/141a35579d8d981f76ff4673c02c7b3b433bb9e052fd2f1c4c379746bf3b83b6.jpg)  
图2-28 dv→probe 实现的基本功能

dev_new_index 中分配一个 ifindex 给设备，所谓分配，其实就是一个 static 整数不

断往上加1，来一个设备就加1。

- 在Linux 2.6早期版本中用dev_base数组来串联每个设备，但是这种方法在查找上不方便，而且扩展性差（当时只有8个设备），随着Linux在路由器和交换机设备上的应用，这种方法不能适应这种类型设备多接口的特性，转而采用dev_name_head来记录。此全局变量是一个hash表，以设备的名字作为输入，再通过一系列hash变换得到一个key，来查找和增删某设备，如果碰到接口特别多的情况比如有128个接口，或者有4095个VLAN，这样的hash查找就比较快了（Linux下VLAN也是一种特殊的“设备”）。当然系统中还有使用dev_base的地方，比如要搜索一个接口但并不知道其名字和接口，只知道接口的ip地址，那么就只好从这个表找——这就是路由查找的本质。  
static struct hlist_head dev_name_head[1 << NETDEV_HASHBITS], 也就是说该hash数组有15个单元，比以前的8个多，如果发生key冲突可以用链表来挂接。  
同样的道理，网口的ifindex也用hash表来存储了，名字叫dev_index_head为了讲清楚以后的问题，先列出register_netdevice函数，请看代码段2-35。

代码段2-35 dev.c register_netdevice函数  
```c
1. /\*\*  
2. \* register_netdevice - 注册一个网络设备  
3. \* @dev: 是将要创建的设备指针，我们后面会详细给出它的结构定义  
4. \*此函数把一个网络设备加到内核中，然后发送一个NETDEVRegister消息给netdev通知链，如果成返回0，如果失败就返回一个负的错误号，指示其出现什么样的错误，你也可以调用register_netdev()代替这个函数  
5. \*/  
6.  
7. int register_netdevice(struct net_device *dev)  
8.  
9. struct hlist_head *head;  
10. struct hlist_node *p;  
11. int ret;  
12.  
13. /* 当设备不存在时，会引起一个致命错误 */  
14. BUG_ON (dev->reg_state != NETREG_UNINITIALIZED);  
15. ...  
16. dev->iflink = -1;  
17.  
18. /* 如果init存在就会调用，如果不存在也不会影响下面的代码执行 */  
19. if (dev->init) {  
20. ret = dev->init(dev);  
21. ...  
22. 1 注意这里只是检查index和name的合法性，  
23.  
24. if (!dev_valid_name(dev->name)) {后面才把dev放入这两个hash表中。  
25. ...  
26.  
27.  
28. dev->ifindex = dev_new_index();  
29. if (dev->iflink == -1)  
30. dev->iflink = dev->ifindex;
```

/检查是否有同名设备*/

```c
head = dev_name_hash (dev->name);  
hlist_for_each(p, head) {  
    struct net_device *d = hlist_entry(p, struct net_device, name_hlist);  
    if (!strcmp(d->name, dev->name, IPNAMESI2)) {  
       说明已经有一个同名的设备在系统中了，  
    最好给你的设备起一个独一无二的名字  
    ret = -EEXIST;  
    ...  
}
```

```txt
/* 
*     nil rebuild_header routine, 
*     that should be never called and used as just bug trap. 
```

```txt
if(!dev->rebuild_header) dev->rebuild_header = default_rebuild_header; 
```

```txt
/* 把设备放入 net_class 类中的目录下，这里面比较复杂而无趣，主要是 sysfs 管理驱动程序的例程，不管它了。*/
```

```txt
rat - netdev_register_sysfs (dev) ; 
```

```txt
/* 明确该设备已经注册过了，以后在进入此函数将会产生一个错误 */
```

```txt
dev->reg_state = NETREG_REGISTERED; 
```

```txt
把设备放入dev_base链表中，这里devtail也是一个全局变量
```

```c
set_bit(_LINK_STATE_present, &dev->state);  
dev->next = NULL;  
*dev.tail = dev;  
dev.tail = &dev->next; 
```

```txt
下面才是把设备分别放入name表和index表*/
```

```txt
hlist_add_head(&dev->name_hlist, head); 
```

```txt
hlist_add_head(&dev->index_hlist, dev_index_hash(dev->ifindex))); 
```

```txt
/* 注册一个通知块，表示检测到一个新设备*/
```

```txt
raw.notifier_call_chain(@netdev_chain, NETDEV_REGISTER, dev); 
```

```txt
ret = 0; 
```

79.

80. out:   
81. return ret;   
82.1

下面这个结构就是刚才一直说的 net_device，它显得非常庞大，这其实是 Linux 内核目前不适合用作高性能操作系统的地方。连它的注释也说了，这是一个 big mistake。请看代码段 2-36。

代码段2-36 netdevice.h net_device结构  
1. $\text{巧}$ 2.Actually，this whole structure is a big mistake.它把I/O数据和高层的数据结构牢牢的据  
在一起，而且开发人员必须知道几乎每个成员在INET模块中的用途。解决办法就是把这个结构中的网络协议  
信息清除出去以至于这个结构看起来清爽一点。  
3. $\text{巧}$ 4.   
5. struct net_device  
6. [  
7.   
8. /\*下面这个成员是接口的名字，它是第一个对这个结构“可见”的字段，也就是说，  
9. 我们高层软件是可以看到这个字符串的内容\*/  
10. char name[IFNAMSBZ];  
11.   
12. /\*特定的I/O区域，主要是内存共享之用，注释里有这么一句；  
13. \*FIXME:Merge these and struct ifmap into one  
14. \*/  
15. ulong men_end;/*shared nam end*/  
16. ulong men_start; /*shared mem start *//\*x86处理器主要使用IO地址空间，而其他架构得处理器使用内存映射IO，IRQ是设备中断号，  
x86处理器也用它\*/  
17. ulong base_addr;/*device I/O address*/  
18. uint irq; /*device IRQ number */  
19.   
20. /\*  
21. \*Some hardware also needs these fields,but they are not  
22. \*part of the usual set specified in Space.c.  
23. /\*  
24. uchar if_port;/*可选择的AUI,TP,...*/  
25. uchar dna; /*DNA通道*/  
26. ulong state;  
27.   
28. struct net_device \*next;  
29.   
30. /\*设备初始化函数，只被调用一次\*/  
31. int (*init)(struct net_device \*dev);  
32.   
33. /\*----Fields preinitialized in Space.c finish here --*/  
34. struct net_device \*next_sched;  
35.   
36. /\*接口索引，是接口的惟一ID\*/  
37. int ifindex;  
38. int iflink;

```c
40. struct net_devicestats* (*getstats) (struct net_device *dev);  
41.  
42. /* 以下是和协议特定的数据 */  
43. void *ip_ptr; /* IPv4 的数据 */  
44. void *ip6_ptr; /* IPv6 的数据 */  
45. void *ax25_ptr; /* AX.25 的数据 */  
/* 还有一些网络协议的ptr指针没有列出来 */  
46. struct list_head poll_list; /* Link to poll list */  
47. int quote;  
48. int weight;  
/* 每个网络设备对应一个队列（也可以没有，例如 lo），它将网络层与设备驱动区分开来。数据包在网络层处理完之后，可能会缓冲到此队列中。这里也只是可能会缓冲，因为数据包被送到此队列后，如果条件允许，立刻就被发送出去。  
此队列是Linux内核中实现QoS的关键。不同的QoS策略采用不同的方式对此队列中的数据包进行处理，默认的方式是FIFO。如果底层driver处理速度跟不上发送数据包的速度，那么当队列满了以后，后续的数据包就会被丢弃。*/  
49. struct Qdisc *qdisc;  
50. struct Qdisc *qdisc Sleeping;  
51. struct Qdisc *qdisc_list;  
52. struct Qdisc *qdisc_ingress;  
53. unsigned long tx_queue_len; /* 每个队列中允许存放的最多报文数量 */  
54.  
55. */  
56. *This marks the end of the "visible" part of the structure. All  
57. *fields hereafter are internal to the system, and may change at  
58. *will (read: may be cleaned up at will).  
59. */  
60. ushort flags; /* 接口标志，与BSD风格保持一致 */  
61. ushort qflags;  
62. /*下面这个标志和'flags'一样，但是对用户空间不可见，在本书中要提到的章节和这个字段有很大关系，它表示设备的类型，而且主要是虚拟设备的标识。表2-7列出其中几种*/  
ushort priv_flags;  
......  
63. uint mtu; /* 接口的最大传输单元（NTU）值 */  
64. ushort type; /* 接口的硬件类型，只在组ARP包的时候有用，表2-8所示是常见的类型 */  
65. ushort hard_header_len; /* hardware hdr length */  
/*指向设备特定数据，其实这段数据就紧跟在net_device()结构后面 */  
66. void *pri;  
67.  
68. struct net_device *master; /* 指向一个组里的主（Master）设备，比如本书后面提到的聚合链路组，其中必有一个主链路，这里的master指针就指向这个主链路的设备 */  
69.  
70. /*接口地址信息。*/  
71. uchar broadcast [MAX_ADDR_LEN]; /* 硬件广播地址 */  
72. uchar dev_addr [MAX_ADDR_LEN]; /* 这就是我们常说的MAC地址 */  
73. uchar addr_len; /* 硬件地址长度，对于以太网设备，必然是6 */  
74.  
75. int promiscuity; /* 是否处于混杂模式，如果是的话，就会把接口收到的报文全部送上去 */  
76.  
77. int watchdog_timeo; /* 链路状态扫描时间间隔，后面的章节会提到 */  
78. struct timer_list watchdog_timer; /* 扫描链路状态的定时器函数结构 */
```

```c
80. /\*此变量将会加入设备名字hash链，即dev_name_head变量\*/   
81. struct hlist_node name_hlist;   
82. /\*此变量将会加入设备名字hash链，即dev_index_head变量\*/   
83 struct hlist_node index_hlist;   
84.   
85 /\*注册/注销状态机\*/   
86. enum{NETREG_UNINITIALIZED=0,   
87. NETREG_REGISTERING, \*/表示正由register_netdevice函数调用\*/   
88. NETREG_REGISTERED, \*/已经完全注册成功了\*/   
89. NETREG_UNREGISTERING, \*/表示正由unregister_netdevice函数调用\*/   
90. NETREG_UNREGISTERED, \*/已经完全注册成功了\*/   
91. NETREG_RELEASED, \*/由free_netdev函数调用\*/   
92. }reg_state;   
93.   
94. /\*关于网络设备的一些特征，常见的如表2-9所示\*/   
95. int features;   
96. /\*下面是接口设备特定的内核函数\*/   
97. int (*open)(struct net_device \*dev);   
98. int (*stop)(struct net_device \*dev);   
99. int (*hard_start_xmit) (struct sk BUFF \*skb,   
100. struct net_device \*dev);   
101.#define HAVE_NETDEV POLL   
102. int (*poll)(struct net_device \*dev,int \*quota);   
103. int (*hard_header)(struct skbuff \*skb,   
104. struct net_device \*dev,   
105. unsigned short type,   
106. void \*daddr,   
107. void \*saddr,   
108. unsigned len);   
109. int (*rebuild_header)(struct skbuff \*skb);   
110.#define HAVE_SET_MAC_ADDR   
111. int (*set_mac_address)(struct net_device \*dev,   
112. void \*addr);   
113.#define HAVE_PRIVATE_IOCTL   
114. int (*do_IOctl)(struct net_device \*dev,   
115. struct ifreq \*ifr,int cmd);   
116.#define HAVE_SET_CONFIG   
117. int (*set_config)(struct net_device \*dev,   
118. struct ifmap \*map);   
119.#define HAVEHEADER_CACHE   
120. int (*hard_header_cache)(struct neighbour \*neigh,   
121. struct hh_cache \*hh);   
122. void (*header_cache_update)(struct hh_cache \*hh,   
123. struct net_device \*dev,   
124. unsigned char *haddr);   
125....   
126..   
127. int (*hard_headerISAe)(struct skbuff \*skb,   
128. unsigned char \*haddr);   
129. int (*neigh_setup)(struct net_device \*dev, struct neigh_parms\*);   
130. (*accept_fastpath)(struct net_device \*,struct dat_entry\*);   
131.#ifdef CONFIG_NETPOLL_RX   
132. int netpoll_rx; 
```

```c
133. #endif  
134. #ifdef CONFIG_NET POLL_CONTROLER  
135. void (*poll_controler) (struct net_device *dev);  
136. #endif  
137.  
138. /* 桥端口的属性 */  
139. struct net_bridge_port*br_port;  
140.  
141. ...  
142. /* class/net/name entry */  
413 struct class_device class_dev;  
144.; 
```

表 2-7 net_device→priv_flags 的值  

<table><tr><td>宏</td><td>值</td><td>含义</td></tr><tr><td>IFF_802_IQ_VLAN</td><td>0x1</td><td>属于B02.IQ的虚拟网络设备,在VLAN章节中会看到</td></tr><tr><td>IFF_EBRIDGE</td><td>0x2</td><td>以太网断设备,在生成树(STP协议中用到)</td></tr><tr><td>IFF MASTER_8023AD</td><td>0x8</td><td>聚合端口设备,在LACP章节中会看到</td></tr></table>

表 2-8 net_device→type 的值  

<table><tr><td>宏</td><td>值</td><td>说明</td></tr><tr><td>ARPHRD_EITHER</td><td>1</td><td>普通以太网（10Mb/s）</td></tr><tr><td>ARPHRD_IEEE802</td><td>6</td><td>IEEE 802.2 以太网/TR/TH</td></tr></table>

表 2-9 net_device→features 的值  

<table><tr><td>宏定义</td><td>值</td><td>说明</td></tr><tr><td>NETIF_F_SG</td><td>1</td><td>Scatter/gather IO</td></tr><tr><td>NETIF_F_IP_CSUM</td><td>2</td><td>Can checksum only TCP/UDP over IPv4</td></tr><tr><td>NETIF_F_NO_CSUM</td><td>4</td><td>不需要做 checksum,比如loopack设备</td></tr><tr><td>NETIF_F_HW_CSUM</td><td>8</td><td>可以对所有报文做 checksum</td></tr><tr><td>NETIF_F_HW_VLAN_TX</td><td>128</td><td>通过硬件发送VLAN报文,这样可以加速发送</td></tr><tr><td>NETIF_F_HW_VLAN_RX</td><td>256</td><td>通过硬件接收VLAN报文,加速接收</td></tr><tr><td>NETIF_F_HW_CHALLENGED</td><td>1024</td><td>设备不支持VLAN报文,报文可能会被认为是错包而被设备丢弃</td></tr></table>

- 有一类设备, 在协议栈里非常特殊, 在每一种 TCP/IP 实现里, 都实现了这样一个设备, 不过, 它不是真正的设备, 而只是一个虚拟的, 用来做调试用的接口, 它就是 loopback 设备。在协议栈初始化的时候, 这个接口必然要创建, 而且一直存在。现在, 我们来看看这个接口是如何初始化的。刚才提到了, 它在被装入的时候, 主动调用了 register_netdev, 而不是由 PCI 模块回调它, 原因很简单, 没有实际设备的 id 与 loopback 设备相符, PCI 自然不会调用它, 所以注册的事情就必须自己动手, 丰衣足食。与 Linux 2.4 内核不同的是, 在 Linux 2.6 内核中, loopback 接口设备的初始化被移到 net_olddevs_init 中。

- 当网卡是以模块动态加载方式初始化的时候，netdev.boot_base 返回 0。因为 net_olddevs_init 在各模块加载之前执行。  
- 当网卡以嵌入内核代码方式初始化的时候，netdev.boot_base回返回1。因为嵌入模块已经被初始化了，所以在net_olddevs_init函数执行的时候可以扫描到设备的存在。下面代码段2-37是loopback设备的定义。

代码段2-37 loopback.c loopback_dev 结构  
```txt
1. struct net_device loopback_dev = {
2. .name = "lo",
3. .mtu = (16 * 1024) + 20 + 20 + 12,
4. .hard_start_xmit = loopback_xmit,
5. .hard_header = ath_header,
6. .hard_header_cache = eth_header_cache,
7. .header_cache_update = ath_header_cache_update,
8. .hard_header_len = ETH_LEN, /* 14个字节 */
9. .addr_len = ETH_ALEN, /* 6个字节 */
10. .tx_queue_len = 0,
11. .type = ARPHD_LOCKBACK, /* 0x0001 */
12. .rebuild_header = eth_rebuild_header,
13. .flags = IFF_LOCKBACK,
14. .features = NETIF_F_SG | NETIF_F_FRAGLIST
15. | NETIF_F_NO_CSUM | NETIF_F_HIGHDMA | NETIF_F_LTTX,
16. }; 
```

net_device{}结构中有2个非常重要的指针：ip_ptr和priv。ip_ptr指向的是更高层次的数据结构，而priv指向的是设备底层的私有数据。因为在Linux内核维护人员来看，大部分网络设备具有的共性，和与物理硬件无关的数据可以归纳为一个数据结构，这是设备抽象层维护的结构，而硬件自己特有的数据比如寄存器和硬件缓冲区由驱动开发人员维护，但也不确定这些数据的大小和类型，于是使用一个void指针来指向。要注意的是net_device和设备私有数据是放在连续的空间的，请读者自行去参考alloc_netdev函数，这减少了申请内存的次数，也不会导致2次释放内存。

而且 net_device{} 这个结构也不必被上层的协议栈操作，比如设备可能有 IP 地址，也可能没有 IP 地址，更重要的是，该设备也不应该只和 IP 协议打叫道，可能还有 PPP 协议、SLIP、X25 协议，所以应该再找一个跟此结构相关但却和协议相关的“代理结构”来记录这些信息。由于并不肯定是和 IP 协议栈交互，那么指向这个“代理结构”的指针类型还是 void 类型。当然按照命名规矩，在 IP 协议栈的框架内，提出了一个 in_device{} 的结构，in 就是 ip network 的意思啦。虽然目前 net_device{} 列出了 ip_ptr 和其他网络的指针，但我敢预言将来这几个指针会用“联合”来代替，除非该设备不仅工作在 IP 网络上还工作在其他类型的网络上。

代码段2-38,inetdevice.h in_device结构  
```txt
1. struct in_device  
2.  
3. struct net_device *dev;  
4. atomic_t refcnt;  
5. int dead; 
```

```c
/\*维护设备的地址列表。对的，是列表，一个设备可以有多个地址，对于普通PC机来说似乎比较少见，但在服务器上比较常用。我们在后面的章节中会介绍它的\*/  
6. struct in_ifaddr \*ifa_list; /\*IP ifaddr chain \*/  
/\*下面m开头这几个成员都是和组播有关系，我删去了好几个\*/  
7. struct ip_mc_list \*mc_list; /\*组播IP过滤链表\*/  
8. struct ip_mc_list \*mc_tomb;  
9. unsigned long mr_v1_seen;  
10....  
11. 维护arp协议互操作的参数  
12. struct neigh��s \*arp��s;  
13. struct ipv4_devconf cnf;  
14.};
```

这个结构和 net_device 的关系如图 2-29 所示，这个关系在 inetdev_init 函数中确定。

![](images/e11d1c8ed46707cffa7b18f173e10814e3c917ffc1de5524b21ce64216dd74ae.jpg)  
图2-29 net_device和in_device、设备特定数据之间的关系

设备无关层采用 in device{} 数据结构保存 IP 地址和邻居信息——虽然是间接的；  
- 网络抽象层采用 net_device{} 数据结构保存设备的名字、编号、地址等共性:  
- 设备特定层的数据则由设备驱动开发人员自己定义, 一般有硬件发送、接收缓冲区、芯片寄存器的信息等。这片内存区一般是紧跟在 net_device{}后面, 由驱动程序在创建 net_device{}的时候顺带把这块内存也创建了。当然还是用 priv 指针指向, 以方便访问。

设备已经被注册了，那么是否就可以工作了呢？不是的，还得靠用户把这些网卡激活，当然，一般都有脚本在系统初始化的时候干这样的活。网卡被激活的时候，它要完成几个非常重用的事情：

(1) 挂接中断处理函数 (ISR), 如果不能为驱动程序申请到中断, 那说明要么网卡没插好, 要么和其他设备发生了冲突, 结果就是设备根本不能用。  
(2) 创建驱动程序内部接收环和发送缓冲区, 网卡一般都要用“环”的方式来存放报文。  
(3) 挂接接口状态扫描定时器, 以 poll 的方式轮询接口是否真正 up 或 down。  
(4) 进一步打开设备特点寄存器, 使其可以开始收发报文了。

这个过程如图2-30所示。

![](images/d6f59d747b930504679260a7a4cb8220af3ee8f4752583804ef47aee02a2b734.jpg)  
图2-30 对设备的Ioctl过程

在上面的这几幅图中（见图2-27、图2-28和图2-30），读者一定都看到了那几个小红旗及方框，其中写明了此时代码要完成的基本任务。现在可以对驱动程序的初始化做一个基本的概括，即Linux 2.6下网络驱动程序的初始化分为4个基本步骤：

第1步，系统把驱动程序装入内存；

第2步，PCI为设备选择正确的驱动程序，并分配相应内存数据结构；

第3步，指定驱动程序如何处理报文格式；

第4步，用户打开设备使其可以真正工作起来。

关于底层驱动的架构已经基本介绍完了，在这里还得提醒读者，在本书中，设备就是接口，接口就是设备，它们在一般情况下是同等意义。也许有读者提出一块网卡可能有2个接口，那么这算多少设备呢？可以这样解答：设备驱动程序可以只有一份，但是你创建的net_device{}结构必须要2个，也就是说，Linux内核本来不是用来做路由器的，现在你要实现路由器级别的Linux，那么你应该接受内核中一些编码风格，那个net_device实际可以改名为net_INTERFACE等。所以，本书中“接口”和“设备”，的含义基本上是接近“接口”概念的。

# TINUX

# 第3章

# 配置网络系统

可以说本章是IP层实现的最核心部分。

一般书籍会告诉你这个路由算法，那个路由协议，但就是没告诉你IP层到底如何组织和利用这些路由信息的。从本章的标题上看，似乎也就是配配网络IP地址之类“低级”的工作。其实不然，读者有没有想过配置IP地址会引起系统发生什么样的变化吗？由配置IP地址是否可以想到路由表项的下发呢？本章将为你揭示IP路由表系统的内幕。

![](images/edf803c2e2e3e6f05bfcbb01c5a534611a7664917fd36daee382eb323e90a97d.jpg)

# 3.1 配置过程分析

路由器中的路由有两种：直连路由和非直连路由。直连路由是在配置完路由器网络接口的 IP 地址后自动生成的，路由器各网络接口所直连的网络之间使用直连路由进行通信。非直连路由就是大家熟知的动态路由和静态路由的总称，所以直连路由的重要性可见一般。而配直连路由的动作就是配置接口地址的过程。因为直连路由是由链路层协议发现的，该路由信息不需要网络管理员维护，也不需要路由器通过某种算法进行计算获得，只要该接口处于活动（Active）状态，路由器就会把通向该网段的路由信息填写到路由表中去，直连路由无法使路由器获取与其不直接相连的路由信息。在本章，我们先介绍配置普通设备的 IP 地址的内部过程，接着再转到 loopback 接口的配置过程，这两个过程有相似之处，所以在一起讲叙。通过介绍接口的 IP 地址配置过程，转入 FIB 系统，讲解我们最感兴趣的路由系统，并用图例演示路由表的变化。最后介绍接口状态的变化，这对于驱动程序开发人员来说也是比较重要的。

# 3.1.1 配置是如何下达到内核的

假设在安装我们的Linux系统时，没有配置IP地址，也没有挂上网线，完完全全是一台“裸机”，这样方便我们跟踪系统到底做了什么。

安装完系统之后，我们可以使用 ifconfig 命令，查看设备信息。我想读者们应该都知道这么一个命令吧。在 Windows 上相应的操作是 ipconfig。带上“-a”参数表示要查看详细的配置，包括 loopback 设备。

ifconfig -a   
```txt
eth0 Link encap: Ethernet HWaddr 00:80:C8:EB:2A:39  
BROADCAST MULTICAST MTU:1500 Metric:1  
RX packets:0 errors:0 dropped:0 overruns:0 frame:0  
TX packets:0 errors:0 dropped:0 overruns:0 carrier:0  
collisions:0 txqueuelen:1000  
RX bytes:0 (0.0 B) TX bytes:0 (0.0 b) Interrupt:5 
```

lo Link encap:Local Loopback   
```txt
inet addr:127.0.0.1 Mask:255.0.0.0 
```

```txt
UP LOOPBACK RUNNING MTU:16436 Metric: I 
```

```txt
RX packets:6 errors:0 dropped:0 overruns:0 frame:0 
```

```txt
TX packets:6 errors:0 dropped:0 overruns:0 carrier:0 
```

```txt
collisions:0 tqueuelen:0 
```

```txt
RXbytes:0 (0.0b) TXbytes:0 (0.0b) 
```

不同的机器和网卡会显示会有不同。例如我的另外一台机器，其eth0的Interrupt等于3。现在运行#strace ifconfig eth0 192.168.18.2 netmask 255.255.255.0，配置好ip地址和网络掩码。为什么要运行strace？在大部分系统上是没有ifconfig的源代码的，那么为了查看ifconfig内部完成了什么操作时，可以用strace命令查看。它收集应用程序执行的系统调用，甚至参数都能记录下来。通过对这个命令的输出进行整理，可以把ifconfig内部调用的系统接口整理为如代码段3-1所示。

代码段3-1 配置ip地址的用户层代码  
```c
1. main (int argc, char **argv)  
1. [  
2. struct sockaddr sa;  
3. struct sockaddr_in ain;  
4. char host[128];  
5. struct aftype *ap;  
6. struct hwtype *hw;  
7. struct ifreq ifr;  
8. char **app;  
9. int fd;  
10.  
11. fd = socket(FF_INET, SOCK_DGRAM, IPPROTO_IP);  
12. ifr.ifr_name = "eth0";  
13. ap = inet_iftype =  
14. {  
15. "inet", NULL, "/**DARPA Internet", "/ AF_INET, sizeof(unsigned long),  
16. INETprint, INET_sprint, INET_input, INET_reserror,  
17. NULL /*INET_rprint *, NULL /*INET_rinput */ ,  
18. INET_getnetmask,  
19. -1, /*这个值会被赋成fd，即刚才打开的socket*/  
20. NULL  
21. ];  
22. host = "192.168.18.2";  
23. ap->input(0, host, sa); /*在此sa→sa_family=AF_INET, af→sa_data已经被设置成  
192.168.1.1 */  
24. memcpy((char *) ifr.ifr_addr, (char *) sa, sizeof(struct sockaddr));  
25. loct1(fd, SIOCSIPADDR, ifr);  
26. loct1(skfd, SIOCGIFFLAGS, ifr);  
27. loct1(skfd, SIOCSIPFLAGS, ifr);  
28. /*开始第二次遍历命令行*/  
29. host = "255.255.255.0";  
30. loct1(skfd, SIOCSIFNETMASK, ifr);  
31.  
32. ] 
```

以上就是 ifconfig 这个命令实际内部“伪”代码，为了减轻复杂度，我们不必把整个代码列出来。从上面的代码中可以看出 ifconfig 实际调用了 2 个系统函数：socket 和 ioct1。4 表示为 socket 打开的文件描述符，姑且认为这是应用程序到达系统内核的一个钥匙吧，后面会介绍一些相关知识。Socket 的参数以后也会详细介绍。然后是 ioct1 这个系统调用。在 Linux 相关的项目中，ioct1 是用户层与内核或设备驱动程序进行配置的一个有效手段。在这里我们

要配置系统的地址，则必定要通过 ioctl。在 ifconfig 的程序中依次调用 4 个 ioctl，完成了对系统地址的配置。其传入的 ioctl 码依次是 SIOCSIFADDR，SIOCGIFFLAGS，SIOCSIFFLAGS，SIOCSIFNETMASK。

下面我们就对出现的系统调用进行分析，首先是 socket。

# 3.1.2 socket系统调用

凡是了解网络编程的人几乎都知道 socket 系统调用，其具体形态和用法就不多说了。但是它在内核中做了什么事就不是每个人都能说清楚的，可以说，它决定了应用程序使用网络的程度，即使用协议栈的哪个层次。首先要知道一个很重要的数据结构，它保存了每次成功的 socket 系统调用在内核中的结果，其结构定义如代码段 3-2 所示。

代码段3-2 net.hsocket{结构  
```c
1. struct socket {  
2. struct proto_op8 *ops;  
3. struct file *file;  
4. struct sock *sk;  
5. wait_queue_head_t wait;  
6. short type;  
7. ...  
8. } 
```

可以看到此结构有 3 个指针还有一个等待队列及 type。我们可以把这 3 个指针看作一个套接字在内核中的路标，一个指向上层——file，一个指向下层——sock，最后指向自己的平行层——ops。下文会详细述说明这 3 个指针的来由。这里最简单的就是 type 字段，它是 socket 的第二个参数，如果要建立的是遵从 TCP/IP 的 socket，type 字段只能是 SOCK_STREAM、SOCK_DGRAM 或 SOCKRaw，分别对应 TCP 和 UDP 及 IP。对于等待队列，读者可以把它看成是一个链表上的节点，我们常见的等待接收报文的动作就是依托这个数据结构实现的，而且下层的 sock 结构的等待队列 sk_sleep 也是指向这个结构，我们会在后面的章节中细说。

前面我们已经介绍过VFS系统和文件系统，一个已安装的Linux操作系统究竟支持几种文件系统类型由文件系统的注册链表决定，当调用socket时要在该链表中找到网络文件系统然后创建对应文件描述符给应用程序使用。应用程序对内核的操作是通过是socket文件描述符，通过网络文件系统定义的通用接口，使用系统调用从用户空间切换到内核空间，控制socket文件描述符对应的内核操作函数，即BSD socket的操作。在BSD socket层中，操作对象就是socket{}结构。每一个这样的结构对应的是一个网络连接。通过网络地址族的不同来区分不同的操作方法，判断是否应该进入到INET socket层。

![](images/40544c4dd9214d3f6e272a5e7b5e2abfc15018b02334f9afe6fef17c10d8dd13.jpg)

谈到这里，我必须要提出一个看法，就是目前 Linux 协议栈内核采用了面向对象的方法去构造整个框架。面向对象的典型实现语言是 $\mathsf{C} + +$ （其实是基于对象，这里不作细分），面向对象的 3 个特征是封装、继承、多态、封装和继承是非常容易理解的，而多态在 $\mathsf{C} + +$ 是用虚拟函数实现，其实 C 语言是完全可以实现所有的特征的。

封装用结构，继承用包含，这都好办。多态呢？其实用函数指针即可实现。系统根据参数的不同会调用不同的函数，比如下面这段代码很好地演示了多态的实现。

using namespace std; typedef struct Shape void (*draw)();   
CShape; void drawRect() {cout $<  <   _{--}$ FUNCTION $<  <$ endl;} void drawEllipse() {cout $<  <   _{--}$ FUNCTION $<  <$ endl;} void drawCircle() {cout $<  <   _{--}$ FUNCTION $<  <$ endl;} int main () CShape s1, s2, s3; s1.draw = drawRect; s2.draw = drawEllipse; s3.draw = drawCircle; vector<CShape $^{\star >}$ v; v.push_back (&s1); v.push_back (&s2); v.push_back (&s3); for (int i = 0; i < v.size(); i++) { v[i]->draw(); } return l;

可以看到，v.push_back()的参数就是一个结构地址，这个地址指向带函数指针的结构。每一个实例的函数指针都指向了不同的函数，但是调用者却不用自己去区分传入的对象是什么，其内部也不用去判断对象的函数指针到底指向哪个函数，代码变得非常简洁，这不就是C++中的多态吗？当然，这里没有涉及到C++的虚拟函数表和动态绑定，但是这解决了向量v对实际draw()函数实现的依赖。

花这么大力气就是要告诉读者, 如果没有这个概念, 阅读协议栈源码是非常困难的。所以请大家仔细体会上面代码的精髓。

在 INET socket 层中，根据建立连接的类型，分成面向连接的和面向无连接两种类型，这是区分 TCP 和 UDP 协议的主要原则。这一层的操作对象主要是 sock {} 类型的数据，而数据存放在 sk_buffer 中。从 INET socket 层到 IP 层，主要是路由过程，发送时根据发送的目标地址确定需要使用的网络设备接口和下一需要传送的机器地址。

在内核中与 socket 对应的系统调用是 sys_s次会议，所谓的创建套接口，就是在 sockfs 这个文件系统中创建一个节点，从 Linux/UNIX 的角度来看，该节点是一个文件，不过这个文件具有非普通文件的属性，于是起了一个独特的名字——socket。由于 sockfs 文件系统是系统初始化时就保存在全局指针 sock_mnt 中的，所以申请一个 inode 的过程便以 sock_mnt 为参数。

从进程角度看，一个套接口就是一个特殊的已打开文件。现在将 socket 结构的宿主 inode 与文件系统挂上钩，就是分配文件号以及 file 结构在目录数中分配一个 dentry 结构。指向 inode 使 file->f_dentry 指向 inode 建立目录项和索引节点（即套接口的节点名），它们之间的关系如图 3-1 所示。

现在我们来看看每个进程是如何对待其打开的文件及 socket 的: 从图 3-1 中可以看到,

标准输入接口（Std in）的文件描述符占据了进程的用户空间的文件描述符数组的第一个单元，这是系统内定的。通过内核的组织，其在内核中的描述符数组的第一个单元存放了指向其inode 的地址，依此类推，标准输出接口（Std out）占据第二个单元，错误输出接口（Std err）占据第三个单元（图上没有画出）。而用户自己打开的文件或 socket，则依次排列到系统已经内定的错误输出接口的 fd 单元后面，比如，如果某进程第一次打开了一个常规文件，它的 fd 必定是 3，当再打开别的文件时，fd 就是 4，这里 3 和 4 就是用户空间中的 fd 数组下标。同样的道理，socket 也可以看作是一个文件。每次调用 socket 返回的 fd 值也是指用户空间的 fd 数组下标。

![](images/bb39bc0a22e5e13234dcbca604f5bc02b7d88b90038b1c4cf7ba4dc786fae56e.jpg)  
图3-1 FD的意义

现在我们来看看 socket 函数本身，经过 glibc 库对其封装，它将通过 int 0x80 产生一个软件中断（注意不是软中断），由内核导向执行 sys_SOCKET，基本上参数会原封不动地传入内核，它们分别是（1）int family，（2）int type，（3）int protocol。其调用树如图 3-2 所示。

由于不关心 security 方面的代码，我们就不研究 security_SOCKET_create 函数了。直接对 sock_alloc 进行分解，struct socket{} 结构就是它创建的，它的调用树如图 3-3 所示。

![](images/4dc4d9b807ea60736eb36601242c203f753a9fff2257083d6e89612134ded107.jpg)  
图3-2sys_SOCKET的函数调用树

![](images/208929c90743442e72273cc10ab8d0ee002415630fb24cb545405395e3a92f3a.jpg)  
图3-3 sock_alloc函数调用树

我们的mnt_sb $\rightarrow$ s_op $\rightarrow$ alloc_inode就是前文中静态全局变量super_operations sockfsOps定义的sock_alloc_inode函数。它仅仅是用kmem_cache_alloc创建一个socket_alloc{}类型的inode，然后返回给alloc_inode进行初始化。所以纵观socket_alloc()的作用就是分配及初始化属于网络类型（应该是socket_alloc类型）的inode。inode结构中的大部分字段只是对真正的文件系统重要，但是，只有一部分由socket使用。socket_alloc类型的inode结构如图3-4所示。

![](images/920a9033159d61075cba01f4fca0b523073843fb28a9c98a5ef34b2ca4920f08.jpg)  
图3-4 soket_alloc结构

此图中的 socket socket 部分就是图 3-1 “FD 的意义” 中那个“特定文件的数据”部分。那么 socket{}结构就表示这是一个跟网络有关的文件描述符，是 INET 层与应用层打开的文件描述一一对应的实体，每一次调用 socket 函数都会在 INET 中保存这么一个实体。

在常规文件系统中常用的 open 系统调用不能用来创建一个 socket, sockets 只能用 socket API 创建。一旦 socket 被创建, IO 操作就和普通文件或设备一样了。sock_alloc 实际上创建了 socket 结构并从 socket inode slab cache 中创建 inode 结构。SOCKET_I 和 SOCK_INDEX 可以相互转换 inode 和 socket。内核通过 fd 找到内存中对应的 inode, 然后再通过 VFS 系统查找到对应的内核模块, 这样用户空间和内核协议栈就搭上了关系。一旦 inode 被创建, socket 层就可以映射 IO 系统调用了。一个 file_operations 结构, socket_fileOps 被创建并初始化, 其中所有的 IO 系统调用都有对应的 socket 对应操作。

注: open 调用返回一个 ENXIO 错误, 参见代码段 3-3。

代码段3-3 socket.c socket_fileOps结构定义  
1. /\*   
2. Socket类型的文件系统和其他文件系统一样有一个特别的操作集合，但是有相当一部分操作是经过直接调 用系统接口而不是通过这个操作集合来完成。   
3. \*/   
4. static struct file_operations socket_file_opa $=$ {   
5. .owner $\equiv$ THIS_MODULE,   
6. .llseek $=$ no_11seek,   
7. ....   
8. .poll $=$ sock poll,   
9. .unlocked_ioctl = sock_ioctl,   
10. .open $=$ sock_no_open,/\*这个open操作明显是socket系统不支持的\*/   
11. .release $=$ sock_close,   
12. ..   
13. .readv $=$ sock_readv,   
14. .writev $=$ sock_writev,   
15. ...   
16. }；

到此为止，我们可以作出结论，VFS 为了使 socket 系统工作——或者说 socket 为了适应 VFS 系统框架，socket 提供了 2 个数据结构：super_operations——sockfsOps 和 file_operations——socket_fileOps。前者是必须的，它创建了 VFS 必需的 inode，使 VFS 可以对其进行文件级别的管理；而后者是可选的，用户层可以使用标准文件系统的操作比如 write() 和 read() 对 socket 对象进行操作，也可以采用系统提供的 send() 和 recv() 接口对 socket 对象进行处理，但

二者都归一到网络内部实现代码中。

在 ifconfig 的代码中 socket 系统调用的第一个参数即 family 是 PF_INET，意味着 socket_create 中的 net_families[family]->create（sock, protocol）指的是 net_families[PF_INET]里的INET_create函数，它在“网络系统初始化”中提及，而第二个参数 type 被赋给 sock→type 成员，而后根据这个 type 获取INETSW[]数组中相对应的成员。

我们再来看inet_create函数，如代码段3-4所示。

代码段3-4af_inet.cinet_create函数  
1. /\*  
2. \*创建一个inet套接字  
3. \*/  
4. static intINET_create(struct socket\*sock,int protocol)  
5.  
6. struct sock\*sk;  
7. struct list_head\*p;  
8. structINET_protocol\*answer;  
9. structINET SOCK\*inet;  
10. struct proto\*answerprot;  
11. unsigned char answer_flags;  
12. char answer_no_check;  
13. int try_loadingsmodule $= 0$ ：  
14. int err;  
15.  
16. sock->state $=$ SS_UNCONNECTED;  
17.  
18. /\*查找请求类型的<type,protocol>二元组.\*/  
19. answer $\equiv$ NULL;  
20. lookup_protocol:  
21. err $\equiv$ -ESOCKTNOSUPPORT;  
22.  
23. list_for_each_rcu(p，&inetsw[sock->type])（  
24. answer $\equiv$ list_entry(p,structINET_protocol，list);  
25./\*由于UDP\*protocol为IPPROTO_UDP(17)，TCP\*protocol为IPPROTO_TCP(6).\*/if（protocol $\equiv$ answer->protocol）{/\*目前只有只有RAW IP\*protocol为IP（0），如果应用程序传入的值也为IP（0），那么会直接走到36行\*/  
26. if（protocol！=IPPROTO_IP)  
27. break;  
28. }else{/\*在ifconfig应用程序中走这个分支\*/  
29. if（IPPROTO_IP $\equiv$ protocol）（  
30. protocol $\equiv$ answer->protocol;  
31. break;  
32. ）  
/\*在ping这种应用（后面章节会介绍）中跳转这个分支，传入的protocol为ICMP（1），而且，如果使用<SOCK_RAM，OSPF（89）>的组合时也会跳转这个分支。即使用IP层数据传输。\*/  
33. if（IPPROTO_IP $\equiv$ answer->protocol)  
34. break;  
35. ）  
/\*在使用<SOCK_RAM，IP>的组合时会造成

```c
创建 socket 失败。  
36. err = -EPROTONOSUPPORT;  
37. answer = NULL;  
38.  
39. err = -EFERM;  
40. if (answer->capability > 0 && !capabie (answer->capability))  
41. goto out_rcu_unlock;  
/* 在 ifconfig 这个例子中，这个 answer 指向的是 UDP，而其 ops 指向 inet_dgram OPS,  
而 prot 指向 udpprot */  
42. sock->ops = answer->ops;  
43. answerprot = answer->prot;  
44. answer_no_check = answer->no_check;  
45. answer_flags = answer->flags;  
46.  
47.  
err = -ENOBUFS;  
/* 一旦 inode 被创建，sock_alloc 会从这个 inode 中得到 socket 结构，然后初始化这个 socket 结  
构的某些字段，并且 fasync_list 被设置成 NULL。要记住，sockets 并不仅仅是为 TCP/IP 而建，保  
存在 socket 结构中的状态并不与 TCP 的相似，TCP 的会更加复杂，socket 状态实际上只反映了是否  
这是一个活动的连接。*/  
49. sk = sk_alloc (PF_INET, GPP_KERNEL, answer_prot, 1);  
50.  
51. err = 0;  
52. sk->sk_no_check = answer_no_check;  
53. if (INET_PROTOSN_REUSE & answer_flags)  
54. sk->sk_reuse = 1;  
55.  
56. inet = inet_sk (sk);  
57. inet->is_icsk = INET_PROTOSN_ICSK & answer_flags;  
58.  
59. if (SOCK企业提供 IP 设置了一个端口号，我们会在下面看到这个端口号有什么作用，  
此时 num 等于 1 (IPPROTO_ICMP) */  
60. inet->num = protocol;  
61. if (IPPROTO_RAM == protocol)  
62. inet->hdrincl = 1;  
63.  
64.  
65. if (ipv4_config.no_pmtu_disc)  
66. inet->pmtudisc = IP_PMTUDISC_DONT;  
67. else  
68. inet->pmtudisc = IP_PMTUDISC_WANT;  
69.  
70. inet->id = 0;  
/* 为每个打开的 socket 设置其等待队列，我们会在“select 实现”的章节中再次看到它 */  
71. sock_init_data (sock, sk);  
72.  
73. sk->sk-destruct = inet SOCK-destruct;  
74. sk->sk_family = PF_INET;  
75. sk->sk_protocol = protocol;  
76. sk->skbackslog_rcv = sk->sk_prot->backlog_rcv;  
77.  
78. if (inet->num) {  
/* 在此我们见识到 num 和 sport 实际就是同一个值，只是在使用主机字节序的情况下使用 num。  
而在网络字节序的情况下使用 sport */
```

79. /\*我们假设任何协议都允许用户在创建socket的时候指派一个号码\*/   
80. inet->sport $\equiv$ htons (inet->num);   
81. /\*把这个sck加到协议hash链中\*/   
82. sk->sk_prot->hash(sk);   
83. }   
/\*udp_prot→init实际上等于NULU，所以对ifconfig这个应用及所有基于UDP应用程序而言， 下面的判断肯定不会是真，而对于TCP和IP应用则必须执行相应的init函数。   
后面介绍TCP时会再次遇到下面两行\*/ static void raw_v4_hash(struct sock\*sk) 1 struct hlist_head\*head -&raw_v4_htable[inet_sk(sk)->num 88.out; 1）; 99. return err; sk_add_node(sk,head);   
90. out_rcu_unlock:   
91.   
92. goto out;

在这段代码中又发现一个极易引起混乱的结构体名字——sock{}，它和socket{}结构的名字只差了两个字母，它和sock确实有关系。为何？socket{}结构表示INET中的实体，而sock{}结构就是网络层实体，sock用来保存打开的连接的状态信息。它一般定义的变量叫sk。下面将演示它们之间的关系。

INET socket 层支持包括 TCP/IP 在内的 internet 地址族。如前所述，这些协议是分层的，一个协议使用另一个协议的服务。Linux 的 TCP/IP 代码和数据结构反映了这一分层模型。Socket 层从已注册的 net_families[] 数据结构中调用 INET 层 socket 支持例程来为它执行工作。例如，一个地址族为 INET 的 socket 建立请求，将用到下层的 INET socket 的建立函数。为了不把 socket 与 TCP/IP 的特定信息搞混，INET socket 层使用它自己的数据结构 sock，它与 socket 结构相连。这一关系意味着后来的套接字相关系统调用能够很容易地重新找到 sock 结构。sock 结构的协议操作指针也在初始化时建立，它依赖与被请求的协议。如果请求的是 TCP，那么 sock 结构的协议操作指针将指向 TCP 连接所必需的 TCP 协议操作集。

socket 系统调用的第三个参数是 protocol，在这里它终于派上用场——赋给 sk->sk_protocol，不过，在 ifconfig 这个应用中，它没有起到作用。

backlog_rev 在此根据协议分别是 tcp_v4_do_rev, udp_queue_rev_skb 或者是 raw_rev_skb，在 ifconfig 的这个应用中，则是第二个。不过，由于目前只是配置，就不在这里介绍它了，而是放在后面去分析这个函数。

根据对sys_SOCKET的分析，我们可以推断出file{}、socket{}、sock{}之间的关系如图3-5所示。

通过 sock_map_fd 指向一个 file 指针，这个文件指针用来维护与该 socket 相关联的伪文件的状态。也就是说，不管你的应用程序是基于 TCP 的还是基于 IP 甚至是跟网络传输没有关系（例如 ifconfig），内核会在 INET 和 Network 层分别创建一个实体来对应你打开的那个文件描述符，而后你对该文件描述符的操作都是通过这两个数据结构的实体去完成。因为系统为每次 socket 系统调用都创建一对 <file, socket, sock> 三元组，所以每个应用程序打开的每一个文件描述符都不会出现互斥操作，也就避免了锁的开销。每次应用程序要访问内核模块

时，都通过fd去操作，内核会在fd对应的数组单元内查找到相应的socket，然后才能将后继的操作进行下去，比如我们马上要分析的ioctl。

![](images/ab21ec34d387acf302c4f5f15b8873388c2ebe1c487994f41dee276885625351.jpg)  
图3-5 file、socket、sock之间的关系

在inet_create函数中创建sock{}的时候调用sk_alloc接口，它根据协议的类型来创建真正的“sock”结构，因为协议不同，实际创建的对象不同，虽然函数返回值都是sock{}指针，但是实际的大小并不相同，比如，对于raw应用，我们应该创建的是raw_sock，而对于tcp来说，应该创建的是tcp_sock，由于在创建sock{}时就已经根据协议类型为其准备了足够大的空间，所以当进入到协议相关的代码部分，使用指针类型强制转型，将其转换为相应的数据结构。如图3-6所示。

![](images/1e3f1b58e9accdba3e39ca595d91386cf018de5c34aa25a59357e9277c0dcaa0.jpg)  
图3-6 sock结构在不同协议的数据块

这副图演示了各种协议下不同 sock{} 的本质，而表 3-1 列出一些相应的强转类型的宏或函数，第一列是宏，第二列是此宏的参数，第三列是这个宏返回的数据结构类型，请读者记住这些宏，为快速阅读代码打好基础。

表3-1  
sk宏的输入输出比较   

<table><tr><td>宏/函数</td><td>输入结构类型</td><td>输出结构类型</td></tr><tr><td>inet_nt</td><td>sock[]</td><td>inet_nt[结构]</td></tr><tr><td>udp_nt</td><td>sock[]</td><td>udp_nt[结构]</td></tr><tr><td>tcp_nt</td><td>sock[]</td><td>tcp_nt[结构]</td></tr><tr><td>raw_nt</td><td>sock[]</td><td>raw_nt[结构]</td></tr></table>

# 3.1.3 IoT代码的实现

ioctl 是标准库里的函数，也就是说大部分操作系统都会支持这个系统调用。它是内核模块和应用程序交互配置的一种手段，而且，它是同步完成的，即它的代码必定处于某个进程的上下文中。读者们会问，还有什么交互手段不是这样的呢？答案是 netlink 接口。它是异步完成配置的工作的，比如读写路由表。先让我们目光集中在 ioctl 上。我们平时使用的 ifconfig 就是调用 ioctl 接口实现配置功能的。

# 1. 配置设备 IP 地址的内核处理过程

ioctl函数在内核中对应的函数是sys_iocl，这个接口的操作属于VFS的，也就是说系统不区分用户要对哪种文件系统进行ioctl，只有根据用户之前打开的文件类型来推断正确的调用路径。比如socket类型的文件系统，它的ioctl必定是由socket_fileOps结构定义的。内核中具体的执行路径如图3-7所示。

如果要编写关于网络部分的 IoT 应用程序代码，那么专门有一个数据结构来作为 IoT 的参数，对于标准的内核，必须使用如代码段 3-5 所示的数据结构。

代码段3-5 if.h ifreq结构  
```c
Ifreq表示对接口操作的“请求”  
1. struct ifreq  
2. {  
3. #define IFHWADDRLEN6  
4. union  
5. {  
/* 所有接口的Ioctl必须有以ifr_name开始的参数定义 */  
6. char ifrn_name[IFNAMESIZE]; /*接口名，比如"eth0"*/  
7. } ifr_ifrn;  
/*下层函数根据Ioctl的命令字来解析此联合的类型和值 */  
8. union{  
9. struct sockaddr ifru_addr;  
10. struct sockaddr ifru.dstaddr;  
11. struct sockaddr ifru_broadaddr;  
12. struct sockaddr ifru_netmask;  
13. struct sockaddr ifru_hwaddr;  
14. short ifru_flags;  
15. int ifru_value; 
```

```txt
16. int ifru_mtu;   
17. struct ifmap ifru_map;   
10. char ifru(slave[IFNAMSIZ]，/* Just fits the size */   
19. char ifru_newname[IFNAMSIZ];   
20. char __user \* ifru_data;   
21. struct if_settings ifru_settings;   
22. 1   
23. ifr_ifru;   
24. 
```

![](images/dc1606fc0c09bb198b45133dc72c4ca4cf9b8ea06bf56438bcdb510eaf8ef9d6.jpg)  
图3-7 locb的内核实现

这个结构在内核中也有同样的定义，所以在devinet joctl函数就用copy_from_user（&ifr, arg.sizeof(struct ifreq)）这样一条语句搞定，arg是void*变量，实际在内部实现中已经变成了unsigned long变量，也就是指针变量。

代码段3-6是devinetioctl函数的部分实现。

代码段3-6devinet.cdevinet_iocl函数  
```txt
1. int devinet_ioctl(unsigned int cmd, void *arg)  
2. [struct ifreq ifreq; 
```

```c
4. struct sockaddr_in sin_orig;  
5. struct sockaddr_in *sin = (struct sockaddr_in *) ifr.ifr_addr;  
6. struct in_device *in_dev;  
7. struct in_ifaddr **ifap = NULL;  
8. struct in_ifaddr *ifa = NULL;  
9. struct net_device *dev;  
10. char *colon;  
11. int ret = -EFAULT;  
12. int tryaddrmatch = 0;  
13.  
14. if (copy_from_user(&ifr, arg, sizeof(struct ifreq)))  
15. goto out;  
16. ifr.ifr_name[IFNANSI8 - 1] = 0;  
17.  
18. /*把初始的地址信息保存起来，以便比较*/  
19. memcpy(&sin_orig, sin, sizeof(*sin));  
20.  
21. colon = strchr(ifr.ifr_name, ':');  
22. if (colon)  
23. *colon = 0;  
24. ... /*以上省略了GET的操作，要注意*/  
25. ret = -ENODEV;  
/*根据用户指定的设备名参数来搜索设备，比如"lo"或者"etho"*/  
26. if ((dev = _dev_get_by_name(ifr.ifr_name)) == NULL)  
27. goto done;  
/*如果用户指定"etho:0"作为参数，就进入下面的复制*/  
28. if (colon)  
29. *colon = ':';  
/*在第一次对进行设备配置时，下面获取的in_dev是NULL，因为此结构的创建是在后面进行的*/  
30. if ((in_dev = _in_dev_get(dev)) != NULL)  
31. if (tryaddrmatch)  
/*如果in_dev不是NULL，说明曾经对该设备进行过配置，于是遍历in_dev上的ifa链表，根据名字和地址进行匹配，如果一致，说明我们要对同一个设备进行操作，在这里我们获得了ifa。*/  
32. for (ifap = &in_dev->ifa_list; (ifa = *ifap) != NULL;  
33. ifap = &ifa->ifa_next) {  
34. if (!strcmp(ifr.ifr_name, ifa->ifa_label) &&  
35. ain_orig.sin_addr.s_addr =  
36. ifa->ifa_address) {  
37. break; /*found */  
38. }  
39. }  
40. }  
41. /* we didn't get a match, maybe the application is  
42. 4.3BSD-style and passed in junk so we fall back to  
43. comparing just the label */  
44. if (!ifa) {  
45. for (ifap = sin_dev->ifa_list; (ifa = *ifap) != NULL;  
46. ifap = &ifa->ifa_next)  
47. if (!strcmp(ifr.ifr_name, ifa->ifa_label))  
48. break;  
49. }  
50. }  
51.  
52. ret = -EADDRNOTAVAIL; 
```

53.if(!ifa&&cmd $\equiv$ SIOCSIFADDR&&cmd $\equiv$ SIOCSIFFLAGS)   
54. goto done;   
55.   
56. switch (cmd)   
57. case SIOCSIFFLAGS:   
58. if (colon)   
59. ret $=$ -EADDRNOTAVAIL;   
60. if (!ifa)   
61. break;   
62. ret $= 0$ 63. if(!(ifr.lfr_flags&IPP_UP))   
64. inet_del_ifa(in_dev,ifap,1);   
65. break;   
66.   
67. ret $=$ dev_change_flags(dev,ifr.lfr_flags);   
68. break;   
69.   
70. case SIOCSIFADDR:/设置接口地址和地址簇\*/   
71. ret $=$ -EINTRVAL;   
72. if(inet_abc_len(sin->sin_addr.s_addr)<0)   
73. break;   
74.   
75. if(!ifa) {   
76. ret $=$ -ENOBUF3;   
/\*申请一个in_ifaddr()结构，并且初始化为0\*/   
77. if((ifa $=$ inet_alloc_ifa())==NULL)   
78. break;   
\*/如果有冒号，就把用户指定的设备名复制到ifa中，这种情况是指对地址别名的接口进行配置（后  
面会介绍），一般字符长度不能超过15，否则就用设备缺省的名字如“etho”\*/   
79. if(colon)   
80. memcpy(ifa->ifa_label,ifr.ifr_name,IFNAMSIS);   
81. else   
82. memcpy(ifa->ifa_label,dev->name,IFNAMSIS);   
83. }else{   
84. ret $= 0$ /\*根据32行，我们得到了ifa，如果用户输入的地址和已经存在的地址   
一样就跳出switch\*/   
85. if(if(afoifa_local $\equiv$ sin->sin_addr.s_addr)   
86. break;   
/\*如果不一样，我们要删掉老的地址，记住我们要传入的是原本就有的ifa\*/   
87. inlet_del_ifa(in_dev,ifap,0);   
88. ifa->ifa_broadcast = 0;   
89. ifa->ifa_anycast = 0;   
90. }   
91.   
92. ifa->ifa_address $\equiv$ ifa->ifa_local $\equiv$ sin->sin_addr.s_addr;   
93.   
94. if(!(dev->flags&IFF_POINTERPOINT))）{ /\*根据IP地址获取网络掩码长度，其值是0,8,16,24，然后在根据其设置网络掩码\*/   
95. ifa->ifa_prefixlen $\equiv$ inet_abc_len(ifa->ifa_address);   
96. ifa->ifa_mask $\equiv$ inet.make Mask(ifa->ifa_prefixlen);   
97. if((dev->flags&IFF_BROADCAST) &&ifa->ifa_prefixlen<31)   
98. ifa->ifa_broadcast $\equiv$ ifa->ifa_address|~ifa->ifa_mask;   
99. }else{

/\*如果是点到点设备，其掩码是32位，即全1\*/   
100. ifa->ifa_prefixlen $= 32$ ·   
101. ifa->ifa_mask $\equiv$ inat.make_mask(32)；   
102. }   
103. ret $=$ inat.set_ifa(dev, ifa);   
104. break;   
105.   
106....   
107.cane SIOCSIFNETHASK: /\*设置接口的网络掩码，必须要合法，比如要求连续的1\*/   
108. ret $=$ -EINTRVAL;   
109. if (bad_mask (sin->sin_addr.s_addr, 0))   
110. break;   
111. ret $= 0$ ·   
112. if (ifa->ifa_mask != sin->sin_addr.s_addr){   
113. inat_del_ifa(in_dev, ifap, 0);   
114. ifa->ifa_mask = sin->sin_addr.s_addr;   
115. ifa->ifa_prefixlen $\equiv$ inat_mask_len (ifa->ifa_mask);   
116. inat_insert_ifa(ifa);   
117. }   
118. break;   
119.}   
120.done:   
121. $\dots \dots ; / ^ { \text{喜} }$ 几乎什么也不做，就退出了\*/   
122.out:   
123. return ret;   
124.rarok:   
125.   
126. ret $=$ copy_to_user(arg,sifr,sizeof(struct ifreq))？-EFNULT:0;   
127. goto out;   
128.

在上面的 switch 语句 SIOCSIFADDR 分支中我们分配了 in_ifaddr{} 结构，然后把它传入下面这个函数 inset_set_ifa，它的基本内容是申请 in_dev{} 结构，然后把它挂到 ifa 结构中，并把这个 ifa 存入设备的地址链表中，如代码段 3-7 所示。

代码段3-7 devinet.c inlet_set_ifa函数  
```c
1. static int inset_set_ifa(struct net_device *dev, struct in_ifaddr *ifa)  
2. {  
3. struct in_device *in_dev = __in_dev_get(dev);  
4.  
5. if (!in_dev) {  
6. in_dev = inatdev_init(dev);  
7. ... ... ...  
8. }  
9. if (ifa->ifa_dev != in_dev) {  
10. ifa->ifa_dev = in_dev;  
11. }  
/*如果是loopback地址，那么该地址的scope是本机范围，否则还是初始值0，即UNIVERSE.*/  
12. if (LOOPBACK(ifa->ifa_local))  
13. ifa->ifa_scope = RT_SCOPE_HOST;  
14. return inset_insert_ifa(ifa);  
15. } 
```

如果在该设备上没有找到相应的IP地址配置，就创建一个in_device结构，内部实现过程如代码段3-8所示。

代码段3-8 devinet.c inetdev_init函数  
```c
1. struct in_device *inetdev_init(struct net_device *dev)  
2. {  
3. struct in_device *in_dev;  
4.  
5. in_dev = kmalloc(sizeof(*in_dev), GFP_KERNEL);  
6. ...  
7.memset(in_dev, 0, sizeof(*in_dev));  
8.  
9. memcpy(&in_dev->cnf, &ipv4_devconf_dflt, sizeof(in_dev->cnf));  
10. in_dev->cnf.sysctl = NULL;  
11. in_dev->dev = dev;  
/*创建一个邻居参数，而且挂到arp Tina上，我们会放到ARP的一章中介绍*/  
12. if ((in_dev->arpisable = noighisable_alloc(dev, &arp Tina)) == NULL)  
13. goto out_kfree;  
14.INET_DEV_count++;  
15.  
/*将ip_ptr指向in_dev*/  
16. dev->ip_ptr = in_dev;  
17.  
18. out:  
19. return in_dev;  
20. out_kfree;  
21. kfree(in_dev);  
22. in_dev = NULL;  
23. goto out;  
24. | 
```

经过这么一番配置，in_ifaddr{}，net_device{}和in_device{}的关系如图3-8所示，而且要记住，in_ifaddr{}这个结构里的成员不会被其他模块改变了。

![](images/c1d8bdd814496324efef375ae5132c19f870f0d98b8ef5ae73e366a06f4ac769.jpg)  
图3-8inet_set_ifa之后数据结构之间的关系

经过上面的分析，代码的大致流程如图3-9所示。

![](images/768a73172fdb6df1bcda51fc98bb0845efdbd9a99c83c66e93a4a9e8e9c70c0f.jpg)  
图3-9devinet_iocll函数调用树

首先调用 rtmsg_ifa 发送消息给应用层，以至于 netlink 的侦听函数会知道这个新地址，然后调用通知链，这会触发对地址分配 IP 事件感兴趣的等候者行动。

对ioctl分析也快到头了，但好像还没看到跟路由有什么关系，难道线索断了？但是我们忘了，blocking_notify_call_chain 扫描的是inetaddr_chain，顾名思义，这是一条链呀，对了，还记得在FIB初始化的时候，它曾经也挂到了这个链上吗？也就是说设置IP地址会引起一次事件，这个事件会影响到另一个很重要的模块——FIB，即路由系统，如图3-10所示。我们会在后面慢慢介入路由系统。

![](images/719aeae7aba25cc7e150bf6c351c2c244d33ef6ebb1199c6196a74519d2cefce.jpg)  
图3-10 inlet_set_ifa发送NETDEV_UP事件

# 2. netlink和rtnetlink接口

虽然我们还没有说到路由系统，但是为了解释刚才看见的 netlink_broadcast 函数，没办法。通常，在 Linux 里当一个应用程序想要增加或删除一个路由时，它会用 nlmsghdr+rtmsg 结构并传给 rtnetlink socket。

rtentry 结构是 UNIX 系统提供给用户接口获取路由的传统途径，实际上 Linux 内部并不使用这个结构，它和 BSD 操作系统用作路由 ioctl 的 rtentry 相似，只是用来方便 Linux 和其

他 UNIX 变种的移植。一个指向 rtentry 的指针作为参数传到 ioctl 系统调用，然后通过 fibConvert_rtentry (int cmd, struct nlmsghdr *nl, struct rtmsg *rtm, struct Kern_rta *rta, struct rtentry *r) 函数把这个结构的成员分拆到 nlmsghdr {}, rtmmsg {}, Kern_rta{} 结构中，如图 3-11 所示。

![](images/478cdc01c5a70b3290b6d93efd1e1b11124a4ed2881ebb78bf9854113053251a.jpg)  
图3-11 nentry被拆分成3个结构

访问 FIB 的主要方法是通过 netlink socket, 它为路由提供了扩展——rtnetlink. netlink 是一个内部的通信协议。它主要用来在应用层和 Linux 内核里大量协议之间传递信息。Netlink 实现了自己的地址族——AF_NETLINK, 它支持大多数的 socket API 函数。netlink 最常用的场合是应用程序预内核内部的路由表交换路由信息。其使用方法是先打开一个 socket: fd = socket (PF_NETLINK, socket_type, netlink_family); socket_type 为 SOCKRaw 或者 SOCK_DGRAM 都可以, 因为 netlink 本身是基于数据报的。比较常用 netlink_family 有下面几种:

- NETLINKROUTE。用来修改和读取路由表的，这是我们后面要讨论的rtnetlink问题。  
- NETLINK ARPD，在用户空间中管理ARP表。  
- NETLINK_USERSOCK。给应用程序（不一定都是协议）发送的消息。

打开了 socket，那么可以用 sendmsg 和 recvmsg 给内核或同台主机应用程序（而不是发给远端主机）发消息。这些调用传递一个指向 nlmsghdr 结构的指针（见图 3-11）。

rtnetlink 是基本 netlink 协议的消息扩展。也就是当创建 PF_NETLINK 的 socket 时，把 netlink_family 设置为 NETLINKROUTE。内核创建了一个轻量级的 socket，专门用于接收来自用户 netlink 接口发送过来的消息，也接收内核部分的消息。图 3-12 演示了 rtnetlink 的初始化部分的函数调用树。

![](images/322638718818b08103bbf200f411d19b26a9c48b70b89cc74903748e81774ae6.jpg)  
图3-12 rntnetlink_init函数调用树

内核模块使用 netlink_broadcast 给 rtnetlink 发送消息，由 rtnetlink_rcv 函数接收并处理。

下面继续解说inet_insert_ifa函数，它里面利用了一个C语言技巧，首先它创建一个skb，然后进入一个判断语句，首先判断inet_fill_ifaddr函数执行的结果，在大部分情况下，必定会进入到最后一个else语句，执行netlink_broadcast，这种技巧在内核代码中比较常见，而且通常能造成读者迷惑，认为最后一句else的情况不会到达。这种技巧的目的在于遍历各种情况，而且不影响代码的“从上到下”顺序，如代码段3-9所示。

代码段3-9 devinet.c rtmsg_ifa函数  
```c
1. static void rtmsg_ifa(int event, struct in_ifaddr* ifa)  
2. {  
3. int size = NLMSG_SPACE (sizeof(struct inaddrmsg) + 128);  
4. struct skBuff *skb = alloc_skb(size, GFP_KERNEL);  
5.  
6. if (inet_fill_ifaddr(skb, ifa, current->pid, 0, event, 0) < 0) {  
    ....../错误处理*/  
7. } else {  
    /*下面这个函数会通知阻塞在某个netlink下的回调函数，然后让它们进行处理。不过要注意，rtnl是一个sock()结构的全局变量*/  
8. netlink_bROADCAST(rtnl, skb, 0, RTNLGRP_IPV4_IFADDR, GFP_KERNEL);  
9. }  
10. }
```

当rtnetlink_rcv接收到这样一个消息，它会根据参数调用对应的函数表。在这里它会调用inet_rtm_newaddr函数。inet_rtm_newroute增加一个新的路由到FIB。inet_rtm_delroute从FIB中删除一条路由。这两个函数从紧跟nlmsghdr后面的内存中抽取rtmsg结构，rtmsg的结构如代码段3-10所示。

代码段3-10 rtnetlink.h rtmsg结构  
```c
struct rtmsg   
{ uchar rtm_family; /\*下面两个是，用来给AF_INET类型的地址创建32位或更小的网络掩码bit的位数\*/ uchar rtm_dst_len; uchar rtm_src_len; uchar rtm_tos; \*/对应IP头部的ToS字段\*/ uchar rtm_table;/包含路由表ID，如果没有配置多个表， 那么就是RT_TABLE_MAIN或RT_TABLE_LOCAL.\*/ uchar rtm_protocol;/指的是表格3-2的列出的值\*/ uchar rtm_scope; uchar rtm_type;/\*路由类型\*/ uint rtm_flags;/可以是表格3-3列出的值\*/
```

表 3-2 rmsg→protocol 的值  

<table><tr><td>协议</td><td>值</td><td>目的</td></tr><tr><td>RTPROT_UNSPEC</td><td>0</td><td>没有指定的值</td></tr><tr><td>RTPROT_REDIRECT</td><td>1</td><td>这条路由是由ICMP重定向消息产生的，IPv4当前没有使用</td></tr><tr><td>RTPROT_KERNEL</td><td>2</td><td>这条路由是内核产生的</td></tr><tr><td>RTPROT_BOOT</td><td>3</td><td>这条路由是在boot的过程中产生的</td></tr><tr><td>RTPROTSTATIC</td><td>4</td><td>这跳路由是管理员静态设置的</td></tr><tr><td>RTPROT_GATED</td><td>8</td><td>这是由网关路由守护进程使用</td></tr><tr><td>RTPROT_ZEBRA</td><td>11</td><td>由Zerba路由守护进程使用（这个Zerba比较有名，从事路由器方面工作的读者不可能不知道）</td></tr></table>

表3-3  
rtmsg→rtm_flags的值   

<table><tr><td>宏</td><td>值</td><td>意义</td></tr><tr><td>RTM_F_NOTIFY</td><td>0x100</td><td>通知用户路由改变</td></tr><tr><td>RTM_F_CONDED</td><td>0x200</td><td>指示路由被clone了</td></tr><tr><td>RTM_F_EQUALIZE</td><td>0x400</td><td>还没有被实现</td></tr><tr><td>RTM_F_PREFIX</td><td>0x800</td><td>前缀地址</td></tr></table>

Rtmsg 结构的协议字段的值如果大于 RTPROT_STATIC，那么内核不会改变它，只是在用户和内核空间传递。它们故意留着给假想的几个路由守护进程使用。代码里的注释推荐这些值应该标准化以避免冲突。

# 3.1.4 loopback 接口的配置过程

前面一节介绍了给本机系统配置IP地址的过程，这一节给大家介绍一下loopback接口的“配置”过程，之所以用引号，是因为此配置不完全是用户自己控制的，为什么不先介绍loopback的配置，原因也在此。上一节已经对配置的过程一步一步做了分解，现在可以了解loopback接口的初始化及配置过程，这也是对普通设备的初始化和配置过程的一个回顾。

要使loopback接口起作用得分为2个步骤。

第一步是在系统初始化的时候就创建一个in_dev{}结构给loopback接口，参照上一节，对于用户配置普通设备的IP地址，这个动作应该是在ioct1→……→inet_set_ifa函数中完成。这是两种不同设备的配置上的区别：loopback设备是自动创建，而普通设备是“用户”手动创建。

而其这种自动还转了一道手，它实际是在ip_netdev_notify收到NETDEV_REGISTER事件后才去调用inetdev_init创建了in_dev{}结构，此函数请参考前一节。而普通设备则是在ocictl的上下文中调用inet_set_ifa函数创建的。

第二步就是当系统初始化结束之后根据系统配置打开 loopback 接口，使之 UP。

如同普通设备的操作，系统必须调用dev_open打开lookback接口，从而发送NETDEV_UP事件给ip_netdev.notifier。这个notifier对于普通设备的事件还是不太感兴趣，啥也不做；但是非常关心loopback接口的UP事件，它创建了in_ifaddr{}结构。这也是loopback接口配置自动化的证明。如图3-13所示。

![](images/98e760e082de19fda0b6a982b480e21d33c86fcc5b810774c7dbf725a95a7235.jpg)  
图3-13 probe发起NET_DEV_REGISTER事件

ip_netdev_NOTIFY的事件处理函数是inetdev_event，其相关代码如代码段3-11所示。

代码段3-11 devinet.c loopback设备对NETDEV_UP事件的处理  
```c
1. inetdev_event
2. }
3. ...
4. case NETDEV_UP:
5. if (dev == loopback_dev) {
6. struct in_ifaddr *ifa;
7. if ((ifa = inet_alloc_ifa()) != NULL) {
8. 这里面每个赋值都很重要，直接决定loopback的性质
9. INADDR_LOCKBACK宏就是0x7f000001 /* 127.0.0.1 */
10. ifa->ifa_local = ifa->ifa_address = htons(INADDR_LOCKBACK);
11. ifa->ifa_prefixlen = 8;
12. ifa->ifa_mask = inot.make_mask(8);
13. ifa->ifa_dev = in_dev;
14. 该地址是属于本机范围的，不属于外部地址，我们会在后面说到这个值
15. ifa->ifa_scope = RT Scorpe_HOST;
16. memcpy(ifa->ifa_label, dev->name, IFNAMESIZE);
17. inet_insert_ifa(ifa);
18. ...
19. } 
```

值得关注的是这段代码又调用了 inset_insert_ifa 函数，它发送 NETDEV_UP 事件给 fib_inetaddr_NOTIFY。如图 3-14 所示。

loopback 设备的配置是否可以改变？可以的！让我们到 Linux 的/etc/sysconfig/ network 目录下（目前是 Redhat，其他版本的 Linux 不一定是此目录，但都大同小异），看到有 ifcfg-lo 这么一个文件，内容如代码段 3-12 所示。

![](images/67343fa961cc9e6b5c909a08f61e29c3fc03e0f3778ac3957bffd47ed7b627c2.jpg)  
图3-14dev_open发起NETDEV_UP事件

代码段3-12 ifcfg-lo文件里的内容  
```txt
DEVICE=1o  
IPADDR=127.0.0.1 #可以改变这个IP地址  
NETMASK=255.0.0.0  
NETWORK=127.0.0.0  
……  
BROADCAST=127.255.255.255  
NAME=loopback
```

可以改动这个文件里的某些项，然后执行#ifup lo 就把里面的内容作为ioctl的参数传给了内核，把缺省的配置改成你想要的。

从图3-14中看到fib_netdev_notifyer收到NETDEV_UP事件（确切的来说此时只有loopback接口），于搜索in_dev $\rightarrow$ fa_list然后循环调用fib_add_ifaddr，实际在缺省情况下在系统启动之后，只有loopback接口才有in_ifaddr{}结构，所以这个动作对于普通设备没有意义。我们将在下面两节介绍fib_add_ifaddr函数，这个函数非常重要，它是打开路由系统的钥匙。

# 3.1.5 IP别名的实现

上一节说的是用 ifconfig 命令来进行接口的地址配置，这个命令已经有点过时，更先进的配置工具是 ip 命令。在本节中我们将介绍一个叫做“IP 别名”的概念。为了能处理 IP 别名，所以要引入 ip 命令。

![](images/62370f683c036dc12d93e4cbe988b08a71fa2f0ac50b196df61dd58b068558ec.jpg)

ip是iproute2软件包里面的一个强大的网络配置工具，Iproute2是一个在Linux下的高级网络管理工具软件。实际上，它是通过rtnetlink sockets方式动态配置内核的一些小工具组成的，从Linux2.2内核开始，就实现了通过rtnetlink sockets用来配置网络协议栈，它是一个现代的强大的接口。最吸引人的特色就是它用完整而有机制的简单命令替代了之前以下命令的功能，如ifconfig, arp, route, iptunnel，而且还添加了其他不少的功能。如今，Iproute2已经在很多主要的发行版里被默认安装。它在配置隧道的时候非常有用。当然本书不打算讨论它们。我们要讨论的IP别名，通过iproute2这个工具可以对它进行设置。

前面已经零零散散的说到了 netlink 技术，现在我们又要介绍一个和它相关的工具——ip 命令。在剩下的章节中我们会常看到 ip 命令的使用。

IP 别名有时候也称为网络接口别名（network interface aliasing）或逻辑接口（logical interface）。IP 别名的概念是：可以在一个网络接口上配置多个 IP 地址。这样就能够在使用单一接口的同一个主机上进行负载平衡以支持多种服务。比如你的主机上只有少量网卡接口，而又要求进行多路接入的扩展，可以按如图 3-15 所示扩展。

![](images/fe8f8f216a2ac095aaf1a2d91469a2a5bd6034a704abb90cbd078c657eca5557.jpg)  
图3-15 