大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 Topic 创建请求处理流程**」，了解了 Topic 是如何被创建出来的，从今天开始，我们来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 Topic 删除请求处理流程**」。

![](https://article-images.zsxq.com/FtEraIwGaL8muRM5B7CN0m3PV5QK)

## **01 总体概述**

在上篇中我们通过「**命令行**」的方式作为打开 Topic 创建的入口，这里同样以这种方式来剖析。

## **1.1 删除 Topic 命令**

大家都知道，删除 Topic 可以执行以下命令：

\# kafka版本 >= 2.2 支持下面方式（推荐）

bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic message

那么今天就来看看通过这个脚本 [kafka-topics.sh](http://kafka-topics.sh/) 内部实现流程。

## **02 kafka-topics.sh**

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

// 调用 $base\_dir/kafka-run-class.sh 脚本并传递相应的参数。其中 "@ 代表传递的为命令行参数。具体执行的封装在 Kafka Core 包中的 TopicCommand 类。

exec $(dirname $0)/kafka-run-class.sh kafka.admin.TopicCommand "$@"

该 shell 脚本就一步，它底层执行的是**封装在 Kafka Core 包中的 TopicCommand 类**。接下来我们来看下该类都做了什么。

## **03 删除 Topic 源码流程**

在 [【服务端 Broker 源码分析系列第二十二篇】图解 Kafka 源码之 Topic 创建请求处理流程](https://articles.zsxq.com/id_2q1nwfkg3fcb.html) 这篇中我们已经深度剖析过了整个请求流程，所以这里就不再详细的分析请求的过程了，直接看重点。

最终当 **KafkaAdminClient** 实例对象创建完成后，会继续传参到 **AdminClientTopicService** 类中，这样就完成了实例化 **topicService** ，接着判断命令行执行删除Topic 操作，源码如下：

// 如果命令行输入 --delete 后会执行该方法

else if (opts.hasDeleteOption)

topicService.deleteTopic(opts)

它会执行 **AdminClientTopicService** 类的伴生类的 **deleteTopic** 方法。

##   
**3.1 deleteTopic()**

case class AdminClientTopicService private (adminClient: Admin) extends TopicService {

// 删除 Topic

override def deleteTopic(opts: TopicCommandOptions): Unit = {

// 1、先获取 Topics

val topics \= getTopics(opts.topic, opts.excludeInternalTopics)

// 2、确认 Topic 是否存在，不存在无法删除

ensureTopicExists(topics, opts.topic, !opts.ifExists)

// 3、调用 adminClient 来删除 Topic

adminClient.deleteTopics(topics.asJavaCollection, new DeleteTopicsOptions().retryOnQuotaViolation(false))

.all().get()

}

// 确保 Topic 是否存在，否则抛异常

private def ensureTopicExists(foundTopics: Seq\[String\], requestedTopic: Option\[String\], requireTopicExists: Boolean): Unit = {

// If no topic name was mentioned, do not need to throw exception.

if (requestedTopic.isDefined && requireTopicExists && foundTopics.isEmpty) {

// If given topic doesn't exist then throw exception

throw new IllegalArgumentException(s"Topic '${requestedTopic.get}' does not exist as expected")

}

}

// 获取 Topics

override def getTopics(topicIncludelist: Option\[String\], excludeInternalTopics: Boolean = false): Seq\[String\] = {

val allTopics \= if (excludeInternalTopics) {

// 获取所有主题

adminClient.listTopics()

} else {

// 获取所有内部主题

adminClient.listTopics(new ListTopicsOptions().listInternal(true))

}

// 根据给定的条件来过滤主题列表，返回符合条件的主题列表。如果有主题包含列表，则只返回允许的主题；如果没有主题包含列表，则根据是否排除内部主题的条件来过滤主题

doGetTopics(allTopics.names().get().asScala.toSeq.sorted, topicIncludelist, excludeInternalTopics)

}

private def doGetTopics(allTopics: Seq\[String\], topicIncludeList: Option\[String\], excludeInternalTopics: Boolean): Seq\[String\] = {

// 如果主题包含列表不为空

if (topicIncludeList.isDefined) {

// 创建一个 IncludeList 实例，使用主题包含列表作为构造函数的参数

val topicsFilter \= IncludeList(topicIncludeList.get)

// 根据过滤条件来过滤所有主题，只返回允许的主题列表

allTopics.filter(topicsFilter.isTopicAllowed(\_, excludeInternalTopics))

} else

// 如果主题包含列表为空，根据是否排除内部主题的条件来过滤所有主题，只返回非内部主题的列表

allTopics.filterNot(Topic.isInternal(\_) && excludeInternalTopics)

}

}

删除 Topic 相对简单些，步骤如下：

1.  先获取 Topics 。
2.  确认 Topic 是否存在，不存在无法删除。
3.  调用 adminClient 来删除 Topic。

那么它到底是如何删除 Topic 的呢？我们接着来剖析。

## **3.2 KafkaAdminClient#deleteTopics()**

@Override

public DeleteTopicsResult deleteTopics(final Collection<String> topicNames,

final DeleteTopicsOptions options) {

// 1、创建一个 Map 来存储主题和对应的KafkaFutureImpl<Void>对象，初始容量为主题名列表的大小

final Map<String, KafkaFutureImpl<Void>> topicFutures = new HashMap<>(topicNames.size());

// 2、创建一个 List 来存储有效的主题名，初始容量为主题名列表的大小

final List<String> validTopicNames = new ArrayList<>(topicNames.size());

// 3、遍历主题名列表中的每个主题名

for (String topicName : topicNames) {

// 检查 topic 名称是否无法表示

if (topicNameIsUnrepresentable(topicName)) {

KafkaFutureImpl<Void> future = new KafkaFutureImpl<>();

future.completeExceptionally(new InvalidTopicException("The given topic name '" +

topicName + "' cannot be represented in a request."));

// 将主题名和对应的异常完成的KafkaFutureImpl对象放入topicFutures中

topicFutures.put(topicName, future);

// 如果 topicFutures 中不包含该主题名

} else if (!topicFutures.containsKey(topicName)) {

// 将该主题名和一个新的KafkaFutureImpl对象放入topicFutures中

topicFutures.put(topicName, new KafkaFutureImpl<>());

// 将该主题名添加到有效的主题名列表中

validTopicNames.add(topicName);

}

}

// 4、如果有效的主题名列表不为空

if (!validTopicNames.isEmpty()) {

// 获取当前的时间戳

final long now \= time.milliseconds();

// 计算截止时间，根据当前时间和指定的超时时间

final long deadline \= calcDeadlineMs(now, options.timeoutMs());

// 5、构造一个调用对象，用于执行删除主题的操作

final Call call \= getDeleteTopicsCall(options, topicFutures, validTopicNames,

Collections.emptyMap(), now, deadline);

// 调用 runnable 对象的 call 方法来执行 Call 对象,并添加到 runnable 线程的 newCalls 队列中。

runnable.call(call, now);

}

// 6、返回一个 DeleteTopicsResult 对象，其中包含 topicFutures 的副本

return new DeleteTopicsResult(new HashMap<>(topicFutures));

}

步骤如下：

1.  创建一个 Map 来存储主题和对应的KafkaFutureImpl<Void>对象，初始容量为主题名列表的大小。
2.  创建一个 List 来存储有效的主题名，初始容量为主题名列表的大小。
3.  遍历主题名列表中的每个主题名，做不同的处理。
4.  如果有效的主题名列表不为空。
5.  构造一个调用对象，用于执行删除主题的操作，调用 runnable 对象的 call 方法来执行 Call 对象,并添加到 runnable 线程的 newCalls 队列中。
6.  返回一个 DeleteTopicsResult 对象，其中包含 topicFutures 的副本。

这里重点来剖析下「**第五步**」删除 topic 的核心方法 getDeleteTopicsCall。

##   
**3.3 getDeleteTopicsCall**

private Call getDeleteTopicsCall(final DeleteTopicsOptions options,

final Map<String, KafkaFutureImpl<Void>> futures,

final List<String> topics,

final Map<String, ThrottlingQuotaExceededException> quotaExceededExceptions,

final long now,

final long deadline) {

return new Call("deleteTopics", deadline, new ControllerNodeProvider()) {

@Override

DeleteTopicsRequest.Builder createRequest(int timeoutMs) {

return new DeleteTopicsRequest.Builder(

new DeleteTopicsRequestData()

.setTopicNames(topics)

.setTimeoutMs(timeoutMs));

}

@Override

void handleResponse(AbstractResponse abstractResponse) {

....

}

@Override

void handleFailure(Throwable throwable) {

....

}

};

}

这段源码我们主要看下 Call 回调函数里面的方法， 这里**先不管 Kafka 客户端如何跟服务端进行通信的细节，我们主要关注删除 Topic 的相关逻辑**。

Call 回调函数中的 createRequest 方法主要用来**创建请求然后根据构建者模式来构建 DeleteTopicsRequest 请求参数**，最终会选择 **ControllerNodeProvider** 这个节点发起网络请求：

  
![](https://article-images.zsxq.com/FsZq6zPxP_zY0U-7FjpL0qZWV7Pf)

所以删除 Topic 操作**也是需要 Controller 来执行的**：

/\*\*

\* Provides the controller node.

\* kafkaAdminClient.java

\*/

private class ControllerNodeProvider implements NodeProvider {

@Override

// 为 Kafka 提供一个可用的 Controller 节点，以确保消息的正常发送和接收

public Node provide() {

// 首先判断 metadataManager 对象是否已准备就绪并且是否已确定 Controller 节点

if (metadataManager.isReady() && (metadataManager.controller() != null)) {

// 返回该控制器节点

return metadataManager.controller();

}

// 否则，发起请求更新

metadataManager.requestUpdate();

// 返回空

return null;

}

}

## **3.4 发起网络请求**

这里可以从下面文章进行学习：

[【生产者源码分析第十一篇】图解 Kafka 源码之生产者流程总结篇](https://articles.zsxq.com/id_84na8x6zekwr.html)

[【原理分析系列第八篇】图解 Kafka 超高并发网络架构演进过程](https://articles.zsxq.com/id_de63boucthq8.html)

[【服务端Broker源码分析系列第七篇】图解Kafka源码之网络层请求处理全流程总结](https://articles.zsxq.com/id_xtza6mo3tkfb.html)

## **3.5 Controller 服务端接收客户端请求**

首先找到服务端接收客户端请求的源码入口： [kafka.server.KafkaRequestHandler#run](http://kafka.server.kafkarequesthandler/#run)。

「**kafkaRequestHandler.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/KafkaRequestHandler.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/KafkaRequestHandler.scala)

主要看下 [apis.handle(request)](http://apis.handle\(request\)/) 方法，可以看到客户端的请求都在 [request.bodyAndSize()](http://request.bodyandsize\(\)%20/) 里面：

/\*\*

\* A thread that answers kafka requests.

\*/

class KafkaRequestHandler(id: Int,

brokerId: Int,

val aggregateIdleMeter: Meter,

val totalHandlerThreads: AtomicInteger,

val requestChannel: RequestChannel,

apis: ApiRequestHandler,

time: Time) extends Runnable with Logging {

this.logIdent = "\[Kafka Request Handler " + id + " on Broker " + brokerId + "\], "

private val shutdownComplete \= new CountDownLatch(1)

@volatile private var stopped \= false

def run(): Unit = {

while (!stopped) {

....

val req \= requestChannel.receiveRequest(300)

....

req match {

....

case request: RequestChannel.Request =>

try {

request.requestDequeueTimeNanos = endTime

trace(s"Kafka request handler $id on broker $brokerId handling request $request")

// 这里是重点

apis.handle(request)

} catch {

....

} finally {

....

}

case null \=\> // continue

}

}

shutdownComplete.countDown()

}

....

}

## **3.5.1 KafkaApis.handle(request)**

根据请求传递 API 来调用不同接口，[request.header.apiKey](http://request.header.apikey/) 匹配客户端传来的 DeleteTopics。

![](https://article-images.zsxq.com/Fvktg5hJ_QS2q8tE9wJ_M2075X3h)

## **3.5.2 handleDeleteTopicsRequest()**

服务端处理删除 Topic 的请求，源码如下：

def handleDeleteTopicsRequest(request: RequestChannel.Request): Unit = {

// 检查是否需要 ZooKeeper 的支持，如果不需要则抛出异常

val zkSupport \= metadataSupport.requireZkOrThrow(KafkaApis.shouldAlwaysForward(request))

.

....

// 获取 DeleteTopicsRequest

val deleteTopicRequest \= request.body\[DeleteTopicsRequest\]

// 创建结果集

val results \= new DeletableTopicResultCollection(deleteTopicRequest.data.topicNames.size)

// 创建用于存放待删除主题的集合

val toDelete \= mutable.Set\[String\]()

// 1、如果当前 Broker 不是 Controller，则返回错误

if (!zkSupport.controller.isActive) {

deleteTopicRequest.topics().forEach { topic =>

results.add(new DeletableTopicResult()

.setName(topic.name())

.setTopicId(topic.topicId())

.setErrorCode(Errors.NOT\_CONTROLLER.code))

}

sendResponseCallback(results)

}

// 2、如果禁用了 deleteTopic，则返回相关错误

else if (!config.deleteTopicEnable) {

val error \= if (request.context.apiVersion < 3) Errors.INVALID\_REQUEST else Errors.TOPIC\_DELETION\_DISABLED

deleteTopicRequest.topics().forEach { topic =>

results.add(new DeletableTopicResult()

.setName(topic.name())

.setTopicId(topic.topicId())

.setErrorCode(error.code))

}

sendResponseCallback(results)

}

// 3、处理删除主题请求

else {

// 获取请求中的主题 ID 集合

val topicIdsFromRequest \= deleteTopicRequest.topicIds().asScala.filter(topicId => topicId != Uuid.ZERO\_UUID).toSet

deleteTopicRequest.topics().forEach { topic =>

// 检查主题名称和主题 ID 是否同时存在，否则抛出异常

if (topic.name() != null && topic.topicId() != Uuid.ZERO\_UUID)

throw new InvalidRequestException("Topic name and topic ID can not both be specified.")

// 根据主题 ID 获取主题名称

val name \= if (topic.topicId() == Uuid.ZERO\_UUID) topic.name()

else zkSupport.controller.controllerContext.topicName(topic.topicId).orNull

results.add(new DeletableTopicResult()

.setName(name)

.setTopicId(topic.topicId()))

}

// 根据权限过滤需要描述和删除的主题

val authorizedDescribeTopics \= authHelper.filterByAuthorized(request.context, DESCRIBE, TOPIC,

results.asScala.filter(result => result.name() != null))(\_.name)

val authorizedDeleteTopics \= authHelper.filterByAuthorized(request.context, DELETE, TOPIC,

results.asScala.filter(result => result.name() != null))(\_.name)

results.forEach { topic =>

val unresolvedTopicId \= topic.topicId() != Uuid.ZERO\_UUID && topic.name() == null

// 如果主题 ID 在请求中，但服务器不支持主题 ID，则返回错误

if (!config.usesTopicId && topicIdsFromRequest.contains(topic.topicId)) {

topic.setErrorCode(Errors.UNSUPPORTED\_VERSION.code)

topic.setErrorMessage("Topic IDs are not supported on the server.")

}

// 如果主题 ID 无法解析，则返回错误

else if (unresolvedTopicId) {

topic.setErrorCode(Errors.UNKNOWN\_TOPIC\_ID.code)

}

// 如果没有描述主题的权限，则返回错误

else if (topicIdsFromRequest.contains(topic.topicId) && !authorizedDescribeTopics.contains(topic.name)) {

// 由于客户端没有 Describe 权限，因此不应在响应中返回名称。然而，我们不认为主题 ID 本身是敏感的，因此没有必要用“UNKNOWN\_TOPIC\_ID”隐藏此情况。

topic.setName(null)

topic.setErrorCode(Errors.TOPIC\_AUTHORIZATION\_FAILED.code)

}

// 如果没有删除主题的权限，则返回错误

else if (!authorizedDeleteTopics.contains(topic.name)) {

topic.setErrorCode(Errors.TOPIC\_AUTHORIZATION\_FAILED.code)

}

// 如果主题不存在，则返回错误

else if (!metadataCache.contains(topic.name)) {

topic.setErrorCode(Errors.UNKNOWN\_TOPIC\_OR\_PARTITION.code)

}

// 添加到待删除集合中

else {

toDelete += topic.name

}

}

// 如果没有待删除的主题，则直接返回结果

if (toDelete.isEmpty)

sendResponseCallback(results)

else {

// 执行删除主题操作

def handleDeleteTopicsResults(errors: Map\[String, Errors\]): Unit = {

// 处理删除主题的结果

errors.foreach {

case (topicName, error) =>

results.find(topicName)

.setErrorCode(error.code)

}

// 返回结果

sendResponseCallback(results)

}

// 调用 adminManager 删除 topics

zkSupport.adminManager.deleteTopics(

deleteTopicRequest.data.timeoutMs,

toDelete,

controllerMutationQuota,

handleDeleteTopicsResults

)

}

}

}

步骤如下：

1.  判断当前 Broker 是否属于 Controller，CreateTopic 操作必须由 Controller 来进行，因为有可能客户端发起请求的时候 Controller 已经变更，所以如果不是的话则抛出异常。
2.  如果禁用了 deleteTopic，则返回相关错误。
3.  否则处理删除主题请求。
4.  获取请求中的主题 ID 集合。
5.  根据权限过滤需要描述和删除的主题。
6.  kafka 相关鉴权，这里忽略，后面抽空分析 kafka 鉴权机制。
7.  处理各种异常情况。
8.  最后调用 zkSupport.adminManager.deleteTopics()。

这里我们主要来看下 「**第三步**」的最后一步。

## **3.5.3 zkSupport.adminManager.deleteTopics()**

「**ZkAdminManager.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/ZkAdminManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/ZkAdminManager.scala)

/\*\*

\* Delete topics and wait until the topics have been completely deleted.

\* The callback function will be triggered either when timeout, error or the topics are deleted.

\*/

def deleteTopics(timeout: Int, // 创建主题的超时时间，单位为毫秒。

topics: Set\[String\], // 要删除 Topic 集合

controllerMutationQuota: ControllerMutationQuota,// 控制器变更配额，在创建主题时用于记录控制器变更的数量。

responseCallback: Map\[String, Errors\] => Unit): Unit = { // 回调函数，用于在删除完成后接收结果。

// 1. map over topics calling the asynchronous delete

// 遍历传入的要删除的主题，并删除相应主题

val metadata \= topics.map { topic =>

try {

controllerMutationQuota.record(metadataCache.numPartitions(topic).getOrElse(0).toDouble)

// 往 zk 中写入数据 标记要被删除的 topic /admin/delete\_topics/{TopicName}

adminZkClient.deleteTopic(topic)

// 删除 Topic 元数据

DeleteTopicMetadata(topic, Errors.NONE)

} catch {

case \_: TopicAlreadyMarkedForDeletionException =>

// swallow the exception, and still track deletion allowing multiple calls to wait for deletion

DeleteTopicMetadata(topic, Errors.NONE)

case e: ThrottlingQuotaExceededException =>

debug(s"Topic deletion not allowed because quota is violated. Delay time: ${e.throttleTimeMs}")

DeleteTopicMetadata(topic, e)

case e: Throwable =>

error(s"Error processing delete topic request for topic $topic", e)

DeleteTopicMetadata(topic, e)

}

}

// 2. if timeout <= 0 or no topics can proceed return immediately

// 2、如果客户端传过来的 timeout<=0 或者 没有主题可以删除 则直接返回异常

if (timeout <= 0 || !metadata.exists(\_.error == Errors.NONE)) {

val results \= metadata.map { deleteTopicMetadata =>

// ignore topics that already have errors

// 忽略已经有错误的主题

if (deleteTopicMetadata.error == Errors.NONE) {

(deleteTopicMetadata.topic, Errors.REQUEST\_TIMED\_OUT)

} else {

(deleteTopicMetadata.topic, deleteTopicMetadata.error)

}

}.toMap

responseCallback(results)

} else {

// 3. else pass the topics and errors to the delayed operation and set the keys

// 如果 timeout > 0 并且存在需要删除的主题，则根据超时时间创建延迟任务并将其添加到延迟主题里

val delayedDelete \= new DelayedDeleteTopics(timeout, metadata.toSeq, this, responseCallback)

val delayedDeleteKeys \= topics.map(TopicKey).toSeq

// try to complete the request immediately, otherwise put it into the purgatory

topicPurgatory.tryCompleteElseWatch(delayedDelete, delayedDeleteKeys)

}

}

步骤如下：

1.  遍历传入的要删除的主题，并删除相应主题。
2.  尝试往 zk 中写入数据 标记要被删除的 topic /admin/delete\_topics/{TopicName}，删除 Topic 元数据，否则抛异常。
3.  如果客户端传过来的 timeout<=0 或者 没有主题可以删除 则直接返回异常。
4.  如果 timeout > 0 并且存在需要删除的主题，则根据超时时间创建延迟任务并将其添加到延迟主题里。

## **3.5.4 写入 ZK 节点数据**

接着我们来看下 [adminZkClient.delete(](http://%20adminzkclient.createtopicwithassignment\(\)%20/)[)](http://%20adminzkclient.createtopicwithassignment\(\)%20/) 这个方法。

def deleteTopic(topic: String): Unit = {

if (zkClient.topicExists(topic)) {

try {

// 如果主题存在，在 zookeeper 上创建删除主题的路径 /admin/delete\_topics/{TopicName}

zkClient.createDeleteTopicPath(topic)

} catch {

// 如果节点已存在，则抛出主题已标记删除异常

case \_: NodeExistsException => throw new TopicAlreadyMarkedForDeletionException(

"topic %s is already marked for deletion".format(topic))

// 如果发生其他错误，则抛出操作异常

case e: Throwable => throw new AdminOperationException(e.getMessage)

}

} else {

// 如果主题不存在，则抛出主题不存在或分区异常

throw new UnknownTopicOrPartitionException(s"Topic \`$topic\` to delete does not exist")

}

}

这里很简单，如果存在直接在 在 zookeeper 上创建删除主题的路径 /admin/delete\_topics/{TopicName}，供 「**Controller**」进行监听。

## **3.5.5 Controller 监听节点变更，执行删除 Topic 流程**

我们在前面讲「**Controller**」时候讲过它会监听 Zookeeper 的一些节点，在上面的流程中已经将节点信息写入到 Zookeeper 中， 当 「**Controller**」监听 [/admin/delete\_topics/{TopicName}](http://admin/delete_topics/%7BTopicName%7D) 节点有变化时就会通知 Controller 做出相应的处理，最终会调用 [KafkaContoller.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala) 的 processTopicDeletion 方法，源码如下：

private def processTopicDeletion(): Unit = {

// 1、如果非 Contorller，直接返回

if (!isActive) return

// 2、获取要删除的主题集合

var topicsToBeDeleted \= zkClient.getTopicDeletions.toSet

debug(s"Delete topics listener fired for topics ${topicsToBeDeleted.mkString(",")} to be deleted")

// 3、如果 /admin/delete\_topics/ 下面的节点有不存在的Topic，则清理掉。

val nonExistentTopics \= topicsToBeDeleted -- controllerContext.allTopics

// 如果存在非存在的主题

if (nonExistentTopics.nonEmpty) {

warn(s"Ignoring request to delete non-existing topics ${nonExistentTopics.mkString(",")}")

// 从zookeeper中删除非存在的主题

zkClient.deleteTopicDeletions(nonExistentTopics.toSeq, controllerContext.epochZkVersion)

}

// 4、 从要删除的主题集合中移除不存在的主题

topicsToBeDeleted --= nonExistentTopics

// 5、 如果启用了删除主题功能

if (config.deleteTopicEnable) {

// 如果还存在要删除的主题

if (topicsToBeDeleted.nonEmpty) {

info(s"Starting topic deletion for topics ${topicsToBeDeleted.mkString(",")}")

// mark topic ineligible for deletion if other state changes are in progress

// 如果其他状态更改正在进行中，则标记主题不可删除

topicsToBeDeleted.foreach { topic =>

val partitionReassignmentInProgress \=

controllerContext.partitionsBeingReassigned.map(\_.topic).contains(topic)

if (partitionReassignmentInProgress)

topicDeletionManager.markTopicIneligibleForDeletion(Set(topic),

reason = "topic reassignment in progress")

}

// add topic to deletion list

// 将主题添加到删除列表中，重启主题删除操作

topicDeletionManager.enqueueTopicsForDeletion(topicsToBeDeleted)

}

} else {

// If delete topic is disabled remove entries under zookeeper path : /admin/delete\_topics

info(s"Removing $topicsToBeDeleted since delete topic is disabled")

// 6、如果禁用了删除主题功能，则删除zookeeper路径下的条目：/admin/delete\_topics

zkClient.deleteTopicDeletions(topicsToBeDeleted.toSeq, controllerContext.epochZkVersion)

}

}

操作步骤如下：

1.  如果非 Contorller，直接返回。
2.  从 /admin/delete\_topics/ 节点下获取要删除的主题集合。
3.  如果 /admin/delete\_topics/ 下面的节点有不存在的Topic，则清理掉。
4.  从要删除的主题集合中移除不存在的主题。
5.  如果启用了删除主题功能，将主题标记为不符合删除条件,放到topicsIneligibleForDeletion中; 不符合删除条件的是: **Topic 分区正在进行分区重分配**。
6.  将主题添加到删除列表 topicsToBeDeleted 中，重启主题删除操作。
7.  如果禁用了删除主题功能，则删除 zookeeper 路径下的条目：[/admin/delete\_topics](http://admin/delete_topics) 下面的节点全部删除,然后流程结束。

这里我们来看下 「**第六步**」，这里先来看下 topicDeletionManager 删除管理器。

## **3.5.6 topicDeletionManager**

Kafka Topic 删除这部分的逻辑是一个单独线程去做的，如下。

「**TopicDeletionManager.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/controller/TopicDeletionManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/controller/TopicDeletionManager.scala)

它是负责对指定 Kafka 主题执行删除操作，清除待删除主题在集群上的信息。

它定义了 3 个类结构以及20多个方法，这里先通过一张 UML 图来入门下：

![](https://article-images.zsxq.com/Fum_l0LxcIzjU6sPixjOygoE-6pG)

总共包括3个部分：

1.  DeletionClient接口：负责实现删除主题以及后续的动作，比如更新元数据等。这个接口里定义了4个方法，分别是deleteTopic、deleteTopicDeletions、mutePartitionModifications和sendMetadataUpdate。我们后面再详细学习它们的代码。
2.  ControllerDeletionClient类：实现DeletionClient接口的类，分别实现了刚刚说到的那4个方法。
3.  TopicDeletionManager类：主题删除管理器类，定义了若干个方法维护主题删除前后集群状态的正确性。比如，什么时候才能删除主题、什么时候主题不能被删除、主题删除过程中要规避哪些操作等等。

这里重点看下 TopicDeleteManager 类。

class TopicDeletionManager(

// KafkaConfig类，保存Broker端参数

config: KafkaConfig,

// 集群元数据

controllerContext: ControllerContext,

// 副本状态机，用于设置副本状态

replicaStateMachine: ReplicaStateMachine,

// 分区状态机，用于设置分区状态

partitionStateMachine: PartitionStateMachine,

// DeletionClient接口，实现主题删除

client: DeletionClient) extends Logging {

this.logIdent = s"\[Topic Deletion Manager ${config.brokerId}\] "

// 是否允许删除主题

val isDeleteTopicEnabled: Boolean = config.deleteTopicEnable

......

}

该类主要的属性有6个：

1.  config：KafkaConfig实例，可以用作获取 Broker 端参数[delete.topic.enable](http://delete.topic.enable/)的值。该参数用于控制是否允许删除主题，默认值是 true，即 Kafka 默认允许用户删除主题。
2.  controllerContext：Controller 端保存的元数据信息。删除主题必然要变更集群元数据信息，因此 TopicDeletionManager需要用到 controllerContext 的方法，去更新它保存的数据。
3.  replicaStateMachine和partitionStateMachine：副本状态机和分区状态机。它们各自负责副本和分区的状态转换，以保持副本对象和分区对象在集群上的一致性状态。后面会单独剖析。
4.  client：DeletionClient 接口。TopicDeletionManager 通过该接口执行 ZooKeeper 上节点的相应更新。
5.  isDeleteTopicEnabled：表明主题是否允许被删除。它是Broker端参数 [delete.topic.enable](http://delete.topic.enable/) 的值，默认是true，表示 Kafka允许删除主题。源码中大量使用这个字段判断主题的可删除性。前面的config参数的主要目的就是设置这个字段的值。被设定之后，config 就不再被源码使用了。

我们再看看 TopicDeletionManager 类实例**是如何被创建的呢？**实际上，它是在 KafkaController 类初始化时就被创建的。

在KafkaController类的源码中，你可以很容易地找到这行代码：

val topicDeletionManager \= new TopicDeletionManager(config, controllerContext, replicaStateMachine,partitionStateMachine, new ControllerDeletionClient(this, zkClient))

可以看到，它实例化了一个全新的 [ControllerDeletionClient](http://controllerdeletionclient%20/) 对象，然后利用这个对象实例和[replicaStateMachine](http://replicastatemachine/)、[partitionStateMachine](http://partitionstatemachine/)，三者一起来创建 [TopicDeletionManager](http://topicdeletionmanager%20/) 实例。

这里画一张流程图，方便理解：

![](https://article-images.zsxq.com/Fo1MexYm7axh9vV3D4ttRYQcRX-2)

我们回头接着剖析上面的方法。

## **3.5.6 将主题添加到删除列表中**

它调用的是 [topicDeletionManager#enqueueTopicsForDeletion](http://topicdeletionmanager/#enqueueTopicsForDeletion) 方法，源码如下：

def enqueueTopicsForDeletion(topics: Set\[String\]): Unit = {

// 如果允许删除主题

if (isDeleteTopicEnabled) {

// 将主题添加到控制器元数据的删除队列中

controllerContext.queueTopicDeletion(topics)

// 重启主题删除操作

resumeDeletions()

}

}

此方法就两步：

1.  将主题添加到控制器元数据的删除队列中。
2.  重启主题删除操作。

接着我们来看下 「**第二步**」。

## **3.5.7 重启主题删除操作**

当主题因为某些事件可能一时无法完成删除，比如**第主体分区正在进行副本重分配**等。一旦这些事件完成后，主题重新具备可删除的资格。此时就会**重启主题删除操作**。

private def resumeDeletions(): Unit = {

// 从元数据缓存中获取要删除的主题列表

val topicsQueuedForDeletion \= Set.empty\[String\] ++ controllerContext.topicsToBeDeleted

// 待重试主题列表

val topicsEligibleForRetry \= mutable.Set.empty\[String\]

// 待删除主题列表

val topicsEligibleForDeletion \= mutable.Set.empty\[String\]

if (topicsQueuedForDeletion.nonEmpty)

info(s"Handling deletion for topics ${topicsQueuedForDeletion.mkString(",")}")

// 遍历每个待删除主题

topicsQueuedForDeletion.foreach { topic =>

// if all replicas are marked as deleted successfully, then topic deletion is done

// 如果该主题所有副本已经是 ReplicaDeletionSuccessful 状态，即该主题已经被删除

if (controllerContext.areAllReplicasInState(topic, ReplicaDeletionSuccessful)) {

// clear up all state for this topic from controller cache and zookeeper

// 调用 completeDeleteTopic 清除元数据和ZK状态

completeDeleteTopic(topic)

info(s"Deletion of topic $topic successfully completed")

} else if (!controllerContext.isAnyReplicaInState(topic, ReplicaDeletionStarted)) {

// 如果判断主题当前无法被删除且未发起删除操作

if (controllerContext.isAnyReplicaInState(topic, ReplicaDeletionIneligible)) {

// 把该主题加到待重试主题列表中用于后续重试

topicsEligibleForRetry += topic

}

}

// // 如果该主题能够被删除

if (isTopicEligibleForDeletion(topic)) {

info(s"Deletion of topic $topic (re)started")

// 把该主题加到待删除主题列表中用于后续删除

topicsEligibleForDeletion += topic

}

}

// 重试待重试主题列表中的主题删除操作

if (topicsEligibleForRetry.nonEmpty) {

retryDeletionForIneligibleReplicas(topicsEligibleForRetry)

}

// 调用 onTopicDeletion 方法，对待删除主题列表中的主题执行删除操作

if (topicsEligibleForDeletion.nonEmpty) {

onTopicDeletion(topicsEligibleForDeletion)

}

}

这里用一张图来说明下其执行流程：

![](https://article-images.zsxq.com/lqlj7fQg3OfQUyLPhAoQiQW_9ch0)

这里最关键的是 completeDeleteTopic 和 onTopicDeletion 方法，接下来我们分别来看下。

## **3.5.8 清除元数据和ZK状态**

private def completeDeleteTopic(topic: String): Unit = {

// 1、注销分区变更监听器，防止删除过程中因分区数据变更导致监听器被触发，引起状态不一致

client.mutePartitionModifications(topic)

// 2、获取该主题下处于 ReplicaDeletionSuccessful 状态的所有副本对象，即所有已经被成功删除的副本对象

val replicasForDeletedTopic \= controllerContext.replicasInState(topic, ReplicaDeletionSuccessful)

// 3、利用副本状态机将这些副本对象转换成 NonExistentReplica 状态。相当于在状态机中删除这些副本

replicaStateMachine.handleStateChanges(replicasForDeletedTopic.toSeq, NonExistentReplica)

// 4、移除 ZooKeeper 上关于该主题的信息

client.deleteTopic(topic, controllerContext.epochZkVersion)

// 5、移除元数据缓存中关于该主题的信息

controllerContext.removeTopic(topic)

}

该方法比较简单，主要就是清除一些信息而已。

## **3.5.9 执行主题删除操作**

private def onTopicDeletion(topics: Set\[String\]): Unit = {

// 1、找出给定待删除主题列表中那些尚未开启删除操作的所有主题

val unseenTopicsForDeletion \= topics.diff(controllerContext.topicsWithDeletionStarted)

if (unseenTopicsForDeletion.nonEmpty) {

// 2、获取到这些主题的所有分区对象

val unseenPartitionsForDeletion \= unseenTopicsForDeletion.flatMap(controllerContext.partitionsForTopic)

// 3、将这些分区的状态依次调整成 OfflinePartition 和 NonExistentPartition，相当于将这些分区从分区状态机中删除

partitionStateMachine.handleStateChanges(unseenPartitionsForDeletion.toSeq, OfflinePartition)

partitionStateMachine.handleStateChanges(unseenPartitionsForDeletion.toSeq, NonExistentPartition)

// 4、把这些主题加到“已开启删除操作”主题列表中

controllerContext.beginTopicDeletion(unseenTopicsForDeletion)

}

// 5、给集群所有 Broker 发送元数据更新请求，因为已经删除了，告诉它们不要再为这些主题处理数据了

client.sendMetadataUpdate(topics.flatMap(controllerContext.partitionsForTopic))

// 6、执行真正的底层物理磁盘文件删除，它实际上是通过副本状态机状态转换操作完成。

onPartitionDeletion(topics)

}

删除操作步骤如下：

1.  找出给定待删除主题列表中那些尚未开启删除操作的所有主题。
2.  获取到这些主题的所有分区对象。
3.  将这些分区的状态依次调整成 OfflinePartition 和 NonExistentPartition，相当于将这些分区从分区状态机中删除。
4.  把这些主题加到“已开启删除操作”主题列表中。
5.  给集群所有 Broker 发送元数据更新请求，因为已经删除了，告诉它们不要再为这些主题处理数据了.
6.  执行真正的底层物理磁盘文件删除，它实际上是通过副本状态机状态转换操作完成。

接着我们重点看下「**第六步**」。

## **3.5.10 执行真正删除动作**

实际上它是通过副本状态机的状态转换操作来完成的，源码如下：

private def onPartitionDeletion(topicsToBeDeleted: Set\[String\]): Unit = {

// 存储所有已死亡的副本

val allDeadReplicas \= mutable.ListBuffer.empty\[PartitionAndReplica\]

// 存储所有待删除的副本，

val allReplicasForDeletionRetry \= mutable.ListBuffer.empty\[PartitionAndReplica\]

// 存储不适合删除的所有主题

val allTopicsIneligibleForDeletion \= mutable.Set.empty\[String\]

// 对每个主题执行以下步骤

topicsToBeDeleted.foreach { topic =>

// 1、获取活着和死亡的副本

val (aliveReplicas, deadReplicas) = controllerContext.replicasForTopic(topic).partition { r => controllerContext.isReplicaOnline(r.replica, r.topicPartition)

}

// 2、获取标记为 ReplicaDeletionSuccessful 的主题的已成功删除副本

val successfullyDeletedReplicas \= controllerContext.replicasInState(topic, ReplicaDeletionSuccessful)

// 3、找到待删除的副本，即从活着的副本中排除已成功删除的副本

val replicasForDeletionRetry \= aliveReplicas.diff(successfullyDeletedReplicas)

// 4、将所有已死亡的副本添加到 allDeadReplicas 缓存中

allDeadReplicas ++= deadReplicas

// 将所有待删除的副本添加到 allReplicasForDeletionRetry 缓存中

allReplicasForDeletionRetry ++= replicasForDeletionRetry

// 5、如果有死亡的副本，则记录该主题不适合删除

if (deadReplicas.nonEmpty) {

debug(s"Dead Replicas (${deadReplicas.mkString(",")}) found for topic $topic")

allTopicsIneligibleForDeletion += topic

}

}

// move dead replicas directly to failed state

// 6、将所有 Dead replicas 副本设置为 "ReplicaDeletionIneligible 不适合删除状态"，如果某些副本已死，也将相应的主题标记为不适合删除，因为它无论如何都不会成功完成。

replicaStateMachine.handleStateChanges(allDeadReplicas, ReplicaDeletionIneligible)

// send stop replica to all followers that are not in the OfflineReplica state so they stop sending fetch requests to the leader

// 7、将所有待删除的副本设置为 "OfflineReplica 下线状态"，当副本状态转换成 OfflineReplica ，此时会对该 Topic 的所有副本所在 Broker 发起 StopReplicaRequest 请求（即通知所有非处于 OfflineReplica 状态的 follower 停止向 Leader 发送拉取 fetch 请求）。

replicaStateMachine.handleStateChanges(allReplicasForDeletionRetry, OfflineReplica)

// 8、将所有待删除的副本设置为 "ReplicaDeletionStarted 待删除"，当副本状态转换成 ReplicaDeletionStarted 状态，此时会对该 Topic 的所有副本所在 Broker 发起 StopReplicaRequest 请求（参数 deletePartitions = true 表示执行删除操作），将发送带有 deletePartition=true 的 StopReplicaRequest ，并将删除相应分区的所有副本中的所有持久数据。

replicaStateMachine.handleStateChanges(allReplicasForDeletionRetry, ReplicaDeletionStarted)

// 9、如果有任何不适合删除的主题，则记录该主题不适合删除的原因，并将其标记为不可删除

if (allTopicsIneligibleForDeletion.nonEmpty) {

markTopicIneligibleForDeletion(allTopicsIneligibleForDeletion, reason = "offline replicas")

}

}

该方法**主要作用是在删除的主题集合上执行一系列操作**，主要包括：

1.  标记死亡的副本。
2.  将活着的副本设置为 Offline。
3.  将所有不适合删除的主题记录下来并将其标记为不可删除。

步骤如下：

1.  获取活着和死亡的副本。
2.  获取标记为 ReplicaDeletionSuccessful 的主题的已成功删除副本。
3.  找到待删除的副本，即从活着的副本中排除已成功删除的副本。
4.  将所有已死亡的副本添加到 allDeadReplicas 缓存中，将所有待删除的副本添加到 allReplicasForDeletionRetry 缓存中。
5.  如果有死亡的副本，则记录该主题不适合删除。
6.  将所有 Dead replicas 副本设置为 "ReplicaDeletionIneligible 不适合删除状态"，如果某些副本已死，也将相应的主题标记为不适合删除，因为它无论如何都不会成功完成。
7.  将所有待删除的副本设置为 "OfflineReplica 下线状态"，当副本状态转换成 OfflineReplica ，此时会对该 Topic 的所有副本所在 Broker 发起 StopReplicaRequest 请求（参数 deletePartitions = false,表示还不执行删除操作,即通知所有非处于 OfflineReplica 状态的 follower 停止向 Leader 发送拉取 fetch 请求）。
8.  将所有待删除的副本设置为 "ReplicaDeletionStarted 待删除"，当副本状态转换成 ReplicaDeletionStarted 状态，此时会对该 Topic 的所有副本所在 Broker 发起 StopReplicaRequest 请求（参数 deletePartitions = true 表示执行删除操作），将发送带有 deletePartition=true 的 StopReplicaRequest ，并将删除相应分区的所有副本中的所有持久数据。
9.  最后如果有任何不适合删除的主题，则记录该主题不适合删除的原因，并将其标记为不可删除。

## **3.5.11 Broker 收到 StopReplicaRequest 请求处理流程**

上面步骤中说到向副本所属 Broker 发送 StopReplicaRequest 请求，**那么到底做了什么呢？我们来看下**。

如果对 KafkaApis 不了解的可以直接移步到：[【服务端Broker源码分析系列第六篇】图解Kafka源码之 KafkaApis 详解](https://articles.zsxq.com/id_k4o72eh3pai3.html) 进行学习。

这里直接定位到 [KafkaApis#](http://kafkaapis/#handleStopReplicaRequest)[handleStopReplicaRequest](http://kafkaapis/#handleStopReplicaRequest) 方法。

  
![](https://article-images.zsxq.com/FieL8whOIhw2jZGkMdDoR8QtaThN)

源码太多了，这里就通过图片直接指向。

  
![](https://article-images.zsxq.com/FsUE2tosNO3d8WUDMGbqDYJCJ2yr)

![](https://article-images.zsxq.com/Fgb2lxEBNY2c0-q2F2NkLthY2WuF)

![](https://article-images.zsxq.com/FtTKlG-9Nwu-S6xOh3QP08FHnAOy)

![](https://article-images.zsxq.com/FgdRJgA2PLGuFzRpqOQLm7BdNRRw)

最后来剖析下 [LogManager#](http://logmanager/#getOrCreateLog)asyncDelete 方法，源码如下：

/\*\*

\* Rename the directory of the given topic-partition "logdir" as "logdir.uuid.delete" and

\* add it in the queue for deletion.

\* @param topicPartition TopicPartition that needs to be deleted

\* @param isFuture True iff the future log of the specified partition should be deleted

\* @param checkpoint True if checkpoints must be written

\* @return the removed log

\*/

def asyncDelete(topicPartition: TopicPartition,

isFuture: Boolean = false,

checkpoint: Boolean = true): Option\[Log\] = {

// 1、使用同步锁确保线程安全

val removedLog: Option\[Log\] = logCreationOrDeletionLock synchronized {

removeLogAndMetrics(

// 从 currentLogs 或者 futureLogs 中移除指定的 topicPartition 的 Log，并返回被移除的 Log

if (isFuture) futureLogs else currentLogs, topicPartition)

}

// 2、根据 removeLogAndMetrics 方法返回的结果 removedLog 进行处理

removedLog match {

case Some(removedLog) =>

// We need to wait until there is no more cleaning task on the log to be deleted before actually deleting it.

// 我们需要等到要删除的日志上没有更多的清理任务，然后才能真正删除它。

if (cleaner != null && !isFuture) {

// 终止正在进行的 Log 清理任务

cleaner.abortCleaning(topicPartition)

if (checkpoint) {

// 更新 Checkpoint 文件以进行删除操作

cleaner.updateCheckpoints(removedLog.parentDirFile, partitionToRemove = Option(topicPartition))

}

}

// 将被删除的 Log 的目录重命名为日志删除目录，命名规则 topic-uuid-delete

removedLog.renameDir(Log.logDeleteDirName(topicPartition))

if (checkpoint) {

val logDir \= removedLog.parentDirFile

// 获取目录下的 Log 文件

val logsToCheckpoint \= logsInDir(logDir)

// 对 Log 文件进行 Checkpoint

checkpointRecoveryOffsetsInDir(logDir, logsToCheckpoint)

checkpointLogStartOffsetsInDir(logDir, logsToCheckpoint)

}

// 将被删除的 Log 添加到待删除列队列中，稍后会被异步删除

addLogToBeDeleted(removedLog)

info(s"Log for partition ${removedLog.topicPartition} is renamed to ${removedLog.dir.getAbsolutePath} and is scheduled for deletion")

case None \=\>

if (offlineLogDirs.nonEmpty) {

// 如果无法删除 Log，可能是因为 Log 所在的目录处于离线状态，抛出异常提示

throw new KafkaStorageException(s"Failed to delete log for ${if (isFuture) "future" else ""} $topicPartition because it may be in one of the offline directories ${offlineLogDirs.mkString(",")}")

}

}

removedLog

}

步骤如下：

1.  使用同步锁确保线程安全，从 currentLogs 或者 futureLogs 中移除指定的 topicPartition 的 Log，并返回被移除的 Log。
2.  根据 removeLogAndMetrics 方法返回的结果 removedLog 进行处理。
3.  我们需要等到要删除的日志上没有更多的清理任务，然后才能真正删除它。
4.  将被删除的 Log 的目录重命名为日志删除目录，命名规则 topic-uuid-delete。
5.  将被删除的 Log 添加到待删除 Log 队列中，稍后会被异步删除。

这里重点来看下 「**第二步**」的最后一步。

## **3.5.12 日志清理定时线程**

private def addLogToBeDeleted(log: Log): Unit = {

// 被删除的 Log 添加到待删除 Log 队列中，稍后会被异步删除

this.logsToBeDeleted.add((log, time.milliseconds()))

}

当将待删除的 Log 添加到 logsToBeDeleted 待删除 Log 队列中后，你是否还记得在 [【服务端 Broker 源码分析系列第十五篇】图解 Kafka 源码之 Log 日志管理操作](https://articles.zsxq.com/id_5p3wdbasv08k.html) 这篇中，里面有一个单独线程 [kafka-delete-logs](http://kafka-delete-logs/) ，它是在 「**LogManager**」初始化时启动后专门来处理的。

该线程底层执行的是 deleteLogs() 方法，主要用来**定时将已被标记为 .delete 后缀的日志文件进行删除**，也就是不断从 logsToBeDeleted 队列中取出然后异步删除。

##   
**3.5.13 StopReplica 请求成功后执行回调函数**

当 Topic 删除完成后会清理相关信息， 触发的地方是: 每个 Broker 执行删除 StopReplica 成功之后，都会执行一个回调函数，如下：

![](https://article-images.zsxq.com/Ft1J5cYhkPe-J2OBP_sdMJxyXiSt)

最终会执行 TopicDeletionStopReplicaResponseReceived 方法，其调用方是 Controller，最终回调也是Controller，它会触发 KafkaController#process 方法。

![](https://article-images.zsxq.com/Fuc-bG9a1b-VtzsYzkwQjFisMcRN)

// KafkaController 类

private def processTopicDeletionStopReplicaResponseReceived(replicaId: Int,

requestError: Errors,partitionErrors: Map\[TopicPartition, Errors\]): Unit = {

// 1、如果当前 Broker 不是 Controler 直接返回

if (!isActive) return

debug(s"Delete topic callback invoked on StopReplica response received from broker $replicaId: " + s"request error = $requestError, partition errors = $partitionErrors")

// 2、获取存在错误的分区

val partitionsInError \= if (requestError != Errors.NONE)

partitionErrors.keySet

else

partitionErrors.filter { case (\_, error) => error != Errors.NONE }.keySet

// 3、根据存在错误的分区和 replicaId 获取存在错误的副本

val replicasInError \= partitionsInError.map(PartitionAndReplica(\_, replicaId))

// 4、将所有失败的副本移到 ReplicaDeletionIneligible 状态

topicDeletionManager.failReplicaDeletion(replicasInError)

// 5、如果存在成功删除的副本，将其从存在错误的分区中移除，并标记为已完成删除

if (replicasInError.size != partitionErrors.size) {

// some replicas could have been successfully deleted

val deletedReplicas \= partitionErrors.keySet.diff(partitionsInError)

topicDeletionManager.completeReplicaDeletion(deletedReplicas.map(PartitionAndReplica(\_, replicaId)))

}

}

步骤如下：

1.  如果当前 Broker 不是 Controler 直接返回。
2.  获取存在错误的分区。
3.  根据存在错误的分区和 replicaId 获取存在错误的副本。
4.  如果回调有异常，存在删除失败的副本，则将副本状态转换成 ReplicaDeletionIneligible 状态。
5.  如果回调正常，存在成功删除的副本，则将其从存在错误的分区中移除，并标记为已完成删除，即状态从 ReplicaDeletionStarted 变更为 ReplicaDeletionSuccessful。

这里最重要的是 「**第四步**」、「**第五步**」。

def failReplicaDeletion(replicas: Set\[PartitionAndReplica\]): Unit = {

// 启用了删除主题功能

if (isDeleteTopicEnabled) {

// 筛选出待删除 topic 集合中删除失败的 replicas

val replicasThatFailedToDelete \= replicas.filter(r => isTopicQueuedUpForDeletion(r.topic))

if (replicasThatFailedToDelete.nonEmpty) {

// 如果有删除失败的 replicas，则记录下待删除的 topics 并使这些 replicas 的状态转变为ReplicaDeletionIneligible，并将这些 topics 标记为不可删除

val topics \= replicasThatFailedToDelete.map(\_.topic)

debug(s"Deletion failed for replicas ${replicasThatFailedToDelete.mkString(",")}. Halting deletion for topics $topics")

replicaStateMachine.handleStateChanges(replicasThatFailedToDelete.toSeq, ReplicaDeletionIneligible)

markTopicIneligibleForDeletion(topics, reason = "replica deletion failure")

// 重启主题删除操作

resumeDeletions()

}

}

}

def completeReplicaDeletion(replicas: Set\[PartitionAndReplica\]): Unit = {

// 筛选出待删除 topic 集合中成功删除的 replicas

val successfullyDeletedReplicas \= replicas.filter(r => isTopicQueuedUpForDeletion(r.topic))

debug(s"Deletion successfully completed for replicas ${successfullyDeletedReplicas.mkString(",")}")

// 将这些 replicas 的状态转变为 ReplicaDeletionSuccessful

replicaStateMachine.handleStateChanges(successfullyDeletedReplicas.toSeq, ReplicaDeletionSuccessful)

// 重启主题删除操作

resumeDeletions()

}

看到这 2 个方法最后都会重启主题删除操作，上面小节已经剖析过，忘记的可以回头去看。

至此，删除 Topic 的流程就剖析完毕了。

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头通过「**命令行的方式**」，引出 Kafka Topic 删除的入口，底层调用**封装在 Kafka Core 包中的 TopicCommand 类**。。

2、接着带大家深度剖析了 「**删除 Topic**」的源码全流程，Kafka 主题的删除依赖于 KafkaAdminClient 跟Controller 服务器进行 RPC 通信完成主题删除流程。在此之前首先需要跟任意 broker 服务器通信得到 Kafka 的metadata 数据，来获得 ControllerNode 的信息，然后再跟 ControllerNode 进行通信。

下篇我们来深度剖析「**副本状态机机制实现原理**」，大家期待，我们下期见。