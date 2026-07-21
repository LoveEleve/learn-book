![](images/d5f85c78333c40156006589cabcd49e69a1e2f79f987f1332392c3376fec9d4d.jpg)

# Linux开源网络全栈详解

从DPDK到OpenFlow

英特尔亚太研发有限公司 编著

# Linux开源网络全栈详解

从DPDK到OpenFlow

本书基于Linux基金会将开源网络技术划分的层次框架，对处于主导地位的、较为流行的开源网络项目进行阐述，主要介绍各个项目的起源发展及背后故事、实现原理与框架、要解决的网络问题，力争用轻松的语言对开源网络进行多方位、深层次的揭秘：

$\bullet$ 开源网络组织及生态  
- OpenFlow   
Linux虚拟网络  
$\bullet$ 高性能数据平面  
- OpenDaylight   
- OpenStack网络  
Kubernetes网络  
Service Mesh   
网络编排与集成

![](images/af1e46fe5efad35e8d82d7cc6f74bb1d697f458cb9ffff202505a19a8827d5e6.jpg)

![](images/180e41f3b424a63bba61d8613195dc050817d93a12b84fe594a369392baa9062.jpg)

博文视点Broadview

![](images/0b9ed0ab4ea69b2e763872dfe3c9d5bee77c96fffb2cfc59896bd181d0c7ddc7.jpg)

![](images/300d9dd6860794b7cc90698675ac754a3f5e15cbb94c7d13942884fdb4e60cb8.jpg)

新浪微博

@博文视点Broadview

![](images/6decd035f902023c080a0d665966ec66a334381b6a3ba28b5c0afbcf999242ed.jpg)  
上架建议：Linux系统

定价：79.00元

![](images/aa359a518dfbfa927ea19b837230ddb100d851d5e8f3ee5fe50e409255003585.jpg)

# Linux开源网络全栈详解

从DPDK到OpenFlow

英特尔亚太研发有限公司 编著

# 内容简介

本书基于Linux基金会划分的开源网络技术层次框架，对处于主导地位的、较为流行的开源网络项目进行阐述，包括DPDK、OpenDaylight、Tungsten Fabric、OpenStack Neutron、容器网络、ONAP、OPNFV等。本书内容主要围绕各个项目的起源与发展、实现原理与框架、要解决的网络问题等方面展开讨论，致力于帮助读者对Linux开源网络技术的实现与发展形成完整、清晰的认识。本书语言通俗易懂，能够带领读者快速走入Linux开源网络的世界并做出自己的贡献。

本书适合参与Linux开源网络项目开发的读者阅读，也适合互联网应用的开发者、架构师和创业者参考。

未经许可，不得以任何方式复制或抄袭本书之部分或全部内容。

版权所有，侵权必究。

# 图书在版编目（CIP）数据

Linux开源网络全栈详解：从DPDK到OpenFlow/英特尔亚太研发有限公司编著.—北京：电子工业出版社，2019.7

ISBN 978-7-121-36786-1

I. $① \mathrm { L } \cdots$ II. $①$ 英…Ⅲ. $①$ Linux操作系统IV. $①$ TP316.85

中国版本图书馆CIP数据核字（2019）第108126号

责任编辑：孙学瑛

文字编辑：宋亚东

印刷：三河市良远印务有限公司

装 订：三河市良远印务有限公司

出版发行：电子工业出版社

北京市海淀区万寿路173信箱 邮编：100036

开本： $720\times 1000$ 1/16 印张：16.75 字数：429千字

版次：2019年7月第1版

印次：2019年7月第1次印刷

定价：79.00元

凡所购买电子工业出版社图书有缺损问题，请向购买书店调换。若书店售缺，请与本社发行部联系，联系及邮购电话：（010）88254888，88258888。

质量投诉请发邮件至zlts@phei.com.cn，盗版侵权举报请发邮件至dbqq@phei.com.cn。

本书咨询联系方式：010-51260888-819，faq@phei.com.cn。

# 推荐序一

Network functions are rapidly transforming from being delivered on proprietary, purpose-built hardware to capabilities running on intelligent and composable infrastructure. We are transforming the network from a statically configured and inflexible offering, to a network which can be provisioned to specific end users, specific verticals and specific needs on a standard server-based infrastructure. Greater customer value is being derived from this flexibility and programmability.

The foundation of the 5G network requires an intelligent infrastructure built on NFV and SDN-based architecture that takes advantage of server volume economics, virtualization and cloud technologies. This enables new services to be deployed more quickly and cost effectively. Intel has a growing portfolio of products and technologies that deliver solutions to help network transformation, bringing advanced performance and intelligence from the network edge to the core of the data center.

Open source software is one key part of the portfolio, which unlocks the platform capabilities of the network for packet processing. Intel invented the Data Plane Development Kit (DPDK), later co-founded as an open source project, and currently leads its growth with the community, helping make DPDK a de-facto standard for packet processing. In addition to DPDK, Intel has significantly contributed to other important open source projects, including Open Virtual Switch

(OVS), FD.io, Vector Packet Processing (VPP) for network stack, Tungsten Fabric (TF) for virtual router, and HYPERSCAN for pattern matching.

We have a skilled and committed team in China who have contributed to these open source projects over the years, and continue to collaborate with our partners in these communities to solve challenging network problems. As a good linkage with the Chinese ecosystem, it is with great pride that the team presents this book as a resource to help contributors who want to get involved, influence communities, and drive continued innovation.

Sandra Rivera

Senior Vice President and General Manager

Network Platforms Group

Intel 5G Executive Sponsor

# 推荐序二

The rise of Cloud and Edge computing has brought about a shift in networking technology. Increasingly, purpose-built physical systems are being replaced with flexible, adaptable solutions built on open source technology. Open software is driving the innovation that powers this evolution, with Software-Defined Networking (SDN) and network function virtualization paving the way for tremendous growth in connected services.

Intel is a strong contributor to open source software across technologies and market segments. Intel has been a leader in advancing the open networking ecosystem, contributing to projects such as the Data Plane Development Kit (DPDK) for data plane acceleration; Open Virtual Switch (OVS) and Vector Packet Processing (VPP) for virtual switch; OpenStack Neutron, OpenDaylight (ODL) and Tungsten Fabric (TF) for SDN; the Open Networking Automation Platform (ONAP) for orchestration and automation; and Open Platform Network Function Virtualization (OPNFV).

This book draws from the deep experience of Intel software engineers who work in open source networking communities and discusses how these projects fit as part of a complete stack for cloud infrastructure. We are proud to offer this resource to help contributors who want to get involved, influence communities, and drive continued innovation.

Imad Sousou

Corporate Vice President, Intel Corporation

General Manager, Intel System Software Products

# 前言

自1991年Linux诞生，时间已经走过了接近三个十年。即将而立之年的Linux早已没有了初生时的稚气，正在各个领域展示自己成熟的魅力。

以Linux为基础，也衍生出了各种开源生态，比如网络，比如存储。而生态离不开形形色色的开源项目，在人人谈开源的今天，一个又一个知名的开源项目正在全球野蛮生长。当然，本书的主题仅限于Linux开源网络生态，面对其中一个又一个扑面而来而又快速更迭的新项目新名词，我们会有一定的紧迫感想去了解这些他们背后的故事，也会有一定的动力去踏上Linux开源网络世界之旅。面对这样的一段旅途，我们心底浮现的最为愉悦的开场白或许应该是“说实话，我学习的热情从来都没有低落过。Just for Fun.”，正如Linus在自己的自传《Just for Fun》中所希望的那样。

面对Linux开源网络这么一个庞大而又杂乱的世界，让人最为惴惴不安的问题或许是：我该如何更快更好的适应这个全新的世界？人工智能与机器学习领域里研究的一个很重要的问题是“为什么我们小时候有人牵一匹马告诉我们那是马，于是之后我们看到其他的马就知道那是马了？”针对这个问题的一个结论是：我们头脑里形成了一个生物关系的拓扑，我们所认知的各种生物都会放进这个拓扑的结构里，而我们随着年纪不断成长的过程就是形成并完善各种各样或树形或环形等拓扑的过程，并以此来认知我们所面对的各种新事物。

由此可见，或许我们认知Linux开源网络世界最快也最为自然的方式就是努力在

脑海里形成它的拓扑，并不断的进行细化。比如这个生态里包括了什么样的层次，每个层次里又有什么样的项目去实现，各个项目又实现了哪些服务以及功能，这些功能又是以什么样的方式实现的，等等，对于我们感兴趣的项目又可以更为细致的去勾勒它其中的脉络。就好似我们头脑里形成的有关一个城市的地图，它有哪些区，区里又有哪些标志建筑以及街道，对于我们熟悉的地方可以将它的周围进行放大细化，甚至于一个微不足道的角落。

# 本书的组织形式

本书的内容组织正是为了尽一切能力帮助读者能够形成有关Linux开源网络世界比较细致的拓扑。首先是前两章，对Linux开源网络的生态以及Linux本身对网络的支持与实现进行了阐述，希望能够帮助读者对Linux开源网络有个全面的基本的认识和了解。

第1章主要基于Linux基金会划分的开源网络技术层次框架，对Linux开源网络生态进行整体的介绍。此外，也介绍了网络有关的开源组织与标准架构。

第2章详尽的介绍了Linux虚拟网络的实现，包括Linux环境下一些网络设备的虚拟化形式，以及组建虚拟化网络时涉及到的主要技术，为更深一步对Linux开源网络生态下的开源项目展开讨论打下基础。

然后第3～7章的内容对Linux开源网络生态各个层次中处于主导地位的、较为流行的项目进行介绍。按照认识的发展规律，通过前面两章的介绍我们已经对Linux开源网络世界有了全局的认识和了解，接下来就可以以兴趣或工作需要为导向，选择一个项目进行深入的钻研和分析。这些章节的内容也是希望能够尽量帮助读者形成对相应项目的比较细致的拓扑，并不求对所有实现细节的详尽分析。

网络数据平面的性能开销复杂多样且彼此关联，第3章即对相关的优化技术与项目进行讨论，包括DPDK、OVS-DPDK、FD.IO等。

第4章讨论网络的控制面，并介绍主要开源SDN（软件定义网络）控制器，包括OpenDaylight与TungstenFabric等。

第5章与第6章分别对OpenStack与Kubernetes两种主要云平台中的网络支持进

行讨论。没有网络，任何虚拟机或者容器都将只是这个虚拟世界中的孤岛，不知道自己生存的价值。

第7章讨论网络世界中的大脑——编排器。内容主要涵盖两种开源的编排器，包括ONAP与OPNFV。

# 感谢

作为英特尔的开源技术中心，参与各个Linux开源网络项目的开发与推广是再为自然不过的事情。除了为各个开源项目的完善与稳定贡献更多的思考和代码，我们也希望能通过这本书让更多的人更快捷的融入Linux开源网络世界的大家庭。

如果没有 Sandra Rivera（英特尔高级副总裁兼网络平台事业部总经理）、Imad Sousou（英特尔公司副总裁兼系统软件产品部总经理）、Mark Skarpness（英特尔系统软件产品部副总裁兼数据中心系统软件总经理）、Timmy Labatte（网络平台事业部副总裁兼软件工程总经理）、练丽萍（英特尔系统软件产品部网络与存储研发总监）、冯晓焰（英特尔系统软件产品部安卓系统工程研发总监）、周林（网络平台事业部中国区软件开发总监）、梁冰（英特尔系统软件产品部市场总监）、王庆（英特尔系统软件产品部网络与存储研发经理）的支持，这本书不可能完成，谨在此感谢他们的关怀与帮助。

也要感谢本书的编辑孙学瑛老师与宋亚东老师，从选题到最后的定稿，整个过程中，都给予我们无私的帮助和指导。

然后要感谢参与各章内容编写的各位同事，他们是郭瑞景、陆连浩、秦凯伦、徐琛杰、应若愚、丁亮、朱礼波、黄海滨、任桥伟、梁存铭、胡雪煜、胡嘉瑜、王潇、何少鹏、姚磊、倪红军、吴菁菁、陈兆彦。为了本书的顺利完成，他们付出了很多努力。

最后感谢所有对Linux开源网络技术抱有兴趣或从事各个Linux开源网络项目工作的人，没有你们的源码与大量技术资料，本书便会成为无源之水。

作者

# 目录

# 第1章 Linux开源网络

1.1 开源网络组织

1.1.1 云计算与三大基金会  
1.1.2 LFN 3

1.2 网络标准及架构 4

1.2.1 OpenFlow 4   
1.2.2 SDN 10   
1.2.3 P4 14   
1.2.4 ETSI的NFV参考架构 17

1.3 Linux 开源网络生态 19

1.3.1 开源硬件 20  
1.3.2 虚拟交换 21   
1.3.3 Linux操作系统 22  
1.3.4 网络控制 23  
1.3.5 云平台 24  
1.3.6 网络编排 27  
1.3.7 网络数据分析 27  
1.3.8 网络集成 28

# 第2章 Linux虚拟网络 29

2.1 TAP/TUN 设备 30  
2.2 Linux Bridge 32   
2.3 MACVTAP 33   
2.4 Open vSwitch 35   
2.5 Linux Network Namespace 37   
2.6 iptables/NAT 42   
2.7 虚拟网络隔离技术 45

2.7.1 虚拟局域网（VLAN） 45  
2.7.2 虚拟局域网扩展（VxLAN） 47  
2.7.3 通用路由封装GRE 49   
2.7.4 通用网络虚拟化封装（Geneve） 50

# 第3章 高性能数据平面 52

3.1 高性能数据面基础 54

3.1.1 内核旁路 54  
3.1.2 平台增强 59  
3.1.3 DPDK 65

3.2 NFV和NFC基础设施 72

3.2.1 网络功能虚拟化 72  
3.2.2 从虚拟机到容器的网络I/O虚拟化 78  
3.2.3 NFVi平台设备抽象 81

3.3 OVS-DPDK 86

3.3.1 OVS-DPDK 概述 86  
3.3.2 OVS-DPDK性能优化 93

3.4 FD.IO：用于报文处理的用户面网络协议栈 98

3.4.1 VPP 98   
3.4.2 FD.IO子项目 101  
3.4.3 与OpenDaylight和OpenStack集成 ..... 107  
3.4.4 vBRAS 109

# 第4章 网络控制 112

# 4.1 OpenDaylight 114

4.1.1 ODL社区 114  
4.1.2 ODL体系结构 115  
4.1.3 YANG 120   
4.1.4 ODL子项目 122  
4.1.5 ODL应用实例 125

# 4.2 Tungsten Fabric 126

4.2.1 Tungsten Fabric 体系结构 126  
4.2.2 Tungsten Fabric 转发平面 134  
4.2.3 Tungsten Fabric 实践 138   
4.2.4 Tungsten Fabric 应用实例 145   
4.2.5 Tungsten Fabric 与 OpenStack 集成 ..... 146

# 第5章 OpenStack网络 147

5.1 OpenStack网络演进 150  
5.2 Neutron体系结构 152

5.2.1 网络资源模型 152  
5.2.2 网络实现模型 159  
5.2.3 Neutron软件架构 164

5.3 Neutron Plugin 165

5.3.1 ML2 Plugin 165   
5.3.2 Service Plugin 170

5.4 Neutron Agent 174

# 第6章 容器网络 177

6.1 容器 177

6.1.1 容器技术框架 180  
6.1.2Docker 184   
6.1.3 Kubernetes 188

6.2 Kubernetes网络 196

6.2.1 Pod 内部的容器间通信 ..... 196  
6.2.2 Pod间通信 197  
6.2.3 Pod与Service之间的网络通信 199  
6.2.4 Kubernetes 外界与 Service 之间的网络通信 ..... 202

6.3 Kubernetes CNI 202   
6.4 Service Mesh 209

6.4.1 Sidecar模式 211  
6.4.2 开源ServiceMesh方案 213

6.5 OpenStack 容器网络项目 Kuryr 217

6.5.1 Kuryr起源 217  
6.5.2 Kuryr 架构 ..... 217

# 第7章 网络编排与集成 221

7.1 ETSI NFV MANO 221

7.1.1 ETSI标准化进展 221  
7.1.2 OASISTOSCA 223   
7.1.3 开源编排器 224

7.2 ONAP 228

7.2.1 ONAP基本框架 230  
7.2.2 ONAP应用场景 234

7.3 OPNFV 237

7.3.1 OPNFV上游 238   
7.3.2 OPNFV项目 245  
7.3.3 OPNFVCI 251   
7.3.4 OPNFV典型用例 252

# 第1章

# Linux开源网络

在人人谈开源的今天，看着一个又一个知名的开源项目在全球快速发展，开发者会非常想去了解这些开源项目。囿于本书的主题，我们只会努力去对Linux开源网络道出个一二三来。

# 1.1 开源网络组织

# 1.1.1 云计算与三大基金会

在形形色色的开源组织里，有三个巨无霸的角色，就是Linux基金会、OpenStack基金会和Apache基金会。而三大基金会又与盛极一时的云计算有着千丝万缕的关系。

整体而言，云计算的开源体系可以分为硬件、容器/虚拟化与虚拟化管理、跨容器和资源调度的管理和应用。在这几个领域里，Linux基金会关注硬件、容器及资源调度管理，在虚拟化层面，也有KVM和Xen等为人熟知的项目。在容器方面，Linux基金会和Docker联合发起了OCI（Open Container Initiative）；在跨容器和资源调度管理上，Linux基金会和Kubernetes发起了CNCF（Cloud Native Computing Foundation）。相比之下，OpenStack基金会更为聚焦，专注于虚拟化管理。

# （1）Linux基金会

Linux基金会的核心目标是推动Linux的发展。我们耳熟能详的Xen、KVM、CNCF

等，都来自Linux基金会。

Linux基金会采用的是会员制，分为银级、金级、白金级三个等级，白金级是最高等级。Linux基金会的会员数量不胜枚举，不过由于白金级高达50万美元的年费门槛，白金级会员却是一份短名单，仅包括思科、富士通、惠普、华为、IBM、Intel、NEC、甲骨文、高通、三星和微软等知名企业。

值得一提的是，作为白金级会员的华为，在Linux基金会成功建立了一个项目——OpenSDS，这是首个由我国主导的Linux基金会项目。OpenSDS旨在为不同的云、容器、虚拟化等环境创建一个通用开放的SDS（Software Defined Storage）解决方案，提供灵活的按需供给的数据存储服务。

另外，2018年3月，由英特尔开源技术中心中国团队主导的车载虚拟化项目ACRN也被Linux基金会接受并发布。ACRN是一个专为物联网和嵌入式设备设计的管理程序，目标是创建一个灵活小巧的虚拟机管理系统。通过基于Linux的服务操作系统，ACRN可以同时运行多个客户操作系统，如Android、Linux其他发行版或RTOS，使其成为许多场景的理想选择。

# （2）OpenStack基金会

近些年，在开源的世界，OpenStack 应该是最为红火的面孔之一。OpenStack 基金会就是围绕 OpenStack 项目发展而来的。2012 年 9 月，在 OpenStack 发行了第 6 个版本 Folsom 的时候，非营利组织 OpenStack 基金会成立。OpenStack 基金会最初拥有 24 名成员，共获得了 1000 万美元的赞助基金，由 RackSpace 的 Jonathan Bryce 担任常务董事。OpenStack 社区决定 OpenStack 项目从此以后都由 OpenStack 基金会管理。

OpenStack 基金会的职责为推进 OpenStack 的开发、发布以及能作为云操作系统被采纳，并服务于来自全球的所有 28000 名个人会员。

OpenStack 基金会的目标是为 OpenStack 开发者、用户和整个生态系统提供服务，并通过资源共享，推进 OpenStack 公有云和私有云的发展，辅助技术提供商在 OpenStack 中集成新兴技术，帮助开发者开发出更好的云计算软件。

OpenStack 基金会在成立之初就设立了专门的技术委员会，用来指导 OpenStack 技术相关的工作。对于技术问题讨论、某项技术决策和未来技术展望，技术委员会负

责提供指导性建议和意见。除此之外，技术委员会还要确保OpenStack项目的公开性、透明性、普遍性、融合性和高质量。

一般情况下，OpenStack 技术委员会由 13 位成员组成，他们完全是由 OpenStack 社区中有过代码贡献的开发者投票选举出来的，通常任职 6 个月后需要重选。有趣的是，其中的 6 位成员是在每年秋天选举产生的，另外 7 位是在每年春季选举产生的，通过时间错开保持了该委员会成员的稳定性和延续性。技术委员会成员候选人的唯一条件是，该候选人必须是 OpenStack 基金会的个人成员，除此之外无其他要求。而且，技术委员会成员也可以同时在 OpenStack 基金会其他部门兼任职位。

而随着越来越多的用户在生产环境中使用OpenStack，以及OpenStack生态圈里越来越多的合作伙伴在云中支持OpenStack，社区指导用户使用和产品发展的使命就变得越来越重要。鉴于此，OpenStack用户委员会应运而生。

OpenStack 用户委员会的主要任务是收集和归纳用户需求，并向董事会和技术委员会报告；以用户反馈的方式向开发团队提供指导；跟踪 OpenStack 部署和使用，并在用户中分享经验和案例；与各地 OpenStack 用户组一起在全球推广 OpenStack。

# （3）Apache基金会

Apache 基金会简称为 ASF, 在它支持的 Apache 项目与子项目中, 所发行的软件产品都需要遵循 Apache 许可证。

对于开发者来说，在Apache的生态世界中，有“贡献者 $\rightarrow$ 提交者 $\rightarrow$ 成员”这样的成长路径。积极为Apache社区贡献代码、补丁或文档就能成为贡献者。通过会员的指定，能够成为提交者，就会拥有一些“特权”。提交者中的优秀分子可以“毕业”成为成员。

Apache 基金会为孵化项目提供组织、法律和财务方面的支持，目前其已经监管了数百个开源项目，包括 Apache HTTP Server、Apache Hadoop、Apache Tomcat 等。其中，Kylin 就是中国首个 Apache 顶级项目。

# 1.1.2 LFN

为了解决项目太多、协调性太差，从而导致的整个生态系统不协调的问题，2018年年初，ONAP、OPNFV、OpenDaylight、FD.IO、PDNA和SNAS等Linux基金会

旗下的六大网络开源项目聚集在一起，创立了用于跨项目合作的LFN（LF Networking Fund）。

LFN的这六大创始开源项目，覆盖了从数据平面到控制平面、编排、自动化、端到端测试等领域，为跨项目协作提供了一个平台。通过统一的董事会管理，LFN消除不同项目之间的重叠或冗余，创建更高效的流程，加快开源网络的发展进程。

LFN 仅仅为各个项目之间的合作提供一个平台，其中的每个项目都将继续保持技术独立和发布蓝图，六个项目的技术指导委员会（TSC）保持不变，但是将由一个技术咨询委员会（TAC）监管。此外，还有一个营销顾问委员会（MAC），统一负责六个项目的市场活动。

新的组织结构解决了各个成员项目之间重复收费的问题，在LFN成立之前，成员想要加入任何一个项目都需要缴纳会员费，但是LFN成立之后只需要缴纳LFN的会员费，就可以参加已经加入及未来即将加入的任何LFN项目。

# 1.2 网络标准及架构

# 1.2.1 OpenFlow

作为SDN的主要实现方式，OpenFlow发展史就是SDN的发展史，对整个SDN的发展起着功不可没的作用。

# 1. OpenFlow 起源

OpenFlow 起源于斯坦福大学的 Clean Slate 项目组，Clean Slate 项目的最终目的非常大胆，是要“重新发明因特网（Reinvent the Internet）”，改变被认为已经略显不合时宜且难以进化发展的现有网络基础架构。

Clean Slate 项目的学术主任（Faculty Director）——Nick McKeown 教授，与他的学生 Martin Casado 发现，如果将传统网络设备的数据平面（Data Plane，数据转发）和控制平面（Control Plane，路由控制）相分离，通过集中式的控制器（Controller）以标准化的接口对各种网络设备进行管理和配置，那么将为网络资源的设计、管理和使用提供更多的可能性，从而更容易推动网络的革新与发展。于是，他们于 2008 年 4 月在 ACM Communications Review 发表了题为 OpenFlow: enabling innovation in

campus networks 的论文，首次提出了 OpenFlow 的概念。

OpenFlow将控制逻辑从网络设备中剥离出来，形成了如图1-1所示的控制转发分离架构。

![](images/615ba21a3b04a3f6291a8c99a6fc60bde5a91dfe5fb5f89ca06719f8673c1666.jpg)  
图1-1OpenFlow控制转发分离架构

在OpenFlow发展的初期，为了达到更好利用现有硬件的目的，需要网络设备中内置一种稀有且昂贵的特殊内存TCAM（TernaryContent-AddressableMemory）来保存流表。

设计OpenFlow的初衷是无须更改已搭载TCAM的网络设备硬件，仅通过软件升级即可实现网络行为变更，能够一边考虑应用现有架构，一边构建虚拟网络，也是OpenFlow广受业界关注的原因所在。

TCAM在初期的OpenFlow设计思想中占有非常重要的地位，很多网络设备中也确实都搭载了TCAM，Nick McKeown教授的论文中就有这样的表述：“目前最先进的以太网交换机和路由器都包含一个能够以线速实现防火墙、NAT、QoS等功能并收集统计信息的流表（通常是基于TCAM构建），而我们正是利用了这一点。”

# 2. OpenFlow 版本变迁

OpenFlow 自产生以来，一直由开放网络基金会（ONF，Open Networking Foundation，一个致力于开放标准和 SDN 应用的用户主导型组织）管理，OpenFlow 协议也经历了很多个版本。

2009年年底发布的1.0版本相对比较弱，只是奠定了OpenFlow协议的基调，它反映的是早期学者对网络设备的一种理想模型假设。这种假设认为交换机有很大的TCAM表项。

但是这种假设脱离了实际，TCAM表项资源非常宝贵，能够保存的流表非常有限，很难满足现实生产环境的需要。

分别于2011年2月与5月发布的OpenFlow1.1和1.2版本增加了很多特性，其中最重要的是引入了Group和MultiTable概念。Group是对一个或者多个端口的抽象，应用于组播或者广播，多个流表可以引用同一个组。MultiTable指的是多级流表。Group和MultiTable的提出可以很大地减少流表数量，更加贴近实际的交换机模型。

OpenFlow1.3于2012年发布，是对1.1和1.2版本的升级，特性变得更为丰富，主要增加了Meter和QOS，可以对网络带宽进行限速并进行有效的管理，从而保证服务质量。

# 3. OpenFlow设计思路

OpenFlow协议的思路是网络设备维护一个FlowTable，并且只通过FlowTable对报文进行处理，FlowTable本身的生成、维护和下发完全由外置的控制器实现。此外，OpenFlow交换机把传统网络中完全由交换机或路由器控制的报文转发，转换为由交换机和控制器共同完成，从而实现报文转发与路由控制的分离。控制器则通过事先规定好的接口操作OpenFlow交换机中的流表，达到数据转发的目的。

在OpenFlow交换机中，包含了安全通道、多级流表和组表。通过安全通道，OpenFlow交换机可以和控制器建立基于OpenFlow协议的连接；而流表则用来匹配OpenFlow交换机收到的报文；组表用来定义流表需要执行的动作。

# 4. FlowTable

OpenFlow 通过用户定义的或预设的规则匹配和处理网络包。一条 OpenFlow 的规则由匹配域、优先级、处理指令和统计数据等字段组成，如图 1-2 所示。

<table><tr><td>Ingress Port</td><td>Ether Source</td><td>Ether Dst</td><td>Ether Type</td><td>Vlan id</td><td>Vlan Priority</td><td>IP src</td><td>IP dst</td><td>IP proto</td><td>IP ToS bits</td><td>TCP/UDP Src Port</td><td>TCP/UDP Dst Port</td></tr></table>

图1-2 OpenFlow规则

在一条规则中，可以根据网络包在L2、L3或者L4等网络报文头的任意字段进行匹配，比如以太网帧的源MAC地址、IP包的协议类型和IP地址或者TCP/UDP的端口号等。目前OpenFlow的规范中还规定了Switch设备厂商可以有选择性地支持通配符进行匹配。OpenFlow未来还计划支持对整个数据包的任意字段进行匹配。

所有 OpenFlow 的规则都被组织在不同的 FlowTable 中，而在同一个 FlowTable 中，按规则的优先级进行先后匹配。一个 OpenFlow Switch 可以包含一个或者多个 FlowTable，从 0 开始依次编号排列。

OpenFlow规范中定义了流水线式的处理流程，如图1-3所示。当网络数据包进入Switch后，必须从table0开始依次匹配，table可以按从小到大的次序越级跳转，但不能从某一table向前跳转至编号更小的table。当数据包成功匹配一条规则后，将首先更新该规则对应的统计数据（如成功匹配数据包总数目和总字节数等），然后根据规则中的指令进行相应操作，比如跳转至后续某一table继续处理，修改或立即执行该数据包对应的ActionSet等。当数据包已经处于最后一个table时，其对应的ActionSet中的所有Action将被执行，包括转发至某一端口、修改数据包的某一字段、丢弃数据包等。OpenFlow规范对目前所支持的Instructions和Actions进行了完整详细的说明和定义。

![](images/c5a1dc32e993f18fcabd2cb58401b1d200a622e40e6316ee46328c211bead4ed.jpg)  
图1-3 数据包处理流程

# 5. OpenFlow通信通道

OpenFlow 协议主要通过对不同类型消息的处理来实现控制器与交换机之间的路由控制。目前，OpenFlow 主要支持三种消息类型，分别是 Controller-to-Switch、Asynchronous（异步消息）及 Symmetric（对称消息）。

- Controller-to-Switch：指由Controller发起，Switch接收并处理的消息，主要包括Features、Configuration、Modify-State、Read-Stats和Barrier等消息。这些消息主要由Controller对Switch进行状态查询和修改配置等操作。  
- Asynchronous: 由 Switch 发送给 Controller, 用来通知 Switch 上发生的某些异步事件的消息, 主要包括 Packet-in、Flow-Removed、Port-Status 和 Error 等。例如, 当某一条规则因为超时而被删除时, Switch 将自动发送一条 Flow-Removed 消息通知 Controller, 以方便 Controller 进行相应的操作, 比如重新设置相关规则。  
- Symmetric：主要用来建立连接，检测对方是否在线等，都是些双向对称的消息，包括Hello、Echo与厂商自定义消息。

Hello、Features、Echo 又分别包含了 Request 与 Reply 消息，每一对 Request 与 Reply 的 Transaction ID 相同，交换机通过 ID 进行识别对应事件端口。图 1-4 所示即为在通常的交换机事件发生时，主要经过的几个交互步骤。

![](images/7d3cbdd04e7dea78c97ea65ce8266a73aa26aad0c08652ed77ba0b596d3a3fb6.jpg)  
图1-4OpenFlow交换机与控制器的交互过程

# 6. OpenFlow 应用

随着OpenFlow以及SDN的发展和推广，其研究和应用领域也得到了不断拓展，

比如网络虚拟化、安全和访问控制、负载均衡、绿色节能，以及与传统网络设备交互和整合等。下面重点介绍网络虚拟化和负载均衡。

# （1）网络虚拟化——FlowVisor

网络虚拟化的本质是对底层网络的物理拓扑进行抽象，在逻辑上对网络资源进行分片或整合，从而满足各种应用对于网络的不同需求。为了达到这个目的，FlowVisor实现了一个特殊的OpenFlow Controller，可以看作其他不同用户或应用的Controller与网络设备之间的一层代理，如图1-5所示。因此，不同用户或应用可以使用自己的Controller来定义不同的网络拓扑，同时FlowVisor又可以保证这些Controller之间能够互相隔离且互不影响。

![](images/b78f664517a0cb4078aa82102842c951f92eea7dc458a7575b45f47590214e8c.jpg)  
图1-5 FlowVisor

FlowVisor 不仅是一个典型的 OpenFlow 应用案例，同时还是一个很好的研究平台，目前已经有很多基于 FlowVisor 的研究和应用。

# （2）负载均衡——Aster\*x

传统的负载均衡方案一般需要在服务器集群的入口处，通过一个 gateway 监测、统计服务器的工作负载，并据此将用户请求动态地分配到负载相对较轻的服务器上。既然网络中的所有网络设备都可以通过 OpenFlow 进行集中式的控制和管理，同时服务器的负载又可以及时地反馈给 OpenFlow Controller，那么 OpenFlow 就非常适合做负载均衡的工作。

如图1-6所示，基于OpenFlow的负载均衡模型Aster\*x通过HostManager和NetManager来分别监测服务器和网络的工作负载，然后将这些信息反馈给FlowManager，

这样 Flow Manager 就可以根据这些实时的负载信息，重新定义网络设备上的 OpenFlow 规则，从而将用户请求（即网络包）按照服务器的能力进行调整和分发。

![](images/cc68b11647c5b5cc208eb2243775e7aa0623bd0fc7861cca1acabe1608b9f992.jpg)  
图1-6 Aster\*x

# 1.2.2 SDN

基于OpenFlow为网络带来的可编程的特性，斯坦福的Nick McKeown教授和他的团队进一步提出了SDN（Software Defined Network，软件定义网络）的概念。

SDN 将控制功能从交换机中剥离出来，形成了一个统一的、集中式的控制平面，而交换机只保留了简单的转发功能，从而形成了转发平面（数据平面）。通过控制平面对数据平面的集中化控制，SDN 为网络提供了开放的编程接口，并实现了灵活的可编程能力，从而使网络能够真正地被软件定义，达到按需定制服务、简化网络运维、灵活管理调度的目标。

