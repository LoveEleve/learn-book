自己动手，

从零开始编写Raft算法来实现分布式一致性算法！

![](images/9d4f8368ea6b342640acfcb9ab1861244ea4d066c05c5835bcb04d79f1a3736b.jpg)

# 一致性算法

开发实战

赵辰 $\odot$ 著

# 系统

选举、日志和多个高级主题逐步深入讲解。

# 详尽

通过3万行源码和测试，详细分析设计细节及实现难点。

# 生产级

基于Netty的生产级异步IO实现。

# 完整

包含交互式客户端的简易分布式KV服务。

# 目录

# Contents

# 第1章 分布式一致性与共识算法简介 /1

1.1 CAP定理 /2  
1.2 共识算法 /3

1.2.1 Paxos算法 /3  
1.2.2 Raft算法 /4  
1.2.3 ZAB算法 /5  
1.2.4 如何选择 /5

1.3 本章小结 /6

# 第2章 Raft核心算法分析 /7

2.1 不考虑分布式一致性的集群 /8  
2.2 日志状态机模型 /8  
2.3 基于Quorum机制的写入 /9  
2.4基于日志比较的选举/10

2.4.1 无重复投票 /10  
2.4.2 一节点一票制 /12

2.5 Raft算法中的选举 /13

2.5.1 逻辑时钟 term /13   
2.5.2 选举中的term和角色迁移 /14  
2.5.3 选举超时 /15  
2.5.4 整体流程 /16

2.6 Raft算法中的日志复制 /18

2.6.1 日志条目 /18  
2.6.2 复制进度 /18

2.6.2 整体流程 /19

2.7 Raft算法中的一些细节问题 /21  
2.7.1 Leader节点不能使用之前term的日志条目决定commitIndex /21  
2.7.2 角色变化表 /24

2.8 本章小结 /25

# 第3章 整体设计 127

3.1 设计目标 /28

3.1.1 正确性 /28  
3.1.2 状态机的通用性 /28  
3.1.3 足够的性能 /28

3.2 设计和实现顺序 /29  
3.3 参考实现 /29

3.4 状态数据分析 /30

3.4.1 需要持久化的状态数据 /30  
3.4.2 服务器可变状态数据 /30  
3.4.3 服务器可变状态数据 /30  
3.4.4 Leader服务器可变数据状态 /30   
3.4.5 服务器ID类型的选择 /31

3.5 静态数据分析 /32

3.5.1 配置 /32  
3.5.2 集群成员列表 /33

3.6 集群成员与映射表 /34

3.6.1 集群成员表 /34  
3.6.2 集群成员信息 /36

3.7 组件分析 /38

3.8 如何解耦组件间的双向调用关系 /39

3.8.1 解决方法1：合并一致性算法组件和通信组件 /39  
3.8.2 解决方案2：使用回调 /39  
3.8.3 解决方案3：延迟设置 /40  
3.8.4 解决方案4：使用Pub-Sub 方案 /41  
3.8.5 解决方案5：借助actor /42  
3.8.6 如何选择 /42

3.9 线程模型分析 /43

3.9.1 异步IO下的处理模型 /43  
3.9.2 处理组件的多线程分析 /45  
3.9.3 日志IO的异步化 /46  
3.9.4 整体线程设计 /48

3.10 项目准备 /48

3.10.1 构建环境 /48   
3.10.2 编辑器 /49  
3.10.3 Protocol Buffer /49   
3.10.4 多模块maven工程 /49

3.11 本章小结 /56

# 第4章 选举实现 /58

4.1 角色建模 /59

4.1.1 Follower角色 /60  
4.1.2 Candidate角色 /61  
4.1.3 Leader角色 /62  
4.1.4 关于状态机模式 /63

4.2 定时器组件 /63

4.2.1 Scheduler接口 /64  
4.2.2 选举超时 /65  
4.2.3 日志复制定时器 /66

4.3 消息建模 /67

4.3.1 RequestVote消息 /67   
4.3.2 AppendEntries消息 /69

4.4 关联组件和工具 /70

4.4.1 RPC接口 /71  
4.4.2 任务执行接口TaskExecutor /72  
4.4.3 部分角色状态持久化 /74

4.5一致性（核心）组件 /78

4.5.1 Node接口 /78  
4.5.2 NodeImpl的基本代码 /78  
4.5.3 系统启动与关闭 /79   
4.5.4 选举超时 /80  
4.5.5 收到 RequestVote 消息 /82  
4.5.6 收到 RequestVote 响应 /85  
4.5.7 成为Leader节点后的心跳消息 /87  
4.5.8 收到来自Leader节点的心跳消息 /88  
4.5.9 Leader节点收到来自其他节点的响应 /90

4.6 测试 /91

4.6.1 测试准备 /91  
4.6.2 系统启动 /97  
4.6.3 选举超时 /98  
4.6.4 收到RequestVote消息 /99   
4.6.5 收到RequestVote响应 /100  
4.6.6 成为Leader节点后的心跳消息 /100  
4.6.7 收到来自Leader节点的心跳消息 /101  
4.6.8 Leader节点收到其他节点的响应 /102

4.7 本章小结 /103

# 第5章 日志实现 /104

5.1 日志实现要求 /105

5.2 日志实现分析 /105

5.2.1 日志条目的内容 /105  
5.2.2 日志存储的选型 /108  
5.2.3 日志条目的序列化 /108  
5.2.4 日志接口 /108

5.3 日志条目序列 /110

5.3.1 抽象实现 AbstractEntrySequence /111   
5.3.2 基于内存的MemoryEntrySequence /115  
5.3.3 基于文件的FileEntrySequence /117

5.4 日志实现 /133

5.4.1 抽象实现 AbstractLog /133  
5.4.2 基于内存的MemoryLog /140  
5.4.3 基于文件的FileLog /141

5.5 与选举部分对接 /142

5.5.1 NodeContext和NodeBuilder /142   
5.5.2 选举超时 /142  
5.5.3 收到RequestVote消息 /142   
5.5.4 发送AppendEntries消息 /143  
5.5.5 收到AppendEntries消息 /143  
5.5.6 收到AppendEntries响应 /144

5.6 测试 /146

5.6.1 MemoryEntrySequence的基本操作 /146  
5.6.2 FileEntrySequence的基本操作 /148  
5.6.3 MemoryLog与FileLog /157

5.7 本章小结 /159

# 第6章 通信实现 /160

6.1 通信接口分析 /161

6.1.1 RequestVote消息和响应 /161

6.1.2 AppendEntries消息和响应 /161

6.2 序列化与反序列化 /162  
6.3 通信实现分析 /165

6.3.1 TCP还是UDP /165   
6.3.2 TCP的重复连接问题 /166  
6.3.3 TCP半包/粘包问题 /167  
6.3.4 同步IO还是异步IO /168

6.4 通信组件的实现 /168

6.4.1 NIO与Netty简介 /169  
6.4.2 组件的初始化和关闭 /171   
6.4.3 序列化和反序列化处理器 /173  
6.4.4 FromRemoteHandler和简易应用层协议 /177  
6.4.5 连接远程节点 /180  
6.4.6 发送消息 /181  
6.4.7 InboundChannelGroup /185

6.5 测试 /186

6.5.1 Encoder /186   
6.5.2 Decoder /187

6.6 本章小结 /188

# 第7章 基于Raft算法的KV服务 /189

7.1 服务设计 /190

7.1.1 整体设计 /190  
7.1.2 服务接口设计 /191  
7.1.3 命令执行流程 /195  
7.1.4 KV组件处理模型 /196  
7.1.5 命令的序列化和反序列化 /197  
7.1.6 客户端交互 /198

7.2 服务实现 /200

7.2.1 Node和状态机接口 /200  
7.2.2 Service和请求包装类 /201   
7.2.3 测试用GET命令 /204  
7.2.4 状态机实现 /204   
7.2.5 通信实现 /207  
7.2.6 组件依赖 /211

7.3 Node的组装与服务的启动 /213

7.3.1 命令行参数 /213  
7.3.2 命令行解析 /214   
7.3.3 以集群成员方式启动 /218  
7.3.4 3节点集群启动命令 /220

7.4 关于测试 /221  
7.5 本章小结 /221

# 第8章 客户端和整体测试 /222

8.1 客户端设计与实现 /223

8.1.1 基于命令行的交互式客户端 /223  
8.1.2 通信设计 /224  
8.1.3 集群成员列表 /224  
8.1.4 命令列表 /225  
8.1.5 命令上下文和服务路由器 /226  
8.1.6 增减服务器的命令 /230  
8.1.7 内部客户端 /230

8.2 客户端的启动和基本操作 /231

8.2.1 集群成员列表操作 /232  
8.2.2 Leader服务器节点 /233  
8.2.3 KV服务操作 /233

8.3 单机模式 /234

8.3.1 Raft算法下的单机模式 /234  
8.3.2 单机模式下的选举 /234  
8.3.3 单机模式下的日志复制 /235  
8.3.4 以单机模式启动 /235  
8.3.5 以单机模式测试 /237

8.4 集群模式 /238

8.4.1 测试前提和流程 /238  
8.4.2 单个节点无法选出Leader /239  
8.4.3 2个节点选出Leader /241   
8.4.4 3个节点正常选出Leader节点 /245  
8.4.5 3个节点中的非Leader节点掉线 /247

8.4.6 掉线的节点重新加入集群 /249  
8.4.7 Leader节点掉线并重新选出Leader /253

# 8.5 本章小结 /258

# 第9章 日志快照 /259

9.1 日志快照的分析和设计 /260

9.1.1 何时生成日志快照 /260  
9.1.2 谁生成日志快照 /260  
9.1.3 日志快照的存储 /260  
9.1.4 日志快照的格式 /261  
9.1.5 日志快照的传输 /262  
9.1.6 日志快照的安装与生成 /262  
9.1.7 接收方实现 /265   
9.1.8 KV服务的数据快照 /265  
9.1.9 日志快照和现有日志条目 /266

9.2 日志快照的实现 /267

9.2.1 日志快照接口 /267  
9.2.2 日志快照写入接口 /272  
9.2.3 日志快照的更新 /276  
9.2.4 日志快照的安装与应用 /277  
9.2.5 日志快照的生成 /279  
9.2.6 变速日志复制 /282  
9.2.7 日志快照安装消息的发送和响应的处理 /284  
9.2.8 日志组件中边界代码的处理 /287

9.3 测试 /289

9.3.1 日志快照的生成 /289  
9.3.2 日志快照的安装 /293

# 9.4 本章小结 /296

# 第10章 集群成员变更 /297

10.1 集群成员的安全变更 /298

10.1.1 直接变更 /298  
10.1.2 单服务器变更 /299

# 10.2 成员变更的一些细节问题 /304

10.2.1 新节点的日志同步 /304  
10.2.2 移除Leader节点自身 /305  
10.2.3 被移除节点的干扰 /306

10.3 成员组件修改 /307

10.3.1 成员变更规则 /307  
10.3.2 成员属性 /307  
10.3.3 成员列表 /310

10.4日志组件修改 /311

10.4.1 日志条目 /311  
10.4.2 日志快照 /313  
10.4.3 日志同步 /321

10.5 增加节点 /324

10.5.1 catch-up过程 /327  
10.5.2 追加日志 /334

10.6 移除节点 /340

10.6.1 核心组件中移除节点的方法 /340  
10.6.2 追加日志 /341  
10.6.3 选举部分的修改 /343

10.7 测试 /344

10.7.1 服务端命令实现 /344  
10.7.2 standby模式 /345

10.7.3 增加节点 /347  
10.7.4 移除节点 /353

10.8 本章小结 /357

# 第11章 Raft算法的优化 /358

11.1 PreVote /359

11.1.1 分析 /359  
11.1.2 实现 /360   
11.1.3 测试 /366

11.2 ReadIndex /368

11.2.1 Raft中的线性一致性 /369  
11.2.2 ReadIndex分析 /370  
11.2.3 实现 /372   
11.2.4 测试 /380

11.3 其他优化 /383

11.3.1 基于时钟的只读请求 /384  
11.3.2 批量日志传输和 pipelining /384   
11.3.3 Leadership Transfer /385   
11.3.4 multi-raft /386

11.4 本章小结 /386

# 第1章

# 分布式一致性与共识算法简介

在介绍Raft算法之前，请考虑一下如果有机会，你会怎么设计一个分布式系统？注意，这里所说的分布式系统是几台服务器组成的一个对外服务的系统，比如分布式KV系统、分布式数据库系统等。

如果是单机系统，数据一般都在本地，基本不需要与外部通信，比如单机数据库系统。但如果有一天你的系统遇到了单机系统难以承受的高请求量，为了防止系统宕机，也为了提高系统的可用性，可以搭建类似master-slave结构的系统，并且允许请求落到slave服务器上。

经过一段时间的设计和编码，你可能会发现这个系统没有想象中那么简单。首先，相比单机系统，分布式系统需要和多台服务器通信，而通信有超时的可能，此时发送方无法确定通信是成功还是失败。其次，一份数据被放到了多台服务器，数据更新有延迟。最后，一旦master服务器宕机，没有一个自动的机制可以立马提升slave服务器为master服务器。

这时你可能会想，是否有方法可以解决上述问题？或者说是否有框架可以解决分布式系统所面临的问题？答案是没有，依据是接下来要讲到的分布式系统领域的CAP定理。

# 1.1 CAP定理

CAP分别是如下3个单词的首字母缩写

（1）Consistency：一致性。  
（2）Availability：可用性。  
（3）Partition-tolerance：分区容错性。

CAP定理指出，在异步网络模型中，不存在一个系统可以同时满足上述3个属性。换句话说，分布式系统必须舍弃其中的一个属性。

图1-1展示了分布式系统中不存在同时覆盖3个属性的区域，但是可以找到同时覆盖两个属性的区域。

![](images/2d3b1fabb45b49cc20e7b25bf79b7be9e484b9a226bb9a95eccaff1434276660.jpg)  
图1-1 CAP理论

在本章开头提到的系统实现问题中，通信超时和更新延迟都属于一致性问题，出现这个问题的原因是存在多台服务器，而每台服务器都有自己的数据。数据冗余虽然可以提高系统的可用性和分区容错性，但是相应地难以满足强一致性。如果想要解决一致性问题，也就是达到强一致性，比如把所有请求全部通过单台服务器处理，那么就很难达到高可用性。

CAP定理对于分布式系统的设计是一个很重要的参考。对于需要在分布式条件下运行的系统来说，如何在一致性、可用性和分区容错性中取舍，或者说要弱化哪一个属性，是首先需要考虑的问题。从经验上来说，可用性或一致性往往是被弱化的对象。

对于要求高可用性的系统来说，往往会保留强一致性。典型的例子就是延迟处理，利用Message Queue之类的中间件，在后台逐个处理队列中的请求，当处理完毕时，系统达到强一致性状态。但是要求强一致性的系统，比如元数据系统、分布式数据库系统，它们的可用性往往是有上限的。

从实现效果上来说，很多人或多或少都了解或者设计过具有强一致性的系统。但是，大部分人并不了解强一致性的系统是如何运作的，也不知道该怎么设计。老实说这确实很难，以至于计算机科学界有一类专门解决这种问题的算法——共识算法。

# 1.2 共识算法

“共识”的意思是保证所有的参与者都有相同的认知（可以理解为强一致性）。共识算法本身可以依据是否有恶意节点分为两类，大部分时候共识算法指的都是没有恶意节点的那一类，即系统中的节点不会向其他节点发送恶意请求，比如欺骗请求。共识算法中最有名的应该是Paxos算法。

# 1.2.1 Paxos算法

Paxos 算法是 Leslie Lamport 在 1998 年发表的 The Part-Time Parliament 中提出的一种共识算法。

Paxos算法除了难懂之外，还难以实现。尽管如此，以下服务还是在生产环境中使用了Paxos算法和Paxos算法的修改版。

（1）GoogleChubby：分布式锁服务。  
（2）GoogleSpanner：NewSQL。  
（3）Ceph。  
（4）Neo4j。  
（5）Amazon Elastic Container Service。

本书不会详细介绍Paxos算法，只是列举一下大体的流程。Paxos算法中的节点有以下3种可能的角色。

（1）Proposer：提出提案，并向Acceptor发送提案。  
（2）Acceptor：参与决策，回应提案。如果提案获得多数（过半）Acceptor接受，则认为提案被批准。  
（3）Learner：不参与决策，只学习最新达成一致的提案。

Paxos算法中的决议过程分两种，一种是对单个value（值）的决议过程，也就是对单个值达成一致；另一种是针对连续多个value的决议过程。前者称为Basic Paxos，后者称为Multi Paxos。

Basic Paxos 的过程如下。

（1）Proposer生成全局唯一且递增的proposal id，并向所有Acceptor发送prepare请求。  
（2）Acceptor收到请求后，如果proposal id正常，则回复已接受的proposal id中最大的决议id和value。  
（3）Proposer接收到多数Acceptor的响应后，从应答中选择proposal id最大的value，并发送给所有Acceptor。  
（4）Acceptor收到proposal之后，接收并持久化当前proposal id和value。  
（5）Proposer收到多数Acceptor的响应之后形成决议，Proposer发送决议给所有Learner。

可以看到，对于多个value，BasicPaxos两次来回的决议使其性能不是很理想，所以有针对连续多个value决议的MultiPaxos。

MultiPaxos的基本思想是先使用BasicPaxos决议出Leader，再由Leader推进决议。MultiPaxos和BasicPaxos具体有以下不同。

（1）每个Proposer使用唯一的id标识。  
（2）使用BasicPaxos在所有Proposer中选出一个Leader，由Leader提交proposal给Acceptor表决，这样BasicPaxos中的（1）～（3）步可以跳过，提高效率。

以上只是对Paxos算法最基本的理解，但在实际实现中还有很多具体问题需要解决。因为部分问题难以解决或者没有可以参考的解决方案，导致以下Paxos的变种算法的出现（按出现的时间先后排列）。

（1）Disk Paxos，2002年。  
（2）CheapPaxos，2003年。  
（3）Fast Paxos，2004年。  
（4）Generalized Paxos，2005年。  
（5）StoppablePaxos，2008年。  
（6）Vertical Paxos，2009年。

从系统实现的角度来说，从变种算法中选择一个并实现可能是比较好的选择，但是无法避免地要对Paxos有一定了解才能开始编码。到了2014年，出现了一种更容易理解的共识算法——Raft算法。

# 1.2.2 Raft算法

Raft算法由斯坦福大学的DiegoOngaro和JohnOusterhout在2014年提出。在保证和Paxos算法一样的正确性的前提下，具体分析了选举及日志复制的实现，甚至还提供了参考代码。

关于Raft算法的论文中给出了计算机科学系学生对于Raft算法的可理解性的调查结果。结论是40多人中的大部分人认为Raft算法更容易理解，相对的只有个位数的人认为Paxos算法更容易理解。论文作者之一的DiegoOngaro之后在他的博士论文中进一步给出了Raft算法的详细分析，包括优化后的集群成员变更算法（第10章会对集群成员变更算法做具体的介绍与分析）。

Raft算法的官方网站raft.github.io中给出了可视化的Raft算法中Leader的选举过程（另一个网站thesecretlivesofdata.com给出了更详细的Leader选举和日志复制的可视化过程），同时给出了一些Raft算法相关的论文、演讲和教程链接。对于想要快速了解Raft算法的人来说，这是一个很好的入口。

官方网站主页上还给出了一些Raft算法实现的列表，其中很多是试验性的实现。这些实现的源代码大部分可以在GitHub上找到。如果想要特定语言的实现版本，可以尝试在上面查找。官方网站主页上Raft算法实现的列表可以通过GitHub Pages的Pull Request通知管理员修改。如果

对自己的算法实现有信心，可以克隆主页的GitHub库，然后添加自己的算法实现后提交PullRequest。

生产环境级别的Raft算法实现比较有名的是基于Go语言的etcd，基于etcd设计出来的上层软件可以用于生产环境。

下面大致介绍一下Raft算法。

Raft算法是单Leader、多Follower模型，可以理解为主从模型。Raft算法中有以下3种角色。

（1）Leader：集群的Leader。  
（2）Candidate：想要成为Leader的候选者。  
（3）Follower：Leader的跟随者。

系统在启动后会马上选举出Leader，之后的请求全部通过Leader处理。Leader在处理请求时，会先加一条日志，把日志同步给Follower。当写入成功的节点过半之后持久化日志，通知Follower也持久化，最后将结果回复给客户端。

更详尽的细节将在第2章进行介绍。

# 1.2.3 ZAB算法

现存的并且可以作为集群一部分的分布式同步软件中，Apache ZooKeeper（简称 ZooKeeper）可能是最有名的一个。ZooKeeper 原本是 Apache Hadoop 的一部分，现在是顶级 Apache Project 中的一个。ZooKeeper 被很多大公司使用，是一个经过生产环境考验的中间件。

从功能上来说，ZooKeeper 是一个分布式等级型 KV 服务（Hierarchical Key-Value Store）。和一般用于缓存的 KV 服务不同，客户端可以监听某个节点下的 Key 的变更，因此 ZooKeeper 经常被用于分布式配置服务。

ZooKeeper的核心是一个名叫ZAB的算法，这是Paxos算法的一个变种。ZAB算法的详细内容这里不做展开，一方面ZAB算法和Paxos算法有相同的地方，另一方面ZooKeeper在面向客户端方面所做的设计可能比ZAB算法更加复杂，因此就算理解了ZAB算法也不一定能完全理解ZooKeeper的设计。

# 1.2.4 如何选择

总体来说，如果是第一次接触分布式一致性算法，那么Raft算法是一个很好的入门算法。但如果想深入研究，Paxos算法仍旧是无法回避的。其实算法本身并无优劣之分，理解其背后的思想才是最重要的。

如果想使用现成的分布式一致性中间件，那么Apache ZooKeeper可能是一个不错的选择。如果想进一步了解分布式一致性的算法细节实现，那么ZooKeeper的源代码很值得阅读和参考。

# 1.3 本章小结

本章主要介绍了在实现分布式系统时可能碰到的CAP问题，也就是一致性、可用性和分区容错性不能同时满足的限制，以及在实现强一致性系统时所用到的共识算法。

和Paxos算法相比，Raft算法在保持正确性的同时更容易理解。

接下来，本书将详细分析Rafi的核心算法。

# 第2章

# Raft核心算法分析

- Raft算法的核心是选举和日志复制。本章参考Raft算法的可视化交互演示，循序渐进地讲解Raft算法中各个节点的交互以及Raft算法如何保证强一致性。

这里以一个分布式Key-Value服务（类似于Memcache和Redis，以下简称KV服务）作为分析的对象。如图2-1所示，面对单台服务器时，只需要直接读写KV服务的数据即可。

面对多台服务器、多个数据副本时，系统架构如图2-2所示。

![](images/bb7ccf7628f9091368732610f1d75994aa250f8d464b7c2b5d647e60544fc9ee.jpg)  
图2-1 单服务器模型

![](images/cfbd01ed307c7fc37bba90522468dffd11d902eb77163f9c047a8fde9195fac6.jpg)  
图2-2 多服务器模型

# 2.1 不考虑分布式致性的集群

假如多服务器模型中的每台服务器都对外服务，那么服务器如何同步变更就成了一个问题（有兴趣的读者可以了解一下Gossip协议）。

一般的解决方案是使用主从模型，即一个Leader（主）服务器，多个Follower（备份）服务器。所有请求都通过Leader服务器处理，Follower服务器只负责备份数据。这样的好处是，变更源只有一个，简化了设计。

但是问题仍旧存在，假设Leader服务器宕机了，那么Follower服务器中哪个服务器成为新的Leader服务器呢？理论上所有Follower服务器的副本数据应该是和Leader服务器一致的，但是由于数据延迟、发送顺序不一致等问题，导致某个时刻每个Follower服务器拥有的数据有可能不一样。

数据不一致的问题需要从以下两方面进行处理。

（1）使用日志写入，而不是直接修改，保证到Follower服务器的同步请求有序而且能够重新计算出当前状态，也就是日志状态机模型。  
（2）写入时，过半服务器写入成功才算整体成功，也就是Quorum机制。

# 2.2 日志状态机模型

举个KV服务下的状态机模型的例子，KV服务中每个SET操作都作为一个日志条目，如表2-1所示。

表 2-1 状态机模型  

<table><tr><td>日志索引</td><td>操作</td><td>当前状态</td></tr><tr><td>1</td><td>X:=1</td><td>{X:1}</td></tr><tr><td>2</td><td>Y:=2</td><td>{X:1,Y:2}</td></tr><tr><td>3</td><td>X:=3</td><td>{X:3,Y:2}</td></tr><tr><td>4</td><td>Z:=4</td><td>{X:3,Y:2,Z:4}</td></tr></table>

表2-1的最左边是日志的索引，中间是操作部分，“关键字：数字”代表设置关键字的值为指定的数字，最右边的当前状态显示了各个变量现在的值。

在状态机模型中，日志从上往下不断追加，当前状态的任何时间点都可以从索引号为1的日志开始计算。比如，日志索引为3的时间点，系统状态可以从日志 $1\sim 3$ 的操作中计算出来；日志索引为4的时间点，系统状态可以从日志 $1\sim 4$ 的操作中计算出来，以此类推。

有了状态机模型之后，如何保证分布式一致性的问题就转换成了，如何保证所有参与的节点按照同一顺序写入的问题。虽然上面的状态机模型是以KV服务为例的，但实际上状态机模型也适用于KV以外的服务。

# 2.3 基于Quorum机制的写入

基于Quorum机制的写入是一个折中方案。在一般的master/slave模式的数据库主从复制中，master服务器并不关心slave服务器的复制进度。master服务器只负责不断写入自己的日志，通过binlog等方式把变更同步给slave服务器。而在一些严格的全量复制中，当所有slave服务器全部同步之后，master服务器才会继续写入。主从复制在master服务器宕机之后数据会丢失，而全量复制则性能非常差。相比之下，过半写入的Quorum机制既可以减少数据丢失的风险，性能也不会太差。

Quorum机制主要关注多副本下如何保证读取到最新副本的问题。比如在图2-3中，假设存在3个服务器节点A、B和C，客户端可以读取任意服务器节点的数据和对应的版本。

![](images/61c588381db1cf67523d1f7b92759c7f8ebc72d445feb1688165061a52526ddc.jpg)  
图2-3Quorum机制的写入

在左边的“写入中”时刻，服务器节点A、B和C的数据版本号如下。

（1）节点A：2。  
（2）节点B：1。  
（3）节点C：1。

这里版本比较大的数字表示数据比较新。客户端选择读取A、B和C3个服务器中任意2个节点的数据，此时客户端读取到的数据版本有以下可能。

（1）节点A与B:2与1。  
（2）节点A与C:2与1。  
（3）节点B与C:1与1。

可以看到在第（3）种情况下，客户端读取到节点B和节点C的数据版本都是1，没有读到最新版本2的数据。相对地，在右边的“过半写入完成”时刻，服务器节点A、B与C的数据版本如下。

（1）节点A:2。  
（2）节点B:2。

（3）节点C:1。

此时客户端读取到的数据版本有以下可能。

（1）节点A与B：2与2。  
（2）节点A与C：2与1。  
（3）节点B与C：2与1。

不管是哪种情况，客户端都能读取到最新版本的数据。所以说当数据过半写入之后，客户端总能读取到最新数据。

对于master/slave或者leader/follower模型的分布式系统来说，客户端并不能直接访问所有节点。但是对于系统内的服务器节点来说，可以通过比较各自持有的日志来决定谁成为新的Leader节点。在此过程中，过半写入的数据往往是有效的数据。

# 2.4 基于日志比较的选举

假设 Leader 节点宕机，那么如何从剩下的服务器节点中选举新 Leader 节点呢？一般情况下，肯定是希望选择拥有最新数据的节点。

理论上，这个拥有最新数据的节点应该有过半节点的支持，也就是说，集群中超过半数的节点（包括这个拥有最新数据的节点自身）的数据不会比这个节点更新。如果不满足这个条件，集群有可能出现“脑裂”现象，比如几个节点拥护一个Leader节点，而另外几个节点拥护另一个Leader节点。

对于如何判断谁的数据更新，可以通过比较来自其他节点的投票请求中的日志索引和自己本地的日志索引来确定。如果自己本地的日志索引比较大，则不支持对方，否则就支持。

# 2.4.1 无重复投票

考虑一个3服务器节点的系统，每个节点的最新日志索引号如下。

（1）节点A：2。  
（2）节点B:2。  
（3）节点C：2。

按照日志比较的规则，节点A会支持B和C，节点B会支持A和C，节点C会支持A和B。此时节点A、B和C各自拿到2票都可以成为Leader节点，再加上自己给自己的1票，每个节点都是3票。

为了减少同时成为Leader节点的概率，要求节点不能重复投票。也就是说，节点收到投票请求并且不反对，则记录自己投过的节点，之后不再投给其他节点。

严格来说，仅仅使用无重复投票的机制是无法避免多个Leader节点的选出的，此处可以了解

一下基本的投票机制。

对于同一个3服务器节点的系统，假如每个节点的最新日志索引号如下。

（1）A：3。  
（2）B:2。  
（3）C:2。

则可能的Leader节点选出结果有4种情况，如表2-2所示。

表 2-2 基于日志比较的选举 (1)  

<table><tr><td>编号</td><td>A投票给</td><td>A票数</td><td>B投票给</td><td>B票数</td><td>C投票给</td><td>C票数</td><td>Leader选出</td></tr><tr><td>1</td><td></td><td>3</td><td>A</td><td>1</td><td>A</td><td>1</td><td>A</td></tr><tr><td>2</td><td></td><td>2</td><td>A</td><td>2</td><td>B</td><td>1</td><td>A,B</td></tr><tr><td>3</td><td></td><td>2</td><td>C</td><td>1</td><td>A</td><td>2</td><td>A,C</td></tr><tr><td>4</td><td></td><td>1</td><td>C</td><td>2</td><td>B</td><td>2</td><td>B,C</td></tr></table>

表2-2最左边是可能的结果的编号，中间部分是节点A、B和C分别投票给谁以及投票过后各自所拥有的票数，最右边是票数超过2的节点，也就是Leader节点的候选节点。

由于节点A的数据比节点B、C都新（索引3大于索引2），所以A不会给其他节点投票，列“A投票给”为空。

注意编号4的情况，节点B或C也可以成为Leader节点，即使节点B和C都没有最新的数据。

对于同一个3服务器节点的系统，假如每个节点的最新日志索引如下。

（1）节点A：3。  
（2）节点B:3。  
（3）节点C:2。

如表2-3所示，Leader节点选出的结果只可能是节点A或者B，节点C永远都不会成为Leader节点。

表 2-3 基于日志比较的选举 (2)  

<table><tr><td>编号</td><td>A投票给</td><td>A票数</td><td>B投票给</td><td>B票数</td><td>C投票给</td><td>C票数</td><td>Leader选出</td></tr><tr><td>1</td><td>B</td><td>3</td><td>A</td><td>2</td><td>A</td><td>1</td><td>A,B</td></tr><tr><td>2</td><td>B</td><td>2</td><td>A</td><td>3</td><td>B</td><td>1</td><td>A,B</td></tr></table>

可以看到，拥有最新数据的节点有可能成为不了Leader节点，比如表2-2中编号4的情况。这是由于3个节点中只有1个节点拥有最新数据，拥有最新数据的节点数没有过半。而在实际中，新数据在其他节点成为Leader之后会被丢弃，所以理论上没有过半写入的数据不能认为是稳定的数据。

# 2.4.2 一节点一票制

实际选举中申请成为Leader的节点不能给其他节点投票，已经投过票的节点也不能再给其他节点投票。也就是说，一个节点最多只能投一票。在此基础上，重新看一下Leader节点的选举情况。

对于一个3服务器节点的系统，假如每个节点的最新日志索引号如下。

（1）节点A：2。  
（2）节点B：2。  
（3）节点C：2。

则Leader节点选出的结果有10种，如表2-4所示。

表2-4 一票制  

