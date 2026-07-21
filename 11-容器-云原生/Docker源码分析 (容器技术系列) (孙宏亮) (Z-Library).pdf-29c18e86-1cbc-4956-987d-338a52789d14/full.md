# Docker源码分析

THE SOURCE CODE ANALYSIS OF DOCKER

孙宏亮著

国内首部Docker源码分析著作  
·从源码角度全面解析Docker设计与实现   
·填补Docker理论与实践之间的鸿沟

容器技术系列

# Docker源码分析

孙宏亮 著

ISBN：978-7-111-51072-7

本书纸版由机械⼯业出版社于2015年出版，电⼦版由华章分社（北京华章图⽂信息有限公司，北京奥维博世图书发⾏有限公司）全球范围内制作与发⾏。

版权所有，侵权必究

客服热线：+ 86-10-68995265

客服信箱：service@bbbvip.com

官⽅⽹址：www.hzmedia.com.cn

新浪微博 $@$ 华章数媒

腾讯微博 $@$ yanfabook

微信公众号 华章电⼦书（微信号：hzebook）

# ⽬录

# 赞誉

# 序

# 前⾔

# 第1章 Docker架构

1.1 引⾔  
1.2 Docker总架构图  
1.3 Docker各模块功能与实现分析  
1.3.1 Docker Client   
1.3.2 Docker Daemon   
1.3.3 Docker Registry   
1.3.4 Graph   
1.3.5 Driver   
1.3.6 libcontainer   
1.3.7 Docker Container   
1.4 Docker运⾏案例分析   
1.4.1 docker pull   
1.4.2 docker run   
1.5 总结

# 第2章 Docker Client创建与命令执⾏

2.1 引⾔  
2.2 创建Docker Client  
2.2.1 Docker命令的flag参数解析   
2.2.2 处理flag信息并收集Docker Client的配置信息  
2.2.3 如何创建Docker Client  
2.3 Docker命令执⾏   
2.3.1 Docker Client解析请求命令  
2.3.2 Docker Client执⾏请求命令  
2.4 总结

# 第3章 启动Docker Daemon

3.1 引⾔  
3.2 Docker Daemon的启动流程  
3.3 mainDaemon（）的具体实现  
3.3.1 配置初始化  
3.3.2 flag参数检查  
3.3.3 创建engine对象  
3.3.4 设置engine的信号捕获   
3.3.5 加载builtins  
3.3.6 使⽤goroutine加载daemon对象并运⾏   
3.3.7 打印Docker版本及驱动信息  
3.3.8 serveapi的创建与运⾏  
3.4 总结

# 第4章 Docker Daemon之NewDaemon实现

4.1 引⾔  
4.2 NewDaemon具体实现   
4.3 应⽤配置信息  
4.3.1 配置Docker容器的MTU  
4.3.2 检测⽹桥配置信息  
4.3.3 查验容器间的通信配置   
4.3.4 处理⽹络功能配置  
4.3.5 处理PID⽂件配置   
4.4 检测系统⽀持及⽤户权限  
4.5 配置⼯作路径  
4.6 加载并配置graphdriver  
4.6.1 创建graphdriver  
4.6.2 验证btrfs与SELinux的兼容性  
4.6.3 创建容器仓库⽬录  
4.6.4 迁移容器⾄aufs类型  
4.6.5 创建镜像graph

4.6.6 创建volumesdriver以及volumes graph  
4.6.7 创建TagStore  
4.7 配置Docker Daemon⽹络环境  
4.7.1 创建Docker⽹络设备  
4.7.2 启⽤iptables功能  
4.7.3 启⽤系统数据包转发功能  
4.7.4 创建DOCKER链  
4.7.5 注册处理⽅法⾄Engine   
4.8 创建graphdb并初始化  
4.9 创建execdriver  
4.10 创建daemon实例  
4.11 检测DNS配置   
4.12 启动时加载已有Docker容器  
4.13 设置shutdown的处理⽅法  
4.14 返回daemon对象实例  
4.15 总结

# 第5章 Docker Server的创建

5.1 引⾔  
5.2 Docker Server创建流程  
5.2.1 创建名为“serveapi”的Job  
5.2.2 配置Job环境变量  
5.2.3 运⾏Job   
5.3 ServeApi运⾏流程  
5.4 ListenAndServe实现   
5.4.1 创建router路由实例  
5.4.2 创建listener监听实例  
5.4.3 创建http.Server  
5.4.4 启动API服务  
5.5 总结

# 第6章 Docker Daemon⽹络

6.1 引⾔  
6.2 Docker Daemon⽹络介绍   
6.3 Docker Daemon⽹络配置接⼝   
6.4 Docker Daemon⽹络初始化  
6.4.1 启动Docker Daemon传递flag参数  
6.4.2 解析⽹络flag参数  
6.4.3 预处理flag参数  
6.4.4 确定Docker⽹络模式  
6.5 创建Docker⽹桥  
6.5.1 提取环境变量  
6.5.2 确定Docker⽹桥设备名  
6.5.3 查找bridgeIface⽹桥设备  
6.5.4 bridgeIface已创建  
6.5.5 bridgeIface未创建  
6.5.6 获取⽹桥设备的⽹络地址  
6.5.7 配置Docker Daemon的iptables   
6.5.8 配置⽹络设备间数据报转发功能  
6.5.9 注册⽹络Handler   
6.6 总结

# 第7章 Docker容器⽹络

7.1 引⾔  
7.2 Docker容器⽹络模式  
7.2.1 bridge桥接模式  
7.2.2 host模式  
7.2.3 other container模式  
7.2.4 none模式  
7.3 Docker Client配置容器⽹络模式  
7.3.1 使⽤Docker Client   
7.3.2 runconfig包解析   
7.3.3 CmdRun执⾏

7.4 Docker Daemon创建容器⽹络流程  
7.4.1 创建容器之⽹络配置  
7.4.2 启动容器之⽹络配置   
7.5 execdriver⽹络执⾏流程  
7.5.1 创建libcontainer的Config对象  
7.5.2 调⽤libcontainer的namespaces启动容器  
7.6 libcontainer实现内核态⽹络配置   
7.6.1 创建exec.Cmd   
7.6.2 启动exec.Cmd创建进程  
7.6.3 为容器进程初始化⽹络环境  
7.7 总结

# 第8章 Docker镜像

8.1 引⾔  
8.2 Docker镜像介绍   
8.3 rootfs   
8.4 Union Mount   
8.5 image   
8.6 layer   
8.7 总结

# 第9章 Docker镜像下载

9.1 引⾔  
9.2 Docker镜像下载流程  
9.3 Docker Client   
9.3.1 解析镜像参数  
9.3.2 配置认证信息  
9.3.3 发送API请求  
9.4 Docker Server   
9.4.1 解析请求参数  
9.4.2 创建并配置Job  
9.4.3 触发执⾏Job

9.5 Docker Daemon   
9.5.1 解析Job参数  
9.5.2 创建session对象  
9.5.3 执⾏镜像下载   
9.6 总结

# 第10章 Docker镜像存储

10.1 引⾔  
10.2 镜像注册   
10.3 验证镜像ID   
10.4 创建镜像路径  
10.4.1 创建mnt、diff和layers⼦⽬录  
10.4.2 挂载祖先镜像并返回根⽬录  
10.5 存储镜像内容  
10.5.1 解压镜像内容  
10.5.2 收集镜像⼤⼩并记录  
10.5.3 存储jsonData信息  
10.6 注册镜像ID   
10.7 总结

# 第11章 docker build实现

11.1 引⾔  
11.2 docker build执⾏流程  
11.2.1 Docker Client与docker build   
11.2.2 Docker Server与docker build   
11.2.3 Docker Daemon与docker build   
11.3 Dockerfile命令解析流程  
11.4 Dockerfile命令分析  
11.4.1 FROM命令  
11.4.2 RUN命令  
11.4.3 ENV命令  
11.5 总结

# 第12章 Docker容器创建

12.1 引⾔  
12.2 Docker容器运⾏流程  
12.3 Docker Daemon创建容器对象  
12.3.1 LookupImage   
12.3.2 CheckDepth   
12.3.3 mergeAndVerifyConfig   
12.3.4 newContainer   
12.3.5 createRootfs   
12.3.6 ToDisk   
12.3.7 Register   
12.4 Docker Daemon启动容器  
12.4.1 setupContainerDns   
12.4.2 Mount   
12.4.3 initializeNetworking   
12.4.4 verifyDaemonSetting   
12.4.5 prepareVolumesForContainer   
12.4.6 setupLinkedContainers   
12.4.7 setupWorkingDirectory   
12.4.8 createDaemonEnvironment   
12.4.9 populateCommand   
12.4.10 setupMountsForContainer   
12.4.11 waitForStart   
12.5 总结

# 第13章 dockerinit启动

13.1 引⾔  
13.2 dockerinit介绍   
13.2.1 dockerinit初始化内容  
13.2.2 dockerinit与Docker Daemon   
13.3 dockerinit执⾏⼊⼝

13.3.1 createCommand分析  
13.3.2 namespace.exec   
13.4 dockerinit运⾏   
13.4.1 reexec.Init（）的分析  
13.4.2 dockerinit的执⾏流程  
13.5 libcontainer的运⾏   
13.5.1 Docker Daemon设置cgroups参数  
13.5.2 Docker Daemon创建⽹络栈资源  
13.5.3 dockerinit配置⽹络栈   
13.5.4 dockerinit初始化mount namespace   
13.5.5 dockerinit完成namespace配置  
13.5.6 dockerinit执⾏⽤户命令Entrypoint   
13.6 总结

# 第14章 libcontainer介绍

14.1 引⾔  
14.2 Docker、libcontainer以及LXC的关系  
14.3 libcontainer模块分析  
14.3.1 namespace   
14.3.2 cgroup   
14.3.3 ⽹络   
14.3.4 挂载   
14.3.5 设备  
14.3.6 nsinit   
14.3.7 其他模块  
14.4 总结

# 第15章 Swarm架构设计与实现

15.1 引⾔  
15.2 Swarm架构  
15.2.1 Swarm Node   
15.2.2 Docker Node

15.2.3 node discovery   
15.2.4 scheduler   
15.3 Swarm命令  
15.3.1 swarm create   
15.3.2 swarm manage   
15.3.3 swarm join   
15.3.4 swarm list   
15.4 总结

# 第16章 Machine架构设计与实现

16.1 引⾔  
16.2 Machine架构  
16.2.1 Machine   
16.2.2 Store   
16.2.3 Host   
16.2.4 Driver   
16.2.5 Provisioner   
16.2.6 Machine运⾏流程  
16.3 Machine与Swarm的结合  
16.4 总结

# 第17章 Compose架构设计与实现

17.1 引⾔  
17.2 Compose介绍   
17.3 Compose架构   
17.4 Compose评价   
17.4.1 Compose单机能⼒  
17.4.2 Compose跨节点能⼒   
17.4.3 Compose与Swarm   
17.5 总结

# 赞誉

像⾕歌⼀样部署⾃⼰的应⽤，这是很多软件⼯程师的梦想。Docker的⽬标是圆很多⼈的梦。⾃从InfoQ推出Docker系列⽂章，作为操作系统课程教师的我⼀直在学习并关注Docker的茁壮成⻓。

当我发现这上⾯刊登的“Docker源码分析”系列⽂章的作者居然是我们课程组的研究⽣助教孙宏亮时，惊喜之情溢于⾔表。宏亮对Docker的理解⼗分深刻，他本⼈是Docker的积极拥护者、倡导者和贡献者。他在研究⽣毕业以后投⾝到了创业公司DaoCloud，去为Docker的梦想开创美好的未来。

最近，我⼜欣喜地发现，这系列⽂章以及后续章节即将正式出版成书，有机会同更多的Docker⽤户、开发者、学习者⻅⾯。本书通过分析解读Docker源码，让读者了解Docker的内部结构和实现，以便更好地使⽤Docker。该书的内容组织深⼊浅出，表述准确到位，有⼤量流程图和代码⽚段帮助读者理解Docker各个功能模块的流程，是学习Docker开源系统的良师益友。

寿黎旦，浙江⼤学计算机学院教授

近年来，Docker迅速⻛靡了云计算世界，但是专门针对Docker的技术实现进⾏深⼊分析的⽂章却相对较少。这⼀⽅⾯由于Docker技术变化很快，源码分析很快会跟不上版本发展；另⼀⽅⾯，对源代码的解析，需要对整个Docker设计具备全局的视⾓，才能深⼊浅出地找到源码中的脉络。

宏亮的这本《Docker源码分析》，恰如其时的出现，弥补了这个空⽩，对于希望参与到Docker社区、参与代码贡献或构建⾃⼰的Docker应⽤环境的读者来说，应是⼀本案头必备书籍。

在崇尚源码⾄上的⼯程师⽂化⾥，⽂档介绍、发布会材料都是苍⽩的，唯有研读源码，才能深刻理解软件背后的原理。与所有其他软件⼀样，读源码并不是学习Docker最快的途径，但是如果有⼈通读源码后给出了详细分析，你就可以轻松地站在巨⼈的肩膀上。

很⾼兴看到国内这么快就出版源码分析的书籍。对于所有想在Docker⽅⾯进阶和想晋升为⾼端⽤户的读者，都值得阅读本书，也希望通过《Docker源码分析》⼀书，可以诞⽣更多的社区贡献者，共同推动Docker的发展。

⻩强，华为Docker Committer

值得⾃豪的是，就在Docker蓬勃发展之际，第⼀本详尽剖析Docker源码的著作出⾃国⼈之⼿。

本书在每章宏观的流程梳理背后都伴随有更加细致深⼊的源码分析。⽆论读者是只想了解使⽤Docker，还是抱着深⼊理解Docker并参与社区开发、⼆次开发的⼼态，本书都值得⼀读。

胡科平，华为Docker Committer

这本书从源码的⾓度对Docker的实现原理进⾏了深⼊的探讨和细腻的讲解，将当前热门的容器技术的背后机理讲解得深⼊浅出明⽩透彻。⽆论是Docker的⽤户还是开发者，通过阅读本书都可以对Docker有更深刻的理解，能够更好地使⽤或者开发Docker。

雷继棠，华为Docker Committer

Docker已经是⼀个成⻓2年时间的云计算技术，它正在以惊⼈的速度在全球范围内扩张⾃⼰的“疆⼟”。我作为Docker中国区的开发者，⾮

常希望能看到有⼀本书详细地告诉我，Docker的每⼀个细节是如何实现的。所以当我在InfoQ上看到宏亮的“Docker源码分析”专栏时眼前⼀亮。今天，它终于汇编成书摆在你我的眼前。我希望你能在这本书上学到更多Docker技术的精髓思想，在实战Docker技术时可以运⽤⾃如！

肖德时，数⼈科技CTO

我家⾥的书柜中⾄今仍然保留着⼀本《Linux内核完全注释》，它伴随我很⻓⼀段时间，使我受益匪浅。10年之后，当我拿到宏亮的《Docker源代码分析》草稿，昨⽇⼜重现。这本书⽆论是对学习Docker，还是掌握Go语⾔，都是⾮常好的⼀⼿资源。雷锋不常有，⼤家要⽀持！

赵鹏，VisualOps创始⼈

# 序

三年前，我在VMware负责Cloud Foundry这⼀款开源PaaS产品在中国的开发者社区和⽣态系统建设⼯作。当时关于Cloud Foundry的中⽂⽂档⾮常少，更不⽤说有深度的技术⼲货了，所以当CSDN上出现了⼀个专门从底层模块和源码的⾓度，对Cloud Foundry做深度剖析的系列博客⽂章时，⼀下⼦就引起了我的注意。

经过⼀番“⼈⾁搜索”，我⾮常惊奇的发现，这⼀系列⼲货的作者孙宏亮，竟然是浙江⼤学计算系的⼀年级研究⽣。当时，VMware跟浙江⼤学在Cloud Foundry⽅⾯有⽐较深⼊的合作，浙⼤计算机系的SEL实验室，投⼊了精锐的师资⼒量，从事分布式系统和新⼀代PaaS的研究⼯作。宏亮初⼊浙江⼤学，就在这样的氛围下开始了他的研究⽣求学⽣涯，应当说是⾮常幸运的。

宏亮在CSDN上的系列⽂章，主打“源码分析”，这正是当时开源社区内⽐较缺少的内容。提笔写“源码分析”，需要⼀定的勇⽓，阅读源码需要耗费⼤量的精⼒，需要从数⼗万⾏代码中整理出清晰的逻辑，从中抽丝拨茧、概括精华，这是⼀份⾮常⾟苦的⼯作。⽽且，分析源码也需要⼀些“挑战权威”的精神，不仅仅是简单的代码解析，更需要提炼出⾃⼰的观点，甚⾄敢于发现和纠正⼀些已有的问题。

在源码分析⽅⾯，宏亮充分体现了“初⽣⽜犊不怕虎”的精神，⾮常详细地剖析了当时Cloud Foundry的⼏个主要模块，条理清晰，技术分析准确到位。宏亮这⼀系列⽂章帮助了包括⼀线互联⽹公司在内的许多企业了解、认识和最终使⽤Cloud Foundry，宏亮也借此奠定了他在PaaS社区的“江湖地位”。

从去年开始，Docker的热潮开始波及中国开发者社区。我有幸跟宏亮⼀起在CSDN主办的第⼀届Container技术⼤会上发表联合主题演

讲，向来宾介绍Cloud Foundry内部的容器技术实现，以及对Docker的⼀些展望。那次⼤会是⼀个很重要的⾥程碑，在那之后，宏亮开始深⼊研究Docker的底层实现，并在InfoQ连载“Docker源码分析”系列⽂章。

对PaaS平台的研究越深⼊，越能够发现和体会Docker对开发和运维的价值。如果说当初的Cloud Foundry模块和源码分析⽂章，是读研期间的学习笔记，那这次宏亮的《Docker源码分析》，则是⼀个经过了深思熟虑、系统性、结构化的⼯作。Docker开源项⽬发展速度⾮常快，这次在⽂章连载内容的基础上出书，为了保证内容的准确性和时效性，宏亮补充了⼤量Docker最新项⽬的内容，特别是对Swarm、Machine和Compose这三个模块的开发进展做了紧密的追踪。

这是⼀本从架构和代码⾓度讲解Docker底层实现的技术图书，我从连载第⼀篇开始就对这个系列的⽂章保持了紧密的关注，也⽬睹了宏亮在后期整理加⼯成书过程中的⾟勤努⼒。在《Docker源码分析》成书付梓出版之际，⾮常幸运，能够为宏亮写着⼀篇推荐序。这本书⾮常适合以下三类读者学习和阅读。

·希望以Docker容器交付软件的程序员。

Docker是未来互联⽹软件的交付件，这件事随着OCP标准的制定，正在逐渐成为事实。程序员和运维⼯程师都需要了解Docker的⼯作⽅式，尤其是Docker镜像的结构，软件通过Dockerfile打包时的优化⽅式等，这些内容在本书中都有⾮常详细的阐述。

·Docker化云计算平台的建设者和维护者。

Docker 公 司 在 今 年 的 全 球 开 发 者 ⼤ 会 上 提 出 了 “ProductionReady”的⼝号，有越来越多的互联⽹公司和传统企业采⽤Docker来构建开发、测试和运维平台。Docker在⽹络、存储、安全等领域的细

节，是平台建设者和维护者必须深⼊了解的部分，这些领域还在不断变化，新的项⽬也层出不穷，但本书对⽹络、存储和安全的基本知识和概念，做了⾮常清晰的介绍，也深⼊到了底层实现的代码。

·Go语⾔程序员。

即使不在项⽬中使⽤Docker，本书也能够为Go语⾔程序员带来帮助。Docker项⽬中⼤量采⽤了Go语⾔，尤其是在处理并发场景时，Docker对Go语⾔的运⽤可谓出神⼊化。本书可以帮助Go语⾔程序员亲⾝体验特⼤型项⽬中Go语⾔的威⼒，以及实战场景中Golang模式和功能的⽤法。

最后，预祝宏亮在Docker的学习和⼯作中再创佳绩，也希望读者可以从本书收获知识，开阔眼界。

喻勇

2015年7⽉13⽇

# 前⾔

# Docker是什么

Docker从2013年诞⽣，短短两年时间就在全球IT技术圈内迅速⾛红，实乃技术圈内不可忽视的⼀阵飓⻛。然⽽，Docker是什么，Docker带来了什么？

Docker官⽅如此描述Docker：“Build，Ship，Run.An open platformfor distributed applications for developers and sysadmins” 。 换 ⾔ 之 ，Docker为开发者与系统管理者提供了分布式应⽤的开放平台，从⽽可以便捷地构建、迁移与运⾏分布式应⽤。

多年来，IT⾏业中开发与运维⼀直是两个界限清晰的词。开发⼯程师专门从事软件的开发⼯作，最终交付软件代码；运维⼯程师则部署前者交付的软件，并接管软件的运⾏与管理。然⽽，在⻓时间的实践过程中，开发与运维分离的⽅式难免存在弊病，两者职责的过分清晰导致软件效率的降低。随着分布式系统的流⾏，系统规模越来越⼤，软件越来越复杂，系统环境配置暴露的问题层出不穷。究其缘由，还是因为开发⼈员缺少软件运⾏环境的认知，⽽运维⼈员对软件逻辑所知甚少。在这样的背景下，DevOps横空出世，提倡开发与运维不可分割，协调并进。

Docker⽆疑是DevOps⼤潮中最具实践价值的不⼆法宝。Docker从Linux内核的⾓度出发，属于轻量级虚拟化技术，有能⼒秒级提供应⽤隔离环境，完成云计算时代分布式应⽤的第⼀需求“隔离”。另外，Docker的镜像技术利⽤联合⽂件系统的优势，⾃下⾄上打包系统软件、系统环境以及软件程序，将运⾏环境与应⽤程序灵活地结合，快速运⾏Docker化的应⽤程序。同时，可读性极强的Dockerfile，极⼤地简化镜像的复杂性，并为镜像的转移与重新构建提供了可能性。

Docker提供轻便的资源分配⽅式，解决应⽤运⾏与系统环境的依赖，弥合应⽤跨节点迁移的鸿沟，种种特性都表明Docker⼏乎就是为“云计算”⽽⽣的。如今，Docker社区不断扩⼤并健康发展，多家国际IT巨头也纷纷宣布⽀持Docker，这⼀切更是让全球IT⼈⼠对Docker的未来充满信⼼。

# 本书的内容

本书是⼀本引导读者了解Docker实现原理的技术普及书。笔者⼀直坚信，了解软件或者系统最直接、最透彻的⽅式就是研读它们的源码。“源码即⽂档”也是⿎励开发者能更多地从源码的⾓度去学习软件或系统的本质。

本书的内容主要集中于3个部分：Docker的架构，Docker的模块，Docker的三驾⻢⻋Swarm、Machine以及Compose。

第⼀部分，主要从宏观的⾓度和读者⼀起领略Docker的架构设计，并初步介绍架构中各模块的职责。

第⼆部分是本书的主体部分，主要针对Docker中多个重要的模块进⾏具体深⼊分析，包括：Docker Client、Docker Daemon、DockerServer、Docker⽹络、Docker镜像、Docker容器等。读者可以发现，Docker的模块之间耦合度⾮常低，很适合循序渐进，层层深⼊。第2章⾄第8章主要从Docker软件的架构⼊⼿，勾勒⾻架；第9章⾄第11章重点讨论Docker镜像技术，夯实基础；第12章⾄第14章则进⼀步分析Docker容器的始末，阐述本质。

第三部分介绍Docker⽣态三驾⻢⻋Swarm、Machine、Compose。Docker拥有强⼤的单机能⼒，三驾⻢⻋可以很好地补充Docker的跨主机能⼒以及部署能⼒。读者可以通过第15章⾄第17章感受Docker⽣态圈中其他功能强⼤的软件。

希望本书能够让读者最⼤化地感受Docker的神奇与魅⼒。

# 关于勘误

由于时间与⽔平都⽐较有限，因此本书难免会存在⼀些纰漏和错误。如果读者发现了问题，请及时与我联系。我也会在本书后续的版本中加以改正。我的邮箱是：allen.sun@daocloud.io。我⾮常希望和⼤家⼀起学习与讨论Docker，并共同推动Docker在社区的发展。

# 致谢

最后，向本书编写过程中给予我巨⼤帮助的⼈们表⽰最诚挚的感谢。感谢我的⽗⺟，没有他们的⿎励和⽀持，此书不可能在如此短的时间内完成。感谢我的⺟校浙江⼤学以及SEL实验室的⽼师与同学们，是他们在我求学过程中给予⽆私的指引与帮助。感谢我的同事熊中祥，是他在本书编写过程中提出了很多宝贵的建议，尤其在Machine和Compose部分。感谢我的同事喻勇和冯钊，他们为本书的编写做了很多沟通与协调⼯作。最后，还要感谢华章公司的编辑们，她们认真细致的⼯作，使本书以完美的形式展现给各位读者。

孙宏亮

2015年6⽉

# 第1章 Docker架构

# 1.1 引⾔

Docker是Linux平台上的⼀款轻量级虚拟化容器的管理引擎。在全球范围内，Docker还是⼀个开源项⽬，整个项⽬基于Go语⾔开发，代码托管于GitHub⽹站上，并遵从Apache 2.0协议。⽬前，Docker可以帮助⽤户在容器内部快速⾃动化部署应⽤，并利⽤Linux内核特性命名空间（namespaces）及控制组（cgroups）等为容器提供隔离的运⾏环境。Docker借助操作系统层的虚拟化实现资源的隔离，因此Docker容器在运⾏时与虚拟机（VM）的运⾏有很⼤的区别，Docker容器与宿主机共享同⼀个操作系统，不会有额外的操作系统开销。这样的优势很明显，因⽽⼤⼤提⾼了资源利⽤率，并且提升了I/O等⽅⾯的性能。

众多新颖的特性以及项⽬本⾝的开放性，导致Docker在不到两年的时间⾥迅速获得了诸多⼚商的⻘睐，其中更是包括Google、Microsoft 、 VMware 等 ⾏ 业 领 航 者 。 Google 在 2014 年 6 ⽉ 推 出 了Kubernetes，宣布⽀持Docker容器的调度管理，⽽2014年8⽉Microsoft则宣布Azure上⽀持Kubernetes，随后传统虚拟化巨头VMware宣布与Docker强强合作。2014年9⽉中旬，Docker更是获得4000万美元的C轮融资，以推动分布式应⽤的发展。

⽬前，Docker的前景被普遍看好。未来的云计算领域乃⾄整个IT领域，Docker都必将扮演不可或缺的⾓⾊。为了帮助⼤家更好地认识Docker、理解Docker并掌握Docker，本书从Docker源码的⾓度出发，详细介绍Docker的架构、Docker的运⾏以及Docker的特性。本章主要介绍Docker架构。

本书关于Docker的分析均基于Docker 1.2.0版本的源码。

本章⽬的是在理解Docker源码的基础上分析Docker架构，分析过程主要按照以下三个部分进⾏：

·Docker的总架构图。  
·Docker架构内部各模块功能与实现的分析。  
·通过具体的Docker命令，阐述Docker的运⾏流程。

# 1.2 Docker总架构图

作为Linux平台上的⼀种容器的管理引擎，Docker并不像其他⼤型分布式系统那样复杂。Docker的源码总量并不多，⽽且清晰的源码结构使得Docker的学习成本并不⾼。换⾔之，Docker源码的学习过程并不枯燥，我们可以从中学到很多东⻄，如Go语⾔的运⽤、Docker架构的设计原理等。Docker对⽤户⽽⾔是⼀个简单的C/S架构，⽤户通过客户端与服务器端建⽴通信，⽽Docker的后端是⼀个松耦合的架构，架构中的模块各司其职、有机组合，⽀撑着Docker运⾏。

Docker 的 总 架 构 如 图 1-1 所 ⽰ 。 架 构 中 主 要 的 模 块 有 ：DockerClient 、 DockerDaemon 、 Docker Registry 、 Graph 、 Driver 、libcontainer以及Docker Container。

对⽤户⽽⾔，Docker Client是与Docker Daemon建⽴通信的最佳途径。⽤户通过Docker Client发起容器的管理请求，请求最终发往DockerDaemon。

Docker Daemon作为Docker架构中的主体部分，⾸先具备服务端的功能，有能⼒接收Docker Client发起的请求；其次具备Docker Client请求的处理能⼒。Docker Daemon内部所有的任务均由Engine来完成，且每⼀项⼯作都以⼀个Job的形式存在。

Docker Daemon需要完成的任务很多，因此Job的种类也很多。若⽤户需要下载容器镜像，Docker Daemon则会创建⼀个名为“pull”的Job，运⾏时从Docker Registry中下载镜像，并通过镜像管理驱动graphdriver将下载的镜像存储在graph中；若⽤户需要为Docker容器创建⽹络环境，Docker Daemon则会创建⼀个名“allocate_interface”的Job，通过⽹络驱动networkdriver分配⽹络接⼝的资源……

libcontainer是⼀套独⽴的容器管理解决⽅案，这套解决⽅案涉及了⼤量Linux内核⽅⾯的特性，如：namespaces、cgroups以及capabilities等。libcontainer很好地抽象了Linux的内核特性，并提供完整、明确的接⼝给Docker Daemon。

当⽤户执⾏运⾏容器这个命令之后，⼀个Docker容器就处于运⾏状态，该容器拥有隔离的运⾏环境、独⽴的⽹络栈资源以及受限的资源等。

![](images/84472998fcf2ef1e5b29ba6803656031d798ace93f55ac4810f1187cbd1f3925.jpg)  
图1-1 Docker总架构图

# 1.3 Docker各模块功能与实现分析

下⾯我们将从Docker的总架构图⼊⼿，抽离出架构内的各个模块，并对各个模块进⾏更为细化的架构分析与功能阐述。

# 1.3.1 Docker Client

Docker Client是Docker架构中⽤户与Docker Daemon建⽴通信的客户端。在⼀台安装有Docker的机器上，⽤户可以使⽤可执⾏⽂件docker作为Docker Client，发起众多Docker容器的管理请求。

Docker Client可以通过以下三种⽅式和Docker Daemon建⽴通信，分别为：tcp：//host：port、unix：//path_to_socket和fd：//socketfd。为简单起⻅，本书主要使⽤第⼀种⽅式作为讲述两者通信的原型。通信⽅式确定后，DockerClient与Docker Daemon建⽴连接并传输请求时，可以通过命令⾏flag参数的形式，设置安全传输层协议（TLS）的有关参数，保证传输的安全性。

Docker Client发送容器管理请求后，请求由Docker Daemon接收并处理，当Docker Client接收到返回的请求响应并做简单处理后，DockerClient⼀次完整的⽣命周期就此结束。若需要继续发送容器管理请求，⽤户必须再次通过可执⾏⽂件docker创建Docker Client，并⾛完以上相同的流程。

# 1.3.2 Docker Daemon

Docker Daemon是Docker架构中⼀个常驻在后台的系统进程。所谓的“运⾏Docker”，即代表运⾏Docker Daemon。总之，DockerDaemon的作⽤主要有以下两⽅⾯：

·接收并处理Docker Client发送的请求。

·管理所有的Docker容器。

Docker Daemon运⾏时，会在后台启动⼀个Server，Server负责接收Docker Client发送的请求；接收请求后，Server通过路由与分发调度，找到相应的Handler来处理请求。

启动Docker Daemon所使⽤的可执⾏⽂件同样是docker，与DockerClient启动所使⽤的可执⾏⽂件docker相同。既然Docker Client与DockerDaemon都可以通过docker⼆进制⽂件创建，那么如何辨别两者就变得⾮常重要。实际上，执⾏docker命令时，通过传⼊的参数可以辨别Docker Daemon与Docker Client，如docker–d代表Docker Daemon的启动，dockerps则代表创建Docker Client，并发送ps请求。

Docker Daemon 的 架 构 ⼤ 致 可 以 分 为 三 部 分 ： Docker Server 、Engine和Job。Daemon的架构如图1-2所⽰。

# 1.Docker Server

Docker Server在Docker架构中专门服务于Docker Client，它的功能是接收并调度分发Docker Client发送的请求。Docker Server的架构如图1-3所⽰。

![](images/dd5509678b52a9e85d1c52c3ca410606d99b52d262b7b67b9a4d606b028fc0a3.jpg)  
图1-2 Docker Daemon架构⽰意图

![](images/7a63bd9437a845dc38975520daacc19f428ae064e271f639270350a4238f7927.jpg)  
图1-3 Docker Server架构⽰意图

在 Docker Daemon 的 启 动 过 程 中 ， DockerServer 第 ⼀ 个 完 成 。Docker Server通过包gorilla/mux，创建了⼀个mux.Router路由器，提供请求的路由功能。在Go语⾔中，gorilla/mux是⼀个强⼤的URL路由器以及调度分发器。创建路由器之后，Docker Server为mux.Router中添加有效的路由项，每⼀个路由项由HTTP请求⽅法（PUT、POST、GET或DELETE）、URL和Handler三部分组成。

由 于 Docker Client 通 过 HTTP 协 议 访 问 Docker Daemon ， 故DockerServer 创 建 完 mux.Router 之 后 ， 将 Server 的 监 听 地 址 以 及mux.Router 作 为 参 数 ， 创 建 ⼀ 个 httpSrv=http.Server{} ， 最 终 执 ⾏httpSrv.Serve（）开始服务于外部请求。

在服务过程中，Docker Server在listener上接收Docker Client的访问请求。对于每⼀个Docker Client请求，DockerServer均会创建⼀个全新

的goroutine来服务。在goroutine中，Docker Server⾸先读取请求内容，然后做请求解析⼯作，接着匹配相应的路由项，随后调⽤相应的Handler来处理，最后Handler处理完请求之后给Docker Client回复响应。

需要注意的是：Docker Server在Docker的启动过程中运⾏，通过⼀个名为“serveapi”的Job来实现。理论上，Docker Server的运⾏只是众多Job中的⼀个，但是为了强调Docker Server的重要性以及它为后续Job服务的重要特性，本书将“serveapi”的Job单独抽离出来分析，理解为Docker Server。

# 2.Engine

Engine是Docker架构中的运⾏引擎，同时也是Docker运⾏的核⼼模块。Engine存储着⼤量的容器信息，同时管理着Docker⼤部分Job的执⾏。换⾔之，Docker中⼤部分任务的执⾏都需要Engine协助，并通过Engine匹配相应的Job完成Job的执⾏。

在Docker源码中，有关Engine的数据结构定义中含有⼀个名为handlers的对象。该handlers对象存储的是关于众多特定Job各⾃的处理⽅ 式 handler 。 举 例 说 明 ， Engine 的 handlers 对 象 中 有 ⼀ 项 为 ：{"create"：daemon.ContainerCreate，}，则说明当执⾏名为“create”的Job时，执⾏的是daemon.ContainerCreate这个handler。

除了容器管理之外，Engine还接管Docker Daemon的某些特定任务。当Docker Daemon遭遇到⾃⾝进程需要退出的情况时，Engine还负责完成DockerDaemon退出前的所有善后⼯作。

# 3.Job

Job可以认为是Docker架构中Engine内部最基本的⼯作执⾏单元。DockerDaemon可以完成的每⼀项⼯作都会呈现为⼀个Job。例如，在

