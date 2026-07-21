# eBPF学习手册

增强Linux内核的可观测性、网络和安全

Learning eBPF

![](images/147f2b280328467f1069592e9c2bd85b6512d2d531a1890a781c913f14c55802.jpg)

# eBPF学习手册

[英]利兹·赖斯（Liz Rice）著

范彬 张保珠 覃璐 译

OREILLY

O'Reilly Media, Inc. 授权中国电力出版社出版

Copyright © 2023 Vertical Shift Ltd. All rights reserved.

Simplified Chinese Edition, jointly published by O'Reilly Media, Inc. and China Electric Power Press, 2024. Authorized translation of the English edition, 2023 O'Reilly Media, Inc., the owner of all rights to publish and sell the same.

All rights reserved including the rights of reproduction in whole or in part in any form.

英文原版由O'Reilly Media, Inc.出版2023。

简体中文版由中国电力出版社出版2024。英文原版的翻译得到O'Reilly Media, Inc.的授权。此简体中文版的出版和销售得到出版权和销售权的所有者——O'Reilly Media, Inc.的许可。

版权所有，未得书面许可，本书的任何部分和全部不得以任何形式重制。

# 图书在版编目（CIP）数据

eBPF学习手册/（英）利兹·赖斯（Liz Rice）著；范彬，张保珠，覃璐译.——北京：

中国电力出版社，2024.8.--ISBN978-7-5198-8988-3

I. TP316.89-62

中国国家版本馆CIP数据核字第2024PR2624号

北京市版权局著作权合同登记图字：01-2024-1241号

出版发行：中国电力出版社

地址：北京市东城区北京站西街19号（邮政编码100005）

网 址：http://www.cepp.sgcc.com.cn

责任编辑：刘炽（liuchi1030@163.com）

责任校对：黄蓓 王小鹏

装帧设计：Karen Montgomery，张健

责任印制：杨晓东

印刷：三河市百盛印装有限公司

版 次：2024年8月第一版

印次：2024年8月北京第一次印刷

开 本：750毫米 $\times 980$ 毫米16开本

印 张：16

字 数：338千字

印数：0001—2500册

定价：78.00元

# O'Reilly Media, Inc.介绍

O'Reilly以“分享创新知识、改变世界”为己任。40多年来我们一直向企业、个人提供成功所必需之技能及思想，激励他们创新并做得更好。

O'Reilly业务的核心是独特的专家及创新者网络，众多专家及创新者通过我们分享知识。我们的在线学习（Online Learning）平台提供独家的直播培训、互动学习、认证体验、图书、视频，等等，使客户更容易获取业务成功所需的专业知识。几十年来O'Reilly图书一直被视为学习开创未来之技术的权威资料。我们所做的一切是为了帮助各领域的专业人士学习最佳实践，发现并塑造科技行业未来的新趋势。

我们的客户渴望做出推动世界前进的创新之举，我们希望能助他们一臂之力。

# 业界评论

“O'Reilly Radar博客有口皆碑。”

Wired

“O'Reilly凭借一系列非凡想法（真希望当初我也想到了）建立了数百万美元的业务。”

Business 2.0

“O'Reilly Conference是聚集关键思想领袖的绝对典范。”

—CRN

“一本O'Reilly的书就代表一个有用、有前途、需要学习的主题。”

—Irish Times

“Tim是位特立独行的商人，他不光放眼于最长远、最广阔的领域，并且切实地按照Yogi Berra的建议去做了：‘如果你在路上遇到岔路口，那就走小路。’回顾过去，Tim似乎每一次都选择了小路，而且有几次都是一闪即逝的机会，尽管大路也不错。”

Linux Journal

# 目录

# 前言 1

# 第1章eBPF是什么，为什么它很重要. 9

1.1 eBPF 起源：伯克利包过滤器 9  
1.2 从 BPF 到 eBPF ..... 11  
1.3 eBPF在生产系统中的演变 11  
1.4命名的挑战 13  
1.5 Linux 内核 13  
1.6为内核添加新功能 16  
1.7 内核模块 17  
1.8动态加载eBPF程序 18  
1.9高性能的eBPF程序 19   
1.10 云原生环境中的 eBPF ..... 20  
1.11总结 23

# 第2章eBPF的“HelloWorld” 24

2.1 BCC 的 “Hello World” 25  
2.2运行“HelloWorld” 28   
2.3 BPF map 30

2.3.1 hash 类型的 map ..... 31

2.3.2 perf 和环形缓冲区 map 34  
2.3.3函数调用 40  
2.3.4尾调用 41

2.4总结 46   
2.5练习 46

# 第3章eBPF程序解析 48

3.1eBPF虚拟机 49

3.1.1 eBPF 寄存器 49  
3.1.2 eBPF指令 50

3.2针对网络接口的eBPF“HelloWorld”示例程序 51  
3.3编译eBPF对象文件 53  
3.4查看eBPF对象文件 54  
3.5 将程序载入内核 ..... 57   
3.6 查看已加载的程序 ..... 57

3.6.1 BPF程序标识 59  
3.6.2编译后的字节码 60  
3.6.3 JIT编译的机器码 60

3.7将程序“附加”到事件上 62  
3.8全局变量 65  
3.9移除程序 67  
3.10卸载程序 68   
3.11 BPF程序调用BPF函数 68  
3.12总结 70   
3.13练习 71

# 第4章bpf()系统调用 73

4.1加载BTF数据 77  
4.2 创建 map 78  
4.3加载eBPF程序 79  
4.4 从用户空间修改 eBPF map ..... 80  
4.5 BPF程序和BPFmap引用 82

4.5.1 Pinning (固定) ..... 83   
4.5.2 BPF链接 84

4.6 eBPF 的其他系统调用 ..... 85

4.6.1 Perf缓冲区初始化 85  
4.6.2附加到perfkprobe事件上 86  
4.6.3设置和读取Perf事件 88

4.7环形缓冲区 89  
4.8 从 BPF map 上读取信息 ..... 92

4.8.1 查找 map 92  
4.8.2 读取 map 元素 ..... 93

4.9总结 94   
4.10练习 95

# 第5章CO-RE、BTF和Libbpf. 98

5.1 BCC 对可移植性的处理方式 ..... 99  
5.2 CO-RE概述 100  
5.3 BPF 类型格式 ..... 101

5.3.1BTF用例 102  
5.3.2 使用 bpftool 工具列出 BTF 信息 ..... 103  
5.3.3BTF类型 104  
5.3.4BTF中的map 107  
5.3.5函数和函数原型的BTF数据 108  
5.3.6 查看 map 和程序的 BTF 数据 ..... 109

5.4生成内核头文件 110  
5.5 支持 CO-RE 的 eBPF 程序 ..... 111

5.5.1头文件 112  
5.5.2定义map 114   
5.5.3 eBPF 程序中的 Section 部分 ..... 115  
5.5.4 使用CO-RE进行内存访问 118  
5.5.5 许可证定义 ..... 120

5.6编译支持CO-RE的eBPF程序 120  
5.6.1 调试信息 ..... 120

5.6.2 优化 ..... 120  
5.6.3目标架构 121  
5.6.4 Makefile 121   
5.6.5 目标文件中的BTF信息 ..... 122

5.7 BPF 重定位 ..... 122  
5.8支持CO-RE的用户空间代码 124  
5.9用户空间代码的libbpf库 124

5.9.1 BPF 骨架 ..... 125  
5.9.2 Libbpf 代码示例 ..... 130

5.10总结 130   
5.11 练习 131

# 第6章eBPF 验证器 132

6.1 验证过程 ..... 133  
6.2 验证器日志 134  
6.3可视化控制流 136  
6.4 验证 helper 函数 ..... 138  
6.5Helper函数参数 138  
6.6检查许可证 139   
6.7内存访问检查 140  
6.8指针解引用之前进行检查 143  
6.9 访问上下文 ..... 144  
6.10运行到完成 144  
6.11循环 145   
6.12检查返回码 146  
6.13 无效指令 ..... 146  
6.14 无法访问的指令 ..... 147  
6.15总结 147   
6.16练习 147

# 第7章eBPF程序类型和附加点类型 150

7.1 程序上下文参数 ..... 151

7.2Helper函数和返回码 151  
7.3 Kfuncs 152   
7.4 跟踪 152

7.4.1 Kprobes和Kretprobes 153   
7.4.2 Fentry/Fexit 156   
7.4.3 Tracepoints 157   
7.4.4开启BTF的Tracepoint 159   
7.4.5 将程序附加到用户空间函数上 ..... 160  
7.4.6 LSM 161

7.5网络 162

7.5.1 Socket 164   
7.5.2 流量控制 164  
7.5.3 XDP 165   
7.5.4 Flow Dissector 165   
7.5.5轻量级隧道 166   
7.5.6 Cgroup 166   
7.5.7 Infrared控制器 166

7.6 BPF 附加点类型 167  
7.7总结 168   
7.8练习 168

# 第8章eBPF网络 170

8.1 数据包丢弃 ..... 171

8.1.1XDP程序返回码 171  
8.1.2 XDP数据包解析 172

8.2 负载均衡和转发 176   
8.3 XDP offloading 179   
8.4流量控制（Traffic Control，TC） 181  
8.5 数据包加密和解密 ..... 185  
8.6 eBPF and Kubernetes 网络 ..... 189

8.6.1避免使用iptables 191  
8.6.2网络程序的协同 192

8.6.3执行网络策略 194  
8.6.4网络传输数据加密 196

8.7总结 198   
8.8练习和进一步阅读 198

# 第9章eBPF安全 199

9.1安全可观测性需要策略和上下文 199  
9.2 使用系统调用处理安全事件 201

9.2.1 seccomp 201   
9.2.2 生成 seccomp 配置文件 ..... 202  
9.2.3 系统调用跟踪类的安全工具 ..... 205

9.3 BPF LSM 207   
9.4Cilium Tetragon 209

9.4.1 附加到内核内部函数上 ..... 209  
9.4.2 预防性安全 210

9.5网络安全 212  
9.6总结 213

# 第10章eBPF编程 214

10.1 bptrace 214   
10.2 内核态 eBPF 程序的编程语言选择 ..... 218  
10.3 BCC Python/Lua/C++ 219   
10.4 C 和 Libbpf 221

10.4.1 Go 223   
10.4.2Gobpf 223   
10.4.3 Ebpf-go 223   
10.4.4 Libbpfgo 226

10.5 Rust 227

10.5.1 Libbpf-rs 228   
10.5.2 Redbpf 228   
10.5.3 Aya 229   
10.5.4 Rust-bcc 231

10.6 测试 BPF 程序 ..... 231  
10.7附加多个eBPF程序 232  
10.8总结 233  
10.9练习 234

# 第11章eBPF的前景 235

11.1 eBPF 基金会 235  
11.2 Windows 上运行 eBPF 236  
11.3 Linux eBPF 的演进 ..... 237  
11.4 eBPF 是一个平台，而不是一个功能 ..... 240  
11.5结论 241

# 前言

近年来，eBPF在云原生社区及相关领域已经成为一个极热门的话题。在网络、安全性、可观测性等方面，涌现出一批基于eBPF平台开发的工具和项目（https://ebpf.io/applications）。这样的项目不仅在持续增加，并且与之前的实现方式相比，有更好的性能和准确性。eBPF相关的会议吸引了成千上万的与会者和观众，如eBPF峰会（https://ebpf.io/summit-2022）和Cloud Native eBPF Day（https://oreil.ly/q9-p3）等，在撰写这本书时，Slack上eBPF频道的成员（http://ebpf.io/slack）已经超过14000名。

为什么这么多基础设施工具要选择eBPF作为底层技术？它又如何实现性能的提升？在从性能监测到网络安全等多个领域，eBPF又发挥了怎样的作用？

本书将通过介绍eBPF的工作原理，以及使用一些eBPF代码来回答这些问题。

# 读者对象

本书的读者对象是开发人员、系统管理员、运维人员，以及对eBPF有兴趣并想深入理解其工作原理的学生。本书将为探索如何编写eBPF程序的朋友提供基础知识，并且，由于eBPF是一个全新的工具和可观测性平台，未来几年，有丰富eBPF开发经验的人员可能会获得相对高薪的岗位。

另外，这本书不仅在你准备写eBPF代码时有用，如果你从事运维、安全或任何其他基础设施相关的工作，现在或者未来几年，很可能会遇到基于eBPF开发的工具，了解这些工具的内部运行机制，就可以更有效地使用它们。例如，如果你知道eBPF程序的事件是如何触发，那么当使用eBPF开发的工具展示性能指标时，你就会有一个更好的思维模型来准确地判断这个性能指标代表的真正含义。如果你是一名应用程序开发人员，可能还会接触到一些eBPF开发的工具，比如在做应用程序的性能调优时，可能会用到类似Parca的工具(https://www.parca.dev)把运行耗时多的函数用火焰图方式展示。如果你正在进行安全工具选型，这本书将帮助你了解eBPF的优势，避免选择一些看似美好，实则不太有效的应对攻击的工具。

即使你还没有用过eBPF工具，本书也能让你对Linux领域有一些有趣的洞见。大多数开发人员无需考虑内核的实现细节，因为编程语言通过高级抽象屏蔽了内核层操作，使他们能够专注开发业务逻辑部分的代码，即使这样用好一种编程语言也挺难的！开发人员需要使用调试和性能分析等工具，才能更有效地完成开发工作。虽然深入了解调试或性能分析工具的内部机制并非必需，但对我们来说，深究这些技术细节是一件既有趣又充实的事。注1同样，大多数人使用eBPF工具，不必在意工具是如何构建的。Arthur C. Clarke写道“任何足够先进的技术都像魔术一样”(https://oreil.ly/gOVID)，就我个人而言，我喜欢深入研究并了解魔术是如何实现的。如果你和我一样，为了更好地使用好这项技术，觉得有必要探索eBPF编程，那么你一定会喜欢这本书。

# 本书涵盖的内容

eBPF正在快速更新迭代中，因此要编写一份不过时的eBPF综合参考书相当困难。不过基本原理和基本原则不太可能有很大变化，这是本书讨论的重点。

第1章分析了eBPF技术强大的原因，并解释了它如何能在操作系统内核中运行自定义程序，为我们提供许多令人兴奋的功能。

第2章的内容开始具体化，通过“Hello World”示例介绍eBPF程序和Map的概念。

第3章详细介绍了内核态eBPF程序，以及其在内核中的运行方式。第4章探讨了用户空间应用程序与内核态eBPF程序之间进行交互的接口。

第5章探讨了“编译一次，到处运行”（CO-RE）技术如何应对近年来eBPF在不同内核版本间兼容性的挑战。

第6章深入讲解了eBPF验证器，这是eBPF最为显著的特点，它能对eBPF代码的验证，使其与内核模块有根本区别。

第7章介绍了许多不同类型的eBPF程序和附加点的概念，其中许多附加点都在网络栈中。第8章进一步探讨了eBPF在网络方面的应用。第9章介绍如何使用eBPF构建安全工具。

第10章概述了各种编程语言中可用的库和框架，用于编写与eBPF程序交互的用户空间应用。

最后，在第11章，我们展开想象畅谈eBPF的未来发展。

# 预备知识

本书假设读者熟悉Linux的基本shell命令，也了解使用编译器将源代码编译成可执行程序的概念，同时假设读者了解make命令，知道如何使用Makefile文件，并提供了一些简单的Makefile示例。

本书中包含了大量的Python、C和Go的代码示例，读者不需要深入了解这些语言，也可以从中受益，但如果读者乐意阅读这些代码，将能更好地理解

本书的内容。同时，假设读者对指针的概念比较熟悉，指针用于指向内存地址的数据类型。

# 示例代码和练习

书中有很多代码示例，如果读者想运行这些代码，可以从https://github.com/lizrice/learning-ebpf获取代码及阅读配套的安装配置说明。

大部分章节的末尾加入了练习，通过扩展示例或自己编写程序的方式来帮助读者更好地探索eBPF编程。

因为eBPF在不断发展更新中，不同的内核版本包括不同的eBPF功能。许多功能在早期版本中受限制，在新版本中限制可能会被取消或放宽。Iovisor项目对不同内核版本支持的BPF功能进行了有用的描述（https://oreil.ly/ SsnEV）。在这本书中，我也尝试把提到的特定的eBPF功能是何时添加的标识出来。书中的例子是使用5.15版本的内核进行测试的，在撰写本书时，一些流行的Linux发行版还不支持这个最新的内核版本。如果读者在这本书出版后不久就开始阅读，会发现有些功能在生产环境中使用的Linux内核上还不起作用。

# eBPF仅适用于Linux吗？

eBPF最初是为Linux开发的，所以同样的方法在其他操作系统中不能使用。但事实上，微软一直在开发使Windows提供eBPF支持（https://oreil.ly/k7AvA），我在第11章中会简要讨论这一点。但在本书的其余部分中，我将重点讨论Linux的实现，所有的示例也都基于Linux实现的。

# 排版约定

本书使用以下排版约定：

斜体 (Italic)

表示URL、主机名、目录和文件名、UNIX命令和选项、程序，偶尔用于强调。

等宽字体（ConstantWidth）

表示程序列表，以及在段落内部引用程序元素，例如变量或函数名、数据库、数据类型、环境变量、语句和关键字。

等宽粗体字（ConstantWidthbold)

表示运行命令时你需要键入的文本。

斜体等宽字体（ConstantWidthItalic)

表示变量输入，应该替换成用户提供的值。

![](images/2c2527d649614a764bc37e1d94644a0ac36c7290726463406285b61e2ae2957d.jpg)

表示提示或建议。

![](images/f0a81517f3f27c8d449ca34d6b224ce03a16afb2a93ebb366be083c0c0f40e13.jpg)

表示一般注释。

![](images/7138e6a5daf630f63ad3cf0815d679b828a5124dd91da61106885151a25ed918.jpg)

表示警告或提醒。

# 代码示例使用说明

附加材料（代码示例、练习等）可在https://github.com/lizrice/learning-ebpf上下载。

如果读者使用代码示例有技术问题，请发送电子邮件至 bookquestions@oreilly.com。

本书的目的是帮助读者使用eBPF完成工作。一般情况下，如果本书提供的示例代码，读者可以在程序和文档中直接使用，除非复制了代码的重要部分，否则无需获得我们的许可。例如，编写了一个使用本书中多个代码片段的程序是不需要许可的；销售或分发O'Reilly图书中的示例代码则需要许可。在引用本书内容包括示例代码回答问题时，无需获得许可；将本书中大量的示例代码纳入读者产品文档中则需要获得许可。

