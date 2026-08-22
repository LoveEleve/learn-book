阅读这篇文章之前，可以去重新下载一下配套代码，我修正了代码中的一些内容。  
为 Broker 引入 TopicConfigManager 配置信息管理器  
  
上一章我们已经实现了 Broker 模块接收 Admin 模块发送过来的创建主题的请求功能，Broker 模块可以在自己的 AdminBrokerProcessor 请求处理器中解析 Admin 模块发送过来的创建主题的请求，从请求中得到对应的主题信息，功能实现到这里，上一章的内容就结束了。我也跟大家说了，Broker 节点创建主题的功能还不完善，所以这一章就让我们把这个功能重构完整。如果要实现的仅仅是 Broker 节点创建主题的功能，其实这个功能已经实现得差不多了， Broker 节点已经可以在 AdminBrokerProcessor 请求处理器中得到 Admin 模块发送过来的主题信息，Broker 节点只要把这个主题信息保存下来，那 Broker 节点也就拥有了这个主题信息，这不就创建主题成功了吗 ？ 比如说我们为 Broker 模块定义一个新的类，就叫做 TopicConfigManager，意思就是主题配置信息管理器 ，这个主题配置信息管理器中保存着 Broker 模块内部所有的主题信息。从上一章我们可以知道，主题信息都封装在一个 TopicConfig 对象中，在同一个 Broker 节点中，每一个主题的名称又都是唯一的， 那我们完全可以在 TopicConfigManager 类中定义一个 Map 成员变量，Map 的 key 就是主题的名称，value 就是封装了该主题所有配置信息的 TopicConfig 对象 。也就是说，这个 TopicConfigManager 类可以先定义成下面这样，请看下面代码块。  
package org.apache.rocketmq.broker.topic;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/17

\* @方法描述：主题配置信息管理器，用于管理主题配置信息，包括主题的读写队列数量、权限、扩展属性等信息。

\*/

