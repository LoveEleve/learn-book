大家好，我是**华仔**, 又跟大家见面了。

昨天群里有球友问了关于 Kafka 消息幂等、顺序、重复的问题，今天写篇文章来说一下这个，希望对大家有所收获，**如果没有再星球微信群的，请从置顶帖加我好友，拉你进群**。

![](https://article-images.zsxq.com/Fr5yZvxU4Rue03cVfos4EwE0Q0e3)

## **01 消息重复的几个场景**

在 Kafka 使用过程中，消息重复这个问题很常见的，在整个全链路中都有可能会导致消息重复。

通常情况下，消息消费时候都会设置一定「**重试次数**」来避免网络波动造成的影响，同时带来副作用是可能「**出现消息重复**」。

这里我整理了消息重复的几个场景，如下图：

![](https://article-images.zsxq.com/FmD8rCvtIscuL5K1hiZNyzSOwrun)

那怎么解决这些问题？

这里你需要先了解下消息的投递语义。

所谓的消息传递语义是 Kafka 提供的 Producer 和 Consumer 之间的消息传递过程中消息传递的保证性。主要分为三种， 如下图所示：

![](https://article-images.zsxq.com/FjUXxx35NB1rEm_YYuss2x-S7eLd)

了解了这三种语义，再来看「**如何解决消息重复**」，即实现精准一次，可分为以下三种方法：

1.  Kafka幂等性Producer 解决方案：用来保证生产端发送消息幂等。但是该方案有局限性，只能保证单分区且单会话，重启后就算新会话。
2.  Kafka事务解决方案：用来保证生产端发送消息幂等，可以解决 Kafka 幂等性Producer的局限性。
3.  消费端幂等解决方案：用来保证消费端接收消息幂等，最后的兜底方案。

## **02 Kafka 幂等性 Producer 解决方案**

> 幂等性指：无论执行多少次同样的运算，结果都是相同的。即一条命令，任意多次执行所产生的影响均与一次执行的影响相同。

幂等性使用示例：在生产端添加对应配置即可:

Properties props \= new Properties();

// 1. 设置幂等，启动幂等。

props.put("enable.idempotence", ture);

// 2. 配置 acks，注意：一定要设置 acks=all，否则会抛异常。当 enable.idempotence 为 true，这里默认为 all

props.put("acks", "all");

// 3. 注意，这里配置 max.in.flight.requests.per.connection 需要 <= 5 ，否则会抛异常 OutOfOrderSequenceException。

props.put("max.in.flight.requests.per.connection", 5);

这里需要注意的是第三步：

1.  对于 0.11 >= Kafka < 1.1 版本：[max.in.flight.request.per.connection](http://max.in.flight.request.per.connection/) = 1。
2.  对于 Kafka >= 1.1 版本：[max.in.flight.request.per.connection](http://max.in.flight.request.per.connection/) <= 5。

为了让大家更好理解，需要了解下 Kafka 幂等机制设计原理。

![](https://article-images.zsxq.com/FlgB_CyQCrwAIk35l4mWijAeJY4w)

1.  pid：当 Producer每次启动后，会向Broker申请一个全局唯一的pid。（如果重启后pid会变化，这也是弊端之一）。
2.  Sequence Number：针对每个<Topic, Partition>都对应一个从 0 开始单调递增的Sequence，同时Broker 端会缓存这个seq num。
3.  **判断是否重复****：**拿<pid, seq num>去Broker里对应的队列ProducerStateEntry.Queue（默认队列长度为 5）查询是否存在
4.  如果nextSeq == lastSeq + 1，即服务端seq + 1 == 生产传入seq，则接收。
5.  如果nextSeq == 0 && lastSeq == Int.MaxValue，即刚初始化，也接收。
6.  反之，要么重复，要么丢消息，均拒绝。

![](https://article-images.zsxq.com/FkaSCdLra19r_huzwK3aMJy1uSCv)

这种设计针对性解决了以下两个问题：

1.  **消息重复****：**在 Broker保存消息后还没发送ack就宕机了，这时候Producer就会重试，这就造成消息重复。
2.  **消息乱序****：**前一条消息发送失败而其后一条发送成功，前一条消息重试后成功，造成的消息乱序。

那什么时候该使用幂等：

1.  如果已经使用acks=all，使用幂等也可以。
2.  如果已经使用acks=0或者acks=1，表示你的系统追求高性能，对数据一致性要求不高。不要使用幂等。

## **03 Kafka 事务**

使用Kafka事务解决幂等的弊端：单会话且单分区幂等。

事务使用示例：分为生产端 和 消费端

Properties props \= new Properties();

// 1. 设置幂等

props.put("enable.idempotence", ture);

// 2. 当 enable.idempotence 为 true，这里默认为 all

props.put("acks", "all");

// 3. 最大等待数

props.put("max.in.flight.requests.per.connection", 5);

// 4. 设定事务 id

props.put("transactional.id", "my-transactional-id");

Producer<String, String> producer = new KafkaProducer<String, String>(props);

// 初始化事务

producer.initTransactions();

try{

// 开始事务

producer.beginTransaction();

// 发送数据

producer.send(new ProducerRecord<String, String>("Topic", "Key", "Value"));

// 数据发送及 Offset 发送均成功的情况下，提交事务

producer.commitTransaction();

} catch (ProducerFencedException | OutOfOrderSequenceException | AuthorizationException e) {

// 数据发送或者 Offset 发送出现异常时，终止事务

producer.abortTransaction();

} finally {

// 关闭 Producer 和 Consumer

producer.close();

consumer.close();

}

这里消费端Consumer需要设置下配置：isolation.level参数

1.  **read\_uncommitted****：**这是默认值，表明Consumer能够读取到Kafka写入的任何消息，不论事务型Producer提交事务还是终止事务，其写入的消息都可以读取。如果你用了事务型Producer，那么对应的Consumer就不要使用这个值。
2.  **read\_committed****：**表明Consumer只会读取事务型Producer成功提交事务写入的消息。当然了，它也能看到非事务型Producer写入的所有消息。

## **04 消费端幂等**

> 如何解决消息重复？ 就是如何解决消费端幂等性问题。
> 
> 只要消费端具备了幂等性，那么重复消费消息的问题也就解决了。

典型的方案是使用：使用消息表来进行去重：

![](https://article-images.zsxq.com/lqP4Pjbsa2kcRIo1cHHyZKCBmFU9)

1.  这个例子中，消费端拉取到一条消息后，开启事务，将消息Id新增到本地消息表中，同时更新订单信息。
2.  如果消息重复，则新增操作insert会异常，同时触发事务回滚。