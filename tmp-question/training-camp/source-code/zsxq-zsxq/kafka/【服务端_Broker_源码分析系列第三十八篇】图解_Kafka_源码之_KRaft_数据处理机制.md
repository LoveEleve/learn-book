大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端** **KRaft Leader 选举机制流程**」，了解了 Kafka 中「**KRaft Leader**」节点是如何选举的以及是如何进行投票的，今天我们接着来深度聊聊「**Kafka 服务端 KRaft 数据处理机制**」，看看 Kafka Kraft Leader 是如何生成 Records 消息的以及是如何进行存储的。

![](https://article-images.zsxq.com/FqQkBieynPBffghyqELhVfpLXpcx)

## **01 总体概述**

在[【服务端 Broker 源码分析系列第三十七篇】图解 Kafka 源码之 KRaft Leader 选举机制流程](https://articles.zsxq.com/id_ox3kxhfx7li2.html) 这篇中，我们剖析了 Kafka Raft 集群启动时的「**Leader 节点**」的选举流程的源码实现，当「**Leader 节点**」选举成功后，其他「**Controller 节点**」都会成为「**Follower 节点**」，之后这些节点会组成一个 Raft 集群，此时「**Leader 节点**」就可以处理「**Controller**」请求。

今天这篇我们将以 Kafka 中最常用的「**Topic 创建场景**」来剖析「**Controller**」请求的运行原理，可以让你搞清楚 Topic 创建时的分区分配流程。

在 [【服务端 Broker 源码分析系列第二十二篇】图解 Kafka 源码之 Topic 创建请求处理流程](https://articles.zsxq.com/id_2q1nwfkg3fcb.html) 这篇中，我们剖析过「**Topic 创建请求处理流程**」，但不是以「**KRaft 模式**」的处理流程，今天我们以「**KRaft 模式**」来剖析。

本文涉及的源码：

「**ControllerApis**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerApis.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerApis.scala)

「**KafkaApis**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaApis.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaApis.scala)

「**MetadataSupport**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/MetadataSupport.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/KafkaApis.scala)

「**QuorumController**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/QuorumController.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/QuorumController.java)

「**KafkaEventQueue**」类源码在 Kafka 源码包的 server-common 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/server-common/src/main/java/org/apache/kafka/queue/KafkaEventQueue.java](https://github.com/apache/kafka/blob/3.0.0/server-common/src/main/java/org/apache/kafka/queue/KafkaEventQueue.java)

「**ClusterControlManager**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/ClusterControlManager.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/ClusterControlManager.java)

  
「**HeartbeatManager**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/BrokerHeartbeatManager.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/BrokerHeartbeatManager.java)

「**StripedReplicaPlacer**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/StripedReplicaPlacer.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/StripedReplicaPlacer.java)

