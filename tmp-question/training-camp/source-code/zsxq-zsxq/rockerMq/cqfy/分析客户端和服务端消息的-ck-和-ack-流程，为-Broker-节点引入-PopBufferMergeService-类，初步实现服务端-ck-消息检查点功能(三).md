  
重构 PopBufferMergeService，实现临时消费偏移量功能  
  
上一章我们已经实现了 PopBufferMergeService 对象，随着这个对象的引入，缓存 PopCheckPoint 消息检查点对象的功能也实现了，讲到最后，只遗留了一个功能没有实现，那就是更新消费队列临时消费偏移量的功能。之前我们是这样分析的： 每当一批消息被 Pop 出 Broker 节点后，就立刻把这批消息的偏移量更新到临时的消费队列最新偏移量中，这样一来才能保证刚才消费的这批消息对其它消费者客户端不可见，其他消费者客户端只需要根据临时最新消费偏移量获取消息即可。如果有消息被消费失败了，那就把这部分消息投递到失败重试队列中供消费者客户端重新消费。只有当一批消息都被成功消费了，这个时候才会把真正的消费偏移量更新到管理器中 。这个逻辑我已经为大家分析得十分清楚了，当然，只有消费者客户端执行普通消费而非顺序消费的情况下，以上逻辑才能成立。大家要记住这一点，因为后面我们还要一起实现消费者客户端顺序消费消息功能。好了，还是让我们言归正传吧，还是先看看上一章遗留的功能该怎么实现。  
  
其实在之前我们已经讨论过了，要想实现更新临时消费偏移量的功能，就应该把 PopBufferMergeService 当作突破口， 因为所有的 PopCheckPoint 对象都缓存在 PopBufferMergeService 中了，而消息偏移量等信息都封装在 PopCheckPoint 对象中，所以要想获得消费队列临时的最新消费偏移量，肯定要从 PopBufferMergeService 中获取 。那接下来就让我们再审视一下 PopBufferMergeService 类的内容，看看所谓的突破口究竟隐藏在这个类的那个地方，我把上一章实现的 PopBufferMergeService 类搬运过来了，请看下面代码块。  
package org.apache.rocketmq.broker.processor;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/10/24

\* @方法描述：ck和ack消息存储缓冲区组件

\*/

