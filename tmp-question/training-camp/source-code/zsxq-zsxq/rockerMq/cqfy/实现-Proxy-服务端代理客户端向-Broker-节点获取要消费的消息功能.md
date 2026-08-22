上一章我们已经实现了消费者客户端向 Proxy 服务端发送 ReceiveMessageRequest 请求的功能，并且这个功能实现起来非常简单，我相信大家很容就能掌握，接下来我们就要加快速度，继续往后实现新的功能，本章要实现的就是 Proxy 节点处理 ReceiveMessageRequest 请求的功能。根据我们之前实现的 RocketMq 生产者客户端和 Proxy 节点的关系来看，客户端并不会再直接和 Broker 节点进行网络通信，客户端会先把请求发送给 Proxy 节点，然后由 Proxy 节点处理请求，根据请求类型和 Broker 节点进行通信，最后再把 Broker 节点回复的响应返回给客户端。这是生产者客户端执行操作的流程，现在换成了消费者客户端也是一样。  
  
为 Proxy 节点引入 ReceiveMessageActivity 活动器  
  
好了，现在要为 Proxy 节点实现处理 ReceiveMessageRequest 请求的功能，那么还是按照之前已经确定的流程来实现，所谓确定的流程，就是客户端发送过来的请求首先会被 proxy 服务端的 GrpcMessagingApplication 对象接收并处理，这一点在之前实现生产者客户端和 Proxy 服务端交互功能的时候已经重复过很多次了，所以我就不再过多解释了。我们已经知道，SimpleConsumerImpl 消费者客户端会执行它的 receiveMessage() 方法把 ReceiveMessageRequest 请求发送给 Proxy 节点，那么在 Proxy 节点内部处理 ReceiveMessageRequest 请求时，肯定也要定义一个同名方法处理请求，也就是说，应该给 Proxy 服务器的 GrpcMessagingApplication 对象定义一个 receiveMessage() 方法接收并处理消费者客户端发送过来的 ReceiveMessageRequest 请求，就像下面代码块展示的这样，请看下面代码块。  
package org.apache.rocketmq.proxy.grpc.v2;  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/3

\* @方法描述：Grpc服务端消息处理应用，当Grpc客户端每一次执行远程调用方法的时候，服务端的这个类的对象，都会根据客户端远程调用的方法的名称，执行对应的方法

\* 这个类的对象就相当于服务端提供目标方法的服务组件

\*/

