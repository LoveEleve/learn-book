朋友们，上一章结尾我跟大家提出了 4 个问题，我把这 4 个问题搬运到这里了：  
1 nacos 服务端每接收一个客户端连接，就要为这个客户端创建一个对应的 ConnectionBasedClient 连接对象，那么这个 ConnectionBasedClient 对象是在哪里被创建的呢？  
2 nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象应该怎么存储呢？因为每一个客户端对应一个唯一的 ConnectionBasedClient 对象，如果想维持一种清楚的映射关系的话，那肯定以 key-value 键值对的方法存放到一个 map 中最为合适，其中 key 就是客户端连接 Id，value 就是对应的 ConnectionBasedClient 对象。这样一来，nacos 服务端只需要根据客户端连接 Id 就可以直接找到与客户端对应的 ConnectionBasedClient 对象，然后执行自己想要的操作即可。分析了这么多，那么存放客户端连接 Id 和 ConnectionBasedClient 对象的 map 应该定义在哪里呢？或者说是不是需要引入新的组件来管理 nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象呢？比如说引入一个客户端管理器？  
3 为每一个客户端创建了 ConnectionBasedClient 对象之后，也按照刚才分析的映射关系存储好了 ConnectionBasedClient 对象之后，当 nacos 客户端注册服务实例信息到客户端时，这些信息该怎么存放到与 nacos 客户端对应的 ConnectionBasedClient 对象中呢？  
4 InstanceRequestHandler 请求处理器该如何重构呢？ConnectionBasedClient 需要被定义为 InstanceRequestHandler 请求处理器的成员变量吗？  
这 4 个问题就是我们本章要重点解决的几个问题，除了这 4 个问题，我还把上一章引入的 ConnectionBasedClient 类的内容给大家搬运到下面了，大家可以先回顾回顾上一章的内容，然后再开始阅读本章内容。请看下面代码块。  
package com.cqfy.nacos.naming.core.v2.client.impl;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/17

\* @方法描述：客户端连接对象，服务器会为每一个接收到的客户端连接创建客户端连接对象，该客户端向服务器注册的服务实例，订阅的服务实例信息的订阅者信息，都会存放到它自己的客户端对象中

\*/

