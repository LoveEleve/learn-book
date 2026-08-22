大家好，我是 华仔, 又跟大家见面了。

在上一篇中，主要带大家深度剖析了「**Kafka 对 NIO SocketChannel、Buffer**」的封装全过程，今天我们接着聊聊 **Kafka 是如何封装 Selector 多路复用器的**，本系列总共分为3篇，今天是中篇，主要剖析4、5两个问题：

1.  针对 Java NIO 的 SocketChannel，kafka 是如何封装统一的传输层来实现最基础的网络连接以及读写操作的？
2.  剖析 KafkaChannel 是如何对传输层、读写 buffer 操作进行封装的？
3.  剖析工业级 NIO 实战：如何基于位运算来控制事件的监听以及拆包、粘包是如何实现的？
4.  剖析 Kafka 是如何封装 Selector 多路复用器的？
5.  剖析 Kafka 封装的 Selector 是如何初始化并与 Broker 进行连接以及网络读写的？
6.  剖析 Kafka 网络发送消息和接收响应的整个过程是怎样的？

![](https://article-images.zsxq.com/FuLyOkO6GZHtdIwqJqLk7NzXDM7V)

## **01 总体概述**

大家都知道在 Java NIO 有个三剑客，即「**SocketChannel通道**」、「**Buffer读写**」、「**Selector多路复用器**」，上篇已经讲解了前2个角色，今天我们来聊聊最后一个重要的角色。

Kafka Selector 是对 Java NIO Selector 的二次封装，主要功能如下：

> 提供网络连接以及读写操作
> 
> 对准备好的事件进行收集并进行网络操作

为了方便大家理解，所有的源码只保留骨干。

## **02 Selector 封装过程**

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/Selector.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/Selector.java)

