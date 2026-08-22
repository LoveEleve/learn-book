上一章结尾我给大家遗留了两个问题，第一个问题就是已经被 ack 完成的消息的消费偏移量应该怎么更新，也就是说，应该把消费队列最新的消费偏移量更新到消费偏移量管理器中，但这个功能我们并没有实现；第二个问题就更明显了，那就是 PopBufferMergeService 对象肯定不能无限缓存为每一批消息创建的 PopCheckPointWrapper 对象，否则就会出现系统资源耗尽，内存溢出的问题，那这个问题该怎么解决呢？  
  
我不知道大家是否真的思考过以上两个问题，也许大家没有仔细思考过，也许有朋友已经根据丰富的经验把第二十三版本代码都阅读完了，提前知道了这两个问题的解决方法。不管怎么样吧，我还是要解释一下，我总是在文章结尾提出新的问题，然后让大家干等两天时间，等我更新了对应的文章才知道解决问题的方法，我这么做并不是处于一种恶趣味，实际上我根本也没有这种故意吊着大家胃口的恶趣味；我之所以总是这么做，真的是希望大家可以自己思考思考，自己寻找解决问题的突破口，因为我们过去已经实现了太多框架，在实现各种框架的过程中掌握的各种技术足以帮助大家解决以上两个问题，突破口真的是显而易见的。就比如说吧，上面的第二个问题吧， 我们已经意识到了 PopBufferMergeService 中不能存储无限的 PopCheckPointWrapper 对象，那这就意味着其内部存储的 PopCheckPointWrapper 对象肯定需要被及时清理，也就是被垃圾回收 ，对吧？而当我们意识到这一点之后，那接下来我们就会思考 PopCheckPointWrapper 对象什么时候要被垃圾回收，这个问题的答案很简单， 肯定是当这个 PopCheckPointWrapper 对象对应的这批消息全部被 ack 了，这个时候，这个 PopCheckPointWrapper 对象就可以被垃圾回收了 ，这个逻辑大家可以理解吧？  
  
而所谓的垃圾回收也很简单，根据我们上一章重构的 PopBufferMergeService 类，我们已经知道了，Broker 节点为每一批被 Pop 出的消息创建的 PopCheckPointWrapper 对象都会存储到 PopBufferMergeService 的 buffer 成员变量和 commitOffsets 成员变量中，也就是下面代码块展示的这样，请看下面代码块。  
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
//存放待提交的消费偏移量的map，key是"topic@cid@queueId"格式的字符串，value是一个QueueWithTime对象，但QueueWithTime对象内部有一个队列，这个队列其实就是存放了

//每一批消息待提交的偏移量信息，这里面存储的PopCheckPointWrapper对象和buffer中存储的是同一个对象

ConcurrentHashMap < String /\*topic@cid@queueId\*/,QueueWithTime < PopCheckPointWrapper >> commitOffsets \= new ConcurrentHashMap <> ();  
  
  
  
  
//省略该类其他内容  
}

在 Java 中，一个对象触发被垃圾回收的操作非常简单，只要这个对象不再被引用即可。既然是这样， 当我们想让一个 PopCheckPointWrapper 对象被垃圾自动回收时，只需要把这个 PopCheckPointWrapper 对象从 buffer 成员变量和 commitOffsets 成员变量中移除即可，这样它就不再被引用了，也就可以在恰当的时机被垃圾回收了 。很好，现在我们已经明确了从内存中清理 PopCheckPointWrapper 对象的方式，也明确了只有这个 PopCheckPointWrapper 对象对应的消息全部被 ack 了才能被垃圾回收，而 PopCheckPointWrapper 对象对应的消息被 ack 是个不定期执行的操作，因为 Broker 节点肯定不知道消费者客户端什么时候会为消息回复 ack 响应；既然是这样， 那肯定需要在 Broker 节点内部启动一个新的线程，让这个线程在循环中定期到 PopBufferMergeService 中扫描哪些 PopCheckPointWrapper 对象已经被 ack 完毕了，如果扫描到哪些 PopBufferMergeService 对象被 ack 完毕了，那就可以从 buffer 和 commitOffsets 成员变量中移除这个 PopBufferMergeService 对象；当然，在移除之前还可以执行一个至关重要的操作，那就是把要移除的 PopBufferMergeService 对象对应的消息偏移量更新到消费偏移量管理器中 。这个逻辑大家肯定能理解吧？别忘了，在前面的章节中我们实现了长轮询服务组件，那个组件的实现逻辑和刚才分析的逻辑几乎一摸一样，都是启动线程扫描内部的数据，然后处理数据。这一点大家应该都能意识到。  
  
