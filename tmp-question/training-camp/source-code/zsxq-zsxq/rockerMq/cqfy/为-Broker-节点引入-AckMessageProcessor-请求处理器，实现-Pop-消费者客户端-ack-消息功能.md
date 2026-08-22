上一篇文章写完之后，我记得我跟大家强调了，一定要在第二十二版本代码中，把 Broker 节点返回消费者客户端响应的流程看完了，因为在这个过程中会涉及到一个非常重要的概念，那就是消费信息回执句柄。其实我本来没必要再耗费篇幅讲解这些内容了，但我就是担心有的朋友没有仔细阅读第二十二版本代码中的内容，那索性现在我就带着大家重新梳理一下 Broker 节点返回消费者客户端响应的流程吧。当然，在新的文章中，我在文章中展示的肯定是第二十三版本代码的内容，所以大家在阅读本篇文章以下内容的时候，可以结合我提供的第二十三版本代码边看边思考。因为本篇文章内容非常简单，只有三个知识点要讲解，而要实现的功能也只有两个，我就尽量加快节奏讲解了，其实之后的内容都很简单，直到后面实现任意延迟级别消息功能时，才会稍微有一些难度，最后就是实现事务消息功能，而这个事务消息功能也是简单到姥姥家了，这么说吧，我姥姥能算出 1 + 1 = 2，那你们就能完全掌握事务消息功能。好了，不扯闲篇了，马上开始本章的内容吧。  
  
梳理 Broker 节点返回响应给消费者客户端的执行流程  
  
在之前实现的消费者客户端到 Broker 节点查询消息的功能中，我只在文章中为大家展示到 PopMessageProcessor 请求处理器的 processRequest() 方法，后续的其他内容我多没有为大家展示，因为后续执行的操作非常简单，在 PopMessageProcessor 请求处理器的 processRequest() 方法结尾处，如果消费者客户端确实从 Broker 节点内部查询到了可消费的消息，那么就会得到一个封装了消息本身内容以及额外信息的 RemotingCommand 对象。就像下面代码块展示的这样，请看下面代码块。  
package org.apache.rocketmq.broker.processor;  
  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/10/20

\* @方法描述：处理Pop模式消费者客户端消费消息请求的处理器

\*/

