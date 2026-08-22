大家好，我是 **华仔**, 又跟大家见面了。

前面「**八篇**」文章通过「**场景驱动方式**」带你深度剖析了 Kafka「**日志系统**」源码架构设计的方方面面，从今天开始，我们来深度剖析 Kafka「**Controller**」的底层源码实现，这是 Controller 系列第一篇，我们先回过头来继续来深度聊聊「**Kafka 服务端启动的流程**」，看看 Kafka 服务端是如何启动的。

![](https://article-images.zsxq.com/Fl_DuQFZ4Ls-FpFSHmm_h-U5lzRj)

## **01 总体概述**

在深入剖析Kafka「**Controller**」之前，我想你可能或多或少会有这样的疑问:

**Kafka 服务端都有哪些组件，这些组件又是通过哪个类来启动的呢？**

这里我们通过启动 Kafka 来了解，大家都知道，启动 Kafka 可以执行以下命令来启动：

\# 1、启动 kafka 服务命令：

bin/kafka-server-start.sh config/server.properties &

那么今天就来看看通过这个脚本 KafkaServer 初始化了哪些组件。

本文会基于 Kafka 「**2.8 版本 ZK 模式**」以及 「**3.0 版本 Raft 模式**」来剖析两种启动模式，先来看「**2.8 版本 ZK 模式**」。

## **02 kafka-server-start.sh**

我们来看下里面的 shell 内容，如下：

#!/bin/bash

\# Licensed to the Apache Software Foundation (ASF) under one or more

\# contributor license agreements. See the NOTICE file distributed with

\# this work for additional information regarding copyright ownership.

\# The ASF licenses this file to You under the Apache License, Version 2.0

\# (the "License"); you may not use this file except in compliance with

\# the License. You may obtain a copy of the License at

#

\# http://www.apache.org/licenses/LICENSE-2.0

#

\# Unless required by applicable law or agreed to in writing, software

\# distributed under the License is distributed on an "AS IS" BASIS,

\# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

\# See the License for the specific language governing permissions and

\# limitations under the License.

\# 1、注释说明该脚本的版权信息和使用许可。

if \[ $# -lt 1 \];

then

echo "USAGE: $0 \[-daemon\] server.properties \[--override property=value\]\*"

exit 1

fi

\# 2、检查命令行参数的个数，若小于 1 则输出脚本的使用方法并退出。

base\_dir=$(dirname $0)

\# 3、获取当前脚本所在目录的路径，并将其赋值给 base\_dir 变量。

if \[ "x$KAFKA\_LOG4J\_OPTS" = "x" \]; then

export KAFKA\_LOG4J\_OPTS="-Dlog4j.configuration=file:$base\_dir/../config/log4j.properties"

fi

\# 4、检查 KAFKA\_LOG4J\_OPTS 环境变量是否设置，若未设置则设置该变量的值。

if \[ "x$KAFKA\_HEAP\_OPTS" = "x" \]; then

export KAFKA\_HEAP\_OPTS="-Xmx1G -Xms1G"

export JMX\_PORT="9999"

export JMX\_RMI\_PORT="10000"

fi

\# 5、检查 KAFKA\_HEAP\_OPTS 环境变量是否设置，若未设置则设置该变量的值，并设置 JMX\_PORT 和 JMX\_RMI\_PORT 环境变量的值，将 EXTRA\_ARGS 变量的值设置为字符串 -name kafkaServer -loggc。

EXTRA\_ARGS=${EXTRA\_ARGS-'-name kafkaServer -loggc'}

\# 6、检查命令行参数中 COMMAND 变量的值是否为 -daemon，若是则将 EXTRA\_ARGS 变量的值添加 -daemon 选项。同时将命令行参数向左移一位，即从 $2 开始计算参数。

COMMAND=$1

case $COMMAND in

\-daemon)

EXTRA\_ARGS="-daemon "$EXTRA\_ARGS

shift

;;

\*)

;;

esac

\# 7、调用 $base\_dir/kafka-run-class.sh 脚本并传递相应的参数。其中 "@ 代表传递的为命令行参数。具体执行的封装在 Kafka 客户端库中的 kafka.Kafka 类。整个脚本的作用是启动 Kafka 服务。

exec $base\_dir/kafka-run-class.sh $EXTRA\_ARGS kafka.Kafka "$@"

这里我们重点来看 「**第 7 步**」，它底层执行的是**封装在 Kafka 客户端库中的 kafka.Kafka 类**。接下来我们来看下该类都做了什么。

## **03 kafka.Kafka 类 服务端启动入口**

「**Kafka.scala**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/log/LogSegment.scala)Kafka[.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/Kafka.scala)