public class TopicConfigManager extends ConfigManager {  
protected static final Logger log \= LoggerFactory.getLogger (LoggerName.BROKER\_LOGGER\_NAME);

//存储主题和主题配置信息的Map，这个map中的主题和对应的配置信息都会注册到Namesrv中

protected ConcurrentMap < String,TopicConfig > topicConfigTable \= new ConcurrentHashMap <> (1024);  
//数据版本，这个数据版本的作用非常重要，每当主题配置信息发生变化时，都会更新数据版本，用于标识主题配置信息的变化

private DataVersion dataVersion \= new DataVersion ();  
protected transient BrokerController brokerController;  
  
//构造方法

public TopicConfigManager () {

}  
//构造方法

public TopicConfigManager (BrokerController brokerController) {

this.brokerController \= brokerController;

}  
  
//把主题信息存放到Map成员变量的方法

protected TopicConfig putTopicConfig (TopicConfig topicConfig) {

上面代码块的内容非常简单，我就不再详细解释了，唯一需要关注的就是上面代码块中的 dataVersion 成员变量，在代码注释中我跟大家说这个 dataVersion 成员变量就是数据版本信息。现在大家肯定还理解不了这是什么意思，接下来就让我来给大家解释一下。请大家想一想，在程序运行的过程中，Admin 控制台是不是可以一直向 Broker 集群中的所有主节点发送请求，这些请求有的可以改变 Broker 节点的配置信息，有的可以改变 Broker 节点的主题信息， 这也就意味着 Broker 节点的内部信息，尤其是主题信息在程序运行的过程中可能会动态变化 。可能某个 Broker 节点创建的主题信息会越来越多，也可能会越来越少，也可能某个指定的主题信息会不断更新，比如某个主题的读写队列数量可能会动态调整等等，也可能是这个主题的读写权限发生变化了。总之，这些都意味着 Broker 节点的主题信息发生了变化， 所以 Broker 节点需要定期把自己的主题信息注册到 Namesrv 节点，让 Namesrv 节点知道最新的路由信息 。  
  
而通过前面的章节我们已经知道了，Broker 节点确实会把自己的主题信息定期更新到 Namesrv 节点，不管是 Broker 组中的主节点还是从节点，都会把自己的配置信息注册到 Namesrv 节点上，这时候大家可能会有疑惑，为什么从节点也要把配置信息注册到 Namesrv 上呢？根据大家使用 RocketMq 的经验，大家肯定会知道 Broker 从节点的主题信息是从 Broker 主节点同步过来的，并且 Admin 模块在创建主题信息的时候，也只是对指定集群中的所有 Broker 主节点创建的，既然是这样的话，那只让 Broker 主节点定期刷新主题信息到 Namesrv 节点不就行了？为什么还要让从节点也定期刷新呢？  
  
请大家思考这样一种情况，现在有一个 Broker 集群，集群中有一个主节点两个从节点。程序启动之后，Admin 模块向 Broker 主节点创建了名为 Test 的主题，然后 Broker 的从节点从主节点把主题信息同步过去了，Broker 的主从节点会把自己的主题信息都注册到 Namesrv，并且主从节点的主题信息都是一致的。过了一会，Broker 主节点运行出现问题了，可能是发生了网络故障，Broker 主节点再把自己的主题信息注册给 Namesrv 时，这条信息一直没有被 Namesrv 接收到，于是运维人员就把 Broker 的 Id 最小的从节点提升为主节点，这个时候被提升为主节点的从节点的 Id 就成为了 0。在这之后，Admin 模块又更新了这个新的主节点的主题信息，然后新的主节点要把自己的主题信息注册到 Namesrv 上，就在 Namesrv 接收到新主节点的主题信息时，旧主节点的信息也被 Namesrv 接收到了，两个 Broker 节点的 Id 又一样，那这个时候 Namesrv 该保存哪个 Broker 节点的信息呢？这个时候 TopicConfigManager 对象的 dataVersion 数据版本对象就可以发挥作用了。  
  
实际上是这样的， 在 Broker 节点每次更新了自己的主题信息后，都会更新对应的数据版本信息，在 DataVersion 对象中有一个 long 整数成员变量，每次更新主题版本信息，这个 long 整数的值就会变大，通过对比 DataVersion 数据信息对象的这个 long 整数，就可以判断哪个主题信息是最新的 。这也就是说， 只要在 Broker 节点把主题信息注册到 Namesrv 的时候，把主题信息的 DataVersion 版本数据也发送过去，那 Namesrv 节点就可以根据每个主题对应的数据版本号判断，哪个信息是最新的，也就是要真正保存的了 。这就是 TopicConfigManager 类中 DataVersion 成员变量的作用。这个 DataVersion 类的内容我就不在文章中展示了，内容非常简单，大家可以直接去我提供的第七版本代码中查看，展示过多的类只会使文章篇幅更长，这其实是无用功。  
  
好了，现在 DataVersion 成员变量的作用介绍完毕了，但是我们并没有在 TopicConfigManager 类中真的更新它，实际上，这个 TopicConfigManager 类中连更新主题信息的方法都没有呢。按照常规思维来看， Broker 节点要想保存从 Admin 模块发送的主题信息，肯定是先看看自己内部有没有对应的主题，如果没有才保存，如果有的话就看看是否需要更新对应的主题信息 ，这个逻辑可以理解吧？但现在的 TopicConfigManager 类并没有这部分功能，所以接下来就让我们为它实现一下，我给这个 TopicConfigManager 类新定义了一个 updateTopicConfig() 方法，具体实现请看下面代码块。  
从上面代码块中可以看到， TopicConfigManager 类的 updateTopicConfig() 方法的内容非常简单，就是把 Admin 发送过来的主题信息存储到 topicConfigTable 成员变量中，如果 topicConfigTable 中已经存在对应的主题信息，那这个操作就会更新对应的主题信息；如果不存在就是新增主题信息，而这些操作执行完毕之后，还会执行更新主题信息版本的操作 ，这些内容看着并不难理解吧？当然，还有一点要补充的就是，在上面代码块的 updateTopicConfig() 方法的最后，大家可以看到执行了 this.persist(topicConfig.getTopicName(), topicConfig) 这样一行代码，我在代码注释中跟大家说这个 persist() 是 TopicConfigManager 继承的父类中的方法，并且这个方法会在每一次主题配置信息发生变化时，把 Broker 节点的配置信息持久化到本地。这个时候大家应该也注意到了 TopicConfigManager 继承的父类，也就是 ConfigManager 类了吧？这个 ConfigManager 类的内容就是我接下来要为大家展示的内容。  
  
在展示 ConfigManager 类的内容之前，我想再罗嗦几句，实际上给 Broker 节点的主题信息设置持久化功能是很有必要的，一个 Broker 节点总可能有因故重启的时候，如果主题都保存在内存中，重启之后不就丢失了吗？所以对这些信息执行持久化操作是很有必要的。当然，既然这些主题信息持久化了 ，这也就意味着 Broker 节点启动的时候肯定要从本地把这些主题信息加载到内存中，而加载本地主题信息到内存的方法也定义在 ConfigManager 类中了 ，接下来就请大家看看我定义完毕的 ConfigManager 类的内容，请看下面代码块。  
到此为止，我们就把和 TopicConfigManager 相关的功能都实现完毕了，大家也都清楚了 Broker 节点究竟是怎么存储主题的，其实就是把从 Admin 接收到的主题信息存储到 TopicConfigManager 主题管理器中。 而 Broker 节点是在 AdminBrokerProcessor 请求处理器中接收到主题信息的，这也就意味着我们要在 AdminBrokerProcessor 请求管理器中得到 TopicConfigManager 对象，然后把主题信息存储到里面 。但现在我们所做的只是定义了 TopicConfigManager 这个类，知道这个类的具体功能了，并没有真的创建这个 TopicConfigManager 主题配置信息管理器，也没有实现 Broker 节点启动的时候加载本地主题信息的功能，所以接下来我们要实现这些内容，也就是要重构 BrokerController 类。  
  
重构 BrokerController 类  
  
这次对 BrokerController 类的重构非常简单，无非就是在 BrokerController 启动的过程中先创建 TopicConfigManager 对象，然后再执行加载本地主题信息的操作，把这些信息加载到 TopicConfigManager 对象中。所以，这个 BrokerController 类可以重构成下面这样，请看下面代码块。  
好了，现在 BrokerController 类也重构完了，在 Broker 节点启动的过程中，先创建了 TopicConfigManager 对象，然后调用该对象的 load() 方法，把本地文件中的主题信息都加载到内存中了。这些功能实现起来太简单了，也没什么可说的了，接下来就该实现本章最重要的一个功能了，那就是 Broker 节点注册向 Namesrv 注册主题信息的功能，确切地说，应该是完善 Brokre 节点向 Namesrv 注册信息的功能。因为 Broker 在向 Namesrv 注册信息时，会把自己的配置信息和内部所有主题信息都发送给 Namesrv 节点。接下来我们就一起实现这个功能吧。  
  
重构 BrokerController 类的 registerBrokerAll() 方法  
  
在开始具体重构功能之前，我们可以先思考思考，Broker 节点需要什么时候把自己的信息注册到 Namesrv 节点中呢？我认为 Broker 有两个时机需要执行这个操作， 一个就是在定时任务中，定期把自己的信息中注册到 Namesrv 节点中，另一个就是在 Broker 节点的主题信息发生更新时，立刻把自己的信息注册到 Namesrv 节点中 ，这样才保证了 Namesrv 节点管理的路由信息的时效性。也就是说我们接下来要重构两个地方， 第一就是定时任务的 registerBrokerAll() 方法，该方法就会把 Broker 的配置信息和主题信息定期注册到 Namesrv 中 ； 第二个地方就是 AdminBrokerProcessor 请求处理器的 updateAndCreateTopic() 方法，因为在该方法中 Broker 节点接收到了来自 Admin 模块的更新主题信息的请求，这也就意味着接下来 Broker 节点要把最新的主题信息注册到 Namesrv 中 。  
  
而在上一章我们实现的 AdminBrokerProcessor 请求处理器的 updateAndCreateTopic() 方法并没有让 Broker 节点真正保存接收到的主题信息，也没有接着执行注册主题信息到 Namesrv 的操作，所以接下来还要重构 AdminBrokerProcessor 请求处理器的 updateAndCreateTopic() 方法。并且， 我决定首先就重构 AdminBrokerProcessor 请求处理器的 updateAndCreateTopic() 方法，因为在该方法中会把请求处理器注册给 Namesrv 节点，最终要调用的仍然是 BrokerController 类的注册 Broker 信息的方法 ，这也就意味着 AdminBrokerProcessor 请求处理器的 updateAndCreateTopic() 方法重构起来非常简单，BrokerController 类的 registerBrokerAll() 方法重构起来比较复杂，那肯定先重构简单的方法啊。接下来就请大家看一下我重构完毕的 AdminBrokerProcessor 中的 updateAndCreateTopic() 方法，请看下面代码块。  
上面代码块中的内容非常简单，我就不再解释了，总之我们可以看到，Broker 节点把主题信息更新到 TopicConfigManager 主题配置信息管理器之后，就调用了 BrokerController 类的 registerIncrementBrokerData() 方法，接下来我就为大家把这个方法的内容给大家展示一下，请看下面代码块。  
从上面代码块中可以看到， BrokerController 类的 registerIncrementBrokerData() 方法最后也是调用了 BrokerController 类的 doRegisterBrokerAll() 方法，把主题信息注册到 Namesrv 上了。而这个 doRegisterBrokerAll() 方法也会被 BrokerController 类的 registerBrokerAll() 方法调用 ，所以接下来我们就可以从 BrokerController 类的 registerBrokerAll() 方法开始重构，一路重构下去，看看 Broke 最后会被重构成什么样子。  
  
BrokerController 类的 registerBrokerAll() 方法会被定时任务定期执行，这就意味着 Broker 节点会在 registerBrokerAll() 方法中把 Broker 节点的配置信息和主题信息收集到一起，然后注册到 Namesrv 节点中。接下来，请大家看看我重构的 registerBrokerAll() 方法，请看下面代码块。  
上面代码块中的注释非常详细，我就不再重复解释了，总之，我们可以看到， 在调用了 BrokerController 类的 doRegisterBrokerAll() 方法之后，就会把 Broker 节点配置信息和所有主题信息都发送给 Namesrv 节点，当然，在该方法中执行的其实是 BrokerOuterAPI 对象的 registerBrokerAll() 方法 ，这个方法其实在前面的章节已经展示过了，那接下来我们就看一看，这个 BrokerOuterAPI 类的 registerBrokerAll() 方法有没有什么改变吧，请看下面代码块。  
好了，现在 BrokerOuterAPI 类也重构完毕了，那到此为止，本章内容就结束了。可以看到，代码量还是非常大的，内容也很多，大家可以多品味品味本章的内容，也可以再等一等，等下一章阅读完了再查看我提供的第七版本代码，现在第七版本代码 Namesrv 模块的很多内容都没有在文章中展示。可以说，本章内容完全是围绕 Broker 节点注册信息到 Namesrv 节点来展开的，至于 Namesrv 节点接收到 Broker 的配置信息和主题信息后应该怎么做我们并没有实现，因为这一部分功能也有点复杂，代码量很多，所以就放到下一章讲解吧。朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/hzf18s8h05zh90bv*  
*All content belongs to its respective owners and creators.*