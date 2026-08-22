上一章我们已经为 Proxy 模块实现了 Netty 服务端，并且成功启动了 Proxy 模块，这一章的内容和上一章差不多，只不过要构建的服务器变成了 Grpc 服务器，然后再成功启动该服务端即可。整体流程和之前一样，没什么难度，所以就让我们直接开始吧(Grpc 的知识我就不再讲解了，在 Nacos 框架课程中已经讲解过一些了，大家应该知道这个框架的使用方式了)。  
  
众所周知，要想使用 Grpc 构建可以通信的客户端和服务端，那首先要做的就是把客户端服务端通信的请求响应格式，以及处理请求的方法定义出来，这时候就轮到 protobuf 登场了，我们只需在 protobuf 文件中定义好请求和响应的格式，定义好处理请求的方法，程序就会自动为我们生成用来通信的代码，我们什么也不必做。当然，这并不意味着定义 protobuf 文件是一件特别简单的事，因为在定义请求和相应之前，我们首先得明确在程序内部可能会存在哪些请求和响应。  
  
上一章我们构建的 Netty 服务端可以处理根据主题获取路由信息的请求，那么现在要使用 Grpc 为 Proxy 构建服务端了，这个服务端显然也要能处理根据主题获取路由信息的请求，所以应该在 protobuf 文件中把这个请求以及对应的响应格式定义出来，当然，肯定还要把处理该请求的方法也定义出来。如果我们把 Grpc 服务端处理的根据主题获取路由信息的请求定义为 QueryRouteRequest，而服务端处理完该请求要回复的响应定义为 QueryRouteResponse，那么目前的 protobuf 文件可以定义成下面这样，请看下面代码块。  
  
  
//这里我解释一下，rocketmq框架把这些数据都定义在了apache.rocketmq.proto组件中了

//如果大家想提前查看所有的请求和响应格式，可以直接去该组件的apache.rocketmq.v2;包下查看

//以下这些代码指定的都是生成文件的格式，我就不解释了，这些内容大家自己查一下就行

syntax \= "proto3";  
import "google/protobuf/duration.proto";

import "google/protobuf/timestamp.proto";  
import "apache/rocketmq/v2/definition.proto";  
package apache.rocketmq.v2;  
option csharp\_namespace \= "Apache.Rocketmq.V2";

option java\_multiple\_files \= true;

option java\_package \= "apache.rocketmq.v2";

option java\_generate\_equals\_and\_hash \= true;

option java\_string\_check\_utf8 \= true;

option java\_outer\_classname \= "MQService";  
  
  
//下面就是定义完毕的请求和响应格式  
  
