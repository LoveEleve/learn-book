上一章我们已经把 Broker 节点向 Namesrv 注册信息的完整功能给实现了，这一章就应该对 Namesrv 进行重构，把 Nmaesrv 接收 Broker 节点注册信息的完整功能给实现了。当然，之前我们已经为 Nmaesrv 简单实现了该功能，但 Namesrv 只能接收 Broker 节点注册过来的配置信息，无法保存 Broker 节点注册过来的主题信息。那这一章就让我们完善 Namesrv 节点的该功能吧。  
  
实现 Broker 和 Namesrv 节点的心跳检测功能  
  
在开始重构 Namesrv 接收 Broker 节点注册信息功能之前，我想先为大家补充 Namesrv 模块的另一个功能，那就是 Namesrv 和 Broker 节点的心跳检测功能。我记得在之前的文章中跟大家说过，在 Broker 模块中定义的两个定时任务： 一个是用来让 Broker 节点定期获得最新可用的 Namesrv 地址集合；另一个就是让 Broker 节点把自己的信息定期发送给 Namesrv 节点，这么做有两个作用，既可以让 Namesrv 获取 Broker 节点最新的信息，又可以以此来维持 Broker 和 Namesrv 建立连接的心跳检测 。 如果 Namesrv 节点在规定时间内没有接收到 Broker 节点发送过来的信息，那就可以认为和这个 Broker 节点建立的连接出现问题了，那 Namesrv 节点就可以不必再维护这个 Broker 注册过来的信息了 。我之前肯定跟大家提到过这样的内容，但是并没有为 Namesrv 实现对应的功能，原因很简单，因为那个时候我们还没有为 Broker 节点实现注册信息到 Namesrv 的功能，这个功能没有实现，那 Namesrv 节点就不可能定期接收 Broker 节点的信息，更不可能保存 Broker 节点的信息了，所以心跳检测的功能也就没办法实现。但现在我们已经把 Broker 定期注册信息到 Namesrv 的完整功能都实现了，Namesrv 也可以接收并保存 Broker 节点的信息了，那接下来我们就可以先把二者心跳检测的功能给实现了。  
  
我实现的思路非常简单，之前不是给 Namesrv 定义了一个 RouteInfoManager 路由信息管理器吗？Namesrv 节点接收到的所有 Broker 节点信息都存储在这个 RouteInfoManager 管理器中，就像下面展示的这样，请看下面代码块。  
package org.apache.rocketmq.namesrv.routeinfo;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/10

\* @方法描述：路由信息管理器，这是Namesrv模块最核心的一个组件，用于管理主题路由信息，包括broker地址、集群地址、broker活跃信息、过滤服务器信息等。

\*/

