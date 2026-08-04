上一章结尾我跟大家留了 5 个问题，分别是：  
1 服务端怎么给接收到的每一个客户端连接建立唯一连接 Id 呢？  
2 服务端想给客户端 B 主动发送消息时，怎么就知道客户端 B 的连接 Id 是多少呢？  
3 存放客户端连接 Id 和对应的连接对象的 map 究竟是什么呢？难道就只是一个普通的 map 吗？这个 map 应该定义在哪里呢？  
4 客户端和服务端建立的连接通道，也就是所谓的连接对象，究竟是什么呢？  
5 客户端的连接 Id 和对应的连接对象在什么时候被放到 map 中呢？  
以上就是上一章结尾我提出的几个问题，当然，除了这 5 个问题，还有一个非常重要的功能等待我们去实现， 那就是为 grpc 服务端实现处理双向流消息的功能 。这一章我就为大家把这几个问题依次解决了，当然，随着这几个问题的解决，肯定会为 nacos 服务端引入很多新的组件，这也没什么可担心的，我们一起实现就好。  
  
引入 AddressTransportFilter 过滤器，创建客户端连接 Id  
  
首先让我们来看第一个问题：服务端怎么给接收到的每一个客户端连接建立唯一连接 Id？这个我就直接讲解吧，因为客户端连接 Id 的创建，是 grpc 框架实现的，别忘了我们之前构建的 nacos 的客户端和服务端，其内部都使用了 grpc 框架， 仅仅从网络通信的功能来看，nacos 的客户端和服务端其实都是一层外壳，内部真正发挥作用的是 grpc 框架的客户端和服务端，真正能感知到 grpc 客户端访问的也只能是 grpc 服务端 。既然是这样，只要 grpc 服务端发现接收到了一个新的客户端连接，就立刻为这个客户端连接创建一个唯一 Id 不行就了？很显然这就是 grpc 服务端的工作。 所以在 nacos 服务端源码中，定义了一个 grpc 框架的过滤器，也就是标题中展示的 AddressTransportFilter 过滤器，这个过滤器中定义了一个 transportReady 方法，并且过滤器中的 transportReady 方法会在服务端接收到客户端连接时被调用。而创建客户端连接 Id 的操作，就定义在了过滤器的 transportReady 方法中 。我已经把 AddressTransportFilter 过滤器实现了，并且加上了详细的中文注释，展示在下面了，请看下面代码快。  
package com.cqfy.nacos.core.remote.grpc;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/10

\* @方法描述：在第七版代码本要用到的grpc过滤器

\*/