public class PopMessageProcessor implements NettyRequestProcessor {  
  
//省略该类部分内容  
  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：处理消费者客户端消费消息请求的方法

\*/

@ Override

public RemotingCommand processRequest (ChannelHandlerContext ctx,RemotingCommand request) throws RemotingCommandException {  
  
//省略该方法部分内容  
//创建Pop消息响应对象

//创建响应头对象

//从请求对象中得到Pop消息请求头对象

以上内容已经在之前的文章中展示过了，大家肯定很熟悉了。那在 PopMessageProcessor 请求处理器的 processRequest() 方法中得到了封装所有消息内容的 RemotingCommand 对象之后呢？这个 RemotingCommand 对象会被怎么处理呢？按道理说，这个 RemotingCommand 对象肯定要被返回外层方法，但是我们也看到了， 在上面的方法中，最终返回给外层方法的是一个 null，原因很简单，从消息存储引擎中查询消息的所有操作都被注册到 CompletableFuture 对象中了，当这些操作都还没有执行完毕的时候，可能先把 null 返回给外层方法，而直到得到封装所有消息内容的 RemotingCommand 响应对象了，才会执行上面代码块中第 88 行的代码，把得到的响应返回出去 。这个逻辑大家可以理解吧？很好，如果大家理解了上面的逻辑，那现在问题就来了，最后返回出去的这个 RemotingCommand 响应，会被返回到哪里呢？也许大家对此已经没什么印象了，那这个时候，我们就要到调用了 PopMessageProcessor 请求处理器的 processRequest() 方法的外层方法中看一下了。 而这个外层方法就是 LocalMessageService 对象的 sendMessage() 方法，注意，这个时候已经来到 Proxy 节点内部了，大家一定要认识到这一点 ！而我已经把该方法的内容也展示在下面代码块中了，请看下面代码块。  
从上面代码块中可以看到，在 LocalMessageService 对象的 popMessage() 方法中执行了这样一行代码，就是这行：SimpleChannel channel = channelManager.createInvocationChannel(ctx)，这行代码表面上返回了一个 SimpleChannel 对象，但实际上返回的是一个 InvocationChannel 对象，该对象正好是 SimpleChannel 的子类，所以返回值可以用 SimpleChannel 来接收。紧接着在 popMessage() 方法中又创建了一个 InvocationContext 对象，并且还把之前创建的用来获取 Broker 节点响应结果的 future 对象设置到了 InvocationContext 对象中。当然，执行到这里还没完，最后程序又执行了非常关键的一步，那就是把刚创建完毕的 InvocationContext 对象注册到了 InvocationChannel 对象中，也就是执行了 channel.registerInvocationContext(request.getOpaque(), invocationContext) 这行代码。具体的内容我就不展示了，大家可以直接阅读第二十三版本代码的对应内容，那说了这么多，执行这些操作有什么用呢？  
  
看到这里，也许大家就已经回忆起来了， 那就是一旦程序执行了 PopMessageProcessor 请求处理器的 processRequest() 方法中的 NettyRemotingAbstract.writeResponse(channel, request, result) 这行代码，那么程序就会跳转到 InvocationChannel 对象的 writeResponse() 方法中 (这个逻辑的具体流程，大家也去阅读第二十三版本代码即可，这都是之前的内容，我就不在文章中重复展示了)，而执行到 InvocationChannel 对象的 writeResponse() 方法中，这时候内容就简单多了，我把相关内容展示在下面代码块了，请看下面代码块。  
从上面代码块中大家可以看到，InvocationChannel 注册 InvocationContext 对象到自己内部执行的就是 registerInvocationContext() 方法，而在该方法中所做的其实就是把 InvocationContext 缓存到 inFlightRequestMap 成员变量中； 而 InvocationChannel 对象的 writeAndFlush() 方法执行的操作就更简单了，无非就是根据响应唯一标识得到对应的 InvocationContext 对象，然后把从 Broker 节点得到的封装了消息所有内容的 RemotingCommand 对象设置到 InvocationContext 对象持有的 future 对象中 ，具体操作就在 context.handle(responseCommand) 这行代码中，请看下面代码块。  
从上面代码块中可以看到，在 InvocationContext 对象的 handle() 方法中，就把 Broker 节点返回的响应设置到了 CompletableFuture 对象中。那到此为止，我是不是就可以这么说： 如果消费者客户端真的从 Broker 节点中查询到了可消费的消息，那么只要程序执行了 PopMessageProcessor 请求处理器的 processRequest() 方法中的 NettyRemotingAbstract.writeResponse(channel, request, result) 这行代码，在 Proxy 节点中创建的 CompletableFuture 对象就可以得到 Broker 节点返回的封装了所有消息内容的 RemotingCommand 响应对象 。这个逻辑理解起来并不困难吧？  
  
很好，如果大家理解了以上逻辑，那剩下的就很好说了，我们已经知道了，这个 CompletableFuture 对象是在 LocalMessageService 对象的 popMessage() 方法中创建的，并且这个 CompletableFuture 对象也得到了 Broker 节点返回的响应对象，这就意味着 Proxy 节点得到了从 Broker 节点返回的响应对象。 那其实程序只需要提前在这个 CompletableFuture 对象中注册回调方法，在 CompletableFuture 得到响应对象后，就可以直接处理 Broker 节点返回的响应对象了 。这个逻辑可以理解吧？好了，那接下来要怎么处理响应对象呢？很简单，只需要把响应对象返回给消费者客户端即可，这就是 Proxy 节点接下来要执行的操作。当然， Proxy 节点目前得到的是一个 RemotingCommand 响应对象，这个 RemotingCommand 对象只能在 RocketMq 框架内部使用，和客户端通信的时候使用的是 grpc 框架，那显然就要在 Proxy 节点内部，把 RemotingCommand 响应对象中的内容转换到另外的对象中，然后再把这个对象返回给消费者客户端就行了 。这个操作也可以理解吧？  
  
好了，到此为止，我就把要讲解的内容说完了，现在我就可以告诉大家了：之前我为大家展示的 LocalMessageService 对象的 popMessage() 方法缺少了一部分内容，也就是向 CompletableFuture 对象注册 thenApply() 回调方法缺少的内容，而这个空缺，恰好可以被我刚才跟大家分析的内容填补完整。这部分的代码我就不再文章中展示了，内容很多，但是都很简单，大家只需要阅读我提供的第二十三版本代码的对应内容即可。而对于这部分内容，我只有一点要强调， 那就是在 thenApply() 回调方法中，Proxy 节点不仅会把 RemotingCommand 响应对象的内容转换到 Proxy 节点内部的用来封装消息内容的对象中(Proxy 节点内部封装消息内容的对象就是 PopResult 对象)，还会给每一条消息都添加一个额外属性，属性的 key 是 "POP\_CK" 字符串信息，属性的 value 则是这批消息在目标队列的起始偏移量 + 不可见时间 + 消息被弹出时间 + 消息重试队列 Id + 消息所属主题 + 加上Broker 节点名称 + 消息所在队列 Id + 消息在消费队列的具体偏移量，这个键值对就会被存储在消息的 Properties 属性中，而这些内容就会随着消息一起返回给消费者客户端 。现在我想说的是， 在这个键值对中，键值对的 value 就是所谓的消息回执句柄，当消费者客户端为某条消息回复 ack 响应时，就会用到这个 value 。也许大家还不太明白这句话是什么意思，大家可以先记住这一点，然后接着往下看，一会就明白了。总之，现在大家应该清楚了，用来确认每一条消息的重要信息，是怎么返回给消费者客户端了吧？  
好了，到此为止，LocalMessageService 对象的 popMessage() 方法就分析完了，当然，Proxy 节点返回消费者客户端响应的功能还没分析完呢。别忘了，LocalMessageService 对象的 popMessage() 方法还有外层方法呢？我想说的是， 当 Proxy 节点把 RemotingCommand 响应对象的内容转换到 PopResult 对象之后，最终就会把 PopResult 对象返回给 ReceiveMessageActivity 活动器的 receiveMessage() 方法，毕竟该方法就是用来处理消费者客户端发送过来的 ReceiveMessageRequest 请求的 。而在 ReceiveMessageActivity 活动器的 receiveMessage() 方法中执行的操作很简单，就是把消息内容直接返回给消费者客户端，就像下面代码块展示的这样，请看下面代码块。  
在上面代码块中可以看到，ReceiveMessageActivity 对象的 receiveMessage() 方法中也有一个 thenAccept() 操作，该操作正好就是用来处理从内层方法获得的封装了所有消息内容的 PopResult 对象的，而在这部分操作的最后，就会把 PopResult 对象的内容返回给消费者客户端，这个操作已经非常清楚了。而在这个过程中，还执行了一个非常重要的操作， 那就是又从每一条消息的属性中得到了用来确认唯一一条消息的消息回执句柄，并且把这个回执句柄存储到了 ReceiptHandleProcessor 回执句柄处理器中，也就是上面代码块第 78 行代码对应的操作 。我在文章中就不对这行代码背后的内容做详细讲解了，要是具体展开的话，又要引入好几个新的类，但说实话，这些类的内容都很少，也很简单，所以就留给大家自己去第二十三版本代码中查看吧。大家一定要记住这里的内容，下一小节我们实现消息 ack 功能时，就会用到缓存回执句柄处理器中的消息回执句柄！到此为止，从 Broker 节点查询到的可消费的一批消息，就被 Proxy 节点返回给了消费者客户端了，接下来就可以让消费者客户端消费这批消息了。  
  
实现消费者客户端 ack 功能  
  
当然，消费者客户端消费消息的功能并不需要被我们实现，因为它是业务上的操作，而业务操作自然是由开发业务的程序员自己定义。消费者客户端得到了消息，只需要按照业务程序员事先定义好的逻辑处理消息即可。这一部分的内容我在前面的代码中也为大家实现了，之前实现 Pop 模式消费者客户端的时候，我为大家提供了一个测试类，这个测试类中就简单展示了消费者客户端消费消息的操作，请看下面代码块。  
从上面代码块中可以看到，当消费者客户端从 Broker 节点得到了一批可以消费的消息之后，就会直接处理这批消息，当然，我在上面代码块中展示的逻辑非常简单，那就是只是在控制台输出了每一条消息的关键信息，大家理解这个意思即可，这个也不是我们要关注的重点。我们要关注的是上面代码块的第 50 行代码，可以看到，在循环中处理完每一条消息之后，消费者立刻执行了 ack() 方法，这里大家应该也意识到了， 那就是消费者客户端成功消费了每一条消息之后，就要为这条消息执行 ack() 方法，而在该方法中就会为这条消息返回 ack 响应给 Broker 节点 。因为接下来的内容都很简单，所以我就直接为大家展示早就实现完毕的代码吧。接下来，就请大家看看在消费者客户端的 ack() 方法中都定义了什么操作吧，请看下面代码块。  
以上代码块展示的就是消费者客户端向 Broker 节点回复消息 ack 响应的方法，当然，从代码中我们也看出来了，实际上回复的是一个 AckMessageRequest 请求对象，大家理解这个意思即可。消费者客户端执行了 ClientManager 客户端管理器的 ackMessage() 方法把 AckMessageRequest 请求对象发送出去了，这个 AckMessageRequest 请求对象肯定会被发送给 Proxy 节点，那接下来我们就要到 Proxy 节点中看看这个 AckMessageRequest 请求是怎么被处理的了。  
  
为 Broker 节点引入 AckMessageProcessor 请求处理器  
  
老规矩，Proxy 节点接收到 AckMessageRequest 请求之后肯定会先把请求交给 GrpcMessagingApplication 对象处理，这就意味着肯定要在 GrpcMessagingApplication 中定义一个 ackMessage() 方法用来接收并处理该请求，我把对应的代码已经展示在下面代码块中了，请看下面代码块。  
从上面代码块中可以看到，消费者客户端发送过来的 AckMessageRequest 请求又交给 GrpcMessingActivity 消息活动处理器去处理了，那接下来我们就到该对象中看看它是怎么处理 GrpcMessingActivity 请求的，请看下面代码块。  
从上面代码块中可以看到，为 了处理消费者客户端发送过来的 AckMessageRequest 请求，我们又给 Proxy 节点新定义了一个 AckMessageActivity 活动器，这个 AckMessageActivity 对象专门用来处理消费者客户端发送过来的 AckMessageRequest 请求 ，那接下来我们就到这个 AckMessageActivity 对象中看看，它究竟是怎么处理请求的，请看下面代码块。  
上面代码块中的内容非常简单，逻辑也很清晰，那就是先根据 AckMessageRequest 请求自己携带的消息回执句柄信息，到 Proxy 节点的回执句柄管理器中得到了对应的消息回执句柄，当两者的信息对应上了，接下来就可以在 AckMessageActivity 活动器的 processAckMessage() 方法中调用 MessagingProcessor 消息处理器的 ackMessage() 方法进一步处理消费者客户端发送的信息了。当然， MessagingProcessor 消息处理器也不会直接处理消息，按照我们之前实现的程序流程的理解，这个 MessagingProcessor 消息处理器在得到信息后，还会经过一系列调用，最后把这些信息交给 LocalMessageService 对象的 ackMessage() 方法来处理，而在这个过程中，消费者客户端请求的信息已经被 Proxy 节点封装到一个 RemotingCommand 对象中了，毕竟 RocketMq 内部模块通信使用的是 Remoting 协议 。就像下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到，LocalMessageService 对象的 ackMessage() 方法执行的操作也很简单，就是直接得到了 Broker 节点内部的 AckMessageProcessor 请求处理器，当然，这个处理器是第二十三版本代码新引入的，然后就把封装了 ack 信息的 RemotingCommand 对象交给 Broker 节点去处理了。这些逻辑都很简单吧？如果大家理解了这些逻辑，那接下来我们就可以来到 Broker 节点内部，看看 Broker 节点新引入的 AckMessageProcessor 请求处理器是怎么处理消息 ack 信息的吧，我已经把相关代码展示在下面了，请看下面代码块。  
上面代码块的内容非常简单，可以看到，Broker 节点在处理 ack 消息的时候，只是先对要 ack 的消息执行了一些简单校验，如果校验都通过了，那接下来就可以执行 appendAck() 方法，真正 ack 消息了。当然，这个 appendAck() 方法我们目前还没有实现。这倒不是因为该方法实现起来非常困难，恰恰相反，该方法实现起来非常简单，我们已经知道了， 所谓消息 ack，其实就是根据消息的唯一信息到缓存在 PopBufferMergeService 对象中的 PopCheckPointWrapper 对象中匹配唯一一条消息，匹配成功后更新消息位图的标志即可，如果消息所有位图都更新成功了，那么就可以认为这一批消息已经被成功 ack 了，之后就可以把这批消息的消费偏移量更新到偏移量管理器中了 。这个逻辑大家应该还记得吧？  
  
所以我们现在只需要在 appendAck() 方法定义这些内容即可，比如说我们可以在该方法中创建一个 AckMsg 对象，这个对象就封装了这条消息的唯一信息，也就是消费组信息，消息所属主题，消息被 Pop 出的时间，还有偏移量等等信息，这些内容我就不再重复了。总之把这条消息的 AckMsg 对象定义完毕了，就可以把这个 AckMsg 对象交给 PopBufferMergeService 对象，让 PopBufferMergeService 对象去自己内部匹配唯一一条消息，执行真正的消息 ack 操作。我已经把这些内容都实现了，请看下面代码块。  
接下来展示的就是 PopBufferMergeService 对象的相关内容，请看下面代码块。  
到此为止，消息 ack 的整个功能就实现完毕了，通过上面的代码块，大家应该也掌握了一条被消费的消息是怎么被 ack 的。现在大家已经可以去阅读第二十三版本的代码了，当然，第二十三版本代码中还是有很多内容没在文章中展示，大家也可以再等等，等后面的文章更新完毕了，就可以阅读第二十三版本代码的所有内容了。本章内容推进得确实有些快，这一点我也意识到了，考虑到实现的功能非常简单，我才加快了文章的节奏。后面几篇文章的内容依然很简单，毕竟 RocketMq 框架课程快结束了，已经接近尾声，确实没什么深奥的内容了。大家还是尽快接受这个遗憾的现实吧，接下来的一段时间你们确实掌握不了什么更高深的技术了。  
  
没错，内容简单并不意味着大家没有疑问，其实我也想到了，看完整篇文章的内容，大家肯定很好奇， 在我们目前重构的 PopBufferMergeService 类中，或者说在我们目前实现的消息 ck 和 ack 功能中，似乎只为 ack 成功的消息更新了消息位图标志，如果一批消息都被 ack 成功了，那么目前程序所做的也仅仅是把这一批消息的位图都更新完毕，这些操作执行完毕了就没有下文了，别忘了一个最重要的操作呀，那就是把这批消息对应的消息偏移量更新到消费偏移量管理器中啊，这个操作怎么没有执行呢 ？这肯定是困扰着大家的第一个问题。  
  
困扰着大家的第二个问题也很明显，那就是在我们实现的 PopBufferMergeService 对象中，缓存着所有待 ack 的消息的 PopCheckPointWrapper 对象，这些对象都缓存在 PopBufferMergeService 的 buffer 成员变量中，这种缓存方式确实没问题。但现在新的问题来了， 这个 buffer 成员变量可以无限存储 PopCheckPointWrapper 对象吗？肯定不能吧，因为你总要考虑内存溢出问题，如果 buffer 中存储了很多待确认的 PopCheckPointWrapper 对象，而消费者客户端 ack 消息偏偏慢得离谱，大量的 PopCheckPointWrapper 对象堆积在内存中，显然会造成内存溢出啊。这个问题该怎么解决呢 ？也就是说，我们必须要考虑 PopCheckPointWrapper 对象清除的问题，要考虑清除，PopCheckPointWrapper 对象在什么时候会被清除。  
  
当然，以上两个问题肯定就不在本章解决了，这是下一章要解决的问题，到时候大家就全都清楚了。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/cc78igh30etgryks*  
*All content belongs to its respective owners and creators.*