[org.apache.kafka.common.network.Selector](http://org.apache.kafka.common.network.selector/)，该类是 Kafka 网络层最重要最核心的实现，也是非常经典的工业级通信框架实现，为了简化，这里称为 Kselector， 接下来我们先来看看该类的重要属性字段：

public class Selector implements Selectable, AutoCloseable {

// 在 Java NIO 中用来监听网络I/O事件

private final java.nio.channels.Selector nioSelector;

// channels 管理

private final Map<String, KafkaChannel> channels; 

// 发送完成的Send集合

private final List<Send> completedSends;

// 已经接收完毕的请求集合

private final LinkedHashMap<String, NetworkReceive> completedReceives;

// 立即连接的集合

private final Set<SelectionKey> immediatelyConnectedKeys;

// 关闭连接的 channel 集合

private final Map<String, KafkaChannel> closingChannels;

// 断开连接的节点集合

private final Map<String, ChannelState> disconnected;

// 连接成功的节点集合

private final List<String> connected;

// 发送失败的请求集合

private final List<String> failedSends;

// 用来构建 KafkaChannel 的工具类

private final ChannelBuilder channelBuilder;

// 最大可以接收的数据量大小

private final int maxReceiveSize;

// 空闲超时到期连接管理器

private final IdleExpiryManager idleExpiryManager;

// 用来管理 ByteBuffer 的内存池

private final MemoryPool memoryPool;

// 初始化 Selector

public Selector(int maxReceiveSize,

            long connectionMaxIdleMs,

            int failedAuthenticationDelayMs,

            Metrics metrics,

            Time time,

            String metricGrpPrefix,

            Map<String, String> metricTags,

            boolean metricsPerConnection,

            boolean recordTimePerConnection,

            ChannelBuilder channelBuilder,

            MemoryPool memoryPool,

            LogContext logContext) {

try {

this.nioSelector = java.nio.channels.Selector.open();

        } catch (IOException e) {

throw new KafkaException(e);

        }

this.maxReceiveSize = maxReceiveSize;

this.time = time;

this.channels = new HashMap<>();

this.explicitlyMutedChannels = new HashSet<>();

this.outOfMemory = false;

this.completedSends = new ArrayList<>();

this.completedReceives = new LinkedHashMap<>();

this.immediatelyConnectedKeys = new HashSet<>();

this.closingChannels = new HashMap<>();

this.keysWithBufferedRead = new HashSet<>();

this.connected = new ArrayList<>();

this.disconnected = new HashMap<>();

this.failedSends = new ArrayList<>();

this.log = logContext.logger(Selector.class);

this.sensors = new SelectorMetrics(metrics, metricGrpPrefix, metricTags, metricsPerConnection);

this.channelBuilder = channelBuilder;

this.recordTimePerConnection = recordTimePerConnection;

this.idleExpiryManager = connectionMaxIdleMs < 0 ? null : new IdleExpiryManager(time, connectionMaxIdleMs);

this.memoryPool = memoryPool;

this.lowMemThreshold = (long) (0.1 \* this.memoryPool.size());

this.failedAuthenticationDelayMs = failedAuthenticationDelayMs;

this.delayedClosingChannels = (failedAuthenticationDelayMs > NO\_FAILED\_AUTHENTICATION\_DELAY) ? new LinkedHashMap<String, DelayedAuthenticationFailureClose>() : null;

    }

}

重要字段如下所示:

1.  **nioSelector：** 在 Java NIO 中用来监听网络I/O事件。
2.  **channels：** 用来进行管理客户端到各个Node节点的网络连接，Map 集合类型 <Node节点id, KafkaChannel>
3.  **completedSends：** 已经发送完成的请求对象 Send 集合，List 集合类型。
4.  **completedReceives：** 已经接收完毕的网络请求集合，LinkedHashMap 集合类型 <ChannelId, NetworkReceive>，其中 value 都是已经接收完毕的 NetworkReceive 类对象。
5.  **immediatelyConnectedKeys：** 立即连接key集合。
6.  **closingChannels：** 关闭连接的 channel 集合。
7.  **disconnected：** 断开连接的集合。Map 集合类型 <ChannelId, ChannelState>，value 是 KafkaChannel 的状态，可以在使用的时候可以通过这个 ChannelState 状态来判断处理逻辑。
8.  **connected：** 成功连接的集合，List 集合类型，存储成功请求的 ChannelId。
9.  **failedSends：** 发送失败的请求集合，List 集合类型， 存储失败请求的 ChannelId。
10.  **channelBuilder：** 用来构建 KafkaChannel 的工具类。
11.  **maxReceiveSize：** 最大可以接收的数据量大小。
12.  **idleExpiryManager：** 空闲超时到期连接管理器。
13.  **memoryPool：** 用来管理 ByteBuffer 的内存池，分配以及回收。

介绍完字段后，我们来看看该类的方法。方法比较多，这里深度剖析下其中几个重要方法，通过学习这些方法的不仅可以复习下 Java NIO 底层组件，另外还可以学到 Kafka 封装这些底层组件的实现思想。

NetworkClient 的请求一般都是交给 Kselector 去处理并完成的。而 Kselector 使用 NIO 异步非阻塞模式负责具体的连接、读写事件等操作。

我们先看下连接过程，客户端在和节点连接的时候，会创建和服务端的 SocketChannel 连接通道。Kselector 维护了每个目标节点对应的 KafkaChannel。

如下图所示：

![](https://article-images.zsxq.com/FumSQ73pOvxrBt49QHs5wpltkf6P)

## **02.1 connect()**

@Override

public void connect(String id, InetSocketAddress address, int sendBufferSize, int receiveBufferSize) throws IOException {

// 1.先确认是否已经被连接过  

    ensureNotRegistered(id);

// 2.打开一个 SocketChannel

SocketChannel socketChannel \= SocketChannel.open();

SelectionKey key \= null;

try {

// 3.设置 socketChannel 信息 

        configureSocketChannel(socketChannel, sendBufferSize, receiveBufferSize);

// 4.尝试发起连接

boolean connected \= doConnect(socketChannel, address);

// 5. 将该 socketChannel 注册到 nioSelector 上，并关注 OP\_CONNECT 事件

        key = registerChannel(id, socketChannel, SelectionKey.OP\_CONNECT);

// 6.如果立即连接成功了

if (connected) {

            ...

// 先将 key 放入 immediatelyConnectedKeys 集合

            immediatelyConnectedKeys.add(key);

// 并取消对 OP\_CONNECT 的监听

            key.interestOps(0);

        }

    } catch (IOException | RuntimeException e) {

if (key != null)

            immediatelyConnectedKeys.remove(key);

        channels.remove(id);

        socketChannel.close();

throw e;

    }

}

// 设置 socketChannel 信息 

private void configureSocketChannel(SocketChannel socketChannel, int sendBufferSize, int receiveBufferSize) throws IOException {

// 1. 设置非阻塞模式

    socketChannel.configureBlocking(false);

// 2. 创建一个新的 Socket

Socket socket \= socketChannel.socket();

// 3. 开启长连接 keepalive 探活机制

    socket.setKeepAlive(true);

// 4. 设置 SocketOptions.SO\_SNDBUF，默认12kb

if (sendBufferSize != Selectable.USE\_DEFAULT\_BUFFER\_SIZE)

        socket.setSendBufferSize(sendBufferSize);

// 5. 设置 SocketOptions.SO\_RCVBUF，默认32kb

if (receiveBufferSize != Selectable.USE\_DEFAULT\_BUFFER\_SIZE)

        socket.setReceiveBufferSize(receiveBufferSize);

// 6. 设置 TcpNoDelay 算法，默认为 false 即开启 Nagle 算法，true 为关闭 Nagle 算法

    socket.setTcpNoDelay(true);

}

// 发起连接

protected boolean doConnect(SocketChannel channel, InetSocketAddress address) throws IOException {

try {

// 调用socketChannel的connect方法进行发起连接，该方法会向远端发起tcp请求

// 因为是非阻塞的，返回时，连接不一定已经建立好（即完成3次握手）。连接如果已经建立好则返回true，否则返回false。

//一般来说server和client在一台机器上，该方法可能返回true。在后面会通过 KSelector.finishConnect() 方法确认连接是否真正建立了。

return channel.connect(address);

    } catch (UnresolvedAddressException e) {

throw new IOException("Can't resolve address: " + address, e);

    }

}

该方法主要是用来**发起网络连接**，连接过程大致分为如下六步：

1.  先确认是否已经被连接过，即是否已经存在于连接成功集合或正在关闭连接的集合里，如果存在说明连接已经存在或者关闭了，就不应再次发起连接。
2.  打开一个 SocketChannel，创建一个连接。
3.  设置 SocketChannel 信息。其中包括设置「**非阻塞模式**」、「**长链接探活机制**」、「**SocketOptions.SO\_SNDBUF 大小**」、「**SocketOptions.SO\_RCVBUF 大小**」、「**关闭 Nagle 算法**」等，其中 SO\_SNDBUF、SO\_RCVBUF 表示内核发送和接收数据缓存的大小。
4.  尝试发起连接，由于是设置为非阻塞，调用完方法会直接返回，「**此时连接不一定已经建立了**」。当然也可能立即就连接上了，如果立即连接上返回值为true，没立即连接上返回值为false。
5.  将该 socketChannel 注册到 nioSelector 上，并关注 OP\_CONNECT 事件，如果上一步没立即连接上，还需要继续监听 OP\_CONNECT 事件，等连接上了再做处理。
6.  如果立即连接成功了，先将 key 放入 immediatelyConnectedKeys 集合，然后取消对 OP\_CONNECT 的监听。此时已经连接成功了就没必要在监听 OP\_CONNECT 事件了。

**这里需要注意下：** 因为是非阻塞方式，所以 channel.connect() 发起连接，「**可能在正式建立连接前就返回了**」，为了确定连接是否建立，需要再调用 「**finishConnect**」 确认完全连接上了。

## **02.2 registerChannel()**

// 将该 socketChannel 注册到 nioSelector 上，并关注感兴趣的事件

protected SelectionKey registerChannel(String id, SocketChannel socketChannel, int interestedOps) throws IOException {

// 1. 将该 socketChannel 注册到 nioSelector 上，并设置读事件监听

SelectionKey key \= socketChannel.register(nioSelector, interestedOps);

// 2. 构建 KafkaChannel，将 key 与 KafkaChannel 做注册绑定

KafkaChannel channel \= buildAndAttachKafkaChannel(socketChannel, id, key);

// 3. 将nodeid，channel 绑定并放入到 channels 集合中

this.channels.put(id, channel);

if (idleExpiryManager != null)

// 4. 更新连接到空闲超时到期连接管理器中，并记录活跃时间

            idleExpiryManager.update(channel.id(), time.nanoseconds());

return key;

}

// 构建 KafkaChannel 并关联 key 和 Channel，方便查找

private KafkaChannel buildAndAttachKafkaChannel(SocketChannel socketChannel, String id, SelectionKey key) throws IOException {

try {

// 1. 构建 KafkaChannel

KafkaChannel channel \= channelBuilder.buildChannel(id, key, maxReceiveSize, memoryPool,

new SelectorChannelMetadataRegistry());

// 2. 将 KafkaChannel 注册到 key 上，并做关联，方便查找

        key.attach(channel);

// 3. 返回建立好的 KafkaChannel

return channel;

    } catch (Exception e) {

try {

            socketChannel.close();

        } finally {

            key.cancel();

        }

throw new IOException("Channel could not be created for socket " + socketChannel, e);

    }

}

该方法主要用来**注册和绑定连接的**，过程如下：

1.  将该 socketChannel 注册到 nioSelector 上，并设置读事件监听。
2.  构建 KafkaChannel，以及将 key 与 KafkaChannel 做关联绑定，方便查找，既可以通过 key 找到 channel，也可以通过 channel 找到 key。

讲解完建立连接后，我们来看看消息发送的相关方法。

[KSelector.send](http://kselector.send/)() 方法是将之前创建的 RequestSend 对象先缓存到 KafkaChannel 的 send 字段中，并关注此连接的 OP\_WRITE 事件，并没有真正发生网络 I/O 操作。**会在下次调用 [KSelector.poll](http://kselector.poll/)() 时，才会将 RequestSend 对象发送出去**。

如果此 KafkaChannel 的 send 字段上还保存着一个未完全发送成功的 RequestSend 请求，为了防止覆盖，会抛出异常。**每个 KafkaChannel 一次 poll 过程中只能发送一个 Send 请求**。

客户端的请求 Send 会被设置到 KafkaChannel 中，**KafkaChannel 的 TransportLayer 会为 SelectionKey 注册 OP\_WRITE 事件**。

此时 Channel 的 SelectionKey 就有了 OP\_CONNECT、OP\_WRITE 事件，**在 Kselector 的轮询过程中当发现这些事件准备就绪后，就开始执行真正的操作**。

基本流程就是：

![](https://article-images.zsxq.com/FnjB-ZE6VxmgYA7KSM8Z4La9ofXN)

## **02.3 send()**

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

// 判断 channelid 是否存在

private KafkaChannel openOrClosingChannelOrFail(String id) {

// 通过 channelid 先从 channels 集合中获取

KafkaChannel channel \= this.channels.get(id);

// 如果为空那么再从 closingChannels 集合中获取

if (channel == null)

        channel = this.closingChannels.get(id);

// 如果还为空则抛异常    

if (channel == null)

throw new IllegalStateException("Attempt to retrieve channel for which there is no connection. Connection id " + id + " existing connections " + channels.keySet());

return channel;

}

该方法主要用来**消息预发送，即在发送的时候把消息线暂存在 KafkaChannel 的 send 字段里，然后等着 poll() 执行真正的发送**，过程如下：

1.  从服务端获取 connectionId。
2.  从 channels 或 closingChannels 集合中找对应的 KafkaChannel，如果都为空就抛异常。
3.  如果关闭连接 closingChannels 集合中存在该连接，说明连接还没有被建立，则把连接放到发送失败 failedSends 的集合中。
4.  否则即是连接建立成功，「**就把要发送的数据先保存在 send 字段里暂存起来，等待后续 poll() 去调用真正的发送**」。
5.  如果暂存异常后，则更新 KafkaChannel 的状态为发送失败。
6.  把 connectionId 放入 failedSends 集合里。
7.  最后关闭连接。

讲完消息预发送，接下来我们来看看最核心的 poll 和 pollSelectionKeys 方法。

在 Kselector 的轮询中可以操作连接事件、读写事件等，**是真正执行网络I/O事件操作的地方**，它会调用 [nioSelector.select](http://nioselector.select/)() 方法等待 I/O 事件就绪。

当 Channel 可写时，发送 [KafkaChannel.send](http://kafkachannel.send/) 字段，「**一次最多只发送一个 RequestSend,有时候一个 RequestSend 也发送不完，需要多次 poll 才能发送完成**」。

当 Channel 可读时，读取数据到 KafkaChannel.receive，「**当读取一个完整的 NetworkReceive ，并在一次 pollSelectionKeys() 完成后会将 NetworkReceive 中的数据转移到 completedReceives 集合中**」。

最后调用 maybeCloseOldestConnection() 方法，根据 lruConnections 记录，设置 channel 状态为过期，并关闭长期空闲的连接。

## **02.4 poll()**

@Override

public void poll(long timeout) throws IOException {

    ...

// 1. 先将上次的结果清理掉

    clear();

boolean dataInBuffers \= !keysWithBufferedRead.isEmpty();

    ...

/\* check ready keys \*/

long startSelect \= time.nanoseconds();

// 2. 调用nioSelector.select线程阻塞等待I/O事件并设置阻塞时间，等待I/O事件就绪发生，然后返回已经监控到了多少准备就绪的事件

int numReadyKeys \= select(timeout);

long endSelect \= time.nanoseconds();

// 记录耗时

this.sensors.selectTime.record(endSelect - startSelect, time.milliseconds());

// 3. 监听到事件发生或立即连接集合不为空或存在缓存数据

if (numReadyKeys > 0 || !immediatelyConnectedKeys.isEmpty() || dataInBuffers) {

// 4. 获取监听到的准备就绪事件集合 

        Set<SelectionKey> readyKeys = this.nioSelector.selectedKeys();

// 在SSL连接才可能会存在缓存数据

if (dataInBuffers) {

// 清除所有的就绪事件

            keysWithBufferedRead.removeAll(readyKeys); //so no channel gets polled twice

            Set<SelectionKey> toPoll = keysWithBufferedRead;

// 重新初始化

            keysWithBufferedRead = new HashSet<>(); //poll() calls will repopulate if needed

// 处理事件

            pollSelectionKeys(toPoll, false, endSelect);

        }

// 5. 处理监听到的准备就绪事件

        pollSelectionKeys(readyKeys, false, endSelect);

// 6. 就绪事件集合清理

// Clear all selected keys so that they are included in the ready count for the next select

        readyKeys.clear();

// 7. 处理立即连接集合

        pollSelectionKeys(immediatelyConnectedKeys, true, endSelect);

// 8. 立即连接集合清理

        immediatelyConnectedKeys.clear();

        ...

        maybeCloseOldestConnection(endSelect);

    } else {

        ...

    }

    ...

}

// 调用nioselector.select进行阻塞监听就绪事件

private int select(long timeoutMs) throws IOException {

if (timeoutMs < 0L)

throw new IllegalArgumentException("timeout should be >= 0");

if (timeoutMs == 0L)

return this.nioSelector.selectNow();

else

return this.nioSelector.select(timeoutMs);

}

该方法主要用来**实现网络操作的，即收集准备就绪事件，并针对事件进行网络操作**，具体的过程如下：

1.  上来先将上次的结果清理掉，大概包括「**completedSends**」、「**connected**」、「**disconnected**」、「**failedSends**」、「**completedReceives**」、「**请求发送或者接受完毕关闭通道**」、「**记录失败状态到disconnected**」等。
2.  调用[nioSelector.select](http://nioselector.select/)线程阻塞等待I/O事件并设置阻塞时间，等待I/O事件就绪发生，然后返回已经监控到了多少准备就绪的事件。
3.  判断是否可以处理网络事件，三个条件满足其一就可以处理：「**监听到事件发生**」、「**立即连接集合不为空**」、「**存在缓存数据**」，其中最后一个是在加密SSL连接才可能有的。
4.  获取监听到的准备就绪事件集合。
5.  调用 pollSelectionKeys() 处理监听到的准备就绪的事件集合，包括「**连接事件**」、「**网络读写事件**」。其中读完的请求放入 completedReceives 集合，写完的响应放入 completedSends 集合，连接成功的放入 connected集合，断开的连接放入 disconnected 集合等。
6.  清除所有选定的键，以便它们包含在下一次选择的就绪计数中。
7.  调用 pollSelectionKeys() 处理立即连接集合。「**这个集合的元素都是一开始做连接就立即连接上的，等待被处理**」。
8.  立即连接集合清理。

## **02.5 pollSelectionKeys()**

void pollSelectionKeys(Set<SelectionKey> selectionKeys,boolean isImmediatelyConnected,long currentTimeNanos) {

//1. 循环调用当前监听到的事件(原顺序或者洗牌后顺序)

for (SelectionKey key : determineHandlingOrder(selectionKeys)) {

// 2. 之前创建连接，把kafkachanel注册到key上，这里就是获取对应的 channel

KafkaChannel channel \= channel(key);

        ...

boolean sendFailed \= false;

// 3. 获取节点id

String nodeId \= channel.id();

        ...

// 4. 更新连接到空闲超时到期连接管理器中，并记录活跃时间

if (idleExpiryManager != null)

            idleExpiryManager.update(nodeId, currentTimeNanos);

try {

// 5. 判断是否可以处理连接事件

if (isImmediatelyConnected || key.isConnectable()) {

//判断连接是否已经建立成功，成功后关注OP\_READ 事件，取消 OP\_CONNECT 事件

//socketChannel是否建立完成(connect是异步的，所以connect方法返回后不一定已经连接成功了)

if (channel.finishConnect()) {

// 添加节点到连接成功集合中

this.connected.add(nodeId);

                    ...

// 获取 socketChannel

SocketChannel socketChannel \= (SocketChannel) key.channel();

                    ...

                } else {

// 连接没有建立完成，下一轮再尝试

continue;

                }

            }

// 6. 如果没有准备就绪就开始准备，即处理 tcp 连接还未完成的连接,进行传输层的握手及认证

if (channel.isConnected() && !channel.ready()) {

// 进行连接准备并进行身份验证

                channel.prepare();

if (channel.ready()) {

                   ...

// 准备好后记录 Metrics 信息

                }

            }

// 7. 如果channel准备就绪，但是状态还是未连接

if (channel.ready() && channel.state() == ChannelState.NOT\_CONNECTED)

// 将状态改为准备就绪

                channel.state(ChannelState.READY);

               ...

// 8. 读事件是否准备就绪了

if (channel.ready() && (key.isReadable() || channel.hasBytesBuffered()) && !hasCompletedReceive(channel) && !explicitlyMutedChannels.contains(channel)) {

// 尝试处理读事件

                attemptRead(channel);

        }

        ...

try {

// 9. 尝试处理写事件

           attemptWrite(key, channel, nowNanos);

        } catch (Exception e) {

            sendFailed = true;

throw e;

        }

/\* cancel any defunct sockets \*/

if (!key.isValid())

// 10.如果连接失效，关闭连接

            close(channel, CloseMode.GRACEFUL);

    } catch (Exception e) {

        ...

    } finally {

        maybeRecordTimePerConnection(channel, channelStartTimeNanos);

    }

 }

}

// 当内存不足时会对selectorKey进行洗牌

private Collection<SelectionKey> determineHandlingOrder(Set<SelectionKey> selectionKeys) {

if (!outOfMemory && memoryPool.availableMemory() < lowMemThreshold) {

        List<SelectionKey> shuffledKeys = new ArrayList<>(selectionKeys);

// 洗牌

        Collections.shuffle(shuffledKeys);

return shuffledKeys;

    } else {

return selectionKeys;

    }

}

// kafkaChannel类方法，判断连接是否已经建立完成

public boolean finishConnect() throws IOException {

SocketChannel socketChannel \= transportLayer.socketChannel();

if (socketChannel != null) {

        remoteAddress = socketChannel.getRemoteAddress();

    }

boolean connected \= transportLayer.finishConnect();

if (connected) {

if (ready()) {

            state = ChannelState.READY;

        } else if (remoteAddress != null) {

            state = new ChannelState(ChannelState.State.AUTHENTICATE, remoteAddress.toString());

        } else {

            state = ChannelState.AUTHENTICATE;

        }

    }

return connected;

}

// kafkaChannel类方法，连接准备

public void prepare() throws AuthenticationException, IOException {

boolean authenticating \= false;

try {

// 通过没有ready

if (!transportLayer.ready())

// 进行握手操作

            transportLayer.handshake();

// 已经ready 但是没有认证

if (transportLayer.ready() && !authenticator.complete()) {

// 开启认证中标识

            authenticating = true;

// 开始认证

            authenticator.authenticate();

        }

    } catch (AuthenticationException e) {

        ...

throw e;

    }

// 准备好以后

if (ready()) {

// 计算认证成功数

        ++successfulAuthentications;

// 状态改为ready

        state = ChannelState.READY;

    }

}

// kafkaChannel类方法，判断 channel 是否就绪

public boolean ready() {

return transportLayer.ready() && authenticator.complete();

}

// kafkaChannel类方法，判断 channel 是否有缓存数据

public boolean hasBytesBuffered() {

return transportLayer.hasBytesBuffered();

}

//PlaintextTransportLayer 方法，明文传输永远返回true

public boolean ready() {

return true;

}

//PlaintextTransportLayer 方法，明文传输永远返回true

public boolean hasBytesBuffered() {

return false;

}

该方法是用来**处理监听到的事件，包括连接事件、读写事件、以及立即完成的连接的**。具体过程如下：

1.  循环调用当前监听到的事件「**连接事件**」、(原顺序或者内存不足时洗牌后顺序)。
2.  获取对应的 channel。
3.  从 channel 中获取节点id。
4.  如果空闲超时到期连接管理器不为空，则更新连接到空闲超时到期连接管理器中，并记录活跃时间。
5.  判断是否可以处理连接事件，有两个判断条件：「**立即连接完成 isImmediatelyConnected**」、「**连接事件准备好 key.isConnectable**」。满足其一就要处理连接事件了。
6.  调用 finishConnect 方法判断连接是否已经建立完成，如果连接成功了，就关注「**OP\_READ 事件**」、取消 「**OP\_CONNECT 事件**」，然后做好接收数据的准备。
7.  如果连接没有建立完成，那么下一轮再尝试。
8.  如果没有准备就绪就处理 tcp 连接还未完成的连接，并进行传输层的握手以及身份认证，最后返回连接ready，准备好后记录 Metrics 信息。
9.  如果channel准备就绪，但是状态还是未连接，修改状态为ready 准备就绪。
10.  判断读事件操作是否准备就绪了。此时要「**同时满足4个条件**」才算读操作准备就绪了，然后尝试处理读事件：
11.  「**channel已经准备就绪**」，这里对于明文连接都是true，所以我们不用关心。
12.  「**读事件已经就绪或者 channel 中有缓存数据**」，而 channel 里有缓存数据对于明文传输连接永远是 false，也不用关心
13.  「**NetworkReceive 对象没有被读完**」还要继续读。
14.  「**加锁 channels 集合中不存在该channel**」，服务端用来处理消息重复的。
15.  尝试处理写事件。
16.  最后如果如果连接失效，则关闭连接。

讲解完最核心的 poll() 和 pollSelectionKeys() 方法后，我们来看看「**网络读写事件**」的处理过程。

## **02.6 attemptWrite()**

private void attemptWrite(SelectionKey key, KafkaChannel channel, long nowNanos) throws IOException {

// 

if (channel.hasSend()

            && channel.ready()

            && key.isWritable()

            && !channel.maybeBeginClientReauthentication(() -> nowNanos)) {

// 进行写操作

        write(channel);

    }

}

该方法用来**判断尝试进行写操作**，方法很简单，必须「**同时满足4个条件**」：

1.  「**还有数据可以发送**」
2.  「**channel 连接就绪**」
3.  「**写事件是可写状态**」
4.  「**客户端验证没有开启**」

当满足以上4个条件后就可以进行写操作了，接下来我们看看写操作的过程。

## **02.7 write()**

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

该方法用来**真正执行写操作**，数据就是上面send()方法被填充的send字段。具体过程如下：

1.  获取 channel 对应的节点id。
2.  将保存在 send 上的数据真正发送出去，但是「**一次不一定能发送完**」，会返回已经发出的字节数。
3.  判断是否发送完成
4.  如果未发送完成返回 null，「**等待下次 poll 继续发送**」，并继续关注这个 channel 的写事件。
5.  如果发送完成，则返回 send，并取消对写事件的关注。
6.  发送完成，将 send 添加到 completedSends 集合中。

接下来我们来看看读操作过程。

## **02.8 attemptRead()**

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

/\*\*

 \* adds a receive to completed receives

 \*/

private void addToCompletedReceives(KafkaChannel channel, NetworkReceive networkReceive, long currentTimeMs) {

if (hasCompletedReceive(channel))

throw new IllegalStateException("Attempting to add second completed receive to channel " + channel.id());

// 将 networkReceive 添加到已经接收完毕网络请求集合里 

this.completedReceives.put(channel.id(), networkReceive);

    ...

}

该方法主要用来**尝试读取数据并添加已经接收完毕的集合中**。

1.  先从 channel 中获取节点id。
2.  然后调用 [channel.read](http://channel.read/)() 方法从传输层中读取数据到 NetworkReceive 对象中。
3.  判断本次是否已经读完了即填满了 NetworkReceive 对象，如果没有读完，那么下次触发读事件的时候继续读取填充，如果读取完成后，则将其置为空，下次触发读事件时则创建新的  NetworkReceive 对象。
4.  当读完后把这个 NetworkReceive 对象添加到已经接收完毕网络请求集合里。

接下来我们看看几个其他比较简单的方法。

## **02.9 completedSends()**

@Override

public List<Send> completedSends() {

return this.completedSends;

}

该方法主要用来**返回发送完成的Send集合数据**。

## **02.10 completedReceives()**

@Override

public Collection<NetworkReceive> completedReceives() {

return this.completedReceives.values();

}

该方法主要用来**返回已经接收完毕的请求集合数据**。

## **02.11 disconnected()**

@Override

public Map<String, ChannelState> disconnected() {

return this.disconnected;

}

该方法主要用来**返回断开连接的 broker 集合数据**。

## **02.12 connected()**

@Override

public List<String> connected() {

return this.connected;

}

该方法主要用来**返回连接成功的 broker 集合数据**。

## **02.13 isChannelReady()**

/\*\*

 \* check if channel is ready

 \*/

@Override

public boolean isChannelReady(String id) {

// 从 Channels 集合中获取该id对应的 channel 

KafkaChannel channel \= this.channels.get(id);

// 然后 channel 不为空 则判断是否准备好

return channel != null && channel.ready();

}

// KafkaChannel 类方法

public boolean ready() {

// 判断传输层是否准备好，默认是 PlaintextTransportLayer

return transportLayer.ready() && authenticator.complete();

}

该方法主要用来**判断对应的 Channel 是否准备好**，参数是 channel id。

## **02.14 addToCompletedReceives()**

/\*\*

 \* adds a receive to completed receives

 \*/

private void addToCompletedReceives(KafkaChannel channel, NetworkReceive networkReceive, long currentTimeMs) {

if (hasCompletedReceive(channel))

throw new IllegalStateException("Attempting to add second completed receive to channel " + channel.id());

// 将 channel id 添加到已经接收完毕的网络请求集合中

this.completedReceives.put(channel.id(), networkReceive);

    sensors.recordCompletedReceive(channel.id(), networkReceive.size(), currentTimeMs);

}

/\*\*

 \* Check if given channel has a completed receive

 \*/

private boolean hasCompletedReceive(KafkaChannel channel) {

// 判断已经接收完毕的网络集合中是否存在该 channel id

return completedReceives.containsKey(channel.id());

}

该方法主要用来**将某个 channel 添加到已经接收完毕的网络请求集合中**。

1.  先判断该 Channel 对应的 id 是否已经存在于已经接收完毕的网络请求集合中。
2.  如果不存在的话再将该 Channel id 添加到已经存在于已经接收完毕的网络请求集合中。
3.  记录 Metrics 信息。

## **03 空闲超时到期连接管理器**

为什么会有这个管理器，大家都知道对于 TCP 大量连接或者重连是会对 Kafka 造成性能影响的，而 Kafka 客户端又不能同时连接过多的节点。因此设计这样一个 LRU 算法，每隔9分钟就删除一个空闲过期的连接，以保证已有连接的有效。

private static class IdleExpiryManager {

// lru 连接集合

private final Map<String, Long> lruConnections;

// 连接最大的空闲时间 默认9分钟

private final long connectionsMaxIdleNanos;

private long nextIdleCloseCheckTime;

// 初始化管理器

public IdleExpiryManager(Time time, long connectionsMaxIdleMs) {

this.connectionsMaxIdleNanos = connectionsMaxIdleMs \* 1000 \* 1000;

// initial capacity and load factor are default, we set them explicitly because we want to set accessOrder = true

// 初始化lru连接集合，设置初始容量，扩容因子，是否排序

this.lruConnections = new LinkedHashMap<>(16, .75F, true);

this.nextIdleCloseCheckTime = time.nanoseconds() + this.connectionsMaxIdleNanos;

    }

// 更新活跃时间

public void update(String connectionId, long currentTimeNanos) {

        lruConnections.put(connectionId, currentTimeNanos);

    }

    ...

// 删除连接

public void remove(String connectionId) {

        lruConnections.remove(connectionId);

    }

}

该类通过「**LinkedHashMap 结构来实现一个 lru 连接集合**」，最核心的方法就是 update() 来更新链接的活跃时间，remove() 来删除连接。

主要用在以下3个地方：

1.  在将 channel 注册到 nioSelector 的时候，即调用 registerChannel() 会第一次设置连接的活跃时间。
2.  在调用 pollSelectionKeys() 检查到准备就绪的网络事件时，更新连接对应的活跃时间。
3.  在调用 close() 关闭连接的时候会从 lru 连接集合中删除该连接。

## **04 网络连接的全流程**

网络连接总共分为以下两个阶段：

> 连接的初始化。
> 
> 完成连接。

![](https://article-images.zsxq.com/Fv4CgzuBnta_gy2nI84nq6VM57e-)

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、带你先整体的梳理了 Kafka 对 Java NIO 三剑客中的 Selector 的功能介绍。

2、又带你剖析了 Selector 的重要方法和具体的操作过程。

3、介绍空闲超时到期连接管理器是什么，有什么作用？

4、最后带你梳理了网络连接的全流程。