在SDN中，网络设备只负责单纯的数据转发，可以采用通用的硬件。如果将网络中所有的网络设备视为被管理的硬件资源，参考操作系统的设计原理，则可以抽象出一个网络操作系统（Network OS）的概念。这个网络操作系统一方面抽象了底层网络设备的具体细节，负责与网络硬件进行交互，实现对硬件的编程控制和接口操作，同时还为上层应用访问网络设备提供了统一的管理视图和编程接口。基于这个网络操作系统，用户可以开发各种网络应用程序，通过软件定义逻辑上的网络拓扑，以满足对网络资源的不同需求，而无须关心底层网络的物理拓扑结构。

# 1. SDN架构

SDN采用了如图1-7所示的基本架构，集中式的控制平面和分布式的转发平面相互分离，控制平面利用控制器、转发通信接口对转发平面上的网络设备进行集中式管理。

![](images/2ecbb458d91ab51c9a92804067eb90b31feb31b823130899cb382d99e1c3c6f9.jpg)  
图1-7 SDN基本架构

- 基础设施层（Infrastructure Layer）：主要承担数据转发功能，由各种网络设备构成，如数据中心的网络路由器，支持OpenFlow的硬件交换机等。  
- 控制层（Control Layer）：网络转发的控制管理平面，负责管理网络的基础设施，主要组成部分为 SDN 控制器。SDN 控制器是整个网络的大脑、控制中心，主要功能是按照配置的业务逻辑，产生对应的数据平面的流转发规则，通过下发给网络设备，控制其进行数据转发。  
应用层（Application Layer）：指商业应用。开发者可以通过SDN控制器提供的北向接口，如REST接口实现应用和网络的联动，例如网络拓扑的可视化、监控等。  
- 南向接口（Sorthbound Interface）：SDN 控制器对网络的控制主要通过 OpenFlow、NetConf 等南向接口实现，包括链路发现、拓扑管理、策略制定、表项下发等。其中，链路发现和拓扑管理主要是控制其利用南向接口的上行通道对底层交换设备上报信息进行统一的监控和统计，而策略制定和表项下发则是控制器利用南向接口的下行通道对网络设备进行统一的控制。  
- 北向接口（Northbound Interface）：北向接口是通过控制器向上层应用开放

的接口，其目标是使得应用能够便利地调用底层的网络资源和能力。因为北向接口是直接为应用服务的，因此其设计需要密切联系应用的业务需求，比如需要从用户、运营商或产品的角度去考量。

在SDN发展初期，控制平面的表现形式更多是以单实例的控制器出现，实现SDN的协议也以OpenFlow为主，因此SDN控制器更多指的是OpenFlow控制器。随着SDN的发展，ONF也在白皮书中提出了SDN的架构标准。广义的SDN支持丰富的南向协议，包括OpenFlow、NetConf、OVsDB、BGPLS、PCEP及厂商协议等，可实现灵活可编程和灵活部署，支持网络虚拟化、SR路由、智能分析和调度。

与南向接口方面已有OpenFlow等国际标准不同，目前还缺少业界公认的北向接口标准。因此，北向接口的协议制定成为当前SDN领域竞争的一大焦点，不同的参与者或从各种角度提出了很多方案。据悉，目前至少有20种控制器，每种控制器都会对外提供北向接口，用于上层应用开发和资源编排。当然，对于上层的应用开发者来说，RESTful API是比较乐于采用的北向接口形式。

# 2. SDN实现

SDN需要某种方法使控制平面能够与数据平面进行通信。OpenFlow就是这样一种方法机制，但OpenFlow并非实现SDN的唯一途径。

# （1）IETF定义的开放SDN架构

如图1-8所示，IETF定义的开放SDN架构的核心思路是重用当前的技术而不是OpenFlow，比如利用Netconf和已有的设备接口。IETF的Netconf使用XML来配置设备，旨在减少与自动化设备配置有关的编程工作量。这种架构充分地利用了现有设备，能够更大限度地保护已有的投资。

![](images/926f295cfd363255bb20e4401a1111fde35bf8f2c90390121f0c4f008e3a7f0f.jpg)  
图1-8 IETF定义的开放SDN架构

# （2）Overlay网络技术

如图1-9所示，是在现行的物理IP网络基础上建立叠加逻辑网络（Overlay Logical Network），屏蔽底层物理网络差异，实现网络资源的虚拟化，使得多个逻辑上彼此隔离的网络分区，以及多种异构的虚拟网络可以在同一共享的物理网络基础设施上共存。

![](images/a33b81735d55ca3621d7089f144346490274ee9bc870966bbb468a02df012a27.jpg)  
图1-9 Overlay网络

在网络技术领域，Overlay 是一种网络架构上叠加的虚拟化技术模式，是指建立在已有网络上的虚拟网，由逻辑节点和逻辑链路构成。其大体框架是对基础的物理 IP 网络不进行大规模修改的条件下，实现应用在网络上的承载，并能与其他网络业务分离。

Overlay网络的主要思想可被归纳为解耦、独立、控制三个方面。解耦是指将网络的控制从网络物理硬件中脱离出来，交给虚拟化的Overlay逻辑网络处理。

独立是指 Overlay 网络承载于物理 IP 网络之上，因此只要 IP 可达，那么相应的虚拟化网络就可以被部署，而无须对原有物理网络架构（例如原有的网络硬件、原有的服务器虚拟化解决方案、原有的网络管理系统、原有的 IP 地址等）做出任何改变。Overlay 网络可以便捷地在现网上部署和实施，这是它最大的优势。

控制是指叠加的逻辑网络将以软件可编程的方式被统一控制，网络资源可以和计算资源、存储资源一起被统一调度和按需交付。

Overlay网络叠加的实现方案包括VXLAN、NVGRE、NVP等，主要由虚拟化技术厂商主导，比如VMware在其虚拟化平台中实现了VxLAN技术、微软在其虚拟化平台中实现了NVGRE技术，其中最典型的代表是Nicira公司提出的NVP（Network Virtualization Platform，网络虚拟化平台）。NVP支持在现有的网络基础设施上利用隧道技术屏蔽底层物理网络的实现细节，实现了网络虚拟化，并利用逻辑上集中的软件进行统一管控，实现网络资源的按需调度。

Overlay网络叠加方案与虚拟化的整合比较便捷，但是在实际应用中，效果会受到底层网络质量的影响。同时，基于网络叠加的技术也会增加网络架构的复杂度，并降低数据的处理性能。

Overlay与SDN可以说天生就是适合互相结合的技术组合。对Overlay网络中的虚拟机进行管理和控制，而SDN恰好可以完美地做到这一点。

# （3）基于专用接口

基于专用接口的方案的实现思路是不改变传统网络的实现机制和工作方式，通过对网络设备的操作系统进行升级改造，在网络设备上开发出专用的API接口，管理人员可以通过API接口实现网络设备的统一配置管理和下发，改变原先需要一台台设备登录配置的手工操作方式，同时这些接口也可供用户开发网络应用，实现网络设备的可编程。典型的基于专用接口的SDN实现方案是的思科ONE架构。

# 1.2.3 P4

现有的SDN解决方案将控制平面与转发平面分离，并提供了控制平面的可编程

能力。而事实上，这种通过软件编程实现的控制平面的功能，在传统的高级交换机和路由器上也都能实现，差别只是厂商把这些功能固化在了硬件中，第三方难以介入进行定制或二次开发。虽然一些高级设备提供了SDK，以便用户能够进行一定程度的定制，但也必须受厂商制定的规范限制，能做的事情十分有限。目前SDN所做的就是打破这些限制，让设备和网络更加灵活，让用户不被厂商的规范所绑定，从而拥有无限的可能。

现有的SDN解决方案为用户开放的是控制平面的可编程能力，那么转发平面又如何呢？在正常情况下，对于转发设备来说，数据包的解析转发流程是由设备转发芯片固化的，所以设备在协议的支持方面并不具备扩展能力。并且，厂商扩展转发芯片所支持的协议特性，甚至开发新的转发芯片以支持新的协议，代价非常高，需要将之前的硬件重新设计，这样势必导致更新的成本居高不下、时间周期长等一系列问题。所以，在一定程度上，这种将支持的协议与功能同硬件绑定的模式限制了网络的快速发展。

因此，新一代的SDN解决方案必须让转发平面也具有可编程能力，让软件能够真正定义网络和网络设备。而P4（Programming Protocol-Independent Packet Processors）正是为用户提供了这种能力，打破了硬件设备对转发平面的限制，让数据包的解析和转发流程也能通过编程去控制，使得网络及设备自上而下地真正向用户开放。

P4 起源于由 Nick 教授等联合发布的一篇论文 P4: Programming Protocol-Independent Packet Processors, 该论文在 SDN 领域引起了极大的反响和关注度。Nick 教授等人又发布了 “The P4 Language Specification” “Barefoot 白皮书” 等文件。再之后, ONF 成立了开源项目 PIF, 为 P4 提供配套的中间表示 IR。

P4是一门主要用于数据平面的编程语言，可简单地将P4语言与C语言进行对比：

C语言程序代码 $\rightharpoondown$ gcc或其他编译器 $\rightharpoonup$ 可执行文件，运行在x86CPU、ARM等目标上。  
P4语言代码 $\rightharpoondown$ P4编译器 $\rightharpoondown$ 硬件或其他形式输出，运行在CPU、FPGA、ASIC等目标上。

P4 解决数据平面的可编程问题，OpenFlow 是解决控制平面的可编程问题。它们

的关系如图1-10所示。

![](images/76a8fa669ed8b3bdff5373a32e884f109f3d2020549466d7f622f3fdd0fd4035.jpg)  
图1-10 OpenFlow与P4的关系

由于P4的定位是高级编程语言，所以P4可以定义任意自己想要的配置。它可以让设备与SDN控制器通过OpenFlow通信，也可以通过本地的交换机操作系统控制，一切皆由P4程序设计而定。在P4语言中，OpenFlow只是一个程序，两者可以协同工作，事实上也已经有了使用P4语言编写的实现OpenFlow功能的程序openflow.p4。

如图1-11所示为P4的架构，P4语言具有下面三个特性：

协议无关性：网络设备不与任何特定的网络协议绑定，用户可以使用P4语言描述任何网络数据平面的协议和数据包处理行为。  
目标无关性：用户不需要关心底层硬件的细节就可实现对数据包处理方式的编程描述。这一特性通过P4前后端编译器实现，前端编译器将P4高级语言程序转换成中间表示IR，后端编译器将IR编译成设备配置，自动配置目标设备。  
- 可重构性：允许用户随时改变包解析和处理的程序，并在编译后配置交换机，真正实现现场可重配能力。

为了实现上述特性，P4语言的编译器采用了模块化的设计，各个模块之间的输入输出都采用标准格式的配置文件，如p4c-bm模块的输出可以作为载入到bmv2模块中的JSON格式配置文件。

![](images/e8a39f1a51efcb56a5797a22ccb2d5a4e6b54fd71cc252014008567dc9c5ed25.jpg)  
图1-11 P4的架构

P4编译器本质上是将在P4程序中表达的数据平面的逻辑翻译成一个在特定可编程数据包处理硬件上的具体物理配置。因此，编译器后端部分自然与其支持的硬件目标紧密结合，而其前端部分则可以在各个P4可编程目标之间通用。这就意味着一个P4程序的具体实现可根据被编译的目标而改变。

P4语言联盟是一个P4开源社区，由工业界和学术界成员组成。它有两个目标：定义P4语言的正式规范，维持开放源码的P4开发工具和P4的参考程序。

P4语言联盟发布了一个P4参考程序“switch.p4”，它能实现各种流行的标准数据平面协议和功能，包括L2和L3（IPv4和IPv6）转发、虚拟局域网（vLAN）、生成树协议（STP）、等价多路径、链路聚合、虚拟路由和转发（VRF）、IP组播、多协议标签交换、各类隧道协议（如虚拟可扩展局域网、通用路由封装、IP-in-IP和Q-in-Q）、数据包镜像、服务质量控制、访问控制、RPF验证、传输协议（TCP、UDP等）。

# 1.2.4 ETSI的NFV参考架构

由于电信运营商网络包括大量的专有硬件设备，如果运营商想要推出一个新的网络服务，如负载均衡或防火墙，就往往需要购置各种新硬件，之后再为这些新硬件匹配合适的空间和电力。能否以软件的方式解决这些问题？NFV（网络功能虚拟化）应运而生。

NFV 由运营商联盟提出，为了加速部署新的网络服务，运营商倾向于放弃笨重且昂贵的专用网络设备，转而使用标准的 IT 虚拟化技术拆分网络功能模块，如 DNS、NAT、Firewall 等。通过将硬件与虚拟化技术结合，NFV 可以实现所有的网络功能。

一些运营商在欧洲通信标准协会ETSI（European Telecommunications Standards Institute）成立了NFV工作组（ETSI ISG NFV），开展网络功能虚拟化研究、标准制定和产业推动工作，致力于将虚拟化技术应用于电信领域。

NFV系统中软件、虚拟层和网络功能分层解耦，打破了电信行业原有的“黑盒化”封闭系统，降低了电信准入门槛，有利于打造更具活力的生态系统，从根本上改变了CT的发展生态。一方面，充分解耦后的碎片化网络对运营商的管理和运维带来了巨大的挑战，这需要依赖新型的管理系统；另一方面，NFV给网络带来极大的灵活性和敏捷性，但这依赖于新的管理系统和自动化编排系统。

![](images/030c594d3e8ffb1199545ceb76cb9a7736f790c4e5ae74a0f7804046f28319eb.jpg)  
如图1-12所示为ETSINFV标准框架  
图1-12 ETSINFV标准框架

其中，NFV infrastructure（NFVI）、MANO和VNF（Virtual Network Function）是顶层的概念实体。

NFVI包含了虚拟化层（Hypervisor或容器管理系统，如Docker）及物理资源，

如交换机、存储设备等。NFVI可以跨越若干个物理位置进行部署，为这些物理站点提供数据连接的网络也成为NFVI的一部分。

VNF与NFV虽然是三个同样的字母调换了顺序，但含义截然不同。NFV是一种虚拟化技术或概念，解决了将网络功能部署在通用硬件上的问题；而VNF指的是具体的虚拟网络功能，提供某种网络服务，是一种软件，利用NFVI提供的基础设施部署在虚拟机、容器或物理机中。相对于VNF，传统的基于硬件的网元可以称为PNF。VNF和PNF能够单独或混合组网，形成ServiceChain，提供特定场景下所需的E2E（End-to-End）网络服务。

MANO 提供了 NFV 的整体管理和编排，向上接入 OSS/BSS（运营支撑系统/业务支撑系统），由 NFVO(NFV Orchestrator)、VNFM(VNF Manager) 及 VIM(Virtualised Infrastructure Manager) 虚拟化基础设施管理器三者共同组成。

编排（Orchestration）一词最早出现于艺术领域，指的是按照一定的目的对各种音乐、舞蹈元素进行排列，以期达到最好的效果。而引申到网络的范畴，编排则指以用户需求为目的，将各种网络服务单元进行有序的安排和组织，生成能够满足用户要求的服务。在NFV架构中，凡是带“O”的组件都有一定的编排作用，各个VNF、PNF及其他各类资源只有在合理编排下，在正确的时间做正确的事情，整个系统才能发挥应有的作用。

VIM 主要负责基础设施层虚拟化资源和硬件资源的管理、监控和故障上报，并面向上层 VNFM 和 NFVO 提供虚拟化资源池，负责虚拟机和虚拟网络的创建和管理，OpenStack 和 VMware 都可以作为 VIM。VNFM 负责 VNF 的生命周期管理，如上线、下线，状态监控。VNFM 基于 VNFD（VNF Descriptor，描述一个 VNF 模块部署与操作行为的配置模板）来管理 VNF。NFVO 负责 NS（Network Service）生命周期的管理和全局资源调度。

# 1.3 Linux开源网络生态

乱花渐欲迷人眼，Linux开源网络世界基本上可以用图1-13理个明白。

![](images/9bcca3e000b24df65bfbe093fa73fcf0cbc7e5f3899c59acf86c5631724e108b.jpg)

ons

[1] Liao et al. 2017, https://doi.org/10.1101/2017.04.29.3658.  
[2]

![](images/ae0a747dd11720357761a3a6cf9957a1ee71de6f3ae51f7b726fe308fdd68412.jpg)  
图1-13 Linux开源网络

# 1.3.1 开源硬件

大部分商业交换机是软硬件一体的，买Cisco就自带NX-OS/iOS，买H3C就自带Commvare。而白牌交换机的出现使得交换机可以选择操作系统成为可能，如同买PC可以安装Windows，也可以安装Linux一样。

在OCP等开放组织、众多芯片商、ODM商、互联网用户的推动下，业界已经在逐步走向开放。在此基础上，网络设备硬件的设计也正朝着模块化、开放标准化的方向革新，软硬件分离也成为一种趋势，如图1-14所示。

![](images/64edb6015cb241af8097bff83e1fbb73f4bcc21db8df4684e24ab1412c75b43e.jpg)  
图1-14 传统的网络设备硬件转换到软硬件分离的新模式

OCP在2013年年中左右成立了Networking工作组，致力于构建开放标准化的数据中心网络相关技术。当前阶段主要聚焦在TOR上：首先联合芯片及硬件厂商制定TOR硬件标准，并推出开放网络安装环境（ONIE，Open Network Install Environment），试图解除交换机软硬件绑定的黑盒状态，形成硬件标准化、软硬件分离的新模式；其次试图将交换机进行ASIC抽象，去构建一个开放标准的API编程接口，屏蔽硬件芯片及平台差异，并最终促成开源网络操作系统的诞生。

2016年2月，Facebook成立了新的TIP（TelecomInfraProject）项目，将运营商、基础设施提供商、系统集成商及其他的科技企业聚集到一起，共同合作发展新技术，用新技术改变传统的构建部署电信网络基础设施的方法，并运用开放的OCP模型刺激创新。

# 1.3.2 虚拟交换

# 1. DPDK

DPDK（Data Plane Development Kit）可提供高性能的数据包处理库和用户空间驱动程序。它不同于Linux系统以通用性设计为目的，而是专注于网络应用中数据包的高性能处理。具体体现在DPDK绕过了Linux内核协议栈对数据包的处理过程，运行在用户空间上利用自身提供的数据平面库来收发数据包。

在最近的一项研究中，使用DPDK的OVS（Open vSwitch）平均吞吐量提高了 $75\%$ 。该技术被英特尔公司推广，可以在多处理器上使用，并作为EPA（Enhanced Platform Awareness，旨在加速数据平面）技术的一部分。EPA除DPDK以外的主要技术是大页、NUMA和SR-IOV：大页通过减少页面查找提高VNF的效率；NUMA确保工作负载使用处理器本地的内存；SR-IOV可以使网络流量旁路管理程序，直接转到虚拟机。

# 2. OVS-DPDK

DVS 是一个具有工业级质量的多层虚拟交换机，它支持 OpenFlow 和 OVSDB 协议。通过可编程扩展，可以实现大规模网络的自动化（配置、管理和维护）。最初的 OVS 版本是通过 Linux 内核进行数据分发的，因而用户能够得到的最终吞吐量受限于 Linux 网络协议栈的性能。

OVS-DPDK使用DPDK技术对OpenVSwitch进行优化。OVS-DPDK是用户态的vSwitch，网络包直接在用户态进行处理。

# 3. FD.IO

FD.IO（Fast Data Input/Output）是Linux基金会旗下的开源项目，LFN六大创始项目之一，成立于2016年2月11日。

FD.IO在通用硬件平台上提供了具有灵活性、可扩展、组件化等特点的高性能I/O服务框架，该框架支持高吞吐量、低延迟、高资源利用率的用户空间I/O服务，并可适用于多种硬件架构和部署环境。

FD.IO 的关键组件来自 Cisco 捐赠的商用 VPP（Vector Packet Processing，矢量分组处理引擎）库。VPP 和 FD.IO 其他子项目如 NSH_SFC、Honeycomb、ONE 等一起用于加速数据平面。

所谓VPP向量报文处理是与传统的标量报文处理相对而言的。传统报文处理方式的逻辑是：按照到达先后顺序来处理，第一个报文处理完，处理第二个，依次类推；函数会频繁嵌套调用，并最终返回。相比而言，向量报文处理则是一次并行处理多个报文，相当于一次处理一个报文数组，扩展了整个数据包集合的查找和计算开销，从而提高了效率。

# 1.3.3 Linux操作系统

在Linux中，网络分为两个层次，分别是网络协议栈，以及接收和发送网络协议的设备驱动程序。网络协议栈是硬件中独立出来的部分，主要用来支持TCP/IP等多种协议，而网络设备驱动程序连接了网络协议栈和网络硬件设备。Linux中与网络有关的实现主要有：

网络驱动程序。  
LinuxVLAN：一种虚拟设备，只有绑定一个真实网卡才能完成实际的数据发送和接收。  
- Linux Bridge（网桥）：工作于二层的虚拟网络设备，功能类似于物理的交换机。其他Linux网络设备可以被绑定到Bridge上作为从设备，并被虚拟化为端口。当一个从设备被绑定到Bridge上时，就相当于真实网络中的交

换机端口插入了一个连接有终端的网线。

- Linux TCP/IP 协议栈：可以处理 IP、ICMP、ARP、TCP/UDP/SCTP 等协议。  
- Linux Socket 函数库：从 Berkeley 大学开发的 BSD UNIX 系统中移植而来。网络的 Socket 数据传输是一种特殊的 I/O。  
Linux应用层协议：处理更高层的协议，常用的有DNS、HTTP、SSH、Telnet等。

# 1.3.4 网络控制

为了更简洁、方便、友好地使用各种硬件资源，SDN把网络设备的控制功能提取出来，统一放到其控制器（SDNC，SDN Controller）中，只保留其数据转发的功能，并抽象出一个网络操作系统的概念。

# 1. OpenDaylight

ODL（OpenDaylight）是由Linux基金会和多家行业巨头如Cisco、Juniper和Broadcom等公司一起创立的开源项目，其目的在于推出一个通用的SDN控制平台。

ODL支持OpenFlow、Netconf和OVsDB等多种南向接口，是一个广义的SDN控制平台。ODL支持分布式集群，不仅可以管理更大的网络，性能更好，还可以相互容灾备份，提升系统的可靠性。它包括一系列功能模块，可以动态地组合，提供不同的服务。

ODL主要的功能模块有拓扑管理、转发管理、主机监测、交换机管理等。ODL控制平台引入了模型驱动的设计思想，构建了服务抽象层MD-SAL，是控制器模块化的核心，能够自动适配底层不同的设备，使开发者专注于业务应用的开发。

# 2. ONOS

ONOS（Open Network Operating System）顾名思义就是要定义一个开放的网络操作系统，其核心的服务对象是服务提供商。既然服务对象要达到运营商的级别，那么其重点就需要考虑可靠性与性能，并能够在白盒系统上创建高性能可编程的运营商网络。

ONOS的北向接口抽象层和API可以使得应用开发变得更加简单，而通过南向接口抽象层和API则可以管控OpenFlow或传统设备。北向接口基于具有全局网络视

图的框架，南向接口包括OpenFlow和Netconf，以便能够管理虚拟和物理交换机。ONOS的核心是分布式的，因此可以水平扩展，架构如图1-15所示。

![](images/472865b479ee28f0756d1f15802da2c8486dae7c9546f5c9f0174e9785c721fd.jpg)  
图1-15 ONOS架构

ONOS在诞生之初就是为了对抗ODL，希望能成为控制器的主流。目前主要的参与者包括AT&T、CIENA、VERIZON、NTT、爱立信、华为、NEC、INTEL、富士通等。

# 3. Tungsten Fabric

Tungsten Fabric是由OpenContrail（由Juniper开源的SDN控制器）向Linux基金会迁移并更名而来的。Tungsten Fabric是一个可扩展的多云网络平台，能够与包括Kubernetes和OpenStack在内的多个云平台集成，并且支持私有云、混合云和公有云部署。

# 1.3.5 云平台

# 1. OpenStack

2010年7月，RackSpace和美国国家航空航天局合作，分别贡献出RackSpace云文件平台代码和NASA Nebula平台代码，并以Apache许可证开源发布了OpenStack第一个版本Austin，以RackSpace所在的美国德州（Texas）首府命名，计划每隔几个月发布一个全新版本，并且以26个英文字母为首字母，从A~Z顺序命名后面的版本代号。

第一版 Austin 仅有 Swift 和 Nova 两个项目，分别来自 RaceSpace 云文件平台和 NASA Nebula 平台，目的为云计算提供对象存储和计算平台。

在2012年9月的Folsom版本中，OpenStack社区将Nova项目中的网络模块和块存储模块剥离出来，成立了两个新的核心项目，分别是Quantum和Cinder。但由于商标版权冲突问题，后来经过提名投票评选Quantum被更名为Neutron。

Neutron 通过插件的方式对众多的网络设备提供商进行支持，比如 Cisco、Juniper 等，同时也支持很多流行的技术，比如 Openswitch、OpenDaylight 和 SDN 等。

Neutron的插件分为CorePlugin和ServicePlugin两类。CorePlugin负责管理和维护Neutron的Network、Subnet和Port三类核心资源的状态信息，这些信息是全局的，只需要也只能由一个CorePlugin管理。Havana版本中实现了ML2（ModularLayer2）CorePlugin用于取代原有的CorePlugin。对三类核心资源进行操作的RESTAPI被neutron-server看作CoreAPI，由Neutron原生支持。

Network：代表一个隔离的二层网段，是为创建它的租户而保留的一个广播域。Subnet和Port始终被分配给某个特定的Network。Network的类型包括Flat、VLAN、VxLAN、GRE等。  
- Subnet: 代表一个IPv4/v6的CIDR地址池，以及与其相关的配置，如网关、DNS等，该Subnet中的VM实例随后会自动继承该配置。Sunbet必须关联一个Network。  
- Port: 代表虚拟交换机上的一个虚机交换端口。VM 的网卡 VIF 连接 Port 后，会拥有 MAC 地址和 IP 地址。Port 的 IP 地址是从 Subnet 地址池中分配的。

Service Plugin 即为除 Core Plugin 以外其他的插件，包括 I3 router、firewall、loadbalancer、VPN、metering 等，主要实现 L3~L7 的网络服务。这些插件要操作的资源比较丰富，对这些资源进行操作的 REST API 被 neutron-server 看作 Extension API，需要厂家自行进行扩展。

# 2. Kubernetes

以前，想要在线上服务器中部署一个应用，首先需要购买一个物理服务器，在服务器上安装一个操作系统，然后安装应用所需要的各种依赖环境，最后才能进行应用

的部署。

在虚拟化技术出现以后，在本地操作系统之上增加了一个Hypervisor层，通过Hypervisor层，可以创建不同的虚拟机，限定每个虚拟机能够使用的物理资源，并且每个虚拟机都是分离、独立的。例如，虚拟机A使用1个CPU、4GB内存、100GB磁盘，虚拟机B使用2个CPU、8GB内存、200GB磁盘等，从而实现物理资源利用率的最大化。如此一来，一台物理机就可以部署多个应用，每个应用都可以独立运行在一个虚拟机里。

但是，因为每一个虚拟机都是一个完整的操作系统，所以需要为其分配一定的物理资源，随着虚拟机数量的增多，操作系统本身消耗的资源势必增多。而且开发与运维的环境都比较复杂，比如前后端开发及测试，基于服务器或云环境运维等，这就导致了开发环境和线上环境的差异，开发环境与运维环境之间无法达到很好的衔接，在部署上线应用时，需要花时间处理环境不兼容的问题。

容器技术的出现解决了这样的问题。容器可以帮开发者把开发环境及应用整个打包带走，打包好的容器可以在任何的环境下运行，从而解决开发环境与运维环境不一致的问题。

容器技术正在成为对云计算领域具有深远影响的变革技术。作为容器的“重度玩家”，Google 在内部的成千上万台服务器上夜以继日地运行着无以计数的容器，并开发了 Borg 用于管理如此巨量的基础设施，而就在几年前，Borg 团队将多年积累的容器运行编排管理经验聚集到了一个名为 Kubernetes 的新项目之上并开源。2015 年，Google 将 Kubernetes 项目捐赠给新成立的 CNCF 基金会。

为了与Borg主题保持一致，Kubernetes又被命名为“九之七项目”（Project Seven of Nine），这也是为什么Kubernetes的Logo有七条边。

Kubernetes，又称为k8s（首字母为k、首字母与尾字母之间有8个字符、尾字母为s，所以简称为k8s），或者简称为“kube”，设计初衷是在主机集群之间提供一个能够自动化部署、可扩展、应用容器可运营的平台。在整个k8s生态系统中，能够兼容大多数的容器技术实现，比如Docker与Rocket。

如图1-16所示，与网络、存储、安全性、遥测和其他服务整合，Kubernetes可以提供全面的容器基础架构。借助Kubernetes的编排功能，可以构建跨多个容器

的应用服务，并且能够跨集群调度、扩展这些容器，长期、持续管理这些容器的健康状况。

![](images/d0f023e117bdcc68f556039411d09feef951e3cbb9a5665d046cb517eba18940.jpg)  
图1-16 Kubernetes与其他服务整合

# 1.3.6 网络编排

NFV 给网络带来极大的灵活性和敏捷性，但它们的实现依赖于新的管理系统和自动化编排系统。在 NFV 体系中，引入了全新的管理和编排系统——NFV MANO（NFV Management and Orchestration）系统，编排器作为其中的核心部件，是网络灵活调整和资源动态调度的关键，是下一代网管系统的核心。

NFV编排器由两层构成：服务编排和资源编排，可以控制新的网络服务，并将VNF集成到虚拟架构中，NFV编排器还能验证并授权NFV基础设施（NFVI）的资源请求。VNF管理器能够管理VNF的生命周期。VIM能够控制并管理NFV基础设施，包括了计算、存储和网络等资源。为了使NFVMANO行之有效，它必须与现有系统中的应用程序接口（API）集成，以便跨多个网络域使用多厂商技术。同样，OSS/BSS也需要与MANO实现互操作。

# 1.3.7 网络数据分析

# 1.PNDA

2016年8月16日，Linux基金会发布了一个网络数据分析平台PNDA（Platform for Network Data Analytics）。较早支持PNDA项目的公司包括Cisco、Deepfield、FRINX、Intersec、Moogsoft、NGENA、Ontology、OpenDataSoft、Tupl等。

PNDA旨在通过集成、缩放和管理一组公开的数据处理技术，并提供部署分析应

用和服务的端到端平台来降低复杂性。PNDA能够支持批量的实时数据流探索和分析，甚至可以达到每秒数百万消息的规模。

# 2. SNAS

SNAS（Streaming Network Analytics System）是一个实时跟踪和分析网络路由拓扑数据的框架。系统将从网络的第2层和第3层挖掘和收集数据，包括IP信息、服务质量及物理和设备规范。

如图1-17所示，SNAS架构主要包括一个高速的收集器、一个高性能的消息总线、消费者应用、数据库、RESTful API及用户应用。高性能的收集器生成解析后的数据并发送给消息总线，消费者应用负责通过消息总线API将数据存储在数据库里，然后用户应用可以通过RESTful API访问数据库里的数据。

![](images/6f0e49c1030a790f0358cc5588f22f39111a8821e00c98c280f23d4b8f21245e.jpg)  
图1-17SNAS架构

# 1.3.8 网络集成

OPNFV（Open Platform for NFV）是运营商级的开源网络集成参考平台。NFV架构里包含多个开源组件，不同开源组件间的集成和测试非常关键。OPNFV则提供了多组件持续开发、集成和测试的开源方案，并不断地向上游组织输出电信级NFV平台的增强特性。

OPNFV目前已经集成了OpenStack、ODL、ONOS、DPDK、ONAP、FD.IO等多个关键组件，发布了7个版本、超过60多个集成套件和几十个自动化测试工具，为NFV集成和测试提供了大量开源参考方案和自动化框架。

# 第2章

# Linux 虚拟网络

在一个传统的物理网络里，可能有一组物理Server，上面分别运行着各种各样的应用，比如Web服务、数据库服务等。为了彼此之间能够互相通信，每组物理Server都拥有一个或多个物理网卡（NIC），这些NIC被连接在物理交换设备上，例如交换机（Switch），如图2-1所示。

![](images/0b940b8d67f552419956cf5e60c207b17808d0ed7d1857e139126d1b67c0a081.jpg)  
图2-1 传统物理网络结构

在虚拟化技术被引入后，上述的多个应用可以按虚拟机的形式分享同一物理Server，虚拟机的生成与管理由Hypervisor或VMM完成，于是图2-1所示的网络结构被演化为图2-2。

![](images/eda4ffc7c6964af266cc68b2e733c6b5a6f23f26a0cdad11c890f619d98e5739.jpg)  
Virtual Machines   
图2-2 虚拟网络结构

虚拟机的网络功能由虚拟网卡（vNIC）提供，Hypervisor 可以为每个虚拟机创建一个或多个 vNIC。站在虚拟机的角度，这些 vNIC 等同于物理的网卡。为了实现与传统物理网络等同的网络结构，与 NIC 一样，Switch 也被虚拟化为虚拟交换机（vSwitch）。各个 vNIC 连接在 vSwitch 的端口上，最后这些 vSwitch 通过物理 Server 的物理网卡访问外部的物理网络。由此可见，一个虚拟的二层网络结构，主要是完成两种网络设备的虚拟化：NIC 硬件与交换设备。

