  
实现 Broker 模块的 registerBrokerAll() 方法  
  
上一章我们几乎把 Broker 模块的核心功能都实现了，就剩下最后一个功能没有完全实现，那就是 Broker 注册信息到 Namesrv 节点的功能，实际上也就是其内部的 registerBrokerAll() 方法没有实现。我在上一章结尾给大家的解释是这个 registerBrokerAll() 方法实现起来非常简单，但这个方法把信息注册到 Namesrv 之后，Namesrv 要保存这些信息的功能实现起来有些复杂，所以这些内容就都放到本章来讲解。当然，我这里说得 Broker 模块的 registerBrokerAll() 方法指的并不只是一个方法，而是 BrokerOuterAPI 类中的 registerBrokerAll() 方法和 BrokerController 类中的 registerBrokerAll() 方法。那接下来我们就一起实现这些方法吧。  
  
既然 Broker 要把自己的信息注册到 Namesrv 节点中，那我们肯定得知道 Broker 内部的哪些信息需要注册到 Namesrv 中，从前面章节的分析我们可以知道，Broker 需要把自己本身的信息和内部创建的主题信息都注册到 Namesrv 节点中，但在第五版本代码中，我们并没有实现 Broker 模块创建主题信息功能，所以在目前的 Broker 模块中，我们只需要把 Broker 自身的信息，也就是定义在配置文件中的信息收集起来，然后注册到 Namesrv 节点即可。这就很容易实现了， Broker 自身的信息无非就是什么所在集群名称，节点名称，Id 等等一系列信息，直接从 BrokerConfig 对象中获取即可，我们可以在 BrokerController 类的 registerBrokerAll() 方法中把 Broker 的这些信息从 BrokerConfig 配置对象中收集了，然后再调用 BrokerOuterAPI 对象的 registerBrokerAll() 方法把这些信息注册到 Namesrv 节点 。所以 BrokerController 类的 registerBrokerAll() 方法可以定义成下面这样，请看下面代码块。  
package org.apache.rocketmq.broker;  
  
//这个代码块的每一个方法都重构了，大家可以仔细看看  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/9

\* @方法描述：Broker模块的控制器，该控制器负责管理Broker的各个组件和服务，包括网络通信、定时任务、配置管理等。

\*/

