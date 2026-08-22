上一章我们已经构建了一个简单模式的消费者客户端，并且已经顺利启动了该客户端。按照我们对生产者客户端启动流程的理解， 当消费者客户端启动之后，也应该像生产者客户端那样，把自己的信息注册到 Proxy 节点中，并且定期执行心跳检测任务，以及同步设置信息的任务，这两个定时任务会在 ClientManagerImpl 客户端管理器中被启动，然后定期执行 ，就像下面代码块展示的这样，请看下面代码块。  
package org.apache.rocketmq.client.java.impl;  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/6/10

\* @方法描述：客户端管理器实现类

\*/

public class ClientManagerImpl extends ClientManager {  
//客户端对象

private final Client client;  
//RPC客户端连接表，key为服务端地址，value为对应的RPC客户端连接

@ GuardedBy ("rpcClientTableLock")

private final Map < Endpoints,RpcClient > rpcClientTable;  
//保护客户端连接表并发安全的读写锁

private final ReadWriteLock rpcClientTableLock;  
//定时任务执行器

private final ScheduledExecutorService scheduler;  
//异步工作执行器，这个执行器专门执行rpc客户端发送请求的操作

private final ExecutorService asyncWorker;  
//省略该类其他内容  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：启动客户端管理器的方法

\*/

上面这些内容大家都很熟悉吧，如果大家之前认真阅读了第十八版本代码，仔细回顾了之前的内容，那对上面两个定时任务内部要执行的操作肯定也很熟悉了，在消费者客户端执行 doHeartbeat() 心跳检测定时任务时，会创建一个 HeartbeatRequest 心跳请求，然后把该请求发送给 Proxy 节点即可，这一点没什么可说的，操作非常简单；而在消费者客户端执行 syncSettings() 定时任务时，就会来到 ClientImpl 对象中执行以下操作，请看下面代码块。  
从上面代码块中可以看到， 消费者客户端最终所做的其实就是使用和 Proxy 节点构建的 ClientSessionImpl 会话器向 Proxy 节点发送 TelemetryCommand 请求，这个请求封装了客户端的设置信息，当然，Proxy 接收到 TelemetryCommand 请求之后，也会把自己内部的设置信息返回给消费者客户端，以便于消费者客户端同步到本地 。这些内容对大家来说肯定都不陌生了吧？因为生产者客户端就是这么做的。  
  
很好，消费者客户端要执行的两个定时任务就分析完毕了，当然，我一开始就跟大家说过的，消费者客户端也要把自己的信息注册给 Proxy 节点，这个注册消费者客户端信息到 Proxy 节点的操作，就是依靠刚才分析的两个定时任务来完成的，因为在执行那两个定时任务的时候，会向 Proxy 节点发送对应的请求，而消费者客户端的信息就会被封装到请求中发送给 Proxy 节点，之后 Proxy 节点就可以处理这些客户端信息了。这些也都是旧知识，我就不再展开讲解了。  
  
根据刚才的分析，我们已经知道了不管是生产者客户端和消费者客户端都会向 Proxy 节点执行相同的定时任务，也就是说，生产者客户端和消费者客户端都会向 Proxy 节点发送相同的请求，那现在我想跟大家讨论的是，Proxy 节点在处理请求的时候，怎么判断该请求是生产者客户端发送的，还是消费者客户端发送的呢？其实这个问题本来不需要讨论，实现起来太简单了，我就怕有些朋友在阅读我提供的第十九版本代码时注意不到这些细节，所以忍不住想在文章中提醒一下。  
  
实际上是这样的， 当客户端和 Proxy 节点建立网络连接之后，就会把自己的设置信息全都注册到 Proxy 节点内部，也就是说，那个时候，Proxy 节点已经得到了客户端的 ClientSettings 信息，这些信息包括客户端的客户端类型，也就是当前客户端是生产者客户端还是消费者客户端 ，这是一个很关键的操作。这样一来，Proxy 节点在处理客户端请求的时候，就会根据客户端的类型来对请求执行不同的操作。除此之外， 当客户端向 Proxy 节点发送 TelemetryCommand 请求，也就是定期执行 syncSettings() 定时任务时，客户端还会把信息类型设置到请求中，如果是生产者客户端向 Proxy 节点发送 TelemetryCommand 请求，就会把消息模式设置为发布模式，如果是消费者客户端向 Proxy 节点发送 TelemetryCommand 请求，就会把消息模式设置为订阅模式 。以此让 Proxy 节点区分来自不同类型客户端的 TelemetryCommand 请求，具体操作请看下面代码块。  
好了，到此为止，和消费者客户端的内容就暂时告一段落了，总之，消费者客户端已经把自己的信息都注册到 Proxy 节点了，那接下来，我们就应该到 Proxy 节点内部，看看 Proxy 节点会怎么处理消费者客户端的信息。还是和之前一样，Proxy 节点处理消费者客户端信息的操作也非常简单，因为我们已经把 Proxy 节点处理生产者客户端信息的功能实现了，而 Proxy 节点又是在同样的方法中处理消费者客户端的信息，所以我们只需要仿照生产者客户端那样实现即可。  
  
Proxy 节点处理客户端信息的方法有两个，对应的是两个请求，一个是 TelemetryCommand 请求、一个是 HeartbeatRequest 请求，当请求被 Proxy 服务端接收到之后，首先会被 GrpcMessagingApplication 对象处理，就像下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到，不管是 TelemetryCommand 请求还是 HeartbeatRequest 请求，都会交给 grpcMessingActivity 活动器处理，所以接下来我们直接看看该活动器中做了什么即可(现在展示的这些代码都是很早之前就实现的内容，我就不详细展开了，也没必要详细展开，快速过一遍，就当帮助大家复习了，我就直接展示代码了)，请看下面代码块。  
从上面代码块中可以看到，不管是 TelemetryCommand 请求还是 HeartbeatRequest 请求，又都交给 ClientActivity 客户端活动器处理了，而这个 ClientActivity 客户端活动器我们也早就实现了，所以接下来就请大家跟我一起到客户端活动器中看看它是怎么处理这两个请求的即可。  
  
首先我们还是先看一下 ClientActivity 活动处理器是怎么处理 TelemetryCommand 请求的吧，请看下面代码块。  
以上代码块的内容虽然很多，但逻辑都很简单，之前我们仅仅是让 ClientActivity 客户端活动器处理生产者客户端的信息，现在终于为 ClientActivity 对象实现了处理消费者客户端信息的功能。并且我们还能看到， ClientActivity 活动处理器在处理生产者客户端和消费者客户端信息的时候，执行的操作几乎都是一致的，都是为客户端创建对应的 GrpcClientChannel 对象，最后又都是把信息交给 MessagingProcessor 处理器处理，只不过需要根据请求的中消息的模式判断是哪种客户端的消息，然后再做具体处理即可， 这些内容之前已经讲过了，而且代码中的注释非常详细，所以我就不再过多解释上面代码的内容了，唯一要解释的一点是为客户端创建的 GrpcClientChannel 对象，这个对象在现在的代码中还用不到，但是等到第 27 版本代码，也就是我们实现事务消息功能时，这个 channel 就会用上了，大家有点印象就行。  
  
好了，ClientActivity 客户端活动器处理消费者客户端发送过来的 TelemetryCommand 请求的操作已经实现完毕了，接下来我们再一起看一下 ClientActivity 客户端活动器处理 HeartbeatRequest 心跳请求的操作吧，请看下面代码块。  
从上面代码块中可以看出， ClientActivity 客户端活动器在处理消费者客户端发送过来的心跳请求时，执行的操作和处理生产者客户端发送的心跳请求几乎没什么区别，都是直接把客户端信息进一步交给 MessagingProcessor 消息处理器去注册 ，这也没什么可展开讲解的，只要大家顺着我提供的第十九版本代码阅读即可。当然，我相信这时候大家肯定也有意识到了，ClientActivity 客户端活动器不管是在处理消费者客户端发送过来的 TelemetryCommand 请求还是 HeartbeatRequest 请求时，最后都会执行 registerConsumer() 方法，这里我要给大家解释一下，重复执行 registerConsumer() 方法并不意味着会把相同的消费者客户端信息注册到节点内部，而是以此来不断更新节点内部缓存的客户端信息最新被更新的时间戳，主题的逻辑就留给大家去我提供的第十九版本代码中阅读吧，这些内容都很常规，非常简单， Proxy 节点的 MessagingProcessor 消息处理器是怎么注册生产者客户端信息的，就会怎么注册消费者客户端信息，只不过生产者客户端的信息会被注册到 ProducerManager 生产者管理器中，而消费者客户端信息会被注册到 ConsumerManager 消费者管理器中 ，这些内容就当是本章的一个作业，交给大家去第十九版本代码中验证吧。  
  
最后我还是要补充一句， ConsumerManager、ProducerManager 管理器对象其实都是 Broker 模块中的组件，在本地模式中，Broker 是内嵌在 Proxy 中运行的，这也就是说其实生产者和消费者客户端的信息最终还是交给了 Broker 节点来管理 。至于 Broker 节点管理这些信息有什么用，现在大家还不清楚，但等到消费者从 Broker 节点内部获取可消费的消息时，大家就知道 Broker 节点缓存的这些信息是如何发挥作用的了。好了朋友们，本章内容就到此结束了，可以看到，在本章我几乎没怎么实现新功能，就算有新的代码，也只是对旧代码的复刻，顶多就是在旧代码的基础上稍作改动，这些内容实在是太简单了，所以我也没有耗费更多的篇幅，对很多内容几乎都是蜻蜓点水，点到为止，并没有深入展开，总是重复相同的内容确实有些无聊，希望大家可以理解。现在大家已经可以阅读第十九版本代码了，下一章我们会再次回到消费者客户端中，真正实现消费者客户端从 Proxy 节点获取可消费消息的功能，朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/pin12cngol1hc6om*  
*All content belongs to its respective owners and creators.*