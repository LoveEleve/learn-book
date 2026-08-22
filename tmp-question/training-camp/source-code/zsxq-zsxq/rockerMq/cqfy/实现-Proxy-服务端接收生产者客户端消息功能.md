  
完善 MessageQueueView 类，引入消息队列概念  
  
我不知道大家是否还对我提供的第八版本代码仍有印象，毕竟距离更新第八版本代码对应的文章已经过去两个月了，如果大家忘记了第八版本代码的内容，那接下来就让我帮助大家简单回顾一下。当然，我能想到肯定有朋友对我忽然提起第八版本代码的内容感到困惑，请大家先按耐住内心的想法，耐心看下去。我记得很清楚，在第八版本代码中，我为 Proxy 服务端实现了从 Namesrv 节点查询并缓存主题路由信息功能。 我记得当时给 Proxy 服务端定义了一个 TopicRouteService 类，还为这个 TopicRouteService 类定义了 LocalTopicRouteService 子类。在 TopicRouteService 类的构造方法中，实现了 Proxy 从 Namesrv 节点查询并缓存主题路由信息功能；而在 LocalTopicRouteService 类中则定义了 getTopicRouteForProxy() 方法，该方法可以从 Proxy 缓存的路由信息中获取指定主题的路由信息 。也许大家已经把这部分的内容忘了，接下来，就让我为大家展示一下相关的代码。  
  
首先是 TopicRouteService 类的部分内容，请看下面代码块。  
package org.apache.rocketmq.proxy.service.route;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/22

\* @方法描述：根据主题名称获取消息队列的服务组件

\*/