当然，既然提到了 PopLongPollingService 长轮询服务组件，那么大家肯定已经想起来了，这个组件本身就是一个线程，因为 PopLongPollingService 类继承了 ServiceThread 类；那回到我们刚才分析的内容，显然也就不必再 Broker 节点内部另起线程了， 直接让 PopBufferMergeService 类也继承 ServiceThread 类好了，这样 PopBufferMergeService 自身就成为了一个线程 ，接下来只要给这个线程定义一个 run() 方法，把该线程要执行的操作定义在 run() 方法中，这样不就行了吗？当然，还是和 PopLongPollingService 组件的启动方式一样，只要在 Broker 节点启动的过程中也启动这个 PopBufferMergeService 线程，那么该线程就可以开始工作，在循环中定期扫描数据，如果没有要处理的数据，那线程就可以休息一会。这些逻辑就不用再详细展开了吧？都是重复了无数次的内容了。好了，分析完这些内容就万事大吉了，只差把最后的代码编写出来，那接下来就请大家看看我已经重构完毕的 PopBufferMergeService 类吧，请看下面代码块。  
从上面代码块中可以看到，我对 PopBufferMergeService 类重构的内容比较简单，仅仅是把该类变成了一个线程，继承了 ServiceThread 类，并且还为该类新添加了一个 run() 方法，这样一来，当 PopBufferMergeService 这个线程对象启动之后，就可以执行 run() 方法中的操作了。而 run() 方法中的内容也很简单，只不过是定期扫描 PopBufferMergeService 内部数据的操作，逻辑都很简单，我就不再重复解释了。当然，真正扫描 PopBufferMergeService 内部数据的方法，也就是 scan() 方法我并没有真正为大家实现。原因也很简单，显然这个 scan() 方法才是最核心的方法，既然是核心方法当然应该先一起讨论讨论，不能实现得特别草率。那现在就请大家思考一下，在这个 scan() 方法中，程序应该执行什么操作呢？  
  
按照我们之前的讨论，显然在 scan() 方法中要查看 buffer 成员变量中的所有 PopCheckPointWrapper 对象，一旦发现了有哪个 PopCheckPointWrapper 对象的消息 ack 标志位都被更新成功了，那接下来就可以把这个 PopCheckPointWrapper 对象从 buffer 成员变量中移除了；当然，光这么做还不能够让这个 PopCheckPointWrapper 对象被垃圾回收，还需要从 commitOffsets 成员变量中移除这个对象才行，而在移除这个对象的过程中，显然就应该把这个对象封装的消息偏移量信息更新到消费偏移量管理器中，这个逻辑大家应该很清楚了吧？如果大家理解了这个逻辑，那在阅读我提供的第二十三版本代码和源码的过程中就轻松多了，因为在源码中就是这么做的： 在源码的 scan() 方法中循环遍历了 buffer 成员变量中的所有 PopCheckPointWrapper 对象，如果有哪个对象被 ack 完毕了，就把该对象从 buffer 成员变量中移除；该循环结束之后，又对 commitOffsets 成员变量中的数据执行了循环遍历，发现哪个 PopCheckPointWrapper 对象被 ack 完毕了，就把该对象封装的消费偏移量提交到消费偏移量管理器中，然后再从 commitOffsets 成员变量中移除该对象 。具体逻辑请看下面代码块。  
上面代码块的内容虽然有点多，但是都是之前分析过的内容，代码注释也非常详细，所以我就不再重复解释了。总之，到此为止，我们解决了之前遗留的两个问题。消费队列的最新消费偏移量也可以正是更新到消费偏移量管理器中了，而已经被 ack 完毕的 PopCheckPointWrapper 对象也可以从内存中清楚了，不必再担心内存溢出的问题了。这些内容大家都可以理解吧？  
  
