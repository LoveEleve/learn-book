上一章我把程序的 ConnectionManager 连接管理器重构了，并且引入了 NacosRuntimeConnectionEjector 连接驱逐器，这标志着 nacos 服务端健康检测的功能实现完毕了，接下来就该步入正轨，开始实现 nacos 注册中心框架的核心功能了。所谓核心功能无非就是注册/注销服务实例功能、订阅/取消订阅服务实例功能、服务端察觉到服务实例信息发生变更之后，主动向客户端发送配置信息的功能，除此之外，似乎也没有什么特别重要的功能了。这一章，我就按照顺序，首先为大家实现 nacos 服务端注册/注销服务实例功能，与之对应的客户端功能，在前面章节我们构建 nacos 客户端时，已经实现完毕了。  
  
探讨 nacos 服务记录客户端操作的方式  
  
虽说这一章要实现 nacos 服务端注册/注销服务实例功能，但是我们还不必急着实现对应的代码。所谓对应的代码其实就是 InstanceRequestHandler 请求处理器。因为 nacos 客户端想要向服务端注册/注销服务实例信息时，会把服务实例信息封装到 InstanceRequest 请求中，然后把该请求发送给服务端。而 nacos 服务端的单向流请求接收器接收到该请求后，会根据请求名称寻找到对应的请求处理器，也就是 InstanceRequestHandler 请求处理器，在该处理器中处理 InstanceRequest 请求，执行注册/注销服务实例的操作。在前几章我就已经为大家引入了 InstanceRequestHandler 请求处理器，但是并没有真正实现它。它的具体内容如下，请看下面代码块。  
package com.cqfy.nacos.naming.remote.rpc.handler;  
@ Component

public class InstanceRequestHandler extends RequestHandler < InstanceRequest,InstanceResponse > {  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/16

\* @方法描述：处理InstanceRequest请求的方法

\*/

@ Override

public InstanceResponse handle (InstanceRequest request,RequestMeta meta) throws NacosException {  
//该方法暂时不做实现

}

}

可以看到，这个 InstanceRequestHandler 请求处理器目前还是一个空实现。nacos 服务端接收 InstanceRequest 请求，并且使用对应的 InstanceRequestHandler 请求处理器处理请求的操作如下，请看下面代码块。  
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

在看过以上两个代码块之后，我想大家现在一定会达成一个共识，那就是只要我们实现了 InstanceRequestHandler 请求处理器，nacos 服务端的注册/注销服务实例的功能就实现了。一步之遥，但是这其中并没有那么简单，请大家想一想，服务实例在服务端该怎么存储呢？也许大家对我提出的这个问题有点困惑，不太清楚是什么意思，接下来，就让我为大家分析一下。  
  
请大家想象一个场景，当我们启动了一个 nacos 注册中心服务端，然后在多个机器上部署了 nacos 的客户端，假如说部署了 5 个客户端，分别是客户端 1、2、3、4、5。在这 5 个客户端里，客户端 1、2、3、4 都向 nacos 服务端注册了服务实例信息，客户端 5 向服务端订阅了相关的服务实例信息，这种情况下，nacos 服务端怎么知道哪个客户端执行了哪些操作呢？说得稍微直接一点，那就是 nacos 服务端怎么知道哪些客户端注册了哪些服务实例信息，哪些客户端订阅了哪些服务实例信息呢？也就是说，nacos 服务端怎么维护来自客户端的操作信息呢？接下来， 就让我们先思考一下，当客户端 1、2、3、4 都向服务端注册了服务实例信息，nacos 服务端该怎么存储这些服务实例信息呢 ？  
  
我们现在还不必关心 nacos 客户端注册的服务实例信息具体内容是什么，也不必关心怎么把注册的服务实例信息推送给订阅者的情况，就只关心 nacos 服务端如何存储客户端注册的服务实例信息即可。请大家想一想，客户端 1、2、3、4 注册了服务实例信息到服务端之后，为了能让服务实例信息和注册客户端对应起来，或者说为了能让服务端有条理地存放客户端注册服务实例信息， 那么采用 key-value 键值对的方式存储服务实例信息显然是最合适的 。 所谓 key-value 键值，其 key 就是客户端，value 就是服务实例信息 。这样一来，我们就可以在 nacos 服务端以如下方式存放客户端注册的服务实例信息，请看下面代码块。  
就像上面代码块展示的这样，每一个客户端注册服务实例到服务端之后，服务端都会以键值对的方式存放服务实例信息，这样一来，nacos 服务端肯定就知道哪个客户端注册了哪个服务实例，对吧？虽然现在大家还不清楚维持这种键值对的好处在哪里，后面我们实现 nacos 客户端订阅服务实例功能时，大家就会清楚了。好了，让我们言归正传吧，既然是 key-valie 键值对存放客户端注册的服务实例信息，显然就要在 nacos 服务端定义一个 map，但是请大家想一想， 当我们把 map 定义好之后，真正向这个 map 里存放客户端的服务实例信息时，该怎么定义键值对的具体类型呢？按照我们刚才的分析，键值对中的 key 肯定要代表客户端信息，value 肯定要代表客户端注册的服务实例的信息，那键值对该以什么对象来表示呢 ？键值对中的 value 还好说，因为从 nacos 客户端注册服务实例到服务端的时候，就是把服务实例信息封装在一个 Instance 对象中，现在服务实例信息要存放到服务端了，仍然可以把这些信息封装到一个 Instance 对象中啊，所以就目前来看， 我们就把键值对的 value 设置为封装了服务实例信息的 Instance 对象吧 。  
  