public class AddressTransportFilter extends ServerTransportFilter {  
  
  
//这个方法实际上会在grpc框架中被调用，当服务端接收到客户端连接的时候，该方法就会被调用，就是在该方法中，服务端为客户端连接生成了唯一的客户端连接Id

//方法参数transportAttrs对象就是grpc框架中的组件，这个组件中封装着当前接收到的客户端连接的相关信息

@ Override

public Attributes transportReady (Attributes transportAttrs) {

//得到封装了客户端网络地址的对象

InetSocketAddress remoteAddress \= (InetSocketAddress) transportAttrs.get (Grpc.TRANSPORT\_ATTR\_REMOTE\_ADDR);

//得到本地地址

InetSocketAddress localAddress \= (InetSocketAddress) transportAttrs.get (Grpc.TRANSPORT\_ATTR\_LOCAL\_ADDR);

//得到客户端发送消息端口号

int remotePort \= remoteAddress.getPort ();

//得到本地端口号

int localPort \= localAddress.getPort ();

//得到客户端IP地址

String remoteIp \= remoteAddress.getAddress ().getHostAddress ();

//在这里创建了一个Attributes对象，一定要记住这个Attributes对象

//并且把客户端的信息都设置到这个连接对象中了

Attributes attrWrapper \= transportAttrs.toBuilder ()

//设置客户端连接Id，key就是ATTR\_TRANS\_KEY\_CONN\_ID，value就是Id，可以看到，所谓的客户端连接ID其实就是当前时间戳，加上客户端IP地址，加上端口号拼接在一起组成的

.set (ATTR\_TRANS\_KEY\_CONN\_ID,System.currentTimeMillis () + "\_" + remoteIp + "\_" + remotePort)

//设置客户端IP

虽然我把这个 AddressTransportFilter 过滤器实现出来了，但我相信肯定有很多朋友不知道这个过滤器的具体使用方法。说实话，关于这个过滤器我也没想要讲太多，因为讲得太多肯定就深入到 grpc 框架内部了。就比如说，我要是详细讲解这些过滤器对象在 grpc 框架内部是怎么创建的，又被存放到哪里，在哪里发挥作用等等，肯定就要引入 grpc 框架的很多组件，比如 NettyServerHandler、ServerImpl、ServerTransportListenerImpl、NettyServerTransport 等等，这些根本不是 nacos 框架的内容，所以大家只需要知道这个 AddressTransportFilter 过滤器对象的作用以及其内部方法被调用的时机即可。  
  
好了，现在让我再来为大家讲解一下 AddressTransportFilter 过滤器的具体使用方法：首先可以看到这个 AddressTransportFilter 类继承了 ServerTransportFilter 类，ServerTransportFilter 就是 grpc 框架内部的一个过滤器抽象父类，AddressTransportFilter 继承了 ServerTransportFilter 类，这也就意味着我定义的 AddressTransportFilter 类的对象就是 grpc 框架要使用的过滤器对象。这个过滤器对象被使用的时机也很清楚， 每当 grpc 服务端接收到客户端连接时，或者说服务端和客户端的传输通道准备完毕之后，我定义的这个 AddressTransportFilter 过滤器对象中的 transportReady 方法就会被回调。注意，这里我要强调一下，这个处理器中的方法只会在客户端和服务端连接建立时被回调，如果是客户端向服务端发送请求，这个过滤器中的方法是不会被回调的，这一点大家一定要记住！在该方法回调的时候，会把服务端当前接收到的客户端的连接的相关信息封装到一个 Attributes 对象中，然后把这个 Attributes 对象传递到过滤器的 transportReady 方法中。这样一来，过滤器对象就可以在自己的 transportReady 方法中创建该客户端的连接 Id 。就在上面代码块的第 35 行， 使用 System.currentTimeMillis() + "\_" + remoteIp + "\_" + remotePort 这行代码创建了客户端连接 Id 。也就是说，客户端连接 ld 是由系统当前时间戳+客户端 IP 地址+端口号创建的。  
  
当然，创建唯一的客户端连接 Id 只是过滤器对象 transportReady() 方法中的一个操作， 在 transportReady() 方法中，最重要的操作就是把和当前接收到的客户端连接的相关信息，或者说是把和当前服务端建立连接的客户端的重要信息都封装到一个 Attributes 对象，然后把 Attributes 对象返回 。在过滤器中创建的这个 Attributes 对象中封装了客户端的 IP 地址，端口号，客户端连接 Id 以及本地端口号，那么创建好的这个对象会返回给谁呢？我相信这是目前大家最关心的问题， 实际上创建好的这个 Attributes 对象会交给 NettyServerHandler 中的 Attributes 成员变量，这一点要从 grpc 框架源码中才能看到 。那么 NettyServerHandler 又是什么呢？关于这个 NettyServerHandler 处理器，我倒是可以跟大家简单讲一讲了。  
  
从名字上就能知道 NettyServerHandler 是一个处理器，并且是 netty 要使用的一个处理器。也确实是这样，grpc 底层使用 netty 构建了服务端，而 netty 是我们非常熟悉的框架，netty 构建的服务端，每节收到一个客户端连接，都要创建一个 SocketChannel。在创建好 SocketChannel 之后，还要初始化这个 SocketChannel，向这个 SocketChannel 的 ChannelPipeline 中添加 Handler 处理器，我相信大家对这些知识已经非常熟悉了，应该都清楚具体的流程，所以我就不再带大家回顾了。 我刚才提到的 NettyServerHandler，就是 grpc 框架服务端定义的一个处理器组件，这个处理器就会添加到当前创建的客户端连接的 SocketChannel 的 ChannelPipeline 中 。也就是说， 每一个客户端的 SocketChannel 都会使用 NettyServerHandler 处理从客户端发送过来的消息，而在处理的过程中，就可以通过 NettyServerHandler 对象获得封装了和当前发送消息的客户端相关信息的 Attributes 对象 。那么得到这个 Attributes 对象之后呢？我们可以从这个 Attributes 对象中获得当前交互的客户端的信息，那么得到这些信息之后呢？  
  
在引入了 AddressTransportFilter 过滤器，成功创建了客户端连接 Id，并且把和客户端相关的信息都封装到了 Attributes 对象中之后，我们目前所做的所有努力只实现了一个功能， 那就是让 grpc 框架可以随心所欲地获得每一个客户端的信息了。反正每一个客户端向服务端发送消息的时候，都会被各自对应的 SocketChannel 接收消息并处理，当使用 NettyServerHandler 处理消息时，就可以从 NettyServerHandler 中获得当前 SocketChanel 对应的客户端的相关信息以及客户端连接 Id(我这里讲解的只是一个大概流程，实际上在 grpc 框架中还有很多其他组件都参与到这个过程中了，NettyServerHandler 也不是直接处理客户端发送过来的消息的对象) 。就目前实现的功能来看，程序执行这些操作是没有问题的。但是，请大家想一想，我们最初创建客户端连接 Id 的目的是什么呀？肯定是为了在 nacos 服务端中使用，对吧？因为我们最终使用的是 nacos 注册中心的服务端，并不是 grpc 这个框架，所以肯定要在 nacos 服务端中得到客户端连接 Id，然后根据客户端连接 Id 得到对应的客户端和服务端之间的连接对象，这样一来，nacos 服务端就可以直接使用连接对象给 nacos 客户端主动发送消息了。但现在我们只是把客户端的信息封装到 Attributes 对象中了，Attributes 对象又赋值给了 NettyServerHandler 的 Attributes 类型的成员变量，兜兜转转，现在所有的客户端信息都在存放在 grpc 框架自己手中，那怎么从 grpc 框架中把客户端的信息交给 nacos 框架使用呢？这时候就又轮到一个新的组件登场了，那就是 grpc 拦截器。  
  
引入 GrpcConnectionInterceptor 拦截器，实现数据向 nacos 转移  
  
没错，我们要引入的一个新的组件就是 GrpcConnectionInterceptor 拦截器对象。这个 GrpcConnectionInterceptor 拦截器也是 grpc 框架内部的组件，GrpcConnectionInterceptor 类实现了 grpc 框架中的 ServerInterceptor 接口，所以我们自己定义的 GrpcConnectionInterceptor 拦截器对象也可以被 grpc 框架使用。GrpcConnectionInterceptor 拦截器和刚才定义的 AddressTransportFilter 过滤器对象的不同之处就在于： 过滤器对象只会在客户端和服务端连接建立或者断开的时候被使用，而除了客户端连接建立和断开的情况，拦截器对象中的方法还会在客户端每次向服务端发送请求时被调用 。这一点大家一定要记清楚， 还有一点要补充的就是拦截器和过滤器同时被使用时，过滤器首先会被使用，然后才会使用拦截器 。这两点补充完毕就可以为大家展示 GrpcConnectionInterceptor 拦截器的具体内容了，等展示完了我在给大家讲解其中的逻辑。因为拦截器中用到了 grpc 框架的一些组件，大家都没见过，我直接讲解大家也不明白，还是先看代码再讲解吧。请看下面代码快。  
上面的拦截器展示完毕之后，如果大家仔细看过了，肯定会有一些疑问，比如说拦截其中定义了一个 interceptCall 方法，这个方法就是触发拦截器工作时要回调的方法，而这个方法中有一些方法参数， 其中最引人注目的肯定是第一个方法参数，也就是 ServerCall 这个对象。这个对象之所以引人注目，是因为在拦截其中要想获得发送请求过来的当前客户端的信息，都要从 ServerCall 这个对象中获得 。我把对应的代码展示在下面代码块中了，请看下面代码块。  
从上面代码块中可以看到，首先从 ServerCall 对象中获得了一个 Attributes 对象，然后又从 Attributes 对象中获得了客户端的相关信息。这一幕似曾相识吧？在我们刚才创建的过滤器对象的方法中，不就是创建了一个 Attributes 对象，然后把客户端相关信息封装到 Attributes 对象中了吗？没错，我可以很负责任的告诉大家， 在拦截器的 interceptCall 方法中，从方法参数 ServerCall 中获得的 Attributes 对象，就是之前在过滤器中创建的那个 Attributes 对象 。至于拦截器是怎么获得过滤器创建的 Attributes 对象的，这里就不细说了，如果要详细解释，还要把 grpc 框架的 ServerCallImpl、NettyServerStream 等等都讲解了，那就太麻烦了，还是那句话，大家只需要知道拦截器的使用方法和作用即可。  
  
好了，现在我们已经可以在拦截器的 interceptCall 方法中得到当前发送请求过来的客户端的相关信息，那么得到这些信息之后，拦截器执行了一个什么操作呢？想必大家也注意到拦截器的 interceptCall 方法创建了一个 Context 对象，并且把客户端的相关信息都设置到 Context 对象中了。现在我可以跟大家解释清楚了， 这个 Context 上下文对象确实会封装客户端的相关信息，然后拦截器的 interceptCall 方法会执行一个非常重要的操作，那就是使用 ThreadLocal 把创建好的 Context 对象存放到线程的私有 map 中 。这样一来， 只要是在 grpc 服务端处理请求的整个生命周期，都可以从处理请求的线程的私有 map 中获得发送请求过来的客户端的相关信息，nacos 服务端当然也可以直接从处理请求的线程中随时获得对应的 Context 对象，也就可以随时获得发送请求过来的客户端的相关信息了 。接下来我会给大家展示一段代码，请大家看一看我们新引入的这个拦截器是怎么把创建好的 Context 对象设置到线程的私有 map 中的。 发起这个操作的代码就是 GrpcConnectionInterceptor 过滤器的 interceptCall 方法的最后一行代码，也就是 Contexts.interceptCall(ctx, call, headers, next) 。这里可以看到调用了一个 Contexts 对象的 interceptCall 方法，方法的第一个参数就是封装了客户端相关信息的 Context 对象。那么接下来，就请大家看一下 Contexts 对象的 interceptCall() 方法，该方法是 grpc 框架中的，大家不用特意去查看 grpc 框架，只看我文章中展示的即可。请看下面代码块。  
接下来请大家看一看 Context 的 attach() 方法，请看下面代码块。  
接下来就请大家 ThreadLocalContextStorage 类的 doAttach() 方法，请看下面代码块。  
到此为止，我就为大家把 grpc 框架的过滤器和拦截器实现完毕了，这两个组件展示完了之后，现在大家应该清楚了 nacos 服务端是怎么为每一个客户端创建唯一连接 Id，并且是怎么把客户端的相关信息封装起来，然后交给 nacos 服务端去使用的。如果这个流程大家都清楚，那么文章一开始提出的五个问题中的第一个问题，就已经得到完美解决了。接下来就应该解决第二个问题了，那就是 服务端想给客户端 B 主动发送消息时，怎么就知道客户端 B 的连接 Id 是多少呢 ？当然——我先把话题岔开一下—— 随着过滤器和拦截器的实现，大家肯定会好奇这两个组件怎么才能在 grpc 服务端发挥作用呢？换句话说，这两个组件该怎么设置到 grpc 服务端中呢 ？还是一如既往的做法，问题太多就暂时放一放，现在还不到跟大家实现这个功能的时候，这是下一章的内容，大家可以暂时记住这个问题，等下一章我再来为大家实现这个功能。还是让我们回到第二个问题上来吧，这个问题确实很重要，但很可惜，和刚才的过滤器和拦截器组件一样，现在我还无法跟大家解释清楚，因为这个也是后面两章的内容，在引入了 ClientServiceIndexesManager 组件之后，我们才能回过头来探讨这个问题，并且解决这个问题。所以，就先跳过第二个问题吧，好在就算当前跳过这个问题，对我们解决后面 3 个问题也不会造成什么影响。好了，接下来就先让我们一起看看第三个问题吧。  
  
引入 ConnectionManager 和服务端 GrpcConnection  
  
第三个问题关心的是存放客户端连接 Id 和对应的连接对象的 map 的本质， 存放客户端连接 Id 和对应的连接对象的 map 究竟是什么呢 ？当然，第三个问题和第四个问题是相关联的，因为 map 中存放的 key 是客户端连接 Id，这个我们已经清楚了，而 value 就是客户端和服务端建立的连接对象，那么 客户端和服务端建立的连接通道，也就是所谓的连接对象，究竟是什么呢 ？只有知道了这个连接对象是什么，才能进一步分析存放键值对的 map 究竟是什么。所以，这一小节我们就把第三个问题和第四个问题连起来看，把这两个问题一起解决了。  
  
接下来我们先来分析分析，服务端和客户端建立的这个连接对象究竟是什么。其实这个也很好分析，我想大家应该都还记得， 在我们构建 nacos 客户端的时候，客户端和服务端连接建立成功之后，就为 nacos 客户端创建了一个 GrpcConnection 连接对象 。具体操作就在 GrpcClient 类的 connectToServer 方法中，我把具体的代码展示在下面了，请看下面代码块。  
在上面代码块中的第 60 行，创建了一个 GrpcConnection 连接对象。如果大家 GrpcConnection 连接对象还有印象，就肯定还记得，这个 GrpcConnection 连接对象就是 nacos 客户端用来向服务端发送消息的对象。我把 nacos 客户端的 GrpcConnection 连接对象的部分代码也搬运到这里了，请看下面代码块。  
上面代码块展示的就是 nacos 客户端的 GrpcConnection 对象，nacos 客户端想发送消息到服务端时，直接调用 GrpcConnection 连接对象的 request() 方法，就可以把消息发送给服务端了。这些都是旧知识了，我想大家应该很容易就能理解。  
  
很好，如果大家已经彻底回忆起来 nacos 客户端的 GrpcConnection 对象，知道这个对象就是用来向 nacos 服务端发送消息用的，那么现在我们构建了 nacos 的服务端，服务端也提供双向流通信模式，那假如 nacos 的服务端想随时发送消息到客户端，这时候该怎么办呢？我相信大家肯定都已经猜到了，这很好办， 只要在服务端接收客户端连接的时候，也创建一个对应的 GrpcConnection 对象，在这个 GrpcConnection 对象中定义一个 request() 方法，那么 nacos 的服务端想和哪个客户端发送消息，只要获得这个为这个客户端连接创建的 GrpcConnection 对象，不就可以直接向这个客户端发送消息了吗 ？这时候关于刚才的第四个问题，也就是服务端和客户端建立的连接对象究竟是什么，也就水落石出了， 其实就是一个 GrpcConnection 对象，这个 GrpcConnection 对象是 nacos 服务端持有的，并且服务端会为每一个客户端连接都创建一个对应的 GrpcConnection 对象 。那么这个连接对象该怎么定义呢？很简单，就先模仿客户端的 GrpcConnection 对象定义好了。客户端的 GrpcConnection 对象中不是有一个 Channel 成员变量，还有一个用来发送消息的存根对象吗？那么我在定义服务端的 GrpcConnection 类的时候， 也向这个类中定义一个 Channel 成员变量，然后再定义一个可以主动向客户端发送消息的流式对象，大家千万别忘了，使用双向流模式才能主动向 nacos 客户端发送消息 。接下来，就请大家看一看我目前实现的 nacos 服务端为每一个客户端连接创建的 GrpcConnection 对象，请看下面代码块。  
以上代码块展示的 GrpcConnection 类就是 nacos 服务端为每一个接收到的客户端连接创建的连接对象。 只要 nacos 服务端想和哪个客户端发送消息，就可以直接获得该客户端对应的 GrpcConnection 对象，然后调用 GrpcConnection 对象的 sendRequestNoAck() 方法，直接把消息发送给客户端即可 。我知道随着服务端 GrpcConnection 连接对象的实现，大家肯定也会对几个问题感到困惑。 首先大家可能会觉得我怎么就把一个 Channel 定义在服务端的 GrpcConnection 类中了 ？这里我解释一下， GrpcConnection 类中的 Channel 成员变量其实就是 netty 中服务端为这个客户端创建的 SocketChannel 通道，它会在某个时刻被 netty 服务端创建的 SocketChannel 赋值 ；除此之外，大家也可以从 GrpcConnection 类的 close()、isConnected() 方法中看到 Channel 在发挥作用，因为说到底 grpc 底层使用的是 netty，通信的通道也是通过 netty 构建的，最终关闭还是要关闭最底层的 Channel，这个大家了解就好，不必深究。至于 GrpcConnection 类中的 Channel 会在什么时候被赋值，下一章大家就知道了，现在大家应该清楚，在拦截器创建 Context 对象的时候，已经把客户端对应的 SocketChannel 设置到 Context 对象中了，这个大家应该都还记得吧？  
  
除此之外，大家可能还会对 GrpcConnection 类的另一个成员变量感到困惑， 也就是对 StreamObserver 流式对象感到困惑。别忘了，到此为止我们还没有为 grpc 服务端实现处理双向流消息的功能，双向流模式都没有引入呢，那这个可以主动向客户端发送消息的流式对象是从哪传递过来的呢 ？还是那句话，暂时把这个问题放到一遍，先等一等，等时机成熟的时候我再回过头为大家解开这里的困惑。  
  
好了，我们接着往下看，这时候就可以解决第三个问题了， 那就是客户端的连接 Id 和对应的连接对象，也就是 GrpcConnection 对象的 map 究竟是什么呢 ？现在我就可以为大家揭开它神秘的面纱了。确实，客户端连接 Id 和该客户端连接对应的 GrpcConnection 对象确实存放在一个 map 中，实际上这个 map 中存放了很多客户端的键值对信息。nacos 服务端不可能只接收一个客户端连接吧？ 肯定会有多个客户端都来访问 nacos 注册中心服务端，这些客户端不是注册服务实例信息，就是订阅相关服务实例信息，每一个和服务端建立连接的客户端，这个客户端的连接 Id 和对应的 GrpcConnection 对象都要存放到同一个 map 中 。但是， 这个 map 不是单独存在的，实际上在 nacos 服务端源码中有一个非常重要的组件，那就是 ConnectionManager 连接管理器，而我们刚才一直重复的 map，就是这个 ConnectionManager 连接管理器中的成员变量 。好了，现在我终于为大家把服务端非常核心的组件 ConnectionManager 连接管理器引入了。那么这个 ConnectionManager 连接管理器该怎么定义呢？就目前我们讲解得内容来说，这个连接管理器中似乎只要定义一个 map 成员变量就行了。也就是下面代码块展示的这样，请看下面代码块。  
好了，现在我就把 ConnectionManager 连接管理器也实现了，可能有朋友会觉得现在实现得这个 ConnectionManager 连接管理器没一点用啊，光定义了存放连接对象的 map 并没有用，主要是定义注册连接管理器和获得连接管理器的方法啊，所谓注册连接管理就是把客户端连接 Id 和对应的 GrpcConnection 连接对象存放到 map 中的方法。这个其实很简单，那我直接在连接管理器中定义两个方法就行了，一个就定义为 register() 方法，一个就定义为 getConnection() 获得连接对象的方法。请看下面代码块。  
好了，现在我就把 ConnectionManager 连接管理器的内容稍微丰富了一下，这个 ConnectionManager 连接管理器，就是我们目前需要的组件了。到此为止，文章刚开始列出的 5 个问题中，第一个、第三个、第四个都解决完毕了。接下来就该解决第五个问题了，也就是 客户端的连接 Id 和对应的连接对象在什么时候被放到 map 中呢 ？现在再来看这个问题，我相信大家的思路可能会稍微清楚一些了，可能有的朋友会意识到，这虽然是一个问题，但是这个问题实际上隐藏了另外两个问题， 那就是服务端为客户端连接创建的 GrpcConnection 连接对象究竟该在哪里创建呢 ？还有一个问题就是 ConnectionManager 连接管理器对象应该在哪里被创建呢？这样问似乎有些不妥当，因为通过上面代码块我们可以看到，ConnectionManager 连接管理器添加了 springboot 的 @Service 注解，所以连接管理器会被 springboot 容器管理，被 springboot 创建， 那么这个 ConnectionManager 连接管理器对象应该在哪里被使用呢 ？因为只有 GrpcConnection 连接对象创建了，才能被注册到 ConnectionManager 连接管理器中。所以现在我们又多出来两个问题要解决。但是，这一章的内容显然已经够多了，很多问题无法解决完毕，问题总是一个接着一个，似乎没有尽头，所以只好等下一章再解决了。  
  
梳理本章的内容  
  
虽然本章的核心内容已经结束了，但是本章的内容非常多，引入的组件和未解决的问题比较多，所以现在我想为大家梳理一下本章的内容，然后整理一下未解决的问题，这些问题都是后面章节要重点关注的问题。首先先让我们一起来看看本章引入了什么新的组件，这些组件又解决了哪些问题。  
1 引入了 AddressTransportFilter 过滤器组件，这个 过滤器组件创建了客户端的连接 Id，并且把客户端的相关信息封装到一个 Attributes 对象中了。  
2 引入了 GrpcConnectionInterceptor 拦截器组件，这个拦截器组件把客户端相关信息封装到了一个 Context 对象中，并且可以让 nacos 服务端随时从处理请求的线程中获得这个 Context 对象，使用对应客户端的相关信息。  
3 引入了 GrpcConnection 连接对象，服务端会为每一个客户端连接创建 GrpcConnection 连接对象，有了这个连接对象，nacos 服务端就可以随时向对应的客户端发送消息了。  
4 引入了 ConnectionManager 连接管理器，这个连接管理器的引入，我们知道了每一个客户端的连接 Id 和对应的 GrpcConnection 连接对象都会注册到连接管理器中 。  
以上就是本章的核心内容，那么本章结束之后，我们还有哪些问题没解决呢？  
1 虽然引入了过滤器和拦截器组件，但我们不知道如何让这两个组件发挥作用，不知道怎么把它们设置到 grpc 的服务端中。  
2 虽然创建了客户端连接 Id，也知道了连接对象是什么，但我们还不知道服务端在哪里为接收到的每一个连接创建 GrpcConnection 连接对象，也不知道在哪里把客户端连接 Id 和对应的 GrpcConnection 连接对象注册到 ConnectionManager 连接管理器中。  
3 GrpcConnection 类的 StreamObserver 成员可能会让大家感到困惑。因为我们还没有为 grpc 服务端实现处理双向流消息的功能，双向流模式都没有引入呢，那这个可以主动向客户端发送消息的流式对象是从哪传递过来的呢？  
其实看了第三个未解决的问题，我相信在大家脑海里会有一种感觉，那就是以上几个问题的解决肯定和 grpc 服务端处理双向流消息功能的实现有关，因为只有引入了双向流模式，grpc 服务端才能主动向客户端发送消息，发送消息时会用到为客户端创建的 GrpcConnection 连接对象，而连接对象中的 StreamObserver 成员变量可能就是引入了双向流模式之后被流式对象赋值的。也许 GrpcConnection 连接对象就是在双向流组件处理客户端消息的过程中被创建的，创建出来就被注册到连接管理器中了。当然，这些还都是我们的猜测，但我想告诉大家的是，在下一章我为 grpc 服务端实现了双向流处理客户端消息的功能后，本章未解决的问题就全部解决了，大家会看到在双向流模式中，本章引入的几个组件是如何串联起来，协同工作的。  
  
当然，在最后我们也不能忘记了其实还有另外两个问题我们没有解决：那就是上一章我为大家实现的 GrpcRequestAcceptor 单向流请求接收器的功能并没有实现完整；第二个问题就是本章被我们跳过的问题， 服务端想给客户端 B 主动发送消息时，怎么就知道客户端 B 的连接 Id 是多少呢 ？大家可以牢牢记住这两个问题，后面我会引入相关组件，实现相关功能，为大家解决这两个问题。好了朋友们，本章的内容就到此结束了，我们下一章见！等下一章结束之后，大家就可以去我提供的第七版本代码中查看相应内容了。  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/nnoyltmgtnln8i6g*  
*All content belongs to its respective owners and creators.*