很好，如果以上问题大家都理解了，那接下来我就要再给大家上点难度了，因为有一个问题已经困扰我很久了，现在终于能把这个问题分享给大家，也算是让我长舒了口气。请大家结合刚才实现的功能，再思考一下：假如现在就是有这么一种极端情况，消费者客户端已经从 Broker 节点获取了很多批消息，这也就意味着有很多个 PopCheckPointWrapper 对象缓存在内存中了，这些对象都等着被消费者客户端 ack 呢。但是非常奇怪，不知道是什么原因，消费者客户端在为消息 ack 的时候，总是不能干脆利落全部回复了。这就导致缓存在 Broker 节点内存的成千上万个 PopCheckPointWrapper 对象，每一个对象都有一两条消息没有被 ack。那这就会造成一种很可怕的情况， 那就是这些尚未被 ack 完全的 PopCheckPointWrapper 对象，都无法从内存中清除，程序运行时间长了，内存中堆积的 PopCheckPointWrapper 对象越来越多，肯定会出现内存溢出的情况 。这个问题大家肯定也能意识到吧？  
  
虽然我们之前实现了清除内存中的 PopCheckPointWrapper 对象的功能，但是清除的前提非常明确，那就是这个 PopCheckPointWrapper 对象封装的消息必须全部被 ack 了，在这种情况下，这个 PopCheckPointWrapper 对象才能从内存中移除。那按照这种逻辑，假如一个 PopCheckPointWrapper 对象始终无法被完全 ack，难道这个 PopCheckPointWrapper 对象就要常驻内存了吗？显然不应该这么做，否则迟早会内存溢出。  
  
想到这里， 我忽然意识到从一开始我们就应该给缓存在内存中的 PopCheckPointWrapper 对象设置一个时间限制，只要 PopCheckPointWrapper 对象在内存中缓存的时间超过了这个限制，并且该对象并没有被完全 ack，我们就可以称这个对象过期了，那就可以强制把这个对象从内存中清除 ，这样一来，肯定就不会发生内存溢出的情况了。可这样一来就回到老问题了，如果一个 PopCheckPointWrapper 对象没有完全被 ack 就从内存中清除了，那么这个对象对应的消费偏移量该怎么提交呢？这么一想，也确实令人头疼，似乎更本不能两全。好了，这个时候我就不卖什么关子了，实际上这么做完全没问题，别忘了 RocketMq 本身是一个消息队列框架，各种各样的消息都可以存储到本地的 CommitLog 文件和消费队列文件中。既然是这样， 那么我们就不妨把所有过期的 PopCheckPointWrapper 对象都存储到本地，如果消费者客户端为某条消息回复了 ack 响应，当 Broker 节点接收到这条消息的 ack 响应后，并不能从内存中，也就是从 PopBufferMergeService 中匹配到唯一一条消息，那这个时候就可以到本地存储的 PopCheckPointWrapper 对象中匹配唯一一条消息，然后执行 ack 操作 。大家可以仔细品味品味这个逻辑。  
  
