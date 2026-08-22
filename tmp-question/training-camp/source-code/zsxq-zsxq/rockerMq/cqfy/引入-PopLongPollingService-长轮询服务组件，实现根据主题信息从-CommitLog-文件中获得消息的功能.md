在上一章我们已经实现了消费者客户端从 Broker 节点查询并获取可消费的消息的功能，想必大家也都把二十一版本代码阅读完毕了，我相信读完代码之后大家心里都有一种感觉，那就是从功能的完整性上来说，我们在上一章实现的消费者客户端从 Broker 节点获取消息功能还有些残缺。当我们把 DefaultMessageStore 消息存储器的 getMessage() 方法实现完毕之后，该功能的实现也就止步于此了，其实在这之后还有很多功能有待完善。就比如说，把查询到的消息返回给消费者客户端的功能还没有实现，也许第二十一版本代码展示的功能是完整的，但是那些内容我并没有跟大家讲解。除此之外，还有上一章结尾遗留的两个问题，这些也都是等待我们实现的功能。在第二十二版本代码中，我就会为大家把这些问题全都解决了。  
  
初步分析 Broker 节点返回消费者客户端响应的操作流程  
  
在正式开始本章内容的时候，我希望大家先别关注我们接下来要实现什么功能，暂时把所有的问题全部抛在一边，请大家跟我一起回忆上一章的一个内容。在上一章我们为 DefaultMessageStore 消息存储引擎实现了完整的 getMessage() 方法，在该方法中 Broker 节点从消息存储引擎中获得了指定的消息，然后把消息封装到了一个 GetMessageResult 对象中，然后把 GetMessageResult 对象返回给了外层方法，就像下面代码块展示的这样，请看下面代码块。  
package org.apache.rocketmq.store;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/6/25

\* @方法描述：默认的消息存储引擎，这个消息存储就应就会把生产者客户端生产的消息提交给CommitLog对象，然后存储到CommitLog文件中

\* 可以说这个类的对象和CommitLog类的对象是rocketmq消息存储模块中最重要的两个功能对象

\*/

