还是老样子，在本章内容开始之前，先把上一章遗留的几个问题展示一下：  
1 虽然引入了过滤器和拦截器组件，但我们不知道如何让这两个组件发挥作用，不知道怎么把它们设置到 grpc 的服务端中。  
2 虽然创建了客户端连接 Id，也知道了连接对象是什么，但我们还不知道服务端在哪里为接收到的每一个连接创建 GrpcConnection 连接对象，也不知道在哪里把客户端连接 Id 和对应的 GrpcConnection 连接对象注册到 ConnectionManager 连接管理器中。  
3 GrpcConnection 类的 StreamObserver 成员可能会让大家感到困惑。因为我们还没有为 grpc 服务端实现处理双向流消息的功能，双向流模式都没有引入呢，那这个可以主动向客户端发送消息的流式对象是从哪传递过来的呢？  
4 之前为大家实现的 GrpcRequestAcceptor 单向流请求接收器的功能并没有实现完整 。  
5 服务端想给客户端 B 主动发送消息时，怎么就知道客户端 B 的连接 Id 是多少呢 ？  
以上 5 个问题就是我们尚未解决的问题，也是我们本章要解决的重点问题。随着这些问题的解决，很多新的组件也会不断引入到我们的程序中，我们的程序也会不断重构完善，直到和 nacos 框架源码一致。接下来，就让我们着手解决这些问题吧。  
  
引入 GrpcBiStreamRequestAcceptor 双向流消息接收器  
  
这一章我并不打算从第一个问题开始解决，因为在上一章结尾我已经跟大家稍微透露了一些重点内容： 那就是随着 grpc 服务端实现了双向流处理客户端消息的功能，这些未解决的问题几乎就全都可以解决了，大家也会看到在双向流模式中，我们之前引入的这几个组件是如何串联起来，协同工作的 。所以，这一章我打算直接就为 grpc 服务端实现处理客户端双向流消息功能，也许真的会向我说得这样，随着双向流功能的实现，所有问题都迎刃而解了。  
  
在前面我们已经为 grpc 服务端实现了处理客户端单向流请求的功能，因为客户端单向流请求调用的目标方法就是 request 方法，所以我们在 grpc 服务端定义了一个单向流请求接收器，也就是 GrpcRequestAcceptor 组件。现在我们要为 grpc 服务端实现处理客户端双向流的功能，其实就仿照着单向流接收器来实现就行。 我们已经知道了客户端发送双向流的目标方法是 requestBiStream 方法，那就可以直接为 grpc 服务端定义一个双向流消息接收器，然后在这个接收器中实现 requestBiStream 方法。比如我们就可以把这个双向流消息接收器定义为 GrpcBiStreamRequestAcceptor，也就是流式请求接收器的意思，然后在这个类中定义提供服务的目标方法 。具体实现请看下面代码块。  
package com.cqfy.nacos.core.remote.grpc;  
  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/15

\* @方法描述：双向流请求接收器，这个接收器主要负责处理服务端接收到的客户端连接，是非常核心的一个组件

\*/

@ Service