此外，由于网络虚拟化概念的引入，对于原来基于物理二层以太网络和三层IP网的网络隔离也提出了诸多方面新的要求，例如可扩展性、安全性、可管理性等。

本章即对Linux环境下一些网络设备的虚拟化形式，以及组建虚拟化网络时涉及的主要技术进行介绍，这些内容也是基于Linux更深一步展开一切网络项目的基础。

# 2.1 TAP/TUN设备

TAP/TUN是Linux内核实现的一对虚拟网络设备，TAP工作在二层，TUN工作在三层，Linux内核通过TAP/TUN设备向绑定该设备的用户空间应用发送数据；反之，用户空间应用也可以像操作硬件网络设备那样，通过TAP/TUN设备发送数据。

基于TAP驱动，即可以实现虚拟网卡的功能，虚拟机的每个vNIC都与Hypervisor

中的一个TAP设备相连。当一个TAP设备被创建时，Linux设备文件目录下将会生成一个与之对应的字符设备文件，用户空间应用可以像打开普通文件一样打开这个文件进行读写。

当对TAP设备文件执行write()操作时，对于Linux网络子系统来说，相当于位于内核空间的TAP设备收到了数据，Linux内核收到此数据后将根据网络配置进行后续处理，处理过程类似于普通的物理网卡从外界收到数据。当用户空间应用执行read()请求时，相当于向内核查询TAP设备上是否有数据需要被发送，有的话则取出到用户空间里，从而完成TAP设备发送数据的功能。在这个过程中，TAP设备可以被当成本机的一个网卡，而操作TAP设备的应用程序相当于另外一台计算机，它通过read/write系统调用，和本机进行网络通信。TAP/TUN的数据传输过程如图2-3所示。

实际上，除了虚拟网卡的驱动，TAP/TUN驱动程序还包括一个字符设备驱动，内核通过字符设备/dev/net/tun与用户空间应用传递网络数据，同时，利用网卡驱动部分接收并发送来自TCP/IP协议栈的网络数据，或反过来将收到的网络数据传给协议栈处理。

![](images/1a1e811edc58fc6bb972f7e34defe8cf1e7db5449af45814bcb406065fb5c117.jpg)  
图2-3 TAP/TUN的数据传输过程

# 2.2 Linux Bridge

Linux Bridge（网桥）是工作在二层的虚拟网络设备，功能类似于物理的交换机。

对于普通的网络设备来说，只有两端，从一端进来的数据会从另一端出去，如物理网卡从外面物理网络收到的数据会转发给内核协议栈，而从内核协议栈过来的数据会转发到外面的物理网络中。而Bridge不同，Bridge有多个端口，数据可以从任何端口进来，进来之后从哪个端口出去要看MAC地址，和物理交换机的原理类似。

Bridge可以绑定其他Linux网络设备作为从设备，并将这些从设备虚拟化为端口，当一个从设备被绑定到Bridge上时，就相当于真实网络中的交换机端口插入了一个连接有终端的网线。

如图2-4所示，Bridge设备br0绑定了真实设备eth0与虚拟设备tap0/tap1。此时，对于Hypervisor的网络协议栈来说，只看得到br0，并不会关心桥接的细节。当这些从设备接收数据包时，会将其提交给br0决定数据包的去向，br0会根据MAC地址与端口的映射关系进行转发。

![](images/0278a9cb2a53d1e4ba759e3836f39484671cdf692c6e95e30a79d32fc804eb44.jpg)  
图2-4LinuxBridge结构

因为Bridge工作在第二层，所以绑定在br0上的从设备eth0、tap0与tap1均不

需要再设置 IP 地址，对上层路由器来说，它们都位于同一子网，因此只需为 br0 设置 IP 地址（Bridge 设备虽然工作于二层，但它只是 Linux 网络设备抽象的一种，能够设置 IP 地址也可以理解），比如 10.0.1.0/24。此时，eth0、tap0 与 tap1 均通过 br0 处于 10.0.1.0/24 网段。

因为具有自己的IP地址，br0可以被加入路由表，并利用它发送数据，而最终实际的发送过程则由某个从设备来完成。此时相当于Linux拥有了另外一个隐藏的虚拟网卡和Bridge相连，IP地址可以看成是这个网卡的，当有符合此IP地址的数据到达Bridge时，内核协议栈认为收到了一包目标为本机的数据，此时应用程序可以通过Socket接收它。

Bridge的实现有一个限制：当一个设备被绑定到Bridge上时，该设备的IP地址会变得无效，Linux不再使用该IP地址在三层接收数据。比如，如果eth0本来具有自己的IP地址192.168.1.1，在绑定到br0之后，它的IP地址会失效，用户程序不再能接收或发送到这个IP地址的数据，只有目的地址为br0 IP的数据包才会被Linux接收。

# 2.3 MACVTAP

传统的Linux网络虚拟化技术采用的是TAP+Bridge方式，将虚拟机连接到虚拟的TAP网卡，然后将TAP网卡绑定到Linux Bridge。这种解决方案实际上就是使用软件，用服务器的CPU模拟网络，但这种技术主要有三个缺点：

- 每台宿主机内都存在 Bridge 会使网络拓扑变得复杂，相当于增加了交换机的级联层数。  
- 同一宿主机上虚拟机之间的流量直接在 Bridge 完成交换，使流量监控、监管变得困难。  
- Bridge 是软件实现的二层交换技术，会加大服务器的负担。

针对云计算中的复杂网络问题，业界主要提出了两种技术标准进行扩展：802.1Qbg与802.1Qbh。802.1Qbh Bridge Port Extension 主要由 VMware 与 Cisco 提出，尝试从接入层到汇聚层提供一个完整的虚拟化网络解决方案，尽可能达到通过软件定义一个可控网络的目的。它扩展了传统的网络协议，因此需要新的网络设备支持，

成本较高。

802.1Qbg Edge Virtual Bridging（EVB）主要由 HP 等公司提出，尝试以较低成本利用现有设备改进软件模拟的网络。802.1Qbg 的一个核心概念是 VEPA，它通过端口汇聚和数据分类转发，把宿主机上原来由 CPU 和软件来做的网络处理工作转移到接入层交换机上，减轻宿主机的 CPU 负载。同时，使得在一级的交换机上做虚拟机网络流量监控成为可能。

为支持这种新的虚拟化网络技术，Linux引入了新的网络设备模型——MACVTAP，用来简化虚拟化环境下的桥接网络，代替传统的TAP+Bridge组合，同时支持新的虚拟化网络技术，如802.1Qbg。和TAP设备一样，每一个MACVTAP设备都拥有一个对应的Linux字符设备，因此能直接被KVM/QEMU使用，方便完成网络数据交换工作。

MACVTAP的实现基于传统的MACVLAN。MACVLAN允许在主机的一个网络接口上配置多个虚拟的网络接口，这些网络接口有自己独立的MAC地址，也可以配置IP地址进行通信。MACVLAN下的虚拟机或者容器和主机在同一个网段中，共享同一个广播域。MACVLAN和Bridge比较相似，但因为它省去了Bridge，所以配置和调试起来比较简单，而且效率也相对更高。

同一个物理网卡上的各个MACVTAP设备，都可以拥有属于自己的MAC地址和IP地址。使用MACVTAP，实现如图2-4所示的网络拓扑，管理员不再需要建立网桥br0，并且同时把物理网卡eth0、连接虚拟机的TAP设备tap0和tap1加入网桥br0中，而只需要在物理网卡eth0上建立两个MACVTAP设备，并让虚拟机直接使用这两个MACVTAP设备就可以了。

MACVTAP设备支持3种操作模式：

- VEPA模式：VEPA模式是默认模式。在这种模式下，两个在同一个物理网卡上的MACVTAP设备（都处于VEPA模式）通信，网络数据会从一个MACVTAP设备通过底层的物理网卡发往外界的交换机。此时，外界交换机必须支持Hairpin模式，只有这样才可以把网络数据重新送回物理网卡，传送给此物理网卡上的另一个MACVTAP设备。  
- 桥接模式：在桥接模式下，同一个物理网卡上的所有桥接模式的MACVTAP

设备直接两两互通，它们之间的通信，网络数据不会经过外界交换机。

- 私有模式：在私有模式时，类似于VEPA模式时外界交换机不支持Hairpin模式的情况。此时，同一个物理设备上的MACVTAP设备之间不能通信。

# 2.4 Open vSwitch

Open vSwitch 是一个具有产品级质量的虚拟交换机，它使用 C 语言进行开发，从而充分考虑了在不同虚拟化平台间的移植性，同时它遵循 Apache2.0 许可，因此对商用也非常友好。

如前所述，对于虚拟网络来说，交换设备的虚拟化是很关键的一环，vSwitch负责连接vNIC与物理网卡，同时也桥接同一物理Server内的各个vNIC。Linux Bridge已经能够很好地充当这样的角色，为什么还需要Open vSwith呢？

在传统数据中心中，网络管理员通过对交换机的端口进行一定的配置，可以很好地控制物理机的网络接入，完成网络隔离、流量监控、数据包分析、Qos 配置、流量优化等一系列工作。

但是在云环境中，仅凭物理交换机的支持，管理员无法区分被桥接的物理网卡上流淌的数据包属于哪个 VM、哪个 OS 及哪个用户，Open vSwitch 的引入则使云环境中虚拟网络的管理以及对网络状态和流量的监控变得容易。

比如，我们可以像配置物理交换机一样，将接入到Open vSwitch（Open vSwitch同样会在物理Server上创建一个或多个vSwitch供各个虚拟机接入）上的各个VM分配到不同的VLAN中实现网络的隔离。也可以在Open vSwitch端口上为VM配置Qos，同时Open vSwitch也支持包括NetFlow、sFlow很多标准的管理接口和协议，可以通过这些接口完成流量监控等工作。

此外，Open vSwitch 也提供了对 Open Flow 的支持，可以接受 Open Flow Controller 的管理。

总之，Open vSwitch 在云环境中的各种虚拟化平台上（比如 Xen 与 KVM）实现了分布式的虚拟交换机，一个物理 Server 上的 vSwitch 可以透明地与另一个 Server 上的 vSwitch 连接在一起，如图 2-5 所示。

![](images/e2c0a743fa6edb3c2d0bfe1998b8bf5c58971ae8d2b42a9456b5524226c1cf22.jpg)  
图2-5 Open vSwitch

而Open vSwitch软件本身，则由内核态的模块以及用户态的一系列后台程序组成，其结构如图2-6所示。

![](images/1694afef2f261c4d2c8a49e8de2bae430bf20b1622d9136365c94ff436a5f118.jpg)  
图2-6 Open vSwitch软件结构

其中 ovs-vswitchd 是最重要的模块，实现了虚拟机交换机的后台，负责与远程的 Controller 进行通信，例如通过 OpenFlow 协议与 OpenFlow Controller 通信，通过 sFlow 协议同 sFlow Trend 通信。此外，ovs-switchd 也负责同内核态模块通信，基于 netlink 机制下发具体的规则和动作到内核态的 datapath。datapath 负责执行数据交换，也就是把从接收端口收到的数据包在流表（Flow Table）中进行匹配，并执行匹配到的动作。每个 datapath 都和一个流表关联，当 datapath 接收数据后，会在流表中查找可以

匹配的Flow，执行对应的动作，比如转发数据到另外的端口。ovsdb-server是一个轻量级的数据库服务器，主要用来记录被ovs-switchd的配置信息。

Open vSwitch 还包括了一系列的命令行工具，主要包括：

- ovs-vsct1: 查询和更新 ovs-vsswitchd 的配置信息。  
- ovsdb-client: ovsdb-server 的客户端命令行工具。  
- ovs-appctl: 用来配置运行中的 Open vSwitch daemon。  
- ovs-dpctl: 用来配置内核模块中的 datapath。  
- ovs-ofctl: 通过 OpenFlow 协议查询和控制 OpenFlow 交换机和控制器。

# 2.5 Linux Network Namespace

Linux Namespace 提供了对系统资源的封装和隔离，处于不同 Namespace 的进程拥有独立的全局系统资源，改变一个 Namespace 中的系统资源只会影响当前 Namespace 里的进程，对其他 Namespace 中的进程没有影响。Linux 内核实现了多种不同类型的 Namespace，提供对不同类型资源的隔离。其中，Network Namespace 提供了对网络资源的隔离，每一个 Network Namespace 都拥有自己独立的网络栈、单独的网络设备、IP 地址和端口号、IP 路由表、防火墙规则、/proc/net 目录。

事实上，如果不考虑内存、CPU等其他共享的资源，仅从网络的角度来看，Network Namespace就和一台虚拟机一样，它可以在一台机器上模拟出多个完整的协议栈。如图2-7所示为Linux Nexwork Namespace结构。

![](images/ed311d802ff39ac92427fcbbd6bfffe69e96adb63938e4cdc8442a809e936cd3.jpg)  
图2-7Linux Network Namespace结构

每个新的 Network Namespace 都默认有一个本地回环 LO 接口，此外，所有的其他网络设备，包括物理/虚拟网络接口、网桥等，只能属于一个 Network Namespace，每个 Socket 也只能属于一个 Network Namespace。当新的 network namespace 被创建时，LO 接口默认是关闭的，需要自己手动启动。

创建 Network Namespace 也非常简单，使用 ip netns add 后面跟着要创建的 Namespace 名称，如果相同名字的 Namespace 已经存在，会产生“Cannot create namespace”的错误。

```txt
$ ip netns add ns1
# 查看所有的 network namespace
$ sudo ip netns list
ns1
```

ip netns 命令创建的 Network Namespace 会出现在 /var/run/netns/ 目录下，如果需要管理其他不是 ip netns 创建的 Network Namespace，只要在这个目录下创建一个指向对应 Network Namespace 文件的链接就可以。

ip命令提供了ip netns exec子命令，可以在对应的Network Namespace中执行，比如要看Network Namespace中有哪些网卡。

```txt
# 在 network namespace ns1 下运行命令
$ ip netns exec ns1 ip addr
1: lo: <LOOPBACK> mtu 65536 qdisc noop state DOWN group default qlen 1
link/loopback 00:00:00:00:00 brd 00:00:00:00:00
```

而且，要执行的可以是任何命令，不只是和网络相关（和网络无关的命令执行的结果和在外部执行没有区别）。例如在下面例子中，执行bash命令之后，后面所有的命令都是在这个Network Namespace中执行的，好处是不用每次执行命令都要把ip netns exec NAME补全，缺点是无法清楚地知道自己当前所在的shell，容易混淆。

```txt
# 在 network namespace ns1 下运行多条命令
$ ip netns exec ns1 bash
$ ip link set dev lo up
$ ip address
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN
group default qlen 1
link/loopback 00:00:00:00:00 brd 00:00:00:00:00
inet 127.0.0.1/8 scope host lo
valid_lft forever preferred_lft forever
```

```txt
inet6 ::1/128 scope host
valid_lft forever preferred_lft forever
$ ping 127.0.0.1 -c1
PING 127.0.0.1 (127.0.0.1) 56(84) bytes of data.
64 bytes from 127.0.0.1: icmp_seq=1 ttl=64 time=0.084 ms
--- 127.0.0.1 ping statistics ---
1 packets transmitted, 1 received, 0% packet loss, time 0ms
rtt min/avg/max/mdev = 0.084/0.084/0.084/0.000 ms
$ ip netns identify $$ #显示当前bash所在的Network Namespace
Ns1
$ exit 
```

有了不同的 Network Namespace 后，也就有了网络的隔离，但如果它们之间没有办法通信，也没有实际用处。要把两个网络连接起来，Linux 提供了 VETH pair。如前所述，可以把 VETH pair 当作双向的管道，从一端发送的网络数据，可以直接被另外一端接收到，也可以想象成两个 Namespace 直接通过一个特殊的虚拟网卡连接起来，可以直接通信。

可以使用ip link add type veth创建一对VETHpair，系统自动生成VETH0和VETH1两个网络接口，如果需要指定它们的名字，则可以使用ip link add vethfoo type veth peer name vethbar，此时创建出来的两个名字就是vethfoo和vethbar。需要记住的是VETHpair无法单独存在，删除其中一个，另一个也会自动消失。

```txt
# 创建 VETH 设备对 veth0/veth1
$ ip link add veth0 type veth peer name veth1
$ ip address
1: lo: <LOOPBACK> mtu 65536 qdisc noop state DOWN group default qlen
1
link/loopback 00:00:00:00:00 brd 00:00:00:00:00
2: veth1@veth0: <BROADCAST, MULTICAST, M-DOWN> mtu 1500 qdisc noop
state DOWN group default qlen 1000
link/ether fa:0e:cc:64:90:81 brd ff:ff:ff:ff:ff
3: veth0@veth1: <BROADCAST, MULTICAST, M-DOWN> mtu 1500 qdisc noop
state DOWN group default qlen 1000
link/ether 0a:c9:cc:b2:9b:c3 brd ff:ff:ff:ff:ff
# 为 VETH 设备对设置 IP 地址
$ ip address add 10.0.1.1/24 dev veth0
$ ip address add 10.0.1.2/24 dev veth1 
```

然后，可以把这对VETHpair分别放到创建的两个Namespace里，可以使用ip link set DEV nets NAME来实现。

```tcl
# 把设备 veth1 加入 Network Namespace ns中
$ ip link set veth1 netns ns1
$ ip netns exec ns1 ip link show veth1
6: veth1@if7: <BROADCAST,MULTICAST> mtu 1500 qdisc noop state DOWN
mode DEFAULT group default qlen 1000
link/ether fa:0e:cc:64:90:81 brd ff:ff:ff:ff:ff link-netsid 0 
```

如图2-8所示，创建两个Network Namespace，分别命名为net0与net1，同时创建了VETHpair对veth0与veth1，将它们分别加入net0与net1，将两个Network Namespace连接起来。

![](images/f4c4e5a40f6a7f13a5deac37edac922f2c1364c24f2edf0d8fc316a039daa3c8.jpg)  
图2-8 使用VETHpair连接两个NetworkNamespace

虽然 VETH pair 可以实现两个 Network Namespace 之间的通信，但是当多个 Namespace 需要通信的时候，就无能为力了。涉及多个网络设备之间的通信，首先想到的是交换机和路由器。因为这里要考虑的只是同一个网络，所以只用到交换机的功能，也就是前面所述的网桥。

和网桥有关的操作可以使用命令brctl，这个命令来自bridge-utils包。

```tcl
# 创建Bridge
$ brctl addbr br0
# 配置Bridge的IP地址
$ ip address add 192.168.0.1/24 dev br0
# 启动Bridge
$ ip link set dev br0 up
# 查看Bridge的IP地址
$ ip address show br0
8:br0:<BROADCAST,MULTICAST,UP,LOWER_UP>mtu 1500 qdisc noqueue
state UP group default qlen 1000
link/ether ae:96:a0:e7:a7:86 brd ff:ff:ff:ff:ff
inet 192.168.0.1/24 scope global mybridge
valid_lft forever preferred_lft forever
inet6 fe80::449:41ff:fe87:55bf/64 scope link
valid_lft forever preferred_lft forever 
```

然后可以创建 VETH pair，比如 veth0 与 veth1，并将一个 VETH 接口 veth0 加入 Network Namespace，另一个 VETH 接口 veth1 加入 Bridge。

```txt
# 添加 VETH 设备 veth1 到 br0 中
$ brctl addif br0 veth0
# 查看 Bridge 信息
$ brctl show
bridge name bridge id STP enabled interfaces
br0 8000.ae96a0e7a786 no veth1
```

```txt
# 从 veth0 设备发送数据
$ ping -I veth0 192.168.0.1
PING 192.168.0.1 (192.168.0.1) from 192.168.0.2 veth1: 56(84) bytes
of data.
```

```txt
$ 在新的命令行窗口中侦听br0中的数据,可以看到从 veth0 设备的 MAC 地址fa:0e:cc:64:90:81发送来的数据包
$ tcpdump -i br0 -elnv
tcpdump: listening on br0, link-type EN10MB (Ethernet), capture size 262144 bytes
11:33:44.577321 fa:0e:cc:64:90:81 > ff:ff:ff:ff:ff, ethertype ARP (0x0806), length 42: Ethernet (len 6), IPv4 (len 4), Request who-has 192.168.0.1 tell 192.168.0.2, length 28
```

如图2-9所示，创建三个Network Namespace分别为net0、net1与net2，同时创建有Bridge br0，以及各个Network Namespace与br0之间连接的VETH pair，br0将net0、net1与net2连接起来。

![](images/0a4b5946249c8b7305bd79c0a35884f2fde5476f20b5b0581b26c358a02d991c.jpg)  
图2-9 使用Bridge连接NetworkNamespace

# 2.6 iptables/NAT

网络地址转换（NAT，Network Address Translation）是一种在IP数据包通过路由器或防火墙时重写来源IP地址或目的IP地址的技术。这种技术被普遍使用在有多台主机通过一个公有IP地址访问外部网络的私有网络中，NAT也可以被称作IP伪装（IP Masquerading），可以分为目的地址转换（DNAT）和源地址转换（SNAT）两类。

DNAT 主要用在从公网访问内部私网的网络数据流中。比如从公网访问地址为 IP1 的公网 IP 地址，NAT 网关根据设定的 DNAT 规则，把 IP 数据报文包头内的目的 IP 地址 IP1 修改为内部的私网 IP 地址 192.168.1.10，从而把 IP 数据报文发送给地址为 192.168.1.10 的内部服务器。DNAT 可以用来将内部私网服务器上的服务暴露给公网使用。

SNAT 主要应用在从内部私网访问公网的网络数据流中。比如内部私网 IP 地址为 192.168.1.20 的机器想访问外部公网 IP 地址为 IP2 的服务，NAT 网管根据设定的 SNAT 规则，把 IP 数据报文包头内的源 IP 地址 192.168.1.20 修改为 NAT 网关自己的公网 IP 地址 IP1。这样内网中没有公网 IP 地址的机器也能访问外部公网中的服务了。

Linux 中的 NAT 功能一般通过 iptables 实现。iptables 是基于 Linux 内核的功能强大的防火墙。iptables/netfilter 在 2001 年加入到 2.4 内核中，netfilter 作为 iptables 内核底层的实现框架而存在，它们之间的关系如图 2-10 所示。

![](images/7b53d9ad69ba16392e46ff6f4d67faf698a3975c379918b7e92b2a2e492bfed0.jpg)  
图2-10 iptables与netfilter的关系

netfilter 提供了一整套对 hook 函数管理的机制，可以在数据包流经的 5 处关键地方（Hook 点），分别是 PREROUTING（路由前）、INPUT（数据包入口）、OUTPUT

（数据包出口）、FORWARD（数据包转发）、POSTROUTING（路由后），写入一定的规则对经过的数据包进行处理，规则一般的定义为“如果数据包头符合这样的条件，就这样处理数据包”。

可以说iptables/netfilter是按照规则来工作的，这些规则分别指定了源地址、目的地址、传输协议（如TCP、UDP、CMP）和服务类型（如HTP、FP和SMTP）等。数据包与规则匹配时，iptables就根据规则定义的方法处理这些数据包，比如放行、拒绝和丢弃等。配置防火墙的主要工作就是添加、修改和删除这些规则。

在每个关键点上，有很多已经按照优先级预先注册了的回调函数进行埋伏，设置的这些规则，就形成了一条链。INPUT规则链匹配目的地址是本机IP地址的数据报文，OUTPUT规则链匹配由本地进程发出的数据报文，FORWARD规则链匹配流经本机的数据报文，PREROUTING规则链用来实现目的地址转换DNAT，POSTROUTING规则链可以用来实现源地址转换SNAT。它们的工作流程如图2-11所示。

![](images/ce46814930198359705c9ca0cfe351c5ad6035d53e0418df27451266804d82a8.jpg)  
图2-11 iptables/netfilter的工作流程

防火墙为了达到“防火”的目的，就需要在内核中设置关卡，所有进出的报文都要通过这些关卡。经过检查后，符合放行条件的才能放行，符合阻拦条件的则需要被阻止。而这些关卡就是所谓的规则链。

每个“链”上都放置了一串规则，但是这些规则有些很相似，例如A类规则都

是对IP地址或者端口的过滤，B类规则是修改报文。此时能把实现相同功能的规则放在一起组成“表”。如此一来，不同功能的规则，可以放置在不同的表中进行管理。

如图2-12所示，iptables主要包含了FILTER、NAT和MANGLE三张常用表，分别负责数据包的过滤、网络地址转换及数据包内容的修改。

![](images/fa5fed6eb3075cdc54b529c0c72070a76e71bcbdff0ce3fdc98a1ac1f416769d.jpg)  
图2-12 iptables常用表

下面是一些iptables命令的使用示例：

目的地址转换，从网络设备ppp0来的所有TCP目的端口为81的数据包，进行DNAT，送往192.168.0.2

# 机器的 TCP 80 目的端口

$ iptables -t nat -A PRERouting -i ppp0 -p tcp --dport 81 -j DNAT --to 192.168.0.2:80

源地址转换，源地址为192.168.0.0/24网段来的IP数据包，进行SNAT，将其源地址改为本机公网IP

地址1.1.1.1

$ iptables -t nat -A POSTROUTING -s 192.168.0.0/24 -j SNAT --to 1.1.1.1

列出NAT表中规则

$ iptables -t nat -L -n
Chain PREROUTING (policy ACCEPT)
target prot opt source destination
DNAT tcp -- 0.0.0.0/0 0.0.0.0/0 tcp dpt:81
to:192.168.0.2:80

Chain INPUT (policy ACCEPT)  
target prot opt source destination  
Chain OUTPUT (policy ACCEPT)  
target prot opt source destination

```txt
Chain POSTROUTING (policy ACCEPT)
target prot opt source destination
SNAT all -- 192.168.0.0/24 0.0.0.0/0 to:1.1.1.1
# 删除SNAT规则
$ iptables -t nat -D POSTROUTING -s 192.168.0.0/24 -j SNAT --to
1.1.1.1
# 动态IP SNAT
$ iptables -t nat -A POSTROUTING -s 192.168.0.0/24 -j MASQUERADE 
```

# 2.7 虚拟网络隔离技术

随着网络虚拟化概念的引入，从可扩展性、安全性、可管理型性等多方面对网络隔离提出了新的要求。应对这些要求，可以使用两种不同类型的技术VLAN和隧道网络来实现。

# 2.7.1 虚拟局域网（VLAN）

LAN（Local Area Network，本地局域网）中的计算机通常使用Hub和Switch连接。一般来说，两台计算机连入同一个Hub或Switch时，它们就在同一个LAN中。一个LAN表示一个广播域，含义就是：LAN中的所有成员都会收到任意一个成员发出的广播包。

VLAN（Virtual LAN，虚拟局域网）表示一个带有 VLAN 功能的 Switch 能将自己的端口划分出多个 LAN。计算机发出的广播包可以被同一个 LAN 中的其他计算机收到，但位于其他 LAN 的计算机则无法收到。简单地说，VLAN 将一个交换机在逻辑上分成了多个交换机，限制了广播的范围，在二层将计算机隔离到不同的 VLAN 中。

比如说，有两组机器 Group A 和 B，我们希望 A 中的机器可以相互访问，B 中的机器也可以相互访问，但是 A 和 B 中的机器无法互相访问有两种方法。一种方法是使用两个交换机，A 和 B 分别接到一个交换机。另一种方法是使用一个带 VLAN 功能的交换机，将 A 和 B 中的机器分别放到不同的 VLAN 中。需要注意的是，VLAN 实现的只是二层的隔离，A 和 B 无法相互访问指的是二层广播包（比如 arp）无法跨越 VLAN 的边界，但在三层上（比如 IP 地址）是可以通过路由器让 A 和 B

互通的。

使用 VLAN，能够更好地控制广播风暴，提高网络整体的安全性，也能使网络管理更加简单直观。不同的 VLAN 由 VLAN tag（VID）标明，IEEE 802.1Q 规定了 VLAN tag 的格式。因此，在 Linux 上使用 VLAN，需要加载 $8021\mathrm{q}$ 的内核模块：

加载VLAN内核模块

$ modprobe 8021q

创建VLAN接口

$ ip link add link enol name enol.10 type vlan id 10

现在的交换机几乎都是支持VLAN的。交换机的端口通常有两种配置模式：Access和Trunk，如图2-13所示。

![](images/c79f718f9a8dd73aeedab76bc21c2d3b79855035d50c5512627c513e8d453655.jpg)  
图2-13 交换机Access端口和Trunk端口

其中，Access端口被打上了VLANtag，表明该端口属于哪个VLAN，Access口只能属于一个VLAN。Access端口都是直接与计算机网卡相连的，这样从该网卡出来的数据包流入Access端口后就被打上了所在的VLANtag。

Trunk 端口一般用于交换机之间的连接，可以允许多个 VLAN 通过，可以接收和发送多个 VLAN 的报文。如果划分了 VLAN，但是没有配置 Trunk，那么交换机之间的每个 VLAN 间通信都要通过一条线路来实现。

对Linux环境来说，如图2-14（a）所示，可以通过Linux Bridge、物理网卡等模拟交换机的VLAN环境。eth0是宿主机上的物理网卡，有一个命名为eth0.10的子设备与之相连。eth0.10就是VLAN设备，其VLAN ID就是VLAN 10。eth0.10挂在命名为brvlan10的Linux Bridge上，虚拟机VM1的虚拟网卡vent0也挂在brvlan10上。

这样配置的效果等同于宿主机用软件实现了一个虚拟交换机，上面定义了一个VLAN10，eth0.10和vnet0都分别接到VLAN10的Access端口上。而eth0就相当于一个Trunk端口，VM1通过vnet0发出来的数据包会被打上VLAN10的标签。

但是Linux在VLAN模拟上有一个不足，即需要多少个VLAN，就得创建多少个Bridge，Trunk端口也需要创建同样数量的类似eth0.10的虚口。这是由于Bridge的转发表没有VLAN tag的维度，要实现不同VLAN独立转发，只能使用多个Bridge实例实现转发表的隔离，如图2-14（b）所示。这里面，eth0.10的作用是定义了VLAN10，而brvlan10的作用是Bridge上的其他网络设备自动加入到VLAN10中。

![](images/38f94f38fb53decd7720e56c435c467675d2cb2864d9e33f8203b32fa097856c.jpg)  
(a)

![](images/c5cb1e679d27728c2bef9d15409715af8f1fdbea902350f456470fe89b1644b4.jpg)  
（b）  
图2-14 LinuxVLAN

# 2.7.2 虚拟局域网扩展（VxLAN）

VxLAN（Virtual Extensible Local Area Network，虚拟局域网扩展）是基于隧道（Tunnel）的一种网络虚拟化技术。隧道是一个虚拟的点对点的连接，提供了一条通路使封装的数据报文能够在这个通路上传输，并且在通路的两端分别对数据报文进行封装及解封装。某个协议的报文要想穿越IP网络在隧道中传输，必须要经过封装与解封装两个过程。隧道提供了一种某一特定网络技术的PDU穿过不具备该技术转发能力的网络的手段，如组播数据包穿过不支持组播的网络。

VxLAN将二层报文用三层协议进行封装，可以对二层网络在三层范围内进行扩展。如图2-15所示，把二层网络的整个数据帧封装在UDP报文中，送到VxLAN隧

道对端。隧道对端的虚拟或者物理网络设备再将其解封装，取出里面的二层网络数据帧发送给真正的目的节点。

![](images/4756e7540eccb9673a25c485661f6b6f163e959e7d6a61a471a122b60e17ccb0.jpg)  
图2-15 VxLAN

VxLAN协议头使用了24bit表示VLANID，可以支持1600多万个VLANID。RFC协议7348号中定义了VxLAN协议。

VxLAN 应用于数据中心内部，使虚拟机可以在互相连通的三层网络范围内迁移，而不需要改变 IP 地址和 MAC 地址，保证业务的连续性。

Linux 对 VxLAN 协议的支持时间并不是很久，直到 2012 年 Stephen Hemminger 才把相关的工作合并到内核中，并最终出现在 3.7.0 内核版本中，应该尽量使用新版本的内核，以免出现因为版本太低导致功能或者性能上的问题。

创建VxLAN接口

```txt
$ ip link add vxlan-10 type vxlan id 10 remote 10.239.12.13 dev eno1
$ ip -d link show vxlan-10 
```

```txt
122: vxlan-10: <BROADCAST, MULTICAST> mtu 1450 qdisc noop state DOWN mode DEFAULT group default qlen 1000 
```

```txt
link/ether 52:8a:79:c5:5d:d0 brd ff:ff:ff:ff:ff promiscuity 0  
vxlan id 10 remote 10.239.12.13 dev enol srcport 0 0 dstport 8472  
ageing 300 addrgenmode eui64 
```

启动VxLAN接口

```txt
$ ip link set dev vxlan-10 up 
```

向V1AN接口发送数据

```txt
$ ping -I vxlan-10 10.0.0.4 
```

```txt
在新的命令行窗口中侦听网卡 enol 中的数据，可以看到带有 VxLAN VNI 为 10 的 UDP 数据包
```

```txt
$ sudo tcpdump -i enol -elvn -T vxlan udp port 8472
tcpdump: listening on enol, link-type EN10MB (Ethernet), capture
size 262144 bytes 
```

```txt
19:07:17.681379 ec:a8:6b:f9:d4:2c > 48:0f:cf:3a:f1:dc, ethertype IPv4 (0x0800), length 92: (tos 0x0, ttl 64, id 18208, offset 0, flags [none], proto UDP (17), length 78) 
```

```txt
10.239.12.12.33697 > 10.239.12.13.8472: VXLAN, flags [I] (0x08), vni 10 
```