public class BrokerController {  
  
//省略该类的其他内容  
//Broker对外发送请求的API组件

protected BrokerOuterAPI brokerOuterAPI;  
//Broker模块的配置信息对象

protected final BrokerConfig brokerConfig;  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/9

\* @方法描述：启动Broker开始工作的方法，该方法也重构了

\*/

public void start () throws Exception {  
//启动Broker组件对外发送请求的API组件

if (this.brokerOuterAPI!= null) {

this.brokerOuterAPI.start ();

}  
//启动Broker模块的基础服务

从上面代码块中可以看到 BrokerController 类的 doRegisterBrokerAll() 方法被重构了，里面多了很多内容， 在该方法中调用了 BrokerOuterAPI 对象的 registerBrokerAll() 方法，而在调用该方法的时候，就把 Broker 节点的内置信息都收集到了，然后传递到了 BrokerOuterAPI 对象的 registerBrokerAll() 方法中 。在代码中我把注释写得非常详细，收集到的每一个 Broker 信息都解释得非常清楚，所以我就不再重复解释了。当然，有些要点我确实需要补充一下，那就是大家会看到在 BrokerController 类的 doRegisterBrokerAll() 方法中有两个方法参数，一个是 checkOrderConfig，另一个是 oneway，其中 oneway 参数的作用非常容易理解，那就是决定本次请求是否为单项请求的标志，这个没什么可说的；而 checkOrderConfig 参数的作用就需要简单解释一下了，但解释的最佳时机并不是现在，所以我希望大家先记住这个方法参数，其实还有 BrokerController 类的 registerBrokerAll() 方法的第三个方法参数 forceRegister，大家现在可以先把这两个方法参数记住，留下印象，等后面版本代码就会真的用到这两个参数了，到时候我再为大家解释它们的作用。  
  
而除了我刚才介绍的两个方法参数， 大家肯定还在 BrokerController 类的 doRegisterBrokerAll() 方法中注意到了，当调用了 BrokerOuterAPI 对象的 registerBrokerAll() 方法后，把消息注册到 Namesrv 节点之后会得到一个响应结果，这个结果就是 registerBrokerResultList 集合 。这里我也要简单解释一下，实际上是这样的， Broker 节点把自己的信息注册到 Namesrv 节点之后，Namesrv 节点会把当前注册过来的 Broker 节点所在集群的主节点网络地址，以及主节点高可用网络地址，还有主题的配置信息(假如说用户定义的某个主题被设置成顺序主题了，也就是这个主题下的消息都要顺序消费，那这个顺序性就是主题的配置信息)都返回给这个 Broker 节点。当 Broker 节点接收到这些信息后会把这些信息封装到 RegisterBrokerResult 对象中，然后返回到 BrokerController 类的 doRegisterBrokerAll() 方法中 。至于为什么 BrokerController 类的 doRegisterBrokerAll() 方法接收到的是一个 RegisterBrokerResult 集合，这是因为 Broker 节点要把自己的信息发送给每一个 Namesrv 节点，既然是这样，那每一个 Namesrv 节点都会给当前 Broker 节点返回信息，所以最后会得到一个 RegisterBrokerResult 集合。说了这么多，接下来我就给大家展示一下这个 RegisterBrokerResult 类的内容，请看下面代码块。  
可以看到，RegisterBrokerResult 类的内容还是很简单的，代码注释中写得都很清楚，我就不再对其进行讲解了。当 Broker 节点接收到了多个 Namesrv 节点返回的 RegisterBrokerResult 集合后，肯定要把主节点最新的信息和主题配置信息更新到自己内部，也就是处理 RegisterBrokerResult 集合中的信息。但这个功能我还没有实现，所以大家会看到 BrokerController 类的 handleRegisterBrokerResult() 方法还是一个伪实现，大家也不必着急，这些方法到后面都会完善，我们还是接着往下看吧。  
  
现在我们已经知道了， 要在BrokerController 类的 doRegisterBrokerAll() 方法中调用 BrokerOuterAPI 对象的 registerBrokerAll() 方法，把收集到的所有 Broker 信息发送给 Namesrv 节点，所以现在我们就要来到 BrokerOuterAPI 类中，把该类的 registerBrokerAll() 方法实现了。那接下来的操作就很容易实现了， 无非就是在 BrokerOuterAPI 类的 registerBrokerAll() 方法中得到所有可用的 Namesrv 地址集合，然后遍历地址集合，使用 RemotingClient 客户端把 Broker 的消息发送给每一个 Namesrv 节点即可 。当然，这些消息总得封装起来吧，这也不难， 现在要发送的就是注册 Broker 信息的请求，那就为该请求定义一个对应的请求头对象，比如说就定义成 RegisterBrokerRequestHeader 对象，然后把 Broker 的信息封装到这个请求头对象中，再把请求头对象编码到 RemotingCommand 对象中，把 RemotingCommand 消息对象发送给 Namesrv 节点即可 。这个 RegisterBrokerRequestHeader 请求头我也定义完毕了，请看下面代码块。  
接下来就是真正实现的 BrokerOuterAPI 类的 registerBrokerAll() 方法，请看下面代码块。  
上面代码块中的注释非常详细，我就不再重复讲解了，这些逻辑我都已经为大家提前分析过了。到此为止，我们就把 Broker 节点注册信息到 Namesrv 的功能实现了。可以看到确实非常简单，接下来我们就该来到 Namesev 中看看 Namesrv 接收到 Broker 节点注册的信息之后，应该执行什么操做吧。  
  
定义 Namesrv 的 RouteInfoManager 管理器  
  
现在要对 Namesrv 模块进行重构，实现 Namesrv 模块保存并管理 Broker 注册过来的信息的功能。要实现一个新的功能，总得有一个起点吧？就比如说 Namesrv 模块要向保存并管理 Broker 注册过来的信息，首先就要得到 Broker 注册过来的信息，这就肯定是在请求处理器中执行的操作了。因为 Broker 的信息会通过请求发送给 Namesrv 节点，Namesrv 节点只有在处理并解析请求的时候才能从请求中得到 Broker 注册过来的信息。所以，这个时候 Namesrv 模块中的两个请求处理器就成了我们目前的关注点。我们已经知道了，在 Namesrv 模块中存在两个请求处理器，一个是 ClientRequestProcessor 请求处理器，另一个是 DefaultRequestProcessor 请求处理器。 ClientRequestProcessor 的作用我已经给大家解释过了，那就是专门处理根据主题获取路由信息请求的，而 DefaultRequestProcessor 请求处理器则处理 Namesrv 模块接收到的其他所有请求，所以 Broker 发送过来的 REGISTER\_BROKER 类型的请求显然应该交给 DefaultRequestProcessor 请求处理器处理 。处理的方式可以非常简单， 比如我们可以为 Namesrv 模块定义一个 RouteInfoManager 路由信息管理器，Broker 的信息其实就是可以被路由的信息，只要让 DefaultRequestProcessor 得到这个 RouteInfoManager 路由信息管理器，然后在处理请求的时候，把从请求中得到的 Broker 的信息交给 RouteInfoManager 管理器管理即可 ，当然，别忘了最后回复给 Broker 节点响应。现在还是纯理论分析阶段，这个 RouteInfoManager 管理器可以先不实现，只定义成一个空架子，可以定义成下面这样，请看下面代码块。  
然后可以在 NamesrvController 类中把 RouteInfoManager 创建出来，就像下面这样，请看下面代码块。  
好了，现在知道 RouteInfoManager 对象是怎么被创建出来的了， 也知道 DefaultRequestProcessor 请求处理器中持有了 NamesrvController 对象的引用，那么 DefaultRequestProcessor 请求处理器就可以通过 NamesrvController 对象得到这个 RouteInfoManager 管理器对象 ，然后直接使用即可。所以这个之前一直没有实现的 DefaultRequestProcessor 请求处理器对象可以定义成下面这样，请看下面代码块。  
从上面代码块中可以看到，DefaultRequestProcessor 请求处理器在 registerBroker() 方法中执行的操作非常简单，那就是直接把从请求中得到的 Broker 的信息交给了 RouteInfoManager 对象，调用该对象的 registerBroker() 方法来管理这些信息。而信息注册成功之后，DefaultRequestProcessor 请求处理器把响应回复给了 Broker 节点。这些逻辑应该非常清楚了吧？我就不再重复解释了，到此为止，我们的目光就应该聚焦在 RouteInfoManager 路由管理器上了，应该真正把这个 RouteInfoManager 管理器定义出来，然后实现它的 registerBroker() 方法真正保存 Broker 节点注册过来的信息。那这个 RouteInfoManager 管理器究竟该怎么定义呢？  
  
其实到这个时候反而是最简单的情况，要做的工作也最轻松，因为已经来到了我们最熟悉的领域，那就是信息的保存与管理。保存管理信息的本质是什么？不就是把一些数据存储在 Map 或者 List 中吗？哪个客户端需要就把对应的信息返回出去，在 SofaJraft、Nacos 框架中我们已经做了大量的重复性工作，尤其是在 Nacos 框架中，Nacos 服务端管理注册过来的服务实例信息，然后再把信息返回给对应的客户端，这些操作大家应该都很熟悉了。说到底这不就是信息的转移与获取吗，在这个过程中保证信息安全即可，说实话，我已经对这些功能感到疲倦了。当然，疲倦并不能成为我懈怠的理由，我仍然会为大家仔细分析一下，Namesrv 的 RouteInfoManager 路由信息管理器存储 Broker 信息的方式。  
  
请大家集合自己使用 RocketMq 的经验思考一下，如果我们构建了 Broker 集群，那么这个集群会以什么模式存在呢？比如说我们构建了一个名称为 Test 的集群，集群中有一个主节点，两个从节点，而主节点和从节点的 Broker 名称都是相同的，并且它们所在集群的名称也都是相同的，都是 Test，但它们的 Broker-Id 是不同的，主节点的 Broker-Id 最小，一般都是 0，从节点的 Id 从 1 开始递增。当然，每一个节点的网络地址都是不同的。程序启动之后，这三个节点要把自己的信息都注册到 Namesrv 节点中，那这些信息应该以什么样的方式存储呢？  
  
定义一个 Map 吗？这是我首先想到的数据结构，Map 的 key 应该是唯一的，既然是这样，那么显然 Broker-Id 应该成为 Map 的 key，那么 value 呢？value 肯定要存储每一个 Broker 节点所在集群名称，Broker 节点的名称，以及网络地址，这么一来肯定要定义一个新的对象来封装这些信息，比如就定义一个 BrokerData 对象，这个 BrokerData 对象封装 Broker 的信息，然后以 Broker-Id —— BrokerData 键值对的形式把信息存储到我们定义好的 Map 中，这样可以吧？逻辑上确实没问题，但我们考虑得情况太简单了，假如现在 Test 集群中并不是只有一个主节点呢？假如集群中存在两个主节点，它们的节点名称分别是 Broker-a 和 Broker-b，这两个主节点各自有两个从节点，格子从节点的名称分别是 Broker-a 和 Broker-b。这些都很容易理解，但现在我想强调的是，一个集群中存在两个 Broker 组，也就是两组 Broker 主从模式节点。因为这两组 Broker 主从节点的主节点的名称已经不同了，所以就不必再 Broker-Id 上再区分它们了，这也就是说，Broker-a 主节点的 Id 可以是 0， Broker-b 的主节点也可以是 0，以此类推，不同 Broker 组的从节点的 Id 也可能相同，也就是下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到，不同 Broker 组的 Broker-Id 可能是相同的，所以现在 Broker-Id 显然不能再作为 Map 的 key 了，那这个时候该怎么办呢？我想大家已经意识到了，不管有多少个 Broker 组，这些 Broker 组的节点名称肯定是不同的，既然这样， 那我就使用 Broker 节点的名称作为 Map 的 key，value 仍然是 BrokerData 对象 。这个可以理解吧？当然，这个时候我对 BrokerData 也做了一些改变， 因为我发现在一组 Broker 中，主从节点有很多信息都是相同的，所以我想让这个 BrokerData 存储一组 Broker 的信息，也就是主从节点的全部信息全封装到一个 BrokerData 对象中，而 key 是这组 Broker 的节点的名称，这个名称是唯一的，这样一来我们不就可以只凭借一个 Broker 节点名称从 Map 中得到 Broker 组的全部信息了吗 ？分析到这里，我已经在脑海中把 BrokerData 的具体内容勾勒出来了，请看下面代码块。  
那么 RouteInfoManager 中应该定义一个 BrokerName —— BrokerData 键值对模式的 Map，请看下面代码块。  
很好，我们已经迈出了实现 RouteInfoManager 管理器的第一步，当然， 如果你想让这个 RouteInfoManager 存储的信息更丰富，更灵活，那还可以定义一个 Map，这个 Map 的 key 是程序中存在的集群的名称，而 value 就是集群中所有 Broker 组的节点名称，这样一来，你就可以快速知道某个集群中有哪些 Broker 组了 ，这样分析下来，这个 RouteInfoManager 又可以简单重构成下面这样，请看下面代码块。  
好了，现在 RouteInfoManager 管理器存储信息的数据结构定义完毕了，这就意味着最难的难点已经被我们攻克了，接下来就可以一路平推了，按照常规思维， 假如一个 Broker 节点把自己的信息注册到了 Namesrv 节点中，在 RouteInfoManager 对象的 registerBroker() 方法中，首先可以判断 clusterAddrTable 是否存在注册信息过来的 Broker 节点所在集群的名称信息，如果不存在则把对应的信息存储到 clusterAddrTable 成员变量中。接着就可以判断 brokerAddrTable 成员变量中是否存在对应的 BrokerData 信息，如果这是 Broker 节点所在的组第一次把信息注册给 Namesrv，那么 brokerAddrTable 中显然不会存储对应的 BrokerData 信息，这个时候就要创建对应的 BrokerData 对象，存储对应的信息。当然，如果 brokerAddrTable 成员变量中存在对应的 BrokerData 信息，这就意味着 Broker 节点所在组的信息早就注册到 Namesrv 中了，这次的注册操作就要判断当前注册信息过来的 Broker 节点信息是否更新了 ，那在程序运行期间， Broker 节点的什么信息会发生变更呢？这个问题就留给大家去代码块中寻找吧，答案非常简单。接下来就让我把重构完毕的 RouteInfoManager 管理器的 registerBroker() 方法展示给大家，请看下面代码块。  
好了朋友们，到此为止我就把 Namesrv 模块保存 Broker 信息的功能实现完毕了，这个功能实现起来并没有那么难吧？不仅这个功能不难，剩下两个带实现的功能，也就是 Namesrv 对 Broker 模块执行心跳检测功能和 Namesrv 信息路由功能实现起来都不难，这些功能我都会为大家实现。现在大家已经可以阅读我提供的第五版本所有代码了，下一章我会为大家先实现了 Broker 创建主题信息功能，好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/kuogmvp4hvg96oag*  
*All content belongs to its respective owners and creators.*