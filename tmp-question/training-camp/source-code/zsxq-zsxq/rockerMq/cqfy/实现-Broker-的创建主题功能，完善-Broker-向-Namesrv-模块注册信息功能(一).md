上一章我们已经把 Broker 模块向 Namesrv 节点注册信息的功能实现了，确切地说，是实现了一部分，因为最核心的功能，Broker 向 Namesrv 注册自己内部的主题信息还没有实现。这也能理解，毕竟我们还没实现 Broker 创建主题功能。这一章我们就一起来实现这个功能，等这个功能实现完毕了， Broker 可以在自己内部创建对应的主题了，那么 Broker 把信息注册到 Namesrv 节点时，就会把自己的配置信息和主题信息一起发送给 Namesrv 节点，这两部分信息合起来，才构成了一个 Broker 节点拥有的完整信息 。  
  
使用 Admin 模块向 Broker 创建主题  
  
我刚意识到，我已经在文章中多次提到主题这个概念，因为我并没有把大家当成新手，潜意识里认为大家都或多或少使用过 RocketMq 框架，对该框架已经有了一个基础认识，很多基础概念大家也都清楚是怎么回事，所以我就一直没有跟大家解释主题这个概念是什么。当然，我接下来并不是真的要和大家解释主题的概念，如果我跟大家说主题就是一个字符串信息，比如主题名称就是 "Test"，那对 Broker 创建了这个主题之后，设置了主题下的读写队列数量，生产者就可以向 "Test"主题下的写队列发送消息，消费者就可以从 "Test" 主题下的读队列消费消息，大家肯定还是不清楚这是怎么回事。那说到队列了，这也是和主题相关的基本概念，我现也没办法展开讲解，大家可以先把它们当成真正的队列。当然，这并不意味着这部分知识就这么过去了，我现在不讲解是因为没有到讲解的时机，很多功能都没实现，不看代码，只用文字描述只会让大家越来越困惑， 如果我说这些队列只是代码上的名词，在物理上并不真正存在 ，我相信这样的话只会让大家更摸不着头脑。所以还是再等一等吧，再等四五个版本代码，我们就会实现生产者客户端向 Broker 发送消息的功能，到时候大家就知道所谓的队列究竟是什么了。  
  
我相信刚才的内容肯定没什么用，即便我写了一大段内容，不清楚的朋友肯定还是不清楚，原因很简单，用文字解释具体的功能实在是太抽象了，具体的功能就应该使用具体的代码来展示，所以，如果有些朋友并不清楚主题概念，我希望大家可以找一些教学视频，或者看看对应的测试类代码，把这一部分的知识弥补了。现在我们已经开始深入到 RocketMq 框架的业务逻辑了，不再是构建什么通信模块的客户端和服务端，所以大家很有必要把 RocketMq 框架的各个概念梳理清楚。好了，闲话少说，接下来就让我们开始实现 Broker 创建主题功能吧。  
  
要实现 Broker 模块创建主题的功能，还需要为我们自己构建的 MQ 框架引入一个新的模块，那就是 Admin 模块，也可以把它成为控制台模块。 当我们想要对 Broker 创建主题时，就可以使用 Admin 控制台模块向 Broker 节点发送创建主题的请求，Broker 模块接收到请求之后，就可以执行对应的创建主题的操作了 。由此看来，这个控制台模块其实也没什么，无非又是一个 Netty 构建的客户端，客户端向 Broker 服务端发送对应的请求而已，这就是 Admin 控制台模块的本质。当然，Admin 控制台不仅可以向 Broker 模块创建主题，还可以更新 Broker 节点的配置信息，除此之外还有非常多的功能，在后面的代码中我都会为大家实现了。在第六版本代码中，我们只需要关注 Admin 模块向 Broker 节点创建主题信息的功能即可。那接下来我们就开始实现这个 Admin 模块吧。  
  
要实现 Admin 模块我们首先要弄清楚一个问题，那就是 Admin 模块可以向 Broker 节点发送创建主题的请求，这就意味着 Admin 模块需要获得 Broker 节点的信息。那这该怎么办呢？ 最简单的方法就是在 Admin 模块启动的时候把要创建主题的 Broker 节点的网络地址设置到命令行参数中，Admin 启动的过程中会解析命令行参数，然后得到对应的 Broker 节点，接着就可以向这个 Broker 节点发送要创建的主题请求了 。  
  
