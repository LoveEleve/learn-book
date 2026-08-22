从本章开始，我们要就开始一起构建 nacos 的配置中心了。说实话，其实 nacos 的配置中心我都不太想更新了，原因有三点：  
1 nacos 的配置中心设计得非常简单，不管是配置中心客户端还是服务端，都非常简单，根本没有什么复杂的逻辑，代码量也很少。  
2 nacos 的配置中心模块用到了很多以前已经实现的功能组件，比如说构建 nacos 配置中心客户端和服务端，用的都是我们在实现注册中心时构建的 RpcClient 和 RpcServer 等等组件，总之，很多重要的组件，甚至是构建配置中心客户端的方式，都和注册中心没什么区别，我一会给大家展示一下代码，大家就清楚了。  
3 在更新 nacos 框架之前，我已经把 hippo4j 动态线程池框架给大家更新完毕了，在更新 hippo4j 框架的时候，我就跟大家说过，这个框架大量借鉴和复制了 nacos 配置中心的代码，就动态变更配置功能来说，这两个框架的设计原理以及用到的功能组件不能说是一模一样，也能说是差不多了。无非就是 hippo4j 使用的是长轮询，nacos 配置中心使用的是长连接，而长连接比长轮询更简单了。还有一点，那就是 hippo4j 框架是我们这个系列课程中最简单的，如果大家把 hippo4j 这个框架看完了，那么在学习 nacos 配置中心时，就会感觉非常简单，甚至不必看文章，直接去看我提供的第十四和十五版本代码即可。  
以上就是我认为的没必要更新 nacos 配置中心的原因，或者说没必要详细更新 nacos 配置中心的原因。当然，我也能猜到，就算我列完以上三个原因，还是有朋友会觉得我有点夸张了，或者认为我想偷懒，不想认认真真保证质量地更新课程。我可以跟大家保证，我绝对没有这种意思，如果大家不相信，可以先看看本章的内容， 在本章我会先为大家把 nacos 配置中心客户端构建完毕，并且实现向服务端发布配置信息和读取配置信息功能 ，看完之后，大家了解了 nacos 配置中心有多简单，就会相信我刚才所说的了。  
  
引入 nacos 配置中心 NacosConfigService 服务组件  
  
请大家先回忆一下，在构建 nacos 注册中心客户端的时候，我们是怎么做的呢？也许大家已经记不太清楚了，没关系我帮大家简单回忆一下。实际上，我们并没有直接创建 nacos 注册中心客户端对象，而是首先定义了一个 NacosNamingService 类，这个类的对象专门提供和 nacos 注册中心相关的各种服务，比如注册服务实例信息到服务端，向服务端订阅某个 Service 等等。而在创建 NacosNamingService 对象的过程中，nacos 客户端也就被创建成功，并且被启动了，之后 nacos 注册中心客户端就可以和服务端成功建立连接，展开通信。我把相关的代码搬运到这里了，帮助大家回顾一下，请看下面代码块。  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/4/10

\* @方法描述：这个类是注册中心客户端的核心类，可以说是注册服务实例，订阅服务实例等等操作的入口类，很多重要的操作都是由这个类的对象发起的

\* 但在第一版本代码中，该类只提供了注册服务实例到注册中心的方法

\*/

@ SuppressWarnings ("PMD.ServiceOrDaoClassShouldEndWithImplRule")