public class DefaultMessageStore implements MessageStore {  
  
//省略该类其他内容  
//ConsumeQueue消息队列存储引擎

protected final ConsumeQueueStoreInterface consumeQueueStore;  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：异步获取指定主题队列消息的方法

\*/

@ Override

public CompletableFuture < GetMessageResult > getMessageAsync (String group,String topic,int queueId,long offset,int maxMsgNums,MessageFilter messageFilter) {

return CompletableFuture.completedFuture (getMessage (group,topic,queueId,offset,maxMsgNums,messageFilter));

}  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

从上面代码块可以清楚地看到，当消息从消息存储引擎中查询出来之后，就会被封装到一个 GetMessageResult 对象中(我把 GetMessageResult 类的内容也展示在上面代码块中了，大家一定要仔细看一看，后面还会用到)，然后把该对象返回给外层方法。那在这之后呢？外层方法得到 GetMessageResult 对象之后会执行什么操作呢？这不用说，肯定是继续把查询到的消息返回给外层方法，直到回复消费者客户端响应，这个逻辑可以理解吧？  
  
很好，那我们就顺着这个操作流程到外层方法看一看，DefaultMessageStore 消息存储引擎 getMessage() 方法的外层方法，也就是调用了该方法的方法，可以追溯到 PopMessageProcessor 请求处理器的 popMsgFromQueue() 方法，那接下来我们就到这个方法中看一看，当 PopMessageProcessor 请求处理器从消息存储引擎中得到了可消费的消息之后，会执行什么操作，我把具体操作都展示在下面代码块中了，请看下面代码块。  
从上面代码块中可以看到，当 PopMessageProcessor 请求处理器在它的 popMsgFromQueue() 方法中调用了消息存储引擎的 getMessageAsync() 方法之后，又向 getMessageAsync() 方法返回的 CompletableFuture 对象中注册了一个回调方法，也就是 thenCompose() 方法中执行的操作。不过这些操作对大家来说应该都很熟悉了，在前面的章节我已经为大家展示并且将结果这些内容了，所以这部分内容并不是我们要关注的重点。 紧接着我们就能看到在 thenCompose() 方法之后又注册了一个 thenApply() 回调方法，而这部分的内容我一直没有为大家实现，原因很简单，在 RocketMq 源码中，在这个回调方法中就要执行一个非常重要的操作，那就是把从消息存储引擎中查询到的消息返回给外层方法 。之前我一直没有为大家实现这部分的内容，是因为我们根本就没有从消息存储引擎中得到消费者客户端要消费的消息呢。  
  
好了，那现在我们已经从消息存储引擎中得到了消费者客户端要消费的消息，并且这些消息被封装在了一个 GetMessageResult，那接下来要在 thenApply() 方法中执行什么操作呢？直接返回这个封装了可消费消息的 GetMessageResult 对象吗？如果是我们自己开发的消息队列框架，或者说按照自己的意愿开发的框架，那么我们当然可以这么做，但我们现在是仿照 RocketMq 源码来实现一个消息队列框架，就得遵循 RocketMq 框架的设计理念，而在 RocketMq 框架中，PopMessageProcessor 请求处理器的 popMsgFromQueue() 方法的返回值已经明确了，该方法会返回一个 CompletableFuture 对象，并且，这个 CompletableFuture 对象会持有一个 Long 整数，这就意味着 PopMessageProcessor 请求处理器的 popMsgFromQueue() 方法会返回一个整数给外层方法，而我们希望返回的是封装了消息结果的 GetMessageResult 对象，这显然和我们预期的结果不太一致，那这是怎么回事呢？  
  
实际上是这样的， PopMessageProcessor 请求处理器的 popMsgFromQueue() 方法返回的确实是一个整数值，并且这个整数值就表示刚刚获得消息的目标队列中剩余的可消费的消息数量 ，这个可消费的消息数量可是大有用处，一会大家就清楚是怎么回事了。很好，既然源码就是这么设计的，那我们就这么实现吧，那怎么得到刚才获得消息的目标队列中剩余的可消费消息的数量呢？这个也很简单，刚才消息存储引擎的 getMessageAsync() 方法不是已经返回了一个 GetMessageResult 对象吗？而在上面代码块中我也为大家展示过了， GetMessageResult 的 nextBeginOffset 成员变量和 maxOffset 成员变量分别表示目标队列最新的消费偏移量和目标队列最大消息偏移量，让 maxOffset - nextBeginOffset 不就得到了目标队列剩余可消费的消息吗 ？我们只要得到了这个值，然后返回给外层方法不就实现了和源码一致的功能吗？由此可见，这个功能实现起来真的非常简单。  
  
但是，我们不能光顾着简单，别忘了我们最终的目的，我们最终肯定是希望把可消费的消息返回给消费者客户端，也就是要把 GetMessageResult 封装的消息内容返回出去，但现在 popMsgFromQueue() 方法的返回值已经确定了，这可怎么办呢？这时候肯定有朋友已经注意到了， 在 popMsgFromQueue() 方法中传进来一个方法参数，也就是该方法的第三个参数，该参数就是一个 GetMessageResult 对象，那显然这个 GetMessageResult 参数就是从外层方法传递进来的。那这下就好办了，我们只需要把可消费的消息从消息存储引擎返回的 GetMessageResult 对象中转移到 popMsgFromQueue() 方法的第三个参数对象中 ，这样一来，外层方法不就得到可消费的消息了吗？大家可以品味品味这些逻辑，如果这些逻辑都理解了，那接下来就让我为大家展示一下相关的代码，请看下面代码块。  
上面代码块展示的内容非常清晰，我就不再重复讲解了，总之到此为止，我们就仿照源码捣鼓了一通，终于可以让 PopMessageProcessor 请求处理器的 popMsgFromQueue() 方法把从消费存储引擎获得的消息，以及获取消息的目标队列剩余可消费的消息数量都返回给外层方法了。很好，那接下来新的问题就来了，当外层方法获取到这些信息之后，又会执行什么操作呢？这个时候我们就得先来到外层方法中看一看了，我把执行了 popMsgFromQueue() 方法的外层方法的内容展示在下面了，请看下面代码块。  
从上面代码块中可以看到，popMsgFromQueue() 的外层方法是名称为 popMsgFromTopic() 的方法，在该方法中会在一个循环中不断执行 popMsgFromQueue() 方法，而循环的原因也很简单， 那就是 Pop 消费者模式可以从主题下所有队列中获取可消费的消息，直到获取的消息数量达到阈值 。好了，那在阅读了 popMsgFromQueue() 的外层方法之后，程序又对之前返回的目标队列剩余可消费的消息数量，以及消息内容本身执行了什么操作呢？这个问题的答案上面代码块已经展示得很清楚了， 首先我们可以看到上面代码块展示的 popMsgFromTopic() 方法的返回值也是一个结果值为 Long 整数的 CompletableFuture 对象，这就意味着目标队列剩余可消费的消息数量肯定还会作为返回值返回给外层方法，而在该方法中我们可以进一步看到，最终返回的这个 Long 整数实际上可能是多个消费队列中剩余可消费数量得总和 ；与此同时我们也可以看到，上面代码块展示的 popMsgFromTopic() 方法的第三个参数也是一个 GetMessageResult 对象，这个方法参数肯定是调用了 popMsgFromTopic() 方法的外层方法传递进来的，并且这个对象会在循环中不断传递给内部循环执行的 popMsgFromQueue() 方法， 这就意味着当 GetMessageResult 参数对象在 popMsgFromQueue() 方法内部封装了查询到的消息之后，popMsgFromTopic() 方法的外层方法也就可以得到消息存储引擎返回的消息了 ，这个逻辑可以理解吧？如果大家理解了这些逻辑，那接下来我们就可以到 popMsgFromTopic() 方法的外层方法中看一看，看看外层方法得到了这些重要信息之后会执行什么操作，而 popMsgFromTopic() 方法的外层方法就是 processRequest() 方法，所以我们直接回到该方法中查看具体内容即可，请看下面代码块。  
从上面代码块中可以看到，当 popMsgFromTopic() 方法把队列剩余可消费的消息数量以及查询到的所有消息返回给外层 processRequest() 方法后，processRequest() 方法似乎并没有处理这些信息，原因很简单，因为处理这些信息的功能我还没有为大家实现。大家可以看到，在上面代码块第 75 行到 78 行的内容是空白的， 本来这里要向 getMessageFuture 这个对象中注册一个回调方法，该回调方法就会处理从消费存储引擎中得到的消息信息 ，但是我并没有真的把这个回调方法定义出来，因为接下来我想跟大家一起讨论一下，在这个回调方法中究竟应该执行什么操作。  
  
引入 PopLongPollingService 长轮询组件  
  
从上一小节最后展示的代码块中我们可以看到，在 PopMessageProcessor 请求处理器的 processRequest() 方法中创建了一个 RemotingCommand 通信对象，并且这个对象是响应对象，在引入了 Proxy 节点之后，我们也清楚了 Broker 节点肯定是先把响应回复给 Proxy 节点，然后 Proxy 节点再把可以消费的消息返回给 Pop 模式消费者客户端。 那这就意味着从消息存储引擎中查询到的消息都要从 GetMessageResult 对象封装到 RemotingCommand 响应对象中才行 ，那现在看来，这个操作显然是在尚未实现的 thenApply() 回调方法中执行的，因为该回调方法要执行的操作就是处理查询到的消息信息，这一点是毋庸置疑的，所以一会我们实现 thenApply() 方法的时候，可以把刚才分析的内容实现了。  
  
那除了把 GetMessageResult 对象封装的消息内容设置到 RemotingCommand 响应对象中，在 thenApply() 回调方法中还需要执行什么操作呢？还有一个非常重要的操作需要执行，也许有的朋友已经意识到了，而有的朋友还在思索，接下来就让我为大家详细分析一种程序可能面临的新情况吧。请大家思考一下，当消费者客户端到 Broker 节点获取对应主题下的消息时，很有可能出现这样一种情况， 那就是 Broker 节点内部根本就没有对应的消息，也就是说该主题下并没有任何消息可以供消费者客户端消息 。如果是这种情况那 Broker 节点应该执行什么操作呢？根据我之前为大家分析的内容来看， Broker 节点显然不能直接就返回消费者客户端空响应，而是应该把消费者客户端的请求挂起，等待一段时间，直到有消息到来，那就可以唤醒被挂起的请求消费消息，然后返回客户端响应；或者是被挂起的请求等待超时，再返回客户端响应 。这些逻辑大家可以理解吧？  
  
如果大家理解了以上逻辑，那接下来的内容就很好说了，我们刚才已经分析了，在尚未实现的 thenApply() 回调方法中要执行的就是处理从消息存储引擎返回的消息信息的操作，而返回的消息信息除了包含要返回给消费者客户端的可消费消息，还包含主题下多个队列中剩余的可消费消息的数量，如果是这样的话， 那我们就可以在 thenApply() 回调方法中添加一个新的功能，那就是首先判断是否从消息存储引擎中获取到了消息，如果没有获取到消息，就意味着 Broker 节点内部的主题下没有可消费的消息，那这个时候就要执行挂起请求的操作 ； 如果获取到了消息，那接下来就要判断主题队列下是否还有可消费的消息，如果存在可消费的消息，那就要唤醒正被挂起的请求继续消费消息 。这两个逻辑大家可以理解吧？  
  
如果大家理解了以上的逻辑，那接下来我们就可以从代码层面上实现刚才分析的内容了，也就是要引入长轮询组件，那这个组件该怎么定义呢？如果只是给这个组件定义一个名字那确实很容易，我可以张口就来 ，可以直接把它定义为 PopLongPollingService 类 ，可名字确定之后，内容该怎么定义呢？这就需要我们好好思考思考了，当然，我们也不能盲目思考，寻找合适的突破点，然后一点点构建它，为它添加新的内容即可。而这个突破点我已经找到了， 那就是我们应该首先思考请求被挂起和被唤醒的操作该如何实现 。  
  
其实首先思考唤醒长轮询请求的操作更容易实现，因为我们现在已经实现了 Broker 节点为消费者查询可消费的消息的功能，确切地说，只需要执行 PopMessageProcessor 请求处理器的 processRequest() 方法，就可以为消费者查询可消费的消息。这也就是说， 如果有某个请求被挂起了，那么只要在唤醒该请求的时候，直接调用 PopMessageProcessor 请求处理器的 processRequest() 方法即可 ，这一点大家可以理解吧？如果这一点大家理解了，那我们很快就能意识到，执行 PopMessageProcessor 请求处理器的 processRequest() 方法需要两个方法参数，第一个是 ChannelHandlerContext 上下文对象，第二个是 RemotingCommand 请求对象， 那这是不是就意味着如果一个请求需要在 PopMessageProcessor 请求处理器的 processRequest() 方法的 thenApply() 回调方法中被挂起，那么我们只需要把 processRequest() 方法的两个参数保存起来即可 ？这样一来， 等到当前消费者要被唤醒的时候，直接找到该消费者的 ChannelHandlerContext 和 RemotingCommand 信息，然后再执行 PopMessageProcessor 请求处理器的 processRequest() 方法不就行了吗 ？大家可以品味品味这个逻辑，如果大家理解了这个逻辑，那接下来就让我为大家展示一下初步定义完毕的 PopLongPollingService 类，请看下面代码块。  
从上面代码块中可以看到，我给 PopLongPollingService 长轮询组件定义了两个方法， 一个是 polling() 方法，用来把请求挂起；另一个是 notifyMessageArriving() 方法，用来通知有消息到达，唤醒被挂起的请求继续消费消息 。当然，这两个方法我都还没有具体实现，因为还有很多细节内容需要分析，就比如说用来挂起请求的 polling() 方法吧，刚才我们已经分析了，要想把请求挂起，只需要把当前消费者用到的 ChannelHandlerContext 和 RemotingCommand 对象保存起来即可，等唤醒请求继续消费的时候直接把这两个对象拿出来使用。那现在问题就来了，保存当前消费者的 ChannelHandlerContext 和 RemotingCommand 对象时，应该怎么做呢？我首先想到的就是按照消费者消费的消息对消费者进行归类，比如说有多个消费者客户端都到 Broker 节点中消费某个主题下的消息，并且这些消费者客户端都是属于同一个消费组，那么我们就可以把同一个消费组、消费同一个主题、消费同一个队列的消费者归为一类， 凡是在同一个消费组在消费同一个主题、同一个队列消息时需要把消费者的请求挂起，那么就把这些被挂起的请求都缓存到一起 。 我们完全可以先定义一个 PopRequest 类，用这个类的对象来封装消费者的 ChannelHandlerContext 和 RemotingCommand 对象，然后再定义一个 Map 来缓存这些信息，Map 的 key 就是主题 + 消费组信息 + 队列 Id 合并成的字符串，Map 的 value 就是一个集合，该集合中专门存储封装了消费者的 ChannelHandlerContext 和 RemotingCommand 对象的 PopRequest 请求 。具体内容如下，请看下面代码块。  
这样一来，当某个主题下的某个队列中有了可消费的消息，那就可以直接根据消费者所属的消费组信息，要消费的消息的主题以及队列信息，从 pollingMap 中得到所有被挂起的消费者请求，然后依次唤醒它们消费即可，而唤醒消费的操作也很简单，那就是重新执行 PopMessageProcessor 请求处理器的 processRequest() 方法，我相信到这里长轮询请求功能的实现思路已经很清晰了，那接下来我就为大家展示一下相关代码，请看下面代码块。  
上面代码块的内容虽然很多，但是注释非常详细，之前也都分析过功能的实现思路，所以我就不再重复讲解了。好了，现在长轮询功能实现完毕了，那接下来就应该使用长轮询功能重构 PopMessageProcessor 请求处理器的 processRequest() 方法，把该方法的 thenApply() 回调方法补充完整，请看下面代码块。  
好了，现在 thenApply() 回调方法的内容也补充完毕了，到此为止本章的内容也就可以结束了。大家可以仔细阅读上面代码块的内容，或者结合我提供的第二十二版本代码反复阅读本篇文章，直到把文章中展示的内容都掌握了，第二十二版本代码中还有很多内容并没有在文章中展示，所以大家也可以再等一等，等后面两篇文章更新完毕了再阅读我提供的第二十二版本代码。当然，本章内容结束并不意味着我们实现的长轮询组件已经达到完美的程度，实际上光我自己就能从 PopLongPollingService 类中找到很多问题。  
  
比如说我们目前只实现了请求被挂起和请求被唤醒功能， 那当被挂起的请求超时了应该执行什么操作呢 ？这显然也是 PopLongPollingService 长轮询组件应该负责的工作，但我们并没有为其实现这个功能，这是第一个缺陷。第二个缺陷也很明显， 那就是在我们刚刚实现的 thenApply() 回调方法中可以看到，只有当某个消费者客户端到 Broker 节点内部获取消息时，发现队列中有可消费的消息了才会唤醒被挂起的请求继续消费消息，那假如 Broker 节点内部主题被生产者投递了消息，但是并没有消费者来消费消息，那之前因为没有消息而被挂起的请求就没办法被唤醒了吗 ？这两个缺陷是不是很明显？至于第三个问题，这就称不上什么程序缺陷了，而是真正的问题， 我相信大家肯定也注意到了，从消息存储引擎返回的消息信息中不仅封装了完整的消息，还会把消息在消费队列中的偏移量，以及获取这批消息时是从消费队列哪里开始获取的，也就是消费队列本次查询消息的起始偏移量也会被返回给外层方法，那返回这些信息究竟有什么用呢 ？我能告诉大家的是这些信息非常重要，至于它们究竟怎么发挥作用，就留到下一章为大家讲解吧。朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/zwqvn633uoxa979i*  
*All content belongs to its respective owners and creators.*