好了，现在键值对的 value 定义好了，那么键值对的 key 要存放什么类型的对象呢？刚才我们已经分析过了，在 nacos 服务端定义的这个 map 肯定要存放客户端和服务实例信息的映射关系，现在服务实例信息的存储方式已经确定了，但是客户端应该以什么形式来表现呢？map 中的每一个 key 都代表一个客户端，nacos 服务端接收到的每个客户端的信息肯定是不同的，也就是 map 的 key 肯定具有唯一性，那么请大家想一想， nacos 服务端在和每一个客户端建立连接之后，是怎么维护每一个客户端的信息的呢？是不是为每一个客户端创建了唯一的客户端连接 Id ？我相信这时候大家应该都意识到了， 其实 map 中的 key 存放的就是每一个客户端的客户端连接 Id 。这样一来，nacos 服务端就可以使用下面这种方式在 map 中存放客户端注册过来的服务实例信息，请看下面代码块。  
而客户端连接 Id 是字符串类型的，所以在服务端定义好的 map，其 key 存放的就是 String 类型的客户端连接 Id，value 存放的就是封装了服务实例信息的 Instance 类型的对象 。到此为止，服务端存放服务实例信息时，就能够和服务实例信息来源的客户端一一对应了，就算有多个客户端都向服务端注册服务实例信息，那么 nacos 的服务端也可以清楚地知道是哪个客户端注册了哪个服务实例信息。如果我们现在决定把 nacos 服务端存放服务实例信息的 map 定义在 InstanceRequestHandler 请求处理器中，那么 InstanceRequestHandler 请求处理器就可以重构成下面这样，请看下面代码块。  
在看过上面的代码块之后，接下来就很好说了，接下来我们只需要实现 InstanceRequestHandler 请求处理器的 handle 方法即可，实现的方法也很简单， 无非就是从 InstanceRequest 请求中得到封装了服务实例信息的 Instance 对象，然后从线程的私有 map 中得到发送当前请求的客户端的连接 Id，再把客户端连接 Id 和对应的 Instance 对象存放到 map 中即可 。这是我们目前明确的编码思路，当然这个思路也不是没有缺陷，请大家想一想，nacos 客户端向服务端注册服务实例，肯定是想注册多少就注册多少，客户端即可以批量注册服务实例信息到服务端，也可以单独注册多个不同的服务实例信息到服务端。 所谓不同的服务实例信息指的就是服务实例信息所属的分组、服务名称、命名空间等等不相同 ，我想这种情况大家应该也可以想象得到。那现在问题就来了，假如客户端 1 向服务端注册了两个不同的服务实例信息，这两个服务实例信息在 nacos 服务端应该怎么存储呢？  
  
之前我们定义的 map 显然就不能再使用了，因为那个 map 只能存放一个服务实例信息，并且 key 代表的是客户端连接 Id，现在一个客户端可以注册多个不同的服务实例信息了，客户端信息肯定不能改变，所以 key 仍然是客户端连接 Id，但是 value 就不能再是一个 Instance 对象了，按照我的理解， 这个 value 存放的也应该是一个 map。map 的 key 代表服务信息，表示当前注册的这个服务实例属于哪个服务，value 代表真正的服务实例信息 。如果大家理解不了这句话的意思，那么接下来就请看看下面的代码块，看完之后应该就明白了，请看下面代码块。  
可以看到， 我现在定义完毕的最新的 map，其 key 是字符串类型，代表客户端唯一的连接 Id，value 是另一个 map，这个 map 中的 key 代表的是客户端注册服务实例所属的服务的信息，而 value 就是真正的服务实例信息 。现在大家应该清楚我刚才的话是什么意思了吧？如果这一点大家都清楚了，那么现在新的问题又来了，在上面代码块中，内嵌的那个 map 中，它的 key 我并没有定义出来，而是直接写成了伪代码，我们在正式程序中肯定不能写成伪代码，那么这个“服务信息”该怎么定义呢？用什么类型的对象来表示呢？  
  