public class NacosNamingService implements NamingService {  
  
//其他成员变量省略  
  
//代理客户端，该客户端就是用来向注册中心服务器发送请求的

//这里之所以叫代理客户端，是因为NamingClientProxy内部真正向服务器发送消息时，使用的是grpc构建的客户端

private NamingClientProxy clientProxy;  
  
  
//构造方法，该构造方法通过Properties文件创建NacosNamingService对象，反射调用的就是这个构造方法

//至于被反射调用的时机，就是在NamingFactory对象中被调用的

public NacosNamingService (Properties properties) throws NacosException {

init (properties);

}  
  
//初始化NacosNamingService对象的方法，在该方法对NacosNamingService内部要使用的一些组件进行了创建和初始化

private void init (Properties properties) throws NacosException {  
//该方法的其他内容省略

//创建代理客户端对象

this.clientProxy \= new NamingClientProxyDelegate (this.namespace,serviceInfoHolder,nacosClientProperties,changeNotifier);

}  
到此为止，我就帮大家把nacos注册中心客户端构建并启动的过程回顾完毕了，接下来还是继续把目光集中在 nacos 配置中心上吧，现在要构建配置中心客户端，并且我之前也说了，nacos 配置中心客户端用到了很多以前实现过的组件，而且也是仿照着注册中心客户端的流程来构建的。那这就很好说了， 我们就可以仿照注册中心客户端，也给 nacos 配置中心客户端定义一个 Service 服务组件，这个组件专门负责 nacos 配置中心向服务端发布配置信息、以及获取配置信息、还有订阅配置信息等功能。这个服务组件就可以定义为 NacosConfigService ，具体实现可以仿照着 nacos 注册中心的 NacosNamingService 来实现。比如说， 我可以在专门为配置中心定义的 NacosConfigService 组件中定义该服务发布的配置信息所属的 namespace 命名空间，也可以在 NacosConfigService 中创建配置中心客户端要使用的 ServerListManager 服务地址管理器，还可以把发布配置信息，查询配置信息等等方法都先定义一下(发布配置信息这些基础知识应该不用我再详细讲解了吧？我相信只要使用过 nacos 配置中心的朋友们都知道发布配置信息的方法，你可以自己定义配置信息的 dataId，该配置信息所属的 group，以及 namespace。在相同的命名空间和 group 下，每一个发布到配置中心服务端的配置信息，或者说配置文件都有一个唯一的 dataId，这些知识大家应该都清楚了，我就不在文章中讲解了) 。所以，我们专门为 nacos 配置中心引入的 NacosConfigService 组件目前可以先简单定义成下面这样，请看下面代码块。  
上面代码块就展示了我们目前为 nacos 配置中心定义好的 NacosConfigService 组件，在看完上面的代码块之后，大家肯定会有很多问题，就比如说， 上面的代码块中根本没有定义和客户端相关的任何成员变量 ，大家肯定会对此感到困惑。当然，我自己把上面的 NacosConfigService 类定义完毕之后，心里也有很多问题，这也是合情合理的，毕竟 NacosConfigService 类目前只是个半成品，它还有很多不完善的地方，很多方法没有实现，也没有定义。就比如说， 我们在 NacosConfigService 对象构造方法中创建了配置中心客户端要使用的 ServerListManager 服务地址管理器，但是我们并没有使用这个服务地址管理器。这一点肯定会让大家感到困惑，而且还让大家感到困惑的可能是这个 ServerListManager 服务地址管理器本身 。虽然我在上面代码块中创建了 ServerListManager 服务地址管理器对象，但是大家目前还并不知道 nacos 配置中心要使用的服务地址管理器是怎么定义的，接下来就让我为大家简单解释一下。  
  
首先让我来为大家解释第一个问题，那就是明明在新定义的 NacosConfigService 类的构造方法中创建了 ServerListManager 服务地址管理器，但是并没有用到它。其实原因非常简单，我们都知道，服务地址管理器肯定是要被客户端使用的，nacos 注册中心客户端就拥有自己的 ServerListManager 服务地址管理器，配置中心客户端当然也要拥有。但目前我们还没有把配置中心的客户端 NacosConfigService 类中，所以也就还没有用到创建完毕的这个配置中心客户端 ServerListManager 服务地址管理器。  
  
至于 nacos 配置中心的 ServerListManager 服务地址管理器是如何定义的，这个就更简单了，和 nacos 注册中心客户端用到的 ServerListManager 服务地址管理器几乎一样，所以我就不在本章中重复讲解了，大家直接去看我提供的第十四版本代码即可，代码中注释非常详细。  
  
好了，在简单回答了和配置中心客户端要使用的 ServerListManager 服务地址管理器的一些问题之后，接下来再让我为大家展示一个 NacosConfigService 服务组件的使用例子， 在这个例子中会为大家展示 NacosConfigService 类的对象如何被创建，以及如何使用 NacosConfigService 对象向配置中心服务端发布配置信息 。当然，我也知道大家信息很着急， 希望我能尽快实现 nacos 配置中心的客户端，并且实现配置中心客户端向服务端发布配置信息的功能，也就是实现上面代码块中的 publishConfig() 方法 。大家先别急，重要的功能和知识肯定是放到后面实现和讲解，等下一小节，我就会为实现这部分的功能了。接下来，还是请大家先看一下 NacosConfigService 对象的使用方式，请看下面代码块。  
上面代码块就展示了 nacos 配置中心的 NacosConfigService 组件的具体用法，结合上面代码块中的注释，大家应该也清楚了配置中心客户端怎么从服务端查询指定的配置信息，以及怎么向配置中心发布配置信息。当然，大家目前也只是清楚 NacosConfigService 对象的使用方式，还并不了解程序内部的运行原理，因为到目前为止，我还没有为 nacos 配置中心引入客户端对象，也没有真正实现 NacosConfigService 类的最重要的 publishConfig() 发布配置信息的方法。接下来就让我来实现这些功能吧。  
  
引入 ClientWorker，启动配置中心客户端  
  
我在上一小节一直没有为 NacosConfigService 组件定义客户端对象， 是因为在 nacos 源码中，配置中心客户端并不是定义在这个类中的 。之前我已经跟大家分析了，nacos 配置中心使用的也是我们早就构建完毕的 RpcClient 客户端对象，那看起来只要把 RpcClient 客户端对象定义在 NacosConfigService 组件中，然后在创建 NacosConfigService 对象的时候，同时也创建 RpcClient 对象，并且把早就创建完毕的 ServerListManager 服务地址管理器交给 RpcClient 对象使用，最后启动 RpcClient 客户端对象，这样一来，nacos 配置中心的客户端就启动成功了，也就可以和服务端建立连接，然后展开通信了。我这样分析下来，应该没有朋友会反对吧？这个流程非常普通，也很容易理解。但当大家看过 nacos 配置中心客户端的源码后，就会发现情况并不总是我们想象的这样， 在 nacos 配置中心源码中，还引入了一个新的组件，就是本小节标题中展示的 ClientWorker 类，翻译过来就是负责客户端工作的组件。而我们要为配置中心构建的客户端，其实就定义在了 ClientWorker 类中 ，并且通过这个 ClientWorker 类的名字我们大概也能意识到， 既然这个组件是用来负责配置中心客户端工作的，它也持有了配置中心客户端对象，那么向配置中心服务端发送消息的任务肯定就会交给 ClientWorker 类的对象来执行 。这样分析下来，我相信每一位朋友都能想到， 真正向配置中心服务端发布配置信息，从服务端查询指定配置信息的操作其实也都是 ClientWorker 类的对象执行的 。所以这个 ClientWorker 类目前可以定义成下面这样，请看下面代码块。  
上面代码块展示的就是我仿照源码定义完毕的 ClientWorker 类，可以看到，我们把配置中心要用到的 RpcClient 客户端对象定义 ClientWorker 类的成员变量了，并且在 ClientWorker 类的构造方法中创建并启动了配置中心客户端。这其实就意味着， 只要 ClientWorker 类的对象一被创建，配置中心客户端也就启动了 。那么 ClientWorker 类的对象应该在哪里被创建呢？  
  
看到这里，我想大家应该也都能猜到了， 既然真正向配置中心服务端发布配置信息，从服务端查询指定配置信息的操作其实也都是 ClientWorker 类的对象执行的，而 NacosConfigService 类中也定义发布配置信息，查询配置信息的方法，这就意味着 NacosConfigService 服务组件只是操作的发起者，ClientWorker 类的对象才是真正的执行者 。所以， 我们只需要把 ClientWorker 对象定义为 NacosConfigService 类的成员变量，在创建 NacosConfigService 对象的时候，在构造方法中把 ClientWorker 对象创建出来即可，这样一来，配置中心客户端也就被创建启动了 。所以，NacosConfigService 类就可以重构成下面这样，请看下面代码块。  
上面代码块中的注释非常详细，所以我就不在文章中重复解释了。看完重构之后的 NacosConfigService 类，现在大家应该彻底清楚了 nacos 配置中心客户端 NacosConfigService 服务组件的工作原理了吧？确切地说， 重要的并不是 NacosConfigService 组件，这个组件只是和服务端打交道的操作的发起者，真正的执行者是 ClientWorker 对象，真正重要的也是 ClientWorker 对象 。所以接下来我们还是要再回到 ClientWorker 类中，把 ClientWorker 类中真正向服务端发布配置信息的方法、以及从服务端查询配置信息的方法实现了。当然，首先要实现的肯定是客户端向服务端发布配置信息的方法，只有发布了配置信息，才能查询呀，所以接下来，我们就一起来分析分析，ClientWorker 对象的 publishConfig() 方法该怎么实现。  
  
其实这个也很好分析，毕竟我们已经有了实现 nacos 注册中心的经验。 在 nacos 框架中，不管注册中心还是配置中心，其客户端和服务端底层都是使用 grpc 框架进行通信的，通信的方式非常直接，客户端发送对应的请求给服务端，服务端有对应的请求处理器可以处理该请求，这是我们实现 nacos 注册中心时，实现的客户端和服务端的通信方式 。来到 nacos 的配置中心，现在配置中心客户端要和服务端进行通信了，并且要进行的是发布配置信息的通信， 那就直接定义一个请求对象，客户端把配置信息都封装在这个请求对象中，然后把请求对象发送给服务端，服务端使用对应的处理器处理该请求对象，从请求对象中解析出配置信息，然后保存即可 。这个逻辑大家应该都能立即吧？看起来非常简单，就是我们曾经实现过无数次的功能了。当然，为配置中心服务端定义对应的请求处理器处理客户端发布的配置信息，这个功能并不会在本章实现，后面我们构建配置中心服务端的时候，会实现对应的功能。那接下来，我们只需要把要发送给配置中心服务端，封装了配置信息的请求对象定义出来即可，有请求对象就意味着有对应的响应对象，所以我也会把与该请求对象对应的响应对象一起定义出来。  
  
我把这个封装了配置信息的请求对象定义为 ConfigPublishRequest，具体实现请看下面代码块。  
与其对应的响应对象就是 ConfigPublishResponse，请看下面代码块。  
好了，请求对象和响应对象都定义完毕之后，接下来就该真正实现 ClientWorker 类的 publishConfig() 方法了，具体的实现逻辑刚才我们已经分析过了， 就是创建一个 ConfigPublishRequest 请求对象，把配置中心客户端要发布的配置信息封装在请求对象中，然后使用 RpcClient 客户端对象发送给配置中心服务端即可 ，请看下面代码块。  
上面代码块中的逻辑和流程都非常清晰，我就不在为大家重复讲解了。总之现在，我们已经实现了 nacos 配置中心客户端向服务端发布配置信息的功能，也许大家会觉得我实现得非常简单，但我可以很负责任地告诉大家，就算你看了 nacos 配置中心的源码，你会发现它真的就是这么简单，源码中配置中心客户端的内容和我为大家提供的第十四版本代码的内容没什么区别，并且就算你从源码中把 nacos 的整个配置中心客户端看完，你会发现它也就只有那么点的类，内容很少，也非常简单。当然，大家不必怀疑，我肯定没有忘记我在上面代码块的第 65 行展示了这样一行代码，请看下面代码块。  
并且我还跟大家说，这行代码中的 encryptedDataKey 这个变量肯定会让大家感到疑惑，实际上，这个 encryptedDataKey 变量在之前大家就见过了，当时我说让大家先记住它，我很快就会解释，它本身就是 ClientWorker 类的 publishConfig() 方法中的一个重要参数。那这个参数是什么呢？现在我们就一起揭开它的面纱吧， 它就是 nacos 配置中心要发布到服务端的配置文件的密钥 。实际上， 在配置信息被客户端发布到服务端的时候，会经过严格的保密处理，也就是对配置信息进行加密，与其相关的密钥也会被一起传输给服务端，等需要的时候，就会使用这个密钥来给配置文件解密 。这就是 encryptedDataKey 变量在 nacos 配置中心客户端中的作用。大家肯定很好奇这个 encryptedDataKey 变量是从哪里传递过来的，因为现在大家看到的是， 这个 encryptedDataKey 变量是 ClientWorker 类的 publishConfig() 方法的一个重要参数 ，那就很简单了，我们只需要看看 ClientWorker 类的 publishConfig() 方法在哪里被调用了，这就不用分析了， 就是在 NacosConfigService 类的 publishConfigInner() 方法中被调用了 ，我把对应的代码又搬运过来了，请看下面代码块。  
但在上面的代码块中，也就是在 NacosConfigService 类的 publishConfigInner() 方法中， 大家既没有看到对要发布的配置信息进行加密的操作，因为只有对其进行加密了，才能得到对应的密钥；当然，没有 encryptedDataKey 密钥，更别说把 encryptedDataKey 密钥传入到 ClientWorker 对象的 publishConfig() 方法中了。还是那句话，大家先别急，现在什么都没有，是因为 NacosConfigService 类的 publishConfigInner() 方法肯定要经过重构，重构之后就全都有了，而且在具体重构该方法之前，我还要引入一个非常重要的功能组件，就是配置文件过滤器，这个过滤器就是对配置信息进行加密和解密的 。当然，这些内容在这一章是实现不了了，我就放到下一章为大家实现了，在下一章看完之后，我提供给大家的第十四版本代码，大家就全都可以看了。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/hhpqaxg56029vxxz*  
*All content belongs to its respective owners and creators.*