```txt
52:8a:79:c5:5d:d0 > ff:ff:ff:ff:ff, ethertype ARP (0x0806), length 42: Ethernet (len 6), IPv4 (len 4), Request who-has 10.0.0.4 tell 
```

10.239.12.12, length 28

# 2.7.3 通用路由封装GRE

GRE（RFC1701）也是基于隧道的一种网络虚拟化技术。与VxLAN相比，GRE使用的是IP报文而非UDP作为传输协议。同时，不同于VxLAN只能封装二层以太网数据帧，GRE可以封装多种不同的协议，包括IP报文（RFC2784，RFC2890）、ARP、以太网帧（NVGRE，RFC7637）等。

如图2-16所示，GRE包头中的type字段，可以指明被封装的数据包类型，例如IP报文（0x0800）、以太网帧（0x6558）等。同时，使用GRE包头中的key字段，可以区分不同虚拟网络的ID（类似于VxLAN的VNI和VLAN中的tag），从而达到隔离不同虚拟网络的目的。

![](images/834ed851e3a99aadb2e782e0324dc449600b6a6856d67d4cf9c0d5ef9aa74398.jpg)  
图2-16 GRE

如图2-17所示，在GRE隧道中，路由器会在封装数据包的IP头部指定要携带的协议，并建立到对端路由器的虚拟点对点连接。其中，Passenger协议表示要封装的乘客协议，比如IPX、AppleTalk、IP、IPSec、DVMRP等，Carrier协议表示封装乘客协议的GRE协议，插入Transport和Passenger协议之间，在GRE包头中定义了传输的协议，Transport协议表示IP协议携带了封装的乘客协议，这个传输协议通常实施在点对点的GRE连接中（GRE是无连接的）。

![](images/8b8a4446b644f97286fd84a179be0d49009e9e1fb5c57ca8fe5be3842c45150c.jpg)  
图2-17 GRE隧道

相比于VxLAN，GRE更加灵活，可以支持的协议也更多。但是目前物理网卡支持GRE协议的还不是很多，大部分GRE协议的处理还要依靠主机CPU，会增加CPU的负载。

```txt
# 创建GRE接口
$ ip tunnel add gre-10 local 10.239.12.12 remote 10.239.36.6 key
10
$ ip -d link show gre-10
125: gre-10@NONE: <POINTPOINT, NOARP> mtu 1472 qdisc noop state DOWN
mode DEFAULT group default qlen 1
link/gre 10.239.12.12 peer 10.239.36.6 promiscuity 0
gre remote 10.239.36.6 local 10.239.12.12 ttl inherit ikey 0.0.0.10
okey 0.0.0.10 addrgenmode eui64
# 显示当前所有的隧道
$ ip tunnel
gre0: gre/ip remote any local any ttl inherit nopmtudisc
gre-10: gre/ip remote 10.239.36.6 local 10.239.12.12 ttl inherit
key 10
# 启动GRE接口
$ ip link set dev gre-10 up
# 向GRE接口发送数据
$ ping -I gre-10 10.0.0.4
# 在新的命令行窗口中侦听网卡 enol 中的数据,可以看到带有 GRE key 为 10 的 IP 数
据包
$ tcpdump -i enol -elnv host 10.239.36.6
tcpdump: listening on enol, link-type EN10MB (Ethernet), capture
size 262144 bytes
11:13:56.899491 ec:a8:6b:f9:d4:2c > 00:00:0c:07:ac:0c, ethertype
IPv4 (0x0800), length 126: (tos 0x0, ttl 64, id 65465, offset 0, flags
[DF], proto GRE (47), length 112)
10.239.12.12 > 10.239.36.6: GREv0, Flags [key present], key=0xa,
proto IPv4 (0x0800), length 92
(tos 0x0, ttl 64, id 1152, offset 0, flags [DF], proto ICMP (1),
length 84)
10.239.12.12 > 10.10.10.4: ICMP echo request, id 21014, seq 101,
length 64
```

# 2.7.4 通用网络虚拟化封装（Geneve）

为了应对VLAN只能有4094的上限，利用隧道技术，产生了诸如VxLAN、NVGRE、STT（无状态传输隧道）等多种技术来实现虚拟网络的隔离要求。但是这类技术互相不能兼容，所以提出了通用网络虚拟化封装Geneve（Generic Network

Virtualization Encapsulation

Geneve技术的RFC正式标准还没产生，还处于IETF草案的阶段。Geneve主要的目的是适应虚拟网络技术的发展和隔离要求，定义一种通用的网络虚拟化隧道封装协议，能够尽可能地兼容目前的VxLAN、NVGRE等正式RFC标准的功能，并且提供高可扩展性来应对以后虚拟网络技术的发展。

Geneve 综合了 VxLAN 和 NVGRE 两者的特点，首先使用了 UDP 作为传输协议，同时吸收了 GRE 可以封装多种不同类型的数据包的优点。Geneve 封装以太网帧的案例如图 2-18 所示。

![](images/ba4bac0174f8a90f8bc08c25eddc2ef02f4ff8658438d2b3c536bb0fac07c2cb.jpg)  
图2-18GENEVE封装以太网帧的案例

Geneve包头中有一个24bit的VNI字段，可以用来指定不同的虚拟网络ID。同时，包头中也有type字段，用来指定封装的内部报文协议。

# 第3章

# 高性能数据平面

数据平面的性能在很大程度上取决于网络 I/O 的性能，而网络数据包从网卡到用户空间的应用程序需要经历多个阶段，如图 3-1 所示。

![](images/18962b55021c053d94922ef7be6901276b2785be6a0e0210cc072c0137ac9755.jpg)

![](images/5d4c29b16e9880ef2fa73a2f35b5718a92329f254a11903f47f71fd833dddefc.jpg)  
图3-1Linux网络数据包的处理流程

当数据包到达网卡后，通过DMA（DirectMemoryAccess）复制到主机的内存空间并触发中断，网络协议栈处理完数据分组后再交由用户空间的应用程序进行处理，整个过程的多个阶段都存在着不可忽视的开销，主要有以下几点。

# （1）网卡中断

轮询与中断是操作系统与硬件设备进行I/O通信的两种主要方式。在一般情况下，网络数据包的到来都是不可预测的，若采用轮询模式，则会造成很高的CPU负载，因此主流操作系统都会采用中断的方式来处理网络的请求。

然而，随着高速网络接口等技术的迅速发展，10 Gbit/s、40 Gbit/s 甚至 100 Gbit/s 的网络接口已经出现。随着网络 I/O 速率的不断提升，网卡面对大量的高速数据分组将会引发频繁的中断，每次中断都会引起一次上下文切换（从当前正在运行的进程切换到因等待网络数据而阻塞的进程），操作系统都需要保存和恢复相应的上下文，造成较高的时延，并引起吞吐量下降。

# （2）内存拷贝

为了使位于用户空间的应用程序能够处理网络数据，内核首先需要将收到的网络数据从内核空间拷贝到用户空间，同样，为了能够发送网络数据，也需要进行从用户空间到内核空间的数据拷贝。每次拷贝都会占用大量的CPU、内存带宽等资源，代价昂贵。

# （3）锁

在Linux内核的网络协议栈实现中，存在大量的共享资源访问。当多个进程需要对某一共享资源进行操作时，就需要通过锁的机制来保证数据的一致。然而，为共享资源上锁或者去锁的过程中通常需要几十纳秒。此外，锁的存在也降低了整个系统的并发性能。

# （4）缓存未命中

缓存能够有效提高系统性能，如果因不合理的设计而造成频繁的缓存未命中，会严重削弱数据平面的性能。以Intel XEON 5500为例，在L3（Last Level Cache）命中与未命中条件下，数据操作耗时相差数倍。如果在系统设计时忽视这一点，存在频繁的跨核调用，由此带来的缓存未命中会造成严重的数据读写时延，从而降低系统的整体性能。

接下来，针对这些开销因素，介绍一些主要的优化技术手段和项目。

# 3.1 高性能数据面基础

对于数据面的包处理而言，使用的主流硬件平台一般大致可分为硬件加速器、网络处理器及通用处理器。依据处理复杂度、成本、功耗等因素的不同，这些硬件平台在各自特定的领域发挥着作用。硬件加速器和网络处理器由于其专属性，往往具有高性能、低成本的优点。而通用处理器则在复杂多变的数据包处理上更有优势。同时，通用处理器由于性能的不断提升及其丰富的生态，为软件定义网络（SDN）提供了快速迭代的平台。

下面就通用处理器上的高性能数据包处理做一些介绍，包含高速数据面的软件开发技术，处理器平台上提供的有助于提升数据面处理性能的硬件特性。

# 3.1.1 内核旁路

在通用处理器上开发高性能数据处理应用，首先要考虑的问题是选择一个好的开发平台。现有的主流开发平台有两大类：一类是基于操作系统的内核；另一类是内核旁路方案，即绕过内核中的低效模块，直接操作硬件资源。在NFV应用中，从性能方面考虑，选择后者的居多。下面就内核的性能问题和内核旁路技术做一些介绍。

# 1. 内核的性能问题

在操作系统的设计中，内核通过硬件抽象和硬件隔离的方法，给上层应用程序的开发带来了简便性，但也导致了一些性能的下降。在网络方面，主要体现在整体吞吐率的减少和报文延迟的增加上。这种程度的性能下降对大多数场景来说可能不是问题，因为整体系统的瓶颈更多地会出现在业务处理逻辑和数据库上面。但对NFV这样的纯网络应用而言，内核的性能就有些捉襟见肘，性能优化显得很有必要。特别是随着网络硬件的发展，10G网卡是服务器的入门级配置，25G网卡正在普及，100G网卡和200G网卡也在应用中，内核所带来的性能下降是高速网络应用急需解决的问题。

数据包在内核中的处理如图3-2所示：左下是网络硬件（NIC），包括网卡传输链表（Descriptor Rings）和配置寄存器（CSRs）；左中是内核空间，包括网卡驱动（Driver）、协议栈（Stack）和系统调用（System Calls）；左上是用户空间，包括各种应用程序；右边是内核空间和用户空间的内存示意图。从中可以看出一个数据包从网卡到应用程序要经过内核中的驱动、协议栈处理，然后从内核的内存复制到用户空

间的内存中，加上系统调用要求的用户到内核空间的切换，都会导致内核性能的下降。

![](images/8e49f659f7df4effd44ad227ed8a0c305b67889ae7a10166a6c6c31466b8006e.jpg)  
图3-2 数据包在内核中的处理

# 2. 内核旁路技术

既然内核的性能不能满足NFV的要求，那么有没有一种方案能够克服这个问题呢？答案就是内核旁路技术，就是应用程序不通过内核直接操作硬件。

如图3-3（a）所示，应用程序在用户空间，而网络驱动在内核空间。每次网络操作的时候都需要从用户空间切换到内核空间。

在应用内核旁路技术之后，图3-3（a）就演变为图3-3（b），应用程序跨过内核直接和网络硬件通信，没有用户空间和内核空间之间的切换，提高了效率。把网络驱动从内核移到用户空间后，即使出问题也不会像之前在内核中那样使操作系统崩溃，这是内核旁路技术带来的另外一个好处。

![](images/f4723495d73f907403929f363fdda1edd82eed716b878aa1ba25752568dc36bf.jpg)

![](images/3a39402471b748c38e0e9ac22526884621b4999547350f3a46fccf8dd02e7466.jpg)  
图3-3 内核旁路技术

# 3. 开源方案

内核旁路之后，应用程序直接和硬件打交道，但也需要解决硬件的抽象接口、内存分配和CPU调度等问题，甚至还有网络协议栈的处理。这方面有DPDK、Netmap、OpenOnload及XDP等开源框架，在一定程度上起到了硬件抽象和隔离功能，简化了应用程序开发。

# （1）DPDK

DPDK 是一个全面的网络内核旁路解决方案，不仅支持众多的网卡类型，也有多种内存和 CPU 调度的优化方案。在 DPDK 之上还有 VPP、fstack 等网络应用和网络协议栈的实现。

# (2)Netmap

Netmap 是一个高效的收发报文的 I/O 框架，已经集成在 FreeBSD 的内部，也可以在 Linux 下编译使用。和 DPDK 不同的是，Netmap 并没有彻底地从内核接管网卡，而是采用一个巧妙的 Netmap ring 结构来从内核管理的网卡上直接接收和发送数据。

如图3-4所示，现代网卡一般都使用多个缓冲区（buffer），并有一个叫NIC ring的环形数组。这些缓冲区是操作系统和网卡硬件共享的，网卡将接收的网络数据放到这些缓冲区之后，操作系统能通过相应的mbufs指针读出，发包的流程则正好相反。

![](images/e76da3d9ddc5fd373cec7bbe6b5755ad7a07ba18df306d64f5874a1c9edfa95b.jpg)  
图3-4 传统的网卡缓冲区

如图3-5所示，Netmap把网卡的缓冲区从内核映射到用户空间，并且实现了自己的发送和接收报文的netmap_ring来对应网卡的NICring。现代网卡一般都支持多队列，每个队列对应着一个netmap_ring。

![](images/dee09afe912388dd6a17d36f700939f93890d0b750d2ccffef2e120ddfaf011f.jpg)  
图3-5Netmapring与NICring

将Netmap接口（netmap_if）绑定到网卡时，应用程序可以选择附加一个或多个Netmap ring。可以提高单个进程的吞吐量和灵活性；而如果只使用一个Netmap ring的话，则可以通过每个Netmap ring对应一个进程/CPU core的方式来构建多进程的高性能系统。

# （3）OpenOnload

OpenOnload 是一个开源的、高性能的 Linux 应用程序加速器，可为 TCP 和 UDP 应用提供更低的、可预测的延迟和更高的吞吐率。和 DPDK 与 Netmap 不同的是，前两者都是高性能的 I/O 框架，而 OpenOnload 更多的是一个内核旁路的协议栈。OpenOnload 在用户空间实现了 TCP 和 UDP 的协议处理，又通过和内核共享部分协议栈信息的方式较好地解决了应用程序的兼容性问题，在金融等领域应用较为广泛。OpenOnload 虽然是开源项目，但由于一些知识产权的限制，现在只能用在 Solarflare

及获得其许可的网卡上。

OpenOnload 的底层 I/O 主要通过 EF_VI 技术来实现。如图 3-6 所示，EF_VI 绕过内核协议栈把网卡中部分网络流量直接发送到用户空间的协议栈中。每个 EF_VI 实例可以访问一条特定的 RX 队列，RX 队列对内核是不可见的。在默认情况下，这个队列也不接收数据，直到创建一个 EF_VI “过滤器”把数据导入队列中。这个过滤器只是一个隐藏的流控制规则。用户用 ethtool 等常用工具看不到这个规则，但实际上它已经存在网卡中了。对于 EF_VI 来说，除了分配 RX 队列并且管理流控制规则，剩下的任务就是提供一个 API 让用户空间可以访问这个队列。

![](images/8f476ff77ee412d93aeef636327bb1748cc2fb4f112137102487f8f3c07022f1.jpg)  
图3-6 OpenOnload

另外一部分流量仍然保留在内核中进行处理，这种技术能够灵活地利用内核和旁路方案两方面的优势，在DPDK社区称为“分叉驱动”。要使用这种技术，需要一个支持多队列的网卡，同时也要支持流控制和SR-IOV。

有了这种网卡，可以实现如下功能：

正常启动网卡，让内核管理一切。  
- 创建一个SR-IOV中的虚拟网卡（VF）。  
- 把特定接收（RX）队列如1号加入VF中。  
通过流控制规则将一个特定的网络流引到1号RX队列中。

完成这些，剩下的步骤就是利用DPDK用户空间的API，从1号RX队列上接收数据包并处理。同时，其他任何队列在内核中的正常处理都不会受到任何影响。

（4）XDP

对于内核在 I/O 和协议栈两个方面的性能问题，内核的开发人员也有清楚的认

识，并提出了各种解决方案，XDP（eXpress Data Path）就是其中之一。XDP绕过了内核的协议栈部分，在继承内核的I/O部分的基础上，提供了介于原有内核和完整内核旁路之间的另一种选择。

如图3-7所示为XDP报文处理流程，中间部分是XDP的包处理引擎。这个引擎采用了一个BPF（Berkeley Packet Filter）的程序解释器，能够把XDP的业务逻辑从内核中隔离出来。即使XDP的业务代码出现错误，也不会导致内核的崩溃，达到了完整内核旁路技术类似的效果。内核的I/O部分接收报文之后，直接交给XDP，由XDP的业务逻辑决定报文的下一步是直接丢弃，是转发，还是本地处理。XDP绕过了内核原先的协议栈处理之后，性能得到较大的提高，是现在内核NFV高速网络处理方面一个不错的选择。

![](images/4239d637fbead0802128d5281912e5efe99d444614b82b3e86c200585f6d473f.jpg)  
图3-7 XDP报文处理流程

# 3.1.2 平台增强

在 IA（Intel Architecture）多核通用处理器的平台下，如何实现高速的网络包处理？对传统的操作系统而言，跨主机的网络通信都会涉及底层网卡驱动及网络协议栈处理。如前所述，不少内核旁路技术的诞生，为在通用处理器下实现高速网络处理提供了可能。除了软件的创新，IA 平台上的许多技术也可以被用来提高网络的处理能力，大致可以归纳为以下几个方面：

# 1. 多核及亲和性

多核处理器是指在一个处理器中集成两个或者多个完整的CPU核及计算引擎，它的出现使性能水平扩展成为可能。原本在单核上顺序执行的任务，可以按照逻辑划分为若干个子任务，分别在不同的核上并行执行。那么，按照什么策略将子任务分配到各个核上执行？这个分配工作一般是由操作系统按照复杂均衡的策略完成的。

利用CPU的亲和性能够使一个特定的任务在指定的核上尽量长时间地运行而不被迁移到其他处理器。在多核处理器上，每个核自己本身会缓存着任务使用的信息，而任务可能会被操作系统调度到其他核上。每个核之间的L1、L2缓存是非共享的，如果任务频繁地在各个核间进行切换，就需要不断地使原来核上的缓存失效，如此一来缓存命中率就低了。当绑定核后，任务就会一直在指定的核运行，大大增加了缓存的命中率。对网络包处理而言，显然可以提高吞吐量和降低延时。

# 2. Intel 数据直接 I/O 技术

Intel数据直接I/O（DataDirectI/O）技术简称DDIO，是从IntelXeonE5系列处理器开始引进的功能。如图3-8所示，DDIO技术能够支持以太网控制器将I/O流量直接传输到处理器高速缓存（LLC）中，缩短将其传输到系统内存的路线，从而降低功耗和I/O延迟。同时，DDIO不依赖外部设备并不需要任何软件的参与。

![](images/1c4ce9fdc80c8c01d0b5c5475b82c07e690bf445b4c61e44bde3a2c26f5933ca.jpg)  
图3-8 DDIO

在没有DDIO的系统中，来自以太网控制器的报文通过DMA最先进入处理器的

系统内存，当CPU核需要处理这个报文时，它会从内存中读取该报文至缓存，也就是说在CPU真正处理报文之前，就发生了内存的读和写。同样地，如果处理器发送一个报文，需要从内存中读取该报文并写入缓存，再将报文回写到内存中，之后通知以太网控制器通过DMA发送出去。

在具有DDIO的系统中，来自以太网控制器的报文直接传输至缓存，对于报文的数据处理来说，避免了多次的内存读写，在提高性能、降低延时的同时也降低了功耗。图3-9与图3-10对比了在网卡收发数据包时，在没有DDIO和有DDIO的系统中数据接收和发送的轨迹。

![](images/e1e6fe7c6833590046c2109db284442744e8c16c3756a3826b8c14ca1a6baa8c.jpg)

![](images/2bc9efc505ff7e1ed111062023093e73e0410445e39c982d95ec65801a238e02.jpg)  
图3-9 网卡接收数据包无DDIO对比有DDIO

![](images/83ffba90c114125f154cf60f5d10440fbf9440700e1acdae2cabd32a79ab796b.jpg)

![](images/ab16c6168ec9e0b81640bf2e2881d92146d6bcf1e445942781db0a26ce1f1092.jpg)  
图3-10 网卡发送数据包无DDIO对比有DDIO

# 3. 大页（Hugepage）

提到大页，有必要简短介绍一下内存地址的转换过程。处理器和操作系统在内存管理中采用受保护的虚拟地址模式，程序使用虚拟地址访问内存，而处理器在收到虚拟地址后，先通过分段机制映射转化为线性地址，然后线性地址通过分页机制映射转化为物理地址。对于Linux实现而言，只采用了分页机制，而没有用分段机制，这样虚拟地址和线性地址总是一致的。

分页机制是指把物理内存分成固定大小的块，按照页表管理，一般常规页的大小为4KB。以图3-10为例，如果按照常规页的大小，将线性地址映射为物理地址，需要读取至少三次页目录表和页表，也就是为了完成这个转换需要访问四次内存。为了加快处理器的内存地址转换过程，处理器在硬件上对页表做了缓存，就是TLB（Translation Look-aside Buffer），它存储了从线性地址到物理地址的直接映射。当处理器需要进行内存地址转换时，它先查找TLB，如果TLB命中，则无须多次访问页表就可以直接得到最终的物理地址，大大缩短了地址转换的时间。如果TLB不命中，则读取内存中的页表进行图3-11中的地址转换，如果在页表中都没找到索引，则产生缺页中断，重新分配物理内存，再进行地址转换。

![](images/19b56c9a5747bae2b5ff18dda6c3276801bcb2ecc140612be325608263aa1f5e.jpg)  
图3-11 从线性地址到物理地址转换（4KB页）

TLB是处理器内部的一个缓存资源，其容量是有限的，以Intel Skylake微架构为例，其4KB页的TLB的容量如表3-1所示。

表 3-1 Intel Skylake 的 TLB 容量 (4KB 页)  

<table><tr><td>Levels</td><td>Entries</td></tr><tr><td>Instruction</td><td>128</td></tr><tr><td>First Level Data</td><td>64</td></tr><tr><td>Second Level</td><td>1536</td></tr></table>

以普通4KB页为例，如果一个程序使用了2MB内存，也就是512个4KB的页，那么TLB中需要存有512个页表表项才能保证不会出现TLB不命中的情况。随着程序的变大或者程序内存使用的增加，TLB也就变得十分有限，导致TLB不命中的情况出现。

大页的出现改善了这一状态。大页，顾名思义，就是分页的基本单位变大，如图3-12和图3-13所示，可以采用2MB或者1GB的大页。它可以减少页表级数，也就是地址转换时访问内存的次数，同时减少TLB不命中的情况。一个使用了2MB内存的程序，TLB中只需要存有1个页表表项就能保证不会出现TLB不命中的情况。对于网络包处理程序，内存需要高频访问，在设计程序时，可以利用大页尽量独占内存防止内存溢出，提高TLB命中率。

![](images/070eb78f760dbce88e057db7cebc2dec6c4c224ffcb928d9f7cf1d505c3c9827.jpg)  
图3-12 从线性地址到物理地址转换（2MB页）

![](images/b0c98f7f18c72e5fe0f6719c5eba7b8ea6d4137655b97b561b3dc6cb6fdafca7.jpg)  
图3-13 从线性地址到物理地址转换（1GB页）

# 4. NUMA

在多核处理器平台中，有时需要将多个处理器像单一系统那样运转，则需要具备对多个处理器及其内存系统进行管理的模式。一般有两个模式：对称多处理（SMP）和非一致性内存访问（NUMA）。SMP模式将多个处理器、内存系统和I/O设备都通过一条总线连接起来。在SMP模式下，所有的硬件资源都是共享的，多个处理器之间没有区别、平等地访问内存和I/O外部设备，并且每个处理器访问内存的任何地址所需时间是相同的，因此SMP也被称为一致内存访问结构（UMA, Uniform Memory Access Architecture）。

很显然，SMP的缺点是扩展性有限，每一个共享的环节都可能造成系统扩展的瓶颈，而最受限制的则是内存。当内存访问达到饱和的时候，增加处理器并不能获得更高的性能，系统总线成为效率瓶颈；处理器与内存之间的通信延迟也增大。

NUMA（Non-Uniform Memory Access Architecture）即非一致性内存访问技术，它的基本特征是具有多个处理器模块（Node），每个处理器模块具有独立的本地内存、I/O设备等，处理器模块之间通过高速互联的接口连接起来。由于Node访问本地内存比访问其他节点的内存的速度要快一些，为了解决非一致性访问内存对性能的影响，NUMA调度器负责将进程尽量在同一节点的CPU之间调度，除非负载太高，才迁移到其他节点。

NUMA技术解决了SMP系统可扩展性问题，它已成为当今高性能服务器的主流体系结构之一。如图3-14所示为IntelXeon5500系列系统，2颗CPU支持NUMA的系统结构，每颗CPU物理上有4个核心。利用NUMA技术，在设计数据包处理程序时，在内存分配上使处理器尽量使用靠近其所在节点的内存，可以水平扩展包处理能力。

![](images/96736a837469c8084fead2ed541ddabd407c97fff59ecba2b6c18400bc0d65cd.jpg)  
图3-14IntelXeon5500系列系统

# 3.1.3 DPDK

DPDK的广泛应用很好地证明了IA多核处理器可以解决高性能数据包处理的需求。其核心思想可以归纳成以下几个方面：

轮询模式：DPDK 轮询网卡是否有网络报文的接收或放送，这样避免了传统网卡驱动的中断上下文的开销，当报文的吞吐量大的时候，性能及延时的改善十分明显。  
- 用户态驱动：DPDK 通过用户态驱动的开发框架在用户态操作设备及数据包，避免了不必要的用户态和内核态之间的数据拷贝和系统调用。同时，为开发者开拓了更广阔的天地，比如快速迭代及程序优化。  
- 降低访问存储开销：高性能数据包处理意味着处理器需要频繁访问数据包。显然降低访问存储开销可以有效地提高性能。DPDK使用大页降低TLB未命中率，保持缓存对齐避免处理器之间缓存交叉访问，利用预取等指令提高缓存的访问率等。  
- 亲和性和独占：利用线程的CPU亲和绑定的方式，将特定的线程指定在固定的核上工作，可以避免线程在不同核间频繁切换带来的开销，提高可扩展性，更好地达到并行处理提高吞吐量的目的。  
- 批处理：DPDK 使用批处理的概念，一次处理多个包，降低了一个包处理的平均开销。  
- 利用IA新硬件技术：IA的新指令、新特性都是DPDK挖掘数据包处理性能的源泉。比如利用vector指令并行处理多个报文，原子指令避免锁开销等。  
- 软件调优：软件调优散布在 DPDK 代码的各个角落，包括利用 threshold 的提高 PCI 带宽的使用率，避免 Cache Miss（缓存不命中）以及 Branch Mispredicts（分支错误预测）的发生等。  
- 充分挖掘外部设备潜能：以网卡为例，一些网卡的功能，例如RSS、Flow director、TSO等技术可以被用来加速网络的处理。比如RSS可以将包负载分担到不同的网卡队列上，DPDK多线程可以分别直接处理不同队列上的数据包。除以太网设备网卡以外，DPDK现已支持多种其他设备，例如cryptography设备，这些专用硬件可以被DPDK应用程序用来加速其网络处理。

# 1. 开发模型

基于上面的技术点，DPDK建议用户使用两种开发模型：

# Run-to-Completion模型

Run-to-Completion 模型指一个报文从收到、处理结束，再发送出去，都由一个核处理，一气呵成。该模型的初衷是避免核间通信带来的性能下降。如图 3-15 所示，在该模型下，每个执行单元在多核系统中分别运行在各自的逻辑核上，也就是多个核上执行一样的逻辑程序。为了可线性扩展吞吐量，可以利用网卡的硬件分流机制，如 RSS，把报文分配到不同的硬件网卡队列上，每个核针对不同的队列轮询，执行一样的逻辑程序，从而提高单位时间处理的网络量。

# Pipeline模型

虽然 Run-to-Completion 模型有许多优势，但是针对单个报文的处理始终集中在一个 CPU 核，无法利用其他 CPU 核，并且程序逻辑的耦合性太强，可扩展性有限。Pipeline 模型的引入正好弥补了这个缺点，它指报文处理像在流水线上一样经过多个执行单元。如图 3-16 所示，在该模型下，每个执行单元分别运行在不同的 CPU 核上，各个执行单元之间通过环形队列连接。这样的设计可以将报文的处理分为多步，将不同的工作交给不同的模块，使得代码的可扩展性更强。

![](images/cf3f65b7ae978d447cdbb3465f55922c799f760df193484ac818717ab9e5a9ec.jpg)  
图3-15 Run to Completion模型

![](images/203b75ae2e75a74cabdf13bbbbb81dfa6e5c87231bb206fa397261b3fc0c17ff.jpg)  
图3-16 Pipeline模型

# 2. 实现框架

DPDK由一系列可用于包处理的软件库组成，能够支持多种类型设备，包括以太网设备、加密设备、事件驱动设备等，这些设备以PMD（Polling Mode Driver）的形式存在于DPDK中，并提供了一系列用于硬件加速的软件接口。

- 核心库（Core Libraries）：这部分是DPDK程序的基础，它包括系统抽象

内存管理、无锁环、缓存池等。

- 流分类（Packet Classification）：支持精确匹配、最长匹配和通配符匹配，提供常用的包处理查表操作。  
- 软件加速库（Accelerated SW Libraries）：一些常用的包处理软件库的集合，比如 IP 分片、报文重组、排序等。  
- Stats：提供用于查询或通知统计数据的组件。  
- QoS：提供网络服务质量相关组件，比如限速（Meter）和调度（Scheduler）。  
- 数据包分组架构（Packet Framework）：提供了搭建复杂的多核 Pipeline 模型的基础组件。

接下来对核心库稍做展开。

# 3. 核心库

核心库是DPDK程序的核心也是基础，几乎所有基于DPDK开发的程序都依赖它。核心库包括系统抽象层、内存管理、无锁环、缓存池等。

系统抽象层屏蔽了各种特异环境，为开发者提供了一套统一的接口，包括DPDK的加载/启动；支持多进程和多线程；核亲和/绑定操作；系统内存的管理；总线的访问，设备的加载；CPU特性的抽象；跟踪及调试函数；中断的处理；Alarm处理。

除系统抽象层以外，无锁环、MemPool及Mbuf的管理也是DPDK的核心所在。

DPDK的rte_ring结构提供了一个支持多生产者和多消费者的无锁环。它是一个先进先出（FIFO）队列，简单且高速，支持成批进队列和出队列。它已用于Memory Pool的管理，同时也可以作为不同执行单元间的通信方式。其结构可以简单地表示为如图3-17所示的形式，生产者和消费者逐自进入各自的Head和Tail指针控制环中对象的移动（入队列，出队列）。

![](images/b3cb4bda279945eb4cc670b5512778beb337b2b00b8f56266140b15c5a481cf2.jpg)  
图3-17 rte_ring结构

DPDK 的 rte_mempool 是负责管理从内存中分配 mempool 的库。mempool 是一个对象池, 如图 3-18 所示, 池中的对象用 rte_ring 管理。在 mempool 中还引入了 Object Cache (对象缓存) 的概念, 用于加速对象的分配和释放过程。具体可参见 DPDK 的开发者手册。

![](images/46a3cf0b664754b4d064451581da447124a41540fda2e894b6ad9768bb2158b2.jpg)  
图3-18 mempool及其对象ring

DPDK的rte_mbuf则提供了一种数据结构，如图3-19所示，它可用于封装网络帧缓存或控制消息缓存。rte_mbuf以ring的形式存在于MemPool中，rte_mbuf就是mempool中的对象。mbuf的结构经过精心设计，其头部大小为两个CacheLine（缓存行），原则上将基础性的、频繁访问的数据放在第一个CacheLine，而将功能扩展性的数据放在第二个CacheLine。如图3-20所示，对于单个mbuf存放不下的大数据包，mbuf还有指向下一个mbuf结构的指针来形成帧链表的结构。

![](images/1181c0ae9f39662b13b9be4997c1b6f75094e3327f11397817fb58ce1b43ec3b.jpg)  
图3-19 rte_mbuf结构

![](images/4916d324345761a1a5cfbc460a0cd1bee4d11575df39fdfeebed2c215b99ffe5.jpg)  
图3-20mbuf链组成大数据包

# 4. 一个简单的DPDK程序

在DPDK代码的samples目录下有一个简单的实例，它实现了一个基于DPDK的简单转发的程序。main()函数是程序的入口，首先，argc和argv参数初始化系统抽象层（EAL，Environment Abstraction Layer）。

```c
int ret = rte_eal_init(args, argv);  
if (ret < 0)  
rte_exit(EXIT_FAILURE, "Error with EAL initialization\n"); 
```

然后，创建存有一定量mbuf的mempool。

```txt
mbuf_pool = rte_mempool_create("MBUF_pool", NUM_MBUFS * nb_ports, MBUF_SIZE, MBUF_CACHE_SIZE, sizeof(struct rte_pktmbuf_pool_private), rte_pktmbuf_pool_init, NULL, rte_pktmbuf_init, NULL, rte_SOCKET_id(), 0); 
```

接着，初始化每一个端口。

