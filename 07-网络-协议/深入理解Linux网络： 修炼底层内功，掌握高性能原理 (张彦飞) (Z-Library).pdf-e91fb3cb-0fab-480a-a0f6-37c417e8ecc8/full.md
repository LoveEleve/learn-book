# 深入理解Linux网络

# 修炼底层内功，掌握高性能原理

张彦飞（@开发内功修炼）著

# 内容简介

本书通过先抛出一些开发、运维等技术人员在工作中经常遇见的问题，激发读者的思考。从这些问题出发，深入地对网络底层实现原理进行拆解，带领读者看清楚问题的核心，理解其背后的技术本质，提高大家的技术功力。例如网络包是如何被接收和发送的？阻塞到底在内部是如何发生的？epoll的底层工作原理又是啥？TCP连接在底层上是如何支持和实现的？书中对这些内容都有深度的阐述。本书旨在通过带领读者修炼底层内功，进而帮助大家深度掌握网络高性能原理。

未经许可，不得以任何方式复制或抄袭本书之部分或全部内容。

版权所有，侵权必究。

# 图书在版编目（CIP）数据

深入理解Linux网络：修炼底层内功，掌握高性能原理/张彦飞著．一北京：电子工业出版社，2022.6

ISBN 978-7-121-43410-5

I. ①深…Ⅱ. ①张…Ⅲ. ①Linux操作系统 IV. ①TP316.85

中国版本图书馆CIP数据核字（2022）第077254号

责任编辑：张月萍

印刷：中国电影出版社印刷厂

装 订：中国电影出版社印刷厂

出版发行：电子工业出版社

北京市海淀区万寿路173信箱

邮编：100036

开 本： $720\times 1000$

印张：21

字数：470千字

版次：2022年6月第1版

印次：2022年6月第1次印刷

印数：6000册 定价：118.00元

凡所购买电子工业出版社图书有缺损问题，请向购买书店调换。若书店售缺，请与本社发行部联系，联系及邮购电话：（010）88254888，88258888。

质量投诉请发邮件至zlts@phei.com.cn，盗版侵权举报请发邮件至dbqq@phei.com.cn。

本书咨询联系方式：（010）51260888-819，faq@phei.com.cn。

# 前言 Preface

# 从大厂的面试说起

互联网大厂是当今很多开发人员，尤其是应届毕业生们所向往的公司。但大家应该都听过关于大厂面试候选人的一句调侃的话，“面试造火箭，工作拧螺丝”。这虽然有一点儿夸张的成分，不过也确实描述得比较形象。在面试中，尤其是顶级互联网大厂的面试，对技术的考查往往都很深。但是到了工作中，可能确实又需要花不少时间在写各种各样的重复CRUD上。

那为啥会出现这种情况，是大厂闲得没事非得为难候选人吗？其实不是，这是因为扎实的底层功力确实对大厂来说很重要。

互联网大厂区别于小公司的一个业务特点就是海量请求，随便一个业界第二梯队的App，每天的后端接口请求数过亿很常见，更不用提微信、淘宝等头部应用了。在这种量级的用户请求下，业务能 $7 \times 24$ 小时稳定地提供服务就非常重要了。哪怕服务故障出现十分钟，对业务造成的损失可能都是不容小觑的。

所以在大厂中，你写出来的程序不是能跑起来就行了，是必须能够稳定运行。程序在运行期间可能会无法避免地遭遇各种线上问题。应用都是跑在硬件、操作系统之上的，因此线上的很多问题都和底层相关。如果遇到线上问题，你是否有能力快速排查和处理？例如有的时候线上访问超时是因为TCP的全连接队列满导致的。如果你对这类底层的知识了解得不够，则根本无法应对。

另外，大厂招聘高水平程序员的目的可能不仅仅是能快速处理问题，甚至希望程序员能在写代码之前就做出预判，从而避免出故障。不知道你所在的团队是否进行过Code Review（代码评审，简称CR）。往往新手程序员自我感觉良好、觉得写得还不错的代码

给资深程序员看一眼就能发现很多上线后可能会出现的问题。

大厂在招人上是不怕花钱的，最怕的是业务不稳定和不可靠。如果以很低的价钱招来水平一般的程序员，结果导致业务三天两头出问题，给业务收入造成损失，那可就得不偿失了。所以，要想进大厂，扎实的内功是不可或缺的。

# 谈谈工作以后的成长

那是不是说已经工作了，或者已经进入大厂了，扎实的内功、能力就可有可无了呢？答案当然是否定的，工作以后内功也同样的重要！

拿后端开发岗来举例。初接触后端开发的朋友会觉得，这个方向太容易了。我刚接触后端开发的时候也有这种错觉。我刚毕业做Windows下的C++开发的时候，项目里的代码编译完生成的工程都是几个GB的，但是转到后端后发现，一个服务端接口可能100行代码就搞定了。

由于看上去的这种“简单性”，许多工作三年左右的后端开发人员会陷入一个成长瓶颈，手头的东西感觉已经特别熟练了，编程语言、框架、MySQL、Nginx、Redis都用得很溜，总感觉自己没有啥新东西可以学习了。

他们真的已经掌握了所有了吗？其实不然，当他们遇到一些线上的问题时，排查和定位手段又极其有限，很难承担得起线上问题紧急救火的重要责任。当程序性能出现瓶颈的时候，只是在网上搜几篇帖子，盲人摸象式地试一试，各个一知半解的内核参数调一调，对关键技术缺乏足够深的认知。

反观另外一些工作经验丰富的高级技术人员，他们一般对底层有着深刻的理解。当线上服务出现问题的时候，都能快速发现关键问题所在。就算是真的遇到了棘手的问题，他们也有能力潜入底层，比如内核源码，去找答案，看看底层到底是怎么干的，为啥会出现这种问题。

所以大厂不仅仅是在招聘时考察应聘者，在内部的晋升选拔中也同样注重考察开发人员对于底层的理解以及性能把控的能力。一个人的内功深浅，决定了他是否具备基本的问题排查以及性能调优能力。内功指的就是当年你曾经学过的操作系统、网络、硬件等知识。互联网的服务都是跑在这些基础设施之上的，只有你对它们有深刻的理解，才能够源源不断想到新的性能分析和调优办法。

所以说，扎实的内功并不是通过大厂面试以后就没有用了，而是会贯穿你整个职业生涯。

# 再聊聊中年焦虑

之前网络曾爆炒一篇标题为“互联网不需要中年人”的文章，疯狂渲染35岁码农的前程问题，制造焦虑。本来我觉得这件事情应该只是媒体博眼球的一个炒作行为而已，不过恰恰两三年前我们团队扩充，需要招聘一些级别高一点儿的开发人员，之后使我对此话题有了些其他想法。那段时间我面试了七十多人，其中有很多工作七八年以上的。

我面试的这些人里，有这么一部分人虽然已经工作了七八年以上，但是所有的经验都集中在手头的那点儿项目的业务逻辑上。对他们稍微深入问一点儿性能相关的问题都没有好的思路，技术能力并没有随着工作年限的增长而增长。换句话说，他们并不是有七八年经验，而是把两三年的经验用了七八年而已。

和这些人交流后，我发现共同的原因就是他们绝大部分的时间都是在处理各种各样的业务逻辑和bug，没有时间和精力去提升自己的底层技术能力，真遇到线上问题也没有耐心钻研下去，随便在网上搜几篇文章都试一试，哪个碰对了就算完事，或者干脆把故障抛给运维人员去解决，导致技术水平一直原地踏步，并没有随着工作年限而同步增长。我从那以后也确实认识到，码农圈里可能真的有中年焦虑存在。

那是不是说这种焦虑就真的无解了呢？答案肯定是“不是”。至少我面试过的这些人里还有一部分很优秀，不但业务经验丰富，而且技术能力出众，目前都发挥着重要作用。你也可以看看你们公司的高级别技术人员，甚至业界的各位技术大牛，相信他们会长期是你们公司甚至业界的中流砥柱。

那么工作了多年的这两类人中，差异如此巨大的原因是什么呢？我思考了很多，也和很多人都讨论过这个问题。最后得出的结论就是大牛们的技术积累是随着工作年限的增长而逐渐增长的，尤其是内功，和普通的开发人员相差巨大。

大牛们对底层的理解都相当深刻。深厚的内功知识又使得他们学习起新技术来非常快。举个例子，在初级开发人员眼里，可能Java的NIO和Golang的net包是两个完全不同的东西，所以学习起来需要分别花费不少精力。但在底层知识深厚的人眼里，它们两个只不过是对epoll的不同封装方式，就像只换了一身衣服，理解起来自然就轻松得多。

如此良性迭代下去，技术好的和普通的开发人员相比，整体技术水平差距越拉越大。普通开发人员越来越焦虑，甚至开始担心技术水平被刚毕业的年轻人赶超。

# 修炼内功的好处

内功，它不帮你掌握最新的开发语言，不教会你时髦的框架，也不会带你走进火热的人工智能，但是我相信它是你成为“大牛”的必经之路。我简单列一下修炼内功的好处。

1）更顺利地通过大厂的面试。大厂的面试对技术的考查比较底层，而网上的很多答案层次都还比较浅。拿三次握手举例，一般网上的答案只说到了初步的状态流转。其实三次握手中包含了非常多的关键技术点，比如全连接队列、半连接队列、防syn flood攻击、队列溢出丢包、超时重发等深层的知识。再拿epoll举例，如果你熟悉它的内部实现方式，理解它的红黑树和就绪队列，就知道它高性能的根本原因是让进程大部分时间都在处理用户工作，而不是频繁地切换上下文。如果你的内功能深入触达这些底层原理，一定会为你的面试加分不少。  
2）为性能优化提供充足的“弹药”。目前大公司内部对于高级和高级以上工程师晋升时考核的重要指标之一就是性能优化。在对内核缺乏认识的时候，大家的优化方式一般都是盲人摸象式的，手段非常有限，做法很片面。当你对网络整体收发包的过程理解了以后，对网络在CPU、内存等方面的开销的理解将会很深刻。这会对你分析项目中的性能瓶颈所在提供极大的帮助，从而为你的项目性能优化提供充足的“弹药”。  
3）内功方面的技术生命周期长。Linux操作系统1991年就发布了，现在还是发展得如火如荼。对于作者Linus，我觉得他也有年龄焦虑，但他可能焦虑的是找不到接班人。反观应用层的一些技术，尤其是很多的框架，生命周期能超过十年我就已经觉得它很牛了。如果你的精力全部押宝在这些生命周期很短的技术上，你说能不焦虑吗！所以我觉得戒掉浮躁，踏踏实实练好内功是你对抗焦虑的解药之一。  
4）内功深厚的人理解新技术非常快。不用说业界的各位“大牛”了，就拿我自己来举两个小例子。我其实没怎么翻过Kafka的源码，但是当我研究完了内核是如何读取文件的、内核处理网络包的整体过程后，就“秒懂”了Kafka在网络这块为啥性能表现很突出了。还有，当我理解了epoll的内部实现以后，回头再看Golang的net包，才切切实实看懂了绝顶精妙的对网络IO的封装。所以你真的弄懂了Linux内核的话，再看应用层的各种新技术就犹如戴了透视镜一般，直接看到骨骼。  
5）内核提供了优秀系统设计的实例。Linux作为一个经过千锤百炼的系统，其中蕴含了大量的世界顶级的设计和实现方案。平时我们在自己的业务开发中，在编码之前也需要先进行设计。比如我在刚工作的时候负责数据采集任务调度，其中的实现就部分参考了操作系统进程调度方案。再比如，如何在管理海量连接的情况下仍然能高效发现某一

条连接上的IO事件，epoll内部的“红黑树 + 队列”组合可以给你提供一个很好的参考。这种例子还有很多很多。总之，如果能将 Linux 的某些优秀实现搬到你的系统中，会极大提升你的项目的实现水平。

时髦的东西终究会过时，但扎实的内功将会伴随你一生。只有具备了深厚的内功底蕴，你才能在发展的道路上走得更稳、走得更远。

# 为什么要写这本书

平时大家都是用各种语言进行业务逻辑的代码编写，无论你用的是PHP、Go，还是Java，都属于应用层的范畴。但是应用层是建立在物理层和内核层之上的。我把在应用层的技术能力称为外功，把Linux内核、设备物理结构方面的技术能力称为内功。前面已经说了，无论是在职业生涯的哪个阶段，扎实的内功都很重要。

那好，既然内功如此重要，那就找一些底层相关的资料加强学习就行了。但很遗憾，我觉得目前市面上的技术资料在内功方向上存在一些不足。

先说网上的技术文章。目前网上的技术文章、博客非常多。大家遇到问题往往先去搜一下。但是你有没有发现，网上入门级资料一搜一大把，而内功深厚、能深入底层原理的文章却十分匮乏。

比如，现在的互联网应用大部分都是通过TCP连接来工作的，那么一台机器最多能撑多少个TCP连接？按道理说，整个业界都在讲高并发，这应该算是很入门的一个问题了。但当年我产生这个疑问的时候，在搜索引擎上搜了个遍也没找到令我满意的答案。后来我干脆自己动手，花了一个多月时间边做测试，边扒内核源码，才算是把问题彻底搞明白了。

再比如，大部分的开发人员都搞过网络相关的开发。那么一个网络包是如何从网卡到达你的进程的？这个问题表面上看起来简单，但实际上很多性能优化方案都和这个接收过程有关，能不能深度理解这个过程决定了你在网络性能上有多少优化措施可用。例如多队列网卡的优化方案是在硬中断这一步开始将工作分散在多个CPU核上，进而提升性能的。我几年前想把这个问题彻底搞清楚，几乎搜遍了互联网，翻遍了各种经典书都没能找到想要的答案。

还比如，网上搜到的三次握手的技术文章都是在说一些简单的内容，客户端如何发起 SYN 握手进入 SYN_SENT 状态，服务端响应 SYN 并回复 SYNACK，然后进入 SYN_

RECV……诸如此类。但实际上，三次握手的过程执行了很多内核操作，比如客户端端口选择、重传定时器启动、半连接队列的添加和删除、全连接队列的添加和删除。线上的很多问题都是因为三次握手中的某一个环节出问题导致的，能否深度理解这个过程直接决定你是否有在线上快速消灭或者避免此类问题的能力。网上能深入介绍三次握手的文章太少了。

你可能会说，网上的文章不足够好，不是还有好多经典书吗？首先我得说，计算机类的一些经典的书确实很不错，值得你去看，但是这里面存在几个问题。

一是底层的书都写得比较深奥难懂，你看起来需要花费大量的时间。假如你已经工作了，很难有这么大块的时间去啃。比如我刚开始深入探寻网络实现的时候，买来了《深入理解Linux内核》《深入理解Linux网络技术内幕》等几本书，利用工作之余断断续续花了将近一年时间才算理解了一个大概。

另外一个问题就是当你真正在工作中遇到一些困惑的时候，会发现很难有一本经典书能直接给你答案。比如在《深入理解Linux网络技术内幕》这本书里介绍了内核中各个组件，如网卡设备、邻居子系统、路由等，把相关源码都讲了一遍。但是看完之后我还是不清楚一个包到底是如何从网卡到应用程序的，一台服务器到底能支持多少个TCP连接。

还有个问题就是计算机技术不同于其他学科，除理论外对实践也有比较高的要求。如果只是停留在经典书里的理论阶段，实际上很多问题根本就不能理解到位。这些书往往又缺乏和实际工作相关的动手实验。比如对于一台服务器到底能支持多少个TCP连接这个问题，我自己就是在做了很多次的实验以后才算比较清晰地理解了。还有就是如果没有真正动过手，那你将来对线上的性能优化也就无从谈起了。

总的来说，看这些经典书不失为一个办法，但考量时间的花费和对工作问题的精准处理，我感觉效率比较低。所以鉴于此，我决定输出一些内容，也就有了这本书的问世。

# 创作思路

虽然底层的知识如此重要，但这类知识有个共同的特点就是很枯燥。那如何才能把枯燥的底层讲好呢？这个问题我思考过很多很多次。

2012年我在腾讯工作期间，在内部KM技术论坛上发表过一篇文章，叫作《Linux文件系统十问》（这篇文章现在在外网还能搜到，因为被搬运了很多次）。当时写作的背

景是“老大”分配给我一个任务，把所有合作方提供的数据里的图片文件都下载并保存起来。我把在工作中产生的几个疑问进行了追根溯源，找到答案以后写成文章发表了出来。比如文件名到底存在了什么地方，一个空文件到底占不占用磁盘空间，Linux目录下子目录太多会有什么问题，等等。这篇文章发表出来以后，竟然在全腾讯公司内部传播开了，反响很大，最后成为了腾讯KM当年的年度热文。

为什么我的一篇简单的Linux文件系统的文章能得到这么强烈的回响？后来我在罗辑思维的一期节目里找到了答案。节目中说最好的学习方式就是你自己要产生一些问题，带着这些问题去知识的海洋里寻找答案，当答案找到的时候，也就是你真正掌握了这些知识的时候。经过这个过程掌握的知识是最深刻的，和你自身的融合程度也是最高的，能完全内化到你的能力体系中。

换到读者的角度来考虑也是一样的。其实读者并不是对底层知识感兴趣，而是对解决工作中的实际问题兴趣很大。这篇文章其实并不是在讲文件系统，而是在讲开发过程中可能会遇到的问题。我只是把文件系统知识当成工具，用它来解决掉这些实际问题而已。

所以我在本书的创作过程中，一直贯穿的是这个思路：以和工作相关的实际的问题为核心。

在每一章中，我并不会一开始就给你灌输软中断、epoll、socket内核对象等内核网络模块的知识，我也觉得这些很乏味，而是每章先抛出几个和开发工作相关的实际问题，然后围绕这几个问题展开探寻。是的，我用的词不是“学习”，而是“探寻”。和学习相比，探寻更强调对要解惑的问题的好奇心，更有意思。

虽然本书中会涉及很多的源码，但这里先强调一下，这并不是一本源码解析的书。大家学习的真正目的是理解和解决项目实践相关的问题，进而提高驾驭手头工作的能力，而源码只是我们达成目的的工具和途径而已。

# 适用读者

本书并不是一本计算机网络的入门书，阅读本书需要你具备起码的计算机网络知识。它适合以下读者：

想通过提升自己的网络内功而进大厂的读者。  
- 不满足于只学习网络协议，也想理解它是怎么实现的读者。

- 虽有几年开发工作经验，但对网络开销把握不准的开发人员。  
想做网络性能优化，但没有成体系的理论指导的读者。  
- 维护各种高并发服务器的运维人员。

# 其他说明

本书中的内容是在我的微信公众号“开发内功修炼”的部分内容的基础上，理顺了整体的框架结构整理而来的。欢迎大家关注我的微信公众号，及时阅读最新内容。另外，由于本人精力有限，书中内容难免会有疏漏。如您发现内容中有不正确的地方，欢迎到微信公众号后台或者联系本人微信批评指正，不胜感激！也欢迎大家加入我的微信交流群，互相学习、共同成长。个人微信账号为zhangyanfei748528。

# 致谢

本书能够得以问世，要感谢许多许多人。

首先要感谢的是我的微信公众号和知乎专栏里的粉丝们。我提笔写下第一篇文章的时候，是根本没敢想能够成体系出一本书的，是你们的认可和鼓励支持着我输出一篇又一篇的硬核技术文。现在回头一看，竟然攒了好几十篇。基于这些文章，将来再整理出一本书都是有可能的。而且很多读者技术也非常优秀，指出了我的文章中不少的瑕疵。飞哥在此对大家表示感谢！

接下来要感谢的是我的爱人，在我写作的过程中给了我很大的支持和鼓励，还帮我分担了很多遛娃、看娃的任务，让我能专心地投入到写作中来。写作要投入的精力是巨大的，如果缺少家人的支持，想完成一本书基本是不可能的。

最后要感谢的是道然科技姚老师以及电子工业出版社的老师们，是你们帮我完成出书过程最后的“临门一脚”。

# 目录

# contents

# 第1章 绪论 / 1

1.1 我在工作中的困惑 / 2

1.1.1 过多的TIME_WAIT / 2  
1.1.2 长连接开销 / 2  
1.1.3 CPU被消耗光了 / 3  
1.1.4 为什么不同的语言网络性能差别巨大 / 4  
1.1.5 访问127.0.0.1过网卡吗 / 4  
1.1.6 软中断和硬中断 / 5   
1.1.7 零拷贝到底是怎么回事 / 5  
1.1.8 DPDK / 5

1.2 本书内容结构 /6

1.3 一些约定 /7  
1.4 一些术语 /8

# 第2章 内核是如何接收网络包的 / 9

2.1 相关实际问题 / 10  
2.2 数据是如何从网卡到协议栈的 / 11

2.2.1 Linux网络收包总览 / 12  
2.2.2 Linux启动 / 13  
2.2.3 迎接数据的到来 / 23  
2.2.4 收包小结 /33

2.3 本章总结 / 34

# 第3章 内核是如何与用户进程协作的 / 41

3.1 相关实际问题 / 42  
3.2 socket的直接创建 / 43

3.3 内核和用户进程协作之阻塞方式 / 46

3.3.1 等待接收消息 / 47   
3.3.2 软中断模块 / 52   
3.3.3 同步阻塞总结 / 57

3.4 内核和用户进程协作之epoll / 59

3.4.1 epoll内核对象的创建 / 60  
3.4.2 为epoll添加socket /62  
3.4.3 epoll_wait之等待接收 / 68   
3.4.4 数据来了 /71  
3.4.5 小结 /79

3.5 本章总结 /80

# 第4章 内核是如何发送网络包的 / 84

4.1 相关实际问题 / 85  
4.2 网络包发送过程总览 / 86  
4.3 网卡启动准备 / 90  
4.4 数据从用户进程到网卡的详细过程 / 92

4.4.1 send系统调用实现 / 92  
4.4.2 传输层处理 /94  
4.4.3 网络层发送处理 / 99  
4.4.4 邻居子系统 / 103  
4.4.5 网络设备子系统 / 105  
4.4.6 软中断调度 / 109  
4.4.7 igb网卡驱动发送 / 111

4.5 RingBuffer内存回收 / 114   
4.6 本章总结 /115

# 第5章 深度理解本机网络IO / 119

5.1 相关实际问题 / 120

5.2 跨机网络通信过程 / 120

5.2.1 跨机数据发送 / 120  
5.2.2 跨机数据接收 / 125  
5.2.3 跨机网络通信汇总 / 127

5.3 本机发送过程 /127

5.3.1 网络层路由 / 127  
5.3.2 本机IP路由 /130  
5.3.3 网络设备子系统 / 131  
5.3.4 “驱动”程序 / 133

5.4 本机接收过程 /135  
5.5 本章总结 /137

# 第6章 深度理解TCP连接建立过程 / 139

6.1 相关实际问题 / 140  
6.2 深入理解listen / 141

6.2.1 listen系统调用 / 141  
6.2.2 协议栈listen / 142   
6.2.3 接收队列定义 / 143  
6.2.4 接收队列申请和初始化 / 145  
6.2.5 半连接队列长度计算 / 146  
6.2.6 listen过程小结 / 148

6.3 深入理解connect / 148

6.3.1 connect调用链展开 / 149   
6.3.2 选择可用端口 /151  
6.3.3 端口被使用过怎么办 / 153  
6.3.4 发起syn请求 / 155  
6.3.5 connect小结 / 156

6.4 完整TCP连接建立过程 / 157

6.4.1 客户端connect / 159   
6.4.2 服务端响应SYN / 160  
6.4.3 客户端响应SYNACK / 162   
6.4.4 服务端响应ACK /164

6.4.5 服务端accept /167  
6.4.6 连接建立过程总结 / 167

6.5 异常TCP连接建立情况 / 169

6.5.1 connect系统调用耗时失控 / 169  
6.5.2 第一次握手丢包 / 171  
6.5.3 第三次握手丢包 / 176  
6.5.4 握手异常总结 / 178

6.6 如何查看是否有连接队列溢出发生 / 179

6.6.1 全连接队列溢出判断 / 179  
6.6.2 半连接队列溢出判断 / 181  
6.6.3 小结 / 183

6.7 本章总结 / 183

# 第7章 一条TCP连接消耗多大内存 / 187

7.1 相关实际问题 / 188  
7.2 Linux内核如何管理内存 / 188

7.2.1 node划分 / 189  
7.2.2 zone划分 / 191  
7.2.3 基于伙伴系统管理空闲页面 / 192  
7.2.4 slab分配器 / 194  
7.2.5 小结 / 197

7.3 TCP连接相关内核对象 / 198

7.3.1 socket函数直接创建 / 198  
7.3.2 服务端socket创建 / 206

7.4 实测TCP内核对象开销 / 207

7.4.1 实验准备 / 207   
7.4.2 实验开始 / 208  
7.4.3 观察ESTABLISH状态开销 / 209   
7.4.4 观察非ESTABLISH状态开销 / 211   
7.4.5 收发缓存区简单测试 / 214  
7.4.6 实验结果小结 / 215

7.5 本章总结 / 216

# 第8章 一台机器最多能支持多少条TCP连接 / 218

8.1 相关实际问题 / 219  
8.2 理解Linux最大文件描述符限制 / 219

8.2.1 找到源码入口 / 220  
8.2.2 寻找进程级限制nofile和fs.nr_open / 221   
8.2.3 寻找系统级限制fs.file-max / 223  
8.2.4 小结 / 224

8.3 一台服务端机器最多可以支撑多少条TCP连接 / 225

8.3.1 一次关于服务端并发的聊天 / 225  
8.3.2 服务器百万连接达成记 / 228  
8.3.3 小结 / 232

8.4 一台客户端机器最多只能发起65535条连接吗 / 232

8.4.1 65535的束缚 /232  
8.4.2 多IP增加连接数 / 234  
8.4.3 端口复用增加连接数 / 236  
8.4.4 小结 / 243

8.5 单机百万并发连接的动手实验 / 243

8.5.1 方案一，多IP客户端发起百万连接 / 244  
8.5.2 方案二，单IP客户端机器发起百万连接 / 248  
8.5.3 最后多谈一点 / 250

8.6 本章总结 / 251

# 第9章 网络性能优化建议 / 253

9.1 网络请求优化 / 254  
9.2 接收过程优化 / 256  
9.3 发送过程优化 / 262  
9.4 内核与进程协作优化 / 268  
9.5 握手挥手过程优化 / 269

# 第10章 容器网络虚拟化 / 272

10.1 相关实际问题 / 273  
10.2 veth设备对 / 274

10.2.1 veth如何使用 / 274  
10.2.2 veth底层创建过程 / 276  
10.2.3 veth网络通信过程 / 278  
10.2.4 小结 / 281

10.3 网络命名空间 / 281

10.3.1 如何使用网络命名空间 / 282  
10.3.2 命名空间相关的定义 / 284  
10.3.3 网络命名空间的创建 / 287  
10.3.4 网络收发如何使用网络命名空间 / 295  
10.3.5 结论 / 296

10.4 虚拟交换机Bridge / 297

10.4.1 如何使用Bridge / 298  
10.4.2 Bridge是如何创建出来的 / 301  
10.4.3 添加设备 / 303  
10.4.4 数据包处理过程 / 305  
10.4.5 小结 / 308

10.5 外部网络通信 / 310

10.5.1 路由和NAT / 311  
10.5.2 实现外部网络通信 / 313  
10.5.3 小结 / 318

10.6 本章总结 / 319

![](images/67e5b45a82b7e628fb8f5856419b28b418a2090b4563bf40599fe9a6d7c3271a.jpg)

开篇先引用一段庖丁解牛里的典故。话说梁惠王因庖丁解牛的技术而惊叹，于是就问庖丁，文惠君曰：“嘻，善哉！技盖至此乎？”意思是：你的技术怎么会高明到这种程度呢？

庖丁曰：“始臣之解牛之时，所见无非牛者。三年之后，未尝见全牛也。”庖丁的回答意思是，我刚开始解牛时，对牛的结构还不了解，看见的无非就是整头的牛，但三年之后，我看见的再也不是整头的牛了，而是牛的内部筋骨肌理，所以技术越来越精进！

开发技术和解牛技术是相通的。在你对底层工作原理不清楚时，能看到的只是个整体。等到技术精进之后，你将能看到内核的筋骨肌理，各个模块是如何有机协作的。当你达到这个境界以后，技术能力也就变得更强了！

# 1.1 我在工作中的困惑

有人说，学习网络就是在学习各种协议，这种说法其实误导了很多的人。

提到计算机网络的知识点，你肯定首先想到的是OSI七层模型、IP、TCP、UDP、HTTP等。关于TCP，再多一点儿你也许会想到三次握手、四次挥手、滑动窗口、流量控制。关于HTTP协议，就是报文格式、GET/POST、状态码、Cookie/Session等。现在市面上与网络相关的书、课程也基本是以协议为主。协议相关的内容确实很重要，但是有了这些知识仍然不能帮我解决在工作实践中遇到的一些问题。

# 1.1.1 过多的TIME_WAIT

有一次我们的运维人员找过来，说某几台线上机器上出现了3万多个TIME_WAIT，说是不行了，应赶紧处理。后来他帮我们打开了tcp_tw_reuse和tcp_tw_recycle，先把问题处理掉了。

虽然问题算是临时处理了，但是我的思考却没有停止，一个TIME_WAIT状态的连接到底会有哪些开销？是端口占用导致新连接无法建立？还是会过多消耗机器上的内存？3万条TIME_WAIT究竟该算是warning还是error？解决TIME_WAIT的更好的办法是什么？这些困惑激发了我强烈的好奇心。

# 1.1.2 长连接开销

另外一次是我们的业务人员要进行性能优化，为了节约频繁的握手、挥手开销，我们将访问MySQL和Redis等数据服务器时的短连接都改成了长连接。

那时我们公司还没有建立统一Redis平台，是业务人员自己维护了一组Redis服务器。当开启长连接后，一个Redis实例上最终出现了6000条TCP连接。当时我的内心是有点儿惶恐的，因为之前从来没试过这么高的并发数。虽然知道连接上大部分时间都是空

闲的，但仍然担心这6000条即使是空闲的连接会不会把服务器搞坏。等上线以后观察一段时间发现没有太大问题才算是稍稍安心一些。

但到了MySQL上，就没那么顺利了。公司很早就提供了统一的MySQL平台。在平台上申请权限时需要为每一个IP填一个并发数，平台的负责人员来进行审批。因为当时使用的是php-fpm，没有连接池，所以我们有多少个fpm进程，就得申请多大的并发数。我们当时申请了200个，然后工程部的同事就找过来了：“你们这单机200个并发不行，太高了！”

我告诉他虽然我们申请了这么高的并发，但其实绝大部分时间连接上都是空闲的。又给他看了我们长连接下Redis的服务器状态，他最终勉强同意我们这么干。

在这个过程中，我发现了一个关键的问题，我当时其实吃不准一条空闲的TCP连接到底有多大的开销。我如果当时能把空闲TCP连接的CPU、内存开销都理解得很透彻，就没有上面这么多的瞎担心了。

把这个问题再拓展拓展，就整理出另外几个问题。

# 1）一台服务器最多可以支撑多少条TCP连接？

我们假设所有的TCP连接都是空连接，那么一台服务器上最多可以支撑多少条TCP连接？你是否能有一个量化的估计？这个最大数字是受CPU配置的影响，还是受内存大小的限制？一台机器有可能支撑起100万条并发长连接吗？当理解了机器在极限情况下的表现，回头再看项目中的并发数，你就不会再有无谓的恐慌了。

# 2）一台客户端最多可以支撑多少条TCP连接？

因为客户端和服务器不一样的地方在于，每次建立TCP连接请求时都会消耗一个端口，而这个端口在TCP协议中又是一个16位的整数（0～65535），那么是否意味着客户端单机最多只能建立起65535条连接？

# 3）一条TCP连接需要消耗多大的内存？

相对前两个问题，这个问题更本质一些。对前面两个问题把握不准很大程度是因为不理解TCP连接的网络开销。我们可以还假设这条TCP连接是空连接，只是进行了三次握手，并没有产生真正的数据。好，请问一条TCP连接需要吃掉多少内存，是几KB，还是几十KB，还是几MB？

# 1.1.3 CPU被消耗光了

还有一次是我的一个线上CPU消耗过高的问题。事发在我们的一组云控接口，是用Nginx + Lua写的。正常情况下，单虚拟机8核8GB可以扛每秒2000左右的QPS，负载一直都比较健康。

但是该服务近期开始偶发一些500状态的请求了，监控时不时会出现报警。通过sar-u命令查看峰值时CPU余量只剩下20%~30%。但奇怪的是，负载竟然是比较正常的，当

时的监控系统展示如图1.1所示。

![](images/44b7d970aaa79f2b95f8a7b467e91c8e3dca4157090e85a6847da5d7b8199f7a.jpg)

![](images/58da92e9eeb4444aeb56fc4da5217dbde6241b7e603c45c241f4788f904d7081.jpg)  
图1.1 CPU与负载监控

后来经过两天的排查发现，根本原因是在端口不充足的情况下，connect系统调用的CPU消耗会大幅度增加。负载指的是就绪状态等待CPU调用的进程数量统计，而服务器上进程又不多，所以自然负载并不高。定位到问题，处理起来办法就多了。最后通过干掉一段不重要的业务逻辑解决了问题。

那为什么在端口不充足的情况下，connect系统调用的CPU消耗会大幅度增加，其根本原因是什么？我又陷入了深深的思考。

# 1.1.4 为什么不同的语言网络性能差别巨大

上一节提到我们的一个用Nginx + Lua写的服务，单虚拟机8核8GB可以扛每秒2000左右的QPS，负载还一直比较健康。但是我们的其他php-fpm的服务却远远到不了这个数，500 QPS都算是比较好的情况了。

那问题来了，为什么使用不同的语言网络性能差别有这么大，这底层的根本原因是什么？所以我接下来深入挖掘了同步阻塞网络IO，去分析阻塞在内核中的到底是一个什么样的操作，也深入分析了epoll的工作原理，终于彻底搞懂了多路复用之所以高性能的根本原因，也终于理解了为什么Redis可以做到每秒处理几万条的请求。