public class RouteInfoManager {  
  
private static final Logger log \= LoggerFactory.getLogger (LoggerName.NAMESRV\_LOGGER\_NAME);  
//这个成员存储的是BrokerName和BrokerData的映射关系，其实就是存储了Broker集群和集群中所有Broker信息的键值对

//因为同一个Broker集群中的Broker节点的名称是相同的，所以这里使用BrokerName作为key

private final Map < String /\* brokerName \*/,BrokerData > brokerAddrTable;  
//这个成员变量存储了集群名称和集群中所有Broker节点名称的映射关系

private final Map < String /\* clusterName \*/,Set < String /\* brokerName \*/ >> clusterAddrTable;  
private final NamesrvController namesrvController;

private final NamesrvConfig namesrvConfig;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/10

\* @方法描述：注册Broker节点信息到路由信息管理器中的方法

\*/

final String clusterName,

final String brokerAddr,

可以看到，Broker 节点的所有信息都会以键值对的方式存储在 brokerAddrTable 成员变量中，通过 Broker 节点的名称就可以从 brokerAddrTable 成员变量中的到对应的 BrokerData 对象，而 BrokerData 对象存储的就是一个 Broker 主从架构组中所有节点的信息。这些旧知识大家肯定都还有印象。我之所以又提到这个 RouteInfoManager 路由信息管理器，以及它的 brokerAddrTable 成员变量，是因为 RouteInfoManager 成员变量确实把 Broker 节点注册过来的信息都保存在自己内部了。现在我们不是要实现 Namesrv 和 Broker 的心跳检测功能吗？ 如果在 Namesrv 的 RouteInfoManager 路由信息管理器保存 Broker 节点信息时，把本次注册过来的 Broker 信息的时间也保存一下，那么 Namesrv 就可以定期检查所有 Broker 信息的最新注册时间和系统当前时间的差值是不是超过了规定的阈值，如果超过了，就可以认为这个 Broker 节点心跳发送超时了，那么这个 Broker 节点的网络连接就可能出问题了 。  
  
就比如说有一个 Broker 节点把自己的信息注册给 Namesrv 节点了，Namesrv 在保存这个 Broker 节点信息的时候，把当前系统时间也保存在一起，这也就意味着 Namesrv 更新了这个 Broker 节点最新更新自己信息的时间，如果 Namesrv 定义的 Broker 节点心跳信息的超时时间为 5 秒，那么 Namesrv 就可以定期扫描自己接收到的 Broker 节点的信息，看看这个 Broker 节点最新更新的时间和当前系统时间的差值是否超过 5 秒了，如果超过了 5 秒，就意味着这个 Broker 节点已经超过 5 秒没有更新自己的信息了，也就意味着有 5 秒没和 Namesrv 进行通信了，这个时候就可以判断这个 Broker 节点心跳超时，网络连接出现问题了。我相信这个逻辑大家都可以理解吧？这些心跳检测的功能已经在很多框架中见过很多次了，所以这对大家来说并不是什么难点。  
  
那真正的难点是什么呢？这就是我接下来想请大家思考的问题： 我们应该让 Namesrv 怎么存储 Broker 节点每次注册信息的时间呢 ？难道要把 Broker 节点每次注册信息的时间存储到 RouteInfoManager 类的 brokerAddrTable 成员变量中？也就是存储在 brokerAddrTable 的 value 中？让每一个 BrokerData 对象存储每一个 Broker 节点更新信息的时间，这样做合理吗？显然是不合理的，原因很简单， Broker 节点注册自己信息到 Namesrv 的行为是无关主从架构组，甚至是无关集群的 。 也就是说在一个 Broker 集群中，不管有多少主从组，每一个组的 Broker 主节点和从节点都会把自己的信息注册给 Namesrv 节点 ，这一点从过去实现的功能代码中都能得到印证！而 BrokerData 对象存储的是一个 Broker 组的节点信息，还不能具体到每一个个体节点，难道一个 Broker 组中的从节点和 Namesrv 心跳检测失败了，Namesrv 就要把这个 Broker 组中的所有主从节点的信息都删除吗？显然不能这么做。所以，如 果 Namesrv 要存储每一个 Broker 节点最新注册信息的时间，肯定要精确到每一个节点的信息，而在一个 Broker 集群中，每一个主从组中的主节点和从节点的名称是一样的，只有节点 Id 和网络地址不一样 ，如果是这样，那我们就可以定义一个 BrokerAddrInfo 类，这个类中可以定义两个成员变量，分别是 clusterName 和 brokerAddr，表示 Broker 节点所在集群的名称和这个 Broker 节点的网络地址。就像下面代码块展示的这样，请看下面代码块。  
请大家想一想，是不是仅仅凭借 clusterName 和 brokerAddr 就可以迅速在 BrokerData 中定义到一个节点？ 先根据 BrokerAddrInfo 对象的 clusterName 成员变量就可以找到对应集群下的 BrokerData 对象，再根据 brokerAddr 就可以从 BrokerData 的 brokerAddrs 成员变量中获取对应节点的 Id，这个时候这个节点的 clusterName，brokerName，brokerId，brokerAddr 也都确认了 。我知道有的朋友会说，为什么不把一个 Broker 节点的 brokerName 和 brokerId 也都定义成 BrokerAddrInfo 的成员变量呢？这个也很好解释， 就 Broker 节点的信息而言，你在其它类中定义的和 Broker 相关信息越多，你需要维护的信息就越多，操作也就更繁琐，而且就算你真的这么做了，当你使用一个 BrokerAddrInfo 对象去 brokerAddrTable 成员变量中确定唯一 Broker 节点信息时，最后不管怎么对比，总会对比到节点的 brokerAddr 网络地址。因为只有网络地址是唯一的，两台机器不可能共享一个网络地址 。所以只对比 clusterName 和 brokerAddr 就能确定一个唯一的 Broker 节点，显然就不再需要让 BrokerAddrInfo 类封装更多信息了。大家可以仔细品味品味这个逻辑。  
  
好了，如果大家理解了以上逻辑，那就可以接着往下看了。我知道现在大家都还不知道我定义这个 BrokerAddrInfo 类的作用，别着急，很快我就会为大家解释。 现在我们已经可以通过一个 BrokerAddrInfo 对象从存储了所有 Broker 节点信息的 brokerAddrTable 成员变量中定位到一个唯一的 Broker 节点，这就意味着假如一个 BrokerAddrInfo 对象包装的网络地址和 Namesrv 节点心跳检测失败了，那我就可以直接根据这个 BrokerAddrInfo 对象从 brokerAddrTable 中找到对应的 Broker 节点，然后从 brokerAddrTable 中移除这个 Broker 节点的信息即可 。这样不就实现了 Namesrv 心跳检测失败后移除对应 Broker 节点信息的功能吗？这个逻辑不难理解吧？很好，如果大家理解了这个逻辑，那接下来就可以思考另一个问题，把心跳检测失败的 Broker 节点的信息从 brokerAddrTable 中移除了之后呢？这样就完了吗？  
  
仅仅是这么做显然还不够，心跳检测失败之后，就意味着对应的 Broker 节点网络出现问题了，这个时候 Namesrv 就没必要再维护和这个 Broker 建立的网络连接了，也就是所谓的 Channel，所以这个时候还要执行关闭对应 Channel 的操作。这个大家也都能理解吧？那现在问题来了 ，当 Namesrv 检测到一个 Broker 节点和自己失联之后，怎么得到和这个 Broker 建立的 Channel 连接，然后关闭呢 ？还有最重要的一点， 那就是 Namesrv 怎么判断这个 Broker 节点心跳检测失败了呢 ？这个时候就轮到另一个新的对象登场了，也就是 BrokerLiveInfo 对象登场，请看下面代码块。  
从上面代码块中可以看到我定义了一个新的 BrokerLiveInfo 类，这个类中定义了很多成员变量。 如果 Namesrv 每次接收到 Broker 节点注册过来的信息时，把 Broker 注册当前信息的时间，以及这个 Broker 节点心跳超时时间，还有和这个 Broker 节点对应的 Channel 等等数据都封装到一个 BrokerLiveInfo 对象中，然后再让 Namesrv 的 RouteInfoManager 存储这个 BrokerLiveInfo 对象，Namesrv 不就可以定期从 RouteInfoManager 中判断有没有 Broker 节点心跳超时了 ？接下来就让我为大家展示一下具体的代码，请看下面代码块。  
  
首先是 Namesrv 接收请求的请求处理器，Broker 节点发送过来的信息会在请求处理器中被接收处理，请看下面代码块。  
现在 RouteInfoManager 对象已经可以在自己的 registerBroker() 方法中得到 Broker 节点的 Channel 了，接下来请大家看看 RouteInfoManager 对象在 registerBroker() 方法中具体执行了什么新的操作，请看下面代码块。  
从上面代码块中可以看到，我 让 Namesrv 节点每次接收 Broker 节点信息的时候，创建了一个 BrokerAddrInfo 对象和 BrokerLiveInfo 对象，并且把这两个对象以键值对的方式存储在 RouteInfoManager 类新定义的 brokerLiveTable 成员变量中 。BrokerAddrInfo 对象可以定位注册到 Namesrv 中的唯一的 Broker 节点信息，BrokerLiveInfo 封装这与这个 Broker 节点对应的网络连接和信息最新的刷新时间。 如果我再给 Namesrv 定义一个定时任务，让定时任务定期扫描 RouteInfoManager 路由信息管理器的 brokerLiveTable 成员变量，也就是 Broker 节点存活表，根据存货表的 value 对象，也就是 BrokerLiveInfo 对象的最新刷新时间判断有没有哪个 Broker 心跳超时了，如果超时了就根据与 value 对应的 key，也就是 BrokerAddrInfo 对象去 brokerLiveTable 成员变量中查找具体的 Broker 节点信息，然后移除这个节点的信息，同时关闭这个 Broker 节点的 Channel，这样就做到了真正的资源释放 。到此为止，刚才提出的那两个问题就被解决了，当然，我肯定还得把 Namesrv 中定期扫描 Broker 节点存活表的定时任务定义出来，这个我已经实现了，并且在 NamesrvController 中启动，请看下面代码块。  
到此为止，我就把 Namesrv 和 Broker 节点心跳检测的功能实现完毕了。可以看到，虽然我们上面展示了很多代码块，代码块中也展示了很多代码，但实际上和心跳检测功能相关的代码并没有多少，这个功能实现起来非常简单。当然，心跳检测之后释放资源的代码确实比较多一些，但这部分内容我又没在文章中展示，那大家在阅读我提供的第七版本代码时还是要花费一些精力的。我能说的就是代码逻辑非常简单，只是需要大家保持足够的耐心和耐性去阅读。好了，这一部分的内容就到此为止吧，接下来就该回到本章的正题，那就是实现 Namesrv 接收 Broker 注册的主题信息功能。  
  
实现 Namesrv 接收 Broker 主题信息功能  
  
要实现 Namesrv 接收 Broker 主题信息功能，我还是那句话，这个功能实现起来非常简单，可以说这个小节只需要分析一个问题即可， 那就是 Namesrv 应该以什么形式存储接收到的 Broker 节点主题信息呢 ？当然，现在我们先不讨论这个问题，我们先把讨论这个问题的前置工作都做好吧。现在我们都清楚了，Namesrv 是在 DefaultRequestProcessor 请求处理器中接收 Broker 节点发送过来的信息，那么肯定也是在这个请求处理器中得到了 Broker 节点发送过来的主题信息，那接下来我们就先看看这个 DefaultRequestProcessor 请求处理器是怎么处理接收到的主题信息的，请看下面代码块。  
从上面代码块中可以看到，Broker 发送过来的所有信息，也就是配置信息和主题信息，都注册到 RouteInfoManager 对象中了，这就意味着 Broker 节点的主题信息也由 RouteInfoManager 对象管理，这显然是理所当然的，因为 RouteInfoManager 的名称就是路由信息管理器，而路由信息不就是从 Broker 节点的主题信息中得到吗？哪个 Broker 节点包含指定主题，就意味着可以被路由给客户端。很好，那接下来我们就可以直接去 RouteInfoManager 路由信息管理器中，也就是这个 RouteInfoManager 对象的 registerBroker() 方法中看看这个 RouteInfoManager 管理器是怎么处理并保存 Broke 主题信息的。  
  
这个时候，我们就可以回过头看看我刚才提出的问题： 那就是 Namesrv 应该以什么形式存储接收到的 Broker 节点主题信息呢 ？ 我们都知道 RouteInfoManager 在它的 registerBroker() 方法中执行的操作肯定很简单，无非就是把主题信息保存到内部的一个数据结构中，大概率是个 Map 。这些操作我不说大家也能想到， 但问题的关键是 Broker 节点的主题信息该怎么有条理地存储？要是定义一个 Map 存储这些主题信息，那 Map 的 key 是什么？value 又该是什么呢 ？这个问题就要仔细分析分析了。  
  
请大家思考一个场景，假如现在有一个 Broker 集群，集群名称为 Test，集群中有三个主从架构组，集群中各个节点的信息如下，请看下面代码块。  
从上面代码块中可以看到，一个集群中的三个 Broker 主从组的全部信息，每一个主从组中 Broker 节点的名称都不相同，但是主从组内部所有节点的名称是相同的。这些内容大家都能看到，除此之外我们还知道， Admin 控制台向 Broker 节点创建主题信息时，是向 Broker 主从组中的主节点创建的，从节点会从主节点同步主节点的主题信息，这样一来，同一个 Broker 主从组中主从 Broker 节点的主题信息应该是相同的 。 也就是说， 只要是同一个主从组中的 Broker 节点，注册到 Namesrv 中的主题信息都是相同的 。  
  
那现在出现这样一种情况，Admin 控制台向上面 Test 集群的三个 Broker 主从组的 Broker 主节点分别创建了相同的主题信息，主题名称为 TestTopic，然后给 broker-a 创建的主题配置信息为 8 个写队列，8 个读队列，权限为读写权限；给 broker-b 创建的主题配置信息为 4 个写队列，4 个读队列，权限为读写权限；给 broker-c 创建的主题配置信息为 8 个写队列，8 个读队列，权限为读写权限；如果我们定义一个新的对象，也就是 QueueData 对象，让这个对象封装一个 Broker 节点的主题配置信息，那这个 QueueData 对象可以定义成下面这样，请看下面代码块。  
好了，这个 QueueData 类定义完毕之后，那 Admin 为上面三个 Broker 主节点创建的主题信息就可以写成下面这样，请看下面代码块。  
好了，随着上面代码块的展示，现在我们已经可以把同一个集群不同主从组的 Broker 节点的主题信息收集到一起了。现在的情况是，broker-a，broker-b，broker-c 拥有相同的主题，只不过是主题配置信息有一点点不同， 如果有客户端来查询指定主题的路由信息，那 Namesrv 是不是应该把 broker-a，broker-b，broker-c 三个主从组中节点的信息都返回给客户端，拥有同一主题的 Broker 节点都是可以被路由的 。这一点也可以理解吧？那如果我们现在定义这样一个 Map，请看下面代码块。  
因为同一个主从组中的主节点和从节点的主题信息是相同的，那上面这个嵌套 Map 是不是就把同一主题下，同一个集群中的所有 Broker 的主题配置信息都收集到一起了 ？ 如果客户端要查询指定主题的路由信息，那么我就可以根据主题信息先从上面这个 Map 中得到所有可以路由的 Broker 节点的名称，并且还可以得到这个 Broker 节点所在主从组的主题配置信息，也就是 QueueData 对象。然后我就可以使用 Broker 节点的名称直接从 RouteInfoManager 对象的 brokerAddrTable 成员变量中查询具体的 Broker 主从组的全部节点信息。然后把所有的 Broker 节点信息和对应的主题配置信息返回给客户端就行了 。这个逻辑大家一定要梳理清楚，要仔细品味品味。  
  
好了，上面的逻辑分析完了，那接下来我就可以告诉大家，实际上在 RouteInfoManager 类中有一个新的成员变量，就是下面代码块展示的这样，请看下面代码块。  
而 RouteInfoManager 对象在自己的 registerBroker() 方法中所做的，就是把 Broker 节点注册过来的主题信息存储到 topicQueueTable 成员变量中 。接下来就让我给大家把 RouteInfoManager 的 registerBroker() 方法重构之后的样子展示给大家，请看下面代码块。  
上面代码块的内容非常多，大家可以仔细阅读阅读，或者直接结合文章阅读我提供的第七版本代码即可，我就不再重复解释代码中的逻辑了，毕竟之前已经分析过了。虽然这些代码看起来内容不少，但都是处理数据的琐碎逻辑，基本上都是对数据进行校验、判重、判空、保存的操作，也没什么技术含量，大家自己看看就行。  
  
当然，阅读到这里我相信很多朋友心里都会有一个疑问，那就是 RouteInfoManager 路由信息管理器中的 brokerAddrTable 成员变量似乎定义的有些问题，这个成员变量结构如下，请看下面代码块。  
定义这个 brokerAddrTable 成员变量的作用很简单，那就是使用 Broker 节点的名称就可以从这个成员变量中获得该节点所在主从组中的所有信息，因为同一个集群中不同主从组的主节点名称是不同的，这一点大家肯定都很熟了。 那不同的 Broker 集群中，两个 Broker 主从组的 Broker 主节点的名称相同了呢 ？ 比如说集群一中有一个名称为 broker-a 的 Broker 主节点，集群二中也有一个这样的主节点，然后这两个节点把自己的信息都注册到同一个 Namesrv 中了，那这种情况下，brokerAddrTable 成员变量中应该存储哪一个集群的 Broker 主从组所有节点的信息呢 ？这个时候是不是就出问题了？所以，这种情况不能出现， 也就是说两个 Broker 集群，注册了相同的 Broker 节点名称到同一个 Namesrv 中的情况不能出现 ，这一点大家要理解，因为源码中就是这么设计的。  
  
好了，这一点解释完毕之后，也许大家会疑惑我为什么在这里突然提起这个，接下来就让我来为大家解释一下， 因为在 RocketMq 源码中，当 Proxy 客户端访问 Namesrv，想从 Namesrv 获得指定主题的路由信息时，Namesrv 就会把这个主题的配置信息，也就是所有的 QueueData 以及与这些 QueueData 对应的所有 Broker 节点都返回给 Proxy 客户端。也就是说，Namesrv 并不会只返回 Broker 主节点的信息，而是会把主从组中主从节点的信息都返回给客户端 。这一点是不是也令大家感到困惑了？按照我们对其他框架集群的理解， 通常情况下只有主节点才参与框架业务活动，只有主节点才拥有主动写入消息和读取消息的能力，一般来说从节点并不参与业务工作，只负责备份数据 。但现在 Namesrv 要把可以路由的所有 Broker 主从节点的信息都返回给客户端，而在 RocketMq 的设计理念中，实际上也是只有 Broker 主节点参与框架业务活动，那 Namesrv 把从节点的信息也返回给客户端了，这又是怎么回事呢？大家可以先记住这个问题，等后面两个版本代码，我就会为大家解答这个问题了。  
  
说了这么做，最后就让我给大家重构一下 Namesrv 的 ClientRequestProcessor 请求处理器，给大家看看 Namesrv 究竟是怎么把指定主题的路由信息返回给客户端的吧。  
  
重构 Namesrv 的 ClientRequestProcessor 请求处理器，实现返回指定主题路由信息的功能  
  
我们已经知道了 Namesev 的 ClientRequestProcessor 请求处理器会处理来自客户端的 GET\_ROUTEINFO\_BY\_TOPIC 请求，而这个 ClientRequestProcessor 请求处理器的内容一直没有被完善，现在我们终于可以重构它了。我把重构完毕的 ClientRequestProcessor 请求处理器展示在下面代码块中了，请看下面代码块。  
到此为止，我们就把 Namesrv 发挥客户端指定路由信息的功能也实现了，实现的逻辑跟我们之前分析的一模一样。这些功能都实现了，本章内容也就结束了，这个时候，大家已经可以阅读我提供的第七版本代码的所有内容了。可以看到，本章内容还是非常多的，大家可以结合文章仔细阅读代码，一定要把这一章展示的存储主题信息，以及返回主题信息给客户端的各种数据结构梳理清楚，因为下一个版本代码还会用到它们。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/drbny8ug7u67txhi*  
*All content belongs to its respective owners and creators.*