RTE ETH FOREACH DEV (portid) { if (port_init portid,mbuf_pool） $! = 0$ ）{ rte_exit(EXIT_FAILURE, "Cannot init port %"PRIu8"\n",portid); }

端口对应的是以太网设备，对一个以太网设备进行收发包前的初始化时，需要经

# 过以下几步：

- rte_eth_dev_config(): 基于应用所需的收发数据包队列数及一些特定的配置信息配置设备。  
- rte_eth_tx_queue_setup(): 创建发送队列，对于硬件设备，驱动需要为其分配DMA ring用于发送数据包。  
- rte_eth_rx_queue_setup(): 创建接收队列，对于硬件设备，驱动需要为其分配DMA ring用于接收数据包。  
- rte_eth_dev_start()：启动该设备，在启动后，该设备就可以用于收发数据包了。

static inline int port_init(void_t port, struct rte_mempool *mbuf_pool) { struct rte_eth_conf port_conf = port_conf_default; const uint16_t rx_rings = 1, tx_rings = 1; struct ether_addr addr; int retval; uint16_t q; if (!rte_eth_dev_is_valid_port.port()) return -1; /* 配置以太网设备. */ retval = rte_eth_devconfigure.port, rx_rings, tx_rings, &port_conf); if (retval != 0) return retval; /* 为每个端口分配和设置接收队列 */ for $(q = 0;q <   rx_{\cdot}$ rings; $q + + )$ { retval = rte_eth_rx_queue_setup.port, q, RX_RING_SIZE, rte_eth_dev_socket_id.port), NULL, mbuf_pool); if (retval < 0) return retval; } /* 为每个端口分配和设置发送队列.*/ for $(q = 0;q <   tx\_ rings;q + + )$ { retval = rte_eth_tx_queue_setup.port, q, TX_RING_SIZE,

rte_eth_dev_SOCKET_id.port),NULL);if(retval $<  0$ 1 return retval; $\}$ /\*启动端口. \*/retval $\equiv$ rte_eth_dev_start.port);if(retval $<  0$ 1 return retval; $/^{*}$ 使能以太网设备和混杂模式，允许接收所有数据包.\*/rte_eth_promiscuous_enable.port);return 0;

在端口初始化完成后，main函数为每个逻辑core启动执行转发程序。转发程序lcore_main()如下所示。可见，该执行程序从端口上通过rte_eth_rx_burst收到数据包后，再由rte_eth_tx_burst将数据包发送出去。

```c
static __attribute__(noreturn)) void   
lcore_main(void)   
{ uint16_t port; 
```

/*为达到最优性能，检查以确认端口所在的NUMA节点和轮询线程所运行的节点是一致的.*/

