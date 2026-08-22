大家好，我是**华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了「**发送网络 I/O 的 Sender 线程的架构设计**」，消息先被暂存然后调用网络I/O组件进行发送，今天主要聊聊 「**真正进行网络 I/O 的 NetworkClient 的架构设计**」，深度剖析下消息是如何被发送出去的。

![](https://article-images.zsxq.com/Fq_VDNgvzpaV9-pIHAo--81D9ejG)

##   
**01 总体概述**

继续通过「**场景驱动**」的方式，来看看消息是如何从客户端发送出去的。

在上篇中，我们知道了消息被 Sender 子线程先**暂存到 KafkaChannel 的 Send 字段中，然后调用 NetworkClient#client.poll() 进行真正发送出去**，如下图所示「**6-11步**」：

![](https://article-images.zsxq.com/FmEcVGpSpG-CMJYg7kjG1-IasE39)

  
**NetworkClient 为**「**生产者**」、「**消费者**」、「**服务端**」**等上层业务提供了网络I/O的能力**。在 NetworkClient 内部使用了前面介绍的 Kafka 对 NIO 的封装组件，同时做了一定的封装，最终实现了网络I/O能力。**NetworkClient 不仅仅用于客户端与服务端的通信，也用于服务端之间的通信。**

接下来我们就来看看，「**NetworkClient 网络I/O组件的架构实现以及发送处理流程**」，为了方便大家理解，所有的源码只保留骨干。

##   
**02 NetworkClient 架构设计**

**NetworkClient 类是 KafkaClient 接口的实现类，**它内部的重要字段有「**Selectable**」、「**InflightRequest**」以及内部类 「**MetadataUpdate**」。

github 源码地址如下：

  
[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/NetworkClient.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/NetworkClient.java)

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/InFlightRequests.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/InFlightRequests.java)

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/ClusterConnectionStates.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/ClusterConnectionStates.java)

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/ClientRequest.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/ClientRequest.java)

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/ClientResponse.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/clients/ClientResponse.java)

## **02.1 关键字段**

