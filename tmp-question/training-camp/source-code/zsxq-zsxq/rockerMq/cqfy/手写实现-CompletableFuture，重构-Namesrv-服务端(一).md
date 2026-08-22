朋友们，在上一章最后一小节我为我们的程序引入了一个 ResponseFuture 类，并且使用这个类重构了 Namesrv 服务端发送请求的和新方法，也就是 NettyRemotingAbstract 类的 invoke0() 方法。当然，尽管我们对程序进行了多次重构，在上一章仍然留下了一个小问题： 那就是目前我们实现的发送请求的方法，根本没有对请求发送失败、或者异常情况的处理 。这一章就让我们一起把这个问题解决了。  
  
剖析 Namesrv 服务端发送请求的伪异步情况  
  
要想解决上一章遗留的问题，我想先请大家思考一下，在我们曾经见过的一些流行框架中，在客户端向服务端发送请求的过程中出现异常了会怎么办呢？就比如说我们刚刚更新完毕的 Nacos 框架吧，在 Nacos 框架的客户端向服务端发送消息时，也就是发送请求时，出现异常了会执行什么操作呢？也许大家已经忘记这部分的内容了，毕竟这是 Nacos 课程最前面的内容，大家肯定已经没有多少印象了，所以我把相关的代码搬运过来，请大家简单回顾一下，请看下面代码块。  
package com.alibaba.nacos.common.remote.client;  
  
  
public abstract class RpcClient implements Closeable {  
//省略其他内容  
//RPC客户端配置信息对象

protected RpcClientConfig rpcClientConfig;  
//当前客户端和服务端的连接

protected volatile Connection currentConnection;  
//客户端最新的收到服务器消息的时间戳

private long lastActiveTimeStamp \= System.currentTimeMillis ();  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/4/11

\* @方法描述：客户端向服务端发送请求的方法

\*/

public Response request (Request request) throws NacosException {

//rpcClientConfig.timeOutMills()得到的是请求超时时间，如果用户没有定义，那么默认为3秒，这个3秒默认时间是客户端配置类中定义好的

return request (request,rpcClientConfig.timeOutMills ());

}  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/4/11

\* @方法描述：真正向服务端发送请求的方法

\*/

上面代码块的内容虽然很多，但是逻辑非常简单，而且我们关注的重点也很直接，就是看看在 Nacos 客户端向服务端发送请求的时候会出现什么异常情况。 从 RpcClient 类的 request(Request request, long timeoutMills) 方法中我们可以看到：当客户端接收到的服务端回复的响应为空或者为错误响应时，就会在 RpcClient 类的 request() 方法中抛出异常 ，还有一个地方也会抛出异常， 那就是在执行 response = this.currentConnection.request(request, timeoutMills) 这个操作的时候 ，我把这个操作的相关代码也展示在上面代码块中了，大家可以看到， 在执行这个操作的时候，因为会同步等待响应到来，如果定义了超时时间但是在规定时间内没有接收到响应，那么 response = this.currentConnection.request(request, timeoutMills) 这个操作也会抛出异常 。那么这些抛出的异常是怎么被处理的呢？在 RpcClient 类的 request(Request request, long timeoutMills) 方法中我们可以看到，如果真的抛出异常了，就意味着这次请求发送失败了，这里的失败指的是请求的完整流程失败了，也就是发送请求——接收成功响应这个流程失败了， 这个时候在 RpcClient 类的 request(Request request, long timeoutMills) 方法中就会重新发送请求，直到请求重试的次数达到阈值 。 如果多次请求重试之后发送请求的操作仍然没有执行成功，RpcClient 类的 request(Request request, long timeoutMills) 方法就会把异常外抛，交给外层方法处理 。而所谓外层方法，就是调用 RpcClient 类的 request(Request request, long timeoutMills) 方法的方法，在 Nacos 框架源码中，在 NamingGrpcClientProxy 类的 requestToServer() 方法中调用了 RpcClient 类的 request() 方法，我把具体的内容也搬运过来了，请大家回顾一下，请看下面代码块。  
以上就是 Nacos 框架中，客户端向服务端发送请求的过程中出现异常之后的处理操作。从展示的整个流程可以看到， 实际上 Naocs 客户端处理异常的方式非常简单，如果发送请求操作失败了，也就是捕捉到异常了，就会先执行请求重试的操作，请求重试之后仍然操作失败，这个时候就会把异常外抛，交给外层方法处理，而外层方法处理异常的操作也很简单，无非就是记录一下请求发送失败的次数 。这些逻辑大家应该都能看懂吧？当然，我在上面给大家展示了一大堆代码，帮助大家回顾之前框架的内容，这并不是说我想为大家重点讲解如何处理请求发送过程中出现的异常，实际上处理异常并不是我们要关注的主要内容，因为每个框架和每个框架并不相同，处理异常的方式也并不会全部相同，可能有的框架会在请求发送失败后执行重试的操作，而有的框架所做的只是记录失败操作日志而已。 我们真正应该关心的是如何抓住异常 ，也就是说， 能让程序准确捕捉到请求发送过程中出现的异常，这个才是我们要关注的重点，也是每个框架的通用功能 ，至于捕捉到异常之后执行什么操作，我们就先不必关心了。  
  
好了，如果大家理解了刚才的内容，那么现在就让我们回到正在构建的 Namesrv 服务端上，目前我们正在实现 Namesrv 服务端对外发送请求的功能，并且已经给 Namesrv 服务端定义了三种对外发送请求的方式，请看下面代码块。  
从上面代码块中可以看到，Namesrv 服务端对外发送请求一共有三种方式，一种是同步发送请求，一种是异步发送请求，一种是发送单向请求。不管程序使用哪种方式发送请求，根据我们刚才对 Nacos 框架发送请求过程的分析，现在我们肯定都应该清楚了， 只要发送请求过程中出现问题，那么我们就应该定义相关的异常，把异常抛出来以便让程序捕获到 ，至于程序捕获到之后的操作，我们可以先不必关心。如果大家理解了这一点，那接下来我们就可以来分析一下，在 Namesrv 服务端对外发送请求的过程中会出现什么异常。  
  
如果 Nacos 客户端向服务端发送了一个同步请求，那么客户端就需要在当前线程中同步等待响应，如果没有定义超时时间，那么线程就会一直阻塞等待服务端回复响应。而 Nacos 客户端使用的同步方式就是 Future，只要调用 Future 的 get() 方法就可以让发送请求的线程阻塞等待响应到来，这一点我刚才已经在代码中为大家展示过了。而回到 Namesrv 的服务端， 我们会发现 Namesrv 的服务端是由 Netty 构建的，而使用 Netty 构建的客户端和服务端，请求的发送和响应的接收最终都是由 Netty 内部的单线程执行器来处理的 。就比如说我们上一章重构完毕的 NettyRemotingAbstract 类的 invoke0() 方法吧，该方法是 Namesrv 服务端真正把请求发送出去的方法，在该方法中会执行获得信号量许可证操作，然后再执行发送请求的操作，并且还为每一个发送成功的请求创建了对应的 ResponseFuture 对象，然后把 ResponseFuture 对象放到了响应表中。如果大家对这些内容还有印象，那肯定就还记得，在 NettyRemotingAbstract 类的 invoke0() 方法中请求发送完毕之后就直接退出当前方法了，当前线程并没有等待接收响应。我把上一章重构完毕的 NettyRemotingAbstract 类的 invoke0() 方法也搬运过来了，请大家简单回顾一下，请看下面代码块。  
从上面代码块中可以看到，如果 Namesrv 服务端使用同步模式对外发送请求，那么发送请求的线程，也就是执行了 NettyRemotingAbstract 类的 invokeSyncImpl() 方法的线程并没有在 invoke0() 方法中同步等待响应的到来。 而响应真正被接收的操作是在 Netty 的 IO 事件处理器中执行的，也就是在 NettyRemotingServer 类的 NettyServerHandler 处理器的 channelRead0() 方法中被接收到，并且响应到来之后，就会把接收到的响应设置到事先创建好的 ResponseFuture 对象中 ，就像下面代码块展示的这样，请看下面代码块。  
这么分析下来，好像在 Namesrv 服务端中，发送请求和接收响应的操作确实不在同一个线程中执行，看起来好像是一个异步发送请求地操作，一个线程把请求发送出去了，然后就直接退出发送请求的方法，等到对方回复了响应，Netty 的单线程执行器会接收到响应，然后把响应设置到事先创建完毕的 ResponseFuture 对象中。虽然我们最后分析出来，发送请求和接收响应的操作在不同的线程中执行，但我可以很明确地告诉大家，这绝对不是真正的异步发送请求的操作。如果真的是异步发送请求的操作，那么 Namesrv 内部的 invokeSyncImpl() 方法就失去了意义。 实际上这只是一种伪异步情况 ，现在大家还不明白是因为我并没把 invokeSyncImpl() 方法彻底重构完整，等重构完毕之后，大家就清除这一切是怎么回事了。  
  
我相信每一位朋友肯定都使用过 JDK 的 Future 功能，线程池异步执行任务的时候，可以把任务执行结果封装在一个 Future 对象中返回给用户，只要用户调用 Future 对象的 get() 方法，就可以获得任务的执行结果。就像下面代码块中展示的这样，请看下面代码块。  
从上面代码块中可以看到，当执行器异步执行我们定义的任务时，主线程也在使用 Future 对象的 get() 方法异步获得任务结果，只要执行器没有执行完任务，这个时候主线程就会一直在 get() 方法阻塞住，直到任务执行完毕之后，主线程才会继续向下执行。 这就是一个典型的伪异步情况，主线程必须等待任务被异步执行完毕之后才能继续向下执行 。这个逻辑对大家来说应该很简单吧？如果这个逻辑都理解了，那接下来就可以再次回到 Namesrv 服务端了。现在 Namesrv 服务端同步发送请求的方法还非常简陋，一点也不完善。 因为我们目前为 Namesrv 服务端定义的同步发送请求的方法只是做到了发送请求，请求发送完毕后就退出方法了，并没有向 Nacos 那样同步等待响应 。 如果我们想让 Namesrv 服务端做到真正地同步发送请求，就必须要在同步发送请求的方法中同步等待响应，这也就是说，在响应没有获得之前，发送请求的线程不能执行其他的操作，必须要阻塞等待响应到来 。但我们都知道，Namesrv 服务端接收接收响应的线程是 Netty 单线程执行器，发送请求是在另外的线程中执行的，两个不一样的线程怎么能做到同步呢？不错，答案已经很清晰了，这不就是我刚才为大家展示的测试类中伪异步的情况吗？只要我们用上 Future，让发送请求的线程在发送了请求之后就调用 Future 的 get() 方法，不就可以阻塞等待响应了吗？当然，我这么说肯定会让大家觉得困惑，好像这句话没有任何铺垫就被我写出来了，别着急朋友们，接下来就让我用刚才分析的思路重构一下 Namesrv 服务端的 invokeSyncImpl() 方法，重构完毕之后，大家就清楚是怎么回事了。  
  
使用 CompletableFuture 重构 invoke0() 方法  
  
按照我们上一小节的分析，要想让两个不相关的线程在一定程度上实现同步操作，可以使用 Future 来实现相关功能。 比如说在 Namesrv 服务端对外发送请求的时候，可以让一个线程发送请求，发送请求的方法会返回一个 Future 对象，然后让发送请求的线程直接调用 Future 对象的 get() 方法，让发送请求的线程阻塞。而当 Netty 的单线程执行器得到了对应的响应后，就可以把响应设置到 Future 对象中，表示任务已经执行完毕，可以通过 get() 方法得到响应了。这个时候，发送请求的线程就可以从 get() 方法中得到响应结果，结束阻塞，然后继续向下执行了 。这个流程大家可以理解吧？而且我相信大家肯定也都还记得，在上一章重构 NettyRemotingAbstract 类的 invoke0() 方法时，我们已经引入了一个 ResponseFuture 类，并且 Netty 的单线程执行器接收到的响应就可以设置到这个 ResponseFuture 对象中。我把 ResponseFuture 类的代码也搬运过来了，请大家再次回顾一下，请看下面代码块。  
在看了上面的代码块之后，也许有朋友会以为我是想使用 ResponseFuture 类的对象让发送请求的线程阻塞，如果这么想就大错特错了，虽然 ResponseFuture 对象确实可以获得 Netty 单线程执行器接收到的响应，并且我们还可以通过请求响应唯一 Id 确立请求和 ResponseFuture 的一对一关系，这个关系确立了，那么请求和响应的关系也就确立了，但是我根本就没有给 ResponseFuture 定义能让线程阻塞的 get() 方法啊。显然我们需要使用另外的 Future 来实现同步发送请求的功能。那么应该直接使用 JDK 的 Future 吗？很明显，我也不会使用 JDK 的 Future，原因很简单，JDK 的 Future 功能太简单了，只具备使线程阻塞的功能， 我需要的 Future 除了具备阻塞线程的功能之外，还可以显示设置任务执行结果，原因很简单，因为我需要在 Netty 的单线程执行器中把接收到的响应手动设置到 Future 中，然后调用了 Future 的 get() 方法的线程就可以结束阻塞，继续向下执行了 。这个功能是 JDK 原生的 Future 不具备的，除此之外， 我要使用的 Future 还可以定义回调方法，这个回调方法会在任务执行完毕之后被自动回调，在异步发送请求的操作中都会定义回调方法，这时候就可以直接使用 Future 的这个功能，把要回调的方法先注册到 Future 中，等到 get() 方法得到了响应结果，那些回调方法就可以直接被回调了 。而这个功能 JDK 原生的 Future 同样不具备。  
  
分析到这里，大家可能以为我会自己定义一个新的 Future 类，只需要实现 JDK 的 Future 接口，或者直接继承 JDK 的 FutureTask 类，重写其中的部分方法，以达到我想要的效果。确实，这么做完全可以，而且自由度非常高，我们可以按照自己的意愿实现一个功能完备的 Future。但现在我并不想这么做， 因为已经有一个非常完美的、现成的 Future 类摆在我们面前了，那就是 CompletableFuture 类 。接下来让我给大家解释一下我要使用 CompletableFuture 的几点原因：  
1 CompletableFuture 提供了 get() 方法获得任务执行结果，可以让调用该方法的线程阻塞等待获取任务执行结果，并且可以限时等待或非限时等待。  
2 CompletableFuture 可以注册回调方法，并且可以注册多个回调方法，甚至是回调方法链，这些回调方法在任务执行完毕后都会被回调。  
3 CompletableFuture 提供了可以显示设置任务结果的 complete() 方法，只要调用该方法把某个结果设置到 CompletableFuture 中，就意味着任务执行完毕了，调用 CompletableFuture 对象 get() 方法的线程就可以结束阻塞，获得任务结果继续向下执行了，那些回调方法也都可以被回调了。  
4 CompletableFuture 还有非常重要的一个功能，那就是在程序执行期间出现异常了，可以直接使用异常设置任务结果，当使用异常设置了任务结果后，程序就可以根据 CompletableFuture 中抛出的异常执行不同的操作了。而这正是我们接下来需要的功能，因为从文章一开始我就跟大家说了，我们要实现 Namesrv 服务端对外发送请求时对异常情况的处理。当然，如何处理并不是我们关心的，我们真正关心的是如何让程序捕捉到异常，现在我们就可以使用 CompletableFuture 的这个功能让程序捕捉到异常 。  
  
分析完了 CompletableFuture 的几个优点之后，那我们应该如何使用 CompletableFuture 来重构程序呢？让发送请求的线程同步等待响应的操作非常容易实现，只需要 在 Namesrv 服务端对外发送请求的时候，让发送请求的线程在发送请求的方法中会返回一个 CompletableFuture 对象，让发送请求的线程再调用 CompletableFuture 对象的 get() 方法，让线程阻塞等待响应即可。而当 Netty 的单线程执行器得到了对应的响应后，就可以把响应设置到 ResponseFuture 对象中，注意这时候已经把响应设置到 ResponseFuture 对象中了。但我们的目的是要把响应设置为 CompletableFuture 的执行结果，这个也很好说，那就直接让 Netty 的单线程执行器调用 CompletableFuture 的 complete() 方法，把得到了响应对象的 ResponseFuture 对象设置为 CompletableFuture 对象的执行结果即可。这样一来就意味着任务已经执行完毕，发送请求的线程可以结束阻塞，通过 get() 方法得到响应了 。这样一套完整的完整的流程执行下来，Namesrv 同步发送请求的操作就执行完毕了。  
  
当然，我们目前分析的只是同步发送请求、同步等待响应结果的实现思路，那么当发送请求的过程中出现了异常该怎么办呢？要想解决这个问题，我们就应该先梳理清楚，在 Namesrv 服务端对外发送请求的过程中会出现什么异常？这个问题其实非常容易分析，在上一小节我为大家展示的 Nacos 客户端向服务端发送请求的过程中出现的异常情况有很多，不仅有对响应异常的处理，还有对请求超时异常的处理，但在 Namesrv 服务端对外同步发送请求的过程中，并不需要对响应进行判断和处理，也就是说，我们没必要因为响应失败就抛出相关的异常。原因很简单， 发送请求的操作是在 NettyRemotingAbstract 类的 invoke0() 方法中执行的，如果根据我们刚才的思路重构该方法，那在这个方法中，发送请求的线程只会把请求发送出去，然后就给外层方法返回一个创建完毕的 CompletableFuture 对象，也就是说真正获得响应的操作并不是在 NettyRemotingAbstract 类的 invoke0() 方法中执行的，而是在调用了该方法的外层方法中执行的，其实就是在 NettyRemotingAbstract 类的 invokeSyncImpl() 方法中同步得到了响应 。这一点大家很快就会在重构之后的代码中看到了。那既然 NettyRemotingAbstract 类的 invoke0() 方法不必关心响应结果，那么它需要关注的异常就只有一个请求超时异常了， 所以如果 Namesrv 服务端在 invoke0() 方法中发送请求超时了，那我们就可以创建相关异常，然后把异常设置到 CompletableFuture 对象中交给程序处理即可 。如果大家能理解这个逻辑，那接下来就请大家看一下我重构完毕的 NettyRemotingAbstract 类，请看下面代码块。  
到此为止，我就把 Namesrv 服务端对外发送请求的功能彻底重构完毕了，之前提出的很多问题也都解决了。大家一定要认真阅读上面代码块的内容，或者直接去我提供的第二版本代码中阅读相关内容也行，上面代码块中的内容虽然很多，但是注释非常详细，我就不再重复讲解了，大家只要按照顺序阅读上面代码块的内容，肯定能看懂其中的逻辑。当然，这有一个非常大的前提，那就是大家对 CompletableFuture 类的内部原理非常清楚，知道该对象的每一个操作是怎么执行的，以及在什么时候执行的。就比如说，我再给大家展示一个 CompletableFuture 的测试类，大家可以看看这个测试类中注册到 CompletableFuture 对象的回调方法都是什么时候被执行的。请看下面代码块。  
在上面的测试类中，我首先创建了一个变量名称为 cf 的 CompletableFuture 对象，然后向这个 cf 对象中注册了名称为 chain1 的任务，接着又向 chain1 中注册了包括 chain2 在内的两个任务，然后又向 chain2 内部注册包括 chain3 在内的两个任务，在这种情况下，测试类一旦启动，这些任务的执行顺序是怎么的呢？还有一点，为什么调用了 cf 对象的 complete() 方法，设置了任务的执行结果之后，所有任务都可以被执行了呢？以下是这个测试类的执行结果，请看下面代码块。  
大家可以先思考一下上面这个例子中任务的执行过程，然后再看看另一个测试类中任务的执行过程，请看下面代码块。  
这个测试类和刚才的测试类的不同点在于，当前的测试类向 cf 对象上注册了三个任务，然后又向这三个任务中分别注册了一个任务，这个测试类的执行结果如下，请看下面代码块。  
大家思考一下这个测试类的执行结果为什么是这样的，这个例子看完了之后，大家可以再看看下面一个测试类，请看下面代码块。  
上面这个测试类的内容非常简单，就是不断地注册新的任务，新的任务总是注册到前一个任务上，也就是前一个 CompletableFuture 对象上。这个测试类的执行结果如下，请看下面代码块。  
看到这里，我相信有些朋友肯定已经晕了，不明白为什么这几个测试类中任务的执行顺序都不相同。我能理解大家的这种感受，因为我自己在阅读 CompletableFuture 源码的时候，搞了很多测试类验证自己的判断是否正确，好几次也差点晕了。当然，到这里还没有结束，接下来再请大家看一个和 CompletableFuture 处理异常相关的测试类，请看下面代码块。  
上面测试类的内容非常简单，只是定义了一个 CompletableFuture 对象，然后向该对象中注册了一个任务，之后就调用了 get() 方法阻塞等待任务执行结果。唯一需要注意的是，在任务开始执行之前，就直接使用一个 TimeoutException 超时异常给 cf 对象设置了任务执行结果。现在我们关心的是，设置的这个 TimeoutException 超时异常会被哪个 catch 代码块捕捉到呢？我把上面测试类的执行结果展示在下面了，请看下面代码块。  
可以看到，虽然我们向 CompletableFuture 对象中设置的是 TimeoutException 异常，但是最终是 ExecutionException 这个 catch 代码块捕捉到了异常。这是为什么呢？还有一点值得关注的是，为什么调用 CompletableFuture 对象的 completeExceptionally() 方法之后，把异常设置为当前 CompletableFuture 对象的任务执行结果之后，注册到这个 CompletableFuture 对象中的任务就不会再执行了呢？这些可能都是让大家困惑的地方，为了让大家彻底弄清楚以上几个测试类的执行结果，搞明白 CompletableFuture 内部的原理，接下来就让我为大家从零到一，一点点实现一个简易的 CompletableFuture 类。  
  
搭建 CompletableFuture 的骨架  
  
要想模仿 JDK 源码实现一个自己的 CompletableFuture 类，首先应该弄清楚 JDK 设计 CompletableFuture 类的原理，在 1.5 的 JDK 源码中，还没有定义 CompletableFuture 类，大家使用的都是是 Future 类。后来因为 Future 使用起来实在是不方便，不能定义回调方法，也不能手动设置任务结果，在 1.8 版本的 JDK 中终于引入了 CompletableFuture 类。CompletableFuture 类的功能非常丰富，不仅可以手动设置任务结果，还可定义回调方法，甚至还可以设计组合任务链。可以看到，CompletableFuture 的功能确实非常丰富，但我想跟大家说的是，CompletableFuture 具备的众多功能也许会让我们眼花缭乱，但我们千万别忘了作为一个 Future，它最基本的功能是什么： 简单来说，CompletableFuture 最基本的功能仍然是让线程调用其 get() 方法获得任务执行结果，如果没有执行结果，线程则直接阻塞，获得任务结果之后，线程才能结束阻塞继续向下运行 。  
  
我能想到有些朋友可能不太认同我这个观点，因为 CompletableFuture 显然可以直接注册回调方法，在很多真正异步的情况下，只需要把要执行的任务注册到 CompletableFuture 对象中即可，然后线程就可以直接去做别的事了，不需要调用 CompletableFuture 的 get() 方法让线程阻塞等待。确实是这样，我当然知道这种情况，但是请听我继续解释一下： 假如我们创建了一个 CompletableFuture 对象，然后向这个 CompletableFuture 对象中注册了多个回调方法，不管怎么样，这些回调方法肯定都会等待这个 CompletableFuture 的任务结果产生了才会按顺序执行，也就是说，只有当 CompletableFuture 对象调用了它的 complete() 方法，注册到该 CompletableFuture 对象上的回调方法才会被执行 。这个逻辑大家可以理解吧？也就是说，只要使用了 CompletableFuture 对象来执行任务，不管怎么样，不管是手动设置还是自动设置，都需要等待这个 CompletableFuture 对象设置了它的任务结果，其他回调方法才能被执行。这个等待的机制，就是 CompletableFuture 最基本的功能，也是它的核心之一。从代码层面上来说，这个等待的机制非常容易实现，所以现在我们要定义自己的 CompletableFuture 类了，不妨就先从这个等待机制开始实现。  
  
当然，现在我们肯定还没想好怎么实现，但这并不能妨碍我们首先构建好 CompletableFuture 类的基本骨架。我现在的思路非常清晰，按照刚才分析的思路，要想实现 CompletableFuture 类的等待机制，首先就应该在该类中定义一个用来表示执行结果的成员变量，就比如说我们可以定义一个 Object 类型的成员变量，变量名称就定义为 result，只要这个 result 被赋值了，那么就可以执行后续操作了，阻塞的线程也就可以继续执行了。而 result 被复制的方法可以有很多种，因为 result 既可以被常规结果赋值，也可以被异常赋值，就目前的情况来说，我们先实现 result 被常规结果赋值的功能吧，那我们可以在 CompletableFuture 类中定义一个 complete() 方法，使用该方法给 CompletableFuture 的 result 成员变量赋值。除此之外，肯定还要再定义两个 get() 方法，一个是无限时等待结果的 get() 方法，一个是有参数的限时等待结果的 get() 方法，有了这两个方法之后，想要等待结果的线程就可以阻塞了。接下来我就可以先把简单定义完毕的 CompletableFuture 类展示给大家，请看下面代码块。  
上面代码块中的内容非常简单，就是定义了一个成员变量和三个空方法。也许会有朋友感到困惑，在这三个空方法中，前两个 get() 方法确实不太容易实现，因为一点调用了 get() 方法，不管是否限时，调用 get() 方法的线程肯定就要阻塞了，让一个正在运行的线程直接阻塞，这个功能实现起来确实有点麻烦，所以前两个 get() 方法没有实现也可以理解，但第三个 complete() 方法为什么不实现呢？这个方法不就是给 result 成员变量赋值吗？直接赋值不就完事了？事情根本没有想象得那么简单，大家可千万别忘了， 当一个 CompletableFuture 对象获得了它得执行结果，这个时候调用了它的 get() 方法的线程就要结束阻塞，并且注册到这个 CompletableFuture 上的回调方法也要被执行 。这也就意味着在 complete() 方法中给 result 成员变量设置了结果之后，就要接着执行唤醒阻塞线程，以及执行回调方法得操作，但现在我们根本就没实现让线程阻塞的功能，也没有向 CompletableFuture 注册什么回调方法，所以 complete() 方法肯定也无法实现。  
  
除此之外，我还想到了一个问题，不管我们要为自己的 CompletableFuture 定义什么内容，说到底它仍然是一个 Future，它的基本功能都是从 JDK 的 Future 那里获得的， 那显然我们自己实现的这个 CompletableFuture 类也应该实现 JDK 的 Future 接口，Future 接口中的方法也应该实现了 。这会我就不给大家展示 JDK 的 Future 接口中都有什么内容了，大家肯定都清楚，接下来我就直接把实现了 Future 接口的 CompletableFuture 类展示给大家，请看下面代码块。  
好了朋友们，现在我已经为大家把实现了 Future 接口的 CompletableFuture 类展示完毕了，虽然目前定义的 CompletableFuture 类中各个方法几乎都没有实现，但一个最基本的骨架已经搭建完毕了，接下来就可以进一步填充细节了。我首先想做的就是实现上面代码块中的两个 get() 方法，也就是实现让线程不限时和限时阻塞的方法，和其他方法比起来，这两个方法实现起来更简单一些。 我只需要在 get() 方法中先判断 CompletableFuture 对象的 result 成员变量是否被赋值了，如果被赋值了，那么直接返回 result 即可；如果没有被赋值，那就阻塞线程，让线程等待 result 被赋值 。而让线程阻塞的方式有很多， 其中最常用的就是使用 LockSupport 的 park() 方法，如果是不限时阻塞，那就可以直接调用 LockSupport.park(object) 方法即可，如果是限时阻塞，那就可以调用 LockSupport.parkNanos(object, nanos) 方法 。当然， LockSupport 要操作的肯定是同一个线程 。 当线程调用了 CompletableFuture 的 get() 方法获取 CompletableFuture 的结果时，如果没有结果，那么就可以使用 LockSupport 使调用 get() 方法的线程阻塞，同时把这个线程记录下来，等到 CompletableFuture 调用了它的 complete() 方法设置了任务结果，就可以在该方法中直接使用 LockSupport 唤醒调用了 get() 方法的线程 。这个思路大家应该都可以理解吧？按照这个思路，那么 CompletableFuture 类就可以先重构成下面这样，请看下面代码块。  
以上就是我重构之后的 CompletableFuture 类，可以看到，目前的 CompletableFuture 类非常简单，简单就意味着不严谨，不严谨就意味着有漏洞。这个我也承认，重构之后的 CompletableFuture 类确实存储在诸多问题：  
1 目前的 CompletableFuture 类并不能处理执行结果为 null 或者为异常的情况。  
2 假如多个线程同时调用了 CompletableFuture 类的 complete() 方法，同时操作 result 成员变量的时候，很容易出现并发问题。  
3 目前的 CompletableFuture 对象只能记录最新调用 get() 方法的线程，如果多个线程同时调用了 CompletableFuture 对象的 get() 方法，那只有最后调用 get() 方法的线程会被记录下来，唤醒的时候也只有这个线程会被唤醒。  
4 线程也是可以被中断的，一个线程调用了 CompletableFuture 的 get() 方法，阻塞等待任务结果，但是也可以在等待的过程中被中断，取消等待操作，这个功能我们并没有实现。  
5 限时阻塞的 get() 方法在等待超时后，没有对结果进行是否为 null 判断，也没有抛出超时异常 。  
以上五点就是目前的 CompletableFuture 类存在的很明显的问题，先不管其他问题，只关注上面第三个问题的话，我相信很多朋友都会给我建议，让我把 CompletableFuture 类中的 thread 成员变量替换成一个存储 Thread 对象的集合，这样一来每当一个线程调用了 CompletableFuture 的 get() 方法，就可以把这个线程添加到集合中，等到要唤醒的时候，直接把集合中的所有线程依次唤醒即可。就像下面代码块展示的这样，请看下面代码块。  
好了，我又对 CompletableFuture 类进行了一次重构，重构之后的 CompletableFuture 类确实在一定程度上解决了并发问题，也解决了多线程阻塞的问题，但是这个 CompletableFuture 类使用了大量的同步锁，看起来显然更笨重了。如果大家没看过 JDK 的源码，也许还不会觉得我们自己实现的这个 CompletableFuture 类有多笨拙，反而会觉得代码很常规，通俗易懂。但我要说的是，只是做到这个程度是不够的，我相信现在大家肯定从设计原理上已经明白了 CompletableFuture 里的 get() 方法和 complete() 方法要执行的操作，所以接下来我会使用 JDK 源码的方式再次重构 CompletableFuture 类，同时解决上面列出的其他几个问题。当然，这些内容在这一章显然是讲不完了，就留到下一章讲解吧。诸位，我们下一章见！  
  
  
  
附：等第八篇文章更新完毕之后，大家就可以去看我提供的第二版本代码了。  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/rhge5bg4zsn928v5*  
*All content belongs to its respective owners and creators.*