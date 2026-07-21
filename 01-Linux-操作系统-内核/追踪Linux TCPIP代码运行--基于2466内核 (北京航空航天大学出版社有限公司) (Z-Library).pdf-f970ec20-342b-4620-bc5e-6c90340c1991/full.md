# 追踪 Linux TCP/IP 代码运行

——基于2.6内核

秦健编著

北京航空航天大学出版社

# 内容简介

本书以应用程序为线索，详细描述了数据包在协议栈的分段、重组、发送、接收过程，同时分析了路由的初始化和设置过程，主要包括socket应用程序、TCP/IP协议、路由、通知链、邻居子系统等内容。全书涵盖了协议栈的全部知识点，对于广大的读者来说这是一本极其难得的技术资料。同时，书中论述了网络设备的工作原理，解释了RTL8169和嵌入式CS8900、DM9000网卡设备的核心过程。

本书可作为3G网络开发人员、嵌入式通信产品开发人员、网络应用开发人员、网络管理人员及网络爱好者、从事网络方向的本科生、研究生的参考书。

# 图书在版编目（CIP）数据

追踪LinuxTCP/IP代码运行——基于2.6内核/秦健

编著.—北京：北京航空航天大学出版社，2010.4

ISBN 978-7-5124-0048-1

I. ①追…Ⅱ. ①秦…Ⅲ. ①计算机网络—通信协议

IV. ①TN915.04

中国版本图书馆CIP数据核字（2010）第051754号

版权所有，侵权必究。

追踪 Linux TCP/IP 代码运行

基于2.6内核

秦健编著

责任编辑 董立娟

关

北京航空航天大学出版社出版发行

北京市海淀区学院路37号（邮编100191）http://www.buaapress.com.cn

发行部电话：（010）82317024传真：（010）82328026

读者信箱：bhpress@263.net 邮购电话：（010）82316936

印刷有限公司印装 各地书店经销

#

开本：787×960 1/16 印张：37 字数：829千字

2010年4月第1版 2010年4月第1次印刷 印数：5000册

ISBN 978-7-5124-0048-1 定价：69.00元（含光盘1张）

# 前言

学习过TCP/IP协议的读者可能会感觉面对长篇大论的一堆文字往往不知道从何入手，甚至很多读者在认真阅读之后也无法领会协议的作用，更无法进入内核的协议栈阅读代码，以至于无法从事网络方面的开发与维护。然而，高速发展的网络时代要求技术人员必须快速掌握和运用协议的知识，那么有没有一种有效的方法可以帮助我们实现这一目标呢？相信读者可以在本书中找到答案。

理论的学习固然是基础，但是不一定具备了理论才开始阅读代码，就像以往大家只采取背单词、记语法的方式学英语，结果十几年之后仍然不会说英语，不会用英语；因为这种英语教学方法只停留在理论层上，现代的英语教学则打破了以理论为主线的学习方法，从应用性和实用性出发，极大改变了学习的效果。因此，本书借鉴现代英语的学习模式，选择从实际应用出发，以应用程序为主线进入Linux内核，通过应用程序的工作过程层层解析内核的协议栈，揭示协议栈的工作路线及网络的真实过程，帮助读者彻底掌握协议栈，使其在阅读完本书之后有一种登上泰山顶峰的兴奋之情。目前，市场针对Linux网络的讲解书籍已经很多，但内容基本都是对理论的泛泛而谈，有的书籍缺少主线，有的书籍缺少与实际应用的结合，有的书籍采用的内核版本过于陈旧，这些问题加大了阅读的难度，导致很多读者使用“压箱子底”的办法放弃了学习。相反，本书克服了上述问题，并且在描述每一个关键过程时，采取穿插图片和逐行解释的方式解决了这些难点。

本书针对的是2.6版本的Linux内核，对于内核的进程管理、内存管理、文件系统等内容，读者可以借鉴本书推荐书籍或者直接跳过这些内容只阅读核心部分；对于难以理解的内容可以先放一放，往往在读第二遍时就会自然而解，因为内核的知识点是互为补充的。由于内容繁多，不足之处在所难免，请读者指正。有兴趣的朋友，可以到http://qinjiangao786.cublog.cn来做客参与技术讨论。您也可以发电子邮件到:embedded engineer@163.com，与作者进一步交流。

广州工程技术职业学院冯伟老师对本书的图片编辑提供了帮助，罗燕军、杨永祥、张文盛、李华、倪巍、冯伟、刘国兵、葛德奇、朱德良、罗兵、彭海、白瑜、颜诗敏、傅海荧、李柱栋、盛晓辉、修宸、张泽荣、陈嘉顺、时安营、周丹、江均勇、杨硕、罗伟彬、李洪彬、杨春雷、付金平、缪晓及钟海文也对本书的编辑提供了支持，在此向他们的辛勤付出表示感谢。

作者

2010年1月

秦健

嵌入式研发工程师。精通Linux内核，多年从事嵌入式软硬件开发，使用Linux平台设计电子产品。已经34岁的他凭借一颗热爱技术、永不放弃的心，十年如一地坚持技术研发的人生道路，推翻了“三十岁是技术员坟墓”的谬论，为年轻工程师树立了榜样。

内容特色

从事技术性工作十余年间，作者不断地探索、研究有效的学习方法，不断提高学习效果，最终在分析Linux内核中得出了“用中学习、用中理解”的方法。这种方法在本书中得到了很好的运用，能帮助读者顺利进入Linux的网络世界，也能使读者掌握正确的学习方法，这就是我们常讲的“授人以鱼，不如授人以渔”。

本书采取从应用到内核，从实践到理论，从软件到硬件的讲解方式，通过应用程序这条主线，帮助读者剖析网络通信的具体过程和细节，让读者理解协议线的具体内容，提高读者的网络开发能力。读者在学习时不需要具备网络知识，只需要具备一定的C语言基础就可以顺利阅读。

读者对象

3G网络开发人员、嵌入式通信产品开发人员、网络应用开发人员、网络管理人员及网络爱好者、从事网络方向的本科生、研究生。

# 目录

# 第1章 本书的计划

1.1 基本路线和要求 1

技术要点：分析路线 服务器程序 学习要求

1.2 TCP/IP协议层的划分与基本知识 4

技术要点：协议层知识 服务器与客户端的通信路线

1.3 函数到系统调用的过程 6

技术要点: 库函数的真实定义 系统调用的总入口及传值

1.4 网络文件系统 11

技术要点：网络文件系统的特点initcall机制网络文件系统的安装

# 第2章 socket的创建 15

2.1 本章几个重要数据结构· 15

技术要点：socket结构、sock结构、sk_buffer结构、tcp_sock结构的定义

2.2分配并初始化socket结构 23

技术要点：分配socket结构空间 分配文件节点

2.3 使用协议族的函数表初始化 socket 27

技术要点：登记函数表 定义、注册及初始化协议族结构

2.4 分配并初始化 sock 结构 38

技术要点：分配结构空间 初始化 sock 结构及数据包队列头

2.5 TCP协议对sock结构初始化 43

技术要点：TCP协议的初始化函数tcp_sock等结构的关联

2.6 socket与文件系统的关联… 46  
技术要点：分配文件指针和文件号 指定文件操作表

# 第3章 socket地址设置 51

3.1 地址设置接口… 51  
技术要点：地址设置过程 查找socket和文件指针 复制数据到内核  
3.2 地址结构定义… 54  
技术要点：结构的定义及赋值 协议族的设置函数 网络空间结构  
3.3 地址类型· 58技术要点：地址类型的概念查验地址类型获取路由函数表  
3.4设置地址和端口 62技术要点：地址与端口的队列结构 地址与端口的查找与建立  
3.5 网络空间总管init_net 73技术要点：init_net结构的初始化队列头的初始化

# 第4章 路由 78

4.1 路由函数表结构及关系图 78  
技术要点：fib_table 结构、fn-zone 结构、fib_node 结构及 fib_info 结构的关系  
4.2 路由函数表的初始化… 78  
技术要点：路由表队列、路由函数表队列的初始化 路由规则结构、路由函数表结构、路由区结构的定义 子网掩码的作用  
4.3 通过路由函数表查找路由信息 ………………………………………… 100  
技术要点：路由区及路由节点的匹配和查找 路由信息、路由跳转结构的查找  
4.4 路由的设置及相关结构的初如化 … 109  
技术要点：设置路由的三条路线 路由配置结构的定义 路由区的建立 路由信息的创建和调整 队列节点的链入与摘除 路由跳转结构的初始化 路由函数表的查找路由别名结构的建立 路由表的冲刷与释放  
4.5 基于输出方向的路由表查找与创建 ………………………………………… 156  
技术要点：路由键值结构的定义 路由表结构的定义 路由表队列的轮询 路由表的创建与查找  
4.6 基于输入方向的路由表查找与创建 … 189  
技术要点：为数据包查找路由表 创建转发的路由表

第5章 通知链· 200

5.1 设备通知链节点的挂入 200技术要点：通知链节点结构的定义 设备配置结构的定义 设备通知链的建立  
5.2 地址通知链节点的挂入 206  
技术要点：地址通知链的定义和链入  
5.3 通知链的调用和执行 ………………………………………… 207  
技术要点：网络设备结构的登记注册 通知节点函数的调用

第6章 netlink概述 212

6.1 netlink的创建 212  
技术要点：netlink的sock结构定义及初始化  
6.2 注册路由的 netlink 217技术要点：路由的 netlink 结构定义及注册  
6.3 通过 netlink 通信 ………………………………………… 219  
技术要点：netlink 的信息结构、消息头结构、路由消息结构的定义及初始化 netlink 数据包的建立及发送

第7章 监听连接请求 234

7.1 内核的监听函数 234技术要点：协议族监听函数的调用 连接数的控制 监听结构的建立及定义 连接请求结构及队列  
7.2 内核的监听队列 241  
技术要点：监听队列的链入过程 监听队列的睡眠等待和唤醒

第8章 接收连接请求 245

8.1 接收连接函数 245  
技术要点：协议族接收函数的调用 定时等待连接请求  
8.2 异步接收方式 253技术要点：异步接收实例 异步唤醒路线 查询客户端sock结构和连接请求结构  
8.3 获取连接请求 257技术要点：客户端socket、sock结构的对接 获取客户端的地址 INET协议族结构的定义

# 第9章 准备连接请求 262

9.1 内核的连接函数 263

技术要点：客户端程序及服务器程序的通信效果 协议族连接函数的调用 源路由的分类和查找 TCP 协议的 socket 结构定义 端口的查找与复用 SYN 数据包的建立 路由项结构的定义 MTU、MSS 的作用及设置 滑动窗口的初始化

9.2 分配数据包结构和数据块空间 286

技术要点：创建并初始化数据包共享结构的定义及数据块的分类TCP的控制结构发送队列的链入和计数数据包的结构示意图

9.3 构建、发送TCP数据包 299

技术要点：TCP 头部的定义 TCP 层的发送过程 克隆数据包的建立 重发数据包的依据 拥塞报告的建立

9.4 进化成IP数据包 312

技术要点：IP层的发送过程 IP选项的定义 IP头部的定义及初始化 ID编号的生成和设置 链路层头部缓存结构的定义 邻居子系统的发送函数

9.5 进化成以太网数据包 330

技术要点: 链路层头部及其缓存结构的初始化 以太网头部结构的定义及初始化

9.6发送以太网数据包 335

技术要点：分段发送的检测 网卡驱动程序的发送过程

# 第10章 邻居子系统 345

10.1 邻居子系统的初始化 345

技术要点：ARP的邻居表结构、邻居结构的定义 邻居表的链入 查找、创建ARP的邻居函数表

10.2 查找邻居结构 355

技术要点：邻居结构的查找过程

10.3 邻居子系统的发送事件 356

技术要点：邻居结构的状态类型及检测 邻居结构的定时器、定时函数 ARP包的建立及初始化 ARP头部的定义 ARP包的发送

10.4 邻居子系统的接收处理 366

技术要点：ARP数据包类型结构 ARP数据包的接收和处理 解包获取客户端地址更新邻居结构

# 第11章 流量控制 374

11.1 排队规则的初始化… 374  
技术要点：排队规则的定义 网卡设备结构的登记与注册 排队规则的创建及初始化  
排队规则函数表的定义  
11.2 排队规则的入队和发送 382  
技术要点：排队规则的入队操作 排队规则对数据包的流量控制和发送过程

# 第12章 建立连接的过程 388

12.1 驱动程序接收并建立数据包 … 388  
技术要点：网卡驱动程序的接收过程 构建数据包 软中断数据结构的运用 软中断函数的接收过程  
12.2 查找数据包类型且调用其处理函数 400技术要点：数据包类型结构的定义 IP数据包类型及数据包类型队列 IP层的接收过程  
12.3 接收或转发IP数据包 408 技术要点：IP选项的检查 源路由的执行 路由表的转发和接收过程  
12.4 TCP数据包的处理 415技术要点：传输层函数表结构的定义TCP协议的函数表注册TCP层的接收过程查找与唤醒服务器进程  
12.5 3次握手过程 ………………………………………… 427  
技术要点：查找、创建客户端sock结构 建立连接请求结构 创建并发送ACK包  
连接请求的转接

# 第13章 Internet控制信息的传输 446

13.1 发送ICMP信息 446技术要点：ICMP的作用 ICMP的发送函数ICMP信息结构、头部结构、缓存结构的定义ICMP控制结构数组ICMP发送速率的设置  
13.2 接收ICMP信息 458技术要点：ICMP协议的函数表ICMP协议的接收过程

# 第14章 数据包的分段与重组 461

14.1 数据包的分段发送… 461技术要点：数据包的分段示意图IP层的分段函数快发送、慢发送过程分段数据的复制

14.2 数据包的分段接收和重组 472

技术要点：IP层的重组函数整理、合并分段数据包过程

14.3 分段数据包的接收队列 482

技术要点：IP分段队列结构 INET协议族的分段队列头结构网络空间、INET协议族的分段管理结构

14.4 查找与创建分段队列 485

技术要点：IPv4分段信息结构 查找、创建INET分段队列头 初始化IP分段队列结构

14.5 释放和销毁分段队列 489

技术要点：清除分段队列 释放分段数据包和队列头

# 第15章 发送和接收数据包 494

15.1 内核的发送、接收函数 494

技术要点：3种发送、接收的系统调用过程 网络文件系统的发送、接收函数

15.2 客户端发送数据包 501

技术要点：消息结构、缓冲区结构的定义与初始化构建发送数据包TCP层的发送过程TCP层的分段发送阻塞的检测

15.3 服务器接收数据包 529

技术要点：TCP层的接收过程 预处理队列的链入和处理 预处理进程的接收过程复制数据到程序的缓冲区 处理后备队列的数据包

# 第16章 socket的关闭 551

16.1 内核的关闭函数 551

技术要点：网络文件系统的关闭函数 删除、释放异步结构 释放接收队列的数据包释放、销毁 sock 结构、连接请求结构

16.2 服务器与客户端的共同关闭 563

技术要点：服务器与客户端FIN、ACK、RST包的交互过程 状态改变与强制关闭

索引 569

参考文献 579

# 本书的计划

# 1.1 基本路线和要求

# 1. 本书的主要特点

本书以socket应用过程为路线，以函数调用为线索，按照代码的执行线路将整个网络的组织和架构串联成一个整体，这是本书的最大特色。  
> 知识点模块化是本书的另一个特点，全部的知识点被分布到各个章节结合实际场景叙述，有时在函数的过程中登台亮相，也有时随着代码的剖析赫然而出，这种灵活的安排加强了阅读的流畅性。

# 2. socket 的概念与意义

socket这个单词是插座的意思，但是在计算机领域中有时称为插口，也有时称为套接字；无论什么称谓其作用是不变的，它是应用程序在网络通信中的桥梁与纽带。

举一个电话通信的例子，在这个例子中用客户与服务商通电话的过程来说明 socket 的含义与作用。客户需要打电话给服务商，因此例子中有 2 部电话就好比 2 个 socket 进程，电话号码是它们的 socket 地址，客户的电话相当于客户端 socket；同理，服务商的电话相当于服务器的 socket。

现在客户拿起电话开始拨打服务商的电话号码，相当于客户端 socket 向服务器 socket 发出连接请求；服务商如果此时正好空闲就可以接听电话，相当于服务器 socket 在等待连接请求，电话铃响起说明服务器 socket 已经接收到了连接请求，此时服务商拿起电话后双方就可以正式通话了，相当于客户端与服务器的 socket 连接成功；通话过程中客户的声音被传送到服务商的电话中，相当于客户端 socket 发送数据而服务器 socket 接收数据；通话完成后，挂起电话则相当于关闭客户端的 socket 和服务器的 socket。

socket例子表面上看是一个简单的电话通信过程，实际上，有些网络可视电话以及网络监控摄像头正是利用socket为载体传输音频和视频信号，从而实现了视频通话的功能。随着3G与4G通信在国内的迅速兴起，应用socket的产品越来越多，将会遍布我们生活的各个角

落；可以说只要有网络存在，就能看到 socket 的身影，如我们熟知的 3G 上网本和智能手机。在这些产品中，无论采用何种 CPU、何种操作系统，它们都会使用 socket 的网络通信方式，其过程基本与本书将要讲述的内容是一致的，甚至与将要列举的例子如出一辙。

# 3. 分析路线

socket 编程不是本书分析的重点，但是可以透过 socket 程序全面掌握整个网络的工作过程，从而了解整个网络通信的内幕和真相。为使我们的分析过程合理有序，本书将这条 socket 主线分成两条路线：将服务器 socket 程序的执行过程作为第一路线，将客户端 socket 程序的执行过程作为第二路线，两条分析路线正好与 socket 编程要求完全一致，也与电话通信的例子十分吻合。图 1.1 说明了服务器和客户端 socket 的执行路线。

![](images/33a7abebcde7e8e0ab3cdb4ab421fa3913d4d01e42ceae6f546eb44215bc8298.jpg)  
图1.1 本书分析路线图

图1.1被中间虚线分为左右两部分，左边部分是服务器socket程序执行过程，右边部分为客户端socket程序执行过程。本书将按照这两部分的箭头方向分别展开，这两条路线能够使我们的知识点层层衔接从而形成一部完整的电视剧，数据结构是人物，函数是剧情，本书就是剧本。图1.1是引导整个分析过程的核心路线图，读者阅读此书时要经常回来参照此图。

按照图1.1服务器部分的第一路线，我们先从服务器的socket程序开始分析。由于服务器的工作路线与客户端不同，书中分别依据两条路线过程各举例服务器与客户端两个socket程序先分析服务器的程序代码，如代码清单1.1所示；第二路线客户端的程序代码以及它们之间的运行试验都将在第9章中列出，这两个程序的代码可以从本书附带光盘中找到，读者可以复制到Linux下测试运行效果。

# 代码清单1.1 服务器程序代码

```txt
int main()  
{int server_fd,client_fd;int server_len,client_len;struct sockaddr_in server_address;struct sockaddr_in client_address;char back[] \(\equiv\) {"I'mserver"};/\*创建服务器socket\*/server_fd \(=\) socket(AF_INET,SOCK_STREAM,0);/\*为服务器socket指定IP地址和端口，由于是本机测试，故指定服务器的IP地址是192.168.1.1而端口则为 \(9266\times /\) server_address.sin_family \(\equiv\) AF_INET;server_address.sin_addr.s_addr \(\equiv\) inet_addr("192.168.1.1");server_address.sin_port \(\equiv\) htons(9266);server_len \(\equiv\) sizeof(server_address);/\*接着将“电话号码"设定给“电话”，也就是将地址结构与socket挂起钩来，指针赋值可以理解为钩子的挂入本书所称的“钩子函数"实际就是函数指针\*/bind(server_fd,(struct sockaddr\)&server_address,server_len);/\*创建一个socket的监听队列（允许接收10个连接)，监听客户端socket的连接请求\*/listen(server_fd,10);while(1){char recv[20];printf("server is waiting\n");/\*程序运行到此处时，说明客户端的连接请求已经到来，接受它的连接请求，克隆出一个socket与客户端建立连接，并将客户端的“电话号码"记录在client_address中，函数返回建立连接的ID号\*/client_len \(\equiv\) sizeof(client_address);
```

```c
client_fd = accept(server_fd, (struct sockaddr*)&client_address, &client_len);
/* 使用 read 和 write 函数接收客户端字符然后发回客户端 */
read(client_fd, recv, 20);
write(client_fd, back, 20);
printf("received from client = %s\n", recv);
close(client_fd);
} close(server_fd);
exit(0); 
```

在服务器路线的分析过程中要以代码清单1.1为参考，按照程序中的调用函数及过程来理顺服务器网络的内容。代码清单1.1的代码中加入了详细的注释，对比图1.1的第一路线可以看出，两者完全吻和。

# 4. 阅读要求

阅读本书需要读者具备一定的C语言知识，但是对网络协议的基础不做要求。假如读者是初次接触网络尤其是TCP/IP协议，可能面对众多网络资料无从下手，也可能在阅读了大量的理论书籍后依然对整个网络的内容理不出头绪，认真阅读本书可以帮助你迅速跨越这些障碍。

但是，对于缺乏操作系统理论的读者来说可能有种“心有余而力不足”的感觉，尤其是对Linux日积月累而成的庞大内核，其网络部分的内容与内核其他内容是密不可分的。然而，这不会成为阅读本书的阻碍，读者并不需要掌握了内核以后再来阅读本书。考虑到此类情况，书中的许多细节上尽量摆脱与内核的纠缠不清，从而站在独立的角度上解析过程，降低了读者阅读时的难度。因此，如果读者是第一次接触Linux，也完全可以在不具备内核的基础上进行学习。推荐在前两遍阅读时把重点放在网络的过程上而跳过感到困难的内容；再次阅读时，要查阅内核方面的书籍来学习将事半功倍，可同时在理论与实践上获得双丰收。推荐内核方面的书籍是《深入理解Linux内核》和《Linux内核源代码情景分析》。

# 1.2 TCP/IP协议层的划分与基本知识

# 1. TCP/IP 协议层的划分

我们先了解一下关于TCP/IP协议层的划分，如图1.2所示。TCP/IP协议层划分为4层，有些资料上分为5层，第五层是物理层即硬件层，图中只画出了应用层、传输层、网络层和链路层。

根据图1.2的划分情况，本书将陆续展开这套包含所有层次在内的传递过程，甚至包含了

![](images/d8e00cc9a24e450ffd8c3030b4a8d03d6955815ceefb33fb4dc51a3a1067e84f.jpg)  
图1.2 TCP/IP协议层的划分示意图

网卡的驱动程序。读者在后面的章节将会看到，计算机网卡 RealTek 8169、嵌入式 DM9000 和 CS8900 的网卡驱动程序内容也将作为实例穿插进本书。我们通过层与层之间的衔接、过渡和数据接力，结合服务器和客户端路线来彻底理解整个网络的过程及作用。下面先了解一下协议的基本知识。

# 2. TCP/UDP协议的基本知识

TCP（Transmission Control Protocol，传输控制协议）和UDP(User Datagram Protocol，用户数据报协议)协议属于传输层协议。TCP协议提供IP环境下的数据可靠传输，提供可靠数据流传送，具有流控、全双工操作和多路复用的特点，可以面向连接、端到端的数据包发送接收；它事先为所发送的数据开辟出连接通道，然后再进行数据发送。UDP则不为IP环境提供可靠性、流控或差错恢复功能。一般来说，TCP对应的是可靠性要求高的应用，而UDP对应的则是可靠性要求低、传输经济的应用。TCP支持的应用协议主要有Telnet(Internet远程登陆协议)、FTP(远程文件传输协议)、SMTP(简单邮件传输协议)等；UDP支持的应用层协议主要有NFS(网络文件系统)、SNMP(简单网络管理协议)、DNS(主域名称系统)、TFTP(通用文件传输协议)等。

Linux内核严谨地按照TCP协议的要求划分层并组织数据包的传递。本书在主路线的指导下，根据内核执行TCP协议的过程，沿着客户端应用程序传递数据到服务器、数据被服务器网卡接收、最终传递到服务程序的整个过程进行追踪分析。如果按照TCP层的划分来看，则网络数据将沿着客户端应用层到链路层，发送到服务器的链路层，最终到达服务器的应用层。

图1.3是本书描述的服务器与客户端的TCP/IP层的通信路线图，不要与图1.1分析路线图混淆；这是层与层的通信路线隐含在主路线之内。由于层与层的调用非常频繁、复杂，并且多种传输协议交叉使用，故而图1.2只能作为数据传递的宏观概念图来使用；但是在学习

TCP数据包发送和接收内容时对照此图有助于理清数据包在层之间的传递。

![](images/dc1ae267d74a57c964f25413e4ef79fb8f0f476d8e80cfc067e032a6cc0bfd3d.jpg)  
图1.3 本书描述的TCP/IP层通信路线

# 1.3 函数到系统调用的过程

Linux 应用程序使用的 C 运行库是 GNU 的 glibc，读者可以从 GNU 的官方网站下载该库的源码文件，也可以从 Linux 的发布网站 www.kernel.org 下载，本书例子中使用的 glibc 版本是 2.3.6。

服务器程序、客户端程序调用的库函数均可在 glibc 源码中找到。例如, 服务器程序调用的 socket() 函数, 读者就可以打开目录 glibc-2.3.6/sysdeps/generic 中的 socke.c 文件。

# 代码清单1.2 库函数socket()

include <errno.h>   
#include <sys/socket.h> $\text{一} _ { \text{一} }$ Create a new socket of type TYPE in domain DOMAIN, using protocol PROTOCOL. If PROTOCOL is zero, one is chosen automatically. Returns a file descriptor for the new socket, or -1 for errors. \*/ int   
_.socket (domain,type,protocol) int domain; int type; int protocol;   
{ _set_errno (ENOSYS); return -1;

weak alias(_socket,socket) stubwarning(socket) #include $<$ stub-tag.h> #define weak alias(name,aliasname）_weak alias(name,aliasname) #define_weak alias(name,aliasname)\ extern_typeof(name)aliasname_attribute_((weak,alias(#name)));

代码清单1.2中并没有socket()函数声明，取而代之的是使用weak alias()为socket()声明了一个“函数别名”socket()，因此_socket()函数就是库函数socket()。

Weak Alias 是 GCC 编译器扩展内容, 指定了函数的 weak 属性。因而, 在 glibc 库中会经常使用 weak_alia()指定库函数。编译 glibc 时将“函数别名”属性保存在 weak symbol 中, 这种用法可以在其他地方定义真实的 socket() 函数, 编译器会自动识别出哪一个是真实的定义, 这里 socket() 真实的定义是用汇编语言来实现的。读者可以打开目录 glibc -2.3.6/sysdeps/unix/sysv/linux/i386 中的 socket.S 文件。

代码清单1.3 汇编函数_SOCKET()  
ENTRY (_socket)  
movl $SYS_ify(socketcall), \%eax /* System call number in \%eax. */ / * Use # # so'socket' is a separate token that might be # defined. */ movl$ P(SOCKOP_,socket), \%ebx /* Subcode is first arg to syscall. */ lea 4(%esp), %ecx /* Address of args is 2nd arg. */  
/* Do the system call trap. */ ENTER_KERNAL  
//对于使用I386系统内核来说,ENTER_KERNAL和SYS_ify宏声明如下  
# define ENTER_KERNAL int $0x80  
# define SYS_ify(syscall_name) __NR_ # # syscall_name  
# define P(a,b) P2(a,b)  
# define P2(a,b) a# # b

将汇编代码的宏转换一下：

```txt
ENTRY（_socket）
```

```asm
movl $__NR_SOCKETcall, %eax /* System call number in %eax. */
/* Use # # so'socket' is a separate token that might be # defined. */
movl $SOCKOP_SOCKET, %ebx /* Subcode is first arg to syscall. */
lea 4(%esp), %ecx /* Address of args is 2nd arg. */
...
/* Do the system call trap. */
int $0x80 
```

当 glibc 在 Linux 系统中编译时，__NR_SOCKETcall 会从内核 include/asm -x86/unistd_32.h 文件中找到它的定义作为系统调用号。

```m4
define__NR_SOCKETcall 102 
```

SOCKOP_SOCKET 的定义在 glibc - 2.3.6/sysdeps UNIX/sysv/linux/socketcall.h 中。

```txt
defineSOCKOP_SOCKET 1 
```

汇编代码将这个宏值作为socket的调用号保存到寄存器ebx中，代码lea4（%esp）， $\% \mathrm{ecx}$ 是将调用socket()时的参数地址保存到寄存器ecx中。

使用寄存器ebx和ecx传值并没有使用堆栈是因为系统调用在内核空间，进入内核空间必然会引起堆栈的切换，这是CPU保护模式的要求；可是寄存器用于传值的数量毕竟有限，因此，对于服务器程序传递的3个参数：AF_INET、SOCK_STREAM、0，采取传递指针的形式（相当于将这些参数看作参数数组）把指针保存到ecx中，我们后面会看到系统调用要用该指针取得每个参数。

当服务器程序运行后,调用socket()函数就会执行glibc中的上述代码,从而将系统调用号102保存到寄存器eax中,然后执行软中断指令int $0x80到达系统调用的总入口system_call()函数;这个函数也是用汇编实现的,在Linux内核目录arch/x86/kernel/entry_32.S文件中。

# 代码清单1.4 汇编函数system_call()

```txt
ENTRY(system_call)   
yscall_call: call \*sys_call_table(，%eax,4) 
```

代码清单1.4中省去了中断执行的许多细节以及寄存器传递参数的内容，system_call()最终使用汇编call指令执行sys_call_table系统调用表102处的函数指针。系统调用表sys_call_table在arch/x86/kernel/syscall_table_32.S文件中。

```txt
ENTRY(sys_call_table)  
...  
.long sys_SOCKETcall /* 102*/ 
```

对照代码清单1.5中的内容，socket()执行的系统调用函数是sys_SOCKETcall()，它也是bind()、listen()、accept()等函数的系统调用入口。

读者可以根据图1.1和代码清单1.1所列出的函数，在glibc源码中看到它们都会执行系统调用sys_SOCKETcall()函数(请注意本书针对的协议是IPV4，Linux内核的版本是2.6.26)。函数sys_SOCKETcall()是内核提供给socket通信的总入口，对于这个函数的分析我们把重点放在与路线有关的内容上，因此只摘取了sys_SOCKETcall()函数中关键的代码部分，如代码清单1.6所示。

# 代码清单1.6 sys_SOCKETcall()的代码片断

```c
2007 asmlinkage long sys_SOCKETcall (int call, unsigned long __user *args)  
2008 {  
2009 unsigned long a[6];  
2010 unsigned long a0, a1;  
2017 if (copy_from_user(a, args, nargs(call))) //复制参数到这里的数组  
2018 return -EFAULT;  
2024 a0 = a[0];  
2025 a1 = a[1];  
2026  
2027 switch (call) { //根据系统调用号确定执行函数  
2028 case SYS_SOCKET:  
2029 err = sys_SOCKET(a0, a1, a[2]);  
} 
```

sys_SOCKETcall()函数是Linux内核socket的系统调用入口，前面介绍了函数的两个参数是分别通过寄存器ebx和ecx来传递的，参数call是具体的socket调用号，ebx寄存器保存值为1，这个值与2028行的宏SYS_SOCKET相同。

参数args是指针，是ecx寄存器传递的参数数组。由于服务器程序在用户空间(Linux分为内核空间和用户空间)而系统调用函数在内核空间，需要将这些参数从服务器程序复制到内核中，即从用户空间复制到内核空间，复制函数为copy_from_user()。

另外，还需要为copy_from_user()函数明确复制的参数个数，这是由nargs[]数组来决定的，以参数call为下标，其传递值为1，从数组中可以找到对应的参数个数。

# 代码清单1.7 nargs[]数组的定义

```c
1990 #define AL(x) ((x) * sizeof(unsigned long))  
1991 static const unsigned char nargs[18] = {  
1992 AL(0), AL(3), AL(3), AL(3), AL(2), AL(3),  
1993 AL(3), AL(3), AL(4), AL(4), AL(4), AL(6),  
1994 AL(6), AL(2), AL(5), AL(5), AL(3), AL(3)  
1995 }; 
```

对应 call 处的值是 AL(3)，再通过宏 AL()的定义得到复制的 3 个参数的字节总数为 12。于是，copy_from_user()函数把 args 指向的 3 个参数 AF_INET、SOCK_STREAM、0 从用户空间的服务器程序复制到内核空间数组 a 中。

可以看到，include/linux/net.h中规定了调用号call的详细内容，细心的读者可以对照一下glibc对这些调用号的定义，它们在目录sysdeps/unix/sysv/linux的socketcall.h文件中，可以看出Linux内核对这些调用号的定义完全与glibc相同，这也是Linux内核对glibc版本严格要求的原因。

# 代码清单1.8 参数call的对应内容

```c
32 #define SYS_SOCKET 1 /* sys_SOCKET(2) */
33 #define SYS_BIND 2 /* sys_bind(2) */
34 #define SYS_connect 3 /* sys_connect(2) */
35 #define SYS_LISTEN 4 /* sys.listen(2) */
36 #define SYS_ACCEPT 5 /* sys.accept(2) */
37 #define SYS_GETSOCKNAME 6 /* sys.getsockname(2) */
38 #define SYS_GETPEERNAME 7 /* sys.getpeername(2) */
39 #define SYS_SOCKETPAIR 8 /* sys_SOCKETpair(2) */
40 #define SYS_SEND 9 /* sys_send(2) */
41 #define SYS_recv 10 /* sys_recv(2) */
42 #define SYS_SENDTO 11 /* sys_sendto(2) */
43 #define SYS_recvFROM 12 /* sys_recvfrom(2) */
44 #define SYS_SHUTDOWN 13 /* sys_shutdown(2) */
45 #define SYS_SETSOCKOPT 14 /* sys_setsockopt(2) */
46 #define SYS_GETSOCKOPT 15 /* sys_getsockopt(2) */
47 #define SYS_SENDMSG 16 /* sys_sendmsg(2) */
48 #define SYS_recvMSG 17 /* sys_recvmsg(2) */ 
```

根据 call 的具体定义就可以明确 sys_SOCKETcall 函数 2027 行的 switch 路线。注意, 代码清单 1.8 注释中的后缀 (2) 表示这些定义均是系统调用号。

sys_SOCKETcall()函数对于服务器程序和客户端程序来说就是一扇大门，服务器程序和客户端程序调用的函数都要通过这扇门找到各自对应的函数。代码清单1.1中调用的socket()已经找到了其对应系统调用函数sys_SOCKET()，这将是创建一个socket的过程，我们只分析其重要的部分。为了清楚显示函数的调用过程，本书在以后的代码清单前面用“→”符号来表达调用路线。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET()   
1216 asmlinkage long sys_SOCKET(int family,int type,int protocol)   
1217{   
1221 retval $=$ sock_create(family,type,protocol,&sock);//创建socket   
1225 retval $=$ sock_map_fd(sock);//与文件系统建立关联   
1236}

从1221行的函数名称上就可以看出，sock_create()的作用就是创建一个服务器的socket插口，传递了3个参数，family为AF_INET,type为SOCK_STREAM,protocol为0。对这个函数的分析将在下一章进行。1225行调用的sock_map_fd()函数为新建的socket在网络文件系统中申请文件号和文件描述符结构。

# 1.4 网络文件系统

在分析 sock_create()过程之前，必须先了解一下网络的初始化入口，这有助于理解后面的分析过程。我们先介绍一个与文件系统有关的结构变量 sock.fs_type，它在 2.6.26 内核的 net/socket.c 中。

# 代码清单1.9 sockfs_type结构变量的定义

```txt
302 static struct file_system_type sockfs_type = {  
303 .name = "sockfs",  
304 .get_sb = sockfs_get_sb,  
305 .kill_sb = kill_anonSUPER,  
306 }; 
```

file_system_type结构代表Linux内核的各种网络文件系统，每一种文件系统必须要有自己的file_system_type结构。sock.fs_type结构定义代表sockfs的网络文件系统，但它并没有真实的物理介质，因此称为虚拟文件系统。虚拟文件系统的安装过程与真实文件系统不同，在推荐的内核书中详细讲述了虚拟文件系统和数据结构file_system_type的意义，在此不再重

复这些内容，但是我们还是需要了解网络文件系统的初始化内容。

Linux 在启动时会执行 init/main.c 中的初始化函数 kernel_init()，这个函数调用了 do_base_setup()，从而调用 do_initcalls() 函数来执行所有的 init 初始化函数。

kernel_init() $\rightarrow$ do/basic_setup() $\rightarrow$ do_initcalls() $\rightarrow$ do_one_initcall()  
741 static void __init do_initcalls(void)  
742 {  
......  
745 for (call = __initcall_start; call < __initcall_end; call++)  
746 do_one_initcall( * call); //调用每一个初始化函数  
......  
750 }  
696 static void __init do_one_initcall (initcall_t fn)  
697 {  
......  
708 result = fn(); //初始化函数  
......  
736}

函数746行调用了do_one_initcall(),这就是称为initcall机制的执行函数。initcall机制实际上是在内核复用了GCC编译器的功能来设置内核的初始化函数，这些函数地址存放在的__initcall_start到__initcall_end中，do_one_initcall()函数将调用所有登记在initcall中的初始化函数。在socket.c文件中可以看到，2188行core_initcall()将sock_init()函数登记到initcall中。

# 代码清单1.10 sock_init()的登记代码

```c
2188 core_initcall(ock_init); /* early initcall */ 
```

在include/INIT.h中可以看到core_initcall的定义。

# 代码清单1.11 core_initcall()的定义

```txt
168 #define __define_initcall(level,fn,id)  
169 static initcall_t __initcall_# #fn# #id__used  
170 __attribute_((section_".initcall" level ".init")) = fn  
...  
#define core_initcall(fn)_define_initcall("1",fn,1)  
...  
191 #define device_initcall(fn) __define_initcall("6",fn,6)  
...  
196 #define __initcall(fn) device_initcall(fn) 
```

```c
251 #define module_init(x)_initcall(x); 
```

GCC编译时把sock_init函数“安装”到initcall中，Linux内核把初始化函数都放在init-call中实现了，这样在初始化完成后，这部分函数的空间可以回收再利用；驱动程序中常用的module_init注册用户自定义的初始化函数也是通过initcall来实现的。sock_init()函数2188行注册成功后，内核初始化时会执行sock_init()函数来登记网络文件系统。

kernel_init() $\rightarrow$ do/basic_setup() $\rightarrow$ do_initcalls() $\rightarrow$ do_one_initcall() $\rightarrow$ sock_init()   
2157 static int __init sock_init(void)   
2158 {....   
2174 init_inodecache();   
2175 register_filesystem(&sock.fs_type);//注册网络文件系统   
2176 sock_mnt $=$ Kern.mount(&sock.fs_type);//安装网络文件系统   
2186}

sock_init()函数主要是将sockfs_type登记到内核安装网络文件系统，2174行处创建了一块用于网络文件节点和socket的高速缓存。2175行的register_filesystem()函数作用是将网络文件系统注册到Linux中，而kernmount()函数则完成了其在Linux内核中的安装，并在内核中建立了网络文件系统的安装点。这两个函数均与内核当前使用的根文件系统相关。

在安装的过程中，内核会调用 sockfs_type 数据结构的 get_sb()钩子函数（本书有时称函数指针为钩子函数），而把结构指针称为钩子结构，强化对这些指针的理解，因为它们就像钩子一样灵活，可以随意挂入和摘除。在代码清单 1.9 中可以看到，sockfs_type 结构中挂入到 get_sb() 的钩子函数为 sockfs_get_sb()，由该函数完成网络文件系统的安装。

sock_init() $\rightarrow$ kernmount() $\rightarrow$ 中间过程从略 $\rightarrow$ sockfs_get_sb()   
292 static int sockfs_get_sb(struct file_system_type \*fs_type,   
293 int flags，const char \*dev_name，void \*data,   
294 structvfsmount\*mnt)   
295{   
296 return get_sb_pseudo(fs_type，"socket:”，&sockfsOps，SOCKFS_MAGIC,   
297mnt）;//完成文件系统安装   
298}

296 行处转交给了 get_sb_pseudo()函数去执行，传递的第一个参数是 sockfs_type，第二个参数是要求创建 socket 名称的根目录项，第三个参数是关于网络文件系统的超级块操作函数表 sockfsOps，而最后一个参数是网络文件系统的安装点。这里重点强调一下 sockfsOps 网络文件系统的超级块操作函数表，创建 socket 时会用到该函数表的内容。

```txt
286 static struct super_operations sockfsOps = {  
287 .alloc_inode = sock_alloc_inode,  
288 .destroy_inode = sockdestroy_inode,  
289 .statfs = simple_statfs,  
290 }; 
```

该函数表对网络文件系统的节点和目录提供了具体的操作函数，以后涉及网络文件系统的重要操作均会到该函数表中查找对应的操作函数，也就是所谓的钩子函数。例如，Linux内核在创建socket节点时会查找sockfsOps的alloc inode()钩子函数，从而转到sock_alloc_inode()完成节点的建立。

get_sb_pseudo()与文件系统密切相关，由它完成网络文件系统的安装过程，从而为下一章socket的创建铺平了道路。

# socket 的创建

# 2.1 本章几个重要数据结构

我们先来了解 socket 数据结构的定义, 它在 include/linux/net.h 中。

# 代码清单2.1 socket结构的定义

```c
117 struct socket{  
118 socket_state state; //socket的状态  
119 unsigned long flags; //socket的标志位  
120 const struct protoOps * ops; //socket的函数操作表  
121 struct fasync_struct *fasync_list; //socket的异步唤醒队列  
122 struct file *file; //与socket关联的文件指针  
123 struct sock *sk; //代表具体协议内容的sock结构指针  
124 wait_queue_head_t wait; //用于等待队列  
125 short type; //socket的类型  
126};
```

代码清单2.1是通用BSD的socket定义。特别应该注意的是sock结构变量，这个结构体的定义非常大，sock结构体是根据使用的协议而挂入socket，每一种协议都有此结构变量，之所以从socket中分离出sock一个这样重要的结构是因为socket是通用的套接字结构体，而sock与具体使用的协议相关。总之，公共的通用部分放在socket结构中，而通用部分的放在sock结构体，这就是socket的定义内容相对少而sock定义内容相对庞大的原因。

# 代码清单2.2 sock结构的定义

```c
198 struct sock{  
199 /\*  
200 \*结构inet_timeout SOCK也会使用sock_common，  
201 \*所以不要在__sk_common前面添加任何内容  
202 \*/  
203 struct sock_common \_sk_common; //与inet_timeout SOCK共享使用
```

```c
204 #define sk_family __sk_common.skc_family //地址族  
205 #define sk_state __sk_common.skc_state //连接状态  
206 #define sk_reuse __sk_common.skc_reuse //确定复用地址  
207 #define sk_bound_dev_if __sk_common.skc_bound_dev_if //绑定设备ID  
208 #define sk_node __sk_common.skc_node //用于链入主哈希表  
209 #define sk_bind_node __sk_common.skc_bind_node //链入绑定哈希表  
210 #define sk_refcnt __sk_common.skc_refcnt //使用计数器  
211 #define sk_hash __sk_common.skc_hash //查找哈希表时的哈希值  
212 #define sk_prot __sk_common.skc_prot //协议函数表  
213 #define sk_net __sk_common.skc_net //所属网络空间  
214 unsigned char sk_shutdown:2，//是否关闭  
//SEND_SHUTDOWN和RCV_SHUTDOWN的掩码，冒号后边的2表示占2位  
215 sk_no_check:2，//是否检查数据包  
216 sk_userlocks:4；//用户锁  
//由SO_SNDBUF和SO_RCVBUF设置，冒号后边的4表示占用4位  
217 unsigned char sk_protocol; //使用协议族的哪一种协议  
218 unsigned short sk_type; //socket的类型，例如SOCK_STREAM等  
219 int sk_rcvbuf; //接收缓冲的长度(字节数)  
220 socket_lock_t sk_lock; //用于同步  
221 /\*  
222 \*用于后备队列使用，它总是使用自旋锁并且“懒散”式的访问，因此而定义  
223 \*  
224 \*  
225 /\*  
226 struct{  
227 struct sk_buffer *head; //记录最先接收到的数据包  
228 struct sk_buffer *tail; //记录最后接收到的数据包  
229 }sk_backlog; //后备队列  
230 wait_queue_head_t \*sk_sleep; //sock等待队列  
231 struct dst_entry \*sk.dst_cache; //路由项缓存  
232 struct xfrm_policy \*sk_policy[2]; //流策略  
233 rwlock_t sk.dst_lock; //路由项缓存锁  
234 atomic_t sk_rmem_alloc; //接收队列的字节数  
235 atomic_t sk_wmem_alloc; //发送队列的字节数  
236 atomic_t sk_omem_alloc; //可选择的字节数,"o"是"可选择"或者"其他"  
237 int sk_sndbuf; //发送缓冲的总长度  
238 struct sk_buffer_head sk_receive_queue; //接收到的数据包队列  
239 struct sk_buffer_head sk_write_queue; //正在发送的数据包队列  
240 struct sk_buffer_head skAsync_wait_queue; //DMA复制的数据包
```

```c
241 int sk_wmem_queue; //全部数据包占用内存计数  
242 int sk_forward_alloc; //记录可用内存长度  
243 gfp_t sk_allocation; //分配模式  
244 int sk-routeCaps; //路由的兼容性标志位  
245 int sk_gso_type; //GSO通用分段的类型  
246 unsigned int sk_gso_max_size; //用于建立通用分段GSO的最大长度  
247 int sk_rcvlowat; //SO_RCVLOWAT设置  
248 unsigned long sk_flags;  
//SO_LINGER(1_onoff), SO_BROADCAST, SO_KEEPALIVE, SO_OOBINLINE 设置  
249 unsigned long sk_lingertime; //停留时间，确定关闭时间  
250 struct sk_bufferhead sk_error_queue; //很少使用  
251 struct proto *sk_prot Creator; //sock创建接口  
252 rweight_t sk_callback_lock; //为后半部处理使用的锁  
253 int sk_err, //出错码  
254 sk_errsoft; //持续出现的错误  
255 atomic_t skdrops; //原始socket发送的计数器  
256 unsigned short sk ACK_backlog; //当前监听到的连接数量  
257 unsigned short sk_max_ACK_backlog;  
//在listen()中监听到的连接数量  
258 __u32 skpriority; //优先级  
259 struct ucred sk_peercred; //SO_PEERCRED设置  
260 long sk_rcvtimeo; //SO_RCVTIMEO设置接收超时时间  
261 long sk SNDtimeo; //SO_SNDTIMEO设置发送超时时间  
262 struct sk_filter *sk_filter; //sock的过滤器  
263 void *skprotinfo; //私有区域，当不使用slab高速缓存时由协议族定义  
264 struct timer_list sk_timer; //sock的冲刷定时器  
265 ktime_t sk_stamp; //最后接收数据包的时间  
266 struct socket *sk_sock; //对应的socket指针  
267 void *sk_user_data; //RPC提供的数据  
268 struct page *sk_sndmsg_page; //发送数据块所在的缓冲页  
269 struct sk_buffer*sk_send_head; //发送数据包的队列头  
270 __u32 sk_sndmsg_off; //发送数据块在缓冲页的结尾  
271 int sk_write_pending; //等待发送的数量  
272 void *sk_security; //用于安全模式  
273 __u32 sk_mark; //通用的数据包掩码  
274 /*XXX 4 bytes hole on 64 bit */  
275 void (*sk_state_change)(struct sock *sk);  
//在sock的状态改变后要调用的函数  
276 void (*sk_data_ready)(struct sock *sk, int bytes);
```

```c
//在数据被处理后要调用的函数  
277 void (*sk_write_space)(struct sock *sk);  
//当发送空间可以使用后，调用的函数  
278 void (*sk_error_report)(struct sock *sk);  
//处理错误的函数  
279 int (*sk_backlog_rcv)(struct sock *sk, struct sk BUFF *skb);  
//处理库存数据包函数  
281 void (*sk-destruct)(struct sock *sk);  
//sock的销毁函数  
282};
```

可以看到，sock结构的内容非常多，这也是其不与socket结构放在一起的原因。socket延续了虚拟文件系统(VFS)中的做法，VFS文件系统中有很多数据结构（比如文件节点inode）都是采取这种“提取公因子”的方法，把重要项放在与应用系统关系密切的结构里，其他相对独立的内容(结构变量)因为要占用大量内存空间，而将这此结构变量分离出来放在另外一些结构中，再让两个结构彼此关联。

结构内容中公用部分的就会采取提取公因子的方法提取出来共享使用，做法就是把与应用程序密切相关的公用部分提取出来放在了socket结构中，而与协议相关的内容则放在socket结构中。我们在后面还会看到一些专用的结构体，如AF_INET协议族的私有结构体inet SOCK(参见本书索引)、unix使用的AF_INET协议族结构体unix SOCK(未列入本书)。

如果读者不理解这种分隔方法，可以将其分成公用、通用、专用部分来记忆。公用部分是socket结构，通用部分是sock结构，而专用部分则是具体协议族使用的结构，比如inet SOCK结构体。假如把上述结构体都合在一起放在socket的定义中，其结果必然形成一个庞然大物，很多与具体协议相关的结构变量大多数时间是处于不使用的空闲状态的，但是分配结构空间时还要同时为它们开辟相应的内存空间，这样的使用方法不灵活还要浪费大量的系统内存。

图2.1说明了socket、sock以及inet_sock结构体中分隔及包含关系。读者清楚了分隔结构的原因后，对今后众多不同的结构体就不会感到杂乱无章了。面对结构体时，首先不是立刻分析它，而应先推理其产生的原因，随后在代码的理解中详细体会其作用；这个过程不仅有趣味，而且潜移默化地掌握了结构的使用技巧，碰到巨型的数据结构也会轻松而解。

sk_buffer数据包结构体是一个非常重要的数据结构，每个协议都是用该结构体用于封装、载运数据包；也就是说，每一个数据包都要用一个sk_buffer数据结构来表示，这里只列出重要部分的内容。

# 代码清单2.3 sk_buffer结构的定义

```txt
251 structsk_buffer{   
252 /\*这两个变量必须放在前面\*/ 
```

![](images/bec92bed3f061341b0cee52dd3dc67424f36e19f16ef2711120d69c5aa9a3ddf.jpg)  
图2.1 分隔关系图

```c
253 struct sk_buffer \* next; //队列中的下一个数据包  
254 struct sk_buffer \* prev; //队列中的前一个数据包  
255  
256 struct sock \* sk; //指向所属的sock结构  
257 ktime_t tstamp; //数据包到达的时间  
258 struct net_device \* dev; //接收数据包的网络设备  
259  
260 union{  
261 struct dst_entry \* dst; //路由项  
262 struct rtable \* rtable; //路由表  
263 };  
264 struct sec_path \* sp; //用于xfrm的安全路径  
265  
266 /*下方的cb是控制数据块，它被每个层使用，如果你想传输自定义变量内容，则它们放在个数组中，前提是必须通过skbClone()函数克隆一个数据包  
271 \*/  
272 char cb[48];  
273  
274 unsigned int len, //全部数据块的总长度  
275 data_len; //分段、分散数据块的总长度  
276 u16 mac_len, //链路层头部的长度  
277 hdr_len; //在克隆数据包时可写的头部长度  
278 union{  
279 _wsum csum; //校验和也可以称作检验和用于验证目的  
280 struct{  
281 __u16 csum_start; //校验和在数据包头部skb->head中的起始位置  
282 __u16 csum_offset; //校验和保存到csum_start中的位置
```

```c
283 }；  
284 }；  
285 __u32 priority; //数据包在队列中的优先级  
286 __u8 local_df:1, //是否允许本地数据分段  
287 cloned:1, //是否允许被克隆  
288 ip_summed:2, //IP校验和标志  
289 nohdr:1, //运载时使用，表示不能修改头部  
290 nfctinfo:3; //数据包连接关系  
291 __u8 pkt_type:3, //数据包的类型  
292 fclone:2, //数据包克隆状态  
293 ipvs_property:1, //数据包所属的ipvs  
294 peeked:1, //数据包是否处于操作状态  
295 nf_trace:1; // netfilter对数据包的跟踪标志  
296 __be16 protocol; //底层驱动使用的数据包协议  
297  
298 void (*destructor)(struct sk_buffer *skb); //销毁数据包的函数  
304 struct nf_bridge_info *nf_bridge; //关于网桥的数据  
331 sk_buffer_data_t transport_header; //指向数据块中传输层头部  
332 sk_buffer_data_t network_header; //指向数据块中网络层头部  
333 sk_buffer_data_t mac_header; //指向数据块中链路层头部  
334 //下面这些内容必须放在结构末尾，参考alloc_skb()函数的内容  
335 sk_buffer_data_t tail; //指向数据块的结束地址  
336 sk_buffer_data_t end; //指向缓冲块的结束地址  
337 unsigned char *head, //指向缓冲块的开始地址  
338 *data; //指向数据块的开始地址  
339 unsigned int truesize; //数据包的实际长度，结构长度与数据块长度之和  
340 atomic_t users; //数据包的使用计数器
```

tcp_sock结构定义非常大，其内容与tcp协议紧密相关，它的重要作用随着后续的分析会越来越清晰，这里只列出部分内容。

# 代码清单2.4 tcp SOCK结构的定义

242 struct tcp SOCK{  
243 /\*inet_connection SOCK//结构变量必须处于tcp SOCK头部，其原因在后面解释  
244 struct inlet_connection SOCK $\text{串}$ inlet conn;  
245 u16 tcp_header_len; /\*发送的tcp头部字节数\*/  
246 u16 xmit_size_goal; /\*分段传送的数据包数量\*/

```c
/\* 头部的预置位  
\* 0x5?10<<16+snd_wnd in net byte order\*/_be32 pred_flags;  
/\* 根据RFC793标准定义的变量。可以参考RFC793和RFC1122了解这些内容\*/u32rcv_nxt;/\*下一个要接收的目标\*/u32 copied_seq;/\*代表还没有读取的数据\*/u32rcv_wup;/\*rcv_nxt在最后一次窗口更新时内容\*/u32snd_nxt;/\*下一个要发送的目标\*/u32snd_una;/\*第一个要ACK的字节\*/u32snd_sml;/\*最近发送数据包中的尾字节\*/u32rcv_tstamp;/\*最后一次接收到ACK的时间\*/u32lsndtime;/\*最后一次发送数据包的时间\*//\*直接复制给用户的数据\*/struct{structsk BUFF_head prequeue;//预处理队列structtask_struct \*task; //预处理进程structiovec \*iov; //用户程序(应用程序)接收数据的缓冲区int memory; //预处理数据包计数器intlen; //预处理长度#ifdefCONFIG_NET_DMA/\*异步复制的内容\*/structdma chan \*dma chan;int wakeup;structdma_pinned_list \*pinned list;dma_cookie_t dma_cookie;#endif}ucopy;u32 snd wl1;/\*窗口更新的顺序\*/u32snd_wnd;/\*期望接收的窗口\*/u32max_window;/\*从对方获得的最大窗口\*/
```

```c
288 u32 mss_cache; /*有效的mss缓存，不包括SACKS*/  
289  
290 u32 window_clamp; /*对外公布的最大窗口*/  
291 u32 rcv_ssthresh; /*当前窗口*/  
292  
293 u32 frto_highmark; /*在RTO时的snd_nxt*/  
294 u8 reordering; /*预设的数据包数量*/  
295 u8 frto counter; /*RTO后的ack次数*/  
296 u8 nonagle; /*是否使用Nagle算法*/  
297 u8 keepalive_probes; /*允许持有的数量*/  
306 u32 packets_out; /*处于飞行中的数据包数量*/  
307 u32 retrans_out; /*转发的数据包数量*/  
308 /\*  
309 \* 接收选项  
310 \*/  
311 struct tcp_options_received rx_opt;  
312  
313 /\*  
314 \* 慢起动与阻塞控制(参考Nagle,和Karn&Partridge)  
315 /\*  
316 u32 SND_ssthresh; /*慢起动的起点值*/  
317 u32 SND_cwnd; /*发送的阻塞窗口*/  
318 u32 SND_cwnd_cnt; /*线性计数器*/  
319 u32 SND_cwnd_clamp; /*不允许snd_cwnd超过的值*/  
320 u32 SND_cwnd_used;  
321 u32 SND_cwnd_stamp;  
322  
323 struct sk_bufferhead out_of_order_queue; /*超出分段规则的队列*/  
324  
325 u32 rcv_wnd; /*当前接收窗口*/  
326 u32 write_seq; /*tcp发送数据的顺序号*/  
327 u32 pushed_seq; /*最后送出的顺序号,需要通知窗口*/  
388 /\*接收队列空间\*/  
389 struct {  
390 int space;  
391 u32 seq;  
392 u32 time;
```

```txt
393 }rcvq_space;   
394   
395 /\*TCP指定的MTU检验内容\*/   
396 struct{   
397 u32 probe_seq_start;   
398 u32 probe_seq_end;   
399 }mtuprobe;   
408}；
```

本书对一些结构有时采取“用中理解”的方式，对某些结构体定义不详细的说明，也就是在不了解结构体作用和定义的情况下直接阅读代码。这样在分析函数之后，我们自然能够理解结构体的作用和定义。这是从实践到理论的学习方法，就像专用结构体变量放在公用部分不用就会造成内存浪费一样，如果把全部的结构体说明和作用罗列在前，让读者花费时间与精力来记忆和理解这些生硬的定义，这与浪费内存没有什么两样，学习效果反而打折。Linux内核中有种经典的“写时复制”的方法，也称为“懒散方法”，有必要吸收到本书的学习之中，因而本书推荐“用中理解”方法来学习内核中的结构体。

试想一下，程序员在编写程序时是先定义结构体还是先编写函数？答案可能有两种：第一种是先编写函数，根据函数的过程来产生结构的需求从而有了结构体的定义；第二种是按照协议规定，如TCP头部和IP头部结构体的定义，这些是协议规定的结构体，因而结构体定义在先，函数编写在后。两种答案虽然相反，可是深入思考一下协议的由来也是经过实践总结而来的，从而得出了从实践到理论的结论。

有时我们需要站在程序员的角度来理解结构体的作用和定义，逆向推理结构体是因何产生、因何而用，这种方式不但提高了理解、阅读代码的水平，更能增强逻辑思维的能力，进而面对任意一段代码的时候从容不迫而游刃有余。Linux之父李纳斯·托沃兹曾经说过，最好的老师是源代码。目前，市场上关于Linux的书籍非常多，绝大多数书籍都是用文字来描述内核的原理和构成，许多读者在完成此类书籍的阅读之后，面对Linux源代码还是“丈二的和尚摸不着头脑”，本书在征求了大量读者的建议后，选择了折中的道路，在源代码基础上尽力对结构体定义加以详尽描述与说明。

# 2.2 分配并初始化socket结构

1.2节介绍了函数到系统调用的过程，我们在此基础上继续分析socket创建过程。注意本书针对的是2.6.26版本的内核，按照图1.1所示的服务器程序路线，我们已经在1.2节看过了创建socket的函数sys_SOCKET()，在这个函数中调用sock_create()来完成创建。这个函数在/net/socket.c中。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create()   
1206 int sock_create(int family, int type, int protocol, struct socket \* \*res)   
1207 {   
1208 return __sock_create(current-> nsproxy-> net_ns, family, type, protocol, res, 0);   
1209 }

socket结构定义已经在代码清单2.1中列出了，函数实际是调用__sock_create()来执行创建任务的，这个函数在同一个文件中。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ _sock_create()   
1093 static int_sock_create(struct net \*net, int family, int type, int protoc   
1094 struct socket $^ { \text{串} }$ res,int Kern)   
1095 {   
1096 int err;   
1097 struct socket \* sock;   
1098 const struct netProto_family \*pf;   
1132 sock $=$ sock_alloc(); //分配socket结构空间   
1140 sock->type $=$ type; //记录socket类型   
1149 if(net_families[family] == NULL）//检查协议族操作表   
1150 request_module("net-pf-%d",family); //安装协议族操作表   
1151 #endif   
1152   
1153 rcu_read_lock();   
1154 pf $=$ rcu_dereference(net_families[family]); //取得协议族操作表   
1169 err $=$ pf->create(net,sock,protocol); //执行协议族的创建函数   
1188 \*res $=$ sock; //返回创建的 socket   
1204

代码中只列出了核心部分，因而省略了一些代码，余下的“主干”部分就是要分析的重点，1132行调用函数sock_alloc()为服务器程序分配socket结构和文件节点。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ sock_alloc() 479 static struct socket \* sock_alloc(void)   
480 {   
481 struct inode \* inode;

482 struct socket \* sock;   
483 //在网络文件系统中创建文件节点同时分配socket结构   
484 inode $=$ new inode(sock_mnt->mnt_sb);   
485 if(!inode)   
486 return NULL;   
487   
488 sock $=$ SOCKET_I(inode); //取得socket结构指针   
489   
490 inode->i_mode $=$ S_IFSOCK|S_IRWXUGO;   
491 inode->iuid $=$ current->fsuid;   
492 inode->igid $=$ current->fsgid;   
493   
494 get_cpu_var(sockets_in_use)++;   
495 put_cpu_var(sockets_in_use);   
496 return sock;   
497

代码涉及文件系统的 inode 文件节点, 调用 new inode() 分配节点, sock_mnt 是 socket 网络文件系统的根节点。这里是在 socket 网络文件系统中分配一个 inode 节点, 分配节点后服务器程序便可以通过函数 read() 和 write() 找到 socket 进行读/写操作, 488 行看到调用 SOCKET_I(), 这是一个内联函数。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ sock_alloc() $\rightarrow$ SOCKET_I()   
700 static inline struct socket \* SOCKET_I(struct inode \*inode)   
701 {   
702 return &container_of(inode, struct socket_alloc, vfs_inode) -> socket;   
703 }   
446 #define container_of(ptr,type,member)({   
447 const sizeof(( (type\*)0) -> member）\*mptr $=$ (ptr);   
448 （type\*)( (char\*)_mptr - offsetof(type,member));   
449

702行处使用了宏container_of。这个宏在Linux中经常出现，主要作用是依据ptr取得type的指针，因此我们把702处的代码转换一下：

```txt
define container_of (inode, struct socket_alloc, vfs_inode) {{ const sizeof((struct socket_alloc *0) -> vfs_inode) __mpr = (inode); (struct socket_alloc *) ((char *) __mpr - offsetof(struct socket_alloc, vfs_inode));}} 
```

转换后的代码中出现一个数据结构 socket_alloc，这个结构只是封装了 socket 和文件节

点在里面。

```txt
struct socket_alloc {
    struct socket socket; //分配的 socket结构
    struct inode vfs_inode; //分配的文件节点结构
};
```

除此之外还引用了另一个宏 offsetof。

```txt
define offsetof (TYPE, MEMBER) ((size_t) &((TYPE *0))->MEMBER) 
```

根据702行的调用代入转换成：

```txt
offsetof(struct socket_alloc,vfs_inode) ((size_t)&((struct socket_alloc * )0) -> vfs_inode) 
```

分析一下 container_of 宏的内容，((struct socket_alloc *)0) -> vfs_inode 是假设数据结构 socket_alloc 正好在 0 处时 vfs_inode 的地址（指针），相当于 vfs_inode 在数据结构中的偏移量，宏名称 offsetof 因此而生。container_of 宏假设 vfs_inode 的地址是节点 inode 的地址，然后减掉 vfs_inode 的偏移量，就得到了当前数据结构 socket_alloc 的起始地址，其起始地址正是 socket 的数据结构，从而得到了 socket 指针。

可是我们并没有在代码中看到对socket_alloc结构的分配，为什么可以通过container_of宏得到新分配的socket指针呢？答案就在new_inode()函数中。在分配节点过程中会通过alloc_inode()函数调用超级块的函数操作表，我们曾对这个函数操作表sockfsOps进行了介绍，最终是调用sock_alloc_inode()函数完成socket_alloc结构的分配。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ sock_alloc() $\rightarrow$ new_inode() $\rightarrow$ alloc_inode() $\rightarrow$ sock_alloc_inode()   
240 static struct inode \* sock_alloc_inode(struct super_block\* sb)   
241 {   
242 struct socket_alloc \*ei;   
243   
244 ei $=$ kmem_cache_alloc(ock_inode_cachep,GFP_KERNEL); //分配socket_alloc结构   
245 if(!ei)   
246 return NULL;   
247 init_waitqueue_head(&ei->socket.wait); //初始化等待队列头   
248 //初始化socket   
249 ei->socket.fasync_list $=$ NULL;   
250 ei->socket.state $=$ SS_UNCONNECTED;   
251 ei->socket.flags $= 0$ .   
252 ei->socket ops $=$ NULL;   
253 ei->socket.sk $=$ NULL;

254 ei->socket.file $\equiv$ NULL;   
255   
256 return &ei->vfs_inode;   
257

代码244行可从slab高速缓存sock_inode_cache直接进行分配，这个缓存块是在init在内的odecache()函数中建立的，该函数在初始化函数sock_init()中被调用。247行处初始化了socket中的等待队列，注意250行处将socket的状态设为未连接。

```c
272 static int init_inodecache(void)  
273 { //创建socket_alloc结构的slab高速缓存  
274 sock_inode_cachep = kmem_cache_create("sock_inode_cache",  
275 sizeof(struct socket_alloc),  
276 0,  
277 (SLAB_HWCACHE allegiance |  
278 SLAB_RECLAIMumble accounts |  
279 SLAB_MEM_SPREAD),  
280 init_once);  
281 if (socket_inode_cachep == NULL)  
282 return -ENOME;  
283 return 0;  
284 } 
```

上述代码中的mem_cache_created()函数是以socket_alloc结构的长度为单位创建高速缓存。关于mem_cache_alloc()和mem_cache_create()两个slab函数这里就不加叙述了，读者可以参阅推荐书籍中内存管理章节内容。sock_alloc()函数其余代码是关于inode节点设置的，如标记为socket节点并记录下当前进程的信息，最后增加socket的使用计数。

# 2.3 使用协议族的函数表初始化socket

我们回到__sock_create()函数中继续往下分析。创建了socket以后，代码1150行与1152行在net_families[]数组中检查对应family的协议族操作表有没有安装，回忆一下服务器程序创建socket时的代码：

```txt
server_sockfd = socket(AF_INET, SOCK_STREAM, 0); 
```

我们先不关心其他参数，重点注意传递的family参数是AF_INET，这个值定义为2，这里也就是检查数组net_families[2]处是否安装了AF_INET协议族操作表，这当然也是被内核初始化安装完成的。该安装是在net/ipv4/af inet.c中，采用initcall机制对inet_init函数进

行了“安装”操作，从而完成 AF_INET 协议族操作表的注册。

1509 fs_initcall(inet_init);

fs_initcall 是一个宏定义，它的定义在 include/linux/init.h 中。

188 #define fs_initcall(fn) __define_initcall("5",fn,5)

fs_initcall 宏将 inlet_init 安装到了 initcall 中，因此 Linux 在初始化时会执行 inlet_init() 函数。

```txt
1401 static int __init inet_init(void)  
1402 {  
    ...  
    1426 (void)sock_register(&inet_familyOps); //安装协议族操作表  
    ...  
    1507 } 
```

代码1426行转入sock_register()中完成注册。

inet_init() $\rightarrow$ sock_register()
2108 int sock register(const struct net.proto_family * ops)
2109 {
...
2118 spin_lock(&net_family_lock);
2119 if (net_families[ops->family])
2120 err = -EEXIST;
2121 else {
2122 net_families[ops->family] = ops; //将操作表登记到全局数组中
2123 err = 0;
2124 }
...
}

2122行把AF_INET协议族的操作表结构inet_familyOps注册到net_families数组中。这个操作表结构定义在/net/ipv4/af_INET.c中。

927 static struct net.proto_family inlet_familyOps $\equiv$ {   
928 .family $=$ PF_INET,   
929 .create $=$ inlet_create,   
930 .owner $\equiv$ THIS_MODULE,   
931 ）;

928行的family值PF_INET就是AF_INET。

结构体 net.proto_family 的内容比较简单，在此就不列出了。找到了协议族操作表之后，_sock_create 函数 1169 行处的 pf-> create(net, sock, protocol) 语句就得以执行，对照 inlet_familyOps 结构的 create 钩子函数，它挂入的是 inlet_create() 函数，代码较多，因此分段分析。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ _sock_create() $\rightarrow$ inlet_create()   
267 static int inlet_create(struct net \*net, struct socket \*sock, int protocol)   
268 {   
269 struct sock \*sk;   
270 struct list_head \*p;   
271 struct inlet_protosw \*answer;   
272 struct inlet SOCK \*inet;   
273 struct proto \*answerprot;   
274 unsigned char answer_flags;   
275 char answer_no_check;   
276 int try_loadngModule $= 0$ .   
277 int err;   
278 //检查socket类型和加密字符   
279 if (sock->type！ $=$ SOCKRaw &&   
280 sock->type！ $=$ SOCK_DGRAM&&   
281 !inet_ehash_secret)   
282 build_ehash_secret();   
283   
284 sock->state $=$ SS_UNCONNECTED;//设置socket的状态为‘未连接状态’   
285   
286 /\*Look for the requested type/protocol pair.*/   
287 answer $=$ NULL;

前面在__sock_create()函数1140行处将服务器程序设置的类型赋给了新分配的sock，从而将sock类型设置成了SOCK_STREAM，即数据流类型。这里首先检查sock的类型，查看是否SOCKRaw（原始类型)和SOCK_DGRAM(数据报类型，一般用于UDP协议）类型的socket，并且判断是否已经有了加密字符，如果没有就会在282行处调用build ehash_secret()来设置。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ inlet_create() $\rightarrow$ build_ehash_secret()   
233 void build_ehash_secret(void)   
234 {   
235 u32 rnd;

```c
236 do{//获得随机数  
237 get_random_bytes(&rnd,sizeof(rnd));  
238 }while (rnd == 0);  
239 spin_lock bh(&inetsw_lock);  
240 if(!inet_ehash_secret)  
241INET_ehash_secret = rnd; //使用随机数作为加密字符  
242 spin_unlock_bh(&inetsw_lock);  
243} 
```

get_random_bytes()是取得一个随机数，也称作“熵”，该值用于加密使用。接下来inet_create()函数将socket设置为SS_UNCONNECTED未连接状态。函数中的answer是inet_protosw结构变量。

inet_protosw结构体用于IP协议对应socket的接口，也就是靠近socket层的协议信息均保存在这个数据结构中。每一种IP协议都有这么一个接口结构，其定义在/include/net/sock.h中。

# 代码清单2.5 inset_protosw结构的定义

70 struct inlet_protosw{  
71 struct list_head list;  
72  
73 /\*下面两个变量用于校对使用\*/  
74 unsigned short type; $/^{\text{串}}$ 对应于socket的类型\*/  
75 unsigned short protocol; $/^{*}$ IP协议编码\*/  
76  
77 struct proto $\ast$ prot; $/^{\text{串}}$ 对应的协议结构体指针\*/  
78 const struct protoOps $\ast$ ops; $/^{\text{串}}$ 对应协议的函数操作表指针\*/  
79  
80 int capability; $/^{\text{串}}$ 兼容性  
81 $\ast$ 标志着是否能够使用该socket  
82  
83 $\ast /$ 84 char no_check; $/^{\text{串}}$ 是否在接收和发送过程中使用检验和\*/  
85 unsigned char flags; $/^{\text{串}}$ 标志位\*/  
86};

我们继续看inet_create()函数余下的代码。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ inlet_create()   
288 lookup_protocol;   
289 err $=$ -ESOCKTNOSUPPORT;

```c
rcu_read_lock();  
list_for_each_rcu(p, &inetsw[sock->type]) {  
    answer = list_entry(p, struct inet_protosw, list);  
} else {  
    /* 检查协议编码是否与内核已经注册的协议相同 */  
if (protocol == answer->protocol) {  
    if (protocol != IPPROTO_IP)  
        break;  
} else {  
    /* 检查是否属虚拟IP协议*/  
if (IPPROTO_IP == protocol) {  
    protocol = answer->protocol;  
    break;  
}  
if (IPPROTO_IP == answer->protocol)  
break;  
}  
err = -EPROTONOSUPPORT;  
answer = NULL;  
}  
if (unlikely的回答 == NULL)) {  
if (try_loadingsmodule < 2) {  
rcu_read_unlock();  
/*  
* 是否指定了名称？例如net-pf-2-proto-132-type-1  
* (net-pf-PF_INETProto-IPPROTO_SCTP-type-SOCK_STREAM)  
*/  
if (++try_loadingsModule == 1)  
request_module("net-pf-%d-proto-%d-type-%d", PF_INET, protocol, sock->type);  
/*  
* 否则就是通用的名称，例如net-pf-2-proto-132  
* (net-pf-PF_INETProto-IPPROTO_SCTP)  
*/  
else  
requestModule("net-pf-%d-proto-%d", PF_INET, protocol);  
goto lookup_protocol; 
```

329 } else   
330 goto out_rcu_unlock;   
331 }   
332   
333 err $=$ -EPERM;   
334 if (answer->capability $\rightharpoondown$ 0 &&! capable(answer->capability))   
335 goto out_rcu_unlock;   
336   
337 err $=$ -EAFNOSUPPORT;   
338 if(!inet_netns.ok(net,protocol))   
339 goto out_rcu_unlock;

代码段很长，看似复杂其实阅读分析却十分容易。290行是对rcu锁的操作，rcu锁非常适用于读多写少的情况。rcu_read_lock()表示下面的代码是读操作的临界区域，从而保证了多CPU情况（如SMP系统架构)的同步调用代码。与409行相对应的rcu_read_unlock()则表示退出读操作临界区域。291行处的list_for_each_rcu()使用了宏list_for_each_rcu，定义于include/linux/list.h中。

```c
641 #define list_for_each_rcu(pos, head)  
642 for(pos = rcu_dereference((head) -> next);  
643 prefetch(pos -> next), pos != (head);  
644 pos = rcu_dereference(pos -> next))  
144 #define rcu_dereference(p) {(}  
145 typedef(p) ___________p1 = ACCESS_ONCE(p);  
146 SMP_read_barrier Depends();  
147 (______________p1);  
148 }  
67 #define ACCESS_ONCE(x) (*(volatile typeof(x) *))&(x)) 
```

rcu_dereference 表示在 rcu 保护下取得指定处的内容, 由此理解 291 行是循环检查 in-etsw 数组找到符合 socket 类型的队列。这个队列是一个 inlet_protosw 结构, 该结构用于内核支持的 TCP 协议而使用。前面介绍了每一种 TCP 协议都有这么一个结构, 这个队列数组也是在 inlet_init() 函数中注册完成的。

```txt
1401 static int __init inset_init(void)  
1402 {  
1403 struct sk_buffer * dummy_skb;  
1404 struct inet_protosw *q; 
```

1405 struct list_head \*r;   
1406 int rc $=$ -EINVALID;   
1447 for $(\mathbf{q} =$ inetsw_array; $\mathrm{q} <   \&$ inetsw_array[INETSW_ARRAY_LEN]； $+ + q)$ 1448 inlet register protosw(q);   
1507

在该函数中，1447行循环调用inet_register_protosw()函数来处理inetsw_array[]数组中的元素，将它们依次登记到数组inetsw[]中。

代码清单2.6INETSW_array数组的定义  
```txt
936 static struct inlet_protosw inletsw_array[] =  
937 {  
938 {  
939 .type = SOCK_STREAM,  
940 .protocol = IPPROTO_TCP,  
941 .prot = &tcpprot,  
942 .ops = &inet_STREAMOps,  
943 .capability = -1,  
944 .no_check = 0,  
945 .flags = INET_protosw_PERMANENT |  
946 INET_protosw_ICSK,  
947 },  
948  
949 {  
950 .type = SOCK_DGRAM,  
951 .protocol = IPPROTOUDP,  
952 .prot = &udpprot,  
953 .ops = &inet_dgramOps,  
954 .capability = -1,  
955 .no_check = UDP_CSUM_DEFAULT,  
956 .flags = INET_protosw_PERMANENT,  
957 },  
958  
959  
960 {  
961 .type = SOCKRaw,  
962 .protocol = IPPROTO_IP, /* wild card */  
963 .prot = &rawprot, 
```

964 .ops $=$ &inet SOCKraw ops,   
965 .capability $=$ CAP_NET_RAW,   
966 .no_check $=$ UDP_CSUM_DEFAULT,   
967 .flags $=$ INET_PROTOSW_REUSE,   
968 }   
969}；

代码清单2.5已经列出了inet_protosw结构的定义。inetsw_array数组定义了3种类型的结构变量，第一种是用于TCP数据流协议使用的，它使用协议标识码IPPROTO_TCP来表示；第二种是UDP的数据报协议使用的，它使用协议标识码IPPROTOUDP来表示；第三种则是RAW原始套接字使用的，可以是用户自己的协议，许多IP欺骗程序都使用该类型协议，它使用的标识码IPPROTO_IP是“虚拟的IP协议”类型。前边介绍了inet_init()函数在注册登记这个数组时使用函数inet register protosw。

inet_init() $\rightarrow$ inet_register_protosw()
973 voidINETRegister_protosw(structinet_protosw\*p)
974 {
975 struct list_head *lh;
976 structINET_protosw\*answer;
977 intprotocol $=$ p->protocol;
978 structlist_head\*lastperm;
979
980 spin_lock_bh(&inetsw_lock);
981
982 if(p->type >=SOCK_MAX)
983 goto out-illegal;
984
985 /*检查参数P的类型是否超越了内核范围*/
986 answer = NULL;
987 lastperm = &inetsw[p->type];
988 list_for_each(lh,&inetsw[p->type]) {
989 answer = list_entry(lh,structinet_protosw,List);
990
991 /*查找匹配的队列*/
992 if(INET_PROTOSW_PERMANENT&answer->flags) {
993 if(protocol == answer->protocol)
994 break;
995 lastperm = lh;
996 }

997   
998 answer $=$ NULL;   
999 }   
1000 if (answer)   
1001 goto outpermanent;   
1002   
1003 /\*插入到队列中\*/   
1009 list_add_rcu(&p->list,lastperm);   
1010 out:   
1011 spin_unlock bh(&inetsw_lock);   
1012   
1013 synchronize_net();   
1014   
1015 return;   
1016   
1017 outPermanent;   
1018 printk(KERN_ERR"Attempt to override permanent protocol $\% d.\backslash n"$ 1019 protocol);   
1020 goto out;   
1021   
1022 out-illegal:   
1023 printf(KERN_ERR   
1024 "Ignoring attempt to register invalid socket type $\% d.\backslash n"$ 1025 p->type);   
1026 goto out;   
1027

该函数循环在inetsw[]数组中查找适合的队列，通过对比要插入的参数P是否具有INET_PROTOSW_PERMANENT固定标志、是否与队列是同一种协议，如果比对成功，则通过p->list链入这个队列中。inet_init()函数将数组inetsw_array中的元素逐一链入到数组队列inetsw[]中。

回到inet_create()函数中，为了能够理解291行list_for_each_rcu循环查找的结果，我们再次回顾一下服务器程序代码：

```txt
server_fd = socket(AF_INET, SOCK_STREAM, 0); 
```

由此可见，传递到inet_create()函数的参数protocol在服务器程序中指定为0。查找过程中，在数组inetsw[]中根据sock->type，服务器程序指定为SOCK_STREAM即TCP协议类型，找到了协议类型的所在的队列。此后292行处代码“answer = list_entry(p, struct inet

_protosw, list);”使得 answer 指向了 TCP 协议的 inlet_protosw 结构，接着判断结构的协议码是否与参数 protocol 相同；这当然与服务器程序传递下来的 0 不同，所以 protocol 调整为协议码 IPPROTO_TCP，这个值是 6。

311 行检查 answer 是否为空, 如果是空则说明该协议类型的结构还没有安装, 这时需要通过 request_module() 函数来安装了; 但是服务器程序执行到这里却找到了 TCP 协议的结构内容。

{
.type $=$ SOCK_STREAM,
(protocol $=$ IPPROTO_TCP,
.proto $=$ &tcp.proto,
 ops $=$ &inet_STREAMOPS,
capability $= -1$ ,
no_check $= 0$ ,
flags $=$ INET PROTOSW_PERMANENT|INET PROTOSW_ICSK,
},

接着对其兼容性进行了检测，主要是对其answer $\longrightarrow$ capability的对比。找到结构的capability值是-1，所以不会直接返回，我们再看余下的代码。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ inlet_create()

341 sock-> ops = answer-> ops;   
342 answer -, prot = answer -> prot;   
343 answer_no_check = answer-> no_check;   
344 answer_flags = answer-> flags;   
345 rcu_read_unlock();   
346   
347 BUG_TRAP( answer -, prot-> slab != NULL);   
348   
349 err = -ENOUBFS;   
350 sk $=$ sk_alloc(net, PF_INET, GFP_KERNEL, answer -, if (sk == NULL)   
351 if (sk == NULL)   
352 goto out;   
353   
354 err = 0;   
355 sk-> sk_no_check = answer_no_check;   
356 if (INET PROTOSW_REUSE & answer_flags)   
357 sk-> sk_reuse = 1;

```c
inet = inet_sk(sk);
inet-> is_ICsk = (INET PROTOSW_ICSK & answer_flags) != 0;
if (SOCK RAW == sock-> type) {
    inet-> num = protocol;
    if (IPPROTORAW == protocol)
        inet-> hdrincl = 1;
}
if (ipv4_config.no_pmtu_disc)
    inet-> pmtudisc = IP_PMTUDISC_DONT;
else
    inet-> pmtudisc = IP_PMTUDISC_WANT;
inet-> id = 0;
sock_init_data(sock, sk);
sk-> sk-destruct = inet_sock-destruct;
sk-> sk_family = PF_INET;
sk-> sk_protocol = protocol;
sk-> skbackslog_rcv = sk-> sk_prot->backlog_rcv; //设置处理库存函数
inet-> uc ttl = -1;
inet-> mc_loop = 1;
inet-> mc ttl = 1;
inet-> mc_index = 0;
inet-> mc_list = NULL;
sk_refcntDebug_inc(sk);
if (inet-> num) {
/* 这里假设协议允许用户指定 socket的编号，创建时自动共享 */
inet-> sport = htons(inet-> num);
/* Add to protocol hash chains. */
sk-> sk_prot->hash(sk); 
```

399 }   
400   
401 if $(\mathrm{sk - > sk\_prot - > init})$ {   
402 err $=$ sk- $\rightharpoondown$ sk_prot->init(sk);   
403 if (err)   
404 sk_common_release(sk);   
405 }   
406 out:   
407 return err;   
408 out_rcu_unlock;   
409 rcu_read_unlock();   
410 goto out;   
411 1

我们已经介绍了345行的rcu_read_unlock()是对应rcu_read_lock()来释放rcu锁的。代码341与342行比较关键(代码加黑突出了其重要性)，一是对socket的协议操作函数sock->ops进行了挂钩；二是answer->prot赋值给answer_prot，将作为参数传给sk_alloc()函数使用，稍后来看它们的作用。

结合代码清单2.5列出的inet_protosw结构内容，socket的操作函数表sock->ops被设置成了inet_STREAMOPS();而answerprot被设置成了tcpprot结构，是struct proto结构变量。proto结构是紧随着socket的网络协议结构，其内部有很多的函数指针，专用于socket传输层使用；而网络层的结构则使用另一个结构体struct inetproto来表示。

# 2.4 分配并初始化 sock 结构

inet_create()函数第350行处分配了一个sock结构，其定义在代码清单2.2中。分配工作是通过 $\mathrm{sk} = \mathrm{sk\_alloc}(\mathrm{net},\mathrm{PF\_INET},\mathrm{GFP\_KERNEL},\mathrm{answer\_prot})$ 来完成的，它将answerprot作为其prot参数使用。注意，已经设置为tcpprot，即TCP传输层的钩子结构。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ __sock_create() $\rightarrow$ inet_create() $\rightarrow$ sk_alloc()

938 struct sock \*sk_alloc(struct net \*net，int family,gfp_tpriority,   
939 struct proto \*prot)   
940 {   
941 struct sock \*sk;   
942   
943 sk $=$ sk_prot_alloc(prot,priority|__GFP_ZERO,family);   
944 if(sk){

945 sk->sk_family $=$ family;   
946 /\*   
947 \*看一下sock结构的定义将会理解为什么需要sk_prot Creator   
948 \*   
949 \*/   
950 sk->sk_prot $=$ sk->sk_prot Creator $=$ prot;   
951 sock_lock_init(sk);   
952 sock_net_set(sk，get_net(net));   
953 }   
954   
955 return sk;   
956}

950行将传输层的钩子结构tcpprot挂入到了sock->skprot上，下面将会看到对这个钩子结构的调用。

在sk_alloc()函数943行处用了sk_prot_alloc()分配一个通用的sock结构体（见代码清单2.2)，它根据tcpprot结构是否提供了slab高速缓存来确定是在高速缓冲中分配还是在通用缓冲中分配(本书将在后面列出tcpprot结构的内容，详见索引）。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ inet_create() $\rightarrow$ sk_alloc() $\rightarrow$ sk_prot_alloc()

883 static struct sock \*sk PROT_alloc(struct proto \*prot,gfp_t priority,   
884 int family)   
885 {   
886 struct sock \*sk;   
887 struct kmem_cache \*slab;   
888   
889 slab $=$ prot->slab;   
890 if(slab！ $=$ NULL)   
891 sk $=$ kmem_cache_alloc(slab,priority);   
892 else   
893 sk $=$ kmalloc(prot->obj_size,priority);   
894   
895 if(sk != NULL){   
896 if (security_sk_alloc(sk,family,priority))   
897 goto out_free;   
898   
899 if(!tryModule_get(prot->owner))   
900 goto out_free(sec;

```c
901 }   
902   
903 return sk;   
904   
905 out_free(sec;   
906 security_sk_free(sk);   
907 out_free:   
908 if(slab != NULL)   
909 kmem_cache_free(slab,sk);   
910 else   
911 kfree(sk);   
912 return NULL;   
913 } 
```

891 行处使用了内存管理的 slab 分配函数 kmem_cache_alloc(), 它表示在专用的 sock 高速缓冲池中分配结构空间; 而 kmalloc() 函数则是在通用的高速缓冲池中分配结构空间, 这两个函数的具体过程属于内存管理的内容因而不作为重点。

无论是高速缓存还是通用缓存，分配成功后都会在sk_alloc()函数中对其进行初始化操作，而调用sock_lock_init()函数对起同步作用的sk_lock锁进行初始化。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ inet_create() $\rightarrow$ sk_alloc() $\rightarrow$ sock_lock_init()

861 static inline void sock_lock_init(struct sock \*sk)   
862 {   
863 sock_lock_init_class_and_name(sk,   
864 af_family_slock_key_strings[sk->sk_family],   
865 af_family_slock_keys $^+$ sk->sk_family,   
866 af_family_key_strings[sk->sk_family],   
867 af_family_keys $^+$ sk->sk_family);   
868 }

该函数调用宏 sock_lock_init_class_and_name 来完成声明并初始化 sk_lock 的内容：

```c
809 #define sock_lock_init_class_and_name(sk, sname, skey, name, key)  
810 do {  
811 sk->sk_lock-owned = 0;  
812 init_waitqueue_head(&sk->sk_lock.wq);  
813 spin_lock_init(&(sk) -> sk_lock.slock);  
814 debug_check_no_locks_freed((void *)&(sk) -> sk_lock,  
815 sizeof((sk) -> sk_lock)); 
```

```erlang
816 lockdep_set_class_and_name(&(sk) -> sk_lock.slock, \  
817 (skey), (sname));  
818 lockdep_init_map(&(sk) -> sk_lock_dep_map, (name), (key), 0);  
819 } while (0) 
```

sk_lock 是 socket_lock_t 结构变量，它专用于 socket 的锁结构。

```c
83 typedef struct{   
84 spinlock_t slock;   
85 int owned;   
86 wait_queue_head_t wq;   
87 /\*   
88 \* We express the mutex- alike socket_lock semantics   
89 \* to the lock validator by explicitly managing   
90 \* the lock as a lock variant (in addition to   
91 \* the lock itself);   
92 /\*   
93 #ifdef CONFIG_DEBUG_LOCK_alloc   
94 struct lockdep_map dep_map;   
95 #endif   
96 } socket_lock_t; 
```

其定义中包括了一个自旋锁slock，还有一个等待队列头wq。宏sock_lock_init_class_and_name正是对其内容的初始化设置。_sock_create()的参数net指定为current->nsproxy->net_ns,这是在进程数据结构task_struct中记录的网络空间结构,我们将会在后面说明结构体struct net的内容。952行调用sock_net_set()函数记录下所属的net空间结构，get_net()则是增加所属net结构的计数器。

sk_alloc()分配了sock结构并进行了初始化后，回到inet_create()函数351行处检查是否成功分配了sock结构，如果分配失败就不能再往下执行而退出了。注意代码中结构变量sk是指sock结构，sock表示socket结构。

359 行处的INET =INET_sk(sk)是通过sock指针得到structINET SOCK *inet结构指针，这是图2.3所说明的关系。了解unix的读者知道其socket的专用结构是unix SOCK，而这里的专用结构是inet SOCK，我们暂时不关心其结构的内容；函数inet_create()还会调用sock_init_data()对新分配sock结构做进一步的初始化操作，使sock和sk挂起钩来。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ inet_create() $\rightarrow$ sock_init_data()

```c
1690 void sock_init_data(struct socket \*sock，struct sock \*sk)   
1691{ 
```

```c
skb_queue_head_init(&sk->sk_receive_queue);  
skb_queue_head_init(&sk->sk_write_queue);  
skb_queue_head_init(&sk->sk_error_queue);  
#ifdef CONFIG_NET_DMA  
skb_queue_head_init(&sk->sk_async_wait_queue);  
#endif 
```

```c
sk->sk_send_head NULL; 
```

```c
init_timer(&sk->sk_timer); 
```

```txt
sk->sk_allocation = GFP_KERNEL; 
```

```txt
sk-> sk_rcvbuf sysctl_rmem_default; 
```

```sql
sk-> sk_sndbuf sysctl_wmem_default; 
```

```txt
sk->sk_state = TCP_CLOSE; 
```

```txt
sk->sk_SOCKET = sock; 
```

```txt
sock_set_flag(sk, SOCK_ZAPPED); 
```

```txt
if（sock）{ 
```

```txt
sk->sk_type = sock->type; 
```

```html
sk->sk_sleep = &sock->wait; 
```

```txt
sock->sk = sk; 
```

```txt
} else 
```

```txt
sk->sk_sleep 
```

```txt
rwlock_init(&sk->sk.dst_lock); 
```

```txt
rwlock_init(&sk->sk_callback_lock); 
```

```txt
lockdep_set_class_and_name(&sk->sk_callback_lock, 
```

```python
af_callback_keys + sk->sk_family, 
```

```c
af_family_clock_key_strings[sk->sk_family]); 
```

```txt
sk->sk_state_change = sock_def_wakeup; 
```

```c
sk->sk_data_ready = sock_def_readable; 
```

```c
sk->sk_write_space = sock_def_write_space; 
```

```txt
sk->sk_error_report = sock_def_error_report; 
```

```txt
sk->sk-destruct = sock_def-destruct; 
```

```c
1730 sk->sk_sndmsg_page = NULL;  
1731 sk->sk_sndmsg_off = 0;  
1732  
1733 sk->sk_peercred.pid = 0;  
1734 sk->sk_peercred.uid = -1;  
1735 sk->sk_peercred gid = -1;  
1736 sk->sk_write_pending = 0;  
1737 sk->sk_rcvlowat = 1;  
1738 sk->sk_rcvtimeo = MAX_SCHEDULE_TIMEOUT;  
1739 sk->sk_sndtimeo = MAX_SCHEDULE_TIMEOUT;  
1740  
1741 sk->sk_stamp = ktime_set(-1L, 0);  
1742  
1743 atomic_set(&sk->sk_refcnt, 1);  
1744 atomic_set(&sk->skdrops, 0);  
1745} 
```

请读者自己对照sock结构的定义(代码清单2.2)理解初始化过程，注意函数skb_queue_head_init是对sock结构中的几个数据包队列头进行初始化，它们是sk_buffer_head 结构。

# 代码清单2.7 sk_buffer_head 结构的定义

116 struct sk_buffer_head {   
117 $\text{一} ^ { \text{一} }$ These two members must be first. \*/   
118 struct sk_buffer next;   
119 struct sk_buffer prev;   
120   
121 u32 qlen;   
122 spinlock_t lock;   
123 ）；

这个结构内部包含了sk_buffer数据包的指针，sk_buffer结构定义在代码清单2.3，这里的next和prev分别指向队列中前一个和后一个数据包，qlen是队列的长度，而lock则是用于队列操作的锁。sock_init_data()函数初始化的数据包队列主要是sk_receive_queue接收数据包队列头和sk_write_queue发送数据包队列头，函数中其余赋值操作对照sock的定义就很容易理解了，代码中有很多钩子的挂钩过程，在后面的分析过程将遇到这些钩子函数，在需要时注意查看这里的钩子函数。

# 2.5 TCP协议对sock结构初始化

inet_create()对inet SOCK结构也进行了一些初始化设置，其402行处的代码非常关键：

401 if $(\mathrm{sk - > sk\_prot - > init})$ { 402 err $=$ sk- $\rightharpoondown$ sk_prot- $\rightharpoonup$ init(sk);

这里调用了运输层的钩子函数init()，回忆sk_alloc()函数950行处将运输层的钩子结构tcpprot挂入到skprot上，因而需要结合tcpprot的定义才能理清init()的设置。

代码清单2.8 tcp.proto结构的定义  
2406 struct proto tcp_prot $=$ {   
2407 .name $=$ "TCP",   
2408 .owner $=$ THISMODULE,   
2409 .close $=$ tcp_close,   
2410 .connect $=$ tcp_v4_connect,   
2411 .disconnect $=$ tcpdisconnect,   
2412 .accept $=$ inlet_csk_accept,   
2413 .ioct1 $=$ tcp_ioct1,   
2414 .init $=$ tcp_v4_init SOCK,   
2415 .destroy $=$ tcp_v4destroy SOCK,   
2416 .shutdown $=$ tcp_shutdown,   
2417 .setsockopt $=$ tcp_setsockopt,   
2418 .getsockopt $=$ tcp_getsockopt,   
2419 .recvmsg $=$ tcp_recvmsg,   
2420 .backlog_rcv $=$ tcp_v4_do_rcv,   
2421 .hash $=$ inet_hash,   
2422 .unhash $=$ inlet_unhash,   
2423 .get_port $=$ inlet_csk_get_port,   
2424 .enter_memory_pressure $=$ tcp-enter_memory_pressure,   
2425 .sockets_allocated $=$ &tcp_sockets_allocated,   
2426 .orphan_count $=$ &tcp_orphan_count,   
2427 .memory_allocated $=$ &tcp_memory_allocated,   
2428 .memory_pressure $=$ &tcp_memory_pressure,   
2429 .sysctl_mem $=$ sysctl.tcp_mem,   
2430 .sysctl_wmem $=$ sysctl.tcp_wmem,   
2431 .sysctl_rmem $=$ sysctl.tcp_rmem,   
2432 .max_header $=$ MAX.tcpHEADER,   
2433 .obj_size $=$ sizeof(struct tcp SOCK),   
2434 .twsk PROT $=$ &tcp时限wait SOCKOps,   
2435 .rsk PROT $=$ &tcp_request SOCKOps,   
2436 .h hashinfo $=$ &tcp_hashinfo,   
2437 # ifdef CONFIG_COMPAT   
2438 .compat_setsockopt $=$ compat.tcp_setsockopt,

2439 .compat_getsockopt $=$ compat.tcp_getsockopt,   
2440 #endif   
2441};

这里我们就只关心代码清单2.8粗体部分的内容，它显然是将初始化钩子函数init()挂入了tcp_v4_init_sock()。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_create() $\rightarrow$ sock_create() $\rightarrow$ inet_create() $\rightarrow$ tcp_v4_init SOCK()

1826 static int tcp_v4_init SOCK(struct sock \*sk)   
1827 {   
1828 struct inlet_connection SOCK \*icsk $=$ inlet_csk(sk);   
1829 struct tcp SOCK \*tp $=$ tcp_sk(sk);   
1830   
1831 skb_queue_head_init(&tp->out_of_order_queue);   
1832 tcp_init_xmit_timers(sk);   
1833 tcp_prequeue_init(tp);   
1834   
1835 icsk->icsk_rto $=$ TCP_TIMEOUT_INIT;   
1836 tp->mdev $=$ TCP_TIMEOUT_INIT;   
1837   
1838 /\*许多关于延时ACK的TCP代码都要统计SYN数据帧，需要高效的阻塞控制算法   
1842 /\*   
1843 tp->snd_cwnd $= 2$ 1844   
1845 /\*See draft-stevens-tcpca-spec-01 for discussion of the   
1846 /\*initialization of these values.   
1847 /\*   
1848 tp->snd_ssthresh $= 0x7FFFFFF$ /\*Infinity\*/   
1849 tp->snd_cwnd_clamp $= \sim 0$ 1850 tp->mss_cache $= 536$ 1851   
1852 tp->reordering $=$ sysctl.tcp_reordering;   
1853 icsk->icsk_ca ops $=$ &tcp_init_congestionOps;   
1854   
1855 sk->sk_state $=$ TCP_CLOSE;//注意sock状态为关闭   
1856   
1857 sk_write_space $=$ sk_STREAM_write_space;   
1858 sock_set_flag(sk,SOCKUSE_WRITEQueue);

1859   
1860 icsk-> icsk.af ops $\equiv$ &ipv4specific;   
1861 icsk-> icskSync_mss $\equiv$ tcp-sync_mss;   
1862 #ifdef CONFIG_TCP_MD5SIG   
1863 tp-> af_specic $\equiv$ &tcp_sock_ipv4_specfic;   
1864 #endif   
1865   
1866 sk-> sk_sndbuf $\equiv$ sysctl.tcp_wmem[1];   
1867 sk->sk_rcvbuf $\equiv$ sysctl.tcp_rmem[1];   
1868   
1869 atomic_inc(&tcp_sockets_allocated);   
1870   
1871 return 0;   
1872 }

函数声明了结构 tcp_sock 变量 tp, 其结构体定义在代码清单 2.4 中已经列出了。上面代码对这个结构变量 tp 和新分配的 sock 结构做了进一步的初始化, 后续分析过程需要回顾这里的初始化内容, 如这里的 sk->sk_write_space = sk_STREAM_write_space 等钩子函数的挂入以及缓冲区的相关设置。至于代码中出现的 inet_connection_sock 结构定义放在以后的章节中列出, 它主要是 inet_sock 结构体的一个扩展, 涉及连接操作的具体信息。tcp_v4_init_sock() 函数对这些结构的初始化设置将关联以后的很多操作, 我们随着章节的过渡逐一解释。函数最后递增 tcp_sockets_allocated, 这是 TCP 的一个全局计数器。

图2.2是创建过程中涉及的数据结构关系。可以看出，从sock结构出发可以得到inet SOCK结构指针，进一步可以得到inet_connection SOCK结构指针和tcp SOCK的指针。这是因为前一个结构体在后一个结构体的最前端，即sock起始地址也是其他结构的起始地址（虚线部分），并且图中也画出了关于接收队列和发送队列与数据包sk_buffer的关系，后面会讲解这些结构体是如何一起分配空间的。

# 2.6 socket 与文件系统的关联

完成了创建并初始化 socket 以后，最后回到 sys_SOCKET()函数中执行 retval = sock_map_fd(sock) 来完成“收尾工作”。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_map_fd()   
398 int sock_map_fd(struct socket \*sock)   
399 {   
400 struct file \*newfile;

![](images/264920287403a6a26e83f09d7d43ce3d38059a89a88917acf3127e75ee837ff2.jpg)  
图2.2 tcp_sock等结构的关联图

```c
401 int fd = sock_alloc_fd(&newfile); //为 socket 分配文件号和文件结构  
402  
403 if (likely(fd >= 0)) {  
404 int err = sock.attach_fd(soap, newfile);  
405  
406 if (unlikely(err < 0)) { //操作过程出现错误则释放文件和文件号  
407 put_filp(newfile);  
408 put_unused_fd(fd);  
409 return err;  
410 }  
411 fd_install(fd, newfile); //使文件号与文件挂钩  
412 }  
413 return fd;  
414 }
```

它主要是调用了一个与文件系统有关的操作函数 sock Attached_fd()。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_map_fd() $\rightarrow$ sock_alloc_fd() 351 static int sock_alloc_fd(struct file \* \* filep)   
352 {   
353 int fd;

```c
fd = get_unused_fd(); //申请文件号
if (likely(fd >= 0)) {
struct file *file = get_empty_filp(); //分配文件结构空间
* filep = file;
if (unlikely(! file)) {
put_unused_fd(fd);
return -ENFILE;
}
} else
* filep = NULL;
return fd;
} 
```

调用文件系统的 get_empty_filp()函数分配了一个文件指针结构，并使用 get_unused_fd()申请一个文件号。如果分配和申请成功，则在 404 行处进入 sock Attached_fd()函数。

sys_SOCKETcall() $\rightarrow$ sys_SOCKET() $\rightarrow$ sock_map_fd() $\rightarrow$ sock Attached_fd()   
369 static int sock Attached_fd(struct socket \* sock, struct file \*file)   
370 {   
371 struct dentry \*dentry;   
372 struct qstr name $=$ {.name $=$ ""};   
373

//创建一个socket文件系统的目录项

374 dentry $=$ d_alloc(ock_mnt->mnt_sb->s_root,&name);   
375 if (unlikely(!dentry))   
376 return-ENOMEM;   
377

//目录项的操作表挂入了socket文件系统的目录操作表

```txt
378 dentry->d_op = &sockfs_dentry_operations; 
```

/* 因为不想把 socket 目录项放入全局的目录项 hash 数组中,因此清除其 DCache_UNHASSED 标志。假装目录项已经 hash 过了,允许通过 proc 文件系统目录/proc/$ pid/fd/XXX 访问 socket */

384 dentry->d_flags $\& =$ ~DCACHE_UNHASSED;   
385 d_instantiate(dentry，SOCK_INODE(sock));   
386   
387 sock->file $=$ file;

```txt
//对socket的文件结构进行初始化，注意传递的文件操作表是socket_fileOps  
388 init_file(file, sock_mnt, dentry, FMODE_READ | FMODE_WRITE, &socket_fileOps);
```

```c
390 SOCK_INODE sock) -> i_fop = &socket_fileOps;  
391 file->f_flags = 0_RDWR;  
392 file->f_pos = 0;  
393 file->private_data = sock; //可以在文件系统中通过这里 private_data 找到 socket  
394  
395 return 0;  
396}
```

在这个函数393行处将socket与文件指针建立了关联。注意，378行目录项的操作表是sockfs_dentry_operations。

```txt
329 static struct dentry_operations sockfs_dentry_operations = {  
330 .d_delete = sockfs_delete_dentry,  
331 .d_dname = sockfs_dname,  
332 }; 
```

其主要操作函数是删除目录项。388行的文件操作表挂入了socket_file OPS。

# 代码清单2.9 socket_fileOps结构的定义

124 static const struct file_operations socket_fileOps $\equiv$ {   
125 .owner $=$ THIS_MODULE,   
126 .llseek $=$ no_11seek,   
127 .aio_read $=$ sock_aio_read,   
128 .aio_write $=$ sock_aio_write,   
129 .poll $=$ sock_poll,   
130 .unlocked_ioctl1 $=$ sock_ioctl1,   
131 #ifdef CONFIG_COMPAT   
132 .compat_ioctl1 $=$ compat_sock_ioctl1,   
133 #endif   
134 .mmap $=$ sock_mmap,   
135 .open $=$ sock_no_open, /\* special open code to disallow open via /   
proc \*/   
136 .release $=$ sock_close,   
137 .fasync $=$ sock_fasync,   
138 .sendpage $=$ sock_sendpage,   
139 .splice_write $=$ generic_splice_sendpage,   
140 .splice_read $=$ sock_splice_read,   
141 };

这个函数表结构提供了 socket 的操作桥梁, 以后可以使用 read() 和 write() 像操作文件那样对 socket 进行读/写操作。代码最后将 file 结构的 private_data 指向了服务器 socket, 后面我们还会看到服务器程序通过 file->private_data 重新取得这个 socket 指针。另外, 从系统调用的角度来看, 也可以继续通过 sys_SOCKETcall() 对 socket 进行 send() 和 recv() 操作。由此看出, socket 是与文件系统紧密相关的, 这也说明了内核的整体性。很多从事内核网络开发和嵌入式开发的人员往往误以为只要掌握内核的某一部分就可以掌握 Linux 全局, 可是随着日积月累的深入学习, 还是发现单凭某一方面的知识远远不足, 总会陷入各种问题中难以找寻出路, 这也是学习内核的困难之处; 解决的最有效方法是不断提高自己阅读代码的能力, 多读内核源码而不要沉溺于理论书籍之中。

# socket 地址设置

# 3.1 地址设置接口

按照图1.1第一路线，服务器程序创建了socket以后接着要绑定地址，为此分别声明了服务器与客户端的地址结构变量。

回顾服务器程序代码(代码清单1.1)，它调用bind()库函数绑定地址给socket。

```txt
bind(server_fd, (struct sockaddr *)&server_address, server_len); 
```

读者可以在 glibc $\rightarrow 2.3.6$ /sysdeps/generic/bind.c 文件中找到 bind() 的定义, 并且可以在 glibc - 2.3.6 /sysdeps/unix/sysv/linux/bind.S 中看到它的汇编入口, 可以参考第 15 章的 send() 函数内容分析一下从 bind() 到库函数再到系统调用的经过。

这个函数最终会像1.3节描述的那样进入sys_SOCKETcall()函数，到达2031行的switch语句段。

```c
2031 case SYS_BIND:  
2032 err = sys_bind(a0, (struct sockaddr __user * )a1, a[2]);  
2033 break; 
```

在2032行调用sys_bind()函数执行绑定任务，参数也是通过寄存器进行的，这个过程与前面讲述的socket()参数传递过程一致。

sys_SOCKETcall() $\rightarrow$ sys_bind()   
1341 asmlinkage long sys_bind (int fd, struct sockaddr __user \* umyaddr, int addrlen)   
1342 {   
1343 struct socket \* sock;   
1344 char address[MAX SOCK_ADDR];   
1345 int err，fput_needed;   
1346

```txt
//找到已经创建的socket  
1347 sock = sockfd.lookup_light(fd, &err, &fput_needed); 
```

```txt
1348 if（sock）{ 
```

//复制地址到内核空间

```c
1349 err = move_addr_to_kernel(umyaddr, addrlen, address);  
1350 if (err >= 0) {  
1351 err = security_SOCKET_bind(soap, struct sockaddr *)address, addrlen);  
1352 
```

//调用具体协议的绑定函数

```c
1355 err = sock-> ops-> bind(sock,  
1356 (struct sockaddr*)  
1357 address, addrlen);  
1358 }  
1359 fput_light(sock-> file, fput_needed);  
1360 }  
1361 return err;  
1362 } 
```

函数1347行找到服务器已经创建的socket，这是通过sockfd.lookup_light()函数来完成的，我们看一下这个函数的内容：

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ sockfd.lookup_light()

455 static struct socket \* sockfd 查 lookup light (int fd, int \* err, int \* fput_needed)   
456 {   
457 struct file \* file;   
458 struct socket \* sock;   
459   
460 \*err $=$ -EBADF;   
461 file $=$ fget_light(fd,fput_needed);//通过文件号找到文件指针   
462 if (file){   
463 sock $=$ sock_from_file(file, err); //在文件指针中获得 socket 指针   
464 if (sock)   
465 return sock;   
466 fput_light(file,\*fput_needed);   
467 }   
468 return NULL;   
469 }

参数fd实际上是从服务器程序中传递过来的server_sockfd，是从系统调用传递过来的。server_sockfd是服务器创建socket时的文件号，所以这里调用fget_light()从当前进程的

files_struct结构中找到网络文件系统中的file文件指针，并增加它的使用计数；由fget_light()函数查找file指针，这个函数与文件系统内容相关。因为这里的重点是socket的过程，对文件系统的内容不做介绍，我们看到463行处接着调用sock_from_file()函数，从名称上即可看出它是根据file找到服务器创建的socket结构指针。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ sockfd.lookup_light() $\rightarrow$ sock_from_file()   
416 static struct socket \*sock_from_file(struct file \*file,int\*err)   
417 {   
418 if (file->f_op == &socket_fileOps) /\*得到socket指针，对应sock.attach_fd（）函数\*/   
419 return file-> private_data;   
420   
421 \*err $=$ -ENOTSOCK;   
422 return NULL;   
423

在上一章 sock Attached_fd()函数中曾强调了393行代码的重要性，它将file结构的private_data指向了服务器的socket。

```txt
file->private_data = sock; 
```

这里服务器要对 socket 进行地址绑定。首先要通过 private_data 指针重新获取已经创建的 socket 指针, 这样 sockfd.lookup_light() 函数的代码就容易理解了。寻找到 file 就是关键, 而 file 结构中有指针指向服务器的 socket。请读者注意结构变量 sock 是指 socket 结构, 而不是 sock 结构, 内核中经常使用 sk 来表示 sock 结构变量。

回到sys_bind()函数中，1349行还需要调用move_addr_to_kernel()函数将地址复制到内核中。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ move_addr_to_kernel()   
182 int move_addr_to_kernel(void __user \*uaddr，int ulen，void \*kaddr)   
183 {   
184 if (ulen $<  0\mid \mid$ ulen $>$ MAX SOCK_ADDR)   
185 return -EINVALID;   
186 if (ulen $= = 0$ ）   
187 return 0;   
188 if (copy_from_user(kaddr，uaddr，ulen))   
189 return -EFAULT;   
190 return audit_sockaddr(ulen，kaddr);   
191 }

188行的copy_from_user()函数就是从用户空间复制数据到内核空间。参数kaddr是kernel address的缩写；uaddr是user address；而ulen是user length的缩写；copy_from_user()函数的作用很直接，它将服务器的socket地址变量server_address复制到内核空间中。如果读者对内核空间和用户空间的概念模糊，请参考推荐书籍中的说明。

# 3.2 地址结构定义

在服务器代码清单 1.1 中先通过 (struct sockaddr *) & .server_address 将地址转换成 socket 的地址结构, 也就是说, 提供的地址被转换成了 sockaddr 结构指针, server_address 在服务器程序中是 struct sockaddr_in 结构类型。

# 代码清单3.1 sockaddr_in结构定义

```c
32 typedef unsigned short sa_family_t;   
179 /\*用于表示IP的socket地址\*/   
180 #define SOCK_SIZE_ 16 /\* sizeof(struct sockaddr) \*/   
181 struct sockaddr_in{   
182 sa_family_t sin_family; /\*地址族 \*/   
183 be16 sin_port; /\*端口号 \*/   
184 struct in_addr sin_addr; /\*IP地址 \*/   
185   
186 /\*用于补齐struct sockaddr的长度\*/   
187 unsigned char __pad[SOCKSIZE__ - sizeof(short int)-   
188 sizeof(unsigned short int)- sizeof(struct in_addr)];   
189}; 
```

将它对比一下内核公用的sockaddr结构可以看出，sockaddr的定义比较简短，这是为了保留对老版本的支持，传递进内核之后，还会将它还原为sockaddr_in结构。

```c
38 struct sockaddr{  
39 sa_family_t sa_family; /*地址族，AF_xxx */  
40 char sa_data[14]; /*14字节的协议地址 */  
41 }; 
```

在函数 sys_bind()中先将服务器的地址结构变量 server_address 通过 move_addr_to_kernel() 复制到局部数组 char address[ MAX_SOCKET_ADDR] 中, MAX_SOCKET_ADDR 定义为 128, 完全可以将服务器的地址装进数组中。

回忆一下服务器程序对地址的设置过程：

server_address.sin_family = AF_INET; //使用AF_INET地址族

server_address.sin_addr.s_addr = inet_addr("192.168.1.1"); //设置IP地址

server_address.sin_port = htons(9266); //设置端口号

对照 sockaddr_in 结构我们发现并没有对 __pad 数组的赋值操作, 这个数组起填充作用, 它保障结构与内核的 sockaddr 结构长度保持对齐。服务器的地址复制到数组 address 后, 接着看到 sys_bind() 函数执行具体协议的绑定函数。

```c
1355 err = sock-> ops-> bind(ock, 1356 (struct sockaddr*) 1357 address, addrlen); 
```

回忆一下inet_create()函数341行对ops钩子结构的操作，那里将socket的ops通过answer结构变量挂入了inet_STREAMOPS。

```txt
341 sock-> ops = answer-> ops; 
```

因此，1355行转入这个结构的bind()去执行，我们先看一下这个结构的定义：

# 代码清单3.2inet_STREAMOps结构定义

```txt
847 const struct protoOps inlet_STREAMOps = {  
848 .family = PF_INET,  
849 .owner = THISMODULE,  
850 .release = inlet_release,  
851 .bind = inlet_bind, //绑定地址函数  
852 .connect = inlet_STREAM_connect,  
853 .socketpair = sock_no_SOCKETpair,  
854 .accept = inlet_accept,  
855 .getname = inlet_getname,  
856 .poll = tcp_poll,  
857 .ioct1 = inlet_ioct1,  
858 .listen = inlet听听,  
859 .shutdown = inlet_shutdown,  
860 .setsockopt = sock_common_setsockopt,  
861 .getsockopt = sock_common_getsockopt,  
862 .sendmsg = tcp_sendmsg,  
863 .recvmsg = sock_common_recvmsg,  
864 .mmap = sock_no_mmap,  
865 .sendpage = tcp_sendpage,  
866 .splice_read = tcp_splice_read,  
867 # ifdef CONFIG_COMPAT  
868 .compat_setsockopt = compat_sock_common_setsockopt,  
869 .compat_getsockopt = compat_sock_common_getsockopt, 
```

```txt
870 #endif 871 
```

这个结构会在本书中经常用到，这里集中注意bind()挂入的钩子函数，也就是inet_bind()。

这个函数在/net/ipv4/Af_inet.c中的449行处。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind()   
449 intinet_bind(struct socket \* sock，struct sockaddr \*uaddr，int addr_len)   
450 {   
451 struct sockaddr_in \*addr $=$ (struct sockaddr_in \*)uaddr;   
452 struct sock \*sk $=$ sock->sk;   
453 structINET SOCK \*inet $=$ inlet_sk(sk);   
454 unsigned short snum;   
455 intchk_addr_ret;   
456 int err;   
457   
458 /\*如果socket提供了自己的绑定函数就使用它（一般用于原始套接字）\*/   
459 if $(\mathrm{sk - >}$ skprot->bind){   
460 err $=$ sk->skprot->bind(sk，uaddr，addr_len);   
461 goto out;   
462 }   
463 err $=$ -EINVALID;   
464 if(addr_len $<  <$ sizeof(struct sockaddr_in))   
465 goto out;   
466 //在路由中检查地址类型

467 chk_addr_ret = inet_addr_type(ock_net(sk), addr->sin_addr.s_addr);   
468   
469 /\* Not specified by any standard per - se, however it breaks too   
470 \* many applications when removed. It is unfortunate since   
471 \* allowing applications to make a non - local bind solves   
472 \* several problems with systems using dynamic addressing.   
473 \* (ie. your servers still start up even if your ISDN link   
474 \* is temporarily down)   
475 \*/   
476 err $=$ -EADDRNOTAVAIL;   
477 if(!sysctl_ip_nonlocal_bind &&   
478 !inet-> freebind &&   
479 addr->sin_addr.s_addr! $=$ htons(INADDR_ANY)&&   
480 chk_addr_ret != RTN_LOCAL&&//是否单播类型

```c
chk_addr_ret! = RTNMULTICAST&&//是否组播类型
chk_addr_ret! = RTN_BROADCAST)//是否广播类型
goto out;
snum = ntohs(addr->sin_port);//取得端口号
err = -EACCES;
if (snum && snum < PROT_SOCKET && ! capable(CAP_NET_BIND_SERVICE))
goto out;
\/
\*\* 使用了两个地址rcv_saddr用于哈希查找而saddr用于发送
\*\*
\*\* In the BSD API these are the same except where it
\*\* would be illegal to use them (multicast/broadcast) in
\*\* which case the sending device address is used.
\*/ 
```

lock_sock(sk); //加锁,如果 sock 锁被其他进程占用了,当前进程睡眠等待唤醒

/\* Check these errors (active socket, double bind). \*/err $=$ -EINVALID;if (sk-> sk_state！ $= \mathrm{TCP}$ CLOSE || inlet-> num)//检查状态，端口是否已指定goto out_release SOCK;inet->rcv_saddr $=$ inlet->saddr $=$ addr->sin_addr.s_addr;//记录绑定的地址if(chk_addr_ret $= =$ RTN MultTICAST||chk_addr_ret $= =$ RTN_BROADCAST)inet->saddr $= 0$ /\*Use device\*/

/\*检查是否允许绑定\*/ if（sk->sk_prot->get_port(sk，snum））{ inlet->saddr $=$ inlet->rcv_saddr $= 0$ //检验失败就清空设置的地址 err $\equiv$ -EADDRINUSE; goto out_release SOCK;

if (inet->rcv_saddr)//如果已经设置地址就增加锁标志，表示已经绑定了地址sk->sk_userlocks $\mid =$ SOCK_BINDADDR_LOCK;

if (snum)//如果端口也已经确定也要增加锁标志，表示已经绑定了端口sk->sk_userlocks $\mid =$ SOCK_BINDPORT_LOCK;  
inet->sport $=$ htons(inet->num);//记录端口  
inet->daddr $= 0$ //初始化目标地址

521 inlet->dport $= 0$ //初始化目标端口  
522 skDst_reset(sk);//初始化缓存的路由内容  
523 err $= 0$ 524 out_release SOCK;  
525 release SOCK(sk); //解锁，并唤醒 sock锁上的其他进程  
526 out;  
527 return err;  
528}

这里先将 sockaddr 结构还原成结构 sockaddr_in, 它已经在代码清单 3.1 中看过了。453 行通过 sock 结构指针得到 INET 协议族的专用结构 inlet_sock 指针, 检查是否提供了 bind 钩子函数, 一般原始套接字提供了该函数。2.3 节的 inlet_create() 函数中, $\mathrm{sk} \rightarrow \mathrm{sk\_prot}$ 挂入 tcp PROT, 但是我们看一下这个结构变量, 并没有看到声明的 bind 钩子函数。

先在464行检测一下地址长度是否正确，再进入inet_addr_type()函数检查地址的类型；在调用该函数时传递的第一个参数是net结构类型，它调用了sock_net()来传递net指针。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind() $\rightarrow$ sock_net()

```c
1290 static inline  
1291 struct net \* sock_net(const struct sock \*sk)  
1292 {  
1293 #ifdef CONFIG_NET_NS  
1294 return sk-> sk_net;  
1295 #else  
1296 return &init_net;  
1297 #endif  
1298 } 
```

struct net 结构非常大, 用于所命名的网络, 这里不列出其结构的定义了。sock_net 函数根据 sk_net 指针取得内核所使用的网络空间结构, 内核中 CONFIG_NET_NS 配置选项是为了让用户定义自己的网络空间结构, 即 net 结构, 但在一般情况下内核不会配置该项, 因此 sock_net() 函数最终返回了系统默认的 init_net 指针。

# 3.3 地址类型

inet_bind()函数467行调用了查看地址类型函数__inet_dev_addr_type(),我们通过对这个函数的分析掌握一下地址类型。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind() $\rightarrow$ inet_addr_type()

186 const struct net_device \* dev,   
187 be32 addr)   
188{   
189 struct flowi f1 $=$ {.nl_u $=$ {.ip4_u $=$ {.daddr $=$ addr}）}；   
190 struct fib_result res;   
191 unsigned ret $=$ RTN_BROADCAST;   
192 struct fib_table \*local_table;   
193

//检查地址是否是零地址或者广播地址

```c
194 if (ipv4_is_zeronet(addr) || ipv4_is_lbcast(addr))  
195 return RTN_BROADCAST;
```

//检查地址是否是组播地址

```c
196 if (ipv4_isMULTicast(addr))  
197 return RTN Multicast;  
198  
199 #ifdef CONFIG_IP Multiple_TABLES  
200 res.r = NULL;  
201 #endif  
202  
203 local_table = fib_get_table(net, RT_TABLE_LOCAL); //查找本地路由函数表  
204 if (local_table) {  
205 ret = RTN_UNICAST;  
206 if (!local_table->tb.lookup(local_table, &fl, &res)) {  
207 if (!dev || dev == res-fi->fib_dev)  
208 ret = res.type;  
209 fib_res_put(&res);  
210 }  
211 }  
212 return ret;  
213 }  
215 unsigned int inet_addr_type(struct net *net, __be32 addr)  
216 {  
217 return __inet_dev_addr_type(net, NULL, addr);  
218 } 
```

函数代码 $189\sim 192$ 行出现几个数据结构，首先是struct flowi结构用于路由键值。这个结构内部的nl_u是一个联合，它包含ip4_u、ip6_u以及dn_u这3个结构体。189行是将服务器的“电话号码”inet_addr("192.168.1.1")赋值给路由键值fl。

struct fib_result 用于路由查询结果，而 struct fib_table 则是路由函数表的结构体（因它

内部封装了大量的函数指针）。函数中首先检查设置的地址 addr 是否是本地的广播地址和组播地址。既然已经将 192.168.1.1 初始化设置到了路由键值 fl 中。接下来还要对这个“电话号码”进行检查，首先是调用 ipv4_is_zeronet()。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_addr_type() $\rightarrow$ __inet_dev_addr_type() $\rightarrow$ ipv4_is_zeronet() static inline bool ipv4_is_zeronet(_be32 addr) { return(addr&htonl(0xff000000)) $= =$ htonl(0x00000000); }

宏htonl根据cpu是小端结尾还是大端结尾将数值转换成要求的字节序，它实际上是调用__cpu_to_be32()来转换的，ipv4_is_zeronet()函数就是检测IP地址高8位为0、确定addr是否为零网地址。ipv4_is_lbcast()也用来检查地址。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_addr_type() $\rightarrow$ __inet_dev_addr_type() $\rightarrow$ ipv4_is_lbcast()

```c
define INADDR_BROADCAST ((unsigned long int) 0xFFFFFF)  
static inline bool ipv4_is_lbcast(_be32 addr)  
{  
/* limited broadcast */  
return addr == htons(INADDR_BROADCAST); 
```

这个函数检查地址 addr 是否是广播地址类型，196 行调用了第三个检查函数 ipv4_isMULTicast()。sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind() $\rightarrow$ inet_addr_type() $\rightarrow$ __inet_dev_addr_type() $\rightarrow$ ipv4_isMULTicast()

```solidity
static inline bool ipv4_isMULTicast(_be32 addr)  
{  
return(addr&htonl(0xf0000000)) == htonl(0xe0000000);  
} 
```

它检查地址 addr 是否是多播地址类型。

我们了解一下广播、组播(多播)和单播地址的概念：

从一台计算机发送数据到网络中所有的计算机过程称作广播；  
从一台计算机发送数据到网络中一组特定的计算机的过程就是组播，也称作多播。  
从一台计算机发送数据到另外一台计算机的过程称作单播。

如果服务器的地址是零地址、广播地址类型和多播地址类型，则直接返回该类型号 RTN_BROADCAST 或者 RTNMULTICAST；注意，零地址也作为广播类型返回。

但是假若服务器地址不是这些地址类型，则就要在203行通过fib_get_table()查找具体的路由函数表。该函数在Linux内核中存在两个同名函数，分别在/include/net/ip_fib.h中和/net/ipv4/fib_frontend.c中，这取决于内核是否配置了CONFIG_IP_multiple_TABLES支持多个路由函数表选项。

为了便于分析该函数，这里概括一下路由的作用：它提供了到达目标地址的捷径，一般是经过一系列的计算得出的，表示源地址到目标地址的路径，位于第三层即IP层，在Linux中的位置极其重要。路由函数表结构提供了计算路由的方法函数，初次接触路由的读者只需有一个感性的认识，就可以在分析fib_get_table()函数的过程中体会与掌握路由的本质，fib_get_table()函数在ip_fib.h中。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_addr_type() $\rightarrow$ __inet_dev_addr_type() $\rightarrow$ fib_get_table()

```c
static inline struct fib_table *fib_get_table(struct net *net, u32 id)  
{  
struct hlist_head *ptr;  
ptr = id == RT_TABLE_LOCAL ?  
&net->ipv4.fib_table_hash[TABLE_LOCAL_INDEX] :  
&net->ipv4.fib_table_hash[TABLE_MAIN_INDEX];  
return hlist_entry(ptr->first, struct fib_table, tb_hlist);  
} 
```

到达这个函数时参数 net 是内核默认的全局网络空间结构 init_net，另一个参数 id 是 RT_TABLE_LOCAL 表示本地路由函数表。注意代码中的 net->ipv4，它是一个 nets_nsipv4 结构，装载着 IPV4 协议在网络空间中的信息。IPV4 所有的路由函数表都会链入到 nets_nsipv4 结构中的 fib_table_hash 数组，数组的每个元素代表一个队列，因此是队列数组。每个路由函数表都通过其内部结构的 tb_hlist 头链入对应的队列。

内核有一个专门用来表示路由表的结构 rtable，我们将在后续章节列出它的内容。

如果在__inet_dev_addr_type()函数204行找到了本地路由函数表，那么就调用本地路由函数表的tb lookup()函数，从而根据fl键值返回struct fib_result结构，这个结构中的type记录着地址类型。关于本地路由函数表的初始化分析及调用路由函数中的tb.lookup()过程详见第4章路由内容。

__inet_dev_addr_type()函数206行的代码将从查找结构中取得地址类型。

if(!local_table->tb.lookup(local_table,&fl,&res)){ if(!dev||dev $= =$ res.fi->fib_dev) ret $=$ res.type; fib_res_put(&res); }

这里用到了查找结果结构 res，即 fib_result 结构，它包含着路由表信息结构指针，内部记录着网络设备结构信息，代码检查是否与传递下来的 net_device 是同一个网络设备结构。在上一级调用函数 inset_addr_type() 中看到，传递下来的这个 net_device 结构变量指针是空的，代码会设置返回值 ret。我们将会在第 4 章的路由初始化 fib_semantic_MATCH() 函数中，看到路由别名结构 fa_type 赋值给 res。

```txt
res->type = fa->fa_type; 
```

所以这里的地址类型取自于路由别名fa_type记录的路由类型值。

将返回值 ret 送回到 inlet_bind()函数 467 行, 此时CHK_addr_ret 记录下 ret 路由类型值, 接着是判断系统是否允许绑定非本机地址, inlet->freebind 标志是否允许自由绑定, 判断是否零地址, 依据检查的地址类型 CHK_addr_ret, 判断地址是否是单播路由、组播路由、广播路由类型。

# 3.4 设置地址和端口

inet_bind()函数485行是对端口变量snum的取值。

```txt
snum = ntohs(addr->sin_port); 
```

直接取得地址的端口号，它是在服务器程序代码中设置的9266，检查它是否小于1024（系统保留了 $0\sim 1023$ 端口）以及是否有绑定权限。然后对sock结构加锁，这个加锁过程有可能睡眠，只能等待其他进程唤醒。

501行检查sock的状态是否处于关闭或者已经绑定端口。504行的代码是一个连贯赋值代码。

```c
inet->rcv_saddr =INET->saddr = addr->sin_addr.s_addr; 
```

这句代码同时把设定的ip地址192.168.1.1记录在inet的接收地址和源地址中。

505行检查地址类型是组播还是，如果是其中之一，则将源地址设置为0，而接收地址不变。509行则是对端口号的检验和取值。

```c
if (sk->sk_prot->get_port(sk, snum)) {  
    inset->saddr = inlet->rcv_saddr = 0;  
    err = -EADDRINUSE;  
    goto out_release SOCK; 
```

我们在上一章曾经看到了结构变量 tcpprot 的内容，现在需要结合它的 get_port 钩子函数分析。

```python
.get_port = inlet_csk_get_port 
```

509行的sk->sk_prot->get_port就转入inet_csk_get_port函数()去执行。这个函数也比较长，因此分段来看。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_csk_get_port()   
88 int inlet_csk_get_port(struct sock \*sk, unsigned short snum)   
89 {   
90 struct inlet_hashinfo \*hashinfo $=$ sk->skprot->h HASHinfo;   
91 struct inlet_bind_hashbucket \*head;   
92 struct hlist_node \*node;   
93 struct inlet_bind_buckets \*tb;   
94 int ret;   
95 struct net \*net $=$ sock_net(sk);   
96   
97 local_bh_disable();   
98 if(!snum）{//如果端口号没有指定   
99 int remaining, rover, low, high;   
100   
101 inlet_get_local_port_range(&low,&high);   
102 remaining $=$ (high - low) + 1;   
103 rover $=$ net_random() $\%$ remaining $^+$ low;   
104   
105 do{//在内核中查找一个端口号   
106 head $=$ &hashinfo->bhash[inet_bhashfn( rover, hashinfo->bhash_size)];   
107 spin_lock(&head->lock);   
108 inlet_bind_buckets_for_each(tb,node,&head->chain)   
109 if(tb->ib_net $= =$ net && tb->port $= =$ rover)   
110 goto next;   
111 break;   
112 next:   
113 spin_unlock(&head->lock);   
114 if（++ rover>high)   
115 rover $=$ low;   
116 }while（--remaining>0);   
117   
118 /\* Exhausted local port range during search? It is not

119 \* possible for us to be holding one of the bind hash   
120 \* locks if this test triggers, because if 'remaining   
121 \* drops to zero, we broke out of the do/while loop at   
122 \* the top level, not from the'break; statement.   
123 \*/   
124 ret $= 1$ .   
125 if (remaining $<   = 0$ 1   
126 goto fail;   
127   
128 /\* OK, here is the one we will use. HEAD is   
129 \* non - NULL and we hold its mutex.   
130 \*/   
131 snum $=$ rover;//记录下找到的端口号

代码中首先从 tcp.proto 结构中取得 inet_hashinfo 结构指针：

.h Hashinfo $\equiv$ &tcp_hashinfo,

对照它的.h Hashinfo 内容取得了 tcp_hashinfo 指针, 它是 struct inet_hashinfo 结构, 用来封装各种协议的绑定哈希表。

# 代码清单3.3 inset_hashinfo结构体的定义

Struct inset_hashinfo{ /\*This is for sockets with full identity only. sockets here will \* always be without wildcards and will have the following invariant: \* TCP_ESTABLISHED $<  \mathrm{sk - >}$ sk_state $<  \mathrm{TCP\_CLOSE}$ \* TIME_WAIT sockets use a separate chain (twchain). \*/ //已经连接的sock结构都链入该哈希桶，它有两个队列，第一个是连接的sock队列 //另一个为定时等待的sock队列 structINET_ehash_buckets\*ehash;//已经建立连接的哈希桶 rwlock_t \*ehash_locks;//队列锁 unsigned int ehash_size;//队列长度 unsigned int ehash_locks_mask;//锁掩码 / \*Ok,lets try this,I give up,we do need a local binding \*TCP hash as well as the others for fast bind/connect. \*/ structinet_bind_hashbucket \*bhash;//管理端口号的哈希桶

```c
unsigned int bhash_size; //哈希桶的长度
/* Note: 4 bytes padding on 64 bit arches */
/* All sockets in TCP_LISTEN state will be in here. This is the only
* table where wildcardd TCP sockets can exist. Hash function here
* is just local port number wumingxiaozu.
*/
struct hlist_head) listening_hash[INET_LHTABLE_SIZE]; //监听哈希队列
/* All the above members are written once at bootup and
* never written again_or_ are predominantly read-access.
*/
* Now align to a new cache line as all the following members
* are often dirty.
*/
rwlock_t lhash_lock __cachelineAligned;
atomic_t lhash_users;
wait_queue_head_t lhash_wait; //等待队列头
struct kmem_cache *bind_bucket_cachep; //高速缓存
}; 
```

这个结构是为了维护INET协议族的hash表使用的，结构中的英文注解也非常详细，91行还看到另一个结构体inet_bind_hashbucket。

```c
Struct inlet_bind_hashbucket{//哈希桶结构 spinlock_t lock;//自旋锁 struct hlist_head chain;//桶队列 ）；
```

这个数据结构表示一个哈希桶，chain代表着各个桶的哈希队列。92行还看到structhlist_node，它是hash表的链头，被链入到hlist_head哈希队列头结构中。93行还看到桶结构inet_bind_bucket。

```c
Struct inlet_bind_bucket{//桶结构 struct net \*ib_net;//网络空间指针 unsigned short port://端口号 signed short fastreuse;//可重复使用 struct hlist_node node;//链入哈希桶chain中的哈希节点 struct hlist_head owners;//sock结构队列   
}；
```

这个结构链接到哈希桶inet_bind_hashbucket中的桶结构。

读者不用担心是否能够记忆这么多结构，随代码的分析过程加深理解后自然会体会其定义的作用。inet_csk_get_port()函数95行使net指向内核的init_net网络空间结构，然后根据端口号snum是否为0而进入if语句，端口号snum是从服务器程序代码中传递而来的，当然不为0；但是也可以在服务器程序代码中设置为0表示由内核分配一个端口号。101行先调用inet_get_local_port_range()函数取得端口号的取值范围。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind() $\rightarrow$ inet_csk_get_port() $\rightarrow$ inet_get_local_port_range()

void inlet_get_local_port_range (int \* low, int \*high)   
{ unsigned seq; do { seq $=$ read_seqbegin(&sysctl_port_range_lock); \*low $=$ sysctl_local_port_range[0]; \*high $=$ sysctl_local_port_range[1]; } while (read_segretry(&sysctl_port_range_lock, seq));

其内部涉及了内核的端口范围数组sysctl_local_port_range。

```c
int sysctl_local_port_range[2] = {32768, 61000}; 
```

这个数组是为了保证端口号在 $32768\sim 61000$ 之间。

102行将根据这个范围和随机数设置一个推荐端口号rover。inet_bhashfn()函数将rover与哈希表的长度进行hash计算，根据哈希结果取得哈希桶中对应队列头head，对队列加锁后进入一个do-while循环，inet_bind_BUCKET_for_each是一个循环宏声明。

define inlet_bind_bucket_for_each(tb, node, head) \ hlist_for_each_entry(tb, node, head, node) #define hlist_for_each_entry(tpos, pos, head, member) for(pos $=$ (head)->first; pos&&（{prefetch(pos->next);1;}）&& （{tpos=hlist_entry(pos,typeof(\*tpos)，member);1}）；pos $=$ pos->next)

将inet_csk_get_port()函数108行的内容代入转换：

for(node $=$ (&head->chain) $\rightharpoonup$ first; node&&（{prefetch(node $\rightharpoondown$ next）；1}）&&

```lisp
（{tb = hlist_entry(node,typeof(* tb), node); 1;}）; node = node -> next) 
```

结合109行的代码来看这个循环过程，其实就是沿着哈希桶的每一个桶队列。循环查找同一个网络空间下每一个端口号的桶结构，如果找到了符合条件的桶结构就说明端口已经被占用了，于是增加参考值rover进行下一轮的检查，最终找到端口号还没被占用的桶结构；此时局部变量指针tb指向了该桶，而端口号snum就指向了这个最终的推荐端口号rover。

从上面的循环宏可以看出，桶结构inet_bind_bucket是通过它内部的哈希节点node链入到哈希桶chain队列中的。

我们在服务器程序代码中指定了端口号为9266，因此并不需要在内核中查找端口号，继续看inet_csk_get_port()函数余下的代码。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind() $\rightarrow$ inet_csk_get_port()

} else{//在哈希桶队列中查找相同端口的桶结构 head $=$ &hashinfo->bhash[inet_bhashfn(snum, hashinfo->bhash_size)] spin_lock(&head->lock); inlet_bind_BUCKET_for_each(tb, node, &head->chain) if(tb->ib_net $\equiv =$ net&&tb->port $\equiv =$ snum) goto tb_found; } tb $\equiv$ NULL; goto tb.not_found; tb_found: if(!hlist_empty(&tb->owners)){//检查sock队列是否为空 if(tb->fastreuse $\geq 0$ && sk->sk_reuse&&sk->>sk_state！=TCP_LISTEN){ goto success; }else{ ret $= 1$ //桶结构中的sock队列是否存在冲突 if(inet_csk(sk)->icsk.af ops->bind_conflict(sk，tb)) goto fail_unlock; } } tb.not_found://如果桶结构不存在就创建 ret $= 1$ if(!tb&&(tb $\equiv$ inlet_bind_buckets_createHashinfo->bind_buckets_cachepe, net,head,snum)) $= =$ NULL) goto fail_unlock;

```c
157 if (hlist_empty(&tb-> owners)) { //如果 sock 队列为空  
158 if (sk->sk_reuse && sk->sk_state != TCP_LISTEN)  
159 tb->fastreuse = 1; //设置桶结构可以复用  
160 else  
161 tb->fastreuse = 0;  
162 } else if (tb->fastreuse &&  
163 (!sk->sk_reuse || sk->sk_state == TCP_LISTEN))  
164 tb->fastreuse = 0;  
165 success://如果还没有绑定桶结构  
166 if (!inet_csk(sk) -> icsk_bind_hash)  
167 inlet_bind_hash(sk, tb, snum);绑定桶结构  
168 BUG_TRAP(inet_csk(sk) -> icsk_bind_hash == tb);  
169 ret = 0;  
170  
171 fail_unlock;  
172 spin_unlock(&head-> lock);  
173 fail;  
174 local_bh_enable();  
175 return ret;  
176} 
```

133 行循环在哈希桶中查找 9266 端口的桶结构 tb, 如果找到就要跳转到 tb_found 标号处。tb-> owners 是一个 sock 队列头, 如果这个队列不为空, 就检查它是否支持快速复用, 并且 sock 也允许复用而且没有处于监听状态下就跳转到 success 处; 如果上述条件没有满足就会执行 148 行处的代码:

```txt
inet_csk(sk) -> icsk.af ops -> bind_conflict(sk, tb) 
```

这句代码需要结合前面tcp_v4_init_sock()函数来看：

```txt
icsk-> icsk.af ops = &ipv4_specific; 
```

因此，148行执行的是ipv4_specific结构中的bind_conflict()钩子函数，看一下这个结构的定义：

```python
. bind_conflict = inlet_csk_bind_conflict 
```

其挂入了inet_csk_bind_conflict()函数，它依次在桶结构的队列中取得每个sock结构。对比绑定的sock结构，如果设备相同，则绑定的地址也相同就“冲突”了，于是跳转到fail_un-lock处返回。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_csk_get_port() $\rightarrow$ inlet_csk_bind_con

flict()  
50 int inlet_csk_bind_conflict(const struct sock \* sk,   
51 const struct inlet_bind;bucket \* tb)   
52 {   
53 const __be32 sk_rcv_saddr $=$ inlet_rcv_saddr(sk);   
54 struct sock \* sk2;   
55 struct hlist_node \* node;   
56 int reuse $=$ sk->sk_reuse;   
57   
58 /\*   
59 \* Unlike other sk lookup places we do not check   
60 \* for sk_net here,since_all_the socks listed   
61 \* in tb-> owners list belong to the same net - the   
62 \* one this bucket belongs to.   
63 \*/   
64   
65 sk_for_each_bound(sk2,node,&tb->owners){   
66 if (sk != sk2 &&   
67 !inet_v6_ipv6only(sk2)&&   
68 (!sk->sk_bound_dev_if||   
69 !sk2->sk_bound_dev_if||   
70 sk->sk_bound_dev_if $= =$ sk2->sk_bound_dev_if）{//是否同一设备   
71 if(!reuse ||!sk2->sk_reuse||   
72 sk2->sk_state $= =$ TCP_LISTEN){   
73 const __be32 sk2_rcv_saddr $=$ inlet_rcv_saddr(sk2);   
74 if(!sk2_rcv_saddr||!sk_rcv_saddr||   
75 sk2_rcv_saddr $= =$ sk_rcv_saddr)//是否绑定地址相同   
76 break;   
77 1   
78 1   
79 1   
80 return node！ $=$ NULL;   
81

这个函数65行处也有一个循环宏sk_for_each_bound，只不过它依次取得每个链入到tb->owners队列的sock结构，每个sock结构正是通过sk_bind_node哈希节点链入到队列的。

```txt
define sk_for_each_bound(_sk, node, list) \ 
```

```txt
hlist_for_each_entry(_sk, node, list, sk_bind_node) #define hlist_for_each_entry (tpos, pos, head, member) for (pos = (head) -> first; pos && { prefetch(pos-> next); 1;} && { tpos = hlist_entry(pos, typeof(*tpos), member); 1;}); pos = pos-> next) 
```

如果执行到inet_csk_get_port()函数139行，则表示没有找到桶结构，此时转到tb_NOT_found处。inet_bind_bucket_create()函数用来创建桶结构，并将端口号等内容记录到新建的桶结构中。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_csk_get_port() $\rightarrow$ inlet_bind_bucket_create()

```txt
struct inlet_bind_buckets * inlet_bind_buckets_create(struct kmem_cache * cachep, struct net * net, struct inlet_bind_hashbucket * head, const unsigned short snum) 
```

//创建桶结构

```c
struct inset_bind_bucket * tb = kmem_cache_alloc(cache, GFPorable);
if(tb != NULL) {
    tb->ib_net = hold_net(net); //记录网络空间
    tb->port = snum; //记录
    tb->fastreuse = 0; //快速复用标记为0，根据sock进行调整
    INIT_HLIST_HEAD(&tb->owners); //初始化sock队列
    hlist_add_head(&tb->node, &head->chain); //将桶结构链入到哈希桶中
} return tb;
} 
```

157行根据桶结构的sock队列是否为空，并且sock支持复用、没有处于监听状态，就设置桶结构的复用标志；但是如果桶结构已经设置了复用标志而sock不支持复用或者已经处于监听状态，则将桶结构的标志清除。

如果一切顺利，最终到达了 success 处，这时 tb 桶结构可能是新建的也可能是在哈希桶中找到的。166 行处的 inlet_csk() 是 inline 函数，统称为内联函数。这类函数的好处是在内核启用优化编译选项时，会将 inline 函数与调用者编译在一起，从而达到快速调用的目的。inline 函数的作用非常像宏的作用，由于它的灵活性，因此在内核中可以将这类内联函数放在头文件中方便调用函数的使用。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_csk_get_port() $\rightarrow$ inlet_csk()   
static inline struct inlet_connection SOCK \* inlet_csk (const struct sock \* sk)   
{ return(struct inlet_connection SOCK \*)sk;

struct inlet_connection SOCK 用于 INET 协议族连接 sock, 在前面的分析过程中多次出现了对该结构的使用。

# 代码清单3.4inet_connection_sock结构体的定义

```c
struct inlet_connection_sock {
    /* inlet_sock has to be the first member! */
    struct inlet_sock icsk_inet; // INET 协议族的 sock 结构
    struct request_sock_queue icsk_accept_queue; // 确定接收的队列
    struct inlet_bind:bucket * icsk_bind_hash; // 绑定的桶结构
    unsigned long icsk_timeout; // 超时
    struct timer_list icsk_retransmit_timer; // 没有 ACK 时的重发定时器
    struct timer_list icsk_delack_timer; // 确定删除的定时器
    __u32 icsk_rto; // 重发超时
    __u32 icsk_pmtu_cookie; // 最近的 pmtu
    const struct tcp_congestionOps * icsk_caOps; // 拥挤情况时的处理函数表
    const struct inlet_connection_sock_aftOps * icsk_aftOps; // AF_INET 指定的函数表
    unsigned int (* icskSync_mss)(struct sock * sk, u32 pmtu); // 同步 MSS 的函数指针
    __u8 icsk_ca_state; // 拥挤情况的处理状态
    __u8 icsk_retransmits; // 重发数量
    __u8 icsk_pending; // 挂起
    __u8 icsk_backoff; // 允许连接的数量
    __u8 icsk_syn_retries; // 允许重新 SYN 的数量
    __u8 icsk_probes_out; // 探测到未应答的窗口
    __u16 icsk_ext hdr_len; // 网络协议头部的长度
    struct {
        __u8 pending; /* ACK is pending */
        __u8 quick; /* Scheduled number of quick acks */
        __u8 pingpong; /* The session is interactive */
        __u8 blocked; /* Delayed ACK was blocked by socket lock */
        __u32 ato; /* Predicted tick of soft clock */
        unsigned long timeout; /* Currently scheduled timeout */
        __u32 lrcvtime; /* timestamp of last received data packet */
        __u16 last_seg_size; /* Size of last incoming segment */
        __u16 rcv_mss; /* MSS used for delayed ACK decisions */
```

```c
}icsk ACK;   
struct{ int enabled; / \*Range of MTUs to search \*/ int search_high; int search_low; /\*Information on the current probe. \*/ int probe_size; }icsk_mtup; u32 icsk_capriv[16]; #define ICSK_CA_PRIV_SIZE (16\* sizeof(u32)) }; 
```

这里将 sock 指针直接转换为 struct inet_connection_sock 指针, 然后判断 icsk_bind_hash 是否绑定了桶结构; 如果没有绑定, 则执行INET_bind_hash()函数, 将 sock 结构链入到桶结构中的 sock 队列。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind() $\rightarrow$ inet_csk_get_port() $\rightarrow$ inet_bind_hash()   
voidINET_bind_hash(struct sock \*sk,structinet_bind_buckets\*tb, const unsigned short snum)   
{ inlet_sk(sk) -> num $=$ snum; sk_add_bind_node(sk,&tb-> owners); inlet_csk(sk) -> icsk_bind_hash = tb;

首先，将 sock 的端口号指定为服务器程序的端口号 9266；然后进入 sk_add_bind_node() 函数，将服务器的 sock 链入到桶结构中的 sock 队列。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind() $\rightarrow$ inet_csk_get_port() $\rightarrow$ inet_bind_hash() $\rightarrow$ sk_add_bind_node()   
static __inline__ void sk_add_bind_node(struct sock \*sk, struct hlist_head \*list)   
{ hlist_add_head(&sk->sk_bind_node，list);

入队操作就是将 sock 中的 sk_bind_node 链入到队列头 hlist_head 中, 这个人队函数前面已经介绍了。最后, icsk_bind_hash 记录下 tb 桶。这样, 端口的绑定工作就在内核中完成了,

从inet_csk_get_port()返回到inet_bind()函数515行处，为了阅读方便再把代码列出如下：

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind()   
515 if (inet-> rcv_saddr)//如果已经绑定地址就增加锁标志，表示已经绑定了地址   
516 sk->sk_userlocks $|\equiv$ SOCK_BINDADDR_LOCK;   
517 if (snum)//如果端口也已经确定也要增加锁标志，表示已经绑定了端口   
518 sk->sk_userlocks $|\equiv$ SOCK_BINDPORT_LOCK;   
519 inlet->sport $\equiv$ htons(inet->num);//记录端口   
520 inlet->daddr $= 0$ //初始化目标地址   
521 inlet->dport $= 0$ //初始化目标端口   
522 sk.dst_reset(sk);//初始化缓存的路由内容   
523 err $= 0$ .   
524 out_release_sock;   
525 release_sock(sk); //解锁，并唤醒 sock 锁上的其他进程   
526 out:   
527 return err;   
528

如果绑定了服务器程序指定的地址和端口号，则对 sock 的用户锁增加相应的锁标志。最后将 inlet->sport 本地端口指定为 9266 端口号，将目标地址 inlet->daddr 设为 0，目标端口 inlet->dport 设为 0，最后调用 skDst_reset()函数清空和释放 skDst_cache 所记录的路由 dst_entry 缓存。再返回到 sys_bind()函数 1359 行，接下来的 fput_light()是递减 socket 的文件使用计数器。这个计数器 f_count 是在 sockfd.lookup_light()函数中调用 fget_light()时递增的，并且那里也会调整 fput_needed 标识，表示是否需要递减文件的计数。现在 fput_light()函数根据 fput_needed 来递减文件的计数器，如果文件的计数器递减到 0，则进一步释放文件结构指针。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ fput_light()   
static inline void fput_light(struct file \*file,int fput_needed) { if (unlikely(fput_needed)) fput(file);

由于fput()函数属于文件系统范畴，则就与内核使用的文件系统相关了，篇幅所限这里就不分析了。

# 3.5 网络空间总管init_net

init_net结构的初始化函数其实也使用了initcall机制登记到内核。内核在do_one_init-

call()调用pure_initcall(net_ns_init)登记net_ns_init()函数。

```python
178 #define pure_initcall(fn) __define_initcall("0",fn,0) 
```

net_ns_init()函数会调用setup_net()来对init_net结构进行详细的设置。这里跳过了设置细节，它类似于socket的初始化流程，只不过在setup_net()的初始化中有一个非常重要的循环。

net_ns_init() $\rightarrow$ setup_net()   
53 list_for_each_entry(ops,&pernet_list, list){   
54 if (ops->init) {   
55 error $=$ ops->init(net);   
56 if (error $<  0$ 1   
57 goto out Undo;   
58 }   
59 }

这个循环会从 pernet_list 队列中依次取得每一个网络空间操作表 - pernet_operations 结构指针, 然后调用其内部的钩子函数 init() 完成对 init_net 的初始设置。pernet_list 是一个队列头, 在 /net/core/Netnamespace.c 文件中的 16 行处, 专门用于 person net (用户自定义网络) 的 pernet_operations 登记, 也就是在打开 CONFIG_NET_NS 情形时才可以将用户自定义的 pernet_operations 结构链入队列。这里要介绍的是系统默认的网络空间 init_net, 那么它的 pernet_operations 是什么时候被初始化的呢? 下面从另一个初始化函数 net_dev_init() 说明这一过程。

在内核/net/core/dev.c中有一句代码：

```txt
subsys_initcall(net_dev_init); 
```

它引用了include/Linux/init.h中的

```python
define subsys_initcall(fn) __define_initcall("4",fn,4) 
```

也是借用initcall机制来实现了登记。因此，net_dev_init()函数会在内核初始化时得到执行，由它来完成对内核网络空间操作表的登记注册工作。

```txt
net_dev_init()  
4562 if (register_pernet_subsys(&netdev_netOps))  
4563 goto out;  
4564  
4565 if (register_pernet_device(&default_deviceOps))  
4566 goto out; 
```

其调用的两个函数都会使用register_pernet_operations()。

net_dev_init() $\rightarrow$ register_pernet_subsys()和register_pernet_device()

```c
300 int register_pernet_subsys(struct pernet_operations * ops)  
301 {  
302 int error;  
303 mutex_lock(&net_mutex);  
304 error = register_pernet_operations(first_device, ops)  
305 mutex_unlock(&net_mutex);  
306 return error;  
307 }  
346 int register_pernet_device(struct pernet_operations * ops)  
347 {  
348 int error;  
349 mutex_lock(&net_mutex);  
350 error = register_pernet_operations(&pernet_list, ops);  
351 if (!error && (first_device == &pernet_list))  
352 first_device = &ops->list;  
353 mutex_unlock(&net_mutex);  
354 return error;  
355 } 
```

304行的first_device定义于Net namespace.c中17行：

```txt
static struct list_head *first_device = &pernet_list; 
```

由此看来，这两个函数调用register_pernet_operations()函数时传递的list参数都指向了pernet_list队列。

net_dev_init() $\rightarrow$ register_pernet_operations()   
264 static int register_pernet_operations(struct list_head $\text{串}$ list,   
265 struct pernetoperations $\ast$ ops)   
266 {   
267 if (ops->init $= =$ NULL)   
268 return 0;   
269 return ops->init(&init_net);   
270 }

请读者注意，Net namespace.c 中还有另一个同名的 register_pernet_operations() 函数，但是必须打开用户自定义网络选项才会调用。

结合 net_dev_init() 函数 4562 行, register_pernet_operations() 函数在 269 行处执行的

init() 就是 netdev_netOps 的 init()。

```c
static struct pernet_operations __net_initdata netdev_netOps = {init = netdev_init,exit = netdev_exit,}; 
```

其init()挂入的是netdev_init(),传递给netdev_init()的参数是init_net,由它完成对init_net的初始化。

net_dev_init() $\rightarrow$ register_pernet_operations() $\rightarrow$ netdev_init()   
4472 static int __net_init netdev_init(struct net \*net)   
4473 {   
4474 INIT_LIST_HEAD(&net->dev_base_head);   
4475   
4476 net->dev_name_head $=$ netdev_create_hash();   
4477 if (net->dev_name_head $= =$ NULL)   
4478 goto err_name;   
4479   
4480 net->dev_index_head $=$ netdev_create_hash();   
4481 if (net->dev_index_head $= =$ NULL)   
4482 goto erridx;   
4483   
4484 return 0;   
4485   
4486 err idx:   
4487 kfree(net->dev_name_head);   
4488 err_name;   
4489 return -ENOME;   
4490 }

函数初始化了init_net的几个队列头，其中有两个是哈希队列。INIT_LIST_HEAD()函数初始化单链的队列头，netdev_create_hash()函数则初始化哈希队列头。4476行是对dev_name_head(以设备名称为主键的哈希队列)的初始化，4480行是对dev_index_head(以设备的序号为主键的哈希队列)的初始化。

net_dev_init() $\rightarrow$ register_pernet_operations() $\rightarrow$ netdev_init() $\rightarrow$ INIT_LIST_HEAD()和 netdev_create_hash()

```c
28 static inline void INIT_LIST_HEAD(struct list_head *list)   
29 { 
```

30 list-> next = list;   
31 list-> prev = list;   
32 }   
4458 static struct hlist_head \* netdev_create_hash(void)   
4459 {   
4460 int i;   
4461 struct hlist_head \* hash;   
4462 //分配哈希队列空间   
4463 hash $=$ kmalloc(sizeof(\*hash) \* NETDEV_hasHENTRIES,GFP_KERNEL);   
4464 if (hash != NULL)   
4465 for $(\mathrm{i} = 0;\mathrm{i} <   \mathrm{NETDEV\_HASHENTRIES};\mathrm{i} + + )$ 4466 INIT_HLIST_HEAD(&hash[i]);   
4467   
4468 return hash;   
4469 }

再看 net_dev_init()函数 4565 行的代码, 这是第二次调用 register_pernet_operations()函数。这次传递的参数 ops 是 default_deviceOps 结构, register_pernet_operations()也会执行这个结构中的 init 函数。

static struct pernet_operations __net_initdata default_deviceOps = { .exit $=$ default_device_exit,   
}；

但是，default_deviceOps结构中并没有设置init()的内容，因此，在268行直接返回0给上层调用函数，只有register_pernet_device()函数在351行对这个返回值进行检查。

net_dev_init() $\rightarrow$ register_pernet_device() if(!error&&(first_device $= =$ &pernet_list)) first_device $= \&$ ops-> list;

first_device 调整为 default_deviceOps 队列头 list。

到这里只是看到了对init_net几个队列头的初始化，并没有其他操作对init_net的网络结构进行设置，但是伴随着代码的逐渐深入将会看到它在多个地方进行了设置和调整。可以说，init_net是整个网络空间的总管，所以有网络命名空间的叫法，这也是struct net的形象表示。限于篇幅不能详细对init_net的初始化操作全部描述，请读者根据上述过程自己归纳总结。

# 路 由

# 4.1 路由函数表结构及关系图

首先看一下分析过程将要涉及的数据结构及关系图，如图4.1所示。

本章的分析过程中将陆续接触图4.1中的结构，我们需要经常回顾此图以理解这些数据结构的作用和关系。从图中可以看出，路由函数表fib_table是路由的总根，可以从它出发找到各个路由区结构fn-zone，也可以找到路由节点结构fib_node或者路由信息结构fib_info。

# 4.2 路由函数表的初始化

我们在3.3节__inet_dev_addr_type()函数留下了路由函数表初始化的疑问，如果要解答这个问题需要再从inet_init()函数看起，由此理清路由函数表的初始化经过并从中找出本地路由函数表的内容。

inet_init()函数内部调用了ip_init(),通过它进一步调用ip_rt_init()函数，由此完成路由函数表的初始化操作。

```c
inet_init() $\rightarrow$ ip_init() 和 ip_rt_init()  
```c
1408 void __init ip_init(void)  
1409 {  
1410 ip_rt_init();  
1411 inlet_initpeers();  
1412  
1413 #if defined(CONFIG_IPMULTICAST) && defined(CONFIGPROC_FS)  
1414 igmp_mc_proc_init();  
1415 #endif  
1416 }  
3030 int __init ip_rt_init(void)  
3031 {
```

```txt
int rc = 0; 
```

//设置路由随机数

```lisp
atomic_set(&rt_genid, (int) ((num_physpages ^ (num_physpages >> 8)) ^ (jiffies ^ (jiffies >> 7)))) 
```

```c
ifdef CONFIG_NET_CLS_ROUT  
ip_rt_acct = __alloc_percpu(256 * sizeof(struct ip_rt_acct));  
if (!ip_rt_acct)  
panic("IP: failed to allocate ip_rt_acct\n");  
#endif 
```

//创建路由项的高速缓存，对象长度等于路由表的长度

```txt
ipv4.dstOps.kmem_cachep =  
kmem_cache_create("ip.dst_cache", sizeof(struct rtable), 0,  
SLAB_HWCACHE Align|SLAB_PANIC, NULL);
```

```txt
ipv4.dst.blackhole ops.kmem_cachep = ipv4.dst OPS.kmem_cachep; 
```

//创建路由哈希桶缓存

```c
rt_hash_table = (struct rt_hash_buckets * )  
alloc_large_system_hash("IP route cache", sizeof(struct rt_hash_buckets), rhash_entries, (num_physpages >= 128 * 1024)? 15:17, 0, &rt_hash_log, &rt_hash_mask, 0);  
memset(rt_hash_table, 0, (rt_hash_mask + 1) * sizeof(struct rt_hash_buckets)); //初始化路由哈希队列  
rt_hash_lock_init();  
ipv4.dstOps.gc thresh = (rt_hash_mask + 1); //记录回收底线  
ip_rt_max_size = (rt_hash_mask + 1) * 16; //哈希桶的最大长度  
devinet_init();  
ip_fib_init();  
rt_secret_timer.function = rt_secret_rebuild;  
rt_secret_timer.data = 0; 
```

3070 init_timer_deferrable(&rt_secret_timer);   
3071   
3072 /\*All the timers,started at system startup tend   
3073 to synchronize.Perturb it a bit.   
3074 \*/   
3075 schedule_delayed_work(&expires_work,   
3076 net_random(）%ip_rt_gc_interval $^+$ ip_rt_gc_interval);   
3077   
3078 rt_secret_timer.expres $=$ jiffies $^+$ net_random(）%ip_rt_secret_interval+   
3079 ip_rt_secret_interval;   
3080 add_timer(&rt_secret_timer);   
3081   
3082 if(ip_rt_proc_init())   
3083 printf(KERN_ERR"Unable to create route proc files\n");   
3084 #ifdef CONFIG_XFRM   
3085 xfrm_init();   
3086 xfrm4_init();   
3087 #endif   
3088 rtnl register(PF_INET,RTM_GETROUTE,inet_rtm_getroute，NULL);   
3089   
3090 return rc;   
3091

函数3034行处调用了atomic_set宏，这个宏的定义在include/asm-x86/atomic_32.h中。

```c
define atomic_set(v,i) ((v->counter) = (i))  
static atomic_t rt_genid __readONLY;  
typedef struct {  
    int counter;  
} atomic_t; 
```

这里一起列出了rt_genid的定义。宏atomic_set的主要作用是设置rt_genid的计数器值，它是一个atomic_t结构类型的变量。由于atomic_set能够通过一次内存的访问就实现了赋值操作，这样的操作称为原子操作。

为什么要使用原子操作？具有操作系统理论的读者可能了解中断会影响当前进程，尤其两个进程对同一个变量进行修改和读取时，如果一个进程正在读取变量值，而另一个进程抢占内核将这个变量修改了，假如第一个进程再进一步读或者写，就会导致变量的“不同步”。

为了保证变量的同步操作，使用了这种原子操作方法，（即避免被其他进程和中断打断的操作就作为原子操作），实际上这只是表面现象，更主要的是对变量操作最终在硬件上采用一条汇编语句实现。Linux通过内嵌汇编语句来实现原子操作，如果是多cpu还会使用lock锁

住总线,这种方法的好处在 SMP 系统中得到了很好的体现。

rt_genid 定义后缀了 __readoretly, 这保证了原子变量也能够在 SMP 系统中安全地访问, 能够使多处理器的缓存保持对变量的同步, 它使用在大多数时间处在读操作的情形中; 可是, 当其中一个处理器对变量写时, 则会增加其他处理器的额外开销, 因此对 __readoretly 的使用是经过程序员仔细斟酌后确定的。

3038 行调用了 __alloc(percpu() 函数也是基于上述作用, 代码针对 SMP 系统的多 CPU 情形分配 percpu 数据结构, 关于 percpu 数据结构将会在后续章节再行讲解, 这里 ip_rt_acct 用于路由参数统计使用。

```txt
struct ip_rt_acct
{
    __u32 o_bytes;
    __u32 o_packets;
    __u32 i_bytes;
    __u32 i_packets;
}; 
```

前两个是发出的字节和包数，后两个是接收的字节和包数。3043行接着对ipv4.dstOps的高速缓存进行了创建，ipv4.dstOps是一个全局dstOps结构变量。这类结构用于路由项的函数表，其内部包含一个高速缓存kmem_cachep（用于路由表rtable结构），这里创建的高速缓存名称定为ip.dst_cache。struct dstOps结构其实属于协议无关的缓存间的接口，也指定了一些事件的协议通知，如链接出错情况，而这里IPv4协议定义了自己的dstOps结构ipv4.dstOps。

关于slab的高速缓存函数kmem_cache_create()是内存管理的内容。代码3047行也将ipv4.dst/blackholeOps指向ipv4.dstOps的路由表缓存。从字面上理解，ipv4.dst/blackholeOps是路由项“黑洞”的处理函数表，这里暂且不列出这两个结构变量的具体内容，到具体的分析过程时再介绍。3049行是对路由表队列rt_hash_table的初始化。

```txt
static struct rt_hash_buckets *rt_hash_table __readONLY; 
```

rt_hash_table 是 rt_hash_buckets 结构，专门用于路由表队列，内部只包含路由表结构队列指针 chain。路由表的结构体是 rtable，这与代码清单 4.2 介绍的 fib_table 具有不同的作用。

```c
struct rt_hash_buckets { //路由哈希队列的定义
struct rtable *chain; //路由表结构队列
}; struct rtable//路由表结构体
```

```txt
union { struct dst_entry dst; }u;   
}; struct dst_entry//路由项的定义   
{ union{ struct dst_entry \* next; struct rtable \*rt_next; struct rt6_info \*rt6_next; struct dn-route \*dn_next; 1 
```

rtable 是路由表结构体, 后续过程还会进一步对它的内容进行介绍。rtable 开始处有一个 union, 其内部声明了一个路由项结构变量 dst; 而 dst_entry 结构的最后也有一个 union, 其内部通过 next 指针将路由项结构链成队列; rtable 则使用 rt_next 链成路由表队列; dst_entry 结构也放在后面探讨。提醒读者不要将路由函数表结构 fib_table 与 rtable 混为一谈, fib_table 内部几乎全是函数指针, 它提供了查询或者创建路由的方法; rtable 则装载着具体路由内容, 这些内容都是通过 fib_table 的函数查找并初始化到 rtable 中的。

在内核中真正起路由作用的是 rtable, 它存在于 rt_hash_buckets 队列中。我们将会看到数据包需要使用的是 rtable, 而不是 fib_table。由此看出, 图 4.1 中的 fib_table 结构及其他相关结构都是搭建路由表 rtable 的基础, 它们为路由表提供具体的路由信息, 这也是本书将 fib_info 结构称为路由信息的原因。

图4.2是路由表结构示意图，概括了路由表在内存中的组织情况（在计算机领域，哈希桶就是指杂凑队列）。

3050 行调用 alloc_large_system_hash()分配一个 rt_hash_buckets 的路由表队列空间，rt_hash_table 指向新分配的路由表队列。alloc_large_system_hash()函数从系统的 bootmem 空间分配大量的路由表空间。bootmem 空间是内核启动时使用的内存管理策略，主要指内核使用的页面；其意图是从这部分内存中按页进行分配，这部分内存页面是不会被 Linux 交换出去或者回收的，一经分配就不再改变了，可见路由表结构的重要性。

分配了路由表队列空间后，还要对其进行初始化设置，3059行的memset()函数因此而调用。rt_hash_mask代表分配的路由哈希桶的数量，要使ipv4.dstOps记录下可以回收的碎片数gc thresh，也要使ip_rt_max_size记录下整个路由的大小。接着3065行调用devinet_init()函数执行一些登记工作。

![](images/2bdf4ccf6e47dc266cb8bb3ea697f0a7231fd02f15116141eecb63589ac7e5a0.jpg)  
图4.2 路由表结构示意图

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ devinet_init()
1638 void __init devinet_init(void)
1639 {
1640 register_pernet_subsys(&devinet ops);
1641
1642 register_gifconf(PF_INET,INET, gifconf); //注册I/O配置程序
1643 register_netdevice.notifier(&ip_netdev.notifier); //注册通知节点
1644 //注册处理路由地址的netlink
1645 rtnl_register(PF_INET,RTM新加ADDR,INET_rtm新加addr,NULL);
1646 rtnl_register(PF_INET,RTM_DELADDR,inet_rtm_deladdr,NULL);
1647 rtnl_register(PF_INET,RTM_GETADDR,NULL,inet_dump_ifaddr);
1648 }

1640 行处再向内核登记一个 pernet_operations 结构，这个结构提供了网络空间的操作表，即网络空间的函数表。这次登记的是 devinet ops 结构变量，它提供了初始化网络空间和释放网络空间的钩子函数。

static __net_initdata struct pernet_operations devinetOps = {init = devinet_init_net,exit $=$ devinet_exit_net,};

register_pernet_subsys()函数的过程前面已经看过了。register_gifconf()函数向内核注册了一个SIOCGIF处理程序(SIOCGIF是socket IO Config Interface,即套接字IO配置接口

的意思), gif 并不是指 GIF 图片而是 generous interface configure(通用接口配置), 这里注册了一个 I/O 接口配置程序 inset_gifconf()。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ devinet_init() $\rightarrow$ register_gifconf()
    int register_gifconf(unsigned int family, gifconf_func_t * gifconf)
{
    if (family >= NPROTO)
        return -EINVALID;
    gifconf_list[family] = gifconf;
    return 0;
}

内核中有一个全局的gifconf_list数组，它是gifconf_func_t函数指针数组，用来保存通用的I/O配置程序。上面1642行以具体的INET协议族为下标，保存inet.gifconf函数指针到数组。这里暂时不关心inet.gifconf的具体作用和过程，1643行注册了一个ip_netdev.notifier通知节点结构。

static struct notifier_block ip_netdev.notifier $=$ {.notifier_call $\equiv$ inetdev_event,

关于通知链结构的注册函数register_netdevice_NOTIFY()和用于路由netlink的rtnl_register()函数请参阅本书通知链和netlink章节。

回到ip_rt_init()函数中，3066行调用了ip_fib_init()函数，这个函数内也会调用rtnl/register()，只不过这次注册的路由netlink是用于管理路由目的，而devinet_init()函数中注册的则用于管理路由地址。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init()
1056 void __init ip_fib_init(void)
1057 //注册用于管理路由的 netlink
1058 rtnl register(PF_INET, RTM-NewROUTE, inet_rtm_newroute, NULL);
1059 rtnl register(PF_INET, RTM_DELROUTE,INET_rtm_delroute, NULL);
1060 rtnl register(PF_INET, RTM_GETROUTE, NULL,inet_dump_fib);
1061
1062 register_pernet_subsys(&fib_net ops);
1063 register_netdevice不斷器(&fib_netdev不斷器);
1064 register_inetaddr不斷器(&fib_inetaddr不斷器);
1065
1066 fib_hash_init();
1067 }

代码共调用了3次rtnl_register()，向rtnl msghandlers数组中登记了路由的3个操作函数，前两次调用过程的参数doit分别指向了inet_rtm_newroute()和inet_rtm_delroute()，第三次调用过程的参数dumpit指向了inet_dump_fib()。它们的参数msgtype分别是RTM NewlyROUTE、RTM_DELROUTE、RTM_GETROUTE，分别代表创建路由、删除路由和获取路由。

前面已经看过了register_pernet_subsys()函数的代码，这里1062行将fib_net ops挂入到了first_device队列中。这个过程是register_pernet_subsys()函数调用register_pernetoperations()完成的，在register_pernet_operations()函数中还会执行fib_net ops结构的init()钩子函数。

```c
static struct pernet_operations fib_netOps = {
init = fib_net_init,
exit = fib_net_exit,
}; 
```

从结构中看到init()挂入的是fib_net_init()函数。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_pernet_subsys() $\rightarrow$ register_pernet_operations $\rightarrow$ fib_net_init()

1021 static int __net_init fib_net_init(struct net \*net)   
1022 {   
1023 int error;   
1024   
1025 error $=$ ip_fib_net_init(net);   
1026 if (error $<  0$ 1   
1027 goto out;   
1028 error $=$ nl_fib.lookup_init(net);   
1029 if (error $<  0$ 1   
1030 goto out_nlf1;   
1031 error $=$ fib_proc_init(net);   
1032 if (error $<  0$ 1   
1033 goto out_proc;   
1034 out:   
1035 return error;   
1036   
1037 out_proc:   
1038 nl_fib.lookup_exit(net);   
1039 out_nfl:   
1040 ip_fib_net_exit(net);

```txt
1041 goto out;   
1042 
```

函数先调用了ip_fib_net_init()来初始化init_net网络空间的fib_table_hash路由函数表队列。

```c
inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_pernet_subsys() $\rightarrow$ register_pernet_operations $\rightarrow$ fib_net_init() $\rightarrow$ ip_fib_net_init()

975 static int __net_init ip_fib_net_init(struct net \*net)   
976 {   
977 int err;   
978 unsigned int i;   
979 //为路由函数表队列分配空间   
980 net-> ipv4.fib_table_hash = kzalloc(   
981 sizeof(struct hlist_head) $\ast$ FIB_TABLE_hasHSZ,GFP_KERNEL);   
982 if (net-> ipv4.fib_table_hash == NULL)   
983 return -ENOMEM;   
984 //初始化每个队列头   
985 for $(\mathrm{i} = 0;\mathrm{i} <   \mathrm{FIB\_TABLE\_HASHSZ};\mathrm{i} + + )$ 986 INIT_HLIST_HEAD(&net-> ipv4.fib_table_hash[i]);   
987 //初始化本地路由函数表和主路由函数表并链入到路由函数表队列数组中   
988 err $=$ fib4/rules_init(net);   
989 if (err $<  0$ ）   
990 goto fail;   
991 return 0;   
992   
993 fail:   
994 kfree(net-> ipv4.fib_table_hash);//如果出现错误则释放路由队列数组的空间   
995 return err;   
996 }

代码中的FIB_TABLE.HasHSZ定义为256，所以最多使用256个路由函数表队列，具体分配是由函数kzalloc()来完成的；它也是调用kmalloc()内存分配函数来完成的，只不过增加了GFP_ZERO清零标志，表示内核新分配的内存自动清零。

分配成功后，在985行的循环要使用INIT_HLIST_HEAD宏来初始化这些路由函数表的队列头。

```txt
define INIT_HLIST_HEAD(ptr) ((ptr) -> first = NULL) 
```

初始化只是使其 first 指针置空，表示现在还没有任何的路由函数表，而 988 行调用 fib4Rules_init() 注册两个路由函数表并登记到队列数组中。内核也有两个同名函数，这里列出的是没有打开多路由函数表选项（也可称作多路由规则选项）的函数代码。

```c
inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_pernet_subsys() $\rightarrow$ register_pernet_operations $\rightarrow$ fib_net_init() $\rightarrow$ ip_fib_net_init() $\rightarrow$ fib4_rule_s_init()

52 static int __net_init fib4Rules_init(struct net \*net)   
53 {   
54 struct fib_table \*local_table，\*main_table;   
55   
56 local_table $=$ fib_hash_table(RT_TABLE_LOCAL);//创建本地路由函数表   
57 if(local_table $= =$ NULL)   
58 return-ENOME;   
59   
60 main_table $=$ fib_hash_table(RT_TABLE_MAIN);//创建主路由函数表   
61 if(main_table $= =$ NULL)   
62 goto fail;   
63 //将两个路由函数表链入队列数组中   
64 hlist_add_head_rcu(&local_table->tb_hlist,   
65 &net->ipv4.fib_table_hash[TABLE_LOCAL_INDEX]);   
66 hlist_add_head_rcu(&main_table->tb_hlist,   
67 &net->ipv4.fib_table_hash[TABLE_MAIN_INDEX]);   
68 return 0;   
69   
70 fail:   
71 kfree(local_table);   
72 return -ENOME;   
73 }

回忆前面 fib_get_table()函数的过程，它正是从路由函数表队列中查找本地路由函数表，且在那里提出了对本地路由函数表何时初始化的疑问，这里的 fib4_rules_init()函数正是它的答案。

再看打开多路由函数表选项时的另一个同名函数：

```c
int __net_init fib4Rules_init(struct net \*net)   
{ int err; struct fib_rule_sops \* ops; //复制模板规则函数表
```

```c
ops = kmemdup(&fib4_ruleOps_template, sizeof(* ops), GFP_KERNEL);  
if (ops == NULL)  
    return -ENOMEM;  
INIT_LIST_HEAD(&ops->rules_list); //初始化链头  
ops->fro_net = net; //记录网络空间结构指针  
fib_rule_register(ops); //链接到网络空间结构中的专用队列  
err = fib_default_rule_init(ops); //创建3个路由规则并链接到专用队列  
if (err < 0)  
    goto fail;  
net->ipv4.rules OPS = ops;  
return 0;  
fail:  
/* 清除已经建立的规则，释放规则函数表*/  
fib_rule_unregister(ops);  
kfree(ops);  
return err;
```

它调用kmemdup()函数复制fib4_ruleOPS_template内核的模板规则到ops,这是fib_rule OPS结构变量,结构的定义在fib_rule.h中,其内部大多是函数指针,在此同时列出了路由规则结构的定义。

# 代码清单4.1 fib_ruleOps与fib_rule结构定义

```txt
struct fibRulesOps 
```

{//路由规则函数表的结构定义

```c
int family; //协议族ID  
struct list_head list; //队列头，用于链入网络空间的队列中  
int rule_size; //规则结构长度  
int addr_size; //地址长度  
int unresolvedRules;  
int nr_goto/rules;
```

//动作函数指针

```txt
int (*action)(struct fib_rule *, struct flowi *, int, struct fibLOOKUP_arg *); 
```

//匹配函数指针

```ocaml
int (*match)(struct fib_rule *, 
```

```txt
struct flowi \*, int); 
```

# //配置函数指针

```txt
int (*configure)(struct fib_rule *, struct sk_buffer *); struct sk_buffer *; struct nlmsghdr *; struct fib_rulehtar *; struct nlattr *); 
```

# //比对函数指针

```txt
int (*compare)(struct fib_rule *, struct fib_rule hdr *, struct nladdr *); 
```

# //填写函数指针

```txt
int (*fill)(struct fib_rule *, struct sk_buffer * , struct nlmsghdr *, struct fib_rule hdr *); 
```

# //查找优先级函数指针

```txt
u32 (* default_prefix)(struct fib_rules ops * ops); 
```

# //统计负载数据能力函数指针

```sql
size_t (*nlmsg_payload)(struct fib_rule *); 
```

# 修改规则队列后，必须刷新缓存的函数指针*/

```c
void (*flush_cache)(void); 
```

```c
int nlgroup; //路由 netlink 的组划分标识  
const struct nla_policy * policy; //netlink 的属性优先级  
struct list_head rules_list; //路由规则队列  
struct module * owner;  
struct net * fro_net; //网络空间结构指针  
};  
struct fib_rule
```

# {//路由规则的结构定义

```c
struct list_head list; //队列头，用来链入路由规则函数的队列中  
atomic_t refcnt; //计数器  
int ifindex; //网络设备ID  
char ifname[IFNAMSIZ]; //用于保存网络设备名称  
u32 mark; //用于过滤作用  
u32 mark_mask; //掩码  
u32 pref; //优先级  
u32 flags; //标志位
```

```c
u32 table;//路由函数表ID  
u8 action;/动作标识  
u32 target;  
struct fib_rule * ctarget;//当前规则  
struct rcu_head rcu;  
struct net * fr_net;//网络空间结构指针 
```

fib4_rule_init()函数通过fib_rule_register()将新建的ops链入到所属网络空间的rulesOps队列中，然后调用fib_default_rule_init()函数增加3个路由规则：本地路由规则、主路由规则、默认路由规则，并将它们链入rules_list队列。

fib4_rule_init() $\rightarrow$ fib_default_rule_init()   
```c
static int fib_defaultRules_init(struct fib/rulesOps * ops)  
{  
    int err;  
    //初始化本地路由规则  
    err = fib_default_rule_add(ops, 0, RT_TABLE_LOCAL, FIB-rule_PERMANENT);  
    if (err < 0)  
        return err;  
    //初始化主路由规则  
    err = fib_default_rule_add(ops, 0x7FFE, RT_TABLE_MAIN, 0);  
    if (err < 0)  
        return err;  
    //初始化默认路由规则  
    err = fib_default_rule_add(ops, 0x7FFF, RT_TABLE_DEFAULT, 0);  
    if (err < 0)  
        return err;  
    return 0;  
}  
int fib_default_rule_add(struct fib/rulesOps * ops, u32 pref, u32 table, u32 flags)  
{  
    struct fib_rule * r; //声明路由规则结构变量  
    r = kzallocOPS->rule_size, GFP_KERNEL); //分配路由规则结构空间  
    if (r == NULL)  
        return -ENOME;  
    atomic_set(&r->refcnt, 1); //增加路由规则计数器
```

r->action $=$ FR_ACT_TO_TB;//动作标识为“使用指定的路由函数表”  
r->pref $\equiv$ pref;//指定优先级  
r->table $\equiv$ table;//指定路由函数表  
r->flags $\equiv$ flags;//记录标志位  
r->fr_net $\equiv$ hold_net(opse $\rightarrow$ fro_net); //记录网络空间  
/\*The lock is not required here, the list in unreachable  
\*at the moment this function is called \*/list_addTAIL(&r->list,&ops->rules_list);//链入路由规则函数表中的队列return0;

从fib4_rules_init()函数过程来看，它完成了fib_get_table()函数的对接，其代码56行调用了fib_hash_table()函数来创建路由函数表。这个函数中定义了一个路由函数表结构变量tb,这类fib_table结构在以前的分析过程中多次出现过。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_pernet_subsys() $\rightarrow$ register_pernet_operations() $\rightarrow$ fib_net_init() $\rightarrow$ ip_fib_net_init() $\rightarrow$ fib4 rulings_init() $\rightarrow$ fib_hash_table()
struct fib_table *fib_hash_table(u32 id)
{
    struct fib_table * tb;
    //在分配fib_table结构空间时连同fn_hash一起分配
    tb = kmalloc(sizeof(struct fib_table) + sizeof(struct fn_hash),
        GFP_KERNEL);
    if (tb == NULL)
        return NULL;
}
//初始化设置fib_table
tb->tb_id = id;
tb->tb_default = -1;
tb->tb.Lookup = fn_hashLOOKUP;
tb->tb_insert = fn_hash_insert;
tb->tb_delete = fn_hash_delete;
tb->tb Flush = fn_hash Flush;
tb->tb_select_default = fn_hash_select_default;
tb->tb_dump = fn_hash_dump;
memset(tb->tb_data, 0, sizeof(struct fn_hash)); //清零fn_hash结构
return tb;

先来看数据结构 fib_table 的定义, 从名称上看它也是路由表的意思, 但这个结构名称并不恰当。前面我们介绍了路由表由 rtable 结构来表示, 而 fib_table 则给路由表提供了查询路由信息的操作方法; fib_table 从目的来看应该定义为“fibOPS”更恰当, 本书为了便于区分称之为路由函数表。注意, 其内部除包含着路由标识符外还有一组函数指针。

# 代码清单4.2 fib_table结构定义

```c
struct fib_table { //路由函数表结构定义
struct hlist_node tb_hlist; //哈希节点
u32 tb_id; //标识符
unsigned tb_stamp; //时间戳
int tb_default; //路由信息结构的队列序号
int (*tbLOOKUP)(struct fib_table *tb, const struct flowi *flp, struct fib_result *res);
int (*tb_insert)(struct fib_table *, struct fib_config *);
int (*tb_delete)(struct fib_table *, struct fib_config *);
int (*tb_dump)(struct fib_table *table, struct skbuff *skb, struct netlink_callback *cb);
int (*tb Flush)(struct fib_table *table);
void (*tb_select_default)(struct fib_table *table, const struct flowi *flp, struct fib_result *res);
unsigned char tb_data[0];
}; 
```

tb_hlist是链入哈希队列的节点，tb_id是路由函数表的标识符，如RT_TABLE_LOCAL表示本地路由函数表，tb.lookup()函数指针用于具体的路由查找过程，tb_insert()和tb_delete()则是对路由的插入和删除操作函数指针，tb_dump()用于路由的转发，tb Flush()用于移除路由信息结构，tb_select_default()用于选择默认的路由。tb_data[0]是一个零元素的数组，用来标识fib_table结构的结尾，当在内存中分配fib_table结构空间时，连同fn_hash结构一起分配；而tb_data数组则表示紧跟在结构后面的fn_hash。fn_hash是路由区队列结构体。

# 代码清单4.3 fn_hash与fn-zone结构定义

```c
struct fn_hash { //路由区队列结构定义
    struct fn-zone     *fn Zones[33]; //路由区表或称作路由区队列
    struct fn-zone     *fn-zone_list; //指向第一个路由区表
}; 
struct fn-zone { //路由区结构定义
    struct fn-zone     *fz_next;   /*指向下一个不为空的路由区结构 */
    struct hlist_head       *fz_hash;   /*哈希队列 */
    int        fz_nent;  /*其包含的路由节总数 */
```

```c
int fz_divisor; /*哈希头数量 */
u32 fz_hashmask; /*确定哈希头的掩码 */
#define FZ_HASHMASK(fz) ((fz) -> fz_hashmask)
int fz_order; /*子网掩码位数 */
__be32 fz_mask; /*子网掩码 */
#define FZ_MASK(fz) ((fz) -> fz_mask);
```

路由区队列结构中包括一个路由区结构数组和一个路由区结构队列fn-zone_list。struct fn-zone结构表示路由区(使用同一个子网掩码的网络称为一个路由区，这里用“区”来表示一套路由集合)。fz_next指向所在队列的下一个路由区结构，由它链入到路由区队列的fn-zone_list中；fz_hash则指向所属的哈希表，fz_nent是路由区包含的路由数量，fz_divisor是哈希表的大小（也就是哈希桶的数量）；fz_hashmask的值是 $\mathrm{fz\_divisor} - 1$ ，它提供了快速的逻辑与运算(AND)所使用的值，提高了运算效率，降低了CPU的使用时间。

fz_order是网络掩码位数。图4.3中的255.255.0.0掩码位数fz_order是16位；fz_mask是网络掩码，它根据fz_order来确定，如fz_order是6时产生的fz_mask二进制码是11111100.00000000.00000000.00000000，即十进制的252.0.0.0。另外，结构中还有两个宏FZ_HASHMASK和FZ_MASK，它们实际上就是取得相应路由区结构的fz_hashmask和fz_mask。

![](images/70fbb7187dda35b866e104882884919c1cf4e324337dc09d5c2f0523e094e911.jpg)  
图4.3 子网掩码示意图

子网掩码是用来判断任意两台计算机的 IP 地址是否属于同一个子网络的，即是否属于同一个路由区。简单理解就是两台计算机各自的 IP 地址与子网掩码进行逻辑“与”（AND）运算，如果得出的结果是相同的，则说明这两台计算机处于同一个子网络（同一个路由区），两台

计算机就可以进行直接的通信。

对于IPV4协议来说，它使用的是32位的网络掩码值，所以每一个路由表就会有33套路由区来表示。因此，对于地址10.0.1.0/24（IP地址/网络掩码表示法，也就是10.0.1.0/255.255.0)和10.0.2.0/24的子网路由来说，它将会放在fn_hash的24位路由区中（第25套路由区），子网路由10.0.3.128/25将会在25位的路由区中（第26套路由区）。

fn_hash 第一个数组就是指向了这个 33 套的路由区队列（数组与指针在 C 语言里是同等的），而每一套路由区队列中的路由区结构又通过 fz_next 链接在一起，fn Zones[0]用于默认网关。fn-zone_list 域就是将正在使用的 fn-zone 链成一个队列。

fib_hash_table()函数将fn_hash结构紧贴在路由函数表tb后面一起分配内存空间，之后对tb进行初始化设置，将钩子函数挂入，包括对路由表的插入、删除、查找函数等。从fib4rules_init()函数过程来看，这里的tb是指本地路由函数表，最后通过memset()函数将tb_data所指向fn_hash结构空间清零初始化。

在fib4_rule_init()函数56行处的local_table指向了新创建的本地路由函数表，57行检查本地路由函数表是否创建成功。60行也以同样的过程传递参数RT_TABLE_MAIN来创建一个主路由函数表，这个路由函数表是内核默认使用的，如果没有指定所使用的路由函数表内核就会使用它。

64 行调用 hlist_add_head_rcu()函数将新创建的两个路由函数表(本地路由函数表和主路由函数表)插入到网络空间的 ipv4 的路由函数表队列。

```c
inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ fib_net_init() $\rightarrow$ ip_fib_net_init() $\rightarrow$ fib4_rule_init() $\rightarrow$ hlist_add_head_rcu()

static inline void hlist_add_head_rcu(struct hlist_node \*n, struct hlist_head \*h)   
{ struct hlist_node \*first $=$ h-> first; n-> next $=$ first; n-> pprev $=$ &h-> first; smp_wmb(); if(first) first->pprev $=$ &n->next; h-> first $=$ n;

对哈希队列头的操作实际就是节点结构与队列头结构的挂钩，实质是将路由函数表结构挂入到 net 空间结构的队列中，因此可以在 fib_get_table() 函数中从这个队列中找到本地路由函数表和主路由函数表。

```c
struct hlist_head { //队列头
    struct hlist_node *first;
}; 
```

路由函数表的初始化操作是开启路由过程的一把钥匙。fib4_rules_init()函数完成登记任务之后，向上返回到fib_net_init()函数中，接下来1028行调用了nl_fibLOOKUP_init()函数。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_pernet_subsys() $\rightarrow$ register_pernet_operations() $\rightarrow$ fib_net_init() $\rightarrow$ nl_fibLOOKUP_init()
static int nl_fibLOOKUP_init(struct net *net)
{
    struct sock *sk;
    sk = netlink_kernel_create(net, NETLINK_FIB LOOKUP, 0, nl_fib_input, NULL, THISMODULE);
    if (sk == NULL)
        return -EAFNOSUPPORT;
    net->ipv4.fibnl = sk;
    return 0;
}

netlink_kernel_create()函数是与netlink相关的重要函数，提供了非阻塞的消息传递功能，具体过程请读者参阅本书netlink的分析内容。

这里将新创建的 sock 记录在网络空间的 ipv4.fibnl 中，以此建立了通过网络空间找到 netlink SOCK 的桥梁。

回到fib_net_init()函数，1031行调用fib_proc_init()在内核proc文件系统中创建一个route节点。由于proc文件系统不是本书的重点，fib_proc_init()函数在此不做分析。

向上返回到函数ip_fib_init()中，为了阅读方便再次列出函数的代码。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init()
1056 void __init ip_fib_init(void)
1057 {
1058 rtnl register(PF_INET, RTM NEWROUTE, inet_rtm_newroute, NULL);

```c
1059 rtnl register(PF_INET, RTM_DELROUTE, inet_rtm_delroute, NULL);  
1060 rtnl register(PF_INET, RTM_GETROUTE, NULL,INET_dump_fib);  
1061  
1062 register_pernet_subsys(&fib_net ops);  
1063 register_netdevice.notifier(&fib_netdev.notifier);  
1064 register_inetaddr.notifier(&fib_inetaddr.notifier);  
1065  
1066 fib_hash_init();  
1067} 
```

1063 行调用 register_netdevice_NOTIFY() 函数将 fib_netdev_NOTIFY 设备通知节点链入到内核的设备通知链 netdev_chain, 1064 行调用 register_inetaddr_NOTIFY() 函数将地址通知节点 fib_inetaddr_NOTIFY 挂入到 inetaddr_chain 地址通知链, 这两个函数放在通知链章节中介绍。读者可以先看一下它的注册过程。

在注册设备通知节点过程中要两次调用 fib_netdev.notifier 结构中的 notifier_call()。

static struct notifier_block fib_netdev.notifier $=$ {.notifier_call $\equiv$ fib_netdev_event,}；

其挂入的是 fib_netdev_event() 函数, 第一次调用时传递的参数 event 是 NETDEVRegister, 第二次调用时是 NETDEV_UP。

933 static int fib_netdev_event(struct notifier_block \* this, unsigned long event, void \* ptr)   
934 {   
935 struct net_device \* dev $=$ ptr;   
936 struct in_device \* in_dev $=$ __in_dev_get_rtnl(dev);   
937   
938 if (event $= =$ NETDEV_UNREGISTER) {   
939 fib_disable_ip(dev, 2);   
940 return NOTIFY_DONE;   
941 }   
942   
943 if(!in_dev)   
944 return NOTIFY_DONE;   
945   
946 switch (event){   
947 case NETDEV_UP:   
948 for_ifa(in_dev){   
949 fib_add_ifaddr(ifa);

```c
950 } endfor_ifa(in_dev);   
951 #ifdef CONFIG_IP-route这条路   
952 fib_sync_up(dev);   
953 #endif   
954 rt_cache Flush(-1);   
955 break;   
956 case NETDEV_DOWN:   
957 fib_disable_ip(dev,0);   
958 break;   
959 case NETDEV_CHANGEMTU:   
960 case NETDEV_CHANGE:   
961 rt_cache Flush(0);   
962 break;   
963 }   
964 return NOTIFY_DONE;   
965 } 
```

对照代码，第一次调用时没有任何操作直接返回了，第二次调用时执行947行内容。for_ifa是一个for循环，对in_dev设备中所有的地址结构执行fib_add_ifaddr()函数，地址结构用in_ifaddr来封装，在fib_add_ifaddr()函数中调用fib_magic()将地址添加到路由表。fib_magic()函数将在后面过程分析，读者可以结合对这个函数的分析内容来理解fib_add_ifaddr()函数的代码。

ip_fib_init()函数最后调用fib_hash_init()来创建两个高速缓存slab。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ fib_hash_init()
void __init fib_hash_init(void)
{
    fn_hash_kmem = kmem_cache_create("ip_fib_hash", sizeof(struct fib_node),
        0, SLAB Panic, NULL);
    fnalias_kmem = kmem_cache_create("ip_fib alias", sizeof(struct fib Alias),
        0, SLAB Panic, NULL);
}

这里创建了两个高速缓存，一个用于fib_node结构使用，它是路由项结构；另一个用于fib alias结构使用，它是路由别名结构，这两个结构将在后面详细介绍。

我们再根据函数前边注释的路线向上返回到ip_rt_init()函数中。3068行代码初始化了一个rt_secret_timer定时器，接着启动了这个定时器，由它执行rt_secret_rebuild()函数定时对缓存冲刷。3082行的ip_rt_proc_init()函数在我们这个过程中是个空函数。

我们不关心 CONFIG_XFRM 处的内容，3088 行处调用了 ip_rt_init() 函数，它在 rtnl msghandlers 数组中登记了一个获得路由的 netlink，其 doit 函数是 inlet_rtm_getroute()。

再向上回到ip_init()函数，1411行调用了inet_initpeers()函数，它初始化了外部IP地址（相对于服务器来说是指客户端地址)的高速缓存。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ inlet_initpeers()
105 void __init inlet_initpeers(void)
106 {
107 struct sysinfo si;
108
109 /* Use the straight interface to information about memory. */
110 si_meminfo(&si);
111 /* The values below were suggested by Alexey Kuznetsov
112 * <kuznet@ms2.inr.ac.ru>. I don't have any opinion about the values
113 * myself. -- SAW
114 */
115 if (si.totalram <= (32768 * 1024) / PAGE_SIZE)
116 inletpeer_threshold >= 1; /* max pool size about 1MB on IA32 */
117 if (si.totalram <= (16384 * 1024) / PAGE_SIZE)
118 inletpeer_threshold >= 1; /* about 512KB */
119 if (si.totalram <= (8192 * 1024) / PAGE_SIZE)
120 inletpeer_threshold >= 2; /* about 128KB */
121
122 peer_cachep = kmem_cache_create("inet-peer_cache",
123 sizeof(struct inletpeer),
124 0, SLAB_HWCACHE Align|SLAB_PANIC,
125 NULL);
126
127 /* All the timers, started at system startup tend
128 to synchronize. Perturb it a bit.
129 */
130 peer-periodic_timer expires = jiffies
131 + net_random() % inletpeer_gc_maxtime
132 + inletpeer_gc_maxtime;
133 add_timer(&peer_periodic_timer);
134 }

我们跳过内存统计函数的调用和计算，这个函数122行处创建了一个名为inetpeer_cache的高速缓存并且设置了peer-periodic_timer定时器。inetpeer_cache用于缓存客户端IP的路由信息，而peer-periodic_timer定时器则为周期性回收这块缓存提供时间依据。

# 4.3 通过路由函数表查找路由信息

我们再沿着_inet_dev_addr_type()函数的过程中来看调用函数表的tb.lookup();在3.3节中介绍了它通过fib_get_table()获取了本地路由函数表，然后调用本地路由函数表的tb lookup(),结合fib_hash_table()函数的初始化内容，显然是执行fn_hash.lookup()函数。为便于对这个函数的分析，先简单的回忆一下_inet_dev_addr_type()函数的有关内容。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_addr_type() $\rightarrow$ __inet_dev_addr_type()  
static inline unsigned __inet_dev_addr_type(struct net \*net, const struct net_device \*dev, _be32 addr)   
{   
......   
local_table $=$ fib_get_table(net,RT_TABLE_LOCAL); if(local_table){ ret $=$ RTN_UNICAST; if(!local_table->tb.lookup(local_table,&fl,&res)){ if(!dev||dev $= =$ res-fi->fib_dev) ret $=$ res.type; fib_res_put(&res); }   
}   
return ret;

fib_get_table()查找本地路由函数表过程已经完成了，此时local_table指向了本地路由函数表。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_addr_type() $\rightarrow$ __inet_dev_addr_type() $\rightarrow$ fn_hash.lookup()  
247 static int   
248 fn_hash.lookup(struct fib_table \* tb, const struct flowi \* flp, struct fib_result \* res)   
249 {   
250 int err;   
251 struct fn-zone \* fz;   
252 struct fn_hash \*t $=$ (struct fn_hash\*)tb-> tb_data;   
253

```c
read_lock(&fib_hash_lock);   
for (fz = t-> fn-zone_list; fz; fz = fz-> fz_next) { struct hlist_head * head; struct hlist_node * node; struct fib_node *f; __be32 k = fz_key(flp->fl4.dst, fz); head = &fz-> fz_hash[fn_hash(k, fz)]; hlist_for_each_entry(f, node, head, fn_hash) { if (f-> fn_key != k) continue; err = fib_semantic_MATCH(&f-> fn Alias, flp, res, f-> fn_key, fz-> fz_mask, fz-> fz_order); if (err <= 0) goto out; } } err = 1;   
out: read_unlock(&fib_hash_lock); return err; 
```

先看一下传递进来的参数，tb是本地路由函数表结构，flp是包含服务器地址的路由键值结构，而res是用来返回路由查找结果的结构，最后返回给__inet_dev_addr_type()函数使用。我们在fib_hash_table()函数中看过，分配路由函数表时一起分配了fn_hash结构的空间，struct fn_hash是用来封装路由区fn-zone结构队列时使用的，这个结构的地址保存在路由函数表中的tb_data中，因此252行处可以从中取得这个结构指针t。

254行加锁以后便进入for循环，循环代码中 $\mathrm{flp}\longrightarrow \mathrm{fl4\_dst}$ 是取得路由键值内的服务器IP地址，fl4.dst在路由键值结构structflowi中是一个宏。

```txt
define f14.dst nl_u.ip4_u.daddr 
```

这种结构宏的方法简化了代码且便于调用。循环代码沿着fn-zone_list队列依次取得每一个路由区结构，然后对其调用fz_key()。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inet_bind() $\rightarrow$ inet_addr_type() $\rightarrow$ __inet_dev_addr_type() $\rightarrow$

fn_hash.lookup() $\rightarrow$ fz_key() static inline __be32 fz_key(_be32 dst, struct fn-zone \* fz) { return dst & FZ_MASK(fz); #define FZ_MASK(fz) ((fz) -> fz_mask)

FZ_MASK取得在路由区结构中的子网掩码，然后与服务器地址相“与”结果赋值给k；它是__be32类型，其实是无符号整型数值u32。在这个过程中服务器要绑定的IP地址是192.168.1.1，假设子网掩码是255.255.255.0，那么这里的计算结果k就是192.168.1.0，这也就意味着下边的查找路由代码会在子网192.168.1.0/24(255.255.255.0)之间。

261行fn_hash()将k(192.168.1.0)与子网掩码位数fz_order(24)在进行哈希，其结果作为fz_hash数组的下标，得到路由节点fib_node结构的队列链头head；然后从这个链头开始依次取出路由节点的子网地址fn_key与 $\mathbf{k}$ 相比较看是否相同，从而判断是否是同一个子网。关于这个路由节点结构的装载后面再来分析，只匹配子网地址还不够，还要调用fib_semantic-match()函数进行详细的检查，如果通过了检查，就会设置res返回给__inet_dev_addr_type()函数，再通过ret $=$ res.type获取服务器地址的类型。

fn_hash.lookup()函数嵌套了两个for循环，外层是对路由区队列的循环，内层是对路由节点队列的循环；代码沿着路由区结构队列对比每一个路由节点，找到同一子网的路由节点后接着调用fib_semantic_MATCH()函数，目前还没有看到路由节点是何时初始化的，我们先完成这里的检查函数再分析路由节点的初始化过程。

sys_SOCKETcall() $\rightarrow$ sys_bind() $\rightarrow$ inlet_bind() $\rightarrow$ inlet_addr_type() $\rightarrow$ __inet_dev_addr_type() $\rightarrow$ fn_hash.lookup() $\rightarrow$ fib_semantic_MATCH()

```c
869 int fib_semantic_MATCH(struct list_head *head, const struct flowi *flp,  
870 struct fib_result *res, __be32 zone, __be32 mask,  
871 int prefixlen)  
872 {  
873 struct fib alias *fa;  
874 int nh_sel = 0;  
875  
876 list_for_each_entry_rcu(fa, head, fa_list) {  
877 int err;  
878  
879 if (fa->fa_tos &&  
880 fa->fa_tos != flp->fl4_tos)  
881 continue; 
```

```txt
if (fa-> fa_scope < flp-> fl4_scope) continue;   
fa-> fa_state |= FA_S_ACCESSED;   
err = fibprops[fa-> fa_type].error; if (err == 0) { struct fib_info \*fi = fa-> fa_info; if (fi-> fib_flags & RTNH_F_DEAD) continue; switch (fa-> fa_type) { case RTN_UNICAST: case RTN_LOCAL: case RTN_BROADCAST: case RTN_ANYCAST: case RTN_ANYCAST: for_nexthops(fi) { if (nh-> nh_flags&RTNH_F_DEAD) continue; if (!flp-> oif || flp-> oif == nh-> nh_oif) break; }   
# ifdef CONFIG_IP-routeMULTIPATH if (nhsel < fi-> fib_nhs) { nh_sel = nhsel; goto out_fill_res; }   
# else if (nhsel < 1) { goto out_fill_res; }   
#endif endfor_nexthops(fi); continue; default; 
```

921 printf(KERN WARNING "fib_semantic_MATCH bad type \#x\n",   
922 fa->fa_type);   
923 return -EINVALID;   
924 }   
925 }   
926 return err;   
927 }   
928 return 1;   
929   
930 out_fill_res:   
931 res->prefixlen $=$ prefixlen;   
932 res->nh_sel $=$ nh_sel;   
933 res->type $=$ fa->fa_type;   
934 res->scope $=$ fa->fa_scope;   
935 res->fi $=$ fa->fa_info;   
936 atomic_inc(&res->fi->fib_clntref);   
937 return 0;   
938 }

872行出现了一个新的数据结构fib alias，从字面翻译是路由别名的意思，它是为了区别不同路由至同一个子网而使用。

# 代码清单4.5 fib alias结构定义

```c
struct fib Alias {
    struct list_head fa_list; //链入到路由节点 fib_node 中
    struct fib_info *fa_info; //路由信息结构保存着如何处理数据包
    u8 fa_tos; //服务类型 TOS
    u8 fa_type; //路由类型
    u8 fa_scope; //路由范围
    u8 fa_state; //状态标志
    #ifdef CONFIG_IP_FIBTRY
    struct rcu_head rcu;
    #endif
};
```

路由节点结构 fib_node 中有一个路由别名队列 fn Alias，从 fn_hash.lookup 传递下来的正是路由节点的 fn Alias 队列指针，这里的循环是沿着这个队列头来进行的，由此可以看出 fib Alias 结构通过 fa_list 链入到路由节点中。

我们再看路由节点结构 fib_node 的定义，这个结构代表一个路由所指向的子网，不同的路由虽然也可指向同一个子网，但是它们的路由信息不同。

# 代码清单 4.6 fib_node 结构定义

```solidity
struct fib_node {
    struct hlist_node fn_hash; //链入到哈希表的节点
    struct list_head fn Alias; //路由别名队列
    __be32 fn_key; //子网地址
    struct fib Alias fn EmbeddedAlias; //分配路由节点空间的时候也同时分配一个路
    //由别名结构空间，因此称之为“嵌入的”；
}
```

fib_semantic_MATCH()函数中876行依次取出挂入到路由节点中的fib alias，首先是检查fib alias的fa_tos，即路由别名的服务类型是否设置了；如果设置就检查是否与键值中的服务类型fl4_tos相同，fl4_tos也是一个结构宏。

```txt
define fl4_tos nl_u.ip4_u.tos 
```

接着就查看路由别名是否定义了路由范围，如果键值中的路由范围大于它，则循环下一个路由别名结构；接着为路由别名结构状态标志增加FA_S_ACCESSED，表示已经访问了。888行从全局的fib_propss数组中取得错误码，这个数组定义了路由表的范围和错误码。

```c
static const struct
{
    int error;
    u8 scope;
} fib_props[RTN_MAX + 1] = {
    .error = 0,
    .scope = RT_SCOPE NOWHERE,
} /* RTN_UNSPEC */
    {
        .error = 0,
        .scope = RTscopesUNIVERSE,
} /* RTN_UNICAST */
    {
        .error = 0,
        .scope = RTscopes_HOST,
} /* RTN_LOCAL */
    {
        .error = 0,
        .scope = RTscopesLINK,
} /* RTN_BROADCAST */
} 
```

```txt
. error = 0,
		.scope = RT_SCHEME LINK,
\}, /* RTN_ANYCAST */
\{
		-error = 0,
		.scope = RT_SCHEME_UNIVERSE,
\}, /* RTN Multicast */
\{
		-error = -EINVALID,
		.scope = RT_SCHEME_UNIVERSE,
\}, /* RTN 黑ACKHOLE */
\{
		-error = -EHOSTUNREACH,
		.scope = RT_SCHEME_UNIVERSE,
\}, /* RTN UNREACHABLE */
\{
		-error = -EACCES,
		.scope = RT_SCHEME_UNIVERSE,
\}, /* RTN PROHIBIT */
\{
		-error = -EAGAIN,
		.scope = RT_SCHEME_UNIVERSE,
\}, /* RTN THROW */
\{
		-error = -EINVALID,
		.scope = RT_SCHEME NOWHERE,
\}, /* RTN NOWHERE */
\}; 
```

以路由别名的fa_type，即路由类型为下标取出数组中的错误码err，如果错误码是0则表示支持该路由类型，就从路由别名的fa_info指针处取得struct fib_info结构指针fi。

# 代码清单4.7 fib_info结构定义

```c
struct fib_info {
    struct hlist_node fib_hash; //通过fib_hash和fib_lhash链入到两个哈希表中
    struct hlist_node fib_lhash;
```

```c
struct net *fib_net; //所属网络空间
int fib_treref; //路由信息结构的使用计数器
atomic_t fib_clntref; //是否释放路由信息结构的计数器
int fibdead; //标志着路由被删除了
unsigned fib_flags; //标志位
int fib_protocol; //安装路由协议
__be32 fib_prefix; //指定的源IP地址，源地址是与目标地址组成一个路由
u32 fibpriority; //路由的优先级
u32 fib_metrica[RTAX_MAX]; //用来保存负载值，包括MTU和MSS等内容
#define fib_mtu_fib_metrica[RTAX_MTU-1] //MTU值
#define fib_window_fib_metrica[RTAX_WINDOW-1] //窗口值
#define fib_rtt_fib_metrica[RTAX_RTT-1] //RTT值
#define fib_advmss_fib_metrica[RTAX_ADVMSS-1] //对外公开的MSS值
int fib_nhs; //跳转结构fib nh的长度
#ifdef CONFIG_IP-route_MULTIPATH
int fib_power; //支持多路径时使用
#endif
struct fib nh fib nh[0]; //下一个跳转结构
#define fib_dev fib nh[0].nh_dev;
};
```

这个结构代表一个路由，它反映了一条路由的信息，路由别名结构fa_info指向这个结构。

890行从路由别名结构的fa_info中取得路由信息结构指针fi，接下来要检查fi是否正处于被删除状态，这要根据它的fib_flags标志中RTNH_F_DEAD来决定。

895行则根据路由别名记录的路由类型来执行switch语句，RTN_UNICAST表示单播路由类型，RTN_LOCAL表示回环的路由(本地转发)类型，RTN_BROADCAST表示广播的路由类型，RTN_ANYCAST表示任意路由类型，RTN_MULTICAST表示组播(多播)的路由类型。

对这些路由类型都是使用相同的处理方法，可以看到 $896\sim 900$ 行之间并没有break，所以都会执行RTN MULTICAST标号处的代码，先是调用了for_next hops宏，内核中根据是否设置了多路径CONFIG_IPROUTE_MULTIPATH定义了两个同名宏，这里只看未打开该功能的宏定义。

```lisp
define for_nexthops(fi) { int nhsel = 0; const struct fib_nh * nh = (fi) -> fib_nh;  
for (nhsel = 0; nhsel < 1; nhsel++) 
```

这个宏从 fib_info 结构中获取 fib nh 路由跳转结构指针，一个 fib nh 结构代表着一次路由跳转的内容。

# 代码清单 4.8 fib nh 结构定义

```c
struct fib_nh {
    struct net_device *nh_dev; //指向网络设备结构
    struct hlist_node nh_hash; //链入到路由设备队列的哈希节点
    struct fib_info *nh_parent; //指向包含这个跳转的路由信息结构
    unsigned nh_flags; //跳转标志位
    unsigned char nh_scope; //路由的跳转范围,以此确定下一个跳转
    # ifdef CONFIG_IP-routeMULTIPATH
        int nh_weight; //跳转压力
        int nh_power; //跳转能力
    #endif
    # ifdef CONFIG_NET_CLSROUTE
        __u32 nh_tclassid;
    #endif
        int nh_oif; //发送设备的ID
        __be32 nh gw; //网关的IP地址
};
```

fib nh 是一个描述路由跳转信息的结构，每一次跳转都要用这个结构来表示。我们结合宏 for_next hops 的定义来看 901 行处的代码。

```txt
for_nexthops(fi) { if(nh->nh_flags&RTNH_F_DEAD) continue; if(!flp->oif||flp->oif == nh->nh_oif) break; } 
```

这段代码是循环检查路由中的跳转结构是否处于移除状态，跳转结构如果正在移除过程中，则其nh_flags标志中应设置RTNH_F_DEAD位。如果路由键值没有定义oif即没有指定发送设备，或者指定的发送设备与目标跳转结构中的nh_oif相同，即同一个设备时就找到了目标跳转结构，则此时跳出循环。

跳出循环后还要查看一下跳转的次数然后跳转到 out_fill_res 标号处，设置用返回的 res 结构，包括代表子网掩码位数的参数 prefixlen，它传递下来的是路由区的 fz_order，也设置了路由跳转的次数 nh_sel 以及路由的范围和类型，并且将路由信息指针也记录到了 res 中。根据前面进行的分析路线这里最需要关注的是 933 行处的代码“res->type = fa->fa_type”，它将路由类型记录到了 res->type 中。

从这个函数层层返回到__inet_dev_addr_type()中，最终将取得的res->type作为返回

值传给inet_bind()函数。

我们接着探索一下代码中出现的路由节点结构、路由别名结构、路由信息结构以及路由跳转结构的初始化过程。

# 4.4 路由的设置及相关结构的初如化

路由的相关结构是在“外力”和通知链设置路由时初始化的。“外力”是指用户使用工具或者路由命令。用户可以通过 net - tools 和 IPROUTE2 来设置的路由，一般来说 net - tools 不能设置 Multipath 多路径路由和 Policy Routing 策略路由这样的高级路由功能；而 IPROUTE2 是内核最新且强大的设置路由的方法，主要是一系列 IP 命令，并且 IPROUTE2 可以兼容 net - tools。

本书将从3条设置路线分析路由设置的过程，它们分别是路由设置路线A、B、C。

# 1. 路由设置路线A

net - tools 通过 ioctl 系统调用进入 ip_rt_ioctl() 函数, 它通过路由函数表的钩子函数实现增加或者删除路由。

```c
ioct1() -> sys_ioctl() -> do_vfs_ioctl() -> vfs_ioctl() -> sock_ioctl() -> net_ioctl() -> ip_rt_ioctl()
int ip_rt_ioctl(struct net *net, unsigned int cmd, void __user *arg) {
    ...
    ...
    if (cmd == SIOCDELRT) {
        tb = fib_get_table(net,_cfg.fc_table);
        if (tb)
            err = tb-> tb_delete(tb, &cfg);
        else
            err = -ESRCH;
    } else {
        tb = fib_new_table(net,_cfg.fc_table);
        if (tb)
            err = tb-> tb_insert(tb, &cfg);
        else
            err = -ENOUBFS;
    }
} 
```

代码中根据命令参数 cmd 分别调用指定路由函数表的 tb_insert()和 tb_delete()两个钩

子函数,这里省略了其他内容。在初始化路由函数表的 fib_hash_table()函数中曾经对这两个钩子函数进行了设置。

```txt
tb-> tb_insert = fn_hash_insert; 
```

```sql
tb-> tb_delete = fn_hash_delete; 
```

增加路由须进入fn_hash_insert()函数，删除路由则进入fn_hash_delete()函数，这是net-tools的设置路线，我们将在后面阅读这两个函数。

# 2. 路由设置路线 B

再看IPROUTE2设置路线，它设置路由的方式主要是通过netlink来实现的，它通过netlink调用inet_rtm_newroute()函数增加路由或者inet_rtm_delroute()函数删除路由。前面ip_fib_init函数中已经分析了这两个函数是如何登记到内核的，现在分析它们的执行过程。

static int inlet_rtm_newroute(struct sk_buff \*skb,struct nlmsghdr \*nlh,void \*arg)   
{ struct net \*net $=$ sock_net(skb->sk); struct fib_configcfg; struct fib_table \*tb; int err; err $=$ rtm_to_fib_config(net,skb,nlh,&cfg); if (err<0) goto errout; tb $=$ fib_new_table(net,cfg.fc_table); if(tb $= =$ NULL）{ err $=$ -ENOUBFS; goto errout; } err $=$ tb->tb_insert(tb,&cfg); errout: return err;   
} static int inlet_rtm_delroute(struct sk_buff \*skb,struct nlmsghdr \*nlh,void \*arg) { struct net \*net $=$ sock_net(skb->sk); struct fib_configcfg; struct fib_table \*tb; int err; err $=$ rtm_to_fib_config(net,skb,nlh,&cfg); if(err<0)

goto errout; tb $=$ fib_get_table(net,cfg.fc_table); if(tb $\equiv =$ NULL）{ err $\equiv$ -ESRCH; goto errout; } err $\equiv$ tb->tb_delete(tb,&cfg); errout: return err; 1

从代码中可看出，它们也像net - tools那样调用tb->tb_insert()和tb->tb_delete()，增加路由进入fn_hash_insert()，删除路由则进入fn_hash_delete()。可以看出，内核对路由的设置最终都集中在这两个钩子函数上。

# 3. 路由设置路线C

第三条路由设置路线是指通知链的方式。前面曾经在ip_fib_init()函数中介绍过内核注册了两个通知节点。

```txt
register_netdevice.notifier(&fib_netdev.notifier);  
register_inetaddr.notifier(&fib_inetaddr.notifier); 
```

这两个通知节点分别被插入到 netdev_chain 链（网络设备状态变动时通知链）和INETaddr.chain 链（地址变动时的通知链）；当网络设备安装或者初始化时，以及 IP 地址改变时都会触发通知链的处理函数。只不过这次操作的地址通知节点是 fib_inetaddr_event。

```c
static struct notifier_block fib_inetaddr.notifier = {notifier_call = fib_inetaddr_event,}; 
```

从notifier_call的内容来看，内核在IP地址发生变化会时会执行fib_inetaddr_event()通知函数。

（对两个通知链 netdev_chain 和INETaddr_chain 均调用了节点的 fib_netdev_event()和fib_inetaddr_event()这两个函数），这两个函数都调用了fib_add_ifaddr()来添加地址到路由中，只不过fib_inetaddr_event()函数中增加了对fib_del_ifaddr()函数的调用，从而使之可以删除路由地址。

无论是fib_add_ifaddr()函数还是fib_del_ifaddr()函数，它们都是调用fib_magic()函数来实现对路由地址的操作。

fib_inetaddr_event() $\rightarrow$ fib_magic()与fib_netdev_event() $\rightarrow$ fib_magic()

```c
674 static void fib_magic(int cmd, int type, __be32 dst, int dst_len, struct in_ifaddr * ifa) 
```

675{   
676 struct net \*net $=$ dev_net(ifa-> ifa_dev->dev);   
677 struct fib_table \*tb;   
678 struct fib_configcfg $=$ //路由配置结构   
679 .fc_protocol $=$ RTPROT_KERNEL,   
680 .fc_type $=$ type,   
681 .fc.dst $=$ dst,   
682 .fc.dst_len $=$ dst_len,   
683 .fc_prefix $=$ ifa-> ifa_local,   
684 .fc_oif $=$ ifa-> ifa_dev-> dev-> ifindex,   
685 .fc_nflags $=$ NLM_F_CREATE|NLM_F_APPEND,   
686 .fc_ninfo $=$ {   
687 .nl_net $=$ net,   
688 },   
689 };   
690   
691 if (type $= =$ RTN_UNICAST)   
692 tb $=$ fib_new_table(net,RT_TABLE_MAIN);   
693 else   
694 tb $=$ fib_new_table(net,RT_TABLE_LOCAL);   
695   
696 if (tb $= =$ NULL)   
697 return;   
698   
699 CFG.fc_table $=$ tb-> tb_id;   
700   
701 if (type！ $=$ RTN_LOCAL)   
702 CFG.fc_scope $=$ RT_SCOPELINK;   
703 else   
704 CFG.fc_scope $=$ RTscopes_HOST;   
705   
706 if (cmd $= =$ RTM NewlyROUTE)   
707 tb-> tb_insert(tb,&cfg);   
708 else   
709 tb-> tb_delete(tb,&cfg);   
710 }

函数中的代码对读者来说不难理解，这里只介绍关键处。678行准备了一个路由配置结构cfg，然后记录下路由函数表的ID并确定路由的范围。706行根据命令参数cmd的内容调

用tb->tb_insert()和tb->tb_delete()。

由此看出，通知链路线与前边两条路线走到了一起。根据已经介绍的路由函数表内容，增加路由调用fn_hash_insert()，删除路由则进入fn_hash_delete中()，这两个函数是内核配置路由的入口。

上面3条路线中多次出现了建立路由函数表的fib_new_table()函数，内核中有两个同名函数，它根据CONFIG_IP_multiple_TABLES多个路由函数表选项确定使用哪一个函数，在此对比两个函数的不同，先看启用多个路由函数表的函数。

```c
struct fib_table *fib_new_table(struct net *net, u32 id) {
    struct fib_table *tb;
    unsigned int h;
    if (id == 0)
        id = RT_TABLE_MAIN;
    tb = fib_get_table(net, id); //查找路由函数表
    if (tb)
        return tb;
    tb = fib_hash_table(id); //创建路由函数表
    if (!tb)
        return NULL;
    h = id & (FIB_TABLE_HASHSZ - 1);
    hlist_add_head_rcu(&tb->tb_hlist, &net->ipv4.fib_table_hash[h]); //链入哈希队列
    return tb;
} 
```

出现的几个函数前面已经看过了，这里先判断路由函数表id，如果没有指定，则默认为主路由函数表。然后通过fib_get_table()来查找主路由函数表，没有找到则说明内核还没有初始化该函数表，于是调用fib_hash_table()来创建它。创建没有成功，则直接返回；如果创建成功，则链入到内核的哈希队列中，hlist_add_head_rcu()函数也在前面分析了。

再看不使用多路由函数表选项的函数：

```c
static inline struct fib_table *fib_new_table(struct net *net, u32 id)  
{  
    return fib_get_table(net, id);  
} 
```

代码只是调用 fib_get_table()函数查找路由函数表。

现在来看3条路线调用的fn_hash_insert()函数，它担负着增加、更新、修改路由的作用。

373{   
374 struct fn_hash \*table $=$ (struct fn_hash \*) tb-> tb_data;   
375 struct fib_node \*new_f $=$ NULL;   
376 struct fib_node \*f;   
377 struct fibalias \*fa，\*new_fa;   
378 struct fn-zone \*fz;   
379 struct fib_info \*fi;   
380 u8tos $=$ cfg->fc_tos;   
381 __be32key;   
382 int err;   
383   
384 if（cfg->fcDst_len>32）//检查设置的IP地址长度   
385 return -EINVALID;   
386   
387 fz $=$ table->fn Zones[cfg->fcDst_len]； //取得对应路由区结构 //如果路由区不存在则创建一个   
388 if（!fz&&!(fz $=$ fn_newzone(table,cfg->fcDst_len))）   
389 return-ENOUBFS;   
390   
391 key $= 0$ 392 if（cfg->fcDst）{//是否设置了地址   
393 if（cfg->fcDst&~FZ_MASK(fz))   
394 return-EINVALID;   
395 key $=$ fz_keycfg->fcDst,fz); //确定子网键值   
396 }   
397   
398 fi $=$ fib_create_infocfg); //查找或者创建路由信息结构

由于函数较长我们分段来看，前面讲述了在没有启动多通路选项的情况下，内核将会建立两个路由函数表，一个是本地路由函数表，另一个是主路由函数表。这里沿着前边服务器分析的路线，传递的第一个参数tb是本地路由函数表，第二个参数是路由配置结构cfg，这个结构在前边分析过程中反复出现过。

# 代码清单4.9 fib_config结构定义

```txt
struct fib_config {
    u8 fc.dst_len; //地址长度
    u8 fc_tos; //服务类型TOS
    u8 fc_protocol; //路由协议
    u8 fc_scope; //路由范围
```

u8 fc_type; //路由类型 $\text{一} \times 3$ bytes unused $\text{串}$ u32 fc_table; //路由函数表  
_be32 fc.dst; //路由目标地址  
_be32 fc_gw; //网关  
int fc_oif; //网络设备ID  
u32 fc_flags; //路由标志位  
u32 fcpriority; //路由优先级  
be32 fc_prefix; //指定的IP地址  
struct nlattr *fc_mx; //指向netlink属性队列  
structrtnexthop \*fc_mp; //配置的跳转结构队列  
int fc_mx_len; //全部netlink属性队列的总长度  
int fc_mp_len; //全部配置跳转结构的总长度  
u32 fc_flow;  
u32 fc_nlflags; //netlink的标志位  
structnl_info fc_nInfo; //netlink的信息结构

它是路由配置使用的数据结构，是从2.6版本内核新加入的结构体，方便了对路由参数的统一管理。可以看出，Linux内核对代码的设计更加规范化，对参数的划分更加结构化，这对于学习和掌握内核非常有益，这种划分更像代码编写规范。尽管参与改进Linux的程序员分布在世界各地，且没有明确的分工与协作，可是这种代码规范默契越来越得到加强，由此出现的类似于fib_config的数据结构使内核的开发效率极大提高。

许多中小型技术企业都是在总结经验后才发现代码规范的重要性，并开始向这种“整体化”的方向发展，甚至企业在引进人才时也提出了代码规范的考核，这种结构的规范化设计对于读者来说也是非常值得学习和借鉴的。

fib_config 的内容是在前边 3 条路线过程中设置的，那时并没有关注它的设置过程。在路线 A 中，它是在 ip_rt_ioctl() 调用 rtentry_to_fib_config() 函数时配置的；在路线 B 中，它是在 inlet_rtm_newroute() 调用 rtm_to_fib_config() 函数时配置的；在路线 C 中，它是在 fib_magic() 函数中创建并初始化的。

我们来看函数的过程，374行通过路由函数表的tb_data指针得到了代表路由区队列的fn_hash指针，然后通过路由配置结构参数cfg得到tos服务类型编码，它是个8位的无符号整型数值；fc.dst_len在这里记录的是子网掩码的位数，这里首先检查一下是否超过了32位，然后以它为下标在路由函数表的fn Zones[]数组中找到对应子网的路由区fz指针；接着对这个路由区结构进行检查，如果所在子网的路由区结构为空，就要通过fn_new-zone()函数来创建新的路由区结构。

fn_hash_insert() $\rightarrow$ fn_new-zone()   
206 static struct fn-zone\*   
207 fn_new-zone(struct fn_hash \*table,int z)   
208 {   
209 int i;   
210 struct fn-zone \* fz $=$ kzalloc(sizeof(struct fn-zone),GFP_KERNEL);//分配路由区结构   
211 if(!fz)   
212 return NULL;   
213   
214 if(z){ //检查子网掩码位数   
215 fz-> fz_divisor = 16; //默认哈希头数量为16   
216 } else{   
217 fz-> fz_divisor = 1;   
218 }   
219 fz-> fz_hashmask $=$ (fz-> fz_divisor - 1); //确定哈希头的掩码   
220 fz-> fz_hash = fz_hash_alloc(fz-> fz_divisor); //分配哈希头结构   
221 if(!fz->fz_hash){   
222 kfree(fz);   
223 return NULL;   
224 }   
225 fz-> fz_order = z; //记录子网掩码位数   
226 fz-> fz_mask $=$ inet.make_mask(z); //转换成子网掩码值   
227   
228 /\*递增子网掩码位数，以它为下标在队列中循环查找第一个不为空的路由区\*/   
229 for $(i = z + 1;i <   = 32;i + + )$ 230 if(table->fn Zones[i])   
231 break;   
232 write_lock_bh(&fib_hash_lock);   
233 if(i>32){   
234 /\*如果没有找到，新建的路由区应该是第一个路由区\*/   
235 fz-> fz_next $=$ table->fn-zone_list;   
236 table->fn-zone_list $=$ fz;   
237 } else{ //找到了，新建的路由区与它建立关联   
238 fz-> fz_next $=$ table->fnzones[i] -> fz_next;   
239 table->fnzones[i] -> fz_next = fz;   
240 }   
241 table->fn Zones[z] $=$ fz; //记录在队列中   
242 fib_hash_genid++;   
243 write_unlock_bh(&fib_hash_lock);

```txt
244 return fz;   
245 
```

注意，传递的参数 $z$ 是子网掩码的长度值（二进制数1的个数，如255.255.255.0的二进制1的个数是24）；参数table是路由区队列，它是从本地路由函数表的tb_data指针处获得的。代码210行调用kzalloc()来分配路由区结构的内存空间。kzalloc()函数也是调用kmalloc()通用高速缓存分配函数来完成任务，只不过它增加了__GFP_ZERO清零标志，表示分配的内存要自动清零。

$\mathrm{fz} \rightarrow \mathrm{fz\_hash}$ 用来记录哈希表队列头。220 行通过 fz_hash_alloc()函数根据 fz_divisor 指定的哈希表个数分配空间，最大分配数量为 16，它是根据子网掩码的位数确定的。225 行和 226 行记录下子网掩码位数和子网掩码值，子网掩码值是通过 inset.make_mask()转换而来。

229行循环在路由区队列中查找对应子网掩码位置处是否存在路由区结构，最终将新创建的路由区结构fz保存到数组中。

回到fn_hash_insert函数392行处，接下来检查cfg->fc.dst是否指定了IP地址，如果指定则通过fz_key()函数计算一个子网键值key，该函数将IP地址与子网掩码进行逻辑“与”运算。

```c
static inline __be32 fz_key(_be32 dst, struct fn-zone * fz)  
{  
return dst & FZ_MASK(fz);  
}
```

接着398行调用fib_create_info()函数创建一个路由信息结构fi，该函数的代码接近200行，在/net/ipv4/fib_semantics.c文件中的687行处。

fn_hash_insert() $\rightarrow$ fib_create_info()   
687 struct fib_info \*fib_create_info(struct fib_config \*cfg)   
688 {   
689 int err;   
690 struct fib_info \*fi $=$ NULL;   
691 struct fib_info \*ofi;   
692 int nhs $= 1$ .   
693 struct net \*net $=$ _cfg->fc_ninfo.nl_net;   
694   
695 /\*检查指定的范围\*/   
696 if (fibprops[cfg-> fc_type].scope >cfg-> fc_scope)   
697 goto err INVALID;   
698   
699 #ifdef CONFIG_IP-routeMulTIPATH

```c
700 if (cfg-> fc_mp) {  
701 nhs = fib_count_nextdropscfg->fc_mp,cfg->fc_mp_len);  
702 if (nhs == 0)  
703 goto err invalid;  
704 }  
705 #endif  
706  
707 err = -ENOUBFS;  
708 if (fib_info_cnt >= fib_hash_size) {  
709 unsigned int new_size = fib_hash_size << 1;  
710 struct hlist_head *new_info_hash;  
711 struct hlist_head *new_laddrhash;  
712 unsigned int bytes;  
713  
714 if (!new_size)  
715 new_size = 1;  
716 bytes = new_size * sizeof(struct hlist_head *);  
717 new_info_hash = fib_hash_alloc(bytes);  
718 new_laddrhash = fib_hash_alloc(bytes);  
719 if (!new_info_hash || !new_laddrhash) {  
720 fib_hash_free(new_info_hash, bytes);  
721 fib_hash_free(new_laddrhash, bytes);  
722 } else  
723 fib_hash_move(new_info_hash, new_laddrhash, new_size);  
724  
725 if (!fib_hash_size)  
726 goto failure;  
727 } 
```

在前面设置路线C的fib_magic()函数中，指定路由类型cfg->fc_typ为RTN_LOCAL，这个值被初始化到结构变量cfg的fc_type中。而在路由设置路线A的ip_rt_ioctl()函数中，这个路由类型则是由struct rtentry rt结构来确定的。

结构 struct rtentry 用于路由命令 route 增加和删除路由，是 ioctl() 为 net - tools 工具准备的。这也是一个规范性的结构体，尤其是 ip_rt_ioctl() 函数要从用户空间 (net - tools 工具运行在用户空间) 将 net - tools 准备的 rtentry 结构复制到内核空间中来时，这个任务由复制函数 copy_from_user() 完成，ip_rt_ioctl() 函数调用 rtentry_to_fib_config() 来初始化 fib_config 配置结构，它根据 rtentry 结构中的 rt_flags 标志来确定cfg->fc_type 的值。

这里696行根据 $\mathrm{cfg}\longrightarrow$ fc_type类型值来检查指定的范围，fib_props数组定义了路由函

数表的范围和错误码，前面已经看过了这个数组的定义。如果内核开启了多通道路由选项，则还要检查cfg->fc_mp是否存在，它是rtnexthop结构指针，是由路由配置程序设置的一组结构，为了区别于内核的fib nh跳转结构定义，本书称它为“配置的跳转结构”。

# 代码清单 4.10 rstnexthop 结构定义

```txt
struct rtnexthop
{
    unsigned short rtnh_len; //指定的跳转结构长度
    unsigned char rtnh_flags; //指定的标志位
    unsigned char rtnh_hops; //指定的跳转次数
    int rtnh_ifindex; //指定的跳转设备
};
```

这个结构指定了路由过程中跳转结构的信息，这些信息将设置到内核的跳转结构 fib_nh 中。如果配置了“配置的跳转结构”，则进入 fib_count_next hops() 函数统计指定的跳转次数。

fn_hash_insert() $\rightarrow$ fn_new-zone() $\rightarrow$ fib_create_info() $\rightarrow$ fib_count_nexthops() static int fib_count_nexthops(struct rtnexthop $^*$ rtnh，int remaining) { int nhs $= 0$ while（rtnh.ok(rtnh,remaining）{ nhs++； rtnh $=$ rtnh_next(rtnh,&remaining); } /*如果参数remaining仍大于0，则丢弃跳转统计结果*/ return remaining $>0?0$ ：nhs;

函数在一个 while 循环调用 rtnh.ok() 检查路由配置结构中的 fc_mp_len, 它记录着配置的跳转结构的总长度, 其作为参数 remaining 同时传递给 rtnh.ok() 函数。

fn_hash_insert() $\rightarrow$ fn_new-zone() $\rightarrow$ fib_create_info() $\rightarrow$ fib_count_nextthops() $\rightarrow$ int rtnh.ok()   
static inline int rtnh ok (const struct rtnexthop \*rtnh, int remaining)   
{ return remaining $> =$ sizeof( \*rtnh) && rtnh-> rtnh_len $> =$ sizeof( \*rtnh) && rtnh-> rtnh_len $<   =$ remaining;   
} static inline struct rtnexthop \*rtnh_next (const struct rtnexthop \*rtnh, int \*remaining)   
{

```c
int totlen = NLAALIGN(rtnh->rtnh_len); //取得rtnexthop结构的长度  
*remaining = totlen; //减少长度范围  
return (struct rtnexthop *) ((char *) rtnh + totlen); 指向下一个rtnexthop结构
```

只要“配置的跳转结构”在要求的长度范围内，则就会继续统计并且对nhs计数器进行累加，最终形成跳转次数，然后在fib_create_info()函数中记录下统计结果。

在内核中有一个全局变量 fib_info_cnt 用于路由信息结构的计数，还有一个全局变量 fib_hash_size 用于路由信息结构队列 fib_info_hash 长度的计数。如果路由信息结构的数量已经达到或者超过了队列长度，则就需要扩充了。内核所有的路由信息结构都要链入到这个队列中，现在既然数量超过了限额就需要调整队列的长度。709 行先使用 fib_hash_size 扩大一倍的长度数 new_size，然后调用 fib_hash_alloc 函数分配两个哈希队列的内存空间，空间大小是字节，它已经换算成了全部队列头所需的空间长度。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_hash_alloc()   
static struct hlist_head \*fib_hash_alloc (int bytes)   
{ if(bytes $<   =$ PAGE_SIZE) return kzalloc(bytes,GFP_KERNEL); else return(struct hlist_head\*) __get_free_pages(GFP_KERNEL|__GFP_ZERO, get_order(bytes));   
}

首先判断分配空间是否小于一个内存页面的大小，即 $4\mathrm{KB}$ ，小于一个页面调用kzalloc()则按实际大小分配，多于一个页面则调用__get_free_pages()按页面分配内存；不过要先通过get_order()根据字节计算出所需要的页面数。

在 fib_create_info()函数中717行和718行共调用了两次分配函数，并且分别由 new_info_hash 和 new_laddrhash 两个哈希表队列头指向了这两个新分配的空间，如果两次都分配成功就会调用 fib_hash_move()将全部的 fib_info 路由信息从旧的哈希表队列中摘下，然后链入到两个刚刚创建的哈希表队列中。

图4.4中的fib_info_hash和fib_info_laddrhash两个哈希队列用于管理路由信息结构，但它们的作用有些不同。内核中所有的路由信息结构都会链入到fib_info_hash队列中便于内核查找，当设置了路由的地址时，对应的路由信息结构才会链入fib_info_laddrhash队列中。这里结合上图来看移动函数fib_hash_move()。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_hash_move()   
631 static void fib_hash_move(struct hlist_head \*new_info_hash,   
632 struct hlist_head \*new_laddrhash,

![](images/20e7e29668ffd1307e941bdc815d55a4758eaf4bd6f09295b3c3648cb81648fb.jpg)  
图4.4 路由信息结构的组织管理

```c
633 unsigned int new_size)  
634 {  
635 struct hlist_head *old_info_hash, *old_laddrhash;  
636 unsigned int old_size = fib_hash_size;  
637 unsigned int i, bytes;  
638  
639 spin_lock_bh(&fib_info_lock);  
640 old_info_hash = fib_info_hash; //记录旧的哈希队列头  
641 old_laddrhash = fib_info_laddrhash;  
642 fib_hash_size = new_size;  
643  
644 for (i = 0; i < old_size; i++) { 
```

//将旧的fib_info_hash队列中所有的路由信息结构移动到新的队列中

645 struct hlist_head \*head $=$ &fib_info_hash[i];   
646 struct hlist_node \*node, \*n;   
647 struct fib_info \*fi;   
648   
649 hlist_for_each_entry_safe(fi,node,n,head,fib_hash){   
650 struct hlist_head \*dest;   
651 unsigned int new_hash;

hlist_del(&fi->fib_hash); //从旧队列中摘链  
new_hash = fib_info_hashfn(fi);  
dest = &new_info_hash[ new_hash];  
hlist_add_head(&fi->fib_hash,dest); //链入到新队列  
}  
全局的fib_info_hash队列头指针，指向新队列  
fib_info_hash = new_info_hash;  
for $(i = 0;i <   old\_ size;i + + )$ {  
struct hlist_head * lhead $=$ &fib_info_laddrhash[i];  
struct hlist_node \* node,\* n;  
struct fib_info\*fi;  
hlist_for_each_entry_safe(fi,node,n,lhead,fib_lhash){  
struct hlist_head \*ldest;  
unsigned int new_hash;  
hlist_del(&fi->fib_lhash); //从旧队列中摘链  
new_hash = fib_laddr_hashfn(fi->fibprefsrc);  
ldest $=$ &new_laddrhash[ new_hash];  
hlist_add_head(&fi->fib_lhash,ldest); //链入到新队列  
}  
全局的fib_info_laddrhash队列头指针，指向新队列  
fib_info_laddrhash = new_laddrhash;  
spin_unlock_bh(&fib_info_lock);  
bytes $=$ old_size \* sizeof(struct hlist_head \*);  
fib_hash_free(old_info_hash,bytes); //释放旧的队列空间  
fib_hash_free(old_laddrhash,bytes);

635 行声明了两个哈希队列头 old_info_hash 和 old_laddrhash, 分别用来记录旧的 fib_in-

fo_hash 和 fib_info_laddrhash 哈希头。644 行和 662 行循环移动路由信息结构到新队列，两个 for 循环内都内嵌了一个宏循环语句 hlist_for_each_entrySAFE。

```txt
define hlist_for_each_entry_safe(tpos, pos, n, head, member)  
for (pos = (head) -> first;  
pos && { n = pos-> next; 1; } &&  
{ tpos = hlist_entry(pos, typeof(* tpos), member); 1;});  
pos = n) 
```

代入传递参数转换一下，代码就清晰了。

for(node $=$ (head)->first;   
node&&({n $\equiv$ node->next;1;}）&& {fi $\equiv$ hlist_entry(node,typeof(*fi)，fib_hash);1}）;   
node $\equiv$ n)

注意，“{A;1}”这种用法表示执行前边语句A后返回1，也就是语句（{n = node->next; 1; }）表示如果n取得了下一个hlist_node节点头则条件结果为true，然后根据节点头指针取得路由信息结构fi，再将node指向下一个节点头。

最外层的循环依次取出两个队列中的哈希头，内层的宏循环从哈希头取得的节点头，穷尽同一队列上的全部节点头，期间不断地通过节点头获取路由信息结构指针fi。代码structhlist_head *head = &fib_info_hash[i]是取得哈希头hlist_head,再从哈希头内的first指针得到节点头hlist_node指针。

```c
struct hlist_head {
    struct hlist_node *first;
}; 
```

结构中只包括节点头hlist_node，注意节点头与路由节点完全不同。路由节点用fib_node结构表示；路由节点结构最前面也有一个节点头fn_hash，由它组织和管理路由节点队列，所有的节点头是通过它的next指针链接成队。

```txt
struct hlist_node {
    struct hlist_node * next, * * pprev;
}; 
```

循环中的每一个节点都与一个 fib_info 结构对应，fib_info 结构最前面也是节点头 fn_hash。同样用来链入到哈希数组中的，也就是路由结构通过 fib_hash 链入到 fib_info_hash 数组中的每一个队列中，hlist_head 指向队列的第一个节点头，因此两个 for 循环以此为路线将所有的 fib_info 结构从旧队列中摘链，链入到 new_info_hash 和 new_laddrhash 队列数组中，hlist_del() 函数完成摘链过程。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_hash_move() $\rightarrow$ hlist_del() //指向非空地址引发页面异常用来表示未初始的队列项 #define LIST_POISON1 ((void \*) 0x00100100) #define LIST_POISON2 ((void \*) 0x00200200) static inline void hlist_del(struct hlist_node $\ast$ n) { _hlist_del(n); $n - >$ next $=$ LIST_POISON1; $n - >$ pprev $=$ LIST_POISON2; } static inline void __hlist_del(struct hlist_node $\ast$ n) { struct hlist_node $\ast$ next $=$ n-> next; struct hlist_node $\ast \ast$ pprev $=$ n-> pprev; $\ast$ pprev $=$ next; if (next) next->pprev $=$ pprev; }

摘链以后，利用fib_info_hashfn()重新计算哈希值，根据该哈希值确定新队列中的位置，调用hlist_add_head()入链新哈希队列，从而挂入fib_create_info()函数中的new_info_hash和new_laddrhash队列数组中。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_hash_move() $\rightarrow$ hlist_add_head()   
static inline void hlist_add_head(struct hlist_node $^*$ n,struct hlist_head \*h)   
{ struct hlist_node \*first $=$ h-> first; n-> next $=$ first; if(first) first-> pprev $=$ &n-> next; h-> first $=$ n; n-> pprev $=$ &h-> first;

函数对队列的摘链和插入代码很简单，都是用指针完成的，这里就不详细解释了。

完成“搬家”后，接下来让 fib_info_hash 和 fib_info_laddrhash 指向了新队列，使它们成为新家的主人。这个过程中“搬家”的动作只是对 hlist_node 结构内的指针操作，并没有把路由信息结构整体移动，因此使用指针的效率是非常高的，避免了释放和分配内存。原来的队列数组还占用着空间需要调用 fib_hash_free() 函数释放它们，old_info_hash 和 old_laddrhash 记

录着原来的队列数组地址。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_hash_move() $\rightarrow$ fib_hash_free()   
static void fib_hash_free(struct hlist_head \*hash，int bytes)   
{ if(!hash) return; if(bytes $<   =$ PAGE_SIZE) kfreeHASH); else free_pages((unsigned long)hash，get_order(bytes));   
}

函数也是根据长度是否大于一个页面，或者使用kfree()按字节数或者free_pages()按页面释放。

回到 fib_create_info()函数中，现在已经调整了两个队列数组容量了，继续往下分析。

fn_hash_insert() $\rightarrow$ fib_create_info()

//为新路由信息结构分配空间

```c
729 fi = kzalloc(sizeof(*fi) + nhs * sizeof(struct fib_nh), GFP_KERNEL);  
730 if (fi == NULL)  
731 goto failure;  
732 fib_info_cnt++; //递增路由信息结构的计数器  
733 //初始化路由信息结构  
734 fi->fib_net = hold_net(net);  
735 fi->fib_protocol =cfg->fc_protocol;  
736 fi->fib_flags =cfg->fc_flags;  
737 fi->fibpriority =cfg->fcpriority;  
738 fi->fib sufsrc =cfg->fc sufsrc;  
739  
740 fi->fib_nhs = nhs;  
741 change Nexthops(fi) { //让所有跳转结构都来“结亲”  
742 nh->nh_parent = fi;  
743 } endfor Nexthops(fi)  
744  
745 if (cfg->fc_mx) { //如果指定了 netlink 的属性队列  
746 struct nlattr *nla;  
747 int remaining;  
748 //循环对取得每个属性结构  
749 nla_for_each_attr(nla,_cfg->fc_mx,_cfg->fc_mx_len, remaining) { 
```

int type $=$ nla_type(nla); //取得属性中记录的类型值 if(type){ if(type $\rightharpoondown$ RTAX_MAX) goto err invalid; //记录属性结构装载的数据地址 fi->fib_metricx[type-1] $=$ nla_get_u32(nla); 1 } #if (cfg-> fc_mp){//如果指定了“下一个跳转”结构队列 #ifdef CONFIG_IP-route Multipath err $=$ fib_get_nhs(fi,cfg->fc_mp,cfg->fc_mp_len,cfg); if(err! $= 0$ ） goto failure; ifcfg->fc_oif &&fi->fib_nh->nh_oif！ $=$ config->fc_oif) goto err invalid; ifcfg->fc_gw&&fi->fib_nh->nh_gw！ $=$ config->fc_gw) goto err invalid; #ifdef CONFIG_NET_CLS-route ifcfg->fc_flow&&fi->fib_nh->nh_tclassid！ $=$ config->fc_flow) goto err invalid; #endif #else goto err invalid; #endif } else{ struct fib_nh\*nh $=$ fi->fib_nh; nh->nh_oif $=$ CFG->fc_oif; nh->nh_gw $=$ config->fc_gw; nh->nh_flags $=$ config->fc_flags; #ifdef CONFIG_NET_CLSROUTE nh->nh_tclassid $=$ config->fc_flow; #endif #ifdef CONFIG_IP-route MULTIPATH nh->nh_weight $= 1$ #endif

```c
788 }   
789   
790 if (fibprops[cfg-> fc_type].error) {   
791 if (cfg-> fc gw ||cfg-> fc_oif ||cfg-> fc_mp)   
792 goto err invalid;   
793 goto link_it;   
794 } 
```

729 行先调用 kzalloc()函数在内存中为新路由信息结构 fi 分配空间，不过这次分配的大小连统计的跳转结构空间一起分配了。前面 701 行对跳转次数 nhs 进行了统计，nhs * sizeof(struct fib_nh) 就是全部跳转结构的总长度。

分配成功后还要增加 fib_info_cnt 的计数；然后是对这个 fib_info 结构进行初始化设置，包括所属网络空间 fib_net 指针、指定的 IP 地址以及优先级和协议等内容，而且把跳转次数也记录在其中。

741 行是一个针对跳转结构的循环宏。

```txt
define change_nexthops(fi) { int nhsel; struct fib_nh * nh;   
for (nhsel = 0, nh = (struct fib_nh*)(fi) -> fib_nh); nhsel < (fi) -> fib_nhs; nh++, nhsel++) #define endfor_nexthops(fi) 
```

内核中有几个相同的定义，它们根据CONFIG_IPROUTE_MULTIPATH多路径选项来调用其中之一。如果没有打开该选项，则循环将只能执行一次只获取一个跳转结构内容，这里假设内核已经设置了该选项。循环宏沿着路由信息中的fib nh跳转队列，依次对所有的fib nh结构设置其nh_parent为fi，即这里新的路由信息结构，使它们“结亲”。

745 行检查配置结构 fc_mx 指针是否不为空, 这个结构指向 struct nlattr 结构队列, 用于 netlink 属性。nattr 的意思是“netlink attributes”。

```c
struct nladdr
{
    __u16 nla_len; //属性包含数据在内的总长度
    __u16 nla_type; //属性类型
};
```

如果 fc_mx 存在的话，就会在 749 行调用循环宏 nla_for_each_attr。

```c
define nla_for_each_attr (pos, head, len, rem) \  
for (pos = head, rem = len;  
    nla.ok(pos, rem);  
    pos = nla_next(pos, & (rem)))  
static inline int nla_OK(const struct nlattr *nla, int remaining) 
```

{ return remaining $> =$ sizeof( $\ast$ nla) && nla-> nla_len $> =$ sizeof( $\ast$ nla) && nla-> nla_len $<   =$ remaining;   
} static inline struct nattr \*nla_next(const struct nlattr \*nla, int \*remaining) { int totlen $=$ NLAALIGN(nla-> nla_len); \*remaining- $= =$ totlen; return (struct nlattr \*) ((char \*) nla + totlen);

这里将宏代码调用的函数一起列出了，前面看过宏 fib_count_next hops 调用了 rtnh.ok 和 rtnh_next 函数，这里的循环宏与它如出一辙，其余作用是沿着配置结构 fc_mx 指向的 netlink 属性队列，依次取出 nlattr 结构后调用 nla_type() 函数取得属性类型；fc_mx_len 记录着整个队列的长度。

```c
static inline int nla_type(const struct nlattr \*nla)   
{ return nla-> nla_type & NLA_TYPE_MASK; 
```

然后以 netlink 的属性类型为下标初始化路由信息中的 fib.metrics 数组，在数组的相应位置记录下属性结构装载的数据地址。这个数据是紧跟在每一个 nattr 结构后面，处于 NLA_HDRLEN 处，装载数据地址是调用 nla_get_u32() 取得的。

```c
static inline u32 nla_get_u32(struct nladdr *nla)   
{ return \* (u32 \*) nla_data(nla);   
} static inline void \*nla_data(const struct nladdr \*nla) { return(char \*) nla + NLA_HDRLEN;   
} #define NLAALIGNTO 4 #define NLAALIGN(len) (((len) + NLAALIGNTO -1)&\~(NLAALIGNTO -1)) #define NLA HDRLEN ((int) NLA ALIGN(sizeof(struct nladdr))) 
```

760 行根据 fc_mp 所指向的 struct rtnexthop“配置的跳转结构”队列，调用 fib_get_nhs() 函数修改路由信息中的跳转结构 fib nh。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_get_nhs()

static int fib_get_nhs(struct fib_info *fi, struct rtnexthop *rtnh, int remaining, struct fib_config *cfg) { change_nexthops(fi) //依次取出每个跳转结构 int attrlen; if(!rtnh.ok(rtnh,remaining))//控制范围 return -EINVALID; nh->nh_flags $=$ (cfg->fc_flags& $\sim 0\mathrm{xFF})$ |rtnh->rtnh_flags; nh->nh_oif $=$ rtnh->rtnh_ifindex; nh->nh_weight $=$ rtnh->rtnh_hops +1; //检查属性结构后面是否还有内容 attrlen $=$ rtnh_attrlen(rtnh); if (attrlen>0){ struct nladdr \*nla，\*attrs $=$ rtnh_attrss(rtnh); //取得属性数据中的网关地址 nla $=$ nla_find(attrrs,attrlen,RTA_GATEWAY); nh->nh_gw $=$ nla?nla_get_be32(nla):0; #ifdef CONFIG_NET_CLS_ROUTel nla $=$ nla_find(attrrs,attrlen,RTA_FLOW); nh->nh_tclassid $=$ nla?nla_get_u32(nla):0; #endif } rtnh $=$ rtnh_next(rtnh,&remaining);//指向下一个“配置的跳转结构” } endfor_nexthops(fi); return 0;

其中，rtnh.ok()和rtnh_next()两个函数组合在一起，循环取得“配置的跳转结构”rt-nexthop；然后根据每一个“配置的跳转结构”的内容对路由信息中的fib nh跳转结构进行设置，这包括指定的标志位、指定的设备和跳转次数等内容。

403 行的 rtnh_attrlen()函数计算 rtnexthop 结构后面的附加内容长度 attrlen。这个内容就是 netlink 的属性结构 struct nattr。

```txt
static inline int rtnh_attrlen(const struct rtnexthop *rtnh) { 
```

```c
return rtnh->rtnh_len - NLAALIGN(sizeof(\*rtnh));   
}   
static inline struct nlattr \*rtnh_attributes(const struct rtnexthop \*rtnh)//获取下一个属性结构 //指针 return(struct nlattr\*)(（char\*)rtnh +NLAALIGN(sizeof(\*rtnh))）; 
```

405 行 rtnh_attributes()则是取得下一个属性结构指针 attrs。紧跟在 rtnexthop 后面可能不止一个属性结构，由 attrlen 决定其个数；然后调用 nla_find()函数以第一个结构指针为队列起点查找 RTA_GATEWAY 网关类型的属性结构。

```c
struct nlattr *nla_find(struct nlattr *head, int len, int attrtype) {
    struct nlattr *nla;
    int rem;
//循环在属性队列中查找attrtype类型的属性结构
    nla_for_each_attr(nla, head, len, rem)
        if (nla_type(nla) == attrtype)
            return nla;
    return NULL;
} 
```

找到以后就要取出网络属性结构装载的数据，其装载的数据是网关地址。fib_get_nhs()函数将取得的网关地址记录到跳转结构的nh gw中，函数将所有的“配置跳转结构”逐一设置到fib nh结构中；返回到fib_create_info()函数765行，要检查配置结构cfg中的网络设备是否与跳转结构记录的网络设备是同一个设备以及网关地址是否相同。

如果配置结构中 fc_mp 没有指定“配置的跳转结构”，那么 fib_create_info() 函数就在 777 行处只取得一个跳转结构 nh，然后记录下配置结构中的网关和网络设备 ID 等内容。接下来要检查使用的路由类型在 fibXE 组数中是否确定为错误（存在出错码），这时如果配置结构指定了网关，也指定了设备 ID 号，并指定了“配置跳转结构”就出错返回了。

再接着阅读 fib_create_info()函数余下的代码。

fn_hash_insert() $\rightarrow$ fib_create_info()  
796 if (cfg-> fc_scope > RT_SCOPE_HOST)  
797 goto errInvalid;  
798  
799 if (cfg-> fc_scope == RTscopes_HOST){//路由范围是否属本机范围  
800 struct fib_nh *nh = fi-> fib_nh;

/\*检查跳转次数和网关地址\*/ if $(\mathrm{nhs}! = 1||\mathrm{nh - > n h\_gw})$ goto err invalid; nh->nh_scope $=$ RTscopesNOWHERE;//修改跳转范围 nh->nh_dev $=$ dev_get_by_index(net,fi->fib_nh->nh_oif); err $=$ ENODEV; if (nh->nh_dev $= =$ NULL) goto failure; }else{//路由范围不是本机范围 change Nexthops(fi){ //检查每一个跳转地址的“合法性” if ((err $=$ fib_check_nhcfg,fi,nh）！ $= 0)$ / goto failure; }endfor_nexthops(fi) } if (fi->fib_prefix) {//如果指定了路由地址则检查其地址类型 if (cfg->fc_type！ $= =$ RTN_LOCALI I！cfg->fc_dst || fi->fib_prefix！ $= =$ CFG->fc_dst) if (inet_addr_type(net,fi->fib_prefix)! $= =$ RTN_LOCAL) goto err invalid; }   
link_it: if((ofi $=$ fib_find_info(fi)! $= =$ NULL）{//检查是否存在相同的路由信息结构 fi->fibdead $= 1$ free_fib_info(fi); offi->fib_treref++; return ofi;//如果存在旧的路由信息结构，将它作为使用对象 }   
//没有旧的路由信息结构，使用新建的 fi->fib_treref++; atomic_inc(&fi->fib_clntref); spin_lock bh(&fib_info_lock); hlist_add_head(&fi->fib_hash, &fib_info_hash[fib_info_hashfn(fi)]);//链入路由信息结构队列 if (fi->fib_prefix) {//如果指定了IP地址，还要链入另一个路由地址队列 struct hlist_head $\ast$ head;

head $=$ &fib_info_laddrhash[fib_laddr_hashfn(fi->fib_prefix)];hlist_add_head(&fi->fib_lhash,head);}change_nextdrops(fi）{//循环取得路由信息中的每一个跳转结构struct hlist_head $\ast$ head;unsigned int hash;if(!nh->nh_dev)//检查跳转结构是否指定了跳转设备continue;hash $=$ fib_devindex_hashfn(nh->nh_dev->ifindex);//确定设备的哈希值head $=$ &fib_info_devhash[hash]://在路由设备的哈希数组中找到队列头hlist_add_head(&nh->nh_hash，head)；//链入路由设备队列}endfor_nextdrops(fi)spin_unlock_bh(&fib_info_lock);return fi;err invalid:err $= -E\mathrm{INVAL}$ ：  
failure:if(fi){fi->fibdead $= 1$ //如果路由信息已经处于删除状态则释放它free_fib_info(fi);1return ERR_PTR(err);

796 行检查一下配置结构指定的范围参数值是否在要求以内，如果这个值是 RT_SCOPE_HOST，则说明要求在本机范围以内，就要调整跳转结构的内容；但是跳转次数大于 1 并且指定了网关地址就说明出错了。805 行调整路由跳转结构的范围为 RT_SCOPE NOWHERE，表达“就在本地，哪里也不去”的意愿。806 行则通过设备 ID 号找到网络设备指针并记录在跳转结构的 nh_dev 中，查找函数 dev_get_by_index() 的参数 ifindex 是跳转结构的 nh_oif，这是由配置传递cfg 设置的；net 也是在配置结构中传递过来的网络空间指针。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ dev_get_by_index()   
```c
struct net_device *dev_get_by_index(struct net *net, int ifindex) { 
```

```c
struct net_device * dev;
read_lock(&dev_base_lock);
dev = __dev_get_by_index(net, ifindex);
if (dev)
    dev_hold(dev);
read_unlock(&dev_base_lock);
return dev;
} 
```

这里略过关于锁的内容，其关键函数是__dev_get_by_index()，由它来找到网络设备结构，并增加结构的使用计数。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ dev_get_by_index() $\rightarrow$ _dev_get_by_index()

```c
struct net_device *__dev_get_by_index(struct net *net, int ifindex) {
    struct hlist_node *p;
    hlist_for_each(p, dev_index_hash(net, ifindex)) {
        struct net_device *dev = hlist_entry(p, struct net_device, index_hlist);
        if (dev->ifindex == ifindex)//比对设备号
            return dev;
    }
    return NULL;
} 
```

dev_index_hash()函数根据设备ID号取得网络设备的哈希队列头。

```c
static inline struct hlist_head \*dev_index_hash(struct net \*net，int ifindex)   
{ return&net->dev_index_head[ifindex&((1<<NETDEV_HASHBITS)-1)];
```

__dev_get_by_index()函数其实就是从网络命名空间的哈希队列中依次查找每个 net_device 结构,只要 net_device 结构的 ifindex 与指定的 ID 号相同就返回这个结构的指针,记录在跳转结构 nh->nh_dev 中。

如果不是本机范围就要循环检查路由信息中的全部跳转结构；由fib_check_nh()函数来执行检查任务，函数内容较为复杂因此分段来看。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_check_nh()

```c
521 static int fib_check_nh(struct fib_config *cfg, struct fib_info *fi,  
522 struct fib_nh *nh) 
```

{ int err; struct net \*net; net $=$ cfg->fc_ninfo.nl_net; if(nh->nh_gw）{//如果跳转结构指定了网关 struct fib_result res; #ifdef CONFIG_IP-routePERVASIVE if(nh->nh_flags&RTNH_F_PERVASIVE) return 0; #endif if(nh->nh_flags&RTNH_F_ONLINK){ struct net_device \*dev; if (cfg-> fc_scale $\rightharpoondown$ RT_SCOPE_LINK) return -EINVALID; if (inet_addr_type(net, nh->nh_gw)! = RTN_UNICAST) return -EINVALID; if ((dev $=$ __dev_get_by_index(net, nh->nh_oif)) == NULL) return -ENODEV; if(! (dev->flags&IFF_UP)) return -ENETDOWN; nh->nh_dev $=$ dev; dev_hold(dev); nh->nh_scale $=$ RT_SCOPE_LINK; return 0; } { struct flowi f1 $=$ {//准备路由键值用于查询网关的地址类型 .nl_u $=$ { .ip4_u $=$ { .daddr $=$ nh->nh_gw, .scope $=$ cfg->fc_scale+1, }, , oif $=$ nh->nh_oif, };

562 /\*It is not necessary,but requires a bit of thinking \*/   
563 if (fl.f14_scope $<$ RT SCOPE LINK)   
564 fl.f14_scope $=$ RTSCOPELINK;   
565 if ((err $=$ fibLOOKup(net,&fl,&res))！ $= 0$ //查询网关地址类型   
566 return err;   
567 }   
568 err $=$ -EINVALID;

函数527行依然是从配置结构中的nl_inf->nl_net处取得网络空间指针，这个nl_info结构描述了netlink的来源信息，是在路由设置路线B中设置的。

528行检查跳转结构的网关nh_gw，这是一个非常大的if语句段。如果指定了网关，则调用路由函数表对网关地址的“合法性”进行一系列的检查。532行先是检查跳转标志nh_flags，如果不支持跳转、不支持递归(RTNH_F_PERVASIVE标志置位)就要返回了。

如果存在 RTNH_F_ONLINK 标志，就表示不需要对跳转地址进行检测，一般用于路由通往虚拟设备时。对于这种情况就先检查一下是否在本地子网范围，然后调用 inlet_addr_type() 函数检查网关的地址是否属于 RTN_UNICAST 单播类型。

调用__dev_get_by_index()函数找到要使用的网络设备结构net_device，找到以后就要让跳转结构 $\mathrm{nh}\longrightarrow \mathrm{nh\_dev}$ 记录下网络设备结构指针，并通过dev_hold()增加其使用计数。

```c
static inline void dev_hold(struct net_device \*dev)   
{ atomic_inc(&dev->refcnt);//原子操作
```

调整跳转结构的范围值为RTscopeLINK后，开始入手网关地址的检查准备工作，先初始化路由键值结构fl，将跳转结构的nh_gw网关地址、nh_oif设备ID号以及范围值fcscope设置其中。接着将这个键值传递调用fibLOOKUP()函数查找路由函数表，并用res返回查询结果。内核根据是否启用了多路由函数表选项同时存在两个fibLOOKUP()函数，先看没有使用多路由函数表的代码。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_check_nh() $\rightarrow$ fibLOOKUP()   
static inline int fib 查 lookup(struct net \*net, const struct flowi \*flp, struct fib_result \*res)   
{ struct fib_table \*table; table $=$ fib_get_table(net, RT_TABLE_LOCAL); //查找本地路由函数表 if(!table->tb 查 lookup(table, flp, res)) return 0; table $=$ fib_get_table(net, RT_TABLE_MAIN); //查找主路由函数表

```c
if(!table->tbLOOKUP(table,flp,res)) return 0;   
return-ENETUNREACH; 
```

没有启用路由规则时仅限于两个路由函数表。地路由函数表和主路由函数表。前面已经看过了这两个路由函数表的初始化，这里也是调用fib_get_table()函数获取两个路由函数表指针，再通过table->tbLOOKUP进入到fn_hashLOOKUP()函数中查找设置网关的地址类型，其过程这里不再重复了。最终通过fn_hash LOOKUP()函数过程得到res值，它是对网关地址的查询结果，res.type记录着它的地址类型。

再来看第二个 fibLOOKUP(), 它在开启了多路由函数表的情况下使用。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_check_nh() $\rightarrow$ fibLOOKUP()   
int fibLOOKUP(struct net \*net, struct flowi \*flp, struct fib_result \*res)   
{ struct fib LOOK_arg arg $=$ {//定义查找变量 .result $=$ res, 1 int err; err $=$ fib/rules LOOKUP(net-> ipv4.rulesOps, flp, 0, &arg); res-> r = arg.rule; return err; }   
149 int fib RulesLookup(struct fib_rule ops \* ops, struct flowi \*fl,   
150 int flags, struct fib LOOK_arg \*arg)   
151 {   
152 struct fib_rule \* rule;   
153 int err;   
154   
155 rcu_read_lock();   
156   
157 list_for_each_entry_rcu(rule, &ops-> rules_list, list){   
158 jumped:   
159 if(!fib_rule_MATCH(rule, ops, fl, flags))   
160 continue;   
161 //检查动作标识，是否转到另一个规则   
162 if (rule->action $= =$ FR_ACT_GOTO){

163 struct fib_rule \* target;   
164   
165 target $=$ rcu_dereference(rule->ctarget);   
166 if(target $= =$ NULL){   
167 continue;   
168 } else{   
169 rule $=$ target;   
170 goto jumped;   
171 1   
172 }elseif(rule->action $= =$ FR_ACT_NOP)//如果没有指定任何动作继续下一个   
173 continue;   
174 else   
175 err $=$ ops->action(rule,fl,flags,arg);   
176   
177 if(err!=-EAGAIN){   
178 fib_rule_get(rule);   
179 arg->rule $=$ rule;   
180 goto out;   
181 1   
182 1   
183   
184 err $=$ -ESRCH;   
185 out:   
186 rcu_read_unlock();   
187   
188 return err;   
189

这个查找路由函数表的代码比较复杂，前面的fib4_rules_init()函数中有两处代码需要用到：

```javascript
ops = kmemdup(&fib4_rulesOPS_template, sizeof(* ops), GFP_KERNEL); net-> ipv4.rulesOps = ops; 
```

这里的 fibLOOKUP()函数用到 ops 规则函数表的内容，并且也用到 3 个 fib_rule 路由规则：本地路由规则、主路由规则、默认路由规则。这 3 个路由规则是 fib_default/rules_init() 函数调用 3 次 fib_default_rule_add()，将它们初始化并“安装”到 ops 的 rules_list 队列中的。除此之外，我们也可使用 ip rule 命令来设置路由规则，也是利用 IPROUTE2 工具来设置，读者可以在 Linux 中安装 IPROUTE2 之后通过 ip rule help 命令来查看命令说明。

在 fib_rule lookup()函数157行沿着这个路由规则队列调用fib_rule_MATCH()函数来查

找适用的路由规则。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_check_nh() $\rightarrow$ fibLOOKUP() $\rightarrow$ fib_rule LOOKUP() $\rightarrow$ fib_rule_MATCH()

static int fib_rule_match(struct fib_rule \* rule, struct fib_ruleOps \* ops, struct flowi \*fl, int flags)   
{ int ret $= 0$ if (rule->ifindex && (rule->ifindex != fl->iif))//检查设备ID goto out; if ((rule->mark~fl->mark)& rule->mark_mask)//掩码过滤 goto out; ret $=$ ops-> match(rule,fl,flags);   
out: return (rule->flags&FIB-rule_INVERT)?！ret：ret;

先对设备ID号和掩码进行比对，然后调用ops中的match()钩子函数，这里实际调用的是fib4_ruleOPS_template结构的match钩子函数。

```c
static struct fib_rulesOps fib4_ruleOps_template = {  
    .family = AF_INET,  
    .rule_size = sizeof(struct fib4_rule),  
    .addr_size = sizeof(u32),  
    .action = fib4_rule_action,  
    .match = fib4_rule_MATCH,  
    .configure = fib4_rule_config,  
    .compare = fib4_rule(compare,  
    .fill = fib4_rule_fill,  
    .default_prefix = fib4_rule_default_prefix,  
    .nlmsg_payload = fib4_rule_nlmsg payloads,  
    .flush_cache = fib4_rule Flush_cache,  
    .nlgroup = RTNLGRP_IPV4Rule,  
    .policy = fib4_rule_policy,  
    .owner = THISModule,  
}; 
```

对应处挂入的是fib4_rule_MATCH()函数，由它来执行进一步的比对任务。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_check_nh() $\rightarrow$ fibLOOKUP() $\rightarrow$ fib_rule LOOKUP() $\rightarrow$ fib_rule_MATCH() $\rightarrow$ fib4_rule_MATCH()

static int fib4_rule_match(struct fib_rule \* rule, struct flowi \*fl, int flags)   
{ struct fib4_rule \*r $=$ (struct fib4_rule \*) rule; _be32 daddr $=$ fl-> fl4.dst; _be32 saddr $=$ fl-> fl4_src; //比对源地址和目标地址 if((saddr\~r->src)&r->srcmask)|| ((daddr\~r->dst)&r->dstmask)) return 0; //比对服务类型码TOS if(r->tos&&(r->tos！=fl->fl4_tos)) return 0;   
return 1;

函数开始处先将路由规则转换成 fib4_rule 结构类型, 这是 IPV4 类型的路由规则; 然后从路由键值中取得目标地址 (前面已经设置为网关地址) 和源地址与路由规则进行比对, 如果两者检查都没有通过也可以, 只要求服务类型码 tos 一致, 比对成功了就确定了使用哪一个路由规则。

返回到 fib_rules.lookup() 函数 175 行, 再通过 ops-> action() 来执行 fib4/rulesOps_template 结构的动作函数 fib4_rule_action()。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_check_nh() $\rightarrow$ fibLOOKUP() $\rightarrow$ fib4_rule_action()

```c
70 static int fib4_rule_action(struct fib_rule \* rule, struct flowi \* flp,  
71 int flags, struct fibLOOKUP_arg \*arg)  
72 {  
73 int err = -EAGAIN;  
74 struct fib_table \*tbl;  
75  
76 switch (rule->action){//检查动作标识码  
77 case FR_ACT_TO_TBL;  
78 break;  
79  
80 case FR_ACT_UNREACHABLE;  
81 err = -ENETUNREACH;  
82 goto errout;  
83  
84 case FR_ACT_PROHIBIT; 
```

85 err $=$ -EACCES;   
86 goto errout;   
87   
88 case FR_ACTBLACKHOLE:   
89 default:   
90 err $=$ -EINVALID;   
91 goto errout;   
92 }   
93 //查找路由函数表   
94 if((tbl $=$ fib_get_table(rule->fr_net, rule->table)) $= =$ NULL)   
95 goto errout;   
96 //调用路由函数表的查找函数查询键值   
97 err $=$ tbl->tb.lookup(tb, flp, (struct fib_result \*) arg-> result);   
98 if (err>0)   
99 err $=$ -EAGAIN;   
100 errout:   
101 return err;   
102 }

首先是对路由规则的动作标识 rule $\rightarrow$ action 的检查。switch 语句只有一处 break, 动作标识为“使用指定的路由函数表”的是可以通过的, 其他标识都视为错误。前面在 fib_default_rule_add() 函数初始化路由规则时看到内核默认的 3 个路由规则都设置为“使用指定的路由函数表”标识。

最终函数也是调用fib_get_table()函数找到路由规则指定的路由函数表，这除了内核设置的本地路由函数表、主路由函数表、默认路由函数表这3个路由函数表外，还有用户通过net-tools或者IPROUTE2工具添加的自定义路由函数表。其中，net-tools利用iotcl()系统调用实现路由函数表的添加和删除，而IPROUTE2则利用netlink来对路由函数表操作，它们都是调用的fib_new_table()函数增加路由函数表，这个函数前面已经分析过了。

找到了路由函数表结构后，也要调用它的tbLOOKUP()函数查找网关地址的地址类型并记录到查询结构res中。

接着看 fib_check_nh()函数余下的代码。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_check_nh()  
569 if (res.type != RTN_UNICAST && res.type != RTN_LOCAL)//检查地址类型  
570 goto out;  
571 nh->nh_scope = resscopes;//调整跳转范围  
572 nh->nh_oif = FIB_RES_OIF(res);//调整网络设备  
573 if ((nh->nh_dev = FIB_RES_DEV(res)) == NULL)//网络设备不能为空

574 goto out;   
575 dev_hold(nh->nh_dev);//增加网络设备的计数器   
576 err $=$ -ENETDOWN;   
577 if(!(nh->nh_dev->flags&IFF_UP))//检查网络设备是否已经处于工作状态   
578 goto out;   
579 err $= 0$ .   
580 out:   
581 fib_res_put(&res);   
582 return err;   
583 } else{//如果跳转结构中没有指定网关   
584 struct in_device \*in_dev;   
585   
586 if(nh->nh_flags&(RTNH_F_PERVASIVE|RTNH_F_ONLINK))   
587 return -EINVALID;   
588   
589 in_dev $=$ inetdev_by_index(net,nh->nh_oif);//取得输入设备结构   
590 if(in_dev $= =$ NULL)//检查是否成功找到   
591 return -ENODEV;   
592 if(! (in_dev->dev->flags&IFF_UP)){//检查网络设备是否处于工作状态   
593 in_devPut(in_dev);   
594 return -ENETDOWN;   
595 }   
596 nh->nh_dev $=$ in_dev->dev;//记录到跳转结构中   
597 dev_hold(nh->nh_dev);//增加网络设备的使用计数   
598 nh->nh_scope $=$ RTscape_HOST;//调整跳转范围   
599 in_devPut(in_dev);   
600 }   
601 return 0;   
602 }

返回到 fib_check nh() 函数后接下来要对返回的类型值 res.type 进行检查, 只有一种情况是不允许的, 即如果网关地址不是 RTN_UNICAST 单播路由类型, 并且不是本地转发的路由类型 RTN_LOCAL。接下来根据查询结构 res 返回的范围值, 记录到跳转结构的 nh->nh_scope 中; 跳转结构的 nh_oif 记录网络设备的 ID; nh_dev 记录网络设备的结构内容, 这是使用 FIB_RES_OIF 和 FIB_RES_DEV 宏从 res 获取对应的内容。

```c
define FIB_RES_OIF(res) (FIB_RES_NH(res),nh_oif) 
```

```txt
define FIB_RES_DEV(res) (FIB_RES_NH(res).nh_dev) 
```

根据是否启用了多路由规则存在两个，第一个是没有启用路由规则的宏

```txt
define FIB_RES_NH(res) ((res).fi-> fib_nh[0]) 
```

另一个是启用多路由规则的宏

```txt
define FIB_RES_NH(res) ((res).fi-> fib_nh[(res).nh_sel]) 
```

查询结构 res 起到了跳板的作用, 可以通过 res 记录的路由信息 fi 找到网络设备的 ID 和网络设备的结构指针, FIB_RES_NH 宏根据多路由规则 (多路由函数表选项) 存在两个宏, 它们的不同之处在于路由信息是否有多个跳转结构, 没有启用路由规则时 fib_nh 数组中只有一个跳转结构, 它由第一个数组元素指向。启用路由规则时则要根据 nh_sel 跳转序号来找到对应的跳转结构, res.nh_sel 是在查找路由函数表的过程函数 fib_semantic_MATCH() 中设置的。

fib_check_nh()函数575行增加这个结构的引用计数，并且检查网络设备是否处于激活状态。至此对网关的查找都是基于配置结构设定的网关，这是在fib_create_info()函数中设置到跳转结构的：

```c
nh->nh_gw =cfg->fc_gw; 
```

接下来，583行是配置结构cfg没有设置网关数据的情况。

net_device结构是Linux内核全部网络协议的公用结构体，其结构内部的ip_ptr指针用于IPV4协议的专用设备结构in_device,in_device则用于dev与net_device彼此关联。589行调用inetdev_by_index()函数根据 $\mathrm{nh}\longrightarrow \mathrm{nh\_oif}$ 设备ID找到网络设备的in_device。592行通过in_dev->dev检查网络设备结构，检查这个设备是否处于激活状态，然后记录到跳转结构 $\mathrm{nh}\longrightarrow \mathrm{nh\_dev}$ 中。因没有指定网关所以将跳转结构中的 $\mathrm{nh}\longrightarrow \mathrm{nh\_scope}$ 设置为RT SCOPE_HOST本机范围。

完成对fib_check_nh()函数的分析之后，返回到fib_create_info()函数812行，这个循环中对路由信息的每一个跳转结构都调用fib_check_nh()函数执行上述的检查过程，尤其是针对网关的重点检查。路由信息结构中的fib predecessors代表指定的IP地址，这个值是在fib_create_info()从配置结构的fc predecessors设置的：

```txt
fi-> fib_prefix =cfg->fc_prefix; 
```

817 行实际是对配置结构 fc_prefix 的检查，也就是对用户设置的路由地址进行检查。如果配置结构中指定的类型 $\mathrm{cfg} \rightarrow$ fc_type 不是本地转发类型，或者目标地址 $\mathrm{cfg} \rightarrow$ fc.dst 是空，或者设置的路由地址与目标地址 $\mathrm{cfg} \rightarrow$ fc.dst 不同，就要调用 inset_addr_type() 函数中重新取得路由地址的类型，并再次检查查询到的地址类型是否是本地转发。inet_addr_type() 函数的过程已经在前面详细分析了。

825 行调用 fib_find_info()函数查看是否有相同的路由信息表。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ fib_find_info()

223{   
224 struct hlist_head \*head;   
225 struct hlist_node \*node;   
226 struct fib_info \*fi;   
227 unsigned int hash;   
228   
229 hash $=$ fib_info_hashfn(nfi);//计算路由信息的哈希值   
230 head $=$ &fib_info_hash[hash]://取得路由信息的队列头   
231 //沿着队列头循环在哈希队列中查找相同的路由信息结构   
232 hlist_for_each_entry(fi,node,head,fib_hash){   
233 if (fi->fib_net！ $= \mathrm{nfi - >}$ fib_net)   
234 continue;   
235 if (fi-> fib_nhs！ $= \mathrm{nfi - >}$ fib_nhs)   
236 continue;   
237 if (nfi-> fib_protocol $= =$ fi-> fib_protocol &&   
238 nfi->fib sufsrc $= =$ fi-> fib sufsrc &&   
239 nfi->fibpriority $= =$ fi-> fibpriority &&   
240 memcmp(nfi->fib_metric,fi->fib_metric, sizeof(fi->fib_metric)) $= = 0\& \&$ 242 ((nfi->fib_flags $\rightharpoonup$ fib_flags)&\~RTNH_F_DEAD) $= = 0\& \&$ 243 (nfi->fib_nhs $= = 0||$ nh_comp(fi,nfi） $= = 0)$ ）   
244 return fi;   
245 }   
246   
247 return NULL;   
248}

fib_info_hash 数组是全部路由信息结构的哈希队列。232 行循环在队列中查找相同的路由信息结构, 查找依据包括: 是否同属一个网络空间、是否跳转次数相同、是否具有相同的协议、是否具有相同的地址和优先级、是否 fib.metrics 数组中的负载值相同、是否具有相同的路由标志和跳转次数。如果这些对比都成功了, 则在队列中找到了相同的路由信息结构, 因而不需要使用新创建的路由信息结构, 就将它的 fibdead 标志为 1 , 表示已经不再使用要删除了。827 行调用 free_fib_info() 函数释放它占用的空间。

```c
fn_hash_insert() \(\rightarrow\) fib_create_info() \(\rightarrow\) free_fib_info()   
void free_fib_info(struct fib_info\*fi)   
{ if(fi->fibdead \(= = 0\) ）{ printf(KERN WARNING"Freeing alive fib_info \(\% p\n"，fi); 
```

return;   
} change_nextdrops(fi）{//先处理路由信息中的跳转结构 if $(\mathrm{nh - > n h\_dev})$ devPut(nh->nh_dev)；//递减网络设备的引用计数 nh->nh_dev $=$ NULL; }endfor_nextdrops(fi); fib_info_cnt--;//递减路由信息结构的计数器 release_net(fi->fib_net); kfree(fi);

释放时先循环处理路由信息结构中的跳转结构nh，如果它记录了网络设备 $\mathrm{nh}\rightarrow \mathrm{nh\_dev}$ 则递减对应设备的使用计数器；这个计数器是在前面的dev_hold()函数中递增的，devPut()是对dev_hold函数的反向操作。对全局的fib_info_cnt路由信息计数器也要递减，还要递减对网络空间的使用计数器，最后调用kfree()释放路由信息结构占用的空间。

fn_hash_insert() $\rightarrow$ fib_create_info() $\rightarrow$ free_fib_info() $\rightarrow$ dev_put()

```c
static inline void dev_put(struct net_device \*dev)   
{ atomic_dec(&dev->refcnt);   
}   
static inline void dev_hold(struct net_device \*dev)   
{ atomic_inc(&dev->refcnt); 
```

内核中的 atomic_dec()和 atomic_inc()是原子操作函数，可以防止其他 CPU 和系统中断抢占当前的操作。

回到 fib_create_info()函数826行处，既然找到了旧的路由信息结构，且释放了新建的路由信息结构，则直接使用旧的路由信息结构即可，因而将旧的路由信息ofi结构指针返回作为使用对象。

如果没有找到旧的路由信息结构，就要增加新创建 fi 的计数器，并将其链入到全局的哈希队列中，将 fi-> fib_hash 哈希链头链入到 fib_info_hash 数组中。操作之前使用了自旋锁 fib_info_lock，防止其他进程的干扰。

如果路由信息结构指定了 IP 地址，则将路由信息结构的 fi-> fib_lhash 链头挂入到路由地址队列数组 fib_info_laddrhash 中。注意，IP 地址是从配置结构cfg-> fc_prefix 记录到 fi-> fib_prefix 中的。

843行循环对路由信息结构中的跳转结构fib nh进行操作，循环取得每一个跳转结构后，检查是否记录了网络设备。然后根据设备ID计算一个哈希值，以这个哈希值为下标在路由设备队列数组fib_info_devhash中找到所属的队列头，将跳转结构链入到队列中。至此，fib_create_info()函数创建路由信息结构的使命就已完成，我们看到所谓的创建并不是绝对，也可以使用已存在的路由信息结构。

再向上返回到fn_hash_insert()函数399行处，接着看如何处理新创建的路由信息结构。

```c
399 if(IS_ERR(fi))//是否出错  
400 return PTR_ERR(fi);  
401 //检查路由数量和队列头数量的比例，是否需要调整路由区的队列头数量  
402 if (fz-> fz_nent > (fz-> fz_divisor << 1) &&  
403 fz-> fz_divisor < FZ_MAX_DIVISOR &&  
404 (cfg-> fc.dst_len == 32 ||  
405 (1 <<cfg->fc.dst_len) > fz-> fz_divisor))  
406 fn_rehash-zone(fz);  
407  
408 f = fib_find_node(fz, key);  
409  
410 if (!f)  
411 fa = NULL;  
412 else  
413 fa = fib_find Alias(&f->fnalias, tos, fi->fibpriority); 
```

先是检查新创建的 fi 是否有错误发生，402 行检查路由区中路由数量是否已经超过哈希头两倍，并且哈希头数量还有扩大的余地，这就需要调用 fn_rehash-zone() 函数调整路由区的队列头数量。

fn_hash_insert() $\rightarrow$ fn_rehash-zone()   
145 static void fn_rehash-zone(struct fn-zone \* fz)   
146 {   
147 struct hlist_head \*ht，\*old_ht;   
148 int old_divisor，new_divisor;   
149 u32 new_hashmask;   
150   
151 old_divisor $=$ fz-> fz_divisor;   
152   
153 switch(old_divisor）{//根据已有的队列头数量确定一个新值   
154 case 16:   
155 new_divisor $= 256$

break;   
case 256: new_divisor $= 1024$ break;   
default: //如果已有数量扩大一倍超过了系统的上限，则不能扩展了 if((old_divisor $<  1) > \mathrm{FZ\_MAX\_DIVISOR}$ { printk(KERN_CRIT"route.c: bad divisor %d！\n", old_divisor); return; } new_divisor $=$ (old_divisor $<  1$ );//默认情况下新数量是原来的一倍 break;   
}   
new_hashmask $=$ (new_divisor-1);//新的队列哈希掩码   
#if RT_CACHE_DEBUG $> = 2$ printk(KERN_DEBUG"fn_rehash-zone: hash for zone %d grows from %d\n", fz-> fz_order, old_divisor);   
endif   
ht $=$ fz_hash_alloc(new_divisor);//按照确定的新数量分配队列头空间   
if (ht){ write_lock_bh(&fib_hash_lock); old_ht $=$ fz-> fz_hash;//记录原来的队列头位置 fz-> fz_hash $=$ ht;//指向新的队列 fz-> fz_hashmask $=$ new_hashmask;//记录新的哈希掩码 fz-> fz_divisor $=$ new_divisor;//记录新的队列头数量 fn_rebuild-zone(fz, old_ht, old_divisor);//将所有路由节点链入到新队列 fib_hash_genid++;//递增队列计数器 write_unlock_bh(&fib_hash_lock); fz_hash_free(old_ht, old_divisor);//释放原来的队列头空间

这个函数先记录下路由区结构原来的队列头数量，这个值决定着路由区队列 fz_hash 的大小 $(\mathrm{fz} \rightarrow \mathrm{fz\_hash})$ 。153 行的 switch 语句根据原有值确定了一个新的队列头数量 new_divisor；之后在 176 行按新数量分配队列头空间，调整路由区的 $\mathrm{fz} \rightarrow \mathrm{fz\_hash}$ 指针使它指向新

创建的哈希队列；之后184行调用fn_rebuild-zone()函数将原来队列中的路由节点全部“搬家”至新队列。

fn_hash_insert() $\rightarrow$ fn_rehash-zone() $\rightarrow$ fn_rebuild-zone()   
static inline void fn_rebuild-zone(struct fn-zone \* fz, struct hlist_head \* old_ht, int old_divisor)   
{ int i; for $(\mathrm{i} = 0;\mathrm{i} <   \mathrm{old\_divisor};\mathrm{i} + + )$ { struct hlist_node \* node， \*n; struct fib_node \*f; //循环获取旧队列中的每一个路由节点 hlist_for_each_entry_safe(f,node,n,&old_ht[i],fn_hash){ struct hlist_head \* new_head; hlist_del(&f->fn_hash);//从旧队列中摘链 new_head $=$ &fz->fz_hash[fn_hash(f->fn_key,fz)];//确定在新队列中的位置 hlist_add_head(&f->fn_hash,new_head);//链入新队列 } 1

函数中的循环代码将路由节点从原来旧队列中摘链，old_ht 记录着旧的队列位置，然后把这些路由节点结构链入到 fz-> fz_hash 指向的新队列中，这在 fn_rehash-zone() 函数 181 行已经指向了新队列。

fn_hash_insert()函数408行用到了前面计算的key值，这个key值是将配置结构的路由地址和子网掩码逻辑计算后的子网键值；根据这个子网值key调用fib_find_node()函数找到子网的路由节点结构。

fn_hash_insert() $\rightarrow$ fib_find_node()   
static struct fib_node \*fib_find node(struct fn-zone \*fz，_be32key) { struct hlist_head \*head $=$ &fz-> fz_hash[fn_hash(key,fz)]; struct hlist_node \*node; struct fib_node \*f;   
hlist_for_each_entry(f,node,head,fn_hash）{//沿着路由节点队列循环查找 if（f->fn_key == key) return f;

```txt
} return NULL; 
```

fib_find_node()是从路由节点队列 fz_hash中循环查找符合子网值的节点。

找到了路由节点结构就可以在413行通过fib_find alias()函数在它的路由别名队列 $f \longrightarrow$ fnalias中查找符合服务类型tos范围和优先级的路由别名结构，这是一个比较宽松的条件对比，是“模糊”性的检查。

fn_hash_insert() $\rightarrow$ fib_find alias()   
struct fib alias \*fib_find alias(struct list_head \*fah，u8 tos，u32 prio) { if (fah){ struct fib alias \*fa; list_for_each_entry(fa,fah,fa_list）{//沿着路由别名队列“模糊”查找 if（fa->fa_tos $>$ tos） continue; if（fa->fa_info->fibpriority $> =$ prio|！ fa->fa_tos<tos) return fa; 1 } 1 return NULL;

至此，指针f指向找到的路由节点，指针fa则指向路由别名队列中的第一个结构，接下来要沿着路由别名队列开始仔细的比对查找过程。

fn_hash_insert()  
```c
426 if (fa && fa-> fa_tol = = tos &&  
427 fa-> fa_info-> fibpriority == fi-> fibpriority) { //检查队列中第一个结构  
428 struct fib Alias *fa_first, *fa_MATCH;  
429  
430 err = -EEXIST;  
431 if (cfg-> fc_nflags & NLM_F_EXCL)  
432 goto out;  
433  
434 / * We have 2 goals:  
435 * 1. Find exact match for type, scope, fib_info to avoid  
436 * duplicate routes 
```

\*2. Find next fa' (or head), NLM_F_APPEND inserts before it   
\*/   
fa_MATCH $=$ NULL;   
fa_first $\equiv$ fa;//记录“模糊"查找的第一个路由别名结构   
fa $=$ list_entry(fa-> fa_list prev, struct fib Alias, fa_list);   
//沿着队列仔细查找最符合的路由别名结构   
list_for_each_entry_CONTINUE(fa, &f-> fn alias, fa_list){ if (fa-> fa_tos != tos) break; if (fa-> fa_info-> fibpriority! = fi-> fibpriority) break; if (fa-> fa_type ==cfg-> fc_type && fa-> fa_scope ==cfg-> fc_scope && fa-> fa_info == fi){ fa match $=$ fa;//找到后就记录它 break; }   
}   
if (cfg-> fc_nlflags&NLM_F_REPLACE){//是否允许修改替换 struct fib_info \*fi_drop; u8 state; fa $=$ fa_first; if (fa match){//如果前边找到了匹配的就不用修改 if (fa $= =$ fa_MATCH) err $= 0$ goto out; } write_lock_bh(&fib_hash_lock); fi_drop $=$ fa-> fa_info; fa-> fa_info $=$ fi; fa-> fa_type $=$ CFG-> fc_type; fa-> fa_scope $=$ CFG-> fc_scope; state $=$ fa-> fa_state; fa-> fa_state &= ~FA_S_ACCESSED; fib_hash_genid++; write_unlock_bh(&fib_hash_lock);

fib_release_info(fi_drop);//递减旧的路由信息结构的使用计数 if(state&FA_S_ACCESSED)//是否已经使用过 rt_cache Flush(-1);//冲刷缓存中的路由表 //通过netlink向IPROUT2发回消息告之设置情况 rtmsg_fib(RTM NewlyROUTE,key,fa,cfg->fcDst_len, tb->tb_id, &cfg->fc_nlinfo,NLM_F_REPLACE); return 0; } /\*Errorifwe find a perfect match which \*uses the same scope,type,and nexthop \*information. /\*if (fa_MATCH)//直接返回匹配的路由别名结构 goto out; //不允许追加到队列就使用“模糊”的结构 if(!(cfg->fc_nflags&NLM_F_APPEND)) fa $=$ fa_first; } err $=$ -ENOENT; if(!(cfg->fc_nflags&NLM_F_CREATE))//不允许创建就退出函数 goto out;

426行还要检查其服务类型值tos是否与配置结构cfg所指定的TOS相同，检查优先级是否相同。这时检查的路由别名fa是路由别名队列的第一个结构，沿着fa_list队列循环查找第一个符合所有条件的路由别名结构，条件不仅仅限于tos和优先级的比对，还增加了对范围、类型的比对，以及它的fa_info是否指向这里的路由信息结构。找到了匹配条件的路由别名结构，就记录在fa_MATCH处，它寓意为匹配的路由别名结构。

如果配置结构的 fc_nflags 标志中设置了替换标志 NLM_F_REPLACE，则重新设置找到的路由别名结构。设置之前先使用 fi_drop 记录下路由别名结构原来的路由信息结构，然后将它指向新建的路由信息结构，接着根据配置结构的内容设置类型和范围，然后清除结构状态“已经受过访问”标志位，让其“重新做人”。

既然使用新的路由信息结构 fi, 那么记录在 fi_drop 原来的路由信息结构则通过 fib_re-release_info 函数递减其计数器, 并检查是否需要释放其占用空间。

fn_hash_insert() $\rightarrow$ fib_release_info()

```c
void fib_release_info(struct fib_info \*fi)   
{ spin_lock_bh(&fib_info_lock); if (fi && --fi-> fib_treref == 0）{//递减并检查路由信息的使用计数器 hlist_del(&fi-> fib_hash);//从路由信息队列中摘链 if (fi-> fib preferringsrc) hlist_del(&fi-> fib_lhash); //从地址队列中摘链 change_nexthops(fi){ if(!nh->nh_dev) continue; hlist_del(&nh->nh_hash);//将跳转结构从路由设备队列中摘链 }endfor_nextdrops(fi) fi->fibdead = 1; fib_infoPut(fi); } spin_unlock_bh(&fib_info_lock); 
```

fib_release_info()函数主要是摘链操作，是对创建路由信息fib_create_info()函数的反向操作。在那里将路由信息以及它的跳转结构链入了3个哈希数组队列：路由信息队列数组、地址队列数组、路由设备队列数组，这里要从3个数组队列中脱链，并将路由信息结构的fibdead标记为1，表示已经删除；最后调用fib_infoPut()函数释放路由信息结构占用空间。

fn_hash_insert() $\rightarrow$ fib_release_info() $\rightarrow$ fib_infoPut()   
static inline void fib_infoput(struct fib_info\*fi) { if (atomic_dec_and_test(&fi->fib_clntref))//递减并检查路由信息的释放计数器 free_fib_info(fi);//释放路由信息占用空间

如果递减释放计数器到达0值，则调用free_fib_info()释放路由信息结构占用的内存空间，该函数在前面看过了。

继续看fn_hash_insert()函数476行，原来的路由别名结构已重新设置过了，并且显示该结构还在使用中，因此需要调用rt_cache Flush()对缓存中的路由表“刷新”。

fn_hash_insert() $\rightarrow$ rt_cache Flush()   
void rt_cache Flush(int delay)   
{ rt_cache Invalidate(); if(delay >=0)

```c
rt_do Flush(! insoftirq();   
}   
#define inSoftirq() (softirq_count())   
static void rt_cache Invalidate(void)   
{ unsigned char shuffle; get_random_bytes(&shuffle, sizeof(shuffle)); atomic_add(shuffle + 1U, &rt_genid);   
} 
```

以前的章节中介绍了原子量rt_genid的操作方法，rt_cache Invalidate()函数就是增加它的值。每次调用rt_cache Flush()都会依据可用内存的物理页数和当前jiffies值生成一个新的随机数保存在全局的rt_genid中，用来防止DoS攻击。它是路由表结构分布算法的一部分，使元素分布没有什么确定性。rt_genid变量先由ip_rt_init()初始化，然后在路由的许多过程中修改它的值，从而起到了“熵”的作用。

in_softirq 是一个宏声明, 主要是统计软中断的数量。如果参数 delay 延时值大于或者等于 0 , 则允许延时才会调用 rt_do Flush() 来冲刷路由表。因为冲刷过程需要一些时间来完成, 所以参数 delay 表明调用函数是否可以接受这种延时要求, 这里其传递下来的是 -1 , 因而只改变 rt_genid 的值, 我们也借机分析一下冲刷的过程。

fn_hash_insert() $\rightarrow$ rt_cache Flush() $\rightarrow$ rt_do Flush()   
static void rt_do Flush(int process_context)   
{ unsigned int i; struct rtable \*rth，\*next; for（i $= 0$ ；i $<   =$ rt_hash_mask; $\mathrm{i + + }$ ）{//沿着路由表哈希桶队列，循环取得每一个路由表 if (process_context && need_resched())//是否需要进程调度 cond_resched(); rth $=$ rt_hash_table[i].chain;//取得队列中的第一个路由表 if(!rth) continue; spin_lock bh(rt_hash_lock_addr(i)); rth $=$ rt_hash_table[i].chain;//加锁后可能情况有所改变，重新取一次路由表 rt_hash_table[i].chain $=$ NULL;//从哈希队列中脱队 spin_unlock_bh(rt_hash_lock_addr(i)); for(；rth;rth $=$ next）{//沿着队列释放每一个路由表 next $=$ rth->u.dst.rth_next; rt_free(rth);

```txt
} 1 
```

冲刷函数先根据是否存在软中断处理、是否需要调度来执行进程调度函数 cond_resched(), cond_resched 函数与进程管理相关不作为本文分析的重点。冲刷主要是循环对路由哈希队列 rt_hash_table 的操作, 沿着每一个哈希队列, 对内存中的路由表 rtable 执行 rt_free() 释放。函数有两处相同的代码用于获取队列的第一个路由表 rth, 这是因为加锁过程中有可能被其他进程抢占而修改队列的内容, 因而加锁后重新取一次 rth。

fn_hash_insert() $\rightarrow$ rt_cache Flush() $\rightarrow$ rt_do Flush() $\rightarrow$ rt_free()   
static inline void rt_free(struct rtable *rt)   
{ call_rcu_bh(&rt->u.dst.rcu_head,dst_rcu_free);

rt_free函数调用了call_rcu_bh()函数，这个函数是关于RCU锁的技术，简而言之，就是把这里的dst_rcu_free()函数通过rcu_data结构登记到内核rcu_bh_data队列中。在软中断情况下通过RCU机制执行释放过程，也就是在系统相对空闲时运行dst_rcu_free()函数，这里假设系统已经进入软中断执行释放函数dst_rcu_free()。

static inline void dst_rcu_free(struct rcu_head \*head)   
{ struct dst_entry \*dst $=$ container_of(head,struct dst_entry，rcu_head); dst_free.dst);

显然，对路由表的冲刷最终是清理其内部的dst_entry表项。

487行说明如果配置结构不允许修改替换，且找到了匹配的路由别名结构，则直接使用它。如果不允许追加到队列，则使用前面“模糊”查找的第一个路由别名结构。如果不允许创建新的结构，则直接退出；反之，函数继续往下执行。rtmsg_fib()函数是关于 netlink 的函数，它向 IPROUTE2 发送消息告之路由的设置情况，对这个函数的分析放在第 6 章 netlink 中进行。

继续看fn_hash_insert()函数余下的代码。

```c
fn_hash_insert()  
498 err = -ENOBUFS;  
499  
500 if (!f){//如果没有已经找到子网的路由节点结构则创建新的路由节点  
501 new_f = kmem_cache_zalloc(fn_hash_kmem, GFP_KERNEL);
```

```c
if (new_f == NULL)  
goto out;  
INIT_HLIST_NODE(&new_f->fn_hash); //初始化哈希队列头  
INIT_LIST_HEAD(&new_f->fnalias);  
new_f->fn_key = key; //记录子网键值  
f = new_f;  
}  
new_fa = &f->fn Embeddedalias; //指向嵌入在节点内部的别名结构  
if (new_fa->fa_info != NULL) { //如果这个结构已经使用了，就创建一个新的  
    new_fa = kmem_cache_alloc(fnalias_kmem, GFP_KERNEL);  
    if (new_fa == NULL)  
        goto out;  
}  
new_fa->fa_info = fi; //路由信息  
new_fa->fa_tot = tos; //TOS  
new_fa->fa_type =cfg->fc_type; //路由类型  
new_fa->fa_scope =cfg->fc_scope; //路由范围  
new_fa->fa_state = 0; //未被访问  
/*  
* Insert new entry to the list.  
*/  
write_lock_bh(&fib_hash_lock);  
if (new_f)  
    fib_insert_node(fz, new_f); //将新建节点结构链入路由区的队列中  
list_addTAIL(&new_fa->fa_list, (fa?&fa->fa_list: &f->fnalias)); //将新建的别名结构链入节点的队列中  
fib_hash_genid++; //增加队列计数器  
write_unlock_bh(&fib_hash_lock);  
if (new_f)  
    fz->fz_nent++; //增加路由区包含的节点数  
rt_cache Flush(-1); //冲刷路由表缓存  
//通过 netlink 向 IPROUTE2 返回消息，告之路由设置情况  
rtmsg_fib(RTM-NewROUTE, key, new_fa,_cfg->fc.dst_len, tb->tb_id, &cfg->fc_ninfo, 0);
```

```c
541 return 0;   
542   
543 out:   
544 if (new_f)   
545 kmem_cache_free(fn_hash_kmem, new_f);   
546 fib_release_info(fi);   
547 return err;   
548} 
```

500行处再次检查对应子网键值的路由节点f是否已经找到，此时配置结构标志也表明允许创建，否则不会到达此处。接着在slab高速缓存fn_hash_kmem中分配一个新的路由节点结构new_f，然后初始化它的队列头，记录子网键值，将f指向新建的new_f。

到达511行时，f可能是前面在路由区中找到的也可能是刚刚创建的，不管何种情形，它内部“嵌入”的路由别名结构是随节点空间一起分配的，这里直接让new_fa取得“嵌入”的路由别名结构地址。然后检查它的fa_info指针是否为空，其中，这个指针指向所属的节点结构。如果指针是空，说明正好可以直接使用这个路由别名结构；如果不为空，则说明路由节点是前面查找到的，并且嵌入的别名结构已经被使用了，就要创建一个新的路由别名结构。

513 行是在高速缓存 fn Alias_kmem 中分配一个新的路由别名结构，以上两个 slab 缓存都是内核在初始化时调用 fib_hash_init() 函数创建的。创建了新的路由别名结构之后，记录下新创建的路由信息结构 f，记录 tos 服务类型值，以及路由类型、路由范围，设置路由状态为 0（表示还没有被访问过）。

对于新创建的路由节点 new_f, 还要调用 fib_insert_node()函数执行“入队”操作。

fn_hash_insert() $\rightarrow$ fib_insert_node()   
static inline void fib_insert_node(struct fn-zone \* fz,struct fib_node \*f) { struct hlist_head \*head $=$ &fz-> fz_hash[fn_hash(f->fn_key,fz)]; hlist_add_head(&f->fn_hash，head);

这个函数将新建的路由节点结构 new_f 插入到路由区结构中的 fz_hash 队列，这是个哈希队列。队列插入函数 hlist_add_head() 也已经在前面看过了。接着还要对新建的路由别名结构执行“入队”操作。通过 list_add.tail() 函数链入到节点的队列中，只不过先判断一下“模糊”查找的路由别名结构 fa 是否存在。如果存在就可以直接插到它的后面，反之插到节点中的队列尾部。

536 行增加一下节点计数器后，也要调用 rt_cache Flush() 冲刷一下缓存。538 行调用 rtmsg_fib 函数向 IPROUTE2 发送消息告之路由的设置情况。至此，fn_hash_insert() 函数在内存中建立了路由需要的环境。

# 4.5 基于输出方向的路由表查找与创建

我们现在以tcp_v4_connect()函数调用ip-route_connect()的过程分析输出方向的路由表查找与创建。tcp_v4_connect()函数将在第9章客户端向服务器发出连接请求的过程中被调用，参数rp用于返回路由表的双指针，dst是目标地址，src是源地址，tos是服务类型值，oif是指定的发送设备ID,protocol是使用的IP协议,sport是源端口,dport是目标端口，sk是用于发送的sock结构，flags是标志。这个函数在include/net/route.h中，读者可以在学习了第9章内容后再阅读本函数。

sys_SOCKETcall() $\rightarrow$ sys_connect( $\rightarrow$ inlet_STREAM_connect( $\rightarrow$ tcp_v4_connect( $\rightarrow$ ip-route_connect()   
147static inline int ipRoute_connect(struct rtable $^ { \text{串} }$ rp,_be32 dst,   
148 be32 src,u32 tos,int oif,u8 protocol,   
149 be16 sport,_be16 dport,struct sock \*sk,   
150 int flags)   
151{//初始化路由键值fl   
152 struct flowif fl $=$ {.oif $=$ oif,   
153 .mark $=$ sk->sk_mark,   
154 .nl_u $=$ {.ip4_u $=$ {.daddr $=$ dst，//目标地址   
155 .saddr $=$ src，//源地址   
156 .tos $=$ tos} //TOS   
157 .proto $=$ protocol，//协议   
158 .uli_u $=$ {.ports =   
159 {.sport $=$ sport，//源端口   
160 .dport $=$ dport}）; //目标端口   
161   
162 int err;   
163 struct net \*net $=$ sock_net(sk);   
164 if(!dst||！src）{//如果没有指定目标地址或者源地址，就要查找路由表   
165 err $=$ _ip-route_output_key(net,rp,&fl);   
166 if(err)   
167 return err;   
168 fl.f14_dst $=$ (\*rp)->rt_dst;//使用路由表的目标地址   
169 fl.f14_src $=$ (\*rp)->rt_src;//使用路由表的源地址   
170 ip_rtPut(\*rp);//递减路由表的路由项计数器

171 \*rp $=$ NULL;   
172 }   
173 security_sk_classify_flow(sk,&fl);   
174 return ip-route_output_flow(net,rp,&fl,sk,flags);//再次查找并调整地址   
175}

该函数152行声明并初始化了路由键值结构fl，这个结构已经多次出现在查找路由过程中，它的内部主要是网络层和传输层的重要信息。

# 代码清单 4.11 flowi 结构定义

```txt
struct flowi { //路由键值结构
    int oif; //负责发送的网络设备
    int iif; //负责接收的网络设备
    __u32 mark; //子网掩码
    union {
        struct {
            __be32 daddr; //目标地址
            __be32 saddr; //源地址，即发送方的地址
            __u8 tos; //服务类型 TOS
            __u8 scope; //范围
        } ip4_u;
        struct {
            struct in6_addr daddr;
            struct in6_addr saddr;
            __be32 flowlabel;
        } ip6_u;
        struct {
            __le16 daddr;
            __le16 saddr;
            __u8 scope;
        } dn_u;
    } nl_u; //该联合内容主要用于网络层
    # define fld.dst nl_u.dn_u.daddr
    # define fld_src nl_u.dn_u.saddr
    # define fld_scope nl_u.dn_u.scpe
    # define fl6.dst nl_u.ip6_u.daddr
    # define fl6_src nl_u.ip6_u.saddr
    # define fl6_flowlabel nl_u.ip6_u.flowlabel
    # define fl4.dst nl_u.ip4_u.daddr
```

```txt
define fl4_src nl_u.ip4_u.saddr
#define fl4_tos nl_u.ip4_u.tos
#define fl4_scope nl_u.ip4_uscopes
__u8 proto; //传输层协议
__u8 flags; //标志位
union {
struct {
__be16 sport; //源端口,即发送方的端口
__be16 dport; //目标端口,即接收方的端口
} ports;
struct {
__u8 type;
__u8 code;
} icmt; //ICMP类型
struct {
__le16 sport;
__le16 dport;
} dnports;
__be32 spi;
struct {
__u8 type;
} mht;
} uli_u; //该联合的内容主要用于传输层
# define fl_ip_sport uli_uPorts.sport
# define fl_ip_dport uli_uPorts.dport
# define fl_ICMP_type uli_u.icmpt.type
# define fl_ICMP_code uli_u.icmpt.code
# define fl_ipsec spi uli_u.sp;
# define fl_mh_type uli_u.mht.type
__u32 secid; /* used by xfrm; see secid.txt */
} __attribute_((__aligned_(BITS_PER_Long/8))); 
```

路由键值结构中大多数是关于目标地址、源地址（这里就是客户端地址）及端口等的信息，这些信息都是基于进入和出口设备的内容。iif 是接收设备的 ID 号，uli_u 联合是用于 IP 层的参数值，先看从 tcp_v4_connect 函数传递的参数。

```c
169 tmp = ip-route_connect(&rt, nexthop, inlet -> saddr,  
170 RT_CONN_FLAGS(sk), sk-> sk_bound_dev_if,  
171 IPPROTO_TCP,  
172 inlet-> sport, usin-> sin_port, sk, 1); 
```

传递给ip route_connect()函数的第一个参数是路由表rt,用来返回给tcp_v4_connect()函数使用,它是rtable路由表结构,主要存放发送数据包的路由信息。我们可以在proc文件系统中通过/proc/net/rt_cache读出路由表的内容,这个结构体在前面路由表哈希桶rt_hash Bucket的描述中简要描述了,并且也列出了它在内存中的关系图。

# 代码清单 4.12 rtable 结构定义

```txt
struct rtable //路由表结构
{
    union
    {
        struct dst_entry dst; //包含的路由项
    } u;
    /* Cache lookup keys */
    struct flowi fl; //路由键值
    struct in_device *dev; //IPv4协议配置网络设备的结构
    int rt_genid; //路由表使用的随机数
    unsigned rt_flags; //路由标志
    __u16 rt_type; //路由类型
    __be32 rt.dst; /*路由的目标地址 */
    __be32 rt_src; /*路由的源地址 */
    int rt_iif; //路由的接收设备
    /* Info on neighbour */
    __be32 rt Gateway; //路由网关
    /* Miscellaneous cached information */
    __be32 rt_spec.dst; /* RFC1122指定的目标地址 */
    struct inlet_peer *peer; /*对方主机的信息 */
};
```

结构中的 u 变量封装着 dst_entry 路由表项的内容, idev 是指向 IPv4 配置内容。当接收的数据包是本地发送的时候, 网络设备就是回接设备。另外, 除了路由标志 rt_flags 和路由类型 rt_type 外, 还包括 rt.dst 路由目标地址和 rt_src 路由源地址。rt_iif 是接收网络设备的 ID 号, 它的值从网络设备结构 net_device 中取得。rt_GATEway 是直接连接的目标主机地址, 当网关是目标地址时, rt_GATEway 就被设置为下一个跳转的网关。peer 是 inlet_peer 结构变量, 这个结构主要用来保存最近路由过程中刚刚访问的目标主机的信息。

第二个参数 nexthop 可能是服务器地址也可能是跳转地址，这是由 tcp_v4_connect() 函数对 IP 选项结构的检查结果决定的。

第三个参数inet $\rightarrow$ saddr是源地址，即发送方的IP地址。

第四个参数是通过宏 RT_CONN_FLAGS 确定服务类型 tos, tos 是 type of service 的意思。

第五个参数是socket使用的网络设备的ID。

第六个参数是宏 IPPROTO_TCP, 它确定使用 TCP/IP 传输控制协议。

第七个参数inet->sport是客户端的端口。

第八个参数usin->sin_port是服务器端口，即目标端口。

先将这些参数设置到路由键值fl中，接着取得网络命名空间结构，这里取得的是全局变量init_net。还要检查目标地址和源地址，如果其中之一为空就会进入__ip-route_output_key()函数，它将根据路由键值fl查找或者创建路由表。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key()

```c
2477int __ip-route_output_key(struct net *net, struct rtable * *rp,
2478 const struct flowi *flp)
2479\{
2480 unsigned hash;
2481 struct rtable *rth;
2482
2483 hash = rt_hash(flp-> fl4.dst, flp-> fl4_src, flp-> oif); //确定哈希值
2484
2485 rcu_read_lock_bh(   );
2486 for (rth = rcu_dereference(rt_hash_table[hash].chain); rth;
2487 rth = rcu_dereference(rth-> u.dst.rt_next)) {//沿着路由表队列循环查找路由表
2488 if (rth-> fl.f14.dst == flp-> fl4.dst &&//目标地址相同
2489 rth-> fl.f14_src == flp-> fl4_src &&//源地址相同
2490 rth-> fl.iif == 0 &&//是否指定接收网络设备
2491 rth-> fl.oif == flp-> oif &&//发送网络设备是否相同
2492 rth-> fl_mark == flp-> mark &&//掩码相同
2493 !((rth-> fl.f14_tot^flp-> fl4_tot) & 
2494 (IPTOS_RT_MASK | RTO_ONLINK)) &&
2495 net_eq(dev_net(rth-> u.dst.dev), net) &&
2496 rth-> rt_genid == atomic_read(&rt_genid) \{
2497 dst_use(&rth-> u.dst, jiffies); //路由项记录使用时间,增加使用计数
2498 RT_CACHE_STAT_INC(out_hit); //递增路由缓存发送方向的命中计数
2499 rcu_read_unlock_bh(   );
2500 * rp = rth; //使上一级函数获得路由表指针 
```

```c
2501 return 0;   
2502 }   
2503 RT_CACHE_STAT_INC(out_hlist_search);//递增路由缓存队列搜索的命中计数   
2504 }   
2505 rcu_read_unlock_bh();   
2506   
2507 return ip-route_output_slow(net, rp, flp);   
2508}
```

内核中有一个全局的rt_hash_table杂凑数组(即哈希数组)，它是一个rt_hash_buckets结构数组。

```c
struct rt_hash_buckets {
    struct rtable *chain;
}; 
```

可以看到，rt_hash_buckets 结构中只包含路由表 rtable 队列指针，而路由表则通过其内部的 u.dst.rt_next 指针链接成队列。

_ip-route_output_key()函数先根据路由键值中的目标地址、源地址以及设备的ID来确定一个hash值，然后循环以这个hash值为下标在rt_hash_table中找到所属哈希队列，队列中的路由表通过u.dst.rtnext指向下一个。循环正是利用这种链接关系沿着队列查找符合路由键值的路由表，找到后就使tcp_v4_connect()函数获取这个路由表指针。

如果没有找到，则在2507行执行ip-route_output_slow()函数，并从该函数返回。在这个函数中将会创建一个新的路由键值；然后调用fibLOOKUP()函数在网络空间中找到路由函数表，再通过它内部的函数来建立路由表。

由于ipRoute_output slowdown()函数比较大，因此分段来看。

sys_SOCKETcall() $\rightarrow$ sys_connect( $\rightarrow$ inlet_STREAM_connect( $\rightarrow$ tcp_v4_connect( $\rightarrow$ ip-route_connect( $\rightarrow$ __ip-route_output_key( $\rightarrow$ ip-route_output Slow(）   
2282static int ipRoute_output_slow(struct net \*net，struct rtable $^ { \text{喜} }$ rp,   
2283 const struct flowi \* oldflp)   
2284{   
2285 u32 tos $=$ RT_FL_TOS(oldflp);//取得前面路由键值中的TOS值   
2286 struct flowi fl $=$ { .nl_u $=$ { .ip4_u =   
2287 { .daddr $=$ oldflp-> f14.dst,//目标地址   
2288 .saddr $=$ oldflp-> f14_src,//源地址   
2289 .tos $=$ tos&IPTOS_RT_MASK,/TOS   
2290 .scope $=$ ((tos & RTO_ONLINK)? //范围   
2291 RT SCOPE LINK:

2292 RT scape_UNIVERSE),   
2293 },   
2294 .mark $=$ oldflp-> mark,//掩码   
2295 .iif $=$ net-> loopback_dev-> ifindex,//接收网络设备   
2296 .oif $=$ oldflp-> oif }; //发送网络设备   
2297 struct fib_result res; //声明查找结构变量   
2298 unsigned flags $= 0$ 2299 struct net_device \*dev_out $=$ NULL;   
2300 int free_res $= 0$ 2301 int err;   
2302   
2303   
2304 res.fi $=$ NULL;   
2305#ifdef CONFIG_IP_multiple_TABLES   
2306 res.r $=$ NULL;   
2307#endif   
2308

参数 net 是 init_net 全局的网络空间结构指针，而 rp 是用来获取路由表的双指针。Old-flp 是从上一级函数传递的路由键值，是在 ip-route_connect() 函数中创建的。

这里创建并初始化了一个新的路由键值 fl。它“克隆”了 oldflp 的内容，因为查找路由过程中会调整路由键值的内容，这里使用 oldflp 的备份 fl 可防止丢失原始的内容。

2297 行声明了一个路由查找结果结构 res, 结构内容在前面讲述了, 这里继续看 ip-route_output Slow 函数的代码。

2309 if(oldflp->fl4_src){//如果指定了源地址  
2310 err = -EINVALID;  
2311 if(ipv4_isMULTicast(oldflp->fl4_src)||//检查地址是否是组播地址  
2312 ipv4_is_lbcast(oldflp->fl4_src)||//检查地址是否是广播地址  
2313 ipv4_is_zeronet(oldflp->fl4_src)) //检查地址是否是零网地址  
2314 goto out;//源地址不允许是上述3种地址类型，因此返回  
2315  
2316 /\*It is equivalent to inset_addr_type(saddr) $= =$ RTN_LOCAL \*/  
2317 dev_out $=$ ip_dev_find(net, oldflp->fl4_src);//查找发送网络设备  
2318 if(dev_out $= =$ NULL)  
2319 goto out;  
2320  
2321 /\*Iremoved check for oif $= =$ dev_out->oif here.  
2322 It was wrong for two reasons:

1. ip_dev_find(net,ADDR) can return wrong iface,ifADDRis assigned to multiple interfaces.   
2. Moreover, we are allowed to send packets withADDRof another iface. -- ANK \*/   
if (oldflp-> oif $= = 0$ //如果没有指定发送设备 && (ipv4_isMULTicast(oldflp-> fl4.dst)|| //目标地址oldflp-> fl4.dst $= =$ htonl(0xFFFFFFF))）{//目标地址\*/Special hack: user can direct multicastsand limited broadcast via necessary interfacewithout fiddling with IP MULTICAST_IF or IP_PKTINFO.This hack is not just for fun, it allowsvic,vat and friends to work. They bind socket to loopback, set ttl to zeroand expect that it will work.From the viewpoint of routing cache they are broken, because we are not allowed to build multicast pathwith loopback source addr (look,routing cachecannot know,that ttl is zero,so that packetwill not leave this host and route is valid).Luckily,this hack is good workaround.\*/fl.oif $=$ dev_out-> ifindex;//记录上面找到的发送设备IDgoto make-route;1if(dev_out)devPut(dev_out);//递减发送网络设备的使用计数dev_out $=$ NULL;

源地址fl4_src在路由键值结构flowi中是以宏的形式出现的。

```txt
define fl4_src nl_u.ip4_u.saddr 
```

nl_u 是路由键值结构的一个联合，这个宏的作用就是取得设置的源地址。这类宏的使用方式提高了可阅读性，使代码更直观。

先检查源地址是否指定了，接着检查这个地址的类型是否是组播、广播或者零地址，这3种类型是不允许用于源地址的。接着调用ip_dev_find()函数查找发送网络设备net_device

结构, 调用时将网络空间结构指针和源地址作为参数传递给该函数。

sys_SOCKETcall() $\rightarrow$ sys_connect(） $\rightarrow$ inlet_STREAM_connect(） $\rightarrow$ tcp_v4_connect(） $\rightarrow$ ip-route_   
connect(） $\rightarrow$ _ip-route_output_key(） $\rightarrow$ ip-route_output Slow(） $\rightarrow$ ip_dev_find()   
156 struct net_device \* ip_dev_find(struct net \* net,_be32 addr)   
157{   
158 struct flowif1 $= \{$ .nl_u $=$ {.ip4_u $=$ {.daddr $=$ addr}）};//记录在目标地址中   
159 struct fib_result res;   
160 struct net_device \* dev $=$ NULL;   
161 struct fib_table \* local_table;//用于查找本地路由函数表   
162   
163 # ifdef CONFIG_IP MULTIPLE_TABLES   
164 res.r $=$ NULL;   
165 #endif   
166   
167 local_table $=$ fib_get_table(net,RT_TABLE_LOCAL);//查找本地路由函数表 //如果找到本地路由函数表就调用它的查找函数，查找地址的类型   
168 if(!local_table||local_table->tb.lookup(local_table,&fl,&res))   
169 return NULL;   
170 if(res.type！ $=$ RTN_LOCAL)//如果地址不属于本地类型就返回   
171 goto out;   
172 dev $=$ FIB_RES_DEV(res);//查找结构中记录着发送设备指针，取得设备指针   
173   
174 if (dev)   
175 dev_hold(dev);//增加网络设备结构的使用计数   
176 out:   
177 fib_resPut(&res);//释放查找结果结构   
178 return dev; //返回网络设备结构指针   
179

只要是查找路由就必然不能缺少路由键值，因此158行创建了用于查找网络设备的路由键值。这里只记录了源地址作为查找目标地址，然后在本地路由表中以这个路由键值为依据查找发送的网络设备，查找结果保存在res中。这期间的查找过程在前面的fn_hash.lookup()函数中都已经讲述了。

如果查找顺利就通过宏FIB_RES_DEV从查找结果res中取得发送网络设备结构net_device，还要增加这个结构的使用计数，避免它被内核释放。

回到ip route_output slowdown()函数2329行，先查看以前的路由键值是否指定了发送设备，如果没有指定而且目标地址是多播地址或者是受限的广播地址时，则就在2347行处设置fl.oif为已经找到的发送设备，然后跳转到make route标号处。这个标号处的内容放在后面分

析，继续看ip-route_output slowdown()函数的代码。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output Slow()

2356 if(oldflp->oif){//如果指定了发送设备  
2357 dev_out $=$ dev_get_by_index(net, oldflp->oif); //找到指定设备的结构  
2358 err $=$ -ENODEV;  
2359 if (dev_out $= =$ NULL)  
2360 goto out;  
2361  
2362 /\*RACE:Check return value of inset_select_addr instead. \*/  
2363 if(_in_dev_get_rtnl(dev_out) $= =$ NULL）{//检查 in_device 结构是否存在  
2364 dev_put(dev_out);  
2365 goto out; /* Wrong error code */  
2366 }  
2367  
2368 if (ipv4_is_localMULTicast(oldflp->fl4.dst) || //如果目标地址是本地组播地址  
2369 oldflp->fl4.dst $= =$ htons(0xFFFFFFF)){//或者是受限的广播地址  
2370 if(!fl.f14_src)//没有指定源地址，就调用查找地址函数  
2371 fl.fl4_src $=$ inlet_select_addr(dev_out,0,  
2372 RT SCOPE LINK);  
2373 goto make-route;  
2374 }  
2375 if(!fl.fl4_src){//如果还没有源地址，就调整范围查找源地址  
2376 if (ipv4_isMULTicast(oldflp->fl4.dst))//目标地址是组播地址  
2377 fl.fl4_src $=$ inlet_select_addr(dev_out,0,  
2378 fl.fl4_scope);  
2379 else if(! oldflp->fl4.dst)//没有指定目标地址  
2380 fl.fl4_src $=$ inlet_select_addr(dev_out,0,  
2381 RT SCOPE_HOST);  
2382 }  
2383 }  
2384

既然前面的路由键值已经指定了发送设备，那就通过dev_get_by_index()函数找到它的net_device结构。找到了设备的结构后还要检查它记录的in_device结构是否存在，in_device结构是IPv4协议配置网络设备的专用结构体，网络设备结构net_device的ip_ptr指向这个结构。

$2368\sim 2380$ 行3次调用查找地址函数inet_select_addr()，用来设置源地址fl4_src，这个

查找地址函数在/net/ipv4/devinet.c中。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ INET_select_addr()

866 __be32 inset_select_addr(const struct net_device *dev, __be32 dst, int scope)

867{

868 be32 addr = 0;

869 struct in_device *in_dev; //IPv4配置设备结构

870 struct net \*net $=$ dev_net(dev);//取得所属网络空间

871

872 rcu_read_lock();

873 in_dev = __in_dev_get_rcu(dev); //取得 IPv4 配置设备结构

874 if(!in_dev)

875 goto no_in_dev;

876

877 for_primary_ifa(in_dev){//地址队列中查找相同范围和目标地址的结构

878 if (ifa-> ifa_scope > scope)

879 continue;

880 if(!dst||inet_ifa_MATCHdst,ifa))

881 addr = ifa-> ifa_local; //记录本地地址

882 break;

883

884 if(!addr)

885 addr = ifa-> ifa_local;

886 } endfor_ifa(in_dev);

887 no_in_dev:

888 rcu_read_unlock();

889

890 if (addr)//找到了本地地址就退出

891 goto out;

892

893 /\* Not loopback addresses on loopback should be preferred

894 in this case. It is important that lo is the first interface

895 in dev_base list.

896 \*/

897 read_lock(&dev_base_lock);

898 rcu_read_lock();

899 for_each_netdev(net, dev) { //扩大搜索范围，再次搜索地址结构

900 if ((in_dev = __in_dev_get_rcu(dev)) == NULL)

901 continue;   
902   
903 for_primary_ifa(in_dev){   
904 if (ifa-> ifa_scope != RT_SCOPELINK &&   
905 ifa-> ifa_scope $<   =$ scope) {   
906 addr $=$ ifa-> ifa_local;//找到就记录本地地址并返回   
907 goto out_unlockboth;   
908 }   
909 } endfor_ifa(in_dev);   
910 }   
911 out_unlockboth;   
912 read_unlock(&dev_base_lock);   
913 rcu_read_unlock();   
914 out:   
915 return addr;   
916}

这个函数先通过__in_dev_get_rcu()取得网络设备结构ip_ptr所记录的配置设备结构in_device,它内部有一个IPv4地址结构队列ifa_list,地址结构用in_ifaddr表示。

# 代码清单 4.13 in_ifaddr 结构定义

```c
struct in_ifaddr
{
    struct in_ifaddr * ifa_next; //指队列中下一个地址结构
    struct in_device * ifa_dev; //指向所属的配置设备结构
    struct rcu_head rcu_head; //队列头
        __be32 ifa_local; //本地地址
        __be32 ifa_address; //目标地址
        __be32 ifa_mask; //子网掩码
        __be32 ifa_bROADCAST; //广播地址
        unsigned char ifa_scope; //范围
        unsigned char ifa_flags; //标志位
        unsigned char ifa_prefixlen; //子网掩码中1的个数
        char ifa_label[IFNAMSIZ]; //兼容老版本内核使用
```

877行处的for_primary_ifa循环宏就是沿着这个队列依次取出每一个地址结构，找出符合指定范围和目标地址的结构。第一次调用时，指定的范围是RT_SCOPE_link子网范围；第二次调用时，指定的范围是路由键值指定值；第三次调用时，指定的范围是RT_SCOPE_HOST本地范围。3次调用都是为了确定源地址，因此目标地址都为0。880行的条件总能满足

足，因此查找过程只限于范围条件了。

如果找到了地址结构，则局部变量 addr 就记录下它的本地地址 ifa_local；返回到 ip-route_output Slow() 函数后，这个地址值会记录到路由键值中的源地址 fl.fl4_src，从而完成了源地址的查找。

如果873行没有取得IPv4配置结构，则跳转到887行no_in_dev标号处。此处有两层循环，外层沿着网络空间的设备结构队列依次取得每一个设备结构net_device，只要它的ip_ptr不为空就说明存在配置结构in_device；然后内层循环再沿着配置结构中的地址队列循环查找符合条件的结构，但是这次检查条件排除了RT_SCOPE_LINK子网范围，找到了地址结构也用addr记录下它的本地地址ifa_local。

我们继续看ip-route_output Slow()函数余下的代码。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output Slow()

```c
2385 if(!fl.f14.dst){//如果没有指定目标地址  
2386 fl.f14.dst = fl.f14_src; //先使用源地址为目标地址  
2387 if(!fl.f14.dst)//如果目标地址还是空  
2388 fl.f14.dst = fl.f14_src = htons(INADDR_LOOPBACK); //使用回接地址  
2389 if(dev_out)//如果找到了发送设备  
2390 dev_put(dev_out); //递减发送设备的使用计数  
2391 dev_out = net->loopback_dev; //使用回接设备作为发送设备  
2392 dev_hold(dev_out); //递增回接设备的使用计数  
2393 fl.oif = net->loopback_dev->ifindex; //路由键值记录发送设备的ID  
2394 res.type = RTN_LOCAL; //记录为本地地址类型  
2395 flags |= RTCF_LOCAL; //增加本地路由标志  
2396 goto make-route;  
2397 }  
2398  
2399 if(fib.Lookup(net, &fl, &res)) { //调用路由函数表根据路由键值查找  
2400 res-fi = NULL;  
2401 if(oldflp->oif) {  
2402 /* Apparently, routing tables are wrong. Assume,  
2403 that the destination is on link.  
2404  
2405 WHY? DW.  
2406 Because we are allowed to send to interface  
2407 even if it has NO routes and NO assigned  
2408 addresses. When oif is specified, routing  
2409 tables are looked up with only one purpose: 
```

to catch if destination is gatewayed, rather than direct. Moreover, if MSG_DONTROUTE is set, we send packet, ignoring both routing tables and ifaddr state. -- ANK  
We could make it even if oif is unknown, likely IPv6, but we do not. $\begin{array}{l}\text{I}\text{f} (\text{fl}. \text{fl4\_src} = = 0)\\ \text{fl}. \text{fl4\_src} = \text{inet\_select\_addr} (\text{dev\_out}, 0, \\ \text{RT\_SCOPE\_LINK}); / / \text{获取源地址}\\ \text{res.type} = \text{RTN\_UNICAST};\\ \text{goto make\_route};\\ \end{array}$ $\begin{array}{l}\text{if (dev\_out)}\\ \text{dev\_put} (\text{dev\_out});\\ \text{err} = -\text{ENETUNREACH};\\ \text{goto out};\\ \end{array}$ $\begin{array}{l}\text{free\_res} = 1;\\ \text{if (res.type} = = \text{RTN\_LOCAL}) \{//如果路由地址是本地类}}\\ \text{if (! fl.f14\_src)}\\ \text{fl.f14\_src} = \text{fl.f14\_dst};\\ \text{if (dev\_out)}\\ \text{dev\_put} (\text{dev\_out});\\ \text{dev\_out} = \text{net-> loopback\_dev}; / / \text{使用回接设备}}\\ \text{dev\_hold} (\text{dev\_out});\\ \text{fl.oif} = \text{dev\_out-> ifindex};\\ \text{if (res.fi)}\\ \text{fib\_info\_put} (\text{res.fi});\\ \text{res.fi} = \text{NULL};\\ \text{flags} | = \text{RTCF\_LOCAL};\\ \text{goto make\_route};\\ \end{array}$

2449 if(res.fi->fib_nhs>1&&fl.oif $= = 0$ 2450 fib_select multipath(&fl,&res);   
2451 else   
2452#endif   
2453 if(!res_prefixlen&&res.type $\equiv =$ RTN_UNICAST&&!fl.oif)   
2454 fib_select_default(net,&fl,&res);//查找路由信息   
2455   
2456 if(!fl.fl4_src)   
2457 fl.fl4_src $\equiv$ FIB_RES_PREFSRC(res);//使用路由信息中的地址   
2458   
2459 if(dev_out)   
2460 devPut(dev_out);   
2461 dev_out $\equiv$ FIB_RES_DEV(res);//使用跳转结构的设备作为发送设备   
2462 dev_hold(dev_out);   
2463 fl.oif $\equiv$ dev_out->ifindex;//记录发送设备号   
2464   
2465   
2466make-route://到达此处已经确定了源地址、目标地址和发送设备，接着创建路由表   
2467 err $\equiv$ ip_mkroute_output(rp,&res,&fl,oldflp,dev_out,flags);   
2468   
2469   
2470 if(free_res)   
2471 fib_res_put(&res);//释放查找结果   
2472 if(dev_out)   
2473 devput(dev_out);//递减发送设备的使用计数   
2474out: return err;   
2475}

2385行检查是否设置了路由键值的目标地址，它在2287行处已经记录了服务器地址，但是这里需要看一下没有设置目标地址的情况；如果没有设置目标地址，就将目标地址设置为源地址，也可能还是空地址，因为源地址可能为空，此时就设置目标地址和源地址都为回接地址127.0.0.1。

既然是本地回接就不需要原来找到的发送网络设备了，所以2390行调用devPut()递减了发送网络设备结构的计数，放弃对它的使用后，改用回接设备，这个设备结构记录在网络空间的loopback_dev处。路由键值的oif记录下回接设备的ID，接着设置查找结果的地址类型为本地路由类型并增加本地路由标志，然后就转到make-route标号处执行。

函数前面主要是对路由键值的源地址和目标地址的设置。2399行fibLOOKUP()函数通过路由函数表查找路由键值的地址类型。这个函数已经在前面分析了，正常情况下该函数返回

值是0；如果返回值不为0就说明路由函数表出现问题了，于是将查找结果res的路由信息指针置NULL，再检查以前的路由键值是否指定发送设备，此时新路由键值的源地址仍旧是0，那就要再次调用inet_select_addr()获取地址，接着调整res记录的地址类型为单播类型，也跳转到make-route处去执行。

如果路由函数表查找出错并且以前的路由键值也没有指定发送设备，则在2427行放弃使用已经找到的发送设备，然后设置出错码ENETUNREACH表示无法送达就返回了。

如果 fibLOOKUP()函数顺利找到了路由函数表，且是本地路由函数表，而路由键值的源地址还不存在，就让它与目标地址相同。因为使用的是本地路由函数表，那就不需要发送设备结构，于是递减它的使用计数放弃对它的使用，然后调整发送设备dev_out指针指向回接设备loopback_dev。增加回接设备的使用计数让路由键值的oif记录下它的索引ID号。如果路由查询结构中记录着路由信息结构，则说明在查找过程中增加了其使用计数，此时用不到路由信息结构就放弃对它的使用，递减它的使用计数后清空查询结果中的指针fi，然后设置本地路由标志RTCF_LOCAL，跳转到make-route标号处。

如果不是本地路由表而是主路由表，或者启用了多路径选项，则继续往下执行，在多路径情况下就通过fib_selectMULTIPath()函数对路由信息的全部跳转结构重新优化计算。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inlet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ fib_select Multipath()

1178void fib_selectMULTipath(const struct flowi \*flp，struct fib_result \*res)   
1179{   
1180 struct fib_info \*fi $=$ res->fi;   
1181 int w;   
1182   
1183 spin_lock_bh(&fibMULTipath_lock);   
1184 if (fi->fib_power <= 0）{//调整路由信息的跳转能力值   
1185 int power = 0;   
1186 change_nexthops(fi){   
1187 if(!(nh->nh_flags&RTNH_F_DEAD)){   
1188 power $+ =$ nh->nh_weight;//记录每个跳转结构的压力值   
1189 nh->nh_power $=$ nh->nh_weight;//能力来源于压力   
1190 }   
1191 }endfor_nexthops(fi);   
1192 fi->fib_power $=$ power;//记录下能力统计结果   
1193 if (power $<   = 0$ {   
1194 spin_unlock_bh(&fibMULTipath_lock);   
1195 /\*Race condition:route has just become dead.\*/   
1196 res->nh_sel $= 0$ //清除跳转计数

1197 return;   
1198 }   
1199 }   
1200   
1201   
1202 /\*wshould be random number $[0..fi->$ fib_power-1],   
1203 it is pretty bad approximation.   
1204 \*/   
1205   
1206 w $=$ jiffies $\%$ fi->fib_power;   
1207   
1208 change_nexthops(fi){   
1209 if(!(nh->nh_flags&RTNH_FDead)&&nh->nh_power）{   
1210 if((w- $=$ nh->nh_power）<=0）{//在能力范围统计跳转次数   
1211 nh->nh_power--;   
1212 fi->fib_power--;   
1213 res->nh_sel=nhsel;//记录跳转次数   
1214 spin_unlock_bh(&fib_multipath_lock);   
1215 return;   
1216 }   
1217 }   
1218 }endfor_nexthops(fi);   
1219   
1220 /\*Race condition:route has just become dead.\*/   
1221 res->nh_sel=0;   
1222 spin_unlock_bh(&fib_multipath_lock);   
1223}

这个函数主要是对跳转次数的统计，根据每个跳转结构的压力调整能力值。在1206行根据时间值确定统计能力的范围，然后根据这个能力范围来统计跳转次数。

在ip route_output slowdown()函数2453行处，如果路由查询结构的子网掩码位数是0，并且路由类型是RTN_UNICAST单播类型且没有指定发送设备，则调用fib_select_default()函数查找路由信息结构；该函数在/net/ipv4/fib_frontend.c中。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ fib_select_default()

```c
119 void fib_select_default(struct net *net,  
120 const struct flowi \* flp, struct fib_result \* res)  
121{ 
```

122 struct fib_table \* tb;   
123 int table $=$ RT_TABLE_MAIN;//使用主路由函数表   
124 #ifdef CONFIG_IPMULTIPLE_TABLES   
125 if(res->r == NULL || res->r->action != FR_ACT_TO_TB)   
126 return;   
127 table $=$ res->r->table;//使用路由规则指定的路由函数表   
128 #endif   
129 tb $=$ fib_get_table(net, table); //取得路由函数表指针   
130 if (FIB_RES_GW(\*res)&& FIB_RES_NH(\*res).nh_scope $= =$ RT_SCOPELINK)   
131 tb->tb_select_default(tb, flp, res);   
132}

先确定使用哪一个路由函数表，起初 table 指定为主路由函数表 ID，但是在启用了多路由函数表选项时就指定为路由规则的路由函数表 ID。129 行调用 fib_get_table() 函数取得指定路由函数表结构指针。这个函数的过程前面描述了，如果跳转结构显示指定了网关并且指定了子网范围，则调用路由函数表结构的 tb_select_default() 函数查找对应的路由信息结构，这在 fib_hash_table() 函数中被挂入了 fn_hash_select_default()，函数在 /net/ipv4/fib_hash.c 中。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ fib_select_default() $\rightarrow$ fn_hash_select_default()

280 static void   
281 fn_hash_select_default(struct fib_table \* tb, const struct flowi \* flp, struct fib_result   
\*res)   
282{   
283 int order, lastidx;   
284 struct hlist_node \* node;   
285 struct fib_node \*f;   
286 struct fib_info \*fi $=$ NULL;   
287 struct fib_info \*last_resort;   
288 struct fn_hash \*t $=$ (struct fn_hash \*)tb-> tb_data;   
289 struct fn-zone \* fz $=$ t-> fn Zones[0];   
290   
291 if (fz == NULL)   
292 return;   
293   
294 lastidx $= -1$ .   
295 last_resort $=$ NULL;

order $= -1$ ：  
read_lock(&fib_hash_lock);  
//外层循环路由节点队列  
hlist_for_each_entry(f,node，&fz-> fz_hash[0]，fn_hash）{struct fib alias \*fa;list_for_each_entry(fa,&f->fnalias,fa_list）{//内层循环路由别名队列struct fib_info \*next_fi $=$ fa->fa_info;if（fa->fa_scope！ $=$ res->scope|| //查找符合范围和单播类型的路fa->fa_type！ $=$ RTN_UNICAST)continue;if（next_fi->fibpriority $\rightharpoondown$ res->fi->fibpriority)//检查路由信息线break；if（！next_fi->fib_nh[0].nh gw|| //检查路由信息结构中的跳转结构next_fi->fib_nh[0].nh_scope！ $=$ RT SCOPE LINK)continue;fa->fa_state| $=$ FA_S_ACCESSED;//路由别名设置访问标志if（fi $= =$ NULL）{if（next_fi！ $=$ res->fi）//如果找到的路由信息结构与查找的不同break;}else if（！fibdetect_death(fi,order,&last_resort，&lastidx，tb->tb_default)){//在邻居子系统中检测是否可以到达fib_result_assign(res,fi);//记录查找到的路由信息结构tb->tb_default $=$ order;//记录路由信息结构在队列中的序号goto out;fi $=$ next_fi;order++;1}  
}  
if（order $<  = 0\mid \mid$ fi $= =$ NULL）{//如果没有找到路由信息结构tb->tb_default $= -1$ //将路由信息的队列序号设为负值goto out;

if(!fibdetect_death(fi,order,&last_resort,&lastidx, tb->tb_default)){//在邻居子系统中检测路由是否可以到达 fib_result_assign(res,fi);//将路由信息结构记录到查询结果中 tb->tb_default $\equiv$ order;//将路由信息结构的队列序号记录到路由函数表中 goto out; } if(lastidx $\geq 0$ ) fib_result_assign(res,last_resort);//将最后认定的路由信息结构记录到结果 tb->tb_default $\equiv$ lastidx;//将最后认定的队列序号记录到路由函数表 ： read_unlock(&fib_hash_lock);

读者在理解这个函数时最好看一下路由结构及关系图，根据路由函数表、路由区、路由节点、路由别名、路由信息这些结构的链接关系来分析函数中的两层循环。

外层的循环是对路由节点队列的循环，内层的循环是对路由别名队列的循环。在内层的循环中先找到符合范围和单播路由类型的路由别名结构，然后再取得它记录的路由信息结构，还要对路由信息结构的优先级、网关和范围进行检查，并且设置路由别名的访问标记。order用来记录路由信息结构在队列中的序号，因此322行路由函数表记录下这个序号，根据这个序号值可以判断是否改变了路由信息结构。

代码中调用了两个函数 fibdetect_death()和fib_result_assign(), fib_result_assign()函数主要将res->fi记录下找到的路由信息结构fi;fibdetect_death()函数则在邻居子系统中检测路由是否可以达到，这个函数在/net/ipv4/fib_semantics.c中。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ fib_select_default() $\rightarrow$ fn_hash_select_default() $\rightarrow$ fibdetect_death()

352 int fibdetect_death(struct fib_info *fi, int order,  
353 struct fib_info \*last_resort, int \*lastidx, int dflt)  
354{  
355 struct neighbour \*n;  
356 int state $=$ NUD_NON;  
357 //在全局邻居表中查找邻居结构  
358 n = neighLOOKup(&arp_tbl, &fi->fib_nh[0].nh_gw, fi->fib_dev);  
359 if (n){  
360 state $=$ n-> nud_state;

```c
361 weigh_release(n);  
362 }  
363 if(state == NUD_REACHABLE)//检测可以到达  
364 return 0;  
365 if((state&NUD_VALID)&&order != dflt)//状态正常但是队列序号与原来不同  
366 return 0;  
367 if((state&NUD_VALID))  
368 (*lastidx<0 && order > dflt)) { //状态正常或者上一次记录的序号是负值而且当前的//队列序号大于原来的
```

369 \*last_resort $=$ fi; //记录当前的路由信息结构  
370 \*lastidx $=$ order;//记录当前的队列序号  
371 }  
372 return 1;  
373}

358行处调用neigh LOOKUP()函数查找邻居结构，这个函数的内容放在邻居子系统中讲述。找到了符合条件的邻居结构后根据其状态nud_state来判断是否可以达到；nud是Network Unreachability Detection的缩写，意思是未连接的网络检测。365行将路由函数表记录的序号与当前路由信息结构的序号相对比，如果邻居结构正常而这里的路由信息序号不同了就说明改变了路由信息结构，于是返回到fn_hash_select_default()函数319行使用新的路由信息结构，记录新的序号。

返回到ipRoute_output slowdown()函数2456行处，如果键值的源地址还是空，就要使用路由信息结构fib_prefix记录的地址。发送设备dev_out也要调整为查找结果中的设备指针，并且路由键值记录的发送设备号也要调整。

最后来到make-route标号处的内容。在前面过程中，只要明确了源地址、目标地址和发送设备，则都要跳转到这里进入ip_mkroute_output()函数中创建一个新的路由表rtable并插入到缓存中。这个函数在/net/ipv4/route.c中。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inlet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ ip-route_output_key() $\rightarrow$ ip-route_output Slow() $\rightarrow$ ip_mkroute_output()

2260static int ip_mkroute_output(struct rtable \* \*rp,   
2261 struct fib_result \*res,   
2262 const struct flowi \*fl,   
2263 const struct flowi \* oldflp,   
2264 struct net_device \*dev_out,   
2265 unsigned flags)   
2266{   
2267 struct rtable \*rth $=$ NULL;

//创建新的路由表

```c
2268 int err = __mkroute_output(&rth, res, fl, oldflp, dev_out, flags);  
2269 unsigned hash;  
2270 if (err == 0) {  
2271 hash = rt_hash(oldflp->fl4.dst, oldflp->fl4_src, oldflp->oif); //计算哈希值  
2272 err = rt_internal_hashhashCode, rth, rp); //由哈希值确定位置插入队列  
2273 }  
2274  
2275 return err;  
2276} 
```

2268行处调用了_mkroute_output()函数来创建新的路由表，这个函数在同一个文件中。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ ip_mkroute_output() $\rightarrow$ __mkroute_output()

2140static int __mkroute_output(struct rtable \* \* result,   
2141 struct fib_result \*res,   
2142 const struct flowi \*fl,   
2143 const struct flowi \* oldflp,   
2144 struct net_device \*dev_out,   
2145 unsigned flags)   
2146{   
2147 struct rtable \*rth;   
2148 struct in_device \*in_dev;   
2149 u32 tos $=$ RT_FL_TOS(oldflp);   
2150 int err $= 0$ .   
2151 //源地址是回接地址，但是发送设备不支持回接就返回   
2152 if (ipv4_is_loopback(fl-> fl4_src) &&！(dev_out-> flags&IFF_LOOPBACK))   
2153 return -EINVALID;   
2154   
2155 if (fl-> fl4.dst == htonl(0xFFFFFFF))//目标地址是否为极限地址   
2156 res->type $=$ RTN_BROADCAST;//设置查询结果返回的类型为广播类型   
2157 else if (ipv4_isMULTicast(fl-> fl4.dst))//目标地址是否为组播地址   
2158 res->type $=$ RTN Multicast;///设置查询结果返回的类型为组播类型   
2159 else if (ipv4_is_lbcast(fl-> fl4.dst) || ipv4_is_zeronet(fl-> fl4.dst))/  
播地址或者零网地址   
2160 return -EINVALID;   
2161

```c
2162 if (dev_out-> flags & IFF_LOOPBACK) //发送设备支持回接
2163 flags |= RTCF_LOCAL; //增加本地路由标志
2164
2165 /* get work reference to inet device */
2166 in_dev = in_dev_get(dev_out); //取得配置结构
2167 if (!in_dev)
2168 return -EINVALID;
2169
2170 if (res->type == RTN_BROADCAST) {//如果是广播类型
2171 flags |= RTCF_BROADCAST | RTCF_LOCAL; //增加广播和本地路由标志
2172 if (res->fi) {//放弃使用路由信息结构
2173 fib_info_put(res->fi);
2174 res->fi = NULL;
2175 }
2176 } else if (res->type == RTN Multicast) {//如果是组播地址
2177 flags |= RTCF Multicast | RTCF_LOCAL; //增加组播和本地路由标志
2178 if (!ip_check_mc(in_dev, oldflp->fl4.dst, oldflp->fl4_src,
oldflp->proto))
2180 flags &= ~RTCF_LOCAL;
2181 /* If multicast route do not exist use
2182 default one, but do not gateway in this case.
2183 Yes, it is hack.
2184 */
2185 if (res->fi && res->prefixlen < 4) {//放弃使用路由信息结构
2186 fib_infoPut(res->fi);
2187 res->fi = NULL;
2188 }
2189 }
2190
2191
2192 rth = dst_alloc(&ipv4.dstOPS); //分配路由表结构
2193 if (!rth)
2194 err = -ENOUBFS;
2195 goto cleanup;
2196 }
2197
2198 atomic_set(&rth->u.dst._refcnt, 1); //设置路由表的使用计数
2199 rth->u.dst.flags = DST_HOST; //路由项中增加目标主机标志
2200 if (IN_DEV_CONF_GET(in_dev, NOXFRM)) //如果配置结构中指定无 XFRM 框架 
```

```txt
rth-> u.dst.flags |= DST_NOXFRM; //路由项中增加无XFRM框架标志 if (IN_DEV_CONF_GET(in_dev, NOPOLICY))//如果配置结构指定无策略 rth-> u.dst.flags |= DST_NOPOLICY; //路由项中增加无策略标志
```

```lisp
rth->fl.fl4.dst = oldflp->fl4.dst; //记录原来指定的目标地址  
rth->fl.fl4_tos = tos; //记录TOS  
rth->fl.fl4_src = oldflp->fl4_src; //记录原来指定的源地址  
rth->fl.oif = oldflp->oif; //记录原来的发送设备  
rth->fl_mark = oldflp->mark; //记录原来的掩码  
rth->rt.dst = fl->fl4.dst; //记录目标地址，来自于上一级函数对路由查询结果  
rth->rt_src = fl->fl4_src; //记录源地址，来自于上一级函数对路由查询结果  
rth->rt_iif = oldflp->oif? : dev_out-> ifindex; //确定接收设备  
/* get references to the devices that are to be hold by the routing cache entry */  
rth->u.dst.dev = dev_out; //确定发送设备  
dev_hold(dev_out); //增加发送设备的使用计数  
rth->idev = in_dev_get(dev_out); //记录下发送设备  
rth->rt Gateway = fl->fl4.dst; //使用目标地址作为网关  
rth->rt_spec.dst = fl->fl4_src; //使用源地址作为指定目标  
rth->u.dst.output = ip_output; //设置路由项的发送数据包函数  
rth->rt_genid = atomic_read(&rt_genid); //记录随机数  
RT_CACHE_STAT_INC(out_slow_tot); //递增计数器  
if (flags & RTCF_LOCAL) { //如果是本地转发  
rth->u.dst-input = ip_localdeliver; //设置路由项的接收数据包函数  
rth->rt_spec.dst = fl->fl4.dst; //使用目标地址作为指定目标}  
if (flags & (RTCF_BROADCAST | RTCF Multicast)) { //如果是广播或者组播  
rth->rt_spec.dst = fl->fl4_src; //使用源地址作为指定目标  
if (flags & RTCF_LOCAL && ! (dev_out->flags & IFF_LOOPBACK)) { //如果是本地转发但是设备不支持回接  
rth->u.dst.output = ip_mc_output; //设置路由项的发送数据包函数  
RT_CACHE_STAT_INC(out Slow_mc); //递增使用计数}  
}  
if (res->type == RTNMulticast) {
```

```c
2239 if (IN_DEV_MFORWARD(in_dev) &&  
2240 ! ipv4_is_localMULTicast(oldflp->fl4.dst)) {  
2241 rth->u.dst-input = ip_mr_input;  
2242 rth->u.dst.output = ip_mc_output;  
2243 }  
2244 }  
2245 #endif  
2246 }  
2247  
2248 rt_set_nexthop(rth, res, 0); //根据查询结果设置路由表  
2249  
2250 rth->rt_flags = flags; //记录标志  
2251  
2252 *result = rth; //返回新建的路由表  
2253 cleanup:  
2254 /* release work reference to inlet device */  
2255 in_dev_put(in_dev); //放弃对配置结构的使用  
2256  
2257 return err;  
2258} 
```

该函数实际上分配了一个新的哈希表结构，然后根据前边函数传递的两个路由键值 old-flp、fl 以及跳转内容对路由表进行初始化设置。上面代码加了比较详细的注释，读者可以根据注释对过程进行了解。

分配路由表是通过dst_alloc()函数来完成的，应该注意传递给它的路由操作表dst ops指针是ipv4.dstOps。在路由表的初始化函数ip_rt_init()里介绍过该结构，它是IPv4的路由操作函数表。

```python
static struct dstOps ipv4_dst OPS = {
    .family = AF_INET,
    .protocol = __constant_htons(ETH_P_IP),
    .gc = rt_garbage_collect,
    .check = ipv4_dst_check,
    .destroy = ipv4_dst_destroy,
    .ifdown = ipv4_dst_ifdown,
    .negative_advice = ipv4_negative_advice,
    .link_failure = ipv4_link_failure,
    .update_pmtu = ip_rt_update_pmtu,
    .local_out = __ip_local_out, 
```

```txt
. entry_size = sizeof(struct rtable),
. entries = ATOMIC_INIT(0),
); 
```

我们结合它的定义来看dst_alloc()函数，该函数在/net/core/dst.c中。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output Slow() $\rightarrow$ ip_mkroute_output() $\rightarrow$ __mkroute_output() $\rightarrow$ dst_alloc()

163void \* dst_alloc(struct dstOps \* ops)   
164{   
165 struct dst_entry \* dst;   
166 //检查"供应是否紧张"调用回收函数   
167 if (ops->gc && atomic_read(&ops->entries) $>$ ops->gc thresh){   
168 if (ops->gc(ops))   
169 return NULL;   
170 }   
171 dst $=$ kmem_cache_zalloc(opst->kmem_cachep,GFP_AROMIC);//分配路由项   
172 if(!dst)   
173 return NULL;   
174 atomic_set(&dst->__refcnt,0);//初如化使用计数   
175 dst->ops $=$ ops;//记录路由项函数表   
176 dst->lastuse $=$ jiffies;//记录当前时间   
177 dst->path $=$ dst;//用于IPsec流量控制   
178 dst->input $=$ dst->output $=$ dst_discard;//暂时使接收和发送函数放弃数据包   
179#if RT_CACHE_DEBUG >= 2   
180 atomic_inc(&dst_total);   
181 #endif   
182 atomic_inc(&ops->entries);//递增路由项函数表的计数器   
183 return dst;   
184}

dst_entry结构应该说是专用于路由目的的结构体，它的定义解释放在后面过程。171行代码从表面上看kmem_cache_zalloc()函数是对dst_entry结构的分配，实际是对路由表结构的分配。这可以从ipv4.dstOps结构对entry_size的设置看出：sizeof(struct rtable)，它指明路由表的长度为路由项的长度。在内核调用ip_rt_init()函数初始化时会创建路由项的高速缓存ip.dst_cache，它正是以路由表的长度为依据建立的。

```txt
ipv4.dstOps.kmem_cachep =  
    kmem_cache_create("ip.dst_cache", sizeof(struct rtable), 0,  
        SLAB_HWCACHE Align|SLAB_PANIC, NULL);
```

因而，在这个高速缓存中分配路由项是按照路由表结构的长度进行的；路由项位于路由表结构的开始处，所以说路由项的指针也是路由表的指针。（dst 在 rtable 结构体的 union 中，位于 rtable 的开始处。）因此，本书称 dst_alloc() 函数的目的是分配路由表结构，只有分配成功才有后面对路由表的初始化过程。

最后的2248行调用rt_set_nexthop()函数，根据前面查询结果设置路由表。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ ip_mkroute_output() $\rightarrow$ __mkroute_output() $\rightarrow$ rt_set_nexthop()

static void rt_set_nexthop(struct rtable \*rt，struct fib_result \*res，u32 itag)   
{ struct fib_info \*fi $=$ res->fi;//取得查找结果中的路由信息结构 if(fi）{//如果存在路由信息结构，如果指定了网关并且跳转范围在子网范围 if(FIB_RES_GW(\*res)&& FIB_RES_NH(\*res).nh_scope $= =$ RT_SCOPELINK) rt->rt Gateway $=$ FIB_RES_GW(\*res);//记录查找结果中的网关地址 memcpy(rt->u.dst.metrics,fi->fib.metrics, sizeof(rt->u.dst.metrics));//复制路由信息的负载值到路由项中 if(fi->fib_mtu == 0）{//如果路由信息的MTU值为0 rt->u.dst.metrics[RTAX_MTU-1] $=$ rt->u.dst.dev->mtu;//复制设备的MTU if(dst_metric_lockled(&rt->u.dst,RTAX_MTU)&& rt->rt Gateway！ $=$ rt->rt_dst&& rt->u.dst.dev->mtu>576) rt->u.dst.metrics[RTAX_MTU-1] $=$ 576; //修改MTU的负载值 } #ifdef CONFIG_NET_CLS_ROUTERt->u.dst.tclassid $=$ FIB_RES_NH(\*res).nh_tclassid;   
#endif }else//没有路由信息结构就使用设备的MTU rt->u.dst.metrics[RTAX_MTU-1] $=$ rt->u.dst.dev->mtu;//记录设备的MTU //设置各种负载值 if(dst_metric(&rt->u.dst,RTAX_HOPLIMIT) $= = 0$ ） rt->u.dstmetrics[RTAX_HOPLIMIT-1] $=$ sysctl_ip_default_ttl;//跳转限制值 if(dst_metric(&rt->u.dst,RTAX_MTU) $>$ IP_MAX_MTU) rt->u.dstmetrics[RTAX_MTU-1] $=$ IP_MAX_MTU;//MTU值 if(dst_metric(&rt->u.dst,RTAX_ADVMSs) $= = 0$ ） rt->u.dst.metrics[RTAX_ADVMSs-1] $=$ max_t(unsigned int,rt->u.dst.dev->mtu

```c
ip_rt_min_advmss); //对外公开MSS值 if (dst_metric(&rt->u.dst, RTAX_ADVMMSS) > 65535 - 40) rt->u.dst.metrics[RTAX_ADVMMSS-1] = 65535 - 40; //对外公开MSS值 #ifdef CONFIG_NET_CLS-route #ifdef CONFIG_IP_multiple_TABLES set_class_tag(rt, fib/rules_tclass(res)); #endif set_class_tag(rt, itag); #endif rt->rt_type = res->type; //记录查询结果中的路由类型 } 
```

回到ip_mkroute_output()函数2271行，为新建的路由表调用rt_hash()函数计算一个哈希值。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ ip_mkroute_output() $\rightarrow$ rt_hash()

```c
static inline unsigned int rt_hash(_be32 daddr, _be32 saddr, int idx) {
return jhash_3words(_force u32)(_be32)(daddr), (_force u32)(_be32)(saddr), idx, atomic_read(&rt_genid)) & rt_hash_mask;
} 
```

它利用目标地址（即服务器地址）、本机地址（即源地址）以及发送设备的索引号和随机数计算一个hash值。然后在2272行调用rt_intern_hash()函数，将新建的路由表rtable链入缓存哈希桶队列中。读者最好对照路由表结构示意图阅读该函数。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ __ip-route_output_key() $\rightarrow$ ip-route_output_slow() $\rightarrow$ ip_mkroute_output() $\rightarrow$ rt_inter_hash()

```c
947 static int rt_intern_hash(unsigned hash, struct rtable *rt, struct rtable **rp)  
948 {  
949 struct rtable *rth, * * rthp; // 确定队列位置  
950 unsigned long now;  
951 struct rtable *cand, * * candp; 
```

952 u32 min_score;   
953 int chain_length;   
954 int attempts $=$ !insoftirq();

```txt
956 restart:  
957 chain_length = 0;  
958 min_score = ~(u32)0;  
959 cand = NULL;  
960 candp = NULL;  
961 now = jiffies; 
```

```txt
963 rthp = &rt_hash_table[hash].chain; //获取所在队列的第一个路由表  
964  
965 spin_lock_bh(rt_hash_lock_addrHash));  
966 while ((rth = *rthp) != NULL) {  
967 if (rth->rt_genid != atomic_read(&rt_genid)){//检查随机数  
968 *rthp = rth->u.dst.rt_next; //指向下一个路由表  
969 rt_free(rth); //释放前面的路由表  
970 continue;  
971 }  
/*路由键值内容是否相同，是否同属一个网络空间，
```

如果相同就说明队列中已经存在相同的路由表结构，把它移动到队列的最前端 $\times /$

```txt
972 if (compare_keys(&rth->fl, &rt->fl) && compare_netns(rth, rt)) {
973 / * Put it first */
974 * rthp = rth->u.dst.rt_next; //指向下一个路由表
975 / *
976 * Since lookup is lockfree, the deletion
977 * must be visible to another weakly ordered CPU before
978 * the insertion at the start of the hash chain.
979 * /
980 rcu_assign_pointer(rth->u.dst.rt_next,
981 rt_hash_table[hash].chain); //它的下一个路由表为原来的第一个路由表
982 / *
983 * Since lookup is lockfree, the update writes
984 * must be ordered for consistency on SMP.
985 * /
986 rcu_assign_pointer(rt_hash_table[hash].chain, rth);
987 
```

dst_use(&rth->u.dst,now);//调整时间 spin_unlock_bh(rt_hash_lock_addr hash)); rt_drop(rt);//放弃新建的路由表 \*rp $=$ rth;//记录找到的路由表 return 0; } if(!atomic_read(&rth->u.dst._refcnt)){//如果还没有使用过 u32 score $=$ rt_score(rth);//计算一个参考分值 if(score $<   =$ min_score）{//找到得分最小的路由表 cand $=$ rth; candp $=$ rthp; min_score $=$ score; 1 chain_length++;//搜索路由表计数 rthp $=$ &rth->u.dst.rt_next;//指向下一个路由表 } if(cand){//找到了合适的路由表 /\*ip_rt_gc_elasticity used to be average length of chain \*length,when exceeded gc becomes really aggressive. \* The second limit is less certain.At the moment it allows \*only 2 entries per bucket.We will see. \*/ if(chain_length>ip_rt_gc_elasticity）{//此时搜索路由表计数超过平均值 \*candp $=$ cand->u.dst.rt_next;//指向它后面的路由表 rt_free(cand); //放弃找到的路由表，达到回收目的 } } /* Try to bind route to arp only if it is output route or unicast forwarding path.

//如果新建路由表是单播类型，且路由键值没有指定网络设备

```c
if (rt->rt_type == RTN_UNICAST || rt->fl.iif == 0) { int err = arp_bind.neighbour(&rt->u.dst); if(err) { spin_unlock_bh(rt_hash_lock_addrHash)); if(err != -ENOUBFS){ rt_drop(rt); //放弃新建路由表 return err; } /* Neighbour tables are full and nothing can be released. Try to shrink route cache, it is most likely it holds some neighbour records. */ if (attempts-->0){//调整回收的底线，回收部分路由表缓 int saved_elasticity = ip_rt_gc_elasticity; int saved_int = ip_rt_gc_min_interval; ip_rt_gc_elasticity = 1; ip_rt_gc_min_interval = 0; rtGarbage_collect(&ipv4DstOps); ip_rt_gc_min_interval = saved_int; ip_rt_gc_elasticity = saved_elasticity; goto restart; } if(net_ratelimit()//打印限速 printf(KERN WARNING "Neighbour table overflow.\n"); rt_drop(rt); //放弃新建的路由表 return -ENOUBFS; } 
```

//新建路由表的下一个表指针调整为队列中第一个路由表

```txt
rt->u.dst.rt_next = rt_hash_table[hash].chain;  
if RT_CACHE_DEBUG >= 2 
```

```txt
if (rt->u.dst. rt_next) { //调试信息
struct rtable *trt;
printf(KERN_DEBUG "rt_cache @ %02x: " NIPQUAD_FMT, hash, 
```

1064 NIPQUAD(rt->rt.dst));   
1065 for(trt $=$ rt->u.dst.rt_next;trt;trt $=$ trt->u.dst.rt_next)   
1066 printf("．"NIPQUAD_FMT，NIPQUAD(trt->rt.dst));   
1067 printf("\n");   
1068 }   
1069#endif   
1070 rt_hash_table[hash].chain $=$ rt;//将新建路由表链入到队列的最前端   
1071 spin_unlock_bh(rt_hash_lock_addr hash));   
1072 \*rp $=$ rt;//记录新建路由表   
1073 return 0;   
1074}

首先，根据传递过来的hash键值在全局哈希桶队列rt_hash_table中找到所在队列的第一个路由表；然后沿着这个队列逐一对比，通过compare_keys()对比路由键值，通过compare_netns()对比网络空间。如果这些对比成功了，就说明路由缓存中已经存在该路由表了，就将找到的路由表通过rcu_assign_pointer()函数移动到队列的最前端。此时不需要新建的路由表，就通过rt_drop()释放它。如果没有找到，则还会在循环中通过积分计算的方法选出一个可以回收的路由表，顺便节省一下路由表空间。

如果新建的路由表为单播类型并且路由键值没有指定网络设备，就要调用arp_bind neighbouring()函数查找邻居结构neighbour并记录到路由项中；这个查找过程将在邻居子系统中看到。最后，将新建的路由表插入到哈希表桶队列中。

最后返回到ip route_connect()函数165行处，此时通过__ip route_output_key()函数查找或者新建了路由表，接下来要调整路由键值的目标地址和源地址。

147static inline int ip-route_connect(struct rtable \* \*rp，_be32 dst,  
148 __be32 src,u32 tos,int oif,u8 protocol,  
149 __be16 sport，_bel6 dport，struct sock \*sk,  
150 int flags)  
151{  
165 err $=$ _ip-route_output_key(net，rp,&fl);  
166 if(err)  
167 return err;  
168 fl.f14.dst $= (\texttt{*}\mathrm{rp}) - >$ rt.dst;//使用路由表的目标地址  
169 fl.f14_src $= (\texttt{*}\mathrm{rp}) - >$ rt_src;//使用路由表的源地址  
170 ip_rtPut(\*rp);//递减路由表的路由项计数器  
171 \*rp $\equiv$ NULL;  
172

```c
173 security_sk_classify_flow(sk, &fl);  
174 return ip-route_output_flow(net, rp, &fl, sk, flags);  
175} 
```

调整了路由键值的内容后，需要递减路由项的使用计数，清空路由表指针。174行进入ip route_output_flow()函数再次查找路由表调整路由地址。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_connect() $\rightarrow$ ip-route_output_flow()

2571int ip-route_output_flow(struct net \*net，struct rtable $^ { \text{串} }$ rp，struct flowi \*flp,  
2572 struct sock \*sk，int flags)  
2573{  
2574 int err;  
2575  
2576 if((err $=$ _ip-route_output_key(net，rp，flp))！ $= 0$ //查找路由表  
2577 return err;  
2578  
2579 if(flp->proto){//如果指明了协议  
2580 if(!flp->fl4_src)//源地址为空  
2581 flp->fl4_src = (\*rp)->rt_src;//使用路由表的源地址  
2582 if(!flp->fl4.dst)//目标地址为空  
2583 flp->fl4.dst = (\*rp)->rt.dst;//使用路由表的目标地址  
2584 err $=$ _xfrmLOOKUP((struct dst_entry \*)rp，flp，sk，  
2585 flags?XFRM LOOKUP_WAIT:0);//通过XFRM调整路由项  
2586 if(err == -EREMOTE)  
2587 err $=$ ipv4.dst.blackhole(rp，flp);//黑洞处理  
2588  
2589 return err;  
2590 }  
2591  
2592 return 0;  
2593}

这个函数再次调用__ip-route_output_key()函数在路由缓存表中查找路由表，找到后也要调整键值中的地址。然后进入__xfrm lookup()函数；这个函数是XFRM框架的内容，实现了IPsec安全协议。如果内核启用了XFRM选项，则所有的数据包都要经过__xfrm.lookup()函数进行处理；它对数据包进行加密、认证和封装，到达目标后再通过XFRM解包和检查，从而保证了数据包的安全传递。由于篇幅所限本书不能对IPsec的内容进行分析，因而未启用XFRM选项，__xfrm lookup()实际为空函数。

# 4.6 基于输入方向的路由表查找与创建

前面介绍的查找、创建路由过程都是数据包基于输出方向进行的，其接收方向的查找、创建过程却要从函数ip route_input()开始，这个函数在/net/ipv4/route.c中。我们以ip_rcv_finish()函数调用它的过程为例来分析。ip_rcv_finish()函数放在第12章中详细分析。

先了解调用ip route_input()函数时的参数，第一个参数skb是接收的数据包，第二个参数daddr是IP头部记录的目标地址，第三个参数saddr是IP头部记录的源地址，第四个参数tos是IP头部记录的服务类型，第五个参数是接收数据包的网络设备。

do_softirq() $\rightarrow$ net_rx_action() $\rightarrow$ process_backlog() $\rightarrow$ netif_receive_skb() $\rightarrow$ deliver_SKb() $\rightarrow$ ip_rcv() $\rightarrow$ ip_rcv_finish() $\rightarrow$ ip-route_input()

```c
2072 int ip-route_input(struct sk_buffer *skb, __be32 daddr, __be32 saddr,  
2073 u8 tos, struct net_device *dev)  
2074 {  
2075 struct rtable *rth;  
2076 unsigned hash;  
2077 int iif = dev-> ifindex; //获取设备的ID  
2078 struct net *net;  
2079  
2080 net = dev_net(dev); //取得设备所属网络空间  
2081 tos &= IPTOS_RT_MASK; //调整后的TOS  
2082 hash = rt_hash(daddr, saddr, iif); //计算HASH值  
2083  
2084 rcu_read_lock();  
/*循环在路由表队列中查找路由表，条件是地址相同、服务类型相同、掩码相同、网络空间相同、随机数相同*/ 
```

```c
2085 for (rth = rcu_dereference(rt_hash_table[hash].chain); rth;  
2086 rth = rcu_dereference(rth-> u.dst. rt_next)) {  
2087 if (((rth-> fl.fl4.dst ^ daddr) |  
2088 (rth-> fl.fl4_src ^ saddr) |  
2089 (rth-> fl.iif ^ iif) |  
2090 rth-> fl.oif |  
2091 (rth-> fl.fl4_tos ^ tos)) == 0 &&  
2092 rth-> fl_mark == skb-> mark &&  
2093 net_eq(dev_net(rth-> u.dst.dev), net) && 
```

2094 rth->rt_genid $= =$ atomic_read(&rt_genid){   
2095 dst_use(&rth->u.dst,jiffies);   
2096 RT_CACHE_STAT_INC(in_hit);//递增命中计数   
2097 rcu_read_unlock();   
2098 skb->rtable $\equiv$ rth;//记录找到的路由表   
2099 return 0;   
2100 }   
2101 RT_CACHE_STAT_INC(in_hlist_search);//递增搜索计数   
2102 }   
2103 rcu_read_unlock();   
2104   
2105 /\*Multicast recognition logic is moved from route cache to here.   
2106 The problem was that too many Ethernet cards have broken/missing   
2107 hardware multicast filters :-(As result the host on multicasting   
2108 network acquires a lot of useless route cache entries, sort of   
2109 SDR messages from all the world. Now we try to get rid of them.   
2110 Really, provided software IP multicast filter is organized   
2111 reasonably (at least, hashed), it does not result in a slowdown   
2112 comparing with route cache reject entries.   
2113 Note, that multicast routers are not affected, because   
2114 route cache entry is created eventually.   
2115 \*/   
2116 if (ipv4_isMULTicast(daddr)){//如果目标地址是组播类型   
2117 struct in_device \*in_dev;   
2118   
2119 rcu_read_lock();   
2120 if ((in_dev $=$ _in_dev_get_rcu(dev))！ $=$ NULL）{//获取配置结构   
2121 int our $=$ ip_check_mc(in_dev,daddr,saddr,   
2122 ip hdr(skb)->protocol);//检查目标地址是否为本地配置组播地址   
2123 if (our   
2124 #ifdef CONFIG_IP_MROUTE   
2125 ||(!ipv4_is_localMULTicast(daddr)&&   
2126 IN_DEV_MFORWARD(in_dev))   
2127 #endif   
2128 ）{   
2129 rcu_read_unlock();   
2130 return ip-route_input_mc(skb,daddr,saddr,   
2131 tos,dev,our); //为组播创建路由表   
2132

```c
2133 }   
2134 rcu_read_unlock();   
2135 return -EINVALID;   
2136 }   
2137 return ip-route_input_slow(skb, daddr, saddr, tos, dev); //创建路由表   
2138 }
```

函数先是在哈希队列中查找路由表，如果找到了就记录到数据包结构中，如果没有找到就进入ip route_input Slow()函数创建路由表。这个函数的过程要比输出方向的ip route_output_flow()函数复杂得多，这里它将根据目标地址来创建本地路由表或者转发路由表（这样区分是因挂入的接收函数不同）。

do_softirq() $\rightarrow$ net_rx_action() $\rightarrow$ process.Backlog() $\rightarrow$ netif_receive_skb() $\rightarrow$ deliver_SKb() $\rightarrow$ ip_rcv() $\rightarrow$ ip_rcv_finish() $\rightarrow$ ip-route_input() $\rightarrow$ ip-route_input Slow()

```c
1889 static int ip-route_input slows(struct sk BUFF *skb, __be32 daddr, __be32 saddr, u8 tos, struct net_device *dev)  
1890 {  
1891 {  
1892 struct fib_result res;  
1893 struct in_device *in_dev = in_dev_get(dev); //获取设备的配置结构  
1894 struct flowif1 = {.nl_u = {.ip4_u = {.daddr = daddr, .saddr = saddr, .tos = tos, .scope = RT_SCOPE_UNIVERSE, }},  
1895 {  
1896 .mark = skb->mark,  
1897 .tos = tos,  
1898 .scope = RT_SCOPE_UNIVERSE,  
1899 }  
1900 .mark = skb->mark,  
1901 .iif = dev->ifindex); //初始化路由键值  
1902 unsigned flags = 0;  
1903 u32 itag = 0;  
1904 struct rtable *rth;  
1905 unsigned hash;  
1906 __be32 spec.dst;  
1907 int err = -EINVALID;  
1908 int free_res = 0;  
1909 struct net *net = dev_net(dev); //获取网络空间结构  
1910  
1911 /* IP on this device is disabled. */  
1912  
1913 if (!in_dev)//如果没有配置结构就返回 
```

```txt
goto out;   
/\* Check for the most weird martians, which can be not detected by fibLOOKUP. 
```

```c
if (ipv4_isMULTicast(saddr) || ipv4_is_lbcast(saddr))  
    ipv4_is_loopback(saddr))//检查源地址是否为广播、组播、回接地址类型  
goto martian_source; //源地址错误，跳转
```

//如果目标地址为极限地址，或者源地址和目标地址同时为零地址  
if（daddr $= =$ htonl(0xFFFFFFF）||（saddr $= = 0$ &&daddr $= = 0$ ）） gotobrd_input;//广播输入，跳转

if (ipv4_is_zeronet(saddr))//如果源地址为零网地址类型 goto martian_source; //源地址错误, 跳转  
```javascript
/\*Accept zero addresses only to limited broadcast; \*I even do not know to fix it or not. Waiting for complains:-) 
```

```c
if (ipv4_is_lbcast(daddr) || ipv4_is_zeronet(daddr)) ||  
ipv4_is_loopback(daddr))//如果目标地址为广播地址、零网地址或者回接地址  
goto martiandestination; //目标地址错误  
/  
* Now we are ready to route packet.  
*/ 
```

free_res = 1; //默认为释放查找结果   
```txt
if((err = fibLOOKup(net, &fl, &res)) != 0) //通过路由函数表查找目标地址 if(!IN_DEV FORWARD(in_dev))//如果设备不支持转发 goto e_hostunreach; //主机无法到达错误，跳转 goto no-route; //无法到达，跳转
```

```txt
RT_CACHE_STAT_INC(in Slow_tot); //递增计数
```

if (res.type == RTN_BROADCAST) //如果目标地址是广播类型 goto brd_input; //广播输入, 跳转  
```txt
if (res.type == RTN_LOCAL) //如果目标地址是本地类型
```

```c
int result;  
result = fibValidate_source(saddr, daddr, tos, net-> loopback_dev-> ifindex, dev, &spec.dst, &itag); //检查源地址  
if (result < 0)  
goto martian_source; //源地址错误  
if (result)  
flags |= RTCF_DIRECTSRC;  
spec.dst = daddr; //记录目标地址  
goto local_input; //本地输入，跳转  
}  
if (!IN_DEV FORWARD(in_dev)) //如果设备不支持转发  
goto e_hostunreach; //主机无法到达，跳转  
if (res.type != RTN_UNICAST) //如果目标地址不是单播类型  
goto martiandestination; //目标地址错误，跳转  
//创建用于转发的路由表  
err = ip_mkroute_input(skb, &res, &fl, in_dev, daddr, saddr, tos);  
done; //返回点  
in_dev_put(in_dev); //递减使用计数  
if (free_res) //如果需要释放查找结果  
fib_resPut(&res); //释放查找结构  
out: return err;  
brd_input; //广播输入  
if (skb->protocol != htons(ETH_P_IP)) //如果不是IP标准协议  
goto einelval; //无法识别，跳转  
if (ipv4_is_zeronet(saddr)) //如果源地址是零网类型  
spec.dst = inlet_select_addr(dev, 0, RT_SCOPE_LINK); //选择目标地址  
else {  
err = fib Validate_source(saddr, 0, tos, 0, dev, &spec.dst, &itag); //检查源地址  
if (err < 0)  
goto martian_source; //源地址错误，跳转  
if (err)  
flags |= RTCF_DIRECTSRC;  
}  
flags |= RTCF_BROADCAST; //增加广播标志
```

res.type = RTN_BROADCAST; //设置地址类型为广播类型
RT_CACHE_STAT_INC(in_brd); //递增计数  
```txt
local_input://本地输入
rth = dst_alloc(&ipv4.dstOps); //创建路由表
if (!rth)
goto e_nobuffs; //空间不足,跳转
rth->u.dst.output = ip_rt_bug; //设置输出方向的函数
rth->rt_genid = atomic_read(&rt_genid); //记录随机数
atomic_set(&rth->u.dst._refcnt, 1); //初始化计数器
rth->u.dst.flags = DST_HOST; //设置路由标志
if (IN_DEV_CONF_GET(in_dev, NOPOLICY))
rth->u.dst.flags |= DST_NOPOLICY;
rth->fl.fl4.dst = daddr; //记录目标地址
rth->rt.dst = daddr;
rth->fl.fl4_tot = tos; //记录TOS
rth->fl_mark = skb->mark; //记录掩码
rth->fl.fl4_src = saddr; //记录源地址
rth->rt_src = saddr;
# ifdef CONFIG_NET_CLS_ROUT
rth->u.dst.tclassid = itag;
#endif
rth->rt_iif =
rth->fl.iif = dev->ifindex; //记录网络设备的ID
rth->u.dst.dev = net->loopback_dev; //记录回接设备
dev_hold(rth->u.dst.dev); //递增使用计数
rth->idev = in_dev_get(rth->u.dst.dev); //记录网络设备结构
rth->rt_GATEway = daddr; //记录路由网关
rth->rt_spec.dst = spec.dst; //记录指定目标地址
rth->u.dst-input = ip_localdeliver; //设置输入函数
rth->rt_flags = flags|RTCF_LOCAL; //增加本地路由标志
if (res.type == RTN_UNREACHABLE) { //如果目标地址不可到达
rth->u.dst-input = ip_error; //设置处理错误函数
rth->u.dst.error = -err; //设置错误码
rth->rt_flags & = ~RTCF_LOCAL; //清除本地路由标志 
```

2030 rth->rt_type $=$ res.type;//记录目标地址类型  
2031 hash $=$ rt_hash(daddr,saddr,fl.iif);//计算哈希值  
2032 err $=$ rt_interr_hashHash,rth,&skb->rtable);//将路由表插入哈希队列并记录到数  
//据包中  
2033 goto done;//返回  
2034 noRoute://无法到达  
2036 RT_CACHE_STAT_INC(in_no-route);//递增计数  
2037 spec_dst $\equiv$ inlet_select_addr(dev,0,RT_SCOPE_UNVERSE);//确定指定目标  
2038 res.type $=$ RTN_UNREACHABLE;//设置为不可到达类型  
2039 if(err $= = -$ ESRCH)  
2040 err $=$ -ENETUNREACH;  
2041 goto local_input;//本地输入，跳转  
2042 /\*  
2044 \* Do not cache martian addresses: they should be logged (RFC1812)  
2045 /\*  
2046 martiandestination://目标地址错误  
2047 RT_CACHE_STAT_INC(in_martian_dst);//递增计数  
2048 #ifdef CONFIG_IP-routeverbose  
2049 if(IN_DEV_LOG MARTIANS(in_dev)&&net_ratelimit())  
2050 printf(KERN_WARNING"martian destination "NIPQUAD_FMT"from"  
2051 NIPQUAD_FMT",dev%s\n",  
2052 NIPQUAD(daddr),NIPQUAD(saddr)，dev->name);  
2053 #endif  
2054  
e_hostunreach://主机无法到达错误  
2056 err $=$ -EHOSTUNREACH;  
2057 goto done;  
2058  
eInvalid://无法识别  
2060 err $=$ -EINVALID;  
2061 goto done;  
2062  
e_nobufs://空间不足  
2064 err $=$ ENOBIFS;  
2065 goto done;  
2066  
2067 martian_source://源地址错误

```c
2068 ip_handle_martian_source(dev, in_dev, skb, daddr,ADDR); //打印错误信息
2069 goto e Invalidate;
2070}
```

函数的主旨就是创建路由表，但是分两种情形，第一种是针对目标地址是转发的情况，则调用ip_mkroute_input()函数创建转发路由表；第二种是针对目标地址是广播类型或者本地类型的情况，这里直接分配路由表并初始化，并指定下一步的处理函数为ip_localdeliver()。对于转发情况调用的ip_mkroute_input()函数，读者可以参照注释理解。

```c
static int ip_mkroute_input(struct sk_buffer *skb, struct fib_result *res, const struct flowi *fl, struct in_device *in_dev, __be32 daddr, __be32 saddr, u32 tos)  
{ struct rtable *rth = NULL; //初始化路由表指针为空 int err; unsigned hash; # ifdef CONFIG_IP-route_MULTIPATH //多路径选项 if(res->fi && res->fi->fib_nhs > 1 && fl->oif == 0) fib_selectMULTIPath(fl, res); //优先跳转结构 #endif /* create a routing cache entry */ err = __mkroute_input(skb, res, in_dev, daddr, saddr, tos, &rth); //创建路由表 if (err) return err; /* put it into the cache */ hash = rt_hash(daddr, saddr, fl->iif); //计数哈希值 return rt_inter_hashHash(rth, &skb->rtable); //将路由表链入哈希队列并记录到数据包中 } static int __mkroute_input(struct sk_buffer *skb, struct fib_result *res, struct in_device *in_dev, __be32 daddr, __be32 saddr, u32 tos, struct rtable * * result) { 
```

```c
struct rtable *rth;
int err;
struct in_device *out_dev;
unsigned flags = 0;
__be32 spec_dst;
u32 itag; 
```

/\*getaworkingreferencetotheoutputdevice\*/   
out_dev $\equiv$ in_dev_get(FIB_RES_DEV(\*res));//取得输出设备的配置结构 if(out_dev $= =$ NULL）{//如果没有配置结构就打印出错信息返回 if(net_ratelimit()) printf(KERN_CRIT"Buginip-route_input"\_.slow().Please,report\n"); return-EINVALID;   
1

```c
err = fibValidate_source(saddr, daddr, tos, FIB_RES_OIF(*res), in_dev->dev, &spec.dst, &itag); //检查源地址 if (err < 0) {
ip_handle_martian_source(in_dev->dev, in_dev, skb, daddr, saddr); //打印源地址错误信息 
```

```txt
err = -EINVALID;  
goto cleanup;  
}  
if (err)  
flags |= RTCF_DIRECTSRC; 
```

//检查配置结构和地址

```c
if (out_dev == in_dev && err &&  
(IN_DEV_SHARED_MEDIA(out_dev) ||  
inet_addr_onlink(out_dev,ADDR,FIB_RES_GW(*res)))  
flags |= RTCF_DOREIRECT; 
```

if（skb->protocol！=htons(ETH_P_IP)）{//如果不是标准IP协议\*/Not IP(i.e.ARP).Do not create route，if it is\*invalid for proxy arp.DNAT routes are always valid.\*/if（out_dev $= =$ in_dev）{

```c
err = -EINTR;  
goto cleanup;  
}  
}  
rth = dst_alloc(&ipv4dstOps); //创建路由表  
if (!rth) {  
    err = -ENOBUFS;  
    goto cleanup;  
}  
atomic_set(&rth->u.dst._refcnt, 1); //初始化使用计数  
rth->u.dst.flags = DST_HOST; //初始化路由标志  
if (IN_DEV_CONF_GET(in_dev, NOPOLICY))  
    rth->u.dst.flags |= DST_NOPOLICY;  
if (IN_DEV_CONF_GET(out_dev, NOXFRM))  
    rth->u.dst.flags |= DST_NOXFRM;  
rth->fl.fl4.dst = daddr; //记录目标地址  
rth->rt.dst = daddr;  
rth->fl.fl4_tot = tos; //记录TOS  
rth->fl_mark = skb->mark; //记录掩码  
rth->fl.fl4_src = saddr; //记录源地址  
rth->rt_src = saddr;  
rth->rt_GATEway = daddr; //记录路由网关地址  
rth->rt_iif =  
    rth->fl.iif = in_dev->dev->ifindex; //记录设备的ID  
rth->u.dst.dev = (out_dev) -> dev; //记录网络设备结构  
dev_hold(rth->u.dst.dev); //递增设备使用计数  
rth->idev = in_dev_get(rth->u.dst.dev); //记录发送设备  
rth->fl.oif = 0;  
rth->rt_spec.dst = spec.dst; //记录指定目标地址  
rth->u.dst-input = ip_forward; //设置输入函数  
rth->u.dst.output = ip_output; //设置发送函数  
rth->rt_genid = atomic_read(&rt_genid); //记录随机数  
rt_set_nexthop(rth, res, itag); //根据查询结构设置路由表  
rth->rt_flags = flags; //记录路由标志 
```

\*result $\equiv$ rth;//返回路由表指针err $= 0$ cleanup:/\*release the working reference to the output device \*/in_dev_put(out_dev);//递减输出设备使用计数return err;

这里将转发路由表的输入函数指定为ip_forward()，它和本地路由表的ip_localdeliver()函数将在接收数据包过程中被调用，读者将在第12章dst_input()函数中看到如何调用这两个函数。

# 通知链

Linux内核为了及时响应某些到来的事件，采取了通知链的方式来执行指定函数。每一个事件都对应一个通知链节点，内核中的notifier_block就是通知链节点的结构定义。

# 代码清单5.1notifier_block结构定义

```c
struct notification_block {
    int (*notifier_call)(struct notification_block *, unsigned long, void *); //通知调用的函数
    struct notification_block *next; //指向下一个通知节点，从而组成链队
    int priority; //优先级
}; 
```

其内部的notifier_call函数指针表示通知链节点要运行的函数。它通过next指针形成了通知链节点的“队列”，故称之为内核的通知链。priority代表优先级别，保证了通知链节点按一定的顺序来执行。本书并不打算以概念解析的方法使读者学习通知链，而采取“用中理解”的方法使读者在分析代码的过程中领会它的内涵。

# 5.1 设备通知链节点的挂入

我们利用devinet_init()函数的过程分析设备通知链的建立。devinet_init()函数调用register_netdevice.notifier()将节点结构ip_netdev.notifier挂入内核的设备通知链netdev_chain队列中。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ devinet_init() $\rightarrow$ register_netdevice.notifier()
1141 int register_netdevice.notifier(struct notifier_block *nb)
1142 {
1143 struct net_device *dev;
1144 struct net_device *last;
1145 struct net *net;
1146 int err;
1147
1148 rtnl_lock();

//将nb节点链入设备通知链netdev_chain中

```c
err = rawNotificationier_chain_register(&netdev_chain, nb);  
if (err)  
    goto unlock;  
if (dev.boot_phase)  
    goto unlock;  
for_each_net(net) {  
    for_each_netdev(net, dev) { //依次调用节点的“通知”函数  
    err = nb->notifier_call(nb, NETDEV_REGISTER, dev);  
    err = notifier_to_errno(err);  
    if (err)//如果出现错误则跳转到rollback处回滚操作  
        goto rollback;  
    if (! (dev->flags & IFF_UP))  
        continue;  
    nb->notifier_call(nb, NETDEV_UP, dev);  
}  
}  
unlock;  
rtlun_unlock();  
return err;  
rollback:  
last = dev;  
for_each_net(net) {  
    for_each_netdev(net, dev) {  
        if (dev == last)  
            break;  
        if (dev->flags & IFF_UP) {  
            nb->notifier_call(nb, NETDEV_GOING_DOWN, dev);  
            nb->notifier_call(nb, NETDEV_DOWN, dev);  
        }  
        nb->notifier_call(nb, NETDEV_UNREGISTER, dev);  
} 
```

```txt
1187 raw.notifier_chain_unregister(&netdev_chain, nb);  
1188 goto unlock;  
1189} 
```

Linux内核有一个设备通知链队列头netdev_chain，它通过宏定义的方式出现在内核中。

static RAW_NOTIFYHEAD(netdev_chain); #define RAW_NOTIFYHEAD(name) struct raw_NOTIFYhead name $=$ \ $\mathrm{RAW\_NOTIFIER\_INIT(name)}$ struct raw_NOTIFYhead { structnotifier_block\*head; }； #define RAW_NOTIFY_INIT(name）{ .head $\equiv$ NULL}

可以看出，netdev_chain 其实也是一个通知链节点，只不过它在定义时被初始化为空。结合 devinet_init() 函数 1643 行来理解 1149 行，这里调用 raw_NOTIFY_chain_register() 实质是将 ip_netdev_NOTIFY 通知链挂入到 netdev_chain 中。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ devinet_init() $\rightarrow$ register_netdevice.notifier() $\rightarrow$ raw.notifier_chain_register()
336 int raw.notifier_chain_register(struct raw.notifier_head *nh,
337 structnotifier_block*n)
338 {
339 returnnotifier_chain_register(&nh->head, n);
340 }
21 static intnotifier_chain_register(structnotifier_block **nl,
22 structnotifier_block*n)
23 {
24 while((*nl) != NULL) {
25 if(n->priority > (*nl) ->priority)
26 break;
27 nl = &((*nl) -> next);
28 }
29 n->next = *nl;
30 rcu_assign_pointer(*nl, n);
31 return 0;
32 }

挂入操作根据通知链的优先级priority来确定在队列位置；rcu_assign_pointer是直接赋值操作 $*\mathrm{nl} = \mathrm{n}$ ，它的定义比较复杂，考虑到SMP系统而采用了RCU保护机制。

回到register_netdevice.notifier()函数中，1152行的dev.boot_phase初始值为1，代表内核处于启动阶段。启动阶段是不允许调用通知链节点函数的，因为这时还不会有任何接收“通知”的设备；dev.boot_phase又是个全局变量，内核在net_dev_init()函数中将其修改为0表示已经度过启动阶段。net_dev_init()函数在内核的initcall机制所处位置为4，而初始函数inet_init()所处位置为5，因而net_dev_init()函数会先将dev.boot_phase置0，因此1152行的检查会顺利通过。

1154行的两层循环则调用所有的通知链节点notifier_call()函数，即依次处理init_net网络空间中全部已经注册的通知事件，告诉目标设备盼望的事件发生了。

对于ip_netdev.notifier结构，其notifier_call挂入的是inetdev_event()。注意，1156行和1164行两次调用了inetdev_event()函数，分别传递了NETDEV_REGISTER(注册设备)和NETDEV_UP(启用设备)作为参数event。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ devinet_init() $\rightarrow$ register_netdevice_notifier() $\rightarrow$ INETDEV_event()
1032 static int inetdev_event(struct notifier_block * this, unsigned long event,
1033 void *ptr)
1034 {
1035 struct net_device * dev = ptr;
1036 struct in_device * in_dev = __in_dev_get_rtnl(dev);
1037
1038 ASSERT_RTNL();
1039
1040 if (!in_dev) {
1041 if (event == NETDEV_REGISTER) {
1042 in_dev =INETDEV_init(dev);
1043 if (!in_dev)
1044 return notifier_from_errno(-ENOMEM);
1045 if (dev->flags & IFF_LOOPBACK) {
1046 IN_DEV_CONF_SET(in_dev, NOXFRM, 1);
1047 IN_DEV_CONF_SET(in_dev, NOPOLICY, 1);
1048 }
1049 }
1050 goto out;
1051 }

```c
switch (event) {  
case NETDEV_REGISTER: //注册设备  
printk(KERN_DEBUG "inetdev_event: bug\n");  
dev->ip_ptr = NULL;  
break;  
case NETDEV_UP://启用设备  
if (dev->mtu < 68)  
break;  
if (dev->flags & IFF_LOOPBACK) {  
struct in_ifaddr * ifa;  
if ((ifa = inet_alloc_ifa()) != NULL) {  
ifa->ifa_local =  
    ifa->ifa_address = htons(INADDR_LOOPBACK);  
    ifa->ifa_prefixlen = 8;  
    ifa->ifa_mask =INET.make_mask(8);  
    in_dev_hold(in_dev);  
    ifa->ifa_dev = in_dev;  
    ifa->ifa_scope = RT_SCOPE_HOST;  
    memcpy(ifa->ifa_label, dev->name, IFNAMSIZ);  
   INET_insert_ifa(ifa);  
}  
}  
ip_mc_up(in_dev);  
break;  
case NETDEV_DOWN://关闭设备  
ip_mc_down(in_dev);  
break;  
case NETDEV_CHANGEMTU://改变设备的MTU  
if (dev->mtu >= 68)  
break;  
/*MTU failed under 68, disable IP */  
case NETDEV_UNREGISTER://撤销设备  
inetdevdestroy(in_dev);  
break;  
case NETDEV_CHANGENAME://设备重命名  
/*Do not notify about label change, this event is* not interesting to applications using netlink. */  
inetdev_changenome(dev, in_dev); 
```

```c
1092   
1093 devinet_sysctl_unregister(in_dev);   
1094 devinet_sysctl register(in_dev);   
1095 break;   
1096 }   
1097 out:   
1098 return NOTIFY_DONE;   
1099} 
```

我们分别来看这两次调用情形，第一次调用参数 event 是 NETDEV_REGISTER，函数调用 __in_dev_get_rtnl() 根据网络设备结构 struct net_device 得到配置结构，这就是 struct in_device 结构指针；in_device 结构包含了关于 IPV4 的一些地址信息。

# 代码清单5.2 in_device结构定义

```c
struct in_device
{
    struct net_device *dev; //指向对应的 net_device 网络设备结构
    atomic_t refcnt; //使用计数器，防止结构被释放
    int dead; //与 refcnt 联合决定是否释放结构
    struct in_ifaddr *ifa_list; /*IPV4 的地址队列 */
    rwlock_t mc_list_lock; //组播队列锁
    struct ip_mc_list *mc_list; /*IPV4 的组播过滤队列 */
    spinlock_t mc_tomb_lock;
    struct ip_mc_list *mc_tomb;
    unsigned long mr_v1 Seen;
    unsigned long mr_v2 Seen;
    unsigned long mr_maxdelay;
    unsigned char mr_qrv;
    unsigned char mr_gqRunning;
    unsigned char mr_ifc_count;
    struct timer_list mr_gq_timer; /*通用查询计时器*/
    struct timer_list mr_ifc_timer; /*接口改变的计时器*/
    struct neigh_parms *arp_parms;
    struct ipv4_devconf cnf;
    struct rcu_head rcu_head);
};
```

我们可以通过ifconfig命令或者ip命令来设置该结构的内容。这个结构通过其dev指针和网卡net_device->ip_ptr建立关联，可以通过这两个指针找到对方的的结构体。结构中的refcnt代表引用计数可以用来确定结构是否可以被内存管理释放和回收，只要为0就可以执

行回收。

如果还没有建立网卡的 in_device, 则结构就会执行 inetdev_init() 创建一个 in_device 结构并初始化, 还要与网卡的 net_device 结构体 ip_ptr 建立关联。

第一次调用，NETDEVRegister 作为参数时说明内核在初始化设备，因而还不能使用 in_device，所以断开与 in_device 结构的关联。这样一来前面辛苦创建的 in_device 就成了“孤儿”，到下次调用时还会再分配一个新的 in_device 结构，这无疑是 2.6.26 版本的一个 bug。

第二次调用，参数 event 是 NETDEV_UP 时说明要启用设备。先检查 dev 中的最大传输单元值 mtu 是否小于 68 个字节，这个数值是 IPV4 规定的最小值。接着检查设备是否为本地回接设备，如果是回接设备则声明了一个 in_ifaddr 结构变量 ifa，这是 IPV4 的地址结构；然后分配 ifa 的空间，随即对其进行了初始化设置，包括本地地址都设置为了本地回接地址，也就是 127.0.0.1。

ifa_local 和 ifa_address 的值形成了一个通道，ifa_local 是本地（本机）地址，而 ifa_address 是外部的地址。1066 行初始化了网络掩码需要的 ifa_prefixlen 和 ifa_mask 的值（可以用 ifconfig 或者 ip 命令修改它们），ifa_dev 记录下 in_device 的结构指针；接着是地址范围 ifascope（也可以通过 ifconfig 命令进行修改），如果设置了本地地址是 127.0.0.1 并且内外部地址相同，则范围就只限于本机了。

接下来保存网卡的名称到 ifa_label 中。在 in_device 结构中有一个地址结构队列 ifa_list，既然是本地回接地址，则必须通过 inset_insert_ifa() 函数将 ifa 插入到 ifa_list 队列中。由 inlet_insert_ifa() 函数根据 ifa 的掩码地址范围和状态标志等内容确定插入的位置，并且在 inlet_insert_ifa() 函数的最后会通过 rtmsg_ifa 和 blocking.notifier_call_chain 函数向内核发送通知，调用通知链来更新路由表；netlink 的监听进程也会知道这个新增的 ifa 结构变量。

# 5.2 地址通知链节点的挂入

我们以前面ip_fib_init()函数的过程来看地址通知节点的挂入，在该函数1064行处调用register_inetaddr.notifier()函数注册地址通知链节点fib_inetaddr.notifier。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_inetaddr.notifier()
int register_inetaddr.notifier(struct notifier_block *nb)
{
    return blocking.notifier_chain_register(&inetaddr.chain, nb);
}

内核中的inetaddr_chain地址通知链是通过宏定义声明的。

static BLOCKING_NOTIFY_HEAD(inetaddr_chain);   
# define BLOCKING_NOTIFY_HEAD(name) struct blocking.notifier_head name = BLOCKING_NOTIFY_INIT(name)   
# define BLOCKING_NOTIFY_INIT(name){ .rwsem $=$ _RWSEM_INITIALIZER((name).rwsem), .head $=$ NULL}

通知链节点结构封装在structblockingNotificationhead结构中，这个结构还包括一个读/写信号量，因此结构的名称寓意为“可阻塞通知链头”。_RWSEM_INITIALIZER是对读/写信号量的初始化，BLOCKINGNotificationHEAD宏将通知链头head初始化为NULL。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_inetaddr.notifier() $\rightarrow$ blocking不是一个notifier_chain_register()
int blocking.notifier_chain_register(struct blocking.notifier.head *nh,
                          struct notifier.block *n)
{
    int ret;
    /* 当进程还没有切换且中断保持关闭时，不调用down_write()函数 */
    if (unlikely(system_state == SYSTEM_BOOTING))
        return notifier_chain_register(&nh->head, n);
    down_write(&nh->rwsem);
    ret = notifier_chain_register(&nh->head, n);
    up_write(&nh->rwsem);
    return ret;
}

这里也是调用notifier_chain_register()来将通知节点插入到inetaddr_chain链中，只不过根据系统初始化状态而确定是否对rwsem信号量的操作，这是一种可读可写的信号量，远比自旋锁要方便和灵活。此处并没有像register_netdevice.notifier()函数那样调用fib_inetaddr_notifier节点中的call函数，接下来我们会看到其专门的调用函数。

# 5.3 通知链的调用和执行

内核中有一个专门调用通知链函数notifier_call_chain()，这里结合一个实例来说明到达这个函数的具体经过。为了让读者有个清晰的认识，本书以网卡的驱动程序为例，重点以DM9000网卡的驱动程序作为分析依据。内核启用DM9000网卡时，会执行DM9000驱动文

件中的初始化函数 dm9000probe(); 这个函数先调用 register_netdev() 向内核登记注册自己的 net_device 设备结构。

dm9000probe() $\rightarrow$ register_netdev()   
intregister_netdev(struct net_device\*dev)   
{ interr; rtnl_lock(); /\* \*如果设备名称是一个格式字符说明调用函数想自动分配一个名称\*/ if(strchr(dev->name,'%')){ err $=$ dev_alloc_name(dev,dev->name); if(err<0) goto out; } err $=$ register_netdevice(dev);   
out: rtnl_unlock(); return err;

dev_alloc_name()为网卡设备分配一个名称并设置到dev->name中，然后调用register_netdevice()进一步完成设备结构的注册登记。

dm9000probe() $\rightarrow$ register_netdev() $\rightarrow$ register_netdevice() intregister_netdevice(struct net_device\*dev)   
{ /\*调用通知链\*/ ret $=$ call_netdevice.notifiers(NETDEVREGISTER，dev);

代码只列出了与通知链相关的部分, 可以看到向通知链发出了“新的设备出现了”的通知。

dm9000probe() $\rightarrow$ register_netdev() $\rightarrow$ register_netdevice() $\rightarrow$ call_netdevice.notifiers() int call_netdevice.notifiers(unsigned long val, struct net_device * dev) { return raw.notifier_call_chain(&netdev_chain, val, dev); } int raw.notifier_call_chain(struct raw.notifier_head * nh,

```c
unsigned long val, void *v)   
{ return __rawNotificationter_call_chain(nh, val, v, -1, NULL);   
} int __rawNotificationter_call_chain(struct rawNotificationier_head *nh, unsigned long val, void *v, int nr_to_call, int *nr Calls)   
{ return notificationier_call_chain(&nh->head, val, v, nr_to_call, nr Calls); } 
```

经过层层调用，最后到达了notifier_call_chain()函数，这里需要结合DM9000驱动的参数来看；nl指向netdev_chain链，val是NETDEV_REGISTER表示注册设备，v指向DM9000的网络设备结构dev，nr_to_call是一1，nrcalls是NULL。

74 static int __kprobesnotifier_call_chain(structnotifier_block\*\*nl,   
75 unsigned long val,void $\ast \mathbf{V}$ 76 int nr_to_call，int $\ast$ nr Calls)   
77 {   
78 int ret $=$ NOTIFY_DONE;   
79 structnotifier_block $\ast$ nb,\*next_nb;   
80   
81 nb $=$ rcu_dereference( $\ast$ nl);   
82   
83 while (nb &&nr_to_call){   
84 next_nb $=$ rcu_dereference(nb->next);   
85 ret $=$ nb->notifier_call(nb，val,v);   
86   
87 if (nr Calls)   
88 $(\ast$ nr Calls）++;   
89   
90 if((ret&NOTIFY_STOP_MASK) $= =$ NOTIFY_STOP_MASK)   
91 break;   
92 nb $=$ next_nb;   
93 nr_to_call--;   
94 1   
95 return ret;   
96

函数沿着 netdev_chain 的链依次调用各个通知节点的 notifier_call() 处理函数, 从而执行

fib_netdev.notifier 通知节点中的钩子函数 fib_netdev_event(), 进而完成 DM9000 设备的登记注册, 这个函数的过程前面已经看过了。

另外，本书前面也分析了内核如何通过socket的ops过渡到inet_STREAMOPS结构的过程，该结构中设置了ioct1系统调用的钩子函数。

```txt
.iocl = inlet_iocl, 
```

也就是对socket的ioctl()系统调用最终转到inet_ioctl()函数执行具体的操作指令，它的调用路线是ioct1(）->sys_ioctl()->do_vfs_ioctl()->vfs_ioctl()->sock_ioctl()->inet_ioctl()。如果是设置IP地址指令，则在这个函数中调用devinet_ioctl()函数，最终进入__inet_insert_ifa()函数。

```c
static int __inet_insert_ifa(struct in_ifaddr * ifa, struct nlmsghdr * nlh, u32 pid)  
{  
    blocking.notifier_call_chain(&inetaddr_chain, NETDEV_UP, ifa);  
} 
```

在该函数中调用“阻塞式通知链”函数blocking.notifier_call_chain()。

inet_ioctl() $\rightarrow$ devinet_ioctl() $\rightarrow$ inlet_set_ifa() $\rightarrow$ __inet_insert_ifa() $\rightarrow$ __blocking.notifier_call_chain()
int __blocking.notifier_call_chain(struct blocking.notifier_head *nh, unsigned long val, void *v, int nr_to_call, int *nr Calls)
{
    int ret = NOTIFY_DONE;
    /* We check the head outside the lock, but if this access is racy then it does not matter what the result of the test is, we re-check the list after having taken the lock anyway: */
    if (rcu_dereference(nh->head)) {
        down_read(&nh->rwsem);
        ret = notifier_call_chain(&nh->head, val, v, nr_to_call, nr Calls);
        up_read(&nh->rwsem);
    }
}

```lua
return ret; 
```

阻塞式是指增加了对RCU锁的使用，代码同样是通过notifier_call_chain()函数执行所有inetaddr_chain链上的通知节点函数。通过列举的两个过程来看，不论对哪一个通知链进行操作，最终都集中到notifier_call_chain()函数。

# netlink 概述

# 6.1 netlink 的创建

netlink 是一套 IP 服务协议, 代表着一种特殊的 socket 通信方式; 可以说, netlink 是 Linux 特有的, 它对于 Linux 内核与用户空间进行双向数据传输是非常好的方式。

在路由过程 nl_fibLOOKUP_init() 函数中，调用 netlink_kernel_create() 函数创建了 netlink 结构，现在分析它的创建过程。

```c
inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_pernet_subsys() $\rightarrow$ register_pernet_operations() $\rightarrow$ fib_net_init() $\rightarrow$ nl_fibLOOKUP_init() $\rightarrow$ netlink_kernel_create()

1357 struct sock \*   
1358 netlink_kernel_create(struct net \*net，int unit，unsigned int groups,   
1359 void（\*input)(struct sk BUFF \*skb),   
1360 struct mutex \*cb_mutex，struct module \*module)   
1361 {   
1362 struct socket \*sock;   
1363 struct sock \*sk;   
1364 struct netlink_sock \*nlk;   
1365 unsigned long \* Listeners $=$ NULL;   
1366   
1367 BUG_ON(! nl_table);   
1368   
1369 if(unit $<  0$ ||unit $> =$ MAX LINKS)   
1370 return NULL;   
1371   
1372 if (socket_create lite(PF_NETLINK，SOCK_DGRAM，unit,&sock))   
1373 return NULL;   
1374   
1375 /\*

```txt
\* We have to just have a reference on the net from sk, but dont \* get_net it. Besides, we cannot get and then put the net here. \* So we create one inside init_net and the move it to net. 
```

```txt
if (_netlink_create(&init_net, sock, cb_mutex, unit) < 0) goto out_sock_release_nosk; 
```

```txt
sk = sock->sk;  
sk_change_net(sk, net); 
```

```javascript
if (groups < 32) groups = 32; 
```

```txt
listeners = kzalloc(NLGRPSZ(groups), GFP_KERNEL);  
if (! listeners)  
    goto out_sock_release; 
```

```txt
sk->sk_data_ready = netlink_data_ready;  
if (input)  
    nlk_sk(sk) -> netlink_rcv = input; 
```

```txt
if (netlink_insert(sk, net, 0))  
    goto out_sock_release; 
```

$\mathrm{nlk} = \mathrm{nlk\_sk(sk)}$ nlk->flags $|\equiv$ NETLINK_KERNEL_SOCKET;

netlink_tablegrab();   
if(!nl_tableunit].registered){ nl_tableunit].groups $=$ groups; nl_tableunit].listeners $=$ listeners; nl_tableunit].cb_mutex $=$ cb_mutex; nl_tableunit].module $\equiv$ module; nl_tableunit].registered $= 1$ .   
}else{ kfree(listeners); nl_tableunit].registered $+ +$ .

```c
1415 netlink_table_ungrab();   
1416 return sk;   
1417   
1418 out_sock_release;   
1419 kfree(listeners);   
1420 netlink_kernel_release(sk);   
1421 return NULL;   
1422   
1423 out_sock_release_nosk;   
1424 sock_release(sock);   
1425 return NULL;   
1426 } 
```

函数中有一个数据结构 struct netlink_sock, 我们看一下它的定义。

# 代码清单6.1 netlink SOCK 结构定义

```c
struct netlink_sock {
    /* 结构体 sock 必须处于最前端，这样可以通过 sock 指针找到 netlink_sock */
    struct sock     sk; //与之相关联的 sock
    u32     pid; //当前进程的 ID
    u32     dst.pid; //目标进程的 ID
    u32     dst_group; //目标进程组号
    u32     flags; //标志位
    u32     subscriptions;
    u32    /groups; //进程组数量
    unsigned long   *groups; //组位图
    unsigned long   state; //状态位
    wait_queue_head_t wait; //等待队列
    // netlink_callback 结构用于显示路由表的内容，例如使用 ip route get 指令
    struct netlink_callback *cb;
    struct mutex         *cb_mutex; //互斥锁
    struct mutex         cb_def_mutex; //默认的互斥锁
    void       (*netlink_rcv)(struct sk_buffer *skb); //接收数据包的函数指针
    struct module        *module; //所属模块
};
```

这个结构用于 netlink 中 socket 的使用。1367 行的代码是为了调试目的；1369 行检查参数 UNIX 的范围，从 nl_fibLOOKUP_init() 传递的值是 NETLINK_FIB LOOKUP，其定义为 10。

```txt
define NETLINK_FIB LOOKUP 10 
```

1372行调用sock_create lite()函数创建netlink的socket。

```c
inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_pernet_subsys() $\rightarrow$ register_pernet_operations() $\rightarrow$ fib_net_init() $\rightarrow$ nl_fib.lookup_init() $\rightarrow$ netlink_kernel_create() $\rightarrow$ sock_create lite()

931 int sock_create lite (int family, int type, int protocol, struct socket \* \*res)   
932 {   
933 int err;   
934 struct socket \* sock $=$ NULL;   
935   
936 err $=$ security_SOCKET_create(family, type, protocol, 1);   
937 if (err)   
938 goto out;   
939   
940 sock $=$ sock_alloc();   
941 if (!sock){   
942 err $=$ -ENOMEM;   
943 goto out;   
944 }   
945   
946 sock->type $=$ type;   
947 err $=$ security_SOCKET_post_create(sock, family, type, protocol, 1);   
948 if (err)   
949 goto out_release;   
950   
951 out:   
952 \* res $=$ sock;   
953 return err;   
954 out_release:   
955 sock_release(sock);   
956 sock $=$ NULL;   
957 goto out;   
958 }

936 行根据内核是否打开了 CONFIGSECURITY_NETWORK 选项来调用 security_SOCKET_create(); 这个选项是为了保证网络安全而增加的 Security hooks 函数表功能, 如果没有打开该选项, 则 security_SOCKET_create()相当于空函数。

940 行也调用了 sock_alloc()，它在网络文件系统中分配一个用于 netlink 的 socket 结构，然后将 socket 的 type 类型指定为 SOCK_DGRAM 类型，也就是 UDP 协议类型。此后在 netlink_kernel_create() 函数 1381 行调用 __netlink_create() 函数，为 netlink 创建一个 sock 结

构并与新建的 socket 挂钩。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ ip_fib_init() $\rightarrow$ register_pernet_subsys() $\rightarrow$ register_pernet_operations() $\rightarrow$ fib_net_init() $\rightarrow$ nl_fibLOOKUP_init() $\rightarrow$ netlink_kernel_create() $\rightarrow$ __ netlink_create()

393 static int __netlink_create(struct net \* net, struct socket \* sock,   
394 struct mutex \* cb_mutex, int protocol)   
395 {   
396 struct sock \* sk;   
397 struct netlink SOCK \* nlk;   
398   
399 sock-> ops = &netlinkOps;   
400   
401 sk $=$ sk_alloc(net, PF_NETLINK,GFP_KERNEL, &netlink.proto);   
402 if(!sk)   
403 return -ENOMEM;   
404   
405 sock_init_data(sock,sk);   
406   
407 nlk $=$ nlk_sk(sk);   
408 if (cb_mutex)   
409 nlk-> cb_mutex $=$ cb_mutex;   
410 else{   
411 nlk-> cb_mutex $=$ &nlk-> cb_def_mutex;   
412 mutex_init(nlk-> cb_mutex);   
413 }   
414 init_waitqueue_head(&nlk-> wait);   
415   
416 sk-> sk-destruct $=$ netlink SOCK-destruct;   
417 sk-> sk_protocol $=$ protocol;   
418 return 0;   
419

401 行的 sk_alloc()函数完成对 sock 的创建, 接着要进行初始化; 399 行将 socket 的函数表 ops 指针指向了 netlinkOps; 401 行传递 netlink Proto 给 sock 的 sk Proto 指针。读者可以顺着 2.4 节的内容完成 sk_alloc()和 sock_init_data()的初始化过程。

由于 sock 指针在 netlink_sock 结构的最前端, 所以 nlk_sk() 函数可以通过 sock 的地址获得 netlink_sock 的指针 nlk。接着对 nlk 进行初始化, 包括对 cb_mutex 锁的设置, 然而 nl_fibLOOKUP_init 函数传递下来 cb_mutex 锁是 NULL。414 行通过 init_waitqueue_head() 函

数初始化 $\mathrm{nlk}\rightarrow$ wait等待队列。

回到 netlink_kernel_create()函数1385行，sk_change_net()函数使sk_net记录下所属的网络空间，并在内存中开辟一部分空间；listeners指向其开始处。1394行将数据处理函数sk_data_ready指向了netlink_data_ready()，这个函数除了调试以外没有其他功能。input参数传递下来是nl_fib_input()函数指针，这里设置进了netlink_rcv中。1402行增加了NETLINK_kernel_SOCKET标志，表示用于netlink的socket。

netlink_kernel_create()函数的其余内容是关于init_net网络空间和nl_table全局变量的初始化操作。

# 6.2 注册路由的 netlink

我们接合4.2节devinet_init()函数对rtnl register()的调用，来分析注册路由的netlink过程，则我们将会看到它使用另一个结构体来表示路由的netlink。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ devinet_init() $\rightarrow$ rtnl_register()
    void rtnl register (int protocol, int msgtype,
        rtnl_doit_func doit, rtnl_dumpit_func dumpit)
{
    if (_rtnl register(protocol, msgtype, doit, dumpit) < 0)
        panic("Unable to register rtnetlink message handler," "protocol = %d, message type = %d\n", protocol, msgtype);
}

函数调用了__rtlregister()。

inet_init() $\rightarrow$ ip_init() $\rightarrow$ ip_rt_init() $\rightarrow$ devinet_init() rtnl_register() $\rightarrow$ __rtnl register()
145 int __rtnl register (int protocol, int msgtype,
146 rtnl_doit_func doit, rtnl_dumpit_func dumpit)
147 {
148 struct rtnl_link *tab;
149 int msgindex;
150
151 BUG_ON(protocol < 0 || protocol >= NPROTO);
152 msgindex = rtm msgindex(msgtype);
153
154 tab = rtnl msghandlers[protocol];
155 if (tab == NULL) { //不存在则为 tab 数组分配空间

```c
156 tab = krealloc(RTM_NR_MSGTYPES, sizeof(*tab), GFP_KERNEL);  
157 if (tab == NULL)  
158 return -ENOBUFS;  
159  
160 rtnl msg handlers[protocol] = tab; //登记到队列数组中  
161 }  
162  
163 if (doit)  
164 tab[msgindex].doit = doit;  
165  
166 if (dumpit)  
167 tab[msgindex].dumpit = dumpit;  
168  
169 return 0;  
170 } 
```

代码148行准备了一个用于路由的netlink结构体struct rtnl_link，这个结构中封装着两个函数指针。

# 代码清单6.2 rtnl_link结构定义

```c
struct rtnl_link
{
    rtnl_doit_func   doit;
    rtnl_dumpit_func dumpit;
}; 
```

doit函数指针是在请求操作信息时使用的，而dumpit函数指针却是在释放操作时使用的。

我们看到，devinet_init()函数中共调用了3次rtnl register()函数(4.2节)，前两次调用分别传递了inet_rtm_newaddr()和inet_rtm_deladdr()，这两个是关于路由设备的地址处理函数将会被IPROUTE2使用的，这两个函数作为doit参数在164行被设置到rtnl_link结构中，而传递的参数dumpit为NULL。第三次调用rtnl register函数时，传递inet_dump_ifaddr作为dumpi参数在167行设置到rtnl_link结构中，doit在这次调用过程中则为NULL。

这3次调用的参数protocol都指定为PF_INET，参数msgtype(路由信息的类型)在3次调用中分别是RTM新加ADDR、RTM_DELADDR和RTM_GETADDR，它们代表创建、删除、获取设备的IP地址，这3个宏都与作为参数与doit函数对应。

在内核中有一个全局的 rtnl msg handlers 数组, 它是 rtnl_link 的队列数组, 这里先将参

数protocol即PF_INET作为数组下标查看是否已经存在对应的rtnl_link结构，如果没有存在则通过kmalloc()在内存中分配一组rtnl_link空间。kmalloc函数用于分配数组空间，分配的数组个数是RTM_NR_MSGTYPES，最终这个函数也是调用__kmalloc()内存分配函数。

分配了tab空间后，将数组rtnl msghandlers对应位置指向tab。应该注意，tab其实是rtnl_link数组(在C语言中数组和指针是等同的)。所以，rtnl msghandlers是队列数组，它的每个元素都指向rtnl_link数组，而数组下标则是根据参数msgtype来确定。152行通过rtm msgindex()函数计算tab数组下标msgindex，然后将doit和dumpit函数指针设置到对应的rtnl_link结构中。

# 6.3 通过 netlink 通信

我们以4.3节fn_hash_insert()函数调用rtmsg_fib()为例，分析netlink的通信过程。  
fn_hash_insert()→rtmsg_fib()

```c
306 void rtmsg_fib(int event, __be32 key, struct fib Alias *fa,  
307 int dst_len, u32 tb_id, struct nl_info *info,  
308 unsigned int nlm_flags)  
309 {  
310 struct sk_buffer *skb;  
311 u32 seq = info-> nlh? info-> nlh-> nlmsg_seq: 0; //确定顺序号  
312 int err = -ENOBUFS;  
313 //创建用于消息发送的数据包结构  
314 skb = nlmsg_new(fib_nlmsg_size(fa-> fa_info), GFP_KERNEL);  
315 if (skb == NULL)  
316 goto errout;  
317 //在数据包中建立消息结构记录路由信息  
318 err = fib_dump_info(skb, info-> pid, seq, event, tb_id,  
319 fa-> fa_type, fa-> fa_scope, key, dst_len,  
320 fa-> fa_tos, fa-> fa_info, nlm_flags);  
321 if (err < 0) {  
322 /* -EMSGSIZE implies BUG in fib_nlmsg_size() */  
323 WARNING_ON(err == -EMSGSIZE);  
324 kfree_skb(skb);  
325 goto errout;  
326 }  
327 err = rtnl_notify(skb, info-> nl_net, info-> pid, RTNLGRP_IPV4-route,  
328 info-> nlh, GFP_KERNEL); //发出数据包 
```

```c
329 errout:  
330 if (err < 0)  
331 rtnl_set_sk_err(info -> nl_net, RTNLGRP_IPV4ROUTE, err);  
332} 
```

根据fn_hash_insert()函数的调用参数来看，第一个参数event是RTM NewlyRoute，表示一个新的路由类型；第二个参数key，表示子网键值；第三个参数fa是找到或者新建的路由别名结构fa；第四个参数dst_len是配置结构中的fc.dst_len，表示IP地址的长度；第五个参数tb_id是路由函数表的ID；第六个参数info是配置结构中的fc_nInfo，是netlink的信息结构struct nl_info；最后一个参数nlm_flags是0。

函数开始先判断配置结构指定的info中是否指定了nh，它是struct nlmsghdr类型。

# 代码清单6.3 nl_info、nlmsghdr、rtmsg结构定义

```c
struct nl_info { //netlink 的信息
    struct nlmsghdr *nh; //消息头
    struct net *nl_net; //网络空间结构指针
    u32 pid; //发送端的进程 ID;
};
struct nlmsghdr //消息头
{
    __u32 nlmsg_len; /* 指明消息的总长度 */
    __u16 nlmsg_type; /* 消息类型 */
    __u16 nlmsg_flags; /* 附加的标志 */
    __u32 nlmsg_seq; /* 顺序号 */
    __u32 nlmsg.pid; /* 发送端的进程 ID */
};
struct rtmsg //路由消息
{
    unsigned char rtm_family; //协议族
    unsigned char rtm.dst_len; //目标地址长度
    unsigned char rtm_src_len; //源地址长度
    unsigned char rtm_tos; //服务类型 TOS
    unsigned char rtm_table; /* 路由函数表 ID */
    unsigned char rtm_protocol; /* 路由协议 */
    unsigned char rtm_scope; /* 路由范围 */
    unsigned char rtm_type; /* 路由类型 */
    unsigned rtm_flags; //标志位
};
```

nlmsghdr结构用于设置或保存netlink的消息头。注意，nlmsg_seq是顺序编号，用来保

证 netlink 消息的可靠传输。每发送一个消息，顺序号 nlmsg_seq 总是自动加 1。需要发送响应消息时，响应消息的顺序号应当与被响应消息的顺序号相等，此后响应消息的顺序号自动加 1，它应当是被响应消息的顺序号加 1。如果接收到的消息顺序号不是期望的顺序号，则表明该消息是一个新的消息；如果接收到的消息的顺序号是期望的顺序号，但它的响应号不等于上次发送消息的顺序号加 1，那么它也是新消息。

我们看到代码中 seq 变量根据 nlh 是否存在而取得这个顺序号, 如果还没有设置就从 0 开始, 接着调用 nlmsg_new() 函数创建一个用于消息的数据包。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ nlmsg_new()   
static inline struct sk_buffer $\text{串}$ nlmsg_new(size_t payload, gfp_t flags)   
return alloc_skb(nlmsg_total_size(payload), flags);

它调用alloc Skyl()函数分配一个数据包sk BUFF结构，这个函数及结构将在数据包发送章节中分析。这里只需要了解sk BUFF结构代表一个数据包，而这个数据包的大小则是在314行处调用fib_nlmsg_size()函数确定的。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ fib_nlmsg_size()   
static inline size_t fib_nlmsg_size(struct fib_info *fi)   
{ size_t payload $=$ NLMSGALIGN(sizeof(struct rtmsg)) +nla_total_size(4）/\*RTA_TABLE\*/ +nla_total_size(4）/\*RTA_DST\*/ +nla_total_size(4）/\*RTA_PRIORITY\*/ +nla_total_size(4）;/\*RTA_PREFSRC\*/ /\*space for nested metrics\*/ payload $+ =$ nla_total_size((RTAX_MAX $\ast$ nla_total_size(4)); if (fi->fib_nhs){ /\*Also handles the special case fib_nhs $= = 1$ /\* each nexthop is packed in an attribute \*/ size_t nhsize $=$ nla_total_size(sizeof(struct rtnexthop)); /\*may contain flow and gateway attribute \*/ nhsize $+ = 2$ \*nla_total_size(4); /\*all nexthops are packed in a nested attribute \*/ payload $+ =$ nla_total_size(fi->fib_nhs $\ast$ nhsize); } return payload;

函数计算一个有效的负载值 payload，计算过程中要考虑到一些附属内容所需空间，这当然包括了跳转结构。

分配了用于消息的数据包skb以后，接着调用fib_dump_info()函数初始化消息内容和头结构。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ fib_dump_info()   
947 int fib_dump_info(struct sk_buffer \*skb,u32 pid,u32 seq,int event,   
948 u32 tb_id,u8 type,u8 scope,_be32 dst,int dst_len,u8 tos,   
949 struct fib_info \*fi, unsigned int flags)   
950 {   
951 struct nlmsghdr \*nlh;   
952 struct rtmsg \*rtm;   
953 //初始化消息头   
954 nlh $=$ nlmsg_put(sk,bid,seq, event,sizeof( \*rtm)，flags);   
955 if (nlh $= =$ NULL)   
956 return -EMSGSIZE;   
957   
958 rtm $=$ nlmsg_data(nlh);//获得消息指针   
959 rtm->rtm_family $=$ AF_INET;   
960 rtm->rtm.dst_len $=$ dst_len;   
961 rtm->rtm_src_len $= 0$ .   
962 rtm->rtm_tot $=$ tos;   
963 if(tb_id<256)//检查路由函数表ID，允许自定义   
964 rtm->rtm_table $=$ tb_id;   
965 else   
966 rtm->rtm_table $=$ RT_TABLE_COMPAT;   
967 NLAPUT_U32(skb,RTA_TABLE,tb_id);   
968 rtm->rtm_type $=$ type;   
969 rtm->rtm_flags $=$ fi->fib_flags;   
970 rtm->rtm_scope $=$ scope;   
971 rtm->rtm_protocol $=$ fi->fib_protocol;   
972   
973 if (rtm->rtm.dst_len)   
974 NLAPUT_BE32(skb,RTADst,dst);//记录路由目标地址到数据包   
975   
976 if (fi->fibpriority)   
977 NLAPUT_U32(skb,RTA_PRIORITY,fi->fibpriority);//记录优先级   
978

if (rtnetlink_put_metricsskb, fi-> fib_metricss $< 0$ //记录负载值 goto nla_put_fail;   
if (fi-> fib preferringsrc) NLAPUTBE32(skb, RTA_PREFSRC, fi-> fib preferringsrc);//记录源地址   
if (fi->fib_nhs == 1){ if (fi->fib_nh->nh_gw)//记录网关地址 NLAPUTBE32(skb, RTA_GATEWAY, fi->fib_nh->nh_gw); if (fi->fib_nh->nh_oif)//记录设备ID NLAPUTU32(skb, RTA_OIF, fi->fib_nh->nh_oif);   
# ifdef CONFIG_NET_CLS_ROUT e if (fi->fib_nh[0].nh_tclassid) NLAPUTU32(skb, RTA_FLOW, fi->fib_nh[0].nh_tclassid);   
#endif   
}   
# ifdef CONFIG_IP_ROUT MULTIPATH if (fi->fib_nhs > 1){ struct rtnexthop \*rtnh; struct nlattr \*mp; mp = nla_nest_start(skb, RTA MULTIPATH);//建立多路径属性结构 if (mp == NULL) goto nla_putfailure; for nexthops(fi){//根据每一个跳转结构循环建立配置跳转结构保存到数据包 rtnh = nla_reserved_nohdr(skb, sizeof(\*rtnh)); if (rtnh == NULL) goto nlaPutfailure; rtnh->rtnh_flags = nh->nh_flags&0xFF; rtnh->rtnh_hops = nh->nh_weight - 1; rtnh->rtnh_ifindex = nh->nh_oif; if (nh->nh_gw)//记录跳转结构的网关地址到数据包 NLA PUT_BE32(skb, RTA_GATEWAY, nh->nh_gw);   
# ifdef CONFIG_NET_CLS_ROUT

```c
1017 if (nh-> nh_tclassid)  
1018 NLA.PUT_U32(skb, RTA_FLOW, nh-> nh_tclassid);  
1019 #endif  
1020 /\*length of rtnetlink header + attributes \*/  
1021 rtnh-> rtnh_len = nlmsg_get_pos(skb) - (void \*) rtnh;  
1022 } endfor_nextdrops(fi);  
1023  
1024 nla_nest_end(skb, mp); //记录全部长度到多路径属性结构中  
1025 }  
1026 #endif  
1027 return nlmsg_end(skb, nlh); //记录全部消息的总长度  
1028  
1029 nla_putfailure;  
1030 nlmsg_cancel(skb, nlh);  
1031 return -EMSGSIZE;  
1032 } 
```

这里先对skb缓冲结构进行检验，确定是否为消息结构分配了足够的空间，这是通过nlmsg_put函数检测的；它也调用__nlmsg_put()做一些初始化工作。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ fib_dump_info() $\rightarrow$ nlmsg_put()   
static inline struct nlmsghdr \* nlmsg_put(struct sk_buffer \* skb, u32 pid, u32 seq, int type, int payload, int flags)   
{ if (unlikely(skTAILroom(sk) $<  <$ nlmsg_total_sizeEGAyoad))//是否准备了足够的空间 return NULL; return NLmsgPut(sk, pid, seq, type, payload, flags);   
}   
static __inline__struct nlmsghdr\* _nlmsgput(struct sk_buffer \* skb, u32 pid, u32 seq, int type, int len, int flags)   
{ struct nlmsghdr \* nlh; int size $=$ NLMSG_LENGTH(len);//消息头结构长度 $^+$ 路由消息结构长度 //在数据包中开辟消息头和消息结构的空间，并取得开始处的消息头指针 nlh $=$ (struct nlmsghdr \*)skb_put(sk,BNLMSG ALIGN(size)); nlh-> nlmsg_type $=$ type; nlh-> nlmsg_len $=$ size; nlh-> nlmsg_flags $=$ flags; nlh-> nlmsg.pid $=$ pid; nlh-> nlmsg_seq $=$ seq;

```c
memset(NLMSG_DATA(nlh) + len, 0, NLMSGALIGN(size) - size); //其余初始化零
return nlh; 
```

skb_put()函数同样将留在数据包的内容一起分析，这里只需要知道它调整了数据包skb的数据块指针，获得了数据块中的nlmsghdr结构头指针nh。然后对它进行初始化设置，这包括消息类型type，它传递过来的RTM Newly ROUTE表明是一个新的路由类型；size是路由消息结构的大小；flags是0；pid是nl_info结构的info->pid，它是内核调用netlink时记录的进程号；seq就是前面讲述的消息顺序号。最后，还要把消息结构后面的负载内容清零。

fib_dummy_info()函数958行接着调用nlmsg_data()从消息头结构后面取得路由消息结构rtm。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ fib_dump_info() $\rightarrow$ nlmsg_data()   
static inline void $^ { \text{喜} }$ nlmsg_data(const struct nlmsghdr \*nh)   
{ return(unsigned char \*)nlh + NLMSG_HDRLEN;

路由消息结构紧跟在消息头结构后面，需要对这个路由消息结构rtm初始化，这些内容对照代码清单6.3的路由消息结构注释理解。966行路由函数表的ID很特殊，它表明可以使用内核以外的路由函数表，这时的路由函数表ID为RT_TABLE_COMPAT。

973 行实际是目标地址的长度, 这是从配置结构 $\mathrm{cfg} \rightarrow$ fc.dst_len 中传递过来的; 如果存在, 则调用 NLAPUTBE32() 以属性结构的形式保存路由目标地址到数据包中。

代码多处调用了NLAPUTBE32()和NLAPUTU32()，这两个宏差别在数值类型，分别是_be32和u32类型；其中，_be32是按位保存，u32则是按字节保存。这两个宏的调用区别是：对地址数据按位保存。

```c
define NLA_PUT_BE32 (skb, attrtype, value) \  
NLA_PUT_TYPE(skb, _, be32, attrtype, value)  
# define NLA_PUT_U32(skb, attrtype, value) \  
NLA_PUT_TYPE(skb, u32, attrtype, value)  
# define NLA_PUT_TYPE(skb, type, attrtype, value) \  
do {  
    type __tmp = value;  
    NLA_PUT(skb, attrtype, sizeof(type), &_, tmp);  
} while(0)  
# define NLA_PUT(skb, attrtype, attrlen, data) \  
do { 
```

```c
if (unlikely(nla_put(skb, attrtype, attrlen, data) < 0)) \  
goto nla_put_fail;  
} while(0)  
int nla_put(struct sk_buffer *skb, int attrtype, int attrlen, const void *data)  
{  
    if (unlikely(skbTAILROOM(skb) < nla_total_size(attrlen))//检查空间是否还充足 return - EMSGSIZE;  
    __nla_put(skb, attrtype, attrlen, data);  
    return 0;  
}  
void __nla_put(struct sk_buffer *skb, int attrtype, int attrlen, const void *data)  
{  
    struct nlattr *nla; //声明 netlink 属性结构  
    nla = __nla_reserved(skb, attrtype, attrlen); //在数据包中开辟属性结构空间 memcpy(nla_data(nla), data, attrlen); //保存数据，其位于属性结构后面  
}  
struct nlattr *__nla_reserved(struct sk_buffer *skb, int attrtype, int attrlen)  
{  
    struct nlattr *nla;  
    nla = (struct nlattr *) skbPut(skb, nla_total_size(attrlen)); //开辟属性结构空间  
    nla->nla_type = attrtype; //记录属性类型  
    nla->nla_len = nla Attr_size(attrlen); //记录包含数据在内的总长度  
    memset((unsigned char *) nla + nla->nla_len, 0, nla_padlen(attrlen)); //清零 return nla;  
}
```

这里把所有的相关代码都列出了，整个调用过程是在数据包中创建 netlink 的属性结构，并记录所要传递数据；属性结构用于内核与用户空间的通信。前面看过配置结构也是利用属性结构从用户空间将数据“搬运”到内核的，并且也看过了它的定义，现在既然也要使用 netlink 通信当然也必须使用该结构。

从宏的调用过程可以看出来，它实际上是在数据包中指定了保存属性结构的位置，然后将数据保存到属性结构的“数据区”；其数据并不包含在结构内部，而是紧跟在属性结构的后面。函数利用宏先后保存目标路由地址，路由优先级，路由源地址，网关，设备ID等内容到每一个属性结构中，注意，这些属性结构都在数据包中。979行调用的rtnetlink_put_metric()函数其实也是调用该宏将每个路由类型的负载值也保存到数据包中。

997行检查是否需要多次跳转，如果是多次跳转则还要再在数据包中创建一个属性结构，

其类型为多路径标识 RTA MULTIPATH; 这类属性结构只是为了提醒说明情况, 所以其数据部分为空。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ fib_dump_info() $\rightarrow$ nla_nest_start ()   
static inline struct nlattr \*nla_nest_start (struct sk BUFF \*skb, int attrtype)   
{ struct nlattr \*start $=$ (struct nlattr \*)skb.tail_pointer(skb); if (nla_put(skb, attrtype, 0, NULL) $<  0$ 1 return NULL; return start;

1005行宏循环沿着路由信息表的跳转结构队列，先是在数据包中调用nla_reserved_nohdr()函数分配“配置跳转结构”rtnexthop所需空间，分配个数与跳转结构数对应；nla_reserved_nohdr()函数分配时并没有为配置跳转结构创建属性结构。前面已经添加了多路径属性mp，由它作为整个配置结构队列的总领。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ fib_dump_info() $\rightarrow$ nla_reserved_nohdr ()   
void $^ { \text{喜} }$ nla_reserved_nohdr(struct sk_buffer \*skb,int attrlen)   
{ if (unlikely(skTAILroom(sk)< NLAALIGN(attrlen))//空间是否充足 return NULL; return _nla_reserved_nohdr(sk,attrlen);   
}   
void $^ { \text{喜} }$ _nla_reserved_nohdr(struct sk_buffer \*skb,int attrlen)   
{ void \*start; //按指定长度开辟空间没有属性头 start $=$ skb_put(sk,B,NLAALIGN(attrlen));memset(start,O,NLAALIGN(attrlen));//清零 return start;

每一个配置跳转结构都记录了跳转结构标志和压力数、使用的设备；如果跳转结构还指定了网关，则还要在配置跳转结构后面紧跟一个记录网关的属性结构。1021行使配置跳转结构记录下目前的长度值，这也包括了网关属性结构的长度在内。循环记录完每一个跳转结构后，就在1024行调用nla_nest_end()函数来调整“配置跳转结构”队列的长度计数，使它记录下整个配置跳转结构的总长度。1027行调用nlmsg_end()函数也会调整消息头的长度计数nlmsg_len，记录整个消息的目前长度。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ fib_dump_info() $\rightarrow$ nla_nest_end()   
static inline int nla_nest_end(struct sk BUFF \*skb,struct nlattr \*start)   
{ start->nla_len $=$ skbTAIL_pointer(skb)-(unsigned char \*)start; return skb->len;   
}   
static inline int nlmsg_end(struct skbuff \*skb,struct nlmsghdr \*nlh)   
{ nlh->nlmsg_len $=$ skbTAIL_pointer(skb)-(unsigned char \*)nlh; return skb->len;

fib_dump_info()函数最后返回数据包的实际长度，回到rtmsg_fib()函数中要对这个返回值进行检查。如果小于0，则表示中间过程出错了，就释放这个分配的数据包结构skb；反之，代表一切顺利，调用rtl_notify()函数将这个载有路由消息的数据包发送出去。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ rtnl_notify()

int rtnl_notify(struct sk_buffer \*skb, struct net \*net, u32 pid, u32 group, struct nlmsghdr \*nlh,gfp_t flags)   
{ struct sock \*rtnl $=$ net->rtnl; int report $= 0$ if (nlh) report $=$ nlmsg_report(nlh); return nlmsg_notify(rtnl, skb, pid, group, report, flags);

系统初始化 netlink 时会把它的 sock 指针记录到 net->rtnl 中, 这里取出它的 sock 结构指针。最终调用 nlmsg_notify() 函数完成发送任务, 但在调用之前先要使用 nlmsg_report() 函数来检验消息头标志中是否含有 NLM_F_ECHO 标志, 它表示是否需要向设置程序返回信息; 然后把这个检验情况传递给 nlmsg_notify().

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ rtnl_notify() $\rightarrow$ nlmsg_notify() int nlmsg_notify(struct sock \*sk, struct sk_buffer \*skb, u32 pid, unsigned int group, int report, gfp_t flags) { int err $= 0$

if (group) { int exclude pid = 0;   
if (report) { atomic_inc(&skb->users); exclude.pid = pid; } /\* errors reported via destination sk->> sk_err \*/ nlmsgMULTicast(sk,skb,exclude.pid,group,flags); } if (report) err $=$ nlmsg_unicast(sk,skb,pid); return err;

首先检查一下参数 group, 这个值是 RTNLGRP_IPV4ROUTE, 它表示 netlink 的多播分组类别。如果需要向用户程序返回消息, 则递增数据包的计数器, 并把当初调用 netlink 的进程号传递给 nlmsgMULTicast() 函数。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ rtnl_notify() (nlmsg_notify() $\rightarrow$ nlmsgMULTicast())  
static inline int nlmsgMULTicast(struct sock \*sk,struct sk_buffer \*skb, u32 pid, unsigned int group,gfp_t flags)   
{ int err; NETLINK_CB(sk).dst_group $=$ group; err $=$ netlink_broadcast(sk,skb,pid,group,flags); if (err $>0$ ) err $= 0$ return err;

将 group 记录到数据包控制信息中，然后调用 netlink_broadcast() 发送数据包。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ rtnl_notify() $\rightarrow$ nlmsg_notify() $\rightarrow$ nlmsgMULTicast() $\rightarrow$ netlink_broadcast()  
1005 int netlink_broadcast(struct sock \* ssk, struct sk_buffer \* skb, u32 pid,   
1006 u32 group,gfp_t allocation)   
1007 {   
1008 struct net \*net $=$ sock_net(ssk);   
1009 struct netlink_broadcast_data info;//用于广播的 netlink 数据结构

struct hlist_node \* node; struct sock \*sk; skb $=$ netlink_trim(sk,allocation); //初始化netlink广播的数据结构 info.exclude_sk $=$ ssk; info.net $=$ net; info.pid $=$ pid; info.group $=$ group; info.failure $= 0$ . info.congested $= 0$ . info.delivered $= 0$ . infoallocation $=$ allocation; info.skb $=$ skb; info.skb2 $=$ NULL; / $\text{喜}$ While we sleep in clone，do not alloc netlink_lock_table();//对数组加锁 sk_for_each_bound(sk,node,&nl_table[ do_one_broadcast(sk,&info); kfree_skb(skb);//释放数据包空间 netlink_unlock_table();//解锁 if (info.skb2)//释放备份的数据包空间 kfree_skb(info.skb2); if (info.delivered）{//传送完成则“让步 if (info.congested &&(allocation yield(); return 0; } if (info失败) return -ENOUBFS; return -ESRCH;

前面介绍了内核中有一个 netlink_table 结构数组 nl_table。1030 行循环沿着 nl_table 的 hlist_node 哈希队列头找到全部 netlink 的 sock 结构, 然后调用 do_one_broadcast() 函数将 netlink_broadcast_data 结构变量 info 挂入到每一个 sock 的接收队列中。netlink_broadcast_data 是用于 netlink 广播使用的数据结构。

# 代码清单6.4 netlink_broadcast_data结构定义

```c
struct netlink_broadcast_data {
    struct sock * exclude_sk; //排除在外的 sock
    struct net * net; //网络空间
    u32 pid; //进程 ID
    u32 group; //所属进程组
    int failure; //是否发送成功标志
    int congested; //发送过程的拥挤情况
    int delivered; //已经发送标志
    gfp_t allocation; //分配标志
    struct sk_buffer * skb, * skb2; //发送数据包指针与备用数据包指针;
};
```

do_one_broadcast()函数在进一步的检测和调整info的内容后，再调用netlink_broadcast Deliver将数据包挂入到netlink的接收队列sk->sk_receive_queue，然后调用sk->sk_data_ready函数；在6.1节netlink_kernel_create()函数创建netlink的时候，将这个函数设置为netlink_data_ready()，它是空函数。

fn_hash_insert() $\rightarrow$ rtmsg_fib() $\rightarrow$ rtnl_notify() $\rightarrow$ nlmsg_notify() $\rightarrow$ nlmsgMULTicast() $\rightarrow$ netlink_broadcast() $\rightarrow$ do_one_broadcast()

static inline int do_one_broadcast(struct sock \*sk, struct netlink_broadcast_data \*p)   
{ struct netlink SOCK \*nlk $=$ nlk_sk(sk); int val; if $(\mathfrak{p} - >$ exclude_sk $= =$ sk)//如果当前找到的sock是排除在外的就退出 goto out; //检查进程的ID号、进程组号以及进程组位图 if (nlk->pid $= =$ p->pid || p->group-1 >= nlk->ngroups || !test_bit(p-> group-1, nlk-> groups)) goto out; //是否属同一个网络空间 if(!net_eq(sock_net(sk),p->net))

```c
goto out;  
//如果 netlink 广播数据已经标识失败就调用 sock 的错误通知函数  
if (p->failure) {  
    netlink_overrun(sk);  
    goto out;  
}  
//增加 sock 的使用计数防止它被其他进程释放  
sock_hold(sk);  
if (p->skb2 == NULL) { //备用数据包指针空闲  
    if (skb_shared(p->skb)) { //要发送数据包是共享使用的  
        p->skb2 = skb Clone(p->skb, p->allocation); //克隆一个数据包  
    } else {  
        p->skb2 = skb_get(p->skb); //备份数据包指针指向发送数据包  
        /*  
            * skb Ownership may have been set when  
            * delivered to a previous socket.  
            */  
            skb_orphan(p->skb2);  
    }  
}  
if (p->skb2 == NULL) { //备份数据包指针仍然为空  
    netlink_overrun(sk); //调用 sock 的错误通知函数  
    /* Clone failed. Notify ALL listeners. */  
    p->failure = 1; //标记发送失败  
} else if (sk_filter(sk, p->skb2)) { //对数据包进行 socket 的安全检测和过滤  
        kfree_skb(p->skb2);  
        p->skb2 = NULL;  
//数据包挂入 sock 的接收队列  
} else if ((val = netlink_broadcastdeliver(sk, p->skb2)) < 0) {  
    netlink_overrun(sk);  
} else { //记录发送的拥挤情况，设置发送成功标志，清空备份数据包指针  
    p->congested |= val;  
    p->delivered = 1;  
    p->skb2 = NULL;  
}  
sock_put(sk); //发送完成释放数据包  
return 0;
```

发送时要对数据包进行共享使用检测，然后克隆一个备份数据包，最终调用 netlink_broadcastdeliver() 函数将数据包挂入到 netlink 的接收队列中。回到 netlink_broadcast() 函数后立即开始进程调度，唤醒等待的进程使它接收该数据包，从而对消息结构进行分析和处理。这个进程就是 IPROUTE2 创建的，现在把路由的设置情况以消息结构的形式返回给它。

# 监听连接请求

# 7.1 内核的监听函数

图1.1的服务器程序执行路线完成了socket的创建和地址设置，接下来就是监听连接请求，也就是执行代码清单1.1的监听函数：

```javascript
listen(server_fd, 10); 
```

服务器程序可以监听10个连接请求，从listen()函数到系统调用的中间过程与1.3节的描述完全一致，读者可以在glibc-2.3.6/sysdeps/generic/listen.c文件中找到它的定义，并且可以在glibc-2.3.6/sysdeps/unix/sysv/linux/listen.S中看到它的汇编入口，读者可以结合第15章的send函数内容分析一下从listen()到库函数再到系统调用的经过。

参数通过寄存器传递到了sys_SOCKETcall()函数，到达2037行处。

2037 case SYS_LISTEN:   
2038 err $=$ sys.listen(a0，a1);   
2039 break;

2038行调用sys.listen()函数执行监听任务，传递的两个参数分别是socket的文件号和连接数。

sys_SOCKETcall() $\rightarrow$ sys.listen()   
asmlinkage long sys Listen (int fd, int backlog)   
{ struct socket \* sock; int err，fput_needed; int somaxconn; sock $=$ sockfd.lookup_light(fd,&err,&fput_needed);//根据文件号找到服务器 socket if（sock）{ somaxconn $=$ sock_net(sock->sk)->core.sysctl_somaxconn; if((unsigned)backlog $\rightharpoondown$ somaxconn)

backlog $=$ somaxconn; err $=$ security_SOCKET.listen(sock,backlog); if(!err) err $=$ sock->ops->listen(sock,backlog);//执行具体协议的监听函数 fput_light(sock->file,fput_needed); } return err;

代码中的 sockfd.lookup_light()函数在前面章节中看过了，它在 socket 文件系统中找到服务器的 socket。对比一下 sys.listen()与 sys_bind()两个函数的代码可以看出，它们大致相同，不同的地方是地址设置时调用了具体协议结构中的绑定函数，而这里则执行的是协议结构的监听函数。

```txt
sock-> ops-> listen(sock, backlog); 
```

也就是说，sys.listen()函数找到服务器的socket以后，通过它的协议操作表结构struct protoOps执行其listen()钩子函数。protoOps协议操作表结构的挂入过程在前面介绍了，我们也看到服务器的sock->ops挂入的是inet_STREAMOps操作表结构，因此这里调用的是inet_STREAMOps结构的listen()，注意参数backlog是限定的连接数10。

const struct protoOps inlet_STREAMOps { .listen $=$ inlet.listen, };

其listen()挂入的是inet听听()函数，这个函数在/net/ipv4/af_inet.c中的194行处。

sys_SOCKETcall() $\rightarrow$ sys.listen(） $\rightarrow$ inet听听()   
194 intinetListen(struct socket \*sock，intbacklog)   
195 {   
196 struct sock \*sk $=$ sock->sk;   
197 unsigned char old_state;   
198 int err;   
199   
200 lock_sock(sk); //加锁，如果sock锁被其他进程占用了，当前进程睡眠等待唤醒   
201   
202 err $=$ -EINVALID;   
203 if (socket->state！ $=$ SS_UNCONNECTED|| sock->type！ $=$ SOCK_STREAM)   
204 goto out;//检查socket的状态和类型

old_state $=$ sk->sk_state;//记录原来的状态 if(!((1<<old_state)&(TCPF_CLOSE|TCPF_LISTEN))) goto out; $\text{一} ^ { \text{一} }$ Really,if the socket is already in listen state \* we can only allow the backlog to be adjusted. \*/ if(old_state！ $= \mathrm{TCP\_LISTEN}$ ）{ err $=$ inet_csk.listen_start(sk,backlog);//建立监听环境 if(err) goto out; } sk->sk_max_ACK_backlog $=$ backlog;//记录下连接数 err $= 0$ .   
out: release SOCK(sk); //解锁，并唤醒 sock 锁上的其他进程 return err;

函数中首先是对服务器的socket状态进行检测，如果不是未连接状态就不能继续往下执行；接着检查socket类型是否为有连接的数据流类型，如果不是数据流类型也不能往下执行。

206行old_state记录下sock的原始状态，后边要调整它的状态值。接着，对TCP的状态位进行检查，查看是否处于关闭或者监听状态，然后在214行进入inet_csk.listen_start()函数。

sys_SOCKETcall() $\rightarrow$ sys.listen( $\rightarrow$ inet听听( $\rightarrow$ inet_csk.listen_start(）   
562 intinet_csk Listen_start(struct sock \*sk，const int nr_table_entries)   
563 {   
564 structINET_sock \*inet $=$ INET_sk(sk);//取得INET的sock指针   
565 structinet_connection_sock \*icsk $=$ INET_csk(sk);//取得INET连接结构指针   
566 intrc $=$ reqsk_queue_alloc(&icsk->icsk_accept_queue，nr_table_entries); //初始化 连接请求队列，并创建监听结构与连接请求结构

567  
568 if $(\mathrm{rc}! = 0)$ 569 return rc;  
570  
571 sk->sk_max_ACK_backlog $= 0$ //最大连接数

```c
sk->sk ACK_backlog = 0; //当前连接数
inet_csk_delack_init(sk);
/* There is race window here: we announce ourselves listening,
* but this transition is still not validated by get_port();
* It is OK, because this socket enters to hash table only
* after validation is complete.
*/
sk->sk_state = TCP_LISTEN; //设置监听状态
if (!sk->skprot->get_port(sk, inlet->num)) { //是否已经绑定了端口
inet->sport = htons(inet->num);
skDst_reset(sk);
sk->skprot->hash(sk); //将sock结构链入监听队列
return 0;
}
//到达这里表示中间出错了，关闭sock，清空连接请求队列，返回出错码
sk->sk_state = TCP_CLOSE;
_reqsk_queuedestroy(&icsk->icsk_accept_queue);
return -EADDRINUSE; 
```

这个函数在/net/ipv4/inet_connection SOCK.c 中的 562 行。首先, 564 行调用 inlet_sk() 函数通过 sock 指针得到 INET 的结构指针 inlet。565 行调用 inlet_csk() 函数通过 sock 指针得到 INET 连接结构指针 icsk, 这个结构用于连接, 它的定义已经在代码清单 3.4 看过了。

参数nr_table_entries的数值是10，这个数值允许多少个客户端连接；如果超过了这个数量，客户端的socket就只好睡眠等待了。

566行调用了reqsk_queue_alloc()函数，为接收队列分配对应连接数量的监听结构。

sys_SOCKETcall() $\rightarrow$ sys.listen() $\rightarrow$ inet听听() $\rightarrow$ inet_csk听听_start() $\rightarrow$ reqsk_queue_alloc()

```c
37 int reqsk_queue_alloc(struct request_sock_queue *queue, unsigned int nr_table_entries)  
38  
40 size_t lopt_size = sizeof(struct listen_sock); //取得监听结构的长度  
41 struct listen_sock *lopt; //监听结构变量  
42 //确定连接数  
43 nr_table_entries = min_t(u32, nr_table_entries, syscall_max_syn_backlog); 
```

nr_table_entries $=$ max_t(u32，nr_table_entries,8);   
nr_table_entries $=$ roundup.pow_of_two(nr_table_entries+1);   
lopt_size $+ =$ nr_table_entries $\ast$ sizeof(struct request SOCK $\ast$ ）;   
if(lopt_size $\gimel$ PAGE_SIZE)//小于一个页面按字节分配空间 lopt $=$ _vmalloc(lopt_size, GFP_KERNEL|GFP_HIGHMEM|_GFP_ZERO, PAGE_KERNEL);   
else lopt $=$ kzalloc(lopt_size,GFP_KERNEL);//在通用缓存中按页面分配空间 if(lopt $= =$ NULL) return -ENOMEM;   
for(lopt->max_qlen_log $= 3$ //2的3次幂，即8 (1<<lopt->max_qlen_log）<nr_table_entries; lopt->max_qlen_log++);   
get_random_bytes(&lopt->hash_rnd,sizeof(lopt->hash_rnd)); rwlock_init(&queue->syn_wait_lock); queue->rskq.accept_head $\equiv$ NULL; lopt->nr_table_entries $\equiv$ nr_table_entries;//记录连接数 write_lock_bh(&queue->syn_wait_lock); queue->listen_opt $\equiv$ lopt;//连接请求队列记录监听结构 write_unlock_bh(&queue->syn_wait_lock); return 0;

函数40行处出现了一种新的结构struct listen_sock，它是专用的监听结构，用于记录监听队列的信息。这个结构体末尾是一个request_sock指针数组，数组的长度是0，后面将会看到这种数组的灵活性。

# 代码清单7.1 listen_sock结构定义

```c
struct listen_sock { //监听结构
u8 max_qlen_log; //连接数量，以2的次幂表示
/* 3 bytes hole, try to use */
int qlen;
int qlen_young;
int clock_hand;
```

```c
u32 hash_rnd; //哈希数  
u32 nr_table_entries; //连接数量  
struct request_sock  
* syn_table[0]; //连接请求结构数组
```

代码43行是求最小值宏min_t,它使用的sysctl_max_syn_backlog被定义为256。也就是说，内核最多支持256个连接请求，可以修改这个值从而调整Linux支持的连接数。44行是求最大值宏确保连接数不小于8，因此nr_table_entries连接数目被限制在了 $8\sim 256$ 之间。

45 行使用 roundup_pow_of_two 宏对连接数目再次调整，它定义在/include/linux/log2.h 文件中。这是一个常量优化检测宏，如果发现表达式是常量，那么就使用可以优化的常量表达式；如果表达式不是常量，就调用另一个宏函数把值向上取整到 2 的幂。

```txt
define roundup_pow_of_two(n)  
(  
    __builtin_constant_p(n)?()  
    (n == 1)?1:  
        (1UL << (ilog2((n)-1)+1))  
    ):  
    __roundup_pow_of_two(n)  
) 
```

46行还有一个新的数据结构struct request_sock，它用来代表连接请求。这个结构在listen_sock结构的末尾处，是以指针数组的形式存在的，但是初始的数组长度是0。从46行对lopt_size的计算可以看出，它的值是监听结构长度加上多个连接请求结构长度之和，而多个连接请求结构长度是由连接数决定的。

对于服务器程序来说，每一个监听结构listen_sock的后面紧跟着10个连接请求，它们组成一个队列。

# 代码清单7.2 request_sock结构定义

```c
struct request_sock { //连接请求结构
struct request_sock *dl_next; /* 指向下一个连接请求结构 */
u16 mss; //MSS值
u8 retrans; //重发次数
u8 cookie-ts; /* synccookie: encode tcpputs in timestamp */
/* The following two fields can be easily recomputed I think -AK */
u32 window_clamp; /* 窗口创建时间 */
u32 rcv_wnd; /* rcv_wnd offered first time */
u32 tsrecent; //时间戳
unsigned long expires; //过期时间 
```

```c
const struct request_sockOps //连接请求函数表
struct sock *sk; //记录接收该连接请求的 sock
u32 secsid; //这两个变量用于XFRM,参考secid.txt
u32 peer_secid;
};
```

lopt_size 确定了所需内存空间的大小, 分配成功后监听结构的 nr_table_entries 记录下连接数, 然后将连接请求队列与这里新分配的监听结构建立关联, 让连接请求队列记录下监听结构。连接请求队列 icsk_accept_queue 是 struct request_sock_queue 结构类型, 它说明了连接请求的接收情况。

# 代码清单7.3 request_sock_queue结构定义

```txt
struct request_sock_queue{//连接请求队列结构   
struct request_sock \* rskq_accept_head; //指向队列第一个连接请求   
struct request_sock \* rskq.accepttail;//指向队列最后一个连接请求   
rwlock_t syn_wait_lock; //同步锁   
u8 rskq_defer Accept; //默认重新 SYN 的数量   
/\*3 bytes hole,try to pack\*/   
struct listen_sock \*listen_opt;//监听结构   
};
```

回到inet_csk.listen_start()函数，573行调用了inet_csk_delack_init()函数，将INET连接结构的icsk_ACK初始化为0。

```c
struct {
    __u8 pending; /* ACK is pending */
    __u8 quick; /* Scheduled number of quick acks */
    __u8 pingpong; /* The session is interactive */
    __u8 blocked; /* Delayed ACK was blocked by socket lock */
    __u32 ato; /* Predicted tick of soft clock */
    unsigned long timeout; /* Currently scheduled timeout */
    __u32 lrcvtime; /* timestamp of last received data packet */
    __u16 last_seg_size; /* Size of last incoming segment */
    __u16 rcv_mss; /* MSS used for delayed ACK decisions */
} icsk_ACK; 
```

这个结构是为了连接请求过程中的“应答”目的。inet_csk_delack_init()函数调用mem-set()将结构内容全部“清零”。

sys_SOCKETcall() $\rightarrow$ sys.listen() $\rightarrow$ inet听听() $\rightarrow$ inet_csk听听_start() static inline void inet_csk_delack_init(struct sock \*sk)

```erlang
{memset(&inet_csk(sk) -> icsk_ACK, 0, sizeof(inet_csk(sk) -> icsk_ACK)); 
```

580行将服务器的sock结构设置为了TCP_LISTEN监听状态。

581 行是对端口的操作。sk->sk_prot->get_port()的过程已经看过了，它实际调用了inet_csk_get_port()来设置端口。在绑定过程中已经完成了对端口的设置，因此这里只不过是检查一下就返回了。585 行是将 sock 结构链入监听队列，我们在下一节分析这一过程。

# 7.2 内核的监听队列

inet_csk听听_start()函数585行执行 $\mathrm{sk - >}$ sk_prot->hash()。

结合tcpprot结构的hash()钩子函数。

```txt
. hash = inset_hash 
```

因而这里实际调用钩子函数inet_hash()，它在/net/ipv4/inet_hashtable.c中的379行处。

sys_SOCKETcall() $\rightarrow$ sys.listen() $\rightarrow$ inet听听() $\rightarrow$ inet_csk听听_start() $\rightarrow$ inet_hash()   
voidINET_hash(struct sock \*sk)   
{ if(sk->sk_state！ $=$ TCP_CLOSE){ local_bh_disable(); __inet_hash(sk); local_bh_enable(); }   
}

这里检查一下 sock 结构的状态, 然后禁用软中断, 接着调用了 __inet_hash()函数。

sys_SOCKETcall() $\rightarrow$ sys.listen() $\rightarrow$ inet听听() $\rightarrow$ inet_csk听听_start() $\rightarrow$ inet_hash() $\rightarrow$ __inet_hash()   
static void __inet_hash(struct sock \*sk)   
{ structINET_hashinfo\*hashinfo $=$ sk->skprot->h HASHinfo;//取得TCP协议的绑定哈希结构 struct hlist_head \*list; rwtlock_t \*lock; if(sk->sk_state！=TCP_LISTEN）{//如果sock结构还没有处理监听状态 __inet_hash_nolisten(sk);//链入到已经连接的sock结构队列

return;   
}   
BUG_TRAP(sk_unhashed(sk));//检测是否已经链入队列   
list $=$ &hashinfo->listening_hash[inet_sk.listen_hashfn(sk)];//确定监听队列 lock $=$ &hashinfo->lhash_lock;//取得队列锁   
inet听听_wlockHashinfo); //加锁，如果无法加锁就将服务器程序进程转入深度睡眠 _sk_add_node(sk, list); //将sock结构链入监听队列中 sockprot_inuse_add(sock_net(sk),sk->sk_prot,1);//增加协议函数表的使用计数 write_unlock lock); //解锁 wake_up(&hashinfo->lhash_wait); //因为是深度睡眠，因此需要唤醒服务器程序进程

这个函数主要是将 sock 链入到 TCP 的绑定哈希结构 tcp_hashinfo 中。对于 sk->skprot->h hashinfo 的初始化已经在地址绑定时完成了, 在那里指定了绑定哈希结构为 tcp_hashinfo, 它是 inet_hashinfo 结构变量, 在代码清单 3.3 介绍了该结构的内容, 前面也看过了 __sk_add_node() 函数。

```txt
struct inset_hashinfo_cachelineAligned tcp_hashinfo = {
    lhash_lock = __RW_LOCK_UNLOCKED.tcp_hashinfo.lhash_lock),
    lhash_users = ATOMIC_INIT(0),
    lhash_wait = __WAITQueue_HEAD_INITIALIZER.tcp_hashinfo.lhash_wait);
}; 
```

先检查 sock 结构的状态, 如果还没有处理监听状态, 就调用 __inet_hash_nolisten() 函数将 sock 结构链入确定连接的哈希桶中。

336 void__inet_hash_nolisten(struct sock \*sk)   
337{   
338 structINET_hashinfo\*hashinfo $=$ sk->skprot->h.hashinfo; //取得TCP协议的绑定哈希   
//结构   
339 struct hlist_head \*list;   
340 rwlock_t \*lock;   
341 structINET_ehash_buckets \*head;   
342   
343 BUG_TRAP(sk_unhashed(sk));//检测是否已经链入   
344   
345 sk->sk_hash $=$ inlet_sk_ehashfn(sk);//计算哈希值   
346 head $=$ inlet_ehash_bucketsHashinfo,sk->sk_hash); //取得哈希桶   
347 list $=$ &head->chain;//获取已经连接的队列头

348 lock $=$ inset_ehash_lockpHashinfo,sk->sk_hash);//获取队列锁  
349  
350 write_lock lock); //加锁  
351 __sk_add_node(sk, list); //将sock结构链入监听队列中  
352 sockprot_inuse_add(sock_net(sk),sk->skprot,1); //递增计数  
353 write_unlocklock); //解锁  
354}

先计算出哈希值。以tcp_hashinfo结构的ehash数组为对象，以哈希值为下标确定所属的队列，然后将服务器的sock链入到这个队列中。

在 tcp_hashinfo 结构的定义中初始化了等待队列。因为在链入该结构的监听队列之前，它有可能被其他进程正在使用，如查询哈希桶和桶结构操作，此时就要将服务器程序进程转入深度睡眠；这种睡眠不能被信号唤醒，而只能使用 wake_up() 函数唤醒。

```c
void inset听听_wlock(struct inset_hashinfo *hashinfo) acquires hashinfo-> lhash_lock)//检测目的   
{ write_lock(&hashinfo->lhash_lock);//加锁 if (atomic_read(&hashinfo->lhash_users)){//如果哈希结构正在使用中 DEFINE_WAIT(wait);//声明当前进程的等待队列头 //循环睡眠，直到哈希结构可以使用为止 for(;;）{//链入哈希结构的等待队列中，深度睡眠 prepare_to_wait_exclusive(&hashinfo->lhash_wait, &wait,TASK_UNINTERRUPTIBLE); if(!atomic_read(&hashinfo->lhash_users))://如果哈希结构可用就跳出循环 break; write_unlock bh(&hashinfo->lhash_lock);//解锁 schedule();//进程调度 write_lock bh(&hashinfo->lhash_lock);//加锁 } //跳出循环后，说明哈希结构可用，就将当前进程的等待队列头从等待队列中脱链 finish_wait(&hashinfo->lhash_wait,&wait); //设置当前进程为可运行状态 1
```

建立服务器程序进程的等待队列头，然后链入到tcp_hashinfo结构的等待队列中。接着，服务器程序进程进入深度睡眠，内核执行进程调度。当服务器程序进程被唤醒后还会检查结构是否可用，如果可用，则跳出循环，从等待队列中摘除服务器程序的等待队列头。

在tcp_hashinfo结构中还有一个专用于监听的hash队列。

```txt
struct hlist_head listening_hash[INET_LHTABLE_SIZE]; 
```

如果 sock 结构已经处于监听状态，则在这里找到对应的监听队列后，通过 __sk_add_node() 函数将服务器的 sock 结构链入到监听队列中，接着递增 skprot 所指向的协议函数表的计数器。最后，通过 wake_up() 唤醒睡眠的服务器程序进程，让它继续运行。

这里的睡眠过程使程序的运行效率受到很大的影响，当有多个进程执行监听时，运行效率更是大打折扣。读者可以在最新的内核中看到这个睡眠操作函数已经被移除，如2.6.30版本中定义了监听结构，将listening_hash变成了监听结构队列；当链入队列时直接使用链头操作，不再睡眠等待，从而极大改善了监听效率。

# 接收连接请求

# 8.1 接收连接函数

本章按照服务器程序路线分析服务器如何接收客户端的socket连接请求，同样采取前面的分析方法，先从服务器程序的接收代码开始（代码清单1.1）。

```c
accept(server_fd, (struct sockaddr *)&client_address, client_len); 
```

注意，第二个参数 client_address 是为保存客户端 socket 地址而使用的结构变量。我们直接到达 sys_SOCKETcall()函数中的 switch 语句，读者可以参考 1.3 节的内容体会服务器程序到达这个函数的过程，可以在 glibc - 2.3.6/sysdeps/generic/accept.c 文件中找到它的定义，并且可以在 glibc - 2.3.6/sysdeps/unix/sysv/linux/accept.S 中看到它的汇编入口；也可以参考第 15 章的 send 函数内容分析一下从 accept() 到库函数再到系统调用的经过。

case SYS_ACCEPT: err $=$ sys_accept(a0，(struct sockaddr_user \*)a1， (int __user \*)a[2]); break;

这里显然是调用sys.accept()函数来接收连接，传递参数分别是socket的文件号、用于记录客户端地址的结构、要求的地址长度。

sys_SOCKETcall() $\rightarrow$ sys.accept()   
1403 asmlinkage long sys_accept(int fd，struct sockaddr __user \*upeer_sockaddr,   
1404 int __user \*upeer_addrlen)   
1405 {   
1406 struct socket \*sock，\*newsock;   
1407 struct file \*newfile;   
1408 int err，len，newfd，fput_needed;   
1409 char address[MAX SOCK_ADDR];   
1410 //查找服务器socket

sock $=$ sockfd.lookup_light(fd,&err,&fput_needed);if(!sock)goto out;err $\equiv$ -ENFILE;if(!(newsock $=$ sock_alloc())//为客户端准备socket结构goto out_put;newsock->type $=$ sock->type;//继承服务器的socket类型和函数表newsock->ops $=$ sock->ops; $/*$ \*We don't need try_module_get here,as the listening socket (sock)\*has the protocol module (sock-> ops-> owner) held.\*/_module_get(newsock->ops->owner);//目前是空函数newfd $=$ sock_alloc_fd(&newfile);//为客户端socket分配文件号和文件结构if (unlikely(newfd $<  0$ ）{err $=$ newfd;sock_release(newsock);goto outPut;1err $=$ sock.attach_fd(newsock,newfile);//客户端socket建立文件系统关联if(err $<  0$ goto out_fdsimple;err $=$ security_sock_accept(sock,newsock);if(err)goto out_fd;/调用服务器socket函数表的接收连接函数err $=$ sock->ops->accept(sock,newsock,sock->file->f_flags);if(err $<  0$ goto out_fd;ifupeer_sockaddr）{//获取客户端socket地址if(newsock->ops->getname(newsock，(struct sockaddr\*)address，&len,2)<0）{

1450 err $=$ -ECONNABORTED;   
1451 goto out_fd;   
1452 }//复制地址到服务器程序   
1453 err $=$ move_addr_to_user(address, len, upeer SOCKaddr,   
1454 upeer_addrlen);   
1455 if (err $<  0$ ）   
1456 goto out_fd;   
1457 }   
1458   
1459 /\*File flags are not inherited via accept() unlike another OSes. \*/   
1460   
1461 fd_install(newfd,newfile);//文件号与文件结构建立关联   
1462 err $=$ newfd;//将文件号返回给服务器程序   
1463   
1464 security_SOCKET_post_accept(sock，newsock);   
1465   
1466 out_put:   
1467 fput_light(sock-> file,fput_needed);   
1468 out:   
1469 return err;   
1470 out_fd.simple;   
1471 sock_release(newsock);   
1472 put_filp(newfile);   
1473 put_unused_fd(newfd);   
1474 goto out_put;   
1475 out_fd:   
1476 fput(newfile);   
1477 put_unused_fd(newfd);   
1478 goto outPut;   
1479 }

函数总的作用是建立服务器与客户端通信的“桥梁”，为客户端创建一个的socket，并在网络文件系统中为它申请文件号和文件结构，最后返回客户端的文件号client_sockfd和客户端的地址client_address。

函数1411行进入sockfd.lookup_light()函数，找到服务器的socket；接着调用sock_alloc()函数为客户端准备一个新的socket，使它“继承”服务器socket的类型和操作函数表。sock->ops函数操作表在服务器socket创建过程中挂入了inet_STREAMOps，这个结构在代码清单3.2介绍过，它是INET协议族的数据流操作表。

1428 行要为新创建的 socket 分配一个可用的文件号和文件结构,接着通过 sock Attached_

fd()函数将客户端socket与文件系统建立关联。创建目录项让它和文件结构都与socket建立联系，这个函数已经在服务器创建socket的过程中讲述了。

1443 行执行函数操作表 inlet_STREAMOps 的 accept() 函数。

const struct protoOps inlet_STREAMOps { .accept $=$ inlet_accept, };

结构的accept()钩子函数对应挂入了inet.accept(),因此1443行执行inet.accept函数来接收客户端的连接请求。

sys_SOCKETcall() $\rightarrow$ sys.accept() $\rightarrow$ inlet Accept()   
int inlet Accept(struct socket \* sock, struct socket \*newsock, int flags)   
{ struct sock \*sk1 $=$ sock->sk;//取得服务器sock指针 int err $=$ -EINVALID; struct sock \*sk2 $=$ sk1->sk_prot->accept(sk1,flags,&err);//从接收队列中获取客户端 sock if(!sk2) goto do_err; lock SOCK(sk2); //加锁，如果sock锁被其他进程占用了，当前进程睡眠等待唤醒 BUG_TRAP((1<<sk2->sk_state)& (TCPFEstablishED|TCPF_CLOSE_WAIT|TCPF_CLOSE)); sock_graft(sk2,newsock);//客户端socket与sock完成对接 newsock->state $=$ SS_CONNECTED://设置客户端socket为连接状态 err $= 0$ ： release_sock(sk2); //解锁，并唤醒sock锁上的其他进程 do_err; return err;

参数 sock 是服务器的 socket 结构指针, newsock 是新建客户端的 socket 指针。在创建服务器 socket 时曾看过 sk_alloc() 函数内容, 它分配服务器 sock 以后便将 TCP 协议结构 tcpprot 挂入到 sk_prot 中, 这里首先执行 tcp_prot 的 accept 函数。

struct sock \*sk2 $=$ sk1->sk_prot->accept(sk1,flags,&err);

对照一下tcpprot结构的accept内容。

```txt
struct proto tcp.proto = { 
```

...... .accept $=$ inlet_csk_accept, 1

其对应处显示为inet_csk.accept()函数。

sys_SOCKETcall() $\rightarrow$ sys.accept() $\rightarrow$ inlet Accept() $\rightarrow$ inlet_csk_accept()   
231 struct sock \*inet_cskAccept(struct sock \*sk，int flags,int \*err)   
232 {   
233 struct inlet_connection_sock \*icsk $=$ inlet_csk(sk);//取得服务器INET连接结构   
234 struct sock \*newsk;   
235 int error;   
236   
237 lock_sock(sk); //加锁，如果sock锁被其他进程占用了，当前进程睡眠等待唤醒   
238   
239 /*We need to make sure that this socket is listening,   
240 \*and that it has something pending.   
241 \*/   
242 error $=$ -EINVALID;   
243 if (sk->sk_state！ $=$ TCP_LISTEN)//接受连接之前必须处于监听状态   
244 goto out_err;   
245   
246 /*检查接收队列的连接请求\*/   
247 if (reqsk_queue_empty(&icsk->icsk_accept_queue)){   
248 long timeo $=$ sock_rcvtimeo(sk，flags&O_NONBLOCK);//确定睡眠时间   
249   
250 /\*If this is a non blocking socket dont sleep\*/   
251 error $=$ -EAGAIN;   
252 if(!timeo)   
253 goto out_err;   
254   
255 error $=$ inlet_csk_wait_for_connect(sk，timeo);//进入循环睡眠等待连接到来   
256 if (error)   
257 goto out_err;   
258 }   
259 //在请求结构中取得客户端的sock   
260 newsk $=$ reqsk_queue_get_child(&icsk->icsk_accept_queue，sk);   
261 BUG_TRAP(newsk->sk_state！ $=$ TCP_SYN_RECV);   
262 out:

```txt
263 release SOCK(sk); //解锁，并唤醒 sock 锁上的其他进程  
264 return newsk;  
265 out_err;  
266 newsk = NULL;  
267 *err = error;  
268 goto out;  
269}
```

传递的参数sk是服务器的sock，这个函数返回的正是客户端socket配对使用的sock结构，因此函数的主要目的就是获取客户端的sock。

233 行先是通过 inlet_csk()取得服务器的连接结构 struct inlet_connection_sock 指针 ic-sk。要执行该接收连接函数，服务器还必须处理于监听状态，否则不能接收客户端的连接请求，这就是 243 行的目的。  
247 行检查连接结构的接收队列 icsk Accept_queue 是否为空。假设未启动客户端程序，则不会向服务器发出连接请求因而该队列现在为空。sock_rcvtimeo()函数确定接收时的等待时间值，取得 sock 结构的 sk_rcvtimeo 接收时间值后，将它传递给 inlet_csk_wait_for_connect()函数睡眠等待连接请求的到来。

sys_SOCKETcall() $\rightarrow$ sys.accept() $\rightarrow$ inlet Accept() $\rightarrow$ inlet_csk_accept() $\rightarrow$ inlet_csk_wait_for_connect()

static int inlet_csk_wait_for_connect(struct sock \*sk，long timeo)   
{ struct inlet_connection SOCK \*icsk $=$ inlet_csk(sk); DEFINE_WAIT(wait);//声明当前进程的等待队列项 int err;

```c
\* True wake - one mechanism for incoming connections: only   
\* one process gets woken up, not the whole herd.   
\* Since we do not race & poll- for established sockets   
\* anymore, the common case will execute the loop only once.   
\* Subtle issue: "add_wait_queue-exclusive()" will be added   
\* after any current non - exclusive waiters, and we know that   
\* it will always _stay_ after any new non - exclusive waiters   
\* because all non - exclusive waiters are added at the   
\* beginning of the wait - queue. As such, its ok to "drop"   
\* our exclusiveness temporarily when we get woken up without   
\* having to remove and re - insert us on the wait queue.wumingxiaozu 
```

\*/   
for(;;）{//循环睡眠，直到时间用完或者有连接请求到来时为止 prepare_to_wait_exclusive(sk->sk_sleep,&wait, TASK_INTERRUPTIBLE);//将当前进程的等待队列项链入等待队列 release SOCK(sk); //解锁，并唤醒 sock 锁上的其他进程 if (reqsk_queue_empty(&icsk->icsk_accept_queue)) timeo $=$ schedule_timeout(timeo);//定时睡眠 lock SOCK(sk); //加锁，如果 sock 锁被其他进程占用了，当前进程睡眠等待唤醒 err $= 0$ ： if(!reqsk_queue_empty(&icsk->icsk_accept_queue))//是否有连接请求到来 break; err $= -EINVAL$ ： if(sk->sk_state！ $\equiv$ TCP_LISTEN) break; err $=$ sock_intr_errno(timeo); if(signal_pending(current))///是否有信号等待处理 break; err $= -EAGAIN$ ： if(!timeo) break;   
} finish_wait(sk->sk_sleep,&wait);//从等待队列中摘链 return err;

函数首先使用宏DEFINE_WAIT声明一个当前进程的等待队列项。

define DEFINE_WAIT(name) wait_queue_t name = { .private $=$ current, .func $=$ autoremove_wake_function, .task_list $=$ LIST_HEAD_INIT((name).task_list), }

这个宏为当前进程建立了名为 wait 的等待队列，接下来进入无限的 for 循环。首先进入 prepare_to_waitexclusive() 函数，将当前进程链入 sock 的等待队列。

sys_SOCKETcall() $\rightarrow$ sys.accept() $\rightarrow$ inlet Accept() $\rightarrow$ inlet_csk_accept() $\rightarrow$ inlet_csk_wait_for_connect() $\rightarrow$ prepare_to_wait_exclusive()

```c
void prepare_to_wait_exclusive(wait_queue_head_t *q, wait_queue_t *wait, int state) 
```

{ unsigned long flags; wait->flags $\equiv$ WQ_FLAG_EXCLUSIVE; spin_lock_irqsave(&q->lock，flags); if(list_empty(&wait->task_list)) _add_wait_queueTAIL(q，wait)；//链入到等待队列的末端 /\* \* dont alter the task state if this is just going to \* queue an async wait queue callback wumingxiaozu \*/ if(is sync_wait(wait)) set_current_state(state);//设置当前进程的状态 spin_unlock_irqrestore(&q->lock，flags）; }

这个函数将当前进程的 wait 等待队列项挂入到服务器 sock 结构的 sk_sleep 队列中；sk_sleep 是 wait_queue_head_t 结构，代表等待队列头，由此可以理解多个进程可以同时对一个 socket 并发的通信。如果链入队列成功，则修改当前进程的状态为可中断状态 TASK_INTERRUPTIBLE，即所谓的浅睡眠；该状态可以使进程等到有连接请求到来后及时被唤醒，恢复运行并及时处理连接请求。

inet_csk_wait_for_connect()函数将当前进程的等待队列项链入到sk_sleep等待队列后，还会再次调用reqsk_queue_empty()函数确认一下icsk_accept_queue队列是否有连接请求；如果还没有连接请求，则当前进程就开始进入循环睡眠了，一直睡眠到时间用完或者有连接请求到达时为止。

schedule_timeout()根据定时时间timeo来进入睡眠，如果睡眠时间用完或者被唤醒就从schedule_timeout()函数返回，此时timeo记录着剩余的时间值。再次锁住sock防止其他进程打扰，再检查一下icsk.accept_queue接收队列是否为空，如果不为空则说明已经有连接请求到来就跳出循环睡眠。

醒来后也要调用 signal_pending() 检查一下是否有信号等待处理，最后如果睡眠的时间已经用完就跳出循环，否则继续循环睡眠直到睡眠时间用完。

跳出循环睡眠后要将当前进程从 sock 中的睡眠队列 sk_sleep 中摘链。

这种睡眠方式是阻塞的、定时的。在连接请求到来之前，服务器程序无法再继续往下运行，要想改变这种情形，我们可以使用ioct1()函数异步接收连接请求，通过信号来执行连接；当然也可以使用select函数轮询，但是后者也会阻塞程序进程，所以效率相对前者较低。如果想提高程序的运行效率，则推荐使用异步接收方式，在本书光盘中提供了异步接收的范例。

# 8.2 异步接收方式

假若服务器程序采用了异步接收的方式，那么在没有连接请求到来的情况下，服务器程序进程可以继续运行执行其他任务；当客户端的连接请求到来时，内核会异步发送信号通知服务器的程序进程接受连接请求。

本书的范例中将接受连接请求的过程放在了信号处理程序中，然后通过FIOASYNC标志执行ioct1系统调用设置异步接收，最后还要将服务器程序进程指定为socket文件的所有者，其运行效果为：

[root@localhost socket_io]# ./server   
server is waiting   
server is waiting   
server is waiting   
received from client $=$ Im client   
server is waiting   
server is waiting   
server is waiting   
[root@localhost socket_io]# ./client   
sent from server $\equiv$ I'm server

从运行效果上看，服务器程序没有停在原地等待连接到来，而是继续运行，不断地循环打印信息。读者可以对照一下第9章的运行效果，在那里由于没有采用异步接收，服务器程序打印一次信息后就原地等待连接请求了，只有连接请求到来后它才会继续运行。

范例的具体代码请读者参考光盘内容，我们这里只看其中的重要语句，注意变量 on 控制创建、修改、删除异步结构。

/\*异步接收设置\*/int on $= 1$ ;ioctl(server_fd,FIOSYNC,&on);/\*指定服务器程序进程作为socket文件的所有者\*/fcnt1(server_fd，F_SETOWN，getpid();

ioctl()的调用路线是ioctl(）->sys_ioctl()->do_vfs_ioctl()->ioctl_fioasync(）-> sock_fasync(),它最终调用了sock_fasync()函数。

```c
static int sock_fasync(int fd, struct file *filp, int on)  
{  
    struct fasync_struct *fa, *fna = NULL, * * prev;  
    struct socket *sock;  
    struct sock *sk; 
```

//on控制创建、修改或者删除异步结构

if (on){//如果允许创建或者修改，就分配新的异步结构空间

```txt
fna = kmalloc(sizeof(struct fasync_struct), GFP_KERNEL); if (fna == NULL) return -ENOME; 
```

sock = filp->private_data; //获取socket结构

sk $=$ sock->sk;//获取sock结构  
if（sk $\equiv =$ NULL）{kfree(fna);return-EINVALID;

lock_sck(sk); //加锁, 如果 sock 锁被其他进程占用了, 当前进程睡眠等待唤醒

prev = &(sock->fasync_list); //获取队列中的第一个异步结构指针

```c
for (fa = *prev; fa != NULL; prev = &fa->fa_next, fa = *prev)  
if (fa->fa_file == filp)//查找异步结构  
break; 
```

if (on){//如果允许创建或者修改

```c
if (fa != NULL) { //找到了该socket结构的异步结构 write_lock_bh(&sk->sk_callback_lock); //加锁 fa->fa_fd = fd; //记录文件号 write_unlock_bh(&sk->sk_callback_lock); //解锁 
```

```c
kfree(fna); //释放新分配的异步结构  
goto out;  
{//没有找到就使用新建的异步结构  
fna->fa_file = filp; //记录关联文件指针  
fna->fa_fd = fd; //记录文件号  
fna->magic = FASYNC_magic; //设置魔数  
fna->fa_next = sock->fasync_list; //指向下一个异步结构  
write_lock_bh(&sk->sk_callback_lock); //加锁  
sock->fasync_list = fna; //链入队列首部  
write_unlock_bh(&sk->sk_callback_lock); //解锁
```

} else{//不允许创建或者修改就是删除操作 if (fa != NULL){//找到了socket的异步结构 write_lock_bh(&sk->sk_callback_lock);//加锁 \*prev $=$ fa->fa_next;//将找到的异步结构脱队 write_unlock_bh(&sk->sk_callback_lock);//解锁 kfree(fa); //释放找到的异步结构 }   
}   
out: release SOCK(sock->sk);//解锁，并唤醒sock锁上的其他进程 return 0;

这里主要是创建一个异步结构链入 socket 的异步队列中，当连接请求到来时，内核通过 sk_wake_async()唤醒服务器程序进程。

static inline void sk_wake_async(struct sock \*sk，int how,int band)   
{ if $(\mathrm{sk - > }$ sk_SOCKET&&sk->sk_SOCKET->fasync_list)//检查socket的异步队列 sock_wake_async(sk->sk_SOCKET，how，band);//异步唤醒进程   
} int sock_wake_async(struct socket \*sock，int how,int band) { if(!sock||！sock->fasync_list)//如果没有指定socket或者异步队列为空，返回 return-1; switch（how）{//异步操作码 caseSOCK_WAKE_WAITD://异步唤醒等待进程 if(test_bit(SOCKASYNC_WAITDATA,&sock->flags))//检查对应标志 break; goto call_kill; caseSOCK_WAKE_SPACE:/异步唤醒等待空间进程 if(!test_and_clear_bit(SOCKASYNC_NOSPACE,&sock->flags))//检查对应标志 break; /\*fall through\*/ caseSOCK_WAKE_IO: call_kill: __kill_fasync(sock->fasync_list，SIGIO，band); //处理异步结构，发送信号 break; caseSOCK_WAKE Urg://紧急IO唤醒

```c
__kill_fasync(sock->fasync_list, SIGURG, band);  
}  
return 0;  
}  
void __kill_fasync(struct fasync_struct *fa, int sig, int band)  
{  
while (fa) { //循环处理每一个异步结构  
struct fown_struct *fown; //文件所有者结构指针  
if (fa->magic != FASYNC_magic) { //如果异步结构的魔数不正确，返回  
printk(KERN_ERR "kill_fasync: bad magic number in "  
    "fasync_struct! \n");  
    return;  
}  
fown = &fa->fa_file->f-owner; //通过异步结构获取文件所有者结构  
/* Dort send SIGURG to processes which have not set a  
queued signum: SIGURG has its own default signalling  
mechanism. */  
if (! (sig == SIGURG && fown->signum == 0)) //如果不是紧急IO信号，并且设置了发送  
//信号值  
send_sigio(fown, fa->fa_fd, band); //发送信号唤醒服务程序进程  
fa = fa->fa_next; //指向下一个异步结构  
} 
```

例如，第12章在连接请求到来时，内核会执行：

```txt
sk_wake_async(sk,SOCK_WAKE_WAITD，POLL_IN);
```

范例代码中的 fcntl()函数将服务器进程指定为文件的所有者，并将它记录到 fown_struct 结构中，因此 sk_wake_async()函数通过 send_sigio()发送信号通知服务器程序接收连接请求。

回到inet_csk.accept()函数260行处，这时队列icsk.accept_queue已经不为空，说明客户端的连接请求已经到来。

```txt
260 newsk = reqsk_queue_get_child(&icsk->icsk_accept_queue, sk); 
```

newsk 代表客户端的 sock 结构, 这是调用 reqsk_queue_get_child() 函数获取的。

sys_SOCKETcall() $\rightarrow$ sys.accept() $\rightarrow$ inlet Accept() $\rightarrow$ inlet_csk_accept() $\rightarrow$ reqsk_queue_get child()

```txt
static inline struct sock *reqsk_queue_get_child(struct request_sck_queue *queue, 
```

```c
struct sock *parent)  
{  
    struct request_sock *req = reqsk_queue_remove(queue); //从接收队列中摘链  
    struct sock *child = req->sk; //取得连接请求结构中的sock指针  
    BUG_TRAP(child != NULL);  
    sk_acceptq Removed(parent); //递减服务器sock的控制连接数  
    __reqsk_free(req); //释放连接请求结构空间  
    return child;
```

函数首先调用 reqsk_queue_remove()从接收队列icsk.accept_queue中摘取链入的客户端请求结构request_sock。

sys_SOCKETcall() $\rightarrow$ sys.accept() $\rightarrow$ inlet Accept() $\rightarrow$ inlet_csk_accept() $\rightarrow$ reqsk_queue_get_ child() $\rightarrow$ reqsk_queue_remove() static inline struct request_sock \* reqsk_queue_remove(struct request_sock_queue \* queue) { struct request_sock \* req $=$ queue-> rskq.accept_head; //取得队列中第一个连接请求 BUG_TRAPreq! $=$ NULL); queue->rskq.accept_head $=$ req->dl_next;//指向下一个连接请求 if (queue->rskq.accept_head $= =$ NULL)//如果已经没有连接请求存在 queue->rskq.accepttail $=$ NULL; //清空指向队列尾部的指针 return req; 1

rskq_accept_head指向队列中第一个连接请求，这个连接请求结构是在客户端连接服务器的过程中挂入的。

reqsk_queue_get_child()函数取得了连接请求sock结构child后，调用sk_acceptq removed()递减服务器的控制连接数sk ACK_backlog，既然取得了客户端的sock结构就调用 __reqsk_free()释放连接请求结构。

# 8.3 获取连接请求

回到inet_csk.accept()函数中，newsk指向从reqsk_queue_get_child()函数返回的客户

端sock结构。这个客户端sock结构是由服务器网卡设备驱动向上层传递数据包时建立的，它的创建过程放在第12章中讲解。

inet_csk.accept()函数向上返回客户端的sock结构指针到inet.accept()函数，此时sk2就指客户端的sock结构，对客户端sock加锁后接着调用sock_graft()函数，参数newsock代表客户端socket，sock_graft()函数将两个结构体对接在一起。

sys_SOCKETcall() $\rightarrow$ sys.accept() $\rightarrow$ inet.accept() $\rightarrow$ sock_graft()   
static inline void sock_graft(struct sock \*sk，struct socket $^ { \text{喜} }$ parent)   
{ write_lock_bh(&sk->sk_callback_lock);//锁住后半部处理 sk->sk_sleep $=$ &parent->wait;//“继承”socket的等待队列 parent->sk $=$ sk;//socket记录sock指针 sk->sk_SOCKET $=$ parent;//sock记录socket指针 security_SOCKET_graft(sk，parent); write_unlock_bh(&sk->sk_callback_lock);//开锁 }

graft是嫁接的意思，这个函数将客户端的socket与sock成功对接，自此在服务器中newsock就代表客户端的socket，将它的状态设置为连接状态。

在inet.accept()函数中，客户端的newsock状态改变成了连接状态；而服务器的socket状态并没有任何改变，这是因为它担负着“孵化”客户端socket的任务。

回到sys.accept()函数1447行，如果参数upeer SOCKaddr存在，则说明准备了客户端地址结构，也说明服务器程序需要获取客户端地址，这个地址结构从服务器程序过来的是client_address，局部数组address用来暂时存放客户端的地址信息。1448行调用客户端的socket函数表来获取地址。

```txt
newsock-> ops-> getname(newsock, (struct sockaddr)address, &len, 2) 
```

前面看到客户端的socket函数表是继承服务器的inet_STREAMOps结构。

```txt
const struct protoOps inlet_STREAMOps = {  
......  
.getName = inlet_getname,  
}; 
```

其getname()挂入了inet_getname()函数，它在/net/ipv4/af_inet.c中的683行处。

sys_accept() $\rightarrow$ inet_getname() intinet_getname(struct socket \*sock，struct sockaddr \*uaddr,

int \*uaddr_len，int peer)   
{ struct sock \*sk $=$ sock->sk;//取得sock结构指针 structinet SOCK \*inet $=$ inlet_sk(sk);//取得INET的sock结构指针 struct sockaddr_in \*sin $=$ (struct sockaddr_in \*)uaddr;//转换地址结构 sin->sin_family $=$ AF_INET; if（peer）{ if(!inet->dport|| (((1<<sk->>sk_state)&(TCPF_CLOSE|TCPF_SYN_SENT))&& peer $= = 1$ ） return-ENOTCONN; sin->sin_port $=$ inlet->dport;//记录端口 sin->sin_addr.s_addr $=$ inlet->daddr;//记录地址 }else{ _be32 addr $=$ inlet->rcv_saddr;//接收地址 if(!addr) addr $=$ inlet->saddr;//源地址 sin->sin_port $=$ inlet->sport;//记录端口 sin->sin_addr.s_addr $=$ addr;//记录地址 } memset(sin->sin_zero,0,sizeof(sin->sin_zero)); \*uaddr_len $=$ sizeof(\*sin); return 0;

参数peer确定是否取客户端的地址，这会影响取地址的对象。如果peer存在且不为1，则取inet记录的目标端口和目标地址；反之，则取源端口和源地址或者接收地址。服务器程序传递下来peer值为2，因而执行if语句。

inet_sk()函数通过客户端的sock指针得到结构体inet SOCK指针。这个结构体在以前的章节中多次看到过，这是INET协议族的公用结构，把它放在这里介绍。

# 代码清单8.1inet SOCK结构定义

```c
struct inlet_sock { //INET 协议族结构
    /* sk and pinet6 has to be the first two members of inlet_sock */
    struct sock sk; //sock 结构, 注意不是指针
    if defined(CONFIG_IPV6) || defined(CONFIG_IPV6MODULE)
        struct ipv6_pinfo *pinet6; //指向 IPv6 的控制块, 不属本书范围
    #endif
    /* socket demultiplex comparisons on incoming packets.wumingxiaozu */ 
```

```txt
_be32 daddr; // 外部IPv4地址
_be32 rcv_saddr; //绑定的本地IPv4地址
_be16 dport; //目标端口
_u16 num; //本地端口
_be32 saddr; //源地址,即发送方的地址
_s16 uc_ttl; //单播TTL
_u16 cmsg_flags; //标志
struct ip_options *opt; //IP选项
_be16 sport; //源端口,即发送方的端口
_u16 id; //ID计数器
_u8 tos; //服务类型TOS
_u8 mc_ttl; //组播TTL
_u8 pmtudisc;
_u8 recverr:1,
	if_icsk:1, //是否INET的连接结构
freebind:1,
hdrincl:1,
mc_loop:1;
int mc_index; //组播设备的索引
_be32 mc_addr; //组播地址
struct ip_mc SOCKlist
*mc_list; //组播表
struct {
unsigned int flags; //标志
unsigned int fragsize; //IP片断的大小
struct ip_options *opt; //IP选项
struct dst_entry *dst; //路由项
int length; /*全部IP片断的总长度*/
_be32 addr;
struct flowi fl; //路由键值
}cork; //这些信息用于每个IP片断建立IP头部时使 
```

结构体最前端是 sock 结构，因而 sock 的地址就代表 inet_sock 结构地址，因此可以通过INET_SK 函数将 sock 指针转换成INET SOCK 结构指针。

sys_accept() $\rightarrow$ inet_getname() $\rightarrow$ inet_sk()   
static inline structINET_SOCKET \*inet_sk(const struct sock\*sk)   
{ return(structINET_SOCKET\*)sk; //将sock指针转换成inet_sock指针

读者可能对代表客户端的inet_sock结构有疑问，毕竟前面没有看到对它的创建和初始化；本书将答案放在第12章中讲述，提醒一点：对客户端inet_sock结构的初始化是在服务器的底层驱动向上层传递数据包的过程中完成的。

这里inet_getname()函数将客户端端口和地址记录在address数组后，还要将这些地址信息传回给服务器程序，地址结构变量client_address将接收这些内容，传回过程是由move_addr_to_user()函数完成的。

```c
err = move_addr_to_user(address, len, upeer SOCKaddr, upeer_addrlen);
```

至此，服务器建立了与客户端通信的“桥梁”。

# 准备连接请求

本章从客户端socket程序出发，沿着第二路线分析客户端与服务器的连接过程，读者可以参考图1.1分析路线图来看客户端程序。

# 代码清单 9.1 客户端程序代码

int main()  
{int client_fd;int server_len;struct sockaddr_in server_address;int result;char temp[] $\equiv$ {"Im client"，recv[20]；/\*像服务器一样，首先要创建客户端的socket，然后使用这个socket向服务器通信\*/client_fd $=$ socket(AF_INET,SOCK_STREAM,0);  
/\*还要设定服务器的“电话号码”，准备连接使用\*/server_address.sin_family $\equiv$ AF_INET;server_address.sin_addr.s_addr $\equiv$ inet_addr("192.168.6.88");server_address.sin_port $\equiv$ htons(9266);server_len $\equiv$ sizeof(server_address);  
/\*向服务器端发出连接请求，请求服务器同意接收\*/result $\equiv$ connect(client_fd,(struct sockaddr\*)&server_address,server_len);if(result $\equiv -1$ {perror("error:connect failed");exit(1);  
}  
/\*连接成功，开始向服务器发送字符并接收返回内容打印到控制台\*/write(client_fd,temp,20);read(client_fd,recv,20);printf("sent from server $\equiv \% \mathrm{s}\backslash \mathrm{n}^{\prime \prime},$ recv);close(client_fd);exit(0);

由于只使用了一台测试主机，因此客户端和服务器程序只能在同一系统中运行。这里将服务器程序放在后台，客户端程序放在前台，以此模仿两台主机的通信。

现在开始测试程序, 我们在 Linux 上同时启动服务器程序和客户端程序。

[root@localhost socket-book]# ./server & [4] 6638 server is waiting [root@localhost socket-book]# ./client received from client = Im client server is waiting sent from server $=$ I'm server

可以看出，程序的执行结果与预期相同，客户端与服务器连接成功并收发了字符。

代码清单9.1中首先创建客户端socket，它调用的socket()函数已经在服务器的分析过程中讲述了；只有connect()函数还没有分析，这个函数负责客户端与服务器的连接，如果该函数没有执行成功，那么服务器就无法接收到连接请求并建立它们的通信“桥梁”，先看对该函数的调用。

```c
connect(client_fd, (struct sockaddr *)&server_address, server_len) 
```

读者可以在 glibc-2.3.6/sysdeps/generic/connect.c 文件中找到它的定义, 并且可以在 glibc-2.3.6/sysdeps UNIX/sysv/linux/connect.S 中看到它的汇编入口; 同样可以参考第 15 章的 send() 函数分析从 connect() 到库函数再到系统调用的经过。

注意，第二个参数在程序中用于服务器地址，是电话通信例子中的“电话号码”。客户端必须将服务器地址传递给 connet()函数，指明目标才能完成与服务器的连接。

```txt
server_address.sin_family = AF_INET;  
server_address.sin_addr.s_addr = inet_addr("192.168.1.1");  
server_address.sin_port = 9266; 
```

服务器地址是 sockaddr_in 结构变量（代码清单 3.1），传递给 connect() 函数时必须转换成 sockadd 结构指针，这两种结构体在服务器的分析过程中看过了。

# 9.1 内核的连接函数

我们直接看sys_SOCKETcall()函数中的switch语句。

```txt
case SYS_connect:  
err = sys_connect(a0, (struct sockaddr __user * )a1, a[2]);  
break; 
```

3个参数分别对应服务器程序的client_fd、server_address和server_len，client_fd是客户端socket的ID，a1是服务器地址，a[2]是服务器地址的长度；如果不清楚它们的对应关系可以回顾第1章服务器创建socket时参数的对照过程，我们直接看sys_connect()函数。

sys_SOCKETcall() $\rightarrow$ sys_connect()   
asmlinkage long sys_connect (int fd, struct sockaddr _user $^*$ uservaddr, int addrlen)   
{ struct socket \* sock; char address[MAX SOCK_ADDR]; int err，fput_needed; sock $=$ sockfd.lookup_light(fd,&err,&fput_needed); //找到客户端的 socket if(!sock) goto out; err $=$ move_addr_to_kernel(uservaddr,addrlen,address);//将地址复制到内核空间 if( $\mathrm{err} <   0$ ) goto out_put; err $=$ security_SOCKET_connect(sock，(struct sockaddr\*)address，addrlen); if(err) goto out_put; err $=$ sock-> ops-> connect(sock，(struct sockaddr\*)address，addrlen, sock-> file-> f_flags);//调用具体协议的连接函数   
out_put: fput_light(sock-> file,fput_needed);   
out: return err;

代码前面几个函数都已经在服务器的分析过程中讲述了，先通过socketfd.lookup_light()函数找到客户端的socket，再通过move_addr_to_kernel()函数将服务器地址复制到这里的address数组中，这是因为客户端程序也是在用户空间运行，而连接函数位于内核空间，所以需要像服务器程序那样将地址结构复制到内核空间。

我们把重点放在 sock-> ops-> connect() 代码上。客户端与服务器创建 socket 过程完全相同, 也会调用 inlet_create() 函数将 inlet_STREAMOps 结构挂入 sock-> ops 中, 从而执行 inlet_STREAMOps 结构中的 connect() 函数。

```txt
const struct protoOps inlet_STREAMOps {
```

```txt
. connect = inet_STREAM_connect,   
}; 
```

可以看到connect()挂入的是inet_STREAM_connect()函数。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect()

int inletStream_connect(struct socket \* sock，struct sockaddr \*uaddr， int addr_len，int flags)   
{ struct sock \*sk $=$ sock->sk; int err; long timeo; lock_sock(sk); //加锁，如果sock锁被其他进程占用了，当前进程睡眠等待唤醒 if(uaddr->sa_family $= =$ AF_UNSPEC）{//检查所属协议族 err $=$ sk->skprot->disconnect(sk,flags); sock->state $=$ err?SS_DISCONNECTING:SS_UNCONNECTED; goto out; } switch (socket->state）{//判断客户端socket状态 default: err $=$ -EINVALID; goto out; case SS_CONNECTED://如果已经连接就直接返回 err $=$ -EISCONN; goto out; case SS_CONNECTED://如果正在连接就跳出语句 err $=$ -EALREADY; /\*Fall out of switch with err，set for this state\*/ break; case SS_UNCONNECTED://如果未连接就执行协议结构的连接函数 err $=$ -EISCONN; if(sk->sk_state！ $=$ TCP_CLOSE) goto out; //调用IPv4协议的连接函数 err $=$ sk->sk_prot->connect(sk,uaddr,addr_len); if(err<0)

goto out; sock->state $=$ SS_CONNECTED://正在连接状态 / \*Just entered SS_CONNECTED state; the only \* difference is that return value in non-blocking \* case is EINPROGRESS, rather than EALREADY. \*/ err $=$ -EINPROGRESS; break;   
} timeo $=$ sock_sndtimeo(sk,flags&0_NONBLOCK);//设置发送定时器 if((1<<sk->sk_state)&(TCPF_SYN_SENT|TCPF_SYN_RECV)){ /\*Error code is set above\*/ if(!timeo||!inet_wait_for_connect(sk,timeeo)//定时等待连接 goto out; err $=$ sock_intr_errno(timeo);//确定超时错误码 if (signal_pending(current))//是否有信号等待处理 goto out; } / \*Connection was closed by RST,timeout,ICMP error \* or another process disconnected us. \*/ if(sk->sk_state $= =$ TCP_CLOSE) goto sock_error; /\*sk->sk_err may be not zero now，if RECVERR was ordered by user \* and error was received after socket entered established state. \*Hence，it is handled normally after connect() return successfully. \*/ sock->state $=$ SS_CONNECTED;//设置客户端 socket状态为连接状态 err $= 0$ ：   
out: release SOCK(sk); //解锁，并唤醒 sock 锁上的其他进程

642 return err;   
643   
644 sock_error:   
645 err $=$ sock_error(sk)?：-ECONNABORTED;   
646 sock->state $\equiv$ SS_UNCONNECTED;   
647 if(sk->sk_prot->disconnect(sk,flags))//执行断开函数   
648 sock->state $\equiv$ SS_DISCONNECTING;   
649 goto out;   
650

这个函数先对客户端 sock 结构加锁, 然后判断服务器地址是否属于 AF_UNSPEC(未确定协议族), 如果相同就执行 tcp.proto 结构中的 disconnect() 函数 (sk->sk.proto 挂入的是 tcp.proto 结构), 它被设置为 tcpdisconnect(), 这个函数会断开连接、复位 socket 的相关结构, 这个过程不是我们关心的, 但是由此看出 connect() 函数也可以切断连接。

585行的switch语句则根据客户端sock的状态来执行相应的语句，客户端在创建过程中调用inet_create()函数将sock状态设置为未连接状态SS_UNCONNECTED。此时是第一次连接，因此执行SS_UNCONNECTED相关代码，系统调用函数可以被中断或者被其他进程抢占，因此这里还要在执行连接函数时判断一下客户端sock结构没有被关闭。601行调用的sk->sk_prot->connect()实际为tcp_prot结构的connect()函数，以前介绍过它是TCP协议结构体。

```txt
struct proto tcp.proto = {
    ...
    .connect = tcp_v4_connect,
    ...
}; 
```

connect()挂入的是tcp_v4_connect()函数，该函数较大因此分段来看。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inlet_STREAM_connect() $\rightarrow$ tcp_v4_connect()   
146 int tcp_v4_connect(struct sock \*sk，struct sockaddr \*uaddr，int addr_len)   
147{   
148 struct inet SOCK \*inet $=$ INET_sk(sk);   
149 struct tcp SOCK \*tp $=$ tcp_sk(sk);   
150 struct sockaddr_in \*usin $=$ (struct sockaddr_in \*)uaddr;   
151 struct rtable \*rt;   
152 __be32 daddr，nexthop;   
153 int tmp;   
154 int err;

```c
if (addr_len < sizeof(struct sockaddr_in))//地址长度是否相符 return - EINTRVAL;   
if (usin-> sin_family != AF_INET)//是否属于INET协议族 return - EAFNOSUPPORT;   
nexthop = daddr = usin-> sin_addr.s_addr;//记录服务器IP地址 if (inet-> opt &&inet->opt->srr){//是否设置了IP选项结构，并指定了源路由 if(!daddr) return - EINTRVAL; nexthop =inet->opt->faddr;//跳转地址为转发地址   
} //查找路由表   
tmp = ip-route_connect(&rt, nexthop,inet->saddr, RT_CONN_FLAGS(sk),sk->sk_bound_dev_if, IPPROTO_TCP, inlet->sport,usin->sin_port,sk,1);   
if (tmp<0){ if (tmp == -ENETUNREACH) IP_INC_STATISTICS_BH(IPSTATS_MIB_OUTNOROUTES); return tmp;   
} //路由表是组播或者广播类型就放弃使用   
if (rt->rt_flags&(RTCF Multicast|RTCF_BROADCAST)){ ip_rt_put(rt); return - ENETUNREACH;   
} //检查IP选项   
if (!inet->opt||!inet->opt->srr) daddr = rt->rt.dst;//使用路由表中的地址作为目标地址   
if (!inet->saddr)//没有指定源地址 inlet->saddr = rt->rt_src;//使用路由表中的地址作为源地址 inlet->rcv_saddr = inlet->saddr;//接收地址与源地址相同 //接收过但是现在地址已经改变，需要复位   
if (tp->rx_opt.tsrecent_stamp&&inet->daddr != daddr){ /\*Reset inherited state\*/ tp->rx_opt.tsrecent = 0;
```

```c
194 tp->rx_opt.tsrecent_stamp = 0;   
195 tp->write_seq = 0;   
196 }   
197 
```

参数uaddr是服务器地址结构，之前复制到内核空间时转换为struct sockaddr结构类型，这是系统调用入口的通用结构，这里将它还原为struct sockaddr_in结构类型。代码156行检查服务器地址长度是否符合标准以及是否是INET协议族。162行先用nexthop记录下服务器地址。

inet->opt是ip_options结构体，是IP选项内容。我们可以在应用程序中通过setsockopt()函数设置这个IP选项，经系统调用传递到内核sys_SOCKETcall()函数，通过层层协议结构的中转最终IP选项在ip_setsockopt()函数中被记录到inet SOCK结构内，也就是这里的inet->opt中。

IP 选项结构体的定义将在本章后面列出。163 行检查这个结构体是否存在，接着再检查是否指定了源路由。代码中的 SRR 是 Source and Record Routes 的意思，在本书中称之为源路由。内核中指定了两种源路由类型 LSRR 和 SSRR，即宽松源路由称为 Loose SRR(LSRR) 和严格源路由称为 Strict SRR(SSRR)。

如果指定了源路由就会将nexthop修改为IP选项的faddr，它保存着转发地址。169行处调用了ip-route_connect()函数，这个函数已经在4.5节讲述了，它负责查找缓存中的路由表，如果没有找到就会创建一个新的路由表。

173行需要对返回值进行检查，找到的路由表如果是多播或者广播类型，就放弃这个路由表并返回。184行检查inet的IP选项内容，daddr记录路由表的目标地址。188行根据路由表的地址修改inet的源地址和接收地址，从接收的时间判断最近是否接收过、目标地址是否改变，如果已经改变就将tcp_sock结构的有关内容复位。

我们接着看 tcp_v4_connect()函数余下的代码。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inlet_STREAM_connect() $\rightarrow$ tcp_v4_connect()   
198 if (tcp_death_row.sysctl_tw_recycle &&   
199！tp->rx_opt.tsrecent_stamp&&rt->rt.dst $= =$ daddr）{//接收过且地址未改变   
200 struct inletpeer \*peer $=$ rt_getpeer(rt);//获取对方信息   
201 /\*   
202 \*VJs idea.We save last timestamp seen from   
203 \*the destination in peer table,when entering state   
204 \*TIME-WAIT\*and initialize rx_opt.tsrecent from it,   
205 \*when trying new connection.   
206 \*/   
207 if (peer! $\equiv$ NULL &&

```c
peer->tcp tsstamp + TCP_PAWS_MSL >= getSeconds(); //调整时间戳
tp->rx_opt.tsrecent_stamp = peer->tcp ts stamp;
tp->rx_opt.tsrecent = peer->tcp ts;
}
}
inet->dport = usin->sin_port; //记录指定的端口
inet->daddr = daddr; //记录路由表的目标地址
inet_csk(sk) -> icsk_ext_hdr_len = 0; //初始化网络层头部
if (inet->opt)
    inlet_csk(sk) -> icsk_ext_hdr_len = inlet->opt->optlen; //记录IP选项规定长度
tp->rx_opt.mss_clamp = 536; //设置MSS最大分段值
/* socket identity is still unknown (sport may be zero).
* However we set state to SYN-SENT and not releasing socket
* lock select source port, enter ourselves into the hash tables and
* complete initialization after this.
*/
tcp_set_state(sk, TCP_SYNSENT); //修改sock状态为SYN状态
err = inlet_hash_connect(&tcp_death_row, sk); //检查端口是否可用
if (err)
    goto failure;
err = ip-route�新ports(&rt, IPPROTO_TCP,
            inlet->sport, inlet->dport, sk); //检查或者创建路由表
if (err)
    goto failure;
/* OK, now commit destination to socket. */
sk->sk_gso_type = SKB_GSO_TCPV4; //设置分段类型
sk_setupCaps(sk, &rt->u.dst); //设置分段标志和分段值
if (!tp->write_seq)
    tp->write_seq = secure tcp_sequence_number(inet->saddr,
            inlet->daddr,
            inlet->sport, 
```

```c
246 usin->sin_port); //计算发送序号  
247  
248 inlet->id = tp->write_seq~jiffies; //设置inet_sock结构的ID  
249  
250 err = tcp_connect(sk); //发送SYN数据包  
251 rt = NULL;  
252 if (err)  
253 goto failure;  
254  
255 return 0;  
256  
257 failure:  
258 /\*  
259 \* This unhashes the socket and releases the local port,  
260 \* if necessary.  
261 \*/  
262 tcp_set_state(sk, TCP_CLOSE); //设置关闭状态  
263 ip_rt_put(rt); //放弃路由表  
264 sk->sk-routeCaps = 0; //清除兼容标志  
265 inlet->dport = 0; //清除端口  
266 return err;  
267} 
```

函数前面的inet SOCK和tcp SOCK结构指针都是通过sock结构取得的：

```c
struct inlet_sock * inlet = inlet_sk(sk);
struct tcp_sock *tp = tcp_sk(sk);
static inline struct inlet_sock * inlet_sk(const struct sock *sk) {
    return (struct inlet_sock *)sk;
} 
static inline struct tcp_sock *tcp_sk(const struct sock *sk) {
    return (struct tcp_sock *)sk;
} 
```

这里调用的两个函数inet_sk()和tcp_sk()都是直接把sock指针转换成inet SOCK指针和tcp SOCK指针；如果看一下inet SOCK的结构会发现它的开始处是sock结构，因而sock结构指针可以作为inet SOCK结构指针。虽然tcp SOCK结构的开始处是inet_connection SOCK结构，但是这个结构开始处也是sock结构，所以sock指针可以直接作为tcp SOCK指针使用。

tcp SOCK 结构是代表 TCP 协议的 socket 结构体, 它的定义非常大, 并且 tcp SOCK 还包括 inlet SOCK 结构, 因此这里就不列出其内容了。sock 结构空间是根据 tcp SOCK 的长度进行分配的, 因此 sock 指针可以直接转换成多种结构体的指针使用, 如图 9.1 所示。

![](images/2b34f47e408db06db63cb7abd6069c4b9ff7830394e34992957371f66601fe8c.jpg)  
图9.1 tcp_sock 结构的包含关系

读者可以回忆一下创建socket时调用inet_create()函数，它使用sk_alloc()分配sock结构空间：

```txt
sk = sk_alloc(net, PF_INET, GFP_KERNEL, answerprot); 
```

answerprot已经挂入了tcpprot结构，sk_alloc()调用skprot_alloc()函数时从该结构中获取空间长度kmalloc(prot->obj_size，priority)。注意，prot就是tcpprot。

tcpprot->obj_size的长度值被设置为：sizeof(struct tcp_sock)

由此看出，分配 sock 空间时连同 tcp_sock 空间一起分配了，因此 sock 指针作为 tcp_sock 结构指针以及其他结构指针是完全合理的。

函数198行看到全局结构体变量tcp_death_row，它被tcp定时器使用，也作为超时重传的依据。这里检查其sysctl_tw_recycle是否存在，用来指示是否有定时器可以回收利用。200行代码的主要作用就是快速从路由表中得到对方的信息，这些信息都封装在struct inet_peer结构体中，根据这些信息调整接收时间。

代码214行根据客户端程序指定的服务器端口以及路由表的目标地址对inet进行设置，daddr记录着路由表的目标地址，这取决于184行对IP选项的检查。

217 行先设置连接结构的网络层头部长度icsk_ext_hdr_len 为 0，再根据inet 的 IP 选项内容的头部长度进行调整，接着设置mss分段最大值，并调用tcp_set_state()函数修改sock的状态为发送状态。

229 行处调用了 inset_hash_connect()函数，它用来检查端口是否可以使用。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ INET hash_connect() $\rightarrow$ __inet_hash_connect()

```c
int inset_hash_connect(struct inset.timewait_death_row \*death_row, struct sock \*sk)   
{ return__inet_hash_connect(death_row，sk，inet_sk_port_offset(sk), _inet_check_established，_inet_hash_nolisten); 
```

它调用了__inet_hash_connect()函数：

415int __inet_hash_connect(struct inset.timewait_death_row * death_row,  
416 struct sock \* sk，u32 port_offset,  
417 int（\*check_established)(struct inset.timewait_death_row \*，  
418 struct sock \*，_u16，struct inset.timewait SOCK \* \*），  
419 void（\*hash)(struct sock \*sk))  
420{  
421 struct inset_hashinfo \*hinfo $=$ death_row-> hashinfo;  
422 const unsigned short snum $=$ inlet_sk(sk) $->$ num;  
423 struct inset_bind_hashbucket \*head;//哈希桶结构指针  
424 struct inset_bind_buckets \*tb;//桶结构指针  
425 int ret;  
426 struct net \*net $=$ sock_net(sk);  
427  
428 if（！snum）{//如果还没有指定本地端口  
429 int i,remaining,low,high,port;  
430 static u32 hint;  
431 u32 offset $=$ hint $^+$ port_offset;  
432 struct hlist_node \*node;  
433 struct inset(TIMewait_sock \*tw $=$ NULL;  
434  
435 inset_get_local_port_range(&low,&high);//获取端口的取值范围  
436 remaining $=$ (high - low) + 1;  
437  
438 local_bh_disable();  
439 for（i = 1；i <= remaining；i++）{//查找未被占用的端口  
440 port $=$ low $^+$ （i+offset）%remaining;  
441 head $=$ &hinfo->bhash[inet_bhashfn.port,hinfo->bhash_size)];  
442 spin_lock(&head->lock);

```txt
/\*Does not bother with rcv_saddr checks, \* because the established check is already \* unique enough. 
```

//在哈希桶队列中查找相同端口的桶结构  
```c
inet_bind_BUCKET_for_each(tb, node, &head->chain) {
    if (tb->ib_net == net && tb->port == port) {
        BUG_TRAP(! hlist_empty(&tb->owners));
        if (tb->fastreuse >= 0)//如果桶结构正在使用就查找下一个 goto next_port;
            if (!check_established(death_row, sk, port, &tw))//端口是否已经长时间不用 goto ok;
                goto next_port;
        }
} 
tb = inet_bind_buckets_create(hinfo->bind_buckets_cachep, net, head, port); //创建一个桶结构
if (!tb) {
    spin_unlock(&head->lock);
    break;
    }
    tb->fastreuse = -1;
    goto ok;
    next_port:
        spin_unlock(&head->lock);
    }
    local_bh_enable();
    return -EADDRNOTAVAIL; 
```

$\mathrm{hint} + = \mathrm{i};$

```txt
/\*Head lock still held and bhs disabled \*/ 
```

```txt
inet_bind_hash(sk, tb, port); //sock结构入队，并记录桶结构
```

```c
481 if (sk_unhashed(sk)){//是否已链入哈希桶  
482 inset_sk(sk) -> sport = htons.port); //记录端口号  
483 hash(sk); //链入哈希桶  
484 }  
485 spin_unlock(&head->lock);  
486  
487 if (tw) {  
488 inset_twsk_deschedule(tw, death_row); //将定时等待结构脱链  
489 inset_twsk_put(tw); //释放定时等待结构  
490 }  
491  
492 ret = 0;  
493 goto out;  
494 }  
495 //如果指定了端口就直接执行这里  
496 head = &hinfo->bhash[inet_bhashfn(snum, hinfo->bhash_size)]; //取得哈希桶  
497 tb = inlet_csk(sk) -> icsk_bind_hash; //取得连接结构中的桶结构指针  
498 spin_lock_bh(&head->lock);  
//如果桶结构与客户端 sock 是对应关系，并且 sock 没有链入到哈希桶中  
499 if (sk_head(&tb->owners) == sk && ! sk->sk_bind_node.next) {  
500 hash(sk); //链入哈希桶  
501 spin_unlock_bh(&head->lock);  
502 return 0;  
503 } else { //如果桶结构不与客户端 sock 对应，并且也已经链入到哈希桶  
504 spin_unlock(&head->lock);  
505 /* No definite answer... Walk to established hash table */  
506 ret = check_established(death_row, sk, snum, NULL); //端口是否可以复用  
507out:  
508 local_bh_enable();  
509 return ret;  
510 }  
511}
```

如果客户端还没有指定本地端口 snum，则代码就会推荐一个端口并且在哈希桶队列中检查是否有相同端口的桶结构，以此判断该端口是否被占用。这种循环语句在 3.4 节详细分析过了，客户端对端口的查找与服务器调用 inlet_csk_get_port() 函数的内容大致相同，并且这里的几个结构也都在 3.4 节介绍了。

check_established函数指针指定为__inet_check Established()，它在用来检查端口是否已经超时不用，由此确定是否复用该端口。如果循环检查完了也没有找到对应端口的桶结构，

则通过inet_bind_buckets_create()函数为推荐端口创建一个桶结构。接着调用inet_bind_hash()函数，将sock结构链入到桶结构tb->owners队列中，再将桶结构记录到inet SOCK中，inet_bind_hash()函数也在3.4节分析了。

481行处还要检查sock结构是否已经链入了哈希桶中，这是通过检查它的哈希节点判断的。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ INET hash_connect() $\rightarrow$ __inet_hash_connect() $\rightarrow$ sk_unhashed()

```c
static inline int sk_unhashed(const struct sock \*sk)   
{ return hlist_unhashed(&sk->sk_node); } static inline int hlist_unhashed(const struct hlist_node \*h) { return!h->pprev; 1 
```

如果还没有链入到哈希桶，就把可用的端号记录到inet_sock结构中，然后调用参数指针hash()，它被指定为__inet_hash_nolisten()。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ INET hash_connect() $\rightarrow$ __inet_hash_connect() $\rightarrow$ __inet_hash_nolisten()

336void __inet_hash_nolisten(struct sock *sk)  
337{  
338 structINET_hashinfo \*hashinfo $=$ sk->skprot->h.hashinfo;  
339 struct hlist_head \*list;  
340 rwtlock_t \*lock;  
341 structINET_ehash_buckets \*head;  
342  
343 BUG_TRAP(sk_unhashed(sk));  
344  
345 sk->sk_hash $=$ INET_sk_ehashfn(sk);//计算哈希值  
346 head $=$ INET_ehash_bucketsHashinfo,sk->sk_hash);//在队列中确定所属哈希桶  
347 list $=$ &head->chain;//取得队列头  
348 lock $=$ INET_ehash_lockpHashinfo,sk->sk_hash); //取得队列锁  
349  
350 write_lock lock); //锁住队列  
351 __sk_add_node(sk, list); //链入队列  
352 sockprot_inuse_add(sock_net(sk),sk->skprot,1); //增加协议结构计数

```txt
353 write_unlock lock); 354 
```

函数出现的结构都在3.4节看过了，338行的h hashinfo之前被设置为了&tcp_hashinfo，该结构内部有专门用来管理端口的sock哈希桶队列。函数先为sock结构计算一个哈希值，然后找到所属的哈希桶，再把sock链入到队列中。

前面都是基于客户端没有指定本地端口的情形；如果客户端指定了本地端口就直接运行到496行处，先取得连接结构中的桶结构指针，检查是否与客户端的sock是对应关系。如果是对应关系而sock还没有入队，则执行入队操作；如果不是对应关系并且已经入队，则查看是否已经超时，超时就可以复用。

回到 tcp_v4_connect()函数233行处，既然端口的检查已经完成，则调用ip route_newports函数，查看路由表是否对应端口的内容，由此判断是否创建一个新的路由表。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ ip-route_newports()

177static inline int ip-route�新ports(struct rtable \* $\text{串}$ rp,u8 protocol,  
178 __be16 sport，_be16 dport，struct sock \*sk)  
179{  
180 if(sport！ $= (\ast$ rp)->fl.fl_ip_sport||  
181 dport！ $= (\ast$ rp)->fl.fl_ip_dport）{//源端口与目标端口与路由表中的不同  
182 struct flowi fl;  
183  
184 memcpy(&fl，&( $\ast$ rp)->fl,sizeof(fl)）;//复制路由表的键值内容  
185 fl.fl_ip_sport $=$ sport;//修改为设置的源端口  
186 fl.fl_ip_dport $=$ dport;//修改为设置的目标端口  
187 fl.proto $=$ protocol;//使用IP协议  
188 ip_rt_put( $\ast$ rp);//放弃对原来路由表的使用  
189 $\ast$ rp $=$ NULL;  
190 security_sk_classify_flow(sk,&fl);  
191 return ip-route_output_flow(sock_net(sk)，rp，&fl，sk，0）;//创建路由表  
192 }  
193 return 0;  
194}

检查客户端本地端口和目标端口是否与路由表中的记录相同，如果不同就准备路由键值fl，然后创建新的路由表。创建路由表是由ip route_output_flow()函数完成的，这个函数在4.5节讲述了。

tcp_v4_connect()函数前面的准备工作做完后，接着在239行设置GSO分段类型。GSO

是 Generic Segmentation Offload 的缩写, 意思是通用分段值, 它的策略是尽可能向后推迟分段, 理想时间是在网卡驱动里分段, 在网卡驱动里把大包 (super-packet) 拆分成 SG list 或者先将一块分配好的内存重组分段, 再交给网卡驱动。

接下来调用sk_setup_caps()函数增加兼容标志位的GSO分段标志，记录GSO分段值。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ sk_set-upCaps()

1083void sk_setup_caps (struct sock \*sk, struct dst_entry \*dst)   
1084{   
1085 __sk.dst_set(sk, dst);   
1086 sk->sk_route_caps $=$ dst->dev->features; //兼容标志取自于设备的特性   
1087 if (sk->sk-route_caps & NETIF_F_GSO)//是否支持 GSO 分段   
1088 sk->sk-route_caps |= NETIF_F_GSO_SOFTWARE; //增加分段标志   
1089 if (sk_can_gso(sk)){//支持分段类型   
1090 if (dst->header_len){   
1091 sk->sk-route_caps & = \~NETIF_F_GSO_MASK;   
1092 } else {   
1093 sk->sk-routecaps $|\equiv$ NETIF_F_SG|NETIF_F_HW_CSUM;   
1094 sk->sk_gso_max_size = dst->dev->gso_max_size;//记录 GSO 的分段值   
1095 }   
1096 }   
1097}

242行根据地址和端口号生成发送序号，inet_sock结构的索引ID也是根据发送序号与内核的时间节拍jiffies计算而成的。

250 行执行 tcp_connect() 函数发送第一次握手的 SYN 数据包。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect()

2337int tcp_connect(struct sock \*sk)   
2338{   
2339 struct tcp SOCK \*tp $=$ tcp_sk(sk);   
2340 struct sk_buffer \*buff;   
2341   
2342 tcp_connect_init(sk);//初始化tcp SOCK结构内容   
2343 //为SYN准备数据包   
2344 buff $=$ alloc_skb_fclone(MAX.tcpHEADER $+15$ ,sk->sk_allocation);   
2345 if (unlikely(buffer == NULL))   
2346 return -ENOBUFS;

/\* Reserve space for headers. \*/

skb_reserved(buffer, MAX_TCPHEADER); //在缓冲块中开辟TCP头部空间

tp->snd_nxt = tp->write_seq; //记录发送序号

建 SYN 非数据类型的通用控制位, 并自增应答序号。

tcp_init_nodata_skb(buffer, tp->write_seq++, TCPCB_FLAG_SYN);

TCP_ECN_send_syn(sk, buff); //设置 ECN 拥塞标志

/\* Send it off. \*/

TCP_SKB_CB(buffer) -> when = tcp_time_stamp; //控制结构中记录发送时间

tp->retrans_stamp = TCP_SKB_CB(buffer) -> when; //初始化重发时间

skb_header_release(buffer); //设置没有头部 nohdr 标志位

__tcp_add_write_queueTAIL(sk,buff);//将数据包链入发送队列

sk-> sk_wmem_queueud + = buff-> truesize; //调整队列长度

sk_mem_charge(sk, buff-> truesize); //调整预分配长度

tp-> packets_out + = tcp_skb_pcount(buffer); //调整“飞行中”的数据包计数器

tcp_transmit_skb(sk, buff, 1, GFP_KERNEL); //发送数据包

/\*We change $\mathfrak{tp} - >$ snd_nxt after the tcp_transmit_skb() call

* in order to make this packet get counted in tcpOutSegs.

￥/

tp->snd_nxt = tp->write_seq; //记录下一个发送序号

tp->pushed_seq = tp->write_seq; //上一次PUSH的序号

TCP_INC_STATSTS(TCP_MIB_ACTIVEOPENS);

/\*Timer for repeating the SYN until an answer. \*/

inet_csk_reset_xmit_timer(sk, ICSK_TIME_RETRANS,

```c
inet_csk(sk) -> icsk_rto, TCP_RTO_MAX); //设置用于重发 SYN 的定时器

return 0;

这里先调用tcp_connect_init()函数初始化tcp SOCK结构。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_connect_init()

2280static void tcp_connect_init(struct sock *sk)

2281{

2282 struct dst_entry \*dst $=$ _sk.dst_get(sk);

2283 struct tcp_sock \*tp $=$ tcp_sk(sk);   
2284 u8 rcv_wscale;   
2285   
2286 /\*Well fix this up when we get a response from the other end.   
2287 \* See tcp_input.c:tcp_rcv_state_process case TCP_SYNSENT.   
2288 \*/   
2289 tp-> tcp_header_len = sizeof(struct tcphdr) +   
2290 (sysctl tcp_timestamps?TCPOLEN_TSTAMP_ALIGNED:0);//记录头部长度   
2291   
2292# ifdef CONFIG.tcp_MD5SIG   
2293 if(tp->afspecific->md5.lookup(sk,sk)! $=$ NULL)   
2294 tp-> tcp_header_len $+ =$ TCPOLEN_MD5SIG_ALIGNED;   
2295 #endif   
2296   
2297 /\*Ifuser gave his TCP_MAXSEG,record it to clamp\*/   
2298 if(tp->rx_opt.user_mss)   
2299 tp->rx_opt.mss_clamp $=$ tp->rx_opt.user_mss;//记录指定的MSS值   
2300 tp->max_window $= 0$ //最大窗口值   
2301 tcp_mtop_init(sk);//初始化MTU内容   
2302 tcp-sync_mss(sk,dst_mtu(dst));//确定同步发送的MSS   
2303   
2304 if(!tp->window_clamp)   
2305 tp->window_clamp $=$ dst_metric(dst,RTAX WINDOW);//记录窗口值   
2306 tp->advmss $=$ dst_metric(dst,RTAX_ADVMSS);//记录对外宣布的MSS值   
2307 tcp_initiize_rcv_mss(sk);//初始化接收的MSS值   
2308   
2309 tcp_select_init_window(tcp_full_space(sk),   
2310 tp->advmss-(tp->rx_opt.tsrecent_stamp?tp->tcp_header_len-sizeof   
(struct tcphdr）:0),   
2311 &tp->rcv_wnd,   
2312 &tp->window_clamp,   
2313 sysctl tcp_window_scaling,   
2314 &rcv_wscale);//确定窗口比例和窗口值   
2315   
2316 tp->rx_opt.rcv_wscale $=$ rcv_wscale;//设置接收方的发送窗口比例   
2317 tp->rcv_ssthresh $=$ tp->rcv_wnd;//记录当前使用的窗口   
2318   
2319 sk->sk_err $= 0$ ：   
2320 sock_reset_flag(sk，SOCK_DONE);//复位标志位

```c
2321 tp->snd_wnd = 0; //希望接收的窗口  
2322 tcp_init wl(tp, tp->write_seq, 0); //记录发送序号  
2323 tp->snd_una = tp->write_seq; //应答时的第一个字节  
2324 tp->snd_sml = tp->write_seq; //最后一个字节  
2325 tp->rcv_nxt = 0; //下一个要接收的  
2326 tp->rcv_wup = 0; //窗口更新时最后一次rcv_nxt  
2327 tp->copied_seq = 0; //未读的数据头  
2328  
2329 inlet_csk(sk) -> icsk_rto = TCP_TIMEOUT_INIT; //重发时间限  
2330 inlet_csk(sk) -> icsk_retransmits = 0; //重发数目  
2331 tcp_clear_retrans(tp); //复位重发计数器  
2332}
```

代码2282行声明了路由项dst_entry结构指针dst，这个结构体在前面已经出现了多次。

# 代码清单9.2 dst_entry结构代码

```c
struct dst_entry   
{ struct rcu_head rcu_head; //用于互斥目的 struct dst_entry \*child; //用于IPsec struct net_device \*dev; //网络设备结构 short error; //错误码 short obsolete; //标识结构用途 int flags; //标志位   
#define DST_HOST1 //表示路由目标是主机 #define DST_NOXFRM2 //以下三项用于IPsec，无XFRM框架 #define DST_NOPOLICY4 //无策略   
#define DST_NOHASH8 //无哈希 unsigned long expires; //过期时间 unsigned short header_len; /\*more space at head required \*/ unsigned short trailer_len; /\*space to reserve at tail \*/ unsigned int rate_tokens; //已经发送的ICMP定向消息 unsigned long rate_last; /\*ICMP限制率\*/ struct dst_entry \*path; //用于IPsec struct neighbour \*neighbour; //邻居结构 struct hh_cache \*hh;//链路层头部的缓存 struct xfrm_state \*xfrm; //XFRM状态 int (\*input)(struct sk_buffer \*)；//接收函数 int (\*output)(struct sk_buffer \*)；//发送函数 struct dstOps \*ops; //路由项函数表
```

u32 metrics[RTAX_MAX]; //各种负载值，包括MTU、MSS和窗口值，初化时取自于路由信息结构中//的负载数组

```c
# ifdef CONFIG_NET_CLS_ROUTer
    __u32 tklassid;
#endif
    /
        * __refcnt wants to be on a different cache line from
        * input/output/ops or performance tanks badly
        /
        atomic_t __refcnt; /* 使用计数器 */
        int __use; //路由项已经被使用的次数
        unsigned long lastuse; //最后一次使用时间
        union {
            struct dst_entry * next; //指向哈希桶队列中的下一个路由项
            struct rtable * rt_next; //指向哈希桶队列的下一个路由表
            struct rt6_info * rt6_next; //IPv6使用这两项
            struct dn-route * dn_next;
        };
);
```

代码首先从 sock 结构获取路由项的结构指针, 然后根据它内部的信息来设置 tcp_sock 结构的内容。

2301行调用tcp_ntup_init()函数对连接结构的MTU内容进行设置。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inlet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_connect_init() $\rightarrow$ tcp(mtup_init() 899void tcp_mtup_init(struct sock \*sk)   
900{   
901 struct tcp SOCK \*tp $=$ tcp_sk(sk);   
902 struct inlet_connection SOCK \*icsk $=$ inlet_csk(sk);   
903   
904 icsk->icsk_mtup.enabled $=$ sysctl tcp_mtu probing $>1$ //MTU启用标志位   
905 icsk->icsk_mtup.search_high $=$ tp->rx_opt.mss_clamp + sizeof(struct tcphdr）+icsk ->icsk.af ops->net_header_len;//MTU的搜索范围，上限与下限

```c
// tcp_mss_to_mtu()根据MSS计算MTU值  
907 icsk->icsk_mtup.search_low = tcp_mss_to_mtu(sk, sysctl tcp_base_mss);  
908 icsk->icsk_mtup.probe_size = 0; //当前搜索大小  
909} 
```

这个函数是对MTU最大传输单元的一些设置，MTU是MaxitumTransmissionUnit的

缩写。在连接结构中有一个icsk_mtup结构，它就是MTU数据结构，是IP分段的依据。

MTU与链路层相关，决定着以太网数据帧的大小。网络层的IP协议要根据MTU对数据包进行分段（或称作分片），例如，以太网的MTU是1500字节。

MSS 是最大分段值, 即 Maximum Segment Size, 与传输层相关, 是 TCP 协议中的一个概念。传输层根据 MSS 对数据包进行分段, 达到最佳的传输效能。这个值一般比 MTU 值要小, 需要减去 TCP 头部的大小 (20 字节) 和 IP 头部的大小 (20 字节), 一般取值为 1460 字节。

通信公司（如中国电信）为了增加认证计费功能，使用PPPoE协议，它处于链路层之中，因此以太网MTU的值需要减去该协议的头部和尾部共8个字节，变成了1492字节，MSS的值也要随之调整为1452字节，才能保证可靠的通信。

根据上面MSS与MTU的关系，我们看函数tcp-sync_mss()，注意传递的pmtu参数是从路由项中取得的MTU值。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_connect_init() $\rightarrow$ tcp-sync_mss()

942 unsigned int tcp_sync_mss(struct sock \*sk，u32 pmtu)   
943{   
944 struct tcp SOCK \*tp $=$ tcp_sk(sk);   
945 struct inlet_connection_sock \*icsk $=$ inlet_csk(sk);   
946 int mss_now;   
947   
948 if(icsk->icsk_mtup.search_high $\rightharpoondown$ pmtu)//检查MTU最大值是否超出指定MTU   
949 icsk->icsk_mtup.search_high $=$ pmtu;//修改为指定的MTU   
950   
951 mss_now $=$ tcp_mtu_to_mss(sk，pmtu);//根据MTU计算MSS   
952 mss_now $=$ tcp_bound_to另一半WND(tp，mss_now);//调整MSS为窗口半值   
953   
954 /\*And store cached results\*/   
955 icsk->icsk_mtup_cookie $=$ pmtu;//记录MTU值   
956 if(icsk->icsk_mtup.enabled)   
957 mss_now $=$ min(mss_now，tcp_mtu_to_mss(sk，icsk->icsk_mtup.search_low));//根据MTU 最小值调整MSS   
958 tp->mss_cache $=$ mss_now;//记录MSS值   
959   
960 return mss_now;   
961}

函数利用MTU值以及窗口值来计算最佳的MSS值，并记录到tcp_sock结构中。调用的tcp_mtu_to_mss()函数以及前面的tcp_mss_to_mtu()函数都是对两者的转换计算。

tcp_connect_init()函数2304行调用了dst_metric()函数，取得路由项设置的窗口值，这个值保存在它的metrics数组中，2302处使用的dst_mtu()也是从该数组中取MTU值。2306再次使用dst_metric()函数取得公开用的MSS，这个值用于通知接收方自己的MSS值。

sys_SOCKETcall() $\rightarrow$ sys_connect(） $\rightarrow$ inet_STREAM_connect(） $\rightarrow$ tcp_v4_connect(） $\rightarrow$ tcp_   
connect(） $\rightarrow$ tcp_connect_init(） $\rightarrow$ dst_metric()   
static inline u32 dst_metric(const struct dst_entry \*dst，int metric)   
{ return dst->metrics[metric-1]；//取得对应类型的负载值

函数接着在2307行进入tcp_initilize_rcv_mss()中初始化接收用MSS的大小。

sys_SOCKETcall() $\rightarrow$ sys_connect(） $\rightarrow$ inet_STREAM_connect(） $\rightarrow$ tcp_v4_connect(） $\rightarrow$ tcp_connect(） $\rightarrow$ tcp_connect_init(） $\rightarrow$ tcp_initializer_rcv_mss()  
409 void tcp_initializer_rcv_mss(struct sock \*sk)  
410{  
411 struct tcp_sock \*tp $=$ tcp_sk(sk);//取本地MSS与公开MSS的最小值  
412 unsigned int hint $=$ min_t(unsigned int,tp->advmss,tp->mss_cache);  
413  
414 hint $=$ min(hint,tp->rcv_wnd/2);//与接收窗口一半做比较，取最小值  
415 hint $=$ min(hint,TCP_MIN_RCVMSS);//内核的规定最小值比较  
416 hint $=$ max(hint,TCP_MIN_MSS);  
417  
418 inlet_csk(sk)->icsk ACK. rcv_mss $=$ hint;//记录到连接结构中  
419}

接着2309行调用tcp_select_initializer_window()函数初始化滑动窗口值。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inlet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_connect_init() $\rightarrow$ tcp_select_initializer() 179 void tcp_select_initializer_window (int __space, __u32 mss, 180 __u32 *rcv_wnd, __u32 *window_clamp, 181 int wscale.ok, __u8 *rcv_wscale) 182{//初始化滑动窗口 183 unsigned int space $=$ (_space $<  0?$ :__space); 184 185 /\* If no clamp set the clamp to the max possible scaled window \*/

if (\*window_clamp $\equiv = 0$ $(\ast$ window_clamp) $= (65535 <   <   14)$ space $=$ min(\*window_clamp, space);   
/\* Quantize space offering to a multiple of mss if possible. /\* if (space $\rightharpoondown$ mss) space $=$ (space / mss) $\text{水}$ mss;   
/\* NOTE: offering an initial window larger than 32767 \* will break some buggy TCP stacks. If the admin tells us \* it is likely we could be speaking with such a buggy stack \* we will truncate our initial window offering to $32\mathrm{K} - 1$ \* unless the remote has sent us a window scaling option, \* which we interpret as a sign the remote TCP is not \* misinterpreting the window field as a signed quantity.   
\*/   
if (sysctl tcp_workaround_signed_windows) $(\ast \mathrm{rcv\_wd}) = \min (\mathrm{space},\mathrm{MAX\_TCP\_WINDOW})$ .   
else $(\ast \mathrm{rcv\_wd}) = \mathrm{space};$ $(\ast \mathrm{rcv\_wscale}) = 0;$ if (wscale ok) { /\* Set window scaling on max possible window \* See RFC1323 for an explanation of the limit to 14 \*/ space $=$ max_t(u32, sysctl tcp_rmem[2], sysctl_rmem_max); space $=$ min_t(u32, space, \*window_clamp); while (space $>65535\& \&$ (\*rcv_wscale) $<  14$ { space $>> = 1$ $(\ast \mathrm{rcv\_wscale}) + + ;$ 1   
/ \* Set initial window to value enough for senders,   
\* following RFC2414. Senders, not following this RFC, \* will be satisfied with 2.   
\*/   
if (mss $>$ (1<< \*rcv_wscale)) {

225 int init_cwnd = 4;   
226 if (mss > 1460 * 3)   
227 init_cwnd = 2;   
228 else if (mss > 1460)   
229 init_cwnd = 3;   
230 if (\*rcv_wnd > init_cwnd \* mss)   
231 \*rcv_wnd = init_cwnd \* mss;   
232 }   
233   
234 /\* Set the clamp no higher than max representable value \*/   
235 (\*window_clamp) $=$ min(65535U $<   <   (\ast$ rcv_wscale), \*window_clamp);   
236}

这个函数是关于滑动窗口的内容，并且注释说明也比较清晰。

# 9.2 分配数据包结构和数据块空间

tcp_connect()函数2344行调用了alloc Skyl_fclone()函数为数据包申请空间，注意MAX_TCPHEADER是各层头部长度的总和， $\mathrm{sk - > }$ sk_allocation是分配标志。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ alloc Skyl_b_fclone()

```c
static inline struct sk_buffer *alloc_skb_fclone(unsigned int size,  
gfp_t priority)  
{  
return __alloc_skb(size, priority, 1, -1);  
} 
```

以前多次看到对__alloc Skyl()函数的调用，现在结合图9.2分析这个函数的内容。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ alloc_SKb_fclone() $\rightarrow$ __alloc_SKb()

```c
181 struct sk_buffer * __alloc_skb(unsigned int size, gfp_t gfp_mask,  
182 int fclone, int node)  
183 {  
184 struct kmem_cache * cache;  
185 struct skb_shared_info * shinfo;  
186 struct sk_buffer * skb;  
187 u8 * data;  
188 //确认使用哪种高速缓冲区 
```

![](images/1a0d24a9a65015a42513879e90b1ae4ecae8beb1b9fdd44f0e8d13f1ec9a9cf7.jpg)  
图9.2 sk_buffer结构分配示意图

189 cache $=$ fclone?skbuff_fclone_cache:skbuff_head_cache;   
190   
191 /\*分配数据包的空间(不含数据块）\*/   
192 skb $=$ kmem_cache_alloc_node(cache,gfp_mask& $\sim$ _GFP_DMA,node);   
193 if(!skb)   
194 goto out;   
195   
196 size $=$ SKB_DATAALIGN(size);//数据块长度按高速缓存行对齐   
197 data $=$ kmalloc_node_track_caller(size + sizeof(struct skb_shared_info),   
198 gfp_mask,node);//分配数据块和共享结构空间，返回超始地址   
199 if(!data)   
200 goto nodata;   
201   
202 /\*   
203 \*Only clear those fields we need to clear,not those that we will   
204 \*actually initialise below.Hence,dort put any more fields after   
205 \*the tail pointer in struct sk_buffer!   
206 \*/   
207 memset(skb,0,offsetof(struct sk_buffer,tail));//清零tail到结构头之间的内容   
208 skb-> truesize $=$ size $^+$ sizeof(struct sk_buffer);//实际尺寸包括结构和缓冲块大小   
209 atomic_set(&skb->users,1);//设置使用计数   
210 skb->head $=$ data;//新分配空间的起始地址为缓冲块起始地址   
211 skb->data $=$ data;//数据块起始地址等于缓冲块起始地址   
212 skb_resetTAIL_pointer(skb);//数据块结束地址等于起始地址   
213 skb->end $=$ skb->tail $^+$ size;//缓冲块结束地址等于新分配空间的结束地址，实际指向了 共享数据结构，它紧跟在缓冲块后边

214 /\*make sure we initialize shinfo sequentially \*/   
215 shinfo $=$ skb_shinfo(skb);//根据缓冲块结束地址获取共享数据结构指针   
216 atomic_set(&shinfo-> dataref,1); //设置数据块的使用计数   
217 shinfo->nr_fragments $= 0$ ;//分散数据块的总数   
218 shinfo->gso_size $= 0$ ;//分段数据包的大小   
219 shinfo->gso_segs $= 0$ ;//分段数据包总数   
220 shinfo->gso_type $= 0$ ;//分段数据包的类型   
221 shinfo->ip6frag_id $= 0$ ;//IPv6使用，不关心   
222 shinfo->frag_list $=$ NULL; //分段数据包队列   
223 if (fclone){   
225 struct sk BUFF \*child $=$ skb + 1;//指向克隆数据包结构   
226 atomic_t \*fclone_ref $=$ (atomic_t \*) (child + 1); //取得数据包后的计数器   
227   
228 skb->fclone $=$ SKB_FCLONE_ORIG; //设置克隆标志位   
229 atomic_set(fclone_ref,1); //设置克隆计数器   
230   
231 child->fclone $=$ SKB_FCLONE_UNAVAILABLE; //设置克隆数据包未使用标志   
232 }   
233 out:   
234 return skb; //返回数据包指针   
235 nodata:   
236 kmem_cache_free(cache,skb); //释放数据包结构空间   
237 skb $=$ NULL;   
238 goto out;   
239}

struct sk_buffer 结构是数据包结构体，函数首先根据参数 fclone 来确定使用两个高速缓冲区中的哪一个。克隆的高速缓冲区 skbuffer_fclone_cache 与标准的 skbuff_head_cache 缓冲区是在系统初始化时创建的，内核通过 core_initcall(sock_init) 即 initcall 机制来实现初始化函数 sock_init()，由它调用 skb_init() 创建这两个高速缓冲区。

sock_init $\rightarrow$ skb_init()   
2361void__initskb_init(void)   
2362{   
2363 skbuff_head_cache $=$ kmem_cache_create("skbuff_head_cache"，   
2364 sizeof(struct sk_buffer),   
2365 0,   
2366 SLAB_HWCACHE Align|SLAB_PANIC,

```c
2367 NULL); //创建普通的数据包缓冲区  
2368 skbuff_fclone_cache = kmem_cache_create("skbuff_fclone_cache",  
2369 (2 * sizeof(struct sk_buffer)) +  
2370 sizeof(atomic_t),  
2371 0,  
2372 SLAB_HWCACHE Alignment|SLAB_PANIC,  
2373 NULL); //创建带克隆数据包的缓冲区，一次可以分配两个数据包结构  
2374}
```

代码调用了slab函数kmem_cache_create(),由它完成对高速缓冲区的创建;__alloc_skb()函数192行的kmem_cache_alloc_node()函数也与slab相关，这两个函数的过程读者可以参考推荐书籍来学习。

fclone参数从alloc Skyl_fclone()传递下来的值为1，因而在skbuff_fclone_cache的克隆高速缓冲区中分配两个数据包结构空间。这个缓冲区的对象大小是数据包长度的两倍，含义是包含第二个数据包的空间，本书称之为克隆数据包，克隆数据包将在发送过程中使用。

分配了数据包sk_buffer结构所需空间后，还要为数据块分配内存空间。数据块是用来保存各层头部和传输层数据的一块内存，提供发送数据给网络设备，长度值是tcp_connect()函数指定的。这次建立的是SYN数据包，数据块的长度指定为是各层头部长度的总和，先使长度按高速缓存的边界对齐，然后在高速缓存中分配数据块和共享结构的内存空间。

为数据块申请的内存空间在本书中称为缓冲块。缓冲块分配后就不变了；缓冲块包含着数据块，数据块是可变的。数据包结构sk_buffer使用指针记录着缓冲块和数据块的起始地址及结束地址，我们可以通过数据块指针改变数据块在缓冲块中的位置和大小，因而在缓冲块范围内，数据块可以随时扩充、移动、占用缓存，直至缓存用光。注意，共享结构空间紧跟在缓冲块后面，因此根据缓冲块的结束地址end可以取得该结构的指针。kmalloc_node_track_caller()分配函数在内核中有多个同名函数，编译时将根据是否设定CONFIG_NUMA非均匀介质内存等选项选择其一。

我们现在介绍一下共享结构skb_shared_info的意义，虽然名称上是共享的意思，但是这个结构的作用并不是为了共享，它记载着分散数据块和分段数据包的信息。

分散集中数据块（简称分散数据块）是指以分散集中的DMA方式存放数据块，被称为scatter-gather DMA。我们知道DMA使网络设备可以直接存取内存，普通数据块DMA方式(BlockDMA)要求内存的物理地址必需连续；而分散集中DMA方式正好相反，可以将数据块分散到物理地址不连续的内存中，并使用链表的方法指向存放数据块的内存。如果网络设备支持分散集中DMA方式，就可以大大提高发送效率。

共享结构的frags数组是skb_frag_t结构，它指出分散数据块在内存的具体页面（内核管理以页为单位，每页为4KB大小）以及在页面的偏移位置，但是所在页面的剩余空间可以同时被其他共享结构的分散数据块使用。

# 代码清单9.3 skb_shared_info与skb_frag_struct结构代码

```c
struct skb_shared_info{//数据包共享结构
    atomic_t dataref;//数据块的使用计数
    unsigned short nr_fragments;//分散的数据块数
    unsigned short gso_size;//分段数据包的大小
    /* Warning: this field is not always filled in (UFO)! */
    unsigned short gso_segs;//分段数据包的个数
    unsigned short gso_type;//分段数据包的类型
    __be32 ip6frag_id;//IPv6使用
    struct sk_buffer *frag_list;//分段数据包队列
    skbfrag_t frags[MAX_SKB_FRAGS];//分散数据块队列(数组)
};
# define MAX_SKB_FRAGS(65536/PAGE_SIZE + 2)//分散数据块的最大个数
typedef struct skbfrag_struct skbfrag_t;
struct skbfrag_struct{//分散数据块结构
    struct page *page;//指向保存分散数据块的页面
    __u32 page_offset;//分散数据块在页面的偏移位置(开始位置)
    __u32 size;//分散数据块的长度
};
```

共享结构的定义中既有分散数据块又有分段数据块，它们属于同一个数据包所有，都用于存放传输层数据；它们的差别是：分散数据块具有专门的分散数据块结构，而分段数据块则使用数据包sk_buffer结构。由于这样复杂的划分方式，我们只好将未分段前的数据块称为基本数据块。读者可以先看一下图9.4。TCP的分段数据包并不链入到共享结构frag_list队列中，而是链入到sock结构的发送队列中，对于frag_list队列的使用将在第14章介绍。

从图9.4中可以看出，数据包关联的数据块有3种：基本数据块、分段数据块及分散数据块。提醒读者注意，本书所指的数据包和分段数据包都是指结构体，由它们负责组织数据块的内容。如果未加特殊说明，本书简称的数据块是指数据包的基本数据块，还要注意网络设备发送对象是数据块而不是数据包结构。到达服务器后，网络设备接收数据块后也会建立对应数据块的数据包结构。

回到__alloc_skb()函数，数据包、缓冲块、共享结构分配工作完成后，就要对数据包进行初始化，记录下分配的缓冲块地址，初始化数据块地址（基本数据块的简称），也对共享结构中的分段信息进行设置。213行代码skb->end实际取得了共享结构指针，skb_shinfo()就是根据它取得附加信息结构指针的。

```c
define skb_shinfo(SKB) ((struct skb_shared_info *)(skb_end_pointer(SKB))) static inline unsigned char *skb_end_pointer(const struct sk BUFF *skb) 
```

```txt
{ returnskb->end; 1 
```

224行代码根据参数fclone确定使用克隆数据包结构，增加数据结构后边的计数器表明克隆数据结构已经占用了。

回到 tcp_connect()函数2349行，skb_reserved()函数实际是将数据块向上移动，参数len为移动距离，这里它指定移动距离为头部的总长MAX_TCPHEADER；而数据块的开始地址和结束地址相同，表示还没有数据块。

```c
static inline void skb_reserved(struct sk_buffer *skb, int len)  
{  
    skb->data + = len; //数据块的开始地址增加len值，数据块向上移动  
    skb->tail + = len; //数据块的结束地址增加len值  
}
```

我们可以参照图9.3来理解这个函数的作用。

![](images/1dc53f29e63fdb256ee3b59c52f391830c874f637de81ae9ea4615c4cb104b16.jpg)  
(a) 执行前

![](images/ac0642f49d4b2b140c4fa885c1288962e2224c236663e4e13283215fac34d374.jpg)  
(b)执行后   
图9.3 skb_reserved()函数示意图

记录下发送序号，调用tcp_init_nodata_skb()函数初始化控制结构内容。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_init_nondata_skb()

```c
333 static void tcp_init_nodata_skb(struct sk_buffer *skb, u32 seq, u8 flags)  
334 {  
335     skb-> csum = 0;  
336  
337     TCP_SKB_CB(skb) -> flags = flags; //设置标志  
338     TCP_SKB_CB(skb) -> sacked = 0; //SACK状态标志  
339  
340     skb_shinfo(skb) -> gso_segs = 1; //分段数据包总数 
```

```c
341 skb_shinfo(skb) -> gso_size = 0; //分段数据包长度  
342 skb_shinfo(skb) -> gso_type = 0; //分段数据包类型  
343  
344 TCP_SKB_CB(skb) -> seq = seq; //记录发送序号  
345 if (flags & (TCPCB_FLAG_SYN | TCPCB_FLAG_FIN))  
346 seq++; //发送序号自增  
347 TCP_SKB_CB(skb) -> end_seq = seq; //最后发送序号  
348}
```

在sk_buffer结构中有一个char类型的数组cb[48]，该数组中用来保存控制信息结构tcp skb_cb。

# 代码清单9.4 tcp_skb_cb结构代码

```c
define TCP_SKB_CB(_skb) ((struct tcp_skb_cb*)&((_skb)->cb[0]))  
struct tcp_skb_cb{//TCP数据包控制结构  
union{  
    struct inlet_skb-param h4;  
#if defined(CONFIG_IPV6)||defined(CONFIG_IPV6MODULE)  
    struct inlet6_skb-param h6;  
#endif  
}header; /*For incoming frames */  
_u32 seq; /*Starting sequence number */  
_u32 end_seq; /*SEQ+FIN+SYN+datalen */  
_u32 when; /*used to compute rtts */  
_u8 flags; /*TCP header flags. */ 
```

```c
/\*NOTE: These must match up to the flags byte in a \* real TCP header. /\# define TCPCB_FLAG_FIN 0x01 #define TCPCB_FLAG_SYN 0x02 #define TCPCB_FLAG_RST 0x04 #define TCPCB_FLAG_PSH 0x08 #define TCPCB_FLAG_ACK 0x10 #define TCPCB_FLAG_URG 0x20 #define TCPCB_FLAG_ECE 0x40 #define TCPCB_FLAG_CWR 0x80 _u8 sacked; / \* State flags for SACK/FACK. \*/ #define TCPCB_SACKED_ACKED 0x01 /\* SKB ACKd by a SACK block \*/ #define TCPCB_SACKED_RETRANS 0x02 /\* SKB retransmitted \*/ 
```

```c
define TCPCB_LOST 0x04 /* SKB is lost */
#define TCPCB_TAGBITS 0x07 /* All tag bits */
#define TCPCB EVER_RETRANS 0x80 /* Ever retransmitted frame */
#define TCPCB_RETRANS (TCPCB_SACKED_RETRANS|TCPCBEver_RETRANS)
__u16 arg_ptr; /* Valid w/URG flags is set. */
__u32 ack_seq; /* Sequence number ACKd */ 
```

结构的注释中也说明变量的作用。tcp_init_nodata_skb()函数为控制结构做了必要的设置，也对共享结构的GSO(Generic Segmentation Offload)分段数据包信息进行了设置。

tcp_connect()函数2353行调用TCP_ECN_send_syn()函数设置阻塞报告ECN的标志。

static inline void TCP_ECN_send_syn(struct sock \*sk，struct sk_buffer \*skb)   
{ struct tcp_sock \*tp $=$ tcp_sk(sk); tp->ecn_flags $= 0$ if（sysctl.tcp_ecn）{ TCP_SKB_CB(skb)->flags| $=$ TCPCB_FLAG_ECE|TCPCB_FLAG_CWR; tp->ecn_flags $=$ TCP_ECN_OK; }

这里根据内核的sysctl tcp_ecn判断是否启用ECN来增加数据包控制信息标志TCPPCB_FLAG_ECE和TCPCB_FLAG_CWR，它们用来表明客户端具有ECN功能，这是遵循RFC793标准设置的。

2356行和2357行除了对数据包的时间设置和初始标志位，还调用了__tcp_add_write_queueTAIL()函数将数据包链入到发送队列中。

```c
static inline void __tcp_add_write_queueTAIL(struct sock \*sk,struct sk_buffer \*skb)   
{ __skb_queueTAIL(&sk->>sk_write_queue,skb);}static inline void __skb_queueTAIL(struct sk_bufferhead\*list, struct sk_buffer\*newsk)   
{ __skb_queue_before(list,(struct sk_buffer\*)list,newsk);   
} static inline void __skb_queue_before(struct sk_bufferhead\*list, struct sk_buffer\*next, struct sk_buffer\*newsk) 
```

```c
__skb_insert(newsk, next -> prev, next, list);
}
static inline void __skb_insert(struct sk_buffer *newsk, struct sk_buffer *prev, struct sk_buffer *next, struct sk_buffer_head *list) {
    newsk->next = next;
    newsk->prev = prev;
    next->prev = prev->next = newsk;
    list->strlen++;
} 
```

入队操作就是调整数据包结构的 next 和 prev 指针, 同时调整队列计数器。

sock结构中有一个记录队列总长度的变量sk_wmem_queueed，入队后也要调整它的计数，也要调用sk_mem_charge()函数减少预分配计数器的值。

static inline void sk_mem_charge(struct sock \*sk,int size)   
{ if(!sk_has_account(sk))//如果不支持分配计数 return; sk->sk_forward_alloc $= =$ size;//调整预分配计数器   
} static inline int sk_has_account(struct sock \*sk) { /\*return true if protocol supports memory accounting \*/ return!!sk->skprot->memory_allocated;//是否支持分配统计

sk_has_account()函数是检查是否支持分配计数功能。

2362行调整packets_out计数器，记录已经处于“飞行”状态的数据包。

```c
static inline int tcp_SKb_pcount(const struct sk_buffer *skb)  
{  
return skb_shinfo(skb) -> gso_segs; //分段数据包总数  
}
```

实际上就是在 tcp_init_nondata_skb() 函数中设置 gso_segs, 其值为 1, 代表只有一个数据包, SYN 数据包没有分段。

2363 行调用 tcp_transmit_skb()函数将这个数据包继续向前推进。我们在分析这个函数之前先看完余下的代码，如果正常发送完毕，则需要调整一下发送序号，并调整状态为 TCP_

MIB_ACTIVEOPENS 激活状态，这是 RFC 1213 要求的标志。调用 inlet_csk_reset_xmit_timer() 函数设置一个定时器，如果超时没有得到服务器的应答就会重新发送 SYN 数据包。通过这些过程后读者已经感觉到数据包结构 sk_buffer 的重要性了，回顾一下它的结构定义。

# 代码清单9.5 sk_buffer结构代码

struct sk_buffer{//数据包结构

```c
/\*These two members must be first. \*/ struct sk_buffer \*next;//指向队列中下一个数据包结构 struct sk_buffer \*prev;//指向队列中上一个数据包结构
```

```c
struct sock *sk; //所属的 sock 结构指针  
ktime_t tstamp; //数据包到达时间  
struct net_device *dev; //网络设备结构指针
```

```c
union{//路由内容 struct dst_entry \*dst;//路由项 struct rtable \*rtable;//路由表   
}; struct sec_path \*sp;//用于XFRM安全路径
```

```txt
/\* This is the control buffer. It is free to use for every \* layer. Please put your private variables there. If you \* want to keep them across layers you have to do a skb Clone() \* first. This is owned by whoever has the skb queued ATM. \*/ char cb[48]://数据包的控制信息 
```

//数据块包含各层头部和传输层数据，而分散数据块只包含传输层数据 unsigned int len, //全部数据块的总长度（基础数据块长度、分段数据块总长度、分散数据块总长度三者之和）

```txt
data_len; //分散数据块总长度和分段数据块总长度之和  
_u16 mac_len, //链路层头部的长度  
hdr_len; //克隆数据包可写的头部长度  
union {  
    _wsum csum; //检验和  
    struct {  
        _u16 csum_start; //检验和相对于skb->head的偏移位置
```

```txt
_u16 csum_offset; //检验和相对于csum_start的偏移位置  
};  
};  
_u32 priority; //数据包的优先级  
_u8 local_df:1, //是否允许本地分段  
cloned:1, //是否可以复制头部  
ip_summed:2, //驱动提供的IP检验和  
nohdr:1, //没有头部标识  
nfctinfo:3; //与连接的关系  
_u8 pkt_type:3, //数据包类型  
fclone:2, //数据包克隆状态  
ipvs_property:1, //数据包所属的ipvs  
peeked:1, //数据包选中标志  
nf_trace:1; // netfilter跟踪标志  
_be16 protocol; //驱动提供的数据包协议
```

```c
void (*destructor)(struct sk_buffer *skb); //析构函数，释放数据包
# if defined (CONFIG_NF_CONNTRACK) || defined (CONFIG_NF_CONNTRACKMODULE)
struct nf_contrack *nfct; // netfilter 相关
struct sk_buffer *nfct_reasm; // netfilter 相关
#endif
# ifdef CONFIG_BRIDGE_NETFILTER
struct nf_bridge_info *nf_bridge; // 网桥信息
#endif
```

```c
int iif; //网络设备ID  
# ifdef CONFIG_NETDEVICESMULTIQUEUE  
_u16 queuemapped; //用于多重设备队列  
#endif  
# ifdef CONFIG_NET_SCHED  
_u16 tc_index; /* traffic control index */  
# ifdef CONFIG_NET_CLS_ACT  
_u16 tc verd; /* traffic control verdict */  
#endif  
#endif  
# ifdef CONFIG_IPV6_NDISC_NODETYPE  
_u8 ndisc_nodetype:2; //链路层提供的路由类型
```

```c
endif /\*14bithole\*/   
#ifdef CONFIG_NET_DMA dma_cookie_t dma_cookie;//使用的DMA缓存   
#endif   
#ifdef CONFIG_NETWORK_SECMARK_u32 secmark;//安全掩码   
#endif   
_u32 mark;//数据包掩码   
sk_buffer_t transport_header;//指向数据块中传输层头部sk_buffer_t network_header;//指向数据块中网络层头部sk_buffer_t mac_header;//指向数据块中链路层头部/\*These elements must be at the end,seealloc_skb()for details.\*/ sk_buffer_t tail;//指向基本数据块的结束地址sk_buffer_t end;//指向缓冲块的结束地址，缓冲块是指申请到的内存unsigned char \*head，//指向缓冲块的起始地址\*data;//指向基本数据块的起始地址 unsigned int truesize;//数据包的实际尺寸包括各种数据块的长度 atomic_t users;//用户计数
```

这个结构容易混淆的地方是数据块的总长度，它的计算之所以这么复杂是由数据包结构支持所有的协议造成的。

TCP协议会将一个完整的数据包划分成多个分段数据包，原来的数据包被分成许多部分，它的数据块也被分隔成基本数据块和分段数据块。若同时采用分散聚合方式存放数据块，还会具有分散数据块，由共享结构来说明这些分段数据块和分散数据块的位置和数量，基本原则是：当基本数据块无法满足存放要求时，就会使用分散数据块；如果还是无法满足存放要求就会创建分段数据块来存放。原来的数据包（本书称为主数据包）只剩下基本数据块的位置信息了，head/end指出基本数据块使用的缓冲块，data/tail指出基本数据块的位置，如图9.4所示。

![](images/694306eb4e1c4be79d183991a52d15f07aecbce050bf614306f1fda9ee6ec04e.jpg)  
图9.4 sk_buffer结构的数据块

# 9.3 构建、发送TCP数据包

在9.2节的基础上，我们现在看发送数据包函数tcp_transmit_skb()，由于代码较多因此分段进行。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inlet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_transmit_SKb()

469 static int tcp_transmit_skb(struct sock \*sk,struct sk BUFF \*skb,int clone_it,  
470 gfp_t gfp_mask)  
471{  
472 const struct inlet_connection_sock \*icsk $=$ inlet_csk(sk); //连接结构指针  
473 struct inlet_sock \*inet;  
474 struct tcp_sock \*tp;  
475 struct tcp_SKb_cb \* tcb;  
476 int tcp_header_size;  
477 #ifdef CONFIG.tcp_MD5SIG  
478 struct tcp_md5sig_key \*md5;  
479 __u8 \* md5_hash_location;  
480 #endif  
481 struct tcphdr \*th;  
482 int sysctl_flags;  
483 int err;  
484  
485 BUG_ON(!skb||！tcp_skb_pcount(sk));//调试目的  
486  
487 /\*If congestion control is doing timestamping,we must  
488 \*take such a timestamp before we potentially clone/copy.  
489 \*/  
490 if (icsk->icsk_ca ops->flags&TCPCongRRT_STAMP)  
491 __net_timestamp(skb);//记录当前时间  
492  
493 if (likely(clone_it)){//是否使用克隆数据包  
494 if (unlikely(skb_cloned(skb))//克隆数据包是否可用  
495 skb $=$ pskb_copy(skb,gfp_mask);//创建一个数据包并复制内容  
496 else  
497 skb $=$ skb_clone(skb,gfp_mask);//使用克隆数据包  
498 if (unlikely(!skb))  
499 return -ENOUBFS;

```c
500 }  
501  
502 inlet = inlet_sk(sk); //取得 inlet_sock 指针  
503 tp = tcp_sk(sk); //取得 tcp_sock 指针  
504 tcb = TCP_SKB_CB(skb); //取得 tcp 控制结构指针  
505 tcp_header_size = tp->tcp_header_len; //取得 tcp 包头部长度  
506  
507 #define SYSCTL_FLAG_TSTAMPS 0x1  
508 #define SYSCTL_FLAG_WSCALE 0x2  
509 #define SYSCTL_FLAG_SACK 0x4  
510
```

TCP 控制结构保存着每一个数据包的控制信息和参数，是等待发送的数据包队列传递给接收方的 TCP 控制信息。传递时它会保存到数据包的 cb 数组，cb 数组的长度是 48 字节，控制结构在 32 位系统中是 36 字节，在 64 位系统中是 40 字节。所以这个结构与数据包的 cb 数组密切联系的，如果调整控制结构的内容，则必须注意 cb 数组的长度。

结构体tcphdr是TCP包的头部结构，曾经多次出现在前面的代码中，看一下这个结构体的定义。

# 代码清单9.6 tcphdr结构代码

```c
struct tcphdr { //TCP包头部结构
    __be16 source; //源端口号
    __be16 dest; //目标端口号
    __be32 seq; //序号
    __be32 ack_seq; //ACK序号 # if defined(_LITTLE-ENDIAN_BITFIELD)//小端结尾
    __u16 res1:4, //保留位
    doff:4, //TCP头部长度
        fin:1, //发送方是否完成数据发送
        syn:1, //是否属于SYN的连接
        rst:1, //是否复位连接
        psh:1, //接收方是否提交给应用程序使用
        ack:1, //ACK序号是否有效
        urg:1, //紧急指针是否有效
        ece:1, //是否拥塞，提醒回应
        cwr:1; //是否拥塞窗口减少，由发送方设置#elif defined(_BIG_ENDIAN_BITFIELD)//大端结尾
        __u16 doff:4, //TCP头部长度
        res1:4, //保留位
        cwr:1, //是否拥塞窗口减少，由发送方设置
        ece:1, //是否拥塞，提醒回应
```

```txt
arg:1, //紧急指针是否有效
ack:1, //ACK序号是否有效
psh:1, //接收方是否提交给应用程序使用
rst:1, //是否复位连接
syn:1, //是否属于同步序号的连接
fin:1; //发送方是否完成数据发送
# else
# error "Adjust your <asm/byteorder.h> defines" #endif
__be16 window; //窗口大小
__sum16 check; //检验和
__be16 urg_ptr; //紧急指针
};
```

这个结构体是按照TCP协议的规定定义的，注意我们称其为TCP数据包是因为目前处于TCP传输层，其头部定义参考图9.5来理解。

![](images/8a00515715ff8fc71c4e468837e940e5f199294f79261dd9a8888a3060546791.jpg)  
图9.5 TCP数据包的头部示意图

代码491行__net_timestamp()函数为数据包打上“时间戳”，注释说明这个时间是为了处理拥塞时使用。检查参数clone_it，这个值传递为1，由其确定是否使用前面克隆的数据包。前面alloc_skb_fclone()函数中看到，分配数据包空间是在克隆高速缓冲区完成的，因此同时分配了克隆数据包结构空间。

skb_cloned()函数检查克隆数据包是否已经被使用了，如果已经使用，则调用pskb_copy()函数新建一个数据包结构并复制原有的内容。

```c
static inline int skb_cloned(const struct sk_buffer * skb)  
{  
    return skb->cloned && (atomic_read(&skb_shinfo(skb) -> dataref) & SKB_DATAREF_MASK) != 1; 
```

如果克隆数据包还没有使用，则调用函数skbClone()。前面过程中看到并未使用克隆数据包，因此这里进入skbClone()函数启用克隆数据包结构并复制原有数据包的内容。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_transmit_SKb() $\rightarrow$ skbClone()

```c
528 struct sk_buffer * skbclone(struct sk_buffer * skb, gfp_t gfp_mask)  
529{  
530 struct sk_buffer * n;  
531  
532 n = skb + 1; //取得克隆数据包指针  
533 if (skb->fclone == SKB_FCLONE_ORIG &&  
534 n->fclone == SKB_FCLONE_UNAVAILABLE) { //检查克隆数据包是否可用  
535 atomic_t *fclone_ref = (atomic_t *) (n + 1); //取得克隆计数器  
536 n->fclone = SKB_FCLONE clones; //设置克隆标志  
537 atomic_inc(fclone_ref); //增加克隆计数器  
538 } else { //不可用就分配数据包结构空间  
539 n = kmem_cache_alloc(skbuffer_head_cache, gfp_mask);  
540 if (!n)  
541 return NULL;  
542 n->fclone = SKB_FCLONE_UNAVAILABLE; //设置数据包未启用克隆标志  
543 }  
544  
545 return __skb Clone(n, skb); //复制原有内容  
546}
```

函数要检查数据包的克隆标志是否可用，如果可用就取得克隆数据包结构指针；否则，就要分配一个新的数据包结构。这次分配是在普通的高速缓冲区中进行的，只产生一个数据包结构空间。对于直接取得或者新创建的结构最后都转入__skbClone()函数复制原有数据包的信息。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_transmit_SKb() $\rightarrow$ skb_clone() $\rightarrow$ __skbClone()

```txt
467 static struct sk_buffer * __skbclone(struct sk_buffer * n, struct sk_buffer * skb)  
468 { 
```

```c
469 #define C(x) n-> x = skb-> x  
470  
471 n-> next = n-> prev = NULL; //初始化队列用的指针  
472 n-> sk = NULL; //初始化所属的 sock 指针  
473 __copy_skb_header(n, skb); //复制头部  
474  
475 C(len); //复制数据块的总长度  
476 C(data_len); //复制分散数据块与分段数据块的总长度  
477 C(mac_len); //复制链路层头部的长度  
//确定克隆数据包可写的头部长度  
478 n-> hdr_len = skb-> nohdr ? skb_headroom(skb) : skb-> hdr_len;  
479 n-> cloned = 1; //设置克隆标志  
480 n-> nohdr = 0; //设置没有头部标志  
481 n-> destructor = NULL; //设置析构函数，用于释放数据包  
482 C(iif); //复制网络设备 ID  
483 C(tail); //复制数据块的结束地址  
484 C(end); //复制缓冲块的结束地址  
485 C(head); //复制缓冲块的起始地址  
486 C(data); //复制数据块的起始地址  
487 C(truesize); //复制数据包的实际尺寸  
488 atomic_set(&n-> users, 1); //设置用户数  
489  
490 atomic_inc(&(skb_shinfo(skb) -> dataref)); //设置数据块的使用计数  
491 skb-> cloned = 1; //设置原来数据包的克隆标志  
492  
493 return n; //返回新数据包指针  
494 #undef C  
495}
```

函数复制原来数据包的内容到克隆数据结构中，代码已经加入了完整的注释，读者可以了解复制的具体内容。

回到 tcp_transmit_skb()函数502处，inet、tp、 tcb等结构变量都获取了对应的指针，变量tcp_header_size也取得了TCP包头部的长度，我们继续看代码。

sys_SOCKETcall() $\rightarrow$ sys_connect() $\rightarrow$ inet_STREAM_connect() $\rightarrow$ tcp_v4_connect() $\rightarrow$ tcp_connect() $\rightarrow$ tcp_transmit_SKb()

```c
511 sysctl_flags = 0; //控制标志  
512 if (unlikely(tb->flags & TCPCB_FLAG_SYN)){//如果没有设置SYN标志  
513 tcp_header_size = sizeof(struct tcphdr) + TCPOLEN MSS; //修改tcp头部长度
```

```c
if (sysctl tcp_timestamps) { //如果指定了 tcp 时间戳
    tcp_header_size + = TCPOLEN_TSTAMPAligned; //增加记录时间的长度
    sysctl_flags |= SYSCTL_FLAG_TSTAMPS; //增加时间戳标志
}
if (sysctl tcp_window_scaling) { //如果指定了 tcp 滑动窗口比例
    tcp_header_size + = TCPOLEN_WSCALEAligned; //增加记录比例的长度
    sysctl_flags |= SYSCTL_FLAG_WSCALE; //增加滑动窗口比例标志
}
if (sysctl tcp_sack) { //如果指定了 SACK 标志
    sysctl_flags |= SYSCTL_FLAG_SACK; //增加 SACK 标志
    if (! (sysctl_flags & SYSCTL_FLAG_TSTAMPS))
        tcp_header_size + = TCPOLEN_SACKPERAligned; //增加 SACK 长度
}
} else if (unlikely(tp->rx_opt.eff_sacks)) { //如果没有指定 SACK 的总长度
        /* A SACK is 2 pad bytes, a 2 byte header, plus
            * 232-bit sequence numbers for each SACK block. */
        */
        tcp_header_size + = (TCPOLEN_SACK_BASEAligned +
            (tp->rx_opt.eff_sacks *
                TCPOLEN_SACK_PERBLOCK)); //计算并增加 SACK 的总长度
}
```

ifdef CONFIG_TCP_MD5SIG /\* \*Are we doing MD5 on this segment? If so - make \* room for it. /\* md5 $=$ tp->af_specific-> md5.lookup(sk,sk); if (md5) tcp_header_size $+ =$ TCPOLEN_MD5SIG_ALIGNED; //增加MD5的长度 #endif //调整数据块的起始地址，向下延伸，开辟TCP头部空间 skb.push(sk,tcp_header_size); skb_reset transporting_header(sk);//数据包记录当前的TCP头部指针 skb_set Owner_w(sk,sk);//建立与sock的关联，设置析构函数

$\begin{array}{rl} & {\mathrm{th = tcp\_hdr(skb); / / }\mathrm{指向数据块中的TCP头部}}\\ & {\mathrm{th - > source = inet - > sport; / / }\mathrm{记录源端口}}\\ & {\mathrm{th - > dest = inet - > dport; / / }\mathrm{记录目标端口}}\\ & {\mathrm{th - > seq = htonl(tpb - > seq); / / }\mathrm{记录发送序号}}\\ & {\mathrm{th - > ack\_seq = htonl(tp - > rcv_nxt); / / }\mathrm{记录ACK序号}}\\ & {\mathrm{*((_{be16\%})th) + 6) = htons((_{tcp\_header\_size} > > 2) <   <   12)}|}\\ & {\mathrm{tcb - > flags); / / }\mathrm{记录头部长度和标志}} \end{array}$ if (unlikely(tpb->flags&TCPCB_FLAG_SYN)){//是否设置SYN标志\*/\*RFC1323:The window in SYN&SYN/ACK segments\*is never scaled.\*/th->window $=$ htons(min(tp->rcv_wnd,65535U));//设置窗口大小}else{\th->window $=$ htons(tp_select_window(sk));}th->check $= 0$ //设置检验和th->urg_ptr $= 0$ //设置紧急指针//检查紧急模式以及紧急指针是否在序号范围内if (unlikely(tp->urg_mode&&between(tp->snd_up,tpb->seq+1,tpb->seq+0xFFFF))) {th->urg_ptr $= \mathrm{htons(tp - > snd_up - tcb - > seq); / / }$ 设置紧急指针th->urg $= 1$ //紧急指针有效}if (unlikely(tpb->flags&TCPCB_FLAG_SYN)){//是否设置SYN标志tcp_syn_build-options(_be32\*)(th+1),tcpadvertise_mss(sk),//使用对外公开的MSS(sysctl_flags&SYSCTL_FLAG_TSTAMPS)，(sysctl_flags&SYSCTL_FLAG_SACK)，(sysctl_flags&SYSCTL_FLAG_WSCALE)，tp->rx_opt. rcv_wscale，tcb->when，tp->rx_opt.tsrecent，

```c
endif
NULL); //初始化 SYN 选项
} else { //如果设置了 SYN 标志就更新选项
tcp_build_and_update_options((_,be32*) (th + 1),
tp, tcb -> when,
# ifdef CONFIG_TCP_MD5SIG
md5 ? & md5_hash_location;
#endif
NULL);
TCP_ECN_send(sk, skb, tcp_header_size); //设置 cwr 拥塞减少标志 ece 提醒标志
}
# ifdef CONFIG_TCP_MD5SIG
/* Calculate the MD5 hash, as we have all we need now */
if (md5) {
tp-> af Specific -> calc_md5_hash.md5_hash_location,
mk, NULL, NULL,
tcp hdr(skb),
sk-> sk_protocol,
skb-> len);
}
#endif
icsk-> icsk.af ops -> send_check(sk, skb -> len, skb); //计算并记录检验和
if (likely(tpb-> flags & TCPCB_FLAG_ACK)) //如果设置了 ACK 标志
tcp_event ACK_sent(sk, tcp_skb_pcount(skb)); //调整快速 ACK 数值
if (skb-> len != tcp_header_size) //检查数据块的总长度
tcp_event_data_sent(tp, skb, sk); //复位阻塞窗