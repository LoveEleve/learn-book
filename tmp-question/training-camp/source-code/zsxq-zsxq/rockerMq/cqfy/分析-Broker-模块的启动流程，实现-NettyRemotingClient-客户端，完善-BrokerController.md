上一章我已经为大家把 Broker 模块的 BrokerStartup 启动器和 BrokerController 类定义完毕了，可以说一个简单的 Broker 模块的架子已经搭建起来了，现在需要做的就是把这个 Broker 模块的核心功能实现了。所谓核心功能其实也很简单，就目前的情况来说，我们定义的这个 Broker 模块的核心功能无非就三个：  
1 在定时任务中定期获得最新的可用的 Namesrv 地址列表。  
2 在定时任务中定期把自己的信息发送给所有的 Namesrv 节点，这个操作就相当于 Broker 模块和 Namesrv 模块的心跳检测功能。如果 Namesrv 在规定时间内没有收到 Broker 模块发送的信息，就可以认为对应的 Broker 模块出故障了。  
3 把客户端对象真正定义出来，因为 Broker 定期向 Namesrv 发送消息，肯定需要使用客户端把消息发送出去 。  
以上就是我们要在本章实现的三个功能，当然在上一章我就跟大家说了，Broker 的服务端还要处理来自客户端的请求，所以肯定还得给 Broker 的 Netty 服务端定义请求处理器，但这一块的内容可以先放一放，因为我们根本还没实现客户端向 Broker 发送请求的功能，所以也就不知道要给 Broker 服务端的请求处理器定义什么内容，所以我们可以先忽略这部分内容，等第六版本代码再为 Broker 模块实现这部分功能，因为在第六版本代码中，我使用 RocketMq 内置的 Admin 模块向 Broker 创建主题的功能。好了这些内容等后面再展开讲解吧，我已经把话题扯远了，接下来还是让我们回到主题，看看上面提出的三个功能如何实现吧。  
  
如果仅仅是定义定时任务的话，那么上一章我们定义的 BrokerController 类已经实现了这些内容，我把相关的代码展示在下面代码块中了，请看下面代码块。  
package org.apache.rocketmq.broker;  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/9

\* @方法描述：Broker模块的控制器，该控制器负责管理Broker的各个组件和服务，包括网络通信、定时任务、配置管理等。

\*/