![](https://article-images.zsxq.com/Fu3qhQ_nNo8mk5au-rIT3Xz0kETj)

从整体上来看，该类就 3 个方法，相对比较简单，我能来看下里面的重点。

1.  调用 [Kafka.scala#getPropsFromArgs()](http://kafka.scala/#getPropsFromArgs\(\)) 方法将启动参数中指定的配置文件加载到内存中
2.  调用 [Kafka.scala#buildServer()](http://kafka.scala/#buildServer\(\)) 方法创建 kafka 的服务端实例对象
3.  调用创建的服务端实例对象的接口方法 [Server.scala#startup()](http://server.scala/#startup\(\)) 方法启动服务端。

这里我们通过「**2.8.x**」版本来讲解，「**2.7.x**」还未增加 KafkaRaftServer 类。

## **3.1 getPropsFromArgs**

def getPropsFromArgs(args: Array\[String\]): Properties = {

// 创建一个命令行参数解析器

val optionParser \= new OptionParser(false)

// 定义 --override 选项，用于覆盖 server.properties 文件中的属性

val overrideOpt \= optionParser.accepts("override", "Optional property that should override values set in server.properties file")

.withRequiredArg()

.ofType(classOf\[String\])

// 定义 --version 选项，用于打印版本信息并退出

optionParser.accepts("version", "Print version information and exit.")

// 若没有提供参数或者参数包含 --help 选项，则打印用法并退出

if (args.length == 0 || args.contains("--help")) {

CommandLineUtils.printUsageAndDie(optionParser, "USAGE: java \[options\] %s server.properties \[--override property=value\]\*".format(classOf\[KafkaServer\].getSimpleName()))

}

// 若参数中包含 --version 选项，则打印版本信息并退出

if (args.contains("--version")) {

CommandLineUtils.printVersionAndDie()

}

// 加载 server.properties 文件中的属性到 Properties 对象中

val props \= Utils.loadProps(args(0))

// 若提供了其他参数，则解析这些参数

if (args.length > 1) {

// 解析参数中的选项和参数值

val options \= optionParser.parse(args.slice(1, args.length): \_\*)

// 检查是否有非选项参数

if (options.nonOptionArguments().size() > 0) {

CommandLineUtils.printUsageAndDie(optionParser, "Found non argument parameters: " + options.nonOptionArguments().toArray.mkString(","))

}

// 将解析得到的选项和参数值添加到 props 对象中

props ++= CommandLineUtils.parseKeyValueArgs(options.valuesOf(overrideOpt).asScala)

}

// 返回解析得到的属性集合

props

}

该函数的作用是**从命令行参数中解析出属性集合**。它内部使用了 OptionParser 类库来解析命令行选项，并调用 [Utils#loadProps()](http://utils/#loadProps\(\)%C2%A0) [](http://utils/#loadProps\(\)%C2%A0)加载指定的配置文件，即从 [server.properties](http://server.properties/) 文件中加载属性。

如果提供了 override 选项，则它将覆盖 [server.properties](http://server.properties/) 文件中的相应属性。函数返回一个 Properties 对象，其中包含了解析得到的属性。

如果没有提供正确的命令行参数或者提供了 --help 或 --version 选项，函数会打印帮助信息或版本信息并退出。

##   
**3.2 buildServer**

private def buildServer(props: Properties): Server = {

// 通过 KafkaConfig.scala#fromProps() 方法将加载到内存中的配置转化构建为 KafkaConfig 对象

val config \= KafkaConfig.fromProps(props, false)

// 调用 KafkaConfig.scala#requiresZookeeper() 方法确定 Kafa 服务端的启动模式。此处主要是通过 process.roles 配置的存在与否来判断，如果这个配置存在则以移除 zk 依赖的 KRaft模式启动，否则以依赖 zk 的旧模式启动

if (config.requiresZookeeper) {

// 直接启动定时任务、网络层、请求处理层

new KafkaServer(

config,

Time.SYSTEM,

threadNamePrefix = None,

enableForwarding = false

)

} else {

// 调用 BrokerServer 等来启动网络层和请求处理层

new KafkaRaftServer(

config,

Time.SYSTEM,

threadNamePrefix = None

)

}

}

  
在 kafka 2.8.x 版本中 新增了 **raft** 协议之后将 BrokerServer、ControllServer 使用了单独的文件来启动最终调用网络层和请求处理层，如果还是使用 zk 的方式启动则是 KafkaServer 启动网络层和请求处理层。

##   
**3.3 main**

\# 2.7.x 版本源码

def main(args: Array\[String\]): Unit = {

try {

// 1、解析命令行参数，获得属性集合

val serverProps \= getPropsFromArgs(args)

// 2、从属性集合创建 KafkaServerStartable 对象

val kafkaServerStartable \= KafkaServerStartable.fromProps(serverProps)

try {

// 如果不是 Windows 操作系统，并且不是 IBM JDK，则注册 LoggingSignalHandler

if (!OperatingSystem.IS\_WINDOWS && !Java.isIbmJdk)

new LoggingSignalHandler().register()

} catch {

// 如果注册 LoggingSignalHandler 失败，则在日志中打印警告信息

case e: ReflectiveOperationException =>

warn("Failed to register optional signal handler that logs a message when the process is terminated " +

s"by a signal. Reason for registration failure is: $e", e)

}

// 3、添加 shutdown hook，用于在程序结束时执行 KafkaServerStartable 的 shutdown 方法

Exit.addShutdownHook("kafka-shutdown-hook", kafkaServerStartable.shutdown())

// 4、启动 KafkaServerStartable 实例

kafkaServerStartable.startup()

// 5、等待 KafkaServerStartable 实例终止

kafkaServerStartable.awaitShutdown()

}

catch {

// 如果有异常发生，则记录日志并退出程序

case e: Throwable =>

fatal("Exiting Kafka due to fatal exception", e)

Exit.exit(1)

}

// 6、正常终止程序

Exit.exit(0)

}

该函数是 **Kafka 服务进程的入口，它是整个 Kafka 运行过程的驱动程序**。该函数首先通过调用 getPropsFromArgs 函数解析命令行参数并获得属性集合，然后使用这些属性创建 KafkaServerStartable 实例。接着，它注册一个 shutdown hook，用于在程序终止时执行 KafkaServerStartable 的 shutdown 方法。然后它启动 KafkaServerStartable 实例，并等待该实例终止。如果发生异常，则记录日志并退出程序。函数最后调用 [Exit.exit](http://exit.exit/) 方法退出程序，返回 0 表示正常终止。

\# 2.8.x 版本

def main(args: Array\[String\]): Unit = {

// 获取Kafka服务的配置信息

val serverProps \= getPropsFromArgs(args)

// 根据配置信息构建Kafka服务

val server \= buildServer(serverProps)

try {

// 注册用于记录日志的信号处理器（若实现失败则退出）

if (!OperatingSystem.IS\_WINDOWS && !Java.isIbmJdk)

new LoggingSignalHandler().register()

} catch {

case e: ReflectiveOperationException =>

warn("Failed to register optional signal handler that logs a message when the process is terminated " +

s"by a signal. Reason for registration failure is: $e", e)

}

// 挂载关闭处理器，用于捕获终止信号和常规终止请求

Exit.addShutdownHook("kafka-shutdown-hook", {

try server.shutdown() // 关闭Kafka服务

catch {

case \_: Throwable =>

fatal("Halting Kafka.") // 日志记录致命错误信息

// 调用Exit.halt()强制退出，避免重复调用Exit.exit()引发死锁

Exit.halt(1)

}

})

try server.startup() // 启动Kafka服务

catch {

case \_: Throwable =>

// 调用Exit.exit()设置退出状态码，KafkaServer.startup()会在抛出异常时调用shutdown()

fatal("Exiting Kafka.")

Exit.exit(1)

}

server.awaitShutdown() // 等待Kafka服务关闭

Exit.exit(0) // 调用Exit.exit()设置退出状态码

}

这里最重要的是 「**第 4 步**」，调用 [kafkaServerStartable.startup()](http://kafkaserverstartable.startup\(\)/) 或者 [server.startup()](http://%20server.startup\(\)/) 来启动 kafka。

这里我们还是以「**ZK 模式**」的方式来启动，下面再进行对 「**Raft 模式**」启动进行补充。

## **04 Zookeeper 模式服务端启动**

## **4.1 KafkaServerStartable 类**

「**KafkaServerStartable.scala**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaServerStartable.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaServerStartable.scala)

![](https://article-images.zsxq.com/FigYc4nP5aObblNOa1qfQ38M2Plg)

在 Scala 语言里，在一个源代码文件中同时定义相同名字的 class 和 object 的用法被称为伴生（Companion）。Class 对象被称为伴生类，它和 Java 中的类是一样的；而 Object 对象是一个单例对象，用于保存一些静态变量或静态方法。

这里我们主要来看下 Class 类代码。

class KafkaServerStartable(val staticServerConfig: KafkaConfig, reporters: Seq\[KafkaMetricsReporter\], threadNamePrefix: Option\[String\] = None) extends Logging {

// 创建 KafkaServer 实例

// 构造函数有两个参数 —— staticServerConfig 表示静态服务器配置，reporters 表示 Kafka 指标报告器。如果 threadNamePrefix 参数未用于构造函数，则默认值为 None。threadNamePrefix 参数表示线程名称前缀，用于调试和维护目的。

private val server \= new KafkaServer(staticServerConfig, kafkaMetricsReporters = reporters, threadNamePrefix = threadNamePrefix)

def this(serverConfig: KafkaConfig) = this(serverConfig, Seq.empty)

// 启动 KafkaServer

// startup 方法尝试启动 Kafka 服务器。如果启动 Kafka 服务器时发生异常，则记录一条 fatal 错误日志并退出程序。对于成功启动的 Kafka 服务器，它将开始监听客户端连接，并在收到消息时执行所需的操作。

def startup(): Unit = {

try server.startup()

catch {

// 如果出现异常，则记录日志并退出程序

case \_: Throwable =>

// KafkaServer.startup() calls shutdown() in case of exceptions, so we invoke \`exit\` to set the status code

fatal("Exiting Kafka.")

Exit.exit(1)

}

}

// 关闭 KafkaServer

// shutdown 方法尝试停止 Kafka 服务器。如果在停止服务器时出现异常，则记录一条 fatal 错误日志并强制退出程序。调用 shutdown 方法后，服务器将不再接受新的请求，并开始等待当前进行中的请求完成。当所有处理中的请求都完成后，服务器将彻底停止。

def shutdown(): Unit = {

try server.shutdown()

catch {

// 如果出现异常，则记录日志并强制退出程序

case \_: Throwable =>

fatal("Halting Kafka.")

// Calling exit() can lead to deadlock as exit() can be called multiple times. Force exit.

Exit.halt(1)

}

}

// setServerState 方法允许从 KafkaServerStartable 对象中设置 broker 状态。如果自定义 KafkaServerStartable 对象想要引入新的状态，则此方法很有用。

def setServerState(newState: Byte): Unit = {

server.brokerState.newState(newState)

}

// 等待 KafkaServer 退出

// awaitShutdown 方法等待 Kafka 服务器完全退出。在 Kafka 服务器执行 shutdown 方法后，它将不再接受新的请求。但是，服务器可能仍在处理一些已经接收的请求。awaitShutdown 方法将阻塞当前线程，直到服务器彻底停止。

def awaitShutdown(): Unit = server.awaitShutdown()

}

KafkaServerStartable 类是**一个可启动和停止的 Kafka 服务器**。类中的 server 成员变量是 KafkaServer 类的实例，它将在 KafkaServerStartable 类对象启动时创建。该类提供了启动和停止 Kafka 服务器的方法，以及设置 broker 状态和等待 Kafka 服务器退出的方法。

跟本文有关系的是 「**启动**」方法，它调用了 **KafkaServer#startup** 方法进行启动。

## **05 KafkaServer 类**

Kafka 集群由多个 Broker 节点构成，每个节点上都运行着一个 Kafka 实例，这些实例之间基于 ZK 来发现彼此，并由集群控制器 KafkaController 统筹协调运行，彼此之间基于 socket 连接进行通信。

「**KafkaServer.scala**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaServer.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaServer.scala)

KafkaServer 为 Kafka 的启动类，其中包含了 Kafka 的所有组件，如 KafkaController、groupCoordinator、replicaManager 等。

class KafkaServer(val config: KafkaConfig, //配置信息

time: Time = Time.SYSTEM, threadNamePrefix: Option\[String\] = None,

kafkaMetricsReporters: Seq\[KafkaMetricsReporter\] = List() //监控上报

) extends Logging with KafkaMetricsGroup {

//标识节点已经启动完成

private val startupComplete \= new AtomicBoolean(false)

//标识节点正在执行关闭操作

private val isShuttingDown \= new AtomicBoolean(false)

//标识节点正在执行启动操作

private val isStartingUp \= new AtomicBoolean(false)

//阻塞主线程等待 KafkaServer 的关闭

private var shutdownLatch \= new CountDownLatch(1)

//日志上下文

private var logContext: LogContext = null

var metrics: Metrics = null

//记录节点的当前状态

val brokerState: BrokerState = new BrokerState

//API接口类，用于处理数据类请求

var dataPlaneRequestProcessor: KafkaApis = null

//API接口，用于处理控制类请求

var controlPlaneRequestProcessor: KafkaApis = null

//权限管理

var authorizer: Option\[Authorizer\] = None

//启动socket，监听9092端口，等待接收客户端请求

var socketServer: SocketServer = null

//数据类请求处理线程池

var dataPlaneRequestHandlerPool: KafkaRequestHandlerPool = null

//命令类处理线程池

var controlPlaneRequestHandlerPool: KafkaRequestHandlerPool = null

//日志管理器

var logDirFailureChannel: LogDirFailureChannel = null

var logManager: LogManager = null

//副本管理器

var replicaManager: ReplicaManager = null

//topic增删管理器

var adminManager: AdminManager = null

//token管理器

var tokenManager: DelegationTokenManager = null

//动态配置管理器

var dynamicConfigHandlers: Map\[String, ConfigHandler\] = null

var dynamicConfigManager: DynamicConfigManager = null

var credentialProvider: CredentialProvider = null

var tokenCache: DelegationTokenCache = null

//分组协调器

var groupCoordinator: GroupCoordinator = null

//事务协调器

var transactionCoordinator: TransactionCoordinator = null

//集群控制器

var kafkaController: KafkaController = null

//定时任务调度器

var kafkaScheduler: KafkaScheduler = null

//集群分区状态信息缓存

var metadataCache: MetadataCache = null

//配额管理器

var quotaManagers: QuotaFactory.QuotaManagers = null

//zk客户端配置

val zkClientConfig: ZKClientConfig = KafkaServer.zkClientConfigFromKafkaConfig(config).getOrElse(new ZKClientConfig())

private var \_zkClient: KafkaZkClient = null

val correlationId: AtomicInteger = new AtomicInteger(0)

val brokerMetaPropsFile \= "meta.properties"

val brokerMetadataCheckpoints \= config.logDirs.map(logDir => (logDir, new BrokerMetadataCheckpoint(new File(logDir + File.separator + brokerMetaPropsFile)))).toMap

private var \_clusterId: String = null

private var \_brokerTopicStats: BrokerTopicStats = null

def clusterId: String = \_clusterId

// Visible for testing

private\[kafka\] def zkClient \= \_zkClient

private\[kafka\] def brokerTopicStats \= \_brokerTopicStats

....

}

## **5.1 startup**

该类方法很多，我们这里只看 startup 启动方法，来看看其内部都启动了哪些组件，来解决本文开头提出的问题。

/\*\*

\* Start up API for bringing up a single instance of the Kafka server.

\* Instantiates the LogManager, the SocketServer and the request handlers - KafkaRequestHandlers

\*/

def startup(): Unit = {

try {

info("starting")

// 是否已关闭

if (isShuttingDown.get)

throw new IllegalStateException("Kafka server is still shutting down, cannot re-start!")

// 是否已启动

if (startupComplete.get)

return

// 是否可以启动

val canStartup \= isStartingUp.compareAndSet(false, true)

if (canStartup) { // 设置broker状态为Starting

brokerState.newState(Starting)

/\* setup zookeeper \*/

// 连接ZK，并创建根节点

initZkClient(time)

/\* initialize features \*/

\_featureChangeListener = new FinalizedFeatureChangeListener(featureCache, \_zkClient)

if (config.isFeatureVersioningSupported) {

\_featureChangeListener.initOrThrow(config.zkConnectionTimeoutMs)

}

/\* Get or create cluster\_id \*/

// 从ZK获取或创建集群id，规则：UUID的mostSigBits、leastSigBits组合转base64

\_clusterId = getOrGenerateClusterId(zkClient)

info(s"Cluster ID = $clusterId")

/\* load metadata \*/

// 获取brokerId及log存储路径，brokerId通过zk生成或者server.properties配置broker.id

// 规则：/brokers/seqid的version值 + maxReservedBrokerId（默认1000），保证唯一性

val (preloadedBrokerMetadataCheckpoint, initialOfflineDirs) = getBrokerMetadataAndOfflineDirs

/\* check cluster id \*/

if (preloadedBrokerMetadataCheckpoint.clusterId.isDefined && preloadedBrokerMetadataCheckpoint.clusterId.get != clusterId)

throw new InconsistentClusterIdException(

s"The Cluster ID ${clusterId} doesn't match stored clusterId ${preloadedBrokerMetadataCheckpoint.clusterId} in meta.properties. " +

s"The broker is trying to join the wrong cluster. Configured zookeeper.connect may be wrong.")

/\* generate brokerId \*/

config.brokerId = getOrGenerateBrokerId(preloadedBrokerMetadataCheckpoint)

logContext = new LogContext(s"\[KafkaServer id=${config.brokerId}\] ")

// 配置logger

this.logIdent = logContext.logPrefix

// initialize dynamic broker configs from ZooKeeper. Any updates made after this will be

// applied after DynamicConfigManager starts.

// 初始化AdminZkClient，支持动态修改配置

config.dynamicConfig.initialize(zkClient)

/\* start scheduler \*/

// 初始化定时任务调度器

kafkaScheduler = new KafkaScheduler(config.backgroundThreads)

kafkaScheduler.startup()

/\* create and configure metrics \*/

// 创建及配置监控，默认使用JMX及Yammer Metrics

kafkaYammerMetrics = KafkaYammerMetrics.INSTANCE

kafkaYammerMetrics.configure(config.originals)

val jmxReporter \= new JmxReporter()

jmxReporter.configure(config.originals)

val reporters \= new util.ArrayList\[MetricsReporter\]

reporters.add(jmxReporter)

val metricConfig \= KafkaServer.metricConfig(config)

val metricsContext \= createKafkaMetricsContext()

metrics = new Metrics(metricConfig, reporters, time, true, metricsContext)

/\* register broker metrics \*/

\_brokerTopicStats = new BrokerTopicStats

// 初始化配额管理器

quotaManagers = QuotaFactory.instantiate(config, metrics, time, threadNamePrefix.getOrElse(""))

notifyClusterListeners(kafkaMetricsReporters ++ metrics.reporters.asScala)

// 用于保证kafka-log数据目录的存在

logDirFailureChannel = new LogDirFailureChannel(config.logDirs.size)

/\* start log manager \*/

// 启动日志管理器，kafka的消息以日志形式存储

logManager = LogManager(config, initialOfflineDirs, zkClient, brokerState, kafkaScheduler, time, brokerTopicStats, logDirFailureChannel)

// 启动日志清理、刷新、校验、恢复等的定时线程

logManager.startup()

metadataCache = new MetadataCache(config.brokerId)

// Enable delegation token cache for all SCRAM mechanisms to simplify dynamic update.

// This keeps the cache up-to-date if new SCRAM mechanisms are enabled dynamically.

// SCRAM认证方式的token缓存

tokenCache = new DelegationTokenCache(ScramMechanism.mechanismNames)

credentialProvider = new CredentialProvider(ScramMechanism.mechanismNames, tokenCache)

// Create and start the socket server acceptor threads so that the bound port is known.

// Delay starting processors until the end of the initialization sequence to ensure

// that credentials have been loaded before processing authentications.

// 启动socket，监听9092端口，等待接收客户端请求

socketServer = new SocketServer(config, metrics, time, credentialProvider)

socketServer.startup(startProcessingRequests = false)

/\* start replica manager \*/

brokerToControllerChannelManager = new BrokerToControllerChannelManagerImpl(metadataCache, time, metrics, config, threadNamePrefix)

// 启动副本管理器，高可用相关

replicaManager = createReplicaManager(isShuttingDown)

replicaManager.startup()

brokerToControllerChannelManager.start()

// 将broker信息注册到ZK上

val brokerInfo \= createBrokerInfo

val brokerEpoch \= zkClient.registerBroker(brokerInfo)

// Now that the broker is successfully registered, checkpoint its metadata

// 校验 broker 信息

checkpointBrokerMetadata(BrokerMetadata(config.brokerId, Some(clusterId)))

/\* start token manager \*/

// 启动 token 管理器

tokenManager = new DelegationTokenManager(config, tokenCache, time , zkClient)

tokenManager.startup()

/\* start kafka controller \*/

// 启动Kafka控制器，只有 Leader 会与ZK建连

kafkaController = new KafkaController(config, zkClient, time, metrics, brokerInfo, brokerEpoch, tokenManager, brokerFeatures, featureCache, threadNamePrefix)

kafkaController.startup()

// admin管理器

adminManager = new AdminManager(config, metrics, metadataCache, zkClient)

/\* start group coordinator \*/

// Hardcode Time.SYSTEM for now as some Streams tests fail otherwise, it would be good to fix the underlying issue

// 启动集群群组协调器

groupCoordinator = GroupCoordinator(config, zkClient, replicaManager, Time.SYSTEM, metrics)

groupCoordinator.startup()

/\* start transaction coordinator, with a separate background thread scheduler for transaction expiration and log loading \*/

// Hardcode Time.SYSTEM for now as some Streams tests fail otherwise, it would be good to fix the underlying issue

// 启动事务协调器

transactionCoordinator = TransactionCoordinator(config, replicaManager, new KafkaScheduler(threads = 1, threadNamePrefix = "transaction-log-manager-"), zkClient, metrics, metadataCache, Time.SYSTEM)

transactionCoordinator.startup()

/\* Get the authorizer and initialize it if one is specified.\*/

// ACL

authorizer = config.authorizer

authorizer.foreach(\_.configure(config.originals))

val authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\] = authorizer match {

case Some(authZ) =>

authZ.start(brokerInfo.broker.toServerInfo(clusterId, config)).asScala.map { case (ep, cs) =>

ep -> cs.toCompletableFuture

}

case None \=\>

brokerInfo.broker.endPoints.map { ep =>

ep.toJava -> CompletableFuture.completedFuture\[Void\](null)

}.toMap

}

// 创建拉取管理器

val fetchManager \= new FetchManager(Time.SYSTEM,

new FetchSessionCache(config.maxIncrementalFetchSessionCacheSlots,

KafkaServer.MIN\_INCREMENTAL\_FETCH\_SESSION\_EVICTION\_MS))

/\* start processing requests \*/

// 初始化数据类请求的KafkaApis，负责数据类请求逻辑处理

dataPlaneRequestProcessor = new KafkaApis(socketServer.dataPlaneRequestChannel, replicaManager, adminManager, groupCoordinator, transactionCoordinator,kafkaController, zkClient, config.brokerId, config, metadataCache, metrics, authorizer, quotaManagers,fetchManager, brokerTopicStats, clusterId, time, tokenManager, brokerFeatures, featureCache)

// 初始化数据类请求处理的线程池

dataPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.brokerId, socketServer.dataPlaneRequestChannel, dataPlaneRequestProcessor, time,

config.numIoThreads, s"${SocketServer.DataPlaneMetricPrefix}RequestHandlerAvgIdlePercent", SocketServer.DataPlaneThreadPrefix)

socketServer.controlPlaneRequestChannelOpt.foreach { controlPlaneRequestChannel =>

// 初始化控制类请求的 KafkaApis

controlPlaneRequestProcessor = new KafkaApis(controlPlaneRequestChannel, replicaManager, adminManager, groupCoordinator, transactionCoordinator,

kafkaController, zkClient, config.brokerId, config, metadataCache, metrics, authorizer, quotaManagers,

fetchManager, brokerTopicStats, clusterId, time, tokenManager, brokerFeatures, featureCache)

// 初始化控制类请求的线程池

controlPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.brokerId, socketServer.controlPlaneRequestChannelOpt.get, controlPlaneRequestProcessor, time,

1, s"${SocketServer.ControlPlaneMetricPrefix}RequestHandlerAvgIdlePercent", SocketServer.ControlPlaneThreadPrefix)

}

Mx4jLoader.maybeLoad()

/\* Add all reconfigurables for config change notification before starting config handlers \*/

config.dynamicConfig.addReconfigurables(this)

/\* start dynamic config manager \*/

dynamicConfigHandlers = Map\[String, ConfigHandler\](ConfigType.Topic -> new TopicConfigHandler(logManager, config, quotaManagers, kafkaController),

ConfigType.Client -> new ClientIdConfigHandler(quotaManagers),

ConfigType.User -> new UserConfigHandler(quotaManagers, credentialProvider),

ConfigType.Broker -> new BrokerConfigHandler(config, quotaManagers))

// Create the config manager. start listening to notifications

// 启动动态配置处理器

dynamicConfigManager = new DynamicConfigManager(zkClient, dynamicConfigHandlers)

dynamicConfigManager.startup()

// 启动请求处理线程

socketServer.startProcessingRequests(authorizerFutures)

// 更新broker状态

brokerState.newState(RunningAsBroker)

shutdownLatch = new CountDownLatch(1)

startupComplete.set(true)

isStartingUp.set(false)

AppInfoParser.registerAppInfo(metricsPrefix, config.brokerId.toString, metrics, time.milliseconds())

info("started")

}

}

catch {

case e: Throwable =>

fatal("Fatal error during KafkaServer startup. Prepare to shutdown", e)

isStartingUp.set(false)

shutdown()

throw e

}

}

这里总结下该方法都启动了哪些组件：

1.  initZkClient(time) 初始化 Zk。
2.  kafkaScheduler 定时器。
3.  logManager 日志模块。
4.  MetadataCache 元数据缓存。
5.  socketServer 网络服务器。
6.  replicaManager 副本模块。
7.  kafkaController 控制器。
8.  groupCoordinator 协调器用于和ConsumerCoordinator 交互
9.  transactionCoordinator 事务相关
10.  fetchManager 副本拉取管理器。
11.  dynamicConfigManager 动态配置管理器。

## **5.2 Broker 状态**

这个是在 2.7.x 版本之前的状态，在 2.8.x 之后版本进行了重构。

sealed trait BrokerStates { def state: Byte }

case object NotRunning extends BrokerStates { val state: Byte = 0 }

case object Starting extends BrokerStates { val state: Byte = 1 }

case object RecoveringFromUncleanShutdown extends BrokerStates { val state: Byte = 2 }

case object RunningAsBroker extends BrokerStates { val state: Byte = 3 }

case object PendingControlledShutdown extends BrokerStates { val state: Byte = 6 }

case object BrokerShuttingDown extends BrokerStates { val state: Byte = 7 }

1.  **NotRunning**：初始状态，标识当前 broker 节点未运行。
2.  **Starting**：标识当前 broker 节点正在启动中。
3.  **RecoveringFromUncleanShutdown**：标识当前 broker 节点正在从上次非正常关闭中恢复。
4.  **RuningAsBroker**：标识当前 broker 节点启动成功，可以对外提供服务。
5.  **PendingControlledShutdown**：标识当前 broker 节点正在等待 controlled shutdown 操作完成。
6.  **BrokerShuttingDown**：标识当前 broker 节点正在执行 shutdown 操作。

这些就是 Zookeeper 模式下 KafkaServer 中主要模块的入口，接下来的文章会通过这些入口一一进行分析。

![](https://article-images.zsxq.com/Fi32FcjE5rGw64p9Cwj8ZIlkyiCx)

## **06 Raft 模式服务端启动**

接下来，我们来剖析下 Raft 模式启动方式的源码流程，其中 「**kafka.scala#getPropsFromArgs**」、「**kafka.scala#buildServer**」、「**kafka.scala#main**」这几部流程一致。

与 Raft 模块涉及的源码：

「**KafkaRaftServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaRaftServer.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaRaftServer.scala)

「**BrokerServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala)

「**SocketServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/network/SocketServer.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala)

「**KafkaRequestHandler**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaRequestHandler.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaRequestHandler.scala)

其中 [KafkaRaftServer](http://kafkaraftserver/) 对象的创建动作会触发执行不少关键成员对象的创建，与本篇直接相关的如下：

1.  **Broker**：BrokerServer 对象，当节点的配置 [process.roles](http://process.roles/) 中指定了 Broker 角色时才会创建，处理消息数据类请求，例如消息的生产消费等。
2.  **Controller**: ControllerServer 对象，当节点的配置 [process.roles](http://process.roles/) 中指定了 controller 角色时才会创建，处理元数据类请求，包括 topic 创建删除等。
3.  **raftManager**: KafkaRaftManager 对象，负责集群选举及元数据同步的组件。

class KafkaRaftServer(

config: KafkaConfig,

time: Time,

threadNamePrefix: Option\[String\]

) extends Server with Logging {

KafkaMetricsReporter.startReporters(VerifiableProperties(config.originals))

KafkaYammerMetrics.INSTANCE.configure(config.originals)

private val (metaProps, offlineDirs) = KafkaRaftServer.initializeLogDirs(config)

private val metrics \= Server.initializeMetrics(

config,

time,

metaProps.clusterId

)

private val controllerQuorumVotersFuture \= CompletableFuture.completedFuture(

RaftConfig.parseVoterConnections(config.quorumVoters))

// KafkaRaftManager 对象，负责集群选举及元数据同步的组件。

private val raftManager \= new KafkaRaftManager\[ApiMessageAndVersion\](

metaProps,

config,

new MetadataRecordSerde,

KafkaRaftServer.MetadataPartition,

KafkaRaftServer.MetadataTopicId,

time,

metrics,

threadNamePrefix,

controllerQuorumVotersFuture

)

// BrokerServer 对象，当节点的配置 process.roles 中指定了 Broker 角色时才会创建，处理消息数据类请求，例如消息的生产消费等。

private val broker: Option\[BrokerServer\] = if (config.processRoles.contains(BrokerRole)) {

Some(new BrokerServer(

config,

metaProps,

raftManager,

time,

metrics,

threadNamePrefix,

offlineDirs,

controllerQuorumVotersFuture,

Server.SUPPORTED\_FEATURES

))

} else {

None

}

// ControllerServer 对象，当节点的配置 process.roles 中指定了 controller 角色时才会创建，处理元数据类请求，包括 topic 创建删除等。

private val controller: Option\[ControllerServer\] = if (config.processRoles.contains(ControllerRole)) {

Some(new ControllerServer(

metaProps,

config,

raftManager,

time,

metrics,

threadNamePrefix,

controllerQuorumVotersFuture

))

} else {

None

}

....

}

经过以上步骤，Kafka 服务端的 Server 对象创建完毕，最终创建了一个 [KafkaRaftServer](http://kafkaraftserver%20/) [](http://kafkaraftserver%20/)对象，调用 [KafkaRaftServer.scala#startup](http://kafkaraftserver.scala/#startup)() 方法启动服务端：

  
![](https://article-images.zsxq.com/Fhu5PspsdWetbwHV3jnk5GvdrhhL)

1.  controller.foreach(\_.startup()) 启动节点上可能存在的 ControllerServer，调用其 [ControllerServer.scala#startup](http://controllerserver.scala/#startup)() 方法。
2.  broker.foreach(\_.startup()) 启动节点上可能存在的 BrokerServer，调用其[BrokerServer.scala#startup](http://brokerserver.scala/#startup)() 方法。

  
本篇将以 [BrokerServer](http://brokerserver/) 的启动为例进行分析，其实从「**网络通信结构**」的角度来看，[BrokerServer](http://brokerserver%20/) 和 [ControllerServer](http://controllerserver%20/) 几乎是完全一致的。

## **6.1 BrokerServer#startup()**

def startup(): Unit = {

// 将状态从 SHUTDOWN 转变为 STARTING，如果状态已经不是 SHUTDOWN，则直接返回

if (!maybeChangeStatus(SHUTDOWN, STARTING)) return

try {

info("Starting broker")

/\* start scheduler \*/

// 初始化定时任务调度器

kafkaScheduler = new KafkaScheduler(config.backgroundThreads)

kafkaScheduler.startup()

/\* register broker metrics \*/

\_brokerTopicStats = new BrokerTopicStats

// 初始化配额管理器

quotaManagers = QuotaFactory.instantiate(config, metrics, time, threadNamePrefix.getOrElse(""))

quotaCache = new ClientQuotaCache()

// 用于保证kafka-log数据目录的存在

logDirFailureChannel = new LogDirFailureChannel(config.logDirs.size)

// 初始化日志管理器，kafka的消息以日志形式存储

logManager = LogManager(config, initialOfflineDirs, configRepository, kafkaScheduler, time,

brokerTopicStats, logDirFailureChannel, keepPartitionMetadataFile = true)

// 初始化元数据缓存

metadataCache = MetadataCache.raftMetadataCache(config.nodeId)

// Enable delegation token cache for all SCRAM mechanisms to simplify dynamic update.

// This keeps the cache up-to-date if new SCRAM mechanisms are enabled dynamically.

tokenCache = new DelegationTokenCache(ScramMechanism.mechanismNames)

credentialProvider = new CredentialProvider(ScramMechanism.mechanismNames, tokenCache)

// 控制器节点

val controllerNodes \= RaftConfig.quorumVoterStringsToNodes(controllerQuorumVotersFuture.get()).asScala

val controllerNodeProvider \= RaftControllerNodeProvider(metaLogManager, config, controllerNodes)

val forwardingChannelManager \= BrokerToControllerChannelManager(

controllerNodeProvider,

time,

metrics,

config,

channelName = "forwarding",

threadNamePrefix,

retryTimeoutMs = 60000

)

forwardingManager = new ForwardingManagerImpl(forwardingChannelManager)

forwardingManager.start()

val apiVersionManager \= ApiVersionManager(

ListenerType.BROKER,

config,

Some(forwardingManager),

brokerFeatures,

featureCache

)

// Create and start the socket server acceptor threads so that the bound port is known.

// Delay starting processors until the end of the initialization sequence to ensure

// that credentials have been loaded before processing authentications.

// 启动 socket，监听 9092 端口，等待接收客户端请求

socketServer = new SocketServer(config, metrics, time, credentialProvider, apiVersionManager)

socketServer.startup(startProcessingRequests = false)

// ISR 信息同步通道管理器

val alterIsrChannelManager \= BrokerToControllerChannelManager(

controllerNodeProvider,

time,

metrics,

config,

channelName = "alterisr",

threadNamePrefix,

retryTimeoutMs = Long.MaxValue

)

// 启动 ISR 信息同步通道管理器

alterIsrManager = new DefaultAlterIsrManager(

controllerChannelManager = alterIsrChannelManager,

scheduler = kafkaScheduler,

time = time,

brokerId = config.nodeId,

brokerEpochSupplier = () => lifecycleManager.brokerEpoch()

)

alterIsrManager.start()

// 初始化副本管理器，高可用相关

this.replicaManager = new RaftReplicaManager(config, metrics, time,

kafkaScheduler, logManager, isShuttingDown, quotaManagers,

brokerTopicStats, metadataCache, logDirFailureChannel, alterIsrManager,

configRepository, threadNamePrefix)

/\* start token manager \*/

if (config.tokenAuthEnabled) {

throw new UnsupportedOperationException("Delegation tokens are not supported")

}

// 启动 token 管理器

tokenManager = new DelegationTokenManager(config, tokenCache, time , null)

tokenManager.startup() // does nothing, we just need a token manager in order to compile right now...

// Create group coordinator, but don't start it until we've started replica manager.

// Hardcode Time.SYSTEM for now as some Streams tests fail otherwise, it would be good to fix the underlying issue

// 初始化消费者组协调器

groupCoordinator = GroupCoordinator(config, replicaManager, Time.SYSTEM, metrics)

// Create transaction coordinator, but don't start it until we've started replica manager.

// Hardcode Time.SYSTEM for now as some Streams tests fail otherwise, it would be good to fix the underlying issue

// 初始化事务协调器

transactionCoordinator = TransactionCoordinator(config, replicaManager,

new KafkaScheduler(threads = 1, threadNamePrefix = "transaction-log-manager-"),

createTemporaryProducerIdManager, metrics, metadataCache, Time.SYSTEM)

val autoTopicCreationChannelManager \= BrokerToControllerChannelManager(controllerNodeProvider,

time, metrics, config, "autocreate", threadNamePrefix, 60000)

autoTopicCreationManager = new DefaultAutoTopicCreationManager(

config, Some(autoTopicCreationChannelManager), None, None,

groupCoordinator, transactionCoordinator)

autoTopicCreationManager.start()

/\* Add all reconfigurables for config change notification before starting the metadata listener \*/

config.dynamicConfig.addReconfigurables(this)

val clientQuotaMetadataManager \= new ClientQuotaMetadataManager(

quotaManagers, socketServer.connectionQuotas, quotaCache)

// 初始化 broker 端元数据监听器

brokerMetadataListener = new BrokerMetadataListener(

config.nodeId,

time,

metadataCache,

configRepository,

groupCoordinator,

replicaManager,

transactionCoordinator,

threadNamePrefix,

clientQuotaMetadataManager)

// 启动网络组件监听器

val networkListeners \= new ListenerCollection()

config.advertisedListeners.foreach { ep =>

networkListeners.add(new Listener().

setHost(ep.host).

setName(ep.listenerName.value()).

setPort(socketServer.boundPort(ep.listenerName)).

setSecurityProtocol(ep.securityProtocol.id))

}

lifecycleManager.start(() => brokerMetadataListener.highestMetadataOffset(),

BrokerToControllerChannelManager(controllerNodeProvider, time, metrics, config,

"heartbeat", threadNamePrefix, config.brokerSessionTimeoutMs.toLong),

metaProps.clusterId, networkListeners, supportedFeatures)

// Register a listener with the Raft layer to receive metadata event notifications

metaLogManager.register(brokerMetadataListener)

val endpoints \= new util.ArrayList\[Endpoint\](networkListeners.size())

var interBrokerListener: Endpoint = null

networkListeners.iterator().forEachRemaining(listener => {

val endPoint \= new Endpoint(listener.name(),

SecurityProtocol.forId(listener.securityProtocol()),

listener.host(), listener.port())

endpoints.add(endPoint)

if (listener.name().equals(config.interBrokerListenerName.value())) {

interBrokerListener = endPoint

}

})

if (interBrokerListener == null) {

throw new RuntimeException("Unable to find inter-broker listener " +

config.interBrokerListenerName.value() + ". Found listener(s): " +

endpoints.asScala.map(ep => ep.listenerName().orElse("(none)")).mkString(", "))

}

val authorizerInfo \= ServerInfo(new ClusterResource(clusterId),

config.nodeId, endpoints, interBrokerListener)

/\* Get the authorizer and initialize it if one is specified.\*/

authorizer = config.authorizer

authorizer.foreach(\_.configure(config.originals))

val authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\] = authorizer match {

case Some(authZ) =>

authZ.start(authorizerInfo).asScala.map { case (ep, cs) =>

ep -> cs.toCompletableFuture

}

case None \=\>

authorizerInfo.endpoints.asScala.map { ep =>

ep -> CompletableFuture.completedFuture\[Void\](null)

}.toMap

}

// 初始化副本拉取管理器

val fetchManager \= new FetchManager(Time.SYSTEM,

new FetchSessionCache(config.maxIncrementalFetchSessionCacheSlots,

KafkaServer.MIN\_INCREMENTAL\_FETCH\_SESSION\_EVICTION\_MS))

// Start processing requests once we've caught up on the metadata log, recovered logs if necessary,

// and started all services that we previously delayed starting.

val raftSupport \= RaftSupport(forwardingManager, metadataCache)

// 数据面请求分发处理器

dataPlaneRequestProcessor = new KafkaApis(socketServer.dataPlaneRequestChannel, raftSupport,

replicaManager, groupCoordinator, transactionCoordinator, autoTopicCreationManager,

config.nodeId, config, configRepository, metadataCache, metrics, authorizer, quotaManagers,

fetchManager, brokerTopicStats, clusterId, time, tokenManager, apiVersionManager)

// 数据面请求I/O 线程池

dataPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.nodeId, socketServer.dataPlaneRequestChannel, dataPlaneRequestProcessor, time,

config.numIoThreads, s"${SocketServer.DataPlaneMetricPrefix}RequestHandlerAvgIdlePercent", SocketServer.DataPlaneThreadPrefix)

socketServer.controlPlaneRequestChannelOpt.foreach { controlPlaneRequestChannel =>

// 控制面请求分发处理器

controlPlaneRequestProcessor = new KafkaApis(controlPlaneRequestChannel, raftSupport,

replicaManager, groupCoordinator, transactionCoordinator, autoTopicCreationManager,

config.nodeId, config, configRepository, metadataCache, metrics, authorizer, quotaManagers,

fetchManager, brokerTopicStats, clusterId, time, tokenManager, apiVersionManager)

// 控制面请求I/O 线程池

controlPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.nodeId, socketServer.controlPlaneRequestChannelOpt.get, controlPlaneRequestProcessor, time,

1, s"${SocketServer.ControlPlaneMetricPrefix}RequestHandlerAvgIdlePercent", SocketServer.ControlPlaneThreadPrefix)

}

// Block until we've caught up on the metadata log

lifecycleManager.initialCatchUpFuture.get()

// Start log manager, which will perform (potentially lengthy) recovery-from-unclean-shutdown if required.

// 启动日志管理器

logManager.startup(metadataCache.getAllTopics())

// Start other services that we've delayed starting, in the appropriate order.

// 启动副本管理器和高水位检测点线程

replicaManager.startup()

replicaManager.startHighWatermarkCheckPointThread()

// 启动消费者组协调器

groupCoordinator.startup(() => metadataCache.numPartitions(Topic.GROUP\_METADATA\_TOPIC\_NAME).

getOrElse(config.offsetsTopicPartitions))

// 启动事务协调器

transactionCoordinator.startup(() => metadataCache.numPartitions(Topic.TRANSACTION\_STATE\_TOPIC\_NAME).

getOrElse(config.transactionTopicPartitions))

// Apply deferred partition metadata changes after starting replica manager and coordinators

// so that those services are ready and able to process the changes.

replicaManager.endMetadataChangeDeferral(

RequestHandlerHelper.onLeadershipChange(groupCoordinator, transactionCoordinator, \_, \_))

socketServer.startProcessingRequests(authorizerFutures)

// We're now ready to unfence the broker.

lifecycleManager.setReadyToUnfence()

// 最后将状态从 STARTING 转变为 STARTED

maybeChangeStatus(STARTING, STARTED)

} catch {

case e: Throwable =>

maybeChangeStatus(STARTING, STARTED)

fatal("Fatal error during broker startup. Prepare to shutdown", e)

shutdown()

throw e

}

}

该方法比较长，其中和网络通信相关的重点其实只有两个，分别是 [SocketServer](http://socketserver%20/) 底层网络服务器的创建及配置启动 和 [KafkaRequestHandlerPool](http://kafkarequesthandlerpool/) 上层请求处理器池的创建启动，涉及的关键对象如下：

1.  kafkaScheduler：KafkaScheduler 对象，定时任务的线程池。
2.  metadataCache: KRaftMetadataCache 对象，集群元数据管理组件。
3.  clientToControllerChannelManager ：BrokerToControllerChannelManager 对象，broker 到 controller 的连接管理器。
4.  forwardingManager：ForwardingManagerImpl 对象，持有 clientToControllerChannelManager 对象，负责转发应该由 controller 处理的请求。
5.  socketServer: SocketServer 对象，面向底层网络的服务器对象。
6.  \_replicaManager: ReplicaManager 对象，副本管理器，负责消息的存储读取。
7.  metadataListener: BrokerMetadataListener 元数据监听器对象，将注册到 KafkaRaftManager 中监听集群元数据变化，例如新建 Topic 时 broker 角色必须要创建自己负责的分区副本文件。
8.  groupCoordinator: GroupCoordinator 对象，普通消费者组的协调器，负责辅助完成消费者组内各个消费者消费分区的协调分配。
9.  dataPlaneRequestProcessor: KafkaApis 对象，上层的请求处理器，持有底层网络服务器的请求队列。socketServer.dataPlaneRequestChannel，负责从队列中取出请求进行处理。
10.  dataPlaneRequestHandlerPool : KafkaRequestHandlerPool 对象，上层的 I/O 请求处理器线程池。

## **6.2 SocketServer**

![](https://article-images.zsxq.com/FuB04aT0njB32HkfkNOnpfBAlhKM)

[SocketServer](http://socketserver%20/) [](http://socketserver%20/)对象创建时会触发内部的请求队列 [RequestChannel](http://requestchannel%20/) [](http://requestchannel%20/)对象的创建，其内部关键成员对象如下：

1.  maxQueuedRequests：请求队列的大小，由 queued.max.requests 配置决定。
2.  dataPlaneProcessors: 缓存网络连接上数据平面数据处理器的 Map。
3.  dataPlaneAcceptors: 缓存网络连接的数据平面接收器的 Map。
4.  dataPlaneRequestChannel: 数据平面 RequestChannel请求队列对象，缓存收到的请求。
5.  controlPlaneRequestChannelOpt: 控制平面的 RequestChannel请求队列对象，默认大小为 20，由配置 control.plane.listener.name 决定是否创建，在 Kafka 3.0 版本的 KRaft 模式下如果该配置存在将报异常。

## **6.3 SocketServer#startup()**

可以看到 [SocketServer](http://socketserver%20/) [](http://socketserver%20/)对象被创建后立即就被调用了启动方法 [SocketServer.scala#startup](http://socketserver.scala/#startup)()，源码如下：

def startup(startProcessingRequests: Boolean = true,

controlPlaneListener: Option\[EndPoint\] = config.controlPlaneListener,

dataPlaneListeners: Seq\[EndPoint\] = config.dataPlaneListeners): Unit = {

// 调用 SocketServer.scala#createControlPlaneAcceptorAndProcessor() 方法创建控制平面的连接接收器及连接处理器，此处是兼容旧版本的处理，在 Kafka 3.0 版本的 KRaft 模式下不支持控制平面的 control.plane.listener.name 配置，故此方法调用可忽略

this.synchronized {

createControlPlaneAcceptorAndProcessor(controlPlaneListener)

// 调用 SocketServer.scala#createDataPlaneAcceptorsAndProcessors() 方法创建数据平面的连接接收器及连接处理器，此处该方法参数来源于默认参数，即 KafkaConfig.scala#dataPlaneListeners() 方法的返回值

createDataPlaneAcceptorsAndProcessors(config.numNetworkThreads, dataPlaneListeners)

if (startProcessingRequests) {

// 根据 startProcessingRequests 参数决定是否启动底层网络监听，此处是不启动的

this.startProcessingRequests()

}

}

....

}

核心步骤如下：

1.  调用 [SocketServer.scala#createControlPlaneAcceptorAndProcessor](http://socketserver.scala/#createControlPlaneAcceptorAndProcessor)() 方法创建控制平面的连接接收器及连接处理器，此处是兼容旧版本的处理，在 Kafka 3.0 版本的 KRaft 模式下不支持控制平面的 [control.plane.listener.name](http://control.plane.listener.name/) 配置，故此方法调用可忽略。
2.  调用 [SocketServer.scala#createDataPlaneAcceptorsAndProcessors](http://socketserver.scala/#createDataPlaneAcceptorsAndProcessors)() 方法创建数据平面的连接接收器及连接处理器，此处该方法参数来源于默认参数，即 [KafkaConfig.scala#dataPlaneListeners](http://kafkaconfig.scala/#dataPlaneListeners)() 方法的返回值。
3.  根据 [startProcessingRequests](http://startprocessingrequests/) 参数决定是否启动底层网络监听，此处是不启动的。

## **6.4 SocketServer#createDataPlanAcceptorAndProcessor()**

private def createDataPlaneAcceptorsAndProcessors(dataProcessorsPerListener: Int,

endpoints: Seq\[EndPoint\]): Unit = {

endpoints.foreach { endpoint =>

connectionQuotas.addListener(config, endpoint.listenerName)

// 首先调用 SocketServer.scala#createAcceptor() 创建连接接收器 Acceptor

val dataPlaneAcceptor \= createAcceptor(endpoint, DataPlaneMetricPrefix)

// 再调用 SocketServer.scala#addDataPlaneProcessors() 为连接接收器创建属于它的 Processor，Processor 个数由 num.network.threads 配置决定。

addDataPlaneProcessors(dataPlaneAcceptor, endpoint, dataProcessorsPerListener)

dataPlaneAcceptors.put(endpoint, dataPlaneAcceptor)

info(s"Created data-plane acceptor and processors for endpoint : ${endpoint.listenerName}")

}

}

该方法会根据配置文件中 listeners 配置的监听器列表，遍历监听器创建对应的 [Acceptor](http://acceptor%20/) [](http://acceptor%20/)和 [Processor](http://processor/)，其核心步骤如下：

1.  首先调用 [SocketServer.scala#createAcceptor](http://socketserver.scala/#createAcceptor)() 创建连接接收器 Acceptor。
2.  再调用 [SocketServer.scala#addDataPlaneProcessors](http://socketserver.scala/#addDataPlaneProcessors)() 为连接接收器创建属于它的 Processor，Processor 个数由 [num.network.threads](http://num.network.threads/) 配置决定。

## **6.5 SocketServer#createAccptor()**

![](https://article-images.zsxq.com/Frp0Y9dH0CR1k3Rn9chCTCz0Xu9U)

## **6.6 SocketServer#Accptor()**

![](https://article-images.zsxq.com/Fk3XpML3vzadshZniUY29QIgzsjv)

其比较关键的成员对象如下：

1.  nioSelector: Java 中的 Selector 对象，负责监听网络连接。
2.  serverChannel: Java 中的服务端 [ServerSocketChannel](http://serversocketchannel%20/) 对象，该对象由[SocketServer.scala#Acceptor#openServerSocket()](http://socketserver.scala/#Acceptor#openServerSocket\(\)) 方法创建，创建时会绑定监听端口。

## **6.7 SocketServer#addDataPlaneProcessors()**

切回 **6.4 第二步**，可以看到核心 for 循环创建  Processor。

  
![](https://article-images.zsxq.com/FmQhzCq530LnZy2kPjS1bQT6O_qL)

核心步骤如下：

1.  调用 [SocketServer.scala#newProcessor](http://socketserver.scala/#newProcessor)() 方法创建 Processor。
2.  调用 [RequestChannel.scala#addProcessor](http://requestchannel.scala/#addProcessor)() 方法将新创建的 Processor 对象保存在内部列表，后续将用于请求处理完成后响应的分配处理。
3.  for 循环结束，调用 [Acceptor#addProcessors()](http://acceptor/#addProcessors\(\)) 将创建的所有 Processor 缓存到内部，后续将用于新建连接的分配。

说完 Acceptor，我们在来剖析下 Processor。

## **6.8 Processor**

private\[kafka\] class Processor(val id: Int,

time: Time,

maxRequestSize: Int,

requestChannel: RequestChannel,

connectionQuotas: ConnectionQuotas,

connectionsMaxIdleMs: Long,

failedAuthenticationDelayMs: Int,

listenerName: ListenerName,

securityProtocol: SecurityProtocol,

config: KafkaConfig,

metrics: Metrics,

credentialProvider: CredentialProvider,

memoryPool: MemoryPool,

logContext: LogContext,

connectionQueueSize: Int,

isPrivilegedListener: Boolean,

apiVersionManager: ApiVersionManager) extends AbstractServerThread(connectionQuotas) with KafkaMetricsGroup {

......

private val newConnections \= new ArrayBlockingQueue\[SocketChannel\](connectionQueueSize)

private val inflightResponses \= mutable.Map\[String, RequestChannel.Response\]()

private val responseQueue \= new LinkedBlockingDeque\[RequestChannel.Response\]()

private\[kafka\] val metricTags \= mutable.LinkedHashMap(

ListenerMetricTag -> listenerName.value,

NetworkProcessorMetricTag -> id.toString

).asJava

newGauge(IdlePercentMetricName, () => {

Option(metrics.metric(metrics.metricName("io-wait-ratio", MetricsGroup, metricTags))).fold(0.0)(m =>

Math.min(m.metricValue.asInstanceOf\[Double\], 1.0))

},

// for compatibility, only add a networkProcessor tag to the Yammer Metrics alias (the equivalent Selector metric

// also includes the listener name)

Map(NetworkProcessorMetricTag -> id.toString)

)

val expiredConnectionsKilledCount \= new CumulativeSum()

private val expiredConnectionsKilledCountMetricName \= metrics.metricName("expired-connections-killed-count", MetricsGroup, metricTags)

metrics.addMetric(expiredConnectionsKilledCountMetricName, expiredConnectionsKilledCount)

private val selector \= createSelector(

ChannelBuilders.serverChannelBuilder(

listenerName,

listenerName == config.interBrokerListenerName,

securityProtocol,

config,

credentialProvider.credentialCache,

credentialProvider.tokenCache,

time,

logContext,

() => apiVersionManager.apiVersionResponse(throttleTimeMs = 0)

)

)

// Visible to override for testing

protected\[network\] def createSelector(channelBuilder: ChannelBuilder): KSelector = {

channelBuilder match {

case reconfigurable: Reconfigurable => config.addReconfigurable(reconfigurable)

case \_ \=\>

}

new KSelector(

maxRequestSize,

connectionsMaxIdleMs,

failedAuthenticationDelayMs,

metrics,

time,

"socket-server",

metricTags,

false,

true,

channelBuilder,

memoryPool,

logContext)

}

......

}

可以看到内部比较关键的属性:

1.  newConnections: 新连接列表，负责缓存由 Acceptor 接收后分配至 Processor 处理的连接。
2.  responseQueue: 响应列表，负责缓存请求处理完成后的响应。
3.  selector: Kafak 的 KSelector 对象，其内部封装着 Java 的 Selector，负责监听分配给 Processor 处理的连接。

## **6.9 KafkaRequestHandler#kafkaRequestHandlerPool()**

回到 **6.3 节** [BrokerServer.scala#startup()](http://brokerserver.scala/#startup\(\)) 方法内成员变量 [dataPlaneRequestHandlerPool](http://dataplanerequesthandlerpool%20/) 的赋值，可以看到实例为 [KafkaRequestHandler.scala#KafkaRequestHandlerPool](http://kafkarequesthandler.scala/#KafkaRequestHandlerPool) 类对象，并且持有了 KafkaApis 对象作为上层的请求处理器。

![](https://article-images.zsxq.com/Ft7x8rg2PxkRiSWUqpyYNJgX9554)

其关键属性如下：

1.  threadPoolSize: 处理器线程池大小，由配置 [num.io.threads](http://num.io.threads/) 决定。
2.  runnables: 处理器KafkaRequestHandler的数组，各个处理器由 [KafkaRequestHandler.scala#KafkaRequestHandlerPool#createHandler()](http://kafkarequesthandler.scala/#KafkaRequestHandlerPool#createHandler\(\)) 方法创建，可以看到实际处理器对象为封装了 KafkaApis 对象的 [KafkaRequestHandler](http://kafkarequesthandler%20/) 对象，该对象被创建后就扔进了新建线程中执行。

## **6.10 KafkaRequestHandler#kafkaRequestHandler()**

class KafkaRequestHandler(id: Int,

brokerId: Int,

val aggregateIdleMeter: Meter,

val totalHandlerThreads: AtomicInteger,

val requestChannel: RequestChannel,

apis: ApiRequestHandler,

time: Time) extends Runnable with Logging {

this.logIdent = s"\[Kafka Request Handler $id on Broker $brokerId\], "

private val shutdownComplete \= new CountDownLatch(1)

private val requestLocal \= RequestLocal.withThreadConfinedCaching

@volatile private var stopped \= false

def run(): Unit = {

while (!stopped) {

// We use a single meter for aggregate idle percentage for the thread pool.

// Since meter is calculated as total\_recorded\_value / time\_window and

// time\_window is independent of the number of threads, each recorded idle

// time should be discounted by # threads.

val startSelectTime \= time.nanoseconds

val req \= requestChannel.receiveRequest(300)

val endTime \= time.nanoseconds

val idleTime \= endTime - startSelectTime

aggregateIdleMeter.mark(idleTime / totalHandlerThreads.get)

req match {

case RequestChannel.ShutdownRequest =>

debug(s"Kafka request handler $id on broker $brokerId received shut down command")

completeShutdown()

return

case request: RequestChannel.Request =>

try {

request.requestDequeueTimeNanos = endTime

trace(s"Kafka request handler $id on broker $brokerId handling request $request")

apis.handle(request, requestLocal)

} catch {

case e: FatalExitError =>

completeShutdown()

Exit.exit(e.statusCode)

case e: Throwable => error("Exception when handling request", e)

} finally {

request.releaseBuffer()

}

case null \=\> // continue

}

}

completeShutdown()

}

private def completeShutdown(): Unit = {

requestLocal.close()

shutdownComplete.countDown()

}

def stop(): Unit = {

stopped = true

}

def initiateShutdown(): Unit = requestChannel.sendShutdownRequest()

def awaitShutdown(): Unit = shutdownComplete.await()

}

从定义可以看到其作为线程任务启动后会在 [KafkaRequestHandler.scala#run()](http://kafkarequesthandler.scala/#run\(\)) 方法中死循环不断处理底层接收到的请求，关键步骤如下：

1.  requestChannel.receiveRequest()不断轮询请求队列，获取请求，如没有请求则线程阻塞。
2.  apis.handle 调用接口方法 [ApiRequestHandler#handle()](http://apirequesthandler/#handle\(\)%20) ，将请求投递到处理器中进行处理。

上层的请求处理器已经启动，此时回到 **6.3 节** 启动底层网络服务器的方法[SocketServer.scala#startProcessingRequests()](http://socketserver.scala/#startProcessingRequests\(\))。

## **6.11 SocketServer#startProcessingRequests()**

def startProcessingRequests(authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\] = Map.empty): Unit = {

info("Starting socket server acceptors and processors")

this.synchronized {

if (!startedProcessingRequests) {

startControlPlaneProcessorAndAcceptor(authorizerFutures)

startDataPlaneProcessorsAndAcceptors(authorizerFutures)

startedProcessingRequests = true

} else {

info("Socket server acceptors and processors already started")

}

}

info("Started socket server acceptors and processors")

}

private def startDataPlaneProcessorsAndAcceptors(authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\]): Unit = {

val interBrokerListener \= dataPlaneAcceptors.asScala.keySet

.find(\_.listenerName == config.interBrokerListenerName)

val orderedAcceptors \= interBrokerListener match {

case Some(interBrokerListener) => List(dataPlaneAcceptors.get(interBrokerListener)) ++

dataPlaneAcceptors.asScala.filter { case (k, \_) => k != interBrokerListener }.values

case None \=\> dataPlaneAcceptors.asScala.values

}

orderedAcceptors.foreach { acceptor =>

val endpoint \= acceptor.endPoint

startAcceptorAndProcessors(DataPlaneThreadPrefix, endpoint, acceptor, authorizerFutures)

}

}

从源码中可以看到，此处核心为调用 [SocketServer.scala#startDataPlaneProcessorsAndAcceptors()](http://socketserver.scala/#startDataPlaneProcessorsAndAcceptors\(\)) 方法，最终启动 Acceptor 和 Prrocessor 的逻辑在 [SocketServer.scala#startAcceptorAndProcessors()](http://socketserver.scala/#startAcceptorAndProcessors\(\)) 方法中。

## **6.12 SocketServer#startAcceptorAndProcessors()**

private def startAcceptorAndProcessors(threadPrefix: String,

endpoint: EndPoint,

acceptor: Acceptor,

authorizerFutures: Map\[Endpoint, CompletableFuture\[Void\]\] = Map.empty): Unit = {

debug(s"Wait for authorizer to complete start up on listener ${endpoint.listenerName}")

waitForAuthorizerFuture(acceptor, authorizerFutures)

debug(s"Start processors on listener ${endpoint.listenerName}")

// 首先调用SocketServer.scala#Acceptor#startProcessors() 将 Acceptor 内部的 Processor 都扔进线程中启动

acceptor.startProcessors(threadPrefix)

debug(s"Start acceptor thread on listener ${endpoint.listenerName}")

// 其次新起线程，将当前 Acceptor 扔进线程中启动

if (!acceptor.isStarted()) {

KafkaThread.nonDaemon(

s"${threadPrefix}-kafka-socket-acceptor-${endpoint.listenerName}-${endpoint.securityProtocol}-${endpoint.port}",

acceptor

).start()

acceptor.awaitStartup()

}

info(s"Started $threadPrefix acceptor and processor(s) for endpoint : ${endpoint.listenerName}")

}

核心步骤如下：

1.  首先调用[SocketServer.scala#Acceptor#startProcessors](http://socketserver.scala/#Acceptor#startProcessors)() 将 Acceptor 内部的 Processor 都扔进线程中启动。
2.  其次新起线程，将当前 Acceptor 扔进线程中启动。

至此，「**kafka Raft 模式服务端启动**」就剖析完了。

## **07 Raft 模式服务端新建连接处理**

当 [Acceptor](http://acceptor/) 连接接收器启动后，会触发 [SocketServer.scala#Acceptor#run](http://socketserver.scala/#Acceptor#run)() 方法执行。

def run(): Unit = {

// 首先通过 serverChannel.register() 将服务端 ServerSocketChannel注册到 Selector 上，并设置监听的事件为 SelectionKey.OP\_ACCEPT

serverChannel.register(nioSelector, SelectionKey.OP\_ACCEPT)

startupComplete()

try {

while (isRunning) {

try {

// 在死循环中不断调用 SocketServer.scala#Acceptor#acceptNewConnections() 方法接收远端连接

acceptNewConnections()

closeThrottledConnections()

}

catch { .... }

}

} finally {

....

}

}

可以看到核心步骤如下：

1.  首先通过 [serverChannel.register()](http://serverchannel.register\(\)/) 将服务端 [ServerSocketChannel](http://serversocketchannel/) 注册到 [Selector](http://selector%20/) 上，并设置监听的事件为 [SelectionKey.OP\_ACCEPT](http://selectionkey.op_accept/)。
2.  在死循环中不断调用 [SocketServer.scala#Acceptor#acceptNewConnections](http://socketserver.scala/#Acceptor#acceptNewConnections)() 方法接收远端连接。

## **7.1 Acceptor#acceptNewConnetions()**

private def acceptNewConnections(): Unit = {

// 首先调用 nioSelector.select() 轮询底层连接，如有连接就绪则调用 nioSelector.selectedKeys() 获取事件处理。

val ready \= nioSelector.select(500)

if (ready > 0) {

val keys \= nioSelector.selectedKeys()

val iter \= keys.iterator()

while (iter.hasNext && isRunning) {

try {

val key \= iter.next

iter.remove()

if (key.isAcceptable) {

// 如果是接收连接事件，则调用 SocketServer.scala#Acceptor#accept() 进行连接接收

accept(key).foreach { socketChannel =>

// Assign the channel to the next processor (using round-robin) to which the

// channel can be added without blocking. If newConnections queue is full on

// all processors, block until the last one is able to accept a connection.

var retriesLeft \= synchronized(processors.length)

var processor: Processor = null

do {

retriesLeft -= 1

// 随后按照连接计数器取模选定一个 Processor

processor = synchronized {

// adjust the index (if necessary) and retrieve the processor atomically for

// correct behaviour in case the number of processors is reduced dynamically

currentProcessorIndex = currentProcessorIndex % processors.length

processors(currentProcessorIndex)

}

currentProcessorIndex += 1

// 最后调用 SocketServer.scala#Acceptor#assignNewConnection() 方法将连接分配给选定的 Processor。

} while (!assignNewConnection(socketChannel, processor, retriesLeft == 0))

}

} else

throw new IllegalStateException("Unrecognized key state for acceptor thread.")

} catch {

case e: Throwable => error("Error while accepting connection", e)

}

}

}

}

其实该方法就是标准的 NIO 处理，可以看到核心步骤如下：

1.  首先调用 [nioSelector.select()](http://nioselector.select\(\)%20/) 轮询底层连接，如有连接就绪则调用 [nioSelector.selectedKeys()](http://nioselector.selectedkeys\(\)/) 获取事件处理。
2.  如果是接收连接事件，则调用 [SocketServer.scala#Acceptor#accept()](http://socketserver.scala/#Acceptor#accept\(\)) 进行连接接收，随后按照连接计数器取模选定一个 Processor，最后调用 [SocketServer.scala#Acceptor#assignNewConnection()](http://socketserver.scala/#Acceptor#assignNewConnection\(\)) 方法将连接分配给选定的 Processor。

## **7.2 Acceptor#assignNewConnection()**

![](https://article-images.zsxq.com/Fju6dvi1AA0Z9zd46sGPokw9aNWW)

## **7.3 Processor#accept()**

![](https://article-images.zsxq.com/Fi3Q1Y9Z_CyMYkwEmQRTNIVcAE9L)

至此，「**kafka Raft 模式服务端新建连接处理**」就剖析完了。

##   
**08 Raft 模式服务端请求处理流程**

新连接入队后，需要将其注册到 Processor 的 Selector 上才能实现读写事件监听，这些主要在[SocketServer.scala#Processor#run](http://socketserver.scala/#Processor#run)() 方法中处理。

override def run(): Unit = {

startupComplete()

try {

while (isRunning) {

try {

// 首先调用 SocketServer.scala#Processor#configureNewConnections()方法将新连接列表中的连接注册到 Selector 上

configureNewConnections()

// 接下来调用 SocketServer.scala#Processor#processNewResponses()方法将响应列表中的响应存入连接缓存，注册监听可写事件SelectionKey.OP\_WRITE

processNewResponses()

// 调用 SocketServer.scala#Processor#poll()方法处理连接上的可读事件，将网络数据暂存到接收缓冲区

poll()

// 调用 SocketServer.scala#Processor#processCompletedReceives()方法将接收缓冲区的网络数据解析为 Kafka 请求，并将其扔进请求队列，等待请求处理器轮询处理

processCompletedReceives()

// 调用 SocketServer.scala#Processor#processCompletedSends() 方法处理成功发送的响应。对临时 Response 队列中的 Response 执行回调逻辑。

processCompletedSends()

processDisconnected()

closeExcessConnections()

} catch {

// We catch all the throwables here to prevent the processor thread from exiting. We do this because

// letting a processor exit might cause a bigger impact on the broker. This behavior might need to be

// reviewed if we see an exception that needs the entire broker to stop. Usually the exceptions thrown would

// be either associated with a specific socket channel or a bad request. These exceptions are caught and

// processed by the individual methods above which close the failing channel and continue processing other

// channels. So this catch block should only ever see ControlThrowables.

case e: Throwable => processException("Processor got uncaught exception.", e)

}

}

} finally {

debug(s"Closing selector - processor $id")

CoreUtils.swallow(closeAll(), this, Level.ERROR)

shutdownComplete()

}

}

可以看到该方法核心步骤如下：

1.  首先调用 [SocketServer.scala#Processor#configureNewConnections](http://socketserver.scala/#Processor#configureNewConnections)()方法将新连接列表中的连接注册到 Selector 上。
2.  接下来调用 [SocketServer.scala#Processor#processNewResponses](http://socketserver.scala/#Processor#processNewResponses)()方法将响应列表中的响应存入连接缓存，注册监听可写事件SelectionKey.OP\_WRITE。
3.  调用 [SocketServer.scala#Processor#poll](http://socketserver.scala/#Processor#poll)()方法处理连接上的可读事件，将网络数据暂存到接收缓冲区。
4.  调用 [SocketServer.scala#Processor#processCompletedReceives](http://socketserver.scala/#Processor#processCompletedReceives)()方法将接收缓冲区的网络数据解析为 Kafka 请求，并将其扔进请求队列，等待请求处理器轮询处理。
5.  调用 [SocketServer.scala#Processor#processCompletedSends](http://socketserver.scala/#Processor#processCompletedSends)() 方法处理成功发送的响应。对临时 Response 队列中的 Response 执行回调逻辑。

关于内部的这些方法，直接点击 [【服务端Broker源码分析系列第三篇】图解Kafka源码SocketServer组件之Porcessor线程架构设计](https://articles.zsxq.com/id_bh7qwrhpw9mb.html) 这篇进行学习，这里就不剖析了。

用一张时序图来总结下：

  
![](https://article-images.zsxq.com/FjiVshhAQY_mgEBt-Rk33qw7xHPV)

## **09 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头通过对「**kafka-server-start.sh**」内容进行剖析，引出了 「**kafka.Kafka**」类。

2、在「**kafka.Kafka**」的 buildServer 方法中有两种启动方式「**ZK**」、「**Raft**」来尝试启动 Kafka 服务器。

3、接着分别基于 「**Zk**」、「**Raft**」两种模式来剖析启动源码流程。

下篇我们来深度剖析「**Broker 启动集群如何感知**」，大家期待，我们下期见。