「**KafkaRaftClient**」类源码在 Kafka 源码包的 raft 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java](https://github.com/apache/kafka/blob/3.0.0/raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java)

##   
**02 Controller 请求处理流程**

当「**Leader 节点**」收到「**Controller**」请求后，会生成并存储 Record 数据。这些 Record 数据存储了「**Controller**」请求的变更内容，比如当「**Leader 节点**」收到创建 Topic 的 「**Controller**」请求，那么生成的 Record 数据存储了「**创建 Topic**」的操作，以及「**Topic 名称**」、「**分区 Leader 副本**」、「**ISR 副本列表**」等内容。

其次「**Follower 节点**」会同步并存储「**Leader 节点**」的 Record 数据，当超过半数「**Controller 节点**」都同步了 Record 数据后，「**KRaft 模块**」就可以保证 Record 数据的安全，此时就可以提交这些数据了。

所以对于「**创建 Topic**」这种会更改集群元数据的请求，在「**KRaft 模式**」下都会交给 Kafka 集群的「**Leader 节点**」来处理。

首先这里我需要告诉大家的是，这类请求在「**KRaft 模式**」下是通过统一的异步处理框架来处理的，如下图：

![](https://article-images.zsxq.com/FppwAznLDsbOkNaMam0gvWOUO76F)

我们简单对上图进行剖析：

1.  **异步事件生成**：首先 [ControllerApis.scala](http://controllerapis.scala/) 将「**创建 Topic**」请求分发给 [QuorumController.java](http://quorumcontroller.java/)，由其负责生成封装了业务逻辑的异步事件 [ControllerWriteEvent](http://controllerwriteevent/)，并将事件投递到事件队列 [KafkaEventQueue.java](http://kafkaeventqueue.java/) 中。
2.  **异步事件消费**：事件处理器 [EventHandler](http://eventhandler%20/) 消费 [KafkaEventQueue.java](http://kafkaeventqueue.java/) 中的事件，封装在 [ControllerWriteEvent](http://controllerwriteevent/) 中的业务逻辑被触发执行，这里的业务逻辑主要指「**创建 Topic**」时的分区分配。
3.  **业务执行结果处理**：对业务处理的结果需要进行后续处理，包括给请求方返回响应以及将集群元数据变动写入内部 Cluster 主题「**\_\_cluster\_metadata**」 中。

## **2.1 生成事件以及 Record 数据**

当我们使用 [kafka-topics.sh](http://kafka-topics.sh/) 脚本创建主题时，该脚本会发送 [CreateTopics](http://createtopics/) 请求到 Broker 端，Broker 端会调用 [KafkaApis](http://kafkaapis/) 来处理请求。

如果 Kafka 服务启动了「**KRaft 模块**」，那么 [KafkaApis](http://kafkaapis/) 中会调用 maybeForwardToController 方法将 「**Controller**」请求转发给「**KRaft 模块**」的「**Leader 节点**」。

![](https://article-images.zsxq.com/FqN9RW5TtpaQKOkzB3tyM2SuapDe)

![](https://article-images.zsxq.com/FoMTkhqjpB8YHkGXGbh7e5-jXege)

![](https://article-images.zsxq.com/Fl0ZaqSBrOCiaWEDVd9j3gVFBbtp)

最终当客户端的请求抵达 Kafka 服务端 [ControllerServer](http://controllerserver/) 后，经过底层网络组件的协议解析处理转换为上层的 [Request](http://request/)，然后会分发到上层的 [ControllerApis.scala#handle()](http://controllerapis.scala/#handle\(\)) 方法进行业务逻辑分发。

![](https://article-images.zsxq.com/FuqjY9V037pBfoWTFOCGKwXws9sk)

## **2.1.1 handleCreateTopics()**

对于 [CreateTopics](http://createtopics/) 请求，处理方法是 [handleCreateTopics()](http://handlecreatetopics\(\)/)，源码如下：

def handleCreateTopics(request: RequestChannel.Request): Unit = {

val createTopicsRequest \= request.body\[CreateTopicsRequest\]

// 调用 ControllerApis.scala#createTopics() 方法将请求分发出去，并获取到一个异步任务 CompletableFuture 对象

val future \= createTopics(createTopicsRequest.data(),

// 首先使用 AuthHelper 组件进行必要的鉴权等操作

authHelper.authorize(request.context, CREATE, CLUSTER, CLUSTER\_NAME),

names => authHelper.filterByAuthorized(request.context, CREATE, TOPIC, names)(identity))

// 持有 CompletableFuture 对象，并调用其 CompletableFuture#whenComplete() 设置异步任务完成时的后续处理

future.whenComplete { (result, exception) =>

// 可以看到此处任务完成的主要处理是调用 RequestHelper.scala#sendResponseMaybeThrottle() 方法将处理结果发送给请求发起方

requestHelper.sendResponseMaybeThrottle(request, throttleTimeMs => {

if (exception != null) {

// 抛异常

createTopicsRequest.getErrorResponse(throttleTimeMs, exception)

} else {

result.setThrottleTimeMs(throttleTimeMs)

new CreateTopicsResponse(result)

}

})

}

}

可以看到其核心逻辑如下：

1.  首先使用 AuthHelper 组件进行必要的鉴权等操作。
2.  调用 [ControllerApis.scala#createTopics](http://controllerapis.scala/#createTopics)() 方法将请求分发出去，并获取到一个异步任务 CompletableFuture 对象。
3.  持有 [CompletableFuture](http://completablefuture/) 对象，并调用其 [CompletableFuture#whenComplete()](http://completablefuture/#whenComplete\(\)) 设置异步任务完成时的回调处理逻辑，可以看到此处任务完成的主要处理是调用 [RequestHelper.scala#sendResponseMaybeThrottle](http://requesthelper.scala/#sendResponseMaybeThrottle)() 方法将处理结果发送给请求发起方。

## **2.1.2 createTopics()**

def createTopics(request: CreateTopicsRequestData,

hasClusterAuth: Boolean,

getCreatableTopics: Iterable\[String\] => Set\[String\])

: CompletableFuture\[CreateTopicsResponseData\] = {

// 初始化主题名称集合

val topicNames \= new util.HashSet\[String\]()

// 初始化重复主题名称集合

val duplicateTopicNames \= new util.HashSet\[String\]()

// 首先检查过滤掉请求携带的 topic 列表中名称重复的 topic，确定需要执行创建动作的 topic 列表

request.topics().forEach { topicData =>

if (!duplicateTopicNames.contains(topicData.name())) {

if (!topicNames.add(topicData.name())) {

topicNames.remove(topicData.name())

duplicateTopicNames.add(topicData.name())

}

}

}

// 有集群授权，则所有 Topic 都可以创建

val authorizedTopicNames \= if (hasClusterAuth) {

topicNames.asScala

} else {

// 没有集群授权，则获取哪些 Topic 有创建权限

getCreatableTopics.apply(topicNames.asScala)

}

// 去掉没有创建权限的 Topic，构造新的请求对象

val effectiveRequest \= request.duplicate()

val iterator \= effectiveRequest.topics().iterator()

while (iterator.hasNext) {

val creatableTopic \= iterator.next()

if (duplicateTopicNames.contains(creatableTopic.name()) ||

!authorizedTopicNames.contains(creatableTopic.name())) {

iterator.remove()

}

}

// 调用 QuorumController.java#createTopics 方法，创建新的Topic并返回操作结果

controller.createTopics(effectiveRequest).thenApply { response =>

duplicateTopicNames.forEach { name =>

response.topics().add(new CreatableTopicResult().

setName(name).

setErrorCode(INVALID\_REQUEST.code).

setErrorMessage("Duplicate topic name."))

}

topicNames.forEach { name =>

if (!authorizedTopicNames.contains(name)) {

response.topics().add(new CreatableTopicResult().

setName(name).

setErrorCode(TOPIC\_AUTHORIZATION\_FAILED.code))

}

}

response

}

}

该方法处理比较清晰，关键步骤如下：

1.  首先检查过滤掉请求携带的 topic 列表中名称重复的 topic，确定需要执行创建动作的 topic 列表
2.  调用 [Controller.java#createTopics](http://quorumcontroller.java/#createTopics) 方法， 进行下一步处理，接口实现为 [QuorumController.java#createTopics()](http://quorumcontroller.java/#createTopics\(\)%C2%A0) 方法 创建新的 Topic 并返回操作结果。

![](https://article-images.zsxq.com/FmpRiXrgVq4zaCRhXPSwo4g-h_ON)

## **2.1.3 QuorumController#createTopics()**

可以看到接着调用该方法来「**创建 Topic**」的请求，源码如下：

@Override

public CompletableFuture<CreateTopicsResponseData>

createTopics(CreateTopicsRequestData request) {

// 如果为空直接返回空数据

if (request.topics().isEmpty()) {

return CompletableFuture.completedFuture(new CreateTopicsResponseData());

}

// 1、首先 appendWriteEvent 最后一个参数为一个匿名 ControllerWriteOperation 实例，该实例调用 ReplicationControlManager#createTopics 方法生成主题的 Record 数据

// 2、然后调用 QuorumController#appendWriteEvent 方法进行事件创建并将 Record 数据存储到 Cluster 主题中。

return appendWriteEvent("createTopics",

time.nanoseconds() + NANOSECONDS.convert(request.timeoutMs(), MILLISECONDS),

() -> replicationControl.createTopics(request));

}

这里执行两个操作，步骤如下：

1.  首先 [appendWriteEvent](http://appendwriteevent%20/) 最后一个参数为一个匿名 [ControllerWriteOperation](http://controllerwriteoperation/) 实例，该实例调用 [ReplicationControlManager#createTopics](http://replicationcontrolmanager/#createTopics) 方法生成主题的 Record 数据。
2.  然后调用 [QuorumController#appendWriteEvent](http://quorumcontroller/#appendWriteEvent) 方法进行事件创建并将 Record 数据存储到 Cluster 主题中。

## **2.1.3 ReplicationControlManager#createTopics()**

/\*\*

\* 创建新的 Topics。

\*/

ControllerResult<CreateTopicsResponseData> createTopics(CreateTopicsRequestData request) {

// 用于存储 Topic 错误信息的 Map

Map<String, ApiError> topicErrors = new HashMap<>();

// 用于存储记录的 List

List<ApiMessageAndVersion> records = new ArrayList<>();

// 1、校验 Topic 名称的合法性，并将错误信息存入 topicErrors

validateNewTopicNames(topicErrors, request.topics());

// 判断哪些 Topic 已经存在，并将错误信息存入 topicErrors

request.topics().stream()

.filter(creatableTopic -> topicsByName.containsKey(creatableTopic.name()))

.forEach(t -> topicErrors.put(t.name(), new ApiError(Errors.TOPIC\_ALREADY\_EXISTS)));

// 验证新的 Topics 的配置是否正确，并计算应当创建的 ConfigRecords

Map<ConfigResource, Map<String, Entry<OpType, String>>> configChanges =

computeConfigChanges(topicErrors, request.topics());

ControllerResult<Map<ConfigResource, ApiError>> configResult =

configurationControl.incrementalAlterConfigs(configChanges);

for (Entry<ConfigResource, ApiError> entry : configResult.response().entrySet()) {

if (entry.getValue().isFailure()) {

topicErrors.put(entry.getKey().name(), entry.getValue());

}

}

records.addAll(configResult.records());

// 2、创建需要的 Topic

Map<String, CreatableTopicResult> successes = new HashMap<>();

for (CreatableTopic topic : request.topics()) {

if (topicErrors.containsKey(topic.name())) continue;

ApiError error \= createTopic(topic, records, successes);

if (error.isFailure()) {

topicErrors.put(topic.name(), error);

}

}

// 创建 Topics 的响应数据

CreateTopicsResponseData data \= new CreateTopicsResponseData();

StringBuilder resultsBuilder \= new StringBuilder();

String resultsPrefix \= "";

for (CreatableTopic topic : request.topics()) {

ApiError error \= topicErrors.get(topic.name());

if (error != null) {

data.topics().add(new CreatableTopicResult()

.setName(topic.name())

.setErrorCode(error.error().code())

.setErrorMessage(error.message()));

resultsBuilder.append(resultsPrefix).append(topic).append(": ")

.append(error.error()).append(" (").append(error.message()).append(")");

resultsPrefix = ", ";

continue;

}

CreatableTopicResult result \= successes.get(topic.name());

data.topics().add(result);

resultsBuilder.append(resultsPrefix).append(topic).append(": ")

.append("SUCCESS");

resultsPrefix = ", ";

}

log.info("createTopics result(s): {}", resultsBuilder.toString());

// 返回创建 Topics 的结果数据

return ControllerResult.atomicOf(records, data);

}

该方法的重点是循环调用 [createTopic](http://createtopic/) 方法来创建需要的 Topic，步骤如下：

1.  首先对请求携带的 topic 校验，包括 topic 名称的校验及 topic 存在性校验等，还包括新的 topic 的配置校验
2.  如果校验通过则遍历 topic 列表，调用 [ReplicationControlManager#createTopic()](http://replicationcontrolmanager/#createTopic\(\)) 方法依次创建 topic。**需要注意此处参数是将消息列表 Records，该集合用来保存记录了 Topic 分区分配信息的消息**。
3.  最后调用 [ControllerResult#atomicOf()](http://controllerresult/#atomicOf\(\)) 方法将 topic 创建请求的响应和分区分配消息记录封装起来，作为业务逻辑的处理结果返回。

## **2.1.4 ReplicationControlManager#createTopic()**

/\*\*

\* 创建新的Topic

\*/

private ApiError createTopic(CreatableTopic topic, // 要创建的Topic对象

List<ApiMessageAndVersion> records, // 用于存储记录的List

Map<String, CreatableTopicResult> successes) { // 用于存储创建成功的Topic的Map

// 用于存储新分区的 Map

Map<Integer, PartitionRegistration> newParts = new HashMap<>();

// 1、请求中手动指定了分区分配方案，则进行方案校验，校验通过则直接采用手动分配方案完成该 topic 下的分区分配。

if (!topic.assignments().isEmpty()) {

....

} else if {

....

} else {

// 获取默认的分区数和复制因子

int numPartitions \= topic.numPartitions() == -1 ? defaultNumPartitions : topic.numPartitions();

short replicationFactor \= topic.replicationFactor() == -1 ? defaultReplicationFactor : topic.replicationFactor();

try {

// 2、请求中未手动指定分区方案，则使用内部算法进行 topic 下各个分区及其副本在 Broker 上的分配，这里主要通过 ClusterControlManager#placeReplicas() 方法进行为每个分区生成 AR 副本列表，并将结果存储到 newParts 变量中。

List<List<Integer>> replicas = clusterControl.placeReplicas(0, numPartitions, replicationFactor);

for (int partitionId \= 0; partitionId < replicas.size(); partitionId++) {

int\[\] r = Replicas.toArray(replicas.get(partitionId));

// 创建新的 PartitionRegistration 并将其添加至 newParts

newParts.put(partitionId, new PartitionRegistration(

r,

r,

Replicas.NONE,

Replicas.NONE,

r\[0\],

0,

0));

}

}

}

....

// 3、 生成 TopicRecord，存储主题变更记录，包括 Topic 名称，TopicId 等。

records.add(new ApiMessageAndVersion(new TopicRecord()

.setName(topic.name())

.setTopicId(topicId), TOPIC\_RECORD.highestSupportedVersion()));

// 4、生成 PartitionRecord，存储分区变更记录，包括 Leader 副本、ISR 副本列表、AR 副本列表等。

for (Entry<Integer, PartitionRegistration> partEntry : newParts.entrySet()) {

int partitionIndex \= partEntry.getKey();

PartitionRegistration info \= partEntry.getValue();

records.add(info.toRecord(topicId, partitionIndex));

}

// 5、返回 ApiError.NONE 表示创建成功

return ApiError.NONE;

}

该方法主要用来 **Leader 节点生成 Record 数据的**，步骤如下：

1.  判断请求中手动指定了分区分配方案，则进行方案校验，校验通过则直接采用手动分配方案完成该 topic 下的分区分配。
2.  判断请求中未手动指定分区方案，则使用内部算法进行 topic 下各个分区及其副本在 Broker 上的分配，这里主要通过 ClusterControlManager#placeReplicas() 方法进行为每个分区生成 AR 副本列表，并将结果存储到 newParts 变量中。
3.  生成 [TopicRecord](http://topicrecord/)，存储主题变更记录，包括 Topic 名称，TopicId 等。
4.  生成 [PartitionRecord](http://partitionrecord/)，存储分区变更记录，包括 Leader 副本、ISR 副本列表、AR 副本列表等。
5.  返回 ApiError.NONE 表示创建成功。

> 这里需要注意一个分区的分配信息都存储在 PartitionRegistration 对象中，该对象会保存分区下所有副本分布的 Broker 列表，并单独保存 Leader 副本所在的 Broker，从源码中可以看到 ISR 副本列表的第一个 Broker 节点上的副本将作为分区下所有副本的 Leader。

至此，「**Leader 节点**」生成 Record 数据的流程就剖析完了。

##   
**2.1.5 QuorumController#appendWriteEvent()**

接着我们将视角转回「**创建事件**」的方法，源码如下：

private <T> CompletableFuture<T> appendWriteEvent(String name,

long deadlineNs,

ControllerWriteOperation<T> op) {

// 初始化写事件

ControllerWriteEvent<T> event = new ControllerWriteEvent<>(name, op);

// 调用 EventQueue#appendWithDeadline() 将新建事件投递到事件队列

queue.appendWithDeadline(deadlineNs, event);

return event.future();

}

该方法将 [ControllerWriteEvent](http://controllerwriteevent/) 任务添加到 [QuorumController#queue](http://quorumcontroller/#queue) 队列中，这里我们称为 「**异步事件队列**」。而 [ControllerWriteEvent](http://controllerwriteevent/) 任务则负责生成并写入 Record 数据。

## **2.1.6 EventQueue#enqueue()**

通过下图可以看到 [appendWithDeadline](http://appendwithdeadline/) 方法最终是调用 [KafkaEventQueue.java#enqueue](http://kafkaeventqueue.java/#enqueue)() 方法实现事件入队。

![](https://article-images.zsxq.com/Fst_0zdxKUi5UrR25qdBxfFRLsZG)

![](https://article-images.zsxq.com/Fus8kK5zqgGAc6ctxZAP-G0-Z6d1)

// 事件处理器

private final EventHandler eventHandler;

// 事件处理器线程

private final Thread eventHandlerThread;

/\*\*

\* Kafka事件队列，用于处理Kafka事件的异步处理。

\*

\* @param time 时间实例，用于获取当前时间

\* @param logContext 日志上下文，用于记录日志

\* @param threadNamePrefix 线程名称前缀

\*/

public KafkaEventQueue(Time time,

LogContext logContext,

String threadNamePrefix) {

this.time = time;

this.lock = new ReentrantLock();

this.log = logContext.logger(KafkaEventQueue.class);

this.eventHandler = new EventHandler();

// 使用指定线程名称前缀创建 KafkaThread，并将 eventHandler 作为任务

this.eventHandlerThread = new KafkaThread(threadNamePrefix + "EventHandler",

this.eventHandler, false);

this.closingTimeNs = Long.MAX\_VALUE;

this.cleanupEvent = null;

// 启动事件处理线程

this.eventHandlerThread.start();

}

@Override

public void enqueue(EventInsertionType insertionType,

String tag,

Function<OptionalLong, OptionalLong> deadlineNsCalculator,

Event event) {

// 将异步事件封装到 EventContext 对象中

EventContext eventContext \= new EventContext(event, insertionType, tag);

// 调用 EventHandler#enqueue 方法将新建的 EventContext 对象加入到待处理队列

Exception e \= eventHandler.enqueue(eventContext, deadlineNsCalculator);

if (e != null) {

eventContext.completeWithException(e);

}

}

## **2.1.7 EventHandler#enqueue()**

该方法是最终将事件加入队列中的，源码如下：

private class EventHandler implements Runnable {

/\*\*

\* 将事件加入队列中。

\*/

Exception enqueue(EventContext eventContext,

Function<OptionalLong, OptionalLong> deadlineNsCalculator) {

lock.lock();

try {

// 判断队列是否正在关闭

if (closingTimeNs != Long.MAX\_VALUE) {

return new RejectedExecutionException();

}

OptionalLong existingDeadlineNs \= OptionalLong.empty();

// 如果事件元数据中有 tag，则放入 tagToEventContext 中

if (eventContext.tag != null) {

EventContext toRemove \= tagToEventContext.put(eventContext.tag, eventContext);

if (toRemove != null) {

existingDeadlineNs = toRemove.deadlineNs;

remove(toRemove);

}

}

// 计算事件的截止时间

OptionalLong deadlineNs \= deadlineNsCalculator.apply(existingDeadlineNs);

boolean queueWasEmpty \= head.isSingleton();

boolean shouldSignal \= false;

// 根据 insertionType 将事件插入队列

switch (eventContext.insertionType) {

case APPEND:

head.insertBefore(eventContext);

if (queueWasEmpty) {

shouldSignal = true;

}

break;

case PREPEND:

head.insertAfter(eventContext);

if (queueWasEmpty) {

shouldSignal = true;

}

break;

case DEFERRED:

if (!deadlineNs.isPresent()) {

// 如果是 DEFERRED 类型的事件，但没有设置截止时间则抛出异常

return new RuntimeException(

"You must specify a deadline for deferred events.");

}

break;

}

// 如果指定了截止时间，则将事件加入 deadlineMap 中，同时更新 eventContext 的截止时间

if (deadlineNs.isPresent()) {

long insertNs \= deadlineNs.getAsLong();

long prevStartNs \= deadlineMap.isEmpty() ? Long.MAX\_VALUE : deadlineMap.firstKey();

// 如果指定的时间点已经被占用，则选取下个时间点

while (deadlineMap.putIfAbsent(insertNs, eventContext) != null) {

insertNs++;

}

eventContext.deadlineNs = OptionalLong.of(insertNs);

// 如果插入的截止时间小于原有截止时间，则需要唤醒超时线程

if (insertNs <= prevStartNs) {

shouldSignal = true;

}

}

// 如果队列之前为空，表示需要唤醒事件处理线程

if (shouldSignal) {

cond.signal();

}

} finally {

lock.unlock();

}

return null;

}

....

}

至此「**事件生成并入队**」的源码流程就剖析完了。

##   
**2.2 消费事件以及存储 Record 数据**

在上一小节，我们「**异步事件**」 和 「**Record 数据**」已经被写入到事件队列中了，接下来剖析下事件是如何被消费的。

事件消费其实就是由 [EventHandler](http://eventhandler/) 事件处理器来完成的，它实现了 [Runnable](http://runnable/) 接口，会在事件队列[KafkaEventQueue](http://kafkaeventqueue/) 被创建的时候启动，触发 [EventHandler#run()](http://eventhandler/#run\(\)) 方法执行，可以看到其核心是执行 [EevntHandler#handleEvents()](http://eevnthandler/#handleEvents\(\)) 方法：

@Override

public void run() {

try {

// 处理事件

handleEvents();

cleanupEvent.run();

} catch (Throwable e) {

log.warn("event handler thread exiting with exception", e);

}

}

## **2.2.1 EventHandler#handleEvents()**

该方法会在 while 死循环中不断轮询获取内部队列中的 [EventContext](http://eventcontext%20/) [](http://eventcontext%20/) 对象，一旦获取到则调用 [EventContext#run()](http://eventcontext/#run\(\)) 方法完成事件消费。

/\*\*

\* 处理事件的方法，如果队列中没有事件则该方法会阻塞直到有事件到来。

\* @throws InterruptedException 如果线程被中断时抛出此异常

\*/

private void handleEvents() throws InterruptedException {

EventContext toTimeout \= null;

EventContext toRun \= null;

while (true) {

if (toTimeout != null) {

// 处理超时事件

toTimeout.completeWithTimeout();

toTimeout = null;

} else if (toRun != null) {

// 处理未超时事件

toRun.run(log);

toRun = null;

}

lock.lock();

try {

long awaitNs \= Long.MAX\_VALUE;

Map.Entry<Long, EventContext> entry = deadlineMap.firstEntry();

if (entry != null) {

// 如果队列中有超时或准备运行的延迟事件，则处理该事件

long now \= time.nanoseconds();

long timeoutNs \= entry.getKey();

EventContext eventContext \= entry.getValue();

if (timeoutNs <= now) {

if (eventContext.insertionType == EventInsertionType.DEFERRED) {

// 如果是准备运行的延迟事件，则将其移出 deadlineMap，加入队列

remove(eventContext);

toRun = eventContext;

} else {

// 如果是普通事件，且超时了，则将其移出 deadlineMap，置为超时事件

remove(eventContext);

toTimeout = eventContext;

}

continue;

} else if (closingTimeNs <= now) {

// 如果队列正在关闭，则将事件移出 deadlineMap，置为超时事件

remove(eventContext);

toTimeout = eventContext;

continue;

}

awaitNs = timeoutNs - now;

}

if (head.next == head) {

if ((closingTimeNs != Long.MAX\_VALUE) && deadlineMap.isEmpty()) {

// 如果队列为空且队列正在关闭，则直接退出循环

return;

}

} else {

// 如果队列中有普通事件，则将其移出队列

toRun = head.next;

remove(toRun);

continue;

}

if (closingTimeNs != Long.MAX\_VALUE) {

long now \= time.nanoseconds();

// 计算还需等待的时间

if (awaitNs > closingTimeNs - now) {

awaitNs = closingTimeNs - now;

}

}

if (awaitNs == Long.MAX\_VALUE) {

cond.await();

} else {

cond.awaitNanos(awaitNs);

}

} finally {

lock.unlock();

}

}

}

##   
**2.2.2 EventContext#run()**

void run(Logger log) throws InterruptedException {

try {

// 调用 Event#run() 方法触发任务执行，在这里也就是触发 ControllerWriteEvent#run() 方法

event.run();

} catch (InterruptedException e) {

throw e;

} catch (Exception e) {

try {

event.handleException(e);

} catch (Throwable t) {

log.error("Unexpected exception in handleException", t);

}

}

}

该方法的重要步骤就是：调用 [Event#run()](http://event/#run\(\)) 方法触发任务执行，在这里也就是触发 [ControllerWriteEvent#run()](http://controllerwriteevent/#run\(\)) 方法。

## **2.2.3 ControllerWriteEvent#run()**

![](https://article-images.zsxq.com/FktHO2ftFq8wNYrbZpnsgYVLO9te)

@Override

public void run() throws Exception {

long now \= time.nanoseconds();

controllerMetrics.updateEventQueueTime(NANOSECONDS.toMillis(now - eventCreatedTimeNs));

int controllerEpoch \= curClaimEpoch;

if (controllerEpoch == -1) {

throw newNotControllerException();

}

startProcessingTimeNs = Optional.of(now);

// 1、ControllerWriteEvent#op 是一个 ControllerWriteOperation 实例，其 generateRecordsAndResult 的方法负责生成对应的 Record 数据。

ControllerResult<T> result = op.generateRecordsAndResult();

if (result.records().isEmpty()) {

....

} else {

// 2、如果执行到这里，表示 ControllerWriteEvent#op 已经生成了 Record 数据，然后将 Record 数据添加到 LeaderState#accumulator 中。

final long offset;

if (result.isAtomic()) {

offset = raftClient.scheduleAtomicAppend(controllerEpoch, result.records());

} else {

offset = raftClient.scheduleAppend(controllerEpoch, result.records());

}

op.processBatchEndOffset(offset);

writeOffset = offset;

resultAndOffset = ControllerResultAndOffset.of(offset, result);

// 3、将 Record 中的变更操作应用于 Controller 节点的数据视图，得到最新的集群元数据

for (ApiMessageAndVersion message : result.records()) {

replay(message.message(), Optional.empty(), offset);

}

// 根据需要生成数据快照

snapshotRegistry.getOrCreateSnapshot(offset);

log.debug("Read-write operation {} will be completed when the log " +

"reaches offset {}.", this, resultAndOffset.offset());

}

// 4、在 QuorumController#purgatory 中添加一个延迟任务

purgatory.add(resultAndOffset.offset(), this);

}

可以看到这个运行方法主要用来**生成 Record 数据并添加到暂存器中**，步骤如下：

1.  [ControllerWriteEvent#op](http://controllerwriteevent/#op) 是一个 [ControllerWriteOperation](http://controllerwriteoperation/) 实例，该 [generateRecordsAndResult](http://generaterecordsandresult%20/) 接口负责生成对应的 [Record](http://record/) 数据，它会触发前面 **2.1.3** 节步骤中设置的业务逻辑处理，即触发 [ReplicationControl.java#createTopics](http://replicationcontrol.java/#createTopics)() 方法执行。
2.  在前面剖析的 [QuorumController#createTopics](http://quorumcontroller/#createTopics) 方法用来处理创建 Topic 请求的，它实现了 [ControllerWriteOperation](http://controllerwriteoperation/) 接口，返回主题的 [Record](http://record/) 数据。
3.  当业务处理完成后，表示 [ControllerWriteEvent#op](http://controllerwriteevent/#op) 已经生成了 [Record](http://record/) 数据，然后根据处理结果进行后续处理。如果处理结果中的消息记录不为空，根据 [ControllerResult.isAtomic](http://controllerresult.isatomic/) 属性确定向集群元数据 topic 写入消息的方式，对于创建 topic 的请求，此处将调用 [KafkaRaftClient.java#scheduleAtomicAppend()](http://kafkaraftclient.java/#scheduleAtomicAppend\(\)) 方法将 [Record](http://record/) 数据添加到 [LeaderState#accumulator](http://leaderstate/#accumulator%20) 中。
4.  将 Record 中的变更操作应用于 Controller 节点的数据视图，得到最新的集群元数据。然后根据需要生成数据快照。
5.  Controller 节点不仅存储数据到 Cluster 主题中，还在内存中维护了一个数据视图，该试图存储了最新的集群元数据用来生成数据快照。
6.  在 [QuorumController#purgatory](http://quorumcontroller/#purgatory) 中添加一个延迟任务。主要是因为在 KRaft 模式中， Leader 节点写入数据后，需要等待超过半数的 Controller 节点同步数据后，才认为该操作写入成功，然后返回成功响应给客户端，因此这里需要添加一个延迟任务，等待 COntroller 节点数据同步。
7.  这里会将当前 [ControllerWriteEvent](http://controllerwriteevent/) 对象作为监听器监听元数据偏移量 offset 的移动，当目标 offset 抵达时，[ControllerWriteEvent#complete()](http://controllerwriteevent/#complete\(\)%20) 方法将被执行，进而通过 [CompletableFuture](http://completablefuture%20/) 一路回调触发异步任务，最终实现 **2.1.1** 节步骤中提到的将请求的处理结果发送给请求方。

## **2.3 创建主题的分区分配过程**

在 **2.1.4 部分**剖析到了最后通过 [ClusterControlManager#placeReplicas()](http://clustercontrolmanager/#placeReplicas\(\)) 来进行分配，接下来我们接着来剖析。

## **2.3.1 ClusterControlManager#placeReplicas()**

public List<List<Integer>> placeReplicas(int startPartition,

int numPartitions,

short numReplicas) {

if (heartbeatManager == null) {

throw new RuntimeException("ClusterControlManager is not active.");

}

// 重点就是调用 BrokerHeartbeatManager#placeReplicas()

return heartbeatManager.placeReplicas(startPartition, numPartitions, numReplicas,

id -> brokerRegistrations.get(id).rack(), replicaPlacer);

}

## **2.3.2 BrokerHeartbeatManager#placeReplicas()**

List<List<Integer>> placeReplicas(int startPartition,

int numPartitions,

short numReplicas,

Function<Integer, Optional<String>> idToRack,

ReplicaPlacer placer) {

Iterator<UsableBroker> iterator = new UsableBrokerIterator(

brokers.values().iterator(), idToRack);

return placer.place(startPartition, numPartitions, numReplicas, iterator);

}

可以看到该方法其实也只是个入口，核心的分区分配功能由 [ReplicaPlacer接口#place()](http://replicaplacer/#place\(\)) 的方法完成，通过追踪 [StripedReplicaPlacer#place()](http://stripedreplicaplacer/#place\(\)) 方法为最终实现。

##   
**2.3.3 StripedReplicaPlacer#place()**

@Override

public List<List<Integer>> place(int startPartition,

int numPartitions,

short replicationFactor,

Iterator<UsableBroker> iterator) {

// 1、初始化一个 RackList 对象来作为分区分配的处理器

RackList rackList \= new RackList(random, iterator);

// 2、检查分区副本数设置是否合法，如果分区副本数参数大于集群内 Broker 节点的总数量则抛出异常

throwInvalidReplicationFactorIfNonPositive(replicationFactor);

throwInvalidReplicationFactorIfZero(rackList.numUnfencedBrokers());

throwInvalidReplicationFactorIfTooFewBrokers(replicationFactor, rackList.numTotalBrokers());

List<List<Integer>> placements = new ArrayList<>(numPartitions);

// 3、遍历分区列表，调用 RackList#place() 方法将每一个分区下的副本分配到各个 Broker 上

for (int partition \= 0; partition < numPartitions; partition++) {

placements.add(rackList.place(replicationFactor));

}

return placements;

}

![](https://article-images.zsxq.com/Fps-d-GIy5AMCDPcrSwGJLunEhxO)

核心步骤如下：

1.  初始化一个 RackList 对象来作为分区分配的处理器。
2.  检查分区副本数设置是否合法，如果分区副本数参数大于集群内 Broker 节点的总数量则抛出异常。
3.  遍历分区列表，调用 [RackList#place()](http://racklist/#place\(\)) 方法将每一个分区下的副本分配到各个 Broker 上。

## **2.3.4 RackList#place()**

RackList 类是 **StripedReplicaPlacer 类中子类**，在该方法内部使用成员变量记录每次分区分配的信息，使用这个数据来**类避免各个分区的 Leader 副本集中在一个 Broker 节点上**。

/\*\*

\* 该方法会返回一个包含副本的 broker ID 列表，其中第一个 Broker 来自获取的第一个 Rack，排除任何已经被剔除的 broker。其中副本的数量由参数 replicationFactor 指定)

\*/

List<Integer> place(int replicationFactor) {

// 检查参数是否合法

throwInvalidReplicationFactorIfNonPositive(replicationFactor);

throwInvalidReplicationFactorIfTooFewBrokers(replicationFactor, numTotalBrokers());

throwInvalidReplicationFactorIfZero(numUnfencedBrokers());

// 如果已经返回的分配 epoch 数等于 unfencedbrokers 的数，则打乱机架列表和 broker 列表，尝试避免重复分配，但是如果只有一个 unfenced broker，则不需要打乱

if (epoch == numUnfencedBrokers && numUnfencedBrokers > 1) {

shuffle();

epoch = 0;

}

// 如果 offset 等于机架名称列表的长度，则重置 offset 为 0

if (offset == rackNames.size()) {

offset = 0;

}

List<Integer> brokers = new ArrayList<>(replicationFactor);

// 将 offset 作为第一个机架索引

int firstRackIndex \= offset;

// 找到第一个可用的 unfenced broker，并将其添加到结果列表中

while (true) {

Optional<String> name = rackNames.get(firstRackIndex);

Rack rack \= racks.get(name);

int result \= rack.nextUnfenced(epoch);

if (result >= 0) {

brokers.add(result);

break;

}

firstRackIndex++;

if (firstRackIndex == rackNames.size()) {

firstRackIndex = 0;

}

}

// 下一个副本的 broker

int rackIndex \= offset;

for (int replica \= 1; replica < replicationFactor; replica++) {

int result \= -1;

do {

if (rackIndex == firstRackIndex) {

firstRackIndex = -1;

} else {

Optional<String> rackName = rackNames.get(rackIndex);

Rack rack \= racks.get(rackName);

result = rack.next(epoch);

}

rackIndex++;

if (rackIndex == rackNames.size()) {

rackIndex = 0;

}

} while (result < 0);

// 将剩余的可用 broker 添加到结果列表中

brokers.add(result);

}

epoch++;

offset++;

return brokers;

}

/\*\*

\* 打乱机架名称列表和机架列表中每个 Rack 中的 broker 列表

\*/

void shuffle() {

Collections.shuffle(rackNames, random);

for (Rack rack : racks.values()) {

rack.shuffle(random);

}

}

分配过程如下图所示：

![](https://article-images.zsxq.com/FgR3v8P0Ticw5qSfokM5fv06NIUR)

至此，创建 Topic 过程中的「**分区副本分配**」就剖析完成了。

##   
**2.4 业务执行结果处理流程**

当业务处理结束后，切回到 **2.2.3** 步骤，如果本次创建 Topic 请求确实产生了 Records 消息，则需要将其写入内部 主题「**\_\_cluster\_metadata**」中 ，这里将触发 [KafkaRaftClient#scheduleAtomicAppend()](http://kafkaraftclient/#scheduleAtomicAppend\(\)) 方法执行，可以看到这里的核心是调用 [KafkaRaftClient#append()](http://kafkaraftclient.java/#append\(\)) 方法，源码如下：

@Override

public long scheduleAtomicAppend(int epoch, List<T> records) {

return append(epoch, records, true);

}

private long append(int epoch, List<T> records, boolean isAtomic) {

// 1、首先是调用 QuorumState#maybeLeaderState() 方法进行 Controller 节点状态检查，如果当前节点已经不是 Leader，则不能继续处理

LeaderState<T> leaderState = quorum.<T>maybeLeaderState().orElseThrow(

() -> new NotLeaderException("Append failed because the replication is not the current leader")

);

// 2、当检查通过后，调用 QuorumState#accumulator() 方法获取批量消息暂存器BatchAccumulator，随后调用 BatchAccumulator.appendAtomic() 方法暂存消息

BatchAccumulator<T> accumulator = leaderState.accumulator();

boolean isFirstAppend \= accumulator.isEmpty();

final long offset;

if (isAtomic) {

offset = accumulator.appendAtomic(epoch, records);

} else {

offset = accumulator.append(epoch, records);

}

if (isFirstAppend || accumulator.needsDrain(time.milliseconds())) {

wakeup();

}

return offset;

}

核心步骤如下：

1.  首先是调用 [QuorumState#maybeLeaderState()](http://quorumstate/#maybeLeaderState\(\)) 方法进行 Controller 节点状态检查，如果当前节点已经不是 Leader，则不能继续处理。
2.  当检查通过后，调用 [QuorumState#accumulator()](http://quorumstate/#accumulator\(\)) 方法获取批量消息暂存器 [BatchAccumulator](http://batchaccumulator/)，随后调用 [BatchAccumulator.appendAtomic()](http://batchaccumulator.appendatomic\(\)/) 方法暂存消息。

由于篇幅问题，这里就不再继续追踪暂存方法逻辑了，可以根据我给出的自行追踪研究。

当消息被暂存下来后，写入的动作将被 [KafkaRaftClient#poll()](http://kafkaraftclient.java/#poll\(\)%20) 方法异步触发，在 [【服务端 Broker 源码分析系列第三十七篇】图解 Kafka 源码之 KRaft Leader 选举机制流程](https://articles.zsxq.com/id_ox3kxhfx7li2.html) 上一篇中我们深度剖析过，直接看这篇的剖析即可，这里就不再赘述。

![](https://article-images.zsxq.com/FnZSn3kjjbZ1H-RV6sFvqvBZLwdA)

至此，「**业务执行结果处理流程**」就剖析完了。

## **03 Controller 请求处理流程总结**

这里通过一张时序图来梳理下整个处理流程，如下：

![](https://article-images.zsxq.com/FsXWLgKPv0sRYlw10X4KK3yLcH4w)

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头引出了「**Topic 创建场景**」来剖析「**Controller**」请求的处理流程是怎样的。

2、接着带大家深度剖析了「**Controller 请求处理流程**」，包括四个维度：「**生成事件以及 Record 数据**」、「**消费事件以及存储 Record 数据**」、「**创建主题的分区分配流程**」、「**业务执行结果处理流程**」。

3、最后通过流程图带大家梳理了整个处理流程，希望让你更好的理解。

下篇我们来深度剖析「**Kraft 元数据主从同步机制**」，大家期待，我们下期见。