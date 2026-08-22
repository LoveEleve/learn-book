大家好，我是**华仔**, 又跟大家见面了。

从今天开始我将为大家奉上 RocketMQ NameServer源码剖析系列文章，正式开启「**RocketMQ 的 NameServer 源码之旅**」，这是第五篇，本篇我们将以「**RocketMQ 5.1.2**」版本为主，来剖析下 RocketMQ 源码之 NameServer 5.X 版本架构设计剖析。

![](https://article-images.zsxq.com/FvVu6GGvvvRm_zxr-dE8n4G5ZsAQ)

## **01 总体概述**

前面几节我们带着这几个问题去探究了 NameServer 的源码设计：

1.  NameServer 启动时需要加载哪些配置以及加载流程如何？
2.  NameServer 启动流程是什么样的？会创建哪些核心数据结构？
3.  NameServer 以什么样的数据结构存储着 Broker 与路由信息的？
4.  Broker 上线、下线、发送心跳这些操作在 NameServer 中是如何进行的？
5.  NameServer 是如何进行 Broker 心跳检测的？

今天我们继续这篇的内容，剖析下「**NameServer 5.x**」版本这些问题时如何解决的。

##   
**02 源码架构差异对比**

## **2.1 5.x 版本源码架构**

![](https://article-images.zsxq.com/FuiXroAWnXTBuUt8s96JocyOf0Qx)

## **2.2 4.9.x 版本源码架构**

![](https://article-images.zsxq.com/Fi7mwhLJTr6JuxJqZPKQb1AXzBsY)

## **2.3 源码对比**

在 5.x 版本中「**NameServer**」和 4.9.x 的源码码有一些变化，[RIP-29](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fapache%2Frocketmq%2Fwiki%2FRIP-29-Optimize-RocketMQ-NameServer) 是 NameServer 的改进增强，主要包括：

1.  添加 Controller 模块嵌入启动逻辑。
2.  添加 [ZoneRouteRPCHook](http://zonerouterpchook/)，支持云特性：多 zone 部署和管理。
3.  不同请求处理分类放在对应的 Processor 中。
4.  [ClientRequestProcessor](http://clientrequestprocessor/)：新增处理客户端请求， 目前包含获取路由信息。
5.  [ClusterTestRequestProcessor](http://clustertestrequestprocessor/)：原有的处理测试请求。
6.  [DefaultRequestProcessor](http://defaultrequestprocessor/)：原有的处理其余 Namesrv 的请求。比如「**KV 配置管理**」、「**Broker 注册**」、「**Broker 心跳**」、「**更新/查询 NameSrv 配置**」等等。
7.  Broker 注册线程池和客户端路由获取线程池隔离。
8.  当前 NameServer 会用同一个线程池和队列去处理所有的客户端路由请求，服务端注册请求等，并且队列的大小和线程数都是不可配置的，如果其中一个类型的请求打爆线程池，将会影响到所有请求。将线程池进行了隔离，将最重要要的客户端路由请求单独隔离出来，队列的大小和线程数均是可配置的。线程池之间的请求处理相互隔离，不受影响。
9.  Topic 路由缓存的优化。
10.  当前 NameServer 当客户端发送路由请求时，会利用 [topicQueueTable](http://topicqueuetable/) 和 [brokerAddrTable](http://brokeraddrtable/) 来构造出最终的路由信息[TopicRouteData](http://topicroutedata/)，这里涉及了在读锁中遍历 broker，有一定的 cpu 耗费。通过构造 [TopicRoute](http://topicroute/) 的缓存[topicRouteDataMap](http://topicroutedatamap/)，直接在客户端请求时返回 [TopicRoute](http://topicroute/)，而额外的代价是在 broker 请求、下线，删除 topic 等行为时同时操作 [topicRouteDataMap](http://topicroutedatamap/)。
11.  批量注销 Broker，增加 [BatchUnRegisterService](http://batchunregisterservice/)，异步化批量处理 Broker下线，加速 Broker下线流程。

## **03 5.x 启动全流程**

在 RocketMQ 5.x 版本中，「**NameServer**」相关的源码都被重构以及封装改造，所以比 4.9.x 版本的源码看起来更加简洁，可读性更强。

目前启动入口仍然是 [NamesrvStartup](http://namesrvstartup/) 启动类，它会调用 [NamesrvController](http://namesrvcontroller/) 类的初始化和启动方法，执行 「**NameServer**」具体模块的初始化和启动。

源码位置：[https://github.com/apache/rocketmq/blob/release-5.1.2/namesrv/src/main/java/org/apache/rocketmq/namesrv/NamesrvStartup.java](https://github.com/apache/rocketmq/blob/release-5.1.2/namesrv/src/main/java/org/apache/rocketmq/namesrv/NamesrvStartup.java)

「**NameServer**」启动需要的配置解释如下：

public class NamesrvStartup {

private final static Logger log \= LoggerFactory.getLogger(LoggerName.NAMESRV\_LOGGER\_NAME);

private final static Logger logConsole \= LoggerFactory.getLogger(LoggerName.NAMESRV\_CONSOLE\_LOGGER\_NAME);

// 临时存储全部的配置 kv，包含 -c 指定的启动文件和 -p 启动的变量

private static Properties properties \= null;

// 从 properties 中解析出来的全部 namesrv 配置

private static NamesrvConfig namesrvConfig \= null;

// 从 properties 中解析出来的全部 Controller 和 Namesrv RPC 服务端启动配置。

// 特别注意 Controller 的配置是 clone 出来的，和 Namesrv 使用的不是同一个对象。

private static NettyServerConfig nettyServerConfig \= null;

// 从 properties 中解析出来的全部 Controller 和 Namesrv RPC 客户端启动配置

private static NettyClientConfig nettyClientConfig \= null;

// 从 properties 中解析出来的全部 Controller 需要的启动配置

private static ControllerConfig controllerConfig \= null;

....

}

##   
**3.1 启动入口**

org.apache.rocketmq.namesrv.NamesrvStartup#main() 方法

public static void main(String\[\] args) {

// 启动 NamesrvController

main0(args);

// 启动 ControllerManager

controllerManagerMain();

}

public static NamesrvController main0(String\[\] args) {

try {

// 解析命令行参数和配置文件

parseCommandlineAndConfigFile(args);

// 创建并启动 NamesrvController

NamesrvController controller \= createAndStartNamesrvController();

return controller;

} catch (Throwable e) {

e.printStackTrace();

System.exit(-1);

}

return null;

}

public static ControllerManager controllerManagerMain() {

try {

// Namesrv 配置中有一个特殊的配置项：enableControllerInNamesrv，默认false。

// 如果设置 enableControllerInNamesrv=true，则 Namesrv 进程会启动一个 Namesrv 服务

// 和 Controller 服务

if (namesrvConfig.isEnableControllerInNamesrv()) {

return createAndStartControllerManager();

}

} catch (Throwable e) {

e.printStackTrace();

System.exit(-1);

}

return null;

}

从上可以看到启动可以分为两块：

1.  启动 NamesrvController，即 main0 方法，该逻辑和4.9.X基本差不多。
2.  启动 Controller 服务，在 5.x 版本中为自动自主切换新增的一个模块，内嵌 NameServer 的时候会启动。

## **3.2 命令行参数解析**

「**NameServer**」启动之前需要先对命令行参数进行解析，将命令行参数解析为 「**NameServer**」启动需要的参数配置。

主要的命令行参数有两个：

  
![](https://article-images.zsxq.com/Fm6ZqsukZkz7vzh15SS-9EGmoynr)

1.  \-c 命令行参数设置配置文件位置，然后将配置文件中的参数值解析设置为配置类的属性值，涉及到的配置有如下几个：
2.  NamesrvConfig （原有）。
3.  NettyServerConfig（原有）。
4.  NettyClientConfig （新增）。
5.  ControllerConfig (新增，只有当 Controller 内嵌 NameServer 的时候才起作用)。
6.  \-p 打印所有配置信息，并退出程序。
7.  其他选项会被转换为 properties 对象，并覆盖 namesrvConfig、nettyServerConfig 和 nettyClientConfig 的属性。

具体直接看源码吧，之前都剖析过（[【NameServer源码分析系列第一篇】图解 RocketMQ 源码之 NameServer 核心配置及加载流程剖析](https://articles.zsxq.com/id_zsw6krkqiajj.html)），这里就不再重复剖析。

![](https://article-images.zsxq.com/FvRagvFdHnXqxy-6uYBFavvq6XC0)

![](https://article-images.zsxq.com/FnxCAhPr7DqIT5r2-dOgxAIEOlc_)

1.  创建一个 [NamesrvConfig](http://namesrvconfig/) 对象，用于存储 namesrv 的配置信息，如 rocketmqHome、kvConfigPath 等。NamesrvConfig 类的具体属性如下表所示：![](https://article-images.zsxq.com/FmZHHH1YakhE0z6p7TmD4Fe_mUzp)
2.  创建一个 [NettyServerConfig](http://nettyserverconfig/) 对象，用于存储 netty 服务端的配置信息，如 listenPort、serverWorkerThreads 等。[NettyServerConfig](http://nettyserverconfig/) 类的具体属性如下表所示：![](https://article-images.zsxq.com/FsMaFyB15Q50LI9dtMJz75uCxfjb)
3.  创建一个 [NettyClientConfig](http://nettyclientconfig/) 对象，用于存储netty客户端的配置信息，如[clientWorkerThreads](http://clientworkerthreads/)、[clientCallbackExecutorThreads](http://clientcallbackexecutorthreads/)等。[NettyClientConfig](http://nettyclientconfig/) 类的具体属性如下表所示：![](https://article-images.zsxq.com/FnBy82jYIQT_OsvIFySDZHBDMNh1)

## **3.3 创建并启动 NamesrvController**

根据 [NamesrvController](http://namesrvcontroller/) 的构造函数创建了三个重要的管理类实例：

1.  KVConfigManager：KV 的持久化、序列化和反序列化处理。
2.  BrokerHousekeepingService：处理客户端和NameServer的连接逻辑，这里的客户端包括：生产者、消费者，以及Broker。
3.  RouteInfoManager：路由管理，主要管理Broker的元数据，Topic的元数据信息。

创建并启动 NamesrvController 的方法是 [namesrv.NamesrvStartup#createAndStartNamesrvController](http://namesrv.namesrvstartup/#createAndStartNamesrvController)：

public static NamesrvController createAndStartNamesrvController() throws Exception {

// 创建 NamesrvController

NamesrvController controller \= createNamesrvController();

// 启动 NamesrvController

start(controller);

// 获取 Netty 服务器配置

NettyServerConfig serverConfig \= controller.getNettyServerConfig();

// 输出启动成功的日志

String tip \= String.format("The Name Server boot success. serializeType=%s, address %s:%d", RemotingCommand.getSerializeTypeConfigInThisServer(), serverConfig.getBindAddress(), serverConfig.getListenPort());

log.info(tip);

System.out.printf("%s%n", tip);

return controller;

}

该方法比较简单，执行流程如下：

1.  调用 createNamesrvController 方法，创建 NamesrvController 对象。
2.  调用 start 方法，传入NamesrvController 对象，启动 NamesrvController 对象。
3.  格式化一个提示信息，包含序列化类型、绑定地址和监听端口信息。
4.  使用 log 对象记录提示信息到日志文件中。
5.  把提示信息打印到控制台中。
6.  返回 NamesrvController 对象。

如果在执行中出现异常，则抛出异常并退出程序。

## **3.3.1 创建 NamesrvController**

「**NameSrvController**」是什么呢？

其实从命名就可以看出这是一个控制器，熟悉 Spring 的童鞋应该不会陌生，Controller 一般用于接受请求，那么 NameServer 接受什么请求呢？

当然是 Broker 的「**注册请求**」、「**心跳请求**」，以及Producer 和 Consumer 的「**拉取路由信息请求**」。

「**NameSrvController**」这个组件，就是 NameServer 专门用来接受 Broker 和客户端的网络请求的一个组件。

![](https://article-images.zsxq.com/FkQstpuBKjo6oK0lC47mbwbAIj41)

public static NamesrvController createNamesrvController() {

// 创建 NamesrvController，传入namesrvConfig、nettyServerConfig、nettyClientConfig配置

final NamesrvController controller \= new NamesrvController(namesrvConfig, nettyServerConfig, nettyClientConfig);

// remember all configs to prevent discard

controller.getConfiguration().registerConfig(properties);

return controller;

}

该方法比较简单，执行流程如下：

1.  创建一个[NamesrvController](http://namesrvcontroller/) 对象，传入[namesrvConfig](http://namesrvconfig/)、[nettyServerConfig](http://nettyserverconfig/)和[nettyClientConfig](http://nettyclientconfig/)对象，这些对象存储了 namesrv 的业务配置和网络配置。
2.  调用 [NamesrvController](http://namesrvcontroller/) 对象的 [getConfiguration](http://getconfiguration%20/) 方法，获取一个 [Configuration](http://configuration/) 对象，该对象负责管理 namesrv 的配置信息。
3.  调用 [Configuration](http://configuration/) 对象的 [registerConfig](http://registerconfig/) 方法，传入 [properties](http://properties/) 对象，将 [properties](http://properties/) 对象中的配置信息注册到[Configuration](http://configuration/) 对象中。
4.  返回 [NamesrvController](http://namesrvcontroller/) 对象。

其实核心就做了一件事情：**解析命令行中的相关参数，然后构建出三个配置对象 ----** 「**NamesrvConfig**」和 「**NettyServerConfig**」和 「**NettyClientConfig**」。其中「**NettyClientConfig**」是 5.x 版本新增的。

我们在启动 NameServer 的时候，是使用 mqnamesrv 命令来启动的，启动的时候可能会在命令行里带入一些参数，所以开头那部分源码，就是解析一下我们传递进去的一些命令行参数而已！

这里最关键的是创建了三个配置对象：

// 初始化 NameServer 配置类，包含 NameServer 的配置，比如 ROCKETMQ\_HOME

namesrvConfig = new NamesrvConfig();

// 初始化 NameServer 网络配置参数（Netty 服务端配置）

nettyServerConfig = new NettyServerConfig();

// 初始化 NameServer 网络配置参数（Netty 客户端配置）

nettyClientConfig = new NettyClientConfig();

// 设置 NameServer 的服务的监听端口号 9876

nettyServerConfig.setListenPort(9876);

1.  **NamesrvConfig：**包含的是 NameServer 自身运行的一些配置参数，NameServer 默认监听请求的端口号是9876，用来接收 Broker 和客户端的请求。
2.  **NettyServerConfig：**包含的是用于接收网络请求的 Netty 服务器的配置参数。
3.  **NettyClientConfig：**包含的是用于接收网络请求的 Netty 客户端的配置参数。

![](https://article-images.zsxq.com/FnnwAZLSavyyPk8AShWvD-lRGVz8)

## **3.3.2 启动 NamesrvController**

[NamesrvController](http://namesrvcontroller/) 对象创建完成后就会进行启动，我们来看下：

public static NamesrvController start(final NamesrvController controller) throws Exception {

if (null == controller) {

throw new IllegalArgumentException("NamesrvController is null");

}

// 初始化controller

boolean initResult \= controller.initialize();

if (!initResult) {

controller.shutdown();

System.exit(-3);

}

// 当jvm关闭的时候，会执行系统中已经设置的所有通过方法addShutdownHook添加的钩子，当系统执行完这些钩子后，jvm才会关闭。所以这些钩子可以在jvm关闭的时候进行内存清理、对象销毁等操作。

Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(log, (Callable<Void>) () -> {

controller.shutdown();

return null;

}));

// 启动controller

controller.start();

return controller;

}

这里三个重点方法：

1.  初始化 controller，如果初始化失败，就调用 [controller#shutdown](http://controller/#shutdown) 方法关闭 controller，并退出程序。
2.  **注册 JVM 钩子函数，如果一个类中使用了线程池，一种优雅的停机方式就是注册一个 JVM 钩子函数，在 JVM 进程关闭之前，先将线程池关闭，释放资源**。
3.  启动 controller。

我们分别来看下 5.x 中与 4.9.x 中有什么异同。

## **3.3.3 初始化 Controller**

源码位置：[https://github.com/apache/rocketmq/blob/release-5.1.2/namesrv/src/main/java/org/apache/rocketmq/namesrv/](https://github.com/apache/rocketmq/blob/release-5.1.2/namesrv/src/main/java/org/apache/rocketmq/namesrv/NamesrvController.java)[NamesrvController](https://github.com/apache/rocketmq/blob/release-5.1.2/namesrv/src/main/java/org/apache/rocketmq/namesrv/NamesrvController.java)[.java](https://github.com/apache/rocketmq/blob/release-5.1.2/namesrv/src/main/java/org/apache/rocketmq/namesrv/NamesrvController.java)

对比 4.9.x 版本的初始化方法，这里进行了封装和重构，使代码更加简洁明了。

public boolean initialize() {

// 加载配置

loadConfig();

// 初始化网络组件

initiateNetworkComponents();

// 初始化两个线程池

initiateThreadExecutors();

// 注册默认请求处理器 DefaultRequestProcessor

registerProcessor();

// 启动定时服务

startScheduleService();

// 初始化 SSL 上下文

initiateSslContext();

// 注册 RPC 钩子

initiateRpcHooks();

return true;

}

我们来分别看下这些方法。

## **3.3.3.1 加载配置**

//加载配置 与 4.9.x 一致

private void loadConfig() {

// 加载 kvConfigPath 下 kvConfig.json 配置文件里的 KV 配置，然后将这些配置放到KVConfigManager#configTable 属性中，KVConfig 配置文件默认路径是 ${user.home}/namesrv/kvConfig.json

this.kvConfigManager.load();

}

从 [NamesrvConfig](http://namesrvconfig/) 对象中保存的 [kvConfigPath](http://kvconfigpath%20/) 指定的文件中加载kv配置，并创建一个 [KVConfigManager](http://kvconfigmanager/) 对象，用于管理和打印 kv 配置。

## **3.3.3.2 初始化网络通信组件**

// 这里比 4.9.x 多了一个 remotingClient 的实例化

private void initiateNetworkComponents() {

// 根据 nettyServerConfig 初始化一个 netty 远程服务器。创建NameServer的netty远程服务

// brokerHousekeepingService 是在 NamesrvController 实例化时构造函数里实例化的，该类负责Broker 连接事件的处理，实现了 ChannelEventListener，主要用来管理 RouteInfoManager#brokerLiveTable

// remotingServer 是一个基于 Netty 的用于 NameServer 与 Broker、Consumer、Producer 进行网络通信的服务端

this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.brokerHousekeepingService);

// 用来向其他服务发送请求或响应

this.remotingClient = new NettyRemotingClient(this.nettyClientConfig);

}

初始化网络通信组件，包括 [remotingClient](http://remotingclient/) 和 [remotingServer](http://remotingserver/)。

1.  [remotingClient](http://remotingclient/) 是一个 [NettyRemotingClient](http://nettyremotingclient/) 对象，它用来向其他服务发送请求或响应。
2.  [remotingServer](http://remotingserver/)是一个 [NettyRemotingServer](http://nettyremotingserver/) 对象，它用来接收和处理来自其他服务的请求或响应。其中[BrokerHousekeepingService](http://brokerhousekeepingservice/) 对象用于处理 broker 的连接和断开事件。

## **3.3.3.3 初始化线程池**

private void initiateThreadExecutors() {

// 初始化默认线程池队列

this.defaultThreadPoolQueue = new LinkedBlockingQueue<>(this.namesrvConfig.getDefaultThreadPoolQueueCapacity());

// 处理默认的远程请求

this.defaultExecutor = new ThreadPoolExecutor(this.namesrvConfig.getDefaultThreadPoolNums(), this.namesrvConfig.getDefaultThreadPoolNums(), 1000 \* 60, TimeUnit.MILLISECONDS, this.defaultThreadPoolQueue, new ThreadFactoryImpl("RemotingExecutorThread\_")) {

@Override

protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {

return new FutureTaskExt<>(runnable, value);

}

};

// 初始化客户端路由信息请求线程池队列

this.clientRequestThreadPoolQueue = new LinkedBlockingQueue<>(this.namesrvConfig.getClientRequestThreadPoolQueueCapacity());

// 处理客户端的路由信息请求

this.clientRequestExecutor = new ThreadPoolExecutor(this.namesrvConfig.getClientRequestThreadPoolNums(), this.namesrvConfig.getClientRequestThreadPoolNums(), 1000 \* 60, TimeUnit.MILLISECONDS, this.clientRequestThreadPoolQueue, new ThreadFactoryImpl("ClientRequestExecutorThread\_")) {

@Override

protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {

return new FutureTaskExt<>(runnable, value);

}

};

}

该方法初始化了两个线程池，如下：

1.  一个是 [defaultExecutor](http://defaultexecutor/)，用来处理默认的远程请求，即处理除了[RequestCode.GET\_ROUTEINFO\_BY\_TOPIC](http://requestcode.get_routeinfo_by_topic/)以外的请求。
2.  另一个是 [clientRequestExecutor](http://clientrequestexecutor/) 线程池，用来处理客户端(生产者和消费者)获取Topic的路由信息请求([RequestCode.GET\_ROUTEINFO\_BY\_TOPIC](http://requestcode.get_routeinfo_by_topic/))。

这两个线程池都使用了 [LinkedBlockingQueue](http://linkedblockingqueue/) 作为任务队列，并且重写了 [newTaskFor](http://newtaskfor/) 方法，使用 [FutureTaskExt](http://futuretaskext/) 包装了 [Runnable](http://runnable/) 任务。

**在 4.9.x 中只有默认的请求处理线程池，而 5.x 版本之后多了一个 clientRequestExecutor 线程池，主要因为增加 NameServer 可用性，即使 defaultExecutor 不能正常工作出现宕机的情况，客户端仍然可以获取 Topic 路由信息而进行线程池的隔离**。具体可以参照[\[RIP-29\]](https://github.com/apache/rocketmq/wiki/RIP-29-Optimize-RocketMQ-NameServer)。

## **3.3.3.4 注册请求处理器**

private void registerProcessor() {

// 根据 isClusterTest() 值，选择使用 ClusterTestRequestProcessor 或者DefaultRequestProcessor 作为默认处理器

if (namesrvConfig.isClusterTest()) {

// ClusterTestRequestProcessor 是一个用于集群测试的处理器，它会在请求前后添加一些环境信息，比如产品环境名称、请求时间等

this.remotingServer.registerDefaultProcessor(new ClusterTestRequestProcessor(this, namesrvConfig.getProductEnvName()), this.defaultExecutor);

} else {

// Support get route info only temporarily

// 在 namesrvConfig.isClusterTest() = false 时如果收到请求的 requestCode 等于 RequestCode.GET\_ROUTEINFO\_BY\_TOPIC 则会使用 ClientRequestProcessor 来处理；当收到其他请求时，会使用DefaultRequestProcessor来处理。

ClientRequestProcessor clientRequestProcessor \= new ClientRequestProcessor(this);

this.remotingServer.registerProcessor(RequestCode.GET\_ROUTEINFO\_BY\_TOPIC, clientRequestProcessor, this.clientRequestExecutor);

// DefaultRequestProcessor 是一个用于正常运行的处理器，它会根据请求的类型，调用不同的方法来处理，比如注册Broker、获取路由信息、更新配置等。

this.remotingServer.registerDefaultProcessor(new DefaultRequestProcessor(this), this.defaultExecutor);

}

}

此部分为 [RIP 29 Optimize RocketMQ NameServer](https://github.com/apache/rocketmq/wiki/RIP-29-Optimize-RocketMQ-NameServer) 中 Thread pool separation 改进。在改进之前 「**NameServer**」使用同一个线程池和队列来处理所有的「**客户端路由请求**」、「**Broker 注册请求**」等，如果其中一种类型的请求爆发，会影响所有的请求。

为了解决这个问题，RIP-29 对处理器进行了线程池隔离，即将最重要的「**客户端路由请求**」单独隔离出来，使用不同的线程池和队列，其中队列大小和线程数都可以配置，这样可以保证不同类型的请求之间不会相互影响。

## **3.3.3.5 启动定时任务**

NamesrvController 初始化会启动定时线程池，其中就包括扫描失效 Broker 的 schedule 线程池。

NamesrvController 启动时会调用 [NamesrvController#startScheduleService](http://namesrvcontroller/#startScheduleService)，启动 namesrv 中的定时任务，其中就包括检查失效 Broker 的 schedule 线程池。

private void startScheduleService() {

// 启动一个定时任务注册心跳机制线程池，首次延迟 5 秒启动，此后每隔 5 秒遍历 RouteInfoManager#brokerLiveTable 执行一次扫描无效的 Broker，并清除 Broker 相关路由信息的任务

this.scanExecutorService.scheduleAtFixedRate(NamesrvController.

this.routeInfoManager::scanNotActiveBroker,

// this.namesrvConfig.getScanNotActiveBrokerInterval() = 5 秒

5, this.namesrvConfig.getScanNotActiveBrokerInterval(), TimeUnit.MILLISECONDS);

// 启动一个定时任务注册打印 KV 配置线程池，延迟 1 分钟启动、此后每 10 分钟执行一次打印出 kvConfig 配置的任务

this.scheduledExecutorService.scheduleAtFixedRate(NamesrvController.

this.kvConfigManager::printAllPeriodically, 1, 10, TimeUnit.MINUTES);

// 每隔 1 秒打印线程池的水位日志，即客户端请求线程池和默认线程池的队列大小和头部任务的慢时间（从创建到执行的时间）

this.scheduledExecutorService.scheduleAtFixedRate(() -> {

try {

NamesrvController.this.printWaterMark();

} catch (Throwable e) {

LOGGER.error("printWaterMark error.", e);

}

}, 10, 1, TimeUnit.SECONDS);

}

这里启动了三个定时任务：

1.  每隔 5 秒时间扫描不活跃的 broker，并清理路由信息「**5.x 版本将周期从10秒改成5秒**」。
2.  每隔 10 分钟打印所有的KV配置信息。
3.  每隔 1 秒打印线程池的水位日志，即客户端请求线程池和默认线程池的队列大小和头部任务的慢时间（从创建到执行的时间）「**5.x 版本新增**」。

## **3.3.3.6 初始化 SSL 上下文**

// rocketmq 可以通过开启 TLS 来提高数据传输的安全性，如果开启了那么需要注册一个监听器来重新加载 SslContext

private void initiateSslContext() {

if (TlsSystemConfig.tlsMode == TlsMode.DISABLED) {

return;

}

String\[\] watchFiles = {TlsSystemConfig.tlsServerCertPath, TlsSystemConfig.tlsServerKeyPath, TlsSystemConfig.tlsServerTrustCertPath};

FileWatchService.Listener listener \= new FileWatchService.Listener() {

boolean certChanged, keyChanged = false;

@Override

public void onChanged(String path) {

if (path.equals(TlsSystemConfig.tlsServerTrustCertPath)) {

LOGGER.info("The trust certificate changed, reload the ssl context");

((NettyRemotingServer) remotingServer).loadSslContext();

}

if (path.equals(TlsSystemConfig.tlsServerCertPath)) {

certChanged = true;

}

if (path.equals(TlsSystemConfig.tlsServerKeyPath)) {

keyChanged = true;

}

if (certChanged && keyChanged) {

LOGGER.info("The certificate and private key changed, reload the ssl context");

certChanged = keyChanged = false;

((NettyRemotingServer) remotingServer).loadSslContext();

}

}

};

try {

fileWatchService = new FileWatchService(watchFiles, listener);

} catch (Exception e) {

LOGGER.warn("FileWatchService created error, can't load the certificate dynamically");

}

}

该方法用来配置 [remotingServer](http://remotingserver/) 使用 TLS 协议进行安全通信。

## **3.3.3.7 初始化 RPC Hook**

**5.x 版本新增内容。**注册RPC钩子，即在 [remotingServer](http://remotingserver/) 处理请求之前或之后执行一些自定义的逻辑。

private void initiateRpcHooks() {

this.remotingServer.registerRPCHook(new ZoneRouteRPCHook());

}

目前只注册了一个 [ZoneRouteRPCHook](http://zonerouterpchook/)，主要用于区域路由。在 [namesrv.route.ZoneRouteRPCHook](http://namesrv.route.zonerouterpchook/) 类中重写了 [doAfterResponse](http://doafterresponse/) 方法，它会在处理请求 [requestCode = RequestCode.GET\_ROUTEINFO\_BY\_TOPIC](http://requestcode%20=%20requestcode.get_routeinfo_by_topic/) 时，根据请求中的 [zoneName](http://zonename/) 参数，过滤掉不属于该区域的 Broker 和 queue 数据，从而实现「**区域隔离**」的功能。

具体来说，[doAfterResponse](http://doafterresponse/) 会设置 response 的 body 为 [namesrv.route.ZoneRouteRPCHook#filterByZoneName](http://namesrv.route.zonerouterpchook/#filterByZoneName) 方法返回值的字节数组格式，[filterByZoneName](http://filterbyzonename%20/) 方法作用是返回过滤掉不属于该区域的 Broker 和 queue 数据后的 [TopicRouteData](http://topicroutedata/) 数据对象。

## **3.3.4 注册 JVM 钩子**

通过 [addShutdownHook](http://addshutdownhook/) 方法注册一个 [ShutdownHookThread](http://shutdownhookthread/) 对象，即 JVM 钩子，用来在程序终止时调用[controller#shutdown](http://controller/#shutdown) 方法，释放资源。

public void shutdown() {

// 远程客户端关闭

this.remotingClient.shutdown();

// 远程服务端关闭

this.remotingServer.shutdown();

// 默认请求处理线程池关闭

this.defaultExecutor.shutdown();

// 客户端路由请求处理线程池关闭

this.clientRequestExecutor.shutdown();

// 定时任务关闭

this.scheduledExecutorService.shutdown();

// 扫描任务关闭

this.scanExecutorService.shutdown();

// 路由信息管理服务关闭

this.routeInfoManager.shutdown();

// 监听 tls 配置文件的变化服务关闭

if (this.fileWatchService != null) {

this.fileWatchService.shutdown();

}

}

## **3.3.5 启动 Controller**

public void start() throws Exception {

// 启动一个 NettyRemotingServer，用于接收和处理客户端的请求。

this.remotingServer.start();

// In test scenarios where it is up to OS to pick up an available port, set the listening port back to config

// 如果nettyServerConfig对象的listenPort属性为0，说明是由操作系统自动分配一个可用端口，那么将remotingServer对象的localListenPort属性赋值给nettyServerConfig对象的listenPort属性，保持一致。

if (0 == nettyServerConfig.getListenPort()) {

nettyServerConfig.setListenPort(this.remotingServer.localListenPort());

}

// 更新本地地址列表，只包含当前机器的IP地址和端口号。

this.remotingClient.updateNameServerAddressList(Collections.singletonList(

NetworkUtil.getLocalAddress()

\+ ":" + nettyServerConfig.getListenPort()));

// 启动一个 NettyRemotingClient，用于向其他服务发送请求

this.remotingClient.start();

// 如果 fileWatchService 对象不为空，调用它的start方法，启动一个文件监视服务，用于动态加载证书文件

if (this.fileWatchService != null) {

this.fileWatchService.start();

}

// 启动一个路由信息管理器，用于维护Broker和Topic的路由关系

this.routeInfoManager.start();

}

启动步骤操作如下：

1.  调用 [remotingServer#start](http://remotingserver/#start) 方法，启动一个 [NettyRemotingServer](http://nettyremotingserver/)，用于接收和处理客户端的请求。
2.  如果[nettyServerConfig](http://nettyserverconfig/)对象的 listenPort 为 0，说明是由操作系统自动分配一个可用端口，那么将 [remotingServer](http://remotingserver/) 对象的 [localListenPort](http://locallistenport/) 属性赋值给 [nettyServerConfig](http://nettyserverconfig/) 对象的 [listenPort](http://listenport/) 属性，保持一致。
3.  调用[remotingClient#updateNameServerAddressList](http://remotingclient/#updateNameServerAddressList)方法，更新本地地址列表，只包含当前机器的IP地址和端口号。
4.  调用 [remotingClient#start](http://remotingclient/#start) 方法，启动一个 [NettyRemotingClient](http://nettyremotingclient/)，用于向其他服务发送请求。
5.  如果[fileWatchService](http://filewatchservice/)对象不为空，调用它的start方法，启动一个文件监视服务，用于动态加载证书文件。
6.  调用 [routeInfoManager#start](http://routeinfomanager/#start) 方法，启动一个路由信息管理器，用于维护Broker和Topic的路由关系。

## **04 路由管理**

## **4.1 路由管理核心组件介绍**

路由管理是指维护 Broker、Topic、Queue 和 Consumer Group 之间的对应关系，以及提供给 Producer 和 Consumer 获取这些关系的服务。

路由管理涉及到以下几个核心组件：

1.  [NameServerController](http://nameservercontroller/)：NameServer 的控制器类，负责初始化、启动和关闭 NameServer 的各个组件。
2.  [RouteInfoManager](http://routeinfomanager/)：NameServer 的核心组件之一，负责维护 Broker、Topic、Queue 和 Consumer Group 的路由信息，以及提供查询、注册和删除的服务。
3.  [BrokerController](http://brokercontroller/)：Broker 的控制器类，负责初始化、启动和关闭 Broker 的各个组件。
4.  [BrokerOuterAPI](http://brokerouterapi/)：Broker 的核心组件之一，负责与 NameServer 通信，定时向 NameServer 注册自身信息，并获取其他 Broker 的信息。
5.  [MQClientInstance](http://mqclientinstance/)：Producer 和 Consumer 的内部实现类，负责管理 Producer 和 Consumer 的信息，并与 NameServer 和 Broker 通信。
6.  [MQClientAPIImpl](http://mqclientapiimpl/)：MQClientInstance 的核心组件之一，负责封装与 NameServer 和 Broker 的通信协议，并执行相应的请求。

> 这里仅围绕 RouteInfoManager 对 RocketMQ 5.1.2 版本路由管理进行分析

## **4.2 RouteInfoManager 路由表管理**

「**NameServer**」的路由管理主要由 RouteInfoManager 对象负责，RouteInfoManager 主要用来管理所有的 Broker、cluster集群、topicQueue 主题队列以及 broker 存活的信息，类关系如下：

  
![](https://article-images.zsxq.com/FmHCKcho17A05m0duulnN2EVRCei)

通过阅读源码可以发现其路由信息主要是由 「**RouteInfoManager**」中的 5 个 HashMap 来维护和存储路由元数据的。它们只会保存在内存中，不会被持久化。下面看一下它们的具体结构。

源码位置：[https://github.com/apache/rocketmq/blob/release-4.9.7/namesrv/src/main/java/org/apache/rocketmq/namesrv/](https://github.com/apache/rocketmq/blob/release-4.9.7/namesrv/src/main/java/org/apache/rocketmq/namesrv/routeinfo%5CRouteInfoManager.java)[routeinfo\\RouteInfoManager.](https://github.com/apache/rocketmq/blob/release-4.9.7/namesrv/src/main/java/org/apache/rocketmq/namesrv/routeinfo%5CRouteInfoManager.java)[java](https://github.com/apache/rocketmq/blob/release-4.9.7/namesrv/src/main/java/org/apache/rocketmq/namesrv/routeinfo%5CRouteInfoManager.java)

public class RouteInfoManager {

....

// Topic 中 Queue 的路由表，消息发送时根据路由表进行 Topic 内的负载均衡，包括broker名称、读写队列的数量等

private final Map<String/\* topic \*/, Map<String, QueueData>> topicQueueTable;

// Broker 基础信息表，包含 brokerName、所属集群名称、broker ID、主备 Broker 地址

private final Map<String/\* brokerName \*/, BrokerData> brokerAddrTable;

// Broker 集群信息，存储集群中所有 Broker 的名称，每个 broker 集群下，包含多个 broker，这里是用 Set 来存储 broker 的名称

private final Map<String/\* clusterName \*/, Set<String/\* brokerName \*/\>> clusterAddrTable;

// Broker 心跳状态信息，每个 broker 地址对应了 broker 的存活信息，存活信息状态包括最后更新的时间戳、注册到 NameServer 时的 Channel 通道、以及 broker 的 HA 地址

private final Map<BrokerAddrInfo/\* brokerAddr \*/, BrokerLiveInfo> brokerLiveTable;

// Broker 上的 FilterServer 列表，用于类模式的消息过滤。在 4.4 之后的版本被废弃

private final Map<BrokerAddrInfo/\* brokerAddr \*/, List<String>/\* Filter Server \*/\> filterServerTable;

// 5.x 版本新增，存储每个 topic 在每个 broker 上的 queue 信息

private final Map<String/\* topic \*/, Map<String/\*brokerName\*/, TopicQueueMappingInfo>> topicQueueMappingInfoTable;

....

public RouteInfoManager(final NamesrvConfig namesrvConfig, NamesrvController namesrvController) {

// 初始化 6 个路由表信息

this.topicQueueTable = new ConcurrentHashMap<>(1024);

this.brokerAddrTable = new ConcurrentHashMap<>(128);

this.clusterAddrTable = new ConcurrentHashMap<>(32);

this.brokerLiveTable = new ConcurrentHashMap<>(256);

this.filterServerTable = new ConcurrentHashMap<>(256);

// 存储每个 topic 在每个 broker 上的 queue 信息 5.x 版本新增

this.topicQueueMappingInfoTable = new ConcurrentHashMap<>(1024);

// 注销 Broker 服务 5.x 版本新增

this.unRegisterService = new BatchUnregistrationService(this, namesrvConfig);

this.namesrvConfig = namesrvConfig;

this.namesrvController = namesrvController;

}

}

这里我们只看下 5.x 版本中新增的部分，其他部分直接点击：[【NameServer源码分析系列第四篇】图解 RocketMQ 源码之RouteInfoManager组件源码设计剖析](https://articles.zsxq.com/id_8boaz51jqig2.html) 。

## **4.2.1 TopicQueueTable 结构**

本结构 5.x 版本并没有做修改和新增，这里直接略过，不再赘述。

## **4.2.2 BrokerAddrTable 结构**

该属性是用来存储 Broker 基础信息表，包含 brokerName、所属集群名称、broker ID、主备 Broker 地址、 「**当前 Broker 所在的 Zone**」， 「**当前 Broker 是否允许 Slave 切换为 Master**」。

### **数据结构：**

HashMap 结构，key是 BrokerName，value 是一个类型是 [BrokerData](http://brokerdata/) 的对象，brokerAddrTable 中的 broker 是一个 broker 组，包含 Master 和 Slave 两部分。[BrokerData](http://brokerdata/) 的数据结构如下，这里可以结合下面 Broker 主从结构逻辑图来理解：

![](https://article-images.zsxq.com/FvRd_yMuu6rWw5sH3RMwJK8IjJve)

![](https://article-images.zsxq.com/FhjBX5Ecti8--GBCNsvzY66LQG2k)

其中 [enableActingMaster](http://enableactingmaster/) 属性在 [RIP 32 Slave Acting Master Mode](https://github.com/apache/rocketmq/wiki/RIP-32-Slave-Acting-Master-Mode) 中被添加，表示「**是否启用 slave acting master 模式**」，用来对旧版本的 HA 适配。

HA 是高可用的缩写，指的是 RocketMQ 的主从架构。旧版本的 HA 指的是「**没有 DLedgerController 模式的主从架构**」，这种架构下，slave 不能提供消息的发送和消费，也不能执行一些 master 才能执行的操作。如果「**启用了 slave acting master 模式**」，当 master 出现故障时，slave 可以承担一些 master 的任务，比如「**读取消息**」、「**扫描和转发特殊消息**」、「**反向同步元数据**」等，这样可以提高可用性和容错性。

## **4.2.3 ClusterAddrTable 结构**

本结构 5.x 版本并没有做修改和新增，这里直接略过，不再赘述。

## **4.2.4 FilterServerTable 结构**

本结构 5.x 版本并没有做修改和新增，这里直接略过，不再赘述。

## **4.2.5 BrokerLiveTable 结构**

该属性是用来存储 Broker 心跳状态信息，每个 broker 地址对应了 broker 的存活信息，存活信息状态包括最后更新的时间戳、注册到 NameServer 时的 Channel 通道、以及 broker 的 HA 地址。

### **数据结构：**

HashMap结构，key是 BrokerAddrInfo 对象地址，value 是 BrokerLiveInfo 结构的该 Broker 信息对象。

brokerLiveTable 中的 broker 是一个 broker 实例，如一个 Master broker 或 一个 Slave broker 实例。BrokerLiveInfo 的数据结构如下：

![](https://article-images.zsxq.com/FjAtg-HiE0p9yQ0-axwZU2OJXYtr)

![](https://article-images.zsxq.com/Fu_aJ66WaS0GlYx-4xsP6VcwUk5k)

## **4.2.6 topicQueueMappingInfoTable 结构**

该属性用来存储每个 topic 在每个 broker 上的queue信息，是「**5.x 版本新增的属性**」。

### **数据结构：**

HashMap 结构，key 是 topic，value 还是一个 HashMap，key为 brokerName，value 为 [TopicQueueMappingInfo](http://topicqueuemappinginfo/)。[TopicQueueMappingInfo](http://topicqueuemappinginfo/) 的数据结构如下：

![](https://article-images.zsxq.com/FhmkTXtSZILvjr_VjppguQGUrdaQ)

![](https://article-images.zsxq.com/Fm5QKhk_1PCfTKrH7ytZGRNSbzSu)

最后来一张图来梳理下这 5 个结构体的关系，如下：

  
![](https://article-images.zsxq.com/lqJOl_ZhbOaNUqjf4uVEK1rIph5P)

## **4.3 路由管理操作**

RouteInfoManager 提供了一系列的方法来更新和查询路由表，大概如下：

1.  [registerBroker](http://registerbroker/)：用于处理broker注册请求，向路由表中添加或更新broker数据、队列数据和过滤器服务器数据，并返回所有主题的队列数据给broker。
2.  [unregisterBroker](http://unregisterbroker/)：用于处理broker注销请求，从路由表中删除指定集群、broker名称和broker地址对应的数据。
3.  [scanNotActiveBroker](http://scannotactivebroker/)：用于定时扫描并删除不活跃的broker数据，判断标准是上次更新时间超过2分钟。
4.  [getAllClusterInfo](http://getallclusterinfo/)：用于获取所有集群信息，返回clusterAddrTable的字符串形式。
5.  [getTopicRouteInfo](http://gettopicrouteinfo/)：用于获取指定主题的路由信息，返回包含broker数据和队列数据的TopicRouteData对象。如果开启了区域模式，还会根据区域名称过滤掉不属于该区域的broker数据和队列数据。
6.  [getSystemTopicList](http://getsystemtopiclist/)：用于获取系统内置主题列表，比如TBW102、OFFSET\_MOVED\_EVENT等。
7.  [getUnitTopics](http://getunittopics/)：用于获取单元化部署的主题列表，即包含“%”符号的主题。
8.  [getHasUnitSubTopicList](http://gethasunitsubtopiclist/)：用于获取存在单元化订阅组的主题列表，即订阅组名称包含“%”符号的主题。
9.  [getHasUnitSubUnUnitTopicList](http://gethasunitsubununittopiclist/)：用于获取存在单元化订阅组但没有单元化部署的主题列表，即订阅组名称包含“%”符号但主题名称不包含“%”符号的主题。

路由管理操作跟 4.9.x 版本的差不多，这里简单来看下几个重要方法。

## **4.3.1 路由注册**

路由注册是通过 Broker 和 NameServer 之间的心跳功能来实现的。主要分为两步：

1.  Broker 启动时向集群中所有 NameServer 发送心跳语句，每隔 30 秒（默认30s，时间间隔在10秒到60秒之间）再发一次。
2.  NameServer 收到心跳包更新 topicQueueTable，brokerAddrTable，brokerLiveTable，clusterAddrTable，filterServerTable。

接下来我们分别来看下。

### **4.3.1.1 Broker 向 NameServer 发送心跳包**

所谓 RocketMQ 路由注册是通过 Broker 与 NameServer 的心跳功能来实现的。当 Broker 启动时，会开启一个定时任务，默认每隔 30s 向集群中的所有 NameServer 发送心跳包信息。

创建了一个线程池注册 Broker，程序启动 10 秒后执行，每隔 30 秒执行一次，默认 30s，时间间隔在 10 秒到 60 秒之间。

源码位置：[https://github.com/apache/rocketmq/blob/release-5.1.2/broker/src/main/java/org/apache/rocketmq/broker/BrokerController.java](https://github.com/apache/rocketmq/blob/release-5.1.2/broker/src/main/java/org/apache/rocketmq/broker/BrokerController.java)

// 添加定时任务方式有所不同

scheduledFutures.add(this.scheduledExecutorService.scheduleAtFixedRate(new AbstractBrokerRunnable(this.getBrokerIdentity()) {

@Override

public void run0() {

try {

if (System.currentTimeMillis() < shouldStartTime) {

BrokerController.LOG.info("Register to namesrv after {}", shouldStartTime);

return;

}

if (isIsolated) {

BrokerController.LOG.info("Skip register for broker is isolated");

return;

}

BrokerController.this.registerBrokerAll(true, false, brokerConfig.isForceRegister());

} catch (Throwable e) {

BrokerController.LOG.error("registerBrokerAll Exception", e);

}

}

}, 1000 \* 10, Math.max(10000, Math.min(brokerConfig.getRegisterNameServerPeriod(), 60000)), TimeUnit.MILLISECONDS));

可以看到 Broker 发送心跳包的定时任务在 [BrokerController#start()](http://brokercontroller/#start\(\)) 方法中启动，每隔 30s 调用 [registerBrokerAll](http://registerbrokerall/) 方法发送一次心跳包（[REGISTER\_BROKER](http://register_broker/) 请求），并将自身的 Topic 队列路由信息发送给 NameServer。

主节点和从节点都会发送心跳和路由信息，Broker 会遍历 NameServer 列表，Broker 依次向每个 NameServer 发送心跳包，发送心跳包的逻辑是采用了 Netty 框架。

另外一个触发 Broker 上报 Topic 配置的操作是修改 Broker 的 Topic 配置（创建/更新），由 [TopicConfigManager](http://topicconfigmanager/) 触发上报。

上报的心跳包请求类型是 [RequestCode.REGISTER\_BROKER](http://requestcode.register_broker/)，当封装 Topic 配置和版本号之后，开始进行实际的路由注册，具体源码如下：

![](https://article-images.zsxq.com/FvWdhZp-J-Ch8CFNeatXUIknIylA)

![](https://article-images.zsxq.com/FtJ_1FlsW1j-nCI3YtAbFXX0AvFu)

源码位置：[https://github.com/apache/rocketmq/blob/release-5.1.2/broker/src/main/java/org/apache/rocketmq/broker/out/BrokerOuterAPI.java](https://github.com/apache/rocketmq/blob/release-5.1.2/broker/src/main/java/org/apache/rocketmq/broker/out/BrokerOuterAPI.java)

/\*\*

\* 向所有 Name server 发送心跳包

\* @return 心跳包发送的响应列表

\*/

public List<RegisterBrokerResult> registerBrokerAll(

final String clusterName,

final String brokerAddr,

final String brokerName,

final long brokerId,

final String haServerAddr,

final TopicConfigSerializeWrapper topicConfigWrapper,

final List<String> filterServerList,

final boolean oneway,

final int timeoutMills,

final boolean enableActingMaster,

final boolean compressed,

final Long heartbeatTimeoutMillis,

final BrokerIdentity brokerIdentity) {

final List<RegisterBrokerResult> registerBrokerResultList = new CopyOnWriteArrayList<>();

// 获取 NameServer 地址列表

List<String> nameServerAddressList = this.remotingClient.getAvailableNameSrvList();

if (nameServerAddressList != null && nameServerAddressList.size() > 0) {

// 为所有心跳请求构造统一的请求头，主要封装 broker 相关信息

final RegisterBrokerRequestHeader requestHeader \= new RegisterBrokerRequestHeader();

requestHeader.setBrokerAddr(brokerAddr);

requestHeader.setBrokerId(brokerId);

requestHeader.setBrokerName(brokerName);

requestHeader.setClusterName(clusterName);

// 主节点地址，初次请求时为空，从节点向 NameServer 注册后更新

requestHeader.setHaServerAddr(haServerAddr);

// 当前 Broker 是否允许 Slave 切换为 Master 5.x 版本新增

requestHeader.setEnableActingMaster(enableActingMaster);

requestHeader.setCompressed(false);

if (heartbeatTimeoutMillis != null) {

requestHeader.setHeartbeatTimeoutMillis(heartbeatTimeoutMillis);

}

// 构造统一的请求体，包括 topic 和 filterServerList 相关信息

RegisterBrokerBody requestBody \= new RegisterBrokerBody();

// Topic 配置，存储 Broker 启动时的一些默认 Topic

requestBody.setTopicConfigSerializeWrapper(TopicConfigAndMappingSerializeWrapper.

from(topicConfigWrapper));

// 消息过滤服务器列表

requestBody.setFilterServerList(filterServerList);

final byte\[\] body = requestBody.encode(compressed);

final int bodyCrc32 \= UtilAll.crc32(body);

requestHeader.setBodyCrc32(bodyCrc32);

// 开启多线程到每个 NameServer 进行注册

final CountDownLatch countDownLatch \= new CountDownLatch(nameServerAddressList.size());

// 遍历所有 NameServer 地址，发送心跳请求

for (final String namesrvAddr : nameServerAddressList) {

brokerOuterExecutor.execute(new AbstractBrokerRunnable(brokerIdentity) {

@Override

public void run0() {

try {

// 实际进行注册方法

RegisterBrokerResult result \= registerBroker(namesrvAddr, oneway, timeoutMills, requestHeader, body);

if (result != null) {

// 封装 NameServer 返回的信息

registerBrokerResultList.add(result);

}

LOGGER.info("Registering current broker to name server completed. TargetHost={}", namesrvAddr);

} catch (Exception e) {

LOGGER.error("Failed to register current broker to name server. TargetHost={}", namesrvAddr, e);

} finally {

countDownLatch.countDown();

}

}

});

}

try {

if (!countDownLatch.await(timeoutMills, TimeUnit.MILLISECONDS)) {

LOGGER.warn("Registration to one or more name servers does NOT complete within deadline. Timeout threshold: {}ms", timeoutMills);

}

} catch (InterruptedException ignore) {

}

}

return registerBrokerResultList;

}

从上面源码来看相对比较简单，步骤如下：

1.  首先获取 NameServer 地址列表。
2.  其次为所有心跳请求封装统一的请求包头和请求体。
3.  然后开启多线程到每个 NameServer 服务器去注册。
4.  最后遍历所有 NameServer 地址，发送心跳请求。

来看下实际的路由注册方法实现，核心源码如下：

private RegisterBrokerResult registerBroker(

final String namesrvAddr,

final boolean oneway,

final int timeoutMills,

final RegisterBrokerRequestHeader requestHeader,

final byte\[\] body

) throws RemotingCommandException, MQBrokerException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,

InterruptedException {

// 创建请求指令，需要注意 RequestCode.REGISTER\_BROKER，NameServer 端的网络处理器会根据requestCode 进行相应的业务处理

RemotingCommand request \= RemotingCommand.createRequestCommand(RequestCode.REGISTER\_BROKER, requestHeader);

request.setBody(body);

// 基于 netty 框架进行网络传输

if (oneway) { // 如果是单向调用，没有返回值，不返回 NameServer 返回结果

try {

this.remotingClient.invokeOneway(namesrvAddr, request, timeoutMills);

} catch (RemotingTooMuchRequestException e) {

// Ignore

}

return null;

}

// 异步调用向 NameServer 发起注册，获取 NameServer 的返回信息

RemotingCommand response \= this.remotingClient.invokeSync(namesrvAddr, request, timeoutMills);

assert response != null;

switch (response.getCode()) {

case ResponseCode.SUCCESS: {

// 获取返回的 reponseHeader

RegisterBrokerResponseHeader responseHeader \=

(RegisterBrokerResponseHeader) response.decodeCommandCustomHeader(RegisterBrokerResponseHeader.class);

// 重新封装返回结果，更新 masterAddr 和 haServerAddr

RegisterBrokerResult result \= new RegisterBrokerResult();

result.setMasterAddr(responseHeader.getMasterAddr());

result.setHaServerAddr(responseHeader.getHaServerAddr());

if (response.getBody() != null) {

result.setKvTable(KVTable.decode(response.getBody(), KVTable.class));

}

return result;

}

default:

break;

}

throw new MQBrokerException(response.getCode(), response.getRemark(), requestHeader == null ? null : requestHeader.getBrokerAddr());

}

从这段源码得出，borker 和 NameServer 之间通过 netty 进行网络传输，Broker 向 NameServer 发起注册时会在请求中添加注册码 [RequestCode.REGISTER\_BROKER](http://requestcode.register_broker/)。

RocketMQ 的每个请求都会定义一个 [requestCode](http://requestcode/)，服务端的网络处理器会根据不同的 [requestCode](http://requestcode/) 进行影响的业务处理。

### **4.3.1.2 NameServer 处理心跳包信息**

Broker 发出路由注册的心跳包之后，NameServer 会根据心跳包中的 [requestCode](http://requestcode/) 进行处理，NameServer 的默认网络处理器是 [DefaultRequestProcessor](http://defaultrequestprocessor/)，当其接收到 [RequestCode.](http://requestcode./)[REGISTER\_BROKER](http://register_broker/) 类型的请求后，将上报的路由信息调用 [RouteInfoManager#registerBroker()](http://routeinfomanager/#registerBroker\(\)) 写入内存中的路由表。

> 这里注册的 broker 是一个 broker 实例而非 broker 组。

![](https://article-images.zsxq.com/Finm0MJWccwf6CoKgDbjfUp_oAW4)

可以看到该方法主要就是判断 [requestCode](http://requestcode/)，如果是 [RequestCode.REGISTER\_BROKER](http://requestcode.register_broker/)，那么确定业务处理逻辑是「**注册 Broker**」。

public RemotingCommand registerBroker(ChannelHandlerContext ctx,

RemotingCommand request) throws RemotingCommandException {

// 获取返回结果

final RemotingCommand response \= RemotingCommand.createResponseCommand(RegisterBrokerResponseHeader.class);

// 响应头

final RegisterBrokerResponseHeader responseHeader \= (RegisterBrokerResponseHeader) response.readCustomHeader();

// 请求头

final RegisterBrokerRequestHeader requestHeader \=

(RegisterBrokerRequestHeader) request.decodeCommandCustomHeader(RegisterBrokerRequestHeader.class);

// 1、解析 requestHeader 并基于 crc32 验签，判断数据是否正确。

if (!checksum(ctx, request, requestHeader)) {

response.setCode(ResponseCode.SYSTEM\_ERROR);

response.setRemark("crc32 not match");

return response;

}

// 2、处理 registerBrokerBody，解析 Topic 信息

TopicConfigSerializeWrapper topicConfigWrapper \= null;

List<String> filterServerList = null;

Version brokerVersion \= MQVersion.value2Version(request.getVersion());

if (brokerVersion.ordinal() >= MQVersion.Version.V3\_0\_11.ordinal()) {

final RegisterBrokerBody registerBrokerBody \= extractRegisterBrokerBodyFromRequest(request, requestHeader);

topicConfigWrapper = registerBrokerBody.getTopicConfigSerializeWrapper();

filterServerList = registerBrokerBody.getFilterServerList();

} else {

// RegisterBrokerBody of old version only contains TopicConfig.

topicConfigWrapper = extractRegisterTopicConfigFromRequest(request);

}

// 调用 RouteInfoManager#registerBroker 来进行 Broker 注册并发送心跳包

RegisterBrokerResult result \= this.namesrvController.getRouteInfoManager().registerBroker(

requestHeader.getClusterName(),

requestHeader.getBrokerAddr(),

requestHeader.getBrokerName(),

requestHeader.getBrokerId(),

requestHeader.getHaServerAddr(),

request.getExtFields().get(MixAll.ZONE\_NAME),

requestHeader.getHeartbeatTimeoutMillis(),

requestHeader.getEnableActingMaster(),

topicConfigWrapper,

filterServerList,

ctx.channel()

);

if (result == null) {

// Register single topic route info should be after the broker completes the first registration.

response.setCode(ResponseCode.SYSTEM\_ERROR);

response.setRemark("register broker failed");

return response;

}

responseHeader.setHaServerAddr(result.getHaServerAddr());

responseHeader.setMasterAddr(result.getMasterAddr());

if (this.namesrvController.getNamesrvConfig().isReturnOrderTopicConfigToBroker()) {

byte\[\] jsonValue = this.namesrvController.getKvConfigManager().getKVListByNamespace(NamesrvUtil.NAMESPACE\_ORDER\_TOPIC\_CONFIG);

response.setBody(jsonValue);

}

response.setCode(ResponseCode.SUCCESS);

response.setRemark(null);

return response;

}

主要步骤分为三步：

1.  首先解析 [requestHeader](http://requestheader/) 并基于 crc32 验签，判断数据是否正确。
2.  接着解析 Topic 信息。
3.  最后调用 [RouteInfoManager#registerBroker](http://routeinfomanager/#registerBroker) 来进行 Broker 注册并发送心跳包。

核心注册以及处理 Broker 心跳信息逻辑是由 [RouteInfoManager#registerBroker](http://routeinfomanager/#registerBroker) 来实现，源码如下：

/\*\*

\* 处理 Broker 心跳信息，存到本地路由表

\* 如果是 SLAVE，则返回 MASTER 的 HA 地址

\*/

public RegisterBrokerResult registerBroker(

final String clusterName,// broker 所属的集群名称

final String brokerAddr,// broker 机器地址

final String brokerName,// broker 名称

final long brokerId,// brokerId

final String haServerAddr,// 跟这个broker互为HA高可用的一个机器地址

final String zoneName, // 当前 Broker 所在的 Zone

final Long timeoutMillis, // 注册超时时间

final Boolean enableActingMaster, // 是否允许 Slave 切换为 Master

final TopicConfigSerializeWrapper topicConfigWrapper, // broker 上面的 Topic 信息

final List<String> filterServerList, // broker 机器上的filter Server 列表

final Channel channel) { // netty channel 网络长连接

// 1、创建一个 RegisterBrokerResult 对象，用于返回注册结果

RegisterBrokerResult result \= new RegisterBrokerResult();

try {

// 2、路由注册需要加写锁，防止并发修改 RouteInfoManager 中的路由表信息

// RouteInfoManager 管理元数据内存结构读的并发一般是大于写的并发的，所以通过读写锁来保证并发安全性的前提下，还可以提升读的性能

this.lock.writeLock().lockInterruptibly();

//init or update the cluster info

// 3、更新集群信息表。根据 Broker 所属的集群名称 clusterName，从 clusterAddrTable 中获取 BrokerName 的集合，然后把 BrokerName 添加进去。

// 此步骤检查 cluster 是否被注册，即 clusterAddrTable 中是否存在 key 等于 clusterName

// 1）、如果 clusterAddrTable 中不存在 clusterName，则创建一个新的 Set 并将其作为 value，并赋值给 brokerNames

// 2）、如果 clusterAddrTable 中存在 clusterName，则直接返回该 value，并赋值给 brokerNames

// 这个computeIfAbsent()是java.util提供的方法，在这里的作用很简单，如果我们传入的clusterName不存在，则执行k -> new HashSet<>()这部分逻辑，这里的含义是为不存在的Key创建默认的空Set作为Value。而返回的brokerNames 就是返回的Set的引用。

Set<String> brokerNames = ConcurrentHashMapUtils.computeIfAbsent((ConcurrentHashMap<String, Set<String>>) this.clusterAddrTable, clusterName, k -> new HashSet<>());

// 把当前 broker 的名称 brokerName 加入到集群对应的 broker 名称集合 brokerNames 中

brokerNames.add(brokerName);

boolean registerFirst \= false;

// 4、维护 brokerAddrTable，根据 brokerName 尝试从 brokerAddrTable 中获取 brokerData 信息，检查 broker 组是否存在

BrokerData brokerData \= this.brokerAddrTable.get(brokerName);

// broker 组不存在第一次维护则创建

if (null == brokerData) {

registerFirst = true; // 设置 registerFirst 设置为 true，表示该 Broker 首次注册

// 创建 BrokerData 对象，HashMap 中的 key 为 brokerId，value 为 brokerAddr

brokerData = new BrokerData(clusterName, brokerName, new HashMap<>());

// 创建 BrokerData，并且维护到 brokerAddrTable 中

this.brokerAddrTable.put(brokerName, brokerData);

}

// 检查 broker 是否是老版本

boolean isOldVersionBroker \= enableActingMaster == null;

// 如果是老版本，则 enableActingMaster 为 null，将 brokerData.enableActingMaster 值设为 false

// 如果不是老版本，则将 brokerData.enableActingMaster 的值设为 true

brokerData.setEnableActingMaster(!isOldVersionBroker && enableActingMaster);

brokerData.setZoneName(zoneName);

// 获取 broker 分组信息

Map<Long, String> brokerAddrsMap = brokerData.getBrokerAddrs();

// 5、添加 brokerId 和 brokerAddr 的映射关系

boolean isMinBrokerIdChanged \= false;

long prevMinBrokerId \= 0;

// 如果 brokerAddrsMap 不为空

if (!brokerAddrsMap.isEmpty()) {

// 将 prevMinBrokerId 设为 brokerAddrsMap 中最小的 brokerId 值，否则设为 0

prevMinBrokerId = Collections.min(brokerAddrsMap.keySet());

}

// 检查正在注册的 brokerId 是否小于 brokerAddrsMap 中的最小 brokerId。

if (brokerId < prevMinBrokerId) {

// 如果是，它将 isMinBrokerIdChanged 设置为 true

isMinBrokerIdChanged = true;

}

//Switch slave to master: first remove <1, IP:PORT> in namesrv, then add <0, IP:PORT>

//The same IP:PORT must only have one record in brokerAddrTable

// 从 brokerAddrsMap 中移除所有 brokerAddr 相等但是 brokerId 不相等的 broker。如果 brokerAddr 相等，但 brokerId 不相等，可能是由于从主切换重新注册，因此需要先移除旧的 broker

brokerAddrsMap.entrySet().removeIf(item -> null != brokerAddr && brokerAddr.equals(item.getValue()) && brokerId != item.getKey());

//If Local brokerId stateVersion bigger than the registering one,

// 6、两种情况直接返回

String oldBrokerAddr \= brokerAddrsMap.get(brokerId);

/\*

\* 这段代码是检查正在注册的 broker 是否与现有 broker 冲突。

\* 如果现有 broker 的状态版本高于正在注册的 broker 的状态版本，说明现有的 broker 是更新的，不应该被覆盖。此时记录警告，并且不会注册 broker，且正在注册的 broker 将从 brokerLiveTable 中删除。这可确保仅使用最新的 broker 信息。

\*/

if (null != oldBrokerAddr && !oldBrokerAddr.equals(brokerAddr)) {

BrokerLiveInfo oldBrokerInfo \= brokerLiveTable.get(new BrokerAddrInfo(clusterName, oldBrokerAddr));

if (null != oldBrokerInfo) {

long oldStateVersion \= oldBrokerInfo.getDataVersion().getStateVersion();

long newStateVersion \= topicConfigWrapper.getDataVersion().getStateVersion();

if (oldStateVersion > newStateVersion) {

log.warn("Registered Broker conflicts with the existed one, just ignore.: Cluster:{}, BrokerName:{}, BrokerId:{}, " +"Old BrokerAddr:{}, Old Version:{}, New BrokerAddr:{}, New Version:{}.",clusterName, brokerName, brokerId, oldBrokerAddr, oldStateVersion, brokerAddr, newStateVersion);

//Remove the rejected brokerAddr from brokerLiveTable.

// 将正在注册的 broker 信息从 brokerLiveTable 中移除

brokerLiveTable.remove(new BrokerAddrInfo(clusterName, brokerAddr));

// 返回不注册/更新 broker 的结果

return result;

}

}

}

/\*

\* 这段代码是检查正在注册的代理是否已在 brokerAddrsMap 中，以及 topicConfigWrapper 是否只有一个 topic 配置。

\* 如果 broker 之前没有注册过，并且只有一个 topic 配置，那么将记录一条警告，指出由于尚未注册 broker，因此无法注册主题配置包装器。此时返回 null，表示注册失败，因为 broker 不允许只有一个 topic 配置就注册。

\*/

if (!brokerAddrsMap.containsKey(brokerId) && topicConfigWrapper.getTopicConfigTable().size() == 1) {

log.warn("Can't register topicConfigWrapper={} because broker\[{}\]={} has not registered.",topicConfigWrapper.getTopicConfigTable(), brokerId, brokerAddr);

return null;

}

// 将当前 broker 信息更新到 brokerAddrsMap 中，即更新 brokerId 和 brokerAddr 的映射关系

String oldAddr \= brokerAddrsMap.put(brokerId, brokerAddr);

registerFirst = registerFirst || (StringUtils.isEmpty(oldAddr));

// 7、判断一个 broker 是否是 master。

boolean isMaster \= MixAll.MASTER\_ID == brokerId;

// 判断一个 broker 是否是 prime slave。prime slave 是一个在 broker 组中拥有最小brokerId 的 slave，它可以在原来的 master 失败时充当 master。

boolean isPrimeSlave \= !isOldVersionBroker && !isMaster

&& brokerId == Collections.min(brokerAddrsMap.keySet());

// 如果不是普通的 slave broker，则进行如下操作

if (null != topicConfigWrapper && (isMaster || isPrimeSlave)) {

ConcurrentMap<String, TopicConfig> tcTable =

topicConfigWrapper.getTopicConfigTable();

// 如果 tcTable 不为空，则遍历 tcTable

if (tcTable != null) {

for (Map.Entry<String, TopicConfig> entry : tcTable.entrySet()) {

// 如果 broker 是第一次注册或者 topicConfig 发生了变化，则更新 topicQueueTable 中的 QueueData

if (registerFirst || this.isTopicConfigChanged(clusterName, brokerAddr,

topicConfigWrapper.getDataVersion(), brokerName,

entry.getValue().getTopicName())) {

final TopicConfig topicConfig \= entry.getValue();

if (isPrimeSlave) {

// Wipe write perm for prime slave

// 擦除 prime slave 的写权限

topicConfig.setPerm(topicConfig.getPerm() & (~PermName.PERM\_WRITE));

}

// 创建/更新 topicQueueTable 中的 QueueData

this.createAndUpdateQueueData(brokerName, topicConfig);

}

}

}

// 如果 broker 是第一次注册或者 broker 实例对应的数据版本和参数中的数据版本不同，则更新 topicQueueMappingInfoTable

if (this.isBrokerTopicConfigChanged(clusterName, brokerAddr, topicConfigWrapper.getDataVersion()) || registerFirst) {

TopicConfigAndMappingSerializeWrapper mappingSerializeWrapper \= TopicConfigAndMappingSerializeWrapper.from(topicConfigWrapper);

Map<String, TopicQueueMappingInfo> topicQueueMappingInfoMap = mappingSerializeWrapper.getTopicQueueMappingInfoMap();

//the topicQueueMappingInfoMap should never be null, but can be empty

for (Map.Entry<String, TopicQueueMappingInfo> entry : topicQueueMappingInfoMap.entrySet()) {

if (!topicQueueMappingInfoTable.containsKey(entry.getKey())) {

topicQueueMappingInfoTable.put(entry.getKey(), new HashMap<>());

}

//Note asset brokerName equal entry.getValue().getBname()

//here use the mappingDetail.bname

topicQueueMappingInfoTable.get(entry.getKey()).put(entry.getValue().getBname(), entry.getValue());

}

}

}

// 8、更新 brokerLiveTable，BrokeLivelnfo 是执行路由删除的重要依据，其中包含最后更新时间

BrokerAddrInfo brokerAddrInfo \= new BrokerAddrInfo(clusterName, brokerAddr);

BrokerLiveInfo prevBrokerLiveInfo \= this.brokerLiveTable.put(brokerAddrInfo,

new BrokerLiveInfo(

System.currentTimeMillis(),

timeoutMillis == null ? DEFAULT\_BROKER\_CHANNEL\_EXPIRED\_TIME : timeoutMillis,

topicConfigWrapper == null ? new DataVersion() : topicConfigWrapper.getDataVersion(),

channel,

haServerAddr));

if (null == prevBrokerLiveInfo) {

log.info("new broker registered, {} HAService: {}", brokerAddrInfo, haServerAddr);

}

// 9、更新 filterServerTable，一个 Broker 可能有多个 Filter Server

if (filterServerList != null) {

if (filterServerList.isEmpty()) {

this.filterServerTable.remove(brokerAddrInfo);

} else {

this.filterServerTable.put(brokerAddrInfo, filterServerList);

}

}

// 10、如果注册过来的此Broker为从节点，则需要查找Broker Master的节点信息并返回，更新对应 master HaAddr 属性

if (MixAll.MASTER\_ID != brokerId) {

String masterAddr \= brokerData.getBrokerAddrs().get(MixAll.MASTER\_ID);

if (masterAddr != null) {

BrokerAddrInfo masterAddrInfo \= new BrokerAddrInfo(clusterName, masterAddr);

BrokerLiveInfo masterLiveInfo \= this.brokerLiveTable.get(masterAddrInfo);

if (masterLiveInfo != null) {

result.setHaServerAddr(masterLiveInfo.getHaServerAddr());

result.setMasterAddr(masterAddr);

}

}

}

if (isMinBrokerIdChanged && namesrvConfig.isNotifyMinBrokerIdChanged()) {

notifyMinBrokerIdChanged(brokerAddrsMap, null,

this.brokerLiveTable.get(brokerAddrInfo).getHaServerAddr());

}

} catch (Exception e) {

log.error("registerBroker Exception", e);

} finally {

// 释放写锁

this.lock.writeLock().unlock();

}

// 返回 RegisterBrokerResult 对象

return result;

}

该方法比较重要，执行步骤如下：

1.  创建一个 RegisterBrokerResult 对象，用于返回注册结果。
2.  加写锁，RouteInfoManager 管理元数据内存结构读的并发一般是大于写的并发的，所以通过读写锁来保证并发安全性的前提下，还可以提升读的性能。
3.  **由于 RouteInfoManager 维护路由信息的数据结构都为 HashMap，存在并发修改的问题，所以要更改路由信息前，需要先加写锁。**
4.  根据 Broker 所属的集群名称 clusterName，从 clusterAddrTable 这个 Map 中获取 BrokerName 的集合，把 BrokerName 添加进去。此步骤检查 cluster 是否被注册，即 clusterAddrTable 中是否存在 key 等于 clusterName
5.  如果 clusterAddrTable 中不存在 clusterName，则创建一个新的 Set 并将其作为 value，并赋值给 brokerNames
6.  如果 clusterAddrTable 中存在 clusterName，则直接返回该 value，并赋值给 brokerNames
7.  维护 brokerAddrTable，根据 brokerName 尝试从 brokerAddrTable 中获取 brokerData 信息并检查 broker 组是否存在
8.  如果是第一次维护的话，registerFirst 设置为 true，然后创建一个新的 BrokerData，并且维护到 brokerAddrTable 中。
9.  如果不是第一次维护的话，则检查存在的 broker 组是否是老版本的 HA 架构。如果是老版本则设置 enableActingMaster 属性为 false，否则设置 enableActingMaster 属性为 true。
10.  维护 broker 地址信息，向 brokerAddrTable 的值 BrokerData 的 brokerAddrsMap 中移除可能冲突的 broker 地址，再把当前 broker 信息更新到 brokerAddrsMap 中，这里分几种情况，如下：
11.  如果 brokerAddrsMap 不为空则将 prevMinBrokerId 设为 brokerAddrsMap 中最小的 brokerId 值，否则设为 0。
12.  检查正在注册的 brokerId 是否小于 brokerAddrsMap 中的最小 brokerId。如果是，它将 isMinBrokerIdChanged 设置为 true。
13.  从 brokerAddrsMap 中移除所有 brokerAddr 相等但是 brokerId 不相等的 broker。如果 brokerAddr 相等，但 brokerId 不相等，可能是由于从主切换重新注册，因此需要先移除旧的 broker。
14.  将当前 broker 信息更新到 brokerAddrsMap 中。
15.  在第 5 步骤中有两种情况需要说明下：
16.  检查正在注册的 broker 是否与现有 broker 冲突，因为一个 broker 实例只能注册一次，如果 brokerId 重复，说明 brokerAddr 也重复，此时需要更新 brokerAddr。如果现有 broker 的状态版本高于正在注册的 broker 的状态版本，说明现有的 broker 是更新的，不应该被覆盖，因此返回不注册/更新 broker 的结果，并将正在注册的 broker 信息从 brokerLiveTable 中移除。
17.  检查正在注册的代理是否已在 brokerAddrsMap 中，以及 topicConfigWrapper 是否只有一个 topic 配置。如果 broker 之前没有注册过，并且只有一个topic配置，此时返回null，表示注册失败，因为 broker 不允许只有一个 topic 配置就注册。
18.  维护 topicQueueTable 和 topicQueueMappingInfoTable。
19.  如果 broker 是第一次注册或者 topicConfig 发生了变化，则更新 topicQueueTable 中的 QueueData。
20.  如果 broker 是第一次注册或者 broker 实例对应的数据版本和参数中的数据版本不同，则更新 topicQueueMappingInfoTable。
21.  维护 brokerLiveTable，BrokeLivelnfo 是执行路由删除的重要依据，其中包含最后更新时间。
22.  维护 filterServerTable，一个 Broker 可能有多个 Filter Server。
23.  维护 result 中主节点的信息。
24.  释放写锁，并返回 RegisterBrokerResult 对象。

**这里相对 4.9.x 版本新增了不少逻辑判断，相对比较复杂，可以对照学习**。

##   
**4.3.2 路由剔除**

当 Broker 挂了，无法向 NameServer 发送心跳包，此时 NameServer 需要把挂掉的 Broker 从路由信息表中删除。

###   
**4.3.2.1 触发条件**

根据前面对 NameServer 启动流程的源码分析，我们知道路由剔除的触发条件主要有两个：

1.  NameServer 会维护一个定时任务，每隔 5s 会扫描一次 brokerLiveTable，如果 [brokerLiveTable#lastUpdateTimestamp](http://brokerlivetable/#lastUpdateTimestamp) 与当前扫描的时间戳进行对比，若超过 120 s，则认为该 Broker 已经无心跳，将失效的 Broker 信息移除，关闭对应的 socket channel，同时更新 topicQueueTable、 brokerAddrTable、clusterAddrTable、brokerLiveTable、filterServerTable。
2.  Broker 正常关闭，触发路由删除，此时会执行 [unregisterBroker](http://unregisterbroker/) 指令。

无论哪种方式，其最终都是调用到 [RouteInfoManager#unRegisterBroker](http://routeinfomanager/#unRegisterBroker%20) 方法来处理。

接下来我们分别来看下。

### **4.3.2.2 NameServer 定时任务触发**

如果某个 Broker 在 NameServer 注册之后又因自身或者网络问题下线了，NameServer 不做处理的话就会被其他的组件当成有效的 Broker，即使这个 Broker 已经挂掉了。

所以，定期对 Broker 的状态进行判断、再根据状态执行对应的清理操作是很有必要的。

我们知道，Broker 会「**每隔 30 秒**」向 NameServer 发送心跳，而 NameServer 端的检查逻辑则是「**每隔 5 秒**」执行一次，所以根据上次心跳到当前时间超过 120 s 就可以判定 broker 是否失效，如下：

  
![](https://article-images.zsxq.com/FlnKMfEnx-A96leLyW_cSOqut9Lx)

![](https://article-images.zsxq.com/FvGTZVjMppysrzl8UwkD42UOB4bb)

![](https://article-images.zsxq.com/FodUxpgQMM-RCa1vG1bhEP_LdiZV)

// 线程池里只有一个线程实例，利用此线程池能极大地减少系统资源地开销，因为扫描 broker 本身不需要过多的资源，开启一个线程足以。

private final ScheduledExecutorService scanExecutorService \= new ScheduledThreadPoolExecutor(1,

new BasicThreadFactory.Builder().namingPattern("NSScanScheduledThread")

.daemon(true).build());

// 每隔 5s 进行扫描判断是否存在失效的 Broker，如果存在则移除失效 broker

public void scanNotActiveBroker() {

try {

log.info("start scanNotActiveBroker");

// 迭代 brokerLiveTable

for (Entry<BrokerAddrInfo, BrokerLiveInfo> next : this.brokerLiveTable.entrySet()) {

// 获取上一次 Broker 更新时间

long last \= next.getValue().getLastUpdateTimestamp();

// 获取心跳超时时间 120 s

long timeoutMillis \= next.getValue().getHeartbeatTimeoutMillis();

// 判断是否超时未续约心跳，如果上一次更新与当前扫描的时间戳进行对比，若超过 120 s，则认为该 Broker 无效

if ((last + timeoutMillis) < System.currentTimeMillis()) {

// 关闭通道

RemotingHelper.closeChannel(next.getValue().getChannel());

log.warn("The broker channel expired, {} {}ms", next.getKey(), timeoutMillis);

// 执行路由剔除操作，删除注册的集群、Broker 信息

this.onChannelDestroy(next.getKey());

}

}

} catch (Exception e) {

log.error("scanNotActiveBroker exception", e);

}

}

可以得出线程池里只有一个线程实例，利用此线程池能极大地减少系统资源地开销，因为扫描 broker 本身不需要过多的资源，开启一个线程足以。

不过「**5.x 版本中**」将扫描失效 Broker 跟其他定时任务执行线程隔离。

![](https://article-images.zsxq.com/FjQHYrcU04Bjf9VyM2V9Lbt9zsAl)

  
![](https://article-images.zsxq.com/Fga7y55HSr_A9TyX9ZPm_FYIbBwl)

最后执行关闭与 Broker 的连接。

public void onChannelDestroy(BrokerAddrInfo brokerAddrInfo) {

// 注销 Broker 请求头

UnRegisterBrokerRequestHeader unRegisterRequest \= new UnRegisterBrokerRequestHeader();

// 是否需要注销

boolean needUnRegister \= false;

// brokerAddrInfo 不为空

if (brokerAddrInfo != null) {

try {

try {

// 加读锁

this.lock.readLock().lockInterruptibly();

// 构造注销 Broker 请求数据

// 获取需要删除的信息，这个方法只是将需要删除的信息加入到 unRegisterRequest

needUnRegister = setupUnRegisterRequest(unRegisterRequest, brokerAddrInfo);

} finally { // 释放读锁

this.lock.readLock().unlock();

}

} catch (Exception e) {

log.error("onChannelDestroy Exception", e);

}

}

// 有需要删除的信息

if (needUnRegister) {

// 提交注销 Broker 请求

boolean result \= this.submitUnRegisterBrokerRequest(unRegisterRequest);

log.info("the broker's channel destroyed, submit the unregister request at once, " +

"broker info: {}, submit result: {}", unRegisterRequest, result);

}

}

// 构造注销 Broker 请求数据

private boolean setupUnRegisterRequest(UnRegisterBrokerRequestHeader unRegisterRequest,

BrokerAddrInfo brokerAddrInfo) {

// 设置集群名称

unRegisterRequest.setClusterName(brokerAddrInfo.getClusterName());

// 设置 Broker 地址

unRegisterRequest.setBrokerAddr(brokerAddrInfo.getBrokerAddr());

for (Entry<String, BrokerData> stringBrokerDataEntry : this.brokerAddrTable.entrySet()) {

BrokerData brokerData \= stringBrokerDataEntry.getValue();

if (!brokerAddrInfo.getClusterName().equals(brokerData.getCluster())) {

continue;

}

for (Entry<Long, String> entry : brokerData.getBrokerAddrs().entrySet()) {

Long brokerId \= entry.getKey();

String brokerAddr \= entry.getValue();

// 从路由缓存中查找否要注销的 Broker 信息

if (brokerAddr.equals(brokerAddrInfo.getBrokerAddr())) {

// 设置 BrokerName

unRegisterRequest.setBrokerName(brokerData.getBrokerName());

// 设置 BrokerID

unRegisterRequest.setBrokerId(brokerId);

// 找到后设置需要注销为 true

return true;

}

}

}

// 找不到后设置需要注销为 false

return false;

}

// 提交注销 Broker 请求

public boolean submitUnRegisterBrokerRequest(UnRegisterBrokerRequestHeader unRegisterRequest) {

// 做法：将需要删除的信息放入一个阻塞队列就完事了，然后会有线程从这个队列拉取数据进行真正的删除操作。

return this.unRegisterService.submit(unRegisterRequest);

}

在「**5.x 版本中**」这个提案 [RIP 29 Optimize RocketMQ NameServer](https://github.com/apache/rocketmq/wiki/RIP-29-Optimize-RocketMQ-NameServer) 中添加了 [BatchUnregistrationService](http://batchunregistrationservice/) 类用来批量注销 Broker。

从上面的源码可以看出注销 Broker 请求也是交由 [BatchUnregistrationService](http://batchunregistrationservice/)[#submit](http://#submit) 方法进行提交的，压入 [BatchUnregistrationService#unregistrationQueue](http://batchunregistrationservice/#unregistrationQueue) 队列中，而 BatchUnregistrationService 继承了 ServiceThead，在 RouteInfoManager 构建时创建，RouteInfoManager 启动时也会启动[BatchUnregistrationService](http://batchunregistrationservice/)，它是一个「**守护线程**」，在 while 死循环中获取 broker 注销请求，处理逻辑源码如下：

public class BatchUnregistrationService extends ServiceThread {

private final RouteInfoManager routeInfoManager;

// 注销 Broker 请求队列

private BlockingQueue<UnRegisterBrokerRequestHeader> unregistrationQueue;

// 初始化服务

public BatchUnregistrationService(RouteInfoManager routeInfoManager, NamesrvConfig namesrvConfig) {

this.routeInfoManager = routeInfoManager;

this.unregistrationQueue = new LinkedBlockingQueue<>(namesrvConfig.getUnRegisterBrokerQueueCapacity());

}

/\*\*

\* 提交注销 Broker 请求到队列中

\* Submits an unregister request to this queue.

\* @param unRegisterRequest the request to submit

\* @return {@code true} if the request was added to this queue, else {@code false}

\*/

public boolean submit(UnRegisterBrokerRequestHeader unRegisterRequest) {

// 放入阻塞队列

return unregistrationQueue.offer(unRegisterRequest);

}

@Override

public void run() {

while (!this.isStopped()) {

try {

// 调用 take 方法会进行阻塞，也就是有数据才往下走，没有数据就阻塞。

final UnRegisterBrokerRequestHeader request \= unregistrationQueue.take();

Set<UnRegisterBrokerRequestHeader> unregistrationRequests = new HashSet<>();

// 拿到请求后，将 queue 中的所有数据转移到 unregistrationRequests 中来。利用 take+drainTo 方法来完成取一次取所有的操作，这波操作很6！

unregistrationQueue.drainTo(unregistrationRequests);

// Add polled request

// 将第一个请求放入进来

unregistrationRequests.add(request);

// 做真正的删除操作

this.routeInfoManager.unRegisterBroker(unregistrationRequests);

} catch (Throwable e) {

log.error("Handle unregister broker request failed", e);

}

}

}

}

> 这里有一个编程小技巧，JUC 的阻塞队列中并没有提供从队列中批量获取对象的阻塞方法。BatchUnregistrationService 中批量获取注销请求对象先使用 take 方法从队列中获取一个对象，如果队列为空，take 方法阻塞。如果 take 方法返回一个注销请求，说明阻塞队列中的对象不空，再调用 drainTo 方法尝试将队列中的对象放入到 SET 中。
> 
> 这样写代码同时满足了阻塞和批量获取注销请求对象的需求。

[BatchUnregistrationService](http://batchunregistrationservice/) 获取到注销请求后，会将注销请求发送给 [RouteInfoManager#unRegisterBroker](http://routeinfomanager/#unRegisterBroker)，路由注销逻辑都是在这个方法中。

public void unregisterBroker(

final String clusterName,

final String brokerAddr,

final String brokerName,

final long brokerId) {

UnRegisterBrokerRequestHeader unRegisterBrokerRequest \= new UnRegisterBrokerRequestHeader();

unRegisterBrokerRequest.setClusterName(clusterName);

unRegisterBrokerRequest.setBrokerAddr(brokerAddr);

unRegisterBrokerRequest.setBrokerName(brokerName);

unRegisterBrokerRequest.setBrokerId(brokerId);

// 注销 Broker 请求

unRegisterBroker(Sets.newHashSet(unRegisterBrokerRequest));

}

public void unRegisterBroker(Set<UnRegisterBrokerRequestHeader> unRegisterRequests) {

try {

Set<String> removedBroker = new HashSet<>();

Set<String> reducedBroker = new HashSet<>();

Map<String, BrokerStatusChangeInfo> needNotifyBrokerMap = new HashMap<>();

// 1、加写锁

this.lock.writeLock().lockInterruptibly();

// 2、循环遍历要注销的请求，并将要注销 broker 从 brokerLiveTable 中删除。

for (final UnRegisterBrokerRequestHeader unRegisterRequest : unRegisterRequests) {

final String brokerName \= unRegisterRequest.getBrokerName();

final String clusterName \= unRegisterRequest.getClusterName();

final String brokerAddr \= unRegisterRequest.getBrokerAddr();

BrokerAddrInfo brokerAddrInfo \= new BrokerAddrInfo(clusterName, brokerAddr);

// 将要注销 broker 从 brokerLiveTable 中删除。

BrokerLiveInfo brokerLiveInfo \= this.brokerLiveTable.remove(brokerAddrInfo);

log.info("unregisterBroker, remove from brokerLiveTable {}, {}",

brokerLiveInfo != null ? "OK" : "Failed",

brokerAddrInfo

);

// 3、从 filterServerTable 中删除要注销的 broker

this.filterServerTable.remove(brokerAddrInfo);

boolean removeBrokerName \= false;

boolean isMinBrokerIdChanged \= false;

// 4、根据 brokerName 从 brokerAddrTable 中获取 BrokerData

BrokerData brokerData \= this.brokerAddrTable.get(brokerName);

if (null != brokerData) {

// 如果当前 brokerId 是 brokerData 中最小的 brokerId，则将 isMinBrokerIdChanged 设置为 true

if (!brokerData.getBrokerAddrs().isEmpty() &&

unRegisterRequest.getBrokerId().equals(

Collections.min(brokerData.getBrokerAddrs().keySet()))) {

isMinBrokerIdChanged = true;

}

// 删除 brokerData 中要注销下线的 broker 地址，即 brokerData 中和 brokerAddr 相同的地址

boolean removed \= brokerData.getBrokerAddrs().entrySet().removeIf(item -> item.getValue().equals(brokerAddr));

log.info("unregisterBroker, remove addr from brokerAddrTable {}, {}",

removed ? "OK" : "Failed",

brokerAddrInfo

);

// 如果 BrokerData 中存活的 broker 地址为空，说明这个 brokerName 已经没有broker 实例了，需要从 brokerAddrTable 中移除

if (brokerData.getBrokerAddrs().isEmpty()) {

// 则会从 brokerAddrTable 中删除 BrokerData

this.brokerAddrTable.remove(brokerName);

log.info("unregisterBroker, remove name from brokerAddrTable OK, {}",

brokerName

);

// 都注销完成标识

removeBrokerName = true;

} else if (isMinBrokerIdChanged) { // 如果最小的 brokerId 发生了变化，需要通知其他broker更新路由信息

// 如果要注销的 Broker 是主节点，还会将当前 broker 的信息放入到needNotifyBrokerMap 中。

needNotifyBrokerMap.put(brokerName, new BrokerStatusChangeInfo(

brokerData.getBrokerAddrs(), brokerAddr, null));

}

}

// 5、如果集群中的所有 broker 都已经注销，则会将 clusterAddrTable 中的集群信息删除。

if (removeBrokerName) {

Set<String> nameSet = this.clusterAddrTable.get(clusterName);

if (nameSet != null) {

// 删除brokerName

boolean removed \= nameSet.remove(brokerName);

log.info("unregisterBroker, remove name from clusterAddrTable {}, {}",

removed ? "OK" : "Failed",

brokerName);

// 如果集群中所有 broker 都注销，则删除集群信息

if (nameSet.isEmpty()) {

this.clusterAddrTable.remove(clusterName);

log.info("unregisterBroker, remove cluster from clusterAddrTable {}",

clusterName

);

}

}

removedBroker.add(brokerName);

} else {

reducedBroker.add(brokerName);

}

}

// 6、清理要注销 Broker 关联的 Topic 信息

cleanTopicByUnRegisterRequests(removedBroker, reducedBroker);

// 7、如果 broker 的主节点宕机，并且在 namesrv 中开启了主节点宕机通知其他节点的配置，就会通知当前 BrokerName 的其他节点。

if (!needNotifyBrokerMap.isEmpty() && namesrvConfig.isNotifyMinBrokerIdChanged()) {

// 如果最小的 brokerId 发生了变化，需要通知其他 broker 更新路由信息

notifyMinBrokerIdChanged(needNotifyBrokerMap);

}

} catch (Exception e) {

log.error("unregisterBroker Exception", e);

} finally {

this.lock.writeLock().unlock();

}

}

路由踢除整体逻辑主要分为 8 步：

1.  加写锁。
2.  循环遍历要注销的请求，并将要注销 broker 从 brokerLiveTable 中删除。
3.  从 filterServerTable 中删除要注销的 broker。
4.  更新 brokerAddrTable。
5.  把 brokerData 中和传入的 brokerAddr 相同的地址移除。
6.  经过上述操作后如果 brokerAddrs 为空，说明这个 brokerName 已经没有 broker 实例了，则将 brokerAddrTable 中 key 为传入的 brokerName 的元素删除，并将 removeBrokerName 设置为 true。
7.  如果集群中的所有 broker 都已经注销，则会将 clusterAddrTable 中的集群信息删除。
8.  如果 removeBrokerName 为 true，则删除 clusterAddrTable 中 key 为传入的 clusterName 的值 Set 中和传入的 brokerName 相等的元素。如果删除元素后此时 Set 集合为空，则将此 cluster 删除。并将删除的 brokerName 添加到 removedBroker 集合中。
9.  如果 removeBrokerName 为 false，则将传入的 brokerName 添加到 reducedBroker 集合中。
10.  清理要注销 Broker 关联的 Topic 信息。
11.  如果 broker 的主节点宕机，并且在 namesrv 中开启了主节点宕机通知其他节点的配置，就会通知当前 BrokerName 的其他节点。
12.  释放写锁。

这里重点看两个方法，即第六步和第七步。

**清理 TopicQueueTable 无效数据**

// 注销后，清理 topicQueueTable 中的无效数据

private void cleanTopicByUnRegisterRequests(Set<String> removedBroker, Set<String> reducedBroker) {

Iterator<Entry<String, Map<String, QueueData>>> itMap = this.topicQueueTable.entrySet().iterator();

// 遍历所有的 topic

while (itMap.hasNext()) {

Entry<String, Map<String, QueueData>> entry = itMap.next();

String topic \= entry.getKey();

// 遍历 topic 对应的 brokerName 和 QueueData

Map<String, QueueData> queueDataMap = entry.getValue();

// 遍历需要移除的 brokerName

for (final String brokerName : removedBroker) {

// 移除这个 brokerName 对应的 QueueData

final QueueData removedQD \= queueDataMap.remove(brokerName);

if (removedQD != null) {

log.debug("removeTopicByBrokerName, remove one broker's topic {} {}", topic, removedQD);

}

}

// 如果 topic 对应的 brokerName 都移除了，那么就移除这个 topic

if (queueDataMap.isEmpty()) {

log.debug("removeTopicByBrokerName, remove the topic all queue {}", topic);

itMap.remove();

}

// 遍历需要减少的 brokerName

for (final String brokerName : reducedBroker) {

final QueueData queueData \= queueDataMap.get(brokerName);

// 如果是主节点，则会把 brokerAddrTable 置为不可写状态

if (queueData != null) {

// 如果这个 brokerName 对应的 brokerData 开启了自动切换 master 的功能，那么就需要判断这个 brokerName 对应的 brokerData 中是否还有 master

if (this.brokerAddrTable.get(brokerName).isEnableActingMaster()) {

// Master has been unregistered, wipe the write perm

if (isNoMasterExists(brokerName)) {

// 如果主节点已经被注销，则 broker 状态改成不可写

queueData.setPerm(queueData.getPerm() & (~PermName.PERM\_WRITE));

}

}

}

}

}

}

**异步通知当前 BrokerName 的其他节点**

主节点宕机通知其他节点的配置是 [notifyMinBrokerIdChanged](http://notifyminbrokeridchanged/)，默认是不开启。

/\*\*

\* 通知最小 BrokerId 发生了变化的方法，向非最小 BrokerId 的 Broker 地址发送通知

\*

\* @param needNotifyBrokerMap 需要通知的Broker信息映射，Key为Broker名称，Value为Broker状态变化信息

\* @throws InterruptedException 当线程被中断时抛出

\* @throws RemotingConnectException 当网络连接异常时抛出

\* @throws RemotingTimeoutException 当远程调用超时时抛出

\* @throws RemotingSendRequestException 当发送请求失败时抛出

\* @throws RemotingTooMuchRequestException 当请求过多时抛出

\*/

private void notifyMinBrokerIdChanged(Map<String,BrokerStatusChangeInfo> needNotifyBrokerMap)

throws InterruptedException,RemotingConnectException,RemotingTimeoutException,

RemotingSendRequestException,RemotingTooMuchRequestException {

for (String brokerName :needNotifyBrokerMap.keySet()) {

BrokerStatusChangeInfo brokerStatusChangeInfo \= needNotifyBrokerMap.get(brokerName);

BrokerData brokerData \= brokerAddrTable.get(brokerName);

if (brokerData != null && brokerData.isEnableActingMaster()) {

// 调用重载方法向非最小 BrokerId 的地址发送通知

notifyMinBrokerIdChanged(brokerStatusChangeInfo.getBrokerAddrs(),

brokerStatusChangeInfo.getOfflineBrokerAddr(),

brokerStatusChangeInfo.getHaBrokerAddr());

}

}

}

/\*\*

\* 向非最小 BrokerId 的 Broker 地址通知最小 BrokerId 发生了变化

\*

\* @param brokerAddrMap BrokerId和对应的地址映射关系

\* @param offlineBrokerAddr 下线的Broker地址

\* @param haBrokerAddr 高可用Broker地址

\* @throws InterruptedException 当线程被中断时抛出

\* @throws RemotingSendRequestException 当发送请求失败时抛出

\* @throws RemotingTimeoutException 当远程请求超时时抛出

\* @throws RemotingTooMuchRequestException 当请求过多时抛出

\* @throws RemotingConnectException 当网络连接异常时抛出

\*/

private void notifyMinBrokerIdChanged(Map<Long,String> brokerAddrMap,String offlineBrokerAddr,

String haBrokerAddr)

throws InterruptedException,RemotingSendRequestException,RemotingTimeoutException,

RemotingTooMuchRequestException,RemotingConnectException {

if (brokerAddrMap == null || brokerAddrMap.isEmpty() || this.namesrvController == null) {

return;

}

// 构造最小 BrokerId 变更通知请求头

NotifyMinBrokerIdChangeRequestHeader requestHeader \= new NotifyMinBrokerIdChangeRequestHeader();

long minBrokerId \= Collections.min(brokerAddrMap.keySet());

requestHeader.setMinBrokerId(minBrokerId);

requestHeader.setMinBrokerAddr(brokerAddrMap.get(minBrokerId));

requestHeader.setOfflineBrokerAddr(offlineBrokerAddr);

requestHeader.setHaBrokerAddr(haBrokerAddr);

// 选择通知的 Broker 地址，即非最小 BrokerId 的 Broker 地址

List<String> brokerAddrsNotify = chooseBrokerAddrsToNotify(brokerAddrMap,offlineBrokerAddr);

log.info("min broker id changed to {},notify {},offline broker addr {}",minBrokerId,brokerAddrsNotify,offlineBrokerAddr);

RemotingCommand request \=

RemotingCommand.createRequestCommand(

RequestCode.NOTIFY\_MIN\_BROKER\_ID\_CHANGE,requestHeader);

for (String brokerAddr :brokerAddrsNotify) {

// 向 Broker 发送通知请求

this.namesrvController.getRemotingClient().invokeOneway(brokerAddr,request,300);

}

}

/\*\*

\* 从给定的 brokerAddrMap 中选择需要通知的 Broker 地址列表

\* @param brokerAddrMap BrokerId 和 BrokerAddr 的映射关系

\* @param offlineBrokerAddr 下线的 Broker 地址

\* @return 需要通知的 Broker 地址列表

\*/

private List<String> chooseBrokerAddrsToNotify(Map<Long,String> brokerAddrMap,String offlineBrokerAddr) {

// 如果 offlineBrokerAddr 不为空，或者 brokerAddrMap 中只有一个 Broker 地址时，通知所有的 Broker 地址

if (offlineBrokerAddr != null || brokerAddrMap.size() == 1) {

// 通知全部 Broker 地址

return new ArrayList<>(brokerAddrMap.values());

}

// 如果有新的 Broker 注册，则通知之前的 Broker

long minBrokerId \= Collections.min(brokerAddrMap.keySet());

List<String> brokerAddrList = new ArrayList<>();

for (Long brokerId :brokerAddrMap.keySet()) {

// 将不是最小 BrokerId 的 Broker 地址添加到通知列表中

if (brokerId != minBrokerId) {

brokerAddrList.add(brokerAddrMap.get(brokerId));

}

}

// 返回需要通知的 Broker 地址列表

return brokerAddrList;

}

![](https://article-images.zsxq.com/FsEuMSRp5yoI7h2YMgOx3wfXrqK-)

至此，**NameServer 定时任务触发，接下来我们来看下 Broker 正常关闭触发。**

### **4.2.3.3 Broker 正常关闭触发**

![](https://article-images.zsxq.com/FiZA4LWsamgtVPf8pZVfnkGA4V5u)

![](https://article-images.zsxq.com/FhYU60blOlCHJt9QNeaB5ERcH-Bb)

/\*\*

\* 从所有NameServer注销指定的Broker

\*

\* @param clusterName 集群名称

\* @param brokerAddr Broker地址

\* @param brokerName Broker名称

\* @param brokerId BrokerId

\*/

public void unregisterBrokerAll(

final String clusterName,

final String brokerAddr,

final String brokerName,

final long brokerId

) {

// 获取所有NameServer的地址列表

List<String> nameServerAddressList = this.remotingClient.getNameServerAddressList();

// 遍历所有NameServer地址进行注销操作

if (nameServerAddressList != null) {

for (String namesrvAddr :nameServerAddressList) {

try {

// 调用注销Broker的方法

this.unregisterBroker(namesrvAddr,clusterName,brokerAddr,brokerName,brokerId);

LOGGER.info("unregisterBroker OK,NamesrvAddr:{}",namesrvAddr);

} catch (Exception e) {

LOGGER.warn("unregisterBroker Exception,NamesrvAddr:{}",namesrvAddr,e);

}

}

}

}

可以看到最终还是调用了 [RouteInfoManager#unRegisterBroker](http://routeinfomanager/#unRegisterBroker)，上面已经剖析过了，这里就不再赘述。

## **4.3.3 路由发现**

RocketMQ 的路由发现是非实时的，当 topic 路由信息发生变化后，NameServer 并不会主动推送给客户端，而是等待客户端定期到 NameServer 主动拉取 Topic 最新路由信息。

这种设计方式大大降低了 NameServer 实现的复杂性。

### **4.3.3.1 Producer 主动拉取**

Producer 端在启动后会开启一系列定时任务，其中有一个任务就是定期从 NameServer 获取 Topic 路由信息。入口是 [MQClientInstance#startScheduledTask()](http://mqclientinstance/#start-ScheduledTask\(\))，核心源码如下：

private void startScheduledTask() {

....

this.scheduledExecutorService.scheduleAtFixedRate(() -> {

try {

// 从 nameserver 更新最新的 topic 路由信息

MQClientInstance.this.updateTopicRouteInfoFromNameServer();

} catch (Exception e) {

log.error("ScheduledTask updateTopicRouteInfoFromNameServer exception", e);

}

}, 10, this.clientConfig.getPollNameServerInterval(), TimeUnit.MILLISECONDS);

....

}

/\*\*

\* 从NameServer更新Topic的路由信息

\*

\* @param topic Topic名称

\* @param isDefault 是否是默认Topic

\* @param defaultMQProducer 默认的MQProducer实例

\* @return 更新是否成功

\*/

public boolean updateTopicRouteInfoFromNameServer(final String topic,boolean isDefault,DefaultMQProducer defaultMQProducer) {

try {

// 尝试获取锁

if (this.lockNamesrv.tryLock(LOCK\_TIMEOUT\_MILLIS,TimeUnit.MILLISECONDS)) {

try {

TopicRouteData topicRouteData;

// 如果是默认Topic且defaultMQProducer不为空，则从NameServer获取默认Topic的路由信息

if (isDefault && defaultMQProducer != null) {

topicRouteData = this.mQClientAPIImpl.getDefaultTopicRouteInfoFromNameServer(clientConfig.getMqClientApiTimeout());

if (topicRouteData != null) {

// 更新队列的读写数量为defaultMQProducer的默认读写队列数

for (QueueData data :topicRouteData.getQueueDatas()) {

int queueNums \= Math.min(defaultMQProducer.getDefaultTopicQueueNums(),data.getReadQueueNums());

data.setReadQueueNums(queueNums);

data.setWriteQueueNums(queueNums);

}

}

} else {

// 从NameServer获取指定Topic的路由信息

topicRouteData = this.mQClientAPIImpl.getTopicRouteInfoFromNameServer(topic,clientConfig.getMqClientApiTimeout());

}

if (topicRouteData != null) {

// 获取旧的路由信息

TopicRouteData old \= this.topicRouteTable.get(topic);

// 判断路由信息是否发生改变

boolean changed \= topicRouteData.topicRouteDataChanged(old);

if (!changed) {

// 如果路由信息未发生改变，则检查是否需要更新Topic的路由信息

changed = this.isNeedUpdateTopicRouteInfo(topic);

} else {

log.info("the topic\[{}\] route info changed,old\[{}\] ,new\[{}\]",topic,old,topicRouteData);

}

if (changed) {

// 更新Broker地址表

for (BrokerData bd :topicRouteData.getBrokerDatas()) {

this.brokerAddrTable.put(bd.getBrokerName(),bd.getBrokerAddrs());

}

// 更新Topic的Endpoint映射表

{

ConcurrentMap<MessageQueue,String> mqEndPoints = topicRouteData2EndpointsForStaticTopic(topic,topicRouteData);

if (!mqEndPoints.isEmpty()) {

topicEndPointsTable.put(topic,mqEndPoints);

}

}

// 更新Producer信息

{

TopicPublishInfo publishInfo \= topicRouteData2TopicPublishInfo(topic,topicRouteData);

publishInfo.setHaveTopicRouterInfo(true);

for (Entry<String,MQProducerInner> entry :this.producerTable.entrySet()) {

MQProducerInner impl \= entry.getValue();

if (impl != null) {

impl.updateTopicPublishInfo(topic,publishInfo);

}

}

}

// 更新Consumer信息

if (!consumerTable.isEmpty()) {

Set<MessageQueue> subscribeInfo = topicRouteData2TopicSubscribeInfo(topic,topicRouteData);

for (Entry<String,MQConsumerInner> entry :this.consumerTable.entrySet()) {

MQConsumerInner impl \= entry.getValue();

if (impl != null) {

impl.updateTopicSubscribeInfo(topic,subscribeInfo);

}

}

}

// 更新Topic的路由信息表

TopicRouteData cloneTopicRouteData \= new TopicRouteData(topicRouteData);

log.info("topicRouteTable.put.Topic = {},TopicRouteData\[{}\]",topic,cloneTopicRouteData);

this.topicRouteTable.put(topic,cloneTopicRouteData);

return true;

}

} else {

log.warn("updateTopicRouteInfoFromNameServer,getTopicRouteInfoFromNameServer return null,Topic:{}.\[{}\]",topic,this.clientId);

}

} catch (MQClientException e) {

if (!topic.startsWith(MixAll.RETRY\_GROUP\_TOPIC\_PREFIX) && !topic.equals(TopicValidator.AUTO\_CREATE\_TOPIC\_KEY\_TOPIC)) {

log.warn("updateTopicRouteInfoFromNameServer Exception",e);

}

} catch (RemotingException e) {

log.error("updateTopicRouteInfoFromNameServer Exception",e);

throw new IllegalStateException(e);

} finally {

// 释放锁

this.lockNamesrv.unlock();

}

} else {

log.warn("updateTopicRouteInfoFromNameServer tryLock timeout {}ms.\[{}\]",LOCK\_TIMEOUT\_MILLIS,this.clientId);

}

} catch (InterruptedException e) {

log.warn("updateTopicRouteInfoFromNameServer Exception",e);

}

return false;

}

![](https://article-images.zsxq.com/Fo69vc1wlnC9APKbA3jchDcChJd1)

![](https://article-images.zsxq.com/Fr0b7uLEkPkfXK-5FX3d_GSVxWoZ)

Producer 端 和 NameServer 端之间通过 netty 进行网络传输，producer 端向 NameServer 端发起的请求中添加注册码 [RequestCode.GET\_ROUTEINFO\_BY\_TOPIC](http://requestcode.get_routeinfo_by_topic/)。

###   
**4.3.3.2 NameServer 返回路由信息**

NameServer 收到 Producer 端发送的请求后，会根据请求中的 [requestCode](http://requestcode/) 进行处理。处理 [requestCode](http://requestcode/) 在「**5.x 版本中**」是在客户端网络处理器 [ClientRequestProcessor](http://defaultrequestprocessor/) 中进行处理，最终通过[RouteInfoManager#pickupTopicRouteData()](http://routeinfomanager/#pickupTopicRouteData\(\)) 来实现。

  
![](https://article-images.zsxq.com/Fs0dHYbVL1lJtWq0OejaGAOSgUl7)

###   
**TopicRouteData 结构**

在剖析源码前，我们先看下 NameServer 端返回给 Producer 端的数据结构。通过源码可以看到，返回的是一个TopicRouteData 对象，具体结构如下。

源码位置：[https://github.com/apache/rocketmq/blob/release-5.1.2/remoting/src/main/java/org/apache/rocketmq/remoting/protocol/route/TopicRouteData.java](https://github.com/apache/rocketmq/blob/release-5.1.2/remoting/src/main/java/org/apache/rocketmq/remoting/protocol/route/TopicRouteData.java)

public class TopicRouteData extends RemotingSerializable {

// 顺序消息配置内容，来自kvConfig

private String orderTopicConf;

// topic 的队列元数据

private List<QueueData> queueDatas;

// topic 存储的 broker 信息列表

private List<BrokerData> brokerDatas;

// Broker 上过滤服务器的地址列表

private HashMap<String/\* brokerAddr \*/, List<String>/\* Filter Server \*/\> filterServerTable;

//It could be null or empty

// 5.x 版本新增，存储每个 topic 在每个 broker 上的 queue 信息

private Map<String/\*brokerName\*/, TopicQueueMappingInfo> topicQueueMappingByBroker;

....

}

了解了返回给 Producer 端的 [TopicRouteData](http://topicroutedata/) 结构后，我们来看下[RouteInfoManager#pickupTopicRouteData](http://routeinfomanager/#pickupTopicRouteData) 方法具体如何实现。

### **pickupTopicRouteData 根据 Topic 选择路由信息**

/\*\*

\* 获取指定主题的路由数据

\* @param topic 主题名称

\* @return TopicRouteData 对象，包含了与指定主题相关的路由信息

\*/

public TopicRouteData pickupTopicRouteData(final String topic) {

// 初始化 Topic 路由数据

TopicRouteData topicRouteData \= new TopicRouteData();

boolean foundQueueData \= false;

boolean foundBrokerData \= false;

// 初始化 brokerData 集合

List<BrokerData> brokerDataList = new LinkedList<>();

topicRouteData.setBrokerDatas(brokerDataList);// 设置 BrokerData 列表

HashMap<String,List<String>> filterServerMap = new HashMap<>();

topicRouteData.setFilterServerTable(filterServerMap);// 设置 FilterServerMap

try {

// 加读锁

this.lock.readLock().lockInterruptibly();

// 从元数据 topicQueueTable 中根据 topic 名字获取队列集合

Map<String,QueueData> queueDataMap = this.topicQueueTable.get(topic);

if (queueDataMap != null) {

// 将获取到的队列集合写入 topicRouteData 的 queueDatas 中

topicRouteData.setQueueDatas(new ArrayList<>(queueDataMap.values()));

foundQueueData = true;

Set<String> brokerNameSet = new HashSet<>(queueDataMap.keySet());

// 根据 QueueData 集合中 brokerName 属性从 brokerAddrTable 取得 brokerData 对象，组成brokerDatas，遍历从 QueueData 集合中提取的 brokerName

for (String brokerName :brokerNameSet) {

// 根据 brokerName 从 brokerAddrTable 获取 brokerData

BrokerData brokerData \= this.brokerAddrTable.get(brokerName);

if (null == brokerData) {

continue;

}

// 克隆 brokerData 对象，并写入到 topicRouteData 的 brokerDatas 中

BrokerData brokerDataClone \= new BrokerData(brokerData);

// 添加 BrokerData 到 brokerDataList 中

brokerDataList.add(brokerDataClone);

foundBrokerData = true;

if (filterServerTable.isEmpty()) {

continue;

}

// 根据查询到的brokerData从filterServerTable查询到对应的filterServer列表，组装为Map

for (final String brokerAddr :brokerDataClone.getBrokerAddrs().values()) {

BrokerAddrInfo brokerAddrInfo \= new BrokerAddrInfo(brokerDataClone.getCluster(),brokerAddr);

// 根据 brokerAddr 获取 filterServerList，封装后写入到topicRouteData 的 filterServerTable 中

List<String> filterServerList = this.filterServerTable.get(brokerAddrInfo);

// 设置 FilterServerMap 中的筛选服务器列表

filterServerMap.put(brokerAddr,filterServerList);

}

}

}

} catch (Exception e) {

log.error("pickupTopicRouteData Exception",e);

} finally {

// 释放读锁

this.lock.readLock().unlock();

}

log.debug("pickupTopicRouteData {} {}",topic,topicRouteData);

if (foundBrokerData && foundQueueData) {

// 设置主题与 Broker 之间的映射关系

topicRouteData.setTopicQueueMappingByBroker(this.topicQueueMappingInfoTable.get(topic));

// 如果不支持 acting master，则直接返回

if (!namesrvConfig.isSupportActingMaster()) {

return topicRouteData;

}

// 如果是同步主题或者 BrokerData 列表为空或者 QueueData 列表为空，则直接返回

if (topic.startsWith(TopicValidator.SYNC\_BROKER\_MEMBER\_GROUP\_PREFIX)) {

return topicRouteData;

}

if (topicRouteData.getBrokerDatas().size() == 0 ||

topicRouteData.getQueueDatas().size() == 0) {

return topicRouteData;

}

boolean needActingMaster \= false;

for (final BrokerData brokerData :topicRouteData.getBrokerDatas()) {

if (brokerData.getBrokerAddrs().size() != 0

&& !brokerData.getBrokerAddrs().containsKey(MixAll.MASTER\_ID)) {

needActingMaster = true;

break;

}

}

if (!needActingMaster) {

return topicRouteData;

}

// 如果需要自动切换 master，则执行下面的逻辑

for (final BrokerData brokerData :topicRouteData.getBrokerDatas()) {

final HashMap<Long,String> brokerAddrs = brokerData.getBrokerAddrs();

if (brokerAddrs.size() == 0 || brokerAddrs.containsKey(MixAll.MASTER\_ID) || !brokerData.isEnableActingMaster()) {

continue;

}

// 对没有主节点的情况进行处理

for (final QueueData queueData :topicRouteData.getQueueDatas()) {

if (queueData.getBrokerName().equals(brokerData.getBrokerName())) {

// 如果queueData的perm不可写，那么就将brokerAddrs中brokerId最小的brokerAddr的brokerId改为masterId

if (!PermName.isWriteable(queueData.getPerm())) {

final Long minBrokerId \= Collections.min(brokerAddrs.keySet());

final String actingMasterAddr \= brokerAddrs.remove(minBrokerId);

brokerAddrs.put(MixAll.MASTER\_ID,actingMasterAddr);

}

break;

}

}

}

return topicRouteData;

}

return null;

}

该方法主要是根据 topic 名找到该 topic 下所有 Queue 在 Broker 上的分布信息，在「**5.x 版本中**」比 4.9.x 多了需要重新设置 Master 的地址的相关逻辑。

## **05 网络请求处理器**

在「**5.x 版本中**」，DefaultRequestProcessor 处理器主要处理除「**获取 Topic 路由**」以外的 NameServer 收到的网络请求，而将「**获取 Topic 路由**」请求处理交由 ClientRequestProcessor 处理器来处理，对应的方法都是 processRequest，源码如下：

// 根据不同的请求类型调用不同的处理方法，并返回处理结果

public class DefaultRequestProcessor implements NettyRequestProcessor {

private static Logger log \= LoggerFactory.getLogger(LoggerName.NAMESRV\_LOGGER\_NAME);

protected final NamesrvController namesrvController;

public DefaultRequestProcessor(NamesrvController namesrvController) {

this.namesrvController = namesrvController;

}

@Override

// 如果 ChannelHandlerContext 不为空，则记录接收到的请求信息

if (ctx != null) {

log.debug("receive request, {} {} {}", request.getCode(), RemotingHelper.parseChannelRemoteAddr(ctx.channel()), request);

}

// 根据请求类型进行处理

switch (request.getCode()) {

case RequestCode.PUT\_KV\_CONFIG:

return this.putKVConfig(ctx, request); // 调用putKVConfig处理配置信息的存储

case RequestCode.GET\_KV\_CONFIG:

return this.getKVConfig(ctx, request); // 调用getKVConfig处理获取配置信息

case RequestCode.DELETE\_KV\_CONFIG:

return this.deleteKVConfig(ctx, request); // 调用deleteKVConfig方法处理删除配置信息

case RequestCode.QUERY\_DATA\_VERSION:

return this.queryBrokerTopicConfig(ctx, request); // 调用queryBrokerTopicConfig处理查询Broker主题配置

case RequestCode.REGISTER\_BROKER:

return this.registerBroker(ctx, request); // 调用registerBroker方法处理注册Broker

case RequestCode.UNREGISTER\_BROKER:

return this.unregisterBroker(ctx, request); // 调用unregisterBroker方法处理取消注册Broker

case RequestCode.BROKER\_HEARTBEAT:

return this.brokerHeartbeat(ctx, request); // 调用brokerHeartbeat方法处理Broker心跳

case RequestCode.GET\_BROKER\_MEMBER\_GROUP:

return this.getBrokerMemberGroup(ctx, request); // 调用getBrokerMemberGroup方法处理获取Broker成员组

case RequestCode.GET\_BROKER\_CLUSTER\_INFO:

return this.getBrokerClusterInfo(ctx, request); // 调用getBrokerClusterInfo方法处理获取Broker集群信息

case RequestCode.WIPE\_WRITE\_PERM\_OF\_BROKER:

return this.wipeWritePermOfBroker(ctx, request); // 调用wipeWritePermOfBroker方法处理擦除Broker的写权限

case RequestCode.ADD\_WRITE\_PERM\_OF\_BROKER:

return this.addWritePermOfBroker(ctx, request); // 调用addWritePermOfBroker方法处理添加Broker的写权限

case RequestCode.GET\_ALL\_TOPIC\_LIST\_FROM\_NAMESERVER:

return this.getAllTopicListFromNameserver(ctx, request); // 调用getAllTopicListFromNameserver方法处理从Nameserver获取所有主题列表

case RequestCode.DELETE\_TOPIC\_IN\_NAMESRV:

return this.deleteTopicInNamesrv(ctx, request); // 调用deleteTopicInNamesrv方法处理在Namesrv中删除主题

case RequestCode.REGISTER\_TOPIC\_IN\_NAMESRV:

return this.registerTopicToNamesrv(ctx, request); // 调用registerTopicToNamesrv方法处理在Namesrv中注册主题

case RequestCode.GET\_KVLIST\_BY\_NAMESPACE:

return this.getKVListByNamespace(ctx, request); // 调用getKVListByNamespace方法处理根据命名空间获取KV列表

case RequestCode.GET\_TOPICS\_BY\_CLUSTER:

return this.getTopicsByCluster(ctx, request); // 调用getTopicsByCluster方法处理根据集群获取主题列表

case RequestCode.GET\_SYSTEM\_TOPIC\_LIST\_FROM\_NS:

return this.getSystemTopicListFromNs(ctx, request); // 调用getSystemTopicListFromNs方法处理从Namesrv获取系统主题列表

case RequestCode.GET\_UNIT\_TOPIC\_LIST:

return this.getUnitTopicList(ctx, request); // 调用getUnitTopicList方法处理获取单元化主题列表

case RequestCode.GET\_HAS\_UNIT\_SUB\_TOPIC\_LIST:

return this.getHasUnitSubTopicList(ctx, request); // 调用getHasUnitSubTopicList方法处理获取具有单元化订阅组的主题列表

case RequestCode.GET\_HAS\_UNIT\_SUB\_UNUNIT\_TOPIC\_LIST:

return this.getHasUnitSubUnUnitTopicList(ctx, request); // 调用getHasUnitSubUnUnitTopicList方法处理获取既有单元化订阅组又有非单元化订阅组的主题列表

case RequestCode.UPDATE\_NAMESRV\_CONFIG:

return this.updateConfig(ctx, request); // 调用updateConfig方法处理更新Namesrv的配置信息

case RequestCode.GET\_NAMESRV\_CONFIG:

return this.getConfig(ctx, request); // 调用getConfig方法处理获取Namesrv的配置信息

case RequestCode.GET\_CLIENT\_CONFIG:

return this.getClientConfigs(ctx, request); // 调用getClientConfigs方法处理获取客户端配置信息

default:

String error \= " request type " + request.getCode() + " not supported";

return RemotingCommand.createResponseCommand(

RemotingSysResponseCode.REQUEST\_CODE\_NOT\_SUPPORTED, error); // 如果请求类型不支持，返回不支持的请求类型错误响应

}

....

}

public class ClientRequestProcessor implements NettyRequestProcessor {

private static Logger log \= LoggerFactory.getLogger(LoggerName.NAMESRV\_LOGGER\_NAME);

protected NamesrvController namesrvController;

private long startupTimeMillis;

private AtomicBoolean needCheckNamesrvReady \= new AtomicBoolean(true);

public ClientRequestProcessor(final NamesrvController namesrvController) {

this.namesrvController = namesrvController;

this.startupTimeMillis = System.currentTimeMillis();

}

@Override

public RemotingCommand processRequest(final ChannelHandlerContext ctx,

final RemotingCommand request) throws Exception {

// 调用 getRouteInfoByTopic 方法处理根据 Topic 获取路由信息

return this.getRouteInfoByTopic(ctx, request);

}

....

}

可以看到，就是根据不同的请求类型调用对应的处理逻辑，这里不关注具体处理细节，只看下有请求类型RequestCode：

1.  KV配置相关的（添加、获取、删除）
2.  **注册、注销 Broker**
3.  **路根据 Topic 获取路由信，交给 ClientRequestProcessor 处理**
4.  TOPIC 相关的（获取所有 topic 列表、删除 topic、获取系统 topic 等等）
5.  获取以及更新 NameServer 配置

我们看到有一个注册 Broker 的请求，该请求肯定是在 Broker 启动时由 Broker 端调用来把自己注册到 NameServer 的。而根据 topic 获取路由信息肯定就是用于路由发现，如 Producer 发消息时进行调用实现负载均衡。

## **06 5.x 与4.9.x 配置对比**

RocketMQ 5.x 「**NameServer**」是在 4.9.X 基础上添加了「**内嵌 Controller 实例**」的功能，其余功能差不多。

RocketMQ 5.0 和 4.9.X 配置项差异如下：

![](https://article-images.zsxq.com/FqqZctmQTIQrfvprpOSlyPW6GHVa)

## **RocketMQ Namesrv 5.x 配置以及默认值**

![](https://article-images.zsxq.com/Fp1dxGgNl3wd6L3khPfkpLtxCBA5)

## **RocketMQ Namesrv 4.9.x 配置以及默认值**

其中空值是比 5.x 少的

![](https://article-images.zsxq.com/Fs_OT6P-QDkGiv2Y1jFtEunGhGvg)