Docker容器内部运⾏⼀个进程，这是⼀个Job；创建⼀个新的容器，这是⼀个Job；在⽹络上下载⼀个⽂档，这是⼀个Job；包括之前在DockerServer部分谈及的，创建Server服务于HTTP协议的API，这也是⼀个Job，等等。

有关Job接⼝的设计，与UNIX进程⾮常相仿。⽐如说，Job有⼀个名称，有运⾏时参数，有环境变量，有标准输⼊与标准输出，有标准错误，还有返回状态等。

对于Job⽽⾔，定义完毕之后，运⾏才能完成Job⾃⾝真正的使命。Job的运⾏函数Run（）则⽤以执⾏Job本⾝。

# 1.3.3 Docker Registry

Docker Registry是⼀个存储容器镜像（Docker Image）的仓库。容器镜像（Docker Image）是容器创建时⽤来初始化容器rootfs的⽂件系统内容。Docker Registry将⼤量的容器镜像汇集在⼀起，并为分散的Docker Daemon提供镜像服务。

Docker的运⾏过程中，有三种情况可能与Docker Registry通信，分别为搜索镜像、下载镜像、上传镜像。这三种情况所对应的Job名称分别为search、pull和push。

不同场景下，Docker Daemon可以使⽤不同的Docker Registry。公有Registry与私有Regsitry就是两种场景模式不同的Docker Registry。其中，⼤家熟知的Docker Hub，就是全球范围内最⼤的公有Registry。Docker可以通过互联⽹访问Docker Hub，并下载容器镜像；同时Docker也允许⽤户构建本地私有Registry，使容器镜像的获取在内⽹完成。

# 1.3.4 Graph

Graph在Docker架构中扮演的⾓⾊是容器镜像的保管者。不论是Docker下载的镜像，还是Docker构建的镜像，均由Graph统⼀化管理。由于Docker⽀持多种不同的镜像存储⽅式，如aufs、devicemapper、Btrfs等，故Graph对镜像的存储也会因以上种类⽽存在⼀些差异。对Docker⽽⾔，同⼀种类型的镜像被称为⼀个repository，如名称为ubuntu的镜像都同属⼀个repository；⽽同⼀个repository下的镜像则会因tag存在差异⽽不同，如ubuntu这个repository下有tag为12.04的镜像，也有tag为14.04的镜像。Docker中Graph的架构如图1-4所⽰。

![](images/f176b606d945fa2037a5f6e204c8ec017d55b57568327071ce91a451e28229d6.jpg)  
图1-4 Graph架构⽰意图

本书对Graph以及容器镜像（Docker Image）的分析将以aufs为主，并在第8章进⾏深⼊分析。

# 1.3.5 Driver

Driver是Docker架构中的驱动模块。通过Driver驱动，Docker可以实现对Docker容器运⾏环境的定制，定制的维度主要有⽹络环境、存储⽅式以及容器执⾏⽅式。需要注意的是，Docker运⾏的⽣命周期中，并⾮⽤户所有的操作都是针对Docker容器的管理，同时包括⽤户对Docker运⾏信息的获取，还包括Docker对Graph的存储与记录等。因此，为了将仅与Docker容器有关的管理从Docker Daemon的所有逻辑中区分开来，Docker的创造者设计了Driver层来抽象不同类别各⾃的功能范畴。

Docker Driver 的 实 现 可 以 分 为 以 下 三 类 驱 动 ： graphdriver 、networkdriver和execdriver。

graphdriver主要⽤于完成容器镜像的管理，包括从远程DockerRegistry上下载镜像并进⾏存储，也包括本地构建完镜像后的存储。当⽤户下载指定的容器镜像时，graphdriver将容器镜像分层存储在本地的指定⽬录下；同时当⽤户需要使⽤指定的容器镜像来创建容器时，graphdriver从本地镜像存储⽬录中获取指定的容器镜像，并按特定规则为容器准备rootfs；另外，当⽤户需要通过指定Dockerfile构建全新镜像时，graphdriver会负责新镜像的存储管理。

在graphdriver的初始化过程之前，有4种⽂件系统或类⽂件系统的驱动Driver在DockerDaemon内部注册，它们分别是aufs、btrfs、vfs和devmapper。其中，aufs、btrfs以及devmapper⽤于容器镜像的管理，vfs⽤于容器volume的管理。Docker在初始化之时，优先通过获取系统环境变量“DOCKER_DRIVER”来提取所使⽤driver的指定类型。因此，之后所有的Graph操作，都使⽤该driver来执⾏。Docker镜像是Docker技术中⾮常关键的。2014年12⽉，在Linux 3.18-rc2版本中OverlayFS被合并⾄Linux内核主线，在Docker 1.4.0版本发布时，Docker官⽅宣布⽀持

overlay这⼀类graphdriver，即⽤户在启动Docker Daemon时可以选择制定graphdriver的类型为overlay。graphdriver的架构如图1-5所⽰。

![](images/f4e6b9d81485f581ad0cf4a95e5ecf4939e1220800fc5a350b7daa69b79ad142.jpg)  
图1-5 graphdriver架构⽰意图

networkdriver的作⽤是完成Docker容器⽹络环境的配置，其中包括Docker Daemon启动时为Docker环境创建⽹桥；Docker容器创建前为其分配相应的⽹络接⼝资源；以及为Docker容器分配IP、端⼝并与宿主机做NAT端⼝映射，设置容器防⽕墙策略等。networkdriver的架构如图1-6所⽰。

![](images/96edcc9bd30c23482b0b5feba9772faa5100a1f812ec976adbf9d8598794baa1.jpg)  
图1-6 networkdriver架构⽰意图

execdriver作为Docker容器的执⾏驱动，负责创建容器运⾏时的命名空间，负责容器资源使⽤的统计与限制，负责容器内部进程的真正运⾏等。在Docker 0.9.0版本之前，execdriver只能通过LXC驱动来实现容器的启动管理。实际上，当时Docker通过LXC驱动调⽤Linux下的LXC⼯具管理容器的创建，并控制管理容器的⽣命周期。从Docker0.9.0开始，在继续⽀持LXC的情况下，Docker的execdriver默认使⽤native驱动，native驱动完全独⽴于LXC，属于Docker项⽬下第⼀个全新的⼦项⽬，⽤于容器的创建与管理。Docker默认使⽤native驱动的具体体现为：Docker Daemon启动过程中加载的ExecDriverflag参数在配置⽂件中已经被设为native。native这个execdriver的存在，使得Docker对Linux容器的创建与管理有了⾃⼰的解决⽅案。execdriver架构如图1-7所⽰。

![](images/48f83e5d42d001cf29c8fea8d0316d56a1a1534228aa957276dad89dbbab302f.jpg)  
图1-7 execdriver架构⽰意图

# 1.3.6 libcontainer

libcontainer是Docker架构中⼀个使⽤Go语⾔设计实现的库，设计初衷是希望该库可以不依靠任何依赖，直接访问内核中与容器相关的系统调⽤。

正是由于libcontainer的存在，Docker可以直接调⽤libcontainer，⽽最终操作容器的namespaces、cgroups、apparmor、⽹络设备以及防⽕墙规则等。这⼀系列操作的完成都不需要依赖LXC或者其他包。libcontainer架构如图1-8所⽰。

![](images/c93d5166d69416a40164934cd8968b7d6ce51950a6ae00e4ddae60a32e653f94.jpg)  
图1-8 libcontainer⽰意图

另外，libcontainer提供了⼀整套标准的接⼝来满⾜上层对容器管理的需求。或者说，libcontainer屏蔽了Docker上层对容器的直接管理。⼜由于libcontainer使⽤Go这种跨平台的语⾔开发实现，且本⾝⼜可以被上层多种不同的编程语⾔访问，因此，很难说未来的Docker⼀定会与

Linux平台紧紧捆绑在⼀起。Docker Daemon的逻辑完全有可能位于其他⾮Linux操作系统的平台上，仅仅通过libcontainer的远程调⽤来实现对Docker容器的管理。另⼀⽅⾯，libcontainer与Docker Daemon的松耦合设计，似乎让⽤户感受到了除Linux Container之外其他的容器技术接⼊ Docker Daemon 的 可 能 性 。 libcontainer 承 接 Linux 内 核 与 DockerDaemon的同时，也让Docker的⽣态在跨平台⽅⾯充满⽣机。与此同时，Microsoft在其著名云计算平台Azure中，也添加了对Docker的⽀持，可⻅Docker的开放程度与业界的⽕热度。

暂不谈Docker，由于本⾝完善的功能以及与应⽤系统的松耦合特性，libcontainer很有可能会在众多其他以容器为原型的平台出现，同时也很有可能催⽣出云计算领域全新的项⽬。

# 1.3.7 Docker Container

Docker Container（Docker容器）是Docker架构中服务交付的最终体现形式。Docker通过DockerDaemon的管理，libcontainer的执⾏，最终创建Docker容器。Docker容器作为⼀个交付单位，功能类似于传统意义上的虚拟机（Virtual Machine），具备资源受限、环境与外界隔离的特点。然⽽，实现⼿段却与KVM、Xen等传统虚拟化技术⼤相径庭。

Docker容器的从⽆到有，涉及Docker利⽤到的很多技术。总⽽⾔之，⽤户可以根据⾃⼰的需求，通过Docker Client向Docker Daemon发送容器的创建与启动请求，请求中将携带容器的配置信息，从⽽达到定制相应Docker容器的⽬的。⽤户对Docker容器的配置有以下4个基本⽅⾯：

·通过指定容器镜像，使得Docker容器可以⾃定义rootfs等⽂件系统。  
·通过指定物理资源的配额，如CPU、内存等，使得Docker容器使⽤受限的物理资源。  
·通过配置容器⽹络及其安全策略，使得Docker容器拥有独⽴且安全的⽹络环境。  
·通过指定容器的运⾏命令，使得Docker容器执⾏指定的任务。

Docker容器⽰意图如图1-9所⽰。

![](images/5a0a2db471a3e07bf0bc0e8a6a960606703f7a40d587e50353e7eeb031f71a7c.jpg)  
图1-9 Docker容器⽰意图

# 1.4 Docker运⾏案例分析

1.3节着重介绍了Docker架构中各个模块的功能，学完后我们可以对Docker的架构有⼀个宏观的认识。熟悉⼀款软件，研究⼀个系统，从静态的⾓度认识架构的各个模块，仅仅是第⼀步；从动态的⾓度，掌握软件或者系统的运⾏原理，即熟知架构中模块间的通信逻辑，⽆疑会让⾃⼰对软件或系统的理解更上⼀层楼。本节将从实际的Docker运⾏案例出发，串联Docker各模块，从⽽学习Docker的运⾏流程。分析原型为Docker中的docker pull与docker run两个命令。

# 1.4.1 docker pull

1.3节中我们提到，⽤户可以为容器指定镜像，作为容器运⾏时的rootfs，既然如此，镜像从何⽽来则成为⼀个关键。答案很简单，⼀切都归功于docker pull命令。

docker pull命令的作⽤是：Docker Daemon从Docker Registry下载指定的容器镜像，并将镜像存储在本地的Graph中，以备后续创建Docker容器时使⽤。docker pull命令的执⾏流程如图1-10所⽰。

图1-10中有编号的箭头表⽰docker pull命令在发起后，Docker架构中相应模块所做的⼀系列运⾏操作。下⾯我们逐⼀分析这些步骤。

1）Docker Client处理⽤户发起的docker pull命令，解析完请求以及参数之后，发送⼀个HTTP请求给Docker Server，HTTP请求⽅法为POST，请求URL为"/images/create？"+"xxx"，实际意义为下载相应的镜像。  
2 ） Docker Server 接 收 以 上 HTTP 请 求 ， 并 交 给 mux.Router ，mux.Router通过URL以及请求⽅法类型来确定执⾏该请求的具体handler。  
3 ） mux.Router 将 请 求 路 由 分 发 ⾄ 相 应 的 handler ， 具 体 为PostImagesCreate。  
4 ） 在 PostImageCreate 这 个 handler 之 中 ， 创 建 并 初 始 化 ⼀ 个 名为"pull"的Job，之后触发执⾏该Job。  
5）名为"pull"的Job在执⾏过程中执⾏pullRepository操作，即从Docker Registry中下载相应的⼀个或者多个Docker镜像。  
6）名为"pull"的Job将下载的Docker镜像交给graphdriver管理。

7）graphdriver负责存储Docker镜像，⼀⽅⾯将实际镜像存储⾄本地⽂件系统中，另⼀⽅⾯为镜像创建对象，由Docker Daemon统⼀管理。

![](images/ff87ec03d679d45927776246353571f1d013a72103c4b368ba8a555458643b99.jpg)  
图1-10 docker pull命令执⾏流程⽰意图

# 1.4.2 docker run

docker run命令的作⽤是创建⼀个全新的Docker容器，并在容器内部运⾏指定命令。Docker Daemon处理⽤户发起的这条命令时，所做⼯作可以分为两部分：第⼀，创建Docker容器对象，并为容器准备所需的rootfs；第⼆，创建容器的运⾏环境，如⽹络环境、资源限制等，最终真正运⾏⽤户指令。因此，在dockerrun命令的完整执⾏流程中，Docker Client给Docker Server发送了两次HTTP请求，第⼆次请求的发起取决于第⼀次请求的返回状态。docker run命令执⾏流程如图1-11所⽰。

![](images/fb0c20108ff3c63ea81de685e188b4c2d8cbb6de10cd2f8598767c072f203741.jpg)  
图1-11 docker run命令执⾏流程⽰意图

图1-11中有编号的箭头表⽰dockerrun命令在发起后，Docker架构中相应模块所做的⼀系列运⾏。下⾯我们逐⼀分析这些步骤：

1）Docker Client处理⽤户发起的docker run命令，解析完请求与参数之后，向Docker Server发送⼀个HTTP请求，HTTP请求⽅法为POST，请求URL为"/containers/create？"+"xxx"，实际意义为创建⼀个容器对象，即Docker Daemon程序逻辑中的容器对象，并⾮实际运⾏的容器。  
2 ） Docker Server 接 收 以 上 HTTP 请 求 ， 并 交 给 mux.Router ，mux.Router通过URL以及请求⽅法来确定执⾏该请求的具体handler。  
3 ） mux.Router 将 请 求 路 由 分 发 ⾄ 相 应 的 handler ， 具 体 为PostContainersCreate。  
4）在PostContainersCreate这个handler之中，创建并初始化⼀个名为"create"的Job，之后触发执⾏该Job。  
5）名为"create"的Job在运⾏过程中执⾏Container.Create操作，该操作需要获取容器镜像来为Docker容器准备rootfs，通过graphdriver完成。  
6）graphdriver从Graph中获取创建Docker容器rootfs所需要的所有镜像。  
7）graphdriver将rootfs的所有镜像通过某种联合⽂件系统的⽅式加载⾄Docker容器指定的⽂件⽬录下。  
8）若以上操作全部正常执⾏，没有返回错误或异常，则DockerClient收到Docker Server返回状态之后，发起第⼆次HTTP请求。请求⽅法为"POST"，请求URL为"/containers/" $^ +$ container_ID $+$ "/start"，实际意义为启动时才创建完毕的容器对象，实现物理容器的真正运⾏。  
9 ） Docker Server 接 收 以 上 HTTP 请 求 ， 并 交 给 mux.Router ，mux.Router通过URL以及请求⽅法来确定执⾏该请求的具体handler。

10 ） mux.Router 将 请 求 路 由 分 发 ⾄ 相 应 的 handler ， 具 体 为PostContainersStart。  
11 ） 在 PostContainersStart 这 个 handler 之 中 ， 创 建 并 初 始 化 名为"start"的Job，之后触发执⾏该Job。  
12）名为"start"的Job执⾏需要完成⼀系列与Docker容器相关的配置⼯作，其中之⼀是为Docker容器⽹络环境分配⽹络资源，如IP资源等，通过调⽤networkdriver完成。  
13）networkdriver为指定的Docker容器分配⽹络资源，其中有IP、port等，另外为容器设置防⽕墙规则。  
14）返回名为"start"的Job，执⾏完⼀些辅助性操作后，Job开始执⾏⽤户指令，调⽤execdriver。  
15）execdriver被调⽤，开始初始化Docker容器内部的运⾏环境，如命名空间、资源控制与隔离，以及⽤户命令的执⾏，相应的操作转交⾄libcontainer来完成。  
16）libcontainer被调⽤，完成Docker容器内部的运⾏环境初始化，并最终执⾏⽤户要求启动的命令。

# 1.5 总结

本章从Docker 1.2.0的源码⼊⼿，⾸先分析抽象出Docker的架构图，并对该架构图中的各个模块进⾏功能与实现的简要分析，最后通过Docker的两个基础命令展⽰了Docker内部的运⾏。

通过对Docker架构的学习，可以全⾯深化对Docker设计、功能与价值的理解。同时在借助Docker实现⽤户定制的分布式系统时，也能更好地找到已有平台与Docker较为理想的契合点。另外，熟悉Docker现有架构以及设计思想，也能为云计算PaaS领域带来更多的启⽰，催⽣出更多创新想法。

# 第2章 Docker Client创建与命令执⾏

# 2.1 引⾔

如今，作为业界领先的轻量级虚拟化容器管理引擎，Docker给全球开发者提供了⼀种新颖、便捷的软件集成测试与部署之道。团队开发软件时，Docker可以提供可复⽤的运⾏环境、灵活的资源配置、便捷的集成测试⽅法，以及⼀键式的部署⽅式。可以说，Docker在简化持续集成、运维部署⽅⾯将其功能发挥得淋漓尽致，它让开发者从重复的持续集成、运维部署中完全解放出来，把精⼒真正地倾注在开发上。

然⽽，要把Docker的功能发挥到极致，并⾮⼀件易事。在深刻理解Docker架构的情况下，熟练掌握Docker Client的使⽤也⾮常有必要。前者可以参阅第1章，本章主要针对后者，从源码的⾓度分析DockerClient，⼒求帮助开发者更深刻地理解Docker Client的具体实现，最终更好地掌握Docker Client的使⽤⽅法。

本章基于Docker 1.2.0的源码，分析Docker Client的内容。主要包括两个部分，分别是DockerClient的创建与Docker Client对命令的执⾏。两部分分析的具体内容如下。

第⼀部分分析Docker Client的创建。这部分的分析可分为以下三个步骤：

·分析如何通过docker命令，解析出命令⾏flag参数，以及docker命令中的请求参数。

·分析如何处理具体的flag参数信息，并收集Docker Client所需的配置信息。

·分析如何创建⼀个Docker Client。

第⼆部分在已有Docker Client的基础上，分析如何执⾏docker命令。这部分的分析⼜可分为以下两个步骤。

·分析如何解析docker命令中的请求参数，获取相应请求的类型。

·分析Docker Client如何执⾏具体的请求命令，最终将请求发送⾄Docker Server。

# 2.2 创建Docker Client

对于Docker这样⼀个Client/Server的架构，客户端的存在意味着Docker相应任务的发起。⽤户⾸先需要创建⼀个DockerClient，随后将特定的请求类型与参数传递⾄Docker Client，最终由Docker Client转义成Docker Server能识别的形式，并发送⾄Docker Server。

Docker Client的创建实质上是Docker⽤户通过⼆进制可执⾏⽂件docker，创建与Docker Server建⽴联系的客户端。以下分3个⼩节分别阐述Docker Client的创建流程。

Docker Client完整的运⾏流程如图2-1所⽰。

![](images/190ede83d40b0a25ba55f04c34830860fe98a243d364b30d744eb70cd5038a1a.jpg)  
图2-1 DockerClient的运⾏流程

通过学习图2-1，我们可以更为清晰地了解Docker Client创建及执⾏请求的过程。其中涉及诸多Docker源码层次中的专有名词，本章后续会⼀⼀解释与分析。

# 2.2.1 Docker命令的flag参数解析

众所周知，在Docker的具体实现中，Docker Server与Docker Client均由可执⾏⽂件docker来完成创建并启动。那么，了解docker可执⾏⽂件通过何种⽅式来区分到底是Docker Server还是Docker Client，就显得尤为重要。

⾸先通过docker命令举例说明其中的区别。Docker Server的启动，命令为docker-d或docker--daemon=true；⽽Docker Client的启动则体现为docker--daemon=false ps、docker pull NAME等。

其实，对于Docker请求中的参数，我们可以将其分为两类：第⼀类为命令⾏参数，即docker程序运⾏时所需提供的参数，如：-D、--daemon=true、--daemon=false等；第⼆类为docker发送给Docker Server的实际请求参数，如：ps、pull NAME等。

对于第⼀类，我们习惯将其称为flag参数，在Go语⾔的标准库中，专门为该类参数提供了⼀个flag包，⽅便进⾏命令⾏参数的解析。

清楚docker⼆进制⽂件的使⽤以及基本的命令⾏flag参数之后，我们 可 以 进 ⼊ 实 现 Docker Client 创 建 的 源 码 中 ， 位于./docker/docker/docker.go。这个go⽂件包含了整个Docker的main函数，也就是整个Docker（不论Docker Daemon还是Docker Client）的运⾏⼊⼝。部分main函数代码如下：

```go
func main() { if reexec Init(){ return } flag.Parse() //FIXME:validate daemon flags here 
```

以上源码实现中，⾸先判断reexec.Init（）⽅法的返回值，若为真，则直接退出运⾏，否则将继续执⾏。reexec.Init（）函数的定义位于./docker/reexec/reexec.go，可以发现由于在docker运⾏之前没有任何Initializer注册，故该代码段执⾏的返回值为假。reexec存在的作⽤是：协调execdriver与容器创建时dockerinit这两者的关系。第13章在分析dockerinit的启动时，将详细讲解reexec的作⽤。

判断reexec.Init（）之后，Docker的main函数通过调⽤flag.Parse（）函数，解析命令⾏中的flag参数。如果熟悉Go语⾔中的flag参数，⼀定知道解析flag参数的值之前，程序必须先定义相应的flag参数。进⼀步查看Docker的源码，我们可以发现Docker在./docker/docker/flag.go中定义了多个flag参数，并通过init函数进⾏部分flag参数的初始化。代码如下：

```txt
var (
    flVersion = flag Bool([[string{"v", "-version"}], false, "Print version information and quit")
    fl Daemon = flag Bool([[string{"d", "-daemon"}], false, "Enable daemon mode")
    flDebug = flag Bool([[string{"D", "-debug"}], false, "Enable debug mode")
    flSocketGroup = flag.String([[string{"G", "-group"}], "docker", "Group to assign the UNIX socket specified by -H when running in daemon modeuse '' (the empty string) to disable setting of a group")
    flEnableCors = flag Bool([[string[#api-enable-cors"], "-api-enable-cors"]
    flTls = flag Bool([[string"-tls"], false, "Use TLS; implied by TLS-verify flags")
    flTlsVerify = flag Bool([[string"-tlsverify"]
    // these are initialized in init() below since their default values depend on dockerCertPath which isn't fully initialized until init()
    runs
    flCa *string
    flCert *string
    flKey *string
    flHosts []
) 
```