public class ConnectionBasedClient {  
  
//客户端连接Id

private final String connectionId;  
  
//客户端存放到注册到服务端的服务实例信息的map，key就是服务实例元信息，value就是对应的服务实例信息

//实际上源码中这个map的value并不是Instance类型的对象，后面重构之后大家就知道这里存放的其实是InstancePublishInfo对象

protected final ConcurrentHashMap < Service,Instance > publishers \= new ConcurrentHashMap <> (16,0.75f,1);  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/17

\* @方法描述：注册服务实例到客户端对象的方法

\*/

@ Override

public boolean addServiceInstance (Service service,InstancePublishInfo instancePublishInfo) {  
//该方法暂时不做实现

}  
  
  
  
分析创建 ConnectionBasedClient 对象的时机  
  
大家回顾了上一章的内容之后，接下来我们就可以开始解决以上 4 个问题了，首先让我们先来看第一个问题： nacos 服务端每接收一个客户端连接，就要为这个客户端创建一个对应的 ConnectionBasedClient 连接对象，那么这个 ConnectionBasedClient 对象是在哪里被创建的呢 ？如果大家还记得服务端为每一个客户端创建唯一连接 Id，以及把客户端连接对象注册到连接管理器的操作流程，那么大家肯定就能想到，其实 nacos 服务端为每一个客户端创建 ConnectionBasedClient 对象的操作流程和处理客户端连接 Id 的流程是紧密联系的。因为在 nacos 服务端看来，为每一个接收到的客户端连接创建了唯一连接 Id，然后又创建了对应的连接对象，这才意味着服务端接收到的每一个客户端连接具像化了，现在我们要创建的 ConnectionBasedClient 对象，无非是让客户端在服务端具像化的程度更深一些。显然， 这个 ConnectionBasedClient 对象就在 nacos 服务端为每一个客户端连接创建了连接对象之后再创建即可 。  
  
那么 nacos 服务端在哪里为每一个客户端连接创建了连接对象呢？也许大家还记得前几章的内容，具体的创建时机就是在 GrpcBiStreamRequestAcceptor 双向流消息处理器的 requestBiStream() 方法中，具体流程请看下面代码块。  
可以看到，在上面代码块中，nacos 服务端为客户端连接创建了连接对象，并且在第 126 行，执行了 connectionManager.register() 这行代码，这行代码就是把刚刚创建好的客户端连接对象注册到了 ConnectionManager 连接管理器中，这些都是旧知识，想必大家看一眼就能回忆起来。但是我们最终的目的是让 nacos 服务端为每一个客户端创建 ConnectionBasedClient 对象，并且刚才我们也分析了，nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象就在 nacos 服务端为每一个客户端连接创建了连接对象之后再创建即可 。但在上面展示的代码块中，只是创建了客户端连接对象，然后把客户端连接对象注册到了 ConnectionManager 连接管理器中，根本没有创建 ConnectionBasedClient 对象的操作呀？别急，很快就有了， 因为我把创建 ConnectionBasedClient 对象的操作放在了 ConnectionManager 连接管理器中 ，原因很简单，通过上面的代码块我们可以看到，nacos 服务端只为客户端创建了连接对象还不够，必须得把连接对象成功注册到 ConnectionManager 连接管理器中，整个操作才算成功执行完毕了，如果没有把连接对象成功注册到连接管理器中，那么 nacos 服务端并不会维持和客户端建立的连接，这样一来，即便为这个客户端创建了 ConnectionBasedClient 对象也没什么用。所以我们应该在连接对象成功注册到 ConnectionManager 连接管理器之后才能真正为客户端创建 ConnectionBasedClient 对象。还好我们之前已经实现了 ConnectionManager 连接管理器，具体内容如下，请看下面代码块。  
可以看到，在上面代码块的 register() 方法中，就把 nacos 服务端为客户端连接创建的连接对象存放到 connections 这个 map 中了，这也就意味着连接对象成功注册到连接管理器中了，按照我们之前的分析，肯定就要接着为客户端创建 ConnectionBasedClient 对象了，那应该怎么做呢？就直接 new 一个 ConnectionBasedClient 对象吗？创建完毕的对象该怎么存储呢？现在大家也许已经意识到了，我们已经开始对之前列出的 4 个问题中的第 2 个问题展开谈论了。第一个问题已经解决了， 那就是在 ConnectionManager 连接管理器的 register() 方法中创建 ConnectionBasedClient 对象(这么说其实还不太准确，实际上 ConnectionBasedClient 对象并不是在 register() 方法中创建的，register() 方法只是创建 ConnectionBasedClient 客户端对象的入口方法，具体逻辑下一小节就讲到了) 。但是怎么创建该对象，创建完毕之后该对象如何存储就成了我们目前要解决的问题，接下来，就让我们一起探讨探讨吧。  
  
引入 ClientManager 客户端管理器  
  
首先让我们先来回顾一下第二个问题： nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象应该怎么存储呢？因为每一个客户端对应一个唯一的 ConnectionBasedClient 对象，如果想维持一种清楚的映射关系的话，那肯定以 key-value 键值对的方法存放到一个 map 中最为合适，其中 key 就是客户端连接 Id，value 就是对应的 ConnectionBasedClient 对象。这样一来，nacos 服务端只需要根据客户端连接 Id 就可以直接找到与客户端对应的 ConnectionBasedClient 对象，然后执行自己想要的操作即可。分析了这么多，那么存放客户端连接 Id 和 ConnectionBasedClient 对象的 map 应该定义在哪里呢？或者说是不是需要引入新的组件来管理 nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象呢？比如说引入一个客户端管理器 ？其实从第二个问题中，我们也可以认识到，nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象应该存放在一个 map 中。因为 nacos 服务端会接收很多客户端连接，也就意味着会为很多客户端创建 ConnectionBasedClient 对象，服务端要想知道创建的每一个 ConnectionBasedClient 对象和哪一个客户端对应，最好的方式就是把客户端连接 Id 和对应的客户端 ConnectionBasedClient 对象存放到一个 map 中，这样就可以根据客户端的连接 Id 来获得对应的 ConnectionBasedClient 对象，所以要定义一个 map 来存放 ConnectionBasedClient 对象是势在必行的。现在的问题就是这个 map 要定义在哪里呢？  
  
在上一小节我们已经明确了 要在 ConnectionManager 连接管理器的 register() 方法中创建 ConnectionBasedClient 对象 。那这是不是就意味着我们要把这个 map 定义在 ConnectionManager 连接管理器中呢？显然不应该这么做，ConnectionManager 连接管理器是用来存放并且管理客户端连接的，这个连接管理器还负责执行 nacos 服务端的健康检测功能，定期清除过期的连接对象，它所执行的操作都是和客户端连接对象有关，没必要让连接管理器和 ConnectionBasedClient 对象产生关联。  
  
其实有一个方法非常合适， 就是再引入一个新的组件，那就是客户端管理器，可以把这个管理器定义为 ClientManager 。反正 nacos 服务端会为每一个客户端创建一个 ConnectionBasedClient 对象，这个对象存放的就是 nacos 客户端注册到服务端的服务实例的信息，那我们就完全可以把 ConnectionBasedClient 对象当成被服务端管理的客户端对象，所以再引入一个 ClientManager 客户端管理器，让这个管理器来存放并且管理服务端为每一个客户端创建的 ConnectionBasedClient 对象，岂不美哉？ 这样一来我们只需要在 ClientManager 客户端管理器中定义一个 map，用这个 map 存放服务端为所有客户端创建的 ConnectionBasedClient 对象即可 。当然，我还要再详细补充一点， 虽说在文章中我们引入的是 ClientManager 客户端管理器组件，但是在 nacos 源码中，ClientManager 只是一个接口，其实现类为 ConnectionBasedClientManager，为了贴合 nacos 源码，接下来我在为大家展示代码的时候，也会直接展示 ConnectionBasedClientManager 类的内容，之后提到客户端管理器，指的都是 ConnectionBasedClientManager 类的对象 。好了，根据我们刚才的分析，新引入的这个 ConnectionBasedClientManager 客户端管理器可以定义成下面这样，请看下面代码块。  
以上就是我们引入的客户端管理器，这个管理器就是用来存放并且管理 nacos 服务端为每一个客户端创建的 ConnectionBasedClient 对象的。从上面的代码块中我们还可以看到，这个客户端管理器上还添加了 springboot 的 @Component 注解，这意味着它是被 springboot 容器管理的 bean 对象。当然，上面代码块展示的内容太过简陋，只在客户端管理器中定义一个 map 是远远不够的，至少还应该把向 map 中添加、从 map 里移除 ConnectionBasedClient 对象的方法也定义好吧，所以接下来我们还要把相关的方法添加到我们定义的 ConnectionBasedClient 类中。  
  
本来直接创建 ConnectionBasedClient 对象，然后添加到 map 就可以了，或者根据客户端连接 Id 直接从 map 中 remove() 对应的 ConnectionBasedClient 对象即可，但是我还是要再额外解释一下，这关系到 ConnectionBasedClient 客户端对象的创建逻辑： 实际上，在 nacos 框架源码中，添加 ConnectionBasedClient 到 ConnectionBasedClientManager 客户端管理器中的操作还用到了一个 ClientFactory 组件，这个组件就是客户端工厂的意思，这个客户端工厂本身是一个接口，在其之下有一个实现类，那就是 ConnectionBasedClientFactory 工厂。nacos 服务端在创建 ConnectionBasedClient 客户端对象的时候，会首先根据服务端当前接收到的客户端连接类型得到对应的客户端工厂；如果是 GRPC 连接类型，那么得到的就是专门创建 ConnectionBasedClient 客户端对象的 ConnectionBasedClientFactory 客户端工厂对象，然后再用这个工厂对象直接创建 ConnectionBasedClient 对象即可 。这就是 nacos 服务端创建 ConnectionBasedClient 对象的流程，大家清楚这个逻辑即可，因为这些类都非常简单，每一个类中只有一个或两个方法，所以我就不在文章中展示了，接下来就让大家看一看我重构之后的 ConnectionBasedClientManager 客户端管理器，请看下面代码块。  
上面代码块中的内容都很简单，而且代码注释非常详细，所以我就不重复讲解其中的逻辑了。总之，现在我们重构了 ConnectionBasedClientManager 客户端管理器，大家也清楚客户端管理器创建以及存放 ConnectionBasedClient 客户端对象的逻辑了： 只要客户端管理器中的 clientConnected(Connection connect) 方法一旦被调用，也就是上面代码块中第 22 行的方法被调用，那么客户端管理器就会为这个客户端连接创建对应的 ConnectionBasedClient 客户端对象，然后把客户端连接 Id 和对应的客户端对象存放到成员变量 clients 中 。好了，到此为止文章开头列出的第二个问题就被我们解决了， 我们已经知道了要使用 ConnectionBasedClientManager 客户端管理器来存放 nacos 服务端为每一个客户端创建的 ConnectionBasedClient 客户端对象 。但是要说完全解决也不太恰当，因为我们刚才分析了，只有客户端管理器中的 clientConnected(Connection connect) 方法被调用，客户端管理器才会为该客户端连接对象创建 ConnectionBasedClient 客户端对象，然后把客户端连接 Id 和客户端对象存放到客户端管理器的 clients 成员变量中， 但是 clientConnected(Connection connect) 方法什么时候被调用呢？只有这个方法真正被调用了，nacos 服务端才会真正为客户端创建 ConnectionBasedClient 对象，然后 nacos 客户端向服务端注册的服务实例信息才能存放到对应的 ConnectionBasedClient 客户端对象中 。所以，接下来我们就要顺手解决这个新的问题，思考一下客户端管理器中的 clientConnected(Connection connect) 方法要在哪里被调用。  
  
这时候就让我们的注意力再次回到 ConnectionManager 连接管理器中吧，因为我们之前就已经明确了，nacos 服务端为每一个客户端创建 ConnectionBasedClient 对象的操作应该放在成功注册客户端连接对象之后，并且我们能看到，在我们目前定义的 ConnectionBasedClientManager 客户端管理器的 clientConnected(Connection connect) 方法中需要一个连接对象参数，既然这样， 那我为何不在客户端连接对象成功注册到连接管理器之后，再调用 ConnectionBasedClientManager 客户端管理器的 clientConnected(Connection connect) 方法呢？也就是在 ConnectionManager 的 register() 方法中执行以上操作，这样一来就可以把客户端连接对象直接传递到客户端管理器的 clientConnected(Connection connect) 方法中了 。就像下面代码块展示的这样，请看下面代码块。  
通过上面代码块大家可以看到，实际上我并没有对 ConnectionManager 连接管理器的 register() 方法进行重构，因为我们遇到了一个问题，那就是目前我们无法在 ConnectionManager 连接管理器中得到 ConnectionBasedClientManager 客户端管理器，也就无法调用客户端管理器中的关键方法为 nacos 客户端创建 ConnectionBasedClient 客户端对象了。这个问题想必大家已经看出来了，那该如何解决呢？最简单的方法就是把 ConnectionBasedClientManager 客户端管理器定义为 ConnectionManager 连接管理器的成员变量，反正 ConnectionBasedClientManager 客户端管理器上添加了 springboot 的 @Component 注解，如果真的这么做了，那么 springboot 的自动注入机制会自动帮我们给 ConnectionManager 连接管理器的成员变量赋值。这一点大家可以理解吧？本来我们完全可以这么做，只不过就是连接管理器的内容稍微杂乱一些，但是在 nacos 源码中采用了另一种方式， 那就是使用了监听回调机制 。 所谓监听回调机制就是在 nacos 服务端定义了一个监听器，这个监听器会一直监听是否有客户端连接对象成功注册到 ConnectionManager 连接管理器中了，一旦监听到该操作成功了，这个监听器就会执行它内部的方法，该方法就会为当前处理的 nacos 客户端创建 ConnectionBasedClient 客户端对象 。我这么一说大家可能就立刻明白了，因为这种监听回调机制在实现 nacos 客户端的时候，大家已经见过很多了。既然我们是仿照 nacos 源码来实现自己的程序，所以接下来我自然也要用监听回调直接重构我们的程序，那该怎么重构呢？  
  
引入监听器和监听器注册表  
  
我们已经明确了要使用监听回调机制来重构程序，那首先就得先实现监听器吧，但是要我说这个监听器其实就是现成的，通过上一小节的分析， 我们已经知道了只要在客户端连接对象成功注册到 ConnectionManager 连接管理器中，也就是在 ConnectionManager 的 register() 方法中调用 ConnectionBasedClientManager 客户端管理器的 clientConnected(Connection connect) 方法，这样一来客户端管理器就会为对应的 nacos 客户端创建 ConnectionBasedClient 客户端对象，然后存放到客户端管理器的 clients 成员变量中 。由此可见，客户端管理器的 clientConnected(Connection connect) 方法只要被调用了，程序就会执行我们期望的操作， 那我直接就把这个 clientConnected(Connection connect) 方法定义为要被回调的关键方法不就好了？而该方法又在 ConnectionBasedClientManager 客户端管理器之后，那么我直接把客户端管理器定义为监听器不就行了 ？实际上在 nacos 源码中正是这么做的， 在程序内部定义了一个 ClientConnectionEventListener 监听器抽象类，然后让客户端管理器继承了该类 ，这样的话，客户端管理器就被同时被定义为了一个监听器，接下来我们就看看在代码层面是怎么实现这些的。首先是 ClientConnectionEventListener 监听器接口，请看下面代码块。  
接下来是重构之后的 ConnectionBasedClientManager 客户端对象，请看下面代码块。  
从上面代码块中可以看到，ConnectionBasedClientManager 客户端管理器继承了 ClientConnectionEventListener 监听器抽象类，这样一来客户端管理器就成为一个监听器了。但成为监听器只不过是第一步，重要的是让监听器中的 clientConnected(Connection connect) 方法被及时回调，要想实现这个操作就要把目光投向 ClientConnectionEventListener 监听器抽象类中了，也就是本小节我为大家展示的第一个代码块。在第一个代码块中， 我们可以看到 ClientConnectionEventListener 监听器抽象类中定义了一个 ClientConnectionEventListenerRegistry 类型的成员变量，我在代码注释中把该成员变量成为监听器注册表，同时大家还可以在第一个代码块中看到其内部定义了一个添加了 @PostConstruct 注解的 init() 方法，该方法会在客户端管理器被 srpingboot 创建之后被调用。而在 init() 方法中执行了监听器注册表对象的 registerClientConnectionEventListener() 方法 。我知道大家可能对我刚才说的这些感到困惑，先不要急，请让我先把这个 ClientConnectionEventListenerRegistry 注册表的内容展示出来，我相信看了这个注册表的内容之后，大家就明白刚才的那些操作有什么作用了，请看下面代码块。  
在上面代码块中， 大家可以看到我在新引入的 ClientConnectionEventListenerRegistry 注册表也是一个被 srpingboot 容器管理的 bean 对象 ，其中定义了一个 clientConnectionEventListeners 集合成员变量，这个成员变量专门存放客户端监听器对象，而 ConnectionBasedClientManager 客户端管理器就是一个监听器对象，然后客户端管理器就通过某些方式存放到该集合中。同时大家也能看到我在注册表中定义了三个方法，第三个方法就是之前在 ClientConnectionEventListener 监听器抽象类中看到的方法，在监听器抽象类的 init() 方法中，注册表对象调用了它的 registerClientConnectionEventListener() 方法。现在我们可以知道了，这个方法的作用就是把一个监听器对象添加到自己的集合成员变量中，而添加的这个监听器其实就是客户端管理器。通过这一系列操作，我们可以清楚地意识到： 在 springboot 启动的过程中，在创建了 ConnectionBasedClientManager 客户端管理器这个 bean 对象之后，就会执行其父类的 init() 方法，然后把这个监听器对象添加到注册表对象的 clientConnectionEventListeners 集合中 。这个流程想必大家已经清楚了吧？那这个流程有什么作用呢？这就要从 ClientConnectionEventListenerRegistry 注册表对象的另外两个方法入手了。  
  
从本小节展示的第一个代码块中可以看到，在 ClientConnectionEventListenerRegistry 客户端监听器注册表这个类中，还定义了 notifyClientConnected() 和 notifyClientDisConnected() 这两个方法，这两个方法分别会在客户端和服务端连接建立和断开之后被回调。 尤其是 notifyClientConnected() 方法，该方法会在 nacos 服务端接收到客户端连接之后被回调，并且回调方法的逻辑正是执行客户端对象的 clientConnected(connection) 方法。这样一来，岂不是只要在客户端连接对象成功注册到连接管理器之后，也就是在 ConnectionManager 连接管理器的 register() 方法中，直接回调客户端监听器注册表的 notifyClientConnected() 方法，不就可以让 nacos 服务端为对应的客户端创建 ConnectionBasedClient 客户端对象了吗 ？当然，要想在 ConnectionManager 连接管理器的 register() 方法中调用监听器注册表的 notifyClientConnected() 方法，显然就应该让连接管理器能够使用监听器注册表对象，这个很好办， 直接把 ClientConnectionEventListenerRegistry 监听器注册表对象定义为连接管理器内部的一个成员变量即可 。如果大家理解了这个逻辑，那么接下来就请看一下我最后重构完毕的 ConnectionManager 连接管理器，请看下面代码块。  
到此为止，文章开头列出的第 2 个问题，才算彻底解决了。代码块中的注释非常详细，每一个组件引入的原因，以及实现的原理我已经分析得非常清楚了，大家可以仔细看看文章，然后自己梳理梳理，我就不再带着大家梳理了，因为还有两个问题等着我们马上解决呢。  
  
引入 EphemeralClientOperationServiceImpl 组件，重构 InstanceRequestHandler 请求处理器  
  
本章开始列出的第 3 和第 4 个问题是关于 InstanceRequestHandler 请求处理器如何重构，以及 nacos 客户端向服务端注册的服务实例信息怎么存放到对应的 ConnectionBasedClient 客户端对象中的。现在我把这两个问题再次搬运到这里，帮助大家回顾一下：  
3 为每一个客户端创建了 ConnectionBasedClient 对象之后，也按照刚才分析的映射关系存储好了 ConnectionBasedClient 对象之后，当 nacos 客户端注册服务实例信息到客户端时，这些信息该怎么存放到与 nacos 客户端对应的 ConnectionBasedClient 对象中呢？  
4 InstanceRequestHandler 请求处理器该如何重构呢？ConnectionBasedClient 需要被定义为 InstanceRequestHandler 请求处理器的成员变量吗？  
很好，现在我要解释一下，我之所以把这两个放到一起，是因为这两个问题的联系非常紧密，确切地说，这两个问题会随着某个新组建的引入被一起解决，所以我就把这两个问题放到一起来讲解了。其实这两个问题很容易就能解决，并且突破口也很容易寻找。虽然剩余的两个问题以 3、4 排序，但真正解决的时候，要先从第 4 个问题寻找突破口。原因很简单，因为 nacos 客户端向服务端注册的服务实例信息都会被封装到 InstanceRequest 请求中发送给服务端，而这个 InstanceRequest 请求会被 InstanceRequestHandler 请求处理器处理，可以这么说： InstanceRequestHandler 请求处理器是正式处理 InstanceRequest 请求的第一步，也是处理客户端注册的服务实例信息的源头 。那我们目前实现的 InstanceRequestHandler 请求处理器是什么样的呢？我把它的代码搬运到下面了，请看下面代码块。  
可以看到，再引入了 ConnectionBasedClient 客户端对象，明确了 nacos 客户端注册的服务实例信息的存储方式之后，我就把之前定义在 InstanceRequestHandler 请求处理器中的 map 成员变量移除了，现在这个请求处理器的 handle() 方法仍然没有完全实现。但是大家也不必着急，接下来我就会为大家重构这个请求处理器了。  
  
首先请大家先思考一下，在我们实现 nacos 客户端向服务端注册服务实例功能时，定义了 InstanceRequest 请求，该请求中封装着要注册的服务实例信息，也就是说只要 nacos 客户端发送 InstanceRequest 请求到服务端，就意味着要注册服务实例信息了；但是大家也别忘了，实际上当 nacos 客户端想注销某些服务实例信息时，也会向服务端发送 InstanceRequest 请求，因为在该 InstanceRequest 请求对象中有一个 type 成员变量，这个成员变量是 String 类型的，表示该请求的类型，服务端会根据这个 type 成员变量判断接收到的 InstanceRequest 请求究竟是注册服务实例的请求还是注销服务实例的请求。因此， 在 InstanceRequestHandler 请求处理器中，应该首先对接收到的 InstanceRequest 请求的类型做判断，然后根据请求类型执行具体的操作 。所以，经过分析之后，InstanceRequestHandler 请求处理器应该进行以下重构，请看下面代码块。  
可以看到，现在 InstanceRequestHandler 请求处理器被重构之后，显然有条理了很多。在 handle() 方法中处理接收到的 InstanceRequest 请求时，会判断请求的类型，然后根据请求的不同类型执行不同的操作。也正因此，我还为 InstanceRequestHandler 请求处理器新定义了 registerInstance() 和 deregisterInstance() 方法，虽然这两个方法暂时没有实现。但是，离实现也不远了。因为接下来要执行的操作非常简单， 在 registerInstance() 方法中传入了一个 Service 参数对象，还有一个 InstanceRequest 参数对象；Service 对象封装了要注册的服务实例所在服务的信息，而 InstanceRequest 请求对象封装了服务实例真正的信息 ，这两个都得到了， 接下来直接得到 nacos 服务端为对应的客户端创建的 ConnectionBasedClient 客户端管理器，然后把 Service 和服务实例信息以键值对的方式存放到客户端管理器的 map 成员变量中不就行了吗 ？而 ConnectionBasedClient 客户端对象是在 ConnectionBasedClientManager 客户端管理器中存放着的，根据客户端连接 Id 就可以从客户端管理器中得到对应的 ConnectionBasedClient 客户端对象，那这样一来，是不是就要把 ConnectionBasedClientManager 客户端管理器这个被 springboot 容器管理的 bean 对象定义为 InstanceRequestHandler 请求处理器的成员变量呢？  
  
我的观点是当然可以这么做，这么做完全没有问题，但是在 nacos 源码中并不是这么做的， 因为我们目前注册的都是临时服务实例信息到服务端，有临时服务实例信息就有持久化的服务实例信息，nacos 为了区分这两种情况，就为它们分别定义了两个不同的客户端处理器，这一章我们要引入的就是专门处理临时服务实例信息的客户端服务组件，也就是 EphemeralClientOperationServiceImpl 类 。引入了这个类之后， 就要把客户端管理器定义为这个 EphemeralClientOperationServiceImpl 类的成员变量了，因为不管怎么说，服务实例信息最终还是要存放到客户端管理器管理的 ConnectionBasedClient 对象中 。当然，这时候大家应该也能想到了， InstanceRequestHandler 请求处理器中当然要定义成员变量，只不过这个成员变量是 EphemeralClientOperationServiceImpl 对象。 接下来就让我为大家把重构之后的 InstanceRequestHandler 请求处理器以及新引入的 EphemeralClientOperationServiceImpl 类展示给大家。  
  
首先是最后重构完毕的 InstanceRequestHandler 请求处理器，请看下面代码块。  
然后就是最新引入的 EphemeralClientOperationServiceImpl 类，请看下面代码块。  
到此为止，我相信大家终于就清楚了 nacos 客户端注册的服务实例信息究竟是怎么注册到服务端的，以及是如何在服务端保存的了，文章开头列出的第 3、4 个问题也就最终得到解决了。当然，唯一需要补充的一点就是我一直还没有为大家完整实现 ConnectionBasedClient 客户端管理器，这个管理器中的 addServiceInstance() 方法和 removeServiceInstance() 方法都没有实现，但这些方法现在都很好实现了，无非就是直接把服务实例信息存放到内部的成员变量中而已，所以接下来我就为大家展示一下重构之后的 ConnectionBasedClient 客户端管理器，请看下面代码块。  
到此为止，我就把 nacos 服务端缓存客户端注册的服务实例信息的功能完全实现了，大家可以去认真看看我提供的第七版本代码了，这个版本代码对应的文章全部更新完毕了。当然，这一章的内容很多，确切地说，第七版本代码对应的内容非常多，又是搭建 nacos 服务端，又要引入 grpc 框架的一些知识，还要实现最核心的缓存服务实例信息功能，总之引入了非常多的组件，而这些组件系统工作的原理，方法调用的流程也比较复杂，我希望大家一定要亲自把文章中程序执行流程的脉络梳理梳理，这对大家掌握 nacos 这部分功能非常有帮助。以这种方式学习，肯定就不用怀疑自己了，你绝对可以精通 nacos 这个框架的源码，精通了框架的源码，再加上工作经验的积累，这就离你精通运用整个 nacos 框架不远了。所以大家一定要亲自梳理梳理这几章的内容。  
  
还有一点需要补充，那就是大家在阅读我提供的第七版本代码时，会在代码中看见一个 ClientManagerDelegate 类，这个类并没有在文章中出现，这个类其实就是一个客户端管理器代理类，它也实现了 ClientManager 这个客户端管理器接口。这个类的内容非常简单，大家自己看看就行，之所以存在这个客户端管理器代理类，是因为在 nacos 服务端有多个客户端管理器，本章使用的只是其中一种，这个后面再说吧。  
  
好了，到此为止我们就把 nacos 服务端缓存服务实例信息的功能实现完毕了，和这个功能相关的其他功能也都实现了，按照顺序， 接下来就该实现 nacos 服务端订阅服务实例功能，然后就是实现服务端主动推送服务实例信息给客户端功能，这中间我还会用一章的篇幅把 nacos 服务端注销服务实例、取消服务实例订阅功能实现完毕，然后搭建 distro 集群和 jraft 集群即可，这些都实现完毕了，整个 nacos 课程就更新了三分之二了，只差最后一个配置中心功能，这门课程就更新完毕了 。之后我会继续为大家迭代 rocketmq 的代码，大家也不必心急，只要想学，总有学不完的框架，一个一个来就行。好了诸位，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/fomarwqy7heuwne6*  
*All content belongs to its respective owners and creators.*