message QueryRouteRequest {

//Resource其实也是一个数据格式，被定义在了definition.proto文件中

//Resource中包含了请求中携带的主题信息，也就是客户端想获得的路由信息所属的主题的信息

Resource topic \= 1;

//Endpoints，被定义在了definition.proto文件中

//Endpoints中包含了发送请求过来的客户端的网络地址

Endpoints endpoints \= 2;

}  
message QueryRouteResponse {

好了，现在我就把一个最简单的 proto 文件定义完毕了， 定义好上面的 proto 文件之后，启动程序，proto 框架就会自动为我们生成对应的代码，当我们使用 Grpc 构建的客户端从服务端查询某个主题的路由信息时，只需要向服务端发送一个包含了主题信息的 QueryRouteResponse 请求即可，而 Grpc 构建的服务端会在 QueryRoute 方法中处理该请求，然后回复给客户端包含了路由信息的 QueryRouteResponse 响应 。这个逻辑应该很清晰吧？  
  
这么来看，定义 proto 文件似乎根本没什么难度，我们的工作量一点也不大，但我刚才并不是这么说的，原因就在于程序中可能会存在多种多样的请求，比如说消费者客户端想获取可消费的消息了，就会使用 Grpc 客户端向 Proxy 模块的 Grpc 服务端发送获取消息的请求；再比如说，生产者客户端想把消息发送给 Broker，这个时候生产者也会把消息发送给 Proxy 模块的 Grpc 服务端，然后让 Proxy 发送给 Broker，这个发送消息的请求该怎么定义呢？再比如说和 Ack 相关的请求和响应，这又该怎么定义呢？由此可见，在一个功能完善的 Mq 框架中，存在多种多样的请求响应需要被定义在 proto 文件中，所以我说定义这个 proto 文件并不是一件特别简单的事。但是回到我们自己的消息队列框架中，目前我们还不需要把所有请求和响应都定义出来，我们要做的仅仅是构建 Proxy 模块的 Grpc 服务端，然后启动客户端测试类，向服务端发送根据主题获取路由信息的请求即可，所以现在我们只需要把 QueryRouteRequest、QueryRouteResponse 定义出来即可。  
  
好了，现在请求和响应定义完毕了，处理请求和响应的方法也定义完毕了，那接下来要定义什么呢？也许有朋友会说应该定义 Grpc 框架构建的服务器了，但我想说的是，这个根本不用我们自己构建，Grpc 框架已经为我们构建好了，直接使用 Grpc 框架创建服务器即可，就像下面代码块展示的这样，请看下面代码块。  
  
下面是我自己定义的 GrpcServerBuilder，也就是 Grpc 服务器的构建器。  
从上面的代码块可以看到，只要我创建一个 GrpcServerBuilder 构建器对象，然后调用该构建器的 build 方法，就可以创建一个 GrpcServer 服务端。而这个 GrpcServer 我也已经定义完毕了，请看下面代码块。  
从上面代码块中可以看到，就算使用 GrpcServerBuilder 构建器创建出来了 GrpcServer 对象，调用 GrpcServer 对象的 start 方法启动了 Proxy 模块的 Grpc 服务端，最终启动的也是 Grpc 框架内置的 Server 服务端，我们所做的一切都是套壳而已。工作够简单吧？好了，现在 Grpc 服务端也能启动了， 那接下来要做的就是让服务端能够处理客户端发送过来的请求了。这个时候我们就要定义一个 BindableService 对象了，因为需要向 Grpc 服务端中设置了对应的服务组件，也就是 BindableService 对象，Grpc 服务端才能使用设置的 BindableService 对象处理请求 。当然，这个 BindableService 肯定也是 Grpc 内置的接口，我们要做的就是按照自己的需求实现这个接口，然后把自己实现的 BindableService 对象设置到 Grpc 服务端中，这样 Grpc 服务端就可以按照我们的需求接收并处理请求了。现在我们的 Grpc 服务端只需要调用 QueryRoute 方法处理 QueryRouteRequest 请求即可，所以我们自己定义的这个 BindableService 对象可以这样实现，我把 BindableService 接口的实现类定义为了 GrpcMessagingApplication，请看下面代码块。  
以上代码块展示的 Grpc 框架的用法我在 Nacos 框架中都讲过了，所以就不再重复解释了。这个 GrpcMessagingApplication 也定义完毕了，接下来只要设置到 Grpc 服务端中即可，我把相关代码展示在下面代码块中了，请看下面代码块。  
上面代码展示的就是一个完整的 Grpc 服务端的创建和启动过程，可以看到，我们已经把 GrpcMessagingApplication 对象设置到服务端中了，这样一来， 当 Proxy 模块的 Grpc 服务端接收到客户端发送过来的 QueryRouteRequest 请求后，就可以自动在 queryRoute() 方法中处理该请求 。 当然，我们还没有真的实现 GrpcMessagingApplication 类的 queryRoute() 方法，还无法真正让 Proxy 模块的 Grpc 服务端处理 QueryRouteRequest 请求，那接下来就让我们实现一下该方法。  
  
如果是执行根据主题获取路由信息的操作，如果大家还记得上一章的内容， 那大家肯定知识我们再上一章引入了一个 MessagingProcessor 消息处理器，并且我跟大家说这个消息处理器对 Proxy 模块非常重要，因为不管是 Netty 服务器还是 Grpc 服务器，最后都会使用 MessagingProcessor 消息处理器来真正处理请求 ，我把该消息处理器的代码再次搬运过来了，请看下面代码块。  
目前这个 MessagingProcessor 消息处理器的内容还很简单，但不妨碍我们使用。不管怎么说吧，按照 RocketMq 5.0 之后的源码来看，我们自己的定义的 Grpc 服务端最后处理请求的时候也要用到这个 MessagingProcessor 消息处理器，既然是这样的话，那么这个 MessagingProcessor 消息处理器显然也应该定义为 GrpcMessagingApplication 类的成员变量，这样 GrpcMessagingApplication 对象就可以直接在 queryRoute() 方法中使用 MessagingProcessor 查询对应主题的路由信息了。这个逻辑应该可以理解吧？  
  
但我并不会这么做，因为我要再定义两个新的类， 一个是 DefaultGrpcMessingActivity 类，另一个是 RouteActivity 类 ，从类名上就可以看出， DefaultGrpcMessingActivity 类就是专门用来处理 Grpc 服务器接收到的消息的，而 RouteActivity 就是专门处理根据主题获取路由信息请求的 。为什么我要这么设计呢？ 因为在 RocketMq 源码中，存在很多个类似 RouteActivity 的消息活动器，这些 Activity 都有自己要处理的对应请求，比如说 RouteActivity 就是专门负责处理 QueryRouteRequest 请求，而其他 Activity 都有自己要处理的请求。这就像 Netty 服务器内部注册的多个请求处理器，每一个请求处理器都对应一个请求 。按着这么分析，那么最终应该持有 MessagingProcessor 消息处理器处理请求的应该是 RouteActivity，所以这个 RouteActivity 可以暂时定义成下面这样，请看下面代码块。  
在展示了 RouteActivity 之后，接下来让我们看看这个 DefaultGrpcMessingActivity。实 际上这个 DefaultGrpcMessingActivity 就是持有了所有 Activity 活动处理器的组件，并且还为每一个 Activity 定义了对外提供服务的方法 ，就像下面展示的这样，请看下面代码块。  
好了朋友们，现在 DefaultGrpcMessingActivity 类也展示完毕了，那我们现在是不是可以这么认为，只要调用了 DefaultGrpcMessingActivity 对象的 queryRoute() 方法，Proxy 模块的 Grpc 服务端就可以真正处理来自客户端的 QueryRouteRequest 请求了？显然是这样的，既然是这样， 那我们就把 DefaultGrpcMessingActivity 对象定义为 GrpcMessagingApplication 的成员变量不就行了 ？就像下面代码块展示的这样，请看下面代码块。  
上面代码块看完之后，现在我们也清楚了， 只要调用 GrpcMessagingApplication 的 create() 方法创建 GrpcMessagingApplication 对象时传入一个 MessagingProcessor 消息处理器，然后再把 GrpcMessagingApplication 设置到 Grpc 服务器中，那么 Grpc 服务器就可以真正开始工作了 。那这些操作又该在哪里执行呢？这就很简单了，当然是在 ProxyStartup 启动器中啊，所以重构之后的 ProxyStartup 启动器可以写成下面这样，请看下面代码块。  
到此为止，我们成功为 Proxy 模块实现了 Grpc 服务端，大家也可以直接阅读我提供的第四版本代码了。如果大家想测试第四版本代码，可以启动 Proxy 模块，然后启动 test 包下的 GrpcTest 测试类，也就是下面这个代码块。  
到此为止，本章的内容就结束了，可以看到这一章我讲得比较快，因为内容太简单了，就四五个类，实在是没什么讲的。当然，如果说最后再补充一点，那还是回到 GrpcMessagingApplication 类中， 在现在的 GrpcMessagingApplication对象中，处理 Grpc 客户端发送过来的 QueryRouteRequest 请求时，queryRoute() 方法会被 Grpc 框架自动调用，这个时候执行该方法的是 Grpc 内部的线程 。这也就意味着，后续执行的所有 RocketMq 内部的业务操作，都是 Grpc 框架线程执行的，这就像使用 Netty 的单线程执行器执行和 RocketMq 相关的业务操作，显然不太合适。那怎么解决这个问题呢？ 最简单也是最直接的方法就是为 queryRoute() 方法定义一个新的线程池，只要是处理 QueryRouteRequest 请求，就可以在 queryRoute() 方法中把处理请求的操作封装成一个任务，提交给新的线程池来执行 ，这个操作大家都能理解吧？已经见得太多了，所以 GrpcMessagingApplication 类又可以重构成下面这样，请看下面代码块。  
好了，现在展示给大家的就是 GrpcMessagingApplication 类的完全体代码了，代码一下子增加了这么多，也许有的朋友反应不过来，但我可以很负责任地告诉大家，新添加的方法真的非常简单，如果大家不想看文章中的代码，可以直接去我提供的第四版本代码中查看。本章的内容就到此为止了，朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/la8pir8tyhmeq92k*  
*All content belongs to its respective owners and creators.*