大家好，我是 **华仔**, 又跟大家见面了。

从今天开始，我们开始对 RocketMQ 进行相关实现原理进行剖析，今天是第二篇，我们来聊聊 RocketMQ Producer 的架构设计，深度剖析下其内部底层原理设计思想，**下面进入正题**。

![](https://article-images.zsxq.com/FtipddxFO0c_qF9PUFLcJOae07xN)

## **01 RocketMQ Producer 总体概述**

**在 RocketMQ 中，我们通常把生产消息的一方称为 Producer 即生产者，它是消息的来源所在，它的主要功能就是将客户端的请求发送到 RocketMQ 服务端上。**

本文从原理实现上来讨论以下 2 个问题：

1.  RocketMQ Producer 的启动流程是怎样的？
2.  RocketMQ Producer 是如何把消息发送到服务端 Broker 上的？

## **02 RocketMQ Producer 之启动流程**

## **2.1 生产者示例**

我们都知道，在 RocketMQ 中，消息发送分为「**同步消息**」、「**异步消息**」、「**单向消息**」。

1.  **同步消息**：即 Producer 将消息发送之后会同步等待 Broker 的响应，并把响应结果传递给业务线程，整个过程中业务线程都处于在等待过程。
2.  **异步消息**：即 Producer 将消息发送请求放进线程池就返回了无需等待 Broker 的响应。后续逻辑处理，网络请求都在线程池中进行，等结果处理完之后回调业务定义好的回调函数。
3.  **单向消息**：只负责发送消息，不管发送结果。

下面给出这三种发送消息的示例：

### **2.1.1 同步发送**

// 这种可靠性同步地发送方式使用的比较广泛，比如：重要的消息通知，短信通知。

public class SyncProducer {

public static void main(String\[\] args) throws Exception {

// 1、实例化消息生产者 Producer

DefaultMQProducer producer \= new DefaultMQProducer("please\_rename\_unique\_group\_name");

// 2、设置 NameServer 的地址

producer.setNamesrvAddr("localhost:9876");

// 3、启动 Producer 实例

producer.start();

for (int i \= 0; i < 100; i++) {

// 创建消息，并指定Topic，Tag和消息体

Message msg \= new Message("TopicTest" /\* Topic \*/,

"TagA" /\* Tag \*/,

("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT\_CHARSET) /\* Message body \*/

);

// 5、发送消息到 Broker

SendResult sendResult \= producer.send(msg);

// 6、通过 sendResult 返回消息是否成功送达

System.out.printf("%s%n", sendResult);

}

// 7、如果不再发送消息，关闭 Producer 实例。

producer.shutdown();

}

}

### **2.1.2 异步发送**

// 异步消息通常用在对响应时间敏感的业务场景，即发送端不能容忍长时间地等待 Broker 的响应。

public class AsyncProducer {

public static void main(String\[\] args) throws Exception {

// 1、实例化消息生产者 Producer

DefaultMQProducer producer \= new DefaultMQProducer("please\_rename\_unique\_group\_name");

// 2、设置 NameServer 的地址

producer.setNamesrvAddr("localhost:9876");

// 3、启动 Producer 实例

producer.start();

// 4、设置异步调用失败后的重试次数

producer.setRetryTimesWhenSendAsyncFailed(0);

int messageCount \= 100;

// 根据消息数量实例化倒计时计算器

final CountDownLatch2 countDownLatch \= new CountDownLatch2(messageCount);

for (int i \= 0; i < messageCount; i++) {

final int index \= i;

// 创建消息，并指定Topic，Tag和消息体

Message msg \= new Message("TopicTest",

"TagA",

"OrderID188",

"Hello world".getBytes(RemotingHelper.DEFAULT\_CHARSET));

// 5、发送消息并通过 SendCallback 接收异步返回结果的回调

producer.send(msg, new SendCallback() {

@Override

public void onSuccess(SendResult sendResult) {

countDownLatch.countDown();

System.out.printf("%-10d OK %s %n", index,

sendResult.getMsgId());

}

@Override

public void onException(Throwable e) {

countDownLatch.countDown();

System.out.printf("%-10d Exception %s %n", index, e);

e.printStackTrace();

}

});

}

// 等待5s

countDownLatch.await(5, TimeUnit.SECONDS);

// 6、如果不再发送消息，关闭 Producer 实例。

producer.shutdown();

}

}

### **2.1.3 单向发送**

// 这种方式主要用在不特别关心发送结果的场景，例如日志发送。

public class OnewayProducer {

public static void main(String\[\] args) throws Exception{

// 1、实例化消息生产者 Producer

DefaultMQProducer producer \= new DefaultMQProducer("please\_rename\_unique\_group\_name");

// 2、设置 NameServer 的地址

producer.setNamesrvAddr("localhost:9876");

// 3、启动 Producer 实例

producer.start();

for (int i \= 0; i < 100; i++) {

// 创建消息，并指定Topic，Tag和消息体

Message msg \= new Message("TopicTest" /\* Topic \*/,

"TagA" /\* Tag \*/,

("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT\_CHARSET) /\* Message body \*/

);

// 4、发送单向消息，没有任何返回结果

producer.sendOneway(msg);

}

// 5、如果不再发送消息，关闭Producer实例。

producer.shutdown();

}

}

这里简述下上面的消息发送流程：

1.  首先，需要实例化一个生产者 producer，并告诉它 NameServer 的地址，这样生产者才能从 NameServer 获取路由信息。
2.  然后 producer 得做一些初始化，**这是很关键的一步**，它要和 NameServer 进行通信并初始化通信模块等等。
3.  当 producer 已经准备好之后，就可以准备要发的消息内容了，即创建消息，并指定Topic，Tag、消息体。
4.  当消息内容准备好之后，producer 就可以把消息发送出去了。
5.  **这此时 producer 是怎么知道要发送到哪个 Broker 地址上呢？**它会去 NameServer 获取路由信息，比如得到 Broker 的地址是 localhost:10911，然后通过网络通信将消息发送给 Broker。
6.  生产者发送的消息通过网络传输给 Broker，**Broker 端需要对消息按照一定的数据结构进行存储，这点是与 Kafka 有所不同的， Kafka 是在 Producer 端发送批次时就确定了消息格式，而 RocketMQ 是在 Broker 端进行组装的**。存储完成之后，把存储结果告知生产者。
7.  **同步消息**：SendResult sendResult = [producer.send](http://producer.send/)(msg);
8.  **异步消息**：[producer.send](http://producer.send/)(msg, new SendCallback() {}
9.  **单向消息**：producer.sendOneway(msg);

通过上面的发送流程中，我们发现有两个地方是非常关键的，也就是 producer 启动与消息发送。

// 启动 Producer 实例

producer.start();

// 发送消息, 同步或异步

producer.send();

下面会从这两行代码为**切入点，来看看 RocketMQ Producer 的设计与实现原理**。

## **2.2 生产者启动初始化**

我们实例化一个生产者 DefaultMQProducer，然后调用它的 start() 方法进行初始化。

DefaultMQProducer producer \= new DefaultMQProducer("please\_rename\_unique\_group\_name");

producer.start();

看似就2行代码，其实底层实现了很多功能，用阿里的一句话来说就是 「**把复杂流程自己，把简单交给别人**」。具体的内部调用流程如下图所示：

  
![](https://article-images.zsxq.com/lpdA6gw6GoqcWxi9yZnZT2tdHJqG)

从图上可以得出：启动流程相对比较长，这里我们来看下流程步骤：

1.  **参数校验，检查配置是否合法**：生产者组名称是否为空，长度是否超过最大值 255，是否满足其命名规则。
2.  初始化 **MQClientInstance** 实例，注册本地路由消息。
3.  **启动通信模块服务**：这里启动的是 **NettyRemotingClient** 服务，**RemotingClient** 是一个接口，底层使用的通讯框架是 Netty，提供了实现类 **NettyRemotingClient** 。它在初始化时实例化 **Bootstrap**，方便后续用来创建 **SocketChannel**。
4.  **启动后台定时任务**：这里总共有5个定时任务，分别为：
5.  「**定时更新 NameServerAddr 信息**」每隔 2 分钟调用一次，延迟 10 秒执行。
6.  「**定时更新 Topic 路由信息**」每隔 30 秒调用一次，延迟 10 ms 执行。
7.  「**定时向 Broker 发送心跳以及清理下线的 Broker**」每隔 30 秒调用一次，延迟 1 秒执行。
8.  「**定时持久化 Consumer 的 Offset 信息**」每隔 5 秒调用一次，延迟 10 秒执行。
9.  「**定时调整线程池大小**」每隔 1 ms 调用一次，延迟 1 ms 执行。
10.  **启动消息拉取服务**：这两个服务都是用于消费者的，这里我们暂且不展开。消息拉取服务 pullMessageService 是从 Broker 拉取消息的服务；而重平衡服务 rebalanceService 用于消费者的负载均衡，负责分配消费者可消费的消息队列。
11.  **启动消息 push 服务**：发送客户端消息出去。
12.  最后设置服务状态为**启动成功**。

## **03 RocketMQ Producer 之前提知识**

当客户端启动完成之后，Producer 就可以开始发送消息了。在讲解发送消息流程之前，我们先来了解一些**前提知识点**，为更好的理解发送流程做准备。

##   
**3.1 Netty 服务端**

我们知道 RocketMQ 是一款高性能、高吞吐量的消息中间件，除了 Producer 端、Consumer 端需要网络传输之外，数据还需要在服务端集群中进行流转，所以一个「**高效**」、「**可靠**」的网络通讯组件是必不可少的。

在这里，RocketMQ 选择的是 **Netty** 通讯框架，我们在使用 Netty 时首先需要考虑的是业务上的数据「**粘包**」、「**拆包**」问题，**Netty** 为我们提供了一套较为常用的解决方案。

1.  消息格式这块，RocketMQ 使用的规则是最为通用的 「**head**」、「**body**」分离方式，其中 head 用来存储消息的长度，而 body 用来存储真正的数据，具体的实现类在 **NettyRemotingClient** 。
2.  消息收发这块，RocketMQ 将所有的消息都封装在同一个协议类 **RemotingCommand** 中，这么做的好处是：统一规范、减轻网络协议来适配不同的消息类型带来的负担。
3.  Netty 为我们提供的 2 个 Handler。
4.  org.apache.rocketmq.remoting.netty.NettyEncoder：**消息编码**，向 Broker 或者 NameServer 发送消息时，进行编码操作，即将 **RemotingCommand** 对象转换为 **byte\[\]** 格式。
5.  org.apache.rocketmq.remoting.netty.NettyDecoder：**消息解码**，接收 Broker 端返回的消息时，进行解码操作，即将 **byte\[\]** 格式转换为 **RemotingCommand** 对象。

## **3.2 消息格式**

消息格式由 「**MsgHeader**」、「**MsgBody**」组成，这里将消息的「**长度**」、「**标记**」、「**版本**」等重要参数都放在 header 中， 而 body 只存放数据。

下面是一张 Netty 视角的消息格式。

  
![](https://article-images.zsxq.com/FoXV8MUJCEr9NZqWAMyKVdRJ7Cqp)

## **3.3 Topic 路由消息**

### **3.3.1 Topic 创建**

在发送消息之前，我们需要先创建一个 Topic，创建 Topic 的命令可以使用 RocketMQ 自带的命令行工具，如下图所示：

![](https://article-images.zsxq.com/Fg877eoYlr0nKAXF6IfjeGpLAPoJ)

mqadmin updateTopic -b <arg> | -c <arg> \[-h\] \[-n <arg>\] \[-o <arg>\] \[-p <arg>\] \[-r <arg>\] \[-s <arg>\] -t <arg> \[-u <arg>\] \[-w <arg>\]

举例说明：

updateTopic -b localhost:10911 -t message -r 16 -w 16 -p 6 -o false -u false -s false

这里简单介绍下每个参数的作用：

1.  \-b ：**broker 地址**，表示 topic 所在 Broker，只支持单个 Broker，地址为 ip:port。
2.  \-c ：**cluster 地址**，表示 topic 所在 cluster，会向 cluster 中所有的 broker 发送请求。
3.  \-t ：topic 名称。
4.  \-r ：**可读队列数**（4.9.x 版本下默认为 16，在 TopicConfig 中定义）。
5.  \-w ：**可写队列数**（4.9.x 版本下默认为 16，在 TopicConfig 中定义）。
6.  \-p ：指定新topic的读写权限 （W=2|R=4|WR=6）2表示当前 topic 仅可写入数据，4 表示仅可读，6 表示可读可写。
7.  \-o ：set topic's order(true|false)。
8.  \-u ：is unit topic (true|false)。
9.  \-s ：has unit sub (true|false)。

执行上面例子的命令，则会在 localhost:10911 对应的 Broker 下创建一个 Topic，且该 Topic 的**读写队列数都为16**。

### **3.3.2 读写队列数**

**读写队列**是 RocketMQ 独有的，对应 Kafka 来说只有一个 Partition 不区分读写。在一般情况下，这两个队列数值**建议设置为相等**。

这里我们来看下 Client 端是如何对它们进行处理的？

### **1、生产端**

// 按照 QueueData 配置的写队列个数，生成对应数量的 MessageQueue

for (int i \= 0; i < qd.getWriteQueueNums(); i++) {

MessageQueue mq \= new MessageQueue(topic, qd.getBrokerName(), i);

info.getMessageQueueList().add(mq);

}

### **2、消费端**

// 按照 QueueData 配置的读队列个数，生成对应数量的 MessageQueue

for (int i \= 0; i < qd.getReadQueueNums(); i++) {

MessageQueue mq \= new MessageQueue(topic, qd.getBrokerName(), i);

mqList.add(mq);

}

为什么要将这 2 个队列数设置相等呢？如果不相等会出现什么问题？我们来看下。

这里我们设置**写队列为8，读队列为6**，如下图所示：

![](https://article-images.zsxq.com/FqD7VlemwwR2IZE6WOoU3l1C2p6k)

从上图可以看出，**6**、**7** 号队列中的数据一定不会被消费，这里有个规则如下：

1.  writeQueueNum > readQueueNum ：此时大于 readQueueNum 部分的队列永远不会被消费。
2.  writeQueueNum < readQueueNum ：此时所有队列中的数据都会被消费，**但部分读队列中的数据会一直为空**。

这么做的好处：可以让我们更加「**精细**」、「**方便**」的控制读写操作。

### **3.3.3 路由数据结构**

接下来，我们来看看 Topic 路由数据结构，本篇是讲解 Producer，所以这里只分析 Producer 从 NameServer 上获取路由数据，**数据结构如下：**

public class TopicRouteData extends RemotingSerializable {

private String orderTopicConf;

// 队列数据信息

private List<QueueData> queueDatas;

// broker 数据信息

private List<BrokerData> brokerDatas;

private HashMap<String/\* brokerAddr \*/, List<String>/\* Filter Server \*/\> filterServerTable;

....

}

// 队列数据信息

public class QueueData implements Comparable<QueueData> {

// broker name

private String brokerName;

// 读队列数

private int readQueueNums;

// 写队列数

private int writeQueueNums;

....

}

// broker 数据信息

public class BrokerData implements Comparable<BrokerData> {

// 集群

private String cluster;

// broker name

private String brokerName;

// broker 地址列表，主要用于主从节点集群场景

private HashMap<Long/\* brokerId \*/, String/\* broker address \*/\> brokerAddrs;

....

}

下面通过一张图来描述下上面这段代码，如下图所示：

![](https://article-images.zsxq.com/lpZAG2IqPTb9ynxL4S4nSpsn7s9k)

从上图可以得出，该集群 **cluster** 下有3个 **broker**，每个 **broker** 下有 1 个 **master**，2 个 **slave** 组成。所以此时你应该理解了类 **BrokerData** 中有 [HashMap<Long/\* brokerId \*/, String/\* broker address \*/> brokerAddrs](http://hashmaplong/*%20brokerId%20*/,%20String/*%20broker%20address%20*/%20brokerAddrs;)

这个变量的原因了吧。

> 这里需要注意的是：master 节点的编号始终为 0 。

接下来，我们来看看 Topic 路由信息是如何变更的，以及何时变更？

### **3.3.4 定时 Topic 路由信息变更**

在文章开头，Producer 初始化启动时，分析过里面会启动 5 个后台任务，其中一个就是 「**定时更新 Topic 路由信息**」每隔 30 秒调用一次，延迟 10 ms 执行。

那么 Topic 路由信息在何时会发生变化或者更新呢？下面举例说明两种场景，假如 RocketMQ 集群有 3 台 master：

1.  分别向其中的 2 台发送了创建 topic 的命令，此时所有的客户端都知道了该 topic 的数据在这两台 broker 上。此时通过 mqadmin 向第 3 台 broker 发送创建 topic 命令时，**nameServer 上的路由信息发生了变更**，等客户端 30 秒轮询后，即可以拿到最新的topic 路由信息。
2.  此时 topic 分别在 3 台 broker 上都创建了，如果**某台 broker 宕机，nameServer 将其摘除**，等客户端 30 秒轮询后，即可以拿到最新的topic 路由信息。

> 这里需要注意的是：客户端路由变更是依赖这 30 秒的轮询服务，如果此时路由信息已经发生变更，但还未到轮询时间，此时客户端还拿着旧的 Topic 路由信息去访问，会出现短暂报错。

下面通过一张图来描述下 Topic 路由信息变更的全过程，如下图所示：

![](https://article-images.zsxq.com/llQd-czMfZSWkyQXbb8lWFVC2Icb)

这里不展开讲里面的细节，等源码剖析时再详细展开，深度剖析里面的实现细节。

> 需要注意的是：上图中 TopicPublishInfo 是由 TopicRouteData 变种而来，多了一个 messageQueueList 的属性，在 producer 端，该属性为写入队列，即某个topic所有的可写入的队列集合。

## **3.4 与 Broker 之间心跳**

接下来，我们来看看与 Broker 之间的心跳流程，这里主要分两部分：

1.  定时向**有效** Broker 发送心跳。
2.  清理**无效** Broker。

### **3.4.1 定时向 Broker 发送心跳**

先来看第一部分，发送心跳数据的流程，如下图所示：

![](https://article-images.zsxq.com/FirFNaXj8ljgkMh0GLvrFsMI3VSJ)

### **3.4.2 清理无效 Broker**

我们知道 RocketMQ 会获取**所有已经注册的 Topic 所在的 broker 信息**，并将这些信息存储变量brokerAddrTable 中，其存储结构如下：

private ConcurrentMap<String, HashMap<Long, String>> brokerAddrTable = new ConcurrentHashMap<String, HashMap<Long, String>>();

1.  ConcurrentMap 的 **key:** 代表 brokerName。
2.  ConcurrentMap 的 **value：**HashMap<Long, String>，其中这里的 key 代表 brokerId（其中 master 的 brokerId 始终为0），value 代表 ip地址。

**那么如何去判断某个 broker 是否无效呢？**

其实也比较简单，就是判断 **broker 是否不存在于** MQClientInstance#topicRouteTable 这个变量中，它是从nameServer 中同步过来的。

举例说明：如果 brokerAddrTable 中有 **broker 1，2，3**，而 topicRouteTable 只有 **broker 1，2** 的话，那么就需要从 brokerAddrTable 中删除 **3**。

## **04 RocketMQ Producer 之发送流程**

等讲解完上面的一些前提知识，等待 Producer 启动完成之后，就可以开始发送消息了。可以看到在 DefaultMQProducer 中发送消息的方法非常多，大致可进行如下分类：

1.  **普通消息**：没有什么特殊的地方，就是普通消息。
2.  **延迟消息**：延时消息在投递时，需要设置指定的延时级别，即等到特定的时间间隔后消息才会被消费者消费。mq服务端 ScheduleMessageService中，为每一个延迟级别单独设置一个定时器，定时(每隔1秒)拉取对应延迟级别的消费队列。目前RocketMQ不支持任意时间间隔的延时消息，只支持特定级别的延时消息，即 "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h"。
3.  **顺序消息**：对于指定的一个 Topic，Producer 保证消息顺序的发到一个队列中，消费的时候需要保证队列的数据只有一个线程消费。
4.  **事务消息**：通过两阶段提交、状态定时回查来保证消息一定发到 broker。

这里以发送**普通消息**为例，用一张图来说明下其发送流程。

![](https://article-images.zsxq.com/Fucuas9Uk1sf8FZCFLtB1WOtWzlW)

这里我们来重点看下「**MessageQueue 选择机制**」，对于 Kafka 来说是选择**分区**去发送，这里是选择**队列**去发送。

##   
**4.1 MessageQueue 选择机制**

我们知道，一个 Topic 的数据是「**分片存储**」在一个或者多个 Broker 上的，底层的存储介质为「**MessageQueue**」，我们来看下 Producer 端是如何选择具体发送到哪个 MessageQueue 上的?

在 Producer 中是通过 「**selectOneMessageQueue**」方法来进行「**MessageQueue**」选择，该方法通过「**Topic 详细元数据**」和「**上一次选择的 MessageQueue 所在的 Broker**」，来决定下一次的选择。

那么其「**核心的选择逻辑**」究竟是怎么样的呢？

1.  其实也比较简单，就是先选择出来一个 **index**，首次选择时肯定是没有的，此时 RocketMQ 会先算出一个**随机值**，然后在此基础上 + 1。
2.  然后将其和当前 Topic 的 **MessageQueue** 首数量进行取模。

![](https://article-images.zsxq.com/lsivBKVDefjcNHNW2cp015wWYk7T)

除了默认方案，RocketMQ 还提供了 「**发送故障延迟**」方案。

##   
**4.2 MessageQueue 发送故障延迟机制**

在实际的选择过程中，会判断当前是否启用了「**发送故障延迟**」，这个由变量 [sendLatencyFaultEnable](http://sendlatencyfaultenable/) 的值决定，其默认值是 false，开启后即执行 「**发送故障延迟**」方案。

那么其「**核心的选择逻辑**」究竟是怎么样的呢？

1.  开启 for 循环，次数为 **MessageQueue** 的数量，选择出一个 **MessageQueue** ，方法与**默认方案**的方法相同。
2.  通过内存表 **faultItemTable** 校验该 MessageQueue 对应的 broker 是否可用，可用则直接返回。
3.  如果不可用则在尝试从规避的 Broker 中选择一个可用的 broker，如果选出来的 broker 有写队列则返回。
4.  如果无可写队列则最后再用**默认方案**选出一个 **MessageQueue** 返回。

下面通过一张图来形象的描述下上面的选择 **MessageQueue** 过程。

![](https://article-images.zsxq.com/lnHvogyGkrVvPiwXPX-3epjDgyOd)

## **4.3 故障延迟关系对应表**

Producer 端在发送消息时会根据**发送消息耗时来**更新 broker 的「**不可用周期时间**」，消息耗时 与 broker 不可用周期对应关系图如下。

当发送消息耗时在 0~100ms 时，不可用周期为0s。

发送消息耗时在 100ms~550ms 时，broker 不可用周期为 30s。以此类推，当发送消息耗时在 3s~15s 时，broker 不可用周期为 10min。

![](https://article-images.zsxq.com/FolFoPg0ncUcwk245_sffoAUKDuz)

在这种机制下，每次发送消息的时候，通过轮询方式选择出一个 **MessageQueue** ，就会去判断这个 broker 是否在**延迟时间内**，如果还需要等待，那就继续轮询下一个 **MessageQueue** 。

如果都在**延迟时间内**，那就找一个相对可用的，如果此时也没有相对可用的，那就继续轮询，直接返回对应的**MessageQueue** 。

最后调用 Netty 网络通信组件将消息发送出去。

## **05 RocketMQ Producer 之总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过三种发送方式「**同步消息**」、「**异步消息**」、「**单向消息**」带你深度剖析了 **RocketMQ** 中生产者的启动流程以及启动过程中的各种实现细节。

2、带你深度剖析了 **RocketMQ** 中生产者的前提知识点，包括「**Netty 服务端**」、「**消息格式**」、「**Topic 路由消息**」、「**与 Broker 之间心跳**」等。

3、带你深度剖析了 **RocketMQ** 中生产者的发送流程，重点剖析了「**MessageQueue 默认选择机制**」、「**MessageQueue 发送故障延迟机制**」这两种实现方案的细节。

下篇我们来深度剖析「**NameServer 架构设计**」，大家期待，我们下期见。