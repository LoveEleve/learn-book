  
为 Proxy 服务端引入 ClientActivity 类  
  
在第九版本代码中我们已经把生产者客户端构建完毕了，实现了生产者客户端的启动，以及向 Proxy 服务端发送心跳请求和遥测请求功能。 这一章我们就要把 Proxy 服务端的对应功能实现了，让 Proxy 服务端可以向生产者客户端回复心跳响应，遥测请求的响应以及管理客户端信息 。这些功能都很简单，因为在之前的很多框架中，我们都实现过类似的功能，远的就不谈了，之前实现的 Nacos 框架，Nacos 服务端不是也具备这些功能吗？如果大家对之前框架的内容还有印象，就能意识到这些功能实现起来确实不复杂，尤其是在 Proxy 模块中。  
  
因为我们自己构建的消息队列的客户端和 Proxy 服务端使用 Grpc 框架进行通信，而在之前构建 Proxy 服务端的时候，我们已经把处理客户端消息的 Grpc 消息处理应用定义完毕了， 我们定义了一个 GrpcMessagingApplication 类，并且在这个类中定义了很多方法来处理客户端发送过来的不同请求，只要是客户端发送过来的请求，首先就会被这个 GrpcMessagingApplication 对象中的方法接收 ，这是之前的知识点，大家也许还有印象，也许已经忘了，现在我把 GrpcMessagingApplication 类的内容搬运到这里帮助大家简单回顾一下，请看下面代码块。  
package org.apache.rocketmq.proxy.grpc.v2;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/3

\* @方法描述：Grpc服务端消息处理应用，当Grpc客户端每一次执行远程调用方法的时候，服务端的这个类的对象，都会根据客户端远程调用的方法的名称，执行对应的方法

\* 这个类的对象就相当于服务端提供目标方法的服务组件

\*/