）   
func init() { flCa = flag.String([]string{"- tlsacert"，filepath.Join.dockerCertPath，defaultCaFile)，"Trust only remotes providing a certificate signed by the CA given here") flCert = flag.String([]string{"- tlscert"，filepath.Join.dockerCertPath，defaultCertFile)，"Path to TLS certificate file") flKey $=$ flag.String([]string{"-tlskey"，filepath.Join.dockerCertPath，defaultKeyFile)，"Path to TLS key file") opts.HostListVar(&flHosts， []string{"H"， "- host"，"The socket(s) to bind to in daemon mode\nspecified using one or more tcp://host:port，unix：//path/to/socket，fd:/\* or fd://socketfd.") }

以上源码展⽰了Docker如何定义flag参数，以及在init函数中实现部分flag参数的初始化。Docker的main函数执⾏前，这些变量创建以及初始化⼯作已经全部完成。这⾥涉及了Go语⾔的⼀个特性，即init函数的执⾏。Go语⾔中引⼊其他包（import package）、变量的定义、init函数以及main函数这四者的执⾏顺序如图2-2所⽰。

关于Golang中的init函数，深⼊分析可以得出以下特性：

·init函数⽤于程序执⾏前包的初始化⼯作，⽐如初始化变量等；  
·每个包可以有多个init函数；  
·包的每⼀个源⽂件也可以有多个init函数；  
·同⼀个包内的init函数的执⾏顺序没有明确的定义；  
·不同包的init函数按照包导⼊的依赖关系决定初始化的顺序；  
·init函数不能被调⽤，⽽是在main函数调⽤前⾃动被调⽤。

![](images/045cc5b56817ecd86e34d54b7dbddde24805d3ea0458562143b60a43f2d38076.jpg)  
图2-2 Go语⾔程序加载顺序图

清楚Go语⾔⼀些基本的特性之后，回到Docker中来。Docker的main函数执⾏之前，Docker已经定义了诸多flag参数，并对很多flag参数进⾏初始化。定义并初始化的命令⾏flag参数有：flVersion、flDaemon、flDebug、flSocketGroup、flEnableCors、flTls、flTlsVerify、flCa、flCert、flKey、flHosts等。

以下具体分析flDaemon：

· 定 义 ： flDaemon=flag.Bool （ []string{"d" ， "-daemon"} ，false，"Enable daemon mode"）；  
·flDaemon的类型为Bool类型；  
·flDaemon名称为"d"或者"-daemon"，该名称会出现在docker命令中，如docker–d；  
·flDaemon的默认值为false；  
·flDaemon的⽤途信息为"Enable daemon mode"；

·访问flDaemon的值时，使⽤指针*flDaemon解引⽤访问。

在解析命令⾏flag参数时，以下语句为合法的（以flDaemon为例）：

·-d，--daemon

·-d=true，--daemon=true

·-d="true"，--daemon $\equiv$ "true"

·-d='true'，--daemon $\ c =$ 'true'

当解析到第⼀个⾮定义的flag参数时，命令⾏flag参数解析⼯作结束。举例说明，当执⾏docker命令docker--daemon=false--version=falseps时，flag参数解析主要完成两个⼯作：

·完成命令⾏flag参数的解析，根据flag的名称-daemon和-version，得知具体的flag参数为flDaemon和flVersion，并获得相应的值，均为false。

·遇到第⼀个⾮定义的flag参数ps时，flag包会将ps及其之后所有的参数存⼊flag.Args（），以便之后执⾏Docker Client具体的请求时使⽤。

如 需 深 ⼊ 学 习 flag 的 实 现 ， 可 以 参 ⻅ Docker 源码./docker/pkg/mflag/flag.go。

# 2.2.2 处理flag信息并收集Docker Client的配置信息

理解Go语⾔解析flag参数的相关知识，可以很⼤程度上帮助理解Docker的main函数的执⾏流程。通过总结，⾸先列出源码中处理的flag信息以及收集Docker Client的配置信息，然后再⼀⼀进⾏分析：

·处理的flag参数有：flVersion、flDebug、flDaemon、flTlsVerify以及flTls。

·为Docker Client收集的配置信息有：protoAddrParts（通过flHosts参数获得，作⽤是提供Docker Client与Docker Server的通信协议以及通信 地 址 ） 、 tlsConfig （ 通 过 ⼀ 系 列 flag 参 数 获 得 ， 如 *flTls 、*flTlsVerify，作⽤是提供安全传输层协议的保障）。

清楚flag参数以及Docker Client的配置信息之后，我们进⼊main函数的源码，具体分析如下。

在flag.Parse（）之后的源码如下：

```txt
if \*flVersion{ showVersion() return } 
```

以上代码很好理解，解析flag参数后，若Docker发现flag参数flVersion为真，则说明Docker⽤户希望查看Docker的版本信息。此时，Docker调⽤showVersion（）显⽰版本信息，并从main函数退出；否则的话，继续往下执⾏。

```go
if \*flDebug{ os.Setenv("DEBUG","1") } 
```

若flDebug参数为真的话，Docker通过os包中的Setenv函数创建⼀个名为DEBUG的环境变量，并将其值设为"1"；继续往下执⾏。

```go
if len(flHosts) == 0 {
    defaultHost := os.Getenv("DOCKER_HOST")
    if defaultHost == "" || *fl Daemon {
        // If we do not have a host, default to UNIX socket
            defaultHost = fmt.Printf("unix: //%s", api.DEFAULTUNIXSOCKET)
    }
    if _, err := apiValidateHost(defaultHost); err != nil {
        log.Fatal(err)
    }
    flHosts = append(flHosts, defaultHost)
} 
```

以上的源码主要分析内部变量flHosts。flHosts的作⽤是为DockerClient提供所要连接的host对象，也就是为Docker Server提供所要监听的对象。

在分析过程中，⾸先判断flHosts变量是否⻓度为0。若是的话，则说明⽤户并没有显性传⼊地址，此时Docker的策略为选⽤默认值。Docker通过os包获取名为DOCKER_HOST环境变量的值，将其赋值于defaultHost。若defaultHost为空或者flDaemon为真，说明⽬前还没有⼀个 定 义 的 host 对 象 ， 则 将 其 默 认 设 置 为 unix socket ， 值 为api.DEFAULTUNIXSOCKET，该常量位于./docker/api/common.go，值为 "/var/run/docker.sock" ， 故 defaultHost为"unix：///var/run/docker.sock"。验证该defaultHost的合法性之后，将

defaultHost的值追加⾄flHost的末尾，继续往下执⾏。当然若flHost的⻓度不为0，则说明⽤户已经指定地址，同样继续往下执⾏。

```txt
if \*flDaemon{ mainDaemon() return } 
```

若flDaemon参数为真，则说明⽤户的需求是启动Docker Daemon。Docker随即执⾏mainDaemon函数，实现Docker Daemon的启动；若mainDaemon函数执⾏完毕，则退出main函数。⼀般mainDaemon函数不会主动终结，Docker Daemon将作为⼀个常驻进程运⾏在宿主机上。本章着重介绍Docker Client的启动，故假设flDaemon参数为假，不执⾏以上代码块。继续往下执⾏。

```go
if len(flHosts) > 1 {
    log.Fatal("Please specify only one -H")
}
protoAddrParts := strings.SplitN(flHosts[0], "/", 2) 
```

由于不执⾏Docker Daemon的启动流程，故属于Docker Client的执⾏逻辑。⾸先，判断flHosts的⻓度是否⼤于1。若flHosts的⻓度⼤于1，则说明需要新创建的Docker Client访问不⽌1个Docker Daemon地址，显然逻辑上⾏不通，故抛出错误⽇志，提醒⽤户只能指定⼀个DockerDaemon地址。接着，Docker将flHosts这个string数组中的第⼀个元素进⾏分割，通过"：//"来分割，分割出的两个部分放⼊变量protoAddrParts数组中。protoAddrParts的作⽤是：解析出Docker Client与Docker Server建⽴通信的协议与地址，为Docker Client创建过程中不可或缺的配置信息之⼀。⼀般情况下，flHosts[0]的值可以是tcp：//0.0.0.0：2375或者unix：///var/run/docker.sock等。

```kotlin
var (
    cli   *client.DockerCLI
    plsConfig pls.Config
) 
plsConfig.InsecureSkipVerify = true 
```

由于之前已经假设过flDaemon为假，可以认定main函数的运⾏是为了Docker Client的创建与执⾏。Docker在这⾥创建了两个变量：⼀个为类型是*client.DockerCli的对象cli，另⼀个为类型是tls.Config的对象tlsConfig。定义完变量之后，Docker将tlsConfig的InsecureSkipVerify属性置为真。tlsConfig对象的创建是为了保障cli在传输数据的时候遵循安全传输层协议（TLS）。安全传输层协议（TLS）⽤于确保两个通信应⽤程序之间的保密性与数据完整性。tlsConfig是Docker Client创建过程中可选的配置信息。

```txt
// If we should verify the server, we need to load a trusted ca  
if *flTlsVerify {  
    *flTls = true  
    certPool := x509.NewCertPool()  
    file, err := ioutil.ReadFile(*flCa)  
    if err != nil {  
        log.Fatal("Couldn't read ca cert %s: %s", *flCa, err)  
    }  
    certPool.Add CertsFromPEM(file)  
    tlsConfig RootsCAs = certPool  
    tlsConfig.InsecureSkipVerify = false  
} 
```

若 flTlsVerify 这 个 flag 参 数 为 真 ， 则 说 明 Docker Client 需 DockerServer⼀起验证连接的安全性。此时，tlsConfig对象需要加载⼀个受信的ca⽂件。该ca⽂件的路径为*flCA参数的值，最终完成tlsConfig对象中RootCAs属性的赋值，并将InsecureSkipVerify属性置为假。

```txt
// If tls is enabled, try to load and send client certificates
if *flTls || *flTlsVerify {
    _, errCert := os.Stat(*flCert)
    _, errKey := os.Stat(*flKey)
    if errCert == nil && errKey == nil {
        *flTls = true
        cert, err := tls.LoadX509KeyPair(*flCert, *flKey)
        if err != nil {
            log.Fatal("Couldn't load X509 key pair: %s. Key encrypted?", err)
        }
        tlsConfig CERTificates = []tls CERTicate{cert}
    }
} 
```

如果flTls和flTlsVerify两个flag参数中有⼀个为真，则说明需要加载并发送客户端的证书。最终将证书内容交给tlsConfig的Certificates属性。

⾄此，flag参数已经全部处理完毕，DockerClient也已经收集到所需的配置信息。下⼀节将主要分析如何创建Docker Client。

# 2.2.3 如何创建Docker Client

Docker Client的创建其实就是在已有配置参数信息的情况下，通过Client包中的NewDockerCli⽅法创建⼀个Docker Clinet实例cli。具体源码实现如下：

if \*flTls || \*flTlsVerify{ cli $=$ client.NewDockerCLI(os.Stdin, os.Stdout, os.Stderr, protoAddrParts[0], protoAddrParts[1], &tlsConfig) } else { cli $=$ client.NewDockerCLI(os.Stdin, os.Stdout, os.Stderr, protoAddrParts[0], protoAddrParts[1], nil) 1

若flag参数flTls为真或者flTlsVerify为真，则说明需要使⽤TLS协议来保障传输的安全性，故创建Docker Client的时候，将tlsConfig参数传⼊；否则，同样创建Docker Client，只不过tlsConfig为nil。

关 于 Client 包 中 的 NewDockerCli 函 数 的 实 现 ， 可 以 具 体 参⻅./docker/api/client/cli.go。

```txt
func NewDockerCLI(in io.ReadCloser, out, err io+writerr, proto, addr string, tlsConfig *tls.Config) *DockerCLI {
    var (
        isTerminal = false
        terminalFd uintptr
        scheme = "http"
    )
    if tlsConfig != nil {
        scheme = "https"
    }
    if in != nil {
        if file, ok := out (*os.File); ok {
            terminalFd = file.Fd()
            isTerminal = term.IsTerminal(tcllFd)
        }
    }
} 
```

}   
if err $= =$ nil { err $=$ out   
}   
return &DockerCLI{ proto: proto, addr: addr, in: in, out: out, err: err, isTerminal: isTerminal, terminalFd: terminalFd, tlsConfig: tlsConfig, scheme: scheme, }

总体⽽⾔，创建DockerCli对象的过程⽐较简单。较为重要的DockerCli的属性有：proto，DockerClient与Docker Server的传输协议；addr，Docker Client需要访问的host⽬标地址；tlsConfig，安全传输层协议的配置。若tlsConfig不为空，则说明需要使⽤安全传输层协议，DockerCli对象的scheme设置为“https”，另外还有关于输⼊、输出以及错误显⽰的配置等。最终函数返回DockerCli对象。

通过调⽤client包中的NewDockerCli函数，程序最终创建了DockerClient，返回main包中的main函数之后，程序继续往下执⾏。

# 2.3 Docker命令执⾏

main函数执⾏到这个阶段，有以下内容需要为Docker命令的执⾏服务：创建完毕的Docker Client，docker命令中的请求参数（经flag解析后存放于flag.Arg（））。也就是说，程序需要使⽤Docker Client来分析Docker命令中的请求参数，得出请求的类型，转义为Docker Server可以识别的请求之后，最终发送给Docker Server。

Docker Client主要完成两⽅⾯的⼯作：解析请求命令，得出请求类型；执⾏具体类型的请求。本节将从这两个⽅⾯深⼊分析DockerClient。

# 2.3.1 Docker Client解析请求命令

Docker Client解析请求命令的⼯作，在Docker命令执⾏部分第⼀个完成。创建Docker Client之后，回到main函数中，继续执⾏的源码如下（位于./docker/docker/docker.go#L102-L110）：

```go
if err := cli Cmdflag.Args()....); err != nil { if sterr, ok := err.(*utils.StatusError); ok { if sterr.Status != "" { log.Println(sterr.Status) } os.Exit(sterr.StatusCode) } log.Fatal(err) } 
```

学习以上源码可以发现，正如之前所说，Docker Client⾸先解析存放于flag.Args（）中的具体请求参数，执⾏的函数为cli对象的Cmd函数。Cmd函数的定义如下（位于./docker/api/client/cli.go#L51-L61）：

// Cmd executes the specified command   
func (cli \*DockerCLI) Cmd(args ...string) error { if len(args) $>0$ { method, exists := cli/method(args[0]) if !exists { fmt.Println("Error: Command not found:", args[0]) return cli CmdHelp(args[1]:...) } return method(args[1]...） 1 return cli CmdHelp(args...)   
}

由代码注释可知，Cmd函数执⾏具体的指令。在源码实现中，⾸先判断请求参数列表的⻓度是否⼤于0。若⻓度不⼤于0，则说明没有请求信息，返回docker命令的Help信息；若⻓度⼤于1，则说明有请求信息，那么Docker Client⾸先通过请求参数列表中的第⼀个元素args[0]来获取具体的请求⽅法method。若上述method⽅法不存在，则返回docker命令的Help信息，若存在，调⽤具体的method⽅法，参数为args[1]及其之后所有的请求参数。

还 是 以 ⼀ 个 具 体 的 docker 命 令 为 例 ， docker--daemon=false--version $\underline { { \underline { { \mathbf { \Pi } } } } }$ false pull Image_Name。通过以上Docker Client的分析，可以总结出以下执⾏流程。

1 ） 解 析 flag 参 数 之 后 ， Docker 将 docker 请 求 参数"pull"和"Image_Name"存放于flag.Args（）。  
2 ） 创 建 好 的 Docker Client 为 cli ， cli 执 ⾏ cli.Cmd （ flag.Args（）…）。  
3 ） Cmd 函 数 中 ， 通 过 args[0] 也 就 是 "pull" ， 执 ⾏ cli.getMethod（args[0]），获取method的名称。  
4 ） 在 getMothod ⽅ 法 中 ， Docker 通 过 处 理 最 终 返 回 method 值为"CmdPull"。  
5 ） 最 终 执 ⾏ method （ args[1 ： ]… ） 也 就 是 CmdPull（args[1：]…）。

# 2.3.2 Docker Client执⾏请求命令

上⼀⼩节通过⼀系列的命令解析，最终找到了具体的命令执⾏⽅法，本⼩节主要介绍Docker Client如何通过具体的执⾏⽅法，处理并发送请求。

不同的Docker尽管请求内容不同，但是请求执⾏流程⼤致相同，故本节依旧以⼀个例⼦来阐述其中的流程，例⼦为：docker pullImage_Name。该命令的作⽤为：DockerClient发起下载镜像的请求，最终由Docker Server接收请求，由Docker Daemon完成镜像的下载与存储。

Docker Client 在 执 ⾏ docker pull Image_Name 请 求 命 令 时 ， 执 ⾏CmdPull函数，传⼊参数为args[1：]...，即Image_Name。源码实现位于./docker/api/client/command.go#L1183-L1224。

下⾯逐⼀分析CmdPull的源码实现。

```go
cmd := cli.Subcmd("pull", "NAME[:TAG]", "Pull an image or a repository from the registry") 
```

通过cli包中的Subcmd⽅法定义⼀个类型为Flagset的对象cmd。

```go
tag := cmd.String([[string{"#t", "#-tag"}, "", "Download tagged image in a repository") 
```

为cmd对象定义⼀个类型为String的flag，名为"#t"或"#-tag"，初始值为空，⽬前这个flag参数基本已经被弃⽤。

```txt
if err := cmd.Parse(args); err != nil {
return nil
} 
```

将args参数进⾏第⼆次flag参数解析，解析过程中，先提取出是否有符合tag这个flag的参数。若有，将其赋值给tag参数，其余的参数存⼊cmd.NArg（）；若没有，则将所有的参数存⼊cmd.NArg（）中。

```m4
if cmd.NArg() != 1 {
    cmd_USAGE()
    return nil
} 
```

判断经过flag解析后的参数列表，若参数列表中参数的个数不为1，则说明需要下拉多个镜像或者没有指定下拉镜像名称，pull命令均不⽀持，则调⽤错误处理⽅法cmd.Usage（），并返回nil。

```go
var (
    v = url.Values {}
    remote = cmd.Arg(0)
)
v.Set("fromImage", remote)
if *tag == "" {
    v.Set("tag", *tag)
} 
```

创建⼀个map类型的变量v，该变量⽤于存放下拉镜像时所需的URL参数；随后将参数列表的第⼀个值cmd.Arg（0）赋给remote变量，并将remote作为键将fromImage的值添加⾄v。

remote, $\equiv$ parsers.ParseRepositoryTag(zero) // Resolve the Repository name from fqn to hostname + name hostname, err: registry ResolveRepositoryName(zero) if err != nil { return err }

通过remote变量⾸先得到镜像的repository名称，并赋给remote⾃⾝，随后通过解析改变后的remote，得出镜像所在的host地址，即Docker Registry的地址。若⽤户没有制定Docker Registry的地址，则Docker默认地址为Docker Hub地址https://index.docker.io/v1/。

```txt
cli.LoadConfigFile() // Resolve the Auth config relevant for this server authConfig := cli.configFile ResolveAuthConfig(hostname) 
```

通过cli对象获取与Docker Server通信所需要的认证配置信息。

```go
pull := func(authConfig registry.AuthConfig) error {
    buf, err := json.Marshall.authConfig)
    if err != nil {
        return err
    }
    registryAuthHeader := []
    base64-encodedEncodeToString(buf),
} 
return cli.stream("POST", "/images/create?"+v Encode(), nil, cli.out, map[string][]string{
    "X-Registry-Auth": registryAuthHeader,
}) 
```

定义⼀个名为pull的函数，传⼊的参数类型为registry.AuthConfig，返回类型为error。函数执⾏块中最主要的内容为：cli.stream（……）部分 。 该 部 分 具 体 向 Docker Server 发 送 POST 请 求 ， 请 求 的 url为"/images/create？"+v.Encode（），请求的认证信息为：map[string][]string{"X-Registry-Auth"：registryAuthHeader，}。

```go
if err := pull(authConfig); err != nil {
    if strings.contains(err.Error(), "Status 401") { 
```

```go
fmt.Fprintln(cluster.out, "\nPlease login prior to pull:") if err := cluster CmdLogin(hostname); err != nil { return err } authConfig := cluster.configFile ResolveAuthConfig(hostname) return pull(authConfig) } return err } 
```

由于上⼀个步骤只是定义pull函数，这⼀步骤具体调⽤执⾏pull函数，实现实际意义上的下载请求发送。若返回成功则表明请求完成，程序直接退出，若返回错误，则做相应的错误处理。若返回错误为401，则表⽰⽤户下载的镜像必须⽤户先登录，随即Docker Client转⾄登录环节，完成之后，继续执⾏pull函数，若完成则最终返回。

以上便是pull请求的全部执⾏过程，其他请求的执⾏在流程上也是⼤同⼩异。总之，请求执⾏过程中，⼤多都是将命令⾏中关于请求的参数进⾏初步处理，并添加相应的辅助信息，最终通过指定的协议向Docker Server发送Docker Client和Docker Server约定好的API请求。

# 2.4 总结

本章从源码的⾓度分析了如何通过docker可执⾏⽂件创建DockerClient，最终发送⽤户请求⾄Docker Server。

通过学习与理解Docker Client相关的源码实现，不仅可以让⽤户熟练掌握Docker命令的使⽤，还可以使⽤户在特殊情况下有能⼒修改Docker Client的源码，使其满⾜⾃⾝系统的某些特殊需求，以达到定制Docker Client的⽬的，最⼤限度地发挥Docker开源思想的价值。

# 第3章 启动Docker Daemon

# 3.1 引⾔

⾃Docker诞⽣以来，便引领了轻量级虚拟化容器领域的技术热潮。在这⼀潮流下，Google、IBM、Redhat等业界翘楚纷纷加⼊Docker阵营。虽然⽬前Docker仍主要基于Linux平台，但是Microsoft却多次宣布对Docker的⽀持，从先前宣布的Azure⽀持Docker与Kubernetes，到如今宣布的下⼀代Windows Server原⽣态⽀持Docker。Microsoft的这⼀系列举措多少喻⽰着向Linux世界的妥协，当然这也不得不让世⼈对Docker的巨⼤影响⼒有重新的认识。

Docker的影响⼒不⾔⽽喻，但如果需要深⼊学习Docker的内部实现 ， 最 重 要 的 就 是 理 解 Docker Daemon 。 在 Docker 架 构 中 ， DockerClient通过特定的协议与Docker Daemon进⾏通信，⽽Docker Daemon主要承载了Docker运⾏过程中的⼤部分⼯作。

Docker Daemon是Docker架构中运⾏在后台的守护进程，⼤致可以分为Docker Server、Engine和Job三部分。三者的关系⼤致如下：Docker Daemon通过Docker Server模块接收Docker Client的请求，并在Engine中处理请求，然后根据请求类型，创建出指定的Job并运⾏。由于⽤户的请求不同，DockerDaemon会创建不同的Job来完成任务，如：⽤户发起镜像下载请求，DockerDaemon创建名为“pull”的Job；⽤户发起启动容器的请求，DockerDaemon创建名为“start”的Job……

Docker Daemon的架构如图3-1所⽰。

本章从源码的⾓度，主要分析Docker Daemon的启动流程。由于Docker Daemon和Docker Client的启动流程有很多的相似之处，故本章不再赘述Docker Daemon启动的前期⼯作、flag参数的解析等内容，着

重分析Docker Daemon启动流程中最为重要的环节：创建Daemon过程中mainDaemon（）的实现。

![](images/b9f8bf9eb41c429797473b0b8de4a479f71defdf2ba22aa18f65615e828f572f.jpg)  
图3-1 DockerDaemon架构⽰意图

# 3.2 Docker Daemon的启动流程

Docker Daemon和Docker Client的启动均通过可执⾏⽂件docker完成，因此两者的启动流程⾮常相似。Docker可执⾏⽂件运⾏时，程序运⾏通过不同的命令⾏flag参数，区分两者，并最终运⾏两者各⾃相应的部分。

启 动 Docker Daemon 时 ， ⼀ 般 可 以 使 ⽤ 以 下 命 令 ： docker--daemon $\underline { { \underline { { \mathbf { \Pi } } } } }$ true、docker–d；docker- $\scriptstyle \cdot \mathbf { d } =$ true等。随后由Docker的main（）函数来解析以上命令的相应flag参数，并最终完成Docker Daemon的启动。

⾸先，附上Docker Daemon的启动流程图，如图3-2所⽰。

![](images/2dd0c2fcf3bb3aac01fcd101928467b97a2f528a87d3efc008d66341eb2e6365.jpg)  
图3-2 DockerDaemon启动流程图

本书第2章已经描述了Docker中main（）函数运⾏的很多前期⼯作，Docker Daemon的启动也会涉及这些⼯作，故在此略去相同部分，主要针对后续仅和Docker Daemon相关的内容进⾏深⼊分析，即mainDaemon（）的具体源码实现。

# 3.3 mainDaemon 的具体实现

Docker Daemon的启动流程图展⽰了DockerDaemon的从⽆到有。通过分析流程图，我们可以得出⼀个这样的结论：区分Docker Daemon与Docker Client的关键在于flag参数flDaemon的值。⼀旦*flDaemon的值为真，则代表docker⼆进制需要启动的是Docker Daemon。有关DockerDaemon的所有的⼯作，都被包含在函数mainDaemon（）的具体实现中。

宏观来讲，mainDaemon（）的使命是：创建⼀个守护进程，并保证其正常运⾏。

从功能的⾓度来说，mainDaemon（）实现了两部分内容：第⼀，创建Docker运⾏环境；第⼆，服务于Docker Client，接收并处理相应请求（完成Docker Server的初始化）。

从实现细节来分析，mainDaemon（）的实现流程主要包含以下步骤：

1）daemon的配置初始化。这部分在init（）函数中实现，即在mainDaemon（）运⾏前就执⾏，但由于这部分内容和mainDaemon（）的运⾏息息相关，可以认为是mainDaemon（）运⾏的先决条件。  
2）命令⾏flag参数检查。  
3）创建engine对象。  
4）设置engine的信号捕获及处理⽅法。  
5）加载builtins。   
6）使⽤goroutine加载daemon对象并运⾏。

7）打印Docker版本及驱动信息。  
8）serveapi的创建与运⾏。

对于以上内容，本章将⼀⼀深⼊分析。

# 3.3.1 配置初始化

mainDaemon（）的运⾏位于./docker/docker/docker/daemon.go，深⼊分析mainDaemon（）的实现之前，我们回到Go语⾔的特性，即变量与 init 函 数 的 执 ⾏ 顺 序 。 在 daemon.go 中 ， Docker 定 义 了 变 量daemonCfg，以及init函数，通过Go语⾔的特性，变量的定义与init函数均会在mainDaemon（）之前运⾏。两者的定义如下：

```txt
var (
    daemonCfg = &daemon.Config\}
)
func init() {
    daemonCfg.InstallFlags()
} 
```

⾸先，Docker声明⼀个名为daemonCfg的变量，代表整个DockerDaemon的配置信息。定义完毕之后，init函数的存在，使得daemonCfg变量能获取相应的属性值。在Docker Daemon启动时，daemonCfg变量被传递⾄Docker Daemon并被使⽤。

Config 对 象 的 定 义 如 下 （ 含 部 分 属 性 的 解 释 ） ， 该 对 象 位于./docker/docker/daemon/config.go：

```txt
type Config struct {  
Pidfle string //Docker Daemon所属进程的PID文件  
Root string //Docker运行时所使用的root路径  
AutoRestart bool //是否一直支持创建容器的重启  
Dns [string//Docker Daemon为容器准备的DNS Server地址  
DnsSearch [string//Docker使用的指定的DNS查找地址  
Mirrors [string//指定的Docker Registry镜像地址  
EnableIptables bool //是否启用Docker的iptables功能  
EnableIpForward bool //是否启用net.ipv4.ip_forward功能
```

```txt
EnableIpMasq bool //启用IP伪装技术  
DefaultIp net.IP //绑定容器端口时使用的默认IP  
BridgeIface string //添加容器网络至已有的网桥接口名  
BridgeIP string //创建网桥的IP地址  
FixedCIDR string //指定IP的IPv4子网，必须被网桥子网包含  
InterContainerCommunication bool //是否允许宿主机上Docker容器间的通信  
GraphDriver string //Docker Daemon运行时使用的特定存储驱动  
GraphOptions []string//可设置的存储驱动选项  
ExecDriver string //Docker运行时使用的特定exec驱动  
Mtu int //设置容器网络接口的MTU  
DisableNetwork bool //是否支持Docker容器的网络模式  
EnableSelenuxSupport bool //是否启用对SELinux功能的支持  
Context map[string][]string
```

Docker声明daemonCfg之后，init函数实现了daemonCfg变量中各属性 的 赋 值 ， 具 体 的 实 现 为 ： daemonCfg.InstallFlags （ ） ， 位于./docker/docker/daemon/config.go，代码如下：

```lua
func (config *Config) InstallFlags() {
    flag.StringVar(&config.Pidfile, []string["p", "-pidfile"], "/var/run/docker.pid", "Path to use for daemon PID file")
    flag.StringVar(&config.root, []string["g", "-graph"], "/var/lib/docker", "Path to use as the root of the Docker runtime")
    flag BoolVar(&config.AutoRestart, []string[#r], "#-restart", true, "--restart on the daemon has been deprecated infavor of --restart policies on docker run")
    flag BoolVar(&config EnableIptables, []string[#iptables], "-iptables", true, "Enable Docker's addition of iptables rules")
    flag BoolVar(&config.EnableForward, []string[#ip-forward], "-ip-forward", true, "Enable net.ipv4.ip_forward")
    flag.StringVar(&config.BridgeIP, []string[#bip], "-bip", "", "Use this CIDR notation address for the network bridge's IP, not compatible with -b")
    flag.StringVar(&config.BridgeIface, [], string["b", "-bridge"], "", "Attach containers to a pre-existing network bridge\nuse 'none' to disable container networking")
    flag BoolVar(&config.InterContainerCommunication, [], string[#icc], "-icc", true, "Enable inter-container communication") 
```

```txt
flag StringVar(&config.GraphDriver, []string["s", "-storage-driver"], "", "Force the Docker runtime to use a specific storage driver")  
flag StringVar(&config.ExecDriver, []string["e", "-exec-driver"], "native", "Force the Docker runtime to use a specific exec driver")  
flag BoolVar(&config.EnableSelenuxSupport, []string[-selinux-enabled], false, "Enable selinux support. SELinux does not presently support the BTRFS storage driver")  
flag.IntVar(&config.Mtu, [], string[#mtu], [-mtu]), 0, "Set the containers network MTU\ref no value is provided: default to the default route MTU or 1500 if no default route is available")  
refs.IPVar(&config.DefaultIp, [], string[#ip], "-ip", "0.0.0.0", "Default IP address to use when binding container ports")  
refs.ListVar(&config.GraphOptions, [], string[-storage-opt]), "Set storage driver options")  
// FIXME: why the inconsistency between "hosts" and "sockets"?  
refs.IPListVar(&config.Dns, [], string[#dns], "-dns"), "Force Docker to use specific DNS servers")  
refs.DnsSearchListVar(&config.DnsSearch, [], string[-dns-search], "Force Docker to use specific DNS search domains")  
} 
```

在函数InstallFlags（）的实现过程中，Docker主要定义了众多类型不⼀的flag参数，并将该参数的值绑定在daemonCfg变量的指定属性上，如：

```txt
flag.StringVar(&config.Pidfile, [string["p", "-pidfile"], "/var/run/docker.pid", "Path to use for daemon PID file") 
```

以上语句的含义为：

·定义⼀个String类型的flag参数。

·该flag的名称为"p"或者"-pidfile"。

· 该 flag 的 默 认 值 为 "/var/run/docker.pid" ， 并 将 该 值 绑 定 在 变 量config.Pidfile上。  
·该flag的描述信息为"Path to use for daemon PID file"。

⾄此，关于Docker Daemon所需要的配置信息均声明并初始化完毕。

# 3.3.2 flag参数检查

从本⼩节开始，程序运⾏真正进⼊Docker Daemon的mainDaemon（），下⾯对此流程进⾏深⼊分析。

mainDaemon（）运⾏的第⼀个步骤是命令⾏flag参数的检查。具体⽽⾔，即当docker命令经过flag参数解析之后，Docker判断剩余的参数是否为0。若为0，则说明Docker Daemon的启动命令⽆误，正常运⾏；若不为0，则说明在启动Docker Daemon的时候，传⼊了多余的参数，此时Docker会输出错误提⽰，并退出运⾏程序。具体代码如下：

```txt
if flag.NArg() != 0 {
    flag_USAGE()
    return
} 
```

# 3.3.3 创建engine对象

在mainDaemon（）运⾏过程中，flag参数检查完毕之后，Docker随即创建engine对象，代码如下：

```txt
eng := engine.New() 
```

Engine是Docker架构中的运⾏引擎，同时也是Docker运⾏的核⼼模块。Engine扮演着Docker Container存储仓库的⾓⾊，并且通过Job的形式管理Docker运⾏中涉及的所有任务。

Engine 结 构 体 的 定 义 位 于 ./docker/docker/engine/engine.go#L47-L60，具体代码如下：

```txt
type Engine struct {  
    handlers map[string]Handler  
    catchall Handler  
    hack Hack // data for temporary hackery (see hack.go)  
    id string  
    Stdout io ,(Writer)  
    Stderr io ,(Writer)  
    Stdin io ,(Reader)  
    Logging bool  
    tasks sync.WaitGroup  
    l sync.RWMutex // lock for shutdown  
    shutdown bool  
    onShutdown [] func() // shutdown handlers  
} 
```

Engine结构体中最为重要的是handlers属性，handlers属性为map类型，key的类型是string，value的类型是Handler。其中Handler类型的定义位于./docker/docker/engine/engine.go#L23，具体代码如下：

可⻅，Handler为⼀个定义的函数。该函数传⼊的参数为Job指针，返回为Status状态。

了解完Engine以及Handler的基本知识之后，我们真正进⼊创建Engine实例的部分，即New（）函数的实现，具体代码如下：

```go
func New() *Engine {
    eng := &Engine{
        handlers: make(map[string]Handler),
        id:     utils.RandomString(),
        Stdout:   os/stdout,
        Stderr:   osStderr,
        Stdin:   os.Stdin,
        Logging:   true,
    }
eng.Register("commands", func(job *Job) Status {
        for _, name := range eng Commands() {
            job.Printf("%s\n", name)
        }
    return StatusOK
})
// Copy existing global handlers
for k, v := range globalhandlers {
    enghandlers[k] = v
}
return eng 
```

分析以上代码，从返回结果可以发现，New（）函数最终返回⼀个Engine实例对象。⽽在代码实现部分，⼤致可以将其分为三个步骤：

1）创建⼀个Engine结构体实例eng，并初始化部分属性，如handlers、id、标准输出stdout、⽇志属性Logging等。  
2）向eng对象注册名为commands的Handler，其中Handler为临时定义的函数func（job*Job）Status{}，该函数的作⽤是通过Job来打印所有已经注册完毕的command名称，最终返回状态StatusOK。  
3）将变量globalHandlers中定义完毕的所有Handler都复制到eng对象的handlers属性中。

⾄此，⼀个基本的Engine对象实例eng已经创建完毕，并实现部分属性的初始化。Docker Daemon启动的后续过程中，仍然会对Engine对象实例进⾏额外的配置。

# 3.3.4 设置engine的信号捕获

Docker在包engine中执⾏完Engine对象的创建与初始化之后，回到mainDaemon（）函数的运⾏，紧接着执⾏的代码为：

signal.Trap(eng.Shutdown)

Docker Daemon作为Linux操作系统上的⼀个后台进程，原则上应该具备处理信号的能⼒。信号处理能⼒的存在，能保障Docker管理员可以通过向Docker Daemon发送信号的⽅式，管理Docker Daemon的运⾏。

再来看以上代码则不难理解其中的含义：在Docker Daemon的运⾏中，设置捕获特定信号后的处理⽅法，特定信号有SIGINT、SIGTERM以及SIGQUIT；当程序捕获SIGINT或者SIGTERM信号时，执⾏相应的善后操作，最后保证Docker Daemon程序退出。

该部分代码的实现位于./docker/docker/pkg/signal/trap.go。实现的流程分为以下4个步骤：

1）创建并设置⼀个channel，⽤于发送信号通知。  
2）定义signals数组变量，初始值为os.SIGINT，os.SIGTERM；若 环境变量DEBUG为空，则添加os.SIGQUIT⾄signals数组。   
3）通过gosignal.Notify（c，signals...）中Notify函数来实现将接收到的signal信号传递给c。需要注意的是只有signals中被罗列出的信号才会被传递给c，其余信号会被直接忽略。  
4）创建⼀个goroutine来处理具体的signal信号，当信号类型为os.Interrupt或者syscall.SIGTERM时，执⾏传⼊Trap函数的具体执⾏⽅

法，形参为cleanup（），实参为eng.Shutdown。

Shutdown （ ） 函 数 的 定 义 位于 ./docker/docker/engine/engine.go#L153-L199 ， 主 要 完 成 的 任 务 是 ：Docker Daemon关闭时，做⼀些必要的善后⼯作。

善后⼯作主要有以下4项：

·Docker Daemon不再接受任何新的Job。

·Docker Daemon等待所有存活的Job执⾏完毕。

·Docker Daemon调⽤所有shutdown的处理⽅法。

·在15秒时间内，若所有的handler执⾏完毕，则Shutdown（）函数返回，否则强制返回。

由于在signal.Trap（eng.Shutdown）函数的具体实现中，⼀旦程序接收到相应的信号，则会执⾏eng.Shutdown这个函数，在执⾏完eng.Shutdown 之 后 ， 随 即 执 ⾏ os.Exit （ 0 ） ， 完 成 当 前 整 个 DockerDaemon 程 序 的 退 出 。 源 码 实 现 位于./docker/docker/pkg/signal/trap.go#L33-L47。

# 3.3.5 加载builtins

DockerDaemon设置完Trap特定信号的处理⽅法（即eng.shutdown（）函数）之后，Docker Daemon 实现了builtins 的加载。Docker 的builtins可以理解为：Docker Daemon运⾏过程中，注册的⼀些任务（Job），这部分任务⼀般与容器的运⾏⽆关，与Docker Daemon的运⾏时信息有关。加载builtins的源码实现如下：

```go
if err :=builtin.Register(eng); err != nil { log.Fatal(err) } 
```

加载builtins完成的具体⼯作是：向engine注册多个Handler，以便后续在执⾏相应任务时，运⾏指定的Handler。这些Handler包括：Docker Daemon宿主机的⽹络初始化、Web API服务、事件查询、版本查 看 、 Docker Registry 的 验 证 与 搜 索 等 。 源 码 实 现 位于./docker/docker/builtins/builtins.go#L16-L30，如下：

```txt
func Register(eng \*engine.Engine) error {
    if err := daemon(eng); err != nil {
        return err
    }
    if err := remote(eng); err != nil {
        return err
    }
    if err := events.New().Install(eng); err != nil {
        return err
    }
    if err := eng.Register("version", dockerVersion); err != nil {
        return err
    }
} 
```

下⾯分析Register函数实现过程中最为主要的5个部分：daemon（eng）、remote（eng）、events.New（）.Install（eng）、eng.Register（ "version" ， dockerVersion ） 以 及 registry.NewService （ ） .Install（eng）。

# 1.注册⽹络初始化处理⽅法

daemon （ eng ） 的 实 现 过 程 ， 主 要 为 eng 对 象 注 册 了 ⼀ 个 键为"init_networkdriver"的处理⽅法，此处理⽅法的值为bridge.InitDriver函数，源码如下：

```go
func daemon(eng *engine.Engine) error {
    return eng.Register("init_networkdriver", bridge InitDriver)
} 
```

需要注意的是，向eng对象注册处理⽅法，并不代表处理⽅法的值函数会被⽴即调⽤执⾏，如注册init_networkdrive时bridge.InitDriver并不 会 直 接 运 ⾏ ， ⽽ 是 将 bridge.InitDriver 的 函 数 ⼊ ⼝ 作 为init_networkdriver的值，写⼊eng的handlers属性中。当Docker Daemon接收到名为init_networkdriver的Job的执⾏请求时，bridge.InitDriver才被Docker Daemon调⽤执⾏。

Bridge.InitDriver 的 具 体 实 现 位于 ./docker/docker/daemon/networkdriver/bridge/driver.go#79-L175 ， 主 要作⽤为：

·获取为Docker服务的⽹络设备地址。  
·创建指定IP地址的⽹桥。

·配置⽹络iptables规则。

· 另 外 还 为 eng 对 象 注 册 了 多 个 Handler ， 如 allocate_interface 、release_interface、allocate_port以及link等。

本书将在第6章详细分析Docker Daemon如何初始化宿主机的⽹络环境。

# 2.注册API服务处理⽅法

remote（eng）的实现过程，主要为eng对象注册了两个Handler，分别为serveapi与acceptconnections，源码实现如下：

```go
func remote(eng \*engine.Engine) error {
    if err := eng.Register("serveapi", apiserverServeApi); err != nil {
        return err
    }
    return eng.Register("acceptconnections", apiserver.AcceptConnections)
} 
```

注册的两个处理⽅法名称分别为serveapi与acceptconnections，相应的执⾏⽅法分别为apiserver.ServeApi与apiserver.AcceptConnections，具体 实 现 位 于 ./docker/docker/api/server/server.go 。 其 中 ， ServeApi 执 ⾏时，通过循环多种指定协议，创建出goroutine协调来配置指定的http.Server，最终为不同协议的请求服务；⽽AcceptConnections的作⽤主要是：通知宿主机上init守护进程Docker Daemon已经启动完毕，可以让Docker Daemon开始服务API请求。

# 3.注册events事件处理⽅法

events.New（）.Install（eng）的实现过程，为Docker注册了多个event事件，功能是给Docker⽤户提供API，使得⽤户可以通过这些API

查看Docker内部的events信息，log信息以及subscribers_count信息。具体的源码位于./docker/docker/events/events.go#L29-L42，如下所⽰：

```go
func (e *Events) Install(eng *engine.Engine) error {
    jobs := map[string]enginepterHandler{
        "events": e.Get,
        "log": e.Log,
        "subscribers_count": e.SubscribersCount,
    }
    for name, job := range jobs {
        if err := eng.Register(name, job); err != nil {
            return err
        }
    }
    return nil
} 
```

# 4.注册版本处理⽅法

eng.Register（"version"，dockerVersion）的实现过程，向eng对象注 册 key 为 version ， value 为 dockerVersion 执 ⾏ ⽅ 法 的 Handler 。dockerVersion的执⾏过程中，会向名为version的Job的标准输出中写⼊Docker的版本、Docker API的版本、git版本、Go语⾔运⾏时版本，以及操作系统版本等信息。dockerVersion的源码实现如下：

```go
func dockerVersion(job *engine JOB) engine.Status {
    v := &engineENV {}
    v.SetJson("Version", dockerversion.VERSION)
    v.SetJson("ApiVersion", api(APIVERSION)
    v.Set("GitCommit", dockerversion.GITCOMMIT)
    v.Set("GoVersion", runtime.Version())
    v.Set("Os", runtime.GOOS)
    v.Set("Arch", runtime GOARCH)
    if kernelVersion, err := kernel.GetKernelVersion(); err == nil { 
```

```go
v.Set("KernelVersion", kernelVersion.String())
}
if _, err := v.CreateTo(job STDout); err != nil {
return job(Error(err))
}
return engine.StatusOK 
```

# 5.注册registry处理⽅法

registry.NewService （ ） .Install （ eng ） 的 实 现 过 程 位于./docker/docker/registry/service.go，功能是：在eng对象对外暴露的API信息中添加docker registry的信息。若registry.NewService（）被成功安装，则会有两个相应的处理⽅法注册⾄eng，Docker Daemon通过Docker Client提供的认证信息向registry发起认证请求；search，在公有registry上搜索指定的镜像，⽬前公有的registry只⽀持Docker Hub。

# Install的具体实现如下：

```go
func (s *Service) Install(eng *engine Engine) error {
    eng.Register("auth", s.Auth)
    eng.Register("search", s.Search)
    return nil
} 
```

⾄此，Docker Daemon所有builtins的加载全部完成，实现了向eng对象注册特定的处理⽅法。

# 3.3.6 使⽤goroutine加载daemon对象并运⾏

Docker执⾏完builtins的加载之后，再次回到mainDaemon（）的执⾏流程中。此时，Docker通过⼀个goroutine协程加载daemon对象并开始运⾏Docker Server。这⼀环节的执⾏，主要包含以下三个步骤：

1）通过init函数中初始化的daemonCfg与eng对象，创建⼀个daemon对象d。  
2）通过daemon对象的Install函数，向eng对象中注册众多的处理⽅法。  
3）在Docker Daemon启动完毕之后，运⾏名为acceptconnections的Job，主要⼯作为向init守护进程发送 $\mathrm { R E A D Y } { = } 1$ 信 号 ， 以 便 DockerServer开始正常接收请求。

源码实现位于./docker/docker/docker/daemon.go#L43-L56，如下所⽰：

```txt
go func() { d, err := daemon.Main Daemon(daemonCfg, eng) if err != nil { log.Fatal(err) } if err := d.Install(eng); err != nil { log.Fatal(err) } if err := eng JOB("acceptconnections").Run(); err != nil { log.Fatal(err) } } 
```

下⾯详细分析三个步骤所做的⼯作。

# 1.创建daemon对象

daemon.NewDaemon（daemonCfg，eng）是创建daemon对象d的核⼼部分，主要作⽤是初始化Docker Daemon的基本环境，如处理config参数，验证系统⽀持度，配置Docker⼯作⽬录，设置与加载多种驱动，创建graph环境，验证DNS配置等。

由 于 daemon.MainDaemon （ daemonCfg ， eng ） 是 加 载 DockerDaemon的核⼼部分，且篇幅过⻓，本书第4章将深⼊分析NewDaemon的实现。

# 2.通过daemon对象为engine注册Handler

Docker创建完daemon对象，goroutine⽴即执⾏d.Install（eng），具体实现位于./docker/daemon/daemon.go，代码如下所⽰：

```python
func (daemon * Daemon) Install(eng *engine. Engine) error {
    for name, method := range map[string]engine Handler{
        "attach": daemon(ContainerAttach,
        "build": daemon CmdBuild,
        "commit": daemon(ContainerCommit,
        "container_changes": daemon(ContainerChanges,
        "container_copy": daemon(ContainerCopy,
        "containerinspect": daemon(ContainerInspect,
        "containers": daemon.Containers,
        "create": daemon(ContainerCreate,
        "delete": daemon(ContainerDestroy,
        "export": daemon(ContainerExport,
        "info": daemonCmdInfo,
        "kill": daemon(ContainerKill,
        ... 
        "image_delete": daemon.ImageDelete,
    } { 
```

```autohotkey
if err := eng.Register(name, method); err != nil {
return err
}
if err := daemonRepositories().Install(eng); err != nil {
return err
}
eng.Hack_SetGlobalVar("httpapi.daemon", daemon)
return nil 
```

以上代码的实现同样分为三部分：

·向eng对象中注册众多的处理⽅法对象。

·daemon.Repositories（）.Install（eng）实现了向eng对象注册多个与 Docker 镜 像 相 关 的 Handler ， Install 的 实 现 位于./docker/docker/graph/service.go。

·eng.Hack_SetGlobalVar（"httpapi.daemon"，daemon）实现向eng对象中类型为map的hack对象中添加⼀条记录，键为httpapi.daemon，值为daemon。

# 3.运⾏名为acceptconnections的Job

Docker在goroutine的最后环节运⾏名为acceptconnections的Job，主要作⽤是通知init守护进程，使Docker Daemon开始接受请求。源码位于./docker/docker/docker/daemon.go#L53-L55，如下所⽰：

```go
// after the daemon is done setting up we can tell the api to start   
// accepting connections   
if err := eng.Job("acceptconnections").Run(); err != nil { log.Fatal(err)   
} 
```

关于Job的运⾏流程⼤同⼩异，总结⽽⾔，都是⾸先创建特定名称的Job，其次为Job配置环境参数，最后运⾏Job对应Handler的函数。作为本书涉及的第⼀个具体Job，下⾯将对acceptconnections这个Job的执⾏进程深⼊分析。

eng.Job（"acceptconnections"）.Run（）的运⾏包含两部分：⾸先执⾏eng.Job（"acceptconnections"），返回⼀个Job实例，随后再执⾏该Job实例的Run（）函数。

eng.Job （ "acceptconnections" ） 的 实 现 位于./docker/docker/engine/engine.go#L115-L137，如下所⽰：

```go
func (eng *Engine) Job(name string, args ...string) *Job {
    job := &Job{
        Eng: eng,
        Name: name,
       Args: args,
        Stdin: NewInput(),
        Stdout: NewOutput(),
        Stderr: NewOutput(),
        env: &Env {},}
    }
    if eng Logging {
        job/stderr.Add(args.NopWriteCloser(eng.Standarderr))
    }
    if handler, exists := enghandlers[name]; exists {
        jobhandler = handler
    } else if eng catches != nil && name != "" {
            jobhandler = eng catches
        }
    return job 
```

通过分析以上创建Job的源码，我们可以发现Docker⾸先创建⼀个类型为Job的job对象，该对象中Eng属性为函数的调⽤者eng，该对象的Name属性为acceptconnections，没有其他参数传⼊。另外在eng对象所有的handlers属性中寻找key为acceptconnections所对应的value值（即具体的Handler）。由于在加载builtins时，源码remote（eng）已经向eng注册 过 这 样 ⼀ 条 记 录 ， 键 为 acceptconnections ， 值 为apiserver.AcceptConnections 。 因 此 ， Job 对 象 的 handler 属 性 为apiserver.AcceptConnections 。 最 后 函 数 返 回 已 经 初 始 化 完 毕 的 对 象Job。

创建完Job对象之后，随即执⾏该job对象的run（）函数。run（）函数的源码实现位于./docker/docker/engine/job.go#L48-L96，该函数执⾏指定的Job，并在Job执⾏完成前⼀直处于阻塞状态。对于名为acceptconnections 的 Job 对 象 ， 运 ⾏ 代 码 为 job.status=job.handler（job），由于job.handler值为apiserver.AcceptConnections，故真正执⾏的是job.status $\underline { { \underline { { \mathbf { \delta \pi } } } } }$ apiserver.AcceptConnections（job）。

AcceptConnections的具体实现属于Docker Server的范畴，深⼊研究Docker Server 可 以 发 现 ， 这 部 分 源 码 位于./docker/docker/api/server/server.go#L1370-L1380，如下所⽰：

```go
func AcceptConnections(job *engine JOB) engine.Status {
    // Tell the init daemon we are accepting requests go systemd.SdNotify("READY=1")
    if activationLock != nil {
        close(activationLock)
    }
    return engine.StatusOK 
```

AcceptConnections 函 数 的 重 点 是 go systemd.SdNotify（ "READY=1" ） 的 实 现 ， 位

于./docker/docker/pkg/system/sdnotify.go#L12-L33，主要作⽤是通知init守护进程Docker Daemon的启动已经全部完成，潜在的功能是要求Docker Daemon开始接收并服务Docker Client发送来的API请求。

⾄此，通过goroutine来加载daemon对象并运⾏启动Docker Server的⼯作全部完成。

# 3.3.7 打印Docker版本及驱动信息

Docker 再 次 回 到 mainDaemon （ ） 的 运 ⾏ 流 程 ， 由 于 Go 语 ⾔goroutine的性质，在goroutine执⾏之时，mainDaemon（）函数内部其他代码也会并发执⾏。

第⼀个执⾏的即为显⽰Docker的版本信息、GitCommit信息、ExecDriver和GraphDriver这两个驱动的具体信息，源码如下：

```javascript
log.Printf("docker daemon: %s %s; execdriver: %s; graphdriver: %s", dockerversion.VERSION, dockerversion.GITCOMMIT, daemonCfg.ExecDriver, daemonCfg.GraphDriver, 
```

# 3.3.8 serveapi的创建与运⾏

打印Docker的部分具体信息之后，Docker Daemon⽴即创建并运⾏名为serveapi的Job，主要作⽤为让Docker Daemon提供Docker Client发起的API服务。实现代码位于./docker/docker/docker/daemon.go#L66，如下所⽰：

```autohotkey
job := eng.Job("serverapi", flHosts...)  
job.SetenvBool("Logging", true)  
job.SetenvBool("EnableCors", *flEnableCors)  
job.Setenv("Version", dockerversion.VVERSION)  
job.Setenv("SocketGroup", *flSocketGroup)  
job.SetenvBool("Tls", *flTls)  
job.SetenvBool("TlsVerify", *flTlsVerify)  
job.Setenv("TlsCa", *flCa)  
job.Setenv("TlsCert", *flCert)  
job.Setenv("TlsKey", *flKey)  
job.SetenvBool("BufferRequests", true)  
if err := jobRUN(); err != nil {  
    log.Fatal(err)  
} 
```

以上代码标志着Docker Daemon真正进⼊状态。实现过程中，Docker ⾸ 先 创 建 ⼀ 个 名 为 serveapi 的 Job ， 并 将 flHosts 的 值 赋 给job.Args。flHosts的作⽤主要是：为Docker Daemon提供使⽤的协议与监听的地址。随后，Docker Daemon为该Job设置了众多的环境变量，如安全传输层协议的环境变量等。最后通过job.Run（）运⾏该serveapi的Job。

由于在eng中key为serveapi的handler，value为apiserver.ServeApi，故 该 Job 运 ⾏ 时 ， 执 ⾏ apiserver.ServeApi 函 数 ， 位于./docker/docker/api/server/server.go。ServeApi函数的作⽤是：对于所

有⽤户定义⽀持协议，Docker Daemon均创建⼀个goroutine来启动相应的http.Server，并为每⼀种协议服务。

由于创建并启动http.Server为Docker架构中有关Docker Server的重要内容，本书将在第5章深⼊介绍Docker Server。

⾄此，我们可以认为Docker Daemon已经完成了serveapi这个Job的初始化⼯作。⼀旦acceptconnections这个Job运⾏完毕，Docker Daemon则会通知init进程Docker Daemon启动完毕，可以开始提供API服务，两个Job通过activationLock进⾏同步。

# 3.4 总结

本章从源码的⾓度分析了Docker Daemon的启动，着重分析了mainDaemon（）的实现。

Docker Daemon作为Docker架构中的主⼲部分，负责了Docker内部⼏乎所有操作的管理。学习Docker Daemon的具体实现，可以对Docker架构有⼀个较为全⾯的认识。总结⽽⾔，Docker的运⾏载体为Daemon，调度管理由Engine负责，任务执⾏靠Job。

# 第4章 Docker Daemon之NewDaemon实现

# 4.1 引⾔

Docker的⽣态系统⽇趋完善，开发者群体也在⽇趋庞⼤，这些现象都使得⼯业界对Docker持续抱有乐观的态度。如今，对于⼴⼤开发者⽽⾔，使⽤Docker已然不是门槛，享受Docker带来的福利也不再困难。然⽽，如何探寻Docker适应的场景，如何发展Docker周边的技术，以及如何弥合Docker新技术与传统物理机或虚拟机技术之间的鸿沟，已经成为Docker研究者们要思考与解决的问题。

在Docker架构中Docker Daemon⽀撑着整个后台进程的运⾏，同时也 统 ⼀ 化 管 理 着 Docker 架 构 中 graph 、 graphdriver 、 execdriver 、volumes、Docke容器等众多资源。可以说，Docker Daemon复杂的运作均由daemon对象来调度，⽽NewDaemon的实现恰巧可以帮助⼤家了解这⼀切的来⻰去脉。通过本章内容的介绍，我们⼒求帮助⼴⼤Docker爱好者更多地理解Docker的核⼼——Docker Daemon的实现。

本章从源码⾓度，分析Docker Daemon加载过程中NewDaemon的实现，整个分析过程如图4-1所⽰。

由图4-1可⻅，Docker Daemon中NewDaemon的执⾏流程主要包含12个独⽴的步骤：处理配置信息、检测系统⽀持及⽤户权限、配置⼯作路径、加载并配置graphdriver、创建Docker Daemon⽹络环境、创建并初始化graphdb、创建execdriver、创建daemon实例、检测DNS配置、加载已有容器、设置shutdown处理⽅法，以及返回daemon实例。

下⾯我们将在NewDaemon的具体实现中，详细分析以上内容。

![](images/2fb2b583b9d5945a0ccbb60bb13f9146e673a362bd05bcc356ac46b2c2b64190.jpg)  
图4-1 Docker Daemon中NewDaemon执⾏流程图

# 4.2 NewDaemon具体实现

本书第3章对于Docker Daemon启动的分析过程，有这样⼀个环节：使⽤协程加载daemon对象。在加载并运⾏daemon对象时，所做的第⼀个⼯作是：

```go
d, err := daemon.New Daemon(daemoncfg, eng) 
```

简单分析⼀下这⾏代码。

·函数名：NewDaemon

·调⽤此函数的包名：daemon

·函数具体实现源⽂件：./docker/docker/daemon/daemon.go

·函数传⼊实参：daemonCfg，定义Docker Daemon运⾏过程中所需的众多配置信息；eng，在mainDaemon中创建的Engine对象实例

·函数返回内容：d，具体的Daemon对象实例；err，错误状态

进⼊./docker/docker/daemon/daemon.go中，寻找函数NewDaemon的具体实现，源码如下：

```go
func New Daemon(config *Config, eng *engine. Engine) (* Daemon, error) {
    daemon, err := New DaemonFromDirectory(config, eng)
    if err != nil {
        return nil, err
    }
    return daemon, nil
} 
```

可 ⻅ ， 在 实 现 NewDaemon 的 过 程 中 ， 要 通 过NewDaemonFromDirectory函数来实现创建Daemon的运⾏环境。该函数的实现，传⼊参数以及返回类型与NewDaemon函数完全相同。接下来将详细分析NewDaemonFromDirectory的实现细节。

# 4.3 应⽤配置信息

NewDaemonFromDirectory的实现过程中，第⼀个⼯作是：如何应⽤传⼊的配置信息。这部分配置信息服务于Docker Daemon的运⾏，并在Docker Daemon启动初期初始化完毕。配置信息的主要功能是：供⽤户⾃由配置Docker的可选功能，使得Docker的运⾏更贴近⽤户期待的运⾏场景。

配置信息的处理包含4部分：

·配置Docker容器的MTU   
·检测⽹桥配置信息   
·查验容器间的通信配置   
·处理PID⽂件配置

下⾯逐⼀分析配置信息的处理。

# 4.3.1 配置Docker容器的MTU

config信息中的Mtu属性应⽤于容器⽹络接⼝的最⼤传输单元（MTU）特性。有关MTU的源码如下：

if config.Mtu $= = 0$ { config.Mtu $\equiv$ GetDefaultNetworkMtu()   
1

可 ⻅ ， 若 config 信 息 中 Mtu 的 值 为 0 ， Docker 则 通 过GetDefaultNetworkMtu函数将Mtu设定为默认的值；否则，采⽤config中的Mtu值。由于在默认的配置⽂件./docker/docker/daemon/config.go（下⽂ 简 称 为 默 认 配 置 ⽂ 件 ） 中 ， Mtu 属 性 的 初 始 值 为 0 ， 故 执 ⾏GetDefaultNetworkMtu。

GetDefaultNetworkMtu 函 数 的 具 体 实 现 位于./docker/daemon/config.go#L65-L70，如下：

```go
func GetDefaultNetworkMtu() int {
    if iface, err := networkdriver.GetDefaultRouteIface(); err == nil {
        return iface.MTU
    }
    return defaultNetworkMtu
} 
```

在GetDefaultNetworkMtu的实现中，Docker通过networkdriver包的GetDefaultRouteIface⽅法获取具体的⽹络接⼝，若该⽹络接⼝存在，则返 回 该 ⽹ 络 接 ⼝ 的 MTU 属 性 值 ； 否 则 返 回 默 认 的 MTU 值defaultNetworkMtu，值为1500。

# 4.3.2 检测⽹桥配置信息

处理完config中的Mtu属性之后，Docker⻢上检测config信息中BridgeIface和BridgeIP这两个属性。BridgeIface和BridgeIP的作⽤是为创建⽹桥的任务init_networkdriver提供参数，源码如下：

```go
if config.BridgeIface != "" && config.BridgeIP != "" { return nil, fmt.Errorf("You specified -b & --bip, mutually exclusive options. Please specify only one.") } 
```

以上代码的含义为：若config中BridgeIface和BridgeIP两个属性均不为空，则返回nil对象，并返回错误信息，错误信息内容为：⽤户同时指定了BridgeIface和BridgeIP，这两个属性属于互斥类型，只能指定其中之⼀。原因是：当⽤户为Docker⽹桥选定已经存在的⽹桥接⼝时，应该沿⽤已有⽹桥的当前IP地址，不应再提供IP地址；当⽤户不选已经存在的⽹桥接⼝作为Docker⽹桥时，Docker会另⾏创建⼀个全新的⽹桥接⼝作为Docker⽹桥，此时⽤户可以为此新创建的⽹桥接⼝设定⾃定义IP地址；当然两者都不选的话，Docker会为⽤户接管完整的Docker⽹桥创建流程，从创建默认的⽹桥接⼝，到尾⽹桥接⼝设置默认的IP地址。⽽在默认配置⽂件中，BridgeIface和BridgeIP的值均为空字符串。

# 4.3.3 查验容器间的通信配置

查验容器间的通信配置，主要是针对config信息中的EnableIptables和Inter-Container Communication属性。

EnableIptables属性主要来源于flag参数--iptables，它的作⽤是：在DockerDaemon 启 动 时 ， 是 否 对 宿 主 机 的 iptables 规 则 作 修 改 。 若EnableIptables的值为false，则代表Docker Daemon启动时不对宿主机的iptables规则作任何修改；若EnableIptables的值为true，则代表DockerDaemon启动时对宿主机的iptables规则作修改。

仅仅分析EnableIptables的作⽤，显得有些抽象，理解起来也较为⽣涩。结合InterContainerCommunication来分析，就会变得⾃然很多。InterContainerCommunication属性来源于flag参数--icc，它的作⽤是：在Docker Daemon启动时，是否开启Docker容器之间互相通信的功能。若InterContainerCommunication的值为false，则Docker Daemon会在宿主机iptables的FORWARD链中添加⼀条Docker容器间流量均DROP的规则；若InterContainerCommunication 为true，则Docker Daemon会在宿主机iptables的FORWARD链中添加⼀条Docker容器间流量均ACCEPT的规则。

通 过 分 析 以 上 的 内 容 ， 我 们 可 以 发 现 ：InterContainerCommunication的值不论为false还是为true，都会在DockerDaemon启动时，动⽤iptables，故EnableIptables的值只能为true，必须允许Docker Daemon启动时对iptables规则作修改的操作。另外，若EnableIptables为true，则说明Docker Daemon启动时允许对iptables规则作修改，⽽此时若InterContainerCommunication为false，则说明DockerDaemon添加⼀条DROP的规则，导致Docker容器间不能互相通信，在此情况下，Docker提供容器间link的机制，仍然可以帮助容器实现互相通信。

# 查验容器间通信配置的源码如下：

```txt
if !config.EnableIptables && !config.InterContainerCommunication{ return nil, fmt.Errorf("You specified --iptables=false with --icc=false. ICC uses iptables to function. Please set --icc or --iptables to true.") } 
```

代码含义为：若EnableIptables和InterContainerCommunication两个属性的值均为false，则返回nil对象以及错误信息。其中错误信息为：⽤户将以上两属性均置为false，容器间通信需要iptables的⽀持，需设置其中之⼀为true。⽽在默认配置⽂件中，这两个属性的值均为true。

# 4.3.4 处理⽹络功能配置

接着，Docker处理config中的DisableNetwork属性，后续创建并执⾏ 创 建 Docker Daemon ⽹ 络 环 境 时 会 使 ⽤ 此 属 性 ， 即 在 名 为init_networkdriver的Job创建并运⾏中体现。

```go
config.DisableNetwork = config.BridgeIface == DisableNetworkBridge 
```

由 于 config 信 息 中 的 BridgeIface 属 性 值 为 空 ， 另 外DisableNetworkBridge 的 值 为 字 符 串 none ， 因 此 最 终 config 中DisableNetwork的值为false。后续名为init_networkdriver的Job执⾏时，需要使⽤DisableNetwork这个属性。

# 4.3.5 处理PID⽂件配置

处理PID⽂件配置，主要⼯作是：为运⾏时Docker Daemon进程的PID号创建⼀个PID⽂件，⽂件的路径即为config中的Pidfile属性，并且为Docker Daemon的shutdown操作添加⼀个删除此Pidfile的函数，以便在Docker Daemon退出的时候，可以在第⼀时间删除Pidfile。实现处理PID⽂件配置信息的源码如下：

```go
if config.Pidfile != "" { if err := utils.CreatePidFile(config.Pidfile); err != nil { return nil, err } eng.OnShutdown(func){ utils.RemovePidFile(config.Pidfile) }   
}
```

在代码执⾏过程中，⾸先检测config中的Pidfile属性是否为空，若为空，则跳过代码块继续执⾏；若不为空，则⾸先在⽂件系统中创建具体的Pidfile，然后向eng的onShutdown属性添加⼀个处理函数，函数具体完成的⼯作为utils.RemovePidFile（config.Pidfile），即在DockerDaemon进⾏shutdown操作的时候，删除Pidfile⽂件。在默认配置⽂件中，Pidfile⽂件的初始值为"/var/run/docker.pid"。

以上便是关于NewDaemon实现过程中配置信息的处理分析。

# 4.4 检测系统⽀持及⽤户权限

初步处理完Docker的配置信息之后，Docker⽴即对⾃⾝的运⾏环境进⾏⼀系列的检测。检测主要包括以下三⽅⾯：

·操作系统类型对Docker Daemon的⽀持；  
·⽤户权限的级别；  
·内核版本与处理器的⽀持。

系统⽀持与⽤户权限检测的实现较为简单，源码实现如下：

```go
if runtime.G00S != "linux" {
    log.Fatal("The Docker daemon is only supported on linux")
}
if os.Geteuid() != 0 {
    log.Fatal("The Docker daemon needs to be run as root")
}
if err := checkKernelAndArch(); err != nil {
    log.Fatal(err.Error())
} 
```

⾸先，通过runtime.GOOS检测操作系统的类型。runtime.GOOS返回运⾏程序所在操作系统的类型，可以是Linux、Darwin、FreeBSD等。结合具体代码，可以发现，若操作系统不为Linux，将报出Fatal错误⽇志，内容为“Docker Daemon只能⽀持Linux操作系统”。

接着，通过os.Geteuid（），检测程序⽤户是否拥有⾜够权限。os.Geteuid（）返回调⽤者所在组的组id。结合具体源码分析可知，若返回不为0，则说明docker程序不是以root⽤户的⾝份运⾏，报出Fatal错误⽇志。

最后，通过checkKernelAndArch（），检测内核的版本以及主机处理 器 类 型 。 checkKernel-AndArch （ ） 的 实 现 同 样 位于./docker/docker/daemon/daemon.go#L1097-L1119。实现过程中，第⼀个⼯作是：检测程序运⾏所在的处理器架构是否为“amd64”，⽽⽬前Docker运⾏时只能⽀持amd64的处理器架构。第⼆个⼯作是：检测Linux内核版本是否满⾜要求，⽽⽬前Docker Daemon运⾏所需的内核版本若过低，则很有可能出现不稳定的状况，因此Docker官⽅建议⽤户升级内核版本⾄3.8.0或以上版本（包括3.8.0）。

# 4.5 配置⼯作路径

配置Docker Daemon的⼯作路径，主要是创建Docker Daemon运⾏中所在的⼯作⽬录。实现过程中，通过config中的Root属性来完成。Docker Daemon的root⽬录作⽤⾮常⼤，⼏乎涵盖Docker在宿主机上运⾏的所有信息，包括：所有的Docker镜像内容、所有Docker容器的⽂件系统、所有Docker容器的元数据、所有容器的数据卷内容等。

在默认配置⽂件中，Root属性的值为”/var/lib/docker”。

在配置⼯作路径的代码实现中，步骤如下：

1）使⽤规范路径创建⼀个TempDir，路径名为tmp。  
2）通过tmp，创建⼀个指向tmp的⽂件符号连接realTmp。  
3）使⽤realTemp的值，创建并赋值给环境变量TMPDIR。  
4）处理config的属性EnableSelinuxSupport。  
5）将realRoot重新赋值于config.Root，并创建Docker Daemon的⼯作根⽬录。

# 4.6 加载并配置graphdriver

加载并配置存储驱动graphdriver，⽬的在于：使得Docker Daemon创建Docker镜像管理所需的驱动环境。graphdriver⽤于完成Docker镜像的管理，包括获取、存储以及容器rootfs的构建等。

# 4.6.1 创建graphdriver

创 建 graphdriver 的 内 容 源 码 位于./docker/docker/daemon/daemon.go#L743-L790。具体细节分析如下：

```go
graphdriver.DefaultDriver = config.GraphDriver  
driver, err := graphdriver.New(config_ROOT, config.GraphOptions) 
```

⾸先，Docker对graphdriver包中的DefaultDriver对象赋值，值为config中的GraphDriver属性，在默认配置⽂件中，GraphDriver属性的值为空；同样的，属性GraphOptions也为空。然后通过GraphDriver中的new函数实现加载graph的存储驱动。

创建具体的graphdriver是极其重要的⼀个环节，实现细节由graphdriver 包 中 的 New 函 数 来 完 成 。 New 函 数 的 实 现 位于 ./docker/docker/daemon/graphdriver/driver.go#L81-L111 ， 实 现 步 骤 如下：

第 ⼀ ， 遍 历 数 组 选 择 graphdriver ， 数 组 内 容 为 os.Getenv（"DOCKER_DRIVER"）和DefaultDriver。若数组不为空，graphdriver则通过GetDriver函数直接返回相应的Driver对象实例；若为空，则继续往下执⾏。这部分内容的作⽤是：让graphdriver的加载⾸先满⾜⽤户的⾃定义选择，⽤户可以通过定义环境变量的⽅式定义graphdriver的类型，其次Docker采⽤默认值，源码如下：

```txt
for _, name := range [os.Getenv("DOCKER_DRV"), DefaultDriver] {
    if name != "" {
        return GetDriver(name, root, options)
    }
} 
```

第⼆，遍历优先级数组priority选择graphdriver，优先级数组priority的 内 容 依 次 为 aufs 、 btrfs 、 devicemapper 和 vfs 。 若 遍 历 验 证 时 ，GetDriver成功，则直接返回当前的Driver对象实例；若不成功，则继续往下执⾏。这部分内容的作⽤是：在没有指定以及默认的驱动时，从优先级数组中选择驱动，⽬前优先级最⾼的为aufs，源码如下：

```go
for _, name := range priority {
    driver, err = GetDriver(name, root, options)
    if err != nil {
        if err == ErrNotSupported || err == ErrPrerequisites || err == ErrIncompatibleFS {
            continue
        }
        return nil, err
    }
    return driver, nil
} 
```

# 第三，从已经注册的drivers数组中选择graphdriver，源码如下：

```txt
for _, initFunc := range drivers {
    if driver, err = initFunc(root, options); err != nil {
        if err == ErrNotSupported || err == ErrPrerequisites || err == ErrIncompatibleFS {
            continue
        }
        return nil, err
    }
    return driver, nil
} 
```

在aufs、btrfs、devicemapper和vfs四个不同类型驱动的init函数中，它们均向graphdriver的drivers数组注册了相应的初始化⽅法。分别位于./docker/docker/daemon/graphdriver/aufs/aufs.go，以及其他三类驱动的

相应位置。这部分内容的作⽤是：在没有优先级数组的时候，同样可以通过注册的驱动来选择具体的graphdriver。

# 4.6.2 验证btrfs与SELinux的兼容性

由于⽬前在btrfs⽂件系统上运⾏的Docker不兼容SELinux，因此当config中配置信息需要启⽤SELinux的⽀持并且驱动的类型为btrfs时，返回nil对象，并报出Fatal⽇志。代码实现如下：

```go
// As Docker on btrfs and SELinux are incompatible at present, error on both being enabled if config.EnableSeleniumSupport && driver.String() == "btrfs" {
return nil, fmt.Errorf("SELinux is not supported with the BTRFS graph driver!") 
```

# 4.6.3 创建容器仓库⽬录

Docker Daemon在创建Docker容器之后，需要将容器的元数据信息放置于某个仓库⽬录下，统⼀管理。⽽这个⽬录即为daemonRepo，值为："/var/lib/docker/containers"。Docker通过daemonRepo创建对应的⽬录。源码实现如下：

```autohotkey
daemonRepo := path.Join(config_ROOT, "containers")  
if err := os.MkdirAll(daemonRepo, 0700); err != nil && !os.IsExist(err) {  
    return nil, err  
} 
```

# 4.6.4 迁移容器⾄aufs类型

Docker Daemon的启动很有可能不是第⼀次，因此Docker环境中也有可能存在⼀部分的遗留内容；Docker Daemon也有可能是版本升级后的第⼀次启动，Docker环境中同样有可能存在遗留内容。因此，当graphdriver的类型为aufs时，DockerDaemon启动需要将现有容器root⽬录下的相应内容都迁移⾄aufs类型；若不为aufs，则继续往下执⾏。

对于aufs类型的graphdriver，在Docker 0.7.x版本之前，Docker将容器镜像的镜像层内容以及镜像元数据均放在同⼀个⽬录下，迁移操作要完成将以上两者拆分存储，以满⾜新版Docker的aufs⽀持。关于Docker镜像的存储，可参⻅第10章。

# 实现源码如下：

```txt
if err = migrateIfAufs(driver, configROOT); err != nil { return nil, err } 
```

这部分的迁移内容主要包括Repositories、Images以及Containers，具 体 源 码 实 现 位于 ./docker/docker/daemon/graphdriver/aufs/migrate.go#L39-L50 ， 如 下 所⽰：

```go
func (a *Driver) Migrate(pth string, setupInit func(p string) error) error {
    if pathExists(path.Join(pth, "graph")) {
        if err := a.migrateRepositories(pth); err != nil {
            return err
        }
    if err := a.migrateImages(path.Join(pth, "graph")); err != nil {
        return err 
```

```go
} return a.migrateContainers(path.Join(pth, "containers"), setupInit) } return nil 
```

迁移镜像库的功能是：在Docker Daemon的root⼯作⽬录下创建repositories-aufs的⽂件，存储所有与镜像相关的镜像库以及镜像标签信息。

迁移Docker镜像的主要功能是：将原有的image镜像都迁移⾄aufs驱动能识别并使⽤的类型，主要是拆分镜像原先的存储⽅式，将内容迁移到aufs所规定的layers、diff与mnt⽬录。

迁移容器的主要功能是：将Docker原先的容器运⾏环境使⽤aufs驱动来进⾏迁移配置，包括创建容器的rootfs，配置容器初始层（initlayer），创建容器的读写层等。

# 4.6.5 创建镜像graph

创 建 镜 像 graph 的 主 要 ⼯ 作 是 ： 通 过 Docker 的 root ⽬ 录 以 及graphdriver实例，实例化⼀个全新的graph对象，⽤以管理在⽂件系统中Docker的root路径下graph⽬录的内容。graph⽬录下的⽂件以镜像ID为单位，分别存储单个镜像的json⽂件以及镜像的⼤⼩⽂件layersize。其中镜像的json⽂件包含镜像⾃⾝的元数据信息。实现源码如下：

```go
g, err := graph.NewGraph(path.Join(config_ROOT, "graph"), driver) 
```

NewGraph的具体实现位于./docker/docker/graph/graph.go，实现过程中返回的对象为Graph类型，定义如下：

```go
type Graph struct { Root string idIndex \*truncindex.TruncIndex driver graphdriver.Driver } 
```

其中Root表⽰graph的⼯作根⽬录，⼀般为"/var/lib/docker/graph"；idIndex使得检索字符串标识符时，允许使⽤任意⼀个该字符串唯⼀的前缀，只要该前缀全局唯⼀，则可确保找到相应的镜像。在这⾥，idIndex⽤于通过简短有效的字符串前缀检索镜像的ID；最后driver表⽰具体的graphdriver类型。

# 4.6.6 创建volumesdriver以及volumes graph

在Docker中数据卷（volume）的概念是：可以从Docker宿主机上挂载到Docker容器内部的特定⽬录。⼀个数据卷可以被多个Docker容器挂载，从⽽使Docker容器可以实现互相共享数据等。在实现数据卷时，Docker需要使⽤driver来管理它，⼜由于数据卷的管理不会像容器⽂件系统管理那么复杂，故Docker采⽤vfs驱动实现数据卷的管理。

Docker的范畴中，数据卷可以分为两种：第⼀种，⽤户使⽤dockerrun命令启动容器时传⼊-v A：B，使得宿主机上的⽬录A可以挂载到容器内部的⽬录B，；第⼆种，⽤户在使⽤Dockerfile时使⽤命令VOLUME/data，或者使⽤dockerrun命令启动容器时传⼊-v/data，⽤户虽然指定在宿主机上的⽬录，但是Docker Daemon⼀般情况下会接管宿主机上的⽬录创建，并再将⽬录挂载⾄Docker容器内部，此时宿主机上的⽬录⼀般位于"/var/lib/docker/vfs/dir/<ID>"。第⼀种数据卷通常可以将其称为bind-mount volume，⽽第⼆种通常称为data volume。

于volumes graph上创建volumesdriver的源码实现如下：

```go
volumesDriver, err := graphdriver.GetDriver("vfs", config_ROOT, config.GraphOptions)  
volumes, err := graph.NewGraph(path.Join(config_ROOT, "Volumes"), volumesDriver) 
```

主要完成⼯作为：使⽤vfs这种类型的driver创建volumesDriver；在Docker的root路径下创建volumes⽬录，并返回volumes这个graph对象实例。

# 4.6.7 创建TagStore

TagStore主要是⽤于管理存储镜像的仓库列表（repository list）。

创建tagStore的源码如下：

```go
repositories, err := graph.NewTagStore(path.Join(config_ROOT, "repositories "+"driver.String)), g) 
```

函 数 NewTagStore 的 实 现 位 于 ./docker/docker/graph/tags.go ，TagStore的定义如下：

```txt
type TagStore struct { path string graph \*Graph Repositories map[string]Repository sync.Mutex pullingPool map[string]chan struct{} pushingPool map[string]chan struct{} } 
```

需要阐述的是TagStore类型中的多个属性的含义。

·path：TagStore中记录镜像仓库的⽂件所在路径，如aufs类型的TagStore path的值为"/var/lib/docker/repositories-aufs"。

·graph：相应的Graph实例对象。

·Repositories：记录镜像仓库的映射数据结构。

·sync.Mutex：TagStore的互斥锁。

·pullingPool：记录池，记录有哪些镜像正在被下载，若某⼀个镜像正在被下载，则驳回其他Docker Client发起下载该镜像的请求。

·pushingPool：记录池，记录有哪些镜像正在被上传，若某⼀个镜像正在被上传，则驳回其他Docker Client发起上传该镜像的请求。

# 4.7 配置Docker Daemon⽹络环境

创建Docker Daemon运⾏环境的时候，配置Docker所在宿主机的⽹络环境是极为重要的⼀个环节。这不仅关系着将来容器对外的通信，同样也关系着容器间的通信。

配置Docker宿主机的⽹络环境时，Docker Daemon通过运⾏名为init_networkdriver的Job来完成。源码实现如下：

```go
if !config.DisableNetwork {
    job := eng.Job("init_networkdriver")
    job.SetenvBool("EnableIptables", config.EnableIptables)
    job.SetenvBool("InterContainerCommunication", config.InterContainerCommunication)
    job.SetenvBool("EnableIpForward", config EnableIpForward)
    job.Setenv("BridgeIface", config.BridgeIface)
    job.Setenv("BridgeIP", config.BridgeIP)
    job.Setenv("DefaultBindingIP", config.DefaultIp.String())
    if err := jobRUN(); err != nil {
        return nil, err
    }
} 
```

分析以上源码可知，通过config中的DisableNetwork属性来判断是否执⾏Job。在默认配置⽂件中，该属性曾被定义，却没有初始值，然⽽在应⽤配置信息这个步骤（4.3节），Docker处理⽹络功能配置时，将DisableNetwork属性赋值为false。故以上判断语句结果为true，执⾏相应的代码块。

配置DockerDaemon⽹络环境的⼯作主要通过init_networkdriver这个Job来完成。Docker⾸先创建名为init_networkdriver的Job，随后为此Job设置环境变量，环境变量的值如下：

·环境变量EnableIptables，使⽤config.EnableIptables来赋值，默认值为true。

· 环 境 变 量 InterContainerCommunication ， 使 ⽤config.InterContainerCommunication来赋值，为默认值true。

·环境变量EnableIpForward，使⽤config.EnableIpForward来赋值，默认值为true。

·环境变量BridgeIface，使⽤config.BridgeIface来赋值，为空字符串""。

·环境变量BridgeIP，使⽤config.BridgeIP来赋值，为空字符串""。

·环境变量DefaultBindingIP，使⽤config.DefaultIp.String（）来赋值，默认值为"0.0.0.0"。

设置完环境变量之后，Docker随即运⾏此Job，由于在eng中key为init_networkdriver 的 handler ， value 为 bridge.InitDriver 函 数 ， 故 执 ⾏bridge.InitDriver 函 数 ， 具 体 的 实 现 位于./docker/docker/daemon/networkdriver/bridge/dirver.go，作⽤为：

·获取为Docker容器服务的⽹络接⼝IP地址。

·创建指定IP地址的⽹桥接⼝。

·启⽤Iptables功能并进⾏配置。

· 另 外 ， Job 为 eng 实 例 注 册 了 4 个 Handler ， Handler 名 分 别 为 ：allocate_interface、release_interface、allocate_port和link。

Docker Daemon的⽹络初始化关乎Docker容器的通信能⼒，是Docker架构中最为基础的知识之⼀。第6章将为⼤家分析Docker

Daemon⽹络环境的创建。

# 4.7.1 创建Docker⽹络设备

创建Docker⽹络设备，属于Docker Daemon创建⽹络环境的第⼀步，实际⼯作是创建名为docker0的⽹桥设备。

在InitDriver函数运⾏过程中，Docker⾸先使⽤Job的环境变量初始化内部变量；然后根据⽬前⽹络环境，判断是否创建docker0⽹桥，若Docker专属⽹桥已存在，则继续往下执⾏；否则，创建docker0⽹桥。具 体 实 现 为 createBridge （ bridgeIP ） ， 以 及 createBridgeIface（bridgeIface）。

createBridge的功能是：在宿主机上启动创建指定名称⽹桥设备的任务，并为该⽹桥设备配置⼀个与其他设备不冲突的⽹络地址。⽽createBridgeIface通过系统调⽤负责创建具体实际的⽹桥设备，并设置MAC地址，通过libcontainer中netlink包的CreateBridge来实现。

# 4.7.2 启⽤iptables功能

创建完⽹桥之后，Docker Daemon为未来的Docker容器以及宿主机配置iptables规则，作⽤是：为Docker容器之间的link操作提供iptables防⽕ 墙 ⽀ 持 。 源 码 位于 ./docker/docker/daemon/networkdriver/bridge/driver/driver.go#L133-L137，如下所⽰：

```go
// Configure iptables for link support  
if enableIPTables {  
    if err := setupIPTables(addr, ipp); err != nil {  
        return job.Error(err)  
    }  
} 
```

其中setupIPtables的调⽤过程中，addr地址为Docker⽹桥的⽹络地址，icc为true，即允许Docker容器间互相访问。假设⽹桥设备名为docker0，⽹桥⽹络地址为docker0_ip，设置iptables规则，具体操作步骤如下：

1）使⽤iptables⼯具开启新建⽹桥的NAT功能，使⽤如下命令：

iptables -I POSTROUTING -t nat -s docker $_0\_ip ! -o docker$ -j MASQUERADE

2）通过icc参数，决定是否允许Docker容器间的通信，并制定相应iptables的Forward链。Docker容器之间建⽴通信，说明数据包从源容器内发出后，经过docker0，并且还需要在docker0处发往docker0，最终转向⽬标容器。换⾔之，从docker0出来的数据包，如果需要继续发往docker0，则说明是Docker容器间的通信数据包。使⽤命令如下：

3）允许接受从容器发出，且⽬标地址不是容器的数据包。换⾔之，允许所有从docker0发出且不是继续发向docker0的数据包，使⽤命令如下：

iptables -I FORWARD -i docker0 ! -o docker0 -j ACCEPT

4）对于发往docker0，并且属于已经建⽴的连接的数据包，Docker⽆条件接受这些连接上的数据包，使⽤命令如下：

iptables -I FORWARD -o docker0 -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT

# 4.7.3 启⽤系统数据包转发功能

在Linux系统上，⽹络设备之间的数据包转发功能是默认禁⽌的。数据包转发，即当宿主机存在多块⽹络设备时，如果其中⼀块⽹络设备接收到数据包，则⽆条件将其转发给另外的⽹络设备。通过修改/proc/sys/net/ipv4/ip_forward的值，将其置为1，则可以保证系统内数据包可以实现转发功能，代码如下：

```go
if ipForward{ // Enable IPv4 forwarding if err := ioutil.WriteFile("/proc/sys/net/ipv4/ip_forward", [],byte{'1', '\n'}, 0644); err != nil { job.Logf("WARNING: unable to enable IPv4 forwarding: %s\n", err) } } 
```

# 4.7.4 创建DOCKER链

Docker在⽹桥设备上创建⼀条名为DOCKER的链，该链的作⽤是在创建Docker容器时实现容器与宿主机的端⼝映射。实现代码位于 ./docker/docker/daemon/networkdriver/bridge/driver/driver.go ， 如 下 所⽰：

```go
if err := iptables.RemoveExistingChain("DOCKER"); err != nil { return job.Error(err)   
}   
if enableIPTables { chain, err := iptables.NewChain("DOCKER", bridgeIface) if err != nil { return job.Error(err) } portmapper.SetIptablesChain(chain) 
```

# 4.7.5 注册处理⽅法⾄Engine

创建完⽹桥，并配置完基本的iptables规则之后，Docker Daemon在⽹络⽅⾯还向Engine中注册了4个处理⽅法，这些处理⽅法的名称与作⽤如下：

·allocate_interface：为Docker容器分配专属⽹络接⼝，分配容器⽹段的IP地址；

·realease_interface：释放Docker容器占⽤的⽹络接⼝资源；

·allocate_port：为Docker容器分配⼀个端⼝；

·link：实现Docker容器间的连接操作。

# 4.8 创建graphdb并初始化

graphdb是⼀个构建在SQLite之上的图形数据库，通常⽤来记录节点命名以及节点之间的关联。在Docker的世界中，⽤户可以通过link操作，使得Docker容器之间建⽴⼀种关联，⽽Docker Daemon正是使⽤graphdb来记录这种容器间的关联信息。Docker创建graphdb的源码如下：

```txt
graphdbPath := path.Join(config_ROOT, "linkgraph.db")  
graph, err := graphdb.NewSqliteConn.graphdbPath)  
if err != nil {  
    return nil, err  
} 
```

以上代码⾸先确定graphdb的⽬录为/var/lib/docker/linkgraph.db；随后 通 过 graphdb 包 内 的 NewSqliteConn 打 开 graphdb ， 使 ⽤ 的 驱 动 为sqlite3 ， 数 据 源 的 名 称 为 /var/lib/docker/linkgraph.db； 最 后 通 过NewDatabase函数初始化整个graphdb，为graphdb创建entity表和edge表，并在这两个表中初始化部分数据。NewSqliteConn函数的实现位于./docker/docker/pkg/graphdb/conn_sqlite3.go，代码实现如下：

```solidity
func NewSqliteConn(root string) (*Database, error) {
    ...
    conn, err := sql.Open("sqlite3", root)
    ...
    return NewDatabase(conn, initDatabase)
} 
```

# 4.9 创建execdriver

execdriver是Docker中⽤来执⾏Docker容器任务的驱动。创建并初始化graphdb之后，Docker Daemon随即开始创建了execdriver，具体源码如下：

ed, err := execdrivers.NewDriver(config.ExecDriver, config.Root, sysInitPath, sysInfo)

分析以上源码可知，DockerDaemon创建execdriver时，需要以下四部分信息。

·config.ExecDriver：Docker运⾏时中⽤户指定使⽤的execdriver类型，在默认配置⽂件中值为native。⽤户也可以在启动DockerDaemon将这个值配置为lxc，则导致Docker使⽤lxc类型的驱动执⾏Docker容器的内部操作。

·config.Root ： Docker 运 ⾏ 时 的 root 路 径 ， 默 认 配 置 ⽂ 件 中为/var/lib/docker。

·sysInitPath ： 系 统 中 存 放 dockerinit ⼆ 进 制 ⽂ 件 的 路 径 ， ⼀ 般为/var/lib/docker/init/dockerinit-1.2.0。

·sysInfo：系统功能信息，包括：容器的内存限制功能，交换区内存限制功能，数据转发功能，以及AppArmor安全功能等。

在执⾏execdrivers.NewDriver之前，⾸先通过以下代码，获取期望的⽬标dockerinit⽂件的路径localPath，以及系统中dockerinit⽂件实际所在的路径sysInitPath：

通 过 执 ⾏ 以 上 代 码 ， localCopy 为 /var/lib/docker/init/dockerinit-1.2.0，⽽sysyInitPath为当前Docker运⾏时中dockerinit-1.2.0实际所处的路 径 ， utils.DockerInitPath 实 现 位 于 ./docker/docker/utils/util.go 。 若localCopy与sysyInitPath不相等，则说明当前系统中的dockerinit⼆进制⽂件，不在localCopy路径下，则需要将其复制⾄localCopy下，并对该⽂件设定权限。

设定完dockerinit ⼆进制⽂件 的 位 置之 后，Docker Daemon 创建sysinfo 对 象 ， 记 录 系 统 的 功 能 属 性 。 SysInfo 的 定 义 位于./docker/docker/pkg/sysinfo/sysinfo.go，如下所⽰：

```txt
type SysInfo struct { MemoryLimit bool SwapLimit bool IPv4ForwardingDisabled bool AppArmor bool } 
```

其中Docker Daemon通过判断cgroups⽂件系统挂载路径下是否均存在 memory.limit_in_bytes 和 memory.soft_limit_in_bytes ⽂ 件 来 为MemoryLimit赋值，若均存在，则置为true，否则置为false；通过判断memory.memsw.limit_in_bytes⽂件是否存在来为SwapLimit赋值，若该⽂件存在，则置为true，否则置为false。AppArmor的值则是通过宿主机上是否存在/sys/kernel/security/apparmor来判断，若存在，则置为true，否则置为false。

执⾏execdrivers.NewDriver时，返回execdriver.Driver对象实例，具体 代 码 实 现 位于 ./docker/docker/daemon/execdriver/execdrivers/execdrivers.go ， 由 于 选择使⽤native作为exec驱动，故执⾏以下代码，返回最终的execdriver，

其 中 native.NewDriver 实 现 位

于./docker/docker/daemon/execdriver/native/driver.go：

return native.NewDriver(path.Join(root, "execdriver", "native"), initPath)

⾄此，⼀个execdriver的实例ed被Docker成功创建。

# 4.10 创建daemon实例

Docker Daemon在经过以上多个环节的设置之后，整合众多已经创建的对象，创建最终的Daemon对象实例daemon。Daemon对象实例daemon涉及的内容极多，⽐如：对于Docker镜像的存储可以通过graph来管理、所有Docker容器的元数据信息都保存在containers对象中、整个Docker Daemon的任务执⾏位于eng属性中，等等。

创建daemon实例的源码实现如下：  
daemon $\coloneqq$ & Daemon{ repository: daemonRepo, containers: &contStore{s: make(map[string]*Container)}, graph: g, repositories: repositories, index: truncindex.NewTruncIndex([]string{}), sysInfo: sysInfo, volumes: volumes, config: config, containerGraph: graph, driver: driver, sysInitPath: sysInitPath, execDriver: ed, eng: eng, }

分析Daemon类型的属性如表4-1所⽰。

表4-1 Daemon类型属性分析表  

<table><tr><td>属性名</td><td>作用</td></tr><tr><td>repository</td><td>存储所有 Docker 容器信息的路径，默认为 /var/lib.docker/container</td></tr><tr><td>containers</td><td>用于存储 Docker 容器信息的对象</td></tr><tr><td>graph</td><td>存储Docker镜像的graph对象</td></tr><tr><td>repositories</td><td>存储本机所有Docker镜像repo信息的对象</td></tr><tr><td>idIndex</td><td>用于通过简短有效的字符串前缀定位唯一的镜像</td></tr><tr><td>sysInfo</td><td>系统功能信息</td></tr><tr><td>volumes</td><td>管理宿主机上 volumes内容的 graphdriver，默认为vfs类型</td></tr><tr><td>config</td><td>Config.go文件中的配置信息，以及执行后产生的配置 DisableNetwork</td></tr><tr><td>containerGraph</td><td>存放Docker镜像关系的 graphdb</td></tr><tr><td>driver</td><td>管理Docker镜像的驱动 graphdriver，默认为 aufs类型</td></tr><tr><td>sysInitPath</td><td>系统 dockerinit二进制文件所在的路径</td></tr><tr><td>execDriver</td><td>Docker Daemon的 exec驱动，默认为 native类型</td></tr><tr><td>eng</td><td>Docker的执行引擎 Engine类型</td></tr></table>

# 4.11 检测DNS配置

创 建 完 Daemon 类 型 实 例 daemon 之 后 ， Docker Daemon 使 ⽤daemon.checkLocaldns （ ） 检 测 Docker 运 ⾏ 环 境 中 DNS 的 配 置 ，checkLocaldns函数的定义位于./docker/docker/daemon/daemon.go#L854-L856，代码如下：

```txt
func (daemon * Daemon) checkLocaldns() error {
    resolveConf, err := resolveconf.Get()
    if err != nil {
        return err
    }
    if len(daemon.config.Dns) == 0 && utils/resolv.conf {
        log.Info("Local (127.0.0.1) DNS resolver found in resolve.conf and containers can't use it. Using default external servers: %v", DefaultDNS)
        daemon.config.Dns = DefaultDNS
    }
    return nil
} 
```

以 上 代 码 ⾸ 先 通 过 resolvconf.Get （ ） ⽅ 法 获 取 宿 主机/etc/resolv.conf中的DNS服务器信息。若宿主机上DNS⽂件resolv.conf中有127.0.0.1，⽽Docker容器在⾃⾝内部不能使⽤该地址，故采⽤默认外在DNS服务器，为8.8.8.8，8.8.4.4；若宿主机上的resolv.conf有Docker容器可以使⽤的DNS服务器地址，则Docker Daemon采⽤该地址。最终Docker Daemon将DNS服务器地址赋值给config⽂件中的Dns属性。⽤户通过Docker Daemon创建Docker容器时，若不指定DNS服务器地址，则Docker Daemon将会使⽤daemon.Config.Dns作为容器内部的DNS服务器地址。

# 4.12 启动时加载已有Docker容器

在Docker Daemon重启时，很有可能之前有遗留的Docker容器。对于这部分容器，DockerDaemon重启前，⼀直将元数据信息存储在daemon.repository（⽬录为/var/lib/docker/containers）中。为了保证重启之后Docker容器的信息不丢失，DockerDaemon⾸先会进⼊该⽬录去查看，是否存在遗留的Docker容器。若存在，则Docker Daemon加载这部分容器，将容器信息收集，并做相应的维护。

Docker Daemon 加 载 Docker 容 器 的 源 码 实 现 位于./docker/docker/daemon/daemon.go#L854-L856，如下：

```autohotkey
if err := daemonrestore(); err != nil {
return nil, err
} 
```

需要注意的是：由于Docker Daemon的重启不会重启所有重启前运⾏的容器，故Docker Daemon加载已有容器时，会判断容器之前的状态是否为运⾏，若是的话，会将该容器的状态置为退出，并在内存中的容器对象以及config.json⽂件中将容器主进程的PID设为0。

# 4.13 设置shutdown的处理⽅法

加 载 完 已 有 Docker 容 器 之 后 ， Docker Daemon 设 置 了 多 项 在shutdown操作中需要执⾏的处理⽅法。也就是说，当Docker Daemon接收到特定信号，需要执⾏shutdown操作时，先执⾏这些处理⽅法完成善后⼯作，最终再实现物理意义上的shutdown。实现源码如下：

```go
eng.OnShutdown(func){ if err := daemon.shutdown(); err != nil { log.Errorf("daemon.shutdown(): %s", err) } if err := portallocator_ReleaseAll(); err != nil { log.Errorf("portallocator_ReleaseAll(): %s", err) } if err := daemon.driver_cleanup(); err != nil { log.Errorf("daemon.driver Cleanup(): %s", err.Error()) } if err := daemon/containerGraph.Close(); err != nil { log.Errorf("daemon/containerGraph.Close(): %s", err.Error()) } } 
```

由以上源码可⻅，eng对象shutdown操作执⾏时，需要执⾏以上作为参数的func（）{……}函数。在该函数中，主要完成以下4部分操作：

·运⾏daemon对象的shutdown函数，做daemon⽅⾯的善后⼯作。  
·通过portallocator.ReleaseAll（），释放所有之前占⽤的端⼝资源。  
·通过daemon.driver.Cleanup（），通过graphdriver实现unmount所有有关镜像layer的挂载点。

·通过daemon.containerGraph.Close（）关闭graphdb的连接。

# 4.14 返回daemon对象实例

当所有的⼯作完成之后，Docker Daemon返回daemon实例，意味着NewDaemon函数执⾏完毕，程序运⾏最终返回⾄mainDaemon（）中，继续通过goroutine完成加载daemon。

# 4.15 总结

本章从源码的⾓度深度分析了Docker Daemon启动过程中daemon对象的创建与加载。在这⼀环节中涉及内容极多，本章着重归纳总结daemon实现的逻辑，⼀⼀深⼊，具体全⾯。

在Docker的架构中，Docker Daemon的内容是最为丰富全⾯的，⽽NewDaemon的实现涵盖了Docker Daemon启动过程中的⼤部分⼯作。可以认为NewDaemon是Docker Daemon实现过程中的核⼼所在。深⼊理解NewDaemon的实现，即掌握了Docker Daemon运⾏的来⻰去脉。

# 第5章 Docker Server的创建

# 5.1 引⾔

Docker架构中，Docker Server是Docker Daemon的重要组成部分。Docker Server最主要的功能是：接收⽤户通过Docker Client发送的请求，并按照相应的路由规则实现请求的路由分发，最终将请求处理后的结果返回⾄Docker Client。

同时，Docker Server具备⼗分优秀的⽤户友好性，多种通信协议的⽀持⼤⼤降低Docker⽤户使⽤Docker的门槛。除此之外，Docker Server设计实现了详尽清晰的API接⼝，以供Docker⽤户使⽤。通信安全⽅⾯，Docker Server可以提供安全传输层协议（TLS），保证DockerClient与Docker Server之间数据的加密传输。并发处理⽅⾯，DockerDaemon⼤量使⽤了Go语⾔中的协程goroutine，⼤⼤提⾼了服务端对于请求的并发处理能⼒。

本章将从源码的⾓度分析Docker Server的创建，分析内容的安排如下：

1）介绍Job“serveapi”的创建与执⾏流程，代表Docker Server的创建。  
2）深⼊分析Job“serveapi”的执⾏流程。  
3）分析Docker Server创建Listener并服务API的流程。

# 5.2 Docker Server创建流程

我们在第3章主要分析了Docker Daemon的启动，⽽在mainDaemon（）运⾏的最后环节，Docker实现了创建并运⾏名为serveapi的Job。这⼀环节的作⽤是：让Docker Daemon提供API访问服务。实质上，这正是实现了Docker架构中Docker Server的创建与运⾏。

从 流 程 的 ⾓ 度 来 说 ， Docker Server 的 创 建 与 运 ⾏ ， 代 表 了Job“serveapi”的整个⽣命周期。整个⽣命周期包括：创建Docker Server的Job，配置Job环境变量，以及触发执⾏Job。

# 5.2.1 创建名为“serveapi”的Job

Docker架构中，Job是Engine内部最基本的任务执⾏单位。创建Docker Server，服务于API请求，同样属于Docker内部的⼀项任务。因此，这⼀任务同样需要表⽰为⼀个可执⾏的Job。换⾔之，需要创建Docker Server，则必须创建⼀个相应的Job。具体的Job创建形式位于./docker/docker/docker/daemon.go#L66，如下所⽰：

```txt
job := eng.Job("serveapi", flHosts...) 
```

以上代码通过Engine实例eng创建⼀个Job类型的实例job，Job实例名为serveapi，同时⽤flHosts的值初始化job.Args。flHosts的作⽤是：配置Docker Server监听的协议与监听的地址。

需要注意的是，第3章中函数mainDaemon（）的具体实现过程中，在加载builtins环节已经向eng对象注册了键为serveapi的处理⽅法，⽽该处理⽅法的值为api.ServeApi。因此，在运⾏名为serveapi的Job时，会执⾏该Job的处理⽅法api.ServeApi。

# 5.2.2 配置Job环境变量

创建完Job实例job之后，Docker Daemon为Job实例配置环境参数。在Job的实现过程中，为Job配置参数的⽅式有两种：第⼀种，创建Job实例时，⽤指定参数直接初始化Job的Args属性；第⼆种，创建完Job后，给Job添加指定的环境变量。以下源码实现了为创建的job配置环境变量。

```txt
job.SetenvBool("Logging", true)  
job.SetenvBool("EnableCors", *flEnableCors)  
job.Setenv("Version", dockerversion.VERSION)  
job.Setenv("SocketGroup", *flSocketGroup)  
job.SetenvBool("Tls", *flTls)  
job.SetenvBool("TlsVerify", *flTlsVerify)  
job.Setenv("TlsCa", *flCa)  
job.Setenv("TlsCert", *flCert)  
job.Setenv("TlsKey", *flKey)  
job.SetenvBool("BufferRequests", true) 
```

对于以上配置环境变量的归纳总结如表5-1所⽰。

# 表5-1 Job环境变量列表

<table><tr><td>环境变量名</td><td>flag 参数</td><td>默认值</td><td>作用值</td></tr><tr><td>Logging</td><td></td><td>true</td><td>启用Docker 容器的日志输出</td></tr><tr><td>EnableCors</td><td>fEnableCors</td><td>false</td><td>在远程API 中提供CORS 头</td></tr><tr><td>Version</td><td></td><td></td><td>显示Docker 版本号</td></tr><tr><td>SocketGroup</td><td>fSocketGroup</td><td>docker</td><td>在 daemon 模式中unix domain socket 分配用户组名</td></tr><tr><td>Tls</td><td>fTls</td><td>false</td><td>使用TLS 安全传输协议</td></tr><tr><td>TlsVerify</td><td>fTlsVerify</td><td>false</td><td>使用TLS 并验证远程客户端</td></tr><tr><td>TlsCa</td><td>fCa</td><td></td><td>指定CA 文件路径</td></tr><tr><td>TlsCert</td><td>fCert</td><td></td><td>TLS 证书文件路径</td></tr><tr><td>TlsKey</td><td>fKey</td><td></td><td>TLS 密钥文件路径</td></tr><tr><td>BufferRequest</td><td></td><td>true</td><td>缓存Docker Client 请求</td></tr></table>

# 5.2.3 运⾏Job

创建完Job，配置完Job的环境变量，意味着DockerServer的创建需求已准备完毕。万事俱备，只⽋东⻛，东⻛就是触发执⾏这个Job。Docker中通过Job实例的run函数完成Job的触发执⾏。触发执⾏serveapi这个Job的具体实现源码如下：

```txt
if err := jobRUN(); err != nil { log.Fatal(err) } 
```

由于Docker已经在eng对象中注册过键为serveapi的处理⽅法，故在运⾏job的时候，执⾏这个处理⽅法的值函数，相应处理⽅法的值为api.ServeApi。⾄此，名为serveapi的Job的⽣命周期已经完备。本章余下内容将深⼊分析Job的处理⽅法，api.ServeApi的执⾏细节。

# 5.3 ServeApi运⾏流程

ServeApi属于Docker Server提供API服务的部分，本⼩节将从源码的⾓度剖析Docker Server的架构设计与实现。

作为⼀个监听请求、处理请求、响应请求的服务端，Docker Server⾸先需要明确⾃⾝可以为多少种通信协议提供服务。稍加深⼊学习Docker这个C/S模式的架构设计，就可以发现Docker Server⽀持的协议包括以下三种：TCP协议、UNIX Socket形式以及fd的形式。随后，Docker Server根据协议的不同，分别创建不同的服务端实例。最后，在不同的服务端实例中，创建相应的路由模块、监听模块，以及处理请求的处理⽅法，形成⼀个完备的服务端。

serveapi这个Job在运⾏时，将执⾏api.ServeApi函数。ServeApi的功能是：循环检查Docker Daemon当前⽀持的所有通信协议，并为每⼀种协议都创建⼀个协程goroutine，并在此协程内部配置⼀个服务于HTTP请 求 的 服 务 端 。 ServeApi 的 源 码 实 现 位于./docker/docker/api/server/server.go#L1339，如下所⽰：

```go
func ServeApi(job *engine JOB) engine.Status {
    if len(job.Args) == 0 {
        return job(Error("usage: %s PROTO://ADDR [PROTO://ADDR ...]", job.Name)
    }
    var (
        protoAddr = job.Args
        chErrors = make chan error, len(protoAddr))
    )
    activationLock = make chan struct{})
    for _, protoAddr := range protoAddr {
        protoAddrParts := strings.SplitN(protoAddr, "/", 2)
        if len(protoAddrParts) != 2 {
            return job.Error("usage: %s PROTO://ADDR [PROTO://ADDR ..." , job.Name) 
```

```go
}   
go func() { log.Info("Listening for HTTP on %s (%s)", protoAddrParts[0], protoAddrParts[1]) chErrors<- ListenAndServeProtoAddrParts[0], protoAddrParts[1], job) }   
}   
for i := 0; i < lenProtoAddr); i += 1 { err :=<-chErrors if err != nil { return job.Error(err) }   
}   
return engine.StatusOK 
```

分析以上源码，通过模块化的划分，我们可以发现ServeApi的执⾏流程主要分为以下4个步骤：

1）检验Job的参数，确保传⼊参数⽆误。  
2）定义Docker Server的监听协议与地址，以及错误信息管道channel。  
3）遍历协议地址，针对协议创建相应的服务端。  
4）通过chErrors建⽴goroutine与主进程之间的协调关系。

下⾯详细分析以上4个步骤：

第⼀，Docker Daemon判断Job的参数，保证传⼊的Job参数⽆误。DockerDaemon判断的依据来源于job.Args的⻓度。由于Docker创建serveapi这个Job时，通过flHosts来初始化job.Args，故job.Args为相当于数组flHost，若flHost的⻓度为0，则说明Docker Server没有监听的协议与地址，参数有误，返回错误信息。源码如下：

```txt
if len(job.Args) == 0 {
return job(Error("usage: %s PROTO://ADDR [PROTO://ADDR ...]", job.Name)
} 
```

第⼆，定义protoAddrs、chErrors与activationLock三个变量，分别代表Docker Server监听的协议与地址，以及Job间的同步channel。

protoAddrs代表flHosts的内容；⽽chError定义了和protoAddrs⻓度⼀致的错误类型管道，chError的作⽤会在下⽂中说明。同时定义的变量activationLock，是⽤以同步serveapi和acceptconnections这两个Job执⾏ 的 管 道 。 serveapi 运 ⾏ 时 ， ServeFd 和 ListenAndServe 函 数 均 由 于activationLock中没有内容⽽阻塞，⽽当运⾏acceptionconnections这个Job时，该Job会⾸先通知init进程Docker Daemon已经启动完毕，并关闭activationLock ， 因 此 ServeFd 以 及 ListenAndServe 不 再 阻 塞 ， 结 果 是serveapi继续执⾏。正是由于activationLock的存在，Docker Daemon可以保证acceptconnections这个Job的运⾏有能⼒通知serveapi开启正式服务于API请求的功能。源码如下：

```txt
var (
    protoAddr = job_args
    chErrors = make chan error, len (protoAddr))
) 
```

第三，遍历协议地址，针对协议创建相应的服务端。协议地址即protoAddrs，也就是job.Args。DockerDaemon将protoAddrs的每⼀元素都按照字符串“：//”进⾏分割，若分割后protoAddrParts的⻓度不为2，则 说 明 协 议 地 址 的 书 写 形 式 有 误 ， 返 回 Job 错 误 ； 若 分 割 后protoAddrParts 的 ⻓ 度 为 2 ， 则 说 明 地 址 协 议 符 合 标 准 ， 获 取protoAddrParts中的协议protoAddrParts[0]与地址protoAddrParts[1]。最后，针对每⼀次循环中获得的协议与地址，Docker Daemon均创建⼀个

goroutine来执⾏ListenAndServe的操作。goroutine的运⾏主要依赖于ListenAndServe（protoAddrParts[0]，protoAddrParts[1]，job）的运⾏结果。若ListenAndServe返回错误，则chErrors中有错误，当前协程执⾏完毕；若没有返回错误，则该协程持续运⾏，持续提供服务。其中最为重要的是ListenAndServe的实现，该函数具体实现了Docker Daemon如何创建listener、router以及server，并协调三者进⾏⼯作，最终服务于API请求。步骤三的源码实现如下：

```go
for _, protoAddr := range protoAddr {
    protoAddrParts := strings.SplitN.proto, "/", 2)
    if len.protoParts) != 2 {
        return job(Error("usage: %s PROTO://ADDR [PROTO://ADDR ..." ], job.Name)
    }
    go func() {
        log.Info("Listening for HTTP on %s (%s)", protoAddrParts[0], protoAddrParts[1])
        chErrors <- ListenAndServe.protoParts[0], protoAddrParts[1], job)
    }(   ) 
```

第四，根据chErrors的值运⾏，若chErrors这个管道中有错误内容，则ServeApi的⼀次循环结束；若⽆错误内容，则循环被阻塞。chErrors这个管道的作⽤是：确保ListenAndServe所对应的协程能和主函数ServeApi进⾏协调，如果协程运⾏出错，主函数ServeApi仍然可以捕获这样的错误，从⽽导致程序的退出。实现源码如下：

```txt
for i := 0; i < len.protoAddr); i += 1 {
    err := <-chErrors
    if err != nil {
        return job.Error(err)
    }
} 
```

⾄此，ServeApi的运⾏流程已经全部分析完毕，其中核⼼部分ListenAndServe的实现，将在5.4节深⼊分析。

# 5.4 ListenAndServe实现

ListenAndServe的功能是：使Docker Server监听某⼀指定地址，并接收该地址上的请求，并对以上请求路由转发⾄相应的处理⽅法处。从实现的⾓度来看，ListenAndServe主要实现了设置⼀个服务于HTTP协议请求的服务端，该服务端监听指定地址上的请求，并对请求做特定 的 协 议 检 查 ， 最 终 完 成 请 求 的 路 由 与 分 发 。 代 码 实 现 位于./docker/docker/api/server/server.go。

ListenAndServe的实现可以分为以下4个部分：

1）创建router路由实例。  
2）创建listener监听实例。  
3）创建http.Server。  
4）启动API服务。

ListenAndServe的执⾏流程如图5-1所⽰。

![](images/cf61262a9461b55d4d458b3c3097a8eb48fdf189d9ad5a9157868e4276227c71.jpg)  
图5-1 ListenAndServe执⾏流程图

本节将按照ListenAndServe执⾏流程图⼀⼀深⼊分析各个部分。

# 5.4.1 创建router路由实例

⾸ 先 ， 在 函 数 ListenAndServe 的 实 现 过 程 中 ， Docker 通 过createRouter创建了⼀个router路由实例。代码源码如下：

```go
r, err := createRouter(job.Eng, job.GetenvBool("Logging"), job.GetenvBool("EnableCors"), job.Getenv("Version"))  
if err != nil {  
    return err  
} 
```

createRouter的实现位于./docker/docker/api/server/server.go#L1094。

创建router路由实例是⼀个重要的环节，路由实例的作⽤是：负责Docker Server对外部请求的路由以及分发。在实现过程中，有两个主要步骤：第⼀，创建全新的router路由实例；第⼆，为router实例添加路由记录。

# 1.创建空路由实例

实质上，createRouter通过包gorilla/mux来实现⼀个功能强⼤的路由器和分发器。源码实现如下：

```go
r := mux.NewRouter()
```

NewRouter（）函数返回了⼀个全新的router实例r。在创建Router实例时，给Router对象实例的两个属性进⾏赋值，分别为nameRoutes和KeepContext。其中namedRoutes属性为map类型，键为string类型，值为Route 路由记录类型；另外，KeepContext 属性为false ，表⽰DockerServer在处理完请求之后，就清除请求的内容，不对请求做存储操作。

源

码

位

于 ./docker/docker/vendor/src/github.com/gorilla/mux/mux.go#L16 ， 如 下所⽰：

```txt
func NewRouter() *Router {
    return &Router{nameisedRoutes: make(map[string]*Route), KeepContext: false}
} 
```

可⻅，以上代码返回的类型为mux.Router。mux.Router会通过⼀系列已经注册过的路由记录，来匹配接收的请求。⾸先通过请求的URL或者其他条件，找到相应的路由记录，并调⽤这条路由记录中的执⾏处理⽅法。mux.Router有以下特性：

·请求可以基于URL的主机名、路径、路径前缀、shemes、请求头和请求值、HTTP请求⽅法类型或者使⽤⾃定义的匹配规则。

·URL主机名和路径可以通过⼀个正则表达式来表⽰。

·注册的URL可以被直接运⽤，也可以被保留，从⽽保证维护资源的使⽤。

·路由记录同样可以作⽤于⼦路由记录：如果⽗路由记录匹配，则嵌套记录只会被⽤来测试。当设计⼀个组内的路由记录共享相同的匹配条件时，如主机名、路径前缀或者其他重复的属性，⼦路由的⽅式会起到相应的效果。

·mux.Router实现了http.Handler接⼝，故和标准的http.ServeMux兼容。

# 2.添加路由记录

Router路由实例r创建完毕，下⼀步⼯作是为Router实例r添加所需要的路由记录。路由记录存储着⽤来匹配请求的信息，包括对请求的

匹配规则，以及匹配之后的处理⽅法执⾏⼊⼝。

回到createRouter实现源码中，Docker⾸先判断Docker Daemon的启动过程中有没有开启DEBUG模式。通过docker可执⾏⽂件启动DockerDaemon，解析flag参数时，若flDebug的值为false，则说明不需要配置DEBUG环境；若flDebug的值为true，则说明需要为Docker Daemon添加DEBUG功能。具体的源码实现如下：

```scala
if os.Getenv("DEBUG") != "" { AttachProfiler(r) } 
```

AttachProiler（r）的功能是为路由实例r添加与DEBUG相关的路由记录，具体实现位于./docker/docker/api/server/server.go#L1083，如下所⽰：

```txt
func AttachProfiler router *mux.Router) {
    router.HandleFunc("/debug/vars", expvarHandler)
    router.HandleFunc("/debug/pprof/"，pprof.Index)
    router.HandleFunc("/debug/pprof/cmdline"，pprof Cmdline)
    router.HandleFunc("/debug/pprof/profile"，pprof.Profile)
    router.HandleFunc("/debug/pprof/symbol"，pprof_SYMBOL)
    router.HandleFunc("/debug/pprof/heap"，pprof Handler("heap").ServeHTTP)
    router.HandleFunc("/debug/pprof/goroutine"，pprof Handler("goroutine").ServeHTTP)
    router.HandleFunc("/debug/pprof/threadcreate"，pprof.Hand1er("threadcreate").ServeHTTP)
} 
```

分析以上源码，可以发现Docker Server使⽤两个包来完成DEBUG相关的⼯作：expvar和pprof。包expvar为公有变量提供标准化的接⼝，使得这些公有变量可以通过HTTP的形式在“/debug/vars”这个URL下被访问，传输格式为JSON。包pprof将Docker Server运⾏时的分析数据通过“/debug/pprof/”这个URL向外暴露。这些运⾏时信息包括以下内容：

可得的信息列表、正在运⾏的命令⾏信息、CPU信息、程序函数引⽤信息、ServeHTTP函数三部分信息的使⽤情况（堆使⽤、协程使⽤和线程使⽤）。

再次回到createRouter函数实现中，完成DEBUG功能的所有⼯作之后，Docker创建了⼀个映射类型的对象m，⽤于初始化路由实例r的路由记录。简化的m对象，源码如下：

```go
m := map[string]string[string]funcutorsFunc{ "GET": { "/events": getEvents, "/info":.Info, "/version":Versions, "/images/json": ImagesJSON, "/images/viz": ImagesViz, "/images/search": ImagesSearch, "/images/{name:.}"/get": getImagesGet, "/images/{name:.}"/history": getImagesHistory, "/images/{name:.}"/json": getImagesByName, "/containers/ps": getContainersJSON, "/containers/json": getContainersJSON, ... }, "POST": { ... "/containers/{name:.}"/copy": postContainersCopy, }, "DELETE": { "/containers/{name:.}": deleteContainers, "/images/{name:.}": deleteImages, }, "OPTIONS": { "" : optionsHandler, }, } 
```

对象m的类型为映射，其中键为string类型，代表HTTP的请求类型，如GET、POST、DELETE等，值为另⼀个映射类型，该映射代表的是URL与执⾏处理⽅法的映射。在第⼆个映射类型中，键为string类型，代表的是请求URL，值为HttpApiFunc类型，代表具体的执⾏处理⽅法。其中HttpApiFunc类型的定义如下：

```txt
type HttpApiFunc func(eng *engine.Engine, version version.Version, w http.ResponseWriter, r *http.Request, vars map[string string) error 
```

完成对象m的定义，随后Docker Server通过该对象m来添加路由实例r的路由记录。对象m的请求⽅法、请求URL和请求处理⽅法这三样内容可以为对象r构建⼀条路由记录。源码实现如下：

```go
for method, routes := range m {
    for route, fmt := range routes {
        logDebugf("Registering %s, %s", method, route)
        localRoute := route
        localFct := fmt
        localMethod := method
        f := makeHttpHandler(eng, logging, localMethod, localRoute, localFct, enableCors, version.dockerversion))
        if localRoute == "" {
            r.Methods(localMethod).HandlerFunc(f)
        } else {
            r.Path("/v{version: [0-9.]+}" localRoute).Methods(localMethod).HandlerFunc(f)
            r.Path(localRoute).Methods(localMethod).HandlerFunc(f)
        }
    }
} 
```

分析以上源码可以发现：在第⼀层循环中，按HTTP请求⽅法划分，获得请求⽅法各⾃的路由记录；第⼆层循环，按匹配请求的URL进⾏划分，获得与URL相对应的执⾏处理⽅法。在嵌套循环中，通过

makeHttpHandler返回⼀个执⾏的函数f。在返回的这个函数中，涉及了⽇志配置信息、CORS信息（跨域资源共享协议），以及版本信息。下⾯举例说明makeHttpHandler的实现，从对象m可以看到，对于GET请求 ， 请 求 URL 为 /info ， 则 请 求 处 理 ⽅ 法 为 getInfo 。 执 ⾏makeHttpHandler的具体源码实现如下：

```go
func makeHandler eng \*engine.Engine, logging bool, localMethod string, localRoute string, handlerFunc HttpApiFunc, enableCors bool, dockerVersion version.Version) http HandlerFunc { return func(w http.ResponseWriter, r \*http.Request) { // log the request log.Depugf("Calling %s %s", localMethod, localRoute) if logging { log.Info("%s %s", r.Method, r.RequestURI) } if strings.Controls(rHeader.Get("User-Agent"), "Docker-Client/") { userAgent := strings.Split(r Header.Get("User-Agent"), "/") if len(userAgent) == 2 && !dockerVersion EQUAL(version.userAgent[1]) { log.Depugf("Warning: client and server don't have the same version (client: %s, server: %s)", userAgent[1], dockerVersion) } } version := version.Version(muxVars(r)["version)]) if version == "" { version = api(APIVERSION } if enableCors { writeCorsHeaders(w, r) } if version.GreaterThan api(APIVERSION) { http.Error(w, fmt.Error("client and server don't have same version (client: %s, server: %s)", vers ion, api(APIVERSION).Error(), http.StatusNotFound) return } if err := handlerFunc(eng, version, w, r, muxVars(r)); err != nil { log.Error("Handler for %s %s returned error: %s", localMethod, localRoute, err) httpError(w, err) 
```

```txt
} 1   
} 
```

可 ⻅ makeHttpHandler 的 执 ⾏ 直 接 返 回 ⼀ 个 函 数 func （ whttp.ResponseWriter ， r*http.Request ） 。 在 func 函 数 的 实 现 中 ， 判 断makeHttpHandler传⼊的logging参数，若为true，则将该处理⽅法的执⾏⽇志显⽰出来；另外通过makeHttpHandler传⼊的enableCors参数判断是否在HTTP请求的头⽂件中添加跨域资源共享信息，若为true，则通过writeCorsHeaders函数向请求响应中添加有关CORS的HTTP Header，源码实现位于./docker/docker/api/server/server.go#L1022，如下所⽰：

```txt
func writeCorsHeaders(w http.ResponseWriter, r *http.Request) {
    w Header().Add("Access-Control-Allow-Origin", "*")
    w Header().Add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")
    w Header().Add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS")
} 
```

最 为 重 要 的 执 ⾏ 部 分 为 handlerFunc （ eng ， version ， w ， r ，mux.Vars（r）），源码如下：

```txt
if err := handlerFunc(eng, version, w, r, muxVars(r)); err != nil {
    log.Errorf("Handler for %s %s returned error: %s", localMethod, localRoute, err)
    httpError(w, err)
} 
```

对于GET请求类型，URL为/info的请求，由于处理⽅法名为getInfo，也就是说handlerFunc这个⾏参的值为getInfo，故执⾏部分直接运⾏getInfo（eng，version，w，r，mux.Vars（r）），⽽getInfo的具体实现位于./docker/docker/api/server/serve.go#L269，如下所⽰：

```txt
func/info(eng \*engine.Engine, version version.Version, w http.ResponseWriter, r \*http.Request, vars map[string]string) error {w Header().Set("Content-Type", "application/json")engServeHTTP(w,r)return nil} 
```

以上makeHttpHandler的执⾏已经完毕，返回func函数，作为指定URL对应的处理⽅法。

创建完处理函数后，Docker需要向路由实例添加新的路由记录。如果URL信息为空，则直接为该HTTP请求⽅法类型添加路由记录；若URL不为空，则为请求URL路径添加新的路由记录。需要额外注意的是，在URL不为空，为路由实例r添加路由记录时，考虑了API版本的问 题 ， 通 过 r.Path （ "/v{version ： [0-9.]+}"+localRoute ） .Methods（localMethod）.HandlerFunc（f）来实现。

⾄此，mux.Router实例r的两部分⼯作已经全部完成：创建空的路由实例r，为r添加相应的路由记录，最后返回配置后的路由实例r。

# 5.4.2 创建listener监听实例

路由模块，完成请求的路由与分发，是ListenAndServe实现中的第⼀项重要⼯作。对于请求的监听功能，同样需要模块来完成。⽽在ListenAndServe实现中，第⼆项重要的⼯作就是创建Listener。Listener是⼀种⾯向流协议的通⽤⽹络监听模块。

在创建Listener之前，Docker先判断Docker Server允许的协议，若协议为fd形式，则直接通过ServeFd来服务请求；若协议不为fd形式，则继续往下执⾏。

程序⾸先需要判断serveapi这个Job的环境中BufferRequests的值，若为true，则通过包listenbuffer创建⼀个Listener的实例l，否则的话直接通 过 包 net 创 建 Listener 实 例 l 。 具 体 的 源 码 位于./docker/docker/api/server/server.go#L1269-L1273，如下所⽰：

```txt
if job.GetenvBool("BufferRequests") { l, err = listenbuffer.NewListenBufferProto, addr, activationLock) } else { l, err = net.ListenProto, addr) } 
```

由于在mainDaemon（）中创建serveapi这个Job之后，给Job添加环境变量时，环境变量BufferRequets的值为true，故使⽤包listenbuffer创建listener实例。

Listenbuffer的作⽤是：让Docker Server⽴即监听指定协议地址上的请求，但是将这些请求暂时先缓存下来，等Docker Daemon全部启动完毕之后，才让Docker Server开始接受这些请求。这样设计有⼀个很⼤的好处，那就是可以保证在Docker Daemon还没有完全启动完毕之前，接收并缓存尽可能多的⽤户请求。

若协议的类型为TCP，另外Job中环境变量Tls或者TlsVerify有⼀个为true，则说明Docker Server需要⽀持HTTPS服务，Docker需要为Docker Server配置安全传输层协议（TLS）的⽀持。实现TLS协议，⾸先需要建⽴⼀个tls.Config类型实例tlsConfig，然后在tlsConfig中加载证书、认证信息等，最终通过tls包中的NewListener函数，创建出适应于接收HTTPS协议请求的Listener实例l，代码如下：

$\rceil =$ tls.NewListener(l, tlsConfig)

⾄此，创建⽹络监听的Listener部分已经全部完成。

# 5.4.3 创建http.Server

Docker Server同样需要创建⼀个Server对象来运⾏HTTP/HTTPS服务 端 。 在 ListenAndServe 实 现 中 第 三 个 重 要 的 ⼯ 作 就 是 创 建http.Server，实现源码如下：

httpSrv : $: =$ http.Server{Addr: addr, Handler: r}

其中addr为需要监听的地址，r为mux.Router路由实例。

# 5.4.4 启动API服务

创建http.Server实例之后，Docker Server⽴即启动API服务，使Docker Server开始在Listener监听实例l上接受请求，并对于每⼀个请求都⽣成⼀个新的协程来做专属服务。对于每⼀个请求，协程会读取请求，查询路由表中的路由记录项，找到匹配的路由记录，最终调⽤路由记录中的处理⽅法，执⾏完毕后，协程对请求返回响应信息。代码如下：

return httpSrv.Serve(l)

⾄此，ListenAndServer的所有流程已经分析完毕，Docker Server已经开始针对不同的协议，服务API请求。

# 5.5 总结

Docker Server 作 为 Docker Daemon 架 构 中 请 求 的 ⼊ ⼝ ， 接 管 了Docker Client与Docker Daemon之间所有的通信。通信API的规范性，通信过程的安全性，服务请求的并发能⼒，往往都是Docker⽤户最为关⼼的内容。本章基于Docker源码，分析了Docker Server⼤部分的细节实现。希望Docker⽤户可以初探Docker Server的设计理念，并且可以更好地利⽤Docker Server创造更⼤的价值。

# 第6章 Docker Daemon⽹络

# 6.1 引⾔

Docker作为⼀个开源的轻量级虚拟化容器引擎技术，已然给云计算领域带来全新的发展模式。Docker借助容器技术彻底释放了轻量级虚拟化技术的威⼒，让容器的伸缩、应⽤的运⾏都变得前所未有的⽅便与⾼效。同时，Docker借助强⼤的镜像技术，让应⽤的分发、部署与管理变得史⽆前例的便捷。然⽽，Docker毕竟是⼀项较为新颖的技术，在Docker的世界中，⽤户并⾮⼀劳永逸，其中最为业界所诟病的便是Docker的⽹络问题。

⽏庸置疑，对于Docker管理者和开发者⽽⾔，如何有效、⾼效地管理Docker容器之间的交互以及Docker容器的⽹络⼀直是⼀个巨⼤的挑战。⽬前，云计算领域中，绝⼤多数系统都采取分布式技术来设计并实现。然⽽，在原⽣态的Docker世界中，Docker的⽹络却不具备跨宿主机的能⼒，这也或多或少制约着Docker在云计算领域的⾼速发展。

Docker⽹络问题的解决势在必⾏，⾯对不同的应⽤场景，不少IT企业都开发了各⾃的新产品来帮助完善Docker的⽹络。这些企业中不乏像Google⼀样的互联⽹翘楚，同时也有不少初创企业率先出击，在最前沿不懈探索。这些新产品包括：Google推出的容器管理和编排⼯具Kubernetes，Zett.io公司开发的通过虚拟⽹络连接跨宿主机容器的⼯具Weave，CoreOS团队针对Kubernetes设计的⽹络覆盖⼯具Flannel，Docker官⽅的⼯程师Jérôme Petazzoni⾃⼰设计的SDN⽹络解决⽅案Pipework，以及SocketPlane项⽬等。

对于Docker管理者与开发者⽽⾔，虽然Docker 的跨宿主机通信能⼒暂时不够完善，但了解Docker⾃⾝的⽹络架构也是很有必要的。只

有深⼊了解Docker⾃⾝的⽹络设计与实现，才能清楚其弊端，从⽽扩展Docker的跨宿主机能⼒。

Docker⾃⾝的⽹络主要包含两部分：Docker Daemon的⽹络配置、Docker容器的⽹络配置。本章主要从源码的⾓度，分析Docker Daemon在启动过程中，为Docker配置的⽹络环境，内容安排如下：

1）Docker Daemon⽹络配置。  
2）运⾏Docker Daemon⽹络初始化任务。  
3）创建Docker⽹桥。

# 6.2 Docker Daemon⽹络介绍

在Docker环境中，Docker容器的⽹络能⼒⼀直备受关⼼。对⼀个Docker容器⽽⾔，它可以独享内部的⽹络栈，并与其他容器分处隔离的⽹络环境。然⽽作为DockerDaemon创建的容器，容器与宿主机之外建⽴通信时，仍然⽆可避免地通过宿主机物理⽹卡或者虚拟机的eth0虚拟⽹卡。既然如此，那么如何构建Docker容器与宿主机之间的⽹络拓扑，将是⼀个学习Docker⽹络时必须理解的关键点。

在⼀台没有安装Docker的宿主机上，⽹络环境很有可能平淡⽆奇。然⽽，当⽤户开始安装并启动Docker时，⼀切发⽣了变化。⽽Docker管理员完全有权限在启动Docker时配置Docker Daemon的⽹络模式。

关于Docker的⽹络模式，⼤家最熟知的应该就是“桥接”模式，也被称为bridge模式。在桥接模式下，Docker的⽹络环境拓扑（包括DockerDaemon⽹络环境和Docker Container⽹络环境）如图6-1所⽰。

![](images/699e2cffdb6f8a342cde26b592642fbf0d2ca6fe10fffa834f3c3caf7db9c4d8.jpg)  
图6-1 Docker⽹络桥接模式⽰意图

然⽽，“桥接”模式是Docker⽹络模式中最为常⽤的模式。除此之外，Docker还为⽤户提供了更多的可选项，本章余下部分将进⾏深⼊分析。

# 6.3 Docker Daemon⽹络配置接⼝

Docker Daemon每次启动的过程中，都会初始化⾃⾝的⽹络环境。初始化后的⽹络环境最终为Docker容器提供⽹络通信服务。为了实现Docker Daemon ⽹ 络 的 初 始 化 ， Docker 管 理 员 可 以 在 启 动 DockerDaemon时，通过参数的形式配置Docker的⽹络环境。配置参数可以通过运⾏docker⼆进制可执⾏⽂件来完成，即通过运⾏docker-d并添加其他与⽹络相关的flag参数来完成。

其 中 ， 涉 及 的 flag 参 数 有 EnableIptables 、 EnableIpForward 、BridgeIface、BridgeIP以及InterContainerCommunication。这5个与⽹络相关的flag参数的定义位于./docker/docker/daemon/config.go#L49-L53，具体源码如下：

```txt
flag.BooleanVar(&config.EnableIptables, [string[#iptables], "-iptables"], true, "Enable Docker's addition of iptables rules")  
flag.BooleanVar(&config.EnableIpForward, [string[#ip-forward", "-ip-forward}], true, "Enable net. ipv4. ip_forward")  
flag.StringVar(&config.BridgeIP, [string[#bip], "-bip"], "", "Use this CIDR notation address for the network bridge's IP, not compatible with -b")  
flag.StringVar(&config.BridgeInterface, [string[b", "-bridge"], "", "Attach containers to a pre-existing network bridge\use 'none' to disable container networking")  
flag.BooleanVar(&config.InterContainerCommunication, [string[#icc", "-icc}], true, "Enable inter- container communication") 
```

以下介绍这5个flag参数的作⽤：

·EnableIptables ：确保Docker Daemon 启 动 时， 能 对宿 主机 上的iptables规则进⾏修改。

·EnableIpForward：确保net.ipv4.ip_forward功能开启，使得宿主机在多⽹络接⼝模式下，数据包可以在⽹络接⼝之间转发。

·BridgeIP：Docker Daemon为⽹络环境中的⽹桥配置的CIDR⽹络地址。

·BridgeIface ： 为 Docker ⽹ 络 环 境 指 定 具 体 的 通 信 ⽹ 桥 ， 若BridgeIface的值为none，则说明不需要为Docker容器创建⽹桥服务，关闭Docker容器的⽹络能⼒。

·InterContainerCommunication：确保Docker容器之间可以完成通信，通过防⽕墙完成。

除DockerDaemon会使⽤到的5个flag参数之外，Docker在创建⽹络环境时，还使⽤⼀个DefaultIP变量，如下所⽰：

```txt
opts.IPVar(&config.DefaultIp, []string"#ip", 
```

```txt
ip'}, "0.0.0.0", "Default IP address to use when binding container ports") 
```

该变量的作⽤是：绑定Docker容器的端⼝与宿主机上的某⼀个端⼝时，将DefaultIp作为默认使⽤的宿主机IP地址。

具备以上Docker Daemon的⽹络背景知识之后，我们来分析如何通过docker⼆进制⽂件启动Docker Dameon并配置相应的⽹络环境。Docker Daemon的⽹络环境和两个flag参数相关性最⼤，分别为BridgeIP和BridgeIface。举例说明如表6-1所⽰。

# 表6-1 Docker Daemon启动命令表

<table><tr><td>启动Docker Daemon命令</td><td>作用分析</td></tr><tr><td>docker -d</td><td>启动Docker Daemon,使用默认网桥 docker0,不指定CIDR网络地址</td></tr><tr><td>docker -d -b=&quot;xxx&quot;</td><td>启动Docker Daemon,使用网桥 xxx,不指定CIDR网络地址</td></tr><tr><td>docker -d --bip=&quot;172.17.42.1&quot;</td><td>启动Docker Daemon,使用默认网桥 docker0,使用指定CIDR网络地址172.17.42.1</td></tr><tr><td>docker -d --bridge=&quot;xxx&quot; --bip=&quot;10.0.42.1&quot;</td><td>报错,出现兼容性问题,不能同时指定BridgeIP和BridgeIface</td></tr><tr><td>docker -d --bridge=&quot;none&quot;</td><td>启动Docker Daemon,不创建Docker网络环境</td></tr></table>

深⼊理解BridgeIface与BridgeIP，并熟练使⽤相应的flag参数，就能做到配置Docker Daemon的⽹络环境。需要特别注意的是，DockerDaemon的⽹络与Docker容器的⽹络存在很⼤的区别。Docker Daemon为Docker容器创建⽹络的⼤环境，Docker容器的⽹络需要DockerDaemon的⽹络提供⽀持，但不唯⼀。举⼀个形象的例⼦，DockerDaemon可以创建docker0⽹桥，为之后Docker容器的桥接模式提供⽀持；然⽽在桥接模式下，Docker容器可以启⽤桥接，转⽽根据⽤户需求创建⾃⾝⽹络。其中Docker容器的⽹络可以是桥接模式的⽹络，同时也可以直接共享宿主机的⽹络设备，另外还有其他模式。关于Docker容器的⽹络，将在第7章进⾏详细介绍。

# 6.4 Docker Daemon⽹络初始化

正如上⼀节所⾔，Docker管理员可以通过与⽹络相关的flag参数BridgeIface与BridgeIP，来为Docker Daemon创建⽹络环境。最简单的，Docker管理员通过执⾏"docker-d"就已经完成Docker Daemon的运⾏。

Docker Daemon⽹络初始化流程如图6-2所⽰。

![](images/e375564b16f77fa75688a999da68293938428d2691d90200764445757d64c654.jpg)  
图6-2 Docker Daemon⽹络初始化流程图

总体⽽⾔，Docker Daemon⽹络的初始化流程主要是根据解析flag参数来决定到底建⽴哪种类型的⽹络环境。从流程图中可知，DockerDaemon创建⽹络环境时有两个分⽀，不难发现分⽀代表的分别是：为Docker创建⼀个⽹络驱动，以及对Docker的⽹络不做任何操作。

下⾯参照Docker Daemon⽹络初始化流程图具体分析实现步骤。

# 6.4.1 启动Docker Daemon传递flag参数

⽤户希望感受到Docker带来的便利，⾸先必须要有⼀个运⾏着的Docker Daemon 。 因 此 ， ⽤ 户 第 ⼀ 步 要 完 成 的 就 是 启 动 DockerDaemon。⽤户可以通过docker可执⾏⽂件来启动Docker Daemon，并在命令⾏中选择性地传⼊所需要的flag参数。

# 6.4.2 解析⽹络flag参数

Docker Daemon的启动前期，Docker使⽤flag包对命令⾏中的flag参数进⾏解析。其中和Docker Daemon⽹络配置相关的flag参数有五个，分别是：EnableIptables、EnableIpForward、BridgeIP、BridgeIface以及InterContanierCommunication，各个flag参数的作⽤本章已有介绍。

# 6.4.3 预处理flag参数

解析完与Docker Daemon⽹络相关的flag参数之后，Docker仍然需要对这些参数的值进⾏预处理。预处理与⽹络配置相关的flag参数信息，包括检测配置信息的兼容性，以及判断是否创建Docker⽹络环境。

⾸先，Docker需要检验flag参数中是否会出现彼此不兼容的信息，源码位于./docker/docker/daemon/daemon.go#L679-L685，如下所⽰：

```lua
// Check for mutually incompatible config options
if config.BridgeIface != "" && config.BridgeIP != "" {
return nil, fmt.Error("You specified -b & --bip, mutually exclusive options. Please specify only one.")
}
if !config EnableIptables && !config.InterContainerCommunication {
return nil, fmt.Error("You specified --iptables=false with --
icc=false. ICC uses iptables to function. Please set --icc or --iptables to true.")
} 
```

flag参数的兼容信息主要有两种。

第⼀种，BridgeIP和BridgeIface配置信息的兼容性。具体表现为⽤户启动Docker Daemon时，若同时指定了BridgeIP和BridgIface的值，则出现兼容问题。原因在于这两者属于互斥对，换⾔之，若⽤户指定了新建⽹桥的设备名，那么该⽹桥已经存在，⽆须指定⽹桥的IP地址BridgeIP；若⽤户指定了新建⽹桥的⽹络IP地址BridgeIP，那么该⽹桥肯定还没有新建成功，则Docker Daemon在新建⽹桥时使⽤默认⽹桥名docker0，⽆须在另⾏指定⽹桥的设备名。

第⼆种，EnableIptables和InterContainerCommunication配置信息的兼容性。具体表现为不能同时指定这两个flag参数为false。原因很简

单，若指定InterContainerCommunication为false，则说明Docker Daemon不允许创建的Docker容器之间进⾏互相通信。但是为了达到以上⽬的，Docker正是使⽤iptables防⽕墙的过滤规则。因此，再次设定EnableIptables为false，关闭iptables的使⽤，即出现了⾃相⽭盾的结果。

检验完系统配置信息的兼容性问题，Docker Daemon接着会判断是否需要为Docker Daemon配置⽹络环境。判断的依据为BridgeIface的值是 否 与 DisableNetworkBridge 的 值 相 等 ， DisableNetworkBridge在./docker/docker/daemon/config.go#L13中被定义为const常量，值为字符串none。因此，若BridgeIface为none，则DisableNetwork为true，最终Docker Daemon 不 会 创 建 ⽹ 络 环 境 ； 若 BridgeIface 不 为 none ， 则DisableNetwork为false，最终Docker Daemon需要创建⽹络环境（桥接模式） 。

# 6.4.4 确定Docker⽹络模式

Docker⽹络模式由配置信息DisableNetwork决定。由于在上⼀节Docker Daemon已经得出DisableNetwork的值，故本节可以确定Docker⽹ 络 模 式 。 这 部 分 的 源 码 实 现 位于./docker/docker/daemon/daemon.go#L792-L805，如下所⽰：

```go
if !config.DisableNetwork {
    job := eng.Job("init_networkdriver")
    job.SetenvBool("EnableIptables", config.EnableIptables)
        job.SetenvBool("InterContainerCommunication", config.InterContainerCommunication)
    job.SetenvBool("EnableIpForward", config EnableIpForward)
    job.Setenv("BridgeIface", config.BridgeIface)
    job.Setenv("BridgeIP", config.BridgeIP)
    job.Setenv("DefaultBindingIP", config.DefaultIp.String())
    if err := jobRUN(); err != nil {
        return nil, err
    }
} 
```

若DisableNetwork为false，则说明需要为Docker Daemon创建⽹络环境，具体的模式为⽹桥模式。创建Docker Daemon⽹络环境的步骤为：

1）创建名为init_networkdriver的Job。  
2）为Job配置环境变量，配置的环境变量有EnableIptables、InterContainerCommunication、EnableIpForward、BridgeIface、BridgeIP以及DefaultBindingIP。  
3）触发执⾏Job。

运⾏init_network实际完成的⼯作是创建Docker⽹桥，这部分内容将会在下⼀节详细分析。

若DisableNetwork为true。则说明不需要为Docker Daemon创建⽹络环境，⽹络模式属于none模式。

以上便是Docker Daemon⽹络初始化的所有流程。

# 6.5 创建Docker⽹桥

Docker的⽹络往往是Docker开发者最常提起的话题。⽽Docker⽹络中最常使⽤的模式为桥接模式。本节将详细分析Docker⽹桥的创建流程。

Docker ⽹ 桥 的 创 建 通 过 init_network 这 个 Job 的 运 ⾏ 来 完 成 。init_network 的 实 现 为 InitDriver 函 数 ， 位于 ./docker/docker/daemon/networkdriver/bridge/driver.go#L79 ， InitDriver函数的运⾏流程如图6-3所⽰。

![](images/8de545df8dcb4b8f833636be6139518a9f09b8a0b4965854a600dbac75a8993e.jpg)  
图6-3 Docker Daemon创建⽹桥流程图

# 6.5.1 提取环境变量

在InitDriver函数的实现过程中，Docker⾸先提取init_networkdriver这个Job的环境变量。这样的环境变量共有6个，各⾃的作⽤在上⽂已经详细说明。具体的实现源码如下：

```go
var (
    network *net.IPNet
    enableIPTables = job.GetenvBool("EnableIptables")
    icc = job.GetenvBool("InterContainerCommunication")
    ipForward = job.GetenvBool("EnableIpForward")
    bridgeIP = job.Getenv("BridgeIP")
)
if defaultIP := job.Getenv("DefaultBindingIP"); defaultIP != "" {
    defaultBindingIP = net.ParseIP(defaultIP)
}
bridgeIface = job.Getenv("BridgeIface") 
```

# 6.5.2 确定Docker⽹桥设备名

提取Job的环境变量之后，Docker随即确定最终使⽤⽹桥设备的名称。为此，Docker⾸先创建了⼀个名为usingDefaultBridge的bool变量，含义为是否使⽤默认的⽹桥设备，默认值为false。接着，若环境变量中bridgeIface的值为空，则说明⽤户启动Docker时，没有指定特定的⽹桥设备名，因此Docker⾸先将usingDefaultBridge置为true，然后使⽤默认的⽹桥设备名DefaultNetworkBridge赋值于bridgeIface，即docker0；若bridgeIface的值不为空，则判断条件不成⽴，继续往下执⾏。这部分的源码实现为：

usingDefaultBridge $\coloneqq$ false   
if bridgeIface $= =$ ""{ usingDefaultBridge $=$ true bridgeIface $=$ DefaultNetworkBridge   
}

# 6.5.3 查找bridgeIface⽹桥设备

确 定 Docker ⽹ 桥 设 备 名 bridgeIface 之 后 ， Docker ⾸ 先 通 过bridgeIface设备名在宿主机上查找该设备是否真实存在。若存在，则返回 该 ⽹ 桥 设 备 的 IP 地 址 ， 若 不 存 在 ， 则 返 回 nil 。 实 现 源 码 位于 ./docker/docker/daemon/networkdriver/bridge/driver.go#L99 ， 如 下 所⽰：

addr, err := networkdriver.GetIfaceAddr(bridgeIface)

GetIfaceAddr 的 实 现 位 于./docker/docker/daemon/networkdriver/utils.go，实现步骤为：⾸先通过 Golang中net包的InterfaceByName⽅法获取名为bridgeIface的⽹桥设备， 会得出以下结果：

·若名为bridgeIface的⽹桥设备不存在，直接返回错误。

·若名为bridgeIface的⽹桥设备存在，返回该⽹桥设备的IP地址。

需要强调的是：GetIfaceAddr函数返回错误，说明当前宿主机上不存在名为bridgeIface的⽹桥设备。⽽这样的结果会有两种不同的情况：第⼀，⽤户指定了bridgeIface，那么usingDefaultBridge为false，⽽该bridgeIface⽹桥设备在宿主机上不存在；第⼆，⽤户没有指定bridgeIface，那么usingDefaultBridge为true，bridgeIface名为docker0，⽽docker0⽹桥在宿主机上也不存在。

当然，若GetIfaceAddr函数返回的是⼀个IP地址，则说明当前宿主机上存在名为bridgeIface的⽹桥设备。这样的结果同样会有两种不同的情况：第⼀，⽤户指定了bridgeIface，那么usingDefaultBridge为false，⽽该bridgeIface⽹桥设备在宿主机上已经存在；第⼆，⽤户没有指定

bridgeIface，那么usingDefaultBridge为true，bridgeIface名为docker0，⽽docker0⽹桥在宿主机上也已经存在。第⼆种情况⼀般是：⽤户在宿主机上第⼀次启动Docker Daemon时，创建了默认⽹桥设备docker0，⽽后docker0⽹桥设备⼀直存在于宿主机上，故之后在不指定⽹桥设备的情况下，重启Docker Daemon，会出现docker0已经存在的情况。

以下两节将分别从bridgeIface已创建与bridgeIface未创建两种不同的情况进⾏深⼊分析。

# 6.5.4 bridgeIface已创建

Docker Daemon 所 在 宿 主 机 上 bridgeIface 的 ⽹ 桥 设 备 存 在 时 ，Docker Daemon仍然需要验证⽤户在配置信息中是否为⽹桥设备指定了IP地址。

⽤户启动Docker Daemon时，假如没有指定bridgeIP参数信息，则Docker Daemon使⽤名为bridgeIface的原有的IP地址。

当⽤户指定了bridgeIP参数信息时，则需要验证：指定的bridgeIP参数信息与bridgeIface⽹桥设备原有的IP地址信息是否匹配。若两者匹配，则验证通过，继续往下执⾏；若两者不匹配，则验证不通过，抛出错误，显⽰“bridgeIP与已有⽹桥配置信息不匹配”。该部分内容位于 ./docker/docker/daemon/networkdriver/bridge/driver.go#L119-L129 ， 源码如下：

network $=$ addr.\*net.IPNet)   
// validate that the bridge ip matches the ip specified by BridgeIP   
if bridgeIP $! = "$ {" bip, _, err := net.ParseCIDR(bridgeIP) if err != nil { return job.Error(err) } if !network.IP Equal(bip) { return job.Error("bridge ip (%s) does not match existing bridge configuration %s", network.IP, bip) }

# 6.5.5 bridgeIface未创建

Docker Daemon所在宿主机上bridgeIface的⽹桥设备未创建时，上⽂已经介绍将存在两种情况：

·⽤户指定的bridgeIface未创建。

·⽤户未指定bridgeIface，⽽docker0暂未创建。

当⽤户指定的bridgeIface不存在于宿主机时，即没有使⽤Docker的默认⽹桥设备名docker0，Docker打印⽇志信息“指定⽹桥设备未找到”，并返回⽹桥未找到的错误信息。源码实现如下：

```groovy
if !usingDefaultBridge {
    job.Logf("bridge not found: %s", bridgeIface)
    return job.Error(err)
} 
```

当使⽤默认⽹桥设备名，⽽docker0⽹桥设备还未创建时，DockerDaemon则⽴即实现创建⽹桥的操作，并返回该docker0⽹桥设备的IP地址。代码如下：

```go
// If the iface is not found, try to create it  
job.Log("creating new bridge for %s", bridgeIface)  
if err := createBridge(bridgeIP); err != nil {  
    return job.Error(err)  
}  
job.Log("getting iface addr")  
addr, err = networkdriver.GetIfaceAddr(bridgeIface)  
if err != nil {  
    return job.Error(err)  
}  
network = addr.*net.IPNet 
```

创建Docker Daemon⽹桥设备docker0的实现，全部由createBridge（ bridgeIP ） 来 实 现 ， createBridge 的 实 现 位于 ./docker/docker/daemon/networkdriver/bridge/driver.go#L245 createBridge函数实现步骤如下：

1）确定⽹桥设备docker0的IP地址。  
2）通过createBridgeIface函数创建docker0⽹桥设备，并为⽹桥设备分配随机的MAC地址。  
3）将第⼀步中已经确定的IP地址，添加给新创建的docker0⽹桥设备。  
4）启动docker0⽹桥设备。

下⾯详细分析4个步骤的具体实现。

第⼀步，Docker Daemon确定docker0的IP地址，实现⽅式为判断⽤户是否为⽹桥设备指定bridgeIP。若⽤户未指定bridgeIP，则从Docker预先准备的IP⽹段列表addrs中查找合适的⽹段。具体的源码实现位于 ./docker/docker/daemon/networkdriver/bridge/driver.go#L257-L278 ， 如下所⽰：

if len_bridgeIP) $! = 0$ { _, err := net.ParseCIDR_bridgeIP) if err != nil { return err } iffaceAddr $=$ bridgeIP   
} else { for_,addr := range addr { _, dockerNetwork, err := net.ParseCIDR(addr) if err != nil {

return err   
} if err := networkdriver.CheckedNameserverOverlaps(nameservers, dockerNetwork); err == nil { if err := networkdriver.CheckedRouteOverlaps(dockerNetwork); err == nil { iffaceAddr $=$ addr break } else{ logDebugf("%s %s",addr,err) 1 } 1

# 其中，Docker Daemon为⽹桥设备准备的候选⽹段地址addrs为：

```csv
"172.17.42.1/16", // Don't use 172.16.0.0/16, it conflicts with EC2 DNS 172.16.0.23
"10.0.42.1/16", // Don't even try using the entire /8, that's too intrusive
"10.1.42.1/16",
"10.42.42.1/16",
"172.16.42.1/24",
"172.16.43.1/24",
"172.16.44.1/24",
"10.0.42.1/24",
"10.0.43.1/24",
"192.168.42.1/24",
"192.168.43.1/24",
"192.168.44.1/24",
} 
```

通过执⾏以上流程，Docker Daemon可以确定⼀个可⽤的IP⽹段地址，为ifaceAddr；若仍然未匹配合适的⽹段地址，DockerDaemon返回错误⽇志，表明没有合适的IP地址赋予docker0⽹桥设备。

第⼆步，DockerDaemon通过createBridgeIface函数创建docker0⽹桥设备。createBridgeIface函数的源码实现如下：

```go
func createBridgeface(name string) error {
    kv, err := kernel.GetKernelVersion()
    // only set the bridge's mac address if the kernel version is > 3.3
    // before that it was not supported
    setBridgeMacAddr := err == nil && (kv.Kernel >= 3 && kv.Major >= 3)
    logDebug("setting bridge mac address = %v", setBridgeMacAddr)
    return netlink.CreateBridge(name, setBridgeMacAddr)
} 
```

以上代码通过宿主机Linux内核信息，确定宿主机内核版本是否⽀持设定⽹桥设备的MAC地址。若Linux内核版本⼤于3.3，则⽀持配置MAC地址，否则不⽀持。⽽Docker在不低于3.8的内核版本上才能稳定运⾏，故可以认为内核⽀持配置MAC地址。最后通过netlink 的CreateBridge函数实现创建docker0⽹桥。

Netlink是Linux中⼀种较为特殊的socket通信⽅式，提供了⽤户应⽤间和内核进⾏双向数据传输的途径。在这种模式下，⽤户态可以使⽤标准的socket API来使⽤netlink强⼤的功能，⽽内核态需要使⽤专门的内核API才能使⽤netlink。

libcontainer的netlink包中的CreateBridge实现了创建实际的⽹桥设备，具体使⽤系统调⽤的代码如下：

syscall.Syscall(syscall.SYS_IOCTL, uintptr(s), SIOC_BRADDBR, uintptr(unsafe.Pointer(nameBytePtr)))

创建完⽹桥设备之后，为docker0⽹桥设备配置MAC地址，实现函数为setBridge-MacAddress。

第三步为创建的docker0⽹桥设备绑定IP地址。上⼀步仅完成了创建名为docker0的⽹桥设备，之后仍需要为docker0⽹桥设备绑定IP地址。具体源码实现为：

if netlinketworkLinkAddIp(iface，ipAddr，ipNet);err $! =$ nil{ return fmt.Errorf("Unable to add private network:%s"，err)   
1

NetworkLinkAddIP的实现同样位于libcontainer中的netlink包，主要的功能为：通过netlink机制为⼀个⽹络接⼝设备绑定⼀个IP地址。

第四步是启动docker0⽹桥设备。具体实现代码如下：

```go
if err := netlinketworkLinkUp(iface); err != nil { return fmt.Errorf("Unable to start network bridge: %s", err) } 
```

NetworkLinkUp的实现同样位于libcontainer中的netlink包，功能为启动docker0⽹桥设备。

⾄此，docker0⽹桥历经确定IP、创建、绑定IP、启动四个环节，createBridge关于docker0⽹桥设备的⼯作全部完成。

# 6.5.6 获取⽹桥设备的⽹络地址

创建完⽹桥设备之后，⽹桥设备必然会存在⼀个⽹络地址。⽹桥⽹络地址的作⽤为：Docker Daemon在创建Docker容器时，使⽤该⽹络地 址 为 Docker 容 器 分 配 IP 地 址 。 Docker 使 ⽤ 源 码 network $\underline { { \underline { { \mathbf { \Pi } } } } }$ addr.（*net.IPNet）获取⽹桥设备的⽹络地址。

# 6.5.7 配置Docker Daemon的iptables

创建完⽹桥之后，Docker Daemon为容器以及宿主机配置iptables，包括为容器之间所需要的link操作提供⽀持，为宿主机上所有的对外对内流量制定传输规则等。这部分详情可参⻅第4章，源码实现位于 ./docker/daemon/networkdriver/bridge/driver/driver.go#L133 ， 如 下 所⽰：

```go
// Configure iptables for link support  
if enableIPTables {  
    if err := setupIPTables(addr, icc); err != nil {  
        return job.Error(err)  
    }  
}  
// We can always try removing the iptables  
if err := iptables.RemoveExistingChain("DOCKER"); err != nil {  
    return job.Error(err)  
}  
if enableIPTables {  
    chain, err := iptables.NewChain("DOCKER", bridgeInterface)  
    if err != nil {  
        return job.Error(err)  
}  
portmapper.SetIptablesChain(chain) 
```

# 6.5.8 配置⽹络设备间数据报转发功能

默认情况下，Linux操作系统上的数据包转发功能是禁⽌的。数据包转发就是当宿主机存在多个⽹络设备时，如果其中⼀个接收到数据包 ， 并 将 其 转 发 给 另 外 的 ⽹ 络 设 备 。 Docker Daemon 通 过 修改/proc/sys/net/ipv4/ip_forward的值，将其置为1，则可以保证系统内数据包可以实现转发功能，代码如下：

```go
if ipForward{ // Enable IPv4 forwarding if err := ioutil.WriteFile("/proc/sys/net/ipv4/ip_forward", [],byte{'1', '\n'}, 0644); err != nil { job.Logf("WARNING: unable to enable IPv4 forwarding: %s\n", err) } } 
```

# 6.5.9 注册⽹络Handler

创建Docker Daemon⽹络环境的最后⼀个步骤是：注册4个与⽹络相 关 的 Handler 。 这 4 个 处 理 ⽅ 法 分 别 是 allocate_interface 、release_interface、allocate_port和link，作⽤分别是为Docker容器分配IP⽹络地址，释放Docker容器⽹络设备，为Docker容器分配端⼝资源，以及在Docker容器之间执⾏link操作。

⾄此，Docker Daemon的⽹络环境初始化⼯作全部完成。

# 6.6 总结

在⼯业界，Docker的⽹络问题备受关注。Docker的容器技术以及镜像技术，已经给Docker实践者带来了诸多效益。然⽽Docker⽹络的发展依然具有很⼤的潜⼒，依然值得⼤家不断探索并推动其发展。

Docker的⽹络环境可以分为Docker Daemon⽹络和Docker Container⽹络。本章从Docker Daemon的⽹络⼊⼿，分析了⼤家熟知的Docker桥接模式。

# 第7章 Docker容器⽹络

# 7.1 引⾔

如今，Docker技术⻛靡全球，⼤家在尝试以及玩转Docker的同时，肯定离不开⼀个概念，那就是“容器”或者“Container”。那么我们⾸先从原理的⾓度⼀窥“容器”或者“Container”的究竟。

第⼀次接触Docker时，⼤家肯定会深深感受到：在Docker容器内部运⾏应⽤程序竟是如此⽅便。第⼀，应⽤程序在Docker容器内部的部署与运⾏⾮常便捷，只要有Dockerfile，应⽤⼀键式的部署绝对不是天⽅夜谭；第⼆，Docker容器内运⾏的应⽤程序还可以受到资源的控制与隔离，⼤⼤满⾜云计算时代应⽤的要求。

⽏庸置疑，Docker的⼀些特性，的确摒弃了传统情况下开发模式的不少弊端。然⽽，这强⼤的功能背后到底是何等技术在“作祟”，⼜是谁可以⽀撑Docker的运⾏并提供丰富的特性？答案很简单，那就Linux内核。

那我们就从Linux内核的⾓度来看看Docker到底为何物，先从Docker容器⼊⼿。对于Docker容器，体验过的开发者都有两点⾮常重要的感受：内部可以跑应⽤（进程），以及提供隔离的环境。当然，后者肯定也是⼯业界称之为“容器”的原因之⼀。

⾸先，我们先来看Docker容器与进程的关系，或者容器与进程的关系。为了理清这两者之间的关系，我不妨提出这样⼀个问题“容器是否可以脱离进程⽽存在”。换句话说就是，能否创建⼀个容器，⽽这个容器内部没有任何进程。答案是否定的。既然答案是否定的，说明不可能先有容器，然后再有进程，那么问题⼜来了，“容器和进程是⼀起

诞⽣的，还是先有进程再有容器呢？”可以说答案是后者。以下将慢慢阐述其中的原因。

阐述问题“容器是否可以脱离进程⽽存在”的原因前，相信⼤家对于下⾯这段话不会有异议：通过Docker创建出的⼀个Docker Container是⼀个容器，⽽这个容器提供了进程组隔离的运⾏环境。那么问题在于，容器到底是通过什么途径来实现进程组运⾏环境的“隔离”呢？此时，就轮到Linux内核隆重登场了。

说 到 运 ⾏ 环 境 的 “ 隔 离 ” ， 相 信 ⼤ 家 肯 定 对 Linux 内 核 中 的namespaces和cgroups略有⽿闻，namespaces主要负责命名空间的隔离，cgroups主要负责资源使⽤的限制。其实，正是这两个神奇的内核特性联合使⽤，才保证了Docker容器之间的“隔离”。那么，namespaces和cgroups⼜和进程有什么关系呢？问题的答案可以⽤以下的次序来表⽰：

1）⽗进程通过fork创建⼦进程时，使⽤namespaces技术，实现⼦进程与⽗进程以及其他进程之间命名空间的隔离。  
2）⼦进程创建完毕之后，使⽤cgroups技术来处理进程，实现进程的资源限制。  
3）namespaces和cgroups这两种技术都⽤上之后，进程所处的“隔离”环境才真正建⽴，此时“容器”真正诞⽣！

从Linux内核的⾓度分析容器的诞⽣，精简的流程即如上三步，⽽这三个步骤也恰好巧妙地阐述了namespaces和cgroups这两种技术和进程的关系，以及进程与容器的关系。进程与容器的关系，⾃然是：容器不能脱离进程⽽存在，先有进程，后有容器。然⽽，⼤家往往会说到“使⽤Docker创建Docker容器，然后在容器内部运⾏进程”。对此，从通俗易懂的⾓度来讲，这完全可以理解，因为“容器”⼀词的存在，本

⾝就较为抽象。如果需要更为准确，可以表述为“使⽤Docker创建⼀个进程，为这个进程创建隔离的环境，这样的环境可以称为Docker容器，然后再在容器内部运⾏⽤户应⽤进程。”在这⾥，笔者的本意不是希望纠正外界对于Docker容器的认识，⽽是希望能和读者⼀起来看看Docker容器技术实现的原理到底⼏何。

更清楚地认识Docker容器之后，很快⼤家的眼球肯定会定位到namespaces和cgroups这两种技术上。Linux内核的这两种技术，竟然起到如此重⼤的作⽤。那么下⾯我们就从Docker容器实现流程的⾓度简要介绍这两者。

⾸先讲述⼀下namespace在容器创建时的⽤法。⽤户启动容器，Docker Daemon会fork出容器中的第⼀个进程A（暂且称为进程A，也就是 Docker Daemon 的 ⼦ 进 程 ） ， 执 ⾏ fork 时 通 过 5 个 参 数 标 志CLONE_NEWNS 、 CLONE_NEWUTS 、 CLONE_NEWIPC 、 1CLONE_PID和CLONE_NEWNET（Docker 1.2.0还没有完全⽀持usernamespace）。Clone系统调⽤⼀旦传⼊了这些参数标志，⼦进程将不再与⽗进程共享相同的命名空间（namespaces），⽽是由Linux为⼦进程创建新的命名空间（namespaces），从⽽保证⼦进程与⽗进程使⽤隔离的环境。另外，如果⼦进程A再次fork出⼦进程B，⽽fork时没有传⼊相应的namespaces参数标志时，⼦进程B将会与A共享相同的命名空间（namespaces）。如果Docker Daemon再次创建⼀个Docker容器，内部进程有D、E和F，那么这三个进程也会处于另外全新的namespaces中。两个容器的namespaces均与Docker Daemon所在的namespaces不同。Docker中Docker Daemon与Docker容器之间的namespaces关系，如图7-1所⽰。

![](images/cf83df95575db2b135426469990c2256b97b911b70bc86f4598524f20c68c936.jpg)  
图7-1 Docker的namespaces关系图

再说起cgroups，⼤家都知道可以使⽤cgroups为进程组做资源的限制。与namespaces不同的是，cgroup的使⽤并不是在创建容器内进程时完成，⽽是在创建容器内进程之后完成，最终使得容器进程处于资源控制的状态。换⾔之，cgroups的运⽤必须要等到容器内第⼀个进程被真正创建出来之后才能实现。当容器内进程创建完毕，Docker Daemon可以获知容器内主进程的PID信息，随后将该PID放置在cgroups⽂件系统的指定位置，做相应的资源限制。如此⼀来，当容器主进程再fork新的⼦进程时，新的⼦进程同样受到与主进程相同的资源限制，效果就是整个进程组受到资源限制。

可以说Linux内核的namespaces和cgroups技术，实现了资源的隔离与限制。那么对于资源的隔离，是否还需要为容器准备必需的资源，⽐如说容器需要使⽤的⽹络资源，容器需要使⽤的⽂件系统挂载点等。这回答案是肯定的。⽹络资源就是⼀个很好的例⼦。当DockerDaemon为进程创建完隔离的运⾏环境之后，我们可以发现进程并没有独⽴的⽹络栈可以使⽤，如独⽴的⽹络接⼝等。此时，Docker Daemon

会将Docker容器内部所需要的资源⼀⼀配备⻬全。⽹络⽅⾯，即为Docker容器通过⽤户指定的⽹络模式，配置相应的⽹络资源。

本章从源码的⾓度，分析Docker容器从⽆到有的过程中，Docker容器⽹络创建的来⻰去脉。简化⽽⾔，Docker容器⽹络的创建流程如图7-2所⽰。

![](images/4c1181de5e795a114f7325d25ddd69714a74de1ce492f536d4b899aa45dec82c.jpg)  
图7-2 Docker容器⽹络创建流程图

本章分析的主要内容有以下5部分：

·Docker容器的⽹络模式  
·Docker Client配置容器⽹络   
·Docker Daemon创建容器⽹络流程  
·execdriver⽹络执⾏流程

# ·libcontainer实现内核态⽹络配置

在Docker容器的⽹络创建过程中，networkdriver模块使⽤并⾮是重点，故分析内容中不涉及networkdriver。不少读者肯定会有⼀些疑惑，为什么Docker容器的⽹络创建很少⽤到networkdriver。这⾥强调⼀下networkdriver在Docker中的作⽤：第⼀，为Docker Daemon创建⽹络环境的时候，初始化Docker Daemon的⽹络环境（详⻅第6章），⽐如创建docker0⽹桥等；第⼆，为Docker容器分配IP地址，为Docker容器做端⼝映射等。⽽与Docker容器⽹络创建有关的内容并不多，如只在桥接模式下，为Docker容器的⽹络接⼝设备分配⼀个IP地址。

# 7.2 Docker容器⽹络模式

如前所述，Docker可以为容器创建隔离的⽹络环境，在隔离的⽹络环境下，Docker容器使⽤独⽴的⽹络栈。看到这，很多读者也许会⾮常赞成以上的⾔论。然⽽，Docker容器⽹络的⾼级特性⼜会让爱好者们感受到其中暗藏的更多⽞机。

直奔主题，其实Docker除可以为Docker容器创建隔离的⽹络环境之外，同样有能⼒为Docker容器创建共享的⽹络环境。换⾔之，当开发者需要Docker容器与宿主机或者其他容器⽹络隔离，Docker可以满⾜这样的需求；当开发者需要Docker容器的⽹络处于共享的⽹络环境时，Docker同样可以满⾜这样的需求，并且⽹络共享，可以是Docker容器与宿主机之间⽹络共享，也可以是Docker容器与其他容器之间⽹络共享。神奇的是，Docker还可以实现不为Docker容器创建⽹络环境。

通过以上描述，可以总结出Docker容器共有以下4种⽹络模式：bridge桥接模式、host模式、other container模式和none模式。下⾯分别介绍Docker的4种⽹络模式。

# 7.2.1 bridge桥接模式

Docker容器的bridge桥接模式是⽬前Docker开发者中使⽤最为⼴泛的⽹络模式。bridge桥接模式可以使Docker容器独⽴使⽤⽹络栈，或者说只有在容器内部的进程，才能使⽤该⽹络栈。bridge桥接模式的实现步骤如下。

1）利⽤veth pair技术，在宿主机上创建两个虚拟⽹络接⼝，假设为veth0和veth1。⽽veth pair技术的特性可以保证⽆论哪⼀个veth接收到⽹络报⽂，都会将报⽂传输给另⼀⽅。  
2）Docker Daemon将veth0附加到Docker Daemon创建的docker0⽹桥上。保证宿主机的⽹络报⽂有能⼒发往veth0。  
3）Docker Daemon将veth1添加到Docker容器所属的⽹络命名空间（namespaces）下，veth1在Docker容器看来就是eth0。⼀⽅⾯，保证宿主机的⽹络报⽂若发往veth0，可以⽴即被veth1收到，实现宿主机到Docker容器之间⽹络的联通性；另⼀⽅⾯，保证Docker容器单独使⽤veth1，实现容器之间以及容器与宿主机之间⽹络环境的隔离性。

Docker容器的bridge桥接模式如图7-3所⽰。

![](images/e47980d5b1161125751eb994b85f3cef95d33fec4f28fd584fd88d607b521555.jpg)  
图7-3 Docker容器bridge桥接模式⽰意图

bridge桥接模式，从原理上实现了Docker容器到宿主机乃⾄其他机器的⽹络联通性。然⽽，由于宿主机的IP地址与veth pair的IP地址不属于同⼀个⽹段，故仅仅依靠veth pair和⽹络命名空间的技术，还不⾜以使宿主机以外的⽹络主动发现Docker容器的存在。为使Docker容器有能⼒让宿主机以外的世界感受到容器内部暴露的服务，Docker采⽤NAT（Network Address Translation，⽹络地址转换）的⽅式让宿主机以外的世界可以将⽹络报⽂发送⾄容器内部。简要来讲，当Docker容器需要暴露服务时，内部服务必须监听容器IP和容器内部端⼝号port_0，以便外界主动发起访问请求。由于宿主机以外的世界，只知道宿主机eth0的⽹络地址，并不知道Docker容器的IP地址，哪怕就算知道Docker容器的IP地址，从⼆层⽹络的⾓度来讲，外界也⽆法直接通过Docker容器的IP地址访问容器内部的服务。因此，Docker使⽤NAT⽅法，将容器内部的服务与宿主机的某⼀个端⼝port_1进⾏“绑定”。

如此⼀来，外界访问Docker容器内部服务的流程为：

1）外界访问宿主机的IP以及宿主机的端⼝port_1。

2）当宿主机接收到这类请求之后，由于存在DNAT规则，会将该请求的⽬的IP（宿主机eth0的IP）和⽬的端⼝port_1进⾏替换，替换为容器IP和容器端⼝port_0。  
3）由于能够识别容器IP，故宿主机可以将请求发送给veth pair。  
4）veth pair将请求发送⾄容器，容器交于内部服务进⾏处理。

使⽤DNAT⽅法，可以使Docker宿主机以外的世界主动访问Docker容器。那么Docker容器如何访问宿主机以外的世界呢？以下简要分析Docker容器内部访问宿主机以外世界的流程：

1）Docker容器内部进程获悉宿主机外部服务的IP地址和端⼝port_2，于是Docker容器发起请求。容器独⽴的⽹络环境保证了请求中报⽂的源IP地址为容器IP（即容器内部eth0，veth pair⼀⽅的IP地址），另外Linux内核会⾃动为进程分配⼀个可⽤端⼝（假设为port_3）。  
2）请求通过容器内部eth0发送⾄veth pair的另⼀端，也就是到达⽹桥docker0处。  
3 ） docker0 ⽹ 桥 开 启 了 数 据 报 转 发 功 能（/proc/sys/net/ipv4/ip_forward），故docker0将请求发送⾄宿主机的eth0处。  
4）宿主机处理请求时，使⽤SNAT对请求进⾏源地址IP替换，即将请求中源地址IP（容器eth0的IP地址）替换为宿主机eth0的IP地址。  
5）宿主机将经过SNAT处理后的报⽂通过请求的⽬的IP地址（宿主机以外世界的IP地址）发送⾄外界。

在这⾥，很多⼈肯定要问：对于Docker容器内部主动发起对外的⽹络请求，请求到达宿主机进⾏SNAT处理发给外界之后，当外界响应请求时，响应报⽂中的⽬的IP地址肯定是Docker Daemon所在宿主机的IP地址，那响应报⽂回到宿主机的时候，宿主机⼜是如何转给Docker容器的呢？关于这样的响应，由于没有做相应的DNAT转换，原则上不会被发送⾄容器内部。为什么说对于这样的响应，不会做DNAT转换呢。原因很简单，DNAT转换是针对特定容器内部服务监听的特定端⼝做的，该端⼝是供服务监听使⽤，⽽容器内部发起的请求报⽂中，源端⼝号肯定不会占⽤服务监听的端⼝，故容器内部发起请求的响应不会在宿主机上经过DNAT处理。

其实，这⼀环节的关键在于iptables，具体的iptables规则如下：

iptables -I FORWARD -o docker0 -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT

此iptables规则的意思是：在宿主机上发往docker0⽹桥的⽹络数据报⽂，如果该数据报⽂所处的连接已经建⽴，则⽆条件接受，并由Linux内核将其转⾄原来的连接上，即回到Docker容器内部。

以上便是Docker容器中bridge桥接模式的简要介绍。可以说，bridger桥接模式实现了两个⽅⾯：第⼀，让容器内部使⽤独⽴的⽹络栈；第⼆，让容器和宿主机以外的世界通过NAT建⽴通信。从功能的⾓度来讲，恰似具备了传统情况下隔离环境中⽹络的功能。然⽽，从使⽤的⾓度来讲，这种⽅式还没有弥合传统⽹络环境与容器⽹络环境的缝隙，换⾔之，NAT⽅式必须使得容器服务在宿主机上有⼀层抽象，这层抽象需要Docker使⽤者⼈为的⼲涉，另外使⽤NAT⽅式，仅仅是三层⽹络上的实现⼿段，影响⽹络传输效率的同时，也会为⽹络的隔离带来不便。

# 7.2.2 host模式

Docker容器中的host模式与bridge桥接模式是完全不同的模式。最⼤的区别是：host模式并没有为容器创建⼀个隔离的⽹络环境。之所以称之为host模式，是因为该模式下的Docker容器会和host宿主机使⽤同⼀个⽹络命名空间，故Docker容器可以和宿主机⼀样，使⽤宿主机的eth0和外界进⾏通信。如此⼀来，Docker容器的IP地址⾃然也是宿主机eth0的IP地址。

Docker容器的host⽹络模式如图7-4所⽰。

图7-4中最左侧的Docker容器，即采⽤了host⽹络模式，⽽其他两个Docker容器依然沿⽤brdige桥接模式，两种模式存在于同⼀个宿主机上并不⽭盾。

![](images/f0120ec8b1b4c3f8e097550752b861aea13db98695ce671379b38d685ee50a5a.jpg)  
图7-4 Docker容器host⽹络模式⽰意图

Docker容器的host⽹络模式在实现过程中，由于不需要额外的⽹桥以及虚拟⽹卡，故不会涉及docker0以及veth pair。上⽂namespace的介

绍 中 曾 经 提 到 ： ⽗ 进 程 在 创 建 ⼦ 进 程 的 时 候 ， 如 果 不 使 ⽤CLONE_NEWNET这个参数标志，那么创建出的⼦进程会与其⽗进程共享同⼀个⽹络命名空间。Docker就是采⽤了这个简单的原理，在创建进程启动容器的过程中，没有传⼊CLONE_NEWNET参数标志，实现Docker容器与宿主机共享同⼀个⽹络环境，⽹络模式即为host⽹络模式。

Docker容器的⽹络模式中，host模式是bridge桥接模式很好的补充。采⽤host模式的Docker容器，可以直接使⽤宿主机的IP地址与外界进⾏通信，若宿主机的eth0是⼀个公有IP，那么容器的IP也拥有这个公有IP。同时容器内服务的端⼝也可以使⽤宿主机的端⼝，⽆需额外进⾏NAT转换。有这样的⽅便，⾃然会折损部分其他的特性，最明显的即是Docker容器⽹络环境的隔离性。另外，使⽤host模式的Docker容器虽然可以让容器内部的服务⽆差别、⽆改造的使⽤，但是由于⽹络的⾮隔离性，⼀旦宿主机上多个Docker容器都采⽤了host模式，并且内部的服务都需要占⽤相同的宿主机端⼝资源，此时会造成宿主机上端⼝资源的竞争，势必影响容器的服务质量。

# 7.2.3 other container模式

Docker容器的other container⽹络模式是Docker中⼀种较为特别的⽹络模式。之所以称为“other container模式”，是因为这个模式下的Docker容器，会使⽤其他容器的⽹络环境。之所以称为“特别”，是因为这个模式下容器的⽹络隔离性会处于bridge桥接模式与host模式之间。Docker容器共享其他容器的⽹络环境，则⾄少这两个容器之间不存在⽹络隔离，⽽这两个容器⼜与宿主机以及除此之外其他的容器存在⽹络隔离。

Docker容器的other container⽹络模式如图7-5所⽰。

图7-5中右侧的Docker容器即采⽤了other container⽹络模式，它具体能感受到⽹络环境即为左侧Docker容器的bridge桥接模式。

在Docker容器的other container⽹络模式实现过程中，不涉及⽹桥，同样也不需要创建虚拟⽹卡veth pair。完成other container⽹络的创建只需要两个步骤：

1）查找other container（即需要被共享⽹络环境的容器）的⽹络命名空间。  
2）新创建的Docker容器的⽹络命名空间使⽤其他容器的⽹络命名空间。

![](images/fef584b3af4225f1282f2bc76f243b42156a2e0423bddb36d669ae52a38a29ba.jpg)  
图7-5 Docker容器other container⽹络模式⽰意图

Docker容器的other container⽹络模式，可以⽤来更好地服务于容器间的通信。需要互相通信的Docker容器在这种模式下，虽然⽹络隔离性没有bridge桥接模式优秀，但是相⽐host模式⾃然要好不少，同时⽹络隔离性稍有弱化的背后，带来的却是容器间⾼效的传输效率，以及容器间互访时简约的⽹络配置，即只需要使⽤localhost来访问其他容器。同时，多个容器共享同⼀个⽹络命令空间的形式，使得这部分容器的粘性⼤⼤提⾼。在容器的⼤规模调度与管理中，容器组必然会是⼀个需要攻克的技术点，⽽other container⽹络模式似乎为容器组的实现提供了⽅向。值得思考的是，Google推出的Kubernetes平台中的Pod的原理，即使⽤了容器间共享⽹络命名空间的思路，实现了组的概念。

# 7.2.4 none模式

Docker容器的第4种⽹络模式是none模式。顾名思义，⽹络环境为none，即不为Docker容器创建任何的⽹络环境。⼀旦Docker容器采⽤了none⽹络模式，那么容器内部就只能使⽤loopback⽹络接⼝，不会再有其他的⽹络资源。

可以说none模式为Docker容器做了极少的⽹络设定，但是俗话说得好“少即是多”，在没有⽹络配置的情况下，作为Docker开发者，才能在这种模式下做⽆限多可能的⽹络定制开发。这也恰恰体现了Docker开放的设计理念。

⾄此，Docker的4种⽹络模式的介绍就告⼀段落，下⽂带来Docker⽹络模式的创建流程分析。

# 7.3 Docker Client配置容器⽹络模式

Docker容器⽹络模式的多样性，极⼤地满⾜了Docker⽤户部署应⽤的⽹络需求。Docker⽤户，或者基于Docker的⼆次开发者，均可以在这基础上，选择使⽤与⾃⾝应⽤最为贴切的⽹络模式。

从图7-2中可以看到，Docker容器⽹络创建流程中第⼀个涉及的Docker模块就是Docker Client。当然，这也容易理解，毕竟Docker容器⽹络环境的创建需要由⽤户发起。⽤户根据⾃⾝对容器的需求，选择⽹络模式，并将其通过Docker Client传递给Docker Daemon。本⼩节将从Docker Client源码的⾓度出发，分析如何通过参数的形式配置Docker容器的⽹络模式，以及Docker Client内部如何处理这些⽹络模式参数。

# 7.3.1 使⽤Docker Client

Docker 架 构 中 ， ⽤ 户 完 全 可 以 使 ⽤ Docker Client 来 配 置 DockerContainer的⽹络模式。实际情况下，启动⼀个容器，Docker才会为Docker容器创建⽹络环境，故配置⽹络模式在⽤户执⾏“启动容器”命令之后完成。启动容器命令为docker run，通过Docker Client发起，使⽤⽅ 式 如 下 所 ⽰ （ 其 中 NETWORKMODE 为 四 种 ⽹ 络 模 式 之 ⼀ ，IMAGE_NAME为镜像名称）：

docker run -it--net NETWORKMODE IMAGE_NAME /bin/bash

执⾏以上命令时，Docker⾸先创建⼀个Docker Client，再由DockerClient解析整条命令的请求内容，最终解析为run命令，并发送⾄DockerDaemon。Docker Client的相关实现详⻅本书第2章。

# 7.3.2 runconfig包解析

解析出run命令之后，Docker Client调⽤相应的处理函数CmdRun处理 关 于 run 请 求 的 具 体 内 容 。 CmdRun 函 数 的 实 现 位于./docker/docker/api/client/commands.go#L1990。CmdRun执⾏的第⼀步就是：通过runconfig包中的ParseSubcommand函数执⾏解析出相应的config、hostConfig，以及cmd对象，源码实现如下：

```go
config, hostConfig, cmd, err := runconfig.ParseSubcommand(cl.Subcmd("run", [OPTIONS] IMAGE [COMMAND] [ARG...]", "Run a command in a new container"), args, nil) 
```

其 中 ， config 的 类 型 为 Config 结 构 体 ， hostConfig 的 类 型 为HostConfig 结 构 体 ， 两 种 类 型 的 定 义 均 位 于 runconfig 包 。 Config 与HostConfig类型同⽤以描述Docker容器的配置信息，然⽽两者之间⼜有着本质的区别，它们各⾃的描述如下：

·Config结构体：描述Docker容器独⽴的配置信息。独⽴的含义是：Config这部分信息描述的是容器本⾝，⽽不会与容器所在宿主机相关。

·HostConfig结构体：描述Docker容器与宿主机相关的配置信息。

# 1.Config结构体

Config结构体描述Docker容器本⾝的属性信息，⽽结构体内的所有属性正是说明了这⼀点，结构体的定义如下：

```txt
type Config struct {  
    Hostname   string  
    Domainname   string  
    User   string 
```

```txt
Memory int64 // Memory limit (in bytes)  
MemorySwap int64 // Total memory usage (memory + swap); set '-1' to disable //swap  
CpuShares int64 // CPU shares (relative weight vs. other containers)  
Cpuset string // Cpuset 0-2, 0,1  
AttachStdin bool  
AttachStdout bool  
AttachStderr bool  
PortSpecs [string // Deprecated - Can be in the format of 8080/tcp  
ExposedPorts map[nat.Port]struct{}  
Tty bool // Attach standard streams to a tty, including stdin if // it is not closed.  
OpenStdin bool // Open stdin  
StdinOnce bool // If true, close stdin after the 1 attached client // disconnects.  
Env [string]  
Cmd [string]  
Image string // Name of the image as it was passed by the // operator (eg. could be symbolic)  
Volumes map[string]struct{}  
WorkingDir string  
Entrypoint [string]  
NetworkDisabled bool  
OnBuild [string] 
```

Config结构体中各属性的详细说明如表7-1所⽰。

表7-1 Config结构体属性介绍表

<table><tr><td>Config 结构体属性名</td><td>类型</td><td>代表含义</td></tr><tr><td>Hostname</td><td>string</td><td>容器主机名</td></tr><tr><td>Domainname</td><td>string</td><td>域名服务器名称</td></tr><tr><td>User</td><td>string</td><td>容器内用户名</td></tr><tr><td>Memory</td><td>int64</td><td>容器的内存使用上限(单位:字节)</td></tr><tr><td>MemorySwap</td><td>int64</td><td>容器所有的内存使用上限(物理内存+交互区),关闭交互区支持置为-1</td></tr><tr><td>CpuShares</td><td>int64</td><td>容器CPU使用share值,其他容器的相对值</td></tr><tr><td>Cpuset</td><td>string</td><td>CPU核的使用集合</td></tr><tr><td>AttachStdin</td><td>bool</td><td>是否附加标准输入</td></tr><tr><td>AttachStdout</td><td>bool</td><td>是否附加标准输出</td></tr><tr><td>AttachStderr</td><td>bool</td><td>是否附加标准出错</td></tr><tr><td>PortsSpecs</td><td>[ ]string</td><td>已弃用</td></tr><tr><td>ExposedPorts</td><td>map[nat.Port]struct{}</td><td>容器内部暴露的端口号</td></tr><tr><td>Tty</td><td>bool</td><td>是否分配一个伪终端 tty</td></tr><tr><td>OpenSttin</td><td>bool</td><td>在没有附加标准输入时,是否依然打开标准输入</td></tr><tr><td>StdinOnce</td><td>bool</td><td>若为真,表示当一个客户关闭标准输入后关闭容器的标准输入</td></tr><tr><td>Env</td><td>[ ]string</td><td>容器的环境变量,可以有多个</td></tr><tr><td>Cmd</td><td>[ ]string</td><td>容器内运行的指令(一个或多个)</td></tr><tr><td>Image</td><td>string</td><td>容器 rootfs 所依赖的镜像名称</td></tr><tr><td>Volumes</td><td>map[string]struct{}</td><td>容器从宿主机上挂载的目录</td></tr><tr><td>WorkingDir</td><td>string</td><td>容器内部进程的指定工作目录</td></tr><tr><td>Entrypoint</td><td>[ ]string</td><td>覆盖镜像中默认的 ENTRYPOINT</td></tr><tr><td>NetworkDisabled</td><td>bool</td><td>是否关闭容器网络功能</td></tr><tr><td>OnBuild</td><td>[ ]string</td><td>指定的命令在构建镜像时不执行,而是在镜像构建完成之后被触发执行</td></tr></table>

# 2.HostConfig结构体

HostConfig结构体描述Docker Container与宿主机相关的属性信息，结构体的定义如下：

```txt
type HostConfig struct {  
    Binds     [string]  
    ContainerIDFile   string  
    LxcConf     [ ]utils.KeyValuePair  
    Privileged   bool  
    PortFindings   nat.PortMap  
    Links     [ ]string  
    PublishAllPorts   bool 
```

```htaccess
Dns []string  
DnsSearch []string  
VolumesFrom []string  
Devices []DeviceMapping  
NetworkMode NetworkMode  
CapAdd []string  
CapDrop []string  
RestartPolicy RestartPolicy 
```

Config结构体中各属性的详细说明如表7-2所⽰。

表7-2 HostConfig结构体属性介绍表  

<table><tr><td>HostConfig 结构体属性名</td><td>类型</td><td>代表含义</td></tr><tr><td>Binds</td><td>[]string</td><td>从宿主机上绑定到容器的 volumes</td></tr><tr><td>ContainerIDFile</td><td>string</td><td>文件名，文件用以写入容器的 ID</td></tr><tr><td>LxcConf</td><td>[]utils.KeyValuePair</td><td>添加自定义的 lxc 选项键值对</td></tr><tr><td>Privileged</td><td>bool</td><td>是否将容器设置为特权模式</td></tr><tr><td>Port bindings</td><td>nat.PortMap</td><td>容器绑定到宿主机的端口</td></tr><tr><td>Links</td><td>[]string</td><td>与其他容器之间的 link 信息</td></tr><tr><td>PublishAllPorts</td><td>bool</td><td>是否在宿主机上映射容器所有的端口信息</td></tr><tr><td>Dns</td><td>[]string</td><td>自定义的 DNS 服务器地址</td></tr><tr><td>DnsSearch</td><td>[]string</td><td>自定义的 DNS 查找服务器地址</td></tr><tr><td>VolumesFrom</td><td>[]string</td><td>将自身 volumes 挂载到此容器的容器</td></tr><tr><td>Devices</td><td>[]DeviceMapping</td><td>为容器添加一个或多个宿主机设备</td></tr><tr><td>NetworkMode</td><td>NetworkMode</td><td>为容器设置的网络模式</td></tr><tr><td>CapAdd</td><td>[]string</td><td>为容器用户添加一个或多个 Linux Capabilities</td></tr><tr><td>CapDrop</td><td>[]string</td><td>为容器用户禁用一个或多个 Linux Capabilities</td></tr><tr><td>RestartPolicy</td><td>RestartPolicy</td><td>当一个容器退出时采取的重启策略</td></tr></table>

# 3.runconfig解析⽹络模式

分析完Config与HostConfig结构体之后，回到runconfig包中分析Docker Client如何解析与Docker容器⽹络模式相关的配置信息，并将这部分信息传递给config实例与hostConfig实例。

runconfig包中的ParseSubcommand函数调⽤parseRun函数完成命令请 求 的 分 析 ， 实 现 代 码 位 于 ./docker/docker/runconfig/parse.go#L37-L39，如下所⽰：

进⼊parseRun函数即可发现，该函数完成了四⽅⾯的⼯作：

·定义与容器配置信息相关的flag参数。  
·解析docker run命令后紧跟的请求内容，将请求内容全部保存⾄flag参数中。  
·通过flag参数验证参数的有效性，并处理得到Config结构体与HostConfig结构体需要的属性值。  
· 创 建 并 初 始 化 Config 类 型 实 例 config 、 HostConfig 类 型 实 例hostConfig，最终返回config、hostConfig与cmd。

本章分析Docker容器的⽹络模式，⽽parseRun函数中有关容器⽹络模 式 的 flag 参 数 有 flNetwork 与 flNetMode ， 两 者 的 定 义 分 别 位于 ./docker/docker/runconfig/parse.go#L62

与./docker/runconfig/parse.go#L75，如下所⽰：

```txt
flNetwork = cmd Bool([]string[#n", "#-networking"], true, "Enable networking for this container")  
flNetMode = cmd.String([]string{-net}], "bridge", "Set the Network mode for the container\n'bridge': creates a new network stack for the container on the docker bridge\n'none': no networking for this container\n'container: <name|id>': reuses another container network stack\n'host': use the host network stack inside the container. Note: the host mode gives the container full access to local system services such as D-bus and is therefore considered insecure.") 
```

可⻅flag参数flNetwork表⽰是否开启容器的⽹络模式，若为true则开启，说明需要给容器创建⽹络环境；否则不开启，说明不给容器赋予⽹络功能。此flag参数的默认值为true，另外使⽤该flag的⽅式为：在

docker run 之 后 设 定 --networking 或 者 -n ， 由 于 flNetwork 的 名 称 为{"#n"，"#-networking"}，故可以判定此flag已经处于弃⽤状态。

另⼀个flag参数flNetMode则表⽰⽤户为容器设定的⽹络模式，共有四种选项分别是：bridge、none、container：<name|id>和host。四种模式的作⽤上⽂已经有所介绍，此处不再赘述。使⽤此flag的⽅式为：在docker run之后设定--net，如：

docker run -it --net host IMAGE_NAME /bin/bash

⽤户使⽤docker run启动容器时设定了以上两个flag参数（实际只有flNetMode⼀个flag参数），则runconfig包会解析出这两个flag的值。最终 ， 通 过 flag 参 数 flNetwork ， 得 到 Config 类 型 实 例 config 的 属 性NetworkDisabled；通过flag参数flNetMode，得到HostConfig类型实例hostConfig的属性NetworkMode。

函数parseRun返回config、hostConfig与cmd，代表着runconfig包解析配置参数⼯作的完成，下⼀节将回到CmdRun的运⾏。

# 7.3.3 CmdRun执⾏

在runconfig包中，Docker Client已经将有关容器⽹络模式的配置置于config对象与hostConfig对象，故在CmdRun函数的执⾏中，更多的是基于config对象与hostConfig参数的配置信息处理，⽽没有其他的容器⽹络处理部分。

CmdRun的主要⼯作是：利⽤Docker Daemon暴露的RESTful API接⼝，将docker run的请求发送⾄Docker Daemon。CmdRun执⾏过程中Docker Client与Docker Daemon的简易交互如图7-6所⽰。

从 CmdRun 的 执 ⾏ 流 程 中 ， 我 们 可 以 发 现 ： 在 解 析 config 、hostConfig与cmd之后，Docker Client⾸先发起请求create container。若Docker Daemon关于该容器的镜像存在，则⽴即执⾏create container操作并返回请求响应；Docker Client 收到响应后，再发起请求startcontainer。若容器镜像还不存在，Docker Daemon返回⼀个404的错误，表⽰镜像不存在；Docker Client收到错误响应之后，再发起⼀个请求pull image，使Docker Daemon⾸先下载镜像，下载完之后Docker Client再次发起请求create container，Docker Daemon创建完之后，DockerClient最终发起请求start container。

![](images/db523b0b1469c831172efba8a1add4e6c9acff0b2c59a1461bd06b6960e56463.jpg)  
图7-6 Docker Client与Docker Daemon交互图

其中关于Docker容器⽹络模式的参数配置均存储于config与hostConfig对象之中，在请求create container和start container发起后，随请求⼀起发送⾄Docker Daemon。

# 7.4 Docker Daemon创建容器⽹络流程

Docker Daemon接收到Docker Client的请求可以分为两次，第⼀次为create container，第⼆次为start container。这两次请求的执⾏过程，都与Docker容器的⽹络相关。以下按照这两个请求的执⾏，具体分析Docker容器⽹络的创建。Docker Daemon如何通过Docker Server解析RESTful请求，并完成路由，在第5章已经详细分析过，故本章不再赘述。

# 7.4.1 创建容器之⽹络配置

Docker Daemon创建容器主要执⾏了create container操作。

创 建 容 器 过 程 中 ， Docker Daemon ⾸ 先 通 过 runconfig 包 中 的ContainerConfigFromJob函数，解析出请求中的config对象，解析过程代码如下：

```autohotkey
config := runconfig(ContainerConfigFromJob(job)
```

⾄此，Docker Client处理得到的config对象，已经传递⾄DockerDaemon的config对象，config对象中已经含有属性NetworkDisabled的具体值。

⽽容器创建的⼯作内容主要有以下两点：

1）创建与Docker容器对应的Container类型实例container。  
2）创建Docker容器的rootfs。

具体的源码实现位于./docker/docker/daemon/create.go#L73-L78，如下所⽰：

```javascript
if container, err = daemon.newContainer(name, config, img); err != nil { return nil, nil, err } if err := daemon.createRootfs/container, img); err != nil { return nil, nil, err } 
```

与Docker容器⽹络模式配置相关的内容主要位于第⼀点，即创建container 实 例 中 。 newContainer 函 数 的 定 义 位于./docker/docker/daemon/daemon.go#L516-L550，具体的container实例如下：

```txt
container := &Container{  
ID: id,  
Created: time.Now().UTC(),  
Path: entrypoint,  
Args: args, //FIXME: de-duplicate from config  
Config: config,  
hostConfig: &runconfig.HostConfig{}  
Image: img.ID, // Always use the resolved image id  
NetworkSettings: &NetworkSettings {},  
Name: name,  
Driver: daemon.driver.String(),  
ExecDriver: daemon.execDriver.Name(),  
State: NewState(),  
} 
```

在container对象中，config对象直接赋值给container对象的Config属性，另外hostConfig属性与NetworkSettings属性均为空。其中hostConfig对象将在start container请求执⾏过程中被赋值，NetworkSettings类型的作 ⽤ 是 描 述 容 器 的 ⽹ 络 具 体 信 息 ， 定 义 位于 ./docker/docker/daemon/network_settings.go#L11-L18 ， 源 码 如 下 所⽰：

```txt
type NetworkSettings struct {  
    IPAddress string  
    IPPrefixLen int  
    Gateway string  
    Bridge string  
    PortMapping map[string]PortMapping // Deprecated 
```

Networksettings类型的各属性详细说明如表7-3所⽰。

表7-3 NetworkSettings属性介绍表  

<table><tr><td>NetworkSettings 属性名称</td><td>类型</td><td>含义</td></tr><tr><td>IPAddress</td><td>string</td><td>容器网络接口的 IP 网络地址</td></tr><tr><td>IPPrefixLen</td><td>int</td><td>网络标识位长度</td></tr><tr><td>Gateway</td><td>string</td><td>容器的默认网关地址</td></tr></table>

（续）

<table><tr><td>NetworkSettings 属性名称</td><td>类型</td><td>含义</td></tr><tr><td>Bridge</td><td>string</td><td>容器网络接口使用的网桥地址</td></tr><tr><td>PortMapping</td><td>map[string]PortMapping</td><td>容器与宿主机的端口映射</td></tr><tr><td>Ports</td><td>nat.PortMap</td><td>容器内部暴露的端口号</td></tr></table>

# 7.4.2 启动容器之⽹络配置

创 建 容 器 阶 段 ， Docker Daemon 创 建 了 容 器 对 象 container ，container对象内部的Config属性含有NetworkDisabled。创建容器完成之后，Docker Daemon应Docker Client的请求，需要执⾏create container的操作。这部分的执⾏同样需要Docker Server接收请求，并分发调度处理。

Docker Daemon启动容器主要执⾏了start container操作。

启 动 容 器 过 程 中 ， Docker Daemon ⾸ 先 通 过 runconfig 包 中 的ContainerHostConfigFromJob函数，解析出请求中的hostConfig对象，解析过程源码如下：

```txt
hostConfig := runconfig(ContainerHostConfigFromJob(job)
```

⾄ 此 ， Docker Client 处 理 得 到 的 hostConfig 对 象 ， 已 经 传 递 ⾄Docker Daemon 的 hostConfig 对 象 ， hostConfig 对 象 中 已 经 含 有 属 性NetworkMode具体值。

容器启动的所有⼯作，均由以下Start函数来完成，源码位于./docker/docker/daemon/start.go#L36-L38，如下所⽰：

```txt
if err := container.Start(); err != nil {
return job(Error("Cannot start container %s: %s", name, err)
} 
```

Start函数实现了容器的启动。更为具体的描述是：Start函数实现了进 程 的 启 动 ， 另 外 在 启 动 进 程 的 同 时 为 进 程 设 定 了 命 名 空 间（namespaces）并为进程做了资源的限制，从⽽保证进程以及之后进程

的⼦进程都会在相同的命名空间内，且受到相同的资源控制。如此⼀来，Start函数创建的进程，以及该进程之后的⼦进程，形成⼀个进程组，该进程组处于资源隔离和资源控制的环境中，我们习惯将这样的进程组环境称为容器，也就是这⾥的Docker容器。

回 到 Start 函 数 的 执 ⾏ ， 位于./docker/docker/daemon/container.go#L275-L320。Start函数的执⾏过程与Docker容器⽹络模式相关的主要有三部分：

·initializeNetwork，初始化container对象中与⽹络相关的属性。

·populateCommand ， 填 充 Docker 容 器 内 部 需 要 执 ⾏ 的 命 令 ，Command中含有进程启动命令，还含有容器环境的配置信息，也包括⽹络配置。

·container.waitForStart（），实现Docker容器内部进程的启动，进程启动之后，为容器创建⽹络环境等。

# 1.初始化容器⽹络配置

容器对象container中有属性hostConfig，属性hostConfig中有属性NetworkMode，初始化容器⽹络配置initializeNetworking（）的主要⼯作就是：通过NetworkMode属性为Docker容器的⽹络做相应的初始化配置⼯作。

Docker Container 的 ⽹ 络 模 式 有 四 种 ， 分 别 为 ： host 、 othercontainer、none以及bridge。initializeNetworking函数的执⾏完全覆盖了这四种模式。

initializeNetworking （ ） 函 数 的 源 码 实 现 位于./docker/docker/daemon/container.go#L881-L933，如下所⽰：

```go
func (container *Container) initializeNetworking() error {
var err error
if container.hostConfig.NetworkMode.IsHost() {
container.Config.Hostname, err = os.Hostname()
if err != nil {
return err
}
parts := strings.SplitN(config.Hostname, ".", 2)
if len-parts) > 1 {
container.Config.Hostname = parts[0]
container.Config.Domainname = parts[1]
}
content, err := ioutil.ReadFile("/etc/hosts")
if os.IsNotExist(err) {
return container.buildHostnameAndHostsFiles("");}
else if err != nil {
return err
}
if err := container.buildHostnameFile(); err != nil {
return err
}
paths, err := container.getRootResourcePath("hosts")
if err != nil {
return err
}
container.HostsPath = hostsPath
return ioutil.WriteFile(config.HostsPath, content, 0644)
} else if container.hostConfig.NetworkMode.IsContainer() {
// we need to get the hosts files from the container to join nc, err := container.getNetworkedContainer()
if err != nil {
return err
}
container.HostsPath = nc.HostsPath
container.ResolvConfPath = nc.ResolvConfPath
container.Config.Hostname = nc.Config.Hostname
container.Config.Domainname = nc.Config.Domainname 
```

```go
} else if container.daemon.configDISABLENetwork {
    container.Config.NetworkDisabled = true
    return container.buildHostnameAndHostsFiles("127.0.1.1")
}
} else {
    if err := container-AllocateNetwork(); err != nil {
        return err
    }
    return container.buildHostnameAndHostsFiles(container.NetworkSettings.IPPAddress)
}
return nil 
```

针对以上源码，下⾯以4种不同的⽹络模式分析initializeNetworking函数的作⽤。

第⼀，host⽹络模式。Docker容器⽹络的host模式意味着容器使⽤宿主机的⽹络环境。虽然Docker容器使⽤宿主机的⽹络环境，但这并不代表Docker容器可以拥有宿主机⽂件系统的视⾓，⽽host宿主机上有很多信息标识的是⽹络信息，故Docker Daemon需要将这部分标识⽹络的信息，从宿主机上添加到Docker容器内部的指定位置。这样的⽹络信息，主要有以下三种：

·宿主机的hostname⽂件，代表容器的主机名。  
·宿主机的hosts⽂件，代表容器的主机名配置⽂件。  
·宿主机resolv.conf⽂件，属于容器的域名⽂件。

由于Docker容器与宿主机共享⽹络，因此Docker容器的hostname⽂件、hosts⽂件以及resolv.conf⽂件原则上与宿主机上的这些⽂件内容应该保持⼀致。结果就是：Docker Daemon将宿主机的hostname⽂件、hosts⽂件以及resolv.conf内容写⼊Docker容器的指定⽬录下。⽬录⼀般

为/var/lib/docker/containers/<container_id>，当容器启动时，再将这部分⽂件挂载进容器内部的指定路径。

第⼆，other container⽹络模式。Docker容器的other container⽹络模式意味着：容器使⽤其他已经创建容器的⽹络环境。

Docker Daemon⾸先判断host⽹络模式，若不为host⽹络模式，则继续判断是否为other container模式。若Docker容器的⽹络模式为othercontainer（假设使⽤的-net参数为--net=container：17adef，其中17adef为容器ID），则⽤户必须指定被共享⽹络的Docker容器，此处容器ID为17adef。在这种情况下，Docker Daemon所做的执⾏操作包括两步。

第⼀步，从container对象的hostConfig属性中找出NetworkMode，并找到相应的容器，即17adef的容器对象container，实现源码如下：

```txt
nc, err := container.getNetworkedContainer() 
```

第 ⼆ 步 ， 将 17adef 容 器 对 象 的 HostsPath 、 ResolveConfPath 、Hostname和Domainname赋值给当前容器对象container，实现源码如下：

```txt
container_hostsPath = nc_hostsPath  
container.ResolvConfPath = nc.ResolvConfPath  
container.Config.Hostname = nc.Config.Hostname  
container.Config.Domainname = nc.Config.Domainname 
```

第三，none⽹络模式。Docker容器的none⽹络模式意味着不给该容器创建任何⽹络环境，容器只能使⽤127.0.1.1的环回接⼝。

Docker Daemon通过config属性的DisableNetwork来判断是否为none⽹络模式。实现源码如下：

```groovy
if container.daemon.configDISABLENetwork {
    container.Config.NetworkDisabled = true
    return container.buildHostnameAndHostsFiles("127.0.1.1")
} 
```

第四，bridge⽹络模式。Docker容器的bridge⽹络模式意味着为容器创建桥接⽹络模式。桥接模式使得Docker容器创建独⽴的⽹络环境，并通过“桥接”的⽅式实现Docker容器与外界的⽹络通信。

初始化bridge⽹络模式的配置，实现源码如下：

```go
if err := container-AllocateNetwork(); err != nil { return err } return container.buildHostnameAndHostsFiles(container.NetworkSettings.IPAddress) 
```

以上代码完成的内容主要也是两部分：第⼀，通过allocateNetwork函数为容器分配⼀个⽹络接⼝可⽤的IP地址，并将⽹络配置（包括IP、bridge、Gateway等）赋值给container对象的NetworkSettings；第⼆，通过NetworkSettings为容器创建hosts、hostname等⽂件。

# 2.创建容器Command信息

Docker 在 实 现 容 器 时 ， 在 源 码 层 次 使 ⽤ 了 Command 类 型 。Command是⼀个⾮常重要的概念，我们可以认为Command类型包含了两部分的内容：第⼀，运⾏容器内进程的外部命令exec.Cmd；第⼆，运⾏容器时启动进程需要的所有基础信息：包括容器进程组的使⽤资源、⽹络资源、使⽤设备、⼯作路径等。通过这两部分的内容，我们清楚如何创建容器内的进程，同时也清楚为容器创建什么样的环境。

⾸ 先 ， 我 们 先 来 看 Command 类 型 的 定 义 ， 位于./docker/docker/daemon/execdriver/driver.go#L84。通过分析Command

类型以及相关的其他数据结构类型，得到的Command类型关系如图7-7所⽰。

![](images/f28691ab872d148094c73d5a8c64fa0849db66ccb842a55d6db1098e33f1db97.jpg)  
图7-7 Command类型关系图

从Command类型关系图中，我们可以看到Command类型中第⼀个属性为exec.Cmd，即代表需要创建的进程具体的外部命令；同时，关于⽹络⽅⾯的属性有Network，Network的类型为*Network；关于Docker容器资源使⽤⽅⾯的属性为Resources，从Resource的类型来看，Docker⽬前能做的资源限制有4个维度，分别为内存，内存+Swap，CPU使⽤，CPU核使⽤；关于挂载的内容，有属性Mounts；等等。

简单介绍Command类型之后，回到Docker Daemon启动容器⽹络的源码分析。紧接在函数initializeNetworking之后，是populateCommand环节 populateCommand 的 函 数 实 现 位于 ./docker/docker/daemon/container.go#L191-L274 。 上 ⽂ 已 经 提 及 ，populateCommand的作⽤是创建execdriver包中的对象Command实例，该Command中既有启动容器进程的外部命令，同时也有众多容器环境的配置信息，包括⽹络。

本⼩节，分析populateCommand如何填充Command对象中的⽹络信息，其他内容的分析会在第12章和第13章进⾏展开。

Docker容器有四种⽹络模式，故populateCommand⾸先需要判断容器属于哪种⽹络模式，随后将具体的⽹络模式信息，写⼊Command对象 的 Network 属 性 中 。 查 验 Docker 容 器 ⽹ 络 模 式 的 源 码 位于./docker/docker/daemon/container.go#L204-L227，如下所⽰：

```go
parts := strings.SplitN(string(c.hostConfig_NETWORKMode),":",2)  
switch parts[0] {  
    case "none":  
        case "host":  
            en.HostNetworking = true  
    case "bridge",":":// empty string to support existing containers if !c.ConfigNETWORKDisabled { network := c.NetworkSettings  
            enInterface = &execdriver_NETWORKInterface{ Gateway: network.Gateway, Bridge: network.Bridge, IPAddress: network.IPAddress, IPPrefixLen: network IPPrefixLen, }  
    }  
    case "container": nc, err := c.getNetworkedContainer()  
    if err != nil { 
```

return err   
}   
en(ContainerID $=$ nc.ID   
default: return fmt.Errorf("invalid network mode:%s",c.hostConfig.NetworkMode)   
}

populateCommand⾸先通过hostConfig对象中的NetworkMode判断容器属于哪种⽹络模式。该部分内容涉及execdriver包中的Network，可参⻅Command类型关系图中的Network类型。若为none模式，则对于Network对象（即en，*execdriver.Network）不做任何操作。若为host模式，则将Network对象的HostNetworking置为true；若为bridge桥接模式，则⾸先创建⼀个NetworkInterface对象，完善该对象的Gateway、Bridge、IPAddress和IPPrefixLen信息，最后将NetworkInterface对象作为Network对象的中Interface属性的值；若为other container模式，则⾸先通过getNetworkedContainer（）函数获知被分享⽹络命名空间的容器，最后将容器ID，赋值给Network对象的ContainerID。由于bridge模式、host模式以及other container模式彼此互斥，故Network对象中Interface属性、ContainerID属性以及HostNetworking三者之中只有⼀个被赋值。当Docker容器的⽹络被查验之后，populateCommand将en实例Network属性的值，传递给Command对象。

# 3.启动容器内部进程

当为容器做好所有的配置之后，Docker Daemon需要真正意义上的启动容器。根据启动容器流程中涉及的Docker模块，Docker Daemon后续 环 节 中 实 现 启 动 容 器 的 请 求 会 被 发 送 ⾄ execdriver ， 再 经 过libcontainer，最后实现Linux内核级别的进程启动。

回到Docker Daemon的启动容器，daemon包中start函数的最后⼀步即 为 执 ⾏ container.waitForStart （ ） 。 waitForStart 函 数 的 定 义 位于./docker/daemon/container.go#L1070-L1082，源码如下：

```go
func (container *Container) waitForStart() error {
    container.monitor = newContainerMonitor/container, container.hostConfig.RestartPolicy)
    select {
        case <-container.monitor.startSignal:
            case err := <-utils.Go/container.monitor.Start):
                return err
            }
        return nil
    } 
```

以 上 源 码 运 ⾏ 过 程 中 ， Docker Daemon ⾸ 先 通 过 函 数newContainerMonitor返回⼀个初始化的containerMonitor对象，该对象中带有容器进程的重启策略。总体⽽⾔，containerMonitor对象⽤以监视容器进程的执⾏。容器进程指的是容器pid namespace内进程号为1的进程，这个进程的状态代表容器的状态。⼀旦该进程停⽌运⾏，则容器内部所有进程都将收到⼀个终⽌信号，最终导致容器的运⾏失败。如果containerMonitor中指定了进程的重启策略，那么⼀旦容器进程没有启动成功，Docker Daemon会使⽤重启策略来重启容器。如果在重启策略下，容器依然没有成功启动，那么containerMonitor对象会负责重置以及清除所有已经为容器准备好的资源，例如已经为容器分配好的⽹络资源（即IP地址），还有为容器准备的rootfs等。

waitForStart （）函数通过container.monitor.Start来实现容器的启动，进⼊./docker/docker/daemon/monitor.go#L100，可以发现启动容器进程位于./docker/docker/daemon/monitor.go#L136，源码如下：

```javascript
exitStatus, err = m/container.daemon.run(m/container, pipes, m.backup) 
```

以 上 源 码 实 际 调 ⽤ 了 daemon 包 中 的 Run 函 数 ， 位于./docker/daemon/daemon.go#L969-L971，如下所⽰：

```txt
func (daemon * Daemon) Run(c *Container, pipes *execdriver.Pipes, startCallback execdriver.StartCallback) (int, error) {
    return daemon.execDriverRUN(ccommand, pipes, startCallback)
} 
```

最终，Run函数中调⽤了execdriver中的Run函数来执⾏DockerContainer的启动命令。

⾄此，⽹络部分在Docker Daemon内部的执⾏已经结束，紧接着程序的运⾏陷⼊execdriver，进⼀步运⾏容器启动的相关步骤。

# 7.5 execdriver⽹络执⾏流程

Docker架构中execdriver的作⽤是启动容器内部进程，最终启动容器。⽬前，在Docker中execdriver作为执⾏驱动，可以有两种选项：lxc与native。其中，lxc驱动会调⽤lxc⼯具实现容器的启动，⽽native驱动会使⽤Docker官⽅发布的libcontainer来启动容器。

Docker Daemon启动过程中，execdriver的类型默认为native，故本章主要分析native驱动在执⾏启动容器时，如何处理⽹络部分。

在Docker Daemon启动容器的最后⼀步，即调⽤了execdriver的Run函数来执⾏。通过分析Run函数的具体实现，可知关于Docker容器的⽹络执⾏流程主要包括两个环节：

1）创建libcontainer的Config对象。  
2）通过libcontainer中的namespaces包执⾏启动容器。

将execdriver.Run函数的运⾏流程展开，与Docker容器⽹络相关的流程，如图7-8所⽰。

![](images/d58608885933b12fa4efbbb5e57543d48cebb4454033c97125917ae50bdd1ad3.jpg)  
图7-8 execdriver.Run执⾏流程图

# 7.5.1 创建libcontainer的Config对象

Run函数位于./docker/docker/daemon/execdriver/native/driver.go#L62-L168。进⼊Run函数的实现，⽴即可以发现该函数通过createContainer创建了⼀个container对象，源码如下：

```go
container, err := d.createContainer(c) 
```

其中c为Docker Daemon创建的execdriver.Command类型实例。以上源码中createContainer函数的作⽤是：使⽤execdriver.Command来填充libcontainer.Config。简要介绍libcontainer.Config的作⽤就是，它定义了在 ⼀ 个 容 器 化 的 环 境 中 执 ⾏ ⼀ 个 进 程 所 需 要 的 所 有 配 置 项 。createContainer函数使⽤Docker Daemon层创建的execdriver.Command，创建更底层libcontainer所需要的Config对象。从这个⾓度来看，execdriver 更 像 是 封 装 了 libcontainer 对 外 的 接 ⼝ ， 实 现 了 将 DockerDaemon特有的容器启动信息转换为底层libcontainer能真正使⽤的容器配置选项。libcontainer.Config类型与其内部对象之间的关系如图7-9所⽰。

createContainer 的 源 码 实 现 部 分 位于 ./docker/docker/daemon/execdriver/native/create.go#L23-L77 ， 如 下 所⽰：

```go
func (d *driver) createContainer(c *execdriver.Command) (*libcontainer.Config, error) {
    container := template.New()
    ...
    if err := d.createNetwork(container, c); err != nil {
        return nil, err
    }
} 
```

```lua
return container, nil } 
```

![](images/98c238f0c149d57bc23be82a2a053ba9a11651a6319282bbe855478c586ba4dc.jpg)  
图7-9 libcontainer.Config类型关系图

# 1.libcontainer.Config模板实例

从createContainer函数的实现以及execdriver.Run执⾏流程图中都可以 看 到 ， create-Container 所 做 的 第 ⼀ 个 操 作 就 是 执 ⾏ template.New（ ） ， 意 为 创 建 ⼀ 个 libcontainer.Config 的 实 例 容 器 。 其 中 ，template.New （ ） 的 定 义 位于./docker/docker/daemon/execdriver/native/template/default_template.go，主要的作⽤为返回libcontainer关于Docker容器的默认配置选项。

Template.New（）的源码实现如下：

```txt
func New() *libcontainer.Config {
container := &libcontainer.Config{
Capabilities: []
"CHOWN",
"DAC_OVERIDE",
"FSETID",
"FOWNER",
"MKNOD",
"NET_raw",
"SETGID",
"SETUID",
"SETFCAP",
"SETPCAP",
"NET_BIND_SERVICE",
"SYS_CHROOT",
"KILL",
"AUDIT_WRITE",
},
Namespaces: map[string]{
"NEWNS": true,
"NEWUTS": true,
"NEWIPC": true,
"NEWPID": true,
"NEWNET": true,
},
Cgroups: &cgroups.Cgroup{
Parent: "docker",
AllowAllDevices: false,
},
MountConfig: &libcontainermountConfig{},
}
if apparmor.IsEnabled(){
container.AppArmorProfile = "docker-default"
}
return container
} 
```

libcontainer.Config默认的模板对象，⾸先设定了Capabilities的默认项，如CHOWN、DAC_OVERRIDE、FSETID等；其次⼜为Docker容器所需设定的namespaces添加默认值，即需要创建5个命名空间，如NEWNS、NEWUTS、NEWIPC、NEWPID和NEWNET，其中不包括user namespace，另外与⽹络相关的namespace是NEWNET；最后设定了⼀些关于cgroup以及apparmor的默认配置。

Template.New（）函数最后返回类型为libcontainer.Config的实例container ， 该 实 例 中 只 包 含 默 认 配 置 项 ， 其 他 内 容 的 添 加 需 要createContainer的后续代码来完成。

# 2.createNetwork实现

在createContainer的实现流程中，为了完善container对象（类型为libcontainer.Config ） ， 后 续 还 有 很 多 步 骤 ， 如 与 ⽹ 络 相 关 的createNetwork函数调⽤，与Linux内核Capabilities相关的setCapabilities函数调⽤，与cgroups相关的setupCgroups函数调⽤，以及与挂载⽬录相关的setupMounts函数调⽤等。本⼩节主要分析createNetwork函数如何为container对象完善⽹络配置项。

createNetwork 函 数 的 定 义 位于./docker/docker/daemon/execdriver/native/create.go#L79-L124 ，该函数主要利⽤execdriver.Command中Network属性的内容，来判断如何创建libcontainer.Config中的Network属性（关于两种Network属性，可以参⻅图7-7和图7-9）。由于Docker容器的4种⽹络模式彼此互斥，故以上Network类型中Interface、ContainerID与HostNetworking最多只有⼀项会被赋值。

execdriver.Command中Network的类型定义如下：

```txt
type Network struct { Interface \*NetworkInterface Mtu int ContainerID string HostNetworking bool } 
```

在以上Network类型的基础上，我们分析createNetwork函数，其具体实现可以归纳为以下4个部分：

1）判断⽹络是否为host模式。  
2）判断⽹络是否为bridge桥接模式。  
3）判断⽹络是否为other container模式。  
4）为Docker容器添加loopback⽹络设备。

⾸先，我们来看execdriver判断容器⽹络是否为host模式的源码：

if c.Network.HostNetworking{ container.Namespaces["NEWNET"] $=$ false return nil   
}

当execdriver.Command类型实例中Network属性的HostNetworking为true，则说明需要为Docker容器创建host⽹络模式，使容器与宿主机共享相同的⽹络命名空间。在host模式的具体介绍中，我们已经阐明，只要Docker Daemon在创建容器进程，进⾏CLONE系统调⽤时，不传⼊CLONE_NEWNET参数标志即可实现共享⽹络命名空间。这部分源码正好准确地验证了这⼀点。Docker Daemon将container对象中代表⽹络命名空间的NEWNET设为false，最终导致libcontainer中不创建新的⽹络命名空间。

```go
if c.Network.Interface != nil {
    vethNetwork := libcontainer.Network{
        Mtu: c.Network.Mtu,
        Address: fmt.Sprintf("%s/%d", c.Network-interface.IPPAddress, c.Network-interface.IPPrefixLen),
        Gateway: c.Network-interface.Gateway,
        Type: "veth",
        Bridge: c.Network-interface.Bridge,
        VethPrefix: "veth",
    }
    container Networks = append/container Networks, &vethNetwork) 
```

当execdriver.Command类型实例中Network属性的Interface不为nil值，则说明需要为Docker容器创建bridge桥接模式，使容器拥有隔离的⽹络环境。于是，以上源码为libcontainer.Config的container对象添加Networks属性vethNetwork，⽹络接⼝类型为veth，以便libcontainer在执⾏时，可以为Docker容器创建veth pair。

接着来看execdriver判断容器⽹络是否为other container模式的代码：

```go
if c.Network.ContAINERID != "" { d.Lock() active := d.activeContainers[c.Network.ContAINERID] d.Unlock() if active == nil || active.cmd.Process == nil { return fmt.Errorf("%s is not a valid running container to join", c.Network.ContAINERID) } cmd := active.cmd nspath := filepath.Join("/proc", fmt.Sprint(cmd.Process.Pid), "ns", "net") container Networks = append/container Networks, &libcontainer.Network{ Type: "nets", 
```

```txt
NsPath: nspath,   
}）   
1
```

当execdriver.Command类型实例中Network属性的ContainerID不为空字符串时，则说明需要为Docker容器创建other container模式，使创建容器共享其他容器的⽹络环境。实现过程中，execdriver⾸先需要在activeContainers中查找需要被共享⽹络环境的容器active；并通过active容器的启动执⾏命令cmd找到容器主进程在宿主机上的PID；随后在proc⽂件系统中找到该进程PID的关于⽹络命名空间的路径nspath，也是整个容器的⽹络命名空间路径；最后为类型为libcontainer.Config的container对象添加Networks属性，Network的类型为netns。

此外，createNetwork函数还实现了为Docker容器创建⼀个loopback环回接⼝，以便容器可以实现内部通信。实现过程中，同样为类型libcontainer.Config的container对象添加Networks属性，Network的类型为loopback，源码如下：

container Networks $=$ []\*libcontainer.Network{ Mtu: c.Network.Mtu, Address: fmt.Sprintf("%s/%d", "127.0.0.1", 0), Gateway:"localhost", Type:"loopback", }   
}

⾄此，createNetwork函数已经把与⽹络相关的配置，全部创建在类型为libcontainer.Config的container对象中，随时等候创建容器进程的来临。

# 7.5.2 调⽤libcontainer的namespaces启动容器

回到execdriver.Run函数，创建完libcontainer.Config实例container，经过⼀系列处理之后，最终execdriver执⾏namespaces.Exec函数实现启动容器。启动过程中container对象依然是namespace.Exec函数中⼀个⾮常重要的参数。namespaces.Exec代表着execdriver把启动Docker容器的⼯作交给libcontainer，之后的程序执⾏将完全陷⼊libcontainer。

调 ⽤ namespaces.Exec 的 源 码 位于 ./docker/daemon/execdriver/native/driver.go#L102-L127 ， 为 了 便 于 理解，简化之后如下：

```go
namespaces.Exec container, c.Stdln, c STDout, c.Standard, c.Console, c.Rootfs, dataPath, args, parameter_1, parameter_2) 
```

# 其中parameter_1为定义的函数，如下所⽰：

```go
func container *libcontainer.Config, console, rootfs, dataPath, init string, child *os.File, args []string) *exec Cmd { c.Path = d.initPath c.Args = append([]string{ DriverName, "-console", console, "-pipe", "3", "-root", filepath.Join(d.root, c.ID), "--", }, args...) // set this to nil so that when we set the clone flags anything else is reset c.SysProcAttr = &syscall.SysProcAttr{ Cloneflags: uintptrnamespaces.GetNamespaceFlags/container.Namespaces)), } cExtraFiles = []*os.File{child} c.Env = container.Environment  
c.Dir = c.Rootfs 
```

```lua
return &cCmd } 
```

同样，parameter_2也为定义的函数，如下所⽰：

```lua
func() { if startCallback != nil { c.LabelerPid = c.Process.Pid startCallback(c) } } 
```

parameter_1 以 及 parameter_2 这 两 个 函 数 均 会 在 libcontainer 的namespaces中发挥很⼤的作⽤，7.6节将进⾏深⼊分析。

⾄此，execdriver模块的执⾏部分已经结束，Docker Daemon的运⾏陷⼊libcontainer。

# 7.6 libcontainer实现内核态⽹络