大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka** **消费者初始化的流程**」，今天我们开启消费端源码的征程，这是第二篇来深度聊聊「**Kafka 消费者网络通信架构设计**」，看看 Kafka 消费者端的网络通信组件是如何设计的。

![](https://article-images.zsxq.com/Frp-tn3UUSneaa-rRqcM-3Vacl7y)

## **01 总体概述**

从今天开始我将以「 **Kafka 3.0**」 版本为主，通过「**场景驱动**」的方式带大家一点点的对 Kafka 源码进行深度剖析，正式开启 「**Kafka 消费者源码之旅**」，跟我一起来掌握 Kafka 源码核心架构设计思想吧。

今天这篇我们先来聊聊 Kafka 消费者初始化后与服务端通信核心流程，带你梳理消费者通信组件架构设计的整体源码分析脉络，接下来会逐一讲解说明。

本文涉及的源码：

「**ConsumerNetworkClient**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkClient.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkClient.java)

「**RequestFeture**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/RequestFeture.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/RequestFeture.java)

「**KafkaConsumer**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java)

「**ConsumerCoordinator**」类源码在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java](https://github.com/apache/kafka/blob/3.0.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java)

## **02 消费者网络通信组件**

在剖析「**消费者拉取数据**」之前，我们先来拆解剖析「**消费者网络通信客户端**」，当你了解了消费者是如何跟 Kafka 集群进行通信的，就会更好的理解消息数据拉取。

在 [【消费者源码分析系列第一篇】图解 Kafka 源码之消费者初始化流程](https://articles.zsxq.com/id_3g80nohn4g6s.html) 这篇初始化中，有个 「**ConsumerNetworkClient**」组件，它就是消费者负责网络通信的组件。

  
我们在前面剖析生产者源码的时候了解到生产者的网络通信组件是 「**NetworkClient**」，如果忘记或者不了解的可以点击这篇 [【生产者源码分析系列第九篇】图解 Kafka 源码之 NetworkClient 网络通信组件架构设计](https://articles.zsxq.com/id_k2pnfv2xq2wb.html) 学习。

消费者的网络通信组件是在「**NetworkClient**」类之上又封装了一层，「**ConsumerNetworkClient**」组件类为上层业务提供了网络服务，包括「**拉取消息数据**」、「**心跳维持**」、「**与消费者协调器交互**」等一系列网络请求都是使用「**ConsumerNetworkClient**」组件来完成网络交互。

从源码看，该类是一个「**线程安全**」类，通过锁的机制保证了线程安全。跟生产者发送消息一样，「**发送请求**」和「**真正的网络I/O**」是解耦的，也是先发送到一个缓冲区，然后从缓冲区取出请求进行真正的网络发送。

![](https://article-images.zsxq.com/FomGtRR9EdVU-CXtwi_MIxpsWS4a)

![](https://article-images.zsxq.com/FgChgXYtRXPXrhH4nzaOqIzNRYyM)

接下来我们来剖析下该类的源码实现。

## **2.1 ConsumerNetworkClient 初始化**

public class ConsumerNetworkClient implements Closeable {

// 一次拉取最大的超时时间 5 秒

private static final int MAX\_POLL\_TIMEOUT\_MS \= 5000;

// the mutable state of this class is protected by the object's monitor (excluding the wakeup

// flag and the request completion queue below).

private final Logger log;

// NetworkClient 对象

private final KafkaClient client;

// 缓冲队列，其中 key 是 Node 节点，value 是发往此 Node 的 ClientRequest 集合。

private final UnsentRequests unsent \= new UnsentRequests();

// 消费者端 Kafka 集群元数据

private final Metadata metadata;

private final Time time;

// 重试退避时间

private final long retryBackoffMs;

// 超时时间，默认 5000 ms

private final int maxPollTimeoutMs;

// 请求超时时间，默认 30000 ms

private final int requestTimeoutMs;

// 是否禁用 wakeup()，因为有的方法已经执行的情况下就没有必要再唤醒 Selector.poll() 方法的阻塞了，比如 close() 方法，因为网络 I/O 已经要关闭了就没必要再唤醒。

private final AtomicBoolean wakeupDisabled \= new AtomicBoolean();

// ReentrantLock 类的对象，用来保证类的线程安全。

private final ReentrantLock lock \= new ReentrantLock(true);

// 用来存储请求的回调对象的队列集合。响应回来后并不是直接调用回调对象里的回调方法，而是放到队列中等响应处理完了统一调用回调方法。

private final ConcurrentLinkedQueue<RequestFutureCompletionHandler> pendingCompletion = new ConcurrentLinkedQueue<>();

// 用来存储待断开网络连接的节点集合。

private final ConcurrentLinkedQueue<Node> pendingDisconnects = new ConcurrentLinkedQueue<>();

// 是否唤醒消费者网络客户端，当有唤醒 selector.poll() 阻塞的操作时，就把这个值设为 true。

private final AtomicBoolean wakeup \= new AtomicBoolean(false);

// 构造方法

public ConsumerNetworkClient(LogContext logContext,

KafkaClient client,

Metadata metadata,

Time time,

long retryBackoffMs,

int requestTimeoutMs,

int maxPollTimeoutMs) {

this.log = logContext.logger(ConsumerNetworkClient.class);

this.client = client;

this.metadata = metadata;

this.time = time;

this.retryBackoffMs = retryBackoffMs;

this.maxPollTimeoutMs = Math.min(maxPollTimeoutMs, MAX\_POLL\_TIMEOUT\_MS);

this.requestTimeoutMs = requestTimeoutMs;

}

....

}

重要字段如下：

1.  **client**：KafkaClient 类对象，负责网络连接。
2.  **unsent**：UnsentRequests 类对象。UnsentRequests 类提供了请求的缓冲区队列，该对象内部维护了一个 unsent 属性，该属性是 [ConcurrentMap<Node, ConcurrentLinkedQueue<ClientRequest>>](http://concurrentmapnode,%20concurrentlinkedqueueclientrequest/)，其中 key 是 Node 节点，value 是发往此 Node 的 [ConcurrentLinkedQueue<ClientRequest>](http://concurrentlinkedqueueclientrequest/) 集合。类似于生产者的 RecordAccumulator，但是没 RecordAccumulator 复杂。
3.  **metadata**：MetaData 类的子类对象，这里是 ConsumerMetadata 类的对象。作用是缓存了消费者元数据。
4.  **retryBackoffMs**：失败重试退避时间，在尝试重试对给定主题分区的失败请求之前等待的时间量，这避免了在某些故障情况下在紧密循环中重复发送请求。对应 [retry.backoff.ms](http://retry.backoff.ms/) 配置，默认 100 ms。
5.  **maxPollTimeoutMs**：用来底层 [Selector.poll](http://selector.poll/)() 方法的阻塞超时时间，即消费者协调器的心跳之间的预期时间。心跳用来确保消费者的会话保持活跃，并在新消费者加入或离开组时进行重平衡。该值必须设置为低于 [session.timeout.ms](http://session.timeout.ms/)，但通常不应设置为高于该值的 1/3。它可以调整得更低，以控制正常重平衡的预期时间。对应 [heartbeat.interval.ms](http://heartbeat.interval.ms/) 配置，默认 3000 ms。构造函数中，[maxPollTimeoutMs](http://maxpolltimeoutms/) 取的是 [maxPollTimeoutMs 与 MAX\_POLL\_TIMEOUT\_MS](http://xn--maxpolltimeoutms%20%20max_poll_timeout_ms-mz49d/) 的最小值，[MAX\_POLL\_TIMEOUT\_MS](http://max_poll_timeout_ms%20/) 默认为 5000 ms。
6.  **requestTimeoutMs**：配置控制客户端等待请求响应的最长超时时间，对应 [request.timeout.ms](http://request.timeout.ms/) 配置默认 30000 ms。因为请求是放到缓冲区的，但是长期在缓冲区中没有发送的请求应该处理掉。如果在超时之前没有收到响应，客户端将在必要时重新发送请求，或者如果重试用尽，则请求失败。
7.  **wakeupDisabled**：bool类型，是否禁用 wakeup() 即中断 KafkaConsumer 线程，因为有的方法已经执行的情况下就没有必要再唤醒 [Selector.poll](http://selector.poll/)() 方法的阻塞了，比如 close() 方法，因为网络 I/O 已经要关闭了就没必要再唤醒。
8.  **wakeup**：bool类型，当有唤醒 [selector.poll](http://selector.poll/)() 阻塞的操作时，就把这个值设为 true。
9.  **lock**：ReentrantLock 类的对象，用来保证类的线程安全，我们不需要高吞吐量，所以使用公平锁来尽量避免饥饿。
10.  **pendingCompletion**：用来存储请求的回调对象的队列集合。响应回来后并不是直接调用回调对象里的回调方法，而是放到队列中等响应处理完了统一调用回调方法。
11.  **pendingDisconnets**：用来存储待断开网络连接的节点集合。

剖析完这些关键字段和属性后，我们来看看请求是如何发送的。

在本小节开头说过，跟生产者发送消息一样，「**发送请求**」和「**真正的网络I/O**」是解耦的，接下来我们也分为两大部分来剖析。

先来看第一部分「**发送请求到缓冲区**」。

##   
**2.2 发送请求到缓冲区**

首先，缓冲区存储的是要发送但还没有真正执行的网络 I/O 请求。

## **2.2.1 send()**

public RequestFuture<ClientResponse> send(Node node, AbstractRequest.Builder<?> requestBuilder) {

return send(node, requestBuilder, requestTimeoutMs);

}

public RequestFuture<ClientResponse> send(Node node,

AbstractRequest.Builder<?> requestBuilder,

int requestTimeoutMs) {

long now \= time.milliseconds();

// 1、新建一个 RequestFutureCompletionHandler 对象作为请求完成的处理器

RequestFutureCompletionHandler completionHandler \= new RequestFutureCompletionHandler();

// 2、构造请求对象，调用 NetworkClient#newClientRequest() 方法生成一个网络 ClientRequest 对象

ClientRequest clientRequest \= client.newClientRequest(node.idString(), requestBuilder, now, true,requestTimeoutMs, completionHandler);

// 3、把请求对象放入缓存队列 unsent 里

unsent.put(node, clientRequest);

// 4、唤醒处在阻塞过程中的 selector.poll() 方法，这样能对发送的请求发起网络请求。

client.wakeup();

return completionHandler.future;

}

核心步骤如下：

1.  新建一个 [RequestFutureCompletionHandler](http://requestfuturecompletionhandler%20/) 对象作为请求完成的处理器。
2.  构造请求对象，调用 [NetworkClient#newClientRequest()](http://networkclient/#newClientRequest\(\)) 方法生成一个网络 ClientRequest 对象。
3.  把请求对象放入缓存队列 unsent 里。
4.  唤醒处在阻塞过程中的 [selector.poll](http://selector.poll/)() 方法，这样能对发送的请求发起网络请求。

## **2.2.2 回调对象 completionHandler**

![](https://article-images.zsxq.com/FiWDMFGEFmchvPgsO8Cild4VhZCG)

从 RequestFutureCompletionHandler 继承关系图我们可以知道，它不仅实现了 RequestCompletionHandler 接口，还组合了 RequestFuture 类，RequestFuture 是一个泛型类，其核心字段与方法如下：

1.  listeners：RequestFutureListener 队列，用来监听请求完成的情况。RequestFutureListener 接口有 onSuccess() 和 onFailure () 两个方法，对应于请求正常完成和出现异常两种情况。
2.  isDone()：表示当前请求是否已经完成，不管正常完成还是出现异常，此字段都会被设置为 true。
3.  value()：记录请求正常完成时收到的响应，与 exception() 方法互斥。此字段非空表示正常完成，反之表示出现异常。
4.  exception()：记录导致请求异常完成的异常类，与 value() 互斥。此字段非空则表示出现异常，反之则表示正常完成。

我们之所以要分析源码，是因为源码中有很多设计模式可以借鉴，应用到你自己的工作中。RequestFuture 中有两处典型的设计模式的使用，我们来看一下：

1.  compose() 方法：使用了适配器模式。
2.  chain() 方法：使用了责任链模式。

我们先来剖析 **RequestFetureCompletionHandler**，源码如下：

![](https://article-images.zsxq.com/FtvqmSpswXNkxYSwbR8gIJJv0n7w)

从上图可以看出 [RequestFutureCompletionHandler](http://requestfuturecompletionhandler/) 实现了 [RequestCompletionHandler](http://requestcompletionhandler/)，这个接口也被生产者的请求回调实现了。其中 [RequestFuture](http://requestfuture/) 给我们提供了异步请求。

接着来看一下回调对象中对响应的处理逻辑，源码如下：

// RequestFutureCompletionHandler 类方法，它是 ConsumerNetworkClient 子类

// 响应异常时的处理

public void onFailure(RuntimeException e) {

//获取异常并把回调对象放到 pendingCompletion 集合里

this.e = e;

pendingCompletion.add(this);

}

@Override

// 响应成功时的处理

public void onComplete(ClientResponse response) {

// 获取 response 并把回调对象放到 pendingCompletion 集合里

this.response = response;

pendingCompletion.add(this);

}

1.  其中 [onFailure()](http://onfailure\(\)/) 是当响应异常时调用的方法，保存异常并把回调对象放入 [pendingCompletion](http://pendingcompletion/) 集合里。
2.  其中 [onComplete()](http://oncomplete\(\)/) 是当响应成功时调用的方法，保存响应并把回调对象放入 [pendingCompletion](http://pendingcompletion/) 集合里。

该类中跟回调对象相关的方法是 fireCompletion。

// RequestFutureCompletionHandler 类方法，它是 ConsumerNetworkClient 子类

public void fireCompletion() {

if (e != null) {

future.raise(e);

} else if (response.authenticationException() != null) {

future.raise(response.authenticationException());

} else if (response.wasDisconnected()) {

log.debug("Cancelled request with header {} due to node {} being disconnected",

response.requestHeader(), response.destination());

future.raise(DisconnectException.INSTANCE);

} else if (response.versionMismatch() != null) {

future.raise(response.versionMismatch());

} else {

future.complete(response);

}

}

从上可以看出：

1.  如果有异常会调用 [RequestFuture<ClientResponse>](http://requestfutureclientresponse/) 对象的 [future#raise(e)](http://future/#raise\(e\)) 方法处理。
2.  如果没有异常就调用 [RequestFuture<ClientResponse>](http://requestfutureclientresponse/) 对象的 [future#complete(response)](http://future/#complete\(response\)) 处理。

我们这里只剖析下正常响应的处理流程，异常响应的处理是类似的，自行研究。

## **2.2.3 异步请求处理**

我们来看看 [RequestFuture<T>](http://requestfuturet/) 是如何处理异步请求的，其中 [complete()](http://complete\(\)/) 方法主要调用了 [fireSuccess()](http://firesuccess\(\)/) 方法处理成功的响应。

// RequestFuture<T> 类方法

public void complete(T value) {

try {

if (value instanceof RuntimeException)

throw new IllegalArgumentException("The argument to complete can not be an instance of RuntimeException");

if (!result.compareAndSet(INCOMPLETE\_SENTINEL, value))

throw new IllegalStateException("Invalid attempt to complete a request future which is already complete");

// 关键调用，用来处理成功的响应。

fireSuccess();

} finally {

completedLatch.countDown();

}

}

private void fireSuccess() {

T value \= value();

while (true) {

// 取出所有的监听器

RequestFutureListener<T> listener = listeners.poll();

if (listener == null)

break;

// 调用onSuccess()方法处理响应

listener.onSuccess(value);

}

}

可以看到 [fireSuccess()](http://firesuccess\(\)/) 就是循环从取出所有的监听器 listener 并调用其 onSuccess() 方法来处理响应。**也就是说 completionHandler 回调对象处理响应的方式就是循环调用 RequestFeture<T>的监听器相关方法来实现的**。

接着我们来剖析下监听器 [listener](http://listener%20/) 是如何加到 [RequestFuture<T>](http://requestfuturet%20/) 上的。

public <S> RequestFuture<S> compose(final RequestFutureAdapter<T, S> adapter) {

// 适配之后的结果

final RequestFuture<S> adapted = new RequestFuture<>();

// 在当前 RequestFuture 中添加监听器。

addListener(new RequestFutureListener<T>() {

@Override

public void onSuccess(T value) {

adapter.onSuccess(value, adapted);

}

@Override

public void onFailure(RuntimeException e) {

adapter.onFailure(e, adapted);

}

});

return adapted;

}

public void addListener(RequestFutureListener<T> listener) {

// 添加到监听器中

this.listeners.add(listener);

if (failed())

fireFailure();

else if (succeeded())

fireSuccess();

}

可以看到该方法是利用适配器 [RequestFutureAdapter](http://requestfutureadapter/) 给当前的 [RequestFuture<T>](http://requestfuturet/) 添加了监听器 [RequestFutureListener](http://requestfuturelistener/)，并通过 adapter 实现了 [RequestFuture](http://requestfuture/) 的泛型类型转换。

![](https://article-images.zsxq.com/FnoZUhjWEkzxPmu-E-WShBhhwcbu)

从上图可以看出，[RequestFutureAdapter](http://requestfutureadapter/) 是适配器的抽象类，主要是定义了 [onSuccess()](http://onsuccess\(\)%20/) 、[onFailure()](http://onfailure\(\)/) 两个方法，这两个方法实现了 [RequestFuture](http://requestfuture/) 的类型转换，从 [RequestFuture<F>](http://requestfuturef/) 到 [RequestFuture<T>](http://requestfuturet/) 的类型转换。

**看到这里是不是觉得很懵逼呢？为什么要搞出一个** [RequestFutureAdapter](http://requestfutureadapter/) **来呢，它又有什么作用呢？**

在前面 [ConsumerNetworkClient#send()](http://consumernetworkclient/#send\(\)) 方法返回值如下图，很显然这只是个原始的响应，它只是为上次业务提供最原始的异步请求。对于消费者「**心跳维持**」、「**拉取消息**」、「**与消费者协调器交互**」等操作都会调用 [ConsumerNetworkClient#send()](http://consumernetworkclient/#send\(\)) 方法去发送请求，因此不同的业务处理的响应也是不一样的，所以对于 [RequestFuture<T>](http://requestfuturet/) 中的 T 的类型的要求也是不一样的。

为了在不改变 [ConsumerNetworkClient#send()](http://consumernetworkclient/#send\(\)) 方法返回类型前提下，所以通过 [RequestFutureAdapter](http://requestfutureadapter/) 实现业务调用时会得到不同的返回类型。

![](https://article-images.zsxq.com/Fm-icd6nVvTmuay71SAeZAqN2YkH)

下面通过一张图来梳理「**ConsumerNetworkClient**」对成功响应的处理流程：

![](https://article-images.zsxq.com/llGZZIIeflTfZ7b3pPlqCo9hfDWG)

chain() 方法与 compose() 方法类似，也是通过 RequestFutureListener 在多个 RequestFuture 之间传递事件。代码如下：

public void chain(final RequestFuture<T> future) {

// 添加监听器

addListener(new RequestFutureListener<T>() {

@Override

public void onSuccess(T value) {

// 通过监听器将 value 传递给下一个 RequestFuture 对象

future.complete(value);

}

@Override

public void onFailure(RuntimeException e) {

// 通过监听器将异常传递给下一个 RequestFuture 对象

future.raise(e);

}

});

}

## **2.3 消费者请求缓冲区 UnsentRequests**

在剖析「**ConsumerNetworkClient**」初始化时，提到一个组件「**UnsentRequests**」，它是「**ConsumerNetworkClient**」内部的静态类，其作用就是为消费者提供一个「**请求缓冲区**」，对缓冲区的相关操作。

  
![](https://article-images.zsxq.com/FlVY5DThUOOQKuCHnJ3UzvRvaSXc)

unsent 是一个 [ConcurrentHashMap](http://concurrenthashmap/) 的类对象，其中 key 是节点，value 是[ConcurrentLinkedQueue<ClientRequest>](http://concurrentlinkedqueueclientrequest/) 类对象，即按照节点和发送节点的请求队列保存的 Map。

既然 unsent 是请求缓冲区，那么就需要有请求出入，先来剖析下请求缓存的方法。

## **2.3.1 请求缓存**

public void put(Node node, ClientRequest request) {

// the lock protects the put from a concurrent removal of the queue for the node

// 首先使用 synchronized 加锁，加锁对象 unsent

synchronized (unsent) {

// 根据 node 取出要发送请求的队列，如果队列不为空就取出队列，如果队列是空的就创建一个队列并放入 unsent 中

ConcurrentLinkedQueue<ClientRequest> requests = unsent.computeIfAbsent(node, key -> new ConcurrentLinkedQueue<>());

// 把请求放入队列中。

requests.add(request);

}

}

核心步骤如下：

1.  首先使用 [synchronized](http://synchronized%20/) 加锁，加锁对象 unsent。
2.  根据 node 取出要发送请求的队列，如果队列不为空就取出队列，如果队列是空的就创建一个队列并放入 unsent 中。
3.  把请求放入队列中。

**这里你是否有疑惑，既然 unsent 是 ConcurrentHashMap 类型，为什么还要加锁呢？**

因为该方法里有「**放入请求队列**」再往「**请求队列添加请求**」这两个连续的动作，如果中间发生了「**删除节点对应请求列表**」的操作，那么往列表添加请求后 unsent 集合并不存在这个请求，而该并没有告诉调用方这个请求是否添加失败了，这样就造成数据的不一致，所以需要加锁保证安全性。

## **2.3.2 过期请求删除**

如果请求在缓冲区很久没发送出去就应该被删除，对应的方法是 removeExpiredRequests()。

private Collection<ClientRequest> removeExpiredRequests(long now) {

// 过期请求集合

List<ClientRequest> expiredRequests = new ArrayList<>();

// 1、遍历请求

for (ConcurrentLinkedQueue<ClientRequest> requests : unsent.values()) {

Iterator<ClientRequest> requestIterator = requests.iterator();

while (requestIterator.hasNext()) {

ClientRequest request \= requestIterator.next();

// 2、计算请求在缓冲区存在的时间

long elapsedMs \= Math.max(0, now - request.createdTimeMs());

// 3、如果请求在缓冲区存在的时间大于超时时间就把请求加入过期集合，并从 unsent 集合中删除。

if (elapsedMs > request.requestTimeoutMs()) {

expiredRequests.add(request);

requestIterator.remove();

} else

break;

}

}

return expiredRequests;

}

public Collection<ClientRequest> remove(Node node) {

// the lock protects removal from a concurrent put which could otherwise mutate the

// queue after it has been removed from the map

synchronized (unsent) {

// 从缓冲区移除请求

ConcurrentLinkedQueue<ClientRequest> requests = unsent.remove(node);

// 返回请求列表

return requests \=\= null ? Collections.<ClientRequest>emptyList() : requests;

}

}

1.  遍历 unsent 里所有的请求。
2.  计算请求在缓冲区存在的时间。
3.  如果请求在缓冲区存在的时间大于请求超时时间，把请求加入过期请求集合并从缓冲区 unsent 删除。

可以看到，「**请求缓冲区**」很简单吧，接着来剖析下真正的网络 I/O 请求发送是如何实现的？

## **2.4 真正网络 I/O 请求**

这里主要是通过 [poll()](http://poll\(\)/) 方法来实现的网络发送。

public boolean poll(RequestFuture<?> future, Timer timer) {

do {

poll(timer, future);

} while (!future.isDone() && timer.notExpired());

return future.isDone();

}

public void poll(Timer timer, PollCondition pollCondition) {

poll(timer, pollCondition, false);

}

public void poll(Timer timer, PollCondition pollCondition, boolean disableWakeup) {

// there may be handlers which need to be invoked if we woke up the previous call to poll

firePendingCompletedRequests();

lock.lock();

try {

// Handle async disconnects prior to attempting any sends

handlePendingDisconnects();

// 1、调用 ConsumerNetworkClient#trySend() 方法将 unsent 中的请求取出，并与目标节点建立连接

long pollDelayMs \= trySend(timer.currentTimeMs());

// 2、调用 NetworkClient#poll() 方法监听底层网络连接，并处理网络数据读写

if (pendingCompletion.isEmpty() && (pollCondition == null || pollCondition.shouldBlock())) {

// if there are no requests in flight, do not block longer than the retry backoff

long pollTimeout \= Math.min(timer.remainingMs(), pollDelayMs);

if (client.inFlightRequestCount() == 0)

pollTimeout = Math.min(pollTimeout, retryBackoffMs);

client.poll(pollTimeout, timer.currentTimeMs());

} else {

client.poll(0, timer.currentTimeMs());

}

timer.update();

// 3、处理断开连接的 node 的消息

checkDisconnects(timer.currentTimeMs());

if (!disableWakeup) {

// 4、如果有 selector.poll() 阻塞中断请求而且有方法标记不能中断，则抛出异常

maybeTriggerWakeup();

}

maybeThrowInterruptException();

// 5、上面调用了 poll() 方法，send 就有可能发送成功了，也可能增加了网络连接，所以再次从 unsent 集合中取请求做预发送

trySend(timer.currentTimeMs());

// 6、处理 unsent 中超时的请求

failExpiredRequests(timer.currentTimeMs());

// clean unsent requests collection to keep the map from growing indefinitely

unsent.clean();

} finally {

lock.unlock();

}

// 调用 ConsumerNetworkClient#firePendingCompletedRequests() 方法回调上层请求的回调处理器

firePendingCompletedRequests();

metadata.maybeThrowAnyException();

}

核心步骤如下：

1.  调用 [ConsumerNetworkClient#trySend()](http://consumernetworkclient/#trySend\(\)) 方法将 unsent 中的请求取出，并与目标节点建立连接。
2.  调用 [NetworkClient#poll()](http://networkclient/#poll\(\)) 方法监听底层网络连接，并处理网络数据读写。
3.  处理断开连接的 node 的消息。检测消费者与每个 Node 之间的连接状态，当检测到连接断开的 Node 时，会将其在 unsent 集合中对应的全部 ClientRequest 对象清除掉，之后调用这些ClientRequest 的回调函数。
4.  如果有 [selector.poll()](http://selector.poll\(\)/) 唤醒阻塞请求而且有方法标记不能唤醒就抛出异常。这种情况主要是在调用 close() 方法的过程中，因为通信要关闭了，唤醒就没有必要了。
5.  再次调用 trySend() 方法。因为这时用于调用了 [NetworkClient.poll()](http://%20networkclient.poll\(\)/) 方法，[KafkaChannel.send](http://kafkachannel.send/) 字段可能已经发送出去了，就又能做预发送的工作了，也有可能 unsent 集合这时出现了向新的 node 发送的请求，所以再尝试一次 [trySend()](http://trysend\(\)/) 方法。
6.  调用 [failExpiredRequests()](http://failexpiredrequests\(\)%20/) 方法处理 unsent 中过期的请求
7.  调用 [firePendingCompletedRequests()](http://firependingcompletedrequests\(\)/) 方法。收到响应时，会把回调对象放入到这个集合里。这个方法是调用回调对象里的回调方法的，完成响应的处理。

对于第一步中调用 [trySend()](http://trysend\(\)/) 方法循环处理 unsent 中缓存的请求，遍历对应每个节点的请求列表，并用判断[NetworkClient.ready(node,now)](http://networkclient.ready\(node,now\)%20/) 判断要发送的节点是否满足发送条件，如果满足就调用 [NetworkClient.send()](http://networkclient.send\(\)/)做好预发送并将请求放入 [InFlightRequest](http://inflightrequest/) 集合中等待响应，最后删除 unsent 集合对应的请求，源码如下。

long trySend(long now) {

long pollDelayMs \= maxPollTimeoutMs;

// 1、按节点从 unsent 里取出缓存队列

for (Node node : unsent.nodes()) {

// 2、取到 node 对应的发送队列

Iterator<ClientRequest> iterator = unsent.requestIterator(node);

if (iterator.hasNext())

// 3、返回 poll() 操作的延迟时间

pollDelayMs = Math.min(pollDelayMs, client.pollDelayMs(node, now));

// 4、轮询队列里的请求，预发送请求。

while (iterator.hasNext()) {

ClientRequest request \= iterator.next();

// 5、调用 NetworkClient#ready() 确保与目标节点建立了连接

if (client.ready(node, now)) {

// 6、调用 NetworkClient#send()方法，完成请求的预发送即存入连接缓冲区，等待连接可写时正式的网络发送。

client.send(request, now);

// 7、删除队列中对应额请求

iterator.remove();

} else {

// try next node when current node is not ready

break;

}

}

}

// 8、返回 poll 延迟时间

return pollDelayMs;

}

核心步骤如下：

1.  按节点从 unsent 里取出缓存队列。
2.  取到 node 对应的发送队列。
3.  返回 poll() 操作的延迟时间。
4.  轮询队列里的请求，预发送请求。
5.  调用 [NetworkClient#ready()](http://networkclient/#ready\(\)) 确保与目标节点建立了连接。
6.  调用 [NetworkClient#send()](http://networkclient/#send\(\)) 方法，完成请求的预发送即存入连接缓冲区，等待连接可写时正式的网络发送。
7.  删除队列中对应额请求。
8.  返回 poll 延迟时间。

关于 [NetworkClient](http://networkclient/) 相关的源码，请移步到 [【生产者源码分析系列第九篇】图解 Kafka 源码之 NetworkClient 网络通信组件架构设计](https://articles.zsxq.com/id_k2pnfv2xq2wb.html) 这里学习。

调用 checkDisconnects() 方法检测连接状态。检测消费者与每个 Node 之间的连接状态，当检测到连接断开的 Node 时，会将其在 unsent 集合中对应的全部 ClientRequest 对象清除掉，之后调用这些ClientRequest 的回调函数。

private void checkDisconnects(long now) {

// any disconnects affecting requests that have already been transmitted will be handled

// by NetworkClient, so we just need to check whether connections for any of the unsent

// requests have been disconnected; if they have, then we complete the corresponding future

// and set the disconnect flag in the ClientResponse

for (Node node : unsent.nodes()) {

// 检测消费者与每个 Node 之间的连接状态

if (client.connectionFailed(node)) {

// Remove entry before invoking request callback to avoid callbacks handling

// coordinator failures traversing the unsent list again.

// 在调用请求回调之前删除条目以避免回调处理再次遍历未发送列表的协调器故障

Collection<ClientRequest> requests = unsent.remove(node);

for (ClientRequest request : requests) {

RequestFutureCompletionHandler handler \= (RequestFutureCompletionHandler) request.callback();

AuthenticationException authenticationException \= client.authenticationException(node);

// 调用 ClientRequest 的回调函数

handler.onComplete(new ClientResponse(request.makeHeader(request.requestBuilder().latestAllowedVersion()),

request.callback(), request.destination(), request.createdTimeMs(), now, true,null, authenticationException, null));

}

}

}

}

检查 wakeupDisabled 和 wakeup，查看是否有其它线程中断。如果有中断请求，则抛出 WakeupException 异常，中断当前 [ConsumerNetworkClient.poll](http://consumernetworkclient.poll/)() 方法。

public void maybeTriggerWakeup() {

// 通过 wakeupDisabled 检测是否在执行不可中断的方法，通过 wakeup 检测是否有中断请求。

if (!wakeupDisabled.get() && wakeup.get()) {

log.debug("Raising WakeupException in response to user wakeup");

// 重置中断标志

wakeup.set(false);

throw new WakeupException();

}

}

处理 unsent 中超时请求。它会循环遍历整个 unsent 集合，检测每个 ClientRequest 是否超时，将过期请求加入到 expiredRequests 集合，并将其从 unsent 集合中删除。调用超时 ClientRequest 的回调函数 onFailure()。

private void failExpiredRequests(long now) {

// clear all expired unsent requests and fail their corresponding futures

// 清除所有过期的未发送请求并使其相应的 futures 失败

Collection<ClientRequest> expiredRequests = unsent.removeExpiredRequests(now);

for (ClientRequest request : expiredRequests) {

RequestFutureCompletionHandler handler \= (RequestFutureCompletionHandler) request.callback();

// 调用回调函数

handler.onFailure(new TimeoutException("Failed to send request after " + request.requestTimeoutMs() + " ms."));

}

}

至此，「**消费者网络通信客户端**」就剖析完了，接下来我们再来深度剖析下「**消费者拉取消息**」的源码流程。

##   
**03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过「**场景驱动**」的方式从消费者调用出发，抛出消费者初始化后是如何跟服务端进行网络通信的?

2、带你剖析了「**消费者通信组件 ConsumerNetworkClient**」的架构设计，其发送流程与生产者类似，也是先发送到缓冲区，然后通过消费缓冲区的请求去发送真正的网络请求。

3、接着带你剖析了「**消费者通信组件 ConsumerNetworkClient**」对响应的处理，通过监听器在多个[RequestFuture](http://requestfuture/) 之间传递事件，实现基础通信组件 [ConsumerNetworkClient](http://consumernetworkclient/) 和上层业务的解耦。

3、最后剖析了「**消费者缓冲区 UnSentRequests**」的设计实现。

下篇我们来深度剖析「**消费者是如何拉取数据的**」，大家期待，我们下期见。