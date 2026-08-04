上一章结束之后，我们就剩下两个问题还没有解决了，分别是：  
之前为大家实现的 GrpcRequestAcceptor 单向流请求接收器的功能并没有实现完整 。  
服务端想给客户端 B 主动发送消息时，怎么就知道客户端 B 的连接 Id 是多少呢 ？  
这两个问题已经拖了很久了，尤其是第一个问题，明明 grpc 服务端的单向流请求接收器 GrpcRequestAcceptor 早就引入了，但只是一个半成品，我一直没有为大家重构完整。上一章把 grpc 服务端的双向流消息接收器实现完整之后，大家也大概清楚了 nacos 服务端的单向流请求接收器和双向流消息接收器分别负责什么功能。当然，对于单向流请求接收器的功能，我也只是在上一章实现双向流消息接收器时简单提了提，并没有详细讲解，这一章我就先为大家实现完整的单向流请求接收器。  
  
引入 RequestHandler、RequestHandlerRegistry、重构 GrpcRequestAcceptor 单向流请求接收器  
  
我在上一章对比单向流和双向流接收器的时候，跟大家说 GrpcRequestAcceptor 单向流请求接收器主要接收并处理客户端发送的功能请求，比如注册服务实例信息、订阅服务实例信息、取消订阅服务实例信息等等操作，它的功能仅限于此，非常单一 。但是在我们之前实现的 GrpcRequestAcceptor 单向流请求接收器中，并没有实现相关功能，这个 GrpcRequestAcceptor 请求接收器只能处理 nacos 客户端发送过来的 ServerCheckRequest 请求，除此之外什么请求也无法处理。我把之前实现的 GrpcRequestAcceptor 请求接收器搬运到这里了，请看下面代码块。  
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
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/16

\* @方法描述：本类最核心的方法，服务端接收并处理客户端发送过来的单向流请求的方法

\*/

@ Override