当然，我也能想到大家肯定还是不明白我刚才讲解的内容，接下来让我按照源码的设计思路为大家详细解释一下，实际上是这样的： 在 RocketMq 源码中，Broker 节点内部有一个系统内置的主题，主题名称就是 "rmq\_sys\_REVIVE\_LOG\_" + BrokerClusterName()当前 Broker 节点所在集群的名称)，我喜欢把这个主题称为预重试队列主题，并且也确实给 PopCheckPointWrapper 对象定义了在内存中的缓存时限 。很好，有了这个主题就好办了。就像我刚才为大家分析的那样，在程序运行期间，总会有各种各样的原因，可能导致一个 PopCheckPointWrapper 对象迟迟没有被完全 ack，当这个 PopCheckPointWrapper 对象在内存中缓存超时后，这个 PopCheckPointWrapper 对象持有的 PopCheckPoint 对象就会被存储到预重试主题下，也就是存储到本地的 CommitLog 文件中，并且也会构建好对应的消费队列；在这之后， Broker 节点还会执行一个重要操作，那就是把已经持久化到本地的 PopCheckPoint 对象封装的消费偏移量正式提交到消费偏移量管理器中 。看到这里大家应该能意识到了： 如果是在内存中已经过期了的 PopCheckPointWrapper 对象，就算它没有被完全 ack，只要它持有的 PopCheckPoint 对象被持久化到预重试主题下了，那么这个对象封装的消费偏移量就可以被正是更新到消费偏移量管理器中 。这样一来，也就不会出现同一批消费被不同客户端重复消费的情况了。  
  
看到这里大家肯定会有疑问，就算是这样有什么用呢？PopCheckPointWrapper 过期了，其持有的 PopCheckPoint 被持久化到预重试主题下了，但是消息并没有被消费成功啊，如果消费成功了肯定就全被 ack 了，那样的话 PopCheckPointWrapper 对象就可以光明正大的从内存中清除，而不是持久化到本地了。我知道大家心里肯定有这样的疑问，所以接下来请听我继续解释： 当有很多 PopCheckPoint 对象被持久化到预重试队列之后，Broker 接收到新的消息 ack 响应了，并没有在内存中匹配到唯一一条消息，那么就会到预重试队列主题下的消费队列中匹配对应的消息，如果匹配成功，就更新消息的标志位，直到所有消息都被 ack 完毕 。这个逻辑理解起来并不困难吧。  
  
那看到这里大家肯定就会有新的问题了，虽然把过期未 ack 的 PopCheckPoint 对象持久化到本地，那如果仍然有消息一直没有 ack 呢？这个时候怎么办呢？大家可要梳理清楚了，持久化到本地，或者说写入到 CommitLog 文件和消费队列本地文件中的数据，可是会被定期删除的。如果消息一直没有 ack，而对应的 PopCheckPoint 对象也被过期删除了，而此时这批消息的消费偏移量已经被更新到消费偏移量管理器中了，那么消费失败的消息不就没有机会补救了吗？按照常规思路，被消费失败的消息显然应该被重新消费，也就是失败重试。大家如果能想到这一点，那接下来的内容对大家来说就没什么难度了： 想必大家还记得每一批被 Pop 出的消息都有一个消息不可见时间吧，而消息被 Pop 出的时间 + 消息不可见时间 = 消息重新被投递的时间 ，这个知识点大家还记得吧？很简单，只要持久化到本地的消息肯定会老老实实在本地呆着，但随着程序运行， 一旦系统时间超过了消息的重新投递时间，那么只要哪些消息没有被 ack，程序就会从 CommitLog 文件中查询到这条消息的真正内容，然后把这条消息投递到真正的失败重试队列中，而真正的失败重试队列的主题就是 "%RETRY%" + 消费者所在消费组信息 + "+"(没错，就是加上这个符号) + 消息所属主题。一旦消息被投递到真正的重试主题之后，就可以被消费者客户端重新消费了 。注意，这个时候，真正的重试队列就登场了，大家一定要把重试队列和预重试队列区分开，把二者的主题区分开，预重试队列存储的都是未被全部 ack 的 PopCheckPoint 对象，而真正的重试队列存储的是消费失败的消息本身，大家一定要梳理清楚这些内容。当然，这个内容理解起来应该也不算难？反正都是文字叙述，我相信大家都能理解这些简单的设计原理。  
  