<table><tr><td>编号</td><td>A</td><td>A票数</td><td>B</td><td>B票数</td><td>C</td><td>C票数</td><td>Leader选出</td></tr><tr><td>1</td><td>自荐</td><td>3</td><td>投票给A</td><td>0</td><td>投票给A</td><td>0</td><td>A</td></tr><tr><td>2</td><td>投票给B</td><td>0</td><td>自荐</td><td>3</td><td>投票给B</td><td>0</td><td>B</td></tr><tr><td>3</td><td>投票给C</td><td>0</td><td>投票给C</td><td>0</td><td>自荐</td><td>3</td><td>C</td></tr><tr><td>4</td><td>自荐</td><td>2</td><td>自荐</td><td>1</td><td>投票给A</td><td>0</td><td>A</td></tr><tr><td>5</td><td>自荐</td><td>1</td><td>自荐</td><td>2</td><td>投票给B</td><td>0</td><td>B</td></tr><tr><td>6</td><td>投票给B</td><td>0</td><td>自荐</td><td>2</td><td>自荐</td><td>1</td><td>B</td></tr><tr><td>7</td><td>投票给C</td><td>0</td><td>自荐</td><td>1</td><td>自荐</td><td>2</td><td>C</td></tr><tr><td>8</td><td>自荐</td><td>2</td><td>投票给A</td><td>0</td><td>自荐</td><td>1</td><td>A</td></tr><tr><td>9</td><td>自荐</td><td>1</td><td>投票给C</td><td>0</td><td>自荐</td><td>2</td><td>C</td></tr><tr><td>10</td><td>自荐</td><td>1</td><td>自荐</td><td>1</td><td>自荐</td><td>1</td><td></td></tr></table>

表2-4的中间部分表示节点是自荐（给自己投票）还是给其他节点投票。节点只能在自荐（投给自己）或者给其他节点投票中选择。

除了全部自荐的情况，Leader节点选出的结果中只有1个节点会被选出。因为系统整体的票数只能跟节点个数相同，加上选举要求支持的节点过半，所以至多只能有1个节点被选出。证明如下。

对于 $N(N > 0)$ 个节点的集群，假设有 $M$ 个节点成为Leader节点，那么这些节点都需要过半节点的支持票，则总票数为

式1： $M*N_{\text{过半}}$

根据节点数为奇数还是偶数， $N_{\text{过半}}$ 有两种情况： $(N + 1) / 2$ 和 $N / 2 + 1$

又因总票数为节点数 $N$ ，所以式1需要满足

式2： $M*N_{\text{过半}} \leqslant N$

此时 $M$ （Leader节点数）为0肯定满足式2， $M$ 为1也满足式2。当 $M$ 为2时，分别考虑 $N$

为奇数和偶数的情况。

奇数情况： $2*(N + 1) / 2 = N + 1$ ，结果大于 $N$ ，式2不成立。

偶数情况： $2*(N / 2 + 1) = N + 2$ ，结果同样大于 $N$ ，式2不成立。

以此类推，当 $M > 2$ 时，式2不成立。

因此，在总票数为节点数的情况下最多只能选出1个Leader节点。

可以看到，基于日志的比较加上一票制基本可以满足Leader节点选举的要求，这其实也是Raft算法里所采用的方法，接下来会具体讲解Raft算法中的选举是如何设计的。

# 2.5 Raft算法中的选举

第1章提到过，在Raft算法中，节点有3种角色：Leader、Candidate（Leader候选人）、Follower。每个节点在任何时候都只可能是这3种角色中的一种。

如图2-4所示，在稳定状态下，存在一个Leader节点和多个Follower节点，Leader节点通过心跳消息与各Follower节点保持联系。

![](images/07f4230789227c63a753a567d4dd01a3f6b0bdaf9e076864704258a0df8e3056.jpg)  
图2-4 稳定状态下的主从模型

包括心跳信息在内，Raft算法中节点之间所使用的消息主要有以下两种。

（1）RequestVote，即请求其他节点给自己投票，一般由Candidate节点发出。  
（2）AppendEntries，字面意思是增加条目，也就是用于日志复制，但在增加日志条目数量为0时也作为心跳信息，一般只由Leader节点发出。

下一章会对消息内容做详细介绍。

# 2.5.1 逻辑时钟term

Raft算法中的选举有一个term参数，其类型为整数。这是一个逻辑时钟值，全局递增，准确

来说是 Lamport Timestamp 的一个变体。

Lamport Timestamp 的算法很简单。假设有多个进程要维护一个全局时间，首先要让每个进程本地有一个全局时间的副本。算法流程如下。

（1）每个进程在事件发生时递增自己本地的时间副本（加1）。  
（2）当进程发送消息时，带上自己本地的时间副本。  
（3）当进程收到消息时，比较消息中的时间值和自己本地的时间副本，选择比较大的时间值加1，并更新自己的时间副本。

整个过程对应到Rafi算法中如下。

（1）在开始选举时，增加 term。  
（2）发送RequestVote消息时，带上自己的term（递增过后的term）。  
（3）节点在收到RequestVote消息或AppendEntries消息时，比较自己本地的term和消息中的term，选择最大的term并更新自己本地的term（注意，此处没有加1操作）。

这样即使服务器间时钟不一致，系统也可以安全地推进逻辑时间。

# 2.5.2 选举中的term和角色迁移

Raft算法中主要使用term作为Leader节点的任期号，选举中需要term参数以及需要使其递增的原因可以理解如下。

（1）因为Follower节点不能重复投票（一节点一票制），所以如果没有新的选举term，节点在第一次选举后不会再给其他节点投票。  
（2）递增之后可以避免给比较早的 term 投票，比如因为慢速网络或者网络分区迟到的投票请求。

在选举的过程中，节点的角色会有所变化，Raft算法中的角色迁移如图2-5所示。

![](images/df5e303c6dddade0d906d33b01c35eab243c90b173e4a20266cbde8105e4fb04.jpg)  
图2-5 角色迁移图

图2-5从左往右依次代表以下步骤

（1）系统启动时所有节点都是Follower节点。  
（2）当没有收到来自Leader节点的心跳消息，即心跳超时时，Follower节点变成Candidate节点，即自荐成为选举的候选人。  
（3）Candidate节点收到过半的支持（包括自己）后，变成Leader节点。

正常情况的选举到此结束。出现Leader节点之后，Leader节点会发送心跳消息给其他节点，防止其他节点从Follower节点变成Candidate节点。

在步骤（3）中，如果Candidate节点没有得到过半支持，比如在图2-6所示的4个节点的集群中，两个Candidate节点分别得到2票（箭头表示请求投票和回复），票数没有过半，无法选出Leader节点，此时Candidate节点选举超时，进入下一轮选举。

![](images/baf770e14e1511900c59e2aa2e6075c7d497b812a312bc18c1e20a03511ad166.jpg)  
图2-6 split vote（分割选举）

票数对半的现象在 Raft 算法中被称为 split vote（分割选举），在偶数个节点的集群中有可能发生，Raft 算法使用随机选举超时来降低 split vote 出现的概率。

图2-5中的Candidate节点和Leader节点在收到term比自己本地的term大的消息后，会退化为Follower节点，这往往发生在网络分区的情况下。

# 2.5.3 选举超时

2.5.2小节中提到，4个节点的集群中同时出现2个Candidate节点，这2个节点各自获取2票时，只能等待下一次选举。但是可能下一次选举还是2票对2票，最坏的情况是无限次2票对2票，那就无法正常选出Leader节点了。为了减少这类问题出现的概率，Raft算法中的选举时间是一个区间内的随机值，比如3~4秒，则每个节点可能的选举超时有3秒、3.1秒、3.5秒等。这样成为Candidate节点的时间点就被错开了，提高了选出Leader节点的概率。

在选出Leader节点之后，各个节点需要在最短的时间内获取新Leader节点的信息，否则选举超时又会进入一轮选举，即要求心跳消息间隔 $<<$ 最小选举间隔。“<<”在这里表示远小于。

节点的选举超时时间在收到心跳消息后会重置。如果不重置，节点会频繁发起选举，系统难以收敛于稳定状态。

举个重置的例子，假设选举超时间隔3~4秒，心跳间隔1秒，则节点会以类似于下面的方式不断修改实际选举超时时间。

（1）节点以Follower角色启动，随机选择选举超时时间为3.3秒，即3.3秒后系统会发起选举。  
（2）节点启动1秒后，收到来自Leader节点的心跳消息，节点重新随机选择一个选举超时时间，并修改下一次选举时间为现在时间的3.4秒后。  
（3）节点启动2秒后，再次收到来自Leader节点的心跳消息，节点再次随机选择一个选举超时时间，并修改下一次选举时间为现在时间的4秒后。

只要Leader持续不断地发送心跳消息，Follower节点就不会成为Candidate角色并发起选举。

# 2.5.4 整体流程

以一个3服务器节点的系统刚启动时的Leader选举为例。

集群中的节点刚启动时，所有节点都是 Follower 节点，没有 Leader 节点。因为没有节点收到来自 Leader 节点的心跳消息，Follower 节点会在选举超时后变成 Candidate 节点。上面也提到，每个节点的选举超时时间不一样，所以这里只考虑只有节点 A 变成 Candidate 节点的情况。

如图2-7所示，节点A变成Candidate节点后，会向其他节点发送RequestVote消息。

![](images/18fe6363c94a31adaa8e0396abbcafc96045c9b86fa8bd6f55e6782404e3dcf1.jpg)  
图2-7 Leader选举开始

如图2-8所示，其他节点（B和C）在收到RequestVote消息后，会对比RequestVote消息中的最后一条日志的元信息（lastLogTerm，lastLogIndex）和自己本地的最后一条日志的元信息，选择给Candidate节点投票还是不投票。

![](images/b82c591b6054f2c3a09a6ba3ee23686b8a40c507c4368ebaab11910cdabb6827.jpg)  
图2-8 投票

Candidate 节点 A 在收到其他节点的投票但是支持数没有过半时，会重置自己的选举超时时间并继续等待其他节点的响应。Candidate 节点在收到投票并满足过半支持后变成 Leader 节点，比如 3 节点集群中拥有 2 个支持。考虑包含 Candidate 节点自己的一票，上述集群中只要收到节点 B 或者 C 中的 1 票后，节点 A 即可当选 Leader 节点。

如图2-9所示，当选后，Candidate节点A升格为Leader节点，并立刻向其他节点发送心跳消息（AppendEntries）。其他节点收到心跳消息之后，重置自己的选举超时时间，保持Follower角色。

![](images/74e98239eba7c3a0fe75a3577ae067bb76cfa2b61e555b1f1f338895557902c4.jpg)  
图2-9 Leader选出

至此，选举结束，Leader节点和Follower节点之间开始日志同步（关于日志同步，具体请看2.6节）。

除了集群启动之外，由于Leader节点宕机、网络分区等导致Follower节点收不到来自Leader节点的心跳消息时，集群也会开始选举。此时的流程和启动时的流程基本一致，不同的是有可能收不到部分节点的响应。理论上Candidate节点只需要过半支持，所以最多可以容许一半以下的节点宕机或者不可用。

# 2.6 Raft算法中的日志复制

# 2.6.1 日志条目

之前介绍的日志状态机模型中，每个操作都会被当作一个日志条目。事实上在Rafi算法中也是这样的，所有来自客户端的数据变更请求都会被当作一个日志条目追加到节点日志中。

Raft算法中的日志条目除了操作还有term，也就是之前提到的选举中用的Leader节点的任期号。日志中的term也会被用于日志比较，新term的日志总比旧term的日志新。

日志条目分为以下两种状态。

（1）已追加但是尚未持久化。  
（2）已持久化。

Raft算法中的节点会维护一个已持久化的日志条目索引，即commitIndex。小于等于commitIndex的日志条目被认为是已提交，或者说是有效的日志条目（在本书中亦称为已持久化），否则就是尚未持久化的数据。

需要注意的是，在Raft算法中，系统启动时commitIndex为0，所以不管日志是否在文件中，系统启动时日志条目都被认为是“未提交”的状态。

# 2.6.2 复制进度

为了跟踪各节点的复制进度，Leader负责记录各个节点的 nextIndex（下一个需要复制日志条目的索引）和 matchIndex（已匹配日志的索引）。

选出Leader节点后，Leader节点会重置（或者说新建）各节点的nextIndex和matchIndex，也就是把matchIndex设置为0，nextIndex设置为Leader节点接下来的日志条目的索引，然后通过和各节点之间发送AppendEntries消息来更新nextIndex和matchIndex。

图2-10展示了复制过程中各个索引之间的关系。注意，matchIndex并不总是和nextIndex邻接，比如在复制刚开始的时候。

![](images/bc0e5bc2ebc68e65631e28aae187627fcebd0a729c5fd8bd09e2ce75a301388e.jpg)  
图2-10 复制进度

复制过程中，Raft算法采用的是乐观策略，认为Follower节点和Leader节点日志不会相差太大。把nextIndex设置成最大值，然后不断回退nextIndex，以找到匹配的日志索引。这时matchIndex会瞬间从0跳到一个比较大的值，然后类似于进度条一样批量同步日志并更新进度。

当系统处理达到稳定状态时，Leader跟踪的各个节点的matchIndex与Leader的commitIndex一致，nextIndex与Leader节点的下一条日志的索引一致。此时，上面的复制进度条就会像 $100\%$ 复制完成一样。

Raft算法针对回退时一条一条回退可能导致处理比较慢的问题，提出了一次回退一个term的方案。不过实际出现这种情况的可能性比较小，所以没有必要使用。

# 2.6.2 整体流程

以下是系统层面日志复制的过程，以一个3服务器节点的系统为例，图2-11、图2-12和图2-13分别展示了每个步骤的内容。

![](images/cc86e103a421d2447e15c47cf61913f7c2d2d296981bfa966db734667a1e8e66.jpg)  
图2-11 日志追加

![](images/37b3f6e622c943204eacc55c7cfcce622115bb6f7c28355e9a1415f5d316efdf.jpg)  
图2-12 日志持久化

![](images/1eb7c60fa8823066c62ab4eee61bd775d522e9018cddff66993b7015e57154bd.jpg)  
图2-13 持久化成功并回复结果

当客户端向Leader节点发送数据变更请求时，Leader节点会先向自己的日志中加一条日志，但是不提交（不增加commitIndex）。

此时Leader节点通过AppendEntries消息向其他节点同步数据，AppendEntries消息包含了最新追加的日志。当超过半数节点（包括Leader节点自己）追加新日志成功之后，Leader节点会持久化日志并推进commitIndex，然后再次通过AppendEntries消息通知其他节点持久化日志。

这里需要说明的是，AppendEntries消息中除了包括需要复制的日志条目之外，还有Leader节点最新的commitIndex。Follower节点参考Leader节点的commitIndex推进自己的commitIndex，也就是持久化日志。

如果追加日志成功的节点没有过半，Leader节点不会推进自己的commitIndex，也不会要求其他节点推进commitIndex。

在Leader节点推进commitIndex的同时，状态机执行日志中的命令，并把计算后的结果返回给客户端。虽然在图2-13中，是在Follower节点都持久化完成后才开始计算结果，但实际上Raft算法允许Follower的日志持久化和状态机应用日志同时进行。换句话说，只要节点的commitIndex推进了，那么表示状态机应用哪条日志的lastApplied也可以同时推进。

一般情况下，日志复制需要来回发送两次AppendEntries消息。追加一次新日志，推进一次Follower节点的commitIndex。需要发送两次的主要原因是需要确保过半追加成功后，系统才能真正提交日志。假如不确认过半追加，碰到“脑裂”或者网络分区的情况时，会出现数据严重不一致的问题。

以5个服务器节点的系统为例，5个节点分别为A、B、C、D和E，复制流程如下。

（1）一开始Leader是节点A，其他节点都是Follower。  
（2）在某个时间点，A、B两个节点与C、D、E3个节点产生网络分区。网络分区时，节点A无法与节点B以外的节点通信。  
（3）此时节点B仍旧接收得到来自节点A的心跳消息，所以不会变成Candidate。  
（4）节点C、D、E收不到来自节点A的心跳消息，进行了选举，假设节点C成为新的Leader

节点（如图2-14所示）。

![](images/eeb8039fd6a937da806e847f163525842625de0191d4866a3e7b8103e4868d80.jpg)  
图2-14 重新选举后的集群

（5）客户端连接节点A和C分别写入，因为Leader节点并不确认过半写入，所以会导致节点A和C各自增加不同的日志。  
（6）当网络分区恢复时，由于分区内节点（A、B）和分区内节点（C、D、E）各自的日志冲突，因此无法合并。

但如果上述过程中Leader节点确认过半追加后再推进commitIndex，节点A不会持久化日志，并且在网络分区恢复后，分区内节点（C、D、E）的日志可以正确复制到分区内节点（A、B）上，保证数据一致性。

# 2.7 Raft算法中的一些细节问题

# 2.7.1 Leader节点不能使用之前term的日志条目决定commitIndex

在 Raft 算法中，要求节点启动时，commitIndex 一开始为 0，而不是节点日志中最后一条日志的索引。

一个简单的解释是，在Leader节点每次收到数据变更请求时，都往磁盘写入日志，但在日志条目还没有被过半复制到其他节点时，不可以认为最后被写入的日志条目是有效的。特别是Leader节点有可能在过半复制完成前宕机，而原来是Leader的节点重启后，不可以认为自己的日志中最后一条日志条目是有效的，因为过半复制完成前自己就宕机了。

这里还牵扯到以下两个问题。

（1）节点之间日志不同时，以哪个节点为准？

（2）Leader节点如何决定commitIndex？

第（1）个问题可以用一句话回答：以Leader节点为准。所以Follower节点与Leader节点不同的日志条目，会被Leader节点的日志条目覆盖。

第（2）个问题的前提是，Follower节点会跟随Leader节点的commitIndex，所以着重看Leader节点如何更新commitIndex。

在之前日志复制的整体流程中，Leader节点的commitIndex是由Follower节点的复制进度决定的。准确来说，所有Follower节点的matchIndex中，过半的matchIndex会成为新的commitIndex。

举个例子，在一个3服务器节点的系统中，A是Leader节点，A记录了Follower节点B与C的matchIndex，如表2-5所示。

表 2-5 各节点的 matchIndex 与 lastLogIndex  

<table><tr><td>节点/角色</td><td>matchIndex</td><td>lastLogIndex</td></tr><tr><td>A/Leader</td><td></td><td>3</td></tr><tr><td>B/Follower</td><td>2</td><td>2</td></tr><tr><td>C/Follower</td><td>3</td><td>3</td></tr></table>

表2-5中中间列表示具体记录的matchIndex，右列的lastLogIndex代表各节点最后一条日志的索引。表中Leader节点A的lastLogIndex为3，节点C的matchIndex为3，此时可以认为过半写入成功，commitIndex更新为3。

以上是日志同步时的commitIndex更新，接下来考虑节点启动时的commitIndex更新。

理论上，节点启动时虽然没有新的日志，但是过程应该和普通日志复制是一样的，选出Leader节点后，Leader节点复制日志给Follower节点，然后计算matchIndex并更新commitIndex。问题在于，如果Leader节点反复宕机，有可能造成过半写入的数据被覆盖，举一个极端的例子如下。

对于一个5服务器节点的系统，初始状态如下。

（1）节点S1：1，2。  
（2）节点S2：1，2。  
（3）节点S3：1。  
（4）节点S4：1。  
（5）节点S5：1。

带下划线的节点S1是Leader。节点名称后的数字1和2代表日志条目对应的选举term。比如“1,2”表示这个节点有term为1和2的日志条目，“1”表示只有term为1的日志条目。

假如节点S1宕机，剩下4个节点进入Leader节点选举，最后节点S5被选出，term为3。此时5个节点的状态分别如下。

（1）节点S1：1，2。  
（2）节点S2：1，2。

（3）节点S3：1。  
（4）节点S4：1。  
（5）节点S5：1，3。

假如此时节点S5不幸宕机了，同时节点S1启动了，进行新一轮Leader节点选举后，S1被选为Leader节点，term为4，并且节点S1复制日志条目2到S3，此时5个节点的状态分别如下。

（1）节点S1：1，2，4。  
（2）节点S2：1，2。  
（3）节点S3：1，2。  
（4）节点S4：1。  
（5）节点S5：1.3。

看起来好像没问题，但是如果节点S1再次宕机，S5再次启动，在节点S2、S3、S4（日志条目“1,3”在日志比较中会大于日志条目“1,2”）的支持下，S5成为新的Leader。那么节点S5的数据会覆盖节点S1~S3已经过半写入的数据，此时结果如下。

（1）节点S1：1，3。  
（2）节点S2：1，3。  
（3）节点S3：1，3。  
（4）节点S4：1，3。  
（5）节点S5：1，3。

从上面的例子可以看到，即使是已经过半写入的日志条目（term为2的日志），也不能确定在接下来的日志复制中不会被覆盖，这个现象导致commitIndex不能简单地用过半matchIndex来计算。

假如节点S1在复制日志条目时，过半复制了包含自己当前term的日志条目，那么就可以确定这些过半写入的数据肯定不会被覆盖。也就是说，即使节点S1宕机，节点S5重启后也不会被选作新的Leader节点（日志“1,3”不会大于日志“1,2,4”），所以节点S1可以安全地把term4的日志数据所对应的日志索引号作为新的commitIndex。

（1）节点S1：1，2，4。  
（2）节点S2：1，2，4。  
（3）节点S3：1，2，4。  
（4）节点S4：1。  
（5）节点S5：1，3。

换句话说，更新commitIndex时需要判断过半matchIndex所对应日志条目的term。对于Leader节点，只有日志条目的term和自己的term一致才可以更新commitIndex。这个条件很重要，请牢记。

由这个条件衍生出来的另一个细节是，当新Leader节点选举出来之后，如果没有来自客户端的数据更新请求，commitIndex就永远不会更新，因为新Leader节点的term肯定比之前Leader

节点复制过来的最后日志的 term 大。解决方法是，新 Leader 节点选出之后，将当前 term 的一个空操作日志加入 term。这样当这个空操作日志被过半写入之后，Leader 节点就可以更新自己的 commitIndex 了。

# 2.7.2 角色变化表

之前介绍 Raft 算法中的选举时，给出过角色迁移图。但迁移图本身比较粗略，为了更容易地实现，我们需要一个相对完整的角色变化表。

角色变化表主要考察各个角色面对Raft算法消息中的term与currentTerm（自己当前的term）的不同关系时，应该如何正确处理。Raft算法中的消息使用term作为逻辑时钟，正常情况下比较好理解，但是需要注意非正常情况下的处理。

由于消息的请求和响应处理有很大的不同，这里分成了两个表（表2-6和表2-7）。

表 2-6 是针对 RequestVote 和 AppendEntries 中请求的角色变化表。

表 2-6 角色变化表 - 请求  

<table><tr><td>序号</td><td>请求</td><td>Follower</td><td>Candidate</td><td>Leader</td></tr><tr><td>1</td><td>RequestVote.term &gt; currentTerm</td><td>更新 currentTerm 按情况投票</td><td>更新 currentTerm 变成 Follower 按情况投票</td><td>更新 currentTerm 变成 Follower 按情况投票</td></tr><tr><td>2</td><td>RequestVote.term = currentTerm</td><td>不投票</td><td>不投票</td><td>不投票</td></tr><tr><td>3</td><td>RequestVote.term &lt; currentTerm</td><td>返回 currentTerm</td><td>返回 currentTerm</td><td>返回 currentTerm</td></tr><tr><td>4</td><td>AppendEntries.term &gt; currentTerm</td><td>更新 currentTerm 追加日志</td><td>更新 currentTerm 变成 Follower 追加日志</td><td>更新 currentTerm 变成 Follower 追加日志</td></tr><tr><td>5</td><td>AppendEntries.term = currentTerm</td><td>追加日志</td><td>变成 Follower 追加日志</td><td>报错</td></tr><tr><td>6</td><td>AppendEntries.term &lt; currentTerm</td><td>返回 currentTerm</td><td>返回 currentTerm</td><td>返回 currentTerm</td></tr></table>

Raft算法给出了收到RequestVote和AppendEntries消息后，比较term与currentTerm大小的情况，这里再补充一下实现时需要注意的几种情况。

（1）Follower角色收到RequestVote消息。多个Candidate节点出现后，一个Candidate节点已经向Follower节点请求投票了，另一个Candidate节点再来请求投票，此时选择不投票（Follower节点需要在自己本地记录投过票的节点）。  
（2）Candidate角色收到RequestVote消息。在多个Candidate节点的情况下，一个Candidate节点收到了来自另一个Candidate节点的请求，此时选择不投票。

（3）Leader角色收到RequestVote消息。在多个Candidate节点的情况下，一个Candidate节点收到了过半节点的支持成为Leader节点，在向其他节点发送心跳消息前，另一个Candidate节点向Leader节点发来了消息，此时只能选择不投票。  
（4）Candidate角色收到AppendEntries消息。在多个Candidate节点的情况下，一个Candidate节点成为Leader节点，剩下的Candidate节点收到了来自Leader节点的心跳消息，此时必须退化为Follower并追加日志。  
（5）Leader角色收到AppendEntries消息。AppendEntries只能是来自Leader节点的消息，如果term相同，说明有可能出现了多个Leader节点，此时必须报错。

相比请求，响应要简单很多，如表2-7所示。除了自己的角色发出去的消息外，这里一律采用忽略的策略。在明确是自己发出去的消息中，如果返回的term大于currentTerm，则退化为Follower。

表 2-7 角色变化表 - 响应  

<table><tr><td>序号</td><td>响应</td><td>Follower</td><td>Candidate</td><td>Leader</td></tr><tr><td>1</td><td>RequestVote.term &gt; currentTerm</td><td>-</td><td>更新 currentTerm 变成 Follower</td><td>-</td></tr><tr><td>2</td><td>RequestVote.term = currentTerm</td><td>-</td><td>根据结果变成 Leader 或者保持 Candidate</td><td>-</td></tr><tr><td>3</td><td>RequestVote.term &lt; currentTerm</td><td>-</td><td>-</td><td></td></tr><tr><td>4</td><td>AppendEntries.term &gt; currentTerm</td><td>-</td><td>-</td><td>更新 currentTerm 变成 Follower</td></tr><tr><td>5</td><td>AppendEntries.term = currentTerm</td><td>-</td><td>-</td><td>更新 commitIndex</td></tr><tr><td>6</td><td>AppendEntries.term &lt; currentTerm</td><td>-</td><td>-</td><td>-</td></tr></table>

除了RequestVote和AppendEntries消息，之后在日志快照的部分还会增加InstallSnapshot等消息。后续增加的消息基本上都比较简单，只需要处理逻辑时钟term的部分，没有特别复杂的和角色相关的逻辑，这里不再展开讲解。

# 2.8 本章小结

本章从不支持分布式一致性的集群开始，介绍了分布式系统中所使用的日志状态机模型，用线性化操作来保证分布式环境下的数据一致性；基于Quorum机制的写入，用过半写入来保证数据的

持久性；以及基于日志比较的选举，在前两者的基础上自动选出数据最新的Leader节点。这些理论是Raft算法的基础，理解了这些理论，对于理解Raft算法的内部设计很有帮助。

在 Raft 核心算法分析中，选举部分介绍了逻辑时钟 term、角色迁移以及选举超时机制，并和日志复制部分一起给出了整体的流程图，进一步加深读者对 Raft 算法的理解。

最后，特别给出了Raft算法中比较难以理解或者说容易被忽视的commitIndex更新条件的分析，以及用于之后的详细设计的详细角色变化表。理解了这两部分，有助于正确实现Raft算法。

总体来说，分布式系统所使用的算法在具体实现时需要在某种程度上了解为什么这么设计，以及不这么设计会有什么结果，否则会在某些特定的场合出现奇怪的问题。希望本章能给读者一些参考，在理解的基础上实现 Raft 算法。

# 第3章

# 整体设计

上一章分析了Raft算法的核心部分，即选举和日志复制。如果只看这两个部分，Raft算法似乎很容易实现。但实际上，除Raft核心算法之外，Raft算法还包括集群成员管理及日志快照等内容。其中集群成员管理、日志快照等功能几乎是Raft算法的必备功能。

如果只是想大致理解Raft算法或者简单实现Raft算法，比如选举部分，一般参考介绍Raft算法的文章或者raft.github.io上的实现就可以满足需求。但是对于想在生产环境下使用Raft算法（比如要实现各种中间件）的人来说，可能需要深入理解更多的内容。本书的目的之一就是为这些读者提供一些参考。

本章将从设计目标开始，分析状态数据、组件关系及线程模型，从整体上给出算法实现的框架。其中，状态数据分析是本章最重要的内容。除了Raft算法中给出的信息之外，具体实现时要关注的内容也在本章的分析中，建议读者在实现之前好好理解状态数据的内容。

# 3.1 设计目标

在设计之前，先了解一下本书的设计目标。本书的设计目标主要有3个：正确性、状态机的通用性以及足够的性能。

# 3.1.1 正确性

对于分布式算法来说，正确性是最重要的。这里的正确性主要指算法实现上的正确性。保证正确性除了在设计和编码时需要仔细推敲之外，组件编码完成后还需要有足够的单元测试，以及整体需要有功能测试。为此，本书在每个组件实现的最后，都会有一个小节专门讲解测试，以保证章节内实现的内容是正确的。

# 3.1.2 状态机的通用性

Raft算法的基础之一是状态机。任何可以用状态机来表示的服务，理论上都可以基于Raft算法实现一个分布式的版本。

尽管如此，实现一个高扩展性的Raft算法库还是比较困难的。一个原因是，Raft算法的实现包括日志、通信等相对通用但是需要满足Raft算法特性的组件。另一个原因是，在类似于实现集群成员管理的情况下，必须了解现有Raft算法库，并大刀阔斧地对其进行改造才可能得到满足自己需求的算法库。所以一般来说，分布式一致性算法会内置几个事先设计好的、可替换的组件实现，比如Apache ZooKeeper。

本书也按照以上方式，预先设计部分组件实现，比如日志部分分为基于内存和基于文件的两个实现，但不保证完全的通用性。相对地，作为上层服务最关心的状态机部分，设计时要保证通用性。

# 3.1.3 足够的性能

算法设计的最后一个目标是足够的性能。但考虑到正确性，本书不会把高性能作为首要目标。在性能优化方面，Raft算法已经提供了一些方案。除此之外，在实现时可以按照具体情况优化部分代码。但实现中不建议过早优化性能，或者在没有分析清楚正确性的情况下，引入复杂的异步编码或多线程编程来达到所谓的高性能。

从分类上来看，Raft算法属于分布式一致性算法，强一致性下无法保证高可用性。换句话说，服务的TPS（一秒能处理的请求）是有上限的，保证有足够的TPS是设计的首要目标。至于读写比（R：W）下的TPS可以作为上层服务的性能参考，但是并不能保证所有上层服务都是读操作非常多、写操作非常少的服务。

# 3.2 设计和实现顺序

本书按照如下顺序设计并实现

（1）核心功能：选举。  
（2）核心功能：日志复制。  
（3）客户端。  
（4）日志快照。  
（5）集群成员管理。  
（6）其他优化。

设计和实现的原则是从核心开始，一点一点添加功能，这样可以保证关注点较小。同时在设计目标中也提到过，实现完一个组件后，需要做相关的测试，以保证组件的正确性。

核心之后是客户端。虽然很多Raft算法都没有给出客户端的部分，但是客户端对于一个完整的Raft算法实现是必需的部分。而且有了客户端，就能更早地从使用方角度测试代码的正确性。

第（4）项日志快照是Raft算法给出的一个优化方案。没有日志快照的状态机基本上不可能用于生产环境，所以这里将日志快照作为必要项列出。

另一个生产环境下非常必要的功能是集群成员管理，即增加成员服务器、移除成员服务器等操作。Diego Ongaro的博士论文中提出了一种叫作“单服务器变更”（Single-Server Membership Change）的集群成员管理方案，并且给出了实现上的细节。本书实现的是单服务器变更。

最后是 Raft 算法的其他优化方案，这部分主要作为参考，本书并没有完全实现这些优化。

# 3.3 参考实现

之前提到过，Raft算法相较于Paxos算法更容易理解，其中一个原因是Raft算法提供了参考实现LogCabin，可以在GitHub上找到LogCabin的源代码。

LogCabin 主要是用 C++ 编写的。如果对 C++ 比较熟悉，可以阅读一下 LogCabin 的源代码，对如何实现 Raft 算法有一个比较直观的认识。如果对 C++ 不熟悉也没有关系，raft.github.io 的实现列表中有各种语言的实现版本。虽然这些版本大部分都算不上完整，或者难以用于生产环境，但是可以帮助读者以自己熟悉的语言开始，了解 Raft 算法具体的实现方法。

