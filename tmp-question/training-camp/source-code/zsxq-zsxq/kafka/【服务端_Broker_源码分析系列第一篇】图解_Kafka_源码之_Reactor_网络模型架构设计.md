大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**Java NIO 多路复用**」 的核心实现原理，今天我们就来深度聊聊「**Broker Reactor 网络模型**」架构设计，看看 Kafka 的超高并发的网络模型架构究竟是如何实现的。

![](https://article-images.zsxq.com/Fg4AtCtq-LhdIvKPi26GVfoi6h_4)

## **01 总体概述**

[【原理分析系列第八篇】图解 Kafka 超高并发网络架构演进过程](https://articles.zsxq.com/id_de63boucthq8.html) 在这篇原理分析篇中，带大家一步步对 Kafka 的网络架构进行了演进优化，最终得出了 Kafka 的网络架构是由 「**Java NIO 多路复用**」、「**Reactor 主从多线程网络模型**」组成的，接下来会逐一讲解说明。

为了方便大家理解，所有的源码只保留骨干。

## **02 网络通信层架构**

在深入学习 Kafka 各个网络通信组件之前，我们先从整体上来看下 Kafka 服务端的网络通信架构，如下图所示：

![](https://article-images.zsxq.com/lojiZJNqUko4zFwqFJ8m4NNg9614)

从图中我们可以得出，Kafka 的网络通信组件主要由两大部分组成：「**SocketServer**」、「**KafkaRequestHandlerPool**」。

其中 SocketServer 组件类是 Kafka 超高并发网络通信层中最重要的子模块。它主要实现了 Reactor 设计模式，包含 「**Acceptor 线程**」、「**Processor 线程**」和 「**RequestChannel**」等对象，都是网络通信的重要组成部分，但是它们不做请求处理，只是请求和响应的 「**搬运工**」。

而 RequestHandlerPool 组件才是真正执行请求的线程池，即 I/O 工作线程池，里面定义了若干个 I/O 线程，主要用来执行真实的请求处理逻辑。

接下来我们分别来看下这2个组件的内部组成。

## **03 SocketServer 初探**

SocketServer 组件类的源码在 Kafka 源码包的 core 包下，具体的github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/SocketServer.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/SocketServer.scala)

它主要是用来接收客户端 Socket 请求连接，并处理服务端与客户端网络 I/O 的核心实现类。

点开上面源码链接，折叠方法后可以看出，该组件类包含8个部分，如下图：

![](https://article-images.zsxq.com/FhvjdhoPraLn5a-W4cGpwMBpsmx8)

这里我先来看看 SocketServer 这个类的重要字段。

class SocketServer(val config: KafkaConfig,

val metrics: Metrics,

val time: Time,

val credentialProvider: CredentialProvider)

extends Logging with KafkaMetricsGroup with BrokerReconfigurable {

// SocketServer的请求队列长度，由 Broker 端参数 queued.max.requests 值而定，默认值是500

private val maxQueuedRequests \= config.queuedMaxRequests

...

// data-plane 即数据面的 Processor 线程类集合。

private val dataPlaneProcessors \= new ConcurrentHashMap\[Int, Processor\]()

// 处理数据类请求的 Acceptor 线程集合，一个 EndPoint 对应一个 Acceptor 线程

private\[network\] val dataPlaneAcceptors \= new ConcurrentHashMap\[EndPoint, Acceptor\]()

// 处理数据类请求的 RequestChannel 对象，多个 Processor 会共享一个 RequestChannel，请求队列长度为 500。

val dataPlaneRequestChannel \= new RequestChannel(maxQueuedRequests, DataPlaneMetricPrefix, time)

// control-plane 用于处理控制类请求的Processor线程

private var controlPlaneProcessorOpt : Option\[Processor\] = None

// 处理控制类请求的 Acceptor 线程集合

private\[network\] var controlPlaneAcceptorOpt : Option\[Acceptor\] = None

// 处理控制类请求专属的 RequestChannel 对象，请求队列长度为 20，这里的队列长度要远远小于数据类请求。

val controlPlaneRequestChannelOpt: Option\[RequestChannel\] = config.controlPlaneListenerName.map(\_ =>

new RequestChannel(20, ControlPlaneMetricPrefix, time))

private var nextProcessorId \= 0

// 控制连接数配额类。我们能够设置单个 IP 创建 Broker 连接的最大数量，单个 Broker 能够运行的最大连接数。

private var connectionQuotas: ConnectionQuotas = \_

private var startedProcessingRequests \= false

private var stoppedProcessingRequests \= false

从上面源码可以看出 SocketServer 实现了 BrokerReconfigurable，这就说明 SocketServer 是支持「**动态配置参数**」，即不用重启 Broker 即可完成参数变更，代码如下：

object SocketServer {

val MetricsGroup = "socket-server-metrics"

val DataPlaneThreadPrefix = "data-plane"

val ControlPlaneThreadPrefix = "control-plane"

val DataPlaneMetricPrefix = ""

val ControlPlaneMetricPrefix = "ControlPlane"

// 支持动态配置的字段

val ReconfigurableConfigs = Set(

KafkaConfig.MaxConnectionsPerIpProp,

KafkaConfig.MaxConnectionsPerIpOverridesProp,

KafkaConfig.MaxConnectionsProp,

KafkaConfig.MaxConnectionCreationRateProp)

val ListenerReconfigurableConfigs = Set(KafkaConfig.MaxConnectionsProp, KafkaConfig.MaxConnectionCreationRateProp)

}

切回到重点属性字段讲解：

1.  **maxQueuedRequests**：这是 SocketServer的请求队列长度，对于服务端来说，针对新进来的请求，Processor 线程会放到请求队列里等待真正的 I/O 工作线程处理，队列的长度就是由 Broker 端参数 [queued.max.requests](http://queued.max.requests/) 值而定，默认是500。
2.  **dataPlaneProcessors**：即数据面的 Processor 线程池，默认 3 个线程。它用来处理单个 TCP 连接上所有请求的线程，并负责将接收到的 Request 添加到 RequestChannel 的 Request 队列上，同时将 Response 返回给 Request。
3.  **dataPlaneAcceptors**：即数据面 Acceptor 线程池，key是 EndPoint，value 是 Acceptor 线程类对象。一个broker 可能会有多个 EndPoint，一个 EndPoint 只会有一个 Acceptor 线程。它用来接收和创建外部 TCP 连接的线程，它的唯一目的就是创建连接，并讲接收到的 Request 请求传递到下游的 Processor 线程处理。
4.  **dataPlaneRequestChannel**：即数据面的RequestChannel类对象。RequestChannel类定义了请求和响应类，并定义了请求集合等功能。
5.  **controlPlanProcessorOpt**：即控制面的 Processor 线程类，因为控制面的请求量不高，所以一个 Processor 线程类基本够用了。
6.  **controlPlanAcceptorOpt**：即控制面的 Acceptor 线程类。
7.  **controlPlanRequestChannelOpt**：即控制面的 RequestChannel 类对象。

![](https://article-images.zsxq.com/FvfBizc9eyQpbTPdIeo2nzpIfMXb)

接下来我们来看看 SocketServer 组件类的启动方法 startUp()，看看它启动时做了什么。

## **3.1 startUp()**

// 核心流程: 创建各类线程，包括 Acceptor、Processor、最后启动线程

def startup(startProcessingRequests: Boolean = true): Unit = {

this.synchronized {

// 实例化控制连接数配额类

connectionQuotas = new ConnectionQuotas(config, time, metrics)

// 1、创建控制面 Acceptor、Processor。包括一个 Acceptor 线程和一个 Processor 线程。

createControlPlaneAcceptorAndProcessor(config.controlPlaneListener)

// 2、创建数据面 Acceptor、Processor。创建一个 Acceptor 线程、三个 Processor 线程，由配置项num.network.threads决定Processor的线程数，默认3个线程

createDataPlaneAcceptorsAndProcessors(config.numNetworkThreads, config.dataPlaneListeners)

if (startProcessingRequests) {

// 3、启动控制面和数据面各自的 Acceptor、 Processor线程

this.startProcessingRequests()

}

}

// ... 监控

}

这里先给你介绍下服务端请求的分类可以分为 「**控制面请求**」、「**数据面请求**」。

1.  **控制面请求：**主要用来控制服务端或协调同步类的请求，例如：主从节点数据同步、拉取，关闭 Broker 节点等等。  
2.  **数据面请求：**主要用来真正业务数据相关的请求，如生产者向 Broker 发送消息请求，消费者向 Broker 拉取消息的请求等。

好，我们看一下这个方法都做了什么工作。

1.  创建控制面 Acceptor、Processor 线程。包括一个 Acceptor 线程和一个 Processor 线程。
2.  创建数据面 Acceptor、Processor 线程。包括创建一个 Acceptor 线程、三个 Processor 线程，由配置项[num.network.threads](https://link.juejin.cn/?target=http%3A%2F%2Fnum.network.threads%2F) 决定 Processor 的线程数，默认3个线程。
3.  启动控制面和数据面各自的 Acceptor 和 Processor 线程。

至此，大家是不是会有个疑问？

> 为什么要分为控制面请求、数据面请求呢？而不是统一处理？

这里给大家解答下，主要的考虑是数据类请求量级很大会造成请求队列等待处理。如果在生产者发送消息请求或者消费者拉取数据请求时，就会使控制类请求被阻塞到数据类请求后面，就会导致控制面请求处理不及时。然而控制类请求也是很重要的，比如通知选主如果处理不及时会造成数据的不一致，所以要将请求分开，这样就不会耦合，也尽量保证不会出现问题。

startup() 启动方法内部创建了控制面和数据面的线程，这里我们只讲解下数据面的相关线程的创建，控制面的请求到下面小节再讲解。

## **3.1 createDataPlanAcceptorsAndProcessors()**

private def createDataPlaneAcceptorsAndProcessors(dataProcessorsPerListener: Int,

endpoints: Seq\[EndPoint\]): Unit = {

// 遍历监听器 endpoints 集合

endpoints.foreach { endpoint =>

// 将监听器加入到连接配额管理下

connectionQuotas.addListener(config, endpoint.listenerName)

// 为监听器创建对应的 Acceptor 线程

val dataPlaneAcceptor \= createAcceptor(endpoint, DataPlaneMetricPrefix)

// 为监听器创建多个 Processor 线程。由 num.network.threads 参数决定

addDataPlaneProcessors(dataPlaneAcceptor, endpoint, dataProcessorsPerListener)

// 将键值对放入到 dataPlaneAcceptors 统一管理

dataPlaneAcceptors.put(endpoint, dataPlaneAcceptor)

info(s"Created data-plane acceptor and processors for endpoint : ${endpoint.listenerName}")

}

}

该方法比较简单，遍历当前 Broker 设置的所有监听器即 EndPoint，一台 Broker 服务器可以配置多个 Kafka 实例，通过端口区分，其中一个 Kafka 实例就是一个 EndPoint。例如：Broker 1 上配置多个 EndPoint：node01：9091、node01:9092、node01:9003等。

从方法可以看出，会做下面**3件事情**：

1.  kafka 服务端为了保证网络质量对每个监听器的连接配额是有限制的，所以需要先将 EndPoint 加入到连接配额管理之下统一管理。
2.  然后创建一个 Acceptor 线程，Acceptor 线程是专门负责与外部建立连接的线程，一个 EndPoint 只会分配一个Acceptor 线程。
3.  然后为监听器创建多个 Processor 线程，Processor 线程是用来处理网络读写操作的，具体线程数由[num.network.threads](https://link.juejin.cn/?target=http%3A%2F%2Fnum.network.threads%2F) 决定，默认是3个线程。
4.  最后把 <endpoint, dataPlaneAcceptor> 键值对保存在集合 dataPlaneAcceptors 中统一管理。

接下来我们分别看一下，Acceptor 线程和 Processor 线程是如何创建的。

## **3.2 createAcceptor()**

private def createAcceptor(endPoint: EndPoint, metricPrefix: String) : Acceptor = {

// 配置 socket 发出和接受数据缓冲区大小，默认128kb。

val sendBufferSize \= config.socketSendBufferBytes

val recvBufferSize \= config.socketReceiveBufferBytes

// 获取 broker 节点id。

val brokerId \= config.brokerId

// 创建 Acceptor 对象

new Acceptor(endPoint, sendBufferSize, recvBufferSize, brokerId, connectionQuotas, metricPrefix)

}

该方法也比较简单，主要做了以下**3件事情**：

1.  首先配置 Socket 发出和接受数据缓冲区大小，默认128kb。对于**生产环境来说，128kb 肯定使不够的，因此你可以根据自己的实际请求情况来进行修改**。对应的参数分别为： [socket.send.buffer.bytes](http://socket.send.buffer.bytes/)、[socket.receive.buffer.bytes](http://socket.receive.buffer.bytes/)。
2.  获取对应 brokerId。
3.  创建 Acceptor 对象。

接下来我们来看下数据面的 Processor 线程的创建过程。

## **3.3 addDataPlanProcessors()**

private def addDataPlaneProcessors(acceptor: Acceptor, endpoint: EndPoint, newProcessorsPerListener: Int): Unit = {

// endpoint 监听器名称

val listenerName \= endpoint.listenerName

// 协议

val securityProtocol \= endpoint.securityProtocol

// Processor 监听器可变数组。

val listenerProcessors \= new ArrayBuffer\[Processor\]()

// 遍历 newProcessorsPerListener，默认为 3个

for (\_ <- 0 until newProcessorsPerListener) {

// 创建 Processor 线程对象

val processor \= newProcessor(nextProcessorId, dataPlaneRequestChannel, connectionQuotas, listenerName, securityProtocol, memoryPool)

// 把 processor 添加到数组中。

listenerProcessors += processor

// 把 processor 加入到 dataPlaneRequestChannel 内的集合中，用来监控 processor 对象

dataPlaneRequestChannel.addProcessor(processor)

//当前 processor 线程Id + 1

nextProcessorId += 1

}

// 把 processId 和 processor 通过map集合存储起来

listenerProcessors.foreach(p => dataPlaneProcessors.put(p.id, p))

//为 acceptor 添加对应的 processor

acceptor.addProcessors(listenerProcessors, DataPlaneThreadPrefix)

}

该方法也比较简单，主要做了以下**5件事情**：

1.  首先获取监听器的名称以及支持的安全协议。这里的安全协议主要指：「**PLAINTEXT**」、「**SSL**」、「**SASL\_PLAINTEXT**」、「**SASL\_SSL**」。
2.  创建 Processor 监听器可变数组。
3.  循环创建 Processor 对象。
4.  将新创建的 processor 对象添加到数组 listenerProcessors 中。
5.  把 processor 加入到 dataPlaneRequestChannel 内的集合中，dataPlaneRequestChannel 对象的实现类是 RequestChannel，用来监控 processor 对象。
6.  将 nexProcessorId + 1，这里的 nextProcessorId 表示当前 Processor 线程的 ID。
7.  在循环结束后， 会将 Processor 对象添加到数据面 Processor 线程集合 dataPlaneProcessors 中。
8.  最后，将 processor 对象添加到 acceptor 对象里的集合里。

综上得出，processor 线程创建结束后会加到以下三个集合中：「**dataPlaneRequestChannel 对象内的 Processor 集合**」、「**dataPlaneProcessors 集合**」、「**acceptor 对象集合**」。这里通过一张图来说明下 acceptor 和 Processor 的对应关系，帮你更好的理解它们。

  
![](https://article-images.zsxq.com/Fhxy3dk5IOzEDvnOXoY9v0iLxPuN)

##   
**3.4 startAcceptorAndProcessors()**

private def startAcceptorAndProcessors(threadPrefix: String,

endpoint: EndPoint,

acceptor: Acceptor,

authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\] = Map.empty): Unit = {

debug(s"Wait for authorizer to complete start up on listener ${endpoint.listenerName}")

waitForAuthorizerFuture(acceptor, authorizerFutures)

debug(s"Start processors on listener ${endpoint.listenerName}")

// 通过后台线程启动 acceptor下的 Processors 线程。

acceptor.startProcessors(threadPrefix)

debug(s"Start acceptor thread on listener ${endpoint.listenerName}")

// 判断是否启动成功，如果失败则通过后台线程启动acceptor线程。

if (!acceptor.isStarted()) {

KafkaThread.nonDaemon(

s"${threadPrefix}-kafka-socket-acceptor-${endpoint.listenerName}-${endpoint.securityProtocol}-${endpoint.port}",

acceptor

).start()

acceptor.awaitStartup()

}

info(s"Started $threadPrefix acceptor and processor(s) for endpoint : ${endpoint.listenerName}")

}

private\[network\] def startProcessors(processorThreadPrefix: String): Unit = synchronized {

if (!processorsStarted.getAndSet(true)) {

startProcessors(processors, processorThreadPrefix)

}

}

private def startProcessors(processors: Seq\[Processor\], processorThreadPrefix: String): Unit = synchronized {

// 线程命名规范如下: processor线程前缀-kafka-network-thread-broker序号-监听器名称-安全协议-Processor序号

// 假设为序号为0的Broker设置PLAINTEXT://localhost:9092作为连接信息，那么3个Processor线程名称分别为：

processors.foreach { processor =>

KafkaThread.nonDaemon(

s"${processorThreadPrefix}-kafka-network-thread-$brokerId-${endPoint.listenerName}-${endPoint.securityProtocol}-${processor.id}",

processor

).start()

}

}

该方法也比较简单，主要做了以下**2件事情**：

1.  启动 acceptor 对象下的 Processors 线程。Processors 线程创建完成后要放入到对应的 Acceptor 线程的对应集合中，一个 EndPoint 监听器对应一个 Acceptor 线程，一个 Acceptor 线程对应默认 3 个 Processor 线程。
2.  具体启动方法就是调用 Acceptor 类的方法 startProcessors()，并以后台线程的形式启动所有的 Acceptor 下所有的 Processor 线程。
3.  通过后台线程启动 acceptor 线程。

这里需要注意是**先创建处理网络读写请求的线程 Processor 再创建建立连接的线程 Acceptor，这样做的原因是当 Acceptor 线程接收到请求后能够把读写请求交给 Processor 线程来完成**。

好了，SocketServer类基本上学习完了。

接下来我们来看看数据面以及控制面的相关源码实现。

##   
**04 数据面、控制面**

从 SocketServer 类的属性以及 startUp() 启动方法可以得出， 「**Data Plane**」、「**Controll Plane**」 两类线程来处理对应的请求，因此 Kafka 可以将请求分为「**数据类请求**」和 「**控制类请求**」两种。 其中 「**Data Plane**」用来处理「**数据类请求**」，而 「**Controll Plane**」用来处理 「**控制类请求**」。

既然知道了 Kafka 将请求分为两大类，那么究竟是通过什么方式来识别和处理的呢？

这里就引入了一个新的概念叫 「**监听器**」，说白了就是创建多套监听器来分别执行「**数据类请求**」、「**控制类请求**」的代码。

我们都知道在 Kafka Broker 端有两个配置项可以用来配置「**监听器**」的，它们分别是：「**listeners**」、「**advertised.listeners**」，那么我们来看看源码中是如何定义的？

具体的github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/cluster/EndPoint.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/cluster/EndPoint.scala)

object EndPoint {

/\*\*

\* Part of the broker definition - matching host/port pair to a protocol

\*/

case class EndPoint(host: String, port: Int, listenerName: ListenerName, securityProtocol: SecurityProtocol) {

// 用来构造完整的监听器连接 格式：监听器名称://主机名:端口

// 比如：PLAINTEXT://192.168.56.1:9092

def connectionString: String = {

val hostport \=

if (host == null)

":"+port

else

Utils.formatAddress(host, port)

listenerName.value + "://" + hostport

}

// java 版本的 EndPoint 实例

def toJava: JEndpoint = {

new JEndpoint(listenerName.value, securityProtocol, host, port)

}

}

}

从上面源码中得出，每个 EndPoint 对象都定义了 4 个属性，如下：

1.  **host**：这是 Broker 对应的主机名。
2.  **port**：这是 Broker 对应的端口。
3.  **listenerName**：这是监听器的名称。Kafka 允许你自定义比如：「**CONTROLLER**」、「**INTERNAL**」、「**EXTERNAL**」等。
4.  **securityProtocol**：这是监听器使用的安全协议。目前 Kafka 支持 4 种安全协议，分别是 「**PLAINTEXT**」、「**SSL**」、「**SASL\_PLAINTEXT**」、「**SASL\_SSL**」。

在上一小节中，我带大家剖析了 SocketServer 中的 「**Data Plane**」的线程池创建过程，有了上面这些知识点后，我们继续来看看 「**Control Plane**」的线程创建过程。

从定义源码来看，「**Control Plane**」属性字段都是以 Opt 结尾的，也就是说都是 Option 类型。即它是支持开关的，如果关闭 「**Control Plane**」设置的话，你可以让 Kafka 不用区分请求类型，但开启 「**Control Plane**」设置的话，生成规则如下：

1.  只有一个 Processor 线程。
2.  也只有一个 Acceptor 线程。
3.  这里的 RequestChannel 里面的请求队列长度是通过「**硬编码**」为 20。

通过这个规则可以得出：**控制类请求的数量要远远小于数据类请求，所以不需要为它创建线程池和更长的请求队列**。

##   
**4.1 创建 Control Plane**

private def createControlPlaneAcceptorAndProcessor(endpointOpt: Option\[EndPoint\]): Unit = {

// 当为 Control Plane 配置了监听器

endpointOpt.foreach { endpoint =>

// 也将监听器加入到连接配额管理下

connectionQuotas.addListener(config, endpoint.listenerName)

// 为监听器创建对应的 Acceptor 线程

val controlPlaneAcceptor \= createAcceptor(endpoint, ControlPlaneMetricPrefix)

// 为监听器创建对应的 Processor 线程

val controlPlaneProcessor \= newProcessor(nextProcessorId, controlPlaneRequestChannelOpt.get, connectionQuotas, endpoint.listenerName, endpoint.securityProtocol, memoryPool)

controlPlaneAcceptorOpt = Some(controlPlaneAcceptor)

controlPlaneProcessorOpt = Some(controlPlaneProcessor)

val listenerProcessors \= new ArrayBuffer\[Processor\]()

listenerProcessors += controlPlaneProcessor

// 将 Processor 线程添加到控制类请求的 RequestChannel 中

controlPlaneRequestChannelOpt.foreach(\_.addProcessor(controlPlaneProcessor))

nextProcessorId += 1

// 把 Processor 线程添加到 Acceptor 线程下的 Processor 线程池中

controlPlaneAcceptor.addProcessors(listenerProcessors, ControlPlaneThreadPrefix)

info(s"Created control-plane acceptor and processor for endpoint : ${endpoint.listenerName}")

}

}

总体创建流程跟 Data Plane 流程一致，只是方法开头需要判断是否配置了 Control Plane 的监听器。

在第三小节 StartUp() 方法的最后会调起 this.startProcessingRequests()，我们来看看。

## **4.2 startProcessingRequests**

def startProcessingRequests(authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\] = Map.empty): Unit = {

info("Starting socket server acceptors and processors")

this.synchronized {

if (!startedProcessingRequests) {

// 启动用来处理控制类请求的 Processor 和 Acceptor 线程

startControlPlaneProcessorAndAcceptor(authorizerFutures)

// 启动用来处理数据类请求的 Processor 和 Acceptor 线程

startDataPlaneProcessorsAndAcceptors(authorizerFutures)

// 打开请求处理中标识

startedProcessingRequests = true

} else {

info("Socket server acceptors and processors already started")

}

}

info("Started socket server acceptors and processors")

}

在第三小节我们已经讲过数据类请求启动的方法 startDataPlaneProcessorsAndAcceptors， 这里我们来看看控制类请求的启动方法，流程基本都一致。

## **4.3 startControlPlaneProcessorAndAcceptor**

/\*\*

\* Start the processor of control-plane acceptor and the acceptor of this server.

\*/

private def startControlPlaneProcessorAndAcceptor(authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\]): Unit = {

controlPlaneAcceptorOpt.foreach { controlPlaneAcceptor =>

// 获取监听器

val endpoint \= config.controlPlaneListener.get

// 启动线程

startAcceptorAndProcessors(ControlPlaneThreadPrefix, endpoint, controlPlaneAcceptor, authorizerFutures)

}

}

该方法主要就是调用 startAcceptorAndProcessors 来启动 「**Acceptor 线程**」、 「**Processor 线程**」，这个上面已经讲过，你可以点击查看。

讲到这里，你是否知道通过哪里可以设置这个 「**Control Plane**」的监听器呢？

在 Broker 端参数中，有个参数为 「**control.plane.listener.name**」可以用来设置 Control Plane 监听器。该值默认为空，表示不启用请求优先级区分。如果设置了，Kafka 就会去 listeners 中寻找对应的监听器。

这里给大家举个例子。

listener.security.protocol.map = CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT

listeners \= CONTROLLER://192.168.56.1:9092,INTERNAL://192.168.56.1:9092

control.plane.listener.name = CONTROLLER

在这个例子中，「**CONTROLLER**」的那套监听器将被用于 「**Control Plane**」，而「**INTERNAL**」的那套监听器将被用于 「**Data Plane**」。那么 Kafka 是如何知道 「**CONTROLLER**」就是给 「**Control Plane**」使用的呢？我们来看看具体的实现。

具体的实现代码在 KafkaConfig.scala 中，这里给出 github 具体的地址：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaConfig.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaConfig.scala)

## **4.4 getControlPlaneListenerNameAndSecurityProtocol**

private def getControlPlaneListenerNameAndSecurityProtocol: Option\[(ListenerName, SecurityProtocol)\] = {

// 1、查看 Broker 端参数 control.plane.listener.name 值，看是否启用了 control plane 监听器

Option(getString(KafkaConfig.ControlPlaneListenerNameProp)) match {

// 如果启用了

case Some(name) =>

// 获取监听器名称

val listenerName \= ListenerName.normalised(name)

// 必须同时设置 Broker 端另外一个参数 listener.security.protocol.map 值，且可以从该参数中获取出该监听器对应的安全认证协议，才会返回对应的安全协议

val securityProtocol \= listenerSecurityProtocolMap.getOrElse(listenerName,

throw new ConfigException(s"Listener with ${listenerName.value} defined in " +

s"${KafkaConfig.ControlPlaneListenerNameProp} not found in ${KafkaConfig.ListenerSecurityProtocolMapProp}."))

// 返回<监听器名称，安全认证协议>

Some(listenerName, securityProtocol)

// 否则如果没有这是该参数值，直接返回 None，表示没有启用 control plane 监听器

case None \=\> None

}

}

该方法主要做了以下**3件事情**：

1.  首先会通过 getString 方法获取 ControlPlaneListenerNameProp的值，即Broker 端参数 [control.plane.listener.name](http://control.plane.listener.name/) 值，在上面的例子中就是 「**CONTROLLER**」字符串。
2.  如果设置了「**Control Plane**」监听器，然后在获取 Broker 端参数 [listener.security.protocol.map](http://listener.security.protocol.map/) 值，并找出 「**CONTROLLER**」对应的安全认证协议，在上面的例子中 「**CONTROLLER**」对应的安全协议是 「**PLAINTEXT**」，那么 ControlPlaneListener 方法会拿到 <**CONTROLLER,PLAINTEXT**\>。
3.  最后，ControlPlaneListener 方法拿到该组值并记录下后，取出监听器名称 「**CONTROLLER**」去寻找 Broker 端参数 listeners 中对应的监听器，在上面这个例子中监听器就是 CONTROLLER://192.168.56.1:9092，然后把它传入到 SocketServer 类的 createControlPlaneAcceptorAndProcessor()，即完成 Control Plane 监听器的查找过程。

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、带你先整体的梳理了 Kafka 网络通信架构设计。

2、带你深度剖析了 SocketServer 组件类的实现，它是服务端网络通信的基础和入口，同时还负责创建和启动服务端网络层的两个线程类Acceptor和Processor。Acceptor负责建立网络连接，Processor负责网络读写。

3、最后带你深度剖析了SocketServer 数据面、控制面的实现原理。

下篇我们来深度剖析「**SocketServer 组件类之 Acceptor 线程架构设计**」，大家期待，我们下期见。