其实我在上面已经给大家把答案展示出来了，我刚才说 nacos 客户端可以向服务端注册不同的服务实例信息，而不同的服务实例信息其实就是这些服务实例 所属的分组、服务名称、命名空间等等不相同 而已，既然这样，我大可以为 nacos 服务端引入一个新的类，把这个类定义为 Service，并且在这个类中定义 namespace、group、name 这几个成员变量封装服务实例所属的服务的信息，这样不就行了吗？具体实现请看下面代码块。  
现在 Service 类定义完毕了，这个类的对象也可以封装服务实例所属服务的信息，而且我们还给 Service 对象重写了 hashCode() 和 equals() 方法，这样一来，这个对象完全可以当作一个 key 存放到 map 中了。当然，我知道这时候肯定有朋友会问， 那 nacos 服务端怎么创建 Service 对象呢？又怎么把客户端注册过来的服务实例所属服务的信息封装到 Service 对象中呢 ？别忘了，nacos 客户端发送过来的 InstanceRequest 请求中本来就封装着服务实例所属服务的信息，所以服务端直接就可以在 InstanceRequestHandler 请求处理器的 handle() 方法中得到服务实例所属的服务信息，这样一来，InstanceRequestHandler 请求处理器就可以重构成下面这样，请看下面代码块。  
到此为止大家应该都清楚了 nacos 客户端注册的服务实例信息究竟应该怎么存放到服务端了吧？总之就是存放到一个 map 中，并且每一个客户端注册服务实例信息，还会根据服务实例所在的服务存放到内嵌的 map 中。当然，我们目前实现的程序肯定还不完美，确切地说有很多功能还没有实现，仅仅是引入了一个简单的 Service 类，InstanceRequestHandler 请求处理器的 handle() 方法还根本没实现呢。但是我迟迟不具体实现这个请求处理器是有原因的，因为我总觉得把 map 定义在 InstanceRequestHandler 请求处理器中的方式不太完美。而我之所以这么觉得，是因为我考虑到了更多的内容。  
  
请大家想一想，nacos 客户端把服务实例信息注册到服务端之后，这就完了吗？对 nacos 客户端来说确实是把重要的工作执行完毕了，但是对 nacos 服务端来说，存放了客户端注册的服务实例信息只是第一步，接下来服务端要考虑的，要做的工作就太多了。比如， 服务端还要把更新之后的配置信息主动推送给订阅了信息的客户端；如果引入集群之后，那么每当一个客户端注册了新的服务实例之后，这些信息肯定也要在集群之间传输复制吧 ？照这样看，大家肯定能意识到不管怎么样，我们的程序肯定还要再定义一些新的组件，也许是处理服务订阅相关操作的组件，也许是在集群间复制传输数据的组件，总之这些组件都要获得客户端注册到服务端的服务实例信息，才能继续履行自己负责的功能。而客户端注册的服务实例的信息都存放在 InstanceRequestHandler 请求处理器的 clients 成员变量中，在我看来，这显然是不合适的。  
  
因为 InstanceRequestHandler 请求处理器只负责处理注册服务实例的 InstanceRequest 请求，从单一职责上来讲，这个处理器肯定不必和处理服务实例信息推送，集群数据传输的组件产生关系 。所以， 我认为不应该把存放服务实例信息的 map 定义在 InstanceRequestHandler 请求处理器中，而是应该再定义一个新的、单独的组件，让这个组件专门存放客户端注册过来的服务实例信息 。这样一来，服务实例信息推送组件，集群数据传输组件，还有其它组件都可以直接从这个新定义的组件中获得客户端注册的服务实例信息，然后执行其他操作。 而这个新的组件我早就想好了，就把它定义为 ConnectionBasedClient 类，这个类的对象就是一个由 nacos 服务端维护的客户端对象，服务端每接收到一个客户端连接，就要创建一个对应的客户端对象，这样一来，nacos 客户端注册的服务实例信息都可以存放到服务端的与其对应的客户端对象中，也就是说 nacos 客户端注册的服务实例信息都有其在服务端对应的客户端对象维护 。  
  
构建 Client 体系，引入 ConnectionBasedClient 类  
  