public class GrpcMessagingApplication extends MessagingServiceGrpc.MessagingServiceImplBase implements StartAndShutdown {  
private final static Logger log \= LoggerFactory.getLogger (LoggerName.PROXY\_LOGGER\_NAME);  
//Grpc服务端的默认消息处理器

private final GrpcMessingActivity grpcMessingActivity;  
//Grpc服务端处理客户端发送过来的根据主题查询路由信息的请求时，就会使用这个线程池来处理

protected ThreadPoolExecutor routeThreadPoolExecutor;  
//构造方法

protected GrpcMessagingApplication (GrpcMessingActivity grpcMessingActivity) {

this.grpcMessingActivity \= grpcMessingActivity;

//从配置管理器中得到Proxy模块的配置信息对象

ProxyConfig config \= ConfigurationManager.getProxyConfig ();

//创建routeThreadPoolExecutor线程池，用于处理主题路由信息请求

this.routeThreadPoolExecutor \= ThreadPoolMonitor.createAndMonitor (

//得到主题路由信息线程池的核心线程数

config.getGrpcRouteThreadPoolNums (),

//得到主题路由信息线程池的最大线程数

config.getGrpcRouteThreadPoolNums (),

//得到主题路由信息线程池的线程空闲存活时间

1,

TimeUnit.MINUTES,

大家对上面代码块的内容一定非常熟悉，之前我们实现 Proxy 服务端功能的时候，就把 queryRoute() 方法实现完毕了，当时我们只是模拟了一个简单的 Grpc 客户端，让客户端向 Proxy 服务端查询指定主题的路由信息， 只要客户端向服务端发送了 QueryRouteRequest 请求，那么这个请求就会被 Proxy 服务端 GrpcMessagingApplication 对象的 queryRoute() 方法接收并处理，并且还在 GrpcMessagingApplication 类中专门定义了一个 routeThreadPoolExecutor 主题路由线程池来处理该请求，当然，在真正处理请求的时候，还是调用了 grpcMessingActivity 消息处理器的同名方法来处理 。那现在我们要为 Proxy 服务端实现处理遥测请求功能和心跳检测功能，看样子只需要仿照 Proxy 服务端处理 QueryRouteRequest 查询路由信息请求的逻辑来实现即可。  
  
比如说我们可以在 GrpcMessagingApplication 类中再定义两个方法，一个是 heartbeat() 方法，专门处理客户端发送过来的心跳请求，另一个是 telemetry() 方法，转么处理客户端发送过来的遥测请求 。当然， 我们还可以在 GrpcMessagingApplication 类中再定义一个 ThreadPoolExecutor 执行器 ，这个执行器专门用来处理客户端发送过来的 心跳请求和遥测请求， 我们可以把这个执行器成员变量定义为 clientManagerThreadPoolExecutor 。当然， 在新定义的这两个方法中并不会真的处理请求，而是会把请求交给 GrpcMessingActivity 消息默认处理器来处理 。这样分析下来之后，GrpcMessagingApplication 类可以重构成下面这样，请看下面代码块。  
以上代码块的逻辑非常简单，也很容易理解吧？我就不再重复解释了。好了，GrpcMessagingApplication 类已经简单重构完毕了，但这只是一个开始，因为请求并没有被真正处理呢，要想真正处理请求，显然应该再深入到 GrpcMessingActivity 消息处理器内部，看看消息处理器是怎么处理请求的。因为 GrpcMessingActivity 消息处理器的实现类是 DefaultGrpcMessingActivity 类，所以我把 DefaultGrpcMessingActivity 类的部分内容搬运过来了，请看下面代码块。  
可以看到，实际上在 DefaultGrpcMessingActivity 类的 queryRoute() 方法中也没有真的处理请求，而是继续调用了 RouteActivity 路由信息活动处理器的同名方法，让 RouteActivity 对象真正处理请求。这部分的内容大家应该还有印象吧？在之前版本代码中，RouteActivity 已经被我们实现并引入了。好了，既然 DefaultGrpcMessingActivity 对象也没有真的处理 QueryRouteRequest 请求，而是把请求交给专门的 RouteActivity 路由信息活动器处理，那仿照这个逻辑， 就可以再给 DefaultGrpcMessingActivity 类定义两个方法，一个是 heartbeat() 方法，一个是 telemetry() 方法，并且这两个方法也无需真的处理对应的请求，而是应该把请求交给专门的活动处理器来处理 。很好，那现在的问题就成了要定义一个活动处理器了， 并且这个活动处理器内部还要实现 heartbeat() 方法和 telemetry() 方法 ，那么这个活动处理器该怎么定义呢？  
  
定义一个新的活动处理器，这可不是一件简单的事啊，既然没那么简单，那我们就从最简单的地方开始思考吧？什么最简单呢？给这个活动处理器命名最简单，因为我已经想好了， 我要把这个活动处理器命名为 ClientActivity 。从名字上来看，ClientActivity 就是是客户端活动处理器的意思，那为什么要给活动处理器定义这个名字呢？原因很简单： 因为目前客户端和服务端进行的活动并没有涉及到消息队列框架的业务逻辑，而是客户端和服务端本身的信息验证和交互逻辑，请求又是客户端发送过来的，所以我就为这些活动专门定义一个客户端活动处理器，只要是类似的请求，都交给这个活动处理器处理 。这样分析下来之后，客户端活动处理器就可以先简单定义成下面这样，请看下面代码块。  
可以看到，目前定义的 ClientActivity 客户端活动处理器还是一个空架子，虽然也定义了两个方法，但方法都没有真正实现，而成员变量则是一个也没有定义，值得注意的一点是，这个 ClientActivity 客户端活动处理器也继承了 AbstractMessingActivity 类，大家可以在阅读第十版本代码的时候多关注关注这一点。  
  
好了，现在客户端活动处理器定义好了，那么 DefaultGrpcMessingActivity 就可以进一步重构成下面这样，请看下面代码块。  
可以看到，当 DefaultGrpcMessingActivity 要处理客户端发送过来的心跳请求和遥测请求时，并不会真的处理它们，而是在对应方法中把请求交给 ClientActivity 客户端活动处理器来处理，所以我们应该把真正处理这两个请求的操作定义在 ClientActivity 客户端活动处理器中。那么 ClientActivity 客户端活动处理器该怎么处理这两个请求呢？换句话说，ClientActivity 客户端活动处理器应该怎么重构呢？  
  
完善 GrpcClientSettingsManager、GrpcChannelManager 类  
  
让我们先把重构 ClientActivity 客户端活动处理器重构的问题暂时放到一边，先缓一缓，因为接下来有一个新的问题需要我们关注。那就是生产者客户端向服务端发送遥测请求和心跳请求的作用是什么呢？发送遥测请求的作用非常简单，上一章结尾我也跟大家特意补充了： 所谓遥测请求就是 TelemetryCommand 请求，发送这个请求的作用非常明确，那就是客户端会把自己当前的配置信息发送给服务端，同时从 Proxy 服务端得到配置信息最新的值，然后更新客户端 ClientSettings 对象和它子类对象中的配置信息 。当然，TelemetryCommand 遥测请求的作用远不止这些，除了同步配置信息， TelemetryCommand 遥测请求还可以收集客户端的运行信息，这一点在第十版本代码中就可以得到印证 。好了，我们先不讨论遥测请求的具体功能，仅从同步配置信息这个功能来说，处理遥测请求似乎非常简单： 也许我们可以定义一个客户端配置信息管理器，当 Proxy 服务端接收到客户端发送过来的遥测请求后，就可以先从遥测请求中得到客户端当前配置信息，然后把配置信息更新到客户端配置信息管理器中，接下来 Proxy 服务端就可以把自己的配置信息对象中的相关信息返回给客户端，更新生产者客户端本地的配置信息 。这些工作都可以由我们要定义的客户端配置信息管理器完成。  
  
至于心跳请求的作用就更简单了，它无非就是为了监测客户端和服务端的网络连接状态是否正常，只要 Proxy 服务端可以定期接收到客户端发送过来的心跳请求，而客户端能够及时接收到服务端回复的心跳响应，就表明客户端服务端的网络连接可以正常工作。这样分析的话，Proxy 服务端在接收到心跳请求后只需要回复一个响应即可，别的什么也不用做，对吧？分析得没有问题，但我们却不能真的这么做， 因为我们不能只考虑心跳检测本身，还要考虑心跳检测失败后要进行操作 。通常来说， 客户端在和服务端建立网络连接后，服务端肯定会对客户端的信息进行管理，也许在服务端内部会定义一个客户端信息管理器，或者是客户端 Channel 管理器，只要是和自己成功构建了连接的客户端，它们的信息都会被服务端保存到管理器中，而保存的这些信息在程序之后运行的过程中都会用上 ，这一点可以理解吧？这些都是很常规、很常见的编码逻辑。很好，如果大家理解了这些逻辑，那么大家就会意识到， 服务端只会管理网络连接状态正常的客户端的信息，如果某个客户端和服务端的网络连接断开了，那么服务端就要从客户端信息管理器中把对应客户端的信息清除了 ，而这就是客户端与服务端心跳检测失败后要执行的操作。之前我们构建生产者客户端的时候，我们不是启动了一个定时任务吗？这个定时任务会定期判断客户端和哪个服务端的网络连接失效了，如果失效了，客户端则会释放对应的网络连接资源；现在我们要为 Proxy 服务端实现心跳检测功能，那么心跳检测失败之后，也要释放对应的客户端信息。这也很容易理解吧？  
  
那么经过上面的分析之后，大家肯定就明白了，Proxy 服务端处理遥测请求和心跳请求的功能实现起来非常简单，要编写的都是很常规的代码，当然，如果说有一点要补充， 那就是在客户端和服务端构建网络连接之后，客户端要把自己的部分信息交给服务端来管理。这个操作可以定义在 Proxy 服务端处理心跳请求的过程中，也可以定义在 Proxy 服务端处理遥测请求的过程中 ，只有这样，客户端与服务端心跳检测失败后，服务端才能清除对应的客户端信息。好了，大概的编码思路我们已经分析完毕了，接下来就该真正编写代码了，根据我们刚才的分析，目前我们至少要引入两个新的组件， 一个是客户端配置信息管理器，另一个就是客户端信息管理器 。接下来我要说的是，如果大家对之前的内容还有印象，比如说对第四版本代码的内容还有印象，那大家肯定都还记得，早在第四版本代码中，我使用 Grpc 框架为大家构建 Proxy 服务端的时候，就已经引入了两个类，一个是 GrpcClientSettingsManager 类，另一个是 GrpcChannelManager 类，从名字上就能看出来，GrpcClientSettingsManager 类就是客户端配置信息管理器的意思，而 GrpcChannelManager 就是客户端 Channel 管理器的意思，所以说，我们早就把对应的组件引入进来了，只不过一直是空实现而已。那有了刚才分析的编码思路，接下来只需要实现它们就行了。  
  
那就让我先为大家把完善之后的 GrpcClientSettingsManager 类展示给大家，请看下面代码块。  
从上面代码块中可以看到， 在重构之后的 GrpcClientSettingsManager 类中，updateClientSettings() 方法会把从客户端发送过来的配置信息更新信息到 CLIENT\_SETTINGS\_MAP 成员变量中，以供后续使用；而 getClientSettings() 方法就得到服务端为客户端设置的配置信息，这个配置信息就可以返回给客户端使用 。我想这些逻辑应该都非常容易理解吧？好了，GrpcClientSettingsManager 类重构完毕了，接下来就让我为大家展示一下重构完毕的 GrpcChannelManager 类，请看下面代码块。  
好了，现在 GrpcChannelManager 类的内容也展示完毕了，可以看到这个类的内容非常简单，只有一个 clientIdChannelMap 成员变量，还有一个 createChannel() 创建 GrpcClientChannel 对象的方法。从这些内容可以看出来，GrpcChannelManager 管理器实际上管理的是 GrpcClientChannel 对象，那 GrpcClientChannel 是什么呢？让我来给大家解释一下： 实际上是这样的，当每一个客户端和 Proxy 服务端建立网络连接之后，Proxy 服务端都会为这个客户端的创建一个 GrpcClientChannel 对象，这个 GrpcClientChannel 对象中封装了一些客户端的信息，最重要的是，这个 GrpcClientChannel 对象中持有了服务端和客户端通信的双向流对象，也就是说，只要找到对应客户端的 GrpcClientChannel 对象，那么 Proxy 服务端就可以只用这个 GrpcClientChannel 对象主动和客户端进行通信 (GrpcClientChannel 类的内容我就先不给大家展示了，坦诚地说，这个类的内容也很重要，涉及到 Broker 节点主动向客户端通信的很多内容，Broker 节点向客户端通信的时候，也是通过 Proxy 服务端来转发请求的，而转发请求，和客户端通信的过程中，就会用到 GrpcChannelManager 对象，这些内容现在就先不展开讲解了，我在第十版本代码中为这些额外的内容都添加了非常详细的注释，大家可以自己看一看，等后面有了真正的使用场景，我再为大家讲解这些内容)。  
  
重构到这里也就差不多了，我想大家也是这么认为的，客户端配置信息管理器也有了，客户端 Channel 管理器也有了，那接下来只需要在 ClientActivity 对象处理对应请求的时候使用这两个组件即可。所以，接下来就可以重构 ClientActivity 类，实现 ClientActivity 对象真正处理遥测请求和心跳请求的功能。  
  
重构 ClientActivity 客户端活动处理器  
  
我决定首先实现 ClientActivity 类的 telemetry() 方法，经过上一小节的分析，我们已经知道，就目前的阶段而言，Proxy 服务端处理客户端发送过来的遥测请求时，要执行的操作无非就是缓存客户端传输过来的配置信息，然后把服务端定义的配置信息返回给客户端，这些操作都要 GrpcClientSettingsManager 客户端配置信息管理器来执行。所以，ClientActivity 类的 telemetry() 方法可以执行成下面这样，请看下面代码块。  
上面代码块的内容虽然很多，但是逻辑非常简单，整个流程执行的操作也很清晰： 当 ClientActivity 客户端活动处理器在 telemetry() 方法中处理 TelemetryCommand 遥测请求时，会直接在内部调用的 processAndWriteClientSettings() 方法中判断当前接收到的 TelemetryCommand 遥测请求是生产者客户端发送的，还是消费者客户端发送的。当然，在第十版本代码中，肯定是生产者客户端发送过来的。判断是生产者客户端发送的之后，就会在对应的分支中执行 registerProducer() 方法，该方法就会为当前发送消息过来的客户端创建对应的 GrpcClientChannel 对象，然后就会执行 processClientSettings() 方法，把服务端为客户端定义的配置信息返回给客户端 。这个流程还是很清晰的，只要大家按照顺序阅读以上代码块的内容，肯定能看懂所有的代码逻辑。如果以上逻辑都清楚了，那接下来我们就可以实现 ClientActivity 客户端活动处理器的 heartbeat() 方法了。  
  
当然，虽然我们还没有构建消费者客户端，但按照刚才 telemetry() 方法实现的逻辑来看， 在实现 heartbeat() 方法时，也应该区分是生产者客户端还是消费者客户端发送过来的请求，然后在对应的分支中回复给客户端心跳成功响应即可 。这样分析下来，heartbeat() 方法可以重构成下面这样。  
可以看到，在 heartbeat() 方法中，每次处理生产者客户端发送过来的心跳请求时，仍然会执行 registerProducer() 方法，然后把心跳成功的响应回复给客户端。虽然每次处理请求请求都会执行 registerProducer() 方法，但根据之前实现的代码，大家应该也能意识到， 每次执行 registerProducer() 方法并不会重复为该客户端创建对应的 GrpcClientChannel 对象，该对象只会被创建一次，之后并不会被重复创建 。  
  
到此为止，Proxy 服务端处理客户端发送的心跳请求和遥测请求的功能也实现完毕了，并且服务端也通过 GrpcChannelManager 客户端 Channel 管理器来管理客户端信息，本章内容到此似乎应该结束了，但是大家肯定会觉得意犹未尽，甚至是有些失落，这就结束了？心跳和遥测请求确实处理完毕了，但是 Proxy 服务端管理客户端信息的功能似乎还没实现吧？或者说是一个半成品？就拿最直接的心跳检测来说吧： 通常的做法是服务端管理了客户端的信息之后，会给这些信息设置一个最新更新时间，每当接收到客户端的心跳请求后，都会使用系统当前时间更新客户端信息的最新时间；服务端内部会启动一个线程执行定时任务，定期扫描客户端信息的最新更新时间和系统当前时间的差值是否超过了阈值，如果服务端长时间没有接收到客户端的心跳请求，那么客户端信息的最新更新时间和系统当前时间的差值肯定会超过阈值，这个时候就可以断定服务端和客户端网络连接出问题了，心跳检测失败，那么服务端也就可以把自己管理的客户端信息清除了 。这个是最常规的逻辑，大家应该能理解吧？如果这些逻辑大家都理解了，那肯定会怀疑我们目前构建的程序的正确性，因为目前 Proxy 服务端根本没有体现出这个功能。这就意味着目前的 Proxy 服务端还是不够完善，还需要继续重构。  
  
引入 ClientProcessor、ClientChannelInfo 和 ProducerManager  
  
在 RocketMq 源码中实际上是这样的，虽然服务端每接收到一个客户端连接都会为该客户端创建对应的 GrpcClientChannel 对象，但我必须得解释清楚，这个 GrpcClientChannel 对象其实只是服务端用来和客户端主动通信，并没有真正封装与其对应的客户端的信息。 在源码中有一个 ClientChannelInfo 类，这个类才是真正用来封装每一个客户端的信息 。我把这个类的内容展示在下面代码块中了，请看下面代码块。  
从上面代码块中可以看到，ClientChannelInfo 类中定义了一个 lastUpdateTimestamp 成员变量，这就意味着， Proxy 服务端每次接收到客户端的心跳请求时，就可以更新 ClientChannelInfo 对象中的最后更新时间了 。那现在问题就来了，这个 ClientChannelInfo 对象应该什么时候被创建呢？答案很简单，就在 ClientActivity 类的 registerProducer() 方法中，我把重构之后的 registerProducer() 方法展示在下面代码块中了，请看下面代码块。  
随着上面代码块内容的展示，我相信大家肯定会思考一个新的问题，那就是为每一个客户端创建的 ClientChannelInfo 对象应该存储在哪里呢？ClientChannelInfo 对象封装的才是应该被服务端管理的信息，但这个 ClientChannelInfo 对象应该怎么被管理呢？我相信大家应该都注意到在上面代码块的第 45 行执行了这样一行代码： this.messagingProcessor.registerProducer(ctx, topicName, clientChannelInfo)，这行代码的作用非常明显，那就是把客户端信息注册到生产者管理器中 。很好，现在终于引入生产者管理器这个概念了，在 RocketMq 源码中， Proxy 服务端定义了两个管理器，一个是生产者管理器，一个是消费者管理器，凡是生产者客户端的信息都会存储到生产者管理器中，而所有消费者客户端的信息都会存储到消费者管理器中 。我们可以先不必关心消费者管理器，现在已经创建好了生产者客户端的 ClientChannelInfo 对象，这个对象显然应该被注册到生产者管理器中。 而所谓的注册，其实就是在生产者管理器中定义一个 Map，Map 的 key 为主题信息，value 为对应的客户端信息，这样一来就可以把相同主题的客户端信息都存储到一起，对后续客户端信息的管理和使用有很大的帮助 。除此之外，生产者管理器还有一个非常重要的功能， 那就是在它内部会定义一个扫描过期客户端信息的方法，这个方法会被定时任务定期执行，如果有客户端信息过期了，也就是客户端信息的最新更新时间和系统当前时间的差值超过阈值，那就意味着客户端连接过期，服务端管理的客户端信息也可以被清除了 。  
  
好了，生产者管理器的概念也解释清楚了，接下来就让我们一起实现这个生产者管理器吧，从刚才重构的 registerProducer() 方法的内容来看，被创建出来的 ClientChannelInfo 对象会交给 messagingProcessor 消息处理器注册到生产者管理器中，所以接下来我们就先到 messagingProcessor 消息处理器中看一看，而默认的消息处理器就是 DefaultMessagingProcessor 类，所以我把重构之后的 DefaultMessagingProcessor 类的内容展示出来了，请看下面代码块。  
从上面代码块中可以看到， 在第十版本代码中，我引入了一个 ClientProcessor 客户端处理器，并且把它定义为了 DefaultMessagingProcessor 类的成员变量，而且我还在 DefaultMessagingProcessor 类中定义了一个 registerProducer() 方法，在该方法中，我把 ClientChannelInfo 客户端信息对象交给 ClientProcessor 客户端处理器去注册到生产者管理器中了 。那接下来我就顺着这个脉络，把新引入的 ClientProcessor 客户端处理器展示给大家，请看下面代码块。  
从上面代码块中可以看到，客户端处理器也并没有真的把 ClientChannelInfo 对象注册到生产者管理器中，而是交给 ServiceManager 对象去注册了，而 ServiceManager 是我们之前就引入的接口，它的实现类是 LocalServiceManager，这么来看，在第十版本代码中 LocalServiceManager 类肯定也经过重构了，接下来就请大家看一下重构之后的 LocalServiceManager 类，请看下面代码块。  
从上面代码块中，可以看到， 在 LocalServiceManager 类新添加的 getProducerManager() 方法中，从 brokerController 中得到了 ProducerManager 生产者管理器，绕了半天，兜兜转换，原来还是从 Broker 节点中得到了生产者管理器，还是 Broker 节点管理器客户端信息啊 。当然，我们目前采用的是本地模式部署的 Proxy 模块，在本地模式下，Broker 本来就是内嵌在 Proxy 服务端启动的，所以在这种模式下，把客户端信息存储在 Broker 节点中也没什么不可以。这时候大家也许会思考，如果使用集群模式部署 Proxy 模块，那客户端信息是否就存储在 Proxy 模块本身中了呢？很遗憾并不是这样， 集群模式下 Proxy 模块仍然依赖了 Broker 的 jar 包，仍然是把客户端信息存储在 Broker 中了。当然，这个时候 Broker 并没有在 Proxy 内部真的启动，Proxy 仅仅是依赖了 Broker 的 jar 包而已 ，在另外的服务器中，Broker 节点会单独部署并启动。也许现在大家还不太清楚，但等看到后面就明白了。写到这里我还是忍不住说一句，当我写这篇文章的时候，我已经阅读到 RocketMq 集群模块的源码了，生产消息、消费消息的核心源码全都看完了，我想说的是，其实 5.2.0 版本的源码结构真的有点混乱，并不是说最新版本的代码就一定是最好的，它当然有很多新的功能，但可能是它还在持续迭代中，新旧功能的交替导致不同部分的代码耦合在一起，真的很影响阅读。看到现在，我越来越觉得其实应该以 4.9 版本的源码为模板来更新，可以适当穿插一些最新版本源码的工作做对比，这样可以达到最好的更新效果。当然，大家也不必觉得新版本的源码没有阅读的价值，恰恰相反，它新增加的很多功能都值得研究，只不过是源码阅读起来有些困难而已，我能做的就是尽最大能力写好文章，帮助大家掌握并消化这些知识。  
  
好了，让我们言归正传，最后看看从 Broker 模块中得到的 ProducerManager 生产者管理器是什么样子吧，请看下面代码块。  
上面代码块中的注释非常详细，我就不重复直接了，大家可以看到，生产者管理器除了保存生产者客户端信息，还可以执行它内部的 scanNotActiveChannel() 方法，扫描并清理过期客户端信息。既然是扫描并清理过期客户端信息的操作，那这个方法肯定会被定时任务执行，那定时任务应该怎么定义呢？定义在哪里呢？这个问题就留给大家自己去第十版本代码中查找吧，答案非常简单，我就不在文章中展示了，本篇文章篇幅已经够长了。  
  
除了定时任务执行的问题，其实还有一个小问题， 那就是到目前为止每当一个客户端成功访问了 Proxy 服务端之后，Proxy 会在 GrpcClientSettingsManager、GrpcChannelManager 以及 ProducerManager 生产者管理器中存储对应的客户端信息，但现在的局面时，当服务端与客户端连接断开后，只有 ProducerManager 中的客户端信息会被清除，而 GrpcClientSettingsManager、GrpcChannelManager 中的信息却不会被清除，这显然是程序存在的缺陷，要清除就应该全部清除了 ，这个逻辑可以理解吧？那 GrpcClientSettingsManager、GrpcChannelManager 中的相关客户端信息该怎么清除呢？大家还记得我之前展示 ClientActivity 类时，跟大家说先不解释该类中 init() 方法的作用，并让大家先记住，现在就到了讲解这个 init() 方法的时候了，我把重构之后的 ClientActivity 类展示在下面了，请看下面代码块。  
可以看到，在 ClientActivity 对象的 init() 方法中，我向生产者管理器中注册了一个 ProducerChangeListenerImpl 监听器， 这个监听器中的方法会在生产者客户端发出客户端注销事件后被回调，而回调方法的内容正是清除 GrpcClientSettingsManager、GrpcChannelManager 中的客户端信息 。到此为止，逻辑就串联起来了，大家也应该清楚了 ProducerManager 生产者管理器中 appendProducerChangeListener() 方法和 callProducerChangeListener() 方法的具体用途了吧？当然，这个 ProducerChangeListenerImpl 监听器究竟是怎么被 messagingProcessor 消息处理器注册到生产者管理器中的逻辑我就不展示了，这些逻辑也很简单，就留给大家去第十版本代码中查看吧。  
  
到此为止，本章的内容才算全部结束，大家也可以去阅读我提供的第十版本代码了，我想说的是，虽然本篇文章篇幅非常长，但是还有很多其他的内容没有展示，大家在阅读第十版本代码的时候可以多花点时间，认真看看，内容都很简单，只要有足够的耐性，肯定能全部掌握。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/nchctf5hgpcwoovp*  
*All content belongs to its respective owners and creators.*