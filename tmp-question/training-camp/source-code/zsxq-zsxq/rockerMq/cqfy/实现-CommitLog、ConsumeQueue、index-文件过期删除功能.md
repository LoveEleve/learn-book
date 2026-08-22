  
之前我们已经把第十七版本代码的功能实现完毕了，当时我跟大家说因为第十七版本代码内容很少，而且非常简单，所以先把第十七版本代码的内容讲解了，然后再回过头实现第十六版本代码的内容，现在就要开始讲解第十六版本代码的内容了，我还是想跟大家提前说一下，实际上第十六版本代码的内容并不难，只不过内容有点多。因为 CommitLog、ConsumeQueue、index 文件过期删除的功能都在第十六版本代码中实现，不仅如此还有 Broker 节点重启过程中的数据加载和数据恢复，以及鼓掌重启等功能也都是在第十六版本代码中实现的，这样看下来，第十六版本代码的内容确实非常多。当然，内容多并不意味着内容难，只要仔细阅读文章，按照我给大家展示的思路学习这些内容，我相信掌握这些内容对大家来说没一点难度。好了，闲话少说，本章我们就先把 CommitLog、ConsumeQueue、index 文件过期删除功能实现了吧。  
  
实现 CommitLog 文件过期删除功能  
  
其实文件过期删除的功能实现起来很简单，没什么可分析的，按照 RocketMq 源码中的设定，只要本地存在超过 72 小时的文件就算过期文件，这就意味着这些本地文件可以被删除了，当然，这个过期行为只限定在了 CommitLog 文件中；并且这也并不是说只要文件一过期就可以被立即删除了，实际上删除文件的操作可能并不会立刻执行，通常来说，要等待每天凌晨 4 点，程序内部才会把已经过期的本地文件删除了。这么分析下来，总结一句话那就是： 对于 CommitLog 本地文件来说，只要有文件存在超过 72 小时，那么在每条凌晨 4 点，程序内部就可以执行删除对应过期文件的操作 。当然， 还有一点大家肯定也能想到了，即然本地文件都删除了，那对应的内存映射文件对象，也就是 DefaultMappedFile 对象肯定要被释放了 ，这一点想必大家也都能理解吧？既然是这样，那对于过期 CommitLog 文件功能的实现，我们就可以直接在程序内部启动一个定时任务，那就开启一个定时任务，定期执行操作，然后在任务中判断哪些本地文件最后一次修改到当前系统时间过去 72 小时不就行了吗？当然，还得判断当前时间是不是凌晨 4 点，如果是凌晨 4 点，那就可以执行 CommitLog 过期本地文件删除的操作。这个逻辑大家都可以理解吧？  
  
删除过期 CommitLog 文件的定时任务被我就定义为 CleanCommitLogService 了，它是 DefaultMessageStore 的内部类，CleanCommitLogService 类的对象定义了定时任务要执行的操作，这个 CleanCommitLogService 对象会在 DefaultMessageStore 构造方法中被创建，然后被 DefaultMessageStore 默认消息存储引擎的定时任务执行器定时执行，就像下面代码块展示的这样，请看下面代码块。  
package org.apache.rocketmq.store;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/6/25

\* @方法描述：默认的消息存储引擎，这个消息存储就应就会把生产者客户端生产的消息提交给CommitLog对象，然后存储到CommitLog文件中

\* 可以说这个类的对象和CommitLog类的对象是rocketmq消息存储模块中最重要的两个功能对象

\*/