public class GrpcBiStreamRequestAcceptor extends BiRequestStreamGrpc.BiRequestStreamImplBase {  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/15

\* @方法描述：该方法是本类最核心的方法，也是处理客户端连接的核心方法

\*/

@ Override

public StreamObserver < Payload > requestBiStream (StreamObserver < Payload > responseObserver) {  
//首先创建一个观察者对象，这个观察者就用来处理客户端发送过来的双向流请求

StreamObserver < Payload > streamObserver \= new StreamObserver < Payload > () {  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/15

\* @方法描述：客户端发送过来双向流消息后，就会被服务端的这个方法接收并处理

\*/

@ Override

public void onNext (Payload payload) {  
在上面的代码块中，可以看到我们刚刚引入的这个 GrpcBiStreamRequestAcceptor 双向流消息接收器继承 grpc 框架的 BiRequestStreamGrpc.BiRequestStreamImplBase 类，这意味着我们定义的这个双向流消息接收器也是 grpc 框架要使用的组件，肯定也要设置到 grpc 的服务端中。 除此之外就是在双向流请求接收器中定义的 requestBiStream() 目标方法了，在这个目标方法中我又定义了一个 streamObserver 流式观察者对象，这个流式观察者对象就是真正用来处理消息的对象 。可以看到，我并没有实现这个流式观察者对象中的三个方法，因为在实现之前，我还有另外的内容想跟大家聊聊。  
  
请大家再次回顾一下，我们构建的 nacos 客户端和 nacos 服务端成功建立连接之后做了什么呢？这个要从 nacos 客户端的 GrpcClient 类的 connectToServer 方法中寻找答案。实际上在我为大家实现 grpc 服务端单向流请求接收器时，就已经带大家回顾过一次了。现在我把 nacos 客户端的相关代码再次搬运到这里，请大家再回顾一下，请看下面代码块。  
从上面的代码块中可以看到，当 nacos 客户端和 nacos 服务端构建连接时，会先通过 serverCheck() 方法向服务端发送一个 ServerCheckRequest 单向流请求，服务端的单向流接收器处理完 ServerCheckRequest 请求后，会回复给客户端一个 ServerCheckResponse 响应。在我为大家实现 grpc 服务端的单向流请求接收器时，就是按照客户端执行的操作实现了对应的流程，因此我们目前实现的 grpc 服务端单向流请求接收器还并不完善，只能处理客户端发送过来的 ServerCheckRequest 请求； 现在要实现 grpc 服务端的双向流消息接收器了，我还是想用原来的方式，也就是先看看 nacos 客户端和服务端构建连接成功之后对服务端执行了什么双向流操作，然后我在 grpc 服务端的双向流接收器中再实现对应的处理不就行了吗 ？所以，现在我想顺着 nacos 客户端的 GrpcClient 类的 connectToServer 方法继续往下看，因为接下来就是 nacos 客户端双向流执行的一些操作，我把相关的代码也搬运过来了，请看下面代码块。  
上面代码块 62 行之后，都是 nacos 客户端执行的双向流的操作，可以看到，nacos 客户端创建了一个 ConnectionSetupRequest 请求，然后使用 nacos 客户端创建的 GrpcConnection 连接对象，调用了该对象的 sendRequest() 方法，把请求发送给服务端了。注意 ，在 nacos 客户端连接对象 GrpcConnection 的 sendRequest() 方法内部，就是使用双向流存根对象把请求发送给 nacos 服务端的，所以我才说这里发送得是一个双向流请求 。既然这个操作流程梳理清楚了，那么接下来我们实现 grpc 服务端双向流消息接收器时就很简单了。我只需要在 grpc 服务端的 GrpcBiStreamRequestAcceptor 接收器中实现对应的操作就行，比如我就可以让双向流消息接收器处理客户端发送过来的 ConnectionSetupRequest 请求即可。这么做完全没有问题，但是，我还是要再说一句，如果我们为 grpc 服务端定义的 GrpcBiStreamRequestAcceptor 双向流消息接收器就只有处理客户端发送过来的 ConnectionSetupRequest 请求的功能，那这个 GrpcBiStreamRequestAcceptor 接收器也未免太没用了。所以，这个 GrpcBiStreamRequestAcceptor 双向流消息接收器肯定不会定义得这么简单 ，处理客户端发送过来的 ConnectionSetupRequest 请求的功能肯定要实现 ，那除此之外，双向流消息接收器还要实现什么其他功能呢？这时候就又要从 nacos 客户端的角度来思考了。  
  
请大家思考一下，就目前我们为 grpc 服务端实现的这两个接收器，一个是单向流请求接收器，一个是双向流消息接收器，不管是哪一个接收器，在实现它们的时候，都是从 nacos 客户端执行的操作出发，然后实现了服务端接收器组件的对应内容。如果我们再观察得仔细一点就会发现，在 nacos 客户端的 GrpcClient 类的 connectToServer 方法中执行的操作都发生在客户端和服务端连接构建完毕之后。也就是说，当 nacos 客户端和服务端的连接成功建立了，客户端就会向服务端执行一些操作， 比如就是发送一个双向流 ConnectionSetupRequest 请求，关于这个请求的作用我在代码中展示的非常详细，就是让客户端告诉服务端自己的一些信息，比如客户端版本号，客户端应用名称以及客户端支持的能力等等 。总之，这个请求是在 nacos 客户端和服务端连接构建成功之后立刻执行的操作，并且这个双向流消息会被 grpc 服务端的双向流消息接收器处理，这样一来，服务端就可以从 ConnectionSetupRequest 请求中获得客户端的信息了。 那 grpc 服务端获得到的客户端信息可以用来做什么呢？当然就是用来创建连接对象了，也就是服务端为每一个客户端创建的 GrpcConnection 连接对象 。  
  
分析到这里，我相信大家已经非常清楚了， 当 nacos 客户端和服务端连接构建成功之后，客户端会向服务端发送一个 ConnectionSetupRequest 双向流请求，grpc 服务端的 GrpcBiStreamRequestAcceptor 双向流消息接收器会从这个请求中获得客户端的一些信息，比如客户端版本号，客户端应用名称以及客户端支持的能力等等。得到了这些客户端的信息之后，grpc 服务端就可以为这个客户端创建连接对象了，然后把客户端 Id 和对应的连接对象存放到 ConnectionManager 连接管理器中 。这也就是说， grpc 服务端为每一个客户端连接创建连接对象的操作，以及把客户端 Id 和对应的连接对象注册到连接管理器的操作，都是在 GrpcBiStreamRequestAcceptor 双向流消息接收器中执行的 。我能想到，看到这里有些朋友可能会觉得困惑，为什么 grpc 服务端必须从客户端发送过来的 ConnectionSetupRequest 请求中获得客户端的附加信息呢？上一章我为大家展示的服务端为客户端连接创建的 GrpcConnection 连接对象好像并不需要这些客户端信息呀， 这个连接管理器似乎只需要两个成员变量，一个就是 netty 创建的客户端 Channel 成员变量，一个就是流式观察者对象 。我把上一章引入的 GrpcConnection 连接对象的代码搬运过来了，请看下面代码块。  
现在我把 GrpcConnection 类的代码展示完毕了，然后我想让大家注意一下，在上面代码块的第 20 行，可以看到 GrpcConnection 类的构造方法的第一个参数是一个 ConnectionMeta 类型的对象，这些代码在上一章就为大家展示了，但是我并没有讲解，直到现在要实现双向流消息接收器了，我才会大家讲解。 实际上这个 ConnectionMeta 对象就是客户端连接的连接元信息对象，这个客户端连接元信息对象就封装了从客户端的 ConnectionSetupRequest 请求中传输过来的信息，还有就是 grpc 过滤器中获得的客户端的连接 Id，IP 地址等等信息 。我就不在文章中展示这个 ConnectionMeta 类的内容了，大家可以去我提供的第七版本代码中查看，这个类的内容非常简单，就是定义了一些成员变量和一些对应的 get、set 方法而已，除此之外，就只有一个刷新客户端连接最新的活跃时间的功能，这个大家也可以先去代码中看看，不看也没关系，后面我也会在文章中为大家实现。  
  
好了，说了这么多，现在终于要实现 grpc 服务端的 GrpcBiStreamRequestAcceptor 双向流消息接收器了，经过刚才一系列分析，现在我对要实现的 GrpcBiStreamRequestAcceptor 双向流消息接收器的思路已经非常明确了。 首先从拦截器创建的 Context 对象中获得客户端的连接 Id、IP 地址、端口号等等信息，再从客户端发送过来的 ConnectionSetupRequest 请求中获得客户端的附加信息，紧接着就可以创建 ConnectionMeta 连接元信息对象，然后为当前客户端连接创建 GrpcConnection 连接对象，把连接对象注册到连接管理器中就行了 。这就是我明确的实现 grpc 服务端 GrpcBiStreamRequestAcceptor 双向流消息接收器的思路。具体实现请看下面代码块。  
到此为止，我就为大家把 grpc 服务端的 GrpcBiStreamRequestAcceptor 双向流消息接收器实现完毕了，上面代码块中的注释非常详细，而且在实现之前我把实现的思路分析得非常透彻，所以我就不再重复讲解上面代码块的内容了，大家可以再仔细品味品味。  
  
如果上面的逻辑大家都清楚了，接下来让我来猜一猜，我想也许有朋友心里又有了新的问题，可能会疑惑：grpc 服务端的单向流请求接收器和双向流消息接收器都是接收器，都可以处理来自客户端的请求，并且 nacos 客户端和服务端构建连接的时候会向这两个接收器发送不同的请求，执行不同的操作，那么，为什么不把创建客户端连接对象，注册客户端连接对象的操作定义在单向流请求接收器中呢？这是因为 nacos 服务端已经为这两个接收定好明确的分工了， GrpcRequestAcceptor 单向流请求接收器主要接收并处理客户端发送的功能请求，比如注册服务实例信息、订阅服务实例信息、取消订阅服务实例信息等等操作，它的功能仅限于此，非常单一，在后面的章节中大家就会体会到这一点；而 GrpcBiStreamRequestAcceptor 双向流消息接收器主要负责向客户端主动发送消息，处理客户端回复的双向流响应，当然还有创建客户端连接对象，以及注册客户端连接对象，当然，主动向客户端发送消息肯定是双向流消息接收器最频繁的操作，比如向客户端发送最新的服务实例信息、最新的配置信息等等 。  
  
好了，这个问题分析完了，接下来就可以回头看看文章一开始列出的 5 个问题了：  
1 虽然引入了过滤器和拦截器组件，但我们不知道如何让这两个组件发挥作用，不知道怎么把它们设置到 grpc 的服务端中。  
2 虽然创建了客户端连接 Id，也知道了连接对象是什么，但我们还不知道服务端在哪里为接收到的每一个连接创建 GrpcConnection 连接对象，也不知道在哪里把客户端连接 Id 和对应的 GrpcConnection 连接对象注册到 ConnectionManager 连接管理器中。  
3 GrpcConnection 类的 StreamObserver 成员可能会让大家感到困惑。因为我们还没有为 grpc 服务端实现处理双向流消息的功能，双向流模式都没有引入呢，那这个可以主动向客户端发送消息的流式对象是从哪传递过来的呢？  
4 之前为大家实现的 GrpcRequestAcceptor 单向流请求接收器的功能并没有实现完整 。  
5 服务端想给客户端 B 主动发送消息时，怎么就知道客户端 B 的连接 Id 是多少呢 ？  
目前来看，这 5 个问题中，有两个已经被解决了，也就是第 2 个和第 3 个问题被解决了，第一个问题还没有解决，并且我在上一章结尾还说过，随着双向流功能的引入，目前引入的所有组件都会被串联起来，协同工作。这个不假，只不过不是在双向流消息接收器中体现出来，而是在 grpc 服务端中体现出来。别忘了，我们目前引入的组件都是为 grpc 服务端引入的，需要把这些组件设置到 grpc 服务端中，这些组件才能发挥作用，一起工作，这也就意味着我们需要重构 grpc 服务端，也就是 BaseGrpcServer 类。  
  
重构 BaseGrpcServer 类，实现完整的 grpc 服务端  
  
其实重构 BaseGrpcServer 类已经没什么可分析的了，BaseGrpcServer 中构建的就是 grpc 服务端，而我们现在引入的组件，过滤器，拦截器，请求接收器等等都是 grpc 服务端要使用的组件，直接把它们设置到 grpc 服务端即可，而这正是 grpc 框架的内容，使用的也是 grpc 框架的编码方式。所以，我就直接给大家把代码展示在下面了，接下来就请大家看看重构之后的 BaseGrpcServer 类是什么样的，请看下面代码块。  
到此为止，BaseGrpcServer 类也重构完毕了，各个组件都设置到 grpc 服务端中了，这也就意味着我们为 nacos 服务端定义的 grpc 服务端终于完整了。 现在 nacos 服务端已经实现了启动、接收客户端连接、为每一个客户端连接创建 GrpcConnection 连接对象、把 GrpcConnection 连接对象注册到 ConnectionManager 连接管理器中、使用单向流和双向流接收器处理客户端消息、可以使用双向流功能随时发送消息给客户端等功能 。看起来已经有点样子了，当然，要实现的功能还有很多，就比如说在本章开始引入的 5 个问题，现在第 1、2、3 个问题都已经解决了，就剩下第 4 个和第 5 个问题没有解决了，让我们再回顾一下这两个问题：  
4 之前为大家实现的 GrpcRequestAcceptor 单向流请求接收器的功能并没有实现完整 。  
5 服务端想给客户端 B 主动发送消息时，怎么就知道客户端 B 的连接 Id 是多少呢 ？  
虽然还有两个问题等待我们解决，但我还是那句话，在 nacos 这个框架中，没有难解决的问题，只有解决不完的问题。我的意思是 nacos 中所有的问题解决起来都很简单，但是随着每一个问题的解决，都会引入新的组件，而这些新的组件又会引入其他问题，最后由一个个问题把 nacos 服务端需要的所有组件都引出来，构成完整的 nacos 服务端。我想从前面几章的内容，大家对这一点已经有所体会了。这一章的内容已经够多了，所以我就不再为大家解决第 4 个问题了，这个问题留到下一章解决。好了朋友们，我们下一章见！  
  
附：这一章看完之后，大家就可以去我提供的第七版本代码中查看对应的代码了，当然有些类的代码可能比文章中展示得更丰富，内容更多，这是因为这些类的内容我还没有在文章中讲解，大家可以先把 grpc 服务端的核心类都看一看，其他的内容等我在文章中讲解了再仔细查看。还有一点，我建议大家一定要把最近几章引入 nacos 服务端的几个组件好好梳理一下，就从服务端启动开始，到服务端接收消息、处理消息结束，看看这中间 nacos 服务端都执行了什么操作，操作流程是怎样的。因为这几章内容确实比较多，实现的功能也很多，大家自己一定要串一下流程，我就不在文章中梳理了，因为梳理下来又要写好多文字，还不如留给大家自己去梳理。我相信如果大家亲自梳理了，并且梳理清楚了，那么这几章的内容大家就全部掌握了，并且会深深印在脑海中了，很长一段时间不会忘记。  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/pnqllxb4xnr5ml7l*  
*All content belongs to its respective owners and creators.*