我们欢迎读者注明出处，但不是必须。标明出处通常包括书名、作者、出版商和ISBN。例如：“Learning eBPF by Liz Rice (O'Reilly).Copyright 2023 Vertical Shift Ltd.,978-1-098-13512-6”。

如果读者觉得对代码示例的使用超出了合理使用范围或上述许可，可以随时通过 permissions@oreilly.com 与我们联系。

# O'Reilly 在线学习平台 (O'Reilly Online Learning)

# OREILLY

近40年来，O'Reilly Media致力于提供技术和商业培训、知识和卓越见解，来帮助众多公司取得成功。

我们拥有独一无二的专家和革新者组成的庞大网络，他们通过图书、文章、会议和我们的在线学习平台分享他们的知识和经验。O'Reilly 的在线学习平台允许你按需访问现场培训课程、深入的学习路径、交互式编程环境，以及 O'Reilly 和 200 多家其他出版商提供的大量文本和视频资源。有关的更多信息，请访问 http://oreilly.com。

# 意见和疑问

请把你对本书的意见和疑问发给出版社：

美国：

O'Reilly Media, Inc.

1005 Gravenstein Highway North

Sebastopol, CA 95472

中国：

北京市西城区西直门南大街2号成铭大厦C座807室（100035）

奥莱利技术咨询（北京）有限公司

我们为这本书创建了一个网页，读者可以通过https://oreil.ly/learning-eBPF访问该页面，了解本书的勘误、示例和其他信息。

如果读者对本书有评论或者技术上的问题，请发送电子邮件至bookquestions@oreilly.com。

有关O'Reilly图书和课程的新闻和信息，请访问https://oreilly.com。

我们的 LinkedIn：https://.linkedin.com/company/oreilly-media。

我们的Twitter：http://twitter.com/oreillymedia。

我们的 Youtube: http://www.youtube.com/oreillymedia。

# 致谢

我要感谢在撰写本书过程中做出了巨大贡献的人们：

- 技术审稿人 Timo Beckers、Jess Males、Quentin Monnet、Kevin Sheldrake 和 Celeste Stinger 提出了详细可行的反馈和改进建议，对此我非常感激。

- 我站在那些推广和持续维护eBPF的巨人们的肩膀上，包括Daniel Borkmann、Thomas Graf、Brendan Gregg、Andrii Nakryiko、Alexei

Starovoitov，以及无数为社区做出贡献的其他人，他们不仅仅贡献了代码，还有会议演讲和博客文章等。

- 感谢在Isovalent公司遇到的才华横溢、很可爱的同事们，其中许多人都是eBPF和内核专家，我从他们身上学到了很多。  
- 还要感谢 O'Reilly 团队，尤其是我的编辑 Rita Fernando，在写作过程中给予我无尽的支持，以及帮助确保书籍按计划完成规划工作；还有 John Devins，感谢你当初鼓励我写这本书。  
- Phil Pearl 不仅对内容提供了有帮助的反馈，还确保我的饮食和起居。对于他给予的支持和鼓励感激不尽，我会一直铭记。

此外，我还要感谢多年来所有愿意花时间给我的工作提出鼓励性评价的优秀人士，无论是在线下活动中还是在社交媒体上。希望我所写或记录的东西，能够帮助他人理解技术概念，或激发他们构思或写文章的愿望，想到这些我就很受鼓舞。谢谢你们！

# eBPF 是什么，为什么它很重要

eBPF是一种革命性的内核技术，它允许开发人员编写动态加载到内核的自定义代码，以此改变内核的行为。如果你还不太了解内核，别担心，我们会在本章稍后进行解释。

eBPF使构建新一代的高性能网络、可观测性和安全工具成为可能。如你所见，如果你想用基于eBPF的工具对应用程序进行监控，你无需修改或重新配置应用程序，这就是eBPF在内核开发中的优势所在。

eBPF可以做的事情包括：

- 对系统的几乎所有方面进行性能追踪。  
- 具有内置可见性的高性能网络。  
- 检测并（可选地）阻止恶意活动。

让我们从BPF（伯克利包过滤器）开始，简要回顾一下eBPF的历史。

# 1.1 eBPF 起源：伯克利包过滤器

eBPF起源于伯克利包过滤器。它最早出现在1993年，劳伦斯伯克利国家实

验室的 Steven McCanne 和 Van Jacobson 撰写的一篇论文中。[注1] 这篇论文讨论了一种虚拟机，用于运行过滤器程序，以确定是否接受或拒绝网络数据包。这些过滤程序使用 BPF 指令集编写。该指令集是一套通用的 32 位指令集，与汇编语言非常相似。以下是直接从论文中摘录的一个例子：

1dh [12]

jeq #ETHERTYPE IP, L1, L2

L1: ret #TRUE

L2: ret #0

这一小段代码的目的是过滤掉那些不是IP协议的数据包。该过滤器的输入是一个以太网包，第一条指令(1dh)加载了包的第12个字节开始的2个字节的值。在下一条指令（jeq），判断这个值#ETHERTYPE是否为IP。如果匹配，执行将跳转到L1指令，并返回一个非零值（这里定义为#TRUE），表示接收该数据包。如果不匹配，那么这个数据包就不是IP包，则通过返回0来拒绝。

你可以编写或使用参考论文中的例子来实现更复杂的过滤器程序，例如，根据数据包的其他内容来做出决策。过滤器程序的开发者可以编写自定义程序在内核中运行，这是非常重要的，正是eBPF的核心功能所在。

BPF是“Berkeley Packet Filter（伯克利包过滤器）”的缩写，它于1997年被首次引入到Linux中，版本号为2.1.75，在tcpdump工具中用于高效捕获要跟踪的数据包。注2

2012年，3.5版本的内核引入了seccomp-bpf，可以通过编写BPF程序来决定用户空间程序允许执行的系统调用集，我们将在第10章中更详细地探讨这一点。这是BPF从狭义的数据包过滤发展成为今天这样一个通用平台的第一步，从此以后，名字中的包过滤器开始变得没有意义！

# 1.2 从 BPF 到 eBPF

从2014年3.18版本内核开始，BPF演进为我们所说的“扩展BPF”或“eBPF”。涉及几个重大变化：

- BPF指令集进行了全面的改造，以便在64位机器上运行更高效，并且将解释器完全重写。  
- 引入了eBPF map，一种可以被BPF程序和用户空间应用访问的数据结构，并允许它们之间共享信息。你将在第2章了解更多关于eBPF map的信息。  
- 新增了bpf()系统调用，以便用户空间程序可以与内核中的eBPF程序进行交互。你将在第4章阅读到有关这个系统调用的内容。  
- 新增了若干 BPF helper 函数。你将在第 2 章看到一些例子，并在第 6 章了解更多细节。  
- 新增了eBPF验证器，以确保eBPF程序安全运行。我们将在第6章进行讨论。

这些为eBPF奠定了基础，但其发展并未放缓！从那时起，eBPF有了显著的变化。

# 1.3 eBPF在生产系统中的演变

自2005年起，Linux内核中就存在了kprobes（内核探针）功能，它几乎可以在内核代码中所有指令上设置监测点。开发人员可以使用kprobes编写内核模块，将函数“附加”到内核代码的指令上，以此用于调试或性能测量。注3

2015年，新增了将eBPF程序“附加”到kprobes上的功能，这是Linux系统追踪方式革命的起点。与此同时，内核的网络栈中也新增了钩子，让eBPF程序在网络功能方面发挥更大的作用。我们将在第8章中对此进行更深入的探讨。

到2016年，基于eBPF的工具已经开始在生产系统中使用。Brendan Gregg在Netflix从事追踪工作，他的工作在基础架构和运维界引起了广泛关注，他说的“eBPF为Linux赋予了超能力”这一观点也同样为人熟知。同年，Cilium项目宣布启动，这是首个在容器环境中使用eBPF替换整个数据路径的网络项目。

次年，Facebook公司（现名Meta）开源了Katran项目（https://oreil.ly/X-WsL）。Katran是一个4层负载均衡器，满足了Facebook对高扩展性和快速的负载均衡解决方案的需求（https://oreil.ly/zl4yX）。自2017年以来，Facebook.com的每一个数据包都会经过eBPF/XDP程序处理。注4对我个人而言，正是这一年，我在得克萨斯州奥斯汀的DockerCon上观看了Thomas Graf的关于eBPF和Cilium项目（https://oreil.ly/doKbd)的演讲（https://oreil.ly/g9ya0)，这项技术所带来的可能性让我感到十分兴奋。

2018年，eBPF成为Linux内核中的一个独立子系统，Isovalent公司的DanielBorkmann(http://borkmann.ch)和Meta公司的AlexeiStarovoitov(https://oreil.ly/K8nXI)担任维护者，之后，Meta公司的Andrii Nakryiko(https://nakryiko.com)也加入了队伍。同年，BTF问世，进一步增强了eBPF程序的可移植性。我们将在第5章对此进行探讨。

2020年发布了LSMBPF，允许eBPF程序附加到LSM接口上。这表明，eBPF的第三个主要用例已经确定，除了网络和可观测性之外，eBPF还是一个出色的安全工具平台。

多年以来，得益于300多名内核开发人员和许多相关用户空间工具（如bpftool，我们将在第3章中介绍）、编译器和编程语言库的贡献者的努力，eBPF的功能得到了大幅提升。程序曾被限制在4096条指令以内，现在这一

限制已增加到100万条指令可通过验证器检验<sup>5</sup>，而且由于支持尾调用和函数调用（我们将在第2章和第3章中介绍），这一限制实际上已变得毫无意义。

![](images/11bd2cb3a437a0d2a12e08c3e0663ec0282ef7dee75f9c805b565325966b74cd.jpg)

要深入了解eBPF的历史，eBPF的维护者更有发言权。

Alexei Starovoitov 对 BPF (https://youtu.be/DAvZH13725I) 的历史作了精彩的演讲。在演讲中，他从软件定义网络 (SDN) 的起源讲起，讨论了早期让内核接受 eBPF 补丁所使用的策略，并透露 eBPF 正式诞生日是 2014 年 9 月 26 日，这一天标志着第一套涵盖验证器、BPF 系统调用和 map 的补丁被内核接受。

同时，Daniel Borkmann 还讨论了 BPF 的历史及其支持网络和跟踪功能的演变。我强烈推荐观看他的演讲“eBPF and Kubernetes: Little Helper Minions for Scaling Microservices（eBPF 和 Kubernetes: 扩展微服务的小助手）”(https://youtu.be/99jUcLt3rSk)，其中充满了许多有趣的信息。

# 1.4 命名的挑战

因为eBPF的应用范围远远超出了包过滤器，所以BPF作为包过滤器这个缩写现在基本上已失去了原有的含义，eBPF已经成为一个独立的术语。由于目前广泛使用的Linux内核都支持eBPF，eBPF和BPF这两个术语在很大程度上可以交替使用。在内核源代码和eBPF编程中，常用的术语是BPF。例如，我们将在第4章中看到，与eBPF交互的系统调用是bpf()，helper函数以bpf_开头，不同类型的BPF程序以BPF_PROG_TYPE开头。在内核社区之外，“eBPF”这个名字似乎已经深入人心，例如：社区网站ebpf.io（https://ebpf.io）和eBPF基金会（http://ebpffoundation）。

# 1.5 Linux 内核

要理解eBPF，你需要牢固掌握Linux内核空间和用户空间之间的区别。我在

《什么是eBPF?》注6文中介绍了这一点，这里对其中的一些内容做了调整，以便用于接下来的阐述。

Linux内核是应用程序与硬件之间的软件层。应用程序运行在用户空间的非特权层中，不能直接访问硬件。相反，应用程序通过系统调用接口发出请求，要求内核代表其执行操作。其中硬件访问可能涉及读/写文件，发送或接收网络流量，或者只是访问内存。内核还负责协调并发进程，使许多应用程序能同时运行。如图1-1所示。

![](images/cb1ff9a1314a3e04b9fc61c7f9947ada334b64e41722c42126871671e687edc8.jpg)  
图1-1：用户空间的应用程序使用系统调用接口向内核发送请求

作为应用程序开发者，我们通常不会直接使用系统调用接口，编程语言为我们提供了易于编程的高级抽象和标准库接口，因此很多人对内核在程序运行时所做的工作并不十分了解。如果你想了解内核被调用的频率，可以使用strace工具来显示应用程序进行的所有系统调用。

下面这个例子展示了使用echo在屏幕上显示“hello”时需要进行100多次的系统调用：

<table><tr><td colspan="6">$ strace -c echo &quot;hello&quot;</td></tr><tr><td colspan="6">hello</td></tr><tr><td>% time</td><td>seconds</td><td>usecs/call</td><td>calls</td><td>errors</td><td>yscall</td></tr><tr><td>24.62</td><td>0.001693</td><td>56</td><td>30</td><td>12</td><td>openat</td></tr><tr><td>17.49</td><td>0.001203</td><td>60</td><td>20</td><td></td><td>mmap</td></tr><tr><td>15.92</td><td>0.001095</td><td>57</td><td>19</td><td></td><td>newstatat</td></tr><tr><td>15.66</td><td>0.001077</td><td>53</td><td>20</td><td></td><td>close</td></tr><tr><td>10.35</td><td>0.000712</td><td>712</td><td>1</td><td></td><td>execve</td></tr><tr><td>3.04</td><td>0.000209</td><td>52</td><td>4</td><td></td><td>mprotect</td></tr><tr><td>2.52</td><td>0.000173</td><td>57</td><td>3</td><td></td><td>read</td></tr><tr><td>2.33</td><td>0.000160</td><td>53</td><td>3</td><td></td><td>brk</td></tr><tr><td>2.09</td><td>0.000144</td><td>48</td><td>3</td><td></td><td>munmap</td></tr><tr><td>1.11</td><td>0.000076</td><td>76</td><td>1</td><td></td><td>write</td></tr><tr><td>0.96</td><td>0.000066</td><td>66</td><td>1</td><td>1</td><td>faccessat</td></tr><tr><td>0.76</td><td>0.000052</td><td>52</td><td>1</td><td></td><td>getrandom</td></tr><tr><td>0.68</td><td>0.000047</td><td>47</td><td>1</td><td></td><td>rseq</td></tr><tr><td>0.65</td><td>0.000045</td><td>45</td><td>1</td><td></td><td>set_robust_list</td></tr><tr><td>0.63</td><td>0.000043</td><td>43</td><td>1</td><td></td><td>prlimit64</td></tr><tr><td>0.61</td><td>0.000042</td><td>42</td><td>1</td><td></td><td>set_tid_address</td></tr><tr><td>0.58</td><td>0.000040</td><td>40</td><td>1</td><td></td><td>futex</td></tr><tr><td>100.00</td><td>0.006877</td><td>61</td><td>111</td><td>13</td><td>total</td></tr></table>

由于应用程序严重依赖内核，这意味着如果我们能监控到应用程序与内核的交互，就能了解到许多关于应用程序是如何运行的信息。有了eBPF，我们就能在内核中添加监控，从而获得这些信息。

例如，如果能够截获打开文件的系统调用，就可以准确地知道每个应用程序访问了哪些文件。但如何截获呢？设想一下：如果我们想通过修改内核来实现截获功能，需要为内核添加新的代码，以便在调用该系统调用时生成特定的输出，这将会涉及哪些步骤。

# 1.6 为内核添加新功能

Linux内核非常复杂，截至本书撰写时已有大约3000万行代码。注7对任何代码库进行修改都需要对现有代码有一定的了解，所以除非你已经是一名内核开发者，否则这可能是一个挑战。

此外，如果你想将你的修改贡献给上游，你将面临的不仅仅是技术上的挑战。Linux 是一个通用操作系统，应用于各种环境和场景。这意味着，如果你希望你的修改成为 Linux 官方发布版本的一部分，不只是编写有效代码那么简单，你的更改需要获得社区（更确切地说是 Linux 的创造者和主要开发者 Linus Torvalds）的认可，并且改动对内核而言是更有益。所以在提交的内核补丁中，只有三分之一被接受。注8

假设你已经找到了一个好的技术方法来实现截获打开文件的系统调用，经过几个月的讨论和辛勤的开发工作，并且假设内核接受了这一改动。太好了！但这个改动要多久才会出现在每个人的电脑上呢？

Linux内核大约每两三个月发布一次新版本，即使某个更改已经进入了Linux版本，它离在大多数人的生产环境中可用还有一段时间，这是因为我们大多数人并不直接使用Linux内核，我们使用像Debian、Red Hat、Alpine和Ubuntu这样的Linux发行版，它们将Linux内核与各种其他组件打包在一起。你可能会发现，你最喜欢的发行版使用的内核是几年前的版本了。

例如，许多企业用户使用红帽企业版Linux（RHEL）。在本书写作时，当前的版本是2021年11月发布的RHEL8.5，它使用的是2018年8月发布的Linux内核4.18版本。

如图1-2所示，将新功能从构思阶段引入到生产环境中的Linux内核，需要花费数年时间。注9

应用开发者：

我想要这个新功能来监控我的应用程序。

![](images/b5b1199ef2ebbb035b9379c908da46f6be60f349c616bcd275f4043fc978ad0b.jpg)

一年以后

我做到了。现在上游内核支持这个功能了。

![](images/1fcccf823e34b40f448826515e340443ab6461067e937c88e4e9cf844e276438.jpg)

嗨，内核开发者。请将这个新功能添加到内核中。

![](images/7436b94fabd18147f972967c732d9abe5f6d6e7f0f2b0f902fb81bf792d6b9e4.jpg)

好的！只需要给我一年时间来说服整个社区，这对所有人都有好处。

但我需要我的Linux发行版也支持这个功能。

![](images/cdcb14b8aba896cbc299252b7b1c4b50fa340bbe73dd70114c44896f90a230a2.jpg)

五年以后

好消息，我们的Linux发行版现在提供了带有你所需功能的内核。

![](images/4dcebaad8255a611c5cc414ed906aab7bdfe8ae2f348cb02884ae92c8cb65fee.jpg)

好的，但是我的需求已经改变了，自从……

![](images/5066cc9ed2cc9397b67cf05926ef3841270de726bea5110b44f7debf9e712f0b.jpg)  
图1-2：为内核添加功能（漫画来自Isovalent公司的Vadim Shchekoldin）

# 1.7 内核模块

如果你不想等待好几年的时间才能让你的更改进入内核，还有另一种选择，Linux内核的设计支持内核模块，这些模块可以根据需要加载和卸载，如果你想改变或扩展内核行为，编写内核模块无疑也是一种方法。内核模块可以独立于Linux内核官方发布版本供他人使用，它不必被上游的主代码库所接受。

这里最大的挑战仍然完全属于内核编程的范畴。用户历来对使用内核模块非常谨慎，原因很简单：如果内核代码崩溃，它会导致整台机器及其上运行的所有内容崩溃。用户如何才能确信一个内核模块是可以安全运行的呢？

“安全运行”并不仅仅意味着不会崩溃，用户需要从安全角度了解一个内核模块是否安全。它是否包含攻击者可能利用的漏洞？我们是否信任模块的作者不会在其中加入恶意代码？因为内核是特权代码，它可以访问机器上的一切，包括所有数据，所以内核中的恶意代码将是一个严重的问题。这同样适用于内核模块。

内核的安全性也是Linux发行版需要这么长时间才能发布新版本的一个重要原因。如果一个内核版本已经被其他人在各种情况下运行了数月甚至数年，问题应该已经被清除了，那么发行版的维护者就可以确信，他们提供给用户/客户的内核是经过加固的，也就是说，运行起来是安全的。

eBPF提供了一种完全不同方法来保证安全：eBPF验证器，它确保只有在运行安全的情况下才会加载eBPF程序，不会导致机器崩溃或陷入死循环，也不会导致数据泄露。我们将在第6章中更详细地讨论该验证过程。

# 1.8 动态加载 eBPF 程序

eBPF程序可以在内核中动态地被加载和移除。一旦它们附加到某个事件上，无论是什么原因触发了该事件，它们都会被触发。例如，如果你将一个程序附加到打开文件的系统调用上，每当有进程尝试打开文件时，该程序都会被触发。当程序加载时，如果该进程已经运行了，也是可以的。相比升级内核后必须重启机器才能使用新功能，这是一个巨大的优势。

这体现出了使用eBPF作为可观测性或安全工具的一个重要优势——它可以立即知道机器上发生的一切活动。在运行容器的环境中，容器内以及宿主机上运行的所有进程对eBPF程序都是可见的。我将在本章稍后部分深入探讨云原生部署对eBPF程序的影响。

此外，如图1-3所示，人们可以通过eBPF快速地创建新的内核功能，而无需其他所有Linux用户都接受相同的更改。

应用开发者：

我想要这个新功能来监控我的应用程序。

![](images/c1b6dfd05dc1c7b51f47cd47f406392bba39b5f4dc7c29f8f4aba73b8ea78076.jpg)

eBpf开发者：

好的！内核无法做到这一点，让我快速地用eBPF来解决这个问题。

![](images/1abb212a5959b7027feb978d9e8facda1bfbdef9d89b620155ac4181bd563ca1.jpg)

几天后……

这是我们eBPF项目的发布版本，现在它已经具备了这个功能。顺便说一句，你不需要重启你的机器。

![](images/e4ab1e6bf05cdc021d2e013890d4385a8ef092e8b0daf4150bbe31bea109ad8f.jpg)

![](images/3f3fc8f0002d8d049f554792fd5144f92b4493316f038133bb7a12a701041fb3.jpg)  
图1-3：使用eBPF添加内核功能（漫画来自Isovalent公司的Vadim Shchekoldin）

# 1.9高性能的eBPF程序

eBPF程序是一种非常高效的添加监测的方式。一旦加载并经过即时编译（你将在第3章中看到），程序就以本地机器指令的形式在CPU上运行。此外，在处理每个事件时，不需要承担内核与用户空间之间切换的成本（这是一项昂贵的操作）。

2018年的一篇论文注10介绍了XDP，其中包括了一些关于eBPF在网络方面能够实现的性能提升的示例。例如，相对于常规的内核实现，在XDP中实现

的路由功能“性能提升了2.5倍”，在负载均衡方面，“XDP比IPVS性能提升了4.3倍”。

对于性能跟踪和安全可观测性来说，eBPF的另一个优势是可以在内核中过滤相关事件，而不用承担将它们发送到用户空间的成本。毕竟过滤特定的网络数据包是BPF最初实现的重点。如今，eBPF程序可以收集各种系统事件的信息，并且可以使用复杂的、定制的程序过滤器，只将相关的信息子集发送到用户空间。

# 1.10 云原生环境中的 eBPF

如今，许多组织选择不直接在服务器上执行应用程序。相反，许多人使用云原生方法：容器、类似 Kubernetes 或 ECS 编排器，或者 Lambda、云函数、Fargate 等无服务器方法。这些方法都使用自动化方法来选择工作负载将运行的服务器；在无服务器的情况下，我们甚至不知道每个工作负载在哪台服务器上运行。

然而，这些方法还是会涉及服务器，每台服务器（无论是虚拟机还是裸机）都运行着一个内核。当应用程序在容器中运行时，如果它们在同一台（虚拟）机器上运行，它们就共享同一个内核。在 Kubernetes 环境中，这意味着给定节点上所有 Pod 中的所有容器使用同一个内核。当我们使用 eBPF 程序对内核进行监测时，该节点上所有容器化的工作负载对 eBPF 程序都是可见的，如图 1-4 所示。

因为节点上所有的进程对eBPF程序可见，加上动态加载eBPF程序的能力，让我们在云原生计算中真正领略到了基于eBPF工具的超强威力：

![](images/5eac3b35729c2eedb45b75249a84b94a78be19e274a5f49e6dc97814afeed0dd.jpg)  
图1-4：内核中的eBPF程序可查看Kubernetes节点上运行的所有应用程序

- 我们不需要改变我们的应用程序，甚至不需要改变它们的配置，就可以使用eBPF工具进行监测。  
- 一旦eBPF程序加载到内核并附加到某个事件上，它就可以开始监控已存在的应用程序进程。

之前在 Kubernetes 应用中增加日志记录、跟踪、安全和服务网格等能力使用的是边车模式。在边车模式中，监测工具作为一个容器“注入”到每个应用程序 Pod 中。这个过程涉及修改定义应用程序 Pod 的 YAML，添加边车容器的定义。这种方法比将监测工具添加到应用程序的源代码中更方便（在边车方法出现之前，我们必须在我们的应用程序中包含一个日志记录库，并在代码的适当位置调用该库）。然而，边车方法也有一些缺点：

- 应用程序Pod必须重启才能添加边车。  
- 必须用某种方式修改应用程序的YAML。这通常是一个自动化过程，但如果出现问题，边车可能不会被添加，这意味着Pod不会被监测。例如，k8s的deployment可能会添加注解，来指示准入控制器将边车YAML添加到该deployment的Pod中。但如果deployment没有正确地添加注解，边车就不会被添加，因此就无法被监测工具发现。

- 当Pod中有多个容器时，它们达到就绪状态的时间可能不同，其顺序可能无法预测。注入边车可能会显著增加Pod的启动时间，或者更糟糕，可能会导致竞争条件或其他不稳定因素。例如，Open Service Mesh的文档(https://oreil.ly/z80Q5)描述了在Envoy代理容器就绪前，应用容器的所有流量都会被丢弃。  
- 当网络功能通过边车以服务网格方式被实现时，这必然意味着应用容器的所有流入和流出的流量都需经过代理容器，这一流程涉及额外的内核网络栈处理，因而会对应用流量造成延迟，如图1-5所示。我们将在第9章讨论如何使用eBPF提高网络效率。

![](images/3185aa7367930e3598e2c62526620f018163c1f62dcc7393fb81ea38548336bb.jpg)  
图1-5：使用服务网格代理边车下容器的网络数据包路径

所有这些问题都是边车模型固有的问题。幸运的是，现在有了eBPF，eBPF作为平台，我们有了一个新的模型可以避免这些问题。此外，因为基于eBPF的工具可以监控（虚拟）机器上发生的一切，这对于不法行为者来说是更难以躲避的。例如，如果攻击者设法在你的主机上部署了一个加密货币挖矿应用，他们可能不会像你对待应用工作负载一样，使用边车对其进行监测。如果你依赖基于边车的安全工具来防止应用程序进行意外的网络连接，那么如果没有注入边车，这个工具就不会发现挖矿应用连接到其挖矿池。相比之下，在

eBPF中实现的网络安全可以监控主机上的所有流量，因此可以轻松阻止这种加密货币挖矿操作。我们将在第8章再次讨论出于安全原因实现网络数据包丢弃的能力。

# 1.11总结

通过本章，你可以了解到eBPF作为一个平台是如此强大。它允许我们改变内核的行为，我们能够灵活地构建定制工具和制定自定义策略。基于eBPF的工具可以监控内核中的任何事件，因此可以监控在（虚拟）机器上运行的所有应用程序，无论它们是否是容器化。eBPF程序还可以动态部署，允许即时改变行为。

到目前为止，我们主要从概念层面上讨论了eBPF。在第2章中，我们将更具体的探索基于eBPF的应用程序的各个构成要素。

# eBPF的“Hello World”

在第1章中，我们讨论了为什么eBPF如此强大，但如果你现在还没有一个具体的认识，不太明白运行eBPF程序到底意味着什么，也没关系。在本章中，我将使用一个简单的“Hello World”示例来帮助你更好地理解它。

在阅读本书的过程中，你会了解到用于编写 eBPF 应用程序的几种不同的库和框架。作为热身，从编程角度来看，我将向你展示可能是最容易上手的方法：BCC Python 框架（https://github.com/iovisor/bcc）。它提供了一种非常简单的方式来编写基本的 eBPF 程序。尽管我不推荐你使用这种方法来编写打算发布给其他用户的应用程序，但它非常适合你迈出第一步。具体原因会在第 5 章中详述。

![](images/2abaffb43c3790fe44f3f42db21085c44a2165e86e9c272d61e6a9edfa47e8c6.jpg)

如果你想亲自尝试这段代码，可访问https://github.com/lizrice/learning-ebpf的chapter2目录。

BCC项目位于https://github.com/iovisor/bcc，安装BCC的说明位于https://github.com/iovisor/bcc/blob/master/INSTALL.md。

# 2.1 BCC 的 “Hello World”

下面是hello.py的完整源代码，它是一个用BCCPython库写的eBPF“HelloWorld”应用程序：注1

```python
#!/usr/bin/python
from bcc import BPF
program = r''
int hello(void *ctx) {
    bpf_trace_printk("Hello World!");
    return 0;
}
b = BPF(text=program)
syscall = b.get_syscall_fnname("execve")
b.attach_kprobe(event=syscall, fn_name="hello")
b(trace_print() 
```

这段代码由两部分组成：在内核中运行的eBPF程序本身，以及将eBPF程序加载到内核并读取它生成的跟踪信息的用户空间代码。如图2-1所示，hello.py是这个应用程序的用户空间部分，而hello()则是在内核中运行的eBPF程序。

让我们深入研究源代码的每一行，以便更好地理解它。

第一行表明这是Python代码，运行该程序使用Python解释器（/usr/bin/python）。

![](images/4442ff6cb2c688108afd2bdd4594ee2e80ef6a63c8fb18c5725f70805bd5b5f9.jpg)  
图2-1：“HelloWorld”的用户空间和内核部分

eBPF程序本身是用C语言编写的，就是这部分：

```c
int hello(void *ctx) { bpf_trace_printk("Hello World!"); return 0; } 
```

eBPF程序所做的只是使用helper函数bpf_trace_PRINT()来打印一条消息。helper函数是区分eBPF和前身cBPF的另一个特性。它们是一组函数，eBPF程序可以调用其与系统进行交互；我将在第5章进一步讨论它们。现在，你可以简单地将其视为打印一行文本。

整个 eBPF 程序被定义为 Python 代码中名为 program 的字符串。这个 C 语言程序需要编译后才能执行，但 BCC 可以帮你完成这项工作（第 3 章将介绍如何自己编译 eBPF 程序）。你需要做的就是在创建 BPF 对象时，将这个字符串作为参数传递，如下一行所示：

```txt
b = BPF(text=program) 
```

eBPF程序需要附加到一个事件上，在这个示例中，我选择将其附加到系统调用execve上，该系统调用用于在系统上执行程序。无论何时，只要在这台机

器上启动一个新程序, 就会调用 execve(), 从而触发 eBPF 程序。虽然“execve()”是 Linux 中的标准接口, 但在内核中实现的函数名称取决于芯片架构, BCC为我们提供了一种方便的方法来查找所运行的机器上的函数名称:

syscall $=$ b.get_syscall_fnname("execve")

其中 syscall 变量代表了使用 kprobe 将程序附加到内核函数的名称（第 1 章中介绍了 kprobes 的概念）注2。你可以像这样将 hello 函数附加到该事件上：

```txt
b.attach_kprobe(event=syscall, fn_name="hello") 
```

此时，eBPF程序已经被加载到内核中，并附加到一个事件上，所以每当机器上启动一个新的可执行程序时，该程序就会被触发。Python代码中剩下要做的就是读取内核输出的跟踪信息，并将其显示在屏幕上：

```txt
b(trace_print() 
```

这个 trace_print() 函数将无限循环打印跟踪信息，直到你使用 Ctrl+C 停止程序。

图2-2展示了这段代码的运行示意图。用户空间的Python程序会编译C语言的eBPF代码，将其加载到内核，并使用kprobe附加到execve系统调用上。每当这台（虚拟）机上的任何应用程序调用execve()时，就会触发eBPF程序hello()，将一行跟踪信息写入一个特定的伪文件（我将在本章稍后介绍该伪文件的位置）。用户空间的Python程序会从伪文件读取跟踪信息，并将其显示给用户。

![](images/fd6f3cc286c31e09be4c1113eee8c6fa8f967da06d6365ed2530f277ab132e11.jpg)  
图2-2：“HelloWorld”运行示意图

# 2.2 运行“Hello World”

运行该程序，根据你所使用的（虚拟）机上发生的情况，你可能会立即看到生成的跟踪信息，因为其他进程可能正在通过 execve 系统调用执行程序注3。如果你没有看到任何信息，打开第二个终端执行任何你喜欢的命令注4，你将看到“Hello World”生成的相应跟踪信息：

$ hello.py

b' bash-5412 [001] .... 90432.904952: 0: bpf_trace_printk: Hello World'

![](images/f9be624b80e2321779213cd4a315f9ffecce0d04e612333ba3cddd164b222210.jpg)

由于eBPF非常强大，使用它需要特权用户。因为特权会自动分配给root用户，因此运行eBPF程序最简单的方法是以root身份运行，或者使用sudo。为了清晰起见，我不会在本书的示例命令中包含sudo，但如果你遇到“Operation not permitted”的错误，首先要检查你是否试图以非特权用户身份运行eBPF程序。

内核5.8版本引入了CAP_BPF能力（capability），它为执行某些eBPF操作（如创建特定类型的map）提供了的足够权限。但是，你可能还需要其他能力：

CAPPERFMON和CAPBPF是加载跟踪程序所必需的能力。  
- CAP NET ADMIN 和 CAP BPF 是加载网络程序所必需的能力。

Milan Landaverde 在博客文章“Introduction to CAP_BPF（CAP_BPF简介）”（https://oreil.ly/G2zFO）中对此有更详细的讨论。

一旦该eBPF hello程序被加载，并附加到一个事件上，它就会被系统上已存在的进程所生成的事件触发。这里将再次提醒你在第1章中学到的几个要点：

- eBPF程序可以用来动态改变系统的行为，无需重启机器或重启现有进程。  
eBPF代码一旦附加到一个事件上，就会立即生效。  
- 其他应用程序无需改变任何内容，就可以对eBPF可见。假如，你在一台机器上有终端访问权限，如果你在机器上运行一个可执行文件，它将会使用execve()系统调用，如果你将hello程序附加到该系统调用上，该程序将被触发，以输出跟踪信息。同样，如果你有一个运行可执行文件的脚本，也会触发这个eBPF hello程序，而你不需要改变终端的shell、脚本或你正在运行的可执行文件的任何内容。

跟踪输出不仅显示了“Hello World”字符串，还显示了触发该eBPF hello程序运行的事件的一些附加上下文信息。在本节开头展示的示例输出中，执行execve系统调用的进程ID为5412，它正在运行bash命令。对于跟踪消息而言，这种上下文信息作为内核跟踪基础设施的一部分被添加进去的（这并不是eBPF所特有的），但正如你将在本章后面看到的，eBPF程序自己也可以获取类似的上下文信息。

你可能会好奇 Python 代码是如何知道从哪里读取跟踪输出的。答案并不复杂，内核中的 bpf_trace_printk() helper 函数总是将输出发送到同一个预定义的伪文件中：/sys/kernel debug/tracing/trace_pipe。你可以通过使用 cat 查看其内容来确认这一点；你需要 root 权限才能访问它。

对于一个简单的“Hello World”示例或基本的调试需求来说，单一的跟踪管道位置就足够了，但它的功能非常有限。输出格式的灵活性很小，它只支持字符串的输出，因此对于传递结构化信息来说不是特别有用。更重要的是，它在（虚拟）机器上只有一个位置，如果你有多个eBPF程序同时运行，它们都会将跟踪输出写入同一个跟踪管道，这可能会让运维人员感到非常困惑。

有一种更好的方式从eBPF程序中获取信息：使用eBPF map。

# 2.3 BPF map

map 是一种数据结构，可以从 eBPF 程序和用户空间程序中访问。map 是区分 eBPF 和前身 cBPF 的重要特性之一（它们通常被称为“eBPF map”，你也经常会看到“BPF map”。一般情况下，这两个术语可以互换）。

map 可以用来在多个 eBPF 程序之间共享数据，或者让用户空间程序和内核中运行的 eBPF 代码之间进行通信。典型的用途包括以下几点：

- 用于用户空间程序写入配置信息，供eBPF程序检索。  
- 用于eBPF程序存储状态，供其他eBPF程序(或后续运行的同一程序)检索。  
- 用于eBPF程序将结果或指标写入map中，供用户空间程序检索来展示结果。

Linux 的 uapi/linux/bpf.h 文件中定义了各种类型的 BPF map（https://oreil.ly/1s1GM），内核文档中也有一些关于它们的信息（https://oreil.ly/5oUW7）。一般来说，它们都是键值存储，在本章中你将看到 hashtable、perf 缓冲区和环形缓冲区及 eBPF 程序数组这几种类型的 map 的示例。一些 map 类型被定义为数组，使用 4 个字节的索引作为键类型。其他 map 类型是 hashtable，可以使用任意数据类型作为键。

一些 map 类型针对特定类型的操作进行了优化，例如先进先出队列（https://oreil.ly/VSoEp）、先进后出堆栈（https://oreil.ly/VSoEp）、最近最少使用数

据存储（https://oreil.ly/vpsun）、最长前缀匹配（https://oreil.ly/hZ5aM）和布隆过滤器（https://oreil.ly/DzCTK）（一种概率性数据结构，可以非常快速地返回元素是否存在的结果）。

一些eBPF的map类型保存关于特定类型对象的信息。例如，sockmaps（https://oreil.ly/UUTHO）和devmaps（https://oreil.ly/jzKYh）保存有关套接字和网络设备的信息，被网络相关的eBPF程序用于重定向流量。程序数组类型map存储了一组被索引的eBPF程序，用于实现尾调用（本章稍后将介绍，即一个程序可以调用另一个程序）。甚至还有一种用于map的map类型（https://oreil.ly/038tN），用于存储map的相关信息。

一些 map 类型为每个 CPU 核心提供了一个独立的数据存储区域，也就是说，内核为每个 CPU 核心分配不同的 map 实例和内存块。在这种情况下，多个 CPU 核心可能同时访问同一个 map，你可能好奇多个 CPU 核心同时访问 map 是否存在并发问题？因此，内核 5.1 版本中为这些 map 添加了自旋锁支持，我们将在第 5 章再次讨论这个问题。

下一个示例来自GitHub仓库中的chapter2/hello-map.py（https://github.com/lizrice/learning-ebpf），该示例展示了使用hash类型map的一些基本操作。同时，它还展示了BCC的一些便捷的抽象，让使用map变得非常容易。

# 2.3.1 hash 类型的 map

与本章前面的示例一样，这个eBPF程序使用kprobe附加到execve系统调用的入口上。它将用键值对填充一个hashtable，其中键是用户ID，值是在该用户ID下运行的进程调用execve的次数。实际上，这个示例将展示每个不同用户运行程序的次数。

首先，让我们看一下eBPF程序本身的C代码：

BPF_HASH(counter_table);

1

```txt
int hello(void *ctx) { u64 uid; u64 counter = 0; u64 *p; uid = bpf_get_current.uid_gid() & 0xFFFFFFF; p = counter_table.lookup(&uid); if (p != 0) { counter = *p; } counter++; counter_table.update(&uid, &counter); return 0; } 
```

BPF_HASH()是BCC的一个宏，用于定义hash类型的map。  
bpf_get_currentuid_gid()是一个helper函数，用于获取触发这个kprobe事件的进程的运行用户ID。用户ID保存在返回的64位值的低32位中（高32位保存的组ID，但这部分被屏蔽了）。  
③ 在hashtable中查找键与用户ID匹配的条目。它返回一个指向hashtable中相应值的指针。  
如果hashtable中有这个用户ID的条目，将counter变量设置为hashtable中的当前值（由p指向）。如果hashtable中没有这个用户ID的条目，指针将为0，counter值将保持为0。  
⑤ 无论当前 counter 值是多少，都会增加 1。  
⑥ 用该用户ID的新counter值更新hashtable。

仔细观察访问hastable的那几行代码：

```javascript
p = counter_table.lookup(&uid); 
```

以及后面的：

```javascript
counter_table.update(&uid, &counter); 
```

如果你在想“这不是标准的C代码！”那你完全正确。C语言不支持像那样

在结构上定义方法。这是一个很好的例子，展示了BCC版本的C语言是一种非常宽松地类C语言，BCC在将代码发送给编译器之前会重写它。BCC提供了一些便捷的快捷方式和宏，可以将代码转换成“标准”的C语言。

就像上一个示例一样，C 代码被定义为名为 program 的字符串。程序被编译、加载到内核，并以与之前的 “Hello World” 示例完全相同的方式，使用 kprobe 附加到 execve 上：

```txt
b = BPF(text=program)  
syscall = b.get_syscall_fnname("execve")  
b.attach_kprobe(event=syscall, fn_name="hello") 
```

这一次，Python端需要做一些额外的工作来从hashtable中读取信息：

while True: sleep(2) $s = \text{""}$ for k,v in b["counter_table"].items(): s+=f"ID{k.value}:{v.value}\t" print(s)

① 这部分代码会无限循环，每两秒钟查找一次要显示的输出。  
BCC 自动创建了一个 Python 对象来表示的 hashtable。这段代码会遍历所有值并将它们打印到屏幕上。

当你运行这个示例时，你需要在第二个终端窗口中运行一些命令。下面是我获得的一些示例输出，右侧是我在另一个终端运行的命令：

```txt
Terminal 1
$ ./hello-map.py
Terminal 2
[blank line(s) until I run something]
ID 501: 1
ID 501: 1
ID 501: 2
ID 501: 3
ID 0: 1
sudo ls 
```

注5： $\mathrm{C + + }$ 可以，但是C不可以。

<table><tr><td>ID 501: 4</td><td>ID 0: 1</td><td>ls</td></tr><tr><td>ID 501: 4</td><td>ID 0: 1</td><td></td></tr><tr><td>ID 501: 5</td><td>ID 0: 2</td><td>sudo ls</td></tr></table>

无论发生任何事情，这个示例每两秒会生成一行输出。在输出结束时，hashtable 包含两个条目：

key=501, value=5   
key=0, value=2

在第二个终端中，我的用户ID是501。以这个用户ID运行ls命令会增加execve计数器。当我运行sudo ls时，这会导致两次调用execve：一次是以用户ID501执行sudo，另一次是以root的用户ID0执行ls。

在这个示例中，我使用了hashtable从eBPF程序传递数据到用户空间程序（我也可以在这里使用数组类型的map，因为数组类型的map的键是一个整数，而hashtable允许你使用任意类型作为键）。hashtable的优势在于数据以自然形式作为键值对存储，但这样用户空间代码必须定期轮询该哈希表。Linux内核已经支持了perf子系统（https://oreil.ly/nTvvH）用于从内核发送数据到用户空间，eBPF也支持使用perf缓冲区及其替代者：BPF环形缓冲区。让我们看一下。

# 2.3.2 perf 和环形缓冲区 map

在本节中，我将介绍一个稍微复杂一点的“Hello World”版本，它使用了BCC的BPF_PERF_OUTPUT宏的功能，这允许你将数据以你选择的结构写入perf或环形缓冲区map中。

![](images/e64a4cc4bb353eaa65462548b907eabc8286a3bbfc6f333a4c2f326a21350c4a.jpg)

内核版本是5.8及以上，有一种更新的结构叫作“BPF环形缓冲区”，通常比BPFperf缓冲区更受青睐。Andrii Nakryiko在他发表的博客：BPF ring buffer（https://oreil.ly/ARRyV）中讨论了二者之间的区别。你将在第4章看到BCC的BPF_RINGBUF_OUTPUT宏的示例。

# 环形缓冲区

环形缓冲区并不是eBPF独有的，这里我还是要解释一下，以防你之前没有接触过它们。你可以将环形缓冲区想象为逻辑上以环形组织的一块内存，有独立的“写”和“读”指针。当任意长度的数据被写入写指针所在的位置，数据的长度信息包含在该数据的头部，写指针移动到该数据末尾之后，为下一次写操作做准备。

类似地，对于读操作，数据从读指针所在位置开始读取，根据头部信息来确定读取多少数据。读指针沿着与写指针相同的方向移动，指向下一个可用的数据片段。图2-3中展示了一个含有三个不同长度的可供读取的数据项的环形缓冲区。

![](images/030ed188b4b7ba2b4a0ec4e78adcbfcdc31d541fc0e02eb4947311086a72977a.jpg)  
图2-3：环形缓冲区

如果读指针追上了写指针，这只是意味着没有数据可读。如果写操作使写指针超过读指针，数据将不会被写入，并且丢弃计数器（drop counter）将会增加。读操作包括丢弃计数器，以显示自上次成功读取以来是否有数据丢失。

如果读和写操作以完全相同的速率发生，没有任何变化，并且它们总是包含相同数量的数据，那么理论上你可以只用一个刚好足够容纳该数据大小的环形缓冲区。在大多数应用中，读、写或两者之间随着时间会有一些变化，所以缓冲区大小需要调整以适应这种情况。

你可以在本书的GitHub仓库（http://github.com/lizrice/learning-ebpf）的chapter2/hello-buffer.py中找到这个示例的源代码。和本章之前看到的第一个“Hello World”示例一样，每次使用execve()系统调用时都会在屏幕上显示字符串“Hello World”。它还会查找发起每次execve()调用的进程ID和命令名称，因此这里将获得与第一个示例类似的输出。这个示例会向你展示更多BPFhelper函数。

下面是该示例中加载到内核中的eBPF程序的代码：

BPF_PERF_OUTPUT(output);   
struct data_t{ 2 int pid; int uid; char command[16]; char message[12];   
}；   
int hello(void \*ctx) { struct data_t data $=$ {； char message[12] $=$ "Hello World"; data.pid $=$ bpf_get_current.pid_tgid()>>32; data.uid $=$ bpf_get_current.uid_gid(&0xFFFFFFF); bpf_get_current_comm(&data.command, sizeof(data.Command)); bpfprobe_read_kernel(&data.message, sizeof(data.message), message); output.perf Submit(ctx，&data,sizeof(data)); return 0;

BCC定义了宏BPF_PERF_OUTPUT，用于创建一个map，该map将用于从内核传递消息到用户空间。该map命名为output。  
每次运行hello()时，会将数据写入到一个结构体。这是该结构体的定义。它包含了进程ID、用户ID、当前运行命令的名称和一个文本消息。  
data是一个本地变量，用于保存要提交的数据结构，message保存了“Hello World”字符串。  
bpf_get_current pid_tgid() 是一个 helper 函数，用于获取触发此 eBPF 程序运行的进程 ID。它返回一个 64 位值，进程 ID 在高 32 位。注 6  
bpf_get_current uid_gid()是上一个示例中用于获取用户ID的helper函数。  
⑥ 同样地，bpf_get_current_comm() 是一个helper函数，用于获取在发起 execve 系统调用的进程中运行的可执行文件（或“命令”）的名称。这是一个字符串，不像进程 ID 和用户 ID 那样是一个数值，在 C 语言中你不能简单地使用 = 赋值一个字符串。你必须将字符串所在字段的地址 (&data command) 作为参数传递给 helper 函数。  
⑦ 在这个示例中，每次消息都是“Hello World”。bpfprobe_read_kernel()将其复制到数据结构中的正确位置。  
此时，数据结构已经填充了进程ID、用户ID、命令名称和消息。调用output.perf_submit()会将这些数据写入map中。

就像第一个“Hello World”示例一样，这个C程序被赋值给Python代码中名为program的字符串中。以下是Python代码的其余部分：

```txt
b = BPF(text=program)  
syscall = b.get_syscall_fnname("execve")  
b.attach_kprobe(event=syscall, fn_name="hello") 
```

```python
def print_event.cpu, data, size): ②  
    data = b["output"].event(data)  
    print(f"[data.pid] {data.uid} {data.commanddecode()} " + \f"[data.messagedecode()])  
b["output"].open_perf_bufferprint_event) ③  
while True: ④  
    b_perf_buffer_poll() 
```

① 编译C代码，将其加载到内核，并将其附加到系统调用事件，这一行与你之前看到的“Hello World”版本没有变化。  
$\bullet$ print_event 是一个回调函数，用于把一行数据输出到屏幕上。BCC 做了一些复杂的操作，以便可以简单地通过 b["output"] 引用 map，并使用 b["output"].event() 从中获取数据。  
b["output"].open_perf_buffer()打开了perf环形缓冲区。该函数以print_event作为参数，用于定义每当从缓冲区读取数据时使用的回调函数。  
程序将无限循环，轮询perf环形缓冲区。如果有数据可用，将调用print_event。

运行这段代码的输出结果与最初的“Hello World”相似：

```txt
$ sudo ./hello-buffer.py
11654 node Hello World
11655 sh Hello World
...
... 
```

和之前一样，你可能需要在同一台（虚拟）机器上打开第二个终端，运行一些命令来触发一些输出。

这个示例与原始的“Hello World”示例的主要区别在于，数据不是通过单一的、中心化的 trace_pipe 传递，而是通过一个名为 output 的环形缓冲区 map 传递，这个 map 是由程序为自己使用创建的，如图 2-4 所示。

![](images/ae23c64b41ef492ae321e57aaf2f68bd97bc7296fa5274045de295521a925e9a.jpg)  
图2-4：使用perf环形缓冲区从内核向用户空间传递数据

你可以使用cat/sys/kernel debug/tracing/trace_pipe来验证信息没有传递到trace_pipe中。

除了展示环形缓冲区 map 的使用外，这个示例还展示了一些 eBPF-helper 函数，用于获取触发 eBPF 程序运行的事件的上下文信息。在这里，你看到了获取用户 ID、进程 ID 和当前命令名称的 helper 函数。正如你将在第 7 章看到的，程序的类型和触发它的事件类型，决定了可用的上下文信息集及可用于获取这些信息的有效 helper 函数集。

事实上，这些供eBPF代码使用的上下文信息，正是eBPF在可观测性方面如此有价值的原因。当事件发生时，eBPF程序不仅可以报告事件的发生，还可以报告触发事件的相关信息。所有这些信息都可以在内核中收集，无需同步切换上下文到用户空间，所以性能很高。

在本书中，你还将看到用于收集其他上下文数据的 eBPF helper 函数的例子，以及 eBPF 程序改变上下文数据，或甚至完全阻止事件发生的示例。

# 2.3.3 函数调用

如前所述，eBPF程序可以调用内核提供的helper函数，但如果你想将编写的代码拆分成多个函数呢？通常，在软件开发中，将公共代码提取到一个函数中，然后从不同地方调用这个函数，而不是重复复制相同的代码行，这被认为是一个好习惯。注8但在早期，eBPF程序不允许调用除helper函数之外的函数。为了解决这个问题，程序员指示编译器“always inline（始终内联）”他们的函数，像这样：

static __always_inline void my_function(void *ctx, int val)

通常，源代码中的函数调用会导致编译器发出跳转指令，这会导致执行跳转到调用函数的一组指令上，并在该函数完成后再跳转回来，图2-5的左侧对此进行了说明。右侧展示了函数被内联时会发生什么：没有跳转指令，而是在调用函数内直接内嵌函数的指令副本。

![](images/68752a5ba836a60f09d0f6be6b06abed76ff6f7a2b95680ec7fb46d639bd39f1.jpg)

![](images/4083d12c5522fe3949eac6a0e3bacee13d9dd3e1124e3c24112e50faba909580.jpg)  
图2-5：非内联和内联函数指令的结构

如果函数在多个地方被调用，这会导致编译后的可执行文件中包含该函数指令的多个副本（有时，编译器可能出于优化目的选择内联函数，这也是无法使用 kprobe 机制将 BPF 程序“附加”到某些内核函数上的原因之一。我将在第 7 章再谈这个问题）。

从Linux内核4.16和LLVM6.0开始，函数必须内联的限制被取消，eBPF程序员就能更自然地编写函数调用，这个被称为“BPF到BPF函数调用”或“BPF子程序”的功能，因为目前BCC框架不支持，所以我们将在下一章再讨论它，当然，你仍然可以在BCC中使用内联函数。

在eBPF中，还有另一种机制用于实现复杂功能分解，即尾调用。

# 2.3.4尾调用

正如在ebpf.io(https://oreil.ly/Loyuz)中所描述的，“尾调用可以调用并执行另一个eBPF程序，并替换执行上下文，这类似于execve()系统调用用于执行常规进程的操作。”换句话说，尾调用完成后，执行不会返回给调用者。

![](images/08a476e77a49acb5a3fe6ed1994fff7bb8f7660f3d3c757408a674cd16b486df.jpg)

尾调用（https://oreil.ly/cOA1r）并不是eBPF编程独有的。尾调用的主要目的是避免在函数递归调用过程中反复向调用栈添加帧，以防止最终发生栈溢出错误。如果你能编写代码使递归函数调用作为其执行的最后操作，那么与该调用函数相关的栈帧实际上不会执行任何操作。在eBPF中，尾调用允许调用一系列函数，而不增加堆栈的大小，这是特别有用，因为其堆栈大小限制为512字节（https://oreil.ly/SZmkd）。

尾调用是通过使用bpf.tail_call()helper函数进行的，该函数具有以下签名：

long bpf_tail_call(void *ctx, struct bpf_map *prog_array_map, u32 index)

该函数的三个参数含义如下：

- ctx 允许将上下文由调用 eBPF 程序传递给被调用程序。

- prog_array_map 是一个 BPF_MAP_TYPE_PROG_ARRAY 类型的 eBPF map，它保存一组标识 eBPF 程序的文件描述符。  
- index 表示应调用该组 eBPF 程序中的哪一个。

这个helper函数与一般的函数不同。如果它执行成功了，它永远不会返回，当前堆栈上运行的eBPF程序被被调用的程序所替换。如果helper函数可能会失败，例如，如果指定的程序在map中不存在，这种情况下，调用程序将继续执行。

用户空间代码必须将所有eBPF程序加载到内核中（像往常一样），同时还需要设置BPF程序数组map。

让我们来看一个使用BCC Python编写的简单示例；你可以在GitHub仓库（http://github.com/lizrice/learning-ebpf）中找到chapter2/hello-tail.py。eBPF主程序附加到所有系统调用的公共入口点的跟踪点上。这个程序使用尾调用来为特定的系统调用操作码输出指定消息信息。如果给定操作码没有尾调用，程序会跟踪输出一条默认消息。

如果使用 BCC 框架进行尾调用 (https://oreil.ly/rT9e1)，你可以使用稍微简单的形式：

```txt
prog_array_map.call(ctx, index) 
```

在将代码传递给编译之前，BCC会将这一行重写为这样：

```txt
bpftail_call(fty，prog_array_map,index)
```

以下是eBPF程序和尾调用的源代码：

```c
BPF_PROG_ARRAY(syscall, 300);  
int hello(struct bpf_raw_tracepoint_args *ctx) {  
    int opcode = ctx->args[1];  
    syscall.call(ctx, opcode);  
} 
```

```c
bpf_trace_printk("Another syscall: %d", opcode); return 0;   
}   
int hello_execve(void *ctx) { bpf_trace_printk("Executing a program"); return 0;   
}   
int hello_timer(struct bpf_raw_tracepoint_args *ctx) { if (ctx->args[1] == 222) { bpf_trace_printk("Creating a timer"); } else if (ctx->args[1] == 226) { bpf_trace_printk("Deleting a timer"); } else { bpf_trace_printk("Some other timer operation"); } return 0;   
}   
int ignore_opcode(void *ctx) { return 0; 
```

BCC 提供了一个 BPF_PROG_ARRAY 宏，用于轻松定义 BPF_MAP_TYPE_PROG_ARRAY 类型的 map。该 map 叫 syscall，设置了 300 个条目，注 9 这对于本例来说足够了。  
在你即将看到的用户空间代码中，将把这个eBPF程序附加到rawtracepoint的sys-enter上，每当进行任何系统调用时，都会触发这个跟踪点。附加到rawtracepoint的eBPF程序接收的上下文是bpf_raw_tracepointargs结构体。  
在sys-enter的情况下，rawtracepoint参数包括系统调用的操作码，用于识别正在进行的系统调用。  
④ 这里我们根据BPF程序数组中的键匹配系统调用的操作码进行尾调用。在将源代码传递给编译器之前，BCC将这行代码重写为对bpf_tail_call()helper函数的调用。

如果尾调用成功，跟踪输出操作码的这行将永远不会被执行。如果 map 中没有匹配的操作码，程序将提供默认的跟踪行。  
$\bullet$ hello_exec()是加载到syscall map中的一个程序，当系统调用操作码显示是execve()系统调用时，它将作为尾调用被执行。它只会生成一行跟踪信息，告诉用户一个新程序正在执行。  
- hello_timer() 是另一个加载到 syscall map 中的程序。在这种情况下，它将被 syscall map 中的多个条目所引用。  
⑧ ignore_opcode() 是一个什么也不做的尾调用程序。我使用它来处理那些我不希望生成任何跟踪信息的系统调用。

现在我们来看看加载和管理这组eBPF程序的用户空间代码：

```lua
b = BPF(text=program)  
b.attach_raw_tracepoint(tp="sys-enter", fn_name="hello")  
ignore_fn = b.load_func("ignore_opcode", BPF.MAX_TRACEPOINT)  
exec_fn = b.load_func("hello_exec", BPF.MAX_TRACEPOINT)  
timer_fn = b.load_func("hello_timer", BPF.MAX_TRACEPOINT)  
prog_array = b.get_table("syscall")  
prog_array[ct.c_int(59)] = ct.c_int exec_fn.fd)  
prog_array[ct.c_int(222)] = ct.c_int timer_fn.fd)  
prog_array[ct.c_int(223)] = ct.c_int timer_fn.fd)  
prog_array[ct.c_int(224)] = ct.c_int timer_fn.fd)  
prog_array[ct.c_int(225)] = ct.c_int timer_fn.fd)  
prog_array[ct.c_int(226)] = ct.c_int timer_fn.fd)  
# Ignore some syscall that come up a lot  
prog_array[ct.c_int(21)] = ct.c_int ignore_fn.fd)  
prog_array[ct.c_int(22)] = ct.c_int ignore_fn.fd)  
prog_array[ct.c_int(25)] = ct.c_int ignore_fn.fd)  
...  
b.trace_print() 
```

① 与你之前看到的附加到 kprobe 的程序不同，这次用户空间代码将主 eBPF 程序附加到 sys-enter 跟踪点上。

这些对b.load_func()的调用为每个尾调用程序返回一个文件描述符。请注意，尾调用需要与其父程序具有相同的程序类型，在本例中是BPF. RAW_TRACEPOINT。另外，值得指出的是，每个尾调用程序本身就是一个独立的eBPF程序。  
③ 用户空间代码会在syscall map中创建条目。map不需要用每个可能的操作码进行完全填充。如果某个操作码没有条目，这只是意味着不会执行尾调用。此外，多个条目指向同一个eBPF程序也是完全可以的。在本例中，我希望与timer相关的任意系统调用都将执行hello_timer()尾调用。  
④ 系统频繁执行的一些系统调用产生的大量跟踪信息，使得跟踪输出变得过于杂乱，以至于难以阅读。使用ignore_opcode()尾调用可以忽略一些系统调用。  
⑤ 将跟踪输出打印到屏幕上，直到用户终止程序。

运行这个程序会生成（虚拟）机器上每个系统调用的跟踪输出，除非该操作码有一个与ignore_opcode()尾调用相关的条目。这是在另一个终端运行ls时的一些输出示例（为便于阅读，省略了一些细节）：

```txt
./hello-tail.py   
b' hello-tail.py-2767 ...Another syscall:62'   
b' hello-tail.py-2767 ...Another syscall:62'   
...   
b' bash-2626 ...Executing a program'   
b' bash-2626 ...Another syscall:220'   
...   
b' <...>-2774 ...Creating a timer'   
b' <...>-2774 ...Another syscall:48'   
b' <...>-2774 ...Deleting a timer'   
...   
b' ls-2774 ...Another syscall:61'   
b' ls-2774 ...Another syscall:61' 
```

哪些系统调用正在执行并不重要，你可以看到不同的尾调用正在被调用，并生成跟踪消息。你还可以看到 map 中没有尾调用条目的操作码，会打印默认消息“Another syscall”。

![](images/2e1c54bb0fad50e715be6cedab80c75a709cb8638507b7bbea627cd5acb1cbdf.jpg)

关于不同内核版本上BPF尾调用成本，请查看Paul Chaignon的博文（https://oreil.ly/jTxcb）。

自内核4.2版本以来，eBPF就支持了尾调用。但长时间以来尾调用与“BPF到BPF函数调用”不兼容，这一限制在内核5.10中被取消。注10

你可以将多达33个尾调用链接在一起，再加上每个eBPF程序的指令复杂度限制增加到100万条指令，这意味着今天eBPF程序员有很大的自由度来编写非常复杂的代码，这些代码完全是在内核中运行。

# 2.4总结

本章通过演示一些eBPF程序的具体示例，帮助你理解关于在内核中运行eBPF代码以及事件触发的模型。同时，你还看到了如何使用BPF map从内核传递数据到用户空间的示例。

使用BCC框架隐藏了许多关于程序是如何构建的、如何加载到内核中，以及如何附加到事件上的细节。在下一章中，将向你展示编写“Hello World”的不同方法，并深入探讨其中隐藏的细节。

# 2.5 练习

如果你想进一步探索“Hello World”，这里有一些可尝试或思考的可选活动：

1. 修改eBPF hello-buffer.py程序，使奇数和偶数进程ID输出不同的跟踪信息。

2. 修改hello-map.py，使eBPF代码可以被多个系统调用触发。例如，openat()通常用于打开文件，write()用于将数据写入文件。你可以先尝试使用kprobes将eBPF hello程序附加到多个系统调用上。然后再尝试运行多个修改过的eBPF hello程序版本，分别“附加”到不同的系统调用上，以展示你可以从多个不同的程序中访问同一个map。

3. eBPF hello-tail.py 程序是一个附加到 raw_tracepoint 的 sys-enter 上的程序示例，任何系统调用的调用都会触发该跟踪点。修改 hello-map.py，使其也“附加”到 raw_tracepoint 的 sys-enter 上，来展示每个用户 ID 所做的系统调用总数。

以下是做出修改后一些示例得到的输出：

<table><tr><td colspan="6">$ ./hello-map.py</td></tr><tr><td>ID 104: 6</td><td>ID 0: 225</td><td></td><td></td><td></td><td></td></tr><tr><td>ID 104: 6</td><td>ID 101: 34</td><td>ID 100: 45</td><td>ID 0: 332</td><td>ID 501: 19</td><td></td></tr><tr><td>ID 104: 6</td><td>ID 101: 34</td><td>ID 100: 45</td><td>ID 0: 368</td><td>ID 501: 38</td><td></td></tr><tr><td>ID 104: 6</td><td>ID 101: 34</td><td>ID 100: 45</td><td>ID 0: 533</td><td>ID 501: 57</td><td></td></tr></table>

4. BCC 提供的 RAW_TRACEPOINT_PROBE 宏（https://oreil.ly/kh-j4）简化了“附加”到 raw_tracepoint 的过程，用户空间的 BCC 代码会自动将 BPF 程序“附加”到指定的跟踪点上。尝试在 hello-tail.py 中这样做：

- 将定义的 hello() 函数替换为 RAW_TRACEPOINT_PROBE(sys-enter)。  
- 从Python代码中移除显式的附加调用b.attach_raw_tracepoint()。

你应该会看到，使用RAW_TRACEPOINT_PROBE宏，BCC会自动“附加”到指定的跟踪点上，程序的工作方式完全相同。这是BCC提供的众多便捷宏中的一个示例。

5. 你可以进一步修改 hello_map.py，在hashtable中将键定义特定的系统调用（而不是特定的用户），输出将显示整个系统中调用该系统调用的总次数。

# 第3章

# eBPF 程序解析

在第2章中，我们介绍了一个使用BCC框架编写的简单eBPF“HelloWorld”程序。本章，我们将通过一个纯C语言编写的“HelloWorld”示例程序，揭示BCC处理过程中的一些细节。

同时，本章还会向你展示eBPF程序从源代码到可执行状态的全过程，如图3-1所示。

![](images/5c2ca9efc5f948a1965afc22c99f32c3250d7c5b42d2882b08908c739e39391f.jpg)  
图3-1：C语言（或Rust）源代码首先被编译成eBPF字节码，随后通过JIT编译转换为本地机器码指令

eBPF程序由一系列eBPF字节码指令组成。我们可以直接使用字节码编写eBPF代码，这类似于使用汇编语言进行编程。然而，人们通常认为使用高级编程语言更为方便。截至本书撰写时，我相信大多数eBPF代码都是用C语言编写，然后编译成eBPF字节码。注1

理论上，这些字节码在内核的eBPF虚拟机中执行。

# 3.1 eBPF虚拟机

eBPF虚拟机与其他虚拟机一样，是计算机的软件实现。它能接受eBPF字节码指令形式的程序，并将其转换为可在CPU上运行的本地机器指令。

在eBPF的早期实现中，字节码指令是在内核中解释的，也就是说，每次运行eBPF程序时，内核都会检查字节码指令并将其转换为机器码，然后执行机器码。由于性能原因，以及为了避免eBPF解释器中可能引入Spectre漏洞，解释执行方式已在很大程度上被JIT（即时编译）技术所替代。JIT编译意味着字节码到本地机器指令的转换只在程序被加载到内核时执行一次。

eBPF字节码包含一系列eBPF寄存器的指令。eBPF指令集和寄存器模型的设计与常见的CPU架构保持完全一致，这样使得将字节码编译（或解释）为机器码的步骤相当简单。

# 3.1.1 eBPF 寄存器

eBPF虚拟机有10个通用寄存器，编号从 $0\sim 9$ 。此外，第10号寄存器专用于保存堆栈帧指针，它是只读的，不能被写入。在eBPF程序执行期间，这些寄存器用于存储数值，以追踪程序状态。

需要注意的是，eBPF虚拟机的寄存器是通过软件模拟实现的。你可以在Linux内核的源代码头文件include/uapi/linux/bpf.h（https://oreil.ly/_ZhU2）中找到从BPF_REG_0到BPF_REG_10的寄存器枚举。

eBPF程序执行前，context参数会被加载到寄存器1中。而函数的返回值则会被存放在寄存器0中。

当调用eBPF程序中的函数时，函数的参数将被放入寄存器1至寄存器5中。如果参数少于5个，那么这些寄存器不会全部被占用。

# 3.1.2 eBPF指令

在linux/bpf.h头文件（https://oreil.ly/_ZhU2）中，定义了一个名为bpf_insn的结构体，用于表示BPF指令：

```c
struct bpf_insn {
    __u8 code; /* opcode */
    __u8 dst_reg:4; /* dest register */
    __u8 src_reg:4; /* source register */
    __s16 off; /* signed offset */
    __s32 imm; /* signed immediate constant */
}; 
```

- 每条指令都有一个操作码（opcode），它定义了指令要执行的操作：例如，将一个值添加到寄存器中是一条指令，跳转到程序中是另一条指令注2。Iovisor项目的“Unofficial eBPF spec”（非官方eBPF规范，https://oreil.ly/FXcPu）中列出了有效指令的清单。  
不同的操作可能涉及两个寄存器。  
根据不同的指令操作，可能会包含一个偏移值（offset）和一个立即数（immediate）两个整型值。

bpf_insn结构体的长度为64位，即8字节。然而，某些情况下，一条指令的长度可能超过8字节，如果需要将一个寄存器设置为64位的值，就无法将全部64位的这个值、操作码以及寄存器信息一起压缩进这个结构中。在这种情况下，会采用总长度为16字节的宽指令编码方式。本章的后续内容将通过示例进行说明。

当加载到内核时，eBPF程序的字节码由一系列bpf_insn结构体组成。验证器将对这些信息进行多次校验，以确保代码可以安全运行。在第6章中，你将了解到更多关于验证过程的详细信息。

大多数的操作码可以归入以下几个类别：

- 将数值加载到寄存器中（包括立即数的值、从内存读取的数值或从其他寄存器读取的数值）。

- 将寄存器中的数值存到内存中。

- 执行算术运算，如对寄存器的内容加一个数值。

- 在满足特定条件时跳转到另一条指令。

![](images/634d4c541b30a8e295066dd1709e8467662dbb413d516d48ae0864dccc4baebf.jpg)

如果你对eBPF架构的概览感兴趣，我推荐阅读《BPF和XDP参考指南》（https://oreil.ly/rvm1i），这是Cilium项目文档的一部分。若你希望深入了解更多细节，内核文档（https://oreil.ly/_2XDT）对eBPF的指令和编码提供了非常详细的解释。

让我们以一个简单的 eBPF 程序为例，来探讨它从 C 语言源代码到 eBPF 字节码，再到机器码指令的转换过程。

![](images/e57a50edce89de97d0050162c19b808228a484ded3871f0a9bb0e1e4e53241a4.jpg)

如果你有兴趣自己编译和运行这段代码，可以在 github.com/lizrice/learning-ebpf上找到相关代码及环境配置的指南。本章的代码存放在 chapter3 目录下。示例程序使用 C 语言编写，并采用了名为 libbpf 的库。有关 libbpf 库的更多信息，请参考第 5 章。

# 3.2针对网络接口的eBPF“HelloWorld”示例程序

在上一章的示例中，“Hello World”跟踪程序是使用 kprobe 由系统调用触发的；  
而在本章，我将展示另一个 eBPF 程序，它会在网络数据包到达时记录一行跟踪信息。

数据包处理是eBPF应用中非常常见的一个场景。尽管我们将在第8章中详细讨论此主题，这里将先介绍一下这类eBPF程序的基本概念。这类程序能够检

查甚至修改数据包的内容，并对内核应如何处理这些数据包进行决策（或评估），评估结果可以指导内核正常处理数据包、丢弃数据包或将其重定向至其他位置。

在展示的这个简单示例中，程序并不对网络数据包进行任何处理；它仅仅在每次接收到网络数据包时，向trace_pipe输出“Hello World”和一个计数器的值。

将eBPF程序保存在以.bpf.c为后缀的文件名中是一个常见的做法，这样做可以将其与可能存在于同一目录下的用户空间C代码区分开来。以下是程序的完整内容：

```c
include <linux/bpf.h> 1  
#include <bpf/bpfhelpers.h>  
int counter = 0; 2  
SEC("xdp") 3  
int hello(void *ctx) { 4  
    bpf_printk("Hello World %d", counter);  
    counter++;  
    return XDP_PASS;  
}  
char LICENSE[] SEC("license") = "Dual BSD/GPL"; 5 
```

$\bullet$ 本示例首先引入了一些头文件。如果你熟悉C语言编程，你会知道每个程序都必须包含头文件，这些头文件为程序使用的任何结构或函数提供了定义。从这些头文件的名称可以推测出，它们与BPF相关。  
$\bullet$ 本示例演示了eBPF程序如何使用全局变量。每次程序执行时，该计数器都会增加。  
SEC()宏定义为xdp，这点在编译后的对象文件中也能看到。第5章将详细讲述SEC名如何使用。目前，你只需要知道它用于定义一种XDP（eXpress Data Path）类型的eBPF程序。  
下面是一个实际的eBPF程序。在eBPF中，程序的名称即为函数名，因此这个程序被命名为hello。它利用bpf_printk()helper函数打印一段文

本，并使全局变量计数器 counter 增 1，随后返回 XDP_PASS。这一返回值指示内核该网络数据包应当按常规流程处理。

最后，SEC()宏还定义了许可证字符串，这是eBPF程序的一个重要要求。内核中的一些BPF hepler函数被标记为“仅限GPL”，如果你想使用这些函数中的任何一个，你的BPF代码必须声明为拥有与GPL兼容的许可证。如果声明的许可证与程序使用的函数不兼容，验证器（将在第6章讨论）会报告错误。某些eBPF程序类型，包括使用BPF LSM的程序（将在第9章介绍），同样需要与GPL兼容（https://oreil.ly/ItahV）。

![](images/58bf9cb18ade8ab236a2fcb6b3bae4f128bc8eb7489e42ba8e7a7f1d63bfe80d.jpg)

你可能好奇为什么上一章使用了bpf_trace(Printk()，而本章则采用了bpf(Printk()。简单来说，bpf_trace(Printk()是BCC版本的命名，而bpf(Printk()则是libbpf版本的命名，但实际上这两者都基于内核函数bpf_trace(Printk()实现的。Andrii Nakryiko在他的博客中撰写了一篇很好的文章（https://oreil.ly/9mNSY），对此进行了详细介绍。

以上是一个eBPF程序示例，它附加在网络接口的XDP钩子上。当网络数据包进入（无论是物理还是虚拟的）网络接口时，就会触发XDP事件。

![](images/ca862b12a3412465f628548f8f384edccb687fdbb9cf46007eabc5c61f77f960.jpg)

一些网卡支持XDP offloading（卸载），允许程序直接在网卡上执行。这意味着，网络数据包可在到达主机CPU之前，就已在网卡层面得到处理。XDP程序可以对每个网络数据包进行检查甚至修改，因此对于执行DDoS防护、防火墙或负载均衡等高性能任务非常有效。你将在第8章中了解到更多相关信息。

至此，你已经熟悉了eBPF程序的C语言源代码，接下来的步骤是将其编译成内核能够识别的对象文件。

# 3.3编译eBPF对象文件

eBPF源代码需编译成eBPF虚拟机能识别的机器指令，即eBPF字节码。通

过指定-target bpf 选项，LLVM 项目（https://llvm.org）中的 Clang 编译器便可完成此编译工作。以下是用于编译的 Makefile 摘要：

```makefile
hello.bpf.o:%.o:%.c
clang \
-target bpf \
-I/usr/include/$(shell uname -m)-linux-gnu \
-g \
-02 -c $< -o $@ 
```

上面的命令将会把hello.bpf.c源代码编译生成一个名为hello.bpf.o的对象文件。这里，-g标志是可选的注3，它可以生成调试信息，使得在查看对象文件时，能够查看到与字节码对应的源代码。接下来，让我们来查看这个目标文件，以便更深入地了解其中包含的eBPF代码内容。

# 3.4 查看 eBPF 对象文件

使用 file 工具查看 eBPF 对象文件内容：

```perl
$ file hello.bpf.o
hello.bpf.o: ELF 64-bit LSB relocatable, eBPF, version 1 (SYSV), with debug_info,
not stripped 
```

这说明该文件是一个 ELF（Executable and Linkable Format）文件，该文件含有 eBPF 代码，适用于 LSB（Least Significant Bit）架构的 64 位平台。如果编译时使用了 -g 标志，它还会包含调试信息。

你可以使用llvm-objdump工具来进一步查看该对象文件中包含的eBPF指令：

```txt
$ llvm-objectdump -S hello.bpf.o 
```

即使你不熟悉反汇编，这条命令的输出结果也不难理解：

```txt
hello.bpf.o: file format elf64-bpf ①   
Disassembly of section xdp: ②   
000000000000000 <hello>: ③   
; bpf_printk("Hello World %d", counter"); ④   
0: 18 06 00 00 00 00 00 00 00 00 00 00 00 00 00 r6 = 0 l1   
2: 61 63 00 00 00 00 00 00 00 r3 = *(u32 *)(r6 + 0)   
3: 18 01 00 00 00 00 00 00 00 00 00 00 00 00 00 r1 = 0 l1   
5: b7 02 00 00 of 00 00 00 r2 = 15   
6: 85 00 00 00 06 00 00 call 6   
; counter++; ⑤   
7: 61 61 00 00 00 00 00 r1 = *(u32 *)(r6 + 0)   
8: 07 01 00 00 01 00 00 00 r1 += 1   
9: 63 16 00 00 00 00 00 00 * (u32 *) (r6 + 0) = r1   
; return XDP_PASS;  
1O: b7 OOOO O2 OOO Ooo rO =2   
11:95 OOOOOOOo ooo exit 
```

$\bullet$ 第一行语句进一步验证hello.bpf.o是一个含有eBPF代码的64位ELF文件。不同工具对 $BPF$ 和eBPF的称呼不一，却没有特别的理由。正如我之前提到的，这些术语现在实际上已经可以互换使用。  
接下来是SEC("xdp")的反汇编语句，与C源代码中的SEC()定义相匹配。  
这里表示这是一个名为hello的函数。  
这里的5行eBPF字节码指令对应于源代码中的bpf_printk("Hello World %d", counter")。  
这里的3行eBPF字节码指令对应于 $\mathrm{count + + }$   
⑥ 另外2行字节码对应于源代码中的return XDP_PASS。

你不需要精确理解每一行字节码和源代码之间的对应关系，因为编译器负责生成字节码，所以这些细节可以不必深究！不过，让我们仔细查看一下输出的内容，以便能够理解输出与本章前面所介绍的eBPF指令及寄存器之间的联系。

每行字节码指令的左侧是该指令与内存中hello程序所在位置的偏移量。正如本章前面所述，eBPF指令的长度一般为8字节，由于在64位平台上，每个

内存位置可容纳8字节，因此通常每个指令的偏移量都会递增一个。然而，本程序中的第一条指令恰好是宽指令编码，需要16个字节，寄存器6设置为64位的0值。之后是另一条16字节的宽指令是第三条指令，寄存器1设置为64位的0值。

每行的首个字节代表操作码（opcode），用于指示内核需要执行的操作，而每条指令行的右侧是该指令的可读解释。

截至本书撰写之时，Iovisor项目提供了最全面的eBPF操作码文档（https://oreil.ly/nLbLp）。与此同时，Linux内核的官方文档（https://oreil.ly/ypjW）对此也在迅速完善中。此外，eBPF基金会正致力于编制一套与特定操作系统无关的BPF标准文档（https://oreil.ly/7ZWzj）。

以偏移量5的指令为例，如下所示：

5: b7 02 00 00 of 00 00 00 r2 = 15

操作码为 $0 \times 17$ ，根据文档，这对应的伪代码是 dst = imm，意味着“将目标寄存器设置为立即数的值”。目标寄存器由第2个字节 $0 \times 02$ 指定，代表“寄存器2”。这里的“立即数”值为 $0 \times 0f$ ，即十进制的15。因此，这条指令的含义是“将寄存器2的值设置为15”，这与指令右侧显示的输出：r2 = 15相对应。

位于偏移量10的指令也是类似的情况：

10: b7 00 00 00 02 00 00 00 r0 = 2

这行代码的操作码同样是0xb7，此次操作将寄存器0的值设为2。当eBPF程序执行完毕时，寄存器0用于存储返回码，而XDP_PASS的值正是2。这与源代码中的return XDP_PASS;语句相匹配。

现在，你已经知道hello.bpf.o中包含了eBPF程序的字节码指令。接下来的步骤是将它加载到内核里。

# 3.5 将程序载入内核

在本例中，我们选用了 bpftool 工具进行操作。当然，你亦可通过编写自己的程序来加载 eBPF 程序，在本书的后续章节中将提供相关的示例。

![](images/f2fee404731abba496fd194cb3bbfd515c290cba552496cba6e027e04ac75ffa.jpg)

一些Linux发行版提供了bpftool软件包，你也可以选择从源代码（https://github.com/libbbpf/bpftool)进行编译安装。关于安装或编译此工具的更多信息，你可以参考Quentin Monnet的博客（https://oreil.ly/Yqepv），同时，Cilium网站（https://oreil.ly/rrnTlg）上也提供了丰富的文档和使用指南。

以下是一个示例，演示了如何使用 bpftool 工具将 eBPF 程序加载到内核。请注意，执行此操作需要 root 权限（或使用 sudo）以获取必要的执行 BPF 程序的权限。

$ bpftool prog load hello.bpf.o /sys/fs/bpf/hello

此操作会将编译好的eBPF对象文件加载到内核中，并将其Pin（固定）在/sys/fs/bpf/hello文件系统上注4。如果命令执行后没有任何输出，表示加载成功。你可以通过1s命令确认程序是否已正确加载。

$ ls /sys/fs/bpf hello

至此，eBPF程序已经成功加载到内核中。接下来，我们将使用bpftool工具来深入探索程序及其在内核中的状态。

# 3.6 查看已加载的程序

bpftool工具能够展示所有加载至内核的BPF程序。如果你亲自尝试操作，输出结果中可能会包含一些主机上已经存在的eBPF程序。为了清晰起见，这

里我们仅展示与“HelloWorld”示例相关的内容：

```txt
$ bpftool prog list
...
540: xdp name hello tag d35b94b4c0c10efb gpl
loaded_at 2022-08-02T17:39:47+0000 uid 0
xlated 96B jited 148B memlock 4096B map_ids 165,166
btf_id 254 
```

程序的 ID 是 540。这个唯一标识是在每个程序加载时由系统分配的一个数字。有了这个 ID，你就能使用 bpftool 来查询并展示该程序的更多详细信息。下面我们采用 --pretty 参数，以 JSON 格式输出结果，使得字段名和值都一目了然：

```json
$ bpftool prog show id 540 --pretty
{
    "id": 540,
    "type": "xdp",
    "name": "hello",
    "tag": "d35b94b4c0c10efb",
    "gplCompatible": true,
    "loaded_at": 1659461987,
    "uid": 0,
    "bytes_xlated": 96,
    "jited": true,
    "bytes_jited": 148,
    "bytes_memlock": 4096,
    "map_ids": [165,166],
    "btf_id": 254
} 
```

根据字段名称，许多信息都很容易理解：

- 程序的 ID 为 540。  
- type字段显示程序是通过XDP事件“附加”到网络接口。第7章将详细讨论其他几种“附加”到不同事件的BPF程序类型。  
- 程序名称为hello，这与源代码中的函数名相对应。

- tag 是程序的另一种标识形式，详细内容稍后解释。  
- gplCompatible: true 指出程序遵循 GPL 兼容许可证。  
- loaded_at 是一个时间戳，记录了程序被加载的具体时间点。  
- uid显示用户ID为0，即root用户加载了此程序。  
- 此程序编译后的eBPF字节码长度为96字节，细节将在后文展开。  
- 通过JIT编译，程序的机器代码长度为148字节，这部分内容也将后续介绍。  
- bytes_memlock字段显示程序锁定了4096字节的内存，这部分内存不会发生内存分页操作。  
- 该程序关联了ID为165和166的BPF map。这可能会让人感到意外，因为源代码中并未直接提及BPF map。在本章的后续部分，你将了解到eBPF程序是如何通过map提供全局数据操作。  
- 第5章将详细介绍BTF，这里你只需知道btf_id字段表示该程序包含一个BTF信息块。这种信息只有在使用-g标志进行编译的情况下，才会被包含在对象文件中。

# 3.6.1 BPF程序标识

tag是根据程序指令计算得出的SHA（Secure Hashing Algorithm）总和，可作为BPF程序的另一个标识。尽管每次加载或卸载程序时，ID可能会改变，但tag保持一致。bpftool工具允许通过ID、名称、标识或固定路径来引用BPF程序，因此在本例中，以下任何一种操作都能得到相同的查询结果：

- bpftool prog show id 540   
- bpftool prog show name hello   
- bpftool prog show tag d35b94b4c0c10efb   
- bpftool prog show pinned /sys/fs/bpf/hello

系统内可能会存在多个相同名称的程序，或多个程序实例拥有相同的 tag，但每个程序的 ID 和其固定路径是唯一的。

# 3.6.2编译后的字节码

bytes_xlated字段显示了编译后eBPF代码的字节码总数。这部分字节码已经通过了验证器，并且可能已经被内核进行了调整（本书后续章节将会提到原因）。

现在，我们借助 bpftool 工具来查看 “Hello World” 代码编译后的版本：

```txt
$ bpftool prog dump xlated name hello
int hello(struct xdp_md * ctx):
; bpftprintk("Hello World %d", counter);
0: (18) r6 = map[id:165][0] + 0
2: (61) r3 = *(u32 *)(r6 + 0)
3: (18) r1 = map[id:166][0] + 0
5: (b7) r2 = 15
6: (85) call bpft_trace_PRINT#-78032
; counter++;
7: (61) r1 = *(u32 *)(r6 + 0)
8: (07) r1 += 1
9: (63) *(u32 *)(r6 + 0) = r1
; return XDP_PASS;
10: (b7) r0 = 2
11: (95) exit 
```

这与我们之前通过llvm-objdump工具看到的反汇编代码非常类似。无论是偏移地址还是指令本身，都显得十分相似，比如，我们可以注意到在偏移量为5的地方，指令为 $r2 = 15$

# 3.6.3 JIT编译的机器码

虽然编译后的eBPF字节码相当低级，但它还不完全是机器码。eBPF使用即时编译（JIT）编译器将eBPF字节码转换为在目标CPU上本地运行的机器码。bytes_jited字段显示机器码的长度为108字节。

为了提升性能，通常eBPF程序是在加载时利用即时编译（JIT）技术进行编译的。与此相较，另一种方法是在程序执行时将eBPF字节码编译成机器码。eBPF的指令集和寄存器设计为直接映射成更接近本地机器的指令，编译过程相对简单且执行速度较快，而采用JIT编译完成的程序运行速度更快，因此目前越来越多的架构已经开始支持JIT编译。注5

bpftool 工具具备生成即时编译（JIT）后的汇编语言代码的能力。如果你对汇编语言不够熟悉，感到难以理解，也无需过于担心！我之所以使用它，仅仅是为了演示 eBPF 代码从源代码到可执行机器指令的整个转换过程。以下是相关命令及其输出：

```asm
$ bpftool prog dump jited name hello
int hello(struct xdp_md * ctx):
bpf_prog_d35b94b4c0c10efb_hello:
; bpf_printk("Hello World %d", counter);
0:     hint    #34
4:     stp   x29, x30, [sp, #-16]!
8:     mov   x29, sp
c:     stp   x19, x20, [sp, #-16]!
10:     stp   x21, x22, [sp, #-16]!
14:     stp   x25, x26, [sp, #-16]!
18:     mov   x25, sp
1c:     mov   x26, #0
20:     hint    #36
24:     sub   sp, sp, #0
28:     mov   x19, #-140733193388033
2c:     movk   x19, #2190, lsl #16
30:     movk   x19, #49152
34:     mov   x10, #0
38:    ldr   w2, [x19, x10]
3c:     mov   x0, #-205419695833089
40:     movk   x0, #709, lsl #16
44:     movk   x0, #5904
48:     mov   x1, #15
4c:     mov   x10, #-6992
50:     movk   x10, #29844, lsl #16 
```

```asm
54: movk x10, #56832, lsl #32  
58: blr x10  
5c: add x7, x0, #0  
; counter++;  
60: mov x10, #0  
64:ldr w0, [x19, x10]  
68: add x0, x0, #1  
6c: mov x10, #0  
70: str w0, [x19, x10]  
; return XDP_PASS;  
74: mov x7, #2  
78: mov sp, sp  
7c: ldp x25, x26, [sp], #16  
80: ldp x21, x22, [sp], #16  
84: ldp x19, x20, [sp], #16  
88: ldp x29, x30, [sp], #16  
8c: add x0, x7, #0  
90: ret 
```

![](images/9099abfe3e275e6ea7dbe1013dc254a1708c8098ffc70e4315c9386b132246f6.jpg)

某些 bpftool 包发行版本尚不支持 JIT 输出。如果遇到这种情况，你将看到“Error: No libbpf support.”的提示。你可以根据 https://github.com/libbpf/bpftool 上提供的指南，自行编译 bpftool。

至此，你已经看到“Hello World”程序被成功加载至内核，但目前它尚未与任何事件绑定，因此无法被触发执行。接下来，我们需要将此程序与特定事件相关联。

# 3.7 将程序“附加”到事件上

BPF程序的程序类型需要与其要附加的事件类型相一致，这一点将在第7章中进行详细说明。在这个例子中，我们有一个XDP类型的程序。你可以利用bpftool将该eBPF示例程序附加到网络接口的XDP事件上，操作如下：

```txt
$ bpftool net attach xdp id 540 dev eth0 
```

![](images/3e650b1243e6cb27a8eafa9d3b9802ebf3775f98f334b6b5354b0f6b3024b329.jpg)

截至撰写本书时，尽管 bpftool 工具尚未能支持附加所有种类的程序，但值得注意的是，最近 bpftool 工具的一系列更新扩展，已使得 bpftool 具备了自动附加 k(ret)probes、u(ret)probes 和 tracepoints 等类型程序的能力（https://oreil.ly/Tt99p）。

在这个例子里，我们使用程序ID540来指定程序，你也可以通过程序名称（前提是唯一的）或者tag来识别被附加的程序。此处，我已将该程序附加至网络接口etho。

可以通过bpftool命令查询所有附加到网络接口上的eBPF程序：

```yaml
$ bpftool net list
xdp:
etho(2) driver id 540
tc:
flow_dissector: 
```

程序ID为540的程序已经被附加到了网络接口etho上的XDP事件。此外，输出信息还揭示了网络协议栈中其他一些eBPF程序可附加的潜在事件，包括：tc（流量控制）和flow_dissector。有关更多信息，请参考第7章。

你也可以通过使用ip link命令来检查网络接口，输出如下所示（为了清晰起见，一些细节已被省略）：

```txt
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1000  
...  
2: etho: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 xdp qdisc fq_codel state UP mode DEFAULT group default qlen 1000  
...  
prog/xdp id 540 tag 9d0e949f89f1a82c jited  
... 
```

在本例中，存在两个网络接口：环回接口lo，用于向本机进程发送流量；eth0接口，用于连接本机与外部网络。输出信息还表明，一个经过JIT编译

的 eBPF 程序已经附加到 etho 的 XDP “钩子” 上，该程序的 ID 为 540，tag 为 9d0e949f89f1a82c。

![](images/882d5c6a65fd3404faf8accff4a7567ef9e307b58f39c67cc08eca15c5240f4e.jpg)

你还可以使用ip link将XDP程序附加或卸载到网络接口。在本章末尾有此练习。第7章中还有更多示例。

目前，每当网络数据包被接收时，“hello”eBPF程序就会生成跟踪信息。你可以通过执行cat /sys/kernel debug/tracing/trace_pipe命令来查看这些信息。这将呈现大量类似于下面的输出：

<idle>-0 [003] d.s.. 655370.944105: bpf_trace_printk: Hello World 4531

<idle>-0 [003] d.s.. 655370.944587: bpf_trace_printk: Hello World 4532

<idle> -0 [003] d.s.. 655370.944896: bpf_trace_printk: Hello World 4533

如果你忘记了 trace_pipe 的位置，可以通过使用 bpftool prog tracerlog 命令来获取相同的输出。

与第2章的示例输出相比，这里每条跟踪信息的开头都标有`<idle> -0`，没有任何与命令或进程ID关联。在第2章的示例中，每个系统调用事件的发生都是由执行命令的用户空间进程通过系统调用API触发的，命令和进程ID构成了执行eBPF程序的上下文部分。在当前示例中，XDP事件是由网络数据包到达触发的，没有任何用户空间进程与此数据包相关。当hello eBPF程序被触发时，除了在内存中接收到该数据包外，系统并未对其进行任何处理，也不知道该数据包的内容或目的地。

你会注意到，每次跟踪的计数器值都会递增一，这与预期一致。在源代码中，计数器 counter 是一个全局变量。下面我们将探讨如何通过在 eBPF 中使用 map 来实现这一功能。

# 3.8 全局变量

如前所述，eBPF map 是一种能够被内核态 eBPF 程序访问，也能被用户空间访问的数据结构。同时，同一程序的多次执行能够反复访问同一个 map，使得 map 能够用于在程序多次执行过程中保留状态。此外，多个不同的程序也能够对同一个 map 进行共享访问。正是因为这些特性，map 可以被广泛应用于实现全局变量的功能。

![](images/565d1d3ef5cf2b5ee7583454742fad97e81a51e5053a06df299d0316c87e4bd1.jpg)

在2019年引入全局变量支持之前（https://oreil.ly/IDftt），eBPF开发者需要在编写程序中显式地使用map来完成同样的工作。

如前所述，我们通过 bpftool 观察到示例程序关联了两个 map，其 ID 分别为 165 和 166（如果你亲自操作，可能会看到不同的 ID，因为这些 ID 是在内核创建 map 时动态分配的）。接下来，让我们探究这些 map 中包含了哪些内容。

bpftool工具能够展示已加载到内核中的map信息。为了说明清晰，这里我仅展示与“HelloWorld”程序示例相关的ID为165和166的map内容：

```txt
$ bpftool map list
165: array name hello.bss flags 0x400
key 4B value 4B max_entries 1 memlock 4096B
btf_id 254
166: array name hello.rodata flags 0x80
key 4B value 15B max_entries 1 memlock 4096B
btf_id 254 frozen 
```

由于C程序编译生成的目标文件中的bss段通常用于存储全局变量。你可以使用bpftool工具查看其内容，示例如下：

```twig
$ bpftool map dump name hello.bss
{{ "value": { ".bss": {{ "counter": 11127 }} 
```

```txt
1 1 
```

你还可以通过运行 bpftool map dump id 165 命令来获取相同的信息。如果再次运行这两条命令中的任何一条，你会注意到计数器 counter 会增加，因为每接收到网络数据包时，程序都会执行。

正如你在第5章中所学到的，只有在BTF信息可用的情况下，bpftool才能打印出map中的字段名（这里变量名为counter）。而且只有在使用-g参数编译时才会包含该信息，如果在编译过程中没有使用-g参数编译，就会看到类似下面这样的结果：

```txt
$ bpftool map dump name hello.bss
key: 00 00 00 00 value: 19 01 00 00
Found 1 element 
```

在没有BTF信息的情况下，bpftool无法确定源代码中使用的变量名。根据我们的推断，由于该表中只有一项，因此十六进制值19010000必定代表计数器的当前值（这里字节按照最小有效字节开始的顺序排列，因此十进制为281）。

在这里，你可以看到eBPF程序使用该map来读写全局变量。map还可以用来保存静态数据，你可以通过查看示例中另一个map来了解。

另一个 map 名为 hello.rodata，这可能是与 hello 程序相关的只读数据。你可以打印该 map 的内容，看看它是否保存了 eBPF 程序跟踪信息的字符串。

```txt
$ bpftool map dump name hello.rodata
{{ "value": { ".rodata": [{ "hello._fmt": "Hello World %d" } ]
} 
```

```txt
} 
```

如果没有使用 -g 参数编译对象，则会看到类似下面的输出：

```txt
$ bpftool map dump id 166
key: 00 00 00 00 value: 48 65 6c 6c 6f 20 57 6f 72 6c 64 20 25 64 00
Found 1 element 
```

这个 map 中有一对键值，键对应的值是一个长度为 12 个字节、以零字节结尾。这些字节代表“Hello World %d”的 ASCII 码。对于你来说，这个信息可能并不太意外。

现在，我们已经完成了查看该程序及 map 的内容，接下来是清理工作。首先，是移除那些已经“附加”到特定事件的 eBPF 程序。

# 3.9移除程序

可以通过以下方式将BPF程序从网络接口上移除：

```txt
$ bpftool net detach xdpr dev etho 
```

如果该命令执行成功，将不会有任何输出显示。你可以通过检查 bpftool net list 命令的输出，确认程序是否已被移除。若其中不再显示 XDP 条目，则表示程序已被移除：

```txt
$ bpftool net list
xdp:
tc:
flow_dissector: 
```

然而，需要注意的是，移除后程序依然会保留在内核中：

```shell
$ bpftool prog show name hello
395: xdp name hello tag 9d0e949f89f1a82c gpl
loaded_at 2022-12-19T18:20:32+0000 uid 0 
```

# 3.10 卸载程序

尽管不存在与bpftoolprogload命令直接相反的操作，但你可以通过删除固定到文件系统上的文件，实现将程序从内核卸载：

```txt
$ rm /sys/fs/bpf/hello
$ bpftool prog show name hello 
```

执行该 bpftool 命令后没有任何输出，这表示程序已经不在内核中加载。

# 3.11 BPF程序调用BPF函数

在第2章中，我们探讨了尾调用（即在一个BPF程序内调用另一个BPF程序）的实践案例，并提到了现在eBPF程序也支持函数调用。下面我们将通过一个简单的示例来演示函数调用，这个示例同样附加到tracepoint的sys-enter上，不同之处在于它会打印系统调用的操作码。相关代码位于chapter3/hello-function.bpf.c。

为了阐明这一点，我编写了一个简单的函数，用于从追踪点的参数中提取系统调用的操作码：

```c
static __attribute((noinline)) int get_opcode(struct bpf_raw_tracepoint_args *ctx) {
return ctx->args[1];
} 
```

默认情况下，编译器很可能会将这个极其简单的函数进行内联处理，然而，这违背了我进行此测试的初衷，因为我们打算仅在一个地方调用它，因此我使用了 attribute((noinline)) 属性来强制编译器避免内联。在正常情况下，你可能应该省略这个属性，让编译器自行编译优化。

以下是调用该函数的eBPF程序的示例：

```c
SEC("raw_tp")  
int hello(struct bpf_raw_tracepoint_args *ctx) {  
    int opcode = get_opcode ctx);  
    bpf_printk("Syscall: %d", opcode);  
    return 0;  
} 
```

将该程序编译为eBPF对象文件后，就可以将它加载到内核中，并利用bpftool命令验证是否加载成功：

```txt
$ bpftool prog load hello FUNC.bpf.o /sys/fs/bpf/hello
$ bpftool prog list name hello
893: raw_tracepoint name hello tag 3d9eb0c23d4ab186 gpl
loaded_at 2023-01-05T18:57:31+0000 uid 0
xlated 80B jited 208B memlock 4096B map_ids 204
btf_id 302 
```

接下来，通过检查eBPF字节码，可以查看get_opcode()函数的调用过程：

```c
$ bpftool prog dump xlated name hello
int hello(struct bpf_raw_tracepoint_args * ctx):
; int opcode = getopcode(ctx);
0: (85) call pc+7#bpf_prog_cbacc90865b1b9a5_get_opcode
; bpf_printk("Syscall:%d", opcode);
1: (18) r1 = map[id:193][0]+0
3: (b7) r2 = 12
4: (bf) r3 = r0
5: (85) call bpf_trace_PRINT#-73584
; return 0;
6: (b7) r0 = 0
7: (95) exit
int get_opcode(struct bpf_raw_tracepoint_args * ctx):
; return ctx->args[1];
8: (79) r0 = *(u64*)(r1 +8)
; return ctx->args[1];
9: (95) exit 
```

$\bullet$ 在此处，我们可以看到hello()eBPF程序正调用get_opcode（）函数。偏移量0的eBPF指令是 $0\times 85$ ，根据指令集文档，此代表“函数调用”。执行此指令时，不会执行位于偏移量1的下一条指令，而是直接跳转到指令7，即 $\mathrm{pc + 7}$ ，也就是跳至偏移量8的指令处。

接下来是get_opcode()字节码，正如所期待的那样，第一个指令正好位于偏移量8。

函数调用指令要求将当前状态保存到eBPF虚拟机的堆栈上，以便在被调用的函数返回后，调用者函数能够恢复执行。鉴于堆栈的大小被限制为512字节，因此BPF程序调用BPF函数的不能嵌套得很深。

![](images/0891a0a37512c0c3fb61e74bf5602127aba2e630af1d231c4417c2778cf75367.jpg)

有关尾调用和BPF程序函数调用的更多详细信息，可以参考Cloudflare博客上Jakub Sitnicki发表的精彩文章：“Assembly within! BPF tail calls on x86 and ARM”（深入了解汇编：x86和ARM上的BPF尾调用，https://oreil.ly/6kOp3）。

# 3.12总结

在本章里，我们探讨了如何将C语言源代码编译成eBPF字节码，并进一步编译成机器码，以便在内核中运行。你还学到了如何使用bpftool工具来查看已加载到内核的eBPF程序和eBPF map，及如何将eBPF程序附加到XDP事件上。

此外，本章还演示了由不同事件触发的各类eBPF程序的示例。例如，当数据包到达网络接口时，会触发XDP事件的eBPF程序，而kprobe和tracepoint事件的eBPF程序，则是在内核代码的特定位置被触发。第7章将进一步探讨其他类型的eBPF程序。

另外，你还了解了如何利用 map 作为 eBPF 程序的全局变量，以及 BPF 程序调用 BPF 函数的调用过程。

在下一章中，我们将深入探讨BPF系统调用，详细说明使用bpftool或其他用户空间加载器来加载BPF程序，并将其“附加”到特定事件的具体机制。

# 3.13 练习

如果你对深入了解BPF程序感兴趣，这里有一些你可以尝试的方法：

1. 尝试运行如下的ip link命令，在网络接口上“附加”与“移除”XDP程序：

```txt
$ ip link set dev etho xdp obj hello.bpf.o sec xdp $ ip link set dev etho xdp off 
```

2. 尝试运行第 2 章中的 BCC 示例程序。当示例程序运行时，打开另一个终端窗口，利用 bpftool 来查看已加载的 BPF 程序。以下是运行 hello-map.py 示例时你可能会看到的：

```csv
$ bpftool prog show name hello
197: kprobe name hello tag ba73a317e9480a37 gpl
loaded_at 2022-08-22T08:46:22+0000 uid 0
xlated 296B jited 328B memlock 4096B map_ids 65
btf_id 179
pids hello-map.py(2785) 
```

你还可以使用 bpftool prog dump 命令查看这些程序的字节码和机器码版本。

3. 运行位于chapter2目录下的hello-tail.py，并在运行过程中观察加载的程序。你将会看到，每个尾调用程序都被单独列出，类似于下面的展示：

```csv
$ bpftool prog list
...
120: raw_tracepoint name hello tag b6bfd0e76e7f9aac gpl
loaded_at 2023-01-05T14:35:32+0000 uid 0
xlated 160B jited 272B memlock 4096B map_ids 29
btf_id 124
pids hello-tail.py(3590)
...
121: raw_tracepoint name ignore_opcode tag a04f5eef06a7f555 gpl
loaded_at 2023-01-05T14:35:32+0000 uid 0
xlated 16B jited 72B memlock 4096B
btf_id 124
pids hello-tail.py(3590)
...
122: raw_tracepoint name hello_exec tag 931f578bd09da154 gpl
loaded_at 2023-01-05T14:35:32+0000 uid 0
xlated 112B jited 168B memlock 4096B
btf_id 124
pids hello-tail.py(3590) 
```

```txt
123: raw_tracepoint name hello_timer tag 6c3378ebb7d3a617 gpl  
loaded_at 2023-01-05T14:35:32+0000 uid 0  
xlated 336B jited 356B memlock 4096B  
btf_id 124  
pids hello-tail.py(3590) 
```

你还可以利用 bpftool prog dump xlated 命令查看字节码指令，并将其与“BPF 程序调用 BPF 函数”一节中的内容做对比。

4. 调研某个问题时需谨慎，因为最佳的做法可能仅仅是理解为什么会这样发生，而不是盲目尝试。当XDP程序返回0值时，这等同于XDP_ABORTED，意味着告诉内核停止对该数据包的任何后续处理。虽然在C语言中0通常表示成功，但这种情况似乎有些违反直觉，而实际就是这样的。因此，如果你试图修改程序使其返回0，并将其附加到虚拟机的eth0接口上，那么所有网络数据包都将被丢弃。如果你是通过SSH连接到虚拟机的，这将导致非常不幸的后果，你可能需要重启虚拟机来恢复访问。一个可行的解决方案是在容器中运行程序，这样XDP程序只会附加到容器的虚拟以太网接口上，不会影响整个虚拟机。你可以在这个GitHub仓库https://github.com/lizrice/lbfrom-scratch找到相关的示例。

# bpf() 系统调用

正如在第1章中所介绍的，用户空间程序通过系统调用请求内核代执行一些操作。因此，如果用户空间程序需要将eBPF程序加载到内核，就不可避免地需要使用系统调用，该系统调用为bpf()。在本章中，我将展示如何利用这一系统调用来加载eBPF程序和eBPF map，并介绍如何通过该系统调用与它们进行交互。

需要特别指出的是，系统调用接口仅被用户空间程序所使用。对于内核态的eBPF代码，它们并不通过系统调用来访问eBPF map，而是依赖于helper函数来进行读写操作。在前两章中，我们已经看到了相关的示例。

如果你打算编写自己的eBPF程序，可能不会直接使用bpf()系统调用。在本书的后续章节中，我们将介绍一些库，它们提供了更高级的抽象层，使操作更为简便。尽管如此，这些高级抽象仍是基于本章所介绍的那些底层系统调用，因此，无论你选择使用哪种库，理解本章介绍的基础知识，包括：加载程序、创建和访问eBPF map等，都是非常重要的。

在介绍bpf()系统调用的具体示例之前，先让我们看一下bpf()手册（https://oreil.ly/NJdIM）中的定义，手册中提到：“bpf()主要用于执行针对eBPF map或eBPF程序的命令”。接下来，我们将看看bpf()函数签名：

int bpf(int cmd,union bpf_attr \*attr, unsigned int size);

在bpf()函数中，第一个参数cmd的作用是指定将要执行的命令，bpf()系统调用支持多种不同的命令，这些命令主要用于操作eBPF程序和eBPF map。例如，图4-1中展示了几种常见的命令，包括加载eBPF程序、创建eBPF map、将程序“附加”到指定事件上，以及访问eBPF map中的键值对。

![](images/71f4cad3affc0c164e3af0fa2e03f0b547a90d1a5dba4d4bb1d9d77219f3909d.jpg)  
图4-1：用户空间程序通过bpf()系统调用与内核中的eBPF程序和eBPF map进行交互

在bpf()系统调用中，第二个参数attr用于提供指定命令所需的数据。而第三个参数size，则用来指明attr中数据的字节数大小。

你可能还记得，在第1章中，我曾使用strace演示了用户空间代码是如何多次发起系统调用的。在本章中，我会使用strace来演示调用bpf()系统调用。虽然strace的输出会显示每个系统调用的参数，但为了保持本章示例的清晰易读，我会省略attr参数中的一些细节，除非它们对理解内容有重要意义时才予以展示。

![](images/166fd4ff2be5f270b94193b438f9d9b3603c0f5a92f776a33a9c50f7168d52ca.jpg)

本章的示例代码和运行环境的设置说明均可在 github.com/lizrice/learning-ebpf 仓库中的 chapter4 下可以找到。

在这个例子中，我要介绍的是一个基于第2章hello-buffer.py示例进一步开发的BCC程序，名为hello-buffer-config.py。该程序在运行时同样会向perf缓冲区发送消息，用以将execve()系统调用事件的信息从内核传递到用户空间。其区别在于，这个版本能够根据用户ID的不同，显示不同的配置信息。

下面是内核侧的eBPF代码：  
```c
struct user msg_t { ① char message[12]; };   
BPF_HASH(config, u32, struct user msg_t); ②   
BPF_PERF_OUTPUT(output); ③   
struct data_t { ④ int pid; int uid; char command[16]; char message[12]; };   
int hello(void *ctx) { ⑤ struct data_t data = {}; struct user msg_t *p; char message[12] = "Hello World"; data.pid = bpf_get_current_PID_tgid() >> 32; data.uid = bpf_get_current_UIDgid() & OxFFFFFF; bpf_get_current_comm(&data.command, sizeof(data.command)); p = config.lookup(&data.uid); if (p != 0) { bpfprobe_read_kernel(&data.message, sizeof(data.message), p->message); } else { bpfprobe_read_kernel(&data.message, sizeof(data.message), message); } output.perf_submit(ctx, &data, sizeof(data)); return 0; } 
```

1 这行定义了一个名为 user msg_t 的结构体，它用于存储 12 个字符。  
BCC的BPF_HASH宏用于定义一个名为config的hash类型的map，以u32类型的键为索引，保存用户ID；值用于保存user msg_t类型的值（如果没有指定键和值的类型，BCC默认将两者都设置为u64）。  
③ perf缓冲区的定义方式与第2章中的方法完全一致。你可以向该缓冲区提交任何类型的数据，因此这里不需要指定具体的数据类型。  
④ 在这个例子中，程序实际上会将一个 data_t 结构的数据提交到 perf 缓冲区中。这也与第 2 章的示例相同。  
⑤ eBPF程序的其他大部分内容与你之前看到的hello()版本基本相同。  
⑥ 唯一不同之处在于，它利用helper函数bpf_get_currentuid.git()来获取用户ID。然后，在config hashtable中搜索以该用户ID为键的条目。若找到相应匹配的条目，则会输出该条目的值信息；若未找到匹配项，则会默认打印出“Hello World”。

用户空间的Python代码多了两行：

```python
b["config"]["ct.c_int(0)] = ct.create_string_buffer(b"Hey root!") b["config"]["ct.c_int(501)] = ct.create_string_buffer(b"Hi user 501!") 
```

在 config hashtable 中，我们为用户 ID 为 0（即本机的根用户）和用户 ID 为 501（代表一个普通用户）分别定义了不同的信息。为了确保键和值的类型与 C 语言中的 user msg_t 定义一致，这段代码使用了 Python 的 ctypes 包。

下面是该示例的一些示范性输出，以及我在另一个终端中执行的命令：

```shell
Terminal 1 Terminal 2
$ ./hello-buffer-config.py
37926 501 bash Hi user 501! ls
37927 501 bash Hi user 501! sudo ls
37929 0 sudo Hey root!
37931 501 bash Hi user 501! sudo -u daemon ls
37933 1 sudo Hello World 
```

既然你已经对这个程序的功能有了一定了解，下一步我将展示其在运行时所调用的bpf()系统函数。为了达到这个目的，我将使用strace重新运行这个程序，并加上-e bpf选项，以便仅显示与bpf()系统调用相关的信息：

```powershell
$ strace -e bpf ./hello-buffer-config.py 
```

你会注意到输出结果中展示了对bpf()系统调用的多次执行。每次执行中，都可以看到bpf()系统调用执行的具体命令。大致的内容是：

```txt
bpf(BPF_BTF_LOAD, ...) = 3  
bpf(BPF_MAP_CREATE, {map_type=BPF_MAP_TYPE_PERF_EVENT_ARRAY...}) = 4  
bpf(BPFMAP CREATE, {map_type=BPF(Map_TYPE_HASH...)} = 5  
bpf(BPF_PROG_LOAD, {prog_type=BPF_PROG_TYPE_KPROBE, ... prog_name="hello", ...}) = 6  
bpf(BPF MAP_UPDATE Elem, ...)
```

我们来逐个分析这些调用。考虑到读者和我的时间都很宝贵，我不打算详细讨论每个调用的每个参数。我将重点介绍那些我认为对理解用户空间程序与eBPF程序交互过程真正有帮助的部分。

# 4.1 加载BTF数据

上面示例中我们看到的对bpf()的第一次调用是这样的：

```txt
bpf(BPF_BTF_LOAD, {btf="\\237\\353\\1\\0...}, 128) = 3
```

从这个输出中，我们可以看到使用了BPF_BTF_LOAD命令。该命令是BPF命令集的一个有效命令（至少在本书撰写时是这样），内核源代码中有详细记录注1。

如果你使用的是相对较老的Linux内核版本，可能就不会见到这个命令的调用，因为它关联到BTF，即BPF类型格式注2。BTF使得eBPF程序能够在不同的

内核版本间实现可移植性，意味着你可以在一台机器上编译程序，然后在另一台机器上使用，即便后者运行的是不同版本的内核，具有不同的内核数据结构。关于这一点，我会在第5章进行更深入的讨论。

这个bpf()系统调用是向内核请求加载一个BTF数据块，该bpf()系统调用的返回代码是一个指向该数据的文件描述符，此示例中文件描述符是3。

![](images/72489143cde8dff751cfea60c5f8faefbd4b2986483e73f6f7d45322055a8c46.jpg)

文件描述符是用于标识已打开文件（或类似文件的对象）的标识符。当你通过open()或openat()系统调用打开文件，返回的结果是一个文件描述符，然后将这个描述符作为参数传递给其他系统调用，例如：read()或write()，以对该文件进行操作。在这种情况下，虽然数据块本身并不完全等同于文件，但它被赋予了一个文件描述符作为标识，便于未来对其进行引用操作。

# 4.2 创建 map

接下来的bpf()系统调用会创建一个名为output的perf缓冲区map：

```txt
bpf(BPF_MAP_CREATE, {map_type=BPF_MAP_TYPE_PERF_EVENT_ARRAY, , key_size=4, value_size=4, max_entries=4, ... map_name="output", ...}, 128) = 4 
```

从名称BPF_MAP_CREATE中，你可能已经猜到这个调用会创建一个eBPF map。你可以看到，这个eBPF map的类型被指定为PERF_EVENT_ARRAY，且被命名为output。这个perf事件map的键和值的长度都是4字节。该map所能容纳的键值对的数量被限制为4个，这是由max_entries字段定义的；我会在本章后面进一步阐述为什么这个map中有4个条目。返回的值4是一个文件描述符，用于用户空间代码访问该map。

示例中输出的下一个bpf()系统调用是创建一个名为config的map：

```python
bpf(BPF_MAP_CREATE, {map_type=BPF_MAP_TYPE_HASH, key_size=4, value_size=12, max_entries=10240... map_name="config", ...btf_fd=3,...}, 128) = 5 
```

这个 map 被设置为 hash 类型的 map，其键的长度为 4 字节，足以存储 32 位

整数的用户ID。值的长度设定为12字节，与user msg_t结构体的长度相等。因未特别指定hashtable尺寸，所以它采用了BCC的默认大小，即10240个条目。

这个bpf()系统调用还将返回一个文件描述符5，用于在以后的系统调用中引用config map。

这里，你还会注意到字段btf_fd=3，这一字段指示内核使用先前BTF数据块的文件描述符3。正如你将在第5章中所见，BTF数据块中详细描述了eBPF程序中使用的数据结构。创建map的bpf()系统调用中包含这些BTF信息，就意味着BTF提供了关于map中键和值类型的详细信息。利用这些信息，bpftool工具能够以易于用户阅读的方式打印出map的内容，正如你在第3章中看到的那样。

# 4.3 加载 eBPF 程序

到目前为止，你已经看到前文示例程序使用bpf()系统调用将BTF数据加载到内核中，并创建了一些eBPF map。接下来，程序将通过下面的bpf()系统调用把eBPF程序加载到内核中：

```python
bpf(BPF_PROG_LOAD, {prog_type=BPF_PROG_TYPE_KPROBE, insn_cnt=44, insns=0xffffa836abe8, license="GPL", ... prog_name="hello", ... expected Attached_type=BPF_CGROUP_INET_INGRESS, prog_btf_fd=3,...}, 128) = 6 
```

下面是一些字段的介绍：

- prog_type字段指明了程序类型，在这里是BPF_PROG_TYPE_KPROBE，表明该程序将被“附加”到kprobe上。有关程序类型的更多详细信息，请参考第7章。  
- insn_cnt字段代表“指令计数”，它指的是程序中字节码指令的总数。  
- eBPF 程序的字节码指令存储在 insns 字段所指向地址的内存中。

- 此程序被标明为 GPL 许可，这意味着它能够使用 GPL 许可的 BPF helper 函数。  
- 程序的名称为“hello”。  
- expected Attached_type字段被设置为BPF_CGROUP_INET_INGRESS可能会引起些许困惑，因为这个名称听起来似乎与处理入口网络流量相关，然而，实际上这个eBPF程序是设计用来“附加”到kprobe上。这是因为，expected Attached_type字段并不适用于所有类型的程序，BPF_PROG_TYPE_KPROBE并不在其适用范围之内，BPF_CGROUP_INET_INGRESS仅是BPF附加点类型列表中的第一个选项<sup>3</sup>，其值为默认值0。  
- prog_btf_fd字段告诉内核该程序要使用之前加载的BTF数据块。这里的值3与BPF_BTF_LOAD命令的系统调用返回的文件描述符相对应，也与创建config map所使用的BTF数据块相同。

如果程序验证不通过（这一点我们会在第6章详细讨论），该系统调用会返回一个负数。在这个例子中，程序加载成功，因此系统调用返回了文件描述符6。在这里，我们简要总结一下，已提到的bpf()系统调用返回的文件描述符，如表4-1所示。

表 4-1: 程序 hello-buffer-config.py 中 bpf(   ) 系统调用的文件描述符:  

<table><tr><td>文件描述符</td><td>表示</td></tr><tr><td>3</td><td>加载BTF数据块</td></tr><tr><td>4</td><td>创建名为output的perf缓冲区类型的map</td></tr><tr><td>5</td><td>创建名为config的hashtable类型的map</td></tr><tr><td>6</td><td>加载eBPF程序</td></tr></table>

# 4.4 从用户空间修改 eBPF map

在Python用户空间源代码中，我们已经看到了用户ID为0的root用户和用户ID为501的用户配置的信息行：

```python
b["config"]["ct.c_int(0)] = ct.create_string_buffer(b"Hey root!") b["config"]["ct.c_int(501)] = ct.create_string_buffer(b"Hi user 501!") 
```

你可以通过类似如下的系统调用看到 map 中定义的条目：

```txt
bpf(BPF_MAP_UPDATE_ELEM, {map_fd=5, key=0xFFFFfa7842490, value=0xFFFFfa7a2b410, flags=BPF_ANY}, 128) = 0 
```

BPF_MAP_UPDATE_ELEM命令用于更新映射中的元素。使用BPF_ANY标志，意味着如果指定的键在map中不存在，系统将创建这个键。在前述程序中，这种操作被执行了两次，分别用于为两个不同的用户ID设置相应的信息。

map_fd字段指明了操作哪个map。在本例中，其值为5，即之前创建configmap时返回的文件描述符。

文件描述符是内核为特定进程分配的，因此，文件描述符为5，仅对Python程序的特定用户空间进程有效。然而，多个用户空间程序（以及内核中的多个eBPF程序）都可以共享对同一个map。在内核中，访问相同map结构的两个用户空间程序可能会被分配不同的文件描述符值。同样，两个用户空间程序也可能在不同的map中使用相同的文件描述符值。

由于键和值均为指针形式，因此在字符串输出中无法直接看到它们的具体数值。但是，你可以利用bpftool来查阅map的详细内容，操作如下所示：

```txt
$ bpftool map dump name config
{{ "key": 0, "value": { "message": "Hey root!" }
}
}, { "key": 501, "value": { "message": "Hi user 501!" }
} 
```

bpftool 如何知道该如何格式化输出？例如，它是如何识别某个值为结构体，并且该结构体中有一个名为 message 的字符串？答案在于，它利用了在创建 map 的 BPF_MAP_CREATE 系统调用中定义的 BTF 信息。下一章将深入讲解 BTF 是如何传递这些信息的。

现在你已经掌握了用户空间与内核如何互动，以加载程序、创建和更新 map 中的数据。到目前为止，eBPF 程序还没“附加”到特定事件上，这个步骤是必须的，否则当事件发生时程序不会被触发。

警告：不同类型的 eBPF 程序会以不同的方式“附加”到不同的事件中！本章后续部分将演示如何不依赖 bpf() 系统调用来将 eBPF 程序附加至 kprobe 事件。同时，在章节末尾，我们还会展示一个例子，说明如何使用 bpf() 系统调用将程序附加到 tracepoint 事件上。

在深入讨论这些细节之前，我想先讨论一下当你退出运行的程序时会发生什么。你会注意到，eBPF程序和eBPF map会被自动卸载，这是由于内核采用引用计数机制来跟踪它们。

# 4.5 BPF 程序和 BPF map 引用

我们知道，当用户空间进程通过bpf()系统调用将BPF程序加载到内核时，会返回一个文件描述符，这在内核中代表了对该BPF程序的引用。这个文件描述符由发起调用的用户空间进程持有；当用户空间进程终止时，文件描述符会被释放，BPF程序的引用数减少。若BPF程序不再被任何用户空间进程引用时，内核便会将BPF程序卸载。

当你把BPF程序“固定”到文件系统上（即在文件系统中为其创建一个特定文件或链接），就会产生一个额外的引用。

# 4.5.1 Pinning（固定）

在第3章中，你已经看到了通过以下命令将BPF程序“固定”到文件系统上的操作：

bpftool prog load hello.bpf.o /sys/fsbpbf/hello

![](images/f53ba2a8d39c73e70d52bfc1a78bfe5c1edf460b3b16a2b545f3185f7ad5cafd.jpg)

这些固定的对象并非存储在磁盘上的实际文件。它们是在伪文件系统（pseudo filesystem）中创建的，这种文件系统在行为上类似于传统的磁盘文件系统，具有目录和文件结构，但其实质是存储在内存中。这意味着，一旦系统重启，这些对象就会消失。

如果仅通过 bpftool 命令加载 BPF 程序却未将其固定到文件系统上，则是毫无意义的。因为一旦 bpftool 命令结束，相应的文件描述符将被释放。若此时 BPF 程序的引用计数降至零，它便会被内核卸载。相反，若把 BPF 程序固定到文件系统，这就意味着即便 bpftool 命令执行完毕，程序由于拥有一个额外的引用，仍然能保持在内核中。

当 BPF 程序被附加到内核上时，其引用计数会相应增加。不同类型的 BPF 程序具有不同的引用计数的行为，具体行为取决于程序的类型。在第 7 章中，你会详细了解到程序类型的相关信息。如果 eBPF 程序与追踪相关，如 kprobes 和 tracepoints，通常与用户空间进程关联；对于这类 eBPF 程序，当相关进程退出时，内核中的引用计数也会相应减少。而一些 eBPF 程序是附加到网络堆栈或 cgroups 上，它们与用户空间进程无直接关联，因此即便是加载这些程序的用户空间进程退出了，这些程序也会保留在系统中。以下是一个通过 ip link 命令加载 XDP 程序的例子：

ip link set dev etho xdp obj hello.bpf.o sec xdp

ip 命令已执行完毕，虽然没有指定固定到文件系统的位置，但使用 bpftool 命令会显示内核中已加载了该 XDP 程序：

```txt
$ bpftool prog list
...
1255: xdp name hello tag 9d0e949f89f1a82c gpl
loaded_at 2022-11-01T19:21:14+0000 uid 0
xlated 48B jited 108B memlock 4096B map_ids 612 
```

这说明该程序的引用计数不为零，因为在ip link命令执行完成后，XDP程序仍然存在。

eBPF map 同样维护着引用计数，当这些引用计数减至零时，相应的 map 会被自动清除。每个 eBPF 程序使用了该 map，该 eBPF map 的引用计数就会增加；用户空间程序也会因持有该 map 的文件描述符，使得 map 的引用计数增加。

eBPF程序的源码可以定义一个map，但并没有实际引用。例如，如果你需要存储有关BPF程序的元数据，可以将其定义为全局变量，正如前一章所介绍的，这些元数据会被储存在BPF map中。如果eBPF程序没有对这个map进行操作，那么程序与map之间就不会自动产生引用计数。然而，用户空间BPF加载程序能够通过bpf()系统调用中的BPF_PROG_BIND_MAP命令，手动建立map与BPF程序之间的绑定。这意味着，即使用户空间加载程序退出，并且不再持有对该map的文件描述符的引用，该map仍旧不会被删除。

BPF map 也可以被固定在文件系统上，用户空间的程序能够通过文件系统中的路径访问这些 BPF map。

![](images/8637cf338a2834c634045e9852d5faba2e97d2dee05ade9dadfadd8a148df7e5.jpg)

Alexei Starovoitov 在他的博文：“Lifetime of BPF Objects”（https://oreil.ly/vofxH）中对BPF引用数和文件描述符做了很好的描述。

另一种创建BPF程序引用的方法是BPF链接。

# 4.5.2 BPF链接

BPF链接为eBPF程序及其附加的事件之间提供了一个抽象层。这个BPF链接本身可以被固定到文件系统上，从而为BPF程序增加了一个引用数。这意

味着即便用户空间加载程序退出，也能保留BPF程序加载的状态。这是因为当用户空间加载程序退出，文件描述符会被释放，BPF程序的引用计数减少，但因为有BPF链接的存在，其引用计数不会降至零。

通过本章末尾的练习，你将有机会亲身体验BPF链接的实践应用。现在，让我们切回到之前示例hello-buffer-config.py中，继续查看它使用的bpf()系统调用。

# 4.6 eBPF的其他系统调用

到目前为止，你已经看到通过bpf()系统调用将BTF数据、eBPF程序、eBPF map及map数据加载到内核中。接下来，strace输出将展示涉及perf缓冲区操作的系统调用。

![](images/182b710544a19f4b5e52c27af383f4fc27620937ed2653d208ac45dd78fde58e.jpg)

在本节剩余部分，我们将详细探讨使用系统调用执行下面一系列操作，包括：perf缓冲区操作、环形缓冲区操作、kprobes事件操作、以map遍历操作。需要指出的是，并非所有eBPF程序都需执行上述步骤。因此，如果你时间紧迫或认为内容过于烦琐，可直接跳至本章小结。

# 4.6.1 Perf缓冲区初始化

从前文你已经看到了使用bpf(BPF_MAP_UPDATE_ELEM)调用向config map添加条目的操作。接下来，strace输出中会显示如下这样的系统调用：

```txt
bpf(BPF_MAP_UPDATE_ELEM, {map_fd=4, key=0xFFFFfa7842490, value=0xFFFFfa7a2b410, flags=BPF_ANY}, 128) = 0 
```

上面的bpf()系统调用与操作config map的调用十分相似，不同之处在于此处的map_fd=4，意味着输出目标为perf缓冲区map。

与之前的bpf()操作类似，键和值均以指针形式存在，因此通过strace输出无法直接识别出键和值的具体数值。在前文示例中，该系统调用被重复执行了四次，每次调用的参数值都保持不变，但我们无法确定每次调用时指针所

指的值是否会改动。通过分析这些bpf(BPF_MAP_UPDATE_ELEM)调用，我们对Perf缓冲区的设置和使用产生了一些疑问：

- 为什么有四次调用 bpf(BPF_MAP_UPDATE_ELEM)？这是否与 output map 创建时有四个条目有关？  
- 在这四次 bpf(BPF_MAP_UPDATE Elem) 调用之后，strace 输出中没有再出现 bpf() 系统调用。这似乎有点奇怪，因为 eBPF 程序在每次被触发时都会向该 map 写入数据。同时，用户空间代码会显示 map 数据，这些数据显然不是通过 bpf() 系统调用从 map 中获取的，那么这些数据是如何获得的呢？

目前尚无明确证据显示eBPF程序是如何“附加”至触发其执行的kprobe事件上的。为了解答上述疑问，我们需要借助strace命令在执行该示例时捕获更多系统调用信息，具体命令如下：

```powershell
$ strace -e bpf,perf_event_open,ioct1,ppoll ./hello-buffer-config.py 
```

为了简洁，将忽略与本示例中的eBPF功能不直接相关的ioct1()调用。

# 4.6.2 附加到perf kprobe事件上

如前所述，eBPF程序“hello”被加载到内核，其文件描述符为6。为了将eBPF程序“附加”至perf的kprobe事件上，需要为perf kprobe事件创建一个文件描述符。下面strace输出用于创建perf的execve()kprobe事件的文件描述符：

```txt
perf_event_open({type=0x6 /* PERF_TYPE_????*/, ..., ...}) = 7 
```

根据perf_event_open()系统调用的官方文档（https://oreil.ly/xpRJs），该调用会“创建一个访问PMU（硬件性能监控单元）信息的文件描述符”。从strace输出中，我们无法直接理解type参数值为6的原因，如果深入阅读该

文档，可以找到Linux如何支持PMU（硬件性能监控单位）动态类型的详细说明：

在 /sys/bus/event_source/devices 目录下，每个 PMU 实例都对应一个子目录。在这些子目录中，都存在一个名为 type 的文件，文件中的内容是一个整数，表示 type 字段的值。

确实，在该目录下可以找到一个名为kprobe/type的文件：

```shell
$ cat /sys/bus/event_source/devices/kprobe/type 6 
```

从上述内容可以推断，在执行perf_event_open()时，type=6意味着这是创建了一个Kprobe类型的perf事件。

虽然strace输出没有提供足够的细节来直接证明kprobe是如何“附加”到execve()系统调用上，接下来，我们将通过查看该系统调用返回的文件描述符，为你提供充分的证据，证明kprobe确实被“附加”到execve()系统调用上。

perf_event_open() 系统调用的返回码是 7，它代表了 perf 的 kprobe 事件的文件描述符，而 6 则代表 eBPF 程序 “hello” 的文件描述符。perf_event_open() 的手册还解释了如何使用 ioctl() 在两者之间创建“附加”关系：

PERF_EVENTIOC_SET_BPF[...]允许将BPF程序“附加”到现有的kprobe跟踪点事件，其参数是之前的bpf(2）系统调用创建的BPF程序的文件描述符。

上面的话解释了下面的ioct1()系统调用。你会在strace输出中看到ioct1()系统调用的参数会指向这两个文件描述符：

```txt
ioct1(7,PERF_EVENT,IOC_SET_BPF,6) = 0 
```

还有另一个 ioct1() 调用用于开启 kprobe 事件：

ioct1(7,PERF_EVENT IOC_ENABLE，0） $= 0$

这样，只要在这台机器上运行 execve() 系统调用，就会触发运行 eBPF 程序。

# 4.6.3 设置和读取 Perf 事件

之前提到过，这里有四次bpf(BPF_MAP_UPDATE_ELEM)系统调用与output perf缓冲区相关，除此之外，与之相关的系统调用还有如下：

```txt
perf_event_open({type=PERF_TYPE_SOFTWARE, size=0 /* PERF_ATTR_SIZE ??? *, config=PERF_COUNT_SW_BPF_OUTPUT, ...}, -1, X, -1, PERF_FLAG_FD_CLOEXEC) = Y 
```

ioct1(Y,PERF_EVENT_IOC_ENABLE，0） $= 0$

```txt
bpf(BPF_MAP_UPDATE_ELEM, {map_fd=4, key=0xFFFFfa7842490, value=0xFFFFfa7a2b410, flags=BPF_ANY}, 128) = 0 
```

perf_event_open()系统调用中的“X”在调用的四次中，输出显示值分别为0、1、2和3。若查看perf_event_open()系统调用的手册，你会发现这个值代表cpu编号，而位于这个值前面的字段则指的是pid或进程ID。文档内容如下：

pid $= = -1$ and cpu $\geq 0$ This measures all processes/threads on the specified CPU.

bpf(BPF_MAP_UPDATE Elem) 出现四次，这与我的笔记本电脑拥有四个 CPU 核心相吻合。这便解释了为什么 output perf 缓冲区 map 中会有四个条目：即每个 CPU 核对应一个条目。这也阐明了为何 BPF_MAP_TYPE_PERF_EVENT_ARRAY 类型的 map 中会有 “ARRAY” 一词，因为 output map 实际上是一个 perf 缓冲区数组，其中每个核心对应的一个 perf 缓冲区。

如果你使用第10章所介绍的eBPF库来编写eBPF程序，那么你无需关心处理CPU核数等细节问题，因为这些eBPF库已经帮你解决了这些问题。但通过使用strace观察BPF程序运行时的系统调用，你可以了解到这些细节信息。

perf_event_open() 系统调用返回的文件描述符，在此示例中我们将其标记为“Y”，其值分别为 8、9、10 和 11。ioct1() 系统调用会为每个文件描述符开启 output perf 缓冲区。bpf(BPF_MAP_UPDATE_ELEM) 系统调用将 map_fd 设置为指向每个 CPU 核的 perf 环缓冲区，以明确数据提交的具体位置。

然后，用户空间代码可以对这四个输出流的文件描述符执行ppoll()调用，这样，不管哪个CPU核上运行eBPF程序“hello”，都能捕获到execve()kprobe事件。下面是ppoll()的系统调用示例：

```erlang
ppoll([\{fd=8, events=POLLIN\}, {fd=9, events=POLLIN\}, {fd=10, events=POLLIN\}, {fd=11, events=POLLIN}], 4, NULL, NULL, 0) = 1 ([{fd=8, revents=POLLIN}]) 
```

如果你尝试亲自运行示例程序，会发现这些ppoll()调用将一直处于阻塞状态，直到某个文件描述符有数据可读。然后直到execve()被触发，eBPF程序写入数据，用户空间程序会通过这个ppoll()调用检索到数据，你才能在屏幕上看到返回码。

在第2章中，我提及如果你的内核版本为5.8或更高，则推荐使用BPF环形缓冲区，而非perf缓冲区注4。下面，我们将探讨一个使用环形缓冲区的同一示例代码的改进版本。

# 4.7 环形缓冲区

正如内核文档（https://oreil.ly/RN_RA）所述，环形缓冲区比perf缓冲区更受青睐，一方面是出于性能考虑，另一方面是即使数据使用不同CPU核提交，也能保持其顺序，这是因为所有内核共享一个缓冲区。

将hello-buffer-config.py改写为使用环形缓冲区并不复杂。在附带的GitHub代码库中，你可以找到此示例的改进版本，位于chapter4/hello-ring-buffer-config.py。表4-2显示了两者的差异。

表 4-2: BCC 代码示例中使用 perf 缓冲区和环形缓冲区之间的差异  

<table><tr><td>hello-buffer-config.py</td><td>hello-ring-buffer-config.py</td></tr><tr><td>BPF_PERF_OUTPUT(output);</td><td>BPF_RINGBUF_OUTPUT(output, 1);</td></tr><tr><td>output.perf_submit(ctx, &amp;data, sizeof(data));</td><td>output.ringbuf_output(&amp;data, sizeof(data), 0);</td></tr><tr><td>b[&quot;output&quot;].open_perf_bufferprint_event)</td><td>b[&quot;output&quot;].open_ring_bufferprint_event)</td></tr><tr><td>b.perf_buffer_poll()</td><td>b.ring_buffer_poll()</td></tr></table>

正如预期，由于变更仅涉及 output 缓冲区类型，因此加载 BPF 程序、加载 config map 以及将 BPF 程序关联到 kprobe 事件的系统调用都未发生改变。

以下是创建output环形缓冲区的bpf()系统调用：

```python
bpf(BPF_MAP_CREATE, {map_type=BPF_MAP_TYPE_RINGBUF, key_size=0, value_size=0, max_entries=4096, ... map_name="output", ..., 128) = 4 
```

strace输出中的主要区别在于，在设置perf缓冲区时，需查看perf_event_open()、ioct1()和bpf(BPF_MAP_UPDATE_ELEM)四种不同的系统调用。而对于环形缓冲区，由于所有CPU核共享同一个文件描述符，因此只需要关注bpf()系统调用。

撰写本书时，BCC对perf缓冲区的处理采用了之前展示的ppoll机制，而对环形缓冲区数据则使用了较新的epoll机制。这里，让我们探讨一下ppoll与epoll之间的差异。

在perf缓冲区的示例hello-buffer-config.py中，我们展示了如何使用ppoll()系统调用，具体如下所示：

```txt
ppoll([[fd=8, events=POLLIN], {fd=9, events=POLLIN}, {fd=10, events=POLLIN}, {fd=11, events=POLLIN}], 4, NULL, NULL, 0) = 1 ([fd=8, revents=POLLIN]) 
```

请注意，用户空间进程需要读取perf缓冲区数据的文件描述符8、9、10和11。每当轮询事件有数据返回之后，都需要再次执行ppoll()调用，以重新

配置这些相同的文件描述符集。相比之下，在使用epoll时，文件描述符集合则由内核对象进行管理。

以下从epoll相关的系统调用中可以看出这一点。下面，程序hello-ring-buffer-config.py配置了对output环形缓冲区的访问。

首先，用户空间程序请求在内核中创建一个新的epoll实例：

```python
epoll_create1(EPOLL_CLOEXEC) = 8 
```

上面的系统调用返回的文件描述符是8。接下来，使用epollCtl()调用，将output环形缓冲区的文件描述符4添加到这个epoll实例的文件描述符集中：

```javascript
epoll_CTL(8, EPOLL_CTL_ADD, 4, {events=EPOLLIN, data={u32=0, u64=0}}) = 0 
```

接下来，用户空间程序使用epoll_wait()来等待环形缓冲区中的数据是否可用。只有当数据可用时，该调用才会返回：

```txt
epoll_wait(8, [events=EPOLLIN, data={'u32=0, u64=0'}], 1, -1, NULL, 8) = 1 
```

当然，如果你使用像BCC或libbpf这样的框架编写代码，你确实不需要深入了解用户空间应用程序如何通过perf缓冲区或环形缓冲区从内核获取信息。但了解这些工作原理是一件有趣的事情，可以帮助你更好地理解系统的运行方式。

然而，你可能需要编写从用户空间访问 map 的代码，如果有一个示例帮助你了解这个过程，将会有所帮助。在本章的早些部分，我们介绍过使用 bpftool 工具来查看 config map 的内容。bpftool 是在用户空间运行的实用工具，我们可以使用 strace 来查看 bpftool 的系统调用，以此获取用户空间访问 map 相关的系统调用信息。

# 4.8 从 BPF map 上读取信息

使用 bpftool 读取 config map 内容时，可以使用以下命令查看其执行的 bpf() 系统调用：

```txt
$ strace -e bpf bpftool map dump name config 
```

正如你所看到，该命令将返回两个主要步骤：

- 遍历所有 map，查找 config map。  
- 如果找到匹配的 map，则遍历该 map 中的所有元素。

# 4.8.1 查找 map

当 bpftool 遍历所有的 map，查找 config map 时，输出结果开始会重复出现一系列类似如下的 bpf() 系统调用：

```txt
bpf(BPF_MAP_GET_NEXT_ID，{start_id=0,...},12）=0 ①   
bpf(BPF_MAP_GET_FD_BY_ID，{map_id=48...，12）=3 ②   
bpf(BPF_OBJ_GET_INFO_BY_FD,{info={bpf_fd=3，..}，16）=0 ③
```

```javascript
bpf(BPF_MAP_GET_NEXT_ID，{start_id=48，...，12）=0 bpf(BPF_MAP_GET_FD_BY_ID，{map_id=116，...，12）=3 bpf(BPF_OBJ_GET_INFO_BY_FD，{info={bpf_fd=3...}}，16）=0
```

BPF_MAP_GET_NEXT_ID用于获取start_id的下一个值作为下一个map的ID。  
BPF_MAP_GET_FD_BY_ID返回指定mapID的文件描述符。  
BPF_OBJ_GET_INFO_BY_FD 查询文件描述符所指对象 map 的相关信息。这些信息包括其名称，以便 bpftool 可以查询它要查找的 map。  
④ 重复以上的系统调用，获取第一步的下一个 map ID。

每个加载到内核中的 map 都会触发这三个系统调用，这些调用中使用 start_id。

和 map_id 变量记录当前的 map ID 和下一个的 map ID。当没有可查询的 map 时，重复模式就结束了，BPF_MAP_GET_NEXT_ID 将返回 ENOENT 值，如下所示：

```txt
bpf(BPF_MAP_GET_NEXT_ID, {start_id=133,...}, 12) = -1 ENOENT (No such file or directory) 
```

如果找到了匹配的 map，bpftool 会保留其文件描述符，以便从 map 中读取元素。

# 4.8.2 读取 map 元素

此时，bpftool已经获取configmap的文件描述符。接下来，让我们来看看读取该map信息的系统调用：

bpf(BPF_MAP_GET_NEXT_KEY，{map_fd=3,key=NULL, ①   
next_key $\equiv$ 0xaaaaf7a63960}，24） $= 0$ bpf(BPF_MAP LOOKUPElem，{map_fd=3,key $\equiv$ 0xaaaaaf7a63960, ②   
value $\equiv$ 0xaaaaaf7a63980，flags $\equiv$ BPF_ANY}，32） $= 0$ [{"key":0, "value":{ "message":"Hey root!" }   
bpf(BPF_MAP_GET_NEXT_KEY，{map_fd=3,key $\equiv$ 0xaaaaaf7a63960, ④   
next_key $\equiv$ 0xaaaaaf7a63960}，24） $= 0$ bpf(BPF_MAP LOOKUPElem，{map_fd=3,key $\equiv$ 0xaaaaaf7a63960,   
value $\equiv$ 0xaaaaaf7a63980，flags $\equiv$ BPF_ANY}，32） $= 0$ 1，{ "key":501, "value":{ "message":"Hi user 501!" }   
bpf(BPF_MAP_GET_NEXT_KEY，{map_fd=3,key $\equiv$ 0xaaaaaf7a63960， ⑤   
next_key $\equiv$ 0xaaaaaf7a63960}，24） $= -1$ ENOENT (No such file or directory) } ⑥   
] $\text{+ + + }$ exited with O +++

$\bullet$ 首先，通过使用bpf(BPF(Map_GET_NEXT_KEY)系统调用，在map中查找有效的键。在此调用中，参数key是指向某个键的指针，该系统调用会返回

该键之后的下一个有效键。如果 key 参数为 NULL 指针，则表示请求 map 中的第一个有效键。内核会将找到的键写入到由 next_key 指针指定的位置。

在提供键的情况下，系统将请求相应的值，并将该值写入到由value参数指定的内存地址中。  
此时，bpftool 成功获取了第一个键值对的内容，并将这些信息显示在屏幕上。  
④ 在这里，bpftool将移至map中的下一个键，获取其对应值，并把该键值对显示在屏幕上。  
BPF_MAP_GET_NEXT_KEY的下一次调用返回ENOENT，表示map中不再有任何条目。  
此时，bpftool完成了向屏幕的输出并随之退出。

请注意，在这个场景中，bpftool进程为configmap分配的文件描述符编号是3。而在hello-buffer-config.py程序中，操作同一个map使用的文件描述符编号则是4。正如之前所述，文件描述符是针对每个进程独立分配的。

通过分析 bpftool 的运行机制，我们可以了解到用户空间程序是如何遍历可用的 map 以及如何访问存储在 map 中的键值对。

# 4.9总结

本章向你展示了用户空间代码如何利用bpf()系统调用加载eBPF程序及创建eBPF map。你学习了如何使用bpf(BPF_PROG_LOAD)系统调用和bpf(BPF_MAP_CREATE)系统调用来加载程序和创建eBPF map。

你学到了内核是如何跟踪eBPF程序和eBPF map的引用数，并会在引用计数降至零时对它们进行释放。此外，你还掌握了如何将BPF对象固定（Pin）到文件系统，以及如何通过使用BPF链接来为BPF程序创建额外的引用。

你已经看到如何使用bpf(BPF_MAP_UPDATE_ELEM)系统调用从用户空间向map中添加条目。同样，还有bpf(BPF_MAPLOOKUP_ELEM)系统调用和bpf(BPF_MAP_DELETE_ELEM)系统调用，分别用于从map中检索和删除条目。此外，bpf(BPF_MAP_GET_NEXT_KEY)系统调用被用于查找map中的下一个键，以此能够遍历map中的所有有效条目。

你已经看到用户空间程序是如何通过perf_event_open()系统调用和ioct1()系统调用将eBPF程序“附加”至perf的kprobe事件上。对于其他类型的eBPF程序，其“附加”方法可能会有显著差异，有些需要使用bpf()系统调用，例如，bpf(BPF_PROG_ATTACH)系统调用可用于“附加”cgroup程序上；bpf(BPF_raw_tracePOINT_OPEN)用于“附加”至tracepoint上（参见本章最后的练习5）。

同时，本章还介绍了如何利用bpf()系统调用中的BPF_MAP_GET_NEXT_ID、BPF_MAP_GET_FD_BY_ID和BPF_OBJ_GET_INFO_BY_FD这几个命令来查询内核中的map或其他对象。

尽管本章没有介绍所有bpf()系统调用的命令，但已经提供了一个关于bpf()系统调用的全面概览。

在本章中，你还看到了BTF数据如何被加载进内核。我提到过，bpftool利用这些信息来解析数据结构的格式，以便更好地进行输出展示。至于BTF数据的具体内容，以及它如何使eBPF程序在不同内核版本间提供移植性，这些内容将在下一章进行详细介绍。

# 4.10 练习

如果你想进一步了解bpf()系统调用，可以尝试以下几种方法：

1. 确认bpf(BPF_PROG_LOAD)系统调用中的insn_cnt字段是否与使用bpftool将eBPF程序转译为字节码时输出的指令数量相一致（这一点在

bpf() 系统调用的手册页中有详细描述，参见 https://oreil.ly/NJdIM)。

2. 运行两个示例程序，会创建两个 config map。如果执行 bpftool map dump name config 命令，输出结果将展示两个不同 map 及其内容的信息。使用 strace 运行两个示例程序时，系统调用的输出会显示追踪两个不同的文件描述符，并且，你可以查看检索 map 信息，以及获取其存储的键值对的过程。

3. 运行其中一个示例程序，使用 bpftool map update 修改该程序的 config map。然后，使用 sudo -u username 命令检查这些配置更改是否被 eBPF 程序正确接收和处理。

4. 在运行hello-buffer-config.py时，通过使用bpftool工具将eBPF程序Pin（固定）到BPF文件系统，具体操作如下所示：

bpftool prog pin name hello /sys/fsbpbf/hi

退出正在运行的程序后，使用 bpftool prog list 命令检查是否仍然加载了名为“hello”的程序。如果存在，可以通过执行 rm/sys/fs/bpf/hi 删除对该程序的引用，从而清理程序。

5. 在系统调用层面，附加到tracepoint比附加到kprobe要简单得多，因为它只涉及bpf()系统调用。请尝试修改hello-buffer-config.py程序，使用BCC的RAW_TRACEPOINT_PROBE宏来将程序“附加”到tracepoint sys-enter上（如果你之前完成了第2章中的练习，就已经有适用的程序可以使用）。你不需要在Python代码中明确编写“附加”程序的代码，因为BCC会为你处理。使用strace运行该程序，你将会看到类似下面的系统调用：

bpf(BPF_raw_TRACEPOINT_OPEN, {raw_tracepoint={'name="sys-enter", prog_fd=6}}, 128) = 7

内核中的跟踪点是 raw_tracepoint sys-enter。eBPF 程序的文件描述符为 6，已被“附加”到该跟踪点。这意味着只要内核执行到该跟踪点，就会触发 eBPF 程序。

6. 运行 BCC 的 libbpf 工具集中的 opensnoop 程序。该工具会创建一些 BPF 链接。你可以使用 bpftool 命令查看这些 BPF 链接，具体操作如下图所示：

```txt
$ bpftool link list
116: perf_event prog 1849
bpf_cookie o
pids opensnoop(17711)
117: perf_event prog 1851
bpf_cookie o
pids opensnoop(17711) 
```

此处输出的程序ID是1849和1851，与已加载的eBPF程序是一致的：

```csv
$ bpftool prog list
...
1849: tracepoint name tracepoint_syscalls_sys-enter_openat
tag 8ee3432dcd98ffc3 gpl run_time_ns 95875 run_cnt 121
loaded_at 2023-01-08T15:49:54+0000 uid 0
xlated 240B jited 264B memlock 4096B map_ids 571,568
btf_id 710
pids opensnoop(17711)
1851: tracepoint name tracepoint_syscalls_sys_exit_openat
tag 387291c2fb839ac6 gpl run_time_ns 8515669 run_cnt 120
loaded_at 2023-01-08T15:49:54+0000 uid 0
xlated 696B jited 744B memlock 4096B map_ids 568,571,569
btf_id 710
pids opensnoop(17711) 
```

7. 在运行 opensnoop 程序时，请尝试使用 bpftool link pin id 116 /sys/fs/bpf/mylink 命令，将其中一个链接固定到 /sys/fs/bpf/mylink（使用 bpftool link list 命令输出中的一个链接 ID）。你应该会发现，即使你终止了 opensnoop 程序，内核中仍会加载该链接和相应的程序。  
8. 如果查看第5章的示例代码，你会发现一个使用libbpf库重新编写的hello-buffer-config.py版本。该库会自动为加载到内核中的程序设置一个BPF链接。使用strace查看该程序的bpf()系统调用，以及bpf(BPFLINK_CREATE)系统调用。

# 第5章

# CO-RE、BTF和Libbpf

第4章中，我们首次接触了BTF（BPF Type Format的缩写）。本章着重讨论BTF存在的原因以及如何通过BTF实现eBPF程序在不同内核版本之间的迁移。BTF是BPF的一项关键技术，采用了“一次编译，到处运行”（CO-RE）的方法，解决了在不同内核版本之间实现eBPF程序的可移植性的问题。

许多eBPF程序访问内核数据结构时，都需要包含相关的Linux头文件，以便eBPF代码可以正确地访问这些数据结构中的字段。然而，Linux内核在不断发展中，这意味着内核数据结构在不同的版本之间可能会发生变化。如果你将一个编译好的eBPF对象文件加载到不同内核版本的机器上，数据结构将无法保证一致性注1。

CO-RE方法是一个重大的进步，它使用高效的方式解决了这个可移植性问题。该方法允许eBPF程序在编译时包含数据结构的信息，并提供了一种调整字段访问方式的机制。如果程序运行在数据结构不同的主机上，程序不会访问目标机器的内核中不存在的字段或数据结构，从而实现程序在不同的内核版本之间的可移植性。

在深入探讨CO-RE的工作原理之前，我们先看下最初在BCC项目中实现跨内核可移植性的方法，以理解为什么CO-RE方法如此令人兴奋。

# 5.1 BCC对可移植性的处理方式

在第2章中，我们用BCC（https://oreil.ly/ReUtn）来展示“Hello World”这样一个基本的eBPF程序示例。BCC项目是编写eBPF程序的第一个流行项目，为用户空间和内核方面的BPF程序提供了一个相对易用的框架，适用于没有太多内核经验的程序员。为了解决不同内核间的可移植性问题，BCC采用了在主机上即时编译eBPF代码的方式。这种方法存在一些问题：

- 需要在即将运行代码的主机上安装编译工具链和内核头文件（这些头文件并不总是默认存在的）。  
- 在工具启动之前，必须等待编译完成，这可能会导致每次启动都需要几秒钟的延迟。  
- 如果要在很多主机上运行，每台主机都要重复进行编译，这是对计算资源的巨大浪费。  
- 一些基于BCC的项目，可以将它们的eBPF源代码和工具链打包到一个容器镜像中，这样可以更容易地分发到每台机器上。但这并不能解决需要确保内核头文件存在的问题，而且如果每台主机上运行了几个这样的BCC容器，可能会导致更多的重复。  
- 嵌入式设备可能没有足够的内存资源来运行编译步骤。

了解了以上问题，如果你计划开发一个新的，并且很重要的eBPF项目，特别是这个项目还要分发给其他人使用的话，不建议使用这种传统BCC方法。在本书中，我们提供了一些BCC的示例，这是学习eBPF基本概念的好方法，特别是使用Python编写用户空间代码非常简洁易读。如果你熟悉BCC，并且希望快速搭建一个解决方案，这也是一个完全不错的选择。但严肃地说，对

于现代eBPF开发来说这并不是最佳的方法。最新的CO-RE方法为eBPF程序的跨内核可移植性问题提供了更好的解决方案。

![](images/02b81a0ec8f88a0d6d31a2d25504ef43d845794500098406c727165079855b9d.jpg)

在 github.com/iovisor/bcc 项目（https://oreil.ly/ReUtn）中包含了一系列用于监控 Linux 机器行为的命令行工具，这些工具的原始版本位于 tools（https://oreil.ly/fI4w_）目录中，大部分是使用 Python 实现的，使用了本节中描述得传统方法来解决可移植性问题。

在BCC的libbpf-tools（https://oreil.ly/ke7yq）目录中，可以找到这些工具更新的版本，是用C编写的，利用了libbpf库和CO-RE，不会出现刚刚列举的问题。这些都是非常实用的工具！

# 5.2 CO-RE 概述

CO-RE方法由几个元素组成注2,3：

# BTF

BTF（https://oreil.ly/iRCuI）是一种用于表示数据结构和函数签名结构的格式。在CO-RE中，可以使用BTF确定编译时和运行时使用的结构之间的差异。bpftool工具可以把BTF数据结构转储成可读的格式输出。从Linux 5.4版本起，内核开始支持BTF。

# 内核头文件

Linux内核源代码包括描述其使用的数据结构的头文件，这些头文件可能在Linux版本之间发生变化。eBPF程序员可以选择包含多个单独的头文件，或者，如本章所述，可以使用bpftool从运行的系统中生成一个名为vmlinux.h的头文件，该头文件包含BPF程序可能需要的有关内核的所有数据结构信息。

# 编译器支持

Clang 编译器对此进行了增强（https://oreil.ly/6xFJm），当使用 -g 标志编译 eBPF 程序时，它会包含 CO-RE 重定位信息，这些重定位信息是从描述内核数据结构的 BTF 信息派生而来的。GCC 编译器的版本 12 也为 BPF 编译目标添加了 CO-RE 支持（https://oreil.ly/_6PEE）。

# 支持数据结构重定位的库

在用户空间程序将eBPF程序加载到内核时，CO-RE方法要求对字节码进行调整，以补偿编译时存在的数据结构与即将运行的目标机器上的数据结构之间的任何差异，调整的依据是编译到对象中的CO-RE重定位信息。有几个库可以解决这个问题，libbpf(https://oreil.ly/E742u)是包含这种重定位功能的原始C库，Cilium eBPF库为Go程序员提供了同样的功能，而Aya则为Rust提供了这一功能。

![](images/5377ec2a1aad0d1b07b65cbcd2c0db98d82b8d8bd2a768f8630806ab2f686f11.jpg)

Andrii Nakryiko 写过一篇很棒的文章 (https://oreil.ly/aeQJo)，介绍了 CO-RE 的背景、工作原理和使用方法。他还写过一本经典的《BPF CO-RE 参考指南》（https://oreil.ly/lbW_T），如果你打算自己编写代码，请务必阅读该指南。他的《libbpf-boostrap 指南》（https://oreil.ly/_jet-) 是另一本必读书，它介绍了如何使用 CO-RE + libbpf + skeletons 从零开始构建 eBPF 应用程序。

现在，你已经对CO-RE的要素有了一个大致的了解。接下来，我们一起开始探索BT是如何工作的。

# 5.3 BPF 类型格式

BTF信息描述了数据结构和代码在内存中的结构。这些信息有多种不同的用法。

# 5.3.1 BTF用例

在这里讨论BTF的主要原因是，通过BTF可以了解eBPF程序编译时的数据结构与运行时环境的数据结构之间的差异，以便可以在程序加载到内核时进行调整。本章稍后将讨论重定位过程，现在，让我们来考虑一下BTF信息的其他用途。

因为BTF信息包含数据结构的结构以及数据结构中每个字段的类型，就有可能根据BTF信息以可读的形式清晰地打印出数据结构的内容。从计算机的角度来看，字符串由一系列字节组成，将这些字节转换成字符后，字符串便易于理解。在上一章中，就有这样一个例子，bpftool工具使用BTF信息输出格式化的map信息。

BTF 信息还包括行和函数信息，这些信息使 bpftool 工具能够在翻译或 JIT 程序转储的输出中插入源代码，这在第 3 章中已有介绍。当你读到第 6 章时，你还会看到源代码信息与验证日志输出交错在一起，这同样来自 BTF 信息。

BPF自旋锁也是依赖于BTF信息。自旋锁用于阻止两个CPU内核同时访问相同的map值。该锁必须是map值的结构体的一部分，就像下面这样：

```c
struct my_value {
    ... <other fields>
    struct bpfspin_lock lock;
    ...
    other fields
}; 
```

在内核中，eBPF程序可以使用bpf.spin_lock()和bpf.spin_unlock()这两个Helper函数获取和释放锁。这两个Helper函数是使用BTF信息来确定在结构体中锁字段的位置，并根据该位置执行相应的操作。

![](images/8ca31f4be184cd47db6a09d1141fc841b90491b097f689a10ff9aabb13d7122a.jpg)

5.1 版本的内核中新增了 spin_lock 支持。spin_lock 的使用有很多限制，例如：只能用于 hash 类型或数组类型 map，不能用于跟踪或 socket 过滤器类型的 eBPF 程序。有关 spin_lock 的更多信息，请参阅 lwn.net 中关于 BPF 并发管理的文章（https://oreil.ly/kAyAU）。

# 5.3.2 使用 bpftool 工具列出 BTF 信息

像显示程序和 map 信息一样，你可以使用 bpftool 工具来显示 BTF 信息。以下命令用于列出加载到内核中的所有 BTF 信息：

```txt
bpftool btf list   
1: name [vmlinux] size 5843164B   
2: name [aes_cecipher] size 407B   
3: name [cryptd] size 3372B   
...   
149: name <anon> size 4372B prog_ids 319 map_ids 103 pins hello-buffer-co(7660)   
155: name <anon> size 37100B pins bpftool(7784) 
```

（为简洁起见，省略了结果中的许多内容。）

列表中的第一个条目是 vmlinux，它对应于前面提到的 vmlinux.h 文件，该文件保存了当前运行的内核的 BTF 信息。

![](images/fe13540b26a5eef120d380ce4a4d03880ff6e9bd6edb13aefa8a68441bd56109.jpg)

本章前面的一些示例重复使用了第4章中的示例程序。在后面章节中，你会发现新的示例，源码地址是github.com/lizrice/learning-ebpf, chapter5目录下。

运行第4章的hello-buffer-config示例，再运行上面的bpftool命令，得到的输出结果如下所示。该程序的BTF信息对应是149:开头的那行信息：

```txt
149: name <anon> size 4372B prog_ids 319 map_ids 103  
pids hello-buffer-co(7660) 
```

这一行输出的含义是什么？

- BTF 信息的 ID 是 149。  
- BTF 信息块的大小约 4KB。  
- 该信息块被prog_id319的BPF程序和map_id103的map使用。

- hello-buffer-config 程序的进程 ID 为 7660（见括号），也使用了该 BTF 信息块。hello-buffer-config 是可执行文件名，该名称已被截断为 15 字符变成 hello-buffer-co。

下面是使用 bpftool 显示 hello-buffer-config 进程中 hello 程序涉及的程序、map 和 BTF 标识符：

```txt
bpftool prog show name hello   
319: kprobe name hello tag a94092da317ac9ba gpl loaded_at 2022-08-28T14:13:35+0000 uid 0 xlated 400B jited 428B memlock 4096B map_ids 103,104 btf_id 149 pids hello-buffer-co(7660) 
```

这两组输出的信息中唯一不同的地方是后面程序中多了一个 map_id，即 104。这是 perf 事件缓冲区 map，它不使用 BTF 信息；因此，它没有出现在 BTF 相关的输出中。

bpftool工具不仅可以转储程序和map的内容，还可以用来查看Blob数据的BTF类型信息。

# 5.3.3BTF类型

如果知道BTF信息的ID，就可以使用bpftool btf dump id <id>命令查看内容。例如：使用前面的ID149，可以得到69行输出，每行是一个类型定义。我们通过解释前几行的内容，方便各位很好地理解后面的内容。前几行的BTF信息与config hash map有关。在源代码中是这样定义的：

```txt
struct user msg_t {
    char message[12];
}; 
```

该 hash table 的键类型为 u32，值类型为 struct user msg_t，该结构体中包

含一个12字节message字段。让我们看看在BTF信息中是如何定义这些类型的。

BTF输出中前三行如下：

[1] typedefef 'u32' type_id=2   
[2] TYPEDEF 'u32' type_id=3   
[3] INT 'unsigned int' size=4 bits_offset=0 nr_bits=32 encoding=(none)

每行开头方括号中的数字是 type ID（因此第一行以 [1] 开头，定义 type_id 为 1，以此类推）。让我们详细了解一下这三种类型详细介绍：

- 类型1定义了一个类型别名为u32，它的type_id为2（即以[2]开头的一行中定义的类型）。如你所知，hash table中的键就是u32类型。  
- 类型2的别名是_u32，它的type_id为3。  
- 类型3是一个整数类型，名称为unsigned int，长度为4字节。

这三种类型都是32位无符号整数类型。在C语言中，整数的长度与平台有关，因此Linux用u32等类型明确定义了特定长度的整数。在本机中，u32与无符号整数相对应。用户空间代码在引用这些类型时，应使用以下划线为前缀的同义词，如_u32。

BTF 输出中接下来的几个类型解释如下：

[4]STRUCT 'user msg t' size $= 12$ vlen $= 1$ 'message' type_id $= 6$ bits_offset $= 0$ [5]INTchar'size $= 1$ bits_offset $= 0$ nr_bits $= 8$ encoding $\equiv$ (none)   
[6]ARRAY'(anon)'type_id $= 5$ index_type_id $= 7$ nr_elements $= 12$ [7]INT'_ARRAY_SIZE_TYPE'_size $= 4$ bits_offset $= 0$ nr_bits $= 32$ encoding $\equiv$ (none)

这些字段与配置参数中user msg_t结构体有关：

- 类型4本身是user msg_t结构体，共有12个字节长。它包含一个名为message的字段，由类型6定义。strlen字段表示该定义中有多少个字段。

- 类型5被命名为char，是一个1字节的整数，完全符合C程序员对“char”类型的预期定义。  
- 类型6字段定义了包含12个元素数组的类型。每个元素的类型为类型5（即char类型），数组的索引是类型7。  
- 类型7是一个长度为4个字节的整数。

了解了这些类型的定义，可以清晰地了解user msg_t结构体在内存中的储存方式，如图5-1所示。

![](images/a22e869bf693b83759370b4629bdc5aa4830f334dc7312afc641eb5392f61630.jpg)  
图5-1：一个user msg_t结构体占用12字节内存

到目前为止，所有类型的bits_offset都设置为0，但下一行输出中的结构体展示有多个字段的情况：

```txt
[8] STRUCT '____btf_map_config' size=16 vlen=2
	'key' type_id=1 bits_offset=0
	'value' type_id=4 bits_offset=32 
```

这是名为 config 的 map 中键值对的结构体定义。在源代码中并没有定义 __utf_map_config 这个类型，它是由 BCC 生成的。其中键的类型是 u32，值的类型是 user msg_t 结构体。它们对应于前面示例中提到的类型 1 和类型 4。

该结构体的BTF信息中另一个重要部分是，值是从结构体的32位以后开始的。这完全是符合逻辑的，因为前32位用来保存键字段的信息。

![](images/779a44c568424fe90448cff3824ae4c8d208bfe795cb72328e45791106c1e0d5.jpg)

在C语言中，结构体字段会自动与边界对齐，因此不能想当然地认为在内存中一个字段总是紧跟的前一个字段之后。例如：

```txt
struct something {
    char letter;
    u64 number;
} 
```

在letter字段之后，number字段之前将预留7个字节的内存空间，这样64位的number字段在内存中就可以整齐存储在被8整除的内存空间中。

在某些情况下，可以打开编译器“打包”功能来避免这些未使用的空间，但这样会降低性能，至少在我的经验中，一般不这么做。更常见的情况是，C语言程序员手工设计结构，以有效利用空间。

# 5.3.4 BTF中的map

上面的示例中有提到BTF中map相关的信息。本章中我们一起看下BTF是如何创建map并传递给内核的。

在第4章有提到map是由BTF(BPF(Map_CREATE)创建的，创建时会调用内核中定义的bpf_attr结构体作为参数（https://oreil.ly/PLrYG），该结构体定义如下（某些细节省略）：

```c
struct { /* anonymous struct used by BPF_MAP_CREATE command */  
    u32 map_type; /* one of enum bpf_map_type */  
    u32 key_size; /* size of key in bytes */  
    u32 value_size; /* size of value in bytes */  
    u32 max_entries; /* max number of entries in a map */ 
```

```c
char map_name[BPF_OBJ_NAME_LEN];  
...  
_u32 btf_fd; /\* fd pointing to a BTF type data */  
_u32 btf_key_type_id; /\* BTF type_id of the key */  
_u32 btf_value_type_id; /\* BTF type_id of the value */  
}; 
```

在引入BTF之前，bpf_attr结构体中并没有btf_*字段，内核也不了解键或值的结构，只能key_size和value_size字段定义需要多少内存量，就分配这么多字节数。开启BTF之后，内核可以通过定义了键和值类型的BTF信息进行解析设置，bpftool等工具也可以根据类型信息进行整齐的打印（如前所述）。有趣的是，传递给键和值的是单独的BTF类型ids。不过，内核并没有用刚才定义的_btf_map_config结构体来定义map，它只是被BCC在用户空间侧使用。

# 5.3.5函数和函数原型的BTF数据

到目前为止，示例中输出的BTF数据是与数据类型有关的内容，但BTF数据还包含有关函数和函数原型的信息。下面是描述了同一个BTF数据中hello函数的信息：

```txt
[31] FUNC-Proto '(anon)' ret_type_id=23 vlen=1'ctx'type_id=10 
```

```txt
[32] FUNC 'hello' type_id=31 linkage=static 
```

类型32定义了名为hello的函数，类型为上一行的类型31。类型31是一个函数原型定义，返回值是type_id为23，输入参数是一个名为ctx的单参数（vlen=1），该参数的type_id为10。为完整起见，下面展示了该函数引用的类型定义：

```javascript
[10] PTR ' (anon)' type_id=0 
```

```txt
[23] INT 'int' size=4 bits_offset=0 nr_bits=32 encoding=SIGNED 
```

类型10是一个匿名指针注4，默认类型为0，该类型不会在BTF输出中显式定义，它是一个void指针。

类型23说明函数的返回值是一个4字节的整数，“encoding $\equiv$ SIGNED”表示这是一个有符号的整数，也就是说，它既可以是正值，也可以是负值。这与hello-buffer-cong.py源代码中的函数定义相对应，如下所示：

```txt
int hello(void *ctx) 
```

到目前为止，所展示的BTF信息示例来自于一个BTF数据块的内容。接下来看看如何只获取与特定map或程序相关的BTF信息。

# 5.3.6 查看 map 和程序的 BTF 数据

bpftool工具可以很容易用于查看特定map相关的BTF类型。例如，下面是使用bpftool查看config map的BTF数据的输出结果：

```txt
bpftool btf dump map name config 
```

```txt
[1] TYPEDEF 'u32' type_id=2 
```

```txt
[4] STRUCT 'user msg_t' size=12 vlen=1 'message' type_id=6 bits_offset=0 
```

你也可以使用 bpftool btf dump prog <prog identity> 命令查看与特定程序相关的 BTF 信息。请查阅手册 (https://oreil.ly/lCoV5) 了解更多内容。

![](images/ef0226a5a5821c00b377b6ea0d18fe5eb5f83a115684ab5cf2abeb3b1d1ce3ef.jpg)

如果想更好地理解BTF类型数据是如何生成和去重的，可以参考Andrii Nakryiko (https://oreil.ly/0-a9g)的另一篇精彩博文。

到目前为止，我们介绍了BTF如何描述数据结构和函数。我们知道用C语言编写eBPF程序需要定义类型和结构体的头文件，接下来将介绍为eBPF程序创建需要的任何内核数据类型的头文件有多简单。

# 5.4生成内核头文件

在启用BTF的内核上运行bpftool btf list命令，你会看到许多已存在的BTF数据块，看起来就像这样：

```txt
$ bpftool btf list
1: name [vminux] size 5842973B
2: name [aes_cecipher] size 407B
3: name [cryptd] size 3372B 
```

该列表中第一项是名为 vmlinux 的 BTF 信息，包括主机注5内核使用的所有数据类型、结构体和函数的定义。

eBPF程序需要引用内核中的数据结构和类型定义。在没有CO-RE的时代，通常要从Linux内核源代码的众多头文件中找出需要用的结构体定义，现在有了更简单的方法，因为支持BTF的工具可以根据BTF信息生成包含内核信息的头文件。

该头文件通常命名为 vmlinux.h，可以使用 bpftool 工具生成，如下所示：

```txt
bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h 
```

该文件定义了内核的所有数据类型，因此，将 vmlinux.h 文件放在 eBPF 程序源代码中就可以根据需要使用任何 Linux 数据结构。在将源代码编译成 eBPF 对象文件后，该对象将包含与这个文件中定义相匹配的 BTF 信息。当程序要在其他机器运行时，用户空间程序在加载过程中，会根据编译时 BTF 信息与目标机器内核上的 BTF 信息之间的差异，对数据结构进行调整。

自Linux内核5.4版本开始 $^{注6}$ ，BTF信息以 /sys/kernel/btf/vmlinux 文件的形式存放在内核中，也可以为旧的内核生成libbpf库能够用的原始BTF数据。也就是说，如果你的目标机器没有BTF信息，你可以自己提供相应的BTF数

据来运行一个支持CO-RE的eBPF程序。你可以在BTFHub(https://oreil.ly/mPSO0)上找到如何生成BTF文件的信息，以及不同Linux发行版的BTF文件存档。

![](images/3ceb3fa0c02cadab82553e45bb79643b67e599f2a5bd87bef714d2064d4487fc.jpg)

如果你想更深入地了解这个主题，BTFHub仓库（https://oreil.ly/mPSO0）中还提供了关于BTF内部的更多阅读资料。

接下来章节，让我们一起看下如何使用此方法和其他策略来编写可以在不同的内核之间移植的eBPF程序。

# 5.5 支持 CO-RE 的 eBPF 程序

eBPF程序是运行在内核中的，本章的后半部分会展示一些用户空间代码，这些代码将与内核中运行的代码进行交互。本节重点先讨论内核侧BPF程序相关的内容。

正如大家已经了解的，eBPF程序会被编译成eBPF字节码，到目前为止，支持这一编译过程的编译器有Clang或用于编译C语言代码的gcc，以及Rust编译器。在第10章中，我会讨论使用Rust编译器的一些选项。在本章中，我将假设你是使用C语言的libbpf库编写程序，并使用Clang编译器来编译。

在本章的剩余部分，我们来看一个叫hello-buffer-config的示例应用。它和上一章中使用BCC框架的hello-buffer-config.py示例非常相似，但这个版本是用C语言编写的，使用了libbpf库和CO-RE。

如果你想把基于BCC编写的eBPF代码迁移到libbpf上，可以参考Andrii Nakryiko的网站上（https://oreil.ly/iWDcv）发布的优秀且全面的指南。BCC提供了一些方便的快捷方式，但使用libbpf库时并不完全相同，libbpf库也提供了自己的一套宏和库函数，来简化eBPF程序员的工作。在我介绍示例的过程中，我会指出BCC和libbpf方法之间的一些差异。

![](images/8266e48a7b337792071e742286a1c6a59427904117d06ee1e181e4fd141f8c74.jpg)

你可以在 github.com/lizrice/learning-ebpf 仓库的 chapter5 目录中找到本节提到的用 C 语言实现的 eBPF 程序示例。

首先，我们来看一下hello-buffer-config.bpf.c，它实现了在内核中运行的eBPF程序。在本章后面，我会向你展示hello-buffer-config.c中的用户空间代码，它负责加载程序并显示输出，就像第4章中的BCC实现例子中的Python代码一样。

像任何C程序一样，一个eBPF程序也需要包含一些头文件。

# 5.5.1头文件

Hello-buffer-config.bpf.c文件的前几行指定了所需的头文件：

```c
include "vmlinux.h" #include <bpf/bpfHelpers.h> #include <bpf/bpf_tracing.h> #include <bpf/bpf_core_read.h> #include "hello-buffer-config.h" 
```

这五个文件分别是 vmlinux.h 文件，几个 libbpf 的头文件，以及我自己写的一个应用程序特定的头文件。让我们来看看为什么这是一个 libbpf 程序所需要的头文件的典型模式。

# 5.5.1.1 内核相关的头文件信息

如果你编写的 eBPF 程序需引用内核的数据结构或类型，最简单的方法是引用本章前面介绍过的 vmlinux.h 文件。或者你也可以引用 Linux 源代码中单独的某个头文件。当然如果不觉得麻烦的话，也可以在代码中自己手动定义类型。如果要使用 libbpf 库中的 BPF 的 helper 函数，需要包含 vmlinux.h 或 linux/ types.h 来引用 BPF 的 helper 函数源代码中定义的类型，比如 u32, u64 等。

vmlinux.h文件是从内核源代码的头文件中派生出来的，但不包含其中的

define值。例如，如果eBPF程序要解析以太网数据包，你可能需要定义一个常量来表明这个数据包的协议是什么（比如0x0800表示这是一个IP数据包，或者0x0806表示这是一个ARP数据包）。如果你不引用内核定义这些值的if_ether.h文件（https://oreil.ly/hoZzP），你就需要在自己的代码中复制一系列的常量值。在hello-buffer-config中我不需要这些值定义，但你会在第8章中看到另一个相关的例子。

# 5.5.1.2 libbpf 相关的头文件

如果要在eBPF代码中使用BPF的helper函数，需要引用libbpf的头文件来获取它们的定义。

![](images/8aff4e7711ef2a087a918d8aa1abe7d55a16356aa37c60330af5e61ad9ea6709.jpg)

关于libbpf库，有一点可能有点令人困惑，它仅是一个用户空间库。你会发现自己在用户空间和eBPF的C代码中都会包含来自libbpf的头文件。

在编写这本书的时候，常见的做法是将 libbpf 库作为一个子模块包含在 eBPF 项目中，并且从源代码构建和安装，也是我在本书的示例仓库中的用法。如果你使用此方式，你只需要在 libbpf/src 目录运行 make install 即可。我认为不久之后，libbpf 库作为 Linux 发行版中一个包提供会很常见，特别是现在 libbpf 库发布了 1.0 里程碑版本 (https://oreil.ly/8BFq6)。

# 5.5.1.3 特定应用程序的头文件

在开发应用程序时，通常需要应用程序特定的头文件，用来定义用户空间和eBPF程序共用的结构体。在我的例子中，hello-buffer-config.h头文件定义了data_t结构体，用于把事件数据从eBPF程序传递到用户空间。这个结构体的代码和你之前看到的BCC版本几乎一样，如下所示：

```txt
struct data_t {
    int pid;
    int uid;
    char command[16];
    char message[12]; 
```

```javascript
char path[16];   
};
```

与之前版本的唯一区别是，增加了一个叫做 path 的字段。

因为在hello-buffer-config.c的用户空间代码中也会引用这个结构体，所以我们将这个结构体定义放在一个单独的头文件中。在BCC版本中，内核和用户空间的代码都定义在一个文件中，BCC在后台做一些处理，使这个结构体可以在Python用户空间代码中被使用。

# 5.5.2定义map

在包含了头文件之后，hello-buffer-config.bpf.c源代码中的接下来几行定义了map结构体，如下所示：

```txt
struct {
    __uint(type, BPF_MAP_TYPE_PERF_EVENT_ARRAY);
    __uint(key_size, sizeof(u32));
    __uint(value_size, sizeof(u32));
} output SEC(".maps");
struct user msg_t {
    char message[12];
};
} 
```

代码行数比用 BCC 实现的例子中多！在 BCC 中，名为 config 的 Map 是用宏创建的，如下所示：

```c
BPF_HASH(config, u64, struct user msg_t);
```

不使用 BCC 的时候，这个宏是不可用的，因此在使用 libbpf 库编写 C 语言代

码中，你必须用比较长的代码来实现。同时，结构体定义中使用了 __uint 宏和 __type 宏，这些和 __array 宏一起是定义在 bpf/bpfHelpers_def.h (https://oreil.ly/2FgjB) 中，如下所示：

```c
define __uint(name, val) int (*name)[val]  
#define __type(name, val) sizeof(val) *name  
#define __array(name, val) sizeof(val) *name[ ] 
```

按照惯例这些宏通常是用在基于libbpf的程序中，我认为使用宏可以让Map的定义更容易阅读。

![](images/479724258984ee2c6c6229124dc0acba1350e5095864d9fedd950f804210b88f.jpg)

config 这个名字和 vmlinux.h 中的一个定义冲突了，所以这个例子中我把这个 Map 重命名为 “my_config”。

# 5.5.3 eBPF程序中的Section部分

使用libbpf库的eBPF程序要求用SEC()宏标记程序类型，像这样：

```autoit
SEC("kprobe") 
```

这句代码会在编译后的 ELF 对象中生成一个名为 kprobe 的 section, libbpf 库会据此将程序加载为 BPF_PROG_TYPE_KPROBE 类型。在第 7 章我们会进一步讨论不同的程序类型。

使用SEC可以根据程序类型把程序附加到指定的事件上。libbpf库会利用该信息执行自动附加，不需要在用户空间代码中显式地去做这件事。例如，在ARM架构的机器上，可以通过SEC定义使用kprobe将程序自动附加到execve系统调用上，Section定义如下所示：

```txt
SEC("kprobe/_arm64_sys_execute") 
```

这样写的前提需要知道该架构上系统调用的函数名（或者通过目标机器上的 /proc/kallsyms 文件来查找，该文件列出了所有的内核符号，包括函数名）。

libbpf库有更轻松的写法，可以使用k(ret)syscall section，让加载器使用kprobe自动附加到特定架构函数上：

```autoit
SEC("ksyscall/execve") 
```

![](images/d7cc0f53e529672fb499bb190892738af72ec332909ee7560edd8f8962c00ee9.jpg)

libbpf文档中列出了有效的Section名和格式（https://oreil.ly/FhHrm）。过去对Section名的要求比较宽松，所以当你遇到一些libbpf1.0之前的eBPF程序，它们的Section名和有效的集合不匹配。不要因此而困扰！

Section 定义中声明了 eBPF 程序要附加到的什么位置，后面紧跟着就是程序本身。eBPF 程序本身跟之前用 C 语言写的函数一样。在示例代码中的 hello() 和第 4 章的 hello() 函数非常相似。让我们来看看这个版本和之前版本的区别有哪些：

```c
SEC("ksyscall/execve")  
int BPF_KPROBE_SYSCALL(hello, const char *pathname)  
{  
    struct data_t data = {};  
    struct user msg_t *p;  
    data.pid = bpf_get_current.pid_tgid() >> 32;  
    data.uid = bpf_get_current.uid gid() & 0xFFFFFFF;  
    bpf_get_current_comm(&data(command, sizeof(data(command));  
    bpfprobe_read_user_str(&data.path, sizeof(data.path),pathname);  
    p = bpf_mapLOOKUP_element(&my_config, &data.uid);  
    if (p != 0) {  
        bpfprobe_read_kernel(&data.message, sizeof(data.message), p->message);  
    } else {  
        bpfprobe_read_kernel(&data.message, sizeof(data.message), message);  
    }  
    bpf_perf_event_output(ctx, &output, BPF_F_CURRENT_CPU, &data, sizeof(data));  
    return 0; 
```

- 这行代码使用了 libbpf 库中的宏 BPF_KPROBE_SYSCALL(https://oreil.ly/pgIIB)，可以很方便地通过系统调用名称传入参数。对于 execve() 函数，第一个参数是要执行的 eBPF 程序的路径。该 eBPF 程序名是 hello。  
因为宏BPF_KPROBE_SYSCALL可以很轻松地访问到execve()的路径名参数，这里将路径名参数发送到perf缓冲区的数据中。注意，复制内存是需要使用BPF的hepler函数的。  
这行bpf_mapLOOKup_element()是一个BPF的helper函数，用于通过键查找map中对应的值。BCC的等价写法是 $p =$ my_config.lookup(&data.uid)。因为BCC在把C代码传递给编译器之前，会重写这个代码去调用底层的bpf_map.Lookup_element()函数。而libbpf库并没有代码重写的过程<sup>7</sup>，所以必须直接用该helper函数。  
④ 这里有另一个类似的例子，也是直接用helper函数bpf_perf_event_output()，而BCC提供的方便的等价写法是output.perf_submit(ctx, &data, sizeof(data))。

其他唯一的区别还有，在BCC版本中，message字符串会被定义为hello()函数内部的一个局部变量。BCC不支持全局变量（至少在写这本书的时候是这样）。在libbpf版本中，我把它定义为一个全局变量，像这样：

```javascript
char message[12] = "Hello World"; 
```

在 chapter4/hello-buffer-config.py 这个文件中，hello 函数的定义方式也不一样，像这样：

```txt
int hello(void *ctx) 
```

之前提到过BPF_KPROBE_SYSCALL是libbpf库增加的一个很方便使用的宏。你不是必须使用这个宏，但它确实使编程变得更轻松。这个宏做了很多工作，

它将所有函数参数作为这个宏的参数提供给系统调用函数。在这个示例中，提供了一个pathname参数，是个字符串，该字符串保存了一个可执行文件的路径，这是execve()函数系统调用的第一个参数。

你可能会注意到ctx变量在源代码hello-buffer-config.bpf.c中并没有明显地定义，但是在向outputperf缓冲区提交数据时，却能够使用ctx，像这样：

```txt
bpf_perf_event_output(ctx, &output, BPF_F_CURRENT_CPU, &data, sizeof(data)); 
```

ctx 变量确实存在，不过是隐藏在 bpf/bpf_tracing.h(https://oreil.ly/pgIIB) 中的 BPF_KPROBE_SYSCALL 宏定义里，是 libbpf 的一部分，可以在 libbpf 中找到一些相关的注释。这样是非常有用的，尽管使用一个没有明显定义的变量可能会令人有些困惑。

# 5.5.4 使用CO-RE进行内存访问

如果eBPF程序是用于跟踪的，那么程序可以通过bpfprobe_read_ $\ast$ )系列的BPFHelper函数，对内存进行访问（还有一个bpfprobe_write_user()helper函数，但仅“用于实验”(https://oreil.ly/ibcy1))。问题是，eBPF验证器通常不会让eBPF程序像在C语言中那样简单地通过指针读取内存（例如， $x = p->y)$ 注9，这个问题将在下一章介绍。

libbpf库为bpfprobe_read_*()helper函数提供了CO-RE包装器，以利用BTF信息，使内存访问调用在不同的内核版本上具有可移植性。下面是这些包装器的一个例子，它在bpf_core_read.h头文件中定义（https://oreil.ly/XWWyc）：

```txt
define bpf_core_read.dst, sz, src)  
bpfprobe_read_kernel.dst, sz, (const void *)_builtin Preserve_access_index(src)) 
```

如示例，bpf_core_read()直接调用了bpfprobe_read_kernel()，并用_builtin Preserve_access_index()包装了src字段。Clang编译器在编译访问内存该地址的eBPF指令时，会生成一个CO-RE重定位条目。

![](images/7148399aff685eed9d583e86a4ba118503c3b6eb012c219ddac97ffaf0c5f44b.jpg)

_builtin_preserve_access_index()指令是C语言的一种扩展，Clang编译器需要进行一些修改才能生成正确的CO-RE重定位条目。这就是为什么有些C编译器不能生成eBPF字节码的原因。关于Clang编译器为了支持eBPF CO-RE所做的修改信息，你可以在LLVM邮件列表上了解更多（https://oreil.ly/jHTHE）。

在本章后面会看到，当eBPF程序加载到内核时，libbpf会根据CO-RE重定位信息修改程序地址，以适应不同内核的BTF信息。如果src在其包含结构中的偏移量在目标内核上不同，还会重写指令。

libbpf库提供了一个BPF_CORE_READ()宏，可以使用一行代码就完成多次bpf_core_read()调用，而不需要每次指针解引用都调用一次Helper函数。例如，如果你想做类似 $d = a->b->c->d$ 的事情，你可以写以下代码：

struct b_t *b;

struct c_t *c;

bpf_core_read(&b, 8, &a->b);

bpf_core_read(&c, 8, &b->c);

bpf_core_read(&d, 8, &c->d);

更简洁的写法如下：

$d =$ BPF_CORE_READ(a,b,c,d);

你可以用bpfprobe_read_kernel()这个Helper函数来读取d的数据。Andrii的指南对此有一个很好的解释（https://oreil.ly/tU0Gb）。

# 5.5.5 许可证定义

在第3章中我们已经了解到eBPF程序必须声明许可证。示例代码如下：

charLICENSE[]SEC("license") $=$ "DualBSD/GPL";

到现在为止，你已经看完了hello-buffer-config.bpf.c示例的所有代码。接下来看如何把它编译成一个目标文件。

# 5.6 编译支持 CO-RE 的 eBPF 程序

在第3章中，你看到了一个从C代码编译成eBPF字节码的Makefile文件的片段。让我们来看看它使用了哪些选项，以及为什么这些选项对于CO-RE/libbpf程序是必要的。

# 5.6.1 调试信息

你必须给Clang编译器传递-g标志，这样它才会包含调试信息，这对于BTF是必要的。但是，-g标志也会把DWARF调试信息添加到输出的目标文件中，而这对于eBPF程序是不需要的，所以你可以通过运行下面的命令来去掉它，从而减小目标文件的大小：

llvm-strip -g <目标文件>

# 5.6.2优化

为了让Clang编译器生成的BPF字节码能够通过验证器，需要使用-O2（2或者更高的级别）优化标志。如果不优化，Clang编译器会输出一些eBPF不支持的指令。例如，Clang编译器默认会用callx<register>指令来调用Helper函数，但是eBPF不支持从寄存器调用。

# 5.6.3 目标架构

如果你使用了libbpf定义的一些宏，你需要在编译时指定目标架构。libbpf的头文件 $bpf/bpf_tracing.h$ 定义了一些平台相关的宏，比如：我在这个例子中使用的BPF_KPROBE宏和BPF_KPROBE_SYSCALL宏。BPF_KPROBE宏可以用于将eBPF程序附加到kprobes上，而BPF_KPROBE_SYSCALL宏是BPF_KPROBE一个变体，专门用于系统调用的kprobe上。

对于kprobe,参数是一个ptRegs结构体,它保存了CPU寄存器内容的副本。由于寄存器是架构相关的,ptRegs结构体的定义取决于程序所运行的架构。这意味着,如果你想使用这些宏,你还需要告诉编译器目标架构是什么。你可以通过设置-D_TARGET_ARCH($ARCH)来做到这一点,其中$ARCH是一个架构名称,比如arm64,amd64等。

另外注意，如果你不使用这些宏，你需要考虑编写架构相关的代码访问kprobe的寄存器信息。

这里也许“一次编译，多架构运行”会更贴切一些！

# 5.6.4 Makefile

下面是一个用于编译CO-RE目标文件的Makefile指令的例子（来自本书的GitHub仓库的chapter5目录中的Makefile）：

```makefile
hello-buffer-config.bpf.o:%.o:%.c
clang \
-target bpf \
-D __TARGET_ARCHIVE $(ARCH) \
-I/usr/include/$(shell uname -m)-linux-gnu \
-Wall \
-02 -g \
-c $<-o $@
llvm-strip -g @$ 
```

示例代码通过在 chapter5 文件夹下运行 make 命令来构建 eBPF 目标文件 hello-buffer-config.bpf.o（稍后会介绍程序配套的用户空间可执行文件）。接下来，让我们查看一下这个目标文件，看看它是否包含了 BTF 信息。

# 5.6.5 目标文件中的BTF信息

内核的BTF文档（https://oreil.ly/5QrBy）描述了在ELF目标文件中BTF数据是如何编码的，分为两个部分：.BTF，包含了数据和字符串信息，.BTF.ext，包含了函数和行信息。你可以用readelf工具来查看这些部分是否已经添加到目标文件中，像这样：

```powershell
$ readelf -S hello-buffer-config.bpf.o | grep BTF
[10] .BTF PROBITS 0000000000000000 000002c0
[11] .rel.BTF REL 0000000000000000 00000e50
[12] .BTF.ext PROBITS 0000000000000000 00000b18
[13] .rel.BTF.ext REL 0000000000000000 00000ea0 
```

bpftool工具可以从目标文件中查看BTF数据，像这样：

```txt
bpftool btf dump file hello-buffer-config.bpf.o 
```

输出的内容和你之前在本章中看到的从加载的程序和Map中导出BTF信息的内容一样。

让我们看看这些BTF信息是如何让程序能够在另一台机器上运行的，而这台机器有不同的内核版本和不同的数据结构。

# 5.7 BPF 重定位

libbpf库可以让eBPF程序适应目标内核上的数据结构，即使这个结构和代码编译的内核不一样。为了做到这一点，libbpf库需要Clang编译器在编译过程中生成的BPF的CO-RE重定位信息。

你可以从linux/bpf.h头文件中的structbpf_core_relo定义中了解更多关于重定位是如何工作的：

```txt
struct bpf_core_relo {
    __u32 insn_off;
    __u32 type_id;
    __u32 access_str_off;
    enum bpf_core_relo-kind kind;
}; 
```

每个需要重定位的指令都会对应一个这样的结构体，eBPF程序的CO-RE重定位数据由多个这样的结构体组成。假设某指令是将一个寄存器设置为某个结构体内的一个字段的值，那么在该指令对应的bpf_core_relo结构体中，insn_off字段标识该指令的位置，type_id字段为结构体的BTF类型，access_str_off字段指示了相对于该结构体应该如何访问该字段。

你刚刚看到，内核数据结构的重定位数据是由Clang编译器自动生成，并编码在ELF目标文件中。下面这一行，你会在vmlinux.h文件的开头附近找到它。这行会让Clang编译器自动生成重定位数据：

```lisp
#pragma clang attribute push (_attribute_((preserve_access_index)), \ apply_to = record) 
```

preserve_access_index 属性告诉 Clang 编译器为某个类型定义生成 BPF CO-RE 重定位。在 vmlinux.h 中，clang attribute push 定义在文件的开头，clang attribute pop 定义在文件末尾，这表示该重定位属性应该作用于该文件中定义的所有类型上，意味着 Clang 编译器为 vmlinux.h 中定义的所有类型生成重定位信息。

通过使用 bpftool 并用 -d 标志打开调试信息，你可以在加载 BPF 程序时，查看生成的重定位信息，像这样：

```txt
bpftool -d prog load hello.bpf.o /sys/fs/bpf/hello 
```

这会产生很多输出，但是和重定位相关的部分看起来像这样：

```txt
libbpf: CO-RE relocating [24] struct user_ctRegs: found target candidate [205]  
struct user_ctRegs in [vminux]  
libbpf: prog 'hello': relo #0: <byte_off> [24] struct user_ctRegs.regs[0]  
(0:0:0 @ offset 0)  
libbpf: prog 'hello': relo #0: matching candidate #0 <byte_off> [205] struct  
user_ctRegs.regs[0] (0:0:0 @ offset 0)  
libbpf: prog 'hello': relo #0: patched insn #1 (LDX/ST/STX) off 0 -> 0 
```

在这个例子中，你可以看到hello程序的BTF信息中类型ID为24，是一个叫user_mtRegs的结构体。libbpf库已经把它和一个内核的同名结构体user_mtRegs匹配起来了，它在vmlinux的BTF数据集中类型ID为205。实际上，因为我在同一台机器上编译和加载程序，所以类型定义是相同的。在这个例子中，结构体开始的偏移量0保持不变，对指令#1的“修补”也没有改变它。

在很多应用中，你不会想让用户运行 bpftool 工具来加载一个 eBPF 程序。相反，你会想把这个功能构建到一个专用的用户空间程序中，并作为一个可执行文件提供给使用者。让我们考虑一下如何编写这个用户空间代码。

# 5.8 支持 CO-RE 的用户空间代码

除了 bpftool 工具外，其他编程语言框架也提供了对 CO-RE 的支持，它们也是在加载 eBPF 程序到内核时实现了重定位。在这一章中，我们将展示如何使用 libbpf 库的 C 代码编写支持 CO-RE 的用户空间代码。我们还将在第 10 章进一步讨论其他语言，包括 Go 语言的 cilibrium/ebpf 和 libbpfgo 包，以及 Rust 语言的 Aya。

# 5.9 用户空间代码的libbpf库

如果你用C语言编写eBPF程序的用户空间部分，libbpf库是一个很好的选择。它是一个用户空间的库，你可以直接使用它。如果你愿意，你甚至可以选择不使用CO-RE。Andrii Nakryiko在他的优秀博客文章“libbpf-booxtrap”（https://oreil.ly/b3v7B）中提供了一个这样的例子。

这个库提供了一些函数，它们封装了第4章介绍的与bpf()相关的系统调用，用于执行一些操作，比如将程序加载到内核并附加到某个事件上，或者从用户空间代码访问Map信息。使用这些抽象的最简单方式是使用自动生成BPF骨架代码。

# 5.9.1 BPF骨架

你可以使用 bpftool 工具从已有的 ELF 格式的 eBPF 对象中自动生成这个骨架代码，像这样：

```txt
bpftool gen skeleton hello-buffer-config.bpf.o > hello-buffer-config.skel.h 
```

查看这个骨架的头文件，你会发现它包含了eBPF程序和Map的结构体定义，以及一些以hello_buffer_config_bpf_开头的函数（以对象文件名开头的）。这些函数管理了eBPF程序和Map的生命周期。虽然你不一定要使用骨架代码，如果你喜欢你可以直接调用libbpf库，但是自动生成的代码通常会节省你输入的时间。

在生成的骨架文件末尾，你会看到一个叫hello_buffer_config_bpf__elf_bytes函数，它返回ELF对象文件hello-buffer-config.bpf.o的字节码内容。一旦骨架生成了，我们就不需要那个对象文件了。你可以通过运行make命令来生成hello-buffer-config可执行文件，然后通过删除.o文件来测试；该可执行文件包含了eBPF字节码。

![](images/a13175b8caa4209614b6fdf51d0659b7546833be0c3d350ee1a08c7779373e6a.jpg)

如果你愿意，你可以使用libbpf库的bpf_object_open_file函数来从一个ELF文件中加载eBPF程序和map，而不必使用骨架文件中的字节码。

下面是一个用户空间代码示例的摘要，用于管理eBPF程序和map的生命周期，它使用了自动生成的骨架代码。为了清晰起见，我省略了一些细节和错误处理，你可以在chapter5/hello-buffer-config.c中找到完整的源代码。

... [other #includes]   
#include "hello-buffer-config.h" 1   
#include "hello-buffer-config.skel.h"   
... [some callback functions]   
int main()   
{ struct hello_buffer_config_bpf \*skel; struct perf_buffer \*pb $=$ NULL; int err; libbpf_set_print(Libbpf_print_fn); 2 ske1 $=$ hello_buffer_config_bpf_open_and_load(); 3 err $=$ hello_buffer_config_bpf.attach(skel); 4 pb $=$ perf_buffer_new(bpf_map_fd(skel->maps.output),8,handle_event, lost_event，NULL，NULL); 5 while(true){ 6 err $=$ perf_buffer POLL(pb，100); } perf_buffer_free(pb); 7 hello_buffer_config_bpfdestroy(skel); return -err;

$\bullet$ 这个文件包含了自动生成的骨架代码的头文件，以及手动编写的，用于用户空间和内核代码之间共享的数据结构的头文件。  
② 这段代码设置了一个回调函数，用于打印 libbpf 库生成的日志消息。  
③ 这里创建了一个skel结构体，表示了ELF字节中定义的所有Map和程序，并把它们加载到内核中。  
程序自动附加到相应的事件上。  
⑤ 这个函数创建了一个结构体用于处理 output perf 缓冲区。  
⑥ 这里持续地轮询perf缓冲区。

这是清理代码。

让我们更详细地探讨其中的一些步骤。

# 把程序和Map加载到内核中

在上面的步骤中，第一个调用自动生成的函数是：

```txt
skel = hello_buffer_config_bpf_open_and_load(); 
```

从名字可以看出，这个函数包含了两个阶段：打开和加载。打开阶段涉及读取 ELF 数据，并部分转换成代表 eBPF 程序和 Map 的结构体。加载阶段把这些 Map 和程序加载到内核中，并根据需要执行 CO-RE 修正。

这两个阶段可以很容易地分开处理，骨架代码提供了单独的name_open()和name_load()函数。你可以在加载之前增加操作，通常是在加载程序之前对程序进行预设置。例如，我可以把一个全局变量c初始化为某个值，像这样：

skel $=$ hello_buffer_config_bpf_open(); if(!skel){ //Error...   
}   
skel->data->c $= 10$ .   
err $=$ hello_buffer_config_bpf_load(skel);

hello_buffer_config_bpf_open()和hello_buffer_config_bpf_load()返回一个叫hello_buffer_config_bpf结构体，它在骨架代码的头文件中定义，包含了对象文件中定义的所有map、程序和数据的信息。

![](images/9c14bd83926e2052447b915517db24d41da81e2ee4e6902a89ed6a69f48a4427.jpg)

骨架对象（在这个例子中是hello_buffer_config_bpf）只是ELF字节信息的用户空间表示。一旦程序被加载到内核中，如果你改变对象中一个值，它不会对内核端的数据产生任何影响，比如，在加载之后改变skel->data->c值是没有效果的。

# 访问已存在的Map

默认情况下，libbpf库可以创建在ELF字节中定义的Map，但有时你可能想要编写一个eBPF程序，重用一个已有的Map。在上一章中你已经看到了一个这样的例子，例如：使用bpftool工具遍历所有的Map，及寻找一个与指定名称匹配的Map；另一个常见的原因是在两个不同的eBPF程序之间共享信息，这种情况下只有一个程序允许创建Map，可以使用bpf_map_set_autocreate()函数设置libbpf库禁用自动创建Map。

如果你知道已存在的Map的固定路径，可以用bpf_obj_get()函数来获取一个该Map的文件描述符。这里有一个非常简单的例子（该示例可以在GitHub仓库的chapter5/find-map.c中找到）：

```c
struct bpf_map_info info = {{};
unsigned int len = sizeof(info);
int findme = bpf_obj_get("/sys/fs/bpf/findme");
if (findme <= 0) {
    printf("No FD\n");
} else {
    bpf_obj_get_info_by_fd(findme, &info, &len);
    printf("Name: %s\n", info.name);
} 
```

实验上面这个简单示例，首先，你可以用 bpftool 工具来创建一个 Map，像这样：

```txt
$ bpftool map create /sys/fs/bpf/findme type array key 4 value 32 entries 4 name findme 
```

运行find-map可执行文件会打印出如下信息：

```txt
Name: findme 
```

让我们回到hello-buffer-config示例和骨架代码。

# 附加到事件上

在hello-buffer-config示例中的下一个骨架函数是把程序附加到execve系统调用函数上：

```javascript
err = hello_buffer_config_bpf Attached(skel); 
```

Libbpf库会自动从程序的SEC()定义中获取附加点。如果你没有定义附加点，libbpf库提供一系列函数，例如：bpf(program__attach_kprobe、bpf(program__attach_xdp等，用于附加不同类型的程序。

# 管理perf事件缓冲区

使用下面的函数设置perf缓冲区，这个函数是libbpf库定义的，不是骨架代码中自动生成的函数：

```txt
pb = perf_buffer_new(bpf_map_fd(skel->maps.output), 8, handle_event, lost_event, NULL, NULL); 
```

你可以看到perf_buffer_new()函数把outputMap的文件描述符作为第一个参数。handle_event参数是一个回调函数，当perf缓冲区中有新的数据到达时，该函数会被调用，而lost_event参数也是一个回调函数，是当perf缓冲区中没有足够的空间让内核写入一个数据条目时，该函数会被调用。在这个例子中，这些回调函数只是把消息打印到屏幕上。

最后，程序必须反复轮询perf缓冲区：

while(true）{ err $=$ perf_buffer__poll(pb，100);

100是超时时间，单位毫秒。当数据到达或缓冲区满时，之前设置的回调函数会被调用。

最后是清理会释放perf缓冲区，并销毁了内核中的eBPF程序和Map，像这样：

```c
perf_buffer_free(pb);  
hello_buffer_config_bpfdestroy(skel); 
```

在libbpf库中有一整套perf_buffer_和ring_buffer_相关的函数，可以帮助你管理perf事件缓冲区。

如果你编译并运行这个hello-buffer-config程序的例子，你会看到以下输出，这与你在第4章中看到的非常相似：

```txt
23664 501 bash Hello World   
23665 501 bash Hello World   
23667 0 cron Hello World   
23668 0 sh Hello World 
```

# 5.9.2 Libbpf 代码示例

下面有很多基于libbpf的eBPF程序的优秀示例，你可以借鉴它们来获得编写自己程序的灵感和指导：

- Libbpf-bootstrap 项目（https://oreil.ly/zB0Co），提供一组示例程序帮助你快速入门。  
- BCC 项目中有很多原来基于 BCC 的工具，现已迁移到 libbpf 版本。你可以在 libbpf-tools 目录（https://oreil.ly/Z9xDX）中找到它们。

# 5.10总结

CO-RE使eBPF程序能够在与构建时不同版本的内核上运行，这显著增强了eBPF的可移植性，极大地便利了那些希望向用户和客户提供成熟工具的开发者。

在本章中，你看到了CO-RE是如何通过将类型信息编码到编译后的对象文

件中，并在它们被加载到内核时，使用重定位来重写指令来实现跨平台。同时，你也对使用libbpf编写的C代码有了一定的了解，包括：基于自动生成的BPF骨架代码，管理运行在内核侧和用户空间的eBPF程序的生命周期。在下一章中，你将学习内核是如何验证eBPF程序是否可以安全运行的。

# 5.11 练习

你可以通过以下方式进一步探索BTF、CO-RE和libbpf：

1. 使用 bpftool btf dump map 和 bpftool btf dump prog 来分别查看与 Map 和程序相关的 BTF 信息。并且你可以用多种方式来指定单个 Map 和程序。

2. 比较 bpftool btf dump file 和 bpftool btf dump prog 的输出。对于同一个程序，它在 ELF 对象文件的形式和在被加载到内核之后的形式应该是相同的。

3. 查看 bpftool -d prog load hello-bufferconfig.bpf.o /sys/fs/bpf/hello 输出的调试信息。你会看到加载的 Section、许可证检查、重定位过程，以及每个 BPF 程序指令的描述。

4. 尝试使用与BTFHub不同的vmlinux头文件来构建一个BPF程序，并在bpftool输出的调试信息中寻找改变偏移量的重定位。

5. 修改hello-buffer-config.c程序，使用Map来为不同的用户ID配置不同的消息（类似第4章中的hello-buffer-config.py示例）。

6. 尝试改变SEC()中Section名，也许是你自己的名字。当你来加载程序到内核时，你应该会看到一个错误，因为libbpf库不认识这个Section名，这表明了libbpf库是使用Section名来确定BPF程序的类型。你可以尝试编写你自己的附加功能代码，来显式地将程序附加到你选择的事件上，而不是依赖libbpf库的自动附加功能。

# eBPF 验证器

前文中多次提及验证器的验证步骤，当eBPF程序加载到内核时，验证器会确保程序的安全性。在本章中，我们将深入探讨eBPF验证器的工作原理。

验证器会检查程序的每一种可能的执行路径，并确保每一条指令都是安全的。验证器还会更新字节码，为程序运行做好准备。在这一章中，我们从一个可以正常运行的例子开始，将代码人为修改为无效代码，查看当运行无效代码时验证器的验证效果。

![](images/d596b8289162be9b061b254327bfaabf2c4e3d6c647d898e9d477fd3538c9fab.jpg)

这一章的示例代码在 github.com/lizrice/learning-ebpf 的 chapter6 目录中。

这一章并不会介绍验证器的每一种检查，本章目的是提供一个概览，用一些说明性的例子来帮助读者处理在编写eBPF代码时可能遇到的验证错误。

需要记住的是，验证器是基于eBPF字节码工作的，而不是直接在源代码上。字节码取决于编译器的输出。由于编译器在编译过程中会进行一些优化，这些优化并不总是与你的期望一致，所以相应地，验证器的验证结果也可能与你期望的结果不同。例如，对于不可达的指令，验证器会拒绝，但是编译器可能在验证器验证之前已经把它们优化掉了。

# 6.1 验证过程

验证器对程序进行分析，并评估所有可能的运行路径。验证器仅按顺序对命令逐条地评估，并不会真正地执行。在验证的过程中，用一个bpf_reg_state结构体来跟踪每个寄存器的状态（这里的寄存器是第3章中提到的eBPF虚拟机的寄存器）。这个结构体包含了一个bpf_reg_type字段，它描述了寄存器中保存的值的类型。下面列举几种可能的类型如：

- NOT_INIT，表示寄存器还没有被赋值。  
- SCALAR_VALUE，表示寄存器被设置为一个非指针的值。  
- PTR_TO_*类型，表示寄存器保存了一个指针。PTR_TO_*类型又分几种，下面是几个示例：

- PTR_TOCTX：说明保存了一个传递BPF程序上下文参数的指针。  
—PTR_TO(PacketET：说明保存一个指向网络包的指针（在内核中保存为skb->data）。  
一PTR_TO_MAP_KEY或PTR_TO_MAP_VALUE：寄存器保存了指向MAP的KEY和VALUE的指针。

还有一些其他的 PTR_TO_* 类型，读者有兴趣可以查阅 linux/bpf.h 头文件（https://oreil.ly/aWb50），那里能找到所有的类型。

bpf_reg_state结构体还会持续跟踪保存在寄存器中值的范围，这也是验证器判断无效操作的一种依据。

当验证器遇到分支，就需要决定是继续顺序执行还是跳转到另一条指令，此时验证器会把寄存器当前的所有状态的一个副本压入栈中，并探索其中一条路径，进而继续评估指令，直到到达程序末尾的返回（或者到达能处理的指令的数量的限制，目前指令数量的限制是一百万条指令注1），然后会从栈中

弹出下一个分支进行评估，这个过程中发现了可能导致操作无效的指令，就会判定为验证失败。

对程序执行的每一种可能性进行验证会非常耗费计算资源，所以在实践中采用了一种“状态剪枝”的优化方法，避免对程序中本质上相同的路径进行重复验证。在程序运行过程中，验证器会记录某些指令相关所有寄存器的状态，如果后来遇到了同样的指令，而且寄存器的状态也相同，那么就不再继续验证后面的路径，实践中已经证明这种优化是有效的。

验证器（https://oreil.ly/pQDES）和“状态剪枝”都经过了大量的优化。验证器以前会在每个跳转指令的前和后都记录状态剪枝信息，但是分析发现平均每四条指令左右就要记录一次状态，而且绝大多数的状态剪枝永远不会被用到。调优后，无论是否有分支，每十条指令存储一次剪枝状态更加高效。

![](images/d1fa84eb4d3575d879bb6b5bf3b4b46325346a3bf32c4a142570b257f7ceccad.jpg)

读者可以在内核文档（https://oreil.ly/atNda）中了解关于验证工作的更多细节。

# 6.2 验证器日志

当程序验证失败时，验证器会生成一个日志，把失败的原因写在日志上。如果读者使用 bpftool prog load，验证器日志会输出到 stderr。当读者使用 libbpf 库写程序时，可以使用 libbpf_set_print() 函数来设置一个处理器，记录（或者其他有用的事件）可能出现的任何错误（本章的 hello-identifier.c 源代码中有一个例子）。

![](images/b8673260c451eb57a1307682adce8468e9de804f0a2c2ea97ecf2794eef7e04e.jpg)

如果读者想深入了解验证器的工作细节，可以把成功和失败的信息都生成日志。在hello-identifier.c中有一个例子，把验证器的日志保存在传入的一个缓冲区中，然后把这个日志的内容输出到屏幕上。

日志中包含了验证器对程序执行细节的总结，像这样：

```txt
processed 61 insns (limit 1000000) max_states_per_insn 0 total_states 4 peak_states 4 mark_read 3 
```

日志中显示，验证器处理了61条指令，其中有的指令可能被不同的分支多次处理。注意，一百万的限制是程序代码中静态指令数量的上限。实际上，程序运行中如果代码中有分支，同一条指令验证器会被多次处理。

对于这个示例程序，total_states状态总数是4个，和peak_states的数量相同。如果一些状态被剪枝了，peak_states的数量会小于total_states。

输出日志中不但有验证器已分析的BPF指令，也有相应的C源代码及验证器状态信息（如果程序编译时使用了-g标志，日志中还会包含调试信息）。下面是一个验证器日志的片段，与hello-identifier.bpf.c程序中的前几行相关：

0: (bf) $r6 = r1$ ；data.counterc $\equiv$ c; 1:(18） $r1 = 0x\text{f}$ fff800008178000 3：(61） $r2 = *(u32*)$ (r1+0) R1_w=map_value(id=0,off=0,ks=4,vs=16,imm=0)R6_w=ctx(id=0,off=0,imm=0) R10=fp0 2 ;c++; 4: (bf) $r3 = r2$ 5:(07） $r3+=1$ 6：(63)*(u32*)(r1+0)=r3 R1_w=map_value(id=0,off=0,ks=4,vs=16,imm=0)R2_w=inv(id=1,umax_ value=4294967295, var_off=(0x0;oxFFFFFF))R3_w=inv(id=0,umin_value=1,umax_value=4294967296, var_off=(0x0;oxFFFFFF))R6_w=ctx(id=0,off=0,imm=0)R10=fp0 3

$\bullet$ 因为在编译时用 $-\mathrm{g}$ 来构建调试信息，所以日志中包含了源代码，以便理解输出的验证器日志和源代码的关系。  
这行输出的是一个寄存器状态信息：在这个阶段，寄存器1包含了一个map，寄存器6保存了上下文，寄存器10是帧（或栈）指针，用来保存局部变量。  
这行是另一个寄存器状态信息。不仅可以看到每个（初始化的）寄存器中保存的值的类型，还可以看到寄存器2和寄存器3的取值范围。

让我们来进一步探讨下，寄存器6保存了上下文，日志中用R6_w=ctx(id=0,off=0,imm=0)来表示。而第一行显示寄存器1的内容被复制到寄存器6。一般是用寄存器1来保存传递给程序的上下文参数，为什么要复制到寄存器6呢？原因是，当调用一个BPF Helper函数时，参数是保存在寄存器1~5中。Helper函数不会修改寄存器6~9的内容，所以把上下文保存到寄存器6意味着代码可以调用一个Helper函数，并且还能访问上下文信息。

寄存器0用于保存Helper函数和eBPF程序的返回值。寄存器10保存了一个指向eBPF栈帧的指针（eBPF程序不能修改它）。让我们看看在指令6之后寄存器2~3的状态信息：

```txt
R2_w=inv(id=1,umax_value=4294967295,var_off=(0x0;0xFFFFFF))  
R3_w=inv(id=0,umin_value=1,umax_value=4294967296,var_off=(0x0;01FFFFFF)) 
```

寄存器2没有umin_value，umax_value的十进制值相当于十六进制0xFFFFFFF，这是一个8字节寄存器能保存的最大值，也就是说，寄存器2可以保存任何可能值。

在指令4中，寄存器2的内容被复制到寄存器3，然后指令5给这个值加了1。因此，寄存器3可以是任何不小于1的值。寄存器3的状态信息中显示，umin_value被设置为1，而umax_value为0xFFFFFFF也说明了这一点。

验证器不仅使用每个寄存器的状态信息，还使用每个寄存器可以包含的值的范围，来确定程序的可能路径。这也用于我之前提到的“状态剪枝”：如果验证器发现不同分支在代码的同一位置时，每个寄存器的类型和可能的值的范围也相同，那么这条路径就会被忽略。而且，如果当前状态是之前状态的子集，也可以被忽略。

# 6.3 可视化控制流

因为验证器会探索eBPF程序的所有可能路径，如果读者在调试中遇到问题

时，这些路径可能会有帮助。bpftool工具可以以DOT格式（https://oreil.ly/V-1WN）生成程序的控制流图，还可以转换为图片，像这样：

$ bpftool prog dump xlated name kprobe_exec visual > out.dot

$ dot -Tpng out.dot > out.png

这会生成一个如图6-1所示的控制流图。

![](images/36fe40da2139ca1d71078e2573d20bb0a8000e13976d5908fc280a758c0c5629.jpg)  
图6-1：控制流图的一部分（完整的图像见本书的GitHub仓库中chapter6/kprobe_exec.png）

# 6.4 验证helper函数

eBPF程序不允许直接调用任何内核函数（除非被注册为kfunc，这部分在下一章会讲到），但eBPF提供了一些Helper函数，使程序能够访问内核的信息。有一个bpf-helpers手册（https://oreil.ly/pdLGW)记录了Helper函数的所有信息。

不同的Helper函数仅对特定的BPF程序类型有效。例如，Helper函数bpf_get_current pid_tgid()用于检索当前的用户空间进程ID和线程ID，对于从一个由网络接口接收到的数据包触发的XDP程序而言，调用它是没有意义的，因为该程序没有涉及用户空间进程。如果想构建xdp程序的例子，读者可以将hello-identifier.bpf.c中的hello eBPF程序的SEC()的定义从“kprobe”改为“xdp”，再尝试加载这个程序时，验证器输出以下信息：

```txt
16: (85) call bpf_get_current pid_tgid#14  
unknown func bpf_get_current pid_tgid#14 
```

unknown func并不是说不知道这个函数，只是说BPF程序类型是未知的。（BPF程序类型是下一章的话题；现在只需要理解成不同的BPF程序类型可以附加到不同类型的事件上。）

# 6.5 Helper函数参数

如果读者看了 kernel/bpf/helpers.c(https://oreil.ly/tjjVR)注2，就会发现每个Helper函数都有一个bpf_func.proto结构体，如下面Helper函数bpf_mapLOOKUP_element()所示：

```lua
const struct bpf_func.proto bpf_mapLOOKUP_element.proto = {
    .func = bpf_mapLOOKUP_element,
    .gpl_only = false,
    .pkt_access = true,
} 
```

```c
RET_type = RET_PTR_TO_MAP_VALUE_OR_NULL,
arg1_type = ARGCONST_MAP_PTR,
arg2_type = ARG_ptr_TO_MAP_KEY,
}; 
```

这个结构体定义了Helper函数的参数和返回值的类型。因为验证器通过追踪每个寄存器中保存的值的类型，验证是否传递了错误的参数给Helper函数。比如，可以做个实验，改变hello程序中调用bpf_map.lookup Elem()时传入的参数，像这样：

```txt
p = bpf_mapLOOKUP_element(&data, &uid); 
```

把原来指向 map 的指针 &my_config，改成一个指向本地变量结构的指针 &data。对于编译器来说没有问题，可以正常构建 BPF 对象文件 hello-identifier.bpf.o，但是当程序试图加载到内核时，验证器日志中就会看到这样的错误：

```txt
27: (85) call bpf_mapLOOKUP_element#1  
R1 type=fp expected=map_ptr 
```

这里fp代表帧指针（Frame pointer），指向栈上调用帧局部变量的地址。寄存器1加载了局部变量data的地址，但是函数期望一个指向map的指针（如之前看到的bpf_func.proto结构体中的arg1_type字段所示）。验证器会跟踪每个寄存器中存储的值的类型，从而能够发现值类型与函数参数不一致。

# 6.6 检查许可证

验证器还会检查许可证，如果程序使用了一个GPL许可的BPFHelper函数，那么程序也必须有一个与GPL兼容的许可证。第6章示例代码hello-identifier.bpf.c的最后一行定义了SEC("license")为“DualBSD/GPL”。如果删除这一行，验证器的输出如下所示：

```txt
37: (85) call bpfprobe_read_kernel#113 cannot call GPL-restricted function from non-GPL compatible program 
```

这是因为Helper函数bpfprobe_read_kernel()是GPL许可的函数，它将gpl_only字段被设置为true。这个eBPF程序中，也调用了非GPL许可的其他Helper函数，这对验证器是没有影响的。

BCC项目中维护了一个列表，其中说明Helper函数的许可是否是GPL(https://oreil.ly/mCpvB)。如果读者对Helper函数的实现细节感兴趣，可以阅读BPF和XDP参考指南中(https://oreil.ly/kVd6j)的相关章节。

# 6.7内存访问检查

验证器会执行检查以确保BPF程序只访问应该访问的内存。

例如，当处理网络包时，一个XDP程序只允许访问保存网络包的内存位置。大多数XDP程序都以类似的内容开始，像下面这样：

```c
SEC("xdp")  
int xdp_load balancer(struct xdp_md *ctx)  
{  
    void *data = (void*)(long)ctx->data;  
    void *data_end = (void*)(long)ctx->data_end; 
```

xdp_md结构体描述了接收到的网络包，作为上下文传递给程序。该结构体中的ctx->data字段是网络包开始的内存位置，ctx->data_end是网络包的结束的位置。验证器会确保程序内存访问不超过这个范围。

例如，以下helloVerifier.bpf.c程序是正确的：

```c
SEC("xdp")  
int xdp_hello(struct xdp_md *ctx) {  
    void *data = (void*)(long)ctx->data;  
    void *data_end = (void*)(long)ctx->data_end;  
    bpf-printk("%x", data_end);  
    return XDP_PASS;  
} 
```

变量 data 和 data_end 很像，但验证器非常聪明，能够识别出 data_end 与网络包的末端相关。程序读取网络包的值时会被检查，确保不会从超出该位置的地方读取，也不允许通过修改 data_end 值的方式来“作弊”。可以做个试验，在 bpf_PRINT() 调用之前添加下面这行：

```txt
data_end++; 
```

验证器会验证不通过，并提示如下信息：

```txt
data_end++; 
```

```txt
1:（07）r3+=1 
```

```txt
R3 pointer arithmetic on pkt_end prohibited 
```

另一个例子展示了当访问一个数组时，需要确保不能访问超出该数组索引的范围。在下面的示例代码要从 message 数组中读取一个字符，像这样：

```txt
if (c < sizeof(message)) {  
    char a = message[c];  
    bpf_printk("%c", a);  
} 
```

上面是正确的写法，变量c可以明确的检查并确保计数器不会超过message数组的大小。但是像下面这样一个简单的“越界”错误就会导致验证不通过：

```lisp
if (c <= sizeof(message)) {  
    char a = message[c];  
    bpf_printk("%c", a);  
} 
```

验证器的验证失败信息如下：

```txt
invalid access to map value, value_size=16 off=16 size=1 R2 max value is outside of the allowed memory range 
```

信息中清晰地说明失败原因是对 map 值的无效访问，因为寄存器 2 可能持有一个超过 map 索引大小的值。如果读者要调试这些错误，需要深入分析日志，

定位出哪一行源代码导致的错误。日志在错误信息之前一般像下面这样结尾（为了更好理解，我删除了一些寄存器状态信息）：

```ini
; if (c <= sizeof(message)) {  
30: (25) if r1 > 0xc goto pc+10  
R0_w = map_value_or_null(id=2, off=0, ks=4, vs=12, imm=0) R1_w = inv(id=0, umax_value=12, var_off=(0x0; 0xf)) R6 = ctx(id=0, off=0, imm=0) ...  
; char a = message[c];  
31: (18) r2 = 0xffff800008e00004  
33: (of) r2 += r1  
lastidx 33 first_idx 19  
regs = 2 stack = 0 before 31: (18) r2 = 0xffff800008e00004  
regs = 2 stack = 0 before 30: (25) if r1 > 0xc goto pc+10  
regs = 2 stack = 0 before 29: (61) r1 = *(u32*) (r8 + 0)  
34: (71) r3 = *(u8 *) (r2 + 0)  
R0_w = map_value_or_null(id=2, off=0, ks=4, vs=12, imm=0) R1_w = invP(id=0, umax_value=12, var_off=(0x0; 0xf)) R2_w = map_value(id=0, off=4, ks=4, vs=16, umax_value=12, var_off=(0x0; 0xf), s32_max_value=15, u32_max_value=15) R6 = ctx(id=0, off=0, imm=0) ... 
```

① 从下往上看错误信息，最后的寄存器状态信息显示寄存器2的最大值可能是12。  
② 再往上看指令31，寄存器2被设置为内存中的一个地址，然后增加了寄存器1的值。输出中显示这对应于访问message[c]的代码行，所以可以推断寄存器2被设置为指向message数组，然后增加了寄存器1中c的值。  
③ 进一步回溯寻找寄存器1的值，日志显示它有一个最大值为12（十六进制为0x0c）。然而，message被定义为一个长度12的字符数组，所以只有索引0~11是在它的范围内。从这里可以看到错误是由源代码 $c \leqslant$ sizeof(message) 引起的。

在步骤2，验证器日志中包含的源代码，有助于我们推断出寄存器和对应源代码变量之间的关系。如果代码是在没有调试信息的情况下编译的，读者需要回溯验证器日志来检查正确性。上述例子表明，调试信息的存在是有意义的。

message 数组被声明为一个全局变量，第 3 章有提到全局变量是使用 map 来实现的，这也是错误信息中提示“invalid access to a map value”的原因。

# 6.8 指针解引用之前进行检查

C程序崩溃的一个很简单方法是对空值（也称为null）的指针进行解引用。指针是指向内存中值的位置，而空值并不是一个有效的内存位置。eBPF验证器要求所有的指针在解引用之前进行检查，以避免这类崩溃发生。

hello-identifier.bpf.c示例代码中有一个名为my_config的hash类型的map，它存储着用户自定义消息，如下行所示：

```txt
p = bpf_mapLOOKup_element(&my_config, &uid); 
```

如果这个 map 中没有对应于 uid 的项，将把 p（它是一个指向消息结构 msg_t 的指针）设置为空。下面的代码，试图释放这个可能为空的指针：

```txt
char a = p->message[0];  
bpf_printk("%c", a); 
```

虽然编译没有问题，但验证器验证不通过，如下所示：

```txt
; p = bpf_mapLOOKup_element(&my_config, &uid);  
25: (18) r1 = 0xffff263ec2fe5000  
27: (85) call bpf_mapLOOKup_element#1  
28: (bf) r7 = r0  
; char a = p->message[0];  
29: (71) r3 = *(u8*)(r7 +0)  
R7 invalid mem access 'map_value_or_null' 
```

$\bullet$ Helper函数返回p的值存储在寄存器0中。返回值赋值到寄存器7中，也就是说寄存器7现在存储了局部变量p的值。  
$\bullet$ 这条指令试图对寄存器7的指针p进行引用。验证器一直在跟踪寄存器7的状态，知道该指针持有一个指向Map的值，可能是空的。

验证器会拒绝这种引用空指针的操作，但是如果像下面这样有一个明确的检查，就可以通过：

if $(p! = 0)$ {char a $=$ p->message[0];

```txt
bpf_printk("%d", cc); } 
```

有些Helper函数包含了指针检查。例如，在bpf-helpers的手册中，bpfprobe_read_kernel()的函数签名如下：

```c
long bpfprobe_read_kernel(void *dst, u32 size, const void *unsafe_ptr) 
```

在这个BPFHelper函数的例子中，Helper函数会检查指针是否为空，从而帮助程序员编写安全的代码。在这个函数中，允许调用者传递一个值为空的指针，但只能作为第三个参数unsafe_ptr。然后函数将在试图解引用指针之前检查是否指针是否为空。

# 6.9 访问上下文

每个eBPF程序都会把上下文信息作为一个参数传递，但是根据程序和附加类型的不同，可能只有允许访问其中的一部分上下文信息。例如，BPF的tracepoint程序（https://oreil.ly/6RFFI）可以接收一个指向某些跟踪点数据的指针。数据的具体格式由跟踪点所决定的，但通常以一些公共字段开头，eBPF程序不允许访问这些公共字段，只有在特定跟踪点之后的字段才可以被程序访问。如果试图读或写错误的字段，会导致“an invalid bpf_context access error”。在本章的练习中有一个这样的例子。

# 6.10 运行到完成

验证器确保eBPF程序会运行到结束，不会无限地消耗资源。这是通过限制处理的指令总数来做到，正如之前提到的，在写这篇文章的时候，这个上限被设置为一百万条指令。这个限制是硬编码到内核中的(https://oreil.ly/IucYm)，不是一个可配置的选项。如果验证器处理的指令总数到达上限，但BPF程序仍然没有到达结尾，那么验证器就会拒绝这个程序。

创建一个永远不会结束的程序很简单，比如写一个永远不会结束的循环。让

我们看看在eBPF程序中如何创建循环。

# 6.11 循环

在Linux内核5.3版本之前，为了确保eBPF程序能通过验证器的检查，内置了对循环的限制注3，不允许程序中的指令跳转回之前的某条指令来形成循环。开发者常用#pragma unroll指令让编译器展开循环，即每次迭代生成一组相同或类似的字节码。这样做节省了程序员重复输入相同代码的时间，但你会在生成的字节码中看到重复的指令。

从Linux 5.3版本开始，验证器在分析所有可能的执行路径时能够向前和向后跟踪分支，这样的话在不超过一百万条指令的限制下，某些形式的循环是可以接受的。

在 xdp_hello 示例程序中有一个循环的例子, 该循环的写法可以成功通过验证:

```txt
for (int i=0; i < 10; i++) { bpf_printk("Looping %d", i); } 
```

对于这个程序，验证器会验证通过，日志会显示已经循环地执行了10次。当然这里没有触及一百万条指令的数量限制。在本章的练习中，该循环程序有另一个版本会触及这个限制，并且也验证失败了。

在5.17版中，引入了一个新的Helper函数bpf_loop()，使用该函数，验证器验证循环会更容易、更有效。该Helper函数的第一个参数是最大迭代次数，同时传递每次迭代要调用的函数。无论迭代调用多少次，验证器只对该函数的BPF指令验证一次。同时，当该函数达到期望结果时，可以返回一个非零值来表示不需要再继续调用它，可以提前终止循环。

还有一个Helper函数bpf_for_each_map_element()（https://oreil.ly/Yg_oQ)，可为Map中的每个项提供调用的回调函数。

# 6.12 检查返回码

eBPF程序的返回码存储在寄存器0(R0)中。如果程序未对RO进行初始化，验证程序将失败，如下所示：

```txt
Ro !read.ok 
```

可以通过注释掉函数中的代码进行尝试；例如，将xdp_hello示例修改为如下所示：

```c
SEC("xdp")  
int xdp_hello(struct xdp_md *ctx) {  
    void *data = (void*)(long)ctx->data;  
    void *data_end = (void*)(long)ctx->data_end;  
    // bpf-printk("%x", data_end);  
    // return XDP_PASS;  
} 
```

这样修改的结果会导致验证失败。但是，如果把Helper函数bpf_printk()那行注释去掉，验证器不会有问题，即使源代码没有明确设置返回值！

这是因为寄存器0也用于保存Helper函数的返回码。从eBPF程序中的Helper函数返回后，寄存器0不再是未初始化的。

# 6.13 无效指令

正如第3章中对eBPF虚拟机的讨论所述，eBPF程序由一组字节码指令组成。验证器会检查字节码指令是否有效，例如，字节码指令只使用已知的操作码。

如果编译器发现了无效的字节码，编译将失败，除非你选择（出于某种原因）手工编写eBPF字节码，否则不会在验证器验证中看到这类错误。不过，最近

内核新增了一些指令，例如：原子操作，如果编译的字节码使用了这些指令，在旧内核上将无法通过验证。

# 6.14 无法访问的指令

验证器还会拒绝包含无法访问指令的程序。通常情况，这些指令也会被编译器优化掉。

# 6.15总结

当我刚开始对eBPF感兴趣时，感觉让代码通过验证器似乎是一门神秘的艺术，看似有效的代码会被拒之门外，出现一些不可预知的错误。随着时间的推移，验证器已经有了很多改进，在本章中的这些例子中，验证日志会给出提示，帮助你找出问题所在。

如果读者对eBPF虚拟机的工作原理有一定的了解，知道在执行eBPF程序时使用一组寄存器来存储临时值时，这些提示就会更有帮助。验证器会跟踪每个寄存器的类型和可能的值范围以确保eBPF程序安全运行。

如果读者尝试自己编写一些eBPF代码，在解决验证错误时需要帮助，可以到eBPF社区Slack频道（http://ebpf.io/slack）提问，很多人还在StackOverflow(https://oreil.ly/nu_0v)上找到了一些有价值的建议。

# 6.16 练习

以下一些练习可以导致验证器验证失败，你可以将其与验证器日志关联起来：

1. 在本章的“内存访问检查”一节中，你可以看到验证程序禁止访问全局message数组末尾以外的内容。在示例代码中，正确代码的是使用下面类似方式访问本地变量data.message：

```javascript
if (c < sizeof(data.message)) { 
```

```txt
char a = data.message[c];  
bpf_printk("%c", a); 
```

试着修改代码，将 $<$ 替换为 $<=$ ，可以看到同样的错误，提示：“invalid variable-offset read from stack R2”（寄存器R2读取无效变量-偏移量）。

2. 在示例的 xdp_hello 代码中找到已注释掉的循环，首先尝试添加如下这样的循环：

```txt
for (int i=0; i < 10; i++) { bpf_printk("Looping %d", i); } 
```

大家会在验证日志中会重复出现下面一系列的内容：

42: (18) $r_1 = 0$ xffff800008e10009  
44: (b7) $r_2 = 11$ 45: (b7) $r_3 = 8$ 46: (85) call bpf_trace_printk#6  
R0=inv(id=0) R1_w=map_value(id=0,off=9,ks=4,vs=26,imm=0) R2_w=inv11  
R3_w=inv8 R6=pkt_end(id=0,off=0,imm=0) R7=pkt(id=0,off=0,r=0,imm=0)  
R10=fp0  
lastidx 46 firstidx 42  
regs=4 stack=0 before 45: (b7) $r_3 = 8$ regs=4 stack=0 before 44: (b7) $r_2 = 11$

从日志中，找出循环变量i的寄存器。

3. 现在尝试在 xdp_hello 程序中添加一个会失败的循环，如下所示：

```c
for (int i=0; i < c; i++) {  
    bpf_printk("Looping %d", i);  
} 
```

你应该看到验证器试图将这个循环探索到结束，但它在完成之前达到了指令复杂性限制（因为全局变量c没有上限）。

4. 编写一个BPF程序，附加到tracepoint上（你可能已经在第四章的练习中完成了这个任务）。第七章会介绍tracepoint，你可以看到tracepoint的上下文参数结构体的定义，是以这些字段开头：

```txt
unsigned short common_type;  
unsigned char common_flags;  
unsigned char common_preempt_count;  
int common pid; 
```

创建一个以这样开头的结构体，并将程序中的上下文参数指向该结构体的指针。在程序中，尝试访问其中任意的一个字段，查看验证器是否会提示如下错误：“invalid bpf_context access”（无效的bpf_context访问）。

# 第7章

# eBPF 程序类型和附加点类型

在前几章中我们看到了很多eBPF程序的例子，你也许已经发现这些程序都与不同类型的事件相关联。有些例子是将程序附加在kprobes上，有些是处理网络数据包的XDP程序。这只是内核中众多附加点中的两个。本章我们将深入探讨不同的程序类型，以及如何将它们附加到不同的事件中。

![](images/ed5adf06d0e17c07a1dc233aafe127f5a6f24d8e456414d998c4501cf8d7adcf.jpg)

你可以使用 github.com/lizrice/learning-ebpf 上的代码和说明来构建并运行本章的示例。本章的代码位于 chapter7 目录中。

在撰写本书时，某些示例不支持ARM处理器。请查看chapter7目录中的README文件，了解更多详情和建议。

目前，内核头文件 uapi/linux/bpf.h(https://oreil.ly/6dNIW) 中列举了约 30 种程序类型和 40 多种附加点类型，其中附加点类型定义了程序可以附加到的具体位置。许多程序类型和附加点类型有对应关系。某些程序类型可以被附加到内核中的多个不同位置上，那么此时必须明确指定附加的附加点类型。

本书并不是一本参考手册，所以并不会把eBPF程序类型一一列举。并且在本书问世时，程序类型还会有新的类型。

# 7.1 程序上下文参数

所有的eBPF程序都需要一个指针类型的上下文参数，该参数指向的结构体取决于触发的事件类型。eBPF程序员需要根据该参数编写相应的程序。假如tracepoint事件，上下文参数就不是一个网络数据包类型。验证器可以根据程序类型，对上下文参数是否正确处理进行验证。并且验证器可以根据程序类型检查出哪些Helper函数是允许访问的。

![](images/e9c24fb7ce66c851de81a38659f00fc54f6aaa17d7e1293f3c7117336fc21927.jpg)

要深入了解不同BPF程序类型传递的上下文参数的详细信息，请查看AlanMaguire在Oracle博客上的文章（https://oreil.ly/6dNIW）。

# 7.2 Helper函数和返回码

第6章讲到验证器会检查Helper函数是否与程序类型兼容。就像第6章的示例中看到的，XDP程序中不允许使用bpf_get_current pid_tgid()这个Helper函数，因为接收数据包和触发XDP钩子的操作中，并不涉及用户空间进程或线程，所以此时使用该函数获取当前进程和线程ID是毫无意义的。

程序类型也决定了程序返回值的定义。还是以XDP为例，返回值会告诉内核，eBPF程序处理完数据包后，内核可能将其传递给网络栈、也可能丢弃或重定向到其他接口。如果一个跟踪点上的eBPF程序不会涉及网络数据包的处理，那么网络相关的返回值就没有意义了。

参见Helper函数手册(https://oreil.ly/e8K73)，由于BPF系统还在持续开发更新中，该手册可能不完整。

使用 bpftool feature 命令，可以获得当前内核版本中每种程序类型支持的 Helper 函数列表。该命令可以显示系统配置，并列出所有可用的程序类型和 Map 类型，及每种程序类型支持的 Helper 函数。

Helper函数被视为是Linux内核的UAPI（用户空间标准接口）的一部分。因此，一旦内核中定义了Helper函数，那么未来就不会改变，即使内核的内部函数和数据结构会发生变化。尽管Linux内核的内部函数在内核的不同版本之间有变化的风险，但如果eBPF程序员需要在eBPF程序中访问内核函数，可以通过BPF内核函数或kfuncs（https://oreil.ly/gKSEx）来实现。

# 7.3 Kfuncs

Kfuncs允许将内核函数注册到BPF子系统中，这样验证器就会允许eBPF程序调用这些函数。同时，允许调用kfunc的eBPF程序类型也需要在BPF子系统上进行注册。

与Helper函数不同Kfuncs函数不保证不同内核版本间是兼容的，因此使用Kfuncs时必须考虑不同内核版本之间的存在差异的可能性。

这里有一套对BPF的“核心”Kfuncs的介绍（https://oreil.ly/06qoi），Kfuncs函数主要用于允许eBPF程序获取和释放对Linux任务及cgroups的引用。

概括地说，eBPF程序类型决定了程序可以附加的事件类型，还决定了它接收到的上下文信息的类型，同时，程序类型还决定了程序可以调用的Helper函数和Kfuncs函数。

BPF程序类型大致可分为两类：跟踪（或perf）程序类型和网络类型程序。接下来，让我们来看几个例子。

# 7.4 跟踪

eBPF程序从用户空间获取内核事件跟踪信息的一种有效方式是，将程序附加到kprobes、tracepoints、rawtracepoints、fentry/fexitprobes和perf事件上。这些跟踪相关的程序类型不会影响内核对这些事件的响应方式（不过，正如你在第9章中看到的，这方面已经有了一些创新）。

这些程序有时被称为“perf-related”程序。例如，使用 bpftool perf 命令可以查看附加到事件的“perf-related”程序：

```txt
$ sudo bpftool perf show
pid 232272 fd 16: prog_id 392 kprobe func __x64_sys_execute offset 0
pid 232272 fd 17: prog_id 394 kprobe func do_execute offset 0
pid 232272 fd 19: prog_id 396 tracepoint sys-enter_execute
pid 232272 fd 20: prog_id 397 raw_tracepoint sched_process_exec
pid 232272 fd 21: prog_id 398 raw_tracepoint sched_process_exec 
```

当运行 chapter7 目录下 hello.bpf.c 的例子时，我们将看到上述输出结果。该代码示例将不同的程序附加到与 execve() 相关的各种事件上。本节将讨论所有这些类型的程序，概括地说，这些程序类型是：

- 使用 kprobe 附加到 execve() 系统调用的入口点。  
- 使用 kprobe 附加到内核函数 do_execute()。  
- 使用tracepoint附加到execve()系统调用的入口。  
- 在调用 execve() 时还会涉及两个版本的 raw tracepoint（本节将介绍其中一个版本，即启用 BTF 的版本）。

注意，运行与跟踪相关的类型的 eBPF 程序，需要使用 CAP_PERFMON，CAP_BPF 或者 CAP_SYS_admin 中的任意一种 capability。

# 7.4.1 Kprobes和Kretprobes

在第1章中讨论过Kprobes的概念，使用kprobe可以将几乎所有的程序注1附加到内核中的任何地方。通常，使用kprobes可以将程序附加到函数的入口，使用kretprobes机制可以将程序附加到函数的出口，也可以使用kprobe将程序附加到函数入口后某个偏移量的指令上。如果选择这样做2，你需要确信

你使用的内核版本可以把程序附加到你指定的位置上！相对而言，因为不同版本间函数内的代码很容易发生变动，所以附加到内核函数入口和出口点更稳妥。

![](images/e1af30ad99d2af38878af8b380a19ee7e9504a8d43e19e3c00c74d495a4b65f6.jpg)

示例 bpftool perf list 的输出中，可以看到两个 kprobes 的偏移量都是 0。

需要注意，如果一个函数被内联，那么使用kprobe的eBPF程序就没办法附加到函数的入口点。当内核编译时，编译器可能选择将某些内核函数“内联”，即不通过跳转指令调用函数，而是将函数的机器码直接嵌入到调用点中。

# 使用kprobes将程序附加到系统调用的入口点

本章的第一个eBPF程序示例叫kprobe_sys_execute，示例中使用kprobe将程序附加到系统调用execve()上。函数定义和Section定义如下所示：

```txt
SEC("ksyscall/execve")  
int BPF_KPROBE_SYSCALL(kprobe_sys_execve, char *pathname) 
```

这和第5章的例子一样。

使用 kprobe 将程序附加到系统调用的原因是因为该类接口是稳定的，即接口在不同版本的内核之间能保持稳定（tracepoints 也是如此，我们稍后会讲到）。然而，不能将使用 kprobe 的程序应用于安全工具上，原因将会在第 9 章详细介绍。

# 使用kprobes将程序附加到其他内核函数上

你可以找到很多使用kprobes将程序附加到系统调用的eBPF工具。就像前面提到的，使用kprobes可以将程序附加到内核中任何非内联的函数。在hello.bpf.c中有个例子，展示了使用kprobe将程序附加到函数do_execute()上，定义如下：

```sql
SEC("kprobe/do_execute")  
int BPF_KPROBE(kprobe_do_execute, struct filename *filename) 
```

do_execute() 并不是系统调用，因此和前一个例子区别如下：

- SEC 格式和上个例子中附加到系统调用入口点是一样的，而且不需要考虑不同平台的问题，因为 do_execve() 像大多数内核函数一样，所有平台是通用的。  
- 这里使用了 BPF_KPROBE，而不是 BPF_KPROBE_SYSCALL，两者作用是完全一样的，只是 BPF_KPROBE_SYSCALL 用来处理系统调用。  
- 还有一个重要的区别：execve()系统调用的参数是pathname，是一个字符串指针(char*），但是对于do_execve()函数，参数是filename，它是一个指向struct filename的指针，是内核中使用的一个数据结构。

你可能会有疑问，怎么知道该使用哪种参数，可以参考内核中的do_execve()函数的定义找到答案：

```c
int do_execve(struct filename *filename, const char __user *const __user *__argv, const char __user *const __user *__envp) 
```

这里忽略了do_execve()的__argv和__envp参数，只声明filename参数，使用类型struct filename*来匹配内核函数的定义。考虑到参数在内存中是按顺序排列的，忽略最后的 $n$ 个参数是可以的，但是如果你想使用后面的参数，参数列表中前面的参数就不能忽略。

filename结构体是在内核内部定义的，这也说明eBPF编程就是内核编程，所以你需要查找do_execve()的定义才能找到函数参数的定义，以及struct filename的定义。filename->name指向要运行的可执行文件名。在示例代码中，可以用以下几行来查找这个名称：

```txt
const char *name = BPF_CORE_READ(filename, name);  
bpfprobe_read_kernel(&data.Command, sizeof(data.Command), name); 
```

总结一下：使用kprobe将程序附加到系统调用上的上下文参数是一个结构体，表示用户空间传递给系统调用的值。使用kprobe将程序附加到普通函数（非系统调用）上的上下文参数是一个调用该函数时传递参数的结构体，这个结构体取决于函数的定义。

Kretprobes 与 kprobes 非常相似，不同之处在于 Kretprobes 是在函数返回时被触发的，Kretprobes 访问的是函数的返回值而不是入口参数，kprobes 访问的是函数的入口参数。

虽然Kprobes和Kretprobes是一种可以将程序附加到内核函数的方法，但是如果你使用的是新版本内核，应该考虑下面比较新的用法。

# 7.4.2 Fentry/Fexit

Fentry/Fexit 一种更高效的跟踪内核函数入口和出口的机制。在 x86 处理器上，Fentry/Fexit 与 BPF trampoline 机制一起在 5.5 版本的内核中引入的；在 ARM 处理器上，要到 Linux 6.0 才支持 BPF trampoline（https://oreil.ly/ccuz1）。如果你的内核足够新，Fentry/Fexit 是当前跟踪内核函数入口和出口的首选方法。同时，你可以使用 Kprobe 或 Fentry 机制编写相同功能的代码。

在 chapter7/hello.bpf.c 中有个 fentry 程序的例子：fentry_execve()。在这个例子中，使用了 libbpf 库的 BPF_PROG 宏声明了 eBPF 的 kprobe 程序。这个宏是一个便捷的包装器，它提供了类型化参数，而不是通用的上下文指针。同时，该例子中还声明了 fentry、fexit 和 tracepoint 的 eBPF 程序。定义如下：

```txt
SEC("fentry/do_execute")  
int BPF_PROG(fentry_execute, struct filename *filename) 
```

在libbpf库中，SEC("fentry/do_execve")说明将程序附加到do_execve()内核函数的入口。和kprobe示例中一样，上下文参数反映了传递给内核函数的参数。

Fentry和fexit附加点的设计比kprobes更有效率。而且fexit还有另一个优势：

当你想将程序附加到某个事件的函数出口时，fexit 可以访问函数的入口参数，而 kretprobe 则不能。你可以在 libbpf-bootstrap 的示例中看到这方面的例子 (https://oreil.ly/6HDh_)。kprobe.bpf.c 和 fentry.bpf.c 都附加到内核函数 do_unlinkat() 上。kretprobe 的 eBPF 程序签名如下所示：

```txt
SEC("kretprobe/do_unlinkat")  
int BPF_KRETPROBE(do_unlinkat_exit, long ret) 
```

使用BPF_KRETPROBE宏定义了一个kretprobe程序，该程序附加到do_unlinkat()函数出口处。程序接收的唯一参数是ret，是do_unlinkat()的返回值。下面是fexit版本：

```txt
SEC("fexit/do_unlinkat")  
int BPF_PROG(do_unlinkat_exit, int dfd, struct filename *name, long ret) 
```

在这个版本中，程序不仅可以访问返回值 ret，还可以访问 do_unlinkat() 函数的入口参数：dfd 和 name。

# 7.4.3 Tracepoints

Tracepoints(https://oreil.ly/yXk_L)是在内核代码的一些特定位置插入特殊指令，对内核的行为进行跟踪和分析（稍后将在本章讨论用户空间的跟踪点）。Tracepoints不是eBPF独有的功能，长期以来，Tracepoints可以被用于输出内核跟踪信息，并被诸如SystemTap（https://oreil.ly/bLmQL）等工具使用。与kprobes相比，Kprobes的优势是可以将程序附加到内核的任意指令上，但Tracepoint则在不同内核版本之间是稳定的（尽管有些Tracepoint在旧的内核上可能不存在。）

通过查看 /sys/kernel/tracing/available_events，可以获得当前内核上可用的跟踪事件：

```shell
$ cat /sys/kernel/tracing/available_events
tls:TLS_device_offload_set
tls:TLS_device decrypted 
```

```txt
...
syscalls:sys_exit_execveat
syscalls:sys-enter_execveat
syscalls:sys_exit_execve
syscalls:sys-enter_execve
```

5.15 版本的内核，列表里有 1400 多个 tracepoints。eBPF 程序的 SEC 部分要与列表中的跟踪点名称匹配，这样，libbpf 库可以自动地将程序附加到 tracepoint 上，如下所示：SEC("tp/tracing subsystem/tracepoint name")。

在 chapter7/hello.bpf.c 中有个与列表中的 syscalls:sys-enter_execve 跟踪点相匹配的例子, 在内核调用 execve() 函数时, 程序被触发。通过程序的 SEC 部分, libbpf 库可以知道这是一个 tracepoint 程序, 以及它应该附加到哪里, 如下所示:

```txt
SEC("tp/syscalls/sys-enter_execve")
```

那么tracepoint的上下文参数是什么呢？稍后介绍BTF部分时可以帮助我们理解这个问题，首先我们一起考虑下在没有BTF时需要怎么做。每个tracepoint都有个format文件来描述跟踪的字段。例如，以下是execve()系统调用入口处tracepoint的format文件：

```txt
$ cat /sys/kernel/tracing/events/syscalls/sys-enter_execve/format
name: sys-enter_execve
ID: 622
format:
field: unsigned short common_type; offset:0; size:2; signed:0;
field: unsigned char common_flags; offset:2; size:1; signed:0;
field: unsigned char common_preempt_count; offset:3; size:1; signed:0;
field:int common pid; offset:4; size:4; signed:1;
field:int __syscall_nb; offset:8; size:4; signed:1;
field: const char *filename; offset:16; size:8; signed:0;
field: const char *const *argv; offset:24; size:8; signed:0;
field: const char *const * envp; offset:32; size:8; signed:0; 
```

```txt
print fmt: "filename: 0x%08lx, argv: 0x%08lx, envp: 0x%08lx", ((unsigned long)(REC->filename)), ((unsigned long)(REC->argv)), ((unsigned long)(REC->envp)) 
```

使用这些信息，在chapter7/hello.bpf.c中定义了一个匹配的结构体，名为my_syscalls-enter_execute:

```c
struct my_syscalls-enter_execute {
    unsigned short common_type;
    unsigned char common_flags;
    unsigned char common_preempt_count;
    int common pid;
    long syscall Nr;
    long filename_ptr;
    long argv_ptr;
    long envp_ptr;
}; 
```

前四个字段不允许eBPF程序访问。如果访问这些字段，程序验证将失败，提示错误为：“an invalid bpf_context access error”。

示例中的eBPF程序在附加到该跟踪点时，可以使用指向该结构体的指针作为上下文参数，像这样：

```txt
int tp_sys-enter_execute(struct my_syscalls-enter_execute *ctx) { 
```

然后，程序可以访问该结构体的内容，如下所示获取文件名指针：

```txt
bpfprobe_read_user_str(&data.command, sizeof(data.Command), ctx->filename_ptr); 
```

使用tracepoint程序类型时，传递给eBPF程序的结构体由一组原始参数映射而成。为了获得更好的性能，可以使用raw_tracepoint类型直接访问这些原始参数。程序的SEC部分定义应以raw_tp（或raw_tracepoint）开头，而不是tp。你需要将参数从u64转换为tracepoint结构使用的类型（当tracepoint是系统调用的入口时，这些参数的类型还取决于芯片架构）。

# 7.4.4 开启BTF的Tracepoint

在前面的示例中有一个叫my_syscalls-enter_execute的结构体，作为eBPF

程序的上下文参数。但是，在eBPF代码中定义结构体或解析原始参数时存在一个风险，即代码可能与运行的内核不匹配，不过好消息是在第5章中讲到过的BTF可以解决这个问题。

有了BTF支持，vmlinux.h中会定义了一个与传递给tracepoint上下文参数相匹配的结构体。eBPF程序的SEC部分应该类似这样：SEC("tp_btf/tracepoint name")，其中“tracepoint name”应替换成/sys/kernel/tracing/available_events中的可用事件。在chapter7/hello.bpf.c的示例程序中，代码如下：

```cmake
SEC("tp_btf/sched_process_exec")  
int handle_exec(struct trace_event_raw_sched_process_exec *ctx) 
```

如例子所示，结构体名称与tracepoint name名称一致，前缀为trace_event_raw。

# 7.4.5 将程序附加到用户空间函数上

前面例子中已经展示了如何将eBPF程序附加到内核源代码中的事件上。用户空间代码也有类似的附加点：uprobes和uretprobes，用于将程序附加到用户空间函数的入口和出口；用户静态定义跟踪点（USDT），用于将程序附加到应用程序代码或用户空间库的指定Trancepoint上。这些程序使用的程序类型都是BPF_PROG_TYPE_KPROBE。

![](images/1cf62c93ab40f2eaa82bc0f7754e34b7dd04112c7a15dc376085d54e438e3ecf.jpg)

将程序附加到用户空间事件的程序例子有很多，BCC项目中有几个例子：

- bashreadline(https://oreil.ly/gDkaQ) 和 funclatency 工具 (https://oreil.ly/zLT54) 是附加到 u(ret)probe 上。  
- BCC中的USDT示例(https://oreil.ly/o894f)。

如果用libbpf库编写BPF程序，可以使用SEC()来定义用户空间探针程序的自动附加点。libbpf文档(https://oreil.ly/oOCBQ)中可以找到SEC部分所需的

格式。例如：使用uprobe将一个程序附加到OpenSSL库的SSL_write()函数的入口上，如下所示：

```cmake
SEC("uprobe/usr/lib/aarch64-linux-gnu/libssl.so.3/SSL_write") 
```

将程序附加到用户空间代码时，有几个问题需要注意：

- 请注意，例子中将程序附加到共享库上，共享库的路径是针对特定架构的，因此定义中需要指定相应的架构。  
- 除非你能控制运行代码的机器，否则是无法知道这个机器上安装了哪些用户空间库和应用程序。  
- 应用程序是作为独立的二进制文件构建的，将程序附加到共享库上可能不会被触发。  
- 容器内部通常使用的是一份文件系统副本，依赖项也是安装在这个副本中。因此容器内部使用的共享库文件与宿主机上的共享库文件可能并不相同。  
- eBPF程序还需要了解应用程序用什么语言编写。原因在于，在C语言中注3，函数的参数通常是使用寄存器传递，而在Go语言中函数的参数是使用堆栈传递，因此，跟踪保存寄存器信息的pt_args结构体用处不大。

尽管如此，还是有很多有用的工具使用eBPF程序跟踪用户空间程序的。例如，可以在SSL库中附加eBPF程序来实现加密和解密信息的跟踪。下一章将对此进行更详细的探讨。再比如Parca(https://www.parca.dev)工具，可以对应用程序进行持续性能分析。

# 7.4.6 LSM

BPF_PROG_TYPE LSM 类型用于将程序附加到 Linux 安全模块（LSM）的 API 上。

LSM是内核中的一个稳定接口，最初是供内核模块用来执行安全策略的。第9章你将看到使用该接口的eBPF安全工具的详细讨论。

BPF_PROG_TYPE LSM 类型是使用 bpf(BPF/raw_tracePOINT_OPEN) 系统调用将程序附加到 LSM 接口上的，因此经常被当作跟踪程序处理。BPF_PROG_TYPE LSM 类型的一个有趣特性是，其返回值会影响内核的行为：非零返回值表示安全检查未通过，内核不会继续执行任何操作。这与 perf 类型的程序有很大不同，perf 类型的程序的返回结果会被忽略。

![](images/44a3770aaf63c59fa5a03a1b339c522b6cd5ea1e4c4ec0ba6f31581ffb189a25.jpg)

Linux内核文档中关于LSMBPF程序的介绍，参见https://oreil.ly/vcPHY。

LSM并不是唯一能在安全方面发挥作用的BPF程序类型。下一节中会讲到许多与网络相关的程序类型，它们可用于网络安全，实现网络流量通过或禁止等网络相关的操作。在第9章中还会讲更多eBPF安全方面的应用。

本章已经讲到了一组内核和用户空间的跟踪程序的类型。通过这些跟踪程序，我们可以了解到整个系统的运行情况。接下来，将讨论附加到网络栈的eBPF程序类型，它们不仅可以用于可观测性，还可以影响到网络栈如何处理发送和接收的数据。

# 7.5 网络

很多不同的eBPF程序类型可用于处理网络栈中不同位置的