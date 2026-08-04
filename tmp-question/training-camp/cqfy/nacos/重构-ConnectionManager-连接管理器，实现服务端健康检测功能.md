在上一章最后一小节，我为大家实现了 nacos 服务端的 HealthCheckRequestHandler 健康检测请求处理器，这个处理器专门处理 nacos 客户端发送过来的健康检测请求，并且我也跟大家解释了我实现这个 HealthCheckRequestHandler 健康检测请求处理器的原因。这个处理器实现得非常简单，就是直接给 nacos 客户端回复一个成功响应即可，这没什么可说的，重点在于 nacos 客户端每一次向服务端发送 HealthCheckRequest 健康检测请求，这个单向流请求总会被 nacos 服务端的单向流请求接收器接收并处理，在处理的过程中会刷新该客户端连接对象在服务端的最新活跃时间，具体代码如下，请看下面代码块。  
package com.cqfy.nacos.core.remote.grpc;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/16

\* @方法描述：单向流请求接收器，这个接收器专门处理客户端发送过来的单向流请求

\* 可以看到，这个GrpcRequestAcceptor类上添加了springboot的@Service注解，这就意味着这个请求接收器也是被springboot容器管理的对象

\*/

@ Service

public class GrpcRequestAcceptor extends RequestGrpc.RequestImplBase {  
  
//请求处理器注册表，这个注册表对象内部持有着服务端所有的请求处理器

@ Autowired

RequestHandlerRegistry requestHandlerRegistry;  
  
//连接管理器

@ Autowired

private ConnectionManager connectionManager;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/16

\* @方法描述：本类最核心的方法，服务端接收并处理客户端发送过来的单向流请求的方法

\*/

@ Override

public void request (Payload grpcRequest,StreamObserver < Payload > responseObserver) {  
可以看到， 在上面代码块的第 62 行，执行了 ConnectionManager 连接管理器的 refreshActiveTime() 方法，该方法就会刷新当前发送请求的客户端在服务端的客户端连接对象的最新活跃时间 。只要客户端在 nacos 服务端对应的客户端连接对象的最新活跃定期被刷新，那么客户端和服务端的连接就会一直处于健康状态，客户端和服务端也就可以正常通信。但是，我们目前实现的 ConnectionManager 连接管理器根本就没有实现对应的 refreshActiveTime() 方法，请看下面代码块。  
从上面的代码块中可以看到，我们目前实现的 ConnectionManager 连接管理器确实没有 refreshActiveTime() 方法。因此，肯定要对 ConnectionManager 连接管理器进行重构。上一章结尾我就为大家讲解到这里了，并没有真正重构 ConnectionManager 连接管理器，这一章我们就先来重构 ConnectionManager 连接管理器，实现客户端连接对象最新活跃时间刷新功能。  
  
重构 ConnectionManager，实现客户端连接最新活跃时间刷新功能  
  
我知道在开始重构 ConnectionManager 连接管理器之前，大家肯定还是有些困惑，不知道所谓的客户端连接对象的最新活跃时间是怎么来的，因为我当时为大家引入客户端连接对象，也就是 GrpcConnection 对象时，没有把这个对象给大家展示全面。我们最终定义完毕的 GrpcConnection 客户端连接对象如下所示，请看下面代码块。  
从上面代码块中可以看到， 创建一个 GrpcConnection 客户端连接对象，需要向其有参构造方法中传递 3 个参数，分别是 ConnectionMeta 连接元信息对象，StreamObserver 流式通信对象，以及 netty 为客户端连接创建的 SocketChannel 。这三个参数都是 GrpcConnection 连接对象内部的成员变量，当然，这几个成员变量有的定义在了 GrpcConnection 类中，有的定义在了 GrpcConnection 的父类 Connection 中，这都没关系，反正我们最终可以通过对应的 get 方法得到 GrpcConnection 对象的各个成员变量。当然，我们最终的目的并不是为了得到这几个成员变量中的某一个，而是为了刷新 GrpcConnection 连接对象的最新活跃时间，那么这个最新活跃时间定义在哪了呢？这时候我就可以对大家明说了，实际上，GrpcConnection 连接对象的最新活跃时间就定义在了 ConnectionMeta 连接元信息对象内部。而这个 ConnectionMeta 连接元信息对象我之前一直没有为大家展示，接下来就为大家展示一下这个 ConnectionMeta 的部分内容，请看下面代码块。  
从上面的代码块可以看出，设置客户端连接对象最新活跃时间的方法已经定义完毕了， 就是 setLastActiveTime() 方法，只要这个方法一被调用，那么客户端在服务端的客户端连接对象的最新活跃时间就会被刷新 ， 那么该方法会在哪里被调用呢？这个我也按照 nacos 框架源码定义好了，该方法就在 GrpcConnection 的父类，也就是 Connection 抽象类中被调用了，具体实现如下，请看下面代码块。  
从上面代码块中可以看到， 在 freshActiveTime() 方法中，调用了客户端连接对象 ConnectionMeta 元信息对象的 setLastActiveTime() 方法，并且使用系统当前时间给客户端连接对象的最新活跃时间赋值了 。这时候大家肯定会追问，那 Connection 类的 freshActiveTime() 方法什么时候被调用呢？这个几乎不用怎么思考就能回答了，肯定是在 ConnectionManager 连接管理器中被调用啊，我们一开始的目的不就是想重构 ConnectionManager 连接管理器，为它定义一个 refreshActiveTime() 方法吗？ 现在实现思路也明确了，就是在连接管理器中根据客户端连接 Id 得到对应的客户端连接对象，然后直接调用连接对象的 freshActiveTime() 方法即可 。好了，接下来就请大家看看我重构之后的 ConnectionManager 连接管理器，请看下面代码块。  
到此为止，我就把服务端刷新客户端连接最新活跃时间的功能实现完毕了。可以看到，这个功能还是非常简单的，甚至是非常不起眼的一个小功能，但我想说的是， 这个功能对服务端连接判活功能的实现至关重要 。那什么是服务端连接判活呢？这个解释起来也非常简单，之前我们在 nacos 客户端实现了健康检测功能， 所谓客户端健康检测就是 nacos 客户端定期向 nacos 服务端发送 HealthCheckRequest 健康检测请求，而该请求会被 nacos 服务端的单向流请求接收器接收，紧接着就会执行刷新客户端连接最新活跃时间的操作，这个是 nacos 客户端主动向服务端检测连接是否健康并刷新活跃时间的功能 ；既然有 nacos 客户端主动向服务端检测连接是否健康，肯定就会有 nacos 服务端主动向客户端检测连接是否健康，那 nacos 服务端怎么主动向客户端检测连接是否健康呢？这个时候，服务端每一个客户端创建客户端连接对象的最新活跃时间就该派上用场了。  
  
引入 NacosRuntimeConnectionEjector，实现服务端对连接主动探活功能  
  
请大家想一想，假如 nacos 服务端想对客户端连接主动探活，是不是只需要判断客户端连接对象的最新活跃时间是否更新及时即可？如果客户端和服务端连接一直健康，那么 nacos 客户端肯定会定期成功发送健康检测请求，也就意味着会定期刷新服务端的客户端连接对象的最新活跃时间。 假如 nacos 服务端定义了一个连接过期时间，比如说这个时间定义为了 2 秒，nacos 客户端会定期发送健康检测请求刷新客户端连接的最新活跃时间，nacos 服务端则可以定义一个定时任务，定时任务定期检查系统当前时间和客户端连接的最新活跃时间的差值是否超过了 2 秒，如果超过 2 秒，就代表客户端连接对象的最新活跃时间过期了，可能就意味着客户端和服务端连接出现问题了 。我想这个逻辑应该非常简单吧？在过去完结的框架课程中，已经见过非常多的健康检查的方式了，这种方式应该是非常普通的一种。  
  
如果上面的逻辑大家都理解了，那么接下来我们要做的事就非常简单了，比 如我们就可以定义一个连接驱逐器，也就是本小节标题中展示的 NacosRuntimeConnectionEjector 类 。在这个 NacosRuntimeConnectionEjector 连接驱逐器中定义一个判断连接是否过期的方法，然后创建定时任务，定期执行该方法即可。 比如说我们就把判断连接是否过期的方法定义为 ejectOutdatedConnection() 方法，在该方法中首先从 ConnectionManager 连接管理器中得到服务端创建的所有客户端连接对象，然后遍历这些客户端连接对象，依次判断每一个客户端连接对象的最新活跃时间是否过期 。这个思路应该没什么问题吧，并且根据刚才分析的思路，我们还可以延伸出两个要点：  
1 NacosRuntimeConnectionEjector 连接驱逐器的 ejectOutdatedConnection() 方法肯定需要用到 ConnectionManager 连接管理器对象，原因很简单，因为在 ejectOutdatedConnection() 方法中需要获得服务端创建的所有客户端连接对象，而所有的客户端连接对象只能从 ConnectionManager 连接管理器对象中获得。所以，我们可以把 ConnectionManager 连接管理器对象定义为 ejectOutdatedConnection() 方法的参数，或者直接就把连接管理器定义为 NacosRuntimeConnectionEjector 连接驱逐器的一个成员变量。在 nacos 源码中，直接把连接管理器定义为 NacosRuntimeConnectionEjector 连接驱逐器的成员变量了，所以我也会采用这种方式实现 NacosRuntimeConnectionEjector 连接管理器。  
2 还有一点需要考虑的就是，假如在 NacosRuntimeConnectionEjector 连接驱逐器的 ejectOutdatedConnection() 方法中发现有些客户端连接对象的最新活跃时间过期了，这可能就意味着对应的 nacos 客户端没能定期发送健康检测请求，没能定期刷新服务端最新的活跃时间，这也就意味着客户端和服务端之间的连接可能出现问题了。那这个时候 nacos 服务端需要做什么呢？直接注销出现问题的客户端连接吗？显然不能这么做，也许只是网络出现波动了，造成客户端连接最新活跃时间没能及时刷新，所以这个时候 nacos 服务端要做的就是使用对应的客户端连接对象主动向 nacos 客户端发送一个探活请求，如果可以接收到该请求的响应，这就意味着客户端和服务端的连接是没有问题的。如果接受不到客户端回复的响应，这时候就意味着客户端和服务端连接确实出问题了，那么 nacos 服务端的连接驱逐器就可以驱逐该客户端连接对象了。  
好了，经过上面的分析之后，现在我们可以着手从代码层面实现这个 NacosRuntimeConnectionEjector 连接驱逐器，以及其中定义的 ejectOutdatedConnection() 方法，请看下面代码块。  
以上就是我为大家定义的 NacosRuntimeConnectionEjector 连接驱逐器，上面代码块中的注释非常详细，而且之前对连接驱逐器的实现思路分析得也非常详细，所以我就不再重复讲解上面代码块的逻辑了，大家认真看一看就成。当然，有一点需要简单提一下，那就是我相信有很多朋友都注意到了，在上面代码块的第 116 行创建了一个 ClientDetectionRequest 请求对象，然后 nacos 服务端使用客户端连接对象把这个 ClientDetectionRequest 请求发送给了客户端，这个 ClientDetectionRequest 请求就是 nacos 服务端向客户端主动探活请求。而我之所以忽然提到这个请求，是因为我想有些朋友可能会觉得这个 ClientDetectionRequest 请求非常熟悉，没错，因为我已经把 nacos 客户端接收并处理该请求的操作实现了，在前几个版本代码就展示给大家了，并且添加了很详细的注释。如果大家仔细看我提供的前六个版本代码了，肯定就知道 nacos 客户端在哪里接收并处理服务端发送过来的 ClientDetectionRequest 探活请求。如果有的朋友忘了，那就可以直接去 nacos 客户端的 RpcClient 类的 start() 方法中查看，具体代码我就不展示。  
  
好了，现在 NacosRuntimeConnectionEjector 连接驱逐器也定义完毕了， 连接驱逐器的 ejectOutdatedConnection() 方法会执行真正的过期连接检测操作，也就是 nacos 服务端的健康检测操作 。但是实现了连接驱逐器只是第一步，重要的是如何让连接驱逐器开始工作，也就是说如何让连接驱逐器的 ejectOutdatedConnection() 方法被执行，并且还需要周期执行，健康检测肯定要定义为一个定时任务啊，这个大家肯定都清楚。那这个定时任务应该怎么定义呢？其实非常简单， 只需要执行连接驱逐器的 ejectOutdatedConnection() 方法即可，但从上面代码块中可以看到，该方法实际上会被连接驱逐器的 doEject() 方法调用，那我们就直接定义一个定时任务，然后在定时任务中直接执行连接驱逐器的 doEject() 方法即可 。这个思路大家应该也可以理解吧，如果理解了这一点，就可以继续思考新的问题，那就是我们要定义的定时任务最终是在哪里创建并提交给定时任务执行器的呢？这就没什么好分析的了， 在 nacos 源码中把服务端健康检测的定时任务定义在了 ConnectionManager 连接管理器中 ，毕竟连接管理器，连接驱逐器都是有关联的组件，我们也模仿实现即可。接下来，就让我来给大家再次重构一下 ConnectionManager 连接管理器。  
  
再次重构 ConnectionManager 连接管理器，实现服务端健康检测功能  
  
因为要把服务端健康检测的定时任务定义在 ConnectionManager 连接管理器中，而该定时任务需要执行连接驱逐器的 doEject() 方法，所以我就把连接驱逐器定义为 ConnectionManager 连接管理器的一个成员变量了，这样一来在 ConnectionManager 连接管器器初始化的时候，就可以直接创建连接驱逐器对象给内部的连接驱逐器成员变量赋值，然后创建服务端健康检测定时任务提交给定时任务执行器即可。接下来，就请大家看看我重构完毕之后的 ConnectionManager 连接管理器，请看下面代码块。  
现在，ConnectionManager 连接管理器就已经重构完毕了，通过上面的代码块，大家就可以知道 nacos 服务端健康检测定时任务究竟是怎么创建的，又是怎么提交给定时任务执行器的，还有 ConnectionManager 连接管理器中的连接驱逐器成员变量是怎么被赋值的。如果这些逻辑大家都彻底掌握了，那么这一章的核心内容也就结束了。当然，本章内容进行到这里，大家肯定也能想到 ConnectionManager 连接管理器的内容绝非文章中展示的这么点，在 ConnectionManager 类中，实际上还有很多内容等待我们添加，也就是说，ConnectionManager 连接管理器肯定还会经过多次重构。确实是这样，就比如说之前我们看到的 ConnectionManager 连接管理器注销客户端连接对象的 unregister() 方法，我就没有为大家实现。除此之外，在 nacos 源码中 ConnectionManager 连接管理器还负责客户端 IP 计数，以及限制 nacos 服务端接收客户端连接数量等等，这些功能会在以后的文章中全部重构完整，这不是什么着急的任务，也不是什么难以实现的功能，顶多和 web 控制台界面交互一下而已。  
  
现在我们还是回到正题吧，诸位，请别忘了上一章还有最后一个问题没有解决呢，那就是 服务端想给客户端 B 主动发送消息时，怎么就知道客户端 B 的连接 Id 是多少呢 ？这个问题一直没有为大家解决，已经拖得够久了，怎么着也该轮到这个问题被解决了吧？当然，要解决这个问题也需要恰当的时机，而现在时机已经成熟了，在上一章我们已经为 nacos 服务端引入了 RequestHandler 请求处理器，并且也定义了各种各样的请求处理器，比如处理客户端注册服务实例请求的 InstanceRequestHandler 处理器、处理客户端订阅服务实例信息的 SubscribeServiceRequestHandler 请求处理器，处理客户端健康检测请求的 HealthCheckRequestHandl 请求处理器。在这三个处理器中，HealthCheckRequestHandl 请求处理器已经实现完毕了，还剩下 InstanceRequestHandler 和 SubscribeServiceRequestHandler 这两个请求处理器没有实现，按顺序来说，也该实现这两个请求处理器了，并且我会首先为大家实现 InstanceRequestHandler 注册客户端服务实例信息的请求处理器。因为只有客户端注册了服务实例信息，其他客户端才能从 nacos 服务端订阅相关的服务实例信息呀，一旦有客户端订阅了服务实例信息，服务端才可以根据服务实例信息的变化，判断是否要主动向对应的客户端发送最新的服务实例信息，这个时候我们就会真正解决一直遗留的那个问题。  
  
以上这些都是后面章节的内容了，大家看完本章内容之后，仍然可以去我提供的第七版本代码中查看本章新引入的连接驱逐器以及重构之后的连接管理器的代码，只不过大家会发现，我提供的第七版本代码中的连接驱逐器和连接管理器，比文章展示的内容要丰富一些，大家可以结合详细的代码注释，仔细看看多出来的代码具体负责什么功能，内容并不难，就当是留给大家的练习吧。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/bubi00y7bilk2d9u*  
*All content belongs to its respective owners and creators.*