public void request (Payload grpcRequest,StreamObserver < Payload > responseObserver) {  
//从请求元数据中得到客户端发送过来的请求的类型，其实就是请求的SimpleName

String type \= grpcRequest.getMetadata ().getType ();

//判断服务器是否已经启动完毕了，还记得上一章结尾我给大家留的问题吗？

//设置ApplicationUtils工具类的启动标志的作用是什么，这里就用到这个启动标志了

if (!ApplicationUtils.isStarted ()) {

//如果服务器还没有启动完毕，就回复给客户端一个错误响应

Payload payloadResponse \= GrpcUtils.convert (ErrorResponse.build (NacosException.INVALID\_SERVER\_STATUS,"Server is starting,please try later."));

//回复客户端错误响应

responseObserver.onNext (payloadResponse);

//表示信息回复完毕

responseObserver.onCompleted ();

上面展示的就是我们之前实现的 GrpcRequestAcceptor 单向流请求接收器，可以看到，这个请求接收器目前只能接收并处理 nacos 客户端发送过来的 ServerCheckRequest 请求。但是我刚才也跟大家说了，这个 GrpcRequestAcceptor 单向流请求接收器实际上会处理很多客户端发送过来的请求，而且是都是非常重要的功能性的请求，比如： 客户端向服务端注册服务实例的请求，订阅服务实例的请求，批量注册服务实例的请求，注册持久化的服务实例的请求等等 。大家在查看前六个版本代码的时候，也许已经从 api 模块下看到了非常多的请求体和响应体，这些都是 nacos 自己定义的请求和响应，当然，在网络传输的过程中，这些请求和响应还要转换成 grpc 框架需要的 Payload 对象才行。接下来我就为大家展示一部分 nacos 框架中的请求体，并为大家解释一下这些请求的作用，对应的响应体我就不再展示了，反正请求和响应都是一一对应的关系。请看下面代码块。  
以上代码块展示得就是我们已经见过并且使用过的请求对象，也展示了一些我们没见过但即将使用的请求对象。这些请求都是要被 nacos 服务端的 GrpcRequestAcceptor 单向流请求接收器处理的。但现在我们实现的 GrpcRequestAcceptor 接收器只能处理 ServerCheckRequest 请求，所以这个 GrpcRequestAcceptor 请求接收器显然要重构一下，把缺失的功能完善了。那该如何重构呢？  
  
我的想法非常简单，考虑到客户端要向服务端发送各种各样的请求，而服务端处理各种请求的操作也都不同。 就像服务端处理客户端发送的注册服务实例的请求，和处理订阅服务实例请求的操作肯定不同，因此在 nacos 服务端为每一个请求创建对应的请求处理器是很有必要的。比如我就定义一个 RequestHandler 抽象类，这个抽象类就代表请求处理器，在抽象类中定义 handle 方法，然后再定义各种请求处理器，让请求处理器实现 Handler 接口 。就像下面展示的这样，请看下面代码块。  
  
首先是请求处理器 RequestHandler 抽象类本身。  
可以看到，在上面代码块中展示的 RequestHandler 请求处理器非常简单 ，我在其内部定义了一个抽象的 handle() 方法，该方法就是真正处理客户端发送过来的请求的方法，该方法由子类实现，也就是各种真正的请求处理器实现 ；与此同时，大家也能在 RequestHandler 类中看到一些关于过滤器处理请求的操作，因为过滤器的内容我还没引入第七版本，只是引入了最外面的一层空壳，所以关于过滤器的内容大家看看注释就行，不需要特别关注。总之，通过上面展示的 RequestHandler 请求处理器顶级父类， 大家可以意识到当客户端发送单向流请求给服务端之后，服务端会使用请求处理器处理该请求，在处理的过程中，会先让请求被用户定义的过滤器处理一遍，如果没有通过过滤器，那就直接返回客户端错误响应，不再继续处理请求，通过过滤器之后，才会真正调用 handle 方法处理客户端发送过来的请求 。  
  
好了，RequestHandler 抽象类已经定义完毕了，接下来就该为 nacos 服务端定义各种请求的处理器实现类了。首先先定义注册服务实例请求的处理器，也就是 InstanceRequest 请求处理器，请看下面代码块。  
如果是订阅服务实例请求，也就为是 SubscribeServiceRequest 请求定义请求处理器，请看下面代码块。  
如果是客户端执行健康检查请求，也就为是 HealthCheckRequest 请求定义请求处理器，请看下面代码块。  
在文章中我们就只定义这三个请求处理器吧，其他的就不再展示了。通过以上展示的代码块内容， 大家可以看到我们定义的这三个请求处理器，都添加了 springboot 的 @Component 注解，这就意味着这些处理器都是可以被 springboot 容器管理的 。当然，这三个请求处理器都还没有被真正实现，其中最重要的 handle() 方法都还是空实现。这倒不是因为它们实现起来比较困难，实际上，这些处理器中的核心方法都很容易实现，但现在有更重要的问题等着我去解决，所以就先把处理器的实现暂且放一放。  
  
目前我们已经实现了注册服务实例信息的请求处理器、订阅服务实例信息的请求处理器、健康检测请求处理器，这几个请求处理器各自处理来自客户端的对应请求。而我们也正是抱着这种请求、处理一一对应的思路才为各个请求定义了请求处理器。但是我们都知道，就我们目前实现的 nacos 服务端来说， 我们为 nacos 服务端定义的 GrpcRequestAcceptor 单向流请求接收器会接收并处理来自 nacos 客户端的所有单向流请求，可我们的目的是让定义的各个请求处理器处理对应的请求 ，这该怎么办呢？这时候大家应该也想到了，这很好办， 反正 GrpcRequestAcceptor 请求接收器会接收来自客户端的所有单向流请求，那就直接在请求接收器中把接收到的请求分发一下，或者说在 GrpcRequestAcceptor 请求接收器中找到与该请求对应的请求处理器，然后让处理器直接处理请求，最后回复 nacos 客户端响应 ，这不就行了吗？  
  
我完全可以把在 nacos 服务端定义的所有请求处理器收集起来，然后把这些处理器存放到 GrpcRequestAcceptor 请求接收器中，这样一来在请求接收器接收到来自客户端的单向流请求，就可以直接从内部找到与该请求对应的处理器，然后处理请求即可。编码思路已经很明显了， 请求和请求处理器是一一对应的关系，那我就可以直接在 GrpcRequestAcceptor 请求处理器中定义一个 map，map 的 key 就是处理器要处理的请求的类名，value 就是对应的请求处理器 。就像下面代码块展示的这样，请看下面代码块。  
在上面代码块的第 31 行，直接就得到了 nacos 客户端发送过来的请求的类名，而我们在 GrpcRequestAcceptor 请求接收器中新定义的 map 成员变量，其中存放的 key 就是请求的类名，value 就是对应的请求处理器，所以正好可以使用请求类名从新定义的 map 中得到对应的请求处理器 ，然后直接使用请求处理器处理请求即可。这个逻辑非常简单，我相信就算是刚刚学习编程的朋友也能看懂，但是，新的问题就来了： 那就是我们怎么把 nacos 服务端中的所有请求处理器添加到 GrpcRequestAcceptor 单向流请求接收器的 map 中呢 ？如果这个问题解决了，那以上全部逻辑和流程就可以跑通了，接下来我们的任务就变得非常简单，只需要为每一个请求实现对应的请求处理器即可。  
  
对于这个问题，我也想好了解决的对策，别忘了，我们在 nacos 服务端定义的所有请求处理器都添加了 @Component 注解，这就意味着它们是 springboot 容器中的 bean 对象，可以被 springboot 自动创建。如果是这样的话，那我等这些请求处理器全部都创建完毕了，然后再从 springboot 容器中得到它们，再把它们放到 map，这样一来，不就把所有的请求处理器都收集完毕了吗？当然， 收集这些请求处理器有一个重要的前提，那就是必须等待 springboot 容器把这些请求处理器全部创建完毕才能收集 ，那怎么就知道 springboot 把这些请求处理器都创建完毕了呢？肯定是 springboot 容器管理的所有 bean 对象都创建完毕了，这些请求处理器肯定也都创建完毕了； 而 springboot 容器管理的 bean 对象创建完毕之后会发布一个 ContextRefreshedEvent 事件，只要我们再定义一个 springboot 的监听器监听该事件即可。只要 springboot 发布了该事件，就在我们自己定义的监听器对象中从 springboot 容器中收集 nacos 服务端所有的请求处理器 。这个监听器我也已经定义好了， 就定义为了 RequestHandlerRegistry 类，翻译过来就是请求处理器注册表的意思，该监听器实现了 springboot 的 ApplicationListener 接口，并且监听 ContextRefreshedEvent 事件 ，该监听器具体实现如下，请看下面代码块。  
上面代码块中注释非常详细，所以我就不再重复讲解了。总之现在我们又引入了一个新的组件，那就是 RequestHandlerRegistry 请求处理器注册表，这个注册表会在所有请求处理器创建完毕之后从 springboot 容器中收集所有的请求处理器对象，然后存放到内部的 map 中。而随着这个 RequestHandlerRegistry 请求处理器注册表的引入，刚才重构好的 GrpcRequestAcceptor 单向流请求接收器又可以简单重构一下了，只需要把 GrpcRequestAcceptor 内部的 map 成员变量替换成 RequestHandlerRegistry 请求处理器注册表即可。请看下面代码块。  
到此为止，这个 GrpcRequestAcceptor 单向流请求接收器的核心功能就重构完整了，当然，这并不意味着 GrpcRequestAcceptor 类的 request() 方法被我们重构完整了。实际上在单向流请求接收器的 request() 方法中，还有很多内容没有展示给大家，我只是把最核心的内容给大家展示了。而那些非核心的内容基本上都是一些判断，比如说判断是否从 RequestHandlerRegistry 请求处理器注册表中找到了对应的请求处理器，判断接收到的客户端的请求是否解析成功等等，内容有点多，但是逻辑非常简单，所以我就不再仔细分析了，接下来我就把重构完整的 GrpcRequestAcceptor 单向流请求接收器展示给大家，请看下面代码块。  
好了，到此为止，GrpcRequestAcceptor 单向流请求接收器就重构完整了，文章开始展示的两个问题只剩下最后一个问题没有解决了。现在我想大家已经对 nacos 服务端核心功能的运行原理有一个大概地了解了。大家肯定清楚 nacos 服务端是怎么启动的，启动之后又是怎么接收客户端连接的，怎么封装客户端相关信息，以及怎么接收客户端请求并且处理客户端请求。我相信这些流程大家都已经非常清楚了。这些流程都清楚之后，就该步入正轨了，所谓步入正轨，就是实现 nacos 服务端的各种请求处理器，因为各种请求处理器执行的操作才是 nacos 框架最核心的功能。当然，即便是最核心的功能，这些处理器实现起来也都没什么难度。就比如说健康检查请求处理器吧，它的实现和空处理器几乎没什么区别。  
  
实现 HealthCheckRequestHandler 健康检测请求处理器  
  
我们都知道当 nacos 客户端和服务端建立连接之后，为了保证客户端和服务端正常通信，nacos 客户端会定期向服务端发送健康检测请求，也就是 HealthCheckRequest 请求，该请求会被 nacos 服务端的单向流请求接收器接收并处理，但是具体处理的操作，我并没有为大家实现。在当时实现 nacos 客户端功能的时候，我只是跟大家说 nacos 客户端会定期向服务端发送 HealthCheckRequest 请求，然后刷新客户端和服务端建立的连接的最新活跃时间，就像下图展示的这样，请看下图。  
  
并且我还在上图中说了，nacos 服务端实现的 HealthCheckRequestHandler 健康检测请求管理器所执行的操作非常简单，就是直接回复给客户端一个 HealthCheckResponse 响应即可。实际上也正是这样，nacos 服务端的 HealthCheckRequestHandler 请求处理器就是这样实现的，请看下面代码块。  
到此为止，nacos 服务端的 HealthCheckRequestHandler 请求处理器就实现完毕了。可以看到，这个 HealthCheckRequestHandler 请求处理器就是这么简单。当然，我在这一章要结束的时候忽然为大家实现这个请求处理器也是有原因的。还是请大家看看上面的图片，在上面的图片中，我用红线标出来一段话，讲解了客户端究竟是怎么维护和服务端建立的连接的。实际上， 当 nacos 客户端和服务端建立连接之后，服务端会为客户端建立连接对象，现在我们已经知道所谓的连接对象就是 GrpcConnection 对象了，并且还会创建一个连接元信息对象，而客户端和服务端连接的最新活跃时间就定义在连接元信息中，现在我们也知道了，这个连接元信息对象就是 ConnectionMeta 对象。并且，只要 nacos 客户端向服务端发送了消息，不管是什么消息，服务端接收到消息后，都会刷新客户端对应的连接的最新活跃时间 。而 nacos 客户端会定期向服务端发送健康检测请求，所以服务端这边的连接的最新活跃时间一致会被刷新，现在我们已经实现了 nacos 服务端很多功能了，那么 nacos 服务端刷新客户端连接最新活跃时间的操作究竟是在哪里执行的呢？其实在上一小节最后的代码块中，我已经把相关的操作展示给大家了， 也就是最后重构完整的 GrpcRequestAcceptor 单向流请求接收器代码的第 202 行 。我把具体内容搬运到下面了，请看下面代码块。  
可以看到，这里直接调用了连接管理器的 refreshActiveTime() 方法刷新了客户端连接的最新活跃时间。大家肯定会感到很突然，因为我们目前实现的 ConnectionManager 连接管理器中根本没有这个方法，我们目前实现的连接管理器如下所示，请看下面代码块。  
显然根本就没有所谓的 refreshActiveTime() 方法，这并不意味着我们编码出错了，而是意味着 ConnectionManager 连接管理器需要重构了。这就是我在这一章结尾为大家实现 nacos 服务端的 HealthCheckRequestHandler 请求处理器的原因，因为我想借此把 nacos 服务端的健康检查机制和拒绝连接功能给实现了，这就意味着需要重构 ConnectionManager 连接管理器，也许大家对此还很迷惑，不知道这些功能和 ConnectionManager 连接管理器有什么关系。这没关系，随着功能的实现，大家就全都清楚了。当然，这些功能在这一章是实现不了了，就留到下一章实现吧，朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/wg34atbcv4iw827q*  
*All content belongs to its respective owners and creators.*