public class DefaultMessageStore implements MessageStore {  
  
protected static final Logger LOGGER \= LoggerFactory.getLogger (LoggerName.STORE\_LOGGER\_NAME);  
protected static final Logger ERROR\_LOG \= LoggerFactory.getLogger (LoggerName.STORE\_ERROR\_LOGGER\_NAME);  
  
//默认的消息存储引擎

private final MessageStoreConfig messageStoreConfig;  
//存储引擎是否关闭的标志

private volatile boolean shutdown \= true;  
//当前存储引擎的运行状态

protected final RunningFlags runningFlags \= new RunningFlags ();  
//定时任务执行器

private final ScheduledExecutorService scheduledExecutorService;  
//broker配置信息

private final BrokerConfig brokerConfig;  
//把消息存储到CommitLog文件中的核心组件

protected final CommitLog commitLog;

从上面代码块中可以看到，在 DefaultMessageStore 默认消息存储引擎启动的时候，就在 start() 方法中启动了 CommitLog 文件过期删除定时任务，这个定时任务每 10 秒会执行一次，任务一旦开始执行，CleanCommitLogService 对象的 run() 方法就会被执行，而在 run() 方法中，删除 CommitLog 过期文件的 deleteExpiredFiles() 方法就会被调用，当然，目前我们还没有真的实现该方法，但该方法具体的实现思路我刚才已经为大家分析过了，所以这个 deleteExpiredFiles() 方法首先起来也很简单。  
  
而根据我们刚才的分析，只需要在 deleteExpiredFiles() 方法中判断当前系统时间是不是凌晨 4 点，如果是凌晨 4 点，那就可以执行 CommitLog 过期文件删除操作了，而执行的时候，肯定需要调用 CommitLog 对象的 deleteExpiredFile() 方法，因为要删除的是 CommitLog 文件夹中的过期文件，那操作 CommitLog 文件夹的肯定是 CommitLog 对象呀，然后再判断 CommitLog 文件夹中的哪些文件是过期的，过期文件删除即可，最后把删除的过期文件对应的内存映射文件对象释放了，那么 CommitLog 过期文件删除操作就算执行完毕了。这个逻辑大家可以理解吧？  
  
我相信以上流程理解起来应该没什么问题，但我要补充的一点是， 实际上在 RocketMq 源码中还执行了一个额外的判断，那就是判断存储本地文件的硬盘的利用率是否超过了阈值，如果超过了阈值，那也要立即执行一次过期文件删除操作，而不必等到凌晨 4 点才执行。原因很简单，假如本地硬盘快没空间了，已经无法再存储新的数据了，那肯定要把过期数据删除了，好腾出新空间存储数据呀 。所以，我们也要仿照 RocketMq 源码，在我们自己的消息队列框架中也实现这个功能， 而且我们要像源码那样定义两个本地空间利用率阈值，这两个阈值分为两个等级，低等级的就是普通阈值，比如说本地硬盘利用率超过 85% 了，这时候就需要立刻执行一次本地过期文件删除操作；而高等级的阈值会被设置到 90%，如果本地硬盘利用率超过 90% 了，那这个时候不仅会立即执行删除过期文件的操作，还会把 DefaultMessageStore 消息存储引擎设置为已写满状态，这样一来消息存储引擎就不会再接收并存储新到来的生产消息了 。这两个阈值的作用大家也要理解清楚。  
  
好了，经过以上分析之后，现在我们就可以真正实现 CleanCommitLogService 类的 deleteExpiredFiles() 方法了，请看下面代码块。  
从上面代码块中可以看到，在执行过期文件删除操作的时候，调用了 CommitLog 对象的 deleteExpiredFile() 方法，那接下来我就为大家把该方法的内容再展示一下，请看下面代码块。  
从上面代码块中可以看到，在 CommitLog 对象的 deleteExpiredFile() 方法中又调用了 MappedFileQueue 内存映射文件管理球的 deleteExpiredFileByTime() 方法，该方法中执行的内容非常简单，就是判断有没有过期文件存在时间达到 72 小时了，如果有则删除该文件，然后释放对应的内存映射文件对象即可，那接下来我就把 MappedFileQueue 内存映射文件管理器的 deleteExpiredFileByTime() 方法展示给大家，请看下面代码块。  
到此为止，我就为大家把 CommitLog 过期文件删除的功能实现了，虽然代码中内容有点多，但是内容都非常简单，几乎不需要怎么分析，所以我也就简单解释几句，然后就直接给大家展示代码了，我们实在是没必要把时间和篇幅耗费在这么简单的内容上，请大家理解一下。  
  
实现 ConsumeQueue、index 文件过期删除功能  
  
好了，CommitLog 过期文件删除功能实现之后，我相信大家都在等着我继续实现 ConsumeQueue、index 文件过期删除功能，而这也正是我接下来要做的。实际上 ConsumeQueue、index 文件过期删除功能实现起来也非常简单，同样是定义一个定时任务，然后在定时任务中执行删除过期文件的操作即可，这个逻辑和刚才实现的 CommitLog 过期文件删除的逻辑没什么区别。但是，我想说的是， 这两个过期文件删除的功能并不是完全一样，最本质的区别就是 ConsumeQueue、index 过期文件的删除并不是根据时间来判断的，而是根据 CommitLog 文件中的最小数据偏移量来判断的 。这句话可能让大家感到困惑，接下来请听我解释一下。  
  
现在我们都知道了，ConsumeQueue、index 文件中的数据都是根据 CommitLog 文件中构建来的，是因为 CommitLog 文件中被写入了新的数据，为了消费者客户端能尽快消费消息，定位到消息，所以我们才异步构建了 ConsumeQueue、index 文件，尤其是异步构建的 ConsumeQueue 文件，和 CommitLog 文件夹中的数据耦合非常紧密，因为消费者完全是根据 ConsumeQueue 文件中的重放消息去消费 CommitLog 文件夹中的消息的。假如说消费者从 ConsumeQueue 文件中得到了一条重放消息，结果这条重放消息存储的消息全局偏移量在 CommitLog 文件夹中并没有对应的数据，这不就出问题了吗？ 所以当 ConsumeQueue、index 文件过期删除的时候，必须严格按照 CommitLog 文件夹中最小数据偏移量来删除 。就比如说，经 过一次过期文件删除之后，CommitLog 文件夹中存储的第一条消息对应的全局偏移量是 200 字节，这也就意味着前 200 字节的消息都已经过期删除了，也不必再被查询到和被消费了，那这个时候就可以使用这个 200 全局偏移量为界限，到 ConsumeQueue、index 文件中判断，因为这两个文件中的消息都封装着各自的全局偏移量信息，只要 ConsumeQueue、index 某个本地文件中存储的最后一条消息对应的全局偏移量是小于 200 字节，那么整个文件就可以被删除了，因为上消费队列中的数据和 index 文件中的数据也都是按顺序存放的，它们内部的重放消息封装的全局偏移量也都是递增的，如果消费队列某个文件中最后一条重放消息，对应的全局偏移量已经过期了，也就是小于 CommitLog 文件中数据的最小全局偏移量，那么这个消费队列文件中的所有数据肯定也都是过期的，可以被删除了， 大家要梳理清楚这个逻辑。  
  
如果以上逻辑大家都理解了，那接下来我们就可以定义 ConsumeQueue、index 文件过期删除的定时任务对象了，这个对象被我定义为了 CleanConsumeQueueService，也是默认消息存储引擎的内部类，创建和启动的逻辑和 CleanCommitLogService 一致，那接下来，就请大家看一下 CleanConsumeQueueService 类的具体内容，请看下面代码块。  
从上面代码块中可以看到，在 CleanConsumeQueueService 对象执行删除过期文件操作方法的时候，也就是执行 deleteExpiredFiles() 方法的时候，会根据 CommitLog 文件中的最小数据偏移量，把 ConsumeQueue、index 文件中的过期数据都删除了。当然真正删除还是调用了各自组件的 deleteExpiredFile() 方法，在文章中我就只为大家展示 ConsumeQueueStore 消费队列管理器的 deleteExpiredFile() 方法了，IndexService 的 deleteExpiredFile() 方法我就不展示了，内容都很简单，大家自己看看就行。  
  
接下来是 ConsumeQueueStore 消费队列管理器的 deleteExpiredFile() 方法，请看下面代码块。  
接下来是 ConsumeQueue 对象的 deleteExpiredFile() 方法，请看下面代码块。  
接下来就是 MappedFileQueue 对象的 deleteExpiredFileByOffset() 方法，请看下面代码块。  
好了，到此为止，ConsumeQueue、index 文件过期删除功能也实现完毕了，本章的核心内容也就结束了。这一章的内容是不是非常简单，连我自己都觉得太简单了，好像水了一篇文章，心里真的有些惭愧。我知道这不是我的风格，也不是我的性格，所以为了减轻自己内心的惭愧，我就再为大家补充一点内容吧。  
  
请大家仔细思考一下，从上面代码块中我们已经知道了，在删除 ConsumeQueue 消费队列过期数据的时候，也是按照文件为单元来删除的，虽然我们会有一个阈值标准，也就是以 CommitLog 文件中最小数据偏移量为标准判断 ConsumeQueue 文件中哪些数据过期了，但是我们并不会真的去对比 ConsumeQueue 消费队列每一个文件中每一条数据，判断这个重放消息是否过期，如果过期就删除。如果真的这么做显然就太麻烦了， 我们所做的只是对比 ConsumeQueue 消费队列的每一个文件，看看文件存储的最后一条重放消息封装的全局偏移量是否小于 CommitLog 的最小数据偏移量，如果小于那么就意味着这个 ConsumeQueue 本地文件可以删除了 。这个流程大家肯定都很清楚了。  
  
很好，如果大家都理解了以上流程，那现在就会出现一种情况，因为在删除 ConsumeQueue 过期文件的时候并不会精确到每一条重放消息，而是以文件为基本单元删除，那这个时候就可能会出现一种情况： 那就是在某个 ConsumeQueue 消费队列的本地文件中，这个文件存储的重放消息有一部分是过期的，有一部分是没有过期的 ， 那当消费者客户端消费消息的时候，就可能访问到 ConsumeQueue 消费队列中已经过期的重放消息，然后根据重放消息封装的消息全局偏移量去 CommitLog 中查询指定消息，肯定就查询不到了，因为这个消息已经被过期删除了 。大家可以品味品味这个逻辑。  
  
如果大家不明白上面的情况究竟是怎么回事，那么接下来请看看我为大家展示的一个示例：假如现在某个主题、Id 的 ConsumeQueue 消费队列下有三个本地文件，文件 1 中存储的重放消息对应的 CommitLog 全局偏移量范围为 101～300 字节；文件二中存储的重放消息对应的 CommitLog 全局偏移量范围为 301～500 字节；文件三中存储的重放消息对应的 CommitLog 全局偏移量范围为 501～700 字节；某个时刻程序内部对 CommitLog 文件执行了过期数据删除操作，在删除完毕之后，CommitLog 中最小数据全局偏移量变成了 401 字节，这也就意味着假如消费者客户端要到 Broker 节点消费消息，只能从 CommitLog 全局偏移量为 401 字节的位置开始消费消息，而之前的消息都因为过期被删除了。很好，再对 CommitLog 文件执行了过期删除操作之后，得到了目前 CommitLog 中最小数据起始偏移量为 401 字节，这个就是要对 ConsumeQueue 消费队列文件进行数据过期删除时做判断的阈值，那程序很快就对 ConsumeQueue 消费对类文件执行了数据过期删除操作，对比之后发现，ConsumeQueue 消费队列下的三个本地文件中，只有文件 1 可以被删除，虽然文件二中的部分重放消息已经过期了，但是文件二存储的最后一条重放消息对应的全局偏移量大于 401，这也就意味着这条重放消息对应的 CommitLog 中的消息还没有过期，可以被消费者客户端正常消费，因此 ConsumeQueue 消费队列下的文件二并不能被删除，文件二都不能被删除，那文件三肯定就更不能被删除了。那既然 ConsumeQueue 消费队列的第二个文件不能被删除，这就意味着有一部分过期消息对应的 CommitLog 全局偏移量还可以被消费者客户端得到，那消费者客户端使用这些过期消息的全局偏移量去 CommitLog 中消费消息，显然无法得到真正的消息，这不就出问题了吗？大家可以仔细思考思考，梳理清楚这种情况出现的原因。  
  
很好，如果现在大家都了解了以上这种情况出现的原因，也知道这种情况的存在是一个 bug，那我们接下来要做的就是要解决这个 bug。那这个 bug 该如何解决呢？其实方法非常简单， 那就是给 ConsumeQueue 类再定义一个 minLogicOffset 成员变量 ， 这个成员变量记录的是消费者客户端到 ConsumeQueue 消费队列中消费消息时，可以消费的最小重放消息的偏移量 。我知道仅凭刚才的文字，大家可能仍然不理解这个新的成员变量的作用，这么关系，还是回到刚才的示例，ConsumeQueue 消费队列下有三个本地文件，这三个本地文件存储的重放消息的整体范围是从 101～700，也就是说当消费者客户端到 ConsumeQueue 消费队列中获取重放消息时，就可以从 ConsumeQueue 消费队列偏移量为 101 的位置开始消费消息，每得到一条重放消息，再根据重放消息封装的全局偏移量到 CommitLog 中查询具体消息即可。 但现在 CommitLog 过期文件删除之后，消费者只能从 CommitLog 文件全局偏移量为 401 的位置开始真正消费消息，那如果我们要是能够确定，全局偏移量 401 的消息在 ConsumeQueue 消费队列文件中对应哪一条重放消息不就行了吗 ？比如说经过对比查询之后，发现在 ConsumeQueue 消费队列第二个文件的偏移量为 60 的重放消息，封装的 CommitLog 全局消息偏移量为 401，那这也就意味着，当消费者客户端到 ConsumeQueue 消费队列中消费消息时，只需要直接从第二个本地文件偏移量为 60 的位置开始读取重放消息，那么在这之后消费到的消息都是没有过期的，都是可以正常消费的， 这个 60 偏移量，就是要赋值给 ConsumeQueue 类的 minLogicOffset 成员变量 ！  
  
现在大家应该清楚了 ConsumeQueue 类的 minLogicOffset 成员变量的作用是什么了吧？虽然我们目前还不会真正用到 ConsumeQueue 类的 minLogicOffset 成员变量，等后面实现消费者客户端消费消息功能时才会用到它，但大家提前了解一下对理解目前程序的运行流程很有帮助。注意，我再强调一下， 这个 ConsumeQueue 类的 minLogicOffset 成员变量表示的是重放消息在 ConsumeQueue 消费队列中的偏移量，也就是消费者客户端可以消费的最早的重放消息的偏移量，并不是消息在 CommitLog 中的全局偏移量，这一点大家一定要梳理清楚；而 minLogicOffset 成员变量对应的重放消息封装的全局偏移量，这个全局偏移量对应的 CommitLog 中的消息，就是消费者客户端可以消费的最早的没有过期的消息，之前的消息全部被过期删除了 。大家一定要梳理清楚这个逻辑。  
  
好了，在理解了以上内容之后，那我们要解决刚才的 bug，只需要在 ConsumeQueue 消费队列每次执行了文件过期删除操作之后，根据 CommitLog 文件的最小全局偏移量给 minLogicOffset 成员变量赋值不就行了？也就是说在每次对 ConsumeQueue 消费队列每次执行了文件过期删除操作之后，根据 CommitLog 文件的最小全局偏移量确定在 ConsumeQueue 消费队列文件中对应哪一条重放消息即可。那这个操作该怎么实现呢？非常简单，在执行了过期文件删除之后，肯定只会在 ConsumeQueue 消费队列的新第一个本地文件中存在新旧数据同时存在的情况，原因很简单，如果新的第一个本地文件中的数据都是旧的，那肯定就会被删除了； 所以我们只需要使用 CommitLog 文件的最小全局偏移量去 ConsumeQueue 消费队列的第一个本地文件中确定哪一条重放消息封装的全局偏移量和 CommitLog 文件的最小全局偏移量相等即可，确定了重放消息之后，就可以得到重放消息在 ConsumeQueue 消费队列中的偏移量，然后把这个偏移量赋值给 minLogicOffset 即可 。这样一来， 等消费者到 ConsumeQueue 消费队列中消费重放消息时，只需要从 minLogicOffset 位置开始消费即可，这个时候消费者客户端最终得到的消息都是合法消息，没有一条消息是过期的 。现在大家应该清楚这个流程了吧？而至于怎么去 ConsumeQueue 消费队列的第一个文件中对比，这就更简单了，直接使用二分法查找对比即可，更多细节我就不再展开了，接下来请大家看看重构之后的 ConsumeQueue 类的内容，就全都明白了，请看下面代码块。  
到此为止本章内容就全部结束了，篇幅虽然有点长，但是内容真的很简单，大家可以直接阅读我提供的第十六版本代码了，虽然还有部分内容没有在文章中展示，大家可以先跳过部分内容，只阅读过期文件删除功能的代码。下一章我将为大家实现 Broker 节点故障恢复和数据加载功能，这部分内容更新完毕之后，那么 Broker 节点存储消息的全部功能就都实现完毕了，接下来我们就可以专心实现消费者客户端从 Broker 节点消费消息的功能了。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/an625lsbrzumhfeg*  
*All content belongs to its respective owners and creators.*