如果希望学习生产环境下 Raft 算法的实现，可以参考用 Go 语言写的 etcd、用 Rust 编写的 tikv，以及用 C++ 编写的 braft。支付宝公开了参考 braft 实现的 soft-jraft，也值得关注。

除此之外，用Java编写的atomix也是非常值得参考的一个实现。atomix的文档本身很丰富，API也非常易懂，提供了Raft算法在KV服务以外的通用化思路。

总体来说，如果不确定在某处如何实现，可以参考既有的实现，了解它们的策略和选择。但是在核心算法方面，还是建议先对Raft算法有一定程度的了解，然后再考虑编码。

# 3.4 状态数据分析

Raft算法的相关论文中给出了每台服务器需要关注的状态数据，下面将对这些状态数据、推断类型以及其他需要的状态数据进行分析。

# 3.4.1 需要持久化的状态数据

每台服务器启动或者关闭时，必须保存的数据如下。

（1）currentTerm：当前选举的 term，推断类型为整数（integer），初始为 0。  
（2）votedFor：投过票给谁。  
（3）log[]：日志条目，注意第一个日志的id为1。

一般来说，votedFor的内容应该是服务器成员的ID或者成员的IP地址加端口。不过从其他状态数据来看，这里也可能是整数，既成员列表中服务器的索引号。具体使用什么类型，本节最后决定。

# 3.4.2 服务器可变状态数据

每台服务器在运行中需要记录的数据如下。

（1）commitIndex：已提交的最高的日志索引，推断类型为整数，初始为0。  
（2）lastApplied：已应用的最高的日志索引，推断类型为整数，初始为0。

这里一方面要注意这两个数据一开始都是0，另一方面从设计上必须满足以下条件。

（1）lastApplied $\leqslant$ commitIndex。  
（2）算法中的commitIndex推进（增大）时，lastApplied会一起推进。

# 3.4.3 服务器可变状态数据

对应上面的Leader服务器可变状态数据，非Leader服务器有以下可变状态数据。

（1）Follower角色服务器的leaderId，即当前Leader服务器的成员ID。  
（2）Candidate角色服务器的votesCount，即作为候选人收到的票数，推断类型为整数。  
（3）不管是什么角色，服务器都需要知道的一个状态数据：当前服务器的角色role。

上述状态数据需要在角色建模时纳入考虑。

# 3.4.4 Leader服务器可变数据状态

Leader 服务器需要记录的数据如下。

（1）nextIndex[]：Follower 服务器的下一个要复制的日志索引，推断类型为整数数组，刚开始时每个元素（nextIndex）为本地日志副本最后的日志索引加 1。

（2）matchIndex[]：Follower 服务器和 Leader 服务器相匹配的日志索引，推断类型为整数数组，刚开始时每个元素（matchIndex）为 0。

从这里可以看出，Raft算法中设定服务器的ID应该是整数，或者说是数组的索引号。

# 3.4.5 服务器ID类型的选择

对于服务器成员固定的集群来说，用整数来标示服务器ID没有任何问题，因为所有服务器共享同一套服务器成员列表（数组），一个简单的4节点集群如图3-1所示。

![](images/9276ed2eb7e3c5cb817b4e9b231fa4f4d2f1d651603d2a9e581d6672dc693110.jpg)  
图3-1 用整数标示服务器ID

但是在后面将会讲解的集群成员变更中，服务器成员列表会有所变化，增加或减少服务器会导致服务器ID不连续。比如说服务器1、服务器2和服务器3组成的集群，增加了服务器4、移除了服务器2之后，剩下服务器1、服务器3和服务器4。这样会导致服务器2的数据不能直接删除，以及在遍历Follower成员列表时，需要判断服务器是否已被移除。

解决方法之一是使用映射表代替服务器成员数组，同时用字符串作为服务器成员的标识（映射表的关键字）。这里的字符串可以是A、B、C这种简单的标识（如图3-2），也可以是服务器IP加端口这种比较具体的方式。

![](images/63db19ffb296544ceaf9c77f5a6969199a3964484b2484106c6421524519c738.jpg)  
图3-2 用映射表标示服务器

本书采用类似于前者的方式，并且设计了一个叫作 NodeId 的类，NodeId 的代码如下。

