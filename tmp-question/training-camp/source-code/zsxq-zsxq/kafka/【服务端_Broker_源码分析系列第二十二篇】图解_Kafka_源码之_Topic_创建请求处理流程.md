大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端控制器 Controller 如何处理事件的**」，了解了 Controller 「**单线程事件处理模型**」的实现流程，从今天开始，我们来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 Topic 创建请求处理流程**」。

  
![](https://article-images.zsxq.com/FlCxZCx6igoJPj3GZ9OjsBAZBWi1)

##   
**01 总体概述**

在深入剖析 Kafka **Topic 创建流程**之前，我想你可能或多或少会有这样的疑问:

**Kafka 服务端是如何进行接收创建请求，内部又是如何处理的呢？**

这里我们通过命令行执行 Kafka Topic 创建、删除作为入口。

## **1.1 创建 Topic 命令**

大家都知道，创建 Topic 可以执行以下命令：

\# zk 方式，新版不推荐使用

bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 3 --partitions 2 --topic message3

\# kafka版本 >= 2.2 支持下面方式（推荐）

bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 3 --partitions 2 --topic message3

那么今天就来看看通过这个脚本 kafka-topics.sh 内部实现流程。

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

## **03 创建 Topic 元数据要求**

我们知道在 Kafka 中的，Topic 是 Kafka 中的基础概念，是一切消息处理的基础，主题属于 Kafka 元数据的一部分，最后会存储在 Zookeeper 中，因此**在创建主题的过程也就是往 Zookeeper 写入数据的过程**。

创建 Topic 需要提交的元数据信息主要包括以下5个：

1.  **主题名称（必填）**：不能是已存在的名称，长度不超过 249 ，最好不要有“."，只输入字母数字或下划线。
2.  **该主题的分区数量 numPartitions（可选）**该主题的分区数量numPartitions（可选）：不输入的话系统根据配置中的默认分区执行。
3.  **每个分区的副本数 replicationFactor（可选）**：不输入的话系统根据配置中的默认分区执行，副本数不能大于broker的数量。
4.  **分区和 Brokers 分配方案 assignments (可选)** ：可以输入分区下标对应的brokerId列表（对应副本所在），不输入的话系统根据默认分配方案进行分配。
5.  **该执行擦破做的配置文件 command-configs（可选）**：默认会找到类路径下的配置，还可以直接指定一些配置，如：brokers,zookeeper等。

##   
**04 创建 Topic 源码流程**

这里先给出一张创建 Topic 的流程，接下来再细细剖析。

![](https://article-images.zsxq.com/lknwXJn2FL2_d_mw28qtKI1WwKLx)

## **4.1 源码入口**

「**TopicCommand.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/admin/TopicCommand.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/admin/TopicCommand.scala)

![](https://article-images.zsxq.com/FhXW8oVNpUmfIfaiG4tkotd1l68v)

从上面源码得出：

1.  根据命令行是否有传入参数--zookeeper 来判断创建哪一种对象 topicService，如果传入了 --zookeeper 则创建 类 ZookeeperTopicService 对象，否则创建类 AdminClientTopicService 对象，**它是本篇我们分析的对象**。
2.  根据传入的参数类型判断是创建 Topic 还是删除 Topic 等等其他，判断依据是：是否在参数里传入了 --create。

这里最终确定创建的是 **AdminClientTopicService** 对象，如下：

val topicService \= AdminClientTopicService(opts.commandConfig, opts.bootstrapServer)

当执行完上面这段源码后，底层会先调用 **Object AdminClientTopicService#apply** 方法。

## **4.2 AdminClientTopicService**

import org.apache.kafka.clients.admin.{Admin, ConfigEntry, ListTopicsOptions, NewPartitions, NewTopic, PartitionReassignment, Config => JConfig}

object AdminClientTopicService {

def createAdminClient(commandConfig: Properties, bootstrapServer: Option\[String\]): Admin = {

bootstrapServer match {

// 如果有指定 Kafka 集群地址，则设置到命令配置中

case Some(serverList) => commandConfig.put(CommonClientConfigs.BOOTSTRAP\_SERVERS\_CONFIG, serverList)

case None \=\>

}

// 创建 AdminClient 实例，底层调用 clients 包下面的 KafkaAdminClient.java

Admin.create(commandConfig)

}

// 默认会执行该方法，类似注解，会调用 createAdminClient

def apply(commandConfig: Properties, bootstrapServer: Option\[String\]): AdminClientTopicService = new AdminClientTopicService(createAdminClient(commandConfig, bootstrapServer))

}

这里看下 **createAdminClient** 方法的执行逻辑：

1.  首先如果有入参 --command-config config/producer.proterties 时则将该文件里面的参数都放到 [map commandConfig](http://map%20commandconfig/) 里面, 并且也加入 bootstrap.servers 的参数。假如配置文件里面已经有了 bootstrap.servers 配置，那么会将其覆盖。
2.  接着将上面的 commandConfig 作为入参调用 [Admin.create](http://admin.create/)(commandConfig) 创建 Admin。从这里我们就可以看出,我们执行 [kafka-topic.sh](http://kafka-topic.sh/) 脚本实际上是 kafka 模拟了一个客户端 Client 来创建 Topic 的过程。

我们主要看下第二步的调用类。

## **4.3 KafkaAdminClient**

「**Admin.java**」源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/admin/Admin.java](https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/admin/Admin.java)

「**kafkaAdminClient.java**」源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/admin/KafkaAdminClient.java](https://github.com/apache/kafka/blob/2.8.0/clients/src/main/java/org/apache/kafka/clients/admin/KafkaAdminClient.java)

![](https://article-images.zsxq.com/FqdArkC61sxHKIWZF-ntCheR-NJW)

[Admin.create](http://admin.create/) 函数主要用于生成用于与 Broker 节点进行通信的 KafkaAdminClient 实例，从 [Admin.create](http://admin.create/) 的源码可以看到，其直接调用了 **kafkaAdminClient#createInternal** 方法来完成 adminClient 的初始化。如下：

![](https://article-images.zsxq.com/FvgccpCa1O73xA8Gpp8_1AHY4ISp)

static KafkaAdminClient createInternal(AdminClientConfig config, TimeoutProcessorFactory timeoutProcessorFactory) {

// 用于记录和报告指标的 Metrics 对象

Metrics metrics \= null;

// 客户端与服务器之间进行网络通信的 NetworkClient 对象

NetworkClient networkClient \= null;

// Time 对象，提供时间相关的操作

Time time \= Time.SYSTEM;

// 生成 Kafka 客户端ID

String clientId \= generateClientId(config);

// 构建客户端与服务器之间的通信渠道的 ChannelBuilder 对象

ChannelBuilder channelBuilder \= null;

// 选择就绪的网络连接的 Selector 对象

Selector selector \= null;

// Kafka 集群支持的 API 版本信息

ApiVersions apiVersions \= new ApiVersions();

// 创建日志上下文

LogContext logContext \= createLogContext(clientId);

try {

// 1、创建 AdminMetadataManager 对象，并设置重试和元数据最大缓存时间配置

AdminMetadataManager metadataManager \= new AdminMetadataManager(logContext,

config.getLong(AdminClientConfig.RETRY\_BACKOFF\_MS\_CONFIG),

config.getLong(AdminClientConfig.METADATA\_MAX\_AGE\_CONFIG));

// 2、解析和验证 bootstrap 服务器地址，更新 metadataManager 的集群信息

List<InetSocketAddress> addresses = ClientUtils.parseAndValidateAddresses(

config.getList(AdminClientConfig.BOOTSTRAP\_SERVERS\_CONFIG),

config.getString(AdminClientConfig.CLIENT\_DNS\_LOOKUP\_CONFIG));

// 将初始化的 Cluster 设置到 metadataManager 中,并设置其 state 为 QUIESCENT 状态.

metadataManager.update(Cluster.bootstrap(addresses), time.milliseconds());

// 3、根据配置创建 MetricsReporter 实例，并配置相关参数

List<MetricsReporter> reporters = config.getConfiguredInstances(AdminClientConfig.METRIC\_REPORTER\_CLASSES\_CONFIG,

MetricsReporter.class,

Collections.singletonMap(AdminClientConfig.CLIENT\_ID\_CONFIG, clientId));

// 4、配置 Metrics 的标签和参数

Map<String, String> metricTags = Collections.singletonMap("client-id", clientId);

MetricConfig metricConfig \= new MetricConfig().samples(config.getInt(AdminClientConfig.METRICS\_NUM\_SAMPLES\_CONFIG)).timeWindow(config.getLong(AdminClientConfig.METRICS\_SAMPLE\_WINDOW\_MS\_CONFIG), TimeUnit.MILLISECONDS).recordLevel(Sensor.RecordingLevel.forName(config.getString(AdminClientConfig.METRICS\_RECORDING\_LEVEL\_CONFIG))).tags(metricTags);

// 5、创建 JmxReporter 实例，启用 JMX 监控

JmxReporter jmxReporter \= new JmxReporter();

jmxReporter.configure(config.originals());

reporters.add(jmxReporter);

// 6、创建 MetricsContext 实例，用于向 Metrics 对象提供上下文信息

MetricsContext metricsContext \= new KafkaMetricsContext(JMX\_PREFIX,

config.originalsWithPrefix(CommonClientConfigs.METRICS\_CONTEXT\_PREFIX));

// 7、创建 Metrics 实例，用于记录和报告指标

metrics = new Metrics(metricConfig, reporters, time, metricsContext);

// 指标组前缀

String metricGrpPrefix \= "admin-client";

// 8、创建 ChannelBuilder 对象，用于构建客户端与服务器之间的通信渠道

channelBuilder = ClientUtils.createChannelBuilder(config, time, logContext);

// 9、创建 Selector 对象，用于选择就绪的网络连接

selector = new Selector(config.getLong(AdminClientConfig.CONNECTIONS\_MAX\_IDLE\_MS\_CONFIG),metrics, time, metricGrpPrefix, channelBuilder, logContext);

// 10、创建 NetworkClient 对象，用于客户端与服务器之间进行网络通信

networkClient = new NetworkClient(

selector,

metadataManager.updater(),

clientId,

1,

config.getLong(AdminClientConfig.RECONNECT\_BACKOFF\_MS\_CONFIG),

config.getLong(AdminClientConfig.RECONNECT\_BACKOFF\_MAX\_MS\_CONFIG),

config.getInt(AdminClientConfig.SEND\_BUFFER\_CONFIG),

config.getInt(AdminClientConfig.RECEIVE\_BUFFER\_CONFIG),

(int) TimeUnit.HOURS.toMillis(1),

config.getLong(AdminClientConfig.SOCKET\_CONNECTION\_SETUP\_TIMEOUT\_MS\_CONFIG),

config.getLong(AdminClientConfig.SOCKET\_CONNECTION\_SETUP\_TIMEOUT\_MAX\_MS\_CONFIG),

ClientDnsLookup.forConfig(config.getString(AdminClientConfig.CLIENT\_DNS\_LOOKUP\_CONFIG)),

time,

true,

apiVersions,

logContext);

// 11、创建并返回 KafkaAdminClient 实例

return new KafkaAdminClient(config, clientId, time, metadataManager, metrics, networkClient, timeoutProcessorFactory, logContext);

} catch (Throwable exc) {

closeQuietly(metrics, "Metrics");

closeQuietly(networkClient, "NetworkClient");

closeQuietly(selector, "Selector");

closeQuietly(channelBuilder, "ChannelBuilder");

throw new KafkaException("Failed to create new KafkaAdminClient", exc);

}

}

该方法相对比较简单，其步骤如下：

1.  创建用于管理 client 端 metadataCache 的 AdminMetadataManager 对象，并设置重试和元数据最大缓存时间配置。
2.  [retry.backoff.ms](http://retry.backoff.ms/) 默认值 100 ms, 当 metadata 相关请求失败后，下一次重试的 backOff 时间。
3.  [metadata.max.age.ms](http://metadata.max.age.ms/) 默认值5分钟，client 端 metadataCache 的过期时间，当 metadata 上一次更新时间超过这个值后，会重新请求获取新的 metadata。
4.  此时，其内部默认的 updater 实例默认为 AdminMetadataUpdater。
5.  解析和验证 bootstrap 服务器地址，更新 metadataManager 的集群信息。
6.  根据 bootstrap.servers 配置的 brokers 列表，初始化生成 Cluster 实例，并添加到 metadataManager 实例中。
7.  此时: Cluster 实例内部的 isBootstrapConfigured 属性默认值为 true。
8.  当 isBootstrapConfigured 值为 true 时,表示 metadataManager 处于未准备好的状态，并同时设置metadataManager 实例的 state 为 QUIESCENT。
9.  根据配置创建 MetricsReporter 实例，并配置相关参数。
10.  配置 Metrics 的标签和参数。
11.  创建 JmxReporter 实例，启用 JMX 监控。
12.  创建 MetricsContext 实例，用于向 Metrics 对象提供上下文信息。
13.  创建 Metrics 实例，用于记录和报告指标。
14.  创建 ChannelBuilder 对象，用于构建客户端与服务器之间的通信渠道。
15.  创建 Selector 对象，用于选择就绪的网络连接。
16.  创建用于与 Broker 进行网络通信的 NetworkClient 对象，用于客户端与服务器之间进行网络通信。
17.  最后创建并返回 **KafkaAdminClient** 实例对象。
18.  在 adminClient 端,其 clientId 的值默认为 adminclient-number 这样的值。
19.  在初始化 **KafkaAdminClient** 时,会同时初始化启动其内部的 **AdminClientRunnable** 线程，**此线程主要用来处理与 Broker 进行通信**。

## **4.3.1 AdminClientRunnable 线程**

当 **KafkaAdminClient** 对象初始化后，其内部的 **AdminClientRunnable I/O** 线程就被启动了。

启动过程中会执行 **processRequests** 方法会生成 **MetadataRequest** 请求，并立即向 Broker 节点请求获取集群的 **metadata** 信息。

在 **KafkaAdminClient** 对象刚初始化完成时，**MetadataManager** 中 [lastMetadataUpdateMs](http://lastmetadataupdatems%20/) 与[lastMetadataFetchAttemptMs](http://lastmetadatafetchattemptms/) 属性默认值为 0，所以当执行到 [metadataManager.metadataFetchDelayMs(now)](http://metadatamanager.metadatafetchdelayms\(now\)/) 得到的 delay 时间为 0，此时将会执行 **makeMetadataCall 方法来生成 MetadataRequest**。

源码如下：

private final class AdminClientRunnable implements Runnable {

....

@Override

// 启动 AdminClientRunnable I/O 线程

public void run() {

log.trace("Thread starting");

try {

// 处理请求

processRequests();

} finally {

....

}

}

// 处理请求，最主要是获取元数据

private void processRequests() {

long now \= time.milliseconds();

while (true) {

// Copy newCalls into pendingCalls.

// 将 newCalls 队列中新生成的 Request 移动到 pendingCalls 队列中

drainNewCalls();

// Check if the AdminClient thread should shut down.

long curHardShutdownTimeMs \= hardShutdownTimeMs.get();

if ((curHardShutdownTimeMs != INVALID\_SHUTDOWN\_TIME) && threadShouldExit(now, curHardShutdownTimeMs))

break;

// Handle timeouts.

TimeoutProcessor timeoutProcessor \= timeoutProcessorFactory.create(now);

timeoutPendingCalls(timeoutProcessor);

timeoutCallsToSend(timeoutProcessor);

timeoutCallsInFlight(timeoutProcessor);

long pollTimeout \= Math.min(1200000, timeoutProcessor.nextTimeoutMs());

if (curHardShutdownTimeMs != INVALID\_SHUTDOWN\_TIME) {

pollTimeout = Math.min(pollTimeout, curHardShutdownTimeMs - now);

}

// Choose nodes for our pending calls.

// maybeDrainPendingCalls：将 pendingCalls 队列的 Request 移动到 callsToSend 队列中( 根据 Call.nodeProvide 查找到 node)

pollTimeout = Math.min(pollTimeout, maybeDrainPendingCalls(now));

//1、判断当前 metadata 是否过期,初始化实例时 metadataFetchDelayMs 函数返回 0,表示过期需要重新获取 metadata。

long metadataFetchDelayMs \= metadataManager.metadataFetchDelayMs(now);

if (metadataFetchDelayMs == 0) {

// 2、将 metadataManager 对应的 state 的状态设置为 UPDATE\_PENDING。

metadataManager.transitionToUpdatePending(now);

// 3、生成 MetadataRequest 请求实例,此时将向 bootstrap.servers 配置的 broker 节点的随机一个节点发起请求。

Call metadataCall \= makeMetadataCall(now);

// 将获取 metadata 信息的 Call 实例添加到 callsToSend 队列中。

if (!maybeDrainPendingCall(metadataCall, now))

pendingCalls.add(metadataCall);

}

// 4、执行 sendEligibleCalls 方法发起 MetadataRequest 请求。

// callsToSend 队列中的 Request 向指定的目标节点发起网络请求，此时请求将由 broker 端接收到后,会被 ForwardingManager 组件包装为 EnvelopeRequest 后,直接转发给 activeController 进行处理.通过ForwardingManager中的BrokerToControllerChannelManager 向activeController转发请求。

pollTimeout = Math.min(pollTimeout, sendEligibleCalls(now));

if (metadataFetchDelayMs > 0) {

pollTimeout = Math.min(pollTimeout, metadataFetchDelayMs);

}

if (!pendingCalls.isEmpty())

pollTimeout = Math.min(pollTimeout, retryBackoffMs);

// Wait for network responses.

log.trace("Entering KafkaClient#poll(timeout={})", pollTimeout);

List<ClientResponse> responses = client.poll(pollTimeout, now);

log.trace("KafkaClient#poll retrieved {} response(s)", responses.size());

// unassign calls to disconnected nodes

unassignUnsentCalls(client::connectionFailed);

// Update the current time and handle the latest responses.

now = time.milliseconds();

// 5、最后当 broker 端接收并完成对 MetadataRequest 请求处理后进行响应的 response

handleResponses(now, responses);

}

}

启动 **AdminClientRunnable I/O** 线程时步骤如下：

1.  判断当前 metadata 是否过期，初始化实例时 metadataFetchDelayMs 函数返回 0，表示过期需要重新获取 metadata。
2.  将 metadataManager 对应的 state 的状态设置为 UPDATE\_PENDING。
3.  生成 MetadataRequest 请求实例,此时将向 bootstrap.servers 配置的 broker 节点的随机一个节点发起请求。
4.  makeMetadataCall 方法生成用于处理 MetadataRequest 的 Call 实例，此 Call 实例对应的 nodeProvider实现为 MetadataUpdateNodeIdProvider，其生成MetadataRequest参数的实现源码如下：

/\*\*

\* Create a new metadata call.

\*/

private Call makeMetadataCall(long now) {

return new Call(true, "fetchMetadata", calcDeadlineMs(now, requestTimeoutMs),

new MetadataUpdateNodeIdProvider()) {

@Override

public MetadataRequest.Builder createRequest(int timeoutMs) {

return new MetadataRequest.Builder(new MetadataRequestData()

.setTopics(Collections.emptyList())

.setAllowAutoTopicCreation(true));

}

}

....

}

}

1.  maybeDrainPendingCall 方法，通过 MetadataUpdateNodeIdProvider 随机获取 bootstrap.servers 中的一个 broker 节点发起 Metadata 请求。

private boolean maybeDrainPendingCall(Call call, long now) {

try {

// 根据 MetadataUpdateNodeIdProvider 随机获取一个 broker 节点，同时把 metadata 请求信息添加到 callsToSend 队列中.

Node node \= call.nodeProvider.provide();

if (node != null) {

log.trace("Assigned {} to node {}", call, node);

call.curNode = node;

getOrCreateListValue(callsToSend, node).add(call);

return true;

} else {

log.trace("Unable to assign {} to a node.", call);

return false;

}

} catch (Throwable t) {

// Handle authentication errors while choosing nodes.

log.debug("Unable to choose node for {}", call, t);

call.fail(now, t);

return true;

}

}

1.  执行 sendEligibleCalls 方法发起 MetadataRequest 请求。
2.  把 callsToSend 队列中已经准备好的请求发送到目标 Broker 节点。此时请求将由 broker 端接收到后,会被 ForwardingManager 组件包装为 EnvelopeRequest 后,直接转发给 activeController 进行处理.通过ForwardingManager中的 BrokerToControllerChannelManager 向 activeController 转发请求。
3.  在初始化时此队列中默认只有 MetadataRequest 请求，因此此时只是向目标节点发起获取 Metadata 请求。
4.  **此时发起的 MetadataRequest 请求将由 Broker 端的 KafkaApis 中的 handleTopicMetadataRequest 进行处理**。
5.  此请求在broker端将返回当前 metadataImage 中所有的 activeBrokers 节点信息，**并从 activeBrokers 中随机选择一个节点作为 Controller 节点**。
6.  请求最终将由 Controller 中的 ReplicationControlManager 组件来进行 CreateTopicsRequest 处理。在 controller 中ReplicationControlManager 组件用于管理集群的 topic 信息以及各 ISR 与 partitionLeader 的平衡。并在向 metadata 中写入 TopicRecord 与 PartitionRecord 消息后, 最后由所有 kafkaServer 节点进行 replay 来执行真正意义上的创建工作。同时 Broker 端收到 activeController 的 response 后,再重新把 response 转发给 client 端。
7.  最后当 broker 端接收并完成对 MetadataRequest 请求处理后进行响应的 response。
8.  它将由 makeMetadataCall 生成的 Call 实例处理。
9.  根据 Metadata 请求响应的 activeBrokers 与 controller 节点信息，重新生成 Cluster 实例，此时生成的Cluster 实例对应的 isBootstrapConfigured 属性的值为 false ，**表示 metadata 已经准备好**。
10.  执行 metadataManager 的 update 函数，把最新生成的 Cluster 更新到 metadataManager 实例中。此时 metadataManager 对应的 lastMetadataUpdateMs 属性(**metadata 最后更新时间**) 将被更新为当前时间。

private void handleResponses(long now, List<ClientResponse> responses) {

for (ClientResponse response : responses) {

int correlationId \= response.requestHeader().correlationId();

Call call \= correlationIdToCalls.get(correlationId);

....

try {

// 最终调用 call#handleResponse

call.handleResponse(response.responseBody());

if (log.isTraceEnabled())

log.trace("{} got response {}", call, response.responseBody().toString(response.requestHeader().apiVersion()));

} catch (Throwable t) {

if (log.isTraceEnabled())

log.trace("{} handleResponse failed with {}", call, prettyPrintException(t));

call.fail(now, t);

}

}

}

}

@Override

public void handleResponse(AbstractResponse abstractResponse) {

MetadataResponse response \= (MetadataResponse) abstractResponse;

long now \= time.milliseconds();

metadataManager.update(response.cluster(), now);

// Unassign all unsent requests after a metadata refresh to allow for a new

// destination to be selected from the new metadata

unassignUnsentCalls(node -> true);

}

最终当 **KafkaAdminClient** 实例对象创建完成后，会继续传参到 **AdminClientTopicService** 类中，这样就完成了实例化 **topicService** ，接着判断命令行执行创建 Topic 操作，源码如下：

// 如果命令行输入 --create 后会执行该方法

if (opts.hasCreateOption)

topicService.createTopic(opts)

它会执行 **AdminClientTopicService** 类的伴生类的 **createTopic** 方法。

## **4.4 createTopic()**

case class AdminClientTopicService private (adminClient: Admin) extends TopicService {

// 创建 Topic

override def createTopic(topic: CommandTopicPartition): Unit = {

// 1、如果配置了副本副本数参数 --replication-factor 时必须大于 0，否则抛异常

if (topic.replicationFactor.exists(rf => rf > Short.MaxValue || rf < 1))

throw new IllegalArgumentException(s"The replication factor must be between 1 and ${Short.MaxValue} inclusive")

// 2、如果配置了分区数参数 --partitions 时也必须大于 0，否则抛异常

if (topic.partitions.exists(partitions => partitions < 1))

throw new IllegalArgumentException(s"The partitions must be greater than 0")

try {

val newTopic \= if (topic.hasReplicaAssignment)

// 3、如果指定了--replica-assignment参数，则按照指定的来分配副本

new NewTopic(topic.name, asJavaReplicaReassignment(topic.replicaAssignment.get))

else {

new NewTopic(

topic.name,

topic.partitions.asJava,

topic.replicationFactor.map(\_.toShort).map(Short.box).asJava)

}

// 4、将配置参数 --config 解析成一个配置 map

val configsMap \= topic.configsToAdd.stringPropertyNames()

.asScala

.map(name => name -> topic.configsToAdd.getProperty(name))

.toMap.asJava

newTopic.configs(configsMap)

// 5、最后调用 adminClient 创建 Topic

val createResult \= adminClient.createTopics(Collections.singleton(newTopic),

new CreateTopicsOptions().retryOnQuotaViolation(false))

createResult.all().get()

println(s"Created topic ${topic.name}.")

} catch {

case e : ExecutionException =>

if (e.getCause == null)

throw e

if (!(e.getCause.isInstanceOf\[TopicExistsException\] && topic.ifTopicDoesntExist()))

throw e.getCause

}

}

....

}

步骤如下：

1.  如果配置了副本副本数参数 --replication-factor 时必须大于 0，否则抛异常。
2.  如果配置了分区数参数 --partitions 时也必须大于 0，否则抛异常。
3.  如果指定了 --replica-assignment 参数，则按照指定的来分配副本。
4.  将配置参数 --config 解析成一个配置 map，configsMap 再赋值给 NewTopic 对象中的 configs。
5.  最后调用 adminClient 创建 Topic，[adminClient.createTopics](http://adminclient.createtopics/)。

那么它到底是如何创建 Topic 的呢？我们接着来剖析。

## **4.5 KafkaAdminClient#createTopics()**

@Override

public CreateTopicsResult createTopics(final Collection<NewTopic> newTopics,

final CreateTopicsOptions options) {

// 1、生成 topic 创建是否成功的 future 监听, client 端可通过对函数调用的返回值来监听创建的成功失败。

final Map<String, KafkaFutureImpl<TopicMetadataAndConfig>> topicFutures = new HashMap<>(newTopics.size());

// 2、创建一个 CreatableTopicCollection 对象来存储需要创建的主题

final CreatableTopicCollection topics \= new CreatableTopicCollection();

// 3、遍历每个 NewTopic 对象

for (NewTopic newTopic : newTopics) {

// 检查 topic 名称是否无法表示

if (topicNameIsUnrepresentable(newTopic.name())) {

// 如果无法表示，创建一个完成异常的future对象，并将其放入topicFutures中

KafkaFutureImpl<TopicMetadataAndConfig> future = new KafkaFutureImpl<>();

future.completeExceptionally(new InvalidTopicException("The given topic name '" +

newTopic.name() + "' cannot be represented in a request."));

topicFutures.put(newTopic.name(), future);

// 如果 topicFutures 中不包含该 topic 名称，则在 topicFutures 中创建一个空的KafkaFutureImpl 对象，并将其作为 value，topic 名称作为 key 放入 topicFutures 中

} else if (!topicFutures.containsKey(newTopic.name())) {

topicFutures.put(newTopic.name(), new KafkaFutureImpl<>());

// 将 NewTopic 对象转换为 CreatableTopic 对象，并添加到 topics 中

topics.add(newTopic.convertToCreatableTopic());

}

}

// 4、如果 topics 不为空，则执行创建主题的操作

如果校验并转换为\`CreatableTopic\`的信息不为空,生成发起\`CreateTopicsRequest\`请求的\`Call\`实例.

//==>

if (!topics.isEmpty()) {

// 获取当前时间

final long now \= time.milliseconds();

// 计算截止时间

final long deadline \= calcDeadlineMs(now, options.timeoutMs());

// 5、创建一个 Call 对象，用于执行创建主题的操作

final Call call \= getCreateTopicsCall(options, topicFutures, topics,

Collections.emptyMap(), now, deadline);

// 调用 runnable 对象的 call 方法来执行 Call 对象,并添加到 runnable 线程的 newCalls 队列中。

runnable.call(call, now);

}

// 6、返回一个新的 CreateTopicsResult 对象，其中包含 topicFutures 的副本

return new CreateTopicsResult(new HashMap<>(topicFutures));

}

步骤如下：

1.  生成 topic 创建是否成功的 future 监听, client 端可通过对函数调用的返回值来监听创建的成功失败。
2.  创建一个 CreatableTopicCollection 对象来存储需要创建的主题。
3.  遍历每个 NewTopic 对象，执行相关操作。
4.  如果 topics 不为空，则执行创建主题的操作。
5.  创建一个 Call 对象，用于执行创建主题的操作，调用 runnable 对象的 call 方法来执行 Call 对象,并添加到 runnable 线程的 newCalls 队列中。
6.  返回一个新的 CreateTopicsResult 对象，其中包含 topicFutures 的副本。

这里重点来剖析下「**第五步**」创建 topic 的核心方法 getCreateTopicsCall。

##   
**4.6 getCreateTopicsCall**

private Call getCreateTopicsCall(final CreateTopicsOptions options,

final Map<String, KafkaFutureImpl<TopicMetadataAndConfig>> futures,

final CreatableTopicCollection topics,

final Map<String, ThrottlingQuotaExceededException> quotaExceededExceptions,

final long now,final long deadline) {

return new Call("createTopics", deadline, new ControllerNodeProvider()) {

@Override

public CreateTopicsRequest.Builder createRequest(int timeoutMs) {

return new CreateTopicsRequest.Builder(

new CreateTopicsRequestData()

.setTopics(topics)

.setTimeoutMs(timeoutMs)

.setValidateOnly(options.shouldValidateOnly()));

}

@Override

public void handleResponse(AbstractResponse abstractResponse) {

....

}

private ConfigEntry configEntry(CreatableTopicConfigs config) {

....

}

@Override

void handleFailure(Throwable throwable) {

....

}

};

}

这段源码我们主要看下 Call 回调函数里面的方法， 这里**先不管 Kafka 客户端如何跟服务端进行通信的细节，我们主要关注创建 Topic 的相关逻辑**。

Call 回调函数中的 createRequest 方法主要用来**创建请求然后根据构建者模式来构建 CreateTopicsRequest 请求参数**，最终会选择 **ControllerNodeProvider** 这个节点发起网络请求：

  
![](https://article-images.zsxq.com/FidKcqXI74Nwj0EpZXHbLdOJoc2w)

所以创建 Topic 操作**是需要 Controller 来执行的**：

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

## **4.7 发起网络请求**

这里可以从下面文章进行学习：

[【生产者源码分析第十一篇】图解 Kafka 源码之生产者流程总结篇](https://articles.zsxq.com/id_84na8x6zekwr.html)

[【原理分析系列第八篇】图解 Kafka 超高并发网络架构演进过程](https://articles.zsxq.com/id_de63boucthq8.html)

[【服务端Broker源码分析系列第七篇】图解Kafka源码之网络层请求处理全流程总结](https://articles.zsxq.com/id_xtza6mo3tkfb.html)

## **4.8 Controller 服务端接收客户端请求**

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

## **4.8.1 KafkaApis.handle(request)**

根据请求传递 API 来调用不同接口，[request.header.apiKey](http://request.header.apikey/) 匹配客户端传来的 CreateTopics

![](https://article-images.zsxq.com/FqfCbS9FNgrVgGn5tIZFTSUZxSiA)

## **4.8.2 handleCreateTopicsRequest()**

先来看下外面的方法 **maybeForwardToController()**

private def maybeForwardToController(

request: RequestChannel.Request, // 请求对象 RequestChannel.Request

handler: RequestChannel.Request => Unit // Request 处理器 handler

): Unit = {

// 用来处理 Controller 返回的响应消息，并将响应消息转发到客户端

def responseCallback(responseOpt: Option\[AbstractResponse\]): Unit = {

// 判断响应消息的类型

responseOpt match {

// 如果响应消息不为空，则转发响应消息到客户端

case Some(response) => requestHelper.sendForwardedResponse(request, response)

case None \=\>

// 否则说明 Controller 返回了不支持版本的异常信息，此时会关闭客户端连接。

requestHelper.closeConnection(request, Collections.emptyMap())

}

}

// 将请求转发给 Controller，如果转发过程出现错误，则会通过响应回调函数将错误信息返回给客户端。

metadataSupport.maybeForward(request, handler, responseCallback)

}

服务端处理创建 Topic 的请求，源码如下：

def handleCreateTopicsRequest(request: RequestChannel.Request): Unit = {

val zkSupport \= metadataSupport.requireZkOrThrow(KafkaApis.shouldAlwaysForward(request))

....

val createTopicsRequest \= request.body\[CreateTopicsRequest\]

val results \= new CreatableTopicResultCollection(createTopicsRequest.data.topics.size)

// 如果当前 Broker 不是属于 Controller 的话，则抛出异常。

if (!zkSupport.controller.isActive) {

createTopicsRequest.data.topics.forEach { topic =>

results.add(new CreatableTopicResult().setName(topic.name)

.setErrorCode(Errors.NOT\_CONTROLLER.code))

}

sendResponseCallback(results)

} else {

createTopicsRequest.data.topics.forEach { topic =>

results.add(new CreatableTopicResult().setName(topic.name))

}

// kafka 相关鉴权逻辑省略...

zkSupport.adminManager.createTopics(

createTopicsRequest.data.timeoutMs,

createTopicsRequest.data.validateOnly,

toCreate,

authorizedForDescribeConfigs,

controllerMutationQuota,

handleCreateTopicsResults)

}

}

步骤如下：

1.  判断当前 Broker 是否属于 Controller，CreateTopic 操作必须由 Controller 来进行，因为有可能客户端发起请求的时候 Controller 已经变更，所以如果不是的话则抛出异常。
2.  kafka 相关鉴权，这里忽略，后面抽空分析 kafka 鉴权机制。
3.  最后调用 zkSupport.adminManager.createTopics()。

这里我们主要来看下 「**第三步**」。

## **4.8.3 zkSupport.adminManager.createTopics()**

「**ZkAdminManager.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/ZkAdminManager.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/ZkAdminManager.scala)

/\*\*

\* Create topics and wait until the topics have been completely created.

\* The callback function will be triggered either when timeout, error or the topics are created.

\*/

def createTopics(timeout: Int, // 创建主题的超时时间，单位为毫秒。

validateOnly: Boolean, // 是否只进行验证而不实际创建主题。

toCreate: Map\[String, CreatableTopic\], // 要创建的主题的Map，其中键是主题名称，值是CreatableTopic对象，表示主题的配置信息。

includeConfigsAndMetadata: Map\[String, CreatableTopicResult\], // 用来包含主题的配置和元数据

controllerMutationQuota: ControllerMutationQuota, // 控制器变更配额，在创建主题时用于记录控制器变更的数量。

responseCallback: Map\[String, ApiError\] => Unit): Unit = { // 回调函数，用于在创建完成后接收结果。

// 1、获取可用的 broker，用于获取分配者

val brokers \= metadataCache.getAliveBrokers.map { b => kafka.admin.BrokerMetadata(b.id, Option(b.rack)) }

// 遍历传入的要创建的主题，并创建相应主题

val metadata \= toCreate.values.map(topic =>

try {

// 检查 Topic 是否存在，如果存在，则抛出 TopicExistsException。

if (metadataCache.contains(topic.name))

throw new TopicExistsException(s"Topic '${topic.name}' already exists.")

// 检查 Topic 配置中是否包含空值，如果包含，则抛出 InvalidRequestException。

val nullConfigs \= topic.configs.asScala.filter(\_.value == null).map(\_.name)

if (nullConfigs.nonEmpty)

throw new InvalidRequestException(s"Null value not supported for topic configs : ${nullConfigs.mkString(",")}")

// 如果同时设置了副本数/分区数和副本分配，则抛出异常

if ((topic.numPartitions != NO\_NUM\_PARTITIONS || topic.replicationFactor != NO\_REPLICATION\_FACTOR) && !topic.assignments().isEmpty) {

throw new InvalidRequestException("Both numPartitions or replicationFactor and replicasAssignments were set. " + "Both cannot be used at the same time.")

}

// 如果没有指定分区数和副本数，则使用默认参数

val resolvedNumPartitions \= if (topic.numPartitions == NO\_NUM\_PARTITIONS)

defaultNumPartitions else topic.numPartitions

val resolvedReplicationFactor \= if (topic.replicationFactor == NO\_REPLICATION\_FACTOR)

defaultReplicationFactor else topic.replicationFactor

// 如果用户指定了分区的副本列表(--partitions --replication-factor) ，则使用用户指定的副本列表，否则调用 assignReplicasToBrokers 方法为主题分区生成副本列表,当然这个 Broker 肯定是 Controller

val assignments \= if (topic.assignments.isEmpty) {

AdminUtils.assignReplicasToBrokers(

brokers, resolvedNumPartitions, resolvedReplicationFactor)

} else {

val assignments \= new mutable.HashMap\[Int, Seq\[Int\]\]

// Note: we don't check that replicaAssignment contains unknown brokers - unlike in add-partitions case,

// this follows the existing logic in TopicCommand

topic.assignments.forEach { assignment =>

assignments(assignment.partitionIndex) = assignment.brokerIds.asScala.map(a => a: Int)

}

assignments

}

trace(s"Assignments for topic $topic are $assignments ")

// 将主题配置添加到 Properties 对象中

val configs \= new Properties()

topic.configs.forEach(entry => configs.setProperty(entry.name, entry.value))

// 验证新主题的创建是否与 broker 规则兼容

adminZkClient.validateTopicCreate(topic.name, assignments, configs)

validateTopicCreatePolicy(topic, resolvedNumPartitions, resolvedReplicationFactor, assignments)

// 计算分区副本分配方式

maybePopulateMetadataAndConfigs(includeConfigsAndMetadata, topic.name, configs, assignments)

if (validateOnly) {

// 如果 validateOnly 为 true 的话，则仅验证该创建主题的方案是否正确，而不实际创建主题，所以此时不需要写入元数据

CreatePartitionsMetadata(topic.name, assignments.keySet)

} else {

// 把 topic 相关数据写入到 zk 中

controllerMutationQuota.record(assignments.size)

adminZkClient.createTopicWithAssignment(topic.name, configs, assignments, validate = false, config.usesTopicId)

populateIds(includeConfigsAndMetadata, topic.name)

CreatePartitionsMetadata(topic.name, assignments.keySet)

}

} catch {

....

}).toBuffer

// 2、如果timeout <= 0，validateOnly为true或没有主题可以创建，则立即返回

if (timeout <= 0 || validateOnly || !metadata.exists(\_.error.is(Errors.NONE))) {

val results \= metadata.map { createTopicMetadata =>

// ignore topics that already have errors

// 忽略已经有错误的主题

if (createTopicMetadata.error.isSuccess && !validateOnly) {

(createTopicMetadata.topic, new ApiError(Errors.REQUEST\_TIMED\_OUT, null))

} else {

(createTopicMetadata.topic, createTopicMetadata.error)

}

}.toMap

responseCallback(results)

} else {

// 3、else pass the assignments and errors to the delayed operation and set the keys

如果 timeout > 0 并且存在需要创建的主题，则根据超时时间创建延迟任务并将其添加到延迟主题里

val delayedCreate \= new DelayedCreatePartitions(timeout, metadata, this,

responseCallback)

val delayedCreateKeys \= toCreate.values.map(topic => TopicKey(topic.name)).toBuffer

// try to complete the request immediately, otherwise put it into the purgatory

topicPurgatory.tryCompleteElseWatch(delayedCreate, delayedCreateKeys)

}

}

步骤如下：

1.  获取可用的 broker，用于获取分配者
2.  遍历要创建的主题，这里关于主题、副本分配、分区的相关校验如下：
3.  检查 Topic 是否存在，或者检查 Topic 配置中是否包含空值，然后抛异常。
4.  检查如果同时设置了副本数（--replication-factor）/分区数（--partitions）和副本分配（--replica-assignment 参数），则抛出异常。
5.  如果用户指定了分区的副本列表(--partitions --replication-factor) ，则使用用户指定的副本列表，否则调用 assignReplicasToBrokers 方法为主题分区生成副本列表,当然这个 Broker 肯定是 Controller。
6.  将主题配置添加到 Properties 对象中。
7.  验证新主题的创建是否与 broker 规则兼容。
8.  计算分区副本分配方式。
9.  如果 validateOnly 为 true 的话，则仅验证该创建主题的方案是否正确，而不实际创建主题，所以此时不需要写入元数据。
10.  把 topic 相关数据写入到 zk 中。
11.  如果 timeout <= 0 或 validateOnly 为 true 或 没有主题可以创建，则立即返回；否则根据超时时间创建延迟任务并将其添加到延迟主题里。

## **4.8.4 写入 ZK 节点数据**

接着我们来看下 [](http://%20adminzkclient.createtopicwithassignment\(\)%20/)[adminZkClient.createTopicWithAssignment(](http://%20adminzkclient.createtopicwithassignment\(\)%20/)[)](http://%20adminzkclient.createtopicwithassignment\(\)%20/) 这个方法，看看都有哪些数据被写入到 zookeeper 中了。

def createTopicWithAssignment(topic: String,

config: Properties,

partitionReplicaAssignment: Map\[Int, Seq\[Int\]\],

validate: Boolean = true,

usesTopicId: Boolean = false): Unit = {

if (validate)

// 验证新主题的创建是否与 broker 规则兼容

validateTopicCreate(topic, partitionReplicaAssignment, config)

info(s"Creating topic $topic with configuration $config and initial partition " +

s"assignment $partitionReplicaAssignment")

// 1、将 topic 配置相关信息写入到 zk 中

zkClient.setOrCreateEntityConfigs(ConfigType.Topic, topic, config)

// 2、将 topic 分区相关信息写入 zk 中

writeTopicPartitionAssignment(topic, partitionReplicaAssignment.map { case (k, v) => k -> ReplicaAssignment(v) }, isUpdate = false, usesTopicId)

}

这里重点剖析下这两步。

### **4.8.4.1 写入 Topic 配置信息**

// 写入或更新实体配置

def setOrCreateEntityConfigs(rootEntityType: String, sanitizedEntityName: String, config: Properties) = {

// 写入配置数据 configData: 待设置的配置数据 SetDataResponse:设置操作响应

def set(configData: Array\[Byte\]): SetDataResponse = {

// 首先调用 SetDataRequest 请求，往 /config/topics/{TopicName} 写入数据，这里一般会返回 NONODE，没有该节点，则往该节点写入数据，如果该节点存在，则直接覆盖。

val setDataRequest \= SetDataRequest(ConfigEntityZNode.path(rootEntityType, sanitizedEntityName), configData, ZkVersion.MatchAnyVersion)

retryRequestUntilConnected(setDataRequest)

}

// 创建或更新配置数据

def createOrSet(configData: Array\[Byte\]): Unit = {

val path \= ConfigEntityZNode.path(rootEntityType, sanitizedEntityName)

try createRecursive(path, configData) // 尝试创建节点及其子节点（如果不存在）

catch {

// 节点已经存在时更新配置数据

case \_: NodeExistsException => set(configData).maybeThrow()

}

}

// 将配置属性编码为字节数组

val configData \= ConfigEntityZNode.encode(config)

// 写入配置数据

val setDataResponse \= set(configData)

// 根据结果匹配处理

setDataResponse.resultCode match {

// 节点不存在则写入数据，并且节点类型是 PERSISTENT 持久节点

case Code.NONODE => createOrSet(configData)

case \_ \=\> setDataResponse.maybeThrow()

}

}

1.  首先调用 SetDataRequest 请求，将主题的相关配置往 Zookeeper 节点 [/config/topics/{TopicName}](http://config/topics/%7BTopicName%7D) 写入数据，这里一般会返回 NONODE，没有该节点，则往该节点写入数据，如果该节点存在，则直接覆盖。
2.  节点不存在的话，则调用 createOrSet 方法，写入数据，并且节点类型是 PERSISTENT **持久节点**。

这里要写入的数据，也就是入参的时候传的 --config 的那些参数，这里的配置会覆盖默认配置。

### **4.8.4.2 写入 Topic 分区信息**

将已经分配好的「**主题**」、「**分区**」、「**副本列表**」等元数据写入到 「**Zookeeper**」节点 [/brokers/topics/{TopicName}](http://brokers/topics/%7BTopicName%7D) 中，节点类型是 PERSISTENT **持久节点**。

private def writeTopicPartitionAssignment(topic: String, replicaAssignment: Map\[Int, ReplicaAssignment\], isUpdate: Boolean, usesTopicId: Boolean = false): Unit = {

try {

将进程的完整副本分配映射到所述主题的当期已知分区。

val assignment \= replicaAssignment.map { case (partitionId, replicas) => (new TopicPartition(topic,partitionId), replicas) }.toMap

if (!isUpdate) {

// 非更新的话生成 TopicID

val topicIdOpt \= if (usesTopicId) Some(Uuid.randomUuid()) else None

// 在 ZooKeeper 中为主题创建副本分配节点（如果不存在）并写入副本分配信息

zkClient.createTopicAssignment(topic, topicIdOpt, assignment.map { case (k, v) => k -> v.replicas })

} else {

// 从 ZooKeeper 中获取主题 ID

val topicIds \= zkClient.getTopicIdsForTopics(Set(topic))

// 直接写入副本分配信息到副本分配节点中

zkClient.setTopicAssignment(topic, topicIds.get(topic), assignment)

}

debug("Updated path %s with %s for replica assignment".format(TopicZNode.path(topic), assignment))

} catch {

case \_: NodeExistsException => throw new TopicExistsException(s"Topic '$topic' already exists.")

case e2: Throwable => throw new AdminOperationException(e2.toString)

}

}

def createTopicAssignment(topic: String, topicId: Option\[Uuid\], assignment: Map\[TopicPartition, Seq\[Int\]\]): Unit = {

val persistedAssignments \= assignment.map { case (k, v) => k -> ReplicaAssignment(v) }

// TopicZNode.path(topic) 这里的节点为 /brokers/topics/{TopicName}

createRecursive(TopicZNode.path(topic), TopicZNode.encode(topicId, persistedAssignments))

}

跟「**Zookeeper**」交互的代码，这里封装了很多与 Zookeeper 的交互方法，如下：

「**ZookeeperClient.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/zookeeper/ZookeeperClient.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/zookeeper/ZookeeperClient.scala)

![](https://article-images.zsxq.com/Fnkt_qIVF1S14CtBoKauTU3EOGfM)

## **4.8.5 Controller 监听节点变更**

我们在前面讲「**Controller**」时候讲过它会监听 Zookeeper 的一些节点，在上面的流程中已经将信息写入到 Zookeeper 中， 当 「**Controller**」监听 [/brokers/topics/{topicName}](http://brokers/topics/%7BtopicName%7D) 节点有变化时就会通知 Controller 做出相应的处理，这里主要是**通知 Broker 将分区写入磁盘中**。

最终会调用 [KafkaContoller.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala) 的 processTopicChange 方法，源码如下：

private def processTopicChange(): Unit = {

// 如果非 Contorller，直接返回

if (!isActive) return

// 从 zk 的 /brokers/topics 节点中获取全量 topic 列表，

val topics \= zkClient.getAllTopicsInCluster(true)

// 找出新增的 topic

val newTopics \= topics -- controllerContext.allTopics

// 找出删除的 topic

val deletedTopics \= controllerContext.allTopics.diff(topics)

// 更新 Controller 元数据

controllerContext.setAllTopics(topics)

// 注册 partition 相关的 hook

registerPartitionModificationsHandlers(newTopics.toSeq)

// 从 zk 的 /brokers/topics/{topicName} 节点中获取给定主题的副本分配并保存在内存中。

val addedPartitionReplicaAssignment \= zkClient.getFullReplicaAssignmentForTopics(newTopics)

// 删除不再存在的 topic

deletedTopics.foreach(controllerContext.removeTopic)

// 更新新 topic 的 partition replica 分配

addedPartitionReplicaAssignment.foreach {

case (topicAndPartition, newReplicaAssignment) => controllerContext.updatePartitionFullReplicaAssignment(topicAndPartition, newReplicaAssignment)

}

info(s"New topics: \[$newTopics\], deleted topics: \[$deletedTopics\], new partition replica assignment " +

s"\[$addedPartitionReplicaAssignment\]")

// 如果有新的 partition 创建，则执行 onNewPartitionCreation 方法

if (addedPartitionReplicaAssignment.nonEmpty)

onNewPartitionCreation(addedPartitionReplicaAssignment.keySet)

}

步骤如下：

1.  从 zk 的 [/brokers/topics](http://brokers/topics) 节点中获取全量 topic 列表，跟当前 Broker 内存中所有 controllerContext.allTopics 的差异，可以找出哪些是新增的 Topic，哪些 Topic 在 zk 上被删除了。
2.  从 zk 的 [/brokers/topics/{topicName}](http://brokers/topics/%7BtopicName%7D%20) [](http://brokers/topics/%7BtopicName%7D%20)节点中获取给定主题的副本分配并保存在内存中，如下图：![](https://article-images.zsxq.com/Fj5RMIp3yv7fbmXszz_hVHsTzXWN)
3.  执行 onNewPartitionCreation，分区状态开始流转。

## **4.8.6 onNewPartitionCreation 状态流转**

private def onNewPartitionCreation(newPartitions: Set\[TopicPartition\]): Unit = {

info(s"New partition creation callback for ${newPartitions.mkString(",")}")

// 1、将待创建的分区状态流转为 NewPartition

partitionStateMachine.handleStateChanges(newPartitions.toSeq, NewPartition)

// 2、将待创建的副本状态流转 NewReplica

replicaStateMachine.handleStateChanges(controllerContext.replicasForPartition(newPartitions).

toSeq, NewReplica)

// 3、将分区状态从刚刚的 NewPartition 流转为 OnlinePartition

partitionStateMachine.handleStateChanges(

newPartitions.toSeq,

OnlinePartition,

Some(OfflinePartitionLeaderElectionStrategy(false))

)

// 4、将副本状态从刚刚的 NewReplica 流转为 OnlineReplica，更新下内存

replicaStateMachine.handleStateChanges(controllerContext.replicasForPartition(newPartitions).

toSeq, OnlineReplica)

}

该方法主要用来处理**分区和副本状态流转的**，步骤如下：

1.  将待创建的分区状态流转为 NewPartition。
2.  将待创建的副本状态流转 NewReplica。
3.  将分区状态从刚刚的 NewPartition 流转为 OnlinePartition。
4.  获取 leaderIsrAndControllerEpochs，**为主题分区选择 Leader，选择策略很简单，取分区副本列表中第一个Broker 节点作为 Leader 副本**。
5.  向 zk 中写入 /brokers/topics/{topicName}/partitions/ 持久节点，无数据。
6.  向 zk 中写入 /brokers/topics/{topicName}/partitions/{分区号} 持久节点，无数据。
7.  向 zk 中写入 /brokers/topics/{topicName}/partitions/{分区号}/state 持久节点，数据为 leaderIsrAndCon。![](https://article-images.zsxq.com/FvMaB5qYD8JW7g2Q9UaR56khCPtH)
8.  向副本所属 Broker 发送 leaderAndIsrRequest 请求。
9.  向所有 Broker 发送 UPDATE\_METADATA 请求。
10.  将副本状态从刚刚的 NewReplica 流转为 OnlineReplica，更新下内存。

这里关于分区状态机和副本状态机相关的知识就不展开了，后面会有单独篇章进行深度剖析。

## **4.8.7 Broker 收到 LeaderAndIsrRequest 请求处理流程**

上面步骤中说到向副本所属 Broker 发送 LeaderAndIsrRequest 请求，**转那么到底做了什么呢？其实就是创建本地 Log**。

如果对 KafkaApis 不了解的可以直接移步到：[【服务端Broker源码分析系列第六篇】图解Kafka源码之 KafkaApis 详解](https://articles.zsxq.com/id_k4o72eh3pai3.html) 进行学习。

这里直接定位到 [KafkaApis#](http://kafkaapis/#handleLeaderAndIsrRequest)[handleLeaderAndIsrRequest](http://kafkaapis/#handleLeaderAndIsrRequest) 方法。

  
![](https://article-images.zsxq.com/FmoFkP1wq5QwoCJO3kTP2H0QKZVW)

源码太多了，但是不是创建 Topic 的重点，这里就通过图片直接指向，后面会有单独篇章进行深度剖析。

![](https://article-images.zsxq.com/FomgVnCU2cXtCYDqkMfZ6aKdI3sY)

![](https://article-images.zsxq.com/FhLKYD_9rHZpBYGdpol5l7WkKdmT)

![](https://article-images.zsxq.com/FkAX_10MAlajwAqLeIgWFcXlVTOC)

![](https://article-images.zsxq.com/FrEhUAB3TLacBKk_ys-iV0nbb-rY)

![](https://article-images.zsxq.com/FuhOQfz51v-BLUwlzwQ51be8AWxW)

最后来剖析下 [LogManager#getOrCreateLog](http://logmanager/#getOrCreateLog) 方法，源码如下：

// 用于获取或创建指定主题分区的日志对象

def getOrCreateLog(topicPartition: TopicPartition, isNew: Boolean = false, isFuture: Boolean = false): Log = {

// 加锁，确保创建和删除日志的安全性

logCreationOrDeletionLock synchronized {

// 获取指定主题分区的日志，如果不存在则进行创建

getLog(topicPartition, isFuture).getOrElse {

// create the log if it has not already been created in another thread

// 如果不是新建日志且存在离线日志目录，则抛出异常

if (!isNew && offlineLogDirs.nonEmpty)

throw new KafkaStorageException(s"Can not create log for $topicPartition because log directories ${offlineLogDirs.mkString(",")} are offline")

// 获取日志目录

val logDirs: List\[File\] = {

val preferredLogDir \= preferredLogDirs.get(topicPartition)

// 如果是 Future，则需要指定首选日志目录

if (isFuture) {

if (preferredLogDir == null)

throw new IllegalStateException(s"Can not create the future log for $topicPartition without having a preferred log directory")

else if (getLog(topicPartition).get.parentDir == preferredLogDir)

throw new IllegalStateException(s"Can not create the future log for $topicPartition in the current log directory of this partition")

}

// 如果存在首选日志目录，则使用首选日志目录创建日志目录列表

if (preferredLogDir != null)

List(new File(preferredLogDir))

else

// 否则使用下一个可用的日志目录

nextLogDirs()

}

// 根据是否为 Future 来获取不同日志目录名称

val logDirName \= {

if (isFuture)

Log.logFutureDirName(topicPartition)

else

Log.logDirName(topicPartition)

}

// 遍历日志目录列表，找到可以成功创建日志目录的路径，如果找不到则抛出异常

val logDir \= logDirs

.iterator // to prevent actually mapping the whole list, lazy map

.map(createLogDirectory(\_, logDirName))

.find(\_.isSuccess)

.getOrElse(Failure(new KafkaStorageException("No log directories available. Tried " + logDirs.map(\_.getAbsolutePath).mkString(", "))))

.get // If Failure, will throw

// 获取日志配置信息

val config \= fetchLogConfig(topicPartition.topic)

// 创建日志对象

val log \= Log(

dir = logDir,

config = config,

logStartOffset = 0L,

recoveryPoint = 0L,

maxProducerIdExpirationMs = maxPidExpirationMs,

producerIdExpirationCheckIntervalMs = LogManager.ProducerIdExpirationCheckIntervalMs,

scheduler = scheduler,

time = time,

brokerTopicStats = brokerTopicStats,

logDirFailureChannel = logDirFailureChannel,

keepPartitionMetadataFile = keepPartitionMetadataFile)

// 如果是 Future，则将日志对象添加到 futureLogs 中，否则添加到currentLogs中

if (isFuture)

futureLogs.put(topicPartition, log)

else

currentLogs.put(topicPartition, log)

info(s"Created log for partition $topicPartition in $logDir with properties " + s"{${config.originals.asScala.mkString(", ")}}.")

// Remove the preferred log dir since it has already been satisfied

preferredLogDirs.remove(topicPartition)

log

}

}

}

在 Kafka 的 Broker 节点中，每个主题的分区数据被保存在一个日志目录（Log Directory）中，这个目录下包含了多个分片日志文件（Log Segment）。

当一个日志目录已经满了，或者存储容量达到了上限，Kafka 使用 FutureLogs 机制来进行跨路径迁移，即将目录下的所有分片日志文件迁移到一个新的目录下。

FutureLogs 的工作原理是：对于迁移目录中的每个日志文件，Kafka 都会创建一个临时的日志文件，将该日志文件中未写入的部分以追加的方式写入到临时日志文件中。当临时文件的大小达到一定阈值或者日志文件关闭时，Kafka 就会打开一个新的日志文件，并将临时文件中的数据刷盘到新的日志文件中。当所有的日志文件迁移完毕后，原来的日志目录将被删除，所有新产生的日志都将被写入到新的目录中。

因此，FutureLogs 机制主要用于管理日志文件的迁移，以保证 Broker 节点中的数据存储容量不会达到上限，并且能够实现数据的平滑迁移，避免数据的丢失和IO影响。

至此，创建 Topic 的流程就剖析完毕了。

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头通过「**命令行的方式**」，引出 Kafka Topic 创建的入口，底层调用**封装在 Kafka Core 包中的 TopicCommand 类**。

2、接着带大家分析了 「**创建 Topic 元数据要求**」。

4、接着带大家深度剖析了 「**创建 Topic**」的源码全流程，Kafka 主题的创建依赖于 KafkaAdminClient 跟Controller 服务器进行 RPC 通信完成主题创建流程。在此之前首先需要跟任意 broker 服务器通信得到 Kafka 的metadata 数据，来获得 ControllerNode 的信息，然后再跟 ControllerNode 进行通信。

这里有几个小问题，欢迎大家留言评论。

1.  删除 Topic 时是在什么时候往 /admin/delete\_topics 写入节点的?
2.  又是什么时候真正执行删除 Topic 磁盘日志的？
3.  Controller 在发送 StopReplicaRequest 请求时是通知所有的 Broker 还是只通知跟被删除 Topic 有关联的Broker，为什么？
4.  在删除 Topic 过程中如果有 Broker 不在线或者删除失败会如何处理?

下篇我们来深度剖析「**Topic 删除请求处理流程**」，大家期待，我们下期见。