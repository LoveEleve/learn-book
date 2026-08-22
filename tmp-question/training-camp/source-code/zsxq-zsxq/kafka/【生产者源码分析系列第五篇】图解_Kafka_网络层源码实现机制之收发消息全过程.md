大家好，我是 华仔, 又跟大家见面了。

在上一篇中，主要带大家深度剖析了「**Kafka 对多路复用器 Selector**」的封装全过程，今天我们主要对 **Kafka 网络层收发流程进行总结下**，本系列总共分为3篇，这是下篇，主要剖析最后一个问题：

> 针对 Java NIO 的 SocketChannel，kafka 是如何封装统一的传输层来实现最基础的网络连接以及读写操作的？
> 
> 剖析 KafkaChannel 是如何对传输层、读写 buffer 操作进行封装的？
> 
> 剖析工业级 NIO 实战：如何基于位运算来控制事件的监听以及拆包、粘包是如何实现的？
> 
> 剖析 Kafka 是如何封装 Selector 多路复用器的？
> 
> 剖析 Kafka 封装的 Selector 是如何初始化并与 Broker 进行连接以及网络读写的？
> 
> 剖析 Kafka 网络发送消息和接收响应的整个过程是怎样的？

![](https://article-images.zsxq.com/FqAzS_LpZO1ALGURdwLdYi4zu-_J)

## **01 总体概述**

通过场景驱动的方式，在网络请求封装和监听好后，我们来看看消息是如何进行网络收发的，都需要做哪些工作。

1.  发送消息流程剖析
2.  消息预发送
3.  消息真正发送
4.  接收响应流程剖析
5.  读取响应结果
6.  解析响应信息
7.  处理回调

为了方便大家理解，所有的源码只保留骨干。

## **02 发送消息流程剖析**

## **02.1 消息预发送**

这部分涉及的东西比较多，此处就简单的说明下，后续会有专门篇章进行剖析。

客户端先准备要发送的消息，流程如下：

1.  Sender 子线程会从 RecordAccumulator 缓冲区拉取要发送的消息集合，抽取到的数据会存放到下面几个地方：
2.  **发送时会放入 inFlightRequests 集合和 KafkaChannel 的 send 对象**，其中 inFlightRequests 后续篇章再进行剖析，这里简单说明下，该集合用来存储和操作待发送消息的缓存区，当请求准备网络发送时，会把请求从队头放入队列；当接收到响应后，会把请求从队尾删除。
3.  **待发送完成后会放入 completedRequests 集合**。
4.  对已经过期的数据进行处理。
5.  封装客户端请求 ClientRequest，把 ClientRequest 类对象发送给 NetworkClient，它主要有以下2个工作要做：
6.  根据 ClientRequest 类对象**构造 InFlightRequest 类对象**。
7.  根据 ClientRequest 类对象**构造 NetworkSend 类对象，并放入到 KafkaChannel 的缓存里**。
8.  此时消息预发送结束。

接下来我们依次看下 Selector 和 KafkaChannel 类的具体源码实现。

### **02.1.1 请求数据暂存内存中**

![](https://article-images.zsxq.com/FvkxbAuACtbTe3luC_bvKoSYdW5W)

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/Selector.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/Selector.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/KafkaChannel.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/KafkaChannel.java)

/\*\*

 \* 消息预发送

 \*/

public void send(Send send) {

// 1. 从服务端获取 connectionId

String connectionId \= send.destination();

// 2. 从数据包中获取对应连接

KafkaChannel channel \= openOrClosingChannelOrFail(connectionId);

// 3. 如果关闭连接集合中存在该连接

if (closingChannels.containsKey(connectionId)) {

// 把 connectionId 放入 failedSends 集合里

this.failedSends.add(connectionId);

    } else {

try {

// 4. 暂存数据预发送，并没有真正的发送,一次只能发送一个

            channel.setSend(send);

        } catch (Exception e) {

// 5. 更新 KafkaChannel 的状态为发送失败  

            channel.state(ChannelState.FAILED\_SEND);

// 6. 把 connectionId 放入 failedSends 集合里

this.failedSends.add(connectionId);

// 7. 关闭连接

            close(channel, CloseMode.DISCARD\_NO\_NOTIFY);

            ...

        }

    }

}

从源码中可以看到调用了 KafkaChannel 类的 setSend() 方法。

public void setSend(Send send) {

if (this.send != null)

throw new IllegalStateException("Attempt to begin a send operation with prior send operation still in progress, connection id is " + id);

// 设置要发送消息的字段

this.send = send;

// 调用传输层增加写事件

this.transportLayer.addInterestOps(SelectionKey.OP\_WRITE);

}

// PlaintextTransportLayer 类方法

@Override

public void addInterestOps(int ops) {

//通过 key.interestOps() | ops 来添加事件

    key.interestOps(key.interestOps() | ops);

}

该方法主要用来**预发送，即在发送网络请求前，将需要发送的ByteBuffer 数据保存到 KafkaChannel 的 send 中**，然后调用传输层方法增加对这个 channel 上「**OP\_WRITE**」事件的关注，同时还保留了「**OP\_READ**」事件，此时该 Channel 是同时可以进行读写的。**当真正执行发送的时候，会先从 send 中读取数据。**

## **02.2 消息真正发送**

![](https://article-images.zsxq.com/FuyZEkS4i2LOqH7PP0nP07uefXH6)

Sender 子线程会调用 Selector 的 「**poll**」方法把请求真正发送出去。

### **02.2.1 poll()**

@Override

public void poll(long timeout) throws IOException {

    ...

// 调用nioSelector.select线程阻塞等待I/O事件并设置阻塞时间，等待I/O事件就绪发生，然后返回已经监控到了多少准备就绪的事件

int numReadyKeys \= select(timeout);

// 监听到事件发生或立即连接集合不为空或存在缓存数据

if (numReadyKeys > 0 || !immediatelyConnectedKeys.isEmpty() || dataInBuffers) {

// 在SSL连接才可能会存在缓存数据

if (dataInBuffers) {

// 处理事件

            pollSelectionKeys(toPoll, false, endSelect);

        }

// 处理监听到的准备就绪事件

        pollSelectionKeys(readyKeys, false, endSelect);

// 处理立即连接集合

        pollSelectionKeys(immediatelyConnectedKeys, true, endSelect);

    } else {

        ...

    }

    ...

}

该方法就干了一件事，**即收集准备就绪事件，并针对事件进行网络操作**，通过上述简化代码可以看出是调用了 「**pollSelectionKeys**」 方法，**真正读写操作在该方法中**，我们来看看：

### **02.2.2 pollSelectionKeys()**

void pollSelectionKeys(Set<SelectionKey> selectionKeys,boolean isImmediatelyConnected,long currentTimeNanos) {

//1. 循环调用当前监听到的事件(原顺序或者洗牌后顺序)

for (SelectionKey key : determineHandlingOrder(selectionKeys)) {

// 2. 之前创建连接，把kafkachanel注册到key上，这里就是获取对应的 channel

KafkaChannel channel \= channel(key);

        ...

// 3. 获取节点id

String nodeId \= channel.id();

        ...

try {

            ...

// 4. 读事件是否准备就绪了

if (channel.ready() && (key.isReadable() || channel.hasBytesBuffered()) && !hasCompletedReceive(channel) && !explicitlyMutedChannels.contains(channel)) {

// 尝试处理读事件

                attemptRead(channel);

        }

        ...

try {

// 5. 尝试处理写事件

           attemptWrite(key, channel, nowNanos);

        } catch (Exception e) {

            sendFailed = true;

throw e;

        }

    } catch (Exception e) {

        ...

    } finally {

       ....

    }

  }

}

该方法主要用来**处理监听到的事件，包括连接事件、读写事件、以及立即完成的连接的**。接下来我们看看尝试进行网络写操作，如何才能进行真正写。

### **02.2.3 attemptWrite()**

private void attemptWrite(SelectionKey key, KafkaChannel channel, long nowNanos) throws IOException {

// 此处需要满足4个条件才可以进行写操作      

if (channel.hasSend()

            && channel.ready()

            && key.isWritable()

            && !channel.maybeBeginClientReauthentication(() -> nowNanos)) {

// 进行写操作

        write(channel);

    }

}

// channel 连接就绪

public boolean ready() {

return transportLayer.ready() && authenticator.complete();

}

// java nio SelectionKey

public final boolean isWritable() {

return (readyOps() & OP\_WRITE) != 0;

}

该方法主要用来**尝试进行网络写操作**，方法很简单，必须「**同时满足4个条件**」：

1.  「**channel 还有数据可以发送**」即数据还未发送完成。
2.  「**channel 连接就绪**」。
3.  「**写事件是可写状态**」只要写缓冲区未写满会一直产生「**OP\_WRITE**」 事件，如果不写数据或者写满时则需要取消 「**OP\_WRITE**」 事件，防止产生不必要的资源消耗。
4.  「**客户端验证没有开启**」。

当满足以上4个条件后就可以进行写操作了，接下来我们看看写操作的过程。

### **02.2.4 write()**

// 执行写操作 

void write(KafkaChannel channel) throws IOException {

// 1.获取 channel 对应的节点id    

String nodeId \= channel.id();

// 2. 将保存在 send 上的数据真正发送出去，但是一次不一定能发送完，会返回已经发出的字节数

long bytesSent \= channel.write();

// 3. 判断是否发送完成，未完成返回null，等待下次poll继续发送

Send send \= channel.maybeCompleteSend();

// 4. 说明已经发出或者发送完成

if (bytesSent > 0 || send != null) {

long currentTimeMs \= time.milliseconds();

if (bytesSent > 0)

// 记录发送字节 Metrics 信息

this.sensors.recordBytesSent(nodeId, bytesSent, currentTimeMs);

// 发送完成

if (send != null) {

// 将 send 添加到 completedSends

this.completedSends.add(send);

//  记录发送完成 Metrics 信息

this.sensors.recordCompletedSend(nodeId, send.size(), currentTimeMs);

        }

    }

}

该方法主要用来**真正执行网络写操作的**，大家知道在网络编程过程中，不一定一次性可以发送完成，此时就需要判断是否发送完成，如果未完成返回null，「**等待下次轮询 poll() 会继续发送，并继续关注这个 channel 的写事件**」，如果发送完成，「**则返回 send，并取消 Selector 在这个 socketchannel 上 OP\_WRITE 事件的关注**」。这里调用了 KafkaChannel 类的 write() 进行写操作发送，并调用 maybeCompleteSend() 判断是否发送完成，我们先来看下 write() 写操作：

### **02.2.5 KafkaChannel.wirte()**

public long write() throws IOException {

// 判断 send 是否为空，如果为空表示已经发送完毕了

if (send == null)

return 0;

    midWrite = true;

// 调用ByteBufferSend.writeTo把数据真正发送出去

return send.writeTo(transportLayer);

}

该方法主要用来**把保存在 send 上的数据真正发送出去**，调用 ByteBufferSend.writeTo 把数据真正发送出去，我们来看看 wirteTo() 方法：

@Override

// 将字节流数据写入到channel中

public long writeTo(GatheringByteChannel channel) throws IOException {

// 1.调用nio底层write方法把buffers写入传输层返回写入的字节数

long written \= channel.write(buffers);

if (written < 0)

throw new EOFException("Wrote negative bytes to channel. This shouldn't happen.");

// 2.计算还剩多少字节没有写入传输层

    remaining -= written;

// 每次发送 都检查是否

    pending = TransportLayers.hasPendingWrites(channel);

return written;

}

该方法主要用来**把 buffers 数组写入到 SocketChannel 里**，因为在网络编程中，写一次不一定可以完全把数据都写成功，所以调用java nio 底层 **[channel.write](http://channel.write/)(buffers)** 方法会返回「**已经写入成功多少字节**」的返回值，这样调用一次后就知道已经写入多少字节了。

当调用 write() 以及一系列底层方法进行写操作后，会返回已经发出的字节数，如果这次没有发送完毕则返回 null，「**等待下次轮询 poll 继续发送网络写操作，并继续关注这个 channel 的写事件**」，所以需要判断下本次是否发送完毕了，我们来看看：

### **02.2.7 maybeCompleteSend()**

// 可能完成发送

public Send maybeCompleteSend() {

// send 不为空且已经发送完毕

if (send != null && send.completed()) {

        midWrite = false;

// 当写数据完毕后，取消传输层对 OP\_WRITE 事件的监听，完成一次写操作

        transportLayer.removeInterestOps(SelectionKey.OP\_WRITE);

// 将 send 赋值给结果集 result

Send result \= send;

// 此时读完后将 send 清空，以便下次写

        send = null;

// 最后返回结果集 result，完成一次写操作

return result;

    }

return null;

}

// PlaintextTransportLayer 类方法

@Override

public void removeInterestOps(int ops) {

// 通过 key.interestOps() & ~ops 来删除事件

    key.interestOps(key.interestOps() & ~ops);

}

// ByteBufferSend

@Override

public boolean completed() {

return remaining <= 0 && !pending;

}

该方法主要用来**判断是否写数据完毕了**，而判断的写数据完毕的条件是 **buffer 中 remaining 没有剩余且 pending 为 false**。如果发送完成，把发送完成的请求添加到发送完成的集合 completedSends 里。

待消息请求发送完成后，又做了哪些工作呢？这里涉及到 NetworkClient 类的相关知识，这里简单说明下，后续再剖析：

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/NetworkClient.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/NetworkClient.java)

private void handleCompletedSends(List<ClientResponse> responses, long now) {

// if no response is expected then when the send is completed, return it

// 上面发送完成将 send 添加到 completedSends 集合，然后遍历这个集合

for (Send send : this.selector.completedSends())     {

// 获取 inFlightRequests 集合发往对应 Broker 的最后一个请求元素

InFlightRequest request \= this.inFlightRequests.lastSent(send.destination());

// 判断是否期望进行响应

if (!request.expectResponse) {

// 如果不期望进行响应就删除inFlightRequests集合发往对应 Broker 请求队列的第一个元素

this.inFlightRequests.completeLastSent(send.destination());

// 把请求添加到 responses 集合里

            responses.add(request.completed(null, now));

        }

    }

}

从源码可以看出会对「**completedSends**」集合和「**inFlightRequests**」集合是一个「**互相协作**」的关系。

其中「**completedSends**」集合是指发送完成但还没有返回的请求集合，而「**inFlightRequests**」集合则是保存了已经发送出去但还没有收到响应结果的 Request 集合。其中「**completedSends**」的元素对应着「**inFlightRequests**」集合对应队列的最后一个元素。

到此发送消息流程剖析完毕，**至于发送完成后续工作，我们待讲解 Sender 和 NetWorkClient 的时候再详细进行剖析**，接下来我们来看看接收响应流程。

## **03 接收响应流程剖析**

![](https://article-images.zsxq.com/Fs24T0fokE9RbsFPoiHvKISTsZjs)

在上面剖析 Selector.pollSelectionKeys() 时候，当网络读事件就绪后会调用 attemptRead() 进行尝试网络读操作，我们来看看：

## **03.1 读取响应结果**

### **03.1.1 attemptRead()**

private void attemptRead(KafkaChannel channel) throws IOException {

// 获取 channel 对应的节点 id

String nodeId \= channel.id();

// 将从传输层中读取数据到NetworkReceive对象中

long bytesReceived \= channel.read();

if (bytesReceived != 0) {

        ...

// 判断 NetworkReceive 对象是否已经读完了

NetworkReceive receive \= channel.maybeCompleteReceive();

// 当读完后把这个 NetworkReceive 对象添加到已经接收完毕网络请求集合里

if (receive != null) {

            addToCompletedReceives(channel, receive, currentTimeMs);

        }

    }

    ...

}

// KafkaChannel 方法

public long read() throws IOException {

if (receive == null) {

// 初始化 NetworkReceive 对象

        receive = new NetworkReceive(maxReceiveSize, id, memoryPool);

    }

// 尝试把 channel 的数据读到 NetworkReceive 对象中

long bytesReceived \= receive(this.receive);

    ...

return bytesReceived;

}

该方法主要用来**尝试读取数据并添加已经接收完毕的集合中**。我们看到会先调用 [KafkaChannel.read](http://kafkachannel.read/)() 方法进行读取，然后判断是否读完了，**如果没有读完，下次轮询时候接着读取，如果读完了就假如到请求读完的集合 completedReceives 中**。

我们来看下是如何判断 NetworkReceive 对象是否已经读完了的：

### **03.1.2 maybeCompleteReceive()**

// 判断 NetworkReceive 对象是否已经读完了

// 如果此时并没有读完一个完整的NetworkReceive对象,则下次触发读事件会继续填充整个NetworkReceive对象，

// 如果读完一个完整的NetworkReceive对象则将其置空，下次触发读事件时会创建一个全新的NetworkReceive对象。

public NetworkReceive maybeCompleteReceive() {

if (receive != null && receive.complete()) {

      receive.payload().rewind();

NetworkReceive result \= receive;

      receive = null;

return result;

  }

return null;

}

// NetworkReceive

public boolean complete() {

return !size.hasRemaining() && buffer != null && !buffer.hasRemaining();

}

该方法主要用来**判断数据已经读取完毕了**，而判断是否读完的条件是 **NetworkReceive 里的 buffer 是否用完**，包括上面说过的表示响应消息头 size ByteBuffer 和响应消息体本身的 buffer ByteBuffer，这两个都读完才算真正读完了。

**如果此时并没有读完一个完整的 NetworkReceive 对象,则下次触发读事件会继续填充整个 NetworkReceive 对象，如果此时读完一个完整的NetworkReceive 对象则将其置空，下次触发读事件时会创建一个全新的NetworkReceive 对象**。

## **03.2 解析响应消息**

等读取完一个完整响应消息后，接下来要做哪些工作呢？那就是要解析这个响应消息，我们来看看是如何实现的：

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/NetworkClient.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/clients/NetworkClient.java)

private void handleCompletedReceives(List<ClientResponse> responses, long now) {

// 当读完后把这个 NetworkReceive 对象添加到已经接收完毕网络请求集合里，然后遍历这个集合

for (NetworkReceive receive : this.selector.completedReceives()) {

// 获取发送请求的node id 

String source \= receive.source();

// 从 InFlightRequest 集合取出对应的元素并删除

InFlightRequest req \= inFlightRequests.completeNext(source);

// 解析该响应

Struct responseStruct \= parseStructMaybeUpdateThrottleTimeMetrics(receive.payload(), req.header,

          throttleTimeSensor, now);

      ....

// 添加响应到响应结果集合中

      responses.add(req.completed(response, now));

  }

}

该方法主要用来**循环遍历 completedReceives 集合做一些响应处理工作**，在文章开始的时候就简单说过，收到响应后会将其从「**inFlightRequests**」中删除掉，然后去解析这个响应：

private static Struct parseStructMaybeUpdateThrottleTimeMetrics(ByteBuffer responseBuffer, RequestHeader requestHeader,Sensor throttleTimeSensor, long now) {

// 获取响应头

ResponseHeader responseHeader \= ResponseHeader.parse(responseBuffer,requestHeader.apiKey().responseHeaderVersion(requestHeader.apiVersion()));

// 获取响应体

Struct responseBody \= requestHeader.apiKey().parseResponse(requestHeader.apiVersion(), responseBuffer);

// 对比响应头 correlationId 和响应体的 correlationId 是否一致，否则抛异常

    correlate(requestHeader, responseHeader);

    ...

return responseBody;

}

该方法主要用来**解析响应的**，并判断响应头跟响应体的 correlationId 值是否一致，否则抛异常。

此时只对响应做了解析但并没有对响应进行处理，而响应处理是通过调用回调方法进行处理的，我们来看下。

## **03.3 处理回调**

private void completeResponses(List<ClientResponse> responses) {

// 遍历响应结果集合

for (ClientResponse response : responses) {

try {

            response.onComplete();

        } catch (Exception e) {

            log.error("Uncaught error in request completion:", e);

        }

    }

}

//ClientResponse 类

public void onComplete() {

if (callback != null)

        callback.onComplete(this);

}

到此接收响应消息流程剖析完毕。

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、带你先整体的梳理了 Kafka 网络层收发流程，主要分为「**发送消息流程**」和「**接收响应流程**」。

2、又带你分别剖析了发送消息流程和接收响应流程的源码实现细节。

源码详细分析请看上两篇：

[【生产者源码分析系列第四篇】图解 Kafka 网络层实现机制之上篇](https://articles.zsxq.com/id_f95tmqef8ueb.html)

[【生产者源码分析系列第五篇】图解 Kafka 网络层实现机制之Selector 多路复用器](https://articles.zsxq.com/id_r3td0dogfg0v.html)