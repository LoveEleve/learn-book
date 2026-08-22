大家过年好，我是 华仔, 又跟大家见面了。

从今天开始我将为大家奉上 Kafka 源码剖析系列文章，正式开启 「**Kafka的源码之旅**」，跟我一起来掌握 Kafka 源码核心架构设计思想吧。

今天这篇我们先来聊聊 Kafka 源码环境搭建、源码全景图以及后续源码剖析之旅路线，带你梳理整体的源码分析脉络。

认真读完这篇文章，并准备一台电脑跟我一起操作，我相信你会对 Kafka 源码环境搭建以及全景图剖析以及源码剖析整体路线，有更加深刻的理解。

![](https://article-images.zsxq.com/Fs6Jf2fhPWp3uiaqsVLgJm8BoRge)

## **01 总体概述**

平常我们在基于 Kafka 做应用开发的时候，可能只是将 Kafka 作为一个消息系统来存取消息、抗高并发以及解耦系统，并不会接触到源码层面的知识。但是大家在使用或者运维 Kafka 时或多或少会遇到一些棘手的生产故障问题，如果你不了解 Kafka 源码层面的实现原理，那么在实际开发中排查问题故障点肯定会受到阻碍。

因此解决问题的最好办法就是学习和阅读源码，这样我们可以快速掌握 「**Kafka 的核心实现细节**」，来帮助我们更深刻的理解 Kafka 内部设计原理，通晓高吞吐、高可用、高并发系统架构如何设计的，另外可以帮助我们快速建立性能分析、故障问题的排查定位思路和解决问题的高效方法和调优方案，减少解决问题的时间成本。

## **02 Kafka 源码环境搭建**

## **版本说明**

在阅读 Kafka 源码之前，首先我们要做一些环境准备工作, Kafka 官方已经更新到 3.1.0版本，但是从 2.8.0 版本开始就去掉了 ZooKeeper 的依赖，生产环境使用不太稳定，因此这里我们将选用「**2.8.0**」版本作为源码研究的版本，后续都会以该版本来剖析源码。

## **环境准备**

1）Kafka版本：这里我们选用**2.8.0**版本。

2）JDK版本：这里我们选用**1.8**版本，自行安装。

3）Scala版本：由于Kafka Broker 端源码是基于Scala写的，这里我们选用**2.12**版本, 2.13 版本刚推出不久, 暂不适合。

4）Gradle版本：这里我们选用**6.8**版本，使用此来安装 kafka 源码环境。

5）Zookeeper版本：这里我们选用最新版**3.8.4**版本。

## **环境搭建**

以下环境需要在 linux 下执行，windows 下可能有问题，可以直接通过 wsl2 安装。

## **01 Scala 环境搭建**

[https://www.scala-lang.org/download/2.12.8.html](https://www.scala-lang.org/download/2.12.8.html)

[https://docs.scala-lang.org/zh-cn/tutorials/scala-for-java-programmers.html](https://docs.scala-lang.org/zh-cn/tutorials/scala-for-java-programmers.html)

[https://www.runoob.com/scala/scala-tutorial.html](https://www.runoob.com/scala/scala-tutorial.html)

![](https://article-images.zsxq.com/FiWCGqL8SA6dSKOWC3tq8pDQaih4)

下载并解压如下：

wget https://downloads.lightbend.com/scala/2.12.8/scala-2.12.8.tgz

![](https://article-images.zsxq.com/FrZ_b7a0GCsN_I5HcH4PbrCSI8Up)

开始配置 Scala 环境变量（以Ubuntu系统为例）：

sudo vim /etc/profile

\# 配置scala安装路径及环境变量

SCALA\_HOME=/home/wangjianghua/src/scala-2.12.8

export SCALA\_HOME

export PATH=$PATH:$SCALA\_HOME/bin

\# 使其生效

source /etc/profile

\# 验证scala环境是否生效

scala -version

出现下面提示，说明 scala 环境搭建成功：

![](https://article-images.zsxq.com/Fhl4lASIK26htlgeDJAZfBrETpKt)

## **02 Gradle 环境搭建**

[https://services.gradle.org/distributions/](https://services.gradle.org/distributions)

![](https://article-images.zsxq.com/FiszGEMo9w_KcCQ3ldG14Ly4i3QW)

安装包说明：

[gradle-x.x-bin.zip:](http://about:blank) 安装发布版

[gradle-x.x-src.zip:](http://about:blank) 源码版

[gradle-x.x-all.zip:](http://about:blank) 全部文件版

Gradle 下载的源码不需要进行安装，下载解压后即可：

wget https://services.gradle.org/distributions/gradle-6.8.1-all.zip

unzip gradle-6.8.1-all.zip

![](https://article-images.zsxq.com/FkGY8979CO7o8rjQ-52uRTZdZ2YW)

开始配置 Gradle 环境变量（以Ubuntu系统为例）：

sudo vim /etc/profile

\# 配置scala安装路径及环境变量

GRADLE\_HOME=/home/wangjianghua/src/gradle-6.8.1

export GRADLE\_HOME

export PATH=$PATH:$GRADLE\_HOME/bin

\# 使其生效

source /etc/profile

\# 验证gradle环境是否生效

gradle -v

出现下面提示，说明 Gradle 环境搭建成功：

  
![](https://article-images.zsxq.com/FnBnQDzlYw2hKFK_S3rXDaFkJf40)

## **03 Zookeeper 环境搭建**

Zookeeper 下载并解压如下：

\# 3.6.3 的目前不维护了，直接使用这个 3.8.4 稳定版

wget https://downloads.apache.org/zookeeper/stable/apache-zookeeper-3.8.4-bin.tar.gz

![](https://article-images.zsxq.com/FhuFPBU48YqeHIzvbLBPnL-nmEw0)

修改 Zookeeper 配置：

\# 进入配置目录

cd apache-zookeeper-3.8.4-bin/conf

\# 修改配置文件名称

mv zoo\_sample.cfg zoo.cfg

\# 如需修改日志目录可以更改zoo.cfg配置的datas路径

dataDir=/xxx/zookeeper/data

启动 Zookeeper 服务：

\# 进入执行目录【bin】

cd apache-zookeeper-3.8.4-bin/bin

\# 执行启动命令

./zkServer.sh start

出现下面提示，说明 Zookeeper 环境搭建成功：

![](https://article-images.zsxq.com/FoNWMZu7Qog0FKXeU9-_i-E8L_h4)

## **04 Kafka 源码环境搭建**

[http://kafka.apache.org/downloads](http://kafka.apache.org/downloads)

![](https://article-images.zsxq.com/FhSx3GWWtYyDSXu9npeSqTaP8Zpj)

**Kafka 源码依赖 Gradle 环境，本文使用 gradle 命令来构建 Kafka 源码环境。**

gradle 命令说明：

\# 下载并更新gradle 套件即 wrapper

gradle

\# 构建 jar包并运行

./gradlew jar

\# 构建项目，看你是idea工具还是eclipse

./gradlew idea

./gradlew eclipse

\# 构建源码包

./gradlew srcJar

\# 构建javadoc文档

./gradlew aggregatedJavadoc

\# 清理并构建

./gradlew clean

首先需要 git clone 并切换到对应的 2.8.0 版本：

![](https://article-images.zsxq.com/FqY6sVBFi8TCttER2P5NkvosyK4x)

![](https://article-images.zsxq.com/FpZvdJzSPZvZurKfRwy1IfdZ5q1G)

此时需要修改下 gradle 目录下的 dependencies.gradle 文件，否则报错：

![](https://article-images.zsxq.com/Fi4QcOX6GVS7xyuofW6QQvVRc9Rb)

![](https://article-images.zsxq.com/Fm-3x7GO-cnvLsocFDpaancImWla)

然后执行 gradle 命令就会构建成功，并更新 gradle wrapper 套件：

![](https://article-images.zsxq.com/Ft4ILRuAx8L93Okt8a3kZA13VjCC)

![](https://article-images.zsxq.com/FsvIZTp_10p76T9allJXj9TTkArs)

待执行完后，会生成 gradle 目录， 如果此时 Jar 包没有下载成功的话，可以从网盘下载后，切换到 gradle/wrapper 目录，将 Jar 包复制到该目录。

![](https://article-images.zsxq.com/FqWB6o-HD_zrSpyjgkWcQ8l6jbm4)

![](https://article-images.zsxq.com/FldjsLsKLvmfJ-c1BA3glr9t17GO)

为了将 kafka 源码导入到 IDEA 编辑器中，我们需要执行 gradle idea 命令，这个命令会下载 kafka 的相关依赖，耗时比较长，然后静静等待它执行完成，中间会下载各种 jar 包文件：

![](https://article-images.zsxq.com/Fhxoq1M9LkSzK5iOCVobHJ6CCysg)

![](https://article-images.zsxq.com/FkVerI5gfHnHDuSgm8CJAdX6fhmG)

执行成功之后，会有如下的输出：

![](https://article-images.zsxq.com/FpTfXyQ5UIkr7mxGDmoNmCXndqbZ)

待执行完后，最后将安装好的 kafka 源码导入到 IDEA 中，得到的项目结果如下图所示：

  
![](https://article-images.zsxq.com/Fi4zzwwZBkHgdRaHVV5jaolItlxe)

在 kafka 中，很多 request 和 response 类都是在编译过程中生成的，下面我们需要执行一下 ./gradlew jar 来生成这些类，该命令执行时间较长，请耐心等待。

./gradlew jar 命令执行完成之后，我们需要在 IDEA 中找到下图几个模块中的 generated 目录，并将其中 java 目录添加到 classpath 中:

\# 进入 kafka 目录执行

./gradlew jar #构建jar包,执行时间比较长,耐心等待

\# 上面使用 gradle idea 替代了

./gradlew idea #把 Kafka 源码导入到idea中,执行时间比较长,耐心等待

./gradlew eclipse #如果是使用的eclipse,可以执行此命令

开始执行 ./gradlew jar：

![](https://article-images.zsxq.com/FqxJOktaZrBt1_8Quo52yDlzqYGm)

查看 gradle/wrapper/gradle-wrapper.properties 内容如下：

![](https://article-images.zsxq.com/FmQ0ad8jkOv9_-3GKDbZuhrNJaCK)

其中 GRADLE\_USER\_HOME 的默认位置:

1.  当未显式定义时，Gradle 会根据操作系统自动设置：
2.  ​Linux/Mac​：~/.gradle（即 /home/用户名/.gradle）
3.  ​Windows​：C:\\Users\\用户名\\.gradle
4.  我的路径 wrapper/dists 的实际位置将是：/home/wangjianghua/.gradle/wrapper/dists。

查看执行结果，静静等待它完成：

![](https://article-images.zsxq.com/Fkv8gkaVeOa3KILtIzKb_qhtIncP)

目前我这里会报错：

![](https://article-images.zsxq.com/FuxPqIV8wEkuZ_wgqGCJ79j037uH)

根据最新的错误信息，核心问题是：​Kafka 项目尝试使用 /usr/lib/jvm/java-11-openjdk-amd64/bin/javac 编译器，但这个路径在您的系统中不存在。​

![](https://article-images.zsxq.com/FqMlUYijs3NI_Op0qGK86hQHQhN9)

执行查看 javac 路径：

which javac

\# 或

update-alternatives --list javac

如果路径不同，创建符号链接：

\# 这一步看你本地是否存在 如果存在不用创建

sudo mkdir -p /usr/lib/jvm/java-11-openjdk-amd64/bin

\# 这一步建立软连接

sudo ln -s $(which javac) /usr/lib/jvm/java-11-openjdk-amd64/bin/javac

![](https://article-images.zsxq.com/FiQZm9YWeP3KOhWNZ1aIBRc-Ib2L)

再次执行 ./gradlew jar

![](https://article-images.zsxq.com/Fp9qO5etO7FPZvflUJG1blhl0WWS)

./gradlew jar 命令执行完成之后，我们需要在 IDEA 中找到下图几个模块中的 generated 目录，并将其中 java 目录添加到 classpath 中。

### **1、client 模块**

![](https://article-images.zsxq.com/FkSWrcgCLJyxON_U0KnpPUbZpiDV)

### **2、core 模块**

![](https://article-images.zsxq.com/FhDqFG-FtNCl0Du3g1hoWXQ0j3Q0)

### **3、metadata 模块**

![](https://article-images.zsxq.com/Fq-JKPceEImTuAHl1tYGUSnhJp1U)

### **4、raft 模块**

![](https://article-images.zsxq.com/Fl9E_u9xxaAxV3zSgWRQ3H12IBci)

至此，IDEA Kafka 源码环境已经安装完成。

##   
**05 源码环境验证**

下面我们来验证一下 Kafka 源码环境是否搭建成功。

## **5.1 copy 配置文件到 resources 目录**

首先，我们将 conf 目录下的 log4j.properties 配置文件拷贝到 src/main/scala 目录下，如下图所示：

![](https://article-images.zsxq.com/FupHvM6uwtGDys6KjKWykfQ4D13u)

接下来，我们修改 config 目录下的 server.properties 文件，将修改其中的 log.dir 配置项，将其指向 kafka 源码目录下的 kafka-logs 目录，server.properties 文件中的其他配置暂时不用修改：

![](https://article-images.zsxq.com/FvPk_o3WhqEgIHDRxu0VFjLdC0WU)

最后，我们在 IDEA 中配置 kafka.Kafka 这个入口类，启动 kafka broker，具体配置如下图所示：

此时执行报错：

![](https://article-images.zsxq.com/Fjm3tSdJPC9v81Un-0YXLYDUaAf8)

启动成功的话，控制台输出没有异常，且能看到如下输出：

## **06 Kafka 源码全景图**

上面聊完 Kafka 源码安装环境，接下来我们先来聊聊 Kafka 源码的一个全景图，看看Kafka 都包括哪些核心模块，如下图所示：

![](https://article-images.zsxq.com/FoUL4G2ub8yByMzsbV0usLcMIzq8)

从功能上讲，Kafka 源码可以分为五大模块：

> 1）服务端源码：实现 Kafka Broker 核心功能模块，包括日志存储、控制器、协调器、元数据管理及状态机管理、延迟机制、消费者组管理、高并发网络架构模型实现等。
> 
> 2）Java 客户端源码：实现了 Producer、Consumer 与 Broker 交互机制以及通用组件支撑代码。
> 
> 3）Connect 源码：用来构建异构数据双向流式同步服务。
> 
> 4）Stream 源码：用来实现实时流处理相关功能。
> 
> 5）Raft 源码：raft 一致性协议实现。

由此可见，「**服务端源码**」是理解 Kafka 底层存储架构和集群运行原理的核心，也是很多线上问题频发的「**重灾区**」。而客户端主要是跟服务端进行交互生产和消费数据，所以「**服务端源码**」和 「**客户端源码**」是 Kafka 实现最核心和精华的代码，也是我们深入研究的核心重点，因此**本源码系列主要剖析客户端以及服务端相关源码实现，其他模块的实现后续有机会在进行剖析**。

接下来我们分别来看看服务端源码和客户端源码的全景图以及我们分析的源码重点。

## **6.1 Kafka 生产端源码全景图**

从生产者端来说，我们可以学习到 Kafka Producer 是如何进行初始化; 集群元数据如何拉取、加载、更新; 又是如何通过 Java NIO 设计客户端网络通信模块的，以及如何通过缓存、异步、批量、内存池等设计保证消息在生产过程中的高性能和可靠性的。

对于不了解 Producer 的读者们，可以查看 **[聊聊 Kafka Producer 那点事](http://mp.weixin.qq.com/s?__biz=Mzg3MTcxMDgxNA==&mid=2247488849&idx=1&sn=febda095589f02553d9191528f271c07&chksm=cefb3c60f98cb576fd9c58d760b9a5e4ae32a0c001e2049b591297d904a0401646448999c78a&scene=21#wechat_redirect)**

我把整个生产端源码按照功能分为5大模块，每个模块会进一步的划分出一些子部分, 详细给出了各个组件级的源码分析, 你可以看下面这张全景图的重点介绍：

![](https://article-images.zsxq.com/Fv53Czi3kYoxLIiW4KaID6vZEC5p)

## **6.2 Kafka 服务端源码全景图**

从服务端来说，我们可以学习到 Kafka 服务端是如何管理和存储日志的，以及分区和副本是如何设计和实现的。

我把整个服务端源码按照功能分为5大模块，每个模块会进一步的划分出一些子部分, 详细给出了各个组件级的源码分析, 你可以看下面这张全景图的重点介绍：

![](https://article-images.zsxq.com/FsMOgu9HBKSPvhQ9-ls4qsNkNlIY)

## **6.3 Kafka 消费端源码全景图**

从消费者来说，我们可以学习到 Kafka consumer 初始化流程，组协调者机制，如何跟服务端进行通信，以及消费组如何实现消费的重平衡的。

对于不了解 Consumer 的读者们，可以先看看 **[聊聊 Kafka Consumer 那点事](https://articles.zsxq.com/id_xqh2l0kkhc7a.html)**

我把整个消费端源码按照功能分为6大模块，每个模块会进一步的划分出一些子部分, 详细给出了各个组件级的源码分析, 你可以看下面这张全景图的重点介绍：

![](https://article-images.zsxq.com/FjBfJDdsBrbbbYd9zjsUjuJLPi9N)

## **6.4 Kafka 源码之旅路线**

上面聊完 Kafka 源码全景图后，接下来我们聊聊如何高效的阅读源码。由于 Kafka 源码大概有50多万行，如果一头扎进去的话，就会出现深陷源码之中而无法自拔。

我们此系列是按照「**场景驱动**」的方式带你一步步深度剖析， 即「**从一条消息生产发送开始逐步探索 Kafka 的运行全流程**」。

因此并不是上来就先从「**服务端 Broker 源码**」开始入手进行探索和剖析，我们会从一条消息发送出去, 首先是「**生产端的源码运行的全流程**」，最核心的是四大块：

> 1）NIO 网络通信模块：带你深度剖析下 Kafka 是如何基于Java NIO 实现一套工业生产级的网络底层通信模块的, 非常经典。
> 
> 2）内存缓冲池设计：带你深度剖析下 Kafka 客户端是如何设计一套支撑百万并发的高吞吐量的缓冲机制的, 非常经典。
> 
> 3）Sender发送线程：这是我们剖析的重点的重点，必须要搞清楚 Kafka 客户端是如何通过网络通信把一批批消息发送到 Kafka Broker 端上去的，这里涉及到很多网络通信的细节，一些参数设置，应对网络故障的时候是如何进行处理的？非常经典。
> 
> 4）集群元数据拉取和更新机制：带你深度剖析下集群元数据拉取组件以及拉取时机；元数据如何在客户端缓存的；如何对Topic元数据支持细粒度的按需加载和同步等待。

此时消息已经被送达 Kafka Broker 端上去了, 这是我们学习的重点的重点。了解「**服务端的源码运行的全流程**」，最核心的是五大块：

> 1）集群架构：带你深度剖析下 Kafka Broker 的集群架构是如何实现的；各个 Broker启动起来后是如何组成一个集群的；集群的 Controller 是如何被选举出来的；故障恢复高可用架构方式是如何实现的等等。
> 
> 2）服务端网络通信模块：带你深度剖析下 Kafka 服务端网络通信模块是如何实现的、了解 Reactor 设计模型以及 Kafka 基于 Reactor 实现的支撑超高并发的网络架构、深度剖析 Acceptor 线程、Processor 线程、RequestChannel、IO线程池等网络底层通信组件实现以及请求处理全流程源码。
> 
> 3）分区与副本：带你深度剖析多副本冗余以及高可用架构如何实现；Leader 和 Follower 数据如何同步、副本如何传输、之间的HW和LEO如何变更；Leader 所在 Broker 故障宕机后， 如何保证系统高可用；副本管理器如何管理副本；Broker是如何异步更新元数据缓存的等等。
> 
> 4）负载均衡与伸缩架构：带你深度剖析如何保证数据均衡的分布在集群的 Broker 机器上的；如何进行 Topic 的 Partition 的扩缩容以及 Broker 的扩缩容。
> 
> 5）日志存储架构：带你深度剖析 Kafka 是如何高效存储的；磁盘读写是如何实现的；日志的存储结构是怎样的；如何利用 OS Cache、零拷贝、稀疏索引、顺序写等优秀设计来支撑超高吞吐量的存储架构的。

待数据存储到 Broker 端上后，就可以启动消费者去消费数据了， 这时我们需要了解「**消费端的源码运行的全流程**」，最核心的六大块：

> 1）消费流程：带你深度剖析消费端是如何初始化的；如何与服务端进行通信的；Consumer 的 poll() 方法是如何进行数据消费的。
> 
> 2）消费者组管理：带你深度剖析Consumer Group 概念、状态机流转；Consumer启动后是如何加入到 Group 的；Consumer Group 管理全流程；Consumer Group 元数据管理设计原理等等。
> 
> 3）Coordinator 机制：带你深度剖析Consumer Coordinator 工作原理；Consumer Coordinator 是如何选举出来 Consumer Leader 的；Consumer Leader 如何制定分区分配方案；Consumer Coordinator 又是如何下发分区分配方案的； 以及 Consumer 是如何定时发送心跳给 Consumer Coordinator 的等等。
> 
> 4）消息重平衡机制：带你深度剖析几种重平衡场景；以及重平衡源码流程分析。
> 
> 5）\_\_consumer\_offsets：带你深度探秘其内幕以及存储结构。
> 
> 6）订阅状态与Offset操作：带你深度剖析消费端订阅状态是如何保存和追踪Topic Partition 和 Offset 的对应关系；以及了解 Offset 如何获取以及提交方式等等。

至此一条消息从生产、存储、消费的整个流程就已经完毕了， 通过「**掌握一种科学的阅读源码的方式--用高效的方式，读最核心的源码**」让你真正掌握 Kafka 底层实现原理，在遇到线上故障时可以游刃有余，快速定位。

## **07 总结**

这里，我们一起来总结一下这篇文章的重点。

1、从零带你搭建 Kafka 源码环境：「**版本说明**」、「**环境准备**」、「**环境搭建**」。

2、带你梳理了「**Kafka 源码全景图**」所包含的核心模块分布，也分别梳理了「**服务端**」、「**生产端**」、「**消费端**」 三端核心源码功能模块。

3、梳理完源码全景图后，又带你剖析我们「**源码系列分享的整体思路**」，从「**场景驱动方式**」入手，带你一步步深度剖析数据流转的整个过程的核心源码实现，从而让你深入理解 Kafka 底层原理，掌握源码的高效阅读方法，快速定位线上问题并制定调优方案。