大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**KafkaRequestHandler I/O 线程池架构设计与源码实现**」，今天我们就来深度聊聊「**KafkaApis 的源码实现**」，看看 Kafka 服务端是如何真正处理网络请求的。

![](https://article-images.zsxq.com/Fk52g3Vx4PWElIDB2UHdihjP_8ye)

## **01 总体概述**

最近这几篇文章都是基于下面这张图来展开的，这是 Kafka 服务端超高并发的网络通信架构，如下图所示：

![](https://article-images.zsxq.com/lojiZJNqUko4zFwqFJ8m4NNg9614)

在上一篇中，我们从整体上带你分析了「**KafkaRequestHandler 业务线程**」和 「**KafkaRequestHandlerPool 业务线程池**」的架构设计和源码实现，它主要用来管理业务线程池包括创建、关闭、以及处理业务请求，今天这篇我们的主角是「**KafkaApis**」类源码实现。

为了方便大家理解，所有的源码只保留骨干。

## **02 KafkaApis 类**

众所周知，「**KafkaApis**」类是 Kafka 中最最重要的源码入口，如果你想了解 Kafka 某个功能的源码实现，一定会先从 [KafkaApis.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaApis.scala) 文件中查找，然后一层层往里追溯，直到定位到对应实现功能的代码为止。这里举个例子：

1.  **如果你想了解关于 Kafka 是如何创建和删除 Topic 的**，只需要查看 handleCreateTopicsRequest(request)、handleDeleteTopicsRequest(request) 这 2 个方法即可。
2.  **如果你想了解关于 Kafka 是如何选举 Leader 的**，只需要查看 handleElectReplicaLeader(request) 这个方法即可。
3.  **如果你想了解关于 Kafka 是如何更新元数据的**，只需要查看 handleUpdateMetadataRequest(request) 这个方法即可。
4.  **如果你想了解关于 Kafka 是如何提交位移的**，只需要查看 handleOffsetCommitRequest(request) 这个方法即可。

通过这样慢慢掌握 Kafka 实现个各种功能的源码分布，让你**慢慢建立对 Kafka 源码的整体认知**。

KafkaApis 类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaApis.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaApis.scala)

这就来看看大名鼎鼎的 KafkaApis 入口文件吧，今天这篇主要是**向大家分享如何自我学习的认知能力**。

##   
**2.1 KafkaApis 类定义**

它的代码非常的规整，其实现逻辑并不复杂，大部分代码都是用来处理 Kafka 请求的，代码如下：

/\*\*

\* Logic to handle the various Kafka requests

\*/

class KafkaApis(val requestChannel: RequestChannel, // 请求通道

val replicaManager: ReplicaManager, // 副本管理器

val adminManager: AdminManager, // 主题、分区、配置等相关处理器

val groupCoordinator: GroupCoordinator, // 消费者组协调器

val txnCoordinator: TransactionCoordinator, // 事务协调器

val controller: KafkaController, // 控制器

val zkClient: KafkaZkClient, // zk 客户端

val brokerId: Int, // brokerid

val config: KafkaConfig, // Kafka 配置类

val metadataCache: MetadataCache, // 元数据缓存类

val metrics: Metrics, // 监控

val authorizer: Option\[Authorizer\], // 授权

val quotas: QuotaManagers, // 配额管理器

val fetchManager: FetchManager, // 拉取管理器

brokerTopicStats: BrokerTopicStats, // topic 状态

val clusterId: String,

time: Time,

val tokenManager: DelegationTokenManager,

val brokerFeatures: BrokerFeatures,

val finalizedFeatureCache: FinalizedFeatureCache) extends ApiRequestHandler with Logging {

type FetchResponseStats \= Map\[TopicPartition, RecordConversionStats\]

this.logIdent = "\[KafkaApi-%d\] ".format(brokerId)

// zk 客户端，3.x 中被移除

val adminZkClient \= new AdminZkClient(zkClient)

private val alterAclsPurgatory \= new DelayedFuturePurgatory(purgatoryName = "AlterAcls", brokerId = config.brokerId)

.....

如果你想研究3.x版本的，下面是最新版地址如下：[https://github.com/apache/kafka/blob/3.4.0/core/src/main/scala/kafka/server/KafkaApis.scala](https://github.com/apache/kafka/blob/3.4.0/core/src/main/scala/kafka/server/KafkaApis.scala) , 逻辑实现上大差不差，可能有些被遗弃了，这块就暂时不带你剖析了，可以通过本篇的学习技能自行学习，如果有问题，随时联系我。

下面通过一张图来总结这些管理器。

![](https://article-images.zsxq.com/FrMEJbvkT4SCDXDX2jbU9vcQRoJE)

从上图可以得出，KafkaApis 类集成了众多「**重量级组件**」，主要是因为在处理不同类型的 RPC 请求时，KafkaApis 会用到不同的组件，因此在创建 KafkaApis 实例时，必须把可能用到的组件都传给它，另外也是你研究这些「**重量级组件**」源码实现原理的入口。

当你点开上面 KafkaApis 类文件时，你会发现它封装了很多关于 handle 开头的方法，每个方法对应一类请求类型，它的总入口方法是 handle()，下面我们就先来看看这个总入口都做了哪些事情：

##   
**2.2 handle() 总入口**

/\*\*

\* Top-level method that handles all requests and multiplexes to the right api

\*/

override def handle(request: RequestChannel.Request): Unit = {

try {

....

// 根据请求头信息中的 apiKey 字段判断属于哪类请求，然后调用对应的 handle\*\*\*\* 方法

request.header.apiKey match {

// 处理生产者的请求，将消息写入 Kafka。

case ApiKeys.PRODUCE => handleProduceRequest(request)

// 处理消费者的请求，从 Kafka 中读取消息。

case ApiKeys.FETCH => handleFetchRequest(request)

// 获取指定分区指定时间戳或偏移量的最近的偏移量。

case ApiKeys.LIST\_OFFSETS => handleListOffsetRequest(request)

// 获取 Kafka 集群的元数据信息。

case ApiKeys.METADATA => handleTopicMetadataRequest(request)

// 更改分区的领导者或ISR集。

case ApiKeys.LEADER\_AND\_ISR => handleLeaderAndIsrRequest(request)

// 停止指定分区的复制。

case ApiKeys.STOP\_REPLICA => handleStopReplicaRequest(request)

// 将 Kafka 集群的元数据信息更新到 Broker 上。

case ApiKeys.UPDATE\_METADATA => handleUpdateMetadataRequest(request)

// 控制 Broker 的关机操作。

case ApiKeys.CONTROLLED\_SHUTDOWN => handleControlledShutdownRequest(request)

// 将消费者组的偏移量提交到 Kafka 中。

case ApiKeys.OFFSET\_COMMIT => handleOffsetCommitRequest(request)

// 从 Kafka 中获取消费者组当前的偏移量。

case ApiKeys.OFFSET\_FETCH => handleOffsetFetchRequest(request)

// 查找并获取指定消费者组或事务的协调器。

case ApiKeys.FIND\_COORDINATOR => handleFindCoordinatorRequest(request)

// 让消费者加入指定的消费者组。

case ApiKeys.JOIN\_GROUP => handleJoinGroupRequest(request)

// 消费者发送心跳给协调器，以表明其仍然是存活的。

case ApiKeys.HEARTBEAT => handleHeartbeatRequest(request)

// 让消费者离开指定的消费者组。

case ApiKeys.LEAVE\_GROUP => handleLeaveGroupRequest(request)

// 同步消费者组的状态。

case ApiKeys.SYNC\_GROUP => handleSyncGroupRequest(request)

// 获取消费者组的描述信息。

case ApiKeys.DESCRIBE\_GROUPS => handleDescribeGroupRequest(request)

// 列出所有消费者组。

case ApiKeys.LIST\_GROUPS => handleListGroupsRequest(request)

// SASL握手，用于客户端认证。

case ApiKeys.SASL\_HANDSHAKE => handleSaslHandshakeRequest(request)

// 获取支持的API版本信息。

case ApiKeys.API\_VERSIONS => handleApiVersionsRequest(request)

// 创建新的Topic。

case ApiKeys.CREATE\_TOPICS => handleCreateTopicsRequest(request)

// 删除指定的Topic。

case ApiKeys.DELETE\_TOPICS => handleDeleteTopicsRequest(request)

// 删除 Kafka 中指定分区或多个分区中的记录。

case ApiKeys.DELETE\_RECORDS => handleDeleteRecordsRequest(request)

// 初始化生产者ID。

case ApiKeys.INIT\_PRODUCER\_ID => handleInitProducerIdRequest(request)

// 获取指定分区中指定副本的最新偏移量。

case ApiKeys.OFFSET\_FOR\_LEADER\_EPOCH => handleOffsetForLeaderEpochRequest(request)

// 将指定分区添加到事务中。

case ApiKeys.ADD\_PARTITIONS\_TO\_TXN => handleAddPartitionToTxnRequest(request)

// 将消费者组的偏移量添加到事务中。

case ApiKeys.ADD\_OFFSETS\_TO\_TXN => handleAddOffsetsToTxnRequest(request)

// 标识事务结束。

case ApiKeys.END\_TXN => handleEndTxnRequest(request)

// 将写入事务标记写入 Kafka 中。

case ApiKeys.WRITE\_TXN\_MARKERS => handleWriteTxnMarkersRequest(request)

// 将消费者组的偏移量提交到 Kafka 中，同时提交事务ID。

case ApiKeys.TXN\_OFFSET\_COMMIT => handleTxnOffsetCommitRequest(request)

// 获取 ACL 列表。

case ApiKeys.DESCRIBE\_ACLS => handleDescribeAcls(request)

// 创建一个新的 ACL。

case ApiKeys.CREATE\_ACLS => handleCreateAcls(request)

// 删除指定的ACL。

case ApiKeys.DELETE\_ACLS => handleDeleteAcls(request)

// 更改指定配置项。

case ApiKeys.ALTER\_CONFIGS => handleAlterConfigsRequest(request)

// 获取指定配置项的描述信息。

case ApiKeys.DESCRIBE\_CONFIGS => handleDescribeConfigsRequest(request)

// 更改指定分区的副本日志目录。

case ApiKeys.ALTER\_REPLICA\_LOG\_DIRS => handleAlterReplicaLogDirsRequest(request)

// 获取 Broker 的日志目录。

case ApiKeys.DESCRIBE\_LOG\_DIRS => handleDescribeLogDirsRequest(request)

// SASL 认证。这个 API 用于服务器验证客户端。

case ApiKeys.SASL\_AUTHENTICATE => handleSaslAuthenticateRequest(request)

// 为指定 Topic 创建新分区。

case ApiKeys.CREATE\_PARTITIONS => handleCreatePartitionsRequest(request)

// 创建代理令牌。

case ApiKeys.CREATE\_DELEGATION\_TOKEN => handleCreateTokenRequest(request)

// 续订代理令牌。

case ApiKeys.RENEW\_DELEGATION\_TOKEN => handleRenewTokenRequest(request)

// 代理令牌失效。

case ApiKeys.EXPIRE\_DELEGATION\_TOKEN => handleExpireTokenRequest(request)

// 获取代理令牌的描述信息。

case ApiKeys.DESCRIBE\_DELEGATION\_TOKEN => handleDescribeTokensRequest(request)

// 删除消费者组。

case ApiKeys.DELETE\_GROUPS => handleDeleteGroupsRequest(request)

// 强制选举分区的领袖。

case ApiKeys.ELECT\_LEADERS => handleElectReplicaLeader(request)

// 增量更改指定配置项。

case ApiKeys.INCREMENTAL\_ALTER\_CONFIGS => handleIncrementalAlterConfigsRequest(request)

// 在重分配之前更改分区分配。

case ApiKeys.ALTER\_PARTITION\_REASSIGNMENTS => handleAlterPartitionReassignmentsRequest(request)

// 获取所有正在进行的分区分配。

case ApiKeys.LIST\_PARTITION\_REASSIGNMENTS => handleListPartitionReassignmentsRequest(request)

// 删除指定主题和分区的所有偏移量提交。

case ApiKeys.OFFSET\_DELETE => handleOffsetDeleteRequest(request)

// 获取客户端配额信息。

case ApiKeys.DESCRIBE\_CLIENT\_QUOTAS => handleDescribeClientQuotasRequest(request)

// 更改客户端配额。

case ApiKeys.ALTER\_CLIENT\_QUOTAS => handleAlterClientQuotasRequest(request)

// 获取 SCRAM 凭据描述信息。

case ApiKeys.DESCRIBE\_USER\_SCRAM\_CREDENTIALS => handleDescribeUserScramCredentialsRequest(request)

// 更改指定用户的 SCRAM 凭据。

case ApiKeys.ALTER\_USER\_SCRAM\_CREDENTIALS => handleAlterUserScramCredentialsRequest(request)

// 更改分区的 ISR 集。

case ApiKeys.ALTER\_ISR => handleAlterIsrRequest(request)

// 更新 Broker 支持的特性。

case ApiKeys.UPDATE\_FEATURES => handleUpdateFeatures(request)

// Until we are ready to integrate the Raft layer, these APIs are treated as

// unexpected and we just close the connection.

// 下面 4 个是 KRaft 模块的逻辑，但是这里是要关闭与客户端连接

case ApiKeys.VOTE => closeConnection(request, util.Collections.emptyMap())

case ApiKeys.BEGIN\_QUORUM\_EPOCH => closeConnection(request, util.Collections.emptyMap())

case ApiKeys.END\_QUORUM\_EPOCH => closeConnection(request, util.Collections.emptyMap())

case ApiKeys.DESCRIBE\_QUORUM => closeConnection(request, util.Collections.emptyMap())

}

} catch {

// 如果是严重错误，则抛出异常

case e: FatalExitError => throw e

// 普通异常的话，记录下错误日志

case e: Throwable => handleError(request, e)

} finally {

// 尝试完成延迟操作

replicaManager.tryCompleteActions()

// 记录一下请求本地完成时间，即 Broker 处理完该请求的时间

if (request.apiLocalCompleteTimeNanos < 0)

request.apiLocalCompleteTimeNanos = time.nanoseconds

}

}

该入口方法相对比较简单，它**主要利用 Scala 中的模式匹配语法，完整的列出了对所有请求类型的处理逻辑**。通过该方法你就可以轻松地串联出 Kafka 处理任何类型请求的源码路径，另外它也是有规律的，比如处理 PRODUCE 的请求就是 handleProduceRequest， 处理 FETCH 的请求就是 handleFetchRequest。

从这个方法的实现规律，我们可以得出，**每当你要新增新的 RPC 协议时，大致要做以下 3 件事情**：

1.  更新 ApiKeys 枚举，添加新的 apiKey 标识。
2.  更新 KafkaApis 的 handle 方法，添加新的 case 分支。
3.  最后在 KafkaApis 中添加对应的 handle\*\*\*Request 方法，实现对该 RPC 请求的处理逻辑。

接下来，我们来看看关于处理生产者消息的请求，对应的方法是 handleProduceRequest()。

##   
**2.3 handleProduceRequest()**

/\*\*

\* Handle a produce request

\*/

def handleProduceRequest(request: RequestChannel.Request): Unit = {

// 1、获取生产者发送的请求体信息 ProduceRequest

val produceRequest \= request.body\[ProduceRequest\]

val numBytesAppended \= request.header.toStruct.sizeOf + request.sizeOfBodyInBytes

// 2、如果请求中包含事务性记录，则需要进行授权操作。

if (produceRequest.hasTransactionalRecords) {

// 如果请求中的事务 ID 不为 null 且能够通过授权检查，则进行以下操作：

// 将可授权的消息进行分区，并构建响应信息用于返回给客户端。

// 将不可授权的主题添加到未授权响应列表中，并返回相应的响应给客户端。

val isAuthorizedTransactional \= produceRequest.transactionalId != null &&

authorize(request.context, WRITE, TRANSACTIONAL\_ID, produceRequest.transactionalId)

if (!isAuthorizedTransactional) {

sendErrorResponseMaybeThrottle(request, Errors.TRANSACTIONAL\_ID\_AUTHORIZATION\_FAILED.exception)

return

}

// Note that authorization to a transactionalId implies ProducerId authorization

// 如果请求中包含幂等记录且不能通过授权检查，则将响应添加到不合格请求响应列表中。

} else if (produceRequest.hasIdempotentRecords && !authorize(request.context, IDEMPOTENT\_WRITE, CLUSTER, CLUSTER\_NAME)) {

sendErrorResponseMaybeThrottle(request, Errors.CLUSTER\_AUTHORIZATION\_FAILED.exception)

return

}

....

// 3、遍历每个分区记录，对其进行授权和验证操作，构建相应的响应信息。

for ((topicPartition, memoryRecords) <- produceRecords) {

// 如果分区记录属于未授权主题，则将相应响应添加到未授权响应列表中。

if (!authorizedTopics.contains(topicPartition.topic))

unauthorizedTopicResponses += topicPartition -> new PartitionResponse(Errors.TOPIC\_AUTHORIZATION\_FAILED)

// 如果分区记录所属主题不存在，则将响应加入到不存在主题的响应列表中。

else if (!metadataCache.contains(topicPartition))

nonExistingTopicResponses += topicPartition -> new PartitionResponse(Errors.UNKNOWN\_TOPIC\_OR\_PARTITION)

else

// 如果分区记录通过了验证，则将其添加到已授权分区记录列表中，并将该分区记录发送给对应的 broker。

try {

ProduceRequest.validateRecords(request.header.apiVersion, memoryRecords)

authorizedRequestInfo += (topicPartition -> memoryRecords)

} catch {

case e: ApiException =>

invalidRequestResponses += topicPartition -> new PartitionResponse(Errors.forException(e))

}

}

该方法主要**负责处理生产者消息的请求**，主要做了以下 3 件事情：

1.  获取生产者发送的请求体信息 ProduceRequest 。
2.  如果请求中包含事务性记录，则需要进行授权操作。
3.  如果请求中的事务 ID 不为 null 且能够通过授权检查，则进行以下操作：
4.  将可授权的消息进行分区，并构建响应信息用于返回给客户端。
5.  将不可授权的主题添加到未授权响应列表中，并返回相应的响应给客户端。
6.  遍历每个分区记录，对其进行授权和验证操作，构建相应的响应信息。
7.  如果分区记录属于未授权主题，则将相应响应添加到未授权响应列表中。
8.  如果分区记录所属主题不存在，则将响应加入到不存在主题的响应列表中。
9.  如果分区记录通过了验证，则将其添加到已授权分区记录列表中，并将该分区记录发送给对应的 broker。

接下来，我们来看看发送响应请求的逻辑，**对于任何一类请求被处理之后都需要做的步骤，Kafka 需要把处理结果发送给请求发送方即客户端**，对应的方法是 sendResponse()。

## **2.4 sendResponse()**

private def sendResponse(request: RequestChannel.Request, // 请求(request)

responseOpt: Option\[AbstractResponse\], // 响应(Optional\[AbstractResponse\])

onComplete: Option\[Send => Unit\] // 请求完成操作(onComplete)

): Unit = {

// Update error metrics for each error code in the response including Errors.NONE

responseOpt.foreach(response => requestChannel.updateErrorMetrics(request.header.apiKey, response.errorCounts.asScala))

// 1、匹配响应类型

val response \= responseOpt match {

// 响应不为 None

case Some(response) =>

// 基于响应构建发送响应

val responseSend \= request.context.buildResponse(response)

val responseString \=

// 如果记录日志开启的话，则记录响应日志

if (RequestChannel.isRequestLoggingEnabled) Some(response.toString(request.context.apiVersion))

// 否则构建无操作响应。

else None

// 构建正常响应。

new RequestChannel.SendResponse(request, responseSend, responseString, onComplete)

case None \=\>

// 否则构建无操作响应。

new RequestChannel.NoOpResponse(request)

}

// 2、最后发送响应请求给 requestChannel,交由 Processor 线程来返回给客户端

requestChannel.sendResponse(response)

}

该方法相对比较简单，主要**负责处理响应请求并发送给客户端**，主要做了以下 2 件事情：

1.  先匹配响应类型，并实例化相应的响应对象。
2.  如果响应不为 None，构建响应。
3.  1）、基于响应构建发送响应。
4.  2）、如果记录日志开启的话，则记录响应日志；否则构建无操作响应。
5.  3）、实例化响应对象。
6.  如果响应为 None，构建误操作响应。
7.  最后发送响应请求给 requestChannel,交由 Processor 线程来返回给客户端。

通过这个方法的学习，我们可以了解到，**KafkaApis 实际上是把处理完成的 Response 添加到 Processor 线程的 ResponseQueue 队列中，所以真正将 Response 响应返回给 Clients 或者其他 Broker，其实是 Processor 线程**。

另外该类中还有其他相关 SendResponse\*\*\* 方法，这几个方法都有一些定制化的逻辑，属于特定情况下的处理。

1.  sendNoOpResponseExemptThrottle：发送 NoOpResponse 类型的 Response 而不受请求通道上限流（throttling）的限制。这里所谓的 NoOpResponse 就是 Processor 线程取出该类型的 Response 后，不执行真正的 I/O 发送操作。
2.  sendErrorResponseExemptThrottle：发送普通 Response 而不受限流限制。
3.  sendErrorResponseMaybeThrottle：发送携带错误信息的 Response 但接受限流的约束。
4.  sendResponseMaybeThrottle：发送普通 Response 但接受限流的约束。

至此，「**KafkaApis**」类的方法就带你剖析完了，至于里面的其他方法，这里就先暂时不分析了，后面根据对于知识点再进行讲解。

##   
**04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过剖析 Kafka 网络通信架构设计，最后真正处理请求的逻辑是 apis.handle() 方法，引出 KafkaApis 类。

2、带你深度剖析了 「**KafkaApis**」类的定义、总入口方法的各类请求的处理方法、讲解了 2 个方法，处理客户端请求以及返回响应请求。

下篇我们来深度剖析「**网络层请求处理全流程总结**」，大家期待，我们下期见。