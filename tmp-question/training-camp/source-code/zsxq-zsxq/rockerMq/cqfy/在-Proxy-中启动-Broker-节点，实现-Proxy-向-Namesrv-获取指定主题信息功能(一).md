  
实现在 Proxy 模块中启动 Broker 节点功能  
  
在很多章之前，我就已经为我们自己的消息队列框架构建了一个简单的 Proxy 模块，也许有的朋友已经把这个模块忘记了，或者是印象没那么深刻了，所以我打算先帮助大家简单回顾一下我们自己搭建的 Proxy 模块，就以这个 Proxy 模块的 Grpc 服务端启动过程来回顾 Proxy 模块吧。  
  
我们首先给 Proxy 模块定义了一个 ProxyStartup 启动器，只要运行这个 ProxyStartup 启动器的 main() 方法，Proxy 模块的服务端就被启动起来了。我把这个 ProxyStartup 启动器的部分内容展示在下面了，请看下面代码块。  
package org.apache.rocketmq.proxy;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/12/31

\* @方法描述：proxy模块的启动类

\*/

public class ProxyStartup {  
private static final Logger log \= LoggerFactory.getLogger (LoggerName.PROXY\_LOGGER\_NAME);  
//proxy模块的启动和关闭管理器

private static final ProxyStartAndShutdown PROXY\_START\_AND\_SHUTDOWN \= new ProxyStartAndShutdown ();

public static void main (String \[\] args) {

try {

//从命令行参数中解析用户配置的参数信息，在我为大家提供的测试类中

//我只配置了proxy程序的部署启动方式，我设置成local模式了，也就是本地模式

//具体的命令为-pm local

CommandLineArgument commandLineArgument \= parseCommandLineArgument (args);

//根据用户配置的命令行菜属初始化proxy模块的配置信息

initConfiguration (commandLineArgument);  
//创建消息处理器，proxy模块接收到的所有客户端请求最后都是由消息处理器处理的

MessagingProcessor messagingProcessor \= createMessagingProcessor ();  
  
//创建Grpc服务器要使用的线程池

ThreadPoolExecutor executor \= createServerExecutor ();

//创建Grpc服务器，ConfigurationManager.getProxyConfig().getGrpcServerPort()得到Grpc服务器的端口号，这里得到的就是8081

GrpcServer grpcServer \= GrpcServerBuilder.newBuilder (executor,ConfigurationManager.getProxyConfig ().getGrpcServerPort ())

//创建Grpc服务器的消息处理器，Grpc服务器接收到的客户端消息都会先交给这个消息处理器处理

从上面代码块中可以看到，在启动 Proxy 模块的 Grpc 服务端之前，先创建了一个 MessagingProcessor 消息处理器对象，然后又创建了一个 GrpcMessagingApplication 对象，并且 GrpcMessagingApplication 对象持有了 MessagingProcessor 消息处理器对象，而这个 GrpcMessagingApplication 对象就是 Grpc 服务器要使用的处理消息的组件。我先把 GrpcMessagingApplication 的相关代码展示给大家，请看下面代码块。  
从上面代码块中可以看到，在调用 GrpcMessagingApplication 对象的 create() 方法的过程中，还会创建一个 DefaultGrpcMessingActivity 对象，它就是 Grpc 服务端的默认消息处理器，这个对象会持有 Grpc 服务端最终处理消息的 MessagingProcessor 消息处理器对象，那接下来我就为大家再把这个 DefaultGrpcMessingActivity 类的内容展示一下，请看下面代码块。  
从上面代码块中可以看到，这个 DefaultGrpcMessingActivity 对象还持有了一个 RouteActivity 主题路由消息活动器，并且这个活动期最终持有了这个 MessagingProcessor 消息处理器对象，那接下来就让我为大家把 RouteActivity 和 MessagingProcessor 这两个对象的内容展示一下，请看下面代码块。  
接着是最终处理请求的 DefaultMessagingProcessor 消息处理器。  
好了，支撑 Proxy 模块的 Grpc 服务端顺利工作需要的所有类都展示完毕之后，接下来我们再来梳理一下 Proxy 的 Grpc 服务端处理请求的具体流程，假如 Grpc 服务端接收到客户端发送过来的 GET\_ROUTEINFO\_BY\_TOPIC 获取指定主题的路由信息的请求，那么这个请求就会以如下流程被 Grpc 服务端处理，请看下面代码块。  
以上就是 Proxy 模块的 Grpc 服务端处理 GET\_ROUTEINFO\_BY\_TOPIC 请求的大概流程流程。相信随着上面代码块的展示，现在大家应该都会以起 Proxy 模块的内容了。那接下来就让我们回到本小节的主题上吧，也就是实现在 Proxy 模块中启动 Broker 节点功能，这个功能实现起来非常简单，几句话就能讲清楚： 那就是让 Proxy 模块在 maven 中继承 Broker 模块，然后在启动 Proxy 模块的过程中启动 Broker 模块即可；当然，这么做有一个前提，那就是以 Local 本地模式部署 Proxy 的时候才可以这么做 。就像下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到， 在创建 MessagingProcessor 消息处理器的时候，判断了一下当前 Proxy 模块是否以本地模式部署，如果是那就启动 Broker 节点，然后把得到的 BrokerController 对象传递到了创建完毕的 MessagingProcessor 消息处理器中 。到此为止，本小节的功能就实现完毕了。  
  
引入 ServiceManager 服务管理器  
  
当然，实现了在 Proxy 模块中启动 Broker 节点功能还算不了什么，顶多算是一个开始，因为只有启动了 Broker 节点，Broker 节点才能创建主题信息，才能把自己的所有信息注册到 Namesrv，Proxy 才能作为客户端的代理组件从 Namesrv 获取指定主题的路由信息，而我们本章的核心内容，就是实现 Proxy 才从 Namesrv 获取指定主题的路由信息功能。  
  
要实现这个功能需要什么呢？首先需要给 Proxy 模块再定义一个客户端，拥有了 Netty 构建的客户端，Proxy 才能和 Namesrv 节点进行通信， 所以客户端是 Proxy 模块必备的组件 。有了客户端之后，Proxy 模块就可以向 Namesrv 发送请求了，也就是 GET\_ROUTEINFO\_BY\_TOPIC 请求，而 Namesrv 接收并处理该请求的功能上一章已经实现了，我们已经把 Namesrv 的 ClientRequestProcessor 请求处理器重构完毕了，请求处理器会直接把该主题下所有的路由信息返回给 Proxy 的客户端。这一点是肯定的吧？  
  
那 Proxy 得到了对应主题的路由信息之后呢？显然是应该把主题路由信息存储起来，按照常规理解，显然也应该把主题路由信息存储到一个 Map 中，Map 的 key 是主题名称，value 就是对应的所有路由信息。就像下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到， 我们新引入了一个 MessageQueueView 类，从名字上看这个类的对象就是一个消息队列视图，该对象会封装一个主题下所有可路由的 Broker 节点信息，以及可路由的节点中的读写队列信息 。讲到这里我想先插一句题外话， 我相信有些朋友到现在还不清楚这些路由信息怎么使用，确切地说，是到现在大家还不知道这些主题配置信息中的读写队列究竟怎么使用，如果只看路由信息的话，那路由信息其实就是可以接收生产者发送的消息或者让消费者获取消息的 Broker 节点信息 。我相信这些路由信息肯定会让一些朋友感到困惑，我还是那句话，不要急，先把这些内容记住，很快我们就会在后面的版本代码中真正使用它们，等我们实现生产者客户端向 Broker 发送消息功能时，大家就会清楚这些路由信息是怎么发挥作用的了。  
  
好了，让我们言归正传， 现在我们已经为 Proxy 模块定义好了存储所有主题路由信息的 Map，这也就意味着 Proxy 接收到从 Namesrv 返回的主题路由信息后，就可以把这些信息都缓存到 topicCache 这个 Map 中，这样一来，当生产者或消费者客户端从 Proxy 模块获取对应主题的路由信息时，Proxy 模块就不必每次都去 Namesrv 查询了 。当然，这也就带来了一些新的问题，那就是 Broker 节点的主题信息是会动态变化的，每次变化之后都会把最新的信息注册到 Namesrv 节点， 这就要求了 topicCache 中缓存的主题路由信息必须是最新的 ，Proxy 不能把旧的主题路由信息返回给生产者或消费者客户端吧？这就给 topicCache 中缓存信息的时效性带来了很高的挑战。那这个问题该怎么解决呢？  
  
为了解决这个问题，我使用了第三方框架，也就是 caffeine 这个高性能的缓存组件。我决定使用这个 caffeine 组件来定义刚才的 topicCache，请看下面代码块。  
好了朋友们，如果大家仔细阅读了上面代码块中的内容，那么大家就会发现在使用 caffeine 组件定义了刚才的 topicCache 之后，这个 topicCache 就具备了新的功能： 当 topicCache 缓存的主题路由信息未命中时，也就是客户端向 Proxy 模块查询对应主题的路由信息，结果在 topicCache 中并没有查询到时，Proxy 就会访问 Namesrv 获得最新的主题路由信息；而当 topicCache 缓存了主题路由信息时，就会给这个路由信息设置一个有效期，有效期过了之后，topicCache 就会刷新缓存，也就是访问 Namesrv 获取该主题最新的路由信息 。这些逻辑应该都很容易理解吧？  
  
如果以上逻辑大家都理解了，那接下来就很好说了，我们只需要在 Proxy 模块中把这个 topicCache 定义出来就行了，我正是这么做的， 我给 Proxy 模块新定义了一个类，就叫做 ServiceManager，也就是服务管理器，这个 ServiceManager 服务管理器只是一个接口 。因为我们目前使用了本地模式部署 Proxy 模块， 所以我为这个 ServiceManager 服务管理器定义了一个名为 LocalServiceManager 的实现类 ，这个 LocalServiceManager 类的内容如下，请看下面代码块。  
从上面代码块中可以看到，我定义的这个 LocalServiceManager 类的内容非常简单，在这个 LocalServiceManager 类的对象创建的过程中，会把客户端工厂对象创建出来， 与此同时还会创建一个 LocalTopicRouteService 对象，这个对象我在代码中也解释了，就是主题路由信息服务对象 。现在大家应该清楚我定义这个 LocalServiceManager 服务管理器的用意了，因为在 RocketMq 源码中就为 Proxy 模块定义了这个服务管理器，并且在这个 LocalServiceManager 服务管理器中定义了多个服务组件，除了主题路由信息服务对象，还有什么事务服务组件，控制台服务组件等等，在我们的第八版本代码中只用到了主题路由信息服务组件，所以我就只引入了 LocalTopicRouteService 类。  
  
那说到现在，这个 LocalTopicRouteService 类的内容还没给大家展示呢，接下来就请大家看一下新出现的 LocalTopicRouteService 主题路由服务组件的内容，请看下面代码块。  
很好，随着 TopicRouteService 类的展示，这个 topicCache 成员变量终于被定义在 Proxy 模块中了。上面代码块的注释非常详细，内容也很简单，我就不再重复它们的逻辑了。现在的情况是， 我们为 Proxy 模块定义了一个 ServiceManager 服务组件管理器和 TopicRouteService 主题路由信息服务组件，在创建 ServiceManager 服务组件管理器的时候，会把 TopicRouteService 主题路由信息管理器创建出来，而随着 TopicRouteService 对象的创建，Proxy 模块存储从 Namesrv 获得的主题路由信息的 Map 也就创建完毕了 。并且我们还能从上面的代码块得到这样一条重要的信息： 那就是只要调用了 TopicRouteService 对象的 getTopicRouteForProxy() 方法，就可以从缓存了主题路由信息的 topicCache 成员变量中得到指定主题的路由信息 。当然，这个 getTopicRouteForProxy() 方法我还没有真的实现，但这个方法的逻辑就是这么回事了。  
  
如果大家理解了以上内容，那现在请大家跟着我的思路，让我们一起看看生产者或消费者客户端要从 Proxy 模块中获取指定主题的路由信息时会发生什么： 当 Broker 节点、Namesrv、Proxy 都启动之后，Broker 节点会把自己的配置信息和主题信息都注册到 Namesrv 中，而 Proxy 在启动的过程中会把存储主题路由信息的 topicCache 数据结构定义完毕，注意，这个时候 topicCache 中并没有缓存任何主题信息；当生产者或消费者第一次向 Proxy 查询对应主题的路由信息时，Proxy 最后肯定会到 topicCache 成员变量中查询，这个时候 topicCache 中没有任何内容，所以缓存不会命中，然后 Proxy 就会访问 Namesrv 获取对应主题的路由信息，然后把主题路由信息返回给客户端，同时把主题信息缓存到 topicCache 中，并且之后会定期刷新该主题的最新路由信息 。这就是 Proxy 缓存并返回生产者、消费者客户端主题路由信息的核心流程，大家可以仔细品味品味这个逻辑。  
  
如果以上内容都理解了，那就让我们来实现本小节最后一个功能，把 LocalTopicRouteService 类的 getTopicRouteForProxy() 方法实现了。这个方法就是用来从父类的 topicCache 中获取指定主题路由信息的，这一点我在之前的代码块中已经为大家注明了。当然，这个方法实现起来并不难，从逻辑上来看，可以说是非常简单，但在这个方法执行的所有操作中，有一个操作需要特别强调，所以我才把这个 getTopicRouteForProxy() 方法单独拿出来放到这里给大家讲解。  
  
首先还是看看该方法的逻辑，这个我已经跟大家说了， 在该方法内会从父类的 topicCache 中获取指定主题路由信息，而指定主题的路由信息已经封装好了，就是 topicCache 成员变量中的 value，value 就是一个 MessageQueueView 对象，而这个 MessageQueueView 对象中持有了 TopicRouteData 对象，TopicRouteData 对象其实就是 Namesrv 返回给 Proxy 客户端的，该对象封装了指定主题的所有路由信息 。这也就是说， 在 LocalTopicRouteService 类的 getTopicRouteForProxy() 方法中只要从父类的 topicCache 成员变量中获取指定主题对应的 value 即可 。这一点大家都赞同吧？而这就是 LocalTopicRouteService 类的 getTopicRouteForProxy() 方法的核心逻辑了。这么来看，这个方法实现起来确实一点也不难，但是，我还是要说一声但是，这个方法仅仅实现到这个程度还不够，请大家想一想， 我们要做的是把这个主题的路由信息返回出去，而路由信息是什么呢？其实就是可以访问的 Broker 节点的网络地址，这个就是路由信息的本质，只有把这个信息返回给生产者或消费者客户端，那生产者客户端才知道要把消息发送给哪个 Broker 节点，消费者客户端才知道自己要从哪个 Broker 节点获取消息 。这个逻辑可以理解吧？  
  
如果大家理解了这个逻辑，那么问题就来了，现在生产者和消费者客户端有了一个代理模块，那就是 Proxy 模块，生产者和消费者客户端不管执行什么操作，都要先把对应的请求发送给 Proxy 节点，让 Proxy 节点为自己代理执行。这也就是说生产者或者消费者客户端想要生产消息或者消费消息，也需要把对应的请求发送给 Proxy，让 Proxy 帮助自己把消息发送给可路由的 Broker 节点，或者帮助自己从可路由的 Broker 节点获取消息。总而言之，生产者和消费者客户端不会直接和 Broker 节点打交道了。如果大家理解了这个逻辑，那接下来就会意识到一个问题， 当 Proxy 模块从 LocalTopicRouteService 对象的 getTopicRouteForProxy() 方法中得到了客户端指定主题的路由信息，然后把这个路由信息返回给客户端的时候，这个时候返回的所有 Broker 节点的网络地址都是自身的地址，那生产者和消费者客户端得到 Broker 节点的地址后，就可以自己直接和 Broker 节点联系了，那 Proxy 模块的存在就没有意义了 。所以，为了避免出现这种情况， 我们需要给 Proxy 模块增加一个新的功能，那就是在 Proxy 模块把可路由的 Broker 节点信息发回给客户端之前，把所有 Broker 节点的网络地址都更换成 Proxy 节点自己的。这样一来，客户端就算得到了一大堆可路由的 Broker 节点信息，最后要生产消息或者获取消息也是向 Proxy 节点发送请求，在 Proxy 模块内部，就会执行真正的路由操作，这个路由操作就会选取可路由的 Broker 主从节点中的主节点，然后把对应的请求发送给主节点，这样一来也就做到了只让 Broker 集群主节点参与业务活动 。当然，现在我们不会对 Proxy 内部真正的路由操作展开讲解，大家只需要先记住这个逻辑即可，后面我们会实现这个功能。  
  
总之，我现在解释了这么多，就是希望大家明白， 在 LocalTopicRouteService 对象的 getTopicRouteForProxy() 方法中，需要把所有 Broker 节点的网络地址都更换成 Proxy 节点自己的 ，大家要清楚执行这些操作的原因。好了，这个内容讲解完毕之后，就可以请大家看一下我最终实现的 LocalTopicRouteService 对象的 getTopicRouteForProxy() 方法了，请看下面代码块。  
大家可以仔细阅读上面的代码块，或者结合文章直接阅读我提供的第八版本代码，把上面代码块中的逻辑梳理清楚了，再继续向下阅读。总之，这一切都完成了之后，我们就可以最终再总结一下，现在的情况是这样的： 使用 ServiceManager 的 getTopicRouteService() 方法可以得到 TopicRouteService，而通过 TopicRouteService 的 getTopicRouteForProxy() 方法可以得到缓存在 Proxy 内部的主题路由信息 。这也就是说， 只要 Proxy 模块得到了 ServiceManager 服务管理器，就可以从服务管理器中得到缓存在 Proxy 内部的主题路由信息 。这一点是毋庸置疑了吧？如果大家理解了这一点，那新的问题就又来了，ServiceManager 该怎么创建？或者说该定义在哪里呢？  
  
重构 DefaultMessagingProcessor 消息处理器  
  
其实分析到这里，我不讲解大家应该也能到猜到了， ServiceManager 服务组件管理器显然应该定义在 DefaultMessagingProcessor 消息处理器中 。原因很简单，我们之前定义了一个 RouteActivity 路由消息活动器，这个 RouteActivity 对象就是用来获取指定主题对应的路由信息的，客户端发送给 Proxy 模块的所有获取路由信息的请求都会被这个 RouteActivity 活动器处理， 而这个 RouteActivity 路由消息活动器内部持有了 DefaultMessagingProcessor 消息处理器对象，最后是从 DefaultMessagingProcessor 消息处理器中得到主题路由信息，那么这就意味着这个 ServiceManager 服务管理器要被 DefaultMessagingProcessor 持有，或者说通过 DefaultMessagingProcessor 消息处理器能得到 ServiceManager 服务管理器，这样才能从 ServiceManager 服务管理器中得到主题路由信息 。这一点也可以理解吧？那我就把 ServiceManager 直接定义成 DefaultMessagingProcessor 类的成员变量了，就像下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到， DefaultMessagingProcessor 的 getTopicRouteDataForProxy() 方法被重构了，获得注定主题的路由信息时，就是通过 ServiceManager 服务组件管理器获得的 。并且我们还可以看到，创建 DefaultMessagingProcessor 消息处理器对象的 createForLocalMode() 方法也被重构了，在该方法中还是用 ServiceManagerFactory 服务组件管理器工厂把 ServiceManager 服务组件管理器创建出来了。而 createForLocalMode() 方法会在 ProxyStartup 启动其中被调用，这个大家应该已经很熟悉了吧？那到此为止，我们就清楚地知道了 ServiceManager 服务组件管理器会被定义在哪里，以及它的创建时机了。  
  
重构 RouteActivity 主题路由消息活动器  
  
好了朋友们，现在我们就已经把 Proxy 模块管理从 Namesrv 获取的主题路由信息的功能都实现完毕了，接下来就该实现 Proxy 模块最重要的功能了，那就是把主题路由信息返回给客户端的功能。在我们之前实现 Proxy 模块中，生产者或消费者客户端发送给 Proxy 模块的 GET\_ROUTEINFO\_BY\_TOPIC 请求都会被 Proxy 模块的 RouteActivity 路由消息活动器处理，就像下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到，在 RouteActivity 路由消息活动器的 queryRoute() 方法中，从消息处理器中获得了指定主题的路由信息，根据我们上一小节实现的功能来看，现在这个 queryRoute() 方法中的操作并不需要改变，反正 MessagingProcessor 消息处理器最后也是使用了 ServiceManager 服务组件管理器得到的指定主题的路由信息。那这是不是就意味着 RouteActivity 路由消息活动器的 queryRoute() 方法并不需要重构了？当然不是这样，实际上这个时候就轮到大家非常熟悉的一个队列，也就是 MessageQueue 队列登场了。我先不跟大家解释这个队列的作用，首先让我来跟大家解释一下这个 MessageQueue 队列的具体内容，我把这个类的核心内容展示在下面代码块了，请看下面代码块。  
好了，现在这个 MessageQueue 类的内容展示完毕了，接下来就让我为大家解释一下，引入这个 MessageQueue 类的作用。在我们之前讲解主题配置信息的时候，我们同时也为主题创建了对应的配置信息，比如说我们创建了一个名为 Test 的主题，这个主题的配置信息是这样的：有 8 个读队列，8 个写队列，拥有读写权限，主题接收的消息就是普通消息；然后我们在一个 Broker-a 的主节点上创建了该主题。这样一来，这个名为 Broker-a 的主节点的从节点也都拥有了这个 Test 主题信息，这个可以理解吧？  
  
那这些主题配置信息中的队列究竟有什么作用呢？现在我可以跟大家解释一下，从概念上来说，大家就可以把这些队列当成 Broker 节点中的队列，对于我们刚才创建的 Test 主题，就意味着会在名称为 Broker-a 的 Broker 主从节点中，给每一个 Broker 节点都定义了 8 个写队列和 8 个读队列，当 我们把这个 Broker 节点的信息返回给生产者和消费者客户端之后，客户端就会根据 Broker 节点中的队列信息把消息路由到具体的队列中，比如说有一个生产者客户端得到了 Test 节点的路由信息，路由信息中只有 Broker-a 主从节点的信息，那么生产者客户端在发送消息的时候就可以先从路由信息中得到 Broker-a 主从组中主节点的信息，然后可以得到 Broker-a 主节点中写队列的信息，客户端知道这个主节点有 8 个写队列之后，那生产者每次向这个 Broker 主节点发送消息，就可以使用轮询的方式，依次向 Broker-a 主节点中每一个写队列发送消息。这就是一个最简单的生产者客户端使用路由信息发送消息的流程。  
  
如果大家熟悉了上面的这个流程，那还是回到这个 MessageQueue 队列上，Proxy 要把主题路由信息返回给生产者客户端的时候，就会把主题路由信息直接以 MessageQueue 队列集合的方式返回给客户端， Broker-a 节点有 8 个写队列和 8 个读队列，那就会创建 16 个 MessageQueue 对象，然后把 MessageQueue 队列集合直接返回给客户端 。这样生产者客户端就可以直接在负载均衡器中使用具体的策略从 MessageQueue 集合中选择一个 MessageQueue 队列发送消息，只要选中了一个 MessageQueue 队列，就可以从队列中得到要发送消息的 Broker 节点的信息，以及该主题接收的消息类型，当然，还不必要保证这个队列是可写队列才能发送消息。也就是说， Proxy 模块在最终回复主题路由消息之前，需要把主题路由消息都转换成一个个 MessageQueue 队列，然后把这些 MessageQueue 队列添加到集合中，把集合返回给客户端即可 。这些流程应该也不难理解吧？  
  
很好，如果大家理解了上面的流程之后，那接下来我还要跟大家解释一句，在 5.0 之后的 RocketMq 版本中，上面的这个流程已经不适用了，当然，Proxy 模块肯定会在最终回复主题路由消息之前，把主题路由消息都转换成一个个 MessageQueue 队列，然后把这些 MessageQueue 队列添加到集合中，把集合返回给客户端。 但是客户端得到 MessageQueue 集合之后，并不会真的使用负载均衡策略选择具体的 MessageQueue 对象工作了，因为引入了 Proxy 模块之后，真正路由的操作并不再由生产者或者消费者客户端负责，而是由 Proxy 模块负责 ，至于如何负责，如何路由，再后面两个版本代码中我就会为大家实现了。所以就算 Proxy 模块把主题路由信息都封装到 MessageQueue 队列集合中，客户端得到这些 MessageQueue 队列信息也会真的派上什么用场。我在这里围绕着 MessageQueue 解释了这么多，就是希望大家明白这一点，当然，如果还有什么要补充，那就是 MessageQueue 队列其实并不是真实存在的队列，并不是真实的队列数据结构，这一点在后面的代码中也会得到印证。  
  
好了朋友们，说了这么多，接下来就给大家简单展示一下 Proxy 模块把主题路由消息转换为 MessageQueue 队列集合的代码，也就是把 RouteActivity 的 queryRoute() 方法简单重构一下。因为这部分的逻辑比较细碎，所以我就不再文章中讲解和全部展示了，因为它的逻辑和操作确实很简单，就是获取数据，转换数据，封装数据的操作，来来回回都是那些信息，只不过把信息以不同的数据机构存储而已。大家也可以结合文章直接阅读我提供的第八版本代码。好了，接下来就请大家看一下重构完毕的 RouteActivity 类的 queryRoute() 方法，请看下面代码块。  
到此为止，本章内容就全部结束了，大家可以阅读我提供的第八版本代码的全部内容了。在阅读代码的过程中，大家可以启动 Proxy 模块下的测试类，也就是那个 Grpc 客户端的测试类。让程序运行起来，这样能更好地验证自己的猜想，以及文章中展示的所有内容的正确性。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/zcfrhmx2qyo0foak*  
*All content belongs to its respective owners and creators.*