public class BrokerController {  
  
//省略该类的其他内容  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/1/9

\* @方法描述：初始化Broker模块各个定时任务的方法，在第五版本代码中，这个方法的内容还很简单，随着代码版本的迭代，这个方法的内容会越来越多

\*/

protected void initializeScheduledTasks () {  
//判断Broker配置信息中是否设置了NameServer的地址，因为Namesrv也可以构建集群，所以这里用户定义的可能是多个NameServer地址，并且用逗号分隔

if (this.brokerConfig.getNamesrvAddr ()!= null) {

//如果设置了NameServer地址，则把用户设置得NameServer地址更新到NettyRemotingClient客户端的namesrvAddrList成员变量中

this.updateNamesrvAddr ();

LOG.info ("Set user specified name server address: {}",this.brokerConfig.getNamesrvAddr ());

//提交一个定期更新NameServer地址的任务

this.scheduledExecutorService.scheduleAtFixedRate (new Runnable () {

@ Override

public void run () {

try {

//在这里执行更新NameServer地址的操作

//之所以要定期更新NameServer地址，是因为NameServer地址可能会发生变化，也许构建的是Namesrv集群

//集群中会添加新的节点，如果有新的Namesrv添加到集群中了，就要让Broker知道，所以需要定期更新Broker中的NameServer地址

BrokerController.this.updateNamesrvAddr ();

从上面代码块中可以看到，我们确实已经把各个定时任务都定义完毕了，定期刷新可用的 Namesrv 地址列表的定时任务，以及定期向 Namesrv 注册信息的定时任务都定义完毕并且启动了。但是我们做到的也仅仅是把定时任务定义完毕了，这只是一层表象，定时任务本身的内容，也就是说定时任务的真正逻辑我们并没有实现。上面代码块中有四个方法我都没有为大家真正实现：  
1 fetchNameServerAddr() 是从Namesrv 地址服务器获取 Namesrv 可用地址的方法，这个方法并没有实现。  
2 updateNameServerAddressListByDnsLookup() 是使用 DNS 的方式获取 Namesrv 可用地址的方法，该方法也没有实现。  
3 updateNameServerAddressList() 是定期从 BrokerConfig 配置信息对象中获取可用的 Namesrv 地址列表的方法，该方法也没有实现。  
4 registerBrokerAll() 就是把 Broker 自己的信息注册到 Namesrv 节点的方法，该方法目前也没有实现。  
这些关键的方法都没有实现，这肯定让大家非常着急，就好像只差一脚就可以射门了，但这个时候足球运动员偏偏手脚不射了，故意吊人胃口。但我这么做并不是在吊大家胃口，而是因为确实还实现不了。 因为这些方法的实现都有一个前提，那就是必须把 Broker 使用的 Netty 客户端定义出来，只有得到了客户端，Broker 才能对外发送消息啊(其实这么说有些不严谨，因为前三个方法并不需要 Netty 客户端，但它们三个都需要另一个组件，很快我就会为大家实现了) 。这时候就可以回到文章一开始提出的三个要点了，要点 1、2 虽然被我列在前面，但是只有第三个要点先实现了，也就是先把客户端实现了，才能继续实现其他功能。所以接下里我们要做的就是先把 Broker 模块使用的 Netty 客户端实现了。  
  
实现 NettyRemotingClient 客户端  
  
Netty 构建的客户端其实非常容易实现，仿照这 Netty 构建的服务端实现即可。 我们之前构建的 Netty 服务端被定义为了 NettyRemotingServer，那么 Netty 构建的客户端就可以被定义为 NettyRemotingClient，这个很容易理解，因为 Netty 构建的客户端和服务端在通信时使用的都是 Remoting 协议 。当然还有一点大家千万别忘了，这一点我在之前构建 NettyRemotingServer 服务端的时候也跟大家讲解过，那就是客户端和服务端有些方法的作用都是相同的，需要定义到它们的抽象父类中，所以我们会看到 NettyRemotingServer 类继承了 NettyRemotingAbstract 抽象父类； 现在要定义 NettyRemotingClient 客户端了，这个 NettyRemotingClient 客户端显然也要继承 NettyRemotingAbstract 抽象父类 。抽象父类中定义的都是处理消息，发送消息，回复消息的公共方法，这些我就不再细说了，都是旧知识。大家可以自己回顾一下。  
  
这些逻辑都梳理清楚了，那接下来就可以开始构建这个 NettyRemotingClient 客户端了，既然是 Netty 构建的客户端，那么 Netty 构建客户端的那些流程都必不可少，肯定会用到封装了客户端配置信息的 NettyClientConfig 对象，还有什么事件循环组，IO 事件处理器，以及 Bootstrap 客户端启动器，构建 Pipeline 等一系列操作都必不可少。所以这个 NettyRemotingClient 可以先定义成下面这样，请看下面代码块。  
上面代码块展示的就是 Netty 构建完毕的客户端，可以看到流程还是很清楚的，而且逻辑和构建 Netty 服务端几乎一致，所以我就不再重复讲解上面代码块的内容了，注释非常详细，大家自己看看就行。阅读完了上面代码块的内容之后，大家肯定会有一些疑问，比如说： start() 方法只是构建了 Netty 客户端，客户端创建完毕了，怎么没有启动呢？Netty 客户端怎么向 Namesrv 节点发送消息呢 ？  
  
第一个问题很容易解答， 客户端创建完毕了，不需要启动，因为客户端是直接访问服务端的，只要服务端启动了，客户端知道服务端的 IP 地址和监听端口号，直接访问这个地址即可 。我们之前使用 Netty 客户端发送消息时不就是这么做的吗？我给大家展示一个具体的例子大家就明白了，请看下面代码块。  
在上面代码块中有一行非常重要的代码，那就是 ChannelFuture f = b.connect().sync()，在执行这行代码之后，Bootstrap 启动器已经知道了服务端的 IP 地址和坚挺的端口号，然后 Bootstrap 启动器对象直接访问服务端，和服务端建立连接即可。之后服务端就可以执行 f.channel().writeAndFlush(request) 这行代码向服务端发送消息即可。除非把客户端和服务端的建立的连接关闭了，否则客户端和服务端的连接会一直维持着，客户端可以一直向服务端发送消息。这些知识大家肯定都很熟悉，不用我再罗嗦了。  
  
好了，那么回到我们刚才实现的 NettyRemotingClient 客户端中，我们可以看到， 在 NettyRemotingClient 客户端的 start() 方法中，只是把 Netty 客户端构建出来了，也就是说 Bootstrap 启动器对象已经可以拿来即用了。这就意味着假如我们想使用 NettyRemotingClient 客户端访问某个服务端，和某个服务端建立网络连接，那只需要得到这个已经完善的 Bootstrap 启动器对象，直接调用 Bootstrap 对象的 connect() 方法访问服务端即可 。这个逻辑应该也很容易理解吧？而在我们定义的 NettyRemotingClient 客户端中，恰好有一个 fetchBootstrap() 方法可以得到 Bootstrap 启动器对象，所以我才说客户端不需要真的启动，只需要访问某个服务端时，和该服务端建立连接就可以了。当然，也许有朋友会说我们构建完毕的 Bootstrap 启动器对象并不知道要访问的服务端的网络地址，这也很简单，调用 connect() 方法连接服务端的时候手动指定不就行了？再说了，Broker 的客户端可能要访问多个 Namesrv 服务器，那么 Broker 客户端的 Bootstrap 启动器访问的服务端也不可能写成固定的，肯定要经常切换。那分析到这里，刚才第二个问题的答案已经不言而喻了，Broker 客户端向 Namesrv 节点发送消息的方式很简单： 如果 Namesrv 的可用地址是一个集合，那就循环这个集合中的每一个地址，使用 Bootstrap 启动器对象和这个地址建立网络连接，网络连接一点建立就会得到对应的 Channel，把买一个 Channel 保存了，就可以随时使用这个 Channel 向对应的 Namesrv 服务端发送消息了 。这个逻辑也不难理解吧？但大家肯定都能看出来，我们现在实现的 NettyRemotingClient 类中并没有保存和 Namesrv 节点建立 Channel 的功能，也并能得到 Namesrv 可用地址的集合，所以接下来我们就要朝着这个方法重构 NettyRemotingClient 类。  
  
重构 NettyRemotingClient 类  
  
重构的方式非常简单，我可以在 NettyRemotingClient 类中定义一些新的成员变量，有 List，也有 Map，List 可以保存可用的 Namesrv 地址，Map 可以保存和每一个 Namesrv 节点建立的 Channel 连接。  
还是那句话，虽然上面代码块中的内容很多，但是内容都很简单，从上面代码块中可以看到，我们给 NettyRemotingClient 客户端新添加的功能只有一个， 那就是让 NettyRemotingClient 客户端和指定的服务端创建连接，而实现该功能的方法就是 NettyRemotingClient 类的 createChannel() 方法，该方法可以接收一个指定的服务端网络地址，然后就可以在该方法中使用客户端的 Bootstrap 启动器去连接指定服务端，把连接成功得到的 Channel 包装到一个 ChannelWrapper 对象中，确切地说是把连接操作成功之后得到的 ChannelFuture 对象包装到一个 ChannelWrapper 对象中，而 ChannelFuture 对象可以直接得到连接对应的 Channel，所以得到了 ChannelWrapper 对象，就意味着得到了客户端与服务端连接，然后再保存 ChannelWrapper 对象到 Map 中即可 。这就是上面代码块中最核心的功能，大家只需要掌握这个功能就行，其他的代码都是围绕着这个功能展开的，大家简单看看就行。到此为止，我们就实现了 NettyRemotingClient 客户端和指定服务端建立连接的功能。这也就意味着，假如我们得到了可用的 Namesrv 地址集合，那我们想让 Broker 的客户端访问哪个 Namesrv 服务端，只需要调用 NettyRemotingClient 客户端的 createChannel() 方法即可。在该方法中会首先判断 channelTables 中是否已经存在与服务端地址对应的 ChannelWrapper 对象，如果存在直接返回对应的 Channel 即可，不存在则创建对应的 Channel，然后保存这个 Channel。一旦这个 Channel 建立成功，那么客户端想对服务端发送什么消息，都可以使用这个 Channel 发送。这些逻辑大家都能理解吧？  
  
那现在就剩下一个问题了， 那就是 NettyRemotingClient 客户端怎么得到可用的 Namesrv 地址列表呢 ？只有知道了可用的 Namesrv 地址集合，才能循环遍历集合中的每一个地址，调用 NettyRemotingClient 对象的 createChannel() 方法和指定的 Namesrv 建立连接，然后才能发送消息。并且大家肯定也都注意到了， 我在 NettyRemotingClient 还定义了两个成员变量，分别是 namesrvAddrList 和 availableNamesrvAddrMap 这两个成员变量。从代码注释中我们也可以知道，namesrvAddrList 成员变量存储的是 Namesrv 地址集合，而 availableNamesrvAddrMap 成员变量存储的是可用的 Namesrv 地址信息 。这两个成员变量定义得非常突兀，而且定义完毕之后，我根本就没使用过这两个成员变量，这是怎么回事呢？接下来请大家听我慢慢分析。  
  
实际上是这样的，NettyRemotingClient 的 namesrvAddrList 成员变量存储的是 Namesrv 地址列表，这个地址列表就是最新的 Namesrv 地址列表集合，而 availableNamesrvAddrMap 存储的就是当前 Broker 中可以使用的 Namesrv 地址的信息，看到这里大家可能会有些发懵，不知道这是怎么回事，我简单解释一下： 我们已经知道了 Broker 内部的定时任务会定期刷新可以使用的 Namesrv 地址列表，实际上这个刷新后的 Namesrv 地址列表会赋值给 NettyRemotingClient 客户端的 namesrvAddrList 成员变量，这样一来，客户端就知道了可用的 Namesrv 地址列表信息；在 NettyRemotingClient 客户端内部还有一个定时任务，这个定时任务会定期把 namesrvAddrList 成员变量中的信息更新到 availableNamesrvAddrMap 成员变量中，如果 Broker 要向所有的 Namesrv 节点发送消息，那就直接得到 NettyRemotingClient 客户端 的 availableNamesrvAddrMap 存储的地址信息，然后向每一个地址发送消息即可 。看到这里大家可能会觉得这么做岂不是太折腾了？直接就是用 NettyRemotingClient 的 namesrvAddrList 成员变量不就完了？反正这个成员变量中存储的就是最新的 Namesrv 地址信息，没错，我也觉得很折腾， 但在 namesrvAddrList 信息更新到 availableNamesrvAddrMap 的过程中，还执行了一些操作，如果有新的 Namesrv 加入了，那就会为这个 Namesrv 节点创建连接 Channel 。好了，说了这么多，接下来让我们来看看再次重构之后的 NettyRemotingClient 类的代码吧。这次展示的代码内容很少，请看下面代码块。  
在阅读了上面的代码块之后，现在大家应该终于清楚了 NettyRemotingClient 客户端的 namesrvAddrList 和 availableNamesrvAddrMap 成员变量的作用了吧？ 只要我们能定期调用 NettyRemotingClient 的 updateNameServerAddressList() 方法，那么就能刷新客户端对象持有的最新可用的 Namesrv 地址列表集合 。那现在新的问题又来了，这个 updateNameServerAddressList() 方法应该怎么被调用呢？还有一点，当调用这个方法的时候，显然已经得到了最新的 Namesrv 地址列表集合，但我们实现客户端不就是为了再上一章定义的定时任务中使用客户端获取最新的 Namesrv 地址集合吗？这究竟是怎么回事啊？写到这里，我意识到我必须再为我们的 Broker 模块引入一个新的类了，那就是 BrokerOuterAPI 类。  
  
引入 BrokerOuterAPI 类  
  
实际上是这样的 ，在 Broker 源码中，如果 Broker 想对外发送信息，或者说想把自己的信息注册到每一个 Namesrv 节点上，并不会直接使用 NettyRemotingClient 客户端对外发送消息，而是会使用 BrokerOuterAPI 类的对象对外发送消息 ，从名字上就能看出来，这个 BrokerOuterAPI 就是专门用来对外工作的，很多对外操作都定义在这个类中，比如更新可用的最新 Namesrv 地址集合，把自己的信息注册到 Namesrv 节点上，而这个 BrokerOuterAPI 类也就是我们接下来要引入的组件。  
  
当然，很容易就能想到， 不管 BrokerOuterAPI 类的对象怎么对外发送消息，最终肯定还是要用到 NettyRemotingClient 客户端对象 ，这是一定的，因为只有这个 NettyRemotingClient 对象提供了对外通信的能力，所以这个 BrokerOuterAPI 类可以先定义成下面这样。我们先实现一个简单的 BrokerOuterAPI 类，并且在该类中定义注册 Broker 信息到 Namesrv 节点的方法，请看下面代码块。  
这就是一个最简单的 BrokerOuterAPI 类，当然，最终版本的 BrokerOuterAPI 类也没有什么难度，但现在我把 BrokerOuterAPI 类定义成这样，我相信大家一眼就能知道这个 BrokerOuterAPI 究竟是怎么工作的。如果我们把它定义为 BrokerController 的成员变量，那么只需要在 BrokerController 的定期注册信息给 Namesrv 节点的定时任务中调用 BrokerOuterAPI 对象的 registerBrokerAll() 方法不就行了？讲解到这里，大家可能也都意识到了，更新可用的最新 Namesrv 地址集合的方法也都定义在这个 BrokerOuterAPI 类中了，接下来我就不卖关子了，直接给大家展示对应的代码，请看重构之后的 BrokerOuterAPI 类，请看下面代码块。  
BrokerOuterAPI 类实现完毕了，然后只需要在 BrokerController 中创建 BrokerOuterAPI 对象，在各个定时任务中调用 BrokerOuterAPI 的对应方法即可，接下来就请大家看看我重构之后的 BrokerController 类，请看下面代码块。  
到此为止，BrokerController 重构得也就差不多了，整个 Broker 模块就差最后一个功能没有实现了，那就是收集自己的内部信息，然后注册到 Namesrv 节点上，也就是要把 registerBrokerAll() 方法真正实现了。我可以很负责任地告诉大家，注册 Broker 信息到 Namesrv 的方法实现起来真的很简单，因为目前要收集的信息很少，只是 Broker 节点自己的信息，还不包含主题信息，所以实现起来非常简单；但是 Broker 节点把信息发送给 Namesrv 节点后， Namesrv 接收信息的操作就稍微复杂一些了，妥善存储这些信息，管理这些信息是非常重要的，这也就意味着 Namesrv 模块也需要再次重构。而这些都是第五版本代码中内容，在这一章显然是讲解不完了，所以放到下一章为大家讲解吧。大家可以再等等，等下一章结束之后，就可以阅读第五版本代码的所有内容了，好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/oux7ymsdwnx6c2os*  
*All content belongs to its respective owners and creators.*