在上一小节结尾，我们只是用文字简单描述了一下新引入的 ConnectionBasedClient 类，接下来，就请大家先看看这个类的简单代码，请看下面代码块。  
上面代码块就是我目前定义好的 ConnectionBasedClient 类，可以看到，我在这类中定义了一个 connectionId 成员变量，代表当前客户端对象的客户端连接 Id，这个成员变量的作用不用我解释，大家应该都可以理解。但我随后又定义了一个 publishers 成员变量，它是一个 map，其中 key 就是客户端注册过来的服务实例所属服务的信息，value 就是真正的服务实例信息。和我们最开始定义的 map 不同，最开始在 InstanceRequestHandler 请求处理器中定义的 map 还内嵌了一个 map，并且 map 的 key 是客户端连接 Id，现在我把客户端连接 Id 给省略了是因为我们已经知道了每一个 nacos 客户端都对应一个服务端的 ConnectionBasedClient 客户端对象，本来就是一对应的关系，只要知道客户端连接 Id 就可以根据客户端连接 Id 找到对应的 ConnectionBasedClient 对象，而 客户端连接 Id 也是 ConnectionBasedClient 类的成员变量，因此存放服务实例信息的 map 就不必再使用客户端连接 Id 当作 key 了。除此之外，就是两个还未实现的方法了，其中 addServiceInstance() 就是把客户端注册的服务实例信息存放到 publishers 这个 map 中的方法，而 removeServiceInstance() 就是把服务实例信息从 map 中移除的方法。有注册服务实例的操作，肯定就有注销服务实例的操作，这个大家可以理解吧。  
  
好了，现在大家对 nacos 服务端存放客户端注册的服务是信息的方式肯定更清楚了，当然，随着 ConnectionBasedClient 类的引入，我相信大家心里肯定也产生了几个问题，有一种知道自己快触及到本质了，但就是被什么东西阻隔住了的感觉。文章是我写的，我当然知道大家困惑的地方在哪里，所以接下来就让我把困扰大家的一些问题列举出来，然后我们一起分析解决这些问题，  
1 nacos 服务端每接收一个客户端连接，就要为这个客户端创建一个对应的 ConnectionBasedClient 连接对象，那么这个 ConnectionBasedClient 对象是在哪里被创建的呢？  
2 nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象应该怎么存储呢？因为每一个客户端对应一个唯一的 ConnectionBasedClient 对象，如果想维持一种清楚的映射关系的话，那肯定以 key-value 键值对的方法存放到一个 map 中最为合适，其中 key 就是客户端连接 Id，value 就是对应的 ConnectionBasedClient 对象。这样一来，nacos 服务端只需要根据客户端连接 Id 就可以直接找到与客户端对应的 ConnectionBasedClient 对象，然后执行自己想要的操作即可。分析了这么多，那么存放客户端连接 Id 和 ConnectionBasedClient 对象的 map 应该定义在哪里呢？或者说是不是需要引入新的组件来管理 nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象呢？比如说引入一个客户端管理器？  
3 为每一个客户端创建了 ConnectionBasedClient 对象之后，也按照刚才分析的映射关系存储好了 ConnectionBasedClient 对象之后，当 nacos 客户端注册服务实例信息到客户端时，这些信息该怎么存放到与 nacos 客户端对应的 ConnectionBasedClient 对象中呢？  
4 InstanceRequestHandler 请求处理器该如何重构呢？ConnectionBasedClient 需要被定义为 InstanceRequestHandler 请求处理器的成员变量吗？  
以上 4 个问题就是随着本章内容的结束，产生的最新的、也是最重要的 4 个问题。当然，我并没有忘记我们还有一个老问题没有解决，但是随着以上 4 个问题的解决，老问题也会被我们解决了。这一章到此就结束了，确实，这一章内容非常少，其实我本来可以多写点，但是我想跟大家解释一下，要解决以上 4 个问题，会向程序中引入非常多新的组件，虽然确实可以在本章率先引入一两个组件，但是为了保证知识和代码的连贯性，我还是打算把新的组件放到下一章引入。所以，下一章的内容会非常多，这一章的篇幅少点，就当是一个休息站，用来给大家休息休息，准备好之后，就可以阅读下一篇文章了。  
  
还有一点要补充的是，本章展示的代码也可以去我提供的第七版本代码中查看，看的时候大家就会发现我在文章中对 ConnectionBasedClient 类做了简化，实际上它还有一个 AbstractClient 父类，并且 AbstractClient 父类还实现了 Client 接口，这些内容就交给大家自己去文章中查看吧。当然，现在也可以不去看文章，我建议大家等等再看，等看完下一章或者是后两章之后再去看代码，到时候第七版本代码对应的文章应该就全部结束了。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/nqqvkhq4cu6tk67y*  
*All content belongs to its respective owners and creators.*