/\*执行程序，直到退出或被强制中止。\*/for（;）{/\*\*从一个端口接收数据包，并将它们从配对的端口转发出去.\*The mapping is $0\rightarrow 1$ ， $1\rightarrow 0$ ， $2\rightarrow 3$ ， $3\rightarrow 2$ ,etc.\*/RTE ETH FOREACH DEV (port）{/\*从配对端口的第一个端口接收一组突发的RX数据包.\*/structrte_mbuf\*bufs[BURST_SIZE];const uint16_t nb_rx $\equiv$ rte_eth_rx_burst port,0,bufs,BURST_SIZE);if(unlikely(nb_rx==0))

```c
continue;
/* 向配对端口的第二个端口发送一组突发的 TX 数据包. */
const uint16_t nb_tx = rte_eth_tx_burst.port^1, 0, buf,
nb_rx);
/* 释放所有未被发送的数据包所占的mbuf. */
if (unlikely(nb_tx < nb_rx)) {
    uint16_t buf;
    for (buf = nb_tx; buf < nb_rx; buf++)
        rte_pktmbuf_free(bufs[buf]);
}
```

# 3.2 NFV和NFC基础设施

如前所述，DPDK为高性能数据面的处理提供了可能。DPDK已作为NVF和NFC（网络功能虚拟化和容器化）的重要组件，参与到NFV和NFC的基础设施建设中。下面就从NFV、NFC和平台设备抽象两方面展开描述NFV和NFC基础设施的特质，以及DPDK对其支持的情况。

# 3.2.1 网络功能虚拟化

网络功能虚拟化的一个重要特征是软硬件解耦。当网络功能从专用硬件向通用硬件平台乃至虚拟通用硬件平台转移时，作为承载各种网络功能的基础设施层（NFVi），其重要性也越发突出。

当使用普通的服务器平台作为运行网络功能的目标平台时，每一个网络功能业务都希望通过基础设施层获得最大可能的网络带宽。基础设施层通常用PCIe网络设备将数据包引入通用处理器，当然，这样的PCIe网络设备在裸机上是物理设备，而在虚拟机上则是虚拟设备。

# 1. NFVi数据平面加速

对于一个VNF（虚拟化网络功能）应用，快速地从NFVi获取网络帧是后续业务逻辑的基础，这就涉及虚拟主机接口（HostI/O Interface）。从NFVi的视角来看，虚

拟主机接口是其面向虚拟主机提供的北向虚接口；从VNF的视角来看，虚拟主机接口是承载其运行的主机I/O设备。

配合不同类型虚拟主机接口，NFVi提供了不同的数据面策略，Bypass和Relay就是两种比较典型的数据面策略。从数据面的角度，前者依赖外部系统提供NFVi数据面，绕过了整个Host软件部分，后者则由Host软件提供NFVi数据面。

再回过头看虚拟主机接口。网络设备按照不同的虚拟化实现方式，可以粗略地分为全模拟（Fully-Emulated）、半虚拟化（Para-Virtualized）和硬直通（Pass-thru）。对于主流的VMM及其网络设备，DPDK支持相对都比较完善。全模拟和半虚拟化类型的虚拟主机接口主要与NFVi的Relay策略一起工作，而硬直通NFVi一般采用Bypass策略。如图3-21所示，E1000就是由VMM全模拟的设备接口，Virtio是QEMU/KVM下的半虚拟化设备，VF则是基于SR-IOV的功能，可用于硬直通。

![](images/0777b57859fe34844720b8965aba1cc7e463f246265bd49a5c8a85de78862a3e.jpg)  
图3-21 不同的虚拟主机接口实现方式

在网络功能虚拟化场景下，对网络带宽都有一定的要求。相对于全模拟设备方式，半虚拟化和硬直通是更为主流的使用方式。下面以QEMU/KVM开源VMM为例，分别介绍这两种方式的特点和优势。

# 2. 半虚拟化

Virtio是QEMU/KVM下的半虚拟化设备框架，支持多种设备类型，设备定义规范也以开源社区的方式维护。

virtio-net 是网卡设备类型的代表。该设备整体由 QEMU 模拟，例如当 Virtio-net 基于 PCIe 总线时，QEMU 通过 Trap-Emulation 的方式模拟了访问 PCIe Bus、PCIe CSR、BAR Registers、Interrupt 等行为。设备中传输的网络帧，本质是通过共享内存的方式，由虚拟主机内的前端设备驱动和后端设备双方按照队列规范进行入队列和出队列的操作。

QEMU 为 Virtio 后端设备的实现提供了一层 vHost 抽象，这样无论 QEMU 进程本身、Kernel，还是另外的独立进程，都可以有不同的适配。vHost-user 就是一个独立于 QEMU 进程的 Virtio 设备后端的实现接口，QEMU 进程和独立进程按照 vHost 协议通过 Unix Socket 互相交互。

如果给这些交互分类，可以分为与设备业务相关部分和无关部分。与设备相关的部分包括功能协商、设置多队列、MTU等。与设备业务无关的部分，主要是搭建前后端共享数据通道，即前端设备如何与后端设备共享内存并互相通知。在这部分工作完成之后，后端设备软件就可以向共享的队列里进行入队列和出队列的操作。

通过DPDK的vHost-user库，用户可以在自己的进程中轻松地与虚拟机进行网络帧的传输。在Open vSwitch中，DPDK模式的NETDEV（即OVS-DPDK）便是使用vHost-user构建其面向虚拟主机的虚接口。

由于 Virtio 的设备驱动默认后端是由软件在相同架构的 CPU 上进行数据入队列和出队列的操作，故而被归到半虚拟化设备中。而随着 Virtio 硬化的出现，这种分类方式的边界也会逐渐变得模糊，这在后续的章节中会继续展开。

总的来说，类似 Virtio 的这种半虚拟化设备有如下一些特点：

- 开放的标准化设备。  
相对较好的性能。  
- 硬件设备无关性。

由于该类虚拟主机接口需要接入软件定义的NFVi数据面，这样天然地将VNF与硬件设备解耦。另一方面，由于其设备完全由软件模拟，状态热迁移也更可实现。

在使用DPDK vHost后端实现对接QEMU的vHost-user后，其I/O性能提高了一个数量级，并可以随着CPU的数量线性提升，使得其在NFV场景下的可用性大大提升。

# 3. 硬直通

硬直通将一个硬件设备的能力直接赋予某一个虚拟主机，使得虚拟主机可以获得和裸机下极其相近的性能。但一台设备如果需要被赋予多个虚拟主机，就需要设备能够被切片，或者说能有设备及总线级别的多路复用技术。

这是为什么硬直通通常和SR-IOV联系在一起的原因，SR-IOV是一种PCIe总线多路复用技术。支持SR-IOV的设备，可以将自身切分成多个VF（Virtual Function），它们与PF（Physical Function）一样，在主机侧呈现为独立标准的PCIe设备。SR-IOV是一种总线虚拟化技术，或者多路复用技术，VF本身并不一定必须使用在虚拟人机里。真正将其与虚拟机结合的，是硬直通技术本身。

那硬直通的本质是什么呢？其主要解决了三部分的问题：设备BAR配置空间的访问、DMA内存请求的直达、中断请求的送达。第一部分有很多方法解决，比如trap-emulation的方式，或通过MMIO的页表映射。第二部分和第三部分需要平台特性的支持，这个特性就是IOMMU（比如Intel VT-d、AMD-Vi）。

IOMMU 支持 DMA 重映射和中断重映射。DMA 重映射支持一级甚至二级地址重映射，使得 DMA 请求可以使用虚拟 I/O 地址访问主存，不再要求 DMA 地址的物理连续性。中断重映射支持将设备中断重定向到 vCPU 的虚拟中断控制器。所以，可以说，硬直通是直接得益于平台 I/O 虚拟化技术的。

DPDK驱动对网卡设备的驱动支持非常全，支持SR-IOV的大多数厂商都提供了VF驱动，可以在厂商驱动目录下找到相关文件。

使用硬直通和SR-IOV技术的VF在VM中有一些特点：

- 几乎和裸机一致的性能。  
设备驱动对运行环境（VM或Bare-Metal）无感知。  
- 硬件设备依赖。

硬直通在带来出色性能的同时也引入了另一个课题，就是硬直通如何友好地支持云化。首当其冲的挑战是如何支持热迁移。

另一个课题由PF/VF模型引入，VF往往不被赋予一些改变设备全局设置的能力，它需要PF作为代理操作，这样就需要一个VF和PF之间的消息通道。VFD（VF Daemon）是由AT&T公司发起的一个开源项目，该项目给出了一种实现方式，统一抽象不同厂商可能共同面临的一个VF代理请求的工作，它的工作方式如图3-22所示。DPDK各驱动也对该方案提供全面的支持。

![](images/eb12659be957951f02126f501df6f8742e7795d99aadde4e2e1b4109b2b87cff.jpg)  
图3-22 VFD的工作方式

# 4.下一代虚拟主机接口

NFV剧烈地重构着网络形态，并依旧不断地生长着。从技术角度讲，有两个关键的因素驱使着各项技术发展，分别是更高的速度和更好的云化。这两个因素有时又是一对矛盾体，更高的性能往往需要更多的硬件亲和，而云化从某种角度又要淡化硬件的特性。

如果硬件规范是统一的，或能抽象统一在一个协定下，则是解决这对矛盾体的一种方式。先不说去除多样性本身是否好，现实中在业界要达成一致的抽象困难重重。

另一种方式，或许就是不同技术向着相同的方向各自演进、互相融合，最终产生新的满足下一代NFV要求的虚拟主机接口。

对于半虚拟化虚拟主机接口，其本身云化能力已经非常完善。所以，其改进方向主要集中在追求更好的网络带宽性能。以 Virtio 为例，最新的 v1.1 Spec 引入 Packed

Ring Layout, 主要是以减少内存访问次数为核心的优化方式。由于硬件通过总线访问内存延迟要远高于 CPU 访问内存，新 Layout 的引入同样也使得 Virtio 的 Ring 格式对于硬件访问更友好。

那最终 Virtio 是否可以像硬直通一样，由硬件设备直接向虚拟主机提供 I/O 呢？vDPA（vHost Data Path Acceleration）便是这样的实践。它并不改变 Virtio 设备模拟的特征，PCIe 总线、设备的 CSR、BAR 配置寄存器等依旧通过陷入模式的方式委托 vHost 处理。VDPA 加速 vHost 如图 3-23 所示。

![](images/32dfc6ff8772a286e953c59c772761b8d982ef93dd398cab6b88a776d4ef24c4.jpg)  
图3-23VDPA加速vHost

针对 vHost 的数据平面，利用之前提到的平台 I/O 虚拟化特性、IOMMU 的 DMA 和中断重映射，使得设备可以直接读写虚拟主机内存和投递中断。这样地址转换、帧 buffer 的复制及额外的中断中继开销至少可以进一步降低，比纯软件的零拷贝的中继效率更高。倘若硬件本身还能按照 Virtio 规范中定义的方式操作 Ring，则整个数据平面就完全地“硬直通”了。

虚拟主机接口数据平面的软硬之间无缝切换成为可能，硬件提供的是加速能力，而且被特定的硬件规范约束。当然，这里NFV仍旧对Virtio Spec有依赖，但对于NFVi是否使用特定的硬件已经没有了依赖。

那硬直通呢？半虚拟化大多是VMM原生的，硬直通具有跨VMM的一致性。硬直通虽然具有最接近裸机的性能，但也有硬件解耦性不够和云化关键特质热迁移能力的不足的问题。这个课题是否可解？答案也是肯定的，硬直通也在慢慢变得软化。从Linux中VFIO模块支持VFIO-PCI到VFIO-MDEV，一个明显的特征就是VF的PCIe的CSR和配置管理不一定需要和硬件一一对应，控制面可以由陷入模式的方式完成，是不是和半虚拟化很像？再进一步，硬直通的数据平面是否可以软化呢？一旦数据平面可以由软件提供，并接入NFVi在Host的数据平面，厂商硬件依赖的硬直通也完成了去设备硬件依赖的属性。

可以看到，虚拟主机接口正在以不同的路径，朝着更好的NFV奔向同一个目标——下一代虚拟主机接口。其特征是NFVi根据VNF的选择提供虚拟主机接口，双方遵照服务质量协议，NFVi运营商可以根据自身需要使用额外无缝硬件加速功能。

# 3.2.2 从虚拟机到容器的网络I/O虚拟化

相比于基于ASIC等专有硬件的网络功能，虚拟化技术将网络功能与底层硬件彻底解耦，不仅为开发人员提供了更加灵活的开发环境，为产品更新提供了更短的迭代周期，更保证了网络功能的高度隔离性、易用性及安全性。凭借这些优势，基于Hypervisor的虚拟化技术，比如KVM、HyperV，已广泛应用于NFV环境中。然而，近两年随着网络速度的不断上升、互联网数据量的不断膨胀，一种更加轻量级的虚拟化技术——容器虚拟化，正逐渐广泛应用于NFV场景中。

如图3-24所示，基于Hypervisor的虚拟化技术与容器虚拟化技术有一定区别。基于Hypervisor的虚拟化技术通过一层中间软件将固定的硬件资源抽象为众多的虚拟化资源（通常称为虚拟机）。每一个虚拟机都具有独立的操作系统，运行于完全独立的上下文中。因此，通常一台主机上运行有多个操作系统。而与完全模拟硬件资源的Hypervisor虚拟化不同，容器虚拟化是一种资源隔离技术。利用操作系统的命名空间和Cgroups资源分配，容器虚拟化将用户程序隔离于不同的资源实体中运行，而每一个资源实体就称为容器。在容器虚拟化中，同一主机上的所有容器共享主机操作系统，不对底层的硬件资源进行模拟。相比于虚拟机，容器是一种更加轻量级的虚拟化，能更高效地利用系统资源，有更快的启动时间，更易于部署和维护。

![](images/addc3431359813777ace80d36f9545261b056a07a0aa494b252b1b8688821e81.jpg)

![](images/7c8811172b5181f4d023f884736ee0e74039e01917e65149c50da02cf2ca8704.jpg)  
图3-24基于Hypervisor和基于容器的虚拟化技术的主要区别

# 1. 面向虚拟机的I/O加速方案

在基于Hypervisor的虚拟化中，I/O虚拟化主要包括两种方式，一种是基于SR-IOV的硬件虚拟化方案，另一种是基于Virtio的半虚拟化。针对这两种方式，DPDK提供了相应的两种用户态加速方案，分别为DPDK PF驱动、DPDK VF驱动和基于DPDK的软件交换机，如图3-25所示。

![](images/94ae0b4323ef639c3c9aae3eb853114b7ab1d571d343010fc7d6c404dcdd411c.jpg)

![](images/a60cd11b8fa62f703d581d00d0dfd902e1396903e45a20067955dbe088e09f15.jpg)  
图3-25 面向虚拟机的网络I/O加速方案

网卡的SR-IOV技术将一个网卡虚拟化成许多VF，并将VF暴露给虚拟机，使每个虚拟机都可以独享虚拟网卡资源，从而获得能够与本机性能媲美的I/O性能。为加速基于SR-IOV的网络，DPDK为其提供了相应的用户态的网卡驱动：PF驱动和VF驱动。通过在虚拟机中使用用户态的VF驱动，虚拟机就可以实现更高效的网络I/O性能。

Virtio是一种半虚拟化技术的通信协议规范，已经广泛应用于虚拟化环境中。在

Virtio环境中，前端的 Virtio 驱动和后端的 vHost 设备互联，利用主机端的虚拟交换机实现网络通信。为加速基于 Virtio 的网络，DPDK 为前端的 Virtio PCI 设备提供了用户态驱动（Virtio Polling Mode Driver），并且支持了用户态的 vHost 设备——vHost-user。通过在虚拟机中使用 Virtio PMD 和软件交换机中使用 vHost-user，可以大幅提高虚拟机的网络 I/O 性能。

# 2. 面向容器的网络I/O加速方案

Linux 为容器提供了十分丰富的网络 I/O 方案，如主机网络和 Docker 默认使用的网桥，然而基于内核的网络 I/O 方案在性能上往往无法满足追求高吞吐、低延迟的 NFV 业务的需求。并且，因为虚拟机和容器是两种完全不同的虚拟化技术，因此面向虚拟机的网络 I/O 加速方案无法直接应用于容器网络。此外，现有大部分的基于容器的应用都运行于 Kubernetes 环境下，这就要求 I/O 加速方案也能同时支持 Kubernetes 运行环境。

为了增强容器网络I/O的性能，Intel为Docker容器也提出了与虚拟化I/O加速方案类似的两个I/O加速方案：SR-IOV网络插件和Virtio-user，如图3-26所示。

![](images/287727bf0e1b5ebdd6215f44a31d7f6724260c1aaf46f7fe9160d2a2c634d5d2.jpg)

![](images/af844cd5647ee88429c7243db2206d0ccb394ba015627e42da926ba60b06961d.jpg)  
图3-26 面向容器的网络I/O加速方案

SR-IOV网络插件将网卡的SR-IOV技术应用到容器中，通过将网卡VF加入容器的网络命名空间，容器运行时可以直接看到网卡。利用DPDK的用户态VF驱动，容器运行时可以实现高速的网络收发包。Virtio-user是DPDK提出的一种遵从Virtio规范的用户态虚拟设备。与QEMU模拟的Virtio-PCI设备相同的是，Virtio-user同样是Virtio的前端设备，可以和任何vHost后端设备，如DPDK vHost-user和Linux Kernel的vHost-net进行通信。然而，与虚拟机使用的Virtio PCI设备不同的是，Virtio-user

是为容器量身定制的，并负责网络I/O。在使用Virtio-user的软件交换机方案中，每个容器运行时都有一个Virtio-user设备，此设备将DPDK使用的大页内存共享给后端的软件交换机；在共享的内存空间中创建Virtio Ring结构，并按照Virtio规范定义的Ring操作方式实现通信。

为支持更加灵活、用户自定义的网络模型，Kubernetes 提供了符合 Container Network Interface（CNI）容器网络规范的网络插件接口。为了将 SR-IOV 和 Virtio-user 应用到基于 Kubernetes 的容器环境中，Intel 为两种 I/O 加速方案分别提供了 SR-IOV CNI 插件和 vHost-user CNI 插件。

# 3.2.3 NFVi平台设备抽象

NFVi需要为VM提供比较好的性能，所以其本身对性能的要求就更高。基于DPDK优化的NFVi是业内主流的方式。NFVi南向接口需要直接面对主机硬件，利用DPDK的硬件抽象框架，能帮助NFVi软件减少不同硬件带来的差异性。

如图3-27所示，DPDK提供多种设备抽象并在不断扩展，已被支持的设备包括ETHERDEV、EVENTDEV、CRYPTODEV、RAWDEV、BBDEV、SECURITYDEV等。这些设备大多面向功能进行设备抽象，其支持的硬件从功能固定的网卡、加解密加速卡，到可编程的Smart-NIC、FPGA，甚至面向无线Base Band的设备都提供了支持。而很多抽象设备都有基于CPU ISA的实现，比如使用AES指令实现的CRYPTODEV实例和OPDL库实现的EVENTDEV。

![](images/8e6cd48b4a94ba7c2987d0625f8d750a69fc1ff15224545a6c76269b33a188e3.jpg)  
图3-27 DPDK支持不同功能硬件加速

# （1）CRYPOTDEV

CRYPTODEV是对加解密设备的抽象，提供一组API，加解密请求被封装在一组Context中，由DPDK应用实例向加解密设备发起加解密的请求。CPU在对称加解密处理上拥有不错的性能，所以CRYPTODEV除支持多厂商的PMD外，还有多个软件实现的加解密设备。

另外，DPDK还为CRYPTODEV提供了一种SCHEDULERPMD，如图3-28所示，它可以根据不同的策略将加解密请求调度到多个加解密设备上。该机制为多设备聚合提供了可能，也会异构加解密（CPU和加解密设备）加速提供了软件框架。

![](images/66441bc8434726b1b592b49daf6781185665ddc3207e4466d604bcf9fb4cc19b.jpg)  
图3-28 CRYPTODEV设计示意图

CRYPTODEV在NFV的一种应用场景是IPSec，DPDK提供了IPSecGW的示例，也在FD.IO、VPP中提供了基于CRYPTODEV的IPSec实现。

# （2）EVENTDEV

EVENTDEV是一种抽象事件设备，其主要作用是为DPDK应用提供事件调度设备接口，便于应用采用事件驱动模型编程，而不需要理解调度设备的具体实现细节。

如图3-29所示，一个EVENTDEV设备提供了3个队列和6个端口，将6个CPU以Pipeline的方式串联起来。其中，Q1和Q2按照Atomic的机制分发调度事件，Q3出口对应单端口，所以不构成调度。通过配置API可以将队列和端口关联起来。在运行时，对于每一个CPU，可以通过端口ID进行事件的入队列和出队列操作。

![](images/67f3df18aa57c4a5c8e6647ce490ef2d5ffcf3846eaf78d5a1e42a8a0549c3d3.jpg)  
图3-29EVERNTDEV应用示意图

EVENTDEV支持3种队列调度类型，分别是Atomic、Ordered及Parallel。Atomic调度类型保证数据流的原子性，同一时间不会将同一条流调度到多个CPU。Ordered调度类型允许将数据流调度到多个CPU，但保证多个CPU下游出队列时，必须按照流分发的次序出队列。Parallel调度类型则允许将数据流调度到多个CPU，但不提供数据流的次序完整性。

可以看到，采用EVENTDEV方式编程，可以非常方便地设计需要水平扩展的NFVi数据面。

# (3) RAWDEV

RAWDEV 为在 DPDK 中还未有该设备抽象的设备提供了一个选项。以 Intel 支持 PR（Partial Re-Configuration）的 FPGA 设备为例，该设备呈现为一个普通 PCIe 设备，设备驱动可以对 FPGA 部分运算资源（AFU）进行运行中的功能加载。

如图3-30所示，在DPDK中，在RAWDEV下实现了一个该设备的驱动，为应用提供加载FPGA逻辑的API。该设备驱动将可用AFU设备挂到一个IFPGA的总线上，注册在该总线上的FPGA逻辑驱动将会对总线上的设备进行驱动绑定，并最终向DPDK框架呈现相对应的抽象设备接口（例如ETHERDEV、CRYPTODEV等）。

![](images/c8b0097dab62b6a616bada5ad218cb50bbf0c16c27fecdba8f4a7d2e5d945bff.jpg)  
图3-30 RAWDEV设计框架

可以看到RAWDEV为DPDK设备框架引入了灵活性，当某设备功能还不具有普适性时，仍可以通过该方式得到开源社区的支持，在更多厂商有类似诉求并达成共识后，可以单独抽象出来成为新的设备抽象类型。

# （4）Representor端口

软件方案实现的Virtual Switch如OVS已经被很多云服务提供商接受，随着网络带宽要求的进一步升级，很多厂商也在尝试使用硬件加速Virtual Switch来提升虚拟机网络性能。随着NIC内嵌的交换功能变得越来越强大，越来越多的功能（比如ACL、tunnel）被引入NIC，传统的ethdev设备模型不能完全涵盖这些硬件的特性，所以Kernel和DPDK都提出了switchdev的概念。

内嵌Switch功能的网卡通过SR-IOV方式暴露出多个端口，用户把VirtualFunction直接分配（pass-through）给虚拟机，设备交由VM直接访问。在host上，可以通过PF内核驱动对VF做管理配置，例如需要更改某个VF的MAC地址，可以利用命令：

ip link set eth0 vfv 0 mac 00:1E:67:65:93:01

这里使用ip命令实现这个配置，用PFinterface $^+$ VFindex标志一个VF设备。对于内嵌Switchoffload功能的NIC，ip等命令提供的配置能力是有限的，我们希望依然用丰富的Switch配置接口对VF端口、流表进行配置。因此，在硬件加速VirtualSwitch的场景下，我们需要为VF提供端口，既为了通过端口下发配置，也为了从这些端口中接受Switch快速路径处理不了的报文。

在DPDK中，为了通过PF对VF进行配置，需要给应用呈现一个VF的端口，通过EthdevAPI对VF端口做管理控制，比如设置MAC/VLAN，通过rte_flowlib配置转发流表等。如图3-31所示，在DPDK中，Representor端品是PF驱动初始化时创建出来的以太网端口，它给应用程序提供了一个标准的端口视角。每一个ethdev都用一个Switchinfo属性，用来标志此端口属于哪一个Switchdev。rte_ether_dev_info_get API可以获取device的flag，通过检查RTE_ETH_DEV REPRESENTORbit判断这个端口是否为Representor端口。

![](images/df2a8724a903b14f587f9b03443aed6d5262b6a694fddfe92dc0f94835815804.jpg)  
图3-31 Representative端口

对于希望创建Representor端口的PF设备，用户通过参数列表指定需要管理的VF，例如需要对某个PCI设备的第0，2，4，5，6，7个VF创建Representor端口，可以给DPDKeal设置如下格式的参数：

```yaml
-wpci:D:B:D.F,representor=[0,2,4-7] 
```

ethdev lib 提供了设备参数解析 API: rte_eth_devargs.parse 来帮助 driver 解析参数。

PF driver 根据用户的需求决定为哪些 VF 创建 Representor 端口，它们基于 PF 的端口而存在，也就是说当 PF 的端口被销毁时，其所有相关的 Representor 端口也将随之被销毁。这里以 i40e 为例，讨论 Representor 端口在 PF driver 中的实现。

在PF（DriverProbe）驱动探测的时候，会用rte_eth_devargsParseAPI解析设备参数，如果用户设置了Representor端口参数，PF驱动不仅创建自己的ethdev，还会为用户指定的VF分别创建ethdev，并将这些ethdev归入同一个SwitchDomain，如图3-32所示。PF驱动会在VF的ethdev中注册一组eth_devOps，App就可以通过标准的ethdevAPI配置VF,i40e实现了很丰富的配置接口，例如promiscuousmode设置、macaddr配置、vlanfilter等。

![](images/f30836e325ebb1aa7ccc6c6fd0b14009e3830ec70fabf6e50c5b19ca79f3f2e7.jpg)  
图3-32DPDK中Representor端口的实现

Representor 端口和一般的以太网端口不同的是，由于它主要用来做管理配置，因此可以不实现收发包函数。如果需要处理 Exception Path 的流量，则需要实现相应的收发包函数接收来自对应 VF 的异常报文。

通过VFRepresentor端口调用的EthernetAPI最终都是通过PFdriver实现的，Representor端口的作用就是封装了PF对VF的配置函数，使得用户可以通过标准的ethdevAPI对VF进行管理。

# 3.3 OVS-DPDK

Open vSwitch（OVS）是一个产品级质量的多层虚拟交换机，基于Apache2.0许可。OVS的设计初衷是支持可编程自动化网络大规模部署及拓展，能够支持标准网络管理接口和协议，如NetFlow、sFlow、IPFIX、RSPAN、CLI、LACP、802.1ag等。同时，它还需要支持与其他现有虚拟交换方案的混合部署，例如VMware公司的vNetwork、思科公司的Nexus1000V等。

由于丰富的功能和优秀的稳定性，OVS在网络部署中得到了广泛的应用。源于近些年云计算的快速发展，很多云服务提供商将OVS用做大量虚拟机对外的快速数据通道，基于OVS发行版进行了功能添加和优化，一些基于硬件加速的OVS方案在近些年也得到了广泛的关注。

OVS基本功能包括如下4大方面。

- 自动化控制：OVS支持OpenFlow，用户可以通过ovs-ofctl使用OpenFlow协议连接交换机实现查询和控制。  
- QoS：支持拥塞管理和流量整形。  
安全：支持VLAN隔离、流量过滤等功能，保证了虚拟网络的安全性。  
- 监控：支持Netflow、SFlow、SPAN、RSPAN等网络监控技术。

# 3.3.1 OVS-DPDK 概述

# 1. OVS-DPDK 基本原理

最初的OVS版本通过Linux内核数据通道进行数据分发。然而，用户能够得到的最终吞吐量受限于Linux网络协议栈的性能。DPDK作为用户态的高性能网络数据

处理库，通过提供一系列的Poll Mode驱动，能够绕过Kernel的性能瓶颈，在物理网卡和用户态之间提供高速数据传输服务。通过将DPDK集成到OVS中，交换机的快速数据路径被切换到用户态，OVS-DPDK原理如图3-33所示。

![](images/6f796afd773301f3cfdcf0094f14d52891d066dbb34ba7d5ff4554d575aca4d3.jpg)  
图3-33 OVS-DPDK原理

如图3-34所示为OVS-DPDK组件示意图。OVS的交换端口通过netdev表示，在OVS-DPDK中，netdev-dpdk是DPDK加速过的I/O端口，包括物理网卡librte_eth（DPDK支持市面上各种主流网卡，由对应的PMD进行驱动）、虚拟接口librte_vhost（DPDK版的用户态vHost实现，可工作在服务器端和客户端为虚拟机和宿主机之间提供高速数据传输通道）、dpif-netdev（提供了用户态的转发功能）、ofproto（OpenFlow交换机的具体实现部分）、ovsdb-server（用来维护OVS的交换流表配置并负责与上层的SDN控制器的通信）。

![](images/940b8e4c32d082776caf290678897434211db9c955a3dcb1e1e6df97bee5a8e4.jpg)  
图3-34 OVS-DPDK组件示意图

# 2. OVS-DPDK 版本及功能

因为OVS-DPDK是基于DPDK提供的库，当DPDK的API有所变化时，OVS也需要做出相应的修改才能适配，所以对OVS和DPDK的版本有一定要求来保证兼容性。OVS-DPDK的很多新功能也依赖于DPDK新功能的添加。因此，建议使用OVS官网推荐的DPDK版本进行编译。

表3-2列出了主流OVS版本对DPDK的版本支持及新增的功能特性。

表 3-2 主流 OVS 版本对 DPDK 的版本支持及新增的功能特性  

<table><tr><td>版本</td><td></td></tr><tr><td>2.9</td><td>·支持 DPDK 17.11
·增加命令 dpif-netdev/pmd-rxq-show 查看每个 PMD 线程的使用率
·增加批处理发包功能
·支持 vHost IOMMU():一旦启用 vHost IOMMU,虚拟机中的 viOMMU 将保证 vHost 只能访问 Virtio 设备允许访问的内存区域。注意,Qemu 2.7~2.9 中配合使用该功能,将不能使用 Virtio 多队列,建议使用 Qemu 2.10 及以上版本。Qemu 中模拟 IOMMU 参考命令如下:
-device intel-iommu,device-iotlb=on,intremap=on</td></tr><tr><td>2.9</td><td>因为 viOMMU 的存在,在虚拟机中,可以将 Virtio 设备绑定到vfio-pci 使用
·增加 vhost-dequeue-zerocopy 支持(试验版)
·增加 dpif-netdev/pmd-rxq-rebalance 进行 CPU 核与rxq 的动态绑定:使用该命令,当提供多个CPU核的时候,系统将根据每个 PMD 线程的工作负载压力,动态地调节CPU核与pmd 线程的绑定关系,使运算负载尽可能均匀地分摊到每个CPU核上,参考命令如下:
$ ovs-appctl dpif-netdev/pmd-rxq-rebalance</td></tr><tr><td>2.8</td><td>·支持 DPDK 17.05.1
·将 DPDK 的日志信息转到 OVS 的默认日志系统
·将 dpdkvhostuser 端口提示为不建议使用</td></tr><tr><td>2.7</td><td>·支持 DPDK 16.11
·支持新参数'n_rxq_desc' and 'n_txq_desc' 配置 DPDK 收发端口的描述符数目
·支持 RX 校验计算卸载: OVS-DPDK 会自动检测网卡是否支持接收端校验计算卸载,如网卡支持,将自动启用。注意,此功能仅适用于硬件网卡,不适用于 vHost 端口
·需要被 OVS 使用的网卡,必须给 dpdk-devargs 参数显式指定网卡的 PCI 地址,参考命令格式如下:
$ ovs-vsctl set Interface dpdk0 type=dpdk options:dpdk-devargs=0000:81:00.0
·支持 DPDK 虚拟设备驱动 (vdev)</td></tr><tr><td>2.6</td><td>- 支持 DPDK 16.07 - 参数 'other_config:pmd-rxq-affinity' 用来显式指定 DPDK 每个 rxq-pmd 线程和 CPU 核的 绑定关系,参考命令如下： \$ ovs-vsctl set Interface\\ other_config:pmd-rxq-affinity=rxq-affinity-list&gt; 可通过命令 dpif-netdev/pmd-rxq-show 查看 rxq 和 core 的绑定关系 支持巨帧 (Jumbo frame) 支持 vHost 客户端模式并支持 vHost 端重连(需配合 Qemu 2.7 以上版本) 去除了 dpdkvhostcuse 端口</td></tr><tr><td>2.5</td><td>支持 DPDK 2.2 支持 vHost-user 端口的多队列 (需配 Qemu 2.5 以上版本)</td></tr></table>

# 3. OVS-DPDK 基本配置方法

下面将以OVS2.9.0版本为例，介绍OVS-DPDK配置中的常用命令，方便读者参考使用。

（1）编译OVS-DPDK

将DPDK加入OVS的编译配置路径

```txt
./boot.sh
./configure --with-dpdk=$DPDK Builds 
```

OVS会使用多个默认文件夹存放OVS系统文件，如果想自定义配置OVS文件路径，可添加如下命令。

```shell
--prefix=/usr --localstatedir=/var --sysconfdir=/etc 
```

编译。

```txt
make 
```

（2）配置OVS

包括建立 OVS-DB，启动 vSwitchd 等，示例如下。

```shell
$ export PATH=$PATH:/usr/local/share/openvswitch/scripts
$ export DB_SOCKET=/usr/local/var/run/openvswitch/db.sock
$ ovs-vsctl --no-wait set Open_vSwitch
other_config:dpdk-init=true
$ ovs-ctl --no-ovsdb-server --db-sock="$DB_SOCKET" start 
```

启动 OVS-DPDK 特有的命令通过参数 other_config 进行配置，包括 DPDK 需要的 CPU、内存、vHost 建立连接的 Socket 文件路径等信息。

与DPDK相关的重要other_config参数如下所示。

- dpdk-init：通知OVS需要支持DPDK端口。此时有两种模式可选，选择为True，OVS在DPDK EAL初始化失败后报错，若选择为Try，OVS则会在DPDK EAL初始化失败后继续运行。  
- dpdk-lcore-mask: 用来设置DPDK初始化时用的CPU核。  
- dpdk-socket-mem: 用来设置被DPDK用到的内存大小。  
- dpdk-sock-dir: 当使用 vHost-user 端口时，这个参数决定存放 vHost-user socket 文件的位置。  
- pmd-cpu-mask: 实际用于DPDK数据转发面的CPU核。

# （3）添加DPDK物理网卡端口

类似 OVS 内核版本，用户可以使用 ovs-vsct1 设置添加 bridge。

```txt
$ ovs-vsct1 add-br br0 -- set bridge br0 datapath_type=netdev 
```

添加物理网卡端口需要指定物理网卡的PCI地址，示例如下。

```powershell
$ ovs-vsct1 add-port br0 myportnameone -- set Interface myportnameone type=dpdk options:dpdk-devargs=0000:06:00.0 
```

# （4）添加vHost-user虚拟端口

vHost和Virtio使用client-server模式，Server端负责创建、管理、销毁vHost Sockets。

OVS-DPDK最主要的应用场景是云服务为虚拟机提供网络服务，在早期的OVS版本中，vHost-user端口启动为Server模式，Qemu虚拟机工作在Client模式，这种工作模式的弊端在于当OVS需要升级或重新配置时，需要重启所有的虚拟机。所以，当OVS支持vHost-user client模式后，更推荐将Qemu虚拟机启动为Server模式，vHost-user端口作为client方进行连接，可以有效避免虚拟机在OVS升级过程中的重启。注意，Qemu版本需大于2.7方可支持vHost client模式。

在 OVS-DPDK 中，支持两种 vHost-user 端口。

- vhost-user(dpdkvhostuser): 即 vHost-user 端口为 Server 模式, 命令示例如下。

```txt
$ ovs-vsct1 add-port br0 vhost-user-1 -- set Interface vhost-user-1
type=dpdkvhostuser 
```

- vhost-user-client(dpdkvhostuserclient)：即 vHost-user 端口为 Client 模式，命令示例如下。

```shell
$ ovs-vsct1 add-port br0 dpdkvhostclient0 -- set Interface dpdkvhostclient0 \
type=dpdkvhostuserclient
options:vhost-server-path=/tmp/dpdkvhostclient0 
```

# 4. 个性化参数配置

接下来将介绍一些OVS-DPDK的个性化配置参数，这些参数可能会提高某些特定场景下的性能，但只有使用者对应用场景有深入的理解才能进行合理配置。

# （1）修改DPDK端口的收发描述符数量

DPDK端口描述符的数量对于某些场景值得优化配置。对于零丢包性能要求较高的场景，需要使用较大的接收端描述符数目。例如，OVS默认网卡的收发端描述符数目为2048，对于使用了vhost-dequeue-zerocopy的场景，需要较小的端口发送描述符数目，如果Virtio端口的描述符为256，推荐将网卡描述符设置为128。在OVS-DPDK中，通过参数n_txq_desc和n_rxq_desc进行配置，示例如下。

```txt
ovs-vsct1 set Interface dpdkport options:n_txq_desc=128  
ovs-vsct1 set Interface dpdkport options:n_rxq_desc=128 
```

# （2）支持巨帧

DPDK默认支持的MTU值为1500字节。如果用户想支持巨帧，则需要采用如下命令修改DPDK端口的MTU值。

```txt
$ ovs-vsct1 set Interface dpdk-p0 mtu_request=9000 
```

需要注意的是，对于物理网卡端口，需要查看网卡的技术文档，确认网卡能支持的最大MTU值。

对于vHost-user端口，需要在Qemu建立虚拟机的时候打开mergeable功能才能支持巨帧，Qemu参数示例如下。

-netdev type=vhost-user,id=mynet1,chardev=char0,vhostforce\ device virtio-net-pci,mac $= 00:00:00:00:00:01$ ,netdev=mynet1,mrg_rxbuf $\equiv$ on

# （3）vhost-dequeue-zerocopy（实验版）

在传统的 vHost deque, 需要一次内存复制, 将数据从虚拟机地址空间复制到宿主机地址空间。如果启用了 vHost deque zerocopy 功能, 则可以节省这次内存复制, 将内存指针直接传递给宿主机上的程序使用, 如图 3-35 所示。

![](images/53c958989d9180021da1411b8d1967f49625d526793f7fa93300f4ffb3f71025.jpg)  
图3-35 流匹配示意图

对于大包来说，该功能的启用能够提高一定的性能，但需要注意的是，因为指针传递内存，vHost必须要等宿主机处理完该段数据相应的任务后，才能归还virt queue上的描述符。如果宿主机使用网卡将数据包发送出去，必须注意以下两点。

- 网卡的发送描述符数目必须小于 Virtio 发送端描述符的数量，否则当网卡占用大量描述符时，Virtio 设备将没有描述符使用，会锁死数据传输。在 Qemu 2.10 版本以前，Virtio 设备的默认描述符数量是 256，这时，推荐将网卡的发送端描述符设置成 128。在 Qemu 2.10 版本以后，可以将 Virtio 的最大发送描述符设置成 1024。

```txt
- chardev socket, id=char0, path=/vhost-net \  
- netdev type=vhost-user, id=netdev0, chardev=char0, vhostforce \ 
```

```txt
-device virtio-netpci, netdev=netdev0, mac=52:54:00:00:00:01, mrg_ \rxbuf=on, tx_queue_size=1024\ 
```

- 网卡将数据发送完毕后，应尽快释放描述符，这样 vHost 可以在第一时间释放锁定的描述符，Virtio 设备可用的描述符将会尽可能多。这样的设置对于保障小包的传输性能是极其重要的。在 DPDK 中，网卡归还描述符的行为取决于网卡驱动中 tx_free_thresh 的设置。以英特尔 I40E 设备为例，默认的设置为“#define I40E_DEFAULT_TX-Free_THRESH 32”，表明当网卡发送端空闲的描述符只剩下 32 个时，网卡才开始归还描述符。当 dequeue-zero-copy 启用后，这样的设置将占用大量 Virtio vring 中的描述符。如果需要获得较好的小包数量，需要将这个值尽可能地调大。

# 3.3.2 OVS-DPDK性能优化

# 1. 多虚拟机环境下 OVS-DPDK 的性能影响因素

OVS-DPDK的主要设计目标是获得高吞吐量的交换能力。这里讨论影响OVS-DPDK的主要因素及高阶配置用法。影响OVS-DPDK吞吐量的关键因素论述如下。

# （1）vHost enqueue burst size

收发函数是数据通道中被调用最多的部分，如果在数据收发中，每次调用收发函数只发送1个包，对CPU运算资源会产生很大的浪费。每次收发函数能够批量处理一批数据包，能大大提升CPU的使用效率。

在DPDK中，默认的TX/RXburst size最大值为32，OVS基于DPDK，也采用这个值。当OVS-DPDK需要支持的端口数较少时，各个端口依然能够批量收发数据包。但是，当OVS-DPDK部署在类似云服务的环境中时，可能会有几十个甚至上百个虚拟机。相应地，OVS中会有相应数量的vHost-user端口提供后端服务。这样就会存在vHost端口burst size过小的问题。在物理网卡每次RX收到的一批数据包中，如果数据包去往不同的VM，则需要调用多个vHost-user端口的TX函数，每个TX函数调用发送的包数量也会很小，这是严重影响吞吐量的因素。

# （2）流匹配开销

在 OVS 中，每个数据包需要根据流匹配结果决定下一步的数据流向和动作。OVS-DPDK 提供三层流匹配方案（EMC、dpcls 和 ofproto），如图 3-36 所示。

![](images/ae99b1e60aec7437a5e33dfa958e634bf87b0fbcee247124ec575cddc22ed587.jpg)  
图3-36 三层流匹配方案

在 OVS-DPDK 中，每个 DPDK PMD 线程都会包含一个 EMC 表，EMC 匹配为精确匹配，EMC 表会缓存一定数量的流信息。当数据包的五元组（源 IP、源端口、目的 IP、目的端口和协议号）完全匹配 EMC 表中的表项时，能够立刻获得相对应的下一步动作信息。

但因空间限制和效率的问题，在OVS-DPDK中EMC表项大小默认为8192。一旦发生EMC失配，将会通过dpcls进行流匹配。dpcls表中包含若干个子表，采用通配符的方式进行模糊匹配，该匹配可以只匹配部分字段，例如只匹配源MAC地址。

dpcls匹配流成功后，会将当前流添加到EMC表中，这样的话，该流的后续数据包可以进行快速处理。当dpcls也失配时，OVS只能将数据包通过ofproto传给OpenFlow控制器进行处理，这个查询是最消耗时间的，将会是EMC查询时间的数十倍。

为了解决以上影响吞吐量的关键问题，用户可以合理利用OVS-DPDK提供的以下功能进行性能优化。

# 批处理发包

为了解决之前提到的影响OVS吞吐量的vHostTXsize（发送大小）问题，OVS-DPDK提出了批处理发包的概念。在此之前，当OVS收到一批数据时，如果匹

配了不同的流，但是发送端口如是同一个，OVS则会进行集体发送来提高效率。但是，对于比较慢的前端，比如虚拟机中Virtio-net驱动，vHost端口的TX size依然很小，于是批处理发包应运而生。

简单说来，当每个端口得知自己有数据包要发送的时候，如果TX的数据包数量很小，并不是立刻发送，而是等待一些时间，直到超时，或者达到了最大TX size 的时候才会发送，示例命令如下。

```txt
$ ovs-vsct1 set Open_vSwitch . other_config:tx-flush-interval=50 
```

需要注意的是，这个参数会同时影响吞吐量和延时。数值设置得越大，吞吐量提高得越大，而且延时也会相应提高。 $50\mathrm{ms}$ 这个参考值，是基于x86平台在PVP测试场景中经过测试达到的一个推荐值，并不适用所有场景。读者在自己网络部署中，需要针对不同的应用场景自行优化这个值的设置。

可以通过如下命令查看当前的每个端口批量发送数据包的平均值。

```scss
$ ovs-appctl dpif-netdev/pmd-stats-show
```

在云服务提供商的宿主机上，一般会运行较多的虚拟机，而且虚拟机中运行的多数是慢速的内核驱动设备，数据离散度较高，该功能的开启能够显著提高多虚拟机环境下的网络吞吐量，对于云服务提供商会有较大的帮助。

- 使用多队列提高EMC命中率

提高 OVS-DPDK 整体吞吐量是很多网络开发人员的主要任务。配置网卡和 vHost 端口的多队列是一个能够快速提高吞吐量的方法。首先，利用网卡多队列功能，可以利用更多的 CPU 核参与到数据转发中来，提供更高的运算能力。其次，根据前面描述的 EMC 特性，EMC 表是基于每个 EMC 线程的，提供多队列，可以提高总的 EMC 表项数，配合网卡自带的 RSS 等分流功能，可以提高 EMC 命中率，大大减少数据包匹配查找的时间。在 OVS 中，配置多队列的命令如下。

```txt
$ ovs-vsct1 set Interface <DPDK interface> options:n_rxq=<integer> 
```

- PMD 线程和 CPU 核的手动绑定

OVS-DPDK会对所有的PMD线程和可用于DPDKPMD的CPU核进行默认绑定。但在有些场景下，需要根据每个PMD线程的工作负载程度重新配置CPU的绑定关系，以获得更好的性能和能耗表现。在OVS-DPDK中，可以利用以下命令进行

配置。

多队列端口的CPU核绑定示例如下。

```txt
$ ovs-vsct1 set interface dpdk-p0 options:n_rxq=4 other_config: pmd-rxq-affinity="0:3,1:7,3:8" 
```

该命令将 dpdk-p0 端口进行如下绑定：队列 0，CPU Core 3；队列 1，CPU Core 7；队列 2，不绑定；队列 3，CPU Core 8。

# 2. GSO

对于通常情况，网卡的MTU一般是1500，而在Linux应用层，能够发送的数据包远远大于这个长度，这个时候就需要对数据包进行分片才能通过网卡发送。对于TCP/IP数据包来说，每个分片过的TCP包都需要通过内核协议栈进行处理；而内核协议栈进行数据包分片的计算开销是很大的，是很多场景下的性能瓶颈。针对这一问题，用户希望在传输层和网络层避免这些切片操作，将这些操作尽可能地延后。现在主流的方法有两种：

- 硬件网卡进行切片，将之前所述的计算任务卸载到网卡。  
- 依然由处理器进行切片，但是将切片动作延迟至发送网卡驱动之前。

现在市面上的主流网卡基本都能支持TSO（TCP Segmentation Offload），如果网卡支持TSO，TCP包交由网卡进行切片是最高效的。但是网卡切片的灵活性并不够，对于非TCP包的切片，例如VXLAN、GRE包等，很多网卡并不支持切片卸载。因此，基于软件的GSO被提了出来，作为网卡切片卸载功能缺失下的有效补充。采用基于软件的GSO（Generic Segmentation Offload）有以下好处：不依赖于硬件网卡，对虚拟机中部署的虚拟网络设备也适用，例如Virtio设备；易于拓展，当有新的协议需要支持分片时，基于软件的GSO更容易快速实现，相比硬件解决方案更具有灵活性。

现在的应用程序编写逻辑一般是在数据包发送给网卡前，先判断网卡是否支持当前包类型的硬件切片卸载，如果网卡支持，则由网卡切片并发送；如果网卡不支持，则调用GSO进行切片，然后发送给网卡驱动。图3-37所示为GSO原理示意图。

GSO在DPDK17.11中被开发集成，作为库提供给用户使用。现在支持三种类型的数据包：TCP、VXLAN和GRE。在DPDK18.08中，UDP包的切片也被集成到

GSO的库中。DPDK GSO集成到OVS的工作正在进行中，初步计划会在OVS2.11版本中发布。

![](images/7bc2a266838386b492e0209abed21f839033dce80ab1d7a5a27640abdf81f7e6.jpg)

![](images/4732164f01457244124a83f8404278f1637cba27b0c6f0d141e46b46edd39fb7.jpg)  
图3-37 GSO原理示意图

在 OVS-DPDK 中，GSO 的最常见的应用场景是跨主机的虚拟机之间的通信。当 Qemu 启动时，Qemu 配置参数 host_tso4 会通知 Virtio，vHost 后端有 TSO 能力，请 Virtio 设备发送大包时无须分片。需要注意的是，必须同时打开 csum，否则 VM 中的 Virtio 设备仍会进行分片动作，Qemu 参考命令如下。

```txt
-netdev type=vhost-user,id=mynet1, chardev=char0,vhostforce \ device virtio-net-pci, mac=00:00:00:00:00:01, netdev=mynet1, mrg_rxbuf=on, csum=on, host_tso4=on 
```

vHost收到大包后，转送给DPDK物理端口进行发送，在物理端口驱动进行数据包发送前，OVS-DPDK调用GSO lib进行TCP包的分片，以此来卸载虚拟机中CPU对数据包分片的工作。

# 3.4 FD.IO：用于报文处理的用户面网络协议栈

FD.IO是许多项目和库的一个集合，基于DPDK并逐渐演化，支持在通用硬件平台上部署灵活可变的业务。FD.IO为软件定义基础设施的开发者提供了一个平台，可以创建多个项目，开发基于软件的报文处理创新方案，便于设计高吞吐量、低延时和有效利用资源的应用程序，并能够应用在多个平台上（x86、ARM和PowerPC）和部署在不同的环境中（裸机、虚拟机和容器）。

FD.IO 的一个关键项目是 VPP（Vector Packet Processing，矢量报文处理）。VPP 是高度模块化的项目，新开发的功能模块很容易被集成进 VPP，而不影响 VPP 底层的代码框架。这就给了开发者很大的灵活性，可以创新不计其数的报文处理解决方案。

除了VPP，FD.IO充分利用DPDK特性以支持额外的项目，包括NSH_SFC、Honeycomb和ONE来加速网络功能虚拟化的数据面。此外，FD.IO还与其他关键的开源项目进行集成，以支持网络功能虚拟化和软件定义网络。目前已经集成的开源项目包括OPNFV、OpenStack和OpenDaylight。

如图3-38所示为FD.IO网络生态系统

![](images/8b196a4038965d9263e1000ee7d3310d18ca8906f9079a56d1420983259f6f87.jpg)  
图3-38 FD.IO网络生态系统

# 3.4.1 VPP

VPP 到底是什么？一个软件路由器？一个虚拟交换机？一个虚拟网络功能？事实上，它不只是这些。VPP 是一个模块化和可扩展的软件框架，用于创建网络数据平面应用程序。更重要的是，VPP 代码为现代通用处理器平台而生，并把重点放在

优化软件和硬件接口上，以便用于实时的网络输入输出操作和报文处理。

VPP充分利用通用处理器优化技术，包括矢量指令（例如Intel SSE和AVX）及I/O和CPU缓存间的直接交互（例如Intel DDIO），以达到最好的报文处理性能。利用这些优化技术的好处是：使用最少的CPU核心指令和时钟周期处理每个报文。在最新的Intel Xeon-SP处理器上，可以达到Tbps的处理性能。

VPP 是一个有效且灵活的数据平面，如图 3-39 所示，它包括一系列按有向图组织的转发图形节点和一个软件框架。该软件框架包含基本的数据结构、定时器、驱动程序、在图形节点间分配 CPU 时间片的调度器、性能调优工具，比如计数器和内建的报文跟踪功能。

VPP采用插件架构，插件与直接内嵌于VPP框架中的模块一样被平等对待。原则上，插件是实现某一特定功能的转发图形节点，但也可以是一个驱动程序，或另外的CLI，或API绑定。插件能被插入VPP有向图的任意位置，从而有利于快速灵活地开发新功能。因此，插件架构使开发者能够充分利用现有模块快速开发出新功能。

![](images/242157d23e857ae3056ede05492a785f76a57a80b5531accea70c8e73cadb2c0.jpg)  
图3-39VPP架构的报文处理有向图

输入节点轮询（或中断驱动）接口的接收队列，得到批量报文。接着把这些报文按照下个节点功能组成一个矢量（vector）或一帧（frame）。比如，输入节点收集所有IPv4的报文并把它们传递给ip4-input节点；输入节点收集所有IPv6的报文并把它们传递给ip6-input节点。当ip6-input节点被调度时，它取出这一帧报文，利用双次

循环（dual-loop）、四次循环（quad-loop）及预取报文到CPU缓存技术处理报文，以达到最优性能。这能够通过减少缓存未命中数来有效利用CPU缓存。当ip6-input节点处理完当前帧的所有报文后，会把报文传递到后续不同的节点。例如，如果某报文校验失败，就被传送到error-drop节点，正常报文被传送到ip6-lookup节点。一帧报文依次通过不同的图形节点，直到它们被interface-output节点发送出去。

按照网络功能一次处理一帧报文，有几个好处：

从软件工程的角度看，每一个图形节点是独立和自治的。VPP图形节点的处理逻辑如图3-40所示。  
从性能的角度看，首要好处是可以优化CPU指令缓存（i-cache）的使用。当前帧的第一个报文加载当前节点的指令到指令缓存，当前帧的后续报文就可以“免费”使用指令缓存。VPP充分利用了CPU的超标量结构，使报文内存加载和报文处理交织进行，更有效地利用CPU处理流水线。  
VPP也充分利用了CPU的预测执行功能来达到更好的性能。从预测重用报文间的转发对象（比如邻接表和路由查找表），以及预先将报文内容加载到CPU的本地数据缓存（d-cache）供下一次循环使用，这些有效使用计算硬件的技术，使得VPP可以利用更细粒度的并行性。

![](images/95709c392a4b192b46ec1c804cad217b515c46704175026b9aa4d7081d8ed99a.jpg)  
图3-40VPP图形节点的处理逻辑

依靠有向图处理的特性，使VPP成为一个松耦合、高度一致的软件架构。每一个图形节点利用一帧报文作为输入和输出的最小处理单位，提供了松耦合的特性。通用功能被组合到每个图形节点中，提供了一致的架构。

在有向图中的节点是可替代的。当这个特性与VPP支持动态加载插件节点相结合时，有趣的新功能可以被快速开发，而不需要新建和编译一个定制的代码版本。

# 3.4.2 FD.IO子项目

# 1.Honeycomb和Hc2vpp

Honeycomb 是一个通用的基于 Java 语言的 netconf/restconf 管理代理，并提供一个框架用于构建特定的代理。它充分利用了许多 OpenDaylight 项目（比如 yangtools、controller、mdsal 和 netconf）的功能和特性。使用 Honeycomb 的最重要例子就是 VPP，如图 3-40 所示。Honeycomb 作为一个管理代理，使得 VPP 可以和 SDN 控制器集成在一起，比如 Opendaylight。

![](images/fbb6ab33225274f85f29824a606990ec58cfd26393494d13128db50432fc1aec.jpg)  
图3-41 Honeycomb架构

Hc2vpp 是一个基于 Java 的代理, 运行在与 VPP 实例相同的主机上。它通过 netconf 或者 restconf 提供 yang 模型, 便于上层应用程序可以控制 VPP 实例。

# 2. CSIT

CSIT（Continuous System Integration and Testing，持续系统集成和测试）项目主

要包含以下内容：

开发软件代码支持全自动化测试VPP的功能测试、性能测试和回归测试。  
在虚拟机或物理计算机上执行CSIT测试用例集

与FD.IO的持续集成系统（比如Gerrit与Jenkins）进行集成。

如图3-42所示，CSIT遵循层次化的系统设计原则，被测设备（DUT）和被测系统（SUT）位于系统的底层，呈现层位于系统的顶层，其他功能单元位于中间层。

![](images/6fec62b1b9eea4b0704cc5ff772909f36bb67d25f0b73d7e938ac04db288795c.jpg)  
图3-42 CSIT系统设计架构

# 3. NSH_SFC

NSH_SFC项目充分利用VPP提供的基本框架和功能，处理NSH协议头部信息，提供基于NSH信息的数据转发，以支持SFC（业务功能链）。目前，该项目已支持SFC协议中要求的所有功能实体，包括IngressClassifier（入向分类）、SFF（业务功能转发）、SF-Proxy（业务转发代理）、EgressClassifier（出向分类）等。

如图3-43所示，NSH_SFC编译生成一个plugin，并安装在VPP的plugin目录下。

当VPP启动时，就会自动加载运行NSH_SFCplugin。当NSH_SFCplugin接收VPP转发来的报文时，就会查找NSH Map表，以便决定下一步的操作（比如push、swap、pop NSH头部），而NSH Entry表中保存着对应的NSH头部信息。

![](images/71ccd2f39b9b9075d52854ff24b5567eaeb34ce1d6f53e855055863fab5fdae0.jpg)  
图3-43 NSH_SFC

# 4. VPP Sandbox

VPP Sandbox 是一个临时的仓库，用于存放实验性的 VPP 扩展、插件、库或脚本。目前仓库里包含很多项目，其中应用比较广泛的是 Router插件。

如图3-44所示，Router插件为每一个数据面接口创建一个Tap接口，通过Netlink listener把应用到操作系统的配置信息镜像到VPP数据平面。

![](images/5cc82c23c1ed1d8aed86bb56b61d1f648d6bc8cb110ddaefb354d8941e5e64c1.jpg)  
图3-44Router插件架构

如图3-45所示为Router插件软件架构，具体的处理流程如下：

（1）Router插件为一个数据面接口创建一个Tap接口。  
（2）把目的地址为本地的报文、组播报文和广播报文通过Tap接口转发到主机协议栈处理。  
（3）BIRD接收并处理Tap接口上的报文。  
（4）librtnl通过Netlink与Linux内核协议栈进行交互  
（5）Router插件通过librtnl侦听Netlink地址、链路、邻接和路由信息。  
（6）把侦听到的信息配置到VPP的路由转发表里

![](images/8741f43bd50bec9f7bb6f593731947f6b04bb14f25ad7b32be3f4eb46878ee61.jpg)  
图3-45Router插件软件架构

# 5. TRex

TRex 是一个低成本、高性能、有状态的流量生成器。TRex 支持如下有状态的功能集。

- 支持DPDK1/2.5/5/10/25/40/50/100Gb/s接口。  
支持虚拟化接口：VMXNET3/E1000。  
- 延迟测量和抖动测量。  
- 流排序检查。  
- NAT、PAT动态翻译学习。

- Python自动化API。  
- Windows GUI 支持实时延迟、抖动和排序。

TRex支持如下无状态的功能集：

- 支持最大 $20\mathrm{Mp / s}$ 的流量。  
- 可以修改报文内部的任何字段。  
支持连续、突发、多个突发。  
- 支持交互模式：Console、GUI。  
- 每条流的统计、延迟和抖动。  
多用户支持。

# 6. GoVPP

GoVPP 是一个基于 Golang 语言的 VPP 管理工具集。它包含一系列的 Golang packages, 提供 Go API 用于 VPP 的管理, 这些 Go API 由 VPP binary API 产生。GoVPP 项目能够被任何用 Golang 语言编写的管理平面和控制平面使用。

GoVPP项目包含分离的GoVPP API package和GoVPP core package。如图3-46所示，GoVPP core作为master，负责与VPP交互。多个Agent plugin作为客户端，通过Go PP core与VPP进行交互。这些客户端能够被构建到独立的共享库中，不需链接GoVPP Core及其依赖文件。

![](images/255292003e9b80e56a6ebf37d86a895c21c6b9eca6adc5b46dae88022ff53876.jpg)  
图3-46GoVPP软件架构

# 7. P4VPP

P4是一种行业特定语言，允许开发者为不同的架构编写一套统一的报文处理程序，包括交换ASIC、网络处理器和通用处理器。

P4VPP项目将P4语言的这些特性在VPP数据平面软件上进行支持。它构建一个P4工具链，使得用户提供的P4程序能够适配到VPP软件平台上，因此能够减少新增功能的工作量，并把为了达到高性能而需要的复杂性转移到P4编译器上。

如图3-47所示为P4VPP架构，显示P4程序如何通过P4VPP编译器适配到VPP软件平台上。

![](images/1bf4ccb13385e8d4427331b08cb3c3eb44c866648aedc1251d11c192067d3013.jpg)  
图3-47 P4VPP架构

# 8. DMM

DMM（Dual Mode Multi-protocol Multi-instance，双模、多协议、多实例）项目实现了一个传输层不可知的框架，用于网络应用程序，能够实现如下功能：

用户面协议栈和内核协议栈能够并存。  
根据用户的功能和性能需求，选择不同的网络协议栈。  
- 一个传输协议栈可以同时运行多个实例。

如图3-48所示，DMM框架向上层应用程序提供POSIX套接字，任何一个协议栈可以作为一个插件加入DMM框架。DMM根据RD策略选择合适的网络协议栈。

![](images/f8b90e815c28b48586335f1f34f540ca0af5aa5398969e517adafc86b79d7658.jpg)  
图3-48 DMM架构

# 3.4.3 与OpenDaylight和OpenStack集成

# 1. 与 OpenDaylight 集成

下面以OpenDaylight里的SFC项目与VPP集成为例，介绍VPP如何与OpenDaylight集成。

如图3-49所示，SFC项目把需要配置的路径信息保存到DataStore中，VPPRenderer和VPPClassifier读取对应的配置信息并进行适配，通过Netconf协议与VPP节点进行交互。

![](images/b7125071686f7303db6c02620f1031ff5eba196d0d4a044ebe8bea99a7a99bce.jpg)  
图3-49 OpenDaylight中SFC软件架构

如图3-50所示，配置的SFC信息需要通过Honeycomb进行信息的转换和适配，然后通过预先建立的VPP通道，把信息配置到VPP数据面。配置信息分为两部分：一部分配置VPP核心功能，包括接口、地址、路由等；另一部分配置SFC特定的信息。

![](images/d7d6753459bd1387f673e2a6c9b130969d5293295d7e3ff55b37836b14f5617b.jpg)  
图3-50 SFC与VPP集成的架构

# 2. 与OpenStack集成

networking-vpp是OpenStack下的一个子项目，目的是通过ML2接口为OpenStack提供一个简单、稳定、产品级的VPP集成方案，用于NFV和云应用场景。

networking-vpp的主要设计原则是简单、可扩展、易用。

- 有效的管理面通信：所有通信是异步的，基于RESTful接口。  
- 可用性：所有状态信息保存到一个高扩展性的 KV 存储集群 etcd 中。  
代码短小、易于理解。

如图3-51所示为Networking-VPP的总体架构。

![](images/8b29ac89b8d0cdc01464466e1c76281a7d388a7e18846bbe149f252b88949132.jpg)  
图3-51 Networking-VPP总体架构

Networking-VPP的主要组件功能如下所示：

- Networking-VPP ML2 驱动：实现了 Neutron ML2 机制的驱动 API，运行在控制节点上。  
- Networking-vpp ML2 代理：配置 VPP 数据平面。运行在每个计算节点上。  
- etcd（版本 $>= 3.0.x$ ）：存储ML2代理信息，并开启ML2驱动与ML2代理之间的通信。etcd实例可以运行在控制节点或专用节点上。

# 3.4.4 vBRAS

如图3-52所示，OpenBRAS项目中建议的vBRAS软件架构。OpenBRAS项目是由中国电信、英特尔、浪潮和其他一些公司共同发起的开源项目，它基于转控分离架构。

![](images/65b0da11536518287e417f4c8e78778498b8e7a4545c114478d93816f4c7b1cc.jpg)  
图3-52 vBRAS软件架构

vBRAS上行报文处理流程如图3-53所示

![](images/f212ea03d780ae41306b62357f2f32f25c65b86399ea4ef669ec1dde9ed028e3.jpg)  
图3-53 vBRAS上行报文处理流程

接入侧网卡接收到报文后，通过 RSS 功能把报文分发到不同的上行工作线程对应的队列中。

每个上行工作线程从对应的队列中读取报文，需要经过一系列的功能模块处理，例如PPPoE解封装、ACL、Policer、路由查找，然后从上连网卡发送出去。

在PPPoE解封装模块，如果是PPPoE协议报文，则通过Tap端口发送给OpenBRAS控制平面。

OpenBRAS控制平面的协议报文通过Tap端口下插到VPP数据平面，并通过数据平面转发。

vBRAS下行报文处理流程如图3-54所示

![](images/33235270a33ccaa604bcc06756d06f7e4d8c95fa1a64b2bd13c3f53008690afd.jpg)  
图3-54 vBRAS下行报文处理流程

具体的下行报文处理流程如下：

上连侧网卡接收报文后，通过 RSS 功能把报文分发到不同的下行工作线程对应的队列中。

每个下行工作线程从对应的队列中读取报文，需要经过一系列的功能模块处理，例如路由查找、HQos、PPPoE封装，然后从接入侧网卡发送出去。

# 第4章

# 网络控制

如图4-1所示为一个完整操作系统的基本视图，它传递了这样的信息——操作系统将硬件和应用程序分离开来。

![](images/7f8f2a0e6415e767f23376cb39b8df59eee9fe6b6e8ea893d981f0529a7aa7ee.jpg)  
图4-1 操作系统基本视图

操作系统一方面负责与计算机硬件进行交互，实现对硬件的编程控制和接口操作，调度对硬件资源的访问，另一方面为用户应用程序提供一个高级的执行环境和访问硬件的虚拟接口。

而对于网络时代，随着云计算机、物联网、大数据、5G的到来，网络流量越来越大，需要解决网络安全控制、网络拥塞控制等各种关键问题。传统网络的网卡、网线、网络协议栈、中继器、Hub、网桥、交换机、路由器、无线AC/AP及提供额外网络服务的防火墙等难以配置与管理，华为、思科等网络提供商各有相应的配置命令。另外，由于大部分网络产品是硬件产品，希望增加新的功能时只能向厂商反映，由厂

商解决用户及运营商的需要，导致周期长、效率低。

这种纷乱的状况促进了 SDN（Software Defined Network，软件定义网络）的诞生，SDN 并不是一个具体的技术与协议，而是一个思想、一个框架，只要网络硬件可以通过软件集中式管理、可编程、控制与转发分离，就可以认为这是一个 SDN 网络。为了更简洁、方便、友好地使用各种硬件资源，SDN 把网络产品的控制功能提取出来，统一放到 SON 控制器（SDNC，SDN Controller）中，只保留其数据转发的功能。

类似于图4-1，SDN的基本视图可以表示为图4-2。

![](images/9dac18884253960fa020abc8380e7f13f9220745cb58730cc088129d21399810.jpg)  
图4-2 SDN基本视图

SDNC基于硬件设备之上，扮演了类似操作系统的角色，将通用的网络硬件与网络应用隔离开来。SDNC如同网络的“大脑”控制着网络中的所有设备，而原来的通用网络硬件只需要听从SDN控制器的命令进行“傻瓜式”转发就可以了。于是，SDN简单模型如图4-3所示。

![](images/fe3d897939adb2c68d460e71ca887c36fd1963ef6a56b6146869d66ab62692fe.jpg)  
图4-3 SDN简单模型

集中控制：逻辑上的集中控制能够支持获得网络资源的全局信息，并根据业务需求进行资源的全局配置和优化，例如流量控制、负载均衡等。  
- 开放接口：通过开放的南向接口和北向接口，能够实现应用和网络的无缝集成，应用能告知网络如何运行才能更好地满足自己的需求，例如业务的宽带、时延需求、计费对路由的影响等。  
- 网络虚拟化：通过南向接口的统一和开放，屏蔽了底层物理转发设备的差异，实现了底层网络对上层应用的透明化。

目前开源的SDNC主要有NOX/POX、Floodlight、Ryu、OpenDaylight（ODL）、OpenContrail、Open Network Operating System（ONOS）等。下面对ODL以及Tungsten Fabric展开介绍。

# 4.1 OpenDaylight

SDN的提出在业界引起了很大的反响，众多的网络用户都将其视为可以摆脱网络设备商牵制的机会。于是在2011年创建了一个非营利性组织ONF，致力于制定SDN统一标准，推动SDN的产业化。ONF的工作重点是制订南向接口标准OpenFlow，并且推出了一系列OpenFlow协议，其中较为稳定的是OpenFlow1.0和OpenFlow1.3版本。ONF基于用户的角度制订协议标准，维护用户的利益，但也存在一些问题。

网络设备的研发是一个系统化工程，需要丰富的实战经验，而这些正是网络用户缺乏的，因此导致制订出来的OpenFlow协议过于理想化，只能在实验及简单网络环境中应用，无法进行大规模商用。在这种情况下，ONF不得不接受网络设备商的参与。2013年4月，设备商和软件商主导创建了另一个SDN组织ODL，网络设备商出于自身利益，也加入SDN大军中。

# 4.1.1 ODL社区

ODL 聚集了行业中领先的供应商（主要是网络厂商）和 Linux 基金会的一些成员，包括思科、IBM、Intel、Juniper、BigSwitch、Broadcade、Redhat、VMware、NEC、Arista、HP、Citrix、Ericsson 等，目的在于通过开源的方式创建一个供应商中立的开放环境，打造一个共同开放的 SDN 平台，在这个平台上每个人都可以贡献自己的力量，从而不断推动 SDN 的部署和创新。

ODL 开源社区采用开放的管理模式，个人以及网络服务供应商或是云服务供应商都可以加入，无论什么人都可以贡献代码。自成立以来，ODL 已经先后推出了氢 Hydrogen、氦 Helium、锂 Lithium、铍 Beryllium、硼 Boron、碳 Carbon、氮 Nitrogen、氧 Oxygen、氟 Fluorine 九个版本。

ODL包括多个子项目。每个项目的运营都离不开贡献者（Contributor）、提交者（Committer）和项目管理者（Project Leader）。贡献者开发代码或贡献其他成果，提交者具有将代码提交到该子项目源代码管理系统的权限，决定项目的设计和技术方向，项目管理者负责制定该子项目项目的整体方向并向TSC（技术指导委员会）汇报。

ODL子项目的推出方式类似于OpenStack，新的子项目成熟后即可加入ODL核心项目。项目提出后进入生命周期，需要做出相应的模型，解释每个部分实现什么功能并付诸代码进行实现。一个新项目的资深成员需要在项目启动三个月内选拔新成员参与项目，项目才能获得TSC的批准。这种方式能够鼓励新成员更加深入地参与，为社区注入源源不断的新生力量。

# 4.1.2 ODL体系结构

根据ODL的官方文档，ODL在设计的时候遵循了六个基本的架构原则。

- 运行时的模块化和可扩展化：支持在控制器运行时进行服务的安装、删除和更新。  
多协议的南向支持：南向支持多种协议。  
- 服务抽象层：南向的多种协议对上层提供统一的服务接口。Hydrogen中全线采用AD-SAL（API-Driven SAL，API驱动），Helium版本AD-SAL和MD-SAL（Model-Driven SAL，模型驱动）共存，Lithium之后基本使用MD-SAL架构。  
- 开放可扩展的北向 API：通过 REST 或函数调用方式为用户或应用服务提供开放可扩展的 API。  
- 支持多租户、切片：允许在逻辑或物理上将网络划分成不同的切片或租户。  
一致性聚合：能够确保网络一致性的横向扩展（scale-out），即良好的“克隆”能力。

依照这样的设计原则，ODL有如图4-4所示的架构。

![](images/db152b46bd54f2a013327acfb8bd215af3e66f64d9acf48d89032db8326b0755.jpg)  
图4-4 ODL架构

大体可以分为三个部分：网络应用、编排、服务；控制器平台；数据平面单元。三者之间通过北向接口与南向接口连接。控制器向上层应用提供北向接口，上层应用通过控制器收集信息并进行分析、部署新的网络规则等。南向接口通过插件的方式支持OpenFlow1.0、OpenFlow1.3、BGP-LS等多种协议，这些协议插件动态地连接在服务抽象层SAL上。

# （1）数据平面单元

ODL架构的底层由物理设备、虚拟设备组成，包括传统交换机、纯OpenFlow交换机、混合模式的交换机等。

# （2）南向接口及协议

ODL控制器通过南向接口访问与控制底层的物理与虚拟设备。南向接口使用Netty管理底层的并发I/O。南向接口能够支持多种协议，OpenFlow1.0、OpenFlow1.3、OVsDB、NETCONF、LISP、BGP、PCEP和SNMP等协议以插件的方式动态地挂载在SAL上，如图4-5所示的SNMP协议插件。

![](images/0891ade8ba6f960750d131ae19b38cb00bd49bfe5860fd5b18f7db9584716317.jpg)  
图4-5 SNMP协议插件

SNMP插件通过SNMP协议把流配置安装到以太网交换机的转发表、ACL和VLAN表中，此外需要扩展SAL的API支持一些设置。

# （3）控制器平台

控制器是 ODL 的核心，基于 Java 开发，理论上可以运行在任何支持 Java 的平台上。

ODL 控制器基于 OSGi（Open Service Gateway Initiative）框架，实现了模块化和可扩展化。OSGi 框架实现了一个优雅、完整和动态的组件模型，可以将 ODL 控制器包含的众多功能模块动态组合并提供不同的服务。软件开发一直在追求模块之间真正的“解耦”，而 OSGi 就可以满足这样的要求：在不同的模块中做到彻底的、物理上的分离，而不是逻辑意义上的分离，也就是说在部署之后，可以在运行时不停止服务器的情况下把某些模块拿下来，而此时其他模块的功能不受影响。

ODL控制器包含的功能模块主要如下所述

- 拓扑管理模块：管理拓扑图并存储和处理网络设备的信息，需要OpenFLow协议模块、SAL模块等进行协助，通过与这些模块的交互获取节点、连接、主机等信息。  
统计模块：实现统计信息收集  
- 交换机管理模块：管理南向接口连接的底层设备，提供交换机和交换机端口的详细信息。  
- 转发规则管理模块（FRM）：管理基本的转发规则（例如OpenFlow规则），通过增、删、改、查流规则实现管理。  
主机跟踪模块：跟踪主机信息，记录主机的IP地址、MAC地址、VLAN

及连接交换机的节点和端口信息。主机跟踪模块能以静态或动态方式工作，在动态模式下，使用ARP跟踪数据库的状态（依赖于ARPHolder模块），在静态模式下，其数据库通过北向接口手动填充。

- ARPHolder 模块：监听 IPV4 和 ARP 数据包，从中获取相关主机信息，并根据不同情况做出不同反应。OpenFlow 协议插件收到 ARP 或 IPV4 包后交给 SAL，然后 SAL 再转交给 ARPHolder，ARPHolder 对这两种数据包分别进行处理。

ODL控制器通过服务抽象层SAL自动适配底层不同的设备，使开发者可以专注于业务应用的开发。SAL北向连接控制器的各种功能模块并为之提供底层设备服务，南向连接众多的协议插件，屏蔽不同协议的差异性。

如前所述，ODL第一个版本Hydrogen中，SAL中采用的是API驱动的AD-SAL架构，如图4-6所示。

![](images/2ee4ac2edf80e478f7e93579b0d069016fb55086b4ba0580c2124f3898c244c9.jpg)  
图4-6 AD-SAL架构

在AD-SAL中，通过统一的抽象服务屏蔽南向的协议差异，并采用了生产者模型和消费者模型，提供数据提供者和数据消费者之间的Request Routing（用于消费者的请求路由，从而寻找对应的生产者）。这些抽象服务由南向API和北向API实现，南北向API是一对一的映射关系，北向Plugin通过调用AD-SAL的北向API实现对南向协议插件的调用，操作其管理的设备和服务。

AD-SAL架构比较好理解，但直接静态地使用Java API进行路由与适配，即一切

必须在编译的时候就决定好，这样会显得很不灵活，开发者在使用时需要考虑下层协议插件对服务抽象层所提供的功能的支持程度。

此外，因为南北向API是一对一的映射关系，会导致同一API无法被复用。所有的南北向插件都需要定义相应的API来承载，这样容易造成模块肥大，影响整个软件架构的可扩展性和可维护性。

于是，在Helium版本中引入了模型驱动的MD-SAL架构，如图4-7所示。

![](images/774a19ccb0769b915c62732ba91f2551d2fbbda1e49f514186908f8c84c1ddd7.jpg)  
图4-7 MD-SAL架构

在MD-SAL中，抽象服务和相应API是由各个插件通过YANG Model来定义的YANG Tools通过各个插件组件的模型定义自动生成API、Service接口和相应的Java代码，开发者通过实现自动生成的Service接口实现具体的API和服务内容，插件通过MD-SAL和生成的API以及RPC、Notification、DataStore模块利用其他各个插件的服务和数据。RPC、Notification、DataStore扮演了生产者模型和消费者模型中Broker的角色，是整个ODL的基础，完成数据提供者和数据消费者之间的连通工作。

- RPC：提供服务的远程调用接口。  
- Notification：提供通知，可以发出和接收通知。  
- DataStore：提供数据存储、读取、事务等功能。

此外，使用YANG定义和渲染API大大简化了新应用程序的开发。API的代码是自动生成的，能够确保提供的接口始终保持一致。因此，这些模型很容易扩展。

# （4）北向接口

ODL控制器向上层应用提供的接口称为北向接口。北向接口分为OSGi框架和

双向REST两种。OSGi框架用于与控制器相同地址空间中运行的应用程序，而RESTAPI用于运行在不同于控制器地址空间的应用程序。上层的应用程序实现业务逻辑，通过控制器收集网络信息并运行特定算法进行分析，然后使用控制器在整个网络中编排新规则。

# （5）网络应用、编排和服务

ODL是为了推动SDN发展而诞生的。网络App和业务流层就是控制和编程的平台，包括一些网络应用和事件，可以控制、引导整个网络。借用这一层，用户可以根据需求调用下层模块，享受下层模块提供的相应等级的服务，大大提高了网络的灵活性。也可以利用控制器部署新规则，实现控制与转发的分离。

# 4.1.3 YANG

YANG是随着NETCONF（Network Configuration Protocol）协议而产生的数据建模语言。NETCONF是一种用于给网络设备发送配置的协议。支持NETCONF的设备就相当于有了一套标准的设备API，通过这套API不仅能够对设备进行配置，还能提交一些本来由CLI实现的命令。NETCONF协议的配置功能非常强大，兼顾监控和故障管理、安全验证和访问控制，得到业界的一致认可。

SDN 即用软件定义网络，如何定义？简单说就是用软件配置网络。但是网络能够配置的东西太多，网络设备以及业务的类型更是多种多样，想在这么复杂的网络上开辟一番新的天地，NETCONF 是 SDN 很好的选择。

如图4-8所示，NETCONF协议采用的是C/S的模式，分为安全传输层、消息层、操作层和内容层。NETCONF规定其传输层必须使用SSH、TLS等带有安全加密的通信协议，相比于其他也允许明文传输的协议来说，其在协议层面就已经对数据安全做了第一道守护。

其中，内容层是唯一没有标准化的层。NETCONF协议的精髓就在于这个开放但规范的内容层，开放体现在NETCONF协议本身没有对内容层的数据结构做任何的限定，规范则体现在需要使用YANG语言对内容层的数据进行建模。

在 NETCONF 出现之前，我们熟知且常用的协议，都是采用在协议中规定报文的结构，按字节流读取并解析的架构。为了更好地在字节流中表达更丰富的报文结构，

采用 TLV（Tag+Length+Value）等方式定义对象。但这种方式并不具备可扩展性，一旦对象有修改，就需要变更代码。而如果一个协议有了大量的扩展后的私有数据，就很难成为标准，更不用说协议栈的代码几乎需要完全重写。

![](images/efafd7cb8b14a57d70ff1f40eda0d33d1e64c7bc2bf81f5ae87269301364d576.jpg)  
图4-8 NETCONF协议

NETCONF协议则完全站在了一个更高的维度来解决这个问题。其内容层并没有指定具体的模型结构，而是使用了一套建模语言YANG。使用YANG语言定义的数据模型都可以作为NETCONF的内容层。所以，协议的扩展对NETCONF来说就是不断地增加和修改YANG文件而已。

YANG语言的目标是对NETCONF数据模型、操作进行建模，也就是对设备的配置、状态及操作进行建模。建好的模型，会以XML的形式进行实例化。例如，向领导请假，领导要求写一个请假单，其中包含请假人的姓名、请假的起止时间、请假事由等。于是做好一个表格，包含了上述要求，并根据实际情况填入真实信息。那么，领导的描述就可以理解为“建模”，而最后提交的填好内容的表格，就是将模型实例化了。

本质上，NETCONF协议交换的是XML，传输的是一个个的RPC，只需把配置按照设备上的NETCONF模型写一个XML，通过一个NETCONF客户端发送给设备，设备就可以配置，并把配置的结果返回。传统上进行网络设备的配置，都是需要编写自动配置脚本的，通过基于SSH连接到设备上，然后一条条地输入命令。而有了NETCONF，我们只需要写一个XML模板，因为配置是整体发送过去的，也就不用

输入一条条命令看执行结果。

更进一步，NETCONF对不同的配置结构通过统一的模型语言YANG Model描述的，理论上只要有目标主机的YANG Model，就可以自动生成配置模板。

而对于ODL来说，模型驱动MD-SAL中所谓的模型，指的就是YANG模型，YANG模型是MD-SAL的灵魂，如图4-9所示。

![](images/1f3cb5b05efdbc949d01762f43b5f9f409eaf00fbe68112556b5f0f3ac49409b.jpg)  
图4-9 ODL与YANG模型

# 4.1.4 ODL子项目

ODL由几十个有着相互依赖的项目组成，如图4-10所示为Carbon版本中众多项目及相互之间的依赖关系。

![](images/da710c531ccfbacd9b344c737bd3f8200b27a3fdf073fcf19dc8e7f3834c20f2.jpg)  
图4-10 ODL的“全家福”片段

（1）ODLparent

核心项目是 ODL 中所有项目的 Maven 配置基础，其他项目只需继承 ODLparent 即可获得 ODL 的统一设置。

ODL采用Maven作为项目管理工具，通过Maven工具可以管理项目的生命周期，包括清除、编译、测试、报告、打包、部署等操作。

(2)YANGtools

核心项目，依赖于ODLparent，旨在开发必需的工具和库，为Java项目和应用提供NETCONF和YANG支持。

（3）MD-SAL

核心项目，依赖于ODLparent、YANGtools，管理基于YANG模型定义的各种插件。基于MD-SAL，SDN控制器中丰富的服务和模块可以使用统一的数据结构、南向接口、北向接口。

（4）NetVirt

一个网络虚拟化解决方案，主要包括以下组件：基于虚拟交换机的开放的虚拟化的软件交换机；基于硬件VTEP的硬件交换机；支持虚拟化环境中的服务功能链；支持OVS和DPDK加速的OVS数据路径，L3VPN、EVPN、ELAN、BGPVPN，分布式L2和L3，NAT，浮动IP和IPv6。

（5）Controller

核心项目，依赖于ODLparent、YANGtools、MD-SAL，为多厂家网络的SDN部署提供一个高可用、模块化、可扩展并可支持多协议的控制器基础框架。在该项目中，模型驱动的服务抽象层使控制器支持多个南向协议插件；面向应用的可扩展北向架构为控制器提供丰富的北向API。

(6) AAA

核心项目，为用户开发身份认证、授权和计费功能，包括为用户提供适用于多种身份认证、授权、计费机制的通用模型，提供可插拔的机制并为通用系统提供插件。

（7）OpenFlow

为ODL提供OpenFlow协议支持，实现控制器与OpenFlow交换机之间的交互。OpenFlow在ODL中的实现分为OpenFlowJava和OpenFlowPlugin两部分。OpenFlowJava负责面向南向设备完成OpenFlow协议的序列化、反序列化、端口监听及消息收发；OpenFlowPlugin负责完成OpenFlow协议的状态管理、会话管理、事件处理等，向SAL层提供服务。

# （8）L2Switch

负责处理“L2”事务，将传统L2Switch设备的控制面剥离到控制器上，使控制器具备L2Switch的处理能力，负责MAC地址学习、数据转发决策等。它是具备L2Switch控制能力的应用插件，通过向软交换机下发流表，从而控制数据包的转发行为。

# （9）dLux

为控制器的使用者提供交互式Web UI应用，通过图形化的用户界面提供用户体验。

# (10) Neutron Northbound

一个向OpenStack Neutron提供北向接口的插件项目，是ODL与OpenStack能够协同工作的重要项目。它提供网络、子网、负载均衡、VPN、安全策略等RESTAPI，并随ODL的发展不断增加。这个项目的主要目的有：

- 让 ODL 和 Openstack Neutron 对接。  
- 隔离 Neutron 和 ODL 各自的内部实现细节。  
- 为 ODL 各个网络子项目提供 Neutron 的虚拟网络接口。

对于OpenStack Neutron，也有一个ODL插件称为Networking-ODL，负责将OpenStack网络配置传递给ODL。Networking-ODL有ML2 driver和L3 plugin的模块，可以支持Neutron L2和L3的API，再将数据转发到ODL控制器上。

OpenStack 和 ODL 之间的通信是使用公共 REST API 完成的。如图 4-11 所示为 OpenStack 与 ODL 融合的架构，该模型简化了 OpenStack 的实现，因为它将所有网络任务卸载到 ODL 上，减轻了 OpenStack 的处理负担。

![](images/57e3a082cb9aac38f13a9bd78ee826b6884be24cee3dcf904026d9365a0fe4f8.jpg)  
图4-11 OpenStack与ODL融合的架构

ODL控制器使用NetVirt，然后配置Open vSwitch实例（使用OpenFlow和OVSDB协议），并提供必要的网络环境，这包括第2层网络、IP路由、安全组等。ODL控制器可以维护不同租户之间的必要隔离。

# 4.1.5 ODL应用实例

目前，业界已有众多采用ODL的成熟案例，包括互联网公司、运营商、服务提供商、研究院和学术机构等。

- AT&T公司的应用比较典型，AT&T公司的需求是节点控制器，基于ODL框架开发出全球性的SDN控制器。在实现的过程中，AT&T公司决定将其控制器的覆盖范围扩展到4~7层，超越了通常的1~3层的SDN控制器的概念。  
中国移动公司利用ODL构建下一代网络。中国移动公司正在将OpenStack与ODL结合使用，以构建包括虚拟私有云在内的NovoDC下的多种企业服

务产品作为其2020年网络愿景。中国移动公司通过整合OpenStack、VMware、多个虚拟机管理程序和ODL来统一协调，追求共同的基础架构和通用框架，使用相同的环境和平台管理不同的云服务，包括公共云、虚拟私有云和电信集成云（TIC）。

- 腾讯公司也是 ODL 的受益者，基于 ODL 构建 DCI 控制器，实现带宽使用的改善，以及网络服务的提升，带给用户的直接感受就是玩游戏时不再卡顿。

# 4.2 Tungsten Fabric

Tungsten Fabric的前身是OpenContrail。OpenContrail是Juniper将全功能的Juniper Contrail商业产品开源后的项目，旨在解决SDN闭源项目带来的演进不够快，无法满足云对于自动化和敏捷开发的需求；OpenFlow标准的很多实现受制于由硬件提供商所开发的缺乏编程能力的封闭网络系统。其目标是成为一个产品级的开源SDN控制器，来满足对于网络虚拟化的公有云和私有云需求。

Tungsten Fabric 能够应付世界上最大的运营商的网络环境，并满足他们对网络的严格要求。在此基础上，Tungsten Fabric 将转向关注企业级应用和边缘计算领域。

# 4.2.1 Tungsten Fabric体系结构

Tungsten Fabric 系统由两部分组成：一个逻辑上集中但物理上分布的控制器；一组物理上分布的 vRouter。vRouter 是在通用虚拟服务器上实现的具有包转发功能的软件。

如图4-12所示，TungstenFabric系统提供了三个接口：一组北向的RESTAPI，用于与编排系统（如OpenStack或CloudStack）和应用程序进行对话；一组南向接口（XMPP或BGP+Netconf），用于与虚拟网络或物理网络（网关路由器和交换机）通信；以及一组用于与其他对等控制器交互的东西向接口（标准BGP）。

![](images/b1109597c131f9aa7cf1b1c3dd72a5a97a34e552452a872bd129d258a504fe88.jpg)  
图4-12 Tungsten Fabric体系结构

# 1. 控制器组成

在内部，控制器由三个主要组件组成。

- 配置节点：负责将上层数据模型转换为适合网络交互的底层形态。  
控制节点：负责以一致的方式向底层网络和对等系统传输该底层状态。  
- 分析节点：负责从网络元素捕获实时数据，并进行抽象，以适合应用程序的形式呈现。

vRouters是完全用软件实现的虚拟网络设备。它们负责通过隧道将数据包从一台虚拟机转发到其他虚拟机。隧道形成一个物理IP网络之上的覆盖网络。每个vRouter由两部分组成：实现控制平面的用户空间代理程序和实现转发引擎的内核模块，也有基于DPDK的用户态转发引擎。

# 2. 基本功能

基于图4-12的结构，TungstenFabric系统实现了三个基本功能。

- 多租户（也称为网络虚拟化或网络分片）网络：能够创建为各组 VM 提供封闭用户组（CUG）的虚拟网络。  
- 网关功能：通过网关路由器连接虚拟网络和物理网络的能力。  
- 服务链：通过一系列物理或虚拟网络服务（如防火墙、深度包检测或负载平衡器）引导数据流的能力。

# 3.节点类型

如图4-13所示，TungstenFabric系统被实现为在通用x86服务器上运行的一组协作节点。每个节点可以是单独的物理服务器，又或者是虚拟机（VM）。给定类型的所有节点都以active-active配置运行，因此单个节点不会成为瓶颈。这种横向扩展的设计提供了冗余和水平可扩展性。

除属于TungstenFabric控制器的节点类型（配置节点、控制节点和分析节点）之外，针对整个TungstenFabric系统中执行特定角色的物理服务器和物理网络设备，还存在一些其他节点类型。

计算节点：是托管虚拟机的通用x86服务器。这些虚拟机可能运行普通的应用程序，也可能运行一定的网络服务，如虚拟负载平衡器或虚拟防火墙每个计算节点都包含一个vRouter，用于实现分布式的转发平面和控制平面  
- 网关节点：是将租户虚拟网络连接到物理网络（例如Internet、客户VPN、另一个数据中心或非虚拟化服务器）的物理网关路由器或交换机。  
- 服务节点：提供诸如深度报文检测（DPI）、入侵检测（IDP）、入侵防护（IPS）、WAN优化器和负载均衡器等网络服务的物理网络元素。服务链可以混合虚拟服务（在计算节点上实现为虚拟机）和物理服务（托管在服务节点上）。

为了清楚起见，图4-13并未显示连接底层IP网络的物理路由器和交换机，每个节点到分析节点的接口也没有显示。

![](images/151a43e71355118b3cfa94ffc17da29282f89574bffc417a0420ffa2f2e170ca.jpg)  
图4-13 TungstenFabric系统内部结构

（1）计算节点

计算节点是托管虚拟机的通用x86服务器。标准配置中Linux是主机操作系统，KVM或Xen是VMM，其他主机操作系统和VMM未来也可能会受到支持。vRouter转发平面位于Linux内核或基于DPDK的用户态程序，vRouter Agent是本地控制平面，计算节点结构如图4-14所示。

![](images/5ca43ee391c65df382bcf5ca3e74850b18ed1ec12af03f0fd42cf9bd3ffa0e78.jpg)  
图4-14 计算节点结构

vRouter Agent 是 Linux 用户空间进程。它作为本地轻型控制平面，提供以下功能：

- 使用XMPP与控制节点交换控制状态，如路由。  
- 使用XMPP从控制节点接收底层配置状态（如路由实例和转发策略）。  
- 向分析节点报告分析状态，如日志、统计信息和事件。  
将转发状态下发到转发平面中。  
与NovaAgent合作发现虚拟机的存在和属性。  
- 对每个新的流的第一个报文应用转发策略，并在转发平面的流表中增加流表项。  
- 代理 DHCP、ARP、DNS 和 MDNS。  
- 在 active-active 冗余模型中，每个 vRouter Agent 连接至少两个控制节点以实现冗余。

vRouter转发平面在Linux中作为内核可加载模块运行或基于DPDK的用户态程序运行，并负责以下功能。

- 将包封装并发送到 Overlay 网络，或解封装从 Overlay 网络接收的包。

将数据包分配给路由实例：基于MPLS标签或虚拟网络标识符（VNI），将从覆盖网络接收的包分配给路由实例；将本地虚拟机的虚拟接口绑定到路由实例；查询转

发信息库（FIB）中的目标地址并将数据包转发到正确的目的地，路由可以是三层IP前缀或二层MAC地址。

如图4-15所示为vRouter转发平面的内部结构

![](images/66ac063c1cdeeadc03ee5d47df5ad87a53f92c5ff7db9cbd12e64bc985e49573.jpg)  
图4-15 vRouter转发平面的内部结构

# （2）控制节点

控制节点的内部结构如图4-16所示

![](images/07946d3e134e0855cfcf2e84d60cac57fca75375bd373a6056f0f707e49bde23.jpg)  
图4-16 控制节点的内部结构

控制节点与多种其他类型的节点通信。

- 控制节点使用IF-MAP（可信网络连接协议）从配置节点接收配置状态。  
- 控制节点使用IBGP（内部边界网关协议）与其他控制节点交换路由，以确保所有控制节点具有相同的网络状态。  
- 控制节点使用XMPP与计算节点上的vRouter agent交换路由。还使用XMPP发送配置状态，例如路由实例和转发策略。  
- 控制节点还代表计算节点代理某些种类的流量。这些代理请求也通过XMPP接收。  
控制节点使用BGP与网关节点（路由器和交换机）交换路由。还使用Netconf发送配置状态。

# （3）配置节点

配置节点的内部结构如图4-17所示

![](images/f256f8e0fadbebee39d3802647799624d4fab6b62b325203a12230580a384606.jpg)  
图4-17 配置节点的内部结构

配置节点通过REST接口与编排系统通信，通过分布式同步机制与其他配置节点通信，通过IF-MAP与控制节点通信。

配置节点还提供发现服务，客户端可以使用该服务定位服务提供者（即提供特定服务的其他节点）。例如，当计算节点中的 vRouter Agent 想连接到控制节点（更确切地说：连接到 active-active 的一对 Control VM）时，它使用发现服务来发现控制节点的 IP 地址。客户端使用本地配置、DHCP 或 DNS 定位服务发现服务器。

配置节点包含以下组件。

- 一个REST API服务器，为编排系统或其他应用程序提供北向接口。该接口用于通过高级数据模型安装配置状态。  
- Redis 消息总线，用于进行部组件之间的通信。  
- 用于持久存储配置的Cassandra数据库。  
- Schema变换器，通过Redis消息总线了解上层数据模型的变化，并将上层数据模型中的这些变化转换（或编译）为底层数据模型中的相应变化。  
- 一个 IF-MAP 服务器提供南向接口，将计算出的低层配置向下推送到控制节点。  
- Zookeeper 用于分配唯一的对象标识符并支持事务操作。

# （4）分析节点

分析节点的内部结构如图4-18所示

分析节点与使用北向REST API的应用程序进行通信，使用分布式同步机制与其他分析节点通信，并与控制和配置节点中的组件通过名为Sandesh的基于XML的协议进行通信，该协议专为处理大量数据而设计。

分析节点包含以下组件。

- 收集器：用于交换控制节点和配置节点中组件的 Sandesh 消息。Sandesh 携带两种消息：分析节点为了报告日志、事件和跟踪而收到的异步消息；分析节点可以发送请求并接收响应以收集特定操作状态的同步消息。  
- NoSQL 数据库：用于存储分析好的数据。收集器收集的所有信息都会持久

地存储在NoSQL数据库中。信息来源不会过滤消息

![](images/2fa44ab0ac2cf95d8af94747bac759f2c190ed994bb09c1090d61e03d7ff658f.jpg)  
图4-18 分析节点的内部结构

- 规则引擎：在发生特定事件时自动收集操作状态。  
- REST API 服务器：提供用于查询分析数据库和检索操作状态的北向接口。  
- 查询引擎：执行来自北向REST API的查询请求，被实现为一个简单的map-reduce引擎。该引擎提供了灵活地访问分析好的数据的功能。绝大多数Tungsten Fabric查询都是时间序列。

# 4.2.2 Tungsten Fabric 转发平面

转发平面是通过覆盖网络实现的。覆盖网络可以是三层（IP）覆盖网络或二层（以太网）覆盖网络。对于三层覆盖网络，最初只支持IPv4，IPv6支持将在更高版本中添加。三层覆盖网络支持单播和多播，还具有代理功能，用于避免DHCP、ARP和某些其他协议泛滥。

下面是支持的几种覆盖网络协议。

# 1. MPLS over GRE

L3覆盖网络的MPLS over GRE封装格式如图4-19所示

![](images/de71a7a4372719c1dc05b17a5eb6e6e87a01526924cb871b0174e883a01a8824.jpg)  
图4-19 L3覆盖网络的MPLSoverGRE封装格式

L2覆盖网络的MPLSoverGRE封装格式如图4-20所示

![](images/09ff1e5b529a25b5e2dd1bb815f2d692c818671b3c2f780bf81233f21e6b0e97.jpg)  
图4-20 L2覆盖网络的MPLS over GRE封装格式

MPLS L3VPN和EVPN通常使用MPLS over MPLS封装，如果内核不支持MPLS，它们也可以使用MPLS over GRE封装。Tungsten Fabric 使用MPLS over GRE封装，而不是 MPLS over MPLS，原因是：首先，数据中心的底层交换机和路由器通常不支持 MPLS；其次，即使底层交换机和路由器支持 MPLS，由于其复杂性，运营商也不希望在数据中心运行 MPLS；第三，没有必要对数据中心内部流量改进，因为带宽足够大。

# 2. VxLAN

对于L2覆盖网络，TungstenFabric还支持VxLAN封装，如图4-21所示。

VxLAN封装的主要优点之一是通过将熵（内部头部的散列）放入外部头部的源UDP端口中，可以更好地支持底层中的多路径。

Tungsten Fabric 的 VxLAN 实现与 VLAN IETF 草案主要有两个不同之处：首先，

它只实现 IETF 草案的数据包封装部分，没有实现 flood-and-learn 控制面，而是基于 XMPP 的控制平面，因此，它不需要底层中的多播组；其次，VxLAN 头中的虚拟网络标识符（VNI）对于出口 vRouter 而言是本地唯一的，而不是全局唯一的。

![](images/297262bcc253f7e7b938a1815d1a94150596188b6592f6fc2816061b5697c901.jpg)  
图4-21 L2覆盖网络的VxLAN封装格式

# 3. MPLS over UDP

Tungsten Fabric 支持第三种封装，即 MPLS over UDP。它支持 L2 和 L3 覆盖网络，使用带有本地重要 MPLS 标签的“内部”MPLS 报头标识目的路由实例（类似于 GRE over MPLS），但它使用带有熵的外部 UDP 报头在底层（如 VLXAN）中进行高效多路径。

L3覆盖网络的MPLS over UDP数据包封装格式如图4-22所示。

![](images/7f5145789dfe53f6d21c89d169b98336576995852fab7bbe8f2244ae84209fe6.jpg)  
图4-22 L3覆盖网络的MPLS over UDP数据包封装格式

L2覆盖网络的MPLS over UDP数据包封装格式如图4-23所示

![](images/229404cb787b2ad8ff95b9371332f990d55259741d52a0dfb16d90457dec5a5f.jpg)  
图4-23 L2覆盖网络的MPLSoverUDP数据包封装格式

如图4-24所示为3层转发实例，以下给出从VM1a向VM2a发送IP分组的事件序列的概要。假定为IPv4，IPv6的步骤与此相似。

![](images/64a47ecf454ba92590e0efff9a4889c08794c7022db45497fdb96975004431f9.jpg)  
图4-24 3层转发实例

（1）VM1a中的应用发送具有目的地IP地址VM2a的IP分组。  
（2）VM1a具有指向routinginstance（路由接口）1a中的169.254.xx link-local地址的默认路由。  
（3）VM1a发送ARP请求为link-local地址。routinginstance（路由接口）1a中的ARP代理响应请求  
（4）VM1a将IP分组发送到routinginstance（路由接口）1a。  
（5）routing instance（路由接口）1a上的IP FIB 1a包含VM 2a及相同虚拟网络中的每个VM的/32路由。该路由通过控制节点使用XMPP进行安装。路由将执行以下操作：利用由vRouter 2为routing instance（路由接口）2a分配的MPLS标签，利用包含计算节点2的目标IP地址的GRE头。  
（6）vRouter1在全局IPFIB1中查找新的目标IP地址（计算节点2的地址）并封装数据包。  
（7）vRouter1将封装的数据包发送到计算节点2，具体如何操作取决于底层网络是二层交换网络还是三层路由网络。假设封装的数据包到达计算节点2。  
（8）计算节点2接收封装的数据包并在全局IPFIB2中执行IP查找。由于外部

目的地IP地址是本地的，因此它将数据包解封装，即移除GRE报头并暴露MPLS报头。

（9）计算节点2查找全局MPLS FIB 2中的MPLS标签并找到指向路由routinginstance 2a的条目。它解封装该分组，即它移除MPLS报头并将IP分组暴露到routinginstance 2a中。  
（10）计算节点2在IPFIB2a中查找暴露的内部目的地IP地址，它找到连接VM2a的虚拟接口的路由。  
（11）计算节点2将分组发送到VM2a。

现在回到步骤（7），中掩盖的部分：封装数据包如何通过底层网络转发。如果底层网络是二层网络，则：

- 被封装的数据包的外部源 IP 地址（计算节点 1）和目标 IP 地址（计算节点 2）位于同一个子网上。  
计算节点1向IP地址计算节点2发送ARP请求。计算节点2向MAC地址计算节点2发送ARP应答。请注意，底层中通常不存在ARP代理。  
- 封装的分组是基于目的 MAC 地址从计算节点 1 切换到计算节点 2 的 MAC 层。

如果底层网络是三层网络，则：

封装数据包的外部源IP地址（计算节点1）和目标IP地址（计算节点2）位于不同的子网上。  
- 底层网络中的所有路由器（物理路由器（S1和S2）和虚拟路由器（vRouter1和vRouter2）都参与某些路由协议，如OSPF。  
封装的分组是基于目的地IP地址从计算节点1路由到计算节点2的IP层等价多路径（ECMP）允许使用多个并行路径。出于这个原因，VxLAN封装包括UDP数据包的源端口中的熵

# 4.2.3 Tungsten Fabric 实践

# 1. 编译和构建TungstenFabric

（1）安装Docker

```txt
//For mac:   
https://docs.docker.com.docker-for-mac/install/#download-dockerer  
-for-mac   
// For CentOS/RHEL/Fedora linux host: yum install docker   
// For Ubuntu linux host: apt-get install docker.io 
```

需要注意的是：确保Docker引擎支持大于10GB的images。为/etc/daemon.json添加启动参数并重启docker daemon。

```txt
"storage-driver": "devicemapper",  
"storage-opts": [  
"dm.basesize=20G"  
]  
$ service docker restart 
```

确保在Docker默认的网络设置下，容器可以访问外部网络，比如关闭防火墙，或需要设置代理服务器。

（2）克隆devsetuprepo

```powershell
$ git clone https://github.com/Juniper/contrail-dev-env $ cd contrail-dev-env 
```

（3）执行脚本创建3个容器

```shell
sudo ./startup.sh 
```

检查3个容器是否已经启动。

```powershell
$ docker ps -a
contrail-developer-sandbox [For running scons, unit-tests etc]
contrail-dev-env-rpm-repo [Repo server for contrail RPMs after they are build]
contrail-dev-env-registry [Registry for contrail containers after they are built] 
```

（4）进入developer-sandbox容器

```powershell
$ docker attach contrail-developer-sandbox 
```

（5）运行scons、UT、make RPMS或者make containers

当第一次进入 developer-sandbox 容器，以下步骤是必须执行的。

```txt
$ cd /root/contrail-dev-env
$ make sync # get latest code using repo tool
$ make fetch Packages # pull thirdParty dependencies
$ make setup # set up docker container
$ make dep # install dependencies 
```

如果想切换到其他的代码版本，比如R5.0，在执行以上命令前，需要先执行以下代码。

```shell
$ cd /root/contrail
$ git config --global user.name "Your Name"
$ git config --global user.email "your@e-mail.com"
$ repo init -b R5.0 
```

现在可以编译TungstenFabric。

```txt
$ cd /root/contrail
$ scons # (or "scons test" etc) 
```

contrail-dev-env/Makefile 还提供了一些功能，如下所述。

- make setup: 初始化配置文件（只需运行一次）。  
- make sync: 使用 repo 程序同步 Tungsten Fabric 的代码。  
- make fetch Packages：下载第三方依赖包（每次代码 checkout 后执行）。  
- make dep: 安装所有编译构建所需的第三方依赖包。  
- make dep-<pkg_name>: 安装 <pkg_name>包编译构建所需的第三方依赖包。  
make list: 列出所有可用的 RPM 目标。  
make rpm: 构建所有 RPM 包。  
make rpm<pkg_name>：构建单个<pkg_name>的RPM包。  
- make list-containing: 列出所有可用的容器目标。  
- make containers：构建容器，需要 RPM 包，存储在 /root/contrail/RPMS。  
- make container-<container_name>: 构建单个容器和它所依赖的所有容器。  
- make clean{-containers,-repo,-rpm}：清除所有构建的对象，包括容器，RPM包和repo。

至此，所有的编译和构建就已完成，下面就可以部署测试了。

# 2.自动化部署TungstenFabric

这里介绍一系列的脚本自动化安装基于微服务架构的Tungsten Fabric。微服务架构下的容器及其服务功能如图4-25所示。

![](images/f5c21b2ab9034dd9708cc5b9eb31776f519911338ea3e27f7902c82b18cbd265.jpg)  
图4-25 微服务架构下的容器及其服务功能

# （1）系统要求

自动化部署有一定的系统要求，包括：

CentOS 7.4。

Ansible 2.4.2.0。

所有服务器节点的计算机名解析必须正常工作，无论通过DNS解析还是通过host文件解析。

docker engine, 测试过 17.03.1-ce。

docker-compose, 测试过 1.17.0。

docker-compose python library, 经测试 1.9.0。

如果使用k8s，测试过1.9.2.0。

所有服务器节点时间必须同步。

# （2）获取脚本

For Contrail R5.0 use

```txt
$ git clone -b R5.0 http://github.com/Juniper/contrail-ansible-deployer 
```

For master branch use

```txt
$ git clone http://github.com/Juniper/contrail-ansible-deployer 
```

# （3）支持不同的服务器环境

自动化部署脚本支持不同的服务器环境，包括bms，物理服务器；kvm，基于KVM的虚拟机；gce，基于GCE的虚拟机；AWS，基于AWS的虚拟就。

# （4）3个自动化部署脚本

第一个脚本用来根据不同的服务器环境，准备主机的操作系统。

```txt
playbooks/provision_instances.yml 
```

第二个脚本用来配置主机，安装所需要的软件及配置操作系统。

```txt
playbooks/configuration_instances.yml 
```

第三个脚本用来安装TungstenFabric。

```ignorefile
playbooks/install_contrail.yml 
```

# （5）配置文件

在执行这3个脚本前，需要准备好配置文件，3个脚本都共用同一个配置文件（默认路径config/instances.yaml）。配置文件由多个分区组成。

# 服务器环境分区：

//bms环境示例

```yaml
provider_config:  
bms:  
ssh pwd: contrail123  
ssh_user: centos  
ssh_public_key: /home/centos/.ssh/id_rsa.pub  
ssh_private_key: /home/centos/.ssh/id_rsa  
ntpserver: 192.168.1.1  
domainsuffix: local 
```

Global services 配置分区配置全局的服务参数，所有参数都是可选的。

```txt
global_configuration: CONTAINER_REGISTRY:opencontrailnightly REGISTRY_PRIVATE_INSECURE: True 
```

```txt
CONTAINER_REGISTRY_USERNAME: YourRegistryUser  
CONTAINER_REGISTRY_PASSWORD: YourRegistryPassword 
```

Contrail services 配置分区配置全局的 contrail 全局参数，所有参数都是可选的。

```txt
contrail_configuration: # Contrail service configuration section CONTRAIL_VERSION:latest UPGRADE_KERNEL:true 
```

如果使用Kolla部署OpenStack，需要使用Kolla services配置分区定义参数。

kolla_config:   
customize: nova.conf: | [libvirt] virt_type $\equiv$ qemu cpu_mode $\equiv$ none   
kolla_globals: networkInterface:"eth0" kolla_external_vip-interface:"etho" enable_haproxy:"no" enable_ironic:"no" enableswift:"no"   
kolla_passwords: metadata_secret: strongmetadatasecret keystone_admin_password: password

Instances 配置分区配置每个实例上运行哪些容器，以及哪些角色（role）会安装到该实例上。

```yaml
instances:  
bms1:  
provider: bms  
ip: 10.10.10.11  
roles:  
openstack:  
config_database:  
config:  
control:  
analytics_database:  
analytics:  
webui:  
openstack_compute:  
vrouter: 
```

```textproto
PHYSICAL_INTERFACE: ens802f1  
CPU_CORE_MASK: "0xff0"  
DPDK_UIO_DRV: igb_uio  
HUGE[PAGES]: 3000  
AGENT_MODE: dpdk  
bms2:  
provider: bms  
ip: 10.10.10.12  
roles:  
openstack_compute:  
vrouter:  
PHYSICAL_INTERFACE: enp24s0f0  
CPU_CORE_MASK: "0xff0"  
DPDK_UIO_DRV: igb_uio  
HUGE[PAGES]: 10240  
AGENT_MODE: dpdk  
bms3:  
provider: bms  
ip: 10.10.10.13  
roles:  
openstack_compute:  
vrouter:  
PHYSICAL_INTERFACE: enp24s0f1  
CPU_CORE_MASK: "0xff0"  
DPDK_UIO_DRV: igb_uio  
HUGE[PAGES]: 10240  
AGENT_MODE: dpdk 
```

（6）运行脚本

准备 Instance:

```txt
$ansible-playbook-i inventory/playbooks/provision_instances.yml 
```

配置 Instance:

```shell
$ansible-playbook -e orchestrator=none|openstack|kubernetes \
-i inventory/playbooks/configuration_instances.yml 
```

安装OpenStack：

```txt
$ansible-playbook -i inventory/playbooks/install_openstack.yml 
```

安装TungstenFabric，编排系统可以是OpenStack或者是Kubernetes或者无：

```shell
$ansible-playbook -e orchestrator=none|openstack|kubernetes \
-i inventory/playbooks/install_contrail.yml 
```

使用非默认的配置文件（支持 YAML 和 JSON 格式）。

```shell
$ansible-playbook -i inventory/ -e config_file=/config/ instances_gce.yml \
playbooks/installcontrail.yml 
```

# 4.2.4 Tungsten Fabric 应用实例

OpenContrail主要针对私有云与NFV两种应用场景，这里以私有云的场景为例。

私有云或虚拟私有云（VPC）及IaaS都涉及多租户的问题。所有租户共享包括计算、存储、网络在内的物理资源，但同时又有自己独立的逻辑资源，这些逻辑资源互相隔离，租户并不清楚自己使用了哪些物理资源，体验上就是服务提供商为自己搭建了一套私有网络。

如图4-26所示为用于虚拟私有云和混合云的OpenContrail解决方案。OpenContrail可以在第三方云提供商的公有云网络中创建虚拟私有云，然后使用L3VPN链接（或IPsec连接）将其扩展到现有数据中心、私有云或分支机构中的旧基础架构。之后的工作，无论是在分支机构上的办公网络、数据中心或私有云，都在同一个虚拟网络上，无论在哪个云上都可以通过安全通道互相访问。这种灵活性允许企业选择多个供应商，进而有助于企业IT组织摆脱“影子IT”。

![](images/c43a8eaf52ec1c6476379e6fbf8d779124025678bdf0286704810da15046a09d.jpg)  
图4-26 用于虚拟私有云和混合云的OpenContrail解决方案

# 4.2.5 Tungsten Fabric 与 OpenStack 集成

如前所述，Tungsten Fabric 的北向 REST API 可以被上层应用使用，与 OpenStack 等上层编排系统集成。实际上 OpenStack 是 Tungsten Fabric 重要的集成对象。在创建虚拟机时，Nova 会向 Neutron 请求网络服务，并将虚拟机接入网络。Neutron 通过插件机制能够提供不同的网络服务，例如 DHCP、VPN 等，而 Tungsten Fabric 就是以 Neutron Plugin 的形式集成到 OpenStack 中的，如图 4-27 所示。

![](images/d95da0e3f70621786302ae625736b259f10f5758076b8054b97692a149fbe926.jpg)  
图4-27 TungstenFabric与OpenStack集成

Nova指示计算节点中的Nova Agent创建虚拟机，Nova Agent与Tungsten Fabric Neutron插件通信以检索新虚拟机的网络属性（例如IP地址）。一旦创建虚拟机，Nova Agent就会通知虚拟路由器代理为虚拟网络配置新创建的虚拟机（例如路由实例中的新路由）。

OpenStack 集成 Tungsten Fabric 后，Nova 创建的虚拟机会接入 Tungsten Fabric 的虚拟路由器，租户可以通过虚拟路由器搭建属于自己的私有虚拟网络。图 4-27 中除网络部分与 Tungsten Fabric 相关外，其他都是正常的 OpenStack 组件和交互。

# 第5章

# OpenStack网络

从第一个版本 Austin 至今，OpenStack 已经成长了 8 年。作为一个 IaaS 范畴的云平台，完整的 OpenStack 系统具有如图 5-1 所示的基本视图：OpenStack 将用户和网络背后丰富的硬件资源分离开来。

![](images/5beca71512bde65ea037220419dbddc35e2648cb8a03f1c591e0699c8b0159af.jpg)  
图5-1OpenStack基本视图

OpenStack一方面负责与运行在物理节点上的Hypervisor进行交互，实现对各种硬件资源的管理与控制，另一方面为用户提供一个满足要求的虚拟机。

至于OpenStack内部，作为AWS的一个跟随者，它的体系结构不可避免地体现着AWS各个组件的痕迹。如图5-2所示为OpenStack架构标准视图。

![](images/08ebc7d3e47f454cc25c1e4e24b7ec0e7d64f9680d340fa2e88028abd585ede4.jpg)  
图5-2OpenStack架构标准视图

图5-2涵盖了OpenStack曾经的七个核心组件，分别是计算（Compute）、对象存储（Object Storage）、认证（Identity）、用户界面（Dashboard）、块存储（Block Storage）、网络（Network）和镜像服务（Image Service）。这七个核心组件除用户界面外，其余六个仍是目前的核心组件。每个组件都是多个服务的集合，一个服务意味着运行的一个进程，根据部署OpenStack的规模，决定了选择将所有服务运行在同一个机器上还是多个机器上。

# （1）Compute

Compute 的项目代号是 Nova，它根据需求提供虚拟机服务，比如创建虚拟机或对虚拟机做热迁移等。从概念上看，它对应 AWS 的 EC2 服务，而且它实现了对 EC2 API 的兼容。如今，Rackspace 和惠普提供商业计算服务正是建立在 Nova 之上的，NASA 内部使用的也是 Nova。

# （2）ObjectStorage

Object Storage 的项目代号是 Swift，它允许存储或检索对象，也可以认为它允许存储或检索文件，它能以低成本的方式通过 RESTful API 管理大量无结构数据。它对应 AWS 的 S3 服务。如今，KT、Rackspace 和 Internap 都提供基于 Swift 的商业存储服务，许多公司的内部也使用 Swift 存储数据。

# (3) Identity

Identity 的项目代号是 Keystone，为所有 OpenStack 服务提供身份验证和授权，跟踪用户及他们的权限，提供一个可用服务及 API 的列表。

# （4） Dashboard

Dashboard 的项目代号是 Horizon，它为所有 OpenStack 的服务提供一个模块化的基于 Django 的界面，通过这个界面，无论最终用户还是运维人员都可以完成大多数的操作，比如启动虚拟机、分配 IP 地址、动态迁移等。

# （5）BlockStorage

Block Storage 的项目代号是 Cinder，提供块存储服务。Cinder最早由Nova中的nova-volume服务演化而来，当时由于Nova已经变得非常庞大并拥有众多功能，也由于volume服务的需求会进一步增加nova-volume的复杂度，比如增加volume调度，允许多个volume driver同时工作，同时考虑需要nova-volume与其他OpenStack项目交互，例如将Glance中的镜像模板转换成可启动的volume，所以OpenStack新成立了一个项目Cinder扩展nova-volume的功能。Cinder对应AWS EBS块存储服务。

# (6) Network

Network 的项目代号是 Neutron，用于提供网络连接服务，允许用户创建自己的虚拟网络并连接各种网络设备接口。

Neutron 通过插件的方式对众多的网络设备提供商进行支持，例如 Cisco、Juniper 等，同时也支持很多流行的技术，例如 Openswitch、OpenDaylight 和 SDN 等。与 Cinder 类似，Neutron 也来源于 Nova，即 nova-network，它最初的项目代号是 Quantum，但由于商标版权冲突问题，后来经过提名投票评选更名为 Neutron。

# （7）Image Service

Image Service 的项目代号是 Glance，它是 OpenStack 的镜像服务组件，相对于其他组件来说，Glance 功能比较单一，代码量也比较少。而且由于新功能的开发数量越来越少，社区的活跃度也没有其他组件那么高，但它仍是 OpenStack 的核心项目。

Glance 主要提供一个虚拟机镜像的存储、查询和检索服务，通过提供一个虚拟

磁盘映像的目录和存储库，为Nova的虚拟机提供镜像服务。它与AWS中的Amazon AMI catalog功能相似。

现在以创建虚拟机为例，介绍这些核心组件是如何相互配合完成工作的。用户首先接触到的是界面Horizon，通过其上的简单界面操作，一个创建虚拟机的请求被发送到OpenStack系统后端。

既然要启动一个虚拟机，就必须指定虚拟机操作系统是什么类型，下载启动镜像以供虚拟机启动使用。这件事就是由 Glance 完成的，而此时 Glance 管理的镜像有可能存储在 Swift 上，所以需要与 Swift 交互得到需要的镜像文件。

在创建虚拟机时，自然而然地需要Cinder提供块服务和Neutron提供网络服务，以便该虚拟机有volume可以使用，能被分配到IP地址与外界网络连接，而且之后该虚拟机资源的访问要经过Keystone的认证才可以继续。至此，OpenStack的所有核心组件都参与了这个创建虚拟机的操作。

在OpenStack管理与控制的各种硬件资源中，网络是最重要的资源之一。

Nova实现了OpenStack虚拟机世界的抽象，并利用对象存储与块存储引入的“永久存储（Persistent Storage）”，为虚拟机世界的主体——虚拟机提供了安身之本，负责为每个虚拟机本身的镜像及它产生的各种数据提供一个家，尽量地做到“居者有其屋”。但是没有网络，任何虚拟机都将只是这个世界中的孤岛，不知道自己生存的价值。

# 5.1 OpenStack网络演进

最初，OpenStack中的网络服务由Nova中一个单独的模块nova-network提供，主要支持以下功能：

- 基于网桥的二层网络配置（目前也支持了OpenVSwitch）。  
基于数据库的IP地址管理和分配  
- 组网模式：仅支持 Flat、Flat/DHCP 及 VLAN/DHCP 等简单网络模型。  
- DHCP（动态主机配置协议）及DNS（域名系统）服务。  
- 基于 IPTables 的防火墙策略及 NAT（网络地址转换）功能。

然而，随着用户需求的不断增长和应用场景的日益复杂，尽管nova-network具备简单、稳定等特点，但为了提供更为丰富的拓扑结构和高级网络服务，支持更多的网络类型，具有更好的可扩展性，一个专门的项目Quantum被创建用于取代原有的nova-network。

之后因为与一家公司的名称冲突，Quantum被改名为Neutron，Neutron在Quantum打下的良好基础上，进一步优化了其插件机制，引入SDN思想，并不断开发与支持了DVR、HA、L2POP等特性，使得Neutron在L2-L7的各个方面都取得了长足的进步。不过，时至今日，nova-network依然存在于Nova项目中，并可替代Neutron为OpenStack提供基础的网络服务。

Rocky 版本 Neutron 支持的特性如表 5-1 所示。

表 5-1 Rocky 版本 Neutron 支持的特性  

<table><tr><td>特性</td><td>状态</td><td>Linux Bridge</td><td>Networking MidoNet</td><td>Networking ODL</td><td>Networking OVN</td><td>Open vSwitch</td></tr><tr><td>Networks</td><td>required</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>Subnets</td><td>required</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>Ports</td><td>required</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>Routers</td><td>required</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>Security Groups</td><td>mature</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>External Networks</td><td>mature</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>Distributed Virtual Routers</td><td>immature</td><td>✘</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>L3 High Availability</td><td>immature</td><td>✓</td><td>✘</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>Quality of Service</td><td>mature</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>Border Gateway Protocol</td><td>immature</td><td>?</td><td>✓</td><td>?</td><td>?</td><td>✓</td></tr><tr><td>DNS</td><td>mature</td><td>✓</td><td>✘</td><td>✓</td><td>✓</td><td>✓</td></tr><tr><td>Trunk Ports</td><td>mature</td><td>✓</td><td>✘</td><td>✘</td><td>✓</td><td>✓</td></tr><tr><td>Metering</td><td>mature</td><td>✓</td><td>✘</td><td>✘</td><td>?</td><td>✓</td></tr><tr><td>Routed Provider Networks</td><td>immature</td><td>✓</td><td>✘</td><td>✘</td><td>✓</td><td>✓</td></tr></table>

可以看到，在Neutron支持的特性中，L2（二层）~L3（三层）的服务为必须支持的，其他L4（四层）~L7（七层）的服务还涵盖了LbaaS（负载均衡即服务）、FwaaS（防火墙即服务）、VPNaaS（虚拟专用网络即服务）、DNS、Metering（网络计量服务）等。此外，Neutron还支持网桥、Networking OVN、Open vSwitch等众多驱动。

# 5.2 Neutron体系结构

类似于各个计算节点在Nova中被泛化为计算资源池，OpenStack所在的整个物理网络在Neutron中也被泛化为网络资源池，通过对物理网络资源的灵活划分与管理，Neutron能够为同一物理网络上的每个租户提供独立的虚拟网络环境。

在OpenStack云环境里基于Neutron构建私有网络的过程，就是创建各种Neutron资源对象并进行连接的过程，完全类似于使用真实的物理网络设备规划网络环境的情况，如图5-3所示。

![](images/9ed688db45fe73aa524604d5fc86b25d624fa1fd1cabe11c7b0df43f55b0a71e.jpg)  
图5-3 典型Neutron网络结构

首先，应该至少有一个由管理员创建的外部网络对象负责OpenStack环境与Internet的连接，然后租户可以创建自己私有的内部网络并在其中创建虚拟机，为了使内部网络中的虚拟机能够访问互联网，必须创建一个路由器将内部网络连接到外部网络，具体可参考使用OpenStack Horizon 创建网络的过程。

在这个过程中，Neutron 提供了一个 L3（三层）的抽象 Router 与一个 L2（二层）的抽象 Network，Router 对应真实网络环境中的路由器，为用户提供路由、NAT 等服务，Network 则对应于一个真实物理网络中的二层局域网（LAN），从租户的角度看，它为租户所私有。

# 5.2.1 网络资源模型

OpenStack项目都是通过RESTful API向外提供服务的，这使得OpenStack的接口在性能、可扩展性、可移植性、易用性等方面达到比较好的平衡。而对于Neutron来说，各种RESTful API背后就是Neutron的网络资源模型。

# 1. 网络资源抽象

Neutron将其管理的对象称为资源，例如图5-3中的Network、Subnet，表面上看与传统网络中的概念一样，但由于Neutron管理的范围（数据中心内）和对象的特点（Host内部虚机VM）等原因，与传统网络的概念并不完全相同，甚至有些令人困惑。Neutron管理的核心网络资源包括如下所述。

Network（网络）：隔离的L2广播域，一般为创建它的用户所有，用户可以拥有多个网络。网络是最基础的，子网和端口都需要关联到网络上，网络上可以有多个子网。同一个网络上的主机一般可以通过交换机或路由器连通。  
- Subnet（子网）：逻辑上隔离的L3域，子网代表了一组IP地址的集合，即背后分配了IP的虚拟机。每个子网必须有一个CIDR（Classless InterDomain Routing），并关联到一个网络。IP可以从CIDR或用户指定池中选取。子网可能会有一个网关、一组DHCP、DNS服务器和主机路由。不同子网之间L3并不互通，必须通过一个路由器进行通信。  
- Port（端口）：虚拟网口是MAC地址和IP地址的承载体，也是数据流量的出入口。虚拟机、路由器均需要绑定Port。一个Network可以有多个Port，一个Port也可以与一个Network中的一个或多个Subnet关联。

这里的 Subnet 从 Neutron 的实现上来看并不能完全理解为物理网络中的子网概念。Subnet 属于网络中的 3 层概念，指定一段 IPv4 或 IPv6 地址并描述其相关的配置信息，它附加在一个二层 Network 上，指明属于这个 Network 的虚拟机可使用的 IP 地址范围。一个 Network 可以同时拥有一个 IPv4 Subnet 和一个 IPv6 Subnet。除此之外，即使为其配置多个 Subnet，也并不能够工作。

目前为止，已经知道 Neutron 通过 L3 的抽象 Router 提供路由器的功能，通过 L2 的抽象 Network/Subnet 完成对真实二层物理网络的映射，并且 Network 有 Linux Bridge、Open vSwitch 等不同的实现方式。此外，在 L2 中，还提供了一个重要的抽象 Port，代表了虚拟交换机上的一个虚拟交换端口，记录其属于哪个网络及对应的 IP 等信息。当一个 Port 被创建时，在默认情况下，会为它分配其指定 Subnet 中可用的 IP。当创建虚拟机时，可以为其指定一个 Port。

对于L2层抽象Network来说，必然需要映射到真正的物理网络，但Linux Bridge

与Open vSwitch等只是虚拟网络的底层实现机制，并不能代表物理网络的拓扑类型，目前Neutron主要实现了如下几种网络类型的支持。

- Flat: Flat类型的网络不支持VLAN，因此不支持二层隔离，所有虚拟机都在一个广播域。

- VLAN：与 Flat 相比，VLAN 类型的网络自然会提供 VLAN 的支持。

- NVGRE/GRE: NVGRE (Network Virtualization using Generic Routing Encapsulation) 是点对点的 IP 隧道技术, 可用于虚拟网络互联。NVGRE 允许在 GRE 内传输以太网帧, 而 GRE key 拆成两部分, 前 24 位作为 Tenant ID, 后 8 位作为 Entropy, 用于区分隧道两端连接的不同虚拟网络。

- VxLAN: VxLAN (Virtual Extensible LAN) 技术的本质是将 L2 层的数据帧头重新定义后, 通过 L4 层的 UDP 进行传输。相比于采用物理 VLAN 实现的网络虚拟化, VxLAN 是 UDP 隧道, 可以穿越 IP 网络, 使得两个虚拟 VLAN 可以实现二层联通, 并且突破 4095 的 VLAN ID 限制, 提供多达 1600 万的虚拟网络容量。

GENEVE：通用网络虚拟化封装（Generic Network Virtualization Encapsulation）由IETF草案定义。在实现上，GENEVE与VxLAN类似，仍然是Ethernet over UDP，也就是用UDP封装Ethernet。VxLAN header是固定长度的（8个字节，其中包含24bit VNI），与VxLAN不同的是，GENEVE header增加了TLV（Type-Length-Value），由8个字节的固定长度和0~252个字节可变长度的TLV组成。GENEVE header中的TLV代表了可扩展的元数据。

除了上述L2与L3的抽象，Neutron提供了更高层次的一些服务，主要有FWaaS、LBaaS和VPNaaS。

# 2. Provider Network 与 Tenant Network

Provider Network（运营商网络）与 Tenant Network（租户网络）从本质上来讲都是 Neutron 的 Network 资源模型。其中 Tenant Network 是由租户创建并管理的网络，如图 5-4 所示。

![](images/74a7f60c0a53198d72cc5e09625c955d651a777559fea4082527ba55c7b17c3a.jpg)  
图5-4 Tenant Network

Provider Network 如图 5-5 所示，是 Neutron 创建并用来映射一个外部网络的。这些外部网络并不在 Neutron 的管理范围之内，因此 Provider Network 的作用就是将 Neutron 内部的虚拟机或网络通过实现的映射与外部网络连通。

![](images/c3675531c0f12ebdb735f7605ced9e168cb68bc798c8dd4181fbe7f2530acf4c.jpg)  
图5-5 Provider Network

Provider Network 与 Tenant Network 区别主要在于：

- 管理的角色与权限不同。Tenant Network 由租户创建，而 Provider Network 由管理员创建。  
- 创建网络时传入的参数不同。创建 Provider Network 时，需要同时传入 provider-network-type、provider-physical-network 和 provider-segment 三个参

数，例如：

```txt
$ openstack network create --provider-network-type vlan
--provider-physical-network public --provider-segment 123
provider_net; 
```

而创建Tenant Network时，租户无法传入上述的三个参数，它们根据Neutron在部署时配置的内容进行自动分配。

# 3. Provider Network 与 Multi-Segment Network

Provider与Segment两个概念对于虚拟网络与物理承载网络的映射非常重要，这里有必要对其做专门的说明。

Segment 可以简单理解为对物理网络一部分的描述，比如它可以是物理网络中很多 VLAN 中的一个 VLAN。Neutron 仅仅使用下面的结构定义一个 Segment：

```txt
{NETWORK_TYPE, PHYSICAL_NETWORK, and SEGMENTATION_ID} 
```

如果 Segment 对应了物理网络中的一个 VLAN，则 SEGMENTATION_ID 就是 VLAN 的 VLAN ID，如果 Segment 对应的是 GRE 网络中的一个 Tunnel，则 SEGMENTATION_ID 就是这个 Tunnel 的 Tunnel ID。Neutron 使用这样简单的方式将 Segment 与物理网络对应起来。

在 Neutron 还被称为 Quantum 的时代，创建虚拟网络时不能指定 VLAN ID 或 Tunnel ID。也就是说，如果此时数据中心已经有了一个 VLAN 的 ID 为 100，需要部署一些 VM 在这个 VLAN 上就比较困难。

当时的一些插件，比如网桥是可以做到这点的，但问题在于并没有一个统一的方法来达到这个目的，所以提出了Provider Network API的需求，经过一段时间的发展，名为Provider的Extension API被添加来管理虚拟网络与物理承载网络之间的映射。换句话说，Provider Network的作用就是指创建虚拟网络时Neutron允许指定虚拟网络占用的物理网络资源。

2013年年初，针对Provider Extension API，又提出了更进一步的改进需求，允许将一个虚拟网络与多个物理网络对应起来，换句话说，就是这个虚拟网络可以包含多个、多种不同的Provider Network，这也就是Multi-Segment Network。

```txt
"network": { 
```

```txt
"segments": [ "provider:segmentation_id":"2", "provider:physical_network": "8bab8453-1bc9-45af-8c70-f83aa9b50453", "provider:network_type":"vlan" }, { "provider:segmentation_id":"100", "provider:network_type":"gre" }, ], "n