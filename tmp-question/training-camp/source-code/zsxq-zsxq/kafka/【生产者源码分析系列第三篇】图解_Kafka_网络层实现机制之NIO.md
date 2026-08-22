大家好，我是 华仔, 又跟大家见面了。

在上一篇中，主要带大家深度剖析了「**生产者元数据**」的拉取、管理全流程，今天我们就来聊聊 **Kafka 是如何对 Java NIO 进行封装的**，本系列总共分为3篇，主要剖析以下几个问题：

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

本篇只讨论前3个问题，剩余的放到后2篇中。

![](https://article-images.zsxq.com/FvatpDXW4vTyi5cgQ5_IGyiEFeKr)

## **01 总体概述**

上篇剖析了「**生产者元数据的拉取和管理的全过程**」，此时发送消息的时候就有了元数据，但是还没有进行网络通信，而网络通信是一个相对复杂的过程，对于 Java 系统来说网络通信一般会采用 NIO 库来实现，所以 **Kafka 对 Java NIO 封装了统一的框架，来实现多路复用的网络 I/O 操作**。

为了方便大家理解，所有的源码只保留骨干。

## **02 Kafka 对 Java NIO 的封装**

如果大家对 Java NIO 不了解的话，可以看下这个文档，这里就不过多介绍了。

[https://pdai.tech/md/java/io/java-io-nio.html](https://pdai.tech/md/java/io/java-io-nio.html)

![](https://article-images.zsxq.com/Fh8r-xi99zeVPA0XKmgPIFvyLUbP)

我们来看看 **Kafka 对 Java NIO 组件做了哪些封装？** 这里先说下结果，后面会深度剖析。

> TransportLayer： 它是一个接口，封装了底层 NIO 的 SocketChannel。
> 
> NetworkReceive： 封装了 NIO 的 ByteBuffer 中的读 Buffer，对网络编程中的粘包、拆包经典实现。
> 
> NetworkSend： 封装了 NIO 的 ByteBuffer 中的写 Buffer。
> 
> KafkaChannel： 对 TransportLayer、NetworkReceive、NetworkSend 进一步封装，屏蔽了底层的实现细节，对上层更友好。
> 
> KafkaSelector： 封装了 NIO 的 Selector 多路复用器组件。﻿﻿

![](https://article-images.zsxq.com/Fh5p5bLZDMPgEQZDYy1iY5Esae-y)

接下来我们挨个对上面组件进行剖析。

## **02 TransportLayer 封装过程**

TransportLayer 接口是对 NIO 中 「**SocketChannel**」 的封装。它的实现类总共有 2 个：

> PlaintextTransportLayer： 明文网络传输实现。
> 
> SslTransportLayer： SSL 加密网络传输实现。

本篇只剖析 PlaintextTransportLayer 的实现。

![](https://article-images.zsxq.com/Ft-qrkfC6LyaBOrZvZ7G5ivx8nYY)

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/PlaintextTransportLayer.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/PlaintextTransportLayer.java)

public class PlaintextTransportLayer implements TransportLayer {

// java nio 中 SelectionKey 事件

private final SelectionKey key;

// java nio 中的SocketChannel

private final SocketChannel socketChannel;

// 安全相关

private final Principal principal \= KafkaPrincipal.ANONYMOUS;

// 初始化

public PlaintextTransportLayer(SelectionKey key) throws IOException {

// 对 NIO 中 SelectionKey 类的对象引用

this.key = key;

// 对 NIO 中 SocketChannel 类的对象引用

this.socketChannel = (SocketChannel) key.channel();

    }

}

从上面代码可以看出，该类就是**对底层 NIO 的 socketChannel 封装引用**。将构造函数的 SelectionKey 类对象赋值给 key，然后从 key 中取出对应的 SocketChannel 赋值给 socketChannel，这样就完成了初始化工作。

![](https://article-images.zsxq.com/Fn8_VirXaGTYk7GIbJos_CMrTYWB)

接下来，我们看看几个重要方法是如何使用这2个 NIO 组件的。

## **02.1 finishConnect()**

@Override

// 判断网络连接是否完成

public boolean finishConnect() throws IOException {

// 1. 调用socketChannel的finishConnect方法，返回该连接是否已经连接完成

boolean connected \= socketChannel.finishConnect();

// 2. 如果网络连接完成以后就删除对OP\_CONNECT事件的监听，同时添加对OP\_READ事件的监听

if (connected)

// 事件操作

        key.interestOps(key.interestOps() & ~SelectionKey.OP\_CONNECT | SelectionKey.OP\_READ);

// 3. 最后返回网络连接

return connected;

}

该方法主要用来**判断网络连接是否完成**，如果完成就关注 「**OP\_READ**」 事件，并取消 「**OP\_CONNECT**」 事件。

> 首先调用 socketChannel 通道的 finishConnect() 判断连接是否完成。
> 
> 如果网络连接完成以后就删除对 OP\_CONNECT 事件的监听，同时添加对 OP\_READ 事件的监听，因为连接完成后就可能接收数据了。
> 
> 最后返回网络连接 connected。

## **二进制位运算事件监听**

这里通过「**二进制位运算**」巧妙的解决了网络事件的监听操作，实现非常经典。

通过 socketChannel 在 Selector 多路复用器注册事件返回 SelectionKey ，SelectionKey 的类型包括：

> OP\_READ： 可读事件，值为：1<<0 == 1 == 00000001。
> 
> OP\_WRITE： 可写事件，值为：1<<2 == 4 == 00000100。
> 
> OP\_CONNECT： 客户端连接服务端的事件，一般为创建 SocketChannel 客户端 channel，值为：1<<3 == 8 ==00001000。
> 
> OP\_ACCEPT： 服务端接收客户端连接的事件，一般为创建 ServerSocketChannel 服务端 channel，值为：1<<4 == 16 == 00010000。

key.interestOps(key.interestOps() & ~SelectionKey.OP\_CONNECT | SelectionKey.OP\_READ);

**首先"～"符号代表按位取反，"&"代表按位取与**，通过 key.interestOps() 获取当前的事件，然后和 OP\_CONNECT事件取反「**11110111**」 后按位与操作。

所以，"& ～xx" 代表删除 xx 事件，**有就删除，没有就不变**；而 "| xx" 代表将 xx 事件添加进去。

## **02.2 read()**

@Override

public int read(ByteBuffer dst) throws IOException {

// 调用 NIO 的通道实现数据的读取 

return socketChannel.read(dst);

}

该方法主要用来**把 socketChannel 里面的数据读取缓冲区 ByteBuffer 里**，通过调用 [socketChannel.read](http://socketchannel.read/)() 实现。

## **02.3 write()**

@Override

public int write(ByteBuffer src) throws IOException {

return socketChannel.write(src);

}

该方法主要用来**把缓冲区 ByteBuffer 的数据写到 SocketChannel 里**，通过调用 [socketChannel.write](http://socketchannel.write/)() 实现。

大家都知道在网络编程中，一次读写操作并一定能把数据读写完，所以就需要判断是否读写完成，势必会涉及数据的「**拆包**」、「**粘包**」操作。 这些操作比较繁琐，因此 **Kafka 将 ByteBuffer 的读写操作进行重新封装，分别对应 NetworkReceive 读操作、NetworkSend 写操作，对于上层调用无需判断是否读写完成，更加友好**。

接下来我们就来分别剖析下这2个类的实现。

## **03 NetworkReceive 封装过程**

![](https://article-images.zsxq.com/Ft9tVGoLnyp4IZiPm2MivOuneh22)

public class NetworkReceive implements Receive {

  ....

// 空 ByteBuffer 

private static final ByteBuffer EMPTY\_BUFFER \= ByteBuffer.allocate(0);

private final String source;

// 存储响应消息数据长度

private final ByteBuffer size;

// 响应消息数据的最大长度

private final int maxSize;

// ByteBuffer 内存池

private final MemoryPool memoryPool;

// 已读取字节大小

private int requestedBufferSize \= -1;

// 存储响应消息数据体

private ByteBuffer buffer;

// 初始化构造函数

public NetworkReceive(int maxSize, String source, MemoryPool memoryPool) {

this.source = source;

// 分配4个字节大小的数据长度

this.size = ByteBuffer.allocate(4);

this.buffer = null;

// 能接收消息的最大长度

this.maxSize = maxSize;

this.memoryPool = memoryPool;

  }

}

> EMPTY\_BUFFER： 空 Buffer，值为 ByteBuffer.allocate(0)。
> 
> source： final类型，用来确定对应 channel id。
> 
> size： final类型，存储响应消息数据长度，大小为4字节。
> 
> maxSize： final类型，接收响应消息数据的最大长度。
> 
> memoryPool： final类型，ByteBuffer 内存池。
> 
> requestedBufferSize： 已读取字节大小。
> 
> buffer： 存储响应消息数据体。

从属性可以看出，包含2个 ByteBuffer，分别是 size 和 buffer。这里重点说下源码中的**size字段**的初始化。通过长度编码方式实现，上来就先分配了**4字节**大小的 ByteBuffer 来存储响应消息数据长度，即32位，与 Java int 占用相同的字节数，完全满足表示消息长度的值。

介绍完字段后，我们来深度剖析下该类的几个重要的方法。

## **03.1 readFrom()**

public long readFrom(ScatteringByteChannel channel) throws IOException {

// 读取数据总大小

int read \= 0;

// 1.判断响应消息数据长度的 ByteBuffer 是否读完

if (size.hasRemaining()) {

// 2.还有剩余，直接读取消息数据的长度

int bytesRead \= channel.read(size);

if (bytesRead < 0)

throw new EOFException();

// 3.每次读取后，累加到总读取数据大小里

      read += bytesRead;

// 4.判断响应消息数据长度的缓存是否读完了

if (!size.hasRemaining()) {

// 5.重置position

          size.rewind();

// 6.读取响应消息数据长度

int receiveSize \= size.getInt();

// 7.如果有异常就抛出

if (receiveSize < 0)

throw new InvalidReceiveException("Invalid receive (size = " + receiveSize + ")");

if (maxSize != UNLIMITED && receiveSize > maxSize)

throw new InvalidReceiveException("Invalid receive (size = " + receiveSize + " larger than " + maxSize + ")");

// 8.将读到数据长度赋值已读取字节大小，即数据体的大小

          requestedBufferSize = receiveSize; 

if (receiveSize == 0) {

              buffer = EMPTY\_BUFFER;

          }

      }

  }

// 9.如果数据体buffer还没有分配，且响应消息数据头已读完

if (buffer == null && requestedBufferSize != -1) {

// 10.分配requestedBufferSize字节大小的内存空间给数据体buffer

      buffer = memoryPool.tryAllocate(requestedBufferSize);

if (buffer == null)

          log.trace("Broker low on memory - could not allocate buffer of size {} for source {}", requestedBufferSize, source);

  }

// 11.判断buffer是否分配成功

if (buffer != null) {

// 12.把channel里的数据读到buffer中

int bytesRead \= channel.read(buffer);

if (bytesRead < 0)

throw new EOFException();

// 13.累计读取数据总大小

      read += bytesRead;

  }

// 14. 返回总大小

return read;

}

该方法主要用来**把对应 channel 中的数据读到 ByteBuffer 中**，包括响应消息数据长度的 size 和响应消息数据体长度的 buffer，可能会被多次调用，每次都需要判断 size 和 buffer 的状态并读取。

在读取时，先读取4字节到 size 中，再根据 size 的大小为 buffer 分配内存，然后读满整个 buffer 时就表示读取完成了。

**通过短短的30行左右代码就解决了工业级**「**拆包**」 、「**粘包**」**问题，相当的经典**。

如果要解决「**粘包**」问题，就是在每个响应数据中间插入一个特殊的字节大小的「**分隔符**」，这里就在响应消息体前面插入4个字节，代表响应消息自己本身的数据大小，如下图所示：

![](https://article-images.zsxq.com/FmgU7sUpudqJRRwrOuJJBGFJaDdd)

![](https://article-images.zsxq.com/FrE-bLLlTgFaqLFkguo-iYZJrhWn)

具体「**拆包**」的操作步骤如下：

![](https://article-images.zsxq.com/Fld_aS0-vX18HuJmvGWlcG4RJP6p)

> 调用 size.hasRemaining() 返回 position 至 limit 之间的字节大小来判断响应消息数据长度的 ByteBuffer 是否读完。
> 
> 当未读完则通过调用 NIO 的方法 channel.read(size)，直接把读取4字节的响应消息数据的长度写入到 ByteBuffer size 中，如果已经读取到了4字节，此时 position=4，与  limit  相同，表示 ByteBuffer size 已经读满了。
> 
> 每次读取后，累加到总读取数据大小里再次判断响应消息数据长度的缓存是否读完了。
> 
> 如果读完了，先重置 position 位置为0，此时就可以从 ByteBuffer 中读取数据了，然后调用 size.getInt() 从 ByteBuffer 当前 position 位置读取4个字节，并转化成int 类型数值赋给 receiveSize，即响应体的长度。
> 
> 如果有异常就抛出，包括响应数据体的长度无效或者大于最大长度等。
> 
> 将读到响应数据长度赋值 requestedBufferSize，即数据体的大小。
> 
> 如果响应数据体 buffer 还没有分配，且响应数据头已读完，分配 requestedBufferSize 字节大小的内存空间给数据体 buffer。
> 
> 如果 buffer 分配成功，表示 size 已读完，此时直接把 channel 里的响应数据读到跟它大小一致的 ByteBuffer 中，再次累计读取数据总大小。
> 
> 最后返回数据总大小。

## **03.2 complete()**

@Override

public boolean complete() {

// 响应消息头已读完 && 响应消息体已读完

return !size.hasRemaining() && buffer != null && !buffer.hasRemaining();

}

该方法主要用来判断是否都读取完成，**即响应头大小和响应体大小都读取完**。

## **03.3 size()**

// 返回大小

public int size() {

return payload().limit() + size.limit();

}

public ByteBuffer payload() {

return this.buffer;

}

该方法主要用来返回**响应头和响应体还有多少数据需要读出**。此时已经剖析完读 Buffer 的封装，接下来我们看看写 Buffer。

## **04 NetworkSend 封装过程**

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/NetworkSend.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/NetworkSend.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/ByteBufferSend.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/ByteBufferSend.java)

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/Send.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/Send.java)

调用关系图如下：

![](https://article-images.zsxq.com/Fs8veTNJKI_Nz-igAmxC6kqbCK90)

## **04.1 Send 接口**

我们先看一下接口 Send 都定义了哪些方法。

public interface Send {

// 要把数据写入目标的 channel id

    String destination();

// 要发送的数据是否发送完了

boolean completed();

// 把数据写到对应 channel 中

long writeTo(GatheringByteChannel channel) throws IOException;

// 发送数据的大小

long size();

}

Send 作为要发送数据的接口, 子类 ByteBufferSend 实现 complete() 方法用于判断是否已经发送完成，实现 writeTo() 方法来实现写入数据到Channel中。

## **04.2 ByteBufferSend 类**

ByteBufferSend 类实现了 Send 接口，**即实现了数据从 ByteBuffer 数组发送到 channel**：

public class ByteBufferSend implements Send {

private final String destination;

// 总共要写多少字节数据

private final int size;

// 用于写入channel里的ByteBuffer数组，说明kafka一次最大传输字节是有限定的

protected final ByteBuffer\[\] buffers;

// 总共还剩多少字节没有写完

private int remaining;

private boolean pending \= false;

public ByteBufferSend(String destination, ByteBuffer... buffers) {

this.destination = destination;

this.buffers = buffers;

for (ByteBuffer buffer : buffers)

            remaining += buffer.remaining();

// 计算需要写入字节的总和

this.size = remaining;

    }

}

我们来看下这个类中的几个重要字段：

> destination： 数据写入的目标 channel id。
> 
> size： 总共需要往 channel 里写多少字节数据。
> 
> buffers： ByteBuffer数组类型，用来存储要写入 channel 里的数据。
> 
> remaining： ByteBuffer数组所有的ByteBuffer 还剩多少字节没有写完。

介绍完字段后，我们来深度剖析下该类的几个重要的方法。

### **04.2.1 writeTo()**

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

该方法主要用来**把 buffers 数组写入到 SocketChannel里**，因为在网络编程中，写一次不一定可以完全把数据都写成功，所以调用底层 [channel.write](http://channel.write/)(buffers) 方法会返回「**已经写入成功多少字节**」的返回值，这样调用一次后就知道已经写入多少字节了。

### **04.2.2 some other**

@Override

public String destination() {

// 返回对应的channel id

return destination;

}

@Override

public boolean completed() {

// 判断是否完成 即没有剩余&pending=false

return remaining <= 0 && !pending;

}

/\*\*

 \* always returns false as there will be not be any

 \* pending writes since we directly write to socketChannel.

 \*/

@Override

public boolean hasPendingWrites() {

// 在PLAINTEXT下 pending 始终为 false

return false;

}

@Override

public long size() {

// 返回写入字节的总和

return this.size;

}

## **04.3 NetworkSend 类**

NetworkSend 类继承了 ByteBufferSend 类，真正用来写 Buffer。

public class NetworkSend extends ByteBufferSend {

// 实例化

public NetworkSend(String destination, ByteBuffer buffer) {

// 调用父类的方法初始化

super(destination, sizeBuffer(buffer.remaining()), buffer);

    }

// 用来构造4个字节的 sizeBuffer

private static ByteBuffer sizeBuffer(int size) {

// 先分配一个4个字节的ByteBuffer

ByteBuffer sizeBuffer \= ByteBuffer.allocate(4);

// 写入size长度值

        sizeBuffer.putInt(size);

// 重置 position

        sizeBuffer.rewind();

// 返回 sizeBuffer

return sizeBuffer;

    }

}

该类相对简单些，就是构建一个发往 channel 对应的节点 id 的消息数据，它的实例化过程如下：

> 先分配一个4个字节的 ByteBuffer 的变量 sizeBuffer，再把要发送的数据长度赋值给 sizeBuffer。
> 
> 此时 sizeBuffer 的响应头字节数和 sizeBuffer 的响应数据就都有了。
> 
> 然后调用父类 ByteBufferSend 的方法进行初始化。

另外 ByteBuffer\[\] 为两个 buffer，可以理解为一个消息头 buffer 即 size，一个消息体 buffer。消息头 buffer 的长度为4byte,存放的是消息体 buffer 的长度。而消息体 buffer 是上层传入的业务数据，所以 **send 就是持有一个待发送的 ByteBuffer**。

接下来我们来看看 KafkaChannel 是如何对上面几个类进行封装的。

## **05 KafkaChannel 封装过程**

github 源码地址如下：

[https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/KafkaChannel.java](https://github.com/apache/kafka/blob/2.7/clients/src/main/java/org/apache/kafka/common/network/KafkaChannel.java)

public class KafkaChannel implements AutoCloseable {

  ....

// 节点 id

private final String id;

// 传输层对象

private final TransportLayer transportLayer;

  ....

// 最大能接收请求的字节数

private final int maxReceiveSize;

// 内存池，用来分配指定大小的 ByteBuffer 

private final MemoryPool memoryPool;

// NetworkReceive 类的实例

private NetworkReceive receive;

// NetworkSend 类的实例

private Send send;

// 是否关闭连接

private boolean disconnected;

  ....

// 连接状态

private ChannelState state;

// 需要连接的远端地址

private SocketAddress remoteAddress;

// 初始化

public KafkaChannel(String id, TransportLayer transportLayer, Supplier<Authenticator> authenticatorCreator,int maxReceiveSize, MemoryPool memoryPool, ChannelMetadataRegistry metadataRegistry) {

this.id = id;

this.transportLayer = transportLayer;

this.authenticatorCreator = authenticatorCreator;

this.authenticator = authenticatorCreator.get();

this.networkThreadTimeNanos = 0L;

this.maxReceiveSize = maxReceiveSize;

this.memoryPool = memoryPool;

this.metadataRegistry = metadataRegistry;

this.disconnected = false;

this.muteState = ChannelMuteState.NOT\_MUTED;

this.state = ChannelState.NOT\_CONNECTED;

  }

}

我们来看下这个类中的几个重要字段：

> id： channel 对应的节点 id。
> 
> transportLayer： 传输层对象。
> 
> maxReceiveSize： 最大能接收请求的字节数。
> 
> memoryPool： 内存池，用来分配指定大小的 ByteBuffer。
> 
> receive： NetworkReceive 类的实例。
> 
> send： NetworkSend 类的实例。
> 
> disconnected： 是否关闭连接。
> 
> state： KafkaChannel 的状态。
> 
> remoteAddress： 需要连接的远端地址。

从属性可以看出，**有3个最重要的成员变量：TransportLayer、NetworkReceive、Send**。KafkaChannel 通过 TransportLayer 进行读写操作，NetworkReceive 用来读取，Send 用来写出。

为了封装普通和加密的Channel「**TransportLayer根据网络协议的不同，提供不同的子类**」而对于 KafkaChannel 提供统一的接口，「**这是策略模式很好的应用**」。

> 每个 NetworkReceive 代表一个单独的响应，KafkaChannel 读取的数据会存储到 NetworkReceive 中，当 NetworkReceive 读满，一个请求就完整读取了。
> 
> 每个 Send 代表一个单独的请求，需要写出时只需赋值此变量，之后调用 write() 方法将其中的数据写出。
> 
> 介绍完字段后，我们来深度剖析下其网络读写操作是如何实现的？

## **05.1 setSend()**

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

该方法主要用来**预发送，即在发送网络请求前，将需要发送的ByteBuffer 数据保存到 KafkaChannel 的 send 中**，然后调用传输层方法增加对这个 channel 上「**OP\_WRITE**」事件的关注。当真正执行发送的时候，会从 send 中读取数据。

## **05.2 write()**

public long write() throws IOException {

// 判断 send 是否为空，如果为空表示已经发送完毕了

if (send == null)

return 0;

    midWrite = true;

// 调用ByteBufferSend.writeTo把数据真正发送出去

return send.writeTo(transportLayer);

}

该方法主要用来**把保存在 send 上的数据真正发送出去**。

> 首先判断要发送的 send 是否为空，如果为空则表示在 KafkaChannel 的 Buffer 的数据都发送完毕了。
> 
> 如果不为空就调用ByteBufferSend.writeTo() 方法通过网络 I/O 操作将数据发送出去。

## **05.3 read()**

public long read() throws IOException {

// 如果receive为空表示数据已经读完，需要重新实例化对象

if (receive == null) {

// 确保分配了 NetworkReceive 

        receive = new NetworkReceive(maxReceiveSize, id, memoryPool);

    }

//如果未读完，尝试读取该对象

long bytesReceived \= receive(this.receive);

if (this.receive.requiredMemoryAmountKnown() && !this.receive.memoryAllocated() && isInMutableState()) {

//pool must be out of memory, mute ourselves.

        mute();

    }

return bytesReceived;

}

该方法主要用来**把从网络I/O操作中读出的数据保存到 NetworkReceive 中**。

> 判断 receive 是否为空，如果为空表示上次已读完，需要重新实例化 NetworkReceive 对象。
> 
> 如果 receive 不为空，表示未读完，此时读取的还是原先的 NetworkReceive 对象，然后再调用 receive() 方法尝试把 channel 的数据读到 NetworkReceive 对象中。
> 
> 最后返回读到的字节数。

## **05.4 maybeCompleteReceive()**

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

该方法主要用来**判断数据已经读取完毕了**，而判断是否读完的条件是 **NetworkReceive 里的 buffer 是否用完**，包括上面说过的表示响应消息头 size ByteBuffer 和响应消息体本身的 buffer ByteBuffer。这两个都读完才算真正读完了。

> 当 buffer 读完后调用 rewind 重置 position位置。
> 
> 将 receive 赋值给结果集 result。
> 
> 此时读完后将 receive 清空，以便下次读。
> 
> 最后返回结果集 result，完成一次读操作。

## **05.5 maybeCompleteSend()**

// 可能完成发送

public Send maybeCompleteSend() {

if (send != null && send.completed()) {

        midWrite = false;

        transportLayer.removeInterestOps(SelectionKey.OP\_WRITE);

Send result \= send;

        send = null;

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

该方法主要用来**是否写数据完毕了**，而判断的写数据完毕的条件是**buffer 中没有剩余且pending为false**。

> 当写数据完毕后，取消传输层对 OP\_WRITE 事件的监听，完成一次写操作。
> 
> 将 send 赋值给结果集 result。
> 
> 此时读完后将 send 清空，以便下次写。
> 
> 最后返回结果集 result，完成一次写操作。

最后我们来聊聊事件注册和取消的具体时机，以便更好的理解网络 I/O 操作。

## **06 事件注册与取消时机**

我们知道 Java NIO 是基于 epoll 模型来实现的。所有基于 epoll 的框架，都有3个阶段：

> 注册事件(OP\_CONNECT, OP\_ACCEPT, OP\_READ, OP\_WRITE)。
> 
> 轮询网络I/O是否就绪。
> 
> 执行实际网络I/O操作。

这里我们来看下相关事件是何时被注册和取消的。

## **06.1 OP\_CONNECT 事件**

### **06.1.1 OP\_CONNECT 事件注册时机**

在 Selector 发起网络连接的时候进行「**OP\_CONNECT**」事件注册。

public void connect(String id, InetSocketAddress address, int sendBufferSize, int receiveBufferSize) throws IOException {

SocketChannel socketChannel \= SocketChannel.open();

SelectionKey key \= null;

try {

// 注册 OP\_CONNECT 到 selector 上

      key = registerChannel(id, socketChannel, SelectionKey.OP\_CONNECT);

    } catch (IOException | RuntimeException e){}

}

### **06.1.2 OP\_CONNECT 事件取消时机**

在 PlainTransportLayer 明文传输层完成连接的时候取消 「**OP\_CONNECT**」事件。

public boolean finishConnect() throws IOException {

// 删除连接事件，添加读事件

  key.interestOps(key.interestOps() & ~SelectionKey.OP\_CONNECT | SelectionKey.OP\_READ);

}

## **06.2 OP\_READ 事件**

### **06.2.1 OP\_READ 事件注册时机**

从上面也可以看出，「**OP\_READ**」事件的注册和「**OP\_CONNECT**」事件的取消是同时进行的。

### **06.2.2 OP\_READ 事件取消时机**

由于 「**OP\_READ**」事件是要一直监听是否有新数据到来，所以不会取消。并且因为是 Java NIO 使用的 「**epoll 的 LT 模式**」，只要「**读缓冲区**」有数据，就会一直触发。

## **06.3 OP\_WRITE 事件**

### **06.3.1 OP\_WRITE 事件注册时机**

在 KafkaChannel 真正发送网络请求之前注册「**OP\_WRITE**」事件。

public void setSend(Send send) {

// 调用传输层增加写事件

this.transportLayer.addInterestOps(SelectionKey.OP\_WRITE);

}

### **06.3.2 OP\_WRITE 事件取消时机**

public Send maybeCompleteSend() {

if (send != null && send.completed()) {

//完成一次发送后取消 OP\_WRITE 事件

        transportLayer.removeInterestOps(SelectionKey.OP\_WRITE);

    }

}

## **06.4 事件总结**

> 对于不同事件类型的「事件就绪」：
> 
> OP\_READ事件就绪：即当有新数据到来，需要去读取。由于是基于 LT 模式，只要读缓冲区有数据，会一直触发。
> 
> OP\_WRITE事件就绪：即本地 socketchannel 缓冲区有没有写满。如果没有写满的话，就会一直触发写事件。所以要避免「写的死循环」问题，写完就要取消写事件。
> 
> OP\_CONNECT事件就绪： 即 connect 连接完成。
> 
> OP\_ACCEPT事件就绪：即有新的连接进来，调用 accept处理。
> 
> 不同类型事件处理方式是不一样的：
> 
> OP\_CONNECT事件：注册1次，连接成功之后，就取消了。有且仅有1次。
> 
> OP\_READ事件：注册之后不取消，一直监听。
> 
> OP\_WRITE事件：每调用一次send，注册1次。send成功，取消注册。

## **07 总结**

这里，我们一起来总结一下这篇文章的重点。

1、带你先整体的梳理了 Kafka 对 Java NIO 封装的组件以及调用关系图。

2、分别带你梳理了传输层 TransportLayer 的明文网络传输层的实现、网络读操作 NetworkReceive、网络写操作 NetworkSend 的实现、以及 KafkaChannel 是如何进一步对上面组件进行封装提供更加友好的网络连接、读写操作的。

3、最后剖析了网络 I/O 操作过程中的事件注册和取消时机。