public class NodeId implementsSerializable{ private final String value; //构造函数 public NodeId(Stringvalue）{ Preconditions.checkNotNull(value); this.value $=$ value;   
1 快速创建实例的一个静态方法

public static NodeId of(String value) { return new NodeId(value);   
//与hashCode方法一起重载   
public boolean equals(Object o){ if(this $= =$ o）return true; if(!o instanceof NodeId))return false; NodeId id $=$ (NodeId)o; return Objects.equals(value，id.value);   
//获取内部的标示符   
public String getValue() { return value;   
//获取哈希码   
public int hashCode(){ return Objects.hash(value);   
//转换为字符串   
public String toString(){ return this.value;

简单来说，这里的 NodeId 只是字符串的一个封装。没有直接使用字符串的原因主要是，假如之后有设计变更，可以不用到处寻找含有服务器节点 ID 的字符串，只需要修改 NodeId 和相关代码就可以了。

NodeId 提供了一个快速构造的静态方法 of，有点类似于 Integer 的 valueOf 方法，当然直接使用构造函数也是没有问题的。

由于 NodeId 需要作为集群成员列表的关键字，因此需要覆写（Override）equals 和 hashCode 方法。

# 3.5 静态数据分析

静态数据指的是Raft算法中没有直接列出，但是在实际使用中需要的不变或者很少变化的数据。服务器成员列表理论上也是配置，但是如果运行中增减服务器，就属于动态变化的数据。

# 3.5.1 配置

基本配置如表3-1所示，之后随着各个章节的展开会有所增加。

表 3-1 基本配置  

<table><tr><td>配置名</td><td>类型</td><td>说明</td><td>本书的默认值</td></tr><tr><td>minElectionTimeout</td><td>int</td><td>最小选举超时间隔</td><td>3000 (ms)</td></tr><tr><td>maxElectionTimeout</td><td>int</td><td>最大选举超时间隔</td><td>4000 (ms)</td></tr><tr><td>logReplicationInterval</td><td>int</td><td>日志复制间隔</td><td>1000 (ms)</td></tr></table>

一般来说，配置会从环境变量、配置文件中读取，并且读取时需要对配置的有效性进行检查。比如这里最小选举超时间隔必须小于或者等于最大选举超时间隔，以及日志复制间隔必须小于最小选举超时间隔。

在刚开始实现的阶段，可以先不从环境变量或者配置文件中读取配置，而使用固定配置。读取时也不做很复杂的校验，之后再完善配置读取和校验部分。

# 3.5.2 集群成员列表

对于固定的成员列表来说，从环境变量或者配置文件中读取即可。但是如果成员列表可变，那么为了在集群启动时读取，就需要考虑在什么地方存储最新的成员列表，可选择的方式有以下3种。

（1）与状态数据一样持久化。  
（2）在日志中存放差分数据，即增减的服务器。  
（3）在日志中存放变更前的列表和差分数据。

这里的选项（2）和（3）是在实现了集群成员变更之后给出的方案。因为集群成员变更会以日志的方式通知所有集群内的节点，所以理论上只需要查看相关集群成员变更的日志，即可计算出最新的成员列表。如果没有集群成员变更的日志，就使用环境变量或者配置文件中的初始配置。

图3-3采用了选项（2）只存放差分数据的方式，最新集群成员列表的计算结果如下。

（1）A:localhost:2333。  
（2）C：localhost:2335。  
（3）D:localhost:2336。

![](images/f26d7219bb933a93ae47c0e40684b4a779fb463768dcca0a8d5adddbc0dd1234.jpg)  
图3-3 日志存放差分数据

图3-4采用了选项（3）的方式，最新集群成员列表的计算结果和图3-3是一样的，区别只是图3-4计算时只需要看最后一个日志项即可。

![](images/3617dc48edce9e050dd6583c7cc17e73b737271bca8322a0d006eb97a4aeb78d.jpg)  
图3-4 日志存放变更前列表和差分数据

选项（2）和（3）的区别是，选项（2）需要最初的成员列表，而选项（3）不需要。考虑到如果环境变量或者配置文件中的初始集群成员的配置变更，可能会导致最终计算结果出错，所以选项（3）相对比较合适。本书也采用这种方式，在之后集群成员变更的章节会有更详细的讲解。

选项（3）的缺点是，可能无法强制集群直接以新的成员列表启动，必须通过手动增减服务器来达到类似的效果。解决方法之一是，提供一种忽略日志并以修改后的初始配置直接启动集群的模式。

# 3.6 集群成员与映射表

由于服务器成员的ID使用字符串的方式实现，那么按照成员ID查找集群成员就需要使用映射表，也就是Map。

一般来说，集群成员表要能实现以下几个必需的操作。

（1）按照成员ID查找成员信息  
（2）遍历成员。

# 3.6.1 集群成员表

实现上述要求的接口和代码如下。

public class NodeGroup{ private final NodeId selfId; //当前节点ID private Map<NodeId, GroupMember> memberMap; //成员表 //单节点构造函数 NodeGroup(NodeEndpoint endpoint){ this(Collectionsi singleton(endpoint),endpoint.getId(); } //多节点构造函数 NodeGroup(Collection<NodeEndpoint> endpoints, NodeId selfId) { this.memberMap $=$ buildMemberMap(endpoints);

this.selfId $=$ selfId;   
1   
//从节点列表中构造成员映射表   
private Map<node id,GroupMember> buildMemberMap( Collection<nodeEndpoint $\rightharpoondown$ endpoints){ Map<nodeid,GroupMember> map $=$ new HashMap<>(); for (NodeEndpoint endpoint: endpoints){ map.put(endpoint.getId(),new GroupMember(endpoint)); } //不允许成员表为空 if(map.isEmpty()）{throw new IllegalArgumentException("endpointsis empty”); } return map;   
1   
//按照节点ID查找成员，找不到时抛错 GroupMember findMember(NodeId id){ GroupMember member $=$ getMember(id); if(member $= =$ null）{ throw new IllegalArgumentException("no such node "+id); 1 return member;   
1   
//按照节点ID查找成员，找不到时返回空 GroupMember getMember(NodeId id) { return memberMap.get(id);}   
//列出日志复制的对象节点，即除自己以外的所有节点 Collection<groupMember> listReplicationTarget(){ return memberMap.values().stream().filter( m->!m.idEquals(selfId).collect(CollectorsToList());   
1

NodeGroup的构造函数支持单节点和多节点两种构造方式。前者退化为单台服务器的构成方式，后者是常规的构造方式。

NodeGroup 中持有当前服务器成员的 ID : selfId。也可以把 selfId 从 NodeGroup 中分离出来，在需要 selfId 的地方再以方法参数的方式传入。

以 NodeId 查找成员信息的 API 有两个，getMember 是不检查成员是否存在直接返回，findMember 是在发现没有此 NodeId 对应的成员时抛出异常，这两个 API 按照场景分开使用。比如 Follower 服务器接收到来自 Leader 服务器的心跳信息，可能需要检查 Leader 服务器是否在 NodeGroup 中，并且在不存在时打印警告日志。当 Follower 服务器收到客户端请求时，需要把客户端引导到 Leader 服务器去处理，这时可以调用 findMember 返回自己所存的 leaderId 对应的成员信息。

很明显，如果系统刚启动或者选举还没结束，Follower 服务器不会有有效的 leaderId，所以结果有可能是 null。

遍历成员列表主要在Leader服务器进行日志复制时进行。日志复制的对象一般来说是除自己以外的所有服务器，所以这里排除了和自己ID（selfId）相同的服务器。

# 3.6.2 集群成员信息

集群成员的主要信息如下。

（1）NodeEndpoint：主要是节点的服务器IP和端口。  
（2）ReplicationState：复制进度，具体字段在日志复制部分介绍，现在可以将它简单理解为包含 nextIndex 和 matchIndex 的一个数据类。

本书把复制进度放进了集群成员信息中。这么做主要是考虑到，假如把复制进度放到其他地方，那么除了根据成员ID查询成员地址（NodeEndpoint）的表之外，还需要一个根据成员ID查询复制进度的表。本书合并了两个表，方便快速查询。当然实现时把两个表分开也没有太大问题，可以根据具体情况来决定。

以下是集群成员的代码。

public class GroupMember{ private final NodeEndpoint endpoint; private ReplicatingState replicatingState; //无日志复制状态的构造函数 GroupMember(NodeEndpoint endpoint){ this(endpoint，null); } //带日志复制状态的构造函数 GroupMember(NodeEndpoint endpoint,ReplicatingState replicatingState) { this.endpoint $=$ endpoint; this.replicatingState $=$ replicatingState; } // getters and setters //获取 nextIndex int getNextIndex() { return ensureReplicatingState().getNextIndex(); } //获取 matchIndex int getMatchIndex() { return ensureReplicatingState().getMatchIndex(); }

//获取复制进度  
```java
private ReplicatingState ensureReplicatingState() { if (replicatingState == null) { throw new IllegalStateException("replication state not set"); } return replicatingState; } 
```

复制进度默认情况下为null，只有当节点成为Leader节点之后，才会重置为实际的复制进度。代码中获取复制进度的相关数据时，先通过ensureReplicatingState方法保证复制进度存在，然后再获取内部数据。如果节点由于某些原因没有重置复制进度，方法会报错。

连接节点的基本信息如下。

```java
public class NodeEndpoint{ private final NodeId id; private final Address address; //节点ID，主机和端口的构造函数 public NodeEndpoint(String id, String host, int port) { this(new NodeId(id), new Address(host, port)); //节点ID和地址的构造函数 public NodeEndpoint(NodeId id, Address address){ Preconditions.checkNotNull(id); Preconditions.checkNotNull(address); this.id = id; this.address = address; // getters and setters   
public class Address{ private final String host; private final int port; //构造函数 public Address(String host, int port){ Preconditions.checkNotNull(host); this.host = host; this.port = port; } // getters and setters 
```

Address 也可以用 SocketInsetAddress 来实现，只是在获取主机名等信息时需要自己做一些处理。

NodeEndpoint额外包含一个成员ID，在从日志中计算集群成员列表等地方使用。

# 3.7 组件分析

在分析完 Raft 核心算法的状态数据和静态数据之后，就可以按照数据的用途归类并推导系统中的模块或者组件了。

以下是本书归纳出来的组件列表，组件之间的关系如图3-5所示。

![](images/ea1737684506bced1fb7ee71ff0caa46f15a7c511ca514bd7df0a0f7261cfbe5.jpg)  
图3-5 组件图

（1）通信组件：此组件不拥有成员表。  
（2）日志组件：拥有commitIndex。  
（3）定时器组件：拥有3个时间配置，即选举超时区间和日志复制间隔。  
（4）成员表组件：拥有成员表和日志复制进度表。  
（5）一致性算法组件：核心组件，拥有除上述组件所拥有的状态数据和配置以外的所有状态数据。

Rafi算法的一些实现把所有数据都放在核心组件或者一个非常大的数据结构中，这么做的一个后果是，所有功能都会读写这个非常大的数据结构，逻辑之间耦合非常厉害，难以分离。合理分割数据并配合相应的操作是面向对象编程的常用做法，这一方面有助于分离逻辑，从而独立开发某块功能；另一方面通过分离数据可以提供并行的可能性。相比之下，单个结构很多时候只会加一个粗粒度锁，没有并行的可能性。

# 3.8 如何解耦组件间的双向调用关系

3.7节的图3-5给出了组件之间的调用关系。核心模块，即一致性算法模块负责调用除定时器以外的所有其他模块，定时器模块会定时调用核心模块，通信模块在收到其他节点的消息时调用一致性模块。

从实现上来说，单向箭头完全没有问题，可以把上述关系想象成一个金字塔一样的依赖图，除通信组件以外的其他组件的依赖关系与构造顺序如图3-6所示。

![](images/57e6dd5a70c9ea898fe7ab27e8813f9e10b70697735728c765ef0189c495bb0b.jpg)  
图3-6 组件依赖关系与构造顺序

启动时只要从下往上构建各个组件就行了，但是如果碰到双向依赖的组件就不行了。比如类A依赖类B，类B依赖类A，这时就不能在构造类A的实例时传入类B的实例，因为构造类B的实例时需要类A的实例。

# 3.8.1 解决方法1：合并一致性算法组件和通信组件

合并一致性算法组件和通信组件。换句话说，就是把一致性算法组件的代码和通信组件的代码放一起。虽然看起来这是最不应该使用的方式，但是一部分Raft算法实现，甚至其他网络服务的程序都是使用这种方式。因为这样最简单，但是相应地代码膨胀也很快，一个类很快就可以超过1000行，达到几千行甚至上万行。

不管使用什么编程语言，都没有办法解决代码快速膨胀的问题，因为这属于代码设计的问题。

# 3.8.2 解决方案2：使用回调

使用回调的代码举例如下。

```txt
class ConsensusComponent {
    private final RpcComponent rpc; // RPC组件
    // 构造函数
public ConsensusComponent(RpcComponent rpc) {
        thisrpc = rpc;
    }
    // 处理消息
public void doSomething(SomePayload payload) {
        rpc.sendMessage(payload, (response) -> {
            // 回调函数中
        });
    }
} 
```

上述代码中，通信组件（RPC Component）作为一致性算法组件（Consensus Component）的成员，在发送信息时调用通信组件的方法并提供了一个回调，通信组件在收到消息的响应之后回调一致性算法组件。

代码中使用了Java8的lambda写法，省略了回调接口的接口名和参数类型声明。回调本身除了写法之外问题不大，只是需要注意RPC的实现。RPC使用同步Socket方式来实现的话很简单，但是如果使用异步IO（比如Netty）的方式来实现，就需要考虑如何保证请求和回调对应起来，更多关于异步IO的内容将在第6章介绍。

# 3.8.3 解决方案3：延迟设置

如果类A与类B相互依赖，那么可以不通过构造函数传入依赖，而是通过setter方法，比如使用如下代码。

class A{ privateBb; //设置依赖的B实例 public void setB(Bb){ this.b $=$ b; 1   
}   
class B{ privateAa; //设置依赖的A实例 publicvoidsetA(Aa）{ this.a=a; 1

因为依赖项A或者B的实例是构造之后才设置的，所以使用时需要注意依赖A或者B有可能为null，以及在系统启动时不能忘记相互设置依赖项。

# 3.8.4 解决方案4：使用Pub-Sub方案

在解决组件的耦合方面，Pub-Sub一直是一个不错的工具。Pub-Sub指的是一方触发事件(Publish，发布），另一方接受（Subscribe，订阅）。双方不完全知道对方的存在，直接依赖（强依赖）变成了间接依赖（弱依赖）。

以系统间耦合为例，如果业务A依赖业务B，业务B又依赖业务A，那么可以在业务A和业务B之间加一个中间系统（Pub-Sub），如图3-7所示，使A和B之间的强依赖变成弱依赖。

![](images/68bc07ac2d4104c9ccf5ffba5859d5b490f374c7b9c8eb7b9494becd5f61039b.jpg)  
图3-7A与B的强依赖变成弱依赖

具体的中间系统可以是MessageQueue类的系统。业务B并不直接调用业务A，业务B向MessageQueue系统发送信息后，订阅者业务A收到消息并回复给业务B。

对于类A和类B的双向依赖，同样可以引入类似于Guava的EventBus这种简单的组件间Pub-Sub工具来弱化依赖关系。代码如下。

```java
class ConsensusComponent {
    // 初始化时注册自己为订阅者
    public void init(EventBus eventBus) {
        eventBus.register(this);
    }
    // 订阅RequestVoteRpc消息
    @Subscribe
    public void onRequestVoteRpc.RequestVoteRpc rpc) {
        // 订阅RequestVoteResult消息
        @Subscribe
        public void onRequestVoteResult(requestVoteResult result) {
            class RpcComponent { 
```

private final EventBus eventBus; //构造函数   
public RpcComponent(EventBus eventBus){ this.eventBus $=$ eventBus;   
//处理消息   
public void process SOCKET socket){ Request request $=$ parseRequest(socket); if(request instanceof RequestVoteRpc){ eventBus.post((RequestVoteRpc)request); //发布RPC消息 } else if(request instanceof RequestVoteResult){ eventBus.post((RequestVoteResult)request); //发布Result消息 1

在上面的代码中，一致性算法组件在init方法中注册自己为订阅者。注册时EventBus会扫描组件的方法，发现有@Subscribe标注的方法时会记录下方法和方法的参数（RequestVoteRpc和RequestVoteResult），实例、方法和方法参数构成一个订阅记录。

另外，RPC组件调用post方法时分别传递了RequestVoteRpc和RequestVoteResult的消息，EventBus按照之前的订阅记录把消息分发给核心组件，完成间接调用。

可以看到，在使用了 EventBus 之后，代码之间的强依赖关系被解耦为相对较弱的依赖关系。一致性算法的部分和 RPC 的部分可以完全分开编码，保证了较小的关注点。

# 3.8.5 解决方案5：借助actor

用过akka的读者可能会知道，akka所基于的actor模型支持按照actor名字发送消息。也就是说，把双向依赖的组件各自作为一个actor的话，就可以在知道对方名字的前提下发送消息，此时不需要持有对方的引用。

# 3.8.6 如何选择

以上给出了5种不同的解决方案。笔者非常不推荐第1种方案，因为很多巨型类就是设计者不知道如何解决双向耦合而导致的。如果不想引入EventBus之类的Pub-Sub机制，那么方案2或者方案3是不错的选择。但从可维护性上来说，第4种方案是最好的选择，也是本书对应的代码中使用的方案。如果是基于akka开发系统，那么可以考虑选择第5种方案。

# 3.9 线程模型分析

除了组件之外，另一个必须考虑的设计是系统整体的线程模型。最简单的线程模型是单线程模型，这种线程模型不用考虑任何与锁、并发相关的问题，但代价是服务整体的性能可能会比较低。当然实际性能如何必须以测试出来的数据为准，本节着重分析和讨论有哪些线程模型方案可以选择。

# 3.9.1 异步IO下的处理模型

对于涉及网络的系统来说，使用传统阻塞（同步）IO还是异步IO会影响系统的线程模型。考虑到大量客户端的场景，本书主要讨论异步IO下的处理模型。

假如使用异步IO，比较常见的是单个Selector（线程）配置多IO处理线程的模型，如图3-8所示。Selector负责事件（连接、可读、可写等）分发，每个IO处理线程会以异步方式处理多个连接的读写。

![](images/b5998877a5dc735fc2366d965559b947f49ca55d6e8f5ea2615cefc87ecc8122.jpg)  
图3-8 NIO处理模型（1）

上述模型存在多个IO线程，这些IO线程会把请求传给系统内具体处理的组件。由于处理逻辑一般很难做成完全并行的，因此如何传递和处理是一个必须考虑的问题。

原则上IO线程内不能有耗时的处理逻辑，因为异步IO下的IO线程是复用的，所以不能长时间处理单个请求，全部IO线程都被占据了的话，系统就无法处理客户端请求了。另外，多个IO线程直接调用具体处理的组件的话，可能会有并发上的问题。

解决方案之一是只允许使用一个IO线程，也就是类似于Redis的方案。这种方案只解决了并发上的问题，并没有解决耗时的处理逻辑的问题。

解决方案之二是给处理组件加锁，此方案同样只解决了并发问题，并没有考虑IO线程内处理耗时的问题。甚至可以说，加锁恶化了IO线程内处理复杂逻辑的耗时问题，因为同一时刻只能进行一个IO线程中的处理，其他IO线程必须等待。

解决方案之三是在IO线程和处理逻辑之间加一个间接层，比如处理队列，如图3-9所示。这样IO线程把请求交给队列之后，可以继续处理其他IO请求。同时异步IO库（比如Netty）提供了

连接对应的Channel，处理逻辑想要回复响应时可以直接调用Channel异步回复，而不用像传统IO一样，IO线程必须等处理完才能回复数据。

![](images/3093e22554e8606f72185e1bdb3f2f679e10aca5e9dd7784a183787d1c2341ec.jpg)  
图3-9 NIO处理模型（2）

注意，处理队列的方案只解决了IO线程中处理耗时的问题，而并发访问处理组件的问题需要从接收端，也就是处理组件端考虑如何解决。很明显，最简单的方法是保证处理逻辑是单线程的。

在队列和处理组件之间，为了实现单线程处理，可以让队列将请求单线程推给处理组件（PUSH模型），也可以让单线程中的处理组件从队列中拉取请求（PULL模型）。这听起来可能有点复杂，但实际上Java内置的线程池类库ThreadPoolExecutor对应的SingleThreadExecutor（通过Executors.newSingleThreadExecutor创建）自带队列和单线程机制，可以直接拿来使用，本书对应的代码也采用此方法编写。

ExecutorService executorService $=$ Executor.newSingleThreadExecutor(); //IO线程中   
executorService.submit()->{ //单线程处理   
}）；

上面的代码创建了一个 SingleThreadExecutor，然后在 IO 线程中提交任务。处理逻辑可以安全地以单线程执行，同时 IO 线程也不会被阻塞。

当然也可以自己实现上述机制，只要满足队列 + 单线程的要求即可。实现时需要注意几个问题，即处理逻辑抛出异常时，异常是否会被 catch、线程是否会退出，以及线程退出后是否会重建。对于 SingleThreadExecutor 来说，线程退出后自动重建。

单线程的另一个好处是，处理组件不需要使用锁之类的机制，因为处理肯定是单线程的，这种处理模式也叫线程封闭。在本书中，处理逻辑所使用的线程被称为主线程。除了主线程的线程封闭之外，本书在其他地方也会使用线程封闭。

# 3.9.2 处理组件的多线程分析

虽然为了简单地处理组件而采用了单线程，但是为了完整分析还是需要考虑一下多线程的处理组件是否可行。需要注意的是，即使处理组件支持多线程访问，在对接IO处理线程时处理时间也必须足够短，如果难以保证，就需要用队列作为缓冲层。

处理组件的多线程分析并不是简单地分析什么地方该使用锁，什么地方该使用队列，因为这些只是解决问题的手段。可以将多线程分析的过程理解为构建一个操作和数据变更的表，从这个表中分析出约束条件，并考虑用什么工具可以满足这些条件，具体如表3-2所示。

表 3-2 操作和数据变更  

<table><tr><td>操作</td><td>当前线程</td><td>节点状态</td><td>日志</td><td>复制进度</td></tr><tr><td>选举超时</td><td>选举超时定时器</td><td>修改，增加 term, Follower-&gt; Candidate</td><td>读取</td><td>N/A</td></tr><tr><td>收到 RequestVote 消息</td><td>IO 线程</td><td>修改，比较 term 后有可 能修改，作为 Follower 需要投票</td><td>读取</td><td>N/A</td></tr><tr><td>收到 RequestVote 回复</td><td>IO 线程</td><td>修改，作为 Candidate 有 可能变成 Leader</td><td>修改，变成 Leader 之后需要 加日志，发送心跳信息时需 要读取日志</td><td>修改，重置</td></tr><tr><td>收到 AppendEntries 消息</td><td>IO 线程</td><td>修改，有可能 Leader/ Candidate -&gt; Follower</td><td>修改，合并日志</td><td>N/A</td></tr><tr><td>收到 AppendEntries 回复</td><td>IO 线程</td><td>修改，有可能 Leader -&gt; Follower</td><td>修改，根据复制进度计算 commitIndex，提交日志</td><td>修改</td></tr></table>

表3-2里给出了Raft核心算法中的5个基本操作，最左列是操作，右边展示了操作所涉及的数据是读取还是修改，以及修改的条件，究竟读取还是修改按照实际完成的Raft算法给出。

总体来说，如果操作有可能是数据修改，那么就认定为修改。通过变更涉及数据的粒度，操作是数据的读取还是修改也会有所变更。这里按照之前给出的组件对数据分类，以下是数据对应的组件。

（1）节点状态：一致性组件（核心组件）。  
（2）日志：日志组件。  
（3）复制进度：成员表组件。

构造表3-2的目的之一是找出比较独立的数据，比如复制进度。因为复制进度只对Leader节点服务器有意义，所以表中只有两处涉及复制进度。

另外，进行细粒度的数据分析有助于找出读写的模式。但很可惜Raft核心算法中基本上每个操作都会修改1～3处数据，所以使用读写锁可能不会有太大的性能提升。关于读写锁是否会比独

占锁有更好的性能，一方面需要进行数据变更分析（而不是仅凭感觉判断），另一方面需要进行实际的性能测试，这里给出的数据变更分析仅作为参考。

从操作频率来看，频率较高的涉及AppendEntries的操作并没有明显可以并行的地方。所以笔者认为，Raft核心算法的操作在整体上并行比较难。不过有一个地方，即日志IO，作为一个相对耗时的IO操作，在精心设计之下有可能可以提高处理效率。

# 3.9.3 日志IO的异步化

考虑到简单性，本书并没有实现异步日志IO，本小节主要是给那些想要实现异步日志IO的读者一些参考。

日志IO的异步化并没有看起来那么简单，异步化可能会破坏Raft核心算法的正确性前提，所以必须分析清楚约束条件，而不是为了异步化而异步化。

在 Raft 核心算法中，Leader 节点服务器的日志会经过以下步骤完全写入文件。

（1）追加日志，但不提交。  
（2）发送日志复制消息给Follower节点服务器。  
（3）过半写入完成后，增加commitIndex，并提交和应用日志。  
（4）发送日志复制请求给Follower节点服务器，Follower节点服务器提交日志。

根据算法的具体实现，实际的日志追加可能是写入文件，也有可能只是写入缓冲，表3-3列出了可能的几种实现。

表 3-3 日志的提交与不提交  

<table><tr><td>操作</td><td>不提交</td><td>提交</td></tr><tr><td>模式1</td><td>写入内存缓冲</td><td>写入文件，清除内存缓冲</td></tr><tr><td>模式2</td><td>写入文件</td><td>无操作</td></tr><tr><td>模式3</td><td>写入内存缓冲，写入文件</td><td>无操作，清除内存缓冲</td></tr></table>

Raft算法的日志追加和数据库的WAL（Write Ahead Log）有所不同，Raft核心算法中的日志只有在过半写入之后才被认为是有效的。提前写入文件（模式2和模式3）的话，有可能之后被回滚。比如2.6.2小节提到的5节点服务器集群，由于网络分区导致Leader服务器无法和其他3个Follower服务器通信时，Leader写入的日志在网络分区之后只会被回滚掉，因为其他3个Follower服务器构成的集群中的日志才被认为是有效的（term比旧Leader高）。

当然，上述情况严格来说属于极端情况，并不常见。模式2的一个问题是，第（3）步提交和应用日志时，如果提前写入文件，必须重新从文件中读取写入的日志条目，这样会有一定的效率损耗。相对地，模式3考虑到了这个问题，同时维护了一个日志缓冲，应用日志时从内存缓冲中获取。

模式1则完全采用延迟写策略。确定过半写入前只使用内存缓冲，正式提交时才写入文件。这样既解决了写入后重新读取的问题，也保证了写入的数据尽量不被回滚，本书对应代码使用此模式。

针对不同的模式，可以异步化的阶段也不同，表3-3中使用下划线标出来的写入文件是主要可以异步化的地方。内存缓冲相关的操作一般来说不用考虑异步化，因为即使异步化了，也只会让代码复杂化，得不到多少性能提升。

在3种模式中，模式3比较适合异步化，其异步化时的操作序列如图3-10所示。

![](images/faaa73935493f7ba58fff0c0fc89e07775398d7b35d7adb0ae3eb99f616c0463.jpg)  
图3-10 模式3的异步化

从理论上来说，提前写入日志和把日志复制到Follower服务器可以并行，从图3-10中也可以看出这一点。但是需要注意，增加commitIndex必须在异步写入之后。也就是说，日志IO必须有先后顺序，所以其实现要求有以下两点。

（1）异步化。

（2）有先后顺序。

这里给出同时满足以上要求的两种实现方式。第一种是异步化所有日志IO的操作加线程封闭，在此基础上异步转同步。具体来说，用之前提到的SingleThreadExecutor线程封闭所有日志IO的操作，异步转同步的地方用Future.get。

第二种是给日志操作加独占锁或者读写锁，异步写入单独一个线程，如图3-11所示。异步写入线程在操作时肯定会获取日志操作的锁，这样之后增加commitIndex就必须等待前面的日志操作完成。

![](images/20a22ac2180cd1cf97412158613628665f3e4b8cbd91bf285f57be9378ab724c.jpg)  
图3-11 日志线程与锁

# 3.9.4 整体线程设计

本书为了使读者易于理解，采用了单线程设计的模式，具体如图3-12所示。

![](images/ccd9e14da251da64b5bfc8de45f14ba23e406f2178c5eaea4f1c89cc1a78da2c.jpg)  
图3-12 单线程设计模式

通信部分使用了基于Netty的NIO，Raft和服务使用不同的端口，但是共用IO线程池。关于通信部分，之后的章节会有更详细的讲解。

从IO线程过来的请求会先追加到队列中，再由单线程的处理组件处理，实际代码使用基于SingleThreadExecutor的线程封闭方式，同时满足队列和单线程处理要求。

代码中为了方便调试，除了IO线程之外，给主要线程都设置了相应的名字，表3-4是主要线程的名称和对应描述。

表 3-4 线程名  

<table><tr><td>线程名</td><td>描述</td></tr><tr><td>node</td><td>主线程，为了区分与 Java 程序的主线程，所以没有使用 main</td></tr><tr><td>state-machine</td><td>状态机</td></tr><tr><td>schedule</td><td>定时器</td></tr><tr><td>group-config-change</td><td>集群配置变更专用线程</td></tr></table>

# 3.10 项目准备

在正式进入编码之前，先准备一下编码的环境。

# 3.10.1 构建环境

本书主要使用Java8作为实现语言，因为本书的实现会使用Java的lambda表达式。Java从8版本开始支持lambda表达式，理论上任何Java8之后的版本都没有问题。

构建工具使用maven。可以自己搭建Java开发环境，然后用mvn（maven的命令）编译、测试

和打包，也可以直接使用IDE内藏的maven。

maven 的版本没有特定要求，一般来说 maven 3.0 以上都可以正常运行。如果有问题，建议更新 maven 的版本。

# 3.10.2 编辑器

本书并没有假设读者使用特定的编辑器或者IDE。可以使用任何自己喜欢的编辑器，或者尝试一下Java几大免费的IDE Eclipse、Netbeans或者Intellij Community Edition。

因为篇幅原因，本书的代码都省去了package和import语言，如果有IDE，应该可以快速导入。

# 3.10.3 Protocol Buffer

本书使用ProtocolBuffer进行序列化和反序列化工作，为了从IDL（接口定义文件）生成序列化和反序列化代码，需要安装ProtocolBuffer的命令行工具。

可以从GitHub上的Protocol Buffer的Releases下找到已经编译好的可执行文件，地址为https://github.com/protocolBufferser/protobuf/releases。

也可以使用系统的包管理工具，比如在 Ubuntu 下执行以下命令。

```batch
sudo apt-get install protobuf-compiler 
```

在Mac下可以执行以下命令。

```txt
brew install protobuf 
```

安装完之后，尝试在命令行中执行以下命令。

```batch
protoc --help 
```

如果能正确输出ProtocolBuffer的帮助信息，表示已经正确安装了ProtocolBuffer。

在本书中使用ProtocolBuffer生成序列化和反序列化代码时，主要执行以下命令。

```txt
protoc -java_out=src/main/java file.proto 
```

java_out表示输出的目录，file.proto是IDL文件。如果在使用过程中出现问题，建议查阅ProtocolBuffer的帮助文档。

# 3.10.4 多模块maven工程

考虑到扩展性，本书设计实现的Raft算法分为以下两部分。

（1）Raft核心：raft-core。  
（2）典型状态机KV服务：raft-kvstore。

拆分 Raft 核心算法和常见的基于 Raft 算法的 KV 服务，有助于拆分 Raft 专属逻辑和状态机内

部的逻辑，这也是本章前面提到的“状态机的通用性”。

raft-kvstore中除了服务端的实现之外，还包括kvstore的客户端实现，具体会在之后的章节讲解。在maven中可以相对简单地创建多模块工程，本书给出手动创建的方式，先创建以下目录。

![](images/1741edb544ad24e243578d4ee8d3ee33ce187b01e04edccf32b225482329850a.jpg)

然后在顶层的 pom.xml 中编写以下内容。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi: schemaLocation="http://maven.apache.org/POM/4.0.0
http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>raft</groupId>
    <artifactId>raft-parent</artifactId>
    <packaging>pom</packaging>
    <version>0.1.1-SNAPSHOT</version>
    <name>raft</name>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <netty.version>4.0.36.Final</netty.version>
    </properties>
</modelVersion> 
```

```xml
<modules>
    <module>raft-core</module>
    <module>raft-kvstore</module>
</modules>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.quava</groupId>
            <artifactId>guava</artifactId>
            <version>25.1-jre</version>
            <exclusions>
                <exclusion>
                    <groupId>org.checkerframework</groupId>
                    <artifactId>checker-qual</artifactId>
                </exclusion>
            <groupId>com.google.errorprone</groupId>
            <artifactId>error_prone Annotations</artifactId>
            </exclusion>
            <exclusion>
                <groupId>com.google.j2objc</groupId>
                <artifactId>j2objc-annotations</artifactId>
            </exclusion>
            <exclusion>
                <groupId>org.apache.mojjo</groupId>
                <artifactId>animal-sniffer-annotations</artifactId>
            </exclusion>
            </exclusions>
    </dependency>
    <!-- https://mvnrepository.com/artifact/com.google.protobuf/])
protobuf-java-->
    <dependency>
        <groupId>com.google.artobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>3.6.0</version>
    </dependency>
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty%-handler</artifactId>
        <version>$({netty-version})</version>
    </dependency> 
```

```xml
<--https://mvnrepository.com/artifact/org.apache.logging.   
log4j/log4j-slf4j-impl -->   
<dependency> <groupId>org.apachelogying.log4j</groupId> <artifactId>log4j-slf4j-impl</artifactId> <version>2.11.1</version>   
</dependency>   
<dependency> <groupId>commons-cli</groupId> <artifactId>commons-cli</artifactId> <version>1.4</version>   
</dependency>   
<--https://mvnrepository.com/artifact/org.jline/jline -->   
<dependency> <groupId>org.jline</groupId> <artifactId>jline</artifactId> <version>3.9.0</version>   
</dependency>   
<dependency> <groupId>jenit</groupId> <artifactId>jenit</artifactId> <version>4.11</version> <scope test</scope>   
</dependency>   
</dependencies>   
</dependencyManagement>   
<build> <pluginManagement> <plugins> <plugin> <artifactId>maven-clean-plugin</artifactId> <version>3.0.0</version> </plugin> <plugin> <artifactId>maven-resources-plugin</artifactId> <version>3.0.2</version> </plugin> <plugin> <artifactId>mavenCompiler-plugin</artifactId> <version>3.7.0</version> <configuration> 
```

```xml
<source>8</source> <target>8</target> </configuration> </plugin> <plugin> <artifactId>maven-surefire-plugin</artifactId> <version>2.20.1</version> </plugin> <plugin> <artifactId>maven.jar-plugin</artifactId> <version>3.0.2</version> </plugin> <plugin> <artifactId>maven-install-plugin</artifactId> <version>2.5.2</version> </plugin> <plugin> <artifactId>maven-assembly-plugin</artifactId> <version>2.6</version> </plugin> </plugins> </pluginManagement> </build> 
```

顶层的pom.xml中，包含以下配置。

（1）依赖的类库。  
（2）插件。

其中主要依赖的类库如表3-5所示

表 3-5 依赖类库  

<table><tr><td>类库名</td><td>描述</td></tr><tr><td>guava</td><td>EventBus 和方便的工具类</td></tr><tr><td>protobuf-java</td><td>Protocol Buffer 的 Java 语言绑定</td></tr><tr><td>netty handler</td><td>Netty 的处理器。直接使用 netty-all 会出现很多不会使用的类库，所以这里做了一些裁剪。Netty 的版本为 4.0</td></tr><tr><td>log4j-slf4j-impl</td><td>日志</td></tr><tr><td>commons-cli</td><td>命令行解析</td></tr><tr><td>jline</td><td>命令行程序</td></tr><tr><td>jenit</td><td>测试框架</td></tr></table>

raft-core中的pom.xml内容如下。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:schemaLocation="http://maven.apache.org/POM/4.0.0"
http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <artifactId>raft-core</artifactId>
    <packaging.jar</packaging>
    <version>0.1.1-SNAPSHOT</version>
    <name>raft-core</name>
    <parent>
        <groupId>raft</groupId>
        <artifactId>raft-parent</artifactId>
        <version>0.1.1-SNAPSHOT</version>
    </parent>
    <dependencies>
        <dependency>
            <groupId>org.apachelogging.log4j</groupId>
            <artifactId>log4j-slf4j-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.quava</groupId>
            <artifactId>guava</artifactId>
        </dependency>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty%-handler</artifactId>
        </dependency>
        <dependency>
            <groupId>jenit</groupId>
            <artifactId>jenit</artifactId>
            <scope test</scope> 
```

```xml
</dependency>  
</dependencies>  
</project>  
raft-core导出了顶层pom.xml中定义的一些类库。  
raft-kvstore中的pom.xml内容如下。  
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi: schemaLocation="http://maven.apache.org/POM/4.0.0" http://maven.apache.org/xsd/maven-4.0.0.xsd">  
<modelVersion>4.0.0</modelVersion>  
<artifactId>raft-kvstore</artifactId>  
<packaging>jar</packaging>  
<version>0.1.1-SNAPSHOT</version>  
{name>xraft-kvstore</name}  
<parent>  
<groupId>raft</groupId>  
<artifactId>raft-parent</artifactId>  
<version>0.1.1-SNAPSHOT</version>  
</parent>  
<dependencies>  
<dependency>  
<groupId>raft</groupId>  
<artifactId>raft-core</artifactId>  
<version>0.1.1-SNAPSHOT</version>  
</dependency>  
<dependency>  
<groupId>commons-cli</groupId>  
<artifactId>commons-cli</artifactId>  
</dependency>  
<dependency>  
<groupId>org.jline</groupId>  
<artifactId>jline</artifactId>  
</dependency>  
<dependency>  
<groupId>jenit</groupId> 
```

```xml
<artifactId>junit</artifactId> <scope>test</scope> </dependency>   
</dependencies>   
<build> <plugins> <plugin> <groupId>org.apache.mojo</groupId> <artifactId>exec-maven-plugin</artifactId> <version>1.6.0</version> </plugin> <plugin> <artifactId>maven-assembly-plugin</artifactId> <configuration> <descriptors> <descriptor>src/assembly/bin.xml</descriptor> </descriptors> </configuration> </plugin> </plugins> </build>   
</project> 
```

raft-kvstore 导出了顶层 pom.xml 中的一些类库及 raft-core。插件 maven-assembly-plugin 是一个用于打包的工具，配置在 src/assembly/bin.xml 文件中。

尝试从IDE打开项目，如果成功请继续看下一章，否则请查询如何搭建多模块maven工程的相关资料。

# 3.11 本章小结

本章从设计的目标开始，分析了设计与实现的顺序并给出了参考实现。在列出了Raft算法实际所需要的状态数据之后，将其归类为不同组件的内部数据，并给出了静态数据分析，以及集群成员与映射表。同时分析了组件之间的关系，针对不同组件之间的双向依赖关系给出了几种解决方案。

在线程模型方面，分析了异步IO下的处理模型，以及多线程化处理组件的可行性，最后给出了日志IO异步化时的一些建议。

分析线程模型之后，提供了开发环境，以及Protocol Buffer的下载和安装指导，并给了一个基本的多模块maven工程框架，作为之后开发的基础。

整体来说，实现Raft算法除了要理解Raft算法本身之外，还需要明白如何使用手中的工具来满足Raft算法的要求，希望本章能给读者一个相对宏观的认识。

# 第4章

# 选举实现

本章开始正式实现Raft算法中的选举部分。Raft算法中的选举需要依赖日志的比较，但考虑到日志组件实现的复杂性，本章先假设所有日志相关的比较都返回OK，也就是无条件投票。现阶段选举的实现主要关注当前服务器的角色、选举超时和节点之间通信的消息，以及对应的处理。

本章主要包括以下内容。

（1）角色建模。  
（2）定时器组件。  
（3）基本RPC组件和消息。  
（4）核心组件。  
（5）测试。

考虑到完全实现RPC组件需要时间，所以本章的测试主要是单元测试，而不是实际启动服务的功能测试。

# 4.1 角色建模

第2章中给出了服务器节点的3种角色，即Follower、Candidate和Leader之间的转换条件，第3章给出了状态数据的分析，将两者结合起来，可以得到表4-1。

表 4-1 角色与状态数据  

<table><tr><td></td><td>Follower</td><td>Candidate</td><td>Leader</td></tr><tr><td>term</td><td>Y</td><td>Y</td><td>Y</td></tr><tr><td>votesCount（收到的票数）</td><td></td><td>Y</td><td></td></tr><tr><td>votedFor（投票给了谁）</td><td>Y</td><td></td><td></td></tr><tr><td>leaderId</td><td>Y</td><td></td><td></td></tr><tr><td>复制进度</td><td></td><td></td><td>Y</td></tr><tr><td>选举超时定时器</td><td>Y</td><td>Y</td><td></td></tr><tr><td>日志复制定时器</td><td></td><td></td><td>Y</td></tr></table>

从表4-1中可以看出，不同角色需要的数据不同。在角色变化的时候，如果一个一个地去修改相关数据，会很容易出错。特别是之后角色增加了新属性，还要到处查找需要设置的地方。为了减少角色变化时数据设置的出错率，可以采用以下方法。

（1）把角色变化时的修改放在一个方法中，比如becomeXXX方法。  
（2）为每个角色建模，即抽取共同的字段作为父类数据，然后给每个角色单独创建一个子类。

笔者推荐使用后者。因为后者把相关数据进行了归类，减少了需要关注的字段，使代码更简洁，以下是具体建模后的父类代码。

```java
abstract class AbstractNodeRole {
    private final RoleName name;
    protected final int term;
    // 构造函数
    AbstractNodeRole(RoleName name, int term) {
        this.name = name;
        this.term = term;
    }
    // 获取当前的角色名
    public RoleName getName() {
        return name;
    }
    // 取消超时或者定时任务
    public abstract void cancelTimeoutOrTask();
    // 获取当前的 term
    public int getTerm() { 
```

```txt
return term; 
```

角色共通的代码在这里有两个，分别是角色名（RoleName）和 term。

```txt
public enum RoleName { FOLLOWER, CANDIDATE, LEADER; } 
```

增加RoleName的目的是减少使用instanceOf进行类型比较的次数，比如类似于下面的比较代码。

```txt
if(currentRole instanceof FollowerNodeRole) {} 
```

部分消息的处理只针对某个特定角色，在检查时通过 getName 方法可以快速排除非预期的角色。

抽象方法 cancelTimeoutOrTask 提供了一个统一的接口，用于取消每个角色对应的选举超时或者日志复制定时任务（每个角色至多对应一个超时或者定时任务）。实际代码中，从一个角色转换到另一个角色时，必须取消当前的超时或者定时任务，然后创建新的超时或者定时任务。

# 4.1.1 Follower角色

以下是Follower角色对应的代码。

```java
public class FollowerNodeRole extends AbstractNodeRole{ private final NodeId votedFor; //投过票的节点，有可能为空 private final NodeId leaderId; //当前leader节点ID，有可能为空 private final ElectionTimeout electionTimeout; //选举超时 //构造函数 public FollowerNodeRole(int term, NodeId votedFor, NodeId leaderId, ElectionTimeout electionTimeout) { super(ROLENAME.FOLLOWER, term); this.votedFor = votedFor; this.leaderId = leaderId; this.electionTimeout = electionTimeout; } //获取投过票的节点 public NodeId getVotedFor() { return votedFor; } //获取当前leader节点ID public NodeId getLeaderId() { return leaderId; } //取消选举定时器 
```

```java
public void cancelTimeoutOrTask() { electionTimeout.cancel(); } @Override public String toString() { return "FollowerNodeRole[" + "term=" + term + ", leaderId=" + leaderId + ", votedFor=" + votedFor + ", electionTimeout=" + electionTimeout + ']'; } 
```

在 FollowerNodeRole 中，除了 NodeId 类型的 votedFor 和 leaderId 之外，还有一个对应选举超时的 electionTimeout 字段，类型是 ElectionTimeout。定时器在之后的小节会有讲解，现在仅需要知道 ElectionTimeout 支持 cancel 就可以了。

注意，角色对应的类的字段都是不可变的，也就是说，Follower 选举超时或者接收到来自 Leader 节点服务器的心跳信息时，必须新建一个角色实例。这样做一方面可以保证并发环境下的数据安全，另一方面可以简化设计。

# 4.1.2 Candidate角色

CandidateNodeRole 和 FollowerNodeRole 很像，CandidateNodeRole 同样有一个选举超时字段。

```java
public class CandidateNodeRole extends AbstractNodeRole{ private final int votesCount; // 票数 private final ElectionTimeout electionTimeout; // 选举超时 // 构造函数，票数1 public CandidateNodeRole(int term, ElectionTimeout electionTimeout) { this(term, 1, electionTimeout); } // 构造函数，可指定票数 public CandidateNodeRole(int term, int votesCount, ElectionTimeout electionTimeout) { super (RoleName.CANDIDATE, term); this.votesCount = votesCount; this.electionTimeout = electionTimeout; } // 获取投票数 public int getVotesCount() { return votesCount; 
```

```java
} //取消选举超时 public void cancelTimeoutOrTask(){ electionTimeout.cancel(); } @Override public String toString(){ return "CandidateNodeRole{" + "term=" + term + ", votesCount=" + votesCount + ", electionTimeout=" + electionTimeout + '}'; } 
```

CandidateNodeRole 提供了两个构造函数，一个预设 votesCount，即投票数为 1；另一个 votesCount 则需要输入。前者是在节点发起选举并变成 Candidate 角色时调用，后者主要在收到其他节点的投票时使用。

也可以给CandidateNodeRole增加特定的增加票数的方法，但需要注意选举超时的处理。一般来说，增加票数意味着重置选举超时，所以需要取消当前的选举超时。

```txt
public CandidateNodeRole increaseVotesCount(ElectionTimeout electionTimeout) { this.electionTimeout.cancel(); return new CandidateNodeRole(term, votesCount + 1, electionTimeout); } 
```

简单起见，ElectionTimeout没有重置方法，所以上述方法需要从外部传入新的选举超时的实例。从上面的代码中可以看到，增加票数的逻辑其实并不复杂，所以本书删除了上述单独增加票数的方法。

# 4.1.3 Leader角色

Leader角色下虽然没有选举超时，但是需要定时给Follower节点发心跳消息，所以有一个日志复制的定时器，具体的Leader角色代码如下。

public class LeaderNodeRole extends AbstractNodeRole{ private final LogReplicationTask logReplicationTask; //日志复制定时器 //构造函数 public LeaderNodeRole(int term,LogReplicationTask logReplicationTask) { super(ROLENAME.LEADER，term); this.logReplicationTask $=$ logReplicationTask;

//取消日志复制定时任务public void cancelTimeoutOrTask(){logReplicationTask.cancel();1@Overridepublic String toString(){return"LeaderNodeRole{term $= \text{"}$ +term $+$ ",logReplicationTask $=$ " $^+$ logReplicationTask $+1$ }';

第2章提到本书把日志复制进度和集群成员表放在了一起，所以Leader的角色数据中没有日志复制进度。

和 Follower、Candidate 角色不同，日志复制定时器不需要重置，所以基本上 Leader 角色的实例被创建之后，除非切换成其他角色，否则不会有修改。

# 4.1.4 关于状态机模式

以上的角色建模侧重于数据包装，没有特别考虑各个角色下的行为。为各个角色建模后，可以按照第2章的角色变迁表给各个角色单独增加方法，即实现状态机模式（State Machine Pattern，非Raft算法中的日志状态机）。笔者按照状态机模式实现过一次，遇到的问题主要有以下两点。

（1）角色变更时涉及的其他状态过多，比如日志、集群成员表等。  
（2）消息处理往往只针对某个特定角色。父类抽象化大部分处理逻辑，子类个性化的场景很少。

第一个问题可以通过传入 StateMachineContext 之类的上下文，间接访问其他状态数据来解决。第二个问题的深层原因是角色数量较少，只有 3 个，而且状态迁移固定，使得状态机模式的优势没有那么突出，因此本书的代码中并没有使用角色状态机。

# 4.2 定时器组件

Raft核心算法中主要有两种定时器，即选举超时和日志复制定时器。前者只执行一次，后者可执行多次。

选举超时的主要操作有以下3种。

（1）新建选举超时。  
（2）取消选举超时。  
（3）重置选举超时。

操作3可以用操作2和操作1的组合，即先取消再新建的方式来代替。实际代码中，重置选举超时主要在Follower角色收到Leader角色的心跳消息时进行，其他时候大部分都是取消加新建定时器的组合。比如Candidate角色收到足够的票数变成Leader角色，需要取消Candidate角色的选举超时，然后新建一个日志复制定时器，此时不能说是重置选举超时。

本书的定时组件只提供新建和取消两个操作。新建通过 Scheduler，取消通过 Scheduler返回的 ElectionTimeout 或者 LogReplicationTask 的 cancel 方法。

简单起见，Scheduler的实现直接使用了JDK中的单线程定时器，也就是Executors的newSingleThreadScheduledExecutor，此定时器自带一个执行线程，不会出现两个定时器同时执行的情况。

# 4.2.1 Scheduler接口

以下是 Scheduler接口的代码。代码使用接口而不是类的主要原因是，在测试时可以使用其他Scheduler实现代替，包括假的定时器实现。

```java
public interface Scheduler {
    // 创建日志复制定时任务
    LogReplicationTask scheduleLogReplicationTask(Runnable task);
    // 创建选举超时器
    ElectionTimeout scheduleElectionTimeout(Runnable task);
    // 关闭定时器
    void stop() throws InterruptedException;
}
```

定时器包含一个stop方法，因为一般来说定时器会有相关的线程存在。以下代码是默认的定时器实现。

```java
public class DefaultScheduler implements Scheduler {
    private final int minElectionTimeout; // 最小选举超时时间
    private final int maxElectionTimeout; // 最大选举超时时间
    private final int logReplicationDelay; // 初次日志复制延迟时间
    private final int logReplicationInterval; // 日志复制间隔
    private final Random electionTimeoutRandom; // 随机数生成器
    private final ScheduledExecutorService scheduledExecutorService;
    // 构造函数
    public DefaultScheduler(
        int minElectionTimeout, int maxElectionTimeout,
        int logReplicationDelay, int logReplicationInterval) {
            // 判断参数是否有效
            // 最小和最大选举超时间隔
            if (minElectionTimeout <= 0 || maxElectionTimeout <= 0 ||
                minElectionTimeout > maxElectionTimeout) {
                    throw new IllegalArgumentException( 
```

"election timeout should not be 0 or min $>$ max");   
}   
// 初次日志复制延迟以及日志复制间隔 if(logReplicationDelay $<  0$ ||logReplicationInterval $\leqslant 0$ ）{ throw new IllegalArgumentException( "log replication delay $<  0$ or log replication interval $\leqslant 0$ "）;   
} this.minElectionTimeout $=$ minElectionTimeout; this.maxElectionTimeout $=$ maxElectionTimeout; this.logReplicationDelay $=$ logReplicationDelay; this.logReplicationInterval $=$ logReplicationInterval; electionTimeoutRandom $=$ new Random(); scheduledExecutorService $=$ Executors.newSingleThreadScheduledExecutor( r->new Thread(r，"schedule"));

DefaultScheduler 主要包含 4 个配置，选举超时区间和日志复制相关的两个配置。构造函数会做一些基本的校验，防止参数不正确。最后一行代码中，设置定时器线程名为“schedule”，这样方便调试。

# 4.2.2 选举超时

接下来是DefaultScheduler中创建选举超时的部分。

public ElectionTimeout scheduleElectionTimeout( Runnable task) { //随机超时时间 int timeout = electionTimeoutRandom.nextInt( maxElectionTimeout - minElectionTimeout) + minElectionTimeout; ScheduledFuture $<  ? >$ scheduledFuture $=$ scheduledExecutorService.schedule( task,timeout,TimeUnit.MILLISECONDS); return new ElectionTimeout(scheduledFuture); }

按照 Raft 算法要求，为了减少 split vote 的影响，在选举超时区间内随机选择一个超时时间，而不是固定的选举超时。然后通过 scheduledExecutorService 的 schedule 创建一个一次性的 ScheduledFuture，最后创建一个 ElectionTimeout 实例。

选举超时 ElectionTimeout 的代码如下。

```java
public class ElectionTimeout {
    private final ScheduledFuture<> scheduledFuture;
    // 构造函数
    public ElectionTimeout(ScheduledFuture<> scheduledFuture) { 
```

thisscheduledFuture $=$ scheduledFuture;   
1   
//取消选举超时   
public void cancel() { thisscheduledFuture.cancel(false);   
}   
@override   
public String toString(){ //选举超时已取消 if(thisscheduledFuture.isCancelled()){ return "ElectionTimeout(state=cancelled)"; 1 //选举超时已执行 if (thisscheduledFuture.isDone()){ return "ElectionTimeout(state=done)"; 1 //选举超时尚未执行，在多少毫秒后执行 return "ElectionTimeout{delay $=$ " + scheduledFuture.getDelay(TimeUnit.MILLSECONDS) $^+$ "ms"；

ElectionTimeout简单来说只是ScheduledFuture的一个封装，功能上只公开了取消的方法（理论上来说，重复取消或者取消已完成的任务不会有问题）。另外，ElectionTimeout重载了 toString方法，方便调试。

ElectionTimeout所封装的ScheduledFuture是一个接口，也就是说，可以通过不同的ScheduledFuture实现来间接修改ElectionTimeout的行为。比如写一个实现Null Object设计模式的ScheduledFuture，这样ElectionTimeout的任何操作都不会有实际作用，之后的测试部分会有具体代码。

# 4.2.3 日志复制定时器

Raft核心算法中的另一种定时器是负责心跳消息的日志复制定时器。其代码实现基本和选举超时一样，只是日志复制定时器是固定间隔执行的定时器。

DefaultScheduler中创建日志复制定时器的代码如下。

public LogReplicationTask scheduleLogReplicationTask(Runnable task) { ScheduledFuture $<  ?>$ scheduledFuture $=$ thisscheduledExecutorService.scheduleWithFixedDelay( task, logReplicationDelay, logReplicationInterval, TimeUnit. MILLSECONDS);

```javascript
return new LogReplicationTask(scheduledFuture); 
```

和选举超时不同，这里调用的是scheduleWithFixedDelay方法。参数logReplicationDelay表示第一次执行的等待时间，logReplicationInterval表示执行间隔。

LogReplicationTask 代码如下。

public class LogReplicationTask {
    private final ScheduledFuture $<\text{?}>$ scheduledFuture;
    // 构造函数
    public LogReplicationTask(ScheduledFuture $<\text{?}>$ scheduledFuture) {
        thisscheduledFuture = scheduledFuture;
    }
    // 取消日志复制定时器
    public void cancel() {
        thisscheduledFuture.cancel(false);
    }
    @Override
    public String toString() {
        return "LogReplicationTask{delay=" + scheduledFuture.getDelay(TimeUnit.MILLSECONDS) + "}";
    }
}

代码构成基本和 ElectionTimeout 一样，这里不再赘述。

# 4.3 消息建模

Raft核心算法中主要有两种消息，一种是RequestVote，另一种是AppendEntries，请求加上响应，共需要4个类。

# 4.3.1 RequestVote消息

请求类 RequestVoteRpc 的代码如下。

public class RequestVoteRpc{ private int term; //选举term private NodeIdcandidateId; //候选者节点ID，一般都是发送者自己 private int lastLogIndex $= 0$ ；//候选者最后一条日志的索引 private int lastLogTerm $= 0$ ；//候选者最后一条日志的term

//getter and setter   
@override   
public String toString() { return"RequestVoteRpc{" + "candidateId $=$ " $^+$ candidateId $^+$ ",lastLogIndex $=$ " $^+$ lastLogIndex $^+$ ",lastLogTerm $=$ " $^+$ lastLogTerm $^+$ ",term $=$ " $^+$ term $^+$ '}';   
}

RequestVoteRpc并没有检查字段是否为空之类的机制，消息是否有效主要还是靠代码自身的处理。

响应类 RequestVoteResult，因为字段简单，直接做成了一个不可变类。

public class RequestVoteResult { private final int term; // 选举term private final boolean voteGranted; // 是否投票 // 构造函数 public RequestVoteResult(int term,boolean voteGranted) { this.term $=$ term; thisvoteGranted $=$ voteGranted; } // 获取term public int getTerm() { return term; } //获取是否投票 public boolean isVoteGranted(){ return voteGranted; } @Override public String toString(){ return "RequestVoteResult[" + "term=" + term + ",voteGranted $=$ " + voteGranted + ']';   
1

# 4.3.2 AppendEntries消息

AppendEntries 消息相对复杂一些，以下是请求类 AppendEntriesRpc 的代码。

public class AppendEntriesRpc { private int term; // 选举term private NodeId leaderId; // leader节点ID private int prevLogIndex $= 0$ ; //前一条日志的索引 private int prevLogTerm; //前一条日志的term private List<Entry> entries $=$ Collections.emptyList(); //复制的日志条目 private int leaderCommit; // leader的commitIndex   
//getter and setter   
@override   
public String toString() { return "AppendEntriesRpc{" + ",entries.size $= \text{串}$ +entries.size(）+ ", leaderCommit $=$ " + leaderCommit + ", leaderId $=$ " + leaderId + ", prevLogIndex $=$ " + prevLogIndex + ", prevLogTerm $=$ " + prevLogTerm + ", term $=$ " + term + '}';   
1

Raft算法中，entries不是用数组而是用List来实现的，理论上使用哪个并没有太大区别，只是List相比之下容易操作一些。

Entry类在本章不会详细介绍，如果想尽快运行起来，可以暂时用Object来代替。在本章的测试环节，entries都是空列表。

响应类 AppendEntriesResult 如下。同样由于字段简单，响应类被设计为一个不变类。

```java
public class AppendEntriesResult {
    private final int term; // 选举term
    private final boolean success; // 是否追加成功
    // 构造函数
    public AppendEntriesResult(int term, boolean success) {
        this.term = term;
        this.success = success;
    }
    // 获取term
    public int getTerm() { 
```

return term;   
}   
//获取是否成功   
publicbooleanisSuccess() { return success;   
}   
@override   
public String toString(){ return "AppendEntriesResult[" $^+$ ",success $=$ " $^+$ success $^+$ ",term $=$ " $^+$ term $^+$ ']';   
1

和 RequestVote 不同，在处理 AppendEntries 响应时需要请求时的数据，在之后的部分会更具体地分析。

# 4.4 关联组件和工具

核心组件是相对复杂的一个组件，作为设计中的核心部分，需要整合各个组件以及负责处理组件之间的交互关系。核心组件包含或者关联的内容如下。

（1）AbstractNodeRole，当前节点的角色信息。  
（2）定时器组件。  
（3）成员表组件。  
（4）日志组件。  
（5）RPC组件。

基本上所有组件都和核心组件有关系。从编码上来说，不建议让核心组件直接持有各个组件的引用，那样核心组件依赖的内容会非常多，难以维护。一个解决方法是，把核心组件依赖的组件放在另一个类中，让核心组件通过访问这个类，间接访问其他组件的引用，也就是加一个间接层类。

具体来说，核心组件的接口名为 Node，实现类的名字为 NodeImpl，这个间接层的类名为 NodeContext。

```java
public class NodeContext{ private NodeId selfId; //当前节点ID private NodeGroup group; //成员列表 //private Log log; //日志 private Connector connector; //RPC组件
```

```java
private Scheduler scheduler; //定时器组件  
private EventBus eventBus;  
private TaskExecutor taskExecutor; //主线程执行器  
private NodeStore store; //部分角色状态数据存储  
//获取自己的节点ID  
public NodeId selfId() { return selfId; } //设置自己的节点ID  
public void setSelfId(NodeId selfId) { this.selfId = selfId; } //othergetter and setter 
```

NodeContext的所有字段中，group、eventBus和schedule在前面已经介绍过，这里不再赘述。剩下的字段中，selfId表示自己的NodeId；connector是RPC组件的接口；taskExecutor是处理逻辑的执行接口，也就是主线程执行器；注释中的Log是日志组件的接口，本章假设日志比较都返回OK，所以暂时不需要日志。部分角色的状态数据存储store接下来会详细讲解。

NodeContext中的getter方法命名和一般的getXXX不同，省略了get。这里单纯是为了让方法名更短，不这么做也没有问题。

# 4.4.1 RPC接口

把RPC调用做成接口，一方面是为了方便切换实现，比如在基于同步IO的实现、基于NIO的实现之间切换，另一方面是为了测试。在本节的测试部分，会看到测试用的RPC实现。

以下是RPC接口的原型代码，具体RPC实现将在第6章讲解。

```txt
public interface Connector{ //初始化 void initialize(); //发送RequestVote消息给多个节点 void sendRequestVote(RequestVoteRpcrpc, Collection<NodeEndpoint>destinationEndpoints); //回复RequestVote结果给单个节点 void replyRequestVote(RequestVoteResult result, NodeEndpointdestinationEndpoint); //发送AppendEntries消息给单个节点 void sendAppendEntries(AppendEntriesRpcrpc, NodeEndpointdestinationEndpoint); 
```

```txt
//回AppendEntries结果给单个节点  
void replyAppendEntries(AppendEntriesResult result, NodeEndpoint destinationEndpoint); //关闭连接器  
void close();
```

initialize 和 close 方法在系统启动和关闭时被调用。以 send 开头的方法用于发送消息给其他节点。参数包含要发送的消息，比如 RequestVoteRpc 或者 AppendEntriesRpc，以及目标节点。RequestVote 消息一般是群发，所以参数是一个集合。AppendEntries 必须根据每个节点的复制进度单独设置 AppendEntries 消息，所以参数是单个节点地址。以 reply 开头的方法是响应时调用的方法，参数是响应的内容和目标节点地址。

# 4.4.2 任务执行接口TaskExecutor

TaskExecutor 是一个抽象化的任务执行器。可以在实际运行中使用异步单线程实现，但是在测试时直接执行。TaskExecutor 通过把核心组件的处理模式分离出来，方便之后快速修改，比如改成多线程。TaskExecutor 接口代码如下。

public interface TaskExecutor { // 提交任务 Future $<  ? >$ submit(Runnable task); // 提交任务，任务有返回值 $\langle V\rangle$ Future $<  V>$ submit(Callable $<  V>$ task); // 关闭任务执行器 void shutdown() throws InterruptedException;

从方法上来看，TaskExecutor是JDK的ExecutorService的一个简化版，使用TaskExecutor时的代码大致如下。

```typescript
context.taskExecutor().submit() ->{ //处理   
}）;
```

以下是异步单线程的实现。

```java
public class SingleThreadTaskExecutor implements TaskExecutor { private final ExecutorService executorService; //构造函数，默认 public SingleThreadTaskExecutor() { this(Executors.defaultThreadFactory()); } 
```

```java
//构造函数，指定名称
public SingleThreadTaskExecutor(String name) {
    this(r -> new Thread(r, name));
}
//构造函数，指定ThreadFactory
private SingleThreadTaskExecutor( ThreadFactory threadFactory ) {
    executorService = Executor.newSingleThreadExecutor (threadFactory);
}
//提交任务并执行
public Future < ? > submit ( Runnable task ) {
    return executorService.submit (task);
}
//提交任务并执行，有返回值
public < V > Future < V > submit(Callable < V > task) {
    return executorService.submit (task);
}
//关闭任务执行器
public void shutdown() throws InterruptedException {
    executorService.shutdown();
    executorService awaitTermination(1, TimeUnit.SECONDS);
} 
```

代码使用Executors的newSingleThreadExecutor生成一个单线程处理实例。

SingleThreadTaskExecutor有两个公开的构造函数，一个默认无参数，一个允许设置线程名，设置线程名主要是为了调试。两个submit方法委托executorService执行，shutdown方法在系统关闭时被调用。

作为对比，直接执行版本的代码如下。

public class DirectTaskExecutor implements TaskExecutor { public Future $<  >$ submit(Runnable task) { FutureTask $<  >$ futureTask $\equiv$ new FutureTask $\ll$ (task, null); futureTask.run(); return futureTask; } public $<  V>$ Future $<  V>$ submit(Callable $<  V>$ task) { FutureTask $<  V>$ futureTask $\equiv$ new FutureTask $<  V>$ (task); futureTask.run(); return futureTask; } public void shutdown() throws InterruptedException { }

直接执行版本的submit方法中创建了一个FutureTask，并在当前线程中执行和返回。

至此，所有核心组件的准备工作基本完成，可以正式开始核心组件的编码了。

# 4.4.3 部分角色状态持久化

3.4.1 小节提到的 currentTerm 和 votedFor（不包括角色）这两个数据在系统重启后会恢复到之前的状态。

不恢复 currentTerm 看起来没有太大影响，不管是收到 Leader 节点的心跳信息，还是发起选举时得到最新的选举 term，系统都会获得最新的值。但是 votedFor 不同，下面以分割选举（2.5.2 小节的图 2-6）为例进行讲解。

假如节点B在给节点A投票后重启，收到来自候选节点D的消息后再次投票，就违反了一票制，所以节点B再次重启后恢复之前的已投票的节点（votedFor），同时对应的currentTerm也必须恢复。

由于这两个数据独立于日志，所以本书单独设计了一个接口。

```txt
public interface NodeStore{ //获取currentTerm int getTerm(); //设置currentTerm void setTerm(int term); //获取votedFor NodeId getVotedFor(); //设置votedFor void setVotedFor(NodeId votedFor); //关闭文件 void close(); 
```

此接口有两个实现，一个是基于内存的实现，主要用于测试，代码如下。

public class MemoryNodeStore implements NodeStore{ private int term; private NodeId votedFor; //构造函数 public MemoryNodeStore() { this(0，null); //构造函数 public MemoryNodeStore(int term, NodeId votedFor) { this.term $=$ term; this.votedFor $=$ votedFor;   
public int getTerm() {

return term;   
public void setTerm(int term) { this.term $=$ term;   
public NodeId getVotedFor() { return votedFor;   
public void setVotedFor(NodeId votedFor) { this.votedFor $=$ votedFor;   
public void close(){

另一个基于文件的实现稍微复杂一些。本书使用二进制格式存放两个数据。文件格式如下，votedFor的实际值是节点ID的字符串形式。

（1）4字节，currentTerm。  
（2）4字节，votedFor（节点ID）长度。   
（3）变长，votedFor内容。

FileNodeStore的初始化代码如下。

public class FileNodeStore implements NodeStore{ //文件名 public static final String FILE_NAME $\equiv$ "node.bin"; private static final long OFFSET_TERM $= 0$ private static final long OFFSET_VOTED_FOR $= 4$ private final SeekableFile seekableFile; private int term $= 0$ private NodeId votedFor $= \mathrm{null}$ //从文件读取 public FileNodeStore(File file){ try{ //如果文件不存在，创建文件 if(!file_exists()){ Files_touch(file); } seekableFile $\equiv$ new RandomAccessFileAdapter(file); initializeOrLoad(); } catch(IOException e){ thrownew NodeStoreException(e);

}  
//从模拟文件读取，用于测试  
publicFileNodeStore(SeekableFile seekableFile){this.seekableFile $=$ seekableFile;try{initializeOrLoad();}catch（IOException e）{throw new NodeStoreException(e);1  
}  
//初始化或者加载  
privatevoidinitializeOrLoad()throwsIOException{if（seekableFile.size() $\equiv = 0$ ）{//初始化//term，4）+（votedForlength，4）=8seekableFile.truncate(8L);seekableFile.seek(0);seekableFile.writeInt(0)；//termseekableFile.writeInt(0)；//votedFor length}else{//加载//读取termterm $=$ seekableFile.readInt();//读取votedForintlength $=$ seekableFile.readInt();if（length>0）{//byte[]bytes $=$ new byte[length];seekableFile.read(bytes);votedFor $=$ new NodeId(new String(bytes));11

FileNodeStore在初始化时允许文件不存在。如果文件不存在，FileNodeStore会创建一个新的文件，否则FileNodeStore会按照之前提到的格式读取数据。

SeekableFile 是一个 API 和 RandomAccessFile 很像的接口，本章只需要把这个接口理解成 RandomAccessFile 即可，后面会详细讲解这个模拟文件接口。

读写currentTerm与votedFor的代码如下。

```java
public int getTerm(){ return term; 
```

```java
public void setTerm(int term) { try { // 定位到term seekableFile.seek(OFFSET_TERM); seekableFile.writeInt(term); } catch (IOException e) { throw new NodeStoreException(e); } this.term = term; } public NodeId getVotedFor() { return votedFor; } public void setVotedFor(NodeId votedFor) { try { seekableFile.seek(OFFSET_VOTED_FOR); // votedFor为空 if (votedFor == null) { seekableFile.writeInt(0); seekableFile.truncate(8L); } else { byte[] bytes = votedFor.getValue().getBytes(); seekableFile.writeInt(bytes.length); seekableFile.write(bytes); } } catch (IOException e) { throw new NodeStoreException(e); } this.votedFor = votedFor; } 
```

最后是FileNodeStore的关闭方法。

```java
public void close() { try { seekableFile.close(); } catch (IOException e) { throw new NodeStoreException(e); } } 
```

# 4.5 一致性（核心）组件

一致性（核心）组件主要包括 Node 接口和 NodeImpl 实现，后者是实现 Raft 核心算法最重要的部分。虽然前面已经做了很多拆分工作，但是 Nodelpl 的代码量还是所有类中最多的。接下来将逐步讲解 NodeImpl 中选举部分的具体实现。

# 4.5.1 Node接口

Node 接口作为暴露给上层服务的接口，现阶段能提供的方法不多，主要是随服务生命周期一起调用的 start 和 stop 方法，之后随着内容的展开，方法会有所增加。

```java
public interface Node {
    // 启动
    void start();
    // 关闭
    void stop() throws InterruptedException;
} 
```

Node 接口代码中的 start 方法在服务启动时调用，stop 方法在服务关闭时调用。

# 4.5.2 Nodelmpl的基本代码

实现 NodeImpl 的核心字段和构造函数如下。

```java
public class NodeImpl implements Node {
    private static final Logger logger = LoggerFactory;
    getLogger(NodeImpl.class); // 日志
    private final NodeContext context; // 核心组件上下文
    private boolean started; // 是否已启动
    private AbstractNodeRole role; // 当前的角色及信息
    // 构造函数
    NodeImpl(NodeContext context) {
        this.context = context;
    }
}
```

logger用来调试日志；context是之前提到的间接访问其他组件的字段；Role表示当前节点的角色；started在这里主要是为了防止重复调用启动方法，以及要求系统只能在启动之后关闭。

NodeImpl的字段只使用final表示不可变，没有使用volatile等修饰符处理多线程访问。因为之前线程模型中提到，处理组件是异步单线程，内部的字段经过线程封闭，保证不会出现多线程安全问题。

# 4.5.3 系统启动与关闭

Node 接口的 start 方法对应系统的启动。根据 Raft 算法，启动时系统的角色为 Follower，term 为 0（在有日志的前提下，需要从最后一条日志条目重新计算最后的 term），启动方法代码如下。

```java
public synchronized void start() { // 如果已经启动，则直接跳过 if (started) { return; } // 注册自己到 EventBus context.eventBus().register(this); // 初始化连接器 context.connect().initialize(); // 启动时为 Follower 角色 NodeStore store = context.store(); changeToRole(new FollowerNodeRole( store.getTerm(), store.getVotedFor(), null, scheduleElectionTimeout())); started = true;
```

启动时，系统从 NodeStore 中读取最后的 currentTerm 和 votedFor，FollowerNodeRole 构造参数中的 null 对应 leaderId 字段。

start方法被整个设定为一个同步方法，防止同时调用，在已经启动的状态下不做任何事情。系统初始化时，在EventBus中注册自己感兴趣的消息，以及初始化RPC组件。切换角色为Follower，并且设置选举超时。

scheduleElectionTimeout代码如下。

```txt
private ElectionTimeout scheduleElectionTimeout() { return context.schedule().scheduleElectionTimeout(this::electionTimeout); } 
```

this::electionTimeout 是一个 lambda 表达式，表示调用当前实例的 electionTimeout 方法。  
electionTimeout 是选举超时的入口方法，下一小节会讲解 electionTimeout 方法。

启动代码中的changeToRole是一个统一的角色变更方法，具体代码如下。

private void changeToRole(AbstractNodeRole newNode) { logger.debug("node{}，role state changed->【】"，context.selfId()， newNode); NodeStore store $=$ context.store(); store.setTerm(newRole术语()); if(newRole.getName() $\equiv =$ RoleName.FOLLOWER){

```javascript
store.setVotedFor(((FollowerNodeRole)newRole).getVotedFor());
}
role = newRole; 
```

调用changeToRole方法主要是为了统一角色变化的代码，以及在角色变化时同步到NodeStore中。stop方法的实现相对简单，首先检查系统是否启动，然后逐个关闭相关组件并设置started为false。具体实现如下。

@override   
public synchronized void stop() throws InterruptedException{ //不允许没有启动时关闭 if(!started){ throw newIllegalStateException("node not started"); } //关闭定时器 context.schedule().stop(); //关闭连接器 context.connect().close(); //关闭任务执行器 context.taskExecutor().shutdown(); started $=$ false;

# 4.5.4 选举超时

按照之前的分析，设置选举超时需要做的事情是变更节点角色以及发送RequestVote消息给其他节点，如图4-1。

![](images/360d0639a368ab5023fb713ea119626a7a71a4fbdc1e6802160979f04c2d3a52.jpg)  
图4-1 选举超时（步骤1-2）

electionTimeout 被调用时，默认是在定时器的线程中，为了在主线程中执行，需要做一次任务转换。选举超时入口方法 electionTimeout 实现如下。

void electionTimeout(){ context.taskExecutor().submit(this::doProcessElectionTimeout); } private void doProcessElectionTimeout(){ //Leader角色下不可能有选举超时 if (role.getName() == RoleName.LEADER){ logger.warn("node{}，current role is leader,ignore election timeout",context.selfId()); return; } //对于follower节点来说是发起选举 //对于candidate节点来说是再次发起选举 //选举term加1 int newTerm $=$ role.getTerm(）+1; role.cancelTimeoutOrTask(); logger.info("startelection"); //变成Candidate角色 changeToRole(new CandidateNodeRole(newTerm,scheduleElectionTimeout()); //发送RequestVote消息 RequestVoteRpcrpc $=$ new RequestVoteRpc(); rpc.setTerm(newTerm); rpc.setCandidateId(context.selfId()); rpc.setLastLogIndex(0); rpc.setLastLogTerm(0); contextconnector().sendRequestVote(rpc,context.group().listEndpointExceptSelf()); 1

在doProcessElectionTimeout中，先检查当前角色是不是Leader，如果是，则打印警告信息后退出。因为对于Leader来说，选举超时没有意义，理论上也不可能发生。

接下来的处理，同时对应了Follower和Candidate两个角色。方法增加term，取消当前的定时任务，转换为Candidate角色，最后发送RequestVote消息给其他节点。

集群成员组件的listEndpointExceptSelf方法返回除当前节点之外的其他节点，代码如下。

```java
class NodeGroup {
    private final NodeId selfId;
    private Map<NodeId, GroupMember> memberMap; 
```

```txt
Set<NodeEndpoint> listEndpointExceptSelf() { Set<NodeEndpoint> endpoints = newHashSet<>(); for (GroupMember member : memberMap.values()) { //判断是不是当前节点 if (!member.getId().equals(selfId)) { endpoints.add(member.getEndpoint()); } return endpoints; } 
```

# 4.5.5 收到RequestVote消息

如图4-2所示，节点收到RequestVote消息后，需要选择投票还是不投。

![](images/aefdbea23b4e2fdc5f7fb34beff42f119d391e6130e0a59e22274d2a63c39c2e.jpg)  
图4-2 投票（步骤3）

当节点收到RequestVote消息（RequestVoteRpc）后，要先比较消息中的term和自己本地的term，之后根据日志决定是否投票。本章为了减少关注点，一律设定为投票。下面代码中的voteForCandidate值始终为true，但实际代码中不能这么做。加入日志后的处理将在下一章给出。

```java
@Subscribe
public void onReceiveRequestVoteRpc.RequestVoteRpcMessage rpcMessage) {
    context.taskExecutor().submit(
        () -> contextconnector().replyRequestVote(
            doProcessRequestVoteRpc(rpcMessage),
            // 发送消息的节点
            context.findMember(rpcMessage.SourceNodeId()).getEndpoint()
        );
    );
} 
```

```javascript
if (rpc.getTerm() < role.getTerm()) { logger.debug("term from rpc < current term, don't vote ({}) < ({})", rpc.getTerm(), role.getTerm()); return new RequestVoteResult(role.getTerm(), false); } //此处无条件投票 boolean voteForCandidate = true; //如果对象的term比自己大，则切换为Follower角色 if (rpc.getTerm() > role.getTerm()) { becomeFollower(rpc.getTerm(), (voteForCandidate ? rpc.getCandidateId()) : null), null, true); return new RequestVoteResult(rpc.getTerm(), voteForCandidate); } //本地的term与消息的term一致 switch (role.getName()) { case FOLLOWER: FollowerNodeRole followerer = (FollowerNodeRole) role; NodeId votedFor = follower.getVotedFor(); //以下两种情况下投票 //case 1.自己尚未投过票，并且对方的日志比自己新 //case 2.自己已经给对方投过票 //投票后需要切换为Follower角色 if ((votedFor == null && voteForCandidate) || // case 1 Objects.equals(votedFor, rpc.getCandidateId())) { // case 2 becomeFollower(node.getTerm(), rpc.getCandidateId(), null, true); return new RequestVoteResult(rpc.getTerm(), true); } return new RequestVoteResult(root.getTerm(), false); case CANDIDATE: //已经给自己投过票，所以不会给其他节点投票 case LEADER: return new RequestVoteResult(root.getTerm(), false); default: throw new想不到("unexpected node role [" + role.getName() +"]"); } 
```

人口方法 onReceiveRequestVoteRpc 的参数是一个叫作 RequestVoteRpcMessage 的类，而不是

RequestVoteResult。RequestVoteRpcMessage 包含 RequestVoteRpc 和节点的 NodeId（sourceNodeId），这样才能回复消息给发送消息的节点。

方法上标注了 Subscribe，表示订阅 EventBus 中类型为 RequestVoteRpcMessage 的消息。和选举超时一样，RPC 组件的调用（在 RPC 的 IO 线程中执行）需要转移到 taskExecutor 对应的主线程中执行。

具体是在doProcessRequestVoteRpc方法中处理RequestVoteRpc。核心组件先判断term，比自己小的不投票并返回自己的term；比自己大的则马上切换为Follower角色，并根据日志投票（上面的代码是一律投票）。一般来说，选举都会落到这部分逻辑中。

对于 term 和自己一致的请求，有以下两种情况。

（1）自己是Candidate角色时，Candidate只为自己投票。  
（2）自己是Leader角色时，投票理论上没有意义。

所以两种情况都不会选择投票，剩下的只有Follower角色。假如Follower角色自己没有投过票，并且对方日志比自己新，则选择投票。或者已经投过票而且投的是同一个节点的话，则回复投票，其他情况下一律不投票。

term一致的一种情况是，出现了两个以上的Candidate节点，部分已投票的Follower节点有可能收到其他Candidate节点的消息，此时由于已经给某个节点投过票，因此不会再投票（对应代码中的case2）。另一种情况是，多个节点以不同的term启动，选举超时后，Candidate角色碰巧发送消息到比自己term大的Follower角色的节点，此时仍旧会比较日志决定是否投票（对应case1）。

becomeFollower 是一个特殊的角色变化方法，有一个是否设置选举超时的参数。

```java
private void becomeFollower(int term, NodeId votedFor, NodeId leaderId, boolean scheduleElectionTimeout) {
    role.cancelTimeoutOrTask(); // 取消超时或者定时器
    if (leaderId != null && !leaderId.equals(node.getLeaderId(context.selfId())))) {
        logger.info("current leader is {}, term {}, leaderId, term);
    }
    // 重新创建选举超时定时器或者空定时器
    ElectionTimeout electionTimeout = scheduleElectionTimeout ? scheduleElectionTimeout() : ElectionTimeout.NONE;
    changeToRole(new FollowerNodeRole(term, votedFor, leaderId, electionTimeout));
} 
```

ElectionTimeout.NONE在这里表示不设置选举超时，代码在测试部分给出。

在之后的服务器成员变更中移除服务器节点时，会涉及不需要设置选举超时的场景，现在只需要知道在常规情况下都需要设置选举超时定时器。

# 4.5.6 收到RequestVote响应

如图4-3所示，收到RequestVote响应后，节点根据票数决定是变成Leader，还是保持Candidate角色继续等待其他节点的投票。

![](images/d189900868bf1d547aaeef8744afd70809e5bd60a64e2b8a351f1de06dddc210.jpg)  
图4-3 票数处理（步骤4）

节点收到RequestVote的响应（RequestVoteResult）时的处理如下。

```java
@Subscribe
public void onReceiveRequestVoteResult(ResultVoteResult result) {
    context.taskExecutor().submit(
        () -> doProcessRequestVoteResult(result);
    );
}
private void doProcessRequestVoteResult(ResultVoteResult result) {
    // 如果对象的 term 比自己大，则退化为 Follower 角色
    if (result.getTerm() > role.getTerm())
        becomeFollower(result.getTerm(), null, null, true);
        return;
    }
    // 如果自己不是 Candidate 角，则忽略
    if (role.getName() != RoleName.CANDIDATE) {
        logger.debug("receive request vote result and current role is not candidate, ignore");
        return;
    }
    // 如果对方的 term 比自己小或者对象没有给自己投票，则忽略
    if (result.getTerm() < role.getTerm() || !result.isVoteGranted())
        return;
}
```

```javascript
countOfMajor); //取消选举超时定时器  
role.cancelTimeoutOrTask();  
if(currentVotesCount > countOfMajor / 2) { //票数过半//成为Leader角色logger.info("become leader，term{}",role.getTerm());//resetReplicatingStates();changeToRole(new LeaderNodeRole(role.getTerm()，scheduleLogReplicationTask());//context.log().appendEntry_role.getTerm());//no-op log}else{ //修改收到的投票数，并重新创建选举超时定时器changeToRole(new CandidateNodeRole(role.getTerm()，currentVotesCount，scheduleElectionTimeout());；1 
```

和收到RequestVote请求时的处理类似，收到来自RPC线程的请求后，马上转移到TaskExecutor对应的主线程中执行，Candidate角色的执行序列如图4-4。

![](images/dff3fff2b3df9f602bba952c42ffb981cd413fea574dc126cfc7c74bf5abe07a.jpg)  
图4-4 Candidate角色执行序列

在doProcessRequestVoteResult中，先比较响应的term和自己的term，如果自己的term小，则step down，即退化到Follower角色，此时调用之前的becomeFollower方法。

接下来判断自己的角色，如果不是Candidate，则打印警告信息后结束。否则检查结果中的term和voteGranted，如果term比自己小或者没有收到投票，则什么都不需要做。

收到非投票消息是否也需要重置选举超时是一个比较微妙的问题，考虑到系统需要快速选出Leader，如果其他节点不投票，会导致选举延长，所以此处通过什么都不做来保证一个选举超时内能收到足够的票数，否则要重开选举term。

响应中的 term 与本地一致并且收到投票后进入下一阶段。根据收集到的票数判断是否可以成为 Leader 角色。countOfMajor 是总节点数。Raft 算法中票数过半（包括自己的 1 票）即可成为 Leader。

成为Leader角色后打印日志，重置日志复制进度，设置自己为Leader角色，启动日志复制定时器，添加一条NO-OP日志。

重置日志复制进度在 Raft 算法中只是简单地把 nextIndex 和 matchIndex 重置为 0。添加 NO-OP 日志需要日志部分的实现。这两部分和选举关系不大，所以此处注释掉，不用关注。

scheduleLogReplicationTask 和 scheduleElectionTimeout 类似，通过 scheduler 开启一个定时器，具体代码如下。

```txt
private LogReplicationTask scheduleLogReplicationTask() { return context.schedule().scheduleLogReplicationTask(this::replicateLog); } 
```

replicateLog对应日志复制的入口方法。

doProcessRequestVoteResult中的else分支是票数没有达到过半时的处理，简单来说，就是增加票数，重置选举超时。重置选举通过之前已经执行过的cancelTimeoutOrTask加上scheduleElectionTimeout的调用实现。

# 4.5.7 成为Leader节点后的心跳消息

如图4-5所示，节点成为Leader节点后必须立刻发送心跳信息。

![](images/52283d17e4fc97382a47c452074030568b4206cf29eb729c073627466b026627.jpg)  
图4-5 心跳消息（步骤6）

Raft算法要求，成为Leader角色后，必须立刻发送心跳消息给其他Follower节点，重置这些Follower节点的选举超时，使集群的主从关系稳定下来，发送心跳信息的代码如下。

```txt
void replicateLog(){ context.taskExecutor().submit(this::doReplicateLog); } 
```

private void doReplicateLog() { logger.debug("replicate log"); //给日志复制对象节点发送AppendEntries消息 for(GroupMember member：context.group().listReplicationTarget()）{ doReplicateLog(member); }   
private void doReplicateLog(GroupMember member) { AppendEntriesRpcrpc $=$ new AppendEntriesRpc(); rpc.setTerm(role术语()); rpc.setLeaderId(context.selfId()); rpc.setPrevLogIndex(0); rpc.setPrevLogTerm(0); rpc.setLeaderCommit(0); contextconnector().sendAppendEntries(rpc，member.getEndpoint());   
1

replicateLog 同样把具体处理（doReplicateLog）放在 TaskExecutor 的主线程中执行。doReplicateLog 方法只是简单地列出复制对象，即不包括自己的所有其他节点，给它们发送 AppendEntries 消息。消息内容中的日志相关部分都设置为 0，entries 在这里没有列出，实际消息中为空列表。

# 4.5.8 收到来自Leader节点的心跳消息

非Leader节点收到来自Leader节点的心跳信息之后需要重置选举超时，并记录当前Leader节点的id，具体代码如下。

```java
@Subscribe
public void onReceiveAppendEntriesRpc(AppendEntriesRpcMessage rpcMessage) \{
(context.taskExecutor().submit(   ) -> contextconnector().replyAppendEntries( doProcessAppendEntriesRpc(rpcMessage), // 发送消息的节点 context.findMember(rpcMessage.SourceNodeId(   )); 
```

AppendEntriesRpcMessage rpcMessage) { AppendEntriesRpcrpc $=$ rpcMessage.get(); //如果对方的term比自己小，则回复自己的term if (rpc.getTerm() $<$ role.getTerm()){//case1 return new AppendEntriesResult(role.getTerm(),false); } //如果对象的term比自己大，则退化为Follower角色 if (rpc.getTerm() $>$ role.getTerm()){//case2 becomeFollower(rpc.getTerm(),null，rpc.getLeaderId(),true); //并追加日志 return new AppendEntriesResult(rpc.getTerm)，appendEntries(rpc)); } assertrpc.getTerm() $= =$ role.getTerm(); switch (role.getName()){ case FOLLOWER://case3 //设置leaderId并重置选举定时器 becomeFollower(rpc.getTerm)， ((FollowerNodeRole)role).getVotedFor(),rpc. getLeaderId(),true); //追加日志 return new AppendEntriesResult(rpc.getTerm)，appendEntries(rpc)); case CANDIDATE:/case4 //如果有两个Candidate角色，并且另外一个Candidate先成了Leader //则当前节点退化为Follower角色并重置选举定时器 becomeFollower(rpc.getTerm()，null，rpc.getLeaderId()，true); //追加日志 return new AppendEntriesResult(rpc.getTerm)，appendEntries(rpc)); case LEADER: //case5 //Leader角色收到AppendEntries消息，打印警告日志 logger.warn("receive append entriesrpc from another leader },ignore", rpc.getLeaderId()); return new AppendEntriesResult(rpc.getTerm()，false); default: throw new IllegalStateException("unexpected node role[" $^+$ role.getName(） $^+$ "])； 1 privateboolean appendEntries(AppendEntriesRpcrpc){ return true;

和 RequestVoteRpcMessage 消息一样，方法参数 AppendEntriesRpcMessage 是一个包含 AppendEntriesRpc 和发送节点信息的类。

具体执行方法中，对于对方 term 和自己 term 不一致的情况，如果小于自己（case 1），则返回自己的 term，并把 AppendEntriesResult 中的 success 设置为 false，表示追加失败。

如果对方的 term 大于自己（case 2），则变成 Follower，不管自己原来是什么角色，并记录 leaderId，重置选举超时，尝试追加日志。

appendEntries包含具体的追加日志的逻辑，本章一律设置结果为true。

对于 term 一致的情况，角色为 Follower 则保留 votedFor，其余和之前的 case 2 一样；角色为 Candidate 则退化为 Follower，设置 leaderId 并尝试追加日志。理论上此时角色不应该是 Leader，如果出现 Leader 则需要人工介入，所以这里打印了警告日志。

# 4.5.9 Leader节点收到来自其他节点的响应

作为整个流程的最后一步，Leader节点收到其他节点的响应之后，执行相应的处理，等待日志复制定时器下一次触发，具体代码如下。

```java
@Subscribe
public void onReceiveAppendEntriesResult(AppendEntriesResultMessage resultMessage) {
    context.taskExecutor().submit() -> doProcessAppendEntriesResult(resultMessage));
}
private void doProcessAppendEntriesResult(AppendEntriesResultMessage resultMessage) {
    AppendEntriesResult result = resultMessage.get();
    // 如果对方的term比自己大，则退化为Follower角色
    if (result.Term() > role.Term()) {
        becomeFollower(result.Term(), null, null, true);
        return;
    }
    // 检查自己的角色
    if (role.getName() != RoleName.LEADER) {
        logger.warn("receive append entries result from node {}
    } but current node is not leader, ignore", resultMessage.SourceNodeId());
} 
```

AppendEntriesResultMessage 是一个同时包含 AppendEntries 请求与响应的包装类。因为 AppendEntriesResult 的内容太少，所以部分数据需要从 AppendEntriesRpc 中获取。

在主体处理代码doProcessAppendEntriesResult中，根据响应中的term来决定角色是否要退化。

为 Follower，当自己不是 Leader 角色时直接打印警告信息并结束。

至此，选举部分的所有核心代码都实现完毕。可以尝试实现RPC组件，然后启动系统并调试。不过在那之前，建议通过单元测试保证主体逻辑正确。

# 4.6 测试

由于篇幅所限，本节只对正常用例进行单元测试，边界条件的测试则不给出。测试按照核心组件的实现顺序，从上至下列出。测试使用Junit $4+$ ，基于@Test标注的方式。

# 4.6.1 测试准备

测试准备主要有两部分。一部分是组件的Mock化，比如定时器组件，测试时不希望完全异步调用，所以就需要修改定时器组件或者实现一个测试专用定时器组件。

另一部分是测试过程中会发现，一些代码在设计上存在不方便测试的地方，比如 NodeImpl 中的 role，外部无法访问。所以需要为测试暴露 role，或者增加其他可以访问 role 的方法。

以下是具体准备的内容。

（1）测试专用定时器组件NullScheduler。  
（2）测试专用RPC组件MockConnector。  
（3）暴露role的状态数据。  
（4）快速构造 NodeImpl 的 NodeBuilder。

首先是测试专用定时器组件。

public class NullScheduler implements Scheduler { //日志 private static final Logger logger $\equiv$ LoggerFactory.getLogger (NullScheduler.class); //创建日志复制定时器 public LogReplicationTask scheduleLogReplicationTask(@Nonnull Runnable task){ logger.debug("schedule log replication task"); return LogReplicationTask.NONE; } //创建选举超时定时器 public ElectionTimeout scheduleElectionTimeout(@Nonnull Runnable task) { logger.debug("schedule election timeout"); return ElectionTimeout.NONE; }

```txt
// 关闭定时器
public void stop() throws InterruptedException {
} 
```

NullScheduler的实现中除了打印日志，还返回了选举定时器的一个NONE实例。之前讲解定时器组件时提到过，ElectionTimeout和LogReplicationTask都是ScheduledFuture的封装，而ScheduledFuture是一个接口，所以可以创建一个NullScheduledFuture来构造ElectionTimeout和LogReplicationTask的NONE实例。

```java
public class NullScheduledFuture implements ScheduledFuture<Object> { public long getDelay(@Nonnull TimeUnit unit) { return 0; } public int compareTo(@Nonnull Delayed o) { return 0; } public boolean cancel(boolean mayInterruptIfRunning) { return false; } public boolean isCancelled() { return false; } public boolean isDone() { return false; } public Object get() throws InterruptedException, ExecutionException { return null; } public Object get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { return null; } 
```

在 ElectionTimeout 和 LogReplicationTask 中分别增加以下代码。

```java
public static final ElectionTimeout NONE = new ElectionTimeout(new NullScheduledFuture());  
public static final LogReplicationTask NONE = new LogReplicationTask(new NullScheduledFuture()); 
```

然后就可以通过 ElectionTimeout.NONE 或者 LogReplicationTask.NONE，返回一个什么都不做

的选举超时或者日志复制定时任务。

简单来说，测试专用RPC组件就是把RPC消息暂存到某个地方，但是实际不发送，在测试代码之外读取这些暂时的RPC消息。也可以用mock框架（比如JMock）预测消息内容。本书为了便于读者理解，专门设计了一个MockConnector，并在其内部存放了一个消息的链表，发送后的消息可以通过MockConnector暴露出来的方法读取到。

以下是MockConnector内部的Message类，用来存放发送的消息。

```java
public class MockConnector implements Connector{ public static class Message { private Object rpc; // RPC消息 private NodeId destinationNodeId; // 目标节点 private Object result; // 结果 // 获取RPC消息 public Object getRpc() { return rpc; } //获取目标节点 public NodeId getDestinationNodeId(){ return destinationNodeId; } //获取结果 public Object getResult(){ return result; } @Override public String toString(){ return "Message{" + "destinationNodeId=" + destinationNodeId + ",rpc=" + rpc + ",result=" + result + '}'; 1 
```

MockConnector 对于 Connector 接口的实现如下。

public class MockConnector implements Connector{ //存放消息的链表 private LinkedList<Message> messages $=$ newLinkedList<>(); //初始化 public void initialize() { 1

# //发送消息

```java
public void sendRequestVote(Collection<NodeEndpoint> destinationEndpoints) { // 对于多目标节点，这里没有完全处理 Message m = new Message(); m.rpc = rpc; messages.add(m); } public void replyVote(RichSelectResult result, NodeEndpoint destinationEndpoint) { Message m = new Message(); m(result = result; mdestinationNodeId = destinationEndpoint.getId(); messages.add(m); } public void sendAppendEntries(AppendEntriesRpc rpc, NodeEndpoint destinationEndpoint) { Message m = new Message(); m.rpc = rpc; mdestinationNodeId = destinationEndpoint.getId(); messages.add(m); } public void replyAppendEntries(AppendEntriesResult result, NodeEndpoint destinationEndpoint) { Message m = new Message(); m(result = result; mdestinationNodeId = destinationEndpoint.getId(); messages.add(m); } // 关闭 public void close() { } 
```

接下来是MockConnector中增加的测试方法。

```java
public class MockConnector implements Connector {
    private LinkedList<Message> messages = new LinkedList<>(); // 获取最后一条消息
    public Message.getLastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }
} 
```

```java
// 获取最后一条消息或者空消息
private Message.getLastMessageOrDefault() {
    return messages.isEmpty() ? new Message() : messages.getLast();
}
// 获取最后一条RPC消息
public Object getRPC() {
    return lastMessageOrDefault().rpc;
}
// 获取最后一条Result消息
public Object getResult() {
    return lastMessageOrDefault().result;
}
// 获取最后一条消息的目标节点
public NodeId getDestinationNodeId() {
    return lastMessageOrDefault().destinationNodeId;
}
// 获取消息数量
public int getMessageCount() {
    return messages.size();
}
// 获取所有消息
public List<Message> getMessages() {
    return new ArrayList<> (messages);
}
// 清除消息
public void clearMessage() {
    messages.clear();
} 
```

有了MockConnector之后，就可以方便地检查发送的RPC消息或者结果了。

如果想要暴露 NodeImpl 内部数据，可以增加一个 package 级别的可见性方法（即不写 public/private/protected 修饰符），保证测试代码在同一个包。以下代码以包可见的级别公开了 context 和 role。

```java
class NodeImpl implements Node {
    private final NodeContext context;
    private final AbstractNodeRole role;
    // 获取核心组件上下文
    NodeContext getContext() {
        return this.context;
    }
    // 获取当前角色 
```

```txt
AbstractNodeRole getRole(){ return this-role; 1 
```

准备工作的最后一部分是快速构建 NodeImpl 实例的 NodeBuilder。由于 NodeImpl 的关联组件太多，每次都从头开始创建比较花时间，因此这里编写了一个 NodeBuilder 类。NodeBuilder 本身也用在正常的系统启动中，所以严格来说不是测试专用的代码，以下是具体的代码。

```java
public class NodeBuilder { private final NodeGroup group; // 集群成员 private final NodeId selfId; // 节点ID private final EventBus eventBus; private Scheduler scheduler = null; // 定时器 private Connector connector = null; // RPC连接器 private TaskExecutor taskExecutor = null; // 主线程执行器 // 单节点构造函数 public NodeBuilder(NodeEndpoint endpoint) { this(Collection抢单List(endpoint), endpoint.getId()); } // 多节点构造函数 public NodeBuilder(Collection<NodeEndpoint> endpoints, NodeId selfId) { this.group = new NodeGroup(endpoints, selfId); this.selfId = selfId; this.eventBus = new EventBus(self.Id.getValue()); } // 设置通信组件 NodeBuilder setConnector(Connector connector) { thisconnector = connector; return this; } // 设置定时器 NodeBuilder setScheduler(Scheduler scheduler) { thisscheduled = scheduler; return this; } // 设置任务执行器 NodeBuilder setTaskExecutor(TaskExecutor taskExecutor) { this.taskExecutor = taskExecutor; return this; } // 构建Node实例 public Node build() { 
```

return new NodeImpl(buildContext());   
1 //构建上下文 private NodeContext buildContext() { NodeContext context $=$ new NodeContext(); context.setGroup(group); context.setSelfId(selfId); context.setEventBus(eventBus); context.setScheduler(scheduler $! =$ null？scheduler：new DefaultScheduler (config)); context.setConnector(connector); context.setTaskExecutor( taskExecutor！ $= \mathrm{null?}$ taskExecutor：new SingleThreadTa skExecutor("node"); ）; return context;

方便起见，NodeBuilder 按照链式 Builder 模式编写。

至此，测试代码准备结束。

# 4.6.2 系统启动

测试要求：系统启动后角色为 Follower，term 为 0，测试代码如下。

```java
public class NodeImplTest {
    private NodeBuilder newNode(Node Id selfId, NodeEndpoint... endpoints) {
        return new NodeBuilder(Arrays.asList(endpoints), selfId)
            .setScheduler(new EmptyScheduler())
            .setConnector(new MockConnector())
            .setTaskExecutor(new DirectTaskExecutor());
        }
        @Test
        public void testStart() {
            NodeImpl node = (NodeImpl) newNode(Node Id.of("A"), new NodeEndpoint("A", "localhost", 2333)).build();
            node.start();
            FollowersNodeRole role = (FollowerNodeRole) node.getRole();
            Assert.assertEquals(0, role.getTerm());
        }
    }
}; 
```

```javascript
Assert.assertNull(root.getVotedFor()); 
```

此处为单节点启动。start 实现和节点数理论上没有直接联系。newNodeBuilder 是用于快速构建测试用的 NodeBuilder 方法，不直接返回 Node 的原因是，之后的方法可能会有特殊要求。

第一次正式启动系统时，如果没有意外，肯定可以通过测试。如果出现任务问题，建议把相关代码重新梳理一下，找到出错的地方。

# 4.6.3 选举超时

测试要求：Follower角色选举超时后变成Candidate角色，并给其他节点发送RequestVote消息。

由于把选举超时 mock 了，因此要触发选举超时，就必须显式调用 electionTimeout。同时流程上必须先调用 start，测试代码如下。

public class NodeImplTest{ @Test public void testElectionTimeoutWhenFollower(){ NodeImpl node $=$ (NodeImpl）newNodeBuilder( Id.of("A")， new NodeEndpoint("A"，"localhost"，2333)， new NodeEndpoint("B"，"localhost"，2334)， new NodeEndpoint("C"，"localhost"，2335) .build(); node.start(); node.electionTimeout(); //选举开始后，初始term为1 CandidateNodeRole role $=$ (CandidateNodeRole）node-role(); Assert.assertEquals(1,role.getTerm()); Assert.assertEquals(1,role.getVotesCount()); MockConnector mockConnector $=$ (MockConnector）nodegetContext(). connector(); //当前节点向其他节点发送RequestVote消息 RequestVoteRpcrpc $=$ (RequestVoteRpc）mockConnector.getRpc(); Assert.assertEquals(1,rpc.getTerm()); Assert.assertEquals(NodeId.of("A")，rpc.getCandidateId()); Assert.assertEquals(0，rpc.getLastLogIndex()); Assert.assertEquals(0，rpc.getLastLogTerm()); }

此处集群不能只有单节点，严格来说单节点是选举超时的边界条件。可以设置到单节点时必须跳过选举超时，因为没有节点会给当前节点投票。

检查RPC消息的部分，发现没有断言消息的数量，原因是MockConnector在记录并发送RequestVote消息时，只会存一条消息。

# 4.6.4 收到RequestVote消息

测试要求：Follower节点收到其他节点的RequestVote消息，投票并设置自己的votedFor为消息来源节点的id。

下面的代码测试了节点C发送RequestVote消息给当前节点A时的情况。

```java
public class NodeImplTest {
    @Test
    public void testOnReceiveRequestVoteRpcFollower() {
        NodeImpl node = (NodeImpl) newNodeBuilder(
            NodeId.of("A"),
            new NodeEndpoint("A", "localhost", 2333),
            new NodeEndpoint("B", "localhost", 2334),
            new NodeEndpoint("C", "localhost", 2335))
            .build();
        }
        node.start();
        RequestVoteRpc rpc = new RequestVoteRpc();
        rpc.setTerm(1);
        rpc.setCandidateId(NodeId.of("C"));
        rpc.setLastLogIndex(0);
        rpc.setLastLogTerm(0);
        node.onReceiveRequestVoteRpc(new RequestVoteRpcMessagerpc,
        NodeId.of("C")); 
        MockConnector mockConnector = (MockConnector) node.getContext(). 
connector();
        RequestVoteResult result = (RequestVoteResult) mockConnector. 
getResult();
        Assert.assertEquals(1, result.getTerm());
        Assert.assertTrue(result.isVoteGranted());
        Assert.assertEquals(NodeId.of("C"), ((FollowerNodeRole) node. 
getRole()).getVotedFor());
    } 
```

现阶段还没有办法测试节点不投票的情况。

# 4.6.5 收到RequestVote响应

测试要求：在3节点系统的节点A、B和C中，节点A变成Candidate角色之后收到投票的RequestVote响应，然后变成Leader角色。

测试代码如下。

public class NodeImplTest{ @Test public void testOnReceiveRequestVoteResult() { NodeImpl node $=$ (NodeImpl）newNodeBuilder( Id.of("A"), new NodeEndpoint("A","localhost",2333), new NodeEndpoint("B","localhost",2334), new NodeEndpoint("C","localhost",2335) ).build(); node.start(); node.electionTimeout(); node.onReceiveRequestVoteResult(new RequestVoteResult(1, true)); LeaderNodeRole role $=$ (LeaderNodeRole) node.getRole(); Assert.assertEquals(1,role术语()); }

注意必须调用选举超时的electionTimeout，否则当前节点不会是Candidate角色。

如果要测试1票以上的场景，至少需要4节点以上的集群设置。另外，此时心跳信息尚未发送。如果要发送心跳消息，需要显式调用NodeImpl的replicateLog方法来模拟日志复制定时器。

# 4.6.6 成为Leader节点后的心跳消息

测试要求：在3节点系统的节点A、B和C中，节点A变成Leader节点后向B和C发送心跳消息。

测试代码如下。

```java
public class NodeImplTest {
    @Test
    public void testReplicateLog() {
        NodeImpl node = (NodeImpl) newNodeBuilder(
            NodeId.of("A"),
            new NodeEndpoint("A", "localhost", 2333),
            new NodeEndpoint("B", "localhost", 2334),
            new NodeEndpoint("C", "localhost", 2335)
        ).build();
    node.start(); 
```

```txt
node ElectionTimeout(); // 发送 RequestVote 消息
node.onReceiveRequestVoteResult(new RequestVoteResult(1, true)); node.replicateLog(); // 发送两条日志复制消息
MockConnector mockConnector = (MockConnector) nodegetContext().connector();
// 总共加起来 3 条消息
Assert.assertEquals(3, mockConnector.get指导意见());
// 检查目标节点
List<MockConnector.Message> messages = mockConnector.getMessages();
Set<nodeId> destinationNodeIds = messages.subList(1, 3).stream().map(MockConnector.Message::getDestinationNodeId)
.map(Collectors.toSet());
Assert.assertEquals(2, destinationNodeIds.size());
Assert.assertTrue(destinationNodeIds.contains(NodeId.of("B")));
Assert.assertTrue(destinationNodeIds.contains(NodeId.of("C")));
AppendEntriesRpc rpc = (AppendEntriesRpc) messages.get(2).getRpc());
Assert.assertEquals(1, rpc.getTerm());
} 
```

此处代码相对复杂一些。代码模拟了从系统启动到发送心跳消息的整个过程，消息总数是3（RequestVote的消息只算1条），断言部分检查了消息的目标节点以及消息中选举term的值。

# 4.6.7 收到来自Leader节点的心跳消息

测试要求：在3节点系统的节点A、B和C中，节点A启动后收到来自Leader节点B的心跳消息，设置自己的term和leaderId以及回复OK。

测试代码如下。

```java
public class NodeImplTest {
    @Test
    public void testOnReceiveAppendEntriesRpcFollower() {
        NodeImpl node = (NodeImpl) newNodeBuilder(
            NodeId.of("A"),
            new NodeEndpoint("A", "localhost", 2333),
            new NodeEndpoint("B", "localhost", 2334),
            new NodeEndpoint("C", "localhost", 2335))
        .build();
    node.start();
    AppendEntriesRpc rpc = new AppendEntriesRpc();
    rpc.setTerm(1);
    rpc.setLeaderId(NodeId.of("B")); 
```

node.onReceiveAppendEntriesRpc(new AppendEntriesRpcMessage(rpc, NodeId.of("B"))); MockConnector connector $=$ (MockConnector) nodegetContext(); connector(); AppendEntriesResult result $=$ (AppendEntriesResult) connector.   
getResult(); Assert.assertEquals(1,result.Term()); Assert.assertTrue(result.isSuccess()); FollowerNodeRole role $=$ (FollowerNodeRole) node-role(); Assert.assertEquals(1,role.Term()); Assert.assertEquals(NodeId.of("B"),role.getLeaderId()); 1

# 4.6.8 Leader节点收到其他节点的响应

测试要求：在3节点系统的节点A、B和C中，A为Leader节点，向其他节点发送消息并收到回复。

测试代码如下。

```java
class NodeImpl {
    @Test
    public void testOnReceiveAppendEntriesNormal() {
        NodeImpl node = (NodeImpl) newNodeBuilder(
            NodeId.of("A"),
            new NodeEndpoint("A", "localhost", 2333),
            new NodeEndpoint("B", "localhost", 2334),
            new NodeEndpoint("C", "localhost", 2335)
        ).build();
        node.start();
        node ElectionTimeout(); // become candidate
        node.onReceiveRequestVoteResult(new RequestVoteResult(1, true));
    // become leader
    node.replicateLog();
    node.onReceiveAppendEntriesResult(new AppendEntriesResultMessage(
            new AppendEntriesResult("", 1, true),
            NodeId.of("B"),
            new AppendEntriesRpc())
        );
    }
} 
```

至此，所有正常场景的测试都已完成。

# 4.7 本章小结

本章主要讲解了角色建模、定时器组件、消息建模、关联组件与工具、一致性（核心）组件等内容，逐步构建了选举部分的实现。构建完成后，使用单元测试保证代码的正确性。总体来说，选举是Raft比较核心的算法部分，而且涉及的周边组件很多，在完全实现前，需要花很多时间在构建周边组件上。

接下来将介绍 Raft 核心算法中另一部分重要的内容——日志部分的实现。本章中省略的很多日志相关的内容也会在下一章进行补充。

# 第5章

# 日志实现

接着上一章的选举实现，本章将讲解日志部分的实现。作为Raft核心算法中另一个重要的部分，日志相比选举要独立、偏底层，并且基本没有依赖，所以实现时的灵活度很高。第2章给出了日志复制的基本过程，但是没有提到日志实现的要求，本章将按照如下顺序讲解日志组件。

（1）日志实现的要求。  
（2）如何实现日志组件。  
（3）日志组件如何和选举组件对接。  
（4）测试。

全部完成后，Raft算法的核心部分基本定型，之后的章节将会在核心的基础上进行加强与扩展。

# 5.1 日志实现要求

和给出具体的处理逻辑的选举组件不同，首先，日志在分布式一致性算法中一直都是一个很重要的基础组件，不管是在与Raft算法作为对比对象的Paxos算法中，还是在Paxos变体算法中。当然，这些算法所要求的日志系统和一般的数据库WAL（Write-Ahead Log），即只会追加日志的日志系统不同，在运行中写入的日志可能会因为冲突而被丢弃或者说被覆盖，这是实现时必须理解和考虑的一点。

其次，日志并不关心上层服务是什么。第2章提到设计目标之一是状态机的通用性。也就是说，日志存储的内容必须是与服务无关的。可以把服务的某个请求转换成一种通用的存储方式，比如转换成二进制存放起来。

最后，因为日志涉及文件IO，存在性能优化的余地。但比起性能，要优先保证日志系统的正确性，在实现时请牢记这一点。

# 5.2 日志实现分析

在具体实现日志组件之前，需要深入分析一下Raft核心算法中的日志部分需要什么样的接口。同时，与实现紧密相关的日志条目的内容、日志存储的选型、日志条目的序列化和反序列化等也需要仔细考察。

# 5.2.1 日志条目的内容

到现在为止，提到的日志类型主要有以下两种。

（1）普通日志条目，即上层服务产生的日志，日志的负载是上层服务操作的内容。  
（2）NO-OP日志条目，即选举产生的新Leader节点增加的第一条空日志。不需要在上层服务的状态机中应用。

一个直接的区分不同类型日志的方法是，给日志条目设置日志类型（kind），这样之后读取时可以按照kind区分。如果上层服务可以自动跳过空日志，也可以把没有负载的普通日志条目作为NO-OP日志。不过之后会看到，服务器增减时的操作也需要作为特殊日志条目加入日志，所以建议还是回归简单的方法，即给日志条目增加类型字段。

上述类型和负载，加上Raft算法中的两个字段，term和index，日志条目总共需要4个字段。虽然NO-OP日志不需要负载字段，但是通用接口需要考虑合集，以下是日志条目Entry的接口。

```txt
public interface Entry { //日志条目类型 int KIND_NO_OP = 0;
```

```txt
int KIND_general = 1; //获取类型 int getKind(); //获取索引 int getIndex(); //获取term int getTerm(); //获取元信息（kind，term和index） EntryMeta getMeta(); //获取日志负载 byte[] getCommandBytes();
```

把日志条目设计为接口而不是数据类（data class 或者 POJO）的一个原因是，可以给 Entry 的 getCommandBytes 方法一定的灵活实现空间，比如将负载的序列化推迟到第一次被调用时才执行。

以 KIND开头的常量为日志条目类型。getMeta方法返回的是除了commandBytes（即负载）以外的日志条目元信息。这个方法在部分场景中很有用，比如在选举开始发送的RequestVote消息中，只需要用最后一条日志的term和index来组装RequestVote消息，此时可以调用日志组件获取最后一条日志的元信息，避免访问日志的负载。

普通日志条目 GeneralEntry 的具体实现如下。

```java
public class GeneralEntry extends AbstractEntry { private final byte[] commandBytes; //构造函数 public GeneralEntry(int index, int term, byte[] commandBytes) { super(KIND_general, index, term); thiscommandBytes = commandBytes; } //获取命令数据 public byte[] getCommandBytes() { return thiscommandBytes; } @Override public String toString(){ return "GeneralEntry{" + "index=" + index + ", term=" + term + '}';   
1
```

在构造函数中，日志条目类型、日志索引和 term 被传递给父类 AbstractEntry，父类 AbstractEntry 抽象化了每条日志条目都有的基本信息。

GeneralEntry相比父类只是多了负载的部分。考虑到简便性，此处getCommandBytes并没有延迟计算，父类AbstractEntry的代码如下。

```java
abstract class AbstractEntry implements Entry {
    private final int kind;
    protected final int index;
    protected final int term;
    // 构造函数
    AbstractEntry(int kind, int index, int term) {
        this-kind = kind;
        this.index = index;
        this.term = term;
    }
    // 获取日志类型
    public int getKind() {
        return thiskind;
    }
    // 获取日志索引
    public int getIndex() {
        return index;
    }
    // 获取日志term
    public int getTerm() {
        return term;
    }
    // 获取元信息
    public EntryMeta getMeta() {
        return new EntryMeta(kind, index, term);
    }
} 
```

代码比较简单，AbstractEntry管理日志元信息中的kind、index和term3个字段。

另一种日志条目是空日志NoOpEntry，它相比普通日志条目要简短很多，具体代码如下。

```java
public class NoOpEntry extends AbstractEntry { //构造函数 public NoOpEntry(int index,int term){ super(KIND_NO_OP,index,term); } public byte[] getCommandBytes(){ return new byte[0]; } @Override
```

```txt
public String toString() { return "NoOpEntry[" + "index=" + index + ", term=" + term + ']'; } 
```

除了日志条目类型是 KIND_NO_OP之外，通过getCommandBytes方法返回的是负载数据为空的二进制数组。

以上是现阶段需要关注的主要日志条目类型，之后在服务器成员变更时还有其他类型的日志条目，具体会在服务器成员变更的章节中讲解。

# 5.2.2 日志存储的选型

使用什么介质存储日志是一个技术选型问题，5.1节提到的日志实现要求中提到日志会被覆写，这是一个关键的信息。加上Rafi算法中的稳定存储要求，可选的存储介质主要有以下几种。

（1）本地文件，使用RandomAccessFile随机访问。  
（2）内嵌KV数据库，本地文件模式  
（3）内嵌RDBMS数据库，本地文件模式

介质（2）和（3）其实比较相近，具体使用哪一种，依赖于团队或者个人的要求与选择。本书为了避免涉及具体内嵌数据库的API，直接使用RandomAccessFile操作本地文件。虽然是直接操作文件，但实现时也考虑了使用索引文件等策略加快操作速度，理论上不会比内嵌数据库慢太多。

另外，不管使用哪种方法，Raft算法的日志存储实现相对其他部分都要更复杂，建议在完全测试后和选举部分对接。当然那样会需要等比较长的时间，所以笔者采用了先实现基于内存的日志存储，然后实现基于文件的日志存储的方式。基于内存的日志一方面可以用于单元测试，另一方面也可用于在与选举部分对接后的集成测试。

# 5.2.3 日志条目的序列化

日志条目在具体写入文件之前，需要进行序列化操作。这里的序列化指的是把日志条目中的命令转换成二进制。在Java中可以直接操作DataOutputStream，也可以使用序列化框架。由于之后RPC组件也需要序列化，所以本书采用ProtocolBuffer v3作为序列化工具。之后在讲解上层服务时会给出ProtocolBuffer的定义文件。

# 5.2.4 日志接口

日志具体需要哪些操作？如果要分析，可能需要自己从头到尾实现一遍才能知道，但那样就

违背循序渐进实现的初衷了。作为参考，表5-1是笔者在实现时整理出来的现阶段需要的主要功能列表。

表 5-1 日志接口功能列表  

<table><tr><td>方法</td><td>功能</td><td>场景</td></tr><tr><td>getLastEntryMeta</td><td>获取最后一条日志条目的 term、 index 等信息</td><td>选取开始时、发送消息时 (RequestVoteRpc)</td></tr><tr><td>createAppendEntriesRpc</td><td>创建日志复制消息</td><td>Leader 向 Follower 发送日志复制消息时</td></tr><tr><td>appendEntry</td><td>增加日志条目</td><td>上层服务操作或者当前节点成为 Leader 后的第一条 NO-OP 日志</td></tr><tr><td>appendEntriesFromLeader</td><td>追加从 Leader 服务器过来的日志 条目序列</td><td>收到来自 Leader 服务器的日志复制请求时</td></tr><tr><td>advanceCommitIndex</td><td>推进 commitIndex</td><td>收到来自 Leader 服务器的日志复制请求时</td></tr></table>

以下是实际的日志接口代码，表5-1中的方法以粗体显示。

public interface Log{ int ALLENTRIES $= -1$ //获取最后一条日志的元信息 EntryMetagetLastEntryMeta(); //创建AppendEntries消息 AppendEntriesRpc createAppendEntriesRpc( int term, NodeId selfId, int nextIndex, int maxEntries); //获取下一条日志的索引 int getNextIndex(); //获取当前的commitIndex int getCommitIndex(); //判断对象的lastLogIndex和lastLogTerm是否比自己新 boolean isNewerThan(int lastLogIndex，int lastLogTerm); //增加一个NO-OP日志 NoOpEntry appendEntry(int term); //增加一条普通日志 GeneralEntry appendEntry(int term,byte[] command); //追加来自Leader的日志条目 boolean appendEntriesFromLeader(int prevLogIndex，int prevLogTerm，List <Entry> entries); //推进commitIndex void advanceCommitIndex(int newCommitIndex，int currentTerm); //void setTextMachine(StateMachine stateMachine); //关闭 void close();

上述代码中包含了表5-1中没有的方法，比如getNextIndex和getCommitIndex，这两个方法是外部用来获取日志组件的nextIndex和当前commitIndex的。getNextIndex在节点成为Leader服务器时使用，Leader服务器需要重置Follower服务器的日志复制进度，此时所有Follower服务器的初始nextLogIndex都是当前服务器下一条日志的索引。

isNewerThan很明显是用于日志比较的方法，更具体一些就是在收到RequestVote消息时，选择是否投票时使用的方法。使用getLastEntryMeta也能达到同样的目的，只是isNewerThan更直接。

close方法用于安全关闭日志组件。注释掉的setStateMachine方法比较特殊，可以简单地理解为是上层服务提供的应用日志的回调接口。在Raft算法中更新日志的commitIndex会间接地更新lastApplied，也就是应用上层服务写入日志时的那些操作。又由于更新commitIndex是在日志组件的内部，如果要通知上层服务应用日志中的操作，最简单的方法可能是让上层服务往日志组件里注册一个应用日志的操作回调接口。也可以在调用advanceCommitIndex方法的核心组件内回调上层服务应用命令的方法，但是那样也需要一个回调接口，所以本质其实是一致的。现阶段setStateMachine不是需要关注的内容，在之后讲解上层服务时会详细分析。

# 5.3 日志条目序列

如果直接实现日志接口，并不需要实现日志条目序列，因为两者是同样的东西。但是之后实现日志相关的优化，即Raft算法中提到的日志快照（snapshot）机制时，可能需要对Log实现进行很大的修改。假如现在提前做好日志组件的拆分，把序列相关的部分抽象出来，这样之后就可以更快地加入日志快照。

以下把日志组件中主要的操作对象，即日志条目序列，抽象为一个叫作 EntrySequence 的接口，这个接口的定义如下。

```txt
public interface EntrySequence {
    // 判断是否为空
    boolean isEmpty();
    // 获取第一条日志的索引
    int getFirstLogIndex();
    // 获取最后一条日志的索引
    int lastLogIndex();
    // 获取下一条日志的索引
    int nextLogIndex();
    // 获取序列的子视图，到最后一条日志
    List<Entry> subList(int fromIndex);
    // 获取序列的子视图，指定范围，不包括 toIndex 所指向的日志
    List<Entry> subList(int fromIndex, int toIndex);
```

```c
// 检查某个日志条目是否存在  
boolean isEntryPresent(int index); // 获取某个日志条目的元信息  
EntryMeta getEntryMeta(int index); // 获取某个日志条目  
Entry getEntry(int index); // 获取最后一个日志条目  
Entry lastEntry(); // 追加日志条目  
void append Entry entry); // 追加多条日志  
void append(List entries); // 推进 commitIndex  
void commit(int index); // 获取当前 commitIndex  
int getCommitIndex(); // 移除某个索引之后的日志条目  
void removeAfter(int index); // 关闭日志序列  
void close();
```

以上大部分方法都可以从名称上看出用途，但有一个不那么明显的方法是subList，实际代码中subList方法主要用于构造AppendEntries消息时获取指定区间的日志条目。另一个方法是removeAfter，主要用于在追加来自Leader节点的日志时出现日志冲突的情况下，移除现有日志。

接下来正式开始实现日志条目序列，日志条目序列主要有以下两个实现。

（1）基于内存的MemoryEntrySequence。  
（2）基于文件的FileEntrySequence。

两者都继承同一个父类 AbstractEntrySequence，父类抽象了大部分和存储无关的方法。下面先从抽象实现开始，讲解日志条目序列的实现。

# 5.3.1 抽象实现AbstractEntrySequence

日志条目序列有以下两个索引。

（1）logIndexOffset，日志索引偏移。  
（2）nextLogIndex，下一条日志的索引。

图5-1展示了索引之间的关系。

![](images/aec6a89b96db4e75acf3106582ac2eb37193aa40ab6656b51757bf85c4955214.jpg)  
图5-1 日志与索引的关系

日志索引偏移在当前的日志条目序列不是从1开始时，比如有日志快照时，现在暂时可以理解为这是第一条日志的索引，不管第一条日志是否存在。初始情况如下。

日志索引偏移 $=$ 下一条日志的索引 $= 1$

从上面的等式可以推导出，当日志索引偏移等于下一条日志的索引时，当前日志条目序列为空。

基于上述分析，日志条目序列索引相关方法的实现如下。

abstract class AbstractEntrySequence implements EntrySequence{ int logIndexOffset; int nextLogIndex; //构造函数，默认为空日志条目序列 AbstractEntrySequence(int logIndexOffset) { this.logIndexOffset $=$ logIndexOffset; this.nextLogIndex $=$ logIndexOffset; } //判断是否为空 publicbooleanisEmpty(){ return logIndexOffset $= =$ nextLogIndex; 1 //获取第一条日志的索引，为空的话抛错 publicintgetFirstLogIndex（）{ if(isEmpty()）{ throw new EmptySequenceException(); } returndoGetFirstLogIndex(); } //获取日志索引偏移 intdoGetFirstLogIndex（）{ return logIndexOffset; } //获取最后一条的索引，为空的话抛错 publicintgetLastLogIndex（）{ if(isEmpty()）{ throw new EmptySequenceException(); }

returndoGetLastLogIndex();   
1   
//获取最后一条日志的索引 intdoGetLastLogIndex（）{ return nextLogIndex-1;   
1   
//获取下一条日志的索引 publicintgetNextLogIndex() { return nextLogIndex;   
1   
//判断日志条目是否存在 publicboolean isEntryPresent(int index){ return !isEmpty(）&&index $\rightharpoondown$ doGetFirstLogIndex（）&&index $<   =$ doGetLastLogIndex();   
1

代码本身并不复杂，主要是区间和索引的判断。

接下来是随机获取日志条目相关方法的抽象实现，所有相关方法最终都委托到一个叫作doGetEntry的方法上。doGetEntry方法的实现涉及从存储读取日志条目的逻辑，所以交给子类来实现。

abstract class AbstractEntrySequence implements EntrySequence{ //获取指定索引的日志条目 public Entry getEntry(int index){ if(!isEntryPresent(index)){ return null; } return doGetEntry(index);   
1 //获取指定索引的日志条目元信息 public EntryMeta getEntryMeta(int index){ Entry entry $=$ getEntry(index); return entry != null?entry.getMeta():null;   
1 //获取最后一条日志条目 public EntrygetLastEntry() { return isEmpty()？null:doGetEntry(doGetLastLogIndex())   
1 //获取指定索引的日志条目（抽象方法） protected abstract Entry doGetEntry(int index);

日志条目序列子视图的方法委托给一个叫作doSubList的方法，doSubList同样需要访问存储中的日志条目。

```txt
abstract class AbstractEntrySequence implements EntrySequence{ //获取一个子视图，不指定结束索引 public List<Entry> subList(int fromIndex){ if(isEmpty() || fromIndex > doGetLastLogIndex()) { return Collections.emptyList(); } return subList(Math.max(fromIndex,doGetFirstLogIndex()， nextLogIndex); } //获取一个子视图，指定结束索引 public List<Entry> subList(int fromIndex，int toIndex）{ if(isEmpty()）{ throw new EmptySequenceException(); } //检查索引 if(fromIndex < doGetFirstLogIndex() || toIndex > doGetLastLogIndex() + 1 || fromIndex > toIndex) { throw new IllegalArgumentException( "illegal from index" + fromIndex + "or to index" + toIndex); } return doSubList(fromIndex,toIndex); } //获取一个子视图（抽象方法） protected abstract List<Entry> doSubList(int fromIndex,int toIndex); 
```

剩下的方法分别委托给doAppend和doRemoveAfter方法，这两个方法都需要操作现有日志存储介质。

```txt
abstract class AbstractEntrySequence implements EntrySequence { //追加多条日志 public void append(List<Entry> entries) { for（Entry entry：entries）{ append(entry); } //追加单条日志 public void append(Entry entry){ 
```

```txt
// 保证新日志的索引是当前序列的下一条日志索引
if (entry.getIndex() != nextLogIndex) {
throw new IllegalArgumentException("entry index must be " + nextLogIndex);
}
doAppend(entry);
// 递增序列的日志索引
nextLogIndex++;
}
// 追加日志（抽象方法）
protected abstract void doAppend Entry entry);
// 移除指定索引后的日志条目
public void removeAfter(int index) {
if (isEmpty() || index >= doGetLastLogIndex()) {
return;
}
doRemoveAfter(index);
}
// 移除指定索引后的日志条目（抽象方法）
protected abstract void removeAfter(int index);
}
```

设计日志条目序列需要考虑一个问题，日志的索引是类似于数据库表的自增列一样由序列自行管理，还是独立于序列每次生成。由于日志条目的抽象实现 AbstractEntry 中，日志的索引是一个必须在构造日志条目时决定的参数，因此不能使用自增列的方案。

本书实际使用的方案中并没有放宽日志条目的索引在构造时的限制，而是通过日志条目序列的getNextLogIndex提前获取下一条日志的索引，然后在addEntry方法中检查Entry的索引是否和当前nextLogIndex一致，如果不一致则抛异常（上面代码中的粗体部分）。这样既可以提前获取需要的日志条目索引，也可以保证实际加入的日志索引是连续的。同样的方法也可以处理来自Leader节点的新日志。因为新日志肯定是已经构建好的数据，无法简单地和自增列共存，所以通过检查后递增的方法可以有效保证数据的正确性。

# 5.3.2 基于内存的MemoryEntrySequence

在抽象实现的基础上，基于内存的具体实现类叫作MemoryEntrySequence。基于内存的日志条目序列使用ArrayList<Entry>作为操作对象，而不使用链表LinkedList的主要原因是，EntrySequence有一些方法需要随机访问日志序列，但是链表的随机访问性能比较低。

由于MemoryEntrySequence只需要操作内存中的列表，整体代码逻辑比较简单，具体实现如下。

public class MemoryEntrySequence extends AbstractEntrySequence{ private final List<Entry> entries $=$ new ArrayList<>(); private int commitIndex $= 0$ //构造函数，日志索引偏移1 public MemoryEntrySequence() { this(1); //构造函数，指定日志索引偏移 public MemoryEntrySequence(int logIndexOffset){ super(logIndexOffset); //获取子视图 protected List<Entry>doSubList(int fromIndex，int toIndex）{ return entries.subList(fromIndex-logIndexOffset,toIndex-logIndexOffset); //按照索引获取日志条目 protected Entry doGetEntry(int index){ return entries.get(index-logIndexOffset); //追加日志条目 protected void doAppend(Entry entry){ entries.add(entry); //提交，检验由外层处理 public void commit(int index){ commitIndex $\equiv$ index; //获取提交索引 public int getCommitIndex(){ return commitIndex; //移除指定索引后的日志条目 protected void removeAfter(int index){ if(index < doGetFirstLogIndex()){ entries.clear(); nextLogIndex $=$ logIndexOffset; }else{ entries.subList(index-logIndexOffset+1,entries.size()); clear(); nextLogIndex $=$ index+1;

} //关闭 public void close() { } @Override public String toString(){ return "MemoryEntrySequence{" + "logIndexOffset $= 1$ + logIndexOffset + ", nextLogIndex $= 1$ + nextLogIndex + ", entries.size $= 1$ + entries.size() + '}';   
}

注意，对于MemoryEntrySequence，日志条目追加时就等于提交了，所以commit方法不会对日志条目做更多的事情。同理，由于基于内存的实现，close方法什么也不会做。

# 5.3.3 基于文件的FileEntrySequence

基于文件的实现要复杂一些，实现FileEntrySequence主要涉及三部分内容。

（1）日志条目文件 EntriesFile。  
（2）日志条目索引文件 EntryIndexFile。  
（3）等待写入的日志条目缓冲pendEntries，类型为LinkedList<Entry>。

日志条目文件是一个包含全部日志条目内容的二进制文件，文件按照顺序从第一条日志条目连续存放到最后一条。文件允许每条日志条目的长度不一样（普通日志的负载长度不固定）。这主要是由于Raft算法中日志的特性，对过去的日志只有从某个索引开始移除的操作，不存在修改中间单条日志的操作，所以不会改变已写入日志条目的长度。

日志条目索引文件同样是一个二进制文件，只包含日志条目的元信息，即index、term、kind命令和在EntriesFile中的数据偏移。假如把EntriesFile理解为一个数据库表文件，使用RandomAccessFile随机访问，那么EntryIndexFile就是数据库表的索引。可以通过索引间接找到某条日志条目的数据偏移，然后访问并获取数据。对于一些只需要知道日志条目元信息的操作，甚至不需要访问日志条目文件，直接通过日志条目索引文件即可。

对于未写入文件的日志条目，被放在一个叫作 pendingEntries 的链表中。此处使用链表而不是 ArrayList 的原因是，提交日志时会从 pendingEntries 的前面移除日志，此操作可能比较频繁，所以用链表比较合适。虽然随机访问仍然存在，但是一般来说只有小部分日志条目存放在 pendingEntries 中，性能损耗可控。

图5-2是EntriesFile的文件结构。

![](images/f47aab7b90f2a874cdc4a869271f25db8495970b057649c6463cba854595ae82.jpg)  
图5-2 EntriesFile文件结构

EntriesFile 按照记录行的方式组织文件。每一行记录的内容有日志条目类型（4 个字节）、日志索引（4 个字节）、日志 term（4 个字节）、命令长度（4 个字节）和具体命令内容（变长）。EntriesFile 没有文件头之类的结构，快速访问依赖日志条目索引文件 EntryIndexFile，图 5-3 是 EntryIndexFile 的文件结构。

![](images/4ca539f038356483eae7337f77b1c513749568c52cf866619386f8c7f111b7fb.jpg)  
图5-3 EntryIndexFile文件结构

以 EntryIndexFile 开头的是起始索引和结束索引。接下来就是日志条目的元信息，包括在 EntriesFile 中的位置偏移、日志类型和日志 term。日志索引不包括在内，因为日志索引可以计算出来。比如第一条日志条目元信息的索引为 minEntryIndex，之后一条为 minEntryIndex + 1，最后一条日志条目元信息的索引为 maxEntryIndex。

在没有任何日志时，EntriesFile和EntryIndexFile两者均为空文件，没有任何内容。另外，EntryIndexFile可以用EntriesFile重建，也就是说只要记录文件还在，索引就可以重建。

通过把日志序列分成两个文件，对应代码也可以分成两个类。同时，为了提高性能，日志索引可以从一开始就加载到内存中，而具体日志条目的负载在需要的时候通过索引快速定位到偏移位置并加载。

EntriesFile和EntryIndexFile的实现都使用了RandomAccessFile。为了更方便地测试，这里把

用到的 RandomAccessFile 方法抽象出来，设计了一个叫作 SeekableFile 的接口，SeekableFile 接口代码如下。

```java
public interface SeekableFile{ //获取当前位置 long position()throws IOException; //移动到指定位置 void seek(long position) throws IOException; //写入整数 void writeInt(int i) throws IOException; //写入长整数 void writeLong(long l) throws IOException; //写入字节数组 void write(byte[]b) throws IOException; //读取整数 int readInt()throws IOException; //读取长整数 long readLong()throws IOException; //读取字节数组，返回读取的长度 int read(byte[] b) throws IOException; //获取文件大小 long size()throws IOException; //裁剪到指定大小 void truncate(long size) throws IOException; //获取从指定位置开始的输入流 InputStream inputStream(long start) throws IOException; //强制输出到磁盘 void flush()throws IOException; //关闭文件 void close()throws IOException; 
```

大部分方法和 RandomAccessFile 一样，部分方法有专门的用途。比如 truncate 用于在日志冲突时删除某个索引后的日志数据，inputStream 方法用于之后的日志快照。

有了这个接口，就可以设计一个像ByteArrayInputStream或ByteArrayOutputStream一样的基于内存的字节数组的实现。涉及文件的测试一般都比较麻烦，通过加入一个间接层可以解决部分问题。

测试用的 SeekableFile 实现会在之后的测试部分中给出，以下是实际运行时使用的基于 RandomAccessFile 的实现，基本上 SeekableFile 接口的每个方法都直接与 RandomAccessFile 的某个方法对应。

```java
public class RandomAccessFileAdapter implements SeekableFile { private final File file; 
```

private final RandomAccessFile randomAccessFile; //构造函数 public RandomAccessFileAdapter(File file) throws FileNotFoundException{ this(file，"rw"); } //构造函数，指定模式 public RandomAccessFileAdapter(File file,String mode) throws FileNotFoundException{ this.file $=$ file; randomAccessFile $\equiv$ new RandomAccessFile(file,mode); 1 //定位 public void seek(long position) throws IOException{ randomAccessFile.seek(position); } //写入整数 public void writeInt(int i) throws IOException{ randomAccessFile.writeInt(i); } //写入长整数 public void writeLong(long l) throws IOException{ randomAccessFile.writeLong(l); } //写入字节数据 public void write(byte[] b) throws IOException{ randomAccessFile.write(b); } //读取整数 public int readInt() throws IOException{ return randomAccessFile.readInt(); } //读取长整数 public long readLong() throws IOException{ return randomAccessFile.readLong(); } //读取字节数据，返回读取的字节数 public int read(byte[] b) throws IOException{ return randomAccessFile.read(b); } //获取文件大小 public long size() throws IOException{ return randomAccessFile.length();

} //裁剪文件大小 public void truncate(long size) throws IOException { randomAccessFile.setLength(size); } //获取从某个位置开始的输入流 public InputStream inputStream(long start) throws IOException { FileInputStream input $=$ new FileInputStream(file); if (start $>0$ ）{input skip(start); } return input; } //获取当前位置 public long position()throws IOException{ return randomAccessFile.getFilePointer(); } //刷新 public void flush()throws IOException{ } //关闭文件 public void close()throws IOException{ randomAccessFile.close(); }

接下来是基于SeekableFile的EntriesFile的具体实现，EntriesFile本身只支持以下几个方法。

（1）追加日志条目。  
（2）加载指定位置偏移的日志条目。  
（3）获取文件大小。  
（4）裁剪文件大小，包括清空，主要用于日志的removeAfter操作。

以下是具体代码。

public class EntriesFile{ private final SeekableFile seekableFile; //构造函数，普通文件 public EntriesFile(File file) throws FileNotFoundException{ this(new RandomAccessFileAdapter(file)); } //构造函数，SeekableFile public EntriesFile(SeekableFile seekableFile){ this.seekableFile $=$ seekableFile; 1 //追加日志条目

public long appendEntry Entry entry) throws IOException {long offset $=$ seekableFile.size();seekableFile.seek(offset);seekableFile.writeInt(entry.getKind());seekableFile.writeInt(entry.getIndex());seekableFile.writeInt(entry.getTerm());byte[] commandBytes $=$ entry Commands.length);seekableFile.writeInt commandedBytes.length);seekableFile.write commandedBytes);return offset;1//从指定偏移加载日志条目public Entry loadEntry(long offset, EntryFactory factory) throws IOException{if (offset $>$ seekableFile.size()){throw new IllegalArgumentException("offset $\gimel$ size");}seekableFile.seek(offset);int kind $=$ seekableFile.readInt();int index $=$ seekableFile.readInt();int term $=$ seekableFile.readInt();int length $=$ seekableFile.readInt();byte[] bytes $=$ new byte[length];seekableFile.read(bytes);return factory.create(kind,index,term,bytes);1//获取大小public long size()throws IOException{return seekableFile.size();1//清除所有内容public void clear()throws IOException{truncate(OL);1//裁剪到指定大小，偏移由调用者提供public void truncate(long offset) throws IOException{seekableFile.truncate(offset);1//关闭文件public void close()throws IOException{seekableFile.close();1

EntriesFile在初始化时不会读取文件内容，换句话说，EntriesFile是按需读取的。

在追加日志条目的操作 appendEntry 中，先定位到文件末尾，然后按照 kind、index、term 命令长度等负载内容的顺序写入文件。

在加载操作loadEntry中，定位到有效的位置偏移后，按照顺序读取日志条目内容，并交给EntryFactory实例化日志条目。

EntryFactory代码比较简单，就是一个普通的Factory实现，根据输入的参数构造对应的日志条目Entry的实例。

```txt
public class EntryFactory { public Entry create(int kind, int index, int term, byte[] commandBytes) { switch (kind) { case Entry.KIND_NO_OP: return new NoOpEntry(index, term); case Entry.KIND_general: return new GeneralEntry(index, term, commandBytes); default: throw new IllegalArgumentException("unexpected entry kind" + kind); } } 
```

至此，EntriesFile的介绍结束。

与 EntriesFile 相比，EntryIndexFile 要复杂一些，先是初始化和加载的部分。

```java
public class EntryIndexFile implements Runnable<EntryIndexItem> { //最大条目索引的偏移 private static final long OFFSET_MAX_ENTRY_INDEX = Integer.BYTES; //单条日志条目元信息的长度 private static final int LENGTH_ENTRY_INDEX_ITEM = 16; private final SeekableFile seekableFile; private int entryIndexCount; //日志条目数 private int minEntryIndex; //最小日志索引 private int maxEntryIndex; //最大日志索引 private Map<Integer, EntryIndexItem> entryIndexMap = new HashMap<>(); //构造函数，普通文件 public EntryIndexFile(File file) throws IOException { this(new RandomAccessFileAdapter(file)); } //构造函数，SeekableFile public EntryIndexFile(SeekableFile seekableFile) throws IOException { this.seekableFile = seekableFile; 
```

load();   
}   
//加载所有日志元信息   
private void load() throws IOException{ if (seekableFile.size() == 0L){ entryIndexCount = 0; return; } minEntryIndex $=$ seekableFile.readInt(); maxEntryIndex $=$ seekableFile.readInt(); updateEntryIndexCount(); //逐条加载 long offset; int kind; int term; for(int i $=$ minEntryIndex;i $<   =$ maxEntryIndex;i++) { offset $=$ seekableFile.readLong(); kind $=$ seekableFile.readInt(); term $=$ seekableFile.readInt(); entryIndexMap.put(i,new EntryIndexItem(i,offset,kind,term)); }   
1   
//更新日志条目数量   
private void updateEntryIndexCount(){ entryIndexCount $=$ maxEntryIndex-minEntryIndex+1;

加载主要在load方法中。对于空文件，设置entryIndexCount为0并结束。和日志条目序列抽象实现AbstractEntrySequence不同，日志条目元信息文件EntryIndexFile判断内容是否为空时并不使用minEntryIndex和maxEntryIndex，因为此时最小和最大索引没有意义，单独设置一个条目数直接判断可能更好。entryIndexCount用于在日志条目遍历时进行简单的修改检验。

如果文件不为空，按照之前介绍的结构读取文件，日志条目元信息被放在一个叫作 EntryIndexItem 的类中。

追加日志条目元信息的代码如下。

```java
public void appendEntryIndex(int index, long offset, int kind, int term) throws IOException {
    if (seekableFile.size() == 0L) {
        // 如果文件为空，则写入minEntryIndex
        seekableFile.writeInt(index);
        minEntryIndex = index;
    }
} 
```

```javascript
} else { //索引检查 if(index != maxEntryIndex + 1) { throw new IllegalArgumentException( "index must be" + (maxEntryIndex + 1) + ", but was" + index); } seekableFile.seek(OFFSET_MAX_ENTRY_INDEX); //跳过minEntryIndex } //写入maxEntryIndex seekableFile.writeInt(index); maxEntryIndex = index; updateEntryIndexCount(); //移动到文件最后 seekableFile.seek(getOffsetOfEntryIndexItem(index)); seekableFile.writeLong(offset); seekableFile.writeInt(kind); seekableFile.writeInt(term); entryIndexMap.put(index, new EntryIndexItem(index, offset, kind, term)); 
```

# //获取指定索引的日志的偏移

```txt
private long getOffsetOfEntryIndexItem(int index) { return (index - minEntryIndex) * LENGTH_ENTRY_INDEX_ITEM + Integer. BYTES * 2; } 
```

方法要处理文件为空和已有条目的情况。文件为空或者说 entryIndexCount 为 0 时，使用当前的日志索引作为 minEntryIndex 和 maxEntryIndex，然后从文件末尾追加数据，追加完成后更新自身的缓存。

appendEntry 只允许顺序追加，这是 Raft 算法中的日志操作的特点。

最后是针对日志 removeAfter 操作的移除元信息的代码，如下所示。

# //清除全部

```java
public void clear() throws IOException {
    seekableFile.truncate(0L);
    entryIndexCount = 0;
    entryIndexMap.clear();
}
//移除某个索引之后的数据
public void removeAfter(int newMaxEntryIndex) throws IOException {
    //判断是否为空
    if (isEmpty() || newMaxEntryIndex >= maxEntryIndex) {
        return;
    }
} 
```

```javascript
//判断新的maxEntryIndex是否比minEntryIndex小  
//如果是则全部移除  
if (newMaxEntryIndex < minEntryIndex) {  
    clear();  
    return;  
}  
//修改maxEntryIndex  
seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);  
seekableFile.writeInt(newMaxEntryIndex);  
//裁剪文件  
seekableFile.truncate(getOfEncodedEntryIndexItem(newMaxEntryIndex + 1));  
//移除缓存中的元信息  
for (int i = newMaxEntryIndex + 1; i <= maxEntryIndex; i++) {  
    entryIndexMap.remove(i);  
}  
maxEntryIndex = newMaxEntryIndex;  
entryIndexCount = newMaxEntryIndex - minEntryIndex + 1; 
```

clear 操作比较简单，直接删除所有数据。removeAfter 操作需要裁剪文件，删除部分缓存等。

除了以上操作之外，EntryIndexFile 还实现了 Iterator<EntryIndexItem> 接口，从外部可以直接遍历 EntryIndexFile 中所有的日志条目元信息。

```java
public Iterator<IndexItem> iterator() { //索引是否为空 if (isEmpty()) { return Collections.emptyIterator(); } return new EntryIndexIterator(entryIndexCount, minEntryIndex);   
}   
private class EntryIndexIterator implements Iterator<IndexItem> { private final int entryIndexCount; //条目总数 private int currentEntryIndex; //当前索引 //构造函数 EntryIndexIterator(int entryIndexCount, int minEntryIndex) { this entryIndexCount = entryIndexCount; this.currentEntryIndex = minEntryIndex; } //是否存在下一条 public boolean hasNext() { checkModification(); return currentEntryIndex <= maxEntryIndex; 
```

```java
}   
// 检查是否修改   
private void checkModification() { if (this entryIndexCount != EntryIndexFile.this.entryIndexCount) { throw new IllegalStateException("entry index count changed"); }   
// 获取下一条   
public EntryIndexItem next() { checkModification(); return entryIndexMap.get(currentEntryIndex++)；   
} 
```

EntriesFile、EntryIndexFile再加上日志条目缓冲 pendingEntries，构成了FileEntrySequence，以下是FileEntrySequence初始化时的代码。

public class FileEntrySequence extends AbstractEntrySequence{ private final EntryFactory entryFactory $=$ new EntryFactory(); private final EntriesFile entriesFile; private final EntryIndexFile entryIndexFile; private final LinkedList<Entry> pendingEntries $=$ new LinkedList<>(); // Raft算法中定义初始commitIndex为0，和日志是否持久化无关 private int commitIndex $= 0$ //构造函数，指定目录 public FileEntrySequence(LogDir logDir，int logIndexOffset）{//默认logIndexOffset由外部决定 super(logIndexOffset); try{ this.entriesFile $\equiv$ new EntriesFile(logDir.getEntriesFile()); this.EntryIndexFile $\equiv$ new EntryIndexFile(logDir. getEntryOffsetIndexFile(); initialize(); } catch（IOException e）{ throw new Exception("failed to open entries file or entry index file",e); } //构造函数，指定文件 public FileEntrySequence(EntriesFile entriesFile, EntryIndexFile entryIndexFile,int logIndexOffset){ //默认logIndexOffset由外部决定 super(logIndexOffset);

this.entriesFile $=$ entriesFile; this entryIndexFile $=$ entryIndexFile; initialize();   
1   
//初始化   
private void initialize() { if (entriesFile.isEmpty()) { return; } //使用日志索引文件的minEntryIndex作为logIndexOffset logIndexOffset $\equiv$ entryIndexFile.getMinEntryIndex(); //使用日志索引文件的maxEntryIndex加1作为nextLogOffset nextLogIndex $\equiv$ entryIndexFile.getMaxEntryIndex(）+1;   
1   
//获取commitIndex   
public int getCommitIndex() { return commitIndex;

初始化过程中，EntryIndexFile的最小日志索引和最大日志索引分别用来计算抽象父类的logIndexOffset和nextLogIndex。日志为空时，logIndexOffset由构造函数的参数指定。

第一个构造函数中的 LogDir 是一个抽象化的获取指定文件地址的接口，单独设计接口是为了避免直接在 FileEntrySequence 中硬编码文件名，LogDir 接口如下。

```java
public interface LogDir {
    // 初始化目录
    void initialize();
    // 是否存在
    boolean exists;
    // 获取 EntriesFile 对应的文件
    File getEntriesFile();
    // 获取 EntryIndexFile 对应的文件
    File getEntryOffsetIndexFile();
    // 获取目录
    File get();
    // 重命名目录
    boolean renameTo(LogDir logDir);
}
```

LogDir的实现很简单，只是一个java.io.File加上预设文件名的封装，在之后日志快照的部分会详细介绍。

FileEntrySequence中用于获取日志条目或者日志条目视图的代码如下。

protected List<Entry>doSubList(int fromIndex，int toIndex){ //结果分为来自文件的与来自缓冲的两部分 List<Entry> result $=$ new ArrayList<>(); //从文件中获取日志条目 if(!entryIndexFile.isEmpty()&&fromIndex $\text{一} =$ entryIndexFile. getMaxEntryIndex(){ int maxIndex $=$ Math.min(entryIndexFile.getMaxEntryIndex(）+1, toIndex); for(inti $=$ fromIndex; $\mathrm{i} <   \mathrm{max}$ Index;i++) { result.add(getEntryInFile(i)); 1 } //从日志缓冲中获取日志条目 if(!pendingEntries.isEmpty()&&toIndex $>$ pendingEntries.getLast(). getIndex()){ Iterator<Entry>iterator $=$ pendingEntries.iterator(); Entry entry; int index; while (iterator.hasNext()) { entry $=$ iterator.next(); index $=$ entry.getIndex(); if(index $\vDash$ toIndex){ break; } if(index $\vDash$ fromIndex){ result.add(index); } } } return result;   
//获取指定位置的日志条目 protected Entry doGetEntry(int index){ if(!pendingEntries.isEmpty()）{ int firstPendingEntryIndex $=$ pendingEntries.getLast().getIndex(); if(index >=firstPendingEntryIndex){ return pendingEntries.get(index-firstPendingEntryIndex); } } assert!entryIndexFile.isEmpty(); return getEntryInFile(index);   
1

```java
public EntryMeta getEntryMeta(int index) { if (!isEmptyPresent(index)) { return null; } if (!pendingEntries.isEmpty()) { int firstPendingEntryIndex = pendingEntries.getLast().getIndex(); if (index >= firstPendingEntryIndex) { return pendingEntries.get(index - firstPendingEntryIndex).META(); } return entryIndexFile.get(index).toEntryMeta(); } // 按照索引获取文件中的日志条目 private Entry getEntryInFile(int index) { long offset = entryIndexFile.getOffset(index); try { return entriesFile.loadEntry(offset, entryFactory); } catch (IOException e) { throw new IOException("failed to load entry" + index, e); } } // 获取最后一条日志 public EntrygetLastEntry() { if (isEmpty()) { return null; } if (!pendingEntries.isEmpty()) { return pendingEntries.getLast(); } assert !entryIndexFile.isEmpty(); return getEntryInFile(entryIndexFile.getMaxEntryIndex()); } 
```

基本上所有操作都要同时考虑日志文件和日志缓冲。如果想要获取最终文件中的日志条目，需要调用getEntryInFile，通过EntriesFile获取指定索引的日志条目。如果想要获取缓冲中的日志条目，则通过链表pendingEntries的get方法内部遍历获取。

FileEntrySequence中追加日志条目和removeAfter操作的代码如下。其中追加操作比较简单，就是将日志条目追加到日志缓冲中。

//追加日志条目

```javascript
protected void doAppend Entry entry) { pendingEntries.add (entry); 
```

）

//移除指定索引之后的日志条目

```txt
protected void removeAfter(int index) { //只需要移除缓冲中的日志 if(!pendingEntries.isEmpty() && index >= pendingEntries.getLast().getIndex() - 1) { //移除指定数量的日志条目 //循环方向是从小到大，但是移除是从后往前 //最终移除指定数量的日志条目 for (int i = index + 1; i <= doGetLastLogIndex(); i++) { pendingEntries.removeLast(); } nextLogIndex = index + 1; return; } try { if(index >= doGetFirstLogIndex()) { //索引比日志缓冲中的第一条日志小 pendingEntries.clear(); entriesFile.truncate(entryIndexFile.getOffset(index + 1)); entryIndexFile.removeAfter(index); nextLogIndex = index + 1; commitIndex = index; } else { //如果索引比第一条日志的索引都小，则清除所有数据 pendingEntries.clear(); entriesFile.clear(); entryIndexFile.clear(); nextLogIndex = logIndexOffset; commitIndex = logIndexOffset - 1; } } catch (IOException e) { throw new IOException(e); } 
```

doRemoveAfter方法中，需要判断移除的日志索引是否在日志缓冲中。如果在，那么就只需要从后往前移除日志缓冲中的部分日志即可，否则需要整体清除日志缓冲，文件需要裁剪。

最后展示一下commit操作的代码。

```txt
public void commit(int index) { // 检查commitIndex if (index < commitIndex) { 
```

```javascript
throw new IllegalArgumentException("commit index < " + commitIndex); } if (index == commitIndex) { return; } // 如果 commitIndex 在文件内，则只更新 commitIndex if (!entryIndexFile.isEmpty() && index <= entryIndexFile. getMaxEntryIndex()) { commitIndex = index; return; } // 检查 commitIndex 是否在日志缓冲的区间内 if (pendingEntries.isEmpty() || pendingEntries.getLast().getIndex() > index || pendingEntries.getLast().getIndex() < index) { throw new IllegalArgumentException("no entry to commit or commit index exceed"); } long offset; Entry entry = null; try { for (int i = pendingEntries.getLast().getIndex(); i <= index; i++) { entry = pendingEntries.removeFirst(); offset = entriesFile.appendEntry(entry); entryIndexFile.appendEntryIndex(i, offset, entry.getKind(), entry.getTerm()); commitIndex = i; } catch (IOException e) { throw new IOException("failed to commit entry " + entry, e); } 
```

commit 操作的主要目的是，把日志缓冲中的日志条目写入日志文件和日志条目索引中。不过由于 Raft 算法的特性，启动时把 commitIndex 设定为 0，理论上有可能新的 commitIndex 仍在文件中。比如集群中的 Follower 节点突然重启，假设重启的这段时间内没有新日志，Follower 节点收到来自 Leader 节点的心跳消息并更新自己的 commitIndex，很明显此时的 commitIndex 仍在文件中。对于 commit 方法来说，此时只需要更新 commitIndex 即可。

一般情况下，新的commitIndex在日志缓冲中，commit方法需要逐个把日志条目写入文件，并设置新的commitIndex。

# 5.4 日志实现

日志条目序列实现之后，接下来考虑如何和日志接口对接。和日志条目序列一样，日志实现分为基于内存的MemoryLog和基于文件的FileLog两个类，这两个类拥有同一个父类AbstractLog。

父类 AbstractLog 负责把 EntrySequence 接口和 Log 的大部分接口方法对接起来。理论上只要有 EntrySequence 即可完整实现 Log 接口，但实际上根据存储部分的具体逻辑（如基于文件、基于内存），需要做特殊处理，比如初始化和之后会讲解的日志快照。

对使用者来说，Log接口的实现只需要知道MemoryLog和FileLog即可。MemoryLog所对应的日志条目序列MemoryEntrySequence，以及FileLog对应的FileEntrySequence作为内部实现并不公开。

日志实现主要集中在AbstractLog内，部分特化的逻辑将在之后讲解。

# 5.4.1 抽象实现AbstractLog

AbstractLog没有设置构造函数，也就是只有默认的构造函数。AbstractLog所依赖的日志条目序列EntrySequence虽然可以作为必要参数通过构造函数传入，但是子类在构造日志条目序列时需要的步骤比较多，所以这里放松了要求，允许子类在构造函数中构造日志条目序列，而不是通过父类的构造函数传入。

下面的代码用于构造 RequestVote 消息内容和 AppendEntries 消息的接口实现。

abstract class AbstractLog implements Log { private static final Logger logger $\equiv$ LoggerFactory.getLogger(AbstractLog.   
class); protected EntrySequence entrySequence; //获取最后一条日志的元信息 public EntryMeta getLastEntryMeta() { if (entrySequence.isEmpty()) { return new EntryMeta(Entry.KIND_NO_OP,0,0); } return entrySequence.getLastEntry().getMeta(); 1 //创建AppendEntries消息 public AppendEntriesRpc createAppendEntriesRpc(int term，NodeId selfId, int nextIndex，int maxEntries）{ //检查nextIndex int nextLogIndex $=$ entrySequence.getLastLogIndex(); if(nextIndex $>$ nextLogIndex）{ throw new IllegalArgumentException("illegal next index" $^+$ nextIndex);

```txt
}  
AppendEntriesRpc rpc = new AppendEntriesRpc();  
rpc.setTerm(term);  
rpc.setLeaderId(selfId);  
rpc.setLeaderCommit(commitIndex);  
//设置前一条日志的元信息，有可能不存在  
Entry entry = entrySequence.getEntry(nextIndex - 1);  
if (entry != null) {  
    rpc.setPrevLogIndex(entry.getIndex());  
    rpc.setPrevLogTerm(entry.getTerm());  
}  
//设置entries  
if (!entrySequence.isEmpty()) {  
    int maxIndex = (maxEntries == ALL_ENTRIES ? nextLogIndex : Math.min(nextLogIndex, nextIndex + maxEntries));  
    rpc.setEntries(entrySequence.subList(nextIndex, maxIndex));  
}  
return rpc; 
```

RequestVote 消息只需要获取最后一条日志的元信息即可。对于空日志，返回一个空日志条目，索引和 term 都为 0（此处也可以返回一个 null 提示日志为空，但是上层代码需要检查）。

由于AppendEntries消息涉及的日志数据比较多，比如前一条日志、字段entries的多条日志之后还会涉及日志快照，所以整体选择在Log中构建。

方法参数maxEntries表示最大读取的日志条数。Raft算法中没有提及AppendEntries消息中日志条目的数量，但是假如传输全部日志条目，有可能会导致网络拥堵。为了限制日志条目数量，本书增加了maxEntries参数。maxEntries默认为-1，表示传输从nextLog到最后的全部日志条目。

以下是用于RequestVote中投票检查的isNewerThan方法。按照Raft算法的要求，取出最后一条日志条目的元信息，先判断term再判断索引。

```txt
public boolean isNewerThan(int lastLogIndex, int lastLogTerm) { EntryMeta lastEntryMeta = getLastEntryMeta(); logger.debug("last entry {}, {}, candidate {},{})", lastEntryMeta.Index(), lastEntryMeta.Term(), lastLogIndex, lastLogTerm); return lastEntryMeta.getTerm() > lastLogTerm || lastEntryMeta.getIndex() > lastLogIndex; } 
```

追加日志条目的 appendEntry 方法实现如下。

```java
//追加NO-OP日志
public NoOpEntry appendEntry(int term) {
    NoOpEntry entry = new NoOpEntry(entrySequencegetNextLogIndex(), term);
    entrySequence.append(term);
    return entry;
}
//追加一般日志
public GeneralEntry appendEntry(int term, byte[] command) {
    GeneralEntry entry = new GeneralEntry(entrySequence.getNextLogIndex(), term, command);
    entrySequence.append(term);
    return entry;
} 
```

两个 appendEntry 代码类似，都是先从 EntrySequence 获取下一条日志的索引，然后追加并返回，之前日志条目序列的部分中有提到这么做的原因。

AbstractLog中最复杂的应该是appendEntriesFromLeader方法。从名称上看，这个方法仅追加日志条目，但实际上在追加之前还需要移除不一致的日志条目。移除时从最后一条匹配的日志条目开始，之后所有冲突的日志条目都会被移除。理论上移除冲突日志的情况很少发生，但是发生时是否能正确处理很重要，建议编写足够的测试来保证处理的正确性。

```java
public boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> leaderEntries) { // 检查前一条日志是否匹配 if (!checkIfPreviousLogMatches(prevLogIndex, prevLogTerm)) { return false; } // Leader节点传递过来的日志条目为空 if (leaderEntries.isEmpty()) { return true; } //移除冲突的日志条目并返回接下来要追加的日志条目（如果还有的话） EntrySequenceView newEntries = removeUnmatchedLog(new EntrySequenceView(leaderEntries)); //仅追加日志 appendEntriesFromLeader(newEntries); return true; } 
```

appendEntriesFromLeader 先检查从 Leader 节点过来的 prevLogIndex 和 prevLogTerm 是否匹配本地日志。如果不匹配，则返回 false，即追加失败。

参数中的prevLogIndex不一定对应最后一条日志，Leader节点会从后往前找到第一个匹配的日志，所以此处需要随机访问EntrySequence来获取日志条目的元信息。

private boolean checkIfPreviousLogMatches(int prevLogIndex, int prevLogTerm) { // 检查指定索引的日志条目 EntryMeta meta $=$ entrySequence.getEntryMeta(prevLogIndex); // 日志不存在 if (meta $= =$ null) { logger.debug("previous log{} not found", prevLogIndex); return false; } int term $=$ meta.getTerm(); if(term != prevLogTerm){ logger.debug("different term of previous log, local {}, remote }, term, prevLogTerm); return false; } return true; }

检查结果如果是匹配，则判断 leaderEntries 是否为空。如果为空，则有可能是来自 Leader 节点的心跳信息，或者日志同步完毕，不管是哪一种，都不需要进一步操作，直接返回结果 true。

接下来按照先移除后追加的方式操作日志。EntrySequenceView 是一个方便操作的类，提供了类似于 EntrySequence 的按照日志索引获取、根据子视图检查等实用功能，用来代替直接操作 Entry 数组，EntrySequenceView 的具体实现如下。

```java
private static class EntrySequenceView implements Iterator<Entry> {
    private final List<Entry> entries;
    private int firstLogIndex = -1;
    private int lastLogIndex = -1;
    // 构造函数
    EntrySequenceView(List<Entry> entries) {
        this.entries = entries;
        if (!entries.isEmpty()) {
            firstLogIndex = entries.get(0).getIndex();
            lastLogIndex = entries.getentries.size() - 1).getIndex();
        }
    }
} // 获取指定位置的日志条目
Entry get(int index) {
    if (entries.isEmpty() || index < firstLogIndex || index > lastLogIndex) {
        return null;
    }
} 
```

```java
} return entries.get(index-firstLogIndex);   
1 //判断是否为空 boolean isEmpty(){ return entries.isEmpty(); } //获取第一条记录的索引，此处没有非空校验 int getFirstLogIndex(){ return firstLogIndex; 1 //获取最后一条记录的索引，此处没有非空校验 int getLastLogIndex(){ return lastLogIndex;   
1 //获取子视图 EntrySequenceView subView(int fromIndex){ if (entries.isEmpty() ||fromIndex>lastLogIndex){ return new EntrySequenceView(CollectionsonEmptyList()); } return new EntrySequenceView( entries.subList(fromIndex-firstLogIndex,entries.size()) ）；   
1 //遍历用 @Override @Nonnull public Iterator<Entry> iterator(){ return entries.iterator();
```

比起操作原来数组的 index，EntrySequenceView 可以直接传入日志条目的索引操作，这也是使用 EntrySequenceView 的主要目的。

移除不一致的操作removeUnmatchedLog，先尝试找到第一个不一致的日志条目，然后按移除之后的日志条目的方式执行，具体代码如下。

```java
private EntrySequenceView removeUnmatchedLog(EntrySequenceView leaderEntries) { // Leader节点过来的entries不应该为空 assert !leaderEntries.isEmpty(); //找到第一个不匹配的日志索引 int firstUnmatched = findFirstUnmatchedLog(leaderEntries); //没有不匹配的日志 
```

if(firstUnmatched<0）{ return new EntrySequenceView(Collectionson.emptyList(); } //移除不匹配的日志索引开始的所有日志 removeEntriesAfter(firstUnmatched-1); //返回之后追加的日志条目 return leaderEntries.subView(firstUnmatched); 1 //查找第一条不匹配的日志 private int findFirstUnmatchedLog(EntrySequenceView leaderEntries) { int logIndex; EntryMeta followerEntryMeta; //从前往后遍历leaderEntries for（Entry leaderEntry：leaderEntries）{ logIndex $=$ leaderEntry.getIndex(); //按照索引查找日志条目元信息 followerEntryMeta $\equiv$ entrySequence.getEntryMeta(logIndex); //日志不存在或者term不一致 if(followerEntryMeta $= =$ null||followerEntryMeta.getTerm()！ $=$ leaderEntry.getTerm(){ return logIndex; 1 //否则没有不一致的日志条目 return -1;

寻找第一个不一致日志条目的findFirstUnmatchedLog方法会遍历leaderEntries，按照logIndex随机访问本地日志序列中的日志元信息，如果本地不存在或者日志的term不一致，则认为当前logIndex对应的日志为第一条冲突日志。如果没找到不一致的日志，则返回-1。

如果存在有冲突的日志条目，则removeUnmatchedLog会调用removeEntriesAfter移除最后一个匹配的日志之后的所有日志。注意，removeEntriesAfter的参数为firstUnmatched-1，也就是最后一个匹配的日志条目的索引。

```javascript
private void removeEntriesAfter(int index) { if (entrySequence.isEmpty() || index >= entrySequence.getLastLogIndex()) { return; } //注意，此处如果移除了已经应用的日志，需要从头开始重新构建状态机 logger.debug("remove entries after {}, index); entrySequence.removeAfter(index); }
```

removeEntriesAfter先判断是否需要移除日志，如果本地日志为空或者对应最后一条日志的索引，则不需要进行任何操作。接下来委托给EntrySequence的removeAfter方法。

注意，移除操作会影响状态机以及 EntrySequence 中的 commitIndex。上面的注释中也提到，如果移除了已经被应用的日志，必须从头开始重新构建状态机。移除被 commit 的日志，理论上是有可能的，此时需要回退到最后一个有效的日志的 index。由于现在尚未实现服务的状态机，因此先不讲解这种可能。

移除操作完成后，leaderEntries中的一部分或者全部日志将按照要求被追加到本地日志中。在removeUnmatchedLog方法的最后，leaderEntries.subView(firstUnmatched）表示本地没有的日志条目序列。

```javascript
private void appendEntriesFromLeader EntrySequenceView leaderEntries) { if (leaderEntries.isEmpty()) { return; } logger.debug("append entries from leader from {} to {}, leaderEntries.getLastLogIndex(), leaderEntries.getLastLogIndex()); for (Entry leaderEntry : leaderEntries) { entrySequence.append(leaderEntry); } } 
```

appendEntriesFromLeader简单地检查一下来自Leader节点的日志条目序列是否为空，如果不为空，则顺序追加日志条目。

AbstractLog 中一个相对复杂的内容是推进 commitIndex 的方法。方法实现如下。

```txt
public void advanceCommitIndex(int newCommitIndex, int currentTerm) {
    if (!validateNewCommitIndex(newCommitIndex, currentTerm)) {
        return;
    }
    logger.debug("advance commit index from {} to {}, commitIndex, newCommitIndex");
    entrySequence.commit(newCommitIndex);
    // advanceApplyIndex();
}
// 检查新的 commitIndex
private boolean validateNewCommitIndex(int newCommitIndex, int currentTerm) {
    // 小于当前的 commitIndex
    if (newCommitIndex <= entrySequence.getCommitIndex()) {
        return false;
    }
    EntryMeta meta = entrySequence.getEntryMeta(newCommitIndex);
} 
```

```javascript
if (meta == null) {
    logger.debug("log of new commit index {}
    not found", newCommitIndex);
    return false;
}
// 日志条目的 term 必须是当前 term，才可推进 commitIndex
if (meta.getTerm() != currentTerm) {
    logger.debug("log term of new commit index != current term {}
! = {}
", entry.getTerm(), currentTerm);
    return false;
} 
```

按照 Raft 算法，推进 commitIndex 需要检查日志的 term 是否与当前 term 一致，validateNew CommitIndex 方法实现了这一检查。如果通过了检查，则调用日志条目序列的 EntrySequence 的 commit 方法推进 commitIndex。

advanceApplyIndex 涉及状态机的操作，现阶段不展开讲解。

# 5.4.2 基于内存的MemoryLog

在抽象实现 AbstractLog 的基础上，MemoryLog 代码如下。

```java
public class MemoryLog extends AbstractLog { //构造函数，无参数 public MemoryLog() { this(new MemoryEntrySequence()); } //构造函数，针对测试 MemoryLog EntrySequence entrySequence) { this entrySequence = entrySequence; } 
```

MemoryLog 只公开了无参数的构造函数，另一个可见的构造函数主要针对测试，MemoryLog 的其他代码沿用抽象实现 AbstractLog。

理论上，基于内存的日志在重启后数据会丢失。换句话说，MemoryLog启动时，系统的日志为空，这个特性在测试中很有用。

MemoryLog 初始化时使用的 MemoryEntrySequence 方法中，logIndexOffset 和 nextLogIndex 默认均为 1。

# 5.4.3 基于文件的FileLog

FileLog 的初始化过程稍微复杂一些，代码如下。

public class FileLog extends AbstractLog{ private final RootDir rootDir; //构造函数 publicFileLog(FilebaseDir){ rootDir $=$ newRootDir(baseDir); //获取最新的日志代 LogGenerationlatestGeneration $=$ rootDir.getLatestGeneration(); if (latestGeneration $! =$ null）{//日志存在 entrySequence $=$ newFileEntrySequence( latestGeneration，latestGeneration.getLogIndexOffset() 1）else{ //日志不存在 LogGenerationfirstGeneration $=$ rootDir.createFirstGeneration(); entrySequence $=$ newFileEntrySequence(firstGeneration,1); }

baseDir表示日志目录的根目录，根目录下存在多个日志的分代（generation）。

```txt
log-root  
| -log-1  
| | -entries.bin  
| / -entries.idx  
| -log-100  
| -entries.bin  
| / -entries.idx 
```

上面的文件树中，log-root为baseDir，下面的log-1、log-100是两个日志代。文件夹名称中的数字是日志索引偏移（logIndexOffset）。第一个日志代因为logIndexOffsetw为1，所以对应文件夹log-1。日志代的日志索引偏移越大，表示此日志代越新。划分为多个分代主要是为了之后的日志快照。日志代目录里的entries.bin对应EntriesFile，entries.idx对应EntryIndexFile。

FileLog 初始化时，读取日志根目录下最新的日志分代。日志分代 LogGeneration 会比较目录名中的日志索引偏移，目录名中 logIndexOffset 最大的就是最新的日志分代。如果没有找到任何日志分代，则创建一个，并设置日志索引偏移为 1。

至此，日志部分的实现基本完成。接下来将给出如何对接选举部分的分析。

# 5.5 与选举部分对接

本节从构造核心组件开始讲解对接，然后是核心组件中的每个事件和消息。关联组件的修改本节并没有列出，比如RPC组件中需要增加针对日志条目的序列化和反序列化处理。

对接后，默认设定日志为基于内存的日志，原有测试应该仍能正常运行，如果有问题，请按照错误提示修改。

# 5.5.1 NodeContext和NodeBuilder

取消 NodeContext 中 Log 的注释，并在 NodeBuilder 中增加 Log 字段，允许外部设置 Log 以及 build 时默认使用 MemroyLog。

```java
private NodeContext buildContext() {
    NodeContext context = new NodeContext();
    // ...
    context.setLog(log != null ? log : new MemoryLog());
    return context;
} 
```

添加了上述代码后，既有代码就不会出现空指针异常。

# 5.5.2 选举超时

使用electionTimeout发送RequestVote消息时，需要设置lastLogIndex和lastLogTerm。实现了Log之后，可以通过Log的getLastEntryMeta方法获取最后一条日志的元信息，并设置最后一条日志的索引和term。

EntryMeta lastEntryMeta $=$ context.log().getLastEntryMeta(); RequestVoteRpcrpc $=$ new RequestVoteRpc(); //... rpc.setLastLogIndex(lastEntryMeta.getIndex()); rpc.setLastLogTerm(lastEntryMeta.getTerm());

之前实现getLastEntryMeta时，如果碰到没有日志的情况，返回的是一个索引和term都为0的EntryMeta实例，而不是一个null，所以此处代码不需要检查返回值lastEntryMeta是否为null。

# 5.5.3 收到RequestVote消息

在收到其他节点发来的RequestVote时，需要比较自己最后一条日志与消息中的最后一条日志的元信息。可以直接使用Log的isNewerThan方法，也就是只要修改一行代码即可。逻辑上是否投票要看对方是否比自己新，所以这里需要取反（本地不比对方新）。

boolean voteForCandidate $=$ !context.log().isNewerThan(
rpc.getLastLogIndex(), rpc.getLastLogTerm());

# 5.5.4 发送AppendEntries消息

AppendEntries 消息理论上主要有两种，一种是成为 Leader 节点后的心跳消息，另一种是普通的日志复制消息。前者传输的日志条目数量为 0。本书并没有刻意区分这两种消息，具体发送多少条日志条目由日志复制进度决定。第一次传输时，由于 nextIndex 指向 Leader 节点接下来的一条日志索引，因此自动传输 0 条日志。

发送 AppendEntries 消息对接日志组件比较简单。由于涉及多个日志数据，因此日志组件直接提供了 createAppendEntriesRpc 的方法。

```java
private void doReplicateLog(GroupMember member, int maxEntries) { AppendEntriesRpc rpc = context.log().createAppendEntriesRpc( role术语(), context.selfId(), membergetNextIndex(), maxEntries); contextconnector().sendAppendEntriesrpc, member.getEndpoint()); } 
```

maxEntries需要外部传入，默认为-1，即从nextIndex到最后一条日志。也可以把它作为一个配置参数，启动时从配置文件中读取。

# 5.5.5 收到AppendEntries消息

之前 appendEntries 的实现只是返回 true，现在有了日志组件，可以对接日志组件的 appendEntriesFromLeader 方法。

private boolean appendEntries(AppendEntriesRpc rpc) {boolean result $=$ context.log().appendEntriesFromLeader(rpc.getPrevLogIndex(),rpc.getPrevLogTerm(),rpc.getEntries());if(result){context.log().advanceCommitIndex( $\mathbf{\Pi}_{\mathrm{max}}$ (rpc.getLeaderCommit()，rpc.getLastEntryIndex()）,rpc.getTerm();1return result;

当追加成功时，Follower节点需要根据Leader节点的commitIndex决定是否推进本地的commitIndex。

# 5.5.6 收到AppendEntries响应

Leader 服务器收到 AppendEntries 的响应，除了更新日志复制进度之外，还需要计算新的 commitIndex，具体代码如下。

private void doProcessAppendEntriesResult(AppendEntriesResultMessage resultMessage) { // ... NodeId sourceNodeId = resultMessage.getSourceNodeId(); GroupMember member = context.group().getMembersourceNodeId); //没有指定的成员 if (member == null){ logger.info( "unexpected append entries result from node $\{\}$ ，node maybe removed", sourceNodeId); return; } AppendEntriesRpc rpc = resultMessage RPC(); if(result.isSuccess()){ //回复成功 //推进matchIndex和nextIndex if(memberadvanceReplicatingState(rpc.getLastEntryIndex())){ //推进本地的commitIndex context.log().advanceCommitIndex( context.group().getMatchIndexOfMajor(),role.Term()); 1 } else { //对方回复失败 if(!member.backOffNextIndex()）{ logger.warn("cannot back off next index more,node $\{\}$ ", sourceNodeId); } }

在收到成功响应的分支中，member 的 advanceReplicatingState 负责把节点的 matchIndex 更新为消息中最后一条日志的索引，nextIndex 更新为 matchIndex + 1。如果 matchIndex 和 nextIndex 没有变化，则 advanceReplicatingState 方法返回 false。

对于 result 不成功的响应，Leader 服务器需要回退 nextIndex。如果回退失败，也就是无法回退（比如 nextIndex 为 1，就不能回退为 0，这种情况理论上不会出现），则打印警告日志。

group 的 getMatchIndexOfMajor 是服务器成员组件用于计算过半 commitIndex 的方法，具体代码如下。

```java
int getMatchindexOfMajor() { List<NodeMatchIndex> matchIndices = new ArrayList<>(); for (GroupMember member : memberMap.values()) { if (!member.idEquals(selfId)) { matchIndices.add(new NodeMatchIndex(member.getId(), member.getMatchIndex())); } int count = matchIndices.size(); //没有节点的情况 if (count == 0) { throw new.IllegalStateException("standalone or no major node"); } Collections.sort(matchIndices); logger.debug("match indices {}, matchIndices); //取排序后的中间位置的 matchIndex return matchIndices.get(count / 2).getMatchIndex(); } 
```

getMatchIndexOfMajor方法收集除了自己以外节点的matchIndex，排序并取中间位置的matchIndex为过半matchIndex。以下用奇数和偶数个节点的集群为例，证明代码的正确性。

比如5服务器节点集群A、B、C、D和E，节点A为Leader服务器，matchIndex分别如下。

（1）节点A：4。  
（2）节点B：2。  
（3）节点 $C:4$   
（4）节点D：3。  
（5）节点E：4。

去除节点A之后，排序得到（B:2）、（D:3）、（C:4）、（E:4）。中间位置为4/2即2，取（C：4）中的4作为过半matchIndex。

又如4服务器节点集群A、B、C和D，节点A为Leader服务器，matchIndex分别如下。

（1）节点A：4。  
（2）节点B：2。  
（3）节点C：4。  
（4）节点D：3。

去掉节点A排序后得到 $(\mathrm{B}:2),(\mathrm{D}:3),(\mathrm{C}:4)$ 。中间位置为 $3 / 2$ 即1，取（D：3）中的3作为过半matchIndex。

设计类 NodeMatchIndex 只是为了同时记录节点 ID 和 matchIndex，NodeMatchIndex 在比较时只比较 matchIndex。如果不需要在日志中同时显示节点 ID 和 matchIndex，则可以省略节点 ID 直接操作 matchIndex。

```java
private static class NodeMatchIndex implements Comparable<NodeMatchIndex> {
    private final NodeId nodeId;
    private final int matchIndex;
    // 构造函数
    NodeMatchIndex(NodeId nodeId, int matchIndex) {
        this.nodeId = nodeId;
        this.matchIndex = matchIndex;
    }
    // 获取 matchIndex
    int getMatchIndex() {
        return matchIndex;
    }
    @Override
    public int compareTo(@Nonnull NodeMatchIndex o) {
        return Integer.matchIndex, o.matchIndex);
    }
    @Override
    public String toString() {
        return "<" + nodeId + ", " + matchIndex + ">";
    }
} 
```

至此，日志组件和选举部分对接完成。

# 5.6 测试

由于日志组件涉及的文件等相比于选举部分要复杂一些，而且日志作为基础组件比较难以集成测试。因此，建议在有足够的单元测试保证正确性之后，再和选举部分对接。

由于MemoryEntrySequence和FileEntrySequence共享了大部分AbstractEntrySequence的代码，因此测试MemoryEntrySequence可以间接测试大部分FileEntrySequence的逻辑。同样地，MemoryLog和FileLog也共享了大部分逻辑，因此本书着重测试MemoryLog，只测试部分和文件相关的FileEntrySequence和FileLog。

# 5.6.1 MemoryEntrySequence的基本操作

日志条目追加操作的代码如下。代码追加了一条NO-OP日志，term为1，然后检查nextLogIndex和lastLogIndex。

```java
public class MemoryEntrySequenceTest {
    @Test
    public void testAppendEntry() {
        MemoryEntrySequence sequence = new MemoryEntrySequence();
        sequence.append(new NoOpEntry(sequence.hasNextLogIndex(), 1));
        Assert.assertEquals(2, sequence.getLastLogIndex());
        Assert.assertEquals(1, sequence.getLastLogIndex());
    }
} 
```

日志条目随机访问操作的代码如下。

```java
public class MemoryEntrySequenceTest {
    // 随机访问日志条目
    @Test
    public void testGetEntry() {
        MemoryEntrySequence sequence = new MemoryEntrySequence(2);
        sequence.append(Arrays.asList(
            new NoOpEntry(2, 1),
            new NoOpEntry(3, 1)
        );
        Assert assertNull(sequence.getEntry(1));
        Assert.assertEquals(2, sequence.getEntry(2).getIndex());
        Assert.assertEquals(3, sequence.getEntry(3).getIndex());
        Assert.assertNull(sequence.getEntry(4));
    }
    // 随机访问日志条目元信息
    @Test
    public void testGetEntryMeta() {
        MemoryEntrySequence sequence = new MemoryEntrySequence(2);
        Assert.assertNull(sequence.getEntry(2));
        sequence.append(new NoOpEntry(2, 1));
        EntryMeta meta = sequence.getEntryMeta(2);
        Assert.assertNotNull.meta);
        Assert.assertEquals(2, meta.getIndex());
        Assert.assertEquals(1, meta.getTerm());
    }
} 
```

子序列操作的代码如下。

```java
public class MemoryEntrySequenceTest { @Test public void testSubListOneElement() { 
```

MemoryEntrySequence sequence $=$ new MemoryEntrySequence(2); sequence.append(Arrays.asList( new NoOpEntry(2,1), new NoOpEntry(3,1) ); List<Entry>subList $\equiv$ sequence.subList(2,3); Assert.assertEquals(1,subList.size()); Assert.assertEquals(2,subList.get(0).getIndex());   
}

移除操作的代码如下。

```java
public class MemoryEntrySequenceTest {
    @Test
    public void testRemoveAfterPartial() {
        MemoryEntrySequence sequence = new MemoryEntrySequence(2);
        sequence.append(Arrays.asList(
            new NoOpEntry(2, 1),
            new NoOpEntry(3, 1)
        ));
        sequence.removeAfter(2);
        Assert.assertEquals(2, sequence.getLastLogIndex());
        Assert.assertEquals(3, sequence.getLastLogIndex());
    }
} 
```

# 5.6.2FileEntrySequence的基本操作

FileEntrySequence依赖EntriesFile和EntryIndexFile。文件相关的测试比较困难，但是本书设计了一个SeekableFile的接口，可以用字节数组代替实际的文件进行EntriesFile和EntryIndexFile的测试，以下是测试用的SeekableFile的实现。

writeXXX 全部委托给 write 方法。以下是构造函数和 writeXXX 的实现。

```java
import com.google.common.primitives.Ints;   
import com.google.common.primitives.Longs;   
public class ByteArraySeekableFile implements SeekableFile { private byte[] content; private int size; private int position; //构造函数 public ByteArraySeekableFile() { 
```

this(new byte[0]);   
}   
//构造函数，指定内容   
public ByteArraySeekableFile(byte[] content){ this(content $=$ content; this.size $=$ content.length; this.position $= 0$ ：   
1   
//检查偏移   
private void checkPosition(long position）{ if (position $<  0$ || position $\rightharpoondown$ size）{ throw new IllegalArgumentException("offset < 0 or offset >   
size"); }   
}   
@override   
public void writeInt(int i) throws IOException { write(Ints.   
toByteArray(i));   
//确保空间大小   
private void ensureCapacity(int capacity){ int oldLength $=$ content.length; if (position $^+$ capacity $<   =$ oldLength）{ return; } if(oldLength $= = 0$ ）{ content $=$ new byte[capacity]; return; } int/newLength $=$ (oldLength $> =$ capacity?oldLength\*2:oldLength+   
capacity); byte[]newContent $=$ new byte下一篇(); System.arraycopy(content,0,newContent,0,oldLength); content $=$ contentType;   
} @Override   
public void writeLong(long l) throws IOException{ write(Longs.toArrayarray(l));   
} @Override   
public void write(byte[] b) throws IOException{ int n $=$ b.length; ensureCapacity(n);

```javascript
System.arraycopy(b, 0, content, position, n); size = Math.max(position + n, size); position += n; } 
```

同样地，readXXX全部委托给read方法，以下是readXXX和其他剩余方法的实现。

public class ByteArraySeekableFile implements SeekableFile { // 定位 public void seek(long position) throws IOException { checkPosition(position); this.position = (int) position; } public int readInt() throws IOException { byte[] buffer = new byte[4]; read(buffer); return Ints.fromByteArray(buffer); } public long readLong() throws IOException { byte[] buffer = new byte[8]; read(buffer); return Longs.fromByteArray(buffer); } public int read(byte[] b) throws IOException { int n = Math.min(b.length, size - position); if $(n > 0)$ { System.arraycopy(content, position, b, 0, n); position += n; } return n; } // 获取大小 public long size() throws IOException { return size; } // 裁剪 public void truncate(long size) throws IOException { if (size < 0) { throw new IllegalArgumentException("size < 0"); } this.size = (int) size; if (position > this.size) {

```java
position = this.size;
}
// 获取从指定位置开始的输入流
public InputStream inputStream(long start) throws IOException {
    checkPosition(start);
    return newByteArrayInputStream(content, (int) start, (int) (size - start));
}
// 获取当前位置
public long position() {
    return position;
}
// 刷新
public void flush() throws IOException {
}
// 关闭
public void close() throws IOException {
} 
```

基于 ByteArraySeekableFile 的 EntriesFile 测试代码如下。

```java
public class EntriesFileTest { // 测试追加日志条目 @Test public void testAppendEntry() throws IOException { ByteArraySeekableFile seekableFile = new ByteArraySeekableFile(); EntriesFile file = new EntriesFile.seekableFile); Assert assertEquals(0L, file.appendEntry(new NoOpEntry(2, 3)); seekableFile.seek(0); Assert assertEquals Entry.KIND_NO_OP, seekableFile.readInt()); Assert assertEquals(2, seekableFile.readInt()); // index Assert assertEquals(3, seekableFile.readInt()); // term Assert assertEquals(0, seekableFile.readInt()); // command bytes length byte[] commandBytes = "test".getBytes(); Assert assertEquals(16L, file.appendEntry(new GeneralEntry(3, 3, commandBytes)); seekableFile.seek(16L); Assert assertEquals Entry.KIND_general, seekableFile.readInt()); Assert assertEquals(3, seekableFile.readInt()); // index Assert assertEquals(3, seekableFile.readInt()); // term 
```

Assert.assertEquals(4,seekableFile.readInt());//command bytes   
length byte[] buffer $=$ new byte[4]; seekableFile.read(buffer); Assert.assertArrayEquals commandBytes,buffer); } //测试加载日志条目 @Test public void testLoadEntry() throws IOException{ ByteArraySeekableFile seekableFile $\equiv$ new ByteArraySeekableFile(); EntriesFile file $\equiv$ new EntriesFile(seekableFile); Assert.assertEquals(OL,file.appendEntry(new NoOpEntry(2,3)); Assert.assertEquals(16L,file.appendEntry(new GeneralEntry(3,3, "test".getBytes()))); Assert.assertEquals(36L,file.appendEntry(new GeneralEntry(4,3, "foo".getBytes()))); EntryFactory factory $\equiv$ new EntryFactory(); Entry entry $\equiv$ file.loadEntry(OL,factory); Assert.assertEquals Entry.KIND_NO_OP,entry.getKind(); Assert.assertEquals(2，entry_Index()); Assert.assertEquals(3，entry.Term()); entry $\equiv$ file.loadEntry(36L,factory); Assert.assertEquals Entry.KIND_general，entry.getKind(); Assert.assertEquals(4，entry_Index()); Assert.assertEquals(3，entry.Term()); Assert.assertArrayEquals("foo".getBytes)，entry.getCommandBytes()); 1 //测试裁剪 @Test public void testTruncate()throws IOException{ ByteArraySeekableFile seekableFile $\equiv$ new ByteArraySeekableFile(); EntriesFile file $\equiv$ new EntriesFile(seekableFile); file.appendEntry(new NoOpEntry(2,3)); Assert.assertTrue(seekableFile.size()>0); file.truncate(OL); Assert.assertEquals(OL,seekableFile.size());

基于 ByteArraySeekableFile 的 EntryIndexFile 测试代码如下。

```txt
public class EntryIndexFileTest { 
```

//构造文件内容  
```java
private ByteArraySeekableFile makeEntryIndexFileContent( int minEntryIndex, int maxEntryIndex) throws IOException { ByteArraySeekableFile seekableFile = new ByteArraySeekableFile(); seekableFile.writeInt(minEntryIndex); seekableFile.writeInt(maxEntryIndex); for (int i = minEntryIndex; i <= maxEntryIndex; i++) { seekableFile.writeLong(10L * i); // offset seekableFile.writeInt(1); // kind seekableFile.writeInt(i); // term } seekableFile.seek(0L); return seekableFile; } //测试加载 @Test public void testLoad() throws IOException { ByteArraySeekableFile seekableFile = makeEntryIndexFileContent(3, 4); EntryIndexFile file = new EntryIndexFile(seekableFile); Assert.assertEquals(3, file.getMinEntryIndex()); Assert.assertEquals(4, file.getMaxEntryIndex()); Assert.assertEquals(2, file.getEntryIndexCount()); EntryIndexItem item = file.get(3); Assert.assertNotNull(item); Assert.assertEquals(30L, item.getOffset()); Assert.assertEquals(1, item.getKind()); Assert.assertEquals(3, item.getTerm()); item = file.get(4); Assert.assertNotNull(item); Assert.assertEquals(40L, item.getOffset()); Assert.assertEquals(1, item.getKind()); Assert.assertEquals(4, item.getTerm()); } 
```

//测试追加  
```java
@Test
public void testAppendEntryIndex() throws IOException {
   ByteArraySeekableFile seekableFile = newByteArraySeekableFile();
    EntryIndexFile file = new EntryIndexFile(seekableFile);
    file.appendEntryIndex(10, 100L, 1, 2);
    Assert.assertEquals(1, file.getEntryIndexCount());
    Assert.assertEquals(10, file.getMinEntryIndex());
    Assert.assertEquals(10, file.getMaxEntryIndex()); 
```

```java
seekableFile.seek(0L); Assert assertEquals(10, seekableFile.readInt()); // min entry index Assert assertEquals(10, seekableFile.readInt()); // max entry index Assert assertEquals(100L, seekableFile.readLong()); // offset Assert assertEquals(1, seekableFile.readInt()); // kind Assert assertEquals(2, seekableFile.readInt()); // term EntryIndexItem item = file.get(10); Assert.assertNotNull(item); Assert.assertEquals(100L, item.getOffset()); Assert.assertEquals(1, item.getKind()); Assert.assertEquals(2, item.getTerm()); file.appendEntryIndex(11, 200L, 1, 2); Assert.assertEquals(2, file.getEntryIndexCount()); Assert.assertEquals(10, file.getMinEntryIndex()); Assert.assertEquals(11, file.getMaxEntryIndex()); seekableFile.seek(24L); // skip min/max and first entry index Assert.assertEquals(200L, seekableFile.readLong()); // offset Assert.assertEquals(1, seekableFile.readInt()); // kind Assert.assertEquals(2, seekableFile.readInt()); // t