public class GrpcMessagingApplication extends MessagingServiceGrpc.MessagingServiceImplBase implements StartAndShutdown {  
private final static Logger log \= LoggerFactory.getLogger (LoggerName.PROXY\_LOGGER\_NAME);  
//Grpc服务端的默认消息处理器

private final GrpcMessingActivity grpcMessingActivity;  
//Grpc服务端处理客户端发送过来的根据主题查询路由信息的请求时，就会使用这个线程池来处理

protected ThreadPoolExecutor routeThreadPoolExecutor;  
//Grpc服务端处理客户端发送过来的客户端信息请求时，就会使用这个线程池来处理

protected ThreadPoolExecutor clientManagerThreadPoolExecutor;  
//和生产者客户端相关的操作都由这个执行器执行

protected ThreadPoolExecutor producerThreadPoolExecutor;  
//和消费者客户端消费消息相关的操作都由这个执行器执行

protected ThreadPoolExecutor consumerThreadPoolExecutor;  
//省略该类其他内容  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：pop消费者客户端从服务端获取消息的方法

\*/

从上面代码块可以看到，我已经给 GrpcMessagingApplication 对象定义了一个接收并处理 ReceiveMessageRequest 请求的 receiveMessage() 方法，并且在该方法中，我们把 Proxy 服务端接收到的 ReceiveMessageRequest 请求交给了 GrpcMessingActivity 消息处理器来处理，这和 Proxy 服务端处理生产者客户端发送过来的请求逻辑是一致的，所以我们接下来只需要和之前一样，到 GrpcMessingActivity 中继续看看它是怎么处理 ReceiveMessageRequest 请求的即可。当然，GrpcMessingActivity 类我已经重构完毕了，所以接下来我就直接给大家展示 GrpcMessingActivity 类的相关代码了，请看下面代码块。  
从上面代码块中可以看到，我们为 Proxy 服务端引入了一个新的组件，那就是 ReceiveMessageActivity 请求活动器，就像之前 Proxy 节点处理生产者客户端发送过来的消息那样，Proxy 节点在专门处理消费者客户端发送过来的请求时，也需要为这个请求定义一个专门的请求活动器处理该请求。那接下来我们就要到 ReceiveMessageActivity 请求活动器中看看它是如何处理 ReceiveMessageRequest 请求的。  
  
当然，我肯定已经把 ReceiveMessageActivity 请求活动器实现了，要不然也不可能马上给大家展示这个类的内容，但是在展示该类的具体内容之前，我还是想多解释几句。根据我们以往的经验，我们应该很清楚，就算让 ReceiveMessageActivity 请求活动器处理 ReceiveMessageRequest 请求，在这个活动器中，也不可能直接执行核心操作，所谓核心操作就是根据消费者客户端发送的请求，返回可以让消费者消费的一批消息，这个操作肯定不会在 ReceiveMessageActivity 活动器中执行。原因很简单， 消息存储在 Broker 节点中，所以最终返回给消费者客户端响应的一定是 Broker 节点，Broker 节点把一批消息返回给消费者客户端供其消费；而在本地模式下，Broker 节点又是内嵌在 Proxy 节点中启动的，所以 Proxy 节点执行的操作是先解析消费者客户端发送过来的请求，然后再向 Broker 节点发送请求以便让 Broker 节点返回可供消费的消息 。我想，这一点逻辑理解起来并不困难。既然是这样，我们知道了 ReceiveMessageActivity 活动器并不会执行什么核心操作，那它负责的工作是什么呢？就让我直接告诉大家吧， 在 ReceiveMessageActivity 内部所作的只是解析请求中的一些参数，进行合法校验，然后把请求内容往更深处的组件传递，比如说，会传递给 DefaultMessagingProcessor 消息处理器，让该处理器进一步处理消费者客户端发送过来的请求内容 。  
  
除了进行参数的合法校验， 在 ReceiveMessageActivity 活动器中还执行了一个很重要的操作，那就是负载均衡选择具体的目标队列 。这一点也很容易理解吧？既然消费者客户端要从 Broker 节点消费消息，那肯定要找到具体的目标队列吧？就像生产者生产消息，要把消息存储到主题下的目标队列中。看到这里大家肯定有些困惑，之前我们不是引入了一种新的消费模式，也就是 Pop 消费模式，在这种模式下，消费者客户端不是可以消费订阅主题下的所有队列中的消息吗？并且我们正在实现的也正是 Pop 消费消息模式，那为什么要在这里执行目标队列负载均衡操作呢？生产者生产消息肯定要在 Proxy 节点内部执行目标队列负载均衡操作，这个功能我们已经实现了，但是根据我们的分析，Pop 消费模式似乎根本不需要负载均衡选择目标队列呀？  
  
接下来就让我为大家解开这个困惑吧， 在 ReceiveMessageActivity 活动器中确实还执行了目标队列负载均衡操作，但是，这个操作并不是真的选择了一个 Id 明确的目标队列，而是得到了一个 Id 为 -1 的目标队列。大家肯定都清楚，队列 Id 肯定是要大于 0 的，但在这里得到了 Id 为 -1 的目标队列，这样一来，在 Broker 节点内部，发现消费者客户端要消费 Id 为 -1 的目标队列时，就会直接让消费者客户端在一个循环中获取主题下所有队列的消息，直到消息数量达到本批次数量上限 。当然，现在我们还看不到 Broker 节点是怎么执行这个操作的，大家可以先记住这个逻辑，先理解了 Proxy 节点内部为消费者客户端执行的负载均衡操作的意义，等后面实现具体功能的时候，大家就全清楚了。  
  
好了，ReceiveMessageActivity 活动器中负载均衡操作解释完毕了，接下来就让我再为大家解释一下活动器内部对客户端请求执行的参数解析操作究竟是什么。首先我们必须要明确一点，既然生产者客户端可以向 Broker 发送顺序消息(只要给生产者定义一个组名，消息就会被发送到同一队列下，保证单一队列消息的顺序性)，那么消费者客户端显然就可以顺序消费消息，当然，我们并不会在第二十版本代码中就实现消费者顺序消费消息功能，至于 Pop 模式下的消费者客户端如何顺序消费消息，我现在也不会跟大家详细剖析它的流程，我只是觉得有必要先让大家提前了解一下消费者客户端具有顺序消费消息的功能。讲到这里大家肯定也意识到了， 在 ReceiveMessageActivity 活动器中肯定就要从接收到的请求中判断消费者客户端是否要执行顺序消费消息操作，如果消费者客户端要执行顺序消费操作，就会把顺序消费的标志设置到客户端设置信息中，然后注册到 Proxy 节点内部 。也许大家不理解这句话是什么意思，就让我继续解释一下，如果大家已经把前面几章对应的代码全部看完了，肯定知道在消费者客户端启动之后会把自己的设置信息注册到 Proxy 节点内部，所谓的设置信息就是下面代码块展示的这些内容，请看下面代码块。  
上面代码块的内容，想必大家已经很熟悉了，我就不再重复解释了。总之，可以看到，客户端的诸多信息都被封装到了 Settings 对象中，之后这个 Settings 对象就会被发送给 Proxy 节点。当然，在上面代码块中并没有看到和消费者客户端顺序消费相关的任何设置信息，这是因为在源码中并没有这么设置，实际上， 只要在创建客户端订阅信息对象的时候，也就是 Subscription 对象，执行该对象的 setFifo() 方法，并且把方法参数设置为 true，就表示当前消费者客户端要顺序消费消息 。就像下面代码块展示的这样，请看下面代码块。  
上面代码块展示的内容可能对大家有些陌生，我把内容展示出来的意思也很简单，就是让大家知道客户端顺序消费的标志怎么设置，至于其他的内容大家也不用特别关心。看到这里也许大家会有一个疑问，既然源码中并没有给 Pop 模式的消费者客户端设置信息定义顺序消费，那当我们使用 Pop 模式消费者客户端，也就是 SimpleConsumerImpl 消费者客户端向 Broker 节点消费消息时，必须要修改客户端源码，否则就没办法使用顺序消费了吗？当然不是这样，这里我就不展开讲解了，等后面我会特意为大家实现一个顺序消费的客户端，到时候大家就清楚这是怎么回事了。好了朋友们，接下来还是让我们继续回到 ReceiveMessageActivity 活动器要执行的操作上。  
  
从刚才的 SimpleConsumerSettings 类的 toProtobuf() 方法中，大家肯定还会注意到， 消费者客户端还会把一个长轮询等待时间设置到 Subscription 对象中，然后注册到 Proxy 节点中 。关于长轮询等待时间，我之前并没有给大家提过，现在解释时机正好，请大家思考一下，当消费者客户端通过 Proxy 节点到 Broker 节点中获取可消费的消息时，如果 Broker 节点内部并没有对应的消息，也就是说客户端订阅的主题下还没有可消费的消息呢，这个时候该怎么办呢？直接让 Broker 节点返回一个空响应吗？我们当然可以这么做， 但最常规的做法肯定是让 Broker 节点把请求挂起，等待一段时间，如果在请求等待的时间中，主题下有消息到来了，那么 Broker 节点就可以把消息返回给客户端；如果超过指定的等待时间仍然没有消息到来，这个时候再让 Broker 节点回复客户端没有可消费的消息的响应也更加合情合理。而这个请求挂起等待的时间，就是长轮询等待时间 。这个概念大家应该都可以理解吧？在很多框架中都用到了长轮询请求功能。  
  
好了，现在既然引入了长轮询概念，那么显然我们还要再实现一个新的功能， 那就是当消费者客户端到 Broker 节点消费消息时，如果对应主题下没有消息，那就应该让 Broker 节点把客户端请求挂起等待，直到等待超时或者有消息到来，才返回客户端响应 。当然，这个功能并不急着实现，显然它是属于 Broker 节点内部的一个功能，等后面真正用到的时候再做实现即可。这会儿我想说的是，这个长轮询等待时间显然也会注册到 Proxy 节点内部， 而在 ReceiveMessageActivity 活动处理器中还会对消费者客户端的长轮询等待时间进行校验 ，也许大家还记得当消费者客户端发送 ReceiveMessageRequest 请求给 Proxy 节点的时候，在 SimpleConsumerImpl 的父类对象中执行了发送消息的方法，也就是 receiveMessage() 方法，具体内容请看下面代码块。  
在上面代码块的第 66 行执行了这样一行代码：clientManager.receiveMessage(endpoints, metadata, request, timeout)，在这行代码中， 客户端管理器就把请求发送出去了，而在这个方法中有一个 timeout 方法参数，这个参数的意义也很明确，那就是消费者客户端等待响应的超时时间 ，这一点没有异议吧？现在我想说的是，这个客户端等待响应的超时时间也会被发送给 Proxy 节点，具体操作是在最终发送请求的 RpcClientImpl 对象中执行的，具体内容请看下面代码块。  
从上面代码块中可以看到，消费者客户端等待响应的超时时间也会随着请求传递给 Proxy 节点，那这和 ReceiveMessageActivity 活动处理器对消费者客户端长轮询等待时间的校验操作有什么关系呢？请大家想一想， 假如 Broker 节点内部并没有消费者客户端要消费的消息，这个时候请求就会被 Broker 节点挂起，等待一段时间之后，或者有消息了，Broker 节点才会回复响应。那假如在请求被挂起的过程中，消费者客户端等待响应的时间到了呢？本来应该回复客户端响应了，但是请求正在被挂起，那客户端等待响应肯定就超时了 ，这个问题可以理解吧？如果理解了这个问题，接下来就很简单，我们只需要在 ReceiveMessageActivity 活动处理器进行请求信息校验时，让它根据客户端请求截至时间修正长轮询等待时间不就完了吗？保证 Broker 节点既能正常挂起客户端长轮询请求，又不至于让客户端请求超时。现在大家应该理解了 ReceiveMessageActivity 活动处理器为什么要对长轮询等待时间进行校验了吧？  
  
好了，现在我们已经对 ReceiveMessageActivity 活动处理器进行了三方面的分析，知道了在 ReceiveMessageActivity 活动处理器中要执行三个重要操作： 第一就是校验消费者请求是否为顺序消费请求，第二就是根据请求超时时间修正请求长轮询等待时间，第三就是执行目标队列 Id 为 -1 的负载均衡操作 ，了解了这些内容之后，接下来我们就可以按照这个逻辑来实现 ReceiveMessageActivity 活动处理器了，请看下面代码块。  
上面代码块中的内容虽然很多，但是注释非常详细，并且之前我已经跟大家详细分析了 ReceiveMessageActivity 活动器中要执行的操作，所以我就不再和大家重复解释上面代码块的内容了，只有一点需要跟大家强调， 那就是大家一定要记住请求的不可见时间这个参数，并且要梳理清楚，这个时间是从消费者客户端传递过来的， 在不久的将来，我们就要用到这个重要参数。  
  
引入 ConsumerProcessor 和 PopMessageProcessor 处理器  
  
好了朋友们，ReceiveMessageActivity 请求活动器的内容就告一段落，要继续往下进行了。而且我也相信大家已经从 ReceiveMessageActivity 活动器的 receiveMessage() 方法中看到了，从客户端请求中解析出来的这些内容最后都交给消息处理器处理了，这和 Proxy 节点处理生产者客户端生产消息请求的逻辑是一样的，所以接下来，我们就要来到 MessagingProcessor 消息请求处理器中，看看它又是怎么进一步处理这些内容的。我已经把消息请求处理器重构完毕了，请看下面代码块。  
从上面代码块中可以看到，就像生产者客户端生产消息的请求需要特定的 ProducerProcessor 生产者处理器来处理，消费者客户端发送的消费消息的请求也需要特定的消费者处理器处理，所以我们又给 Proxy 节点引入了 ConsumerProcessor 消费者处理器，而在 DefaultMessagingProcessor 默认消息处理器的 popMessage() 方法中，从消费者客户端请求中解析出的内容最终又交给了刚刚定义的 ConsumerProcessor 消费者处理器处理了，所以接下来我们就要继续看一看，这个 ConsumerProcessor 消费者处理器要执行什么操作。  
  
其实到这里问题的答案已经很明朗了，Proxy 节点肯定不会真正处理客户端请求，而是把请求转发给 Broker 节点处理，而 ConsumerProcessor 消费处理器又是 Proxy 内部的处理器，显然这个处理器肯定也不会真正处理客户端信息，它要做的不过是把消费者客户端发送的请求内容转发给 Broker 节点而已。当然，具体执行到何种程度还有待商榷。这个时候我们不妨看看 ProducerProcessor 生产者处理器是怎么处理生产客户端发送的请求的，如果大家回顾了对应的代码之后， 就会发现 ProducerProcessor 生产者处理器内部执行的操作非常简单，其实就是创建了一个 RemotingCommand 通信对象的请求头，然后把客户端信息封装到了请求头中，而这个 RemotingCommand 通信对象，就会被 Proxy 节点发送给 Broker 节点，一旦执行了这一步，就意味着 Proxy 节点把客户端的请求内容转发给了 Broker 节点 。很好，了解了这一点之后，那么实现 ConsumerProcessor 消费者处理器时，只需要照葫芦画瓢即可， 我们也只需要在该处理器内部创建 RemotingCommand 通信对象的请求头，把消费者客户端的请求内容封装到请求头内部即可，接下来只要把请求头向内部组件继续传递即可 。接下来就请大家看看我实现完毕的 ConsumerProcessor 消费处理器，请看下面代码块。  
从上面代码块中可以看到，在 ConsumerProcessor 消费处理器内部创建了一个 PopMessageRequestHeader 请求头对象，然后把消费者客户端请求内容封装到该请求头中了，然后又把请求头交给服务管理器中对应的服务组件去处理了，所谓的服务组件肯定就是 LocalMessageService 对象，因为之前 ProducerProcessor 生产者处理器就是把请求头交给 LocalMessageService 去进一步处理了。接下来我就不卖什么关子了，就直接给大家展示 LocalMessageService 重构之后的内容了，请看下面代码块。  
上面代码块展示的内容非常简单，可以看到，LocalMessageService 本地消息服务组件在执行 popMessage() 方法的时候，无非就是使用之前创建完毕的请求头创建了 RemotingCommand 通信对象，然后把 RemotingCommand 通信对象给 Broker 节点处理了，到此为止，消费者客户端的请求才算是真正传递给了 Broker 节点。当然， 因为我们目前使用的是本地模式部署 Proxy 节点，Broker 和 Proxy 部署在同一进程，所以在 Proxy 节点内部就可以直接得到 Broker 节点的 BrokerController 控制器对象，然后通过 BrokerController 控制器得到 Broker 节点专门处理 Pop 消费者客户端消费消息请求的 PopMessageProcessor 处理器，让这个处理器真正处理消费者客户端的请求 ，这个逻辑展示得非常清楚了吧？  
  
当然，在第二十版本代码中，我并没有给大家把 Broker 节点的 PopMessageProcessor 请求处理器真正实现了，只是把它定义出来了，就像下面代码块展示的这样，请看下面代码块。  
可以看到，PopMessageProcessor 请求处理器处理请求的 processRequest() 方法，根本就没有被我实现，这是第二十一版本代码的内容，就留到下一章讲解吧。至于 PopMessageProcessor 请求处理器是怎么创建的，以及又是怎么注册到 Broker 节点的服务端内部，这些内容就留给大家去 Broker 节点的 BrokerController 控制器中查看吧，内容非常简单，都是在重复以前已经实现的功能，所以我就不在文章中讲解了。好了朋友们，本章内容就到此结束了，现在大家已经可以阅读我提供的第二十版本代码的所有内容了，也可以启动测试类，梳理一下程序的执行流程，大家可以在 PopMessageProcessor 请求处理器处理请求的 processRequest() 方法输出一些请求信息，让它打印在控制台上，以此验证程序执行流程的正确性。下一章就要开始真正实现 Broker 节点处理消费者客户端消费消息请求的功能了，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/bltgewedfybnkwtr*  
*All content belongs to its respective owners and creators.*