public class NetworkClient implements KafkaClient {

// 状态枚举值

private enum State {

ACTIVE,

CLOSING,

CLOSED

}

/\* the selector used to perform network i/o \*/

// 用于执行网络 I/O 的选择器

private final Selectable selector;

// Metadata元信息的更新器, 它可以尝试更新元信息

private final MetadataUpdater metadataUpdater;

/\* the state of each node's connection \*/

// 管理集群所有节点连接的状态

private final ClusterConnectionStates connectionStates;

/\* the set of requests currently being sent or awaiting a response \*/

// 当前正在发送或等待响应的请求集合

private final InFlightRequests inFlightRequests;

/\* the socket send buffer size in bytes \*/

// 套接字发送数据的缓冲区的大小（以字节为单位）

private final int socketSendBuffer;

/\* the socket receive size buffer in bytes \*/

// 套接字接收数据的缓冲区的大小（以字节为单位）

private final int socketReceiveBuffer;

/\* the client id used to identify this client in requests to the server \*/

// 表示客户端id，标识客户端身份

private final String clientId;

/\* the current correlation id to use when sending requests to servers \*/

// 向服务器发送请求时使用的当前关联 ID

private int correlation;

/\* default timeout for individual requests to await acknowledgement from servers \*/

// 单个请求等待服务器确认的默认超时

private final int defaultRequestTimeoutMs;

/\* time in ms to wait before retrying to create connection to a server \*/

// 重连的退避时间

private final long reconnectBackoffMs;

/\*\*

\* True if we should send an ApiVersionRequest when first connecting to a broker.

\* 是否需要与 Broker 端的版本协调，默认为 true

\* 如果为 true 当第一次连接到一个 broker 时，应当发送一个 version 的请求，用来得知 broker 的版本， 如果为 false 则不需要发送 version 的请求。

\*/

private final boolean discoverBrokerVersions;

// broker 端版本

private final ApiVersions apiVersions;

// 存储着要发送的版本请求，key 为 nodeId，value 为构建请求的 Builder

private final Map<String, ApiVersionsRequest.Builder> nodesNeedingApiVersionsFetch = new HashMap<>();

// 取消的请求集合

private final List<ClientResponse> abortedSends = new LinkedList<>();

从该类属性字段来看比较多，这里说几个关键字段：

1.  **selector**：Kafka 自己封装的 Selector，该选择器负责监听「**网络I/O事件**」、「**网络连接**」、「**读写操作**」。[图解 Kafka 源码网络层实现机制之 Selector 多路复用器](https://t.zsxq.com/0brYsHYhU)。
2.  **metadataUpdater**：NetworkClient 的内部类，主要用来实现Metadata元信息的更新器, 它可以尝试更新元信息。
3.  **connectionStates**：管理集群所有节点连接的状态，底层使用 Map<nodeid, NodeConnectionState\>实现，NodeConnectionState 枚举值表示连接状态，并且记录了最后一次连接的时间戳。
4.  **inFlightRequests**：用来保存当前正在发送或等待响应的请求集合。
5.  **socketSenderBuffer**：表示套接字发送数据的缓冲区的大小。
6.  **socketReceiveBuffer**：表示套接字接收数据的缓冲区的大小。
7.  **clientId**：表示客户端id，标识客户端身份。
8.  **reconnectBackoffMs**：表示重连的退避事件，为了防止短时间内大量重连造成的网络压力，设计了这么一个时间段，在此时间段内不得重连。

## **02.2 关键方法**

NetworkClient 类的方法也不少，这里针对关键方法逐一讲解下。

### **02.2.1 ready()**

/\*\*

\* Begin connecting to the given node, return true if we are already connected and ready to send to that node.

\*

\* @param node The node to check

\* @param now The current timestamp

\* @return True if we are ready to send to the given node

\*/

@Override

public boolean ready(Node node, long now) {

// 空节点

if (node.isEmpty())

throw new IllegalArgumentException("Cannot connect to empty node " + node);

// 1、判断节点是否准备好发送请求

if (isReady(node, now))

return true;

// 2、判断节点连接状态

if (connectionStates.canConnect(node.idString(), now))

// if we are interested in sending to a node and we don't have a connection to it, initiate one

// 3、初始化连接，但此时不一定连接成功了

initiateConnect(node, now);

return false;

}

/\*\*

\* Check if the node with the given id is ready to send more requests.

\* @param node The node

\* @param now The current time in ms

\* @return true if the node is ready

\*/

@Override

public boolean isReady(Node node, long now) {

// if we need to update our metadata now declare all requests unready to make metadata requests first priority

// 当发现正在更新元数据时，会禁止发送请求 && 当连接没有创建完毕或者当前发送的请求过多时，也会禁止发送请求

return !metadataUpdater.isUpdateDue(now) && canSendRequest(node.idString(), now);

}

/\*\*

\* Are we connected and ready and able to send more requests to the given connection?

\* 检测连接状态、发送请求是否过多

\* @param node The node

\* @param now the current timestamp

\*/

private boolean canSendRequest(String node, long now) {

// 三个条件必须都满足

return connectionStates.isReady(node, now) && selector.isChannelReady(node) &&

inFlightRequests.canSendMore(node);

}

该方法表示**某个节点是否准备好并可以发送请求，主要做了三件事：**

1.  先判断节点是否已经准备好连接并接收请求了，需要满足以下四个条件：
2.  !metadataUpdater.isUpdateDue(now)：不能是正在更新元数据的状态，且元数据不能过期。
3.  canSendRequest(node.idString(), now)：此处有3个条件。(1)、客户端和 node 连接是否处于 ready 状态；(2)、客户端和 node 的 channel 是否建立好； (3)、inFlightRequests 中对应的节点是否可以接收更多的请求。
4.  如果连接好返回 true 表示准备好，如果**没有准备好接收请求**，则会尝试与对应的 Node 连接，此处也需要满足两个条件：
5.  首先连接必须是 isDisconnected，不能是 connecteding 状态，即客户端与服务端的连接状态是**没有连接上**。
6.  两次重试之间时间差要大于重试退避时间，**目的就是为了避免网络拥塞，防止重连过于频繁造成网络压力过大**。
7.  这块下面会讲解。
8.  最后初始化连接。

### **02.2.2 initiateConnect()**

/\*\*

\* 创建连接

\* Initiate a connection to the given node

\* @param node the node to connect to

\* @param now current time in epoch milliseconds

\*/

private void initiateConnect(Node node, long now) {

String nodeConnectionId \= node.idString();

try {

// 1、更新连接状态为正在连接

connectionStates.connecting(nodeConnectionId, now, node.host(), clientDnsLookup);

// 获取连接地址

InetAddress address \= connectionStates.currentAddress(nodeConnectionId);

log.debug("Initiating connection to node {} using address {}", node, address);

// 2、调用 selector 尝试异步进行连接，后续通过selector.poll进行监听事件就绪

selector.connect(nodeConnectionId,

new InetSocketAddress(address, node.port()),

this.socketSendBuffer,

this.socketReceiveBuffer);

} catch (IOException e) {

log.warn("Error connecting to node {}", node, e);

// Attempt failed, we'll try again after the backoff

connectionStates.disconnected(nodeConnectionId, now);

// Notify metadata updater of the connection failure

metadataUpdater.handleServerDisconnect(now, nodeConnectionId, Optional.empty());

}

}

该方法主要是**进行初始化连接**，做了两件事：

1.  调用 connectionStates.connecting() 更新连接状态为正在连接。
2.  调用 selector.connect() 异步发起连接，此时不一定连接上了，后续 Selector.poll() 会监听连接是否准备好并完成连接，如果连接成功，则会将 ConnectionState 设置为 CONNECTED。

当连接准备好后，接下来我们来看下发送相关的方法。

### **02.2.3 send()、doSend()**

/\*\*

\* ClientRequest 是客户端的请求，封装了 requestBuilder

\*/

public final class ClientRequest {

// 节点地址

private final String destination;

// ClientRequest 中通过 requestBuilder 给不同类型的请求设置不同的请求内容

private final AbstractRequest.Builder<?> requestBuilder;

// 请求头的 correlationId

private final int correlationId;

// 请求头的 clientid

private final String clientId;

// 创建时间

private final long createdTimeMs;

// 是否需要进行响应

private final boolean expectResponse;

// 请求的超时时间

private final int requestTimeoutMs;

// 回调函数 用来处理响应

private final RequestCompletionHandler callback;

......

}

/\*\*

\* Queue up the given request for sending. Requests can only be sent out to ready nodes.

\* @param request The request

\* @param now The current timestamp

\* 发送请求，这个方法 生产者和消费者都会调用，其中 ClientRequest 表示客户端的请求。

\*/

@Override

public void send(ClientRequest request, long now) {

doSend(request, false, now);

}

// 检测请求版本是否支持，如果支持则发送请求

private void doSend(ClientRequest clientRequest, boolean isInternalRequest, long now) {

// 确认是否活跃

ensureActive();

// 目标节点id

String nodeId \= clientRequest.destination();

// 是否是 NetworkClient 内部请求 这里为 false

if (!isInternalRequest) {

// 检测是否可以向指定 Node 发送请求，如果还不能发送请求则抛异常

if (!canSendRequest(nodeId, now))

throw new IllegalStateException("Attempt to send a request to node " + nodeId + " which is not ready.");

}

AbstractRequest.Builder<?> builder = clientRequest.requestBuilder();

try {

// 检测版本

NodeApiVersions versionInfo \= apiVersions.get(nodeId);

// ... 忽略

// builder.build()是 ProduceRequest.Builder，结果是ProduceRequest

// 调用 doSend 方法

doSend(clientRequest, isInternalRequest, now, builder.build(version));

} catch (UnsupportedVersionException unsupportedVersionException) { log.debug("Version mismatch when attempting to send {} with correlation id {} to {}", builder, clientRequest.correlationId(), clientRequest.destination(), unsupportedVersionException);

// 请求的版本不协调，那么生成 clientResponse

ClientResponse clientResponse \= new ClientResponse(clientRequest.makeHeader(builder.latestAllowedVersion()),

clientRequest.callback(), clientRequest.destination(), now, now,

false, unsupportedVersionException, null, null);

// 添加到 abortedSends 集合里

abortedSends.add(clientResponse);

}

}

/\*\*

\* isInternalRequest 表示发送前是否需要验证连接状态，如果为 true 则表示客户端已经确定连接是好的

\* request表示请求体

\*/

private void doSend(ClientRequest clientRequest, boolean isInternalRequest, long now, AbstractRequest request) {

// 目标节点地址

String destination \= clientRequest.destination();

// 生成请求头

RequestHeader header \= clientRequest.makeHeader(request.version());

if (log.isDebugEnabled()) {

log.debug("Sending {} request with header {} and timeout {} to node {}: {}",

clientRequest.apiKey(), header, clientRequest.requestTimeoutMs(), destination, request);

}

// 1、构建 NetworkSend 对象 结合请求头和请求体，序列化数据，保存到 NetworkSend

Send send \= request.toSend(destination, header);

// 2、构建 inFlightRequest 对象 保存了发送前的所有信息

InFlightRequest inFlightRequest \= new InFlightRequest(

clientRequest,

header,

isInternalRequest,

request,

send,

now);

// 3、把 inFlightRequest 加入 inFlightRequests 集合里

this.inFlightRequests.add(inFlightRequest);

// 4、调用 Selector 异步发送数据，并将 send 和对应 kafkaChannel 绑定起来，并开启该 kafkaChannel 底层 socket 的写事件，等待下一步真正的网络发送

selector.send(send);

}

@Override

public boolean active() {

// 判断状态是否是活跃的

return state.get() == State.ACTIVE;

}

// 确认是否活跃

private void ensureActive() {

if (!active())

throw new DisconnectException("NetworkClient is no longer active, state is " + state);

}

从上面源码可以看出此处发送并不是**真正的网络发送，而是先将数据发送到缓存中**。

1.  首先最外层是 send() ，里面调用 doSend() 。
2.  这里的 doSend() 主要的作用是**判断 inFlightRequests 集合上对应的节点是不是能发送请求** ，需要满足三个条件：
3.  客户端和 node 连接是否处于 ready 状态。
4.  客户端和 node 的 channel 是否建立好。
5.  inFlightRequests 集合中对应的节点是否可以接收更多的请求。
6.  最后再次调用另一个 doSend()，用来**最终的请求发送到缓存中**。步骤如下：
7.  构建 NetworkSend 对象 结合请求头和请求体，序列化数据，保存到 NetworkSend。
8.  构建 inFlightRequest 对象。
9.  把 inFlightRequest 加入 inFlightRequests 集合里等待响应。
10.  调用Selector异步发送数据，并将 send 和对应 kafkaChannel 绑定起来，并开启该 kafkaChannel 底层 socket 的写事件，等待下一步真正的网络发送。

综上可以得出这里的发送过程其实是把要发送的请求先封装成 inFlightRequest，然后放到 inFlightRequests 集合里，然后放到对应 channel 的字段 NetworkSend 里缓存起来。

总之，**这里的发送过程就是为了下一步真正的网络I/O发送而服务的**。

接下来看下真正网络发送的方法。

### **02.2.4 poll()**

该方法执行网络发送并把响应结果「**pollSelectionKeys 的各种读写**」做各种状态处理，此处是通过调用 handleXXX() 方法进行处理的，代码如下：

/\*\*

\* Do actual reads and writes to sockets.

\* @param timeout The maximum amount of time to wait (in ms) for responses if there are none immediately,

\* must be non-negative. The actual timeout will be the minimum of timeout, request timeout and

\* metadata timeout

\* @param now The current time in milliseconds

\* @return The list of responses received

\*/

@Override

public List<ClientResponse> poll(long timeout, long now) {

// 确认是否活跃

ensureActive();

// 取消发送是否为空

if (!abortedSends.isEmpty()) {

// If there are aborted sends because of unsupported version exceptions or disconnects,

// handle them immediately without waiting for Selector#poll.

List<ClientResponse> responses = new ArrayList<>();

handleAbortedSends(responses);

completeResponses(responses);

return responses;

}

// 1、尝试更新元数据

long metadataTimeout \= metadataUpdater.maybeUpdate(now);

try {

// 2、执行网络 I/O 操作，真正读写发送的地方，如果客户端的请求被完整的处理过了，会加入到completeSends 或 complteReceives 集合中

this.selector.poll(Utils.min(timeout, metadataTimeout, defaultRequestTimeoutMs));

} catch (IOException e) {

log.error("Unexpected error during I/O", e);

}

// process completed actions

long updatedNow \= this.time.milliseconds();

// 响应结果集合：真正的读写操作, 会生成responses

List<ClientResponse> responses = new ArrayList<>();

// 3、完成发送的handler，处理 completedSends 集合

handleCompletedSends(responses, updatedNow);

// 4、完成接收的handler，处理 completedReceives 队列

handleCompletedReceives(responses, updatedNow);

// 5、断开连接的handler，处理 disconnected 列表

handleDisconnections(responses, updatedNow);

// 6、处理连接的handler，处理 connected 列表

handleConnections();

// 7、处理版本协调请求（获取api版本号） handler

handleInitiateApiVersionRequests(updatedNow);

// 8、超时连接的handler，处理超时连接集合

handleTimedOutConnections(responses, updatedNow);

// 9、超时请求的handler，处理超时请求集合

handleTimedOutRequests(responses, updatedNow);

// 10、完成响应回调

completeResponses(responses);

return responses;

}

这里的步骤比较多，我们按照先后顺序讲解下。

1.  尝试更新元数据。
2.  调用 Selector.poll() 执行真正网络 I/O 操作，可以点击查看 [图解 Kafka 源码网络层实现机制之 Selector 多路复用器](https://t.zsxq.com/0brYsHYhU) 主要操作以下3个集合。
3.  connected集合：已经完成连接的 Node 节点集合。
4.  completedReceives集合：接收完成的集合，即 KafkaChannel 上的 NetworkReceive 写满后会放入这个集合里。
5.  completedSends集合：发送完成的集合，即 channel 上的 NetworkSend 读完后会放入这个集合里。
6.  调用 handleCompletedSends() 处理 completedSends 集合。
7.  调用 handleCompletedReceives() 处理 completedReceives 队列。
8.  调用 handleDisconnections() 处理与 Node 断开连接的请求。
9.  调用 handleConnections() 处理 connected 列表。
10.  调用 handleInitiateApiVersionRequests() 处理版本号请求。
11.  调用 handleTimedOutConnections() 处理连接超时的 Node 集合。
12.  调用 handleTimedOutRequests() 处理 inFlightRequests 集合中的超时请求，并修改其状态。
13.  调用 completeResponses() 完成每个消息自定义的响应回调。

接下来看下第 **3~9** 步骤的方法实现。

### **02.2.5 handleCompletedSends()**

当 NetworkClient 发送完请求后，就会调用 **handleCompletedSends** 方法，表示请求已经发送到 Broker 端了。

/\*\*

\* Handle any completed request send. In particular if no response is expected consider the request complete.

\* @param responses The list of responses to update

\* @param now The current time

\*/

private void handleCompletedSends(List<ClientResponse> responses, long now) {

// if no response is expected then when the send is completed, return it

// 1、遍历 completedSends 发送完成的请求集合，通过调用 Selector 获取从上一次 poll 开始的请求

for (Send send : this.selector.completedSends()) {

// 2、从 inFlightRequests 集合获取该 Send 关联对应 Node 的队列取出最新的请求，但并没有从队列中删除，取出后判断这个请求是否期望得到响应

InFlightRequest request \= this.inFlightRequests.lastSent(send.destination());

// 3、是否需要响应, 如果不需要响应,当Send请求完成时,就直接返回.还是有request.completed生成的ClientResponse对象

if (!request.expectResponse) {

// 4、如果不需要响应就取出 inFlightRequests 中该 Sender 关联对应 Node 的 inFlightRequest，即提取最新的请求

this.inFlightRequests.completeLastSent(send.destination());

// 5、调用 completed() 生成 ClientResponse,第一个参数为null,表示没有响应内容,把请求添加到 Responses 集合

responses.add(request.completed(null, now));

}

}

}

该方法主要用来在客户端发送请求后，**对响应结果进行处理**，做了五件事：

1.  遍历 seletor 中的 completedSends 集合，逐个处理完成的 Send 对象。
2.  从 inFlightRequests 集合获取该 Send 关联对应 Node 的队列中第一个元素，但并没有从队列中删除，取出后判断这个请求是否期望得到响应。
3.  判断是否需要响应。
4.  如果不需要响应就删除 inFlightRequests 中该 Sender 关联对应 Node 的 inFlightRequest，对于 Kafka 来说，有些请求是不需要响应的，对于发送完不用考虑是否发送成功的话，就构建 callback 为 null 的 Response 对象。
5.  通过 InFlightRequest.completed()，生成 ClientResponse，第一个参数为 null 表示没有响应内容，最后把 ClientResponse 添加到 Responses 集合。

从上面源码可以看出，「**completedSends**」集合与「**InflightRequests**」集合协作的关系。

但是这里有个问题：**如何保证从 Selector 返回的请求，就是对应到 InflightRequests 集合队列的最新的请求呢？**

  
**completedSends** 集合保存的是最近一次调用 poll() 方法中发送成功的请求「**发送成功但还没有收到响应的请求集合**」。而 **InflightRequests** 集合存储的是已经发送但还没收到响应的请求。**每个请求发送都需要等待前面的请求发送完成，**这样就能保证同一时间只有一个请求正在发送，因为 Selector 返回的请求是从上一次 poll 开始的，这样就对上了。

「**completedSends**」的元素对应着「**InflightRequests**」集合里对应队列的最后一个元素， 如下图所示：

![](https://article-images.zsxq.com/FrbBe6uyZ9YKH_B_V7PDp0B4SdmY)

### **02.2.6 handleCompletedReceives()**

当 NetworkClient 收到响应时，就会调用 **handleCompletedReceives** 方法。

/\*\*

\* Handle any completed receives and update the response list with the responses received.

\* @param responses The list of responses to update

\* @param now The current time

\* 处理 CompletedReceives 队列,根据返回的响应信息实例化 ClientResponse ,并加到响应集合里

\*/

private void handleCompletedReceives(List<ClientResponse> responses, long now) {

// 1、遍历 CompletedReceives 响应集合，通过 Selector 返回未处理的响应

for (NetworkReceive receive : this.selector.completedReceives()) {

// 2、获取发送请求的 Node id

String source \= receive.source();

// 3、从 inFlightRequests 集合队列获取已发送请求「最老的请求」并删除（从 inFlightRequests 删除，因为inFlightRequests 存储的是未收到请求响应的 ClientRequest，现在请求已经有响应了，就不需要保存了）

InFlightRequest req \= inFlightRequests.completeNext(source);

// 4、解析响应，并且验证响应头，生成 responseStruct 实例

Struct responseStruct \= parseStructMaybeUpdateThrottleTimeMetrics(receive.payload(), req.header,throttleTimeSensor, now);

// 生成响应体

AbstractResponse response \= AbstractResponse.parseResponse(req.header.apiKey(), responseStruct, req.header.apiVersion());

....

// If the received response includes a throttle delay, throttle the connection.

// 流控处理

maybeThrottle(response, req.header.apiVersion(), req.destination, now);

// 5、判断返回类型

if (req.isInternalRequest && response instanceof MetadataResponse)

// 处理元数据请求响应

metadataUpdater.handleSuccessfulResponse(req.header, now, (MetadataResponse) response);

else if (req.isInternalRequest && response instanceof ApiVersionsResponse)

// 处理版本协调响应

handleApiVersionsResponse(responses, req, now, (ApiVersionsResponse) response);

else

// 普通发送消息的响应，通过 InFlightRequest.completed()，生成 ClientResponse，将响应添加到 responses 集合中

responses.add(req.completed(response, now));

}

}

// 解析响应，并且验证响应头，生成 responseStruct 实例

private static Struct parseStructMaybeUpdateThrottleTimeMetrics(ByteBuffer responseBuffer, RequestHeader requestHeader, Sensor throttleTimeSensor, long now) {

// 解析响应头

ResponseHeader responseHeader \= ResponseHeader.parse(responseBuffer,

requestHeader.apiKey().responseHeaderVersion(requestHeader.apiVersion()));

// 解析响应体

Struct responseBody \= requestHeader.apiKey().parseResponse(requestHeader.apiVersion(), responseBuffer);

// 验证请求头与响应头的 correlation id 必须相等

correlate(requestHeader, responseHeader);

if (throttleTimeSensor != null && responseBody.hasField(CommonFields.THROTTLE\_TIME\_MS))

throttleTimeSensor.record(responseBody.get(CommonFields.THROTTLE\_TIME\_MS), now);

return responseBody;

}

该方法主要用来**处理接收完毕的网络请求集合**，做了五件事：

1.  遍历 selector 中的 completedReceives 集合，逐个处理完成的 Receive 对象。
2.  获取发送请求的 Node id。
3.  从 inFlightRequests 集合队列获取已发送请求「**最老的请求**」并删除（从 inFlightRequests 删除，因为inFlightRequests 存储的是未收到请求响应的 ClientRequest，现在请求已经有响应了，就不需要保存了）。
4.  解析响应，并且验证响应头，生成 responseStruct 实例，生成响应体。
5.  处理响应结果，此处分为三种情况：
6.  处理元数据请求响应，则调用 metadataUpdater.handleSuccessfulResponse()。
7.  处理版本协调响应，则调用 handleApiVersionsResponse()。
8.  普通发送消息的响应，通过 InFlightRequest.completed()，生成 ClientResponse，将响应添加到 responses 集合中。

从上面源码可以看出，「**completedReceives**」集合与「**InflightRequests**」集合也有协作的关系, **completedReceives** 集合指的是接收到的响应集合，如果请求已经收到响应了，就可以从 **InflightRequests** 删除了，这样 **InflightRequests** 就起到了可以防止请求堆积的作用。

与 「**completedSends**」正好相反，「**completedReceives**」集合对应 「**InflightRequests**」集合里对应队列的第一个元素，如下图所示：

![](https://article-images.zsxq.com/Fr54i8wcswzUPpO4BvS0wALAKtXT)

### **02.2.7 handleDisconnections()**

/\*\*

\* Handle any disconnected connections

\* @param responses The list of responses that completed with the disconnection

\* @param now The current time

\*/

private void handleDisconnections(List<ClientResponse> responses, long now) {

// 遍历断开连接的 broker 集合数据

for (Map.Entry<String, ChannelState> entry : this.selector.disconnected().entrySet()) {

// 获取节点

String node \= entry.getKey();

log.debug("Node {} disconnected.", node);

// 处理断开连接

processDisconnection(responses, node, now, entry.getValue());

}

}

// 处理断开连接

private void processDisconnection(List<ClientResponse> responses,String nodeId,long now,

ChannelState disconnectState) {

// 处理节点连接状态为 DISCONNECTED，并更新超时时间和重试退避时间

connectionStates.disconnected(nodeId, now);

apiVersions.remove(nodeId);

nodesNeedingApiVersionsFetch.remove(nodeId);

// 判断状态

switch (disconnectState.state()) {

// 授权失败

case AUTHENTICATION\_FAILED:

AuthenticationException exception \= disconnectState.exception();

connectionStates.authenticationFailed(nodeId, now, exception);

log.error("Connection to node {} ({}) failed authentication due to: {}", nodeId,

disconnectState.remoteAddress(), exception.getMessage());

break;

//

case AUTHENTICATE:

log.warn("Connection to node {} ({}) terminated during authentication. This may happen " +

"due to any of the following reasons: (1) Authentication failed due to invalid " +

"credentials with brokers older than 1.0.0, (2) Firewall blocking Kafka TLS " +

"traffic (eg it may only allow HTTPS traffic), (3) Transient network issue.",

nodeId, disconnectState.remoteAddress());

break;

// 未连接

case NOT\_CONNECTED:

log.warn("Connection to node {} ({}) could not be established. Broker may not be available.", nodeId, disconnectState.remoteAddress());

break;

default:

break; // Disconnections in other states are logged at debug level in Selector

}

// 取消请求

cancelInFlightRequests(nodeId, now, responses);

// 将连接失败通知元数据更新器

metadataUpdater.handleServerDisconnect(now, nodeId, Optional.ofNullable(disconnectState.exception()));

}

### **02.2.8 handleConnections()**

/\*\*

\* Record any newly completed connections

\* 处理连接的handler

\*/

private void handleConnections() {

// 遍历 selector 的刚刚连接成功的集合

for (String node : this.selector.connected()) {

if (discoverBrokerVersions) {

// 更新连接的状态为版本协调状态

this.connectionStates.checkingApiVersions(node);

// 将请求保存到 nodesNeedingApiVersionsFetch 集合里

nodesNeedingApiVersionsFetch.put(node, new ApiVersionsRequest.Builder());

log.debug("Completed connection to node {}. Fetching API versions.", node);

} else {

// 更新连接的状态为连接状态

this.connectionStates.ready(node);

log.debug("Completed connection to node {}. Ready.", node);

}

}

}

该方法主要用来**处理请求连接的，并且会在第一次连接到 Broker 时创建版本请求**。

创建完版本请求，接下来我们来看看如何发送版本请求和处理版本响应的，poll() 方法会调用**handleInitiateApiVersionRequests 发送版本协调请求，然后调用 handleApiVersionsResponse 负责处理版本响应。**

### **02.2.9 handleInitiateApiVersionRequests()**

// 发送版本协调请求

private void handleInitiateApiVersionRequests(long now) {

// 遍历请求集合 nodesNeedingApiVersionsFetch, 在调用 handleConnections 时被加入的

Iterator<Map.Entry<String, ApiVersionsRequest.Builder>> iter = nodesNeedingApiVersionsFetch.entrySet().iterator();

while (iter.hasNext()) {

Map.Entry<String, ApiVersionsRequest.Builder> entry = iter.next();

String node \= entry.getKey();

// 判断是否允许发送请求

if (selector.isChannelReady(node) && inFlightRequests.canSendMore(node)) {

log.debug("Initiating API versions fetch from node {}.", node);

ApiVersionsRequest.Builder apiVersionRequestBuilder \= entry.getValue();

// 调用 newClientRequest 生成请求

ClientRequest clientRequest \= newClientRequest(node, apiVersionRequestBuilder, now, true);

// 发送请求

doSend(clientRequest, true, now);

// 完成后移除

iter.remove();

}

}

}

### **02.2.10 handleApiVersionsResponse()**

// 处理版本协调响应

private void handleApiVersionsResponse(List<ClientResponse> responses,

InFlightRequest req, long now, ApiVersionsResponse apiVersionsResponse) {

// 目标节点

final String node \= req.destination;

// 判断响应和版本是否协调

if (apiVersionsResponse.data.errorCode() != Errors.NONE.code()) {

// 处理响应异常

if (req.request.version() == 0 || apiVersionsResponse.data.errorCode() != Errors.UNSUPPORTED\_VERSION.code()) {

log.warn("Received error {} from node {} when making an ApiVersionsRequest with correlation id {}. Disconnecting.",

Errors.forCode(apiVersionsResponse.data.errorCode()), node, req.header.correlationId());

// 关闭节点连接

this.selector.close(node);

// 处理断开连接

processDisconnection(responses, node, now, ChannelState.LOCAL\_CLOSE);

} else {

// Starting from Apache Kafka 2.4, ApiKeys field is populated with the supported versions of

// the ApiVersionsRequest when an UNSUPPORTED\_VERSION error is returned.

// If not provided, the client falls back to version 0.

short maxApiVersion \= 0;

if (apiVersionsResponse.data.apiKeys().size() > 0) {

ApiVersionsResponseKey apiVersion \= apiVersionsResponse.data.apiKeys().find(ApiKeys.API\_VERSIONS.id);

if (apiVersion != null) {

maxApiVersion = apiVersion.maxVersion();

}

}

nodesNeedingApiVersionsFetch.put(node, new ApiVersionsRequest.Builder(maxApiVersion));

}

return;

}

NodeApiVersions nodeVersionInfo \= new NodeApiVersions(apiVersionsResponse.data.apiKeys());

// 更新版本

apiVersions.update(node, nodeVersionInfo);

// 更新连接状态为 ready，可以正常发送请求了。

this.connectionStates.ready(node);

log.debug("Recorded API versions for node {}: {}", node, nodeVersionInfo);

}

### **02.2.11 handleTimeOutConnections()**

/\*\*

\* Handle socket channel connection timeout. The timeout will hit iff a connection

\* stays at the ConnectionState.CONNECTING state longer than the timeout value,

\* as indicated by ClusterConnectionStates.NodeConnectionState.

\* 处理超时连接

\* @param responses The list of responses to update

\* @param now The current time

\*/

private void handleTimedOutConnections(List<ClientResponse> responses, long now) {

// 获取超时连接的节点集合

Set<String> nodes = connectionStates.nodesWithConnectionSetupTimeout(now);

for (String nodeId : nodes) {

// 先关闭节点连接器

this.selector.close(nodeId);

log.debug(

"Disconnecting from node {} due to socket connection setup timeout. " +

"The timeout value is {} ms.",

nodeId,

connectionStates.connectionSetupTimeoutMs(nodeId));

// 处理断开连接

processDisconnection(responses, nodeId, now, ChannelState.LOCAL\_CLOSE);

}

}

/\*\*

\* 收集超时连接的节点集合: 从正在连接的节点集合中迭代出超时连接的节点集合

\* Return the Set of nodes whose connection setup has timed out.

\* @param now the current time in ms

\*/

public Set<String> nodesWithConnectionSetupTimeout(long now) {

return connectingNodes.stream()

.filter(id -> isConnectionSetupTimeout(id, now))

.collect(Collectors.toSet());

}

### **02.2.12 handleTimeOutRequests()**

/\*\*

\* Iterate over all the inflight requests and expire any requests that have exceeded the configured requestTimeout.

\* The connection to the node associated with the request will be terminated and will be treated as a disconnection.

\* 处理超时请求

\* @param responses The list of responses to update

\* @param now The current time

\*/

private void handleTimedOutRequests(List<ClientResponse> responses, long now) {

// 获取超时请求的节点list

List<String> nodeIds = this.inFlightRequests.nodesWithTimedOutRequests(now);

for (String nodeId : nodeIds) {

// close connection to the node

// 先关闭节点连接器

this.selector.close(nodeId);

log.debug("Disconnecting from node {} due to request timeout.", nodeId);

// 处理断开连接

processDisconnection(responses, nodeId, now, ChannelState.LOCAL\_CLOSE);

}

}

/\*\*

\* InFlightRequests.java 收集超时请求的节点集合

\* Returns a list of nodes with pending in-flight request, that need to be timed out

\* @param now current time in milliseconds

\* @return list of nodes

\*/

public List<String> nodesWithTimedOutRequests(long now) {

List<String> nodeIds = new ArrayList<>();

// 遍历节点队列

for (Map.Entry<String, Deque<NetworkClient.InFlightRequest>> requestEntry : requests.entrySet()) {

// 获取节点id

String nodeId \= requestEntry.getKey();

// 获取队列数据

Deque<NetworkClient.InFlightRequest> deque = requestEntry.getValue();

// 判断是否超时

if (hasExpiredRequest(now, deque))

// 超时后将节点加入

nodeIds.add(nodeId);

}

return nodeIds;

}

// InFlightRequests.java 判断是否超时

private Boolean hasExpiredRequest(long now, Deque<NetworkClient.InFlightRequest> deque) {

for (NetworkClient.InFlightRequest request : deque) {

long timeSinceSend \= Math.max(0, now - request.sendTimeMs);

// 节点队列请求是否超时，如果有一个请求超时，就认为这个 broker 超时了，默认 30000 ms。

if (timeSinceSend > request.requestTimeoutMs)

return true;

}

return false;

}

poll() 方法会将已完成的请求，生成 ClientResponse 对象收集起来，最后执行回调函数 **completeResponses**，如果是异常响应则要求重发，如果是正常响应则调用每个消息自定义的 callback。

###   
**02.2.13 completeResponses()**

private void completeResponses(List<ClientResponse> responses) {

for (ClientResponse response : responses) {

try {

// 调用响应完成的回调函数

response.onComplete();

} catch (Exception e) {

log.error("Uncaught error in request completion:", e);

}

}

}

public class ClientResponse {

// 请求头

private final RequestHeader requestHeader;

// 回调函数，由 clientRequest 来指定

private final RequestCompletionHandler callback;

// 目标节点地址

private final String destination;

// 接受时间

private final long receivedTimeMs;

private final long latencyMs;

private final boolean disconnected;

private final UnsupportedVersionException versionMismatch;

private final AuthenticationException authenticationException;

// 响应体

private final AbstractResponse responseBody;

....

// 响应完成的回调函数

public void onComplete() {

if (callback != null)

// 调用 requestCompletionHandler 回调

callback.onComplete(this);

}

....

}

### **02.2.14 leastLoadedNode() 获取负载最小的节点**

/\*\*

\* Choose the node with the fewest outstanding requests which is at least eligible for connection. This method will

\* prefer a node with an existing connection, but will potentially choose a node for which we don't yet have a

\* connection if all existing connections are in use. If no connection exists, this method will prefer a node

\* with least recent connection attempts. This method will never choose a node for which there is no

\* existing connection and from which we have disconnected within the reconnect backoff period, or an active

\* connection which is being throttled.

\*

\* @return The node with the fewest in-flight requests.

\*/

@Override

public Node leastLoadedNode(long now) {

// 从元数据中获取所有的节点

List<Node> nodes = this.metadataUpdater.fetchNodes();

if (nodes.isEmpty())

throw new IllegalStateException("There are no nodes in the Kafka cluster");

int inflight \= Integer.MAX\_VALUE;

Node foundConnecting \= null;

Node foundCanConnect \= null;

Node foundReady \= null;

int offset \= this.randOffset.nextInt(nodes.size());

for (int i \= 0; i < nodes.size(); i++) {

int idx \= (offset + i) % nodes.size();

Node node \= nodes.get(idx);

// 节点是否可以发送请求

if (canSendRequest(node.idString(), now)) {

// 获取节点的队列大小

int currInflight \= this.inFlightRequests.count(node.idString());

// 如果为 0 则返回该节点，负载最小

if (currInflight == 0) {

// if we find an established connection with no in-flight requests we can stop right away

log.trace("Found least loaded node {} connected with no in-flight requests", node);

return node;

} else if (currInflight < inflight) { // 如果队列大小小于最大值

// otherwise if this is the best we have found so far, record that

inflight = currInflight;

foundReady = node;

}

} else if (connectionStates.isPreparingConnection(node.idString())) {

foundConnecting = node;

} else if (canConnect(node, now)) {

if (foundCanConnect == null ||

this.connectionStates.lastConnectAttemptMs(foundCanConnect.idString()) >

this.connectionStates.lastConnectAttemptMs(node.idString())) {

foundCanConnect = node;

}

} else {

log.trace("Removing node {} from least loaded node selection since it is neither ready " +

"for sending or connecting", node);

}

}

// We prefer established connections if possible. Otherwise, we will wait for connections

// which are being established before connecting to new nodes.

if (foundReady != null) {

log.trace("Found least loaded node {} with {} inflight requests", foundReady, inflight);

return foundReady;

} else if (foundConnecting != null) {

log.trace("Found least loaded connecting node {}", foundConnecting);

return foundConnecting;

} else if (foundCanConnect != null) {

log.trace("Found least loaded node {} with no active connection", foundCanConnect);

return foundCanConnect;

} else {

log.trace("Least loaded node selection failed to find an available node");

return null;

}

}

该方法主要是**选出一个负载最小的节点**，如下图所示：

![](https://article-images.zsxq.com/Fvaoqf2u7g-tBPnvgKdddgGkaxd0)

## **03 InflightRequests 集合设计**

通过上面的代码分析，我们知道「**InflightRequests**」集合的作用就是缓存已经发送出去但还没有收到响应的 ClientRequest 请求集合。底层是通过 **ReqMap<string, Deque<NetworkClient.InFlightRequest>> 实现**，其中 key 是 NodeId，value 是发送到对应 Node 的 ClientRequest 请求队列，默认为5个，参数：[max.in.flight.](http://max.in.flight.requests.per.connection/)[requests](http://max.in.flight.requests.per.connection/)[.per.connection](http://max.in.flight.requests.per.connection/) 配置请求队列大小。**它为每个连接生成一个双端队列，因此它能控制请求发送的速度**。

其作用有以下2个：

1.  **节点是否正常**：收集从「**开始发送**」到「**接收响应**」这段时间的请求，来判断要发送的 Broker 节点是否正常，请求和连接是否超时等等，也就是说用来**监控发送到各个节点请求是否正常**。
2.  **节点的负载情况**：Deque 队列到一定长度后就认为某个 Broker 节点负载过高了。

/\*\*

\* The set of requests which have been sent or are being sent but haven't yet received a response

\* 用来缓存已经发送出去或者正在发送但均还没有收到响应的 ClientRequest 请求集合

\*/

final class InFlightRequests {

// 每个连接最大执行中的请求数

private final int maxInFlightRequestsPerConnection;

// 节点 Node 至客户端请求双端队列 Deque<NetworkClient.InFlightRequest> 的映射集合，key为 NodeId, value 是请求队列

private final Map<String, Deque<NetworkClient.InFlightRequest>> requests = new HashMap<>();

/\*\* Thread safe total number of in flight requests. \*/

// 线程安全的 inFlightRequestCount

private final AtomicInteger inFlightRequestCount \= new AtomicInteger(0);

// 设置每个连接最大执行中的请求数

public InFlightRequests(int maxInFlightRequestsPerConnection) {

this.maxInFlightRequestsPerConnection = maxInFlightRequestsPerConnection;

}

这里通过「**场景驱动**」的方式来讲解关键方法，当有新请求需要发送处理时，会在**队首入队**。而实际被处理的请求，则是从**队尾出队**，保证入队早的请求先得到处理。

## **03.1 canSendMore()**

先来看下发送条件限制， NetworkClient 调用这个方法用来判断是否还可以向指定 Node 发送请求。

/\*\*

\* Can we send more requests to this node?

\* @param node Node in question

\* @return true iff we have no requests still being sent to the given node

\* 判断该连接是否还能发送请求

\*/

public boolean canSendMore(String node) {

// 获取节点对应的双端队列

Deque<NetworkClient.InFlightRequest> queue = requests.get(node);

// 判断条件 队列为空 || (队首已经发送完成 && 队列中没有堆积更多的请求)

return queue \=\= null || queue.isEmpty() ||

(queue.peekFirst().send.completed() && queue.size() < this.maxInFlightRequestsPerConnection);

}

从上面代码可以看出**限制条件**，队列虽然可以存储多个请求，但是新的请求要是加进来条件是上一个请求必须发送成功。

条件判断如下：

1.  queue == null || queue.isEmpty()，队列为空就能发送。
2.  判断 queue.peekFirst().send.completed() 队首是否发送完成。
3.  如果队首的请求迟迟发送不出去，可能就是网络的原因，因此不能继续向此 Node 发送请求。
4.  **队首的请求与对应的 KafkaChannel.send 字段指向的是同一个请求，为了避免未发送的消息被覆盖掉，也不能让 KafkaChannel.send 字段指向新请求**。
5.  queue.size() < this.maxInFlightRequestsPerConnection，该条件就是为了**判断队列中是否堆积过多请求，**如果 Node 已经堆积了很多未响应的请求，说明这个节点出现了网络拥塞，继续再发送请求，则可能会超时。

## **03.2 add() 入队**

/\*\*

\* Add the given request to the queue for the connection it was directed to

\* 将请求添加到队列首部

\*/

public void add(NetworkClient.InFlightRequest request) {

// 这个请求要发送到哪个 Broker 节点上

String destination \= request.destination;

// 从 requests 集合中根据给定请求的目标 Node 节点获取对应 Deque<ClientRequest> 双端队列 reqs

Deque<NetworkClient.InFlightRequest> reqs = this.requests.get(destination);

// 如果双端队列reqs为null

if (reqs == null) {

// 构造一个双端队列 ArrayDeque 类型的 reqs

reqs = new ArrayDeque<>();

// 将请求目标 Node 节点至 reqs 的映射关系添加到 requests 集合

this.requests.put(destination, reqs);

}

// 将请求 request 添加到 reqs 队首

reqs.addFirst(request);

// 增加计数

inFlightRequestCount.incrementAndGet();

}

## **03.3 completeNext() 出队最老请求**

/\*\*

\* Get the oldest request (the one that will be completed next) for the given node

\* 取出该连接对应的队列中最老的请求

\*/

public NetworkClient.InFlightRequest completeNext(String node) {

// 根据给定 Node 节点获取客户端请求双端队列 reqs，并从队尾出队

NetworkClient.InFlightRequest inFlightRequest \= requestQueue(node).pollLast();

// 递减计数器

inFlightRequestCount.decrementAndGet();

return inFlightRequest;

}

对比下入队和出队这2个方法，「**入队 add()**」时是通过 addFirst() 方法添加到队首的，所以队尾的请求是时间最久的，也是应该先处理的，所以「**出队 completeNext()**」是通过 pollLast()，将队列中时间最久的请求元素移出进行处理。

## **03.4 lastSent() 获取最新请求**

/\*\*

\* Get the last request we sent to the given node (but don't remove it from the queue)

\* @param node The node id

\*/

public NetworkClient.InFlightRequest lastSent(String node) {

return requestQueue(node).peekFirst();

}

## **03.5 completeLastSent() 出队最新请求**

/\*\*

\* Complete the last request that was sent to a particular node.

\* @param node The node the request was sent to

\* @return The request

\* 取出该连接对应的队列中最新的请求

\*/

public NetworkClient.InFlightRequest completeLastSent(String node) {

// 根据给定 Node 节点获取客户端请求双端队列 reqs，并从队首出队

NetworkClient.InFlightRequest inFlightRequest \= requestQueue(node).pollFirst();

// 递减计数器

inFlightRequestCount.decrementAndGet();

return inFlightRequest;

}

最后我们来看看「**InflightRequests**」，表示正在发送的请求，存储着请求发送前的所有信息。

另外它支持生成响应 ClientResponse，当正常收到响应时，completed()会根据响应内容生成对应的 ClientResponse，当连接突然断开后，disconnected() 会生成 ClientResponse 对象，代码如下：

static class InFlightRequest {

// 请求头

final RequestHeader header;

// 这个请求要发送到哪个 Broker 节点上

final String destination;

// 回调函数

final RequestCompletionHandler callback;

// 是否需要进行响应

final boolean expectResponse;

// 请求体

final AbstractRequest request;

// 发送前是否需要验证连接状态

final boolean isInternalRequest; // used to flag requests which are initiated internally by NetworkClient

// 请求的序列化数据

final Send send;

// 发送时间

final long sendTimeMs;

// 请求的创建时间，即 ClientRequest 的创建时间

final long createdTimeMs;

// 请求超时时间

final long requestTimeoutMs;

.....

/\*\*

\* 收到响应，回调的时候据响应内容生成 ClientResponse

\*/

public ClientResponse completed(AbstractResponse response, long timeMs) {

return new ClientResponse(header, callback, destination, createdTimeMs, timeMs,

false, null, null, response);

}

/\*\*

\* 当连接突然断开，也会生成 ClientResponse。

\*/

public ClientResponse disconnected(long timeMs, AuthenticationException authenticationException) {

return new ClientResponse(header, callback, destination, createdTimeMs, timeMs,

true, null, authenticationException, null);

}

}

## **04 ClusterConnectionState 设计**

该类用来管理集群所有节点连接的状态数据。

## **04.1 canConnect()**

/\*\*

\* Return true iff we can currently initiate a new connection. This will be the case if we are not

\* connected and haven't been connected for at least the minimum reconnection backoff period.

\* @param id the connection id to check

\* @param now the current time in ms

\* @return true if we can initiate a new connection

\* 判断连接状态

\*/

public boolean canConnect(String id, long now) {

// 获取对应节点的连接状态

NodeConnectionState state \= nodeState.get(id);

if (state == null)

return true;

else

// 首先连接必须是 isDisconnected，不能是 connecteding 状态，即客户端与服务端的连接状态是没有连接上 && 两次重试之间时间差要大于重试退避时间，目的就是为了避免网络拥塞，防止重连过于频繁造成网络压力过大

return state.state.isDisconnected() &&

now - state.lastConnectAttemptMs >= state.reconnectBackoffMs;

}

## **04.2 disconnected()**

/\*\*

\* Enter the disconnected state for the given node.

\* @param id the connection we have disconnected

\* @param now the current time in ms

\* 断开连接状态

\*/

public void disconnected(String id, long now) {

// 获取对应节点的连接状态

NodeConnectionState nodeState \= nodeState(id);

nodeState.lastConnectAttemptMs = now;

// 修改重试退避时间

updateReconnectBackoff(nodeState);

// 如果连接状态为正在连接

if (nodeState.state == ConnectionState.CONNECTING) {

// 修改节点超时时间

updateConnectionSetupTimeout(nodeState);

// 移除正在连接节点

connectingNodes.remove(id);

} else {

// 重置节点超时时间

resetConnectionSetupTimeout(nodeState);

}

// 处理节点连接状态为 DISCONNECTED，并更新超时时间和重试退避时间

nodeState.state = ConnectionState.DISCONNECTED;

}

/\*\*

\* Increment the failure counter and update the node connection setup timeout exponentially.

\* The delay is socket.connection.setup.timeout.ms \* 2\*\*(failures) \* (+/- 20% random jitter)

\* Up to a (pre-jitter) maximum of reconnect.backoff.max.ms

\*

\* @param nodeState The node state object to update

\*/

private void updateConnectionSetupTimeout(NodeConnectionState nodeState) {

// 递增失败次数

nodeState.failedConnectAttempts++;

// 修改节点连接超时时间

nodeState.connectionSetupTimeoutMs = connectionSetupTimeout.backoff(nodeState.failedConnectAttempts);

}

/\*\*

\* Resets the failure count for a node and sets the connection setup timeout to the base

\* value configured via socket.connection.setup.timeout.ms

\* @param nodeState The node state object to update

\*/

private void resetConnectionSetupTimeout(NodeConnectionState nodeState) {

// 重置节点失败计数

nodeState.failedConnectAttempts = 0;

// 连接超时时间为初始时间

nodeState.connectionSetupTimeoutMs = connectionSetupTimeout.backoff(0);

}

## **04.3 isReady()**

/\*\*

\* Return true if the connection is in the READY state and currently not throttled.

\*

\* @param id the connection identifier

\* @param now the current time in ms

\*/

public boolean isReady(String id, long now) {

return isReady(nodeState.get(id), now);

}

private boolean isReady(NodeConnectionState state, long now) {

// 状态不为空 && 状态已准备好 && 节流时间等于小于当前时间

return state != null && state.state == ConnectionState.READY && state.throttleUntilTimeMs <= now;

}

## **04.4 connecting()**

/\*\*

\* Enter the connecting state for the given connection, moving to a new resolved address if necessary.

\* @param id the id of the connection

\* @param now the current time in ms

\* @param host the host of the connection, to be resolved internally if needed

\* @param clientDnsLookup the mode of DNS lookup to use when resolving the {@code host}

\*/

public void connecting(String id, long now, String host, ClientDnsLookup clientDnsLookup) {

// 获取对应节点的连接状态

NodeConnectionState connectionState \= nodeState.get(id);

// 节点状态不为空 && 节点状态对应 host 能匹配上

if (connectionState != null && connectionState.host().equals(host)) {

connectionState.lastConnectAttemptMs = now;

// 节点状态改为正在连接

connectionState.state = ConnectionState.CONNECTING;

// Move to next resolved address, or if addresses are exhausted, mark node to be re-resolved

connectionState.moveToNextAddress();

// 添加到正在连接节点集合中

connectingNodes.add(id);

return;

} else if (connectionState != null) {

log.info("Hostname for node {} changed from {} to {}.", id, connectionState.host(), host);

}

// Create a new NodeConnectionState if nodeState does not already contain one

// for the specified id or if the hostname associated with the node id changed.

nodeState.put(id, new NodeConnectionState(ConnectionState.CONNECTING, now,

reconnectBackoff.backoff(0), connectionSetupTimeout.backoff(0), host, clientDnsLookup));

// 添加到正在连接节点集合中

connectingNodes.add(id);

}

## **04.5 ready()**

/\*\*

\* Enter the ready state for the given node.

\* @param id the connection identifier

\*/

public void ready(String id) {

// 获取对应节点的连接状态

NodeConnectionState nodeState \= nodeState(id);

// 设置节点状态为ready

nodeState.state = ConnectionState.READY;

nodeState.authenticationException = null;

// 重置重连退避时间

resetReconnectBackoff(nodeState);

// 重置连接超时时间

resetConnectionSetupTimeout(nodeState);

// 当连接准备好后从正在连接节点集合中删除

connectingNodes.remove(id);

}

其他的方法这里就不逐一讲解了，自行查看对应源码。

## **05 完整请求流程串联**

一条完整的请求主要分为以下几个阶段：

1.  调用 NetworkClient 的 ready()，连接服务端。
2.  调用 NetworkClient 的 poll()，处理连接。
3.  调用 NetworkClient 的 newClientRequest()，创建请求 ClientRequest。
4.  然后调用 NetworkClient 的 send()，发送请求。
5.  最后调用 NetworkClient 的 poll()，处理响应。

![](https://article-images.zsxq.com/FnNjVjOoIJig5DVYO780srR1Wf6X)

## **05.1 创建连接过程**

NetworkClient 发送请求之前，都需要先和 Broker 端创建连接。NetworkClient 负责管理与集群的所有连接。

  
![](https://article-images.zsxq.com/FpgEMZTDXfrbbwt3BleX5AG9BJf6)

## **05.2 生成请求过程**

![](https://article-images.zsxq.com/FvYF426Sx5FupPNPnhPrTmxb4cat)

## **05.3 发送请求过程**

![](https://article-images.zsxq.com/FnB2uQjMU_gPyggNXj5mjNpXSYaC)

## **05.4 处理响应过程**

### **05.4.1 请求发送完成**

### ![](https://article-images.zsxq.com/Fu5lQlrdzLX2OHK_G0rWjm-mOgBy)

### **05.4.2 请求收到响应**

![](https://article-images.zsxq.com/FjeWXlsC_URYjh8S7tVy0Y3Kte0x)

###   
**05.4.3 执行处理响应**

![](https://article-images.zsxq.com/Fhx6QmmYTakukHEfNVT0lhdTN-Jj)

## **06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、开篇总述消息被 Sender 子线程先将消息**暂存到 KafkaC****hannel 的 send 中**，等调用「**poll()**」执行真正的网络I/O 操作，从而引出了为客户端提供网络 I/O 能力的 「**NetworkClient 组件**」。

2、带你深度剖析了「**NetworkClient 组件**」 、「**InflightRequests**」、「**ClusterConnectionState** 」的实现细节。

3、最后带你串联了整个消息发送请求和处理响应的流程，让你有个更好的整体认知。