有了这些深度的理解，再看其他语言里的网络模型，例如Java的NIO、Golang的net包将更轻松。因为不同的语言，只是对内核提供的网络IO进行不同方式的封装而已，本质上都相差无几。

# 1.1.5 访问127.0.0.1过网卡吗

现在的互联网业务中，尤其是近期随着sidecar等模式的兴起，本机网络IO的应用也越来越广泛。那么问题来了，本机网络IO和跨机比起来，执行过程是怎样的？数据需要经过网卡吗？性能有没有那么一点点的优势？有的话，那是节约了哪一部分的开销呢？

网上还有文章建议把本机的网络通信中指定的本机IP都换成127.0.0.1，这样就能节约一些开销，从而提升性能。我对此感到好奇，这个说法靠谱吗？如果说它靠谱，那到底是节约了哪些开销？

# 1.1.6 软中断和硬中断

在内核的网络模块中，有两个很重要的组件，硬中断和软中断，软中断还分成了NET_RX（R指的是Receive）和NET_TX（T指的是Transmit）等几大类。从字面意思上来看，RX是接收，TX是发送。但是即使在收发差不多相同的服务器上NET_RX也比NET_TX要大得多，对此我也是非常好奇。

<table><tr><td colspan="5">$ cat /proc/softirqs</td></tr><tr><td></td><td>CPU0</td><td>CPU1</td><td>CPU2</td><td>CPU3</td></tr><tr><td>HI:</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>TIMER:</td><td>1670794607</td><td>218940516</td><td>3765758957</td><td>3937988107</td></tr><tr><td>NET_TX:</td><td>384508</td><td>285972</td><td>244566</td><td>258230</td></tr><tr><td>NET_RX:</td><td>1591545176</td><td>1212716226</td><td>1017620906</td><td>1058380340</td></tr></table>

还有一次一位粉丝和我反馈，他执行了一次测试，调用send命令发送一个“Hello World”出去之后，NET_TX并没有增加。对于这个我更感觉诧异了。

类似的疑惑还有。我们在线上有一组服务器的网络IO比较高，在单任务队列的机器上，过多的软中断si（top命令里展示的软件中断CPU消耗占比）开销都打在一个核上了。所以我们决定开启多队列网卡优化调研，发现要想把软中断si开销分散到多个CPU核上，操作的却是硬中断号和CPU之间的绑定关系。这又是为什么？

在Linux上使用top等命令查看CPU开销时，展示结果中把总开销分成了us、sy、hi、si等几项。其中us是花在用户空间的CPU占比，sy是内核空间占比，hi是硬件中断消耗占比，si是软件中断CPU占比。

# 1.1.7 零拷贝到底是怎么回事

很多性能优化方案里都会提到零拷贝。零拷贝到底是怎么回事，是真的没有数据的内存拷贝了？究竟是避免了哪步到哪步的拷贝操作？如果不了解数据在网络包收发时各个不同内核组件中的拷贝过程，对零拷贝根本理解不到本质上。

# 1.1.8 DPDK

老的还没学完，又有很多新技术出来了。比如DPDK究竟是什么，是否需要学习和使用它？其实理解不了这个新技术的根本原因可能是你对Linux内核工作原理不清楚。当你掌握了Linux内核的网络处理过程以后，回头再看DPDK这类Kernel-ByPass的技术，直接就可以大致理解了。

这些问题都是飞哥在工作中陆陆续续遇到的，都是和实践相关的。如果对于网络你

只是学过协议，而不了解Linux内核的实现，对于这些问题其实是无能为力的。而且当我产生这些疑惑时，在网上进行了很多的搜索，但一直没有搜到能深入根本原因的结论。索性我就撸起袖子，通过挖掘内核源码，做测试，自己在实现层面把计算机网络扒了个遍。把这些问题彻底搞明白，也就形成了本书的内容。

# 1.2 本书内容结构

# 第1章 绪论

这一章分享了飞哥在工作的十多年中遇到的一些线上问题，以及由此带来的困惑和疑问。

# 第2章 内核是如何接收网络包的

在这一章中，深入分析了Linux网络接收包的过程。在这里，你将看到网卡、RingBuffer、硬中断、软中断等组件是如何紧密配合的，也将了解到发送过程是如何消耗CPU的，也会深刻理解为什么网卡开启多队列能提升网络性能。

# 第3章 内核是如何与用户进程协作的

在这一章中，将分析阻塞到底干了什么，为什么同步阻塞的网络IO模型性能比较差，还有epoll之所以高效的深层次原理。通过学习这一章你将能理解为什么Redis可以达到10万QPS的高性能。

# 第4章 内核是如何发送网络包的

在这一章，我们会看到为什么软中断中NET_RX要比NET_TX高得多，也能理解内核在发送网络包时都涉及哪些内存拷贝操作。理解了这个再来看Kafka里用到的零拷贝，就能很容易理解了。还能了解到在查看发送网络包的CPU消耗时，应该sy（CPU在内核空间的消耗占比）和si（CPU在软中断上的消耗占比）同时都看。

# 第5章 深度理解本机网络IO

现在本机网络IO用得也很多。那么本机网络IO过网卡吗？和外网网络通信相比，在内核收发流程上有什么差别？访问本机服务时，使用127.0.0.1能比使用本机IP（例如192.168.x.x）更快吗？这些问题你将在这一章看到清晰的讲解。

# 第6章 深度理解TCP连接建立过程

实际上内核实现的三次握手过程涉及很多关键操作，如半/全连接队列的创建与长度限制、客户端端口的选择、半连接队列的添加与删除、全连接队列的添加与删除，以及重传定时器的启动。在这章中，你将深入理解内核的这些底层工作。再遇到线上因三次握手而导致的问题时，相信你就能从容应对了。

# 第7章 一条TCP连接消耗多大内存

内核和应用程序一样，也是需要不停地申请和释放内存的。但和应用程序不同的是，内核使用一种叫作SLAB的方式来管理内存。在这一章中，你将理解这种内存分配方式，并通过源码解析以及slabtop等工具看到一条TCP状态的空连接是如何消耗内存的、消耗是多大。

# 第8章 一台机器最多能支持多少条TCP连接

在到处都在谈论高并发的今天，弄清楚一台机器最多能支持多少条TCP连接这个问题非常重要。不仅仅是服务端，在客户端最大能达到多少条TCP连接，如何突破65535个端口号的束缚创建更多连接，都将在这一章中进行讨论。此外，这一章还分析了一个实际需求，做一个支持一亿个用户的长连接推送需要多少台机器。

# 第9章 网络性能优化建议

在这一章，将讨论一些网络开发时可用的优化手段。例如RingBuffer的扩容、多队列网卡的使用、配置充足的端口范围、使用零拷贝，等等。这一章还将讨论为什么DPDK等Kernel-ByPass之类的新技术性能会很不错。

# 第10章 容器网络虚拟化

现在越来越多的公司在线上生产环境中不再将服务部署到实体物理机或者KVM虚拟机上，而是部署到基于Docker的容器云上。这就对技术人员提出了新的挑战，你需要理解容器网络工作原理。如果理解不到位，很有可能你没有能力定位线上问题，也没有能力进行性能等方面的优化。这一章深入分析容器网络中的核心技术点——veth、namespace、bridge等技术。

# 1.3 一些约定

本书所使用的Linux源码版本是3.10，之所以采用这个版本是因为写作时我们公司线上Linux主要是基于3.10的。另外，如果涉及驱动程序（简称驱动），默认采用的都是Intel的igb网卡驱动。还有就是测试环境数据结果，如无特殊说明，也是在3.10的内核版本的服务器上做的。

关于B和b，B代表的是一个Byte（字节），而b代表的是一个bit（位）。在本书中，内存开销主要使用B作为单位。

关于K和k，分别代表1024和1000，这两个差别并不大，所以本书中有些地方是混着用了。

# 1.4 一些术语

在本书的内容中，会提到不少专业术语。在这里把一些关键术语都列出来，后面再出现时可能就提一下，不详细介绍了。

- hi：CPU开销中硬中断消耗的部分。  
- si：CPU开销中软中断消耗的部分。  
- skb: skb是struct sk_buffer对象的简称。struct sk_buffer是Linux网络模块中的核心结构体，各个层用到的数据包都是存在这个结构体里的。  
- NAPI：Linux 2.5以后的内核引入的一种高效网卡数据处理的技术，先用中断唤醒内核接收数据，后续采用poll轮询从网卡设备获取数据，通过减少中断次数来提高内核处理网卡数据的效率。  
- MSI/MSIx：MSI是Message Signal Interrupt的首字母缩写，是一种触发CPU中断的方式。

# 第2章

# 内核是如何接收网络包的

![](images/45a577077775cbf79ed647587213f18535ca5ee80898dd0b7a5dd3d8d6e70f77.jpg)

# 2.1 相关实际问题

在现在的互联网世界里，所有技术岗位的人员几乎都是天天和网络请求打交道。平时我们在做网络开发的时候，如果需要接收网络数据，只需要简单的几行代码就可以搞定。如果拿C语言来举例（Java、Golang、PHP等其他语言也是类似的），一行read函数调用代码就能接收来自对端的数据。

int main(){ int sock $=$ socket(AF_INET,SOCK_STREAM,0); connect(ock，...）; read(ock，buffer,sizeof(buffer)-1); }

从开发视角来看，只要客户端有对应的数据发送过来，服务端执行read后就能收到。那你是否深入思考过，在Linux下数据是如何从网卡一步步地到达你的进程里的，这中间都需要哪几个内核组件进行协同？这个问题看起来简单，但实际上隐藏了非常多的技术点。

# 1）RingBuffer到底是什么，RingBuffer为什么会丢包？

在网络性能相关的技术文章中经常能看到RingBuffer这一关键词。RingBuffer到底存在于哪一块，是如何被用到的，真的就只是一个环形的队列吗？在有的技术文章里指出RingBuffer内存是预先分配好的，还有的则说RingBuffer里使用的内存是随着网络包的收发而动态分配的。这两个说法哪一个是正确的？为什么RingBuffer会丢包，如果丢包了的话应该怎么去解决？

# 2）网络相关的硬中断、软中断都是什么？

有人说网卡是通过硬中断来通知CPU有新包到达的，又有人说网络里面还有个软中断。那硬中断和软中断的区别是什么，二者又是怎么协作的呢？另外，在很多性能优化的技术文章中会提到网卡中断绑定，不知道你有没有思考过为什么大部分文章中提到的都是操作硬中断号和CPU之间的绑定关系，但最终的效果却是软中断跟着一起调整了，软中断开销也被绑定到不同的CPU？你想过这是为什么吗？

# 3）Linux里的ksoftirqd内核线程是干什么的？

到服务器上执行“ps -ef | grep ksoftirqd”，看是不是有几个名字叫作“ksoftirqd/*”的内核线程。我把我手头虚拟机上的结果展示一下：

```tcl
root 3 2 0 Jan04 ? 00:00:19 [ksoftirqd/0]  
root 13 2 0 Jan04 ? 00:00:47 [ksoftirqd/1]  
root 18 2 0 Jan04 ? 00:00:10 [ksoftirqd/2]  
root 23 2 0 Jan04 ? 00:00:51 [ksoftirqd/3] 
```

你知道这几个内核线程是做什么用的吗？你的机器上有几个？为什么有这么多？它们和软中断又是什么关系？

# 4）为什么网卡开启多队列能提升网络性能？

相信不少读者在关注网络性能优化时听说过用多队列网卡来提升网络性能，但是你是否清楚这一性能优化方案的基本原理是什么？理解了原理你也就能知道什么时候该动用这个方法，用的话开到几个队列合适。

# 5）tcpdump是如何工作的？

我们平时工作中经常会用到tcpdump，但你知道它是如何工作，如何和内核进行配合的吗？

# 6)iptables/netfilter是在哪一层实现的？

在网络包的收发过程中，我们可以通过iptables/netfilter配置一些规则来进行包的过滤。那么你知道它工作在内核中的哪一层吗？

# 7）tcpdump能否抓到被iptables封禁的包？

如果某些数据包被iptables封禁，是否可以通过tcpdump抓到？

# 8）网络接收过程中的CPU开销如何查看？

在网络接收过程中，CPU是如何被消耗的？CPU中的si、sy开销究竟是什么含义？

# 9）DPDK是什么神器？

老的还没学完，又有很多新技术出来了，比如DPDK究竟是什么，是否需要学习和使用它。其实，你理解不了这个新技术的根本原因可能是你对Linux内核的工作原理不清楚。当你掌握了Linux内核的网络处理过程后，回头再看DPDK这类Kernel-ByPass的技术，直接就有四五成的把握了。

可以看到，上面几个问题总体来说都和底层相关。我们为什么要了解这么底层呢？如果你负责的应用不是高并发的，流量也不大，确实没必要往下看。但是对于今天的互联网公司，由于我国人口基数大，几乎随便一个二线App都需要为百万、千万甚至上亿数量级的用户提供稳定的服务。深入理解Linux系统内部是如何实现的，以及各个部分之间是如何交互的，对你进行线上问题的处理、性能分析和优化将会有非常大的帮助。

带着这些疑问，让我们开始进入网络包接收过程的探寻之旅吧！

# 2.2 数据是如何从网卡到协议栈的

我们在应用层执行read调用后就能很方便地接收到来自网络的另一端发送过来的数据，其实在这一行代码下隐藏着非常多的内核组件细节工作。在本节中，将详细讲解

包是如何从网卡跑到协议栈的。另外说明一下，本节提及的网卡驱动以Intel的igb网卡为例，其他类型的网卡工作过程类似。

# 2.2.1 Linux网络收包总览

在TCP/IP网络分层模型里，整个协议栈被分成了物理层、链路层、网络层、传输层和应用层。应用层对应的是我们常见的Nginx、FTP等各种应用，也包括我们写的各种服务端程序。Linux内核以及网卡驱动主要实现链路层、网络层和传输层这三层上的功能，内核为更上面的应用层提供socket接口来支持用户进程访问。以Linux的视角看到的TCP/IP网络分层模型应该是图2.1这样的。

![](images/98d47568ef11dcc646faff80ba53fa003e63e47c69a4242afaad91fed67cd3fc.jpg)  
图2.1 TCP/IP网络分层模型

在Linux的源码中，网络设备驱动对应的逻辑位于driver/net/ethernet，其中Intel系列网卡的驱动在driver/net/ethernet/intel目录下，协议栈模块代码位于kernel和net目录下。

内核和网络设备驱动是通过中断的方式来处理的。当设备上有数据到达时，会给CPU的相关引脚触发一个电压变化，以通知CPU来处理数据。对于网络模块来说，由于处理过程比较复杂和耗时，如果在中断函数中完成所有的处理，将会导致中断处理函数（优先级过高）过度占用CPU，使得CPU无法响应其他设备，例如鼠标和键盘的消息。因此Linux中断处理函数是分上半部和下半部的。上半部只进行最简单的工作，快速处理然后释放CPU，接着CPU就可以允许其他中断进来。将剩下的绝大部分的工作都放到下半部，可以慢慢、从容处理。2.4以后的Linux内核版本采用的下半部实现方式是软中断，由ksoftirqd内核线程全权处理。硬中断是通过给CPU物理引脚施加电压变化实现的，而软

中断是通过给内存中的一个变量赋予二进制值以标记有软中断发生。

好了，大概了解了网卡驱动、硬中断、软中断和ksoftirqd线程之后，在这几个概念的基础上给出一个内核收包的路径示意图，如图2.2所示。

![](images/2b5bb32fad0b25dd9ab73611004a4e54c4d90b1dbb29586c273d4a3379748312.jpg)  
图2.2 内核收包路径

当网卡收到数据以后，以DMA的方式把网卡收到的帧写到内存里，再向CPU发起一个中断，以通知CPU有数据到达。当CPU收到中断请求后，会去调用网络设备驱动注册的中断处理函数。网卡的中断处理函数并不做过多工作，发出软中断请求，然后尽快释放CPU资源。ksoftirqd内核线程检测到有软中断请求到达，调用poll开始轮询收包，收到后交由各级协议栈处理。对于TCP包来说，会被放到用户socket的接收队列中。

相信读者通过图2.2已经能够从整体上把握Linux对数据包的处理过程，但是要想了解更多网络模块工作的细节，还得往下看。

# 2.2.2 Linux启动

Linux驱动、内核协议栈等模块在能够接收网卡数据包之前，要做很多的准备工作才行。比如要提前创建好ksoftirqd内核线程，要注册好各个协议对应的处理函数，网卡设备子系统要提前初始化好，网卡要启动好。只有这些都准备好后，我们才能真正开始接收数据包。那么我们现在来看看这些准备工作都是怎么做的。

# 创建ksoftirqd内核线程

Linux的软中断都是在专门的内核线程（ksoftirqd）中进行的，因此我们非常有必要看

一下这些线程是怎么初始化的，这样才能在后面更准确地了解收包过程。该线程数量不是1个，而是N个，其中N等于你的机器的核数。

系统初始化的时候在kernel/smpboot.c中调用了smpboot register percpu_thread，该函数进一步会执行到spawn_ksoftirqd（位于kernel/softirq.c）来创建出softirqd线程，执行过程如图2.3所示。

![](images/e44cb104e4011f6d914fb950d3d0bab675457ade3445dafd0b1198e1d4b02c84.jpg)  
图2.3 创建ksoftirqd

相关代码如下：

```txt
//file: kernel/softirq.c
static struct cmp-hotplug_thread softirq Threads = {
    .store     = &ksoftirqd,
    .thread_should_run = ksoftirqd_should_run,
    .thread_fn   = run_ksoftirqd,
    .thread_comm  = "ksoftirqd/%u",
}; 
```

当ksoftirqd被创建出来以后，它就会进入自己的线程循环函数ksoftirqdshould_run和run_ksoftirqd了。接下来判断有没有软中断需要处理。这里需要注意的一点是，软中断不仅有网络软中断，还有其他类型。Linux内核在interrupt.h中定义了所有的软中断类型，如下所示：

```c
//file: include/linux/interrupt.h  
enum  
{  
    HI_SOFTIRQ = 0,  
    TIMER_SOFTIRQ,  
    NET_TX_SOFTIRQ,  
    NET_RX_SOFTIRQ,  
    BLOCK_SOFTIRO,  
    BLOCK_IOPOLL_SOFTIRQ, 
```

```txt
TASKLET_SOFTIRQ,  
SCHED_SOFTIRQ,  
HRTIMER_SOFTIRQ,  
RCU_SOFTIRQ,  
NR_SOFTIRQS 
```

# 网络子系统初始化

在网络子系统的初始化过程中，会为每个CPU初始化softnet_data，也会为RX_SOFTIRQ和TX_SOFTIRQ注册处理函数，流程如图2.4所示。

![](images/0a80fab4a14d57944ef0a4f7194b5d5a09a4562882488828d43af5172c5e9a5d.jpg)  
图2.4 网络子系统初始化

Linux内核通过调用subsys_initcall来初始化各个子系统，在源代码目录里你可以用grep命令搜索出许多对这个函数的调用。这里要说的是网络子系统的初始化，会执行net_dev_init函数。

//file: net/core/dev.c   
static int __init net_dev_init(void)   
{ ....... for_each POSSIBLE_cpu(i){ struct softnet_data \*sd $=$ &per_cpu(softnet_data,i); memset(sd,0,sizeof(\*sd)); skb_queue_head_init(&sd->input_pkt_queue); skb_queue_head_init(&sd->process_queue); sd->completion_queue $=$ NULL; INIT_LIST_HEAD(&sd->poll_list);

```matlab
......   
}   
opensoftirq(NET_TX_SOFTIRQ，net_tx_action); opensoftirq(NET_RX_SOFTIRQ，net_rx_action);   
}   
subsys_initcall(net_dev_init); 
```

在这个函数里，会为每个CPU都申请一个softnet_data数据结构，这个数据结构里的poll_list用于等待驱动程序将其poll函数注册进来，稍后网卡驱动程序初始化的时候可以看到这一过程。

另外，opensoftirqq为每一种软中断都注册一个处理函数。NET_TX_SOFTIRQ的处理函数为net_tx_action，NET_RX_SOFTIRQ的处理函数为net_rx_action。继续跟踪opensoftirqq后发现这个注册的方式是记录在softirq_qvec变量里的。后面ksoftirqd线程收到软中断的时候，也会使用这个变量来找到每一种软中断对应的处理函数。

```c
//file: kernel/softirq.c  
void opensoftirq(int nr, void (*action)(struct softirq_action *))  
{  
    softirq_vec[nr].action = action;  
} 
```

# 协议栈注册

内核实现了网络层的IP协议，也实现了传输层的TCP协议和UDP协议。这些协议对应的实现函数分别是ip_rcv()、tcp_v4_rcv()和udp_rcv()。和平时写代码的方式不一样的是，内核是通过注册的方式来实现的。Linux内核中的fs_initcall和subsys_initcall类似，也是初始化模块的入口。fs_initcall调用inet_init后开始网络协议栈注册，通过inet_init，将这些函数注册到inet_protos和ctype_base数据结构中，如图2.5所示。

![](images/dee0ba36cfd2203d7a5086913c8f1c429432d13aa94c2634f679a11ce083342a.jpg)  
图2.5 协议栈注册

相关代码如下。

//file: net/ipv4/af_inet.c   
static struct packet_type ipPacket_type _readmostly $=$ { .type $=$ cpu_to_be16(ETH_P_IP), .func $=$ ip_rcv,   
};   
static const struct net_protocol udp_protocol $=$ { .handler $=$ udp_rcv, errhandler $=$ udp_err, .no_policy $=$ 1, .netns.ok $= 1$ }；   
static const struct net_protocol tcp_protocol $=$ { .early_demux $=$ tcp_v4早早_demux, .handler $=$ tcp_v4_rcv, errhandler $=$ tcp_v4_err, .no_policy $=$ 1, .netns.ok $= 1$ }；   
static int __init inet_init(void)   
{ if (inet_add_protocol(&icmp_protocol, IPPROTO_ICMP) < 0 pr crit("%s:Cannot add ICMP protocol\n",__func_) if (inet_add_protocol(&udp_protocol, IPPROTOUDP) < 0) pr crit("%s:Cannot add UDP protocol\n",__func_); if (inet_add_protocol(&tcp_protocol, IPPROTO_TCP) < 0) pr crit("%s:Cannot add TCP protocol\n",__func_); ... dev_add-pack(&ippacket_type);

从上面的代码中可以看到，udp_protocol结构体中的handler是udp_rcv，tcp_protocol结构体中的handler是tcp_v4_rcv，它们通过inet_add_protocol函数被初始化进来。

```c
//file: net/ipv4/protocol.c  
int inset_add_protocol(const struct net_protocol *prot, unsigned char protocol)  
{  
    if (!prot->netns.ok) {  
        pr_err("Protocol %u is not namespace aware, cannot register.\n", protocol);  
    return -EINVALID;  
} 
```

```c
} return！cmpxchg((const struct net_protocol \*\*)&inet_protos[protocol], NULL,prot)?0：-1;   
}
```

inet_add_protocol函数将TCP和UDP对应的处理函数都注册到inet_protos数组中了。再看“dev_add-pack(&ip_packet_type);”这一行，ip_packet_type结构体中的type是协议名，func是ip_rcv函数，它们在dev_add-pack中会被注册到ctype_base哈希表中。

```lisp
//file: net/core/dev.c
void dev_add-pack(struct packet_type *pt)
{
    struct list_head *head = htons(pt);
    ...
} 
```

这里需要记住inet.proto记录着UDP、TCP的处理函数地址，ptype_base存储着ip_rcv()函数的处理地址。后面将讲到软中断中会通过ctype_base找到ip_rcv函数地址，进而将IP包正确地送到ip_rcv()中执行。在ip_rcv中将会通过inet.proto找到TCP或者UDP的处理函数，再把包转发给udp_rcv()或tcp_v4_rcv()函数。建议大家好好读一读inet_init这个函数的代码。

扩展一下，如果看一下ip_rcv和udp_rcv等函数的代码，能看到很多协议的处理过程。例如，ip_rcv中会处理iptables netfilter过滤，udp_rcv中会判断socket接收队列是否满了，对应的相关内核参数是net.core.rmem_max和net.core.rmem_default。

# 网卡驱动初始化

每一个驱动程序（不仅仅包括网卡驱动程序）会使用module_init向内核注册一个初始化函数，当驱动程序被加载时，内核会调用这个函数。比如igb网卡驱动程序的代码位于drivers/net/ethernet/intel/igb/igb_main.c中。

```txt
//file: drivers/net/ethernet/intel/igb/igb_main.c  
static struct pci_driver igb_driver = {  
    .name = igb_driver_name, 
```

.id_table $=$ igb_pciTbl, .probe $=$ igb_probe, .remove $=$ igb_remove, }; static int __init igb_init_module(void) { .. ret $\equiv$ pciregister_driver(&igb_driver); return ret; }

驱动的pci register driver调用完成后，Linux内核就知道了该驱动的相关信息，比如igb网卡驱动的igb_driver_name和igbprobe函数地址，等等。当网卡设备被识别以后，内核会调用其驱动的probe方法（igb_driver的probe方法是igbprobe）。驱动的probe方法执行的目的就是让设备处于ready状态。对于igb网卡，其igbprobe位于drivers/net/ethernet/intel/igb/igb_main.c下。函数igbprobe主要执行的操作如图2.6所示。

![](images/0794e51f2d0712d4aff6bc5177113d3404403f1192f25cc23a2851ede81ad503.jpg)  
图2.6 网卡驱动初始化

可以看到在第5步中，网卡驱动实现了ethtool所需要的接口，也在这里完成函数地址的注册。当ethtool发起一个系统调用之后，内核会找到对应操作的回调函数。对于igb网卡来说，其实现函数都在drivers/net/ethernet/intel/igb/igb_ethertool.c下。你这次能彻底理解ethtool的工作原理了吧？这个命令之所以能查看网卡收发包统计、能修改网卡自适应模式、能调整RX队列的数量和大小，是因为ethtool命令最终调用到了网卡驱动的相应方法，而不是ethtool本身有这个超能力。

第6步注册igb_netdevOps用的是igb_netdevOps变量，其中包含igb_open等函数，该函数在网卡启动的时候会被调用。

```c
//file: drivers/net/ethernet/intel/igb/igb_main.c  
static const struct net_deviceOps igb_netdevOps = {  
    ndo_open = igb_open,  
    ndo_stop = igb_close,  
    ndo_start_xmit = igb_xmit_frame,  
    ndo_get_stats64 = igb_get_stats64,  
    ndo_set_rx_mode = igb_set_rx_mode,  
    ndo_set_mac_address = igb_set_mac,  
    ndo_change_mtu = igb_change_mtu,  
    ndo_do_ioctl1 = igb_ioctl1,.... 
```

第7步在igbprobe初始化过程中，还调用到了igb_alloc_q_vector。它注册了一个NAPI机制必需的poll函数，对于igb网卡驱动来说，这个函数就是igb_poll，代码如下所示。

```c
//file:drivers/net/ethernet/intel/igb/igb_main.c  
static int igb_alloc_q_vector(...){  
    .......  
    /* initialize NAPI */  
    netif_napi_add(adapter->netdev, &q_vector->napi, igb_poll, 64);  
} 
```

# 启动网卡

当上面的初始化都完成以后，就可以启动网卡了。回忆前面网卡驱动初始化时，曾提到了驱动向内核注册了structure net_deviceOps变量，它包含着网卡启用、发包、设置MAC地址等回调函数（函数指针）。当启用一个网卡时（例如，通过ifconfig eth0 up），net_deviceOps变量中定义的ndo_open方法会被调用。这是一个函数指针，对于igb网卡来说，该指针指向的是igb_open方法。它通常会做如图2.7所示的事情。

![](images/cd611ef083422b40cd8c643e8f9d40abb729f63fe0712a97862c09cecee614e2.jpg)  
图2.7 启动网卡的过程

下面来看看源码。

```c
//file: drivers/net/ethernet/intel/igh/igh_main.c  
static int __igb_open(struct net_device *netdev, bool resuming) 
```

{

//分配传输描述符数组

```txt
err = igb_setup_all_tx-resources(adapter); 
```

//分配接收描述符数组

```txt
err = igb_setup_all_rx-resources(adapter); 
```

//注册中断处理函数

```txt
err = igb_request_irq(adapter);  
if (err)  
    goto err_req_irq; 
```

//启用NAPI

for $(i = 0$ ; i $<$ adapter->num_q_vectors; i++) napi_enable(&(adapter->q_vector[i]->napi));

以上代码中，_igb_open函数调用了igb_setup_all_tx-resources和igb_setup_all_rx-resources。在调用igb_setup_all_tx-resources这一步操作中，分配了RingBuffer，并建立内存和Rx队列的映射关系。（Rx和Tx队列的数量和大小可以通过ethtool进行配置。）

//file:drivers/net/ethernet/intel/igb/igb_main.c   
static int igb_setup_all_rxResources(struct igb_adapter \*adapter)   
{ for $(\mathrm{i} = 0$ .i $<$ adapter->num_rx_queue; $\mathrm{i + + })$ { err $=$ igb_setup_rxResources(adapter->rx_ring[i]); } return err;

在上面的源码中，通过循环创建了若干个接收队列，如图2.8所示。

![](images/b879c569c7d00ad3863eb0bcb634277b4f6706b198265095422e0db2f48c6362.jpg)  
图2.8 接收队列

再来看看每一个队列是如何创建出来的。

```txt
//file:drivers/net/ethernet/intel/igb/igb_main.c  
int igb_setup_txResources(struct igb_ring *tx_ring) { 
```

//1. 申请igb_rx_buffer数组内存

```c
size = sizeof(struct igb_rx_buffer) * rx-ring->count;  
rx-ring->rx_buffer_info = vzalloc(size); 
```

//2. 申请e1000_adv_rx_desc DMA数组内存

```c
rx_ring->size = rx_ring->count * sizeof.union e1000_adv_rx_desc);  
rx_ring->size = ALIGNN(rx_ring->size, 4096);  
rx_ring->desc = dma_alloc_coherent(dev, rx_ring->size, &rx_ring->dma, GFP_KERNEL); 
```

//3. 初始化队列成员

rx_ring->next_to_alloc $= 0$ .   
rx_ring->next_to_clean $= 0$ .   
rx_ring->next_to_use $= 0$ .   
return 0;   
}

从上述源码可以看到，实际上一个RingBuffer的内部不是仅有一个环形队列数组，而是有两个，如图2.9所示。

1）igb_rx_buffer数组：这个数组是内核使用的，通过vzalloc申请的。  
2）e1000 advant_rx_desc数组：这个数组是网卡硬件使用的，通过dma_alloc_coherent分配。

![](images/c00f46b8f7c54ca57227dfc1e9747770cf0ff00d4f518b22526a4f06e755dd7f.jpg)  
图2.9 接收队列内部

再接着看中断函数是如何注册的，注册过程见igb_request_irq。

//file:drivers/net/ethernet/intel/igb/igb_main.c   
static int igb_request_irq(struct igb_adapter \*adapter)   
{ if (adapter->msix_entries）{ err $=$ igb_requesti_msix(adapter); if(!err)

goto request_done;   
}   
}   
static int igb_request_msix(struct igb_adapter \*adapter)   
{ for $(\mathbf{i} = \emptyset ;\mathbf{i} <$ adapter->num_qVectors; i++) { err $=$ request_irq(adapter->msix_entries[vector].vector, igb_msix_ring,0,q_vector->name,

在上面的代码中跟踪函数调用，调用顺序为__igb_open => igb_request_irq => igb_request_msix。在igb_request_msix中可以看到，对于多队列的网卡，为每一个队列都注册了中断，其对应的中断处理函数是igb_msix_ring（该函数也在drivers/net/ethernet/intel/igb/igb_main.c下）。还可以看到，在msix方式下，每个RX队列有独立的MSI-X中断，从网卡硬件中断的层面就可以设置让收到的包被不同的CPU处理。（可以通过irqbalance，或者修改 /proc/irq/IRQ_NUMBER/smp-affinity，从而修改和CPU的绑定行为。）

当做好以上准备工作以后，就可以开门迎客（接收数据包）了！

# 2.2.3 迎接数据的到来

# 硬中断处理

首先，当数据帧从网线到达网卡上的时候，第一站是网卡的接收队列。网卡在分配给自己的RingBuffer中寻找可用的内存位置，找到后DMA引擎会把数据DMA到网卡之前关联的内存里，到这个时候CPU都是无感的。当DMA操作完成以后，网卡会向CPU发起一个硬中断，通知CPU有数据到达。硬中断的处理过程如图2.10所示。

![](images/abc3af0b849d2dc7d7fc00f092c347bf7149e1c9eae9e3da5ba780371c2e3c1c.jpg)  
图2.10 硬中断处理

# ★注意

当RingBuffer满的时候，新来的数据包将被丢弃。使用ifconfig命令查看网卡的时候，可以看到里面有个overruns，表示因为环形队列满被丢弃的包数。如果发现有丢包，可能需要通过ethtool命令来加大环形队列的长度。

在前面的“启动网卡”部分，讲到了网卡的硬中断注册的处理函数是igb_msix_ring。

//file: drivers/net/ethernet/intel/igb/igb_main.c   
staticirqreturn_tigb_msix_ring(intirq，void\*data)   
{ struct igb_q_vector \*q_vector $=$ data; /\*Write the ITR value calculated from the previous interrupt.\*/ igb_write_itr(q_vector); napi_schedule(&q_vector->napi); return IRQ_HANDLED;   
}

其中的igb_write_iitr只记录硬件中断频率（据说是在减少对CPU的中断频率时用到）。顺着napi_schedule调用一路跟踪下去，调用顺序为_napi_schedule $\Longrightarrow$ __napi_schedule。

```c
//file: net/core/dev.c  
static inline void __napi_schedule(struct softnet_data *sd, struct napi_struct *napi)  
{  
    list_addTAIL(&napi->poll_list, &sd->poll_list);  
    __raisesoftirq_irqfNET_RX_SOFTIRQ);  
} 
```

这里可以看到，list_addTAIL修改了CPU变量softnet_data里的poll_list，将驱动napi_struct传过来的poll_list添加了进来。softnet_data中的poll_list是一个双向列表，其中的设备都带有输入帧等着被处理。紧接着__raisesoftirq_irqoff触发了一个软中断NET_RX_SOFTIRQ，这个所谓的触发过程只是对一个变量进行了一次或运算而已。

```c
//file:kernel/softirq.c  
void __raisesoftirq_irqoff(unsigned int nr)  
{ tracesoftirqaleza(r); orsoftirq pending(1UL << nr); } //file: include/linux/interrupt.h 
```

define orsoftirqpending(x) (localsoftirqpending() $\mid =$ （x))

```txt
//file: include/linux/irq_cpusstat.h
#define localsoftirq_pending()
    __IRQ_STAT(smpprocessor_id(), __softirq_pending) 