很好，功能的设计原理已经讲解完毕了，接下来就应该对一些细节问题进行深入探讨，比如说把未被全部 ack 的 PopCheckPoint 对象持久化到本地，那应该怎么持久化呢？这个非常简单，就像 Broker 节点存储生产者消息那样就行，直接创建 MessageExtBrokerInner 对象即可， 每一个 MessageExtBrokerInner 对象都封装一个 PopCheckPoint 对象，然后把 MessageExtBrokerInner 对象交给消息存储引擎存储即可 。这是很久之前实现的功能，大家应该已经很熟悉了。当然， MessageExtBrokerInner 除了封装 PopCheckPoint 对象，肯定还要封装消息所属主题信息，以及消费队列 Id，注意，这个时候创建的 MessageExtBrokerInner 对象要被存放到预重试队列中，那么该对象封装的消息主题肯定是预重试主题，队列 Id 肯定也是预重试队列 Id ，那这两个信息从何而来呢？我不说大家肯定已经回忆起来了，这两个信息在之前的代码中大家已经见过很多次了，当时我没有为大家解释，现在倒是可以完全展示给大家了。接下来就请大家先看一看我重构之后的 PopBufferMergeService 类以及 PopMessageProcessor 类，请看下面代码块。  
以上代码块的内容非常多，好在注释详细，我就不再一一展开讲解了，阅读完上面的代码块之后，大家也就清楚了过期的并且未被全部 ack 的 PopCheckPoint 对象是怎么持久化到消息预重试队列中，也清楚了预重试主题以及预重试消费队列的队列 Id 是怎么来的。在这个功能中，大家的所有疑问应该烟消云散了。  
  
当然，这并不意味着我们把过期消息 ack 功能和消费失败重试功能实现完毕了，按照我们之前说的，过期的未被全部 ack 的 PopCheckPoint 对象都存放到预重试消费队列中了，而当 Broker 节点接收到新的消息 ack 响应后，在内存中匹配不到唯一一条消息，就会到预重试消费队列中匹配，那这个过程又是怎样的呢？ 别忘了消息的 ack 响应可是由 AckMessageProcessor 处理器处理的，而要从本地消费队列中匹配消息，这该怎么办呢 ？  
  
假设我们已经实现了从预重试消费队列中 ack 消息的功能，接收到 ack 响应之后，匹配成功则没什么要处理的，假如没有匹配成功呢？也就是说存储在预重试队列中的消息也迟迟没有 ack，那这个时候该怎么办呢？按照我们之前的分析，就应该把没有 ack 的消息重新从 CommitLog 文件中查询出来，然后投递到真正的重试队列中，那这个过程又是怎么样的呢？谁来投递呢？什么时候投递呢？ 怎么从 CommitLog 文件中查询消息本身的内容呢？由谁来查询呢 ？  
  
查询到了之后就要执行重新投递的操作，也就是要把消息投递到真正的重试队列中，这个操作很简单，只要直到真正的重试队列主题和队列 Id 即可，在这之后这些消息就可以被重新消费，一旦被消费者客户端重新消费，那就意味着失败重试功能实现完毕了，那现在最终的问题来了， 消费者客户端怎么去真正的重试队列中消费消息呢 ？  
  
你看，在本章结尾我又为大家提出了三个问题，请大家不要怪我，这可能就是我的一种习惯吧。当然，我很快就会更新下一篇文章，等下一篇文章更新完毕之后，大家就可以阅读第二十三版本代码的全部内容了。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/gs1i00htn8nbfhed*  
*All content belongs to its respective owners and creators.*