public abstract class TopicRouteService extends AbstractStartAndShutdown {  
  
//MQ客户端API工厂实例，用于提供MQ客户端API

private final MQClientAPIFactory mqClientAPIFactory;  
//Proxy模块专门缓存从Namesrv获取的主题路由信息

protected final LoadingCache < String /\* topicName \*/,MessageQueueView > topicCache;  
//这个成员变量在第八版本代码中也没用上

protected final ScheduledExecutorService scheduledExecutorService;  
//用于刷新topicCache缓存的线程池

protected final ThreadPoolExecutor cacheRefreshExecutor;  
  
//构造方法

public TopicRouteService (MQClientAPIFactory mqClientAPIFactory) {

//得到Proxy模块的配置信息

ProxyConfig config \= ConfigurationManager.getProxyConfig ();

//创建定时任务执行器，虽然在第八版本代码中没用到这个执行器

this.scheduledExecutorService \= ThreadUtils.newSingleThreadScheduledExecutor (

new ThreadFactoryImpl ("TopicRouteService\_")

);  
//创建缓存刷新执行器，用于刷新topicCache缓存

从上面代码块中可以看到，只要当 TopicRouteService 对象被创建的时候，就会在构造方法中创建出来 topicCache 对象，而这个对象就会用来缓存从 Namesrv 节点查询到的指定主题路由信息。 至于 Proxy 节点什么时候去 Namesrv 中查询，这一点我在注释中也解释得很清楚，就是在客户端第一次向 Proxy 服务端查询指定主题路由信息时，如果 Proxy 的 topicCache 缓存没有命中，Proxy 则会直接去 Namesrv 节点查询指定主题路由信息，然后缓存得到的主题路由信息，再返回给客户端对象 。这个逻辑大家应该都很清楚了。  
  
好了，TopicRouteService 类的核心内容展示完毕之后，接下来我再给大家展示一下 LocalTopicRouteService 类的部分内容，请看下面代码块。  
现在 LocalTopicRouteService 类的内容也展示完毕了，可以看到， 在 LocalTopicRouteService 类中定义的 getTopicRouteForProxy() 方法就可以返回指定主题下的所有路由信息，而在该方法中会调用父类的 getAllMessageQueueView() 方法，从父类的 topicCache 成员变量中获取指定主题的路由信息 。这些逻辑大家应该都很熟悉了。当然，回顾这些内容并不是本章重点，我带领大家回顾这些旧知识主要是为了引入一些新的内容，当大家回顾完了之前的内容之后，请大家再看一看 TopicRouteService 代码块中的内容，在 TopicRouteService 类中，当 Proxy 去 Namesrv 节点查询并获取了指定主题路由信息之后，会把路由信息缓存到 topicCache 成员变量中，而在缓存路由信息的时候，会把路由信息封装在一个 MessageQueueView 对象中，作为 topicCache 的 value 被保存起来；这个对象就是在 TopicRouteService 类代码块的第 67 行代码被创建出来的，也就是被 buildMessageQueueView() 方法创建的 。接下来我就把 buildMessageQueueView() 方法展示一下，请看下面代码块。  
从上面代码块中可以看到， Proxy 服务端在处理从 Namesrv 获得的指定主题路由信息时，会把路由信息封装到一个 MessageQueueView 对象中，这也就意味着，每一个 MessageQueueView 对象都封装着一个特定主题的所有路由信息 。那么使用 MessageQueueView 对象是怎么封装指定主题的路由信息的呢？很好，绕了一大圈，终于讲到这个 MessageQueueView 类了。其实我在第八版本代码中已经为大家引入了这个 MessageQueueView 类，但当时引入的这个类并不完整，内容有很多残缺，当时引入的 MessageQueueView 类是这样的，请看下面代码块。  
从上面代码块中可以看到，MessageQueueView 类的内容并不完整，我把很多内容注释掉了，因此 Proxy 服务端得到指定主题的路由信息之后，只把路由信息封装到了 TopicRouteWrapper 对象中，该对象的内容我就不会为大家展示了，内容很简单，如果全都展示那就太浪费文章篇幅了。好了，看到这里大家一定能猜到我接下来要做什么了，肯定是把注释的内容放开，真正实现 MessageQueueView 类。没错，这就是我要做的，我相信大家在使用 RocketMq 框架的过程中，多少都了解一点 RocketMq 框架存储消息的原理： 那就是在启动 RocketMq 程序之前，一定要给 Broker 节点定义好配置信息，创建好主题信息，以及该主题拥有什么权限，以及这些权限分别对应多少队列。如果一个 Broker 节点拥有一个 Test 主题，该主题也拥有读写权限，并且读写权限的队列都是八，这就意味着在这个 Broker 节点的 Test 主题下可以存在八个写队列来存储生产者客户端发送过来的消息，与之对应的是可以存在八个读队列来让消费者客户端消费消息，通常来说，读写队列的数量是相等的 。这些概念大家多少应该清楚一些， 而为一个 Broker 节点的主题定义多个读写队列，在大家的认识中肯定会觉得这么做能够提高程序的运行消息，比如多个写队列不仅可以使多个生产者在并发情况下并发写入消息，而多个消费者队列，则可以让多个消费者并发消费消息 ，我相信每一位朋友都有这种认知，我们先不讨论这种认知是否正确，就先当它是正确的，等后面程序更加完善了，看看后面编写的代码能否为这种认知提供有力支持。  
  
我们现在要考虑的是写队列和读队列该如何实现，原因很简单，现在已经实现了生产者客户端生产和发送消息的功能，那接下来肯定就要实现存储消息功能。而根据我们刚才的分析，在存储消息的时候，消息要被发送到写队列当中存储起来，而消费消息的时候要从读队列中获取消息，但现在读写队列都没有实现呢，所以接下来应该先把读写队列实现了。当然，我也能想到现在有很多朋友已经在考虑， 既然消息是被写入到写队列中，那消费者又是怎么从读队列中得到消息的呢？换句话说，写队列中的消息是怎么传递给读队列的呢 ？请大家先把这些问题放到一边，后面实现具体功能的时候，我们再详细讨论。  
  
好了，让我们言归正传开始实现读写队列吧。在开始实现读写队列之前，让我们先明确一个点，应该为每一个 Broker 节点的主题都实现都写队列吗？显然不必这么做， 因为在 Broker 集群中，只有 Broker 集群主节点真正参与业务活动，也就是说只有 Broker 集群的主节点才能接收消息、被消费者消费消息，所以只给 Broker 集群组中的主节点实现读写队列即可，如果多个 Broker 主从组拥有相同的主题信息，那么就要给多个 Broker 主节点都实现读写队列 。那接下来就好说了， 我可以直接定义一个 MessageQueue 类，这个类就是消息队列的意思，因为不管是写入消息还是读取消息，都是从队列中获得的，那就直接定义一个消息队列，这个消息队列既可以代表读写列，也可以代表写队列，说到底消息队列就是一个媒介，生产者客户端可以把消息发送给消息队列存储起来，消费者客户端可以从消息队列的到消息消费 。大家可以品味品味这个逻辑，如果这个逻辑理解了，那接下来我们就要思考这个消息队列应该定义什么内容了。我对 MessageQueue 类的定义思考得非常简单，只想给它定义三个成员变量， 分别是该队列所属的主题信息，该队列所属的 Broker 节点信息以及该队列的队列 Id ，这三个信息可以说是消息队列最核心的信息了，这样分析下来，MessageQueue 消息队列可以定义成下面这样，请看下面代码块。  
我知道这时候肯定有朋友会问，消息队列不是用来存储消息和消费消息的吗？你定义的这个消息队列根本就没有存储消息的容器啊！没错，确实是这样，接下来就让我再解释一下，实际上是这样的： 我们都知道生产者生产的消息肯定会发送给 Broker 节点，Broker 节点把消息转交给存储引擎存储，但现在我们要实现的是 Proxy 接收生产者客户端发送过来的消息，然后把消息转发给 Broker 节点的功能。那 Proxy 节点在转发消息的时候要怎么做呢？是不是首先要找到接收消息的目标 Broker 节点？Proxy 节点接收到生产者生产的消息后，肯定会根据这条消息所属主题信息得到该主题下的所有路由信息，然后从一堆路由信息中找到目标 Broker 节点 ，这个逻辑大家可以理解吧？那 Proxy 要怎么该主题下的所有路由信息中找到目标 Broker 节点呢？并且这个目标 Broker 节点还是集群中的主节点，这该怎么实现呢？  
  
在 RocketMq 源码中执行的操作非常简单： 那就是当 Proxy 节点从 Namesrv 得到指定主题的所有路由信息后，就会从这些路由信息中筛选出所有的主节点，然后遍历这些主节点，根据主节点中该主题的配置信息，为遍历到的主节点创建出所有的写队列和读队列，其实就是根据队列数量创建对应的 MessageQueue 消息队列 。当然， 这个时候消息队列中也都包含了主节点的网络地址 。到此为止，Proxy 就得到了指定主题可以路由的所有 Broker 主节点信息，并且也把 Broker 节点的读写队列创建完毕了。也就是说， 一个主题下所有的写队列都创建完毕了，并且每个写队列都包含队列所属的主节点的网络地址 ， 如果现在 Proxy 节点要把生产者的消息转发给一个 Broker 主节点，那就可以直接找到消息所属主题下的所有写队列，然后根据负载均衡策略从所有写队列中选择一个目标队列，目标队列中又包含队列所属 Broker 主节点的信息，这样不就可以直接把消息转发给目标 Broker 节点了吗 ？RocketMq 源码就是这么做的，而且要发送给 Broker 节点内部的写队列也确定了，也知道写队列 Id。这样一来，当目标 Broker 节点接收到消息，把消息交给存储引擎存储时，也许存储引擎就会把消息存储在自己内部的消息队列中，因为这个时候要存储的写队列的 Id 已经确定了，对吧？也许就是这么做的，等后面的功能实现完毕了，到时候直接验证即可，当然，也可能不是这么做的，到时候自见分晓。对我们来说，怎样存储消息还不是当前的重点，重点是 Proxy 怎么把消息发送给 Broker 节点，而发送的核心逻辑我已经剖析完毕了，并且就是仿照 RocketMq 源码来分析的，所以接下来我们只需要仿照 RocketMq 源码继续完善程序即可。  
  
现在我们已经把 MessageQueue 消息队列定义完毕了，只不过和 RocketMq 源码比起来，这个消息队列还没有封装队列所属 Broker 主节点的网络信息，这个很好办，我直接在 MessageQueue 类中再定义一个成员变量，表示队列所属 Broker 主节点的网络地址即可。当然， 我们也可以仿照 RocketMq 源码，再定义一个 AddressableMessageQueue 类，让这个类封装 MessageQueue 消息队列对象，然后再持有队列所属 Broker 主节点的网络地址 。就像下面代码块展示的这样，请看下面代码块。  
好了，当 AddressableMessageQueue 类定义完毕之后，现在我们就清楚了，当 Proxy 为可路由的 Broker 节点创建读写队列时，不必再直接创建 MessageQueue 消息队列对象了，而是创建 AddressableMessageQueue 对象即可，从名字上就能看出来，这个类的对象就是可寻址消息队列的意思，能够提供队列所属 Broker 主节点的网络信息。好了，这一切分析完毕，接下来就可以实现新的功能，那就是让 Proxy 为可路由的 Broker 节点创建读写队列功能，并且还要把都写队列各自的选择器创建了，那这个功能该怎么实现呢？这就要回到 MessageQueueView 类中了。  
  
之前我们已经分析过了， Proxy 节点从 Namesrv 服务端得到指定主题的路由信息后，会把这些信息封装到 MessageQueueView 对象中，也就是说每一个 MessageQueueView 对象都对应一个主题的所有路由信息，知道根据主题信息得到了对应的 MessageQueueView 对象，那就可以从该对象中得到对应的路由信息 。如果是这样的话， 那我就直接让 MessageQueueView 对象持有读写队列的队列选择器，而读写队列选择器则持有各自的全部可路由队列 。这样一来， 只要得到了 MessageQueueView 对象，就可以直接使用写队列选择器直接选择具体的目标消息队列，然后把消息发送给消息队列所属的 Broker 主节点即可 。 这个逻辑非常清晰吧？如果大家理解了这个逻辑，那就可以看看 MessageQueueView 类中被注释掉的内容，是不是恰好是两个队列选择器，一个是 MessageQueueSelector 写队列选择器，一个是 MessageQueueSelector 读队列选择器，请看下面代码块。  
到此为止，我就终于把读写队列选择器引入进来了，这个可以说是本章的核心内容。好了，现在读写队列选择器有了，那读写队列选择器怎么持有各自的可路由队列信息呢？在 MessageQueueView 类的构造方法中就给出了问题的答案，可以看到，在构造方法中就直接使用 topicRouteWrapper 成员变量构建了读写队列选择器，而 topicRouteWrapper 成员变量拥有指定主题下的所有路由信息。至于队列选择器的构建过程也很简单， 那就是先判断要构建的是读队列还是写队列，然后再筛选路由信息，得到路由信息中的所有 Broker 主节点，接着再为所有主节点创建队列即可 。接下来就请大家看一下 MessageQueueSelector 类的具体内容，请看下面代码块。  
上面代码块中的注释非常详细，我就不再重复讲解了，到此为止，MessageQueueSelector 队列选择器的内容就全部展示完毕了，功能也齐全了。这也意味着本章的核心内容讲解完毕了，当然，我知道本章我们最重要实现的是 Proxy 节点接收生产者客户端发送过来的消息，并且把消息转发给 Broker 节点的功能，这个功能肯定还没实现呢。但要我说，这个功能中最核心的步骤就是根据 Proxy 缓存的指定主题的路由信息找到接收消息的 Broker 节点，这个功能实现之后，其他的操作就都很简单了。这也就意味着接下来的内容就没什么可讲的了，没什么技术含量，都是一些常规代码，不是验证数据是否合规合法，就是转换请求类型，以便 Broker 节点能够顺利接收，我相信，大家自己结合注释阅读这些代码也能读懂。所以接下来我就直接展示我提供的第十二版本代码的部分内容，讲解代码逻辑和程序执行流程了，不然文章篇幅只会越来越长。  
  
实现 Proxy 转发消息给 Broker 节点功能  
  
按照老流程，当生产者客户端调用了 sendMessage() 方法把生产的消息发送给 Proxy 服务端之后，肯定应该由 Proxy 服务端的 sendMessage() 方法来接收。这很好办， 直接给处理消息的 GrpcMessagingApplication 类定义 sendMessage() 方法即可 。而根据之前我们实现的客户端到 Proxy 服务端查询路有消息功能、以及 Proxy 服务端处理遥测请求和心跳请求的功能来看， 给 GrpcMessagingApplication 类新定义的 sendMessage() 方法也不会真的处理请求，肯定是定义一个专门的执行器来处理此类请求，然后把发送消息请求提交给 GrpcMessingActivity 消息活动处理器来处理 ，就像下面代码块展示的这样，请看下面代码块。  
当然，就算是 GrpcMessingActivity 消息活动处理器得到了消息，也不会真的处理，而是会再定义一个专门处理生产者生产的消息的活动处理器，也就是 SendMessageActivity 活动处理器，让该处理器来处理消息，就像下面代码块展示的这样，请看下面代码块。  
当然， 就算到了 SendMessageActivity 活动处理器中也不会真的处理消息，因为和之前实现的 RouteActivity 活动处理器、ClientActivity 活动处理器一样，SendMessageActivity 内部也继承了 AbstractMessingActivity 类，内部也持有了 MessagingProcessor 消息处理器。也就是说，就算 SendMessageActivity 得到了消息，也不会处理消息，而是把消息交给 MessagingProcessor 消息处理器来处理 。当然，这并不意味着 SendMessageActivity 活动处理器什么也没做，它执行的操作非常简单， 那就是得到了消息队列路由选择器，除此之外，它还做了一个非常重要的工作，那就是把从生产者客户端接收到的消息对象转换为了 RocketMq 内部模块使用的消息对象了 。 生产者客户端向服务端发送的消息被封装到了 Grpc 框架通信使用的 Message 对象中，SendMessageActivity 活动处理器把这个消息对象中的内存转存到 RocketMq 内部模块使用的消息对象中了 。接下来，就让我为大家展示一下 SendMessageActivity 类的内容，请看下面代码块。  
上面代码块中的注释非常详细，我就不再重复解释了，既然 SendMessageActivity 活动处理器会把消息交给 MessagingProcessor 处理，那接下来，就让我们到 MessagingProcessor 接口的实现类，也就是 DefaultMessagingProcessor 类中看看消息处理器是怎么处理消息的，请看下面代码块。  
从上面代码块中可以看到，消息处理器也没有直接处理消息，而是新定义了一个 ProducerProcessor 生产者处理器，把消息交给生产者处理器处理了。那 ProducerProcessor 生产者处理器是怎么处理消息的呢？这个就没什么好分析的了，我就直接跟大家讲解了吧：在 ProducerProcessor 中执行的操作很简单， 第一个就是验证要处理的消息的消息类型是否和主题配置的消息类型一致；第二个操作就是使用之前传输进来的消息队列选择器选择目标消息队列，目标消息队列一旦确定，那么 Broker 节点的网络地址也就确定了，接下来就可以把消息发送给目标 Broker 节点了；第三个要执行的操作就是既然要把消息传输给 Broker 节点，Proxy 和 Broker 之间通信使用的是 RemotingCommand 协议，那肯定就要创建 RemotingCommand 请求对象，创建对应的请求头，这就是剩下要执行的操作 。接下来就让我为大家展示一下新引入的 ProducerProcessor 类，请看下面代码块。  
好了，现在 ProducerProcessor 类也展示完毕了，并且我们可以看到， 在 ProducerProcessor 对象处理消息时，最终把转换成 RemotingCommand 协议的消息对象交给 ServiceManager 去处理了，实际上是从 ServiceManager 中得到了一个 MessageService 对象，然后使用 MessageService 对象把消息发送出去了，这个 MessageService 显然是刚引入的，从字面意思上看就是消息服务组件的意思 。那接下来我们就先到 ServiceManager 类的实现类 LocalServiceManager 类中查看一下，看看它是怎么返回 MessageService 对象的，请看下面代码块。  
LocalServiceManager 返回 MessageService 对象的代码也实现了，我们也知道了 MessageService 其实是一个接口，其实现类为 LocalMessageService，并且这个 LocalMessageService 类还持有了 brokerController 对象，那接下来我们就看看 LocalMessageService 类的内容，请看下面代码块。  
看完了上面代码块的内容，大家肯定十分遗憾，我可以很负责任地告诉大家， LocalMessageService 就是 Proxy 内部最后处理生产者消息的对象，在 LocalMessageService 对象的 sendMessage() 方法中，消息确实要被发送给 Broker 节点 ，但是我并没有真的实现，而是写成了伪代码，仅仅是把消息接收成功的信息输出在控制台上，以便验证程序正确性。我想跟大家解释一下我为什么这么做， 在源码的 sendMessage() 方法中会使用请求头创建 RequestCode.SEND\_MESSAGE 类型的请求对象，然后使用 brokerController 成员变量得到 Broker 服务端处理 RequestCode.SEND\_MESSAGE 请求的请求处理器，把请求直接交给请求处理器处理 。注意，这个操作过程中并没有涉及到网络传输，因为 Proxy 为本地部署，Proxy 直接就可以得到 Broker 节点；如果是集群模式部署 Proxy 节点，那就设计网络通信了，Proxy 创建 RequestCode.SEND\_MESSAGE 类型的请求，请求发送给 Broker 服务端，Broker 服务端根据请求类型找到对应的请求处理器处理请求。但现在是本地模式部署的 Proxy 节点，也就是说，按照源码的做法已经可以直接在 sendMessage() 方法中把请求创建完毕，然后交给 Broker 服务端对应的请求处理器了，但我们并没有为 Broker 服务端实现对应的请求处理器，所以就不能真的把请求交给 Broker 处理。现在大家应该清楚是怎么回事了吧？而为 Broker 节点实现处理 RequestCode.SEND\_MESSAGE 类型请求的处理器则是下一章的内容，到时候我会为大家详细讲解。  
  
到此为止本章的内容就全部结束了，现在大家也可以去阅读第十二版本代码了，本章的内容非常多，大家一定要多花点时间好好看看，真正掌握了每个版本代码的内容才去阅读下一个版本代码。我知道阅读代码是一件非常枯燥的事，如果我的课程是免费的，那我一定没有足够得动力写这么多篇文章，阅读这么多框架源码，我相信这种枯燥对每一位朋友来说都是相同的，但学习本身就是一件枯燥的事情，不管我把文章写得多么通俗易懂，知识的难度就在那里摆着，框架的架构，类关系，方法逻辑不会因为我写的文章好坏而发生任何一丁点变化，我能做的就是尽量提供高质量的文章，把每一个功能迭代、实现清楚，至于真正学习框架，仍然是大家自己的事。我希望每一位朋友都能坚持下来，好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/wzmxcv7dm0fu75az*  
*All content belongs to its respective owners and creators.*