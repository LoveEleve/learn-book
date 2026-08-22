大家好，我是 **华仔**, 又跟大家见面了。

通过前面文章的学习，我们都知道在 Kafka 运行过程中整个网络层是基于「**Java NIO 多路复用**」 进行封装的，在开启服务端 Broker Reactor 网络模型架构设计之前，我们先来重温下 Java NIO 多路复用的技术细节，这样对后续学习会更有帮助。

![](https://article-images.zsxq.com/Fqxqf4ZN1MtUj8pPqKJQ1NLTy88l)

## **01 NIO 代码实现**

Java NIO 是 Java 1.4 版本引入的一种新的 I/O API，它提供了与传统 I/O API 不同的 I/O 处理方式，可以更高效地处理 I/O 操作。

先来看下 NIO Server 端代码实现。

##   
**1.1 NIO Server 端**

import java.io.IOException;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;

import java.nio.channels.SelectionKey;

import java.nio.channels.Selector;

import java.nio.channels.ServerSocketChannel;

import java.nio.channels.SocketChannel;

import java.util.Iterator;

import java.util.Set;

public class NIOServer {

private Selector selector;

public void initServer(int port) throws IOException {

try{

// 1、创建ServerSocketChannel，打开服务端

ServerSocketChannel serverSocketChannel \= ServerSocketChannel.open();

// 设置为非阻塞模式

serverSocketChannel.configureBlocking(false);

// 绑定端口

serverSocketChannel.socket().bind(new InetSocketAddress(port),100);

// 创建 Selector 多路复用器

selector = Selector.open();

// 2、将ServerSocketChannel注册到Selector上，并监听ACCEPT事件

serverSocketChannel.register(selector, SelectionKey.OP\_ACCEPT);

System.out.println("Server started on port " + port);

}catch (IOException e){

e.printStackTrace();

}

// 3、循环处理事件

while (true) {

try{

// 阻塞等待事件

selector.select();

// 获取事件集合

Set<SelectionKey> selectionKeys = selector.selectedKeys();

Iterator<SelectionKey> iterator = selectionKeys.iterator();

// 4、轮询 SelectionKey 集合里的 SelectionKey，一个 SelectionKey 代表一个网络事件。

while (iterator.hasNext()) {

SelectionKey selectionKey \= iterator.next();

// 处理事件

handleEvent(selectionKey);

// 移除事件

iterator.remove();

}

}catch (Exception e){

e.printStackTrace();

}

}

}

private void handleEvent(SelectionKey selectionKey) throws IOException {

try {

// 5、判断网络事件的类型并做出对应的处理

if (selectionKey.isAcceptable()) {

// 处理客户端 ACCEPT 事件

ServerSocketChannel serverSocketChannel \= (ServerSocketChannel) selectionKey.channel();

// 通过 TCP 三次握手，建立和获取获取客户端和服务器的连接SocketChannel

SocketChannel socketChannel \= serverSocketChannel.accept();

socketChannel.configureBlocking(false);

// 注册网络读事件

socketChannel.register(selector, SelectionKey.OP\_READ);

System.out.println("Client connected: " + socketChannel.getRemoteAddress());

} else if (selectionKey.isReadable()) {

// 处理网络 READ 事件

SocketChannel socketChannel \= (SocketChannel) selectionKey.channel();

ByteBuffer buffer \= ByteBuffer.allocate(1024);

int bytesRead \= socketChannel.read(buffer);

if (bytesRead > 0) {

// 开始从 Buffer 读数据。

buffer.flip();

byte\[\] bytes = new byte\[buffer.remaining()\];

buffer.get(bytes);

String message \= new String(bytes);

System.out.println("Received message from " + socketChannel.getRemoteAddress() + ": " + message);

// 回复客户端

ByteBuffer responseBuffer \= ByteBuffer.wrap(("Server received message: " + message).getBytes());

socketChannel.write(responseBuffer);

} else {

// 客户端关闭连接

System.out.println("Client disconnected: " + socketChannel.getRemoteAddress());

socketChannel.close();

}

}

}catch (Exception e){

e.printStackTrace();

}

}

public static void main(String\[\] args) throws IOException {

new NIOServer().initServer(8888);

}

}

这里给大家简单的讲解下 Server 端代码步骤。

1.  **服务端初始化：**首先初始化一个 ServerSocketChannel 对象，然后调用它的 open() 方法，设置服务端服务的 TCP 端口号为 8888，看下面 main 函数传值，表示服务端可以接收外部客户端的请求了。
2.  **创建 Selector：**把 serverSocketChannel 注册在 selector 内，并让 selector 监听 serverSocketChannel 的网络连接事件「**OP\_ACCEPT 事件**」。
3.  这里 OP\_ACCEPT 就是外部客户的要连接到服务端请求连接的事件，当客户端发起请求后，Selector 会监到 「**OP\_ACCEPT 事件**」，然后通过 「**TCP 三次握手**」建立连接，这样客户端和服务端之间的连接就建立好了。
4.  所以 ServerSocketChannel 只有服务端会使用，主要是为建立连接而服务的，客户端想要和服务端建立连接都要通过 ServerSocketChannel 来建立。
5.  **轮询 Selector 上注册的事件：**整个代码在 while(true) 循环中，会不断地轮询执行 [selector.select](http://selector.select/)() 方法来查看是否有网络事件，所以只要有网络事件，早晚都会发现网络事件。该方法是非阻塞方法，没有网络事件时会返回 Null。
6.  **无限轮询 SelectionKey 集合里的 SelectionKey：**一个 SelectionKey 代表一个网络事件。
7.  这里的网络事件主要有两种事件，「**可以连接的事件**」、「**可以读取的事件**」。
8.  如果是 selectionkey.isAcceptable() == true，表示可以连接了，然后调用 [serverSocketChannel.accept](http://serversocketchannel.accept/)() 通过三次握手来实现 TCP 连接。
9.  如果是 key.isReadable() == true，表示可以读取了，然后创建一个 ByteBuffer 类的对象把数据写到 readBuffer，然后再从 readBuffer 来读取数据。读取客户端数据结束后，服务端会给客户端发送响应，最后再调用 [channel.write](http://channel.write/)(）来实现，当然写数据也是通过与 ByteBuffer 配合来实现的。

我们通过一张图来展示服务端代码实现流程，如下图：

![](https://article-images.zsxq.com/FiFLO648V83AD526_YlDkpzqXoXv)

## **1.2 NIO Client 端**

讲完 Server 端代码，我们来看看 Client 端代码：

import java.io.IOException;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;

import java.nio.channels.SocketChannel;

import java.util.Scanner;

public class NIOClient {

public void startClient(String host, int port) throws IOException {

try{ // 1、创建SocketChannel，用来与服务端连接，并实现网络读写操作。

SocketChannel socketChannel \= SocketChannel.open();

// 设置为非阻塞模式

socketChannel.configureBlocking(false);

// 2、连接服务器，通过三次握手实现 TCP 连接

socketChannel.connect(new InetSocketAddress(host, port));

// 创建一个 selector 对象

selector = Selector.open();

// 把 SocketChannel 注册到 selector 上，并监听请求连接事件 OP\_CONNECT

channel.register(selector, SelectionKey.OP\_CONNECT);

// 3、 遍历网络事件

while (true){

// 阻塞等待事件

selector.select();

Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

// 迭代事件集合

while (keyIterator.hasNext()){

SelectionKey key \= (SelectionKey) keyIterator.next();

keyIterator.remove();

// 4、 当是否可以连接时

if(key.isConnectable()){

// 如果连接成功了

if(channel.finishConnect()){

// 监听网络读事件

key.interestOps(SelectionKey.OP\_READ);

// 向服务端发送数据

channel.write(ByteBuffer.wrap("hello".getBytes()));

}else {

// 连接失败取消

key.cancel();

}

// 5、 当有数据要读取时

}else if(key.isReadable()){

SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

ByteBuffer buffer \= ByteBuffer.allocate(1024);

int bytesRead \= socketChannel.read(buffer);

if (bytesRead > 0) {

// 开始从 Buffer 读数据。

buffer.flip();

byte\[\] bytes = new byte\[buffer.remaining()\];

buffer.get(bytes);

String message \= new String(bytes);

System.out.println("Received message from " + socketChannel.getRemoteAddress() + ": " + message);

// 回复服务端

ByteBuffer responseBuffer \= ByteBuffer.wrap(("Client received message: " + message).getBytes());

socketChannel.write(responseBuffer);

} else {

// 客户端关闭连接

System.out.println("Client disconnected: " + socketChannel.getRemoteAddress());

socketChannel.close();

}

}

}

} catch (Exception e){

e.printStackTrace();

}

}

public static void main(String\[\] args) throws IOException {

// 这里启动十个线程，来模拟十个客户端。

for(int i \= 0;i < 100;i++){

new NIOClient().startClient("localhost", 8888);

}

}

}

客户端代码通过多线程模拟了一百个客户端同时向服务端发送请求，这里也简单的讲解下代码步骤：

1.  **首先创建一个 SocketChannel 用来与服务端进行连接：**这个 SocketChannel 是用来保持与客户端连接的 Channel，用来实现网络读写操作。其实，本质上还是通过三次握手来实现 TCP 连接。然后把 OP\_CONNECT 事件注册在新创建的 selector 上。
2.  **无限循环轮询 SelectionKey 集合里的 SelectionKey：**这里的网络事件主要也有两种事件，「**可以连接的事件**」、「**可以读取的事件**」。
3.  如果 key.isConnectable() == true，表示可以连接了，但是可以连接并不意味着已经连接上了。如果 channel.finishConnect()==true，此时才认为连接成功建立了，把 OP\_READ 事件注册到 selector 上，这样我们就可以接收到服务端的数据了。再通过和 ByteBuffer 配合向服务端发送数据。
4.  如果 key.isReadable() == true，表示网络读事件来了，然后读取服务端给我们发送的数据，读取后再次向服务端发送数据。

上面就是简单的 Java NIO 实现，大家可以自行运行下，这样可以帮助更好的理解其实现机制。

## **02 NIO 核心原理实现**

上面小节面给大家讲解了一个 Java NIO 的例子以及实现步骤，接下来我们来讲述下 NIO 核心原理实现。

其实 Java NIO 的多路复用机制并不是 Java NIO 本身来实现的，**它是通过调用操作系统的 API 来实现多路复用的**。

## **2.1 NIO 服务端初始化过程**

先来看看服务端的初始化过程。

// 1、创建ServerSocketChannel，打开服务端

ServerSocketChannel serverSocketChannel \= ServerSocketChannel.open();

// 设置为非阻塞模式

serverSocketChannel.configureBlocking(false);

// 绑定端口

serverSocketChannel.socket().bind(new InetSocketAddress(port),100);

// 创建 Selector 多路复用器

selector = Selector.open();

// 2、将ServerSocketChannel注册到Selector上，并监听ACCEPT事件

serverSocketChannel.register(selector, SelectionKey.OP\_ACCEPT);

1.  首先，第一步会创建一个 ServerSocketChannel 的实例，然后配置为非阻塞。
2.  调用 [serverSocketChannel.socket](http://serversocketchannel.socket/)() 返回 ServerSocketChannel 相关的 TCP Socket，然后调用 bind() 方法，设置 Socket 端口。因此 ServerSocketChannel  的作用就是 TCP 协议下用来监听对 TCP 某个端口的连接请求。
3.  接下来我们来看一个非常**重要的参数 BackLog**，这是 bind() 方法的第二个参数，上面例子是100，该值作用是：**同时要建立的连接数量，如果要大于这个数，就直接拒绝连接，是操作系统设置的连接请求的等待队列**。如果瞬时涌入1万个连接，但是建立连接需要 TCP 三次握手比较耗时，会在短时间内搞挂服务端，从而无法正常使用。

我们再来看看 Selector 多路复用器是如何注册 Channel 的？

## **2.2 Selector 的工作原理**

同时 Selector 也是基于底层操作系统来实现的。

public static Selector open() throws IOException {

return SelectorProvider.provider().openSelector();

}

从代码可以看出打开了一个 Selector，**因为操作系统底层实现了 Select 机制，用 Select 机制来监听注册到自己上面的 Channel 有没有网络事件。当每次注册某个 Channel 的网络事件到 Selector 上时都会位网络事件分配一个 SelectorKey**。

这里的 SelectionKey 是一个枚举类型，主要有下面几种类型：

1.  **OP\_CONNECT**：代表客户端的请求连接的事件。客户端的 SocketChannel 会向 Selector 注册这个事件， Selector 会监听服务端是否连接准备好了。
2.  **OP\_ACCEPT**：代表服务端的接收连接的事件。服务端的 ServerSocketChannel 会向 Selector 注册这个事件，Selector 会监听接收到客户端的请求连接。
3.  **OP\_READ**：代表服务端、客户端的网络读事件。服务端和客户端的 SocketChannel 都会向 Selector 注册这个事件。Selector 会监听是否接收到对方发送的数据了。
4.  **OP\_WRITE**：代表服务端、客户端的网络写事件。服务端和客户端的 SocketChannel 都会向 Selector 注册这个事件。Selector 会监听是否可以向对方发送数据了。

当注册在 Selector 上的 Channel 越来越多，也就是说注册在 Selector 上的网络事件越来越多，此时 Selector 就可以监听很多「**请求连接事件**」、「**读写事件**」，这样我们就通过 NIO 多路复用技术来响应海量客户端流量，从而实现了高性能网络服务端的目的。

此外，当对某个网络事件不感兴趣也可以取消对网络事件的监听，比如当客户端已经连接到了服务端，就可以取消对 OP\_CONNECT 事件的关注，其**好处是能够通过减少关注的事件数量从而减少 Selector 的负载，提升多路复用的效率**。同时，一个 SocketChannel 配置的时候建议采用「**非阻塞模式**」，这样程序如果没有网络事件也可以继续往下执行，而不会被阻塞。

最后通过一张图来说明下多路复用建立连接的过程。

![](https://article-images.zsxq.com/FlgC7jsxJOMET67HKK_eJ5NdUpqg)

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、第一部分通过一个 Server-Client 的例子给大家展示了如何使用 Java NIO 来实现高效通信。

2、第二部分给大家讲解了 Java NIO 的核心实现原理，包括「**连接时如何被建立的**」、「**Selector 的工作原理**」。

3、下一节，我们就来正式开启服务端 Broker 源码之 Reactor 网络模型架构设计。