那如果构建了一个庞大的 Broker 集群，Broker 节点太多了，不可能全部写到 Admin 的命令行参数中，如果 Admin 想对集群中所有 Broker 主节点创建相同主题(从节点的配置信息会被同步，从节点不必单独创建主题)，又该怎么办呢？这个时候其实就可以使用第二种方法： 我们都知道 Broker 节点的信息都注册到了 Namesrv 中，这就意味着 Admin 模块要想获得 Broker 节点的信息，肯定要访问 Namesrv 模块，那就可以把 Namesrv 的地址设置到 Admin 的命令行参数中，这样一来，Admin 就可以直接访问 Namesrv 节点获取对应的 Broker 节点的网络地址，然后再向每一个 Broker 节点发送创建主题信息的请求即可 。这一点应该好理解吧？当然有一点必须要指出来，那就是当 Admin 使用这种方式向 Broker 创建主题时，必须要等到 Namesrv 模块和 Broker 模块启动完毕，并且 Broker 已经把自己信息注册到 Namesrv 上了，Admin 自己才能启动。如果过早启动，Namesrv 还没启动或者还没接收到 Broker 注册的信息，那么 Admin 根本就得不到 Broker 节点的信息。这一点大家必须弄清楚。  
  
好了，上面的逻辑分析完毕之后，接下来就是常规流程了，无非就是使用 Netty 构建的客户端向 Broker 发送创建主题的请求，所以接下来我就要给大家展示相关的代码了。 按照 Namesrv、Broker 模块的开发经验，在实现 Admin 模块时首先可以定义一个 Admin 模块的启动器，也就是 MQAdminStartup 类，这个 MQAdminStartup 启动器在启动的时候会解析命令行参数，如果命令行中设置了指定 Broker 节点的网络地址，那么就直接向这个地址发送对应的请求，如果命令行中设置的是 Namesrv 地址，那么就需要访问 Namesrv 节点获取所有 Broker 主节点的网络地址，然后向这些 Broker 节点发送对应请求。这就是 MQAdminStartup 启动器在启动过程中要执行的操作 。  
  
当然，Admin 既然可以向 Broker 节点创建主题，自然也就可以删除指定 Broker 节点的某个主题。如果是这样的话， 在启动 Admin 模块的时候还需要在命令行中把要执行的操作定义出来，如果要执行创建主题的操作，就可以在命令行中定义一个 updatetopic 字符串信息，表示更新主题，也就是创建主题信息的意思；如果想要删除某个主题，那就可以在命令行中定义一个 deleteTopic 删除主题的字符串信息 。并且不管是更新还是删除主题，它们的参数信息必须定义在命令行首位，就像下面代码块展示的这样，请看下面代码块。  
Admin 模块的命令行参数信息介绍完毕之后，我们也知道了 Admin 既可以创建主题信息，也可以删除主题信息，那接下来我想定义两个对象， 一个是 UpdateTopicSubCommand，这个对象专门用来执行创建主题信息的操作；另一个是 DeleteTopicSubCommand，这个对象专门用来执行删除主题信息的操作 。这两个对象可以先定义成下面这样，请看下面代码块。  
定义好了这两个对象之后， 当 MQAdminStartup 启动器启动了，就可以根据命令行中的参数信息判断要执行什么操做，如果要执行更新创建主题操作，就可以选择 UpdateTopicSubCommand 对象，执行更新创建主题的操作，也就是执行该对象的 execute() 方法；如果要执行删除主题的操作，那就使用 DeleteTopicSubCommand 对象执行命令即可 。这样分析完毕之后，接下来就可以给大家展示 Admin 模块具体的代码了。  
  
