  
完善 LocalMessageService 类  
  
在上一章结尾，我们为 Proxy 模块引入了 LocalMessageService 类，并且我们也知道了就是在 LocalMessageService 对象中，Proxy 模块把接收到的生产消息转发给了 Broker 节点，当然，我们并没有真的实现这个 LocalMessageService 类，而是写成了伪代码，就像下面展示的这样，请看下面代码块。  
package org.apache.rocketmq.proxy.service.message;  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/6/17

\* @方法描述：本地消息服务组件，当proxy模块以本地模式部署时，就会使用这个类的对象把生产者客户端发送的消息专拣给部署在同一进程的Broker节点处理

\*/

public class LocalMessageService implements MessageService {  
private static final Logger log \= LoggerFactory.getLogger (LoggerName.PROXY\_LOGGER\_NAME);  
//Broker控制器，得到了这个控制器就相当于得到了Broker节点，可以把生产者生产的消息交给Broker节点处理

private final BrokerController brokerController;  
public LocalMessageService (BrokerController brokerController,RPCHook rpcHook) {

this.brokerController \= brokerController;

}  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：把生产者生产者消息转交给Broker节点处理的方法，在第十二版本代码中，我并没有实现该方法，等到第十三版本代码就会实现了

\*/

@ Override

public CompletableFuture < List < SendResult >> sendMessage (ProxyContext ctx,AddressableMessageQueue messageQueue,  
Message messages \= msgList.get (0);

//获取消息字节数组

byte \[\] bytes \= messages.getBody ();

//解码字节数组为字符串

String string \= new String (bytes,StandardCharsets.UTF\_8);

System.out.println ("接收到客户端发送过来的消息了:" + string + "！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！");

SendResult sendResult \= new SendResult ();

本来要在 LocalMessageService 对象的 sendMessage() 方法中，把 Proxy 接收到的生产消息转发给 Broker 节点，但我们并没有这么做，我把原因也解释清楚了： 因为当前 Proxy 节点是在本地模式下部署启动的，在本地模式下，Broker 和 Proxy 模块都被部署在了同一进程中，Proxy 直接就可以得到 Broker 节点。只要在 sendMessage() 方法中创建出 RequestCode.SEND\_MESSAGE 类型的请求，然后就可以直接把请求交给 Broker 节点处理了。当然，我们肯定得使用 brokerController 成员变量得到 Broker 服务端处理 RequestCode.SEND\_MESSAGE 请求的请求处理器，才能把请求直接交给请求处理器处理 。这个逻辑大家应该都很清楚了，但现在的问题是我们并没有为 Broker 模块实现对应的请求处理器，所以无法真的把消息直接交给 Broker 处理，于是就把 LocalMessageService 类的 sendMessage() 方法写成了伪代码。但坦诚地说，为 Broker 模块定义专门处理 RequestCode.SEND\_MESSAGE 请求的请求处理器并不是什么难事，这些内容放在上一章实现也完全可以，但为了防止文章篇幅过长，我就没有继续展开讲解。当然，我能想到肯定有很多朋友已经迫不及待想看后面的内容了，那么接下来，就让我们一起为 Broker 模块实现这个请求处理器。  
  
如果大家对之前的内容还有印象，那肯定还记得其实我们已经为 Broker 模块实现了一个请求处理器，那就是专门处理控制台发送过来的请求的处理器，也就是 AdminBrokerProcessor 请求处理器；如果我们使用控制台向 Broker 节点发送了 RequestCode.UPDATE\_AND\_CREATE\_TOPIC 更新和创建主题类型的请求，那么 Broker 节点接收到请求之后，就会把请求交给 AdminBrokerProcessor 请求处理器处理，在 Broker 节点内部创建或者更新对应的主题信息。这些流程大家应该都还有印象吧？当然，既然 AdminBrokerProcessor 请求处理器是专门为 Broker 节点服务的，那这个处理器肯定就是在 Broker 模块中创建的，也许大家已经把 AdminBrokerProcessor 请求处理器的创建过程忘记了，现在我把这部分的代码搬运过来了，帮助大家简单回顾一下，请看下面代码块。  
从上面代码块中可以看到， 在 Broker 节点启动然后初始化的过程中，在把各种请求处理器注册到 Broker 服务端中时，也就是在上面代码块的 registerProcessor() 方法中，创建了 AdminBrokerProcessor 请求处理器，然后把该处理器注册到 Broker 服务端了 。这个流程很清晰吧？我相信阅读完以上的示例代码，大家对请求处理器的创建时机以及发挥作用的原理多少都能回忆起一些了。  
  
好了，AdminBrokerProcessor 请求处理器的例子展示完之后，那现在我们要给 Broker 模块实现新的请求处理器了，并且这个处理器专门处理 RequestCode.SEND\_MESSAGE 类型的请求， 那么我们不妨就直接定义一个 SendMessageProcessor 请求处理器好了，从名字上就能看出来，这个处理器就是专门用来处理 RequestCode.SEND\_MESSAGE 类型的请求 。这个 SendMessageProcessor 请求处理器肯定也是在 Broker 模块中被创建的，根据刚才的例子， 我们完全可以在 BrokerController 类的 registerProcessor() 方法中把 SendMessageProcessor 处理器创建出来，然后把该处理器注册到 Broker 的 Netty 服务端中；当然，我们还得专门为该处理器定义一个执行器，这个执行器就可以定义为 sendMessageExecutor 。这个逻辑可以理解吧？这样编写代码的话，就相当于是完全模仿了 AdminBrokerProcessor 处理器的创建和注册流程，从逻辑上来说没有一点问题。但在 RocketMq 源码中并不是这么做的， 在源码中把 SendMessageProcessor 请求处理器定义为了 BrokerController 的成员变量，在 BrokerController 的构造方法中创建了 SendMessageProcessor 对象，然后在 registerProcessor() 方法中把处理器注册到 Broker 的 Netty 服务端中 。我认为这么做也没什么问题，逻辑都是相同的，因为我们是仿照 RocketMq 源码来实现消息队列框架，所以就像源码那样创建和注册 SendMessageProcessor 请求处理器吧，我已经把 BrokerController 类重构完毕了，请看下面代码块。  
好了，现在重构之后的 BrokerController 类也展示完毕了，我们已经知道 SendMessageProcessor 请求处理器怎么创建和注册了，这些都是非常简单逻辑。当然，我们还没有真的实现 SendMessageProcessor 处理器，但这并不妨碍我们继续分析其他尚未完善的内容该如何完善，现在就让我们先假装这个 SendMessageProcessor 请求处理器已经实现完毕了，那么按照正常的流程，处理器已经定义完毕了，接下来应该执行什么呢？  
  
接下来就是很常规的流程了， Broker 节点处理 RequestCode.SEND\_MESSAGE 类型请求的处理器已经定义完毕了，那么 Proxy 节点就可以直接把 RequestCode.SEND\_MESSAGE 类型的请求发送给 Broker 节点了，而 Broker 节点的 Netty 接收到请求之后，就会根据请求类型找到对应的请求处理器处理请求 ，这个流程可以说是最常规的了。但是，我们的程序目前是以本地模式启动的，在本地模式下 Proxy 和 Broker 节点部署在同一进程中，两个节点本来就部署在一起，那么两个节点之间通信就根本涉及不到网络传输，这就意味着，Proxy 节点可以直接得到 Broker 节点，如果还能再从 Broker 节点中得到处理 RequestCode.SEND\_MESSAGE 请求的处理器，不就可以直接处理请求了吗？  
  
一切就是这么巧，从上面展示的代码块中可以看到， 我给 BrokerController 类定义了一个 getSendMessageProcessor() 方法，这个方法可以直接返回 Broker 节点的 SendMessageProcessor 请求处理器；而在 LocalMessageService 类中定义了一个 BrokerController 成员变量，这样一来，只需要在 LocalMessageService 类的 sendMessage() 方法中使用 BrokerController 成员变量得到 SendMessageProcessor 请求处理器，不就可以直接把请求交给 Broker 节点处理了吗 ？如果要对上一章没有真正实现的 LocalMessageService 类进行重构，那就可以重构成下面这样，请看下面代码块。  
从上面代码块可以看到，正如我们之前分析的那样，只要使用 BrokerController 得到 SendMessageProcessor 请求处理器，就可以直接把封装了生产消息的请求交给 Broker 节点来处理，这一点已经很清楚了吧？当然，上面代码块的 sendMessage() 方法依然是伪方法，并没有真的被我们实现，原因很简单， 因为 Broker 模块属于 RocketMq 程序的内部模块，而内部模块会使用 Remoting 协议进行通信，虽然 Proxy 节点和 Broker 节点在本地模式下通信并不涉及网络传输，但是也会发送请求和响应，Proxy 节点把消息请求发送给 Broker 节点，Broker 节点的 SendMessageProcessor 请求处理器处理完毕之后要把响应结果回复给 Proxy 节点，Proxy 节点再把消息存储结果回复给生产者客户端，这是一套完整的消息存储流程 。而在目前的 LocalMessageService 类的 sendMessage() 方法中，Proxy 节点持有的只是 Message 消息对象本身，要把这些生产消息发送给 Broker 节点，肯定要把这些消息封装到对应的 Remoting 协议请求对象中，然后把请求交给 Broker 节点处理。所以我们接下来要做的就是重构 LocalMessageService 类，在 LocalMessageService 类的 sendMessage() 方法中创建出 Remoting 协议中发送生产消息的请求对象，然后把请求对象交给 Broker 节点处理。这样分析下来，LocalMessageService 可以重构成下面这样，请看下面代码块。  
从上面代码块中可以看到，我们已经把生产消息封装到了一个RemotingCommand 请求对象中，然后把该对象交给 Broker 节点的 SendMessageProcessor 请求处理器处理。这些操作执行完毕之后，生产者生产的消息就终于传递给 Broker 节点了，那接下来，Broker 节点就要开始存储这些消息了。那这个功能该怎么实现呢？是不是要开始真的实现 SendMessageProcessor 请求处理器了呢？毕竟 Broker 节点的 SendMessageProcessor 请求处理器是首先处理生产消息的组件。这就是我们接下来要讨论的内容。  
  
引入 DefaultMessageStore 消息存储引擎  
  
按理说我们确实该真的实现 Broker 节点的 SendMessageProcessor 请求处理器了，然后在请求处理器中解析 Proxy 节点发送过来的生产消息，然后把生产消息存储在本地。这就是一个大概的流程，但这个流程就像把大象装进冰箱要分成三步一样，太笼统了，没有一点细节。就比如说 SendMessageProcessor 请求处理器在处理消息的时候，该怎么存储消息呢？需要得到已经被选中的消息队列的 Id 吗？别忘了， 在 Proxy 节点把消息发送给 Broker 节点的时候，创建了 RequestCode.SEND\_MESSAGE 类型请求的请求头，也就是 SendMessageRequestHeader 对象，而目标消息队列的 Id 就被封装到请求头中了 。至于为什么可能会用到消息队列的 Id，是因为之前我们分析过，在 Proxy 发送生产消息给 Broker 节点时，已经为 Broker 节点的对应主题创建了读写队列，并且我们还使用了队列选择器选中了某个具体的写队列，然后把消息发送给写队列所属的 Broker 节点了。当时我们是这么理解的： 在 Proxy 节点中明确了生产消息要发送的写队列，那么当消息发送到写队列所属的 Broker 节点之后，消息就要存储到 Broker 内部的写队列中，写队列的 Id 就是我们之前在 Proxy 节点选中的写队列的 Id 。大家对这个逻辑应该还有印象，那现在 Broker 节点已经接收到生产者客户端生产的消息了，也能得到消息队列 Id，那这个时候是不是应该在 Broker 内部找到对应主题下的消息队列，也就是写队列，然后把消息存储到写队列中呢？也许大家会觉得这么做没什么不合适，但我想说的是，在 RocketMq 源码中，Broker 节点存储消息的逻辑和我们预想得可能一点也不一致。  
  
实际上是这样的， 在源码中 Broker 接收到生产者客户端的消息后，并不会直接处理消息，而是会把消息交给 store 存储模块下的存储引擎处理，这个存储引擎就是 DefaultMessageStore，翻译过来就是默认存储器的意思 ，我自己习惯把它称为存储引擎。那这个时候可能会有朋友认为，既然 Broker 模块不会直接处理消息，而是把消息交给 DefaultMessageStore 默认存储引擎处理，那是不是会在 DefaultMessageStore 默认存储引擎中找到对应的目标写队列，然后把消息存储到目标队列中呢？我知道有朋友可能会这么思考，所以我就提前告诉大家吧： 即便是在源码的 DefaultMessageStore 默认存储引擎中，也不会把消息存储到所谓的目标队列中，而是会把消息存储到 CommitLog 文件中，也就是说，这些消息数据都会存储到本地硬盘中 。现在大家肯定很懵，既不清楚 DefaultMessageStore 默认存储引擎和 CommitLog 文件是什么，也不明白为什么在真正存储消息的时候，突然用不到所谓的目标队列了。我知道这些问题肯定会让大家困惑不已，但我还是那句话，别着急，耐心等待我为大家依次解决这些问题。  
  
既然我们从 RocketMq 源码中知道了生产者客户端生产的消息要存储到 DefaultMessageStore 默认存储引擎中，那我们目前要做的就是为我们自己的消息队列框架把这个 DefaultMessageStore 存储引擎定义出来，至于 CommitLog 文件是什么，以及消息是如何存储在 CommitLog 文件中的，这个大家最关心的问题还是先放一放，等后面再讲解吧。那 DefaultMessageStore 默认存储引擎该怎么定义呢？这个就没什么好说的了，就直接仿照源码实现吧，我想为自己解释几句，我并非想偷懒，不为大家分析这个类的具体实现逻辑，而是很多逻辑现在没办法分析， 就比如说在 DefaultMessageStore 类中定义了一个 load() 加载数据的方法，该方法会在 Broker 节点启动的过程中被调用，按照常规理解，这个方法的作用似乎就是把本地数据加载到内存中，但是等这个加载数据的功能全部实现了，大家就会发现加载数据的操作以及流程比你想象中要复杂得多，而所谓加载的数据也和你一开始想象的不太一样。现在我们连存储数据的功能都没实现，怎么可能先实现加载数据的功能呢 ？这就好比建造一座二层小楼，你能直接建造第二层吗？显然不行，所以我采取的策略是，先把存储数据这些最基础的功能实现了，然后再回过头把其他功能再一一完善了。  
  
好了，解释完毕之后，就让我直接按照 RocketMq 源码中的内容先把 DefaultMessageStore 消息存储引擎定义出来吧， 因为 DefaultMessageStore 消息存储引擎要把消息存储到 CommitLog 文件中，所以在源码中也定义了一个 CommitLog 类，这个类的对象就是用来操作 CommitLog 文件的，而且还把 CommitLog 对象定义为了 DefaultMessageStore 类的成员变量 ，这样一来，DefaultMessageStore 就可以直接得到 CommitLog 对象，然后把消息交给 CommitLog 对象存储了。除此之外， DefaultMessageStore 中还需要定义一个 MessageStoreConfig 消息存储引擎的配置信息成员变量，这个 MessageStoreConfig 配置信息对象中封装了消息存储引擎运行过程中需要的所有配置信息 ；这两个成员变量是必须定义的，当然还有一些其他的成员变量也要定义在 DefaultMessageStore 类中，但这些成员变量的作用都很简单清晰，我就不在依次分析了，一会直接看我提供的代码块即可。  
  
除了成员变量，DefaultMessageStore 默认存储引擎中肯定还要定义一些方法，就算我们是仿照源码来实现的，一开始也没必要弄得特别复杂，所以我现在只给 DefaultMessageStore 类定义了五个核心方法和其他一些无关紧要的方法： 五个核心方法就是加载数据的 load() 方法、启动消息存储引擎的 start() 方法、终止存储引擎工作的 shutdown() 方法，以及最重要，也是最核心的把消息交给 CommitLog 对象存储到本地的方法；按照管理来说，存储数据，尤其是数据落盘的操作一般都分为同步和异步，所以要把第四个核心方法分为同步存储和异步存储两个方法，同步让 CommitLog 对象存储的方法被定义为了 putMessage() 方法，异步让 CommitLog 对象存储的方法被定义为了 asyncPutMessage() 方法 。这样被分析下来之后，这个 DefaultMessageStore 类目前可以被定义成下面这样，请看下面代码块。  
上面代码块的内容虽然有点多，但是都很简单，每一个方法的内容基本上都不会超过 5 行代码，毕竟这些方法都做了大量简化，而且注释也很详细，所以我就不再重复讲解了。唯一需要强调的就是在上面代码块中可以看到， 在 putMessage() 同步存储消息到 CommitLog 文件的方法中，仍然是执行了 asyncPutMessage() 异步存储消息的操作，只不过会让线程阻塞同步等待结果而已。而 asyncPutMessage() 异步存储消息的方法就更简单了，就是直接把消息交给 CommitLog 对象去异步存储，然后定义了一个回调方法，判断存储时间是否超时，超时则记录日志告警信息 。这就是目前实现的 DefaultMessageStore 类的核心内容，好了，现在 DefaultMessageStore 类实现完毕了，那接下来我把经过大量简化后的 CommitLog 类也给大家展示一下，请看下面代码块。  
从上面代码块中可以看到 CommitLog 类的内容确实非常简单，最核心的方法，也就是异步落盘消息的 asyncPutMessage() 方法也被我写成了伪实现，大家可以启动第十三版本代码的各个模块，然后启动生产者客户端测试类，就能在控制台上看到输出的消息信息了。当然，还是那句话，大家先别去深入探究 CommitLog 文件究竟是什么，以及消息是如何存储在 CommitLog 文件中的，先把第十三版本这些内容都掌握了，这是最简单的内容，后面我会为大家详细重构 CommitLog 类和 DefaultMessageStore 类，到时候大家就知道 CommitLog 文件究竟是什么，以及消息是怎么存储的了。  
  
好了，现在 DefaultMessageStore 类和 CommitLog 类都定义完毕了，那接下来就有一个很普通的问题了，那就是 DefaultMessageStore 类和 CommitLog 类的对象是在哪里被创建的呢？这时候我就不卖什么关子了： CommitLog 对象就是在 DefaultMessageStore 类的构造方法中被创建的 ，这部分内容我没有展示，大家直接看我提供的第十三版本代码即可，而 DefaultMessageStore 对象就是在 BrokerController 类中被创建的，就在 Broker 节点启动过程中，在 BrokerController 类的 initialize() 方法中被创建的，具体实现请看下面代码块。  
好了，到此为止和 DefaultMessageStore 相关的内容就讲解完毕了，接下来似乎可以回头看看了，在第二小节开始，我们就决定要实现 SendMessageProcessor 请求处理器，但是我们不知道请求处理器究竟该如何处理请求，由此引出了 DefaultMessageStore 和 CommitLog 两个类，虽然往前迈了一步，知道了消息存储时似乎并不需要什么消息队列的 Id，更不需要寻找什么消息队列，但消息究竟怎么被 CommitLog 存储到本地文件中，这个逻辑我们还没梳理清楚，还不知道如何实现这个功能。但在第十三版本代码中我们也没必要寻根问底了，该实现的功能总会实现，反正我们已经知道了消息要被提交给 DefaultMessageStore 消息存储引擎来处理，那就先根据这个逻辑把 Broker 服务端的 SendMessageProcessor 请求处理器实现了吧。  
  
实现 Broker 的 SendMessageProcessor 请求处理器  
  
对 SendMessageProcessor 请求处理器的实现其实也没什么可分析的，要说核心逻辑， 无非就是把 Proxy 节点发送过来的请求直接提交给 DefaultMessageStore 消息处理器来处理，当然提交的时候要判断是同步存储还是异步存储，然后执行响应的方法即可 。如果只展示这个核心逻辑，那么 SendMessageProcessor 请求处理器可以实现成下面这样，请看下面代码块。  
上面代码块的内容非常简单，我相信每一个朋友都能看懂，因为 SendMessageProcessor 类实现得太简陋了，以至于会让大家产生这么实现究竟是否正确的怀疑。我可以很肯定地告诉大家，我们不能真的把 SendMessageProcessor 类编写成这样，因为还有很多其他的操作根本没有执行。 比如说 SendMessageProcessor 接收到请求之后，肯定需要解析请求，判断当前处理的请求中封装的消息为单条消息还是批量消息，不同的消息模式需要执行不同的处理方法 ；除此之外还有一点要特别注意，那就是在 RocketMq 源码中， 当 Broker 的 SendMessageProcessor 请求处理器把生产者生产的消息交给 DefaultMessageStore 存储引擎去存储时，会把消息封装到 MessageExtBrokerInner 对象中 ，从名字上就能看出来，MessageExtBrokerInner 就是 Broker 节点内部使用的消息对象的意思。 除了消息本身的内容，还会把目标队列 Id，消息生产的时间以及生产消息的服务器的网络地址等等信息都封装到 MessageExtBrokerInner 对象中，然后把该对象交给 DefaultMessageStore 存储引擎去处理 ，所以我们也要模仿源码把这个功能也实现了；除此之外，还有一点需要注意， 那就是要把 Broker 节点回复 Proxy 节点响应的功能也考虑到，别忘了我们在 LocalMessageService 类的 sendMessage() 方法中执行了什么操作，我们直接把请求交给 Broker 节点的 SendMessageProcessor 处理器处理了，这就意味着不管 SendMessageProcessor 请求处理器把请求交给谁处理，它肯定需要把请求的处理结果返回给 Proxy 节点 。虽然我们现在还不必实现 Broker 节点回复 Proxy 节点响应的功能，但至少考虑到这一点，要把这个功能的位置预留出来，等 Broker 节点存储消息的功能全实现了，再把回复 Proxy 节点响应的功能给补上。如果把我们刚才分析的内容重构到 SendMessageProcessor 请求处理器中，那这个处理器中的内容就更丰富了，就变成了下面这样，请看下面代码块。  
到此为止，本章内容就全部结束了，大家也可以直接阅读我提供的第十三版本代码的所有内容了，可以看到本章内容并不多，只不过是实现了 Broker 节点的 SendMessageProcessor 请求处理器，然后又引入了两个新组件，分别是 DefaultMessageStore 类和 CommitLog 类，虽然这两个类目前还不完善，有很多功能没有实现，但我可以很明确地告诉大家，在未来，这两个类将会是 RocketMq 存储消息功能模块最核心的两个组件，到时候大家就清楚了。下一章我将会继续完善 DefaultMessageStore 和 CommitLog 类，开始真正实现消息存储功能，大家对于消息队列的困惑也将被解决，为什么我们明明选择了目标消息队列，也知道消息队列 Id，但在真正存储消息的时候用不上消息队列呢？这里说的消息队列指的是写队列，随着消息存储功能的实现，这个问题很快就会被解决。好了朋友们，我们下一章见！  
  
  
附：大家一定要把第十三版本代码看完了，掌握完毕了，再去阅读下一篇文章。而且我还要再强调一下，第十三版本代码中有部分内容并没有在文章中讲解，就是 Broker 回复 Proxy 节点响应的代码，大家真的先不必关心 Broker 节点怎么回复响应，这是个复杂的操作，等我们实现了消息存储功能之后，就会从头开始梳理 Proxy 节点接收 Broker 节点响应的全流程，也就是从 LocalMessageService 类的 sendMessage() 方法中开始梳理。还有一点我想说的是，大家千万不要觉得现在的进度有点慢，好像每个版本代码实现的功能并不多，也并不复杂，觉得我把内容拆分得过于细致，甚至有点罗嗦了。我还是那句话，请大家一定要保持耐心，尽可能认真阅读完每一篇文章，看完每一个版本代码，RocketMq 的体量不算很大，但也绝不能称为小框架，有很多核心内容值得学习，把这些内容和功能一点点引入，并且实现，这样学起来才没什么坡度。请大家保持足够的耐心吧！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/ewu46n3orxsr0gtp*  
*All content belongs to its respective owners and creators.*