public class PopBufferMergeService {  
  
private static final Logger POP\_LOGGER \= LoggerFactory.getLogger (LoggerName.ROCKETMQ\_POP\_LOGGER\_NAME);  
//存储待确认的PopCheckPointWrapper消息的缓冲区，key是这批消息的唯一表示

//注意，每一PopCheckPointWrapper对象封装的都是一个PopCheckPoint对象，而每一个PopCheckPoint对象对应的都是一批待ack的消息，因为消息是一批一批从队列中弹出来的

//key=point.getTopic() + point.getCId() + point.getQueueId() + point.getStartOffset() + point.getPopTime() + point.getBrokerName();

ConcurrentHashMap < String /\*mergeKey\*/,PopCheckPointWrapper > buffer \= new ConcurrentHashMap <> (1024 \* 16);  
//服务是否可用的标志，默认是可用的

private volatile boolean serving \= true;  
//统计待处理的ck消息的数量

private AtomicInteger counter \= new AtomicInteger (0);  
private final BrokerController brokerController;  
private final PopMessageProcessor popMessageProcessor;  
//队列锁管理器

private final PopMessageProcessor.QueueLockManager queueLockManager;  
//当前节点是否为主节点的标志

private volatile boolean master \= false;  
好了，我已经看完了上面的代码块，可以说没有一点发现了秘密的惊喜，失望倒是汹涌而来。上面代码块中的内容真的太少了，也没什么可看的，就是实现了缓存 PopCheckPoint 对象到 buffer 成员变量中的功能。而实现这个功能的目的也很简单： 一个 PopCheckPoint 对象可以匹配多个 ack 响应，当其内部的位图成员变量的所有 bit 都更新为 1，那也就意味着这个 PopCheckPoint 对象对应的一批消息就都被成功响应了。而之所以缓存 PopCheckPoint 对象的原因很简单，就是希望在接收到 ack 响应后，Broker 节点可以根据 ack 响应中的信息从 PopBufferMergeService 的 buffer 成员变量中匹配到对应的 PopCheckPoint 对象，然后再更新 PopCheckPoint 对象中对应的位图标志 。我相信就算我不跟大家分析这里逻辑，大家自己也能想清楚，毕竟这都是很简单的内容，在之前实现的诸多框架中，这样的内容已经被大家看过无数次了。  
  
好吧，目前 PopBufferMergeService 对象最有用的价值又一次被我们分析完了，看来除此之外，我们并没有得到什么更有价值的信息，那接下来该怎么办呢？这时候我忽然想到，其实我忽略了 PopBufferMergeService 类中的一些内容，刚才我们只顾着看 buffer 成员变量缓存 PopCheckPoint 对象了，忘记了关注封装 PopCheckPoint 对象的 PopCheckPointWrapper 对象。从上面代码块中大家也可以看出来，我对这个 PopCheckPointWrapper 内部类看都没看一眼，直接就把它的内容省略了。但现在我们并没有从剩余的内容中找到突破口，那只好再回过头看看这个 PopCheckPointWrapper 内部类中有什么内容吧，我把这个内部类也搬运到这里了，请看下面代码块。  
我相信在阅读完上面代码块之后，大多数朋友心里都没什么感觉，也谈不上什么失望，因为本来就不对其抱有希望，但我可以很明确地告诉大家，所谓的突破口就隐藏在上面的代码块中。在上面代码块中，我给大家展示的 PopCheckPointWrapper 内部类的内容很少，但是这并不意味着它的内容易于理解，可以看到，这个内部类的很多成员变量上都没添加注释。这倒不是我懒惰，而是有的成员变量必须结合具体的使用情况来讲解，没有具体的使用情况，只是一昧强行添加注释只会适得其反，令大家在代码的迷宫中陷得更深。  
  
好了，让我们言归正传吧，我刚才跟大家说 PopCheckPointWrapper 内部类中隐藏着实现新功能的突破口，这话确实不假。请大家把目光集中到 PopCheckPointWrapper 类的 nextBeginOffset 成员变量上，我在代码块中已经为这个成员变量添加了详细注释，想必大家也都清楚了这个成员变量的具体作用。但是大家未必知道这个成员变量是怎么被赋值的，其实整个赋值的流程我在之前的代码中已经为大家实现了。如果大家对前面的内容还有印象，应该会记得从 DefaultMessageStore 消息存储引擎的 getMessage() 方法中返回查询到的一批消息时，除了返回消息本身的内容，还会返回这批消息的额外信息，就像下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到，在查询到消息之后，这批消息本身的内容和额外信息都会被封装到 GetMessageResult 对象中返回给外层方法， 其中有一个非常关键的信息，也就是 nextBeginOffset 代表的在这批消息之后，下一批要被消费的消息在当前消费队列中的起始偏移量也会返回给外层方法 。这一点大家一定要记住！而外层方法则是 PopMessageProcessor 请求处理器的 popMsgFromQueue() 方法。好了，这就意味着这个 nextBeginOffset 被 PopMessageProcessor 请求处理器的 popMsgFromQueue() 方法得到了，那这个信息会被怎么处理呢？也许大家已经回忆起来了，如果消费者确实从消息存储引擎获取了一批消息，那么在 PopMessageProcessor 请求处理器的 popMsgFromQueue() 方法中就会执行 appendCheckPoint() 方法，为这批消息创建 PopCheckPoint 对象，并且把这个 PopCheckPoint 对象缓存到 PopBufferMergeService 对象中。好了，现在最关键的地方到了， 在程序执行 appendCheckPoint() 方法的过程中，就会把 GetMessageResult 对象封装的 nextBeginOffset 信息封装到 PopCheckPointWrapper 对象中 ，就像下面代码块展示的这样，请看下面代码块。  
上面代码块看完之后，我相信大家就都彻底清楚了 PopCheckPointWrapper 内部类的 nextBeginOffset 成员变量的具体含义，那知道了具体含义之后，要怎么使用这个成员变量呢？毕竟我已经跟大家说了，这个内部类的 nextBeginOffset 成员变量就是实现新功能的突破口。很好，终于步入正题了，现在请大家思考这样一个场景，有三个 Pop 消费者客户端到 Id 为 1 的消费队列中获取消息，第一个消费者客户端先从消费队列中获取了 32 条消息，消费的起始偏移量为 20 字节，整批消息为 640 字节，那么封装了这一批消息的 PopCheckPointWrapper 对象的 nextBeginOffset 就应该被赋值为 660 字节，这个大家可以理解吧？好了，接下来第二个消费者客户端到同一个队列中继续消费了，那显然应该从 660 字节开始消费，再消费 640 字节，那么封装了第二个消费者客户端获取的这批消息的 PopCheckPointWrapper 对象，它的 nextBeginOffset 就应该被赋值为 660 + 640，也就是 1300 字节，这个大家也能理解吧？很好，紧接着第三个消费者客户端过来消费了，从 1300 字节开始消费，也消费了 640 字节，那封装了这一批消息的 PopCheckPointWrapper 对象的 nextBeginOffset 成员变量就应该被赋值为 1300 + 640，也就是 1940，这一点应该也可以理解吧？如果这个时候，有第四个消费者客户端要来同一个队列中消费消息，那么它怎么知道最新的消费偏移量呢？显然， 第四个消费者客户端是不是只需要找到与第三个客户端对应的 PopCheckPointWrapper 对象，然后得到该对象的 nextBeginOffset 成员变量的值，就可以从目标队列最新的消费偏移量开始消费消息了 ，我这么说大家应该都赞同吧？  
  
如果是这样的话，那我忽然产生了一个想法： 那就是我不妨直接定义一个队列，这个队列只存放 PopCheckPointWrapper 对象，并且必须是从同一个目标队列中获取消息后产生的 PopCheckPointWrapper 对象，而且先被消费的消息，它们对应的 PopCheckPointWrapper 对象会放在队列前面，越晚被消费的消息，它们对应的 PopCheckPointWrapper 对象会被放在队列尾部。那假如有一个消费者客户端要到目标队列中消费最新的消息，它只需要从这个队列中获取队列最后一个 PopCheckPointWrapper 对象，得到该对象的 nextBeginOffset 成员变量，这个时候它就得到了目标队列最新的消费偏移量，然后就可以直接消费了 。大家可以品味品味这个逻辑，那我现在就可以很明确地告诉大家，消费队列最新临时消费偏移量的功能已经被我们实现了， 所谓最新的临时偏移量，就从我们刚才要定义的队列的最后一个 PopCheckPointWrapper 对象中获取即可 。当然，我们肯定要先把这个队列定义出来，这样我们刚才分析的一切才更有说服力。  
  
那这个队列该怎么定义呢？很简单，就用最普通的队列就行，JDK 自带的就可以，源码中使用的是 LinkedBlockingDeque 对象，所以我们也使用这个 LinkedBlockingDeque 对象即可。但是，有一点我要解释清楚， 我们不可能只使用一个队列，最终使用的肯定还是一个 Map 数据结构来存储同一个消费队列产生的 PopCheckPointWrapper 对象 ，原因很简单，消费者客户端肯定会从不同的消费队列中消费消息，如果一个消费队列对应一个我们要定义的存放 PopCheckPointWrapper 对象的队列，那显然会用到多个存放 PopCheckPointWrapper 对象的队列， 那我们就可以定义一个 Map，Map 的 key 是 topic@cid@queueId 字符串信息，也就是消费者组 + 主题信息 + 队列 Id，这样一来正好可以确认消费者组消费的主题下的唯一一个消费队列，而 value 就是我们自己使用的队列，也就是 LinkedBlockingDeque 对象 。这样分析下来，这个 Map 就可以定义成下面这样，请看下面代码块。  
这么定义没什么问题吧？我们确实可以这么做，但在 RocketMq 源码中并不是这么做的，在源码中对 LinkedBlockingDeque 对象做了一层封装，引入了一个新的类，那就是 QueueWithTime 类，这个类也是 PopBufferMergeService 的内部类，而刚才定义的 commitOffsets 这个 Map 也是 PopBufferMergeService 的成员变量，我已经仿照源码对 PopBufferMergeService 类进行了简单重构，请看下面代码块。  
好了，现在新的内部类 QueueWithTime 也引入了，新的 commitOffsets 成员变量也定义了，那怎么把创建好的 PopCheckPointWrapper 对象存放到 commitOffsets 成员变量中呢？这个时候就要对 PopBufferMergeService 类的 addCk() 方法进行一翻重构了，这次重构我们不仅会实现 commitOffsets 存储 PopCheckPointWrapper 对象功能，还会根据之前的分析，把从 PopBufferMergeService 对象中获取指定消费队列的最新临时消费偏移量的方法也实现了，请看下面代码块。  
上面代码块中的内容虽然有点多，但是注释很详细，而且逻辑也很简单，我就不再重复讲解了。大家可以重点关注上面的 getLatestOffset() 方法，该方法就是从 PopBufferMergeService 对象中获得指定消费队列最新临时消费偏移量的方法。到此为止，大家应该清楚了， 只要消费者客户端从 Broker 节点获取了一批消息，都会在 PopBufferMergeService 对象中立刻存储当前消费队列最新的临时消费偏移量，供下一个消费者客户端使用 ，这一点应该说是毫无争议了。但是在我们目前实现的程序中，消费者客户端在消费消息之前都会先从消费偏移量管理器中查询一下目标队列的最新消费偏移量，也就是下面代码块展示的这样，请看下面代码块。  
从上面代码块中可以看到，消费者客户端在消费消息之前，都会到 ConsumerOffsetManager 消费偏移量管理器中得到目标队列最新的消费偏移量，但我们已经实现了消费队列临时偏移量功能，显然应该让消费者客户端到 PopBufferMergeService 对象中获取目标队列的最新临时消费偏移量，这一点大家可以理解吧？当然 ，这样一来消费者客户端就会得到同一目标队列的两个消费偏移量，那最后肯定是要对比一下，看看哪个消费偏移量大，哪个大就用哪个 。就像下面代码块展示的这样，请看下面代码块。  
好了朋友们，到此为止本章内容就结束了，大家也就可以去阅读第二十二版本代码的所有内容了。当然，在第二十二版本代码中，PopBufferMergeService 类的大部分内容我都没在文章中展示，大家也不必关注，等后面我们实现二十三版本代码的时候，大家再看就行。还有一点我想说的是，虽然我们已经在第二十二版本代码中耗费了好几个篇章了，但是这个版本代码的一些内容还是没有被我展示在文章中，就比如说消息的额外信息是怎么返回给消费者客户端的，消息回执句柄又是什么，这些内容同样重要，它们都出现在 Proxy 节点返回响应给消费者客户端的过程中。大家一定要把这部分内容好好看看，我就不在文章中讲解了，代码中注释非常详细，一定要把这些内容都看了，然后再阅读下一篇文章。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/gvl46ghox23ekcy4*  
*All content belongs to its respective owners and creators.*