首先是 MQAdminStartup 启动器的内容，请看下面代码块。  
好了，MQAdminStartup 的内容展示完毕之后，接下来我们就要真正实现 UpdateTopicSubCommand 类的 execute() 方法， 这个时候 UpdateTopicSubCommand 类的 execute() 方法已经得到了 Admin 命令行中的参数信息，那这个方法首先要做的肯定就是得到命令行参数中要创建的主题名称，然后创建该主题；之后就可以接着判断命令行中设置的是具体的 Broker 节点的地址，还是 Namesrv 节点的地址。如果是指定 Broker 节点的地址，那就直接向该节点发送创建主题信息的请求即可，如果命令行中设置的是 Namesrv 地址，那就需要先访问 Namesrv 获得指定集群下所有 Broker 主节点的地址，然后向每一个 Broker 节点发送创建主题信息的请求即可 。按照这个流程，接下来就让我为大家展示一下重构完毕的 UpdateTopicSubCommand 类的内容，给大家看看 UpdateTopicSubCommand 对象的 execute() 方法究竟要执行什么操作(DeleteTopicSubCommand 类的内容我就不再文章中为大家展示了，内容非常简单，大家直接看我提供的第六版本代码即可)。请看下面代码块。  
从上面代码块中可以看到 UpdateTopicSubCommand 对象的 execute() 方法中执行的操作和我们之前分析的一模一样，而且代码块中的注释非常详细，所以代码逻辑我就不再赘述了。我接下来要为大家简单补充的是，在上面代码块的第 35 行，大家可以看到创建了一个 DefaultMQAdminExt 对象，接着在后面的代码中启动了这个 DefaultMQAdminExt 对象，也就是调用了该对象的 start() 方法。 我想简单解释一下这个 DefaultMQAdminExt 对象，这个对象内部就持有了 Netty 构建的客户端对象，也就是 NettyRemotingClient 对象，当执行到该对象的 createAndUpdateTopicConfig() 方法，或者是执行到 CommandUtil.fetchMasterAddrByClusterName(defaultMQAdminExt, addr) 这行代码时，其实最终都是在 DefaultMQAdminExt 对象内部调用了 NettyRemotingClient 客户端对象的 invokeSync() 方法，把对应的请求发送给了 Broker 节点或者是 Namesrv 节点 。大家知道这个操作最终做了什么就行，DefaultMQAdminExt 类的具体内容我就不再文章中展示了，因为整个 Admin 模块的内容都很简单，我提供的第六版本代码注释也很详细，我就不在文章中耗费篇幅讲解这些不重要的组件了。当然，把 Admin 向 Namesrv 节点发送请求获取指定集群所有 Broker 主节点的方法展示一下，把 Admin 向 Broker 发送创建主题请求的方法展示一下也不是不行，请看下面两个代码块。  
首先是 Admin 向 Namesrv 节点发送请求获取指定集群所有 Broker 主节点的方法。  
接下来是 Admin 向 Broker 发送创建主题请求的方法。  
到此为止，我们就把 Admin 模块的核心功能实现完毕了。接下来就应该把目光集中到 Broker 模块中，因为这个时候 Admin 已经实现了向 Broker 节点发送创建主题的请求，也就是发送 UPDATE\_AND\_CREATE\_TOPIC 类型的请求，那 Broker 节点接收到这个请求之后就应该处理这个请求。所以我们应该重构 Broker 模块，为 Broker 模块实现这个功能。  
  
为 Broker 引入 AdminBrokerProcessor 请求处理器  
  
上一章我们实现 Broker 模块时，为 Broker 模块定义了一个 BrokerOuterAPI 类，这个类的对象专门用于 Broker 节点对外执行某些操作。现在 Broker 节点可以接收客户端发送过来的请求了，那要想处理这些请求，肯定要定义一个请求处理器，因为目前 Broker 节点处理的都是来自 Admin 模块的请求，那我就把这个请求处理器定义为 AdminBrokerProcessor 吧。我们现在要做的就是在 Broker 模块启动的过程中把这个请求处理器创建出来，然后注册到 Broker 的服务端中。而这部分功能都在 BrokerController 类中，所以我把 BrokerController 类简单重构了一下，请看下面代码块。  
好了，现在 AdminBrokerProcessor 请求处理器已经注册到 Broker 的服务端了，接下来我们就该真正实现这个 AdminBrokerProcessor 请求处理器了。它的实现逻辑也很简单，无非就是在处理请求的 processRequest() 方法中判断当前接收到的是什么类型的请求，如果是 UPDATE\_AND\_CREATE\_TOPIC 类型的请求，就执行更新或创建主题的操作，如果是 DELETE\_TOPIC\_IN\_BROKER 类型的请求，那就执行删除指定主题的操作。所以这个 AdminBrokerProcessor 请求处理器可以定义成下面这样，请看下面代码块。  
从上面代码块中可以看到，当 AdminBrokerProcessor 请求处理器接收到 UPDATE\_AND\_CREATE\_TOPIC 类型的消息后，就会执行它的 updateAndCreateTopic() 方法处理该请求。在该方法中我们可以看到， Broker 已经解析了请求，并且从请求中得到了完整的主题信息对象，也就是 TopicConfig 对象。如果 Broker 把这个 TopicConfig 对象保存起来，那么我们就可以说 Broker 节点创建对应的主题成功了 。但是在请求处理起的 updateAndCreateTopic() 方法中并没有这么做，原因很简单，在第六版本代码中这个 updateAndCreateTopic() 方法还并不完善，我只实现了 Broker 节点接收主题信息的功能，并没有实现保存主题信息的功能，实际上更新主题信息的功能我也没实现。这些内容都放在第七版本代码中实现了，因为这些功能一旦实现了，就要紧接着把 Broker 注册自己的配置信息和主题信息到 Namesrv 节点的功能也实现了，这一部分的代码量还是有些多的，所以我就把这些功能都放在第七版本代码中实现了。到了下一篇文章，大家就知道 Broker 节点是怎么保存和更新主题信息的了。好了朋友们，我们下一章见！  
  
附录：  
现在大家可以阅读第六版本代码的所有内容了。  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/piiyvdt788gasu0m*  
*All content belongs to its respective owners and creators.*