```

之前讲过，Linux在硬中断里只完成简单必要的工作，剩下的大部分的处理都是转交给软中断的。通过以上代码可以看到，硬中断处理过程真的非常短，只是记录了一个寄存器，修改了一下CPU的poll_list，然后发出一个软中断。就这么简单，硬中断的工作就算是完成了。

# ksoftirqd内核线程处理软中断

网络包的接收处理过程主要都在ksoftirqd内核线程中完成，软中断都是在这里处理的，流程如图2.11所示。

![](images/4159ebd60a73131977b6f1b6acecb97152b7f3a94b4ba4a4d8a42d23ae6ba127.jpg)  
图2.11 软中断处理

前文介绍内核线程初始化的时候，曾介绍了ksoftirqd中两个线程函数ksoftirqdshould_run和run_ksoftirqd。其中ksoftirqd.should_run函数的代码如下：

```c
//file: kernel/softirq.c  
static int ksoftirqdshould_run(unsigned int cpu)  
{ return localsoftirq pending();  
}  
#define localsoftirqpending() \ IRQ_STAT(smp Processor_id(), softirq pending) 
```

从以上代码可以看到，此函数和硬中断中调用了同一个函数localsoftirq_pending。使用方式的不同之处在于，在硬中断处理中是为了写入标记，这里只是读取。如果硬中

断中设置了NET_RX_SOFTIRQ，这里自然能读取到。接下来会真正进入内核线程处理函数run_ksoftirqd进行处理：

```txt
//file: kernel/softirq.c   
static void run_ksoftirqd(unsigned int cpu) { local_irq_disable(); if(localsoftirq_pending()){ _dosoftirq(); } local_irq_enable(); 
```

在_dosoftirq中，判断根据当前CPU的软中断类型，调用其注册的action方法。

```txt
asmlinkage void __do_softirq(void)  
{  
    do {  
        if (pending & 1) {  
            unsigned int vec_NR = h - softirq_vec;  
            int prev_count = preempt_count();  
            ...  
            trace_softirq_entry vec_NR);  
            h->action(h);  
            trace_softirq_exit vec_NR);  
            ...  
        }  
    h++;  
    pending >= 1;  
} while (pending); 
```

这里需要注意一个细节，硬中断中的设置软中断标记，和ksoftirqd中的判断是否有软中断到达，都是基于smp Processor_id()的。这意味着只要硬中断在哪个CPU上被响应，那么软中断也是在这个CPU上处理的。所以说，如果你发现Linux软中断的CPU消耗都集中在一个核上，正确的做法应该是调整硬中断的CPU亲和性，将硬中断打散到不同的CPU核上去。看到这里大家也就弄清楚了本章开篇处提到的第二个疑惑。

我们再来把精力集中到这个核心函数net_rx_action上来。

//file:net/core/dev.c   
static void net_rx_action(struct softirq_action \*h)   
{ struct softnet_data \*sd $=$ &_get_cpu_var(softnet_data); unsigned long time_limit $=$ jiffies + 2;

int budget $=$ netdev-budget;   
void \*have;   
local_irq_disable();   
while(!list_empty(&sd->poll_list)) { n $=$ list_first_entry(&sd->poll_list,struct napi_struct,poll_list); work $= 0$ if(test_bit(NAPI_STATE_SCHED，&n->state)){ work $=$ n->poll(n,weight); trace_napi_poll(n); } budget $= =$ work;net_rx_action

有人问在硬中断中将设备添加到poll_list，会不会重复添加呢？答案是不会的，在软中断处理函数net_rx_action这里，一进来就调用local_irq_disable把所有的硬中断都关了，不会给硬中断重复添加poll_list的机会。硬中断的处理函数本身也有类似的判断机制，打磨了几十年的内核在细节考虑上还是很完善的。

函数开头的time_limit和budget是用来控制net_rx_action函数主动退出的，目的是保证网络包的接收不霸占CPU不放，等下次网卡再有硬中断过来的时候再处理剩下的接收数据包。其中budget可以通过内核参数调整。这个函数中剩下的核心逻辑是获取当前CPU变量softnet_data，对其poll_list进行遍历，然后执行到网卡驱动注册到的poll函数。对于igb网卡来说，就是igb驱动里的igb POLL函数。

//file: drivers/net/ethernet/intel/igb/igb_main.c   
```c
static int igb_poll(struct napi_struct *napi, int budget)  
{  
    ...  
    if (q_vector->tx.ring)  
        clean_COMPLETE = igb_clean_tx_irq(q_vector);  
    if (q_vector->rx.ring)  
        clean_COMPLETE &= igb.clean_rx_irq(q_vector, budget);  
    ...  
} 
```

在读取操作中，igb_poll的重点工作是对igb_clean_rx_irq的调用。

```c
//file: drivers/net/ethernet/intel/igb/igb_main.c  
static bool igb_clean_rx_irq(struct igb_q_vector *q_vector, const int budget) { 
```

do{ /\* retrieve a buffer from the ring \*/ skb $=$ igb_fetch_rx_buffer(rx-ring,rx_desc,skb); /\* fetch next buffer in frame if non-eop \*/ if (igb_is_non_eop(rx-ring,rx_desc)) continue; } /\* verify the packet layout is correct \*/ if (igb_cleanupheaders(rx-ring,rx_desc,skb)){ skb $=$ NULL; continue; } /\* populate checksum,timestamp,VLAN,and protocol \*/ igb_process_skb_fields(rx-ring,rx_desc,skb); napi_gro_receive(&q_vector->napi,skb);

igb_fetch_rx_buffer和igb_is_non_eop的作用就是把数据帧从RingBuffer取下来。

skb被从RingBuffer取下来以后，会通过igb_alloc_rxBuffers申请新的skb再重新挂上去。所以不要担心后面新包到来的时候没有skb可用。

为什么需要两个函数呢？因为有可能数据帧要占多个RingBuffer，所以是在一个循环中获取的，直到帧尾部。获取的一个数据帧用一个sk_buffer来表示。收取完数据后，对其进行一些校验，然后开始设置sbk变量的timestamp、VLAN id、protocol等字段。接下来进入napi_gro_receive函数。

```c
//file: net/core/dev.c  
gro_result_t napi_gro_receive(struct napi_struct *napi, struct sk BUFF *skb)  
{  
    skb_gro_reset_offset(skb);  
    return napi_skb_finish(dev_gro_receive(napi, skb), skb);  
} 
```

dev_gro_receive这个函数代表的是网卡GRO特性，可以简单理解成把相关的小包合并成一个大包，目的是减少传送给网络栈的包数，这有助于减少对CPU的使用量。暂且忽略这些，直接看napi Skyl_finish，这个函数主要就是调用了netif_receive Skyl。

```c
//file: net/core/dev.c  
static gro_result_t napi_skb_finish(gro_result_t ret, struct sk BUFF *skb)  
{  
    switch (ret) { 
```

case GRO_NORMAL: if (netif_receive_skb(skb)) ret $=$ GRO_DROP; break;

在netifreceive_skb中，数据包将被送到协议栈中。

# 网络协议栈处理

netif_receive_skb函数会根据包的协议进行处理，假如是UDP包，将包依次送到ip_rcv、udp_rcv等协议处理函数中进行处理，如图2.12所示。

![](images/9856c32ec0f00ad59781d5a42fca395b3297cf847fb5c39fbc10d713f6d49f0e.jpg)  
图2.12 网络协议栈处理

```c
//file: net/core/dev.c  
int netif_receive_SKb(struct sk BUFF *skb)  
{ // RPS处理逻辑，先忽略 return __netif_receive_SKb(skb); }  
static int __netif_receive_SKb(struct skbuff *skb) { ret = __netif_receive_SKb_core(skb, false); }  
static int __netif_receive_SKb_core(struct skbuff *skb, bool pfmemalloc) { // pcap逻辑，这里会将数据送入抓包点。tcpdump就是从这个入口获取包的 list_for_each_entry_rcu(ctype, &ctype_all, list) { if (!ctype->dev ||ctype->dev == skb->dev) { if (pt prev) ret = deliver_SKb(skb, ptprev, orig_dev); pt prev = ptype; 
```

```txt
}   
}   
.... list_for_each_entry_rcu(type, &ptype_base[ntohs(type) & PTYPE_HASH_MASK], list) { if (ptype->type == type && (ptype->dev == null_or_dev ||机型->dev == skb->dev ||机型->dev == orig_dev)) { if (pt prevail) ret = deliver_skb(skb, pt prevail, orig_dev); pt prevail =机型; }   
} 
```

在__netif_receive_skb_core中，我看到了原来经常使用的tcpdump命令的抓包点。tcpdump是通过虚拟协议的方式工作的，它会将抓包函数以协议的形式挂到pty_all上。设备层遍历所有的“协议”，这样就能抓到数据包来供我们查看了。tcpdump会执行到packet_create。

//file: net/packet/afPacket.c   
static int packet_create(struct net \*net, struct socket \*sock, ...)   
{ . po->prot hook FUNC $=$ packet_rcv; if (sock->type $= =$ SOCKPACKET) po->prot hook FUNC $=$ packet_rcv_spkt; po->prot hook.af_packet PRIV $=$ sk; register_prot-hook(sk);   
}

registerprot hook函数会把tcpdump用到的“协议”挂到ptype_all上。我看到这里很是激动，看来读一遍源码的时间真的没白费。

接着__netif_receive_skb_core函数取出protocol，它会从数据包中取出协议信息，然后遍历注册在这个协议上的回调函数列表。subtype_base是一个哈希表，在前面的“协议栈注册”部分提到过。ip_rcv函数地址就是存在这个哈希表中的。

```c
//file: net/core/dev.c   
static inline int deliver_skb(struct sk BUFF \*skb, struct packet_type \*pt prevail, struct net_device \*orig_dev)   
{ return pt prevail->func(skb, skb->dev, pt prevail, orig_dev); 
```

pt prevail->func这一行就调用到了协议层注册的处理函数。对于IP包来讲，就会进入ip_rcv（如果是ARP包，会进入arp_rcv）。

# IP层处理

再来看看Linux在IP层都做了什么，包又是怎样进一步被送到UDP或TCP处理函数中的。下面是IP层接收网络包的主入口ip_rcv。

```c
//file: net/ipv4/ip_input.c  
int ip_rcv(struct sk BUFF *skb, ...)  
{ return NF_HOOK(NFPROTO_IPV4, NF_INET_PRE_ROUTING, skb, dev, NULL, ip_rcv_finish); } 
```

这里的NF_HOOK是一个钩子函数，它就是我们日常工作中经常用到的iptables netfilter过滤。如果你有很多或者很复杂的netfilter规则，会在这里消耗过多的CPU资源，加大网络延迟。另外，使用NF_HOOK在源码中搜索可以搜到很多filter的过滤点，想深入研究netfilter可以从搜索NF_HOOK的这些引用处入手。通过搜索结果可以看到，主要是在IP、ARP等层实现的。

```txt
grep -r "NF_HOOK" \*   
net/ipv4/arp.c: NF_HOOK(NFPROTOARP, NF_ARP_OUT, skb, NULL, skb->dev, dev_queue_xmit);   
net/ipv4/arp.c: return NF_HOOK(NFPROTOARP, NF_ARP_IN, skb, dev, NULL, arp_process);   
net/ipv4/ip_input.c: return NF_HOOK(NFPROTO_IPV4, NF_INET_LOCAL_IN, skb, skb->dev, NULL,   
net/ipv4/ip_input.c: return NF_HOOK(NFPROTO_IPV4, NF_INET_PRE_ROUTING, skb, dev, NULL,   
net/ipv4/ip_forward.c: return NF_HOOK(NFPROTO_IPV4, NF_INET_FORWARD, skb, skb->dev,   
net/ipv4/xfrm4_output.c: return NF_HOOK_COND(NFPROTO_IPV4, NF_INET_POST ROUTING, skb,   
net/ipv4/ip_output.c: NF_HOOK(NFPROTO_IPV4, NF_INET POST ROUTING,   
net/ipv4/ip_output.c: NF_HOOK(NFPROTO_IPV4, NF_INET_POST ROUTING,   
NET/IPV4/IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IPV4IP 
```

当执行完注册的钩子后就会执行到最后一个参数指向的函数ip_rcv_finish。

static int ip_rcv_finish(struct sk BUFF *skb)   
{ if(!skb.dst(skb)){ int err $=$ ip-route_input_noref(skb,iph->daddr,iph->saddr, iph->tos,skb->dev); } return dst_input(skb);

跟踪ip route_input_noref后看到它又调用了ip route_input_mc。在ip route_input_mc中，函数ip_localdeliver被赋值给了dst-input。

//file: net/ipv4/route.c   
```c
static int ip-route_input_mc(struct sk_buffer *skb, __be32 daddr, __be32 saddR, u8 tos, struct net_device *dev, int our)  
{ if (our) { rth->dst_input = ip_localdeliver; rth->rt_flags |= RTCF_LOCAL; } } 
```

所以回到ip_rcv_finish中的return dst_input(skb)。

//file: include/net/dst.h   
```txt
static inline int dst_input(struct sk_buffer *skb)  
{ return skb.dst(skb) ->input(skb); } skb.dst(skb) ->input调用的input方法就是路由子系统赋的ip_localdeliver。
```

//file: net/ipv4/ip_input.c   
```c
int ip_localdeliver(struct sk_buffer *skb)   
{ if(ip_is_structure(ip_hdr(skb))）{ if(ip_defrag(skb,IP_DEFRAG_LOCAL_DELIVER)) return 0; } return NF Hook(NFPROTO_TPV4,NF_INET_LOCAL_IN,skb,skb->dev,NULL, ip_localdeliver_finish);
```

} //file: net/ipv4/ip_input.c   
static int ip_local Deliver_finish(struct sk BUFF \*skb)   
{ intprotocol $=$ ip hdr(skb)->protocol; const struct net_protocol \*ipprot; ipprot $=$ rcu_dereference(inet_protos[protocol]); if (ipprot != NULL){ ret $=$ IPProt->handler(skb); }

如“协议栈注册”部分所讲，inet_protos中保存着tcp_v4_rcv和udp_rcv的函数地址。这里将会根据包中的协议类型选择分发，在这里skb包将会进一步被派送到更上层的协议中，UDP和TCP。

# 2.2.4 收包小结

网络模块是Linux内核中最复杂的模块了，看起来一个简简单单的收包过程就涉及许多内核组件之间的交互，如网卡驱动、协议栈、内核ksoftirqd线程等。看起来很复杂，本节想通过源码 + 图示的方式，尽量以容易理解的方式来将内核收包过程讲清楚。现在让我们再串一串整个收包过程。

当用户执行完recvfrom调用后，用户进程就通过系统调用进行到内核态工作了。如果接收队列没有数据，进程就进入睡眠状态被操作系统挂起。这块相对比较简单，剩下大部分的“戏份”都是由Linux内核其他模块来“表演”了。

首先在开始收包之前，Linux要做许多的准备工作：

创建ksoftirqd线程，为它设置好它自己的线程函数，后面指望着它来处理软中断呢。  
协议栈注册，Linux要实现许多协议，比如ARP、ICMP、IP、UDP和TCP，每一个协议都会将自己的处理函数注册一下，方便包来了迅速找到对应的处理函数。  
- 网卡驱动初始化，每个驱动都有一个初始化函数，内核会让驱动也初始化一下。在这个初始化过程中，把自己的DMA准备好，把NAPI的poll函数地址告诉内核。  
- 启动网卡，分配RX、TX队列，注册中断对应的处理函数。

以上是内核准备收包之前的重要工作，当上面这些都准备好之后，就可以打开硬中断，等待数据包的到来了。

当数据到来以后，第一个迎接它的是网卡：

- 网卡将数据帧DMA到内存的RingBuffer中，然后向CPU发起中断通知。

CPU响应中断请求，调用网卡启动时注册的中断处理函数。  
中断处理函数几乎没干什么，只发起了软中断请求。  
- 内核线程ksoftirqd发现有软中断请求到来，先关闭硬中断。  
ksoftirqd线程开始调用驱动的poll函数收包。  
- poll函数将收到的包送到协议栈注册的ip_rcv函数中。  
ip_rcv函数将包送到udp_rcv函数中（对于TCP包是送到tcp_rcv_v4）。

# 2.3 本章总结

本章讲述了网络包是如何一步一步地从网卡、RingBuffer最后到接收缓存区中的，然后内核又是如何进一步处理把它送到协议栈的。理解了之后，我们回顾一下本章开篇提到的几个问题。

# 1）RingBuffer到底是什么，RingBuffer为什么会丢包？

RingBuffer是内存中的一块特殊区域，平时所说的环形队列其实是笼统的说法。事实上这个数据结构包括igb_rx_buffer环形队列数组、e1000 advant_rx_desc环形队列数组及众多的skb，参见图2.9。

网卡在收到数据的时候以DMA的方式将包写到RingBuffer中。软中断收包的时候来这里把skb取走，并申请新的skb重新挂上去。有些网上的技术文章讲到RingBuffer内存是预先分配好的，有的文章则认为RingBuffer里使用的内存是随着网络包的收发而动态分配的。这两个说法之所以看起来有点混乱，是因为没有说清楚是指针数组还是skb。指针数组是预先分配好的，而skb会随着收包过程而动态申请。

这个RingBuffer是有大小和长度限制的，长度可以通过ethtool工具查看。

```txt
#ethtool -g eth0  
Ring parameters for eth0:  
Pre-set maximums:  
RX: 4096  
RX Mini: 0  
RX Jumbo: 0  
TX: 4096  
Current hardware settings:  
RX: 512  
RX Mini: 0  
RX Jumbo: 0  
TX: 512 
```

Pre-set maximums指的是RingBuffer的最大值，Current hardware settings指的是当前的设置。从上面代码中可以看到我的网卡设置RingBuffer最大允许值为4096，目前的实际设置是512。

如果内核处理得不及时导致RingBuffer满了，那后面新来的数据包就会被丢弃，通过ethtool或ifconfig工具可以查看是否有RingBuffer溢出发生。

ethtool -S eth0

···

rx_fifo Errors: 0

tx_fifo.errors: 0

rx_fifo Errors如果不为0的话（在ifconfig中体现为overruns指标增长），就表示有包因为RingBuffer装不下而被丢弃了。那么怎么解决这个问题呢？很自然，首先我们想到的是加大RingBuffer这个“中转仓库”的大小。通过eth tool就可以修改。

ethtoo1-G eth1 rx 4096 tx 4096

这样网卡会被分配更大一点的“中转站”，可以解决偶发的瞬时的丢包。不过这种方法有个小副作用，那就是排队的包过多会增加处理网络包的延时。所以另外一种解决思路更好，那就是让内核处理网络包的速度更快一些，而不是让网络包傻傻地在RingBuffer中排队。怎么加快内核消费RingBuffer中任务的速度呢，接下来的内容会提到。

# 2）网络相关的硬中断、软中断都是什么？

在网卡将数据放到RingBuffer中后，接着就发起硬中断，通知CPU进行处理。不过在硬中断的上下文里做的工作很少，将传过来的poll_list添加到了CPU变量softnet_data的poll_list里（softnet_data中的poll_list是一个双向列表，其中的设备都带有输入帧等着被处理），接着触发软中断NET_RX_SOFTIRQ。

在软中断中对softnet_data的设备列表poll_list进行遍历，执行网卡驱动提供的poll来收取网络包。处理完后会送到协议栈的ip_rcv、udp_rcv、tcp_rcv_v4等函数中。

# 3）Linux里的ksoftirqd内核线程是干什么的？

在飞哥手头的一台四核的虚拟机上有四个ksoftirqd内核线程。是的没错，机器上有几个核，内核就会创建几个ksoftirqd线程出来。

<table><tr><td>root</td><td>3</td><td>2</td><td>0 Jan04 ?</td><td>00:00:19 [ksoftirqd/0]</td></tr><tr><td>root</td><td>13</td><td>2</td><td>0 Jan04 ?</td><td>00:00:47 [ksoftirqd/1]</td></tr><tr><td>root</td><td>18</td><td>2</td><td>0 Jan04 ?</td><td>00:00:10 [ksoftirqd/2]</td></tr><tr><td>root</td><td>23</td><td>2</td><td>0 Jan04 ?</td><td>00:00:51 [ksoftirqd/3]</td></tr></table>

内核线程ksoftirqd包含了所有的软中断处理逻辑，当然也包括这里提到的NET_RX_SOFTIRQ。在_dosoftirq中根据软中断的类型，执行不同的处理函数。对于软中断NET_RX_SOFTIRQ来说是net_rx_action函数。

```txt
//file: kernel/softirq.c  
asmlinkage void __dosoftirq(void)  
do { 
```

if (pending & 1) { unsigned int vec_NR = h - softirq_vec; int prev_count = preempt_count(); ... tracesoftirq_entry vec_NR); h->action(h); tracesoftirq_exit vec_NR); } h++; pending $\gg = 1$ . } while (pending);

可见，软中断是在ksoftirqd内核线程中执行的。软中断的信息可以从/proc/softirqs读取。

```txt
$ cat /proc/softirqs
CPU0 CPU1 CPU2 CPU3
HI: 0 2 2 0
TIMER: 704301348 1013086839 831487473 2202821058
NET_TX: 33628 31329 32891 105243
NET_RX: 418082154 2418421545 429443219 1504510793
BLOCK: 37 0 0 25728280
BLOCK_IOPOLL: 0 0 0 0
TASKLET: 271783 273780 276790 341003
SCHED: 1544746947 1374552718 1287098690 2221303707
HRTIMER: 0 0 0 0
RCU: 3200539884 3336543147 3228730912 3584743459 
```

这里显示了每一个CPU上执行的各种类型的软中断的次数。拿CPU0来举例，执行了418082 154次NET_RX、33628次NET_TX。至于为什么NET_RX比NET_TX高这么多，将在第4章讲解。

# 4）为什么网卡开启多队列能提升网络性能？

在讲这个之前，先讲一下多队列网卡。现在的主流网卡基本上都是支持多队列的，通过ethtool可以查看当前网卡的多队列情况。拿我手头的一台物理实机来举例。

```txt
ethtool -1 eth0  
Channel parameters for eth0:  
Pre-set maximums:  
RX: 0  
TX: 0  
Other: 1  
Combined: 63  
Current hardware settings: 
```

```txt
RX: 0  
TX: 0  
Other:  
Combined: 
```

上述结果表示当前网卡支持的最大队列数是63，当前开启的队列数是8。通过sysfs伪文件系统也可以看到真正生效的队列数。

```txt
ls /sys/class/net/eth0/queues  
rx-0 rx-1 rx-2 rx-3 rx-4 rx-5 rx-6 rx-7  
tx-0 tx-1 tx-2 tx-3 tx-4 tx-5 tx-6 tx-7 
```

如果想加大队列数，ethtool工具可以搞定。

```batch
ethtool -L eth0 combined 32 
```

通过/proc/interrupts可以看到该队列对应的硬件中断号（由于32核的实机展示起来太多了，所以下面的结果中删掉了不少CPU列的数据）。

cat /proc/interrupts   
```txt
CPU0 CPU1 CPU... CPU31   
52: 3172 0 0 IR-PCI-MSI-edge eth0-TxRx-0   
53: 527 0 0 IR-PCI-MSI-edge eth0-TxRx-1   
54: 577 0 0 IR-PCI-MSI-edge eth0-TxRx-2   
55: 31 0 0 IR-PCI-MSI-edge eth0-TxRx-3   
56: 33 0 0 IR-PCI-MSI-edge eth0-TxRx-4   
57: 21 0 0 IR-PCI-MSI-edge eth0-TxRx-5   
58: 21 0 0 IR-PCI-MSI-edge eth0-TxRx-6   
59: 23 0 0 IR-PCI-MSI-edge eth0-TxRx-7 
```

以上内容显示网卡输入队列eth0-TxRx-0的中断号是52，eth0-TxRx-1的中断号是53，总共开启了8个接收队列。

通过该中断号对应的smpAffinity可以查看到亲和的CPU核是哪一个。

```txt
#cat /proc/irq/53/smpAffinity 8 
```

这个亲和性是通过二进制中的比特位来标记的。例如8是二进制的1000，第4位为1，代表的就是第4个CPU核心——CPU3。

从以上内容可知，每个队列都会有独立的、不同的中断号。所以不同的队列在将数据收取到自己的RingBuffer后，可以分别向不同的CPU发起硬中断通知。而在硬中断的处理中，有一个不起眼但是特别重要的小细节，调用__raisesoftirq_irqoff发起软中断的时候，是基于当前CPU核smp Processor_id（localsoftirq_pending）的。

```txt
//__raise_softirq_irqoff => or_softirq_pending => local_softirq_pending 
```

```cpp
//file: include/linux/irq_cpstat.h
#define localsoftirq_pending()
    __IRQ_STAT(smp Processor_id(), __softirq_pending) 
```

这意味着哪个核响应的硬中断，那么该硬中断发起的软中断任务就必然由这个核来处理。

所以在工作实践中，如果网络包的接收频率高而导致个别核si偏高，那么通过加大网卡队列数，并设置每个队列中断号上的smp-affinity，将各个队列的硬中断打散到不同的CPU上就行了。这样硬中断后面的软中断CPU开销也将由多个核来分担。

# 5）tcpdump是如何工作的？

tcpdump工作在设备层，是通过虚拟协议的方式工作的。它通过调用packet_create将抓包函数以协议的形式挂到ptype_all上。

当收包的时候，驱动中实现的igb POLL函数最终会调用到__netif_receive_skb_core，这个函数会在将包送到协议栈函数（ip_rcv、arp_rcv等）之前，将包先送到type_all抓包点。我们平时工作中经常会用到的tcpdump就是基于这些抓包点来工作的。

这次你知道 tcpdump是如何和内核进行配合的了吧！

# 6)iptables/netfilter是在哪一层实现的？

netfilter主要是在IP、ARP等层实现的。可以通过搜索对NFHOOK函数的引用来深入了解 netfilter的实现。如果配置过于复杂的规则，则会消耗过多的CPU，加大网络延迟。

# 7）tcpdump能否抓到被iptables封禁的包？

通过本章的深入分析可以得知，tcpdump工作在设备层，将包送到IP层以前就能处理。而netfilter工作在IP、ARP等层。从图2.13收包流程处理顺序上来看，netfilter是在tcpdump后面工作的，所以iptables封禁规则影响不到tcpdump的抓包。

![](images/1c3507e8bff4829dfc3fa8cc7d33aaa898a00821384e5fed67fb81515f4facfe.jpg)  
图2.13 收包工作过程

不过发包过程恰恰相反，发包的时候，netfilter在协议层就被过滤掉了，所以topdump什么也看不到，如图2.14所示。

![](images/a4438344dfd0354a8987bbb6db9b611c46e8b263aa46edd7dacf602874cb276a.jpg)  
图2.14 发包工作过程

# 8）网络接收过程中的CPU开销如何查看？

在网络包的接收处理过程中，主要工作集中在硬中断和软中断上，二者的消耗都可以通过top命令来查看。

top

top - 13:22:55 up 403 days, 19:31, 4 users, load average: 0.00, 0.01, 0.05

Tasks: 435 total, 1 running, 434 sleeping, 0 stopped, 0 zombie

%Cpu0 : 0.0 us, 0.3 sy, 0.3 ni, 99.3 id, 0.0 wa, 0.0 hi, 0.0 si, 0.0 st

%Cpu1 : 0.0 us, 0.0 sy, 0.3 ni, 99.7 id, 0.0 wa, 0.0 hi, 0.0 si, 0.0 st

··

%Cpu31 : 0.0 us, 0.0 sy, 0.0 ni,100.0 id, 0.0 wa, 0.0 hi, 0.0 si, 0.0 st

其中hi是CPU处理硬中断的开销，si是处理软中断的开销，都是以百分比的形式来展示的。

另外这里多说一下，如果发现某个核的si过高，那么很有可能你的业务上当前数据包的接收已经非常频繁了，需要通过上面说的多队列网卡配置来让其他核参与进来，分担这个核接收包的内核工作量。

# 9）DPDK是什么神器？

通过前面的内容可以看到，对于数据包的接收，内核需要进行非常复杂的工作。而且在数据接收完之后，还需要将数据复制到用户空间的内存中。如果用户进程当前是阻塞的，还需要唤醒它，又是一次上下文切换的开销。

那么有没有办法让用户进程能绕开内核协议栈，自己直接从网卡接收数据呢？如果这样可行，那繁杂的内核协议栈处理、内核态到用户态内存拷贝开销、唤醒用户进程开销等就可以省掉了。确实有，DPDK就是其中的一种。很多时候对新技术不够了解，原因是对老技术没有真正理解透彻。

在本章中，详细分析了网络包是如何从网卡中一步一步地达到内核协议栈的。通过对源码的详细了解，我们也彻底弄清楚了多任务队列网卡、RingBuffer、硬中断、软中断等概念，也明白了该如何查看内核在接收网络包时的CPU开销。

理解了这些以后，你将能得到一幅“地图”。在这张“地图”上你能找到之前听说过的各个技术点的正确位置，例如tcpdump、netfilter等。有了它，你再看各个技术点的前后依赖关系就能理解得更清晰。相当于你以前只看到了单个点，只能见树，这次终于可以看见林了。

不过在本章数据只到了协议栈，还没有达到用户进程，下一章我们继续深入分析！

# 第3章

# 内核是如何与用户进程协作的

![](images/2c4ec1cedff2f851e539f4193ff94af617eb3dd6f689aa6a0bad8d0b6c72453e.jpg)

# 3.1 相关实际问题

在上一章中讲述了网络包是如何被从网卡送到协议栈的，接下来内核还有一项重要的工作，就是在协议栈接收处理完输入包以后，要能通知到用户进程，让用户进程能够收到并处理这些数据。进程和内核配合有很多种方案，本章只深入分析两种典型的。

第一种是同步阻塞的方案（在Java中习惯叫BIO），一般都是在客户端使用。它的优点是使用起来非常方便，非常符合人的思维方式，但缺点就是性能较差。典型的用户进程代码如下。

```txt
int main()
{
    int sk = socket(AF_INET, SOCK_STREAM, 0);
    connect(sk, ...)
    recv(sk, ...)
} 
```

第二种是多路IO复用的方案，这种方案在服务端用得比较多。Linux上多路复用方案有select、poll、epoll，它们三个中epoll的性能表现是最优秀的。在本章中只分析epoll（Java中对应的是NIO）。一段典型的使用epoll的C代码如下。

```txt
int main(){ listen(lfd, ...); cfd1 = accept(...); cfd2 = accept(...); efd = epoll_create(...); epollCtl(efd, EPOLL_CTL_ADD, cfd1, ...); epollCtl(efd, EPOLL_CTL_ADD, cfd2, ...); epoll_wait(efd, ...）   
} 
```

无论是这两种方案中的哪一种，内核都能在接收到数据的时候和用户进程协作，通知用户进程进行下一步处理。但是在高并发情况下，同步阻塞的IO方案的性能比较差，epoll的表现较好。具体原因是什么，在本章中将进行深入的分析。学习完本章以后，你将深刻地理解以下几个工作实践中的问题。

# 1）阻塞到底是怎么一回事？

在网络开发模型中，经常会遇到阻塞和非阻塞的概念。更要命的是还有人经常把这两个概念和同步、异步放到一起，让本来就理解得不是很清楚的概念更是变成一团浆糊。通过本章对源码的分析，你将深刻理解阻塞到底是怎么一回事。

# 2）同步阻塞IO都需要哪些开销？

都说同步阻塞IO性能差，我觉得这个说法太笼统。我们应该更深入地了解这种方式下都需要哪些CPU开销，而不是只简单地说差。

# 3）多路复用epoll为什么就能提高网络性能？

多路复用的概念在网络编程里非常重要，但可惜很多人对它理解得不够彻底。比如为什么epoll就比同步阻塞的IO模型性能好？可能有的人能隐隐约约知道内部的一个红黑树，但其实这仍然不是epoll性能优越的根本原因。

# 4）epoll也是阻塞的？

很多人以为只要一提到阻塞，就是性能差。当听说epoll也是可能会阻塞进程的以后，感觉诧异，阻塞咋还能性能高？这都是对epoll理解不深造成的，本章将会深度拆解epoll的工作原理。epoll的阻塞并不影响它高性能。

# 5）为什么Redis的网络性能很突出？

大家平时除了写代码，也会用到很多开源组件。在这些开源组件中，Redis的性能表现非常抢眼。那么它性能优异的秘诀究竟在哪里？我们在开发自己的接口的时候是否有可以学习它的点？

# 3.2 socket的直接创建

在开始介绍网络IO模型之前，需要先介绍一个前序知识，那就是socket是如何在内核中表示的。在后面分析阻塞或者epoll的时候，我们需要不定时来回顾socket的内核结构。

从开发者的角度来看，调用socket函数可以创建一个socket。

int main()   
{ int sk $=$ socket(AF_INET,SOCK_STREAM,0);

等这个socket函数调用执行完以后，用户层面看到返回的是一个整数型的句柄，但其实内核在内部创建了一系列的socket相关的内核对象（是的，不是只有一个）。它们互相之间的关系如图3.1所示。当然了，这个对象比图示的更复杂，图中只展示关键内容。

我们来翻翻源码，看看上面的结构是如何被创造出来的。

```txt
//file:net/socket.c  
SYSCALL DEFINE3(socket, int, family, int, type, int, protocol) 
```

{ ..... retval $=$ sock_create(family,type,protocol,&sock);

![](images/1cff77f4c994dac377305480572e6ef33c5ea5e98dd46cf9c99ed35209c5cbff.jpg)  
图3.1 socket内核结构

sock_create是创建socket的主要位置，其中sock_create又调用了__sock_create。

```lisp
//file:net/socket.c  
int __sock_create(struct net *net, int family, ...)  
{  
    struct socket *sock;  
    const struct net.proto_family *pf;  
    //分配socket对象  
    sock = sock_alloc();  
    //获得每个协议族的操作表  
    pf = rcu_dereference(net_families[family]); 
```

//调用每个协议族的创建函数，对于AF_INET对应的是inet_createerr $=$ pf->create(net,sock,protocol,kern);

在_sock_create里，首先调用sock_alloc来分配一个struct sock内核对象，接着获取协议族的操作函数表，并调用其create方法。对于AF_INET协议族来说，执行到的是inet_create方法。

//file:net/ipv4/af_inet.c   
tatic int inlet_create(struct net \*net, struct socket \*sock, int protocol,int   
kern)   
{ struct sock \*sk; //static struct inlet_protosw inetsw_array[] $=$ 11{ .type $=$ SOCK_STREAM, .protocol $=$ IPPROTO_TCP, .prot $=$ &tcpprot, .ops $=$ &inet_STREAMOps, .no_check $=$ 0, .flags $=$ INET_PROTOSW_PERMANENT I 11,}，   
11} list_for_each_entry_rcu的答案，&inetsw[sock->type],list）{ //将 inlet stream ops 赋到socket->ops上 sock->ops $=$ answer->ops; //获得 tcp prot answerprot $=$ answer->prot; //分配 sock对象，并把 tcp prot 赋到sock->skprot上 sk $=$ sk_alloc(net,PF_INET,GFP_KERNEL,answerprot); //对 sock对象进行初始化 sock_init_data(sock,sk);

在inet_create中，根据类型SOCK_STREAM查找到对于TCP定义的操作方法实现集合inet_STREAMOps和tcpprot，并把它们分别设置到socket->ops和sock->skprot上，如图3.2所示。

![](images/c3627d793060f4848b5764f50edc3b4c4d5d3593c9cd73e9c1185bf34a486d73.jpg)  
图3.2 socket ops方法

再往下看到了sock_init_data。在这个方法中将sock中的sk_data_ready函数指针进行了初始化，设置为默认sock_def_readable，如图3.3所示。

![](images/1609b56fc4b1ee8530de2b9c1d6a51fd0827ac6a2d23c085bd80635116e40744.jpg)  
图3.3 sk_data_ready初始化

```c
//file: net/core/sock.c  
void sock_init_data(struct socket *sock, struct sock *sk)  
{  
    sk->sk_data_ready = sock_def_readable;  
    sk->sk_write_space = sock_def_write_space;  
    sk->sk_error_report = sock_def_error_report;  
} 
```

当软中断上收到数据包时会通过调用sk_data_ready函数指针（实际被设置成了sock_def_readable()）来唤醒在sock上等待的进程。这个将在后面介绍软中断的时候再说，目前记住这个就行了。

至此，一个tcp对象，确切地说是AF_INET协议族下的SOCK_STREAM对象就算创建完成了。这里花费了一次socket系统调用的开销。

# 3.3 内核和用户进程协作之阻塞方式

本章开头说过同步阻塞的网络IO（在Java中习惯叫BIO）的优点是使用起来非常方便，但缺点就是性能非常差。俗话说得好，“知己知彼，方能百战百胜”。下面来深入

分析同步阻塞网络IO的内部实现。

在同步阻塞IO模型中，虽然用户进程里在最简单的情况下只有两三行代码，但实际上用户进程和内核配合做了非常多的工作。先是用户进程发起创建socket的指令，然后切换到内核态完成了内核对象的初始化。接下来，Linux在数据包的接收上，是硬中断和ksoftirqd线程在进行处理。当ksoftirqd线程处理完以后，再通知相关的用户进程。

从用户进程创建socket，到一个网络包抵达网卡被用户进程接收，同步阻塞IO总体上的流程如图3.4所示。

![](images/4536f7ed48fc209550762cfbb017e9f1d55e952629cda555c50709af051effe3.jpg)  
图3.4 同步阻塞工作流程

下面用图解加源码分析的方式来详细拆解上面的每一个步骤，来看一下在内核里它们是怎么实现的。阅读完本章，你将深刻理解同步阻塞的网络IO性能低下的原因！

# 3.3.1 等待接收消息

接下来看recv函数依赖的底层实现。首先通过strace命令跟踪，可以看到stdlib函数recv会执行recvform系统调用。

进入系统调用后，用户进程就进入了内核态，执行一系列的内核协议层函数，然后到socket对象的接收队列中查看是否有数据，没有的话就把自己添加到socket对应的等待队列里。最后让出CPU，操作系统会选择下一个就绪状态的进程来执行。整个流程如图3.5所示。

![](images/d61afc82a5760888f3d3b913843916cb707915688b9d76ade6cbc9770cffba62.jpg)  
图3.5 recvfrom系统调用

看完整个流程图，接下来根据源码来看更具体的细节。其中要关注的重点是recvfrom最后是怎么把自己的进程阻塞掉的（假如没有使用O_NONBLOCK标记）。

//file: net/socket.c   
SYSCALL DEFINE6(recvfrom, int, fd, void __user *, ubuf, size_t, size, unsigned int, flags, struct sockaddr __user *, addr, int __user *, addr_len)   
{ struct socket *sock; //根据用户传入的fd找到socket对象 sock $=$ sockfd.lookup_light(fd,&err,&fput_needed); err $=$ sock_recvmsg(sock,&msg,size,flags);

接下来的调用顺序为：

sock_recvmsg $= = >$ _sock_recvmsg $\equiv >$ _sock_recvmsg_nosec

```c
static inline int __sock_recvmsg_nosec(struct kiocb *iocb, struct socket *sock, struct msghdr *msg, size_t size, int flags) { return sock->ops->recvmsg(iocb, sock, msg, size, flags); } 
```

调用socket对象ops里的recvmsg，回忆图3.1中的socket对象图，从图3.6可以看到recvmsg指向的是inet_recvmsg方法。

![](images/c00c6b8ca812f71c87bf70466a6e34941cb9eacc88b72eb570c23d36de5b4316.jpg)  
图3.6 recvmsg方法

```c
//file: net/ipv4/af_inet.c  
int inlet_recvmsg(struct kiocb *iocb, struct socket *sock, struct msghdr *msg, size_t size, int flags)  
{  
    ...  
    err = sk->sk_prot->recvmsg(iocb, sk, msg, size, flags & MSG_DONTWAIT, flags & ~MSG_DONTWAIT, &addr_len); 
```

这里又遇到一个函数指针，这次调用的是socket对象里的sk.prot下的recvmsg方法。同样从图3.1中得出这个recvmsg方法对应的是tcp_recvmsg方法。

```c
//file: net/ipv4/tcp.c  
int tcp_recvmsg(struct kiocb *iocb, struct sock *sk, struct msghdr *msg, size_t len, int nonblock, int flags, int *addr_len)  
{  
    int copied = 0;  
    .......  
    do {  
        //遍历接收队列接收数据  
        skb_queue_walk(&sk->sk_receive_queue, skb) {  
            .......  
        }  
        .......  
    } 
```

```c
if (copied >= target) {
    release_sock(sk);
    lock_sock(sk);
} else //没有收到足够数据，启用sk_wait_data阻塞当前进程
    sk_wait_data(sk, &timeo);
}
```

终于看到了我们想要看的内容，skb_queue_walk在访问sock对象下的接收队列了，如图3.7所示。

![](images/8f32a82ee5a596a4a0ded06abcdfaca5da85f7e421465717ff089c4027740589.jpg)  
图3.7 接收队列读取

如果没有收到数据，或者收到的不够多，则调用sk_wait_data把当前进程阻塞掉。

//file:net/core/sock.c  
int sk_wait_data(struct sock \*sk,long \*timeo)  
{ //当前进程(current)关联到所定义的等待队列项上DEFINE_WAIT(wait); //调用sk_sleep获取sock对象下的wait//并准备挂起，将进程状态设置为可打断（INTERRUPTIBLE）prepare_to_wait(sk_sleep(sk)，&wait,TASK_INTERRUPTIBLE);set_bit(SOCKASYNC_WAITDATA,&sk->>sk_SOCKET->flags); //通过调用schedule_timeout让出CPU，然后进行睡眠rc $=$ sk_wait_event(sk,timeeo,！skb_queue_empty(&sk->>sk_receive_queue));

下面再来详细看看sk_wait_data是怎样把当前进程给阻塞掉的，如图3.8所示。

![](images/514eb738b3d9b657d151701a2736492b1da2df37c8d9ee943af4a771819d3f20.jpg)  
图3.8 进程阻塞

首先在DEFINE_WAIT宏下，定义了一个等待队列项wait。在这个新的等待队列项上，注册了回调函数autoremove_wake_function，并把当前进程描述符current关联到其(private成员上。

//file: include/linux/wait.h   
define DEFINE_WAIT(name)DEFINE_WAIT FUNC(name，autoremove_wake_function)   
#defineDEFINE_WAIT FUNC(name，function） wait_queue_t name $=$ { .private $=$ current, .func $=$ function, .task_list $=$ LIST_HEAD_INIT((name).task_list),

紧接着在sk_wait_data中调用sk_sleep获取sock对象下的等待队列列表头wait_queue_head_t。sk_sleep源码如下。

//file: include/net/sock.h   
static inline wait_queue_head_t \*sk_sleep(struct sock \*sk)   
{ BUILD_bug_ON(offsetof(struct socket_wq,wait) $! = 0$ ）; return &rcu_dereference_raw(sk->sk_wq)->wait;

接着调用prepare_to_wait来把新定义的等待队列项wait插入sock对象的等待队列。

```txt
//file: kernel/wait.c  
void 
```

```c
prepare_to_wait(wait_queue_head_t *q, wait_queue_t *wait, int state) { unsigned long flags; wait->flags & = ~WQ_FLAG_EXCLUSIVE; spin_lock_irqsave(&q->lock, flags); if (list_empty(&wait->task_list)) __add_wait_queue(q, wait); set_current_state(state); spin_unlock_irqrestore(&q->lock, flags); } 
```

这样后面当内核收完数据产生就绪事件的时候，就可以查找socket等待队列上的等待项，进而可以找到回调函数和在等待该socket就绪事件的进程了。

最后调用sk_wait_event让出CPU，进程将进入睡眠状态，这会导致一次进程上下文的开销，而这个开销是昂贵的，大约需要消耗几个微秒的CPU时间。

在接下来的内容里将能看到进程是如何被唤醒的。

# 3.3.2 软中断模块

接着我们再转换一下视角，来看负责接收和处理数据包的软中断这边。前文讲到了网络包到网卡后是怎么被网卡接收，最后再交由软中断处理的，这里直接从TCP协议的接收函数tcp_v4_rcv看起，总体接收流程见图3.9。

![](images/98a4c33e58c6d2fe1fe066d7225428acd4b49548ff9ea170d8c2192088f361e8.jpg)  
图3.9 软中断接收数据过程

软中断（也就是Linux里的ksoftirqd线程）里收到数据包以后，发现是TCP包就会执行tcp_v4_rcv函数。接着往下，如果是ESTABLISH状态下的数据包，则最终会把数据拆出来放到对应socket的接收队列中，然后调用sk_data_ready来唤醒用户进程。

我们看更详细一些的代码。

```c
// file: net/ipv4/tcp_ipv4.c  
int tcp_v4_rcv(struct sk_buffer *skb)  
{  
    th = tcp hdr(skb); //获取tcp header  
    iph = ip hdr(skb); //获取ip header 
```

//根据数据包header中的IP、端口信息查找到对应的socket

```javascript
sk = __inet.lookup_SKb(&tcp_hashinfo, skb, th->source, th->dest); 
```

//socket未被用户锁定

if(!sock-owned_by_user(sk)){ if(!tcp_prequeue(sk,skb)) ret $=$ tcp_v4_do_rcv(sk,skb);} }

在tcp_v4_rcv中，首先根据收到的网络包的header里的source和dest信息在本机上查询对应的socket。找到以后，直接进入接收的主体函数tcp_v4_do_rcv来一探究竟。

```c
//file: net/ipv4/tcp_ipv4.c  
int tcp_v4_do_rcv(struct sock *sk, struct sk BUFF *skb)  
{  
    if (sk->sk_state == TCP_ESTABLISHED) {  
        //执行连接状态下的数据处理  
        if (tcp_rcv_established(sk, skb, tcp hdr(skb), skb->len)) {  
            rsk = sk;  
            goto reset;  
        }  
        return 0;  
    }  
//其他非ESTABLISH状态的数据包处理 
```

假设处理的是ESTABLISH状态下的包，这样就又进入tcp_rcv_established函数进行处理。

```c
//file: net/ipv4/tcp_input.c  
int tcp_rcv_established(struct sock *sk, struct sk BUFF *skb, const struct tcphdr *th, unsigned int len) { 
```

//接收数据放到队列中

```txt
eaten = tcp_queue_rcv(sk, skb, tcp_header_len, &fragsolen); 
```

//数据准备好，唤醒socket上阻塞掉的进程

```erlang
sk->sk_data_ready(sk, 0); 
```

在tcp_rcv_established中通过调用tcp_queue_rcv函数，完成了将接收到的数据放到socket的接收队列上，如图3.10所示。

![](images/d365300284e2f7ece30f85091ee9a344828f6fe5e465a634490b0af13d14fef0.jpg)  
图3.10 添加到接收队列

函数tcp_queue_rcv的源码如下。

```c
//file: net/ipv4/tcp_input.c  
static int __must_check tcp_queue_rcv(struct sock *sk, struct sk BUFF *skb, int hdrlen, bool *fragstolen)  
{ 
```

//把接收到的数据放到socket的接收队列的尾部

```txt
if(!eaten){ _skb_queueTAIL(&sk->sk_receive_queue, skb); skb_set-owner_r(skb, sk); } 
```

```txt
return eaten; 
```

调用tcp_queue_rcv接收完成之后，接着调用sk_data_ready来唤醒在socket上等待的用户进程。这又是一个函数指针。回想在3.2节曾介绍过，在创建socket的流程里执行到的sock_init_data函数已经把sk_data_ready指针设置成sock_def_readable函数了。它是默认的数据就绪处理函数。

```c
//file: net/core/sock.c
static void sock_def_readable(struct sock *sk, int len)
{
struct socket_wq *wq;
rcu_read_lock();
wq = rcu_dereference(sk->sk_wq);
//有进程在此socket的等待队列
if (wq_has_sleeper(wq))
//唤醒等待队列上的进程
wake_up_interruptible_SYNC_poll(&wq->wait, POLLIN | POLLPRI | POLLRDNORM | POLLRDBAND);
sk_wake_async(sk, SOCK_WAKE_WAITD, POLL_IN);
rcu_read_unlock();
} 
```

在sock_def_readable中再一次访问到了sock->sk_wq下的wait。回忆一下前面调用recvfrom时，在执行过程的最后，通过DEFINE_WAIT(wait)将当前进程关联的等待队列添加到sock->sk_wq下的wait里了。

那接下来就是调用wake_up_interruptible_sync_poll来唤醒在socket上因为等待数据而被阻塞掉的进程了，如图3.11所示。

![](images/aa805c510202da6a826c7f6a75e9fc30f30fbcd649205a2645df2473a52f2360.jpg)  
图3.11 唤醒等待进程

```c
//file: include/linux/wait.h
#define wake_up_interruptible_SYNC_poll(x, m)
    __wake_up_SYNC_key((x), TASK_INTERRUPTIBLE, 1, (void *)(m))
//file: kernel/sched/core.c
void __wake_up_SYNC_key(wait_queue_head_t *q, unsigned int mode,
        int nr_exclusive, void *key)
{
    unsigned long flags;
    int wake_flags = WF_SYNC;
    if (unlikely(!q))
        return;
    if (unlikely(!nr_exclusive))
        wake_flags = 0;
    spin_lock_irqsave(&q->lock, flags);
    __wake_up_common(q, mode, nr_exclusive, wake_flags, key);
    spin_unlock_irqrestore(&q->lock, flags);
} 
```

__wake_up_common实现唤醒。这里注意一下，该函数调用的参数nr-exclusive传入的是1，这里指的是即使有多个进程都阻塞在同一个socket上，也只唤醒一个进程。其作用是为了避免“惊群”，而不是把所有的进程都唤醒。

```c
//file: kernel/sched/core.c
static void __wake_up_common_WAIT_queue_head_t *q, unsigned int mode,
	int nr_exclusive, int wake_flags, void *key)
{
wait_queue_t *curr, *next;
list_for_each_entrySAFE(curr, next, &q->task_list, task_list) {
unsigned flags = curr->flags;
if (curr->func(curr, mode, wake_flags, key) && (flags & WQ_FLAG_EXCLUSIVE) && !--nr_exclusive)
break;
}
} 
```

在__wake_up_common中找出一个等待队列项curr，然后调用其curr->func。回忆前面在recv函数执行的时候，使用DEFINE_WAIT()定义等待队列项的细节，内核把curr->func设置成了autoremove_wake_function。

//file: include/linux/wait.h   
define DEFINE_WAIT(name)DEFINE_WAIT FUNC(name，autoremove_wake_function) #defineDEFINE_WAIT FUNC(name，function） \ wait_queue_t name $=$ { .private $=$ current, .func $=$ function, .task_list $=$ LIST_HEAD_INIT((name).task_list),

在autoremove_wake_function中，调用了default_wake_function。

//file: kernel/sched/core.c   
```c
int default_wake_function(wait_queue_t *curr, unsigned mode, int wake_flags, void *key)  
{ return try_to_wake_up(curr->private, mode, wake_flags); } 
```

调用try_to_wake_up时传入的task_struct是curr->private，这个就是当时因为等待而被阻塞的进程项。当这个函数执行完的时候，在socket上等待而被阻塞的进程就被推入可运行队列里了，这又将产生一次进程上下文切换的开销。

# 3.3.3 同步阻塞总结

好了，我们把上面的流程总结一下。同步阻塞方式接收网络包的整个过程分为两部分：

- 第一部分是我们自己的代码所在的进程，我们调用的socket()函数会进入内核态创建必要内核对象。recv()函数在进入内核态以后负责查看接收队列，以及在没有数据可处理的时候把当前进程阻塞掉，让出CPU。  
- 第二部分是硬中断、软中断上下文（系统线程ksoftirqd）。在这些组件中，将包处理完后会放到socket的接收队列中。然后根据socket内核对象找到其等待队列中正在因为等待而被阻塞掉的进程，把它唤醒。

同步阻塞总体流程如图3.12所示。每次一个进程专门为了等一个socket上的数据就被从CPU上拿下来，然后换上另一个进程，如图3.13所示。等到数据准备好，睡眠的进程又会被唤醒，总共产生两次进程上下文切换开销。根据业界的测试，每一次切换大约花费 $3\sim 5$ 微秒，在不同的服务器上会有一点儿出入，但上下浮动不会太大。

要知道从开发者角度来看，进程上下文切换其实没有做有意义的工作。如果是网络IO密集型的应用，CPU就会被迫不停地做进程切换这种无用功。

![](images/8e22448e0cddedfe92dac7711e83bcf6ffa031ff67910ed54c7c17ed348bcf8e.jpg)  
图3.12 同步阻塞流程汇总

![](images/a79a1de9d5cf6f4afc997d268ca3018dc9d58c9215036772af900d56ede8a9a6.jpg)  
图3.13 进程切换

这种模式在客户端角色上，现在还存在使用的情形。因为你的进程可能确实要等MySQL的数据返回成功之后，才能渲染页面返回给用户，否则什么也干不了。

# 注意

注意一下，这里说的是角色，不是具体的机器。例如对于你的PHP/Java/Golang接口机，接收用户请求的时候，是服务端角色，但在请求Redis的时候，就变为客户端角色了。

不过现在有一些封装得很好的网络框架，例如Sogou Workflow、Golang的net包等，在网络客户端角色上也早已摒弃了这种低效的模式！

在服务端角色上，这种模式完全没办法使用。因为这种简单模型里的socket和进程是一对一的。现在要在单台机器上承载成千上万，甚至十几万、上百万的用户连接请求。如果用上面的方式，就得为每个用户请求都创建一个进程。相信你在无论多原始的服务端网络编程里，都没见过有人这么干吧。

如果让我给它起一个名字的话，就叫单路不复用（飞哥自创名词）。那么有没有更高效的网络IO模型呢？当然有，那就是你所熟知的select、poll和epoll了。下一节再开始拆解epoll的实现！

# 3.4 内核和用户进程协作之epoll

在上一节的recvfrom中，我们看到用户进程为了等待一个socket就得被阻塞掉。进程在Linux上是一个开销不小的家伙，先不说创建，仅是上下文切换一次就得几微秒。所以为了高效地对海量用户提供服务，必须要让一个进程能同时处理很多TCP连接才行。现在假设一个进程保持了1万条连接，那么如何发现哪条连接上有数据可读了、哪条连接可写了？

一种方法是我们可以采用循环遍历的方式来发现IO事件，以非阻塞的方式for循环遍历查看所有的socket。但这种方式太低级了，我们希望有一种更高效的机制，在很多连接中的某条上有IO事件发生时直接快速把它找出来。其实这个事情Linux操作系统已经替我们都做好了，它就是我们所熟知的IO多路复用机制。这里的复用指的就是对进程的复用。

在Linux上多路复用方案有select、poll、epoll。它们三个中的epoll的性能表现是最优秀的，能支持的并发量也最大。所以下面把epoll作为要拆解的对象，深入揭秘内核是如何实现多路的IO管理的。

为了方便讨论，还是把epoll的简单示例搬出来（只是个例子，实践中不这么写）。

```txt
int main(){ listen(lfd, ...); 
```

$\begin{array}{l}\mathrm{cfd1} = \mathrm{accept}(\dots);\\ \mathrm{cfd2} = \mathrm{accept}(\dots);\\ \mathrm{efd} = \mathrm{epoll\_create}(\dots);\\ \mathrm{epoll\_ctrl(efd,EPOLL\_CTL_ADD,cdf1,\dots)};\\ \mathrm{epoll\_ctrl(efd,EPOLL\_CTL_ADD,cdf2,\dots)};\\ \mathrm{epoll\_wait(efd,\dots)} \end{array}$

其中和epoll相关的函数是如下三个：

- epoll_create: 创建一个epoll对象。  
- epollCtl: 向epoll对象添加要管理的连接。  
- epoll_wait：等待其管理的连接上的IO事件。

借助这个demo来展开对epoll原理的深度拆解。相信等你理解了本节内容以后，对epoll的驾驭能力将变得炉火纯青！

# 3.4.1 epoll内核对象的创建

在用户进程调用epoll_create时，内核会创建一个struct eventpoll的内核对象，并把它关联到当前进程的已打开文件列表中，如图3.14所示。

![](images/e1e969c1e899e0f2a0479553c2e695e7e44ffe2c9bf5a86555e6558bcd334398.jpg)  
图3.14 进程与epoll的关系

对于struct eventpoll对象，更详细的结构如图3.15所示（同样只列出和本章主题相关的成员）。

![](images/1d1960161ca17e38a968a96ee4e126a3ea1b74131f97c244d70bcab9570e044a.jpg)  
图3.15 eventpoll对象

epoll_create的源代码相对比较简单，在fs/eventpoll.c中。

```c
// file: fs/eventpoll.c  
SYSCALL DEFINE1(epoll_create1, int, flags)  
{  
    struct eventpoll *ep = NULL;  
    //创建一个eventpoll对象  
    error = ep_alloc(&ep);  
}  
struct eventpoll的定义也在这个源文件中。
```

```txt
// file: fs/eventpoll.c  
struct eventpoll {  
    //sys_epoll_wait用到的等待队列 wait_queue_head_t wq;  
    //接收就绪的描述符都会放到这里 struct list_head rdlist;  
    //每个epoll对象中都有一棵红黑树 struct rb_root rbr;  
    ...  
} 
```

eventpoll这个结构体中的几个成员的含义如下：

- wq: 等待队列链表。软中断数据就绪的时候会通过wq来找到阻塞在epoll对象上的用户进程。  
- rbr：一棵红黑树。为了支持对海量连接的高效查找、插入和删除，eventpoll内部

使用了一棵红黑树。通过这棵树来管理用户进程下添加进来的所有socket连接。

- rdlist：就绪的描述符的链表。当有连接就绪的时候，内核会把就绪的连接放到rdlist链表里。这样应用进程只需要判断链表就能找出就绪连接，而不用去遍历整棵树。

当然这个结构被申请完之后，需要做一点点的初始化工作，这都在ep_alloc中完成。

```c
//file:fs/eventpoll.c  
static int ep_alloc(struct eventpoll **pep)  
{  
    struct eventpoll *ep;  
    //申请eventpoll内存  
    ep = kzalloc(sizeof(*ep), GFP_KERNEL);  
    //初始化等待队列头  
    init_waitqueue_head(&ep->wq);  
    //初始化就绪列表  
    INIT_LIST_HEAD(&ep->rdlist);  
    //初始化红黑树指针  
    ep->rbr = RB_ROOT;  
    ...  
} 
```

说到这里，这些成员其实只是刚被定义或初始化了，还都没有被使用。它们会在下面被用到。

# 3.4.2 为epoll添加socket

# 理解这一步是理解整个epoll的关键。

为了简单起见，我们只考虑使用E POLL_CTL_ADD添加socket，先忽略删除和更新。

假设现在和客户端的多个连接的socket都创建好了，也创建好了epoll内核对象。在使用epollCtl注册每一个socket的时候，内核会做如下三件事情：

1. 分配一个红黑树节点对象epitem。  
2. 将等待事件添加到socket的等待队列中，其回调函数是ep poll callback。  
3. 将epitem插入episode对象的红黑树。

通过epollCtl添加两个socket以后，这些内核数据结构最终在进程中的关系大致如图3.16所示。

![](images/8a9aaddb19fb9dee848bb15573aa34990e8df901664b7acebbe32f6494362c79.jpg)  
图3.16 为epoll添加两个socket的进程

我们来详细看看socket是如何添加到epoll对象里的，找到epollCtl的源码。

```c
// file: fs/eventpoll.c
SYSCALL DEFINE4(epollCtl, int, epfd, int, op, int, fd, struct epoll_event __user *, event)
{
    struct eventpoll *ep;
    struct file *file, *tfile;
    //根据epfd找到eventpoll内核对象
    file = fget(epfd);
    ep = file->private_data;
    //根据socket句柄号，找到其file内核对象
    tfile = fget(fd);
    switch (op) {
        case EPOLL_CTL_ADD:
            if (!epi) {
                epds.events |= POLLERR | POLLHUP;
                error = ep_insert(ep, &epds, tfile, fd);
            } else
            error = -EEXIST;
            clear_tfile_check_list();
        break;
    }
} 
```

在epoll_ctrl中首先根据传入fd找到eventpoll、socket相关的内核对象。对于E POLL_

CTL_ADD操作来说，然后会执行到ep_insert函数。所有的注册都是在这个函数中完成的。

```c
//file: fs/eventpoll.c
static int ep_insert(struct eventpoll *ep,
						struct epoll_event *event,
						struct file *tfile, int fd)
\{
	//1 分配并初始化epitem
	//分配一个epi对象
 struct epitem *epi;
 if (! (epi = kmem_cache_alloc(epi_cache, GFP_KERNEL)))
 return -ENOEM;
 //对分配的epi对象进行初始化
 //epi->ffd中存了句柄号和struct file对象地址
 INIT_LIST_HEAD(&epi->pwqlist);
 epi->ep = ep;
 ep_set_fdd(&epi->ffd, tfile, fd);
 //2 设置socket等待队列
 //定义并初始化ep_pqueue对象
 struct ep_pqueue epq;
 epq.epi = epi;
 init POLL_funcptr(&epq.pt, ep_ptable_queue_proc);
 //调用ep_ptable_queue_proc注册回调函数
 //实际注入的函数为ep_poll_callback
 revents = ep_item POLL(epi, &epq.pt);
 \ldots \ldots
 //3 将epi插入eventpoll对象的红黑树中
 ep_rbstree_insert(ep, epi);
 \ldots \ldots 
```

# 分配并初始化epitem

对于每一个socket，调用epollCtl的时候，都会为之分配一个epitem。该结构的主要数据结构如下：

```txt
//file: fs/eventpoll.c  
struct epitem {  
    //红黑树节点  
    struct rb_node rbn;  
    //socket文件描述符信息  
    struct epoll_filefd ffd;
```

```c
//所归属的eventpoll对象
struct eventpoll *ep;
//等待队列
struct list_head pwqlist; 
```

对epitem进行一些初始化，首先在epi->ep = ep这行代码中将其ep指针指向eventpoll对象。另外用要添加的socket的file、fd来填充epitem->ffd。epitem初始化后的关联关系如图3.17所示。

![](images/551156a1c0f2ff1b07989b6d3fa0d2ff3339db8f1c97a6554413a9b9fe3c7efc.jpg)  
图3.17epitem初始化

其中使用到的ep_set_ffd函数如下。

```c
static inline void ep_setffd(struct epoll_filefd *ffd, struct file *file, int fd) {  
   ffd->file = file;  
   ffd->fd = fd;  
} 
```

# 设置socket等待队列

在创建epitem并初始化之后，ep_insert中第二件事情就是设置socket对象上的等待任务队列，并把函数fs/eventpoll.c文件下的ep_poll_callback设置为数据就绪时候的回调函数，如图3.18所示。

这一块的源代码稍微有点绕，读者如果没有耐心的话直接跳到下面的粗体字部分来看。首先来看ep_item POLL。

```txt
static inline unsigned int ep_item POLL(struct item *epi, poll_table *pt) { pt->key = epi->event.events; return epi->ffd.file->f_op->poll(epi->ffd.file, pt) & epi->event.events; } 
```

![](images/8e88f4521633022c24b9607bf200f6a9894219078f82aeb5c4f4d50432639a47.jpg)  
图3.18 设置socket等待队列

看，这里调用到了socket下的file->f_op->poll。通过前面3.1节的socket的结构图，我们知道这个函数实际上是sock POLL。

```c
/* No kernel lock held - perfect */  
static unsigned int sock_poll(struct file *file, poll_table *wait) {  
    ...  
    return sock->ops->poll(file, sock, wait);  
} 
```

同样回看3.1节里的socket的结构图，sock->ops->poll其实指向的是tcp POLL。

//file: net/ipv4/tcp.c   
unsigned int tcp POLL(struct file \*file, struct socket \*sock, poll_table \*wait)   
{ struct sock \*sk $=$ sock->sk; sock POLL_wait(file, sk_sleep(sk), wait);   
}

在sock_poll_wait的第二个参数传参前，先调用了sk_sleep函数。在这个函数里它获取了sock对象下的等待队列列表头wait_queue_head_t，稍后等待队列项就插到这里。这里稍微注意下，是socket的等待队列，不是epoll对象的。下面来看sk_sleep源码。

```c
//file: include/net/sock.h  
static inline wait_queue_head_t *sk_sleep(struct sock *sk)  
{  
    BUILD_bug_ON(offsetof(struct socket_wq, wait) != 0);  
    return &rcu_dereference_raw(sk->sk_wq)->wait;  
} 
```

接着真正进入sock_poll_wait。

```c
static inline void sock_poll_wait(struct file *filp, wait_queue_head_t *wait_address, poll_table *p)  
{ poll_wait(filp, wait_address, p); }  
static inline void poll_wait(struct file * filp, wait_queue_head_t * wait_address, poll_table *p)  
{ if (p && p->qproc && wait_address) p->qproc(filp, wait_address, p); } 
```

这里的qproc是个函数指针，它在前面的init POLL_funcptr调用时被设置成了ep_table_queue_proc函数。

static int ep_insert(..)   
{ ... init POLL FUNCptr(&epq.pt, ep�性 table queue proc); 1 } //file: include/linux/poll.h static inline void init POLL FUNCptr(poll_table \*pt, poll_queue Proc qproc) { pt->_qproc $=$ qproc; pt->_key $\equiv$ ~0UL;/\* all events enabled \*/   
}

“敲黑板”！！！注意，费了半天劲儿，终于到了重点了！在ep�性列的proc函数中，新建了一个等待队列项，并注册其回调函数为ep poll_callback函数，然后再将这个等待项添加到socket的等待队列中。

```c
//file: fs/eventpoll.c  
static void ep_ptable_queueproc(struct file *file, wait_queue_head_t *whead, poll_table *pt)  
{  
    struct eppoll_entry *pwq;  
    f(epi->nwait >= 0 && (pwq = kmem_cache_alloc(pwq_cache, GFP_KERNEL))) { //初始化回调方法  
        init_waitqueue_func_entry(&pwq->wait, ep_poll_callback);  
        //将ep POLL_callback放入socket的等待队列whead（注意不是epoll的等待队列）  
        add_wait_queue(whead, &pwq->wait);  
    } 
```

在前面介绍阻塞式的系统调用recvfrom时，由于需要在数据就绪的时候唤醒用户进程，所以等待对象项的private（这个变量名起得令人无语）会设置成当前用户进程描述符current。而这里的socket是交给epoll来管理的，不需要在一个socket就绪的时候就唤醒进程，所以这里的q->private没有什么用就设置成了NULL。

//file:include/linux/wait.h   
static inline void init_waitqueue_func_entry( wait_queue_t \*q，wait_queue_func_t func)   
{ q->flags $= 0$ . q->private $=$ NULL; //将ep POLL_callback注册到wait_queue_t对象上 //有数据到达的时候调用q->func q->func $=$ func;   
}

如上，等待队列项中仅将回调函数q->func设置为ep POLL_callback。在3.4.4节中将看到，软中断将数据收到socket的接收队列后，会通过注册的这个ep POLL_callback函数来回调，进而通知epoll对象。

# 插入红黑树

分配完epitem对象后，紧接着把它插入红黑树。一个插入了一些socket描述符的epoll里的红黑树的示意图如图3.19所示。

![](images/ddfcfd2623eb18ce8ff5ca622d481b375e0ccea9238e58a8c355b1b5d532ff4b.jpg)  
图3.19 插入红黑树

这里再聊聊为什么要用红黑树，很多人说是因为效率高。其实我觉得这个解释不够全面，要说查找效率，树哪能比得上哈希表。我个人认为更为合理的解释是为了让epoll在查找效率、插入效率、内存开销等多个方面比较均衡，最后发现最适合这个需求的数据结构是红黑树。

# 3.4.3 epoll_wait之等待接收

epoll_wait做的事情不复杂，当它被调用时它观察eventpoll->rdlist链表里有没有数据。有数据就返回，没有数据就创建一个等待队列项，将其添加到eventpoll的等待队列上，然后把自己阻塞掉完事，如图3.20所示。

![](images/075317d5ff5cf84f8849eeaaef1c1e9ff8cf6634f989ce4f81b1bc318f9b3fdf.jpg)  
图3.20 epoll_wait原理

epollCtl添加socket时也创建了等待队列项。不同的是这里的等待队列项是挂在epoll对象上的，而前者是挂在socket对象上的。

其源代码如下：

```c
//file: fs/eventpoll.c  
SYSCALL DEFINE4(epoll_wait, int, epfd, ...)  
{  
    ...  
    error = ep POLL(ep, events, maxevents, timeout);  
}  
static int ep POLL(struct eventpoll *ep, ...)  
{  
    wait_queue_t wait;  
    ...  
fetch_events: //1 判断就绪队列上有没有事件就绪  
if (!ep_events-available(ep)) { 
```

```c
//2 定义等待事件并关联当前进程
init_waitqueue_entry(&wait, current);
//3 把新waitqueue添加到epoll->wq链表
_add_wait_queue_exclusive(&ep->wq, &wait);
for (;;) {
......
//4 让出CPU，主动进入睡眠状态
set_current_state(TASK_INTERRUPTIBLE);
if (!schedule_hrtimeout_range(to, slack, HRTIMER_MODE_ABS))
timed_out = 1;
......
} 
```

# 判断就绪队列上有没有事件就绪

首先调用ep_events-available来判断就绪链表中是否有可处理的事件。

//file: fs/eventpoll.c  
static inline int ep_events-available(struct eventpoll *ep)  
{  
    return !list_empty(&ep->rdlist) || ep->ovfli $t =$ EP_UNACTIVE_PTR;

# 定义等待事件并关联当前进程

假设确实没有就绪的连接，那接着会进入init_waitqueue_entry中定义等待任务，并把current（当前进程）添加到waitqueue上。

# 注意

是的，当没有IO事件的时候，epoll也会阻塞掉当前进程。这个是合理的，因为没有事情可做了占着CPU也没什么意义。网上的一些文章有个很不好的习惯，讨论阻塞、非阻塞等概念的时候都不说主语。这会导致你看得云里雾里。拿epoll来说，epoll本身是阻塞的，但一般会把socket设置成非阻塞。只有说了主语，这些概念才有意义。

//file: include/linux/wait.h

```c
static inline void init_waitqueue_entry(wait_queue_t *q, struct task_struct *p) { q->flags = 0; q->private = p; 
```

q->func $=$ default_wake_function;

注意这里的回调函数名称是default_wake_function。后续在3.4.4节中将会调用该函数。

# 添加到等待队列

```c
static inline void __add_wait_queue_exclusive(wait_queue_head_t *q, wait_queue_t *wait) { wait->flags |= WQ_FLAG_EXCLUSIVE; __add_wait_queue(q, wait); } 
```

在这里，把上一小节定义的等待事件添加到了epoll对象的等待队列中。

# 让出CPU主动进入睡眠状态

通过set_current_state把当前进程设置为可打断。调用schedule_httimeout_range让出CPU，主动进入睡眠状态。

```c
//file: kernel/hrtimer.c  
int __sched schedule_hrttimeout_range(ktime_t *Expires, unsigned long delta, const enum hrtimer_mode mode)  
{  
    return schedule_hrttimeout_range_clock(Expires, delta, mode, CLOCK_MONOTONIC);  
}  
int __sched schedule_hrttimeout_range_clock(...){  
    schedule();  
} 
```

在schedule中选择下一个进程调度。

```c
//file: kernel/sched/core.c  
static void __sched__schedule(void)  
{ next = pick_next_task(rq); context_SWITCH(rq, prev, next); } 
```

# 3.4.4 数据来了

在前面epollCtl执行的时候，内核为每一个socket都添加了一个等待队列项。在epoll_

wait运行完的时候，又在event poll对象上添加了等待队列元素。在讨论数据开始接收之前，我们把这些队列项的内容再总结到图3.21中。

![](images/8445b1376ef61ac08bb26495a9d6f80b36791523ae42283f45332752cfd09048.jpg)  
图3.21 各种epoll相关队列

- socket->sock->sk_data_ready设置的就绪处理函数是sock_def_readable。  
- 在socket的等待队列项中，其回调函数是ep_poll_callback。另外其private没用了，指向的是空指针null。  
- 在eventpoll的等待队列项中，其回调函数是default_wake_function。其private指向的是等待该事件的用户进程。

在这一小节里，将看到软中断是怎样在数据处理完之后依次进入各个回调函数，最后通知到用户进程的。

# 将数据接收到任务队列

关于软中断是怎么处理网络帧的，这里不再过多介绍，回头看第2章即可。我们直接从TCP协议栈的处理入口函数tcp_v4_rcv开始说起。

```c
// file: net/ipv4/tcp_ipv4.c  
int tcp_v4_rcv(struct sk_buffer *skb)  
{  
    .......  
    th = tcp hdr(skb); //获取TCP头  
    iph = ip hdr(skb); //获取IP头  
    //根据数据包头中的IP、端口信息查找到对应的socket  
    sk = __inetLOOKUP_SKB(&tcp_hashinfo, skb, th->source, th->dest);  
    .......  
    //socket未被用户锁定
```

if(!sock-owned_by_user(sk)){ if(!tcp_prequeue(sk, skb)) ret $=$ tcp_v4_do_rcv(sk, skb); } 1

在tcp_v4_rcv中首先根据收到的网络包的header里的source和dest信息在本机上查询对应的socket。找到以后，我们直接进入接收的主体函数tcp_v4_do_rcv来看。

```c
//file: net/ipv4/tcp_ipv4.c  
int tcp_v4_do_rcv(struct sock *sk, struct sk BUFF *skb)  
{  
    if (sk->sk_state == TCP_ESTABLISHED) {  
        //执行连接状态下的数据处理  
        if (tcp_rcv_established(sk, skb, tcp hdr(skb), skb->len)) {  
            rsk = sk;  
            goto reset;  
        }  
    return 0;  
}  
//其他非ESTABLISH状态的数据包处理 
```

我们假设处理的是ESTABLISH状态下的包，这样就又进入tcp_rcv_established函数中进行处理了。

```c
//file: net/ipv4/tcp_input.c  
int tcp_rcv_established(struct sock *sk, struct sk BUFF *skb, const struct tcphdr *th, unsigned int len)  
{ 
```

//将数据接收到队列中  
eaten = tcp_queue_rcv(sk, skb, tcp_header_len, &fragstolen);

//数据准备好，唤醒socket上阻塞掉的进程sk->sk_data_ready(sk,0);

在tcp_rcv_established中通过调用tcp_queue_rcv函数完成了将接收数据放到socket的接收队列上，如图3.22所示。

![](images/901320b4ab02fd5aebe9f44bdb500fb15b71fc39fd3f5b9655a37285d2de490a.jpg)  
图3.22 将数据保存到socket接收队列

源码如下所示。

```c
//file: net/ipv4/tcp_input.c  
static int __must_check tcp_queue_rcv(struct sock *sk, struct sk BUFF *skb, int hdrlen, bool *fragstolen)  
{ //把接收到的数据放到socket的接收队列的尾部  
if (!eaten) { _skb_queueTAIL(&sk->sk_receive_queue, skb); skb_set-owner_r(skb, sk); } return eaten; 
```

# 查找就绪回调函数

调用tcp_queue_rcv完成接收之后，接着再调用sk_data_ready来唤醒在socket上等待的用户进程。这又是一个函数指针。回想3.1节在accept函数创建socket流程里提到的sock_init_data函数，其中已经把sk_data_ready设置成sock_def_readable函数了。它是默认的数据就绪处理函数。

当socket上数据就绪时，内核将以sock_def_readable这个函数为入口，找到epollCtl添加socket时在其上设置的回调函数ep_poll_callback，如图3.23所示。

![](images/c3ebc29151f47712c02576b34d9450a3a990e94f798c9e8be0ab859eb961352a.jpg)  
图3.23 就绪回调

接下来详细看看细节。

```c
//file: net/core/sock.c
static void sock_def_readable(struct sock *sk, int len)
{
    struct socket_wq *wq;
    rcu_read_lock();
    wq = rcu_dereference(sk->sk_wq);
    //这个名字起得不好，并不是有阻塞的进程，
    //而是判断等待队列不为空
    if (wq_has_sleeper(wq)) {
        //执行等待队列项上的回调函数
        wake_up_interruptible_SYNC_poll(&wq->wait, POLLIN | POLLPRI | POLLRDNORM | POLLRDBAND);
        sk_wake_async(sk, SOCK_WAKE_WAITD, POLL_IN);
        rcu_read_unlock();
    }
}
```

这里的函数名其实都有迷惑人的地方：

- wq_has_sleeper，对于简单的recvfrom系统调用来说，确实是判断是否有进程阻塞。但是对于epoll下的socket只是判断等待队列是否不为空，不一定有进程阻塞。  
- wake_up_interruptible_SYNC_poll，只是会进入socket等待队列项上设置的回调函数，并不一定有唤醒进程的操作。

接下来重点看wake_up_interruptible_sync POLL。我们看一下内核是怎么找到等待队列项里注册的回调函数的。

```c
//file: include/linux/wait.h
#define wake_up_interruptible_SYNC_poll(x, m) \
wake_up_SYNC_key((x), TASK_INTERRUPTIBLE, 1, (void *)(m))
//file: kernel/sched/core.c
void _wake_up_SYNC_key(wait_queue_head_t *q, unsigned int mode,
	int nr_exclusive, void *key)
\{
	wake_up_common(q, mode, nr_exclusive, wake_flags, key);
\} 
```

接着进入_wake_up_common。

```c
static void __wake_up_common(wait_queue_head_t *q, unsigned int mode, int nr_exclusive, int wake_flags, void *key) { wait_queue_t *curr, *next; list_for_each_entrySAFE(curr, next, &q->task_list, task_list) { unsigned flags = curr->flags; if (curr->func(curr, mode, wake_flags, key) && (flags & WQ_FLAG_EXCLUSIVE) && !--nr_exclusive) break; } 
```

在 wake_up_common 中，选出等待队列里注册的某个元素curr，回调其curr->func。之前调用ep_insert的时候，把这个func设置成ep_poll_callback了。

# 执行socket就绪回调函数

由前面的内容可知，已经找到了socket等待队列项里注册的函数ep_poll_callback，接着软中断就会调用它。

```c
//file: fs/eventpoll.c  
static int ep POLL_callback_WAIT_queue_t *wait, unsigned mode, int sync, void *key)  
{ //获取wait对应的epitem struct epitem *epi = ep_item_from_wait.wait); //获取epitem对应的eventpoll结构体 struct eventpoll *ep = epi->ep; 
```

//1 将当前epitem添加到eventpoll的就绪队列中

list_addTAIL(&epi->rdlink, &ep->rdlist);

//2 查看eventpoll的等待队列上是否有等待

if (waitqueue.active(&ep->wq))  
wake_up_locked(&ep->wq);

在ep poll_callback中根据等待任务队列项上额外的base指针可以找到epitem，进而也可以找到eventpoll对象。

它做的第一件事就是把自己的epitem添加到epoll的就绪队列中。接着它又会查看eventpoll对象上的等待队列里是否有等待项（epoll_wait执行的时候会设置）。如果没有等待项，软中断的事情就做完了。如果有等待项，那就找到等待项里设置的回调函数，如图3.24所示。

![](images/b32b8833bf742c92c9aa66f1ebc55d9cd03f9fc8c231b7f70d88250e6631faf6.jpg)  
图3.24 回调eventpoll等待项

依次调用wake_up_lock() $\coloneqq >$ _wake_up_lock() $\coloneqq >$ _wake_up_common。 static void _wake_up_common_WAIT_queue_head_t \*q，unsigned int mode, int nr_exclusive，int wake_flags，void \*key) { wait_queue_t \*curr,\*next; list_for_each_entrySAFE(curr,next,&q->task_list，task_list){ unsigned flags $=$ curr->flags; if (curr->func(curr，mode，wake_flags,key)&& （flags&WQ_FLAG_EXCLUSIVE）&&！--nr_exclusive) break; }

在__wake_up_common里，调用curr->func。这里的func是在epoll_wait时传入的 default_wake_function函数。

# 执行epoll就绪通知

在default_wake_function中找到等待队列项里的进程描述符，然后唤醒它，如图3.25所示。

![](images/5c5ae72129d02752e65e5c569c17f5f3f74dbbef655bb68658099754ebc45041.jpg)  
图3.25 唤醒用户进程

源代码如下：

```c
//file:kernel/sched/core.c  
int default_wake_function(wait_queue_t *curr, unsigned mode, int wake_flags, void *key)  
{  
    return try_to_wake_up curr->private, mode, wake_flags);  
} 
```

等待队列项curr->private指针是在epoll对象上等待而被阻塞掉的进程。

将epoll_wait进程推入可运行队列，等待内核重新调度进程。当这个进程重新运行后，从epoll_wait阻塞时暂停的代码处继续执行。把rdlist中就绪的事件返回给用户进程。

```txt
//file: fs/eventpoll.c   
static int ep POLL(struct eventpoll \*ep, struct epoll_event _user \*events, int maxevents,long timeout)   
{ .remove_wait_queue(&ep->wq,&wait); set_current_state(TASK_RUNNING); 1 check_events: //给用户进程返回就绪事件 ep_send_events(ep,events,maxevents)) 
```

从用户角度来看，epoll_wait只是多等了一会儿而已，但执行流程还是顺序的。

# 3.4.5 小结

我们来用图3.26总结episode的整个工作流程。

![](images/37b4e23c76d87d4bcc2a9b4a17d0d3a9c8eb5eacb49f1add3fd715ca4098e3a4.jpg)  
图3.26 epoll原理汇总

其中软中断回调时的回调函数调用关系整理如下：

sock_def_readable: sock对象初始化时设置的。

$= >$ ep_poll_callback：调用epollCtl时添加到socket上的。

$= >$ default_wake_function：调用epoll_wait时设置到epoll上的。

总结一下，epoll相关的函数里内核运行环境分两部分：

- 用户进程内核态。调用epoll_wait等函数时会将进程陷入内核态来执行。这部分代码负责查看接收队列，以及负责把当前进程阻塞掉，让出CPU。

- 硬、软中断上下文。在这些组件中，将包从网卡接收过来进行处理，然后放到socket的接收队列。对于epoll来说，再找到socket关联的epitem，并把它添加到epoll对象的就绪链表中。这个时候再捎带检查一下epoll上是否有被阻塞的进程，如果有唤醒它。

为了介绍到每个细节，本章涉及的流程比较多，把阻塞都介绍进来了。

但其实在实践中，只要活儿足够多，epoll_wait根本不会让进程阻塞。用户进程会一直干活儿，一直干活儿，直到epoll_wait里实在没活儿可干的时候才主动让出CPU。这就是epoll高效的核心原因所在！

# 3.5 本章总结

好了，同步阻塞的recvfrom和多路复用的epoll都深度拆解完了，现在回过头再看本章开篇提出的问题。

# 1）阻塞到底是怎么一回事？

网络开发模型中，经常会遇到阻塞和非阻塞的概念。通过本章对源码的分析，我们理解了阻塞其实说的是进程因为等待某个事件而主动让出CPU挂起的操作。在网络IO中，当进程等待socket上的数据时，如果数据还没有到来，那就把当前进程状态从TASK_RUNNING修改为TASK_INTERRUPTPLE，然后主动让出CPU。由调度器来调度下一个就绪状态的进程来执行。

所以，以后你在分析某个技术方案是不是阻塞的时候，关键要看进程有没有放弃CPU。如果放弃了，那就是阻塞。如果没放弃，那就是非阻塞。事实上，recvfrom也可以设置成非阻塞。在这种情况下，如果socket上没有数据到达，调用直接返回空，而不是挂起等待。

# 2）同步阻塞IO都需要哪些开销？

通过本章的介绍可以了解到同步阻塞IO的开销主要有以下这些：

- 进程通过recv系统调用接收一个socket上的数据时，如果数据没有达到，进程就被从CPU上拿下来，然后再换上另一个进程。这导致一次进程上下文切换的开销。  
- 当连接上的数据就绪的时候，睡眠的进程又会被唤醒，又是一次进程切换的开销。  
- 一个进程同时只能等待一条连接，如果有很多并发，则需要很多进程。每个进程都将占用大约几MB的内存。

从CPU开销角度来看，一次同步阻塞网络IO将导致两次进程上下文切换开销。每一

次切换大约花费3~5微秒。从开发者角度来看，进程上下文切换其实没在做有意义的工作。如果是网络IO密集型的应用，CPU就不停地做进程切换，CPU吭哧吭哧累得要死，还被程序员吐槽性能差。

另外就是一个进程同一时间只能处理一个socket，我们现在要在单台机器上承载成千上万，甚至十几万、上百万的用户连接请求。如果用上面的方式，那就得为每个用户请求都创建一个进程。内存可能都不够用。

如果用一句话来概括，那就是：同步阻塞网络IO是高性能网络开发路上的绊脚石！所以在服务端的网络IO模型里，没有人用同步阻塞网络IO。

# 3）多路复用epoll为什么就能提高网络性能？

其实epoll高性能最根本的原因是极大程度地减少了无用的进程上下文切换，让进程更专注地处理网络请求。

在内核的硬、软中断上下文中，包从网卡接收过来进行处理，然后放到socket的接收队列。再找到socket关联的epitem，并把它添加到epoll对象的就绪链表中。

在用户进程中，通过调用epoll_wait来查看就绪链表中是否有事件到达，如果有，直接取走进行处理。处理完毕再次调用epoll_wait。在高并发的实践中，只要活儿足够多，epoll_wait根本不会让进程阻塞。用户进程会一直干活儿，一直干活儿，直到epoll_wait里实在没活儿可干的时候才主动让出CPU。这就是epoll高效的核心原因所在！

至于红黑树，仅仅是提高了epoll查找、添加、删除socket时的效率而已，不算epoll在高并发场景高性能的根本原因。

# 4）epoll也是阻塞的？

很多人以为只要一提到阻塞，就是性能差，其实这就冤枉了阻塞。本章多次讲过，阻塞说的是进程因为等待某个事件而主动让出CPU挂起的操作。

例如，一个epoll对象下添加了一万个客户端连接的socket。假设所有这些socket上都还没有数据达到，这个时候进程调用epoll_wait发现没有任何事情可干。这种情况下用户进程就会被阻塞掉，而这种情况是完全正常的，没有工作需要处理，那还占着CPU是没有道理的。

阻塞不会导致低性能，过多过频繁的阻塞才会。epoll的阻塞和它的高性能并不冲突。

# 5）为什么Redis的网络性能都很突出？

Redis在网络IO性能上表现非常突出，单进程的服务器在极限情况下可以达到10万的QPS。

我们来看下它某个版本的源码，其实非常简洁。

```txt
void aeMain(aeEventLoop *eventLoop) {
    while (!eventLoop->stop) {
        ...
    }
} 
```

```txt
//开始处理事件  
aeProcessEvents(eventLoop，AE_ALL_EVENTS);  
}  
}  
aeMain是Redis事件循环，在这个循环里进入aeProcessEvents。  
//file:src/ae.c  
int aeProcessEvents(ajeEventLoop *eventLoop，int flags)  
{  
//等待事件  
numevents = aeApiPoll(eventLoop，tvp);  
for（j=0；j<j<numevents;j++) {  
//处理  
aeFileEvent *fe = &eventLoop->events[eventLoop->fired[j].fd];  
fe->rfileProc()  
fe->wfileProc()  
}  
}
```

aeProcessEvents中通过调用aeApiPoll来等待时间，其实aeApiPoll只是一个对epoll_wait的封装而已。

```c
//file: src/ae_epoll.c  
static int aeApiPoll(ajeEventLoop *eventLoop, struct timeval *tvp) {  
    .......  
    retval = epoll_wait(state->epfd, state->events, eventLoop->setsize, tvp ? (tvp->tv_sec*1000 + tvp->tv_usec/1000) : -1);  
} 
```

Redis的这个事件循环，可以简化到用如下伪码来表示。

```txt
void aeMain(aeEventLoop *eventLoop) {
    job = epoll_wait(...)
    do_job();
} 
```

Redis的主要业务逻辑就是在本机内存上的数据结构的读写，几乎没有网络IO和磁盘IO，单个请求处理起来很快。所以它把主服务端程序干脆就做成了单进程的，这样省去了多进程之间协作的负担，也更大程度减少了进程切换。进程主要的工作过程就是调用epoll_wait等待事件，有了事件以后处理，处理完之后再调用epoll_wait。一直工作，一直工作，直到实在没有请求需要处理，或者进程时间片到的时候才让出CPU。工作效率发挥到了极致！

# ★注意

其他一些服务或者网络IO框架一般是多进程的配合，谁来等待事件，谁来处理事件，谁来发送结果，就是大家经常听到的各种Reactor、Proactor模型。这就会有进程通信开销，以及可能会带来的进程上下文切换CPU消耗。

在行业里和工作中，你一定也见过这样的大神程序员，一个人就能写出非常优秀的项目。对于大神来说，省去了和他人的沟通和交流成本，反而工作效率能发挥到极致。这感觉和Redis有点像。

# 注意

虽然单进程的Redis性能很高，单实例可以支持最高10万QPS，但仍然有公司有更高的性能要求。所以在Redis 6.0版本中也开始支持多进程了，不过默认情况下仍然是关闭的。

# 第4章

# 内核是如何发送网络包的

![](images/bf4e376f6c9cc857c00a27ebce71435062c49c01151a223ba8d7d80e6765c4b3.jpg)

4

CHAPIT

# 4.1 相关实际问题

前面的章节中，我们讨论了Linux接收网络包的过程，以及内核如何和用户进程进行协作。在本章中，将深度讨论内核发送网络包的过程。

我们先来思考如下几个问题。

# (1) 在查看内核发送数据消耗的CPU时，应该看sy还是si？

内核在发送网络包的时候，是需要CPU进行很多的处理工作的。在top命令展示的结果里，和内核相关的项目有这么几个：sy、hi和si等。那么发送网络包的消耗主要是在哪个数据中体现呢？

# 2）在服务器上查看/proc/softirqs，为什么NET_RX要比NET_TX大得多的多？

软中断类型有好几种，只拿网络IO相关的来说，NET_RX是接收（R表示receive），NET_TX是传输（T表示transmit）。对于一个既收取用户请求，又给用户返回数据的服务器来说，这两块的数字应该差不多才对，至少不会有数量级的差异。但事实上，你拿手头的任何一台服务器来看，NET_RX都要比NET_TX多得多。

拿我手头的一台线上接口服务器来看，NET_RX要比NET_TX高了三个数量级：

<table><tr><td colspan="5">$ cat /proc/softirqs</td></tr><tr><td></td><td>CPU0</td><td>CPU1</td><td>CPU2</td><td>CPU3</td></tr><tr><td>HI:</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>TIMER:</td><td>1670794607</td><td>218940516</td><td>3765758957</td><td>3937988107</td></tr><tr><td>NET_TX:</td><td>384508</td><td>285972</td><td>244566</td><td>258230</td></tr><tr><td>NET_RX:</td><td>1591545176</td><td>1212716226</td><td>1017620906</td><td>1058380340</td></tr></table>

那你是否清楚产生这种情况的原因是什么？

# 3）发送网络数据的时候都涉及哪些内存拷贝操作？

你可能在一些博客里见过一种说法是用“零拷贝”的技术来提高性能。但是我觉得在理解“零拷贝”之前，首先应该搞清楚发送网络数据涉及哪些内存拷贝。不理解这个基础知识，对“零拷贝”很难理解到点上。

这些问题其实我们在线上经常会遇到、看到，但我们似乎很少去深究。如果真的能透彻地把这些问题理解到位，我们对性能的掌控能力将会变得更强。

# 4）零拷贝到底是怎么回事？

很多性能优化方案里都会提到零拷贝。但是零拷贝到底是怎么回事，是真的没有数据的内存拷贝了吗？究竟避免了哪步到哪步的拷贝操作？如果不了解数据在网络包收发时在各个不同内核组件中的拷贝过程，对零拷贝很难理解到本质。

# 5）为什么Kafka的网络性能很突出？

大家一定对Kafka出类拔萃的性能有所耳闻。那么它性能优异的秘诀究竟在哪儿？如果能够理解清楚，那对提高我们自己手中项目代码的性能一定会有很大的价值。

# 4.2 网络包发送过程总览

还是先从一段简单的代码切入。如下代码是一个典型服务端程序的典型缩微代码：

int main(){ fd $=$ socket(AF_INET,SOCK_STREAM，0); bind(fd，...); listen(fd，...); cfd $=$ accept(fd，...); //接收用户请求 read(cfd，...); //用户请求处理 dosomething(); //给用户返回结果 send(cfd，buf,sizeof(buf)，0);   
}

下面来讨论上述代码中，调用send之后内核是怎样把数据包发送出去的。

我觉得看Linux源码最重要的是要有整体上的把握，而不是一开始就陷入各种细节。这里先给大家准备了一个总的流程图，见图4.1。下面简单阐述发送的数据是如何一步一步被发送到网卡的。

![](images/295546bedd4c5ca089b56cadaecfdf2fb10fdb3cd45dab8e15415fb4df2137d9.jpg)  
图4.1 网络发送过程概览

在图4.1中，可以看到用户数据被拷贝到内核态，然后经过协议栈处理后进入RingBuffer。随后网卡驱动真正将数据发送了出去。当发送完成的时候，是通过硬中断来通知CPU，然后清理RingBuffer。

因为本章后面要进入源码分析，所以我们再从源码的角度给出一个流程图，如图4.2所示。

![](images/f48d90984a8d1f4edaa4259228807b46d155a5254a1bbd0c29a6a10067caaa2c.jpg)  
图4.2 网络发送过程

![](images/b486c66102832778cf9da35112176041394a161131848a0719db5dd633064bed.jpg)  
图4.2 续

虽然这时数据已经发送完毕，但其实还有一件重要的事情没做，那就是释放缓存队列等内存。那内核是如何知道什么时候才能释放内存的呢？当然是等网络发送完毕之后。网卡在发送完毕的时候，会给CPU发送一个硬中断来通知CPU，见图4.3。

![](images/7a531c0c4aa54215af8477d041302b99490b1c732a54d2e6cfb1b85cbb0569e1.jpg)  
图4.3 发送完毕清理

注意，这里的主题虽然是发送数据，但是硬中断最终触发的软中断却是NET_RX_SOFTIRQ，而并不是NET_TX_SOFTIRQ！！！（T表示transmit，R表示receive）

意不意外，惊不惊喜？！！

所以这就是开篇问题2的一部分的原因（注意，这只是一部分原因）。

问题2：在服务器上的/proc/softirqs里NET_RX要比NET_TX大得多得多？

传输完成最终会触发NET_RX，而不是NET_TX。所以自然你观测/proc/softirqs也就能看到NET_RX更多了。

好，现在你已经对内核是怎么发送网络包的有一个全局上的认识了。不要得意，我们需要了解的细节才是更有价值的地方，让我们继续！

# 4.3 网卡启动准备

在第2章介绍网络包接收过程中，介绍过网卡的启动过程。当时深入地介绍过接收队列RingBuffer。现在再来详细地看一看传输队列RingBuffer。

现在的服务器上的网卡一般都是支持多队列的。每一个队列都是由一个RingBuffer表示的，开启了多队列以后的网卡就会对应有多个RingBuffer，如图4.4所示。

![](images/a6563bbeaeccbd41db1208e0bcb9acab436736293e9e274da914ede544cb39b2.jpg)  
图4.4 网卡的接收和发送队列

网卡在启动时最重要的任务之一就是分配和初始化RingBuffer，理解了RingBuffer将会非常有助于掌握发送。所以接下来看看网卡启动时分配传输队列RingBuffer的实际过程。

在网卡启动的时候，会调用到__igb_open函数，RingBuffer就是在这里分配的。

//file:drivers/net/ethernet/intel/igb/igb_main.c   
static int _igb_open(struct net_device \*netdev，bool resuming)   
{ struct igb_adapter \*adapter $=$ netdevpriv(netdev);

//分配传输描述符数组err $=$ igb_setup_all_txResources(adapter); //分配接收描述符数组err $=$ igb_setup_all_rxResources(adapter); //开启全部队列netif_tx_start_all_queueues(netdev);

上面的__igb_open函数调用igb_setup_all_tx-resources分配所有的传输RingBuffer，调用igb_setup_all_rx-resources创建所有的接收RingBuffer。

//file:drivers/net/ethernet/intel/igb/igb_main.c  
static int igb_setup_all_txResources(struct igb_adapter *adapter)  
{ //有几个队列就构造几个RingBuffer for $(\mathbf{i} = 0;\mathbf{i} <   \mathbf{a}$ dapter->num_tx_queue; i++) { igb_setup_txResources(adapter->tx_ring[i]); }

真正的RingBuffer构造过程是在igb_setup_tx-resources中完成的。

```c
//file:drivers/net/ethernet/intel/igb/igb_main.c  
int igb_setup_tx-resources(struct igb_ring *tx_ring)  
{  
//1.申请igb_tx_buffer数组内存  
size = sizeof(struct igb_tx_buffer) * tx_ring->count;  
tx_ring->tx_buffer_info = vzalloc(size);  
//2.申请e1000_adv_tx_desc DMA数组内存  
tx_ring->size = tx_ring->count * sizeof.union e1000_adv_tx_desc);  
tx_ring->size = ALIGN(tx_ring->size, 4096);  
tx_ring->desc = dma_alloc_coherent(dev, tx_ring->size, &tx_ring->dma, GFP_KERNEL);  
//3.初始化队列成员  
tx_ring->next_to_use = 0;  
tx_ring->next_to_clean = 0; 
```

从上述源码可以看到，一个传输 RingBuffer 的内部也不仅仅是一个环形队列数组：

- jqb tx buffer数组：这个数组是内核使用的，通过zalloc申请。  
- e1000_adv_tx_desc数组：这个数组是网卡硬件使用的，通过dma_alloc_coherent分配。

这个时候它们之间还没有什么联系。将来在发送的时候，这两个环形数组中相同位置的指针都将指向同一个skb，如图4.5所示。这样，内核和硬件就能共同访问同样的数据了，内核往skb写数据，网卡硬件负责发送。

![](images/742f6ac445bc4aa2504c6d6b82ce25baecfa14943f4a6684a045e6d7250fb1fe.jpg)  
图4.5 发送队列细节

最后调用netif_tx_start_all_queues开启队列。另外，硬中断的处理函数igb_msix_ring，其实也是在__igb_open中注册的。

# 4.4 数据从用户进程到网卡的详细过程

# 4.4.1 send系统调用实现

send系统调用的源码位于文件 net/socket.c中。在这个系统调用里，内部其实真正使用的是sendto系统调用。整个调用链条虽然不短，但其实主要只干了两件简单的事情：

- 第一是在内核中把真正的socket找出来，在这个对象里记录着各种协议栈的函数地址。  
- 第二是构造一个 struct msghdr对象，把用户传入的数据，比如buffer地址、数据长度什么的，都装进去。

剩下的事情就交给下一层，协议栈里的函数inet_sendmsg了，其中inet_sendmsg函数的地址是通过socket内核对象里的ops成员找到的。大致流程如图4.6所示。

![](images/015e9642071090494608ed4b4517e70c1f5fedb123e878aeb457de2139f52a29.jpg)  
图4.6 send系统调用

有了上面的了解，再看源码就要容易多了，源码如下：

```c
//file: net/socket.c  
SYSCALL DEFINE4 send, int, fd, void __user *, buff, size_t, len, unsigned int, flags)  
{ return sys_sendto(fd, buff, len, flags, NULL, 0);  
}  
SYSCALL DEFINE6(……)  
{ //1.根据fd找到socket sock = sockfd.lookup_light(fd, &err, &fput_needed); //2.构造msghdr struct msghdr msg; struct ioveciov;ioviov_base = buff;ioviov_len = len; msg.msg_iovlen = 1; msg.msg_iov = &iov; msg.msg_flags = flags; //3.发送数据 sock_sendmsg(sock, &msg, len); 
```

从源码可以看到，在用户态使用的send函数和sendto函数其实都是sendto系统调用实现的。send只是为了方便，封装出来的一个更易于调用的方式而已。

在sendto系统调用里，首先根据用户传进来的socket句柄号来查找真正的socket内核对象。接着把用户请求的buff、len、flag等参数都统统打包到一个struct msghdr对象中。

接着调用了 sock_sendmsg $\coloneqq >$ _sock_sendmsg $\coloneqq = >$ _sock_sendmsg_nosec。在 sock_sendmsg_nosec中，函数调用将会由系统调用进入协议栈，我们来看它的源码。

```c
//file: net/socket.c   
static inline int __sock_sendmsg_nosec(..)   
{ return sock->ops->sendmsg(iocb, sock, msg, size);   
} 
```

通过3.2节的socket内核对象结构图可以看到，这里调用的是sock->ops->sendmsg，实际执行的是inet_sendmsg。这个函数是AF_INET协议族提供的通用发送函数。

# 4.4.2 传输层处理

# 传输层拷贝

在进入协议栈inet_sendmsg以后，内核接着会找到socket上的具体协议发送函数。对于TCP协议来说，那就是tcp_sendmsg（同样也是通过socket内核对象找到的）。

在这个函数中，内核会申请一个内核态的skb内存，将用户待发送的数据拷贝进去。注意，这个时候不一定会真正开始发送，如果没有达到发送条件，很可能这次调用直接就返回了，大概过程如图4.7所示。

![](images/8820d2af5968a7fff27f5f68b8ecbd40a5191b815bae248d58688a58ceb0c765.jpg)  
图4.7 传输层拷贝

我们来看inet_sendmsg函数的源码。

```c
//file: net/ipv4/af_inet.c  
int inlet_sendmsg(……)  
{ return sk->sk_prot->sendmsg(iocb, sk, msg, size); } 
```

在这个函数中会调用到具体协议的发送函数。同样参考3.2节里的socket内核对象结构图，可以看到对于TCP下的socket来说，sk->sk_prot->sendmsg指向的是tcp_sendmsg（对于UDP下的socket来说是udp_sendmsg）。

tcp_sendmsg这个函数比较长，分成多块来看。先看以下这一段。

```c
//file: net/ipv4/tcp.c  
int tcp_sendmsg(...){  
    while(...){  
        while(...){  
            //获取发送队列  
            skb = tcp_write_queueTAIL(sk);  
            //申请skb并拷贝  
            }  
        }  
}  
//file: include/net/tcp.h  
static inline struct sk BUFF *tcp_write_queueTAIL(const struct sock *sk)  
{  
    return skbpeekTAIL(&sk->sk_write_queue);  
} 
```

理解对socket调用tcp_write_queueTAIL是理解发送的前提。如上所示，这个函数是在获取socket发送队列中的最后一个skb。skb是struct sk_buffer对象的简称，用户的发送队列就是该对象组成的一个链表，如图4.8所示。

![](images/2c980c3295a15c1a8741e1bdda40adfb63649e67b14cfd28a50503cd1241bc9c.jpg)  
图4.8 socket发送队列

接着看 tcp_sendmsg的其他部分。

```txt
//file: net/ipv4/tcp.c 
```

int tcp_sendmsg(struct kiocb *iocb, struct sock *sk, struct msghdr *msg, size_t size)   
{ //获取用户传递过来的数据和标志iov $=$ msg->msg_iov; //用户数据地址iovlen $=$ msg->msg_iovlen; //数据块数为1flags $=$ msg->msg_flags; //各种标志//遍历用户层的数据块while(--iovlen $\Rightarrow$ 0）{ //待发送数据块的地址unsigned char _user \*from $=$ iov->iov_base;while（seglen>0）{ //需要申请新的skbif（copy $\Leftarrow$ 0）{ //申请skb，并添加到发送队列的尾部skb $=$ sk_STREAM_alloc_skb(sk,select_size(sk,sg),sk->sk_allocation);//把skb挂到socket的发送队列上skbEntail(sk,skb);1//skb中有足够的空间if（skb_availroom(sk)>0）{ //将用户空间的数据拷贝到内核空间，同时计算校验和//from是用户空间的数据地址skb_add_data_nocache(sk,skb,from,copy);

这个函数比较长，不过其实逻辑并不复杂。其中 msg->msgiov 存储的是用户态内存要发送的数据的buffer。接下来在内核态申请内核内存，比如 skb，并把用户内存里的数据拷贝到内核态内存中，如图4.9所示。这就会涉及一次或者几次内存拷贝的开销。

![](images/d50f9d669f08a44b16b738741484c1c8c97e6c435f633b7ccc1518ff0bf890c5.jpg)  
图4.9 发送队列新skb申请

至于内核什么时候真正把skb发送出去，在tcp_sendmsg中会进行一些判断。

```c
//file: net/ipv4/tcp.c  
int tcp_sendmsg(...){while(...){while(...){//申请内核内存并进行拷贝//发送判断if Forced.push(tp)){tcp_mark.push(tp,skb);\_tcp.push_pending_frames(sk,mss_now,TCP_NAGLE_PUSH);} else if (skb == tcp_send_head(sk))tcp.push_one(sk,mss_now);}continue;1} 
```

只有满足forced.push(tp)或者skb == tcp_send_head(sk)成立的时候，内核才会真正启动发送数据包。其中forced.push(tp)判断的是未发送的数据是否已经超过最大窗口的一半了。

条件都不满足的话，这次用户要发送的数据只是拷贝到内核就算完事了！

# 传输层发送

假设现在内核发送条件已经满足了，我们再来跟踪实际的发送过程。在上面的函数中，当满足真正发送条件的时候，无论调用的是__tcpishly pending_frames还是tcppush_one，最终都会实际执行到tcp_write_xmit。

所以直接从 tcp_write_xmit 看起，这个函数处理了传输层的拥塞控制、滑动窗口相关的工作。满足窗口要求的时候，设置 TCP 头然后将 skb 传到更低的网络层进行处理。传输层发送流程总图如图 4.10 所示。

我们来看看tcp_write_xmit的源码。

```c
//file: net/ipv4/tcp_output.c  
static bool tcp_write_xmit(struct sock *sk, unsigned int mss_now, int nonagle, int push_one, gfp_t gfp)  
{ //循环获取待发送skb while ((skb = tcp_send_head(sk))) { //滑动窗口相关 cwnd_quota = tcp_cwnd_test(tp, skb); tcp_snd_wnd_test(tp, skb, mss_now); 
```

![](images/a7b36deb73fa1a6f9c99965bda6d12f6fea428c52c68b7edc73d884253a80d52.jpg)  
图4.10 传输层发送流程总图

可以看到之前在网络协议里学的滑动窗口、拥塞控制就是在这个函数中完成的，这部分就不过多展开了，感兴趣的读者自己找这段源码来读。这里只看发送主过程，那就走到了tcp_transmit Skyl。

```c
//file: net/ipv4/tcp_output.c  
static int tcp_transmit_SKb(struct sock *sk, struct sk BUFF *skb, int clone_it, gfp_t gfp_mask)  
{ //1.克隆新skb出来 if (likely(clone_it)) { skb = skbClone(skb, gfp_mask); } //2.封装TCP头 th = tcp hdr(skb); th->source = inlet->inet_sport; th->dest = inlet->inet_dport; th->window = ...; th->urg = ...; 
```

# //3.调用网络层发送接口

```c
err = icsk->icsk_a f ops->queue_xmit(skb, &inet->cork.f1); 
```

第一件事是先克隆一个新的skb，这里重点说说为什么要复制一个skb出来。

这是因为skb后续在调用网络层，最后到达网卡发送完成的时候，这个skb会被释放掉。而我们知道TCP协议是支持丢失重传的，在收到对方的ACK之前，这个skb不能被删除。所以内核的做法就是每次调用网卡发送的时候，实际上传递出去的是skb的一个拷贝。等收到ACK再真正删除。

第二件事是修改skb中的TCP头，根据实际情况把TCP头设置好。这里要介绍一个小技巧，skb内部其实包含了网络协议中所有的头（header）。在设置TCP头的时候，只是把指针指向skb的合适位置。后面设置IP头的时候，再把指针挪一挪就行，如图4.11所示。避免频繁的内存申请和拷贝，效率很高。

![](images/1c8b1bebd9f1397dfa01fd946523e1ee68eb00d7032847b886b1d7c776c0e4ac.jpg)  
图4.11 skb

tcp_transmit_skb是发送数据位于传输层的最后一步，接下来就可以进入网络层进行下一层的操作了。调用了网络层提供的发送接口icsk->icsk.af ops->queue_xmit()。

在下面这个源码中，可以看出queue_xmit其实指向的是ip_queue_xmit函数。

```txt
//file: net/ipv4/tcp_ipv4.c  
const struct inlet_connection_sock.af ops ipv4specific = {  
    .queue_xmit = ip_queue_xmit,  
    .send_check = tcp_v4_send_check,  
    .......  
} 
```

自此，传输层的工作也就都完成了。数据离开了传输层，接下来将会进入内核在网络层的实现。

# 4.4.3 网络层发送处理

Linux内核网络层的发送的实现位于 net/ipv4/ipv_output.c 这个文件。传输层调用到的 ip_queue_xmit 也在这里。（从文件名上也能看出来进入IP层了，源文件名已经从tcp_xxx 变成了ip_xxx。）

在网络层主要处理路由项查找、IP头设置、netfilter过滤、skb切分（大于MTU的话）

等几项工作，处理完这些工作后会交给更下一层的邻居子系统来处理。网络层发送处理过程如图4.12所示。

![](images/1c13adbb21026bc95bb1ac22abde63a9c69b0d5358bbee2d6e5659bac7ea62c2.jpg)  
图4.12 网络层发送处理

我们来看网络层入口函数ip_queue_xmit的源码。

```c
//file: net/ipv4/ip_output.c  
int ip_queue_xmit(struct sk BUFF *skb, struct flowi *f1)  
{  
//检查 socket中是否有缓存的路由表  
rt = (struct rtable *)_sk.dst_check(sk, 0);  
if (rt == NULL) {  
//没有缓存则展开查找  
//查找路由项，并缓存到socket中  
rt = ip-route_outputPorts();  
sk_setupCaps(sk, &rt->dst);  
}  
//为skb设置路由表  
skb.dst_set_noref(skb, &rt->dst);  
//设置IP头  
iph = ip hdr(skb);  
iph->protocol = sk->sk_protocol;  
iph->ttl = ip_select_ttl(inet, &rt->dst);  
iph->frag_off = ...; 
```

```txt
//发送ip_local_out(skb);1 
```

ip_queue_xmit 已经到了网络层，在这个函数里我们看到了网络层相关的功能路由项查找，如果找到了则设置到skb上（没有路由的话就直接报错返回了）。

在Linux上通过route命令可以看到本机的路由配置，如图4.13所示。

![](images/9c91d175d81c64235b8360dfe241839c810f59d4fc32868f94dd42f83e8682ca.jpg)  
图4.13 本机路由配置

在路由表中，可以查到某个目的网络应该通过哪个 Iface（网卡）、哪个 Gateway（网关）发送出去。查找出来以后缓存到socket上，下次再发送数据就不用查了。

接着把路由表地址也放到skb里。

```txt
//file: include/linux/skbuff.h  
struct sk_buffer{ //保存了一些路由相关信息 unsigned long __skb_refdst; } 
```

接下来就是定位到skb里的IP头的位置，然后开始按照协议规范设置IP头，如图4.14所示。

![](images/37a3e6cb97e838c93b996d9675948a9477440888134646e83572a36ead853191.jpg)  
图4.14 skb

再通过ip_local_out进入下一步的处理。

```c
//file: net/ipv4/ip_output.c  
int ip_local_out(struct sk_buffer *skb)  
{ //执行 netfilter 过滤 err = __ip_local_out(skb); 
```

//开始发送数据

```txt
if (likely(err == 1))  
    err = dst_output(skb); 
```

在调用ip_local_out $= >$ _ip_local_out $= >$ nf hook的过程中会执行netfilter过滤。如果使用iptables配置了一些规则，那么这里将检测是否命中规则。如果你设置了非常复杂的 netfilter 规则，在这里这个函数将会导致你的进程CPU开销大增。

还是不多展开，继续只探讨和发送有关的过程dst_output。

```c
//file: include/net/dst.h  
static inline int dst_output(struct sk_buffer *skb)  
{  
    return skb.dst(skb) ->output(skb);  
} 
```

此函数找到这个skb的路由表（dst条目），然后调用路由表的output方法。这又是一个函数指针，指向的是ip_output方法。

```c
//file: net/ipv4/ip_output.c  
int ip_output(struct sk BUFF *skb)  
{ //统计  
......  
//再次交给netfilter，完毕后回调ip_finish_output  
return NFHOOK_COND(NFPROTO_IPV4, NF_INET_POST_ROUTING, skb, NULL, dev, ip_finish_output, !(IPCB(skb) -> flags & IPSKB_RERouted));  
} 
```

在ip_output中进行一些简单的统计工作，再次执行netfilter过滤。过滤通过之后回调ip_finish_output。

```c
//file: net/ipv4/ip_output.c  
static int ip_finish_output(struct sk BUFF *skb)  
{ //大于MTU就要进行分片了  
if (skb->len > ip_skb.dst_htu(skb) && !skb_is_gso(skb))  
return ip_structure(skb, ip_finish_output2);  
else  
return ip_finish_output2(skb);  
}
```

在ip_finish_output中可以看到，如果数据大于MTU，是会执行分片的。

实际MTU大小通过MTU发现机制确定，在以太网中为1500字节。QQ研发团队在早期，会尽量控制自己的数据包尺寸小于MTU，通过这种方式来优化网络性能。因为分片会带来两个问题：1.需要进行额外的切分处理，有额外性能开销；2.只要一个分片丢失，整个包都要重传。所以避免分片既杜绝了分片开销，也大大降低了重传率。

在ip_finish_output2中，发送过程终于进入下一层，邻居子系统。

```c
//file: net/ipv4/ip_output.c  
static inline int ip_finish_output2(struct sk BUFF *skb)  
{ //根据下一跳的IP地址查找邻居项，找不到就创建一个  
nexthop = (_force u32) rt_nexthop(rt, ip hdr(skb) ->daddr);  
neigh = __ipv4_neighLOOKup_noref(dev, nexthop);  
if (unlikely(!neigh))  
    neigh = __neigh_create(&arp_tbl, &nexthop, dev, false);  
//继续向下层传递  
int res = dst_neigh_output(dst, neigh, skb);  
} 
```

# 4.4.4 邻居子系统

邻居子系统是位于网络层和数据链路层中间的一个系统，其作用是为网络层提供一个下层的封装，让网络层不必关心下层的地址信息，让下层来决定发送到哪个MAC地址。

而且这个邻居子系统并不位于协议栈 net/ipv4/目录内，而是位于net/core/neighbour.c。因为无论是对于IPv4还是IPv6，都需要使用该模块，如图4.15所示。

![](images/eb1fd67a2e7559e4325bfb6a2d6c1079f95c75604028f517406b676b819bc1fd.jpg)  
图4.15 邻居子系统位置

在邻居子系统里主要查找或者创建邻居项，在创建邻居项的时候，有可能会发出实际的arp请求。然后封装MAC头，将发送过程再传递到更下层的网络设备子系统。大致流程如图4.16所示。

![](images/f76b497881a5a4ace66b648a89ae8462e9f6c431202697a6750cb69eab86b056.jpg)  
图4.16 邻居子系统

理解了大致流程后，再回头看源码。在上面的ip_finish_output2源码中调用了__ipv4_neighLOOKUP_noref。它在arp缓存中进行查找，其第二个参数传入的是路由下一跳IP信息。

//file: include/net/arp.h   
extern struct neigh_table arp_tbl;   
static inline struct neighbour \*_ipv4_neighLOOKUP_noref( struct net_device \*dev, u32 key)   
{ struct neigh_hash_table \*nht $=$ rcu_dereference_bh(arp_tbl.nht); //计算哈希值，加速查找 hash_val $=$ arp_hashfn(.......); for (n $=$ rcu_dereference_bh(nht->hash_buckets[hash_val]); n != NULL; n $=$ rcu_dereference_bh(n->next)) { if (n->dev $= =$ dev && \*(u32\*)n->primary_key == key) return n; }   
}

如果找不到，则调用 __neigh_create 创建一个邻居。

```c
//file: net/core/neighbour.c  
struct neighbour *_neigh_create(……)  
{  
//申请邻居表项  
struct neighbour *n1, *rc, *n = neigh_alloc(tb1, dev); 
```

//构造赋值

```c
memcpy(n->primary_key, pkey, key_len);  
n->dev = dev;  
n->parms->neigh_setup(n); 
```

//最后添加到邻居哈希表中

```c
rcu_assign_pointer(nht->hash_buckets[hash_val], n); 
```

有了邻居项以后，此时仍然不具备发送IP报文的能力，因为目的MAC地址还未获取。调用dst_neigh_output继续传递skb。

//file: include/net/dst.h

```c
static inline int dst_neigh_output(struct dst_entry *dst, struct neighbour *n, struct sk_buffer *skb) { return n->output(n, skb); } 
```

调用output，实际指向的是neigh Resolve_output。在这个函数内部有可能发出arp网络请求。

```txt
//file: net/core/Neighbour.c  
int neigh Resolve_output(){ 
```

//注意：这里可能会触发arp请求

```c
if(!neigh_event_send(neigh,skb)){ //neigh->ha是MAC地址 dev-hard_header(skb，dev，ntohs(skb->protocol)， neigh->ha，NULL，skb->len); //发送 dev_queue_xmit(skb); }
```

当获取到硬件MAC地址以后，就可以封装skb的MAC头了。最后调用dev_queue_xmit将skb传递给Linux网络设备子系统。

# 4.4.5 网络设备子系统

邻居子系统通过dev_queue_xmit进入网络设备子系统。网络设备子系统的工作流程如图4.17所示。

![](images/7abbbd7b9db5737228eedc2870626554bed167ebb9eec5227ce722630e5d64b4.jpg)  
图4.17 网络设备子系统

我们从dev_queue_xmit来看起。

```c
//file: net/core/dev.c  
int dev_queue_xmit(struct sk BUFF *skb)  
{ //选择发送队列 txq = netdev_pick_tx(dev, skb); //获取与此队列关联的排队规则 q = rcu_dereference_bh(txq->qdisc); //如果有队列，则调用_dev_xmit_skb继续处理数据 if (q->enqueue) { rc = _dev_xmit_skb(sk, q, dev, txq); goto out; } //没有队列的是回环设备和隧道设备
```

在4.3节里讲过，网卡是有多个发送队列的（尤其是现在的网卡）。上面对netdev_pick_tx函数的调用就是选择一个队列进行发送。

netdev_pick_tx发送队列的选择受XPS等配置的影响，而且还有缓存，也是一小套复杂的逻辑。这里我们只关注两个逻辑，首先会获取用户的XPS配置，否则就自动计算了。代码见 netdev_pick_tx下的__netdev_pick_tx函数。

```c
//file: net/core/flow_dissector.c  
u16 _netdev_pick_tx(struct net_device *dev, struct sk BUFF *skb)  
{ //获取XPS配置  
int new_index = get_xps_queue(dev, skb);  
//自动计算队列  
if (new_index < 0)  
    new_index = skb_tx_hash(dev, skb);} 
```

然后获取与此队列关联的qdisc。在Linux上通过tc命令可以看到qdisc类型，例如对于我的某台多队列网卡机器是mq disc。

```txt
#tc qdisc
qdisc mq 0: dev eth0 root 
```

大部分的设备都有队列（回环设备和隧道设备除外），所以现在进入__dev_xmit_skb。

```c
//file: net/core/dev.c  
static inline int __dev_xmit_skb(struct sk BUFF *skb, struct Qdisc *q, struct net_device *dev, struct netdev_queue *txq)  
{ 
```

```c
//1. 如果可以绕开排队系统
if ((q->flags & TCQ_F_CAN_BYPASS) && !qdisc_qlen(q) && qdisc_run_begin(q)) {
......
}
```

```txt
//2.正常排队  
else{//入队q->enqueue(skb，q) //开始发送_qdisc_run(q);1}
```

上述代码中分两种情况，一种是可以 bypass（绕过）排队系统，另外一种是正常排队。我们只看第二种情况。

先调用q->enqueue把skb添加到队列里，然后调用_qdisc_run开始发送。

```txt
//file: net/sched/sch.Generic.c  
void __qdisc_run(struct Qdisc *q) 
```

int quota $=$ weight_p;   
//循环从队列取出一个skb并发送 while(qdisc_restart(q)) { //如果发生下面情况之一，则延后处理： //1. quota用尽 //2.其他进程需要CPU if（--quota $\text{一} = 0$ |need_resched()）{//将触发一次NET_TX_SOFTIRQ类型softirq _netif_schedule(q); break; }   
1

在上述代码中可以看到，while循环不断地从队列中取出skb并进行发送。注意，这个时候其实都占用的是用户进程的系统态时间(sy)。只有当quota用尽或者其他进程需要CPU的时候才触发软中断进行发送。

所以这就是为什么在服务器上查看/proc/softirqs，一般NET_RX都要比NET_TX大得多的第二个原因。对于接收来说，都要经过NET_RX软中断，而对于发送来说，只有系统态配额用尽才让软中断上。

我们来把注意力再放到 qdisc_restart 上，继续看发送过程。

static inline int qdisc_restart(struct Qdisc *q)   
{ //从 qdisc中取出要发送的skb skb $=$ dequeue_skb(q); return sch_direct_xmit(skb,q,dev,txq,root_lock); } qdisc_restart从队列中取出一个skb，并调用sch_direct_xmit继续发送。

```c
//file: net/sched/sch_generic.c  
int sch_direct_xmit(struct sk BUFF *skb, struct Qdisc *q, struct net_device *dev, struct netdev_queue *txq, spinlock_t *root_lock)  
{ //调用驱动程序来发送数据 ret = dev-hard_start_xmit(skb, dev, txq); } 
```

# 4.4.6 软中断调度

在4.4.5节我们看到了如果发送网络包的时候系统态CPU用尽了，会调用__netif_schedule触发一个软中断。该函数会进入__netif_reschedule，由它来实际发出NET_TX_SOFTIRQ类型软中断。

软中断是由内核进程来运行的，该进程会进入net_tx_action函数，在该函数中能获取发送队列，并也最终调用到驱动程序里的入口函数dev-hard_start_xmit，如图4.18所示。

![](images/7baa9cbf5187fad3c88c6d74541591dd622fdbeac31be8d07a77c0c5cefb7ffc.jpg)  
图4.18 网络发送软中断调度

```c
//file: net/core/dev.c
static inline void __netif_reschedule(struct Qdisc *q)
{
    sd = &get_cpu_var(softnet_data);
    q->next_sched = NULL;
    *sd->output_queueTAILP = q;
    sd->output_queueTAILP = &q->next_sched;
    ...
    ...
    raisesoftirq_irqoff(NET_TX_SOFTIRQ);
} 
```

在该函数里软中断能访问到的softnet_data设置了要发送的数据队列，添加到output_queue里了。紧接着触发了NET_TX_SOFTIRQ类型的软中断。（T代表transmit，传输。）

软中断的入口代码这里也不详细讲了，2.2.3节已经讲过。这里直接从NET_TX_SOFTIRQsoftirq注册的回调函数net_tx_action讲起。用户态进程触发完软中断之后，会有一个软中断内核线程执行到net_tx_action。

牢记，这以后发送数据消耗的CPU就都显示在si这里，不会消耗用户进程的系统时间。

```c
//file: net/core/dev.c
static void net_tx_action(struct softirqslication *h)
{
    //通过softnet_data获取发送队列
    struct softnet_data *sd = &get_cpu_var(softnet_data);
    //如果output queue上有qdisc
    if (sd->output_queue) {
        //将head指向第一个qdisc
        head = sd->output_queue;
        //遍历qdsics列表
        while (head) {
            struct Qdisc *q = head;
            head = head->next_sched;
            //发送数据
            qdisc_run(q);
        }
    }
} 
```

软中断这里会获取softnet_data。前面我们看到进程内核态在调用_netif_reschedule的时候把发送队列写到softnet_data的output_queue里了。软中断循环遍历sd->output_queue发送数据帧。

下面来看qdisc_run，它和进程用户态一样，也会调用_qdisc_run。

```c
//file: include/net/pkt_sched.h  
static inline void qdisc_run(struct Qdisc *q)  
{ if (qdisc_run_begin(q)) __qdisc_run(q); } 
```

然后也是进入 qdisc_restart $= >$ sch_direct_xmit，直到进入驱动程序函数dev-hard_start_xmit。

# 4.4.7 igb网卡驱动发送

通过前面的介绍可知，无论对于用户进程的内核态，还是对于软中断上下文，都会调用网络设备子系统中的dev-hard_start_xmit函数。在这个函数中，会调用到驱动里的发送函数igb_xmit_frame。

在驱动函数里，会将skb挂到RingBuffer上，驱动调用完毕，数据包将真正从网卡发送出去。网卡驱动工作流程如图4.19所示。

![](images/2767ed7bed6ad021bd11ee48e5b44d07a31a42105cf737af9919f0eb2c0bb5f8.jpg)  
图4.19 网卡驱动工作流程

我们来看看实际的源码。

```c
//file: net/core/dev.c  
int dev-hard_start_xmit(struct sk BUFF *skb, struct net_device *dev, struct netdev_queue *txq) 
```

```txt
//获取设备的回调函数集合 ops  
const struct net_deviceOps *ops = dev->netdevOps;
```

```javascript
//获取设备支持的功能列表  
features = netif_skb_features(skb);
```

```javascript
//调用驱动的ops里的发送回调函数ndo_start_xmit将数据包传给网卡设备skb_len = skb->len; 
```

rc $=$ ops->ndo_start_xmit(skb,dev);其中ndo_start_xmit是网卡驱动要实现的一个函数，是在net_deviceOps中定义的。

```txt
//file: include/linux/netdevice.h
struct net_deviceOps {
    netdev_tx_t (*ndo_start_xmit) (struct sk BUFF *skb, struct net_device *dev);
} 
```

在igb网卡驱动源码中找到了net_device_op参数。

//file:drivers/net/ethernet/intel/igb/igb_main.c   
```txt
static const struct net_deviceOps igb_netdevOps = { ndo_open = igb_open, ndo_stop = igb_close, ndo_start_xmit = igb_xmit_frame, }; 
```

也就是说，对于网络设备层定义的ndo_start_xmit，igb的实现函数是igb_xmit_frame。这个函数是在网卡驱动初始化的时候被赋值的。具体初始化过程参见2.2.2节。所以在上面网络设备层调用ops->ndo_start_xmit的时候，实际会进入igb_xmit_frame这个函数。我们进入这个函数来看看驱动程序是如何工作的。

//file:drivers/net/ethernet/intel/igb/igb_main.c   
static netdev_tx_t igb_xmit_frame(struct sk_buffer \*skb, struct net_device \*netdev)   
{ return igb_xmit_frame_ring(sk，igb_tx_queue_mapping(adapter，skb));   
}   
netdev_tx_t igb_xmit_frame_ring(struct sk_buffer \*skb, struct igb_ring \*tx_ring)   
{ //获取TX Queue中下一个可用缓冲区信息 first $=$ &tx_ring->tx_buffer_info[tx_ring->next_to_use]; first->skb $=$ skb; first->bytecount $=$ skb->len; first->gso_segs $= 1$ . //igb_tx_map函数准备给设备发送的数据 igb_tx_map(tx_ring，first，hdr_len);   
}

在这里从网卡的发送队列的RingBuffer中取下来一个元素，并将skb挂到元素上，如图4.20所示。

![](images/84eb1bd6eaa41712e478b40c4be986962edca0e0362cd633820f7d9ee39bf020.jpg)  
图4.20 传输队列RingBuffer中的skb

igb_tx_map函数将skb数据映射到网卡可访问的内存DMA区域。

```c
//file:drivers/net/ethernet/intel/igb/igb_main.c
static void igb_tx_map(struct igb_ring *tx_ring,
						struct igb_tx_buffer *first,
					 const u8 hdr_len)
\{
	//获取下一个可用描述符指针
	tx_desc = IGB_TX_DESC(tx_ring, i);
	//为skb->data构造内存映射，以允许设备通过DMA从RAM中读取数据
 dma = dma_map_single(tx_ring->dev, skb->data, size, DMA_TO_DEVICE);
	//遍历该数据包的所有分片，为skb的每个分片生成有效映射
	for (frag = &skb_shinfo(skb) ->frags[0];; frag++) \{
		tx_desc->read.buffer_addr = cpu_to_1e64DMA);
		tx_desc->read.cmd_type_len = ...;
		tx_desc->read.olinfo_status = 0;
	\}
	//设置最后一个descriptor
_cmd_type |= size | IGB_TXD_DCMD;
	tx_desc->read.cmd_type_len = cpu_to_1e32(cmd_type);
\} 
```

当所有需要的描述符都已建好，且skb的所有数据都映射到DMA地址后，驱动就会进入到它的最后一步，触发真实的发送。

# 4.5 RingBuffer内存回收

当数据发送完以后，其实工作并没有结束。因为内存还没有清理。当发送完成的时候，网卡设备会触发一个硬中断来释放内存。在第2章中，详细讲述过硬中断和软中断的处理过程。在发送硬中断的过程里，会执行RingBuffer内存的清理工作，如图4.21所示。

![](images/bf5d65631153d83b5f07abccd632f13c6142d0bd21b67ed4d37ad7ffc98f1881.jpg)  
图4.21 RingBuffer 回收

再回头看一下硬中断触发软中断的源码。

```c
//file: drivers/net/ethernet/intel/igb/igb_main.c  
static inline void __napi_schedule(...){  
    list_addTAIL(&napi->poll_list, &sd->poll_list);  
    __raisesoftirq_irqoff(NET_RX_SOFTIRQ);  
} 
```

这里有个很有意思的细节，无论硬中断是因为有数据要接收，还是发送完成通知，从硬中断触发的软中断都是NET_RX_SOFTIRQ。这个在4.1节讲过了，它是软中断统计中RX要高于TX的一个原因。

好，我们接着进入软中断的回调函数igb_poll。在这个函数里，有一行igb_clean_tx_irq，参见以下源码。

```c
//file: drivers/net/ethernet/intel/igb/igb_main.c  
static int igb_poll(struct napi_struct *napi, int budget)  
{ //performs the transmit completion operations 
```

if (q_vector->tx.ring) clean_COMPLETE $=$ igb_clean_tx_irq(q_vector);

我们来看看当传输完成的时候，igb.clean_tx_irq都干什么了。

```c
//file:drivers/net/ethernet/intel/igb/igb_main.c  
static bool igb_clean_tx_irq(struct igb_q_vector *q_vector)  
{ //释放skb dev_kfree_SKb_any(tx_buffer->skb); //清除tx_buffer数据 tx_buffer->skb = NULL; dma_unmap_len_set(tx_buffer, len, 0); //清除最后的DMA位置，解除映射 while (tx_desc != eop_desc) { } 
```

无非就是清理了skb，解除了DMA映射，等等。到了这一步，传输才算是基本完成了。

为什么说是基本完成，而不是全部完成了呢？因为传输层需要保证可靠性，所以skb其实还没有删除。它得等收到对方的ACK之后才会真正删除，那个时候才算彻底发送完毕。

# 4.6 本章总结

下面用一张图总结整个发送过程，见图4.22。

了解了整个发送过程以后，我们再来回顾本章开篇提到的几个问题。

# 1）我们在监控内核发送数据消耗的CPU时，应该看sy还是si？

在网络包的发送过程中，用户进程（在内核态）完成了绝大部分的工作，甚至连调用驱动的工作都干了。只当内核态进程被切走前才会发起软中断。发送过程中，绝大部分（ $90\%$ ）以上的开销都是在用户进程内核态消耗掉的。

只有一少部分情况才会触发软中断（NET_TX类型），由软中断 ksoftirqd内核线程来发送。

所以，在监控网络IO对服务器造成的CPU开销的时候，不能仅看si，而是应该把si、sy都考虑进来。

![](images/adf2c08dd796d70e0b8e0bf4351eeb5a1dc62d9af350e36e8723f85b56ad4cce.jpg)  
图4.22 网络发送过程汇总

# 2）在服务器上查看/proc/softirqs，为什么NET_RX要比NET_TX大得多的多？

之前我认为NET_RX是接收，NET_TX是传输。对于一个既收取用户请求，又给用户返回的服务器来说，这两块的数字应该差不多才对，至少不会有数量级的差异。但事实上，我手头的一台服务器是图4.23这样的。

经过本章的源码分析，发现造成这个问题的原因有两个。

第一个原因是当数据发送完以后，通过硬中断的方式来通知驱动发送完毕。但是硬中断无论是有数据接收，还是发送完毕，触发的软中断都是NET_RX_SOFTIRQ，并不是NET_TX_SOFTIRQ。

![](images/0156fed1ff779fb1f5fe74477f9b1aaf3cc37bb4456c3f70883aa51d05fd99f0.jpg)  
图4.23 软中断查看

第二个原因是对于读来说，都是要经过NET_RX软中断的，都走ksoftirqd内核线程。而对于发送来说，绝大部分工作都是在用户进程内核态处理了，只有系统态配额用尽才会发出NET_TX，让软中断上。

综合上述两个原因，那么在机器上查看NET_RX比NET_TX大得多就不难理解了。

# 3）发送网络数据的时候都涉及哪些内存拷贝操作？

这里的内存拷贝，只特指待发送数据的内存拷贝。

第一次拷贝操作是在内核申请完skb之后，这时候会将用户传递进来的buffer里的数据内容都拷贝到skb。如果要发送的数据量比较大，这个拷贝操作开销还是不小的。

第二次拷贝操作是从传输层进入网络层的时候，每一个skb都会被克隆出来一个新的副本。目的是保存原始的skb，当网络对方没有发回ACK的时候，还可以重新发送，以实现TCP中要求的可靠传输。不过这次只是浅拷贝，只拷贝skb描述符本身，所指向的数据还是复用的。

第三次拷贝不是必需的，只有当IP层发现skb大于MTU时才需要进行。此时会再申请额外的skb，并将原来的skb拷贝为多个小的skb。

这里插个题外话，大家在谈论网络性能优化中经常听到“零拷贝”，我觉得这个词有一点点夸张的成分。TCP为了保证可靠性，第二次的拷贝根本就没法省。如果包大于MTU，分片时的拷贝同样避免不了。

看到这里，相信内核发送数据包对你来说，已经不再是一个完全不懂的黑盒了。本章哪怕你只看懂十分之一，也已经掌握了这个黑盒的打开方式。将来优化网络性能时，你就会知道从哪儿下手了。

# 4）零拷贝到底是怎么回事？

是的，本章通篇还没有讲过“零拷贝”。但是我们已经把Linux在发送网络数据包时的所有内存拷贝操作都介绍了一遍，理解了这个再去理解“零拷贝”就容易得多，这里只拿sendfile系统调用来举例。

如果想把本机的一个文件通过网络发送出去，我们的做法之一就是先用 read系统调

用把文件读取到内存，然后再调用send把文件发送出去。

假设数据之前从来没有读取过，那么read硬盘上的数据需要经过两次拷贝才能到用户进程的内存。第一次是从硬盘DMA到Page Cache。第二次是从Page Cache拷贝到用户内存。send系统调用在前面讲过了。那么read + send系统调用发送一个文件出去数据需要经过的拷贝过程如图4.24所示。

![](images/57d54ac6b024720cea2ac9a1368845371116de4f2d1c4c003056533ae65dac5a.jpg)  
图4.24 read + send系统调用发送文件经过的拷贝过程

如果要发送的数据量比较大，那需要花费不少的时间在大量的数据拷贝上。前面提到的sendfile就是内核提供的一个可用来减少发送文件时拷贝开销的一个技术方案。在sendfile系统调用里，数据不需要拷贝到用户空间，在内核态就能完成发送处理，如图4.25所示，这就显著减少了需要拷贝的次数。

![](images/1c7f2fb41cdd1a6428dc52f8744cec56d1dfd806480258bc37b18d5c09245c00.jpg)  
图4.25 sendfile系统调用发送文件的过程

# 5）为什么Kafka的网络性能很突出？

大家一定对Kafka出类拔萃的性能有所耳闻。当然，Kafka高性能的原因有很多，其中的重要原因之一就是采用了sendfile系统调用来发送网络数据包，减少了内核态和用户态之间的频繁数据拷贝。

![](images/03bba6cc28023b1bce9d513582c3100ad211b856b4c7d07824bd12ae3542c3a7.jpg)

# 5.1 相关实际问题

前面的章节深度分析了网络包的接收，也拆分了网络包的发送，总之收发流程算是闭环了。不过还有一种特殊的情况没有讨论，那就是接收和发送都在本机进行。而且实践中这种本机网络IO出现的场景还不少，而且还有越来越多的趋势。例如LNMP技术栈中的nginx和php-fpm进程就是通过本机来通信的，还有就是最近流行的微服务中sidecar模式也是本机网络IO。

所以，我想如果能深度理解这个问题，在实践中将非常有意义。按照习惯，我们还是从几个实际中的问题引入。

# 1）127.0.0.1 本机网络IO需要经过网卡吗？

在跨机网络IO中，数据包肯定都是要经过网卡发送出去的。那么，在本机网络IO的情况下，收发数据需要经过网卡吗？如果把网卡拔了，127.0.0.1上数据收发能否正常工作？

# 2）数据包在内核中是什么走向，和外网发送相比流程上有什么差别？

假如本机网络IO和跨机IO收发流程不一样，那么是在哪几个环节上不同呢？

# 3）访问本机服务时，使用127.0.0.1能比使用本机IP（例如192.168.x.x）更快吗？

实际上，使用本机IO通信的时候也有两种方法。一种方法是用127.0.0.1，一种方法是使用本机IP，例如192.168.x.x这种。那么这两种方法在性能上会有什么差异吗？哪种方法性能更好呢？

铺垫完毕，拆解正式开始！！！

# 5.2 跨机网络通信过程

在开始讲述本机通信过程之前，还是先来回顾跨机网络通信。

# 5.2.1 跨机数据发送

在第4章中介绍了数据包的发送过程。如图5.1所示，从send系统调用开始，直到网卡把数据发送出去。

如图5.1所示，用户数据被拷贝到内核态，然后经过协议栈处理后进入RingBuffer。随后网卡驱动真正将数据发送了出去。当发送完成的时候，是通过硬中断来通知CPU，然后清理RingBuffer。从代码的视角得到的流程如图5.2所示。

![](images/def23ef565755770e0ee92c48c7b90abb4cab2d1a386b3d136013546ef6f0f6a.jpg)  
图5.1 数据发送流程

![](images/c67d492b836cbaa8493122231166564d572cf2bd89d6486a5e514b189bf49542.jpg)  
图5.2 数据发送源码

# 协议栈

# 传输层

```c
//file: net/ipv4/af_inet.c  
int inlet_sendmsg(……)  
{  
    return sk->sk_prot->sendmsg(iocb, sk, msg, size);  
}  
//file: net/ipv4/tcp.c  
int tcp_sendmsg(……)  
{  
    …  
}  
//file: net/ipv4/tcp_output.c  
static int tcp_transmit_SKb(……)  
{  
    //封装TCP头  
    th = tcp hdr(skb);  
    th->source = inlet->inet_sport;  
    th->dest = inlet->inet_dport;  
    //调用网络层发送接口  
    err = icsk->icsk.afOps->queue_xmit(skb); 
```

# 网络层

```lisp
//file: net/ipv4/ip_output.c  
int ip_queue_xmit(struct sk BUFF *skb, struct flowi *fl)  
{  
    res = ip_local_out(skb);  
}  
//file: net/ipv4/ip_output.c  
static inline int ip_finish_output2(struct sk BUFF *skb)  
{  
    //继续向下层传递  
    int res = dst_neigh_output(dst, neigh, skb);  
} 
```

# 邻居子系统

![](images/29506f537fbf6990ff7649b78ea62b267b3352a78e6515b518cdd6bb06f0199d.jpg)  
图5.2 （续）

```c
//file: include/net/dst.h  
static inline int dst_neigh_output(...{ return neigh_hh_output(hh, skb); } //file: include/net/neighbour.h  
static inline int neigh_hh_output(...{ skb.push(skb, hh_len); return dev_queue_xmit(skb); } 
```

![](images/3327671652c85744e17dae307ccb797bd19ad7781d7cde06d7823dcb378d2985.jpg)  
图5.2 （续）

等网络发送完毕，网卡会给CPU发送一个硬中断来通知CPU。收到这个硬中断后会释放RingBuffer中使用的内存，如图5.3所示。

![](images/b3cf9206e545e71ddde880e674f029a07c2957881dd8905b97c48e3c06d8c195.jpg)  
图5.3 RingBuffer清理

# 5.2.2 跨机数据接收

在第2章中介绍了数据接收过程。当数据包到达另外一台机器的时候，Linux 数据包的接收过程开始了，如图5.4所示。

![](images/dbee6dbb5de597e0f4cefb0e104fe990af32bda3f927db1e0fb2b75024b939d2.jpg)  
图5.4 接收过程

当网卡收到数据以后，向CPU发起一个中断，以通知CPU有数据到达。当CPU收到中断请求后，会去调用网络驱动注册的中断处理函数，触发软中断。ksoftirqd检测到有软中断请求到达，开始轮询收包，收到后交由各级协议栈处理。当协议栈处理完并把数据放到接收队列之后，唤醒用户进程（假设是阻塞方式）。

我们再同样从内核组件和源码视角看一遍，如图5.5所示。

![](images/69d3e7ef182397219ae9174fc53c3ef2e7f7b9ec3f7059affdf03e6d6414e5f4.jpg)  
图5.5 数据接收源码

![](images/f911805c3a3b25ba88f76bbc8b2833c8baef27fd7ec330017cc2620cc190c242.jpg)  
图5.5 （续）

# 5.2.3 跨机网络通信汇总

那么汇总起来，一次跨机网络通信的过程就如图5.6所示。

![](images/e85d28414eff312931e08c27ff682af00155e4e546d4f8e141b15f58cce8a457.jpg)  
图5.6 单次跨机网络通信过程

# 5.3 本机发送过程

5.2节介绍了跨机时整个网络的发送过程，在本机网络IO的过程中，流程会有一些差别。为了突出重点，将不再介绍整体流程，而是只介绍和跨机逻辑不同的地方。有差异的地方总共有两处，分别是路由和驱动程序。

# 5.3.1 网络层路由

发送数据进入协议栈到达网络层的时候，网络层入口函数是ip_queue_xmit。在网络层里会进行路由选择，路由选择完毕，再设置一些IP头，进行一些netfilter的过滤，将包交给邻居子系统。网络层工作流程如图5.7所示。

对于本机网络IO来说，特殊之处在于在local路由表中就能找到路由项，对应的设备都将使用loopback网卡，也就是常说的lo设备。

![](images/f66600e730ee86940ae338638c0aa59962cd365e366afba4212ce92b4b562ea4.jpg)  
图5.7 网络层路由

下面详细看看路由网络层里这段路由相关工作过程。从网络层入口函数ip_queue_xmit看起。

```c
//file: net/ipv4/ip_output.c  
int ip_queue_xmit(struct sk BUFF *skb, struct flowi *f1)  
{  
    //检查socket中是否有缓存的路由表  
    rt = (struct rtable *)_skDst_check(sk, 0);  
    if (rt == NULL) {  
        //没有缓存则展开查找  
        //查找路由项，并缓存到socket中  
        rt = ip-route_outputPorts();  
        sk_setupCaps(sk, &rt->dst);  
    }
```

查找路由项的函数是ip route_outputPorts，它又依次调用ip route_output_flow、ip route_output_key、fibLOOKUP函数。调用过程略过，直接看fib LOOKUP的关键代码。

```c
//file:include/net/ip_fib.h  
static inline int fibLOOKUP(struct net *net, const struct flowi4 *flp, struct fib_result *res) 
```

{

```txt
struct fib_table *table;
table = fib_get_table(net, RT_TABLE_LOCAL);
if (!fib_table.lookup(table, flp, res, FIB Lookup_NOREF)) return 0;
table = fib_get_table(net, RT_TABLE_MAIN);
if (!fib_table.lookup(table, flp, res, FIB Lookup_NOREF)) return 0;
return -ENETUNREACH; 
```

在fibLOOKUP中将会对local和main两个路由表展开查询，并且先查询local后查询main。我们在Linux上使用ip命令可以查看到这两个路由表，这里只看local路由表（因为本机网络IO查询到这个表就终止了）。

ip route list table local

local 10.143.x.y dev eth0 proto kernel scope host src 10.143.x.y

local 127.0.0.1 dev lo proto kernel scope host src 127.0.0.1

从上述结果可以看出，对于目的是127.0.0.1的路由在local路由表中就能够找到。fibLOOKUP的工作完成，返回_ip-route_output_key函数继续执行。

//file: net/ipv4/route.c

```c
struct rtable *__ip-route_output_key(struct net *net, struct flowi4 *fl4) {
    if (fibLOOKUP(net, fl4, &res)) {
        if (res.type == RTN_LOCAL) {
            dev_out = net->loopback_dev;
            return rth;
        }
    } 
```

对于本机的网络请求，设备将全部使用net->loopback_dev，也就是lo虚拟网卡。

接下来的网络层仍然和跨机网络IO一样，最终会经过ip_finish_output，进入邻居子系统的入口函数dst_neigh_output。

本机网络IO需要进行IP分片吗？因为和正常的网络层处理过程一样，会经过ip_finish_output函数，在这个函数中，如果skb大于MTU，仍然会进行分片。只不过lo虚拟网卡的MTU比Ethernet要大很多。通过ifconfig命令就可以查到，物理网卡MTU一般为1500，而lo虚拟接口能有65535个。

在邻居子系统函数中经过处理后，进入网络设备子系统（入口函数是dev_queue_xmit）。

# 5.3.2 本机IP路由

本章开篇提到的第3个问题的答案就在5.3.1节。但这个问题描述起来有点长，因此单独用一小节来讲。

问题：用本机IP（例如192.168.x.x）和用127.0.0.1在性能上有差别吗？

前面讲过，选用哪个设备是路由相关函数_ip-route_output_key确定的。

//file: net/ipv4/route.c   
```c
struct rtable *__ip-route_output_key(struct net *net, struct flowi4 *fl4) {
    if (fibLOOKUP(net, fl4, &res)) {
        if (res.type == RTN_LOCAL) {
            dev_out = net->loopback_dev;
            ...
        }
    }
    rth = __mkroute_output(&res, fl4, orig_oif, dev_out, flags);
    return rth;
} 
```

在fibLOOKUP函数里会查询到local路由表。

ip route list table local   
```batch
local 10.162.*.\* dev eth0 proto kernel scope host src 10.162.*.\*  
local 127.0.0.1 dev lo proto kernel scope host src 127.0.0.1 
```

很多人在看到这个路由表的时候就被它迷惑了，以为上面的10.162.*真的会被路由到eth0（其中10.162.*是我的本机局域网IP，我把后面两段用*号隐藏起来了）。

但其实内核在初始化local路由表的时候，把local路由表里所有的路由项都设置成了RTN_LOCAL，不只是127.0.0.1。这个过程是在设置本机IP的时候，调用fib_inetaddr_event函数完成设置的。

```c
static int fib_inetaddr_event(struct notifier_block \*this, unsigned long event, void \*ptr)   
{ switch (event){ case NETDEV_UP: fib_add_ifaddr(ifa); break; 
```

```c
case NETDEV_DOWN: fib_del_ifaddr(ifa, NULL); //file:ipv4/fib_frontend.c  
void fib_add_ifaddr(struct in_ifaddr *ifa) { fib_magic(RTM NewlyRoute, RTN_LOCAL, addr, 32, prim); } 
```

所以即使本机IP不用127.0.0.1，内核在路由项查找的时候判断类型是RTN_LOCAL，仍然会使用net->loopback_dev，也就是lo虚拟网卡。

为了稳妥起见，再抓包确认一下。开启两个控制台窗口。其中一个对lo设备进行抓包。因为局域网内会有大量的网络请求，为了方便过滤，这里使用一个特殊的端口号8888。如果这个端口号在你的机器上已经占用了，需要再换一个。

```txt
tcpdump -i eth0 port 8888 
```

另外一个窗口使用telnet对本机IP端口发出几条网络请求。

```txt
telnet 10.162.*.* 8888 
```

```txt
Trying 10.162.129.56... 
```

```txt
telnet: connect to address 10.162.129.56: Connection refused 
```

这时候切回第一个控制台，发现什么反应都没有。说明包根本就没有过eth0这个设备。

把设备换成lo再抓。当telnet发出网络请求以后，在tcpdump所在的窗口下看到了抓包结果。

```txt
tcpdump -i lo port 8888 
```

```txt
tcpdump: verbose output suppressed, use -v or -vv for full protocol decode listening on lo, link-type EN10MB (Ethernet), capture size 65535 bytes 08:22:31.956702 IP 10.162.*.*.62705 > 10.162.*.*.ddi-tcp-1: Flags [S], seq 678725385, win 43690, options [mss 65495,nop,wscale 8], length 0 08:22:31.956720 IP 10.162.*.*.ddi-tcp-1 > 10.162.*.*.62705: Flags [R.], seq 0, ack 678725386, win 0, length 0 
```

# 5.3.3 网络设备子系统

网络设备子系统的入口函数是dev_queue_xmit。之前讲述跨机发送过程时介绍过，对于真的有队列的物理设备，该函数进行了一系列复杂的排队等处理后，才调用dev-hard_start_xmit，从这个函数再进入驱动程序来发送。在这个过程中，甚至还有可能触发软中断进行发送，流程如图5.8所示。

![](images/ca4dfc6de3f0c75bf6cfaeb14e2aba8eabf4a194c648909cb973cfbafc9e433f.jpg)  
图5.8 物理网卡设备数据发送

但是对于启动状态的回环设备（q->enqueue 判断为false）来说，就简单多了。没有队列的问题，直接进入dev-hard_start_xmit。接着进入回环设备的“驱动”里发送回调函数loopback xmit，将skb“发送”出去，如图5.9所示。

![](images/d1cf521e7b4d121e655faa78cdc4cca6fb7b68a12bbfba6a9147d7fcb4c4b262.jpg)  
图5.9 回环设备数据发送

下面来看看详细的过程，从网络设备子系统的入口函数dev_queue_xmit看起。

```txt
//file: net/core/dev.c  
int dev_queue_xmit(struct sk BUFF *skb)  
{  
    q = rcu_dereference_bh(txq->qdisc);  
    if (q->enqueue) { //回环设备这里为false  
        rc = __dev_xmit_SKB(skb, q, dev, txq);  
        goto out;  
    } 
```

//开始回环设备处理  
```txt
if (dev->flags & IFF_UP) {
    dev-hard_start_xmit(skb, dev, txq, ...);
    ...
} 
```

在dev-hard_start_xmit函数中还将调用设备驱动的操作函数。

```c
//file: net/core/dev.c  
int dev-hard_start_xmit(struct sk BUFF *skb, struct net_device *dev, struct netdev_queue *txq)  
{ //获取设备驱动的回调函数集合ops const struct net_deviceOps *ops = dev->netdevOps; //调用驱动的ndo_start_xmit进行发送 rc = ops->ndo_start_xmit(skb, dev); } 
```

# 5.3.4 “驱动”程序

回环设备的“驱动”程序的工作流程如图5.10所示。

![](images/e07dfdfe20460233f5544b89bd2945733f4e781f4afd76e4842f731cb6193dac.jpg)  
图5.10 回环设备的“驱动”程序的工作流程

对于真实的igb网卡来说，它的驱动代码都在drivers/net/ethernet/intel/igb/igb_main.c文件里。顺着这个路径，我找到了loopback（回环）设备的“驱动”代码位置，在drivers/net/loopback.c中。

```txt
//file:drivers/net/loopback.c  
static const struct net_deviceOps loopbackOps = {  
    .ndo_init = loopback_dev_init,  
    .ndo_start_xmit = loopback_xmit,  
    .ndo_get/stats64 = loopback_get/stats64,  
}; 
```

所以对dev-hard_start_xmit调用实际上执行的是loopback“驱动”里的loopback_xmit。为什么我把“驱动”加个引号呢，因为loopback是一个纯软件性质的虚拟接口，并没有真正意义上对物理设备的驱动。

```c
//file:drivers/net/loopback.c   
static netdev_tx_t loopback_xmit(struct sk BUFF \*skb, struct net_device \*dev)   
{ //剥离掉和原 socket的联系 skb_orphan(skb); //调用netif_rx if (likely(netif_rx(skb) == NET_RX_SUCCESS)) { 1 
```

在skb_orphan中先把skb上的socket指针去掉了（剥离出来）。

注意，在本机网络IO发送的过程中，传输层下面的skb就不需要释放了，直接给接收方传过去就行，总算是省了一点点开销。不过可惜传输层的skb同样节约不了，还是要频繁地申请和释放。

接着调用 netif_rx，在该方法中最终会执行到 enqueue_to_backward（netif_rx -> netif_rx_internal -> enqueue_to_backward）。

//file: net/core/dev.c   
static int enqueue_to_backlog(struct sk BUFF \*skb, int cpu, unsigned int \*qtail)   
{ sd $=$ &per_cpu(softnet_data, cpu); .skb_queueTAIL(&sd->input_pkt_queue, skb); napi_schedule(sd, &sd->backlog);

在enqueue_to.Backlog函数中，把要发送的skb插入softnet_data->input_pkt_queue队列中并调用napi_schedule来触发软中断。

```c
//file:net/core/dev.c   
static inline void __napi_schedule(struct softnet_data \*sd, struct napi_struct \*napi)   
{ list_addTAIL(&napi->poll_list,&sd->poll_list); __raisesoftirq_irqoff(NET_RX_SOFTIRQ);   
} 
```

只有触发完软中断，发送过程才算完成了。

# 5.4 本机接收过程

发送过程触发软中断后，会进入软中断处理函数net_rx_action，如图5.11所示。

![](images/af9cc954fa2b806b3f2269145cc1229e30c95b9bb3053834ed13cd9085c9e543.jpg)  
图5.11 数据接收

在跨机的网络包的接收过程中，需要经过硬中断，然后才能触发软中断。而在本机的网络IO过程中，由于并不真的过网卡，所以网卡的发送过程、硬中断就都省去了，直接从软中断开始。

在软中断被触发以后，会进入NET_RX_SOFTIRQ对应的处理方法net_rx_action中（至于细节参见第2.2.3节）。

```c
//file: net/core/dev.c  
static void net_rx_action(struct softirq_action *h) {  
    while (!list_empty(&sd->poll_list)) {  
        work = n->poll(n, weight);  
    }  
} 
```

前面介绍过，对于igb网卡来说，poll实际调用的是igb POLL函数。那么loopback网卡的poll函数是哪个呢？由于poll_list里面是struct softnet_data对象，我们在net_dev_init中找到了蛛丝马迹。

//file:net/core/dev.c   
static int __init net_dev_init(void)   
{ for_each POSSIBLE_cpu(i){ sd->backlog poll $=$ process_backlog; 1

原来struct softnet_data 默认的poll 在初始化的时候设置成了 process.Backlog 函数，来看看它都干了什么。

static int process.Backlog(struct napi_struct *napi, int quote)  
{ while(){ while ((skb = __skb_queue(&sd->process_queue))) { __netif_receive_SKb(sk); } //skb_queue_spliceTAIL_init()函数用于将链表a连接到链表b上，//形成一个新的链表b，并将原来a的头变成空链表。 qlen $=$ skb_queue_len(&sd->input_pkt_queue); if (qlen) skb_queue_spliceTAIL_init(&sd->input_pkt_queue, &sd->process_queue); }

这次先看对skb_queue_splice_tail_init的调用。源码就不看了，直接说它的作用，是把sd->input_pkt_queue里的skb链到sd->process_queue链表上去。

然后再看 __skb dequeue, __skbdeque 是从 sd->process_queue 取下来包进行处理。这样和前面发送过程的结尾处就对上了，发送过程是把包放到了 input_pkt_queue 队列里，如图5.12所示。

![](images/a9a4477bd2aa0318f7314fd0954fc596afba21f5329a4aae63d7c3c2c519e387.jpg)  
图5.12 队列的执行

最后调用__netif_receive_skb将数据送往协议栈。在此之后的调用过程就和跨机网络IO又一致了。送往协议栈的调用链是__netif_receive_skb =>__netif_receive_skb_core =>

deliver_SKb，然后将数据包送入ip_rcv中（参见第2章）。网络层再往后是传输层，最后唤醒用户进程。

# 5.5 本章总结

总结一下本机网络IO的内核执行流程，总体流程如图5.13所示。

![](images/20248f4196e0efe3af2d9ef18fd913393e93e475aaba4eae4eebe06e0334ea30.jpg)  
图5.13 本机网络IO过程

回想下跨机网络IO的流程如图5.6所示。

我们现在可以回顾开篇的三个问题啦。

# 1）127.0.0.1 本机网络IO需要经过网卡吗？

通过本章的介绍可以确定地得出结论，不需要经过网卡。即使把网卡拔了，本机网络还是可以正常使用的。

# 2）数据包在内核中是什么走向，和外网发送相比流程上有什么差别？

总的来说，本机网络IO和跨机网络IO比较起来，确实是节约了驱动上的一些开销。发送数据不需要进RingBuffer的驱动队列，直接把skb传给接收协议栈（经过软中断）。但是在内核其他组件上，可是一点儿都没少，系统调用、协议栈（传输层、网络层等）、设备子系统整个走了一遍。连“驱动”程序都走了（虽然对于回环设备来说只是一个纯软件的虚拟出来的东西）。所以即使是本机网络IO，切忌误以为没啥开销就滥用。

如果想在本机网络IO上绕开协议栈的开销，也不是没有办法，但是要动用eBPF。使用eBPF的sockmap和sk redirect 可以达到真正不走协议栈的目的。这个技术不在本书的讨论范围之内，感兴趣的读者可以用这几个关键字上搜索引擎查找相关资料。

3）访问本机服务时，使用127.0.0.1能比使用本机IP（例如192.168.x.x）更快吗？

很多人的直觉是用本机IP会走网卡，但正确结论是和127.0.0.1没有差别，都是走虚拟的环回设备lo。这是因为内核在设置IP的时候，把所有的本机IP都初始化到local路由表里了，类型写死了是RTN_LOCAL。在后面的路由项选择的时候发现类型是RTN_LOCAL就会选择lo设备了。还不信的话你也动手抓包试试！

![](images/5a1c44cfc6d0d3bf99775126a4dac94b752a30737b8ecdfdf76baf78550dc929.jpg)

# 6.1 相关实际问题

目前的互联网应用绝大部分都是运行在TCP之上的，所以说TCP是当今互联网的基石一点儿也不为过。本章就来深度分析TCP连接的建立过程。为了测试你是否需要在这方面进行加强，还是从以下几个问题来引入。

1）为什么服务端程序都需要先listen一下？

int main(int argc, char const \*argv[])   
{ int fd $=$ socket(AF_INET,SOCK_STREAM,0); bind(fd，...); listen(fd，128); accept(fd，...);

上面是一段精简的服务端程序。我想问的是，为什么在服务端非得listen一下，然后才能接收来自客户端们的连接请求呢？listen内部执行的时候到底干了些啥？

2）半连接队列和全连接队列长度如何确定？

TCP服务端在处理三次握手的时候，需要有半连接队列和全连接队列来配合完成。那么这两个数据结构在内核中是什么样子，如果想修改它们的长度，应该如何操作？

3）“Cannot assign requested address”这个报错你知道是怎么回事吗？该如何解决？

你在工作中可能出现过“Cannot assign requested address”这个错误。那么这个错误是如何产生的呢，你是否足够清楚？如果再次遭遇这个问题，又该如何解决它呢？

4）一个客户端端口可以同时用在两条连接上吗？

假设客户端有个端口号，比如10000，已经有了一条和某个服务的ESTABLISH状态的连接了。那么下次再想连接其他的服务端，这个端口还能被使用吗？

5）服务端半/全连接队列满了会怎么样？

如果服务端接收到的连接请求过于频繁，导致半/全连接队列满了会怎么样，会不会导致线上问题？如何确定是否有连接队列溢出发生？如果有，该如何解决？

6）新连接的socket内核对象是什么时候建立的？

服务端在接收客户端的时候需要创建新连接，对应内核就是各种内核对象。那么这些内核对象都是在什么时候建立的呢，是accept函数执行的时候吗？

7）建立一条TCP连接需要消耗多长时间？

接口耗时是衡量服务接口的重要指标之一。接口很多时候都需要和其他的服务器建立连接然后获取一些必要数据。那么，你知道建立一条TCP连接大概需要多长时间吗？

8）把服务器部署在北京，给纽约的用户访问可行吗？

假如中国和美国之间的网络设备非常通畅，我们要建一个网站给美国用户访问。是否可以为了省事直接在北京部署服务器让美国用户来使用？再扩展一点，如果将来人类真的移民火星的时候，我们是否可以在北京部署服务器来让火星用户访问呢？

9）服务器负载很正常，但是CPU被打到底了是怎么回事？

这是一个飞哥在线上真实遭遇的故障。当时是一组服务器上线了一个新功能，然后没多久以后突然就出现了如图6.1所示的奇怪状况。

![](images/8548b30887c6b9a0ca382fc3334e6be5eed465894a3a4bdda0d563ad3acb0e6d.jpg)

![](images/fd8e608b731ec44dbaea35742cf70ea579886b222c41dbd68ea6a8ddc01d56f6.jpg)  
图6.1 CPU消耗异常

这是一台4核的虚拟机。按照对负载的正常理解，4核的服务器负载在4以下都算是正常的。这台机器出故障的时候负载并不高，只有3左右，但是CPU却被打到了 $100\%$ ，也就是说被打到底了。通过本章我们把CPU消耗光的根本原因揪出来。

带着这些问题，让我们开启本章的探秘之旅！

# 6.2 深入理解listen

在服务端程序里，在开始接收请求之前都需要先执行listen系统调用。那么listen到底是干了啥？本节就来深入了解一下。

# 6.2.1 listen系统调用

可以在net/socket.c下找到listen系统调用的源码。

//file: net/socket.c

SYSCALL DEFINE2(listen, int, fd, int, backlog)

{ //根据fd查找socket内核对象 sock $=$ sockfd.lookup_light(fd,&err，&fput_needed); if（sock）{//获取内核参数net.core.somaxconn somaxconn $\equiv$ sock_net(sock->sk)->core.sysctl_somaxconn; if((unsigned int)backlog $\rightharpoondown$ somaxconn) backlog $\equiv$ somaxconn; //调用协议栈注册的listen函数 err $\equiv$ sock->ops->listen(sock,backlog);

用户态的socket文件描述符只是一个整数而已，内核是没有办法直接用的。所以该函数中第一行代码就是根据用户传入的文件描述符来查找对应的socket内核对象。

再接着获取了系统里的net.core.somaxconn内核参数的值，和用户传入的backlog比较后取一个最小值传入下一步。

所以，虽然listen允许我们传入backlog（该值和半连接队列、全连接队列都有关系），但是如果用户传入的值比net.core.somaxconn还大的话是不会起作用的。

接着通过调用sock->ops->listen进入协议栈的listen函数。

# 6.2.2 协议栈listen

关于AF_INET类型的socket内核对象这里不再赘述，可以参考3.2节。这里sock->ops->listen指针指向的是inet听听函数。

```c
//file: net/ipv4/af_inet.c  
int inlet听听(struct socket *sock, int backlog)  
{ //还不是listen 状态（尚未listen过） if(old_state != TCP_LISTEN) { //开始监听 err = inlet_csk听听_start(sk, backlog); } //设置全连接队列长度 sk->sk_max_ACKbackslog = backlog; } 
```

先看一下最底下这行，sk->sk_max_ACKbackslog是全连接队列的最大长度。所以这里我们就知道了一个关键技术点，服务端的全连接队列长度是执行listen函数时传入的backlog和net_core.somaxconn之间较小的那个值。

注意

如果你在线上遇到了全连接队列溢出的问题，想加大该队列长度，那么可能需要同时考虑执行listen函数时传入的backlog和net.core.somaxconn。

再回过头看inet_csk.listen_start函数。

```c
//file: net/ipv4/inet_connection SOCK.c  
int inlet_csk.listen_start(struct sock *sk, const int nr_table_entries)  
{  
    struct inlet_connection_sock *icsk = inlet_csk(sk);  
    //icsk->icsk.accept_queue是接收队列，详情见2.3节  
    //接收队列内核对象的申请和初始化，详情见2.4节  
    int rc = reqsk_queue_alloc(&icsk->icsk.accept_queue, nr_table_entries);  
} 
```

在函数一开始，将struct sock对象强制转换成了inet_connection_sock，名叫icsk。

这里简单讲讲为什么可以这么强制转换，这是因为inet_connection_sock是包含sock的。tcp SOCK、inet_connection_sock、inet SOCK、sock是逐层嵌套的关系，如图6.2所示，类似面向对象里的继承的概念。

![](images/36eae2adc9ff4ec83d935f3f05fb4bbd59fe958aa3802c5af4a5db09a9bc5dab.jpg)  
图6.2 tcp_sock结构

对于TCP的socket来说，sock对象实际上是一个tcp_sock。因此TCP中的socket对象随时可以强制类型转换为tcp_sock、inet_connection_sock、inet_sock来使用。

在接下来的一行reqsk_queue_alloc中实际上包含了两件重要的事情。一是接收队列数据结构的定义。二是接收队列的申请和初始化。这两块都比较重要，我们分别在6.2.3节和6.2.4节介绍。

# 6.2.3 接收队列定义

icsk->icsk_accept_queue定义在inet_connection_sock下，是一个request SOCK_queue类型的对象，是内核用来接收客户端请求的主要数据结构。我们平时说的全连接队列、半连接队列全都是在这个数据结构里实现的，如图6.3所示。

![](images/5f221b113273c909928775f8baeddcf95a10cb32319a4893103b222e6c0c212b.jpg)  
图6.3 接收队列

我们来看具体的代码。

```txt
//file: include/net/inet_connection_sock.h
struct inlet_connection_sock {
    /* inlet_sock has to be the first member! */
    struct inlet_sock icsk_inet;
    struct request_sock_queue icsk_accept_queue;
    ......
} 
```

再来查找request_sock_queue的定义。

```c
//file: include/net/request SOCK.h  
struct request SOCK_queue {  
    //全连接队列  
    struct request SOCK *rskq.accept_head;  
    struct request SOCK *rskq.accept.tail;  
    //半连接队列  
    struct listen SOCK *listen_opt;  
}; 
```

对于全连接队列来说，在它上面不需要进行复杂的查找工作，accept处理的时候只是先进先出地接受就好了。所以全连接队列通过rskq.accept_head和rskq.accept.tail以链表的形式来管理。

和半连接队列相关的数据对象是listen_opt，它是listen_sock类型的。

```txt
//file: include/net/request_sock.h 
```

```c
struct listen_sock {
    u8 max_qlen_log;
    u32 nr_table_entries;
    ...
    struct request_sock *syn_table[0];
}; 
```

因为服务端需要在第三次握手时快速地查找出来第一次握手时留存的request_sock对象，所以其实是用了一个哈希表来管理，就是struct request_sock *syn_table[0]。max_qlen_log和nr_table_entries都和半连接队列的长度有关。

# 6.2.4 接收队列申请和初始化

了解了全/半连接队列数据结构以后，让我们再回到inet_csk.listen_start函数中。它调用了reqsk_queue_alloc来申请和初始化icsk.accept_queue这个重要对象。

```lisp
//file: net/ipv4/inet_connection SOCK.c  
int inet_csk.listen_start(struct sock *sk, const int nr_table_entries)  
{  
    int rc = reqsk_queue_alloc(&icsk->icsk.accept_queue, nr_table_entries);  
} 
```

在reqsk_queue_alloc这个函数中完成了接收队列request_sock_queue内核对象的创建和初始化。其中包括内存申请、半连接队列长度的计算、全连接队列头的初始化，等等。让我们进入它的源码：

```c
//file: net/core/request_sock.c  
int reqsk_queue_alloc(struct request_sock_queue *queue, unsigned int nr_table_entries)  
{ size_t lopt_size = sizeof(struct listen SOCK); struct listen SOCK *lopt; //计算半连接队列的长度 nr_table_entries = min_t(u32, nr_table_entries, sysctl_max_syn_backlog); nr_table_entries = ....... //为listen_sock对象申请内存，这里包含了半连接队列 lopt_size += nr_table_entries * sizeof(struct request_sock *); if (lopt_size > PAGE_SIZE) lopt = vzalloc(lopt_size); else lopt = kzalloc(lopt_size, GFP_KERNEL); 
```

```c
//全连接队列头初始化  
queue->rskq.accept_head = NULL;  
//半连接队列设置  
lopt->nr_table_entries = nr_table_entries;  
queue->listen_opt = lopt;  
} 
```

开头定义了一个struct listen_sock指针。这个listen_sock就是我们平时经常说的半连接队列。

接下来计算半连接队列的长度。计算出来实际大小以后，开始申请内存。最后将全连接队列头queue->rskq.accept_head设置成了NULL，将半连接队列挂到了接收队列queue上。

# 注意

这里要注意一个细节，半连接队列上每个元素分配的是一个指针大小（sizeof(struct request_sock *））。这其实是一个哈希表。真正的半连接用的request_sock对象是在握手过程中分配的，计算完哈希值后挂到这个哈希表上。

# 6.2.5 半连接队列长度计算

在6.2.4节曾提到reqsk_queue_alloc函数中计算了半连接队列的长度，由于这个略有点复杂，所以单独用一小节讨论它。

```txt
//file: net/core/request_sock.c  
int reqsk_queue_alloc(struct request_sock_queue *queue, unsigned int nr_table_entries)  
{ //计算半连接队列的长度  
nr_table_entries = min_t(u32, nr_table_entries, sysctl_max_syn_backlog);  
nr_table_entries = max_t(u32, nr_table_entries, 8);  
nr_table_entries = roundup_pow_of_two(nr_table_entries + 1);  
//为了效率，不记录 nr_table_entries  
//而是记录2的N次幂等于 nr_table_entries  
for (lopt->max_qlen_log = 3;  
(1 << lopt->max_qlen_log) < nr_table_entries;  
lopt->max_qlen_log++)  
} 
```

传进来的nr_table_entries在最初调用reqsk_queue_alloc的地方可以看到，它是内核参数net.core.somaxconn和用户调用listen时传入的backlog二者之间的较小值。

在这个reqsk_queue_alloc函数里，又将会完成三次的对比和计算。

- min_t(u32, nr_table_entries, sysctl_max_syn_backlog)这句是再次和sysctl_max_synbacklog内核对象取了一次最小值。  
- max_t(u32, nr_table_entries, 8)这句保证nr_table_entries不能比8小，这是用来避免新手用户传入一个太小的值导致无法建立连接的。  
- roundup_pow_of_two(nr_table_entries + 1)是用来上对齐到2的整数次幂的。

说到这里，你可能已经开始头疼了。确实这样的描述是有点抽象。咱们换个方法，通过两个实际的案例来计算一下。

假设：某服务器上内核参数net.core.somaxconn为128，net.ipv4.tcp_max_syn_backlog为8192。那么当用户backlog传入5时，半连接队列到底是多长呢？

和代码一样，我们还是把计算分为四步，最终结果为16。

1. min (backlog, somaxconn) = min $(5, 128) = 5$   
2. min (5, tcp_max_syn_backlog) = min (5, 8192) = 5   
3. max (5, 8) = 8   
4. roundup.pow_of_two $(8 + 1) = 16$

somaxconn和tcp_max_syn_backlog保持不变，listen时的backlog加大到512。再算一遍，结果为256。

1. min (backlog, somaxconn) = min (512, 128) = 128   
2. min (128, tcp_max_syn_backlog) = min (128, 8192) = 128   
3. max (128, 8) = 128   
4. roundup.pow_of_two $(128 + 1) = 256$

算到这里，我把半连接队列长度的计算归纳成了一句话，半连接队列的长度是min(backlog, somaxconn, tcp_max_syn_backlog) + 1再上取整到2的N次幂，但最小不能小于16。我用的内核源码是3.10，你手头的内核版本可能和这个稍微有些出入。

★注意

如果你在线上遇到了半连接队列溢出的问题，想加大该队列长度，那么就需要同时考虑somaxconn、backlog和tcp_max_syn_backlog三个内核参数。

最后再说一点，为了提升比较性能，内核并没有直接记录半连接队列的长度。而是采用了一种晦涩的方法，只记录其N次幂。假设队列长度为16，则记录max_qlen_log为4（2的4次方等于16），假设队列长度为256，则记录max_qlen_log为8（2的8次方等于

256）。大家只要知道这个就是为了提升性能就行了。

# 6.2.6 listen过程小结

计算机系的学生就像背八股文一样记着服务端socket程序流程：先bind，再listen，然后才能accept。至于为什么需要先listen一下才可以accpet，大家平时关注得太少了。

通过本节对listen源码的简单浏览，我们发现listen最主要的工作就是申请和初始化接收队列，包括全连接队列和半连接队列。其中全连接队列是一个链表，而半连接队列由于需要快速地查找，所以使用的是一个哈希表（其实半连接队列更准确的叫法应该叫半连接哈希表）。详细的接收队列结构参见图6.3。全/半两个队列是三次握手中很重要的两个数据结构，有了它们服务端才能正常响应来自客户端的三次握手。所以服务端都需要调用listen才行。

除此之外我们还有额外收获，我们还知道了内核是如何确定全/半连接队列的长度的。

# 1. 全连接队列的长度

对于全连接队列来说，其最大长度是listen时传入的backlog和net.core.somaxconn之间较小的那个值。如果需要加大全连接队列长度，那么就要调整backlog和somaxconn。

# 2. 半连接队列的长度

在listen的过程中，我们也看到了对于半连接队列来说，其最大长度是min(backlog, somaxconn, tcp_max_syn_backlog) + 1再上取整到2的N次幂，但最小不能小于16。如果需要加大半连接队列长度，那么需要一并考虑backlog、somaxconn和tcp_max_syn_backlog这三个参数。网上任何告诉你修改某一个参数就能提高半连接队列长度的文章都是错的。

所以，不放过一个细节，你可能会有意想不到的收获！

# 6.3 深入理解connect

客户端在发起连接的时候，创建一个socket，然后瞄准服务端调用connect就可以了，代码可以简单到只有两句。

int main(){ fd $=$ socket(AF_INET,SOCK_STREAM,0); connect(fd，...）; 1

但是区区两行代码，背后隐藏的技术细节却很多。

3.2节简单介绍过socket函数是如何在内核中创建相关内核对象的。socket函数执行完毕后，从用户层视角我们看到返回了一个文件描述符fd。但在内核中其实是一套内核

对象组合，包含file、socket、sock等多个相关内核对象构成，每个内核对象还定义了ops操作函数集合。由于本节我们还会用到这个数据结构图，所以这里再画一次。

![](images/85a6f0b5cc5b6446c8fe82227673e4d5e2ac57d8ce71ca513a33a9499007ca1e.jpg)  
图6.4 socket数据结构

接下来就进入connect函数的执行过程。

# 6.3.1 connect调用链展开

当在客户端机上调用connect函数的时候，事实上会进入内核的系统调用源码中执行。

//file: net/socket.c   
SYSCALL DEFINE3.connect, int, fd, struct sockaddr __user *, uservaddr, int, addrlen)   
{ struct socket \*sock; //根据用户fd查找内核中的socket对象 sock $=$ sockfd.lookup_light(fd, &err, &fput_needed); //进行connect err $=$ sock->ops->connect(sock,(struct sockaddr \*)&address, addrlen, sock->file->f_flags);

```txt
… 
```

这段代码首先根据用户传入的fd（文件描述符）来查询对应的socket内核对象。对于AF_INET类型的socket内核对象来说，sock->ops->connect指针指向的是inet_STREAM_connect函数。

```c
//file: ipv4/af_inet.c  
int inletStream_connect(struct socket *sock, ...)  
{  
    ____inetStream_connect(soap, uaddr, addr_len, flags);  
}  
int ____inetStream_connect(struct socket *sock, ...)  
{  
    struct sock *sk = sock->sk;  
    switch (soap->state) {  
        case SS_UNCONNECTED:  
            err = sk->sk_prot->connect(sk, uaddr, addr_len);  
            sock->state = SS_CONNECTED;  
            break;  
        }  
    ...  
} 
```

刚创建完毕的socket的状态就是SS_UNCONNECTED，所以在__inet_STREAM_connect中的switch判断会进入case SS_UNCONNECTED的处理逻辑中。

上述代码中sk取的是socket对象。对于AF_INET类型的TCP socket来说，sk->sk_prot->connect指针指向的是tcp_v4_connect方法。

我们来看tcp_v4_connect的代码，它位于net/ipv4/tcp_ $\mathrm{pv4.c}$

```c
//file: net/ipv4/tcp_ipv4.c  
int tcp_v4_connect(struct sock *sk, struct sockaddr *uaddr, int addr_len)  
{ //设置 socket 状态为TCP_SYN_SENT  
tcp_set_state(sk, TCP_SYNSENT);  
//动态选择一个端口  
err = inet_hash_connect(&tcp_death_row, sk);  
//函数用来根据sk中的信息，构建一个syn报文，并将它发送出去。  
err = tcp_connect(sk);
```

在这里将把socket状态设置为TCP_SYNSENT。再通过inet_hash_connect来动态地选择一个可用的端口。

# 6.3.2 选择可用端口

找到inet_hash_connect的源码，我们来看看到底端口是如何选择出来的。

```c
//file:net/ipv4/inet_hashtables.c  
int inset_hash_connect(struct inset_timestamp_death_row *death_row, struct sock *sk)  
{ return __inet_hash_connect(death_row, sk, inset_sk_port_offset(sk), __inet_check_established, __inet_hash_nolisten); } 
```

这里需要提一下在调用__inet_hash_connect时传入的两个重要参数：

- inlet_sk_port_offset(sk): 这个函数根据要连接的目的IP和端口等信息生成一个随机数。  
- __inet_check Established：检查是否和现有ESTABLISH状态的连接冲突的时候用的函数。

了解了这两个参数后，进入__inet_hash_connect函数。这个函数比较长，为了方便理解，先看前面这一段。

```txt
//file:net/ipv4/inet_hashtables.c  
int __inet_hash_connect(...)  
{  
//是否绑定过端口  
const unsigned short snum = inet_sk(sk) ->inet_num;  
//获取本地端口配置  
inet_get_local_port_range(&low, &high);  
remaining = (high - low) + 1;  
if (!snum) {  
//遍历查找  
for (i = 1; i <= remaining; i++) {  
    port = low + (i + offset) % remaining;  
    }  
} 
```

在这个函数中首先判断了inet_sk(sk) -> inlet_num，如果调用过bind，那么这个函数会选择好端口并设置在inet_num上。假设没有调用过bind，所以snum为0。

接着调用inet_get_local_port_range，这个函数读取的是net ipv4.ip_local_port_range这

个内核参数，来读取管理员配置的可用的端口范围。

# 注意

该参数的默认值是3276861000，意味着端口总可用的数量是61000-32768=28232个。如果觉得这个数字不够用，那就修改net.ip4.ip_local_port_range内核参数。

接下来进入了for循环。其中offset是通过inet_sk_port_offset(sk)计算出的随机数。那这段循环的作用就是从某个随机数开始，把整个可用端口范围遍历一遍。直到找到可用的端口后停止。

接下来看看如何确定一个端口是否可用。

```c
//file:net/ipv4/inet_hashtables.c  
int __inetr_hash_connect(..)  
{  
    for (i = 1; i <= remaining; i++) {  
        port = low + (i + offset) % remaining;  
    }  
    //查看是否是保留端口，是则跳过  
    if (inet_is_reserved_local_port.port) continue;  
    //查找和遍历已经使用的端口的哈希链表  
    head = &hinfo->bhash[inet_bhashfn(net, port, hinfo->bhash_size)];  
    inset_bind_BUCKET_for_each(tb, &head->chain) {  
        //如果端口已经被使用  
        if (net_eq(ab_net(tb), net) && tb->port == port) {  
            //通过 check_established 继续检查是否可用  
            if (!check_established(death_row, sk, port, &tw)) goto ok;  
        }  
    }  
    //未使用的话  
    tb = inet_bind_buckets_create(hinfo->bind_buckets_cachep, ...);  
    ....... goto ok;  
}  
return -EADDRNOTAVAIL;  
ok: 
```

首先判断的是inet_is_reserved_local_port，这个很简单，就是判断要选择的端口是否在net.ipv4.ip_local_reserved_ports中，在的话就不能用。

如果你因为某种原因不希望某些端口被内核使用，那么把它们写到ip_local_reserved_ports这个内核参数中就行了。

整个系统中会维护一个所有使用过的端口的哈希表，它就是hinfo->bhash。接下来的代码就会在这里查找端口。如果在哈希表中没有找到，那么说明这个端口是可用的。至此端口就算是找到了。这个时候通过net_bind_bucket_create申请一个inet_bind_bucket来记录端口已经使用了，并用哈希表的形式都管理了起来。后面在7.4节的实验环节能看到这个inet_bind_bucket内核对象。

遍历完所有端口都没找到合适的，就返回-EADDRNOTAVAIL，你在用户程序上看到的就是Cannot assign requested address这个错误。怎么样，是不是很眼熟，你见过它的对吧，哈哈！

```c
/* Cannot assign requested address */ #define EADDRNOTAVAIL 99 
```

以后当你再遇到Cannot assign requested address 错误，应该想到去查一下 net.ips4.ip_local_port_range 中设置的可用端口的范围是不是太小了。

# 6.3.3 端口被使用过怎么办

回顾刚才的__inet_hash_connect，为了描述简单，之前跳过了端口号已经在bhash中存在时候的判断。这是由于，其一这个过程比较长，其二这段逻辑很有价值，所以单独拿出来讲。

```txt
//file:net/ipv4/inet_hashtables.c  
int __inet_hash_connect(...){  
    for (i = 1; i <= remaining; i++) {  
        port = low + (i + offset) % remaining;  
    }  
    //如果端口已经被使用  
    if (net_eq(ib_net(tb), net) && tb->port == port) {  
        //通过 check Established 继续检查是否可用  
        if (!check Established人死亡_row, sk, port, &tw)) goto ok;  
    }  
} 
```

port在bhash中如果已经存在，就表示有其他的连接使用过该端口了。请注意，如果check_established返回0，该端口仍然可以接着使用！

这里可能会让很多读者困惑了，一个端口怎么可以被用多次呢？

回忆一下四元组的概念，两对四元组中只要任意一个元素不同，都算是两条不同的连接。以下的两条TCP连接完全可以同时存在（假设192.168.1.101是客户端，192.168.1.100是服务端）：

连接1：192.168.1.1015000192.168.1.1008090  
连接2：192.168.1.1015000192.168.1.1008091

check_established作用就是检测现有的TCP连接中是否四元组和要建立的连接四元素完全一致。如果不完全一致，那么该端口仍然可用！！！

这个check_established是由调用方传入的，实际上使用的是__inet_check_established。我们来看它的源码。

```c
//file: net/ipv4/inet_hashtable.c  
static int __inet_check_established(struct inet_timeout_death_row *death_row, struct sock *sk, __u16 lport, structINET_timeout_sock **twp) { 
```

//找到哈希桶

```c
struct inset_ehash_BUCKET *head = inset_ehash_buckets(hinfo, hash); 
```

//遍历看看有没有四元组一样的，一样的话就报错

```c
sk_nulls_for_each(sk2, node, &head->chain) {
    if (sk2->sk_hash != hash)
        continue;
    if (likely(INET_MATCH(sk2, net, acookie,晷d,晷r, ports, dif)))
        goto not_unique;
}  
unique:
    //要用了，记录，返回0（成功）
    return 0;
not_unique:
    return -EADDRNOTAVAIL;
} 
```

该函数首先找到inet一笑 hash bucket，这个和bhash类似，只不过这是所有ESTABLISH状态的socket组成的哈希表。然后遍历这个哈希表，使用INET_MATCH来判断是否可用。

INET_MATCH源码如下。

```c
// include/net/inet_hashtables.h
#define INET_MATCH(_sk, _net, _cookie, _saddr, _daddr, _ports, _dif) \
((inet_sk(_sk) ->inet_portpair == (_ports)) && \
(inet_sk(_sk) ->inet_daddr == (_saddr)) && \
(inet_sk(_sk) ->inet_rcv_saddr == (_daddr)) && 
```

(!(sk) -> sk_bound_dev_if || $(\text{sk}) -> \text{sk_bound_dev_if} == (\text{dif}))$ && net_eq(sock_net(sk), (_net)))

在INET_MATCH中将_saddr、_daddr、_ports都进行了比较。当然除了IP和端口，INET_MATCH还比较了其他一些项目，所以TCP连接还有五元组、七元组之类的说法。为了统一，这里还沿用四元组的说法。

如果匹配，就是四元组完全一致的连接，所以这个端口不可用。也返回-EADDRNOTAVAIL。

如果不匹配，哪怕四元组中有一个元素不一样，例如服务端的端口号不一样，那么就返回0，表示该端口仍然可用于建立新连接。

注意

所以一台客户端机最大能建立的连接数并不是65535。只要服务端足够多，单机发出百万条连接没有任何问题。

# 6.3.4 发起syn请求

再回到tcp_v4_connect，这时我们的inet_hash_connect已经返回了一个可用端口。接下来就进入tcp_connect，源码如下所示。

```c
//file: net/ipv4/tcp_ipv4.c  
int tcp_v4_connect(struct sock *sk, struct sockaddr *uaddr, int addr_len)  
{  
    //动态选择一个端口  
    err = inet_hash_connect(&tcp_death_row, sk);  
    //函数用来根据sk中的信息，构建一个完成的syn报文，并将它发送出去。  
    err = tcp_connect(sk);  
}
```

到这里其实就和本章要讨论的主题没有关系了，所以只简单看一下。

```c
//file:net/ipv4/tcp_output.c  
int tcp_connect(struct sock *sk)  
{ //申请并设置skb buff = alloc_skb_fclone(MAX.tcpHEADER + 15, sk->sk_allocation); tcp_init_nodata_skb(buffer, tp->write_seq++, TCPHDR_SYN); //添加到发送队列sk_write_queue tcp_connect_queue_skb(sk, buff); //实际发出syn 
```

```c
err = tp->fastopen_req ? tcp_send_syn_data(sk, buff) : tcp_transmit_skb(sk, buff, 1, sk->sk_allocation); //启动重传定时器 inlet_csk_reset_xmit_timer(sk, ICSK_TIME_RETRANS, inlet_csk(sk) ->icsk_rto, TCP_RTO_MAX); } 
```

tcp_connect一口气做了这么几件事：

- 申请一个skb，并将其设置为SYN包。  
- 添加到发送队列上。  
- 调用tcp_transmit_SKb将该包发出。  
启动一个重传定时器，超时会重发。

该定时器的作用是等到一定时间后收不到服务端的反馈的时候来开启重传。首次超时时间是在TCP_TIMEOUT_INIT宏中定义的，该值在Linux 3.10版本中是1秒，在一些老版本中是3秒。

```c
//file:ipv4/tcp_output.c  
void tcp_connect_init(struct sock *sk)  
{ //初始化为TCP_TIMEOUT_INIT  
inet_csk(sk) ->icsk_rto = TCP_TIMEOUT_INIT; 
```

TCP_TIMEOUT_INIT 在include/net/tcp.h中被定义成了1秒。

```cpp
//file: include/net/tcp.h  
#define TCP_TIMEOUT_init((unsigned)(1*HZ)) 
```

在一些老版本，比如v2.6.30版本下，这个初始值是3秒。

```txt
//file: include/net/tcp.h  
#define TCP_TIMEOUT_INIT ((unsigned)(3*HZ)) 
```

# 6.3.5 connect小结

小结一下，客户端在执行connect函数的时候，把本地socket状态设置成了TCP_SYN_SENT，选了一个可用的端口，接着发出SYN握手请求并启动重传定时器。

现在我们搞清楚了TCP连接中客户端的端口会在两个位置确定。

第一个位置，是本节重点介绍的connect系统调用执行过程。在connect的时候，会随机地从ip_local_port_range选择一个位置开始循环判断。找到可用端口后，发出syn握手包。如果端口查找失败，会报错“Cannot assign requested address”。这个时候你应该

首先想到去检查一下服务器上的net ipv4.ip_local_port_range参数，是不是可以再放得多一些。

如果你因为某种原因不希望某些端口被用到，那么把它们写到ip_local_reserved_ports这个内核参数中就行了，内核在选择的时候会跳过这些端口。

另外还要注意一个端口是可以被用于多条TCP连接的。所以一台客户端机最大能建立的连接数并不是65535。只要连接的服务端足够多，单机发出百万条连接没有任何问题。我给大家展示一下实验时的实际截图（见图6.5），来实际看一下一个端口号确实是被用在了多条连接上。

![](images/35252f69657c51bccf606527a54499babe7010268119e2680968cac0a101d094.jpg)  
图6.5 一个端口号可以用于多条连接

图6.5中左边的192是客户端，右边的119是服务端的IP。可以看到客户端的10000这个端口号是用在了多条连接上的。

多说一句，上面的选择端口都是从ip_local_port_range范围中的某一个随机位置开始循环的。如果可用端口很充足，则能快一些找到可用端口，那循环很快就能退出。假设实际中ip_local_port_range中的端口快被用光了，这时候内核就大概率要把循环多执行很多轮才能找到可用端口，这会导致connect系统调用的CPU开销上涨。后面将在6.5.1节中详细介绍这种情况。

如果在connect之前使用了bind，将会使得connect系统调用时的端口选择方式无效。转而使用bind时确定的端口。调用bind时如果传入了端口号，会尝试首先使用该端口号，如果传入了0，也会自动选择一个。但默认情况下一个端口只会被使用一次。所以对于客户端角色的socket，不建议使用bind！

# 6.4 完整TCP连接建立过程

在后端相关岗位的入职面试中，三次握手的出场频率非常高，甚至说它是必考题也不为过。一般的答案都是说客户端如何发起SYN握手进入SYNSENT状态，服务端响应SYN并回复SYNACK，然后进入SYN_RECV……

飞哥想给出一份不一样的答案。其实三次握手在内核的实现中，并不只是简单的状态的流转，还包括端口选择、半连接队列、syncookie、全连接队列、重传计时器等关键操作。如果能深刻理解这些，你对线上的把握和理解将更进一步。如果有面试官问起你三次握手，相信这份答案一定能帮你在面试官面前赢得加分。

在基于TCP的服务开发中，三次握手的主要流程如图6.6所示。

![](images/0a36c93c6263ed0b999bdea0bae5e6605b4a2bc1fa687c1d3af8ce45a5451d7c.jpg)  
图6.6 三次握手

服务端核心逻辑是创建socket绑定端口，listen监听，最后accept接收客户端的请求。

# //服务端核心代码

int main(int argc, char const \*argv[])   
{ int fd $=$ socket(AF_INET,SOCK_STREAM,0); bind(fd,...); listen(fd,128); accept(fd,...);

客户端的核心逻辑是创建socket，然后调用connect连接服务端。

# //客户端核心代码

int main(){ fd $=$ socket(AF_INET,SOCK_STREAM, $\text{念}$ ); connect(fd，...）; }

# 6.4.1 客户端connect

这个已经在上一节重点讲过了，这里只简单回顾一下。客户端通过调用connect来发起连接。在connect系统调用中会进入内核源码的tcp_v4_connect。

//file: net/ipv4/tcp_ipv4.c  
int tcp_v4_connect(struct sock \*sk, struct sockaddr \*uaddr, int addr_len)  
{ //设置 socket 状态为TCP_SYN_SENT tcp_set_state(sk，TCP_SYN_SENT); //动态选择一个端口 err $=$ inet_hash_connect(&tcp_death_row, sk); //函数用来根据sk中的信息，构建一个完成的syn报文，并将它发送出去 err $=$ tcp_connect(sk);   
}

在这里将完成把socket状态设置为TCP_SYN_SENT。再通过inet_hash_connect来动态地选择一个可用的端口后，进入tcp_connect。

//file:net/ipv4/tcp_output.c  
int tcp_connect(struct sock *sk)  
{tcp_connect_init(sk); //申请skb并构造为一个SYN包  
......  
//添加到发送队列sk_write_queuetcp_connect_queue_skb(sk，buff);  
//实际发出synerr $=$ tp->fastopen_req?tcp_send_syn_data(sk，buff）：tcp_transmit_skb(sk，buff,1，sk->sk_allocation);  
//启动重传定时器inet_csk_reset_xmit_timer(sk，ICSK_TIME_RETRANS,inet_csk(sk)->icsk_rto，TCP_RTO_MAX);

在tcp_connect申请和构造SYN包，然后将其发出。同时还启动了一个重传定时器，该定时器的作用是等到一定时间后收不到服务器的反馈的时候来开启重传。在Linux 3.10版本中首次超时时间是1秒，在一些老版本中是3秒。

总结一下，客户端在调用connect的时候，把本地socket状态设置成了TCP_SYN_sent，选了一个可用的端口，接着发出SYN握手请求并启动重传定时器。

# 6.4.2 服务端响应SYN

在服务端，所有的TCP包（包括客户端发来的SYN握手请求）都经过网卡、软中断，进入tcp_v4_rcv。在该函数中根据网络包（skb）TCP头信息中的目的IP信息查到当前处于listen状态的socket，然后继续进入tcp_v4_do_rcv处理握手过程。

```c
//file: net/ipv4/tcp_ipv4.c  
int tcp_v4_do_rcv(struct sock *sk, struct sk BUFF *skb)  
{  
    .......  
    //服务端收到第一步握手SYN或者第三步ACK都会走到这里  
if (sk->sk_state == TCP_LISTEN) {  
    struct sock *nsk = tcp_v4_hnd_req(sk, skb);  
}  
if (tcp_rcv_state_process(sk, skb, tcp hdr(skb), skb->len)) {  
    rsk = sk;  
    goto reset;  
} 
```

在tcp_v4_do_rcv中判断当前socket是listen状态后，首先会到tcp_v4_hnd_req查看半连接队列。服务端第一次响应SYN的时候，半连接队列里必然空空如也，所以相当于什么也没干就返回了。

//file:net/ipv4/tcp_ipv4.c   
static struct sock \*tcp_v4_hnd_req(struct sock \*sk,struct sk BUFF \*skb)   
{ //查找listen socket的半连接队列 struct request_sock \*req $=$ inet_csk_search_req(sk,&prev，th->source, iph->saddr，iph->daddr); return sk;

在tcp_rcv_state_process里根据不同的socket状态进行不同的处理。

```c
//file:net/ipv4/tcp_input.c  
int tcp_rcv_state_process(struct sock *sk, struct sk BUFF *skb, const struct tcphdr *th, unsigned int len)  
{  
    switch (sk->sk_state) {  
        //第一次握手  
        case TCP_LISTEN:  
            if (th->syn) { //判断是否为SYN握手包  
                if (icsk->icsk.af ops->conn_request(sk, skb) < 0)  
                    switch (sk->sk_state) {  
                        switch (sk->sk_state) {  
                            switch (sk->sk_state) {  
                                switch (sk->sk_state) {  
                                    switch (sk->sk_state) {  
                                          switch (sk->sk_state) {  
                                            switch (sk->sk_state) {  
                                                  switch (sk->sk_state) {  
                                    switch (sk->sk_state) {  
                                          switch (sk->sk_state) {  
                                            switch (sk->sk_state) {  
                                                  switch (sk->sk_state) {  
                                    switch (sk->sk_state) {  
                                          switch (sk->sk_state) {  
                                            switch (sk->sk_state) {  
                                                  switch (sk->sk_state) {  
                                    switch (sk->sk_state) {  
                                        switch (sk->sk_state) {  
                                          switch (sk->sk_state) {  
                                            switch (sk->sk_state) {  
                                                switch (sk->sk_state) {  
                                            switch (sk->sk_state) {  
                                                switch (sk->sk_state) {  
                                            switch (sk->sk_state) {  
                                                switch (sk->sk_state) {  
                                            switch (sk->sk_state) {  
                                                switch (sk->sk_state) {  
                                            switch (sk->sk_state) {  
                                                switch (sk->sk_state) {  
                                             switch (sk->sk_state) {  
                                               switch (sk->sk_state) {  
                                         switch (sk->sk_state) {  
                                             switch (sk->sk_state) {  
                                             switch (sk->sk_state) {  
                                             switch (sk->sk_state) {  
                                             switch (sk->sk_state) {  
                                             switch (sk->sk_state) {  
                                             switch (sk->sk_state) {  
                                             switch (sk->sk��态）{  
                                             switch (sk->sk��态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk��态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk��态）{  
                                             switch (sk->sk��态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态） {  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态） {  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk��态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->sk状态）{  
                                             switch (sk->skip) { 
```

return 1;

}

其中conn_request是一个函数指针，指向tcp_v4 conn_request。服务端响应SYN的主要处理逻辑都在这个tcp_v4 conn_request里。

//file: net/ipv4/tcp_ipv4.c

```c
int tcp_v4conn_request(struct sock \*sk,struct sk BUFF\*skb) { 
```

//看看半连接队列是否满了

```c
if (inet_csk_reqsk_queue_is_full(sk) && !isn) { want_cookie = tcp_syn_flood_action(sk, skb, "TCP"); if (!want_cookie) goto drop; } 
```

//在全连接队列满的情况下，如果有young_ACK，那么直接丢弃

```c
if (sk_acceptq_is_full(sk) && inet_csk_reqsk_queue_young(sk) > 1) { NET_INC_STATSHBH(sock_net(sk), LINUX_MIB_LISTENOVERFLows); goto drop; } 
```

··

//分配request_sock内核对象

```txt
req = inetrequires_alloc(&tcp_request_sockOps); 
```

//构造 syn+ack包

```txt
skb_synack = tcp.make_synack(sk, dst, req, fastopen_cookie_present(&valid_foc) ? &valid_foc : NULL); 
```

if (likely(!do_fastopen)) {

//发送 syn + ack响应

```txt
err = ip_build_and_send_pkt(skb_synack, sk, ireq->loc_addr, ireq->rmt_addr, ireq->opt); 
```

//添加到半连接队列，并开启计时器

```c
inet_csk_reqsk_queue_hash_add(sk, req, TCP_TIMEOUT_INIT);
```

}

在这里首先判断半连接队列是否满了，如果满了进入tcp_syn_flood_action去判断是否开启了tcp_syncookies内核参数。如果队列满，且未开启tcp_syncookies，那么该握手包将被直接丢弃！

接着还要判断全连接队列是否满。因为全连接队列满也会导致握手异常，那干脆就在第一次握手的时候也判断了。如果全连接队列满了，且young_ACK数量大于1的话，那么同样也是直接丢弃。

![](images/08c1b619da9ade79cace946e09e9264b50600dea9bfab4a5be19a71f86c10d74.jpg)

young_ACK是半连接队列里保持着的一个计数器。记录的是刚有SYN到达，没有被SYN_ACK重传定时器重传过SYN_ACK，同时也没有完成过三次握手的sock数量。

接下来是构造synack包，然后通过ip_build_and_send pkt把它发送出去。

最后把当前握手信息添加到半连接队列，并开启计时器。计时器的作用是，如果某个时间内还收不到客户端的第三次握手，服务端会重传synack包。

总结一下，服务端响应ack的主要工作是判断接收队列是否满了，满的话可能会丢弃该请求，否则发出synack。申请request_sock添加到半连接队列中，同时启动定时器。

# 6.4.3 客户端响应SYNACK

客户端收到服务端发来的synack包的时候，也会进入tcp_rcv_state_process函数。不过由于自身socket的状态是TCP_SYNSENT，所以会进入另一个不同的分支。

```c
//file:net/ipv4/tcp_input.c  
//除了ESTABLISHED和TIME_WAIT，其他状态下的TCP处理都走这里  
int tcp_rcv_state_process(struct sock *sk, struct sk BUFF *skb, const struct tcphdr *th, unsigned int len)  
{  
    switch (sk->sk_state) {  
        //服务器收到第一个ACK包  
        case TCP_LISTEN:  
            .......  
        //客户端第二次握手处理  
        case TCP_SYNSENT:  
            //处理synack包  
            queued = tcp_rcv_synsent_state_process(sk, skb, th, len);  
            return 0;  
}
```

tcp_rcv_synsent_state_process是客户端响应synack的主要逻辑。

```c
//file:net/ipv4/tcp_input.c   
static int tcp_rcv_synsent_state_process(struct sock \*sk, struct sk BUFF \*skb, const struct tcphdr \*th, unsigned int len)   
{ tcp ACK(sk, skb, FLAG_SLOWPATH); 
```

//连接建立完成

```c
tcp_finish_connect(sk, skb); 
```

```c
if (sk->sk_write_pending || icsk->icsk_accept_queue.rskq_defer.accept || icsk->icsk ACK.pingpong) //延迟确认.... else { tcp_send_ACK(sk); }   
}   
tcp ACK()->tcp Clean_rtx_queue() //file: net/ipv4/tcp_input.c   
static int tcpClean_rtx_queue(struct sock \*sk, int prior_fackets, u32 prior_snd_una)   
{ //删除发送队列 . //删除定时器 tcp_rearm_rto(sk);   
} //file: net/ipv4/tcp_input.c   
void tcp_finish_connect(struct sock \*sk, struct sk BUFF \*skb)   
{ //修改 socket 状态 tcp_set_state(sk, TCPEstablishED); //初始化拥塞控制 tcp_init_congestion_control(sk); .... .. //保活计时器打开 if (sock_flag(sk, SOCK_KEEPOPEN)) inlet_csk_reset_keepalive_timer(sk, keepalive_time_when(tp));   
} 
```

客户端将自己的socket状态修改为ESTABLISHED，接着打开TCP的保活计时器。

```c
//file:net/ipv4/tcp_output.c  
void tcp_send_ACK(struct sock *sk)  
{ //申请和构造ack包 buff = alloc_skb(MAX_TACHEADER, sk_gfp_atomic(sk, GFPAtomic)); //发送出去 tcp_transmit_skb(sk, buff, 0, sk_gfp_atomic(sk, GFPAtomic)); } 
```

在tcp_send_ACK中构造ack包，并把它发送出去。

客户端响应来自服务端的synack时清除了connect时设置的重传定时器，把当前socket状态设置为ESTABLISHED，开启保活计时器后发出第三次握手的ack确认。

# 6.4.4 服务端响应ACK

服务端响应第三次握手的ack时同样会进入tcp_v4_do_rcv。

//file: net/ipv4/tcp_ipv4.c   
int tcp_v4_do_rcv(struct sock \*sk, struct sk BUFF \*skb)   
{ if (sk->sk_state == TCP_LIST