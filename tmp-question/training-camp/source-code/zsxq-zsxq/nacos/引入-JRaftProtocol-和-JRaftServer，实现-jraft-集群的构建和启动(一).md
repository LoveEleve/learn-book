  
引入 RaftConfig 配置类  
  
上一章我已经为大家引入 nacos 服务端的 Raft 集群要使用的状态机组件，也就是 NacosStateMachine 类，我把该类的内容搬运过来了，帮助大家简单回顾一下，请看下面代码块。  
package com.cqfy.nacos.core.distributed.raft;  
  
/\*\*

\* @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。

\* @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。

\* @Date:2024/8/27

\* @Description:jraft集群组应用日志的状态机

\*/

class NacosStateMachine extends StateMachineAdapter {  
  
//表示当前节点是否为领导者

private final AtomicBoolean isLeader \= new AtomicBoolean (false);  
//当前状态机要被使用的集群的集群组Id

private final String groupId;  
//当前节点

private Node node;  
//节点当前任期

private volatile long term \= \- 1;  
//集群组当前领导者IP地址

private volatile String leaderIp \= "unknown";  
//当前集群组处理数据的数据处理器

protected final RequestProcessor processor;  
  
//构造方法

NacosStateMachine (RequestProcessor processor) {

this.processor \= processor;

this.groupId \= processor.group ();

除了 nacos 要使用的状态机组件，我还为大家引入了 nacos 服务端各个数据类型对应的数据处理器组件，也就是 RequestProcessor 抽象类，当然，我并没有把各个数据处理器都实现了，而是只实现了处理持久化服务实例信息的数据处理器，也就是 PersistentClientOperationServiceImpl 类。我把该类的内容也搬运过来了，请看下面代码块。  
在我们为 nacos 服务端的每一种数据构建 Raft 集群时，会为这些数据构建不同的 Raft 集群组，使用 sofajraft 框架构建即可。在构建每一个集群组的时候，还会为每个集群组都创建一个 NacosStateMachine 状态机对象，与此同时还会把各个集群组要使用的数据处理器对象创建出来，交给 NacosStateMachine 状态机对象使用。这样一来，每一个 Raft 集群组的状态机就可以处理自己的数据了 。  
  
好了，关于为 nacos 构建的 Raft 集群组要使用的状态机就分析到这里吧，接下来就可以正式为 nacos 服务端构建 Raft 集群组了。这时候我们就可以按照集群工作流程来分析了，首先要把集群中各个节点的信息收集完毕，然后根据集群节点的各个信息构建 Raft 集群，然后启动 Raft 集群，接着是领导者选举等等工作。其实分析到这里大家应该也能意识到， 使用 jraft 框架构建 Raft 集群是一件非常简单省事的工作，因为我们只需要把 Raft 集群各个节点的信息交给 jraft 框架就行了，剩下的一切工作 jraft 框架都会为我们完成 。当然，通过之前对 jraft 框架的学习，大家应该也都清楚 jraft 框架在构建 Raft 集群的过程中做了什么。 所以我们现在要做的就是把要构建为 Raft 集群的各个服务器的信息收集起来即可，也就是获得 Raft 集群配置信息 。  
  
这就很好办了， 比如说我们就可以先定义一个 RaftConfig 类，这个类就是 jraft 框架构建 Raft 集群的配置信息类，这个类的对象中封装了 Raft 集群中所有节点的信息 ，可以定义成下面这样，请看下面代码块。  
好了，现在 RaftConfig 类定义完毕了，并且我刚才也跟大家说了， 这个类的对象中封装了 Raft 集群中所有节点的信息，当我们要使用 jraft 框架构建 Raft 集群时，就可以把这个 RaftConfig 配置类的对象交给 jraft 框架使用 。这一点理解起来并没有什么难度，但是我们刚才的问题还没有解决呢？究竟该怎么收集 Raft 集群所有节点的信息呢？只有把这些节点信息收集起来，交给 RaftConfig 配置类的对象保管，jraft 才能使用 RaftConfig 对象构建并启动 Raft 集群呀。  
  
这个时候就让我们来回顾一下 Distro 集群的节点信息是怎么收集的吧，这个很简单， 我们会在 nacos 服务器中定义一个本地文件，也就是 cluster.conf 集群配置文件，这个集群配置文件中就定义了 Distro 集群所有节点的信息 ，这个大家应该还记得吧？ 当我们构建 Distro 集群的时候，会先启动集群节点寻址器，在我为大家提供的代码中，我实现的是 FileConfigMemberLookup 文件寻址器，这个寻址器一旦开始工作，就会从本地的 cluster.conf 集群配置文件中把所有节点信息加载到内存中，最后把收集到的节点信息交给 ServerMemberManager 集群成员管理器来管理 。这个流程想必大家都还记得吧？如果有朋友忘记了可以回顾一下之前的文章，我就不在本章中展示具体的代码了。总之， 当 Distro 集群构建完毕之后，该集群的所有节点信息都会保存在 ServerMemberManager 集群成员管理器中 。  
  
很好，这一点清楚了之后，那么请大家想一想， Distro 集群的节点信息是不是就是 nacos 的 Raft 集群的节点信息 ？假如我们启动了 5 台 nacos 服务器，这 5 台服务器是一个 Distro 集群，Distro 集群用来备份 nacos 服务端的临时服务实例信息，现在要为 nacos 服务端构建 Raft 集群了，用 Raft 集群来备份持久服务实例信息，难道这个时候 nacos 服务器就会减少到三台了吗？肯定不会这样吧？仍然是 5 台 nacos 服务器，我们要用这 5 台 nacos 服务器构建 Raft 集群。如果是这样的话， 那直接从 ServerMemberManager 集群成员管理器中得到 Distro 集群成员的信息，然后把这些信息交给 RaftConfig 对象来管理，这样不就得到了要构建的 Raft 集群的配置信息了吗 ？这个逻辑并不难理解吧？ 所以现在我们要做的就是 ServerMemberManager 集群成员管理器中获得所有成员的信息，然后把这些信息交给 RaftConfig 对象来保管，这样 jraft 框架就可以使用 RaftConfig 配置对象成功创建 Raft 集群了 。那这个操作该怎么实现呢？这个时候我劝大家先别急，先让我们思考另一个问题， 那就是当我们使用 RaftConfig 对象成功收集到了 Raft 集群的配置信息之后，怎么把它交给 jraft 框架使用呢 ？  
  
引入 JRaftProtocol 和 JRaftServer 组件  
  
我在文章中为大家反复强调过了，在 jraft 框架的帮助下，nacos 服务端 Raft 集群的构建变得异常简单，所以我想先把构建 Raft 集群时用到所有组件先定义出来，然后直接把它们组装一下，这些组件就可以协同工作了。而我在上一小节提出的问题： 也就是当我们使用 RaftConfig 对象成功收集到了 Raft 集群的配置信息之后，怎么把它交给 jraft 框架使用 ？这个问题就会 nacos 服务端引出两个非常重要的 Raft 集群组件。  
  
请大家想一想，如果我们要在 nacos 中使用 jraft 框架，应该怎么使用？一般来说是不是应该定义一个和 jraft 框架相关的组件，把 jraft 框架提供的服务封装在这个组件中，然后 nacos 直接通过这个组件使用 jraft 提供的功能，就像在 nacos 中使用 grpc 框架，也是定义了 grpc 的服务组件，这个可以理解吧？ 就比如说我们可以定义一个 JRaftServer 类，在这个类中定义 jraft 框架构建 Raft 集群，启动 Raft 集群的功能，除了这两个功能，还可以定义 jraft 框架处理客户端请求的功能，比如说读请求或者写请求，这个也可以理解吧？总之我们定义的这个 JRaftServer 类就是纯粹的提供 jraft 框架功能的组件 。  
  
如果 JRaftServer 组件的作用大家都清楚了，也都知道这个组件要创建并启动 Raft 集群，那么这个 JRaftServer 组件是不是肯定就要用到 RaftConfig 配置信息对象了？因为 Raft 集群配置信息都封装在这个对象中，而 jraft 框架只有得到集群配置信息之后，才能构建集群，启动集群， 所以这个 JRaftServer 组件显然就要获得 RaftConfig 配置信息对象 。就目前分析的情况来说，这个 JRaftServer 组件可以先简单定义成下面这样，请看下面代码块。  
可以看到，在上面展示的 JRaftServer 组件中，定义了几个 Raft 集群处理数据要用到的方法，还把 jraft 框架构建 Raft 集群的关键对象，也就是 conf 成员变量定义出来了。而且从上面代码块中我们还可以看到，在 JRaftServer 组件初始化的时候，也就是调用了该组件的 init() 方法，就会把一个封装了 Raft 集群配置信息的 RaftConfig 对象赋值给 JRaftServer 的 raftConfig 成员变量。这样一来，等到调用了 JRaftServer 组件的 start() 方法，真正启动 JRaftServer 服务组件时，就可以从 raftConfig 成员变量中解析出 Raft 集群的每一个节点的信息，然后设置到 conf 成员变量中了。当 jraft 构建的 Raft 集群启动时，就会从 conf 成员变量中获得集群当前的配置信息。这也就是上面代码块中 createMultiRaftGroup() 方法的逻辑，该方法就是用来构建 Raft 集群组并启动每一个集群组的，代码块中的注释非常详细，所以我就不再赘述了，如果大家看得比较仔细，通过以上代码块，应该就能清楚 nacos 服务端 Raft 集群组具体的构建流程了。大家可以仔细品味品味这个流程。  
  
当然，上面的代码块看完之后，我相信还会有朋友感到困惑，因为我现在为大家展示的 JRaftServer 组件根本没有定义多少和 jraft 框架有关的东西，这个我自己也知道，我不定义太多和 jraft 框架相关的组件是有原因的：首先我们本章的核心是使用 jraft 框架为 nacos 服务端构建 Raft 集群，这个是我们的目标，如果我现在就把 JRaftServer 中用到的 jraft 框架的各种组件都定义出来，然后一一分析讲解，这不就变成带领大家回顾 jraft 框架的知识了吗？这就偏离主题了。第二个原因就是 JRaftServer 类中定义的和 jraft 框架相关的组件确实有点多，我无法在文章中全部展示给大家，就比如说 jraft 框架要用到的 RpcServer 集群节点服务器，还有 CliService 集群客户端，以及用来得到和定期刷新 Raft 集群领导者的 RouteTable 路由表组件等等。这些我都没有定义出来，而这些组件的作用非常重要，就比如说nacos 服务端 Raft 集群的某个节点接收了一个写请求指令，写请求指令只能被集群领导者处理，如果当前节点不是领导者，肯定需要从 RouteTable 路由表组件中找到集群的领导者，然后把写请求转发给领导者，让领导者处理。这些都是我后来为大家补充的 sofajraft 框架第十三版本代码对应的内容，如果大家忘记了，可以去 sofajraft 课程中回顾回顾对应的文章和代码。总之，还是我之前那句话，大家一定要熟练掌握 jraft 框架之后再来阅读本章的内容，这时候大家会觉得本章内容非常简单。  
  
好了，分析完 JRaftServer 组件内容简陋的原因之后，接下来让我们再来分析， JRaftServer 组件的 init() 方法应该在什么时候被调用 。因为经过刚才的分析，我们都已经知道了，只有 JRaftServer 对象的 init() 方法被调用了，封装了 Raft 集群所有节点信息的 RaftConfig 配置对象才能交给 JRaftServer 组件使用，JRaftServer 组件才能在 createMultiRaftGroup() 方法中为每一种数据构建 Raft 集群组，然后启动集群。当然，我还是要再多说一句， 在 JRaftServer 对象的 createMultiRaftGroup() 中，在开始遍历封装了各个数据处理器的 processors 成员变量时，这里大家肯定会对 processors 集合中的数据是怎么添加的感到疑惑？我在 JRaftServer 类中并没有展示任何能把 nacos 服务端的所有数据处理器都添加到 processors 成员变量中代码和方法，那这是怎么回事呢 ？还会我写在代码注释中的那句话，请大家一定要记住这个问题，最后我就会解决它。好了，言归正传，还是让我们继续分析JRaftServer 组件的 init() 方法应该在什么时候被调用吧， 其实这时候就要引入另一个组件了，也就是标题中展示的 JRaftProtocol 类 。  
  
其实这个 JRaftProtocol 组件存在的原因非常简单，就像 Distro 集群拥有一个用来负责 DIstro 协议层面操作的 DistroProtocol 组件一样，nacos 的 Raft 集群也有一个用来负责 Raft 协议层面操作的组件。注意， 我这里说的用来负责 Raft 协议层面操作指的并不是和 jraft 框架提供的关于 Raft 协议的相关操作，如果是这样的话直接使用 JRaftServer 组件不就完了 ？ 这里的 Raft 协议层面的操作指的是 nacos 业务层面和 Raft 协议层面衔接的一些操作，也就是说我们新定义的这个 JRaftProtocol 组件要在 nacos 的业务层面被使用，而这个 JRaftProtocol 组件被使用之后，程序就会从 nacos 业务层面转到了 Raft 集群层面了 。光这么说肯定让大家有些模糊，所以接下来我直接把 JRaftProtocol 类的代码给大家展示一下，大家就清楚了，请看下面代码块。  
通过以上代码块可以看到， JRaftProtocol 对象持有了 JRaftServer 对象，这样一来，nacos 业务层面的操作就可以通过 JRaftProtocol 对象交给 Raft 层面的 JRaftServer 组件去执行，而且我们还可以看到在 JRaftProtocol 类中也定义了一个 RaftConfig 成员变量，以及一个 init() 方法，并且也是 init() 方法被调用之后，JRaftProtocol 对象的 RaftConfig 成员变量就会被赋值了 ； 同时在 init() 方法中，还会对 JRaftServer 组件进行初始化操作，然后启动 JRaftServer 服务，开始构建 Raft 集群并且启动 Raft 集群 。那现在问题就变成了 JRaftProtocol 对象的 init() 方法在什么时候被调用呢？只有这个 JRaftProtocol 对象的 init() 方法被调用了，JRaftProtocol 才会获得封装了 Raft 集群配置信息的 RaftConfig 对象，然后 JRaftServer 才能获得对应的 RaftConfig 对象，Raft 集群才能被顺利构建和启动。这一连串的流程都要依靠这个 RaftConfig 对象，那接下来，我就为大家展示一下 JRaftProtocol 对象是如何被创建的，又是在什么时候获得了 RaftConfig 对象。 这时候我就不卖关子了，直接引入 ProtocolManager 协议管理器对象，在 ProtocolManager 协议管理器对象中创建 JRaftProtocol 对象，然后从 ServerMemberManager 中获得 Raft 集群需要的配置信息，封装到 RaftConfig 对象中，再把 RaftConfig 对象交给 JRaftProtocol 对象使用 。具体内容请看下面代码块。  
上面代码块中的内容非常详细，只要大家是按照方法顺序来阅读的，肯定就知道 JRaftProtocol 对象是怎么被创建的，也知道 JRaftProtocol 对象的 init() 方法是在哪里被调用的了。代码块中的注释非常详细，所以我就不再跟大家重复梳理逻辑了。总之， 只要在 ProtocolManager 的 initCPProtocol() 方法中调用 JRaftProtocol 对象的 init() 方法，程序就会像我们之前分析的那样运行，nacos 服务端各个数据的 Raft 集群组也就会被构建成功了。而 ProtocolManager 的 initCPProtocol() 方法又会在该类的 getCpProtocol() 方法中被调用，所以现在我们只需要知道该类的 getCpProtocol() 方法在什么时候被调用就行了 。这时候就需要重构一下处理持久服务实例信息的数据处理器了，也就是对 PersistentClientOperationServiceImpl 类进行重构。  
  
根据之前我为大家展示的 PersistentClientOperationServiceImpl 类的代码块，大家可以知道该类上面添加了 springboot 的 @Component 注解，这也就意味着它的对象会被 springboot 容器管理，会被 springboot 自动创建。实际上，在该类中还定义了一个无参构造方法，在该方法中执行了一些重要操作，具体实现请看下面代码块。  
从上面代码块中可以看到， 当 PersistentClientOperationServiceImpl 类的对象被 springboot 容器创建的时候，就会执行无参构造方法，而在执行无参构造方法的时候，就会从 springboot 容器中得到 ProtocolManager 对象，调用该对象的 getCpProtocol() 方法；紧接着还会把自己这个持久的服务实例信息处理器添加到 JRaftServer 对象的 processors 成员变量中 。现在大家应该清楚数据处理器是怎么添加到 JRaftServer 组件的 processors 成员变量中了吧？ 在本章中我只为大家展示了持久服务实例信息的数据处理器，其实在其他数据的处理器中也是这么做的，它们也都被 springboot 容器管理，也会在自己的无参构造方法中执行上面代码块的构造方法中的操作，都是一样的，都是通过这种方式把自己添加到 JRaftServer 对象的 processors 成员变量中 。到此为止，本章展示的所有组件的逻辑就全都串联起来了，nacos 服务端的 Raft 集群也就构建完毕了，本章的内容也就到此为止了。  
  
当然，目前我们只是把 Raft 集群构建完毕了，可能有很多构建过程中的细节大家都还不清楚，这没关系，只要大家完全掌握了 sofajraft 框架，那么大家就可以直接去看我提供的第十二版本代码了，代码中注释非常详细，我相信大家都能看懂其中的逻辑。当然，有些方法可能大家现在还不太明白怎么使用，比如 JRaftProtocol 对象的向 Raft 集群写入数据的 write() 方法，还有从 Raft 集群中读取数据的方法，这些大家可能都还不清楚怎么使用。因为我们虽然在本章成功构建并启动了 Raft 集群，但是并没有给大家提供一个 nacos 服务端 Raft 集群工作的具体例子，这没关系，下一章我就会为大家实现 nacos 客户端向服务端注册持久服务实例的功能了，到时候大家就知道持久服务实例的 Raft 集群组在 nacos 服务端究竟是怎么工作的了。好了朋友们，这一章就到此为止吧，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/dgrf5ixpglawnri3*  
*All content belongs to its respective owners and creators.*